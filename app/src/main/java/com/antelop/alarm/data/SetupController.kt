package com.antelop.alarm.data

import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.provider.Telephony

class SetupController(
    private val context: Context,
    private val preferencesRepository: AppPreferencesRepository,
) {
    data class DefaultSmsStatus(
        val isDefault: Boolean,
        val packageMatch: Boolean,
        val roleHeld: Boolean,
        val currentDefaultPackage: String?,
    )

    fun getDefaultSmsStatus(): DefaultSmsStatus {
        val currentDefaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        val packageMatch = currentDefaultPackage == context.packageName
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            false
        }
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleHeld || packageMatch
        } else {
            packageMatch
        }
        return DefaultSmsStatus(
            isDefault = isDefault,
            packageMatch = packageMatch,
            roleHeld = roleHeld,
            currentDefaultPackage = currentDefaultPackage,
        )
    }

    fun isDefaultSmsApp(): Boolean {
        return getDefaultSmsStatus().isDefault
    }

    private suspend fun syncDefaultSmsState(isDefault: Boolean) {
        preferencesRepository.setDefaultSmsApp(isDefault)
    }

    suspend fun refreshAppState(): DefaultSmsStatus {
        val status = getDefaultSmsStatus()
        syncDefaultSmsState(status.isDefault)
        return status
    }

    suspend fun confirmSetupCompleted(): Boolean {
        val status = getDefaultSmsStatus()
        syncDefaultSmsState(status.isDefault)
        preferencesRepository.setSetupCompleted(true)
        return status.isDefault
    }

    @Deprecated("Use getDefaultSmsStatus", ReplaceWith("getDefaultSmsStatus().isDefault"))
    fun legacyIsDefaultSmsApp(): Boolean {
        val packageMatch = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            val roleHeld = roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
            return roleHeld || packageMatch
        }
        return packageMatch
    }

    fun createDefaultSmsRoleIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }

    fun createAppDetailsIntent(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    }

    fun createBatterySettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    fun createAutostartIntent(): Intent {
        return Intent().apply {
            component = android.content.ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createNotificationPolicyIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    fun hasNotificationPolicyAccess(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.isNotificationPolicyAccessGranted
    }

    fun canUseFullScreenIntent(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager.canUseFullScreenIntent()
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
