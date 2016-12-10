/*
 * Copyright 2015 contributors as indicated by the @authors tag.
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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.NoRouteToHostException;

import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.daskiworks.ghwatch.backend.RemoteSystemClient.Response;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;

/**
 * Template used to load JSON data from remote system. Error handling is covered.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public abstract class RemoteJSONObjectGetTemplate<T extends BaseViewData, U> {

  protected String TAG;

  protected Context context;

  protected AuthenticationManager authenticationManager;

  protected String logName;

  /**
   * @param TAG to be used for logging
   * @param context to be used
   * @param authenticationManager to be used for authenticating against remote system
   * @param logName loaded object name to be used in error log messages
   */
  public RemoteJSONObjectGetTemplate(String TAG, Context context, AuthenticationManager authenticationManager, String logName) {
    super();
    this.TAG = TAG;
    this.context = context;
    this.authenticationManager = authenticationManager;
    this.logName = logName;
  }

  /**
   * @return object instance returned later from {@link #loadData(String, Object)}
   */
  protected abstract T createResponseObject();

  /**
   * Process data loaded from remote system. Called only if data are loaded successfully.
   * 
   * @param remoteResponse JSON data from remote system
   * @param returnValue created by {@link #createResponseObject()} to fill loaded data into
   * @param inputObject passed to {@link #loadData(String, Object)} method. Eg. input object to fill data in and return in response.
   * @throws JSONException
   */
  protected abstract void processData(JSONObject remoteResponse, T returnValue, U inputObject) throws JSONException;

  /**
   * Load data from remote system. Thread safe implementation is a plus.
   * 
   * @param apiUrl to load data from. Can be null, but data error response is returned in this case
   * @param inputObject optional input object to be used in {@link #processData(JSONObject, BaseViewData, Object)}
   * @return view representation of loaded data with appropriate status
   */
  public T loadData(String apiUrl, U inputObject) {
    T ret = createResponseObject();
    if (apiUrl != null) {
      try {
        Log.d(TAG, logName + " loading data from: " + apiUrl);
        Response<JSONObject> resp = RemoteSystemClient.getJSONObjectFromUrl(context, authenticationManager.getGhApiCredentials(context), apiUrl, null);
        processData(resp.data, ret, inputObject);
      } catch (InvalidObjectException e) {
        ret.loadingStatus = LoadingStatus.DATA_ERROR;
        Log.w(TAG, logName + " loading failed due data format problem: " + e.getMessage(), e);
      } catch (NoRouteToHostException e) {
        ret.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
      } catch (AuthenticationException e) {
        ret.loadingStatus = LoadingStatus.AUTH_ERROR;
      } catch (IOException e) {
        Log.w(TAG, logName + " loading failed due connection problem: " + e.getMessage());
        ret.loadingStatus = LoadingStatus.CONN_ERROR;
      } catch (JSONException e) {
        ret.loadingStatus = LoadingStatus.DATA_ERROR;
        Log.w(TAG, logName + " loading failed due data format problem: " + e.getMessage());
      } catch (Exception e) {
        Log.e(TAG, logName + " loading failed due unexpected error: " + e.getMessage(), e);
        ret.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
      }
    } else {
      ret.loadingStatus = LoadingStatus.DATA_ERROR;
      Log.e(TAG, logName + " loading failed because source URL is not available");
    }
    return ret;
  }

}
