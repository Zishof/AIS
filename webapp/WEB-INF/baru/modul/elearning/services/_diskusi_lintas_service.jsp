<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.CriteriaSpecification"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(401);
        out.print("{\"status\":\"error\"}");
        return;
    }

    int startIdx  = 0;
    int limitRows = 20;
    try { startIdx  = Integer.parseInt(request.getParameter("mulai")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_diskusi_lintas_service.jsp:29");}
    try { limitRows = Integer.parseInt(request.getParameter("banyak")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_diskusi_lintas_service.jsp:30");}

    String ta          = request.getParameter("q_ta");
    String smt         = request.getParameter("q_smt");
    String q_mk        = request.getParameter("q_matakuliah");
    String q_fakultas  = request.getParameter("q_fakultas");
    String q_jurusan   = request.getParameter("q_jurusan");

    boolean isSivitas = tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null
                     || tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null;
    boolean isSuperAdmin = !isSivitas && tbmuser.ambilFakultas() == null
                        && tbmuser.ambilJurusan() == null && tbmuser.ambilSekolah() == null
                        && tbmuser.ambilYayasan() == null;

    JSONObject resp = new JSONObject();
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();

        // ------------------------------------------------------------------
        // 1. Cari pertemuan yang punya diskusi (join PertemuanPunyaDiskusi)
        // ------------------------------------------------------------------
        Criteria cPtm = sess.createCriteria(Pertemuan.class, "ptm");
        cPtm.add(Restrictions.or(Restrictions.isNull("ptm.aktif"), Restrictions.eq("ptm.aktif", true)));

        // Hak akses sivitas
        if (isSivitas) {
            Disjunction djAkses = Restrictions.disjunction();
            if (tbmuser.getMahasiswa() != null)
                djAkses.add(Restrictions.like("ptm.mahasiswas", "%," + tbmuser.getMahasiswa().getId() + ",%"));
            if (tbmuser.ambilDosen() != null)
                djAkses.add(Restrictions.like("ptm.dosens", "%," + tbmuser.ambilDosen().getId() + ",%"));
            if (tbmuser.getSiswa() != null)
                djAkses.add(Restrictions.like("ptm.siswas", "%," + tbmuser.getSiswa().getId() + ",%"));
            if (tbmuser.ambilGuru() != null)
                djAkses.add(Restrictions.like("ptm.gurus", "%," + tbmuser.ambilGuru().getId() + ",%"));
            cPtm.add(djAkses);
        } else if (!isSuperAdmin) {
            if (tbmuser.ambilJurusan() != null)
                cPtm.add(Restrictions.eq("ptm.jurusan", tbmuser.ambilJurusan()));
            else if (tbmuser.ambilFakultas() != null) {
                cPtm.createAlias("ptm.jurusan", "jFak", CriteriaSpecification.LEFT_JOIN);
                cPtm.add(Restrictions.eq("jFak.fakultas", tbmuser.ambilFakultas()));
            }
            if (tbmuser.ambilSekolah() != null)
                cPtm.add(Restrictions.eq("ptm.sekolah", tbmuser.ambilSekolah()));
        }

        // Filter akademik dari UI
        if (ta != null && !ta.trim().isEmpty()) cPtm.add(Restrictions.eq("ptm.ta", ta));
        if (smt != null && !smt.trim().isEmpty()) cPtm.add(Restrictions.eq("ptm.smt", smt));

        if (q_mk != null && !q_mk.trim().isEmpty()) {
            cPtm.createAlias("ptm.perkuliahan", "prk", CriteriaSpecification.LEFT_JOIN);
            cPtm.createAlias("prk.matakuliah", "mk", CriteriaSpecification.LEFT_JOIN);
            cPtm.add(Restrictions.or(
                Restrictions.ilike("mk.kode", q_mk, MatchMode.ANYWHERE),
                Restrictions.ilike("mk.nama", q_mk, MatchMode.ANYWHERE)));
        }
        if (q_jurusan != null && !q_jurusan.trim().isEmpty()) {
            try { cPtm.add(Restrictions.eq("ptm.jurusan.id", Long.parseLong(q_jurusan))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_diskusi_lintas_service.jsp:90");}
        } else if (q_fakultas != null && !q_fakultas.trim().isEmpty()) {
            cPtm.createAlias("ptm.jurusan", "jFak", CriteriaSpecification.LEFT_JOIN);
            try { cPtm.add(Restrictions.eq("jFak.fakultas.id", Long.parseLong(q_fakultas))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_diskusi_lintas_service.jsp:93");}
        }

        // Hanya yang punya diskusi (jumlah > 0)
        cPtm.add(Restrictions.gt("ptm.jumlahDiskusi", 0));
        cPtm.addOrder(Order.desc("ptm.id"));

        // Count total
        cPtm.setProjection(Projections.rowCount());
        Long total = (Long) cPtm.uniqueResult();
        if (total == null) total = 0L;

        // Ambil data
        cPtm.setProjection(null);
        cPtm.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        cPtm.setFirstResult(startIdx);
        cPtm.setMaxResults(limitRows);
        cPtm.addOrder(Order.desc("ptm.tanggal"));

        List<Pertemuan> listPtm = cPtm.list();

        // ------------------------------------------------------------------
        // 2. Susun JSON array
        // ------------------------------------------------------------------
        JSONArray arr = new JSONArray();
        if (listPtm != null) {
            for (int i = 0; i < listPtm.size(); i++) {
                Pertemuan p = listPtm.get(i);
                JSONObject item = new JSONObject();
                item.put("id", p.getId());
                item.put("topik", p.getTopik() != null ? p.getTopik() : "");
                item.put("ke", p.getPertemuanKe() != null ? p.getPertemuanKe() : 0);
                item.put("tgl", p.getTanggal() != null ? Common.dateFormat6.get().format(p.getTanggal()) : "");
                item.put("jumlahDiskusi", p.ambilJumlahPertemuanPunyaDiskusi());
                item.put("komentarDitutup", p.getKomentarDitutup() != null && p.getKomentarDitutup());

                // Nama matakuliah/mata pelajaran
                String namaKelas = "";
                try {
                    if (p.getPerkuliahan() != null && p.getPerkuliahan().getMatakuliah() != null)
                        namaKelas = p.getPerkuliahan().getMatakuliah().getNama();
                    else if (p.getJadwalPelajaran() != null && p.getJadwalPelajaran().getMatapelajaran() != null)
                        namaKelas = p.getJadwalPelajaran().getMatapelajaran().getNama();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_diskusi_lintas_service.jsp:136");}
                item.put("namaKelas", namaKelas);

                // Nama dosen/guru pertama
                String pengajar = "";
                try {
                    List<Dosen> dosens = p.ambilDosen();
                    if (dosens != null && !dosens.isEmpty()) pengajar = dosens.get(0).getNama();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_diskusi_lintas_service.jsp:144");}
                item.put("pengajar", pengajar);

                arr.put(item);
            }
        }

        resp.put("status", "ok");
        resp.put("data", arr);
        resp.put("total", total);
        resp.put("sisaTampil", Math.max(0L, total - startIdx - limitRows));

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_diskusi_lintas_service.jsp:157");
        resp.put("status", "error");
        resp.put("pesan", e.getMessage());
    } finally {
        if (sess != null) {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_diskusi_lintas_service.jsp:162");}
            HibernateUtil.closeSessionQuietly(sess);
        }
    }

    out.print(resp.toString());
    out.flush();
%>
