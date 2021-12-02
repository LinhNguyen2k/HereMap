package com.example.heremap.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.here.android.mpa.common.GeoCoordinate

class MapViewModel : ViewModel() {
    var startLocation = MutableLiveData<GeoCoordinate>()
    var endLocation = MutableLiveData<GeoCoordinate>()
    var typeDraw = MutableLiveData<Int>()
    var typeVehicle = MutableLiveData<Int>()

    fun setStartLocation(location: GeoCoordinate) {
        startLocation.value = location
    }

    fun setEndLocation(location: GeoCoordinate) {
        endLocation.value = location
    }
    fun setTypeDraw(isCheck : Int){
        typeDraw.value = isCheck
    }
    fun setTypeVehicle(type: Int) {
        typeVehicle.value = type
    }
}