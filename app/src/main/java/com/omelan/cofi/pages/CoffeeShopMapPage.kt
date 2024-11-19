package com.omelan.cofi.pages

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.poi.*
import com.omelan.cofi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeShopMapPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationClient by remember { mutableStateOf<LocationClient?>(null) }
    var poiSearch by remember { mutableStateOf<PoiSearch?>(null) }
    var hasLocationPermission by remember { 
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var markerIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var lastSearchTime = 0L
    var currentShownMarker by remember { mutableStateOf<Marker?>(null) }

    val searchNearbyShops = { location: LatLng ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSearchTime > 3000) {
            val nearbySearchOption = PoiNearbySearchOption()
                .location(location)
                .keyword("咖啡")
                .radius(2000)
                .pageCapacity(50)
            
            poiSearch?.searchNearby(nearbySearchOption)
            Log.d("CoffeeShopMap", "开始搜附近咖啡店")
            lastSearchTime = currentTime
        }
    }

    val initPoiSearch = remember {
        {
            poiSearch = PoiSearch.newInstance().apply {
                setOnGetPoiSearchResultListener(object : OnGetPoiSearchResultListener {
                    override fun onGetPoiResult(result: PoiResult?) {
                        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                            Log.e("CoffeeShopMap", "POI搜索失败: ${result?.error}")
                            return
                        }
                        
                        Log.d("CoffeeShopMap", "搜索成功，找到 ${result.allPoi.size} 个结果")
                        result.allPoi.forEach { poi ->
                            Log.d("CoffeeShopMap", "咖啡店: ${poi.name}, 地址: ${poi.address}")
                        }

                        mapView?.map?.let { baiduMap ->
                            baiduMap.clear()

                            markerIcon?.let { icon ->
                                result.allPoi.forEach { poi ->
                                    Log.d("CoffeeShopMap", "正在添加标记: ${poi.name}, ${poi.address}")
                                    val marker = baiduMap.addOverlay(MarkerOptions().apply {
                                        position(poi.location)
                                        icon(icon)
                                        val titleText = poi.name ?: "未知咖啡店"
                                        title(titleText)
                                        extraInfo(Bundle().apply {
                                            putString("title", titleText)
                                            putString("address", poi.address ?: "")
                                        })
                                        animateType(MarkerOptions.MarkerAnimateType.grow)
                                    }) as Marker
                                    Log.d("CoffeeShopMap", "标记创建完成: title=${marker.title}, name=${poi.name}")
                                }
                                Log.d("CoffeeShopMap", "添加了${result.allPoi.size}个咖啡店标记")
                                
                                baiduMap.showMapPoi(true)
                            }
                        }
                    }
                    
                    override fun onGetPoiDetailResult(result: PoiDetailResult?) {}
                    override fun onGetPoiIndoorResult(result: PoiIndoorResult?) {}
                    override fun onGetPoiDetailResult(result: PoiDetailSearchResult?) {}
                })
            }
        }
    }

    val initLocationClient = remember {
        {
            try {
                Log.d("CoffeeShopMap", "开始初始化定位客户端")
                locationClient = LocationClient(context.applicationContext).apply {
                    val option = LocationClientOption().apply {
                        isOpenGps = true
                        setCoorType("bd09ll")
                        setScanSpan(100000)
                        setIsNeedAddress(true)
                        setIsNeedLocationDescribe(true)
                        setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy)
                    }
                    Log.d("CoffeeShopMap", "定位参数设置完成")
                    locOption = option
                    
                    registerLocationListener(object : BDAbstractLocationListener() {
                        override fun onReceiveLocation(location: BDLocation) {
                            Log.d("CoffeeShopMap", "收到定位: lat=${location.latitude}, lng=${location.longitude}")
                            Log.d("CoffeeShopMap", "定位类型: ${location.locType}")
                            Log.d("CoffeeShopMap", "定位描述: ${location.locationDescribe}")
                            
                            val latLng = LatLng(location.latitude, location.longitude)
                            mapView?.map?.let { baiduMap ->
                                val locData = MyLocationData.Builder()
                                    .accuracy(location.radius)
                                    .direction(location.direction)
                                    .latitude(location.latitude)
                                    .longitude(location.longitude)
                                    .build()
                                baiduMap.setMyLocationData(locData)
                                Log.d("CoffeeShopMap", "已更新地图定位数据")
                            }
                            searchNearbyShops(latLng)
                        }
                    })
                    Log.d("CoffeeShopMap", "开始定位")
                    start()
                }
            } catch (e: Exception) {
                Log.e("CoffeeShopMap", "定位初始化失败", e)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            initLocationClient()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("CoffeeShopMap", "权限状态: $hasLocationPermission")
        if (!hasLocationPermission) {
            Log.d("CoffeeShopMap", "请求位置权限")
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            Log.d("CoffeeShopMap", "已有权限,初始化")
            initLocationClient()
        }
        initPoiSearch()
        try {
            val drawable = context.getDrawable(R.drawable.ic_coffee_marker)
            drawable?.let {
                val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, 48, 48)
                drawable.draw(canvas)
                markerIcon = BitmapDescriptorFactory.fromBitmap(bitmap)
                Log.d("CoffeeShopMap", "标记图标初始化成功")
            }
        } catch (e: Exception) {
            Log.e("CoffeeShopMap", "标记图标初始化失败", e)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            locationClient?.stop()
            mapView?.onDestroy()
            poiSearch?.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Cafe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        AndroidView(
            factory = { ctx ->
                MapView(ctx).also { mv ->
                    Log.d("CoffeeShopMap", "地图视图创建")
                    mapView = mv
                    val baiduMap = mv.map
                    
                    baiduMap.isMyLocationEnabled = true
                    Log.d("CoffeeShopMap", "地图定位图层已启用")
                    
                    baiduMap.setInfoWindowAdapter(object : InfoWindowAdapter {
                        override fun getInfoWindowView(marker: Marker): View {
                            val title = marker.extraInfo?.getString("title") ?: "未知咖啡店"
                            val address = marker.extraInfo?.getString("address") ?: ""
                            Log.d("CoffeeShopMap", "创建信息窗口: title=$title, address=$address")
                            
                            return TextView(context).apply {
                                text = android.text.Html.fromHtml(
                                    "<b>$title</b><br/>$address",
                                    android.text.Html.FROM_HTML_MODE_COMPACT
                                )
                                setTextColor(android.graphics.Color.BLACK)
                                setPadding(20, 10, 20, 10)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                minWidth = 300
                                maxWidth = 600
                                gravity = android.view.Gravity.CENTER
                                elevation = 8f
                                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                                background = android.graphics.drawable.GradientDrawable().apply {
                                    setColor(android.graphics.Color.WHITE)
                                    cornerRadius = 8f
                                }
                            }
                        }
                        
                        override fun getInfoWindowViewYOffset(): Int {
                            return -80
                        }

                        override fun getInfoWindow(marker: Marker?): InfoWindow? {
                            return null
                        }
                    })
                    
                    baiduMap.setOnMarkerClickListener { marker ->
                        if (currentShownMarker == marker) {
                            marker.hideInfoWindow()
                            currentShownMarker = null
                        } else {
                            currentShownMarker?.hideInfoWindow()
                            marker.showInfoWindow()
                            currentShownMarker = marker
                        }
                        true
                    }
                    
                    val myLocationConfiguration = MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.NORMAL,
                        true,
                        null,
                        0xAAFFFF88.toInt(),
                        0xAA00FF00.toInt()
                    )
                    baiduMap.setMyLocationConfiguration(myLocationConfiguration)
                    Log.d("CoffeeShopMap", "地图定位配置已设置")
                }
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        )
    }
} 

