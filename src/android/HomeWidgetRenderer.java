package com.homewidget.plugin;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Builds the RemoteViews for each widget kind. Resources are looked up by name rather than
 * through R, because the plugin is compiled into apps with different package names.
 */
final class HomeWidgetRenderer {

    private static final long MINUTE = 60L * 1000L;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;
    /** " - " with a middle dot (U+00B7), built from its code so the source stays plain ASCII. */
    private static final String SEPARATOR = " " + (char) 0xB7 + " ";

    private HomeWidgetRenderer() {}

    static RemoteViews build(Context context, String kind) {
        if (BaseWidgetProvider.KIND_BILL_SMALL.equals(kind)) {
            return bill(context, kind, "hw_bill_small", true);
        }
        if (BaseWidgetProvider.KIND_EVENT.equals(kind)) {
            return event(context, kind);
        }
        if (BaseWidgetProvider.KIND_COUNTDOWN.equals(kind)) {
            return countdown(context, kind);
        }
        if (BaseWidgetProvider.KIND_EVENT_SMALL.equals(kind)) {
            return eventSmall(context, kind);
        }
        return bill(context, BaseWidgetProvider.KIND_BILL, "hw_bill", false);
    }

    // ---- Bill (layout B) and small bill ----

    private static RemoteViews bill(Context context, String kind, String layout, boolean small) {
        RemoteViews views = new RemoteViews(context.getPackageName(), res(context, "layout", layout));
        JSONObject data = HomeWidgetStore.load(context, HomeWidgetStore.BILL);

        if (data == null) {
            setText(context, views, "hw_caption", appName(context));
            setText(context, views, "hw_value", null);
            setText(context, views, "hw_due", null);
            setText(context, views, "hw_empty", "Open the app to load your bill");
            if (!small) {
                views.setViewVisibility(id(context, "hw_button"), View.GONE);
            }
            views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, kind, ""));
            return views;
        }

        int accent = accent(context, data);
        String header = data.optString("header", "");
        String title = data.optString("title", "");

        setText(context, views, "hw_empty", null);
        setText(context, views, "hw_caption", small ? firstNonEmpty(title, header) : join(header, title));
        setText(context, views, "hw_value", data.optString("value", ""));
        setText(context, views, "hw_due", join(data.optString("badge", ""), data.optString("subtitle", "")));
        views.setTextColor(id(context, "hw_due"), accent);

        if (!small) {
            String action = data.optString("footerAction", "");
            if (action.isEmpty()) {
                views.setViewVisibility(id(context, "hw_button"), View.GONE);
            } else {
                views.setViewVisibility(id(context, "hw_button"), View.VISIBLE);
                views.setTextViewText(id(context, "hw_button_text"), action);
                views.setInt(id(context, "hw_button_bg"), "setColorFilter", accent);
            }
        }

        views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, kind, data.optString("tapAction", "")));
        return views;
    }

    // ---- Event date tile (layout E) ----

    private static RemoteViews event(Context context, String kind) {
        RemoteViews views = new RemoteViews(context.getPackageName(), res(context, "layout", "hw_event"));
        JSONObject data = loadEvent(context);

        if (data == null) {
            views.setViewVisibility(id(context, "hw_tile"), View.GONE);
            setText(context, views, "hw_title", appName(context));
            setText(context, views, "hw_time", null);
            setText(context, views, "hw_location", null);
            setText(context, views, "hw_relative", null);
            setText(context, views, "hw_empty", "Open the app to load your event");
            views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, kind, ""));
            return views;
        }

        long start = data.optLong("start");
        Date date = new Date(start);
        int accent = accent(context, data);

        views.setViewVisibility(id(context, "hw_tile"), View.VISIBLE);
        views.setInt(id(context, "hw_tile_top"), "setColorFilter", accent);
        setText(context, views, "hw_month", format("MMM", date).toUpperCase(Locale.getDefault()));
        setText(context, views, "hw_day", format("dd", date));
        setText(context, views, "hw_weekday", format("EEE", date));

        setText(context, views, "hw_empty", null);
        setText(context, views, "hw_title", data.optString("title", ""));
        setText(context, views, "hw_time", time(context, date));
        setText(context, views, "hw_location", data.optString("location", ""));
        setText(context, views, "hw_relative", relative(start));
        views.setTextColor(id(context, "hw_relative"), accent);

        views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, kind, data.optString("tapAction", "")));
        return views;
    }

    // ---- Event countdown (layout F) ----

    private static RemoteViews countdown(Context context, String kind) {
        RemoteViews views = new RemoteViews(context.getPackageName(), res(context, "layout", "hw_countdown"));
        JSONObject data = loadEvent(context);

        if (data == null) {
            setText(context, views, "hw_caption", appName(context));
            views.setViewVisibility(id(context, "hw_boxes"), View.GONE);
            setText(context, views, "hw_footer", null);
            setText(context, views, "hw_empty", "Open the app to load your event");
            views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, kind, ""));
            return views;
        }

        long start = data.optLong("start");
        Date date = new Date(start);
        int accent = accent(context, data);
        // Same hue as the accent at ~15% opacity for the box backgrounds.
        int boxTint = (accent & 0x00FFFFFF) | 0x26000000;

        long left = Math.max(0L, start - System.currentTimeMillis());
        long days = left / DAY;
        long hours = (left % DAY) / HOUR;
        long minutes = (left % HOUR) / MINUTE;

        setText(context, views, "hw_empty", null);
        setText(context, views, "hw_caption", join(data.optString("title", ""), data.optString("location", "")));
        views.setViewVisibility(id(context, "hw_boxes"), View.VISIBLE);

        setNumber(context, views, "hw_days", String.valueOf(days), accent);
        setNumber(context, views, "hw_hours", twoDigits(hours), accent);
        setNumber(context, views, "hw_minutes", twoDigits(minutes), accent);
        setNumber(context, views, "hw_days_unit", days == 1 ? "day" : "days", accent);
        setNumber(context, views, "hw_hours_unit", hours == 1 ? "hour" : "hours", accent);
        setNumber(context, views, "hw_minutes_unit", "min", accent);
        views.setInt(id(context, "hw_box1_bg"), "setColorFilter", boxTint);
        views.setInt(id(context, "hw_box2_bg"), "setColorFilter", boxTint);
        views.setInt(id(context, "hw_box3_bg"), "setColorFilter", boxTint);

        String dateLine = format("EEE d MMM", date) + SEPARATOR + time(context, date);
        setText(context, views, "hw_footer", left == 0 ? "Started" + SEPARATOR + dateLine : dateLine);

        views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, kind, data.optString("tapAction", "")));
        return views;
    }

    // ---- Small event ----

    private static RemoteViews eventSmall(Context context, String kind) {
        RemoteViews views = new RemoteViews(context.getPackageName(), res(context, "layout", "hw_event_small"));
        JSONObject data = loadEvent(context);

        if (data == null) {
            setText(context, views, "hw_title", appName(context));
            setText(context, views, "hw_relative", null);
            setText(context, views, "hw_time", null);
            setText(context, views, "hw_empty", "Open the app to load your event");
            views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, kind, ""));
            return views;
        }

        long start = data.optLong("start");
        Date date = new Date(start);

        setText(context, views, "hw_empty", null);
        setText(context, views, "hw_title", data.optString("title", ""));
        setText(context, views, "hw_relative", relative(start));
        views.setTextColor(id(context, "hw_relative"), accent(context, data));
        setText(context, views, "hw_time", format("EEE d MMM", date) + SEPARATOR + time(context, date));

        views.setOnClickPendingIntent(id(context, "hw_root"), launchIntent(context, kind, data.optString("tapAction", "")));
        return views;
    }

    // ---- Helpers ----

    /** The event only counts as loaded when it has a start time. */
    private static JSONObject loadEvent(Context context) {
        JSONObject data = HomeWidgetStore.load(context, HomeWidgetStore.EVENT);
        return data != null && data.optLong("start", 0L) > 0L ? data : null;
    }

    /** "Today", "Tomorrow", "In 15 days" or "Started", counted in calendar days. */
    private static String relative(long start) {
        long now = System.currentTimeMillis();
        if (start <= now) {
            return "Started";
        }
        long days = calendarDaysBetween(now, start);
        if (days <= 0) {
            return "Today";
        }
        if (days == 1) {
            return "Tomorrow";
        }
        return "In " + days + " days";
    }

    private static long calendarDaysBetween(long from, long to) {
        // Rounding keeps the result right across daylight-saving changes (23 or 25 hour days).
        return Math.round((midnight(to) - midnight(from)) / (double) DAY);
    }

    private static long midnight(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static String format(String pattern, Date date) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
    }

    /** Respects the phone's 12/24-hour setting. */
    private static String time(Context context, Date date) {
        return android.text.format.DateFormat.getTimeFormat(context).format(date);
    }

    private static String twoDigits(long value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static void setNumber(Context context, RemoteViews views, String name, String value, int color) {
        setText(context, views, name, value);
        views.setTextColor(id(context, name), color);
    }

    private static String join(String first, String second) {
        if (first == null || first.isEmpty()) {
            return second;
        }
        if (second == null || second.isEmpty()) {
            return first;
        }
        return first + SEPARATOR + second;
    }

    private static String firstNonEmpty(String first, String second) {
        return first != null && !first.isEmpty() ? first : second;
    }

    private static String appName(Context context) {
        return context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    }

    private static int accent(Context context, JSONObject data) {
        String hex = data == null ? "" : data.optString("accentColor", "");
        if (!hex.isEmpty()) {
            try {
                return Color.parseColor(hex);
            } catch (IllegalArgumentException ignored) {
                // Fall back to the default accent below.
            }
        }
        int colorId = res(context, "color", "hw_accent");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(colorId);
        }
        return context.getResources().getColor(colorId);
    }

    private static PendingIntent launchIntent(Context context, String kind, String tapAction) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            intent = new Intent();
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(BaseWidgetProvider.EXTRA_FROM_WIDGET, true);
        intent.putExtra(BaseWidgetProvider.EXTRA_TAP_ACTION, tapAction == null ? "" : tapAction);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        // One request code per widget kind: intents that differ only in extras would otherwise
        // share a PendingIntent, and every widget would open with the last tapAction.
        return PendingIntent.getActivity(context, 7310 + kindIndex(kind), intent, flags);
    }

    private static int kindIndex(String kind) {
        for (int i = 0; i < BaseWidgetProvider.KINDS.length; i++) {
            if (BaseWidgetProvider.KINDS[i].equals(kind)) {
                return i;
            }
        }
        return 0;
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
