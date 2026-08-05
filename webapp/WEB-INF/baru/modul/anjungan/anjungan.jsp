<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// ============================================================================================
// PENGENDALI UTAMA (MAIN CONTROLLER) ANJUNGAN LAYANAN MANDIRI
// Bertugas mengecek sesi pengguna dan mengarahkan ke halaman yang tepat.
// ============================================================================================
try {
    // Mengambil profil pengguna yang sedang aktif (Login)
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    // KONDISI 1: JIKA PENGGUNA BELUM LOGIN
    if (tbmuser == null || tbmuser.getUserId() == null) {
%>
        <!-- Arahkan ke Halaman Pendaratan (Landing Page) Anjungan -->
        <jsp:include page="/WEB-INF/baru/modul/anjungan/_belum_login_anjungan.jsp"></jsp:include>
<%
    } 
    // KONDISI 2: JIKA PENGGUNA SUDAH LOGIN
    else {
        // Mengambil data relasional pengguna dari basis data
        Siswa siswa = tbmuser.getSiswa();
        Mahasiswa mahasiswa = tbmuser.getMahasiswa();
        Dosen dosen = tbmuser.ambilDosen();
        Guru guru = tbmuser.ambilGuru();
        Fakultas fakultas = tbmuser.ambilFakultas();
        Jurusan jurusan = tbmuser.ambilJurusan();
        Sekolah sekolah = null;
        
        if (tbmuser.ambilSekolah() != null) {
            sekolah = tbmuser.ambilSekolah();
        }
        
        Yayasan yayasan = tbmuser.ambilYayasan();
        
        // Menyimpan objek ke dalam request agar dapat diakses oleh halaman dasbor
        request.setAttribute("tbmuser", tbmuser);
%>
        <!-- Arahkan ke Halaman Dasbor Anjungan Utama -->
        <jsp:include page="/WEB-INF/baru/modul/anjungan/_telah_login_anjungan.jsp"></jsp:include>
<%
    }
} catch (Exception e) {
    // Menangkap dan mencetak galat jika terjadi kesalahan sistem
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/anjungan/anjungan.jsp:54");
} finally {
    // Memastikan sesi koneksi pangkalan data ditutup dengan aman untuk mencegah kebocoran memori
    ais.database.hibernate.HibernateUtil.closeSession();
}
%>