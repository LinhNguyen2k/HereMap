package com.example.heremap.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.lifecycle.ViewModelProvider
import com.example.heremap.R
//import com.example.heremap.ResultListActivity
import com.example.heremap.adapter.AutoSuggestAdapter
import com.example.heremap.viewmodel.MapViewModel
import com.google.android.gms.maps.model.LatLng
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.routing.*
import com.here.android.mpa.search.*
import com.here.android.mpa.search.TextAutoSuggestionRequest.AutoSuggestFilterType
import java.io.File
import java.io.IOException
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
    private var typeVehicle: Int? = null
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
    private val m_mapPoint = ArrayList<MapObject>()
    private val mapFragment: AndroidXMapFragment?
        get() = m_activity.supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment?
    private val mapViewModel by lazy {
        ViewModelProvider(m_activity).get(MapViewModel::class.java)
    }

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
            ) return
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f,
                (this as LocationListener))
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            btnSwap.setOnClickListener {
                val temp = mapViewModel.startPoint
                mapViewModel.startPoint = mapViewModel.endPoint
                mapViewModel.endPoint = temp
                m_map!!.removeMapObjects(mapViewModel.m_Route)
                mapViewModel.m_Route.clear()
                val geo = GeoCoordinate(mapViewModel.endPoint!!.latitude,
                    mapViewModel.endPoint!!.longitude)
                mapViewModel.getLocation(geo, 1, true)
                mapViewModel.getLocation(geo, 0, false)
                if (mapViewModel.startPoint != LatLng(location!!.latitude, location.longitude)) {
                    val temp = mapViewModel.startPoint
                    mapViewModel.startPoint = mapViewModel.endPoint
                    mapViewModel.endPoint = temp
                }
            }
            m_mapFragment!!.init { error ->
                if (error == OnEngineInitListener.Error.NONE) {
                    mapViewModel.m_map = m_mapFragment!!.map
                    mapViewModel.startPoint = LatLng(location!!.latitude, location.longitude)
                    mapViewModel.loadGPS(location)
                    mapViewModel.startPoint = LatLng(location.latitude, location.longitude)
                    observe()
                    m_mapFragment!!.mapGesture!!.addOnGestureListener(object :
                        MapGesture.OnGestureListener.OnGestureListenerAdapter() {
                        override fun onTapEvent(p0: PointF): Boolean {
                            val geo = m_map!!.pixelToGeo(p0)
                            if (!isFirstClick)
                                mapViewModel.dropMarker()
                            isFirstClick = false
                            layout_time.visibility = View.INVISIBLE
                            mapViewModel.setMarker(geo!!.latitude, geo.longitude)
                            return false
                        }

                        override fun onLongPressEvent(p0: PointF): Boolean {
                            val geo = m_map!!.pixelToGeo(p0)
                            drawGeo(geo!!)
                            mapViewModel.endPoint = LatLng(geo.latitude, geo.longitude)
                            m_map!!.removeMapObjects(mapViewModel.m_Route)
                            mapViewModel.m_Route.clear()
                            m_map!!.removeMapObjects(m_mapPoint)
                            m_mapPoint.clear()
                            return false
                        }
                    }, 100, true)
                    m_map = m_mapFragment!!.map
                    btnGps.setOnClickListener {
                        mapViewModel.loadGPS(location)
                    }
                }
            }
        }
    }

    private fun observe() {
        mapViewModel.typeVehicle.observe(m_activity) {
            typeVehicle = it
        }
        mapViewModel.result_time1.observe(m_activity) {
            tv_time.text = it
        }
        mapViewModel.result_time2.observe(m_activity) {
            tv_time2.text = it
        }
        mapViewModel.result_distance1.observe(m_activity) {
            createRoute.text = it
        }
        mapViewModel.result_distance2.observe(m_activity) {
            tv_route2.text = it
        }

    }

    fun drawGeo(position: GeoCoordinate) {
        val request = ReverseGeocodeRequest(position)
        request.execute { location: com.here.android.mpa.search.Location?, errorCode: ErrorCode ->
            val address = location!!.address
            AlertDialog.Builder(m_activity)
                .setTitle(address!!.text)
                .setMessage(" Lat: ${position.latitude} \n Long: ${position.longitude}\n${
                    location.address
                }").setPositiveButton(
                    "Di chuyển đến"
                ) { _, _ ->
                    mapViewModel.endPoint = LatLng(position.latitude, position.longitude)
                    while (mapViewModel.m_mapObjectList.size > 1) {
                        m_map!!.removeMapObject(mapViewModel.m_mapObjectList[mapViewModel.m_mapObjectList.size - 1])
                        mapViewModel.m_mapObjectList.removeAt(mapViewModel.m_mapObjectList.size - 1)
                    }
                    mapViewModel.setMarker(position.latitude, position.longitude)
                    mapViewModel.getLocation(position, 1, true)
                    mapViewModel.getLocation(position, 0, false)
                    layout_time.visibility = View.VISIBLE
                    cardView_btnSwap.visibility = View.VISIBLE
                }
                .setNegativeButton("OK") { _, _ ->
                }
                .show()
        }
    }

    private fun initControls() {
        m_searchView = m_activity.findViewById(R.id.search)
        m_searchView!!.setOnQueryTextListener(m_searchListener)
        val localeAdapter = ArrayAdapter<CharSequence>(
            m_activity, android.R.layout.simple_spinner_item)
        localeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        m_resultsListView = m_activity.findViewById(R.id.resultsListViev)
        m_autoSuggestAdapter = AutoSuggestAdapter(m_activity,
            android.R.layout.simple_list_item_1, m_autoSuggests)
        m_resultsListView!!.adapter = m_autoSuggestAdapter
        m_resultsListView!!.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                val item = parent.getItemAtPosition(position) as AutoSuggest
                m_map!!.removeMapObjects(mapViewModel.m_Route)
                mapViewModel.m_Route.clear()
                while (mapViewModel.m_mapObjectList.size > 1) {
                    m_map!!.removeMapObject(mapViewModel.m_mapObjectList[mapViewModel.m_mapObjectList.size - 1])
                    mapViewModel.m_mapObjectList.removeAt(mapViewModel.m_mapObjectList.size - 1)
                }
                cardView_btnSwap.visibility = View.INVISIBLE
                handleSelectedAutoSuggest(item)
            }
        // Initialize filter options view
        val linearLayout = m_activity.findViewById<LinearLayout>(R.id.filterOptionsContainer)
        m_filterOptionsContainer = LinearLayout(m_activity)
        linearLayout.orientation = LinearLayout.VERTICAL
        m_useFilteringCheckbox = CheckBox(m_activity)
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
                        mapViewModel.endPoint = LatLng(p0!!.location!!.coordinate!!.latitude,
                            p0.location!!.coordinate!!.longitude)
                        handlePlace(p0.location!!.coordinate!!)
                        setSearchMode(false)

                    }
                }
            }
//            AutoSuggest.Type.SEARCH -> {
//                val autoSuggestSearch = autoSuggest as AutoSuggestSearch
//                val discoverRequest = autoSuggestSearch.suggestedSearchRequest
//                discoverRequest!!.execute { p0, p1 ->
//                    if (p1 == ErrorCode.NONE) {
//                        s_discoverResultList = p0!!.items
//                        val intent = Intent(m_activity, ResultListActivity::class.java)
//                        m_activity.startActivity(intent)
//                    }
//                }
//            }
            else -> {}
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

    private fun showDialog(place: GeoCoordinate) {
        val routeOptions = RouteOptions()
        val routePlan = RoutePlan()
        val tunes = arrayOf("Xe ô tô", "Xe Máy(Xe tay ga)")
        val alertDialog: AlertDialog = AlertDialog.Builder(m_activity)
            .setIcon(R.drawable.ic_baseline_moving_24)
            .setTitle("Chọn phương tiện di chuyển")
            .setSingleChoiceItems(tunes, -1)
            { dialog, i ->
                if (tunes[i] == "Xe ô tô") {
                    mapViewModel.setTypeVehicle(0)
                    routePlan.routeOptions = routeOptions
                    mapViewModel.getLocation(place, 0, false)
                    mapViewModel.getLocation(place, 1, true)
                    layout_time.visibility = View.VISIBLE
                    dialog.dismiss()
                } else {
                    mapViewModel.setTypeVehicle(1)
                    routePlan.routeOptions = routeOptions
                    mapViewModel.getLocation(place, 0, false)
                    mapViewModel.getLocation(place, 1, true)
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

    companion object {
        var s_discoverResultList: List<DiscoveryResult>? = null
        private var m_marker_image: Image? = null
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