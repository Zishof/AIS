<%@page import="ais.action.servlet.Main"%>
<%@page import="ais.common.SecurityFilter"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.hibernate.Session" %>
<%@ page import="org.hibernate.criterion.Restrictions" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.common.ConstantValues" %>
<%@ page import="ais.database.hibernate.HibernateUtil" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.database.model.Mahasiswa" %>
<%@ page import="ais.database.model.sekolah.Siswa" %>
<%@ page import="ais.database.model.sisdes.Penduduk" %>
<%
    JSONObject jsonResponse = new JSONObject();
    
    // Menggunakan getParameter sesuai standar Servlet API
    String username = request.getParameter("username");
    String password = request.getParameter("password");

    if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
        jsonResponse.put("status", "error");
        jsonResponse.put("message", Common.getBahasaConfig("Nama pengguna dan kata sandi tidak boleh kosong."));
        out.print(jsonResponse.toString());
        return;
    }

    Session dbSession = null;
    try {
        dbSession = HibernateUtil.openSession();
        String mypassword = Common.desEncrypter.get().encrypt(password);
        
        Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(dbSession.createCriteria(Mahasiswa.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.eq("nim", username))
                .add(Restrictions.eq("pass", mypassword))
                .setMaxResults(1), Mahasiswa.class);
                
        Siswa siswa = null;
        Tbmuser tbmuser = null;
        
        if (mahasiswa == null) {
            siswa = (Siswa) ConstantValues.simpleObject(dbSession.createCriteria(Siswa.class)
                    .add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
                    .add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", username))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("pass", mypassword))
                    .setMaxResults(1), Siswa.class);
        }

        if (mahasiswa == null && siswa == null) {
            tbmuser = (Tbmuser) ConstantValues.simpleObject(dbSession.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("userId", username))
                    .add(Restrictions.eq("userPassword", mypassword))
                    .setMaxResults(1), Tbmuser.class);
        }

        String linkProfile = "";
        
        if (tbmuser != null) {
            SecurityFilter.doAutoLogin(tbmuser.getUserId(), Common.desEncrypter.get().decrypt(tbmuser.getUserPassword()), false, linkProfile);
            Main.checkAndSetUserSession(request, true);
            jsonResponse.put("status", "success");
            jsonResponse.put("message", Common.getBahasaConfig("Otentikasi berhasil. Mengalihkan ke dasbor..."));
        } 
        else if (mahasiswa != null) {
            SecurityFilter.doAutoLogin(mahasiswa.getNim(), Common.desEncrypter.get().decrypt(mahasiswa.getPass()), false, linkProfile);
            Main.checkAndSetUserSession(request, true);
            jsonResponse.put("status", "success");
            jsonResponse.put("message", Common.getBahasaConfig("Otentikasi berhasil. Mengalihkan ke dasbor..."));
        } 
        else if (siswa != null) {
            // Memastikan menggunakan getter yang tepat, sesuaikan jika di model menggunakan getNomorInduk()
            String identitasSiswa = siswa.getNomorIndukNasional() != null ? siswa.getNomorIndukNasional() : siswa.getNomorInduk();
            SecurityFilter.doAutoLogin(identitasSiswa, Common.desEncrypter.get().decrypt(siswa.getPass()), false, linkProfile);
            Main.checkAndSetUserSession(request, true);
            jsonResponse.put("status", "success");
            jsonResponse.put("message", Common.getBahasaConfig("Otentikasi berhasil. Mengalihkan ke dasbor..."));
        } 
        else {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", Common.getBahasaConfig("Nama pengguna atau kata sandi tidak valid. Silakan periksa kembali."));
        }

    } catch (Exception e) {
        jsonResponse.put("status", "error");
        jsonResponse.put("message", Common.getBahasaConfig("Terjadi kesalahan pada sistem. Silakan hubungi administrator."));
    } finally {
        if (dbSession != null && dbSession.isOpen()) {
            dbSession.disconnect();
            dbSession.close();
        }
        HibernateUtil.closeSessionQuietly(dbSession);
    }

    out.print(jsonResponse.toString());
    out.flush();
%>