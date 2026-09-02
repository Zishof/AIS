package ais.common.newui.finance;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
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
import ais.common.newui.finance.NewUiInstallmentPaymentService.Filter;
import ais.common.newui.finance.NewUiInstallmentPaymentService.HistoryRow;
import ais.common.newui.finance.NewUiInstallmentPaymentService.Option;
import ais.common.newui.finance.NewUiInstallmentPaymentService.Row;
import ais.common.newui.finance.NewUiInstallmentPaymentService.Snapshot;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/** Native CicilanPembayaran endpoint with self-scope and configured reversal guard. */
public final class NewUiInstallmentPaymentController {
    private static final ThreadLocal<SimpleDateFormat> ISO=new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };
    private NewUiInstallmentPaymentController(){}
    public static void handle(HttpServletRequest q,HttpServletResponse r)throws Exception{
        String action=text(q.getParameter("action"),"list");
        if("receipt".equals(action)){receipt(q,r);return;}
        r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");JSONObject j=new JSONObject();
        try{
            String privilege="reverse".equals(action)?"delete":"list";
            if(!NewUiRouteGuard.isActionAuthorized(q,"root","cicilan_pembayaran",privilege)){r.setStatus(403);fail(j,"ACTION_FORBIDDEN","Hak akses tidak tersedia.");write(r,j);return;}
            Tbmuser user=Common.getCurrentUser(q);Scope scope=scope(user);NewUiInstallmentPaymentService s=new NewUiInstallmentPaymentService();
            if("list".equals(action))encode(j,s.load(filter(q,scope)));
            else if("options".equals(action)){options(j,s.options());j.put("canReverse",!scope.restricted&&canReverse(user)).put("selfScoped",scope.restricted);}
            else if("history".equals(action))history(j,s.history(requiredId(q.getParameter("id")),scope.studentId,scope.candidateId));
            else if("reverse".equals(action)){csrf(q);if(scope.restricted||!canReverse(user))throw new SecurityException("Reversal hanya tersedia untuk administrator yang dikonfigurasi.");s.reverse(requiredId(q.getParameter("id")),null,null);}
            else throw new IllegalArgumentException("Aksi tidak dikenal.");j.put("csrfHeader",ais.common.newui.NewUiCsrfUtil.LEGACY_HEADER).put("csrfToken",ais.common.newui.NewUiCsrfUtil.getTokenOkFlat(q));j.put("ok",true);
        }catch(SecurityException e){r.setStatus(403);fail(j,"FORBIDDEN",e.getMessage());}
        catch(IllegalArgumentException e){r.setStatus(422);fail(j,"VALIDATION_FAILED",e.getMessage());}
        catch(Exception e){r.setStatus(500);fail(j,"INTERNAL_ERROR","Gagal memproses cicilan pembayaran.");try{ais.common.ErrorAuditUtil.record(e,"NewUiInstallmentPaymentController");}catch(Exception ignored){}}
        write(r,j);
    }
    private static void receipt(HttpServletRequest q,HttpServletResponse r)throws Exception{if(!NewUiRouteGuard.isActionAuthorized(q,"root","cicilan_pembayaran","list")){r.sendError(403);return;}Tbmuser u=Common.getCurrentUser(q);Scope sc=scope(u);File f=new NewUiInstallmentPaymentService().receipt(requiredId(q.getParameter("id")),sc.studentId,sc.candidateId);if(f==null||!f.isFile()){r.sendError(404,"Bukti pembayaran tidak berhasil dibuat.");return;}r.setContentType("application/pdf");r.setHeader("Cache-Control","no-store");r.setHeader("Content-Disposition","inline; filename=\"bukti-pembayaran.pdf\"");if(f.length()<=Integer.MAX_VALUE)r.setContentLength((int)f.length());FileInputStream in=new FileInputStream(f);try{OutputStream out=r.getOutputStream();byte[]b=new byte[8192];int n;while((n=in.read(b))>=0)out.write(b,0,n);out.flush();}finally{in.close();}}
    private static Scope scope(Tbmuser u){Scope s=new Scope();if(u!=null&&u.getMahasiswa()!=null){s.studentId=u.getMahasiswa().getId();s.restricted=true;}else if(u!=null&&u.getBiodataCalonMahasiswa()!=null){s.candidateId=u.getBiodataCalonMahasiswa().getId();s.restricted=true;}return s;}
    private static boolean canReverse(Tbmuser u){if(u==null)return false;try{if(u.hakAkses()!=null&&Tbmrole.ADMINISTRATOR.equalsIgnoreCase(u.hakAkses().getRoleId()))return true;String roles=Common.getKonfigurasi("admin_yang_bisa_menghapus_data_pembayaran_mahasiswa","am").getNilai();if(u.hakAkses()!=null&&contains(roles,u.hakAkses().getRoleId()))return true;String users=Common.getKonfigurasi("admin_lain_bisa_menghapus_pembayaran_mahasiswa","").getNilai();return contains(users,u.getUserId());}catch(Exception e){return false;}}
    private static boolean contains(String source,String value){if(source==null||value==null)return false;for(String x:source.split(";"))if(value.trim().equalsIgnoreCase(x.trim()))return true;return false;}
    private static Filter filter(HttpServletRequest q,Scope s){Filter f=new Filter();f.studentId=s.studentId;f.candidateId=s.candidateId;f.query=q.getParameter("q");f.academicYear=q.getParameter("academicYear");f.cohort=integerObject(q.getParameter("cohort"));f.facultyId=id(q.getParameter("facultyId"));f.studyProgramId=id(q.getParameter("studyProgramId"));String semester=clean(q.getParameter("semester"));f.semesterOdd=semester==null?null:Boolean.valueOf("odd".equals(semester));f.start=date(q.getParameter("start"));f.end=date(q.getParameter("end"));f.depositOnly=bool(q.getParameter("depositOnly"));f.itemIds=ids(q.getParameterValues("itemId"),q.getParameter("itemIds"));f.page=Math.max(0,integer(q.getParameter("page"),0));f.size=Math.min(100,Math.max(10,integer(q.getParameter("size"),20)));return f;}
    private static void encode(JSONObject j,Snapshot s)throws Exception{JSONArray a=new JSONArray();for(Row x:s.rows)a.put(new JSONObject().put("id",x.id).put("activityId",x.activityId).put("identifier",x.identifier).put("name",x.name).put("faculty",x.faculty).put("studyProgram",x.studyProgram).put("academicYear",x.academicYear).put("semester",x.semester).put("stage",x.stage).put("activity",x.activity).put("paymentType",x.paymentType).put("item",x.item).put("date",format(x.date)).put("receiptDate",format(x.receiptDate)).put("amount",x.amount).put("deposit",x.deposit).put("penalty",x.penalty).put("billed",x.billed).put("activityPaid",x.activityPaid).put("remaining",x.remaining).put("note",x.note));j.put("rows",a).put("total",s.total).put("amount",s.amount).put("deposit",s.deposit).put("penalty",s.penalty);}
    private static void history(JSONObject j,List<HistoryRow>v)throws Exception{JSONArray a=new JSONArray();for(HistoryRow x:v)a.put(new JSONObject().put("revision",x.revision).put("date",format(x.date)).put("user",x.user).put("type",x.type).put("amount",x.amount).put("deposit",x.deposit).put("note",x.note));j.put("rows",a);}
    private static void options(JSONObject j,NewUiInstallmentPaymentService.Options o)throws Exception{j.put("items",options(o.items)).put("faculties",options(o.faculties)).put("studyPrograms",options(o.studyPrograms));}private static JSONArray options(List<Option>v)throws Exception{JSONArray a=new JSONArray();for(Option x:v)a.put(new JSONObject().put("id",x.id).put("label",x.label).put("parentId",x.parentId));return a;}
    private static List<Long>ids(String[]many,String csv){List<Long>o=new ArrayList<Long>();if(many!=null)for(String v:many)addId(o,v);if(clean(csv)!=null)for(String v:csv.split(","))addId(o,v);return o;}private static void addId(List<Long>o,String v){Long x=id(v);if(x!=null&&!o.contains(x))o.add(x);}
    private static synchronized String format(Date d){return d==null?null:ISO.get().format(d);}private static synchronized Date date(String v){if(clean(v)==null)return null;try{return ISO.get().parse(v);}catch(Exception e){throw new IllegalArgumentException("Format tanggal harus yyyy-MM-dd.");}}
    /**
     * Helper validasi dan normalisasi request controller: memeriksa CSRF/metode HTTP, mengurai id/angka/boolean,
     * membersihkan teks, serta membentuk respons JSON gagal atau sukses.
     *
     * <p>Seluruh helper bersifat privat dan stateless; autentikasi, otorisasi scope mahasiswa/calon mahasiswa, dan
     * transaksi pembayaran tetap didelegasikan ke service serta filter keamanan terkait.</p>
     *
     * @see NewUiInstallmentPaymentController
     */
    private static void csrf(HttpServletRequest q){if(!"POST".equalsIgnoreCase(q.getMethod()))throw new SecurityException("Metode HTTP tidak valid.");Object e=q.getSession().getAttribute("newUiCsrfToken");String v=q.getHeader("X-CSRF-Token");if(e==null||v==null||!String.valueOf(e).equals(v))throw new SecurityException("Token CSRF tidak valid.");}private static Long requiredId(String v){Long x=id(v);if(x==null)throw new IllegalArgumentException("ID wajib diisi.");return x;}private static Long id(String v){if(clean(v)==null)return null;try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException("ID tidak valid.");}}private static Integer integerObject(String v){return clean(v)==null?null:Integer.valueOf(integer(v,0));}private static int integer(String v,int d){try{return Integer.parseInt(v);}catch(Exception e){return d;}}private static boolean bool(String v){return"true".equalsIgnoreCase(v);}private static String clean(String v){return v==null||v.trim().length()==0?null:v.trim();}private static String text(String v,String d){return clean(v)==null?d:v.trim();}private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}/** Scope otorisasi hasil resolusi pengguna; membatasi operasi ke mahasiswa atau calon mahasiswa yang diizinkan. */
    private static final class Scope{Long studentId,candidateId;boolean restricted;}
}
