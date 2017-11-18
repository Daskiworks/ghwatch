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
package com.daskiworks.ghwatch.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.daskiworks.ghwatch.ActivityTracker;
import com.daskiworks.ghwatch.R;
import com.daskiworks.ghwatch.backend.UnreadNotificationsService;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.StringViewData;

/**
 * Broadcast receiver called from Android notification to perform background actions like:
 * <ul>
 * <li>mark one notification as read
 * <li>mute thread for one notification
 * <li>open notification detail</li>
 * </ul>
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class AndroidNotifiationActionsReceiver extends BroadcastReceiver {

  public static final String INTENT_EXTRA_KEY_ACTION = "com.daskiworks.ghwatch.ACTION";
  public static final String INTENT_EXTRA_VALUE_ACTION_MARKASREAD = "MARKASREAD";
  public static final String INTENT_EXTRA_VALUE_ACTION_MUTE = "MUTE";
  public static final String INTENT_EXTRA_VALUE_ACTION_SHOW = "SHOW";
  public static final String INTENT_EXTRA_VALUE_ACTION_DELETED = "DELETED";

  public static final String INTENT_EXTRA_KEY_NOTIFICATION_ID = "com.daskiworks.ghwatch.NOTIFICATION_ID";
  public static final String INTENT_EXTRA_KEY_IS_BUNDLED = "com.daskiworks.ghwatch.BUNDLED";

  @Override
  public void onReceive(Context context, Intent intent) {
    context = context.getApplicationContext();
    UnreadNotificationsService unreadNotificationsService = new UnreadNotificationsService(context);

    long id = intent.getLongExtra(INTENT_EXTRA_KEY_NOTIFICATION_ID, -1);
    if (id > -1) {
      switch (intent.getStringExtra(INTENT_EXTRA_KEY_ACTION)) {
        case INTENT_EXTRA_VALUE_ACTION_MARKASREAD:
          new MarkNotificationAsReadTask(context, unreadNotificationsService).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
          ActivityTracker.sendEvent(context, ActivityTracker.CAT_UI, "notification_mark_read_fromandroidnotification", "", 0L);
          break;
        case INTENT_EXTRA_VALUE_ACTION_MUTE:
          new MuteNotificationThreadTask(context, unreadNotificationsService).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
          ActivityTracker.sendEvent(context, ActivityTracker.CAT_UI, "notification_mute_thread_fromandroidnotification", "", 0L);
          break;
        case INTENT_EXTRA_VALUE_ACTION_SHOW:
          new ShowNotificationTask(context, unreadNotificationsService).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
          ActivityTracker.sendEvent(context, ActivityTracker.CAT_UI, "notification_show_fromandroidnotification", "", 0L);
          break;
        case INTENT_EXTRA_VALUE_ACTION_DELETED:
          //nothing to do, it is here only to dismiss summary notification in case of bundled notifications
          break;
      }

      if (intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_BUNDLED, false)) {
        unreadNotificationsService.markAndroidNotificationBundledDetailRead((int) id);
      } else {
        unreadNotificationsService.markAndroidNotificationsRead();
      }
    } else {
      unreadNotificationsService.markAndroidNotificationsRead();
    }
  }

  private final class MarkNotificationAsReadTask extends AsyncTask<Long, String, BaseViewData> {
    UnreadNotificationsService unreadNotificationsService;
    Context context;

    MarkNotificationAsReadTask(Context context, UnreadNotificationsService unreadNotificationsService) {
      this.unreadNotificationsService = unreadNotificationsService;
      this.context = context;
    }

    @Override
    protected BaseViewData doInBackground(Long... params) {
      return unreadNotificationsService.markNotificationAsRead(params[0]);
    }

    @Override
    protected void onPostExecute(final BaseViewData viewData) {
      if (isCancelled() || viewData == null)
        return;
      if (viewData.loadingStatus == LoadingStatus.OK) {
        Toast.makeText(context, R.string.message_notification_marked_read, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private final class MuteNotificationThreadTask extends AsyncTask<Long, String, BaseViewData> {
    UnreadNotificationsService unreadNotificationsService;
    Context context;

    MuteNotificationThreadTask(Context context, UnreadNotificationsService unreadNotificationsService) {
      this.unreadNotificationsService = unreadNotificationsService;
      this.context = context;
    }

    @Override
    protected BaseViewData doInBackground(Long... params) {
      return unreadNotificationsService.muteNotificationThread(params[0]);
    }

    @Override
    protected void onPostExecute(final BaseViewData viewData) {
      if (isCancelled() || viewData == null)
        return;
      if (viewData.loadingStatus == LoadingStatus.OK) {
        Toast.makeText(context, R.string.message_notification_thread_muted, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private final class ShowNotificationTask extends AsyncTask<Long, String, StringViewData> {
    UnreadNotificationsService unreadNotificationsService;
    Context context;

    ShowNotificationTask(Context context, UnreadNotificationsService unreadNotificationsService) {
      this.unreadNotificationsService = unreadNotificationsService;
      this.context = context;
    }

    @Override
    protected StringViewData doInBackground(Long... params) {
      return unreadNotificationsService.getNotificationViewUrl(params[0]);
    }

    @Override
    protected void onPostExecute(final StringViewData viewData) {
      if (isCancelled() || viewData == null || viewData.data == null)
        return;
      if (viewData.loadingStatus == LoadingStatus.OK) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(viewData.data));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(browserIntent);
      }
    }
  }


}
