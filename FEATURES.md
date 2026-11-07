# TaskManager — Feature List

A native Android task-management app (Kotlin · Jetpack Compose · MVVM · Room · Hilt).
Package `com.skai.taskmanager`. Fully offline — **no internet permission**, no cloud sync,
no backend, no ads, no analytics, no third-party SDKs.

This document describes the **current state** of the app (v6 — see `CHANGES.md` for the
version-by-version history).

---

## Tasks — data model

Every task has:
- Unique ID, Title (required), Description (optional)
- Category — Work, Personal, Shopping, Other
- Priority — High, Medium, Low
- **Time of day** (optional, e.g. 1:00 PM) — timed tasks slot into the list at their time
- Due date (optional)
- **Period** assignment (`periodId`, optional) — which part of the day it belongs to; null = "Anytime"
- Completion status + completion timestamp
- **Archived** flag — moved out of Today into History (kept, never deleted)
- **Skipped** flag — the user skipped this occurrence for the day
- **Alert profile** + optional custom sound URI (how its reminder announces itself)
- Recurring flag + recurrence rule (Daily / Weekly / Monthly / Custom every *N* days)
- Optional **focus-session** fields (duration, state, start/end, remaining, DND preference,
  auto-complete, scheduled seconds, warning/end alert profiles)
- Created-at timestamp

### Recurring tasks (template vs. instance)
- A recurring task is split into a **template** (the repeating pattern) and **instances**
  (concrete occurrences) that point back to it.
- Skipping / postponing / completing a single instance never mutates the template.
- Completing (or skipping) a recurring instance rolls the pattern forward by generating the next
  occurrence — exactly one, never a duplicate.

### Smart ordering
Lists are ordered intelligently (`TaskOrdering`):
1. High-priority **untimed** tasks first (urgent things to surface, e.g. "call mom")
2. **Timed** tasks next, in chronological order (1 PM lunch → 2 PM walking → 3 PM work)
3. Everything else after, by priority then title
4. Completed tasks sink to the bottom

### Automatic day rollover
Runs at midnight (alarm), on any app/notification event after the day changes, and via a
15-minute safety-net tick. Idempotent:
- Tasks **completed on a previous day** are archived → they leave Today, stay in History.
- Tasks **planned but not done** are filed into History as **"Missed"**.
- Each **recurring routine** gets exactly one fresh instance at its next occurrence, so
  routines continue and nothing ever appears twice.
- Undated one-off tasks are left alone (they're an ongoing backlog, not a missed obligation).

---

## Focus sessions (Session Tasks)

- Any task can be given a **duration**, turning it into a Session Task with a live countdown.
- Controls from the app **and** the notification: Start · Pause · Resume · **+N/+N/+N** quick
  extensions (durations user-configurable) · Done · Restart · Stop.
- **Do Not Disturb** (optional, per user default): enabled while the session runs and the
  previous state restored when it ends. First use asks with a one-time prompt.
- **"Almost done" warning** at a user-configurable lead time (default 5 min) and an
  **end-of-timer alert** — each with its own per-task alert profile.
- Optional **auto-complete on timer end** if the user doesn't respond.
- Full-screen popups over the lock screen for the warning and completion.
- Completed sessions are recorded to statistics (activity name + focused seconds).

---

## Alerts, reminders & notifications

### Task time reminders
- A task with a time pops up a reminder **at that time**, and **auto-dismisses** after a
  user-set duration (10–600 s, default 2 min).
- Several tasks at the same minute are combined into one reminder.
- Two action buttons:
  - **Skip** — files the task in History as "Skipped"; recurring tasks roll to their next occurrence.
  - **Snooze Xm** — re-reminds after a user-set duration (default 10 min), if still open.

### Alert profiles (per task)
Chosen in Add/Edit (for reminders) and Task Detail (for session warning/end):

| Profile | Behaviour |
|---|---|
| Silent | Notification only |
| Vibrate | A short buzz |
| Chime | Vibration + soft chime *(default)* |
| Melody | Long vibration + a short melody |
| Alarm | Loud alarm, constant vibration until dismissed |
| Custom sound | Any audio file from the device (permission persisted) |

Played by `AlertPlayer` rather than a channel sound (channels can't vary sound per notification).
Dismissing the reminder stops the sound; the loudest profile wins when several fire together.

### Task-list notification ("command center")
- **Collapsed:** current period name + progress fraction + one-line preview.
- **Expanded:** header + progress, **+ Task / + Note** buttons, then compact single-line task rows.
- Tap a row to complete/un-complete it; tap the body to open the app.
- Shows the **current period's** tasks plus Anytime tasks; falls back to
  **"Today's Tasks · No Active Period"** in a schedule gap.
- Brief **transition summary** at each period boundary.
- **Persistent or dismissible** (user setting): pinned by default, or swipeable — in which case a
  cleared list stays cleared and returns alongside the next task reminder.

### Notes reminder (note ping)
- Optional recurring nudge at a **user-set interval** with a **custom message**.
- **Pinned notes are shown inside the notification** (up to 5).
- A **"Pin note"** button opens a small popup to choose pinned notes without opening the app.

### Quick-add popups (don't open the app)
- **+ Task**, **+ Note** and **Pin note** open translucent dialog-only activities, save, and close.
- A task added this way is auto-assigned to the **currently active period**; giving it a duration
  makes it a Session Task.

---

## Home-screen widget

- Resizable: default **2×3**, resizes up to **3×4**; row count adapts to height.
- Styled to mirror the notification (dark rounded card, same row treatment).
- **Switch button** cycles three pages, remembered per widget:
  1. **Tasks** — the current period's list with progress; tap a row to complete
  2. **Done today** — everything completed today; tap to un-complete
  3. **Notes** — all notes, pinned first (📌)
- **Add button is page-specific**: + Task on Tasks, + Note on Notes, hidden on Done.
- Tapping the header opens the app; updates live with the same data as the notification.

---

## Screens

### Today (home)
- Time-of-day **greeting** ("Good morning…") + date in the top bar
- Progress card — "5 of 8 tasks completed", animated bar, percentage, "N to go"
- Active tasks, then a **Completed** section with **Move to history**
- Session tasks render with a live countdown row and a state-aware action button
- FAB to add; friendly empty state

### Add / Edit Task
- Title (inline validation), multi-line description, Category + Priority chips
- **Time picker** (clearable) — picking a time auto-assigns the matching period
- **Reminder alert** picker (appears once the task has a time) incl. custom-sound picker
- **Period** selector, due-date picker (Material 3, clearable)
- Recurring toggle + recurrence selector (Daily / Weekly / Monthly / Custom interval)
- Handles create and edit; can open pre-set to recurring (Timetable) or pre-assigned to a period

### Task Detail
- All task info + complete toggle, Edit, Delete (with confirmation)
- For session tasks: **Session alerts** section to set the warning and end alert profiles

### Notes *(bottom-nav tab)*
- Notes with timestamps; **pin/unpin** (pinned sort first and appear in the note ping)
- Add via popup, delete per note, empty state

### History *(bottom-nav tab)*
- Completed **and** missed/skipped tasks grouped by day as collapsible sections
- Day headers show "N done · N missed"; missed/skipped rows are styled distinctly
- **Search** by title/description; searching auto-expands matching days

### Profile *(bottom-nav tab)*
- Avatar (photo picker, copied into app storage) with ring + camera badge, name headline
- **"Your details"** card — display name and "about you"
- **Customize hub** card with tinted icon rows: **Schedule · Timetable · Statistics · Settings**
- App-version footer

### Schedule *(Profile → Customize)* — the scheduling engine
- Every configured period with name, range, enabled toggle, edit, delete (blocked at one period)
- Reorder (move up/down); per-period editor with time pickers and inline validation
  (name required ≤ 20 chars, end after start, no overlaps)
- Lists tasks assigned to the period, with a shortcut to add one
- Defaults seeded on first launch: Morning 6–12, Afternoon 12–17, Evening 17–20, **Night 20–23:59**

### Timetable *(Profile → Customize)*
- The user's recurring tasks (their routine), ordered by time of day
- Tasks added here are created as recurring automatically

### Statistics *(Profile → Customize)*
- **Daily / Weekly / Monthly** tabs; total focus time card
- Per-activity cards with duration, share percentage and a progress bar

### Settings *(Profile → Customize)*
Grouped into titled cards:
- **Reminders** — task reminders on/off, on-screen duration, snooze duration,
  notes reminder on/off, ping interval, custom message
- **Notifications** — persistent vs. dismissible task list
- **Focus sessions** — DND during sessions (+ permission shortcut), auto-complete on timer end,
  "almost done" warning lead time
- **Quick extensions** — the three extension durations

### Navigation
- Bottom navigation: **Today · Profile · Notes · History**
- Schedule, Timetable, Stats, Settings, Add/Edit and Detail push on top (bottom bar hidden)
- Slide + fade transitions; back-stack/state preserved across tabs

---

## Design & theming
- Material 3 (Material You); dynamic color on Android 12+, brand palette on older devices
- Full dark-mode support (including the launch window)
- Shared design tokens (`AppCardShape`, `appCardColors`) so cards are consistent everywhere
- Priority accents, category/priority/recurrence/time chips, tonal empty-state badges

---

## Architecture / technical
- MVVM + Repository pattern; **Room is the single source of truth**
- Reactive UI via Coroutines + Flow (`StateFlow` per screen)
- Hilt DI throughout, incl. entry points for framework-constructed receivers/widgets
- **`TaskBoard`** — a plain singleton owning the task-list notification, the widgets and every
  alarm (reminders, note pings, period transitions, midnight rollover, keep-alive). Fully
  event-driven: alarms and buttons are handled directly in `NotificationActionReceiver`'s
  `goAsync` window; Room Flow observers keep everything instant while the process is alive.
  **No foreground service** backs the notification.
- **`SessionService` is the app's only foreground service** (`specialUse`) — started when a
  focus session starts, self-stopping when it ends
- Pure, stateless **`SchedulingEngine`** (active period, next boundary, overlap, validation) and
  shared **`TaskFilters`** so the app and notification agree on "today"
- Offline-first Room database, **version 11**, with incremental migrations (v1→v11) preserving data
- Min SDK 26 · Target/Compile SDK 35 · AGP 8.7.3 · Gradle 8.9
- Release signing via an untracked `keystore.properties` + `release-keystore.jks`

### Permissions
`POST_NOTIFICATIONS` (runtime, Android 13+) · `FOREGROUND_SERVICE` /
`FOREGROUND_SERVICE_SPECIAL_USE` (focus sessions only) · `RECEIVE_BOOT_COMPLETED` · `VIBRATE` ·
`USE_FULL_SCREEN_INTENT` (session alerts) · `ACCESS_NOTIFICATION_POLICY` (optional DND).
**No `INTERNET`, no advertising ID.**

### Notification channels
`task_notification_channel` (LOW, silent) · `task_reminder_channel_v2` (HIGH, silent — sound comes
from `AlertPlayer`) · `note_ping_channel` (HIGH) · `session_channel` (LOW) ·
`session_alert_channel` (HIGH)

---

## Explicitly out of scope
- Cloud sync / networking / backend / multi-device
- Drag-and-drop period reordering (uses move up/down to avoid a Compose-version-coupled dependency)
- Internal scrolling inside the notification (shows as many compact rows as the system allows)
- Custom notification/widget themes (e.g. a "notebook" skin) — explored, not built
