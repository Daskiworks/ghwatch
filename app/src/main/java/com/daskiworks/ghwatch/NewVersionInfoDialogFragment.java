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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.daskiworks.ghwatch.backend.PreferencesUtils;

/**
 * {@link DialogFragment} for New version info dialog.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class NewVersionInfoDialogFragment extends DialogFragment {

  public static final String PREF_LAST_VERSION_INFO_SHOW_TAG = "LAST_VERSION_INFO_SHOW_TAG";

  private static final String TAG = NewVersionInfoDialogFragment.class.getSimpleName();

  private static final String VERSION_VALUE = "1.27";

  protected View view;

  /**
   * Check if this dialog should be shown now.
   * 
   * @param activity
   * @return true if should be shown
   */
  public static boolean isShowScheduled(MainActivity activity) {
    String lastShowedVersion = PreferencesUtils.getString(activity, NewVersionInfoDialogFragment.PREF_LAST_VERSION_INFO_SHOW_TAG, null);
    if(lastShowedVersion == null){
      //do not show for first version user has, but make sure it is shown for next version
     storeShowOccurence(activity);
     return false;
    } else {
      return !VERSION_VALUE.equals(lastShowedVersion);
    }
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    storeShowOccurence(getActivity());
    super.onDismiss(dialog);
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    storeShowOccurence(getActivity());
    super.onCancel(dialog);
  }

  @SuppressLint("InflateParams")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(R.string.dialog_nv_title);
    view = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_version, null);
    builder.setView(view);

    builder.setCancelable(true);
    builder.setPositiveButton(R.string.button_close, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        storeShowOccurence(getActivity());
      }
    });
    builder.setNegativeButton(R.string.dialog_sad_title, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "new_version_dialog_app_support", null, 0L);
        DialogFragment d = new SupportAppDevelopmentDialogFragment();
        d.show(NewVersionInfoDialogFragment.this.getFragmentManager(), ActivityBase.FRAGMENT_DIALOG);
      }
    });

    ActivityTracker.sendView(getActivity(), TAG);
    return builder.create();
  }

  protected void openBrowser(String url) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(browserIntent);
  }

  protected static void storeShowOccurence(Activity activity) {
    if (activity != null) {
      PreferencesUtils.storeString(activity, PREF_LAST_VERSION_INFO_SHOW_TAG, VERSION_VALUE);
      (new BackupManager(activity)).dataChanged();
    }
  }

}
