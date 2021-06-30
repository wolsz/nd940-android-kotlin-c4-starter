package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    private val TAG = SaveReminderFragment::class.java.simpleName


    private fun checkLocationPermissions() {
        if (isForegroundPermissionGranted() && isBackgroundPermissionGranted()) {
            checkDeviceLocationSettingsAndSave()
        } else if (!isForegroundPermissionGranted()) {
            requestForegroundPermissions()
        } else if (!isBackgroundPermissionGranted()) {
            requestBackgroundPermissions()
        }
    }

    private fun requestForegroundPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_PERMISSION_RESULT_CODE
        )
    }

    private fun requestBackgroundPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_BACKGROUND_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun isForegroundPermissionGranted(): Boolean {

        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isBackgroundPermissionGranted(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
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
                    checkLocationPermissions()
                } else {
                    Snackbar.make(
                        requireView(),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            REQUEST_BACKGROUND_PERMISSIONS_REQUEST_CODE -> {
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

    @SuppressLint("MissingPermission")
    private fun doingGeofencingNow(remindData: ReminderDataItem) {

        val geofence = Geofence.Builder()
            .setRequestId(remindData.id)
            .setCircularRegion(
                remindData.latitude!!,
                remindData.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

//        Log.i("TEST", "added geofences" + remindData.latitude + " " + remindData.longitude)

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.i("TEST", "added geofences" + remindData.latitude + " " + remindData.longitude)
                _viewModel.saveReminder(remindData)

            }
            addOnFailureListener {
                Log.i("TEST", "failed geofences")

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            val locationManager =
                requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                goToLocationSelection()
            } else {
                checkDeviceLocationSettingsAndSave()
            }

        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value


            reminderDataItem = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude
            )

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                checkLocationPermissions()
            }
//            val isaValidReminder = _viewModel.validateAndSaveReminder(reminderDataItem)

        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun goToLocationSelection() {
        _viewModel.navigationCommand.value =
            NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
    }

    private fun checkDeviceLocationSettingsAndSave(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    this.startIntentSenderForResult(
                        exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(
                        TAG,
                        "checkDeviceLocationSettingsAndSave: Error getting location settings resolution: " + sendEx.message
                    )
                }
            } else {
                Log.i(TAG, "checkDeviceLocationSettingsAndSave: Snackbar")
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndSave()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
//                goToLocationSelection()
                doingGeofencingNow(reminderDataItem)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndSave(false)
        }
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT =
            "RemindersActivity.savereminder.action.ACTION_GEOFENCE_EVENT"
        const val GEOFENCE_RADIUS_IN_METERS = 100f
    }

}

private const val REQUEST_FOREGROUND_PERMISSION_RESULT_CODE = 1
private const val REQUEST_BACKGROUND_PERMISSIONS_REQUEST_CODE = 2

const val REQUEST_TURN_DEVICE_LOCATION_ON = 5