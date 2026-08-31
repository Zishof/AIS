package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataLokasiBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Shift;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD modul SIRS untuk {@link JadwalDokter} (jadwal praktik dokter/tenaga medis per lokasi,
 * shift, hari, dan poli), dibangun di atas kerangka generik {@link GenericCrudAction}. Combo shift
 * pada form maupun filter pencarian bersifat cascading terhadap lokasi terpilih (hanya menampilkan
 * {@link Shift} milik lokasi tersebut), dan memilih shift otomatis menampilkan label jam
 * mulai/selesainya.
 *
 * <p>
 * Selain ubah/hapus standar, grid menyediakan tombol "Copy Jadwal" yang mengkloning
 * {@link JadwalDokter} ({@code clone()}, id direset ke {@code null}) dan membuka form sebagai entri
 * baru — mempercepat pembuatan jadwal serupa untuk dokter/hari lain.
 * </p>
 */
public class JadwalDokterAction extends GenericCrudAction<JadwalDokter> {

    private static final long serialVersionUID = 3786091220301468178L;

    // Extra search fields (auto-wired from ZUL)
    private Combobox searchhari;
    private AmbilDataLokasiBanbox searchlokasi;
    private Combobox searchshift;
    private AmbilDataDokterBanbox searchdokter;
    private Combobox searchpoly;

    // Form fields
    private Combobox shift;
    private Combobox poly;
    private AmbilDataDokterBanbox dokter;
    private Combobox hari;
    private AmbilDataLokasiBanbox lokasi;
    private MyTextbox keterangan;
    private Datebox jadwalDokterDimulai;
    private Datebox jadwalDokterSampai;
    private Label waktuMulai;
    private Label waktuSelesai;

    // ======================== Abstract implementations ========================

    /** @return kelas entitas yang dikelola layar ini, {@link JadwalDokter}. */
    @Override
    protected Class<JadwalDokter> getEntityClass() { return JadwalDokter.class; }

    /** @return instance {@link JadwalDokter} kosong untuk form tambah baru. */
    @Override
    protected JadwalDokter createNewEntity() { return new JadwalDokter(); }

    /** @return judul jendela form tambah/ubah. */
    @Override
    protected String getWindowTitle() { return "Jadwal Dokter"; }

    /** Inisialisasi layar: mengisi combo hari pencarian, memasang listener refresh pada filter dokter/lokasi (lokasi juga memuat ulang shift cascading), dan combo poli. */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        for (String h : Common.haris) {
            Comboitem comboitem = new Comboitem();
            comboitem.setLabel(h);
            comboitem.setValue(h);
            searchhari.appendChild(comboitem);
        }

        searchdokter.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(arg0);
            }
        });

        searchlokasi.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                searchshift.setSelectedItem(null);
                Common.clear(searchshift);
                if (searchlokasi.getAttribute("lokasi") != null) {
                    Common.insertCombo(searchshift, "nama", "keteranganLabel", Shift.class,
                            Restrictions.eq("lokasi", searchlokasi.getAttribute("lokasi")));
                }
                onSearchDefault(arg0);
            }
        });

        Common.insertCombo(searchpoly, "nama", "jenis", Poly.class);
    }

    /** Membentuk criteria pencarian {@link JadwalDokter} berdasarkan filter lokasi, dokter, shift, hari, dan poli, diurut id menurun bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JadwalDokter.class)
                .add(searchlokasi == null || searchlokasi.getAttribute("lokasi") == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("lokasi", searchlokasi.getAttribute("lokasi")))
                .add(searchdokter == null || searchdokter.getAttribute("dokter") == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("dokter", searchdokter.getAttribute("dokter")))
                .add(searchshift == null || searchshift.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("shift", searchshift.getSelectedItem().getValue()))
                .add(searchhari == null || searchhari.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))
                .add(searchpoly == null || searchpoly.getSelectedItem() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("poly", searchpoly.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    /** @return renderer baris grid untuk {@link JadwalDokter} ({@link JadwalDokterRenderer}). */
    @Override
    protected MyRowRenderer createRenderer() {
        return new JadwalDokterRenderer();
    }

    // ======================== Form content ========================

    /**
     * Membangun form tambah/ubah {@link JadwalDokter}: lokasi (memicu pemuatan ulang combo shift
     * cascading), shift (mengisi label waktu mulai/selesai otomatis), tenaga medis, poli, hari,
     * rentang tanggal berlaku, dan keterangan, plus toolbar Batal/Simpan.
     */
    @Override
    protected void buildFormContent(MyWindow window, final JadwalDokter jadwalDokter) throws Exception {
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

        lokasi = new AmbilDataLokasiBanbox();
        lokasi.setValue(jadwalDokter.getLokasi() == null ? "" : jadwalDokter.getLokasi().toString());
        lokasi.setAttribute("lokasi", jadwalDokter.getLokasi());
        lokasi.setWidth("100%");
        fb.addRow("Lokasi", lokasi);

        shift = new Combobox();
        Common.insertCombo(shift, "nama", "keteranganLabel", Shift.class);
        Common.selectComboItem(shift, jadwalDokter.getShift());
        shift.setWidth("100%");
        fb.addRow("Shift", shift);

        dokter = new AmbilDataDokterBanbox();
        dokter.setValue(jadwalDokter.getDokter() == null ? "" : jadwalDokter.getDokter().getNama());
        dokter.setAttribute("dokter", jadwalDokter.getDokter());
        dokter.setWidth("100%");
        fb.addRow("Tenaga Medis", dokter);

        poly = new Combobox();
        Common.insertCombo(poly, "nama", "jenis", Poly.class);
        Common.selectComboItem(poly, jadwalDokter.getPoly());
        poly.setWidth("100%");
        fb.addRow("Poly", poly);

        waktuMulai = new Label(
                jadwalDokter.getShift() == null ? "" : Common.timeFormat.get().format(jadwalDokter.getShift().getMulai()));
        fb.addRow("Waktu Mulai", waktuMulai);

        waktuSelesai = new Label(
                jadwalDokter.getShift() == null ? "" : Common.timeFormat.get().format(jadwalDokter.getShift().getSampai()));
        fb.addRow("Waktu Selesai", waktuSelesai);

        final EventListener shiftEventListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Shift myShift = (Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue());
                if (myShift != null) {
                    waktuMulai.setValue(Common.timeFormat.get().format(myShift.getMulai()));
                    waktuSelesai.setValue(Common.timeFormat.get().format(myShift.getSampai()));
                }
            }
        };

        EventListener lokasiEventListener = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                shift.setSelectedItem(null);
                Common.clear(shift);
                if (lokasi.getAttribute("lokasi") != null) {
                    Common.insertCombo(shift, "nama", "keteranganLabel", Shift.class,
                            Restrictions.eq("lokasi", lokasi.getAttribute("lokasi")));
                    Common.selectComboItem(shift, jadwalDokter.getShift());
                }
                shiftEventListener.onEvent(null);
            }
        };

        shift.addEventListener("onChange", shiftEventListener);
        lokasi.setEventListener(lokasiEventListener);
        lokasiEventListener.onEvent(null);

        hari = new Combobox();
        for (String h : Common.haris) {
            Comboitem comboitem = new Comboitem();
            comboitem.setLabel(h);
            comboitem.setValue(h);
            hari.appendChild(comboitem);
        }
        Common.selectComboItem(hari, jadwalDokter.getHari() == null ? "" : jadwalDokter.getHari());
        hari.setWidth("100%");
        fb.addRow("Hari", hari);

        Hbox dateHbox = new Hbox();
        dateHbox.appendChild(jadwalDokterDimulai = new Datebox(jadwalDokter.getJadwalDokterDimulai()));
        dateHbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
        dateHbox.appendChild(jadwalDokterSampai = new Datebox(jadwalDokter.getJadwalDokterSampai()));
        jadwalDokterDimulai.setFormat(Common.dateFormat2.get().toPattern());
        jadwalDokterSampai.setFormat(Common.dateFormat2.get().toPattern());
        fb.addRow("Berlaku mulai", dateHbox);

        keterangan = new MyTextbox(jadwalDokter.getKeterangan() == null ? "" : jadwalDokter.getKeterangan());
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

    /** Memvalidasi (shift, tenaga medis, hari, poli, lokasi wajib terisi) dan menyimpan {@link JadwalDokter} dari nilai form saat ini. @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal. */
    public boolean onSave(Event event) throws Exception {
        if (shift.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Shift wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu Lokasi agar daftar Shift tersedia; (2) pilih Shift pada daftar yang tersedia; (3) simpan kembali data setelah Shift ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (dokter.getAttribute("dokter") == null) {
            MyMessageboxConfig.show("Mohon maaf, Tenaga Medis wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Tenaga Medis; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Tenaga Medis ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (hari.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Hari wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Hari pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Hari ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (poly.getSelectedItem() == null) {
            MyMessageboxConfig.show("Mohon maaf, Poli wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Poli pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Poli ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (lokasi.getAttribute("lokasi") == null) {
            MyMessageboxConfig.show("Mohon maaf, Lokasi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Lokasi; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Lokasi ditentukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        JadwalDokter entity = currentEntity;
        if (entity.getId() != null) {
            entity = (JadwalDokter) session.load(JadwalDokter.class, entity.getId());
            currentEntity = entity;
        }
        entity.setPoly((Poly) poly.getSelectedItem().getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setJadwalDokterDimulai(jadwalDokterDimulai.getValue());
        entity.setJadwalDokterSampai(jadwalDokterSampai.getValue());
        entity.setHari(hari.getSelectedItem() == null ? null : hari.getSelectedItem().getValue().toString());
        entity.setDokter((Dokter) dokter.getAttribute("dokter"));
        entity.setShift((Shift) shift.getSelectedItem().getValue());
        entity.setLokasi((Lokasi) lokasi.getAttribute("lokasi"));
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid {@link JadwalDokter}: hari + rentang tanggal berlaku, lokasi, shift (dengan detail revisi), tenaga medis, poli, keterangan, dan tombol aksi ubah/copy/hapus. */
    class JadwalDokterRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final JadwalDokter jadwalDokter = (JadwalDokter) arg1;

            new Html((jadwalDokter.getHari() == null ? "" : jadwalDokter.getHari() + "<br>")
                    + (jadwalDokter.getJadwalDokterDimulai() == null ? ""
                            : " " + Common.dateFormat2.get().format(jadwalDokter.getJadwalDokterDimulai()))
                    + (jadwalDokter.getJadwalDokterSampai() == null ? ""
                            : " s.d " + Common.dateFormat2.get().format(jadwalDokter.getJadwalDokterSampai())))
                    .setParent(arg0);

            new Label(jadwalDokter.getLokasi() == null ? "" : jadwalDokter.getLokasi().getNama()).setParent(arg0);
            RevisiHelper.createNewRevisi(JadwalDokter.class, jadwalDokter, jadwalDokter.getShift().toString()).setParent(arg0);
            new Label(jadwalDokter.getDokter().toString()).setParent(arg0);
            new Label(jadwalDokter.getPoly() == null ? "" : jadwalDokter.getPoly().getNama()).setParent(arg0);
            new Label(jadwalDokter.getKeterangan()).setParent(arg0);

            Hbox toolbar = new Hbox();

            Toolbarbutton btnEdit = new MyToolbarbuttonConfig("", "/img/edit.gif");
            btnEdit.setTooltiptext("Ubah Data");
            btnEdit.setVisible(edit);
            btnEdit.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    currentEntity = jadwalDokter;
                    buildFormContent(addWindow, jadwalDokter);
                    addWindow.setVisible(true);
                    addWindow.onModal();
                }
            });
            btnEdit.setParent(toolbar);

            Toolbarbutton btnCopy = new MyToolbarbuttonConfig("", "/img/copy.png");
            btnCopy.setTooltiptext("Copy Jadwal");
            btnCopy.setVisible(edit);
            btnCopy.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    JadwalDokter jadwalDokterCopy = (JadwalDokter) jadwalDokter.clone();
                    jadwalDokterCopy.setId(null);
                    currentEntity = jadwalDokterCopy;
                    buildFormContent(addWindow, jadwalDokterCopy);
                    addWindow.setVisible(true);
                    addWindow.onModal();
                }
            });
            btnCopy.setParent(toolbar);

            Toolbarbutton btnDelete = new MyToolbarbuttonConfig("", "/img/delete.gif");
            btnDelete.setTooltiptext("Hapus Data");
            btnDelete.setVisible(delete);
            btnDelete.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data jadwal dokter ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            Common.refreshDelete(jadwalDokter);
                                            onSearchDefault(event);
                                        } catch (Exception e) {
                                            ais.common.Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(Common.pesan(
                                                    "Mohon maaf, data jadwal dokter ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
                                                    e.getMessage()));
                                        }
                                    }
                                }
                            });
                }
            });
            btnDelete.setParent(toolbar);
            ais.ui.util.MenuAksiBaris.pasang(toolbar);
            toolbar.setParent(arg0);
        }
    }
}
