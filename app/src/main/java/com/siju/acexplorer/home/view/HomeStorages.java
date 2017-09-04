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

package com.siju.acexplorer.home.view;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.siju.acexplorer.R;
import com.siju.acexplorer.model.SectionItems;
import com.siju.acexplorer.model.groups.StoragesGroup;
import com.siju.acexplorer.theme.Theme;

import java.util.ArrayList;

import static com.siju.acexplorer.model.groups.Category.FILES;

/**
 * Created by Siju on 03 September,2017
 */
public class HomeStorages implements View.OnClickListener {

    private HomeUiView homeUiView;
    private LinearLayout layoutStorages;
    private TableLayout storagesContainer;
    private ArrayList<SectionItems> storagesList;

    private Context context;

    HomeStorages(HomeUiView homeUiView, Theme theme) {
        this.homeUiView = homeUiView;
        this.context = homeUiView.getContext();
        init();
        setTheme(theme);
    }

    private void setTheme(Theme theme) {
        switch (theme) {
            case DARK:
                layoutStorages.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_background));
                break;
            case LIGHT:
                layoutStorages.setBackgroundColor(ContextCompat.getColor(context, R.color.light_home_lib));
                break;
        }
    }

    private void init() {
        storagesContainer =  homeUiView.findViewById(R.id.storagesContainer);
        layoutStorages =  homeUiView.findViewById(R.id.layoutStorages);
        initializeStorageGroup();
        inflateStoragesItem();
    }

    private void initializeStorageGroup() {
        storagesList = new ArrayList<>();
        storagesList = StoragesGroup.getInstance().getStoragesList();
    }


    private void inflateStoragesItem() {
        storagesContainer.removeAllViews();

        for (int i = 0; i < storagesList.size(); i++) {
            RelativeLayout storageItemContainer = (RelativeLayout) View.inflate(context, R.layout.storage_item,
                    null);
            ProgressBar progressBarSpace =  storageItemContainer
                    .findViewById(R.id.progressBarSD);
            ImageView imageStorage =  storageItemContainer.findViewById(R.id.imageStorage);
            TextView textStorage =  storageItemContainer.findViewById(R.id.textStorage);
            TextView textSpace =  storageItemContainer.findViewById(R.id.textSpace);
            View homeStoragesDivider = storageItemContainer.findViewById(R.id.home_storages_divider);

            imageStorage.setImageResource(storagesList.get(i).getIcon());
            textStorage.setText(storagesList.get(i).getFirstLine());
            textSpace.setText(storagesList.get(i).getSecondLine());
            progressBarSpace.setProgress(storagesList.get(i).getProgress());

            storagesContainer.addView(storageItemContainer);
            Log.d(this.getClass().getSimpleName(), "inflateStoragesItem: "+storageItemContainer);
            storageItemContainer.setOnClickListener(this);
            storageItemContainer.setTag(storagesList.get(i).getPath());
            if (i + 1 == storagesList.size()) {
                homeStoragesDivider.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public void onClick(View view) {
        Object tag = view.getTag();
        homeUiView.loadFileList((String) tag, FILES);
    }
}
