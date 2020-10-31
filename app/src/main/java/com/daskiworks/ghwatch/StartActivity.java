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
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.daskiworks.ghwatch.LoginDialogFragment.LoginDialogListener;
import com.daskiworks.ghwatch.alarm.AlarmBroadcastReceiver;
import com.daskiworks.ghwatch.auth.AuthenticationManager;
import com.daskiworks.ghwatch.auth.GithubAccountAuthenticator;
import com.daskiworks.ghwatch.backend.PreferencesUtils;

/**
 * Activity used to show list of Notifications.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class StartActivity extends AppCompatActivity implements LoginDialogListener {

  private static final String TAG = StartActivity.class.getSimpleName();

  static final int CHOOSE_ACCOUNT_ACCESSTOKEN_REQUEST = 1;

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

    AccountManager accountManager = AccountManager.get(this);
    Account[] accs = accountManager.getAccountsByType(GithubAccountAuthenticator.ACCOUNT_TYPE);

    Log.d(TAG, "Existing accounts: " + accs);

    if (accs != null && accs.length > 0) {
      accountManager.getAuthToken(accs[0], GithubAccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN, null, this, new AccountManagerCallback<Bundle>() {
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
          try {
            Bundle result = future.getResult();
            String username = result.getString(AccountManager.KEY_ACCOUNT_NAME);
            String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
            Log.d(TAG, "username: " + username + " and token:" + authToken);
            //TODO store auth token for use better way
            AuthenticationManager.getInstance().storeAuthTokenTmp(StartActivity.this, username, authToken);
            showMainPage(false);
          } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
          }

        }
      }, null);
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
    AccountManager accountManager = AccountManager.get(this);
    AccountManagerFuture<Bundle> ret = accountManager.addAccount(GithubAccountAuthenticator.ACCOUNT_TYPE, GithubAccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN, null, null, this, new AccountManagerCallback<Bundle>() {
      @Override
      public void run(AccountManagerFuture<Bundle> future) {
        try {
          Bundle result = future.getResult();

          Intent intent = (Intent) result.get(AccountManager.KEY_INTENT);

          //no intent so account exists already
          if (intent == null) {
            showMainPage(true);
          } else {
            StartActivity.this.startActivityForResult(intent, CHOOSE_ACCOUNT_ACCESSTOKEN_REQUEST);
          }
        } catch (Exception e) {
          Log.e(TAG, e.getMessage(), e);
        }

      }
    }, null);


    /* TODO remove
    if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG_LOGIN_DIALOG) == null) {
      LoginDialogFragment ldf = new LoginDialogFragment();
      Bundle arg = new Bundle();
      arg.putBoolean(LoginDialogFragment.ARG_CANCELABLE, true);
      ldf.setArguments(arg);
      ldf.show(getFragmentManager(), FRAGMENT_TAG_LOGIN_DIALOG);
    }
     */

  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == CHOOSE_ACCOUNT_ACCESSTOKEN_REQUEST) {
      if (resultCode == RESULT_OK) {

        String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
        String accountType = data.getExtras().getString(AccountManager.KEY_ACCOUNT_TYPE);
        Account account = new Account(accountName, accountType);

        AccountManager accountManager = AccountManager.get(this);
        accountManager.getAuthToken(account, GithubAccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN, null, false, new AccountManagerCallback<Bundle>() {
          @Override
          public void run(AccountManagerFuture<Bundle> future) {
            try {
              Bundle bundle = future.getResult();

              AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
              builder.setTitle(bundle.getString(AccountManager.KEY_ACCOUNT_NAME))
                      .setMessage(bundle.getString(AccountManager.KEY_AUTHTOKEN))
                      .show();
            } catch (Exception ex) {
              ex.getMessage();
            }
          }
        }, null);
      }
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
