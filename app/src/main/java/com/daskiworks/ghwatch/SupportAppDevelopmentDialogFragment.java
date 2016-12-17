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

import com.android.vending.billing.IInAppBillingService;
import com.daskiworks.ghwatch.backend.DonationService;
import com.daskiworks.ghwatch.backend.PreferencesUtils;

/**
 * {@link DialogFragment} for Support App Development dialog.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class SupportAppDevelopmentDialogFragment extends DialogFragment {

  public static final String PREF_LAST_SUPPORT_SHOW_TIMESTAMP = "LAST_SUPPORT_SHOW_TIMESTAMP";

  private static final String TAG = SupportAppDevelopmentDialogFragment.class.getSimpleName();
  private static final long AUTO_SHOW_PERIOD = 60L * Utils.MILLIS_DAY;
  private static final long AUTO_SHOW_FIRST = 10L * Utils.MILLIS_DAY;

  protected View view;

  /**
   * Check if this dialog should be shown now.
   * 
   * @param activity
   * @return true if dialog should be shown now.
   */
  public static boolean isAutoShowScheduled(MainActivity activity) {

    if (PreferencesUtils.readDonationTimestamp(activity) != null)
      return false;

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
      showDonatedInfo();
    } else {

      view.findViewById(R.id.button_donate_1).setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
          if (DonationService.buyItem(getActivity(), SupportAppDevelopmentDialogFragment.this, mService, DonationService.INAPP_CODE_DONATION_1)) {
            writeActionPerformedTimestamp(getActivity());
          }
        }
      });

      view.findViewById(R.id.button_donate_2).setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
          if (DonationService.buyItem(getActivity(), SupportAppDevelopmentDialogFragment.this, mService, DonationService.INAPP_CODE_DONATION_2)) {
            writeActionPerformedTimestamp(getActivity());
          }
        }
      });

      view.findViewById(R.id.button_donate_restore).setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
          if (DonationService.restoreDonation(getActivity(), SupportAppDevelopmentDialogFragment.this, mService)) {
            writeActionPerformedTimestamp(getActivity());
          }
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

  public void showDonatedInfo() {
    if (view != null) {
      view.findViewById(R.id.button_donate_1).setVisibility(View.GONE);
      view.findViewById(R.id.button_donate_2).setVisibility(View.GONE);
      view.findViewById(R.id.button_donate_restore).setVisibility(View.GONE);
      view.findViewById(R.id.text_donated).setVisibility(View.VISIBLE);
    }
  }

  private void openBrowser(String url) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(browserIntent);
    writeActionPerformedTimestamp(getActivity());
  }

  public static void writeActionPerformedTimestamp(Context context) {
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
