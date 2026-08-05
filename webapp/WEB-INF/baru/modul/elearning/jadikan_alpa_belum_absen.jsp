<%@ page language="java" pageEncoding="UTF-8"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.List"%>
<%
    response.setContentType("application/json; charset=UTF-8");

    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        out.print("{\"status\":\"error\",\"message\":\"Tidak terautentikasi\"}");
        return;
    }

    String reqPertemuan = request.getParameter("pertemuan");
    if (reqPertemuan == null || reqPertemuan.trim().isEmpty()) {
        out.print("{\"status\":\"error\",\"message\":\"Parameter pertemuan wajib diisi\"}");
        return;
    }

    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, reqPertemuan.trim(), true);
    if (pertemuan == null) {
        out.print("{\"status\":\"error\",\"message\":\"Pertemuan tidak ditemukan\"}");
        return;
    }

    if (!pertemuan.bolehUbahAbsenSaja(tbmuser)) {
        out.print("{\"status\":\"error\",\"message\":\"Tidak mempunyai hak akses\"}");
        return;
    }

    Statusabsensi alpa = ConstantValues.TIDAK_ADA_ALASAN;
    if (alpa == null) {
        out.print("{\"status\":\"error\",\"message\":\"Status Alpha tidak ditemukan\"}");
        return;
    }

    int jumlah = 0;
    Session mySession = HibernateUtil.openSession();
    try {
        mySession.refresh(pertemuan);

        VOPembelajaran vop = pertemuan.ambilVOPembelajaran();

        List<Long> mhsIds = (vop != null) ? vop.ambilMahasiswaById() : null;
        if (mhsIds != null) {
            for (Long mId : mhsIds) {
                Statusabsensi cur = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(mId));
                if (cur == null || (ConstantValues.BELUM_ABSEN != null && ConstantValues.BELUM_ABSEN.getId().equals(cur.getId()))) {
                    pertemuan.populate(mId, alpa, "", null, "", "", "Mahasiswa");
                    jumlah++;
                }
            }
        }

        List<Long> swIds = (vop != null) ? vop.ambilSiswaById() : null;
        if (swIds != null) {
            for (Long sId : swIds) {
                Statusabsensi cur = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(sId));
                if (cur == null || (ConstantValues.BELUM_ABSEN != null && ConstantValues.BELUM_ABSEN.getId().equals(cur.getId()))) {
                    pertemuan.populate(sId, alpa, "", null, "", "", "Siswa");
                    jumlah++;
                }
            }
        }

        if (jumlah > 0) {
            mySession.getTransaction().begin();
            Common.refreshUpdate(mySession, pertemuan);
            mySession.getTransaction().commit();
        }

        out.print("{\"status\":\"ok\",\"jumlah\":" + jumlah + "}");
    } catch (Exception e) {
        try {
            if (mySession.getTransaction() != null && mySession.getTransaction().isActive()) {
                mySession.getTransaction().rollback();
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/jadikan_alpa_belum_absen.jsp:86");}
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/jadikan_alpa_belum_absen.jsp:87");
        out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
    } finally {
        HibernateUtil.closeSessionQuietly(mySession);
    }
    out.flush();
%>
