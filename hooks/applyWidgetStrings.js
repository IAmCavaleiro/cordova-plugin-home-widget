'use strict';

// Writes the WIDGET_LABEL / WIDGET_DESCRIPTION plugin variables into hw_strings.xml.
// Newer cordova-android templates have no res/values/strings.xml, so a <config-file>
// with $VARIABLES can't be used; the plugin ships its own strings file instead.

const fs = require('fs');
const path = require('path');

const PLUGIN_ID = 'cordova-plugin-home-widget';
const DEFAULTS = {
    WIDGET_LABEL: 'Summary',
    WIDGET_DESCRIPTION: 'Shows your latest summary at a glance'
};

function readVariables(projectRoot) {
    try {
        const pkg = JSON.parse(fs.readFileSync(path.join(projectRoot, 'package.json'), 'utf8'));
        const saved = (pkg.cordova && pkg.cordova.plugins && pkg.cordova.plugins[PLUGIN_ID]) || {};
        return Object.assign({}, DEFAULTS, saved);
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

module.exports = function (context) {
    const projectRoot = context.opts.projectRoot;
    const target = path.join(projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'res', 'values', 'hw_strings.xml');
    if (!fs.existsSync(target)) {
        return;
    }
    const vars = readVariables(projectRoot);
    const xml = '<?xml version="1.0" encoding="utf-8"?>\n' +
        '<resources>\n' +
        '    <string name="hw_widget_label">' + escapeAndroidString(vars.WIDGET_LABEL) + '</string>\n' +
        '    <string name="hw_widget_description">' + escapeAndroidString(vars.WIDGET_DESCRIPTION) + '</string>\n' +
        '</resources>\n';
    fs.writeFileSync(target, xml, 'utf8');
};
