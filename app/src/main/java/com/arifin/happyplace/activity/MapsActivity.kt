package com.arifin.happyplace.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.arifin.happyplace.R
import com.arifin.happyplace.model.HappyPlaceModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mhappyPlaceDetailDetail: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        mhappyPlaceDetailDetail = intent.getParcelableExtra(MainActivity.HAPPY_PLACE_DETAILS)

        mhappyPlaceDetailDetail.let {
            if (it != null){
                setSupportActionBar(toolbar_map)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = it?.title

                toolbar_map.setNavigationOnClickListener {
                    onBackPressed()
                }

                val supportMapFragment: SupportMapFragment =
                    supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                supportMapFragment.getMapAsync(this)

            }

        }

    }

    override fun onMapReady(map: GoogleMap?) {
        val position = LatLng(mhappyPlaceDetailDetail!!.latitude,
            mhappyPlaceDetailDetail!!.longitude)

        map!!.addMarker(MarkerOptions().position(position).title(mhappyPlaceDetailDetail!!.location))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 20f)
        map.animateCamera(newLatLngZoom)
    }
}