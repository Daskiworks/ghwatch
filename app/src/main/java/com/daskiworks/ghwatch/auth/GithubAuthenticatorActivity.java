package com.daskiworks.ghwatch.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Browser;
import android.util.Log;
import android.widget.Toast;

import com.daskiworks.ghwatch.ActivityTracker;
import com.daskiworks.ghwatch.R;
import com.daskiworks.ghwatch.StartActivity;
import com.daskiworks.ghwatch.backend.GHConstants;
import com.daskiworks.ghwatch.model.GHCredentials;
import com.daskiworks.ghwatch.model.GHUserInfo;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.openidconnect.IdTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Github OAuth authenticator activity.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class GithubAuthenticatorActivity extends AccountAuthenticatorActivity {

  private static final String TAG = "GithubAuthenticatorActi";

  public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
  public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
  public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

  /**
   * Global instance of the HTTP transport.
   */
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  private static final DataStoreFactory DATA_STORE_FACTORY;

  /**
   * Global instance of the JSON factory.
   */
  static final JsonFactory JSON_FACTORY = new JacksonFactory();

  private static final String TOKEN_SERVER_URL = "https://github.com/login/oauth/access_token";
  private static final String AUTHORIZATION_SERVER_URL = "https://github.com/login/oauth/authorize";
  private static String API_KEY;
  private static String API_SECRET;
  private static final String[] SCOPES = new String[]{"notifications", "repo"};
  private static final String REDIRECT_URI = "com.daskiworks.ghwatch.auth://login";

  private AccountManager accountManager;
  private AuthorizationCodeFlow flow;

  static {

    InputStream in = GHConstants.class.getResourceAsStream("clients.properties");
    try {
      Properties props = new Properties();
      if (in != null)
        props.load(in);
      else
        Log.e(TAG, "clients.properties file not found to get Github API keys from");
      API_SECRET = props.getProperty("clients");
      API_KEY = props.getProperty("clienti");
    } catch (IOException e) {
      Log.e(TAG, "Unable to load secrets for github authentication " + e.getMessage());
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (IOException e) {
          // OK
        }
    }

    DATA_STORE_FACTORY = new MemoryDataStoreFactory();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");
    accountManager = AccountManager.get(getBaseContext());

    //check existing accounts so we can handle refresh in browser tab
    Account account = AuthenticationManager.getInstance().getAccountFromSystemAccountManager(this);
    Log.d(TAG, "Existing account: " + account);
    if (account != null) {
      StartActivity.showMainPage(this, true);
    } else {
      setContentView(R.layout.activity_authenticator);

      try {
        flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                HTTP_TRANSPORT,
                JSON_FACTORY,
                new GenericUrl(TOKEN_SERVER_URL),
                new ClientParametersAuthentication(API_KEY, API_SECRET),
                API_KEY,
                AUTHORIZATION_SERVER_URL)
                .setScopes(Arrays.asList(SCOPES))
                .setDataStoreFactory(DATA_STORE_FACTORY)
                //.setDataStoreFactory(DATA_STORE_FACTORY)
                .build();

        if (!isRedirect(getIntent())) {
          String authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
          Log.d(TAG, "redirecting to authorizationUrl=" + authorizationUrl);
          // Open the login page in the native browser
          //TODO OAUTH2 how to make sure browser's tab is closed after the auth flow finishes?
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl));
          //browserIntent.putExtra(Browser.EXTRA_APPLICATION_ID, this.getPackageName());
          browserIntent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, Boolean.TRUE);
          browserIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
          browserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          browserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
          browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(browserIntent);
        }
      } catch (Exception ex) {
        Log.e(TAG, ex.getMessage());
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    //check existing accounts so we can handle refresh in browser tab
    Account account = AuthenticationManager.getInstance().getAccountFromSystemAccountManager(this);
    Log.d(TAG, "Existing account: " + account);
    if (account != null) {
      StartActivity.showMainPage(this, true);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    if (isRedirect(intent)) {
      String authorizationCode = extractAuthorizationCode(intent);
      Log.d(TAG, "Executing GetTokens for authorizationCode=" + authorizationCode);
      new GetTokens(flow).execute(authorizationCode);
    }
  }

  private boolean isRedirect(Intent intent) {
    String data = intent.getDataString();
    return Intent.ACTION_VIEW.equals(intent.getAction()) && data != null;
  }

  private String extractAuthorizationCode(Intent intent) {
    String data = intent.getDataString();
    Log.d(TAG, "ExtractAuthorizationCode from data=" + data);
    Uri uri = Uri.parse(data);
    return uri.getQueryParameter("code");
  }

  private class GetTokens extends AsyncTask<String, Integer, IdTokenResponse> {

    private final AuthorizationCodeFlow flow;

    public GetTokens(AuthorizationCodeFlow flow) {
      this.flow = flow;
    }

    protected IdTokenResponse doInBackground(String... params) {
      try {
        TokenRequest request = flow.newTokenRequest(params[0])
                .setRedirectUri(REDIRECT_URI).setRequestInitializer(new HttpRequestInitializer() {
                  @Override
                  public void initialize(HttpRequest request) throws IOException {
                    request.getHeaders().setAccept("application/json");
                  }
                });

        return IdTokenResponse.execute(request);
      } catch (Exception ex) {
        Log.e(TAG, "AccessTokenObtainingError: " + ex.getMessage());
        return null;
      }
    }

    protected void onPostExecute(IdTokenResponse result) {
      //DO NOT LOG accessToken in PROD code!!!!
      if (GHConstants.DEBUG && result != null) {
        Log.d(TAG, "Obtained accessToken=" + result.getAccessToken());
      }
      new GetUserInfo(result).execute();
    }

  }

  private class GetUserInfo extends AsyncTask<String, Integer, GHUserInfo> {

    IdTokenResponse result;

    public GetUserInfo(IdTokenResponse result) {
      this.result = result;
    }

    protected GHUserInfo doInBackground(String... params) {
      if (result == null)
        return null;
      AuthenticationManager am = AuthenticationManager.getInstance();
      return am.loadUserInfoFromServer(GithubAuthenticatorActivity.this, am.createGHCredentials(result.getAccessToken()));
    }

    protected void onPostExecute(GHUserInfo userInfo) {
      Log.d(TAG, "userInfo=" + userInfo);
      String trackLabel = "OK";
      if (userInfo == null) {
        trackLabel = "ERROR";
        Toast.makeText(GithubAuthenticatorActivity.this.getApplicationContext(), R.string.auth_err_comm, Toast.LENGTH_LONG).show();
      } else {
        String userName = userInfo.getUsername();
        Account account = new Account(userName, GithubAccountAuthenticator.ACCOUNT_TYPE);
        accountManager.addAccountExplicitly(account, null, null);
        accountManager.setAuthToken(account, GithubAccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN, result.getAccessToken());

        Bundle data = new Bundle();
        data.putString(AccountManager.KEY_ACCOUNT_NAME, userName);
        data.putString(AccountManager.KEY_ACCOUNT_TYPE, GithubAccountAuthenticator.ACCOUNT_TYPE);
        data.putString(AccountManager.KEY_AUTHTOKEN, result.getAccessToken());

        Intent intent = new Intent();
        intent.putExtras(data);

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
      }
      ActivityTracker.sendEvent(GithubAuthenticatorActivity.this.getApplicationContext(), ActivityTracker.CAT_BE, "loginOauth2", trackLabel, 0L);
      finish();

    }

  }
}
