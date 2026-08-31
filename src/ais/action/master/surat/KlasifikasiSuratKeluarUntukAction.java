package ais.action.master.surat;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.ui.util.FormBuilder;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.surat.KlasifikasiSuratKeluarUntukDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.surat.KlasifikasiSuratKeluarUntuk;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data "Klasifikasi Surat Keluar Untuk" (peruntukan/aturan penerbitan surat
 * keluar bagi mahasiswa pada modul persuratan, mis. surat keterangan aktif kuliah). Setiap baris
 * mendefinisikan syarat penerbitan surat berdasarkan status awal dan status mahasiswa saat ini
 * ({@code statusAwalMahasiswa}/{@code statusMahasiswa}, kosong berarti berlaku untuk semua status),
 * beserta tiga aturan tambahan: tanggal surat tidak bisa diubah, mahasiswa hanya bisa cetak sekali,
 * dan mahasiswa harus sudah membayar sebelum bisa mencetak. Memperluas {@link GenericCrudAction}
 * untuk mewarisi kerangka baku cari/tambah/ubah/hapus; penyimpanan dan penghapusan didelegasikan ke
 * {@link KlasifikasiSuratKeluarUntukDao} (via {@link DaoFactory}). Renderer baris kelas ini
 * membangun toolbar edit/hapus sendiri (berbeda dari pola {@code Common.copyEditDeleteButtons} yang
 * dipakai sebagian besar layar CRUD sejenis), termasuk penanganan galat khusus saat penghapusan
 * gagal karena data masih berelasi dengan data lain.
 */
public class KlasifikasiSuratKeluarUntukAction extends GenericCrudAction<KlasifikasiSuratKeluarUntuk> {

    private static final long serialVersionUID = 1L;

    // Form fields
    private Textbox nama;
    private Textbox keterangan;
    private Combobox statusAwalMahasiswa;
    private Combobox statusMahasiswa;
    private MyCheckboxConfig tanggalSuratTidakBisaDiubah;
    private MyCheckboxConfig mahasiswaHanyaBisaCetakSuratSekali;
    private MyCheckboxConfig mahasiswaHarusTelahMembayar;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<KlasifikasiSuratKeluarUntuk> getEntityClass() { return KlasifikasiSuratKeluarUntuk.class; }

    @Override
    protected KlasifikasiSuratKeluarUntuk createNewEntity() { return new KlasifikasiSuratKeluarUntuk(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Klasifikasi Surat Keluar"; }

    /** Menyusun kriteria pencarian {@link KlasifikasiSuratKeluarUntuk} berdasarkan nama (filter {@code searchnama}), diurutkan berdasarkan nama bila diminta. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(KlasifikasiSuratKeluarUntuk.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** Menyediakan renderer baris grid {@link KlasifikasiSuratKeluarUntukRenderer} untuk daftar hasil pencarian. */
    @Override
    protected MyRowRenderer createRenderer() {
        return new KlasifikasiSuratKeluarUntukRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah klasifikasi surat keluar (nama peruntukan, status awal/status mahasiswa, tiga checkbox aturan, keterangan) beserta tombol batal/simpan pada jendela dialog. */
    @Override
    protected void buildFormContent(MyWindow window, final KlasifikasiSuratKeluarUntuk entity) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center with card
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        nama = new Textbox(entity.getNama() == null ? "" : entity.getNama());
        nama.setWidth("100%");
        fb.addRow("Diperuntukkan untuk", nama);

        statusAwalMahasiswa = new Combobox();
        Common.insertComboDanSemua(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class);
        Common.selectComboItem(statusAwalMahasiswa, entity.getStatusAwalMahasiswa());
        statusAwalMahasiswa.setWidth("100%");
        statusAwalMahasiswa.setReadonly(true);
        fb.addRow("Status Awal Mahasiswa", statusAwalMahasiswa,
                "Pilih semua jika untuk semua status awal mahasiswa atau tidak digunakan");

        statusMahasiswa = new Combobox();
        Common.insertComboDanSemua(statusMahasiswa, "nama", StatusMahasiswa.class);
        Common.selectComboItem(statusMahasiswa, entity.getStatusMahasiswa());
        statusMahasiswa.setWidth("100%");
        statusMahasiswa.setReadonly(true);
        fb.addRow("Status Mahasiswa", statusMahasiswa,
                "Pilih semua jika untuk semua status mahasiswa atau tidak digunakan");

        tanggalSuratTidakBisaDiubah = new MyCheckboxConfig("Tanggal Surat Tidak Bisa Diubah");
        tanggalSuratTidakBisaDiubah.setChecked(entity.getTanggalSuratTidakBisaDiubah());
        fb.addRow("", tanggalSuratTidakBisaDiubah);

        mahasiswaHanyaBisaCetakSuratSekali = new MyCheckboxConfig("Mahasiswa Hanya Bisa Cetak Surat Sekali");
        mahasiswaHanyaBisaCetakSuratSekali.setChecked(entity.getMahasiswaHanyaBisaCetakSuratSekali());
        fb.addRow("", mahasiswaHanyaBisaCetakSuratSekali);

        mahasiswaHarusTelahMembayar = new MyCheckboxConfig("Mahasiswa Harus Telah Membayar");
        mahasiswaHarusTelahMembayar.setChecked(entity.getMahasiswaHarusTelahMembayar());
        fb.addRow("", mahasiswaHarusTelahMembayar);

        keterangan = new Textbox(entity.getKeterangan() == null ? "" : entity.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        south.setParent(borderlayout);

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
     * Memvalidasi lalu menyimpan data klasifikasi surat keluar dari form: menolak bila nama
     * peruntukan kosong atau sudah terdaftar pada baris lain; jika lolos menyimpan/memperbarui
     * entitas lewat {@link KlasifikasiSuratKeluarUntukDao} dan mengembalikan {@code true}.
     *
     * @param event event ZK pemicu penyimpanan (tombol simpan)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     * @throws Exception diteruskan apa adanya dari kegagalan DAO saat menyimpan
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Peruntukan Klasifikasi Surat Keluar belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Peruntukan; (2) isikan nama peruntukan secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaKlasifikasiSuratKeluarUntuk()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Peruntukan Klasifikasi Surat Keluar sudah ada di database. Langkah yang dapat dilakukan: (1) periksa daftar peruntukan yang sudah ada; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        KlasifikasiSuratKeluarUntukDao dao = DaoFactory.getInstance().getKlasifikasiSuratKeluarUntukDao();
        KlasifikasiSuratKeluarUntuk entity = currentEntity;
        if (entity.getId() != null) {
            entity = dao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setMahasiswaHarusTelahMembayar(mahasiswaHarusTelahMembayar.isChecked());
        entity.setMahasiswaHanyaBisaCetakSuratSekali(mahasiswaHanyaBisaCetakSuratSekali.isChecked());
        entity.setTanggalSuratTidakBisaDiubah(tanggalSuratTidakBisaDiubah.isChecked());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
                : statusAwalMahasiswa.getSelectedItem().getValue()));
        entity.setStatusMahasiswa((StatusMahasiswa) (statusMahasiswa.getSelectedItem() == null ? null
                : statusMahasiswa.getSelectedItem().getValue()));
        if (entity.getId() != null) {
            dao.update(entity);
        } else {
            dao.save(entity);
        }
        return true;
    }

    /**
     * Memeriksa apakah nama peruntukan yang diisi di form sudah dipakai baris lain (mengecualikan
     * baris yang sedang diedit sendiri).
     *
     * @return {@code true} bila nama sudah terpakai baris lain, {@code false} bila belum
     */
    public Boolean checkNamaKlasifikasiSuratKeluarUntuk() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(KlasifikasiSuratKeluarUntuk.class)
                .setProjection(Projections.rowCount())
                .add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid daftar klasifikasi surat keluar: kolom nama (dengan link riwayat revisi), status awal/status mahasiswa, keterangan, dan toolbar edit/hapus khusus (hapus menampilkan pesan galat rinci bila entitas masih berelasi dengan data lain). */
    class KlasifikasiSuratKeluarUntukRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final KlasifikasiSuratKeluarUntuk entity = (KlasifikasiSuratKeluarUntuk) arg1;

            RevisiHelper.createNewRevisi(KlasifikasiSuratKeluarUntuk.class, entity, entity.getNama()).setParent(arg0);
            new Label(entity.getStatusAwalMahasiswa() == null ? "-"
                    : entity.getStatusAwalMahasiswa().getNama()).setParent(arg0);
            new Label(entity.getStatusMahasiswa() == null ? "-"
                    : entity.getStatusMahasiswa().getNama()).setParent(arg0);
            new Label(entity.getKeterangan()).setParent(arg0);

            Hbox toolbar = new Hbox();
            MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
            button.setTooltiptext("Ubah Data");
            button.setVisible(edit);
            button.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    openForm(entity);
                }
            });
            button.setParent(toolbar);

            button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
            button.setTooltiptext("Hapus Data");
            button.setVisible(delete);
            button.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
                            new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = Integer.parseInt(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            KlasifikasiSuratKeluarUntukDao dao = DaoFactory.getInstance()
                                                    .getKlasifikasiSuratKeluarUntukDao();
                                            dao.delete(dao.merge(entity));
                                            onSearchDefault(event);
                                        } catch (Exception e) {
                                            Common.tampilErrorJikaAdmin(e);
                                            MyMessageboxConfig.show(
                                                    "Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
                                                            + e.getMessage());
                                        }
                                    }
                                }
                            });
                }
            });
            button.setParent(toolbar);
            ais.ui.util.MenuAksiBaris.pasang(toolbar);
            toolbar.setParent(arg0);
        }
    }
}
