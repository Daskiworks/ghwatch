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

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Preference object that shows String preference text value as Summary.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class ShowTextPreference extends Preference {

  public ShowTextPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setShouldDisableView(false);
    setSelectable(true);
    setEnabled(false);
    setPersistent(false);
  }

  @Override
  public CharSequence getSummary() {
    return getPreferenceManager().getSharedPreferences().getString(getKey(), "");
  }
}