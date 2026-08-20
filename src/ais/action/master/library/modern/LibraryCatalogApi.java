package ais.action.master.library.modern;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.json.JSONException;

/** Thin endpoint facade used by the JSP adapter. */
public final class LibraryCatalogApi {
    private LibraryCatalogApi() { }

    public static JSONObject handle(HttpServletRequest request) throws JSONException {
        String action = request.getParameter("action");
        LibraryCatalogSearchService service = new LibraryCatalogSearchService();
        if ("references".equals(action)) return service.references();
        if ("search".equals(action) || "latest".equals(action)) {
            return service.search(LibraryCatalogSearchRequest.from(request)).toJson();
        }
        return new JSONObject().put("ok", false).put("error", "Operasi katalog tidak dikenal.");
    }
}
