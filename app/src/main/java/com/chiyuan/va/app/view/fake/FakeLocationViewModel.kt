package com.chiyuan.va.app.view.fake

import androidx.lifecycle.MutableLiveData
import com.chiyuan.va.entity.location.BLocation
import com.chiyuan.va.app.bean.FakeLocationBean
import com.chiyuan.va.app.data.FakeLocationRepository
import com.chiyuan.va.app.view.base.BaseViewModel


class FakeLocationViewModel(private val mRepo: FakeLocationRepository) : BaseViewModel() {

    val appsLiveData = MutableLiveData<List<FakeLocationBean>>()


    fun getInstallAppList(userID: Int) {
        launchOnUI {
            mRepo.getInstalledAppList(userID, appsLiveData)
        }
    }

    fun setPattern(userId: Int, pkg: String, pattern: Int) {
        launchOnUI {
            mRepo.setPattern(userId, pkg, pattern)
        }
    }

    fun setLocation(userId: Int, pkg: String, location: BLocation) {
        launchOnUI {
            mRepo.setLocation(userId, pkg, location)
        }
    }

}