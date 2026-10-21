package com.homewidget.plugin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

/**
 * Draws the widget. Resources are looked up by name rather than through R,
 * because the plugin is compiled into apps with different package names.
 */
public class HomeWidgetProvider extends AppWidgetProvider {

    static final String EXTRA_TAP_ACTION = "com.homewidget.plugin.TAP_ACTION";
    static final String EXTRA_FROM_WIDGET = "com.homewidget.plugin.FROM_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, buildViews(context));
        }
    }

    /** Redraws every copy of the widget. Called by the plugin after the data changes. */
    static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, HomeWidgetProvider.class));
        if (ids == null || ids.length == 0) {
            return;
        }
        RemoteViews views = buildViews(context);
        for (int id : ids) {
            manager.updateAppWidget(id, views);
        }
    }

    static int installedCount(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, HomeWidgetProvider.class));
        return ids == null ? 0 : ids.length;
    }

    static RemoteViews buildViews(Context context) {
        String pkg = context.getPackageName();
        RemoteViews views = new RemoteViews(pkg, res(context, "layout", "hw_home_widget"));
        JSONObject data = HomeWidgetStore.load(context);

        int appIcon = context.getApplicationInfo().icon;
        if (appIcon != 0) {
            views.setImageViewResource(id(context, "hw_icon"), appIcon);
        }

        if (data == null) {
            String appName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
            setText(context, views, "hw_header", appName);
            setText(context, views, "hw_title", null);
            setText(context, views, "hw_value", null);
            setText(context, views, "hw_subtitle", null);
            setText(context, views, "hw_badge", null);
            setText(context, views, "hw_footer", null);
            setText(context, views, "hw_footer_action", null);
            setText(context, views, "hw_empty", "Open the app to load your data");
            views.setViewVisibility(id(context, "hw_progress"), View.GONE);
            views.setViewVisibility(id(context, "hw_divider"), View.GONE);
            views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, ""));
            return views;
        }

        setText(context, views, "hw_empty", null);
        setText(context, views, "hw_header", data.optString("header", null));
        setText(context, views, "hw_title", data.optString("title", null));
        setText(context, views, "hw_value", data.optString("value", null));
        setText(context, views, "hw_subtitle", data.optString("subtitle", null));
        setText(context, views, "hw_badge", data.optString("badge", null));
        setText(context, views, "hw_footer", data.optString("footer", null));
        setText(context, views, "hw_footer_action", data.optString("footerAction", null));

        boolean hasFooter = data.has("footer") || data.has("footerAction") || data.has("progress");
        views.setViewVisibility(id(context, "hw_divider"), hasFooter ? View.VISIBLE : View.GONE);

        int progressId = id(context, "hw_progress");
        if (data.has("progress")) {
            views.setViewVisibility(progressId, View.VISIBLE);
            views.setProgressBar(progressId, 100, data.optInt("progress", 0), false);
        } else {
            views.setViewVisibility(progressId, View.GONE);
        }

        int accent = parseColor(data.optString("accentColor", null));
        if (accent != 0) {
            views.setInt(id(context, "hw_accent_bar"), "setColorFilter", accent);
            views.setTextColor(id(context, "hw_badge"), accent);
            views.setTextColor(id(context, "hw_footer_action"), accent);
        }

        views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, data.optString("tapAction", "")));
        return views;
    }

    private static PendingIntent launchIntent(Context context, String tapAction) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            intent = new Intent();
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_FROM_WIDGET, true);
        intent.putExtra(EXTRA_TAP_ACTION, tapAction == null ? "" : tapAction);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 7310, intent, flags);
    }

    private static void setText(Context context, RemoteViews views, String name, String value) {
        int viewId = id(context, name);
        if (value == null || value.isEmpty()) {
            views.setViewVisibility(viewId, View.GONE);
        } else {
            views.setViewVisibility(viewId, View.VISIBLE);
            views.setTextViewText(viewId, value);
        }
    }

    private static int parseColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0;
        }
        try {
            return Color.parseColor(hex);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    private static int id(Context context, String name) {
        return res(context, "id", name);
    }

    private static int res(Context context, String type, String name) {
        Resources r = context.getResources();
        int value = r.getIdentifier(name, type, context.getPackageName());
        if (value == 0) {
            throw new IllegalStateException("HomeWidget resource missing: " + type + "/" + name);
        }
        return value;
    }
}
