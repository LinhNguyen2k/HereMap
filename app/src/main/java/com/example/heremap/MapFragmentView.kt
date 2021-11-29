/*
 * Copyright (c) 2011-2020 HERE Europe B.V.
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
package com.example.heremap

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.routing.*
import com.here.android.mpa.search.*
import com.here.android.mpa.search.TextAutoSuggestionRequest.AutoSuggestFilterType
import com.nokia.maps.p1
import java.io.File
import java.io.IOException
import java.lang.StringBuilder
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class MapFragmentView(private val m_activity: AppCompatActivity) : LocationListener {
    private var m_mapFragment: AndroidXMapFragment? = null
    private var m_mapFragmentContainer: View? = null
    private var m_map: Map? = null
    private var m_searchView: SearchView? = null
    private val m_searchListener: SearchListener
    private var m_autoSuggestAdapter: AutoSuggestAdapter? = null
    private val m_autoSuggests: MutableList<AutoSuggest>
    private var m_resultsListView: ListView? = null
    private var m_filterOptionsContainer: LinearLayout? = null
    private var m_useFilteringCheckbox: CheckBox? = null
    private var startLocation: LatLng? = null
    private var endPoint: LatLng? = null
    var createRoute: TextView
    var layout_time: RelativeLayout
    var tv_time: TextView
    var tv_time2: TextView
    var tv_route2: TextView
    var btnGps: Button
    var cardView_btnSwap: CardView
    var btnFind: Button
    var btnSwap: Button
    var isFirstClick: Boolean = true
    private var m_mapRoute: MapRoute? = null
    private val m_mapObjectList = ArrayList<MapObject>()
    private val m_Route = ArrayList<MapObject>()
    private val m_mapPoint = ArrayList<MapObject>()
    private val mapFragment: AndroidXMapFragment?
        private get() = m_activity.supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment?

    private fun initMapFragment() {
        m_mapFragment = mapFragment
        m_mapFragmentContainer = m_activity.findViewById(R.id.mapfragment)
        layout_time.visibility = View.INVISIBLE
        val path = File(m_activity.getExternalFilesDir(null), ".here-map-data")
            .absolutePath
        MapSettings.setDiskCacheRootPath(path)

        if (m_mapFragment != null) {
            val locationManager =
                m_activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(m_activity,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    m_activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f,
                (this as LocationListener))
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            startLocation = LatLng(location!!.latitude, location!!.longitude)

            btnSwap.setOnClickListener {
                var temp = startLocation
                startLocation = endPoint
                endPoint = temp
                if (m_Route != null) {
                    m_map!!.removeMapObjects(m_Route)
                    m_Route.clear()
                }
                var geo = GeoCoordinate(endPoint!!.latitude, endPoint!!.longitude)
                getLocation(geo, 1, true)
                getLocation(geo, 0, false)
                if (startLocation != LatLng(location!!.latitude, location!!.longitude)) {
                    var temp = startLocation
                    startLocation = endPoint
                    endPoint = temp
                }
            }
            m_mapFragment!!.init { error ->
                if (error == OnEngineInitListener.Error.NONE) {
                    loadGPS(location!!)
                    m_mapFragment!!.mapGesture!!.addOnGestureListener(object :
                        MapGesture.OnGestureListener.OnGestureListenerAdapter() {
                        override fun onTapEvent(p0: PointF): Boolean {
                            val geo = m_map!!.pixelToGeo(p0)
                            if (!isFirstClick)
                                dropMarker()
                            isFirstClick = false
                            layout_time.visibility = View.INVISIBLE
                            setMarker(geo!!.latitude, geo!!.longitude)
                            return false
                        }

                        override fun onLongPressEvent(p0: PointF): Boolean {
                            val geo = m_map!!.pixelToGeo(p0)
                            drawGeo(geo!!)
                            if (m_Route != null) {
                                m_map!!.removeMapObjects(m_Route)
                                m_Route.clear()
                            }
                            if (m_mapPoint != null) {
                                m_map!!.removeMapObjects(m_mapPoint)
                                m_mapPoint.clear()
                            }
                            return false
                        }
                    }, 100, true)
                    m_map = m_mapFragment!!.map
                    btnGps.setOnClickListener {
                        loadGPS(location!!)
                    }
                }
            }
        }
    }

    fun drawGeo(position: GeoCoordinate) {
        val request = ReverseGeocodeRequest(position)
        request.execute(ResultListener<com.here.android.mpa.search.Location> { location: com.here.android.mpa.search.Location?, errorCode: ErrorCode ->
            var address = location!!.address
            AlertDialog.Builder(m_activity)
                .setTitle(address!!.text)
                .setMessage(" Lat: ${position.latitude} \n Long: ${position.longitude}\n${
                    location!!.address
                }").setPositiveButton(
                    "Di chuyển đến"
                ) { _, _ ->
                    endPoint = LatLng(position.latitude, position.longitude)
                    while (m_mapObjectList.size > 1) {
                        m_map!!.removeMapObject(m_mapObjectList[m_mapObjectList.size - 1])
                        m_mapObjectList.removeAt(m_mapObjectList.size - 1)
                    }
                    setMarker(position.latitude, position.longitude)
                    getLocation(position, 1, true)
                    getLocation(position, 0, false)
                    layout_time.visibility = View.VISIBLE
                }
                .setNegativeButton("OK") { _, _ ->
                }
                .show()
        })
    }

    private fun setMarker(lat: Double, long: Double) {
        val marker_img2 = Image()
        marker_img2.setImageResource(R.drawable.imagegps)
        val marker2 = MapMarker()
        marker2.coordinate = GeoCoordinate(lat, long)
        marker2.icon = marker_img2
        m_map!!.addMapObject(marker2)
        m_mapObjectList.add(marker2)
    }

    private fun dropMarker() {
        if (m_mapObjectList.size > 1) {
            m_map!!.removeMapObject(m_mapObjectList[m_mapObjectList.size - 1])
            m_mapObjectList.removeAt(m_mapObjectList.size - 1)
        }
    }

    private fun loadGPS(location: Location) {
        m_map = m_mapFragment!!.map
        m_map!!.setCenter(GeoCoordinate(location!!.latitude,
            location.longitude),
            Map.Animation.NONE)
        val marker_img2 = Image()
        try {
            marker_img2.setImageResource(R.drawable.imagegps)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val marker2 = MapMarker(m_map!!.center, marker_img2)
        m_map!!.removeMapObject(marker2)
        m_map!!.addMapObject(marker2)
        m_mapObjectList.add(marker2)
        m_map!!.zoomLevel = 11.0
    }

    // Search view
    private fun initControls() {
        m_searchView = m_activity.findViewById(R.id.search)
        m_searchView!!.setOnQueryTextListener(m_searchListener)
        val localeAdapter = ArrayAdapter<CharSequence>(
            m_activity, android.R.layout.simple_spinner_item, AVAILABLE_LOCALES)
        localeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        m_resultsListView = m_activity.findViewById(R.id.resultsListViev)
        m_autoSuggestAdapter = AutoSuggestAdapter(m_activity,
            android.R.layout.simple_list_item_1, m_autoSuggests)
        m_resultsListView!!.adapter = m_autoSuggestAdapter
        m_resultsListView!!.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                val item = parent.getItemAtPosition(position) as AutoSuggest
                if (m_Route != null) {
                    m_map!!.removeMapObjects(m_Route)
                    m_Route.clear()
                }
                while (m_mapObjectList.size > 1) {
                    m_map!!.removeMapObject(m_mapObjectList[m_mapObjectList.size - 1])
                    m_mapObjectList.removeAt(m_mapObjectList.size - 1)
                }
                cardView_btnSwap.visibility = View.INVISIBLE
                handleSelectedAutoSuggest(item)
            }
        // Initialize filter options view
        val linearLayout = m_activity.findViewById<LinearLayout>(R.id.filterOptionsContainer)
        m_filterOptionsContainer = LinearLayout(m_activity)
        linearLayout.orientation = LinearLayout.VERTICAL
        m_useFilteringCheckbox = CheckBox(m_activity)
        m_useFilteringCheckbox!!.text = "Use filter"
        m_useFilteringCheckbox!!.isChecked = false
        m_useFilteringCheckbox!!.setOnCheckedChangeListener { _, isChecked ->
            m_filterOptionsContainer!!.visibility =
                if (isChecked) View.VISIBLE else View.GONE
        }
        m_useFilteringCheckbox!!.visibility = View.GONE
        m_filterOptionsContainer!!.visibility = View.GONE
        m_filterOptionsContainer!!.orientation = LinearLayout.VERTICAL
        m_filterOptionsContainer!!.setPadding(50, 0, 0, 0)
        val filterOptions = AutoSuggestFilterType.values()
        for (filterOption in filterOptions) {
            val curCB = CheckBox(m_activity)
            curCB.isChecked = false
            curCB.text = filterOption.toString()
            m_filterOptionsContainer!!.addView(curCB)
        }
        linearLayout.addView(m_useFilteringCheckbox)
        linearLayout.addView(m_filterOptionsContainer)
    }

    private fun applyResultFiltersToRequest(request: TextAutoSuggestionRequest) {
        if (m_useFilteringCheckbox!!.isChecked) {
            val filterOptions = AutoSuggestFilterType.values()
            val totalFilterOptionsCount = m_filterOptionsContainer!!.childCount
            val filtersToApply: MutableList<AutoSuggestFilterType> = ArrayList(filterOptions.size)
            for (i in 0 until totalFilterOptionsCount) {
                if ((m_filterOptionsContainer!!.getChildAt(i) as CheckBox).isChecked) {
                    filtersToApply.add(filterOptions[i])
                }
            }
            if (filtersToApply.isNotEmpty()) {
                request.setFilters(EnumSet.copyOf(filtersToApply))
            }
        }
    }

    override fun onLocationChanged(location: Location) {}
    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}
    private inner class SearchListener : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String): Boolean {
            if (newText.isNotEmpty()) {
                doSearch(newText)
            } else {
                setSearchMode(false)
            }
            return false
        }
    }

    private fun doSearch(query: String) {
        setSearchMode(true)

        val textAutoSuggestionRequest = TextAutoSuggestionRequest(query)
        textAutoSuggestionRequest.setSearchCenter(m_map!!.center)
        applyResultFiltersToRequest(textAutoSuggestionRequest)
        textAutoSuggestionRequest.execute { p0, p1 ->
            if (p1 == ErrorCode.NONE) {
                processSearchResults(p0!!)
            } else {
                handleError(p1!!)
            }
        }
    }

    private fun processSearchResults(autoSuggests: List<AutoSuggest>) {
        m_activity.runOnUiThread {
            m_autoSuggests.clear()
            m_autoSuggests.addAll(autoSuggests)
            m_autoSuggestAdapter!!.notifyDataSetChanged()
        }
    }

    private fun handleSelectedAutoSuggest(autoSuggest: AutoSuggest) {
        when (autoSuggest.type) {
            AutoSuggest.Type.PLACE -> {
                val autoSuggestPlace = autoSuggest as AutoSuggestPlace
                val detailsRequest = autoSuggestPlace.placeDetailsRequest
                detailsRequest!!.execute { p0, p1 ->
                    if (p1 == ErrorCode.NONE) {
                        endPoint = LatLng(p0!!.location!!.coordinate!!.latitude,
                            p0!!.location!!.coordinate!!.longitude)
                        handlePlace(p0!!.location!!.coordinate!!)
                        setSearchMode(false)

                    } else {
                        handleError(p1!!)
                    }
                }
            }
            AutoSuggest.Type.SEARCH -> {
                val autoSuggestSearch = autoSuggest as AutoSuggestSearch
                val discoverRequest = autoSuggestSearch.suggestedSearchRequest
                discoverRequest!!.execute { p0, p1 ->
                    if (p1 == ErrorCode.NONE) {
                        s_discoverResultList = p0!!.items
                        val intent = Intent(m_activity, ResultListActivity::class.java)
                        m_activity.startActivity(intent)
                    } else {
                        handleError(p1!!)
                    }
                }
            }
        }
    }

    fun setSearchMode(isSearch: Boolean) {
        if (isSearch) {
            m_mapFragmentContainer!!.visibility = View.INVISIBLE
            m_resultsListView!!.visibility = View.VISIBLE
            layout_time.visibility = View.INVISIBLE
        } else {
            m_mapFragmentContainer!!.visibility = View.VISIBLE
            m_resultsListView!!.visibility = View.INVISIBLE
            layout_time.visibility = View.INVISIBLE
        }
    }

    var marker: MapMarker? = null
    private fun handlePlace(place: GeoCoordinate) {
        if (marker != null) {
            m_map!!.removeMapObject(marker!!)
        }
        m_map = m_mapFragment!!.map
        m_map!!.setCenter(place, Map.Animation.NONE)
        m_marker_image = Image()
        try {
            m_marker_image!!.setImageResource(R.drawable.marker)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        marker = MapMarker(m_map!!.center, m_marker_image!!)
        m_map!!.removeMapObject(marker!!)
        m_map!!.addMapObject(marker!!)
        m_mapPoint.add(marker!!)
        m_map!!.zoomLevel = 11.0
        btnFind.setOnClickListener {
            showDialog(place)
            cardView_btnSwap.visibility = View.VISIBLE
        }

    }

    // Xóa giữ liệu chỉ lấy 1 điểm đánh dấu
    private fun getLocation(place: GeoCoordinate, count: Int, isCheck: Boolean) {
        val routePlan = RoutePlan()
        val routeOptions = RouteOptions()
        if (count == 0) {
            routeOptions.routeType = RouteOptions.Type.SHORTEST
        } else {
            routeOptions.routeType = RouteOptions.Type.FASTEST
        }
        routeOptions.routeCount = 1
        routePlan.routeOptions = routeOptions
        val destination = RouteWaypoint(place)
        val startPoint =
            RouteWaypoint(GeoCoordinate(startLocation!!.latitude, startLocation!!.longitude))

        routePlan.addWaypoint(startPoint)
        routePlan.addWaypoint(destination)
        val coreRouter = CoreRouter()
        coreRouter.calculateRoute(
            routePlan,
            object : Router.Listener<List<RouteResult>, RoutingError> {
                override fun onProgress(i: Int) {
                }

                override fun onCalculateRouteFinished(
                    p0: List<RouteResult>?,
                    p1: RoutingError,
                ) {
                    if (p1 == RoutingError.NONE) {

                        if (p0!![0].route != null) {
                            m_mapRoute = MapRoute(p0[0]!!.route)
                            m_mapRoute!!.isManeuverNumberVisible = true
                            val df = DecimalFormat("#.00")
                            if (isCheck) {
                                m_mapRoute!!.color = Color.BLUE
                                val timeInHours: Int =
                                    ((p0[0].route.getTtaExcludingTraffic(Route.WHOLE_ROUTE)!!
                                        .duration))
                                tv_time.text = "Thời gian:  ${formatTime(timeInHours)}"

                                val a = ((m_mapRoute!!.route!!.length).toDouble() / 1000)
                                createRoute.text = "Khoảng cách ${df.format(a)} km"

                            } else {
                                m_mapRoute!!.color = Color.GRAY
                                val timeInHours: Int =
                                    ((p0[0].route.getTtaExcludingTraffic(Route.WHOLE_ROUTE)!!
                                        .duration))
                                tv_time2.text = "Thời gian:  ${formatTime(timeInHours)}"
                                val a = ((m_mapRoute!!.route!!.length).toDouble() / 1000)
                                tv_route2.text = "Khoảng cách: ${df.format(a)} km"
                            }
                            m_map!!.addMapObject(m_mapRoute!!)
                            val gbb = p0[0]!!.route.boundingBox
                            m_map!!.zoomTo(gbb!!, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION)
                            m_Route.add(m_mapRoute!!)
                            cardView_btnSwap.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(m_activity,
                                "Error:route results returned is not valid",
                                Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(m_activity,
                            "Error:route calculation returned error code: $p1",
                            Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun clearMap() {
        m_map!!.removeMapObjects(m_mapObjectList)
        m_mapObjectList.clear()
    }

    private fun showDialog(place: GeoCoordinate) {
        var routeOptions = RouteOptions()
        var routePlan = RoutePlan()
        val tunes = arrayOf("Xe ô tô", "Xe Máy(Xe tay ga)")
        val alertDialog: AlertDialog = AlertDialog.Builder(m_activity)
            .setIcon(R.drawable.ic_baseline_moving_24)
            .setTitle("Chọn phương tiện di chuyển")
            .setSingleChoiceItems(tunes, -1)
            { dialog, i ->
                if (tunes[i] == "Xe ô tô") {
                    routeOptions.transportMode =
                        RouteOptions.TransportMode.CAR
                    routePlan.routeOptions = routeOptions
                    getLocation(place, 1, true)
                    getLocation(place, 0, false)
                    layout_time.visibility = View.VISIBLE
                    dialog.dismiss()
                } else {
                    routeOptions.transportMode =
                        RouteOptions.TransportMode.SCOOTER
                    routePlan.routeOptions = routeOptions
                    getLocation(place, 1, true)
                    getLocation(place, 0, false)
                    layout_time.visibility = View.VISIBLE
                    dialog.dismiss()
                }

            }
            .setNegativeButton("Hủy Bỏ"
            ) { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        alertDialog.show()
    }

    private fun handleError(errorCode: ErrorCode) {
        showMessage("Error", "Error description: " + errorCode.name, true)
    }

    private fun showMessage(title: String, message: String, isError: Boolean) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(m_activity)
        builder.setTitle(title).setMessage(message)
        if (isError) {
            builder.setIcon(android.R.drawable.ic_dialog_alert)
        } else {
            builder.setIcon(android.R.drawable.ic_dialog_info)
        }
        builder.setNeutralButton("OK", null)
        builder.create().show()
        setSearchMode(false)
//        initMapFragment()
    }

    private fun formatTime(second: Int): String {
        var result = ""
        var second = second
        val hours = second / 3600
        second -= hours * 3600
        val minutes = second / 60
        second -= minutes * 60
        if (hours > 0) {
            result += "$hours giờ "
        }
        if (minutes > 0) {
            result += " $minutes phút"
        }

        return result
    }

    companion object {
        var s_discoverResultList: List<DiscoveryResult>? = null
        private var m_marker_image: Image? = null
        private val AVAILABLE_LOCALES = arrayOf(
            "",
            "af-ZA",
            "sq-AL",
            "ar-SA",
            "az-Latn-AZ",
            "eu-ES",
            "be-BY",
            "bg-BG",
            "ca-ES",
            "zh-CN",
            "zh-TW",
            "hr-HR",
            "cs-CZ",
            "da-DK",
            "nl-NL",
            "en-GB",
            "en-US",
            "et-EE",
            "fa-IR",
            "fil-PH",
            "fi-FI",
            "fr-FR",
            "fr-CA",
            "gl-ES",
            "de-DE",
            "el-GR",
            "ha-Latn-NG",
            "he-IL",
            "hi-IN",
            "hu-HU",
            "id-ID",
            "it-IT",
            "ja-JP",
            "kk-KZ",
            "ko-KR",
            "lv-LV",
            "lt-LT",
            "mk-MK",
            "ms-MY",
            "nb-NO",
            "pl-PL",
            "pt-BR",
            "pt-PT",
            "ro-RO",
            "ru-RU",
            "sr-Latn-CS",
            "sk-SK",
            "sl-SI",
            "es-MX",
            "es-ES",
            "sv-SE",
            "th-TH",
            "tr-TR",
            "uk-UA",
            "uz-Latn-UZ",
            "vi-VN"
        )
    }

    init {
        m_searchListener = SearchListener()
        m_autoSuggests = ArrayList()
        createRoute = m_activity.findViewById(R.id.createRoute)
        tv_time = m_activity.findViewById(R.id.tv_time)
        layout_time = m_activity.findViewById(R.id.layout_time)
        btnGps = m_activity.findViewById(R.id.btnGps)
        btnFind = m_activity.findViewById(R.id.btnFind)
        cardView_btnSwap = m_activity.findViewById(R.id.cardView_btnSwap)
        btnSwap = m_activity.findViewById(R.id.btnSwap)
        tv_time2 = m_activity.findViewById(R.id.tv_time2)
        tv_route2 = m_activity.findViewById(R.id.createRoute2)
        initMapFragment()
        initControls()
    }
}