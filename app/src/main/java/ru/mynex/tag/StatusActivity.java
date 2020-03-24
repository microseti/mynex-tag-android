/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;

import android.util.Log;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.content.Intent;

public class StatusActivity extends AppCompatActivity {

    private static final int LIMIT = 20;

    // private static final LinkedList<String> messages = new LinkedList<>();
    private static ArrayList<String> messages = new ArrayList<>();
    // private static final Set<ArrayAdapter<String>> adapters = new HashSet<>();
    private static ArrayAdapter<String> adapter = null;
    private static ListView listView;

    private static void notifyAdapters() {
        // Log.d("ru.mynex.tag - notifyAdapters", "s1");
        // for (ArrayAdapter<String> adapter : adapters) {

            // Log.d("ru.mynex.tag - notifyAdapters", "s2");

            if (adapter != null)
                adapter.notifyDataSetChanged();

            // Log.d("ru.mynex.tag - notifyAdapters", "s3");

        // }
        // Log.d("ru.mynex.tag - notifyAdapters", "s3");
    }

    public static void addMessage(String message) {
        // Log.d("ru.mynex.tag - addMessage", message);
        // Log.d("ru.mynex.tag - addMessage", "s1");
        DateFormat format = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        // Log.d("ru.mynex.tag - addMessage", "s2");
        message = format.format(new Date()) + " - " + message;
        // Log.d("ru.mynex.tag - addMessage", "s3");
        // messages.add("Hello world");
        messages.add(message);
        // Log.d("ru.mynex.tag - addMessage", "s4");
        while (messages.size() > LIMIT) {
            messages.remove(0);
            // messages.removeFirst();
        }
        // Log.d("ru.mynex.tag - addMessage", "s5");
        notifyAdapters();
        // Log.d("ru.mynex.tag - addMessage", "s6");
    }

    public static void clearMessages() {
        messages.clear();
        // Log.d("ru.mynex.tag - clearMessages", "s1");
        notifyAdapters();
        // Log.d("ru.mynex.tag - clearMessages", "s2");
    }

    // private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        // getActionBar().setDisplayShowHomeEnabled( true );

        // Disable back icon in top left and hide app name.
        // getActionBar().setDisplayHomeAsUpEnabled( false );
        // getActionBar().setDisplayShowTitleEnabled( false );

        // Toolbar toolbar = findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        // if (getSupportActionBar() != null) {
        //     getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //     getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
        //     getSupportActionBar().setTitle("");
        // }

        // // adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        // adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, messages);
        // ListView listView = findViewById(android.R.id.list);
        // listView.setAdapter(adapter);
        // adapters.add(adapter);


        // Log.d("ru.mynex.tag - onCreate", "s1");
        // adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, messages);
        listView = findViewById(android.R.id.list);
        listView.setAdapter(adapter);
        // Log.d("ru.mynex.tag - onCreate", "s2");
        // adapters.add(adapter);


        // Log.d("ru.mynex.tag - onCreate", "s3");
        // addMessage("Hello");
        // addMessage("Wjorld");

        // notifyAdapters();

        // addMessage("New World");

        // notifyAdapters();

        // addMessage("Here I");

        // Log.d("ru.mynex.tag - onCreate", "s4");
    }

    @Override
    protected void onDestroy() {
        // adapters.remove(adapter);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.status, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.clear:
                clearMessages();
                return true;
            case R.id.back:
                startActivity(new Intent(this, MainActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        
    }

}
