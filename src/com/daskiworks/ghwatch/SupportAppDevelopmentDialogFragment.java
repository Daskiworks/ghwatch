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

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
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

  private static final int BUY_REQUEST_CODE = 1010;
  private String buyDeveloperPayload = System.currentTimeMillis() + "";

  public static final String PREF_LAST_SUPPORT_SHOW_TIMESTAMP = "LAST_SUPPORT_SHOW_TIMESTAMP";

  private static final String INAPP_CODE_DONATION_1 = "donation_1";
  private static final String INAPP_CODE_DONATION_2 = "donation_2";

  private static final String TAG = SupportAppDevelopmentDialogFragment.class.getSimpleName();
  private static final long AUTO_SHOW_PERIOD = 180L * Utils.MILLIS_DAY;
  private static final long AUTO_SHOW_FIRST = 10L * Utils.MILLIS_DAY;

  protected View view;

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
    view = getActivity().getLayoutInflater().inflate(R.layout.dialog_support_app_development, null);
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

    if (PreferencesUtils.readDonationTimestamp(getActivity()) != null) {
      showDonatedInfo(view);
    } else {

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

      view.findViewById(R.id.button_donate_restore).setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
          restoreDonation();
        }
      });
    }

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

  protected void showDonatedInfo(View view) {
    view.findViewById(R.id.button_donate_1).setVisibility(View.GONE);
    view.findViewById(R.id.button_donate_2).setVisibility(View.GONE);
    view.findViewById(R.id.button_donate_restore).setVisibility(View.GONE);
    view.findViewById(R.id.text_donated).setVisibility(View.VISIBLE);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (BUY_REQUEST_CODE == requestCode) {
      int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
      String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
      // String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
      // TODO billing validate signature?

      String errorMessage;
      if (resultCode == Activity.RESULT_OK) {
        if (responseCode == 0) {
          try {
            JSONObject jo = new JSONObject(purchaseData);
            String developerPayload = jo.getString("developerPayload");
            if (buyDeveloperPayload.equals(developerPayload)) {
              String sku = jo.getString("productId");
              if (INAPP_CODE_DONATION_1.equals(sku) || INAPP_CODE_DONATION_2.equals(sku)) {
                String orderId = jo.getString("orderId");
                storeDonationExists();
                Toast.makeText(getActivity(), R.string.message_ok_billing, Toast.LENGTH_LONG).show();
                Log.i(TAG, "InApp billing success. orderId=" + orderId);
                return;
              } else {
                errorMessage = "invalid productId " + sku;
              }
            } else {
              errorMessage = "invalid developer payload in response";
            }
          } catch (JSONException e) {
            errorMessage = "billing response data format error: " + e.getMessage();
          }
        } else {
          errorMessage = "billing RESPONSE_CODE is not OK: " + requestCode;
        }
      } else {
        errorMessage = "billing result code is not ok: " + requestCode;
      }
      Log.w(TAG, "InApp billing error - " + errorMessage);
      Toast.makeText(getActivity(), R.string.message_err_billing_error, Toast.LENGTH_LONG).show();
      ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_BE, "in_app_billing_error", errorMessage, 0L);
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  protected void storeDonationExists() {
    writeActionPerformedTimestamp(getActivity());
    PreferencesUtils.storeDonationTimestamp(getActivity(), System.currentTimeMillis());
    PreferencesUtils.storeBoolean(getActivity(), PreferencesUtils.PREF_SERVER_DETAIL_LOADING, true);
    showDonatedInfo(view);
  }

  /**
   * @param sku id of product we want to buy
   * @return true if buy flow started
   */
  private void buyItem(String sku) {
    ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "app_support_donate_click", sku, 0L);
    if (mService != null) {
      try {
        Bundle buyIntentBundle = mService.getBuyIntent(3, getActivity().getPackageName(), sku, IabHelper.ITEM_TYPE_INAPP, buyDeveloperPayload);
        int rc = buyIntentBundle.getInt(IabHelper.RESPONSE_CODE);
        if (rc == IabHelper.BILLING_RESPONSE_RESULT_OK) {
          PendingIntent pendingIntent = buyIntentBundle.getParcelable(IabHelper.RESPONSE_BUY_INTENT);
          getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(), BUY_REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
              Integer.valueOf(0));
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

  private void restoreDonation() {
    ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "app_support_donate_restore_click", null, 0L);
    if (mService != null) {
      try {
        Bundle ownedItems = mService.getPurchases(3, getActivity().getPackageName(), IabHelper.ITEM_TYPE_INAPP, null);
        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
          ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
          Log.d(TAG, "InApp billing - owned skus: " + ownedSkus);
          if (ownedSkus != null && (ownedSkus.contains(INAPP_CODE_DONATION_1) || ownedSkus.contains(INAPP_CODE_DONATION_2))) {
            storeDonationExists();
            Toast.makeText(getActivity(), R.string.message_ok_billing, Toast.LENGTH_LONG).show();
            ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "app_support_donate_restored", null, 0L);
            return;
          } else {
            Toast.makeText(getActivity(), R.string.message_err_billing_check_not_found, Toast.LENGTH_LONG).show();
            return;
          }
        } else {
          ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_BE, "message_err_billing_check_error", "RESPONSE_CODE " + response, 0L);
          Log.e(TAG, "InApp billing - RESPONSE_CODE " + response);
        }
      } catch (Exception e) {
        ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_BE, "message_err_billing_check_error", e.getMessage(), 0L);
        Log.e(TAG, "InApp billing - exception " + e.getMessage());
      }
      Toast.makeText(getActivity(), R.string.message_err_billing_check_error, Toast.LENGTH_SHORT).show();
    } else {
      ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_BE, "message_err_billing_check_error", "service unavailable", 0L);
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
