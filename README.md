# cordova-plugin-home-widget

Android home screen widgets for Cordova and **OutSystems 11** mobile apps.
Your app sends a bill and/or an event, and the widgets on the phone's home screen
redraw themselves, even when the app is closed. Tapping a widget opens the app and
tells it what was tapped.

The launcher's widget picker offers five widgets; the user adds the ones they like:

| Widget in the picker | Type (`requestPin`) | Shows |
|---|---|---|
| Bill | `bill` | Caption, big amount, due line, pay button |
| Bill (small), 2x1 | `billSmall` | Title, amount, due line |
| Event | `event` | Date tile (month / day / weekday), title, time, location, "In 15 days" |
| Event countdown | `countdown` | Days / hours / minutes boxes and the date |
| Event (small), 2x1 | `eventSmall` | Title, "In 15 days", date and time |

```
Bill                                   Event
┌───────────────────────────────┐      ┌─────────────────────────────────┐
│ My telecom · October bill     │      │ ┌─NOV─┐ Product launch           │
│ €32,50                        │      │ │ 05  │ 18:30                    │
│ Due in 15 days · 5 Nov        │      │ │ Wed │ Lisbon Congress Centre   │
│ (        Pay now          )   │      │ └─────┘ In 15 days               │
└───────────────────────────────┘      └─────────────────────────────────┘
```

Event widgets refresh themselves every 30 minutes (the most Android allows), so
"In N days" stays right and the countdown's minutes can lag by up to half an hour.

Supports **Android 6+** (cordova-android 8 or later, including OutSystems MABS 11).
iOS is planned for a later version.

---

## JavaScript API

The plugin is available as `window.HomeWidget` after `deviceready`.

```js
// Bill: shown by "Bill" and "Bill (small)". Every field is optional; empty fields are hidden.
HomeWidget.update({
  header: 'My telecom',
  title: 'October bill',
  value: '€32,50',
  badge: 'Due in 15 days',    // due line, part 1
  subtitle: '5 Nov',          // due line, part 2
  footerAction: 'Pay now',    // button text; leave out for no button
  accentColor: '#E60000',     // #RRGGBB or #AARRGGBB
  tapAction: 'pay-bill'       // any string, returned by onTap()
}, onSuccess, onError);

// Event: shown by "Event", "Event countdown" and "Event (small)". title and start are required.
HomeWidget.updateEvent({
  title: 'Product launch',
  start: '2026-11-05T18:30:00', // Date, milliseconds, or a date string (no zone = phone's local time)
  location: 'Lisbon Congress Centre',
  accentColor: '#5B3CC4',
  tapAction: 'open-event'
}, onSuccess, onError);

HomeWidget.clear(onSuccess, onError);                 // remove bill and event (use on logout)
HomeWidget.clear('event', onSuccess, onError);        // or only 'bill' / 'event'
HomeWidget.isPinSupported(function (yes) { ... });    // Android 8+ launchers
HomeWidget.requestPin('countdown', onSuccess, onError); // "Add widget?" dialog; type from the table above
HomeWidget.getInstalledCount(function (n) { ... });   // widgets of all types on the home screen
HomeWidget.onTap(function (tapAction) { ... });       // a widget opened the app
```

Data is saved on the device, so the widgets keep showing it after a reboot.

### Plugin variables

| Variable | Default | What it does |
|---|---|---|
| `WIDGET_LABEL` | `Bill` | Name of the bill widgets in the picker (the small one gets " (small)") |
| `WIDGET_DESCRIPTION` | `Your latest bill at a glance` | Description of the bill widgets (Android 12+) |
| `EVENT_LABEL` | `Event` | Name of the event widgets (" countdown" and " (small)" are added) |
| `EVENT_DESCRIPTION` | `Your next event at a glance` | Description of the event widgets (Android 12+) |

---

## Using it in OutSystems 11

### 1. Put the plugin in a Git repository

Push this folder to GitHub (public, or private with an access token in the URL)
and create a tag:

```bash
git init && git add . && git commit -m "HomeWidget 1.0.0"
git tag 1.0.0
git remote add origin https://github.com/<you>/cordova-plugin-home-widget.git
git push origin main --tags
```

### 2. Create the plugin wrapper module

In Service Studio, create a new **Mobile** app (or add a module to an existing one)
called `HomeWidgetPlugin`. Open the module properties and set
**Extensibility Configurations** to:

```json
{
  "plugin": {
    "url": "https://github.com/<you>/cordova-plugin-home-widget.git#1.0.0",
    "variables": [
      { "name": "WIDGET_LABEL", "value": "Bill" },
      { "name": "WIDGET_DESCRIPTION", "value": "Your latest bill at a glance" }
    ]
  }
}
```

### 3. Add a structure

`HomeWidgetData` (public):

| Attribute | Type | Notes |
|---|---|---|
| Header | Text | |
| Title | Text | |
| Value | Text | |
| Subtitle | Text | |
| Badge | Text | |
| Footer | Text | |
| FooterAction | Text | |
| Progress | Integer | Default `-1` = hide the bar |
| AccentColor | Text | `#RRGGBB` |
| TapAction | Text | |

### 4. Add public client actions

Each one has a single **JavaScript** node. The plugin answers asynchronously, so every
callback must end with **`$resolve()`**: that is what makes OutSystems wait. Don't return
your own `new Promise(...)`; OutSystems 11 doesn't wait for it, and the outputs come back
empty (`Success = False`, no `ErrorMessage`).

**CheckHomeWidgetPlugin** — output `IsAvailable` (Boolean)

```js
$parameters.IsAvailable = !!(window.cordova && window.HomeWidget);
```

**UpdateHomeWidget** — input `Data` (HomeWidgetData); outputs `Success` (Boolean), `ErrorMessage` (Text).
Give the JavaScript node one Text input per field (`Header` = `Data.Header`, …) and an Integer
input `Progress` = `Data.Progress`, then copy its `Success` / `ErrorMessage` outputs to the action's.

```js
if (!window.HomeWidget) {
    $parameters.Success = false;
    $parameters.ErrorMessage = 'The HomeWidget plugin is not available on this device.';
    $resolve();
    return;
}
window.HomeWidget.update({
    header: $parameters.Header,
    title: $parameters.Title,
    value: $parameters.Value,
    subtitle: $parameters.Subtitle,
    badge: $parameters.Badge,
    footer: $parameters.Footer,
    footerAction: $parameters.FooterAction,
    progress: $parameters.Progress < 0 ? null : $parameters.Progress,
    accentColor: $parameters.AccentColor,
    tapAction: $parameters.TapAction
}, function () {
    $parameters.Success = true;
    $resolve();
}, function (error) {
    $parameters.Success = false;
    $parameters.ErrorMessage = String(error || 'Unknown error from the HomeWidget plugin.');
    $resolve();
});
```

**ClearHomeWidget** — outputs `Success`, `ErrorMessage`

```js
if (!window.HomeWidget) { $parameters.Success = false; $parameters.ErrorMessage = 'Plugin not available.'; $resolve(); return; }
window.HomeWidget.clear(
    function () { $parameters.Success = true; $resolve(); },
    function (e) { $parameters.Success = false; $parameters.ErrorMessage = String(e); $resolve(); });
```

**RequestPinHomeWidget** — outputs `Success`, `ErrorMessage`

```js
if (!window.HomeWidget) { $parameters.Success = false; $parameters.ErrorMessage = 'Plugin not available.'; $resolve(); return; }
window.HomeWidget.requestPin(
    function () { $parameters.Success = true; $resolve(); },
    function (e) { $parameters.Success = false; $parameters.ErrorMessage = String(e); $resolve(); });
```

### 5. Add a block for taps

Create a public block **HomeWidgetTapListener** with an event `OnTap`
(input `TapAction`, Text). Add a client action `RaiseOnTap` (input `TapAction`)
that triggers the event, and in the block's **OnReady** add a JavaScript node:

```js
if (window.HomeWidget) {
  window.HomeWidget.onTap(function (tapAction) {
    $actions.RaiseOnTap(tapAction);
  });
}
```

Put the block once in your app's **Layout** (or the first screen), and in the
`OnTap` handler navigate based on `TapAction`, for example `"pay-bill"` →
the payment screen.

### 6. Use it in your app

1. Reference `HomeWidgetPlugin` from your mobile app (Manage Dependencies).
2. After you fetch the user's data (for example the latest bill), call
   `UpdateHomeWidget` with a `HomeWidgetData` record.
3. Optionally add an **Add to home screen** button that calls `RequestPinHomeWidget`.
4. On logout, call `ClearHomeWidget` so no personal data stays on the home screen.
5. Generate the Android build in Service Studio (Distribute → Android App),
   install it, long-press the home screen, choose **Widgets**, and drag in yours.

---

## Testing without OutSystems

```bash
npm install -g cordova
cordova create hwtest com.example.hwtest HWTest
cd hwtest
cordova platform add android
cordova plugin add ../cordova-plugin-home-widget --variable WIDGET_LABEL="Bill"
cordova run android
```

Then call `HomeWidget.update({...})` from `www/js/index.js` after `deviceready`,
or from Chrome DevTools (`chrome://inspect`) while the app runs.

---

## How it works

- `HomeWidget.update()` saves the data as JSON in SharedPreferences and asks
  `AppWidgetManager` to redraw every copy of the widget.
- `HomeWidgetProvider` builds the widget with `RemoteViews` from that saved data,
  so it works when the app isn't running and after a reboot.
- Resources are looked up by name (`getIdentifier`), because the plugin is compiled
  into apps with different package names.
- A tap launches the app with an extra holding `tapAction`. The plugin delivers it
  to `onTap()`, whether the app was cold-started or already running.
- Light and dark mode colors are in `res/android/values` and `values-night`.

## Known limitations (1.0)

- Android only.
- The widget updates when the app calls `update()`. To refresh it while the app
  is closed, a later version can add a data-only push message or WorkManager job.
- One layout and one data set per app; every copy of the widget shows the same data.
- `progress` uses the default accent color from `hw_colors.xml`; `accentColor`
  tints the bar on the left, the badge text and the footer action.

## Roadmap ideas

- iOS (WidgetKit extension + App Group)
- Small (2×1) layout
- Background refresh from a push message
- OutSystems Forge component with a demo app
