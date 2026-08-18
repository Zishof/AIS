<%@page import="java.util.List"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
try {
    // Definisi URL kembali ke halaman hak akses
    String urlHakAkses = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common&s=hak_akses";

    // 1. Validasi Input Role ID dari form
    String roleIdStr = request.getParameter("role_id");
    if (roleIdStr == null || roleIdStr.trim().isEmpty()) {
        out.print("<script>window.location.href='" + urlHakAkses + "&error=Hak%20akses%20tidak%20valid';</script>");
        return;
    }
   

    // 2. Ambil data pengguna dan daftar hak akses dari session
    Tbmuser tbmuser = Common.getCurrentUser(request);
	List<Tbmrole> tbmroles = tbmuser == null ? null : tbmuser.ambilRoles();

    if (tbmuser == null || tbmroles == null) {
        out.print("<script>window.location.href='" + Common.ROOT + "/login2.jsp?login_error=Sesi%20Anda%20telah%20habis,%20silakan%20masuk%20kembali';</script>");
        return;
    }

    // 3. Cari objek Tbmrole yang dipilih pengguna
    Tbmrole selectedRole = null;
    for (Tbmrole role : tbmroles) {
        if (role.getRoleId().equals(roleIdStr)) {
            selectedRole = role;
            break;
        }
    }

    if (selectedRole == null) {
        out.print("<script>window.location.href='" + urlHakAkses + "&error=Hak%20akses%20tidak%20ditemukan%20dalam%20daftar%20Anda';</script>");
        return;
    }

    // 4. Implementasi Logika Inti
    Tbmrole curentRole = tbmuser.hakAkses();

    // Set penanda bahwa user sudah memilih hak akses
    session.setAttribute("udah_tanya", true);

    // Simpan hak akses yang dipilih ke map statis aplikasi
    Tbmuser.getUserRoleYgDipakai.put(tbmuser.getUserId(), selectedRole);

    // Bersihkan cache menu lama dari session
    session.removeAttribute("current_menus");

    // 5. Pengecekan Perubahan Institusi (Sekolah/Fakultas)
    Long sek1 = selectedRole.getSekolah() == null ? -1L : selectedRole.getSekolah().getId();
    Long sek2 = (curentRole == null || curentRole.getSekolah() == null) ? -1L : curentRole.getSekolah().getId();
    
    Long sek3 = (selectedRole.getFakultas() == null || selectedRole.getFakultas().getPerguruanTinggi() == null) ? -1L 
              : selectedRole.getFakultas().getPerguruanTinggi().getId();
    Long sek4 = (curentRole == null || curentRole.getFakultas() == null || curentRole.getFakultas().getPerguruanTinggi() == null) ? -1L 
              : curentRole.getFakultas().getPerguruanTinggi().getId();

    // 6. Pengalihan Halaman (Redirect) ke Main menggunakan Javascript
    String redirectUrl = Common.ROOT + "/main?d=" + Common.randLong();
    out.print("<script>window.location.href='" + redirectUrl + "';</script>");

} catch (NumberFormatException nfe) {
    System.err.println("Kesalahan format role_id: " + nfe.getMessage());
    out.print("<script>window.location.href='" + Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common&s=hak_akses&error=Format%20hak%20akses%20tidak%20valid';</script>");
} catch (Exception e) {
    System.err.println("Kesalahan sistem saat memproses pilihan hak akses: " + e.getMessage());
    out.print("<script>window.location.href='" + Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common&s=hak_akses&error=Terjadi%20kesalahan%20sistem,%20silakan%20coba%20lagi';</script>");
}
%>