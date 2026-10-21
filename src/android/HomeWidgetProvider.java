package com.homewidget.plugin;

/** "Bill" widget (layout B). Keeps the original class name so existing manifests stay valid. */
public class HomeWidgetProvider extends BaseWidgetProvider {
    @Override
    String kind() {
        return KIND_BILL;
    }
}
