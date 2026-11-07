# TaskManager (Stage 1)

A native Android task-management app built with **Kotlin**, **Jetpack Compose**, and an
**MVVM + Repository** architecture. Fully offline (Room/SQLite), no networking, no
notifications, no widgets — Stage 1 focuses purely on core in-app task management.

## Tech stack

| Concern        | Choice                                   |
|----------------|------------------------------------------|
| Language       | Kotlin                                   |
| UI             | Jetpack Compose + Material 3 (Material You) |
| Architecture   | MVVM + Repository pattern                 |
| Database       | Room (local SQLite, offline)             |
| DI             | Hilt                                     |
| Async          | Coroutines + Flow                        |
| Navigation     | Navigation Compose (bottom nav bar)      |
| Min / Target   | SDK 26 / SDK 34                          |

## Features

- **Today** — today's active tasks on top, completed-today below, a progress indicator
  ("5 of 8 tasks completed"), tap to toggle completion, and a FAB to add tasks.
- **All Tasks** — full list with search (title/description), filters (category, priority,
  completion status, due date), swipe-to-delete, and tap-to-open detail.
- **Add / Edit** — title, description, category, priority, a Material 3 date picker, plus a
  recurring toggle with a recurrence-rule selector (daily / weekly / monthly / custom days).
- **Task Detail** — all task info with edit, delete (confirmed), and complete/incomplete.
- **History** — completed tasks with completion dates; recurring tasks show a **streak**
  (consecutive completions) and total completion count.
- Material You dynamic color on Android 12+, full **dark mode** support, and smooth
  slide/fade screen transitions.

## Recurring tasks: template vs. instance

A recurring task is split into two persisted pieces:

- a **template** (`task_templates`) holding the repeating pattern, and
- one or more **instances** (`tasks`) that each point back to the template (`templateId`).

Completing, skipping, or postponing a single instance never mutates the template, so the
pattern is preserved. Completing a recurring instance rolls the pattern forward by generating
the next pending occurrence. (Full background scheduling is intentionally out of scope for
Stage 1 — the rule is stored and honored on completion, but there are no alarms/notifications.)

## Project structure

```
app/src/main/java/com/example/taskmanager/
├─ TaskManagerApplication.kt      # @HiltAndroidApp
├─ MainActivity.kt                # @AndroidEntryPoint, hosts Compose
├─ domain/model/                  # Task, Category, Priority, RecurrenceRule
├─ data/
│  ├─ local/                      # Room: entities, DAO, Converters, TaskDatabase
│  ├─ mapper/                     # entity <-> domain mapping
│  └─ repository/                 # TaskRepository + impl (template/instance logic)
├─ di/                            # Hilt modules (Database, Repository)
├─ util/DateUtils.kt             # java.time date helpers + DatePicker interop
└─ ui/
   ├─ theme/                      # Material 3 theme, colors, type
   ├─ navigation/                 # routes, bottom nav, NavGraph
   ├─ components/                 # reusable composables (TaskListItem, chips, etc.)
   └─ screens/
      ├─ today/  alltasks/  addedit/  detail/  history/   (Screen + ViewModel each)
```

## Building & running

Open the project in **Android Studio** (Hedgehog or newer) and run the `app` configuration on
an emulator or device (API 26+). Android Studio supplies the Android SDK and its own Gradle.

From the command line (requires the Android SDK installed and `local.properties` pointing at
it via `sdk.dir`, or the `ANDROID_HOME` env var):

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew installDebug       # install on a connected device/emulator
```

The Gradle wrapper (8.2) is included. JDK 17 is required (AGP 8.2.2).
