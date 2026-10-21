package com.homewidget.plugin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;

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

    abstract String kind();

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        manager.updateAppWidget(appWidgetIds, HomeWidgetRenderer.build(context, kind()));
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
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (String kind : KINDS) {
            int[] ids = idsFor(context, kind);
            if (ids.length > 0) {
                manager.updateAppWidget(ids, HomeWidgetRenderer.build(context, kind));
            }
        }
    }

    static int installedCount(Context context) {
        int total = 0;
        for (String kind : KINDS) {
            total += idsFor(context, kind).length;
        }
        return total;
    }
}
