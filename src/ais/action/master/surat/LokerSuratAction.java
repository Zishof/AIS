package ais.action.master.surat;

import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.ui.util.FormBuilder;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.surat.LokerSuratDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.LokerSurat;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD master data Loker Surat (lokasi fisik penyimpanan arsip surat — boks, rak, lemari,
 * lantai, ruang) pada modul persuratan, dibangun di atas {@link GenericCrudAction}. Loker
 * dikaitkan dengan satuan kerja pemiliknya dan dapat difilter berdasarkan hierarki satuan kerja
 * (termasuk anak-anaknya, lewat {@link SatuanKerjaTreeModel}).
 *
 * <p>
 * Kelas ini dipakai untuk dua konteks berbeda ("surat masuk" dan "surat keluar") lewat parameter
 * URL {@code tipe} (default {@code "surat"}), dibaca di {@link #onAfterInit(Component)} — baris
 * data lama yang belum memiliki {@code tipe} otomatis diisi mengikuti konteks saat ini di
 * {@link LokerSuratRenderer}. Pencarian daftar mendukung filter status aktif, tipe, satuan kerja
 * (beserta turunannya) dan kecocokan sebagian nama. Form simpan (menggunakan
 * {@link LokerSuratDao}, bukan sesi Hibernate langsung) memvalidasi nama wajib isi dan tidak
 * duplikat, serta otomatis mem-prapopulasi satuan kerja dari satuan kerja pengguna yang login
 * saat menambah data baru. Mendukung cetak/unduh dan unggah data massal lewat toolbar tambahan.
 * </p>
 */
public class LokerSuratAction extends GenericCrudAction<LokerSurat> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Extra search field (auto-wired from ZUL)
    private AmbilDataSatuanKerjaBanbox searchparent;

    // URL-parameter-driven tipe filter
    private String tipe = "surat";
    private SatuanKerjaTreeModel satuanKerjaTreeModel;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private Textbox boks;
    private Textbox rak;
    private Textbox lemari;
    private Textbox lantai;
    private AmbilDataRuangBanbox ruang;
    private AmbilDataSatuanKerjaBanbox satuanKerja;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<LokerSurat> getEntityClass() { return LokerSurat.class; }

    @Override
    protected LokerSurat createNewEntity() { return new LokerSurat(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Loker Surat"; }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "boks", "lantai", "lemari", "rak", "gedung", "ruang",
                "tipe", "keterangan", "aktif", "satuanKerja" };
    }

    /** Memasang listener pemilihan satuan kerja induk pada pencarian, menyiapkan model hierarki satuan kerja, membaca parameter URL {@code tipe}, dan menambahkan tombol cetak/unggah data massal ke toolbar. */
    @Override
    protected void onAfterInit(Component comp) throws Exception {
        searchparent.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(arg0);
            }
        });
        satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        if (execution.getParameter("tipe") != null && !execution.getParameter("tipe").trim().isEmpty()) {
            tipe = execution.getParameter("tipe").trim();
        }

        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(LokerSurat.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, LokerSurat.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    /**
     * Membangun kriteria pencarian daftar loker surat: difilter berdasarkan status aktif (bila
     * dicentang), tipe ({@code "surat"} atau {@code "surat_keluar"} sesuai konteks), satuan
     * kerja terpilih beserta seluruh turunannya (atau satuan kerja yang dapat diakses pengguna
     * bila tidak ada pilihan eksplisit), dan kecocokan sebagian nama.
     */
    @Override
    public Criteria initCriteria(boolean order) {
        SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
        Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
        if (parent != null) {
            satuanKerjas.clear();
            satuanKerjas.add(parent);
            satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
        }
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(LokerSurat.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
                .add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.or(
                                parent == null ? Restrictions.isNull("satuanKerja")
                                        : Restrictions.sqlRestriction("false"),
                                Restrictions.in("satuanKerja", satuanKerjas)));
        if (order) criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new LokerSuratRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah (kode, nama, boks/rak/lemari/lantai, ruang, satuan kerja, keterangan) beserta toolbar Batal/Simpan pada {@code window}; satuan kerja diisi otomatis dari pengguna yang login untuk data baru. */
    @Override
    protected void buildFormContent(MyWindow window, final LokerSurat lokerSurat) throws Exception {
        // Pre-populate satuanKerja dari user yang sedang login jika belum diisi
        if (lokerSurat.getSatuanKerja() == null) {
            ais.database.model.Tbmuser tbmuser = Common.getCurrentUser();
            if (tbmuser != null && tbmuser.ambilSatuanKerja() != null) {
                lokerSurat.setSatuanKerja(tbmuser.ambilSatuanKerja());
            }
        }

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

        kode = new Textbox(lokerSurat.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Loker", kode);

        nama = new Textbox(lokerSurat.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Loker", nama);

        boks = new Textbox(lokerSurat.getBoks());
        boks.setWidth("100%");
        fb.addRow("Boks No.", boks);

        rak = new Textbox(lokerSurat.getRak());
        rak.setWidth("100%");
        fb.addRow("Rak No.", rak);

        lemari = new Textbox(lokerSurat.getLemari());
        lemari.setWidth("100%");
        fb.addRow("Almari/Lemari", lemari);

        lantai = new Textbox(lokerSurat.getLantai());
        lantai.setWidth("100%");
        fb.addRow("Lantai", lantai);

        ruang = new AmbilDataRuangBanbox();
        ruang.setAttribute("ruang", lokerSurat.getRuang());
        ruang.setValue(lokerSurat.getRuang() == null ? "" : lokerSurat.getRuang().getNama());
        ruang.setWidth("100%");
        fb.addRow("Ruang", ruang);

        satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
        satuanKerja.setValue(lokerSurat.getSatuanKerja() == null ? "" : lokerSurat.getSatuanKerja().getNama());
        satuanKerja.setAttribute("satuanKerja", lokerSurat.getSatuanKerja());
        satuanKerja.setWidth("100%");
        fb.addRow("Satuan Kerja", satuanKerja);

        keterangan = new Textbox(lokerSurat.getKeterangan());
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
     * Memvalidasi (nama wajib isi, nama tidak duplikat) dan menyimpan (create-or-update, lewat
     * {@link LokerSuratDao}) entitas loker surat dari isian form, dengan {@code tipe} yang
     * berlaku pada layar ini disimpan ke entitas.
     *
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan
     *         peringatan sudah ditampilkan ke pengguna)
     */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Loker Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Loker; (2) isikan nama loker surat secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkNamaLokerSurat()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Loker Surat sudah ada di database. Langkah yang dapat dilakukan: (1) periksa daftar loker surat yang sudah ada; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        LokerSuratDao lokerSuratDao = DaoFactory.getInstance().getLokerSuratDao();
        LokerSurat entity = currentEntity;
        if (entity.getId() != null) {
            entity = lokerSuratDao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setBoks(boks.getValue());
        entity.setRak(rak.getValue());
        entity.setLemari(lemari.getValue());
        entity.setLantai(lantai.getValue());
        entity.setRuang((Ruang) ruang.getAttribute("ruang"));
        entity.setKeterangan(keterangan.getValue());
        entity.setTipe(tipe);
        entity.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
        if (entity.getId() != null) {
            lokerSuratDao.update(entity);
        } else {
            lokerSuratDao.save(entity);
        }
        return true;
    }

    /** Mengecek apakah nama pada form sudah dipakai loker surat lain (di luar entitas yang sedang diedit). */
    public Boolean checkNamaLokerSurat() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(LokerSurat.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Perenderan satu baris tabel loker surat: kode, nama (dengan tautan riwayat revisi), lokasi fisik (boks/rak/lemari/lantai/ruang/gedung), satuan kerja, keterangan, checkbox status aktif, dan tombol edit/hapus. Mengisi otomatis {@code tipe} yang kosong pada data lama sesuai konteks layar saat ini. */
    class LokerSuratRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final LokerSurat lokerSurat = (LokerSurat) arg1;

            // Pastikan tipe terisi jika belum ada (migrasi data lama)
            if (lokerSurat.getTipe() == null) {
                lokerSurat.setTipe(tipe);
                Common.refreshUpdate(lokerSurat);
            }

            new Label(lokerSurat.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(LokerSurat.class, lokerSurat, lokerSurat.getNama()).setParent(arg0);
            new Label(lokerSurat.getBoks()).setParent(arg0);
            new Label(lokerSurat.getRak()).setParent(arg0);
            new Label(lokerSurat.getLemari()).setParent(arg0);
            new Label(lokerSurat.getLantai()).setParent(arg0);
            new Label(lokerSurat.getRuang() == null ? "" : lokerSurat.getRuang().getNama()).setParent(arg0);
            new Label(lokerSurat.getGedung() == null ? "" : lokerSurat.getGedung().getNama()).setParent(arg0);
            new Label(lokerSurat.getSatuanKerja() == null ? "" : lokerSurat.getSatuanKerja().getNama()).setParent(arg0);
            new Label(lokerSurat.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(lokerSurat.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    lokerSurat.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(lokerSurat);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, lokerSurat, LokerSuratAction.this).setParent(arg0);
        }
    }
}
