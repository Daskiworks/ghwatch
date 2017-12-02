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

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.daskiworks.ghwatch.LoginDialogFragment.LoginDialogListener;
import com.daskiworks.ghwatch.alarm.AlarmBroadcastReceiver;
import com.daskiworks.ghwatch.backend.AuthenticationManager;
import com.daskiworks.ghwatch.backend.PreferencesUtils;

/**
 * Activity used to show list of Notifications.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class StartActivity extends AppCompatActivity implements LoginDialogListener {

  private static final String TAG = StartActivity.class.getSimpleName();

  public static final String FRAGMENT_TAG_LOGIN_DIALOG = "loginDialogFragment";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    PreferencesUtils.setAppNightMode(this);

    // set default preferences first time app is started
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    // start notifications during first app run after installation
    SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
    boolean isFirstRun = wmbPreference.getBoolean("FIRSTRUN", true);
    if (isFirstRun) {
      ActivityTracker.sendEvent(this, ActivityTracker.CAT_UI, "FIRST_RUN", "FIRST_RUN", 0L);
      AlarmBroadcastReceiver.startServerPoolingIfEnabled(this);
      SharedPreferences.Editor editor = wmbPreference.edit();
      editor.putBoolean("FIRSTRUN", false);
      editor.putLong("FIRSTRUNTIMESTAMP", System.currentTimeMillis());
      editor.commit();
      (new BackupManager(this)).dataChanged();
    }

    if (AuthenticationManager.getInstance().loadCurrentUser(this) != null) {
      showMainPage(false);
    } else {
      setContentView(R.layout.activity_start);
      Button button = (Button) findViewById(R.id.button_login_github);
      button.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          showLoginDialog();
        }
      });
    }

  }

  protected void showMainPage(boolean showAnimation) {
    Intent intent = new Intent(this, MainActivity.class);
    if (!showAnimation) {
      this.startActivity(intent);
      finish();
      overridePendingTransition(0, 0);
    } else {
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      this.startActivity(intent);
    }
  }

  protected void showLoginDialog() {
    if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG_LOGIN_DIALOG) == null) {
      LoginDialogFragment ldf = new LoginDialogFragment();
      Bundle arg = new Bundle();
      arg.putBoolean(LoginDialogFragment.ARG_CANCELABLE, true);
      ldf.setArguments(arg);
      ldf.show(getFragmentManager(), FRAGMENT_TAG_LOGIN_DIALOG);
    }

  }

  @Override
  protected void onResume() {
    super.onResume();
    ActivityTracker.sendView(this, TAG);
  }

  @Override
  public void afterLoginSuccess(LoginDialogFragment dialog) {
    showMainPage(true);
  }

}
