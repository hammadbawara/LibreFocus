# LibreFocus

You are an expert Android developer and AI assistant specialized in building **LibreFocus**, a native Android app developed in **Kotlin** using **Jetpack Compose** and **MVVM (Model-View-ViewModel) architecture**. Your primary role is to assist in generating clean, maintainable, and testable code.

---

## 🎯 Project Overview
LibreFocus helps users **reduce screen time** and promote **healthier digital habits** through visualization, behavioral nudges, gamification, and AI-driven personalization.

---

## 📦 Core Dependencies

### Dependency Injection
- **Koin** - Lightweight DI framework
- Define modules in `di/` folder using `module { }`
- Use `viewModel { }` for ViewModels, `single { }` for singletons

### Networking
- **Ktor** - HTTP client for remote operations
- Place clients/services in `data/remote/`
- Use coroutine-based suspend functions

### Local Persistence
- **DataStore** (currently in use) - For app preferences
- **Room** - For usage tracking, categories, gamification data

---

## 🏗️ Current Project Structure

```
com/librefocus/
├── LibreFocus.kt                    # Application class with Koin initialization
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── UsageDatabase.kt     # Room database instance
│   │   │   ├── dao/
│   │   │   │   ├── AppDao.kt
│   │   │   │   ├── AppCategoryDao.kt
│   │   │   │   ├── HourlyAppUsageDao.kt
│   │   │   │   ├── DailyDeviceUsageDao.kt
│   │   │   │   └── SyncMetadataDao.kt
│   │   │   ├── entity/
│   │   │   │   ├── AppEntity.kt
│   │   │   │   ├── AppCategoryEntity.kt
│   │   │   │   ├── HourlyAppUsageEntity.kt
│   │   │   │   ├── DailyDeviceUsageEntity.kt
│   │   │   │   └── SyncMetadataEntity.kt
│   │   │   └── converter/           # Type converters for Room
│   │   ├── PreferencesDataStore.kt  # DataStore wrapper
│   │   └── UsageStatsProvider.kt    # System UsageStatsManager wrapper
│   └── repository/
│       ├── PreferencesRepository.kt
│       ├── UsageTrackingRepository.kt
│       ├── BackupRestoreRepository.kt  # Backup/restore operations
│       ├── CategoryRepository.kt
│       ├── LimitRepository.kt
│       └── AppRepository.kt
├── di/
│   ├── AppModule.kt                 # ViewModels DI module
│   ├── DatastoreModule.kt           # DataStore & DateTimeFormatterManager DI
│   ├── DatabaseModule.kt            # Room database DI module
│   ├── ChatbotModule.kt             # AI Chatbot DI module
│   └── WorkerModule.kt              # WorkManager DI module
├── models/
│   ├── AppUsage.kt
│   ├── AppUsageData.kt
│   ├── AppUsageAverages.kt
│   ├── HourlyUsageData.kt
│   ├── UsageValuePoint.kt
│   ├── UsageEventData.kt
│   ├── BackupData.kt                # Backup data models
│   └── DateTimePreferences.kt       # Date/Time settings model
├── utils/
│   ├── TimeUtils.kt
│   ├── DateTimeFormatterManager.kt  # Centralized date/time formatting
│   └── UsageSyncScheduler.kt        # WorkManager scheduling helper
├── workers/
│   └── UsageSyncWorker.kt           # Background usage sync worker
├── services/
│   └── UsageMonitoringService.kt    # Foreground service for monitoring
└── ui/
    ├── MainActivity.kt
    ├── MainViewModel.kt
    ├── common/
    │   └── AppScaffold.kt           # Common scaffold for consistent UI behavior
    ├── navigation/
    │   ├── NavGraph.kt              # Main navigation graph
    │   └── Screen.kt                # Screen routes
    ├── home/
    │   ├── HomeScreen.kt
    │   └── HomeViewModel.kt
    ├── onboarding/
    │   ├── OnBoardingNavGraph.kt
    │   ├── AppIntroScreen.kt
    │   ├── PermissionScreen.kt
    │   └── OnboardingViewModel.kt
    ├── stats/
    │   ├── StatsScreen.kt
    │   ├── StatsViewModel.kt
    │   ├── StatsUiState.kt
    │   ├── StatsChart.kt
    │   ├── StatsComponents.kt
    │   ├── StatsSelectors.kt
    │   └── StatsUtils.kt
    ├── settings/
    │   ├── SettingsScreen.kt        # Settings UI with date/time config
    │   └── SettingsViewModel.kt     # Settings state management
    ├── chatbot/
    │   ├── ChatbotActivity.kt       # AI chatbot screen
    │   ├── ChatViewModel.kt         # Chat logic with Groq API
    │   └── ChatContextProvider.kt   # Provides usage context to chatbot
    ├── components/
    │   └── FloatingChatButton.kt    # Global FAB for chatbot access
    ├── limits/
    │   └── LimitsScreen.kt          # App limits UI (placeholder)
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## ✅ Progress Tracker

### Completed Features
- [x] Basic project structure with MVVM architecture
- [x] Koin dependency injection setup
- [x] DataStore for preferences management
- [x] Onboarding flow (intro + permissions)
- [x] Home screen with usage sync
- [x] Navigation graphs for Home and Onboarding
- [x] Material 3 theming (colors, typography, dynamic theme support)
- [x] Basic app usage data models
- [x] Usage and Preferences repositories
- [x] **Room database implementation**
  - [x] App usage history tracking (hourly aggregation)
  - [x] App entities and categories
  - [x] Daily device usage (unlock tracking)
  - [x] Sync metadata for incremental updates
  - [x] Complete DAO layer with Flow support
- [x] **Usage tracking infrastructure**
  - [x] UsageStatsProvider for system stats access
  - [x] UsageTrackingRepository with hourly aggregation
  - [x] WorkManager for periodic background sync (UsageSyncWorker)
  - [x] Session duration calculation
  - [x] Launch count tracking
  - [x] Unlock count tracking
  - [x] UsageSyncScheduler for scheduling management
- [x] **Stats/Analytics Screen**
  - [x] Daily/weekly/monthly usage charts
  - [x] Metric selection (usage time, launches, unlocks)
  - [x] Time range selector with custom range picker
  - [x] Period navigation (previous/next)
  - [x] Top apps list
  - [x] Summary statistics (total, average)
  - [x] Chart with formatted axis labels
- [x] **Date & Time Settings System**
  - [x] User-configurable date/time preferences
  - [x] System defaults toggle
  - [x] Time format selection (System/12H/24H)
  - [x] Date format selection (System/DD-MMM-YYYY/MM-DD-YYYY/YYYY-MM-DD)
  - [x] Time zone selection
  - [x] Live preview of date/time formatting
  - [x] Centralized DateTimeFormatterManager
  - [x] Reactive formatting with cached formatters
  - [x] App-wide UI refactored to use centralized formatting
- [x] **Backup & Restore System**
  - [x] Complete database backup to compressed ZIP files
  - [x] Backup includes all 9 database tables (categories, apps, usage, limits, etc.)
  - [x] Compact JSON format to minimize file size
  - [x] Export via system document picker
  - [x] Two-step confirmation for data restoration
  - [x] Complete data replacement strategy (delete all → import new)
  - [x] Reset all database data with two-step verification
  - [x] Integrated into Settings screen
- [x] **AI Chatbot Assistant**
  - [x] Context-aware AI chatbot using Groq API
  - [x] Real-time usage data integration (screen time, top apps)
  - [x] Llama 3.3 70B model for intelligent responses
  - [x] Floating action button for quick access
  - [x] Full-screen chat interface with message history
  - [x] Provides personalized advice based on user behavior

### 🚧 In Progress / To Be Implemented

#### 1. Data Layer Enhancement
- [x] ~~Implement Room database for:~~
  - [x] ~~App usage history tracking~~
  - [x] ~~App categories (custom + predefined)~~
  - [x] ~~Gamification data (badges, streaks, points)~~
  - [x] ~~Focus sessions and blocking rules~~
- [x] ~~Create entities, DAOs, and database migrations~~
- [ ] Add Ktor client for remote sync (optional)

#### 2. Core Features
- [x] **Insights & Analytics**
  - [x] Daily/weekly/monthly usage charts
  - [x] Custom date range selection
  - [x] Multiple metric types (time, launches, unlocks)
  - [x] User-configurable date/time formatting
  - [ ] Usage trends and heatmaps
  - [ ] Category-based statistics
  
- [x] **App Categorization**
  - [x] Auto-categorization logic
  - [x] Custom category CRUD operations
  - [x] Category management UI
  - [ ] Category-based insights UI
  
- [ ] **Prevention Tools**
  - [ ] App blocking functionality
  - [ ] Grayscale mode implementation
  - [ ] Focus mode with timers
  - [ ] Wait time before app launch
  - [ ] Shorts/Reels time limits for YouTube/Instagram
  
- [ ] **Gamification**
  - [ ] Badge system
  - [ ] Streak tracking
  - [ ] Points and milestones
  - [ ] Weekly/monthly goals
  - [ ] Progress visualization
  
- [x] **AI & Motivation**
  - [x] AI chatbot assistant with context awareness
  - [x] Real-time behavior analysis
  - [x] Personalized advice and guidance
  - [ ] AI-driven automation (focus mode triggers)
  - [ ] Alternative app suggestions
  
- [x] **Security & Persistence**
  - [x] Complete backup & restore functionality
  - [x] Database reset with verification
  - [ ] Anti-uninstall protection

#### 3. UI/UX Improvements
- [x] ~~Complete insights/analytics screens~~
- [x] ~~Settings screen with theme and date/time configuration~~
- [x] Common AppScaffold component for UI consistency across screens
- [x] App categorization management UI
- [x] Data management UI (backup/restore/reset)
- [x] AI chatbot interface with floating action button
- [ ] Gamification dashboard
- [ ] Prevention tools settings
- [ ] Enhanced data visualization (heatmaps, trends)

#### 4. Background Services
- [x] ~~WorkManager for periodic usage tracking~~
- [x] ~~Foreground service for active monitoring (placeholder)~~
- [ ] Accessibility service for app blocking

---

## 📋 DO's and DON'Ts

### ✅ DO

#### Architecture & Code Organization
- **DO** follow MVVM pattern strictly (Model → Repository → ViewModel → UI)
- **DO** place models in `models/` (current structure) or `domain/model/`
- **DO** keep repositories in `data/repository/`
- **DO** organize UI by feature in `ui/[feature]/`
- **DO** use Koin for all dependency injection
- **DO** use DataStore for simple key-value preferences
- **DO** use Room for complex structured data (usage history, categories, gamification)
- **DO** isolate Android-specific APIs in provider/utility classes (e.g., AppInfoProvider)

#### Code Quality
- **DO** use Kotlin coroutines for async operations
- **DO** use StateFlow/SharedFlow for state management in ViewModels
- **DO** write composable functions with descriptive names
- **DO** add content descriptions for accessibility
- **DO** handle loading, success, and error states explicitly
- **DO** validate user inputs and handle edge cases

#### Dependency Management
- **DO** define Koin modules in `di/` folder
- **DO** inject dependencies via constructor: `class MyRepo(private val dataStore: PreferencesDataStore)`
- **DO** use `viewModel { }` for ViewModels in Koin modules
- **DO** use `single { }` for repositories, databases, and data sources

#### Data Layer
- **DO** use suspend functions for database/network operations
- **DO** use proper Dispatchers (IO for database/network, Main for UI)
- **DO** create separate DAOs for each entity in Room
- **DO** version Room database properly with migrations

---

### ❌ DON'T

#### Architecture & Code Organization
- **DON'T** use UseCases/Interactors layer (simplified MVVM for this project)
- **DON'T** put business logic in Composables
- **DON'T** access repositories directly from UI layer
- **DON'T** mix navigation logic with UI composables
- **DON'T** create god classes or ViewModels
- **DON'T** use Android-specific APIs directly in UI, ViewModel, or Repository layers

#### Dependencies & Libraries
- **DON'T** use Hilt/Dagger (use Koin instead)
- **DON'T** use Retrofit (use Ktor for networking)
- **DON'T** use SharedPreferences (use DataStore or Room)
- **DON'T** add unnecessary dependencies

#### Code Quality
- **DON'T** block main thread with heavy operations
- **DON'T** ignore coroutine exceptions (use proper error handling)
- **DON'T** hardcode strings (use string resources)
- **DON'T** hardcode colors or dimensions (use theme system)
- **DON'T** create memory leaks (avoid Activity/Fragment references in ViewModels)
- **DON'T** create custom DateTimeFormatter instances - **ALWAYS use DateTimeFormatterManager** for consistency across the app

#### Data Layer
- **DON'T** perform database operations on main thread
- **DON'T** expose mutable state from ViewModels (use immutable StateFlow)
- **DON'T** forget to handle null/empty states
- **DON'T** skip database migrations

---

## � Key Files & Components

### Core Application
- **LibreFocus.kt** - Application class with Koin initialization

### Data Layer

#### Local Data
- **PreferencesDataStore.kt** - DataStore wrapper for user preferences
  - Manages onboarding state, app theme, date/time preferences
  - Exposes Flow-based reactive preferences
  - Keys: USE_SYSTEM_DATETIME_KEY, DATE_FORMAT_KEY, TIME_ZONE_ID_KEY
  
- **UsageStatsProvider.kt** - System UsageStatsManager wrapper
  - Fetches app usage events from Android system
  - Calculates session duration and launch counts
  - Retrieves device unlock statistics

#### Database (Room)
- **UsageDatabase.kt** - Room database instance
- **DAOs**: AppDao, AppCategoryDao, HourlyAppUsageDao, DailyDeviceUsageDao, SyncMetadataDao
- **Entities**: AppEntity, AppCategoryEntity, HourlyAppUsageEntity, DailyDeviceUsageEntity, SyncMetadataEntity

#### Repositories
- **PreferencesRepository.kt** - Abstracts DataStore operations
  - Provides Flow<DateTimePreferences>, Flow<String> for theme
  - Methods: setUseSystemDateTime(), setDateFormat(), setTimeZoneId()
  
- **UsageTrackingRepository.kt** - Manages usage stats collection and storage
  - syncUsageStats(): Fetches and aggregates usage data hourly
  - Handles incremental sync using SyncMetadata
  - Calculates total usage time, launches, unlocks per app

- **BackupRestoreRepository.kt** - Handles backup, restore, and reset operations
  - createBackup(): Exports all 9 database tables to BackupData model
  - exportBackup(uri, data): Writes compressed ZIP file with compact JSON
  - importBackup(uri): Reads and parses backup ZIP file
  - restoreBackup(data): Deletes all data and imports backup (complete replacement)
  - resetAllData(): Clears all database tables

### Models
- **DateTimePreferences.kt** - Immutable data model for date/time settings
  - Properties: useSystemDefaults, timeFormat, dateFormat, timeZoneId
  - Enums: TimeFormat (SYSTEM/TWELVE_HOUR/TWENTY_FOUR_HOUR), DateFormat (SYSTEM/DD_MMM_YYYY/MM_DD_YYYY/YYYY_MM_DD)
  - Helper methods: getEffectiveTimeZoneId(), getEffectiveTimeFormat(), getEffectiveDateFormat()
  
- **AppUsage.kt, AppUsageData.kt, HourlyUsageData.kt** - Usage data models
- **UsageValuePoint.kt, UsageEventData.kt** - Chart and event data models
- **AppUsageAverages.kt** - Aggregated statistics model

### Utilities
- **DateTimeFormatterManager.kt** - **CRITICAL** Centralized date/time formatting manager
  - Purpose: Single source of truth for all date/time formatting across the app
  - Dependencies: Context (for AndroidDateFormat.is24HourFormat), Flow<DateTimePreferences>, Locale
  - Exposes: Flow<FormattedDateTimePreferences> with cached DateTimeFormatter instances
  - Key Features:
    - Creates formatters based on user preferences (or system defaults)
    - Caches formatters for performance
    - Provides convenient formatting methods via FormattedDateTimePreferences wrapper
  - Methods: formatTime(), formatDate(), formatDateTime(), formatShortDate(), formatDayLabel(), formatMonthLabel(), formatHour(), formatHourRange(), formatDateRange()
  - **ALWAYS** use this for any date/time display - no hardcoded patterns!
  
- **TimeUtils.kt** - Helper functions for time calculations
  - roundToHourStart(), roundToDayStart() for timestamp normalization
  
- **UsageSyncScheduler.kt** - WorkManager scheduling helper
  - schedulePeriodicSync(): Sets up background usage sync

### AI Chatbot
- **ChatViewModel.kt** - Manages chatbot conversation and API integration
  - Uses Groq API with Llama 3.3 70B model
  - Maintains message history with StateFlow
  - Handles JSON response parsing with fallback strategies
  - Default model: "llama-3.3-70b-versatile"

- **ChatContextProvider.kt** - Provides real-time user behavior context to chatbot
  - Fetches today's screen time from HourlyAppUsageDao
  - Retrieves top 3 apps and daily unlock count
  - Generates formatted context string for AI prompts
  - Interface: IChatContextProvider for testability

- **ChatbotActivity.kt / ChatbotScreen** - Full-screen chat interface
  - Message list with user/assistant distinction
  - Text input with send button
  - Navigation integration

- **FloatingChatButton.kt** - Global FAB for chatbot access
  - Appears on all main screens except chatbot itself
  - Quick access to AI assistant

### Background Processing
- **UsageSyncWorker.kt** - WorkManager worker for periodic usage sync
  - Runs every 15 minutes (configurable)
  - Calls UsageTrackingRepository.syncUsageStats()
  
- **UsageMonitoringService.kt** - Foreground service placeholder for real-time monitoring

### Dependency Injection (Koin)
- **AppModule.kt** - ViewModels registration
  - Registers: MainViewModel, HomeViewModel, OnboardingViewModel, StatsViewModel, SettingsViewModel
  
- **DatastoreModule.kt** - DataStore and related services
  - Provides: PreferencesDataStore, PreferencesRepository, DateTimeFormatterManager
  - **Important**: DateTimeFormatterManager registered as singleton with Context dependency
  
- **DatabaseModule.kt** - Room database and DAOs
  - Provides: UsageDatabase instance, all DAOs, BackupRestoreRepository
  
- **ChatbotModule.kt** - AI chatbot dependencies
  - Provides: IChatContextProvider implementation (ChatContextProvider)
  - Factory: ChatViewModel with injected context provider
  - Model configuration: llama-3.3-70b-versatile
  
- **WorkerModule.kt** - WorkManager dependencies
  - Provides: UsageSyncScheduler, UsageStatsProvider, UsageTrackingRepository

### UI Layer

#### Navigation
- **Screen.kt** - Sealed class defining all screen routes
- **NavGraph.kt** - Main navigation graph with bottom navigation
- **OnBoardingNavGraph.kt** - Onboarding flow navigation

#### Screens
- **HomeScreen.kt** - Today's usage overview with sync button
- **StatsScreen.kt** - Analytics with charts, metric/period selectors
  - Observes formattedPreferences StateFlow
  - Passes FormattedDateTimePreferences to chart components
  
- **SettingsScreen.kt** - Settings UI with multiple sections:
  - Appearance: App Theme selection (SYSTEM/LIGHT/DARK)
  - App Management: Category management navigation
  - Data Management: Backup/Restore/Reset operations with two-step confirmations
  - Date & Time: useSystemDefaults toggle, time format, date format, time zone, live preview card
  - About: App version and GitHub link
  
- **ChatbotActivity.kt** - AI chatbot interface
  - Full-screen chat with message history
  - Context-aware responses based on usage data
  
- **LimitsScreen.kt** - App limits feature (implemented)
- **AppIntroScreen.kt, PermissionScreen.kt** - Onboarding screens

#### ViewModels
- **StatsViewModel.kt** - Manages stats screen state
  - Non-nullable _periodState: MutableStateFlow<StatsPeriodState> (fixed NPE bug)
  - Observes formattedPreferences: StateFlow<FormattedDateTimePreferences?>
  - Methods: onNavigatePrevious(), onNavigateNext(), onPeriodChange(), refreshData()
  - Uses FormattedDateTimePreferences for all date/time formatting
  
- **SettingsViewModel.kt** - Manages theme, date/time, and backup settings
  - Exposes: appTheme, dateTimePreferences, formattedPreferences, backupState StateFlows
  - Theme methods: setAppTheme()
  - Date/Time methods: setUseSystemDefaults(), setTimeFormat(), setDateFormat(), setTimeZone()
  - Backup methods: createAndExportBackup(uri), importAndRestoreBackup(uri), resetAllData(), clearBackupState()
  - getAvailableTimeZones(): Returns grouped timezone list for picker
  - BackupState sealed class: Idle, InProgress(message), Success(message), Error(message)
  
- **ChatViewModel.kt** - Manages AI chatbot conversation
  - Maintains messages StateFlow with user and assistant messages
  - sendMessage(text): Sends user message and gets AI response
  - Uses Groq API with Ktor HTTP client
  - Includes system prompt with user behavior context
  
- **HomeViewModel.kt** - Home screen state and sync operations
- **OnboardingViewModel.kt** - Onboarding flow state

#### Stats Components
- **StatsChart.kt** - Usage chart with formatted axis labels
  - UsageChartCard accepts optional FormattedDateTimePreferences
  - Uses formatBottomLabel() with formatted preferences
  
- **StatsComponents.kt** - Reusable stats UI components
- **StatsSelectors.kt** - Metric and period selectors
- **StatsUtils.kt** - Formatting utilities for stats
  - formatBottomLabel() with FormattedDateTimePreferences parameter
  
- **StatsUiState.kt** - UI state definitions for stats screen

#### Theme
- **Theme.kt** - Material 3 theme configuration with dynamic color support
- **Color.kt** - Color palette definitions
- **Type.kt** - Typography definitions

#### Common UI Components
- **AppScaffold.kt** - Common scaffold wrapper for consistent app behavior
  - Provides unified Scaffold with customizable top bar, bottom bar, FAB, and snackbar
  - Automatically collapses top app bar on scroll for better UX
  - Combines scroll behaviors for top and bottom bars using nestedScroll
  - Includes AppBottomNavigationBar for consistent bottom navigation across screens
  - Used by all main screens (Home, Stats, Settings, Categories, Limits) for UI consistency

---

## 🎨 Date & Time Settings System Architecture

### Overview
The app implements a comprehensive date/time settings system that allows users to configure how dates and times are displayed throughout the app. This system follows a **single source of truth** principle with centralized formatting.

### Key Principles
1. **All timestamps stored in UTC milliseconds** - Database and internal calculations use UTC
2. **Conversion to display timezone happens ONLY at UI layer** - Using DateTimeFormatterManager
3. **User preferences respected consistently** - Settings propagate reactively via Flow
4. **No hardcoded date/time patterns** - All formatting goes through DateTimeFormatterManager
5. **System defaults as fallback** - When useSystemDefaults=true or format=SYSTEM

### Data Flow
```
User Changes Setting in SettingsScreen
    ↓
SettingsViewModel.setTimeFormat() / setDateFormat() / setTimeZone()
    ↓
PreferencesRepository.setDateTimePreferences()
    ↓
PreferencesDataStore saves to DataStore
    ↓
Flow<DateTimePreferences> emits new value
    ↓
DateTimeFormatterManager.formattedPreferences Flow creates new formatters
    ↓
StatsViewModel observes formattedPreferences StateFlow
    ↓
StatsScreen/StatsChart re-composes with new formatters
    ↓
UI displays updated date/time formatting
```

### Components Interaction

#### 1. DateTimePreferences Model
```kotlin
data class DateTimePreferences(
    val useSystemDefaults: Boolean = true,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val dateFormat: DateFormat = DateFormat.SYSTEM,
    val timeZoneId: String? = null
)

enum class TimeFormat { SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR }
enum class DateFormat { SYSTEM, DD_MMM_YYYY, MM_DD_YYYY, YYYY_MM_DD }
```

#### 2. DateTimeFormatterManager
- **Purpose**: Creates and caches DateTimeFormatter instances based on user preferences
- **Input**: Context, Flow<DateTimePreferences>, Locale
- **Output**: Flow<FormattedDateTimePreferences> with ready-to-use formatters
- **Key Features**:
  - Detects system 24-hour format using AndroidDateFormat.is24HourFormat(context)
  - Creates locale-aware formatters
  - Caches formatters for performance (recreated only when preferences change)
  - Provides convenient formatting methods

#### 3. FormattedDateTimePreferences Wrapper
```kotlin
data class FormattedDateTimePreferences(
    val preferences: DateTimePreferences,
    val zoneId: ZoneId,
    val timeFormatter: DateTimeFormatter,
    val dateFormatter: DateTimeFormatter,
    // ... other cached formatters
) {
    fun formatTime(utcMillis: Long): String
    fun formatDate(utcMillis: Long): String
    fun formatDateTime(utcMillis: Long): String
    // ... other convenience methods
}
```

#### 4. Usage in ViewModels
```kotlin
class StatsViewModel(
    private val dateTimeFormatterManager: DateTimeFormatterManager
) : ViewModel() {
    
    val formattedPreferences: StateFlow<FormattedDateTimePreferences?> = 
        dateTimeFormatterManager.formattedPreferences
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Use formatted preferences for all date/time operations
}
```

#### 5. Usage in UI
```kotlin
@Composable
fun StatsScreen(viewModel: StatsViewModel = koinViewModel()) {
    val formattedPrefs by viewModel.formattedPreferences.collectAsStateWithLifecycle()
    
    formattedPrefs?.let { prefs ->
        Text(prefs.formatDate(timestamp))
    }
}
```

### Settings UI Structure
**Appearance Section**:
- App Theme: SYSTEM / LIGHT / DARK

**Date & Time Section**:
- Use system date & time: Toggle (enables/disables custom settings)
- Time Format: SYSTEM / 12-Hour / 24-Hour
- Date Format: SYSTEM / DD-MMM-YYYY / MM-DD-YYYY / YYYY-MM-DD
- Time Zone: Grouped picker with search
- Live Preview Card: Shows current date/time formatted with current settings

### Important Notes
- **Context Dependency**: DateTimeFormatterManager REQUIRES Context for AndroidDateFormat.is24HourFormat()
- **Non-nullable State**: StatsViewModel uses non-nullable _periodState to prevent NullPointerException
- **Cached Formatters**: Formatters are expensive to create, so they're cached and recreated only on preference changes
- **ViewModel Merge**: DateTimeSettingsViewModel was merged into SettingsViewModel to avoid redundancy

---

## �🔧 Code Generation Guidelines

When generating code:

1. **Analyze the feature** and determine which folder it belongs to
2. **Check current structure** before suggesting new files
3. **Provide complete, compilable code** with:
   - All necessary imports
   - Proper annotations
   - Koin injection setup
   - Error handling
   - Add comments only where the code is not self-explanatory or could be ambiguous

4. **Follow naming conventions**:
   - ViewModels: `[Feature]ViewModel.kt`
   - Screens: `[Feature]Screen.kt`
   - Repositories: `[Feature]Repository.kt`
   - Data classes: Descriptive nouns (e.g., `AppUsage.kt`)

5. **Include Koin setup** if adding new dependencies:
   ```kotlin
   val myModule = module {
       single { MyRepository(get()) }
       viewModel { MyViewModel(get()) }
   }
   ```

6. **Use Material 3 components** with proper theming
7. **Implement proper state management**:
   ```kotlin
   sealed class UiState<out T> {
       object Loading : UiState<Nothing>()
       data class Success<T>(val data: T) : UiState<T>()
       data class Error(val message: String) : UiState<Nothing>()
   }
   ```

---

## 🎯 Feature Implementation Priority

### Phase 1: Foundation (Current)
1. ✅ Project setup
2. ✅ Basic navigation
3. ✅ Onboarding flow
4. ✅ Usage tracking infrastructure

### Phase 2: Core Analytics
1. ✅ Room database setup
2. ✅ Usage stats collection
3. ✅ Basic insights visualization
4. ✅ Date/time settings system
5. ✅ App categorization
6. ✅ Backup & restore

### Phase 3: AI & User Engagement
1. ✅ AI chatbot assistant
2. Prevention tools (app blocking, focus mode)
3. Gamification system
4. Advanced analytics (trends, heatmaps)

### Phase 4: Prevention & Control
1. App blocking mechanism
2. Focus mode with timers
3. Grayscale mode
4. Wait time before app launch
5. Shorts/Reels time limits

### Phase 5: Advanced Features
1. Gamification (badges, streaks, points)
2. AI-driven automation
3. Usage trends and heatmaps
4. Anti-uninstall protection

---

## 🚀 Quick Reference

**When adding a new feature:**
1. Create model in `models/`
2. Create repository in `data/repository/`
3. Create ViewModel in `ui/[feature]/`
4. Create Screen in `ui/[feature]/`
5. Add to navigation graph
6. Register dependencies in Koin module

**Always ensure:**
- ✅ Proper error handling
- ✅ Loading states
- ✅ Accessibility support
- ✅ Material 3 theming
- ✅ Koin dependency injection
- ✅ Coroutine usage for async operations
- ✅ DateTimeFormatterManager for all date/time display
- ✅ Android-specific APIs isolated in provider/utility classes

**Date/Time Formatting:**
- ✅ NEVER hardcode date/time patterns
- ✅ NEVER create custom DateTimeFormatter instances
- ✅ ALWAYS use DateTimeFormatterManager for all date/time display
- ✅ Store timestamps in UTC milliseconds
- ✅ Convert to display timezone at UI layer only
- ✅ Inject DateTimeFormatterManager into ViewModels that need formatting

---

## 📚 Additional Documentation

For detailed implementation guides and context, refer to:
- **USAGE_TRACKING_IMPLEMENTATION.md** - Usage stats tracking system
- **SYNC_USAGE_GUIDE.md** - Background sync implementation
- **Agents.md** (this file) - Complete project architecture and guidelines

---