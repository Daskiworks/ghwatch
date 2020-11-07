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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.model.GHCredentials;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;


import androidx.annotation.NonNull;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpException;
import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.HttpRequest;
import cz.msebera.android.httpclient.HttpRequestInterceptor;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.auth.AuthScope;
import cz.msebera.android.httpclient.auth.AuthState;
import cz.msebera.android.httpclient.auth.AuthenticationException;
import cz.msebera.android.httpclient.auth.Credentials;
import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.CredentialsProvider;
import cz.msebera.android.httpclient.client.methods.HttpDelete;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpPut;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.client.protocol.ClientContext;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.auth.BasicScheme;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.protocol.ExecutionContext;
import cz.msebera.android.httpclient.protocol.HttpContext;

/**
 * Helper class used to communicate with server.
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class RemoteSystemClient {

  public static class Response<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // we do not store data on disc, only statistics values
    public transient T data;

    public boolean notModified = false;
    public Long poolInterval;
    public String lastModified;
    public String rateLimit;
    public String rateLimitRemaining;
    public Long rateLimitReset;

    public long requestStartTime;
    public long requestStopTime;
    public long requestDuration;

    public String linkNext;

    protected void snapRequestDuration() {
      requestStopTime = System.currentTimeMillis();
      requestDuration = requestStopTime - requestStartTime;
    }

    public void fill(Response<?> r2) {
      r2.notModified = notModified;
      r2.poolInterval = poolInterval;
      r2.lastModified = lastModified;
      r2.rateLimit = rateLimit;
      r2.rateLimitRemaining = rateLimitRemaining;
      r2.rateLimitReset = rateLimitReset;
      r2.requestDuration = requestDuration;
      r2.requestStartTime = requestStartTime;
      r2.requestStopTime = requestStopTime;
      r2.linkNext = linkNext;
    }
  }

  private static final String TAG = "RemoteSystemClient";

  private static File errorLogFile;


  /**
   * Get file where Github API call error are logged.
   *
   * @param context to be used
   * @return file instance
   */
  public synchronized static File getErrorLogFile(Context context) {
    if (errorLogFile == null)
      errorLogFile = new File(context.getExternalCacheDir(), "githubApiCallErrors.txt");
    return errorLogFile;
  }

  protected synchronized static void logGithubAPiCallError(Context context, IOException exception) {
    if (!PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_LOG_GITHUB_API_CALL_ERROR_TO_FILE, false)) {
      return;
    }
    File f = getErrorLogFile(context);
    PrintWriter output = null;
    try {
      output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8")));
      output.println(DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + " - " + exception.getMessage());
      output.flush();
    } catch (IOException e) {
      Log.w(TAG, "Can't write Github API call error into log file due to: " + e.getMessage());
    } finally {
      if (output != null) {
        output.close();
      }
    }
  }

  /**
   * Get JSON array from specified url. It is contained in <code>list</code> item of output JSO
   *
   * @param context used to access services
   * @param url     to load data from
   * @return JSON array object
   * @throws NoRouteToHostException  if internet connection is not available
   * @throws AuthenticationException if authentication fails
   * @throws IOException             if there is problem during data readig from server
   * @throws JSONException           if returned JSON is invalid
   * @throws URISyntaxException      if url is invalid
   */
  public static Response<JSONArray> getJSONArrayFromUrl(Context context, GHCredentials apiCredentials, String url, Map<String, String> headers)
          throws AuthenticationException, IOException, JSONException, URISyntaxException {
    Response<String> wr = readInternetDataGet(context, apiCredentials, url, headers);
    Response<JSONArray> ret = new Response<JSONArray>();
    wr.fill(ret);
    if (!wr.notModified) {
      ret.data = new JSONArray(wr.data);
    }
    return ret;
  }

  /**
   * Get JSON object from specified url. It is contained in <code>list</code> item of output JSON
   *
   * @param context used to get services over
   * @param url     to load data from
   * @return JSON object
   * @throws NoRouteToHostException  if internet connection is not available
   * @throws AuthenticationException if authentication fails
   * @throws IOException             if there is problem during data readig from server
   * @throws JSONException           if returned JSON is invalid
   * @throws URISyntaxException      if url is invalid
   */
  public static Response<JSONObject> getJSONObjectFromUrl(Context context, GHCredentials apiCredentials, String url, Map<String, String> headers)
          throws AuthenticationException, IOException, JSONException, URISyntaxException {
    Response<String> wr = readInternetDataGet(context, apiCredentials, url, headers);
    Response<JSONObject> ret = new Response<JSONObject>();
    wr.fill(ret);
    if (!wr.notModified) {
      ret.data = new JSONObject(wr.data);
    }
    return ret;
  }

  private static Response<String> readInternetDataGet(Context context, GHCredentials apiCredentials, String url, Map<String, String> headers)
          throws URISyntaxException, IOException, AuthenticationException {
    if (!Utils.isInternetConnectionAvailable(context))
      throw new NoRouteToHostException("Network not available");

    Log.d(TAG, "Going to perform GET request to " + url);

    try {
      URI uri = new URI(url);
      DefaultHttpClient httpClient = prepareHttpClient(uri);

      HttpGet httpGet = new HttpGet(uri);
      setAuthenticationHeader(httpGet, apiCredentials);
      setHeaders(httpGet, requestGzipCompression(headers));

      // create response object here to measure request duration
      Response<String> ret = new Response<String>();
      ret.requestStartTime = System.currentTimeMillis();

      HttpResponse httpResponse = httpClient.execute(httpGet);
      int code = httpResponse.getStatusLine().getStatusCode();

      parseResponseHeaders(context, httpResponse, ret);
      Log.d(TAG, "Response http code: " + code);
      if (code == HttpStatus.SC_NOT_MODIFIED) {
        ret.notModified = true;
        ret.snapRequestDuration();
        writeReponseInfo(ret, context);
        return ret;
      }
      processStandardHttpResponseCodes(httpResponse);

      ret.data = getResponseContentAsString(httpResponse);
      ret.snapRequestDuration();
      writeReponseInfo(ret, context);
      return ret;
    } catch (IOException e) {
      logGithubAPiCallError(context, e);
      throw e;
    }
  }

  /**
   * Request Gzip compression by the server by adding relevant header.
   *
   * @param headers map to add into, can be null
   * @return header map, never null
   */
  @NonNull
  private static Map<String, String> requestGzipCompression(Map<String, String> headers) {
    //request gzip compression
    if (headers == null)
      headers = new HashMap<>();
    headers.put("Accept-Encoding", "gzip");
    return headers;
  }

  protected static String getResponseContentAsString(HttpResponse httpResponse) throws IOException {
    if (httpResponse == null)
      return null;
    HttpEntity httpEntity = httpResponse.getEntity();
    if (httpEntity != null) {
      InputStream is = httpEntity.getContent();
      //handle gzip compression if used by the server
      if ("gzip".equals(getHeaderValue(httpResponse, "Content-Encoding"))) {
        is = new GZIPInputStream(is);
      }

      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 50);
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
          sb.append(line + "\n");
        }
        return sb.toString();
      } finally {
        if (is != null)
          is.close();
      }
    } else {
      return null;
    }
  }

  protected static void setHeaders(HttpRequestBase httpRequest, Map<String, String> headers) {
    httpRequest.setHeader("User-Agent", "GH::watch");
    if (headers != null) {
      for (Entry<String, String> he : headers.entrySet()) {
        Log.d(TAG, "Set request header " + he.getKey() + ":" + he.getValue());
        httpRequest.setHeader(he.getKey(), he.getValue());
      }
    }
  }

  public static Response<?> postNoData(Context context, GHCredentials apiCredentials, String url, Map<String, String> headers) throws
          URISyntaxException, IOException, AuthenticationException {
    if (!Utils.isInternetConnectionAvailable(context))
      throw new NoRouteToHostException("Network not available");
    Log.d(TAG, "Going to perform POST request to " + url);

    try {
      URI uri = new URI(url);
      DefaultHttpClient httpClient = prepareHttpClient(uri);

      HttpPost httpPost = new HttpPost(uri);
      setAuthenticationHeader(httpPost, apiCredentials);
      setHeaders(httpPost, headers);

      // create response object here to measure request duration
      Response<String> ret = new Response<String>();
      ret.requestStartTime = System.currentTimeMillis();

      HttpResponse httpResponse = httpClient.execute(httpPost);
      parseResponseHeaders(context, httpResponse, ret);

      processStandardHttpResponseCodes(httpResponse);

      ret.snapRequestDuration();
      writeReponseInfo(ret, context);
      return ret;
    } catch (IOException e) {
      logGithubAPiCallError(context, e);
      throw e;
    }
  }


  protected static void setAuthenticationHeader(HttpRequestBase request, GHCredentials apiCredentials) {
    if (apiCredentials != null)
      request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(apiCredentials.getUsername(), apiCredentials.getPassword()), "UTF-8", false));
  }

  protected static void setJsonContentTypeHeader(HttpRequestBase request) {
    request.setHeader("Content-Type", "application/json; charset=utf-8");
  }

  public static Response<String> putToURL(Context context, GHCredentials apiCredentials, String url, Map<String, String> headers, String content)
          throws URISyntaxException, IOException, AuthenticationException {
    if (!Utils.isInternetConnectionAvailable(context))
      throw new NoRouteToHostException("Network not available");

    Log.d(TAG, "Going to perform PUT request to " + url);

    try {
      URI uri = new URI(url);
      DefaultHttpClient httpClient = prepareHttpClient(uri);


      HttpPut httpPut = new HttpPut(uri);

      setAuthenticationHeader(httpPut, apiCredentials);
      setJsonContentTypeHeader(httpPut);
      setHeaders(httpPut, requestGzipCompression(headers));

      if (content != null)
        httpPut.setEntity(new StringEntity(content, "UTF-8"));

      // create response object here to measure request duration
      Response<String> ret = new Response<String>();
      ret.requestStartTime = System.currentTimeMillis();

      HttpResponse httpResponse = httpClient.execute(httpPut);

      parseResponseHeaders(context, httpResponse, ret);

      processStandardHttpResponseCodes(httpResponse);

      ret.data = getResponseContentAsString(httpResponse);

      ret.snapRequestDuration();
      writeReponseInfo(ret, context);
      return ret;
    } catch (IOException e) {
      logGithubAPiCallError(context, e);
      throw e;
    }
  }

  public static Response<String> deleteToURL(Context context, GHCredentials apiCredentials, String url, Map<String, String> headers)
          throws URISyntaxException, IOException, AuthenticationException {
    if (!Utils.isInternetConnectionAvailable(context))
      throw new NoRouteToHostException("Network not available");

    try {
      URI uri = new URI(url);
      DefaultHttpClient httpClient = prepareHttpClient(uri);

      HttpDelete httpPut = new HttpDelete(uri);
      setAuthenticationHeader(httpPut, apiCredentials);
      setHeaders(httpPut, requestGzipCompression(headers));

      // create response object here to measure request duration
      Response<String> ret = new Response<String>();
      ret.requestStartTime = System.currentTimeMillis();

      HttpResponse httpResponse = httpClient.execute(httpPut);

      parseResponseHeaders(context, httpResponse, ret);

      processStandardHttpResponseCodes(httpResponse);

      ret.data = getResponseContentAsString(httpResponse);

      ret.snapRequestDuration();
      writeReponseInfo(ret, context);
      return ret;
    } catch (IOException e) {
      logGithubAPiCallError(context, e);
      throw e;
    }
  }

  protected static void processStandardHttpResponseCodes(HttpResponse httpResponse) throws AuthenticationException, IOException {
    int code = httpResponse.getStatusLine().getStatusCode();
    Log.d(TAG, "Response http code: " + code);
    if (code >= 200 && code <= 299)
      return;
    if (code == HttpStatus.SC_UNAUTHORIZED || code == HttpStatus.SC_FORBIDDEN) {
      String OTP = getHeaderValue(httpResponse, "X-GitHub-OTP");
      if (code == HttpStatus.SC_UNAUTHORIZED && OTP != null && OTP.contains("required")) {
        throw new OTPAuthenticationException(Utils.trimToNull(OTP.replace("required;", "")));
      }
      throw new AuthenticationException("Authentication problem: " + getResponseContentAsString(httpResponse));
    } else if (code == HttpStatus.SC_BAD_REQUEST || code == HttpStatus.SC_NOT_FOUND) {
      throw new InvalidObjectException("HttpCode=" + code + " message: " + getResponseContentAsString(httpResponse));
    } else {
      throw new IOException("HttpCode=" + code + " message: " + getResponseContentAsString(httpResponse));
    }
  }

  protected static DefaultHttpClient prepareHttpClient(URI uri) {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpParams params = httpClient.getParams();
    HttpConnectionParams.setConnectionTimeout(params, 30000);
    HttpConnectionParams.setSoTimeout(params, 30000);
    return httpClient;
  }

  protected static void parseResponseHeaders(Context context, HttpResponse httpResponse, Response<String> ret) {
    if (Log.isLoggable(TAG, Log.DEBUG))
      Log.d(TAG, "HTTP Response headers: " + dumpHeaders(httpResponse.getAllHeaders()));

    ret.lastModified = getHeaderValue(httpResponse, "Last-Modified");
    ret.rateLimit = getHeaderValue(httpResponse, "X-RateLimit-Limit");
    ret.rateLimitRemaining = getHeaderValue(httpResponse, "X-RateLimit-Remaining");

    String v = getHeaderValue(httpResponse, "X-RateLimit-Reset");
    if (v != null) {
      try {
        ret.rateLimitReset = Long.parseLong(v) * 1000;
        Log.d(TAG, "Response header X-RateLimit-Reset parsed: " + ret.rateLimitReset);
      } catch (Exception e) {
        Log.w(TAG, "Problem with 'X-RateLimit-Reset' header value '" + v + "' parsing: " + e.getMessage());
      }
    }

    String vpi = getHeaderValue(httpResponse, "X-Poll-Interval");
    if (vpi != null) {
      try {
        ret.poolInterval = Long.valueOf(vpi);
      } catch (NumberFormatException e) {
        Log.w(TAG, "'X-Poll-Interval' header value is not a number: " + vpi);
      }
    }

    String linkH = getHeaderValue(httpResponse, "Link");
    if (linkH != null) {
      for (String linkPart : linkH.split(",")) {
        if (linkPart != null && linkPart.contains("rel=\"next\"")) {
          linkPart = linkPart.trim();
          ret.linkNext = Utils.trimToNull(linkPart.substring(linkPart.indexOf("<")+1, linkPart.indexOf(">;")));
        }
      }
    }
    Log.d(TAG, "Response Link.next parsed: " + ret.linkNext);
  }

  private static String dumpHeaders(Header[] headers) {
    if (headers == null || headers.length == 0)
      return "";

    StringBuilder sb = new StringBuilder();
    for (Header h : headers) {
      sb.append("\n").append(h.getName()).append(": ").append(h.getValue());
    }
    return sb.toString();
  }

  private static void writeReponseInfo(Response<?> response, Context context) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = sharedPref.edit();
    editor.putString(PreferencesUtils.INT_SERVERINFO_APILIMIT, response.rateLimit);
    editor.putString(PreferencesUtils.INT_SERVERINFO_APILIMITREMAINING, response.rateLimitRemaining);
    editor.putString(PreferencesUtils.INT_SERVERINFO_APILIMITRESETTIMESTAMP, response.rateLimitReset + "");
    editor.putString(PreferencesUtils.INT_SERVERINFO_LASTREQUESTDURATION, response.requestDuration + "");
    editor.commit();
  }

  private static String getHeaderValue(HttpResponse httpResponse, String headerName) {
    Header header = httpResponse.getLastHeader(headerName);
    if (header != null)
      return Utils.trimToNull(header.getValue());
    return null;
  }

  private static HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
      AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
      CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
      HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

      if (authState.getAuthScheme() == null) {
        AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
        Credentials creds = credsProvider.getCredentials(authScope);
        if (creds != null) {
          authState.setAuthScheme(new BasicScheme());
          authState.setCredentials(creds);
        }
      }
    }
  };

}
