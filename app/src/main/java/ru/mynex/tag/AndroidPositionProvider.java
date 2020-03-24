/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
 * Copyright 2020 Alexey Voloshin (smartbyter@yandex.ru)
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
package ru.mynex.tag;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class AndroidPositionProvider extends PositionProvider implements LocationListener {

    private LocationManager locationManager;
    private String provider;
    private GpsStatus mStatus;

    public AndroidPositionProvider(Context context, PositionListener listener) {
        super(context, listener);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        provider = getProvider(preferences.getString(MainFragment.KEY_ACCURACY, "medium"));
    }

    @SuppressLint("MissingPermission")
    public void startUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    provider, distance > 0 || angle > 0 ? MINIMUM_INTERVAL : interval, 0, this);
        } catch (RuntimeException e) {
            listener.onPositionError(e);
        }
    }

    public void stopUpdates() {
        locationManager.removeUpdates(this);
    }

    @SuppressLint("MissingPermission")
    public void requestSingleLocation() {
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (location != null) {
                Log.d("ru.mynex.tag - onPositionUpdate", "1");
                listener.onPositionUpdate(new Data(context, location));
            } else {
                locationManager.requestSingleUpdate(provider, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Log.d("ru.mynex.tag - onPositionUpdate", "2");
                        listener.onPositionUpdate(new Data(context, location));
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }

                    // // @Override
                    // public void onGpsStatusChanged(int event) {
                    //     Log.d("ru.mynex.tag - onGpsStatusChanged", "on");
                    //     mStatus = locationManager.getGpsStatus(mStatus);
                    //     switch (event) {
                    //         case GpsStatus.GPS_EVENT_STARTED:
                    //             // Do Something with mStatus info
                    //             break;
                    
                    //         case GpsStatus.GPS_EVENT_STOPPED:
                    //             // Do Something with mStatus info
                    //             break;
                    
                    //         case GpsStatus.GPS_EVENT_FIRST_FIX:
                    //             // Do Something with mStatus info
                    //             break;
                    
                    //         case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    //             // Do Something with mStatus info
                    //             break;
                    //     }
                    
                    // }

                }, Looper.myLooper());
            }
        } catch (RuntimeException e) {
            listener.onPositionError(e);
        }
    }

    private static String getProvider(String accuracy) {
        switch (accuracy) {
            case "high":
                return LocationManager.GPS_PROVIDER;
            case "low":
                return LocationManager.PASSIVE_PROVIDER;
            default:
                return LocationManager.NETWORK_PROVIDER;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("ru.mynex.tag - onLocationChanged", "1");
        // processLocation(location, locationManager);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }


}
