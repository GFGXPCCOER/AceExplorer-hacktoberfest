package com.siju.acexplorer.storage.modules.picker.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;

import androidx.preference.PreferenceManager;

import com.siju.acexplorer.AceApplication;
import com.siju.acexplorer.main.model.StorageUtils;

import java.util.List;


public class PickerModelImpl implements PickerModel {

    private static final String RINGTONE_PICKER_PATH = "ringtone_picker_path";
    private PickerModel.Listener listener;
    private SharedPreferences preferences;

    public PickerModelImpl() {
        Context context = AceApplication.Companion.getAppContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void getStoragesList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                List<String> list = StorageUtils.INSTANCE.getStorageDirectories();
                listener.onStoragesFetched(list);
            }
        }).start();
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void saveLastRingtoneDir(String currentPath) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(RINGTONE_PICKER_PATH, currentPath).apply();
    }

    public String getLastSavedRingtoneDir() {
        return preferences.getString(RINGTONE_PICKER_PATH, null);
    }
}
