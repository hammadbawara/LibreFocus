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
│   │   │   ├── dao/                 # Room DAOs
│   │   │   └── entity/              # Room entities
│   │   └── datastore/
│   │       └── PreferencesDataStore.kt
│   ├── repository/
│   │   ├── PreferencesRepository.kt
│   │   ├── UsageRepository.kt
│   │   └── UsageTrackingRepository.kt
│   └── local/
│       └── datasource/
│           └── UsageStatsDataSource.kt
├── di/
│   ├── AppModule.kt
│   ├── DatastoreModule.kt
│   ├── DatabaseModule.kt
│   └── WorkerModule.kt
├── models/
│   ├── AppUsage.kt
│   ├── AppUsageData.kt
│   ├── HourlyUsageData.kt
│   └── UsageValuePoint.kt
├── utils/
│   └── TimeUtils.kt
├── workers/
│   └── UsageTrackingWorker.kt
└── ui/
    ├── MainActivity.kt
    ├── MainViewModel.kt
    ├── home/
    ├── onboarding/
    ├── stats/
    │   ├── StatsScreen.kt
    │   ├── StatsViewModel.kt
    │   └── components/
    └── theme/
```

---

## ✅ Progress Tracker

### Completed Features
- [x] Basic project structure with MVVM architecture
- [x] Koin dependency injection setup
- [x] DataStore for preferences management
- [x] Onboarding flow (intro + permissions)
- [x] Home screen foundation
- [x] Navigation graphs for Home and Onboarding
- [x] Material 3 theming (colors, typography)
- [x] Basic app usage data model
- [x] Usage and Preferences repositories
- [x] **Room database implementation**
  - [x] App usage history tracking (hourly aggregation)
  - [x] App entities and categories
  - [x] Daily device usage (unlock tracking)
  - [x] Sync metadata
- [x] **Usage tracking infrastructure**
  - [x] UsageStatsDataSource for system stats
  - [x] UsageTrackingRepository with hourly aggregation
  - [x] WorkManager for periodic background sync
  - [x] Session duration calculation
  - [x] Launch count tracking
  - [x] Unlock count tracking
- [x] **Stats/Analytics Screen**
  - [x] Daily/weekly/monthly usage charts
  - [x] Metric selection (usage time, launches, unlocks)
  - [x] Time range selector with custom range picker
  - [x] Period navigation (previous/next)
  - [x] Top apps list
  - [x] Summary statistics

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
  - [x] Category-based statistics
  - [ ] Usage trends and heatmaps
  
- [ ] **App Categorization**
  - [ ] Auto-categorization logic
  - [ ] Custom category CRUD operations
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
  
- [ ] **AI & Motivation**
  - [ ] Personalized motivational messages
  - [ ] AI-driven automation (focus mode triggers)
  - [ ] Alternative app suggestions
  
- [ ] **Security & Persistence**
  - [ ] Anti-uninstall protection
  - [ ] Backup & restore functionality

#### 3. UI/UX Improvements
- [x] ~~Complete insights/analytics screens~~
- [ ] App categorization management UI
- [ ] Gamification dashboard
- [ ] Prevention tools settings
- [ ] AI/motivation screens
- [ ] Enhanced data visualization (heatmaps, trends)

#### 4. Background Services
- [x] ~~WorkManager for periodic usage tracking~~
- [ ] Foreground service for active monitoring
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

#### Data Layer
- **DON'T** perform database operations on main thread
- **DON'T** expose mutable state from ViewModels (use immutable StateFlow)
- **DON'T** forget to handle null/empty states
- **DON'T** skip database migrations

---

## 🔧 Code Generation Guidelines

When generating code:

1. **Analyze the feature** and determine which folder it belongs to
2. **Check current structure** before suggesting new files
3. **Provide complete, compilable code** with:
   - All necessary imports
   - Proper annotations
   - Koin injection setup
   - Error handling
   - Documentation comments

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
4. 🚧 App categorization

### Phase 3: Prevention Tools
1. App blocking mechanism
2. Focus mode
3. Grayscale mode
4. Wait time feature

### Phase 4: Engagement
1. Gamification system
2. Badges and streaks
3. Progress tracking
4. Motivational messages

### Phase 5: Advanced Features
1. AI personalization
2. Shorts/Reels tracking
3. Backup & restore
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

---