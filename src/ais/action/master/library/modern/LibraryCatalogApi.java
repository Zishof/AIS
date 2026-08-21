package ais.action.master.library.modern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
        if ("suggestions".equals(action)) {
            if (!allow(request.getSession(), "catalog-suggest", 40, 60000L))
                return new JSONObject().put("ok", false).put("error", "Terlalu banyak permintaan saran. Coba lagi sebentar.");
            return suggestions(request);
        }
        if ("recommendations".equals(action)) {
            Long itemId = positiveLong(request.getParameter("itemId"));
            return new JSONObject().put("ok", true).put("items", service.recommendations(itemId, 6));
        }
        if ("search".equals(action) || "latest".equals(action)) {
            if (!allow(request.getSession(), "catalog-search", 90, 60000L))
                return new JSONObject().put("ok", false).put("error", "Terlalu banyak pencarian. Coba lagi sebentar.");
            LibraryCatalogSearchRequest searchRequest=LibraryCatalogSearchRequest.from(request);
            LibraryScopeResolver.apply(searchRequest);
            JSONObject result = service.search(searchRequest).toJson();
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

    private static boolean allow(HttpSession session, String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();
        String startKey = key + "-start";
        String countKey = key + "-count";
        synchronized (session) {
            Long start = (Long) session.getAttribute(startKey);
            Integer count = (Integer) session.getAttribute(countKey);
            if (start == null || now - start.longValue() >= windowMillis) {
                session.setAttribute(startKey, Long.valueOf(now));
                session.setAttribute(countKey, Integer.valueOf(1));
                return true;
            }
            int next = count == null ? 1 : count.intValue() + 1;
            session.setAttribute(countKey, Integer.valueOf(next));
            return next <= limit;
        }
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
