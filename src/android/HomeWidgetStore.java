package com.homewidget.plugin;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/** Keeps the latest bill and event data in SharedPreferences so widgets can redraw without the app running. */
final class HomeWidgetStore {

    static final String BILL = "data";
    static final String EVENT = "event";

    private static final String PREFS = "cordova_home_widget";

    private HomeWidgetStore() {}

    static void save(Context context, String key, JSONObject data) {
        prefs(context).edit().putString(key, data.toString()).apply();
    }

    static JSONObject load(Context context, String key) {
        String raw = prefs(context).getString(key, null);
        if (raw == null) {
            return null;
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    static void clear(Context context, String key) {
        prefs(context).edit().remove(key).apply();
    }

    static void clearAll(Context context) {
        prefs(context).edit().remove(BILL).remove(EVENT).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
