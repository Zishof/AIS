<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================
    // 1. BLOK PENANGANAN LOGOUT
    // =========================================================================
    String actionReq = request.getParameter("action");
    if ("logout".equals(actionReq)) {
        out.clearBuffer();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Menghapus sesi login alumni
        request.getSession().removeAttribute("alumni_logged_in");
        
        // Mengirimkan respons JSON sukses
        out.print("{\"status\":\"sukses\", \"pesan\":\"" + Common.getBahasaConfig("Logout berhasil dilakukan.") + "\"}");
        out.flush();
        return;
    }

    // =========================================================================
    // 2. BLOK PENANGANAN LOGIN (AJAX) - CASE INSENSITIVE
    // =========================================================================
    if ("proses_login_alumnii".equals(actionReq) || "proses_login_alumni".equals(actionReq)) {
        out.clearBuffer();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String identitas = request.getParameter("identitas");
        String tglStr = request.getParameter("tgl");
        String blnStr = request.getParameter("bln");
        String thnStr = request.getParameter("thn");
        
        // Validasi Input Kosong
        if (identitas == null || identitas.trim().isEmpty()) {
             out.print("{\"status\":\"gagal\", \"pesan\":\"" + Common.getBahasaConfig("Nama Lengkap / Email / NIM harus diisi") + "\"}");
             out.flush(); return;
        }
        if (thnStr == null || thnStr.trim().isEmpty() || blnStr == null || blnStr.trim().isEmpty() || tglStr == null || tglStr.trim().isEmpty()) {
             out.print("{\"status\":\"gagal\", \"pesan\":\"" + Common.getBahasaConfig("Tanggal, Bulan, dan Tahun lahir harus diisi lengkap") + "\"}");
             out.flush(); return;
        }
        
        Session sess = HibernateUtil.openSession();
        try {
            int thn = Integer.parseInt(thnStr);
            int bln = Integer.parseInt(blnStr) - 1; 
            int tgl = Integer.parseInt(tglStr);
            
            java.util.Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
            calendar.set(java.util.Calendar.YEAR, thn);
            calendar.set(java.util.Calendar.MONTH, bln);
            calendar.set(java.util.Calendar.DATE, tgl);
            
            // Kueri Database dengan .ignoreCase() untuk mengabaikan Case Sensitive
            Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
                sess.createCriteria(Mahasiswa.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("statusKeluar.id", 1L))
                    .add(Restrictions.eq("tanggallahir", calendar.getTime()))
                    .setMaxResults(1)
                    .add(Restrictions.or(
                        // Menggunakan ignoreCase() untuk mengabaikan huruf besar/kecil
                        Restrictions.eq("nim", identitas.trim()).ignoreCase(),
                        Restrictions.or(
                            Restrictions.eq("nama", identitas.trim()).ignoreCase(),
                            Restrictions.eq("email", identitas.trim()).ignoreCase()
                        )
                    )),
                Mahasiswa.class
            );

            // Evaluasi Hasil Autentikasi
            if (mahasiswa == null) {
                out.print("{\"status\":\"gagal\", \"pesan\":\"" + Common.getBahasaConfig("Mahasiswa dengan identitas tersebut tidak ditemukan, atau Tanggal Lahir yang Anda masukkan salah!") + "\"}");
            } else {
                // Set objek mahasiswa ke dalam sesi
                request.getSession().setAttribute("alumni_logged_in", mahasiswa);
                out.print("{\"status\":\"sukses\", \"pesan\":\"" + Common.getBahasaConfig("Autentikasi berhasil dilakukan.") + "\"}");
            }
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/alumni/tracer_study.jsp:90");
            out.print("{\"status\":\"gagal\", \"pesan\":\"" + Common.getBahasaConfig("Terjadi kesalahan pada sistem, harap periksa kembali data Anda.") + "\"}");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/tracer_study.jsp:93");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/tracer_study.jsp:94");}
            HibernateUtil.closeSessionQuietly(sess);
        }
        
        out.flush();
        return; 
    }

    // =========================================================================
    // 3. BLOK ROUTING HALAMAN (BERDASARKAN SESI)
    // =========================================================================
    Mahasiswa mhsLoggedIn = (Mahasiswa) request.getSession().getAttribute("alumni_logged_in");
    
    if (mhsLoggedIn != null) {
%>
        <jsp:include page="/WEB-INF/baru/modul/alumni/sukses_login_alumni.jsp"></jsp:include>
<%
    } else {
%>
        <jsp:include page="/WEB-INF/baru/modul/alumni/belum_login_alumni.jsp"></jsp:include>
<%
    }
%>