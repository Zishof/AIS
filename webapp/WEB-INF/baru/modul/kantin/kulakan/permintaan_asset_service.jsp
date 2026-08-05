<%@page session="false"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.PermintaanPengadaanMasterAsset"%>
<%@page import="ais.database.model.asset.PermintaanPengadaanMasterAssetDetail"%>
<%@page import="ais.database.model.asset.MasterAsset"%>
<%@page import="ais.database.model.asset.Lokasi"%>
<%@page import="ais.database.model.rab.SatuanKerja"%>
<%!
    // Format Rp aman utk skalar Number
    static double d(Object o){ return o==null?0.0:((Number)o).doubleValue(); }
    static String s(Object o){ return o==null?"":o.toString(); }
    // Daftar Disposisi SOP aktif untuk picker "Dijalankan via SOP"
    static org.json.JSONArray disposisiSopList(org.hibernate.Session session) throws Exception {
        org.json.JSONArray arr = new org.json.JSONArray();
        for (Object o : session.createCriteria(ais.database.model.sop.DisposisiSop.class).add(org.hibernate.criterion.Restrictions.or(org.hibernate.criterion.Restrictions.isNull("aktif"), org.hibernate.criterion.Restrictions.eq("aktif", true))).addOrder(org.hibernate.criterion.Order.desc("id")).setMaxResults(500).list()) {
            ais.database.model.sop.DisposisiSop d = (ais.database.model.sop.DisposisiSop) o;
            org.json.JSONObject j = new org.json.JSONObject();
            j.put("id", d.getId());
            String nm = (d.getSop()!=null ? s(d.getSop().getNama()) : "Disposisi");
            if (d.getKeterangan()!=null && !d.getKeterangan().trim().isEmpty()) nm += " - " + s(d.getKeterangan());
            j.put("nama", nm);
            arr.put(j);
        }
        return arr;
    }
    // Set disposisiSop pada dokumen + catat id dokumen ke properti disposisi (pola FormSop.onSave + runner SOP)
    static ais.database.model.sop.DisposisiSop tautkanProperti(org.hibernate.Session session, org.json.JSONObject data, String key, Long docId) throws Exception {
        if (data.isNull("disposisiSop") || data.getString("disposisiSop").trim().isEmpty()) return null;
        ais.database.model.sop.DisposisiSop disp = (ais.database.model.sop.DisposisiSop) session.get(ais.database.model.sop.DisposisiSop.class, Long.parseLong(data.getString("disposisiSop").trim()));
        if (disp == null) return null;
        org.json.JSONObject prop = (disp.getProperti()==null || disp.getProperti().trim().isEmpty()) ? new org.json.JSONObject() : new org.json.JSONObject(disp.getProperti());
        org.json.JSONObject entry = prop.isNull(key) ? new org.json.JSONObject() : prop.getJSONObject(key);
        entry.put("id", docId);
        prop.put(key, entry);
        disp.setProperti(prop.toString());
        session.update(disp);
        return disp;
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
    // Pedagang/toko dikunci ke tokonya sendiri; hanya admin-kantin (tanpa pedagang) boleh memilih toko.
    ais.database.model.inventory.Toko scopeToko = (tbmuser.getPedagang() != null) ? tbmuser.getPedagang().getToko() : null;
    boolean lockToko = (scopeToko != null);
    SimpleDateFormat dfTgl = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat dfOut = new SimpleDateFormat("dd-MM-yyyy");

    if ("combo".equals(aksi)) {
        // Daftar Satuan Kerja + Lokasi untuk form
        JSONArray sk = new JSONArray();
        for (Object o : session.createCriteria(SatuanKerja.class).addOrder(Order.asc("nama")).setMaxResults(2000).list()) {
            SatuanKerja x = (SatuanKerja) o; JSONObject j = new JSONObject();
            j.put("id", x.getId()); j.put("nama", s(x.getNama())); sk.put(j);
        }
        JSONArray lk = new JSONArray();
        for (Object o : session.createCriteria(Lokasi.class).addOrder(Order.asc("nama")).setMaxResults(2000).list()) {
            Lokasi x = (Lokasi) o; JSONObject j = new JSONObject();
            j.put("id", x.getId()); j.put("nama", s(x.getNama())); lk.put(j);
        }
        JSONArray tk = new JSONArray();
        for (Object o : session.createCriteria(ais.database.model.inventory.Toko.class).addOrder(Order.asc("nama")).setMaxResults(2000).list()) {
            ais.database.model.inventory.Toko x = (ais.database.model.inventory.Toko) o; JSONObject j = new JSONObject();
            j.put("id", x.getId()); j.put("nama", s(x.getNama())); tk.put(j);
        }
        result.put("toko", tk);
        result.put("lockToko", lockToko);
        if (lockToko) { org.json.JSONArray __ot = new org.json.JSONArray(); org.json.JSONObject __jt = new org.json.JSONObject(); __jt.put("id", scopeToko.getId()); __jt.put("nama", scopeToko.getNama()==null?"":scopeToko.getNama()); __ot.put(__jt); result.put("toko", __ot); } // pedagang: dropdown hanya tokonya
        result.put("tokoTerkunci", lockToko ? (""+scopeToko.getId()) : "");
        result.put("tokoTerkunciNama", lockToko ? s(scopeToko.getNama()) : "");
        result.put("disposisiSop", disposisiSopList(session));
        result.put("satuanKerja", sk); result.put("lokasi", lk); result.put("status","00");

    } else if ("cariMasterAsset".equals(aksi)) {
        String q = request.getParameter("q"); if (q==null) q="";
        Criteria c = session.createCriteria(MasterAsset.class).addOrder(Order.asc("nama")).setMaxResults(50);
        if (!q.trim().isEmpty()) c.add(Restrictions.or(Restrictions.ilike("nama", q.trim(), MatchMode.ANYWHERE), Restrictions.ilike("kode", q.trim(), MatchMode.ANYWHERE)));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            MasterAsset m = (MasterAsset) o; JSONObject j = new JSONObject();
            j.put("id", m.getId()); j.put("kode", s(m.getKode())); j.put("nama", s(m.getNama()));
            j.put("harga", m.getHargaBeliDefault()==null?0.0:m.getHargaBeliDefault()); arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else if ("cariAkun".equals(aksi)) {
        String q = request.getParameter("q"); if (q==null) q="";
        Criteria c = session.createCriteria(ais.database.model.akunting.Akun.class).addOrder(Order.asc("kode")).setMaxResults(40);
        if (!q.trim().isEmpty()) c.add(Restrictions.or(Restrictions.ilike("nama", q.trim(), MatchMode.ANYWHERE), Restrictions.ilike("kode", q.trim(), MatchMode.ANYWHERE)));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) { ais.database.model.akunting.Akun a=(ais.database.model.akunting.Akun)o; JSONObject j=new JSONObject();
            j.put("id", a.getId()); j.put("nama", (a.getKode()==null?"":s(a.getKode())+" - ")+s(a.getNama())); arr.put(j); }
        result.put("data", arr); result.put("status","00");

    } else if ("cariWorkspace".equals(aksi)) {
        String q = request.getParameter("q"); if (q==null) q="";
        Criteria c = session.createCriteria(ais.database.model.rab.Workspace.class).addOrder(Order.desc("id")).setMaxResults(40);
        if (!q.trim().isEmpty()) c.add(Restrictions.or(Restrictions.ilike("nama", q.trim(), MatchMode.ANYWHERE), Restrictions.ilike("kode", q.trim(), MatchMode.ANYWHERE)));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) { ais.database.model.rab.Workspace w=(ais.database.model.rab.Workspace)o; JSONObject j=new JSONObject();
            j.put("id", w.getId());
            String nm = (w.getKode()==null?"":s(w.getKode())+" - ")+s(w.getNama());
            if (w.getSatuanKerja()!=null) nm += " ("+s(w.getSatuanKerja().getNama())+")";
            j.put("nama", nm); arr.put(j); }
        result.put("data", arr); result.put("status","00");

    } else if ("infoAnggaran".equals(aksi)) {
        // Info saldo anggaran live (reuse JenisUangMukaAction.hitungSaldo, pola ZK PR form)
        result.put("status","00");
        String wsId = request.getParameter("workspace");
        if (wsId==null || wsId.trim().isEmpty()) { result.put("ada", false); out.print(result.toString()); return; }
        ais.database.model.rab.Workspace w = (ais.database.model.rab.Workspace) session.get(ais.database.model.rab.Workspace.class, Long.parseLong(wsId.trim()));
        if (w==null) { result.put("ada", false); out.print(result.toString()); return; }
        Long prId = (request.getParameter("id")==null || request.getParameter("id").trim().isEmpty()) ? null : Long.valueOf(request.getParameter("id").trim());
        Date tgl = (request.getParameter("tanggal")==null || request.getParameter("tanggal").trim().isEmpty()) ? new Date() : dfTgl.parse(request.getParameter("tanggal").trim());
        double nilai = w.getHargaTotal()==null?0.0:w.getHargaTotal();
        double sisa = 0.0;
        try { Double sd = ais.action.master.akunting.JenisUangMukaAction.hitungSaldo(null, null, prId, null, w, tgl); sisa = sd==null?0.0:sd; } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/permintaan_asset_service.jsp:140");}
        result.put("ada", true);
        result.put("nilai", nilai); result.put("sisa", sisa); result.put("dalamProses", nilai - sisa);
        result.put("unit", w.getSatuanKerja()==null?"":s(w.getSatuanKerja().getNama()));
        result.put("akun", w.getAkun()==null?"":((w.getAkun().getKode()==null?"":s(w.getAkun().getKode())+" - ")+s(w.getAkun().getNama())));
        result.put("periode", (w.getMulai()==null?"":dfOut.format(w.getMulai())) + (w.getSelesai()==null?"":" s.d "+dfOut.format(w.getSelesai())));

    } else if ("list".equals(aksi)) {
        String kode = request.getParameter("kode");
        String tglM = request.getParameter("tglMulai");
        String tglA = request.getParameter("tglAkhir");
        String status = request.getParameter("status"); // semua/pending/disetujui/ditolak
        Criteria c = session.createCriteria(PermintaanPengadaanMasterAsset.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.desc("id")).setMaxResults(300);
        if (lockToko) c.add(Restrictions.eq("toko", scopeToko));
        if (kode!=null && !kode.trim().isEmpty()) c.add(Restrictions.ilike("kode", kode.trim(), MatchMode.ANYWHERE));
        if (tglM!=null && !tglM.trim().isEmpty()) c.add(Restrictions.ge("tanggalPembuatan", dfTgl.parse(tglM.trim())));
        if (tglA!=null && !tglA.trim().isEmpty()) { Calendar cal=Calendar.getInstance(); cal.setTime(dfTgl.parse(tglA.trim())); cal.add(Calendar.DAY_OF_MONTH,1); c.add(Restrictions.lt("tanggalPembuatan", cal.getTime())); }
        if ("disetujui".equals(status)) c.add(Restrictions.isNotNull("disetujuiOleh"));
        else if ("ditolak".equals(status)) c.add(Restrictions.isNotNull("ditolakOleh"));
        else if ("pending".equals(status)) c.add(Restrictions.and(Restrictions.isNull("disetujuiOleh"), Restrictions.isNull("ditolakOleh")));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            PermintaanPengadaanMasterAsset p = (PermintaanPengadaanMasterAsset) o;
            JSONObject j = new JSONObject();
            j.put("id", p.getId());
            j.put("kode", s(p.getKode()));
            j.put("tanggal", p.getTanggalPembuatan()==null?"":dfOut.format(p.getTanggalPembuatan()));
            j.put("satuanKerja", p.getSatuanKerja()==null?"":s(p.getSatuanKerja().getNama()));
            j.put("nilai", p.getNilai()==null?0.0:p.getNilai());
            j.put("keterangan", s(p.getKeterangan()));
            String st = p.getDisetujuiOleh()!=null ? "disetujui" : (p.getDitolakOleh()!=null ? "ditolak" : "pending");
            j.put("status", st);
            j.put("toko", p.getToko()==null?"":s(p.getToko().getNama()));
            j.put("sop", p.getDisposisiSop()!=null);
            Number ji = (Number) session.createCriteria(PermintaanPengadaanMasterAssetDetail.class).add(Restrictions.eq("permintaanPengadaanMasterAsset", p)).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
            j.put("jumlahItem", ji==null?0:ji.intValue());
            arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else if ("detail".equals(aksi)) {
        Long id = Long.parseLong(request.getParameter("id").trim());
        PermintaanPengadaanMasterAsset p = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, id);
        if (p==null){ result.put("status","02"); result.put("message","Data tidak ditemukan"); out.print(result.toString()); return; }
        JSONObject h = new JSONObject();
        h.put("id", p.getId()); h.put("kode", s(p.getKode()));
        h.put("tanggal", p.getTanggalPembuatan()==null?"":dfTgl.format(p.getTanggalPembuatan()));
        h.put("satuanKerja", p.getSatuanKerja()==null?"":(""+p.getSatuanKerja().getId()));
        h.put("lokasi", p.getLokasi()==null?"":(""+p.getLokasi().getId()));
        h.put("toko", p.getToko()==null?"":(""+p.getToko().getId()));
        h.put("disposisiSop", p.getDisposisiSop()==null?"":(""+p.getDisposisiSop().getId()));
        h.put("akun", p.getAkun()==null?"":(""+p.getAkun().getId()));
        h.put("akunNama", p.getAkun()==null?"":((p.getAkun().getKode()==null?"":s(p.getAkun().getKode())+" - ")+s(p.getAkun().getNama())));
        h.put("workspace", p.getWorkspace()==null?"":(""+p.getWorkspace().getId()));
        h.put("workspaceNama", p.getWorkspace()==null?"":s(p.getWorkspace().toString()));
        h.put("tanpaAnggaran", p.getTanpaAnggaran()!=null && p.getTanpaAnggaran());
        h.put("danaTitipan", p.getDanaTitipan()!=null && p.getDanaTitipan());
        h.put("wajibPerjanjian", p.getWajibAdaPerjanjianKerjasama()!=null && p.getWajibAdaPerjanjianKerjasama());
        h.put("keterangan", s(p.getKeterangan()));
        h.put("status", p.getDisetujuiOleh()!=null?"disetujui":(p.getDitolakOleh()!=null?"ditolak":"pending"));
        JSONArray lines = new JSONArray();
        for (Object o : session.createCriteria(PermintaanPengadaanMasterAssetDetail.class).add(Restrictions.eq("permintaanPengadaanMasterAsset", p)).addOrder(Order.asc("id")).list()) {
            PermintaanPengadaanMasterAssetDetail dt = (PermintaanPengadaanMasterAssetDetail) o;
            JSONObject j = new JSONObject();
            j.put("masterAsset", dt.getMasterAsset()==null?"":(""+dt.getMasterAsset().getId()));
            j.put("masterAssetNama", dt.getMasterAsset()==null?"":(s(dt.getMasterAsset().getKode())+" - "+s(dt.getMasterAsset().getNama())));
            j.put("jumlah", dt.getJumlah()==null?0.0:dt.getJumlah());
            j.put("hargaBeli", dt.getHargaBeli()==null?0.0:dt.getHargaBeli());
            j.put("keterangan", s(dt.getKeterangan()));
            lines.put(j);
        }
        h.put("lines", lines);
        result.put("data", h); result.put("status","00");

    } else if ("simpan".equals(aksi)) {
        JSONObject data = new JSONObject(request.getParameter("data"));
        Transaction tx = session.beginTransaction();
        try {
            PermintaanPengadaanMasterAsset p;
            boolean baru = data.isNull("id") || data.getString("id").trim().isEmpty();
            if (baru) { p = new PermintaanPengadaanMasterAsset(); p.setDibuatOleh(tbmuser); p.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate()); p.setAktif(true); }
            else { p = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, Long.parseLong(data.getString("id").trim())); }
            if (p.getDisetujuiOleh()!=null) { tx.rollback(); result.put("status","03"); result.put("message","Permintaan sudah disetujui, tidak bisa diubah."); out.print(result.toString()); return; }
            String kode = data.optString("kode","").trim();
            if (kode.isEmpty()) kode = "PR-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            p.setKode(kode);
            p.setKeterangan(data.optString("keterangan",""));
            if (!data.optString("tanggal","").trim().isEmpty()) p.setTanggalPembuatan(dfTgl.parse(data.getString("tanggal").trim()));
            if (!data.optString("satuanKerja","").trim().isEmpty()) p.setSatuanKerja((SatuanKerja) session.get(SatuanKerja.class, Long.parseLong(data.getString("satuanKerja").trim())));
            if (!data.optString("lokasi","").trim().isEmpty()) p.setLokasi((Lokasi) session.get(Lokasi.class, Long.parseLong(data.getString("lokasi").trim())));
            p.setAkun(data.optString("akun","").trim().isEmpty()?null:(ais.database.model.akunting.Akun) session.get(ais.database.model.akunting.Akun.class, Long.parseLong(data.getString("akun").trim())));
            p.setWorkspace(data.optString("workspace","").trim().isEmpty()?null:(ais.database.model.rab.Workspace) session.get(ais.database.model.rab.Workspace.class, Long.parseLong(data.getString("workspace").trim())));
            p.setTanpaAnggaran(data.optBoolean("tanpaAnggaran", false));
            p.setDanaTitipan(data.optBoolean("danaTitipan", false));
            p.setWajibAdaPerjanjianKerjasama(data.optBoolean("wajibPerjanjian", false));
            if (lockToko) p.setToko(scopeToko);
            else p.setToko(data.optString("toko","").trim().isEmpty()?null:(ais.database.model.inventory.Toko) session.get(ais.database.model.inventory.Toko.class, Long.parseLong(data.getString("toko").trim())));

            JSONArray lines = data.isNull("lines") ? new JSONArray() : data.getJSONArray("lines");
            double nilai = 0;
            for (int i=0;i<lines.length();i++){ JSONObject l=lines.getJSONObject(i); nilai += (l.optDouble("jumlah",0) * l.optDouble("hargaBeli",0)); }
            p.setNilai(nilai);
            session.saveOrUpdate(p);
            session.flush();
            p.setDisposisiSop(tautkanProperti(session, data, PermintaanPengadaanMasterAsset.class.getName(), p.getId())); session.update(p); session.flush();

            // Ganti detail: hapus lama lalu sisipkan ulang
            session.createQuery("delete from PermintaanPengadaanMasterAssetDetail where permintaanPengadaanMasterAsset.id = :pid").setLong("pid", p.getId()).executeUpdate();
            for (int i=0;i<lines.length();i++){
                JSONObject l = lines.getJSONObject(i);
                if (l.isNull("masterAsset") || l.getString("masterAsset").trim().isEmpty()) continue;
                double jml = l.optDouble("jumlah",0); if (jml<=0) continue;
                double hrg = l.optDouble("hargaBeli",0);
                PermintaanPengadaanMasterAssetDetail dt = new PermintaanPengadaanMasterAssetDetail();
                dt.setPermintaanPengadaanMasterAsset(p);
                dt.setMasterAsset((MasterAsset) session.get(MasterAsset.class, Long.parseLong(l.getString("masterAsset").trim())));
                dt.setJumlah(jml); dt.setHargaBeli(hrg); dt.setHargaTotal(jml*hrg);
                dt.setKeterangan(l.optString("keterangan",""));
                session.save(dt);
            }
            tx.commit();
            result.put("status","00"); result.put("id", p.getId()); result.put("message","Permintaan tersimpan.");
        } catch (Exception ex) { try { tx.rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/permintaan_asset_service.jsp:264");} throw ex; }

    } else if ("setujui".equals(aksi)) {
        Long id = Long.parseLong(request.getParameter("id").trim());
        Transaction tx = session.beginTransaction();
        try {
            PermintaanPengadaanMasterAsset p = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, id);
            if (p!=null){ p.setDisetujuiOleh(tbmuser); p.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate()); session.update(p); }
            tx.commit(); result.put("status","00"); result.put("message","Permintaan disetujui.");
        } catch (Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/permintaan_asset_service.jsp:273");} throw ex; }

    } else if ("hapus".equals(aksi)) {
        Long id = Long.parseLong(request.getParameter("id").trim());
        Transaction tx = session.beginTransaction();
        try {
            PermintaanPengadaanMasterAsset p = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, id);
            if (p!=null && p.getDisetujuiOleh()!=null){ tx.rollback(); result.put("status","03"); result.put("message","Sudah disetujui, tidak bisa dihapus."); out.print(result.toString()); return; }
            if (p!=null){
                session.createQuery("delete from PermintaanPengadaanMasterAssetDetail where permintaanPengadaanMasterAsset.id = :pid").setLong("pid", id).executeUpdate();
                session.delete(p);
            }
            tx.commit(); result.put("status","00"); result.put("message","Permintaan dihapus.");
        } catch (Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/permintaan_asset_service.jsp:286");} throw ex; }

    } else {
        result.put("status","98"); result.put("message","Aksi tidak dikenal.");
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/kulakan/permintaan_asset_service.jsp:292");
    try { result.put("status","99"); result.put("message", "Error: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/permintaan_asset_service.jsp:293");}
}
out.print(result.toString());
%>
