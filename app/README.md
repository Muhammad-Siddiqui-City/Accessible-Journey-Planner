# Accessible Journey Planner (Android)

UI-only clone of the Lovable web app. All 8 screens with navigation; no API or business logic.

## Open in Android Studio

1. Open the **lovable** folder (project root) in Android Studio.
2. Sync Gradle (File → Sync Project with Gradle Files).
3. Run on emulator or device (minSdk 26).

## Screens

- **MainActivity** – Bottom nav: Home, Journeys, Analytics, Settings.
- **Home** – Search bar (→ Journeys), quick actions (→ Arrivals / Journeys), disruptions, nearby stations (→ Arrivals), transport modes.
- **Journey** – From/To, filters, Find Routes, suggested routes (tap → Route Details), route preview.
- **Live Arrivals** – Full-screen; back, refresh, line pills, arrivals list, stat cards.
- **Route Details** – Full-screen; back, share, bookmark, map, summary, step-by-step, alternatives (tap → same screen), Start Navigation.
- **Analytics** – Stats cards, weekly chart placeholder, time saved placeholder, transport modes, frequent routes, achievement card.
- **Settings** – Accessibility toggles, language, notifications, privacy links, Send feedback (→ Feedback).
- **Feedback** – Rating stars, issue type, comments, quick tags, Submit → thank-you, Return Home.
- **NotFound** – 404, Return to Home.

## Structure

- `ui.main` – MainActivity, bottom nav, tab fragments.
- `ui.home`, `ui.journey`, `ui.analytics`, `ui.settings` – Tab fragments.
- `ui.arrivals`, `ui.routedetails`, `ui.feedback`, `ui.notfound` – Full-screen activities.
- `widgets` – LineBadgeView (optional reusable line badge).

Data is hardcoded; buttons and tabs only navigate or show toasts.
