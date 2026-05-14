# Reps — Claude Code Master Context
> *Every rep starts on the plate.*

## What is Reps?
Reps is a fully local, AI-powered Android fitness and nutrition tracking app.
It helps users lose fat, preserve muscle, and hit protein goals using on-device
Gemini Nano AI — no cloud, no subscriptions, no data leaving the phone.

**Min SDK:** 31 (Android 12+)
**Target SDK:** 35 (Android 15)
**Primary test device:** Samsung S24 Ultra (One UI 7, Android 15)
**Theme:** Dark only (gym aesthetic, high contrast)
**Language:** Kotlin (no Java)
**UI:** Jetpack Compose only — no XML layouts, ever

---

## Architecture Rules (NON-NEGOTIABLE)

### Pattern
- **MVVM strictly** — no business logic in Composables or Activities
- **Repository pattern** — ViewModels never touch DAOs directly
- **Single source of truth** — Room DB is the only persistent state
- **Unidirectional data flow** — UI observes StateFlow from ViewModel only

### Layering
```
UI (Compose Screens)
    ↕ StateFlow / Events
ViewModel (state holder, no logic)
    ↕ suspend functions
Repository (business logic, data coordination)
    ↕
Data Sources: Room DAO | Retrofit API | DataStore | AI Repository
```

### DI
- **Hilt everywhere** — every ViewModel, Repository, DAO, and UseCase is Hilt-injected
- Never instantiate dependencies manually
- Use `@HiltViewModel` for all ViewModels
- Provide all Retrofit clients and Room DB via `@Module` / `@Provides`

### Naming conventions
- Screens: `FooScreen.kt` (Composable entry point)
- ViewModels: `FooViewModel.kt`
- Repositories: `FooRepository.kt` + `FooRepositoryImpl.kt`
- DAOs: `FooDao.kt`
- Entities: `FooEntity.kt`
- DTOs (API response): `FooDto.kt`
- Mappers: `FooMapper.kt` (extension functions, never in Entity/DTO)
- Use Cases (complex logic only): `DoFooUseCase.kt`

### Package structure
```
com.reps.app
├── core/
│   ├── data/         (Room DB, DAOs, Entities)
│   ├── network/      (Retrofit, DTOs, API services)
│   ├── domain/       (Repository interfaces, UseCases, domain models)
│   └── di/           (Hilt modules)
├── feature/
│   ├── onboarding/
│   ├── dashboard/
│   ├── meal/         (logging, planning, suggestions)
│   ├── food/         (search, barcode, custom foods)
│   ├── workout/      (logging, templates, shoulder-safe filter)
│   ├── progress/     (weight, measurements, charts)
│   ├── grocery/      (list generator, store grouping)
│   └── settings/     (profile, goals, preferences)
├── ai/
│   ├── AIRepository.kt           (interface)
│   ├── GeminiNanoRepository.kt   (ML Kit / AICore impl)
│   └── RuleBasedRepository.kt    (fallback for unsupported devices)
└── ui/
    ├── theme/        (RepsTheme, colors, typography, shapes)
    └── components/   (shared Composables)
```

---

## Tech Stack

| Layer | Library | Version |
|---|---|---|
| UI | Jetpack Compose BOM | latest stable |
| Navigation | Navigation Compose | latest stable |
| DI | Hilt | 2.51+ |
| Local DB | Room | 2.6+ |
| Preferences | DataStore (Proto) | 1.1+ |
| Network | Retrofit + OkHttp | latest stable |
| JSON | Kotlin Serialization | latest stable |
| Background | WorkManager | 2.9+ |
| Charts | Vico | latest stable |
| Camera | CameraX | latest stable |
| Barcode scan | ML Kit Barcode Scanning | latest stable |
| On-device AI | ML Kit GenAI (Gemini Nano) | latest stable |
| Image loading | Coil 3 | latest stable |
| Coroutines | Kotlinx Coroutines | 1.8+ |

---

## Database Design Philosophy

**Everything is data, nothing is hardcoded.**
Meal plans, food items, workout templates, exercise lists, dietary preferences,
store names, cuisine tags — all live in Room or JSON assets. No enum or string
should represent user-facing content that could change.

### Key design decisions
- Use `Long` for all primary keys (auto-generated)
- Use `@TypeConverter` for lists (stored as JSON strings in columns)
- Every user-facing entity has a `isCustom: Boolean` flag — system seed data
  vs user-created data. Users can edit/delete only custom entries.
- Soft delete pattern: `isDeleted: Boolean = false` — never hard delete seed data
- All timestamps as `Long` (epoch millis)

---

## Dynamic / Configurable Design

The app is fully dynamic. Everything a user sees is driven by data:

### User profile (DataStore)
- Name, age, weight, target weight, height
- Dietary restrictions (tags: vegetarian, no-beef, gluten-free, etc.)
- Cuisine preferences (South Indian, North Indian, Mediterranean, etc.)
- Cook days per week, preferred meal count
- Activity level, workout days

### Meal plans (Room + JSON asset)
- Meal plan templates are JSON assets, seeded into Room on first install
- Users can create, edit, clone, and delete meal plan templates
- Each meal slot in a template points to a FoodItem by ID
- Macro targets auto-calculate from user profile — not hardcoded

### Food database (Room, seeded from assets + APIs)
- `indian_foods.json` seeded on first install (South Indian dishes with accurate macros)
- USDA FoodData Central API: search → cache in Room, never call twice for same food
- Open Food Facts: barcode scan → cache in Room
- User can add fully custom food items
- All food items have cuisine tags for filtering

### Workouts (Room + JSON asset)
- Exercise templates seeded from `exercises.json` asset + Wger API on first install
- Each exercise has: muscle groups, equipment, `isShoulderSafe: Boolean`,
  `restrictedMovements: List<String>` (e.g. "overhead", "behind-neck")
- Users can create custom exercises and workout templates
- Workout templates are fully editable

---

## AI Integration

### Engine: MediaPipe LLM Inference API + Gemma 3n E2B
We use MediaPipe LLM Inference (NOT ML Kit GenAI / AICore) because:
- Full prompt control — no restricted high-level API surface
- Works reliably on S24 Ultra (Snapdragon 8 Gen 3 NPU)
- Model is already pushed to device at `/data/local/tmp/llm/model.task`
- No internet required at runtime — 100% on-device

### Gradle dependency
```kotlin
implementation("com.google.mediapipe:tasks-genai:0.10.27")
```

### Model path
```kotlin
const val MODEL_PATH = "/data/local/tmp/llm/model.task"
```

### Initialization pattern
```kotlin
val options = LlmInference.LlmInferenceOptions.builder()
    .setModelPath(MODEL_PATH)
    .setMaxTokens(1024)
    .setTopK(40)
    .setTemperature(0.7f)
    .setRandomSeed(101)
    .build()
val llmInference = LlmInference.createFromOptions(context, options)
```

Always initialize LlmInference as a singleton via Hilt — it is expensive to
create and must NOT be instantiated per-request. Provide it in a Hilt module
with `@Singleton` scope.

### Architecture
```kotlin
interface AIRepository {
    suspend fun parseNaturalLanguageMeal(input: String): List<ParsedFoodItem>
    suspend fun getDailyInsight(log: DayLog): String
    suspend fun getMealSuggestion(remainingMacros: Macros): String
    suspend fun getChatResponse(history: List<ChatMessage>): String
    fun isAvailable(): Boolean
}
```

`MediaPipeAIRepository` — primary implementation using LlmInference.
Wraps all calls in `withContext(Dispatchers.IO)`.
Returns `Result<T>` — never throws to ViewModel.
Uses streaming API (`generateResponseAsync`) for chat screen for
incremental token display. Uses non-streaming for short structured responses.

`RuleBasedAIRepository` — pure Kotlin fallback, zero dependencies.
Used when model file is not found at MODEL_PATH (e.g. first install before
developer pushes model, or on unsupported devices).
Parses meal input via keyword matching against local Room food DB.
Returns template-based insight strings based on macro deltas.

Hilt binding:
```kotlin
@Provides @Singleton
fun provideAIRepository(
    @ApplicationContext context: Context,
    foodRepository: FoodRepository
): AIRepository {
    return try {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(MODEL_PATH)
            .setMaxTokens(1024)
            .build()
        val llm = LlmInference.createFromOptions(context, options)
        MediaPipeAIRepository(llm, foodRepository)
    } catch (e: Exception) {
        RuleBasedAIRepository(foodRepository)  // graceful fallback
    }
}
```

### Prompt engineering rules
- Always inject user context into EVERY prompt — model has zero memory between calls
- Keep prompts under 400 tokens for fast NPU inference (target <2 sec response)
- Always request JSON output for structured responses — parse with try/catch
- Wrap JSON output request in clear delimiters: "Respond ONLY with valid JSON. No preamble."
- For chat, maintain rolling window of last 8 messages max to stay within token limit
- Temperature 0.2 for structured/JSON responses, 0.7 for conversational chat
- Test all prompts in Google AI Edge Gallery on S24 Ultra before coding

### Streaming for chat (AI Coach screen)
```kotlin
llmInference.generateResponseAsync(prompt) { partialResult, done ->
    // partialResult is each token as it's generated
    // Emit to StateFlow<String> for live UI updates
    // done = true signals end of response
}
```

### AI features by screen
| Screen | AI Feature | Response type |
|---|---|---|
| Meal Log | Natural language → parsed food items | JSON, non-streaming |
| Dashboard | End-of-day insight card | Short text, non-streaming |
| Meal Planner | Suggest meal with X protein remaining | JSON, non-streaming |
| AI Coach | Full conversational chat | Streaming tokens |
| Workout | Flag unsafe exercises, suggest alternatives | Short text, non-streaming |

---

## External APIs (Cache-and-Forget Pattern)

All APIs are called once per unique query and results cached in Room.
The app must function 100% offline after initial seeding.

### USDA FoodData Central
- Base URL: `https://api.nal.usda.gov/fdc/v1/`
- Key: stored in `local.properties` as `USDA_API_KEY`, injected via BuildConfig
- Use for: generic food search (chicken breast, eggs, oats, etc.)
- Cache entity: `FoodItem` with `source = "USDA"`

### Open Food Facts
- Base URL: `https://world.openfoodfacts.org/api/v0/`
- No key required
- Use for: barcode scanning of packaged foods
- Cache entity: `FoodItem` with `source = "OFF"`

### API Ninjas Nutrition
- Base URL: `https://api.api-ninjas.com/v1/nutrition`
- Key: stored in `local.properties` as `API_NINJAS_KEY`
- Use for: natural language food queries as fallback when Gemini Nano unavailable
- Cache entity: `FoodItem` with `source = "NINJAS"`

### Wger Exercise DB
- Base URL: `https://wger.de/api/v2/`
- No key required, read-only
- Use for: seeding exercise database on first install only
- Called once via WorkManager `OneTimeWorkRequest` on first launch

---

## Seed Data Assets
Located in `app/src/main/assets/`:
- `indian_foods.json` — 60+ Indian dishes with accurate macros per standard serving
- `meal_plan_templates.json` — 3 default South Indian meal plan templates
- `exercises.json` — 50 shoulder-safe exercises pre-tagged

These are seeded into Room by `SeedDatabaseWorker` (WorkManager, runs once on install).
Check a `DataStore` flag `is_db_seeded: Boolean` before seeding — never seed twice.

---

## Error Handling Rules
- Never crash on network failure — all API calls return `Result<T>`
- Use sealed class for UI state: `data class Success`, `data class Error`, `object Loading`
- Room operations never throw to UI — wrap in try/catch in Repository
- AI inference failures silently fall back to `RuleBasedRepository`
- Show user-friendly error messages, never raw exception messages

---

## Code Style Rules
- No magic numbers — use named constants in companion objects
- No hardcoded strings in Composables — use `stringResource()` always
- No business logic in Composables — call ViewModel functions only
- Composables must be preview-able — add `@Preview` for every screen
- Use `LazyColumn` for all lists — never `Column` with forEach for dynamic data
- `BigDecimal` for all macro/calorie calculations — never Float or Double
- All database writes on `Dispatchers.IO` — enforce via Repository
- Coroutine scope: ViewModels use `viewModelScope`, Repositories inject `CoroutineDispatcher`

---

## Build Phases

Feed Claude Code one phase at a time. Complete and test each phase before moving on.

### Phase 1 — Foundation (Week 1)
Room DB schema, all entities, DAOs, Hilt modules, DataStore setup,
RepsTheme (dark), Navigation graph skeleton, SeedDatabaseWorker,
`indian_foods.json` asset, basic onboarding flow (name, weight, goal, dietary prefs)

### Phase 2 — Food Database (Week 1–2)
FoodItem search screen, USDA API integration + Room caching, barcode scanner
(CameraX + ML Kit), custom food creation screen, food detail screen

### Phase 3 — Meal Logging (Week 2)
Daily meal log screen, add meal flow (search / barcode / natural language input),
macro progress bar (protein, carbs, fat, calories), meal history, edit/delete log entries

### Phase 4 — Meal Planning (Week 3)
Weekly meal plan screen, meal plan templates (from seed JSON), meal slot editor,
grocery list auto-generator (grouped by store category), batch cook planner

### Phase 5 — Workout Logging (Week 3–4)
Exercise DB (seeded from Wger + exercises.json), workout template builder,
daily workout log, shoulder-safe filter toggle, rep/set/weight tracker,
rest timer (WorkManager)

### Phase 6 — Progress Tracking (Week 4)
Weight check-in screen, body measurements, Vico charts (weight over time,
macro adherence, workout frequency), goal progress visualization,
streak tracker (protein goal hit, workout done, water target)

### Phase 7 — AI Layer (Week 5)
AIRepository interface + both impls, Gemini Nano integration (ML Kit GenAI),
natural language meal parsing, daily insight card on dashboard,
AI meal suggestion, AI Coach chat screen (full conversational UI)

---

## What NOT to do
- Never use XML layouts
- Never access DAO from ViewModel directly
- Never hardcode user-specific data (macros, goals, food preferences)
- Never call APIs on the main thread
- Never store API keys in source code — use local.properties + BuildConfig
- Never use SharedPreferences — use DataStore only
- Never use AsyncTask — use coroutines only
- Never ignore the `isShoulderSafe` flag in workout suggestions
- Never skip the `is_db_seeded` check before seeding
