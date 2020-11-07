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


import cz.msebera.android.httpclient.auth.AuthenticationException;

/**
 * Exception thrown when two factor authentication is necessary
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class OTPAuthenticationException extends AuthenticationException {

  private static final long serialVersionUID = 1L;
  private String otpType;

  public OTPAuthenticationException(String otpType) {
    super("Required two-factor authentication using OTP type " + otpType);
    this.otpType = otpType;
  }

  public String getOtpType() {
    return otpType;
  }

}
