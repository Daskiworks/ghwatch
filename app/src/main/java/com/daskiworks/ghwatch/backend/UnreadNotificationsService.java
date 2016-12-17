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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.daskiworks.ghwatch.ActivityTracker;
import com.daskiworks.ghwatch.MainActivity;
import com.daskiworks.ghwatch.R;
import com.daskiworks.ghwatch.UnreadAppWidgetProvider;
import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.alarm.MarkNotifiationAsReadReceiver;
import com.daskiworks.ghwatch.backend.RemoteSystemClient.Response;
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.Notification;
import com.daskiworks.ghwatch.model.NotificationStream;
import com.daskiworks.ghwatch.model.NotificationStreamViewData;
import com.daskiworks.ghwatch.model.NotificationViewData;
import com.daskiworks.ghwatch.model.StringViewData;

import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.NoRouteToHostException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Service used to work with unread notifications.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class UnreadNotificationsService {

  private static final String TAG = "UnreadNotificationsSvc";

  /**
   * URL to load notifications from.
   */
  private static final String URL_NOTIFICATIONS = GHConstants.URL_BASE + "/notifications";
  // private static final String URL_NOTIFICATIONS = GHConstants.URL_BASE+"/notifications?all=true";

  private static final String URL_REPOS = GHConstants.URL_BASE + "/repos/";
  private static final String URL_THREADS = GHConstants.URL_BASE + "/notifications/threads/";

  /**
   * Name of file where data are persisted.
   */
  private static final String persistFileName = "NotificationsUnread.td";

  /**
   * Reload from server is forced automatically if data in persistent store are older than this timeout [millis]
   */
  private static final long FORCE_VIEW_RELOAD_AFTER = 5 * Utils.MILLIS_MINUTE;

  /**
   * Id of android notification so we can update it.
   */
  public static final int ANDROID_NOTIFICATION_ID = 0;

  // few fields initialized in constructor
  private Context context;
  private File persistFile;
  private AuthenticationManager authenticationManager;

  // few data loaders - initialized lazily when necessary only
  private NotificationDetailLoader notificationDetailLoader;
  private NotificationViewUrlLoader notificationViewUrlLoader;

  /**
   * Create service.
   *
   * @param context this service runs in
   */
  public UnreadNotificationsService(Context context) {
    this.context = context;
    persistFile = context.getFileStreamPath(persistFileName);
    this.authenticationManager = AuthenticationManager.getInstance();
  }

  /**
   * Get unread notifications for view.
   *
   * @param reloadStrategy if data should be reloaded from server
   * @return info about notifications
   */
  public NotificationStreamViewData getNotificationStreamForView(ViewDataReloadStrategy reloadStrategy) {

    NotificationStreamViewData nswd = new NotificationStreamViewData();
    NotificationStream ns = null;
    synchronized (TAG) {
      NotificationStream oldNs = Utils.readFromStore(TAG, context, persistFile);

      // user from store if possible, apply timeout of data from store
      if (reloadStrategy == ViewDataReloadStrategy.IF_TIMED_OUT) {
        ns = oldNs;
        if (ns != null && ns.getLastFullUpdateTimestamp() < (System.currentTimeMillis() - FORCE_VIEW_RELOAD_AFTER))
          ns = null;
      } else if (reloadStrategy == ViewDataReloadStrategy.NEVER) {
        ns = oldNs;
      }

      // read from server
      try {
        if (ns == null && reloadStrategy != ViewDataReloadStrategy.NEVER) {
          // we DO NOT use lastModified here because it returns only notifications newly added after given date, not all unread
          ns = readNotificationStreamFromServer(null);
          keepNotificationDetailDataAfterReload(ns, oldNs);
          if (ns != null) {
            Utils.writeToStore(TAG, context, persistFile, ns);
            updateWidgets();
          }
        }
      } catch (InvalidObjectException e) {
        nswd.loadingStatus = LoadingStatus.DATA_ERROR;
        Log.w(TAG, "NotificationStream loading failed due data format problem: " + e.getMessage(), e);
      } catch (NoRouteToHostException e) {
        nswd.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
        Log.d(TAG, "NotificationStream loading failed due connection not available.");
      } catch (AuthenticationException e) {
        nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
        Log.d(TAG, "NotificationStream loading failed due authentication problem: " + e.getMessage());
      } catch (IOException e) {
        nswd.loadingStatus = LoadingStatus.CONN_ERROR;
        Log.w(TAG, "NotificationStream loading failed due connection problem: " + e.getMessage());
      } catch (JSONException e) {
        nswd.loadingStatus = LoadingStatus.DATA_ERROR;
        Log.w(TAG, "NotificationStream loading failed due data format problem: " + e.getMessage());
      } catch (Exception e) {
        nswd.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
        Log.e(TAG, "NotificationStream loading failed due: " + e.getMessage(), e);
      }

      // Show content from store because we are unable to read new one but want to show something
      if (ns == null)
        ns = oldNs;

      nswd.notificationStream = ns;
      return nswd;
    }
  }

  private void keepNotificationDetailDataAfterReload(NotificationStream ns, NotificationStream oldNs) {
    if (oldNs != null && ns != null) {
      for (Notification n : ns) {
        Notification oldN = oldNs.getNotificationById(n.getId());
        if (oldN != null) {
          n.setSubjectStatus(oldN.getSubjectStatus());
          n.setSubjectDetailHtmlUrl(oldN.getSubjectDetailHtmlUrl());
          n.setSubjectLabels(oldN.getSubjectLabels());
        }
      }
    }
  }

  public BaseViewData markNotificationAsRead(long id) {
    BaseViewData nswd = new BaseViewData();
    try {
      RemoteSystemClient.postNoData(context, authenticationManager.getGhApiCredentials(context), URL_THREADS + id, null);
      synchronized (TAG) {
        NotificationStream oldNs = Utils.readFromStore(TAG, context, persistFile);
        if (oldNs != null) {
          oldNs.removeNotificationById(id);
          Utils.writeToStore(TAG, context, persistFile, oldNs);
          updateWidgets();
        }
      }
    } catch (NoRouteToHostException e) {
      nswd.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
    } catch (AuthenticationException e) {
      nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
    } catch (IOException e) {
      Log.w(TAG, "NotificationRead marking failed due connection problem: " + e.getMessage());
      nswd.loadingStatus = LoadingStatus.CONN_ERROR;
    } catch (Exception e) {
      Log.e(TAG, "NotificationRead marking failed due: " + e.getMessage(), e);
      nswd.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
    }
    return nswd;
  }

  public BaseViewData muteNotificationThread(long id) {
    BaseViewData nswd = new BaseViewData();
    try {
      RemoteSystemClient.putToURL(context, authenticationManager.getGhApiCredentials(context), URL_THREADS + id + "/subscription", null, "{\"ignored\":true}");
      // #49 mark it as read also to be removed from list
      markNotificationAsRead(id);
    } catch (NoRouteToHostException e) {
      nswd.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
    } catch (AuthenticationException e) {
      nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
    } catch (IOException e) {
      Log.w(TAG, "Notification thread mute failed due connection problem: " + e.getMessage());
      nswd.loadingStatus = LoadingStatus.CONN_ERROR;
    } catch (Exception e) {
      Log.e(TAG, "Notification thread mute failed due: " + e.getMessage(), e);
      nswd.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
    }
    return nswd;
  }

  public BaseViewData markAllNotificationsAsRead(String repository) {
    BaseViewData nswd = new BaseViewData();
    try {
      String url = URL_NOTIFICATIONS;
      if (repository != null) {
        url = URL_REPOS + repository + "/notifications";
      }
      RemoteSystemClient.putToURL(context, authenticationManager.getGhApiCredentials(context), url, null, "{}");
    } catch (NoRouteToHostException e) {
      nswd.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
    } catch (AuthenticationException e) {
      nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
    } catch (IOException e) {
      Log.w(TAG, "NotificationRead marking failed due connection problem: " + e.getMessage());
      nswd.loadingStatus = LoadingStatus.CONN_ERROR;
    } catch (Exception e) {
      Log.e(TAG, "NotificationRead marking failed due: " + e.getMessage(), e);
      nswd.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
    }
    return nswd;
  }

  /**
   * Get notification object containing all detailed data for view. May be loaded from server in this method, so do not call this from GUI thread!
   *
   * @param notification to get detail data for
   * @return view response with {@link Notification} containing all data
   */
  public NotificationViewData getNotificationDetailForView(Notification notification) {
    String apiUrl = notification.getSubjectUrl();
    if (notification.isDetailLoaded()) {
      return new NotificationViewData(notification);
    } else {
      if (notificationDetailLoader == null)
        notificationDetailLoader = new NotificationDetailLoader(TAG, context, authenticationManager, persistFile);
      return notificationDetailLoader.loadData(apiUrl, notification);
    }
  }

  /**
   * Get web view URL for the notification. May be loaded from server in this method, so do not call this from GUI thread!
   *
   * @param notification to get view url for
   * @return response with URL from data
   */
  public StringViewData getNotificationViewUrl(Notification notification) {
    StringViewData nswd = new StringViewData();

    String apiUrl = notification.getViewBaseUrl();
    if (apiUrl != null) {
      if (notification.isDetailLoaded() && apiUrl.equals(notification.getSubjectUrl())) {
        nswd.data = notification.getSubjectDetailHtmlUrl();
      } else if (apiUrl.equals(notification.getSubjectUrl()) && PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_SERVER_DETAIL_LOADING)
          && PreferencesUtils.readDonationTimestamp(context) != null) {
        NotificationViewData nvd = getNotificationDetailForView(notification);
        nswd.loadingStatus = nvd.loadingStatus;
        if (nvd.notification != null) {
          nswd.data = nvd.notification.getSubjectDetailHtmlUrl();
        }
      } else {
        if (notificationViewUrlLoader == null)
          notificationViewUrlLoader = new NotificationViewUrlLoader(apiUrl, context, authenticationManager);
        nswd = notificationViewUrlLoader.loadData(apiUrl, null);
      }
      if (nswd.data == null) {
        nswd.loadingStatus = LoadingStatus.DATA_ERROR;
        Log.w(TAG, "Notification html view URL loading failed due data format problem: no 'html_url' field in response");
      }
    } else {
      nswd.loadingStatus = LoadingStatus.DATA_ERROR;
    }
    return nswd;
  }

  ;

  /**
   * Switch if we will use optimized pooling or not.
   */
  private static final boolean USE_OPTIMIZED_POOLING = true;

  private static final long BACKGROUND_FORCE_FULL_RELOAD_AFTER = Utils.MILLIS_HOUR * 6L;
  private static final long BACKGROUND_FORCE_FULL_RELOAD_AFTER_WIFI = Utils.MILLIS_HOUR * 1L;

  /**
   * Prepare "Last-Modified" content which is used to do optimized calls to GitHub rest API by incremental updates. It decides based on
   * {@link #USE_OPTIMIZED_POOLING} switch and on time of last full update. We do full updates once a time to prevent problems with incremental updates.
   *
   * @param oldNs used to prepare header content
   * @return header content
   */
  public String prepareLastModifiedHeaderContent(NotificationStream oldNs, boolean isWiFi) {

    if (PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_SERVER_CHECK_FULL, false))
      return null;

    long ts = 0;
    if (isWiFi) {
      ts = System.currentTimeMillis() - BACKGROUND_FORCE_FULL_RELOAD_AFTER_WIFI;
    } else {
      ts = System.currentTimeMillis() - BACKGROUND_FORCE_FULL_RELOAD_AFTER;
    }

    if (USE_OPTIMIZED_POOLING && oldNs != null && oldNs.getLastFullUpdateTimestamp() > ts) {
      return oldNs.getLastModified();
    }
    return null;
  }

  /**
   * Return true if background check of unread notifications is necessary.
   *
   * @param context to be used
   * @return true if check is necessary
   */
  public static boolean isUnreadNotificationsServerCheckNecessary(Context context) {
    return PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_NOTIFY, true)
        || PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_EXISTS, false);
  }

  /**
   * Check new notifications on GitHub and fire androidNotification if necessary.
   * <p/>
   * Check is done asynchronously, new thread is started inside of this method.
   *
   * @see #newNotificationCheckImpl()
   */
  public void newNotificationCheck() {
    Thread t = new Thread() {
      public void run() {
        newNotificationCheckImpl();
      }
    };
    t.start();
  }

  /**
   * Real business logic for check new notifications on GitHub and fire androidNotification if necessary.
   *
   * @see #newNotificationCheck()
   */
  protected void newNotificationCheckImpl() {
    Log.d(TAG, "Notification check started");
    try {
      synchronized (TAG) {
        NotificationStream oldNs = Utils.readFromStore(TAG, context, persistFile);

        String lastModified = prepareLastModifiedHeaderContent(oldNs, Utils.isInternetConnectionAvailableWifi(Utils.getConnectivityManager(context)));

        NotificationStream ns = readNotificationStreamFromServer(lastModified);

        if (ns != null) {
          if (lastModified != null) {
            // incremental update has been performed and some new notif is available (ns is not null), so we have to add old ones to keep them
            if (oldNs != null) {
              for (Notification n : oldNs) {
                ns.addNotification(n);
              }
              ns.setLastFullUpdateTimestamp(oldNs.getLastFullUpdateTimestamp());
            }
          }
          keepNotificationDetailDataAfterReload(ns, oldNs);
          Utils.writeToStore(TAG, context, persistFile, ns);

          fireAndroidNotification(ns, oldNs);
          updateWidgetsFromBackgroundCheck(ns, oldNs);
        }
      }
    } catch (NoRouteToHostException e) {
      Log.d(TAG, "Notification check failed due: " + e.getMessage());
    } catch (Exception e) {
      Log.w(TAG, "Notification check failed due: " + e.getMessage());
    } finally {
      PreferencesUtils.storeLong(context, PreferencesUtils.INT_SERVERINFO_LASTUNREADNOTIFBACKREQUESTTIMESTAMP, System.currentTimeMillis());
      Log.d(TAG, "Notification check finished");
    }
  }

  /**
   * @param lastModified timestamp used in "If-Modified-Since" http header, can be null
   * @return null if lastModified used and nothing new
   * @throws InvalidObjectException
   * @throws NoRouteToHostException
   * @throws AuthenticationException
   * @throws IOException
   * @throws JSONException
   * @throws URISyntaxException
   */
  protected NotificationStream readNotificationStreamFromServer(String lastModified) throws InvalidObjectException, NoRouteToHostException,
      AuthenticationException, IOException, JSONException, URISyntaxException {

    //TODO #80 detect which URL should be used
    String url = URL_NOTIFICATIONS;

    Map<String, String> headers = null;
    if (lastModified != null) {
      headers = new HashMap<String, String>();
      headers.put("If-Modified-Since", lastModified);
    }

    Response<JSONArray> resp = RemoteSystemClient.getJSONArrayFromUrl(context, authenticationManager.getGhApiCredentials(context), url, headers);

    if (resp.notModified)
      return null;

    NotificationStream ns = NotificationStreamParser.parseNotificationStream(resp.data);
    ns.setLastModified(resp.lastModified);
    if (lastModified == null)
      ns.setLastFullUpdateTimestamp(System.currentTimeMillis());
    return ns;
  }

  /**
   * Call this when you want to mark android notification as read - remove it
   */
  public void markAndroidNotificationsRead() {
    Utils.getNotificationManager(context).cancel(ANDROID_NOTIFICATION_ID);
  }

  /**
   * Call this when you want to reset notification alert in widgets (cancel highlight)
   */
  public void markAndroidWidgetsAsRead() {
    if (PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_EXISTS, false)) {
      PreferencesUtils.storeBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_HIGHLIGHT, false);
      updateWidgets();
    }
  }

  protected NotificationStream filterForAndroidNotification(NotificationStream ns) {
    NotificationStream ret = new NotificationStream();

    for (Notification n : ns) {
      String p = PreferencesUtils.getNotificationFilterForRepository(context, n.getRepositoryFullName(), true);
      if (!PreferencesUtils.PREF_NOTIFY_FILTER_NOTHING.equalsIgnoreCase(p)) {
        if (PreferencesUtils.PREF_NOTIFY_FILTER_ALL.equalsIgnoreCase(p) || !"subscribed".equalsIgnoreCase(n.getReason())) {
          ret.addNotification(n);
        }
      }
    }

    return ret;
  }

  protected void fireAndroidNotification(NotificationStream newStream, NotificationStream oldStream) {
    if (newStream == null || !PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_NOTIFY, true))
      return;

    Log.d(TAG, "fireAndroidNotification count before filter " + newStream.size());
    newStream = filterForAndroidNotification(newStream);
    Log.d(TAG, "fireAndroidNotification count after filter " + newStream.size());
    if (newStream.isNewNotification(oldStream)) {

      NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.github_notification)
          .setContentTitle(context.getString(R.string.an_title_more)).setPriority(NotificationCompat.PRIORITY_DEFAULT);
      mBuilder.setAutoCancel(true);

      ShortcutBadger.applyCount(context, newStream.size());

      if (newStream.size() > 1)
        mBuilder.setNumber(newStream.size());


      boolean allFromOne = newStream.allNotificationsFromSameRepository();

      if (newStream.size() == 1 || allFromOne) {
        // only one repository
        Notification n = newStream.get(0);
        Bitmap b = ImageLoader.getInstance(context).loadImageWithFileLevelCache(n.getRepositoryAvatarUrl());
        if (b != null) {
          mBuilder.setLargeIcon(b);
        } else {
          mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.github_notification));
        }
        mBuilder.setContentText(n.getRepositoryFullName());
      } else {
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.github_notification));
      }

      Intent resultIntent = null;
      if (newStream.size() == 1) {
        mBuilder.setContentTitle(context.getString(R.string.an_title_one));
        Notification n = newStream.get(0);
        mBuilder.setContentText(n.getRepositoryFullName() + ": " + n.getSubjectTitle());
        NotificationCompat.BigTextStyle btStyle = new NotificationCompat.BigTextStyle();
        btStyle.bigText(n.getSubjectTitle());
        btStyle.setSummaryText(n.getRepositoryFullName());
        mBuilder.setStyle(btStyle);
        Intent actionIntent = new Intent(context, MarkNotifiationAsReadReceiver.class);
        actionIntent.putExtra(MarkNotifiationAsReadReceiver.INTENT_EXTRA_KEY_ID, n.getId());
        mBuilder.addAction(R.drawable.ic_action_dismis_all, context.getString(R.string.action_mark_read),
            PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        resultIntent = new Intent(context, MainActivity.class);
      } else {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for (Notification n : newStream) {
          if (allFromOne) {
            inboxStyle.addLine(n.getSubjectTitle());
          } else {
            inboxStyle.addLine(n.getRepositoryFullName() + ": " + n.getSubjectTitle());
          }
        }
        if (allFromOne)
          inboxStyle.setSummaryText(newStream.get(0).getRepositoryFullName());
        else
          inboxStyle.setSummaryText(" ");
        mBuilder.setStyle(inboxStyle);

        Intent actionIntent = new Intent(context, MainActivity.class);
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        actionIntent.setAction(MainActivity.INTENT_ACTION_DISMISS_ALL);
        mBuilder
            .addAction(R.drawable.ic_action_dismis_all, context.getString(R.string.action_all_read), PendingIntent.getActivity(context, 0, actionIntent, 0));

        resultIntent = new Intent(context, MainActivity.class);
      }

      resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
      resultIntent.setAction(MainActivity.INTENT_ACTION_SHOW);
      PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);
      mBuilder.setContentIntent(resultPendingIntent);

      String nsound = PreferencesUtils.getString(context, PreferencesUtils.PREF_NOTIFY_SOUND, null);
      Log.d(TAG, "Notification sound from preference: " + nsound);
      if (nsound != null) {
        mBuilder.setSound(Uri.parse(nsound));
      }
      if (PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_NOTIFY_VIBRATE, true)) {
        mBuilder.setVibrate(new long[]{0, 300, 100, 150, 100, 150});
      }

      mBuilder.setLights(0xffffffff, 100, 4000);

      // mId allows you to update the notification later on.
      Utils.getNotificationManager(context).notify(ANDROID_NOTIFICATION_ID, mBuilder.build());
      ActivityTracker.sendEvent(context, ActivityTracker.CAT_NOTIF, "new_notif", "notif count: " + newStream.size(), Long.valueOf(newStream.size()));
    } else if (newStream.isEmpty()) {
      // #54 dismiss previous android notification if no any Github notification is available (as it was read on another device)
      Utils.getNotificationManager(context).cancel(ANDROID_NOTIFICATION_ID);
      ShortcutBadger.removeCount(context);
    }
  }

  protected void updateWidgetsFromBackgroundCheck(NotificationStream newStream, NotificationStream oldStream) {
    if (newStream == null)
      return;
    if (PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_EXISTS, false)) {
      if (newStream.isNewNotification(oldStream)) {
        PreferencesUtils.storeBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_HIGHLIGHT, true);
      }
      updateWidgets();
    }
  }

  protected void updateWidgets() {
    if (PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_EXISTS, false)) {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
      int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, UnreadAppWidgetProvider.class));
      Intent intent = new Intent(context, UnreadAppWidgetProvider.class);
      intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
      context.sendBroadcast(intent);
      Log.d(TAG, "Widget update Intent fired");
    }
  }

  public void flushPersistentStore() {
    persistFile.delete();
  }

}
