package ais.common.newui.sekolah;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.sekolah.NewUiTagihanService.Choice;
import ais.common.newui.sekolah.NewUiTagihanService.Filter;
import ais.common.newui.sekolah.NewUiTagihanService.History;
import ais.common.newui.sekolah.NewUiTagihanService.MoveTarget;
import ais.common.newui.sekolah.NewUiTagihanService.Row;
import ais.common.newui.sekolah.NewUiTagihanService.Snapshot;
import ais.database.model.Tbmuser;

/** JSON controller native untuk halaman Tagihan New UI. */
public final class NewUiTagihanController {
    private static final String MODULE="sekolah", PAGE="tagihan";
    private NewUiTagihanController() { }
    public static void handle(HttpServletRequest request,HttpServletResponse response)throws Exception{
        response.setContentType("application/json; charset=UTF-8"); response.setHeader("Cache-Control","no-store"); JSONObject json=new JSONObject();
        try{
            String action=text(request.getParameter("action"),"list"); String privilege=privilege(action);
            if(!NewUiRouteGuard.isActionAuthorized(request,MODULE,PAGE,privilege)){response.setStatus(403);fail(json,"ACTION_FORBIDDEN","Hak akses tidak tersedia.");write(response,json);return;}
            boolean mutation=!"list".equals(action)&&!"move_targets".equals(action)&&!"history".equals(action);
            if(mutation&&(!"POST".equalsIgnoreCase(request.getMethod())||!csrf(request))){response.setStatus(403);fail(json,"CSRF_INVALID","Token CSRF tidak valid.");write(response,json);return;}
            NewUiTagihanService service=new NewUiTagihanService(); Tbmuser user=Common.getCurrentUser(request);
            if("list".equals(action)){Snapshot data=service.load(filter(request),user);encode(json,data);json.put("syncEnabled",syncEnabled()).put("admin",safeAdmin());}
            else if("generate".equals(action))json.put("generated",service.autoGenerate(id(request,"studentId",true),user));
            else if("sync".equals(action)){if(!syncEnabled())throw new SecurityException("Fitur sinkronisasi dinonaktifkan oleh konfigurasi.");json.put("synchronized",service.synchronize(filter(request),user));}
            else if("toggle".equals(action))service.toggle(id(request,"id",true),bool(request,"active"),user);
            else if("move_targets".equals(action))encodeTargets(json,service.moveTargets(id(request,"id",true),user));
            else if("move".equals(action))service.movePayment(id(request,"id",true),id(request,"targetId",true),user);
            else if("delete".equals(action))service.delete(id(request,"id",true),user);
            else if("history".equals(action))encodeHistory(json,service.history(id(request,"id",true),user));
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok",true);
        }catch(SecurityException e){response.setStatus(403);fail(json,"FORBIDDEN",e.getMessage());}
        catch(IllegalArgumentException e){response.setStatus(422);fail(json,"VALIDATION_FAILED",e.getMessage());}
        catch(Exception e){response.setStatus(500);fail(json,"INTERNAL_ERROR","Gagal memproses tagihan. Detail telah dicatat di log server.");try{ais.common.ErrorAuditUtil.record(e,"NewUiTagihanController");}catch(Exception ignored){}}
        write(response,json);
    }
    private static void encode(JSONObject j,Snapshot d)throws Exception{JSONArray rows=new JSONArray();for(Row x:d.rows)rows.put(new JSONObject().put("id",x.id).put("studentId",x.studentId).put("candidateId",x.candidateId).put("code",x.code).put("name",x.name).put("school",x.school).put("item",x.item).put("information",x.information).put("academicYear",x.academicYear).put("photo",x.photo).put("month",x.month).put("year",x.year).put("installment",x.installment).put("nominal",x.nominal).put("fine",x.fine).put("paid",x.paid).put("settled",x.settled).put("active",x.active).put("notBill",x.notBill).put("canToggle",x.canToggle).put("canMove",x.canMove));j.put("rows",rows).put("total",d.total);JSONArray years=new JSONArray();for(String y:d.options.academicYears)years.put(y);JSONArray students=new JSONArray(),candidates=new JSONArray();for(Choice c:d.options.students)students.put(choice(c));for(Choice c:d.options.candidates)candidates.put(choice(c));j.put("academicYears",years).put("students",students).put("candidates",candidates);}
    private static JSONObject choice(Choice c)throws Exception{return new JSONObject().put("id",c.id).put("code",c.code).put("name",c.name);}
    private static void encodeTargets(JSONObject j,List<MoveTarget> values)throws Exception{JSONArray a=new JSONArray();for(MoveTarget x:values)a.put(new JSONObject().put("id",x.id).put("label",x.label));j.put("targets",a);}
    private static void encodeHistory(JSONObject j,List<History> values)throws Exception{JSONArray a=new JSONArray();for(History x:values)a.put(new JSONObject().put("revision",x.revision).put("type",x.type).put("timestamp",x.timestamp).put("by",x.by).put("information",x.information).put("nominal",x.nominal).put("fine",x.fine).put("active",x.active).put("paymentId",x.paymentId));j.put("history",a);}
    private static Filter filter(HttpServletRequest r){Filter f=new Filter();f.q=r.getParameter("q");f.fee=r.getParameter("fee");f.academicYear=r.getParameter("academicYear");f.studentId=id(r,"studentId",false);f.candidateId=id(r,"candidateId",false);f.settingId=id(r,"settingId",false);f.schoolId=id(r,"schoolId",false);f.foundationId=id(r,"foundationId",false);f.month=integerObject(r,"month");f.year=integerObject(r,"year");f.unpaidOnly=bool(r,"unpaidOnly");f.activeOnly=!"false".equalsIgnoreCase(r.getParameter("activeOnly"));f.page=integer(r,"page",0);f.size=Math.min(100,Math.max(10,integer(r,"size",20)));return f;}
    private static String privilege(String a){return "delete".equals(a)||"move".equals(a)?"delete":("generate".equals(a)||"sync".equals(a)||"toggle".equals(a)?"update":"list");}
    private static boolean safeAdmin(){try{return Common.getApakahAdmin();}catch(Exception e){return false;}}
    private static boolean syncEnabled(){try{return Common.bolehKonfigurasi("aktifkan_tombol_singkronkan_tagihan_siswa");}catch(Exception e){return false;}}
    private static boolean csrf(HttpServletRequest r){Object e=r.getSession().getAttribute("newUiCsrfToken");String v=r.getHeader("X-CSRF-Token");return e!=null&&v!=null&&String.valueOf(e).equals(v);}
    private static boolean bool(HttpServletRequest r,String n){return"true".equalsIgnoreCase(r.getParameter(n));}
    private static Long id(HttpServletRequest r,String n,boolean required){String v=r.getParameter(n);if(v==null||v.trim().length()==0){if(required)throw new IllegalArgumentException(n+" wajib diisi.");return null;}try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static Integer integerObject(HttpServletRequest r,String n){String v=r.getParameter(n);if(v==null||v.trim().length()==0)return null;try{return Integer.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static int integer(HttpServletRequest r,String n,int f){try{return Integer.parseInt(text(r.getParameter(n),String.valueOf(f)));}catch(Exception e){return f;}}
    private static String text(String v,String f){return v==null||v.trim().length()==0?f:v.trim();}
    private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m==null?"Operasi ditolak.":m);}
    private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}
}
