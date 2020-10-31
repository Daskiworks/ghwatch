package com.daskiworks.ghwatch.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Github OAuth authenticator.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class GithubAccountAuthenticator extends AbstractAccountAuthenticator {

  public static final String ACCOUNT_TYPE = "com.daskiworks.ghwatch.auth";
  public static final String AUTH_TOKEN_TYPE_ACCESS_TOKEN = "accessToken";


  private final Context mContext;

  public GithubAccountAuthenticator(Context context) {
    super(context);
    this.mContext = context;
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    return null;
  }

  @Override
  public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
    final Intent intent = new Intent(mContext, GithubAuthenticatorActivity.class);
    intent.putExtra(GithubAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
    intent.putExtra(GithubAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
    intent.putExtra(GithubAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

    final Bundle bundle = new Bundle();
    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
    return bundle;
  }

  @Override
  public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
    return null;
  }

  @Override
  public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
    return null;
  }

  @Override
  public String getAuthTokenLabel(String authTokenType) {
    return null;
  }

  @Override
  public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
    return null;
  }

  @Override
  public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
    return null;
  }
}
