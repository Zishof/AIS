package ais.action.master.generic.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GenericCrudJson {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").disableHtmlEscaping().create();
    private GenericCrudJson() { }
    public static String toJson(Object value) { return GSON.toJson(value); }
    public static Object fromJson(String value, Class type) { return GSON.fromJson(value, type); }
}
