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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.daskiworks.ghwatch.backend.PreferencesUtils;
import com.daskiworks.ghwatch.backend.UnreadNotificationsService;
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.LoadingStatus;
import com.daskiworks.ghwatch.model.Notification;
import com.daskiworks.ghwatch.model.NotificationStream;
import com.daskiworks.ghwatch.model.NotificationViewData;

/**
 * {@link ListView} adapter used to show list of notifications from {@link NotificationStream}.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class NotificationListAdapter extends BaseAdapter {

  public ImageLoader imageLoader;
  private UnreadNotificationsService unreadNotificationsService;
  private LayoutInflater layoutInflater;
  private NotificationStream notificationStream;
  private Context context;

  private String filterByRepository;

  private List<Notification> filteredNotifications = null;

  public NotificationListAdapter(Context activity, final NotificationStream notificationStream, ImageLoader imageLoader,
      UnreadNotificationsService unreadNotificationsService) {
    this.context = activity;
    layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.notificationStream = notificationStream;
    this.imageLoader = imageLoader;
    this.unreadNotificationsService = unreadNotificationsService;
  }

  public void setNotificationStream(NotificationStream notificationStream) {
    this.notificationStream = notificationStream;
    filteredNotifications = null;
  }

  private List<Notification> getFilteredNotifications() {
    if (filteredNotifications == null) {
      filteredNotifications = new ArrayList<Notification>();
      if (notificationStream != null) {
        for (Notification n : notificationStream) {
          if (filterByRepository == null || filterByRepository.equals(n.getRepositoryFullName())) {
            filteredNotifications.add(n);
          }
        }
      }
    }
    return filteredNotifications;
  }

  public void removeNotificationByPosition(int position) {
    notificationStream.removeNotificationById(getFilteredNotifications().get(position).getId());
    filteredNotifications = null;
  }

  public void removeNotificationById(long id) {
    notificationStream.removeNotificationById(id);
    filteredNotifications = null;
  }

  @Override
  public int getCount() {
    return getFilteredNotifications().size();
  }

  @Override
  public Object getItem(int position) {
    List<Notification> nl = getFilteredNotifications();
    if (nl != null && position >= 0 && position < nl.size())
      return nl.get(position);
    else
      return null;
  }

  @Override
  public long getItemId(int position) {
    return getFilteredNotifications().get(position).getId();
  }

  public static void updateNotificationDetails(View listItem, Notification notification) {
    View tvStatus = listItem.findViewById(R.id.status);
    Integer statusColor = notification.getSubjectStatusColor();
    if (statusColor != null) {
      tvStatus.setBackgroundColor(statusColor);
    }
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    View listItem = null;
    if (convertView == null) {
      listItem = layoutInflater.inflate(R.layout.list_notifications, parent, false);
    } else {
      listItem = convertView;
      // listItem = layoutInflater.inflate(R.layout.list_notifications, parent, false);
    }

    // Initialize the views in the layout
    ImageView iv = (ImageView) listItem.findViewById(R.id.thumb);
    TextView tvType = (TextView) listItem.findViewById(R.id.type);
    TextView tvTitle = (TextView) listItem.findViewById(R.id.title);
    TextView tvRepoName = (TextView) listItem.findViewById(R.id.repo_name);
    tvRepoName.setSelected(true);

    // Set the views in the layout
    final Notification notification = getFilteredNotifications().get(position);
    imageLoader.displayImage(notification.getRepositoryAvatarUrl(), iv);
    tvTitle.setText(notification.getSubjectTitle());
    tvType.setText(Utils.formatNotificationTypeForView(notification.getSubjectType()) + ", " + notification.getReason());
    tvRepoName.setText(notification.getRepositoryFullName());

    if (notification.isDetailLoaded()) {
      updateNotificationDetails(listItem, notification);
    } else if (PreferencesUtils.getBoolean(context, PreferencesUtils.PREF_SERVER_DETAIL_LOADING)) {
      View tvStatus = listItem.findViewById(R.id.status);
      tvStatus.setBackgroundColor(Color.TRANSPARENT);
      // TODO #57 handle correct reuse of views
      new DetailedDataLoaderTask(listItem).execute(notification);
    }

    if (notification.getUpdatedAt() != null) {
      ((TextView) listItem.findViewById(R.id.time)).setText(Utils.formatDateIntervalFromNow(context, notification.getUpdatedAt()));
    }

    ImageButton imgButton = (ImageButton) listItem.findViewById(R.id.button_menu);
    imgButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        PopupMenu popup = new PopupMenu(context, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.list_notifications_context, popup.getMenu());
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

          @Override
          public boolean onMenuItemClick(MenuItem item) {
            if (onItemMenuClickedListener != null)
              return onItemMenuClickedListener.onMenuItemClick(notification, item);
            return false;
          }
        });

      }
    });

    return listItem;
  }

  private final class DetailedDataLoaderTask extends AsyncTask<Notification, String, NotificationViewData> {

    View listItem;

    public DetailedDataLoaderTask(View listItem) {
      super();
      this.listItem = listItem;
    }

    @Override
    protected NotificationViewData doInBackground(Notification... params) {
      return unreadNotificationsService.getNotificationDetailForView(params[0]);
    }

    @Override
    protected void onPostExecute(NotificationViewData result) {
      if (result.loadingStatus == LoadingStatus.OK && result.notification != null)
        updateNotificationDetails(listItem, result.notification);
      super.onPostExecute(result);
    }

  }

  private OnItemMenuClickedListener onItemMenuClickedListener;

  /**
   * Listener for notification menu item clicks.
   */
  public static interface OnItemMenuClickedListener {
    public boolean onMenuItemClick(Notification notification, MenuItem item);
  }

  public void setOnItemMenuClickedListener(OnItemMenuClickedListener onItemMenuClickedListener) {
    this.onItemMenuClickedListener = onItemMenuClickedListener;
  }

  /**
   * Set filter by repository into this data source. Null menas no filter. {@link #notifyDataSetChanged()} is called inside.
   * 
   * @param filterByRepository
   */
  public void setFilterByRepository(String filterByRepository) {
    this.filterByRepository = filterByRepository;
    filteredNotifications = null;
    notifyDataSetChanged();
  }

  @Override
  public void notifyDataSetChanged() {
    filteredNotifications = null;
    super.notifyDataSetChanged();
  }

}
