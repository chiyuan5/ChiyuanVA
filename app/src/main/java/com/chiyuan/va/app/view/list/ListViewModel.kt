package com.chiyuan.va.app.view.list

import androidx.lifecycle.MutableLiveData
import com.chiyuan.va.app.bean.InstalledAppBean
import com.chiyuan.va.app.data.AppsRepository
import com.chiyuan.va.app.view.base.BaseViewModel


class ListViewModel(private val repo: AppsRepository) : BaseViewModel() {

    val appsLiveData = MutableLiveData<List<InstalledAppBean>>()

    val loadingLiveData = MutableLiveData<Boolean>()

    fun previewInstalledList() {
        launchOnUI { repo.previewInstallList() }
    }

    fun getInstallAppList(userID: Int) {
        launchOnUI { repo.getInstalledAppList(userID, loadingLiveData, appsLiveData) }
    }
}
