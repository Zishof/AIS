package ais.common.newui.akunting;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.akunting.NewUiJournalService.BatchResult;
import ais.common.newui.akunting.NewUiJournalService.Draft;
import ais.common.newui.akunting.NewUiJournalService.Filter;
import ais.common.newui.akunting.NewUiJournalService.HistoryRow;
import ais.common.newui.akunting.NewUiJournalService.Line;
import ais.common.newui.akunting.NewUiJournalService.LineDraft;
import ais.common.newui.akunting.NewUiJournalService.Option;
import ais.common.newui.akunting.NewUiJournalService.Row;
import ais.common.newui.akunting.NewUiJournalService.Snapshot;
import ais.database.model.Tbmuser;

/** JSON/PDF/CSV endpoint for the native general-journal screen. */
public final class NewUiJournalController {
    private static final ThreadLocal<SimpleDateFormat> ISO=new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };
    private NewUiJournalController(){}
    public static void handle(HttpServletRequest q,HttpServletResponse r)throws Exception{
        String action=text(q.getParameter("action"),"list");
        if("receipt".equals(action)){receipt(q,r);return;}if("combinedReceipt".equals(action)){combinedReceipt(q,r);return;}if("attachment".equals(action)){attachment(q,r);return;}if("export".equals(action)){export(q,r);return;}
        r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");JSONObject j=new JSONObject();
        try{String privilege=privilege(action,q);if(!NewUiRouteGuard.isActionAuthorized(q,"akunting",routePage(q),privilege)){r.setStatus(403);fail(j,"ACTION_FORBIDDEN","Hak akses jurnal tidak tersedia.");write(r,j);return;}NewUiJournalService s=new NewUiJournalService();Tbmuser user=Common.getCurrentUser(q);
            if("list".equals(action))encode(j,s.load(filter(q)));
            else if("detail".equals(action))j.put("row",row(s.detail(requiredId(q.getParameter("id")))));
            else if("options".equals(action)){options(j,s.options());j.put("admin",Common.getApakahAdmin()).put("deleteAllEnabled",Common.getApakahAdmin()&&Common.bolehKonfigurasi("tampilkan_bersihkan_jurnal",ais.database.model.Konfigurasi.TIDAK_AKTIF)).put("csrfHeader","X-CSRF-Token").put("csrfToken",csrfToken(q));}
            else if("history".equals(action))history(j,s.history(requiredId(q.getParameter("id"))));
            else if("save".equals(action)){csrf(q);j.put("id",s.save(draft(q),user));}
            else if("delete".equals(action)){csrf(q);s.delete(requiredId(q.getParameter("id")));}
            else if("post".equals(action)){csrf(q);s.post(requiredId(q.getParameter("id")),user,date(q.getParameter("postingDate")),q.getParameter("note"));}
            else if("unpost".equals(action)){csrf(q);s.unpost(requiredId(q.getParameter("id")));}
            else if("postingActive".equals(action)){csrf(q);s.setPostingActive(requiredId(q.getParameter("id")),bool(q.getParameter("value")));}
            else if("sync".equals(action)){csrf(q);batch(j,s.sync(filter(q)));}
            else if("postBatch".equals(action)){csrf(q);batch(j,s.postBatch(filter(q),user,date(q.getParameter("postingDate")),q.getParameter("note")));}
            else if("unpostBatch".equals(action)){csrf(q);batch(j,s.unpostBatch(filter(q)));}
            else if("activateBatch".equals(action)){csrf(q);batch(j,s.activateBatch(filter(q)));}
            else if("import".equals(action)){csrf(q);NewUiJournalService.ImportResult x=s.importCsv(q.getParameter("csv"),user);batch(j,x);j.put("ids",new JSONArray(x.ids));}
            else if("upload".equals(action)){csrf(q);upload(q,s,user);}
            else if("cleanDuplicates".equals(action)){csrf(q);admin();confirm(q,"BERSIHKAN DUPLIKAT");NewUiJournalService.MaintenanceResult x=s.cleanDuplicates();j.put("groups",x.groups).put("lines",x.lines);}
            else if("deleteAll".equals(action)){csrf(q);admin();if(!Common.bolehKonfigurasi("tampilkan_bersihkan_jurnal",ais.database.model.Konfigurasi.TIDAK_AKTIF))throw new SecurityException("Fitur hapus seluruh jurnal tidak diaktifkan.");confirm(q,"HAPUS SEMUA JURNAL");NewUiJournalService.MaintenanceResult x=s.deleteAll();j.put("groups",x.groups).put("lines",x.lines);}
            else throw new IllegalArgumentException("Aksi jurnal tidak dikenal.");j.put("ok",true);
        }catch(SecurityException e){r.setStatus(403);fail(j,"FORBIDDEN",e.getMessage());}catch(IllegalArgumentException e){r.setStatus(422);fail(j,"VALIDATION_FAILED",e.getMessage());}catch(IllegalStateException e){r.setStatus(409);fail(j,"INVALID_STATE",e.getMessage());}catch(Exception e){r.setStatus(500);fail(j,"INTERNAL_ERROR","Gagal memproses jurnal: "+safe(e.getMessage()));try{ais.common.ErrorAuditUtil.record(e,"NewUiJournalController");}catch(Exception ignored){}}write(r,j);
    }
    private static String privilege(String a,HttpServletRequest q){if("save".equals(a)){try{return jsonId(new JSONObject(q.getParameter("payload")),"id")==null?"create":"update";}catch(Exception e){return"create";}}if("delete".equals(a)||"cleanDuplicates".equals(a)||"deleteAll".equals(a))return"delete";if("post".equals(a)||"unpost".equals(a)||"postBatch".equals(a)||"unpostBatch".equals(a)||"activateBatch".equals(a)||"postingActive".equals(a))return"approve";if("sync".equals(a)||"import".equals(a)||"upload".equals(a))return"update";return"list";}
    private static Draft draft(HttpServletRequest q)throws Exception{JSONObject p=new JSONObject(required(q.getParameter("payload"),"Payload jurnal wajib diisi."));Draft d=new Draft();d.id=jsonId(p,"id");d.code=p.optString("code",null);d.description=p.optString("description",null);d.journalType=p.optString("journalType",null);d.invoice=p.optString("invoice",null);d.payee=p.optString("payee",null);d.date=date(p.optString("date",null));d.typeId=jsonId(p,"typeId");d.workUnitId=jsonId(p,"workUnitId");d.workspaceId=jsonId(p,"workspaceId");JSONArray a=p.optJSONArray("lines");if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);LineDraft l=new LineDraft();l.id=jsonId(x,"id");l.accountId=jsonId(x,"accountId");l.description=x.optString("description",null);l.debit=x.optDouble("debit",0);l.credit=x.optDouble("credit",0);d.lines.add(l);}return d;}
    private static Filter filter(HttpServletRequest q){Filter f=new Filter();f.id=firstId(q.getParameter("filterId"),q.getParameter("grup"));f.query=q.getParameter("q");f.lineQuery=q.getParameter("lineQuery");f.start=dateCompat(first(q.getParameter("start"),q.getParameter("mulai")));f.end=dateCompat(first(q.getParameter("end"),q.getParameter("sampai")));f.typeId=id(q.getParameter("typeId"));f.workUnitId=id(q.getParameter("workUnitId"));f.workspaceId=firstId(q.getParameter("workspaceId"),q.getParameter("workspace"));f.journalType=q.getParameter("journalType");f.min=decimal(q.getParameter("min"));f.max=decimal(q.getParameter("max"));f.postingId=id(q.getParameter("postingId"));String posted=clean(q.getParameter("posted"));f.posted=posted==null?null:Boolean.valueOf("true".equals(posted));f.unbalanced=bool(q.getParameter("unbalanced"));f.unpostedActive=bool(q.getParameter("unpostedActive"));f.accountIds=ids(first(q.getParameter("accountIds"),q.getParameter("akun")));f.closingId=id(q.getParameter("closing"));f.closingBefore=dateCompat(q.getParameter("tgl_closing"));f.postedActive=tri(q.getParameter("sudah_posting"));f.closed=tri(q.getParameter("sudah_closing"));f.allJournals=bool(q.getParameter("semua_jurnal"));f.sourceEntity=clean(q.getParameter("entitas"));f.ref=clean(q.getParameter("ref"));f.kind=clean(q.getParameter("jenis"));String[]source={"pertangungjawaban","kasKecil","kasBesar","pajak","saldoAwalMasterAsset","pemesananPengadaanMasterAsset"};for(String x:source)if(bool(q.getParameter(x))){f.sourceEntity=x;break;}if("pemesananPengadaanMasterAsset".equals(f.sourceEntity))f.ref=bool(q.getParameter("dp_balik"))?"DP_PEKERJAAN":"KOSONG";if("saldoAwalMasterAsset".equals(f.sourceEntity))f.ref=bool(q.getParameter("dp_vendor"))?"DP_BALIK_PEKERJAAN":clean(q.getParameter("termin"))==null?"KOSONG":"PEKERJAAN_NON_DP";f.page=Math.max(0,integer(q.getParameter("page"),0));f.size=Math.min(100,Math.max(10,integer(q.getParameter("size"),20)));return f;}
    private static void encode(JSONObject j,Snapshot s)throws Exception{JSONArray a=new JSONArray();for(Row x:s.rows)a.put(row(x));j.put("rows",a).put("total",s.total).put("debit",s.debit).put("credit",s.credit);}
    private static JSONObject row(Row x)throws Exception{JSONObject o=new JSONObject().put("id",x.id).put("code",x.code).put("description",x.description).put("date",format(x.date)).put("typeId",x.typeId).put("type",x.type).put("workUnitId",x.workUnitId).put("workUnit",x.workUnit).put("workspaceId",x.workspaceId).put("workspace",x.workspace).put("journalType",x.journalType).put("invoice",x.invoice).put("payee",x.payee).put("user",x.user).put("debit",x.debit).put("credit",x.credit).put("balanced",x.balanced).put("hasAttachment",x.hasAttachment).put("postingId",x.postingId).put("postingActive",x.postingActive).put("postingDate",format(x.postingDate)).put("closingId",x.closingId).put("closing",x.closing);JSONArray a=new JSONArray();for(Line l:x.lines)a.put(new JSONObject().put("id",l.id).put("accountId",l.accountId).put("accountCode",l.accountCode).put("account",l.account).put("description",l.description).put("debit",l.debit).put("credit",l.credit).put("status",l.status));o.put("lines",a);return o;}
    private static void options(JSONObject j,NewUiJournalService.Options o)throws Exception{j.put("accounts",options(o.accounts)).put("types",options(o.types)).put("workUnits",options(o.workUnits)).put("workspaces",options(o.workspaces)).put("lastClosing",format(o.lastClosing));}
    private static JSONArray options(List<Option>v)throws Exception{JSONArray a=new JSONArray();for(Option x:v)a.put(new JSONObject().put("id",x.id).put("label",x.label).put("parentId",x.parentId));return a;}
    private static void history(JSONObject j,List<HistoryRow>v)throws Exception{JSONArray a=new JSONArray();for(HistoryRow x:v)a.put(new JSONObject().put("revision",x.revision).put("date",format(x.date)).put("type",x.type).put("code",x.code).put("description",x.description).put("debit",x.debit).put("credit",x.credit).put("postingId",x.postingId));j.put("rows",a);}
    private static void batch(JSONObject j,BatchResult b)throws Exception{j.put("success",b.success).put("errors",new JSONArray(b.errors));}
    private static void receipt(HttpServletRequest q,HttpServletResponse r)throws Exception{if(!NewUiRouteGuard.isActionAuthorized(q,"akunting",routePage(q),"list")){r.sendError(403);return;}File f=new NewUiJournalService().receipt(requiredId(q.getParameter("id")));stream(r,f,"application/pdf","inline; filename=\"bukti-jurnal.pdf\"");}
    private static void combinedReceipt(HttpServletRequest q,HttpServletResponse r)throws Exception{if(!NewUiRouteGuard.isActionAuthorized(q,"akunting",routePage(q),"list")||!Common.getApakahAdmin()){r.sendError(403);return;}File f=new NewUiJournalService().combinedReceipt(filter(q));stream(r,f,"application/pdf","inline; filename=\"bukti-jurnal-gabungan.pdf\"");}
    private static void attachment(HttpServletRequest q,HttpServletResponse r)throws Exception{if(!NewUiRouteGuard.isActionAuthorized(q,"akunting",routePage(q),"list")){r.sendError(403);return;}NewUiJournalService.Attachment a=new NewUiJournalService().attachment(requiredId(q.getParameter("id")));if(a==null){r.sendError(404);return;}if(clean(a.link)!=null){r.sendRedirect(a.link);return;}stream(r,a.file,"application/octet-stream","attachment; filename=\""+fileName(a.name)+"\"");}
    private static void upload(HttpServletRequest q,NewUiJournalService s,Tbmuser u)throws Exception{ServletFileUpload parser=new ServletFileUpload(new DiskFileItemFactory());parser.setFileSizeMax(20L*1024L*1024L);List<FileItem>items=parser.parseRequest(q);Long id=null;FileItem file=null;for(FileItem x:items)if(x.isFormField()&&"id".equals(x.getFieldName()))id=Long.valueOf(x.getString("UTF-8"));else if(!x.isFormField()&&x.getSize()>0)file=x;if(id==null||file==null)throw new IllegalArgumentException("ID jurnal dan file bukti wajib diisi.");InputStream in=file.getInputStream();try{s.upload(id,file.getName(),in,u);}finally{in.close();}}
    private static void export(HttpServletRequest q,HttpServletResponse r)throws Exception{if(!NewUiRouteGuard.isActionAuthorized(q,"akunting",routePage(q),"list")){r.sendError(403);return;}Filter f=filter(q);f.page=0;f.size=100;NewUiJournalService s=new NewUiJournalService();r.setContentType("text/csv; charset=UTF-8");r.setHeader("Content-Disposition","attachment; filename=\"grup-transaksi.csv\"");PrintWriter w=r.getWriter();w.write('\ufeff');w.println("kode,tanggal,jenis_transaksi,keterangan,no_bukti,status,total_debet,total_kredit,satuan_kerja,workspace");int read=0,total=1;while(read<total){Snapshot x=s.load(f);total=x.total;for(Row z:x.rows){csv(w,z.code);w.print(',');csv(w,format(z.date));w.print(',');csv(w,z.type);w.print(',');csv(w,z.description);w.print(',');csv(w,z.postingId);w.print(',');csv(w,z.closingId!=null?"CLOSING":z.postingId!=null?"POSTED":"DRAFT");w.print(',');w.print(z.debit);w.print(',');w.print(z.credit);w.print(',');csv(w,z.workUnit);w.print(',');csv(w,z.workspace);w.println();read++;}f.page++;if(x.rows.isEmpty())break;}w.flush();}
    private static void stream(HttpServletResponse r,File f,String type,String disposition)throws Exception{if(f==null||!f.isFile()){r.sendError(404);return;}r.setContentType(type);r.setHeader("Cache-Control","no-store");r.setHeader("Content-Disposition",disposition);if(f.length()<=Integer.MAX_VALUE)r.setContentLength((int)f.length());FileInputStream in=new FileInputStream(f);try{OutputStream out=r.getOutputStream();byte[]b=new byte[8192];int n;while((n=in.read(b))>=0)out.write(b,0,n);out.flush();}finally{in.close();}}
    private static void csv(PrintWriter w,Object v){String s=v==null?"":String.valueOf(v);w.print('"');w.print(s.replace("\"","\"\""));w.print('"');}
    /**
     * Terbitkan token CSRF pada aksi bootstrap baca ({@code options}).
     *
     * <p>Sebelumnya {@link #csrf} menuntut atribut sesi {@code newUiCsrfToken}
     * yang tidak pernah diterbitkan controller ini, sehingga seluruh jalur
     * tulis jurnal hanya berhasil bila pengguna kebetulan sudah membuka salah
     * satu layar lain yang mencetaknya pada sesi yang sama. Idiomnya sama
     * dengan {@code NewUiPembelianController.csrfToken}.</p>
     */
    private static String csrfToken(HttpServletRequest q){String t=ais.common.newui.NewUiCsrfUtil.getToken(q.getSession(true));if(q.getSession().getAttribute("newUiCsrfToken")==null)q.getSession().setAttribute("newUiCsrfToken",t);return String.valueOf(q.getSession().getAttribute("newUiCsrfToken"));}
    private static void csrf(HttpServletRequest q){if(!"POST".equalsIgnoreCase(q.getMethod()))throw new SecurityException("Metode HTTP tidak valid.");Object e=q.getSession().getAttribute("newUiCsrfToken");String v=q.getHeader("X-CSRF-Token");if(e==null||v==null||!String.valueOf(e).equals(v))throw new SecurityException("Token CSRF tidak valid.");}
    private static List<Long>ids(String csv){List<Long>o=new ArrayList<Long>();if(clean(csv)!=null)for(String v:csv.split(",")){Long x=id(v);if(x!=null&&!o.contains(x))o.add(x);}return o;}
    private static Long jsonId(JSONObject o,String k){Object v=o.opt(k);return v==null||JSONObject.NULL.equals(v)||clean(String.valueOf(v))==null?null:id(String.valueOf(v));}
    private static Long requiredId(String v){Long x=id(v);if(x==null)throw new IllegalArgumentException("ID jurnal wajib diisi.");return x;}
    private static Long id(String v){if(clean(v)==null)return null;try{return Long.valueOf(v.trim());}catch(Exception e){throw new IllegalArgumentException("ID tidak valid.");}}
    private static Long firstId(String a,String b){return id(first(a,b));}
    private static Double decimal(String v){if(clean(v)==null)return null;try{return Double.valueOf(v.trim());}catch(Exception e){throw new IllegalArgumentException("Nominal filter tidak valid.");}}
    private static int integer(String v,int d){try{return Integer.parseInt(v);}catch(Exception e){return d;}}
    private static boolean bool(String v){return"true".equalsIgnoreCase(v);}
    private static synchronized String format(Date d){return d==null?null:ISO.get().format(d);}
    private static synchronized Date date(String v){if(clean(v)==null)return null;try{return ISO.get().parse(v.trim());}catch(Exception e){throw new IllegalArgumentException("Format tanggal harus yyyy-MM-dd.");}}
    private static synchronized Date dateCompat(String v){if(clean(v)==null)return null;String[]p={"yyyy-MM-dd","dd-MM-yyyy","dd/MM/yyyy","yyyy/MM/dd"};for(String x:p)try{return new SimpleDateFormat(x).parse(v.trim());}catch(Exception ignored){}throw new IllegalArgumentException("Format tanggal tidak valid: "+v);}
    private static Boolean tri(String v){return clean(v)==null?null:Boolean.valueOf(v.trim());}
    private static String first(String a,String b){return clean(a)!=null?a:b;}
    private static String required(String v,String m){if(clean(v)==null)throw new IllegalArgumentException(m);return v;}
    private static String clean(String v){return v==null||v.trim().length()==0?null:v.trim();}
    private static String text(String v,String d){return clean(v)==null?d:v.trim();}
    private static String safe(String v){return clean(v)==null?"kesalahan internal":v.replace('\n',' ').replace('\r',' ');}
    private static String fileName(String v){String n=clean(v)==null?"bukti-transaksi":v;return n.replaceAll("[^A-Za-z0-9._-]","_");}
    private static void admin(){if(!Common.getApakahAdmin())throw new SecurityException("Operasi ini hanya tersedia untuk administrator.");}
    private static void confirm(HttpServletRequest q,String expected){if(!expected.equals(q.getParameter("confirmation")))throw new SecurityException("Frasa konfirmasi tidak sesuai.");}
    private static String routePage(HttpServletRequest q){return"semua_grup_transaksi".equals(q.getParameter("page"))?"semua_grup_transaksi":"grup_transaksi";}
    private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}
    private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}
}
