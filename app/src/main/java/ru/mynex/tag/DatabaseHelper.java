/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import java.util.Date;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "mynex_tag.db";

    public interface DatabaseHandler<T> {
        void onComplete(boolean success, T result);
    }

    private static abstract class DatabaseAsyncTask<T> extends AsyncTask<Void, Void, T> {

        private DatabaseHandler<T> handler;
        private RuntimeException error;

        public DatabaseAsyncTask(DatabaseHandler<T> handler) {
            this.handler = handler;
        }

        @Override
        protected T doInBackground(Void... params) {
            try {
                return executeMethod();
            } catch (RuntimeException error) {
                this.error = error;
                return null;
            }
        }

        protected abstract T executeMethod();

        @Override
        protected void onPostExecute(T result) {
            handler.onComplete(error == null, result);
        }
    }

    private SQLiteDatabase db;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "deviceId TEXT," +
                "time INTEGER," +
                "latitude REAL," +
                "longitude REAL," +
                "altitude REAL," +
                "speed REAL," +
                "course REAL," +
                "accuracy REAL," +
                "status INTEGER," +
                "sats INTEGER," +
                "battery REAL," +
                "mqua INTEGER," +
                "mock INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS data;");
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS data;");
        onCreate(db);
    }

    public void insertData(Data data) {
        ContentValues values = new ContentValues();
        values.put("deviceId", data.getDeviceId());
        values.put("time", data.getTime().getTime());
        values.put("latitude", data.getLatitude());
        values.put("longitude", data.getLongitude());
        values.put("altitude", data.getAltitude());
        values.put("speed", data.getSpeed());
        values.put("course", data.getCourse());
        values.put("accuracy", data.getAccuracy());
        values.put("status", data.getStatus());
        values.put("sats", data.getSats());
        values.put("battery", data.getBattery());
        values.put("mqua", data.getGsmStrength());
        values.put("mock", data.getMock() ? 1 : 0);

        db.insertOrThrow("data", null, values);
    }

    public void insertDataAsync(final Data data, DatabaseHandler<Void> handler) {
        new DatabaseAsyncTask<Void>(handler) {
            @Override
            protected Void executeMethod() {
                insertData(data);
                return null;
            }
        }.execute();
    }

    public Data selectData() {
        Data data = new Data();

        Cursor cursor = db.rawQuery("SELECT * FROM data ORDER BY id LIMIT 1", null);
        try {
            if (cursor.getCount() > 0) {

                cursor.moveToFirst();

                data.setId(cursor.getLong(cursor.getColumnIndex("id")));
                data.setDeviceId(cursor.getString(cursor.getColumnIndex("deviceId")));
                data.setTime(new Date(cursor.getLong(cursor.getColumnIndex("time"))));
                data.setLatitude(cursor.getDouble(cursor.getColumnIndex("latitude")));
                data.setLongitude(cursor.getDouble(cursor.getColumnIndex("longitude")));
                data.setAltitude(cursor.getDouble(cursor.getColumnIndex("altitude")));
                data.setSpeed(cursor.getDouble(cursor.getColumnIndex("speed")));
                data.setCourse(cursor.getDouble(cursor.getColumnIndex("course")));
                data.setAccuracy(cursor.getDouble(cursor.getColumnIndex("accuracy")));
                data.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
                data.setSats(cursor.getInt(cursor.getColumnIndex("sats")));
                data.setBattery(cursor.getDouble(cursor.getColumnIndex("battery")));
                data.setGsmStrength(cursor.getInt(cursor.getColumnIndex("mqua")));
                data.setMock(cursor.getInt(cursor.getColumnIndex("mock")) > 0);

            } else {
                return null;
            }
        } finally {
            cursor.close();
        }

        return data;
    }

    public void selectDataAsync(DatabaseHandler<Data> handler) {
        new DatabaseAsyncTask<Data>(handler) {
            @Override
            protected Data executeMethod() {
                return selectData();
            }
        }.execute();
    }

    public void deleteData(long id) {
        if (db.delete("data", "id = ?", new String[] { String.valueOf(id) }) != 1) {
            throw new SQLException();
        }
    }

    public void deleteDataAsync(final long id, DatabaseHandler<Void> handler) {
        new DatabaseAsyncTask<Void>(handler) {
            @Override
            protected Void executeMethod() {
                deleteData(id);
                return null;
            }
        }.execute();
    }

}
