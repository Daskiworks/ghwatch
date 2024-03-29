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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.daskiworks.ghwatch.ActivityTracker;
import com.daskiworks.ghwatch.MainActivity;
import com.daskiworks.ghwatch.R;
import com.daskiworks.ghwatch.UnreadAppWidgetProvider;
import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.alarm.AndroidNotifiationActionsReceiver;
import com.daskiworks.ghwatch.auth.AuthenticationManager;
import com.daskiworks.ghwatch.backend.RemoteSystemClient.Response;
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.Notification;
import com.daskiworks.ghwatch.model.NotificationStream;
import com.daskiworks.ghwatch.model.NotificationStreamViewData;
import com.daskiworks.ghwatch.model.NotificationViewData;
import com.daskiworks.ghwatch.model.Repository;
import com.daskiworks.ghwatch.model.StringViewData;
import com.daskiworks.ghwatch.model.WatchedRepositoriesViewData;


import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.NoRouteToHostException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.auth.AuthenticationException;
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
   * Reload from server is forced automatically for {@link ViewDataReloadStrategy#IF_TIMED_OUT} mode if data in persistent store are older than this timeout [millis]
   */
  private static final long FORCE_VIEW_RELOAD_AFTER = 30 * Utils.MILLIS_MINUTE;

  /**
   * Id of android notification so we can update it. In case of bundled notifications we use it for main notification.
   */
  public static final int ANDROID_NOTIFICATION_MAIN_ID = 0;
  /**
   * Group key for Bundled Android notification used for Noughat+
   */
  private static final String ANDROID_NOTIFICATION_GROUP_KEY = "GHWatch";
  /**
   * ID of the notification channel used for github notifications
   */
  private static final String CHANNEL_ID = "nch_github_not";

  // few fields initialized in constructor
  private final Context context;
  private final File persistFile;
  private final AuthenticationManager authenticationManager;

  // few data loaders - initialized lazily when necessary only
  private NotificationDetailLoader notificationDetailLoader;
  private NotificationViewUrlLoader notificationViewUrlLoader;
  private final int notificationColor;

  /**
   * Create service.
   *
   * @param context this service runs in
   */
  public UnreadNotificationsService(Context context) {
    this.context = context;
    this.persistFile = context.getFileStreamPath(persistFileName);
    this.authenticationManager = AuthenticationManager.getInstance();
    this.notificationColor = context.getResources().getColor(R.color.apptheme_colorPrimary);
    createNotificationChannel();
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
        Log.w(TAG, "NotificationStream loading failed due to data format problem: " + e.getMessage(), e);
      } catch (NoRouteToHostException e) {
        nswd.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
        Log.d(TAG, "NotificationStream loading failed due to connection not available.");
      } catch (AuthenticationException e) {
        nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
        Log.d(TAG, "NotificationStream loading failed due to authentication problem: " + e.getMessage());
      } catch (IOException e) {
        nswd.loadingStatus = LoadingStatus.CONN_ERROR;
        Log.w(TAG, "NotificationStream loading failed due to connection problem: " + e.getMessage());
      } catch (JSONException e) {
        nswd.loadingStatus = LoadingStatus.DATA_ERROR;
        Log.w(TAG, "NotificationStream loading failed due to data format problem: " + e.getMessage());
      } catch (Exception e) {
        nswd.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
        Log.e(TAG, "NotificationStream loading failed due to: " + e.getMessage(), e);
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
      Log.w(TAG, "NotificationRead marking failed due to connection problem: " + e.getMessage());
      nswd.loadingStatus = LoadingStatus.CONN_ERROR;
    } catch (Exception e) {
      Log.e(TAG, "NotificationRead marking failed due to: " + e.getMessage(), e);
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
      Log.w(TAG, "Notification thread mute failed due to connection problem: " + e.getMessage());
      nswd.loadingStatus = LoadingStatus.CONN_ERROR;
    } catch (Exception e) {
      Log.e(TAG, "Notification thread mute failed due to: " + e.getMessage(), e);
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
      Log.w(TAG, "NotificationRead marking failed due to connection problem: " + e.getMessage());
      nswd.loadingStatus = LoadingStatus.CONN_ERROR;
    } catch (Exception e) {
      Log.e(TAG, "NotificationRead marking failed due to: " + e.getMessage(), e);
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

  public StringViewData getNotificationViewUrl(long notificationId) {
    Notification n = null;
    NotificationStream oldNs = Utils.readFromStore(TAG, context, persistFile);
    if (oldNs != null) {
      n = oldNs.getNotificationById(notificationId);
    }
    if (n != null)
      return getNotificationViewUrl(n);
    else
      return null;
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
        Log.w(TAG, "Notification html view URL loading failed due to data format problem: no 'html_url' field in response");
      }
    } else {
      nswd.loadingStatus = LoadingStatus.DATA_ERROR;
    }
    return nswd;
  }


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
      Log.d(TAG, "Notification check failed due to: " + e.getMessage());
    } catch (Exception e) {
      Log.w(TAG, "Notification check failed due to: " + e.getMessage());
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

    NotificationStreamParser.IRepoVisibilityAdapter rva = createRepoVisibilityAdapter();

    String url = prepareNotificationLoadingURL(rva);

    Map<String, String> headers = null;
    if (lastModified != null) {
      headers = new HashMap<String, String>();
      headers.put("If-Modified-Since", lastModified);
    }

    Response<JSONArray> resp = RemoteSystemClient.getJSONArrayFromUrl(context, authenticationManager.getGhApiCredentials(context), url, headers);

    if (resp.notModified)
      return null;

    NotificationStream ns = NotificationStreamParser.parseNotificationStream(null, resp.data, rva);
    ns.setLastModified(resp.lastModified);
    if (lastModified == null)
      ns.setLastFullUpdateTimestamp(System.currentTimeMillis());

    //handle paging
    while (resp.linkNext != null) {
      resp = RemoteSystemClient.getJSONArrayFromUrl(context, authenticationManager.getGhApiCredentials(context), resp.linkNext, headers);
      NotificationStreamParser.parseNotificationStream(ns, resp.data, rva);
    }

    return ns;
  }

  @NonNull
  private String prepareNotificationLoadingURL(NotificationStreamParser.IRepoVisibilityAdapter rva) {
    String url = URL_NOTIFICATIONS;
    //#80 detect which URL should be used, call repo based url if only one repo is visible
    WatchedRepositoriesService wrs = new WatchedRepositoriesService(context);
    WatchedRepositoriesViewData wr = wrs.getWatchedRepositoriesForView(ViewDataReloadStrategy.IF_TIMED_OUT);
    if (wr.loadingStatus == LoadingStatus.OK) {
      Set<String> visibleRepos = new HashSet<String>();
      for (Repository r : wr.repositories) {
        if (rva.isRepoVisibile(r.getRepositoryFullName())) {
          visibleRepos.add(r.getRepositoryFullName());
        }
      }
      if (visibleRepos.size() == 1) {
        url = GHConstants.URL_BASE + "/repos/" + visibleRepos.iterator().next() + "/notifications";
      }
    }
    Log.d(TAG, "Notification loading URL: " + url);
    return url;
  }

  @NonNull
  private NotificationStreamParser.IRepoVisibilityAdapter createRepoVisibilityAdapter() {
    return new NotificationStreamParser.IRepoVisibilityAdapter() {
      @Override
      public boolean isRepoVisibile(String repoFullName) {
        return PreferencesUtils.PREF_REPO_VISIBILITY_VISIBLE.equals(PreferencesUtils.getRepoVisibilityForRepository(context, repoFullName, true));
      }
    };
  }

  /**
   * Call this when you want to mark android notification issued by this app as read - remove it
   */
  public void markAndroidNotificationsRead() {
    Utils.getNotificationManager(context).cancel(ANDROID_NOTIFICATION_MAIN_ID);
  }

  /**
   * Call this when you want to mark android notification issued by this app as read - remove it
   */
  public void markAndroidNotificationBundledDetailRead(int androidNotificationIdToCancel) {
    Utils.getNotificationManager(context).cancel(androidNotificationIdToCancel);
    long c = PreferencesUtils.getLong(context, NUM_OF_BUNDLED_ANDROID_NOTIFICATIONS,0);
    if(c>1){
      PreferencesUtils.storeLong(context, NUM_OF_BUNDLED_ANDROID_NOTIFICATIONS, --c);
    } else {
      markAndroidNotificationsRead();
    }

    int bc = PreferencesUtils.getInt(context, NUM_OF_BADGED_ANDROID_NOTIFICATIONS,0);
    if(bc>1){
      PreferencesUtils.storeInt(context, NUM_OF_BADGED_ANDROID_NOTIFICATIONS, --bc);
      ShortcutBadger.applyCount(context, bc);
    } else {
      ShortcutBadger.removeCount(context);
    }

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
      ShortcutBadger.applyCount(context, newStream.size());
      PreferencesUtils.storeInt(context, NUM_OF_BADGED_ANDROID_NOTIFICATIONS, newStream.size());

      //TEST with only one notification
      if(false) {
        Notification on = newStream.get(0);
        Notification on2 = newStream.get(1);
        newStream = new NotificationStream();
        newStream.addNotification(on);
        newStream.addNotification(on2);
      }

      fireAndroidNotificationBundledStyle(newStream);

      ActivityTracker.sendEvent(context, ActivityTracker.CAT_NOTIF, "new_notif", "notif count: " + newStream.size(), Long.valueOf(newStream.size()));
    } else if (newStream.isEmpty()) {
      // #54 dismiss previous android notification if no any Github notification is available (as it was read on another device)
      Utils.getNotificationManager(context).cancel(ANDROID_NOTIFICATION_MAIN_ID);
      ShortcutBadger.removeCount(context);
    }
  }

  protected void fireAndroidNotificationBundledStyle(NotificationStream newStream) {
    Log.i(TAG, "Going to fire Noughat Bundled style notification");
    NotificationManager notificationManager = Utils.getNotificationManager(context);
    android.app.Notification summary = buildAndroidNotificationBundledStyleSummary(newStream.get(0).getUpdatedAt());
    notificationManager.notify(ANDROID_NOTIFICATION_MAIN_ID, summary);
    int i = 0;
    for (Notification n : newStream) {
      android.app.Notification notification = buildAndroidNotificationBundledStyleDetail(n);
      notificationManager.notify((int) n.getId(), notification);
      if (++i == 49)
        break;
    }

    PreferencesUtils.storeLong(context, NUM_OF_BUNDLED_ANDROID_NOTIFICATIONS,i);
  }

  private static final String NUM_OF_BUNDLED_ANDROID_NOTIFICATIONS = "NUM_OF_BUNDLED_ANDROID_NOTIFICATIONS";
  private static final String NUM_OF_BADGED_ANDROID_NOTIFICATIONS = "NUM_OF_BADGED_ANDROID_NOTIFICATIONS";

  private android.app.Notification buildAndroidNotificationBundledStyleSummary(Date timestamp) {
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.an_title_more))
            .setSmallIcon(R.drawable.notification)
            .setGroup(ANDROID_NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)
            .setCategory(android.app.Notification.CATEGORY_SOCIAL)
            .setColor(notificationColor)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setCategory(android.app.Notification.CATEGORY_SOCIAL);
    if (timestamp != null) {
      mBuilder.setWhen(timestamp.getTime()).setShowWhen(true);
    }
    buildNotificationAlerting(mBuilder);
    //open GH::watch on click
    buildNotificationAddContentIntentToOpenApp(mBuilder, new Intent(context, MainActivity.class));
    return mBuilder.build();
  }

  private void createNotificationChannel() {
    CharSequence name = context.getString(R.string.an_channel_name);
    String description = context.getString(R.string.an_channel_description);
    int importance = NotificationManager.IMPORTANCE_DEFAULT;
    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
    channel.setDescription(description);
    // Register the channel with the system; you can't change the importance
    // or other notification behaviors after this
    NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
    notificationManager.createNotificationChannel(channel);
  }

  private android.app.Notification buildAndroidNotificationBundledStyleDetail(Notification n) {
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(n.getRepositoryFullName())
            .setContentText(n.getSubjectTitle())
            .setSmallIcon(R.drawable.notification)
            .setCategory(android.app.Notification.CATEGORY_SOCIAL)
            .setColor(notificationColor)
            .setGroup(ANDROID_NOTIFICATION_GROUP_KEY);
    NotificationCompat.BigTextStyle btStyle = new NotificationCompat.BigTextStyle();
    btStyle.bigText(n.getSubjectTitle());
    btStyle.setSummaryText(n.getRepositoryFullName());
    mBuilder.setStyle(btStyle);
    Bitmap b = ImageLoader.getInstance(context).loadImageWithFileLevelCache(n.getRepositoryAvatarUrl());
    if (b != null) {
      mBuilder.setLargeIcon(b);
    }
    if (n.getUpdatedAt() != null) {
      mBuilder.setWhen(n.getUpdatedAt().getTime()).setShowWhen(true);
    }
    buildNotificationActionMarkOneAsRead(mBuilder, n, true);
    buildNotificationActionMuteThreadOne(mBuilder, n, true);
    buildNotificationSetContetnIntentShowDetail(mBuilder, n, true);
    buildNotificationDeletedIntent(mBuilder,n,true);
    return mBuilder.build();
  }

  protected void buildNotificationActionMarkOneAsRead(NotificationCompat.Builder mBuilder, Notification n, boolean bundled) {
    Intent actionIntent = new Intent(context, AndroidNotifiationActionsReceiver.class);
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_NOTIFICATION_ID, n.getId());
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_IS_BUNDLED, bundled);
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_ACTION, AndroidNotifiationActionsReceiver.INTENT_EXTRA_VALUE_ACTION_MARKASREAD);
    mBuilder.addAction(R.drawable.ic_done_white_36dp, context.getString(R.string.action_mark_read),
            PendingIntent.getBroadcast(context, generatePendingIntentRequestCode(), actionIntent, PendingIntent.FLAG_UPDATE_CURRENT));
  }

  protected void buildNotificationActionMuteThreadOne(NotificationCompat.Builder mBuilder, Notification n, boolean bundled) {
    Intent actionIntent = new Intent(context, AndroidNotifiationActionsReceiver.class);
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_NOTIFICATION_ID, n.getId());
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_IS_BUNDLED, bundled);
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_ACTION, AndroidNotifiationActionsReceiver.INTENT_EXTRA_VALUE_ACTION_MUTE);
    mBuilder.addAction(R.drawable.ic_clear_white_36dp, context.getString(R.string.action_mute_thread),
            PendingIntent.getBroadcast(context, generatePendingIntentRequestCode(), actionIntent, PendingIntent.FLAG_UPDATE_CURRENT));
  }

  protected void buildNotificationDeletedIntent(NotificationCompat.Builder mBuilder, Notification n, boolean bundled) {
    Intent actionIntent = new Intent(context, AndroidNotifiationActionsReceiver.class);
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_NOTIFICATION_ID, n.getId());
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_IS_BUNDLED, bundled);
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_ACTION, AndroidNotifiationActionsReceiver.INTENT_EXTRA_VALUE_ACTION_DELETED);
    mBuilder.setDeleteIntent(PendingIntent.getBroadcast(context, generatePendingIntentRequestCode(), actionIntent, PendingIntent.FLAG_UPDATE_CURRENT));
  }

  protected void buildNotificationSetContetnIntentShowDetail(NotificationCompat.Builder mBuilder, Notification n, boolean bundled) {
    Intent actionIntent = new Intent(context, AndroidNotifiationActionsReceiver.class);
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_NOTIFICATION_ID, n.getId());
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_IS_BUNDLED, bundled);
    actionIntent.putExtra(AndroidNotifiationActionsReceiver.INTENT_EXTRA_KEY_ACTION, AndroidNotifiationActionsReceiver.INTENT_EXTRA_VALUE_ACTION_SHOW);
    mBuilder.setContentIntent(PendingIntent.getBroadcast(context, generatePendingIntentRequestCode(), actionIntent, PendingIntent.FLAG_UPDATE_CURRENT));
  }

  private static int piid = 1;

  protected int generatePendingIntentRequestCode() {
    if (piid == Integer.MAX_VALUE)
      piid = 0;
    return piid++;
  }

  private void buildNotificationAddContentIntentToOpenApp(NotificationCompat.Builder mBuilder, Intent resultIntent) {
    resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    resultIntent.setAction(MainActivity.INTENT_ACTION_SHOW);
    PendingIntent resultPendingIntent = PendingIntent.getActivity(context, generatePendingIntentRequestCode(), resultIntent, PendingIntent.FLAG_IMMUTABLE);
    mBuilder.setContentIntent(resultPendingIntent);
  }

  protected void buildNotificationAlerting(NotificationCompat.Builder mBuilder) {
    String nsound = PreferencesUtils.getString(context, PreferencesUtils.PREF_NOTIFY_SOUND, null);
    Log.d(TAG, "Notification sound from preference: " + nsound);
    if (nsound != null) {
      mBuilder.setSound(Uri.parse(nsound));
    }
    if (PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_NOTIFY_VIBRATE, true)) {
      mBuilder.setVibrate(new long[]{0, 300, 100, 150, 100, 150});
    }

    mBuilder.setLights(0xffffffff, 100, 4000);
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
