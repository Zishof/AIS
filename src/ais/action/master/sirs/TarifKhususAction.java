package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.TarifKhususPunyaAlatMedisDetailAction;
import ais.action.master.sirs.detail.TarifKhususPunyaItemDetailAction;
import ais.action.master.sirs.detail.TarifKhususPunyaTindakanDetailAction;
import ais.action.master.sirs.helper.AmbilDataAsuransiBanbox;
import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.TarifKhusus;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD pendataan {@link TarifKhusus} pada modul SIRS: paket tarif khusus (kontrak) yang
 * berlaku untuk kombinasi dokter, asuransi, komunitas, dan/atau pasien tertentu, dalam rentang
 * tanggal berlaku. Detail rincian tarif per kategori (tindakan/perawatan, alat medis, item/obat,
 * bed/tempat tidur) dikelola lewat sub-layar terpisah yang dimuat saat baris di-expand (lihat
 * {@link TarifKhususRenderer}). Dibangun di atas kerangka {@link GenericCrudAction}.
 */
public class TarifKhususAction extends GenericCrudAction<TarifKhusus> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private MyTextbox nama;
    private MyDatebox mulai;
    private MyDatebox sampai;
    private AmbilDataDokterBanbox dokter;
    private AmbilDataAsuransiBanbox asuransi;
    private Combobox komunitas;
    private AmbilDataPasienBanbox pasien;
    private Checkbox aktif;
    private MyTextbox keterangan;

    // ======================== Abstract implementations ========================

    /** Kelas entitas yang dikelola: {@link TarifKhusus}. */
    @Override
    protected Class<TarifKhusus> getEntityClass() { return TarifKhusus.class; }

    /** Membuat instance {@link TarifKhusus} kosong untuk form tambah data baru. */
    @Override
    protected TarifKhusus createNewEntity() { return new TarifKhusus(); }

    /** Judul jendela: {@code "Pendataan Tarif Khusus"}. */
    @Override
    protected String getWindowTitle() { return "Pendataan Tarif Khusus"; }

    /** Menyusun kriteria pencarian {@link TarifKhusus}, difilter ilike berdasarkan nama, terurut nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(TarifKhusus.class)
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (order) criteria.addOrder(Order.asc("nama"));
        return criteria;
    }

    /** Penyedia renderer baris grid hasil pencarian: {@link TarifKhususRenderer}. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new TarifKhususRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah {@link TarifKhusus}: nama, rentang tanggal berlaku, dokter/asuransi/komunitas/pasien (kriteria berlakunya tarif), status aktif, dan keterangan, beserta tombol Batal dan Simpan. */
    @Override
    protected void buildFormContent(MyWindow window, final TarifKhusus tarifKhusus) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        // Card
        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);

        // Header

        // Plain Grid
        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        nama = new MyTextbox(tarifKhusus.getNama() == null ? "" : tarifKhusus.getNama());
        nama.setWidth("100%");
        nama.setRows(2);
        fb.addRow("Nama Tarif", nama);

        mulai = new MyDatebox(tarifKhusus.getMulai());
        mulai.setWidth("100%");
        fb.addRow("Tarif berlaku mulai", mulai);

        sampai = new MyDatebox(tarifKhusus.getSampai());
        sampai.setWidth("100%");
        fb.addRow("Tarif berlaku sampai", sampai);

        dokter = new AmbilDataDokterBanbox();
        dokter.setAttribute("dokter", tarifKhusus.getDokter());
        dokter.setValue(tarifKhusus.getDokter() == null ? ""
                : tarifKhusus.getDokter().getKode() + " - " + tarifKhusus.getDokter().getNama());
        dokter.setWidth("100%");
        fb.addRow("Dokter", dokter);

        asuransi = new AmbilDataAsuransiBanbox();
        asuransi.setValue(tarifKhusus.getAsuransi() == null ? "" : tarifKhusus.getAsuransi().getNama());
        asuransi.setAttribute("asuransi", tarifKhusus.getAsuransi());
        asuransi.setWidth("100%");
        fb.addRow("Asuransi", asuransi);

        komunitas = new Combobox();
        Common.insertCombo(komunitas, "nama", "keterangan", Komunitas.class);
        Common.selectComboItem(komunitas, tarifKhusus.getKomunitas());
        komunitas.setWidth("100%");
        fb.addRow("Komunitas", komunitas);

        pasien = new AmbilDataPasienBanbox();
        pasien.setValue(tarifKhusus.getPasien() == null ? "" : tarifKhusus.getPasien().getNama());
        pasien.setAttribute("pasien", tarifKhusus.getPasien());
        pasien.setWidth("100%");
        fb.addRow("Pasien", pasien);

        aktif = new Checkbox();
        aktif.setChecked(tarifKhusus.getAktif());
        fb.addRow("Aktif (berlaku)", aktif);

        keterangan = new MyTextbox(tarifKhusus.getKeterangan() == null ? "" : tarifKhusus.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        // South
        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);

        org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan");
        save.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        save.setParent(toolbar);
        borderlayout.setParent(window);
    }

    // ======================== Save logic ========================

    /**
     * Memvalidasi dan menyimpan data {@link TarifKhusus}: menolak bila nama kosong, tanggal mulai
     * berlaku belum diisi, atau nama sudah dipakai tarif khusus lain (dicek via
     * {@link #checkNamaTarifKhusus()}), lalu menyimpan/memperbarui entitas.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Tarif wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Tarif pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (mulai.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, tanggal Mulai Berlaku Tarif wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal mulai berlaku pada kolom yang tersedia; (2) pastikan kolom tanggal tidak dikosongkan; (3) simpan kembali data setelah tanggal terisi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaTarifKhusus()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Tarif yang Bapak/Ibu masukkan sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) gunakan nama tarif yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        TarifKhusus entity = currentEntity;
        if (entity.getId() != null) {
            entity = (TarifKhusus) session.load(TarifKhusus.class, entity.getId());
            currentEntity = entity;
        }
        entity.setMulai(mulai.getValue());
        entity.setSampai(sampai.getValue());
        entity.setAktif(aktif.isChecked());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setDokter((Dokter) dokter.getAttribute("dokter"));
        entity.setAsuransi((Asuransi) asuransi.getAttribute("asuransi"));
        entity.setPasien((Pasien) pasien.getAttribute("pasien"));
        entity.setKomunitas(komunitas.getSelectedItem() == null ? null : (Komunitas) komunitas.getSelectedItem().getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** Memeriksa apakah nama pada form sudah dipakai {@link TarifKhusus} lain (mengecualikan entitas yang sedang diedit). */
    public Boolean checkNamaTarifKhusus() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(TarifKhusus.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid untuk {@link TarifKhusus}: nama (dengan tombol riwayat revisi), rentang tanggal berlaku, status aktif, dokter/asuransi/komunitas/pasien, keterangan, tombol edit/hapus, dan panel detail (dibuka via {@link MyDetail}) berisi 4 tab rincian tarif per kategori. */
    class TarifKhususRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final TarifKhusus tarifKhusus = (TarifKhusus) arg1;

            final MyDetail detail = new MyDetail();
            detail.setParent(arg0);
            detail.addEventListener(Events.ON_OPEN, new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    Common.clear(detail);
                    if (detail.isOpen()) {
                        Tabbox tabbox = new Tabbox();
                        tabbox.setParent(detail);
                        tabbox.setHeight("100%");
                        tabbox.setWidth("100%");

                        Tabs tabs = new Tabs();
                        tabs.setParent(tabbox);

                        new Tab("Tarif Tindakan dan Perawatan").setParent(tabs);
                        new Tab("Tarif Alat Medis dan Kesehatan").setParent(tabs);
                        new Tab("Tarif Obat-Obatan").setParent(tabs);
                        new Tab("Tarif Bed").setParent(tabs);

                        Tabpanels tabpanels = new Tabpanels();
                        tabpanels.setParent(tabbox);

                        TarifKhususPunyaTindakanDetailAction tindakanDetail =
                                new TarifKhususPunyaTindakanDetailAction(tarifKhusus);
                        tindakanDetail.setParent(tabpanels);
                        tindakanDetail.display();

                        TarifKhususPunyaAlatMedisDetailAction alatMedisDetail =
                                new TarifKhususPunyaAlatMedisDetailAction(tarifKhusus, AlatMedis.JENIS_UMUM);
                        alatMedisDetail.setParent(tabpanels);
                        alatMedisDetail.display();

                        TarifKhususPunyaItemDetailAction itemDetail =
                                new TarifKhususPunyaItemDetailAction(tarifKhusus);
                        itemDetail.setParent(tabpanels);
                        itemDetail.display();

                        TarifKhususPunyaAlatMedisDetailAction bedDetail =
                                new TarifKhususPunyaAlatMedisDetailAction(tarifKhusus, AlatMedis.JENIS_TEMPAT_TIDUR);
                        bedDetail.setParent(tabpanels);
                        bedDetail.display();
                    }
                }
            });

            RevisiHelper.createNewRevisi(TarifKhusus.class, tarifKhusus, tarifKhusus.getNama()).setParent(arg0);
            new Label(tarifKhusus.getMulai() == null ? "" : Common.dateFormat4.get().format(tarifKhusus.getMulai())).setParent(arg0);
            new Label(tarifKhusus.getSampai() == null ? "" : Common.dateFormat4.get().format(tarifKhusus.getSampai())).setParent(arg0);
            new Label(tarifKhusus.getAktif() ? "Ya" : "Tidak").setParent(arg0);
            new Label(tarifKhusus.getDokter() == null ? "" : tarifKhusus.getDokter().toString()).setParent(arg0);
            new Label(tarifKhusus.getAsuransi() == null ? "" : tarifKhusus.getAsuransi().toString()).setParent(arg0);
            new Label(tarifKhusus.getKomunitas() == null ? "" : tarifKhusus.getKomunitas().toString()).setParent(arg0);
            new Label(tarifKhusus.getPasien() == null ? "" : tarifKhusus.getPasien().toString()).setParent(arg0);
            new Label(tarifKhusus.getKeterangan()).setParent(arg0);

            Common.copyEditDeleteButtons(edit, delete, tarifKhusus, TarifKhususAction.this).setParent(arg0);
        }
    }
}
