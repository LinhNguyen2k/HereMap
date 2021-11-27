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
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isNotEmpty
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.routing.*
import com.here.android.mpa.search.*
import com.here.android.mpa.search.TextAutoSuggestionRequest.AutoSuggestFilterType
import java.io.File
import java.io.IOException
import java.lang.StringBuilder
import java.util.*

/**
 * This class encapsulates the properties and functionality of the Map view. It also implements 3
 * types of AutoSuggest requests that HERE Mobile SDK for Android (Premium) provides as example.
 */
class MapFragmentView(private val m_activity: AppCompatActivity) : LocationListener {
    private var m_mapFragment: AndroidXMapFragment? = null
    private var m_mapFragmentContainer: View? = null
    private var m_map: Map? = null
    private var q_map: Map? = null
    private var n_map: Map? = null
    private var m_searchView: SearchView? = null
    private val m_searchListener: SearchListener
    private var m_autoSuggestAdapter: AutoSuggestAdapter? = null
    private val m_autoSuggests: MutableList<AutoSuggest>
    private var m_resultsListView: ListView? = null
    private var m_filterOptionsContainer: LinearLayout? = null
    private var m_useFilteringCheckbox: CheckBox? = null
    var createRoute: TextView
    var layout_time : LinearLayout
    var tv_time: TextView
    var btnGps: Button
    private var m_mapRoute: MapRoute? = null
    private val m_tap_marker: MapScreenMarker? = null
    private val m_map_markers = LinkedList<MapMarker>()
    private val mapFragment: AndroidXMapFragment?
        private get() = m_activity.supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment?

    private fun initMapFragment() {
        m_mapFragment = mapFragment
        m_mapFragmentContainer = m_activity.findViewById(R.id.mapfragment)

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
            if (createRoute == null){
                layout_time.visibility = View.INVISIBLE
            } else {
                layout_time.visibility = View.VISIBLE
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f,
                (this as LocationListener))
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            m_mapFragment!!.init { error ->
                if (error == OnEngineInitListener.Error.NONE) {
                    m_map = m_mapFragment!!.map
                    btnGps.setOnClickListener {
                        n_map = m_mapFragment!!.map
                        n_map!!.setCenter(GeoCoordinate(location!!.latitude,
                            location.longitude),
                            Map.Animation.NONE)
                        val marker_img2 = Image()
                        try {
                            marker_img2.setImageResource(R.drawable.imagegps)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        val marker2 = MapMarker(n_map!!.center, marker_img2)
                        n_map!!.removeAllMapObjects()
                        marker2.isDraggable = true
                        n_map!!.addMapObject(marker2)
                        n_map!!.zoomLevel = 11.0
                    }
                }
            }
        }
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
                        handlePlace(p0!!)
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
            AutoSuggest.Type.UNKNOWN -> {
            }
            else -> {
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
            layout_time.visibility = View.VISIBLE
        }
    }

    // Xóa giữ liệu chỉ lấy 1 điểm đánh dấu
    var marker: MapMarker? = null
    private fun handlePlace(place: Place) {
        if (marker != null) {
            m_map!!.removeMapObject(marker!!)
        }
        val sb = StringBuilder()
        // lấy địa chỉ
        sb.append("địa chỉ").append("""
    ${place.location!!.coordinate.toString()}""".trimIndent())
        m_map = m_mapFragment!!.map
        m_map!!.setCenter(GeoCoordinate(place.location!!.coordinate!!), Map.Animation.NONE)
        m_marker_image = Image()
        try {
            m_marker_image!!.setImageResource(R.drawable.marker)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        marker = MapMarker(m_map!!.center, m_marker_image!!)
        m_map!!.addMapObject(marker!!)

        btnGps.setOnClickListener { }

        val coreRouter = CoreRouter()

        val routePlan = RoutePlan()

        val routeOptions = RouteOptions()
        routeOptions.transportMode =
            RouteOptions.TransportMode.CAR
        routeOptions.setHighwaysAllowed(false)
        routeOptions.routeType =
            RouteOptions.Type.SHORTEST
        routeOptions.routeType =
            RouteOptions.Type.FASTEST
        routeOptions.routeCount = 2
        routePlan.routeOptions = routeOptions

        val startPoint = RouteWaypoint(GeoCoordinate(
            place.location!!.coordinate!!))
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
        val destination = RouteWaypoint(GeoCoordinate(location!!.latitude, location!!.longitude))

        routePlan.addWaypoint(startPoint)
        routePlan.addWaypoint(destination)


        coreRouter.calculateRoute(
            routePlan,
            object : Router.Listener<List<RouteResult>, RoutingError> {
                override fun onProgress(i: Int) {
                }
                override fun onCalculateRouteFinished(p0: List<RouteResult>?, p1: RoutingError) {
                    if (p1 == RoutingError.NONE) {
//                        m_map!!.removeMapObject(m_mapRoute!!)
                        if (p0!![0].route != null) {

                            m_mapRoute = MapRoute(p0[0]!!.route)
                            m_mapRoute!!.isManeuverNumberVisible =
                                true

                            m_map!!.addMapObject(m_mapRoute!!)

                            //-------------------------------------
                            val a = m_mapRoute!!.route!!.length.toString()
                            createRoute.text =  "Khoảng cách $a m"
//                            val tv_times = m_mapRoute!!.route!!.getRouteElementsFromDuration(
//                                m_mapRoute!!.route!!.length.toLong())
//                            tv_time.text = tv_times.toString()
                            val gbb = p0[0]!!.route
                                .boundingBox
                            m_map!!.zoomTo(gbb!!, Map.Animation.NONE,
                                Map.MOVE_PRESERVE_ORIENTATION)
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

        //------------------------------------------------------------------------------------------------
        sb.append("Tên: ").append("""
    ${place.name}
    
    """.trimIndent())
        sb.append("Alternative name:").append(place.alternativeNames)
        showMessage("Vị Trí", sb.toString(), false)
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
        initMapFragment()
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
        initMapFragment()
        initControls()
    }
}