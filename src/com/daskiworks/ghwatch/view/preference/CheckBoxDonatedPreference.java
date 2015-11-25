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
package com.daskiworks.ghwatch.view.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.daskiworks.ghwatch.R;
import com.daskiworks.ghwatch.backend.PreferencesUtils;

/**
 * Checkbox preference that may be used only by donators.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class CheckBoxDonatedPreference extends CheckBoxPreference {

  @SuppressLint("NewApi")
  public CheckBoxDonatedPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public CheckBoxDonatedPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public CheckBoxDonatedPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CheckBoxDonatedPreference(Context context) {
    super(context);
  }

  @Override
  protected View onCreateView(ViewGroup parent) {
    if (PreferencesUtils.readDonationTimestamp(getContext()) == null) {
      setChecked(false);
    }
    return super.onCreateView(parent);
  }

  @Override
  protected void onClick() {
    if (PreferencesUtils.readDonationTimestamp(getContext()) == null) {
      Toast.makeText(getContext(), R.string.message_donation_only, Toast.LENGTH_LONG).show();
    } else {
      super.onClick();
    }
  }

}
