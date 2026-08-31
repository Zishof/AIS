package ais.action.master.spmi;

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
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

/**
 * Layar CRUD data master "Standar SPMI/Referensi Eksternal" (Sistem Penjaminan Mutu Internal):
 * mendata standar mutu beserta jenisnya ({@link JenisSPMI}) dan nomor urut tampilnya. Dibangun di
 * atas {@link BaseSPMIAction} (menyediakan pola form/pencarian dasar SPMI bersama); kelas ini
 * menambahkan field spesifik (nomor urut, nama, jenis SPMI, keterangan), filter pencarian jenis
 * SPMI, validasi field wajib, serta baris daftar dengan checkbox aktif/tidak aktif yang langsung
 * tersimpan saat diubah (di luar alur simpan form) dan tombol edit/hapus.
 */
public class StandarSPMIAction extends BaseSPMIAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // ---- Search fields ----
    private Combobox searchjenisSPMI;

    // ---- Form fields ----
    private MyIntbox nomorUrut;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox jenisSPMI;

    // ---- Current entity ----
    private StandarSPMI standarSPMI;

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    /** Menginisialisasi bahasa, hak akses, dropdown filter jenis SPMI, pemuatan data awal, paging, dan tombol cetak/unggah setelah komponen ZK dirakit. */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();

        Common.insertComboDanSemua(searchjenisSPMI, "nama", "keterangan", 
                JenisSPMI.class, Restrictions.eq("aktif", true));

        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(StandarSPMI.class,
                new String[]{"id", "nomorUrut", "jenisSPMI", "nama", "keterangan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    /** Merender satu baris daftar Standar SPMI: nomor urut, jenis SPMI, label revisi+nama, keterangan, checkbox aktif (langsung tersimpan saat diubah), dan tombol edit/hapus. */
    class StandarSPMIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final StandarSPMI item = (StandarSPMI) obj;

            new Label(item.getNomorUrut() + "").setParent(row);
            new Label(item.getJenisSPMI().getNama()).setParent(row);
            RevisiHelper.createNewRevisi(StandarSPMI.class, item, item.getNama()).setParent(row);
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

            Common.copyEditDeleteButtons(edit, delete, item, StandarSPMIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    /** Membuka form tambah dengan entitas {@link StandarSPMI} baru (kosong). */
    public void onAdd(Event event) throws Exception {
        init(new StandarSPMI());
    }

    /** Menyiapkan form tambah/edit untuk entitas {@code obj} dan membuka jendela form. */
    @Override
    public void init(GeneralValueObject obj) throws Exception {
        standarSPMI = (StandarSPMI) obj;
        buildForm(standarSPMI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    /** Membangun baris-baris form tambah/edit Standar SPMI (nomor urut, nama, jenis SPMI, keterangan) dan mendaftarkan handler simpan. */
    private void buildForm(final StandarSPMI item) {
        FormHolder fh = prepareFormWindow("Pendataan Standar SPMI/Referensi Eksternal");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "No Urut");
        row.appendChild(nomorUrut = new MyIntbox(item.getNomorUrut()));

        row = addFormRow(rows, "Standar SPMI/Referensi Eksternal *");
        row.appendChild(nama = new Textbox(item.getNama()));
        nama.setWidth("90%");
        nama.setRows(5);

        row = addFormRow(rows, "Jenis Standar SPMI *");
        row.appendChild(jenisSPMI = new Combobox());
        jenisSPMI.setWidth("90%");
        Common.insertCombo(jenisSPMI, "nama", "keterangan",
                JenisSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(jenisSPMI, item.getJenisSPMI() == null
                ? (searchjenisSPMI.getSelectedItem() == null ? null : searchjenisSPMI.getSelectedItem().getValue())
                : item.getJenisSPMI());
        jenisSPMI.setReadonly(true);

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

    /** Memvalidasi (nomor urut, nama, dan jenis SPMI wajib diisi) lalu menyimpan/memperbarui entitas {@link StandarSPMI} dari nilai form. @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal. */
    public boolean onSave(Event event) throws Exception {
        if (nomorUrut.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, nomor urut Standar SPMI belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Nomor Urut pada form dengan nilai angka; "
                    + "(2) pastikan kolom tidak kosong sebelum menyimpan; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Standar SPMI/Referensi Eksternal belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Standar SPMI/Referensi Eksternal pada form; "
                    + "(2) pastikan teks tidak kosong atau hanya berisi spasi; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenisSPMI.getSelectedItem() == null || jenisSPMI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Standar SPMI belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Jenis Standar SPMI dari daftar pilihan; "
                    + "(2) pastikan daftar sudah memuat data Jenis SPMI yang tersedia; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (standarSPMI.getId() != null) {
            standarSPMI = (StandarSPMI) session.load(StandarSPMI.class, standarSPMI.getId());
        }
        standarSPMI.setNomorUrut(nomorUrut.getValue());
        standarSPMI.setNama(nama.getValue());
        standarSPMI.setJenisSPMI((JenisSPMI) jenisSPMI.getSelectedItem().getValue());
        standarSPMI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, standarSPMI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    /** Membangun kriteria pencarian {@link StandarSPMI} berdasarkan filter status aktif, nama (ILIKE sebagian), dan jenis SPMI, diurutkan menurut nomor urut bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(StandarSPMI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchjenisSPMI.getSelectedItem() == null || searchjenisSPMI.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("jenisSPMI", searchjenisSPMI.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.asc("nomorUrut"));
        return criteria;
    }

    /** Menjalankan pencarian dengan kriteria saat ini, memuat satu halaman hasil sesuai paging aktif, dan menyegarkan grid daftar. */
    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<StandarSPMI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new StandarSPMIRenderer());
    }
}
