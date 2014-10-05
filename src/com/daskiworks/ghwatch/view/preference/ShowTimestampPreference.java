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
package com.daskiworks.ghwatch.view.preference;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Preference object that shows String(with Long number) value formatted as Date and Time as Summary.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class ShowTimestampPreference extends ShowTextPreference {

  private static final String TAG = ShowTimestampPreference.class.getSimpleName();

  public ShowTimestampPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public CharSequence getSummary() {
    SharedPreferences sp = getPreferenceManager().getSharedPreferences();

    long ts = -1;
    if (ts < 0) {
      try {
        ts = sp.getLong(getKey(), -1);
      } catch (ClassCastException e) {
        try {
          String s = sp.getString(getKey(), null);
          if (s != null) {
            ts = Long.parseLong(s);
          }
        } catch (Exception e2) {
          Log.d(TAG, "Property " + getKey() + " is not convertable to long number to show it as datetime");
        }

      }
    }
    if (ts > 0) {
      Date d = new Date(ts);
      return DateFormat.getDateFormat(getContext()).format(d) + " " + DateFormat.getTimeFormat(getContext()).format(d);
    } else {
      return "";
    }
  }
}