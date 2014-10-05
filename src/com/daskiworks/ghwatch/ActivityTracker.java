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

import android.content.Context;

import com.daskiworks.ghwatch.backend.GHConstants;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

/**
 * Activity tracker component.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class ActivityTracker {

  private static Tracker mGaTracker;
  private static GoogleAnalytics mGaInstance;

  private static Tracker getTracker(Context context) {
    if (mGaTracker == null) {
      mGaInstance = GoogleAnalytics.getInstance(context);
      mGaTracker = mGaInstance.getTracker("UA-37771622-3");
      GAServiceManager.getInstance().setDispatchPeriod(30);
    }
    return mGaTracker;
  }

  /**
   * Track screen view.
   * 
   * @param context
   * @param appScreen
   */
  public static void sendView(Context context, String appScreen) {
    if (!GHConstants.DEBUG)
      getTracker(context).sendView(appScreen);
  }

  /** Event category - notifications related events */
  public static final String CAT_NOTIF = "notif";
  /** Event category - UI action related events (clicks on action element like buttons, links, etc) */
  public static final String CAT_UI = "ui";
  /** Event category - backend activities (ie. backedn systems calls) related events */
  public static final String CAT_BE = "be";
  /** Event category - preferences related events (preference changed) */
  public static final String CAT_PREF = "pref";

  /**
   * Track event.
   * 
   * @param context
   * @param category of event, see CAT_XX constants
   * @param action action of event
   * @param label of event
   * @param value of event
   */
  public static void sendEvent(Context context, String category, String action, String label, Long value) {
    if (!GHConstants.DEBUG)
      getTracker(context).sendEvent(category, action, label, value);
  }

}
