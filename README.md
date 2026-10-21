# cordova-plugin-home-widget

A generic Android home screen widget for Cordova and **OutSystems 11** mobile apps.
Your app sends a few fields (title, value, subtitle, badge, progress, color) and the
widget on the phone's home screen redraws itself, even when the app is closed.
Tapping it opens the app and tells it what was tapped.

```
┌──────────────────────────────────────────┐
│▌ [icon] My telecom        Due in 15 days │  header · badge
│▌ October bill                            │  title
│▌ €32,50                    Pay by 5 Nov  │  value · subtitle
│▌ ──────────────────────────────────────  │
│▌ ████████░░░░░░░░░░░░                    │  progress (optional)
│▌ 8.2 of 20 GB used              Pay now  │  footer · footerAction
└──────────────────────────────────────────┘
 ▌ = accent color
```

Version 1.0 supports **Android 6+** (cordova-android 9 or later, which covers
current OutSystems MABS versions). iOS is planned for a later version.

---

## JavaScript API

The plugin is available as `window.HomeWidget` after `deviceready`.

```js
HomeWidget.update({
  header: 'My telecom',
  badge: 'Due in 15 days',
  title: 'October bill',
  value: '€32,50',
  subtitle: 'Pay by 5 Nov',
  progress: 41,               // 0–100, leave out to hide the bar
  footer: '8.2 of 20 GB used',
  footerAction: 'Pay now',
  accentColor: '#E60000',     // #RRGGBB or #AARRGGBB
  tapAction: 'pay-bill'       // any string, returned by onTap()
}, onSuccess, onError);

HomeWidget.clear(onSuccess, onError);                 // back to the empty state
HomeWidget.isPinSupported(function (yes) { ... });    // Android 8+ launchers
HomeWidget.requestPin(onSuccess, onError);            // system "Add widget?" dialog
HomeWidget.getInstalledCount(function (n) { ... });   // copies on the home screen
HomeWidget.onTap(function (tapAction) { ... });       // widget opened the app
```

Every field is optional. A field you leave out (or send empty) is hidden.
Data is saved on the device, so the widget keeps showing it after a reboot.

### Plugin variables

| Variable | Default | What it does |
|---|---|---|
| `WIDGET_LABEL` | `Summary` | Name in the launcher's widget picker |
| `WIDGET_DESCRIPTION` | `Shows your latest summary at a glance` | Description in the picker (Android 12+) |

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

Each one has a single **JavaScript** node. OutSystems waits for the returned Promise.

**CheckHomeWidgetPlugin** — output `IsAvailable` (Boolean)

```js
$parameters.IsAvailable = !!(window.cordova && window.HomeWidget);
```

**UpdateHomeWidget** — input `Data` (HomeWidgetData); outputs `Success` (Boolean), `ErrorMessage` (Text)

```js
return new Promise(function (resolve) {
  if (!window.HomeWidget) {
    $parameters.Success = false;
    $parameters.ErrorMessage = "The HomeWidget plugin isn't available on this device.";
    resolve();
    return;
  }
  var d = $parameters.Data;
  window.HomeWidget.update({
    header: d.Header,
    title: d.Title,
    value: d.Value,
    subtitle: d.Subtitle,
    badge: d.Badge,
    footer: d.Footer,
    footerAction: d.FooterAction,
    progress: d.Progress < 0 ? null : d.Progress,
    accentColor: d.AccentColor,
    tapAction: d.TapAction
  }, function () {
    $parameters.Success = true;
    resolve();
  }, function (error) {
    $parameters.Success = false;
    $parameters.ErrorMessage = String(error);
    resolve();
  });
});
```

**ClearHomeWidget** — outputs `Success`, `ErrorMessage`

```js
return new Promise(function (resolve) {
  if (!window.HomeWidget) { $parameters.Success = false; $parameters.ErrorMessage = "Plugin not available."; resolve(); return; }
  window.HomeWidget.clear(
    function () { $parameters.Success = true; resolve(); },
    function (e) { $parameters.Success = false; $parameters.ErrorMessage = String(e); resolve(); });
});
```

**RequestPinHomeWidget** — outputs `Success`, `ErrorMessage`

```js
return new Promise(function (resolve) {
  if (!window.HomeWidget) { $parameters.Success = false; $parameters.ErrorMessage = "Plugin not available."; resolve(); return; }
  window.HomeWidget.requestPin(
    function () { $parameters.Success = true; resolve(); },
    function (e) { $parameters.Success = false; $parameters.ErrorMessage = String(e); resolve(); });
});
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
