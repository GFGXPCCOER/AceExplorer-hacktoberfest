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
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.siju.acexplorer.BaseActivity;
import com.siju.acexplorer.DrawerListener;
import com.siju.acexplorer.R;
import com.siju.acexplorer.billing.BillingStatus;
import com.siju.acexplorer.common.Logger;
import com.siju.acexplorer.common.SharedPreferenceWrapper;
import com.siju.acexplorer.filesystem.FileConstants;
import com.siju.acexplorer.filesystem.backstack.BackStackInfo;
import com.siju.acexplorer.filesystem.backstack.NavigationCallback;
import com.siju.acexplorer.filesystem.backstack.NavigationInfo;
import com.siju.acexplorer.filesystem.helper.FileOpsHelper;
import com.siju.acexplorer.filesystem.model.BackStackModel;
import com.siju.acexplorer.filesystem.model.FavInfo;
import com.siju.acexplorer.filesystem.model.FileInfo;
import com.siju.acexplorer.filesystem.modes.ViewMode;
import com.siju.acexplorer.filesystem.operations.Operations;
import com.siju.acexplorer.filesystem.views.FastScrollRecyclerView;
import com.siju.acexplorer.filesystem.zip.ZipViewer;
import com.siju.acexplorer.model.groups.Category;
import com.siju.acexplorer.permission.PermissionUtils;
import com.siju.acexplorer.storage.view.custom.CustomGridLayoutManager;
import com.siju.acexplorer.storage.view.custom.CustomLayoutManager;
import com.siju.acexplorer.storage.view.custom.DividerItemDecoration;
import com.siju.acexplorer.storage.view.custom.GridItemDecoration;
import com.siju.acexplorer.theme.Theme;
import com.siju.acexplorer.utils.Dialogs;
import com.siju.acexplorer.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.siju.acexplorer.filesystem.FileConstants.ADS;
import static com.siju.acexplorer.filesystem.helper.MediaStoreHelper.scanFile;
import static com.siju.acexplorer.filesystem.helper.ViewHelper.viewFile;
import static com.siju.acexplorer.filesystem.operations.OperationUtils.ACTION_OP_REFRESH;
import static com.siju.acexplorer.filesystem.operations.OperationUtils.ACTION_RELOAD_LIST;
import static com.siju.acexplorer.filesystem.operations.OperationUtils.KEY_FILEPATH;
import static com.siju.acexplorer.filesystem.operations.OperationUtils.KEY_OPERATION;
import static com.siju.acexplorer.model.groups.Category.DOWNLOADS;
import static com.siju.acexplorer.model.groups.Category.FILES;
import static com.siju.acexplorer.model.groups.Category.checkIfFileCategory;

/**
 * Created by Siju on 02 September,2017
 */
public class StoragesUiView extends CoordinatorLayout implements View.OnClickListener, NavigationCallback {

    private final String TAG = this.getClass().getSimpleName();
    private Theme theme;
    private Fragment fragment;
    private StorageBridge bridge;
    private CoordinatorLayout mMainLayout;
    private FastScrollRecyclerView fileList;
    private FileListAdapter fileListAdapter;
    private ArrayList<FileInfo> fileInfoList;
    private String currentDir;
    private Category category;
    private int viewMode = ViewMode.LIST;
    private boolean isZipViewer;
    private SharedPreferenceWrapper sharedPreferenceWrapper;
    private TextView mTextEmpty;
    private boolean mIsDualModeEnabled;
    private boolean isDragStarted;
    private long mLongPressedTime;
    private View mItemView;
    private ArrayList<FileInfo> draggedData = new ArrayList<>();
    private RecyclerView.LayoutManager layoutManager;
    private String mLastSinglePaneDir;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ActionMode actionMode;
    private SparseBooleanArray mSelectedItemPositions = new SparseBooleanArray();

    private String mSelectedPath;
    private Button buttonPathSelect;
    private final HashMap<String, Bundle> scrollPosition = new HashMap<>();
    private int gridCols;
    private SharedPreferences preferences;
    private int mCurrentOrientation;
    private final ArrayList<FileInfo> mCopiedData = new ArrayList<>();
    private final boolean mIsRootMode = true;
    private Dialogs dialogs;
    private boolean shouldStopAnimation = true;
    private DividerItemDecoration mDividerItemDecoration;
    private GridItemDecoration mGridItemDecoration;
    private boolean mInstanceStateExists;
    private final int DIALOG_FRAGMENT = 5000;
    public static final int SAF_REQUEST = 2000;
    private boolean isPremium = true;
    private AdView mAdView;
    private boolean isInSelectionMode;
    private FloatingActionsMenu fabCreateMenu;
    private FloatingActionButton fabCreateFolder;
    private FloatingActionButton fabCreateFile;
    private FloatingActionButton fabOperation;
    private FrameLayout frameLayoutFab;
    private boolean isHomeScreenEnabled;
    private NavigationInfo navigationInfo;
    private BackStackInfo backStackInfo;
    private Theme currentTheme;
    private FileOpsHelper fileOpHelper;

    private boolean isPasteVisible;

    private DrawerListener drawerListener;
    private MenuControls menuControls;
    private DragHelper dragHelper;


    public StoragesUiView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public static StoragesUiView inflate(ViewGroup parent) {
        return (StoragesUiView) LayoutInflater.from(parent.getContext()).inflate(R.layout.main_list,
                parent,
                false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initializeViews();

    }

    private void initializeViews() {
        mMainLayout = findViewById(R.id.main_content);
        fileList = findViewById(R.id.recyclerViewFileList);
        mTextEmpty = findViewById(R.id.textEmpty);
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        int colorResIds[] = {R.color.colorPrimaryDark, R.color.colorPrimary, R.color
                .colorPrimaryDark};
        mSwipeRefreshLayout.setColorSchemeResources(colorResIds);
        mSwipeRefreshLayout.setDistanceToTriggerSync(500);

//        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        frameLayoutFab = findViewById(R.id.frameLayoutFab);
//        aceActivity.syncDrawerState();
        fabCreateMenu = findViewById(R.id.fabCreate);
        fabCreateFolder = findViewById(R.id.fabCreateFolder);
        fabCreateFile = findViewById(R.id.fabCreateFile);
        fabOperation = findViewById(R.id.fabOperation);
        frameLayoutFab.getBackground().setAlpha(0);
    }


    private void setTheme() {
        currentTheme = ((BaseActivity) getActivity()).getCurrentTheme();
        switch (currentTheme) {
            case DARK:
                mMainLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color
                        .dark_background));

//                frameLayoutFab.setBackgroundColor(ContextCompat.getColor(getContext(), R.color
// .dark_overlay));
                break;
        }
    }


    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    private AppCompatActivity getActivity() {
        return (AppCompatActivity) fragment.getActivity();
    }

    public void setBridgeRef(StorageBridge bridge) {
        this.bridge = bridge;
    }

    void initialize() {
        sharedPreferenceWrapper = new SharedPreferenceWrapper();
        dragHelper = new DragHelper(getContext());
        setTheme();
        fileList.setOnDragListener(dragHelper.getDragEventListener());
        menuControls = new MenuControls(getActivity(), this, theme);
        menuControls.setCategory(category);
        checkBillingStatus();
        registerReceivers();

        fileOpHelper = new FileOpsHelper(this);
        dialogs = new Dialogs();
        navigationInfo = new NavigationInfo(this, this);
        backStackInfo = new BackStackInfo();

        mCurrentOrientation = getResources().getConfiguration().orientation;
        getPreferences();
        getArgs();
        setupList();
        if (shouldShowPathNavigation()) {
            navigationInfo.setNavDirectory(currentDir, isHomeScreenEnabled, category);
        }
        else {
            navigationInfo.addHomeNavButton(isHomeScreenEnabled, category);
        }
        backStackInfo.addToBackStack(currentDir, category);
        refreshList();
        initializeListeners();
        createDualFrag();
    }

    private void checkBillingStatus() {
        BillingStatus billingStatus = bridge.checkBillingStatus();
        isPremium = billingStatus == BillingStatus.PREMIUM;
        Log.d(TAG, "checkBillingStatus: " + billingStatus);
        switch (billingStatus) {
            case PREMIUM:
                onPremiumVersion();
                break;
            case UNSUPPORTED:
            case FREE:
                onFreeVersion();
                break;
        }
    }


    public void onPermissionGranted() {
        getLibraries();
    }


    private boolean hasStoragePermission() {
        return PermissionUtils.hasStoragePermission();
    }

    public void onFreeVersion() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        showAds();
    }

    public void onPremiumVersion() {
        hideAds();
    }

    private BroadcastReceiver adsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals(ADS)) {
                isPremium = intent.getBooleanExtra(FileConstants.KEY_PREMIUM, false);
                if (isPremium) {
                    onPremiumVersion();
                }
                else {
                    onFreeVersion();
                }
            }
        }
    };


    private void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter(ADS);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(adsReceiver, intentFilter);
    }


    private void hideAds() {
        LinearLayout adviewLayout = findViewById(R.id.adviewLayout);
        if (adviewLayout.getChildCount() != 0) {
            adviewLayout.removeView(mAdView);
        }
    }

    private void showAds() {
        // DYNAMICALLY CREATE AD START
        LinearLayout adviewLayout = findViewById(R.id.adviewLayout);
        // Create an ad.
        if (mAdView == null) {
            mAdView = new AdView(getActivity().getApplicationContext());
            mAdView.setAdSize(AdSize.BANNER);
            mAdView.setAdUnitId(getResources().getString(R.string.banner_ad_unit_id));
            // DYNAMICALLY CREATE AD END
            AdRequest adRequest = new AdRequest.Builder().build();
            // Start loading the ad in the background.
            mAdView.loadAd(adRequest);
            // Add the AdView to the view hierarchy. The view will have no size until the ad is
            // loaded.
            adviewLayout.addView(mAdView);
        }
        else {
            ((LinearLayout) mAdView.getParent()).removeAllViews();
            adviewLayout.addView(mAdView);
            // Reload Ad if necessary.  Loaded ads are lost when the activity is paused.
            if (!mAdView.isLoading()) {
                AdRequest adRequest = new AdRequest.Builder().build();
                // Start loading the ad in the background.
                mAdView.loadAd(adRequest);
            }
        }
        // DYNAMICALLY CREATE AD END
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void initializeListeners() {

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                removeScrolledPos(currentDir);
                refreshList();
            }
        });

        setupFab();

        fileListAdapter.setOnItemClickListener(new FileListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (actionMode != null && !isPasteVisible) {
                    itemClickActionMode(position, false);
                }
                else {
                    handleItemClick(position);
                }
            }
        });
        fileListAdapter.setOnItemLongClickListener(new FileListAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position) {
                Logger.log(TAG, "On long click" + isDragStarted);
                if (position >= fileInfoList.size() || position == RecyclerView.NO_POSITION) {
                    return;
                }

                if (!isZipViewer && !isPasteVisible) {
                    itemClickActionMode(position, true);
                    mLongPressedTime = System.currentTimeMillis();

                    if (actionMode != null && fileListAdapter.getSelectedCount() >= 1) {
                        mSwipeRefreshLayout.setEnabled(false);
                        mItemView = view;
                        isDragStarted = true;
                    }
                }
            }
        });


        fileList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        if (shouldStopAnimation) {
                            stopAnimation();
                            shouldStopAnimation = false;
                        }
                        break;
                }
            }
        });

        fileList.setOnTouchListener(touchListener);
    }

    private void setupFab() {
        fabCreateFile.setOnClickListener(this);
        fabCreateFolder.setOnClickListener(this);
        fabOperation.setOnClickListener(this);

        fabCreateMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu
                .OnFloatingActionsMenuUpdateListener() {

            @Override
            public void onMenuExpanded() {
                frameLayoutFab.getBackground().setAlpha(240);
                frameLayoutFab.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        fabCreateMenu.collapse();
                        return true;
                    }
                });
            }

            @Override
            public void onMenuCollapsed() {
                frameLayoutFab.getBackground().setAlpha(0);
                frameLayoutFab.setOnTouchListener(null);
            }
        });
    }

    private View.OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int event = motionEvent.getActionMasked();

            if (shouldStopAnimation) {
                stopAnimation();
                shouldStopAnimation = false;
            }

            if (isDragStarted && event == MotionEvent.ACTION_UP) {
                isDragStarted = false;
            }
            else if (isDragStarted && event == MotionEvent.ACTION_MOVE && mLongPressedTime !=
                    0) {
                long timeElapsed = System.currentTimeMillis() - mLongPressedTime;
//                    Logger.log(TAG, "On item touch time Elapsed" + timeElapsed);

                if (timeElapsed > 1000) {
                    mLongPressedTime = 0;
                    isDragStarted = false;
                    Logger.log(TAG, "On touch drag path size=" + draggedData.size());
                    if (draggedData.size() > 0) {
                        Intent intent = new Intent();
                        intent.putParcelableArrayListExtra(FileConstants.KEY_PATH, draggedData);
                        ClipData data = ClipData.newIntent("", intent);
                        int count = fileListAdapter.getSelectedCount();
                        View.DragShadowBuilder shadowBuilder = dragHelper.getDragShadowBuilder(mItemView, count);
                        if (Utils.isAtleastNougat()) {
                            view.startDragAndDrop(data, shadowBuilder, draggedData, 0);
                        }
                        else {
                            view.startDrag(data, shadowBuilder, draggedData, 0);
                        }
                    }
                }
            }
            return false;
        }

    };



    private void handleItemClick(int position) {
        if (position >= fileInfoList.size() || position == RecyclerView.NO_POSITION) {
            return;
        }

        switch (category) {
            case AUDIO:
            case VIDEO:
            case IMAGE:
            case DOCS:
                viewFile(getContext(), fileInfoList.get(position).getFilePath(),
                        fileInfoList.get(position).getExtension());
                break;
            case FILES:
            case DOWNLOADS:
            case COMPRESSED:
            case FAVORITES:
            case PDF:
            case APPS:
            case LARGE_FILES:
                genericFileItemClick(position);
                break;
        }
    }

    private void genericFileItemClick(int position) {
        if (fileInfoList.get(position).isDirectory()) {
            onDirectoryClicked(position);
        }
        else {
            onFileClicked(position);
        }
    }

    private void onDirectoryClicked(int position) {
        calculateScroll();
        if (isZipMode()) {
            zipViewer.onDirectoryClicked(position);
        }
        else {
            String path = fileInfoList.get(position).getFilePath();
            category = FILES;
            reloadList(path, category);
        }
    }

    private void onFileClicked(int position) {
        String filePath = fileInfoList.get(position).getFilePath();
        String extension = fileInfoList.get(position).getExtension();

        if (!isZipMode() && isZipViewable(filePath)) {
            openZipViewer(filePath);
        }
        else {
            if (isZipMode()) {
                zipViewer.onFileClicked(position);
            }
            else {
                viewFile(getContext(), filePath, extension);
            }
        }
    }


    private boolean isZipViewable(String filePath) {
        return filePath.toLowerCase().endsWith(".zip") ||
                filePath.toLowerCase().endsWith(".jar") ||
                filePath.toLowerCase().endsWith(".rar");
    }


    public void setPremium() {
        isPremium = true;
        hideAds();
    }


    public void onPause() {
        pauseAds();
        Logger.log(TAG, "OnPause");
        if (!mInstanceStateExists) {
            getActivity().unregisterReceiver(mReloadListReceiver);
        }
    }

    public void onResume() {
        if (!mInstanceStateExists) {
            IntentFilter intentFilter = new IntentFilter(ACTION_RELOAD_LIST);
            intentFilter.addAction(ACTION_OP_REFRESH);
            getActivity().registerReceiver(mReloadListReceiver, intentFilter);
        }
        resumeAds();
    }


    public void handleActivityResult(int requestCode, int resultCode, Intent intent) {
        Logger.log(TAG, "OnActivityREsult==" + resultCode);
        switch (requestCode) {
            case DIALOG_FRAGMENT:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    mSelectedPath = intent.getStringExtra("PATH");
                    if (buttonPathSelect != null) {
                        buttonPathSelect.setText(mSelectedPath);
                    }
                }
                break;
            case SAF_REQUEST:
                String uriString = preferences.getString(FileConstants.SAF_URI, null);

                Uri oldUri = uriString != null ? Uri.parse(uriString) : null;

                if (resultCode == Activity.RESULT_OK) {
                    Uri treeUri = intent.getData();
                    Log.d(TAG, "tree uri=" + treeUri + " old uri=" + oldUri);
                    // Get Uri from Storage Access Framework.
                    // Persist URI - this is required for verification of writability.
                    if (treeUri != null) {
                        preferences.edit().putString(FileConstants.SAF_URI, treeUri.toString())
                                .apply();
                        int takeFlags = intent.getFlags();
                        takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent
                                .FLAG_GRANT_WRITE_URI_PERMISSION);
                        getActivity().getContentResolver().takePersistableUriPermission(treeUri,
                                takeFlags);
                        fileOpHelper.handleSAFOpResult(mIsRootMode);
                    }
                }
                // If not confirmed SAF, or if still not writable, then revert settings.
                else {
                    if (oldUri != null) {
                        preferences.edit().putString(FileConstants.SAF_URI, oldUri.toString())
                                .apply();
                    }

                    Toast.makeText(getContext(), getResources().getString(R.string.access_denied_external),
                            Toast.LENGTH_LONG).show();
                }
        }
    }

    private void addItemDecoration() {

        switch (viewMode) {
            case ViewMode.LIST:
                if (mDividerItemDecoration == null) {
                    mDividerItemDecoration = new DividerItemDecoration(getActivity(), currentTheme);
                }
                else {
                    fileList.removeItemDecoration(mDividerItemDecoration);
                }
                fileList.addItemDecoration(mDividerItemDecoration);
                break;
            case ViewMode.GRID:
                if (mGridItemDecoration == null) {
                    mGridItemDecoration = new GridItemDecoration(getContext(), currentTheme,
                            gridCols);
                }
                else {
                    fileList.removeItemDecoration(mGridItemDecoration);
                }
                fileList.addItemDecoration(mGridItemDecoration);
                break;
        }
    }


    private ZipViewer zipViewer;

    public void openZipViewer(String path) {
        calculateScroll();
        isZipViewer = true;
        zipViewer = new ZipViewer(this, path);
        refreshList();
    }

    public void endZipMode() {
        isZipViewer = false;
        zipViewer = null;
    }


    private final BroadcastReceiver mReloadListReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_RELOAD_LIST)) {
                calculateScroll();
                String path = intent.getStringExtra(KEY_FILEPATH);
                Logger.log(TAG, "New zip PAth=" + path);
                if (path != null) {
                    scanFile(getActivity().getApplicationContext(), path);
                }
                refreshList();
            }
            else if (action.equals(ACTION_OP_REFRESH)) {
                Bundle bundle = intent.getExtras();
                Operations operation = (Operations) bundle.getSerializable(KEY_OPERATION);
                menuControls.onOperationResult(intent, operation);
            }
        }
    };

    public boolean onBackPressed() {

        if (menuControls.isSearch()) {
          return true;
        }
        else if (isZipMode()) {
            zipViewer.onBackPressed();
        }
        else {
            return backOperation();
        }
        return false;
    }

    private boolean backOperation() {

        if (checkIfBackStackExists()) {
            removeScrolledPos(currentDir);
            backStackInfo.removeEntryAtIndex(backStackInfo.getBackStack().size() - 1);

            String currentDir = backStackInfo.getDirAtPosition(backStackInfo.getBackStack().size
                    () - 1);
            Category currentCategory = backStackInfo.getCategoryAtPosition(backStackInfo
                    .getBackStack().size() - 1);
            if (checkIfFileCategory(currentCategory)) {
//                navigationInfo.setInitialDir();
                category = currentCategory;
                if (shouldShowPathNavigation()) {
                    navigationInfo.setInitialDir(currentDir);
                    navigationInfo.setNavDirectory(currentDir, isHomeScreenEnabled,
                            currentCategory);
                }
                else {
                    navigationInfo.addHomeNavButton(isHomeScreenEnabled, currentCategory);
                }
            }
            else {
                hideFab();
            }
            this.currentDir = currentDir;
            refreshList();
            menuControls.setTitleForCategory(currentCategory);
            if (currentCategory.equals(FILES)) {
                showFab();
            }

            return false;
        }
        else {
            removeFileFragment();
  /*          if (!isHomeScreenEnabled) {
                getActivity().finish();
            }*/
            return true;
        }
    }

    private boolean checkIfBackStackExists() {
        int backStackSize = backStackInfo.getBackStack().size();
        Logger.log(TAG, "checkIfBackStackExists --size=" + backStackSize);


        if (backStackSize == 1) {
            backStackInfo.clearBackStack();
            return false;
        }
        else if (backStackSize > 1) {
            return true;
        }
        return false;
    }


    private boolean shouldShowPathNavigation() {
        return category.equals(FILES) || category.equals(DOWNLOADS);

    }


    public boolean isFabExpanded() {
        return fabCreateMenu.isExpanded();
    }

    public void collapseFab() {
        fabCreateMenu.collapse();
    }


    /**
     * Called from {@link #onBackPressed()} . Does the following:
     * 1. If homescreen enabled, returns to home screen
     * 2. If homescreen disabled, exits the app
     */
    private void removeFileFragment() {

        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id
                .main_container);
        Fragment dualFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id
                .frame_container_dual);

        backStackInfo.clearBackStack();
        Logger.log(TAG, "RemoveFragmentFromBackStack--frag=" + fragment);

        if (dualFragment != null) {
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim
                    .enter_from_right, R.anim
                    .exit_to_left);
            ft.remove(dualFragment);
            ft.commitAllowingStateLoss();
        }
    }

    private void resumeAds() {
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    private void pauseAds() {
        if (mAdView != null) {
            mAdView.pause();
        }
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick: ");
        switch (view.getId()) {
            case R.id.fabCreateFile:
                new Dialogs().createFileDialog(this, mIsRootMode, currentDir);
                fabCreateMenu.collapse();
                break;
            case R.id.fabCreateFolder:
                new Dialogs().createDirDialog(this, mIsRootMode, currentDir);
                fabCreateMenu.collapse();
                break;

        }
    }


    private void toggleDragData(FileInfo fileInfo) {
        if (draggedData.contains(fileInfo)) {
            draggedData.remove(fileInfo);
        }
        else {
            draggedData.add(fileInfo);
        }
    }

    private void itemClickActionMode(int position, boolean isLongPress) {
        fileListAdapter.toggleSelection(position, isLongPress);

        boolean hasCheckedItems = fileListAdapter.getSelectedCount() > 0;
        ActionMode actionMode = getActivity().getActionMode();
        if (hasCheckedItems && actionMode == null) {
            // there are some selected items, start the actionMode
//            aceActivity.updateDrawerIcon(true);

            startActionMode();
        }
        else if (!hasCheckedItems && actionMode != null) {
            // there no selected items, finish the actionMode
//            mActionModeCallback.endActionMode();
            actionMode.finish();
        }
        if (getActionMode() != null) {
            FileInfo fileInfo = fileInfoList.get(position);
            toggleDragData(fileInfo);
            SparseBooleanArray checkedItemPos = fileListAdapter.getSelectedItemPositions();
            setSelectedItemPos(checkedItemPos);
            this.actionMode.setTitle(String.valueOf(fileListAdapter
                    .getSelectedCount()) + " selected");
        }
    }

    private void calculateScroll() {
        View vi = fileList.getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        int position;
        if (viewMode == ViewMode.LIST) {
            position = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        }
        else {
            position = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
        }
        Bundle bundle = new Bundle();
        bundle.putInt(FileConstants.KEY_POSITION, position);
        bundle.putInt(FileConstants.KEY_OFFSET, top);

        putScrolledPosition(currentDir, bundle);
    }

    private void setSelectedItemPos(SparseBooleanArray selectedItemPos) {
        mSelectedItemPositions = selectedItemPos;
        menuControls.setupMenuVisibility(selectedItemPos);
    }


    // 1 Extra for Footer since {#getItemCount has footer
    // TODO Remove this 1 when if footer removed in future
    private void toggleSelectAll(boolean selectAll) {
        fileListAdapter.clearSelection();
        for (int i = 0; i < fileListAdapter.getItemCount() - 1; i++) {
            fileListAdapter.toggleSelectAll(i, selectAll);
        }
        SparseBooleanArray checkedItemPos = fileListAdapter.getSelectedItemPositions();
        setSelectedItemPos(checkedItemPos);
        actionMode.setTitle(String.valueOf(fileListAdapter.getSelectedCount()) + " " + getResources().getString
                (R.string.selected));
        fileListAdapter.notifyDataSetChanged();
    }

    private void clearSelection() {
        fileListAdapter.removeSelection();
    }


    public void reloadList(String path, Category category) {
        currentDir = path;
        this.category = category;
        if (shouldShowPathNavigation()) {
            navigationInfo.setNavDirectory(currentDir, isHomeScreenEnabled, category);
        }
        else {
            navigationInfo.addHomeNavButton(isHomeScreenEnabled, category);
        }
        backStackInfo.addToBackStack(path, category);
        refreshList();
    }


    private void stopAnimation() {
        if (!fileListAdapter.mStopAnimation) {
            for (int i = 0; i < fileList.getChildCount(); i++) {
                View view = fileList.getChildAt(i);
                if (view != null) {
                    view.clearAnimation();
                }
            }
        }
        fileListAdapter.mStopAnimation = true;
    }


    public boolean isZipMode() {
        return isZipViewer;
    }

    public void resetZipMode() {
        isZipViewer = false;
    }


    public void onReset() {
        // Clear the data in the adapter.
        Log.d(TAG, "onLoaderReset: ");
        fileListAdapter.updateAdapter(null);
    }

    private void onDataLoaded(ArrayList<FileInfo> data) {
        mSwipeRefreshLayout.setRefreshing(false);

        if (data != null) {

            Log.d(TAG, "on onLoadFinished--" + data.size());

            shouldStopAnimation = true;
            fileInfoList = data;
            fileListAdapter.setCategory(category);
            fileList.setAdapter(fileListAdapter);
            fileListAdapter.updateAdapter(fileInfoList);

            addItemDecoration();

            if (!data.isEmpty()) {

                Log.d("TEST", "on onLoadFinished scrollpos--" + scrollPosition.entrySet());
                getScrolledPosition();
                fileList.stopScroll();
                mTextEmpty.setVisibility(View.GONE);
            }
            else {
                mTextEmpty.setVisibility(View.VISIBLE);
            }
        }
    }

    List<FileInfo> getFileList() {
        return fileInfoList;
    }

    private void getScrolledPosition() {
        if (currentDir != null && scrollPosition.containsKey(currentDir)) {
            Bundle b = scrollPosition.get(currentDir);
            if (viewMode == ViewMode.LIST) {
                ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(b.getInt
                        (FileConstants
                                .KEY_POSITION), b.getInt(FileConstants.KEY_OFFSET));
            }
            else {
                ((GridLayoutManager) layoutManager).scrollToPositionWithOffset(b.getInt
                        (FileConstants
                                .KEY_POSITION), b.getInt(FileConstants.KEY_OFFSET));
            }
        }
    }

    private void putScrolledPosition(String path, Bundle position) {
        Log.d(TAG, "putScrolledPosition: " + path);
        scrollPosition.put(path, position);
    }

    private void removeScrolledPos(String path) {
        if (path == null) {
            return;
        }
        Log.d(TAG, "removeScrolledPos: " + path);
        scrollPosition.remove(path);
    }

    public void onZipContentsLoaded(ArrayList<FileInfo> fileInfoList) {
        onDataLoaded(fileInfoList);
    }

    void switchView() {
        fileListAdapter = null;
        fileList.setHasFixedSize(true);

        if (viewMode == ViewMode.LIST) {
            layoutManager = new CustomLayoutManager(getActivity());
            fileList.setLayoutManager(layoutManager);

        }
        else {
            refreshSpan();
        }

        shouldStopAnimation = true;

        fileListAdapter = new FileListAdapter(getContext(), fileInfoList, category, viewMode);
        fileList.setAdapter(fileListAdapter);
        if (viewMode == ViewMode.LIST) {
            if (mGridItemDecoration != null) {
                fileList.removeItemDecoration(mGridItemDecoration);
            }
            if (mDividerItemDecoration == null) {
                mDividerItemDecoration = new DividerItemDecoration(getActivity(), currentTheme);
            }
            mDividerItemDecoration.setOrientation();
            fileList.addItemDecoration(mDividerItemDecoration);
        }
        else {
            if (mDividerItemDecoration != null) {
                fileList.removeItemDecoration(mDividerItemDecoration);
            }
            addItemDecoration();
        }

        initializeListeners();

    }

    /**
     * Show dual pane in Landscape mode
     */
    public void showDualPane() {

        // For Files category only, show dual pane
        mIsDualModeEnabled = true;
        refreshSpan();
    }

    private void getPreferences() {
        Bundle bundle = bridge.getUserPrefs();
        gridCols = bundle.getInt(FileConstants.KEY_GRID_COLUMNS, 0);
        isHomeScreenEnabled = bundle.getBoolean(FileConstants.PREFS_HOMESCREEN, true);
        viewMode = bundle.getInt(FileConstants.PREFS_VIEW_MODE, ViewMode.LIST);
    }

    private void getArgs() {
        if (getArguments() != null) {
            currentDir = getArguments().getString(FileConstants.KEY_PATH);
            category = (Category) getArguments().getSerializable(FileConstants.KEY_CATEGORY);
            isZipViewer = getArguments().getBoolean(FileConstants.KEY_ZIP, false);
            mIsDualModeEnabled = getArguments().getBoolean(FileConstants.KEY_DUAL_ENABLED, false);

            if (checkIfLibraryCategory(category)) {
                hideFab();
            }
            else {
                showFab();
            }
            navigationInfo.showNavigationView();
            if (shouldShowPathNavigation()) {
                navigationInfo.setInitialDir(currentDir);
            }
            mLastSinglePaneDir = currentDir;
        }
    }

    private Bundle getArguments() {
        return fragment.getArguments();
    }

    private void createDualFrag() {
        if (mIsDualModeEnabled && fragment instanceof FileList) {
//            aceActivity.toggleDualPaneVisibility(true);
            showDualPane();
//            aceActivity.createDualFragment();
        }
    }

    private boolean checkIfLibraryCategory(Category category) {
        return !category.equals(FILES);
    }

    public void showFab() {
        frameLayoutFab.setVisibility(View.VISIBLE);
    }

    public void hideFab() {
        frameLayoutFab.setVisibility(View.GONE);
    }



    private void setupList() {
        fileList.setHasFixedSize(true);
        if (viewMode == ViewMode.LIST) {
            layoutManager = new CustomLayoutManager(getActivity());
            fileList.setLayoutManager(layoutManager);
        }
        else {
            refreshSpan();
        }
        fileList.setItemAnimator(new DefaultItemAnimator());
        fileListAdapter = new FileListAdapter(getContext(), fileInfoList,
                category, viewMode);
    }

    public void refreshList() {
        Log.d(TAG, "onCreateLoader: ");
        fileInfoList = new ArrayList<>();
        if (fileListAdapter != null) {
            fileListAdapter.clearList();
        }
        mSwipeRefreshLayout.setRefreshing(true);
        if (isZipMode()) {
            zipViewer.onCreateLoader(null);
        }
        else {
            bridge.loadData(currentDir, category, false);
        }
    }


    @Override
    public void onPreExecute() {
        mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onPostExecute() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onProgressUpdate(FileInfo val) {
        addSearchResult(val);
    }

    @Override
    public void onCancelled() {
        mSwipeRefreshLayout.setRefreshing(false);

    }

    private boolean isInitialSearch;

    private void addSearchResult(FileInfo fileInfo) {
        // initially clearing the array for new result set
        if (!isInitialSearch) {
            fileInfoList.clear();
            fileListAdapter.clear();

        }
        isInitialSearch = true;
        fileInfoList.add(fileInfo);
        fileListAdapter.updateSearchResult(fileInfo);
        stopAnimation();
//        aceActivity.addToBackStack(currentDir, mCategory);

    }

    private void startActionMode() {

        hideFab();
        menuControls.startActionMode();
    }

    private ActionMode getActionMode() {
        return actionMode;
    }


    public FileOpsHelper getFileOpHelper() {
        return fileOpHelper;
    }



    @Override
    public void onHomeClicked() {
        endActionMode();
        removeFileFragment();
        getActivity().onBackPressed();
    }



    @Override
    public void onNavButtonClicked(String dir) {

        if (isZipMode()) {
            zipViewer.onBackPressed();
        }
        else {
            currentDir = dir;
            int position = 0;
            ArrayList<BackStackModel> backStack = backStackInfo.getBackStack();
            for (int i = 0; i < backStack.size(); i++) {
                if (currentDir.equals(backStack.get(i).getFilePath())) {
                    position = i;
                    break;
                }
            }
            for (int j = backStack.size() - 1; j > position; j--) {
                backStackInfo.removeEntryAtIndex(j);
            }

            refreshList();
            navigationInfo.setNavDirectory(currentDir, isHomeScreenEnabled, FILES);
        }
    }


    private int getThemeStyle() {
        switch (currentTheme) {
            case DARK:
                return R.style.BaseDarkTheme_Dark;
            case LIGHT:
                return R.style.BaseLightTheme_Light;
        }
        return R.style.BaseDarkTheme_Dark;

    }


    private void updateFavouritesGroup(ArrayList<FileInfo> fileInfoList) {
        ArrayList<FavInfo> favInfoArrayList = new ArrayList<>();
        for (int i = 0; i < fileInfoList.size(); i++) {
            FileInfo info = fileInfoList.get(i);
            String name = info.getFileName();
            String path = info.getFilePath();
            FavInfo favInfo = new FavInfo();
            favInfo.setFileName(name);
            favInfo.setFilePath(path);
            SharedPreferenceWrapper sharedPreferenceWrapper = new SharedPreferenceWrapper();
            sharedPreferenceWrapper.addFavorite(getActivity(), favInfo);
            favInfoArrayList.add(favInfo);
        }

//        aceActivity.updateFavourites(favInfoArrayList);
    }

    private void removeFavorite(ArrayList<FileInfo> fileInfoList) {
        ArrayList<FavInfo> favInfoArrayList = new ArrayList<>();
        for (int i = 0; i < fileInfoList.size(); i++) {
            FileInfo info = fileInfoList.get(i);
            String name = info.getFileName();
            String path = info.getFilePath();
            FavInfo favInfo = new FavInfo();
            favInfo.setFileName(name);
            favInfo.setFilePath(path);
            SharedPreferenceWrapper sharedPreferenceWrapper = new SharedPreferenceWrapper();
            sharedPreferenceWrapper.removeFavorite(getActivity(), favInfo);
            favInfoArrayList.add(favInfo);
        }
        refreshList();
//        aceActivity.removeFavourites(favInfoArrayList);
    }






/*    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(this.getClass().getSimpleName(), "onCreateOptionsMenu" + "Fragment=");
*//*
//        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_base, menu);
        mSearchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mPasteItem = menu.findItem(R.id.action_paste);
        mPasteItem.setVisible(mIsPasteItemVisible);
        viewMode = sharedPreferenceWrapper.getViewMode(getActivity());
        mViewItem = menu.findItem(R.id.action_view);
        updateMenuTitle();
        setupSearchView();*//*
    }*/


    public void refreshSpan() {
        if (viewMode == ViewMode.GRID) {
            if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT || !mIsDualModeEnabled ||
                    checkIfLibraryCategory(category)) {
                gridCols = getResources().getInteger(R.integer.grid_columns);
            }
            else {
                gridCols = getResources().getInteger(R.integer.grid_columns_dual);
            }
            Log.d(TAG, "Refresh span--columns=" + gridCols + "category=" + category + " dual " +
                    "mode=" +
                    mIsDualModeEnabled);

            layoutManager = new CustomGridLayoutManager(getActivity(), gridCols);
            fileList.setLayoutManager(layoutManager);
        }
    }


    private boolean isFilesCategory() {
        return category.equals(FILES);
    }

    public void endActionMode() {
        isInSelectionMode = false;
        if (actionMode != null) {
            actionMode.finish();
        }
        actionMode = null;
        menuControls.hideBottomToolbar();
        mSelectedItemPositions = new SparseBooleanArray();
        mSwipeRefreshLayout.setEnabled(true);
        draggedData.clear();
    }

    public boolean isInSelectionMode() {
        return isInSelectionMode;
    }


    @Override
    public void onConfigurationChanged(final Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        if (mCurrentOrientation != newConfig.orientation) {
            mCurrentOrientation = newConfig.orientation;
            refreshSpan();
        }
        Logger.log(TAG, "onConfigurationChanged " + newConfig.orientation);

    }


    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    public void onViewDestroyed() {
        fileList.stopScroll();
        if (!mInstanceStateExists) {
            preferences.edit().putInt(FileConstants.KEY_GRID_COLUMNS, gridCols).apply();
            sharedPreferenceWrapper.savePrefs(getActivity(), viewMode);
        }
        menuControls.removeSearchTask();

        if (fileListAdapter != null) {
            fileListAdapter.onDetach();
        }
    }

    private void clearSelectedPos() {
        mSelectedItemPositions = new SparseBooleanArray();
    }


    public void setDrawerListener(DrawerListener drawerListener) {
        this.drawerListener = drawerListener;
    }


}
