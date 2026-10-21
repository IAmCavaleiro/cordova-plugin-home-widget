var exec = require('cordova/exec');

var SERVICE = 'HomeWidget';

function isObject(value) {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
}

/**
 * Normalises the data the app sends so the native side gets a predictable shape.
 * All fields are optional; anything left out is hidden on the widget.
 *
 *   header      small text next to the app icon      "My telecom"
 *   title       label above the main value            "October bill"
 *   value       the big number or word                "€32,50"
 *   subtitle    text on the right of the value        "Pay by 5 Nov"
 *   badge       pill in the top-right corner          "Due in 15 days"
 *   footer      bottom line                           "8.2 of 20 GB used"
 *   footerAction text on the bottom right             "Pay now"
 *   progress    0-100, draws a bar above the footer   41
 *   accentColor hex color for bar, badge and action   "#E60000"
 *   tapAction   string handed back to onTap()         "pay-bill"
 */
function normalise(data) {
    var out = {};
    var textFields = ['header', 'title', 'value', 'subtitle', 'badge', 'footer', 'footerAction', 'tapAction'];

    textFields.forEach(function (key) {
        if (data[key] !== undefined && data[key] !== null && String(data[key]) !== '') {
            out[key] = String(data[key]);
        }
    });

    if (data.progress !== undefined && data.progress !== null && data.progress !== '') {
        var p = Number(data.progress);
        if (isNaN(p)) {
            throw new Error('progress must be a number between 0 and 100');
        }
        out.progress = Math.max(0, Math.min(100, Math.round(p)));
    }

    if (data.accentColor) {
        var color = String(data.accentColor).trim();
        if (!/^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(color)) {
            throw new Error('accentColor must look like #RRGGBB or #AARRGGBB');
        }
        out.accentColor = color;
    }

    out.updatedAt = Date.now();
    return out;
}

var HomeWidget = {
    /** Saves the data and redraws every copy of the widget on the home screen. */
    update: function (data, success, error) {
        if (!isObject(data)) {
            if (error) error('update() needs an object');
            return;
        }
        var payload;
        try {
            payload = normalise(data);
        } catch (e) {
            if (error) error(e.message);
            return;
        }
        exec(success, error, SERVICE, 'update', [payload]);
    },

    /** Removes the saved data; the widget shows its empty state. */
    clear: function (success, error) {
        exec(success, error, SERVICE, 'clear', []);
    },

    /** Returns true if the launcher lets the app ask to add the widget (Android 8+). */
    isPinSupported: function (success, error) {
        exec(function (result) { success(result === true || result === 1); }, error, SERVICE, 'isPinSupported', []);
    },

    /** Opens the system dialog that adds the widget to the home screen. */
    requestPin: function (success, error) {
        exec(success, error, SERVICE, 'requestPin', []);
    },

    /** Returns how many copies of the widget are currently on the home screen. */
    getInstalledCount: function (success, error) {
        exec(success, error, SERVICE, 'getInstalledCount', []);
    },

    /**
     * Calls callback(tapAction) whenever the user opens the app by tapping the widget,
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
