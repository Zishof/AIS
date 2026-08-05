<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.PertemuanPunyaDiskusi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.Date"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
// 1. Inisialisasi JSON Response dan Pengecekan Keamanan Sesi
JSONObject responseJson = new JSONObject();
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) {
    responseJson.put("status", "error");
    responseJson.put("message", Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali."));
    out.print(responseJson.toString());
    out.flush();
    return;
}

// 2. Mengambil Parameter dari AJAX Request
String idPertemuanParam = request.getParameter("idPertemuan");
String idParentParam = request.getParameter("idParent"); // Jika ini adalah balasan/tanggapan
String isiKomentar = request.getParameter("isiKomentar");

// Validasi Parameter Wajib
if (idPertemuanParam == null || idPertemuanParam.trim().isEmpty() || isiKomentar == null || isiKomentar.trim().isEmpty()) {
    responseJson.put("status", "error");
    responseJson.put("message", Common.getBahasaConfig("Data pertemuan atau isi komentar tidak boleh kosong."));
    out.print(responseJson.toString());
    out.flush();
    return;
}

Session mySession = null;
Transaction tx = null;

try {
    // 3. Membuka Sesi Hibernate dan Memulai Transaksi
    mySession = HibernateUtil.getSessionFactory().openSession();
    tx = mySession.beginTransaction();

    Long idPertemuan = Long.parseLong(idPertemuanParam.trim());
    Pertemuan pertemuan = (Pertemuan) mySession.get(Pertemuan.class, idPertemuan);

    if (pertemuan == null) {
        responseJson.put("status", "error");
        responseJson.put("message", Common.getBahasaConfig("Data pertemuan tidak ditemukan di dalam sistem."));
        out.print(responseJson.toString());
        return;
    }

    // Cek apakah ini balasan dari komentar lain (Parent)
    PertemuanPunyaDiskusi parent = null;
    if (idParentParam != null && !idParentParam.trim().isEmpty()) {
        Long idParent = Long.parseLong(idParentParam.trim());
        parent = (PertemuanPunyaDiskusi) mySession.get(PertemuanPunyaDiskusi.class, idParent);
    }

    // 4. Membentuk Objek Diskusi Baru
    PertemuanPunyaDiskusi diskusiBaru = new PertemuanPunyaDiskusi();
    diskusiBaru.setPertemuan(pertemuan);
    diskusiBaru.setParent(parent);
    
    // Perbaikan: Menyimpan isi teks komentar ke dalam field "isi"
    diskusiBaru.setIsi(isiKomentar.trim());
    
    // Perbaikan: Menyimpan waktu saat ini ke dalam field "tanggal_dirubah"
    diskusiBaru.setTanggal_dirubah(new Date());

    // 5. Menyisipkan Pemilik (Otoritas) Komentar Berdasarkan Peran Sivitas Aktif
    if (tbmuser.getMahasiswa() != null) diskusiBaru.setMahasiswa(tbmuser.getMahasiswa());
    if (tbmuser.ambilDosen() != null) diskusiBaru.setDosen(tbmuser.ambilDosen());
    if (tbmuser.getSiswa() != null) diskusiBaru.setSiswa(tbmuser.getSiswa());
    if (tbmuser.ambilGuru() != null) diskusiBaru.setGuru(tbmuser.ambilGuru());
    if (tbmuser.getCalonSiswa() != null) diskusiBaru.setCalonSiswa(tbmuser.getCalonSiswa());
    if (tbmuser.getBiodataCalonMahasiswa() != null) diskusiBaru.setBiodataCalonMahasiswa(tbmuser.getBiodataCalonMahasiswa());
    
    diskusiBaru.setTbmuser(tbmuser);

    // 6. Mengeksekusi Penyimpanan ke Database
    mySession.save(diskusiBaru);
    tx.commit(); // Permanenkan transaksi

    try { pertemuan.reInitPertemuanPunyaDiskusi(mySession); } catch (Exception reInitEx) { ais.common.ErrorAuditUtil.record(reInitEx, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_simpan_diskusi.jsp:89"); /* non-critical cache refresh, transaction already committed */ }

    // 7. Berikan Umpan Balik Sukses ke UI/UX
    responseJson.put("status", "success");
    responseJson.put("message", Common.getBahasaConfig("Komentar atau diskusi Anda telah berhasil dikirimkan."));

} catch (Exception e) {
    if (tx != null) tx.rollback(); // Kembalikan state DB jika terjadi error
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_simpan_diskusi.jsp:97");
    responseJson.put("status", "error");
    responseJson.put("message", Common.getBahasaConfig("Terjadi kesalahan sistem saat menyimpan data diskusi."));
} finally {
    // 8. Selalu Tutup Sesi Hibernate di Blok Finally untuk mencegah Memory Leak
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

// Kembalikan JSON String ke Browser Klien
out.print(responseJson.toString());
out.flush();
%>