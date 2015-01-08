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
package com.daskiworks.ghwatch;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.daskiworks.ghwatch.backend.PreferencesUtils;

/**
 * {@link DialogFragment} for Support App Development dialog.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class SupportAppDevelopmentDialogFragment extends DialogFragment {

  public static final String PREF_LAST_SUPPORT_SHOW_TIMESTAMP = "LAST_SUPPORT_SHOW_TIMESTAMP";

  private static final String INAPP_CODE_DONATION_1 = "donation_1";
  private static final String INAPP_CODE_DONATION_2 = "donation_2";

  private static final String TAG = SupportAppDevelopmentDialogFragment.class.getSimpleName();
  private static final long AUTO_SHOW_PERIOD = 180L * Utils.MILLIS_DAY;
  private static final long AUTO_SHOW_FIRST = 10L * Utils.MILLIS_DAY;

  public static boolean isAutoShowScheduled(MainActivity activity) {
    long lastShowTimestamp = PreferencesUtils.getLong(activity, SupportAppDevelopmentDialogFragment.PREF_LAST_SUPPORT_SHOW_TIMESTAMP, 0);
    if (lastShowTimestamp == 0) {
      storeTimestampOfLastShow(activity, System.currentTimeMillis() - (AUTO_SHOW_PERIOD - AUTO_SHOW_FIRST));
      (new BackupManager(activity)).dataChanged();
    } else {
      return lastShowTimestamp <= (System.currentTimeMillis() - AUTO_SHOW_PERIOD);
    }
    return false;
  }

  IInAppBillingService mService;

  ServiceConnection mServiceConn = new ServiceConnection() {
    @Override
    public void onServiceDisconnected(ComponentName name) {
      mService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      mService = IInAppBillingService.Stub.asInterface(service);
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = new Intent(IabHelper.INTENT_ACTION_BIND);
    intent.setPackage(IabHelper.INTENT_PACKAGE);
    getActivity().bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
  };

  @Override
  public void onDestroy() {
    if (mService != null) {
      getActivity().unbindService(mServiceConn);
    }
    super.onDestroy();
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    writeActionPerformedTimestamp(getActivity());
    super.onDismiss(dialog);
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    writeActionPerformedTimestamp(getActivity());
    super.onCancel(dialog);
  }

  @SuppressLint("InflateParams")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(R.string.dialog_sad_title);
    View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_support_app_development, null);
    builder.setView(view);

    view.findViewById(R.id.button_social_rate).setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "app_support_rate", null, 0L);
        openBrowser("market://details?id=" + getActivity().getPackageName());
      }

    });

    view.findViewById(R.id.button_social_share).setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "http://play.google.com/store/apps/details?id=" + getActivity().getPackageName());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.dialog_sad_button_share_title)));
        ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "app_support_share", null, 0L);
        writeActionPerformedTimestamp(getActivity());
      }
    });

    view.findViewById(R.id.button_donate_1).setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        buyItem(INAPP_CODE_DONATION_1);
      }
    });

    view.findViewById(R.id.button_donate_2).setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        buyItem(INAPP_CODE_DONATION_2);
      }
    });

    view.findViewById(R.id.button_src).setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "app_support_sourcecode", null, 0L);
        openBrowser("https://github.com/Daskiworks/ghwatch");
      }

    });

    view.findViewById(R.id.button_bug).setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "app_support_bug_feature", null, 0L);
        openBrowser("https://github.com/Daskiworks/ghwatch/issues");
      }

    });

    builder.setCancelable(true);
    builder.setPositiveButton(R.string.dialog_sad_button_close, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        writeActionPerformedTimestamp(getActivity());
      }
    });

    ActivityTracker.sendView(getActivity(), TAG);
    return builder.create();
  }

  /**
   * @param sku id of product we want to buy
   * @return true if buy flow started
   */
  private void buyItem(String sku) {
    ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "app_support_donate_click", sku, 0L);
    if (mService != null) {
      try {
        Bundle buyIntentBundle = mService.getBuyIntent(3, getActivity().getPackageName(), sku, IabHelper.ITEM_TYPE_INAPP, null);
        int rc = buyIntentBundle.getInt(IabHelper.RESPONSE_CODE);
        if (rc == IabHelper.BILLING_RESPONSE_RESULT_OK) {
          PendingIntent pendingIntent = buyIntentBundle.getParcelable(IabHelper.RESPONSE_BUY_INTENT);
          getActivity().startIntentSender(pendingIntent.getIntentSender(), new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
          writeActionPerformedTimestamp(getActivity());
          return;
        } else {
          ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_BE, "in_app_billing_error", "GetBuyIntent error " + rc, 0L);
          Log.e(TAG, "InApp billing - GetBuyIntent response error with code " + rc);
        }
      } catch (Exception e) {
        ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_BE, "in_app_billing_error", e.getMessage(), 0L);
        Log.e(TAG, "InApp billing - exception " + e.getMessage());
      }
      Toast.makeText(getActivity(), R.string.message_err_billing_error, Toast.LENGTH_SHORT).show();
    } else {
      ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_BE, "in_app_billing_error", "service unavailable", 0L);
      Log.w(TAG, "InApp billing - service unavailable");
      Toast.makeText(getActivity(), R.string.message_err_billing_unavailable, Toast.LENGTH_SHORT).show();
    }
  }

  private void openBrowser(String url) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(browserIntent);
    writeActionPerformedTimestamp(getActivity());
  }

  private void writeActionPerformedTimestamp(Context context) {
    if (context != null) {
      long timestamp = System.currentTimeMillis();
      storeTimestampOfLastShow(context, timestamp);
    } else {
      Log.w(TAG, "Context not available to store last show timestamp!");
    }
  }

  protected static void storeTimestampOfLastShow(Context context, long timestamp) {
    PreferencesUtils.storeLong(context, PREF_LAST_SUPPORT_SHOW_TIMESTAMP, timestamp);
  }

}
