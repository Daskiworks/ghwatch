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
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.daskiworks.ghwatch.backend.GHConstants;

/**
 * {@link DialogFragment} for About dialog.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class AboutDialogFragment extends DialogFragment {

  private static final String TAG = AboutDialogFragment.class.getSimpleName();

  @SuppressLint("InflateParams")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(R.string.action_about);
    View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_about, null);
    TextView appVersion = (TextView) view.findViewById(R.id.app_version);
    try {
      PackageInfo pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
      String version = pi.versionName;
      if (GHConstants.DEBUG) {
        version = version + " (" + pi.versionCode + ")";
      }
      appVersion.setText(version);
    } catch (NameNotFoundException e) {
      Log.e(TAG, e.getMessage(), e);
    }
    builder.setView(view);
    builder.setCancelable(true);
    builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {

      }
    });

    ActivityTracker.sendView(getActivity(), TAG);
    return builder.create();
  }

}
