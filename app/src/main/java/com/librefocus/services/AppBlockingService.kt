package com.librefocus.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.librefocus.data.local.AppInfoProvider
import com.librefocus.data.local.UsageStatsProvider
import com.librefocus.data.repository.LimitRepository
import com.librefocus.models.DayOfWeek
import com.librefocus.models.Limit
import com.librefocus.models.UsageLimitType
import com.librefocus.utils.roundToDayStart
import com.librefocus.utils.roundToHourStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class AppBlockingService : AccessibilityService(), KoinComponent {

    private val limitRepository: LimitRepository by inject()
    private val usageStatsProvider: UsageStatsProvider by inject()
    private val appInfoProvider: AppInfoProvider by inject()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var currentForegroundPackage: String? = null
    private var activeLimits: List<Limit> = emptyList()

    private var overlayManager: BlockingOverlayManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        overlayManager = BlockingOverlayManager(this) {
            // User clicked Go Home
            performGlobalAction(GLOBAL_ACTION_HOME)
            overlayManager?.hide()
        }

        // Collect available limits continuously
        serviceScope.launch {
            limitRepository.getEnabledLimits().collectLatest { limits ->
                activeLimits = limits
                checkCurrentAppOverLimit()
            }
        }

        // Periodic check in case an app runs out of usage allocation while in the foreground
        serviceScope.launch {
            while (isActive) {
                checkCurrentAppOverLimit()
                delay(5000L) // Check every 5 seconds
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            val className = event.className?.toString()

            if (packageName != null) {
                // Ignore System UI (notifications, volume panel, etc) so overlay stays active
                if (packageName == "com.android.systemui" || packageName == "android") {
                    return
                }

                if (packageName == this.packageName) {
                    // If the user actually launched the main app, process it (which hides the overlay).
                    // If it's just the overlay being drawn (e.g., FrameLayout or ComposeView), ignore it.
                    if (className?.contains("MainActivity") == true) {
                        currentForegroundPackage = packageName
                        serviceScope.launch { checkCurrentAppOverLimit() }
                    }
                    return
                }
                
                // Set the current open app
                currentForegroundPackage = packageName
                serviceScope.launch {
                    checkCurrentAppOverLimit()
                }
            }
        }
    }

    private suspend fun checkCurrentAppOverLimit() {
        val packageName = currentForegroundPackage ?: return

        // Wait to process on background thread
        withContext(Dispatchers.IO) {
            // Check if package is targeted by any active limit
            val matchingLimits = activeLimits.filter { limit ->
                limit.selectedAppPackages.contains(packageName)
            }

            if (matchingLimits.isEmpty()) {
                withContext(Dispatchers.Main) { overlayManager?.hide() }
                return@withContext
            }

            for (limit in matchingLimits) {
                when (limit) {
                    is Limit.Schedule -> {
                        if (isScheduleBlocked(limit)) {
                            showBlockedOverlay(packageName, limit)
                            return@withContext
                        }
                    }
                    is Limit.UsageLimit -> {
                        if (isUsageBlocked(packageName, limit)) {
                            showBlockedOverlay(packageName, limit)
                            return@withContext
                        }
                    }
                    else -> {
                        // LaunchCount not implemented here per requirements
                    }
                }
            }

            // Not blocked
            withContext(Dispatchers.Main) {
                overlayManager?.hide()
            }
        }
    }

    private fun isScheduleBlocked(limit: Limit.Schedule): Boolean {
        val calendar = Calendar.getInstance()
        val currentDay = mapCalendarDayToDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
        
        if (!limit.selectedDays.contains(currentDay)) return false

        if (limit.isAllDay) return true

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        for (slot in limit.timeSlots) {
            val fromTimeInMinutes = slot.fromHour
            val toTimeInMinutes = slot.toHour

            if (fromTimeInMinutes <= toTimeInMinutes) {
                // Same day range (e.g. 09:00 -> 17:00)
                if (currentTimeInMinutes in fromTimeInMinutes until toTimeInMinutes) {
                    return true
                }
            } else {
                // Overnight range: starts today, ends tomorrow (e.g. 22:00 -> 06:00)
                if (currentTimeInMinutes >= fromTimeInMinutes || currentTimeInMinutes < toTimeInMinutes) {
                    return true
                }
            }
        }
        return false
    }

    private fun isUsageBlocked(packageName: String, limit: Limit.UsageLimit): Boolean {
        val calendar = Calendar.getInstance()
        val currentDay = mapCalendarDayToDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
        
        if (!limit.selectedDays.contains(currentDay)) return false

        val currentTimeMillis = System.currentTimeMillis()
        val startTimeMillis = when (limit.limitType) {
            UsageLimitType.DAILY -> roundToDayStart(currentTimeMillis)
            UsageLimitType.HOURLY -> roundToHourStart(currentTimeMillis)
        }

        // Get actual usage since the start time boundary
        val usageMillis = usageStatsProvider.getAppUsageSince(
            packageName,
            startTimeMillis,
            currentTimeMillis
        )

        val durationMillis = limit.durationMinutes * 60 * 1000L
        return usageMillis >= durationMillis
    }

    private suspend fun showBlockedOverlay(packageName: String, limit: Limit) {
        val appName = appInfoProvider.getAppName(packageName)
        val limitName = limit.name

        // Determine message and schedule
        val unblockMessage = when (limit) {
            is Limit.Schedule -> {
                if (limit.isAllDay) {
                    "Blocked all day by '${limit.name}'"
                } else {
                    "Currently inside a blocked time slot by '${limit.name}'"
                }
            }
            is Limit.UsageLimit -> {
                val type = if (limit.limitType == UsageLimitType.DAILY) "Daily" else "Hourly"
                "You reached your $type limit of ${limit.durationMinutes} minutes.\n(${limit.name})"
            }
            else -> "Blocked by limit"
        }

        val scheduleInfo = "Blocked until condition resolves."

        withContext(Dispatchers.Main) {
            overlayManager?.show(
                blockedAppName = appName,
                unblockTimeMessage = unblockMessage,
                scheduleInformation = scheduleInfo
            )
        }
    }

    private fun mapCalendarDayToDayOfWeek(calendarDay: Int): DayOfWeek {
        return when (calendarDay) {
            Calendar.MONDAY -> DayOfWeek.MON
            Calendar.TUESDAY -> DayOfWeek.TUE
            Calendar.WEDNESDAY -> DayOfWeek.WED
            Calendar.THURSDAY -> DayOfWeek.THU
            Calendar.FRIDAY -> DayOfWeek.FRI
            Calendar.SATURDAY -> DayOfWeek.SAT
            Calendar.SUNDAY -> DayOfWeek.SUN
            else -> DayOfWeek.MON
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        serviceScope.cancel()
        overlayManager?.hide()
        super.onDestroy()
    }
}
