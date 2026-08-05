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
import ais.database.model.spi.ChecklistAuditSPI;
import ais.database.model.spi.KriteriaAuditSPI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

/**
 * <h2>ChecklistAuditSPIAction &mdash; Pengendali Layar Data Master Checklist Audit (Level 3/Daun)</h2>
 *
 * <p>
 * Pengendali ZK untuk tab ketiga (dan terakhir) layar Setup SPI, tempat staf Satuan Pengawasan
 * Internal mendata langkah uji/pertanyaan pemeriksaan paling rinci (mis. "Periksa apakah saldo kas
 * kecil fisik sesuai dengan catatan pembukuan") di bawah satu {@link KriteriaAuditSPI}. Baris-baris
 * yang didata di sini adalah yang KELAK akan dirender satu per satu sebagai daftar centang saat
 * seorang auditor melaksanakan penugasan audit di lapangan (fitur pelaksanaan audit dibangun pada
 * fase berikutnya, Bagian C) &mdash; lihat javadoc {@link ChecklistAuditSPI} untuk penjelasan
 * lengkap prinsip snapshot yang mendasari relasi checklist ini dengan tabel temuan di masa depan.
 * </p>
 *
 * <h3>Struktur identik level 2, hanya induk yang berbeda</h3>
 * <p>
 * Kelas ini SENGAJA dibuat dengan struktur yang nyaris identik dengan {@link KriteriaAuditSPIAction}
 * (filter kategori induk, formulir dengan combobox induk wajib diisi, validasi urutan tampil) karena
 * pola kebutuhannya memang sama: satu entitas anak yang harus selalu terhubung ke satu entitas induk
 * tertentu. Konsistensi pola ini disengaja agar staf pengembang yang sudah memahami satu level dapat
 * langsung memahami level lainnya tanpa belajar pendekatan baru, dan agar perbaikan bug/pola pada
 * satu level mudah direplikasi ke level yang lain bila suatu hari diperlukan.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class ChecklistAuditSPIAction extends BaseSPIAction {

    private static final long serialVersionUID = 1L;

    // ---- Search fields ----
    private Combobox searchkriteriaAuditSPI;

    // ---- Form fields ----
    private MyIntbox nomorUrut;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox kriteriaAuditSPI;

    // ---- Current entity ----
    private ChecklistAuditSPI checklistAuditSPI;

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();

        Common.insertComboDanSemua(searchkriteriaAuditSPI, "nama", "keterangan",
                KriteriaAuditSPI.class, Restrictions.eq("aktif", true));

        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(ChecklistAuditSPI.class,
                new String[]{"id", "nomorUrut", "kriteriaAuditSPI", "nama", "keterangan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    class ChecklistAuditSPIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final ChecklistAuditSPI item = (ChecklistAuditSPI) obj;

            new Label(item.getNomorUrut() + "").setParent(row);
            new Label(item.getKriteriaAuditSPI().getNama()).setParent(row);
            RevisiHelper.createNewRevisi(ChecklistAuditSPI.class, item, item.getNama()).setParent(row);
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

            Common.copyEditDeleteButtons(edit, delete, item, ChecklistAuditSPIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        init(new ChecklistAuditSPI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        checklistAuditSPI = (ChecklistAuditSPI) obj;
        buildForm(checklistAuditSPI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    private void buildForm(final ChecklistAuditSPI item) {
        FormHolder fh = prepareFormWindow("Pendataan Checklist Audit SPI");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "No Urut");
        row.appendChild(nomorUrut = new MyIntbox(item.getNomorUrut()));

        row = addFormRow(rows, "Langkah Uji/Checklist *");
        row.appendChild(nama = new Textbox(item.getNama()));
        nama.setWidth("90%");
        nama.setRows(5);

        row = addFormRow(rows, "Kriteria Audit *");
        row.appendChild(kriteriaAuditSPI = new Combobox());
        kriteriaAuditSPI.setWidth("90%");
        Common.insertCombo(kriteriaAuditSPI, "nama", "keterangan",
                KriteriaAuditSPI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(kriteriaAuditSPI, item.getKriteriaAuditSPI() == null
                ? (searchkriteriaAuditSPI.getSelectedItem() == null ? null : searchkriteriaAuditSPI.getSelectedItem().getValue())
                : item.getKriteriaAuditSPI());
        kriteriaAuditSPI.setReadonly(true);

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
            MyMessageboxConfig.show("Mohon maaf, Nomor Urut Checklist Audit belum diisi."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) isi kolom Nomor Urut dengan angka yang menentukan urutan langkah uji;"
                    + " (2) pastikan nomor urut tidak kosong dan tidak duplikat dalam satu kriteria;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Langkah Uji/Checklist belum diisi."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) isi kolom Langkah Uji dengan prosedur atau pertanyaan yang akan diverifikasi saat audit;"
                    + " (2) pastikan isian tidak kosong dan spesifik;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (kriteriaAuditSPI.getSelectedItem() == null || kriteriaAuditSPI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Kriteria Audit belum dipilih."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) pilih Kriteria Audit dari daftar yang tersedia untuk mengaitkan checklist ini;"
                    + " (2) jika kriteria yang dibutuhkan belum ada, tambahkan melalui menu Kriteria Audit SPI;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (checklistAuditSPI.getId() != null) {
            checklistAuditSPI = (ChecklistAuditSPI) session.load(ChecklistAuditSPI.class, checklistAuditSPI.getId());
        }
        checklistAuditSPI.setNomorUrut(nomorUrut.getValue());
        checklistAuditSPI.setNama(nama.getValue());
        checklistAuditSPI.setKriteriaAuditSPI((KriteriaAuditSPI) kriteriaAuditSPI.getSelectedItem().getValue());
        checklistAuditSPI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, checklistAuditSPI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(ChecklistAuditSPI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchkriteriaAuditSPI.getSelectedItem() == null || searchkriteriaAuditSPI.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("kriteriaAuditSPI", searchkriteriaAuditSPI.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.asc("nomorUrut"));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<ChecklistAuditSPI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new ChecklistAuditSPIRenderer());
    }
}
