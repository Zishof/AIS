package ais.common.newui.menu;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.Koperasi;
import ais.common.newui.menu.NewUiModuleFunctionService.Function;
import ais.common.newui.menu.NewUiPembagianShuService.Result;
import ais.common.newui.menu.NewUiPembagianShuService.Row;

/** Endpoint JSON khusus Pembagian SHU dengan RBAC menu, CSRF, dan transaksi Java. */
public final class NewUiPembagianShuController {
    private NewUiPembagianShuController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/json; charset=UTF-8"); response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            Tbmuser user = Common.getCurrentUser(request);
            NewUiHybridMenuSnapshot snapshot = NewUiHybridMenuAccessService.getSnapshot(request);
            List<NewUiHybridMenuNode> menus = snapshot.getSearchableNodes();
            Function function = NewUiModuleFunctionService.find("koperasi", "shu", user, menus);
            NewUiHybridMenuNode target = function == null ? null : function.getTarget();
            String action = request.getParameter("action"); if (action == null || action.trim().length() == 0) action = "list";
            action = action.trim().toLowerCase();
            String guardedAction = "calculate".equals(action) ? "update" : "list";
            if (target == null || !NewUiHybridMenuRouteGuard.isActionAuthorized(request, target, guardedAction)) {
                response.setStatus(403); json.put("ok", false).put("code", "ACTION_FORBIDDEN").put("message", "Menu Pembagian SHU atau privilege yang diperlukan tidak tersedia.");
            } else {
                Koperasi koperasi = Common.getCurrentKoperasi(); int year = integer(request.getParameter("year"), java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
                NewUiPembagianShuService service = new NewUiPembagianShuService(); Result result;
                if ("calculate".equals(action)) {
                    if (!"POST".equalsIgnoreCase(request.getMethod())) { response.setStatus(405); json.put("ok", false).put("code", "METHOD_NOT_ALLOWED").put("message", "Gunakan POST untuk menghitung SHU."); write(response,json); return; }
                    String expected = (String) request.getSession().getAttribute("newUiCsrfToken"); String sent = request.getHeader("X-CSRF-Token");
                    if (expected == null || sent == null || !expected.equals(sent)) { response.setStatus(403); json.put("ok", false).put("code", "CSRF_INVALID").put("message", "Token CSRF tidak valid."); write(response,json); return; }
                    double[] p = new double[] { decimal(request,"cadangan"), decimal(request,"jasaModal"), decimal(request,"jasaUsaha"), decimal(request,"pendidikan"), decimal(request,"pengurus"), decimal(request,"sosial"), decimal(request,"lain") };
                    result = service.calculate(year, decimal(request,"totalShu"), p, koperasi);
                } else if ("list".equals(action) || "detail".equals(action)) result = service.load(year, koperasi);
                else { response.setStatus(400); json.put("ok", false).put("code", "UNKNOWN_ACTION").put("message", "Action tidak dikenali."); write(response,json); return; }
                encode(json, result); json.put("ok", true);
            }
        } catch (IllegalArgumentException error) {
            response.setStatus(422); json.put("ok", false).put("code", "VALIDATION_FAILED").put("message", error.getMessage());
        } catch (Exception error) {
            response.setStatus(500); json.put("ok", false).put("code", "INTERNAL_ERROR").put("message", "Gagal memproses pembagian SHU.");
            try { ais.common.ErrorAuditUtil.record(error, "NewUiPembagianShuController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    private static void encode(JSONObject json, Result result) throws Exception {
        json.put("year", result.year).put("distributed", result.distributed); JSONObject head = new JSONObject();
        if (result.head != null) { head.put("id",result.head.getId()).put("totalShu",result.head.getTotalShu()).put("cadangan",result.head.getPersenCadangan()).put("jasaModal",result.head.getPersenJasaModal()).put("jasaUsaha",result.head.getPersenJasaUsaha()).put("pendidikan",result.head.getPersenPendidikan()).put("pengurus",result.head.getPersenPengurus()).put("sosial",result.head.getPersenSosial()).put("lain",result.head.getPersenLain()).put("status",result.head.getStatus()); }
        json.put("head", head); JSONArray rows = new JSONArray();
        for (int i=0;i<result.rows.size();i++) { Row row=result.rows.get(i); rows.put(new JSONObject().put("id",row.id).put("member",row.member).put("savings",row.savings).put("participation",row.participation).put("capital",row.capital).put("business",row.business).put("total",row.total).put("paid",row.paid)); }
        json.put("rows",rows).put("count",rows.length());
    }
    private static int integer(String value,int fallback){try{return value==null?fallback:Integer.parseInt(value);}catch(Exception e){return fallback;}}
    private static double decimal(HttpServletRequest request,String name){String value=request.getParameter(name);if(value==null||value.trim().length()==0)return 0;return Double.parseDouble(value.replace(",","."));}
    private static void write(HttpServletResponse response,JSONObject json)throws IOException{response.getWriter().print(json.toString());}
}
