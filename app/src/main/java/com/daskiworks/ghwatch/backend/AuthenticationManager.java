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

import android.content.Context;
import android.util.Log;

import com.daskiworks.ghwatch.ActivityTracker;
import com.daskiworks.ghwatch.Utils;
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

  private static String client_s;
  private static String client_i;

  static {
    InputStream in = GHConstants.class.getResourceAsStream("clients.properties");
    try {
      Properties props = new Properties();
      if (in != null)
        props.load(in);
      else
        Log.e(TAG, "clients.properties file not found to get Github API keys from");
      client_s = props.getProperty("clients");
      client_i = props.getProperty("clienti");
    } catch (IOException e) {
      Log.e(TAG, "Unable to load secrets for github authentication " + e.getMessage());
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (IOException e) {
          // OK
        }
    }

  }

  private static final String GH_AUTH_REQ_URL = GHConstants.URL_BASE + "/authorizations/clients/" + client_i;
  private static final String GH_USER_REQ_URL = GHConstants.URL_BASE + "/user";

  /**
   * Reload from server is forced automatically if data in persistent store are older than this timeout [millis]
   */
  private static final long USER_INFO_RELOAD_AFTER = Utils.MILLIS_DAY;

  private static final String culiFileName = "cui.td";
  private static final String cuFileName = "cu.td";

  private static final String GH_AUTH_REQ_CONTENT = "{\"client_secret\":\"" + client_s
      + "\",\"scopes\":[\"notifications\",\"repo\"],\"note\":\"GH:watch android app\", \"fingerprint\":\"*fp*\"}";

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
   * @param context
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
   * Get info about current user logged in this application.
   * 
   * @return current user or null if not logged in yet.
   */
  public GHUserLoginInfo loadCurrentUser(Context context) {
    return Utils.readFromStore(TAG, context, getCuliFile(context));
  }

  public static class LoginViewData extends BaseViewData {
    public boolean isOtp;
    public String otpType;
  }

  /**
   * Login user for this application.
   * 
   * @param username for user
   * @param password for user to login
   * @param otp one time password for 2fact authentication
   * @return info about login success
   * 
   */
  public LoginViewData login(Context context, String username, String password, String otp) {
    LoginViewData nswd = new LoginViewData();
    String trackLabel = "OK";
    try {
      String token = remoteLogin(context, new GHCredentials(username, password), otp);
      Log.d(TAG, "Login token: " + token);
      GHUserLoginInfo ui = new GHUserLoginInfo(AccountType.LOCAL, username, token);
      storeCurrentUserLogin(context, ui);
    } catch (OTPAuthenticationException e) {
      nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
      nswd.isOtp = true;
      nswd.otpType = e.getOtpType();
      Log.d(TAG, "Login failed due OTP required: " + e.getMessage());
      trackLabel = "OTP_REQUESTED";
    } catch (InvalidObjectException e) {
      nswd.loadingStatus = LoadingStatus.DATA_ERROR;
      Log.w(TAG, "Login failed due data format problem: " + e.getMessage(), e);
      trackLabel = "ERR_DATA";
    } catch (NoRouteToHostException e) {
      nswd.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
      trackLabel = "ERR_CONN_NOT_AVAILABLE";
    } catch (AuthenticationException e) {
      nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
      Log.d(TAG, "Login failed due authentication problem: " + e.getMessage());
      trackLabel = "ERR_AUTH";
    } catch (IOException e) {
      nswd.loadingStatus = LoadingStatus.CONN_ERROR;
      Log.w(TAG, "Login failed due connection problem: " + e.getMessage());
      trackLabel = "ERR_CONN";
    } catch (JSONException e) {
      nswd.loadingStatus = LoadingStatus.DATA_ERROR;
      Log.w(TAG, "Login failed due data format problem: " + e.getMessage());
      trackLabel = "ERR_DATA";
    } catch (Exception e) {
      nswd.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
      Log.e(TAG, "Login failed due: " + e.getMessage(), e);
      trackLabel = "ERR_UNKNOWN";
    }
    ActivityTracker.sendEvent(context, ActivityTracker.CAT_BE, "login", trackLabel, 0L);
    return nswd;
  }

  private String remoteLogin(Context context, GHCredentials cred, String otp) throws JSONException, NoRouteToHostException, AuthenticationException,
      ClientProtocolException, URISyntaxException, IOException {
    Map<String, String> headers = null;
    otp = Utils.trimToNull(otp);
    if (otp != null) {
      headers = new HashMap<String, String>();
      headers.put("X-GitHub-OTP", otp);
    }
    String content = GH_AUTH_REQ_CONTENT.replace("*fp*", System.currentTimeMillis() + "");
    Response<String> resp = RemoteSystemClient.putToURL(context, cred, GH_AUTH_REQ_URL, headers, content);
    JSONObject jo = new JSONObject(resp.data);
    return jo.getString("token");
  }

  private void storeCurrentUserLogin(Context context, GHUserLoginInfo currentUserLoginInfo) {
    Utils.writeToStore(TAG, context, getCuliFile(context), currentUserLoginInfo);
    if (currentUserInfo == null || (currentUserLoginInfo != null && !currentUserLoginInfo.getUsername().equals(currentUserInfo.getUsername()))) {
      currentUserInfo = null;
      Utils.deleteFromStore(context, getCuFile(context));
      loadUserInfoFromServer(context, getGhApiCredentials(context));
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
      credentials = new GHCredentials(ui.getT(), "x-oauth-basic");
      return credentials;
    } else {
      return null;
    }
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

  protected void loadUserInfoFromServer(Context context, GHCredentials ghCredentials) {
    try {
      Response<JSONObject> r = RemoteSystemClient.getJSONObjectFromUrl(context, ghCredentials, GH_USER_REQ_URL, null);
      if (r.data != null) {
        currentUserInfo = new GHUserInfo(r.data);
        Utils.writeToStore(TAG, context, getCuFile(context), currentUserInfo);
      }
    } catch (Throwable th) {
      Log.e(TAG, "User informations loading error: " + th.getMessage(), th);
    }
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
