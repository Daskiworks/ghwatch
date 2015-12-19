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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.daskiworks.ghwatch.model.NotifCount;
import com.daskiworks.ghwatch.model.NotificationStream;

/**
 * {@link ListView} adapter used to show list of repositories we have notifications for in {@link NotificationStream}.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class NotificationRepositoriesListAdapter extends BaseAdapter {

  private LayoutInflater layoutInflater;
  private NotificationStream notificationStream;
  @SuppressWarnings("unused")
  private Context context;

  public NotificationRepositoriesListAdapter(Context activity, final NotificationStream notificationStream) {
    this.context = activity;
    layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.notificationStream = notificationStream;
  }

  @Override
  public int getCount() {
    return notificationStream.getRepositoriesInfo().size();
  }

  @Override
  public Object getItem(int position) {
    return notificationStream.getRepositoriesInfo().get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  public void setNotificationStream(NotificationStream notificationStream) {
    this.notificationStream = notificationStream;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    View listItem = null;
    if (convertView == null) {
      listItem = layoutInflater.inflate(R.layout.list_repositories_for_notifications, parent, false);
    } else {
      listItem = convertView;
    }

    // Initialize the views in the layout
    TextView tvRepoName = (TextView) listItem.findViewById(R.id.repo_name);
    tvRepoName.setSelected(true);
    TextView tvCount = (TextView) listItem.findViewById(R.id.count);

    NotifCount notifCount = notificationStream.getRepositoriesInfo().get(position);
    tvRepoName.setText(notifCount.title);
    tvCount.setText(notifCount.count + "");
    return listItem;
  }

  /**
   * Set selection into listView based on repository filter value.
   * 
   * @param repositoriesListView to set selection into
   * @param filterByRepository value of filter to set selection for
   * @return true if filterByRepository is found in data (we have notification for it)
   */
  public boolean setSelectionForFilter(ListView repositoriesListView, String filterByRepository) {
    if (filterByRepository != null) {
      int i = 0;
      for (NotifCount nc : notificationStream.getRepositoriesInfo()) {
        if (filterByRepository.equals(nc.title)) {
          repositoriesListView.setItemChecked(i, true);
          return true;
        }
        i++;
      }
    }
    repositoriesListView.setItemChecked(-1, true);
    return false;
  }

}
