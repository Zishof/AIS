package ais.action.master.akreditasi;

import java.io.ByteArrayOutputStream;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.model.Jurusan;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Kelas dasar untuk semua jendela laporan Akreditasi LKPS.
 *
 * <p>Kelas ini menyediakan infrastruktur tampilan yang dipakai bersama oleh
 * seluruh laporan LKPS (Laporan Kinerja Program Studi): tata letak BorderLayout
 * (North = filter + tombol aksi, Center = konten laporan), tombol "Tampilkan Data"
 * dan "Download Excel", serta dua Combobox bersama — Fakultas dan Program Studi —
 * yang saling berelasi secara otomatis (memilih Fakultas langsung memfilter daftar
 * Program Studi).</p>
 *
 * <h3>Cara pemakaian di sub-kelas</h3>
 * <ol>
 *   <li>Di konstruktor, panggil {@link #initFakultasJurusan()} <em>sebelum</em>
 *       {@link #buildBase(boolean)} untuk menginisialisasi kedua Combobox.</li>
 *   <li>Override {@link #buildFilters(Row)} dan panggil
 *       {@link #addFakultasJurusanFilter(Row)} untuk menyisipkan filter Fakultas
 *       + Program Studi ke baris filter. Tambahkan filter lain (misal Tahun
 *       Akademik) setelahnya.</li>
 *   <li>Gunakan {@link #getSelectedJurusan()} untuk mendapatkan Program Studi
 *       yang dipilih pengguna pada saat data dimuat.</li>
 *   <li>Override {@link #getSheetCode()} untuk mengembalikan kode sheet LKPS
 *       (mis. {@code "LKPS-1.A.2"}) — kode ini menentukan konfigurasi kolom tabel
 *       dan grafik yang dipakai oleh {@link SaptoUtil}.</li>
 *   <li>Override {@link #onCetak(Event)} untuk menjalankan query dan mengisi
 *       data ke {@code Label} yang diteruskan ke {@link #display(Label, int)}.</li>
 * </ol>
 *
 * <h3>Manajemen sesi database</h3>
 * <ul>
 *   <li>{@code HibernateUtil.currentNativeSession()} — WAJIB ditutup di blok
 *       {@code finally}: {@code session.disconnect()} + {@code session.close()}
 *       + {@code HibernateUtil.closeSession()}.</li>
 *   <li>{@code HibernateUtil.currentSession()} — JANGAN ditutup; sudah dikelola
 *       oleh framework secara otomatis.</li>
 * </ul>
 *
 * <h3>Kompatibilitas</h3>
 * <p>Kelas ini kompatibel dengan ZK 5.5, Java 1.7, dan Hibernate 3.6.
 * Blok try-catch menggunakan sintaks Java 1.6 (satu Exception per blok catch,
 * tidak menggunakan multi-catch {@code |}).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see ais.action.master.sapto.util.SaptoUtil
 * @see ais.action.master.sapto.util.SaptoGridConfig
 */
public abstract class AkreditasiBaseWindow extends MyWindow {

    private static final long serialVersionUID = 1L;
    private static final String MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Area konten utama — laporan/tabel ditampilkan di sini. */
    protected Center contentCenter;

    /**
     * Combobox Fakultas — diinisialisasi oleh {@link #initFakultasJurusan()}.
     * Memilih item di Combobox ini otomatis memfilter daftar {@link #jurusan}.
     */
    protected Combobox fakultas = new Combobox();

    /**
     * Combobox Program Studi (Jurusan) — diinisialisasi oleh {@link #initFakultasJurusan()}.
     * Daftar isinya diperbarui otomatis ketika pengguna memilih Fakultas.
     */
    protected Combobox jurusan = new Combobox();

    /** Konstruktor default untuk ZK deserialisasi. */
    public AkreditasiBaseWindow() {
        super();
    }

    /**
     * Konstruktor dengan parameter tampilan jendela.
     *
     * @param title    judul jendela yang tampil di title bar
     * @param border   gaya border ZK (mis. {@code "normal"})
     * @param closable {@code true} jika jendela dapat ditutup pengguna
     * @throws Exception jika ZK gagal membuat komponen
     */
    public AkreditasiBaseWindow(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
    }

    // -------------------------------------------------------------------------
    // Inisialisasi Fakultas & Program Studi
    // -------------------------------------------------------------------------

    /**
     * Menginisialisasi Combobox Fakultas dan Program Studi secara bersamaan
     * dengan relasi otomatis: memilih Fakultas akan memperbarui daftar Program
     * Studi yang sesuai.
     *
     * <p>Metode ini memanggil {@link Common#initFakultasDanJurusanDanSemua(Combobox, Combobox)}
     * yang menangani: memuat semua Fakultas, memuat Program Studi sesuai Fakultas
     * terpilih, serta mengunci pilihan sesuai hak akses pengguna yang sedang login
     * (jika pengguna hanya boleh akses satu Fakultas/Prodi tertentu).</p>
     *
     * <p><strong>Kapan memanggil:</strong> panggil di konstruktor sub-kelas,
     * <em>sebelum</em> memanggil {@link #buildBase(boolean)}.</p>
     */
    protected void initFakultasJurusan() {
        Common.initFakultasDanJurusanDanSemua(fakultas, jurusan);
    }

    /**
     * Menambahkan pasangan label+Combobox Fakultas dan Program Studi ke baris
     * filter yang diberikan.
     *
     * <p>Setelah kedua Combobox ditambahkan, listener {@code onChange} dipasang
     * agar perubahan pilihan otomatis memicu {@link #onCetak(Event)} sehingga
     * data diperbarui secara langsung tanpa perlu klik tombol "Tampilkan".</p>
     *
     * <p><strong>Urutan pemanggilan:</strong> panggil dari dalam override
     * {@link #buildFilters(Row)} sebagai langkah pertama sebelum filter lain
     * (mis. Tahun Akademik).</p>
     *
     * @param row baris filter ZK ({@link Row}) tempat Combobox disisipkan
     */
    protected void addFakultasJurusanFilter(Row row) {
        row.appendChild(new MyLabelConfig("Fakultas"));
        fakultas.setWidth("90%");
        row.appendChild(fakultas);
        fakultas.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onCetak(null);
            }
        });

        row.appendChild(new MyLabelConfig("Program Studi"));
        jurusan.setWidth("90%");
        row.appendChild(jurusan);
        jurusan.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onCetak(null);
            }
        });
    }

    /**
     * Mengembalikan Program Studi (Jurusan) yang sedang dipilih pengguna, atau
     * {@code null} jika pengguna memilih "Semua Program Studi".
     *
     * <p>Nilai {@code null} berarti query tidak perlu dibatasi berdasarkan
     * Program Studi — tampilkan data dari semua prodi.</p>
     *
     * @return objek {@link Jurusan} terpilih, atau {@code null} jika "Semua"
     */
    protected Jurusan getSelectedJurusan() {
        if (jurusan.getSelectedItem() == null) return null;
        Object val = jurusan.getSelectedItem().getValue();
        return (val instanceof Jurusan) ? (Jurusan) val : null;
    }

    // -------------------------------------------------------------------------
    // Pembangunan tata letak dasar
    // -------------------------------------------------------------------------

    /**
     * Membangun tata letak dasar dengan North (filter) dan Center (konten),
     * termasuk tombol "Tampilkan Data" dan "Download Excel".
     *
     * <p>Overload dari {@link #buildBase(boolean, boolean)} dengan
     * {@code showNorth=true}.</p>
     *
     * @param autoLoad {@code true} untuk langsung memanggil {@link #onCetak(Event)}
     *                 saat komponen selesai dibangun
     */
    protected void buildBase(boolean autoLoad) {
        buildBase(autoLoad, true);
    }

    /**
     * Membangun tata letak lengkap jendela laporan.
     *
     * <p>Tata letak yang dibuat:</p>
     * <pre>
     * ┌───────────────────────── North ──────────────────────────┐
     * │  [Filter 1]  [Filter 2]  [Tampilkan Data] [Download Excel]│
     * ├───────────────────────── Center ─────────────────────────┤
     * │  (area konten — tabel/chart/spreadsheet)                  │
     * └──────────────────────────────────────────────────────────┘
     * </pre>
     *
     * <p>North berisi baris filter yang diisi oleh {@link #buildFilters(Row)}.
     * Center diisi oleh {@link #display(org.zkoss.zul.Label, int)} setelah
     * data selesai dimuat dari background thread.</p>
     *
     * @param autoLoad  {@code true} untuk langsung memuat data saat komponen selesai
     * @param showNorth {@code false} untuk menyembunyikan panel filter (tanpa filter)
     */
    protected void buildBase(boolean autoLoad, boolean showNorth) {
        setHeight("100%");
        setWidth("100%");

        Borderlayout bl = new MyBorderlayout();
        bl.setParent(this);

        North north = new North();
        north.setCollapsible(true);
        north.setVisible(showNorth);
        north.setParent(bl);
        ZkCompat.setFlex(north, true);

        MyGrid toolGrid = new MyGrid();
        toolGrid.setWidth("100%");
        toolGrid.setParent(north);

        contentCenter = new Center();
        contentCenter.setParent(bl);
        ZkCompat.setFlex(contentCenter, true);

        Rows rows = new Rows();
        rows.setParent(toolGrid);

        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);

        buildFilters(row);

        MyToolbarbuttonConfig btnSearch = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
        btnSearch.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onCetak(null);
            }
        });
        btnSearch.setParent(row);

        MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
        btnDownload.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                try {
                    Spreadsheet excelku = (Spreadsheet) contentCenter.getAttribute("excelku");
                    if (excelku == null) {
                        Common.showInfo("Tampilkan data terlebih dahulu, lalu klik Download.");
                        return;
                    }
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    excelku.getBook().write(bout);
                    bout.close();
                    Filedownload.save(bout.toByteArray(), MIME_XLSX, getSheetCode() + ".xlsx");
                } catch (Exception ex) {
                    Common.tampilErrorJikaAdmin(ex);
                }
            }
        });
        btnDownload.setParent(row);

        if (autoLoad) {
            onCetak(null);
        }
    }

    // -------------------------------------------------------------------------
    // Metode abstrak & hook untuk sub-kelas
    // -------------------------------------------------------------------------

    /**
     * Hook untuk sub-kelas menambahkan widget filter ke baris filter North.
     *
     * <p>Implementasi default kosong — tidak ada filter tambahan. Sub-kelas
     * sebaiknya memanggil {@link #addFakultasJurusanFilter(Row)} sebagai langkah
     * pertama di dalam override metode ini.</p>
     *
     * @param row baris filter tempat widget disisipkan
     */
    protected void buildFilters(Row row) {
        // Override in subclass to add filter widgets
    }

    /**
     * Mengembalikan kode sheet LKPS yang unik untuk laporan ini, mis.
     * {@code "LKPS-1.A.2"}. Kode ini dipakai oleh {@link SaptoUtil} untuk
     * menentukan konfigurasi kolom, deskripsi, dan tipe grafik yang sesuai.
     *
     * @return kode sheet LKPS (tidak boleh null)
     */
    protected abstract String getSheetCode();

    /**
     * Dipanggil saat pengguna mengklik "Tampilkan Data" atau mengubah filter.
     * Sub-kelas harus menjalankan query di background thread, mengisi atribut
     * {@code "datas"} ke Label, lalu mengosongkan nilai Label agar timer
     * di {@link SaptoUtil#displayWorksheet} mendeteksi data sudah siap.
     *
     * @param event event ZK (bisa {@code null} jika dipanggil secara programatik)
     */
    public abstract void onCetak(Event event);

    // -------------------------------------------------------------------------
    // Utilitas konten
    // -------------------------------------------------------------------------

    /**
     * Mengosongkan area konten sehingga laporan baru dapat ditampilkan.
     * Dipanggil di awal {@link #onCetak(Event)} sebelum memuat data baru.
     */
    protected void clearContent() {
        Common.clear(contentCenter);
    }

    /**
     * Memulai proses tampil data berbasis timer — memanggil
     * {@link SaptoUtil#displayWorksheet(org.zkoss.zul.Label, String, org.zkoss.zk.ui.Component, int)}.
     *
     * @param label   Label penanda progres; nilai diisi sub-kelas saat loading,
     *                dikosongkan saat data siap
     * @param maxCols jumlah kolom maksimum untuk spreadsheet Excel
     */
    protected void display(org.zkoss.zul.Label label, int maxCols) {
        SaptoUtil.displayWorksheet(label, getSheetCode(), contentCenter, maxCols);
    }

    /**
     * Varian {@link #display(org.zkoss.zul.Label, int)} dengan listener klik sel
     * untuk laporan yang mendukung drill-down ke detail data.
     *
     * @param label       Label penanda progres
     * @param maxCols     jumlah kolom maksimum spreadsheet
     * @param onCellClick listener yang dipanggil saat pengguna mengklik sel Excel
     */
    protected void display(org.zkoss.zul.Label label, int maxCols, org.zkoss.zk.ui.event.EventListener onCellClick) {
        SaptoUtil.displayWorksheet(label, getSheetCode(), contentCenter, maxCols, onCellClick);
    }
}
