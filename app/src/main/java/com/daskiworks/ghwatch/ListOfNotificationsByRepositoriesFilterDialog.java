/*
 * Copyright 2017 contributors as indicated by the @authors tag.
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

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.daskiworks.ghwatch.model.NotifCount;
import com.daskiworks.ghwatch.model.NotificationStreamViewData;

/**
 * Bottom Sheet Dialog Fragment used to filer notifications by repository.
 */
public class ListOfNotificationsByRepositoriesFilterDialog extends BottomSheetDialogFragment {

  private static final String TAG = ListOfNotificationsByRepositoriesFilterDialog.class.getSimpleName();

  private ListView repositoriesListViewTablet;
  private NotificationRepositoriesListAdapter repositoriesListAdapterTablet;


  //Bottom Sheet Callback
  private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
      if (newState == BottomSheetBehavior.STATE_HIDDEN) {
        dismiss();
      }

    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
    }
  };

  @Override
  public void onPause(){
    super.onPause();
    //hide dialog on screen rotation as list of repositories is missing in main activity after rotation so dialog is empty
    this.dismissAllowingStateLoss();
  }

  @Override
  public void setupDialog(Dialog dialog, int style) {
    super.setupDialog(dialog, style);
    //Get the content View
    View contentView = View.inflate(getContext(), R.layout.dialog_list_repositories_for_notifications_content, null);
    dialog.setContentView(contentView);

    //Set the coordinator layout behavior
    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
    CoordinatorLayout.Behavior behavior = params.getBehavior();

    //Set callback
    if (behavior != null && behavior instanceof BottomSheetBehavior) {
      ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
    }

    NotificationStreamViewData vd = getMainActivity().getViewData();
    if (vd != null) {
      //fill in list of repositories
      repositoriesListViewTablet = (ListView) contentView.findViewById(R.id.repositories_list);
      repositoriesListViewTablet.setOnItemClickListener(new RepositoriesListItemClickListener());
      repositoriesListAdapterTablet = new NotificationRepositoriesListAdapter(getActivity(), (getMainActivity()).getViewData().notificationStream);
      repositoriesListViewTablet.setAdapter(repositoriesListAdapterTablet);
      if (!repositoriesListAdapterTablet.setSelectionForFilter(repositoriesListViewTablet, getMainActivity().getFilterByRepository())) {
        // repo no more in data so reset filter
        getMainActivity().setFilterByRepository(null);
      }
    }
    ActivityTracker.sendView(getActivity(), TAG);
  }

  protected MainActivity getMainActivity() {
    return (MainActivity) getActivity();
  }

  private class RepositoriesListItemClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (repositoriesListViewTablet != null) {
        NotifCount nc = (NotifCount) repositoriesListAdapterTablet.getItem(position);
        if (nc != null) {
          if(!getMainActivity().setFilterByRepository(nc.title));
        }
      }
      dismissAllowingStateLoss();
    }
  }
}
