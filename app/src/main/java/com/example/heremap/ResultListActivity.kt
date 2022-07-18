package com.example.heremap

import android.app.ListActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.heremap.adapter.ResultListAdapter
import com.example.heremap.fragment.MapFragmentView

import com.here.android.mpa.search.*

class ResultListActivity : ListActivity() {
    private var m_placeDetailLayout: LinearLayout? = null
    private var m_placeName: TextView? = null
    private var m_placeLocation: TextView? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_list)
        initUIElements()
        val listAdapter = ResultListAdapter(this,
            android.R.layout.simple_list_item_1, MapFragmentView.s_discoverResultList!!)
        setListAdapter(listAdapter)
    }

    private fun initUIElements() {
        m_placeDetailLayout = findViewById<View>(R.id.placeDetailLayout) as LinearLayout
        m_placeDetailLayout!!.visibility = View.GONE
        m_placeName = findViewById<View>(R.id.placeName) as TextView
        m_placeLocation = findViewById<View>(R.id.placeLocation) as TextView
        val closePlaceDetailButton = findViewById<View>(R.id.closeLayoutButton) as Button
        closePlaceDetailButton.setOnClickListener {
            if (m_placeDetailLayout!!.visibility == View.VISIBLE) {
                m_placeDetailLayout!!.visibility = View.GONE
            }
        }
    }

    public override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val result = MapFragmentView.s_discoverResultList!![position]
        if (result.resultType == DiscoveryResult.ResultType.PLACE) {
            val placeLink = result as PlaceLink
            val placeRequest = placeLink.detailsRequest
            placeRequest!!.execute(m_placeResultListener)
        } else if (result.resultType == DiscoveryResult.ResultType.DISCOVERY) {

        }
    }

    private val m_placeResultListener: ResultListener<Place> = object : ResultListener<Place> {

        override fun onCompleted(place: Place?, errorCode: ErrorCode?) {
            if (errorCode == ErrorCode.NONE) {

                m_placeDetailLayout!!.visibility = View.VISIBLE
                m_placeName!!.text = place!!.name
                val geoCoordinate = place.location!!.coordinate
                m_placeLocation!!.text = geoCoordinate.toString()
            } else {
                Toast.makeText(applicationContext,
                    "ERROR:Place request returns error: $errorCode", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}