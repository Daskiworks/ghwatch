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
import java.io.InvalidObjectException;
import java.net.NoRouteToHostException;
import java.net.URISyntaxException;

import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.util.Log;

import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.backend.RemoteSystemClient.Response;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.Repository;
import com.daskiworks.ghwatch.model.WatchedRepositories;
import com.daskiworks.ghwatch.model.WatchedRepositoriesViewData;

/**
 * Service used to work with watched repositories.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class WatchedRepositoriesService {

  private static final String TAG = "WatchedRepositoriesService";

  /**
   * URL to load notifications from.
   */
  private static final String URL_NOTIFICATIONS = GHConstants.URL_BASE + "/user/subscriptions";

  /**
   * Name of file where data are persisted.
   */
  private static final String fileName = "WatchedRepositories.td";

  /**
   * Reload from server is forced automatically if data in persistent store are older than this timeout [millis]
   */
  private static final long FORCE_VIEW_RELOAD_AFTER = 12 * Utils.MILLIS_HOUR;

  private Context context;

  private File persistFile;

  private AuthenticationManager authenticationManager;

  /**
   * Create service.
   * 
   * @param context this service runs in
   */
  public WatchedRepositoriesService(Context context) {
    this.context = context;
    persistFile = context.getFileStreamPath(fileName);
    this.authenticationManager = AuthenticationManager.getInstance();
  }

  /**
   * Get watched repositories for view.
   * 
   * @param reloadStrategy if data should be reloaded from server
   * @return view data
   */
  public WatchedRepositoriesViewData getWatchedRepositoriesForView(ViewDataReloadStrategy reloadStrategy) {

    WatchedRepositoriesViewData nswd = new WatchedRepositoriesViewData();
    WatchedRepositories ns = null;
    synchronized (TAG) {
      WatchedRepositories oldNs = Utils.readFromStore(TAG, context, persistFile);

      // user from store if possible, apply timeout of data from store
      if (reloadStrategy == ViewDataReloadStrategy.IF_TIMED_OUT) {
        ns = oldNs;
        if (ns != null && ns.getLastFullUpdateTimestamp() < (System.currentTimeMillis() - FORCE_VIEW_RELOAD_AFTER))
          ns = null;
      } else if (reloadStrategy == ViewDataReloadStrategy.NEVER) {
        ns = oldNs;
      }

      // read from server
      try {
        if (ns == null && reloadStrategy != ViewDataReloadStrategy.NEVER) {
          ns = readFromServer(URL_NOTIFICATIONS);
          if (ns != null) {
            Utils.writeToStore(TAG, context, persistFile, ns);
          }
        }
      } catch (InvalidObjectException e) {
        nswd.loadingStatus = LoadingStatus.DATA_ERROR;
        Log.w(TAG, "Watched Repositories loading failed due data format problem: " + e.getMessage(), e);
      } catch (NoRouteToHostException e) {
        nswd.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
        Log.d(TAG, "Watched Repositories loading failed due connection not available.");
      } catch (AuthenticationException e) {
        nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
        Log.d(TAG, "Watched Repositories loading failed due authentication problem: " + e.getMessage());
      } catch (IOException e) {
        nswd.loadingStatus = LoadingStatus.CONN_ERROR;
        Log.w(TAG, "Watched Repositories loading failed due connection problem: " + e.getMessage());
      } catch (JSONException e) {
        nswd.loadingStatus = LoadingStatus.DATA_ERROR;
        Log.w(TAG, "Watched Repositories loading failed due data format problem: " + e.getMessage());
      } catch (Exception e) {
        nswd.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
        Log.e(TAG, "Watched Repositories loading failed due: " + e.getMessage(), e);
      }

      // Show content from store because we are unable to read new one but want to show something
      if (ns == null)
        ns = oldNs;

      nswd.repositories = ns;
      return nswd;
    }
  }

  /**
   * @param id of repository to unwatch
   * @return view data with result of call
   */
  public BaseViewData unwatchRepository(long id) {
    BaseViewData nswd = new BaseViewData();
    try {
      synchronized (TAG) {
        WatchedRepositories oldNs = Utils.readFromStore(TAG, context, persistFile);
        if (oldNs != null) {
          Repository ret = oldNs.removeRepositoryById(id);
          if (ret != null) {
            RemoteSystemClient.deleteToURL(context, authenticationManager.getGhApiCredentials(context),
                GHConstants.URL_BASE + "/repos/" + ret.getRepositoryFullName() + "/subscription", null);
            Utils.writeToStore(TAG, context, persistFile, oldNs);
          }
        }
      }
    } catch (NoRouteToHostException e) {
      nswd.loadingStatus = LoadingStatus.CONN_UNAVAILABLE;
    } catch (AuthenticationException e) {
      nswd.loadingStatus = LoadingStatus.AUTH_ERROR;
    } catch (IOException e) {
      Log.w(TAG, "Repository unwatch failed due connection problem: " + e.getMessage());
      nswd.loadingStatus = LoadingStatus.CONN_ERROR;
    } catch (Exception e) {
      Log.e(TAG, "Repository unwatch failed due: " + e.getMessage(), e);
      nswd.loadingStatus = LoadingStatus.UNKNOWN_ERROR;
    }
    return nswd;
  }

  protected WatchedRepositories readFromServer(String url) throws InvalidObjectException, NoRouteToHostException, AuthenticationException, IOException,
      JSONException, URISyntaxException {

    Response<JSONArray> resp = RemoteSystemClient.getJSONArrayFromUrl(context, authenticationManager.getGhApiCredentials(context), url, null);

    if (resp.notModified)
      return null;

    WatchedRepositories ns = WatchedRepositoriesParser.parseNotificationStream(resp.data);
    ns.setLastFullUpdateTimestamp(System.currentTimeMillis());
    return ns;
  }

  public void flushPersistentStore() {
    persistFile.delete();
  }

}
