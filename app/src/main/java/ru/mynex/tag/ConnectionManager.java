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

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;

// import java.net.HttpURLConnection;
// import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
// import java.io.ClosedChannelException;
import java.net.SocketException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
// import android.io.Thread;


public class ConnectionManager extends Thread {

    private static final int STATE_NOT_CONNECTED = 0;
    private static final int STATE_CONNECTION = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_REGISTERING = 3;
    private static final int STATE_REGISTERED = 4;

    private final int TIMEOUT = 10 * 1000;
    private static Socket socket = null;
    // private boolean registered = false;
    // private boolean connected = false;
    private long counter = 0;
    private boolean stopFlag = false;
    private String saddr;
    private String ident;
    private static int state = STATE_NOT_CONNECTED;
    private static int register_count = 0;
    private int timeout_counter = 0;

    public ConnectionManager() {
        // socket = new Socket();
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

            if (stopFlag) {
                stopFlag = false;
                break;
            }

            sleepMs(500);

            if (timeout_counter > 0) {
                timeout_counter--;
                continue;
            }

            if (state == STATE_NOT_CONNECTED) {
                if (!connect()) timeout_counter = 20;
            } else if (state == STATE_CONNECTED) {
                sendIdent(ident);
            } else if (state == STATE_REGISTERING) {
                Log.d("ru.mynex.tag - register_count", String.valueOf(register_count));
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    try {
                        String line = reader.readLine();
                        if (line != null && line.contains("R+OK")) {
                            Log.d("ru.mynex.tag - register", "response ok");
                            state = STATE_REGISTERED;
                        }
                    } catch (SocketTimeoutException ex) {
                        Log.d("ru.mynex.tag - SocketTimeoutException", ex.getMessage());
                        state = STATE_CONNECTED;
                    }
                } catch (IOException ex) {
                    Log.d("ru.mynex.tag - IOException", ex.getMessage());
                    state = STATE_CONNECTED;
                }
                register_count++;
                if (register_count == 10) {
                    register_count = 0;
                    Log.d("ru.mynex.tag - run", "pre disconnect 1");
                    disconnect();
                }
            } else if (state == STATE_REGISTERED) {
                // здесь нужно слушать команды от сервера, например, отвечать на пинг

                // BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // try {
                //     String line = reader.readLine();
                //     if (line != null && line.contains("R+OK")) {
                //         state = STATE_REGISTERED;
                //     }
                // } catch (SocketTimeoutException ex) {
                //     Log.d("ru.mynex.tag - SocketTimeoutException", ex.getMessage());
                // }

                // Log.d("ru.mynex.tag - run STATE_REGISTERED", String.valueOf(register_count));
                sleepMs(500);
            }

        }

        Log.d("ru.mynex.tag - run", "pre disconnect 2");
        disconnect();
    }

    public void startx(String saddr, String ident) {
        stopFlag = false;
        register_count = 0;
        this.saddr = saddr;
        this.ident = ident;
        this.start();
    }

    public void stopx() {
        stopFlag = true;
    }

    public boolean isConnected() {
        return state >= STATE_CONNECTED;
    }

    public boolean isRegistered() {
        return state >= STATE_REGISTERED;
    }

    public boolean connect() {

        Log.d("ru.mynex.tag - connect", "try to connect");

        if (isConnected()) return true;

        try {
            URI uri = new URI("my://" + saddr);
            String hostname = uri.getHost();
            int port = uri.getPort();
        
            try {
                
                // Log.d("ru.mynex.tag - hostname", hostname);
                // Log.d("ru.mynex.tag - port", String.valueOf(port));
                socket = new Socket(hostname, port);
                try {
                    socket.setSoTimeout(TIMEOUT);
                } catch (SocketException ex) {
                    Log.d("ru.mynex.tag - SocketException", ex.getMessage());
                }
                // Reuse the address when trying to reconnect.
                // socket.setReuseAddress(true);
                // registered = false;
                state = STATE_CONNECTED;
                Log.d("ru.mynex.tag - connect", "connect ok");
                return true;


            } catch (SocketException ex) {
    
                Log.d("ru.mynex.tag - SocketException", ex.getMessage());
                return false;
    
            } catch (UnknownHostException ex) {

                Log.d("ru.mynex.tag - Server not found", ex.getMessage());
                return false;

            } catch (IOException ex) {

                Log.d("ru.mynex.tag - I/O error", ex.getMessage());
                return false;

            }
        } catch (URISyntaxException ex) {

            Log.d("ru.mynex.tag - URISyntaxException", ex.getMessage());
            return false;

        }

    }

    public boolean disconnect() {

        Log.d("ru.mynex.tag - disconnect", "try to disconnect");
        // connected = false;
        // return true;

        // if (!connected) return true;
        // connected = false;
        // registered = false;
        state = STATE_NOT_CONNECTED;

        if (socket == null) {
            Log.d("ru.mynex.tag - disconnect", "socket is null");
            return true;
        }

        if (!socket.isConnected() || socket.isClosed()) {
            Log.d("ru.mynex.tag - disconnect", "already closed");
            socket = null;
            return true;
        }

        try {

            socket.close();
            Log.d("ru.mynex.tag - disconnect", "disconnect ok");
            socket = null;
            return true;

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
            return false;

        }

    }

    public boolean sendIdent(String ident) {

        try {

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            Log.d("ru.mynex.tag - sendIdent", "try to register");
            writer.println(ident);
            writer.flush();
            Log.d("ru.mynex.tag - sendIdent", "send ok");
            state = STATE_REGISTERING;
            return true;

        } catch (SocketException ex) {

            System.out.println("SocketException: " + ex.getMessage());
            state = STATE_NOT_CONNECTED;
            return false;

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());
            state = STATE_NOT_CONNECTED;
            return false;

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
            state = STATE_NOT_CONNECTED;
            return false;
        }

    }


    public static boolean sendPack(String pack) {

        if (state != STATE_REGISTERED) return false;

        try {

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            Log.d("ru.mynex.tag - sendIdent", "try to send pack");
            writer.println(pack);
            writer.flush();
            Log.d("ru.mynex.tag - sendPack", "send ok");
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            try {
                String line = reader.readLine();
                if (line != null && line.contains("R+OK")) {
                    Log.d("ru.mynex.tag - sendPack", "response ok");
                    return true;
                } else {
                    state = STATE_CONNECTED;
                }
            } catch (SocketTimeoutException ex) {
                Log.d("ru.mynex.tag - SocketTimeoutException", ex.getMessage());
                state = STATE_CONNECTED;
            }

        } catch (SocketException ex) {

            System.out.println("SocketException: " + ex.getMessage());
            state = STATE_NOT_CONNECTED;

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());
            state = STATE_NOT_CONNECTED;

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
            state = STATE_NOT_CONNECTED;
        }

        return false;
    }



    public interface RequestHandler {
        void onComplete(boolean success);
    }

    private static class RequestAsyncTask extends AsyncTask<String, Void, Boolean> {

        private RequestHandler handler;

        public RequestAsyncTask(RequestHandler handler) {
            this.handler = handler;
        }

        @Override
        protected Boolean doInBackground(String... request) {
            return sendPack(request[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            handler.onComplete(result);
        }
    }

    public static void sendRequestAsync(String request, RequestHandler handler) {
        RequestAsyncTask task = new RequestAsyncTask(handler);
        task.execute(request);
    }

}