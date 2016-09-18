package com.siju.acexplorer;

import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


import com.siju.acexplorer.filesystem.FileConstants;
import com.siju.acexplorer.filesystem.ui.DialogBrowseFragment;


/**
 * Created by Siju on 04-09-2016.
 */
public class TransparentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null && intent.getAction().equals(RingtoneManager
                .ACTION_RINGTONE_PICKER)) {
//            mRingtonePickerIntent = true;
            showRingtonePickerDialog();
        }
    }

    private void showRingtonePickerDialog() {

        DialogBrowseFragment dialogFragment = new DialogBrowseFragment();
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL,checkTheme());
        Bundle args = new Bundle();
        args.putBoolean("ringtone_picker", true);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "Browse Fragment");


    }

    private int  checkTheme() {
        int mCurrentTheme = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(FileConstants.CURRENT_THEME, FileConstants.THEME_LIGHT);

        if (mCurrentTheme == FileConstants.THEME_DARK) {
            return R.style.Dark_AppTheme_NoActionBar;
        }
        else {
            return R.style.AppTheme_NoActionBar;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("TAG", "On activity result");
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
