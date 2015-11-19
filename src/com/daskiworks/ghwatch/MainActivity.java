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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.daskiworks.ghwatch.LoginDialogFragment.LoginDialogListener;
import com.daskiworks.ghwatch.backend.GHConstants;
import com.daskiworks.ghwatch.backend.UnreadNotificationsService;
import com.daskiworks.ghwatch.backend.ViewDataReloadStrategy;
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.NotifCount;
import com.daskiworks.ghwatch.model.Notification;
import com.daskiworks.ghwatch.model.NotificationStreamViewData;
import com.daskiworks.ghwatch.model.StringViewData;

/**
 * Activity used to show list of Notifications.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class MainActivity extends ActivityBase implements LoginDialogListener, OnRefreshListener {

  private static final String STATE_FILTER_REPOSITORY = "STATE_FILTER_REPOSITORY";

  public static String INTENT_ACTION_RESET_FILTER = "com.daskiworks.ghwatch.ACTION_RESET_FILTER";
  public static String INTENT_ACTION_DISMISS_ALL = "com.daskiworks.ghwatch.ACTION_MARK_ALL_READ";
  public static String INTENT_ACTION_SHOW = "com.daskiworks.ghwatch.ACTION_SHOW";

  private static final String TAG = MainActivity.class.getSimpleName();

  // common fields
  private DataLoaderTask dataLoader;
  private ShowNotificationTask showNotificationTask;

  // view components
  private ListView notificationsListView;
  private NotificationListAdapter notificationsListAdapter;

  private ListView repositoriesListView;
  private NotificationRepositoriesListAdapter repositoriesListAdapter;

  // backend services
  private ImageLoader imageLoader;
  private UnreadNotificationsService unreadNotificationsService;

  // filters
  private String filterByRepository = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "onCreate() intent: " + getIntent());

    if (savedInstanceState != null) {
      filterByRepository = savedInstanceState.getString(STATE_FILTER_REPOSITORY);
    }

    setContentView(R.layout.activity_main);

    imageLoader = ImageLoader.getInstance(getApplicationContext());
    unreadNotificationsService = new UnreadNotificationsService(getBaseContext());

    initNavigationDrawer(NAV_DRAWER_ITEM_UNREAD_NOTIF);

    repositoriesListView = (ListView) findViewById(R.id.repositories_list);
    repositoriesListView.setOnItemClickListener(new RepositoriesListItemClickListener());

    // initialization of main content
    notificationsListView = (ListView) findViewById(R.id.list);
    notificationsListView.setVerticalFadingEdgeEnabled(true);
    SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(notificationsListView, new NotificationsListSwipeDismissListener());
    notificationsListView.setOnTouchListener(touchListener);
    // Setting this scroll listener is required to ensure that during ListView scrolling,
    // we don't look for swipes.
    notificationsListView.setOnScrollListener(touchListener.makeScrollListener());

    initSwipeLayout(this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putCharSequence(STATE_FILTER_REPOSITORY, filterByRepository);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Log.d(TAG, "onNewIntent() intent: " + getIntent());
    if (intent != null && INTENT_ACTION_RESET_FILTER.equals(intent.getAction())) {
      resetNotificationsFilter();
    }
    super.onNewIntent(intent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!checkUserLoggedIn()) {
      finish();
      return;
    }
    ActivityTracker.sendView(this, TAG);

    Intent intent = getIntent();
    Log.d(TAG, "onResume() intent: " + getIntent());
    if (intent != null && INTENT_ACTION_DISMISS_ALL.equals(intent.getAction())) {
      showMarkAllNotificationsAsReadDialog();
    } else {
      if (intent != null && INTENT_ACTION_RESET_FILTER.equals(intent.getAction())) {
        resetNotificationsFilter();
      }
      if (SupportAppDevelopmentDialogFragment.isAutoShowScheduled(this)) {
        showSupportAppDevelopmentDialog();
      }
    }
    intent.setAction(null);
    refreshList(ViewDataReloadStrategy.IF_TIMED_OUT, false);
    unreadNotificationsService.markAndroidWidgetsAsRead();
    unreadNotificationsService.markAndroidNotificationsRead();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_activity_actions, menu);
    MenuItem mi = menu.findItem(R.id.action_notifCheck);
    if (mi != null) {
      mi.setVisible(GHConstants.DEBUG);
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_all_read).setVisible(notificationsListAdapter != null && !notificationsListAdapter.isEmpty());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "onDestroy()");
    if (dataLoader != null)
      dataLoader.cancel(true);
    if (showNotificationTask != null) {
      Utils.dismissDialogSafe(showNotificationTask.progress);
      showNotificationTask.progress = null;
      showNotificationTask.cancel(true);
    }
    notifyDataSetChanged();
    super.onDestroy();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (super.onOptionsItemSelected(item)) {
      return true;
    }

    switch (item.getItemId()) {
    case R.id.action_all_read:
      showMarkAllNotificationsAsReadDialog();
      return true;
    case R.id.action_notifCheck:
      Toast.makeText(MainActivity.this, "New notification check started with empty store", Toast.LENGTH_SHORT).show();
      unreadNotificationsService.flushPersistentStore();
      unreadNotificationsService.newNotificationCheck();
      return true;
    default:
      return false;
    }
  }

  /**
   * Called when "Swipe to refresh" is finished.
   */
  @Override
  public void onRefresh() {
    ActivityTracker.sendEvent(this, ActivityTracker.CAT_UI, "unread_notifications_refresh", "", 0L);
    refreshList(ViewDataReloadStrategy.ALWAYS, false);
  }

  public void refreshList(ViewDataReloadStrategy reloadStrateg, boolean supressErrorMessages) {
    if (dataLoader == null)
      (dataLoader = new DataLoaderTask(reloadStrateg, supressErrorMessages)).execute();
  }

  protected void onDrawerMenuItemSelected(int position) {
    if (position == NAV_DRAWER_ITEM_UNREAD_NOTIF) {
      resetNotificationsFilter();
    }
    super.onDrawerMenuItemSelected(position);
  }

  protected void resetNotificationsFilter() {
    filterByRepository = null;
    if (repositoriesListAdapter != null) {
      repositoriesListAdapter.setSelectionForFilter(repositoriesListView, filterByRepository);
    }
    if (notificationsListAdapter != null) {
      notificationsListAdapter.setFilterByRepository(filterByRepository);
    }
  }

  private final class NotificationsListSwipeDismissListener implements SwipeDismissListViewTouchListener.DismissCallbacks {
    @Override
    public boolean canDismiss(int position) {
      return true;
    }

    @Override
    public void onDismiss(ListView listView, int[] reverseSortedPositions) {
      for (int position : reverseSortedPositions) {
        Notification tr = (Notification) notificationsListAdapter.getItem(position);
        if (tr != null) {
          new MarkNotificationAsReadTask().execute(tr.getId());
          ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mark_read_swipe", "", 0L);
          notificationsListAdapter.removeNotificationByPosition(position);
        }
      }
      notifyDataSetChanged();
    }
  }

  private final class NotificationsListItemClickListener implements OnItemClickListener {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      Notification notification = (Notification) notificationsListAdapter.getItem(position);
      if (notification != null) {
        showNotificationTask = new ShowNotificationTask();
        showNotificationTask.execute(notification);
        ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_show", "", 0L);
      }
    }
  }

  private final class NotificationsListItemMenuClickListener implements NotificationListAdapter.OnItemMenuClickedListener {
    @Override
    public boolean onMenuItemClick(Notification notification, MenuItem item) {
      switch (item.getItemId()) {
      case R.id.action_mark_read:
        new MarkNotificationAsReadTask().execute(notification.getId());
        notificationsListAdapter.removeNotificationById(notification.getId());
        ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mark_read_menu", "", 0L);
        notifyDataSetChanged();
        return true;
      case R.id.action_mute_thread:
        new MuteNotificationThreadTask().execute(notification.getId());
        notificationsListAdapter.removeNotificationById(notification.getId());
        ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mute_thread", "", 0L);
        notifyDataSetChanged();
        return true;
      default:
        return false;
      }
    }
  }

  private final class RepositoriesListItemClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (repositoriesListView != null) {
        NotifCount nc = (NotifCount) repositoriesListAdapter.getItem(position);
        if (nc != null) {
          if (filterByRepository != null && filterByRepository.equals(nc.title)) {
            filterByRepository = null;
            repositoriesListView.setItemChecked(position, false);
          } else {
            filterByRepository = nc.title;
          }
          if (notificationsListAdapter != null) {
            notificationsListAdapter.setFilterByRepository(filterByRepository);
          }
          ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_filter_by_repository", filterByRepository != null ? "SET"
              : "RESET", 0L);
        }
      }
      navigationDrawerClose();

    }
  }

  private final class DataLoaderTask extends AsyncTask<String, String, NotificationStreamViewData> {

    ViewDataReloadStrategy reloadStrategy = null;
    boolean supressErrorMessages = false;

    DataLoaderTask(ViewDataReloadStrategy reloadStrategy, boolean supressErrorMessages) {
      this.reloadStrategy = reloadStrategy;
      this.supressErrorMessages = supressErrorMessages;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected NotificationStreamViewData doInBackground(String... args) {
      return unreadNotificationsService.getNotificationStreamForView(reloadStrategy);
    }

    @Override
    protected void onPostExecute(final NotificationStreamViewData viewData) {
      try {
        if (isCancelled() || viewData == null)
          return;
        if (viewData.loadingStatus != LoadingStatus.OK) {
          if (!supressErrorMessages)
            showServerCommunicationErrorAllertDialog(viewData.loadingStatus, viewData.notificationStream != null);
        }
        if (viewData.notificationStream != null) {

          if (notificationsListAdapter != null) {
            notificationsListAdapter.setNotificationStream(viewData.notificationStream);
            notificationsListAdapter.notifyDataSetChanged();
          } else {
            notificationsListAdapter = new NotificationListAdapter(MainActivity.this, viewData.notificationStream, imageLoader);
            notificationsListView.setAdapter(notificationsListAdapter);
            notificationsListAdapter.setOnItemMenuClickedListener(new NotificationsListItemMenuClickListener());
            notificationsListView.setOnItemClickListener(new NotificationsListItemClickListener());
          }

          if (repositoriesListAdapter != null) {
            repositoriesListAdapter.setNotificationStream(viewData.notificationStream);
            repositoriesListAdapter.notifyDataSetChanged();
          } else {
            repositoriesListAdapter = new NotificationRepositoriesListAdapter(MainActivity.this, viewData.notificationStream);
            repositoriesListView.setAdapter(repositoriesListAdapter);
          }
          if (!repositoriesListAdapter.setSelectionForFilter(repositoriesListView, filterByRepository)) {
            // repo no more in data so reset filter
            filterByRepository = null;
          }
          if (notificationsListAdapter != null) {
            notificationsListAdapter.setFilterByRepository(filterByRepository);
          }

          if (viewData.notificationStream.size() == 0) {
            swipeLayout2.setVisibility(View.VISIBLE);
            swipeLayout.setVisibility(View.GONE);
          } else {
            swipeLayout2.setVisibility(View.GONE);
            swipeLayout.setVisibility(View.VISIBLE);
          }
        }
      } finally {
        dataLoader = null;
        swipeLayout.setRefreshing(false);
        swipeLayout2.setRefreshing(false);
        hideInitialProgressBar();
        invalidateOptionsMenu();
      }
    }
  }

  private final class ShowNotificationTask extends AsyncTask<Notification, String, StringViewData> {

    ProgressDialog progress;

    @Override
    protected void onPreExecute() {
      progress = ProgressDialog.show(MainActivity.this, null, getString(R.string.progress_get_view_url_title), true, true, new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          ShowNotificationTask.this.cancel(true);
        }
      });
    }

    @Override
    protected StringViewData doInBackground(Notification... params) {
      return unreadNotificationsService.getGithubDataHtmlUrl(params[0]);
    }

    @Override
    protected void onPostExecute(StringViewData result) {
      try {
        if (isCancelled() || result == null) {
          Utils.dismissDialogSafe(progress);
          return;
        }
        if (result.loadingStatus != LoadingStatus.OK) {
          Utils.dismissDialogSafe(progress);
          showServerCommunicationErrorAllertDialog(result.loadingStatus, false);
        } else if (result.data != null) {
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.data));
          startActivity(browserIntent);
          Utils.dismissDialogSafe(progress);
        }
      } finally {
        progress = null;
        MainActivity.this.showNotificationTask = null;
      }
    }
  }

  private final class MarkNotificationAsReadTask extends AsyncTask<Long, String, BaseViewData> {

    @Override
    protected BaseViewData doInBackground(Long... params) {

      return unreadNotificationsService.markNotificationAsRead(params[0]);
    }

    @Override
    protected void onPostExecute(final BaseViewData viewData) {
      if (isCancelled() || viewData == null)
        return;
      if (viewData.loadingStatus != LoadingStatus.OK) {
        showServerCommunicationErrorAllertDialog(viewData.loadingStatus, false);
        refreshList(ViewDataReloadStrategy.NEVER, true);
      } else {
        // Toast.makeText(MainActivity.this, R.string.message_notification_marked_read, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private final class MuteNotificationThreadTask extends AsyncTask<Long, String, BaseViewData> {

    @Override
    protected BaseViewData doInBackground(Long... params) {

      return unreadNotificationsService.muteNotificationThread(params[0]);
    }

    @Override
    protected void onPostExecute(final BaseViewData viewData) {
      if (isCancelled() || viewData == null)
        return;
      if (viewData.loadingStatus != LoadingStatus.OK) {
        showServerCommunicationErrorAllertDialog(viewData.loadingStatus, false);
        refreshList(ViewDataReloadStrategy.NEVER, true);
      } else {
        Toast.makeText(MainActivity.this, R.string.message_notification_thread_muted, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private final class MarkAllNotificationsAsReadTask extends AsyncTask<Object, String, BaseViewData> {

    @Override
    protected void onPreExecute() {
      swipeLayout.setRefreshing(true);
    }

    @Override
    protected BaseViewData doInBackground(Object... params) {
      return unreadNotificationsService.markAllNotificationsAsRead(filterByRepository);
    }

    @Override
    protected void onPostExecute(final BaseViewData viewData) {
      boolean supressErrorMessage = false;
      ViewDataReloadStrategy reloadStrategy = ViewDataReloadStrategy.ALWAYS;
      try {
        if (isCancelled() || viewData == null)
          return;
        if (viewData.loadingStatus != LoadingStatus.OK) {
          showServerCommunicationErrorAllertDialog(viewData.loadingStatus, false);
          supressErrorMessage = true;
          reloadStrategy = ViewDataReloadStrategy.NEVER;
        } else {
          supressErrorMessage = false;
          reloadStrategy = ViewDataReloadStrategy.ALWAYS;
          // Toast.makeText(MainActivity.this, R.string.message_notifications_all_marked_read, Toast.LENGTH_SHORT).show();
        }
      } finally {
        refreshList(reloadStrategy, supressErrorMessage);
      }
    }
  }

  private void showServerCommunicationErrorAllertDialog(LoadingStatus loadingStatus, boolean showStaledDataWarning) {
    StringBuilder sb = new StringBuilder();
    sb.append(getString(loadingStatus.getResId()));
    if (showStaledDataWarning) {
      sb.append("\n").append(getString(R.string.message_staled_data));
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(sb).setCancelable(true).setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
      }
    });

    builder.create().show();
  }

  private void showMarkAllNotificationsAsReadDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.message_confirm_notifications_all_mark_read_title);
    if (filterByRepository == null) {
      builder.setMessage(R.string.message_confirm_notifications_all_mark_read);
    } else {
      builder.setMessage(R.string.message_confirm_notifications_all_mark_read_repo);
    }
    builder.setCancelable(true);
    builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notifications_mark_read_all", "", 0L);
        new MarkAllNotificationsAsReadTask().execute();
      }
    });
    builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
      }
    });
    builder.create().show();
  }

  @Override
  public void afterLoginSuccess(LoginDialogFragment dialog) {
    refreshList(ViewDataReloadStrategy.ALWAYS, false);
  }

  protected void notifyDataSetChanged() {
    if (notificationsListAdapter != null)
      notificationsListAdapter.notifyDataSetChanged();
    if (repositoriesListAdapter != null) {
      repositoriesListAdapter.notifyDataSetChanged();
    }
  }
}
