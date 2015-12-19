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
import com.daskiworks.ghwatch.image.ImageLoader;
import com.daskiworks.ghwatch.model.Repository;
import com.daskiworks.ghwatch.model.WatchedRepositories;

/**
 * {@link ListView} adapter used to show list of watched repositories from {@link WatchedRepositories}.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class WatchedRepositoryListAdapter extends BaseAdapter {

  public ImageLoader imageLoader;
  private LayoutInflater layoutInflater;
  private WatchedRepositories repositoriesData;
  private Context context;

  private List<Repository> filteredRepositories = null;

  public WatchedRepositoryListAdapter(Context activity, final WatchedRepositories repositoriesData, ImageLoader imageLoader) {
    this.context = activity;
    layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.repositoriesData = repositoriesData;
    this.imageLoader = imageLoader;
  }

  public void setNotificationStream(WatchedRepositories repositoriesData) {
    this.repositoriesData = repositoriesData;
    filteredRepositories = null;
  }

  private List<Repository> getFilteredRepositories() {
    if (filteredRepositories == null) {
      filteredRepositories = new ArrayList<Repository>();
      if (repositoriesData != null) {
        for (Repository n : repositoriesData) {
          // filtering can be added here
          filteredRepositories.add(n);

        }
      }
    }
    return filteredRepositories;
  }

  public void removeRepositoryByPosition(int position) {
    repositoriesData.removeRepositoryById(getFilteredRepositories().get(position).getId());
    filteredRepositories = null;
  }

  public void removeRepositoryById(long id) {
    repositoriesData.removeRepositoryById(id);
    filteredRepositories = null;
  }

  @Override
  public int getCount() {
    return getFilteredRepositories().size();
  }

  @Override
  public Object getItem(int position) {
    return getFilteredRepositories().get(position);
  }

  @Override
  public long getItemId(int position) {
    return getFilteredRepositories().get(position).getId();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    View listItem = null;
    if (convertView == null) {
      listItem = layoutInflater.inflate(R.layout.list_watched_repos, parent, false);
    } else {
      listItem = layoutInflater.inflate(R.layout.list_watched_repos, parent, false);
    }

    // Initialize the views in the layout
    ImageView iv = (ImageView) listItem.findViewById(R.id.thumb);
    TextView tvRepoName = (TextView) listItem.findViewById(R.id.repo_name);
    TextView tvNotifFilter = (TextView) listItem.findViewById(R.id.notif_filter);

    // Set the views in the layout
    final Repository repository = getFilteredRepositories().get(position);
    imageLoader.displayImage(repository.getRepositoryAvatarUrl(), iv);
    tvRepoName.setText(repository.getRepositoryFullName());
    tvRepoName.setSelected(true);

    StringBuilder sb = new StringBuilder(context.getString(R.string.pref_notifyFilter));
    sb.append(": ");
    String[] sa = context.getResources().getStringArray(R.array.pref_notifyFilterFull_entries);
    int p = Integer.parseInt(PreferencesUtils.getNotificationFilterForRepository(context, repository.getRepositoryFullName(), false));
    sb.append(sa[p]);
    if (p == 0) {
      sb.append(" (");
      sb.append(sa[Integer.parseInt(PreferencesUtils.getNotificationFilter(context))]);
      sb.append(")");
    }
    tvNotifFilter.setText(sb);

    View.OnClickListener cl = new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        handleMenuItemClicked(repository, R.id.action_view);
      }
    };
    listItem.setOnClickListener(cl);

    ImageButton imgButton = (ImageButton) listItem.findViewById(R.id.button_menu);
    imgButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        PopupMenu popup = new PopupMenu(context, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.list_watched_repos_context, popup.getMenu());
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

          @Override
          public boolean onMenuItemClick(MenuItem item) {
            return handleMenuItemClicked(repository, item.getItemId());
          }
        });

      }
    });

    return listItem;
  }

  private boolean handleMenuItemClicked(Repository repository, int menuItemId) {
    if (onItemMenuClickedListener != null)
      return onItemMenuClickedListener.onMenuItemClick(repository, menuItemId);
    return false;
  }

  private OnItemMenuClickedListener onItemMenuClickedListener;

  /**
   * Listener for notification menu item clicks.
   */
  public static interface OnItemMenuClickedListener {
    public boolean onMenuItemClick(Repository repository, int menuItemId);
  }

  public void setOnItemMenuClickedListener(OnItemMenuClickedListener onItemMenuClickedListener) {
    this.onItemMenuClickedListener = onItemMenuClickedListener;
  }

  @Override
  public void notifyDataSetChanged() {
    filteredRepositories = null;
    super.notifyDataSetChanged();
  }

}
