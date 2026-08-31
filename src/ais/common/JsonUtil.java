package ais.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tipe khusus untuk json util. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit pada
 * perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code toMap()}, {@code toList}(). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 */
public class JsonUtil {

    /**
     * Mengonversi JSONObject menjadi Map<String, Object>
     */
    @SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(JSONObject jsonobj) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();
        Iterator<String> keys = jsonobj.keys();
        
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonobj.get(key);
            
            // Rekursif jika value berupa Array atau Object di dalamnya
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            
            // Mengatasi JSON NULL bawaan org.json
            if (value == JSONObject.NULL) {
                value = null;
            }
            
            map.put(key, value);
        }
        return map;
    }

    /**
     * Mengonversi JSONArray menjadi List<Object>
     */
    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            
            // Rekursif
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            
            if (value == JSONObject.NULL) {
                value = null;
            }
            
            list.add(value);
        }
        return list;
    }
}