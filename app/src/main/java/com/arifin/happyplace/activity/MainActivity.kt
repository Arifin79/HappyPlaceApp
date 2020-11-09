package com.arifin.happyplace.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arifin.happyplace.database.DataBaseHandler
import com.arifin.happyplace.R
import com.arifin.happyplace.adapter.HappyPlaceAdapter
import com.arifin.happyplace.model.HappyPlaceModel
import com.arifin.happyplace.util.SwipeToDeleteCallBack
import com.arifin.happyplace.util.SwipeToEditCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    companion object {
        private var ADD_HAPPY_ACTIVITY_REQUEST_CODE = 1
        const val HAPPY_PLACE_DETAILS = "extra_place_details"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab_add_happy_place.setOnClickListener {
            val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent, ADD_HAPPY_ACTIVITY_REQUEST_CODE)
        }

//        untuk get data dari DataBaseHandler
        getHappyPlacesListFromLocalDB()

    }

    private fun getHappyPlacesListFromLocalDB() {
//        variable supaya databasenya bisa kita gunakan di mainActivity
        val dbHandler = DataBaseHandler(this)
//        di gunakan untuk menjalankan aksi getyang berasal dari database
        val getHappyPlacesList: ArrayList<HappyPlaceModel> = dbHandler.getHappyPlaceList()

        if (getHappyPlacesList.size > 0) {
            rv_happy_place_list.visibility = View.VISIBLE
            tv_no_records.visibility = View.GONE
            setupHappyPlacesRecyclerView(getHappyPlacesList)
        } else {
            rv_happy_place_list.visibility = View.GONE
            tv_no_records.visibility = View.VISIBLE
        }
    }

    //    fungsion ini bertujuan untuk create recyclerView di dalam MainActivity
    private fun setupHappyPlacesRecyclerView(happyPlaceList: ArrayList<HappyPlaceModel>) {
//    untuk mendeteksi data ketika ada perubahan seperti adad data baru
        rv_happy_place_list.layoutManager = LinearLayoutManager(this)
//    buat trigger ketika ada databaru
        rv_happy_place_list.setHasFixedSize(true)

//    untuk menjalankan adapter kita ke dalam activty sehingga recyclerView bisa berjalan dengan seharusnya
        val placesAdapter = HappyPlaceAdapter(this, happyPlaceList)
        rv_happy_place_list.adapter = placesAdapter

        placesAdapter.setOnClickListener(object : HappyPlaceAdapter.OnClickListener {
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity, HappyDetailActivity::class.java)
                intent.putExtra(HAPPY_PLACE_DETAILS, model)
                startActivity(intent)
            }
        })

        val editSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_happy_place_list.adapter as HappyPlaceAdapter
                adapter.notifyEditItem(
                    this@MainActivity,
                    viewHolder.adapterPosition,
                    ADD_HAPPY_ACTIVITY_REQUEST_CODE
                )
            }
        }

        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(rv_happy_place_list)

        val deleteSwipeHandler = object : SwipeToDeleteCallBack(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_happy_place_list.adapter as HappyPlaceAdapter
                adapter.removeAt(viewHolder.adapterPosition)

                getHappyPlacesListFromLocalDB()
            }
        }

        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(rv_happy_place_list)

    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_HAPPY_ACTIVITY_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                getHappyPlacesListFromLocalDB()
            } else{
                Log.e("Activity", "Cancelled or Back Pressed")
            }
        }
    }
}