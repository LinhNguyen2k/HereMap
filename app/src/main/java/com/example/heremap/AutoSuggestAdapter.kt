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

import android.content.Context
import android.graphics.Color
import com.here.android.mpa.search.AutoSuggest
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.here.android.mpa.search.AutoSuggestPlace
import com.here.android.mpa.search.AutoSuggestQuery
import com.here.android.mpa.search.AutoSuggestSearch

class AutoSuggestAdapter(
    context: Context?,
    resource: Int,
    private val m_resultsList: List<AutoSuggest>
) : ArrayAdapter<AutoSuggest>(
    context!!, resource, m_resultsList) {
    override fun getCount(): Int {
        return m_resultsList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val autoSuggest = getItem(position)
        if (convertView == null) {
            convertView =
                LayoutInflater.from(context).inflate(R.layout.result_autosuggest_list_item,
                    parent, false)
        }
        var tv: TextView? = null
        tv = convertView!!.findViewById(R.id.header)
        tv.setBackgroundColor(Color.DKGRAY)

        // set title
        tv = convertView.findViewById(R.id.title)
        tv.text = autoSuggest.title

        // set highlightedTitle
        tv = convertView.findViewById(R.id.highlightedTitle)
        tv.text =
            HtmlCompat.fromHtml(autoSuggest.highlightedTitle, HtmlCompat.FROM_HTML_MODE_LEGACY)

        // set request URL
//        tv = convertView.findViewById(R.id.url)
//        tv.text = "Url: " + autoSuggest.url

        // set Type
        tv = convertView.findViewById(R.id.type)
        tv.text = "Type: " + autoSuggest.type.name
        when (autoSuggest.type) {
            AutoSuggest.Type.PLACE -> {
                val autoSuggestPlace = autoSuggest as AutoSuggestPlace
                // set vicinity
                tv = convertView.findViewById(R.id.vicinity)
                tv.visibility = View.VISIBLE
                if (autoSuggestPlace.vicinity != null) {
                    tv.text = "Vicinity: " + autoSuggestPlace.vicinity
                } else {
                    tv.text = "Vicinity: null"
                }

                // set category
                tv = convertView.findViewById(R.id.category)
                tv.visibility = View.VISIBLE
                if (autoSuggestPlace.category != null) {
                    tv.text = "Category: " + autoSuggestPlace.category
                } else {
                    tv.text = "Category: null"
                }

                // set position
                tv = convertView.findViewById(R.id.position)
                tv.visibility = View.VISIBLE
                if (autoSuggestPlace.position != null) {
                    tv.text = "Position: " + autoSuggestPlace.position.toString()
                } else {
                    tv.text = "Position: null"
                }

                // set boundaryBox
//                tv = convertView.findViewById(R.id.boundaryBox)
//                tv.visibility = View.VISIBLE
                if (autoSuggestPlace.boundingBox != null) {
                    tv.text = "BoundaryBox: " + autoSuggest.boundingBox.toString()
                } else {
                    tv.text = "BoundaryBox: null"
                }
            }
            AutoSuggest.Type.QUERY -> {
                val autoSuggestQuery = autoSuggest as AutoSuggestQuery
                // set completion
                tv = convertView.findViewById(R.id.vicinity)
                tv.text = "Completion: " + autoSuggestQuery.queryCompletion

                // set category
                tv = convertView.findViewById(R.id.category)
                tv.visibility = View.GONE

                // set position
                tv = convertView.findViewById(R.id.position)
                tv.visibility = View.GONE

//                // set boundaryBox
//                tv = convertView.findViewById(R.id.boundaryBox)
//                tv.visibility = View.GONE
            }
            AutoSuggest.Type.SEARCH -> {
                val autoSuggestSearch = autoSuggest as AutoSuggestSearch
                // set vicinity
                tv = convertView.findViewById(R.id.vicinity)
                tv.visibility = View.GONE

                // set category
                tv = convertView.findViewById(R.id.category)
                tv.visibility = View.VISIBLE
                if (autoSuggestSearch.category != null) {
                    tv.text = "Category: " + autoSuggestSearch.category
                } else {
                    tv.text = "Category: null"
                }

                // set position
                tv = convertView.findViewById(R.id.position)
                tv.visibility = View.VISIBLE
                if (autoSuggestSearch.position != null) {
                    tv.text = "Position: " + autoSuggestSearch.position.toString()
                } else {
                    tv.text = "Position: null"
                }

                // set boundaryBox
//                tv = convertView.findViewById(R.id.boundaryBox)
//                tv.visibility = View.VISIBLE
//                if (autoSuggestSearch.boundingBox != null) {
//                    tv.text = "BoundaryBox: " + autoSuggestSearch.boundingBox.toString()
//                } else {
//                    tv.text = "BoundaryBox: null"
//                }
            }
            else -> {
            }
        }
        return convertView
    }

    override fun getItem(position: Int): AutoSuggest {
        return m_resultsList[position]
    }
}