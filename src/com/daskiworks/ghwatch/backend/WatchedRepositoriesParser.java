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

import java.io.InvalidObjectException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.daskiworks.ghwatch.model.NotificationStream;
import com.daskiworks.ghwatch.model.Repository;
import com.daskiworks.ghwatch.model.WatchedRepositories;

/**
 * Helper class used to parse {@link NotificationStream} from JSON data read from server.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class WatchedRepositoriesParser {

  @SuppressWarnings("unused")
  private static final String TAG = WatchedRepositoriesParser.class.getSimpleName();

  public static WatchedRepositories parseNotificationStream(JSONArray json) throws InvalidObjectException {
    WatchedRepositories ret = new WatchedRepositories();
    try {

      for (int i = 0; i < json.length(); i++) {
        JSONObject repository = json.getJSONObject(i);
        JSONObject owner = repository.getJSONObject("owner");
        ret.addRepository(new Repository(repository.getLong("id"), repository.getString("url"), repository.getString("full_name"), owner
            .getString("avatar_url"), repository.getString("html_url")));
      }
    } catch (Exception e) {
      throw new InvalidObjectException("JSON message is invalid: " + e.getMessage());
    }
    return ret;
  }
}
