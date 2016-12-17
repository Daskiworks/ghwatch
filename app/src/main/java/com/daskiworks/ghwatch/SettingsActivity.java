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

import android.app.ActionBar;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.util.Log;
import android.view.MenuItem;

import com.daskiworks.ghwatch.LoginDialogFragment.LoginDialogListener;
import com.daskiworks.ghwatch.alarm.AlarmBroadcastReceiver;
import com.daskiworks.ghwatch.backend.AuthenticationManager;
import com.daskiworks.ghwatch.backend.PreferencesUtils;
import com.daskiworks.ghwatch.model.GHUserLoginInfo;

/**
 * Application settings activity.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class SettingsActivity extends ActivityBase implements OnSharedPreferenceChangeListener, LoginDialogListener {

  private static final String TAG = SettingsActivity.class.getSimpleName();

  private SettingsFragment sf = null;
  AuthenticationManager authenticationManager;

  private BackupManager mBackupManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mBackupManager = new BackupManager(this);
    // Display the fragment as the main content.
    sf = new SettingsFragment();
    getFragmentManager().beginTransaction().replace(android.R.id.content, sf).commit();

    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    authenticationManager = AuthenticationManager.getInstance();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {
    switch (menuItem.getItemId()) {
      case android.R.id.home:
        Intent homeIntent = new Intent(this, MainActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }
    return (super.onOptionsItemSelected(menuItem));

  }

  @Override
  protected void onResume() {
    super.onResume();
    sf.getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    ActivityTracker.sendView(this, TAG);
  }

  @Override
  protected void onPause() {
    super.onPause();
    sf.getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    if (key.equals(PreferencesUtils.PREF_NOTIFY)) {
      ActivityTracker.sendEvent(this, ActivityTracker.CAT_PREF, PreferencesUtils.PREF_NOTIFY,
              "" + sharedPreferences.getBoolean(PreferencesUtils.PREF_NOTIFY, true), 0L);
      if (sharedPreferences.getBoolean(PreferencesUtils.PREF_NOTIFY, true)) {
        AlarmBroadcastReceiver.startServerPoolingIfEnabled(this);
      } else {
        AlarmBroadcastReceiver.stopServerPoolingIfDisabled(this);
      }
    } else if (key.equals(PreferencesUtils.PREF_NOTIFY_VIBRATE)) {
      ActivityTracker.sendEvent(this, ActivityTracker.CAT_PREF, PreferencesUtils.PREF_NOTIFY_VIBRATE,
              "" + sharedPreferences.getBoolean(PreferencesUtils.PREF_NOTIFY_VIBRATE, true), 0L);
    } else if (key.equals(PreferencesUtils.PREF_SERVER_CHECK_PERIOD)) {
      ListPreference periodPref = (ListPreference) sf.findPreference(PreferencesUtils.PREF_SERVER_CHECK_PERIOD);
      periodPref.setSummary(periodPref.getEntry());
      AlarmBroadcastReceiver.startServerPoolingIfEnabled(this);
      ActivityTracker.sendEvent(this, ActivityTracker.CAT_PREF, PreferencesUtils.PREF_SERVER_CHECK_PERIOD, periodPref.getEntry().toString(), 0L);
    } else if (key.equals(PreferencesUtils.PREF_NOTIFY_FILTER)) {
      ListPreference notifFilterPref = (ListPreference) sf.findPreference(PreferencesUtils.PREF_NOTIFY_FILTER);
      notifFilterPref.setSummary(notifFilterPref.getEntry());
      ActivityTracker.sendEvent(this, ActivityTracker.CAT_PREF, "notif_filter", notifFilterPref.getEntry().toString(), 0L);
    } else if (key.equals(PreferencesUtils.PREF_SERVER_DETAIL_LOADING) || key.equals(PreferencesUtils.PREF_SERVER_LABELS_LOADING)) {
      ActivityTracker.sendEvent(this, ActivityTracker.CAT_PREF, key, "" + sharedPreferences.getBoolean(key, false), 0L);
    }
    mBackupManager.dataChanged();
  }

  public static class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Load the preferences from an XML resource
      addPreferencesFromResource(R.xml.preferences);

      if (Utils.getVibrator(getActivity()) == null) {
        Preference p = findPreference(PreferencesUtils.PREF_NOTIFY_VIBRATE);
        ((PreferenceCategory) findPreference("pref_cat_notificationSettings")).removePreference(p);
      }

    }

    @Override
    public void onStart() {
      super.onStart();
      // Set summary to be the user-description for the selected value
      ListPreference connectionPref = (ListPreference) findPreference(PreferencesUtils.PREF_SERVER_CHECK_PERIOD);
      connectionPref.setSummary(connectionPref.getEntry());

      ListPreference notifFilterPref = (ListPreference) findPreference(PreferencesUtils.PREF_NOTIFY_FILTER);
      notifFilterPref.setSummary(notifFilterPref.getEntry());

      Preference userAccountPref = findPreference(PreferencesUtils.PREF_SERVER_ACCOUNT);
      GHUserLoginInfo currentUser = ((SettingsActivity) getActivity()).authenticationManager.loadCurrentUser(getActivity());
      setCurrentUserPreferenceSummary(getActivity(), userAccountPref, currentUser);
      userAccountPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
          LoginDialogFragment ldf = new LoginDialogFragment();
          Bundle arg = new Bundle();
          arg.putBoolean(LoginDialogFragment.ARG_CANCELABLE, true);
          ldf.setArguments(arg);
          ldf.show(getFragmentManager(), "LOGIN_DIALOG");
          return true;
        }
      });

      final RingtonePreference notificationSoundPref = (RingtonePreference) findPreference(PreferencesUtils.PREF_NOTIFY_SOUND);
      notificationSoundPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          String rps = (String) newValue;
          if (rps != null && !rps.isEmpty()) {
            Ringtone rt = RingtoneManager.getRingtone(getActivity(), Uri.parse(rps));
            notificationSoundPref.setSummary(rt.getTitle(getActivity()));
          } else {
            notificationSoundPref.setSummary(getString(R.string.notificationSoundDisabled));
          }
          return true;

        }
      });
      String rps = getPreferenceManager().getSharedPreferences().getString(PreferencesUtils.PREF_NOTIFY_SOUND, null);
      if (rps != null && !rps.isEmpty()) {
        Log.d(TAG, "Ringtone preference value: " + rps);
        Ringtone rt = RingtoneManager.getRingtone(getActivity(), Uri.parse(rps));
        notificationSoundPref.setSummary(rt.getTitle(getActivity()));
      } else {
        notificationSoundPref.setSummary(getString(R.string.notificationSoundDisabled));
      }

    }

  }

  @Override
  public void afterLoginSuccess(LoginDialogFragment dialog) {
    Preference userAccountPref = sf.findPreference(PreferencesUtils.PREF_SERVER_ACCOUNT);
    GHUserLoginInfo currentUser = authenticationManager.loadCurrentUser(getApplicationContext());
    setCurrentUserPreferenceSummary(getApplicationContext(), userAccountPref, currentUser);
  }

  protected static void setCurrentUserPreferenceSummary(Context context, Preference userAccountPref, GHUserLoginInfo currentUser) {
    if (currentUser != null) {
      userAccountPref.setSummary(currentUser.getUsername());
    }
  }

}
