package com.antelop.alarm.data

import android.content.ContentResolver
import android.content.ContentValues
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import com.antelop.alarm.model.ConversationSummary
import com.antelop.alarm.model.IncomingSms
import com.antelop.alarm.model.SmsMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object SmsColumns {
    const val ID = "_id"
    const val THREAD_ID = "thread_id"
    const val ADDRESS = "address"
    const val BODY = "body"
    const val DATE = "date"
    const val TYPE = "type"
    const val READ = "read"
    const val SEEN = "seen"
}

class SmsRepository(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeConversations(): Flow<List<ConversationSummary>> {
        return observeSmsChanges { queryConversations() }
    }

    fun observeMessages(threadId: Long): Flow<List<SmsMessage>> {
        return observeSmsChanges { queryMessages(threadId) }
    }

    suspend fun insertIncomingMessage(message: IncomingSms): Long = withContext(ioDispatcher) {
        val values = ContentValues().apply {
            put(SmsColumns.ADDRESS, message.address)
            put(SmsColumns.BODY, message.body)
            put(SmsColumns.DATE, message.timestampMillis)
            put(SmsColumns.TYPE, 1)
            put(SmsColumns.READ, 0)
            put(SmsColumns.SEEN, 0)
        }
        val uri = contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        uri?.lastPathSegment?.toLongOrNull() ?: -1L
    }

    @Suppress("DEPRECATION")
    suspend fun sendMessage(address: String, body: String) = withContext(ioDispatcher) {
        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(body)
        if (parts.size > 1) {
            smsManager.sendMultipartTextMessage(address, null, parts, null, null)
        } else {
            smsManager.sendTextMessage(address, null, body, null, null)
        }
        val values = ContentValues().apply {
            put(SmsColumns.ADDRESS, address)
            put(SmsColumns.BODY, body)
            put(SmsColumns.DATE, System.currentTimeMillis())
            put(SmsColumns.TYPE, 2)
            put(SmsColumns.READ, 1)
            put(SmsColumns.SEEN, 1)
        }
        contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
    }

    suspend fun markThreadAsRead(threadId: Long): Int = withContext(ioDispatcher) {
        val values = ContentValues().apply {
            put(SmsColumns.READ, 1)
            put(SmsColumns.SEEN, 1)
        }
        contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${SmsColumns.THREAD_ID}=? AND ${SmsColumns.TYPE}=? AND ${SmsColumns.READ}=?",
            arrayOf(threadId.toString(), "1", "0"),
        )
    }

    private fun <T> observeSmsChanges(loader: suspend () -> List<T>): Flow<List<T>> {
        return callbackFlow {
            val handler = Handler(Looper.getMainLooper())
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    launch(ioDispatcher) {
                        trySend(loader())
                    }
                }
            }
            launch(ioDispatcher) {
                trySend(loader())
            }
            contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }.flowOn(ioDispatcher)
    }

    private suspend fun queryConversations(): List<ConversationSummary> = withContext(ioDispatcher) {
        val projection = arrayOf(
            SmsColumns.ID,
            SmsColumns.THREAD_ID,
            SmsColumns.ADDRESS,
            SmsColumns.BODY,
            SmsColumns.DATE,
            SmsColumns.TYPE,
            SmsColumns.READ,
        )
        val messages = mutableListOf<SmsMessage>()
        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${SmsColumns.DATE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(SmsColumns.ID)
            val threadIdIndex = cursor.getColumnIndexOrThrow(SmsColumns.THREAD_ID)
            val addressIndex = cursor.getColumnIndexOrThrow(SmsColumns.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(SmsColumns.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(SmsColumns.DATE)
            val typeIndex = cursor.getColumnIndexOrThrow(SmsColumns.TYPE)
            val readIndex = cursor.getColumnIndexOrThrow(SmsColumns.READ)
            while (cursor.moveToNext()) {
                messages += SmsMessage(
                    id = cursor.getLong(idIndex),
                    threadId = cursor.getLong(threadIdIndex),
                    address = cursor.getString(addressIndex).orEmpty(),
                    body = cursor.getString(bodyIndex).orEmpty(),
                    timestampMillis = cursor.getLong(dateIndex),
                    type = cursor.getInt(typeIndex),
                    read = cursor.getInt(readIndex) == 1,
                )
            }
        }
        messages.groupBy { it.threadId }
            .values
            .map { threadMessages ->
                val newest = threadMessages.maxByOrNull { it.timestampMillis } ?: return@map null
                ConversationSummary(
                    threadId = newest.threadId,
                    address = newest.address.ifBlank { "未知号码" },
                    snippet = newest.body.ifBlank { "(空短信)" },
                    timestampMillis = newest.timestampMillis,
                    messageCount = threadMessages.size,
                    unreadCount = threadMessages.count { !it.read && !it.isOutgoing },
                )
            }
            .filterNotNull()
            .sortedByDescending { it.timestampMillis }
    }

    private suspend fun queryMessages(threadId: Long): List<SmsMessage> = withContext(ioDispatcher) {
        val projection = arrayOf(
            SmsColumns.ID,
            SmsColumns.THREAD_ID,
            SmsColumns.ADDRESS,
            SmsColumns.BODY,
            SmsColumns.DATE,
            SmsColumns.TYPE,
            SmsColumns.READ,
        )
        buildList {
            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${SmsColumns.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${SmsColumns.DATE} ASC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(SmsColumns.ID)
                val threadIdIndex = cursor.getColumnIndexOrThrow(SmsColumns.THREAD_ID)
                val addressIndex = cursor.getColumnIndexOrThrow(SmsColumns.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(SmsColumns.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(SmsColumns.DATE)
                val typeIndex = cursor.getColumnIndexOrThrow(SmsColumns.TYPE)
                val readIndex = cursor.getColumnIndexOrThrow(SmsColumns.READ)
                while (cursor.moveToNext()) {
                    add(
                        SmsMessage(
                            id = cursor.getLong(idIndex),
                            threadId = cursor.getLong(threadIdIndex),
                            address = cursor.getString(addressIndex).orEmpty(),
                            body = cursor.getString(bodyIndex).orEmpty(),
                            timestampMillis = cursor.getLong(dateIndex),
                            type = cursor.getInt(typeIndex),
                            read = cursor.getInt(readIndex) == 1,
                        ),
                    )
                }
            }
        }
    }
}
