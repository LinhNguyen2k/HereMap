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

import android.app.ListActivity
import android.os.Bundle
import android.view.View
import android.widget.*

import com.here.android.mpa.search.*

/*A list view to present DiscoveryResult */
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
        /*
         * An overlay layout will pop up to display some place details.To simplify the logic, this
         * layout is currently not being handled for screen rotation event.It disappears if the
         * screen is being rotated.
         */
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

    /* Retrieve details of the place selected */
    public override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val result = MapFragmentView.s_discoverResultList!![position]
        if (result.resultType == DiscoveryResult.ResultType.PLACE) {
            /* Fire the PlaceRequest */
            val placeLink = result as PlaceLink
            val placeRequest = placeLink.detailsRequest
            placeRequest!!.execute(m_placeResultListener)
        } else if (result.resultType == DiscoveryResult.ResultType.DISCOVERY) {
            /*
             * Another DiscoveryRequest object can be obtained by calling DiscoveryLink.getRequest()
             */
            Toast.makeText(this, "This is a DiscoveryLink result", Toast.LENGTH_SHORT).show()
        }
    }

    private val m_placeResultListener: ResultListener<Place> = object : ResultListener<Place> {

        override fun onCompleted(place: Place?, errorCode: ErrorCode?) {
            if (errorCode == ErrorCode.NONE) {
                /*
                 * No error returned,let's show the name and location of the place that just being
                 * selected.Additional place details info can be retrieved at this moment as well,
                 * please refer to the HERE Android SDK API doc for details.
                 */
                m_placeDetailLayout!!.visibility = View.VISIBLE
                m_placeName!!.text = place!!.name
                val geoCoordinate = place.location!!
                    .coordinate
                m_placeLocation!!.text = geoCoordinate.toString()
            } else {
                Toast.makeText(applicationContext,
                    "ERROR:Place request returns error: $errorCode", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}