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

import android.annotation.SuppressLint;
import android.util.Log;

import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.model.Notification;
import com.daskiworks.ghwatch.model.NotificationStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InvalidObjectException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * Helper class used to parse {@link NotificationStream} from JSON data read from server.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class NotificationStreamParser {

  private static final String TAG = NotificationStreamParser.class.getSimpleName();
  @SuppressLint("SimpleDateFormat")
  private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");

  public static interface IRepoVisibilityAdapter {
    public boolean isRepoVisibile(String repoFullName);
  }

  public static NotificationStream parseNotificationStream(JSONArray json, IRepoVisibilityAdapter repoVisibilityAdapter) throws InvalidObjectException {
    NotificationStream ret = new NotificationStream();
    try {

      for (int i = 0; i < json.length(); i++) {
        JSONObject notification = json.getJSONObject(i);
        JSONObject subject = notification.getJSONObject("subject");
        JSONObject repository = notification.getJSONObject("repository");
        String repoFullName = Utils.trimToNull(repository.getString("full_name"));
        if(repoFullName == null || !repoVisibilityAdapter.isRepoVisibile(repoFullName))
          continue;
        String updatedAtStr = Utils.trimToNull(notification.getString("updated_at"));
        Date updatedAt = null;
        try {
          if (updatedAtStr != null) {
            if (updatedAtStr.endsWith("Z"))
              updatedAtStr = updatedAtStr.replace("Z", "GMT");
            updatedAt = df.parse(updatedAtStr);
          }
        } catch (ParseException e) {
          Log.w(TAG, "Invalid date format for value: " + updatedAtStr);
        }

        ret.addNotification(new Notification(notification.getLong("id"), notification.getString("url"), subject.getString("title"), subject.getString("type"),
            subject.getString("url"), subject.getString("latest_comment_url"), repoFullName, repository.getJSONObject("owner").getString(
                "avatar_url"), updatedAt, notification.getString("reason")));

      }
    } catch (Exception e) {
      throw new InvalidObjectException("JSON message is invalid: " + e.getMessage());
    }
    return ret;
  }
}
