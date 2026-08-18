<%@page session="false"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.sop.Sop"%>
<%@page import="ais.database.model.sop.DisposisiSop"%>
<%@page import="ais.database.model.sop.DisposisiAlurSop"%>
<%@page import="ais.database.model.sop.AlurSop"%>
<%@page import="ais.database.model.sop.JenisSop"%>
<%@page import="ais.database.model.sop.DokumenAlurSop"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.action.master.sop.helper.ProsesDisposisiSopService"%>
<%!
    static String s(Object o){ return o==null?"":o.toString(); }
    // Nama pengguna untuk ditampilkan di kartu langkah; aman terhadap null.
    static String namaUser(Tbmuser u){
        if (u==null) return "";
        String n = u.getUserNama();
        if (n!=null && !n.trim().isEmpty()) return n.trim();
        return s(u.getUserId());
    }
    // Status keseluruhan satu pengajuan SOP (sama dengan listPengajuan).
    static String statusPengajuan(DisposisiSop d){
        if (d.getDisposisiEnd()!=null) return "selesai";
        if (d.getDisposisiSetuju()!=null) return "disetujui";
        return "menunggu";
    }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status","01"); result.put("message", Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali."));
        out.print(result.toString()); return;
    }
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    SimpleDateFormat dfOut = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    if ("setujuiBulk".equals(aksi)) {
        // ============================================================================
        // SETUJUI MASSAL (bulk): menyetujui banyak pengajuan yang MENUNGGU DISPOSISI
        // pengguna aktif sekaligus. Otorisasi inheren: memakai criteriaMenungguSaya
        // sehingga HANYA langkah yang memang menunggu aktor = pengguna ini yang diproses
        // (aman untuk pengurus/pengawas/pembina yang antriannya banyak).
        // - ids : daftar disposisiSopId (csv) yang dicentang; KOSONG = SEMUA yang menunggu saya.
        // - keterangan : catatan bersama (opsional) untuk semua yang disetujui.
        // Item yang butuh PILIH RUTE manual (>1 opsi & wajib / berupa pilihan) atau CATATAN
        // WAJIB sedang kosong akan DILEWATI (diproses satu per satu oleh pengguna).
        // Reuse penuh ProsesDisposisiSopService.prosesLangkah (jalur setujui yang teruji).
        // ============================================================================
        String idsStr = request.getParameter("ids");
        String ketBulk = request.getParameter("keterangan");
        if (ketBulk == null) ketBulk = "";
        java.util.Set<Long> pilih = new java.util.HashSet<Long>();
        if (idsStr != null && !idsStr.trim().isEmpty()) {
            for (String p : idsStr.split(",")) { try { pilih.add(Long.valueOf(p.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:70");} }
        }
        // ---- PASS 1: kumpulkan keputusan (hindari interleave lazy-load dgn mutasi session) ----
        java.util.List<Object[]> tugas = new java.util.ArrayList<Object[]>(); // {dsId, stepId, alurId, List<Long> rute}
        int lewat = 0;
        java.util.List<?> pendingList;
        try {
            pendingList = ais.action.master.sop.helper.PengajuanAndaSopUtil
                    .criteriaMenungguSaya(session, tbmuser, false).list();
        } catch (Exception e) { pendingList = new java.util.ArrayList<Object>(); }
        for (Object o : pendingList) {
            DisposisiAlurSop step = (DisposisiAlurSop) o;
            if (step == null || step.getDisposisiSop() == null || step.getAlurSop() == null) continue;
            Long dsId = step.getDisposisiSop().getId();
            if (dsId == null) continue;
            if (!pilih.isEmpty() && !pilih.contains(dsId)) continue; // hanya yang dicentang
            AlurSop a = step.getAlurSop();
            if (Boolean.TRUE.equals(a.getCatatanWajibDiisi()) && ketBulk.trim().isEmpty()) { lewat++; continue; }
            java.util.List<Long> rute = new java.util.ArrayList<Long>();
            java.util.List opsi = null;
            try { opsi = a.ambilAlurSetelahnya(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:90");}
            boolean ruteOpsional = Boolean.TRUE.equals(a.getAlurSetelahnyaTidakWajib());
            boolean berupaPilihan = Boolean.TRUE.equals(a.getAlurSetelahnyaBerupaPilihan());
            int nOpsi = (opsi == null ? 0 : opsi.size());
            if (nOpsi > 0) {
                if (!ruteOpsional && (nOpsi > 1 || berupaPilihan)) { lewat++; continue; } // butuh pilih manual
                for (Object oo : opsi) { AlurSop na = (AlurSop) oo; if (na != null && na.getId() != null) rute.add(na.getId()); }
            }
            tugas.add(new Object[]{ dsId, step.getId(), a.getId(), rute });
        }
        // ---- PASS 2: proses setujui (reuse prosesLangkah, masing-masing session sendiri) ----
        int ok = 0, gagal = 0;
        for (Object[] t : tugas) {
            try {
                ProsesDisposisiSopService.Hasil h = ProsesDisposisiSopService.prosesLangkah(
                        tbmuser, (Long) t[0], (Long) t[1], (Long) t[2], tbmuser.getUserId(),
                        new Date(), null, ketBulk, true, false, (java.util.List<Long>) t[3]);
                if (h != null && h.ok) ok++; else gagal++;
            } catch (Exception e) { gagal++; }
        }
        result.put("status", "00");
        result.put("disetujui", ok);
        result.put("dilewati", lewat);
        result.put("gagal", gagal);
        result.put("message", "Disetujui " + ok + " pengajuan"
                + (lewat > 0 ? ", dilewati " + lewat + " (perlu pilih rute / catatan manual)" : "")
                + (gagal > 0 ? ", gagal " + gagal : "") + ".");
        out.print(result.toString());
        return;

    } else if ("listMenungguSaya".equals(aksi)) {
        // Daftar pengajuan yang MENUNGGU DISPOSISI pengguna aktif (untuk panel Setujui Massal).
        JSONArray arr = new JSONArray();
        java.util.List<?> pend;
        try {
            pend = ais.action.master.sop.helper.PengajuanAndaSopUtil
                    .criteriaMenungguSaya(session, tbmuser, false).addOrder(Order.desc("id")).list();
        } catch (Exception e) { pend = new java.util.ArrayList<Object>(); }
        for (Object o : pend) {
            DisposisiAlurSop step = (DisposisiAlurSop) o;
            if (step == null || step.getDisposisiSop() == null || step.getAlurSop() == null) continue;
            DisposisiSop ds = step.getDisposisiSop();
            AlurSop a = step.getAlurSop();
            JSONObject j = new JSONObject();
            j.put("id", ds.getId());
            String namaSop = "";
            try { if (ds.getSop() != null) namaSop = s(ds.getSop().getNama()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:136");}
            if (namaSop.isEmpty()) { try { if (a.getSop() != null) namaSop = s(a.getSop().getNama()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:137");} }
            j.put("sop", namaSop);
            j.put("langkah", s(a.getNama()));
            j.put("catatan", s(step.getKeterangan()));
            try { j.put("waktu", step.getWaktu() == null ? "" : dfOut.format(step.getWaktu())); } catch (Exception e) { j.put("waktu", ""); }
            boolean manual = false; // butuh pilih rute / catatan manual -> tidak bisa bulk
            try {
                java.util.List opsi = a.ambilAlurSetelahnya();
                boolean opsional = Boolean.TRUE.equals(a.getAlurSetelahnyaTidakWajib());
                boolean pilihan = Boolean.TRUE.equals(a.getAlurSetelahnyaBerupaPilihan());
                int n = opsi == null ? 0 : opsi.size();
                manual = (!opsional && (n > 1 || pilihan)) || Boolean.TRUE.equals(a.getCatatanWajibDiisi());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:149");}
            j.put("manual", manual);
            arr.put(j);
        }
        result.put("status", "00");
        result.put("data", arr);
        out.print(result.toString());
        return;

    } else if ("listSop".equals(aksi)) {
        // Daftar SOP yang bisa diajukan (Pengajuan Baru), per Jenis SOP
        String q = request.getParameter("q");
        Criteria c = session.createCriteria(Sop.class)
                .add(ais.action.master.sop.DisposisiSopAction.createCriterionSop(tbmuser))
                .addOrder(Order.asc("nama")).setMaxResults(500);
        if (q!=null && !q.trim().isEmpty()) c.add(Restrictions.or(
                Restrictions.ilike("nama", q.trim(), MatchMode.ANYWHERE), Restrictions.ilike("kode", q.trim(), MatchMode.ANYWHERE)));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            Sop sop = (Sop) o; JSONObject j = new JSONObject();
            j.put("id", sop.getId()); j.put("kode", s(sop.getKode())); j.put("nama", s(sop.getNama()));
            j.put("versi", s(sop.getVersi()));
            j.put("jenis", sop.getJenisSop()==null?"Lainnya":s(sop.getJenisSop().getNama()));
            arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else if ("listPengajuan".equals(aksi)) {
        // Pengajuan SOP milik user (diajukan oleh saya, langsung atau via langkah start)
        String fStatus = request.getParameter("status"); // semua/menunggu/disetujui/selesai
        Criteria c = session.createCriteria(DisposisiSop.class)
                .createAlias("disposisiStart", "ds", Criteria.LEFT_JOIN)
                .add(Restrictions.or(Restrictions.eq("diajukanOleh", tbmuser), Restrictions.eq("ds.diajukanOleh", tbmuser)))
                .addOrder(Order.desc("id")).setMaxResults(300);
        int cMenunggu=0, cDisetujui=0, cSelesai=0;
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            DisposisiSop d = (DisposisiSop) o;
            String st = d.getDisposisiEnd()!=null ? "selesai" : (d.getDisposisiSetuju()!=null ? "disetujui" : "menunggu");
            if ("selesai".equals(st)) cSelesai++; else if ("disetujui".equals(st)) cDisetujui++; else cMenunggu++;
            if (fStatus!=null && !fStatus.trim().isEmpty() && !"semua".equals(fStatus) && !fStatus.equals(st)) continue;
            JSONObject j = new JSONObject();
            j.put("id", d.getId());
            j.put("sop", d.getSop()==null?"":((d.getSop().getKode()==null?"":s(d.getSop().getKode())+" - ")+s(d.getSop().getNama())));
            j.put("jenis", (d.getSop()!=null && d.getSop().getJenisSop()!=null)?s(d.getSop().getJenisSop().getNama()):"Lainnya");
            j.put("waktu", d.getWaktu()==null?"":dfOut.format(d.getWaktu()));
            j.put("keterangan", s(d.getKeterangan()));
            j.put("status", st);
            arr.put(j);
        }
        result.put("data", arr);
        result.put("countMenunggu", cMenunggu); result.put("countDisetujui", cDisetujui); result.put("countSelesai", cSelesai);
        result.put("countTotal", cMenunggu+cDisetujui+cSelesai);
        result.put("status","00");

    } else if ("timeline".equals(aksi)) {
        // Riwayat alur (timeline) satu pengajuan SOP + flag aturan disposisi (versi native JSP).
        // Aturan (identik dgn ZK TampilanAlurSopAction/DisposisiAlurSopAction):
        //   - bolehEditProses : Proses SOP (rute berikutnya) hanya boleh diubah bila setelahnya==null
        //                       (langkah sedang menunggu, atau langkah berikutnya sudah dihapus/Batal).
        //   - admin (Common.getApakahAdmin) : boleh "Ubah" field lain (keterangan, upload dokumen) di SEMUA langkah.
        boolean isAdmin = Common.getApakahAdmin();
        Long idDs = null;
        try { idDs = Long.valueOf(request.getParameter("id")); } catch (Exception e) { idDs = null; }
        if (idDs == null) { result.put("status","02"); result.put("message","Pengajuan tidak ditemukan."); out.print(result.toString()); return; }
        DisposisiSop ds = (DisposisiSop) session.get(DisposisiSop.class, idDs);
        if (ds == null) { result.put("status","02"); result.put("message","Pengajuan tidak ditemukan."); out.print(result.toString()); return; }

        // Header pengajuan
        JSONObject head = new JSONObject();
        head.put("id", ds.getId());
        head.put("sop", ds.getSop()==null?"":s(ds.getSop().getNama()));
        head.put("kode", (ds.getSop()!=null && ds.getSop().getKode()!=null)?s(ds.getSop().getKode()):"");
        head.put("jenis", (ds.getSop()!=null && ds.getSop().getJenisSop()!=null)?s(ds.getSop().getJenisSop().getNama()):"Lainnya");
        head.put("diajukanOleh", namaUser(ds.getDiajukanOleh()));
        head.put("keterangan", s(ds.getKeterangan()));
        head.put("waktu", ds.getWaktu()==null?"":dfOut.format(ds.getWaktu()));
        head.put("status", statusPengajuan(ds));
        head.put("isAdmin", isAdmin);
        result.put("head", head);

        // Susun langkah: query SEMUA langkah pengajuan (urut id asc) — sama dengan TampilanAlurSopAction.
        // (TIDAK menelusuri setelahnya: langkah START tidak pernah di-set setelahnya; rutenya disimpan
        //  pada "sebelumnya" langkah berikutnya, sehingga walk via setelahnya akan terputus di start.)
        JSONArray steps = new JSONArray();
        List<?> stepList = session.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.isNotNull("alurSop"))
                .add(Restrictions.eq("disposisiSop", ds))
                .addOrder(Order.asc("id")).list();
        // Himpunan id langkah yang SUDAH punya anak (sudah dirutekan ke langkah berikutnya).
        java.util.Set<Long> adaAnak = new java.util.HashSet<Long>();
        for (Object o : stepList) {
            DisposisiAlurSop st = (DisposisiAlurSop) o;
            if (st.getSebelumnya() != null && st.getSebelumnya().getId() != null) adaAnak.add(st.getSebelumnya().getId());
        }
        int urut = 0;
        for (Object o : stepList) {
            DisposisiAlurSop cur = (DisposisiAlurSop) o;
            AlurSop al = cur.getAlurSop();
            // ATURAN: langkah "ujung" (boleh re-route) = BELUM punya anak (belum dirutekan). Robust utk start.
            boolean bolehEditProses = !adaAnak.contains(cur.getId());
            JSONObject j = new JSONObject();
            j.put("id", cur.getId());
            j.put("urut", ++urut);
            j.put("namaAlur", al==null?"(langkah)":s(al.getNama()));
            j.put("aktor", al==null?"":s(al.getAktor()));
            j.put("oleh", namaUser(cur.getDiajukanOleh()));
            j.put("selesai", Boolean.TRUE.equals(cur.getSelesai()));
            j.put("kembali", Boolean.TRUE.equals(cur.getKembali()));
            j.put("catatan", s(cur.getKeterangan()));
            j.put("waktu", cur.getWaktu()==null?"":dfOut.format(cur.getWaktu()));
            j.put("batasWaktu", cur.getWaktuMaksimal()==null?"":dfOut.format(cur.getWaktuMaksimal()));
            j.put("parameter", s(cur.getParameterTambahan()));
            j.put("berupaPilihan", al!=null && Boolean.TRUE.equals(al.getAlurSetelahnyaBerupaPilihan()));
            j.put("isUjung", bolehEditProses);
            // FLAG ATURAN:
            j.put("bolehEditProses", bolehEditProses);
            j.put("bolehUbah", isAdmin || bolehEditProses); // admin: semua langkah; non-admin: hanya ujung
            j.put("bolehUpload", isAdmin);

            // Dokumen lampiran langkah (start vs alur menentukan ref + jenis) — untuk tampil & unduh.
            JSONArray docs = new JSONArray();
            boolean stepStart = al != null && Boolean.TRUE.equals(al.getStart());
            if (al != null && al.getDokumenAlurSops() != null) {
                for (DokumenAlurSop da : al.getDokumenAlurSops()) {
                    if (da == null || !Boolean.TRUE.equals(da.getAktif())) continue;
                    Long refDoc; String jenisDoc;
                    if (stepStart) { refDoc = ds.getId(); jenisDoc = DokumenAlurSop.class.getName() + "_" + da.getId(); }
                    else { refDoc = cur.getId(); jenisDoc = DokumenAlurSop.class.getName() + "_alur_" + da.getId(); }
                    LampiranLain ll = null;
                    try { ll = LampiranLain.ambil(refDoc, jenisDoc); } catch (Exception exDoc) { ais.common.ErrorAuditUtil.record(exDoc, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:279");}
                    JSONObject jd = new JSONObject();
                    jd.put("nama", s(da.getNama()));
                    jd.put("kode", s(da.getKode()));
                    jd.put("wajib", Boolean.TRUE.equals(da.getWajib()));
                    jd.put("ref", refDoc);
                    jd.put("jenis", jenisDoc);
                    jd.put("ada", ll != null);
                    jd.put("namaFile", ll == null ? "" : s(ll.getNama()));
                    docs.put(jd);
                }
            }
            j.put("dokumen", docs);

            steps.put(j);
        }
        result.put("steps", steps);
        result.put("status","00");

    } else if ("opsiProses".equals(aksi)) {
        // Data form proses disposisi: opsi rute (Proses SOP) + flag aturan (versi native JSP).
        boolean isAdmin = Common.getApakahAdmin();
        Long stepId=null; try { stepId=Long.valueOf(request.getParameter("stepId")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:301");}
        if (stepId==null) { result.put("status","02"); result.put("message","Langkah tidak ditemukan."); out.print(result.toString()); return; }
        DisposisiAlurSop step = (DisposisiAlurSop) session.get(DisposisiAlurSop.class, stepId);
        if (step==null) { result.put("status","02"); result.put("message","Langkah tidak ditemukan."); out.print(result.toString()); return; }
        AlurSop al = step.getAlurSop();
        // Pre-select + flag ujung: alurSop dari langkah anak (sebelumnya = step ini)
        JSONArray pre = new JSONArray();
        List<?> kids = session.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.isNotNull("alurSop"))
                .add(Restrictions.eq("sebelumnya", step))
                .setProjection(org.hibernate.criterion.Projections.groupProperty("alurSop.id")).list();
        for (Object k : kids) { if (k!=null) pre.put(((Long)k).longValue()); }
        // ATURAN: rute hanya bisa diubah bila langkah BELUM punya anak (robust utk langkah start).
        boolean bolehEditProses = (kids == null || kids.isEmpty());
        result.put("stepId", step.getId());
        result.put("disposisiSopId", step.getDisposisiSop()==null?JSONObject.NULL:step.getDisposisiSop().getId());
        result.put("alurSopId", al==null?JSONObject.NULL:al.getId());
        result.put("namaAlur", al==null?"":s(al.getNama()));
        result.put("keterangan", s(step.getKeterangan()));
        result.put("bolehEditProses", bolehEditProses);
        result.put("isAdmin", isAdmin);
        result.put("bisaKembali", step.getSebelumnya()!=null);
        result.put("catatanWajib", al!=null && Boolean.TRUE.equals(al.getCatatanWajibDiisi()));
        result.put("ruteOpsional", al!=null && Boolean.TRUE.equals(al.getAlurSetelahnyaTidakWajib()));
        result.put("berupaPilihan", al!=null && Boolean.TRUE.equals(al.getAlurSetelahnyaBerupaPilihan()));
        JSONArray opsi = new JSONArray();
        if (al!=null) {
            for (Object o : al.ambilAlurSetelahnya()) {
                AlurSop a = (AlurSop) o; if (a==null) continue;
                JSONObject j = new JSONObject(); j.put("id", a.getId()); j.put("nama", s(a.getNama())); opsi.put(j);
            }
        }
        result.put("opsiRute", opsi);
        result.put("preselected", pre);
        result.put("status","00");

    } else if ("simpanDisposisi".equals(aksi)) {
        // Proses penuh (mode aktor): simpan langkah + routing rute berikutnya (native, via service).
        Long dsId=null, stepId=null, alurId=null;
        try { dsId=Long.valueOf(request.getParameter("disposisiSopId")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:340");}
        String stepStr=request.getParameter("disposisiAlurSopId");
        if (stepStr!=null && !stepStr.trim().isEmpty()) { try { stepId=Long.valueOf(stepStr.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:342");} }
        try { alurId=Long.valueOf(request.getParameter("alurSopId")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:343");}
        String ket = request.getParameter("keterangan");
        boolean setujui = "true".equals(request.getParameter("setujui"));
        boolean kembali = "true".equals(request.getParameter("kembali"));
        String selStr = request.getParameter("selanjutnya");
        List<Long> sel = new ArrayList<Long>();
        if (selStr!=null && !selStr.trim().isEmpty()) {
            for (String part : selStr.split(",")) { try { sel.add(Long.valueOf(part.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:350");} }
        }
        ProsesDisposisiSopService.Hasil h = ProsesDisposisiSopService.prosesLangkah(
                tbmuser, dsId, stepId, alurId, tbmuser.getUserId(), new Date(), null, ket, setujui, kembali, sel);
        result.put("status", h.ok?"00":"02");
        result.put("message", h.pesan);
        if (h.disposisiAlurSopId!=null) result.put("stepId", h.disposisiAlurSopId);

    } else if ("editKeterangan".equals(aksi)) {
        // Mode admin: sunting catatan langkah tengah TANPA mengubah routing.
        Long stepId=null; try { stepId=Long.valueOf(request.getParameter("disposisiAlurSopId")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:360");}
        ProsesDisposisiSopService.Hasil h = ProsesDisposisiSopService.updateKeterangan(stepId, request.getParameter("keterangan"));
        result.put("status", h.ok?"00":"02");
        result.put("message", h.pesan);

    } else if ("opsiPengajuanBaru".equals(aksi)) {
        // Data form "Pengajuan Baru" (HYBRID): hanya SOP TANPA form data yang native.
        Long sopId=null; try { sopId=Long.valueOf(request.getParameter("sopId")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:367");}
        if (sopId==null) { result.put("status","02"); result.put("message","SOP tidak ditemukan."); out.print(result.toString()); return; }
        Sop sopObj = (Sop) session.get(Sop.class, sopId);
        if (sopObj==null) { result.put("status","02"); result.put("message","SOP tidak ditemukan."); out.print(result.toString()); return; }
        AlurSop start = (AlurSop) session.createCriteria(AlurSop.class)
                .add(Restrictions.eq("sop", sopObj)).add(Restrictions.eq("start", true))
                .addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
        if (start==null) { result.put("status","02"); result.put("message","SOP ini belum memiliki langkah awal (start)."); out.print(result.toString()); return; }
        boolean adaForm = start.getFormInputan()!=null && !start.getFormInputan().trim().isEmpty();
        result.put("adaForm", adaForm); // true -> JSP arahkan ke form ZK (SOP ber-form)
        result.put("sopId", sopId);
        result.put("sopNama", s(sopObj.getNama()));
        if (!adaForm) {
            result.put("namaAlur", s(start.getNama()));
            result.put("catatanWajib", Boolean.TRUE.equals(start.getCatatanWajibDiisi()));
            result.put("ruteOpsional", Boolean.TRUE.equals(start.getAlurSetelahnyaTidakWajib()));
            result.put("berupaPilihan", Boolean.TRUE.equals(start.getAlurSetelahnyaBerupaPilihan()));
            JSONArray opsi = new JSONArray();
            for (Object o : start.ambilAlurSetelahnya()) {
                AlurSop a=(AlurSop)o; if(a==null) continue;
                JSONObject j=new JSONObject(); j.put("id",a.getId()); j.put("nama",s(a.getNama())); opsi.put(j);
            }
            result.put("opsiRute", opsi);
        }
        result.put("status","00");

    } else if ("simpanPengajuanBaru".equals(aksi)) {
        // Buat pengajuan baru native (DisposisiSop + langkah start + routing) via service.
        Long sopId=null; try { sopId=Long.valueOf(request.getParameter("sopId")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:395");}
        String ket = request.getParameter("keterangan");
        String selStr = request.getParameter("selanjutnya");
        List<Long> sel = new ArrayList<Long>();
        if (selStr!=null && !selStr.trim().isEmpty()) {
            for (String p : selStr.split(",")) { try { sel.add(Long.valueOf(p.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:400");} }
        }
        ProsesDisposisiSopService.Hasil h = ProsesDisposisiSopService.buatPengajuanBaru(tbmuser, sopId, ket, new Date(), sel);
        result.put("status", h.ok?"00":"02");
        result.put("message", h.pesan);
        if (h.disposisiAlurSopId!=null) result.put("disposisiSopId", h.disposisiAlurSopId);

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:409");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoptampilanalursopzul/pengajuan_sop_service.jsp:410");}
}
out.print(result.toString());
%>
