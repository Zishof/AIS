package ais.action.master.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.StokOpname;
import ais.database.model.inventory.Toko;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Stok Opname (Penyesuaian Stok) — konversi dari {@code modul/kantin/stok/index.jsp} ke ZK.
 *
 * <p>Mencatat hasil hitung fisik barang lalu menyamakannya dengan stok sistem: selisih (fisik
 * dikurangi sistem) disimpan dan stok produk dihitung ulang otomatis lewat {@link StokKantinUtil}.
 * Tersedia juga tombol "Sinkronkan Stok" untuk menghitung ulang seluruh produk, dan unduh Excel.</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang yang tidak boleh melihat toko lain hanya melihat &amp; mencatat
 * opname tokonya sendiri (pilihan toko dikunci).</p>
 */
public class StokOpnameKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host; // wired dari ZUL

    private Toko scopeToko;
    private boolean adminBolehPilihToko = true;

    // Filter
    private Textbox txtFilterProduk;
    private Datebox dtFilterBulan;
    private Combobox cboFilterToko;
    private Grid grdList;

    // Form dialog
    private MyWindow formWin;
    private Long editId;
    private Combobox cboFormToko;
    private Combobox cboFormProduk;
    private Datebox dtWaktu;
    private MyDoublebox dbFisik;
    private Label lblSistem;
    private Label lblSelisih;
    private Textbox txtKet;
    private final Map<Long, Double> produkStokMap = new HashMap<Long, Double>();

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
        resolveScopeToko();

        DashboardUiKit.attachIntro(comp, scopeToko == null ? "Stok Opname (Penyesuaian Stok)"
                : "Stok Opname — " + scopeToko.getNama(),
                "Samakan stok di aplikasi dengan stok fisik nyata di kios/gudang. Catat hasil hitung fisik, "
                        + "selisihnya tersimpan dan stok produk langsung diperbarui otomatis.");

        if (host == null) {
            return;
        }
        buildUI();
        loadList();
    }

    private void resolveScopeToko() {
        try {
            Toko ct = Common.getCurrentToko();
            if (ct != null && ct.getId() != null) {
                Boolean b = ct.getBolehMelihatTokolain();
                if (b == null || !b.booleanValue()) {
                    scopeToko = ct;
                    adminBolehPilihToko = false;
                }
            }
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/StokOpnameKantinAction.java:116");
        }
    }

    // ======================== Build UI ========================

    private void buildUI() {
        host.getChildren().clear();

        Div filterCard = new Div();
        filterCard.setSclass("ais-crud-filter-card");
        filterCard.setParent(host);
        Div fbody = new Div();
        fbody.setSclass("ais-crud-filter-body");
        fbody.setParent(filterCard);
        fbody.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Cari riwayat opname, saring per produk/bulan/toko, atau catat opname baru. "
                        + "Tombol Sinkronkan Stok menghitung ulang seluruh stok dari rekam jejak.")));

        Div ctr = new Div();
        ctr.setStyle("display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;");
        ctr.setParent(fbody);

        txtFilterProduk = new Textbox();
        txtFilterProduk.setWidth("100%");
        txtFilterProduk.setTooltiptext("nama / kode produk");
        txtFilterProduk.addEventListener("onOK", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                loadList();
            }
        });
        ctr.appendChild(kolom("Nama / Kode Produk", txtFilterProduk, "1 1 200px"));

        dtFilterBulan = new Datebox();
        dtFilterBulan.setWidth("150px");
        dtFilterBulan.setFormat("MM-yyyy");
        dtFilterBulan.setTooltiptext("Pilih bulan opname (kosongkan untuk semua)");
        ctr.appendChild(kolom("Bulan Opname", dtFilterBulan, "0 0 auto"));

        if (adminBolehPilihToko) {
            cboFilterToko = new Combobox();
            cboFilterToko.setWidth("100%");
            cboFilterToko.setReadonly(true);
            Common.insertComboDanSemua(cboFilterToko, "nama", Toko.class, Restrictions.eq("aktif", true));
            ctr.appendChild(kolom("Toko / Kios", cboFilterToko, "1 1 180px"));
        }

        Button bSaring = new Button("Saring");
        bSaring.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                loadList();
            }
        });
        MyToolbarbuttonConfig bCatat = new MyToolbarbuttonConfig("Catat Opname", "/img/tambah.png");
        bCatat.setTooltiptext("Catat hasil hitung fisik baru");
        bCatat.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                bukaForm(null);
            }
        });
        Button bSinkron = new Button("Sinkronkan Stok");
        bSinkron.setTooltiptext("Hitung ulang seluruh stok produk dari rekam jejak pengadaan/opname/penjualan");
        bSinkron.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onSinkron();
            }
        });
        Button bExcel = new Button("Unduh Excel");
        bExcel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                downloadExcel();
            }
        });
        Button bScan = new Button("SO by Scan (HP/PDT)");
        bScan.setTooltiptext("Stok opname dengan scan barcode kamera HP / alat PDT");
        bScan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                org.zkoss.zk.ui.util.Clients.evalJavaScript(
                        "window.open('" + Common.ROOT + "/pages/master/inventory/stok_opname_scan.zul','_blank')");
            }
        });
        Hlayout tools = new Hlayout();
        tools.setStyle("gap:6px;flex-wrap:wrap;");
        tools.appendChild(bSaring);
        tools.appendChild(bCatat);
        tools.appendChild(bScan);
        tools.appendChild(bSinkron);
        tools.appendChild(bExcel);
        ctr.appendChild(kolom(" ", tools, "1 1 auto"));

        Div dataCard = new Div();
        dataCard.setSclass("ais-crud-data-card");
        dataCard.setParent(host);

        grdList = new Grid();
        grdList.setSclass("dgrid");
        grdList.setWidth("100%");
        grdList.setStyle("border:0;background:transparent;");
        grdList.setMold("paging");
        grdList.setPageSize(15);
        grdList.setParent(dataCard);
        Columns cols = new Columns();
        cols.setParent(grdList);
        addCol(cols, "Waktu Opname", "150px");
        addCol(cols, "Produk", null);
        if (scopeToko == null) {
            addCol(cols, "Toko", "140px");
        }
        addCol(cols, "Stok Sistem", "90px");
        addCol(cols, "Stok Fisik", "90px");
        addCol(cols, "Selisih", "90px");
        addCol(cols, "Keterangan", "180px");
        addCol(cols, "", "100px");
    }

    // ======================== List ========================

    private String filterSql() {
        StringBuilder w = new StringBuilder(" WHERE 1=1 ");
        if (scopeToko != null) {
            w.append(" AND a.toko = ").append(scopeToko.getId()).append(" ");
        } else if (cboFilterToko != null && cboFilterToko.getSelectedItem() != null
                && cboFilterToko.getSelectedItem().getValue() instanceof Toko) {
            Toko t = (Toko) cboFilterToko.getSelectedItem().getValue();
            w.append(" AND a.toko = ").append(t.getId()).append(" ");
        }
        String kw = txtFilterProduk == null ? "" : txtFilterProduk.getValue().trim().replace("'", "''");
        if (!kw.isEmpty()) {
            w.append(" AND (p.nama ILIKE '%").append(kw).append("%' OR p.kode ILIKE '%").append(kw).append("%') ");
        }
        if (dtFilterBulan != null && dtFilterBulan.getValue() != null) {
            String bln = new java.text.SimpleDateFormat("yyyy-MM").format(dtFilterBulan.getValue());
            w.append(" AND TO_CHAR(a.waktuopname,'YYYY-MM') = '").append(bln).append("' ");
        }
        return w.toString();
    }

    private void loadList() {
        if (grdList == null) {
            return;
        }
        Rows rows = new Rows();
        String sql = "SELECT a.id, TO_CHAR(a.waktuopname,'DD-MM-YYYY HH24:MI'), p.nama, COALESCE(p.kode,''), "
                + "t.nama, a.stoksistem, a.stokfisik, a.selisih, COALESCE(a.keterangan,''), a.produk, a.toko "
                + "FROM koperasi.stok_opname a "
                + "LEFT JOIN koperasi.produk p ON a.produk = p.id "
                + "INNER JOIN koperasi.toko t ON a.toko = t.id "
                + filterSql() + " ORDER BY a.waktuopname DESC, a.id DESC LIMIT 500";
        List<Object[]> data = rows(sql);
        if (data.isEmpty()) {
            Row r = new Row();
            r.setParent(rows);
            Label l = new Label(ais.common.Common.getBahasaConfig("Belum ada data opname."));
            l.setStyle("color:#64748b;padding:10px;");
            l.setParent(r);
            ganti(grdList, rows);
            return;
        }
        for (Object[] d : data) {
            final Long id = lng(d[0]);
            final Long produkId = lng(d[9]);
            final Long tokoId = lng(d[10]);
            final String namaProduk = str(d[2]) + (str(d[3]).isEmpty() ? "" : " [" + str(d[3]) + "]");
            final double sistem = num(d[5]);
            final double fisik = num(d[6]);
            final String ket = str(d[8]);

            Row row = new Row();
            row.setValign("middle");
            row.setParent(rows);
            new Label(str(d[1])).setParent(row);
            Label lp = new Label(namaProduk);
            lp.setStyle("font-weight:600;color:#1d4ed8;");
            lp.setParent(row);
            if (scopeToko == null) {
                new Label(str(d[4])).setParent(row);
            }
            Label ls = new Label(qty(sistem));
            ls.setStyle("color:#64748b;");
            ls.setParent(row);
            Label lf = new Label(qty(fisik));
            lf.setStyle("font-weight:700;");
            lf.setParent(row);
            double selisih = num(d[7]);
            Label lse = new Label((selisih > 0 ? "+" : "") + qty(selisih));
            lse.setStyle("font-weight:800;color:" + (selisih < 0 ? "#dc2626" : selisih > 0 ? "#16a34a" : "#64748b") + ";");
            lse.setParent(row);
            Label lk = new Label(ket);
            lk.setStyle("color:#64748b;font-size:11px;");
            lk.setParent(row);

            Hlayout aksi = new Hlayout();
            aksi.setStyle("gap:4px;");
            Button bEdit = new Button("Ubah");
            bEdit.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    editId = id;
                    bukaFormEdit(id, tokoId, produkId, namaProduk, sistem, fisik, ket);
                }
            });
            aksi.appendChild(bEdit);
            Button bHapus = new Button("Hapus");
            bHapus.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    onHapus(id, produkId);
                }
            });
            aksi.appendChild(bHapus);
            aksi.setParent(row);
        }
        ganti(grdList, rows);
    }

    // ======================== Form ========================

    private void bukaForm(Long id) throws Exception {
        editId = id;
        tutupForm();

        formWin = new MyWindow(id == null ? "Catat Opname Baru" : "Ubah Data Opname", "normal", true);
        formWin.setParent(host.getPage().getFirstRoot());
        formWin.setWidth("520px");

        Vlayout box = new Vlayout();
        box.setStyle("padding:10px;");
        box.setParent(formWin);

        // Toko
        if (adminBolehPilihToko) {
            box.appendChild(labelMini("Toko / Kios *"));
            cboFormToko = new Combobox();
            cboFormToko.setWidth("100%");
            cboFormToko.setReadonly(true);
            Common.insertCombo(cboFormToko, "nama", Toko.class, Restrictions.eq("aktif", true));
            cboFormToko.addEventListener("onSelect", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    loadProdukCombo(tokoTerpilihForm());
                }
            });
            box.appendChild(cboFormToko);
        }

        // Produk
        box.appendChild(labelMini("Produk / Barang *"));
        cboFormProduk = new Combobox();
        cboFormProduk.setWidth("100%");
        cboFormProduk.setReadonly(true);
        cboFormProduk.addEventListener("onSelect", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                isiStokSistem();
            }
        });
        box.appendChild(cboFormProduk);

        // Waktu
        box.appendChild(labelMini("Waktu Opname *"));
        dtWaktu = new Datebox();
        dtWaktu.setWidth("100%");
        dtWaktu.setFormat("dd-MM-yyyy HH:mm");
        dtWaktu.setValue(new Date());
        box.appendChild(dtWaktu);

        // Stok sistem + fisik + selisih
        Hlayout rowStok = new Hlayout();
        rowStok.setStyle("gap:10px;margin-top:6px;");
        rowStok.setParent(box);

        Vlayout cSistem = new Vlayout();
        cSistem.setSpacing("0");
        cSistem.appendChild(labelMini("Stok Sistem"));
        lblSistem = new Label("0");
        lblSistem.setStyle("font-weight:800;color:#64748b;padding:6px 0;");
        cSistem.appendChild(lblSistem);
        rowStok.appendChild(cSistem);

        Vlayout cFisik = new Vlayout();
        cFisik.setSpacing("0");
        cFisik.appendChild(labelMini("Stok Fisik (nyata) *"));
        dbFisik = new MyDoublebox((Double) null);
        dbFisik.setWidth("120px");
        dbFisik.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                hitungSelisih();
            }
        });
        cFisik.appendChild(dbFisik);
        rowStok.appendChild(cFisik);

        Vlayout cSel = new Vlayout();
        cSel.setSpacing("0");
        cSel.appendChild(labelMini("Selisih"));
        lblSelisih = new Label("0");
        lblSelisih.setStyle("font-weight:800;padding:6px 0;");
        cSel.appendChild(lblSelisih);
        rowStok.appendChild(cSel);

        // Keterangan
        box.appendChild(labelMini("Keterangan / alasan selisih"));
        txtKet = new Textbox();
        txtKet.setMultiline(true);
        txtKet.setRows(2);
        txtKet.setWidth("100%");
        txtKet.setTooltiptext("mis. barang basi, hilang, rusak");
        box.appendChild(txtKet);

        Hlayout btns = new Hlayout();
        btns.setStyle("gap:8px;justify-content:flex-end;margin-top:12px;");
        btns.setParent(box);
        Button bBatal = new Button("Batal");
        bBatal.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                tutupForm();
            }
        });
        btns.appendChild(bBatal);
        MyToolbarbuttonConfig bSimpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        bSimpan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onSimpan();
            }
        });
        btns.appendChild(bSimpan);

        // Default: pedagang langsung muat produk tokonya
        if (!adminBolehPilihToko && scopeToko != null) {
            loadProdukCombo(scopeToko.getId());
        }

        formWin.doOverlapped();
    }

    private void bukaFormEdit(Long id, Long tokoId, Long produkId, String namaProduk, double sistem, double fisik,
            String ket) throws Exception {
        bukaForm(id);
        if (adminBolehPilihToko && cboFormToko != null && tokoId != null) {
            Common.selectComboItem(cboFormToko, new Toko(tokoId));
        }
        loadProdukCombo(tokoId);
        // pilih produk di combo
        if (cboFormProduk != null && produkId != null) {
            for (Object o : cboFormProduk.getItems()) {
                Comboitem ci = (Comboitem) o;
                if (produkId.equals(ci.getValue())) {
                    cboFormProduk.setSelectedItem(ci);
                    break;
                }
            }
        }
        // Pertahankan stok sistem historis saat opname dilakukan (jangan tergantikan stok terkini).
        if (lblSistem != null) {
            lblSistem.setValue(qty(sistem));
        }
        if (dbFisik != null) {
            dbFisik.setValue(fisik);
        }
        if (txtKet != null) {
            txtKet.setValue(ket);
        }
        hitungSelisih();
    }

    private Long tokoTerpilihForm() {
        if (!adminBolehPilihToko) {
            return scopeToko == null ? null : scopeToko.getId();
        }
        if (cboFormToko != null && cboFormToko.getSelectedItem() != null
                && cboFormToko.getSelectedItem().getValue() instanceof Toko) {
            return ((Toko) cboFormToko.getSelectedItem().getValue()).getId();
        }
        return null;
    }

    /** Muat produk milik toko + stok sistem terkini (subquery rekam jejak), simpan ke peta. */
    private void loadProdukCombo(Long tokoId) {
        if (cboFormProduk == null) {
            return;
        }
        cboFormProduk.getItems().clear();
        cboFormProduk.setValue("");
        produkStokMap.clear();
        if (tokoId == null) {
            return;
        }
        String sql = "SELECT a.id, a.nama, COALESCE(a.kode,''), ("
                + "COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk pp WHERE pp.produk=a.id),0)"
                + "+COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname so WHERE so.produk=a.id),0)"
                + "-COALESCE((SELECT SUM(qty) FROM koperasi.pembelian pb WHERE pb.produk=a.id),0)"
                // konsisten dengan StokKantinUtil.recomputeStokProduk: stok sistem juga dikurangi
                // pemakaian bahan baku, agar baseline opname tidak overstate utk produk yang dipakai.
                + "-COALESCE((SELECT SUM(qty) FROM koperasi.pemakaian_bahan_baku pmb WHERE pmb.produk=a.id),0)) AS stok "
                + "FROM koperasi.produk a WHERE a.aktif=true AND a.toko=" + tokoId + " ORDER BY a.nama ASC";
        for (Object[] r : rows(sql)) {
            Long pid = lng(r[0]);
            String nama = str(r[1]) + (str(r[2]).isEmpty() ? "" : " [" + str(r[2]) + "]");
            double stok = num(r[3]);
            produkStokMap.put(pid, Double.valueOf(stok));
            Comboitem ci = new Comboitem(nama);
            ci.setValue(pid);
            ci.setParent(cboFormProduk);
        }
    }

    private void isiStokSistem() {
        Long pid = produkTerpilih();
        double stok = (pid != null && produkStokMap.containsKey(pid)) ? produkStokMap.get(pid).doubleValue() : 0;
        if (lblSistem != null) {
            lblSistem.setValue(qty(stok));
        }
        hitungSelisih();
    }

    private Long produkTerpilih() {
        if (cboFormProduk == null || cboFormProduk.getSelectedItem() == null) {
            return null;
        }
        return (Long) cboFormProduk.getSelectedItem().getValue();
    }

    private void hitungSelisih() {
        double sistem = parseLabel(lblSistem);
        double fisik = (dbFisik == null || dbFisik.getValue() == null) ? 0 : dbFisik.getValue();
        double selisih = fisik - sistem;
        if (lblSelisih != null) {
            lblSelisih.setValue((selisih > 0 ? "+" : "") + qty(selisih));
            lblSelisih.setStyle("font-weight:800;padding:6px 0;color:"
                    + (selisih < 0 ? "#dc2626" : selisih > 0 ? "#16a34a" : "#64748b") + ";");
        }
    }

    private void tutupForm() {
        if (formWin != null) {
            formWin.setParent(null);
            formWin = null;
        }
    }

    // ======================== Simpan / Hapus / Sinkron ========================

    private void onSimpan() throws Exception {
        Long tokoId = tokoTerpilihForm();
        Long produkId = produkTerpilih();
        if (tokoId == null) {
            MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu diminta untuk memilih Toko / Kios terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar Toko / Kios; (2) pilih salah satu Toko / Kios yang sesuai; (3) ulangi kembali proses ini.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        if (produkId == null) {
            MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu diminta untuk memilih Produk terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar Produk; (2) pilih salah satu Produk yang sesuai; (3) ulangi kembali proses ini.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        if (dbFisik == null || dbFisik.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, kolom Stok Fisik (hasil penghitungan nyata) belum diisi. Langkah yang dapat dilakukan: (1) lakukan penghitungan fisik atas produk; (2) masukkan hasilnya pada kolom Stok Fisik; (3) ulangi kembali proses penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }

        try {
            org.hibernate.Session s = HibernateUtil.currentSession();
            StokOpname so = (editId == null) ? new StokOpname() : (StokOpname) s.get(StokOpname.class, editId);
            if (so == null) {
                so = new StokOpname();
            }
            so.setToko((Toko) s.get(Toko.class, tokoId));
            so.setProduk((Produk) s.get(Produk.class, produkId));
            so.setStokSistem(Double.valueOf(parseLabel(lblSistem)));
            so.setStokFisik(dbFisik.getValue());
            // WAJIB ditulis eksplisit -- lihat catatan di StokOpnameScanUtil.simpanOpname soal
            // getSelisih() (computed getter) yang TIDAK boleh diandalkan mengisi kolom ini otomatis.
            so.setSelisih(Double.valueOf(so.getStokFisik() - so.getStokSistem()));
            so.setWaktuOpname(dtWaktu == null || dtWaktu.getValue() == null ? new Date() : dtWaktu.getValue());
            so.setKeterangan(txtKet == null ? null : txtKet.getValue());
            try {
                so.setOleh(Common.getCurrentUser().getUserId());
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/StokOpnameKantinAction.java:600");
            }
            Common.refreshSaveOrUpdate(s, so);

            // Stok produk dihitung ulang otomatis (selisih opname memengaruhi stok).
            StokKantinUtil.recomputeStokProduk(produkId);

            tutupForm();
            loadList();
            MyMessageboxConfig.show("Data Stok Opname telah berhasil disimpan.", "Info",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            MyMessageboxConfig.showFormat(
                    "Mohon maaf, data gagal disimpan. Keterangan: {V1}. Langkah yang dapat dilakukan: (1) periksa kembali kelengkapan dan kebenaran data yang dimasukkan; (2) ulangi kembali proses penyimpanan; (3) apabila kendala masih berlanjut, mohon menghubungi Administrator sistem.",
                    "Kesalahan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, e.getMessage());
        }
    }

    private void onHapus(final Long id, final Long produkId) throws Exception {
        if (id == null) {
            return;
        }
        MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data opname ini? Perlu diperhatikan bahwa stok produk akan dihitung ulang dan data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
                MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        int i = new Integer(e.getData().toString());
                        if (i != MyMessageboxConfig.OK) {
                            return;
                        }
                        try {
                            org.hibernate.Session s = HibernateUtil.currentSession();
                            StokOpname so = (StokOpname) s.get(StokOpname.class, id);
                            if (so != null) {
                                Common.refreshDelete(s, so);
                            }
                            StokKantinUtil.recomputeStokProduk(produkId);
                            loadList();
                            MyMessageboxConfig.show("Data opname telah berhasil dihapus.", "Info",
                                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        } catch (Exception ex) {
                            Common.tampilErrorJikaAdmin(ex);
                        }
                    }
                });
    }

    private void onSinkron() throws Exception {
        MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghitung ulang seluruh stok produk berdasarkan rekam jejak (pengadaan, opname, dan penjualan)? Perlu diperhatikan bahwa proses ini dapat memakan waktu.", "Sinkronkan Stok", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
                MyMessageboxConfig.QUESTION, new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        int i = new Integer(e.getData().toString());
                        if (i != MyMessageboxConfig.OK) {
                            return;
                        }
                        try {
                            StokKantinUtil.recomputeStokToko(scopeToko == null ? null : scopeToko.getId());
                            loadList();
                            MyMessageboxConfig.show("Proses sinkronisasi stok telah selesai dilakukan.", "Info",
                                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        } catch (Exception ex) {
                            Common.tampilErrorJikaAdmin(ex);
                        }
                    }
                });
    }

    // ======================== Excel ========================

    private void downloadExcel() {
        try {
            String sql = "SELECT TO_CHAR(a.waktuopname,'DD-MM-YYYY HH24:MI'), p.nama, COALESCE(p.kode,''), t.nama, "
                    + "a.stoksistem, a.stokfisik, a.selisih, COALESCE(a.keterangan,'') "
                    + "FROM koperasi.stok_opname a LEFT JOIN koperasi.produk p ON a.produk=p.id "
                    + "INNER JOIN koperasi.toko t ON a.toko=t.id " + filterSql() + " ORDER BY a.waktuopname DESC";
            List<Object[]> data = rows(sql);
            org.zkoss.poi.ss.usermodel.Workbook wb = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook();
            org.zkoss.poi.ss.usermodel.Sheet sheet = wb.createSheet("Stok Opname");
            int rn = 0;
            org.zkoss.poi.ss.usermodel.Row head = sheet.createRow(rn++);
            String[] cols = { "Waktu Opname", "Produk", "Kode", "Toko", "Stok Sistem", "Stok Fisik", "Selisih",
                    "Keterangan" };
            for (int i = 0; i < cols.length; i++) {
                head.createCell(i).setCellValue(cols[i]);
            }
            for (Object[] d : data) {
                org.zkoss.poi.ss.usermodel.Row r = sheet.createRow(rn++);
                r.createCell(0).setCellValue(str(d[0]));
                r.createCell(1).setCellValue(str(d[1]));
                r.createCell(2).setCellValue(str(d[2]));
                r.createCell(3).setCellValue(str(d[3]));
                r.createCell(4).setCellValue(num(d[4]));
                r.createCell(5).setCellValue(num(d[5]));
                r.createCell(6).setCellValue(num(d[6]));
                r.createCell(7).setCellValue(str(d[7]));
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            wb.write(baos);
            org.zkoss.zul.Filedownload.save(baos.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "Stok_Opname_" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx");
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ======================== Util ========================

    private Div kolom(String label, Component field, String flex) {
        Div d = new Div();
        d.setStyle("display:flex;flex-direction:column;flex:" + flex + ";");
        d.appendChild(labelMini(label));
        d.appendChild(field);
        return d;
    }

    private static Label labelMini(String text) {
        Label l = new Label(text);
        l.setStyle("font-size:11px;font-weight:700;color:#64748b;display:block;margin-bottom:3px;");
        return l;
    }

    private void addCol(Columns columns, String label, String width) {
        Column c = new Column();
        c.setLabel(label);
        if (width != null) {
            c.setWidth(width);
        }
        c.setParent(columns);
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

    private static double parseLabel(Label l) {
        if (l == null) {
            return 0;
        }
        try {
            return Double.parseDouble(l.getValue().replace("+", "").replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /** Tampilkan qty: tanpa desimal bila bulat. */
    private static String qty(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
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

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Long lng(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }
}
