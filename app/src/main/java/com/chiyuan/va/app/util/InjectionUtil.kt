package com.chiyuan.va.app.util

import com.chiyuan.va.app.data.AppsRepository
import com.chiyuan.va.app.data.FakeLocationRepository
import com.chiyuan.va.app.data.GmsRepository

import com.chiyuan.va.app.view.apps.AppsFactory
import com.chiyuan.va.app.view.fake.FakeLocationFactory
import com.chiyuan.va.app.view.gms.GmsFactory
import com.chiyuan.va.app.view.list.ListFactory



object InjectionUtil {

    private val appsRepository = AppsRepository()



    private val gmsRepository = GmsRepository()

    private val fakeLocationRepository = FakeLocationRepository()

    fun getAppsFactory() : AppsFactory {
        return AppsFactory(appsRepository)
    }

    fun getListFactory(): ListFactory {
        return ListFactory(appsRepository)
    }


    fun getGmsFactory():GmsFactory{
        return GmsFactory(gmsRepository)
    }

    fun getFakeLocationFactory():FakeLocationFactory{
        return FakeLocationFactory(fakeLocationRepository)
    }
}