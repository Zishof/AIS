<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Paket"%>
<%@page import="ais.database.model.Program"%>
<%@page import="ais.database.model.GelombangPendaftaran"%>
<%@page import="ais.database.model.JenisSeleksi"%>
<%@page import="ais.database.model.JurusanSekolahMahasiswaBaru"%>
<%@page import="ais.database.model.PaketJurusanPmb"%>
<%@page import="ais.database.model.PaketPunyaProgram"%>
<%@page import="ais.database.model.PilihanPaketPerJurusanMhsBaru"%>
<%@page import="ais.database.model.PaketPunyaGelombangPendaftaran"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
    // Helper untuk membersihkan teks agar aman di dalam JSON (Java 1.7)
    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // Helper untuk merakit Array JSON dari Koleksi String dengan tanda kutip
    private String joinQuoted(Collection<String> elements) {
        if (elements == null || elements.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : elements) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(s)).append("\"");
            i++;
        }
        return sb.toString();
    }
%>

<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
    Date currentDate = WaktuUtil.getDate(); // Tanggal saat ini untuk filter aktif
    
    String ta = request.getParameter("ta");
    boolean filterTa = (ta != null && !ta.trim().isEmpty());

    StringBuilder json = new StringBuilder();
    Session sessionLocal = null;

    try {
        sessionLocal = HibernateUtil.openSession();
        
        // 1. Ambil Semua Jurusan yang aktif
        List<Jurusan> listJurusan = ConstantValues.simpleList(sessionLocal.createCriteria(Jurusan.class)
                .createAlias("fakultas", "fakultas")
                .add(Restrictions.or(Restrictions.isNull("fakultas.aktif"), Restrictions.eq("fakultas.aktif", true)))
                .add(pt != null && pt.getId() != null ? Restrictions.eq("fakultas.perguruanTinggi", pt) : Restrictions.sqlRestriction("true"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("nama")), Jurusan.class, true); // Menggunakan true [cite: 739]

        // 2. Ambil Paket Jurusan (Pilihan 1-5 aktif)
        List<PaketJurusanPmb> listPjp = ConstantValues.simpleList(sessionLocal.createCriteria(PaketJurusanPmb.class)
        		.createAlias("jurusan", "jurusan")
                .add(Restrictions.or(Restrictions.isNull("jurusan.aktif"), Restrictions.eq("jurusan.aktif", true)))
                .createAlias("jurusan.fakultas", "fakultas")
                .add(Restrictions.or(Restrictions.isNull("fakultas.aktif"), Restrictions.eq("fakultas.aktif", true)))
                .add(Restrictions.eq("pilihan1", true)).add(Restrictions.eq("pilihan2", true))
                .add(Restrictions.eq("pilihan3", true)).add(Restrictions.eq("pilihan4", true))
                .add(Restrictions.eq("pilihan5", true))
                .createAlias("paket", "paket").add(Restrictions.eq("paket.aktif", true))
                .add(Restrictions.isNotNull("jurusan")), PaketJurusanPmb.class, true); // Menggunakan true [cite: 740, 741]

        Map<Long, List<Paket>> mapJurusanToPakets = new HashMap<Long, List<Paket>>();
        for (PaketJurusanPmb pjp : listPjp) {
            Long jurId = pjp.getJurusan().getId();
            if (!mapJurusanToPakets.containsKey(jurId)) mapJurusanToPakets.put(jurId, new ArrayList<Paket>());
            mapJurusanToPakets.get(jurId).add(pjp.getPaket());
        }

        // 3. Ambil Program studi per paket
        List<PaketPunyaProgram> listPpp = ConstantValues.simpleList(sessionLocal.createCriteria(PaketPunyaProgram.class)
                .createAlias("paket", "paket").add(Restrictions.eq("paket.aktif", true)), PaketPunyaProgram.class, true); // Menggunakan true [cite: 743]

        Map<Long, List<Program>> mapPaketToPrograms = new HashMap<Long, List<Program>>();
        for (PaketPunyaProgram ppp : listPpp) {
            Long pakId = ppp.getPaket().getId();
            if (!mapPaketToPrograms.containsKey(pakId)) mapPaketToPrograms.put(pakId, new ArrayList<Program>());
            mapPaketToPrograms.get(pakId).add(ppp.getProgram());
        }

        // 4. Ambil Gelombang Pendaftaran yang sedang dibuka (Join dengan Paket)
        Criteria critPpg = sessionLocal.createCriteria(PaketPunyaGelombangPendaftaran.class)
                .createAlias("paket", "paket").add(Restrictions.eq("paket.aktif", true))
                .createAlias("gelombangPendaftaran", "gel"); // Alias ke property gelombangPendaftaran
        
        if (filterTa) critPpg.add(Restrictions.eq("gel.tahunAkademik", ta));
        
        // Filter Tanggal Aktif: mulai <= hari ini <= sampai
        critPpg.add(Restrictions.le("gel.mulai", currentDate));
        critPpg.add(Restrictions.ge("gel.sampai", currentDate));
        critPpg.add(Restrictions.or(Restrictions.isNull("gel.aktif"), Restrictions.eq("gel.aktif", true)));
        
        List<PaketPunyaGelombangPendaftaran> listPpg = ConstantValues.simpleList(critPpg, PaketPunyaGelombangPendaftaran.class, true); // Menggunakan true [cite: 746]

        Map<Long, List<GelombangPendaftaran>> mapPaketToGelombang = new HashMap<Long, List<GelombangPendaftaran>>();
        Set<Long> gelombangIdsInPaket = new HashSet<Long>();
        for (PaketPunyaGelombangPendaftaran ppg : listPpg) {
            Long pakId = ppg.getPaket().getId();
            if (!mapPaketToGelombang.containsKey(pakId)) mapPaketToGelombang.put(pakId, new ArrayList<GelombangPendaftaran>());
            mapPaketToGelombang.get(pakId).add(ppg.getGelombangPendaftaran());
            gelombangIdsInPaket.add(ppg.getGelombangPendaftaran().getId());
        }

        // 5. Ambil Gelombang Mandiri/Bebas yang sedang dibuka
        Criteria critGb = sessionLocal.createCriteria(GelombangPendaftaran.class);
        if (filterTa) critGb.add(Restrictions.eq("tahunAkademik", ta));
        
        // Filter Tanggal Aktif
        critGb.add(Restrictions.le("mulai", currentDate));
        critGb.add(Restrictions.ge("sampai", currentDate));
        critGb.add(gelombangIdsInPaket.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.not(Restrictions.in("id", gelombangIdsInPaket)))
              .add(Restrictions.or(Restrictions.eq("bisaDipilihPendaftarOnline", true), Restrictions.isNull("bisaDipilihPendaftarOnline")))
              .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
              
        List<GelombangPendaftaran> gelombangBebas = ConstantValues.simpleList(critGb, GelombangPendaftaran.class, true); // Menggunakan true [cite: 751]

        // 6. Ambil Persyaratan Sekolah Asal
        List<PilihanPaketPerJurusanMhsBaru> listPjs = ConstantValues.simpleList(sessionLocal.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
                .createAlias("paket", "paket").add(Restrictions.eq("paket.aktif", true)), PilihanPaketPerJurusanMhsBaru.class, true); // Menggunakan true [cite: 752]

        Map<Long, List<JurusanSekolahMahasiswaBaru>> mapPaketToSekolah = new HashMap<Long, List<JurusanSekolahMahasiswaBaru>>();
        for (PilihanPaketPerJurusanMhsBaru pjs : listPjs) {
            Long pakId = pjs.getPaket().getId();
            if (!mapPaketToSekolah.containsKey(pakId)) mapPaketToSekolah.put(pakId, new ArrayList<JurusanSekolahMahasiswaBaru>());
            mapPaketToSekolah.get(pakId).add(pjs.getJurusanSekolahMahasiswaBaru());
        }

        // 7. Konstruksi JSON Response
        json.append("{\"status\":\"success\", \"fakultasList\":[");
        Long lastFakId = null; boolean isFirstF = true;

        for (Jurusan j : listJurusan) {
            Fakultas f = j.getFakultas();
            if (lastFakId == null || !lastFakId.equals(f.getId())) {
                if (!isFirstF) json.append("]},");
                json.append("{\"id\":").append(f.getId()).append(",\"nama\":\"").append(escapeJson(f.getNama())).append("\",\"jurusans\":[");
                lastFakId = f.getId(); isFirstF = false;
            } else json.append(",");
            
            List<Paket> pakets = mapJurusanToPakets.containsKey(j.getId()) ? mapJurusanToPakets.get(j.getId()) : new ArrayList<Paket>();
            Set<String> nP = new LinkedHashSet<String>(); Set<String> nG = new LinkedHashSet<String>(); Set<String> nS = new LinkedHashSet<String>(); Set<String> nPr = new LinkedHashSet<String>(); Set<String> nSk = new LinkedHashSet<String>(); 
            StringBuilder btns = new StringBuilder("[");
            
            int bCount = 0;
            for (Paket p : pakets) {
                nP.add(p.getNama());
                if (mapPaketToGelombang.containsKey(p.getId())) {
                    for (GelombangPendaftaran g : mapPaketToGelombang.get(p.getId())) {
                        nG.add(g.getNama() + " (" + g.getTahunAkademik() + ")");
                        if (g.ambilJenisSeleksi() != null) for (JenisSeleksi js : g.ambilJenisSeleksi()) nS.add(js.getNama());
                        btns.append(bCount++ > 0 ? "," : "").append("{\"gelId\":").append(g.getId()).append(",\"pktId\":").append(p.getId()).append(",\"label\":\"Daftar ").append(escapeJson(g.getNama())).append("\"}");
                    }
                }
                if (mapPaketToPrograms.containsKey(p.getId())) for (Program pr : mapPaketToPrograms.get(p.getId())) nPr.add(pr.getNamaBaru() != null ? pr.getNamaBaru() : pr.getNama());
                if (mapPaketToSekolah.containsKey(p.getId())) for (JurusanSekolahMahasiswaBaru sk : mapPaketToSekolah.get(p.getId())) nSk.add(sk.getJenisSekolahMahasiswaBaru().getNama() + " (" + sk.getNama() + ")");
            }
            btns.append("]");

            json.append("{")
                .append("\"id\":").append(j.getId()).append(",")
                .append("\"nama\":\"").append(escapeJson(j.getNama())).append("\",")
                .append("\"jenjang\":\"").append(escapeJson(j.getJenjang()!=null?j.getJenjang().getNama():"")).append("\",")
                .append("\"deskripsi\":\"").append(escapeJson(j.getDeskripsi())).append("\",")
                .append("\"pakets\":[").append(joinQuoted(nP)).append("],")
                .append("\"gelombangs\":[").append(joinQuoted(nG)).append("],")
                .append("\"seleksis\":[").append(joinQuoted(nS)).append("],")
                .append("\"programs\":[").append(joinQuoted(nPr)).append("],")
                .append("\"sekolahs\":[").append(joinQuoted(nSk)).append("],")
                .append("\"buttons\":").append(btns)
                .append("}");
        }
        if (!isFirstF) json.append("]}");
        json.append("]}");
        out.print(json.toString());
    } catch (Exception e) { 
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_pilihan_prodi.jsp:196");
        out.print("{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"); 
    }
    finally {
        if (sessionLocal != null) {
            try { sessionLocal.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pilihan_prodi.jsp:201");}
            try { sessionLocal.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pilihan_prodi.jsp:202");}
            try { sessionLocal.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pilihan_prodi.jsp:203");}
        }
    }
%>