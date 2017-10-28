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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import com.daskiworks.ghwatch.LoginDialogFragment.LoginDialogListener;
import com.daskiworks.ghwatch.backend.GHConstants;
import com.daskiworks.ghwatch.backend.PreferencesUtils;
import com.daskiworks.ghwatch.backend.UnreadNotificationsService;
import com.daskiworks.ghwatch.backend.ViewDataReloadStrategy;
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.NotifCount;
import com.daskiworks.ghwatch.model.Notification;
import com.daskiworks.ghwatch.model.NotificationStreamViewData;
import com.daskiworks.ghwatch.model.StringViewData;
import com.daskiworks.ghwatch.view.SwipeDismissListViewTouchListener;

import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Activity used to show list of Notifications.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
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

  private ListView repositoriesListViewTablet;
  private NotificationRepositoriesListAdapter repositoriesListAdapterTablet;

  // backend services
  private ImageLoader imageLoader;
  private UnreadNotificationsService unreadNotificationsService;

  // filters
  private String filterByRepository = null;
  private NotificationStreamViewData viewDataCurrent;

  public NotificationStreamViewData getViewData() {
    return viewDataCurrent;
  }

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

    initNavigationDrawer(R.id.nav_unread);

    repositoriesListViewTablet = (ListView) findViewById(R.id.repositories_list);
    if (repositoriesListViewTablet != null)
      repositoriesListViewTablet.setOnItemClickListener(new RepositoriesListItemClickListener());

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

  private static boolean refreshOnNextResume;

  /**
   * Call if you want shown data to be fully refreshed on next resume of this activity
   */
  public static void refreshInNextResume() {
    refreshOnNextResume = true;
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
      if (NewVersionInfoDialogFragment.isShowScheduled(this)) {
        showDialog(new NewVersionInfoDialogFragment());
      } else if (SupportAppDevelopmentDialogFragment.isAutoShowScheduled(this)) {
        showSupportAppDevelopmentDialog();
      }
    }
    if (intent != null)
      intent.setAction(null);
    refreshList(refreshOnNextResume ? ViewDataReloadStrategy.ALWAYS : ViewDataReloadStrategy.IF_TIMED_OUT, false);
    refreshOnNextResume = false;
    unreadNotificationsService.markAndroidWidgetsAsRead();
    unreadNotificationsService.markAndroidNotificationsRead();
    ShortcutBadger.removeCount(getApplicationContext());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_activity_actions, menu);
    setDebugMenuItemVisibility(menu, R.id.action_notifCheck);
    setDebugMenuItemVisibility(menu, R.id.action_donationTogle);
    if (repositoriesListViewTablet != null) {
      menu.findItem(R.id.action_open_filter_dialog).setVisible(false);
    }
    return super.onCreateOptionsMenu(menu);
  }

  protected void setDebugMenuItemVisibility(Menu menu, int id) {
    MenuItem mi = menu.findItem(id);
    if (mi != null) {
      mi.setVisible(GHConstants.DEBUG);
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_open_filter_dialog).setVisible(repositoriesListViewTablet == null && notificationsListAdapter != null && !notificationsListAdapter.isEmpty());
    menu.findItem(R.id.action_all_read).setVisible(notificationsListAdapter != null && !notificationsListAdapter.isEmpty());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "onDestroy()");
    if (dataLoader != null)
      dataLoader.cancel(true);
    if (notificationsListAdapter != null)
      notificationsListAdapter.cancel();
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
      case R.id.action_open_filter_dialog:
        if (viewDataCurrent != null) {
          showRepositoryFilterDialog();
        }
        return true;
      case R.id.action_all_read:
        showMarkAllNotificationsAsReadDialog();
        return true;
      case R.id.action_notifCheck:
        Toast.makeText(MainActivity.this, "New notification check started with empty store", Toast.LENGTH_SHORT).show();
        unreadNotificationsService.flushPersistentStore();
        unreadNotificationsService.newNotificationCheck();
        return true;
      case R.id.action_donationTogle:
        Long l = PreferencesUtils.readDonationTimestamp(this);
        if (l == null) {
          l = System.currentTimeMillis();
        } else {
          l = null;
        }
        PreferencesUtils.storeDonationTimestamp(this, l);
        Toast.makeText(MainActivity.this, "Donation status toggled to " + (l != null ? "on" : "off"), Toast.LENGTH_SHORT).show();
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
      (dataLoader = new DataLoaderTask(reloadStrateg, supressErrorMessages)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  protected void onDrawerMenuItemSelected(MenuItem position) {
    if (position.getItemId() == R.id.nav_unread) {
      resetNotificationsFilter();
    }
    super.onDrawerMenuItemSelected(position);
  }

  protected void resetNotificationsFilter() {
    filterByRepository = null;
    if (repositoriesListAdapterTablet != null) {
      repositoriesListAdapterTablet.setSelectionForFilter(repositoriesListViewTablet, filterByRepository);
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
          new MarkNotificationAsReadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tr.getId());
          ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mark_read_swipe", "", 0L);
          notificationsListAdapter.removeNotificationByPosition(position);
        }
      }
      notifyDataSetChanged();
    }
  }

  private final class NotificationsListItemClickListener implements OnItemClickListener {

    private CheckBox rememberCheckbox;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
      final Notification notification = (Notification) notificationsListAdapter.getItem(position);
      if (notification != null) {
        String mrow = PreferencesUtils.getMarkReadOnShow(MainActivity.this);
        if (PreferencesUtils.PREF_MARK_READ_ON_SHOW_YES.equals(mrow)) {
          showNotification(notification, position, true);
        } else if (PreferencesUtils.PREF_MARK_READ_ON_SHOW_NO.equals(mrow)) {
          showNotification(notification, position, false);
        } else {
          AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
          builder.setTitle(R.string.dialog_mnar_text)
                  .setCancelable(true)
                  .setPositiveButton(R.string.dialog_mnar_btn_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                      if (rememberCheckbox != null && rememberCheckbox.isChecked()) {
                        PreferencesUtils.setMarkReadOnShow(MainActivity.this, PreferencesUtils.PREF_MARK_READ_ON_SHOW_YES);
                      }
                      ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mark_read_on_show_dialog_yes", null, 0L);
                      showNotification(notification, position, true);
                    }
                  })
                  .setNegativeButton(R.string.dialog_mnar_btn_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                      if (rememberCheckbox != null && rememberCheckbox.isChecked()) {
                        PreferencesUtils.setMarkReadOnShow(MainActivity.this, PreferencesUtils.PREF_MARK_READ_ON_SHOW_NO);
                      }
                      ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mark_read_on_show_dialog_no", null, 0L);
                      showNotification(notification, position, false);
                    }
                  });

          LayoutInflater inflater = MainActivity.this.getLayoutInflater();
          View dialogView = inflater.inflate(R.layout.notification_mark_read_on_show_dialog, null);
          builder.setView(dialogView);
          rememberCheckbox = (CheckBox) dialogView.findViewById(R.id.remember);

          AlertDialog alert = builder.create();
          ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mark_read_on_show_dialog_show", null, 0L);
          alert.show();
        }
      }
    }
  }

  protected void showNotification(Notification notification, int position, boolean markAsReadOnShow) {
    showNotificationTask = new ShowNotificationTask(notification, markAsReadOnShow, position);
    showNotificationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_show", "", 0L);
  }

  protected void markNotificationAsReadOnShow(int position, Notification notification) {
    new MarkNotificationAsReadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, notification.getId());
    ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mark_read_on_show", "", 0L);
    notificationsListAdapter.removeNotificationByPosition(position);
    notifyDataSetChanged();
  }

  private final class NotificationsListItemMenuClickListener implements NotificationListAdapter.OnItemMenuClickedListener {
    @Override
    public boolean onMenuItemClick(Notification notification, MenuItem item) {
      switch (item.getItemId()) {
        case R.id.action_mark_read:
          new MarkNotificationAsReadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, notification.getId());
          notificationsListAdapter.removeNotificationById(notification.getId());
          ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_mark_read_menu", "", 0L);
          notifyDataSetChanged();
          return true;
        case R.id.action_mute_thread:
          new MuteNotificationThreadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, notification.getId());
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
      if (repositoriesListViewTablet != null) {
        NotifCount nc = (NotifCount) repositoriesListAdapterTablet.getItem(position);
        if (nc != null) {
          if (!setFilterByRepository(nc.title)) {
            repositoriesListViewTablet.setItemChecked(position, false);
          }
        }
      }
    }
  }

  /**
   * Set filer by repository to the activity. If called for existing filter it is reset.
   *
   * @param repositoryName name of the repository to set
   * @return true if filter ser, false if reset by this call.
   */
  public boolean setFilterByRepository(String repositoryName) {
    boolean ret = true;
    if (filterByRepository != null && filterByRepository.equals(repositoryName)) {
      filterByRepository = null;
      ret = false;
    } else {
      filterByRepository = repositoryName;
    }
    if (notificationsListAdapter != null) {
      notificationsListAdapter.setFilterByRepository(filterByRepository);
    }
    ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "notification_filter_by_repository", filterByRepository != null ? "SET"
            : "RESET", 0L);
    return ret;
  }

  /**
   * Get current filter by repository.
   *
   * @return name of repo we currently filter over
   */
  public String getFilterByRepository() {
    return filterByRepository;
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

          viewDataCurrent = viewData;

          if (notificationsListAdapter != null) {
            notificationsListAdapter.setNotificationStream(viewData.notificationStream);
            notificationsListAdapter.notifyDataSetChanged();
          } else {
            notificationsListAdapter = new NotificationListAdapter(MainActivity.this, viewData.notificationStream, imageLoader, unreadNotificationsService);
            notificationsListView.setAdapter(notificationsListAdapter);
            notificationsListAdapter.setOnItemMenuClickedListener(new NotificationsListItemMenuClickListener());
            notificationsListView.setOnItemClickListener(new NotificationsListItemClickListener());
          }

          if (repositoriesListViewTablet != null) {
            if (repositoriesListAdapterTablet != null) {
              repositoriesListAdapterTablet.setNotificationStream(viewData.notificationStream);
              repositoriesListAdapterTablet.notifyDataSetChanged();
            } else {
              repositoriesListAdapterTablet = new NotificationRepositoriesListAdapter(MainActivity.this, viewData.notificationStream);
              repositoriesListViewTablet.setAdapter(repositoriesListAdapterTablet);
            }
            if (!repositoriesListAdapterTablet.setSelectionForFilter(repositoriesListViewTablet, filterByRepository)) {
              // repo no more in data so reset filter
              filterByRepository = null;
            }
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

    boolean markAsReadOnShow = false;
    int position;
    Notification notification;

    public ShowNotificationTask(Notification notification, boolean markAsReadOnShow, int position) {
      this.markAsReadOnShow = markAsReadOnShow;
      this.position = position;
      this.notification = notification;
    }

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
      return unreadNotificationsService.getNotificationViewUrl(notification);
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
          if (markAsReadOnShow) {
            markNotificationAsReadOnShow(position, notification);
          }
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

  private void showRepositoryFilterDialog() {
    ListOfNotificationsByRepositoriesFilterDialog bottomSheetDialogFragment = new ListOfNotificationsByRepositoriesFilterDialog();
    bottomSheetDialogFragment.show(getSupportFragmentManager(), "Bottom Sheet Dialog Fragment");
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
        new MarkAllNotificationsAsReadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
    if (repositoriesListAdapterTablet != null) {
      repositoriesListAdapterTablet.notifyDataSetChanged();
    }
    if (notificationsListView != null) {
      notificationsListView.measure(0, 0);
      notificationsListView.requestLayout();
    }
  }

  protected static final String PREF_LAST_RATEUS_SHOW_TIMESTAMP = "LAST_RATEUS_SHOW_TIMESTAMP";

  protected long RATEUS_SHOW_PERIOD = 5 * Utils.MILLIS_DAY;

  protected void storeTimestampOfLastRateusShow(long timestamp) {
    PreferencesUtils.storeLong(this, PREF_LAST_RATEUS_SHOW_TIMESTAMP, timestamp);
  }

  protected boolean isRateUsShowScheduled() {

    long lastShowTimestamp = PreferencesUtils.getLong(this, PREF_LAST_RATEUS_SHOW_TIMESTAMP, 0);
    if (lastShowTimestamp == 0) {
      storeTimestampOfLastRateusShow(System.currentTimeMillis());
    } else {
      return lastShowTimestamp <= (System.currentTimeMillis() - RATEUS_SHOW_PERIOD);
    }
    return false;
  }

  @Override
  public void onBackPressed() {

    if (isRateUsShowScheduled()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.dialog_rateus_text)
              .setCancelable(false)
              .setPositiveButton(R.string.dialog_rateus_btn_rate, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + MainActivity.this.getPackageName()));
                  startActivity(browserIntent);
                  ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "app_rateus_btn_rate", null, 0L);
                  MainActivity.this.storeTimestampOfLastRateusShow(System.currentTimeMillis() + (365 * Utils.MILLIS_DAY));
                  MainActivity.this.finish();
                }
              })
              .setNeutralButton(R.string.dialog_rateus_btn_never, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "app_rateus_btn_never", null, 0L);
                  MainActivity.this.storeTimestampOfLastRateusShow(System.currentTimeMillis() + (365 * Utils.MILLIS_DAY));
                  MainActivity.this.finish();
                }
              }).setNegativeButton(R.string.dialog_rateus_btn_later, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "app_rateus_btn_later", null, 0L);
          MainActivity.this.storeTimestampOfLastRateusShow(System.currentTimeMillis());
          MainActivity.this.finish();
        }
      });
      AlertDialog alert = builder.create();
      ActivityTracker.sendEvent(MainActivity.this, ActivityTracker.CAT_UI, "app_rateus_show", null, 0L);
      alert.show();
    } else {
      super.onBackPressed();
    }
  }
}
