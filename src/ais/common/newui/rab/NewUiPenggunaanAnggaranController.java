package ais.common.newui.rab;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.rab.NewUiPenggunaanAnggaranService.Filter;
import ais.common.newui.rab.NewUiPenggunaanAnggaranService.History;
import ais.common.newui.rab.NewUiPenggunaanAnggaranService.ReprocessResult;
import ais.common.newui.rab.NewUiPenggunaanAnggaranService.Row;
import ais.common.newui.rab.NewUiPenggunaanAnggaranService.Snapshot;
import ais.common.newui.rab.NewUiPenggunaanAnggaranService.WorkspaceOption;

/** JSON controller native; pekerjaan proses ulang dijalankan asinkron seperti server-push ZK. */
public final class NewUiPenggunaanAnggaranController {
    private static final String MODULE="rab", PAGE="penggunaan_anggaran";
    private static final Map<String,Job> JOBS=new ConcurrentHashMap<String,Job>();
    private NewUiPenggunaanAnggaranController() { }

    public static void handle(HttpServletRequest req,HttpServletResponse resp)throws Exception{
        resp.setContentType("application/json; charset=UTF-8");resp.setHeader("Cache-Control","no-store");
        JSONObject json=new JSONObject();
        try{
            String action=text(req.getParameter("action"),"list");
            String privilege=("reprocess_start".equals(action)||"reprocess_status".equals(action))?"update":"list";
            if(!NewUiRouteGuard.isActionAuthorized(req,MODULE,PAGE,privilege)){resp.setStatus(403);fail(json,"ACTION_FORBIDDEN","Hak akses tidak tersedia.");write(resp,json);return;}
            NewUiPenggunaanAnggaranService service=new NewUiPenggunaanAnggaranService();
            if("list".equals(action)) encode(json,service.load(filter(req)));
            else if("history".equals(action)) encodeHistory(json,service.history(id(req.getParameter("id"))));
            else if("reprocess_start".equals(action)){
                requirePostCsrf(req); purgeJobs(); final Filter f=filter(req); final Job job=new Job();
                job.id=UUID.randomUUID().toString();job.owner=req.getSession().getId();JOBS.put(job.id,job);
                Thread worker=new Thread(new Runnable(){public void run(){try{job.state="RUNNING";job.message="Menyiapkan proses ulang...";job.result=new NewUiPenggunaanAnggaranService().reprocess(f,new NewUiPenggunaanAnggaranService.Progress(){public void update(int p,String m){job.percent=p;job.message=m;}});job.percent=100;job.state="DONE";}catch(Exception e){job.state="FAILED";job.message=e.getMessage()==null?"Proses ulang gagal.":e.getMessage();try{ais.common.ErrorAuditUtil.record(e,"NewUiPenggunaanAnggaranController.reprocess");}catch(Exception ignored){}}finally{job.finishedAt=System.currentTimeMillis();}}},"newui-penggunaan-anggaran-"+job.id);
                worker.setDaemon(true);worker.start();json.put("jobId",job.id);
            } else if("reprocess_status".equals(action)){
                Job job=JOBS.get(req.getParameter("jobId"));if(job==null||!req.getSession().getId().equals(job.owner))throw new IllegalArgumentException("Pekerjaan proses ulang tidak ditemukan atau sudah kedaluwarsa.");encodeJob(json,job);
            } else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("csrfHeader",ais.common.newui.NewUiCsrfUtil.LEGACY_HEADER).put("csrfToken",ais.common.newui.NewUiCsrfUtil.getTokenOkFlat(req));json.put("ok",true);
        }catch(SecurityException e){resp.setStatus(403);fail(json,"CSRF_INVALID",e.getMessage());}
        catch(IllegalArgumentException e){resp.setStatus(422);fail(json,"VALIDATION_FAILED",e.getMessage());}
        catch(Exception e){resp.setStatus(500);fail(json,"INTERNAL_ERROR","Gagal memproses penggunaan anggaran.");try{ais.common.ErrorAuditUtil.record(e,"NewUiPenggunaanAnggaranController");}catch(Exception ignored){}}
        write(resp,json);
    }

    private static Filter filter(HttpServletRequest q){Filter f=new Filter();f.q=q.getParameter("q");f.workspaceId=id(q.getParameter("workspaceId"));f.start=date(q.getParameter("start"));f.end=date(q.getParameter("end"));f.activeOnly=!"false".equalsIgnoreCase(q.getParameter("active"));f.page=integer(q.getParameter("page"),0);f.size=integer(q.getParameter("size"),20);source(f,q,"jurnal");source(f,q,"pengadaan");source(f,q,"uang_muka");source(f,q,"pertanggungjawaban");source(f,q,"saldo_awal");source(f,q,"gaji");source(f,q,"kas_kecil");source(f,q,"kas_besar");return f;}
    private static void source(Filter f,HttpServletRequest q,String k){String v=q.getParameter("source_"+k);f.sources.put(k,Boolean.valueOf(v==null||"true".equalsIgnoreCase(v)));}
    private static void encode(JSONObject j,Snapshot d)throws Exception{JSONArray rows=new JSONArray();for(Row x:d.rows)rows.put(new JSONObject().put("id",x.id).put("code",x.code).put("name",x.name).put("note",x.note).put("ref",x.ref).put("time",x.time==null?JSONObject.NULL:x.time.getTime()).put("amount",x.amount).put("active",x.active).put("workspaceId",x.workspaceId).put("workspace",x.workspace).put("budget",x.budget).put("source",x.source).put("sopId",x.sopId).put("sop",x.sop).put("sopName",x.sopName).put("transferStatus",x.transferStatus));JSONArray ws=new JSONArray();for(WorkspaceOption x:d.workspaces)ws.put(new JSONObject().put("id",x.id).put("label",x.label));j.put("rows",rows).put("workspaces",ws).put("total",d.total).put("active",d.active).put("inactive",d.inactive).put("totalAmount",d.totalAmount).put("sourceCounts",map(d.sourceCounts)).put("transferCounts",map(d.transferCounts));}
    private static void encodeJob(JSONObject j,Job x)throws Exception{j.put("jobId",x.id).put("state",x.state).put("percent",x.percent).put("message",x.message);if(x.result!=null)j.put("processed",x.result.processed).put("duplicatesRemoved",x.result.duplicatesRemoved).put("perSource",map(x.result.perSource));}
    private static void encodeHistory(JSONObject j,java.util.List<History>v)throws Exception{JSONArray a=new JSONArray();for(History x:v)a.put(new JSONObject().put("revision",x.revision).put("type",x.type).put("timestamp",x.timestamp).put("by",x.by).put("code",x.code).put("name",x.name).put("amount",x.amount).put("active",x.active).put("note",x.note));j.put("history",a);}
    private static JSONObject map(Map<String,Integer>m)throws Exception{JSONObject j=new JSONObject();for(Map.Entry<String,Integer>e:m.entrySet())j.put(e.getKey(),e.getValue());return j;}
    private static void requirePostCsrf(HttpServletRequest q){if(!"POST".equalsIgnoreCase(q.getMethod()))throw new IllegalArgumentException("Metode HTTP tidak valid.");Object expected=q.getSession().getAttribute("newUiCsrfToken");String actual=q.getHeader("X-CSRF-Token");if(expected==null||actual==null||!String.valueOf(expected).equals(actual))throw new SecurityException("Token CSRF tidak valid.");}
    private static void purgeJobs(){long now=System.currentTimeMillis();for(Map.Entry<String,Job>e:JOBS.entrySet())if(e.getValue().finishedAt>0&&now-e.getValue().finishedAt>3600000L)JOBS.remove(e.getKey());}
    private static Long id(String v){if(v==null||v.trim().length()==0)return null;try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException("Workspace tidak valid.");}}
    private static Date date(String v){if(v==null||v.trim().length()==0)return null;try{return new SimpleDateFormat("yyyy-MM-dd").parse(v);}catch(Exception e){throw new IllegalArgumentException("Tanggal tidak valid.");}}
    private static int integer(String v,int d){try{return Integer.parseInt(v);}catch(Exception e){return d;}}private static String text(String v,String d){return v==null||v.trim().length()==0?d:v.trim();}private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}
    /**
     * Pekerjaan latar bersarang milik {@link NewUiPenggunaanAnggaranController} untuk job. Tipe ini membatasi
     * state yang dibawa ke eksekusi asinkron dan tidak boleh membawa session request secara implisit.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiPenggunaanAnggaranController}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
     * kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String id}, {@code String owner},
     * {@code String state}, {@code String message}, {@code int percent}, {@code long finishedAt}, {@code
     * ReprocessResult result}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiPenggunaanAnggaranController
     */
    private static final class Job{String id,owner,state="QUEUED",message="Menunggu proses...";volatile int percent;volatile long finishedAt;volatile ReprocessResult result;}
}
