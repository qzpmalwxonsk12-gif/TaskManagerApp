# Appearance update — 1.3

Open **Profile → Settings → Make it yours**.

- **Studio** is the new default app style: warm ivory / forest palettes, serif headings, outlined soft cards, a progress feature card, and an inset navigation bar. It supports the device's light and dark modes.
- **Original** restores the previous Material You palette, typography, card shape, progress card, and navigation styling.
- Widget appearance is independent of the app: **Paper Garden**, **Neon Night**, **Botanical**, or **Classic**. The selection applies to all placed widgets. The Settings preview uses the same RemoteViews renderer as the launcher, with sample data and disabled actions.
- All three widget pages remain: tasks, done today, and notes. Row completion, quick add, and page switching are preserved. New themes show an overflow count when the widget is too small to show everything.
- Appearance choices live in a separate `appearance` SharedPreferences file. The Room database and task data format are unchanged.

## Main implementation

- `ui/theme/AppearancePreferences.kt`: persistent, independent app/widget selections.
- `ui/theme/StudioTheme.kt`: light/dark palettes and typography.
- `ui/components/StudioProgressCard.kt`: Studio home progress treatment.
- `ui/screens/settings/AppearanceSettings.kt`: selectors and actual widget preview.
- `widget/TaskWidget.kt`, `widget/WidgetPalette.kt`: rendering, palettes, and size-aware row budgets.
- `res/layout/widget_task_classic.xml`: original widget layout retained separately.
- `res/layout/widget_task_styled.xml`, `widget_task_neon.xml`, `widget_styled_row.xml`: themed widget layouts.

## Artwork

`app/src/main/res/drawable-nodpi/widget_paper_art.png` was generated with the built-in imagegen tool. The original generated image is preserved outside the project. Neon and botanical backgrounds are native Android vectors.

Final image prompt:

> Use case: illustration-story. Asset type: production background texture for an Android task list home-screen widget, portrait 1024x1536. Create a beautiful warm ivory paper stationery illustration, flat front-facing, full bleed rectangular sheet with no outside environment or perspective. Very subtle warm cream watercolor paper texture. Center and upper 85 percent must be almost entirely empty pale ivory for dark task text overlaid by app. Only along the bottom 15 percent: tiny adorable chibi sleeping cream kitten and small white bunny nestled among delicate pink cosmos flowers and sage leaves, hand drawn fine muted brown pencil outlines with soft watercolor fill. A few very small botanical sprigs at the extreme top corners occupying under 5 percent width. Sophisticated Japanese stationery, restrained charming detail, soft blush, sage, butter yellow. No lettering, no text, no task list, no icons, no watermark, no UI, no border. Artwork intentionally kept low contrast and at edges so a task list is readable.

## Animation scope

This update uses static widget artwork. Android RemoteViews supports ViewFlipper, which can cycle views automatically with transitions. A small repeating decorative sequence is a possible future addition; arbitrary Compose, video, or game-style animation does not run inside a standard RemoteViews widget. Launcher behavior and battery use need device testing before promising continuous playback.

References: [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews), [ViewFlipper](https://developer.android.com/reference/android/widget/ViewFlipper), [widget update guidance](https://developer.android.com/develop/ui/views/appwidgets/advanced).

## Verification commands

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest --max-workers=1 --no-watch-fs --console=plain
./gradlew.bat :app:assembleRelease :app:lintRelease --max-workers=1 --no-watch-fs --console=plain
```

The Android rendering tests cover saved selections, live theme switching, widget inflation, all widget pages, and multiple widget sizes. They also write rendered component previews to `app/build/appearance-previews`. These are Robolectric renders with sample content, not screenshots from a physical phone or a launcher.

Six tests passed on simulated Android API 35. The widget matrix covers all four themes across three pages and three sizes (36 combinations), plus empty/overflow states and enlarged font settings. The app tests switch styles and select widget themes through the real appearance controls.

The preview gallery is `artifacts/appearance-1.3/preview.html`, with standalone PNGs under its `previews` folder. App overview images compose the real shared components with sample tasks; the Settings panel and widgets use their actual renderers.

On this Windows machine, Gradle encountered cache-directory rename failures. The successful Debug test run used the original Gradle 8.9 wrapper with `--no-daemon --max-workers=1 --no-watch-fs -Dorg.gradle.internal.instrumentation.agent=false`. Completed generated transform entries were copied to their missing final cache locations and hash-verified; source cache entries were retained. The wrapper and security settings were not changed.

Release assembly and full Release lint also passed with those build flags (lint: 0 errors, 66 warnings). The signed APK is `artifacts/appearance-1.3/TaskManager-1.3.apk` (version 1.3, versionCode 4). APK signature verification and ZIP alignment passed, and the signing certificate matches the existing root release APK. The original root APK is unchanged. Build logs, lint details, and test result XMLs are under the artifact's `verification` directory.

No Android device or configured emulator was available. Physical-device installation, OEM launcher behavior, and widget operation across real device sleep/reboot remain untested.
