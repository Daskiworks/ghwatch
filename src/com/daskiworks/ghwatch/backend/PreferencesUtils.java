/*
 * Copyright 2014 contributors as indicated by the @authors tag.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.daskiworks.ghwatch.backend;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Utilities to work with default preferences.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class PreferencesUtils {

  /*
   * Names of user edited preferences.
   */
  public static final String PREF_SERVER_CHECK_PERIOD = "pref_serverCheckPeriod";
  public static final String PREF_SERVER_CHECK_FULL = "pref_serverCheckFull";
  public static final String PREF_SERVER_DETAIL_LOADING = "pref_serverDetailLoading";
  public static final String PREF_SERVER_ACCOUNT = "pref_serverAccount";
  public static final String PREF_NOTIFY = "pref_notify";
  public static final String PREF_NOTIFY_VIBRATE = "pref_notifyVibrate";
  public static final String PREF_NOTIFY_SOUND = "pref_notifySound";
  public static final String PREF_NOTIFY_FILTER = "pref_notifyFilter";

  public static final String PREF_NOTIFY_FILTER_INHERITED = "0";
  public static final String PREF_NOTIFY_FILTER_ALL = "1";
  public static final String PREF_NOTIFY_FILTER_PARTICIPATING = "2";
  public static final String PREF_NOTIFY_FILTER_NOTHING = "3";

  /*
   * Names of internal preferences
   */
  public static final String INT_SERVERINFO_APILIMIT = "pref_serverInfo_APILimit";
  public static final String INT_SERVERINFO_APILIMITREMAINING = "pref_serverInfo_APILimitRemaining";
  public static final String INT_SERVERINFO_APILIMITRESETTIMESTAMP = "pref_serverInfo_APILimitResetTimestamp";
  public static final String INT_SERVERINFO_LASTREQUESTDURATION = "pref_serverInfo_lastRequestDuration";
  public static final String INT_SERVERINFO_LASTUNREADNOTIFBACKREQUESTTIMESTAMP = "pref_serverInfo_lastUnredNotifBackRequestTimestamp";

  /* Set to true if at least one widget for unread notifications exists */
  public static final String PREF_WIDGET_UNREAD_EXISTS = "pref_widget_unread_exists";
  public static final String PREF_WIDGET_UNREAD_HIGHLIGHT = "pref_widget_unread_highlight";

  /**
   * Get string preference.
   * 
   * @param context to get preferences for
   * @param key of preference
   * @param defaultValue
   * @return preference value
   */
  public static String getString(Context context, String key, String defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
  }

  /**
   * Patch preferences after restored from cloud - remove preferences which shouldn't be restored on new device.
   * 
   * @param context
   */
  public static void patchAfterRestore(Context context) {
    SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = wmbPreference.edit();
    editor.remove(PREF_WIDGET_UNREAD_EXISTS);
    editor.remove(PREF_WIDGET_UNREAD_HIGHLIGHT);
    editor.remove(PREF_SERVER_ACCOUNT);
    editor.commit();
  }

  /**
   * Read Notification Filter setting for defined repository.
   * 
   * @param context to read preference over
   * @param repositoryName to get preference for
   * @param inheritResolve if true then {@link #PREF_NOTIFY_FILTER_INHERITED} is not returned but resolved from master preference
   * @return some of {@link #PREF_NOTIFY_FILTER_ALL} {@link #PREF_NOTIFY_FILTER_PARTICIPATING}, {@link #PREF_NOTIFY_FILTER_NOTHING} or
   *         {@link #PREF_NOTIFY_FILTER_INHERITED}
   */
  public static String getNotificationFilterForRepository(Context context, String repositoryName, boolean inheritResolve) {
    String rs = getString(context, getNotificationFilterRepositoryPrefName(repositoryName), PREF_NOTIFY_FILTER_INHERITED);
    if (inheritResolve && PREF_NOTIFY_FILTER_INHERITED.equals(rs)) {
      return getNotificationFilter(context);
    } else {
      return rs;
    }
  }

  /**
   * Read Notification Filter master setting
   * 
   * @param context to read preference over
   * @return some of {@link #PREF_NOTIFY_FILTER_ALL} {@link #PREF_NOTIFY_FILTER_PARTICIPATING}, {@link #PREF_NOTIFY_FILTER_NOTHING} or
   *         {@link #PREF_NOTIFY_FILTER_INHERITED}
   */
  public static String getNotificationFilter(Context context) {
    return getString(context, PREF_NOTIFY_FILTER, PREF_NOTIFY_FILTER_ALL);
  }

  /**
   * Store Notification Filter setting for defined repository.
   * 
   * @param context used to write preference
   * @param repositoryName to write preference for
   * @param value of preference, some of {@link #PREF_NOTIFY_FILTER_ALL} {@link #PREF_NOTIFY_FILTER_PARTICIPATING}, {@link #PREF_NOTIFY_FILTER_NOTHING} or
   *          {@link #PREF_NOTIFY_FILTER_INHERITED}
   */
  public static void setNotificationFilterForRepository(Context context, String repositoryName, String value) {
    storeString(context, getNotificationFilterRepositoryPrefName(repositoryName), value);
    (new BackupManager(context)).dataChanged();
  }

  protected static String getNotificationFilterRepositoryPrefName(String repositoryName) {
    return PREF_NOTIFY_FILTER + "-" + repositoryName;
  }

  /**
   * Get boolean preference.
   * 
   * @param context
   * @param key
   * @param defaultValue
   * @return
   */
  public static boolean getBoolean(Context context, String key, boolean defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
  }

  /**
   * Get boolean preference. Same as {@link #getBoolean(Context, String, boolean)} with <code>false</code> as default.
   */
  public static boolean getBoolean(Context context, String key) {
    return getBoolean(context, key, false);
  }

  /**
   * Get long preference.
   * 
   * @param context
   * @param key
   * @param defaultValue
   * @return
   */
  public static long getLong(Context context, String key, long defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getLong(key, defaultValue);
  }

  /**
   * Store long preference.
   * 
   * @param context
   * @param key
   * @param value
   */
  public static void storeLong(Context context, String key, long value) {
    SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = wmbPreference.edit();
    editor.putLong(key, value);
    editor.commit();
  }

  /**
   * Store boolean preference.
   * 
   * @param context
   * @param key
   * @param value
   */
  public static void storeBoolean(Context context, String key, boolean value) {
    SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = wmbPreference.edit();
    editor.putBoolean(key, value);
    editor.commit();
  }

  /**
   * Store String preference.
   * 
   * @param context
   * @param key of preference
   * @param value to store
   */
  public static void storeString(Context context, String key, String value) {
    SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = wmbPreference.edit();
    editor.putString(key, value);
    editor.commit();
  }

  /**
   * Remove preference.
   * 
   * @param context
   * @param key of preference to remove
   */
  public static void remove(Context context, String key) {
    SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = wmbPreference.edit();
    editor.remove(key);
    editor.commit();
  }

}
