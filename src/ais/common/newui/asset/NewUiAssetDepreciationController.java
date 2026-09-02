package ais.common.newui.asset;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.asset.NewUiAssetDepreciationService.DepreciationRow;
import ais.common.newui.asset.NewUiAssetDepreciationService.Filter;
import ais.common.newui.asset.NewUiAssetDepreciationService.Option;
import ais.common.newui.asset.NewUiAssetDepreciationService.Options;
import ais.common.newui.asset.NewUiAssetDepreciationService.Progress;
import ais.common.newui.asset.NewUiAssetDepreciationService.Row;
import ais.common.newui.asset.NewUiAssetDepreciationService.Snapshot;

/** JSON endpoint for the native Asset Detail and depreciation page. */
public final class NewUiAssetDepreciationController {
    private static final Progress JOB=new Progress();
    private static final ThreadLocal<SimpleDateFormat> ISO=new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };
    private NewUiAssetDepreciationController(){}
    public static void handle(HttpServletRequest q,HttpServletResponse r)throws Exception{
        r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");JSONObject j=new JSONObject();
        try{
            String a=text(q.getParameter("action"),"list"),priv="sync".equals(a)?"update":"list";
            if(!NewUiRouteGuard.isActionAuthorized(q,"asset","asset_detail",priv)){r.setStatus(403);fail(j,"ACTION_FORBIDDEN","Hak akses tidak tersedia.");write(r,j);return;}
            NewUiAssetDepreciationService s=new NewUiAssetDepreciationService();
            if("list".equals(a))encode(j,s.load(filter(q,s.options())));
            else if("details".equals(a))details(j,s.details(requiredId(q.getParameter("id"))));
            else if("options".equals(a))options(j,s.options());
            else if("job".equals(a))job(j);
            else if("sync".equals(a)){csrf(q);start(s,requiredDate(q.getParameter("targetDate")));job(j);}
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            j.put("csrfHeader",ais.common.newui.NewUiCsrfUtil.LEGACY_HEADER).put("csrfToken",ais.common.newui.NewUiCsrfUtil.getTokenOkFlat(q));j.put("ok",true);
        }catch(SecurityException e){r.setStatus(403);fail(j,"FORBIDDEN",e.getMessage());}
        catch(IllegalArgumentException e){r.setStatus(422);fail(j,"VALIDATION_FAILED",e.getMessage());}
        catch(Exception e){r.setStatus(500);fail(j,"INTERNAL_ERROR","Gagal memproses penyusutan aset.");try{ais.common.ErrorAuditUtil.record(e,"NewUiAssetDepreciationController");}catch(Exception ignored){}}
        write(r,j);
    }
    private static synchronized void start(final NewUiAssetDepreciationService s,final Date d){if(JOB.running)throw new IllegalArgumentException("Sinkronisasi penyusutan masih berjalan.");JOB.total=0;JOB.done=0;JOB.failed=0;JOB.running=true;Thread t=new Thread(new Runnable(){public void run(){try{s.sync(d,JOB);}finally{JOB.running=false;}}},"new-ui-asset-depreciation");t.setDaemon(true);t.start();}
    private static Filter filter(HttpServletRequest q,Options o){Filter f=new Filter();f.query=q.getParameter("q");f.brand=q.getParameter("brand");f.assetTypeId=id(q.getParameter("assetTypeId"));f.roomId=id(q.getParameter("roomId"));f.start=date(q.getParameter("start"));f.end=date(q.getParameter("end"));f.asOf=date(q.getParameter("asOf"));if(f.asOf==null)f.asOf=new Date();Long unit=id(q.getParameter("workUnitId"));if(unit!=null)f.workUnitIds=descendants(unit,o.workUnits);f.page=Math.max(0,integer(q.getParameter("page"),0));f.size=Math.min(100,Math.max(10,integer(q.getParameter("size"),20)));return f;}
    private static Set<Long> descendants(Long root,List<Option> all){Set<Long> out=new HashSet<Long>();out.add(root);boolean changed=true;while(changed){changed=false;for(Option x:all)if(x.parentId!=null&&out.contains(x.parentId)&&out.add(x.id))changed=true;}return out;}
    private static void encode(JSONObject j,Snapshot s)throws Exception{JSONArray a=new JSONArray();for(Row x:s.rows)a.put(new JSONObject().put("id",x.id).put("name",x.name).put("barcode",x.barcode).put("note",x.note).put("masterAsset",x.masterAsset).put("brand",x.brand).put("assetType",x.assetType).put("room",x.room).put("workUnit",x.workUnit).put("purchaseDate",format(x.purchaseDate)).put("lastDate",format(x.lastDate)).put("month",x.month).put("purchaseValue",x.purchaseValue).put("minimumValue",x.minimumValue).put("economicLife",x.economicLife).put("monthlyDepreciation",x.monthlyDepreciation).put("accumulated",x.accumulated).put("bookValue",x.bookValue));j.put("rows",a).put("total",s.total);}
    private static void details(JSONObject j,List<DepreciationRow> rows)throws Exception{JSONArray a=new JSONArray();for(DepreciationRow x:rows)a.put(new JSONObject().put("id",x.id).put("month",x.month).put("date",format(x.date)).put("purchaseValue",x.purchaseValue).put("monthly",x.monthly).put("accumulated",x.accumulated).put("bookValue",x.bookValue).put("note",x.note));j.put("rows",a);}
    private static void options(JSONObject j,Options o)throws Exception{j.put("assetTypes",options(o.assetTypes)).put("rooms",options(o.rooms)).put("workUnits",options(o.workUnits));}private static JSONArray options(List<Option>x)throws Exception{JSONArray a=new JSONArray();for(Option v:x)a.put(new JSONObject().put("id",v.id).put("label",v.label).put("parentId",v.parentId));return a;}
    private static void job(JSONObject j)throws Exception{j.put("job",new JSONObject().put("total",JOB.total).put("done",JOB.done).put("failed",JOB.failed).put("running",JOB.running));}
    private static synchronized String format(Date d){return d==null?null:ISO.get().format(d);}private static synchronized Date date(String v){if(clean(v)==null)return null;try{return ISO.get().parse(v);}catch(Exception e){throw new IllegalArgumentException("Format tanggal harus yyyy-MM-dd.");}}private static Date requiredDate(String v){Date d=date(v);if(d==null)throw new IllegalArgumentException("Tanggal penyusutan wajib diisi.");return d;}
    private static void csrf(HttpServletRequest q){if(!"POST".equalsIgnoreCase(q.getMethod()))throw new SecurityException("Metode HTTP tidak valid.");Object e=q.getSession().getAttribute("newUiCsrfToken");String v=q.getHeader("X-CSRF-Token");if(e==null||v==null||!String.valueOf(e).equals(v))throw new SecurityException("Token CSRF tidak valid.");}
    private static Long requiredId(String v){Long x=id(v);if(x==null)throw new IllegalArgumentException("ID wajib diisi.");return x;}private static Long id(String v){if(clean(v)==null)return null;try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException("ID tidak valid.");}}private static int integer(String v,int d){try{return Integer.parseInt(v);}catch(Exception e){return d;}}private static String clean(String v){return v==null||v.trim().length()==0?null:v.trim();}private static String text(String v,String d){return clean(v)==null?d:v.trim();}private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}
}
