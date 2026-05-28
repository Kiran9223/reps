# Reps — AI-Powered Fitness & Nutrition Tracker

> *Every rep starts on the plate.*

A **local-first**, AI-powered Android fitness and nutrition tracker. Reps helps you lose fat, preserve muscle, and hit protein goals with your data stored on-device in Room and DataStore. Core logging works offline; optional on-device LLM (MediaPipe + Gemma) and optional cloud assist (Gemini API) power smarter features when you enable them.

---

## Table of Contents

- [Privacy & Data](#privacy--data)
- [Overview](#overview)
- [Features](#features)
  - [Onboarding](#1-onboarding)
  - [Dashboard](#2-dashboard)
  - [Meal Logging](#3-meal-logging)
  - [Meal Planning](#4-meal-planning)
  - [Workout Logging](#5-workout-logging)
  - [Progress Tracking](#6-progress-tracking)
  - [Grocery Lists](#7-grocery-lists)
  - [AI Coach (Chat)](#8-ai-coach)
  - [Import Plans](#9-import-plans)
  - [Settings](#10-settings)
- [AI Architecture](#ai-architecture)
- [Tech Stack](#tech-stack)
- [Database Schema](#database-schema)
- [Project Structure](#project-structure)
- [Build Setup](#build-setup)

---

## Privacy & Data

Reps is designed **local-first**: your meal logs, workouts, weight, measurements, grocery lists, macro targets, and profile preferences live on your phone. There is no Reps account and no Reps-owned backend.

### Always on this device

| Data | Storage |
|---|---|
| Meal & workout logs, templates, history | Room |
| Profile, goals, dietary prefs | DataStore |
| Food & exercise library (after seed/cache) | Room |
| AI Coach chat history | Room (until you clear chat) |

### On-device AI (optional)

Push the Gemma model to the device (see [Build Setup](#on-device-ai-model-optional)). When loaded, these features use **MediaPipe LLM Inference** only — prompts and responses do not leave the phone:

- Natural-language meal parsing  
- Daily insights & meal suggestions  
- Grocery item categorization  
- Nutrition estimates (“Estimate with AI”)  
- Shoulder-safe alternatives & quick workouts  

If the model is not installed, the app uses **rule-based fallbacks** for some of these (template insights, keyword matching). Natural-language meal parsing requires the on-device model.

### Cloud assist (optional, build-time)

If you add `GEMINI_API_KEY` to `local.properties`, a **Hybrid** router sends specific tasks to **Google Gemini** (`gemini-2.5-flash`). Your data is only sent when you use those features:

| Feature | What is sent to Google |
|---|---|
| **AI Coach** | Your profile summary, today’s macros, and the last 8 chat messages + your new message |
| **Import Plan** | The plan text you paste when you tap Parse |
| **Workout template / exercise detail AI** | Template name or exercise name + context from your local exercise list (for matching) |

You can use Reps **without** a Gemini key: logging, planning, progress, and on-device AI (with model) still work; AI Coach and Import Plan parsing will not.

### Optional network APIs (not generative AI)

These fetch public food/exercise data once per query and **cache results in Room** for offline use:

| API | Purpose |
|---|---|
| USDA FoodData Central | Generic food search |
| Open Food Facts | Barcode lookup |
| Wger | Exercise search / one-time seed |

API keys for USDA and API Ninjas are optional; Wger and Open Food Facts do not require keys.

### In the app

Open **Settings → Privacy & data** to see whether on-device AI and cloud assist are active on your build.

---

## Overview

| Property | Value |
|---|---|
| Platform | Android |
| Privacy model | Local-first; optional on-device LLM; optional Gemini cloud assist |
| Min SDK | 31 (Android 12) |
| Target SDK | 35 (Android 15) |
| Language | Kotlin (100%) |
| UI | Jetpack Compose — Material 3 |
| Theme | Dark only |
| Architecture | MVVM + Repository + Hilt |
| Primary test device | Samsung S24 Ultra (One UI 7) |

---

## Features

### 1. Onboarding

A five-step first-launch wizard that builds the user's complete fitness profile.

| Step | What it collects |
|---|---|
| Welcome | App introduction |
| Personal | Name, age |
| Body stats | Current weight, target weight, height |
| Training profile | Activity level, workout days per week, shoulder restriction flag |
| Food preferences | Dietary restrictions, cuisine preferences |

**Dietary restriction options:** Vegetarian, Vegan, No beef, No pork, No seafood, Gluten-free, Dairy-free  
**Cuisine options:** South Indian, North Indian, Mediterranean, Continental, Chinese

All data is persisted in DataStore and used throughout the app to personalise macro targets, food suggestions, and AI responses.

---

### 2. Dashboard

The home screen. Shows today's nutrition at a glance with quick-add shortcuts and AI-generated feedback.

**What you can do:**
- View calories, protein, carbs, and fat consumed vs. daily targets (macro progress bars)
- Navigate to any past date
- See a per-meal-slot breakdown (Breakfast, Lunch, Dinner, Snacks)
- Log water intake
- Tap any logged food to edit serving size or swipe to delete
- Request an AI daily insight card (personalised 2-sentence feedback based on your day)
- Request an AI meal suggestion for any slot ("What should I have for lunch to hit my protein?")
- Quick-log a suggested meal directly from the dashboard
- View a 7-day snapshot: days protein goal was hit, average calorie intake, current streak

**Macro targets** are auto-calculated from the user's profile using TDEE estimation and are never hardcoded.

---

### 3. Meal Logging

A suite of four ways to add food — covering every scenario from a quick scan to typing a description in plain English.

#### 3a. Food Search
- Search the full food library by name (debounced, instant results)
- Filter by All / Indian / Recent / Custom tabs
- Tap any result to go to Food Detail and add it with a custom serving multiplier (0.25× – 5×)

#### 3b. Barcode Scanner
- Real-time camera scanning using CameraX + ML Kit Barcode Detection
- Looks up the scanned barcode in the local Room database
- On match: navigates directly to Food Detail
- Packaged foods previously scanned are cached permanently — no repeat API calls

#### 3c. Natural Language Entry
- Type a description like `"2 cups rice, 150g chicken breast, 1 cup dal"` and the LLM parses it
- Returns a list of food tokens (name, quantity, unit) matched against the library
- Falls back to a rule-based keyword parser if the on-device model isn't loaded
- Review parsed items before logging — confirm or cancel individual entries

#### 3d. Custom Food Creation
- Manually enter food name, serving description, and all macro values
- **"Estimate with AI" button:** type a food name like `"4 pooris"` and the on-device LLM estimates calories, protein, carbs, fat, fiber, and serving weight — auto-filling all fields
- The rule-based fallback knows common Indian foods (poori, idli, dosa, roti, paratha, upma, vada, etc.) and handles quantity prefixes ("4 pooris" → 4 × per-unit values)
- Add cuisine tags for filtering
- Saved foods appear instantly in all search results

---

### 4. Meal Planning

Full weekly meal plan management with the ability to create, edit, clone, and activate plans.

#### 4a. Meal Plan View
- View the active plan across 4 day patterns (Mon/Thu, Tue/Fri, Wed/Sat, Sunday)
- See all meal slots for each day with food names and macro totals
- Edit mode: swap foods, adjust servings per slot
- **Log Today's Plan:** logs the entire current day's planned meals to the meal log in one tap
- Clone an existing plan as a starting point for a new one
- Delete plans

#### 4b. Create / Edit Meal Plan
- Name the plan, add a description, and choose a cuisine type
- Add foods to specific slots (Breakfast / Morning Snack / Lunch / Afternoon Snack / Dinner / Evening Snack) for each of the 4 days
- Remove individual foods or entire days
- Save creates a new plan and sets it as active automatically

#### 4c. Batch Cook Planner
- Analyses the active meal plan and groups all required foods into prep sessions
- **Sunday session** covers Monday–Wednesday meals
- **Wednesday session** covers Thursday–Sunday meals
- Foods grouped into categories: Proteins, Carbs, Curries & Sides, Snacks
- Deduplicates foods that appear across multiple days

#### 4d. Import Plans from Text
See [Import Plans](#9-import-plans).

---

### 5. Workout Logging

End-to-end workout tracking from template creation to set-by-set logging with rest timers.

#### 5a. Workout Log Home
- See all saved workout templates
- See the 20 most recent completed sessions with duration, set count, and exercise count
- **Shoulder-safe filter toggle:** when enabled, hides any template containing non-shoulder-safe exercises
- Start a workout from a template or as a blank quick-start session
- Edit or delete templates (applies to seeded templates too)

#### 5b. Active Workout Session
- Work through exercises set by set
- Log reps and weight for every set — form shows previous best for reference
- **Rest timer:** 90-second countdown auto-starts after each set; skip button available
- **Personal record indicator:** highlights when current weight or reps exceeds your previous best for that exercise
- **Shoulder safety warning:** if your profile has a shoulder restriction and the current exercise isn't marked shoulder-safe, a warning appears with AI-suggested alternatives
  - AI returns 2–3 safer substitutes with explanations (e.g. "Cable Row — works lats without overhead stress")
- Finish workout saves all sets and marks the session complete

#### 5c. Workout History
- Tap any recent session card to view a full read-only history of every exercise, set, rep count, and weight used

#### 5d. Create / Edit Templates
- Name the template and add a description
- **"Generate with AI" button:** type a template name like `"Push Day"` or `"Leg Day"` and the AI suggests a description and a full exercise list with sets, reps, and target weights — auto-filling all fields. Uses Gemini if available, falls back to the on-device model or keyword-based presets
- Pick exercises from the Exercise Library (search + muscle-group filter)
- Set target sets, reps, and optional target weight per exercise
- Reorder exercises by drag handle
- Save creates a new template; same screen pre-fills for edit

#### 5e. Exercise Library
- Search by name (instant, debounced)
- Filter by muscle group (chip row)
- Toggle shoulder-safe filter
- Tap to view exercise details or select for a template (pick mode)
- Exercises seeded from a bundled `exercises.json` asset on first install
- **"Search online" fallback:** if a search returns no local results, a button appears to query the Wger exercise database — found exercises are downloaded, mapped, and saved to Room so they appear immediately and are available offline from that point forward; already-cached exercises are skipped to avoid duplicates
- **"Create Custom" FAB:** always visible — opens the Custom Exercise Creation screen. Also appears as a button in the empty-results state when a search returns nothing

#### 5f. Custom Exercise Creation
- Enter an exercise name and tap **"Estimate with AI"** — the AI fills in muscle groups, equipment, shoulder-safe flag, restricted movements, and a description
- All fields are editable after AI fill — nothing is locked in
- Shoulder Safe toggle defaults to on; disable it to flag movements that stress the shoulder joint
- Muscle Groups and Equipment are comma-separated text fields (e.g. `"Quadriceps, Glutes"`, `"Barbell, Rack"`)
- Restricted Movements field captures unsafe movement patterns (e.g. `"overhead, behind-neck"`)
- AI backend: tries Gemini first for richer descriptions, falls back to on-device MediaPipe, then keyword matching — always returns a result
- Saved exercises appear immediately in the Exercise Library and can be added to any template

---

### 6. Progress Tracking

#### 6a. Progress Overview
- Weight history list with dates and notes
- Goal progress bar (start weight → target weight) with percentage complete and kg remaining
- **Estimated weeks to goal** calculated from your recent weight trend (linear regression)
- Streak counter: consecutive days the protein goal was hit
- Weekly stats: days protein goal hit, average daily calories
- Log a new weight check-in from a bottom sheet (weight + optional notes)
- Delete past check-in entries

#### 6b. Body Measurements
- Log waist, chest, arm, and thigh circumference (cm)
- Add optional notes per entry
- View full measurement history
- Delete past entries

---

### 7. Grocery Lists

Persistent, multi-list grocery management with AI-powered automatic categorisation.

#### 7a. Lists Overview
- See all your saved grocery lists with item counts and bought progress
- Create a new named list (FAB → dialog)
- Delete a list (confirms with dialog; CASCADE-deletes all items)

#### 7b. List Detail
- Items displayed grouped by category: **Protein · Dairy · Produce · Frozen · Pantry**
- **Sticky add-item bar** fixed at the bottom — always visible while scrolling
- Type an item name and tap Add:
  - Calls the on-device LLM to categorise the item (PROTEIN / DAIRY / PRODUCE / FROZEN / PANTRY)
  - A brief spinner shows while the AI runs; falls back to keyword matching if unavailable
  - Item immediately appears under the correct category header
- Tap the checkbox to mark an item bought (strikethrough)
- Delete individual items with the bin icon
- **"Add from meal plan" button:** imports all unique food ingredients from the active meal plan into the list, pre-categorised using the keyword mapper
- Share the full list as formatted text (categories + check/uncheck symbols) via the system share sheet

---

### 8. AI Coach

A full conversational fitness coach. **Requires cloud assist** (`GEMINI_API_KEY` at build time). Messages are sent to Google Gemini; see [Privacy & Data](#privacy--data).

**What the AI knows about you (injected into every message):**
- Your name, weight, target weight
- Today's calorie and protein intake vs targets
- Workout frequency and shoulder restriction status
- Dietary restrictions

**Capabilities:**
- Answer any fitness or nutrition question
- Provide workout advice, meal suggestions, and motivation
- **In-chat actions:** when you ask the AI to create something, it embeds a structured action in its reply that you can review and confirm:

| Action | What it does |
|---|---|
| `LOG_MEAL` | Logs a list of foods to a specific meal slot today |
| `UPDATE_GOAL` | Updates your target weight or weekly workout days |
| `CREATE_WORKOUT_PLAN` | Creates a new workout template with exercises, sets, and reps |
| `CREATE_MEAL_PLAN` | Creates a new weekly meal plan with slots and foods |

- Streaming responses: text appears token-by-token as the LLM generates it
- Chat history stored in Room; the last 8 messages are sent as context with each new message
- Clear chat resets history

---

### 9. Import Plans

Paste any workout or diet plan text (copied from a website, PDF, or message) and the AI parses it into a structured plan you can review before saving.

**Workflow:**
1. Choose plan type: **Workout** or **Meal**
2. Paste your plan text into the input field
3. Tap **Parse** — Gemini analyses the text (handles merged table columns, inconsistent formatting)
4. **Review screen:** see every day, exercise/food, set/rep/quantity — delete anything you don't want
5. Edit the plan name
6. Tap **Confirm Import**

**What happens on save:**
- **Workout:** Each day in the parsed plan becomes a workout template. Exercises are fuzzy-matched against the Exercise Library (name-contains matching). Unmatched exercises are skipped and reported.
- **Meal:** Foods are fuzzy-matched against the Food Library. Unmatched foods are skipped and reported. A new meal plan template is created.

> **Note:** Plan parsing uses Gemini (cloud) for reliability with complex, table-formatted input. On-device MediaPipe is used for all other AI features to avoid quota usage.

---

### 10. Settings

Edit every aspect of your profile at any time:

- Name, age, current weight, target weight, height
- Activity level (Sedentary / Light / Moderate / Active / Very Active)
- Workout days per week
- Shoulder restriction toggle
- Dietary restrictions (multi-select)
- Cuisine preferences (multi-select)

Changes immediately affect macro target calculations throughout the app.

---

## AI Architecture

Reps uses a three-tier hybrid AI system to balance capability, privacy, and API costs.

```
┌─────────────────────────────────────────────────────────┐
│                    HybridAIRepository                   │
│                                                         │
│  Chat / Plan Import ──────────────────► GeminiRepository│
│                                         (Cloud, Gemini  │
│  Exercise details /                     2.5 Flash)      │
│  Template generation  ─► Gemini first,                  │
│                          then MediaPipe fallback         │
│                                                         │
│  All other tasks ──────────────────────►                 │
│  (NL meal, insights,         MediaPipeAIRepository      │
│   meal suggest,         ──► (On-device, Gemma 3n via    │
│   shoulder alts,             MediaPipe LLM Inference)   │
│   AI categorise,                                        │
│   AI estimate nutrition)                                │
│                                                         │
│  Fallback (model not found)                             │
│  ─────────────────────────► RuleBasedAIRepository       │
│                              (Pure Kotlin, no deps)     │
└─────────────────────────────────────────────────────────┘
```

| Task | Backend | Reason |
|---|---|---|
| Chat | Gemini | Requires long context, action parsing |
| Import plan (parse) | Gemini | Complex table/text parsing needs strong model |
| Exercise detail estimation | Gemini → MediaPipe fallback | Richer descriptions from cloud; always works offline |
| Workout template generation | Gemini → MediaPipe fallback | Exercise selection from live DB; keyword presets as last resort |
| Nutrition estimation | MediaPipe | Fast, frequent, runs on save |
| Grocery categorisation | MediaPipe | Simple classification, called per item |
| Natural language meal | MediaPipe | Frequent, short output |
| Daily insight | MediaPipe | Short text, called once per day |
| Meal suggestion | MediaPipe | Short JSON, called on demand |
| Shoulder alternatives | MediaPipe | Called at most once per workout |

**MediaPipe model path:** `/data/local/tmp/llm/model.task`  
**Gemini model:** `gemini-2.5-flash`

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose BOM + Material 3 |
| Navigation | Navigation Compose |
| Dependency Injection | Hilt 2.51+ |
| Local DB | Room 2.6+ |
| Preferences | DataStore Preferences |
| Network | Retrofit + OkHttp |
| JSON | Kotlinx Serialization |
| Background tasks | WorkManager |
| Charts | Vico |
| Camera | CameraX |
| Barcode scanning | ML Kit Barcode Scanning |
| On-device AI | MediaPipe LLM Inference (`tasks-genai:0.10.27`) |
| Cloud AI | Google Generative AI SDK (Gemini) |
| Image loading | Coil 3 |
| Async | Kotlinx Coroutines 1.8+ |

---

## Database Schema

Room database version: **4**  
All timestamps stored as epoch milliseconds (`Long`). All primary keys are auto-generated `Long`.

| Table | Description |
|---|---|
| `food_items` | Food library — name, serving size, macros, barcode, cuisine tags, source, isCustom |
| `meal_logs` | One row per day — the container for that day's entries |
| `meal_log_entries` | Individual logged foods — date, slot, foodId, serving multiplier |
| `exercises` | Exercise library — name, muscle groups, isShoulderSafe, description |
| `workout_templates` | Template definitions — name, description, isCustom |
| `workout_template_exercises` | Template exercise list — templateId, exerciseId, sets, reps, targetWeight, sort order |
| `workout_logs` | Completed workout sessions — templateId, startTime, endTime |
| `workout_sets` | Individual sets within a session — exerciseId, reps, weight, rest seconds |
| `weight_logs` | Weight check-ins — date, weightKg, notes |
| `meal_plan_templates` | Meal plan definitions — name, description, cuisineType, isCustom |
| `meal_plan_slots` | Foods within a plan — planId, dayIndex, mealType, foodId, servingMultiplier |
| `body_measurements` | Measurement logs — date, waist, chest, arms, thighs, notes |
| `ai_chat_messages` | Chat history — content, isFromUser, timestamp |
| `grocery_lists` | Grocery list containers — name, createdAt |
| `grocery_items` | Items in a list — listId (CASCADE FK), name, category, quantity, isBought, addedAt |

**Migrations:** `1→2` (body measurements), `2→3` (AI chat messages), `3→4` (grocery lists + items)

---

## Project Structure

```
com.reps.app
├── ai/
│   ├── AIRepository.kt              # Interface + shared models
│   ├── GeminiRepository.kt          # Cloud AI implementation
│   ├── MediaPipeAIRepository.kt     # On-device AI implementation
│   ├── RuleBasedAIRepository.kt     # Zero-dependency fallback
│   ├── HybridAIRepository.kt        # Routes tasks to correct backend
│   ├── ImportPlanModels.kt          # Serialisable parsed plan types
│   └── AIAction.kt                  # In-chat action models
│
├── core/
│   ├── data/
│   │   ├── RepsDatabase.kt          # Room database (v4)
│   │   ├── dao/                     # All DAOs
│   │   ├── entity/                  # All Room entities
│   │   ├── repository/              # Repository implementations
│   │   ├── mapper/                  # Entity ↔ domain mappers
│   │   ├── relation/                # Room JOIN relations
│   │   ├── datastore/               # DataStore wrappers
│   │   └── worker/                  # WorkManager workers (seed, rest timer, daily insight)
│   ├── domain/
│   │   ├── model/                   # Domain models (pure Kotlin)
│   │   └── repository/              # Repository interfaces
│   └── di/                          # Hilt modules (DB, Repositories, AI, Dispatchers, Network)
│
├── feature/
│   ├── onboarding/                  # 5-step profile wizard
│   ├── dashboard/                   # Home — daily nutrition summary
│   ├── meal/                        # Meal log, plan, batch cook, create plan
│   ├── food/                        # Search, barcode, detail, custom creation, NL entry
│   ├── workout/                     # Log, active session, history, template builder, library
│   ├── progress/                    # Weight check-ins, measurements, charts
│   ├── grocery/                     # Lists overview + detail with AI categorisation
│   ├── importplan/                  # AI-powered plan text importer
│   ├── ai/                          # AI Coach chat screen
│   ├── settings/                    # Profile editor
│   └── more/                        # More menu
│
├── navigation/
│   ├── Screen.kt                    # All route definitions
│   └── RepsNavGraph.kt              # NavHost with all composables
│
└── ui/
    ├── theme/                       # RepsTheme, colours, typography
    └── components/                  # Shared composables
```

---

## Build Setup

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- Android device or emulator running API 31+

### API Keys

Create `local.properties` in the project root (this file is gitignored) and add:

```properties
GEMINI_API_KEY=your_gemini_api_key_here
USDA_API_KEY=your_usda_fooddata_key_here
API_NINJAS_KEY=your_api_ninjas_key_here
```

- **GEMINI_API_KEY** — Optional but required for **AI Coach** and **Import Plan** parsing. When set, those features send data to Google Gemini. Get a free key at [aistudio.google.com](https://aistudio.google.com). Leave unset for a fully local build (no cloud AI).
- **USDA_API_KEY** — Optional. Used for generic food search (chicken, eggs, etc.). Get a free key at [fdc.nal.usda.gov](https://fdc.nal.usda.gov/api-key-signup.html).
- **API_NINJAS_KEY** — Optional fallback for natural language food queries.

### On-Device AI Model (Optional)

For on-device LLM features (nutrition estimation, AI categorisation, meal suggestions, etc.), push the Gemma model to your device:

```bash
adb push <path-to-model.task> /data/local/tmp/llm/model.task
```

The app gracefully falls back to `RuleBasedAIRepository` if the model file is not present.

### Build & Run

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and run directly on a device/emulator.

### Seed Data

On first install, `SeedDatabaseWorker` automatically seeds:
- `indian_foods.json` — 60+ Indian dishes with accurate macros
- `exercises.json` — 50+ exercises pre-tagged with muscle groups and shoulder-safe flags
- Wger exercise API call (one-time, via WorkManager) to extend the exercise library

Seeding only runs once — controlled by a `is_db_seeded` DataStore flag.
