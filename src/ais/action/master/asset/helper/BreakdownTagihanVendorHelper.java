package ais.action.master.asset.helper;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.BreakdownItemTagihanVendor;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper popup untuk input breakdown detail item tagihan vendor dan cetak.
 *
 * <p><b>Cara pakai:</b>
 * <pre>
 *   BreakdownTagihanVendorHelper.tampilkanPopup(saldoAwal, eventTarget);
 * </pre>
 * </p>
 *
 * <p>Data breakdown disimpan di tabel {@code asset.breakdown_item_tagihan_vendor}
 * (entity {@link BreakdownItemTagihanVendor}) yang terpisah dari tabel utama
 * sehingga <b>tidak mengubah kalkulasi, alur persetujuan, maupun posting
 * yang sudah berjalan.</b></p>
 */
public final class BreakdownTagihanVendorHelper {

    private static final NumberFormat NF = NumberFormat.getInstance(new Locale("id", "ID"));

    static { NF.setMaximumFractionDigits(2); NF.setGroupingUsed(true); }

    private BreakdownTagihanVendorHelper() {}

    // ════════════════════════════════════════════════════════════════════════
    // Sinkron baris pajak sesuai mode (Sesuai PO vs Breakdown)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Selaraskan baris PAJAK tagihan vendor dengan mode-nya:
     * <ul>
     *   <li>BREAKDOWN: baris pajak PER DETAIL (+ baris DPT-nya) di-NONAKTIFKAN, lalu dibuat/
     *       di-update SATU baris Pajak BREAKDOWN (tertaut langsung ke tagihan, nilai = Bukti
     *       Potong) beserta baris DPT "Pembayaran pajak"-nya. -> PPh = Bukti Potong, tidak dobel.</li>
     *   <li>SESUAI PO: baris Pajak BREAKDOWN (+ DPT-nya) dinonaktifkan, baris pajak per detail
     *       diaktifkan kembali (dan akan dibuat ulang oleh Pajak.buat saat detail dirender).</li>
     * </ul>
     * Idempoten. Memakai sesi khusus (openSession) agar tidak terganggu closeSession lain.
     */
    @SuppressWarnings("unchecked")
    public static void sinkronPajakBreakdown(SaldoAwalMasterAsset saldoAwalParam) {
        if (saldoAwalParam == null || saldoAwalParam.getId() == null) {
            return;
        }
        org.hibernate.Session session = HibernateUtil.openSession();
        org.hibernate.Transaction tx = null;
        try {
            SaldoAwalMasterAsset sa = (SaldoAwalMasterAsset) session.get(SaldoAwalMasterAsset.class,
                    saldoAwalParam.getId());
            if (sa == null) {
                return;
            }
            boolean breakdown = Boolean.TRUE.equals(sa.getBreakdownAktif());
            // Mode breakdown: nilai PPh = "Bukti Potong" (input manual di form breakdown), BUKAN
            // hasil hitung per-item. (Mode "Sesuai PO" memakai N.PPH per-item, ditangani di tempat
            // lain.) Sesuai aturan: breakdown=ya → PPh dari Bukti Potong.
            double bp = sa.getBreakdownBuktiPotong() == null ? 0.0 : sa.getBreakdownBuktiPotong();

            java.util.List<ais.database.model.akunting.Pajak> detailPajaks = session
                    .createCriteria(ais.database.model.akunting.Pajak.class)
                    .createAlias("saldoAwalMasterAssetDetail", "d").add(Restrictions.eq("d.saldoAwal", sa)).list();
            ais.database.model.akunting.Pajak pajakBreakdown = (ais.database.model.akunting.Pajak) session
                    .createCriteria(ais.database.model.akunting.Pajak.class).add(Restrictions.eq("saldoAwal", sa))
                    .setMaxResults(1).uniqueResult();

            tx = session.beginTransaction();

            // (1) Baris pajak PER DETAIL + DPT-nya: nonaktif saat breakdown, aktif saat PO.
            for (ais.database.model.akunting.Pajak dp : detailPajaks) {
                dp.setAktif(!breakdown);
                session.update(dp);
                if (dp.getDaftarPengajuanTransfer() != null) {
                    dp.getDaftarPengajuanTransfer().setAktif(!breakdown);
                    session.update(dp.getDaftarPengajuanTransfer());
                }
            }

            // (2) Baris pajak BREAKDOWN (tertaut langsung ke tagihan) + DPT-nya.
            // Kolom jenis_pajak_barang pada tabel pajak NOT NULL. Bila Jenis PPh breakdown belum
            // dikonfigurasi (sa.getBreakdownJenisPph() == null), pembuatan baris PPh Bukti Potong
            // akan melanggar constraint NOT NULL. Karena itu baris breakdown hanya dibuat bila
            // Jenis PPh tersedia; jika tidak, baris breakdown lama (bila ada) dinonaktifkan.
            if (breakdown && bp > 0 && sa.getBreakdownJenisPph() != null) {
                if (pajakBreakdown == null) {
                    pajakBreakdown = new ais.database.model.akunting.Pajak();
                    pajakBreakdown.setSaldoAwal(sa);
                    pajakBreakdown.setSatuanKerja(sa.getSatuanKerja());
                    pajakBreakdown.setTanggal(ais.ui.util.WaktuUtil.getDate());
                    pajakBreakdown.setNama("PPh Bukti Potong " + (sa.getKode() == null ? "" : sa.getKode()));
                }
                pajakBreakdown.setNilai(bp);
                pajakBreakdown.setAktif(true);
                // Jenis PPh (utk posting jurnal); nilai TIDAK ikut % jenis ini — tetap = Bukti Potong.
                pajakBreakdown.setJenisPajakBarang(sa.getBreakdownJenisPph());
                session.saveOrUpdate(pajakBreakdown);
                session.flush();

                ais.database.model.akunting.DaftarPengajuanTransfer dpt = pajakBreakdown.getDaftarPengajuanTransfer();
                if (dpt == null) {
                    dpt = (ais.database.model.akunting.DaftarPengajuanTransfer) session
                            .createCriteria(ais.database.model.akunting.DaftarPengajuanTransfer.class)
                            .add(Restrictions.eq("pajak", pajakBreakdown)).setMaxResults(1).uniqueResult();
                }
                if (dpt == null) {
                    dpt = new ais.database.model.akunting.DaftarPengajuanTransfer();
                    dpt.setPajak(pajakBreakdown);
                }
                dpt.setNama("Pembayaran pajak " + pajakBreakdown.getNama());
                dpt.setAktif(true);
                session.saveOrUpdate(dpt);
                pajakBreakdown.setDaftarPengajuanTransfer(dpt);
                session.update(pajakBreakdown);
            } else if (pajakBreakdown != null) {
                pajakBreakdown.setAktif(false);
                session.update(pajakBreakdown);
                if (pajakBreakdown.getDaftarPengajuanTransfer() != null) {
                    pajakBreakdown.getDaftarPengajuanTransfer().setAktif(false);
                    session.update(pajakBreakdown.getDaftarPengajuanTransfer());
                }
            }

            tx.commit();
        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BreakdownTagihanVendorHelper.java:156");}
            Common.tampilErrorJikaAdmin(e);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Entry point
    // ════════════════════════════════════════════════════════════════════════

    /** Buka popup breakdown untuk {@code saldoAwal}. */
    public static void tampilkanPopup(final SaldoAwalMasterAsset saldoAwal, Component parent) {
        try {
            final MyWindow w = new MyWindow();
            w.setTitle("Breakdown Detail Item — " + nvl(saldoAwal.getKode()));
            w.setWidth("95%");
            // Tinggi adaptif (90% layar) + bisa di-resize, supaya ringkasan bawah
            // (Subtotal/PPN/Jumlah Total) tidak terpotong di layar pendek.
            w.setHeight("90%");
            w.setSizable(true);
            w.setMaximizable(true);

            final Vbox root = new Vbox();
            root.setWidth("100%");
            root.setHflex("1");
            // Isi seluruh tinggi window agar grid bisa mengembang & ringkasan tetap di bawah.
            root.setVflex("1");
            root.setParent(w);

            // ── toolbar ─────────────────────────────────────────────────────
            Toolbar tb = new Toolbar();
            tb.setHeight("40px");
            tb.setParent(root);

            final Grid grid = new Grid();
            grid.setWidth("100%");
            // Grid mengisi ruang tersisa (toolbar di atas, ringkasan di bawah) dan
            // SCROLL sendiri saat baris banyak -> ringkasan bawah selalu terlihat.
            grid.setVflex("1");
            grid.setStyle("min-height:200px;");
            grid.setSizedByContent(false);

            final Rows rows = new Rows();

            final Vbox summaryBox = new Vbox();
            summaryBox.setStyle("margin:6px 4px 2px 4px;padding:8px 12px;"
                + "background:#f8fafc;border:1px solid #e2e8f0;border-radius:4px;");
            summaryBox.setWidth("340px");
            summaryBox.setStyle("align-self:flex-end;" + summaryBox.getStyle());

            // + Group
            MyToolbarbuttonConfig btnGroup = new MyToolbarbuttonConfig("+ Grup", null);
            btnGroup.setTooltiptext("Tambah baris judul grup / section");
            btnGroup.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    BreakdownItemTagihanVendor item = new BreakdownItemTagihanVendor();
                    item.setSaldoAwal(saldoAwal);
                    item.setTipe(BreakdownItemTagihanVendor.GROUP);
                    item.setDeskripsi("Nama Kelompok");
                    item.setUrutan(nextUrutan(saldoAwal));
                    Common.refreshSaveOrUpdate(item);
                    rebuildRows(rows, summaryBox, saldoAwal);
                }
            });
            btnGroup.setParent(tb);

            // + Barang
            MyToolbarbuttonConfig btnBarang = new MyToolbarbuttonConfig("+ Barang", null);
            btnBarang.setTooltiptext("Tambah item barang");
            btnBarang.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    Common.refreshSaveOrUpdate(buatItem(saldoAwal, BreakdownItemTagihanVendor.BARANG));
                    rebuildRows(rows, summaryBox, saldoAwal);
                }
            });
            btnBarang.setParent(tb);

            // + Jasa
            MyToolbarbuttonConfig btnJasa = new MyToolbarbuttonConfig("+ Jasa", null);
            btnJasa.setTooltiptext("Tambah item jasa");
            btnJasa.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    Common.refreshSaveOrUpdate(buatItem(saldoAwal, BreakdownItemTagihanVendor.JASA));
                    rebuildRows(rows, summaryBox, saldoAwal);
                }
            });
            btnJasa.setParent(tb);

            // Separator VERTIKAL (pemisah inline). Separator horizontal default memicu
            // pindah baris sehingga "Cetak"/"Tutup" turun ke baris kedua & menimpa grid.
            // Vertikal = tetap satu baris di samping "+ Grup / + Barang / + Jasa".
            org.zkoss.zul.Separator sep = new org.zkoss.zul.Separator();
            sep.setOrient("vertical");
            sep.setParent(tb);

            // Cetak
            MyToolbarbuttonConfig btnCetak = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
            btnCetak.setTooltiptext("Cetak breakdown detail item ke printer");
            btnCetak.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    cetakBreakdown(saldoAwal);
                }
            });
            btnCetak.setParent(tb);

            // Tutup
            MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup", "/img/svg/close.svg");
            btnTutup.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    // detach() melepas window dari PAGE (window dipasang sebagai root via setPage),
                    // setParent(null) tidak akan menutup karena parent memang sudah null.
                    w.detach();
                }
            });
            btnTutup.setParent(tb);

            // ── grid ────────────────────────────────────────────────────────
            grid.setParent(root);

            Columns cols = new Columns();
            cols.setParent(grid);
            mkCol(cols, "#",           "38px");
            mkCol(cols, "Deskripsi",   "32%");
            mkCol(cols, "Qty",         "8%");
            mkCol(cols, "UOM",         "7%");
            mkCol(cols, "Harga/Unit",  "14%");
            mkCol(cols, "Diskon%",     "8%");
            mkCol(cols, "Line Total",  "14%");
            mkCol(cols, "Jenis",       "9%");
            mkCol(cols, "",            "5%");

            rows.setParent(grid);
            rebuildRows(rows, summaryBox, saldoAwal);

            // ── summary ─────────────────────────────────────────────────────
            summaryBox.setParent(root);

            // Pasang window ke PAGE yang AKTIF sebelum onModal(). Versi lama memakai
            // getFirstPage().getFirstRoot() yang bisa null -> window tak ter-attach ->
            // SuspendNotAllowedException "Not attached, <MyWindow null>". Pakai page dari komponen
            // pemicu (parent) yang pasti hidup; fallback ke first page desktop bila parent null.
            org.zkoss.zk.ui.Page halaman = (parent != null && parent.getPage() != null)
                    ? parent.getPage()
                    : Executions.getCurrent().getDesktop().getFirstPage();
            w.setPage(halaman);
            w.onModal();
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Row rendering
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private static void rebuildRows(final Rows rows,
                                    final Vbox summaryBox,
                                    final SaldoAwalMasterAsset saldoAwal) {
        Common.clear(rows);

        // Query memakai native session — currentSession() (session ZK) dibungkus
        // ThreadLocalSessionContext yang transaction-protected sehingga createCriteria melempar
        // "createCriteria is not valid without active transaction". Native session ditutup di
        // finally; entitas hasil hanya dipakai sebagai field skalar saat membangun baris
        // (deskripsi/qty/harga/diskon/jenis/total) sehingga tetap aman walau menjadi detached.
        List<BreakdownItemTagihanVendor> items;
        org.hibernate.Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            items = session.createCriteria(BreakdownItemTagihanVendor.class)
                .add(Restrictions.eq("saldoAwal", saldoAwal))
                .addOrder(Order.asc("urutan"))
                .list();
        } finally {
            if (session != null) {
                try {
                    if (session.isOpen()) {
                        session.disconnect();
                        session.close();
                    }
                } catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BreakdownTagihanVendorHelper.java:338");
                    // abaikan
                }
            }
            HibernateUtil.closeSession();
        }

        int no = 0;
        for (BreakdownItemTagihanVendor iter : items) {
            final BreakdownItemTagihanVendor item = iter;
            final Row row = new Row();
            row.setParent(rows);

            if (BreakdownItemTagihanVendor.GROUP.equals(item.getTipe())) {
                no++;
                // Nomor
                lbl(row, String.valueOf(no)).setStyle("font-size:10px;font-weight:bold;");
                // Deskripsi — editable, bold
                final Textbox tbDesc = tbx(item.getDeskripsi(), "98%");
                tbDesc.setStyle("font-weight:bold;");
                tbDesc.addEventListener("onChange", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        item.setDeskripsi(tbDesc.getValue());
                        Common.refreshSaveOrUpdate(item);
                    }
                });
                tbDesc.setParent(row);
                // Qty, UOM, Harga, Diskon, Total — kosong untuk GROUP
                lbl(row, "").setParent(row);
                lbl(row, "").setParent(row);
                lbl(row, "").setParent(row);
                lbl(row, "").setParent(row);
                lbl(row, "").setParent(row);
                // Jenis combobox
                jenisCombo(item, row, summaryBox, saldoAwal);

            } else {
                // ITEM row — semua kolom editable
                no++;
                lbl(row, String.valueOf(no)).setStyle("font-size:10px;color:#6b7280;");

                // Deskripsi
                final Textbox tbDesc = tbx(item.getDeskripsi(), "98%");
                tbDesc.addEventListener("onChange", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        item.setDeskripsi(tbDesc.getValue());
                        Common.refreshSaveOrUpdate(item);
                    }
                });
                tbDesc.setParent(row);

                // Line Total label — dibuat dulu agar bisa diupdate oleh event qty/harga/diskon
                final Label lblTotal = new Label(fmtNum(item.getLineTotal()));
                lblTotal.setStyle("font-size:11px;color:#1e40af;");

                // Qty
                final Textbox tbQty = tbxNum(fmtQty(item.getQty()), "98%");
                tbQty.addEventListener("onChange", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        item.setQty(parseDbl(tbQty.getValue(), item.getQty()));
                        double lt = item.hitungLineTotal();
                        item.setLineTotal(lt);
                        lblTotal.setValue(fmtNum(lt));
                        Common.refreshSaveOrUpdate(item);
                        rebuildSummary(summaryBox, saldoAwal);
                    }
                });
                tbQty.setParent(row);

                // UOM
                final Textbox tbUom = tbx(item.getUom(), "98%");
                tbUom.addEventListener("onChange", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        item.setUom(tbUom.getValue());
                        Common.refreshSaveOrUpdate(item);
                    }
                });
                tbUom.setParent(row);

                // Harga Satuan
                final Textbox tbHarga = tbxNum(fmtNum(item.getHargaSatuan()), "98%");
                tbHarga.addEventListener("onChange", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        item.setHargaSatuan(parseMoney(tbHarga.getValue(), item.getHargaSatuan()));
                        double lt = item.hitungLineTotal();
                        item.setLineTotal(lt);
                        lblTotal.setValue(fmtNum(lt));
                        Common.refreshSaveOrUpdate(item);
                        rebuildSummary(summaryBox, saldoAwal);
                    }
                });
                tbHarga.setParent(row);

                // Diskon%
                final Textbox tbDiskon = tbxNum(
                    item.getDiskonPersen() > 0 ? String.valueOf(item.getDiskonPersen().intValue()
                        == item.getDiskonPersen() ? (long) item.getDiskonPersen().doubleValue()
                        : item.getDiskonPersen()) : "", "98%");
                tbDiskon.addEventListener("onChange", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        item.setDiskonPersen(parseDbl(tbDiskon.getValue(), 0.0));
                        double lt = item.hitungLineTotal();
                        item.setLineTotal(lt);
                        lblTotal.setValue(fmtNum(lt));
                        Common.refreshSaveOrUpdate(item);
                        rebuildSummary(summaryBox, saldoAwal);
                    }
                });
                tbDiskon.setParent(row);

                // Line Total (read-only display)
                lblTotal.setParent(row);

                // Jenis
                jenisCombo(item, row, summaryBox, saldoAwal);
            }

            // Hapus button (semua tipe)
            MyToolbarbuttonConfig btnHapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
            btnHapus.setTooltiptext("Hapus baris ini");
            btnHapus.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    // Hapus lewat helper aman (mengelola session + transaksi sendiri); delete pada
                    // currentSession ZK transaction-protected berisiko "not valid without active transaction".
                    Common.refreshDelete(item);
                    rebuildRows(rows, summaryBox, saldoAwal);
                }
            });
            btnHapus.setParent(row);
        }

        rebuildSummary(summaryBox, saldoAwal);
    }

    /** Tambah Combobox pilih Jenis (Barang/Jasa) ke row. */
    private static void jenisCombo(final BreakdownItemTagihanVendor item,
                                    Row row,
                                    final Vbox summaryBox,
                                    final SaldoAwalMasterAsset saldoAwal) {
        final Combobox cb = new Combobox();
        cb.setWidth("98%");
        cb.setReadonly(true);
        Comboitem ci1 = new Comboitem("Barang");
        ci1.setValue(BreakdownItemTagihanVendor.BARANG);
        ci1.setParent(cb);
        Comboitem ci2 = new Comboitem("Jasa");
        ci2.setValue(BreakdownItemTagihanVendor.JASA);
        ci2.setParent(cb);
        cb.setValue(BreakdownItemTagihanVendor.JASA.equals(item.getJenis()) ? "Jasa" : "Barang");
        cb.addEventListener("onSelect", new EventListener() {
            public void onEvent(Event e) throws Exception {
                Comboitem sel = cb.getSelectedItem();
                if (sel != null) {
                    item.setJenis((String) sel.getValue());
                    Common.refreshSaveOrUpdate(item);
                    rebuildSummary(summaryBox, saldoAwal);
                }
            }
        });
        cb.setParent(row);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Summary / subtotal
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private static void rebuildSummary(final Vbox summaryBox, final SaldoAwalMasterAsset saldoAwal) {
        Common.clear(summaryBox);

        // Native session (lihat catatan di rebuildRows): hindari createCriteria pada session ZK
        // transaction-protected. Tutup di finally; entitas dipakai untuk penjumlahan skalar saja.
        List<BreakdownItemTagihanVendor> all;
        org.hibernate.Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            all = session.createCriteria(BreakdownItemTagihanVendor.class)
                .add(Restrictions.eq("saldoAwal", saldoAwal))
                .add(Restrictions.eq("tipe", BreakdownItemTagihanVendor.ITEM))
                .list();
        } finally {
            if (session != null) {
                try {
                    if (session.isOpen()) {
                        session.disconnect();
                        session.close();
                    }
                } catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BreakdownTagihanVendorHelper.java:525");
                    // abaikan
                }
            }
            HibernateUtil.closeSession();
        }

        double subtotalBarang = 0, subtotalJasa = 0;
        for (BreakdownItemTagihanVendor it : all) {
            if (BreakdownItemTagihanVendor.JASA.equals(it.getJenis())) subtotalJasa += it.getLineTotal();
            else subtotalBarang += it.getLineTotal();
        }
        final double totalBarangJasa = subtotalBarang + subtotalJasa;

        double ppnPersen = saldoAwal.getBreakdownPpnPersen() != null ? saldoAwal.getBreakdownPpnPersen() : 0;
        final double nilaiPpn = Math.round(totalBarangJasa * ppnPersen / 100.0 * 100) / 100.0;
        final double jumlahTotal = totalBarangJasa + nilaiPpn;
        double buktiPotong = saldoAwal.getBreakdownBuktiPotong() != null ? saldoAwal.getBreakdownBuktiPotong() : 0;
        final double totalTransfer = jumlahTotal - buktiPotong;

        // ── Subtotal Barang & Jasa ─────────────────────────────────────────
        summaryRow(summaryBox, "Subtotal Barang", fmtRp(subtotalBarang), false);
        summaryRow(summaryBox, "Subtotal Jasa",   fmtRp(subtotalJasa),   false);

        // ── PPN row (label + % input + nilai) ─────────────────────────────
        Hbox hPpn = new Hbox();
        hPpn.setStyle("justify-content:space-between;align-items:center;margin:2px 0;");
        hPpn.setWidth("100%");
        hPpn.setParent(summaryBox);

        // label "PPN" + input persen
        Hbox hPpnLeft = new Hbox();
        hPpnLeft.setStyle("align-items:center;gap:4px;");
        hPpnLeft.setParent(hPpn);
        new Label(ais.common.Common.getBahasaConfig("PPN")).setParent(hPpnLeft);
        final Textbox tbPpn = new Textbox(ppnPersen == 0 ? "" : fmtQty(ppnPersen));
        tbPpn.setWidth("42px");
        tbPpn.setTooltiptext("Masukkan % PPN");
        tbPpn.setStyle("font-size:11px;text-align:right;");
        tbPpn.setParent(hPpnLeft);
        new Label("%").setParent(hPpnLeft);

        final Label lblNilaiPpn = new Label(fmtRp(nilaiPpn));
        lblNilaiPpn.setStyle("font-size:11px;");
        lblNilaiPpn.setParent(hPpn);

        // label Jumlah Total (rebuilt when PPN changes)
        final Label lblJumlahTotal = new Label(fmtRp(jumlahTotal));
        lblJumlahTotal.setStyle("font-size:11px;font-weight:bold;");

        // label Total Transfer (rebuilt when PPN or bukti potong changes)
        final Label lblTotalTransfer = new Label(fmtRp(totalTransfer));
        lblTotalTransfer.setStyle("font-size:11px;font-weight:bold;color:#1e40af;");

        tbPpn.addEventListener("onChange", new EventListener() {
            public void onEvent(Event e) throws Exception {
                double pct = parseDbl(tbPpn.getValue(), 0.0);
                double nilaiPpnBaru = Math.round(totalBarangJasa * pct / 100.0 * 100) / 100.0;
                double jumlahBaru   = totalBarangJasa + nilaiPpnBaru;
                double bpVal = saldoAwal.getBreakdownBuktiPotong() != null ? saldoAwal.getBreakdownBuktiPotong() : 0;
                lblNilaiPpn.setValue(fmtRp(nilaiPpnBaru));
                lblJumlahTotal.setValue(fmtRp(jumlahBaru));
                lblTotalTransfer.setValue(fmtRp(jumlahBaru - bpVal));
                saldoAwal.setBreakdownPpnPersen(pct);
                Common.refreshSaveOrUpdate(saldoAwal);
            }
        });

        // ── Jumlah Total ──────────────────────────────────────────────────
        Hbox hJT = new Hbox();
        hJT.setStyle("justify-content:space-between;align-items:center;margin:2px 0;border-top:1px solid #e2e8f0;padding-top:3px;");
        hJT.setWidth("100%");
        hJT.setParent(summaryBox);
        Label lblJTLbl = new Label(ais.common.Common.getBahasaConfig("Jumlah Total"));
        lblJTLbl.setStyle("font-size:11px;font-weight:bold;");
        lblJTLbl.setParent(hJT);
        lblJumlahTotal.setParent(hJT);

        // ── Jenis PPh (untuk POSTING jurnal; NILAI PPh tetap dari Bukti Potong) ──
        Hbox hJenis = new Hbox();
        hJenis.setStyle("justify-content:space-between;align-items:center;margin:2px 0;");
        hJenis.setWidth("100%");
        hJenis.setParent(summaryBox);
        new Label(ais.common.Common.getBahasaConfig("Jenis PPh")).setParent(hJenis);
        final org.zkoss.zul.Combobox cbJenisPph = new org.zkoss.zul.Combobox();
        cbJenisPph.setWidth("160px");
        cbJenisPph.setReadonly(true);
        cbJenisPph.setStyle("font-size:11px;");
        Common.insertComboDanSemua(cbJenisPph, new String[] { "nama", "persen" }, "keterangan",
                ais.database.model.asset.JenisPajakBarang.class, "Tanpa Pajak", Restrictions.eq("aktif", true));
        Common.selectComboItem(cbJenisPph, saldoAwal.getBreakdownJenisPph());
        cbJenisPph.setParent(hJenis);
        cbJenisPph.addEventListener("onChange", new EventListener() {
            public void onEvent(Event e) throws Exception {
                ais.database.model.asset.JenisPajakBarang jp = cbJenisPph.getSelectedItem() == null ? null
                        : (ais.database.model.asset.JenisPajakBarang) cbJenisPph.getSelectedItem().getValue();
                saldoAwal.setBreakdownJenisPph(jp);
                Common.refreshSaveOrUpdate(saldoAwal);
                if (Boolean.TRUE.equals(saldoAwal.getBreakdownAktif())) {
                    sinkronPajakBreakdown(saldoAwal);
                }
            }
        });

        // ── Bukti Potong row (label + input nominal) ───────────────────────
        Hbox hBp = new Hbox();
        hBp.setStyle("justify-content:space-between;align-items:center;margin:2px 0;");
        hBp.setWidth("100%");
        hBp.setParent(summaryBox);
        new Label(ais.common.Common.getBahasaConfig("Bukti Potong")).setParent(hBp);
        final Textbox tbBp = new Textbox(buktiPotong == 0 ? "" : fmtNum(buktiPotong));
        tbBp.setWidth("110px");
        tbBp.setTooltiptext("Nominal bukti potong");
        tbBp.setStyle("font-size:11px;text-align:right;");
        tbBp.setParent(hBp);

        tbBp.addEventListener("onChange", new EventListener() {
            public void onEvent(Event e) throws Exception {
                double bp = parseMoney(tbBp.getValue(), 0.0);
                double ppnPct = saldoAwal.getBreakdownPpnPersen() != null ? saldoAwal.getBreakdownPpnPersen() : 0;
                double ppnVal = Math.round(totalBarangJasa * ppnPct / 100.0 * 100) / 100.0;
                double jt = totalBarangJasa + ppnVal;
                lblTotalTransfer.setValue(fmtRp(jt - bp));
                saldoAwal.setBreakdownBuktiPotong(bp);
                Common.refreshSaveOrUpdate(saldoAwal);
                // Selaraskan baris pajak (nilai Pajak breakdown = Bukti Potong) bila mode breakdown.
                if (Boolean.TRUE.equals(saldoAwal.getBreakdownAktif())) {
                    sinkronPajakBreakdown(saldoAwal);
                }
            }
        });

        // ── Total Transfer ────────────────────────────────────────────────
        Hbox hTT = new Hbox();
        hTT.setStyle("justify-content:space-between;align-items:center;margin:2px 0;border-top:1px solid #e2e8f0;padding-top:3px;");
        hTT.setWidth("100%");
        hTT.setParent(summaryBox);
        Label lblTTLbl = new Label(ais.common.Common.getBahasaConfig("Total Transfer"));
        lblTTLbl.setStyle("font-size:11px;font-weight:bold;color:#1e40af;");
        lblTTLbl.setParent(hTT);
        lblTotalTransfer.setParent(hTT);

        // ── Special Notes ─────────────────────────────────────────────────
        Vbox vNotes = new Vbox();
        vNotes.setStyle("margin-top:8px;border-top:1px dashed #d1d5db;padding-top:6px;");
        vNotes.setWidth("100%");
        vNotes.setParent(summaryBox);
        Label lblNotesHdr = new Label(ais.common.Common.getBahasaConfig("Special Notes and Instructions:"));
        lblNotesHdr.setStyle("font-size:10px;font-weight:bold;color:#374151;");
        lblNotesHdr.setParent(vNotes);
        final Textbox tbNotes = new Textbox(
            saldoAwal.getBreakdownSpecialNotes() != null ? saldoAwal.getBreakdownSpecialNotes() : "");
        tbNotes.setMultiline(true);
        tbNotes.setRows(3);
        tbNotes.setWidth("100%");
        tbNotes.setStyle("font-size:11px;resize:vertical;");
        tbNotes.setTooltiptext("Mohon transfer ke rekening ...");
        tbNotes.setParent(vNotes);
        tbNotes.addEventListener("onChange", new EventListener() {
            public void onEvent(Event e) throws Exception {
                saldoAwal.setBreakdownSpecialNotes(tbNotes.getValue());
                Common.refreshSaveOrUpdate(saldoAwal);
            }
        });
    }

    private static void summaryRow(Vbox parent, String label, String value, boolean bold) {
        Hbox h = new Hbox();
        h.setWidth("100%");
        h.setStyle("justify-content:space-between;align-items:center;margin:2px 0;");
        h.setParent(parent);
        Label lbl = new Label(label);
        lbl.setStyle("font-size:11px;" + (bold ? "font-weight:bold;" : "color:#4b5563;"));
        lbl.setParent(h);
        Label val = new Label(value);
        val.setStyle("font-size:11px;" + (bold ? "font-weight:bold;" : ""));
        val.setParent(h);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Cetak / Print
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Cetak HTML breakdown. Memakai IFRAME tersembunyi (bukan window.open), karena window.open yang
     * dipicu lewat respons AU ZK (bukan gesture klik langsung) DIBLOKIR popup-blocker browser →
     * tombol "seakan tak bisa diklik". Iframe ditulis di window yang sama lalu di-print → andal,
     * tidak terblokir.
     */
    @SuppressWarnings("unchecked")
    public static void cetakBreakdown(SaldoAwalMasterAsset saldoAwal) {
        try {
            if (saldoAwal == null) {
                ais.ui.util.MyMessageboxConfig.show("Data tagihan tidak ditemukan.", "Cetak Breakdown",
                        ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
                return;
            }
            // Beri umpan balik bila breakdown belum dibuat — supaya tombol tidak terasa "mati".
            Object jml = HibernateUtil.currentSession().createCriteria(BreakdownItemTagihanVendor.class)
                    .add(Restrictions.eq("saldoAwal", saldoAwal))
                    .setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
            long jumlahItem = (jml instanceof Number) ? ((Number) jml).longValue() : 0L;
            if (jumlahItem == 0L) {
                ais.ui.util.MyMessageboxConfig.show(
                        "Breakdown belum dibuat untuk tagihan ini. Silakan buat breakdown-nya terlebih dulu.",
                        "Cetak Breakdown", ais.ui.util.MyMessageboxConfig.OK,
                        ais.ui.util.MyMessageboxConfig.INFORMATION);
                return;
            }

            String html = buildHtmlCetak(saldoAwal);
            String js = "(function(){try{"
                + "var _h='" + escJs(html) + "';"
                + "var _o=document.getElementById('_aisBrkdwnPrint'); if(_o&&_o.parentNode){_o.parentNode.removeChild(_o);}"
                + "var _f=document.createElement('iframe'); _f.id='_aisBrkdwnPrint';"
                + "_f.style.position='fixed';_f.style.right='0';_f.style.bottom='0';"
                + "_f.style.width='0';_f.style.height='0';_f.style.border='0';"
                + "document.body.appendChild(_f);"
                + "var _d=_f.contentWindow.document; _d.open(); _d.write(_h); _d.close();"
                + "setTimeout(function(){try{_f.contentWindow.focus();_f.contentWindow.print();}catch(e){}},500);"
                + "}catch(e){}})();";
            Clients.evalJavaScript(js);
        } catch (Exception ex) {
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/asset/helper/BreakdownTagihanVendorHelper.java:748");
            // Tampilkan ke PENGGUNA (bukan hanya admin) agar tombol tidak terasa "mati saat diklik".
            try {
                ais.ui.util.MyMessageboxConfig.show("Gagal mencetak breakdown: " + ex.getMessage(),
                        "Cetak Breakdown", ais.ui.util.MyMessageboxConfig.OK,
                        ais.ui.util.MyMessageboxConfig.EXCLAMATION);
            } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BreakdownTagihanVendorHelper.java:754");
            }
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static String buildHtmlCetak(SaldoAwalMasterAsset saldoAwal) {
        List<BreakdownItemTagihanVendor> items = HibernateUtil.currentSession()
            .createCriteria(BreakdownItemTagihanVendor.class)
            .add(Restrictions.eq("saldoAwal", saldoAwal))
            .addOrder(Order.asc("urutan"))
            .list();

        double subtotalBarang = 0, subtotalJasa = 0;
        for (BreakdownItemTagihanVendor it : items) {
            if (BreakdownItemTagihanVendor.ITEM.equals(it.getTipe())) {
                if (BreakdownItemTagihanVendor.JASA.equals(it.getJenis()))
                    subtotalJasa += it.getLineTotal();
                else
                    subtotalBarang += it.getLineTotal();
            }
        }
        double total = subtotalBarang + subtotalJasa;

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<title>Breakdown Tagihan — ").append(esc(saldoAwal.getKode())).append("</title>");
        sb.append("<style>");
        sb.append("*{box-sizing:border-box;margin:0;padding:0;}");
        sb.append("body{font-family:Arial,sans-serif;font-size:12px;color:#111;padding:24px 32px;}");
        sb.append("h2{font-size:15px;margin-bottom:4px;}");
        sb.append(".meta{font-size:11px;color:#555;margin-bottom:16px;}");
        sb.append("table{width:100%;border-collapse:collapse;margin-bottom:12px;}");
        sb.append("thead th{background:#1e3a5f;color:#fff;padding:7px 8px;text-align:left;font-size:11px;}");
        sb.append("thead th.r{text-align:right;}thead th.c{text-align:center;}");
        sb.append("tbody td{padding:5px 8px;border-bottom:1px solid #e5e7eb;font-size:11px;vertical-align:top;}");
        sb.append("tbody td.r{text-align:right;}tbody td.c{text-align:center;}");
        sb.append(".group td{background:#f1f5f9;font-weight:bold;font-size:11px;}");
        sb.append(".summary{float:right;width:310px;margin-top:12px;}");
        sb.append(".summary table{border-top:2px solid #1e3a5f;}");
        sb.append(".summary td{padding:5px 8px;font-size:11px;border-bottom:1px solid #e5e7eb;}");
        sb.append(".summary td.r{text-align:right;}");
        sb.append(".sum-total td{font-weight:bold;border-top:2px solid #1e3a5f;}");
        sb.append(".notes{clear:both;margin-top:32px;font-size:11px;border-top:1px solid #ddd;padding-top:12px;}");
        sb.append("@media print{body{padding:12px 16px;}}");
        sb.append("</style></head><body>");

        // Header
        sb.append("<h2>Breakdown Detail Item Tagihan Vendor</h2>");
        sb.append("<div class='meta'>");
        sb.append("No. Tagihan: <b>").append(esc(saldoAwal.getKode())).append("</b>");
        if (saldoAwal.getTanggalTagihan() != null)
            sb.append("&nbsp;&nbsp;|&nbsp;&nbsp;Tanggal: <b>")
              .append(Common.dateFormat.get().format(saldoAwal.getTanggalTagihan())).append("</b>");
        if (saldoAwal.getPenyedia() != null && saldoAwal.getPenyedia().getNama() != null)
            sb.append("&nbsp;&nbsp;|&nbsp;&nbsp;Vendor: <b>")
              .append(esc(saldoAwal.getPenyedia().getNama())).append("</b>");
        sb.append("</div>");

        // Table
        sb.append("<table><thead><tr>");
        sb.append("<th style='width:32px'>#</th>");
        sb.append("<th>Description</th>");
        sb.append("<th class='c' style='width:60px'>QTY</th>");
        sb.append("<th class='c' style='width:55px'>UOM</th>");
        sb.append("<th class='r' style='width:110px'>Price / Unit</th>");
        sb.append("<th class='c' style='width:65px'>Discount</th>");
        sb.append("<th class='r' style='width:110px'>Line Total</th>");
        sb.append("</tr></thead><tbody>");

        int no = 0;
        for (BreakdownItemTagihanVendor item : items) {
            if (BreakdownItemTagihanVendor.GROUP.equals(item.getTipe())) {
                sb.append("<tr class='group'><td colspan='7'>")
                  .append(esc(item.getDeskripsi())).append("</td></tr>");
            } else {
                no++;
                sb.append("<tr>");
                sb.append("<td>").append(no).append("</td>");
                sb.append("<td>").append(esc(item.getDeskripsi())).append("</td>");
                sb.append("<td class='c'>").append(fmtQty(item.getQty())).append("</td>");
                sb.append("<td class='c'>").append(esc(item.getUom())).append("</td>");
                sb.append("<td class='r'>").append(fmtNum(item.getHargaSatuan())).append("</td>");
                String dis = item.getDiskonPersen() > 0
                    ? item.getDiskonPersen().intValue() == item.getDiskonPersen().doubleValue()
                        ? ((int) item.getDiskonPersen().doubleValue()) + "%"
                        : item.getDiskonPersen() + "%"
                    : "-";
                sb.append("<td class='c'>").append(dis).append("</td>");
                sb.append("<td class='r'>").append(fmtNum(item.getLineTotal())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table>");

        // Summary
        double ppnPersen = saldoAwal.getBreakdownPpnPersen() != null ? saldoAwal.getBreakdownPpnPersen() : 0;
        double nilaiPpn  = Math.round(total * ppnPersen / 100.0 * 100) / 100.0;
        double jumlahTotal = total + nilaiPpn;
        double buktiPotong = saldoAwal.getBreakdownBuktiPotong() != null ? saldoAwal.getBreakdownBuktiPotong() : 0;
        double totalTransfer = jumlahTotal - buktiPotong;

        sb.append("<div class='summary'><table>");
        sb.append("<tr><td>Subtotal Barang</td><td class='r'>Rp. ").append(fmtNum(subtotalBarang)).append("</td></tr>");
        sb.append("<tr><td>Subtotal Jasa</td><td class='r'>Rp. ").append(fmtNum(subtotalJasa)).append("</td></tr>");
        if (ppnPersen > 0) {
            sb.append("<tr><td>PPN ").append(fmtQty(ppnPersen)).append("%</td><td class='r'>Rp. ")
              .append(fmtNum(nilaiPpn)).append("</td></tr>");
        }
        sb.append("<tr class='sum-total'><td>Jumlah Total</td><td class='r'>Rp. ")
          .append(fmtNum(jumlahTotal)).append("</td></tr>");
        if (buktiPotong > 0) {
            sb.append("<tr><td>Bukti Potong</td><td class='r'>Rp. ").append(fmtNum(buktiPotong)).append("</td></tr>");
            sb.append("<tr class='sum-total'><td>Total Transfer</td><td class='r'>Rp. ")
              .append(fmtNum(totalTransfer)).append("</td></tr>");
        }
        sb.append("</table></div>");

        // Notes
        sb.append("<div class='notes'>");
        sb.append("<b>Special Notes and Instructions:</b><br>");
        if (saldoAwal.getBreakdownSpecialNotes() != null && !saldoAwal.getBreakdownSpecialNotes().trim().isEmpty()) {
            sb.append(esc(saldoAwal.getBreakdownSpecialNotes())).append("<br>");
        } else {
            if (saldoAwal.getPenyedia() != null) {
                String rek = saldoAwal.getPenyedia().getNoRek();
                String bank = saldoAwal.getPenyedia().getBank();
                String atasnama = saldoAwal.getPenyedia().getAtasNama();
                if (rek != null && !rek.trim().isEmpty())
                    sb.append("Mohon transfer pembayaran ke rekening: ").append(esc(bank)).append(" ")
                      .append(esc(rek))
                      .append(atasnama != null ? " a.n. " + esc(atasnama) : "").append("<br>");
            }
        }
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Versi RINGKASAN read-only yang bisa DITEMPEL inline (fragment, style ter-scope ke
     * {@code .ais-bd-view}) — dipakai panel "Daftar Barang / Jasa Tagihan Vendor" saat mode
     * breakdown: daftar item + Subtotal Barang/Jasa, PPN, Jumlah Total, Jenis PPh, Bukti Potong,
     * Total Transfer (sama dgn layout popup/CETAK, tanpa input).
     */
    @SuppressWarnings("unchecked")
    public static String buildHtmlRingkasanEmbed(SaldoAwalMasterAsset saldoAwal) {
        if (saldoAwal == null) {
            return "";
        }
        List<BreakdownItemTagihanVendor> items = HibernateUtil.currentSession()
            .createCriteria(BreakdownItemTagihanVendor.class)
            .add(Restrictions.eq("saldoAwal", saldoAwal))
            .addOrder(Order.asc("urutan"))
            .list();

        double subtotalBarang = 0, subtotalJasa = 0;
        for (BreakdownItemTagihanVendor it : items) {
            if (BreakdownItemTagihanVendor.ITEM.equals(it.getTipe())) {
                if (BreakdownItemTagihanVendor.JASA.equals(it.getJenis()))
                    subtotalJasa += it.getLineTotal();
                else
                    subtotalBarang += it.getLineTotal();
            }
        }
        double total = subtotalBarang + subtotalJasa;
        double ppnPersen = saldoAwal.getBreakdownPpnPersen() != null ? saldoAwal.getBreakdownPpnPersen() : 0;
        double nilaiPpn = Math.round(total * ppnPersen / 100.0 * 100) / 100.0;
        double jumlahTotal = total + nilaiPpn;
        double buktiPotong = saldoAwal.getBreakdownBuktiPotong() != null ? saldoAwal.getBreakdownBuktiPotong() : 0;
        double totalTransfer = jumlahTotal - buktiPotong;
        String jenisPph = saldoAwal.getBreakdownJenisPph() != null && saldoAwal.getBreakdownJenisPph().getNama() != null
            ? saldoAwal.getBreakdownJenisPph().getNama() : "-";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='ais-bd-view'>");
        sb.append("<style>");
        sb.append(".ais-bd-view{font-family:Arial,sans-serif;font-size:12px;color:#111;padding:8px 4px;}");
        sb.append(".ais-bd-view table.it{width:100%;border-collapse:collapse;margin-bottom:8px;}");
        sb.append(".ais-bd-view table.it thead th{background:#1e3a5f;color:#fff;padding:6px 8px;text-align:left;font-size:11px;}");
        sb.append(".ais-bd-view table.it thead th.r{text-align:right;}.ais-bd-view table.it thead th.c{text-align:center;}");
        sb.append(".ais-bd-view table.it tbody td{padding:5px 8px;border-bottom:1px solid #e5e7eb;font-size:11px;vertical-align:top;}");
        sb.append(".ais-bd-view table.it tbody td.r{text-align:right;}.ais-bd-view table.it tbody td.c{text-align:center;}");
        sb.append(".ais-bd-view tr.grp td{background:#f1f5f9;font-weight:bold;}");
        sb.append(".ais-bd-view .sum{float:right;width:320px;margin-top:6px;}");
        sb.append(".ais-bd-view .sum table{width:100%;border-collapse:collapse;border-top:2px solid #1e3a5f;}");
        sb.append(".ais-bd-view .sum td{padding:5px 8px;font-size:11px;border-bottom:1px solid #e5e7eb;}");
        sb.append(".ais-bd-view .sum td.r{text-align:right;}");
        sb.append(".ais-bd-view .sum tr.tot td{font-weight:bold;border-top:2px solid #1e3a5f;color:#1e40af;}");
        sb.append(".ais-bd-view .clr{clear:both;}");
        sb.append("</style>");

        sb.append("<table class='it'><thead><tr>");
        sb.append("<th style='width:32px'>#</th><th>Deskripsi</th>");
        sb.append("<th class='c' style='width:60px'>Qty</th><th class='c' style='width:55px'>UOM</th>");
        sb.append("<th class='r' style='width:120px'>Harga/Unit</th>");
        sb.append("<th class='c' style='width:70px'>Diskon%</th>");
        sb.append("<th class='r' style='width:120px'>Line Total</th>");
        sb.append("</tr></thead><tbody>");
        int no = 0;
        for (BreakdownItemTagihanVendor item : items) {
            if (BreakdownItemTagihanVendor.GROUP.equals(item.getTipe())) {
                sb.append("<tr class='grp'><td colspan='7'>").append(esc(item.getDeskripsi())).append("</td></tr>");
            } else {
                no++;
                sb.append("<tr><td>").append(no).append("</td>");
                sb.append("<td>").append(esc(item.getDeskripsi())).append("</td>");
                sb.append("<td class='c'>").append(fmtQty(item.getQty())).append("</td>");
                sb.append("<td class='c'>").append(esc(item.getUom())).append("</td>");
                sb.append("<td class='r'>").append(fmtNum(item.getHargaSatuan())).append("</td>");
                String dis = item.getDiskonPersen() != null && item.getDiskonPersen() > 0
                    ? (fmtQty(item.getDiskonPersen()) + "%") : "-";
                sb.append("<td class='c'>").append(dis).append("</td>");
                sb.append("<td class='r'>").append(fmtNum(item.getLineTotal())).append("</td></tr>");
            }
        }
        sb.append("</tbody></table>");

        sb.append("<div class='sum'><table>");
        sb.append("<tr><td>Subtotal Barang</td><td class='r'>Rp. ").append(fmtNum(subtotalBarang)).append("</td></tr>");
        sb.append("<tr><td>Subtotal Jasa</td><td class='r'>Rp. ").append(fmtNum(subtotalJasa)).append("</td></tr>");
        if (ppnPersen > 0) {
            sb.append("<tr><td>PPN ").append(fmtQty(ppnPersen)).append("%</td><td class='r'>Rp. ")
              .append(fmtNum(nilaiPpn)).append("</td></tr>");
        }
        sb.append("<tr class='tot'><td>Jumlah Total</td><td class='r'>Rp. ")
          .append(fmtNum(jumlahTotal)).append("</td></tr>");
        sb.append("<tr><td>Jenis PPh</td><td class='r'>").append(esc(jenisPph)).append("</td></tr>");
        if (buktiPotong > 0) {
            sb.append("<tr><td>Bukti Potong</td><td class='r'>Rp. ").append(fmtNum(buktiPotong)).append("</td></tr>");
            sb.append("<tr class='tot'><td>Total Transfer</td><td class='r'>Rp. ")
              .append(fmtNum(totalTransfer)).append("</td></tr>");
        }
        sb.append("</table></div><div class='clr'></div>");
        sb.append("</div>");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Factory helpers
    // ════════════════════════════════════════════════════════════════════════

    private static BreakdownItemTagihanVendor buatItem(SaldoAwalMasterAsset saldoAwal, String jenis) {
        BreakdownItemTagihanVendor item = new BreakdownItemTagihanVendor();
        item.setSaldoAwal(saldoAwal);
        item.setTipe(BreakdownItemTagihanVendor.ITEM);
        item.setJenis(jenis);
        item.setDeskripsi("");
        item.setQty(1.0);
        item.setHargaSatuan(0.0);
        item.setDiskonPersen(0.0);
        item.setLineTotal(0.0);
        item.setUrutan(nextUrutan(saldoAwal));
        return item;
    }

    private static int nextUrutan(SaldoAwalMasterAsset saldoAwal) {
        Number n = (Number) HibernateUtil.currentSession()
            .createCriteria(BreakdownItemTagihanVendor.class)
            .add(Restrictions.eq("saldoAwal", saldoAwal))
            .setProjection(Projections.max("urutan"))
            .uniqueResult();
        return n == null ? 10 : n.intValue() + 10;
    }

    // ════════════════════════════════════════════════════════════════════════
    // UI mini-factories
    // ════════════════════════════════════════════════════════════════════════

    private static void mkCol(Columns cols, String label, String width) {
        Column c = new Column(label);
        if (width != null) c.setWidth(width);
        c.setParent(cols);
    }

    private static Label lbl(Component parent, String val) {
        Label l = new Label(val == null ? "" : val);
        l.setParent(parent);
        return l;
    }

    private static Textbox tbx(String val, String width) {
        Textbox t = new Textbox(val == null ? "" : val);
        if (width != null) t.setWidth(width);
        return t;
    }

    private static Textbox tbxNum(String val, String width) {
        Textbox t = tbx(val, width);
        t.setStyle("text-align:right;");
        return t;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Formatting & parsing
    // ════════════════════════════════════════════════════════════════════════

    private static String nvl(String s) { return s == null ? "" : s; }

    private static String fmtNum(Double v) {
        if (v == null || v == 0.0) return "0";
        try { return NF.format(v); } catch (Exception e) { return String.valueOf(v); }
    }

    private static String fmtQty(Double v) {
        if (v == null) return "";
        if (v == v.longValue()) return String.valueOf(v.longValue());
        return String.valueOf(v);
    }

    private static String fmtRp(double v) { return "Rp. " + fmtNum(v); }

    private static double parseDbl(String s, Double fallback) {
        if (s == null || s.trim().isEmpty()) return fallback == null ? 0 : fallback;
        try { return Double.parseDouble(s.replace(".", "").replace(",", ".")); }
        catch (NumberFormatException ex) { return fallback == null ? 0 : fallback; }
    }

    private static double parseMoney(String s, Double fallback) {
        if (s == null || s.trim().isEmpty()) return fallback == null ? 0 : fallback;
        // Hapus pemisah ribuan (titik), ganti koma desimal ke titik
        String clean = s.replaceAll("[^0-9,.]", "");
        // Jika ada lebih dari satu titik, asumsi format 1.000.000 → hapus titik
        if (clean.indexOf('.') != clean.lastIndexOf('.'))
            clean = clean.replace(".", "");
        clean = clean.replace(",", ".");
        try { return Double.parseDouble(clean); }
        catch (NumberFormatException ex) { return fallback == null ? 0 : fallback; }
    }

    /** Escape HTML entities. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Escape untuk JavaScript single-quoted string. */
    private static String escJs(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 64);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '\'') sb.append("\\'");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') { /* skip */ }
            else sb.append(c);
        }
        return sb.toString();
    }
}
