# Changes

Version history lives in git: `v1` (original app) → `v2` (fixes + reminders) → `v3`
(pinned notes + alarm system) → `v4` (skip/snooze, dismissible notification, widget)
→ `v5` (Play-compliant architecture) → `v6` (UI overhaul).
Revert to any version with `git reset --hard <tag>`.

---

# Version 6 — UI overhaul

No functional changes — purely visual, aimed at "production app" rather than "test build".

- **Profile** (the most-improved screen): the flat expandable "Customize" list is now a
  proper settings hub — avatar with a tinted ring and camera badge, the user's name as a
  headline, a "Your details" card for editing, and a hub card whose rows have colored
  circular icon chips (Schedule/Timetable/Statistics/Settings each get their own tone),
  inset dividers and chevrons. An app-version footer rounds it out.
- **Settings**: the bare stack of switches/fields is grouped into four titled cards —
  Reminders, Notifications, Focus sessions, Quick extensions.
- **Today**: the header now greets you ("Good morning / afternoon / evening / night")
  above the date, left-aligned.
- **Empty states**: icons now sit in a soft tonal circle instead of floating loose.
- **Statistics**: activity rows use the shared card style and gained a per-activity
  progress bar; the total card matches the app's rounded look.

---

# Version 5 — Play-policy compliance: no more dashboard foreground service

**What the user sees: nothing changes.** Same notification, same buttons, same widget,
same reminders. What changed is *how* it runs.

## Why
Google Play scrutinizes `FOREGROUND_SERVICE_SPECIAL_USE`. The task-list notification was
backed by a permanently running foreground service that spent ~99% of its time idle —
exactly the pattern Play policy rejects. The focus-session timer, by contrast, is a
sanctioned FGS use case (a user-started, user-visible countdown).

## What changed
- **`TaskNotificationService` (permanent FGS) deleted.** Its entire job moved to a new
  `notification/TaskBoard.kt` — a plain singleton that renders the notification/widgets
  and manages every alarm, invoked event-by-event:
  - alarms & notification buttons → `NotificationActionReceiver` handles them directly
    in its ~10-second window (the work takes milliseconds) — no service start;
  - while the process is alive (app open, or cached after any event), Room Flow
    observers started from the Application class keep everything instantly in sync;
  - after boot, `BootReceiver` re-arms all alarms (with a fresh note-ping countdown)
    and re-posts the notification.
- **`SessionService` is now the app's only foreground service** — started when a focus
  session starts, self-stopping when it ends. For the Play declaration form, the
  `specialUse` case is: *"countdown timer notification for a user-started focus
  session, with pause/extend/complete controls and optional Do Not Disturb."*
- Notifications persist independently of any process, so nothing is lost when Android
  reclaims memory: the next alarm, button press, or app open re-renders everything.
- versionCode 3 / versionName 1.2; both `taskmanager-release.apk` and
  `taskmanager-release.aab` are built signed at the project root.

## Honest trade-offs
- On aggressive OEM builds, if the cached process is killed, a data change made *only*
  by the system clock (nothing else happening) can wait for the next alarm (at most the
  15-minute keep-alive) instead of updating instantly. Every user-visible interaction
  still updates immediately, because the interaction itself wakes the process.
- The task-reminder alert sound now plays from a cached (not foreground) process; its
  looping "Alarm" profile is capped by the reminder's on-screen duration as before, but
  an extremely memory-starved device could cut a long alarm short.

---

# Version 4 — Skip/Snooze, dismissible notification, home-screen widget

## 1. Skip & Snooze buttons on task reminders ⏭💤
Every task-reminder pop-up now carries two buttons:
- **Skip** — the task is skipped for the day: it leaves Today immediately and is filed
  in History with a "Skipped" label (distinct from "Missed"). A recurring task gets its
  next occurrence automatically (tomorrow for daily rules), so the routine continues.
- **Snooze Xm** — the reminder goes away and pops up again after your snooze duration
  (Settings → Reminders → **Snooze duration**, default 10 minutes). Snoozing keeps the
  task's own alert sound/vibration profile, and only re-reminds if the task is still open.

## 2. Dismissible task-list notification (optional)
Settings → Reminders → **Persistent task list** (on by default — unchanged behavior).
When turned **off**:
- the task-list notification becomes a normal notification you can swipe away
  (including "Clear all");
- it is **not** resurrected by ordinary data changes — cleared stays cleared;
- whenever a task reminder fires, the task list arrives right alongside it, as requested;
- reminders, note pings and the midnight rollover keep working exactly the same.

## 3. Home-screen widget 📱
A resizable widget (default **2×3**, resizes up to **3×4**) that mirrors the persistent
notification's look (dark rounded card, same rows):
- **Switch button (▸)** cycles three pages, remembered per widget:
  1. **Tasks** — the current period's list with the same "3/7" progress; tapping a row
     completes the task (and un-completes on the Done page);
  2. **Done today** — everything completed today;
  3. **Notes** — all notes, pinned first (📌).
- **Add button** is page-specific: "+ Task" on the tasks page, "+ Note" on the notes
  page (hidden on Done). Both open the same quick-add popups as the notification.
- Tapping the header opens the app.
- The widget updates live whenever tasks/notes change (driven by the same data flows as
  the notification), adapts its row count to its size, and each widget remembers its own
  page independently.

## 4. Schedule fix: 22:00 tasks were outside every period 🐛
The default schedule was Morning 6–12, Afternoon 12–17, Evening 17–20, **Night 20–22** —
and periods are half-open, so a 22:00 task fell into the 22:00–midnight *gap* and was
treated as "Anytime". Fixes:
- The default **Night period now runs 20:00–23:59** (fresh installs, and a migration
  repairs the untouched default on existing installs — a customized Night is left alone).
- Fixed a save-time race where a quickly saved timed task could get no period because
  the period list hadn't loaded yet; the period is now resolved against the database
  directly when needed.

## 5. Under the hood
- DB migration **v11**: `skipped` on tasks, `snoozeMinutes` + `persistentTaskList` in
  settings, Night-period repair.
- Fixed a v3 oversight: dismissing a reminder didn't actually stop an in-progress
  alert sound (the stop signal was never routed) — it does now.
- Completing a task from the notification/widget now nudges the service so both refresh
  even if the system had killed it.
- New files: `widget/TaskWidget.kt`, `widget/TaskWidgetProvider.kt`, widget layout/
  drawables/provider-info resources.

---

# Version 3 — Pinned notes & the alarm/chime system

## 1. Pin a note 📌
- Every note card now has a **pin button**; pinned notes sort to the top with a
  "Pinned" label.
- The periodic **notes reminder now shows your pinned notes right inside the
  notification** (up to 5, each on its own line).
- The notification also has a **"Pin note" button** that opens a small popup —
  without opening the app — where you can tick/untick which notes are pinned.

## 2. Task alarms/chimes 🔔
Every timed task can choose how its reminder announces itself ("Reminder alert"
section in Add/Edit Task, shown once the task has a time):

1. **Vibrate** — a small vibration.
2. **Chime** — vibration plus a soft chime *(default)*.
3. **Melody** — a long vibration and a short part of a song (your device's ringtone melody).
4. **Alarm** — full-on alarm sound with constant vibration until you dismiss it.
5. **Custom sound** — pick any audio file from your phone (permission is persisted,
   so it keeps working after reboots).
6. **Silent** — notification only.

Details:
- Swiping the reminder away (or its auto-dismiss timeout) also stops the sound and
  vibration; an Alarm-profile sound is capped at the reminder's on-screen duration.
- If several tasks fire at the same minute, the **strongest profile wins**.
- Sounds are played by a new `AlertPlayer` (not the notification channel), because
  Android channels can't vary sound per task. The reminder channel itself is silent.

## 3. Session task alarms ⏱🔔
- The **5-minute warning** and the **timer-end alert** of every session task are now
  individually customizable with the same six profiles (Task Details → "Session
  alerts", shown for session tasks). Defaults: warning = Vibrate, end = Chime.
- The end alert rings whether or not you're looking at the phone; the Alarm profile
  keeps ringing until you act on the popup (Done / extend / restart / stop) or a
  2-minute cap.
- The warning lead time itself is configurable too: Settings → Focus sessions →
  **"Almost done" warning (minutes before end)** — the old hardcoded 5 minutes is
  now just the default.

## 4. Under the hood
- DB migration **v10**: alert columns on tasks, `pinned` on notes,
  `sessionWarningMinutes` in settings. Existing data preserved.
- New files: `domain/model/TaskAlert.kt`, `notification/AlertPlayer.kt`,
  `ui/components/AlertPicker.kt`.
- The old fixed-sound reminder channel is deleted and replaced by a silent one
  (`task_reminder_channel_v2`); per-task sounds come from `AlertPlayer`.

---

# Version 2 — fixes, reminders, UI polish

All changes made on top of the original app (tagged `v1` in git).
**To revert everything back to the previous version:** `git reset --hard v1`
**To come back to this version afterwards:** `git reset --hard v2`

No existing functionality was removed — the persistent notification, focus sessions,
schedule periods, timetable, stats, notes and quick-add all work exactly as before.

---

## 1. Bug fixes / logic corrections

### Automatic day rollover (the "tasks show up double / never go to history" bug)
The app previously had **no** automatic end-of-day handling: completed tasks only left
"Today" via the manual *Move to history* button, unfinished tasks stayed on the Today list
forever as "overdue", and a recurring task could end up with **two visible copies** the next
day (e.g. yesterday's leftover instance next to a newly generated one).

New behavior — at midnight (and whenever the app/notification wakes up after midnight):
- Tasks **completed yesterday** are archived automatically: they leave Today but stay in
  History under their day. Nothing is deleted.
- Tasks that were **planned for yesterday but not done** are filed into History as
  **"Missed"** (shown with their own icon and a "missed" label).
- Each **recurring task** whose instance was missed gets exactly **one** fresh instance at
  its next occurrence (daily → today, weekly → keeps its weekly cadence). Stale duplicates
  are archived first, and a template that already has an upcoming instance is skipped —
  so a task can never appear twice.
- Undated one-off tasks (your general backlog) are intentionally left alone.

The rollover is idempotent and triggered from three places so it can't be missed:
a dedicated midnight alarm, the persistent notification's 15-minute keep-alive (safety
net if the alarm is delayed by Doze), and the Today screen's own midnight ticker.

Files: `TaskRepository.kt`, `TaskRepositoryImpl.kt`, `TaskDao.kt`, `TodayViewModel.kt`,
`TaskNotificationService.kt`, `NotificationActionReceiver.kt`, `NotificationActions.kt`.

### Recurring session tasks carried stale session state to the next day
When a recurring *session* (focus) task was completed, the auto-generated next occurrence
copied the old instance's session fields — so tomorrow's task could be born already in the
"FINISHED" state with a stale timer. New instances now start fully reset (IDLE, no timer,
not archived). The next occurrence's due date is also normalized to start-of-day so it can
never drift into the middle of a day.

### Today screen vs. persistent notification disagreed on "today"
The Today screen counted overdue tasks as today's tasks; the notification only counted
tasks dated exactly today, so the two could show different lists and different progress
numbers. There is now a single shared definition (`domain/scheduling/TaskFilters.kt`)
used by both. The Today screen's active list also now excludes archived tasks (it didn't
before — archived-but-unfinished tasks would have reappeared).

### History screen
- Now includes missed tasks (grouped under the day they were planned for) alongside
  completed ones; day headers show "3 done · 1 missed".
- Missed rows get a distinct icon and no strikethrough, so they're not confused with
  completed work.

### Small consistency fixes
- The session "5 minutes remaining" popup text was hardcoded; it now derives from the
  actual warning-threshold constant, so changing the threshold can't silently desync it.

## 2. Duplicate code removed
- The card background styling (`surfaceVariant.copy(alpha = 0.4f)`) was copy-pasted in
  `TaskListItem`, `SessionTaskRow` and the Notes card → extracted to a shared
  `ui/components/Cards.kt` (`appCardColors()` + `AppCardShape`), which also gives every
  card the same rounded shape.
- `DateUtils.formatCompletedLabel` duplicated the Today/Yesterday logic of
  `formatDayLabel` → now reuses it.
- `TodayScreen` had a pointless private `TaskRow` wrapper → removed.
- `AddEditTaskViewModel` derived "which period contains this time" twice (once in
  `onTimeChange`, once in `save`) → extracted to one `periodForTime()` helper.
- `TaskNotificationService` built near-identical broadcast PendingIntents in four places
  → one `broadcastPendingIntent()` helper.
- The old per-file duplicated "is planned for today / completed today" predicates
  (`TodayViewModel`, `NotificationContent`) → one `TaskFilters` object.

## 3. New: task time reminders 🎉
For any task with a time of day (e.g. *Exercise* at 9:00 AM), a reminder notification now
**pops up at that exact time** and **disappears by itself** after a configurable duration
(default: 2 minutes).

- Uses its own high-importance channel ("Task reminders") and its own notification id, so
  it **never touches the persistent task-list notification** — that one stays silent,
  pinned and unchanged.
- If several tasks share the same time (or an alarm is delivered late), they're combined
  into one reminder ("9:00 AM — 2 tasks due").
- Completing or editing a task automatically reschedules the pending reminder.
- Survives reboots (rescheduled when the notification service restarts on boot).

## 4. New: periodic notes ping 🎉
An optional recurring notification that reminds you to look at your notes:
- **You choose the interval** (e.g. every 30 minutes) and **the message** (default
  "Check your notes", fully customizable).
- Off by default; toggle it in Settings.
- Auto-dismisses after a minute, uses its own channel/id — again fully separate from the
  persistent notification.
- Task edits or other data changes never reset its countdown; only changing its own
  settings does.

## 5. New Settings section ("Reminders")
Settings now has a Reminders section on top:
- **Task time reminders** on/off.
- **Reminder stays on screen (seconds)** — how long the task pop-up lives (10–600 s).
- **Notes reminder** on/off.
- **Ping every (minutes)** — the notes interval.
- **Reminder message** — the custom notes prompt.

Stored in the settings table via a new DB migration (v8 → v9) — existing data is preserved.

## 6. UI cleanup
- One consistent card style (same rounded corners + tint) across Today, Notes and session
  rows instead of three slightly different ones.
- Section headers ("TO DO", "COMPLETED") restyled as clean uppercase labels.
- Progress card: rounded shape, bigger percentage, and a "N to go / All done — nice work!"
  line under the bar.
- History: clearer day headers (done/missed counts) and distinct styling for missed tasks.
- Settings reorganized into titled sections (Reminders / Focus sessions).
- Profile → Settings entry subtitle updated to mention reminders.

## 7. Versioning / how to revert
The project is now a git repository:
- `v1` = the app exactly as it was before any of these changes.
- `v2` = everything described above.

| I want to… | Command |
|---|---|
| Go back to the original version | `git reset --hard v1` |
| Go to version 2 | `git reset --hard v2` |
| Go to version 3 | `git reset --hard v3` |
| Go to version 4 | `git reset --hard v4` |
| Go to version 5 (latest) | `git reset --hard v5` |
| See every change in detail | `git diff v1 v5` |

> Note: the new version bumps the database to v9. Reverting the *code* to v1 and
> reinstalling over a device that already ran v2 will make Room recreate the database
> (the project has destructive-migration fallback enabled), which clears stored tasks on
> that device. Reverting before installing v2 on a device has no such effect.
