package ais.action.master.generic.v2.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import ais.action.master.generic.v2.GenericCrudCsrf;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudFacade;
import ais.action.master.generic.v2.GenericCrudJson;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.common.Common;

/** HTTP bridge native untuk operasi khusus Mahasiswa; tidak bergantung desktop ZK. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class MahasiswaNativeOperationController {
    private MahasiswaNativeOperationController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            GenericCrudRequestContext context = new GenericCrudFacade().context(request,
                    "ais.database.model.Mahasiswa", "root", "mahasiswa");
            String operation = lower(request.getParameter("nativeSubroute"));
            String action = lower(request.getParameter("action"));
            if (action.length() == 0 || "operation_meta".equals(action)) {
                Map data = new LinkedHashMap();
                data.put("operation", operation); data.put("title", title(operation));
                data.put("mode", operation.startsWith("upload_") ? "UPLOAD" : "DOWNLOAD");
                data.put("accept", operation.startsWith("upload_") ? ".xlsx" : "");
                data.put("csrf", GenericCrudCsrf.token(request));
                writeJson(response, 200, data); return;
            }
            if ("operation_download".equals(action)) {
                if (!context.isCanRead()) throw new GenericCrudException(403, "FORBIDDEN", "Hak READ diperlukan.");
                if ("download_lampiran".equals(operation) && (!context.isCanCreate() || !context.isCanUpdate()))
                    throw new GenericCrudException(403, "FORBIDDEN", "Download lampiran memerlukan CREATE dan UPDATE sesuai MahasiswaAction.");
                MahasiswaExistingBulkOperationService.Download download;
                if ("download_password".equals(operation)) download = MahasiswaExistingBulkOperationService.downloadPassword(context);
                else if ("download_rfid".equals(operation)) download = MahasiswaExistingBulkOperationService.downloadRfid(context);
                else if ("download_photo_massal".equals(operation)) {
                    File file = MahasiswaExistingBulkOperationService.downloadPhotos(context);
                    response.setContentType("application/zip");
                    response.setHeader("Content-Disposition", "attachment; filename=Foto_Mahasiswa.zip");
                    copy(file, response.getOutputStream()); return;
                } else if ("download_lampiran".equals(operation)) {
                    File file = MahasiswaExistingBulkOperationService.downloadAttachments(context);
                    response.setContentType("application/zip");
                    response.setHeader("Content-Disposition", "attachment; filename=Lampiran_Mahasiswa.zip");
                    copy(file, response.getOutputStream()); return;
                } else throw unsupported();
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(download.getFileName(), "UTF-8").replace("+", "%20"));
                response.setContentLength(download.getBytes().length);
                response.getOutputStream().write(download.getBytes()); response.getOutputStream().flush(); return;
            }
            if ("operation_upload".equals(action)) {
                if (!context.isCanUpdate()) throw new GenericCrudException(403, "FORBIDDEN", "Hak UPDATE diperlukan.");
                GenericCrudCsrf.requireMutation(request);
                List items = uploadParts(request);
                FileItem item = (FileItem) items.get(0);
                File report;
                if ("upload_photo_massal".equals(operation)) {
                    if (!context.isCanDelete()) throw new GenericCrudException(403, "FORBIDDEN", "Upload foto massal memerlukan READ, UPDATE, dan DELETE.");
                    List medias = new ArrayList();
                    for (int i = 0; i < items.size(); i++) {
                        FileItem photo = (FileItem) items.get(i);
                        byte[] bytes = photo.get();
                        medias.add(new org.zkoss.util.media.AMedia(photo.getName(), null,
                                photo.getContentType(), bytes));
                    }
                    report = MahasiswaExistingBulkOperationService.uploadPhotos(medias, context);
                } else if ("import_data".equals(operation)) {
                    if (!context.isCanCreate() || !context.isCanUpdate()) throw new GenericCrudException(403, "FORBIDDEN", "Import memerlukan CREATE dan UPDATE.");
                    report = MahasiswaExistingBulkOperationService.importEpsbed(item.getInputStream(), item.getName(), context);
                } else {
                    if (item.getName() == null || !item.getName().toLowerCase().endsWith(".xlsx"))
                        throw new GenericCrudException(400, "XLSX_REQUIRED", "Berkas wajib berformat xlsx.");
                    if ("upload_password".equals(operation)) report = MahasiswaExistingBulkOperationService.uploadPassword(item.getInputStream(), context);
                    else if ("upload_rfid".equals(operation)) report = MahasiswaExistingBulkOperationService.uploadRfid(item.getInputStream(), context);
                    else if ("upload_ukt".equals(operation)) report = MahasiswaExistingBulkOperationService.uploadUkt(item.getInputStream(), context);
                    else if ("upload_status".equals(operation)) report = MahasiswaExistingBulkOperationService.uploadStatus(item.getInputStream(), context);
                    else throw unsupported();
                }
                response.setContentType("text/plain; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=laporan_" + operation + ".txt");
                copy(report, response.getOutputStream()); return;
            }
            if ("operation_execute".equals(action)) {
                if (!context.isCanUpdate()) throw new GenericCrudException(403, "FORBIDDEN", "Hak UPDATE diperlukan.");
                GenericCrudCsrf.requireMutation(request);
                File report;
                if ("sync_status".equals(operation)) {
                    int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                    report = MahasiswaExistingBulkOperationService.synchronizeStatus(context,
                            request.getParameter("tahunAkademik"), emptyToNull(request.getParameter("semester")),
                            integer(request.getParameter("mulai"), currentYear - 4), integer(request.getParameter("sampai"), currentYear),
                            checked(request, "statusMahasiswa"), checked(request, "statusKrs"),
                            checked(request, "perkuliahan"), checked(request, "pembayaran"),
                            checked(request, "nonAktifkan"), checked(request, "reloadTemporary"));
                } else if ("feeder".equals(operation)) report = MahasiswaExistingBulkOperationService.exportFeeder(context);
                else if ("ojs".equals(operation)) report = MahasiswaExistingBulkOperationService.exportOjs(context);
                else throw unsupported();
                response.setContentType("text/plain; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=laporan_sinkronisasi_status.txt");
                copy(report, response.getOutputStream()); return;
            }
            throw new GenericCrudException(400, "ACTION_NOT_ALLOWED", "Action operasi tidak terdaftar.");
        } catch (GenericCrudException error) {
            writeError(response, error.getStatus(), error.getCode(), error.getMessage());
        } catch (Exception error) {
            Common.tampilErrorJikaAdmin(error);
            writeError(response, 500, "INTERNAL_ERROR", error.getMessage() == null ? "Terjadi kesalahan internal." : error.getMessage());
        }
    }

    private static List uploadParts(HttpServletRequest request) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) throw new GenericCrudException(400, "FILE_REQUIRED", "Berkas wajib dipilih.");
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        upload.setFileSizeMax(20L * 1024L * 1024L); upload.setSizeMax(21L * 1024L * 1024L);
        List parts = upload.parseRequest(request); List result = new ArrayList();
        for (int i = 0; i < parts.size(); i++) {
            FileItem item = (FileItem) parts.get(i);
            if (!item.isFormField() && "file".equals(item.getFieldName()) && item.getSize() > 0) result.add(item);
        }
        if (!result.isEmpty()) return result;
        throw new GenericCrudException(400, "FILE_REQUIRED", "Berkas wajib dipilih.");
    }

    private static void copy(File file, OutputStream output) throws Exception {
        FileInputStream input = new FileInputStream(file);
        try { byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read); output.flush(); }
        finally { input.close(); }
    }
    private static GenericCrudException unsupported() { return new GenericCrudException(400, "OPERATION_NOT_ALLOWED", "Operasi Mahasiswa belum terdaftar."); }
    private static String lower(String value) { return value == null ? "" : value.trim().toLowerCase(); }
    private static String title(String key) {
        if ("upload_password".equals(key)) return "Upload Password Mahasiswa";
        if ("download_password".equals(key)) return "Download Password Mahasiswa";
        if ("upload_rfid".equals(key)) return "Upload RFID Mahasiswa";
        if ("download_rfid".equals(key)) return "Download RFID Mahasiswa";
        if ("upload_photo_massal".equals(key)) return "Upload Foto Massal Mahasiswa";
        if ("download_photo_massal".equals(key)) return "Download Foto Massal Mahasiswa";
        if ("upload_ukt".equals(key)) return "Upload UKT Mahasiswa";
        if ("upload_status".equals(key)) return "Upload Status Mahasiswa";
        if ("sync_status".equals(key)) return "Sinkronisasi Status Mahasiswa";
        if ("import_data".equals(key)) return "Import EPSBED Mahasiswa";
        if ("feeder".equals(key)) return "Kirim Mahasiswa ke Neo Feeder";
        if ("ojs".equals(key)) return "Export Mahasiswa ke OJS";
        if ("download_lampiran".equals(key)) return "Download Lampiran Mahasiswa";
        return "Operasi Mahasiswa";
    }
    private static boolean checked(HttpServletRequest request, String key) { return "true".equalsIgnoreCase(request.getParameter(key)) || "on".equalsIgnoreCase(request.getParameter(key)); }
    private static int integer(String value, int fallback) { try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; } }
    private static String emptyToNull(String value) { return value == null || value.trim().length() == 0 ? null : value.trim(); }
    private static void writeJson(HttpServletResponse response, int status, Object data) throws java.io.IOException {
        response.setStatus(status); response.setContentType("application/json; charset=UTF-8");
        Map value = new LinkedHashMap(); value.put("ok", Boolean.TRUE); value.put("data", data);
        response.getWriter().write(GenericCrudJson.toJson(value)); response.getWriter().flush();
    }
    private static void writeError(HttpServletResponse response, int status, String code, String message) throws java.io.IOException {
        if (response.isCommitted()) return;
        response.setStatus(status); response.setContentType("application/json; charset=UTF-8");
        Map value = new LinkedHashMap(); value.put("ok", Boolean.FALSE); value.put("code", code); value.put("message", message);
        response.getWriter().write(GenericCrudJson.toJson(value)); response.getWriter().flush();
    }
}
