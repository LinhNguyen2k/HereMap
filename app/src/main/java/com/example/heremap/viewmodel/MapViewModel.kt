package com.example.heremap.viewmodel

import android.graphics.Color
import android.location.Location
import com.here.android.mpa.mapping.Map
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.heremap.R
import com.google.android.gms.maps.model.LatLng
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.Image
import com.here.android.mpa.mapping.MapMarker
import com.here.android.mpa.mapping.MapObject
import com.here.android.mpa.mapping.MapRoute
import com.here.android.mpa.routing.*
import java.io.IOException
import java.text.DecimalFormat

class MapViewModel : ViewModel() {
    var m_map: Map? = null
    var startLocation = MutableLiveData<GeoCoordinate>()
    var endLocation = MutableLiveData<GeoCoordinate>()
    var typeVehicle = MutableLiveData<Int>()
    var result_time1 = MutableLiveData<String>()
    var result_time2 = MutableLiveData<String>()
    var result_distance1 = MutableLiveData<String>()
    var result_distance2 = MutableLiveData<String>()
    var startPoint: LatLng? = null
    var endPoint: LatLng? = null
    var m_mapRoute: MapRoute? = null
    val m_mapObjectList = ArrayList<MapObject>()
    val m_Route = ArrayList<MapObject>()

    init {
        typeVehicle.value = 0
        result_time1.value = ""
        result_time2.value = ""
        result_distance1.value = ""
        result_distance2.value = ""

    }


    fun setTypeVehicle(type: Int) {
        typeVehicle.value = type
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

    fun getLocation(place: GeoCoordinate, count: Int, isCheck: Boolean) {
        val routePlan = RoutePlan()
        val routeOptions = RouteOptions()
        when (count) {
            0 -> routeOptions.routeType = RouteOptions.Type.SHORTEST
            1 -> routeOptions.routeType = RouteOptions.Type.FASTEST
        }
        routeOptions.routeCount = 1
        routePlan.routeOptions = routeOptions
        val destination = RouteWaypoint(GeoCoordinate(place.latitude, place.longitude))
        val startPoint =
            RouteWaypoint(GeoCoordinate(startPoint!!.latitude, startPoint!!.longitude))
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
                            m_mapRoute = MapRoute(p0[0].route)
                            m_map!!.addMapObject(m_mapRoute!!)
                            m_mapRoute!!.isManeuverNumberVisible = true
                            val df = DecimalFormat("#.00")
                            if (isCheck) {
                                m_mapRoute!!.color = Color.BLUE
                                val timeInHours: Int =
                                    ((p0[0].route.getTtaExcludingTraffic(Route.WHOLE_ROUTE)!!
                                        .duration))
                                result_time1.value = "Thời gian:  ${formatTime(timeInHours)}"

                                val a = ((m_mapRoute!!.route!!.length).toDouble() / 1000)
                                result_distance1.value = "Khoảng cách ${df.format(a)} km"

                            } else {
                                m_mapRoute!!.color = Color.GRAY
                                val timeInHours: Int =
                                    ((p0[0].route.getTtaExcludingTraffic(Route.WHOLE_ROUTE)!!
                                        .duration))
                                result_time2.value = "Thời gian:  ${formatTime(timeInHours)}"
                                val a = ((m_mapRoute!!.route!!.length).toDouble() / 1000)
                                result_distance2.value = "Khoảng cách: ${df.format(a)} km"
                            }
                            val gbb = p0[0].route.boundingBox
                            m_map!!.zoomTo(gbb!!, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION)
                            m_Route.add(m_mapRoute!!)
                        }
                    }
                }
            })
    }

    fun setMarker(lat: Double, long: Double) {
        val marker_img2 = Image()
        marker_img2.setImageResource(R.drawable.imagegps)
        val marker2 = MapMarker()
        marker2.coordinate = GeoCoordinate(lat, long)
        marker2.icon = marker_img2
        m_map!!.addMapObject(marker2)
        m_mapObjectList.add(marker2)
    }

    fun loadGPS(location: Location) {
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

    fun dropMarker() {
        if (m_mapObjectList.size > 1) {
            m_map!!.removeMapObject(m_mapObjectList[m_mapObjectList.size - 1])
            m_mapObjectList.removeAt(m_mapObjectList.size - 1)
        }
    }

}