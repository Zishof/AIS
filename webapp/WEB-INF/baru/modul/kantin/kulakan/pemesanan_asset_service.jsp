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
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.PemesananPengadaanMasterAsset"%>
<%@page import="ais.database.model.asset.PemesananPengadaanMasterAssetDetail"%>
<%@page import="ais.database.model.asset.PermintaanPengadaanMasterAsset"%>
<%@page import="ais.database.model.asset.PermintaanPengadaanMasterAssetDetail"%>
<%@page import="ais.database.model.asset.PerjanjianKerjasamaMasterAsset"%>
<%@page import="ais.database.model.asset.JenisPemesananPengadaanAsset"%>
<%@page import="ais.database.model.asset.JenisPajakBarang"%>
<%@page import="ais.database.model.asset.JenisPajakPpn"%>
<%@page import="ais.database.model.asset.MasterAsset"%>
<%@page import="ais.database.model.asset.Lokasi"%>
<%@page import="ais.database.model.rab.SatuanKerja"%>
<%!
    static String s(Object o){ return o==null?"":o.toString(); }
    static double dbl(JSONObject o, String k){ return o.isNull(k)?0.0:o.optDouble(k,0.0); }
    static boolean bln(JSONObject o, String k){ return !o.isNull(k) && (o.optBoolean(k,false) || "true".equals(o.optString(k,""))); }
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
        result.put("satuanKerja", listCombo(session, SatuanKerja.class, "nama", false));
        result.put("lokasi", listCombo(session, Lokasi.class, "nama", false));
        result.put("perjanjian", listCombo(session, PerjanjianKerjasamaMasterAsset.class, "kode", false));
        result.put("jenisPemesanan", listCombo(session, JenisPemesananPengadaanAsset.class, "nama", false));
        result.put("jenisPajakBarang", listCombo(session, JenisPajakBarang.class, "nama", true));
        result.put("jenisPajakPpn", listCombo(session, JenisPajakPpn.class, "nama", true));
        result.put("toko", listCombo(session, ais.database.model.inventory.Toko.class, "nama", false));
        result.put("lockToko", lockToko);
        if (lockToko) { org.json.JSONArray __ot = new org.json.JSONArray(); org.json.JSONObject __jt = new org.json.JSONObject(); __jt.put("id", scopeToko.getId()); __jt.put("nama", scopeToko.getNama()==null?"":scopeToko.getNama()); __ot.put(__jt); result.put("toko", __ot); } // pedagang: dropdown hanya tokonya
        result.put("tokoTerkunci", lockToko ? (""+scopeToko.getId()) : "");
        result.put("tokoTerkunciNama", lockToko ? s(scopeToko.getNama()) : "");
        result.put("disposisiSop", disposisiSopList(session));
        result.put("status","00");

    } else if ("cariMasterAsset".equals(aksi)) {
        String q = request.getParameter("q"); if (q==null) q="";
        Criteria c = session.createCriteria(MasterAsset.class).addOrder(Order.asc("nama")).setMaxResults(50);
        if (!q.trim().isEmpty()) c.add(Restrictions.or(Restrictions.ilike("nama", q.trim(), MatchMode.ANYWHERE), Restrictions.ilike("kode", q.trim(), MatchMode.ANYWHERE)));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) { MasterAsset m=(MasterAsset)o; JSONObject j=new JSONObject();
            j.put("id", m.getId()); j.put("kode", s(m.getKode())); j.put("nama", s(m.getNama()));
            j.put("harga", m.getHargaBeliDefault()==null?0.0:m.getHargaBeliDefault()); arr.put(j); }
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
        result.put("status","00");
        String wsId = request.getParameter("workspace");
        if (wsId==null || wsId.trim().isEmpty()) { result.put("ada", false); out.print(result.toString()); return; }
        ais.database.model.rab.Workspace w = (ais.database.model.rab.Workspace) session.get(ais.database.model.rab.Workspace.class, Long.parseLong(wsId.trim()));
        if (w==null) { result.put("ada", false); out.print(result.toString()); return; }
        Date tgl = (request.getParameter("tanggal")==null || request.getParameter("tanggal").trim().isEmpty()) ? new Date() : dfTgl.parse(request.getParameter("tanggal").trim());
        double nilai = w.getHargaTotal()==null?0.0:w.getHargaTotal();
        double sisa = 0.0;
        try { Double sd = ais.action.master.akunting.JenisUangMukaAction.hitungSaldo(null, null, null, null, w, tgl); sisa = sd==null?0.0:sd; } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/pemesanan_asset_service.jsp:97");}
        result.put("ada", true);
        result.put("nilai", nilai); result.put("sisa", sisa); result.put("dalamProses", nilai - sisa);
        result.put("unit", w.getSatuanKerja()==null?"":s(w.getSatuanKerja().getNama()));
        result.put("akun", w.getAkun()==null?"":((w.getAkun().getKode()==null?"":s(w.getAkun().getKode())+" - ")+s(w.getAkun().getNama())));
        result.put("periode", (w.getMulai()==null?"":dfOut.format(w.getMulai())) + (w.getSelesai()==null?"":" s.d "+dfOut.format(w.getSelesai())));

    } else if ("listPr".equals(aksi)) {
        // PR yang sudah disetujui & BELUM dipesan (header.pemesananPengadaanMasterAsset null) → kandidat impor
        Criteria c = session.createCriteria(PermintaanPengadaanMasterAsset.class)
                .add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.isNull("pemesananPengadaanMasterAsset"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.desc("id")).setMaxResults(200);
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) { PermintaanPengadaanMasterAsset p=(PermintaanPengadaanMasterAsset)o; JSONObject j=new JSONObject();
            j.put("id", p.getId()); j.put("kode", s(p.getKode())); j.put("nilai", p.getNilai()==null?0.0:p.getNilai());
            j.put("satuanKerja", p.getSatuanKerja()==null?"":s(p.getSatuanKerja().getNama())); arr.put(j); }
        result.put("data", arr); result.put("status","00");

    } else if ("importPr".equals(aksi)) {
        Long prId = Long.parseLong(request.getParameter("id").trim());
        PermintaanPengadaanMasterAsset pr = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, prId);
        JSONObject h = new JSONObject();
        if (pr!=null){
            h.put("satuanKerja", pr.getSatuanKerja()==null?"":(""+pr.getSatuanKerja().getId()));
            h.put("lokasi", pr.getLokasi()==null?"":(""+pr.getLokasi().getId()));
            h.put("keterangan", s(pr.getKeterangan()));
        }
        JSONArray lines = new JSONArray();
        for (Object o : session.createCriteria(PermintaanPengadaanMasterAssetDetail.class).add(Restrictions.eq("permintaanPengadaanMasterAsset", pr)).addOrder(Order.asc("id")).list()){
            PermintaanPengadaanMasterAssetDetail dt=(PermintaanPengadaanMasterAssetDetail)o;
            double sisa = (dt.getJumlah()==null?0:dt.getJumlah()) - (dt.getJumlahDatang()==null?0:dt.getJumlahDatang());
            if (sisa <= 0) continue;
            JSONObject j=new JSONObject();
            j.put("prDetailId", dt.getId());
            j.put("masterAsset", dt.getMasterAsset()==null?"":(""+dt.getMasterAsset().getId()));
            j.put("masterAssetNama", dt.getMasterAsset()==null?"":(s(dt.getMasterAsset().getKode())+" - "+s(dt.getMasterAsset().getNama())));
            j.put("jumlah", sisa); j.put("hargaBeli", dt.getHargaBeli()==null?0.0:dt.getHargaBeli()); j.put("keterangan", s(dt.getKeterangan()));
            lines.put(j);
        }
        h.put("lines", lines); result.put("data", h); result.put("status","00");

    } else if ("list".equals(aksi)) {
        String kode = request.getParameter("kode");
        String tglM = request.getParameter("tglMulai");
        String tglA = request.getParameter("tglAkhir");
        String status = request.getParameter("status");
        Criteria c = session.createCriteria(PemesananPengadaanMasterAsset.class)
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
            PemesananPengadaanMasterAsset p=(PemesananPengadaanMasterAsset)o; JSONObject j=new JSONObject();
            j.put("id", p.getId()); j.put("kode", s(p.getKode())); j.put("kodeInvoice", s(p.getKodeInvoice()));
            j.put("tanggal", p.getTanggalPembuatan()==null?"":dfOut.format(p.getTanggalPembuatan()));
            j.put("satuanKerja", p.getSatuanKerja()==null?"":s(p.getSatuanKerja().getNama()));
            j.put("nilai", p.getNilai()==null?0.0:p.getNilai());
            j.put("status", p.getDisetujuiOleh()!=null?"disetujui":(p.getDitolakOleh()!=null?"ditolak":"pending"));
            j.put("toko", p.getToko()==null?"":s(p.getToko().getNama()));
            j.put("sop", p.getDisposisiSop()!=null);
            Number ji=(Number) session.createCriteria(PemesananPengadaanMasterAssetDetail.class).add(Restrictions.eq("pemesananPengadaanMasterAsset", p)).setProjection(Projections.rowCount()).uniqueResult();
            j.put("jumlahItem", ji==null?0:ji.intValue()); arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else if ("detail".equals(aksi)) {
        Long id = Long.parseLong(request.getParameter("id").trim());
        PemesananPengadaanMasterAsset p=(PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, id);
        if (p==null){ result.put("status","02"); result.put("message","Data tidak ditemukan"); out.print(result.toString()); return; }
        JSONObject h=new JSONObject();
        h.put("id", p.getId()); h.put("kode", s(p.getKode())); h.put("kodeInvoice", s(p.getKodeInvoice())); h.put("keterangan", s(p.getKeterangan()));
        h.put("tanggal", p.getTanggalPembuatan()==null?"":dfTgl.format(p.getTanggalPembuatan()));
        h.put("pengiriman", p.getPengirimanPalingLambat()==null?"":dfTgl.format(p.getPengirimanPalingLambat()));
        h.put("tglMulai", p.getTanggalMulai()==null?"":dfTgl.format(p.getTanggalMulai()));
        h.put("tglSampai", p.getTanggalSampai()==null?"":dfTgl.format(p.getTanggalSampai()));
        h.put("satuanKerja", p.getSatuanKerja()==null?"":(""+p.getSatuanKerja().getId()));
        h.put("lokasi", p.getLokasi()==null?"":(""+p.getLokasi().getId()));
        h.put("toko", p.getToko()==null?"":(""+p.getToko().getId()));
        h.put("disposisiSop", p.getDisposisiSop()==null?"":(""+p.getDisposisiSop().getId()));
        h.put("workspace", p.getWorkspace()==null?"":(""+p.getWorkspace().getId()));
        h.put("workspaceNama", p.getWorkspace()==null?"":s(p.getWorkspace().toString()));
        h.put("perjanjian", p.getPerjanjianKerjasamaMasterAsset()==null?"":(""+p.getPerjanjianKerjasamaMasterAsset().getId()));
        h.put("jenisPemesanan", p.getJenisPemesananPengadaanAsset()==null?"":(""+p.getJenisPemesananPengadaanAsset().getId()));
        h.put("jenisPajakPpnDp", p.getJenisPajakPpnDp()==null?"":(""+p.getJenisPajakPpnDp().getId()));
        h.put("dp", p.getDp()==null?0.0:p.getDp());
        h.put("catatanKesepakatan", s(p.getCatatanKesepakatan()));
        h.put("byTermin", p.getByTermin()!=null && p.getByTermin());
        h.put("tanpaAnggaran", p.getTanpaAnggaran()!=null && p.getTanpaAnggaran());
        h.put("pembelianLangsung", p.getPembelianLangsung()!=null && p.getPembelianLangsung());
        h.put("tampaPermintaan", p.getTampaPermintaan()!=null && p.getTampaPermintaan());
        h.put("formula", (p.getFormula()==null||p.getFormula().trim().isEmpty())?"[]":p.getFormula());
        h.put("status", p.getDisetujuiOleh()!=null?"disetujui":(p.getDitolakOleh()!=null?"ditolak":"pending"));
        JSONArray lines=new JSONArray();
        for (Object o : session.createCriteria(PemesananPengadaanMasterAssetDetail.class).add(Restrictions.eq("pemesananPengadaanMasterAsset", p)).addOrder(Order.asc("id")).list()){
            PemesananPengadaanMasterAssetDetail dt=(PemesananPengadaanMasterAssetDetail)o; JSONObject j=new JSONObject();
            j.put("masterAsset", dt.getMasterAsset()==null?"":(""+dt.getMasterAsset().getId()));
            j.put("masterAssetNama", dt.getMasterAsset()==null?"":(s(dt.getMasterAsset().getKode())+" - "+s(dt.getMasterAsset().getNama())));
            j.put("jumlah", dt.getJumlah()==null?0.0:dt.getJumlah());
            j.put("hargaBeli", dt.getHargaBeli()==null?0.0:dt.getHargaBeli());
            j.put("hargaPotongan", dt.getHargaPotongan());
            j.put("diskonPersen", dt.getDiskonDalamBentukPersen()!=null && dt.getDiskonDalamBentukPersen());
            j.put("persenPpn", dt.getPersenPpn());
            j.put("persenPph", dt.getPersenPph());
            j.put("jenisPajakBarang", dt.getJenisPajakBarang()==null?"":(""+dt.getJenisPajakBarang().getId()));
            j.put("jenisPajakPpn", dt.getJenisPajakPpn()==null?"":(""+dt.getJenisPajakPpn().getId()));
            j.put("hargaTotal", dt.getHargaTotal());
            j.put("keterangan", s(dt.getKeterangan()));
            j.put("prDetailId", dt.getPermintaanPengadaanMasterAssetDetail()==null?"":(""+dt.getPermintaanPengadaanMasterAssetDetail().getId()));
            lines.put(j);
        }
        h.put("lines", lines); result.put("data", h); result.put("status","00");

    } else if ("simpan".equals(aksi)) {
        JSONObject data = new JSONObject(request.getParameter("data"));
        Transaction tx = session.beginTransaction();
        try {
            PemesananPengadaanMasterAsset p;
            boolean baru = data.isNull("id") || data.getString("id").trim().isEmpty();
            if (baru){ p=new PemesananPengadaanMasterAsset(); p.setDibuatOleh(tbmuser); p.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate()); p.setAktif(true); }
            else { p=(PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, Long.parseLong(data.getString("id").trim())); }
            if (p.getDisetujuiOleh()!=null){ tx.rollback(); result.put("status","03"); result.put("message","PO sudah disetujui, tidak bisa diubah."); out.print(result.toString()); return; }

            String kode = data.optString("kode","").trim();
            if (kode.isEmpty()) kode = "PO-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            p.setKode(kode);
            p.setKodeInvoice(data.optString("kodeInvoice","").trim());
            p.setKeterangan(data.optString("keterangan",""));
            if (!data.optString("tanggal","").trim().isEmpty()) p.setTanggalPembuatan(dfTgl.parse(data.getString("tanggal").trim()));
            p.setPengirimanPalingLambat(data.optString("pengiriman","").trim().isEmpty()?null:dfTgl.parse(data.getString("pengiriman").trim()));
            p.setTanggalMulai(data.optString("tglMulai","").trim().isEmpty()?null:dfTgl.parse(data.getString("tglMulai").trim()));
            p.setTanggalSampai(data.optString("tglSampai","").trim().isEmpty()?null:dfTgl.parse(data.getString("tglSampai").trim()));
            p.setSatuanKerja(data.optString("satuanKerja","").trim().isEmpty()?null:(SatuanKerja) session.get(SatuanKerja.class, Long.parseLong(data.getString("satuanKerja").trim())));
            p.setLokasi(data.optString("lokasi","").trim().isEmpty()?null:(Lokasi) session.get(Lokasi.class, Long.parseLong(data.getString("lokasi").trim())));
            p.setWorkspace(data.optString("workspace","").trim().isEmpty()?null:(ais.database.model.rab.Workspace) session.get(ais.database.model.rab.Workspace.class, Long.parseLong(data.getString("workspace").trim())));
            if (lockToko) p.setToko(scopeToko);
            else p.setToko(data.optString("toko","").trim().isEmpty()?null:(ais.database.model.inventory.Toko) session.get(ais.database.model.inventory.Toko.class, Long.parseLong(data.getString("toko").trim())));
            p.setPerjanjianKerjasamaMasterAsset(data.optString("perjanjian","").trim().isEmpty()?null:(PerjanjianKerjasamaMasterAsset) session.get(PerjanjianKerjasamaMasterAsset.class, Long.parseLong(data.getString("perjanjian").trim())));
            p.setJenisPemesananPengadaanAsset(data.optString("jenisPemesanan","").trim().isEmpty()?null:(JenisPemesananPengadaanAsset) session.get(JenisPemesananPengadaanAsset.class, Long.parseLong(data.getString("jenisPemesanan").trim())));
            p.setJenisPajakPpnDp(data.optString("jenisPajakPpnDp","").trim().isEmpty()?null:(JenisPajakPpn) session.get(JenisPajakPpn.class, Long.parseLong(data.getString("jenisPajakPpnDp").trim())));
            p.setDp(dbl(data,"dp"));
            p.setCatatanKesepakatan(data.optString("catatanKesepakatan",""));
            p.setByTermin(bln(data,"byTermin"));
            p.setTanpaAnggaran(bln(data,"tanpaAnggaran"));
            p.setPembelianLangsung(bln(data,"pembelianLangsung"));
            p.setTampaPermintaan(bln(data,"tampaPermintaan"));
            p.setFormula(data.isNull("formula")?"[]":data.getString("formula"));

            JSONArray lines = data.isNull("lines") ? new JSONArray() : data.getJSONArray("lines");
            // Hitung nilai pakai logika entity (DPP-potongan+PPN-PPh)
            double nilai = 0;
            for (int i=0;i<lines.length();i++){
                JSONObject l=lines.getJSONObject(i);
                PemesananPengadaanMasterAssetDetail dt=new PemesananPengadaanMasterAssetDetail();
                dt.setJumlah(dbl(l,"jumlah")); dt.setHargaBeli(dbl(l,"hargaBeli"));
                dt.setHargaPotongan(dbl(l,"hargaPotongan")); dt.setDiskonDalamBentukPersen(bln(l,"diskonPersen"));
                dt.setPersenPpn(dbl(l,"persenPpn")); dt.setPersenPph(dbl(l,"persenPph"));
                nilai += dt.getHargaTotal();
            }
            p.setNilai(nilai);

            if (baru) session.save(p); else session.update(p);
            session.flush();
            p.setDisposisiSop(tautkanProperti(session, data, PemesananPengadaanMasterAsset.class.getName(), p.getId())); session.update(p); session.flush();

            // Lepas tautan PR lama lalu hapus detail lama (saat edit) agar bisa disusun ulang
            if (!baru){
                session.createQuery("update PermintaanPengadaanMasterAssetDetail set pemesananPengadaanMasterAssetDetail=null where pemesananPengadaanMasterAssetDetail in (select id from PemesananPengadaanMasterAssetDetail where pemesananPengadaanMasterAsset.id=:pid)").setLong("pid", p.getId()).executeUpdate();
                session.createQuery("delete from PemesananPengadaanMasterAssetDetail where pemesananPengadaanMasterAsset.id=:pid").setLong("pid", p.getId()).executeUpdate();
                session.flush();
            }

            for (int i=0;i<lines.length();i++){
                JSONObject l=lines.getJSONObject(i);
                if (l.isNull("masterAsset") || l.getString("masterAsset").trim().isEmpty()) continue;
                double jml=dbl(l,"jumlah"); if (jml<=0) continue;
                PemesananPengadaanMasterAssetDetail dt=new PemesananPengadaanMasterAssetDetail();
                dt.setPemesananPengadaanMasterAsset(p);
                dt.setMasterAsset((MasterAsset) session.get(MasterAsset.class, Long.parseLong(l.getString("masterAsset").trim())));
                dt.setJumlah(jml); dt.setHargaBeli(dbl(l,"hargaBeli"));
                dt.setHargaPotongan(dbl(l,"hargaPotongan")); dt.setDiskonDalamBentukPersen(bln(l,"diskonPersen"));
                dt.setPersenPpn(dbl(l,"persenPpn")); dt.setPersenPph(dbl(l,"persenPph"));
                if (!l.optString("jenisPajakBarang","").trim().isEmpty()) dt.setJenisPajakBarang((JenisPajakBarang) session.get(JenisPajakBarang.class, Long.parseLong(l.getString("jenisPajakBarang").trim())));
                if (!l.optString("jenisPajakPpn","").trim().isEmpty()) dt.setJenisPajakPpn((JenisPajakPpn) session.get(JenisPajakPpn.class, Long.parseLong(l.getString("jenisPajakPpn").trim())));
                dt.setKeterangan(l.optString("keterangan",""));
                dt.getHargaTotal(); // set hargaTotal pada entity

                PermintaanPengadaanMasterAssetDetail prDet = null;
                if (!l.optString("prDetailId","").trim().isEmpty())
                    prDet = (PermintaanPengadaanMasterAssetDetail) session.get(PermintaanPengadaanMasterAssetDetail.class, Long.parseLong(l.getString("prDetailId").trim()));
                if (prDet!=null) dt.setPermintaanPengadaanMasterAssetDetail(prDet);
                session.save(dt);
                session.flush();

                // Kaskade tautan PR (sama dgn ZK): PR detail → PO detail, PR header → PO
                if (prDet!=null){
                    prDet.setPemesananPengadaanMasterAssetDetail(dt);
                    session.saveOrUpdate(prDet);
                    if (prDet.getPermintaanPengadaanMasterAsset()!=null && prDet.getPermintaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()==null){
                        PermintaanPengadaanMasterAsset prH = prDet.getPermintaanPengadaanMasterAsset();
                        prH.setPemesananPengadaanMasterAsset(p); session.saveOrUpdate(prH);
                    }
                    session.flush();
                }
            }
            tx.commit();
            result.put("status","00"); result.put("id", p.getId()); result.put("message","Pemesanan tersimpan.");
        } catch (Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/pemesanan_asset_service.jsp:311");} throw ex; }

    } else if ("setujui".equals(aksi)) {
        Long id=Long.parseLong(request.getParameter("id").trim());
        Transaction tx=session.beginTransaction();
        try { PemesananPengadaanMasterAsset p=(PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, id);
            if (p!=null){ p.setDisetujuiOleh(tbmuser); p.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate()); session.update(p); }
            tx.commit(); result.put("status","00"); result.put("message","PO disetujui.");
        } catch(Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/pemesanan_asset_service.jsp:319");} throw ex; }

    } else if ("hapus".equals(aksi)) {
        Long id=Long.parseLong(request.getParameter("id").trim());
        Transaction tx=session.beginTransaction();
        try { PemesananPengadaanMasterAsset p=(PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, id);
            if (p!=null && p.getDisetujuiOleh()!=null){ tx.rollback(); result.put("status","03"); result.put("message","Sudah disetujui, tidak bisa dihapus."); out.print(result.toString()); return; }
            if (p!=null){
                session.createQuery("update PermintaanPengadaanMasterAssetDetail set pemesananPengadaanMasterAssetDetail=null where pemesananPengadaanMasterAssetDetail in (select id from PemesananPengadaanMasterAssetDetail where pemesananPengadaanMasterAsset.id=:pid)").setLong("pid", id).executeUpdate();
                session.createQuery("update PermintaanPengadaanMasterAsset set pemesananPengadaanMasterAsset=null where pemesananPengadaanMasterAsset.id=:pid").setLong("pid", id).executeUpdate();
                session.createQuery("delete from PemesananPengadaanMasterAssetDetail where pemesananPengadaanMasterAsset.id=:pid").setLong("pid", id).executeUpdate();
                session.delete(p);
            }
            tx.commit(); result.put("status","00"); result.put("message","PO dihapus.");
        } catch(Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/pemesanan_asset_service.jsp:333");} throw ex; }

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/kulakan/pemesanan_asset_service.jsp:337");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/pemesanan_asset_service.jsp:338");}
}
out.print(result.toString());
%>
<%!
    @SuppressWarnings({"unchecked","rawtypes"})
    static JSONArray listCombo(Session session, Class cls, String orderProp, boolean withPersen) throws Exception {
        JSONArray arr = new JSONArray();
        for (Object o : session.createCriteria(cls).addOrder(Order.asc(orderProp)).setMaxResults(3000).list()) {
            JSONObject j = new JSONObject();
            j.put("id", invoke(o, "getId"));
            Object nama = has(o,"getNama") ? invoke(o,"getNama") : invoke(o,"getKode");
            j.put("nama", nama==null?"":nama.toString());
            if (withPersen && has(o,"getPersen")) { Object pr=invoke(o,"getPersen"); j.put("persen", pr==null?0.0:((Number)pr).doubleValue()); }
            arr.put(j);
        }
        return arr;
    }
    static org.json.JSONArray disposisiSopList(org.hibernate.Session session) throws Exception {
        org.json.JSONArray arr = new org.json.JSONArray();
        for (Object o : session.createCriteria(ais.database.model.sop.DisposisiSop.class).add(org.hibernate.criterion.Restrictions.or(org.hibernate.criterion.Restrictions.isNull("aktif"), org.hibernate.criterion.Restrictions.eq("aktif", true))).addOrder(org.hibernate.criterion.Order.desc("id")).setMaxResults(500).list()) {
            ais.database.model.sop.DisposisiSop d = (ais.database.model.sop.DisposisiSop) o;
            org.json.JSONObject j = new org.json.JSONObject();
            j.put("id", d.getId());
            String nm = (d.getSop()!=null ? s(d.getSop().getNama()) : "Disposisi");
            if (d.getKeterangan()!=null && !d.getKeterangan().trim().isEmpty()) nm += " - " + s(d.getKeterangan());
            j.put("nama", nm); arr.put(j);
        }
        return arr;
    }
    static ais.database.model.sop.DisposisiSop tautkanProperti(org.hibernate.Session session, org.json.JSONObject data, String key, Long docId) throws Exception {
        if (data.isNull("disposisiSop") || data.getString("disposisiSop").trim().isEmpty()) return null;
        ais.database.model.sop.DisposisiSop disp = (ais.database.model.sop.DisposisiSop) session.get(ais.database.model.sop.DisposisiSop.class, Long.parseLong(data.getString("disposisiSop").trim()));
        if (disp == null) return null;
        org.json.JSONObject prop = (disp.getProperti()==null || disp.getProperti().trim().isEmpty()) ? new org.json.JSONObject() : new org.json.JSONObject(disp.getProperti());
        org.json.JSONObject entry = prop.isNull(key) ? new org.json.JSONObject() : prop.getJSONObject(key);
        entry.put("id", docId); prop.put(key, entry);
        disp.setProperti(prop.toString()); session.update(disp);
        return disp;
    }
    static boolean has(Object o, String m){ try { o.getClass().getMethod(m); return true; } catch(Exception e){ return false; } }
    static Object invoke(Object o, String m){ try { return o.getClass().getMethod(m).invoke(o); } catch(Exception e){ return null; } }
%>
