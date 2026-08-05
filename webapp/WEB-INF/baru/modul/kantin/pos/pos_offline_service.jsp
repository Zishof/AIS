<%@page session="false"%>
<%--
  Endpoint server untuk POS Offline-First (dipakai mesin ais_pos_offline.js, versi JSP & ZK).

  Aksi:
   - data : unduh seluruh produk toko (untuk cache lokal kasir) -> {id,kode,nama,hargaJual,hargaBeli}.
   - sync : terima daftar transaksi tertunda (JSON), persist tiap transaksi secara IDEMPOTEN
            (kode header = clientTrxId; bila sudah ada -> dilewati/duplikat), lalu recompute stok.
            Akuntansi/jurnal mengikuti mekanisme posting yang sudah ada (di luar lingkup endpoint ini).
            Header ditandai sumberTransaksi=OFFLINE_SYNC + waktuSinkron=waktu server saat itu (lihat
            PembelianAnggotaKoperasi), dipakai halaman riwayat_sinkronisasi.jsp untuk membedakan
            transaksi yang sempat tertunda offline dari transaksi online biasa.

  Sesi: currentSession() (dikelola kerangka kerja) -> TIDAK ditutup manual.
  Toko: pedagang dikunci ke tokonya; admin-kantin boleh memakai tokoId dari transaksi.
--%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Produk"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pembelian"%>
<%@page import="ais.database.model.koperasi.PembelianAnggotaKoperasi"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.koperasi.CaraPembayaranKoperasi"%>
<%@page import="ais.action.master.inventory.PriceTagUtil"%>
<%@page import="ais.action.master.inventory.StokKantinUtil"%>
<%!
    /** Null-safe {@code toString()} — dipakai membangun JSON balasan supaya tidak pernah menulis literal "null". */
    static String s(Object o){ return o==null?"":o.toString(); }

    /**
     * Membaca field {@code Long} opsional dari objek transaksi JSON kiriman klien
     * ({@code tokoId}/{@code caraBayar}/{@code member}/{@code produkId}, dst.) — dipakai berkali-kali
     * di jalur {@code aksi=sync} supaya pola "ada &amp; tidak null &amp; parse ke Long" tidak ditulis
     * ulang di tiap field. Mengembalikan {@code null} (BUKAN melempar exception) bila field tidak ada/
     * null; kegagalan parse (field ada tapi bukan angka) TETAP dilempar apa adanya ke pemanggil --
     * setiap pemanggilnya sudah berada di dalam blok try/catch per-transaksi (baris {@code aksi=sync}
     * di bawah), sehingga satu field cacat pada satu transaksi hanya menggagalkan transaksi ITU SAJA
     * (dilaporkan lewat {@code h.put("error", ...)}), bukan seluruh batch sinkronisasi.
     *
     * @param o   objek JSON yang dibaca (baris transaksi atau baris item).
     * @param key nama field yang dicari.
     * @return nilai {@link Long}, atau {@code null} bila field tidak ada/null.
     */
    // FIX kompilasi JSP "Unhandled exception type JSONException": org.json.JSONException di versi
    // library ini adalah checked exception (extends Exception, bukan RuntimeException), jadi
    // o.get(key) WAJIB dideklarasikan throws -- sesuai javadoc di atas, kegagalan parse memang
    // SENGAJA dibiarkan menembus ke pemanggil (masing2 sudah dalam try/catch per-transaksi).
    static Long optLong(JSONObject o, String key) throws JSONException {
        return (o.has(key) && !o.isNull(key)) ? Long.valueOf(o.get(key).toString()) : null;
    }

    /**
     * Mencari id toko yang berlaku untuk permintaan ini: PRIORITAS PERTAMA pedagang yang sedang login
     * (dikunci ke tokonya sendiri, tidak bisa dipalsukan lewat parameter), baru kalau tidak ada
     * (pengguna admin-kantin yang boleh memilih toko manapun) jatuh ke parameter query {@code toko}.
     * Mengembalikan {@code null} bila keduanya tidak tersedia/parameter tidak valid — pemanggil di
     * {@code aksi=data}/{@code aksi=sync} menangani {@code null} ini sebagai "toko tidak diketahui"
     * masing-masing dengan caranya sendiri (bukan dianggap error di sini).
     *
     * @param request request HTTP yang sedang diproses.
     * @return id toko yang berlaku, atau {@code null} bila tidak bisa ditentukan.
     */
    private Long resolveToko(HttpServletRequest request) {
        Tbmuser u = Common.getCurrentUser(request);
        if (u != null && u.getPedagang() != null && u.getPedagang().getToko() != null) return u.getPedagang().getToko().getId();
        String p = request.getParameter("toko");
        if (p != null && p.trim().length() > 0) { try { return Long.valueOf(p.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/pos_offline_service.jsp:34");} }
        return null;
    }

    /**
     * Mem-parse timestamp waktu transaksi yang dikirim klien (perangkat kasir), mencoba beruntun 4
     * format umum karena klien JS bisa mengirim salah satu dari beberapa representasi tanggal
     * tergantung bagaimana {@code Date} di-serialize di browser: ISO 8601 dengan milidetik, ISO 8601
     * tanpa milidetik, format SQL polos, atau tanggal saja tanpa jam. Karakter penanda zona waktu UTC
     * ({@code "Z"}) dibuang lebih dulu supaya pola ISO di atas cocok (perangkat kasir dianggap
     * melaporkan waktu lokalnya, bukan dikonversi ke UTC di sini).
     *
     * @param s teks waktu dari klien; boleh {@code null}.
     * @return waktu hasil parse pada format pertama yang cocok; {@code new Date()} (waktu SERVER saat
     *         ini) sebagai fallback bila {@code s} null atau tidak cocok format manapun -- gagal-aman,
     *         supaya satu timestamp yang tidak terbaca tidak menggagalkan penyimpanan transaksinya.
     */
    private Date parseWaktu(String s) {
        if (s == null) return new Date();
        String v = s.trim().replace("Z", "");
        String[] pola = { "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd" };
        for (int i = 0; i < pola.length; i++) {
            try { return new SimpleDateFormat(pola[i]).parse(v); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/pos_offline_service.jsp:42");}
        }
        return new Date();
    }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return;
    }
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    Long tokoScope = resolveToko(request);

    if ("data".equals(aksi)) {
        JSONArray arr = new JSONArray();
        for (Object o : PriceTagUtil.listProduk(session, tokoScope, null)) {
            Produk p = (Produk) o; JSONObject j = new JSONObject();
            j.put("id", p.getId()); j.put("kode", s(p.getKode())); j.put("nama", s(p.getNama()));
            j.put("hargaJual", p.getHargaJual()==null?0:p.getHargaJual());
            j.put("hargaBeli", p.getHargaBeli()==null?0:p.getHargaBeli());
            arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else if ("sync".equals(aksi)) {
        String body = request.getParameter("transaksi");
        if (body == null || body.trim().length() == 0) { result.put("status","03"); result.put("message","Tidak ada transaksi."); out.print(result.toString()); return; }
        JSONArray daftar = new JSONArray(body);
        JSONArray hasil = new JSONArray();
        for (int i = 0; i < daftar.length(); i++) {
            JSONObject trx = daftar.getJSONObject(i);
            String clientTrxId = trx.optString("clientTrxId", null);
            JSONObject h = new JSONObject();
            h.put("clientTrxId", clientTrxId);
            if (clientTrxId == null || clientTrxId.length() == 0) { h.put("error","clientTrxId kosong"); hasil.put(h); continue; }
            try {
                // IDEMPOTEN: lewati bila transaksi ini sudah pernah tersimpan.
                Object ada = session.createCriteria(PembelianAnggotaKoperasi.class)
                        .add(Restrictions.eq("kode", clientTrxId)).setMaxResults(1).uniqueResult();
                if (ada != null) { h.put("duplikat", true); hasil.put(h); continue; }

                Long tokoId = tokoScope != null ? tokoScope : optLong(trx, "tokoId");
                if (tokoId == null) { h.put("error","toko tidak diketahui"); hasil.put(h); continue; }
                Toko toko = (Toko) session.get(Toko.class, tokoId);
                Date waktu = parseWaktu(trx.optString("waktu", null));

                // Fase 1 (perbaikan celah lama): sebelumnya caraBayar/member/diskon/cashback per-item
                // DIBUANG diam-diam di sini walau payload klien sudah membawanya (lihat posTrxOffline()
                // di _pos.jsp) -- transaksi offline yang tersinkron kehilangan tautan member/wallet,
                // catatan metode pembayaran, dan diskon per-item. Diperbaiki dengan memetakan seluruh
                // field ini, sama seperti jalur online (KantinHelper.bayar()).
                Long caraBayarId = optLong(trx, "caraBayar");
                CaraPembayaranKoperasi caraBayar = caraBayarId == null ? null : (CaraPembayaranKoperasi) session.get(CaraPembayaranKoperasi.class, caraBayarId);
                Long memberId = optLong(trx, "member");
                AnggotaKoperasi member = memberId == null ? null : (AnggotaKoperasi) session.get(AnggotaKoperasi.class, memberId);

                PembelianAnggotaKoperasi header = new PembelianAnggotaKoperasi();
                header.setKode(clientTrxId);                 // kunci idempoten
                header.setToko(toko);
                header.setTanggalPembayaran(waktu);
                header.setTotalBiaya(trx.has("total") ? trx.getDouble("total") : 0d);
                header.setBayar(trx.has("bayar") ? trx.getDouble("bayar") : 0d);
                header.setBayarTunai(trx.has("bayar") ? trx.getDouble("bayar") : 0d);
                header.setKembalian(trx.has("kembalian") ? trx.getDouble("kembalian") : 0d);
                header.setCaraPembayaranKoperasi(caraBayar);
                header.setAnggotaKoperasi(member);
                header.setKeterangan("POS Offline (sinkron otomatis)");
                header.setSumberTransaksi(PembelianAnggotaKoperasi.SUMBER_OFFLINE_SYNC);
                header.setWaktuSinkron(new Date()); // waktu SERVER menerima sinkron -- beda dgn "waktu" (waktu transaksi asli di kasir, dipakai tanggalPembayaran di atas)
                try { header.setOleh(tbmuser.getUserId()); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/pos_offline_service.jsp:104");}
                // Simpan header DULU (butuh id sebelum baris Pembelian di bawah bisa mereferensikannya
                // sebagai FK) -- totalDiskon/totalCashback diisi & disimpan ulang setelah loop item.
                Common.refreshSaveOrUpdate(session, header);

                JSONArray items = trx.optJSONArray("items");
                Set<Long> produkTerjual = new HashSet<Long>();
                double totalDiskonHeader = 0d;
                double totalCashbackHeader = 0d;
                if (items != null) {
                    for (int k = 0; k < items.length(); k++) {
                        JSONObject it = items.getJSONObject(k);
                        Long produkId = optLong(it, "produkId");
                        if (produkId == null) continue;
                        double qty = it.has("qty") ? it.getDouble("qty") : 0d;
                        double harga = it.has("harga") ? it.getDouble("harga") : 0d;
                        double diskon = it.has("diskon") ? it.getDouble("diskon") : 0d;
                        double cashback = it.has("cashback") ? it.getDouble("cashback") : 0d;
                        totalDiskonHeader += diskon;
                        totalCashbackHeader += cashback;
                        Pembelian d = new Pembelian();
                        d.setPembelianAnggotaKoperasi(header);
                        d.setProduk((Produk) session.get(Produk.class, produkId));
                        d.setToko(toko);
                        d.setKios(toko == null ? null : toko.getKode());
                        d.setQty(qty);
                        d.setHargaSatuan(harga);
                        d.setHargaJual(harga);
                        d.setDiskon(Double.valueOf(diskon));
                        d.setCashback(Double.valueOf(cashback));
                        d.setTotal((qty * harga) - diskon);
                        d.setWaktu(waktu);
                        Common.refreshSaveOrUpdate(session, d);
                        produkTerjual.add(produkId);
                    }
                }
                header.setTotalDiskon(Double.valueOf(totalDiskonHeader));
                header.setTotalCashback(Double.valueOf(totalCashbackHeader));
                Common.refreshSaveOrUpdate(session, header);
                // Update stok (cache produk.stok) tiap produk terjual — reuse util native (tanpa sesi).
                for (Long pid : produkTerjual) {
                    try { StokKantinUtil.recomputeStokProdukViaSql(pid); } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/pos/pos_offline_service.jsp:132"); }
                }
                h.put("ok", true);
                h.put("waktuSinkron", header.getWaktuSinkron() == null ? null : header.getWaktuSinkron().getTime());
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/pos/pos_offline_service.jsp:136");
                h.put("error", e.getMessage());
            }
            hasil.put(h);
        }
        result.put("status","00"); result.put("hasil", hasil);

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/pos/pos_offline_service.jsp:145");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/pos_offline_service.jsp:146");}
}
out.print(result.toString());
%>
