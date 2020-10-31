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
package com.daskiworks.ghwatch.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.daskiworks.ghwatch.R;
import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.auth.AuthenticationManager;
import com.daskiworks.ghwatch.backend.PreferencesUtils;
import com.daskiworks.ghwatch.backend.UnreadNotificationsService;

/**
 * Broadcast receiver used to start alarm and process alarm wakeups.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {
  private static final String TAG = AlarmBroadcastReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent != null && (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))) {
      Log.i(TAG, "'Boot completed' or 'My package replaced' event received");
      startServerPoolingIfEnabled(context);
    } else {
      // fail early if internet is not available or user is not logged in to save battery
      ConnectivityManager cm = Utils.getConnectivityManager(context);
      if (Utils.isInternetConnectionAvailable(cm)
              && AuthenticationManager.getInstance().getGhApiCredentials(context) != null && (!PreferencesUtils.getServerCheckWifiOnly(context) || Utils.isInternetConnectionAvailableWifi(cm))) {
        UnreadNotificationsService s = new UnreadNotificationsService(context);
        s.newNotificationCheck();
      }
    }
  }

  /**
   * Start alarm used to periodically check new notifications on server and fire Android notifications if it is enabled in preferences. Period taken from
   * default preferences also.
   *
   * @param context
   */
  public static void startServerPoolingIfEnabled(Context context) {
    if (UnreadNotificationsService.isUnreadNotificationsServerCheckNecessary(context)) {
      String pv = PreferencesUtils.getString(context, PreferencesUtils.PREF_SERVER_CHECK_PERIOD, "" + R.string.pref_serverCheckPeriod_default);
      int ipv = R.string.pref_serverCheckPeriod_default;
      try {
        ipv = Integer.parseInt(pv);
      } catch (NumberFormatException e) {
        Log.e(TAG, PreferencesUtils.PREF_SERVER_CHECK_PERIOD + " preference value is not number but: " + pv + ". Default value used: " + ipv);
      }
      startServerPooling(context, ipv);
    }
  }

  public static void startServerPooling(Context context, int periodInMinutes) {
    Log.i(TAG, "starting alarm to check new notifications every " + periodInMinutes + " minutes");
    Utils.getAlarmManager(context).setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, Utils.MILLIS_MINUTE, periodInMinutes * Utils.MILLIS_MINUTE,
            prepareAlarmIntent(context));
  }

  public static void stopServerPoolingIfDisabled(Context context) {
    if (!UnreadNotificationsService.isUnreadNotificationsServerCheckNecessary(context)) {
      Log.i(TAG, "stopping alarm to check new notifications");
      Utils.getAlarmManager(context).cancel(prepareAlarmIntent(context));
    }
  }

  protected static PendingIntent prepareAlarmIntent(Context context) {
    Intent aintent = new Intent(context, AlarmBroadcastReceiver.class);
    PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, aintent, 0);
    return alarmIntent;
  }
}
