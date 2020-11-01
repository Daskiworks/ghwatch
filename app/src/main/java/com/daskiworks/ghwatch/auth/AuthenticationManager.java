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
package com.daskiworks.ghwatch.auth;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.net.NoRouteToHostException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import com.daskiworks.ghwatch.ActivityTracker;
import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.backend.GHConstants;
import com.daskiworks.ghwatch.backend.OTPAuthenticationException;
import com.daskiworks.ghwatch.backend.RemoteSystemClient;
import com.daskiworks.ghwatch.backend.RemoteSystemClient.Response;
import com.daskiworks.ghwatch.model.AccountType;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.GHCredentials;
import com.daskiworks.ghwatch.model.GHUserInfo;
import com.daskiworks.ghwatch.model.GHUserLoginInfo;
import com.daskiworks.ghwatch.model.LoadingStatus;

/**
 * Authentication manager class.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class AuthenticationManager {

  private static final String TAG = "AuthenticationManager";

  private static final String GH_USER_REQ_URL = GHConstants.URL_BASE + "/user";

  /**
   * Reload from server is forced automatically if data in persistent store are older than this timeout [millis]
   */
  private static final long USER_INFO_RELOAD_AFTER = Utils.MILLIS_DAY;

  private static final String culiFileName = "cui.td";
  private static final String cuFileName = "cu.td";

  private File culiFile;
  private File cuFile;

  private GHUserInfo currentUserInfo;
  private ExecutorService userInfoExecutorService = Executors.newFixedThreadPool(1);
  private UserInfoLoaderTask userInfoLoaderTask;

  private GHCredentials credentials;

  private static AuthenticationManager instance = null;

  /**
   * Get instance of manager for use.
   * 
   * @return
   */
  public static AuthenticationManager getInstance() {
    if (instance == null) {
      instance = new AuthenticationManager();
    }
    return instance;
  }

  private File getCuliFile(Context context) {
    if (culiFile == null)
      culiFile = context.getFileStreamPath(culiFileName);
    return culiFile;
  }

  private File getCuFile(Context context) {
    if (cuFile == null)
      cuFile = context.getFileStreamPath(cuFileName);
    return cuFile;
  }

  private AuthenticationManager() {
    super();
  }


  /**
   * Take existing account from the system {@link AccountManager}
   * @param context to be used
   * @return account or null if no one exists
   */
  public Account getAccountFromSystemAccountManager(Context context){
    AccountManager accountManager = AccountManager.get(context);
    Account[] accs = accountManager.getAccountsByType(GithubAccountAuthenticator.ACCOUNT_TYPE);
    if (accs != null && accs.length > 0) {
      return accs[0];
    }
    return null;
  }


  /**
   * Get info about current user logged in this application.
   * 
   * @return current user or null if not logged in yet.
   */
  public GHUserLoginInfo loadCurrentUser(Context context) {
    return Utils.readFromStore(TAG, context, getCuliFile(context));
  }

  public void storeAuthToken(Context context, String username, String token){
    GHUserLoginInfo ui = new GHUserLoginInfo(AccountType.LOCAL, username, token);
    storeCurrentUserLogin(context, ui);
  }


  private void storeCurrentUserLogin(Context context, GHUserLoginInfo currentUserLoginInfo) {
    Utils.writeToStore(TAG, context, getCuliFile(context), currentUserLoginInfo);
    if (currentUserInfo == null || (currentUserLoginInfo != null && !currentUserLoginInfo.getUsername().equals(currentUserInfo.getUsername()))) {
      loadUserInfoFromServerAsync(context);
    }
    this.credentials = null;
  }

  /**
   * Get credentials of current user for GH API calls.
   * 
   * @return credentials for API calls
   */
  public GHCredentials getGhApiCredentials(Context context) {
    if (credentials != null)
      return credentials;
    GHUserLoginInfo ui = loadCurrentUser(context);
    if (ui != null) {
      credentials = createGHCredentials(ui.getT());
      return credentials;
    } else {
      return null;
    }
  }

  public GHCredentials createGHCredentials(String token){
    return new GHCredentials(token, "x-oauth-basic");
  }

  /**
   * Get info about currently logged in user. Load it from filesystem so may take longer time, so should be called asynchronously from GUI.
   * 
   * @param context used to load it
   * @return info about current user or null
   */
  public GHUserInfo getCurrentUserInfo(Context context) {
    if (currentUserInfo == null) {
      currentUserInfo = Utils.readFromStore(TAG, context, getCuFile(context));
      if (currentUserInfo == null || currentUserInfo.getUpdateTimestamp() < (System.currentTimeMillis() - USER_INFO_RELOAD_AFTER)) {
        loadUserInfoFromServerAsync(context);
      }
    }
    return currentUserInfo;
  }

  protected void loadUserInfoFromServerAsync(Context context) {
    if (userInfoLoaderTask == null) {
      userInfoLoaderTask = new UserInfoLoaderTask(context, getGhApiCredentials(context));
      userInfoExecutorService.submit(userInfoLoaderTask);
    }
  }

  public GHUserInfo loadUserInfoFromServer(Context context, GHCredentials ghCredentials) {
    try {
      Response<JSONObject> r = RemoteSystemClient.getJSONObjectFromUrl(context, ghCredentials, GH_USER_REQ_URL, null);
      if (r.data != null) {
        currentUserInfo = new GHUserInfo(r.data);
        Utils.writeToStore(TAG, context.getApplicationContext(), getCuFile(context), currentUserInfo);
        return currentUserInfo;
      }
    } catch (Throwable th) {
      Log.e(TAG, "User information loading error: " + th.getMessage(), th);
    }
    return null;
  }

  private class UserInfoLoaderTask implements Runnable {

    private GHCredentials ghCredentials;
    private Context context;

    UserInfoLoaderTask(Context context, GHCredentials ghCredentials) {
      this.context = context.getApplicationContext();
      this.ghCredentials = ghCredentials;
    }

    @Override
    public void run() {
      try {
        loadUserInfoFromServer(context, ghCredentials);
      } finally {
        userInfoLoaderTask = null;
      }
    }

  }
}
