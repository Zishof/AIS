package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.SQLQuery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.action.master.koperasi.util.KantinDiskonEngine;
import ais.action.servlet.api.KantinHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Toko Online Member (Beranda Anggota) — konversi dari {@code modul/kantin/_beranda_anggota + toko_online}.
 *
 * <p>Halaman belanja swalayan untuk anggota yang sedang login: lihat saldo, pilih tenant, masukkan
 * produk ke keranjang (boleh dari beberapa tenant sekaligus), diskon promo dihitung otomatis, lalu
 * bayar dari saldo. Penyimpanan memakai ulang {@link KantinHelper#bayar} (langsung) atau
 * {@link KantinHelper#draft_bayar} (bila cara bayar manual = bayar di kasir).</p>
 */
public class BerandaAnggotaKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host;

    private AnggotaKoperasi member;
    private double memberSaldo;
    private Long memberJenisId;
    private Long memberTipeId;

    private Combobox cboToko;
    private Combobox cboBayar;
    private Textbox txtCatatan;
    private Textbox txtCariProduk;
    private Long tokoIdAktif;
    private String tokoNamaAktif;

    private Grid grdProduk;
    private Grid grdKeranjang;
    private Label lblSaldo;
    private Label lblTotal;

    private final List<Item> cart = new ArrayList<Item>();
    /** Aturan diskon aktif; perhitungannya milik {@link KantinDiskonEngine}. */
    private List<KantinDiskonEngine.Aturan> rules = new ArrayList<KantinDiskonEngine.Aturan>();

    /**
     * Tipe implementasi bersarang {@link Item} milik {@link BerandaAnggotaKantinAction}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * BerandaAnggotaKantinAction}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String kode}, {@code
     * String nama}, {@code double harga}, {@code Long idToko}, {@code String namaToko}, {@code int jumlah}, {@code
     * double diskon}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see BerandaAnggotaKantinAction
     */
    private static final class Item {
        final Long id;
        final String kode;
        final String nama;
        final double harga;
        final Long idToko;
        final String namaToko;
        int jumlah;
        double diskon;
        double cashback;
        Long aturanDiskonId;

        Item(Long id, String kode, String nama, double harga, Long idToko, String namaToko) {
            this.id = id;
            this.kode = kode;
            this.nama = nama;
            this.harga = harga;
            this.idToko = idToko;
            this.namaToko = namaToko;
            this.jumlah = 1;
        }
    }

    // ======================== Lifecycle ========================

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();

        try {
            ais.database.model.Tbmuser u = Common.getCurrentUser();
            member = u == null ? null : u.getAnggotaKoperasi();
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/BerandaAnggotaKantinAction.java:118");
        }

        if (host == null) {
            return;
        }
        if (member == null) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='padding:24px;text-align:center;color:#64748b;'>"
                            + "<div style='font-size:16px;font-weight:800;color:#0f172a;'>Akses Khusus Anggota</div>"
                            + "<div style='margin-top:6px;font-size:12px;'>Halaman belanja ini hanya untuk Anggota Koperasi yang sudah login.</div></div>"));
            return;
        }

        memberJenisId = member.getJenisAnggotaKoperasi() == null ? null : member.getJenisAnggotaKoperasi().getId();
        memberTipeId = member.getTipeAnggotaKoperasi() == null ? null : member.getTipeAnggotaKoperasi().getId();
        refreshSaldo();
        loadRules();

        DashboardUiKit.attachIntro(comp, "Toko Online — " + safe(member.getNama()),
                "Belanja produk kantin dari sini. Pilih tenant, masukkan barang ke keranjang (boleh beda tenant), "
                        + "lalu bayar. Diskon promo dihitung otomatis dan pembayaran dipotong dari saldo Anda.");

        buildUI();
        loadToko();
        loadPayment();
        loadProduk();
    }

    private void refreshSaldo() {
        try {
            Double s = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(member);
            memberSaldo = s == null ? 0 : s.doubleValue();
        } catch (Exception e) {
            memberSaldo = 0;
        }
        if (lblSaldo != null) {
            lblSaldo.setValue("Saldo: Rp " + DashboardUiKit.money(memberSaldo));
        }
    }

    /**
     * Muat aturan diskon aktif.
     *
     * <p>Perhitungannya sengaja tidak lagi ditulis di sini: layar belanja
     * native memakai aturan yang sama, dan dua salinan hitungan uang pasti
     * menyimpang. Lihat {@link KantinDiskonEngine}.</p>
     */
    private void loadRules() {
        rules = KantinDiskonEngine.muatAturan(HibernateUtil.currentSession());
    }

    // ======================== Build UI ========================

    private void buildUI() {
        host.getChildren().clear();

        // Header: identitas + saldo
        Div head = new Div();
        head.setSclass("ais-crud-filter-card");
        head.setParent(host);
        Div hb = new Div();
        hb.setSclass("ais-crud-filter-body");
        hb.setStyle("display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;");
        hb.setParent(head);

        Div ident = new Div();
        ident.setStyle("flex:1 1 220px;");
        ident.setParent(hb);
        Label nm = new Label(safe(member.getNama()));
        nm.setStyle("font-weight:800;font-size:14px;color:#0f172a;display:block;");
        nm.setParent(ident);
        lblSaldo = new Label("Saldo: Rp " + DashboardUiKit.money(memberSaldo));
        lblSaldo.setStyle("font-weight:800;color:#16a34a;display:block;");
        lblSaldo.setParent(ident);

        Div cToko = new Div();
        cToko.setStyle("flex:1 1 200px;");
        cToko.setParent(hb);
        cToko.appendChild(mini("Pilih Tenant / Toko"));
        cboToko = new Combobox();
        cboToko.setWidth("100%");
        cboToko.setReadonly(true);
        cboToko.addEventListener("onSelect", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Comboitem it = cboToko.getSelectedItem();
                tokoIdAktif = (it == null || it.getValue() == null) ? null : (Long) it.getValue();
                tokoNamaAktif = it == null ? "" : it.getLabel();
                loadProduk();
            }
        });
        cToko.appendChild(cboToko);

        Div cBayar = new Div();
        cBayar.setStyle("flex:1 1 180px;");
        cBayar.setParent(hb);
        cBayar.appendChild(mini("Cara Pembayaran"));
        cboBayar = new Combobox();
        cboBayar.setWidth("100%");
        cboBayar.setReadonly(true);
        cBayar.appendChild(cboBayar);

        Div cCat = new Div();
        cCat.setStyle("flex:1 1 180px;");
        cCat.setParent(hb);
        cCat.appendChild(mini("Catatan (opsional)"));
        txtCatatan = new Textbox();
        txtCatatan.setWidth("100%");
        txtCatatan.setTooltiptext("mis. tidak pakai sambal, antar ke meja");
        cCat.appendChild(txtCatatan);

        // Dua panel: katalog | keranjang
        Div dataCard = new Div();
        dataCard.setSclass("ais-crud-data-card");
        dataCard.setParent(host);
        Div two = new Div();
        two.setStyle("display:flex;flex-wrap:wrap;gap:14px;align-items:flex-start;");
        two.setParent(dataCard);

        Div pProduk = new Div();
        pProduk.setStyle("flex:2 1 360px;min-width:300px;");
        pProduk.setParent(two);
        pProduk.appendChild(section("Katalog Produk"));
        Hlayout cari = new Hlayout();
        cari.setStyle("width:100%;gap:6px;margin-bottom:8px;");
        cari.setParent(pProduk);
        txtCariProduk = new Textbox();
        txtCariProduk.setWidth("100%");
        txtCariProduk.setTooltiptext("Ketik nama produk lalu Enter");
        txtCariProduk.addEventListener("onOK", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                loadProduk();
            }
        });
        cari.appendChild(txtCariProduk);
        Button bCari = new Button("Cari");
        bCari.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                loadProduk();
            }
        });
        cari.appendChild(bCari);
        grdProduk = new Grid();
        grdProduk.setSclass("dgrid");
        grdProduk.setWidth("100%");
        grdProduk.setStyle("border:0;background:transparent;");
        grdProduk.setParent(pProduk);
        Columns gpc = new Columns();
        gpc.setParent(grdProduk);
        addCol(gpc, "Produk", null);
        addCol(gpc, "Harga", "120px");
        addCol(gpc, "", "80px");

        Div pCart = new Div();
        pCart.setStyle("flex:1 1 320px;min-width:280px;");
        pCart.setParent(two);
        pCart.appendChild(section("Keranjang Belanja"));
        grdKeranjang = new Grid();
        grdKeranjang.setSclass("dgrid");
        grdKeranjang.setWidth("100%");
        grdKeranjang.setStyle("border:0;background:transparent;");
        grdKeranjang.setParent(pCart);
        Columns gkc = new Columns();
        gkc.setParent(grdKeranjang);
        addCol(gkc, "Produk", null);
        addCol(gkc, "Jumlah", "120px");
        addCol(gkc, "Subtotal", "110px");
        addCol(gkc, "", "40px");

        Div totBox = new Div();
        totBox.setStyle("margin-top:10px;padding:12px;border:1px solid #e2e8f0;border-radius:12px;background:#fff;");
        totBox.setParent(pCart);
        Div rowT = new Div();
        rowT.setStyle("display:flex;justify-content:space-between;align-items:center;");
        rowT.setParent(totBox);
        Label lt = new Label(ais.common.Common.getBahasaConfig("Total Bayar"));
        lt.setStyle("font-weight:800;font-size:14px;color:#475569;");
        lt.setParent(rowT);
        lblTotal = new Label(ais.common.Common.getBahasaConfig("Rp 0"));
        lblTotal.setStyle("font-weight:900;font-size:18px;color:#16a34a;");
        lblTotal.setParent(rowT);

        MyToolbarbuttonConfig bayar = new MyToolbarbuttonConfig("Konfirmasi & Bayar", "/img/save.gif");
        bayar.setStyle("width:100%;margin-top:10px;font-weight:800;");
        bayar.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onBayar();
            }
        });
        bayar.setParent(pCart);

        recompute();
    }

    // ======================== Data ========================

    private void loadToko() {
        if (cboToko == null) {
            return;
        }
        for (Object[] r : rows("SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC")) {
            Comboitem it = new Comboitem(str(r[1]));
            it.setValue(((Number) r[0]).longValue());
            it.setParent(cboToko);
        }
    }

    private void loadPayment() {
        if (cboBayar == null) {
            return;
        }
        cboBayar.getItems().clear();
        StringBuilder sql = new StringBuilder("SELECT cpk.id, cpk.nama, cpk.manual FROM koperasi.cara_pembayaran_koperasi cpk "
                + "WHERE cpk.aktif = true AND cpk.online = true");
        if (memberJenisId != null) {
            sql.append(" AND (SELECT jak.daftar_cara_pembayaran_yang_boleh_di_pilih FROM koperasi.jenis_anggota_koperasi jak "
                    + "WHERE jak.id = ").append(memberJenisId).append(") LIKE '%,' || cpk.id || ',%'");
        }
        sql.append(" ORDER BY cpk.nama ASC");
        for (Object[] r : rows(sql.toString())) {
            Comboitem it = new Comboitem(str(r[1]));
            it.setValue(((Number) r[0]).longValue());
            it.setAttribute("manual", Boolean.valueOf(bool(r[2])));
            it.setParent(cboBayar);
        }
    }

    private void loadProduk() {
        if (grdProduk == null) {
            return;
        }
        Rows rows = new Rows();
        if (tokoIdAktif == null) {
            tambahPesan(rows, "Silakan pilih tenant/toko terlebih dahulu. Gunakan kotak pilihan Toko di bagian atas untuk memilih toko yang ingin dikunjungi.");
            ganti(grdProduk, rows);
            return;
        }
        String kw = txtCariProduk == null ? "" : txtCariProduk.getValue().trim().replace("'", "''");
        StringBuilder sql = new StringBuilder("SELECT id, kode, nama, COALESCE(hargajual,0) FROM koperasi.produk "
                + "WHERE aktif = true AND toko = " + tokoIdAktif);
        if (!kw.isEmpty()) {
            sql.append(" AND (nama ILIKE '%").append(kw).append("%' OR kode = '").append(kw).append("')");
        }
        sql.append(" ORDER BY nama ASC LIMIT 80");

        List<Object[]> data = rows(sql.toString());
        if (data.isEmpty()) {
            tambahPesan(rows, "Tidak ada produk yang cocok.");
        } else {
            for (Object[] r : data) {
                final Long pid = ((Number) r[0]).longValue();
                final String kode = str(r[1]);
                final String nama = str(r[2]);
                final double harga = num(r[3]);
                Row row = new Row();
                row.setParent(rows);
                Label ln = new Label(nama);
                ln.setStyle("font-weight:600;");
                ln.setParent(row);
                Label lh = new Label("Rp " + DashboardUiKit.money(harga));
                lh.setStyle("color:#16a34a;font-weight:700;");
                lh.setParent(row);
                Button add = new Button("+");
                add.setTooltiptext("Tambahkan ke keranjang");
                add.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        tambahKeKeranjang(pid, kode, nama, harga);
                    }
                });
                add.setParent(row);
            }
        }
        ganti(grdProduk, rows);
    }

    // ======================== Keranjang ========================

    private void tambahKeKeranjang(Long id, String kode, String nama, double harga) {
        for (Item it : cart) {
            if (it.id.equals(id) && it.idToko != null && it.idToko.equals(tokoIdAktif)) {
                it.jumlah += 1;
                recompute();
                return;
            }
        }
        cart.add(new Item(id, kode, nama, harga, tokoIdAktif, tokoNamaAktif));
        recompute();
    }

    private void recompute() {
        double total = 0;
        for (Item it : cart) {
            evaluasiDiskon(it);
            total += it.harga * it.jumlah - it.diskon;
        }
        renderKeranjang();
        if (lblTotal != null) {
            lblTotal.setValue("Rp " + DashboardUiKit.money(total));
        }
    }

    private void renderKeranjang() {
        Rows rows = new Rows();
        if (cart.isEmpty()) {
            Row r = new Row();
            r.setParent(rows);
            Label l = new Label(ais.common.Common.getBahasaConfig("Keranjang masih kosong."));
            l.setStyle("color:#64748b;");
            l.setParent(r);
        } else {
            String tokoTerakhir = null;
            for (final Item it : cart) {
                if (it.namaToko != null && !it.namaToko.equals(tokoTerakhir)) {
                    tokoTerakhir = it.namaToko;
                    Row hr = new Row();
                    hr.setParent(rows);
                    Label hl = new Label(it.namaToko);
                    hl.setStyle("font-weight:800;color:#1d4ed8;font-size:11px;background:#eef4ff;display:block;padding:3px 6px;");
                    hl.setParent(hr);
                }
                Row row = new Row();
                row.setValign("middle");
                row.setParent(rows);
                Vlayout nb = new Vlayout();
                nb.setSpacing("0");
                Label ln = new Label(it.nama);
                ln.setStyle("font-weight:600;");
                ln.setParent(nb);
                Label lp = new Label("@ Rp " + DashboardUiKit.money(it.harga)
                        + (it.diskon > 0 ? " · diskon Rp " + DashboardUiKit.money(it.diskon) : ""));
                lp.setStyle("font-size:10px;color:#64748b;");
                lp.setParent(nb);
                nb.setParent(row);

                Hlayout qty = new Hlayout();
                qty.setStyle("gap:4px;align-items:center;");
                Button minus = new Button("-");
                minus.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        it.jumlah -= 1;
                        if (it.jumlah <= 0) {
                            cart.remove(it);
                        }
                        recompute();
                    }
                });
                qty.appendChild(minus);
                Label qn = new Label(String.valueOf(it.jumlah));
                qn.setStyle("min-width:22px;text-align:center;font-weight:700;");
                qty.appendChild(qn);
                Button plus = new Button("+");
                plus.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        it.jumlah += 1;
                        recompute();
                    }
                });
                qty.appendChild(plus);
                qty.setParent(row);

                Label sub = new Label("Rp " + DashboardUiKit.money(it.harga * it.jumlah - it.diskon));
                sub.setStyle("font-weight:700;color:#16a34a;");
                sub.setParent(row);

                Button del = new Button("x");
                del.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        cart.remove(it);
                        recompute();
                    }
                });
                del.setParent(row);
            }
        }
        ganti(grdKeranjang, rows);
    }

    /**
     * Nilai diskon &amp; cashback satu baris keranjang lewat mesin bersama.
     *
     * <p>Hasilnya disalin balik ke {@link Item} agar tampilan keranjang tetap
     * seperti sebelumnya, sementara aturannya dievaluasi di satu tempat yang
     * juga dipakai kontrak native.</p>
     */
    private void evaluasiDiskon(Item it) {
        KantinDiskonEngine.Baris b = new KantinDiskonEngine.Baris(it.id, it.idToko, it.harga, it.jumlah);
        KantinDiskonEngine.evaluasi(b, rules, memberJenisId, memberTipeId, new Date());
        it.diskon = b.diskon;
        it.cashback = b.cashback;
        it.aturanDiskonId = b.aturanDiskonId;
    }

    // ======================== Bayar ========================

    private void onBayar() throws Exception {
        if (cart.isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, keranjang belanja masih kosong. Langkah yang dapat dilakukan: (1) pilih produk dari daftar dengan menekan tombol \"+\"; (2) pastikan toko/tenant sudah dipilih; (3) ulangi pembayaran setelah menambah produk.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        Comboitem pi = cboBayar.getSelectedItem();
        if (pi == null || pi.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, cara pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih cara pembayaran dari daftar yang tersedia di bawah keranjang; (2) pastikan cara pembayaran aktif; (3) ulangi proses pembayaran.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        Long caraBayarId = (Long) pi.getValue();
        boolean manual = Boolean.TRUE.equals(pi.getAttribute("manual"));

        double grandTotal = 0;
        for (Item it : cart) {
            grandTotal += it.harga * it.jumlah - it.diskon;
        }
        refreshSaldo();
        if (!manual && memberSaldo < grandTotal) {
            MyMessageboxConfig.show("Saldo Anda tidak mencukupi. Saldo Rp " + DashboardUiKit.money(memberSaldo)
                    + ", total belanja Rp " + DashboardUiKit.money(grandTotal) + ".", "Saldo Kurang",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        // Kelompokkan per toko (multi-tenant) lalu proses satu per satu.
        LinkedHashMap<Long, List<Item>> byToko = new LinkedHashMap<Long, List<Item>>();
        for (Item it : cart) {
            Long tk = it.idToko;
            if (!byToko.containsKey(tk)) {
                byToko.put(tk, new ArrayList<Item>());
            }
            byToko.get(tk).add(it);
        }

        String catatan = txtCatatan == null ? null : txtCatatan.getValue();
        boolean allOk = true;
        String err = "";
        for (Long tk : byToko.keySet()) {
            JSONArray arr = new JSONArray();
            for (Item it : byToko.get(tk)) {
                JSONObject t = new JSONObject();
                t.put("id", it.id);
                t.put("kode", it.kode == null ? "" : it.kode);
                t.put("nama", it.nama == null ? "" : it.nama);
                t.put("harga", it.harga);
                t.put("jumlah", it.jumlah);
                t.put("diskon", it.diskon);
                t.put("cashback", it.cashback);
                if (it.aturanDiskonId != null) {
                    t.put("aturanDiskon", it.aturanDiskonId);
                }
                arr.put(t);
            }
            JSONObject payload = new JSONObject();
            payload.put("kodeUnik", "ONL-" + System.currentTimeMillis() + Common.getGeneratedBarCode(4));
            payload.put("idToko", tk);
            payload.put("waktu", Common.dateFormat3.get().format(new Date()));
            payload.put("caraBayar", caraBayarId);
            payload.put("id_member", member.getId());
            if (catatan != null && !catatan.trim().isEmpty()) {
                payload.put("keterangan", catatan.trim());
            }
            payload.put("transaksi", arr);

            JSONObject hasil = new JSONObject();
            if (manual) {
                KantinHelper.draft_bayar(Common.getCurrentUser(), payload, hasil);
            } else {
                KantinHelper.bayar(Common.getCurrentUser(), payload, hasil);
            }
            String st = hasil.optString("status", "");
            if (!("00".equals(st) || "success".equalsIgnoreCase(st))) {
                allOk = false;
                err = hasil.optString("description", "Gagal memproses transaksi untuk salah satu tenant.");
                break;
            }
        }

        if (allOk) {
            String pesan = manual ? "Pesanan dibuat. Silakan selesaikan pembayaran di kasir."
                    : "Pembayaran berhasil. Terima kasih telah berbelanja!";
            cart.clear();
            if (txtCatatan != null) {
                txtCatatan.setValue("");
            }
            refreshSaldo();
            recompute();
            MyMessageboxConfig.show(pesan, "Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } else {
            MyMessageboxConfig.show(err, "Gagal", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
        }
    }

    // ======================== Util ========================

    private static Label mini(String text) {
        Label l = new Label(text);
        l.setStyle("font-size:11px;font-weight:700;color:#64748b;display:block;margin-bottom:3px;");
        return l;
    }

    private static Component section(String judul) {
        return DashboardUiKit.html("<div style='font-size:14px;font-weight:800;color:#0f172a;margin:4px 0 8px;"
                + "border-left:4px solid " + DashboardUiKit.PRIMARY + ";padding-left:10px;'>"
                + DashboardUiKit.esc(judul) + "</div>");
    }

    private void addCol(Columns columns, String label, String width) {
        Column c = new Column();
        c.setLabel(label);
        if (width != null) {
            c.setWidth(width);
        }
        c.setParent(columns);
    }

    private void tambahPesan(Rows rows, String pesan) {
        Row r = new Row();
        r.setParent(rows);
        Label l = new Label(pesan);
        l.setStyle("color:#64748b;padding:10px 4px;");
        l.setParent(r);
    }

    private void ganti(Grid grid, Rows rows) {
        if (grid == null) {
            return;
        }
        if (grid.getRows() != null) {
            grid.getRows().detach();
        }
        rows.setParent(grid);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql) {
        try {
            SQLQuery q = HibernateUtil.currentSession().createSQLQuery(sql);
            return q.list();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Object[]>();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Long lng(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static boolean bool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        String s = o.toString().trim();
        return s.equals("t") || s.equalsIgnoreCase("true") || s.equals("1");
    }

    private static Date date(Object o) {
        return (o instanceof Date) ? (Date) o : null;
    }
}
