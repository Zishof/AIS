package ais.common.newui.inventory;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.model.Tbmuser;
import ais.common.newui.inventory.NewUiPengajuanPembelianGudangService.Option;
import ais.common.newui.inventory.NewUiPengajuanPembelianGudangService.RequestRow;
import ais.common.newui.inventory.NewUiPengajuanPembelianGudangService.Snapshot;
import ais.common.newui.inventory.NewUiPengajuanPembelianGudangService.ThresholdRow;

/** Endpoint JSON native inventory/pengajuan_pembelian_gudang. */
public final class NewUiPengajuanPembelianGudangController {
    private static final String MODULE="inventory", PAGE="pengajuan_pembelian_gudang";
    private NewUiPengajuanPembelianGudangController() { }
    public static void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/json; charset=UTF-8"); response.setHeader("Cache-Control","no-store"); JSONObject json=new JSONObject();
        try {
            Tbmuser user=Common.getCurrentUser(request); String action=text(request.getParameter("action"),"list").toLowerCase();
            boolean mutating=!"list".equals(action);
            if(!NewUiRouteGuard.isActionAuthorized(request,MODULE,PAGE,mutating?"update":"list")) { response.setStatus(403); fail(json,"ACTION_FORBIDDEN","Hak akses menu tidak mencukupi."); }
            else if(mutating && !"POST".equalsIgnoreCase(request.getMethod())) { response.setStatus(405); fail(json,"METHOD_NOT_ALLOWED","Gunakan POST untuk perubahan data."); }
            else if(mutating && !csrf(request)) { response.setStatus(403); fail(json,"CSRF_INVALID","Token CSRF tidak valid."); }
            else {
                NewUiPengajuanPembelianGudangService service=new NewUiPengajuanPembelianGudangService();
                if("save_threshold".equals(action)) service.saveThreshold(id(request,"id",false),id(request,"productId",true),id(request,"warehouseId",true),decimal(request,"minimum"),bool(request,"active"),request.getParameter("note"),user);
                else if("delete_threshold".equals(action)) service.deleteThreshold(id(request,"id",true));
                else if("update_status".equals(action)) service.updateStatus(id(request,"id",true),request.getParameter("status"),user);
                else if("run_now".equals(action)) json.put("created",service.runNow());
                else if(!"list".equals(action)) { response.setStatus(400); fail(json,"UNKNOWN_ACTION","Aksi tidak dikenali."); write(response,json); return; }
                encode(json,service.load()); json.put("ok",true);
            }
        } catch(IllegalArgumentException error) { response.setStatus(422); fail(json,"VALIDATION_FAILED",error.getMessage()); }
        catch(Exception error) { response.setStatus(500); fail(json,"INTERNAL_ERROR","Gagal memproses pengajuan pembelian gudang."); try{ais.common.ErrorAuditUtil.record(error,"NewUiPengajuanPembelianGudangController");}catch(Exception ignored){} }
        write(response,json);
    }
    private static void encode(JSONObject json,Snapshot data)throws Exception{
        JSONArray thresholds=new JSONArray();for(int i=0;i<data.thresholds.size();i++){ThresholdRow r=data.thresholds.get(i);thresholds.put(new JSONObject().put("id",r.id).put("productId",r.productId).put("warehouseId",r.warehouseId).put("product",r.product).put("warehouse",r.warehouse).put("minimum",r.minimum).put("active",r.active).put("note",r.note));}
        JSONArray requests=new JSONArray();for(int i=0;i<data.requests.size();i++){RequestRow r=data.requests.get(i);requests.put(new JSONObject().put("id",r.id).put("product",r.product).put("source",r.source).put("destination",r.destination).put("quantity",r.quantity).put("stock",r.stock).put("status",r.status).put("automatic",r.automatic).put("created",r.created==null?JSONObject.NULL:r.created.getTime()).put("note",r.note));}
        JSONArray products=new JSONArray();for(int i=0;i<data.products.size();i++){Option o=data.products.get(i);products.put(new JSONObject().put("id",o.id).put("label",o.label));}
        JSONArray warehouses=new JSONArray();for(int i=0;i<data.warehouses.size();i++){Option o=data.warehouses.get(i);warehouses.put(new JSONObject().put("id",o.id).put("label",o.label));}
        json.put("thresholds",thresholds).put("requests",requests).put("products",products).put("warehouses",warehouses).put("requestCount",requests.length());
    }
    private static boolean csrf(HttpServletRequest r){Object expected=r.getSession().getAttribute("newUiCsrfToken");String sent=r.getHeader("X-CSRF-Token");return expected!=null&&sent!=null&&String.valueOf(expected).equals(sent);}
    private static Long id(HttpServletRequest r,String n,boolean required){String v=r.getParameter(n);if(v==null||v.trim().length()==0){if(required)throw new IllegalArgumentException(n+" wajib diisi.");return null;}try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static double decimal(HttpServletRequest r,String n){try{return Double.parseDouble(text(r.getParameter(n),"0").replace(",","."));}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static boolean bool(HttpServletRequest r,String n){String v=r.getParameter(n);return "true".equalsIgnoreCase(v)||"1".equals(v)||"on".equalsIgnoreCase(v);}
    private static String text(String v,String d){return v==null||v.trim().length()==0?d:v.trim();}
    private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}
    private static void write(HttpServletResponse r,JSONObject j)throws IOException{r.getWriter().print(j.toString());}
}
