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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import com.daskiworks.ghwatch.alarm.AlarmBroadcastReceiver;
import com.daskiworks.ghwatch.backend.PreferencesUtils;
import com.daskiworks.ghwatch.backend.UnreadNotificationsService;
import com.daskiworks.ghwatch.backend.ViewDataReloadStrategy;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.NotificationStreamViewData;

/**
 * Number of unread notifications widget.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class UnreadAppWidgetProvider extends AppWidgetProvider {

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    new DataLoaderTask(context, appWidgetManager, appWidgetIds).execute();
  }

  @Override
  public void onEnabled(Context context) {
    PreferencesUtils.storeBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_EXISTS, true);
    AlarmBroadcastReceiver.startServerPoolingIfEnabled(context);
    ActivityTracker.sendEvent(context, ActivityTracker.CAT_UI, "widget_enabled", "unread_count", null);
  }

  @Override
  public void onDisabled(Context context) {
    PreferencesUtils.storeBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_EXISTS, false);
    AlarmBroadcastReceiver.stopServerPoolingIfDisabled(context);
    ActivityTracker.sendEvent(context, ActivityTracker.CAT_UI, "widget_disabled", "unread_count", null);
  }

  private final class DataLoaderTask extends AsyncTask<String, String, NotificationStreamViewData> {

    Context context;
    AppWidgetManager appWidgetManager;
    int[] appWidgetIds;

    public DataLoaderTask(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
      super();
      this.context = context;
      this.appWidgetManager = appWidgetManager;
      this.appWidgetIds = appWidgetIds;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected NotificationStreamViewData doInBackground(String... args) {
      UnreadNotificationsService unreadNotificationsService = new UnreadNotificationsService(context);
      return unreadNotificationsService.getNotificationStreamForView(ViewDataReloadStrategy.NEVER);
    }

    @Override
    protected void onPostExecute(final NotificationStreamViewData viewData) {
      if (isCancelled())
        return;

      String val = "-";
      int valInt = 0;

      if (viewData != null && viewData.loadingStatus == LoadingStatus.OK && viewData.notificationStream != null) {
        valInt = viewData.notificationStream.size();
        val = valInt + "";
      }

      boolean highlight = PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_WIDGET_UNREAD_HIGHLIGHT, false);

      for (int wid : appWidgetIds) {

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_unread);
        views.setTextViewText(R.id.count, val);
        if (highlight && valInt > 0)
          views.setTextColor(R.id.count, 0xffF57D22);
        else
          views.setTextColor(R.id.count, 0xffffffff);

        views.setOnClickPendingIntent(R.id.widget, pendingIntent);

        appWidgetManager.updateAppWidget(wid, views);
      }
    }
  }

}
