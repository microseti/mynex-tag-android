/*
 * Copyright 2013 - 2019 Anton Tananaev (anton@traccar.org)
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
 
import java.util.Date;
import java.time.Instant;
 
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus;
import android.location.GpsSatellite;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
 
public class PositionProvider extends Thread implements GpsStatus.Listener, LocationListener  {
 
    protected static final int MINIMUM_INTERVAL = 1000;
    protected static final int UPDATE_INTERVAL = 1000;
    int count = 0;
    private String provider;
    protected SharedPreferences preferences;
 
    protected final Context context;
    private LocationManager locationManager;
    protected long interval;
    protected double distance;
    protected double angle;
    private boolean stopFlag = false;
    private long lastNow;
    private Location lastLocation = null;
    private int satellites = 0;
    private int satellitesInFix = 0;
    private int counter = 0;
    private int locationStatus = 0;

    private TelephonyManager mTelephonyManager;
    private MyPhoneStateListener mPhoneStatelistener;   
    private int gsmSignalStrength = 0;


    class MyPhoneStateListener extends PhoneStateListener {
        
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            gsmSignalStrength = signalStrength.getLevel() + 1; // (0-4) + 1
        }

    }

    private void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Log.d("ru.mynex.tag - sleep ms", String.valueOf(ms));
            Log.d("ru.mynex.tag - sleep exception", ex.getMessage());
        }
    }

    public void run() {
        for(;;) {

            long now = Instant.now().getEpochSecond();

            if (now != lastNow) {
                boolean updateFlag = false;
                counter++;
                if (now % interval == 0) {
                    counter = 0;
                    updateFlag = true;
                }
                
                Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                if (location != null && (lastLocation == null
                        || distance > 0 && location.distanceTo(lastLocation) >= distance
                        || angle > 0 && Math.abs(location.getBearing() - lastLocation.getBearing()) >= angle)) {

                    updateFlag = true;
                    lastLocation = location;
                }
                
                boolean lstatus = satellitesInFix >= 3;
                if ((locationStatus == 0 && lstatus) || (locationStatus == 1 && !lstatus)) {
                    updateFlag = true;
                }
                locationStatus = lstatus ? 1 : 0;

                if (updateFlag) {
                    Data data = new Data(context, lastLocation);
                    data.setStatus(locationStatus);
                    data.setSats(satellitesInFix);
                    data.setGsmStrength(gsmSignalStrength);
                    listener.onPositionUpdate(data);
                }
            }

            lastNow = now;

            if (stopFlag) {
                stopFlag = false;
                break;
            }
            sleepMs(100); // 100ms - это нормально, так как идет поиск начала секунды
        }
    }

    public interface PositionListener {
        void onPositionUpdate(Data data);
        void onPositionError(Throwable error);
    }

    protected final PositionListener listener;

    @Override
    public void onGpsStatusChanged(int event) {
        satellites = 0;
        satellitesInFix = 0;
        int timetofix = locationManager.getGpsStatus(null).getTimeToFirstFix();
        for (GpsSatellite sat : locationManager.getGpsStatus(null).getSatellites()) {
            if(sat.usedInFix()) {
                satellitesInFix++;              
            }
            satellites++;
        }
    }

    @SuppressLint("MissingPermission")
    public void startUpdates() {
        try {
            
            locationManager.requestLocationUpdates(provider, UPDATE_INTERVAL, 0, this);
            stopFlag = false;
        } catch (RuntimeException e) {
            listener.onPositionError(e);
        }
    }

    public void stopUpdates() {
        locationManager.removeGpsStatusListener(this);
        locationManager.removeUpdates(this);
        stopFlag = true;
    }

    public PositionProvider(Context context, PositionListener listener) {

        this.context = context;
        this.listener = listener;

        mPhoneStatelistener = new MyPhoneStateListener();
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        provider = getProvider(preferences.getString(MainFragment.KEY_ACCURACY, "medium"));
        locationManager.addGpsStatusListener(this);

        interval = Long.parseLong(preferences.getString(MainFragment.KEY_INTERVAL, "120"));
        distance = Integer.parseInt(preferences.getString(MainFragment.KEY_DISTANCE, "0"));
        angle = Integer.parseInt(preferences.getString(MainFragment.KEY_ANGLE, "0"));

        locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

        lastNow = Instant.now().getEpochSecond();

        start(); // run thread

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
        // Log.d("ru.mynex.tag - onLocationChanged", "1");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        // Log.d("ru.mynex.tag - onStatusChanged", String.valueOf(status));
        // if (provider.equals(LocationManager.GPS_PROVIDER)) {
        // } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
        // }
        // Log.d("ru.mynex.tag - onStatusChanged provider", provider);
        // Log.d("ru.mynex.tag - onStatusChanged status", String.valueOf(status));
        // locationStatus = status > 0 ? 1 : 0;

    }

    @Override
    public void onProviderEnabled(String provider) {
        // Log.d("ru.mynex.tag - onProviderEnabled", provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Log.d("ru.mynex.tag - onProviderDisabled", provider);
    }

}
