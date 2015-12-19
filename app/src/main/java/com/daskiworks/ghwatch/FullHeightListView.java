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
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * List view which always shows all items, so should be used inside of scroll view!
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class FullHeightListView extends ListView {

  public FullHeightListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public FullHeightListView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public FullHeightListView(Context context) {
    super(context);
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // HACK! TAKE THAT ANDROID!
    // Calculate entire height by providing a very large height hint.
    // But do not use the highest 2 bits of this integer; those are
    // reserved for the MeasureSpec mode.
    int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
    super.onMeasure(widthMeasureSpec, expandSpec);

    ViewGroup.LayoutParams params = getLayoutParams();
    params.height = getMeasuredHeight();

  }

}
