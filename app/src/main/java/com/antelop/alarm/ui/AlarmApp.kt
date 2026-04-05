@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.antelop.alarm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antelop.alarm.data.SetupController
import com.antelop.alarm.di.appContainer
import com.antelop.alarm.model.AppPreferences
import com.antelop.alarm.model.ConversationSummary
import com.antelop.alarm.model.ExternalComposeRequest
import com.antelop.alarm.model.KeywordEntry
import com.antelop.alarm.model.SmsMessage
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private const val ROUTE_INBOX = "inbox"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_GUIDE = "guide"
private const val ROUTE_COMPOSER = "composer?address={address}&body={body}"
private const val ROUTE_CONVERSATION = "conversation/{threadId}/{address}"

@Composable
fun AlarmApp(
    initialComposeRequest: ExternalComposeRequest,
    onConsumeComposeRequest: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs by container.preferencesRepository.preferences.collectAsStateWithLifecycle(
        initialValue = AppPreferences(),
    )
    var hasLoadedDefaultSmsState by rememberSaveable { mutableStateOf(false) }
    var defaultSmsStatus by remember { mutableStateOf<SetupController.DefaultSmsStatus?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(initialComposeRequest) {
        if (initialComposeRequest.address.isNotBlank() || initialComposeRequest.body.isNotBlank()) {
            navController.navigate(
                "composer?address=${Uri.encode(initialComposeRequest.address)}&body=${Uri.encode(initialComposeRequest.body)}",
            )
            onConsumeComposeRequest()
        }
    }

    LaunchedEffect(container) {
        defaultSmsStatus = container.setupController.refreshAppState()
        hasLoadedDefaultSmsState = true
    }

    DisposableEffect(lifecycleOwner, container) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    defaultSmsStatus = container.setupController.refreshAppState()
                    hasLoadedDefaultSmsState = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasLoadedDefaultSmsState) {
        SetupCheckingScreen()
        return
    }

    if (defaultSmsStatus?.isDefault != true) {
        SetupBlockingScreen(
            setupController = container.setupController,
            prefs = prefs,
            defaultSmsStatus = defaultSmsStatus,
            onConfirmSetup = {
                val isDefault = container.setupController.confirmSetupCompleted()
                defaultSmsStatus = container.setupController.getDefaultSmsStatus()
                isDefault
            },
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                listOf(
                    ROUTE_INBOX to "短信",
                    ROUTE_SETTINGS to "设置",
                    ROUTE_GUIDE to "指南",
                ).forEach { (route, title) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = { navController.navigate(route) },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(
                                        if (currentRoute == route) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                    ),
                            )
                        },
                        label = { Text(title) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == ROUTE_INBOX) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("composer?address=&body=")
                    },
                ) {
                    Text("新短信")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_INBOX,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ROUTE_INBOX) {
                InboxScreen(
                    conversations = container.smsRepository.observeConversations()
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                        .value,
                    onOpenConversation = { summary ->
                        navController.navigate(
                            "conversation/${summary.threadId}/${Uri.encode(summary.address)}",
                        )
                    },
                )
            }
            composable(
                route = ROUTE_CONVERSATION,
                arguments = listOf(
                    navArgument("threadId") { type = NavType.LongType },
                    navArgument("address") { type = NavType.StringType },
                ),
            ) { entry ->
                ConversationScreen(
                    threadId = entry.arguments?.getLong("threadId") ?: 0L,
                    fallbackAddress = entry.arguments?.getString("address").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    prefs = prefs,
                    setupController = container.setupController,
                    onAddKeyword = { container.preferencesRepository.addKeyword(it) },
                    onUpdateKeyword = { id, value -> container.preferencesRepository.updateKeyword(id, value) },
                    onToggleKeyword = { id, enabled -> container.preferencesRepository.toggleKeyword(id, enabled) },
                    onDeleteKeyword = { id -> container.preferencesRepository.deleteKeyword(id) },
                    onUpdateRingtone = { uri ->
                        container.preferencesRepository.updateAlertSettings { current ->
                            current.copy(ringtoneUri = uri)
                        }
                    },
                    onToggleVibration = { enabled ->
                        container.preferencesRepository.updateAlertSettings { current ->
                            current.copy(vibrationEnabled = enabled)
                        }
                    },
                    onToggleAlarmStyle = { enabled ->
                        container.preferencesRepository.updateAlertSettings { current ->
                            current.copy(alarmStyleEnabled = enabled)
                        }
                    },
                    onMarkSetupCompleted = {
                        container.setupController.confirmSetupCompleted()
                    },
                )
            }
            composable(ROUTE_GUIDE) {
                HyperOsGuideScreen(
                    setupController = container.setupController,
                    setupCompleted = prefs.appState.setupCompleted,
                    onMarkSetupCompleted = {
                        container.setupController.confirmSetupCompleted()
                    },
                )
            }
            composable(
                route = ROUTE_COMPOSER,
                arguments = listOf(
                    navArgument("address") { type = NavType.StringType },
                    navArgument("body") { type = NavType.StringType },
                ),
            ) { entry ->
                ComposerScreen(
                    initialAddress = entry.arguments?.getString("address").orEmpty(),
                    initialBody = entry.arguments?.getString("body").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun SetupCheckingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在检查默认短信状态…")
        }
    }
}

@Composable
private fun SetupBlockingScreen(
    setupController: SetupController,
    prefs: AppPreferences,
    defaultSmsStatus: SetupController.DefaultSmsStatus?,
    onConfirmSetup: suspend () -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        scope.launch {
            isChecking = true
            val isDefault = onConfirmSetup()
            statusMessage = if (isDefault) {
                "默认短信设置已生效，正在进入短信主页。"
            } else {
                "系统还没有把本应用设为默认短信 App，请先完成这一步。"
            }
            isChecking = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFFFFFF), Color(0xFFF8FAFC)),
                    ),
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "先把它设为默认短信应用",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "否则系统不会稳定把来信投递给本应用，你要的“别漏掉抄牌短信”目标就达不到。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("当前关键词", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    prefs.keywords.forEach { keyword ->
                        Text("• ${keyword.value}")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("系统检测结果", fontWeight = FontWeight.SemiBold)
                    Text("ROLE_SMS: ${defaultSmsStatus?.roleHeld?.let { if (it) "已持有" else "未持有" } ?: "检查中"}")
                    Text("默认短信包名匹配: ${defaultSmsStatus?.packageMatch?.let { if (it) "是" else "否" } ?: "检查中"}")
                    Text("系统返回默认包名: ${defaultSmsStatus?.currentDefaultPackage ?: "空"}")
                    Text("本应用包名: ${context.packageName}")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    defaultSmsLauncher.launch(setupController.createDefaultSmsRoleIntent())
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("请求设为默认短信 App")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    context.startActivitySafely(setupController.createAutostartIntent())
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("打开小米自启动设置")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    context.startActivitySafely(setupController.createBatterySettingsIntent())
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("打开电池优化设置")
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                enabled = !isChecking,
                onClick = {
                    scope.launch {
                        isChecking = true
                        val isDefault = onConfirmSetup()
                        statusMessage = if (isDefault) {
                            "设置已记录，正在重新检查。"
                        } else {
                            "设置已记录，但系统还没把本应用设为默认短信 App。请先点上面的按钮完成默认短信设置。"
                        }
                        isChecking = false
                    }
                },
            ) {
                if (isChecking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("检查中…")
                    }
                } else {
                    Text(
                        if (prefs.appState.setupCompleted) "重新检查系统设置" else "我已完成系统设置",
                    )
                }
            }
            statusMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InboxScreen(
    conversations: List<ConversationSummary>,
    onOpenConversation: (ConversationSummary) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("短信会话") })
        },
    ) { innerPadding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有短信。收到短信后，这里会自动显示。")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(conversations, key = { it.threadId }) { summary ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenConversation(summary) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = summary.address,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                if (summary.unreadCount > 0) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("${summary.unreadCount} 未读") },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = summary.snippet,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatTimestamp(summary.timestampMillis),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationScreen(
    threadId: Long,
    fallbackAddress: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val messages by container.smsRepository.observeMessages(threadId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val listState = rememberLazyListState()
    var hasJumpedToLatest by remember(threadId) { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val address = messages.firstOrNull()?.address?.ifBlank { fallbackAddress } ?: fallbackAddress
    val unreadIncomingCount = messages.count { !it.read && !it.isOutgoing }

    LaunchedEffect(unreadIncomingCount, threadId) {
        if (unreadIncomingCount > 0) {
            container.smsRepository.markThreadAsRead(threadId)
        }
    }

    LaunchedEffect(threadId, messages.lastOrNull()?.id, hasJumpedToLatest) {
        if (messages.isNotEmpty() && !hasJumpedToLatest) {
            listState.scrollToItem(messages.lastIndex)
            hasJumpedToLatest = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(address.ifBlank { "短信详情" }) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入短信内容") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (draft.isNotBlank() && address.isNotBlank()) {
                                scope.launch {
                                    container.smsRepository.sendMessage(address, draft)
                                    draft = ""
                                }
                            }
                        },
                    ),
                    maxLines = 4,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (draft.isNotBlank() && address.isNotBlank()) {
                            scope.launch {
                                container.smsRepository.sendMessage(address, draft)
                                draft = ""
                            }
                        }
                    },
                ) {
                    Text("发送")
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SmsMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (message.isOutgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.body.ifBlank { "(空短信)" },
                    color = if (message.isOutgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatTimestamp(message.timestampMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isOutgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ComposerScreen(
    initialAddress: String,
    initialBody: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var address by rememberSaveable { mutableStateOf(initialAddress) }
    var body by rememberSaveable { mutableStateOf(initialBody) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建短信") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("收件号码") },
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("短信内容") },
            )
            Button(
                onClick = {
                    if (address.isNotBlank() && body.isNotBlank()) {
                        scope.launch {
                            context.appContainer.smsRepository.sendMessage(address, body)
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("发送短信")
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    prefs: AppPreferences,
    setupController: SetupController,
    onAddKeyword: suspend (String) -> Unit,
    onUpdateKeyword: suspend (String, String) -> Unit,
    onToggleKeyword: suspend (String, Boolean) -> Unit,
    onDeleteKeyword: suspend (String) -> Unit,
    onUpdateRingtone: suspend (String?) -> Unit,
    onToggleVibration: suspend (Boolean) -> Unit,
    onToggleAlarmStyle: suspend (Boolean) -> Unit,
    onMarkSetupCompleted: suspend () -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var newKeyword by rememberSaveable { mutableStateOf("") }
    var editingKeyword by remember { mutableStateOf<KeywordEntry?>(null) }
    var completionMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val picked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        scope.launch {
            onUpdateRingtone(picked?.toString())
        }
    }
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    if (editingKeyword != null) {
        AlertDialog(
            onDismissRequest = { editingKeyword = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = editingKeyword ?: return@TextButton
                        scope.launch {
                            onUpdateKeyword(current.id, current.value)
                        }
                        editingKeyword = null
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingKeyword = null }) {
                    Text("取消")
                }
            },
            title = { Text("编辑关键字") },
            text = {
                OutlinedTextField(
                    value = editingKeyword?.value.orEmpty(),
                    onValueChange = { value ->
                        editingKeyword = editingKeyword?.copy(value = value)
                    },
                    label = { Text("关键字") },
                )
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关键字列表", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    prefs.keywords.forEach { keyword ->
                        KeywordRow(
                            keyword = keyword,
                            onToggle = { enabled ->
                                scope.launch { onToggleKeyword(keyword.id, enabled) }
                            },
                            onEdit = { editingKeyword = keyword },
                            onDelete = {
                                scope.launch { onDeleteKeyword(keyword.id) }
                            },
                        )
                        HorizontalDivider()
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newKeyword,
                            onValueChange = { newKeyword = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("新增关键字") },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newKeyword.isNotBlank()) {
                                    scope.launch {
                                        onAddKeyword(newKeyword)
                                        newKeyword = ""
                                    }
                                }
                            },
                        ) {
                            Text("添加")
                        }
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("提醒方式", style = MaterialTheme.typography.titleMedium)
                    Text("自动停止时间固定为 10 分钟。")
                    SettingsSwitchRow(
                        title = "启用震动",
                        checked = prefs.alertSettings.vibrationEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { onToggleVibration(enabled) }
                        },
                    )
                    SettingsSwitchRow(
                        title = "闹钟式提醒增强",
                        checked = prefs.alertSettings.alarmStyleEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { onToggleAlarmStyle(enabled) }
                        },
                    )
                    OutlinedButton(
                        onClick = {
                            ringtoneLauncher.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_TYPE,
                                        RingtoneManager.TYPE_ALARM,
                                    )
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        prefs.alertSettings.ringtoneUri?.let(Uri::parse),
                                    )
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "选择铃声：${
                                if (prefs.alertSettings.ringtoneUri.isNullOrBlank()) "系统默认/内置兜底"
                                else "已自定义"
                            }",
                        )
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("系统权限", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationsPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivitySafely(setupController.createAppDetailsIntent())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("授予通知权限")
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivitySafely(setupController.createNotificationPolicyIntent())
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (setupController.hasNotificationPolicyAccess()) "勿扰访问已授予"
                            else "开启勿扰访问（最佳努力）",
                        )
                    }
                    TextButton(onClick = {
                        scope.launch {
                            val isDefault = onMarkSetupCompleted()
                            completionMessage = if (isDefault) {
                                "已记录权限设置。"
                            } else {
                                "已记录，但当前系统默认短信应用已不是本应用，请重新设置。"
                            }
                        }
                    }) {
                        Text(if (prefs.appState.setupCompleted) "已记录权限设置" else "我已完成权限设置")
                    }
                    completionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun KeywordRow(
    keyword: KeywordEntry,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(keyword.value, style = MaterialTheme.typography.bodyLarge)
            Text(if (keyword.enabled) "已启用" else "已停用", style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = keyword.enabled, onCheckedChange = onToggle)
        TextButton(onClick = onEdit) { Text("编辑") }
        TextButton(onClick = onDelete) { Text("删除") }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HyperOsGuideScreen(
    setupController: SetupController,
    setupCompleted: Boolean,
    onMarkSetupCompleted: suspend () -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("HyperOS 指南") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GuideCard(
                title = "1. 设为默认短信 App",
                body = "这是可靠接收 `SMS_DELIVER_ACTION` 的前提。没有这一步，就不该相信这个应用能稳稳拦住抄牌短信。",
                action = {
                    context.startActivitySafely(setupController.createDefaultSmsRoleIntent())
                },
                buttonLabel = "打开默认短信设置",
            )
            GuideCard(
                title = "2. 打开自启动",
                body = "路径通常在 手机管家/安全中心 > 授权管理 > 自启动管理。K70 至尊版上这一步很重要。",
                action = {
                    context.startActivitySafely(setupController.createAutostartIntent())
                },
                buttonLabel = "打开自启动页",
            )
            GuideCard(
                title = "3. 关闭电池限制",
                body = "把本应用设为“不限制”或“无限制”，否则系统在某些场景会压缩后台行为。",
                action = {
                    context.startActivitySafely(setupController.createBatterySettingsIntent())
                },
                buttonLabel = if (setupController.isIgnoringBatteryOptimizations()) "已接近完成" else "打开电池设置",
            )
            GuideCard(
                title = "4. 允许通知和勿扰访问",
                body = "这样命中车牌短信时，提醒更容易在锁屏和静音场景下把你叫出来。",
                action = {
                    context.startActivitySafely(setupController.createNotificationPolicyIntent())
                },
                buttonLabel = "打开勿扰访问",
            )
            Button(
                enabled = !isSubmitting,
                onClick = {
                    scope.launch {
                        isSubmitting = true
                        val isDefault = onMarkSetupCompleted()
                        statusMessage = if (isDefault) {
                            "已记录并重新检查系统设置。"
                        } else {
                            "已记录，但当前系统默认短信应用已不是本应用，请重新设置。"
                        }
                        isSubmitting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        isSubmitting -> "检查中…"
                        setupCompleted -> "重新检查以上设置"
                        else -> "我已完成以上设置"
                    },
                )
            }
            statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun GuideCard(
    title: String,
    body: String,
    action: () -> Unit,
    buttonLabel: String,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = action) {
                Text(buttonLabel)
            }
        }
    }
}

private fun Context.startActivitySafely(intent: Intent) {
    runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.getOrElse {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun formatTimestamp(timestampMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestampMillis))
}
