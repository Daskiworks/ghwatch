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

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.daskiworks.ghwatch.backend.PreferencesUtils;

/**
 * Configuration Backup agent - http://developer.android.com/guide/topics/data/backup.html
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class MyBackupAgent extends BackupAgentHelper {

  private static final String TAG = MyBackupAgent.class.getSimpleName();

  static final String PREFS_BACKUP_KEY = "prefs";

  @Override
  public void onCreate() {
    SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, this.getPackageName() + "_preferences");
    addHelper(PREFS_BACKUP_KEY, helper);
    Log.d(TAG, "Backup manager created");
  }

  @Override
  public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
    Log.i(TAG, "Backup started");
    super.onBackup(oldState, data, newState);
    Log.i(TAG, "Backup finished");
  }

  @Override
  public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
    Log.i(TAG, "Restore from backup started");
    super.onRestore(data, appVersionCode, newState);
    PreferencesUtils.patchAfterRestore(this);
    Log.i(TAG, "Restore from backup finished");
  }
}
