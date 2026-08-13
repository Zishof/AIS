package ais.common.newui.inventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.inventory.NewUiPembelianService.LineInput;
import ais.common.newui.inventory.NewUiPembelianService.Option;
import ais.common.newui.inventory.NewUiPembelianService.PurchaseRow;
import ais.common.newui.inventory.NewUiPembelianService.Snapshot;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/** JSON endpoint Pembelian New UI dengan RBAC dan CSRF. */
public final class NewUiPembelianController {
    private static final String MODULE="inventory", PAGE="pembelian";
    private NewUiPembelianController() { }
    public static void handle(HttpServletRequest req,HttpServletResponse res)throws Exception {
        res.setContentType("application/json; charset=UTF-8");res.setHeader("Cache-Control","no-store");JSONObject j=new JSONObject();
        try {
            String a=text(req.getParameter("action"),"list").toLowerCase();String privilege="delete".equals(a)?"delete":"save".equals(a)?"save":"list";
            if(!NewUiRouteGuard.isActionAuthorized(req,MODULE,PAGE,privilege)){res.setStatus(403);fail(j,"ACTION_FORBIDDEN","Hak akses tidak tersedia.");write(res,j);return;}
            if(("save".equals(a)||"delete".equals(a))&&(!"POST".equalsIgnoreCase(req.getMethod())||!csrf(req))){res.setStatus(403);fail(j,"CSRF_INVALID","Metode atau token CSRF tidak valid.");write(res,j);return;}
            Tbmuser user=Common.getCurrentUser(req);NewUiPembelianService svc=new NewUiPembelianService();Toko shop=svc.currentShop(user);
            if("save".equals(a)){JSONArray items=new JSONArray(text(req.getParameter("items"),"[]"));List<LineInput> lines=new ArrayList<LineInput>();for(int i=0;i<items.length();i++){JSONObject x=items.getJSONObject(i);lines.add(new LineInput(Long.valueOf(x.getLong("productId")),x.getDouble("quantity")));}svc.save(id(req,"id"),req.getParameter("invoice"),id(req,"shopId"),id(req,"studentId"),id(req,"collegeStudentId"),req.getParameter("note"),lines,shop,user);}
            else if("delete".equals(a))svc.delete(requiredId(req,"id"),shop,user);
            else if("products".equals(a)){j.put("ok",true).put("options",options(svc.findProducts(id(req,"shopId"),req.getParameter("q"),shop)));write(res,j);return;}
            else if("members".equals(a)){j.put("ok",true).put("options",options(svc.findMembers(text(req.getParameter("type"),"student"),req.getParameter("q"),user)));write(res,j);return;}
            else if(!"list".equals(a))throw new IllegalArgumentException("Aksi tidak dikenal.");
            Snapshot d=svc.load(req.getParameter("q"),id(req,"shopId"),id(req,"studentId"),date(req,"start"),date(req,"end"),integer(req,"page",0),Math.min(100,Math.max(10,integer(req,"size",20))),shop,user);encode(j,d);j.put("ok",true);
        }catch(IllegalArgumentException e){res.setStatus(422);fail(j,"VALIDATION_FAILED",e.getMessage());}
        catch(Exception e){res.setStatus(500);fail(j,"INTERNAL_ERROR","Gagal memproses pembelian.");try{ais.common.ErrorAuditUtil.record(e,"NewUiPembelianController");}catch(Exception ignored){}}
        write(res,j);
    }
    private static void encode(JSONObject j,Snapshot d)throws Exception{JSONArray a=new JSONArray();for(int i=0;i<d.rows.size();i++){PurchaseRow r=d.rows.get(i);a.put(new JSONObject().put("id",r.id).put("invoice",r.invoice).put("productId",r.productId).put("productCode",r.productCode).put("product",r.product).put("quantity",r.quantity).put("unitPrice",r.unitPrice).put("amount",r.amount).put("time",r.time==null?JSONObject.NULL:Long.valueOf(r.time.getTime())).put("member",r.member).put("shopId",r.shopId).put("shop",r.shop).put("studentId",r.studentId).put("collegeStudentId",r.collegeStudentId).put("note",r.note));}j.put("rows",a).put("shops",options(d.shops)).put("total",d.total).put("unitTotal",d.unitTotal).put("grandTotal",d.grandTotal);}
    private static JSONArray options(List<Option> v)throws Exception{JSONArray a=new JSONArray();for(int i=0;i<v.size();i++){Option o=v.get(i);a.put(new JSONObject().put("id",o.id).put("label",o.label).put("code",o.code).put("barcode",o.barcode).put("price",o.price));}return a;}
    private static boolean csrf(HttpServletRequest r){Object e=r.getSession().getAttribute("newUiCsrfToken");String v=r.getHeader("X-CSRF-Token");return e!=null&&v!=null&&String.valueOf(e).equals(v);}
    private static Long id(HttpServletRequest r,String n){String v=r.getParameter(n);if(v==null||v.trim().length()==0)return null;try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static Long requiredId(HttpServletRequest r,String n){Long v=id(r,n);if(v==null)throw new IllegalArgumentException(n+" wajib diisi.");return v;}
    private static int integer(HttpServletRequest r,String n,int f){try{return Integer.parseInt(text(r.getParameter(n),String.valueOf(f)));}catch(Exception e){return f;}}
    private static Date date(HttpServletRequest r,String n){String v=r.getParameter(n);if(v==null||v.trim().length()==0)return null;try{return new SimpleDateFormat("yyyy-MM-dd").parse(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static String text(String v,String f){return v==null||v.trim().length()==0?f:v.trim();}
    private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}
    private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}
}
