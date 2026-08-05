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
<%@page import="ais.database.model.asset.PenerimaanPengadaanMasterAsset"%>
<%@page import="ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail"%>
<%@page import="ais.database.model.asset.PemesananPengadaanMasterAsset"%>
<%@page import="ais.database.model.asset.PemesananPengadaanMasterAssetDetail"%>
<%@page import="ais.database.model.asset.PenyediaAsset"%>
<%@page import="ais.database.model.asset.JenisPenerimaanBarang"%>
<%@page import="ais.database.model.asset.DetailTransaksiAsset"%>
<%@page import="ais.action.master.library.util.LibraryUtil"%>
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
        result.put("satuanKerja", listCombo(session, SatuanKerja.class, "nama"));
        result.put("lokasi", listCombo(session, Lokasi.class, "nama"));
        result.put("penyedia", listCombo(session, PenyediaAsset.class, "nama"));
        result.put("jenisPenerimaan", listCombo(session, JenisPenerimaanBarang.class, "nama"));
        result.put("pemilikAsset", listCombo(session, ais.database.model.asset.PemilikAsset.class, "nama"));
        result.put("toko", listCombo(session, ais.database.model.inventory.Toko.class, "nama"));
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

    } else if ("cariRuang".equals(aksi)) {
        String q = request.getParameter("q"); if (q==null) q="";
        Criteria c = session.createCriteria(ais.database.model.Ruang.class).addOrder(Order.asc("nama")).setMaxResults(40);
        if (!q.trim().isEmpty()) c.add(Restrictions.ilike("nama", q.trim(), MatchMode.ANYWHERE));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) { ais.database.model.Ruang w=(ais.database.model.Ruang)o; JSONObject j=new JSONObject();
            j.put("id", w.getId()); j.put("nama", s(w.getNama())); arr.put(j); }
        result.put("data", arr); result.put("status","00");

    } else if ("listPo".equals(aksi)) {
        // PO disetujui = siap diterima
        Criteria c = session.createCriteria(PemesananPengadaanMasterAsset.class)
                .add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.desc("id")).setMaxResults(200);
        if (lockToko) c.add(Restrictions.eq("toko", scopeToko));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) { PemesananPengadaanMasterAsset p=(PemesananPengadaanMasterAsset)o; JSONObject j=new JSONObject();
            j.put("id", p.getId()); j.put("kode", s(p.getKode())); j.put("nilai", p.getNilai()==null?0.0:p.getNilai());
            j.put("satuanKerja", p.getSatuanKerja()==null?"":s(p.getSatuanKerja().getNama())); arr.put(j); }
        result.put("data", arr); result.put("status","00");

    } else if ("importPo".equals(aksi)) {
        Long poId = Long.parseLong(request.getParameter("id").trim());
        PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, poId);
        JSONObject h = new JSONObject();
        if (po!=null){
            h.put("poId", ""+po.getId());
            h.put("satuanKerja", po.getSatuanKerja()==null?"":(""+po.getSatuanKerja().getId()));
            h.put("lokasi", po.getLokasi()==null?"":(""+po.getLokasi().getId()));
            h.put("toko", po.getToko()==null?"":(""+po.getToko().getId()));
            h.put("keterangan", s(po.getKeterangan()));
        }
        JSONArray lines = new JSONArray();
        for (Object o : session.createCriteria(PemesananPengadaanMasterAssetDetail.class).add(Restrictions.eq("pemesananPengadaanMasterAsset", po)).addOrder(Order.asc("id")).list()){
            PemesananPengadaanMasterAssetDetail dt=(PemesananPengadaanMasterAssetDetail)o;
            JSONObject j=new JSONObject();
            j.put("poDetailId", dt.getId());
            j.put("masterAsset", dt.getMasterAsset()==null?"":(""+dt.getMasterAsset().getId()));
            j.put("masterAssetNama", dt.getMasterAsset()==null?"":(s(dt.getMasterAsset().getKode())+" - "+s(dt.getMasterAsset().getNama())));
            j.put("jumlah", dt.getJumlah()==null?0.0:dt.getJumlah());
            j.put("diterima", dt.getJumlah()==null?0.0:dt.getJumlah());
            j.put("hargaBeli", dt.getHargaBeli()==null?0.0:dt.getHargaBeli());
            j.put("hargaPotongan", dt.getHargaPotongan());
            j.put("diskonPersen", dt.getDiskonDalamBentukPersen()!=null && dt.getDiskonDalamBentukPersen());
            j.put("persenPpn", dt.getPersenPpn()); j.put("persenPph", dt.getPersenPph());
            j.put("keterangan", s(dt.getKeterangan()));
            lines.put(j);
        }
        h.put("lines", lines); result.put("data", h); result.put("status","00");

    } else if ("list".equals(aksi)) {
        String kode = request.getParameter("kode");
        String tglM = request.getParameter("tglMulai");
        String tglA = request.getParameter("tglAkhir");
        String status = request.getParameter("status");
        Criteria c = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.desc("id")).setMaxResults(300);
        if (lockToko) c.add(Restrictions.eq("toko", scopeToko));
        if (kode!=null && !kode.trim().isEmpty()) c.add(Restrictions.ilike("kode", kode.trim(), MatchMode.ANYWHERE));
        if (tglM!=null && !tglM.trim().isEmpty()) c.add(Restrictions.ge("tanggalPembuatan", dfTgl.parse(tglM.trim())));
        if (tglA!=null && !tglA.trim().isEmpty()) { Calendar cal=Calendar.getInstance(); cal.setTime(dfTgl.parse(tglA.trim())); cal.add(Calendar.DAY_OF_MONTH,1); c.add(Restrictions.lt("tanggalPembuatan", cal.getTime())); }
        if ("disetujui".equals(status)) c.add(Restrictions.isNotNull("disetujuiOleh"));
        else if ("pending".equals(status)) c.add(Restrictions.isNull("disetujuiOleh"));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            PenerimaanPengadaanMasterAsset p=(PenerimaanPengadaanMasterAsset)o; JSONObject j=new JSONObject();
            j.put("id", p.getId()); j.put("kode", s(p.getKode())); j.put("kodeTagihan", s(p.getKodeTagihan()));
            j.put("tanggal", p.getTanggalPembuatan()==null?"":dfOut.format(p.getTanggalPembuatan()));
            j.put("penyedia", p.getPenyedia()==null?"":s(p.getPenyedia().getNama()));
            j.put("toko", p.getToko()==null?"":s(p.getToko().getNama()));
            j.put("nilai", p.getNilai()==null?0.0:p.getNilai());
            j.put("status", p.getDisetujuiOleh()!=null?"disetujui":"pending");
            j.put("sop", p.getDisposisiSop()!=null);
            Number ji=(Number) session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class).add(Restrictions.eq("penerimaanPengadaanMasterAsset", p)).setProjection(Projections.rowCount()).uniqueResult();
            j.put("jumlahItem", ji==null?0:ji.intValue()); arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else if ("detail".equals(aksi)) {
        Long id = Long.parseLong(request.getParameter("id").trim());
        PenerimaanPengadaanMasterAsset p=(PenerimaanPengadaanMasterAsset) session.get(PenerimaanPengadaanMasterAsset.class, id);
        if (p==null){ result.put("status","02"); result.put("message","Data tidak ditemukan"); out.print(result.toString()); return; }
        JSONObject h=new JSONObject();
        h.put("id", p.getId()); h.put("kode", s(p.getKode())); h.put("kodeTagihan", s(p.getKodeTagihan())); h.put("keterangan", s(p.getKeterangan()));
        h.put("tanggal", p.getTanggalPembuatan()==null?"":dfTgl.format(p.getTanggalPembuatan()));
        h.put("tanggalTagihan", p.getTanggalTagihan()==null?"":dfTgl.format(p.getTanggalTagihan()));
        h.put("satuanKerja", p.getSatuanKerja()==null?"":(""+p.getSatuanKerja().getId()));
        h.put("lokasi", p.getLokasi()==null?"":(""+p.getLokasi().getId()));
        h.put("toko", p.getToko()==null?"":(""+p.getToko().getId()));
        h.put("disposisiSop", p.getDisposisiSop()==null?"":(""+p.getDisposisiSop().getId()));
        h.put("pemilikAsset", p.getPemilikAsset()==null?"":(""+p.getPemilikAsset().getId()));
        h.put("ruang", p.getRuang()==null?"":(""+p.getRuang().getId()));
        h.put("ruangNama", p.getRuang()==null?"":s(p.getRuang().getNama()));
        h.put("tanpaAnggaran", p.getTanpaAnggaran()!=null && p.getTanpaAnggaran());
        h.put("penyedia", p.getPenyedia()==null?"":(""+p.getPenyedia().getId()));
        h.put("jenisPenerimaan", p.getJenisPenerimaanBarang()==null?"":(""+p.getJenisPenerimaanBarang().getId()));
        h.put("poId", p.getPemesananPengadaanMasterAsset()==null?"":(""+p.getPemesananPengadaanMasterAsset().getId()));
        h.put("kurir", s(p.getKurir()));
        h.put("tampaPemesanan", p.getTampaPemesanan()!=null && p.getTampaPemesanan());
        h.put("status", p.getDisetujuiOleh()!=null?"disetujui":"pending");
        JSONArray lines=new JSONArray();
        for (Object o : session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class).add(Restrictions.eq("penerimaanPengadaanMasterAsset", p)).addOrder(Order.asc("id")).list()){
            PenerimaanPengadaanMasterAssetDetail dt=(PenerimaanPengadaanMasterAssetDetail)o; JSONObject j=new JSONObject();
            j.put("masterAsset", dt.getMasterAsset()==null?"":(""+dt.getMasterAsset().getId()));
            j.put("masterAssetNama", dt.getMasterAsset()==null?"":(s(dt.getMasterAsset().getKode())+" - "+s(dt.getMasterAsset().getNama())));
            j.put("jumlah", dt.getJumlah()==null?0.0:dt.getJumlah());
            j.put("diterima", dt.getDiterima()==null?0.0:dt.getDiterima());
            j.put("hargaBeli", dt.getHargaBeli()==null?0.0:dt.getHargaBeli());
            j.put("hargaPotongan", dt.getHargaPotongan());
            j.put("diskonPersen", dt.getDiskonDalamBentukPersen()!=null && dt.getDiskonDalamBentukPersen());
            j.put("persenPpn", dt.getPersenPpn()); j.put("persenPph", dt.getPersenPph());
            j.put("kondisi", s(dt.getKondisi())); j.put("keterangan", s(dt.getKeterangan()));
            j.put("poDetailId", dt.getPemesananPengadaanMasterAssetDetail()==null?"":(""+dt.getPemesananPengadaanMasterAssetDetail().getId()));
            lines.put(j);
        }
        h.put("lines", lines); result.put("data", h); result.put("status","00");

    } else if ("simpan".equals(aksi)) {
        JSONObject data = new JSONObject(request.getParameter("data"));
        Transaction tx = session.beginTransaction();
        try {
            PenerimaanPengadaanMasterAsset p;
            boolean baru = data.isNull("id") || data.getString("id").trim().isEmpty();
            if (baru){ p=new PenerimaanPengadaanMasterAsset(); p.setDibuatOleh(tbmuser); p.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate()); p.setAktif(true); }
            else { p=(PenerimaanPengadaanMasterAsset) session.get(PenerimaanPengadaanMasterAsset.class, Long.parseLong(data.getString("id").trim())); }
            if (p.getDisetujuiOleh()!=null){ tx.rollback(); result.put("status","03"); result.put("message","BAST sudah disetujui, tidak bisa diubah."); out.print(result.toString()); return; }

            String kode = data.optString("kode","").trim();
            if (kode.isEmpty()) kode = "BAST-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            p.setKode(kode);
            p.setKodeTagihan(data.optString("kodeTagihan","").trim());
            p.setKeterangan(data.optString("keterangan",""));
            if (!data.optString("tanggal","").trim().isEmpty()) p.setTanggalPembuatan(dfTgl.parse(data.getString("tanggal").trim()));
            p.setTanggalTagihan(data.optString("tanggalTagihan","").trim().isEmpty()?null:dfTgl.parse(data.getString("tanggalTagihan").trim()));
            p.setSatuanKerja(data.optString("satuanKerja","").trim().isEmpty()?null:(SatuanKerja) session.get(SatuanKerja.class, Long.parseLong(data.getString("satuanKerja").trim())));
            p.setLokasi(data.optString("lokasi","").trim().isEmpty()?null:(Lokasi) session.get(Lokasi.class, Long.parseLong(data.getString("lokasi").trim())));
            if (lockToko) p.setToko(scopeToko);
            else p.setToko(data.optString("toko","").trim().isEmpty()?null:(ais.database.model.inventory.Toko) session.get(ais.database.model.inventory.Toko.class, Long.parseLong(data.getString("toko").trim())));
            p.setPenyedia(data.optString("penyedia","").trim().isEmpty()?null:(PenyediaAsset) session.get(PenyediaAsset.class, Long.parseLong(data.getString("penyedia").trim())));
            p.setJenisPenerimaanBarang(data.optString("jenisPenerimaan","").trim().isEmpty()?null:(JenisPenerimaanBarang) session.get(JenisPenerimaanBarang.class, Long.parseLong(data.getString("jenisPenerimaan").trim())));
            p.setPemilikAsset(data.optString("pemilikAsset","").trim().isEmpty()?null:(ais.database.model.asset.PemilikAsset) session.get(ais.database.model.asset.PemilikAsset.class, Long.parseLong(data.getString("pemilikAsset").trim())));
            p.setRuang(data.optString("ruang","").trim().isEmpty()?null:(ais.database.model.Ruang) session.get(ais.database.model.Ruang.class, Long.parseLong(data.getString("ruang").trim())));
            p.setTanpaAnggaran(data.optBoolean("tanpaAnggaran", false));
            p.setPemesananPengadaanMasterAsset(data.optString("poId","").trim().isEmpty()?null:(PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, Long.parseLong(data.getString("poId").trim())));
            p.setKurir(data.optString("kurir",""));
            p.setTampaPemesanan(bln(data,"tampaPemesanan"));

            JSONArray lines = data.isNull("lines") ? new JSONArray() : data.getJSONArray("lines");
            double nilai = 0;
            for (int i=0;i<lines.length();i++){
                JSONObject l=lines.getJSONObject(i);
                PenerimaanPengadaanMasterAssetDetail dt=new PenerimaanPengadaanMasterAssetDetail();
                dt.setDiterima(dbl(l,"diterima")); dt.setHargaBeli(dbl(l,"hargaBeli"));
                dt.setHargaPotongan(dbl(l,"hargaPotongan")); dt.setDiskonDalamBentukPersen(bln(l,"diskonPersen"));
                dt.setPersenPpn(dbl(l,"persenPpn")); dt.setPersenPph(dbl(l,"persenPph"));
                nilai += dt.getHargaTotal();
            }
            p.setNilai(nilai);
            if (baru) session.save(p); else session.update(p);
            session.flush();
            p.setDisposisiSop(tautkanProperti(session, data, PenerimaanPengadaanMasterAsset.class.getName(), p.getId())); session.update(p); session.flush();

            if (!baru){
                session.createQuery("delete from PenerimaanPengadaanMasterAssetDetail where penerimaanPengadaanMasterAsset.id=:pid").setLong("pid", p.getId()).executeUpdate();
                session.flush();
            }
            for (int i=0;i<lines.length();i++){
                JSONObject l=lines.getJSONObject(i);
                if (l.isNull("masterAsset") || l.getString("masterAsset").trim().isEmpty()) continue;
                double dit=dbl(l,"diterima");
                PenerimaanPengadaanMasterAssetDetail dt=new PenerimaanPengadaanMasterAssetDetail();
                dt.setPenerimaanPengadaanMasterAsset(p);
                dt.setMasterAsset((MasterAsset) session.get(MasterAsset.class, Long.parseLong(l.getString("masterAsset").trim())));
                dt.setJumlah(dbl(l,"jumlah")); dt.setDiterima(dit);
                dt.setHargaBeli(dbl(l,"hargaBeli")); dt.setHargaPotongan(dbl(l,"hargaPotongan")); dt.setDiskonDalamBentukPersen(bln(l,"diskonPersen"));
                dt.setPersenPpn(dbl(l,"persenPpn")); dt.setPersenPph(dbl(l,"persenPph"));
                dt.setKondisi(l.optString("kondisi","")); dt.setKeterangan(l.optString("keterangan",""));
                if (!l.optString("poDetailId","").trim().isEmpty())
                    dt.setPemesananPengadaanMasterAssetDetail((PemesananPengadaanMasterAssetDetail) session.get(PemesananPengadaanMasterAssetDetail.class, Long.parseLong(l.getString("poDetailId").trim())));
                session.save(dt);
            }
            tx.commit();
            result.put("status","00"); result.put("id", p.getId()); result.put("message","Penerimaan tersimpan.");
        } catch (Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/penerimaan_asset_service.jsp:260");} throw ex; }

    } else if ("setujui".equals(aksi)) {
        Long id=Long.parseLong(request.getParameter("id").trim());
        Transaction tx=session.beginTransaction();
        try {
            PenerimaanPengadaanMasterAsset p=(PenerimaanPengadaanMasterAsset) session.get(PenerimaanPengadaanMasterAsset.class, id);
            if (p==null){ tx.rollback(); result.put("status","02"); result.put("message","Data tidak ditemukan"); out.print(result.toString()); return; }
            p.setDisetujuiOleh(tbmuser); p.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate()); session.update(p);
            // Idempoten: bersihkan entri stok lama utk BAST ini lalu buat ulang
            session.createSQLQuery("delete from asset.detail_transaksi_asset where penerimaan_pengadaan_master_asset_detail in (select id from asset.penerimaan_pengadaan_master_asset_detail where penerimaan_pengadaan_master_asset = :id)").setLong("id", id).executeUpdate();
            session.flush();
            // Buat stok masuk (DetailTransaksiAsset = BELI_MASUK) per item + sinkron ke kantin (REUSE logika ZK)
            for (Object o : session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class).add(Restrictions.eq("penerimaanPengadaanMasterAsset", p)).list()){
                PenerimaanPengadaanMasterAssetDetail dt=(PenerimaanPengadaanMasterAssetDetail)o;
                DetailTransaksiAsset dta=new DetailTransaksiAsset();
                dta.setPenerimaanPengadaanMasterAssetDetail(dt);
                dta.setQtyBonus(0.0);
                dta.setMasterAsset(dt.getMasterAsset());
                dta.setKeterangan("Transaksi Terima Barang/Jasa");
                dta.setKodeTransaksi(LibraryUtil.BELI_MASUK);
                dta.setPemilikAsset(p.getPemilikAsset());
                dta.setLokasi(p.getLokasi());
                dta.setRuang(p.getRuang());
                dta.setQty(dt.getDiterima());
                dta.setTanggal(p.getTanggalPembuatan());
                session.save(dta);
                session.flush();
                try { ais.action.master.inventory.KantinAssetSyncUtil.syncPengadaanDariBast(session, dt); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/penerimaan_asset_service.jsp:288");}
            }
            tx.commit(); result.put("status","00"); result.put("message","BAST disetujui & stok masuk dicatat.");
        } catch(Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/penerimaan_asset_service.jsp:291");} throw ex; }

    } else if ("hapus".equals(aksi)) {
        Long id=Long.parseLong(request.getParameter("id").trim());
        Transaction tx=session.beginTransaction();
        try { PenerimaanPengadaanMasterAsset p=(PenerimaanPengadaanMasterAsset) session.get(PenerimaanPengadaanMasterAsset.class, id);
            if (p!=null && p.getDisetujuiOleh()!=null){ tx.rollback(); result.put("status","03"); result.put("message","Sudah disetujui, tidak bisa dihapus."); out.print(result.toString()); return; }
            if (p!=null){
                session.createQuery("delete from PenerimaanPengadaanMasterAssetDetail where penerimaanPengadaanMasterAsset.id=:pid").setLong("pid", id).executeUpdate();
                session.delete(p);
            }
            tx.commit(); result.put("status","00"); result.put("message","BAST dihapus.");
        } catch(Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/penerimaan_asset_service.jsp:303");} throw ex; }

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/kulakan/penerimaan_asset_service.jsp:307");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/penerimaan_asset_service.jsp:308");}
}
out.print(result.toString());
%>
<%!
    @SuppressWarnings({"unchecked","rawtypes"})
    static JSONArray listCombo(Session session, Class cls, String orderProp) throws Exception {
        JSONArray arr = new JSONArray();
        for (Object o : session.createCriteria(cls).addOrder(Order.asc(orderProp)).setMaxResults(3000).list()) {
            JSONObject j = new JSONObject();
            j.put("id", invoke(o, "getId"));
            Object nama = has(o,"getNama") ? invoke(o,"getNama") : invoke(o,"getKode");
            j.put("nama", nama==null?"":nama.toString());
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
