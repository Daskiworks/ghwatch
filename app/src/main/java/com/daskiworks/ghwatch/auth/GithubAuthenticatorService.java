package com.daskiworks.ghwatch.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service for Github OAuth authenticator.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class GithubAuthenticatorService  extends Service {
  @Override
  public IBinder onBind(Intent intent) {
    GithubAccountAuthenticator authenticator = new GithubAccountAuthenticator(this);
    return authenticator.getIBinder();
  }
}