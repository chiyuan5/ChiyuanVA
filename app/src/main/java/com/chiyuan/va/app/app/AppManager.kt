package com.chiyuan.va.app.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chiyuan.va.app.view.main.ChiyuanVALoader


object AppManager {
    private const val TAG = "CVA_AppManager"

    @JvmStatic
    val mChiyuanVALoader by lazy {
        try {
            ChiyuanVALoader()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ChiyuanVALoader: ${e.message}")

            ChiyuanVALoader() 
        }
    }

    @JvmStatic
    val mChiyuanVACore by lazy {
        try {
            mChiyuanVALoader.getChiyuanVACore()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ChiyuanVACore: ${e.message}")
            throw e 
        }
    }

    @JvmStatic
    val mRemarkSharedPreferences: SharedPreferences by lazy {
        try {
            App.getContext().getSharedPreferences("UserRemark", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SharedPreferences: ${e.message}")
            throw e 
        }
    }

    fun doAttachBaseContext(context: Context) {
        try {
            mChiyuanVALoader.attachBaseContext(context)
            mChiyuanVALoader.addLifecycleCallback()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doAttachBaseContext: ${e.message}")
            
        }
    }

    fun doOnCreate(context: Context) {
        try {
            mChiyuanVALoader.doOnCreate(context)
            initThirdService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in doOnCreate: ${e.message}")
            
        }
    }

    private fun initThirdService(context: Context) {
        try {
            
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in initThirdService: ${e.message}")
        }
    }
}
