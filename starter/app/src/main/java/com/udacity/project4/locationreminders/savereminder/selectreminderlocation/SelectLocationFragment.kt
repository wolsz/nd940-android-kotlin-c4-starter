package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*


class SelectLocationFragment : BaseFragment(), GoogleMap.OnPoiClickListener,
    GoogleMap.OnMapLongClickListener,
    OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var selectedLatLong: LatLng
    private lateinit var selectedPointOfInterest: PointOfInterest
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastKnownLocation: Location

    private val defaultLatLng = LatLng(37.42221469076899, -122.0840897022055)

    private val TAG = SelectLocationFragment::class.java.simpleName


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())


        _viewModel.isSelected.value = false

        binding.savePosFab.setOnClickListener {
            onLocationSelected()

        }


        return binding.root
    }


    private fun onLocationSelected() {

        _viewModel.reminderSelectedLocationStr.value = selectedPointOfInterest.name
        _viewModel.selectedPOI.value = selectedPointOfInterest
        _viewModel.latitude.value = selectedPointOfInterest.latLng.latitude
        _viewModel.longitude.value = selectedPointOfInterest.latLng.longitude

        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.setOnMapLongClickListener(this)
        map.setOnPoiClickListener(this)
        map.setMapStyle()

        enableMyLocation()
    }




    override fun onPoiClick(poi: PointOfInterest) {
        map.clear() // clearing any other places they might have selected before
        selectedPointOfInterest = poi
        selectedLatLong = poi.latLng
        val poiMarker = map.addMarker(
            MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
        )
        poiMarker.showInfoWindow()
        _viewModel.isSelected.value = true
    }

    override fun onMapLongClick(latLng: LatLng) {
        map.clear()

        selectedLatLong = latLng

        val snippet = String.format(
            Locale.getDefault(),
            "Lat: %1$.5f, Long: %2$.5f",
            latLng.latitude,
            latLng.longitude
        )

        selectedPointOfInterest = PointOfInterest(selectedLatLong, "poiId", snippet)

        val marker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
        marker.showInfoWindow()
        _viewModel.isSelected.value = true

    }

    private fun GoogleMap.setMapStyle() {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            this.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style
                )
            )
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }



    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isForegroundPermissionGranted()) {
            enableMap()
            checkLocationPermissions()

        } else {
            requestForegroundPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMap() {
        val zoomLevel = 15f

        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        val defPlace = Location(LocationManager.GPS_PROVIDER)
        defPlace.latitude = defaultLatLng.latitude
        defPlace.longitude = defaultLatLng.longitude

        try {
            val locationResult = fusedLocationClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) {
                if (it.isSuccessful) {
                    lastKnownLocation = it.result ?: defPlace
                    selectedLatLong =
                        LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                    selectedPointOfInterest = PointOfInterest(selectedLatLong, "", "")

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLong, zoomLevel))
                    map.addMarker(MarkerOptions().position(selectedLatLong))
                }
            }
        } catch (e: Exception) {

        }
    }

    private fun isForegroundPermissionGranted(): Boolean {

        return checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isBackgroundPermissionGranted(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED ==
                    checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }
    }

    private fun requestForegroundPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_PERMISSION_RESULT_CODE)
    }

    private fun requestBackgroundPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_BACKGROUND_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    fun checkLocationPermissions() {
        if (!isForegroundPermissionGranted()) {
            requestForegroundPermissions()
        } else if (!isBackgroundPermissionGranted()) {
            requestBackgroundPermissions()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult: ")

        when (requestCode) {
            REQUEST_FOREGROUND_PERMISSION_RESULT_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: ------  $requestCode no no -----------")
                    enableMap()
                    checkLocationPermissions()
                } else {
                    Snackbar.make(
                        requireView(),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            REQUEST_BACKGROUND_PERMISSIONS_REQUEST_CODE-> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkLocationPermissions()
                } else {
                    Snackbar.make(
                        requireView(),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

        }
    }

}

private const val REQUEST_FOREGROUND_PERMISSION_RESULT_CODE = 1
private const val REQUEST_BACKGROUND_PERMISSIONS_REQUEST_CODE = 2
