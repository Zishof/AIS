package ais.action.master.library.modern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import ais.common.newui.PortalLoginApi;

/** Authentication adapter for the public library portal. */
public final class LibraryLoginApi {
    private LibraryLoginApi() { }

    public static JSONObject handle(HttpServletRequest request, HttpServletResponse response) throws JSONException {
        return PortalLoginApi.handle(request, response, "library");
    }
}
