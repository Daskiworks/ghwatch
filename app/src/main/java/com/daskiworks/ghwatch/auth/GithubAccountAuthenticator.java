package com.daskiworks.ghwatch.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.daskiworks.ghwatch.R;

/**
 * Github OAuth authenticator.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class GithubAccountAuthenticator extends AbstractAccountAuthenticator {

  private static final String TAG = "GithubAccountAuthentica";

  public static final String ACCOUNT_TYPE = "com.daskiworks.ghwatch.auth";
  public static final String AUTH_TOKEN_TYPE_ACCESS_TOKEN = "accessToken";
  private static final int ERROR_CODE_ONE_ACCOUNT_ALLOWED = 4242;

  private final Context mContext;
  private final Handler mHandler;

  public GithubAccountAuthenticator(Context context) {
    super(context);
    this.mContext = context;
    this.mHandler = new Handler();
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    return null;
  }

  @Override
  public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {

    Log.d(TAG, "addAccount(" + accountType + "," + authTokenType + ")");

    // allow only one account to be created
    AccountManager accountManager = AccountManager.get(mContext);
    Account[] accs = accountManager.getAccountsByType(GithubAccountAuthenticator.ACCOUNT_TYPE);
    if (accs != null && accs.length > 0) {
      final Bundle result = new Bundle();
      result.putInt(AccountManager.KEY_ERROR_CODE, ERROR_CODE_ONE_ACCOUNT_ALLOWED);
      result.putString(AccountManager.KEY_ERROR_MESSAGE, mContext.getString(R.string.auth_err_one_account_allowed));

      mHandler.post(new Runnable() {
        @Override
        public void run() {

          Toast.makeText(mContext.getApplicationContext(), R.string.auth_err_one_account_allowed, Toast.LENGTH_LONG).show();
        }
      });

      return result;
    }

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
    Log.d(TAG, "getAuthToken(" + authTokenType + ")");
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
