package com.homewidget.plugin;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/** Keeps the latest widget data in SharedPreferences so the widget can redraw without the app running. */
final class HomeWidgetStore {

    private static final String PREFS = "cordova_home_widget";
    private static final String KEY_DATA = "data";

    private HomeWidgetStore() {}

    static void save(Context context, JSONObject data) {
        prefs(context).edit().putString(KEY_DATA, data.toString()).apply();
    }

    static JSONObject load(Context context) {
        String raw = prefs(context).getString(KEY_DATA, null);
        if (raw == null) {
            return null;
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY_DATA).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
