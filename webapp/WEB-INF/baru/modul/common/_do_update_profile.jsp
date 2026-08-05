<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.model.BiodataMahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.hibernate.Session" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.hibernate.HibernateUtil" %>
<%@ page import="ais.database.model.GeneralValueObject" %>
<%@ page import="ais.database.model.Mahasiswa" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.database.model.sekolah.Siswa" %>
<%@ page import="ais.database.model.sekolah.Guru" %>
<%@ page import="ais.database.model.Dosen" %>
<%@ page import="ais.database.model.Pegawai" %><%
    String status = "error";
    String message = "";
    Session ses = null;
    Transaction tx = null;
    try {
        String emailBaru = request.getParameter("email");
        String noHpBaru = request.getParameter("noHp");

        if (emailBaru != null) emailBaru = emailBaru.trim();
        if (noHpBaru != null) noHpBaru = noHpBaru.trim();

        Tbmuser users = Common.getCurrentUser(request);
        
        if (users == null || users.getUserId() == null) {
            status = "error";
            message = Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali.");
        } else if (emailBaru == null || emailBaru.isEmpty() || noHpBaru == null || noHpBaru.isEmpty()) {
            status = "error";
            message = Common.getBahasaConfig("Alamat Email dan Nomor Telepon tidak boleh kosong.");
        } else {
            ses = HibernateUtil.openSession();
            
            Mahasiswa mahasiswa = users.getMahasiswa();
            Siswa siswa = users.getSiswa();
            Guru guru = users.ambilGuru();
            Dosen dosen = users.ambilDosen();
            Pegawai pegawai = users.ambilPegawai();
            AnggotaKoperasi anggotaKoperasi = users.getAnggotaKoperasi();
            
            ses = HibernateUtil.openSession();
            tx = ses.getTransaction();
            tx.begin();
            
            
            ses.refresh(users);
	       	 if(anggotaKoperasi != null){
	            	ses.refresh(anggotaKoperasi);
	            	//anggotaKoperasi.setTelp(noHpBaru);
	            	anggotaKoperasi.setHp(noHpBaru);
	            	anggotaKoperasi.setEmail(emailBaru);
	               ses.update(anggotaKoperasi);
	               users.setAnggotaKoperasi(anggotaKoperasi);
	            }

            // Memperbarui entitas yang relevan berdasarkan peran pengguna
            if (mahasiswa != null) {
            	
            	BiodataMahasiswa biodataMahasiswa  = mahasiswa.ambilBiodata(true);
            	biodataMahasiswa.setEmail(emailBaru);
            	biodataMahasiswa.setHp(noHpBaru);
            	Common.refreshUpdate(ses, biodataMahasiswa);
                mahasiswa.setEmail(emailBaru);
                mahasiswa.setTelp(noHpBaru); 
                Common.refreshUpdate(ses, mahasiswa);
                users.setMahasiswa(mahasiswa);
            } else if (siswa != null) {
                siswa.setAlamatEmail(emailBaru);
                siswa.setTeleponSiswa(noHpBaru);
                Common.refreshUpdate(ses, siswa);
                users.setSiswa(siswa);
            } else if (guru != null) {
                guru.setAlamatGuru(emailBaru);
                guru.setTeleponGuru(noHpBaru);
                Common.refreshUpdate(ses, guru);
            } else if (dosen != null) {
                dosen.setEmail(emailBaru);
                dosen.setHp(noHpBaru);
                Common.refreshUpdate(ses, dosen);
            } else if (pegawai != null) {
                pegawai.setEmail(emailBaru);
                pegawai.setHp(noHpBaru);
                Common.refreshUpdate(ses, pegawai);
            } else {
                // Modifikasi untuk administrator jika objek turunan tidak ada
                users.setEmail(emailBaru);
                users.setHp(noHpBaru); 
                GeneralValueObject.ubahDataHistory(users);
                Common.refreshUpdate(ses, users);
            }

            // Memperbarui atribut sesi sehingga tampilan nama/email langsung ikut berubah
            session.setAttribute("users", users);
            session.setAttribute("usersTemp", users);

            status = "success";
            message = Common.getBahasaConfig("Profil berhasil diperbarui.");
            
         // Simpan transaksi jika semua proses berjalan lancar
            if (tx != null && tx.isActive()) {
                tx.commit();
            }
        }
    } catch (Exception e) {
    	if (tx != null && tx.isActive()) {
            try {
                tx.rollback();
            } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_do_update_profile.jsp:112");
                // Abaikan kesalahan saat rollback
            }
        }
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/common/_do_update_profile.jsp:116"); 
        status = "error";
        message = Common.getBahasaConfig("Terjadi kesalahan sistem saat menyimpan data profil.");
    } finally {
        if (ses != null) {
            try { ses.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_do_update_profile.jsp:121");}
            try { ses.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_do_update_profile.jsp:122");}
        }
        HibernateUtil.closeSessionQuietly(ses);
    }

    // Mengirimkan respons JSON dengan format penggabungan variabel String yang direkomendasikan
    out.print("{\"status\":\"" + status + "\", \"message\":\"" + message + "\"}");
%>