package ais.common.newui.sekolah;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.sekolah.NewUiInterviewCalonSiswaService.Option;
import ais.common.newui.sekolah.NewUiInterviewCalonSiswaService.ParticipantRow;
import ais.common.newui.sekolah.NewUiInterviewCalonSiswaService.SessionRow;
import ais.common.newui.sekolah.NewUiInterviewCalonSiswaService.Snapshot;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/** Endpoint JSON RBAC/CSRF untuk interview PSB New UI. */
public final class NewUiInterviewCalonSiswaController {
    private static final String MODULE="sekolah",PAGE="interview_calon_siswa";
    private NewUiInterviewCalonSiswaController(){}
    public static void handle(HttpServletRequest request,HttpServletResponse response)throws Exception{
        response.setContentType("application/json; charset=UTF-8");response.setHeader("Cache-Control","no-store");JSONObject json=new JSONObject();
        try{String action=text(request.getParameter("action"),"list").toLowerCase();String privilege=privilege(action);if(!NewUiRouteGuard.isActionAuthorized(request,MODULE,PAGE,privilege)){response.setStatus(403);fail(json,"ACTION_FORBIDDEN","Hak akses untuk aksi ini tidak tersedia.");write(response,json);return;}
            if(!"list".equals(action)){if(!"POST".equalsIgnoreCase(request.getMethod())){response.setStatus(405);fail(json,"METHOD_NOT_ALLOWED","Gunakan POST untuk perubahan data.");write(response,json);return;}if(!csrf(request)){response.setStatus(403);fail(json,"CSRF_INVALID","Token CSRF tidak valid.");write(response,json);return;}}
            Tbmuser user=Common.getCurrentUser(request);Sekolah school=SekolahUtil.getSekolah(request);Yayasan foundation=SekolahUtil.getYayasan(request);if((school==null||school.getId()==null)&&(foundation==null||foundation.getId()==null))throw new IllegalArgumentException("Sekolah atau yayasan aktif tidak tersedia.");
            NewUiInterviewCalonSiswaService service=new NewUiInterviewCalonSiswaService();
            if("save_session".equals(action))service.saveSession(id(request,"id",false),request.getParameter("name"),request.getParameter("academicYear"),date(request,"start",true),date(request,"end",true),integer(request,"platform",0),request.getParameter("zoom"),request.getParameter("bbb"),request.getParameter("skype"),request.getParameter("wa"),request.getParameter("other"),nullableInteger(request,"capacity"),request.getParameter("note"),id(request,"employeeId",false),id(request,"waveId",false),school,foundation,user);
            else if("delete_session".equals(action))service.deleteSession(id(request,"id",true),school,foundation);
            else if("add_participant".equals(action))service.addParticipant(id(request,"sessionId",true),request.getParameter("registration"),date(request,"start",false),date(request,"end",false),request.getParameter("note"),school,foundation,user);
            else if("remove_participant".equals(action))service.removeParticipant(id(request,"id",true),school,foundation);
            else if(!"list".equals(action)){response.setStatus(400);fail(json,"UNKNOWN_ACTION","Aksi tidak dikenali.");write(response,json);return;}
            encode(json,service.load(school,foundation,id(request,"waveId",false)));json.put("ok",true);
        }catch(IllegalArgumentException e){response.setStatus(422);fail(json,"VALIDATION_FAILED",e.getMessage());}
        catch(Exception e){response.setStatus(500);fail(json,"INTERNAL_ERROR","Gagal memproses sesi wawancara calon siswa.");try{ais.common.ErrorAuditUtil.record(e,"NewUiInterviewCalonSiswaController");}catch(Exception ignored){}}
        write(response,json);
    }
    private static String privilege(String a){if("save_session".equals(a))return text(null,"save");if("delete_session".equals(a)||"remove_participant".equals(a))return "delete";if("add_participant".equals(a))return "create";return "list";}
    private static void encode(JSONObject j,Snapshot d)throws Exception{JSONArray sessions=new JSONArray();for(int i=0;i<d.sessions.size();i++){SessionRow r=d.sessions.get(i);JSONArray participants=new JSONArray();for(int p=0;p<r.participants.size();p++){ParticipantRow x=r.participants.get(p);participants.put(new JSONObject().put("id",x.id).put("candidateId",x.candidateId).put("name",x.name).put("registration",x.registration).put("start",millis(x.start)).put("end",millis(x.end)).put("ready",x.ready).put("note",x.note));}sessions.put(new JSONObject().put("id",r.id).put("name",r.name).put("academicYear",r.academicYear).put("start",millis(r.start)).put("end",millis(r.end)).put("platform",r.platform).put("zoom",r.zoom).put("bbb",r.bbb).put("skype",r.skype).put("wa",r.wa).put("other",r.other).put("conference",r.conference).put("capacity",r.capacity).put("note",r.note).put("employeeId",r.employeeId).put("employee",r.employee).put("waveId",r.waveId).put("wave",r.wave).put("participants",participants).put("participantCount",participants.length()));}j.put("sessions",sessions).put("employees",options(d.employees)).put("waves",options(d.waves));}
    private static JSONArray options(java.util.List<Option>d)throws Exception{JSONArray a=new JSONArray();for(int i=0;i<d.size();i++)a.put(new JSONObject().put("id",d.get(i).id).put("label",d.get(i).label));return a;}
    private static Object millis(Date d){return d==null?JSONObject.NULL:Long.valueOf(d.getTime());}private static boolean csrf(HttpServletRequest r){Object e=r.getSession().getAttribute("newUiCsrfToken");String s=r.getHeader("X-CSRF-Token");return e!=null&&s!=null&&String.valueOf(e).equals(s);}
    private static Long id(HttpServletRequest r,String n,boolean required){String v=r.getParameter(n);if(v==null||v.trim().length()==0){if(required)throw new IllegalArgumentException(n+" wajib diisi.");return null;}try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static int integer(HttpServletRequest r,String n,int fallback){Integer v=nullableInteger(r,n);return v==null?fallback:v.intValue();}private static Integer nullableInteger(HttpServletRequest r,String n){String v=r.getParameter(n);if(v==null||v.trim().length()==0)return null;try{return Integer.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static Date date(HttpServletRequest r,String n,boolean required){String v=r.getParameter(n);if(v==null||v.trim().length()==0){if(required)throw new IllegalArgumentException(n+" wajib diisi.");return null;}try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");f.setLenient(false);return f.parse(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static String text(String v,String d){return v==null||v.trim().length()==0?d:v.trim();}private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}private static void write(HttpServletResponse r,JSONObject j)throws IOException{r.getWriter().print(j.toString());}
}
