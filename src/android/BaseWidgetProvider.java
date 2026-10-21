package com.homewidget.plugin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Shared code for the five widgets in the launcher's picker. Each subclass only names its kind;
 * HomeWidgetRenderer draws it from the data the app saved.
 */
public abstract class BaseWidgetProvider extends AppWidgetProvider {

    static final String EXTRA_TAP_ACTION = "com.homewidget.plugin.TAP_ACTION";
    static final String EXTRA_FROM_WIDGET = "com.homewidget.plugin.FROM_WIDGET";

    static final String KIND_BILL = "bill";
    static final String KIND_BILL_SMALL = "billSmall";
    static final String KIND_EVENT = "event";
    static final String KIND_COUNTDOWN = "countdown";
    static final String KIND_EVENT_SMALL = "eventSmall";

    static final String[] KINDS = { KIND_BILL, KIND_BILL_SMALL, KIND_EVENT, KIND_COUNTDOWN, KIND_EVENT_SMALL };

    /** Broadcast the minute ticker sends to EventCountdownWidgetProvider. */
    static final String ACTION_TICK = "com.homewidget.plugin.TICK";

    private static final long MINUTE = 60L * 1000L;

    abstract String kind();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TICK.equals(intent.getAction())) {
            refreshKind(context, KIND_COUNTDOWN);
            updateTicker(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        manager.updateAppWidget(appWidgetIds, HomeWidgetRenderer.build(context, kind()));
        updateTicker(context);
    }

    @Override
    public void onDisabled(Context context) {
        // The last widget of this type was removed; stop ticking if no countdown is left.
        updateTicker(context);
    }

    /**
     * Keeps the countdown's minutes live: while a countdown widget is on the home screen and the
     * event hasn't started (plus one more tick to show "Started"), schedules the next redraw for
     * the moment the minute changes. The alarm doesn't wake the phone, so it costs almost no
     * battery; it fires as soon as the phone is awake again. No exact-alarm permission is needed.
     */
    static void updateTicker(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) {
            return;
        }
        Intent intent = new Intent(context, EventCountdownWidgetProvider.class).setAction(ACTION_TICK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent tick = PendingIntent.getBroadcast(context, 7400, intent, flags);

        long now = System.currentTimeMillis();
        long start = HomeWidgetRenderer.eventStart(context);
        boolean needed = start > 0L && start + MINUTE > now && idsFor(context, KIND_COUNTDOWN).length > 0;
        if (!needed) {
            alarms.cancel(tick);
            return;
        }
        // Next change of the displayed minutes, counted from the event's start; at least 1 s away.
        long untilNextMinute = start > now ? (start - now) % MINUTE : MINUTE;
        long trigger = now + Math.max(1000L, untilNextMinute) + 500L;
        alarms.setWindow(AlarmManager.RTC, trigger, 5000L, tick);
    }

    static void refreshKind(Context context, String kind) {
        int[] ids = idsFor(context, kind);
        if (ids.length > 0) {
            AppWidgetManager.getInstance(context).updateAppWidget(ids, HomeWidgetRenderer.build(context, kind));
        }
    }

    static Class<? extends BaseWidgetProvider> providerFor(String kind) {
        if (KIND_BILL_SMALL.equals(kind)) {
            return BillSmallWidgetProvider.class;
        }
        if (KIND_EVENT.equals(kind)) {
            return EventWidgetProvider.class;
        }
        if (KIND_COUNTDOWN.equals(kind)) {
            return EventCountdownWidgetProvider.class;
        }
        if (KIND_EVENT_SMALL.equals(kind)) {
            return EventSmallWidgetProvider.class;
        }
        return HomeWidgetProvider.class;
    }

    static boolean isKnownKind(String kind) {
        for (String k : KINDS) {
            if (k.equals(kind)) {
                return true;
            }
        }
        return false;
    }

    static int[] idsFor(Context context, String kind) {
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, providerFor(kind)));
        return ids == null ? new int[0] : ids;
    }

    /** Redraws every copy of every widget. Called by the plugin after the data changes. */
    static void refreshAll(Context context) {
        for (String kind : KINDS) {
            refreshKind(context, kind);
        }
        updateTicker(context);
    }

    static int installedCount(Context context) {
        int total = 0;
        for (String kind : KINDS) {
            total += idsFor(context, kind).length;
        }
        return total;
    }
}
