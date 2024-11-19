package com.omelan.cofi

import android.app.Application
import android.os.Build
import com.baidu.location.LocationClient
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.CoordType
import com.kieronquinn.monetcompat.core.MonetCompat

class CofiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 百度地图隐私政策设置
        SDKInitializer.setAgreePrivacy(this, true)
        LocationClient.setAgreePrivacy(true)
        
        // 初始化地图SDK
        SDKInitializer.initialize(this)
        SDKInitializer.setCoordType(CoordType.BD09LL)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            MonetCompat.enablePaletteCompat()
        }
    }
}
