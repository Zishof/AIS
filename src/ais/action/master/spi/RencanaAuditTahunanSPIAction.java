package ais.action.master.spi;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.spi.JenisAuditSPI;
import ais.database.model.spi.ProfilRisikoSPI;
import ais.database.model.spi.RencanaAuditTahunanSPI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

/**
 * <h2>RencanaAuditTahunanSPIAction &mdash; Pengendali Layar Rencana Audit Tahunan (PKPT)</h2>
 *
 * <p>
 * Pengendali ZK untuk layar penyusunan Program Kerja Pengawasan Tahunan (PKPT) &mdash; daftar unit
 * kerja mana saja yang direncanakan diaudit sepanjang satu tahun, jenis audit apa, dan pada
 * triwulan mana. Lihat javadoc {@link RencanaAuditTahunanSPI} untuk penjelasan lengkap perbedaan
 * penugasan Reguler (berbasis {@link ProfilRisikoSPI}) dan Khusus (insidental).
 * </p>
 *
 * <h3>Combobox Profil Risiko dibangun manual, bukan lewat {@code Common.insertCombo}</h3>
 * <p>
 * Label combobox {@link #profilRisikoSPI} pada formulir perlu menampilkan gabungan tiga informasi
 * sekaligus (unit kerja, tahun, zona risiko &mdash; lihat {@link ProfilRisikoSPI#toString()}), yang
 * bukan satu kolom database tunggal. Karena {@code Common.insertCombo} membangun label combobox
 * langsung dari SATU nama properti Hibernate lewat refleksi, method itu tidak cocok dipakai di
 * sini; combobox ini karenanya diisi manual lewat {@link #populateProfilRisikoCombo(Combobox)}
 * yang mengambil data lewat query lalu memakai {@code toString()} entitas sebagai label &mdash;
 * pola yang sama dipakai layar-layar lain di aplikasi ini saat label combobox perlu gabungan lebih
 * dari satu field (mis. combobox status pada {@code TindakLanjutSPMIAction}).
 * </p>
 *
 * <h3>Pemilih Unit Kerja memakai pencarian bertahap, bukan combobox biasa</h3>
 * <p>
 * {@link #searchsatuanKerja} dan {@link #satuanKerja} (pada formulir) memakai
 * {@link AmbilDataSatuanKerjaBanbox} &mdash; konvensi baku di 300-an layar lain aplikasi ini untuk
 * memilih {@link SatuanKerja}. Versi awal layar ini memakai {@code Combobox} biasa yang diisi lewat
 * {@code Common.insertComboDanSemua(...)}, yang memuat SELURUH baris {@link SatuanKerja} ke memori
 * setiap kali layar dibuka &mdash; terbukti membuat layar ini terasa sangat lambat pada instalasi
 * dengan banyak unit kerja. {@link AmbilDataSatuanKerjaBanbox} memuat data secara bertahap (pohon
 * hierarki per-level, tabel "sering dipakai" dengan paging sisi server).
 * </p>
 *
 * <h3>Tautan Profil Risiko bersifat opsional pada formulir</h3>
 * <p>
 * Item pertama combobox {@link #profilRisikoSPI} selalu berupa opsi kosong "(Tidak ada / Audit
 * Khusus)" &mdash; wajar dipilih ketika {@link #jenisPenugasan} diisi "Khusus (Insidental)", karena
 * audit semacam itu tidak lahir dari hasil pemeringkatan risiko terjadwal. Lihat javadoc
 * {@link RencanaAuditTahunanSPI#getProfilRisikoSPI()} untuk penjelasan lebih lengkap.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class RencanaAuditTahunanSPIAction extends BaseSPIAction {

    private static final long serialVersionUID = 1L;

    // ---- Search fields ----
    private MyIntbox searchtahun;
    private AmbilDataSatuanKerjaBanbox searchsatuanKerja;
    private Combobox searchjenisAuditSPI;
    private Combobox searchstatusRealisasi;

    // ---- Form fields ----
    private MyIntbox tahun;
    private AmbilDataSatuanKerjaBanbox satuanKerja;
    private Combobox jenisAuditSPI;
    private Combobox profilRisikoSPI;
    private Combobox triwulanRencana;
    private Combobox jenisPenugasan;
    private Combobox statusRealisasi;
    private Textbox  keterangan;

    // ---- Current entity ----
    private RencanaAuditTahunanSPI rencanaAuditTahunanSPI;

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();

        searchsatuanKerja.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onSearchDefault(e);
            }
        });
        Common.insertComboDanSemua(searchjenisAuditSPI, "nama", "keterangan",
                JenisAuditSPI.class, Restrictions.eq("aktif", true));

        searchstatusRealisasi.appendChild(new Comboitem("Semua Status"));
        for (java.util.Map.Entry<String, String> e : RencanaAuditTahunanSPI.STATUS_REALISASI_DATA.entrySet()) {
            Comboitem ci = new Comboitem(e.getValue());
            ci.setValue(e.getKey());
            searchstatusRealisasi.appendChild(ci);
        }
        searchstatusRealisasi.setSelectedIndex(0);
        searchstatusRealisasi.setReadonly(true);

        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(RencanaAuditTahunanSPI.class,
                new String[]{"id", "tahun", "satuanKerja", "jenisAuditSPI", "triwulanRencana",
                        "jenisPenugasan", "statusRealisasi", "keterangan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    class RencanaAuditTahunanSPIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final RencanaAuditTahunanSPI item = (RencanaAuditTahunanSPI) obj;

            new Label(item.getTahun() == null ? "-" : item.getTahun().toString()).setParent(row);
            new Label(item.getSatuanKerja() == null ? "-" : item.getSatuanKerja().getNama()).setParent(row);
            new Label(item.getJenisAuditSPI() == null ? "-" : item.getJenisAuditSPI().getNama()).setParent(row);
            new Label("TW" + item.getTriwulanRencana()).setParent(row);

            Label jenisBadge = new Label(item.getJenisPenugasanLabel());
            jenisBadge.setSclass("ais-badge " + (RencanaAuditTahunanSPI.KHUSUS.equals(item.getJenisPenugasan())
                    ? "ais-badge-biru" : "ais-badge-abu"));
            jenisBadge.setParent(row);

            Label statusBadge = new Label(item.getStatusRealisasiLabel());
            statusBadge.setSclass("ais-badge " + statusSclass(item.getStatusRealisasi()));
            statusBadge.setParent(row);

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

            Common.copyEditDeleteButtons(edit, delete, item, RencanaAuditTahunanSPIAction.this).setParent(row);
        }
    }

    private static String statusSclass(String status) {
        if (RencanaAuditTahunanSPI.SELESAI.equals(status)) return "ais-badge-hijau";
        if (RencanaAuditTahunanSPI.SEDANG_BERJALAN.equals(status)) return "ais-badge-kuning";
        return "ais-badge-abu";
    }

    // =====================================================================
    // Combobox manual: Profil Risiko (label gabungan, lihat javadoc kelas)
    // =====================================================================

    @SuppressWarnings("unchecked")
    private void populateProfilRisikoCombo(Combobox cb) {
        Common.clear(cb);
        Comboitem kosong = new Comboitem("(Tidak ada / Audit Khusus)");
        kosong.setValue(null);
        cb.appendChild(kosong);

        Session session = HibernateUtil.currentSession();
        List<ProfilRisikoSPI> list = session.createCriteria(ProfilRisikoSPI.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.desc("tahun"))
                .list();
        for (ProfilRisikoSPI p : list) {
            Comboitem ci = new Comboitem(p.toString());
            ci.setValue(p);
            cb.appendChild(ci);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        init(new RencanaAuditTahunanSPI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        rencanaAuditTahunanSPI = (RencanaAuditTahunanSPI) obj;
        buildForm(rencanaAuditTahunanSPI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    private void buildForm(final RencanaAuditTahunanSPI item) {
        FormHolder fh = prepareFormWindow("Pendataan Rencana Audit Tahunan (PKPT)");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "Tahun Rencana *");
        row.appendChild(tahun = new MyIntbox(item.getTahun()));

        row = addFormRow(rows, "Unit Kerja *");
        try {
            row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        satuanKerja.setWidth("90%");
        SatuanKerja satuanKerjaAwal = item.getSatuanKerja() == null
                ? (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja")
                : item.getSatuanKerja();
        if (satuanKerjaAwal != null) {
            satuanKerja.setValue(satuanKerjaAwal.getNama());
            satuanKerja.setAttribute("satuanKerja", satuanKerjaAwal);
        }

        row = addFormRow(rows, "Jenis Audit *");
        row.appendChild(jenisAuditSPI = new Combobox());
        jenisAuditSPI.setWidth("90%");
        Common.insertCombo(jenisAuditSPI, "nama", "keterangan",
                JenisAuditSPI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(jenisAuditSPI, item.getJenisAuditSPI());

        row = addFormRow(rows, "Dasar Profil Risiko");
        row.appendChild(profilRisikoSPI = new Combobox());
        profilRisikoSPI.setWidth("90%");
        profilRisikoSPI.setReadonly(true);
        populateProfilRisikoCombo(profilRisikoSPI);
        Common.selectComboItem(profilRisikoSPI, item.getProfilRisikoSPI());

        row = addFormRow(rows, "Triwulan Rencana *");
        row.appendChild(triwulanRencana = new Combobox());
        triwulanRencana.setReadonly(true);
        for (int tw = 1; tw <= 4; tw++) {
            Comboitem ci = new Comboitem("Triwulan " + tw);
            ci.setValue(tw);
            triwulanRencana.appendChild(ci);
        }
        Common.selectComboItem(triwulanRencana, item.getTriwulanRencana());

        row = addFormRow(rows, "Jenis Penugasan *");
        row.appendChild(jenisPenugasan = new Combobox());
        jenisPenugasan.setReadonly(true);
        for (java.util.Map.Entry<String, String> e : RencanaAuditTahunanSPI.JENIS_PENUGASAN_DATA.entrySet()) {
            Comboitem ci = new Comboitem(e.getValue());
            ci.setValue(e.getKey());
            jenisPenugasan.appendChild(ci);
        }
        Common.selectComboItem(jenisPenugasan, item.getJenisPenugasan());

        row = addFormRow(rows, "Status Realisasi *");
        row.appendChild(statusRealisasi = new Combobox());
        statusRealisasi.setReadonly(true);
        for (java.util.Map.Entry<String, String> e : RencanaAuditTahunanSPI.STATUS_REALISASI_DATA.entrySet()) {
            Comboitem ci = new Comboitem(e.getValue());
            ci.setValue(e.getKey());
            statusRealisasi.appendChild(ci);
        }
        Common.selectComboItem(statusRealisasi, item.getStatusRealisasi());

        row = addFormRow(rows, "Keterangan");
        row.appendChild(keterangan = new Textbox(item.getKeterangan()));
        keterangan.setWidth("90%");
        keterangan.setRows(4);

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
        if (tahun.getValue() == null || tahun.getValue() < 2000 || tahun.getValue() > 2100) {
            MyMessageboxConfig.show("Mohon maaf, Tahun Rencana Audit belum diisi dengan benar."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) isi kolom Tahun Rencana dengan angka tahun yang valid (contoh: 2025);"
                    + " (2) pastikan tahun berada dalam rentang 2000 hingga 2100;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (satuanKerja.getAttribute("satuanKerja") == null) {
            MyMessageboxConfig.show("Mohon maaf, Unit Kerja belum dipilih."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) klik tombol pemilih Unit Kerja dan cari unit yang akan dijadwalkan audit tahunannya;"
                    + " (2) pastikan unit kerja sudah terdaftar di master data;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenisAuditSPI.getSelectedItem() == null || jenisAuditSPI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Audit belum dipilih."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) pilih Jenis Audit dari daftar yang tersedia;"
                    + " (2) jika jenis yang dibutuhkan belum ada, tambahkan melalui menu Master Jenis Audit SPI;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (rencanaAuditTahunanSPI.getId() != null) {
            rencanaAuditTahunanSPI = (RencanaAuditTahunanSPI) session.load(RencanaAuditTahunanSPI.class, rencanaAuditTahunanSPI.getId());
        }
        rencanaAuditTahunanSPI.setTahun(tahun.getValue());
        rencanaAuditTahunanSPI.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
        rencanaAuditTahunanSPI.setJenisAuditSPI((JenisAuditSPI) jenisAuditSPI.getSelectedItem().getValue());
        rencanaAuditTahunanSPI.setProfilRisikoSPI((ProfilRisikoSPI) profilRisikoSPI.getSelectedItem().getValue());
        rencanaAuditTahunanSPI.setTriwulanRencana((Integer) triwulanRencana.getSelectedItem().getValue());
        rencanaAuditTahunanSPI.setJenisPenugasan((String) jenisPenugasan.getSelectedItem().getValue());
        rencanaAuditTahunanSPI.setStatusRealisasi((String) statusRealisasi.getSelectedItem().getValue());
        rencanaAuditTahunanSPI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, rencanaAuditTahunanSPI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(RencanaAuditTahunanSPI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchtahun.getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("tahun", searchtahun.getValue()))
                .add(searchsatuanKerja.getAttribute("satuanKerja") == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("satuanKerja", searchsatuanKerja.getAttribute("satuanKerja")))
                .add(searchjenisAuditSPI.getSelectedItem() == null || searchjenisAuditSPI.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("jenisAuditSPI", searchjenisAuditSPI.getSelectedItem().getValue()))
                .add(searchstatusRealisasi.getSelectedIndex() <= 0 || searchstatusRealisasi.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("statusRealisasi", searchstatusRealisasi.getSelectedItem().getValue()));
        if (order) criteria.addOrder(Order.desc("tahun")).addOrder(Order.asc("triwulanRencana"));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<RencanaAuditTahunanSPI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new RencanaAuditTahunanSPIRenderer());
    }
}
