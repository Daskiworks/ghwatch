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

import com.daskiworks.ghwatch.backend.AuthenticationManager;
import com.daskiworks.ghwatch.backend.RemoteSystemClient;

/**
 * GH credentials class used to call remote API. See {@link RemoteSystemClient} and {@link AuthenticationManager#getGhApiCredentials()}.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class GHCredentials implements Serializable {

  private static final long serialVersionUID = 1L;

  private String username;
  private String password;

  public GHCredentials(String username, String password) {
    super();
    this.username = username;
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

}
