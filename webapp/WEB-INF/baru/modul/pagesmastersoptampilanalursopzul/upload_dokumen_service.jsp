<%@page session="false"%>
<%--
  Endpoint UPLOAD dokumen lampiran disposisi SOP — versi native JSP (multipart).
  Pola multipart proven (lihat modul/karir/_karir_service.jsp) + simpan via jalur BLOB
  FileFotoLain.createFileFotoLain (sama dengan upload dokumen alur ZK; dilayani servlet
  /AmbilLampiranLain). Hanya admin (Common.getApakahAdmin) — sesuai aturan disposisi.

  Param (multipart/form-data):
    - ref   : id acuan (langkah alur = disposisiAlurSop.id; langkah pengaju = disposisiSop.id)
    - jenis : "ais.database.model.sop.DokumenAlurSop_alur_<id>" (alur) / "..._<id>" (start)
    - file  : berkas yang diunggah
--%>
<%@page import="java.util.*"%>
<%@page import="java.io.*"%>
<%@page import="org.json.*"%>
<%@page import="org.apache.commons.fileupload.FileItem"%>
<%@page import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page import="org.apache.commons.fileupload.servlet.ServletFileUpload"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.file.FileFotoLain"%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
Session session = null;
try {
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status","01"); result.put("message","Sesi Anda telah berakhir, silakan masuk kembali.");
        out.print(result.toString()); return;
    }
    // ATURAN: upload/ganti dokumen alur SOP hanya untuk admin.
    if (!Common.getApakahAdmin()) {
        result.put("status","03"); result.put("message","Hanya admin yang dapat mengunggah/mengganti dokumen.");
        out.print(result.toString()); return;
    }
    if (!ServletFileUpload.isMultipartContent(request)) {
        result.put("status","02"); result.put("message","Permintaan bukan multipart.");
        out.print(result.toString()); return;
    }

    Long ref = null; String jenis = null; FileItem fileItem = null;
    DiskFileItemFactory factory = new DiskFileItemFactory();
    File tmpDir = new File(Common.REAL_PATH + "/tmp/disposisi_upload_tmp");
    try { tmpDir.mkdirs(); factory.setRepository(tmpDir); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:49");}
    factory.setSizeThreshold(1024 * 1024);
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setHeaderEncoding("UTF-8");
    List items = upload.parseRequest(request);
    for (int i = 0; i < items.size(); i++) {
        FileItem item = (FileItem) items.get(i);
        if (item == null) continue;
        if (item.isFormField()) {
            String fn = item.getFieldName();
            String val; try { val = item.getString("UTF-8"); } catch (Exception e) { val = item.getString(); }
            if ("ref".equals(fn)) { try { ref = Long.valueOf(val.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:60");} }
            else if ("jenis".equals(fn)) { jenis = val; }
        } else if (item.getSize() > 0) {
            fileItem = item;
        }
    }

    if (ref == null || jenis == null || jenis.trim().isEmpty()) {
        result.put("status","02"); result.put("message","Parameter dokumen tidak lengkap."); out.print(result.toString()); return;
    }
    if (fileItem == null) {
        result.put("status","02"); result.put("message","Berkas belum dipilih."); out.print(result.toString()); return;
    }

    // Tulis ke file temp (createFileFotoLain membaca dari File).
    String original = fileItem.getName();
    if (original != null) {
        int sl = Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\'));
        if (sl >= 0) original = original.substring(sl + 1);
    }
    if (original == null || original.trim().isEmpty()) original = "dokumen";
    File tempFile = new File(tmpDir, System.currentTimeMillis() + "_" + original);
    InputStream in = null; FileOutputStream fos = null;
    try {
        in = fileItem.getInputStream(); fos = new FileOutputStream(tempFile);
        byte[] buf = new byte[8192]; int len;
        while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
    } finally {
        try { if (fos != null) fos.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:88");}
        try { if (in != null) in.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:89");}
        try { fileItem.delete(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:90");}
    }

    // Simpan via jalur BLOB proven (sama dengan upload dokumen alur ZK):
    // soft-delete lampiran lama (ref+jenis) + simpan blob baru + audit.
    session = HibernateUtil.getSessionFactory().openSession();
    FileFotoLain.createFileFotoLain(tbmuser, session, LampiranLain.class, false, ref, jenis, null, tempFile, original);

    try { tempFile.delete(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:98");}
    result.put("status","00"); result.put("message","Dokumen berhasil diunggah.");
} catch (Throwable e) {
    try { Common.tampilErrorJikaAdmin(new Exception("Upload dokumen disposisi gagal: " + e.getMessage(), e)); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:101");}
    try { result.put("status","99"); result.put("message","Gagal mengunggah: " + (e.getMessage() == null ? "" : e.getMessage())); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:102");}
} finally {
    if (session != null) { try { session.clear(); session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/upload_dokumen_service.jsp:104");} }
}
out.print(result.toString());
%>
