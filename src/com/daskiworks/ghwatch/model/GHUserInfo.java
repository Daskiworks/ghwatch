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
package com.daskiworks.ghwatch.model;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.daskiworks.ghwatch.backend.AuthenticationManager;

/**
 * Info about GH user. See {@link AuthenticationManager}.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class GHUserInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String username;
  private String name;
  private String avatarUrl;
  private String htmlUrl;
  private long updateTimestamp;

  public GHUserInfo() {

  }

  public GHUserInfo(JSONObject data) throws JSONException {
    super();
    this.username = data.getString("login");
    this.name = data.getString("name");
    this.avatarUrl = data.getString("avatar_url");
    this.htmlUrl = data.getString("html_url");
    this.updateTimestamp = System.currentTimeMillis();
  }

  public String getUsername() {
    return username;
  }

  public String getName() {
    return name;
  }

  public long getUpdateTimestamp() {
    return updateTimestamp;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public String getHtmlUrl() {
    return htmlUrl;
  }

}
