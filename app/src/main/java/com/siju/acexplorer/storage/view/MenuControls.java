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

package com.siju.acexplorer.storage.view;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.Toast;

import com.siju.acexplorer.R;
import com.siju.acexplorer.analytics.Analytics;
import com.siju.acexplorer.model.FileInfo;
import com.siju.acexplorer.model.groups.Category;
import com.siju.acexplorer.model.helper.FileUtils;
import com.siju.acexplorer.model.helper.ShareHelper;
import com.siju.acexplorer.model.root.RootUtils;
import com.siju.acexplorer.storage.model.ViewMode;
import com.siju.acexplorer.storage.model.operations.Operations;
import com.siju.acexplorer.theme.Theme;
import com.siju.acexplorer.view.dialog.DialogHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.siju.acexplorer.R.string.hide;
import static com.siju.acexplorer.model.groups.Category.FAVORITES;
import static com.siju.acexplorer.model.groups.Category.FILES;

/**
 * Created by Siju on 04 September,2017
 */
public class MenuControls implements Toolbar.OnMenuItemClickListener,
                                     android.support.v7.widget.SearchView.OnQueryTextListener
{

    private final String TAG = this.getClass().getSimpleName();

    private final ArrayList<FileInfo> copiedData = new ArrayList<>();

    private Context    context;
    private Activity   activity;
    private Theme      theme;
    private Toolbar    toolbar;
    private Toolbar    bottomToolbar;
    private MenuItem   mRenameItem;
    private MenuItem   mInfoItem;
    private MenuItem   mArchiveItem;
    private MenuItem   mFavItem;
    private MenuItem   mExtractItem;
    private MenuItem   mHideItem;
    private MenuItem   mPermissionItem;
    private MenuItem   mViewItem;
    private MenuItem   searchItem;
    private SearchView searchView;

    private StoragesUiView storagesUiView;
    private Category       category;

    private String  currentDir;
    private boolean isMoveOperation;
    private boolean isPasteVisible;


    MenuControls(Activity activity, StoragesUiView storagesUiView, Theme theme) {
        this.activity = activity;
        this.storagesUiView = storagesUiView;
        this.context = storagesUiView.getContext();
        init();
        setTheme(theme);
    }


    void setCategory(Category category) {
        this.category = category;
    }

    void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
        Log.d(TAG, "setCurrentDir: " + currentDir);
    }

    private void init() {
        bottomToolbar = storagesUiView.findViewById(R.id.toolbar_bottom);
        toolbar = storagesUiView.findViewById(R.id.toolbar);
        setToolbar();
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (storagesUiView.isActionModeActive()) {
                    endActionMode();
                } else {
                    storagesUiView.openDrawer();
                }
            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                inflateBaseMenu();
            }
        }, 200);
    }




    @SuppressWarnings("ConstantConditions")
    private void setToolbar() {
        toolbar.setTitle(R.string.app_name);
        ((AppCompatActivity) activity).setSupportActionBar(toolbar);
        ((AppCompatActivity) activity).getSupportActionBar().setHomeButtonEnabled(true);
        ((AppCompatActivity) activity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) activity).getSupportActionBar().setHomeAsUpIndicator(R.drawable
                                                                                          .ic_drawer);
        storagesUiView.syncDrawer();
    }

    private void setTheme(Theme theme) {
        this.theme = theme;
        bottomToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color
                .colorPrimary));
        switch (theme) {
            case DARK:
                toolbar.setPopupTheme(R.style.Dark_AppTheme_PopupOverlay);
                bottomToolbar.setPopupTheme(R.style.Dark_AppTheme_PopupOverlay);
                break;
            case LIGHT:
                toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay);
                bottomToolbar.setPopupTheme(R.style.AppTheme_PopupOverlay);
                break;
        }
    }


    void startActionMode() {

        setupActionModeToolbar();
        bottomToolbar.getMenu().clear();
        EnhancedMenuInflater.inflate(activity.getMenuInflater(), bottomToolbar.getMenu(),
                                     category);
        setupMenu();
        bottomToolbar.setOnMenuItemClickListener(this);
        showBottomToolbar();
    }

    private void setupMenu() {
        Menu menu = bottomToolbar.getMenu();
        mRenameItem = menu.findItem(R.id.action_edit);
        mInfoItem = menu.findItem(R.id.action_info);
        mArchiveItem = menu.findItem(R.id.action_archive);
        mFavItem = menu.findItem(R.id.action_fav);
        mExtractItem = menu.findItem(R.id.action_extract);
        mHideItem = menu.findItem(R.id.action_hide);
        mPermissionItem = menu.findItem(R.id.action_permissions);
        // Dont show Fav and Archive option for Non file mode
        if (!category.equals(FILES)) {
            mArchiveItem.setVisible(false);
            mExtractItem.setVisible(false);
            mFavItem.setVisible(false);
            mHideItem.setVisible(false);
        }

        switch (theme) {
            case LIGHT:
                mInfoItem.setIcon(R.drawable.ic_info_black);
                mArchiveItem.setIcon(R.drawable.ic_archive_black);
                mExtractItem.setIcon(R.drawable.ic_extract_black);
                mPermissionItem.setIcon(R.drawable.ic_permissions_black);
                mFavItem.setIcon(R.drawable.ic_favorite_black);
                break;
            case DARK:
                break;
        }

    }

    void setupMenuVisibility(SparseBooleanArray selectedItemPos) {
        List<FileInfo> fileInfoList = storagesUiView.getFileList();
        if (selectedItemPos.size() > 1) {
            mRenameItem.setVisible(false);
            mInfoItem.setVisible(false);

        } else {
            mRenameItem.setVisible(true);
            mInfoItem.setVisible(true);
            if (selectedItemPos.size() == 1) {

                boolean isDirectory = fileInfoList.get(selectedItemPos.keyAt(0))
                        .isDirectory();
                String filePath = fileInfoList.get(selectedItemPos.keyAt(0))
                        .getFilePath();

                boolean isRoot = RootUtils.isRootDir(filePath);
                if (FileUtils.isFileCompressed(filePath)) {
                    mExtractItem.setVisible(true);
                    mArchiveItem.setVisible(false);
                }
                if (isRoot) {
                    mPermissionItem.setVisible(true);
                    mExtractItem.setVisible(false);
                    mArchiveItem.setVisible(false);
                }
                if (!isDirectory) {
                    mFavItem.setVisible(false);
                }

            }
            String fileName = fileInfoList.get(selectedItemPos.keyAt(0)).getFileName();

            if (fileName.startsWith(".")) {
                mHideItem.setTitle(context.getString(R.string.unhide));
                if (theme.equals(Theme.DARK)) {
                    mHideItem.setIcon(R.drawable.ic_unhide_white);
                } else {
                    mHideItem.setIcon(R.drawable.ic_unhide_black);

                }
            } else {
                mHideItem.setTitle(context.getString(hide));
                if (theme.equals(Theme.DARK)) {
                    mHideItem.setIcon(R.drawable.ic_hide_white);
                } else {
                    mHideItem.setIcon(R.drawable.ic_hide_black);
                }
            }
        }
    }

    private void setupActionModeToolbar() {
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.action_mode);
        toolbar.setNavigationIcon(R.drawable.ic_up_arrow);
    }

    private void clearActionModeToolbar() {
        toolbar.getMenu().clear();
        inflateBaseMenu();
        toolbar.setNavigationIcon(R.drawable.ic_drawer);
        setToolbarText(context.getString(R.string.app_name));
    }

    private void inflateBaseMenu() {
        toolbar.inflateMenu(R.menu.file_base);
        setupMenuItems(toolbar.getMenu());
    }

    private void setupMenuItems(Menu menu) {
        searchItem = menu.findItem(R.id.action_search);
        searchView = (android.support.v7.widget.SearchView) searchItem.getActionView();
        mViewItem = menu.findItem(R.id.action_view);
        setupSearchView();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Log.d(TAG, "onMenuItemClick: " + item);
        List<FileInfo> fileInfoList = storagesUiView.getFileList();
        SparseBooleanArray selectedItems = storagesUiView.getSelectedItems();

        switch (item.getItemId()) {

            case R.id.action_view:
                storagesUiView.switchView();
                int mode = storagesUiView.getViewMode();
                Analytics.getLogger().switchView(mode == ViewMode.LIST);
                updateMenuTitle(mode);
                break;

            case R.id.action_sort:
                showSortDialog();
                break;

            case R.id.action_cut:
                if (selectedItems != null && selectedItems.size() > 0) {
                    isMoveOperation = true;
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_CUT);
                    onCutCopyOp(selectedItems, fileInfoList);
                }
                break;

            case R.id.action_copy:
                if (selectedItems != null && selectedItems.size() > 0) {
                    isMoveOperation = false;
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_COPY);
                    onCutCopyOp(selectedItems, fileInfoList);
                }
                break;

            case R.id.action_paste:
                isPasteVisible = false;
                if (copiedData.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_PASTE);
                    ArrayList<FileInfo> info = new ArrayList<>();
                    info.addAll(copiedData);
                    Log.d(TAG, "onMenuItemClick: CurrentDir:"+currentDir);
                    storagesUiView.onPasteAction(isMoveOperation, info, currentDir);
                    copiedData.clear();
                    endActionMode();
                }
                break;

            case R.id.action_delete:

                if (selectedItems != null && selectedItems.size() > 0) {
                    ArrayList<FileInfo> filesToDelete = new ArrayList<>();
                    for (int i = 0; i < selectedItems.size(); i++) {
                        FileInfo info = fileInfoList.get(selectedItems.keyAt(i));
                        filesToDelete.add(info);
                    }
                    if (category.equals(FAVORITES)) {
                        Analytics.getLogger().operationClicked(Analytics.Logger.EV_DELETE_FAV);
                        storagesUiView.removeFavorite(filesToDelete);
                        Toast.makeText(context, context.getString(R.string.fav_removed), Toast
                                .LENGTH_SHORT).show();
                    } else {
                        Analytics.getLogger().operationClicked(Analytics.Logger.EV_DELETE);
                        DialogHelper.showDeleteDialog(context, filesToDelete, deleteDialogListener);
                    }
                    endActionMode();

                }
                break;

            case R.id.action_share:
                if (selectedItems != null && selectedItems.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_SHARE);
                    ArrayList<FileInfo> filesToShare = new ArrayList<>();
                    for (int i = 0; i < selectedItems.size(); i++) {
                        FileInfo info = fileInfoList.get(selectedItems.keyAt(i));
                        if (!info.isDirectory()) {
                            filesToShare.add(info);
                        }
                    }
                    ShareHelper.shareFiles(context, filesToShare, category);
                    endActionMode();
                }
                break;

            case R.id.action_edit:

                if (selectedItems != null && selectedItems.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_RENAME);
                    final String oldFilePath = fileInfoList.get(selectedItems.keyAt(0)).
                            getFilePath();
                    int renamedPosition = selectedItems.keyAt(0);
                    renameDialog(oldFilePath, renamedPosition);
                    endActionMode();
                }
                break;

            case R.id.action_info:

                if (selectedItems != null && selectedItems.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_PROPERTIES);
                    FileInfo fileInfo = fileInfoList.get(selectedItems.keyAt(0));
                    DialogHelper.showInfoDialog(context, fileInfo, category.equals(FILES));
                    endActionMode();
                }
                break;

            case R.id.action_archive:

                if (selectedItems != null && selectedItems.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_ARCHIVE);
                    ArrayList<FileInfo> paths = new ArrayList<>();
                    for (int i = 0; i < selectedItems.size(); i++) {
                        FileInfo info = fileInfoList.get(selectedItems.keyAt(i));
                        paths.add(info);
                    }
                    DialogHelper.showCompressDialog(context, paths,
                                                    compressDialogListener);
                    endActionMode();
                }
                break;

            case R.id.action_fav:

                if (selectedItems != null && selectedItems.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_ADD_FAV);
                    int count = 0;
                    ArrayList<FileInfo> favList = new ArrayList<>();
                    for (int i = 0; i < selectedItems.size(); i++) {
                        FileInfo info = fileInfoList.get(selectedItems.keyAt(i));
                        // Fav option meant only for directories
                        if (info.isDirectory()) {
                            favList.add(info);
                            count++;
                        }
                    }

                    if (count > 0) {
                        storagesUiView.updateFavouritesGroup(favList);
                    }
                    endActionMode();
                }
                break;

            case R.id.action_extract:

                if (selectedItems != null && selectedItems.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_EXTRACT);
                    FileInfo fileInfo = fileInfoList.get(selectedItems.keyAt(0));
                    String currentFile = fileInfo.getFilePath();
                    DialogHelper.showExtractOptions(context, currentFile,
                                                    extractDialogListener);
                    endActionMode();
                }

                break;

            case R.id.action_hide:

                if (selectedItems != null && selectedItems.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_HIDE);
                    ArrayList<FileInfo> infoList = new ArrayList<>();
                    ArrayList<Integer> pos = new ArrayList<>();
                    for (int i = 0; i < selectedItems.size(); i++) {
                        infoList.add(fileInfoList.get(selectedItems.keyAt(i)));
                        pos.add(selectedItems.keyAt(i));

                    }
                    storagesUiView.hideUnHideFiles(infoList, pos);
                    endActionMode();
                }
                break;

            case R.id.action_permissions:
                if (selectedItems != null && selectedItems.size() > 0) {
                    Analytics.getLogger().operationClicked(Analytics.Logger.EV_PERMISSIONS);
                    FileInfo info = fileInfoList.get(selectedItems.keyAt(0));
                    isDirectory = info.isDirectory();
                    storagesUiView.getPermissions(info.getFilePath(), isDirectory);
                    endActionMode();
                }
                break;

            case R.id.action_create:
                storagesUiView.showCreateDirDialog();
                break;

            case R.id.action_cancel:
                endActionMode();
                break;

            case R.id.action_select_all:
                storagesUiView.onSelectAllClicked();
                break;
        }
        return false;
    }

    private void clearSelection() {
        storagesUiView.clearSelection();
        storagesUiView.clearSelectedPos();
    }

    private boolean isDirectory;


    boolean isPasteOp() {
        return isPasteVisible;
    }

    private void onCutCopyOp(SparseBooleanArray selectedItems, List<FileInfo> fileInfoList) {
        FileUtils.showMessage(context, selectedItems.size() + " " +
                context.getString(R.string
                                          .msg_cut_copy));
        copiedData.clear();
        for (int i = 0; i < selectedItems.size(); i++) {
            copiedData.add(fileInfoList.get(selectedItems.keyAt(i)));
        }
        isPasteVisible = true;
        hideSelectAll();
        showPasteIcon();
        setToolbarText(String.format(context.getString(R.string.clipboard), copiedData.size()));
        clearSelection();
        storagesUiView.endDrag();
    }

    void hideBottomToolbar() {
        bottomToolbar.setVisibility(View.GONE);
    }


    boolean isSearch() {
        return !searchView.isIconified();
    }

    void endSearch() {
        searchItem.collapseActionView();
    }


    private void renameDialog(final String oldFilePath, final int
            position) {
        String fileName;
        if (new File(oldFilePath).isFile()) {
            fileName = oldFilePath.substring(oldFilePath.lastIndexOf("/") + 1, oldFilePath
                    .lastIndexOf("."));
        } else {
            fileName = oldFilePath.substring(oldFilePath.lastIndexOf("/") + 1, oldFilePath
                    .length());
        }

        storagesUiView.showRenameDialog(oldFilePath, fileName, position);
    }


    private void showPasteIcon() {
        bottomToolbar.setVisibility(View.VISIBLE);
        bottomToolbar.getMenu().clear();
        bottomToolbar.inflateMenu(R.menu.action_mode_paste);
    }

    private void hideSelectAll() {
        toolbar.getMenu().findItem(R.id.action_select_all).setVisible(false);
    }


    void endActionMode() {
        isPasteVisible = false;
        hideBottomToolbar();
        clearActionModeToolbar();
        storagesUiView.endActionMode();
        if (isSearchActive()) {
            isSearchActive = false;
            storagesUiView.refreshList();
        }
    }

    private void updateMenuTitle(int viewMode) {
        mViewItem.setTitle(viewMode == ViewMode.LIST ? R.string.action_view_grid : R.string
                .action_view_list);
    }


    private void showSortDialog() {
        DialogHelper.showSortDialog(context, storagesUiView.getSortMode(), alertDialogListener);
    }


    private void showBottomToolbar() {
        bottomToolbar.setVisibility(View.VISIBLE);
    }

    void setToolbarText(String text) {
        toolbar.setTitle(text);
    }

    private void setupSearchView() {
        // Disable full screen keyboard in landscape
        searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(this);
        searchView.setMaxWidth(Integer.MAX_VALUE);

        SearchManager searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(storagesUiView.getActivity()
                                                                                 .getComponentName()));
        }
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Analytics.getLogger().searchClicked(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });
    }

    private void hideSearchView() {
        searchItem.collapseActionView();
    }

    void performVoiceSearch(String query) {
        searchView.setQuery(query, false);
    }

    private boolean isSearchActive() {
        return isSearchActive;
    }
    private boolean isSearchActive;


    @Override
    public boolean onQueryTextChange(String query) {
        if (storagesUiView.isActionModeActive()) {
            return true;
        }
        isSearchActive = !query.isEmpty();
        storagesUiView.onQueryTextChange(query);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        hideSearchView();
        isSearchActive = false;
        return false;
    }


    private DialogHelper.ExtractDialogListener extractDialogListener = new DialogHelper
            .ExtractDialogListener()
    {

        @Override
        public void onPositiveButtonClick(Dialog dialog, String currentFile, String newFileName,
                                          boolean isChecked) {
            storagesUiView.onExtractPositiveClick(dialog, currentFile, newFileName, isChecked);
        }

        @Override
        public void onSelectButtonClick(Button pathButton) {
            storagesUiView.showSelectPathDialog(pathButton);

        }
    };


    private DialogHelper.AlertDialogListener alertDialogListener = new DialogHelper
            .AlertDialogListener()
    {

        @Override
        public void onPositiveButtonClick(View view) {
            storagesUiView.sortFiles((int) view.getTag());
        }

        @Override
        public void onNegativeButtonClick(View view) {

        }

        @Override
        public void onNeutralButtonClick(View view) {

        }
    };

    private DialogHelper.DeleteDialogListener deleteDialogListener = new DialogHelper
            .DeleteDialogListener()
    {
        @Override
        public void onPositiveButtonClick(View view, ArrayList<FileInfo> filesToDelete) {
            storagesUiView.deleteFiles(filesToDelete);
        }
    };

    private DialogHelper.PermissionDialogListener permissionDialogListener = new DialogHelper
            .PermissionDialogListener()
    {

        @Override
        public void onPositiveButtonClick(String path, boolean isDir, String permissions) {
            storagesUiView.setPermissions(path, isDir, permissions);

        }
    };

    private DialogHelper.CompressDialogListener compressDialogListener = new DialogHelper
            .CompressDialogListener()
    {

        @Override
        public void onPositiveButtonClick(Dialog dialog, Operations operation, String
                newFileName, String extension, ArrayList<FileInfo> paths) {
            storagesUiView.onCompressPosClick(dialog, newFileName, extension, paths);
        }

        @Override
        public void onNegativeButtonClick(Operations operation) {

        }
    };


    void onPermissionsFetched(ArrayList<Boolean[]> permissionList) {
        DialogHelper.showPermissionsDialog(context, currentDir, isDirectory, permissionList,
                                           permissionDialogListener);
    }



}
