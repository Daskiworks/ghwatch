/*
 * Copyright 2015 contributors as indicated by the @authors tag.
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

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.daskiworks.ghwatch.ActivityTracker;
import com.daskiworks.ghwatch.IabHelper;
import com.daskiworks.ghwatch.R;
import com.daskiworks.ghwatch.SupportAppDevelopmentDialogFragment;

/**
 * Service used to process donations.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class DonationService {

  public static final String INAPP_CODE_DONATION_1 = "donation_1";
  public static final String INAPP_CODE_DONATION_2 = "donation_2";

  private static final int BUY_REQUEST_CODE = 1016;

  private static String buyDeveloperPayload = System.currentTimeMillis() + "";

  private static final String TAG = DonationService.class.getSimpleName();

  /**
   * @param sku id of product we want to buy
   * @return true if buy flow started
   */
  public static boolean buyItem(Activity context, SupportAppDevelopmentDialogFragment dialog, IInAppBillingService mService, String sku) {
    ActivityTracker.sendEvent(context, ActivityTracker.CAT_UI, "app_support_donate_click", sku, 0L);
    if (mService != null) {
      try {
        Bundle buyIntentBundle = mService.getBuyIntent(3, context.getPackageName(), sku, IabHelper.ITEM_TYPE_INAPP, buyDeveloperPayload);
        int rc = buyIntentBundle.getInt(IabHelper.RESPONSE_CODE);
        if (rc == IabHelper.BILLING_RESPONSE_RESULT_OK) {
          PendingIntent pendingIntent = buyIntentBundle.getParcelable(IabHelper.RESPONSE_BUY_INTENT);
          context.startIntentSenderForResult(pendingIntent.getIntentSender(), BUY_REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
              Integer.valueOf(0));
          return true;
        } else if (rc == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
          DonationService.storeDonationExists(context);
          refreshSupportAppDevelopmentDialogDonated(dialog);
          Toast.makeText(context, R.string.message_ok_billing_previous, Toast.LENGTH_LONG).show();
          return true;
        } else {
          ActivityTracker.sendEvent(context, ActivityTracker.CAT_BE, "in_app_billing_error", "GetBuyIntent error " + rc, 0L);
          Log.e(TAG, "InApp billing - GetBuyIntent response error with code " + rc);
        }
      } catch (Exception e) {
        ActivityTracker.sendEvent(context, ActivityTracker.CAT_BE, "in_app_billing_error", e.getMessage(), 0L);
        Log.e(TAG, "InApp billing - exception " + e.getMessage());
      }
      Toast.makeText(context, R.string.message_err_billing_error, Toast.LENGTH_SHORT).show();
    } else {
      ActivityTracker.sendEvent(context, ActivityTracker.CAT_BE, "in_app_billing_error", "service unavailable", 0L);
      Log.w(TAG, "InApp billing - service unavailable");
      Toast.makeText(context, R.string.message_err_billing_unavailable, Toast.LENGTH_SHORT).show();
    }
    return false;
  }

  public static boolean processBillingResult(Context context, Fragment fragment, int requestCode, int resultCode, Intent data) {
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
                storeDonationExists(context);
                refreshSupportAppDevelopmentDialogDonated(fragment);
                Toast.makeText(context, R.string.message_ok_billing, Toast.LENGTH_LONG).show();
                Log.i(TAG, "InApp billing success. orderId=" + orderId);
                return true;
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
      Toast.makeText(context, R.string.message_err_billing_error, Toast.LENGTH_LONG).show();
      ActivityTracker.sendEvent(context, ActivityTracker.CAT_BE, "in_app_billing_error", errorMessage, 0L);
      return true;
    }
    return false;
  }

  public static boolean restoreDonation(Activity activity, SupportAppDevelopmentDialogFragment dialog, IInAppBillingService mService) {
    ActivityTracker.sendEvent(activity, ActivityTracker.CAT_UI, "app_support_donate_restore_click", null, 0L);
    if (mService != null) {
      try {
        Bundle ownedItems = mService.getPurchases(3, activity.getPackageName(), IabHelper.ITEM_TYPE_INAPP, null);
        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
          ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
          Log.d(TAG, "InApp billing - owned skus: " + ownedSkus);
          if (ownedSkus != null && (ownedSkus.contains(INAPP_CODE_DONATION_1) || ownedSkus.contains(INAPP_CODE_DONATION_2))) {
            DonationService.storeDonationExists(activity);
            refreshSupportAppDevelopmentDialogDonated(dialog);
            Toast.makeText(activity, R.string.message_ok_billing, Toast.LENGTH_LONG).show();
            ActivityTracker.sendEvent(activity, ActivityTracker.CAT_UI, "app_support_donate_restored", null, 0L);
            return true;
          } else {
            Toast.makeText(activity, R.string.message_err_billing_check_not_found, Toast.LENGTH_LONG).show();
            return false;
          }
        } else {
          ActivityTracker.sendEvent(activity, ActivityTracker.CAT_BE, "message_err_billing_check_error", "RESPONSE_CODE " + response, 0L);
          Log.e(TAG, "InApp billing - RESPONSE_CODE " + response);
        }
      } catch (Exception e) {
        ActivityTracker.sendEvent(activity, ActivityTracker.CAT_BE, "message_err_billing_check_error", e.getMessage(), 0L);
        Log.e(TAG, "InApp billing - exception " + e.getMessage());
      }
      Toast.makeText(activity, R.string.message_err_billing_check_error, Toast.LENGTH_SHORT).show();
    } else {
      ActivityTracker.sendEvent(activity, ActivityTracker.CAT_BE, "message_err_billing_check_error", "service unavailable", 0L);
      Log.w(TAG, "InApp billing - service unavailable");
      Toast.makeText(activity, R.string.message_err_billing_unavailable, Toast.LENGTH_SHORT).show();
    }
    return false;
  }

  public static void consumeAllPurchases(Activity activity, SupportAppDevelopmentDialogFragment dialog, IInAppBillingService mService) {
    try {
      Bundle ownedItems = mService.getPurchases(3, activity.getPackageName(), IabHelper.ITEM_TYPE_INAPP, null);
      int response = ownedItems.getInt("RESPONSE_CODE");
      if (response == 0) {
        ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
        for (String purchaseData : purchaseDataList) {
          JSONObject jo = new JSONObject(purchaseData);
          mService.consumePurchase(3, activity.getPackageName(), jo.getString("purchaseToken"));
        }
      }
    } catch (Exception e) {
      ActivityTracker.sendEvent(activity, ActivityTracker.CAT_BE, "message_err_billing_check_error", e.getMessage(), 0L);
      Log.e(TAG, "InApp billing - exception " + e.getMessage());
    }
  }

  protected static void refreshSupportAppDevelopmentDialogDonated(final Fragment f) {
    if (f != null && f instanceof SupportAppDevelopmentDialogFragment && f.getActivity() != null) {
      f.getActivity().runOnUiThread(new Runnable() {
        public void run() {
          ((SupportAppDevelopmentDialogFragment) f).showDonatedInfo();
        }
      });
    }
  }

  public static void storeDonationExists(Context context) {
    PreferencesUtils.storeDonationTimestamp(context, System.currentTimeMillis());
    PreferencesUtils.storeBoolean(context, PreferencesUtils.PREF_SERVER_DETAIL_LOADING, true);
  }

}
