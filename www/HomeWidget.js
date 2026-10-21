var exec = require('cordova/exec');

var SERVICE = 'HomeWidget';

/** Widget types, as named in requestPin() and in the launcher's widget picker. */
var KINDS = ['bill', 'billSmall', 'event', 'countdown', 'eventSmall'];

function isObject(value) {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function copyText(data, keys, out) {
    keys.forEach(function (key) {
        if (data[key] !== undefined && data[key] !== null && String(data[key]) !== '') {
            out[key] = String(data[key]);
        }
    });
}

function copyColor(data, out) {
    if (data.accentColor) {
        var color = String(data.accentColor).trim();
        if (!/^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(color)) {
            throw new Error('accentColor must look like #RRGGBB or #AARRGGBB');
        }
        out.accentColor = color;
    }
}

/**
 * Bill data, shown by the "Bill" and "Bill (small)" widgets. All fields are optional.
 *
 *   header       small text above the amount, before the title   "My telecom"
 *   title        what the amount is                               "October bill"
 *   value        the big amount                                   "€32,50"
 *   badge        first part of the due line, in the accent color  "Due in 15 days"
 *   subtitle     second part of the due line                      "5 Nov"
 *   footerAction text of the button (no text = no button)         "Pay now"
 *   accentColor  hex color for the due line and the button        "#E60000"
 *   tapAction    string handed back to onTap()                    "pay-bill"
 *
 * footer and progress are still accepted for compatibility but not shown by these layouts.
 */
function normaliseBill(data) {
    var out = {};
    copyText(data, ['header', 'title', 'value', 'subtitle', 'badge', 'footer', 'footerAction', 'tapAction'], out);
    if (data.progress !== undefined && data.progress !== null && data.progress !== '') {
        var p = Number(data.progress);
        if (isNaN(p)) {
            throw new Error('progress must be a number between 0 and 100');
        }
        out.progress = Math.max(0, Math.min(100, Math.round(p)));
    }
    copyColor(data, out);
    out.updatedAt = Date.now();
    return out;
}

/**
 * Event data, shown by the "Event", "Event countdown" and "Event (small)" widgets.
 *
 *   title       event name (required)                          "Product launch"
 *   start       when it starts (required): Date, milliseconds,
 *               or a date string such as "2026-11-05T18:30:00"
 *               (no time zone = the phone's local time)
 *   location    where                                          "Lisbon Congress Centre"
 *   accentColor hex color for the date tile and countdown      "#5B3CC4"
 *   tapAction   string handed back to onTap()                  "open-event"
 */
function normaliseEvent(data) {
    var out = {};
    copyText(data, ['title', 'location', 'tapAction'], out);
    if (!out.title) {
        throw new Error('updateEvent() needs a title');
    }
    var start = data.start instanceof Date ? data.start.getTime()
        : typeof data.start === 'number' ? data.start
        : Date.parse(String(data.start || ''));
    if (!start || isNaN(start)) {
        throw new Error('updateEvent() needs a valid start date, e.g. "2026-11-05T18:30:00"');
    }
    out.start = start;
    copyColor(data, out);
    out.updatedAt = Date.now();
    return out;
}

function send(action, normalise, data, success, error) {
    if (!isObject(data)) {
        if (error) error(action + '() needs an object');
        return;
    }
    var payload;
    try {
        payload = normalise(data);
    } catch (e) {
        if (error) error(e.message);
        return;
    }
    exec(success, error, SERVICE, action, [payload]);
}

var HomeWidget = {
    KINDS: KINDS,

    /** Saves the bill and redraws the "Bill" widgets. */
    update: function (data, success, error) {
        send('update', normaliseBill, data, success, error);
    },

    /** Saves the event and redraws the "Event" widgets. */
    updateEvent: function (event, success, error) {
        send('updateEvent', normaliseEvent, event, success, error);
    },

    /**
     * Removes saved data; the widgets show their empty state.
     * clear(success, error) removes everything (use it on logout);
     * clear('bill' | 'event', success, error) removes one of them.
     */
    clear: function (what, success, error) {
        if (typeof what === 'function') {
            error = success;
            success = what;
            what = '';
        }
        exec(success, error, SERVICE, 'clear', [what || '']);
    },

    /** Returns true if the launcher lets the app ask to add a widget (Android 8+). */
    isPinSupported: function (success, error) {
        exec(function (result) { success(result === true || result === 1); }, error, SERVICE, 'isPinSupported', []);
    },

    /**
     * Opens the system dialog that adds a widget to the home screen.
     * kind: 'bill' (default), 'billSmall', 'event', 'countdown' or 'eventSmall'.
     */
    requestPin: function (kind, success, error) {
        if (typeof kind === 'function') {
            error = success;
            success = kind;
            kind = 'bill';
        }
        kind = kind || 'bill';
        if (KINDS.indexOf(kind) < 0) {
            if (error) error("Unknown widget type '" + kind + "'. Use " + KINDS.join(', ') + '.');
            return;
        }
        exec(success, error, SERVICE, 'requestPin', [kind]);
    },

    /** Returns how many widgets of this app (all types) are on the home screen. */
    getInstalledCount: function (success, error) {
        exec(success, error, SERVICE, 'getInstalledCount', []);
    },

    /**
     * Calls callback(tapAction) whenever the user opens the app by tapping a widget,
     * including a cold start (the tap that launched the app is delivered once).
     */
    onTap: function (callback, error) {
        if (typeof callback !== 'function') {
            if (error) error('onTap() needs a function');
            return;
        }
        exec(function (action) { callback(action || ''); }, error, SERVICE, 'registerTapListener', []);
    }
};

module.exports = HomeWidget;
