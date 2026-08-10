package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vlayout;

import ais.action.servlet.api.KantinHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Toko;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Monitor &amp; Proses Pesanan Online (Draft) — konversi dari {@code modul/kantin/pesanan}.
 *
 * <p>Menampilkan pesanan yang dibuat member tapi belum dibayar (draft, {@code lunas IS NULL}).
 * Petugas dapat menekan <b>Proses</b> untuk menyelesaikannya menjadi transaksi: memilih cara
 * pembayaran lalu menyimpan lewat {@link KantinHelper#bayar} (memakai ulang logika kasir; field
 * {@code draftPembelianAnggotaKoperasi} membuat draft otomatis ditandai lunas).</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang yang tidak boleh melihat toko lain hanya melihat pesanan tokonya.</p>
 */
public class PesananKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host;
    private Long scopeTokoId = null;
    private String scopeTokoNama = null;

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
        resolveScopeToko();

        String judul = scopeTokoId == null ? "Pesanan Online (Draft)" : "Pesanan Online — " + scopeTokoNama;
        String desc = "Pesanan yang sudah dibuat member tetapi belum dibayar. Tekan Proses untuk menyelesaikannya "
                + "menjadi transaksi setelah member membayar.";
        if (scopeTokoId != null) {
            desc = "Khusus pesanan toko Anda (" + scopeTokoNama + "). " + desc;
        }
        DashboardUiKit.attachIntro(comp, judul, desc);

        buildList();
    }

    public void onRefresh(Event event) throws Exception {
        buildList();
    }

    private void resolveScopeToko() {
        try {
            Toko ct = Common.getCurrentToko();
            if (ct != null && ct.getId() != null) {
                Boolean b = ct.getBolehMelihatTokolain();
                if (b == null || !b.booleanValue()) {
                    scopeTokoId = ct.getId();
                    scopeTokoNama = ct.getNama();
                }
            }
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PesananKantinAction.java:92");
        }
    }

    private String andToko(String alias) {
        return scopeTokoId == null ? "" : (" AND " + alias + ".toko = " + scopeTokoId);
    }

    // ======================== Daftar pesanan ========================

    private void buildList() {
        if (host == null) {
            return;
        }
        host.getChildren().clear();

        List<Object[]> drafts = rows("SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'dd-MM-yyyy HH24:MI'), "
                + "COALESCE(ak.nama,'-'), COALESCE(t.nama,'-'), COALESCE(a.total_biaya,0), COALESCE(a.keterangan,''), "
                + "a.anggota_koperasi, a.toko FROM koperasi.draft_pembelian_anggota_koperasi a "
                + "LEFT JOIN koperasi.anggota_koperasi ak ON ak.id = a.anggota_koperasi "
                + "LEFT JOIN koperasi.toko t ON t.id = a.toko WHERE a.lunas IS NULL" + andToko("a")
                + " ORDER BY a.tanggal_pembayaran DESC LIMIT 200");

        double totalNilai = 0;
        for (Object[] d : drafts) {
            totalNilai += num(d[4]);
        }
        List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
        kartu.add(new DashboardUiKit.Stat("Pesanan Menunggu", DashboardUiKit.money(drafts.size()),
                "belum dibayar", DashboardUiKit.WARN));
        kartu.add(new DashboardUiKit.Stat("Total Nilai", "Rp " + DashboardUiKit.money(totalNilai),
                "estimasi pemasukan", DashboardUiKit.PRIMARY));
        host.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Daftar pesanan online member yang menunggu diproses. Klik Proses untuk menyelesaikan pembayaran.")));
        host.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));

        if (drafts.isEmpty()) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='font-size:12px;color:#64748b;padding:14px;'>Tidak ada pesanan yang menunggu.</div>"));
            return;
        }

        Grid grid = new Grid();
        grid.setSclass("dgrid");
        grid.setWidth("100%");
        grid.setStyle("border:0;background:transparent;");
        if (drafts.size() > 25) {
            grid.setMold("paging");
            grid.setPageSize(25);
        }
        Columns columns = new Columns();
        columns.setParent(grid);
        columns.setSizable(true);
        addCol(columns, "No.", "48px");
        addCol(columns, "Waktu", "130px");
        addCol(columns, "Member", null);
        addCol(columns, "Toko", "140px");
        addCol(columns, "Total", "120px");
        addCol(columns, "Catatan", "160px");
        addCol(columns, "", "170px");

        Rows rows = new Rows();
        rows.setParent(grid);
        int no = 1;
        for (Object[] d : drafts) {
            final Long draftId = ((Number) d[0]).longValue();
            final Long idMember = d[6] == null ? null : ((Number) d[6]).longValue();
            final Long idToko = d[7] == null ? null : ((Number) d[7]).longValue();
            final String keterangan = str(d[5]);
            final String memberNama = str(d[2]);

            Row row = new Row();
            row.setValign("middle");
            row.setParent(rows);
            new Label(String.valueOf(no++)).setParent(row);
            new Label(str(d[1])).setParent(row);
            Label lm = new Label(memberNama);
            lm.setStyle("font-weight:700;");
            lm.setParent(row);
            new Label(str(d[3])).setParent(row);
            Label lt = new Label("Rp " + DashboardUiKit.money(num(d[4])));
            lt.setStyle("font-weight:800;color:#16a34a;");
            lt.setParent(row);
            new Label(keterangan).setParent(row);

            Hlayout aksi = new Hlayout();
            aksi.setStyle("gap:4px;");

            Button detail = new Button("Detail");
            detail.setTooltiptext("Lihat seluruh data pesanan ini");
            detail.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    bukaDetail(draftId);
                }
            });
            aksi.appendChild(detail);

            Button proses = new Button("Proses");
            proses.setTooltiptext("Selesaikan pesanan menjadi transaksi");
            proses.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    bukaProses(draftId, idToko, idMember, keterangan, memberNama);
                }
            });
            aksi.appendChild(proses);

            aksi.setParent(row);
        }
        grid.setParent(host);
    }

    // ======================== Dialog proses ========================

    private void bukaProses(final Long draftId, final Long idToko, final Long idMember, final String keterangan,
            String memberNama) throws Exception {
        final MyWindow w = new MyWindow("Proses Pesanan", "normal", true);
        w.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        w.setWidth("460px");

        Vlayout box = new Vlayout();
        box.setStyle("padding:10px;");
        box.setParent(w);

        box.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Pesanan dari " + DashboardUiKit.esc(memberNama) + " akan diselesaikan menjadi transaksi setelah "
                        + "member membayar. Pilih cara pembayaran lalu tekan Bayar.")));

        // Rincian item pesanan
        List<Object[]> items = rows("SELECT COALESCE(p.nama,d.nama), COALESCE(d.qty,0), COALESCE(d.hargasatuan,0), "
                + "COALESCE(d.diskon,0) FROM koperasi.draft_pembelian d LEFT JOIN koperasi.produk p ON p.id = d.produk "
                + "WHERE d.draft_pembelian_anggota_koperasi = " + draftId);
        Grid gi = new Grid();
        gi.setSclass("dgrid");
        gi.setWidth("100%");
        gi.setStyle("border:0;background:transparent;");
        Columns gc = new Columns();
        gc.setParent(gi);
        addCol(gc, "Produk", null);
        addCol(gc, "Qty", "60px");
        addCol(gc, "Subtotal", "120px");
        Rows gr = new Rows();
        gr.setParent(gi);
        for (Object[] it : items) {
            double sub = num(it[1]) * num(it[2]) - num(it[3]);
            Row r = new Row();
            r.setParent(gr);
            new Label(str(it[0])).setParent(r);
            new Label(DashboardUiKit.money(num(it[1]))).setParent(r);
            new Label("Rp " + DashboardUiKit.money(sub)).setParent(r);
        }
        gi.setParent(box);

        // Cara pembayaran
        Hlayout hb = new Hlayout();
        hb.setStyle("gap:8px;align-items:center;margin-top:8px;");
        hb.setParent(box);
        hb.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara Bayar:")));
        final Combobox cboBayar = new Combobox();
        cboBayar.setReadonly(true);
        cboBayar.setWidth("220px");
        Common.insertCombo(cboBayar, "nama", ais.database.model.koperasi.CaraPembayaranKoperasi.class,
                Restrictions.eq("aktif", true));
        hb.appendChild(cboBayar);

        Hlayout btns = new Hlayout();
        btns.setStyle("gap:8px;justify-content:flex-end;margin-top:12px;");
        btns.setParent(box);
        MyToolbarbuttonConfig bayar = new MyToolbarbuttonConfig("Bayar & Selesaikan", "/img/save.gif");
        bayar.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                doProses(draftId, idToko, idMember, keterangan, cboBayar, w);
            }
        });
        btns.appendChild(bayar);
        Button batal = new Button("Batal");
        batal.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                w.detach();
            }
        });
        btns.appendChild(batal);

        w.doOverlapped();
    }

    private void doProses(Long draftId, Long idToko, Long idMember, String keterangan, Combobox cboBayar, MyWindow w)
            throws Exception {
        if (cboBayar.getSelectedItem() == null || cboBayar.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, cara pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih cara pembayaran dari daftar yang tersedia; (2) pastikan cara pembayaran aktif untuk toko ini; (3) ulangi proses.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        if (idToko == null) {
            MyMessageboxConfig.show("Mohon maaf, pesanan ini tidak memiliki toko yang valid. Langkah yang dapat dilakukan: (1) pastikan pesanan dibuat dengan memilih toko terlebih dahulu; (2) buat pesanan baru melalui menu Beranda Kantin; (3) hubungi Administrator jika masalah berlanjut.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        Long caraBayarId = ((ais.database.model.koperasi.CaraPembayaranKoperasi) cboBayar.getSelectedItem().getValue())
                .getId();

        // Bangun daftar transaksi dari item draft (diskon/cashback sudah tersimpan saat pesanan dibuat).
        JSONArray arr = new JSONArray();
        for (Object[] it : rows("SELECT p.id, COALESCE(p.kode,''), d.nama, COALESCE(d.hargasatuan,0), "
                + "COALESCE(d.qty,0), COALESCE(d.diskon,0), d.aturan_diskon, COALESCE(d.cashback,0) "
                + "FROM koperasi.draft_pembelian d LEFT JOIN koperasi.produk p ON p.id = d.produk "
                + "WHERE d.draft_pembelian_anggota_koperasi = " + draftId)) {
            JSONObject t = new JSONObject();
            t.put("id", it[0] == null ? null : ((Number) it[0]).longValue());
            t.put("kode", str(it[1]));
            t.put("nama", str(it[2]));
            t.put("harga", num(it[3]));
            t.put("jumlah", num(it[4]));
            t.put("diskon", num(it[5]));
            if (it[6] != null) {
                t.put("aturanDiskon", ((Number) it[6]).longValue());
            }
            t.put("cashback", num(it[7]));
            arr.put(t);
        }
        if (arr.length() == 0) {
            MyMessageboxConfig.show("Mohon maaf, pesanan tidak memiliki rincian barang. Langkah yang dapat dilakukan: (1) kembali ke halaman Beranda Kantin; (2) tambahkan minimal satu produk ke keranjang; (3) buat kembali pesanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("kodeUnik", "PSN-" + System.currentTimeMillis() + Common.getGeneratedBarCode(4));
        payload.put("idToko", idToko);
        payload.put("waktu", Common.dateFormat3.get().format(new Date()));
        payload.put("caraBayar", caraBayarId);
        if (idMember != null) {
            payload.put("id_member", idMember);
        }
        payload.put("draftPembelianAnggotaKoperasi", draftId);
        if (keterangan != null && !keterangan.trim().isEmpty()) {
            payload.put("keterangan", keterangan.trim());
        }
        payload.put("transaksi", arr);

        JSONObject hasil = new JSONObject();
        KantinHelper.bayar(Common.getCurrentUser(), payload, hasil);

        String status = hasil.optString("status", "");
        if ("00".equals(status) || "success".equalsIgnoreCase(status)) {
            w.detach();
            buildList();
            MyMessageboxConfig.show("Pesanan berhasil diproses menjadi transaksi.", "Berhasil",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } else {
            MyMessageboxConfig.show(hasil.optString("description", "Gagal memproses pesanan."), "Gagal",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
        }
    }

    // ======================== Dialog detail lengkap (read-only) ========================

    /**
     * Menampilkan SELURUH data satu pesanan dalam jendela read-only: informasi kepala
     * (kode, pemesan, toko, waktu, metode, status), catatan penuh, daftar semua item
     * beserta diskon/cashback/subtotal, dan ringkasan total tagihan. Tidak mengubah data.
     */
    private void bukaDetail(final Long draftId) throws Exception {
        final MyWindow w = new MyWindow("Detail Lengkap Pesanan", "normal", true);
        w.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        w.setWidth("640px");

        Vlayout box = new Vlayout();
        box.setStyle("padding:12px;");
        box.setParent(w);

        List<Object[]> hd = rows("SELECT COALESCE(a.kode,'-'), TO_CHAR(a.tanggal_pembayaran,'dd-MM-yyyy HH24:MI'), "
                + "COALESCE(ak.nama,'-'), COALESCE(ak.kode_identitas,''), COALESCE(t.nama,'-'), "
                + "COALESCE(a.keterangan,''), COALESCE(a.total_biaya,0), COALESCE(a.total_diskon,0), "
                + "COALESCE(a.totalcashback,0), COALESCE(cpk.nama,'-'), a.lunas "
                + "FROM koperasi.draft_pembelian_anggota_koperasi a "
                + "LEFT JOIN koperasi.anggota_koperasi ak ON ak.id = a.anggota_koperasi "
                + "LEFT JOIN koperasi.toko t ON t.id = a.toko "
                + "LEFT JOIN koperasi.cara_pembayaran_koperasi cpk ON cpk.id = a.cara_pembayaran_koperasi "
                + "WHERE a.id = " + draftId);
        if (hd.isEmpty()) {
            box.appendChild(DashboardUiKit.html(
                    "<div style='padding:14px;color:#dc2626;'>Data pesanan tidak ditemukan.</div>"));
            w.doOverlapped();
            return;
        }
        Object[] h = hd.get(0);
        boolean lunas = h[10] != null && !str(h[10]).isEmpty();
        String statusBadge = lunas
                ? "<span style='background:#16a34a;color:#fff;padding:3px 10px;border-radius:999px;font-size:11px;font-weight:700;'>Lunas &amp; Selesai</span>"
                : "<span style='background:#f59e0b;color:#1f2937;padding:3px 10px;border-radius:999px;font-size:11px;font-weight:700;'>Belum Dibayar</span>";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:8px;margin-bottom:10px;'>");
        sb.append("<div><div style='font-size:11px;color:#64748b;text-transform:uppercase;font-weight:700;'>Kode Pesanan</div>");
        sb.append("<div style='font-size:18px;font-weight:800;color:#2563eb;'>").append(DashboardUiKit.esc(str(h[0]))).append("</div></div>");
        sb.append("<div>").append(statusBadge).append("</div></div>");
        sb.append("<div style='display:grid;grid-template-columns:1fr 1fr;gap:8px 16px;font-size:12px;'>");
        String kodeMember = str(h[3]);
        sb.append(infoCell("Pemesan", (kodeMember.isEmpty() ? "" : ("[" + DashboardUiKit.esc(kodeMember) + "] ")) + DashboardUiKit.esc(str(h[2]))));
        sb.append(infoCell("Toko / Pedagang", DashboardUiKit.esc(str(h[4]))));
        sb.append(infoCell("Waktu Pesan", DashboardUiKit.esc(str(h[1]))));
        sb.append(infoCell("Metode Bayar", DashboardUiKit.esc(str(h[9]))));
        sb.append("</div>");
        String ket = str(h[5]);
        if (!ket.trim().isEmpty()) {
            sb.append("<div style='margin-top:10px;padding:10px;background:#f1f5f9;border:1px solid #e2e8f0;border-radius:8px;'>");
            sb.append("<div style='font-size:11px;color:#64748b;font-weight:700;margin-bottom:3px;'>Catatan / Keterangan Pesanan</div>");
            sb.append("<div style='white-space:pre-wrap;color:#0f172a;'>").append(DashboardUiKit.esc(ket)).append("</div></div>");
        }
        box.appendChild(DashboardUiKit.html(sb.toString()));

        List<Object[]> items = rows("SELECT COALESCE(p.nama,d.nama), COALESCE(p.kode,''), COALESCE(d.hargasatuan,0), "
                + "COALESCE(d.qty,0), COALESCE(d.diskon,0), COALESCE(d.cashback,0) "
                + "FROM koperasi.draft_pembelian d LEFT JOIN koperasi.produk p ON p.id = d.produk "
                + "WHERE d.draft_pembelian_anggota_koperasi = " + draftId + " ORDER BY d.id");

        box.appendChild(DashboardUiKit.html("<div style='font-size:13px;font-weight:700;color:#0f172a;margin:12px 0 4px;'>"
                + "Rincian Item Pesanan (" + items.size() + ")</div>"));

        Grid gi = new Grid();
        gi.setSclass("dgrid");
        gi.setWidth("100%");
        gi.setStyle("border:0;background:transparent;");
        Columns gc = new Columns();
        gc.setParent(gi);
        addCol(gc, "Produk", null);
        addCol(gc, "Qty", "50px");
        addCol(gc, "Harga", "100px");
        addCol(gc, "Diskon", "95px");
        addCol(gc, "Cashback", "95px");
        addCol(gc, "Subtotal", "110px");
        Rows gr = new Rows();
        gr.setParent(gi);
        double subtotal = 0;
        for (Object[] it : items) {
            double harga = num(it[2]);
            double qty = num(it[3]);
            double disk = num(it[4]);
            double cb = num(it[5]);
            double sub = harga * qty - disk;
            subtotal += harga * qty;

            Row r = new Row();
            r.setValign("middle");
            r.setParent(gr);
            String kode = str(it[1]);
            new Label(str(it[0]) + (kode.isEmpty() ? "" : (" (" + kode + ")"))).setParent(r);
            new Label(DashboardUiKit.money(qty)).setParent(r);
            new Label("Rp " + DashboardUiKit.money(harga)).setParent(r);
            Label ld = new Label(disk > 0 ? "-Rp " + DashboardUiKit.money(disk) : "-");
            if (disk > 0) {
                ld.setStyle("color:#dc2626;");
            }
            ld.setParent(r);
            Label lc = new Label(cb > 0 ? "+Rp " + DashboardUiKit.money(cb) : "-");
            if (cb > 0) {
                lc.setStyle("color:#0891b2;");
            }
            lc.setParent(r);
            Label ls = new Label("Rp " + DashboardUiKit.money(sub));
            ls.setStyle("font-weight:700;");
            ls.setParent(r);
        }
        gi.setParent(box);

        StringBuilder tb = new StringBuilder();
        tb.append("<div style='display:flex;justify-content:flex-end;margin-top:10px;'><div style='min-width:280px;font-size:12px;'>");
        tb.append(totRow("Subtotal", "Rp " + DashboardUiKit.money(subtotal), "#0f172a"));
        tb.append(totRow("Total Diskon", "-Rp " + DashboardUiKit.money(num(h[7])), "#dc2626"));
        tb.append(totRow("Total Cashback", "+Rp " + DashboardUiKit.money(num(h[8])), "#0891b2"));
        tb.append("<div style='display:flex;justify-content:space-between;padding:8px 0 0;margin-top:4px;border-top:1px solid #e2e8f0;'>");
        tb.append("<span style='font-weight:800;color:#0f172a;'>TOTAL TAGIHAN</span>");
        tb.append("<span style='font-weight:800;color:#16a34a;font-size:16px;'>Rp ").append(DashboardUiKit.money(num(h[6]))).append("</span></div></div></div>");
        box.appendChild(DashboardUiKit.html(tb.toString()));

        Hlayout btns = new Hlayout();
        btns.setStyle("gap:8px;justify-content:flex-end;margin-top:12px;");
        btns.setParent(box);
        Button tutup = new Button("Tutup");
        tutup.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                w.detach();
            }
        });
        btns.appendChild(tutup);

        w.doOverlapped();
    }

    private static String infoCell(String label, String val) {
        return "<div><div style='color:#64748b;'>" + label + "</div><div style='font-weight:700;color:#0f172a;'>"
                + val + "</div></div>";
    }

    private static String totRow(String label, String val, String color) {
        return "<div style='display:flex;justify-content:space-between;padding:3px 0;'><span style='color:#64748b;'>"
                + label + "</span><span style='font-weight:700;color:" + color + ";'>" + val + "</span></div>";
    }

    // ======================== Util ========================

    private void addCol(Columns columns, String label, String width) {
        Column c = new Column();
        c.setLabel(label);
        if (width != null) {
            c.setWidth(width);
        }
        c.setParent(columns);
    }

    // KE-FIX (Bad value for type double : "Toko Al Bahjah"/"-"): autodiscovery tipe kolom
    // Hibernate untuk native SQLQuery.list() sempat memetakan kolom teks (mis.
    // COALESCE(ak.nama,'-')/COALESCE(t.nama,'-')) sebagai double, melempar
    // org.postgresql.util.PSQLException: Bad value for type double. Baca lewat JDBC
    // ResultSet.getObject() (pola yang sudah dipakai DashboardKantinAction.rows() untuk bug
    // yang sama) agar tipe kolom diambil apa adanya, bukan ditebak Hibernate. Session dari
    // currentSession() TIDAK ditutup di sini (siklus hidupnya milik ZK, ditutup di akhir request).
    private List<Object[]> rows(String sql) {
        java.sql.Statement st = null;
        java.sql.ResultSet rs = null;
        List<Object[]> out = new ArrayList<Object[]>();
        try {
            java.sql.Connection conn = HibernateUtil.currentSession().connection();
            st = conn.createStatement();
            rs = st.executeQuery(sql);
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] r = new Object[cols];
                for (int i = 1; i <= cols; i++) {
                    r[i - 1] = rs.getObject(i);
                }
                out.add(r);
            }
            return out;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Object[]>();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/koperasi/PesananKantinAction.java:rows-rs-close"); }
            try { if (st != null) st.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/koperasi/PesananKantinAction.java:rows-st-close"); }
        }
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
