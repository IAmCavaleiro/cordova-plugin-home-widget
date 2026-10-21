package com.homewidget.plugin;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** JavaScript bridge: window.HomeWidget.update(), updateEvent(), clear(), requestPin(), onTap() ... */
public class HomeWidget extends CordovaPlugin {

    private CallbackContext tapListener;
    private String pendingTapAction;

    @Override
    protected void pluginInitialize() {
        // The app may have been cold-started by a tap on the widget.
        captureTap(cordova.getActivity().getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        // The app was already running and the widget brought it to the front.
        if (captureTap(intent)) {
            deliverPendingTap();
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) throws JSONException {
        switch (action) {
            case "update":
                save(HomeWidgetStore.BILL, args.optJSONObject(0), callback);
                return true;
            case "updateEvent":
                save(HomeWidgetStore.EVENT, args.optJSONObject(0), callback);
                return true;
            case "clear":
                clear(args.optString(0, ""), callback);
                return true;
            case "isPinSupported":
                callback.success(isPinSupported() ? 1 : 0);
                return true;
            case "requestPin":
                requestPin(args.optString(0, BaseWidgetProvider.KIND_BILL), callback);
                return true;
            case "getInstalledCount":
                callback.success(BaseWidgetProvider.installedCount(context()));
                return true;
            case "registerTapListener":
                tapListener = callback;
                PluginResult noResult = new PluginResult(PluginResult.Status.NO_RESULT);
                noResult.setKeepCallback(true);
                callback.sendPluginResult(noResult);
                deliverPendingTap();
                return true;
            default:
                return false;
        }
    }

    private void save(final String key, final JSONObject data, final CallbackContext callback) {
        if (data == null) {
            callback.error("The widget data must be an object");
            return;
        }
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HomeWidgetStore.save(context(), key, data);
                    BaseWidgetProvider.refreshAll(context());
                    callback.success();
                } catch (Exception e) {
                    callback.error("Could not update the widget: " + e.getMessage());
                }
            }
        });
    }

    /** what: "bill", "event", or empty for everything (use that on logout). */
    private void clear(final String what, final CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if ("bill".equals(what)) {
                        HomeWidgetStore.clear(context(), HomeWidgetStore.BILL);
                    } else if ("event".equals(what)) {
                        HomeWidgetStore.clear(context(), HomeWidgetStore.EVENT);
                    } else {
                        HomeWidgetStore.clearAll(context());
                    }
                    BaseWidgetProvider.refreshAll(context());
                    callback.success();
                } catch (Exception e) {
                    callback.error("Could not clear the widget: " + e.getMessage());
                }
            }
        });
    }

    private boolean isPinSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        return AppWidgetManager.getInstance(context()).isRequestPinAppWidgetSupported();
    }

    private void requestPin(String kind, CallbackContext callback) {
        if (!BaseWidgetProvider.isKnownKind(kind)) {
            callback.error("Unknown widget type '" + kind + "'. Use bill, billSmall, event, countdown or eventSmall.");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !isPinSupported()) {
            callback.error("This launcher doesn't let apps add widgets. Long-press the home screen and choose Widgets instead.");
            return;
        }
        ComponentName provider = new ComponentName(context(), BaseWidgetProvider.providerFor(kind));
        boolean shown = AppWidgetManager.getInstance(context()).requestPinAppWidget(provider, null, null);
        if (shown) {
            callback.success();
        } else {
            callback.error("The launcher refused the request.");
        }
    }

    private boolean captureTap(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(BaseWidgetProvider.EXTRA_FROM_WIDGET, false)) {
            return false;
        }
        String action = intent.getStringExtra(BaseWidgetProvider.EXTRA_TAP_ACTION);
        pendingTapAction = action == null ? "" : action;
        // Consume the extras so a screen rotation or restart doesn't report the tap twice.
        intent.removeExtra(BaseWidgetProvider.EXTRA_FROM_WIDGET);
        intent.removeExtra(BaseWidgetProvider.EXTRA_TAP_ACTION);
        return true;
    }

    private void deliverPendingTap() {
        if (tapListener == null || pendingTapAction == null) {
            return;
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK, pendingTapAction);
        result.setKeepCallback(true);
        tapListener.sendPluginResult(result);
        pendingTapAction = null;
    }

    private Context context() {
        return cordova.getActivity().getApplicationContext();
    }
}
