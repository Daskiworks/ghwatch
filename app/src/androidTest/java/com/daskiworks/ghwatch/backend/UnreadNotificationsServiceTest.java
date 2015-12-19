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
package com.daskiworks.ghwatch.backend;

import android.test.AndroidTestCase;

import com.daskiworks.ghwatch.Utils;
import com.daskiworks.ghwatch.model.NotificationStream;

/**
 * Unit test for {@link UnreadNotificationsService}
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class UnreadNotificationsServiceTest extends AndroidTestCase {

  public void test_prepareLastModifiedHeaderContent() {

    try {

      PreferencesUtils.remove(getContext(), PreferencesUtils.PREF_SERVER_CHECK_FULL);

      UnreadNotificationsService tested = new UnreadNotificationsService(getContext());

      // case - no last data exists
      assertNull(tested.prepareLastModifiedHeaderContent(null, false));

      NotificationStream oldNs = new NotificationStream();

      // case - last check is too young so we can do incremental update
      oldNs.setLastModified("LastMod");
      oldNs.setLastFullUpdateTimestamp(System.currentTimeMillis() - (6 * Utils.MILLIS_HOUR) + Utils.MILLIS_SECOND);
      assertEquals("LastMod", tested.prepareLastModifiedHeaderContent(oldNs, false));

      // case - last check is too young but full update is configured
      PreferencesUtils.storeBoolean(getContext(), PreferencesUtils.PREF_SERVER_CHECK_FULL, true);
      oldNs.setLastModified("LastMod");
      oldNs.setLastFullUpdateTimestamp(System.currentTimeMillis() - (6 * Utils.MILLIS_HOUR) + Utils.MILLIS_SECOND);
      assertNull(tested.prepareLastModifiedHeaderContent(oldNs, false));
      PreferencesUtils.storeBoolean(getContext(), PreferencesUtils.PREF_SERVER_CHECK_FULL, false);

      // case - last check is too old so we can do full update
      oldNs.setLastFullUpdateTimestamp(System.currentTimeMillis() - (6 * Utils.MILLIS_HOUR) - Utils.MILLIS_SECOND);
      assertNull(tested.prepareLastModifiedHeaderContent(oldNs, false));

      // WIFI

      // case - last check is too young so we can do incremental update
      oldNs.setLastModified("LastMod");
      oldNs.setLastFullUpdateTimestamp(System.currentTimeMillis() - (1 * Utils.MILLIS_HOUR) + Utils.MILLIS_SECOND);
      assertEquals("LastMod", tested.prepareLastModifiedHeaderContent(oldNs, true));

      // case - last check is too young but full update is forced
      PreferencesUtils.storeBoolean(getContext(), PreferencesUtils.PREF_SERVER_CHECK_FULL, true);
      oldNs.setLastModified("LastMod");
      oldNs.setLastFullUpdateTimestamp(System.currentTimeMillis() - (1 * Utils.MILLIS_HOUR) + Utils.MILLIS_SECOND);
      assertNull(tested.prepareLastModifiedHeaderContent(oldNs, true));
      PreferencesUtils.storeBoolean(getContext(), PreferencesUtils.PREF_SERVER_CHECK_FULL, false);

      // case - last check is too old so we can do full update
      oldNs.setLastFullUpdateTimestamp(System.currentTimeMillis() - (1 * Utils.MILLIS_HOUR) - Utils.MILLIS_SECOND);
      assertNull(tested.prepareLastModifiedHeaderContent(oldNs, true));
    } finally {
      PreferencesUtils.remove(getContext(), PreferencesUtils.PREF_SERVER_CHECK_FULL);
    }
  }

}
