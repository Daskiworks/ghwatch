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
package com.daskiworks.ghwatch;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * Utilities.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class Utils {

  public static final long MILLIS_SECOND = 1000L;
  public static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;
  public static final long MILLIS_HOUR = 60 * MILLIS_MINUTE;
  public static final long MILLIS_DAY = 24 * MILLIS_HOUR;

  /**
   * Get Vibrator if available in system.
   * 
   * @param context to use for get
   * @return {@link Vibrator} instance or null if not available in system.
   */
  public static Vibrator getVibrator(Context context) {
    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    if (v.hasVibrator())
      return v;
    return null;
  }

  /**
   * Get {@link ConnectivityManager} from context.
   * 
   * @param context
   * @return manager
   */
  public static ConnectivityManager getConnectivityManager(Context context) {
    return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
  }

  /**
   * Check if Internet connections available.
   * 
   * @param conMgr to be used
   * @return true if active connection is available
   */
  public static boolean isInternetConnectionAvailable(ConnectivityManager conMgr) {
    NetworkInfo ni = conMgr.getActiveNetworkInfo();
    return ni != null && ni.isConnected() && ni.isAvailable();
  }

  /**
   * Check if WiFi Internet connection is available.
   * 
   * @param conMgr to be used
   * @return true if active connection is available
   */
  public static boolean isInternetConnectionAvailableWifi(ConnectivityManager conMgr) {
    NetworkInfo ni = conMgr.getActiveNetworkInfo();
    return ni != null && ni.isConnected() && ni.isAvailable() && ni.getType() == ConnectivityManager.TYPE_WIFI;
  }

  /**
   * Check if Internet connections available.
   * 
   * @param context to be used for checking
   * @return true if active connection is available
   */
  public static boolean isInternetConnectionAvailable(Context context) {
    NetworkInfo ni = getConnectivityManager(context).getActiveNetworkInfo();
    return ni != null && ni.isConnected() && ni.isAvailable();
  }

  /**
   * Get {@link AlarmManager} from context.
   * 
   * @param context
   * @return manager
   */
  public static AlarmManager getAlarmManager(Context context) {
    return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  }

  /**
   * Get {@link NotificationManager} from context.
   * 
   * @param context
   * @return manager
   */
  public static NotificationManager getNotificationManager(Context context) {
    return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  /**
   * Delete file from persistent store.
   * 
   * @param context
   * @param file to delete
   * @return true if really deleted
   */
  public static boolean deleteFromStore(Context context, File file) {
    return context.deleteFile(file.getName());
  }

  /**
   * Write notification to the persistent store.
   * 
   * @param TAG for logging
   * @param context
   * @param file to write into
   * @param data to persist
   * @return true if persisting is OK, false if failed.
   */
  public static boolean writeToStore(String TAG, Context context, File file, Serializable data) {
    FileOutputStream fos = null;
    ObjectOutputStream oos = null;

    try {
      fos = context.openFileOutput(file.getName(), Context.MODE_PRIVATE);
      oos = new ObjectOutputStream(fos);
      oos.writeObject(data);
      oos.flush();
      return true;
    } catch (Exception e) {
      Log.w(TAG, "File write to persistens store failed: " + e.getMessage(), e);
      return false;
    } finally {
      closeStream(oos);
    }
  }

  /**
   * @param TAG for logging
   * @param context
   * @param file to read from
   * @return stream from store. <code>null</code> if not in store or load failed.
   */
  @SuppressWarnings("unchecked")
  public static <T> T readFromStore(String TAG, Context context, File file) {

    if (file == null || !file.exists())
      return null;

    FileInputStream fis = null;
    ObjectInputStream ois = null;

    try {
      fis = context.openFileInput(file.getName());
      ois = new ObjectInputStream(fis);
      return (T) ois.readObject();
    } catch (InvalidClassException e) {
      Log.w(TAG, "Class changed so we can't load data from store " + e.getMessage());
    } catch (Exception e) {
      Log.w(TAG, "File read from persistens store failed: " + e.getMessage(), e);
    } finally {
      closeStream(fis);
    }
    return null;
  }

  /**
   * Copy two streams.
   * 
   * @param is input stream to copy from.
   * @param os stream to copy to.
   */
  public static void copyStream(InputStream is, OutputStream os) {
    final int buffer_size = 1024;
    try {
      byte[] bytes = new byte[buffer_size];
      for (;;) {
        int count = is.read(bytes, 0, buffer_size);
        if (count == -1)
          break;
        os.write(bytes, 0, count);
      }
    } catch (Exception ex) {
    }
  }

  /**
   * null and exception safe stream close.
   * 
   * @param stream to close
   */
  public static void closeStream(Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        // OK
      }
    }
  }

  public static String trimToNull(String value) {
    if (value == null || "null".equalsIgnoreCase(value))
      return null;
    value = value.trim();
    if (value.isEmpty())
      return null;
    return value;
  }

  /**
   * Format date interval of provided date from now for very short visualization.
   * 
   * @param context
   * @param date
   * @return formatted date interval
   */
  public static CharSequence formatDateIntervalFromNow(Context context, Date date) {
    if (date == null)
      return "";
    long interval = System.currentTimeMillis() - date.getTime();
    if (interval < 0)
      interval = 0;

    if (interval < MILLIS_HOUR) {
      return interval / (MILLIS_MINUTE) + context.getString(R.string.time_interval_abbreviation_minute);
    }
    if (interval < MILLIS_DAY) {
      return interval / (MILLIS_HOUR) + context.getString(R.string.time_interval_abbreviation_hour);
    }

    if (interval < MILLIS_DAY * 10L) {
      return interval / (MILLIS_DAY) + context.getString(R.string.time_interval_abbreviation_day);
    }

    return DateFormat.getDateFormat(context).format(date);
    // return DateUtils.getRelativeTimeSpanString(updatedAt.getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
  }

  /**
   * Format notification type text for view.
   * 
   * @param subjectType to get view text for
   * @return view text
   */
  public static CharSequence formatNotificationTypeForView(String subjectType) {
    if (subjectType == null || subjectType.isEmpty())
      return "";

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < subjectType.length(); i++) {
      char ch = subjectType.charAt(i);
      if (i > 0 && Character.isUpperCase(ch))
        sb.append(" ");
      sb.append(ch);
    }

    return sb;
  }

  /**
   * Method to safe dismiss Dialog without exception - workaround for bug #68
   * 
   * @param dialog
   */
  public static void dismissDialogSafe(Dialog dialog) {
    if (dialog == null)
      return;
    try {
      dialog.dismiss();
    } catch (final IllegalArgumentException e) {
      // Ignore this exception
    }
  }

}