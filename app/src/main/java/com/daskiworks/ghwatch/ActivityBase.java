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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.daskiworks.ghwatch.auth.AuthenticationManager;
import com.daskiworks.ghwatch.auth.GithubAccountAuthenticator;
import com.daskiworks.ghwatch.backend.DonationService;
import com.daskiworks.ghwatch.backend.PreferencesUtils;
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.GHUserInfo;

import java.util.concurrent.RejectedExecutionException;

/**
 * Abstract base for activities in this app.
 * <p>
 * Contains navigation drawer support over, you can init drawer using {@link #initNavigationDrawer(int)} in your {@link #onCreate(Bundle)} implementation.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public abstract class ActivityBase extends AppCompatActivity {

  private static final String TAG = ActivityBase.class.getSimpleName();

  protected static final String FRAGMENT_DIALOG = "dialogFragment";

  protected DrawerLayout mDrawerLayout;
  protected ActionBarDrawerToggle mDrawerToggle;
  protected NavigationView mDrawerNavigationView;
  private int navDrawerMenuSelectedItem;
  protected View mDrawerHeaderView;

  /**
   * Swipe layout support. A {@link #initSwipeLayout(OnRefreshListener)} must be called in {@link #onCreate(Bundle)} of activity.
   */
  protected View contentExistsLayout;
  protected SwipeRefreshLayout swipeLayout;
  protected SwipeRefreshLayout swipeLayout2;
  protected View initialProgressBar;

  protected int nightTheme;

  /**
   * Init SwipeRefreshLayout in the activity. A {@link #swipeLayout} is filled with object.
   *
   * @param listener called on refresh swipe
   */
  protected void initSwipeLayout(OnRefreshListener listener) {
    contentExistsLayout = (View) findViewById(R.id.swipe_around);
    swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
    if(contentExistsLayout == null)
      contentExistsLayout = swipeLayout;
    if (swipeLayout != null) {
      swipeLayout.setOnRefreshListener(listener);
      swipeLayout.setColorSchemeResources(android.R.color.holo_red_light, R.color.apptheme_colorPrimary, android.R.color.holo_orange_light, R.color.apptheme_colorPrimary);
    }

    swipeLayout2 = (SwipeRefreshLayout) findViewById(R.id.swipe_container_2);
    if (swipeLayout2 != null) {
      swipeLayout2.setOnRefreshListener(listener);
      swipeLayout2.setColorSchemeResources(android.R.color.holo_red_light, R.color.apptheme_colorPrimary, android.R.color.holo_orange_light, R.color.apptheme_colorPrimary);
    }

    initialProgressBar = findViewById(R.id.initial_progress);
  }

  protected void hideInitialProgressBar() {
    if (initialProgressBar != null)
      initialProgressBar.setVisibility(View.GONE);
  }

  /**
   * Init navigation drawer for activity. Layout xml file must be appropriate!
   *
   * @param selectedItem in drawer main menu which represents this activity, see <code>NAV_DRAWER_ITEM_xx</code> constants.
   */
  protected void initNavigationDrawer(final int selectedItem) {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // initialization of navigation drawer
    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (mDrawerLayout != null) {

      mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
              R.string.drawer_open, R.string.drawer_close) {

        /** Called when a drawer has settled in a completely closed state. */
        public void onDrawerClosed(View view) {
          super.onDrawerClosed(view);
          invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        }

        /** Called when a drawer has settled in a completely open state. */
        public void onDrawerOpened(View drawerView) {
          super.onDrawerOpened(drawerView);
          invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        }
      };

      // Set the drawer toggle as the DrawerListener
      mDrawerLayout.addDrawerListener(mDrawerToggle);
      mDrawerToggle.syncState();


      mDrawerNavigationView = (NavigationView) findViewById(R.id.navigation_drawer_view);
      mDrawerNavigationView.setNavigationItemSelectedListener(
              new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem item) {
                  onDrawerMenuItemSelected(item);
                  navigationDrawerClose();
                  return true;
                }
              });

      if ((getResources().getConfiguration().uiMode
              & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
        ColorStateList ndcl = ColorStateList.valueOf(getResources().getColor(R.color.light_grey));
        mDrawerNavigationView.setItemTextColor(ndcl);
        mDrawerNavigationView.setItemIconTintList(ndcl);
      }
      navDrawerMenuSelectedItem = selectedItem;
      if (getSupportActionBar() != null) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
      }
      navigationDrawerShowUserInfo();
    }
  }

  /**
   * Show user info in navigation drawer.
   */
  protected void navigationDrawerShowUserInfo() {
    if (mDrawerLayout != null) {
      mDrawerHeaderView = mDrawerNavigationView.getHeaderView(0);
      try {
        (new ShowUserInfoTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      } catch (RejectedExecutionException e) {
        //nothing to do
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns
    // true, then it has handled the app icon touch event
    if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }


  protected void onDrawerMenuItemSelected(MenuItem menuItem) {
    if (navDrawerMenuSelectedItem != menuItem.getItemId()) {
      Intent intent = null;
      switch (menuItem.getItemId()) {
        case R.id.nav_unread:
          intent = new Intent(this, MainActivity.class);
          intent.setAction(MainActivity.INTENT_ACTION_RESET_FILTER);
          break;
        case R.id.nav_watched:
          intent = new Intent(this, WatchedRepositoriesActivity.class);
          break;
        case R.id.nav_settings:
          intent = new Intent(this, SettingsActivity.class);
          break;
        case R.id.nav_support:
          showSupportAppDevelopmentDialog();
          break;
        case R.id.nav_about:
          AboutDialogFragment ldf = new AboutDialogFragment();
          ldf.show(this.getFragmentManager(), FRAGMENT_DIALOG);
          break;
      }
      if (intent != null) {
        Log.d(TAG, "Intent fro drawer navigation : " + intent + " with action " + intent.getAction());

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(intent);
      }
    }

  }


  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    if (mDrawerToggle != null)
      mDrawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mDrawerToggle != null)
      mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    nightTheme = PreferencesUtils.setAppNightMode(this);
  }

  protected boolean checkUserLoggedIn() {

    AccountManager accountManager = AccountManager.get(this);
    Account[] accs = accountManager.getAccountsByType(GithubAccountAuthenticator.ACCOUNT_TYPE);

    Log.d(TAG, "Existing accounts: " + accs);

    if (accs == null || accs.length == 0) {
      Intent intent = new Intent(this, StartActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      this.startActivity(intent);
      return false;
    }
    return true;
  }

  @Override
  protected void onResume() {
    super.onResume();

    //recreate me if app theme changed in meantime
    int currentNightTheme = PreferencesUtils.setAppNightMode(this);
    if (nightTheme != currentNightTheme) {
      recreate();
      nightTheme = currentNightTheme;
    }
    navigationDrawerClose();
  }


  protected void navigationDrawerClose() {
    if (mDrawerLayout != null)
      mDrawerLayout.closeDrawers();
    if (mDrawerNavigationView != null)
      mDrawerNavigationView.setCheckedItem(navDrawerMenuSelectedItem);
  }


  /**
   * Show dialog. Dialog is shown only if no other dialog is shown currently.
   *
   * @param dialog to show
   * @return true if shown, false if not.
   */
  protected boolean showDialog(DialogFragment dialog) {
    Fragment f = this.getFragmentManager().findFragmentByTag(FRAGMENT_DIALOG);
    if (f == null) {
      dialog.show(this.getFragmentManager(), FRAGMENT_DIALOG);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Show dialog with application support info
   */
  protected void showSupportAppDevelopmentDialog() {
    showDialog(new SupportAppDevelopmentDialogFragment());
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (!DonationService.processBillingResult(this, this.getFragmentManager().findFragmentByTag(FRAGMENT_DIALOG), requestCode, resultCode, data)) {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }


  private final class ShowUserInfoTask extends AsyncTask<Object, String, GHUserInfo> {

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected GHUserInfo doInBackground(Object... params) {
      return AuthenticationManager.getInstance().getCurrentUserInfo(getApplicationContext());
    }

    @Override
    protected void onPostExecute(final GHUserInfo result) {
      if (isCancelled() || result == null) {
        return;
      }

      TextView mDrawerHeaderUserName = (TextView) mDrawerHeaderView.findViewById(R.id.user_name);
      TextView mDrawerHeaderUserUserame = (TextView) mDrawerHeaderView.findViewById(R.id.user_username);

      mDrawerHeaderUserName.setText(result.getName());
      mDrawerHeaderUserUserame.setText(result.getUsername());

      if (result.getAvatarUrl() != null)
        ImageLoader.getInstance(getApplicationContext()).displayImage(result.getAvatarUrl(), (ImageView) mDrawerHeaderView.findViewById(R.id.drawer_user_thumb), ActivityBase.this);

      if (result.getHtmlUrl() != null) {
        mDrawerHeaderView.setClickable(true);
        mDrawerHeaderView.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View v) {
            navigationDrawerClose();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getHtmlUrl()));
            startActivity(browserIntent);
            ActivityTracker.sendEvent(getApplicationContext(), ActivityTracker.CAT_UI, "nav_drawer_user_profile_show", "", 0L);
          }
        });

      }

    }
  }

}
