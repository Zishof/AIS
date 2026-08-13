package ais.common.newui.kursus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.kursus.NewUiCourseComponentService.History;
import ais.common.newui.kursus.NewUiCourseComponentService.Input;
import ais.common.newui.kursus.NewUiCourseComponentService.Meeting;
import ais.common.newui.kursus.NewUiCourseComponentService.Option;
import ais.common.newui.kursus.NewUiCourseComponentService.Row;
import ais.common.newui.kursus.NewUiCourseComponentService.Snapshot;
import ais.database.model.Tbmuser;
import ais.database.model.kursus.KomponenProdukKursus;
import ais.database.model.rab.SatuanKerja;

/** Controller JSON/multipart bersama untuk seluruh turunan KomponenDataProdukKursusAction. */
@SuppressWarnings("unchecked")
public final class NewUiCourseComponentController {
    private NewUiCourseComponentController() {}

    public static void handle(HttpServletRequest req, HttpServletResponse res, String page, String fixedType) throws Exception {
        res.setContentType("application/json; charset=UTF-8");
        res.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            String action = text(req.getParameter("action"), "list");
            String privilege = "list";
            if ("save".equals(action) || "import_csv".equals(action) || "copy".equals(action))
                privilege = "save".equals(action) && clean(req.getParameter("id")) != null ? "update" : "create";
            else if ("active".equals(action) || "generate_meetings".equals(action)) privilege = "update";
            else if ("delete".equals(action)) privilege = "delete";
            if (!NewUiRouteGuard.isActionAuthorized(req, "kursus", page, privilege)) {
                res.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(res, json); return;
            }
            SaveRequest pending = "save".equals(action) ? parseSave(req) : null;
            String type = fixedType == null ? validType(pending == null ? req.getParameter("type") : pending.type) : fixedType;
            Set<SatuanKerja> units = SekolahUtil.ambilSatuanKerjas();
            Tbmuser user = Common.getCurrentUser(req);
            NewUiCourseComponentService service = new NewUiCourseComponentService();
            if ("list".equals(action)) encode(json, service.load(type, req.getParameter("q"), id(req.getParameter("unitId")), !"false".equals(req.getParameter("active")), integer(req.getParameter("page"), 0), Math.min(100, Math.max(10, integer(req.getParameter("size"), 20))), units));
            else if ("get".equals(action)) json.put("row", row(service.get(requiredId(req.getParameter("id")), type, units)));
            else if ("meetings".equals(action)) json.put("meetings", meetings(service.meetings(requiredId(req.getParameter("id")), type, units, false)));
            else if ("history".equals(action)) json.put("history", history(service.history(requiredId(req.getParameter("id")), type, units)));
            else {
                postCsrf(req);
                if ("save".equals(action)) {
                    SaveRequest value = pending;
                    if (value.input.id == null && fileType(type) && value.file == null && value.link == null) throw new IllegalArgumentException("File atau link " + type + " wajib diisi.");
                    Long saved = service.save(value.input, type, units, user);
                    InputStream in = value.file == null ? null : value.file.getInputStream();
                    try { service.attach(saved, type, value.file == null ? null : value.file.getName(), in, value.link, user); }
                    finally { if (in != null) try { in.close(); } catch (Exception ignored) {} }
                    json.put("id", saved);
                } else if ("copy".equals(action)) json.put("id", service.copy(requiredId(req.getParameter("id")), type, units, user));
                else if ("import_csv".equals(action)) {
                    List<CsvRow> rows = parseCsv(req, type); int imported = 0;
                    for (CsvRow value : rows) {
                        String rowType = fixedType == null ? validType(value.type) : fixedType;
                        if (fileType(rowType) && clean(value.link) == null) throw new IllegalArgumentException("Baris " + (imported + 2) + " memerlukan kolom link HTTP/HTTPS.");
                        Long saved = service.save(value.input, rowType, units, user);
                        if (fileType(rowType)) service.attach(saved, rowType, null, null, value.link, user);
                        imported++;
                    }
                    json.put("imported", imported);
                } else if ("active".equals(action)) service.setActive(requiredId(req.getParameter("id")), type, bool(req.getParameter("value")), units, user);
                else if ("delete".equals(action)) service.remove(requiredId(req.getParameter("id")), type, units, user);
                else if ("generate_meetings".equals(action)) json.put("meetings", meetings(service.meetings(requiredId(req.getParameter("id")), type, units, true)));
                else throw new IllegalArgumentException("Aksi tidak dikenal.");
            }
            json.put("ok", true);
        } catch (SecurityException e) { res.setStatus(403); fail(json, "CSRF_INVALID", e.getMessage()); }
        catch (IllegalArgumentException e) { res.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) { res.setStatus(500); fail(json, "INTERNAL_ERROR", "Gagal memproses komponen produk kursus."); try { ais.common.ErrorAuditUtil.record(e, "NewUiCourseComponentController"); } catch (Exception ignored) {} }
        write(res, json);
    }

    private static SaveRequest parseSave(HttpServletRequest req) throws Exception {
        Map<String,String> values = new HashMap<String,String>(); FileItem file = null;
        if (ServletFileUpload.isMultipartContent(req)) {
            ServletFileUpload parser = new ServletFileUpload(new DiskFileItemFactory()); parser.setFileSizeMax(50L * 1024L * 1024L);
            for (FileItem value : (List<FileItem>) parser.parseRequest(req)) if (value.isFormField()) values.put(value.getFieldName(), value.getString("UTF-8")); else if (value.getSize() > 0) file = value;
        } else {
            String[] names = {"type","id","code","name","note","active","defaultPrice","price","meetings","start","duration","unitId","itemId","examId","tutor1","tutor2","tutor3","tutor4","tutor5","link"};
            for (String name : names) values.put(name, req.getParameter(name));
        }
        SaveRequest out = new SaveRequest(); out.input = input(values); out.file = file; out.link = clean(values.get("link")); out.type = clean(values.get("type")); return out;
    }

    private static List<CsvRow> parseCsv(HttpServletRequest req, String fallbackType) throws Exception {
        if (!ServletFileUpload.isMultipartContent(req)) throw new IllegalArgumentException("Berkas CSV wajib dipilih.");
        FileItem file = null; ServletFileUpload parser = new ServletFileUpload(new DiskFileItemFactory()); parser.setFileSizeMax(10L * 1024L * 1024L);
        for (FileItem value : (List<FileItem>) parser.parseRequest(req)) if (!value.isFormField() && value.getSize() > 0) { file = value; break; }
        if (file == null) throw new IllegalArgumentException("Berkas CSV kosong.");
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
        try {
            String head = reader.readLine(); if (head == null) throw new IllegalArgumentException("Berkas CSV kosong.");
            List<String> headers = csv(head); List<CsvRow> out = new ArrayList<CsvRow>(); String line;
            while ((line = reader.readLine()) != null) {
                if (clean(line) == null) continue; List<String> cells = csv(line); Map<String,String> values = new HashMap<String,String>();
                for (int i = 0; i < headers.size(); i++) values.put(headers.get(i).replace("\ufeff", "").trim().toLowerCase(), i < cells.size() ? cells.get(i) : "");
                CsvRow row = new CsvRow(); row.input = input(values); row.type = text(values.get("type"), fallbackType); row.link = clean(values.get("link")); out.add(row);
            }
            if (out.isEmpty()) throw new IllegalArgumentException("CSV tidak mempunyai baris data."); return out;
        } finally { reader.close(); }
    }

    private static Input input(Map<String,String> values) {
        Input out = new Input(); out.id = id(values.get("id")); out.code = values.get("code"); out.name = values.get("name"); out.note = values.get("note"); out.active = !"false".equalsIgnoreCase(values.get("active")); out.defaultPrice = !"false".equalsIgnoreCase(value(values,"defaultPrice","defaultprice")); out.price = dbl(values.get("price")); out.meetings = integer(values.get("meetings"), 10); out.start = date(values.get("start"), "yyyy-MM-dd"); out.duration = date(values.get("duration"), "HH:mm"); out.unitId = id(value(values,"unitId","unitid")); out.itemId = id(value(values,"itemId","itemid")); out.examId = id(value(values,"examId","examid")); out.tutor1 = id(values.get("tutor1")); out.tutor2 = id(values.get("tutor2")); out.tutor3 = id(values.get("tutor3")); out.tutor4 = id(values.get("tutor4")); out.tutor5 = id(values.get("tutor5")); return out;
    }

    private static List<String> csv(String line) { List<String> out = new ArrayList<String>(); StringBuilder value = new StringBuilder(); boolean quoted = false; for (int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}else if(c==','&&!quoted){out.add(value.toString());value.setLength(0);}else value.append(c);}out.add(value.toString());return out; }
    private static void encode(JSONObject json, Snapshot data) throws Exception { JSONArray rows=new JSONArray();for(Row value:data.rows)rows.put(row(value));json.put("rows",rows).put("total",data.total).put("units",options(data.units)).put("employees",options(data.employees)).put("items",options(data.items)).put("exams",options(data.exams)); }
    private static JSONObject row(Row value) throws Exception { return new JSONObject().put("id",value.id).put("code",value.code).put("name",value.name).put("note",value.note).put("type",value.type).put("active",value.active).put("defaultPrice",value.defaultPrice).put("price",value.price).put("meetings",value.meetings).put("start",value.start==null?JSONObject.NULL:value.start.getTime()).put("duration",value.duration==null?JSONObject.NULL:value.duration.getTime()).put("unitId",value.unitId).put("unit",value.unit).put("itemId",value.itemId).put("item",value.item).put("examId",value.examId).put("exam",value.exam).put("tutorIds",new JSONArray(value.tutorIds)).put("tutors",new JSONArray(value.tutors)).put("hasAttachment",value.hasAttachment).put("attachmentName",value.attachmentName).put("attachmentUrl",value.attachmentUrl); }
    private static JSONArray options(List<Option> values) throws Exception { JSONArray out=new JSONArray();for(Option value:values)out.put(new JSONObject().put("id",value.id).put("label",value.label));return out; }
    private static JSONArray meetings(List<Meeting> values) throws Exception { JSONArray out=new JSONArray();for(Meeting value:values)out.put(new JSONObject().put("id",value.id).put("number",value.number).put("date",value.date==null?JSONObject.NULL:value.date.getTime()).put("start",value.start).put("end",value.end).put("topic",value.topic).put("status",value.status));return out; }
    private static JSONArray history(List<History> values) throws Exception { JSONArray out=new JSONArray();for(History value:values)out.put(new JSONObject().put("revision",value.revision).put("type",value.type).put("timestamp",value.timestamp).put("by",value.by).put("code",value.code).put("name",value.name).put("componentType",value.componentType).put("price",value.price).put("active",value.active).put("note",value.note));return out; }
    private static boolean fileType(String type){return KomponenProdukKursus.VIDEO.equals(type)||KomponenProdukKursus.EBOOK.equals(type);}
    private static String validType(String value){if(clean(value)==null)return null;for(String type:KomponenProdukKursus.s)if(type.equals(value))return type;throw new IllegalArgumentException("Jenis komponen tidak valid.");}
    private static void postCsrf(HttpServletRequest req){if(!"POST".equalsIgnoreCase(req.getMethod()))throw new SecurityException("Metode HTTP tidak valid.");Object expected=req.getSession().getAttribute("newUiCsrfToken");String value=req.getHeader("X-CSRF-Token");if(expected==null||value==null||!String.valueOf(expected).equals(value))throw new SecurityException("Token CSRF tidak valid.");}
    private static Long requiredId(String value){Long id=id(value);if(id==null)throw new IllegalArgumentException("ID wajib diisi.");return id;}
    private static Long id(String value){if(clean(value)==null)return null;try{return Long.valueOf(value);}catch(Exception e){throw new IllegalArgumentException("ID tidak valid.");}}
    private static boolean bool(String value){return "true".equalsIgnoreCase(value);}
    private static int integer(String value,int fallback){try{return Integer.parseInt(value);}catch(Exception e){return fallback;}}
    private static double dbl(String value){try{return Double.parseDouble(value);}catch(Exception e){return 0;}}
    private static java.util.Date date(String value,String format){if(clean(value)==null)return null;try{return new SimpleDateFormat(format).parse(value);}catch(Exception e){throw new IllegalArgumentException("Tanggal/waktu tidak valid.");}}
    private static String value(Map<String,String> values,String mixed,String lower){String value=values.get(mixed);return value==null?values.get(lower):value;}
    private static String clean(String value){return value==null||value.trim().length()==0?null:value.trim();}
    private static String text(String value,String fallback){return clean(value)==null?fallback:value.trim();}
    private static void fail(JSONObject json,String code,String message)throws Exception{json.put("ok",false).put("code",code).put("message",message);}
    private static void write(HttpServletResponse res,JSONObject json)throws Exception{res.getWriter().write(json.toString());}
    private static final class SaveRequest{Input input;FileItem file;String link,type;}
    private static final class CsvRow{Input input;String link,type;}
}
