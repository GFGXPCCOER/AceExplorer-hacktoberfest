/*
 * Copyright (C) 2017 Ace Explorer owned by Siju Sakaria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.siju.acexplorer.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.siju.acexplorer.R;
import com.siju.acexplorer.billing.BillingHelper;
import com.siju.acexplorer.billing.BillingStatus;
import com.siju.acexplorer.common.Logger;
import com.siju.acexplorer.filesystem.model.FavInfo;
import com.siju.acexplorer.filesystem.utils.FileUtils;
import com.siju.acexplorer.model.SectionGroup;
import com.siju.acexplorer.model.SectionItems;
import com.siju.acexplorer.model.groups.DrawerGroups;
import com.siju.acexplorer.premium.Premium;
import com.siju.acexplorer.settings.SettingsActivity;
import com.siju.acexplorer.theme.Theme;

import java.util.ArrayList;

import static com.siju.acexplorer.filesystem.storage.StorageUtils.getDownloadsDirectory;

/**
 * Created by Siju on 28 August,2017
 */
public class NavigationDrawer implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    private static final int PREFS_REQUEST = 1000;
    private static final int REQUEST_INVITE = 4000;

    private Activity activity;
    private Context context;

    private DrawerLayout drawerLayout;
    private NavigationView drawerPane;
    private ExpandableListView expandableListView;
    private ExpandableListAdapter expandableListAdapter;
    private DrawerGenericAdapter drawerGenericAdapter;
    private ImageView imageInvite;
    private ActionBarDrawerToggle drawerToggle;
    private ListView genericList;

    private ArrayList<SectionGroup> totalGroupData = new ArrayList<>();
    private ArrayList<SectionItems> favouritesGroupChild = new ArrayList<>();
    private boolean isPremium;
    private MainUiView uiView;


    NavigationDrawer(Activity activity, MainUiView uiView, Theme theme) {
        this.context = activity.getBaseContext();
        this.activity = activity;
        this.uiView = uiView;
        init();
        setTheme(theme);
        initListeners();
        setAdapter();
        uiView.getTotalGroupData();
    }

    private void init() {
        drawerLayout = uiView.findViewById(R.id.drawer_layout);
        drawerPane = uiView.findViewById(R.id.drawerPane);
        genericList = uiView.findViewById(R.id.listGeneric);
        expandableListView = uiView.findViewById(R.id.expandListDrawer);
        imageInvite = uiView.findViewById(R.id.imageInvite);
        registerContextMenu();
    }

    private void setTheme(Theme theme) {
        switch (theme) {
            case DARK:
                drawerPane.setBackgroundColor(ContextCompat.getColor(context, R
                        .color.dark_background));
                break;
            case LIGHT:
                drawerPane.setBackgroundColor(ContextCompat.getColor(context, R
                        .color.light_background));
                break;
        }
    }

    private void initListeners() {
        drawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, R.string
                .navigation_drawer_open, R.string
                .navigation_drawer_close) {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0);
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int
                    childPosition, long id) {
                onDrawerItemClick(groupPosition, childPosition);
                return false;
            }
        });

        expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return false;
            }
        });
        imageInvite.setOnClickListener(this);
        genericList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                onClick(isPremium ? position + 1 : position);
            }
        });


    }

    private void setAdapter() {
        drawerGenericAdapter = new DrawerGenericAdapter(context);
        genericList.setAdapter(drawerGenericAdapter);
    }

    private void registerContextMenu() {
        activity.registerForContextMenu(expandableListView);
    }

    void unregisterContextMenu() {
        activity.unregisterForContextMenu(expandableListView);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageInvite:
                Intent inviteIntent = new AppInviteInvitation.IntentBuilder(context.getString(R
                        .string.app_invite_title))
                        .setMessage(context.getString(R.string.app_invite_msg))
                        .build();
                activity.startActivityForResult(inviteIntent, REQUEST_INVITE);
                break;

        }
    }

    private void onClick(int position) {

        switch (position) {
            case 0:
                if (BillingHelper.getInstance().getInAppBillingStatus().equals(BillingStatus
                        .UNSUPPORTED)) {

                    Toast.makeText(context, context.getString(R.string.billing_unsupported), Toast
                            .LENGTH_SHORT).show();
                }
                else {
                    Premium premium = new Premium(activity);
                    premium.showPremiumDialog();
                }
                break;
            case 1: // Rate us
                Intent intent = new Intent(Intent.ACTION_VIEW);
                // Try Google play
                intent.setData(Uri
                        .parse("market://details?id=" + context.getPackageName()));
                if (FileUtils.isPackageIntentUnavailable(context, intent)) {
                    // Market (Google play) app seems not installed,
                    // let's try to open a webbrowser
                    intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" +
                            context.getPackageName()));
                    if (FileUtils.isPackageIntentUnavailable(context, intent)) {
                        Toast.makeText(context,
                                context.getString(R.string.msg_error_not_supported),
                                Toast.LENGTH_SHORT).show();
                    }
                    else {
                        startActivity(intent);
                    }
                }
                else {
                    startActivity(intent);
                }
                drawerLayout.closeDrawer(drawerPane);
                break;
            case 2: // Settings
                Intent intent1 = new Intent(activity, SettingsActivity.class);
                final int enter_anim = android.R.anim.fade_in;
                final int exit_anim = android.R.anim.fade_out;
                activity.overridePendingTransition(enter_anim, exit_anim);
                activity.startActivityForResult(intent1,
                        PREFS_REQUEST);
                expandableListView.setSelection(0);
                drawerLayout.closeDrawer(drawerPane);
                break;
        }
    }

    private void startActivity(Intent intent) {
        activity.startActivity(intent);
    }

    private void onDrawerItemClick(int groupPos, int childPos) {
        String path = totalGroupData.get(groupPos).getmChildItems().get(childPos).getPath();
        Log.d(TAG, "onDrawerItemClick: " + path);
        displaySelectedGroup(groupPos, childPos, path);
        drawerLayout.closeDrawer(drawerPane);
    }

    private void displaySelectedGroup(int groupPos, int childPos, String path) {
        DrawerGroups drawerGroups = DrawerGroups.getGroupFromPos(groupPos);
        switch (drawerGroups) {
            case STORAGE:
            case FAVORITES:
                uiView.onStorageItemClicked(groupPos, path);
                break;
            case LIBRARY:
                uiView.onLibraryItemClicked(childPos + 1);
                break;
        }
    }

    void updateFavourites(ArrayList<FavInfo> favInfoArrayList) {
        for (int i = 0; i < favInfoArrayList.size(); i++) {
            SectionItems favItem = new SectionItems(favInfoArrayList.get(i).getFileName(),
                    favInfoArrayList.get(i)
                            .getFilePath(), R.drawable.ic_fav_folder, favInfoArrayList.get(i)
                    .getFilePath
                            (), 0);
            if (!favouritesGroupChild.contains(favItem)) {
                favouritesGroupChild.add(favItem);
            }
        }
        expandableListAdapter.notifyDataSetChanged();

    }

    void removeFavourites(ArrayList<FavInfo> favInfoArrayList) {
        for (int i = 0; i < favInfoArrayList.size(); i++) {
            SectionItems favItem = new SectionItems(favInfoArrayList.get(i).getFileName(),
                    favInfoArrayList.get(i)
                            .getFilePath(), R.drawable.ic_fav_folder, favInfoArrayList.get(i)
                    .getFilePath
                            (), 0);
            if (favouritesGroupChild.contains(favItem)) {
                favouritesGroupChild.remove(favItem);
            }
        }
        expandableListAdapter.notifyDataSetChanged();
    }

    FavInfo removeFavorite(int groupPos, int childPos) {
        String path = totalGroupData.get(groupPos).getmChildItems().get(childPos).getPath();
        String name = totalGroupData.get(groupPos).getmChildItems().get(childPos)
                .getFirstLine();
        FavInfo favInfo = new FavInfo();
        favInfo.setFileName(name);
        favInfo.setFilePath(path);
        favouritesGroupChild.remove(childPos);
        expandableListAdapter.notifyDataSetChanged();
        return favInfo;
    }

    void resetFavouritesGroup() {

        for (int i = favouritesGroupChild.size() - 1; i >= 0; i--) {
            if (!favouritesGroupChild.get(i).getSecondLine().equalsIgnoreCase
                    (getDownloadsDirectory())) {
                favouritesGroupChild.remove(i);
            }
        }
        expandableListAdapter.notifyDataSetChanged();
        expandableListView.smoothScrollToPosition(0);

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_INVITE:
                if (resultCode == Activity.RESULT_OK) {
                    // Get the invitation IDs of all sent messages
                    String[] ids = AppInviteInvitation.getInvitationIds(resultCode, intent);
                    for (String id : ids) {
                        Logger.log(TAG, "handleActivityResult: sent invitation " + id);
                    }
                }
                else {
                    Toast.makeText(context, context.getString(R.string.app_invite_failed), Toast
                            .LENGTH_SHORT).show();
                }
                break;
        }
    }

    void setPremium() {
        isPremium = true;
        drawerGenericAdapter.setPremium();
    }

    public void syncDrawerState() {
        drawerToggle.syncState();
    }

    void openDrawer() {
        drawerLayout.openDrawer(Gravity.START);
    }

    void onTotalGroupDataFetched(ArrayList<SectionGroup> totalData) {
        totalGroupData = totalData;
        favouritesGroupChild = totalGroupData.get(1).getmChildItems();
        expandableListAdapter = new ExpandableListAdapter(context, totalGroupData);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.expandGroup(0);
    }
}
