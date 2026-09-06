<%@page import="ais.common.Common"%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.GelombangPendaftaran"%>
<%@page import="ais.database.model.JenisSeleksi"%>
<%@page import="ais.database.model.JenisSekolahMahasiswaBaru"%>
<%@page import="ais.database.model.JurusanSekolahMahasiswaBaru"%>
<%@page import="ais.database.model.PilihanPaketPerJurusanMhsBaru"%>
<%@page import="ais.database.model.PaketPunyaGelombangPendaftaran"%>
<%@page import="ais.database.model.PaketJurusanPmb"%>
<%@page import="ais.database.model.PaketPunyaProgram"%>
<%@page import="ais.database.model.Paket"%>
<%@page import="ais.database.model.Program"%>
<%@page import="ais.database.model.Wilayah"%>
<%@page import="ais.database.model.NamaSekolahAsal"%>
<%@page import="ais.database.model.Agama"%>
<%@page import="ais.database.model.JenisKartuIdentitasMahasiswaBaru"%>
<%@page import="ais.database.model.Negara"%>
<%@page import="ais.database.model.PendidikanOrangTua"%>
<%@page import="ais.database.model.PekerjaanOrangTua"%>
<%@page import="ais.database.model.PendapatanOrangTua"%>
<%@page import="ais.database.model.AfiliasiCalonMahasiswa"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
    // Helper untuk JSON Response agar aman dari String/Enter/Kutip
    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

	//=========================================================================
	// HELPER METHODS: Pencarian Kota dan Propinsi berdasarkan Wilayah (Fuzzy)
	// =========================================================================
	@SuppressWarnings("unchecked")
	private ais.database.model.Propinsi findOrCreatePropinsi(org.hibernate.Session session, String namaProp) {
	    if (namaProp == null || namaProp.trim().isEmpty()) return null;
	
	    String cleanNamaTarget = org.apache.commons.lang.StringUtils.replace(namaProp, "Prop.", "").trim().toLowerCase();
	    java.util.List<ais.database.model.Propinsi> list = ConstantValues.simpleList(  session.createCriteria(ais.database.model.Propinsi.class)
	        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
	        .add(org.hibernate.criterion.Restrictions.isNotNull("nama"))
	        .add(org.hibernate.criterion.Restrictions.ne("nama", ""))
	        ,ais.database.model.Propinsi.class);
	
	    ais.database.model.Propinsi bestMatch = null;
	    int minDistance = Integer.MAX_VALUE;
	
	    for (ais.database.model.Propinsi p : list) {
	        if (p.getNama() == null) continue;
	        String cleanName = org.apache.commons.lang.StringUtils.replace(p.getNama(), "Prop.", "").trim().toLowerCase();
	        int distance = org.apache.commons.lang.StringUtils.getLevenshteinDistance(cleanName, cleanNamaTarget);
	        if (distance < minDistance) {
	            minDistance = distance;
	            bestMatch = p;
	        }
	    }
	
	    if (bestMatch != null && minDistance < 2) return bestMatch;
	
	    // Jika tidak ketemu, buat baru
	    ais.database.model.Propinsi newProp = new ais.database.model.Propinsi();
	    newProp.setNama(namaProp.trim());
	    try {
	        newProp.setNegara((ais.database.model.Negara) ais.common.ConstantValues.ambil(ais.database.model.Negara.class.getName(), 1L, true));
	    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_service.jsp:73");}
	
	    boolean isNewTx = false;
	    if (!session.getTransaction().isActive()) {
	        session.getTransaction().begin();
	        isNewTx = true;
	    }
	    session.save(newProp);
	    if (isNewTx) session.getTransaction().commit();
	
	    return newProp;
	}
	
	@SuppressWarnings("unchecked")
	private ais.database.model.Kota findBestMatchKota(org.hibernate.Session session, ais.database.model.Propinsi p, String namaKab) {
	    if (namaKab == null || p == null) return null;
	
	    String cleanTarget = org.apache.commons.lang.StringUtils.replace(namaKab, "Kab.", "");
	    cleanTarget = org.apache.commons.lang.StringUtils.replace(cleanTarget, "Kota", "").trim().toLowerCase();
	    
	    java.util.List<ais.database.model.Kota> list = session.createCriteria(ais.database.model.Kota.class)
	    	.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
	        .add(org.hibernate.criterion.Restrictions.eq("propinsi", p))
	        .add(org.hibernate.criterion.Restrictions.isNotNull("nama"))
	        .list();
	
	    ais.database.model.Kota bestMatch = null;
	    int minDistance = Integer.MAX_VALUE;
	
	    for (ais.database.model.Kota k : list) {
	        if (k.getNama() == null) continue;
	        String cleanName = org.apache.commons.lang.StringUtils.replace(k.getNama(), "Kab.", "");
	        cleanName = org.apache.commons.lang.StringUtils.replace(cleanName, "Kota", "").trim().toLowerCase();
	        int distance = org.apache.commons.lang.StringUtils.getLevenshteinDistance(cleanName, cleanTarget);
	        if (distance < minDistance) {
	            minDistance = distance;
	            bestMatch = k;
	        }
	    }
	    return (minDistance < 2) ? bestMatch : null;
	}
%>

<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    String action = request.getParameter("action");
    
    String gelIdStr = request.getParameter("gelId");
    String jenisSekolahIdStr = request.getParameter("jenisSekolahId");
    String jurIdStr = request.getParameter("jurId");
    String pktIdStr = request.getParameter("paketId");
    
    Long gelId = (gelIdStr != null && !gelIdStr.trim().isEmpty() && !gelIdStr.equals("undefined") && !gelIdStr.equals("null")) ? Long.valueOf(gelIdStr) : null;
    Long jenisSekolahId = (jenisSekolahIdStr != null && !jenisSekolahIdStr.trim().isEmpty() && !jenisSekolahIdStr.equals("undefined")) ? Long.valueOf(jenisSekolahIdStr) : null;
    Long jurId = (jurIdStr != null && !jurIdStr.trim().isEmpty() && !jurIdStr.equals("undefined")) ? Long.valueOf(jurIdStr) : null;
    Long pktId = (pktIdStr != null && !pktIdStr.trim().isEmpty() && !pktIdStr.equals("undefined")) ? Long.valueOf(pktIdStr) : null;

    Session sessionLocal = null;
    StringBuilder json = new StringBuilder();

    try {
        sessionLocal = HibernateUtil.openSession();
        GelombangPendaftaran gel = (gelId != null) ? (GelombangPendaftaran) sessionLocal.get(GelombangPendaftaran.class, gelId) : null;

        // =====================================================================
        // 1. DATA REFERENSI LENGKAP (1x Fetch untuk semua combo box standard)
        // =====================================================================
        if ("referensi_lengkap".equals(action)) {
            json.append("{\"status\":\"success\",");
            
            // Agama
            List<Object[]> lsAgama = sessionLocal.createCriteria(Agama.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama"))).addOrder(Order.asc("nama")).list();
            json.append("\"agama\":["); 
            for(int i=0; i<lsAgama.size(); i++) { if(i>0) json.append(","); json.append("{\"id\":\"").append(lsAgama.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(lsAgama.get(i)[1] != null ? lsAgama.get(i)[1].toString() : "")).append("\"}"); } 
            json.append("],");

            // Identitas
            List<Object[]> lsId = sessionLocal.createCriteria(JenisKartuIdentitasMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama"))).addOrder(Order.asc("nama")).list();
            json.append("\"identitas\":["); 
            for(int i=0; i<lsId.size(); i++) { if(i>0) json.append(","); json.append("{\"id\":\"").append(lsId.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(lsId.get(i)[1] != null ? lsId.get(i)[1].toString() : "")).append("\"}"); } 
            json.append("],");

            // Negara
            List<Object[]> lsNegara = sessionLocal.createCriteria(Negara.class).setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama"))).addOrder(Order.asc("nama")).list();
            json.append("\"negara\":["); 
            for(int i=0; i<lsNegara.size(); i++) { if(i>0) json.append(","); json.append("{\"id\":\"").append(lsNegara.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(lsNegara.get(i)[1] != null ? lsNegara.get(i)[1].toString() : "")).append("\"}"); } 
            json.append("],");

            // Afiliasi
            List<Object[]> lsAfil = sessionLocal.createCriteria(AfiliasiCalonMahasiswa.class).setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama"))).addOrder(Order.asc("nama")).list();
            json.append("\"afiliasi\":["); 
            for(int i=0; i<lsAfil.size(); i++) { if(i>0) json.append(","); json.append("{\"id\":\"").append(lsAfil.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(lsAfil.get(i)[1] != null ? lsAfil.get(i)[1].toString() : "")).append("\"}"); } 
            json.append("],");

            // Pendidikan
            List<Object[]> lsPend = sessionLocal.createCriteria(PendidikanOrangTua.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama"))).addOrder(Order.asc("nama")).list();
            json.append("\"pendidikan\":["); 
            for(int i=0; i<lsPend.size(); i++) { if(i>0) json.append(","); json.append("{\"id\":\"").append(lsPend.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(lsPend.get(i)[1] != null ? lsPend.get(i)[1].toString() : "")).append("\"}"); } 
            json.append("],");

            // Pekerjaan
            List<Object[]> lsPek = sessionLocal.createCriteria(PekerjaanOrangTua.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama"))).addOrder(Order.asc("nama")).list();
            json.append("\"pekerjaan\":["); 
            for(int i=0; i<lsPek.size(); i++) { if(i>0) json.append(","); json.append("{\"id\":\"").append(lsPek.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(lsPek.get(i)[1] != null ? lsPek.get(i)[1].toString() : "")).append("\"}"); } 
            json.append("],");

            // Pendapatan
            List<Object[]> lsPendap = sessionLocal.createCriteria(PendapatanOrangTua.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama"))).addOrder(Order.asc("nama")).list();
            json.append("\"pendapatan\":["); 
            for(int i=0; i<lsPendap.size(); i++) { if(i>0) json.append(","); json.append("{\"id\":\"").append(lsPendap.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(lsPendap.get(i)[1] != null ? lsPendap.get(i)[1].toString() : "")).append("\"}"); } 
            json.append("]}");
        }

        // =====================================================================
        // 2. GELOMBANG PENDAFTARAN
        // =====================================================================
        else if ("gelombang".equals(action)) {
            String ta = request.getParameter("ta");
            Criteria crit = sessionLocal.createCriteria(GelombangPendaftaran.class);
            if(ta != null && !ta.isEmpty() && !ta.equals("undefined")) crit.add(Restrictions.eq("tahunAkademik", ta));
            crit.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            crit.add(Restrictions.or(Restrictions.isNull("bisaDipilihPendaftarOnline"),
                    Restrictions.eq("bisaDipilihPendaftarOnline", true)));
            crit.add(Restrictions.and(Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()),
                    Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate())));
            ais.database.model.PerguruanTinggi ptGelombangDropdown = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
            crit.add(ptGelombangDropdown == null || ptGelombangDropdown.getId() == null
                    ? Restrictions.sqlRestriction("1=1")
                    : Restrictions.eq("perguruanTinggi", ptGelombangDropdown));

            crit.createAlias("jenisSeleksi", "js", Criteria.LEFT_JOIN);
            crit.setProjection(Projections.projectionList()
                .add(Projections.property("id")).add(Projections.property("nama"))
                .add(Projections.property("tampilkanUploadFoto")).add(Projections.property("tampilFormTambahanSaatRegistrasi"))
                .add(Projections.property("tampilFormTambahanSaatLoginCalonMhs")).add(Projections.property("tidakBolehMemilihProgramLain"))
                .add(Projections.property("program")).add(Projections.property("mahasiswaPindahanBolehMendaftar"))
                .add(Projections.property("tahunAngkatanMinimal")).add(Projections.property("tahunAngkatanMaksimal"))
                .add(Projections.property("js.id")).add(Projections.property("jenisSeleksiLain"))
            ).addOrder(Order.asc("nama"));
            
            List<Object[]> list = crit.list();
            json.append("{\"status\":\"success\",\"data\":[");
            for (int i=0; i<list.size(); i++) {
                Object[] r = list.get(i);
                if(i>0) json.append(",");
                json.append("{")
                    .append("\"id\":\"").append(r[0]).append("\",\"nama\":\"").append(escapeJson(r[1] != null ? r[1].toString() : "")).append("\",")
                    .append("\"tampilkanUploadFoto\":").append(r[2]!=null?(Boolean)r[2]:true).append(",")
                    .append("\"tampilFormTambahanSaatRegistrasi\":").append(r[3]!=null?(Boolean)r[3]:true).append(",")
                    .append("\"tampilFormTambahanSaatLoginCalonMhs\":").append(r[4]!=null?(Boolean)r[4]:true).append(",")
                    .append("\"tidakBolehMemilihProgramLain\":").append(r[5]!=null?(Boolean)r[5]:false).append(",")
                    .append("\"program\":\"").append(r[6]!=null?escapeJson(r[6].toString()):"").append("\",")
                    .append("\"mahasiswaPindahanBolehMendaftar\":").append(r[7]!=null?(Boolean)r[7]:false).append(",")
                    .append("\"tahunAngkatanMinimal\":").append(r[8]!=null?r[8]:1970).append(",")
                    .append("\"tahunAngkatanMaksimal\":").append(r[9]!=null?r[9]:2030).append(",")
                    .append("\"jenisSeleksiId\":\"").append(r[10]!=null?r[10]:"").append("\",")
                    .append("\"jenisSeleksiLain\":\"").append(r[11]!=null?escapeJson(r[11].toString()):"").append("\"}");
            }
            json.append("]}");
        }

        // =====================================================================
        // 3. JENIS SELEKSI (Master dan Filter)
        // =====================================================================
        else if ("jenis_seleksi_master".equals(action)) {
            List<Object[]> list = sessionLocal.createCriteria(JenisSeleksi.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama")).add(Projections.property("kode")))
                .addOrder(Order.asc("nama")).list();
            json.append("{\"status\":\"success\",\"data\":[");
            for(int i=0; i<list.size(); i++) { 
                if(i>0) json.append(","); 
                json.append("{\"id\":\"").append(list.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(list.get(i)[1] != null ? list.get(i)[1].toString() : "")).append("\",\"kode\":\"").append(list.get(i)[2]!=null?escapeJson(list.get(i)[2].toString()):"").append("\"}"); 
            }
            json.append("]}");
        }
        else if ("jenis_seleksi".equals(action)) {
            List<JenisSeleksi> jsList = (gel != null) ? gel.ambilJenisSeleksi() : new ArrayList<JenisSeleksi>();
            json.append("{\"status\":\"success\",\"data\":[");
            boolean first = true;
            for (JenisSeleksi js : jsList) {
                if (!first) json.append(",");
                json.append("{\"id\":\"").append(js.getId()).append("\",\"nama\":\"").append(escapeJson(js.getNama())).append("\"}");
                first = false;
            }
            json.append("]}");
        }

        // =====================================================================
        // 4. JENIS SEKOLAH
        // =====================================================================
        else if ("jenis_sekolah".equals(action)) {
            List<Object[]> results = new ArrayList<Object[]>();
            boolean fetchAll = false;

            if (gel == null) {
                fetchAll = true;
            } else {
                List<Long> paketsIds = sessionLocal.createCriteria(PaketPunyaGelombangPendaftaran.class)
                    .add(Restrictions.eq("gelombangPendaftaran", gel))
                    .add(Restrictions.isNotNull("paket"))
                    .setProjection(Projections.groupProperty("paket.id")).list();
                
                if (paketsIds.isEmpty()) {
                    fetchAll = true;
                } else {
                    results = sessionLocal.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
                        .add(Restrictions.in("paket.id", paketsIds))
                        .createAlias("jurusanSekolahMahasiswaBaru", "jur")
                        .createAlias("jur.jenisSekolahMahasiswaBaru", "jen")
                        .add(Restrictions.or(Restrictions.isNull("jen.aktif"), Restrictions.eq("jen.aktif", true)))
                        .setProjection(Projections.distinct(Projections.projectionList()
                            .add(Projections.property("jen.id"))
                            .add(Projections.property("jen.nama"))))
                        .addOrder(Order.asc("jen.nama"))
                        .list();
                }
            }

            if (fetchAll || results.isEmpty()) {
                results = sessionLocal.createCriteria(JenisSekolahMahasiswaBaru.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama")))
                    .addOrder(Order.asc("nama")).list();
            }

            json.append("{\"status\":\"success\",\"data\":[");
            for (int i = 0; i < results.size(); i++) {
                Object[] row = results.get(i);
                if (i > 0) json.append(",");
                json.append("{\"id\":\"").append(row[0]).append("\",\"nama\":\"").append(escapeJson(row[1] != null ? row[1].toString() : "")).append("\"}");
            }
            json.append("]}");
        }

        // =====================================================================
        // 5. JURUSAN SEKOLAH 
        // =====================================================================
        else if ("jurusan_sekolah".equals(action)) {
            List<Object[]> results = new ArrayList<Object[]>();
            
            if (jenisSekolahId != null) {
                boolean fetchAll = false;
                if (gel == null) {
                    fetchAll = true;
                } else {
                    List<Long> paketsIds = sessionLocal.createCriteria(PaketPunyaGelombangPendaftaran.class)
                        .add(Restrictions.eq("gelombangPendaftaran", gel))
                        .add(Restrictions.isNotNull("paket"))
                        .setProjection(Projections.groupProperty("paket.id")).list();

                    if (paketsIds.isEmpty()) {
                        fetchAll = true;
                    } else {
                        results = sessionLocal.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
                            .add(Restrictions.in("paket.id", paketsIds))
                            .createAlias("jurusanSekolahMahasiswaBaru", "jur")
                            .add(Restrictions.eq("jur.jenisSekolahMahasiswaBaru.id", jenisSekolahId))
                            .add(Restrictions.or(Restrictions.isNull("jur.aktif"), Restrictions.eq("jur.aktif", true)))
                            .setProjection(Projections.distinct(Projections.projectionList()
                                .add(Projections.property("jur.id"))
                                .add(Projections.property("jur.nama"))))
                            .addOrder(Order.asc("jur.nama"))
                            .list();
                    }
                }

                if (fetchAll || results.isEmpty()) {
                    results = sessionLocal.createCriteria(JurusanSekolahMahasiswaBaru.class)
                        .add(Restrictions.eq("jenisSekolahMahasiswaBaru.id", jenisSekolahId))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("nama")))
                        .addOrder(Order.asc("nama")).list();
                }
            }

            json.append("{\"status\":\"success\",\"data\":[");
            for (int i = 0; i < results.size(); i++) {
                Object[] row = results.get(i);
                if (i > 0) json.append(",");
                json.append("{\"id\":\"").append(row[0]).append("\",\"nama\":\"").append(escapeJson(row[1] != null ? row[1].toString() : "")).append("\"}");
            }
            json.append("]}");
        }

        // =====================================================================
        // 6. PAKET 
        // =====================================================================
        else if ("paket".equals(action)) {
            List<Object[]> results = new ArrayList<Object[]>();
            
            if (gel != null) {
                List<PaketPunyaGelombangPendaftaran> ppgpList = sessionLocal.createCriteria(PaketPunyaGelombangPendaftaran.class)
                    .add(Restrictions.isNotNull("paket")).add(Restrictions.eq("gelombangPendaftaran", gel)).list();
                
                if (ppgpList.size() == 1) {
                    Paket tunggal = ppgpList.get(0).getPaket();
                    if (tunggal.getAktif() == null || tunggal.getAktif()) {
                        results.add(new Object[]{tunggal.getId(), tunggal.getNama(), tunggal.getJumlahProdiYgBolehDiambil()});
                    }
                } 
                else if (jurId != null) {
                    List<Long> paketsSemuaIds = sessionLocal.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
                        .createAlias("paket", "p")
                        .add(Restrictions.or(Restrictions.isNull("p.aktif"), Restrictions.eq("p.aktif", true)))
                        .add(Restrictions.eq("jurusanSekolahMahasiswaBaru.id", jurId))
                        .setProjection(Projections.groupProperty("p.id")).list();
                        
                    if (!paketsSemuaIds.isEmpty()) {
                        results = sessionLocal.createCriteria(PaketPunyaGelombangPendaftaran.class)
                            .add(Restrictions.in("paket.id", paketsSemuaIds)).add(Restrictions.eq("gelombangPendaftaran", gel))
                            .createAlias("paket", "p").add(Restrictions.or(Restrictions.isNull("p.aktif"), Restrictions.eq("p.aktif", true)))
                            .add(Restrictions.eq("p.bisaDipilihSemuaGelombang", false))
                            .setProjection(Projections.distinct(Projections.projectionList().add(Projections.property("p.id")).add(Projections.property("p.nama")).add(Projections.property("p.jumlahProdiYgBolehDiambil"))))
                            .addOrder(Order.asc("p.nama")).list();
                            
                        if (results.isEmpty()) {
                            results = sessionLocal.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
                                .createAlias("paket", "p").add(Restrictions.eq("jurusanSekolahMahasiswaBaru.id", jurId))
                                .add(Restrictions.or(Restrictions.isNull("p.aktif"), Restrictions.eq("p.aktif", true)))
                                .add(Restrictions.or(Restrictions.eq("p.bisaDipilihSemuaGelombang", true), Restrictions.isNull("p.bisaDipilihSemuaGelombang")))
                                .setProjection(Projections.distinct(Projections.projectionList().add(Projections.property("p.id")).add(Projections.property("p.nama")).add(Projections.property("p.jumlahProdiYgBolehDiambil"))))
                                .addOrder(Order.asc("p.nama")).list();
                        }
                    }
                }
            }

            json.append("{\"status\":\"success\",\"data\":[");
            for (int i = 0; i < results.size(); i++) {
                Object[] row = results.get(i);
                if (i > 0) json.append(",");
                json.append("{\"id\":\"").append(row[0]).append("\",\"nama\":\"").append(escapeJson(row[1] != null ? row[1].toString() : "")).append("\",\"maxProdi\":\"").append(row[2] != null ? row[2] : 0).append("\"}");
            }
            json.append("]}");
        }

        // =====================================================================
        // 7. PROGRAM & PRODI 
        // =====================================================================
        else if ("program_prodi".equals(action)) {
            json.append("{\"status\":\"success\",\"programs\":[");
            
            if (pktId != null) {
                // Program
                List<Object[]> programs = sessionLocal.createCriteria(PaketPunyaProgram.class)
                    .add(Restrictions.eq("paket.id", pktId))
                    .createAlias("program", "prog")
                    .setProjection(Projections.distinct(Projections.projectionList().add(Projections.property("prog.nama")).add(Projections.property("prog.namaBaru"))))
                    .list();
                    
                if (programs.isEmpty()) {
                    programs = sessionLocal.createCriteria(Program.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .setProjection(Projections.projectionList().add(Projections.property("nama")).add(Projections.property("namaBaru"))).list();
                }
                
                for (int i = 0; i < programs.size(); i++) {
                    Object[] p = programs.get(i);
                    if (i > 0) json.append(",");
                    json.append("{\"nama\":\"").append(escapeJson(p[0] != null ? p[0].toString() : "")).append("\",\"namaBaru\":\"").append(escapeJson(p[1] != null ? p[1].toString() : p[0] != null ? p[0].toString() : "")).append("\"}");
                }
                
                json.append("],\"prodis\":[");
                
                // Prodi
                List<Object[]> prodis = sessionLocal.createCriteria(PaketJurusanPmb.class)
                    .add(Restrictions.eq("paket.id", pktId))
                    .createAlias("jurusan", "jur").createAlias("jur.jenjang", "jen", Criteria.LEFT_JOIN).createAlias("jur.fakultas", "fak", Criteria.LEFT_JOIN)
                    .add(Restrictions.or(Restrictions.isNull("jur.aktif"), Restrictions.eq("jur.aktif", true)))
                    .setProjection(Projections.projectionList()
                        .add(Projections.property("jur.id"))               // 0
                        .add(Projections.property("jur.nama"))             // 1
                        .add(Projections.property("fak.nama"))             // 2
                        .add(Projections.property("jen.nama"))             // 3
                        .add(Projections.property("kelamin"))              // 4
                        .add(Projections.property("pilihan1"))             // 5
                        .add(Projections.property("pilihan2"))             // 6
                        .add(Projections.property("pilihan3"))             // 7
                        .add(Projections.property("pilihan4"))             // 8
                        .add(Projections.property("pilihan5"))             // 9
                    ).list();

                for (int i = 0; i < prodis.size(); i++) {
                    Object[] p = prodis.get(i);
                    if (i > 0) json.append(",");
                    json.append("{")
                        .append("\"id\":\"").append(p[0]).append("\",")
                        .append("\"nama\":\"").append(escapeJson(p[1] != null ? p[1].toString() : "")).append("\",")
                        .append("\"fakultas\":\"").append(p[2] != null ? escapeJson(p[2].toString()) : "").append("\",")
                        .append("\"jenjang\":\"").append(p[3] != null ? escapeJson(p[3].toString()) : "").append("\",")
                        .append("\"kelamin\":\"").append(p[4] != null ? escapeJson(p[4].toString()) : "Semua").append("\",")
                        .append("\"pilihan1\":").append(Boolean.TRUE.equals(p[5]) ? "true" : "false").append(",")
                        .append("\"pilihan2\":").append(Boolean.TRUE.equals(p[6]) ? "true" : "false").append(",")
                        .append("\"pilihan3\":").append(Boolean.TRUE.equals(p[7]) ? "true" : "false").append(",")
                        .append("\"pilihan4\":").append(Boolean.TRUE.equals(p[8]) ? "true" : "false").append(",")
                        .append("\"pilihan5\":").append(Boolean.TRUE.equals(p[9]) ? "true" : "false")
                        .append("}");
                }
            } else {
                json.append("],\"prodis\":[");
            }
            json.append("]}");
        } 
        
        // =====================================================================
        // 8. PENCARIAN DINAMIS: WILAYAH (GROUP BY & PRIORITAS FEEDER)
        // =====================================================================
        else if ("wilayah".equals(action)) {
            String keyword = request.getParameter("keyword");
            List<Object[]> finalList = new ArrayList<Object[]>();
            
            if(keyword != null && keyword.length() >= 3) {
                // Ambil lebih dari batas awal, beserta kolom feeder
                List<Object[]> rawList = sessionLocal.createQuery(
                    "select w.id, w.nama, w.feeder from " + Wilayah.class.getName() + 
                    " w where w.level = '3' and lower(w.nama) like :nama order by w.nama asc")
                    .setParameter("nama", "%" + keyword.toLowerCase() + "%")
                    .setMaxResults(200) 
                    .list();
                    
                // Logika Prioritas & Unik menggunakan Java Map
                Map<String, Object[]> uniqueMap = new LinkedHashMap<String, Object[]>();
                
                for (Object[] row : rawList) {
                    String nama = row[1] != null ? row[1].toString().trim() : "";
                    String feeder = row[2] != null ? row[2].toString().trim() : "";
                    boolean hasFeeder = !feeder.isEmpty();

                    if (!uniqueMap.containsKey(nama)) {
                        uniqueMap.put(nama, row); // Belum ada nama ini, masukkan
                    } else {
                        Object[] existingRow = uniqueMap.get(nama);
                        String existingFeeder = existingRow[2] != null ? existingRow[2].toString().trim() : "";
                        boolean existingHasFeeder = !existingFeeder.isEmpty();

                        // Jika data baru PUNYA feeder, sedangkan data lama TIDAK PUNYA feeder -> Timpa (Override)
                        if (hasFeeder && !existingHasFeeder) {
                            uniqueMap.put(nama, row);
                        }
                    }
                }
                
                finalList = new ArrayList<Object[]>(uniqueMap.values());
                if (finalList.size() > 50) {
                    finalList = finalList.subList(0, 50); // Batasi hasil final maksimal 50
                }
            }
            
            json.append("{\"status\":\"success\",\"data\":[");
            for(int i=0; i<finalList.size(); i++) { 
                if(i>0) json.append(","); 
                json.append("{\"id\":\"").append(finalList.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(finalList.get(i)[1] != null ? finalList.get(i)[1].toString() : "")).append("\"}"); 
            }
            json.append("]}");
        }
        
        // =====================================================================
        // 9. PENCARIAN DINAMIS: NAMA SEKOLAH ASAL
        // =====================================================================
        else if ("sekolah_asal".equals(action)) {
            String keyword = request.getParameter("keyword");
            List<Object[]> list = new ArrayList<Object[]>();
            if(keyword != null && keyword.length() >= 3) {
                list = sessionLocal.createQuery(
                    "select s.id, s.nama from " + NamaSekolahAsal.class.getName() + 
                    " s where (s.aktif = true or s.aktif is null) and lower(s.nama) like :nama order by s.nama asc")
                    .setParameter("nama", "%" + keyword.toLowerCase() + "%")
                    .setMaxResults(50)
                    .list();
            }
            json.append("{\"status\":\"success\",\"data\":[");
            for(int i=0; i<list.size(); i++) { 
                if(i>0) json.append(","); 
                json.append("{\"id\":\"").append(list.get(i)[0]).append("\",\"nama\":\"").append(escapeJson(list.get(i)[1] != null ? list.get(i)[1].toString() : "")).append("\"}"); 
            }
            json.append("]}");
        }
        
        else if ("get_kota_propinsi_by_kecamatan".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null && !idStr.isEmpty()) {
                ais.database.model.Wilayah kecamatan = (ais.database.model.Wilayah) sessionLocal.get(ais.database.model.Wilayah.class, Long.parseLong(idStr));
                
                if (kecamatan != null && kecamatan.getWilayahInduk() != null) {
                    ais.database.model.Wilayah wilayahKab = kecamatan.getWilayahInduk();
                    ais.database.model.Wilayah wilayahProp = wilayahKab.getWilayahInduk();
                    
                    ais.database.model.Propinsi prop = null;
                    ais.database.model.Kota kota = null;
                    
                    if (wilayahProp != null) {
                        prop = findOrCreatePropinsi(sessionLocal, wilayahProp.getNama());
                    }
                    if (prop != null) {
                        kota = findBestMatchKota(sessionLocal, prop, wilayahKab.getNama());
                    }
                    
                    json.append("{\"status\":\"success\"");
                    if (prop != null) {
                        json.append(",\"propinsi_id\":\"").append(prop.getId()).append("\",\"propinsi_nama\":\"").append(escapeJson(prop.getNama())).append("\"");
                    }
                    if (kota != null) {
                        json.append(",\"kota_id\":\"").append(kota.getId()).append("\",\"kota_nama\":\"").append(escapeJson(kota.getNama())).append("\"");
                    }
                    json.append("}");
                } else {
                    json.append("{\"status\":\"error\", \"message\":\"Data wilayah induk tidak lengkap\"}");
                }
            } else {
                json.append("{\"status\":\"error\", \"message\":\"ID Kecamatan kosong\"}");
            }
        }
        
        // =====================================================================
        // PENCARIAN KABUPATEN/KOTA (level=2) — untuk form tambah kecamatan
        // =====================================================================
        else if ("wilayah_kab".equals(action)) {
            String keyword = request.getParameter("keyword");
            java.util.List<Object[]> list = new java.util.ArrayList<Object[]>();
            if (keyword != null && keyword.trim().length() >= 2) {
                list = sessionLocal.createQuery(
                    "select w.id, w.nama from " + ais.database.model.Wilayah.class.getName() +
                    " w where w.level = '2' and lower(w.nama) like :nama order by w.nama asc")
                    .setParameter("nama", "%" + keyword.trim().toLowerCase() + "%")
                    .setMaxResults(30).list();
            }
            json.append("{\"status\":\"success\",\"data\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) json.append(",");
                json.append("{\"id\":\"").append(list.get(i)[0]).append("\",\"nama\":\"")
                    .append(escapeJson(list.get(i)[1] != null ? list.get(i)[1].toString() : "")).append("\"}");
            }
            json.append("]}");
        }

        // =====================================================================
        // TAMBAH KECAMATAN BARU — aktif hanya jika konfigurasi pmb_tambah_kecamatan_aktif
        // =====================================================================
        else if ("tambah_kecamatan".equals(action)) {
            if (!ais.common.Common.bolehKonfigurasi("pmb_tambah_kecamatan_aktif")) {
                json.append("{\"status\":\"error\",\"message\":\"Fitur tidak aktif\"}");
            } else {
                String namaKec = request.getParameter("nama");
                String kabIdStr = request.getParameter("kab_id");
                if (namaKec == null || namaKec.trim().isEmpty()) {
                    json.append("{\"status\":\"error\",\"message\":\"Nama kecamatan tidak boleh kosong\"}");
                } else {
                    ais.database.model.Wilayah kec = new ais.database.model.Wilayah();
                    kec.setNama(namaKec.trim());
                    kec.setLevel("3");
                    if (kabIdStr != null && !kabIdStr.trim().isEmpty()) {
                        try {
                            ais.database.model.Wilayah kab = (ais.database.model.Wilayah)
                                sessionLocal.get(ais.database.model.Wilayah.class, Long.parseLong(kabIdStr.trim()));
                            if (kab != null) kec.setWilayahInduk(kab);
                        } catch (Exception eKab) { /* abaikan kab_id tidak valid */ }
                    }
                    sessionLocal.beginTransaction();
                    Long newId = (Long) sessionLocal.save(kec);
                    sessionLocal.getTransaction().commit();
                    json.append("{\"status\":\"success\",\"id\":\"").append(newId)
                        .append("\",\"nama\":\"").append(escapeJson(namaKec.trim())).append("\"}");
                }
            }
        }

        else {
            json.append("{\"status\":\"error\", \"message\":\"Action not found\"}");
        }

        out.print(json.toString());

    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_service.jsp:588");
        out.print("{\"status\":\"error\", \"message\":\"" + escapeJson(e.getMessage()) + "\"}");
    } finally {
        if (sessionLocal != null) {
            try { sessionLocal.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_service.jsp:592");}
            try { sessionLocal.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_service.jsp:593");}
            try { sessionLocal.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_service.jsp:594");}
        }
    }
%>