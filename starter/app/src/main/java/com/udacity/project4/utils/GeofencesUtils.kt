package com.udacity.project4.utils

import android.content.res.Resources
import com.google.android.gms.location.GeofenceStatusCodes
import com.udacity.project4.R

/**
 * Returns the error string for a geofencing error code.
 */
fun errorMessage(resources: Resources, errorCode: Int): String {

    return when (errorCode) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
            R.string.geofence_not_available
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
            R.string.geofence_too_many_geofences
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
            R.string.geofence_too_many_pending_intents
        )
        else -> resources.getString(R.string.unknown_geofence_error)
    }
}