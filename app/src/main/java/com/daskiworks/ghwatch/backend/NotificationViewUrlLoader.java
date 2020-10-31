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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.auth.AuthenticationManager;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.StringViewData;

/**
 * Object used to load notification view URL.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class NotificationViewUrlLoader extends RemoteJSONObjectGetTemplate<StringViewData, Object> {

  public NotificationViewUrlLoader(String TAG, Context context, AuthenticationManager authenticationManager) {
    super(TAG, context, authenticationManager, "Notification html view URL");
  }

  @Override
  protected StringViewData createResponseObject() {
    return new StringViewData();
  }

  @Override
  protected void processData(JSONObject remoteResponse, StringViewData returnValue, Object inputObject) throws JSONException {
    returnValue.data = Utils.trimToNull(remoteResponse.getString("html_url"));
    if (returnValue.data == null) {
      returnValue.loadingStatus = LoadingStatus.DATA_ERROR;
    }
  }

}
