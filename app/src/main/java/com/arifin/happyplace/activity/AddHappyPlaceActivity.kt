package com.arifin.happyplace.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import com.arifin.happyplace.R
import com.arifin.happyplace.database.DataBaseHandler
import com.arifin.happyplace.model.GetAddressFromLasLng
import com.arifin.happyplace.model.HappyPlaceModel
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    private var cal = Calendar.getInstance()
    private var saveImageToInternalStorage: Uri? = null
    private var mHappyPlaceDetails: HappyPlaceModel? = null
    private lateinit var dateListener: DatePickerDialog.OnDateSetListener
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    companion object{
        private const val GALERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)

        mHappyPlaceDetails =
            intent.getParcelableExtra(MainActivity.HAPPY_PLACE_DETAILS)

        mHappyPlaceDetails.let {
            setSupportActionBar(tool_add_place)
            supportActionBar?.setTitle("Edit Happy Place")
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            tool_add_place.setNavigationOnClickListener {
                onBackPressed()
            }

            if (!Places.isInitialized()){
                Places.initialize(
                    this@AddHappyPlaceActivity,
                    resources.getString(R.string.google_maps_key)
                )
            }

        }

        dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateView()
        }


        edt_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)
        edt_location.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        tv_select_current_location.setOnClickListener(this)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        mHappyPlaceDetails.let {
            if (it != null){
                saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
                iv_place_image.setImageURI(saveImageToInternalStorage)
                edt_title.setText(it!!.title)
                edt_description.setText(it!!.description)
                edt_date.setText(it!!.date)
                edt_location.setText(it!!.location)
                mLatitude = it.latitude
                mLongitude = it.longitude

                btn_save.text = "UPDATE"
            }
        }

    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)
    }

    private fun updateView() {
        val format = "dd-MM-yyyy"
        val dataformat = SimpleDateFormat(format, Locale.getDefault())
        edt_date.setText(dataformat.format(cal.time).toString())
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.edt_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val dialogItem = arrayOf(
                    "Select Photo from Galery",
                    "Take Photo With Camera"
                )

                pictureDialog.setItems(dialogItem) { _, which ->
                    when (which) {
                        0 -> choosePhotoFromGalery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()

            }
            R.id.btn_save -> {
                when {
                    edt_title.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title",
                        Toast.LENGTH_SHORT).show()
                    }
                    edt_description.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description",
                            Toast.LENGTH_SHORT).show()
                    }
                    edt_location.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please select location",
                            Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please add image",
                            Toast.LENGTH_SHORT).show()
                    }

                    else -> {

                        val happyPlaceModel = HappyPlaceModel(
                            if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            edt_title.text.toString(),
                            saveImageToInternalStorage.toString(),
                            edt_description.text.toString(),
                            edt_date.text.toString(),
                            edt_location.text.toString(),
                            mLatitude,
                            mLongitude
                        )

                        val dbHandler = DataBaseHandler(this)
                        if (mHappyPlaceDetails == null){
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        } else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }
            R.id.edt_location -> {
                try {
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )

                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        "Your location provider is turned off. Please turn it on.",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {
                    Dexter.withActivity(this)
                        .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ).withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                if (report!!.areAllPermissionsGranted()) {

                                    requestNewLocationData()
                                }
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                            ) {
                                tryForPermissions()
                            }
                        }).onSameThread().check()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val mLoacationRequest = LocationRequest()
        mLoacationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLoacationRequest.interval = 0
        mLoacationRequest.fastestInterval = 0
        mLoacationRequest.numUpdates = 1

        mFusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLoacationRequest, mLoacationCallBack,
            Looper.myLooper()
        )
    }

    private val mLoacationCallBack = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")

            val addressTask =
                GetAddressFromLasLng(this@AddHappyPlaceActivity, mLatitude, mLongitude)

            addressTask.setAddressListener(object : GetAddressFromLasLng.AddressListener {
                override fun onAddressFound(address: String?){
                    Log.e("Address ::", "" + address)
                    edt_location.setText(address)
                }
                override fun onError() {
                    Log.e("Get Address ::", "Somethingis wrong...")
                }
            })

            addressTask.getAddress()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALERY) {
                if (data != null) {
                    val contentUri = data.data
                    try {
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentUri)
                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        iv_place_image.setImageBitmap(selectedImageBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Failed to Load the image from",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (requestCode == CAMERA) {
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
                saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)
                Log.e("Save Image: ", "path :: $saveImageToInternalStorage")
                iv_place_image.setImageBitmap(thumbnail)

            }else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)

                edt_location.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        } else if (requestCode == Activity.RESULT_CANCELED){
            Log.e("Cancelled", "Cancelled")
        }

    }

    private fun takePhotoFromCamera() {
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(p0: MultiplePermissionsReport) {
                if (p0.areAllPermissionsGranted()) {
                    val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(galleryIntent, CAMERA)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?
            ) {
                tryForPermissions()
            }
        }).onSameThread().check()
    }

    private fun choosePhotoFromGalery() {
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(p0: MultiplePermissionsReport) {
                if (p0.areAllPermissionsGranted()) {
                    val galleryIntent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, GALERY)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?
            ) {

                tryForPermissions()
            }
        }).onSameThread().check()
    }


    fun tryForPermissions() {
        AlertDialog.Builder(this).setMessage(
            "" +
                    "It look like you have turned of permission required" +
                    "For this feature. It can be enable under this" +
                    "Application settings"
        )
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}