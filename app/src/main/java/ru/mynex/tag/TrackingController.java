/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class TrackingController implements PositionProvider.PositionListener, NetworkManager.NetworkHandler {

    private static final String TAG = TrackingController.class.getSimpleName();
    private static final int RETRY_DELAY = 30 * 1000;
    private static final int WAKE_LOCK_TIMEOUT = 120 * 1000;

    private boolean isOnline;
    private boolean isWaiting;

    private Context context;
    private Handler handler;
    private SharedPreferences preferences;

    private String saddr;
    private boolean buffer;

    private PositionProvider positionProvider;
    private DatabaseHelper databaseHelper;
    private NetworkManager networkManager;
    private ConnectionManager connectionManager = new ConnectionManager();

    public TrackingController(Context context) {
        Log.d("ru.mynex.tag - TrackingController", "Create!");
        this.context = context;
        handler = new Handler();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        positionProvider = new PositionProvider(context, this);
        databaseHelper = new DatabaseHelper(context);
        networkManager = new NetworkManager(context, this);
        isOnline = networkManager.isOnline();

        saddr = preferences.getString(MainFragment.KEY_SADDR, context.getString(R.string.settings_saddr_default_value));
        buffer = preferences.getBoolean(MainFragment.KEY_BUFFER, true);

        
    }
    
    public void start() {

        networkManager.start();
        String deviceId = preferences.getString(MainFragment.KEY_DEVICE, "undefined");
        long interval = Long.parseLong(preferences.getString(MainFragment.KEY_INTERVAL, "300"));
        String ident = ProtocolFormatter.formatIdent(deviceId, interval);
        connectionManager.startx(saddr, ident);
        
        if (isOnline) {
            read();
        }
        try {
            positionProvider.startUpdates();
        } catch (SecurityException e) {
            Log.w(TAG, e);
        }
    }

    public void stop() {
        connectionManager.stopx();
        networkManager.stop();

        try {
            positionProvider.stopUpdates();
        } catch (SecurityException e) {
            Log.w(TAG, e);
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onPositionUpdate(Data data) {
        log("ru.mynex.tag - onPositionUpdate", data);
        Log.d("ru.mynex.tag - onPositionUpdate", "s1");
        // StatusActivity.addMessage("Hello world");

        StatusActivity.addMessage(context.getString(R.string.status_location_update));
        Log.d("ru.mynex.tag - onPositionUpdate", "s2");
        if (data != null) {
            if (buffer) {
                // Log.d("ru.mynex.tag - onPositionUpdate", "write");
                write(data);
            } else {
                // Log.d("ru.mynex.tag - onPositionUpdate", "send");
                send(data);
            }
        }
    }

    @Override
    public void onPositionError(Throwable error) {
    }

    @Override
    public void onNetworkUpdate(boolean isOnline) {
        int message = isOnline ? R.string.status_network_online : R.string.status_network_offline;
        StatusActivity.addMessage(context.getString(message));
        if (!this.isOnline && isOnline) {
            read();
        }
        this.isOnline = isOnline;
    }

    //
    // State transition examples:
    //
    // write -> read -> send -> delete -> read
    //
    // read -> send -> retry -> read -> send
    //

    private void log(String action, Data data) {
        if (data != null) {
            action += " (" +
                    "id:" + data.getId() +
                    " time:" + data.getTime().getTime() / 1000 +
                    " bat:" + data.getBattery() +
                    " gsm:" + data.getGsmStrength() +
                    " lat:" + data.getLatitude() +
                    " lon:" + data.getLongitude() + ")";
        }
        Log.d(TAG, action);
    }

    private void write(Data data) {
        log("write", data);
        databaseHelper.insertDataAsync(data, new DatabaseHelper.DatabaseHandler<Void>() {
            @Override
            public void onComplete(boolean success, Void result) {
                if (success) {
                    if (isOnline && isWaiting) {
                        read();
                        isWaiting = false;
                    }
                }
            }
        });
    }

    private void read() {
        log("read", null);
        databaseHelper.selectDataAsync(new DatabaseHelper.DatabaseHandler<Data>() {
            @Override
            public void onComplete(boolean success, Data result) {
                if (success) {
                    if (result != null) {
                        if (result.getDeviceId().equals(preferences.getString(MainFragment.KEY_DEVICE, null))) {
                            send(result);
                        } else {
                            delete(result);
                        }
                    } else {
                        isWaiting = true;
                    }
                } else {
                    retry();
                }
            }
        });
    }

    private void delete(Data data) {
        log("delete", data);
        databaseHelper.deleteDataAsync(data.getId(), new DatabaseHelper.DatabaseHandler<Void>() {
            @Override
            public void onComplete(boolean success, Void result) {
                if (success) {
                    read();
                } else {
                    retry();
                }
            }
        });
    }

    private void send(final Data data) {
        log("ru.mynex.tag - send", data);
        
        String pack = ProtocolFormatter.formatPackage(data, null);

        // Log.d(TAG, "ru.mynex.tag - ident:" + ident);
        // Log.d("ru.mynex.tag - pack", pack);

        // delete(data);

        ConnectionManager.sendRequestAsync(pack, new ConnectionManager.RequestHandler() {
            @Override
            public void onComplete(boolean success) {
                if (success) {
                    if (buffer) {
                        delete(data);
                    }
                } else {
                    StatusActivity.addMessage(context.getString(R.string.status_send_fail));
                    if (buffer) {
                        retry();
                    }
                }
            }
        });
    }

    private void retry() {
        log("retry", null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isOnline) {
                    read();
                }
            }
        }, RETRY_DELAY);
    }

}
