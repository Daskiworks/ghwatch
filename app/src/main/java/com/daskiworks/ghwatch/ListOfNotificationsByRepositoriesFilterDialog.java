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
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.daskiworks.ghwatch.model.NotifCount;
import com.daskiworks.ghwatch.model.NotificationStreamViewData;

/**
 * Bottom Sheet Dialog Fragment used to filer notifications by repository in {@link MainActivity}.
 */
public class ListOfNotificationsByRepositoriesFilterDialog extends BottomSheetDialogFragment {

  private static final String TAG = ListOfNotificationsByRepositoriesFilterDialog.class.getSimpleName();

  private ListView repositoriesListView;
  private NotificationRepositoriesListAdapter repositoriesListAdapter;


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
  public void onPause() {
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

    //Setup the coordinator layout behavior
    BottomSheetBehavior behavior = BottomSheetBehavior.from((View) contentView.getParent());
    if (behavior != null) {
      behavior.setBottomSheetCallback(mBottomSheetBehaviorCallback);
    }

    //Workaround to open bottom sheet at full height even in landscape mode
    contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        FrameLayout bottomSheet = (FrameLayout) dialog.findViewById(android.support.design.R.id.design_bottom_sheet);
        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setPeekHeight(0);
      }
    });

    //Show list of repositories we have notifications for
    String currentFilter = getMainActivity().getFilterByRepository();
    NotificationStreamViewData vd = getMainActivity().getViewData();
    if (vd != null) {
      //fill in list of repositories
      repositoriesListView = (ListView) contentView.findViewById(R.id.repositories_list);
      repositoriesListView.setOnItemClickListener(new RepositoriesListItemClickListener());
      repositoriesListAdapter = new NotificationRepositoriesListAdapter(getActivity(), (getMainActivity()).getViewData().notificationStream);
      repositoriesListView.setAdapter(repositoriesListAdapter);
      if (!repositoriesListAdapter.setSelectionForFilter(repositoriesListView, currentFilter)) {
        // repo no more in data so reset filter
        getMainActivity().setFilterByRepository(null);
        currentFilter = null;
      }
    }

    //Setup reset filter button
    ImageButton resetButton = (ImageButton) contentView.findViewById(R.id.action_reset_filter);
    if (currentFilter == null) {
      resetButton.setVisibility(View.GONE);
    } else {
      resetButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          getMainActivity().setFilterByRepository(null);
          dismiss();
        }
      });
      //long press tooltip!
      resetButton.setOnLongClickListener(new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View view) {
          Toast.makeText(getContext(), view.getContentDescription(), Toast.LENGTH_SHORT).show();
          return true;
        }
      });
    }

    ActivityTracker.sendView(getActivity(), TAG);
  }

  protected MainActivity getMainActivity() {
    return (MainActivity) getActivity();
  }

  private class RepositoriesListItemClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (repositoriesListView != null) {
        NotifCount nc = (NotifCount) repositoriesListAdapter.getItem(position);
        if (nc != null) {
          if (!getMainActivity().setFilterByRepository(nc.title)) ;
        }
      }
      dismissAllowingStateLoss();
    }
  }
}
