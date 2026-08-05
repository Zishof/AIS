<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.hibernate.Session" %>
<%@ page import="org.hibernate.Transaction" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.common.PasswordChecker" %>
<%@ page import="ais.database.hibernate.HibernateUtil" %>
<%@ page import="ais.database.model.GeneralValueObject" %>
<%@ page import="ais.database.model.Mahasiswa" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.database.model.sekolah.Siswa" %>
<%@ page import="ais.database.model.sisdes.Penduduk" %>
<%@ page import="ais.ui.util.WaktuUtil" %><%
    // Menyiapkan respons bawaan berformat JSON
    String status = "error";
    String message = "";
    
    Session ses = null;
    Transaction tx = null;

    try {
        String passwordLama = request.getParameter("passwordLama");
        String passwordBaru = request.getParameter("passwordBaru");

        if (passwordLama != null) passwordLama = passwordLama.trim();
        if (passwordBaru != null) passwordBaru = passwordBaru.trim();

        // Ambil data pengguna dari sesi standar JSP/Servlet
        Tbmuser users = (Tbmuser) session.getAttribute("users");
        if (users == null) {
            users = (Tbmuser) session.getAttribute("usersTemp");
        }

        // 1. Validasi Sesi dan Input Dasar
        if (users == null) {
            status = "error";
            message = Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali.");
        } else if (passwordBaru == null || passwordBaru.isEmpty()) {
            status = "error";
            message = Common.getBahasaConfig("Kata sandi baru tidak boleh kosong.");
        } else if (!PasswordChecker.isValidPassword(passwordBaru)) {
            status = "error";
            message = Common.getBahasaConfig("Kata sandi harus memenuhi kriteria: minimal 8 karakter, mengandung huruf, angka, dan setidaknya satu karakter spesial.");
        } else {
            
            Mahasiswa mahasiswa = users.getMahasiswa();
            Siswa siswa = users.getSiswa();
            Penduduk penduduk = users.getPenduduk();
            AnggotaKoperasi anggotaKoperasi = users.getAnggotaKoperasi();

            boolean isOldPasswordCorrect = false;
            boolean isParentContext = false;

            // 2. Verifikasi Kata Sandi Lama
            if (siswa == null && mahasiswa == null && penduduk == null) {
                if (passwordLama == null || passwordLama.isEmpty() || (users.getUserPassword() != null && passwordLama.equals(Common.desEncrypter.get().decrypt(users.getUserPassword().trim())))) {
                    isOldPasswordCorrect = true;
                }
            } else if (siswa != null) {
                if (passwordLama == null || passwordLama.isEmpty() || (siswa.getPass() != null && passwordLama.equals(Common.desEncrypter.get().decrypt(siswa.getPass().trim())))) {
                    isOldPasswordCorrect = true;
                } else if (siswa.getPassOrtu() != null && passwordLama.equals(Common.desEncrypter.get().decrypt(siswa.getPassOrtu().trim()))) {
                    isOldPasswordCorrect = true;
                    isParentContext = true;
                }
            } else if (penduduk != null) {
                if (passwordLama == null || passwordLama.isEmpty() || (penduduk.getPass() != null && passwordLama.equals(Common.desEncrypter.get().decrypt(penduduk.getPass().trim())))) {
                    isOldPasswordCorrect = true;
                }
            } else if (mahasiswa != null) {
                if (passwordLama == null || passwordLama.isEmpty() || (mahasiswa.getPass() != null && passwordLama.equals(Common.desEncrypter.get().decrypt(mahasiswa.getPass().trim())))) {
                    isOldPasswordCorrect = true;
                } else if (mahasiswa.getPassOrtu() != null && passwordLama.equals(Common.desEncrypter.get().decrypt(mahasiswa.getPassOrtu().trim()))) {
                    isOldPasswordCorrect = true;
                    isParentContext = true;
                }
            }

            // 3. Proses Eksekusi Pembaruan
            if (!isOldPasswordCorrect) {
                status = "error";
                message = Common.getBahasaConfig("Kata sandi lama yang Anda masukkan tidak sesuai.");
            } else {
                ses = HibernateUtil.openSession();
                tx = ses.getTransaction();
                tx.begin();

                String encryptedNewPass = Common.desEncrypter.get().encrypt(passwordBaru);

                
                
                 if (siswa == null && mahasiswa == null && penduduk == null) {
                	 ses.refresh(users);
                	 if(anggotaKoperasi != null){
                     	ses.refresh(anggotaKoperasi);
                     	anggotaKoperasi.setPass(passwordBaru);
                        ses.update(anggotaKoperasi);
                        users.setAnggotaKoperasi(anggotaKoperasi);
                     }
                	 
                	
                    users.setUserPassword(encryptedNewPass);
                    users.setUbahPasword(WaktuUtil.getDate());
                    ses.update(users);
                    Common.saveOrUpdateUserAccess(users, null, users.getUserId(), passwordBaru, users.getEmail());
                    
                    session.setAttribute("users", users);
                    session.setAttribute("usersTemp", users);
                    
                    message = Common.getBahasaConfig("Kata sandi berhasil diperbarui.");
                    status = "success";

                } else if (siswa != null) {
                	ses.refresh(siswa);
                    if (isParentContext) {
                        siswa.setPassOrtu(encryptedNewPass);
                        message = Common.getBahasaConfig("Kata sandi orang tua siswa berhasil diperbarui.");
                    } else {
                        siswa.setPass(encryptedNewPass);
                        siswa.setUbahPasword(WaktuUtil.getDate());
                        message = Common.getBahasaConfig("Kata sandi siswa berhasil diperbarui.");
                    }
                    ses.update(siswa);
                    users.setSiswa(siswa);
                    session.setAttribute("users", users);
                    session.setAttribute("usersTemp", users);
                    status = "success";

                } else if (penduduk != null) {
                	ses.refresh(penduduk);
                    penduduk.setPass(encryptedNewPass);
                    penduduk.setUbahPasword(WaktuUtil.getDate());
                    ses.update(penduduk);
                    users.setPenduduk(penduduk);
                    session.setAttribute("users", users);
                    session.setAttribute("usersTemp", users);
                    message = Common.getBahasaConfig("Kata sandi penduduk berhasil diperbarui.");
                    status = "success";

                } else if (mahasiswa != null) {
                	ses.refresh(mahasiswa);
                    if (isParentContext) {
                        mahasiswa.setPassOrtu(encryptedNewPass);
                        ses.update(mahasiswa);
                        Common.saveOrUpdateUserAccess(null, mahasiswa, mahasiswa.getUserOrtu(), passwordBaru, mahasiswa.getEmail());
                        message = Common.getBahasaConfig("Kata sandi orang tua mahasiswa berhasil diperbarui.");
                    } else {
                        mahasiswa.setPass(encryptedNewPass);
                        mahasiswa.setUbahPasword(WaktuUtil.getDate());
                        ses.update(mahasiswa);
                        Common.saveOrUpdateUserAccess(null, mahasiswa, mahasiswa.getNim(), passwordBaru, mahasiswa.getEmail());
                        message = Common.getBahasaConfig("Kata sandi mahasiswa berhasil diperbarui.");
                    }
                    users.setMahasiswa(mahasiswa);
                    session.setAttribute("users", users);
                    session.setAttribute("usersTemp", users);
                    status = "success";
                }

                // Simpan transaksi jika semua proses berjalan lancar
                if (tx != null && tx.isActive()) {
                    tx.commit();
                }
            }
        }
    } catch (Exception e) {
        // Kembalikan kondisi data (rollback) jika terjadi kegagalan agar tidak ada data korup
        if (tx != null && tx.isActive()) {
            try {
                tx.rollback();
            } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_do_ganti_password.jsp:171");
                // Abaikan kesalahan saat rollback
            }
        }
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/common/_do_ganti_password.jsp:175"); // Tercatat di log server untuk proses pelacakan
        status = "error";
        message = Common.getBahasaConfig("Terjadi kesalahan sistem saat memproses penggantian kata sandi.");
    } finally {
        // Blok ini selalu dieksekusi untuk memastikan tidak ada kebocoran memori (memory leak)
        if (ses != null) {
            try {
                ses.disconnect();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_do_ganti_password.jsp:183");}
            try {
                ses.close();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_do_ganti_password.jsp:186");}
        }
        HibernateUtil.closeSessionQuietly(ses);
    }

    // Mengirimkan respons JSON ke klien di titik akhir eksekusi file
    out.print("{\"status\":\"" + status + "\", \"message\":\"" + message + "\"}");
%>