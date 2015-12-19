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

/**
 * Info about GH user login. See {@link AuthenticationManager}.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class GHUserLoginInfo implements Serializable {

  private static final long serialVersionUID = 3L;

  private AccountType accountType;
  private String username;
  private String t;
  private long loginDate;

  public GHUserLoginInfo(AccountType accountType, String username, String t) {
    super();
    this.accountType = accountType;
    this.username = username;
    this.t = t;
    this.loginDate = System.currentTimeMillis();
  }

  public String getUsername() {
    return username;
  }

  public String getT() {
    return t;
  }

  public long getLoginDate() {
    return loginDate;
  }

  public AccountType getAccountType() {
    return accountType;
  }

}
