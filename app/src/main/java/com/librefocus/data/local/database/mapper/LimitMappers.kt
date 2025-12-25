package com.librefocus.data.local.database.mapper

import com.librefocus.data.local.database.converter.RoomTypeConverters
import com.librefocus.data.local.database.dao.CompleteLimitData
import com.librefocus.data.local.database.entity.LaunchCountEntity
import com.librefocus.data.local.database.entity.LimitEntity
import com.librefocus.data.local.database.entity.ScheduleLimitEntity
import com.librefocus.data.local.database.entity.UsageLimitEntity
import com.librefocus.models.DayOfWeek
import com.librefocus.models.Limit
import com.librefocus.models.ResetPeriod
import com.librefocus.models.TimeSlot
import com.librefocus.models.UsageLimitType
import java.util.UUID

/**
 * Mapper functions to convert between domain models (Limit sealed class)
 * and database entities (LimitEntity + type-specific entities).
 */

// ========== Domain to Entity Mapping ==========

/**
 * Converts a Limit domain model to a set of entities for database storage.
 * Returns a triple of (LimitEntity, ScheduleLimitEntity?, UsageLimitEntity?, LaunchCountEntity?).
 */
fun Limit.toEntities(): LimitEntities {
    val limitEntity = LimitEntity(
        id = id,
        name = name,
        isEnabled = isEnabled,
        limitType = when (this) {
            is Limit.Schedule -> "SCHEDULE"
            is Limit.UsageLimit -> "USAGE_LIMIT"
            is Limit.LaunchCount -> "LAUNCH_COUNT"
        },
        selectedAppPackages = RoomTypeConverters.fromStringList(selectedAppPackages),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    return when (this) {
        is Limit.Schedule -> LimitEntities(
            limit = limitEntity,
            scheduleLimit = ScheduleLimitEntity(
                limitId = id,
                isAllDay = isAllDay,
                timeSlots = RoomTypeConverters.fromTimeSlotList(timeSlots),
                selectedDays = RoomTypeConverters.fromDayOfWeekSet(selectedDays)
            )
        )
        
        is Limit.UsageLimit -> LimitEntities(
            limit = limitEntity,
            usageLimit = UsageLimitEntity(
                limitId = id,
                limitType = limitType.name,
                durationMinutes = durationMinutes,
                selectedDays = RoomTypeConverters.fromDayOfWeekSet(selectedDays)
            )
        )
        
        is Limit.LaunchCount -> LimitEntities(
            limit = limitEntity,
            launchCountLimit = LaunchCountEntity(
                limitId = id,
                maxLaunches = maxLaunches,
                resetPeriod = resetPeriod.name,
                selectedDays = RoomTypeConverters.fromDayOfWeekSet(selectedDays)
            )
        )
    }
}

// ========== Entity to Domain Mapping ==========

/**
 * Converts CompleteLimitData (entities from database) to a Limit domain model.
 * Returns null if the data is inconsistent (e.g., limitType doesn't match available data).
 */
fun CompleteLimitData.toDomainModel(): Limit? {
    val selectedAppPackages = RoomTypeConverters.toStringList(limit.selectedAppPackages)
    
    return when (limit.limitType) {
        "SCHEDULE" -> {
            val scheduleLimitData = scheduleLimit ?: return null
            Limit.Schedule(
                id = limit.id,
                name = limit.name,
                isEnabled = limit.isEnabled,
                selectedAppPackages = selectedAppPackages,
                createdAt = limit.createdAt,
                updatedAt = limit.updatedAt,
                isAllDay = scheduleLimitData.isAllDay,
                timeSlots = RoomTypeConverters.toTimeSlotList(scheduleLimitData.timeSlots),
                selectedDays = RoomTypeConverters.toDayOfWeekSet(scheduleLimitData.selectedDays)
            )
        }
        
        "USAGE_LIMIT" -> {
            val usageLimitData = usageLimit ?: return null
            Limit.UsageLimit(
                id = limit.id,
                name = limit.name,
                isEnabled = limit.isEnabled,
                selectedAppPackages = selectedAppPackages,
                createdAt = limit.createdAt,
                updatedAt = limit.updatedAt,
                limitType = try {
                    UsageLimitType.valueOf(usageLimitData.limitType)
                } catch (e: IllegalArgumentException) {
                    UsageLimitType.DAILY // Default fallback
                },
                durationMinutes = usageLimitData.durationMinutes,
                selectedDays = RoomTypeConverters.toDayOfWeekSet(usageLimitData.selectedDays)
            )
        }
        
        "LAUNCH_COUNT" -> {
            val launchCountLimitData = launchCountLimit ?: return null
            Limit.LaunchCount(
                id = limit.id,
                name = limit.name,
                isEnabled = limit.isEnabled,
                selectedAppPackages = selectedAppPackages,
                createdAt = limit.createdAt,
                updatedAt = limit.updatedAt,
                maxLaunches = launchCountLimitData.maxLaunches,
                resetPeriod = try {
                    ResetPeriod.valueOf(launchCountLimitData.resetPeriod)
                } catch (e: IllegalArgumentException) {
                    ResetPeriod.DAILY // Default fallback
                },
                selectedDays = RoomTypeConverters.toDayOfWeekSet(launchCountLimitData.selectedDays)
            )
        }
        
        else -> null // Unknown limit type
    }
}

// ========== Helper Data Classes ==========

/**
 * Container for all entity types related to a limit.
 * Only one of the type-specific entities should be non-null.
 */
data class LimitEntities(
    val limit: LimitEntity,
    val scheduleLimit: ScheduleLimitEntity? = null,
    val usageLimit: UsageLimitEntity? = null,
    val launchCountLimit: LaunchCountEntity? = null
)

// ========== Factory Functions ==========

/**
 * Creates a new Schedule limit with a generated ID and current timestamps.
 */
fun createScheduleLimit(
    name: String,
    isEnabled: Boolean = true,
    selectedAppPackages: List<String>,
    isAllDay: Boolean,
    timeSlots: List<TimeSlot>,
    selectedDays: Set<DayOfWeek>
): Limit.Schedule {
    val now = System.currentTimeMillis()
    return Limit.Schedule(
        id = UUID.randomUUID().toString(),
        name = name,
        isEnabled = isEnabled,
        selectedAppPackages = selectedAppPackages,
        createdAt = now,
        updatedAt = now,
        isAllDay = isAllDay,
        timeSlots = timeSlots,
        selectedDays = selectedDays
    )
}

/**
 * Creates a new Usage limit with a generated ID and current timestamps.
 */
fun createUsageLimit(
    name: String,
    isEnabled: Boolean = true,
    selectedAppPackages: List<String>,
    limitType: UsageLimitType,
    durationMinutes: Int,
    selectedDays: Set<DayOfWeek>
): Limit.UsageLimit {
    val now = System.currentTimeMillis()
    return Limit.UsageLimit(
        id = UUID.randomUUID().toString(),
        name = name,
        isEnabled = isEnabled,
        selectedAppPackages = selectedAppPackages,
        createdAt = now,
        updatedAt = now,
        limitType = limitType,
        durationMinutes = durationMinutes,
        selectedDays = selectedDays
    )
}

/**
 * Creates a new Launch Count limit with a generated ID and current timestamps.
 */
fun createLaunchCountLimit(
    name: String,
    isEnabled: Boolean = true,
    selectedAppPackages: List<String>,
    maxLaunches: Int,
    resetPeriod: ResetPeriod,
    selectedDays: Set<DayOfWeek>
): Limit.LaunchCount {
    val now = System.currentTimeMillis()
    return Limit.LaunchCount(
        id = UUID.randomUUID().toString(),
        name = name,
        isEnabled = isEnabled,
        selectedAppPackages = selectedAppPackages,
        createdAt = now,
        updatedAt = now,
        maxLaunches = maxLaunches,
        resetPeriod = resetPeriod,
        selectedDays = selectedDays
    )
}

/**
 * Creates an updated copy of a Limit with a new updatedAt timestamp.
 */
fun Limit.withUpdatedTimestamp(): Limit {
    val now = System.currentTimeMillis()
    return when (this) {
        is Limit.Schedule -> copy(updatedAt = now)
        is Limit.UsageLimit -> copy(updatedAt = now)
        is Limit.LaunchCount -> copy(updatedAt = now)
    }
}
