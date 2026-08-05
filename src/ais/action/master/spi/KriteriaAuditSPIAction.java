package ais.action.master.spi;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.spi.JenisAuditSPI;
import ais.database.model.spi.KriteriaAuditSPI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

/**
 * <h2>KriteriaAuditSPIAction &mdash; Pengendali Layar Data Master Kriteria Audit (Level 2)</h2>
 *
 * <p>
 * Pengendali ZK untuk tab kedua layar Setup SPI, tempat staf Satuan Pengawasan Internal mendata
 * standar/kriteria acuan pemeriksaan (mis. "Kepatuhan SOP Kas Kecil", "Rekonsiliasi Bank Bulanan")
 * di bawah satu {@link JenisAuditSPI} (kategori besar seperti "Audit Keuangan"). Lihat javadoc
 * {@link KriteriaAuditSPI} untuk penjelasan lengkap tentang kedudukan level ini dalam hierarki 3
 * tingkat checklist audit SPI.
 * </p>
 *
 * <h3>Dua peran Combobox Jenis Audit yang berbeda pada layar ini</h3>
 * <p>
 * Layar ini memakai combobox "Jenis Audit" pada DUA tempat dengan tujuan berbeda: pada kartu filter
 * pencarian ({@code searchjenisAuditSPI}, dipakai {@link #initCriteria(boolean)} untuk mempersempit
 * daftar yang ditampilkan ke satu kategori saja), dan pada formulir tambah/ubah ({@code jenisAuditSPI},
 * dipakai {@link #buildForm(KriteriaAuditSPI)} untuk menentukan kriteria ini akan menjadi anak dari
 * kategori audit yang mana). Saat menambah data baru lewat tombol Tambah setelah filter kategori
 * tertentu sudah dipilih, formulir OTOMATIS mempra-isi kombo kategori dengan pilihan filter yang
 * sedang aktif ({@link #buildForm(KriteriaAuditSPI)}, baris {@code Common.selectComboItem}) &mdash;
 * mengurangi klik berulang bagi staf yang sedang fokus mendata banyak kriteria dalam satu kategori
 * yang sama secara berurutan.
 * </p>
 *
 * <h3>Kriteria wajib memiliki induk (tidak bisa yatim)</h3>
 * <p>
 * Berbeda dengan {@link JenisAuditSPIAction} yang datanya berdiri sendiri, kelas ini WAJIB memvalidasi
 * bahwa combobox Jenis Audit pada formulir sudah terisi sebelum data boleh disimpan
 * ({@link #onSave(Event)}) &mdash; sejalan dengan constraint {@code nullable = false} pada kolom
 * {@code jenis_audit_spi} di database (lihat {@link KriteriaAuditSPI#getJenisAuditSPI()}). Validasi
 * di sisi antarmuka ini memberi pesan kesalahan yang ramah pengguna SEBELUM permintaan sampai ke
 * database, alih-alih membiarkan pengguna menerima pesan kesalahan teknis dari basis data.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class KriteriaAuditSPIAction extends BaseSPIAction {

    private static final long serialVersionUID = 1L;

    // ---- Search fields ----
    private Combobox searchjenisAuditSPI;

    // ---- Form fields ----
    private MyIntbox nomorUrut;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox jenisAuditSPI;

    // ---- Current entity ----
    private KriteriaAuditSPI kriteriaAuditSPI;

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();

        Common.insertComboDanSemua(searchjenisAuditSPI, "nama", "keterangan",
                JenisAuditSPI.class, Restrictions.eq("aktif", true));

        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(KriteriaAuditSPI.class,
                new String[]{"id", "nomorUrut", "jenisAuditSPI", "nama", "keterangan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    class KriteriaAuditSPIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final KriteriaAuditSPI item = (KriteriaAuditSPI) obj;

            new Label(item.getNomorUrut() + "").setParent(row);
            new Label(item.getJenisAuditSPI().getNama()).setParent(row);
            RevisiHelper.createNewRevisi(KriteriaAuditSPI.class, item, item.getNama()).setParent(row);
            new Label(item.getKeterangan()).setParent(row);

            final MyCheckboxConfig aktifCb = new MyCheckboxConfig("Aktif");
            aktifCb.setDisabled(!edit);
            aktifCb.setChecked(item.getAktif());
            aktifCb.setParent(row);
            row.setAttribute("checkbox", aktifCb);
            aktifCb.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    item.setAktif(aktifCb.isChecked());
                    Common.refreshSaveOrUpdate(item);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, item, KriteriaAuditSPIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        init(new KriteriaAuditSPI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        kriteriaAuditSPI = (KriteriaAuditSPI) obj;
        buildForm(kriteriaAuditSPI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    private void buildForm(final KriteriaAuditSPI item) {
        FormHolder fh = prepareFormWindow("Pendataan Kriteria Audit SPI");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "No Urut");
        row.appendChild(nomorUrut = new MyIntbox(item.getNomorUrut()));

        row = addFormRow(rows, "Kriteria/Standar Acuan *");
        row.appendChild(nama = new Textbox(item.getNama()));
        nama.setWidth("90%");
        nama.setRows(5);

        row = addFormRow(rows, "Jenis Audit *");
        row.appendChild(jenisAuditSPI = new Combobox());
        jenisAuditSPI.setWidth("90%");
        Common.insertCombo(jenisAuditSPI, "nama", "keterangan",
                JenisAuditSPI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(jenisAuditSPI, item.getJenisAuditSPI() == null
                ? (searchjenisAuditSPI.getSelectedItem() == null ? null : searchjenisAuditSPI.getSelectedItem().getValue())
                : item.getJenisAuditSPI());
        jenisAuditSPI.setReadonly(true);

        row = addFormRow(rows, "Keterangan");
        row.appendChild(keterangan = new Textbox(item.getKeterangan()));
        keterangan.setWidth("90%");
        keterangan.setRows(5);

        finaliseFormWindow(fh, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
    }

    // =====================================================================
    // Save
    // =====================================================================

    public boolean onSave(Event event) throws Exception {
        if (nomorUrut.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Nomor Urut Kriteria Audit belum diisi."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) isi kolom Nomor Urut dengan angka yang menentukan urutan tampil kriteria;"
                    + " (2) pastikan nomor urut tidak kosong dan tidak duplikat dengan kriteria lain;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Kriteria/Standar Acuan belum diisi."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) isi kolom Kriteria/Standar Acuan dengan teks standar atau regulasi yang menjadi acuan audit;"
                    + " (2) pastikan isian tidak kosong dan relevan;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenisAuditSPI.getSelectedItem() == null || jenisAuditSPI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Audit belum dipilih."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) pilih Jenis Audit dari daftar yang tersedia untuk mengaitkan kriteria ini;"
                    + " (2) jika jenis yang dibutuhkan belum ada, tambahkan melalui menu Master Jenis Audit SPI;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (kriteriaAuditSPI.getId() != null) {
            kriteriaAuditSPI = (KriteriaAuditSPI) session.load(KriteriaAuditSPI.class, kriteriaAuditSPI.getId());
        }
        kriteriaAuditSPI.setNomorUrut(nomorUrut.getValue());
        kriteriaAuditSPI.setNama(nama.getValue());
        kriteriaAuditSPI.setJenisAuditSPI((JenisAuditSPI) jenisAuditSPI.getSelectedItem().getValue());
        kriteriaAuditSPI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, kriteriaAuditSPI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(KriteriaAuditSPI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchjenisAuditSPI.getSelectedItem() == null || searchjenisAuditSPI.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("jenisAuditSPI", searchjenisAuditSPI.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.asc("nomorUrut"));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<KriteriaAuditSPI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new KriteriaAuditSPIRenderer());
    }
}
