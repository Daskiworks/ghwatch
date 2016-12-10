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

import android.content.Context;
import android.util.Log;

import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.model.Label;
import com.daskiworks.ghwatch.model.Notification;
import com.daskiworks.ghwatch.model.NotificationStream;
import com.daskiworks.ghwatch.model.NotificationViewData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Loader used to load Labels for Pull Requests during notification detail loading.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * @see NotificationDetailLoader
 */
public class NotificationDetailLabelLoader extends RemoteJSONObjectGetTemplate<NotificationViewData, Notification> {

  /**
   * @param TAG                   for log and synchronization of notification stream persistence!
   * @param context
   * @param authenticationManager
   */
  public NotificationDetailLabelLoader(String TAG, Context context, AuthenticationManager authenticationManager) {
    super(TAG, context, authenticationManager, "Notification Label detail");
  }

  @Override
  protected NotificationViewData createResponseObject() {
    return new NotificationViewData();
  }

  @Override
  protected void processData(JSONObject remoteResponse, NotificationViewData returnValue, Notification inputObject) throws JSONException {
    if (inputObject != null) {
      inputObject.setSubjectLabels(null);
      if (remoteResponse.has("labels")) {
        NotificationDetailLoader.processLabelsJson(remoteResponse, inputObject);
      }
    }
  }

}
