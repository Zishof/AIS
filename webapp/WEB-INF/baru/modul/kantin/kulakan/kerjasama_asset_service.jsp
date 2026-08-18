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
<%@page import="ais.database.model.asset.PerjanjianKerjasamaMasterAsset"%>
<%@page import="ais.database.model.asset.PerjanjianKerjasamaMasterAssetDetail"%>
<%@page import="ais.database.model.asset.PermintaanPengadaanMasterAsset"%>
<%@page import="ais.database.model.asset.PermintaanPengadaanMasterAssetDetail"%>
<%@page import="ais.database.model.asset.PenyediaAsset"%>
<%@page import="ais.database.model.asset.JenisPerjanjianKerjasamaAsset"%>
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
        result.put("jenisPerjanjian", listCombo(session, JenisPerjanjianKerjasamaAsset.class, "nama"));
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

    } else if ("listPr".equals(aksi)) {
        Criteria c = session.createCriteria(PermintaanPengadaanMasterAsset.class)
                .add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.isNull("pemesananPengadaanMasterAsset"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.desc("id")).setMaxResults(200);
        if (lockToko) c.add(Restrictions.eq("toko", scopeToko));
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
            JSONObject j=new JSONObject();
            j.put("prDetailId", dt.getId());
            j.put("masterAsset", dt.getMasterAsset()==null?"":(""+dt.getMasterAsset().getId()));
            j.put("masterAssetNama", dt.getMasterAsset()==null?"":(s(dt.getMasterAsset().getKode())+" - "+s(dt.getMasterAsset().getNama())));
            j.put("jumlah", dt.getJumlah()==null?0.0:dt.getJumlah()); j.put("hargaBeli", dt.getHargaBeli()==null?0.0:dt.getHargaBeli()); j.put("keterangan", s(dt.getKeterangan()));
            lines.put(j);
        }
        h.put("lines", lines); result.put("data", h); result.put("status","00");

    } else if ("list".equals(aksi)) {
        String kode = request.getParameter("kode");
        String tglM = request.getParameter("tglMulai");
        String tglA = request.getParameter("tglAkhir");
        String status = request.getParameter("status");
        Criteria c = session.createCriteria(PerjanjianKerjasamaMasterAsset.class)
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
            PerjanjianKerjasamaMasterAsset p=(PerjanjianKerjasamaMasterAsset)o; JSONObject j=new JSONObject();
            j.put("id", p.getId()); j.put("kode", s(p.getKode())); j.put("nomor", s(p.getNomorPerjanjianKerjasama()));
            j.put("tanggal", p.getTanggalPembuatan()==null?"":dfOut.format(p.getTanggalPembuatan()));
            j.put("penyedia", p.getPenyedia()==null?"":s(p.getPenyedia().getNama()));
            j.put("toko", p.getToko()==null?"":s(p.getToko().getNama()));
            j.put("nilai", hitungNilai(session, p));
            j.put("status", p.getDisetujuiOleh()!=null?"disetujui":"pending");
            j.put("sop", p.getDisposisiSop()!=null);
            Number ji=(Number) session.createCriteria(PerjanjianKerjasamaMasterAssetDetail.class).add(Restrictions.eq("perjanjianKerjasamaMasterAsset", p)).setProjection(Projections.rowCount()).uniqueResult();
            j.put("jumlahItem", ji==null?0:ji.intValue()); arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else if ("detail".equals(aksi)) {
        Long id = Long.parseLong(request.getParameter("id").trim());
        PerjanjianKerjasamaMasterAsset p=(PerjanjianKerjasamaMasterAsset) session.get(PerjanjianKerjasamaMasterAsset.class, id);
        if (p==null){ result.put("status","02"); result.put("message","Data tidak ditemukan"); out.print(result.toString()); return; }
        JSONObject h=new JSONObject();
        h.put("id", p.getId()); h.put("kode", s(p.getKode())); h.put("nomor", s(p.getNomorPerjanjianKerjasama())); h.put("kodeInvoice", s(p.getKodeInvoice())); h.put("keterangan", s(p.getKeterangan()));
        h.put("tanggal", p.getTanggalPembuatan()==null?"":dfTgl.format(p.getTanggalPembuatan()));
        h.put("mulai", p.getPengirimanMulai()==null?"":dfTgl.format(p.getPengirimanMulai()));
        h.put("pengiriman", p.getPengirimanPalingLambat()==null?"":dfTgl.format(p.getPengirimanPalingLambat()));
        h.put("satuanKerja", p.getSatuanKerja()==null?"":(""+p.getSatuanKerja().getId()));
        h.put("lokasi", p.getLokasi()==null?"":(""+p.getLokasi().getId()));
        h.put("toko", p.getToko()==null?"":(""+p.getToko().getId()));
        h.put("disposisiSop", p.getDisposisiSop()==null?"":(""+p.getDisposisiSop().getId()));
        h.put("penyedia", p.getPenyedia()==null?"":(""+p.getPenyedia().getId()));
        h.put("jenisPerjanjian", p.getJenisPerjanjianKerjasamaAsset()==null?"":(""+p.getJenisPerjanjianKerjasamaAsset().getId()));
        h.put("ppn", p.getPpn()!=null && p.getPpn());
        h.put("persenPpn", p.getPersenPpn());
        h.put("dp", p.getDp()==null?0.0:p.getDp());
        h.put("catatanKesepakatan", s(p.getCatatanKesepakatan()));
        h.put("formula", (p.getFormula()==null||p.getFormula().trim().isEmpty())?"[]":p.getFormula());
        h.put("status", p.getDisetujuiOleh()!=null?"disetujui":"pending");
        JSONArray lines=new JSONArray();
        for (Object o : session.createCriteria(PerjanjianKerjasamaMasterAssetDetail.class).add(Restrictions.eq("perjanjianKerjasamaMasterAsset", p)).addOrder(Order.asc("id")).list()){
            PerjanjianKerjasamaMasterAssetDetail dt=(PerjanjianKerjasamaMasterAssetDetail)o; JSONObject j=new JSONObject();
            j.put("masterAsset", dt.getMasterAsset()==null?"":(""+dt.getMasterAsset().getId()));
            j.put("masterAssetNama", dt.getMasterAsset()==null?"":(s(dt.getMasterAsset().getKode())+" - "+s(dt.getMasterAsset().getNama())));
            j.put("jumlah", dt.getJumlah()==null?0.0:dt.getJumlah());
            j.put("hargaBeli", dt.getHargaBeli()==null?0.0:dt.getHargaBeli());
            j.put("hargaPotongan", dt.getHargaPotongan()==null?0.0:dt.getHargaPotongan());
            j.put("keterangan", s(dt.getKeterangan()));
            j.put("prDetailId", dt.getPermintaanPengadaanMasterAssetDetail()==null?"":(""+dt.getPermintaanPengadaanMasterAssetDetail().getId()));
            lines.put(j);
        }
        h.put("lines", lines); result.put("data", h); result.put("status","00");

    } else if ("simpan".equals(aksi)) {
        JSONObject data = new JSONObject(request.getParameter("data"));
        Transaction tx = session.beginTransaction();
        try {
            PerjanjianKerjasamaMasterAsset p;
            boolean baru = data.isNull("id") || data.getString("id").trim().isEmpty();
            if (baru){ p=new PerjanjianKerjasamaMasterAsset(); p.setDibuatOleh(tbmuser); p.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate()); p.setAktif(true); }
            else { p=(PerjanjianKerjasamaMasterAsset) session.get(PerjanjianKerjasamaMasterAsset.class, Long.parseLong(data.getString("id").trim())); }
            if (p.getDisetujuiOleh()!=null){ tx.rollback(); result.put("status","03"); result.put("message","PKS sudah disetujui, tidak bisa diubah."); out.print(result.toString()); return; }

            String kode = data.optString("kode","").trim();
            if (kode.isEmpty()) kode = "PKS-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            p.setKode(kode);
            p.setNomorPerjanjianKerjasama(data.optString("nomor","").trim());
            p.setKodeInvoice(data.optString("kodeInvoice","").trim());
            p.setKeterangan(data.optString("keterangan",""));
            if (!data.optString("tanggal","").trim().isEmpty()) p.setTanggalPembuatan(dfTgl.parse(data.getString("tanggal").trim()));
            p.setPengirimanMulai(data.optString("mulai","").trim().isEmpty()?null:dfTgl.parse(data.getString("mulai").trim()));
            p.setPengirimanPalingLambat(data.optString("pengiriman","").trim().isEmpty()?null:dfTgl.parse(data.getString("pengiriman").trim()));
            p.setSatuanKerja(data.optString("satuanKerja","").trim().isEmpty()?null:(SatuanKerja) session.get(SatuanKerja.class, Long.parseLong(data.getString("satuanKerja").trim())));
            p.setLokasi(data.optString("lokasi","").trim().isEmpty()?null:(Lokasi) session.get(Lokasi.class, Long.parseLong(data.getString("lokasi").trim())));
            if (lockToko) p.setToko(scopeToko);
            else p.setToko(data.optString("toko","").trim().isEmpty()?null:(ais.database.model.inventory.Toko) session.get(ais.database.model.inventory.Toko.class, Long.parseLong(data.getString("toko").trim())));
            p.setPenyedia(data.optString("penyedia","").trim().isEmpty()?null:(PenyediaAsset) session.get(PenyediaAsset.class, Long.parseLong(data.getString("penyedia").trim())));
            p.setJenisPerjanjianKerjasamaAsset(data.optString("jenisPerjanjian","").trim().isEmpty()?null:(JenisPerjanjianKerjasamaAsset) session.get(JenisPerjanjianKerjasamaAsset.class, Long.parseLong(data.getString("jenisPerjanjian").trim())));
            p.setPpn(bln(data,"ppn"));
            p.setPersenPpn(dbl(data,"persenPpn"));
            p.setDp(dbl(data,"dp"));
            p.setCatatanKesepakatan(data.optString("catatanKesepakatan",""));
            p.setFormula(data.isNull("formula")?"[]":data.getString("formula"));

            if (baru) session.save(p); else session.update(p);
            session.flush();
            p.setDisposisiSop(tautkanProperti(session, data, PerjanjianKerjasamaMasterAsset.class.getName(), p.getId())); session.update(p); session.flush();

            JSONArray lines = data.isNull("lines") ? new JSONArray() : data.getJSONArray("lines");
            if (!baru){
                session.createQuery("update PermintaanPengadaanMasterAssetDetail set perjanjianKerjasamaMasterAssetDetail=null where perjanjianKerjasamaMasterAssetDetail in (select id from PerjanjianKerjasamaMasterAssetDetail where perjanjianKerjasamaMasterAsset.id=:pid)").setLong("pid", p.getId()).executeUpdate();
                session.createQuery("delete from PerjanjianKerjasamaMasterAssetDetail where perjanjianKerjasamaMasterAsset.id=:pid").setLong("pid", p.getId()).executeUpdate();
                session.flush();
            }
            for (int i=0;i<lines.length();i++){
                JSONObject l=lines.getJSONObject(i);
                if (l.isNull("masterAsset") || l.getString("masterAsset").trim().isEmpty()) continue;
                double jml=dbl(l,"jumlah"); if (jml<=0) continue;
                PerjanjianKerjasamaMasterAssetDetail dt=new PerjanjianKerjasamaMasterAssetDetail();
                dt.setPerjanjianKerjasamaMasterAsset(p);
                dt.setMasterAsset((MasterAsset) session.get(MasterAsset.class, Long.parseLong(l.getString("masterAsset").trim())));
                dt.setJumlah(jml); dt.setHargaBeli(dbl(l,"hargaBeli")); dt.setHargaPotongan(dbl(l,"hargaPotongan"));
                dt.setKeterangan(l.optString("keterangan",""));
                PermintaanPengadaanMasterAssetDetail prDet = null;
                if (!l.optString("prDetailId","").trim().isEmpty())
                    prDet = (PermintaanPengadaanMasterAssetDetail) session.get(PermintaanPengadaanMasterAssetDetail.class, Long.parseLong(l.getString("prDetailId").trim()));
                if (prDet!=null) dt.setPermintaanPengadaanMasterAssetDetail(prDet);
                session.save(dt);
                session.flush();
                if (prDet!=null){ prDet.setPerjanjianKerjasamaMasterAssetDetail(dt); session.saveOrUpdate(prDet); session.flush(); }
            }
            tx.commit();
            result.put("status","00"); result.put("id", p.getId()); result.put("message","Perjanjian kerjasama tersimpan.");
        } catch (Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/kerjasama_asset_service.jsp:230");} throw ex; }

    } else if ("setujui".equals(aksi)) {
        Long id=Long.parseLong(request.getParameter("id").trim());
        Transaction tx=session.beginTransaction();
        try { PerjanjianKerjasamaMasterAsset p=(PerjanjianKerjasamaMasterAsset) session.get(PerjanjianKerjasamaMasterAsset.class, id);
            if (p!=null){ p.setDisetujuiOleh(tbmuser); p.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate()); session.update(p); }
            tx.commit(); result.put("status","00"); result.put("message","PKS disetujui.");
        } catch(Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/kerjasama_asset_service.jsp:238");} throw ex; }

    } else if ("hapus".equals(aksi)) {
        Long id=Long.parseLong(request.getParameter("id").trim());
        Transaction tx=session.beginTransaction();
        try { PerjanjianKerjasamaMasterAsset p=(PerjanjianKerjasamaMasterAsset) session.get(PerjanjianKerjasamaMasterAsset.class, id);
            if (p!=null && p.getDisetujuiOleh()!=null){ tx.rollback(); result.put("status","03"); result.put("message","Sudah disetujui, tidak bisa dihapus."); out.print(result.toString()); return; }
            if (p!=null){
                session.createQuery("update PermintaanPengadaanMasterAssetDetail set perjanjianKerjasamaMasterAssetDetail=null where perjanjianKerjasamaMasterAssetDetail in (select id from PerjanjianKerjasamaMasterAssetDetail where perjanjianKerjasamaMasterAsset.id=:pid)").setLong("pid", id).executeUpdate();
                session.createQuery("delete from PerjanjianKerjasamaMasterAssetDetail where perjanjianKerjasamaMasterAsset.id=:pid").setLong("pid", id).executeUpdate();
                session.delete(p);
            }
            tx.commit(); result.put("status","00"); result.put("message","PKS dihapus.");
        } catch(Exception ex){ try{tx.rollback();}catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/kerjasama_asset_service.jsp:251");} throw ex; }

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/kulakan/kerjasama_asset_service.jsp:255");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/kerjasama_asset_service.jsp:256");}
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
    static double hitungNilai(Session session, PerjanjianKerjasamaMasterAsset p) {
        try {
            Number sum = (Number) session.createQuery("select sum(d.jumlah*d.hargaBeli - coalesce(d.hargaPotongan,0)) from PerjanjianKerjasamaMasterAssetDetail d where d.perjanjianKerjasamaMasterAsset.id=:id").setLong("id", p.getId()).uniqueResult();
            double nilai = sum==null?0.0:sum.doubleValue();
            if (p.getPpn()!=null && p.getPpn() && p.getPersenPpn()!=null) nilai += nilai * (p.getPersenPpn()/100.0);
            return nilai;
        } catch (Exception e) { return 0.0; }
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
