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

import java.lang.reflect.Field;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daskiworks.ghwatch.backend.AuthenticationManager;
import com.daskiworks.ghwatch.backend.DonationService;
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.GHUserInfo;

/**
 * Abstract base for activities in this app.
 * <p>
 * Contains navigation drawer support over, you can init drawer using {@link #navigationDrawerInit()} in your {@link #onCreate(Bundle)} implementation.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public abstract class ActivityBase extends Activity {

  private static final String TAG = ActivityBase.class.getSimpleName();

  private static final String FRAGMENT_DIALOG = "dialogFragment";

  protected static final int COLOR_BACKGROUND_DRAWER = 0xFFEEEEEE;

  protected static final int NAV_DRAWER_ITEM_UNREAD_NOTIF = 0;
  protected static final int NAV_DRAWER_ITEM_WATCHED_REPOS = 1;

  protected View mDrawerView;
  protected DrawerLayout mDrawerLayout;
  protected ActionBarDrawerToggle mDrawerToggle;
  protected ListView mDrawerMenuList;
  protected NavigationDrawerAdapter mDrawerAdapter;
  protected String[] mDrawerMenuTitles;

  private CharSequence mDrawerTitle;
  private CharSequence mTitle;
  private int navDrawerMenuSelectedItem = -1;

  /**
   * Swipe layout support. A {@link #initSwipeLayout(OnRefreshListener)} must be called in {@link #onCreate(Bundle)} of activity.
   */
  protected SwipeRefreshLayout swipeLayout;
  protected SwipeRefreshLayout swipeLayout2;
  protected ProgressBar initialProgressBar;

  /**
   * Init SwipeRefreshLayout in the activity. A {@link #swipeLayout} is filled with object.
   * 
   * @param listener called on refresh swipe
   */
  protected void initSwipeLayout(OnRefreshListener listener) {
    swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
    if (swipeLayout != null) {
      swipeLayout.setOnRefreshListener(listener);
      swipeLayout.setColorSchemeResources(android.R.color.holo_red_light, R.color.apptheme_color, android.R.color.holo_orange_light, R.color.apptheme_color);
    }

    swipeLayout2 = (SwipeRefreshLayout) findViewById(R.id.swipe_container_2);
    if (swipeLayout2 != null) {
      swipeLayout2.setOnRefreshListener(listener);
      swipeLayout2.setColorSchemeResources(android.R.color.holo_red_light, R.color.apptheme_color, android.R.color.holo_orange_light, R.color.apptheme_color);
    }

    initialProgressBar = (ProgressBar) findViewById(R.id.initial_progress);
  }

  protected void hideInitialProgressBar() {
    if (initialProgressBar != null)
      initialProgressBar.setVisibility(View.GONE);
  }

  /**
   * Init navigation drawer for activity. Layout xml file must be appropriate!
   * 
   * @param selectedItem in drawer main menu which represents this activity, see <code>NAV_DRAWER_ITEM_xx</code> constants.
   * 
   * @see #navigationDrawerClose()
   */
  protected void initNavigationDrawer(int selectedItem) {
    // initialization of navigation drawer
    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (mDrawerLayout != null) {
      navDrawerMenuSelectedItem = selectedItem;
      mTitle = getTitle();
      mDrawerTitle = getResources().getString(R.string.app_name);
      mDrawerView = findViewById(R.id.drawer_view);
      // set drawer background color not to be transparent, we can't do it in layout due reuse for other layouts
      mDrawerView.setBackgroundColor(COLOR_BACKGROUND_DRAWER);
      mDrawerView.invalidate();

      mDrawerMenuTitles = getResources().getStringArray(R.array.action_list);
      mDrawerMenuList = (ListView) findViewById(R.id.drawer_menu);
      mDrawerAdapter = new NavigationDrawerAdapter(this, R.layout.drawer_list_item, mDrawerMenuTitles);
      mDrawerMenuList.setAdapter(mDrawerAdapter);
      mDrawerMenuList.setOnItemClickListener(new DrawerItemClickListener());

      mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_navigation_drawer, R.string.drawer_open, R.string.drawer_close) {

        public void onDrawerClosed(View view) {
          super.onDrawerClosed(view);
          getActionBar().setTitle(mTitle);
        }

        public void onDrawerOpened(View drawerView) {
          super.onDrawerOpened(drawerView);
          getActionBar().setTitle(mDrawerTitle);
        }

      };
      mDrawerLayout.setDrawerListener(mDrawerToggle);
      getActionBar().setDisplayHomeAsUpEnabled(true);
      getActionBar().setHomeButtonEnabled(true);
      navigationDrawerShowUserInfo();
    }
  }

  /**
   * Show user info in navigation drawer.
   */
  protected void navigationDrawerShowUserInfo() {
    if (mDrawerLayout != null) {
      (new ShowUserInfoTask()).execute();
    }
  }

  private class DrawerItemClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      onDrawerMenuItemSelected(position);
    }
  }

  protected void onDrawerMenuItemSelected(int position) {
    if (navDrawerMenuSelectedItem != position) {
      Intent intent = null;
      switch (position) {
      case NAV_DRAWER_ITEM_UNREAD_NOTIF:
        intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.INTENT_ACTION_RESET_FILTER);
        break;
      case NAV_DRAWER_ITEM_WATCHED_REPOS:
        intent = new Intent(this, WatchedRepositoriesActivity.class);
        break;
      }
      if (intent != null) {
        Log.d(TAG, "Intent frow drawer navigation : " + intent + " with action " + intent.getAction());
        navigationDrawerClose();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(intent);
      }
    }
    navigationDrawerClose();
  }

  /**
   * Close navigation drawer if opened. Used when some item in drawer is used.
   */
  protected void navigationDrawerClose() {
    if (mDrawerLayout != null) {
      mDrawerLayout.closeDrawer(mDrawerView);
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

    // hack to show menu overlay button in Action Bar even for phone with hardware menu buttons.
    try {
      ViewConfiguration config = ViewConfiguration.get(this);
      Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
      if (menuKeyField != null) {
        menuKeyField.setAccessible(true);
        menuKeyField.setBoolean(config, false);
      }
    } catch (Exception ex) {
      // Ignore
    }

  }

  protected boolean checkUserLoggedIn() {
    if (AuthenticationManager.getInstance().loadCurrentUser(this) == null) {
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
    navigationDrawerClose();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns true, then it has handled the app icon touch event
    if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    switch (item.getItemId()) {
    case R.id.action_settings:
      this.startActivity(new Intent(this, SettingsActivity.class));
      return true;
    case R.id.action_supportAppDevelopment:
      showSupportAppDevelopmentDialog();
      return true;
    case R.id.action_about:
      AboutDialogFragment ldf = new AboutDialogFragment();
      ldf.show(this.getFragmentManager(), FRAGMENT_DIALOG);
      return true;
    default:
      return false;
    }
  }

  /**
   * Show dialog with application support info
   */
  protected void showSupportAppDevelopmentDialog() {
    SupportAppDevelopmentDialogFragment f = (SupportAppDevelopmentDialogFragment) this.getFragmentManager().findFragmentByTag(FRAGMENT_DIALOG);
    if (f == null) {
      SupportAppDevelopmentDialogFragment sdf = new SupportAppDevelopmentDialogFragment();
      sdf.show(this.getFragmentManager(), FRAGMENT_DIALOG);
    } else {
      f.showDonatedInfo();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (!DonationService.processBillingResult(this, this.getFragmentManager().findFragmentByTag(FRAGMENT_DIALOG), requestCode, resultCode, data)) {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Item adapter for navigation drawer. Makes selected items bold.
   */
  public class NavigationDrawerAdapter extends ArrayAdapter<String> {
    public NavigationDrawerAdapter(Context context, int resource, String[] objects) {
      super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      TextView view = (TextView) super.getView(position, convertView, parent);
      if (position == navDrawerMenuSelectedItem) {
        view.setTypeface(Typeface.DEFAULT_BOLD);
      } else {
        view.setTypeface(Typeface.DEFAULT);
      }
      return view;
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
      TextView userName = (TextView) findViewById(R.id.user_name);
      if (userName != null)
        userName.setText(result.getName());
      TextView userUserName = (TextView) findViewById(R.id.user_username);
      if (userUserName != null)
        userUserName.setText(result.getUsername());
      if (result.getAvatarUrl() != null)
        ImageLoader.getInstance(getApplicationContext()).displayImage(result.getAvatarUrl(), (ImageView) findViewById(R.id.drawer_user_thumb));

      if (result.getHtmlUrl() != null) {
        RelativeLayout hv = (RelativeLayout) findViewById(R.id.drawer_user_header);
        hv.setClickable(true);
        hv.setOnClickListener(new OnClickListener() {

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
