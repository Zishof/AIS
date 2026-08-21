package ais.action.master.library.modern;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Anggota;
import ais.common.newui.NewUiCsrfUtil;

/** Thin endpoint facade used by the JSP adapter. */
public final class LibraryCatalogApi {
    private LibraryCatalogApi() { }

    public static JSONObject handle(HttpServletRequest request) throws JSONException {
        String action = request.getParameter("action");
        LibraryCatalogSearchService service = new LibraryCatalogSearchService();
        if ("references".equals(action)) return service.references();
        if ("suggestions".equals(action)) return suggestions(request);
        if ("recommendations".equals(action)) {
            Long itemId = positiveLong(request.getParameter("itemId"));
            return new JSONObject().put("ok", true).put("items", service.recommendations(itemId, 6));
        }
        if ("search".equals(action) || "latest".equals(action)) {
            JSONObject result = service.search(LibraryCatalogSearchRequest.from(request)).toJson();
            JSONObject allowed = capabilities(request);
            result.put("capabilities", allowed);
            if (!allowed.optBoolean("digital", false)) {
                JSONArray items = result.optJSONArray("items");
                if (items != null) for (int i = 0; i < items.length(); i++) items.getJSONObject(i).put("digitalUrl", "");
            }
            result.put("csrf", NewUiCsrfUtil.getToken(request.getSession()));
            return result;
        }
        return new JSONObject().put("ok", false).put("error", "Operasi katalog tidak dikenal.");
    }

    private static Long positiveLong(String raw) {
        try { long value=Long.parseLong(raw == null ? "" : raw.trim()); return value>0?Long.valueOf(value):null; }
        catch (Exception ignored) { return null; }
    }

    private static JSONObject suggestions(HttpServletRequest request) throws JSONException {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            return new JSONObject().put("ok", true).put("items",
                    new LibraryFacetService().suggestions(session, request.getParameter("query")));
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private static JSONObject capabilities(HttpServletRequest request) throws JSONException {
        Tbmuser user = Common.getCurrentUser(request);
        Anggota member = user == null ? null : Anggota.buatAtauAmbilAnggota(user, false);
        boolean memberActive = member != null && member.getId() != null && !Boolean.FALSE.equals(member.getAktif());
        return new JSONObject().put("authenticated", user != null).put("member", memberActive)
                .put("reservation", memberActive).put("favorite", memberActive)
                .put("digital", user != null);
    }
}
