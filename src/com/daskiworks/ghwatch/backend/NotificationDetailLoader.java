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

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.model.Notification;
import com.daskiworks.ghwatch.model.NotificationStream;
import com.daskiworks.ghwatch.model.NotificationViewData;

/**
 * Loader used to load notification detail. It is stored back to the notification stream persistent store not to be loaded again!
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class NotificationDetailLoader extends RemoteJSONObjectGetTemplate<NotificationViewData, Notification> {

  private File persistFile;

  /**
   * @param TAG for log and synchronization of notification stream persistence!
   * @param context
   * @param authenticationManager
   * @param notificationsStreamPersistFile used for notification stream persistence so we can write loaded data into.
   */
  public NotificationDetailLoader(String TAG, Context context, AuthenticationManager authenticationManager, File notificationsStreamPersistFile) {
    super(TAG, context, authenticationManager, "Notification detail");
    this.persistFile = notificationsStreamPersistFile;
  }

  @Override
  protected NotificationViewData createResponseObject() {
    return new NotificationViewData();
  }

  @Override
  protected void processData(JSONObject remoteResponse, NotificationViewData returnValue, Notification inputObject) throws JSONException {
    if (inputObject != null) {
      inputObject.setSubjectDetailHtmlUrl(Utils.trimToNull(remoteResponse.getString("html_url")));
      if (inputObject.getSubjectDetailHtmlUrl() == null) {
        Log.w(TAG, "Notification detail loading problem due data format problem: no 'html_url' field in response");
      }

      if (remoteResponse.has("merged") && remoteResponse.getBoolean("merged")) {
        inputObject.setSubjectStatus("merged");
      } else {
        inputObject.setSubjectStatus(Utils.trimToNull(remoteResponse.getString("state")));
      }
      inputObject.setDetailLoaded(true);

      synchronized (TAG) {
        NotificationStream ns = Utils.readFromStore(TAG, context, persistFile);
        if (ns != null) {
          Notification n = ns.getNotificationById(inputObject.getId());
          if (n != null) {
            n.setSubjectDetailHtmlUrl(inputObject.getSubjectDetailHtmlUrl());
            n.setSubjectStatus(inputObject.getSubjectStatus());
            n.setDetailLoaded(true);
            Utils.writeToStore(TAG, context, persistFile, ns);
          }
        }
      }
      returnValue.notification = inputObject;
    }
  }

}
