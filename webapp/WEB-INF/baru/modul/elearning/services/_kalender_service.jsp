<%@page import="ais.common.ConstantValues"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.CriteriaSpecification"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.DetachedCriteria"%>
<%@page import="org.hibernate.criterion.Subqueries"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.sekolah.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(401); 
        return;
    }

    String startStr = request.getParameter("start");
    String endStr = request.getParameter("end");
    
    // TANGKAP PARAMETER CHECKBOX KATEGORI
    boolean showPertemuan = !"false".equals(request.getParameter("show_pertemuan"));
    boolean showUjian = !"false".equals(request.getParameter("show_ujian"));
    boolean showTugas = !"false".equals(request.getParameter("show_tugas"));
    boolean showTugasKelompok = !"false".equals(request.getParameter("show_tugas_kelompok"));
    
    SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date startDate = null;
    Date endDate = null;
    try {
        if(startStr != null && startStr.length() >= 10) startDate = sdfIso.parse(startStr.substring(0, 10));
        if(endStr != null && endStr.length() >= 10) endDate = sdfIso.parse(endStr.substring(0, 10));
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_kalender_service.jsp:45");}

    if (startDate == null) startDate = new Date();
    if (endDate == null) { endDate = new Date(); endDate.setMonth(endDate.getMonth() + 1); }

    String ta = request.getParameter("q_ta");
    String smt = request.getParameter("q_smt");
    String q_fakultas = request.getParameter("q_fakultas");
    String q_jurusan = request.getParameter("q_jurusan");
    String q_yayasan = request.getParameter("q_yayasan");
    String q_sekolah = request.getParameter("q_sekolah");

    boolean isSivitas = tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null || tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null;
    
    if (!isSivitas) {
        if (tbmuser.ambilJurusan() != null) q_jurusan = tbmuser.ambilJurusan().getId().toString();
        if (tbmuser.ambilFakultas() != null) q_fakultas = tbmuser.ambilFakultas().getId().toString();
        if (tbmuser.ambilSekolah() != null) q_sekolah = tbmuser.ambilSekolah().getId().toString();
        if (tbmuser.ambilYayasan() != null) q_yayasan = tbmuser.ambilYayasan().getId().toString();
    } else {
        q_jurusan = null; q_fakultas = null; q_sekolah = null; q_yayasan = null;
    }

    final boolean fIsSivitas = isSivitas;
    final Siswa siswa = tbmuser.getSiswa();
    final Mahasiswa mahasiswa = tbmuser.getMahasiswa();
    final Dosen dosen = tbmuser.ambilDosen();
    final Guru guru = tbmuser.ambilGuru();
    
    final String fTa = ta;
    final String fSmt = smt;
    final String fJurusan = q_jurusan;
    final String fFakultas = q_fakultas;
    final String fSekolah = q_sekolah;
    final String fYayasan = q_yayasan;

    class FilterHelper {
        public void apply(Criteria c, String prefix) {
            if (fIsSivitas) {
                if (siswa != null || mahasiswa != null || dosen != null || guru != null) {
                    Disjunction djAkses = Restrictions.disjunction();
                    if (siswa != null) djAkses.add(Restrictions.like(prefix + "siswas", "%," + siswa.getId() + ",%"));
                    if (mahasiswa != null) djAkses.add(Restrictions.like(prefix + "mahasiswas", "%," + mahasiswa.getId() + ",%"));
                    if (dosen != null) djAkses.add(Restrictions.like(prefix + "dosens", "%," + dosen.getId() + ",%"));
                    if (guru != null) djAkses.add(Restrictions.like(prefix + "gurus", "%," + guru.getId() + ",%"));
                    c.add(djAkses);
                }
            }
            
            if (fTa != null && !fTa.trim().isEmpty()) c.add(Restrictions.eq(prefix + "ta", fTa));
            if (fSmt != null && !fSmt.trim().isEmpty()) c.add(Restrictions.eq(prefix + "smt", fSmt));

            if (fJurusan != null && !fJurusan.trim().isEmpty()) {
                try { c.add(Restrictions.eq(prefix + "jurusan.id", Long.parseLong(fJurusan))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_kalender_service.jsp:98");}
            } else if (fFakultas != null && !fFakultas.trim().isEmpty()) {
                // alias always safe here: if/else if above prevents double-add within a single apply() call
                if (prefix.isEmpty()) c.createAlias("jurusan", "jStruk", CriteriaSpecification.LEFT_JOIN);
                else c.createAlias(prefix + "jurusan", "jStruk", CriteriaSpecification.LEFT_JOIN);
                try { c.add(Restrictions.eq("jStruk.fakultas.id", Long.parseLong(fFakultas))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_kalender_service.jsp:103");}
            }

            if (fSekolah != null && !fSekolah.trim().isEmpty()) {
                try { c.add(Restrictions.eq(prefix + "sekolah.id", Long.parseLong(fSekolah))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_kalender_service.jsp:107");}
            } else if (fYayasan != null && !fYayasan.trim().isEmpty()) {
                if (prefix.isEmpty()) c.createAlias("sekolah", "sStruk", CriteriaSpecification.LEFT_JOIN);
                else c.createAlias(prefix + "sekolah", "sStruk", CriteriaSpecification.LEFT_JOIN);
                try { c.add(Restrictions.eq("sStruk.yayasan.id", Long.parseLong(fYayasan))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_kalender_service.jsp:111");}
            }
        }
    }
    
    FilterHelper filterHelper = new FilterHelper();
    Session sess = HibernateUtil.openSession();
    JSONArray events = new JSONArray();

    try {
        // ==============================================================================
        // 1. QUERY: PERTEMUAN / KELAS (Dijalankan hanya jika Checkbox dipilih)
        // ==============================================================================
        if (showPertemuan) {
            Criteria cPert = sess.createCriteria(Pertemuan.class);
            cPert.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            cPert.add(Restrictions.ge("mulai", startDate));
            cPert.add(Restrictions.le("mulai", endDate));
            cPert.add(Restrictions.or(Restrictions.eq("judultugas", ""), Restrictions.isNull("judultugas")) );
            filterHelper.apply(cPert, ""); 
            
            cPert.setMaxResults(250);
            List<Pertemuan> listPert = ConstantValues.simpleList(cPert,Pertemuan.class);
            if (listPert != null) {
                String lblPert = Common.getBahasaConfig("Pert.");
                for(Pertemuan p : listPert) {
                    if (p.getMulai() == null) continue;
                    JSONObject ev = new JSONObject();
                    ev.put("id", "P_" + p.getId());
                    String infoKe = p.getPertemuanKe() != null ? p.getPertemuanKe().toString() : "-";
                    try{ ev.put("title", lblPert + " " + infoKe + " | " + p.ambilVOPembelajaran().infoSangatSimple()); } 
                    catch(Exception e){ ev.put("title", lblPert + " " + infoKe + " | " + p.info()); }
                    
                    ev.put("start", sdfDateTime.format(p.getMulai()));
                    if (p.getSelesai() != null) ev.put("end", sdfDateTime.format(p.getSelesai()));
                    
                    ev.put("display", "block");
                    ev.put("backgroundColor", "#0275d8");
                    ev.put("borderColor", "#01447e");
                    ev.put("textColor", "#ffffff"); 
                    
                    JSONObject ext = new JSONObject();
                    ext.put("kategori", Common.getBahasaConfig("Pertemuan Akademik"));
                    ext.put("icon", "fas fa-chalkboard-teacher");
                    ext.put("keterangan", p.getMetodePembelajaran() != null ? p.getMetodePembelajaran() : Common.getBahasaConfig("Tatap Muka / Online"));
                    ev.put("extendedProps", ext);
                    events.put(ev);
                }
            }
        }

        // ==============================================================================
        // 2. QUERY: JADWAL UJIAN (Dijalankan hanya jika Checkbox dipilih)
        // ==============================================================================
        if (showUjian) {
            Criteria cUjian = sess.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "pPert");
            cUjian.add(Restrictions.or(Restrictions.isNull("pPert.aktif"), Restrictions.eq("pPert.aktif", true)));
            cUjian.add(Restrictions.ge("mulaiUjian", startDate));
            cUjian.add(Restrictions.le("mulaiUjian", endDate));
            filterHelper.apply(cUjian, "pPert.");

            cUjian.setMaxResults(250);
            List<PertemuanPunyaUjian> listUjian = ConstantValues.simpleList(cUjian,PertemuanPunyaUjian.class);
            if (listUjian != null) {
                String lblUjian = Common.getBahasaConfig("Ujian");
                for(PertemuanPunyaUjian u : listUjian) {
                    if (u.getMulaiUjian() == null) continue;
                    JSONObject ev = new JSONObject();
                    ev.put("id", "U_" + u.getId());
                    ev.put("title", u.getNama() != null ? u.getNama() : lblUjian);
                    ev.put("start", sdfDateTime.format(u.getMulaiUjian()));
                    if (u.getSampaiUjian() != null) ev.put("end", sdfDateTime.format(u.getSampaiUjian()));
                    
                    ev.put("display", "block");
                    ev.put("backgroundColor", "#d9534f");
                    ev.put("borderColor", "#b52b27");
                    ev.put("textColor", "#ffffff");
                    
                    JSONObject ext = new JSONObject();
                    ext.put("kategori", Common.getBahasaConfig("Jadwal Ujian"));
                    ext.put("icon", "fas fa-check-square");
                    ext.put("keterangan", u.getNama() != null ? u.getNama() : "-");
                    ev.put("extendedProps", ext);
                    events.put(ev);
                }
            }
        }

        // ==============================================================================
        // 3A & 3B. QUERY: TUGAS INDIVIDU (Dijalankan hanya jika Checkbox dipilih)
        // ==============================================================================
        if (showTugas) {
            // 3A. Tugas dari Tabel Tugas Pertemuan
            Criteria cTugas = sess.createCriteria(TugasPertemuan.class).createAlias("pertemuanData", "pPert");
            cTugas.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            cTugas.add(Restrictions.or(Restrictions.isNull("pPert.aktif"), Restrictions.eq("pPert.aktif", true)));
            cTugas.add(Restrictions.isNotNull("judultugas"));
            cTugas.add(Restrictions.ge("selesai", startDate));
            cTugas.add(Restrictions.le("selesai", endDate));
            filterHelper.apply(cTugas, "pPert.");

            cTugas.setMaxResults(250);
            List<TugasPertemuan> listTugas = ConstantValues.simpleList(cTugas,TugasPertemuan.class);
            if (listTugas != null) {
                String lblTugas = Common.getBahasaConfig("Tugas");
                String lblTenggat = Common.getBahasaConfig("Tenggat Waktu");
                for(TugasPertemuan t : listTugas) {
                    if (t.getMulai() == null) continue;
                    JSONObject ev = new JSONObject();
                    ev.put("id", "T_" + t.getId() + "_TugasPertemuan");
                    ev.put("title", lblTugas + ": " + t.getJudultugas());
                    ev.put("start", sdfDateTime.format(t.getMulai()));
                    if (t.getSelesai() != null) ev.put("end", sdfDateTime.format(t.getSelesai()));
                    
                    ev.put("display", "block");
                    ev.put("backgroundColor", "#218838");
                    ev.put("borderColor", "#1e7e34");
                    ev.put("textColor", "#ffffff");
                    
                    JSONObject ext = new JSONObject();
                    ext.put("kategori", Common.getBahasaConfig("Tugas Individu"));
                    ext.put("icon", "fas fa-clipboard-list");
                    ext.put("keterangan", lblTenggat + ": " + (t.getSelesai() != null ? sdfDateTime.format(t.getSelesai()) : "-"));
                    ev.put("extendedProps", ext);
                    events.put(ev);
                }
            }

            // 3B. Tugas dari Tabel Pertemuan
            Criteria cTugasPert = sess.createCriteria(Pertemuan.class);
            cTugasPert.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            cTugasPert.add(Restrictions.isNotNull("judultugas"));
            cTugasPert.add(Restrictions.ne("judultugas", ""));
            cTugasPert.add(Restrictions.ge("selesai", startDate));
            cTugasPert.add(Restrictions.le("selesai", endDate));
            filterHelper.apply(cTugasPert, ""); 

            cTugasPert.setMaxResults(250);
            List<Pertemuan> listTugasPert = ConstantValues.simpleList(cTugasPert,Pertemuan.class);
            if (listTugasPert != null) {
                String lblTugas = Common.getBahasaConfig("Tugas");
                String lblTenggat = Common.getBahasaConfig("Tenggat Waktu");
                for(Pertemuan t : listTugasPert) {
                    if (t.getMulai() == null) continue;
                    JSONObject ev = new JSONObject();
                    ev.put("id", "T_" + t.getId() + "_Pertemuan");
                    ev.put("title", lblTugas + ": " + t.getJudultugas());
                    ev.put("start", sdfDateTime.format(t.getMulai()));
                    if (t.getSelesai() != null) ev.put("end", sdfDateTime.format(t.getSelesai()));
                    
                    ev.put("display", "block");
                    ev.put("backgroundColor", "#218838");
                    ev.put("borderColor", "#1e7e34");
                    ev.put("textColor", "#ffffff");
                    
                    JSONObject ext = new JSONObject();
                    ext.put("kategori", Common.getBahasaConfig("Tugas Individu"));
                    ext.put("icon", "fas fa-clipboard-list");
                    ext.put("keterangan", lblTenggat + ": " + (t.getSelesai() != null ? sdfDateTime.format(t.getSelesai()) : "-"));
                    ev.put("extendedProps", ext);
                    events.put(ev);
                }
            }
        }
        
        // ==============================================================================
        // 4. QUERY: TUGAS KELOMPOK (Dijalankan hanya jika Checkbox dipilih)
        // ==============================================================================
        if (showTugasKelompok) {
            Criteria cTugasKlp = sess.createCriteria(TugasKelompok.class).createAlias("pertemuanData", "pPert");
            cTugasKlp.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            cTugasKlp.add(Restrictions.or(Restrictions.isNull("pPert.aktif"), Restrictions.eq("pPert.aktif", true)));
            cTugasKlp.add(Restrictions.isNotNull("judultugas"));
            cTugasKlp.add(Restrictions.ge("selesai", startDate));
            cTugasKlp.add(Restrictions.le("selesai", endDate));
            filterHelper.apply(cTugasKlp, "pPert.");

            cTugasKlp.setMaxResults(250);
            List<TugasKelompok> listTugasKlp = ConstantValues.simpleList(cTugasKlp,TugasKelompok.class);
            if (listTugasKlp != null) {
                String lblTgsKlp = Common.getBahasaConfig("Tgs Klp");
                String lblTenggat = Common.getBahasaConfig("Tenggat Waktu");
                for(TugasKelompok tk : listTugasKlp) {
                    if (tk.getMulai() == null) continue;
                    JSONObject ev = new JSONObject();
                    ev.put("id", "TK_" + tk.getId());
                    ev.put("title", lblTgsKlp + ": " + tk.getJudultugas());
                    ev.put("start", sdfDateTime.format(tk.getMulai()));
                    if (tk.getSelesai() != null) ev.put("end", sdfDateTime.format(tk.getSelesai()));
                    
                    ev.put("display", "block");
                    ev.put("backgroundColor", "#6610f2");
                    ev.put("borderColor", "#520dc2");
                    ev.put("textColor", "#ffffff");
                    
                    JSONObject ext = new JSONObject();
                    ext.put("kategori", Common.getBahasaConfig("Tugas Kelompok"));
                    ext.put("icon", "fas fa-users-cog");
                    ext.put("keterangan", lblTenggat + ": " + (tk.getSelesai() != null ? sdfDateTime.format(tk.getSelesai()) : "-"));
                    ev.put("extendedProps", ext);
                    events.put(ev);
                }
            }
        }

    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_kalender_service.jsp:317");
    } finally {
        try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_kalender_service.jsp:319");}
        ais.common.ElearningSessionUtil.closeQuietly(sess);
        HibernateUtil.closeSessionQuietly(sess);
    }

    out.print(events.toString());
    out.flush();
%>