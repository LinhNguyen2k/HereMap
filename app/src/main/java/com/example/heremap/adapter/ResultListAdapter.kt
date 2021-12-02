
package com.example.heremap.adapter
import android.content.Context
import com.here.android.mpa.search.DiscoveryResult
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.heremap.R

class ResultListAdapter(
    context: Context?,
    resource: Int,
    private val m_discoveryResultList: List<DiscoveryResult>
) : ArrayAdapter<DiscoveryResult?>(
    context!!, resource, m_discoveryResultList) {
    override fun getCount(): Int {
        return m_discoveryResultList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val discoveryResult = m_discoveryResultList[position]
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.result_list_item,
                parent, false)
        }

        var tv = convertView!!.findViewById<View>(R.id.name) as TextView
        tv.text = discoveryResult.title
        tv = convertView.findViewById<View>(R.id.vicinity) as TextView
        tv.text = String.format("Vicinity: %s", discoveryResult.vicinity)
        return convertView
    }
}