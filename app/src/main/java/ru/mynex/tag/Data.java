/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Date;
import java.time.Instant;

public class Data {

    Context context;

    private long id;
    private String deviceId;
    private Date time;
    private double latitude;
    private double longitude;
    private double altitude;
    private int status;
    private int sats;
    private double speed;
    private double course;
    private double accuracy;
    private double battery;
    private int gsmStrength;
    private boolean mock;

    protected SharedPreferences preferences;

    public Data() {}

    public Data(Context context, Location location) {

        this.context = context;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.deviceId = preferences.getString(MainFragment.KEY_DEVICE, "undefined");

        // Log.d("ru.mynex.tag - before", "Date6");

        time = new Date();
        // time = new Date(Instant.now());
        // time = new Date(Instant.now().getEpochSecond());
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            altitude = location.getAltitude();
            speed = location.getSpeed() * 3.6; // speed m/s -> km/h
            course = location.getBearing();
            accuracy = location.getAccuracy();
        } else {
            latitude = 0;
            longitude = 0;
            altitude = 0;
            speed = 0;
            course = 0;
            accuracy = 0;
        }
        status = 0;
        sats = 0;
        battery = getBatteryLevel();
        gsmStrength = 0;
        if (location != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.mock = location.isFromMockProvider();
        }
    }

    private double getBatteryLevel() {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
            return (level * 100.0) / scale;
        }
        return 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public int getSats() {
        return sats;
    }

    public void setSats(int sats) {
        this.sats = sats;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getCourse() {
        return course;
    }

    public void setCourse(double course) {
        this.course = course;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getBattery() {
        return battery;
    }

    public void setBattery(double battery) {
        this.battery = battery;
    }

    public int getGsmStrength() {
        return gsmStrength;
    }

    public void setGsmStrength(int gsmStrength) {
        this.gsmStrength = gsmStrength;
    }

    public boolean getMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

}
