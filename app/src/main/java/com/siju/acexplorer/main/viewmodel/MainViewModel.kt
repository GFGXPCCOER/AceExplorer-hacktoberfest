package com.siju.acexplorer.main.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.siju.acexplorer.AceApplication
import com.siju.acexplorer.billing.repository.BillingRepository
import com.siju.acexplorer.billing.repository.localdb.Premium
import com.siju.acexplorer.main.model.MainModelImpl
import com.siju.acexplorer.main.model.StorageItem
import com.siju.acexplorer.main.model.StorageUtils
import com.siju.acexplorer.permission.PermissionHelper
import com.siju.acexplorer.preferences.PreferenceConstants
import com.siju.acexplorer.theme.Theme

enum class Pane {
    SINGLE,
    DUAL
}
class MainViewModel : ViewModel() {

    var navigateToSearch = MutableLiveData<Boolean>()
    var isDualPaneInFocus = false
    private set

    private val billingRepository = BillingRepository.getInstance(AceApplication.appContext)
    val premiumLiveData: LiveData<Premium>
    private val mainModel = MainModelImpl()
    private lateinit var permissionHelper: PermissionHelper
    lateinit var permissionStatus: LiveData<PermissionHelper.PermissionState>
    val theme: LiveData<Theme>
    private var storageList: ArrayList<StorageItem>? = null
    val dualMode : LiveData<Boolean>
    val sortMode: LiveData<Int>
    private val _homeClicked = MutableLiveData<Boolean>()

    val homeClicked: LiveData<Boolean>
        get() = _homeClicked

    private val _storageScreenReady = MutableLiveData<Boolean>()
    val storageScreenReady : LiveData<Boolean>
    get() = _storageScreenReady

    private val _refreshGridColumns = MutableLiveData<Pair<Pane, Boolean>>()

    val refreshGridCols : LiveData<Pair<Pane, Boolean>>
    get() = _refreshGridColumns

    private val _reloadPane = MutableLiveData<Pair<Pane, Boolean>>()

    val reloadPane : LiveData<Pair<Pane, Boolean>>
    get() = _reloadPane

    init {
        billingRepository.startDataSourceConnections()
        premiumLiveData = billingRepository.premiumLiveData
        theme = mainModel.theme
        dualMode = mainModel.dualMode
        sortMode = mainModel.sortMode
    }

    override fun onCleared() {
        super.onCleared()
        billingRepository.endDataSourceConnections()
    }

    fun buyPremiumVersion(activity: Activity) {
        billingRepository.purchaseFullVersion(activity)
    }

    fun setPermissionHelper(permissionHelper: PermissionHelper) {
        this.permissionHelper = permissionHelper
        permissionStatus = permissionHelper.permissionStatus
        permissionHelper.checkPermissions()
    }

    fun isPremiumVersion() = premiumLiveData.value?.entitled == true

    fun isFreeVersion() = premiumLiveData.value?.entitled == false

    fun isDualPaneEnabled() = dualMode.value == true

    fun getSortMode() : Int {
        return sortMode.value ?: PreferenceConstants.DEFAULT_VALUE_SORT_MODE
    }

    fun onPermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionHelper.onPermissionResult(requestCode, permissions, grantResults)
    }

    fun onResume() {
        permissionHelper.onForeground()
    }

    fun requestPermissions() {
        permissionHelper.requestPermission()
    }

    fun showPermissionRationale() {
        permissionHelper.showRationale()
    }

    fun setStorageList(storageList: ArrayList<StorageItem>) {
        this.storageList = storageList
    }

    fun getExternalSdList() : ArrayList<String> {
        val extSdList = arrayListOf<String>()
        storageList?.let {
            for(storage in it) {
                if (storage.storageType == StorageUtils.StorageType.EXTERNAL) {
                     extSdList.add(storage.path)
                }
            }
        }
        return extSdList
    }

    fun onHomeClicked() {
        _homeClicked.value = true
    }

    fun setHomeClickedFalse() {
        _homeClicked.value = false
    }

    fun setNavigatedToSearch() {
        navigateToSearch.value = false
    }

    fun setStorageReady() {
       _storageScreenReady.value = true
    }

    fun setStorageNotReady() {
        _storageScreenReady.value = false
    }

    fun refreshLayout(pane: Pane) {
        Log.d(this.javaClass.simpleName, "refreshLayout:$pane")
        _refreshGridColumns.value = Pair(pane, true)
    }

    fun setRefreshDone(pane: Pane) {
        _refreshGridColumns.value = Pair(pane, false)
    }

    fun setReloadPane(pane: Pane, reload : Boolean) {
        Log.d(this.javaClass.simpleName, "setReloadPane:$pane, reload:$reload")
        _reloadPane.value = Pair(pane, reload)
    }

    fun setPaneFocus(isDualPaneInFocus: Boolean) {
        this.isDualPaneInFocus = isDualPaneInFocus
    }

}