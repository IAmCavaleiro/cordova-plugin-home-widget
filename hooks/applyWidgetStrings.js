'use strict';

// Writes the WIDGET_* / EVENT_* plugin variables into hw_strings.xml (names and descriptions
// in the launcher's widget picker). Newer cordova-android templates have no
// res/values/strings.xml, so a <config-file> with $VARIABLES can't be used; the plugin ships
// its own strings file instead.

const fs = require('fs');
const path = require('path');

const PLUGIN_ID = 'cordova-plugin-home-widget';
const DEFAULTS = {
    WIDGET_LABEL: 'Bill',
    WIDGET_DESCRIPTION: 'Your latest bill at a glance',
    EVENT_LABEL: 'Event',
    EVENT_DESCRIPTION: 'Your next event at a glance'
};

function readVariables(projectRoot) {
    try {
        const pkg = JSON.parse(fs.readFileSync(path.join(projectRoot, 'package.json'), 'utf8'));
        const saved = (pkg.cordova && pkg.cordova.plugins && pkg.cordova.plugins[PLUGIN_ID]) || {};
        const vars = Object.assign({}, DEFAULTS);
        Object.keys(saved).forEach(function (key) {
            if (saved[key] !== undefined && saved[key] !== null && String(saved[key]) !== '') {
                vars[key] = String(saved[key]);
            }
        });
        return vars;
    } catch (e) {
        return Object.assign({}, DEFAULTS);
    }
}

// Android string resources need XML escaping plus escaped quotes and apostrophes.
function escapeAndroidString(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\\/g, '\\\\')
        .replace(/'/g, "\\'")
        .replace(/"/g, '\\"');
}

function stringsXml(vars) {
    const strings = {
        hw_label_bill: vars.WIDGET_LABEL,
        hw_label_bill_small: vars.WIDGET_LABEL + ' (small)',
        hw_label_event: vars.EVENT_LABEL,
        hw_label_countdown: vars.EVENT_LABEL + ' countdown',
        hw_label_event_small: vars.EVENT_LABEL + ' (small)',
        hw_widget_description: vars.WIDGET_DESCRIPTION,
        hw_event_description: vars.EVENT_DESCRIPTION
    };
    const lines = Object.keys(strings).map(function (name) {
        return '    <string name="' + name + '">' + escapeAndroidString(strings[name]) + '</string>';
    });
    return '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n' + lines.join('\n') + '\n</resources>\n';
}

module.exports = function (context) {
    const projectRoot = context.opts.projectRoot;
    const target = path.join(projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'res', 'values', 'hw_strings.xml');
    if (!fs.existsSync(target)) {
        return;
    }
    fs.writeFileSync(target, stringsXml(readVariables(projectRoot)), 'utf8');
};
