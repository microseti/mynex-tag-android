/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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

import android.util.Log;
import android.net.Uri;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.Clock;
import java.text.DecimalFormat;

public class ProtocolFormatter {

    public final static int CRC_POLYNOM = 0x8c;
    public final static int CRC_PRESET = 0;

    public static byte crc8(byte[] data) {
        int crc_U = CRC_PRESET;
        for(int i = 0; i < data.length; i++) {
            crc_U ^= Byte.toUnsignedInt(data[i]);
            for(int j = 0; j < 8; j++) {
                if((crc_U & 0x01) != 0) {
                    crc_U = (crc_U >>> 1) ^ CRC_POLYNOM;
                } else {
                    crc_U = (crc_U >>> 1);
                }
            }
        }
        return (byte) crc_U;
    }

    public static String formatIdent(String deviceId, long interval) {
        // version, time sync, ping, ident
        return "I+100,0," + (interval + 30) + "," + deviceId;
    }

    public static String formatPackage(Data data, String alarm) {
        long unixTime = data.getTime().getTime() / 1000;
        int unixTimeMs = (int) ((data.getTime().getTime() / 1000.0 - unixTime) * 1000);
        
        String deviceTime = Instant.ofEpochSecond(unixTime)
        .atZone(ZoneId.of("GMT-0"))
        .format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));

        String res = String.format("P+%s,%03d,%d,{", deviceTime, unixTimeMs, data.getId());
        res += "\"glat\":" + String.format(java.util.Locale.US, "%.7f", data.getLatitude());
        res += ",\"glng\":" + String.format(java.util.Locale.US, "%.7f", data.getLongitude());
        res += ",\"gspe\":" + String.format(java.util.Locale.US, "%.1f", data.getSpeed());
        res += ",\"gcou\":" + String.format(java.util.Locale.US, "%.1f", data.getCourse());
        res += ",\"galt\":" + String.format(java.util.Locale.US, "%.1f", data.getAltitude());
        res += ",\"gacu\":" + String.format(java.util.Locale.US, "%.1f", data.getAccuracy());
        res += ",\"gsta\":" + String.valueOf(data.getStatus());
        res += ",\"gsat\":" + String.valueOf(data.getSats());
        res += ",\"mqua\":" + String.valueOf(data.getGsmStrength());
        res += ",\"bat\":" + String.format(java.util.Locale.US, "%.1f", data.getBattery());

        if (data.getMock()) {
            res += ",\"mock\":\"" + String.valueOf(data.getMock()) + "\"";
        }

        if (alarm != null) {
            res += ",\"alarm\":\"" + alarm + "\"";
        }

        res += "}";
        res += String.format("*%02X\n", crc8(res.getBytes()));
        return res;
    }
}
