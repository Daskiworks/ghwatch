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
import android.os.AsyncTask;
import android.widget.Toast;

import com.daskiworks.ghwatch.R;
import com.daskiworks.ghwatch.backend.UnreadNotificationsService;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.LoadingStatus;

/**
 * Broadcast receiver called from Android notification to mark one notification as read.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class MarkNotifiationAsReadReceiver extends BroadcastReceiver {

  public static final String INTENT_EXTRA_KEY_ID = "com.daskiworks.ghwatch.ID";

  @Override
  public void onReceive(final Context context, Intent intent) {
    UnreadNotificationsService unreadNotificationsService = new UnreadNotificationsService(context);
    unreadNotificationsService.markAndroidNotificationsRead();
    long id = intent.getLongExtra(INTENT_EXTRA_KEY_ID, -1);
    if (id > -1) {
      new MarkNotificationAsReadTask(context, unreadNotificationsService).execute(id);
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

}
