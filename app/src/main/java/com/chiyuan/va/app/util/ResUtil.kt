package com.chiyuan.va.app.util

import androidx.annotation.StringRes
import com.chiyuan.va.app.app.App


fun getString(@StringRes id:Int,vararg arg:String):String{
    if(arg.isEmpty()){
        return App.getContext().getString(id)
    }
    return App.getContext().getString(id,*arg)
}

