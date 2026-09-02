package ais.common.newui.akunting;

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
import ais.common.newui.akunting.NewUiTransferWorkflowService.Filter;
import ais.common.newui.akunting.NewUiTransferWorkflowService.Option;
import ais.common.newui.akunting.NewUiTransferWorkflowService.Options;
import ais.common.newui.akunting.NewUiTransferWorkflowService.PaymentMethodDraft;
import ais.common.newui.akunting.NewUiTransferWorkflowService.PaymentMethodRow;
import ais.common.newui.akunting.NewUiTransferWorkflowService.ProcessDraft;
import ais.common.newui.akunting.NewUiTransferWorkflowService.ProcessRow;
import ais.common.newui.akunting.NewUiTransferWorkflowService.ProcessSnapshot;
import ais.common.newui.akunting.NewUiTransferWorkflowService.Row;
import ais.common.newui.akunting.NewUiTransferWorkflowService.Snapshot;
import ais.common.newui.akunting.NewUiTransferWorkflowService.SyncResult;
import ais.database.model.Tbmuser;

/** HTTP endpoint for the native transfer-request workspace. */
public final class NewUiTransferWorkflowController {
    private static final ThreadLocal<SimpleDateFormat> ISO=new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };
    private NewUiTransferWorkflowController(){}
    public static void handle(HttpServletRequest q,HttpServletResponse r)throws Exception{
        String action=text(q.getParameter("action"),"list");
        if("receipt".equals(action)){receipt(q,r);return;}
        r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");JSONObject j=new JSONObject();
        try{String page=page(q),privilege=privilege(action);if(!NewUiRouteGuard.isActionAuthorized(q,"akunting",page,privilege)){r.setStatus(403);fail(j,"ACTION_FORBIDDEN","Hak akses workflow transfer tidak tersedia.");write(r,j);return;}NewUiTransferWorkflowService s=new NewUiTransferWorkflowService();Tbmuser user=Common.getCurrentUser(q);
            if("list".equals(action))snapshot(j,s.load(filter(q)));
            else if("processes".equals(action))processes(j,s.processes(filter(q)));
            else if("options".equals(action))options(j,s.options());
            else if("create_process".equals(action)){csrf(q);j.put("id",s.createProcess(processDraft(q),user));}
            else if("update_process".equals(action)){csrf(q);s.updateProcess(processDraft(q),user);}
            else if("add_items".equals(action)){csrf(q);s.addItems(id(q,"processId",true),ids(q.getParameter("ids")),user);}
            else if("remove_item".equals(action)){csrf(q);s.removeItem(id(q,"processId",true),id(q,"itemId",true),user);}
            else if("approve".equals(action)){csrf(q);s.approve(id(q,"id",true),user);}
            else if("reject".equals(action)){csrf(q);s.reject(id(q,"id",true),user);}
            else if("realize".equals(action)){csrf(q);s.realize(id(q,"id",true),date(q.getParameter("date")),user);}
            else if("cancel_realization".equals(action)){csrf(q);s.cancelRealization(id(q,"id",true),user);}
            else if("set_process_active".equals(action)){csrf(q);s.setProcessActive(id(q,"id",true),bool(q,"active"),user);}
            else if("delete_process".equals(action)){csrf(q);s.deleteProcess(id(q,"id",true));}
            else if("set_disposition".equals(action)){csrf(q);s.setDisposition(id(q,"id",true),bool(q,"transfer"),bool(q,"transitory"),user);}
            else if("save_note".equals(action)){csrf(q);s.updateNote(id(q,"id",true),q.getParameter("note"),user);}
            else if("sync".equals(action)){csrf(q);sync(j,s.sync());}
            else if("payment_methods".equals(action))paymentMethods(j,s.paymentMethods(q.getParameter("q"),!"false".equals(q.getParameter("active"))));
            else if("save_payment_method".equals(action)){csrf(q);j.put("id",s.savePaymentMethod(paymentDraft(q),user));}
            else if("set_payment_method_active".equals(action)){csrf(q);s.setPaymentMethodActive(id(q,"id",true),bool(q,"active"),user);}
            else throw new IllegalArgumentException("Aksi workflow transfer tidak dikenal.");j.put("csrfHeader",ais.common.newui.NewUiCsrfUtil.LEGACY_HEADER).put("csrfToken",ais.common.newui.NewUiCsrfUtil.getTokenOkFlat(q));j.put("ok",true);
        }catch(SecurityException e){r.setStatus(403);fail(j,"FORBIDDEN",e.getMessage());}catch(IllegalArgumentException e){r.setStatus(422);fail(j,"VALIDATION_FAILED",e.getMessage());}catch(IllegalStateException e){r.setStatus(409);fail(j,"INVALID_STATE",e.getMessage());}catch(Exception e){r.setStatus(500);fail(j,"INTERNAL_ERROR","Gagal memproses workflow transfer: "+safe(e.getMessage()));try{ais.common.ErrorAuditUtil.record(e,"NewUiTransferWorkflowController");}catch(Exception ignored){}}write(r,j);
    }
    private static String privilege(String a){if("create_process".equals(a)||"save_payment_method".equals(a))return"create";if("approve".equals(a)||"realize".equals(a))return"approve";if("reject".equals(a)||"cancel_realization".equals(a))return"reject";if("delete_process".equals(a))return"delete";if("update_process".equals(a)||"add_items".equals(a)||"remove_item".equals(a)||"set_disposition".equals(a)||"save_note".equals(a)||"sync".equals(a)||"set_payment_method_active".equals(a)||"set_process_active".equals(a))return"update";return"list";}
    private static Filter filter(HttpServletRequest q){Filter f=new Filter();f.id=id(q,"filterId",false);f.workUnitId=id(q,"workUnitId",false);f.query=q.getParameter("q");f.start=date(q.getParameter("start"));f.end=date(q.getParameter("end"));f.activeOnly=!"false".equals(q.getParameter("active"));f.waiting=bool(q,"waiting");f.submitted=bool(q,"submitted");f.transitory=bool(q,"transitory");f.transferred=bool(q,"transferred");f.advance=bool(q,"advance");f.accountability=bool(q,"accountability");f.bigCash=bool(q,"bigCash");f.smallCash=bool(q,"smallCash");f.procurement=bool(q,"procurement");f.termin=bool(q,"termin");f.downPayment=bool(q,"downPayment");f.tax=bool(q,"tax");f.employee=bool(q,"employee");f.cooperative=bool(q,"cooperative");f.page=Math.max(0,integer(q.getParameter("page"),0));f.size=Math.min(100,Math.max(10,integer(q.getParameter("size"),20)));return f;}
    private static ProcessDraft processDraft(HttpServletRequest q)throws Exception{JSONObject p=new JSONObject(required(q.getParameter("payload"),"Payload proses transfer wajib diisi."));ProcessDraft d=new ProcessDraft();d.id=jsonId(p,"id");d.methodId=jsonId(p,"methodId");d.title=p.optString("title",null);d.note=p.optString("note",null);d.date=date(p.optString("date",null));JSONArray a=p.optJSONArray("itemIds");if(a!=null)for(int i=0;i<a.length();i++){Long x=id(String.valueOf(a.get(i)));if(x!=null&&!d.itemIds.contains(x))d.itemIds.add(x);}return d;}
    private static PaymentMethodDraft paymentDraft(HttpServletRequest q)throws Exception{JSONObject p=new JSONObject(required(q.getParameter("payload"),"Payload cara transfer wajib diisi."));PaymentMethodDraft d=new PaymentMethodDraft();d.id=jsonId(p,"id");d.code=p.optString("code",null);d.name=p.optString("name",null);d.description=p.optString("description",null);d.accountId=jsonId(p,"accountId");d.transitoryAccountId=jsonId(p,"transitoryAccountId");d.workUnitId=jsonId(p,"workUnitId");d.active=p.optBoolean("active",true);d.makeDefault=p.optBoolean("makeDefault",false);return d;}
    private static void snapshot(JSONObject j,Snapshot s)throws Exception{JSONArray a=new JSONArray();for(Row x:s.rows)a.put(row(x));j.put("rows",a).put("total",s.total).put("waiting",s.waiting).put("submitted",s.submitted).put("transitory",s.transitory).put("transferred",s.transferred).put("amount",s.allAmount);}
    private static JSONObject row(Row x)throws Exception{return new JSONObject().put("id",x.id).put("code",x.code).put("name",x.name).put("note",x.note).put("kind",x.kind).put("status",x.status).put("time",format(x.time)).put("amount",x.amount).put("active",x.active).put("account",x.account).put("bank",x.bank).put("accountName",x.accountName).put("accountNumber",x.accountNumber).put("processId",x.processId).put("processCode",x.processCode).put("processTitle",x.processTitle).put("realizedBy",x.realizedBy).put("workUnitId",x.workUnitId).put("workUnit",x.workUnit).put("sopId",x.sopId).put("sop",x.sop);}
    private static void processes(JSONObject j,ProcessSnapshot s)throws Exception{JSONArray a=new JSONArray();for(ProcessRow x:s.rows){JSONArray items=new JSONArray();for(Row y:x.items)items.put(row(y));a.put(new JSONObject().put("id",x.id).put("code",x.code).put("title",x.title).put("note",x.note).put("date",format(x.date)).put("amount",x.amount).put("active",x.active).put("methodId",x.methodId).put("method",x.method).put("status",x.status).put("approvedBy",x.approvedBy).put("approvedAt",format(x.approvedAt)).put("realizedBy",x.realizedBy).put("realizedUserId",x.realizedUserId).put("realizedAt",format(x.realizedAt)).put("items",items));}j.put("rows",a).put("total",s.total).put("waiting",s.waiting).put("approved",s.approved).put("realized",s.realized);}
    private static void options(JSONObject j,Options o)throws Exception{j.put("methods",options(o.methods)).put("accounts",options(o.accounts)).put("workUnits",options(o.workUnits));}
    private static JSONArray options(List<Option>v)throws Exception{JSONArray a=new JSONArray();for(Option x:v)a.put(new JSONObject().put("id",x.id).put("label",x.label));return a;}
    private static void paymentMethods(JSONObject j,List<PaymentMethodRow>v)throws Exception{JSONArray a=new JSONArray();for(PaymentMethodRow x:v)a.put(new JSONObject().put("id",x.id).put("code",x.code).put("name",x.name).put("description",x.description).put("active",x.active).put("defaultPayment",x.defaultPayment).put("accountId",x.accountId).put("account",x.account).put("transitoryAccountId",x.transitoryAccountId).put("transitoryAccount",x.transitoryAccount).put("workUnitId",x.workUnitId).put("workUnit",x.workUnit));j.put("rows",a).put("total",v.size());}
    private static void sync(JSONObject j,SyncResult x)throws Exception{j.put("total",x.total).put("success",x.success).put("skipped",x.skipped).put("failed",x.failed).put("messages",new JSONArray(x.messages));}
    private static void receipt(HttpServletRequest q,HttpServletResponse r)throws Exception{if(!NewUiRouteGuard.isActionAuthorized(q,"akunting",page(q),"list")){r.sendError(403);return;}File f=new NewUiTransferWorkflowService().receipt(id(q,"id",true));if(f==null||!f.isFile()){r.sendError(404);return;}r.setContentType("application/pdf");r.setHeader("Cache-Control","no-store");r.setHeader("Content-Disposition","inline; filename=proses-transfer.pdf");if(f.length()<=Integer.MAX_VALUE)r.setContentLength((int)f.length());FileInputStream in=new FileInputStream(f);try{OutputStream out=r.getOutputStream();byte[]b=new byte[8192];int n;while((n=in.read(b))>=0)out.write(b,0,n);out.flush();}finally{in.close();}}
    private static void csrf(HttpServletRequest q){if(!"POST".equalsIgnoreCase(q.getMethod()))throw new SecurityException("Metode HTTP tidak valid.");Object e=q.getSession().getAttribute("newUiCsrfToken");String v=q.getHeader("X-CSRF-Token");if(e==null||v==null||!String.valueOf(e).equals(v))throw new SecurityException("Token CSRF tidak valid.");}
    private static List<Long>ids(String csv){List<Long>o=new ArrayList<Long>();if(clean(csv)!=null)for(String v:csv.split(",")){Long x=id(v);if(x!=null&&!o.contains(x))o.add(x);}return o;}
    private static Long jsonId(JSONObject o,String k){Object v=o.opt(k);return v==null||JSONObject.NULL.equals(v)?null:id(String.valueOf(v));}
    private static Long id(HttpServletRequest q,String n,boolean required){Long x=id(q.getParameter(n));if(required&&x==null)throw new IllegalArgumentException(n+" wajib diisi.");return x;}
    private static Long id(String v){if(clean(v)==null)return null;try{return Long.valueOf(v.trim());}catch(Exception e){throw new IllegalArgumentException("ID tidak valid.");}}
    private static int integer(String v,int d){try{return Integer.parseInt(v);}catch(Exception e){return d;}}
    private static boolean bool(HttpServletRequest q,String n){return"true".equalsIgnoreCase(q.getParameter(n));}
    private static synchronized Date date(String v){if(clean(v)==null)return null;try{return ISO.get().parse(v.trim());}catch(Exception e){throw new IllegalArgumentException("Format tanggal harus yyyy-MM-dd.");}}
    private static synchronized String format(Date d){return d==null?null:ISO.get().format(d);}
    private static String page(HttpServletRequest q){String p=q.getParameter("page");return"proses_transfer".equals(p)||"cara_pembayaran_transfer".equals(p)?p:"daftar_pengajuan_transfer";}
    private static String required(String v,String m){if(clean(v)==null)throw new IllegalArgumentException(m);return v;}
    private static String clean(String v){return v==null||v.trim().length()==0?null:v.trim();}
    private static String text(String v,String d){return clean(v)==null?d:v.trim();}
    private static String safe(String v){return clean(v)==null?"kesalahan internal":v.replace('\n',' ').replace('\r',' ');}
    private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}
    private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}
}
