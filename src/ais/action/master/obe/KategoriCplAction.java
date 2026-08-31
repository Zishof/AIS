package ais.action.master.obe;

import java.util.List;

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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.obe.KategoriCpl;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;

/**
 * Layar CRUD master data Kategori CPL (Capaian Pembelajaran Lulusan) pada modul OBE
 * (Outcome-Based Education): Sikap, Pengetahuan, Keterampilan Umum, Keterampilan Khusus — empat
 * kategori baku ({@link #DEFAULT_CATEGORIES}) yang otomatis dibuat per perguruan tinggi lewat
 * {@link #ensureDefaults} bila belum ada data sama sekali, sehingga setiap institusi selalu punya
 * kategori dasar tanpa perlu input manual. Memperluas {@code ObeBaseAction} untuk mewarisi kerangka
 * layar CRUD OBE (pencarian, form, toolbar) yang seragam antar-entitas OBE. Kategori dapat
 * dinonaktifkan langsung dari grid lewat checkbox "Aktif" tanpa membuka form edit.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class KategoriCplAction extends ObeBaseAction {

    private static final long serialVersionUID = 1L;
    /** Kategori CPL baku (kode, nama) yang dibuat otomatis untuk setiap perguruan tinggi baru. */
    private static final String[][] DEFAULT_CATEGORIES = {
        {"S", "Sikap"},
        {"P", "Pengetahuan"},
        {"KU", "Keterampilan Umum"},
        {"KK", "Keterampilan Khusus"}
    };

    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;
    private KategoriCpl kategoriCpl;

    /** Menginisialisasi komponen umum layar, memastikan kategori CPL baku tersedia untuk perguruan tinggi berjalan, lalu memuat pencarian awal. */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        String[] contents = {"id", "kode", "nama", "keterangan", "aktif"};
        initCommon(comp, KategoriCpl.class, contents);
        ensureDefaults(HibernateUtil.currentSession(), perguruanTinggi);
        onSearchDefault(null);
    }

    /** Membuka form tambah kategori CPL baru. */
    public void onAdd(Event event) throws Exception {
        initForm(new KategoriCpl());
    }

    /** Membuka form ubah untuk kategori CPL yang dipilih dari grid. */
    @Override
    public void init(GeneralValueObject obj) throws Exception {
        initForm((KategoriCpl) obj);
    }

    /** Membangun form tambah/ubah kategori CPL (kode, nama, keterangan) beserta tombol simpan pada jendela dialog. */
    private void initForm(KategoriCpl item) {
        kategoriCpl = item;
        FormContext ctx = buildFormBorderlayout("Pendataan Kategori CPL");
        Rows rows = ctx.rows;

        kode = new Textbox(item.getKode());
        kode.setWidth("90%");
        addFormRow(rows, "Kode Kategori", kode);

        nama = new Textbox(item.getNama());
        nama.setWidth("90%");
        addFormRow(rows, "Nama Kategori", nama);

        keterangan = new Textbox(item.getKeterangan());
        keterangan.setWidth("90%");
        keterangan.setRows(3);
        addFormRow(rows, "Keterangan", keterangan);

        buildSouthToolbar(ctx.borderlayout, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        attachAndShow(ctx.borderlayout);
    }

    /**
     * Memvalidasi lalu menyimpan data kategori CPL dari form: menolak bila nama kosong atau sudah
     * dipakai kategori lain milik perguruan tinggi yang sama, jika lolos menyimpan/memperbarui
     * entitas dan mengembalikan {@code true}.
     *
     * @param event event ZK pemicu penyimpanan (tombol simpan)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     * @throws Exception diteruskan apa adanya dari kegagalan Hibernate saat menyimpan
     */
    public boolean onSave(Event event) throws Exception {
        if (!validateNamaRequired(nama, "Kategori CPL")) return false;

        Session session = HibernateUtil.currentSession();
        Number count = (Number) session.createCriteria(KategoriCpl.class)
                .add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
                .add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT))
                .add(kategoriCpl.getId() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", kategoriCpl.getId()))
                .setProjection(Projections.rowCount()).uniqueResult();
        if (count != null && count.intValue() > 0) {
            MyMessageboxConfig.show("Nama kategori CPL sudah ada di database", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }

        if (kategoriCpl.getId() != null) {
            kategoriCpl = (KategoriCpl) session.load(KategoriCpl.class, kategoriCpl.getId());
        }
        kategoriCpl.setKode(kode.getValue());
        kategoriCpl.setNama(nama.getValue());
        kategoriCpl.setKeterangan(keterangan.getValue());
        kategoriCpl.setPerguruanTinggi(perguruanTinggi);
        Common.refreshSaveOrUpdate(session, kategoriCpl);
        return true;
    }

    /** Menyusun kriteria pencarian {@link KategoriCpl} milik perguruan tinggi berjalan, difilter status aktif/kode/nama, diurutkan kode lalu nama bila diminta. */
    @Override
    public Criteria initCriteria(boolean order) {
        Criteria c = HibernateUtil.currentSession().createCriteria(KategoriCpl.class)
                .add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (searchkode != null && !searchkode.getValue().trim().isEmpty()) {
            c.add(Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        }
        if (searchnama != null && !searchnama.getValue().trim().isEmpty()) {
            c.add(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        }
        if (order) c.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
        return c;
    }

    /** Menjalankan pencarian default (memakai kriteria sekarang) dan merender hasilnya ke grid, no-op bila komponen pencarian belum siap. */
    @Override
    public void onSearchDefault(Event event) {
        if (searchnama == null) return;
        executeSearch(initCriteria(false), initCriteria(true), new KategoriCplRenderer());
    }

    /**
     * Mengambil daftar kategori CPL aktif milik satu perguruan tinggi, diurutkan kode lalu nama.
     * Memastikan kategori baku sudah tersedia (lewat {@link #ensureDefaults}) sebelum mengambil data
     * — dipakai layar lain (mis. pengisian CPL) yang butuh daftar kategori tanpa membuka layar ini.
     *
     * @param session sesi Hibernate aktif
     * @param pt      perguruan tinggi yang kategorinya diambil
     * @return daftar kategori CPL aktif, diurutkan kode lalu nama
     */
    public static List<KategoriCpl> activeCategories(Session session, PerguruanTinggi pt) {
        ensureDefaults(session, pt);
        return ConstantValues.simpleList(session.createCriteria(KategoriCpl.class)
                .add(Restrictions.eq("perguruanTinggi", pt))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("kode")).addOrder(Order.asc("nama")), KategoriCpl.class);
    }

    /**
     * Memastikan perguruan tinggi yang diberikan memiliki setidaknya satu kategori CPL; bila belum
     * punya data sama sekali, membuat keempat kategori baku {@link #DEFAULT_CATEGORIES} sekaligus.
     * Tidak melakukan apa pun bila {@code session}/{@code pt} null atau kategori sudah ada.
     *
     * @param session sesi Hibernate aktif
     * @param pt      perguruan tinggi yang diperiksa/diisi kategori bakunya
     */
    public static void ensureDefaults(Session session, PerguruanTinggi pt) {
        if (session == null || pt == null) return;
        Number count = (Number) session.createCriteria(KategoriCpl.class)
                .add(Restrictions.eq("perguruanTinggi", pt))
                .setProjection(Projections.rowCount()).uniqueResult();
        if (count != null && count.longValue() > 0) return;
        for (String[] value : DEFAULT_CATEGORIES) {
            KategoriCpl item = new KategoriCpl();
            item.setKode(value[0]);
            item.setNama(value[1]);
            item.setPerguruanTinggi(pt);
            item.setAktif(true);
            Common.refreshSaveOrUpdate(session, item);
        }
    }

    /** Renderer baris grid daftar kategori CPL: kolom kode, nama (ringkas), keterangan (ringkas), checkbox aktif (toggle langsung tersimpan), dan tombol edit/hapus. */
    class KategoriCplRenderer extends MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            final KategoriCpl item = (KategoriCpl) obj;
            row.setValign("top");
            new Label(item.getKode()).setParent(row);
            namaCellRingkas(KategoriCpl.class, item, item.getNama()).setParent(row);
            ringkasanKeterangan(item.getKeterangan()).setParent(row);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(item.getAktif());
            checkbox.setParent(row);
            row.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    item.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(item);
                }
            });
            Common.copyEditDeleteButtons(edit, delete, item, KategoriCplAction.this).setParent(row);
        }
    }
}
