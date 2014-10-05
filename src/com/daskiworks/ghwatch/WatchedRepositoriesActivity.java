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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;

import com.daskiworks.ghwatch.LoginDialogFragment.LoginDialogListener;
import com.daskiworks.ghwatch.backend.PreferencesUtils;
import com.daskiworks.ghwatch.backend.ViewDataReloadStrategy;
import com.daskiworks.ghwatch.backend.WatchedRepositoriesService;
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.Repository;
import com.daskiworks.ghwatch.model.WatchedRepositoriesViewData;

/**
 * Activity used to show list of watched repositories.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class WatchedRepositoriesActivity extends ActivityBase implements LoginDialogListener {

  private static final String TAG = WatchedRepositoriesActivity.class.getSimpleName();

  // common fields
  private DataLoaderTask dataLoader;

  // view components
  private Menu mainMenu;
  private ListView repositoriesListView;
  private WatchedRepositoryListAdapter repositoriesListAdapter;

  // backend services
  private ImageLoader imageLoader;
  private WatchedRepositoriesService watchedRepositoriesService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_watched_repos);

    imageLoader = ImageLoader.getInstance(getApplicationContext());
    watchedRepositoriesService = new WatchedRepositoriesService(getBaseContext());

    navigationDrawerInit(NAV_DRAWER_ITEM_WATCHED_REPOS);

    // initialization of main content
    repositoriesListView = (ListView) findViewById(R.id.list);
    repositoriesListView.setVerticalFadingEdgeEnabled(true);
    ActivityTracker.sendView(this, TAG);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!checkUserLoggedIn())
      return;

    refreshList(ViewDataReloadStrategy.IF_TIMED_OUT, false);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mainMenu = menu;
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.watched_repos_activity_actions, menu);
    if (dataLoader != null)
      animateRefreshButton();
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (dataLoader != null)
      dataLoader.cancel(true);
    notifyDataSetChanged();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    if (super.onOptionsItemSelected(item)) {
      return true;
    }

    switch (item.getItemId()) {
    case R.id.action_refresh:
      refreshList(ViewDataReloadStrategy.ALWAYS, false);
      ActivityTracker.sendEvent(this, ActivityTracker.CAT_UI, "watched_repositories_refresh", "", 0L);
      return true;
    default:
      return false;
    }
  }

  public void refreshList(ViewDataReloadStrategy reloadStrateg, boolean supressErrorMessages) {
    if (dataLoader == null)
      (dataLoader = new DataLoaderTask(reloadStrateg, supressErrorMessages)).execute();
  }

  @SuppressLint("InflateParams")
  protected void animateRefreshButton() {
    if (mainMenu != null) {
      MenuItem item = mainMenu.findItem(R.id.action_refresh);
      if (item != null && item.getActionView() == null) {
        LayoutInflater inflater = (LayoutInflater) getApplication().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.action_refresh_rotate, null);
        iv.setScaleType(ScaleType.CENTER);
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.refresh_rotate);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        item.setActionView(iv);
      }
    }
  }

  private final class RepositoriesListItemMenuClickListener implements WatchedRepositoryListAdapter.OnItemMenuClickedListener {
    @Override
    public boolean onMenuItemClick(Repository repository, int menuItemId) {
      switch (menuItemId) {
      case R.id.action_view:
        if (repository != null) {
          if (repository.getHtmlUrl() != null) {
            ActivityTracker.sendEvent(WatchedRepositoriesActivity.this, ActivityTracker.CAT_UI, "watched_repository_show", "", 0L);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(repository.getHtmlUrl()));
            startActivity(browserIntent);
          } else {
            Log.w(TAG, "html_url not present for repository " + repository.getRepositoryFullName());
          }
        }
        return true;
      case R.id.action_unwatch:
        showMarkAllNotificationsAsReadDialog(repository);
        return true;
      case R.id.action_pref_notifyFilter:
        showPrefNotifFilterDialog(repository);
        return true;
      default:
        return false;
      }
    }
  }

  public void showPrefNotifFilterDialog(final Repository repository) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    final String[] sa = getResources().getStringArray(R.array.pref_notifyFilterFull_entries);
    sa[0] = sa[0] + " (" + sa[Integer.parseInt(PreferencesUtils.getNotificationFilter(this))] + ")";
    builder.setTitle(R.string.pref_notifyFilter).setNegativeButton(android.R.string.cancel, null).setItems(sa, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        PreferencesUtils.setNotificationFilterForRepository(WatchedRepositoriesActivity.this, repository.getRepositoryFullName(), which + "");
        ActivityTracker.sendEvent(WatchedRepositoriesActivity.this, ActivityTracker.CAT_PREF, "notif_filter_repo", "" + which, 0L);
        notifyDataSetChanged();
      }
    });
    builder.create().show();
  }

  private final class DataLoaderTask extends AsyncTask<String, String, WatchedRepositoriesViewData> {

    ViewDataReloadStrategy reloadStrategy = null;
    boolean supressErrorMessages = false;

    DataLoaderTask(ViewDataReloadStrategy reloadStrategy, boolean supressErrorMessages) {
      this.reloadStrategy = reloadStrategy;
      this.supressErrorMessages = supressErrorMessages;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      animateRefreshButton();
    }

    @Override
    protected WatchedRepositoriesViewData doInBackground(String... args) {
      return watchedRepositoriesService.getNotificationStreamForView(reloadStrategy);
    }

    @Override
    protected void onPostExecute(final WatchedRepositoriesViewData viewData) {
      try {
        if (isCancelled() || viewData == null)
          return;
        if (viewData.loadingStatus != LoadingStatus.OK) {
          if (!supressErrorMessages)
            showServerCommunicationErrorAllertDialog(viewData.loadingStatus, viewData.repositories != null);
        }
        if (viewData.repositories != null) {

          if (repositoriesListAdapter != null) {
            repositoriesListAdapter.setNotificationStream(viewData.repositories);
            repositoriesListAdapter.notifyDataSetChanged();
          } else {
            repositoriesListAdapter = new WatchedRepositoryListAdapter(WatchedRepositoriesActivity.this, viewData.repositories, imageLoader);
            repositoriesListView.setAdapter(repositoriesListAdapter);
            repositoriesListAdapter.setOnItemMenuClickedListener(new RepositoriesListItemMenuClickListener());
          }

          if (viewData.repositories.size() == 0) {
            findViewById(R.id.list_empty_text).setVisibility(View.VISIBLE);
            repositoriesListView.setVisibility(View.GONE);
          } else {
            findViewById(R.id.list_empty_text).setVisibility(View.GONE);
            repositoriesListView.setVisibility(View.VISIBLE);
          }
        }
      } finally {
        dataLoader = null;
        if (mainMenu != null) {
          MenuItem item = mainMenu.findItem(R.id.action_refresh);
          if (item != null && item.getActionView() != null) {
            item.getActionView().clearAnimation();
            item.setActionView(null);
          }
        }
        invalidateOptionsMenu();
      }
    }
  }

  private final class UnwatchRepositoryTask extends AsyncTask<Long, String, BaseViewData> {

    @Override
    protected BaseViewData doInBackground(Long... params) {

      return watchedRepositoriesService.unwatchRepository(params[0]);
    }

    @Override
    protected void onPostExecute(final BaseViewData viewData) {
      if (isCancelled() || viewData == null)
        return;
      if (viewData.loadingStatus != LoadingStatus.OK) {
        showServerCommunicationErrorAllertDialog(viewData.loadingStatus, false);
        refreshList(ViewDataReloadStrategy.NEVER, true);
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

  private void showMarkAllNotificationsAsReadDialog(final Repository repository) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.message_confirm_unwatch_repository_title);
    builder.setMessage(R.string.message_confirm_unwatch_repository);
    builder.setCancelable(true);
    builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        new UnwatchRepositoryTask().execute(repository.getId());
        repositoriesListAdapter.removeRepositoryById(repository.getId());
        ActivityTracker.sendEvent(WatchedRepositoriesActivity.this, ActivityTracker.CAT_UI, "repository_unwatch", "", 0L);
        notifyDataSetChanged();
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
    if (repositoriesListAdapter != null)
      repositoriesListAdapter.notifyDataSetChanged();
  }
}
