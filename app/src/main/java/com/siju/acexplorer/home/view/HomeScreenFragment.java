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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.siju.acexplorer.DrawerListener;
import com.siju.acexplorer.R;
import com.siju.acexplorer.common.Logger;
import com.siju.acexplorer.home.model.HomeModel;
import com.siju.acexplorer.home.model.HomeModelImpl;
import com.siju.acexplorer.home.model.LoaderHelper;
import com.siju.acexplorer.home.presenter.HomePresenter;
import com.siju.acexplorer.home.presenter.HomePresenterImpl;

public class HomeScreenFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();

    private HomePresenter homePresenter;
    private HomeView homeView;
    private HomeModel homeModel;
    private DrawerListener drawerListener;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_base, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        Logger.log(TAG, "onActivityCreated" + savedInstanceState);

        LinearLayout linearLayout = getActivity().findViewById(R.id.home_base);
        homeView = new HomeBridge(this, linearLayout, drawerListener);
        homeModel = new HomeModelImpl();
        LoaderHelper loaderHelper = new LoaderHelper(this);

        homePresenter = new HomePresenterImpl(homeView, homeModel, loaderHelper,
                getActivity().getSupportLoaderManager());

        homeView.init();
    }


    @Override
    public void onPause() {
        homeView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        homeView.onResume();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        homeView.handleActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onDestroy() {
        homeView.onExit();
        super.onDestroy();
    }

    public void updateFavoritesCount(int size) {
        homeView.updateFavoritesCount(size);
    }

    public void setDrawerListener(DrawerListener drawerListener) {
        this.drawerListener = drawerListener;
    }
}
