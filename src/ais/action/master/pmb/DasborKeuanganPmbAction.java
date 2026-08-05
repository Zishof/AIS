package ais.action.master.pmb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.KegiatanProsesHeper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * DasborKeuanganPmbAction — Dasbor Keuangan Penerimaan Mahasiswa Baru (PMB).
 *
 * <p>Kelas ini bertanggung jawab menampilkan seluruh ringkasan, analitik, dan
 * visualisasi data keuangan calon mahasiswa baru secara real-time. Dirancang
 * agar dapat dipahami oleh pengguna non-teknis — mulai dari staf keuangan,
 * pimpinan, hingga manajemen — tanpa perlu memahami database atau teknologi
 * informasi secara mendalam.</p>
 *
 * <h3>Panel yang Ditampilkan</h3>
 * <ol>
 *   <li><b>Kartu Ringkasan (6 kartu)</b> — Angka-angka kunci: total calon
 *       terdaftar, total tagihan, sudah dibayar, sisa belum lunas, total yang
 *       sudah lunas daftar ulang, dan jumlah yang belum melakukan pembayaran
 *       apapun. Keenam angka ini memberi gambaran instan kondisi keuangan PMB
 *       tanpa perlu membuka laporan detail.</li>
 *   <li><b>Status Pelunasan (Donut Chart)</b> — Lingkaran terbagi dua warna
 *       yang menunjukkan berapa persen calon sudah lunas daftar ulang versus
 *       yang belum. Ideal untuk presentasi kepada pimpinan karena langsung
 *       terlihat proporsinya secara visual.</li>
 *   <li><b>Realisasi Pembayaran (Progress Bar)</b> — Batang kemajuan yang
 *       memperlihatkan seberapa besar total tagihan yang sudah masuk ke kas
 *       institusi, disertai nominal Dibayar dan Sisa secara terpisah.</li>
 *   <li><b>Perjalanan Pendaftaran (Funnel/Corong)</b> — Visualisasi corong
 *       5 tahap yang menunjukkan berapa calon yang berhasil melewati tiap
 *       tahap: mendaftar → lulus seleksi → bayar registrasi → ada tagihan
 *       daftar ulang → lunas daftar ulang. Sangat berguna untuk mendeteksi
 *       di tahap mana banyak calon "gugur".</li>
 *   <li><b>Radar Kesehatan Keuangan (Spider Web Chart)</b> — Grafik radar
 *       SVG 5 sumbu yang menampilkan 5 indikator kesehatan sekaligus dalam
 *       satu tampilan: % tagihan terbayar, % bayar registrasi, % ada tagihan
 *       DU, % lunas DU, dan % registrasi lunas. Semakin lebar area radarnya,
 *       semakin sehat kondisi keuangan PMB.</li>
 *   <li><b>Distribusi per Program Studi</b> — Grafik batang horizontal yang
 *       membandingkan tagihan dan pembayaran antar program studi, sehingga
 *       jelas program studi mana yang paling banyak calonnya dan bagaimana
 *       kondisi pembayarannya masing-masing.</li>
 *   <li><b>Distribusi per Gelombang</b> — Grafik batang horizontal per
 *       gelombang pendaftaran, untuk melihat apakah gelombang awal atau
 *       akhir yang lebih banyak menghasilkan pembayaran.</li>
 *   <li><b>Tren Tagihan Registrasi per Tahun</b> — Grafik batang ganda
 *       (abu = tagihan, biru = terbayar) per angkatan, memperlihatkan
 *       pertumbuhan penerimaan dari tahun ke tahun.</li>
 *   <li><b>Tren Tagihan Daftar Ulang per Tahun</b> — Sama dengan di atas
 *       namun untuk komponen daftar ulang (hijau = terbayar).</li>
 *   <li><b>Rekap Tabel + Download Excel</b> — Tabel numerik lengkap per
 *       angkatan beserta tombol unduh ke file Excel dua lembar (rekap +
 *       per program studi).</li>
 * </ol>
 *
 * <h3>Prinsip Reuse dalam Kelas Ini</h3>
 * <p>Kelas ini memaksimalkan penggunaan method pembantu (helper) yang dapat
 * dipakai ulang:</p>
 * <ul>
 *   <li>{@link #kartu} — dipakai oleh semua 6 kartu ringkasan</li>
 *   <li>{@link #legendItem} — dipakai oleh donut chart</li>
 *   <li>{@link #buildFunnelStep} — dipakai 5x untuk 5 tahap corong</li>
 *   <li>{@link #buildGrafikPerTahunGeneric} — dipakai untuk registrasi DAN daftar ulang</li>
 *   <li>{@link #buildGrafikHorizontalBatang} — dipakai untuk per-prodi DAN per-gelombang</li>
 *   <li>{@link #buildBarHorizontalRow} — dipakai untuk setiap baris di grafik horizontal</li>
 *   <li>{@link #buildWhere} dan {@link #applyParams} — dipakai oleh semua query HQL</li>
 * </ul>
 *
 * <h3>Manajemen Sesi Hibernate</h3>
 * <p>Semua query menggunakan sesi yang dibuka via
 * {@code HibernateUtil.getSessionFactory().openSession()} dan <b>selalu</b>
 * ditutup di blok {@code finally} (clear → disconnect → close).
 * Tidak menggunakan {@code currentSession()} karena eksekusi terjadi dari
 * timer ZK yang mungkin berjalan di thread tanpa sesi aktif.</p>
 *
 * <h3>Kompatibilitas</h3>
 * <ul>
 *   <li>Java 1.7: tidak ada lambda, try-with-resources, diamond operator, Stream API.</li>
 *   <li>Hibernate 5.x: menggunakan {@code org.hibernate.Query}.</li>
 *   <li>ZK 5.5: menggunakan komponen klasik (Grid, Div, Html, Combobox).</li>
 *   <li>Responsif: CSS grid auto-fill dan media query 640px / 768px.</li>
 *   <li>Grafik: HTML/CSS/SVG murni — tanpa JFreeChart atau library eksternal.</li>
 * </ul>
 *
 * <h3>Cara Menambah Panel Baru</h3>
 * <ol>
 *   <li>Tambahkan field data baru di bagian "State data".</li>
 *   <li>Tambahkan query di {@link #queryData(Session)}.</li>
 *   <li>Buat method builder HTML baru (ikuti pola {@code build*()}).</li>
 *   <li>Panggil method tersebut dari {@link #renderDasbor()}.</li>
 * </ol>
 *
 * @author AIS Team
 * @version 2026.07.08
 */
public class DasborKeuanganPmbAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 20260708L;

    // ── Wire ZK ──────────────────────────────────────────────────────────────
    private Div rootContainer;

    // ── Filter aktif ─────────────────────────────────────────────────────────
    private int    filterTahun    = 0;
    private String filterSemester = "";
    private String filterJenis    = "";   // "" semua, "REG" registrasi, "DU" daftar ulang

    // ── Referensi komponen UI ────────────────────────────────────────────────
    private Combobox cboTahun;
    private Combobox cboSemester;
    private Combobox cboJenis;
    private Div      divDasborContent;
    private Div      divBtnProses;     // diisi ulang setiap render agar ikut filterTahun

    // ── State data (dipertahankan agar Excel bisa reuse tanpa query ulang) ───
    private long totalCalon          = 0L;
    private long totalTagihan        = 0L;
    private long totalDibayar        = 0L;
    private long totalLunas          = 0L;   // lunas daftar ulang
    private long lulusSeleksi        = 0L;   // bcm.prodiLulus IS NOT NULL
    private long totalBayarReg       = 0L;   // reg.dibayar > 0
    private long totalPunyaDU        = 0L;   // INNER JOIN pembayaranDaftarUlang
    private long totalLunasReg       = 0L;   // reg.lunas = true

    private List<Object[]> dataPerTahun     = new ArrayList<Object[]>();
    private List<Object[]> dataPerTahunDU   = new ArrayList<Object[]>();
    private List<Object[]> dataPerProdi     = new ArrayList<Object[]>();
    private List<Object[]> dataPerGelombang = new ArrayList<Object[]>();

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        filterTahun    = Calendar.getInstance().get(Calendar.YEAR);
        filterSemester = "";
        bangunHeaderDanFilter();
        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                muatDasbor();
            }
        });
    }

    // =========================================================================
    // HEADER & FILTER BAR
    // =========================================================================

    private void bangunHeaderDanFilter() {
        new Html(buildCss()).setParent(rootContainer);

        new Html(
            "<div class='dgk-header'>" +
            "<div class='dgk-title'>&#x1F4CA; Dasbor Keuangan PMB</div>" +
            "<div class='dgk-subtitle'>Ringkasan tagihan dan pembayaran seluruh calon mahasiswa baru." +
            " Atur filter lalu klik <b>Tampilkan</b> untuk memperbarui data.</div>" +
            "</div>"
        ).setParent(rootContainer);

        Div bar = new Div();
        bar.setSclass("dgk-filter-bar");
        bar.setParent(rootContainer);

        new Label(ais.common.Common.getBahasaConfig("Tahun:")).setParent(bar);
        cboTahun = new Combobox();
        cboTahun.setWidth("88px");
        cboTahun.setReadonly(true);
        Comboitem ciSemua = new Comboitem("Semua");
        ciSemua.setValue(Integer.valueOf(0));
        cboTahun.appendChild(ciSemua);
        int nowYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int t = nowYear + 1; t >= nowYear - 5; t--) {
            Comboitem ci = new Comboitem(String.valueOf(t));
            ci.setValue(Integer.valueOf(t));
            cboTahun.appendChild(ci);
            if (t == filterTahun) cboTahun.setSelectedItem(ci);
        }
        if (cboTahun.getSelectedItem() == null) cboTahun.setSelectedIndex(0);
        cboTahun.setParent(bar);

        new Label(ais.common.Common.getBahasaConfig("  Semester:")).setParent(bar);
        cboSemester = new Combobox();
        cboSemester.setWidth("88px");
        cboSemester.setReadonly(true);
        String[][] sems = {{"Semua", ""}, {"Ganjil", "Ganjil"}, {"Genap", "Genap"}};
        for (String[] sv : sems) {
            Comboitem ci = new Comboitem(sv[0]);
            ci.setValue(sv[1]);
            cboSemester.appendChild(ci);
        }
        cboSemester.setSelectedIndex(0);
        cboSemester.setParent(bar);

        new Label(ais.common.Common.getBahasaConfig("  Jenis:")).setParent(bar);
        cboJenis = new Combobox();
        cboJenis.setWidth("180px");
        cboJenis.setReadonly(true);
        String[][] jenisOpts = {
            {"Semua", ""},
            {"Pembayaran Registrasi", "REG"},
            {"Pembayaran Daftar Ulang", "DU"}
        };
        for (String[] jv : jenisOpts) {
            Comboitem ci = new Comboitem(jv[0]);
            ci.setValue(jv[1]);
            cboJenis.appendChild(ci);
        }
        cboJenis.setSelectedIndex(0);
        cboJenis.setParent(bar);

        Toolbarbutton btnTampilkan = new Toolbarbutton("Tampilkan");
        btnTampilkan.setImage("/img/search.gif");
        btnTampilkan.setParent(bar);
        btnTampilkan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Comboitem ciT = cboTahun.getSelectedItem();
                Object vT = ciT != null ? ciT.getValue() : null;
                filterTahun = (vT instanceof Integer) ? ((Integer) vT).intValue() : 0;
                Comboitem ciS = cboSemester.getSelectedItem();
                Object vS = ciS != null ? ciS.getValue() : null;
                filterSemester = (vS instanceof String) ? (String) vS : "";
                Comboitem ciJ = cboJenis.getSelectedItem();
                Object vJ = ciJ != null ? ciJ.getValue() : null;
                filterJenis = (vJ instanceof String) ? (String) vJ : "";
                muatDasbor();
            }
        });

        // Placeholder untuk tombol Proses — diisi ulang setiap render agar ikut filterTahun
        divBtnProses = new Div();
        divBtnProses.setStyle("display:inline-flex;gap:6px;align-items:center;");
        divBtnProses.setParent(bar);

        divDasborContent = new Div();
        divDasborContent.setStyle("margin:0;padding:0 16px 32px;box-sizing:border-box;");
        divDasborContent.setParent(rootContainer);
    }

    // =========================================================================
    // LOAD DASHBOARD
    // =========================================================================

    /**
     * Memuat ulang seluruh konten dasbor. Method ini membersihkan konten lama,
     * menampilkan indikator loading, menjalankan semua query ke database dalam
     * satu sesi, lalu merender ulang seluruh panel.
     * <p>Sesi Hibernate dibuka dan ditutup di sini — tidak ada sesi yang bocor.</p>
     */
    private void muatDasbor() {
        Common.clear(divDasborContent);
        new Html(
            "<div style='text-align:center;padding:60px 16px;color:#94a3b8;font-size:13px;'>" +
            "<div style='font-size:28px;margin-bottom:8px;'>&#x23F3;</div>" +
            "Memuat data&hellip;</div>"
        ).setParent(divDasborContent);

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            queryData(s);
        } catch (Exception ex) {
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/pmb/DasborKeuanganPmbAction.java:295");
        } finally {
            try { s.clear();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/pmb/DasborKeuanganPmbAction.java:297");}
            try { s.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/pmb/DasborKeuanganPmbAction.java:298");}
            try { s.close();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/pmb/DasborKeuanganPmbAction.java:299");}
        }

        Common.clear(divDasborContent);
        renderDasbor();
    }

    // =========================================================================
    // QUERY DATA
    // =========================================================================

    /**
     * Menjalankan seluruh query agregat ke database dalam satu sesi yang sama.
     * Tiap kelompok query dibungkus try-catch independen sehingga kegagalan
     * satu query tidak menghentikan query lainnya. Hasil disimpan ke field
     * instance agar method render dan Excel dapat memakainya tanpa query ulang.
     *
     * <p><b>Query yang dijalankan:</b></p>
     * <ol>
     *   <li>Total calon mahasiswa (dengan filter tahun/semester/jenis)</li>
     *   <li>Total tagihan &amp; dibayar registrasi</li>
     *   <li>Total tagihan &amp; dibayar daftar ulang</li>
     *   <li>Total lunas daftar ulang</li>
     *   <li>Lulus seleksi (bcm.prodiLulus IS NOT NULL)</li>
     *   <li>Sudah bayar registrasi (reg.dibayar > 0)</li>
     *   <li>Punya tagihan daftar ulang (INNER JOIN du)</li>
     *   <li>Registrasi lunas (reg.lunas = true)</li>
     *   <li>Agregat per tahun masuk — registrasi</li>
     *   <li>Agregat per tahun masuk — daftar ulang</li>
     *   <li>Agregat per program studi</li>
     *   <li>Agregat per gelombang pendaftaran</li>
     * </ol>
     *
     * @param s sesi Hibernate yang sudah dibuka oleh {@link #muatDasbor()}
     */
    private void queryData(Session s) {

        // 1. Total calon
        try {
            Query q = s.createQuery(
                "SELECT COUNT(bcm.id) FROM BiodataCalonMahasiswa bcm" + buildWhere(true));
            applyParams(q, true);
            Number n = (Number) q.uniqueResult();
            totalCalon = n == null ? 0L : n.longValue();
        } catch (Exception e) { totalCalon = 0L; }

        // 2. Tagihan & dibayar registrasi
        long regTag = 0L, regByr = 0L;
        if ("".equals(filterJenis) || "REG".equals(filterJenis)) {
            try {
                Query q = s.createQuery(
                    "SELECT SUM(reg.tagihan), SUM(reg.dibayar) " +
                    "FROM BiodataCalonMahasiswa bcm LEFT JOIN bcm.pembayaranRegistrasi reg" +
                    buildWhere(true));
                applyParams(q, true);
                Object[] row = (Object[]) q.uniqueResult();
                if (row != null) {
                    regTag = row[0] == null ? 0L : ((Number) row[0]).longValue();
                    regByr = row[1] == null ? 0L : ((Number) row[1]).longValue();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/DasborKeuanganPmbAction.java:359"); /* keep 0 */ }
        }

        // 3. Tagihan & dibayar daftar ulang
        long duTag = 0L, duByr = 0L;
        if ("".equals(filterJenis) || "DU".equals(filterJenis)) {
            try {
                Query q = s.createQuery(
                    "SELECT SUM(du.tagihan), SUM(du.dibayar) " +
                    "FROM BiodataCalonMahasiswa bcm LEFT JOIN bcm.pembayaranDaftarUlang du" +
                    buildWhere(true));
                applyParams(q, true);
                Object[] row = (Object[]) q.uniqueResult();
                if (row != null) {
                    duTag = row[0] == null ? 0L : ((Number) row[0]).longValue();
                    duByr = row[1] == null ? 0L : ((Number) row[1]).longValue();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/DasborKeuanganPmbAction.java:376"); /* keep 0 */ }
        }
        totalTagihan = regTag + duTag;
        totalDibayar = regByr + duByr;

        // 4. Lunas daftar ulang
        if (!"REG".equals(filterJenis)) {
            try {
                StringBuilder hql = new StringBuilder(
                    "SELECT COUNT(bcm.id) FROM BiodataCalonMahasiswa bcm " +
                    "JOIN bcm.pembayaranDaftarUlang du WHERE du.lunas = true");
                if (filterTahun > 0) hql.append(" AND bcm.tahun = :tahun");
                if (filterSemester != null && filterSemester.length() > 0)
                    hql.append(" AND bcm.semesterMulai = :semester");
                Query q = s.createQuery(hql.toString());
                applyParams(q, true);
                Number n = (Number) q.uniqueResult();
                totalLunas = n == null ? 0L : n.longValue();
            } catch (Exception e) { totalLunas = 0L; }
        } else {
            totalLunas = 0L;
        }

        // 5. Lulus seleksi (prodiLulus tidak null)
        try {
            StringBuilder hql = new StringBuilder(
                "SELECT COUNT(bcm.id) FROM BiodataCalonMahasiswa bcm WHERE bcm.prodiLulus IS NOT NULL");
            if (filterTahun > 0) hql.append(" AND bcm.tahun = :tahun");
            if (filterSemester != null && filterSemester.length() > 0)
                hql.append(" AND bcm.semesterMulai = :semester");
            Query q = s.createQuery(hql.toString());
            applyParams(q, true);
            Number n = (Number) q.uniqueResult();
            lulusSeleksi = n == null ? 0L : n.longValue();
        } catch (Exception e) { lulusSeleksi = 0L; }

        // 6. Sudah bayar registrasi (dibayar > 0)
        try {
            StringBuilder hql = new StringBuilder(
                "SELECT COUNT(bcm.id) FROM BiodataCalonMahasiswa bcm " +
                "JOIN bcm.pembayaranRegistrasi reg WHERE reg.dibayar > 0");
            if (filterTahun > 0) hql.append(" AND bcm.tahun = :tahun");
            if (filterSemester != null && filterSemester.length() > 0)
                hql.append(" AND bcm.semesterMulai = :semester");
            Query q = s.createQuery(hql.toString());
            applyParams(q, true);
            Number n = (Number) q.uniqueResult();
            totalBayarReg = n == null ? 0L : n.longValue();
        } catch (Exception e) { totalBayarReg = 0L; }

        // 7. Punya tagihan daftar ulang
        try {
            Query q = s.createQuery(
                "SELECT COUNT(bcm.id) FROM BiodataCalonMahasiswa bcm " +
                "JOIN bcm.pembayaranDaftarUlang du" + buildWhere(true));
            applyParams(q, true);
            Number n = (Number) q.uniqueResult();
            totalPunyaDU = n == null ? 0L : n.longValue();
        } catch (Exception e) { totalPunyaDU = 0L; }

        // 8. Registrasi lunas
        try {
            StringBuilder hql = new StringBuilder(
                "SELECT COUNT(bcm.id) FROM BiodataCalonMahasiswa bcm " +
                "JOIN bcm.pembayaranRegistrasi reg WHERE reg.lunas = true");
            if (filterTahun > 0) hql.append(" AND bcm.tahun = :tahun");
            if (filterSemester != null && filterSemester.length() > 0)
                hql.append(" AND bcm.semesterMulai = :semester");
            Query q = s.createQuery(hql.toString());
            applyParams(q, true);
            Number n = (Number) q.uniqueResult();
            totalLunasReg = n == null ? 0L : n.longValue();
        } catch (Exception e) { totalLunasReg = 0L; }

        // 9. Per tahun — registrasi (selalu tampil semua tahun untuk grafik tren)
        try {
            Query q = s.createQuery(
                "SELECT bcm.tahun, COUNT(bcm.id), SUM(reg.tagihan), SUM(reg.dibayar) " +
                "FROM BiodataCalonMahasiswa bcm LEFT JOIN bcm.pembayaranRegistrasi reg" +
                buildWhere(false) +
                " GROUP BY bcm.tahun ORDER BY bcm.tahun DESC");
            applyParams(q, false);
            q.setMaxResults(8);
            dataPerTahun = q.list();
        } catch (Exception e) { dataPerTahun = new ArrayList<Object[]>(); }

        // 10. Per tahun — daftar ulang
        try {
            Query q = s.createQuery(
                "SELECT bcm.tahun, COUNT(bcm.id), SUM(du.tagihan), SUM(du.dibayar) " +
                "FROM BiodataCalonMahasiswa bcm LEFT JOIN bcm.pembayaranDaftarUlang du" +
                buildWhere(false) +
                " GROUP BY bcm.tahun ORDER BY bcm.tahun DESC");
            applyParams(q, false);
            q.setMaxResults(8);
            dataPerTahunDU = q.list();
        } catch (Exception e) { dataPerTahunDU = new ArrayList<Object[]>(); }

        // 11. Per program studi
        try {
            Query q = s.createQuery(
                "SELECT j.nama, COUNT(bcm.id), SUM(reg.tagihan), SUM(reg.dibayar) " +
                "FROM BiodataCalonMahasiswa bcm LEFT JOIN bcm.pembayaranRegistrasi reg " +
                "JOIN bcm.prodiLulus j" + buildWhere(true) +
                " GROUP BY j.nama ORDER BY j.nama");
            applyParams(q, true);
            q.setMaxResults(20);
            dataPerProdi = q.list();
        } catch (Exception e) { dataPerProdi = new ArrayList<Object[]>(); }

        // 12. Per gelombang pendaftaran
        try {
            Query q = s.createQuery(
                "SELECT g.nama, COUNT(bcm.id), SUM(reg.tagihan), SUM(reg.dibayar) " +
                "FROM BiodataCalonMahasiswa bcm LEFT JOIN bcm.pembayaranRegistrasi reg " +
                "JOIN bcm.gelombangPendaftaran g" + buildWhere(true) +
                " GROUP BY g.nama ORDER BY g.nama");
            applyParams(q, true);
            q.setMaxResults(20);
            dataPerGelombang = q.list();
        } catch (Exception e) { dataPerGelombang = new ArrayList<Object[]>(); }
    }

    /**
     * Membangun klausa WHERE HQL berdasarkan filter aktif.
     * @param includeTahun true jika filter tahun harus diikutsertakan
     * @return string " WHERE ..." atau "" jika tidak ada filter
     */
    private String buildWhere(boolean includeTahun) {
        List<String> conds = new ArrayList<String>();
        if (includeTahun && filterTahun > 0) conds.add("bcm.tahun = :tahun");
        if (filterSemester != null && filterSemester.length() > 0)
            conds.add("bcm.semesterMulai = :semester");
        if (conds.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" WHERE ");
        for (int i = 0; i < conds.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(conds.get(i));
        }
        return sb.toString();
    }

    private void applyParams(Query q, boolean includeTahun) {
        if (includeTahun && filterTahun > 0) q.setInteger("tahun", filterTahun);
        if (filterSemester != null && filterSemester.length() > 0)
            q.setString("semester", filterSemester);
    }

    // =========================================================================
    // RENDER DASHBOARD
    // =========================================================================

    /**
     * Merender seluruh panel dasbor secara berurutan setelah data tersedia.
     * Urutan panel dirancang dari ringkasan (atas) ke detail (bawah), mengikuti
     * prinsip information hierarchy — pengguna langsung melihat angka kunci
     * sebelum menyelami grafik dan tabel detail.
     */
    private void renderDasbor() {
        // Rebuild tombol Proses dengan tahun angkatan sesuai filter aktif.
        // Keduanya mulai=sampai=filterTahun dan disabled agar operator tidak perlu mengetik ulang.
        Common.clear(divBtnProses);
        int tahunProses = filterTahun > 0 ? filterTahun : Calendar.getInstance().get(Calendar.YEAR);
        MyToolbarbuttonConfig btnProsesReg = KegiatanProsesHeper.prosesUlangTagihan(
                "Proses Tagihan Registrasi", "/img/save.gif",
                ConstantValues.PENDAFTARAN_CALON_MAHASISWA, true, tahunProses);
        btnProsesReg.setParent(divBtnProses);
        MyToolbarbuttonConfig btnProsesDU = KegiatanProsesHeper.prosesUlangTagihan(
                "Proses Tagihan Daftar Ulang", "/img/save.gif",
                ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, true, tahunProses);
        btnProsesDU.setParent(divBtnProses);

        String filterLabel;
        if (filterTahun > 0 && filterSemester != null && filterSemester.length() > 0)
            filterLabel = "Tahun " + filterTahun + " &bull; Semester " + filterSemester;
        else if (filterTahun > 0)
            filterLabel = "Tahun " + filterTahun;
        else if (filterSemester != null && filterSemester.length() > 0)
            filterLabel = "Semester " + filterSemester;
        else
            filterLabel = "Semua Tahun";
        if ("REG".equals(filterJenis)) filterLabel += " &bull; Registrasi";
        else if ("DU".equals(filterJenis)) filterLabel += " &bull; Daftar Ulang";
        String updated = new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date());

        new Html(
            "<div class='dgk-filter-info'>Menampilkan: <b>" + filterLabel + "</b>" +
            " &nbsp;&bull;&nbsp; Diperbarui: " + updated + "</div>"
        ).setParent(divDasborContent);

        // 1. 6 Kartu ringkasan
        new Html(buildKartuRingkasan()).setParent(divDasborContent);

        // 2. Donut + Progress
        new Html(buildDuaKolom()).setParent(divDasborContent);

        // 3. Funnel + Radar
        new Html(buildFunnelDanRadar()).setParent(divDasborContent);

        // 4. Per prodi
        new Html(buildGrafikHorizontalBatang(
            "Tagihan per Program Studi",
            "Perbandingan jumlah calon dan pembayaran registrasi di tiap program studi.",
            dataPerProdi, "linear-gradient(90deg,#6366f1,#a5b4fc)"
        )).setParent(divDasborContent);

        // 5. Per gelombang
        new Html(buildGrafikHorizontalBatang(
            "Tagihan per Gelombang Pendaftaran",
            "Perbandingan calon dan pembayaran berdasarkan gelombang penerimaan.",
            dataPerGelombang, "linear-gradient(90deg,#f59e0b,#fde68a)"
        )).setParent(divDasborContent);

        // 6. Tren registrasi per tahun
        new Html(buildGrafikPerTahunGeneric(
            "Tren Tagihan Registrasi per Angkatan",
            "Perkembangan tagihan dan pembayaran registrasi dari tahun ke tahun.",
            dataPerTahun,
            "linear-gradient(90deg,#1d4ed8,#60a5fa)",
            "Tagihan Reg", "Dibayar Reg"
        )).setParent(divDasborContent);

        // 7. Tren daftar ulang per tahun
        new Html(buildGrafikPerTahunGeneric(
            "Tren Tagihan Daftar Ulang per Angkatan",
            "Perkembangan tagihan dan pembayaran daftar ulang dari tahun ke tahun.",
            dataPerTahunDU,
            "linear-gradient(90deg,#16a34a,#4ade80)",
            "Tagihan DU", "Dibayar DU"
        )).setParent(divDasborContent);

        // 8. Tabel rekap + Excel
        buildTabelRekap();
    }

    // =========================================================================
    // HTML BUILDERS — KARTU RINGKASAN
    // =========================================================================

    /**
     * Membangun baris 6 kartu ringkasan numerik. Keenam angka dipilih agar
     * admin keuangan langsung mengetahui kondisi terkini tanpa perlu membuka
     * laporan lain. Warna kartu menggunakan kode: biru = netral, ungu = uang,
     * hijau = positif, merah = perlu perhatian, oranye = peringatan.
     */
    private String buildKartuRingkasan() {
        long sisa           = Math.max(0L, totalTagihan - totalDibayar);
        long belumBayarReg  = Math.max(0L, totalCalon - totalBayarReg);
        int  pctDibayar     = totalTagihan > 0
            ? (int) Math.round(totalDibayar * 100.0 / totalTagihan) : 0;
        String tagihanSub   = "REG".equals(filterJenis) ? "Tagihan registrasi"
            : "DU".equals(filterJenis) ? "Tagihan daftar ulang"
            : "Tagihan registrasi &amp; daftar ulang";

        return "<div class='dgk-cards'>" +
            kartu("#1e40af", "#dbeafe", "&#x1F465;",
                "Total Calon Mahasiswa", fmt(totalCalon),
                "Semua calon yang terdaftar") +
            kartu("#5b21b6", "#ede9fe", "&#x1F4B0;",
                "Total Tagihan", "Rp " + fmt(totalTagihan), tagihanSub) +
            kartu("#065f46", "#d1fae5", "&#x2705;",
                "Sudah Dibayar", "Rp " + fmt(totalDibayar),
                pctDibayar + "% dari total tagihan") +
            kartu("#7f1d1d", "#fee2e2", "&#x23F3;",
                "Sisa Belum Lunas", "Rp " + fmt(sisa),
                "Masih perlu dilunasi") +
            kartu("#14532d", "#dcfce7", "&#x1F3C6;",
                "Lunas Daftar Ulang", fmt(totalLunas),
                "Sudah selesaikan semua kewajiban") +
            kartu("#78350f", "#fef3c7", "&#x26A0;",
                "Belum Bayar Registrasi", fmt(belumBayarReg),
                "Perlu segera ditindaklanjuti") +
            "</div>";
    }

    /**
     * Membangun HTML satu kartu ringkasan.
     * Method ini dipanggil berulang oleh {@link #buildKartuRingkasan()}.
     *
     * @param warna  warna aksen (border atas + ikon)
     * @param bg     warna latar ikon
     * @param ikon   emoji atau karakter Unicode untuk ikon
     * @param lbl    label judul kartu (huruf kapital kecil)
     * @param val    nilai utama yang ditampilkan besar
     * @param sub    keterangan singkat di bawah nilai
     * @return HTML string satu kartu
     */
    private String kartu(String warna, String bg, String ikon,
                         String lbl, String val, String sub) {
        return "<div class='dgk-card' style='border-top:3px solid " + warna + ";'>" +
            "<div class='dgk-card-ico' style='background:" + bg + ";color:" + warna + ";'>"
                + ikon + "</div>" +
            "<div class='dgk-card-lbl'>" + esc(lbl) + "</div>" +
            "<div class='dgk-card-val'>" + esc(val) + "</div>" +
            "<div class='dgk-card-sub'>" + sub + "</div>" +
            "</div>";
    }

    // =========================================================================
    // HTML BUILDERS — DUA KOLOM (DONUT + PROGRESS)
    // =========================================================================

    private String buildDuaKolom() {
        return "<div class='dgk-2col'>" + buildDonut() + buildProgressPanel() + "</div>";
    }

    private String buildDonut() {
        long belum = Math.max(0L, totalCalon - totalLunas);
        int pct = totalCalon > 0
            ? (int) Math.round(totalLunas * 100.0 / totalCalon) : 0;
        return "<div class='dgk-sect'>" +
            "<div class='dgk-sect-title'>Status Pelunasan</div>" +
            "<div class='dgk-sect-desc'>Dari " + fmt(totalCalon) + " calon, berapa yang sudah lunas daftar ulang.</div>" +
            "<div style='display:flex;align-items:center;justify-content:center;gap:24px;" +
            "flex-wrap:wrap;padding:12px 0;'>" +
            "<div style='width:130px;height:130px;border-radius:50%;" +
            "background:conic-gradient(#16a34a 0% " + pct + "%,#e2e8f0 " + pct + "% 100%);" +
            "position:relative;box-shadow:0 4px 12px rgba(0,0,0,.12);flex:0 0 130px;'>" +
            "<div style='position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);" +
            "width:86px;height:86px;border-radius:50%;background:#fff;" +
            "display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;'>" +
            "<div style='font-size:22px;font-weight:900;color:#1e293b;'>" + pct + "%</div>" +
            "<div style='font-size:9px;font-weight:700;color:#64748b;letter-spacing:.5px;'>LUNAS</div>" +
            "</div></div>" +
            "<div style='display:flex;flex-direction:column;gap:12px;'>" +
            legendItem("#16a34a", "Sudah Lunas", fmt(totalLunas)) +
            legendItem("#cbd5e1", "Belum Lunas", fmt(belum)) +
            "</div></div></div>";
    }

    /**
     * Membangun satu item legenda (titik warna + label + nilai).
     * Dipakai ulang oleh donut chart.
     */
    private String legendItem(String color, String label, String val) {
        return "<div style='display:flex;align-items:center;gap:8px;'>" +
            "<div style='width:13px;height:13px;border-radius:50%;background:" + color +
            ";flex:0 0 13px;'></div>" +
            "<div><div style='font-weight:700;font-size:13px;color:#1e293b;'>" + val + "</div>" +
            "<div style='font-size:11px;color:#64748b;'>" + esc(label) + "</div></div></div>";
    }

    private String buildProgressPanel() {
        long sisa = Math.max(0L, totalTagihan - totalDibayar);
        int pct = totalTagihan > 0
            ? (int) Math.round(totalDibayar * 100.0 / totalTagihan) : 0;
        String color = pct >= 80 ? "#16a34a" : pct >= 50 ? "#f59e0b" : "#ef4444";
        return "<div class='dgk-sect'>" +
            "<div class='dgk-sect-title'>Realisasi Pembayaran</div>" +
            "<div class='dgk-sect-desc'>Dari total tagihan, sudah berapa persen yang masuk ke kas.</div>" +
            "<div style='padding:12px 0 4px;'>" +
            "<div style='font-size:38px;font-weight:900;color:" + color +
            ";line-height:1;'>" + pct + "%</div>" +
            "<div style='font-size:11px;color:#64748b;margin:4px 0 12px;'>tagihan sudah terbayar</div>" +
            "<div style='height:14px;background:#f1f5f9;border-radius:7px;" +
            "overflow:hidden;margin-bottom:16px;box-shadow:inset 0 1px 3px rgba(0,0,0,.08);'>" +
            "<div style='height:100%;width:" + pct + "%;background:linear-gradient(90deg," +
            color + ",#86efac);border-radius:7px;'></div></div>" +
            "<div style='display:grid;grid-template-columns:1fr 1fr;gap:10px;'>" +
            "<div style='background:#f0fdf4;border-radius:10px;padding:12px;'>" +
            "<div style='font-size:9px;font-weight:700;color:#16a34a;text-transform:uppercase;" +
            "letter-spacing:.5px;margin-bottom:3px;'>Sudah Dibayar</div>" +
            "<div style='font-size:13px;font-weight:800;color:#14532d;word-break:break-all;'>" +
            "Rp " + fmt(totalDibayar) + "</div></div>" +
            "<div style='background:#fef2f2;border-radius:10px;padding:12px;'>" +
            "<div style='font-size:9px;font-weight:700;color:#ef4444;text-transform:uppercase;" +
            "letter-spacing:.5px;margin-bottom:3px;'>Masih Sisa</div>" +
            "<div style='font-size:13px;font-weight:800;color:#7f1d1d;word-break:break-all;'>" +
            "Rp " + fmt(sisa) + "</div></div>" +
            "</div></div></div>";
    }

    // =========================================================================
    // HTML BUILDERS — FUNNEL + RADAR
    // =========================================================================

    private String buildFunnelDanRadar() {
        return "<div class='dgk-2col'>" + buildFunnel() + buildRadarChart() + "</div>";
    }

    /**
     * Membangun panel funnel (corong) 5 tahap yang menggambarkan perjalanan
     * calon mahasiswa dari mendaftar hingga lunas. Lebar batang setiap tahap
     * proporsional terhadap total pendaftar, sehingga langsung terlihat secara
     * visual di tahap mana banyak calon berhenti.
     */
    private String buildFunnel() {
        long[]   counts = {totalCalon, lulusSeleksi, totalBayarReg, totalPunyaDU, totalLunas};
        String[] labels = {
            "Mendaftar", "Lulus Seleksi",
            "Bayar Registrasi", "Ada Tagihan DU", "Lunas Daftar Ulang"
        };
        String[] colors = {"#3b82f6", "#8b5cf6", "#f59e0b", "#10b981", "#16a34a"};

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='dgk-sect'>");
        sb.append("<div class='dgk-sect-title'>Perjalanan Pendaftaran</div>");
        sb.append("<div class='dgk-sect-desc'>Dari yang mendaftar, berapa yang sampai lunas.</div>");
        sb.append("<div style='padding:6px 0 2px;'>");
        for (int i = 0; i < counts.length; i++) {
            sb.append(buildFunnelStep(i + 1, labels[i], counts[i],
                totalCalon, colors[i], i == counts.length - 1));
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    /**
     * Membangun satu tahap funnel. Dapat dipakai ulang untuk tahap manapun
     * dengan parameter yang berbeda-beda.
     *
     * @param nomor   nomor urut tahap (1–5)
     * @param label   nama tahap yang ditampilkan
     * @param jumlah  jumlah calon di tahap ini
     * @param total   total pendaftar (basis 100%)
     * @param warna   warna CSS aksen tahap ini
     * @param isLast  true jika ini tahap terakhir (tidak ada anak panah bawah)
     * @return HTML string satu baris tahap funnel
     */
    private String buildFunnelStep(int nomor, String label, long jumlah,
                                   long total, String warna, boolean isLast) {
        int pct = total > 0 ? (int) Math.round(jumlah * 100.0 / total) : 0;
        return "<div style='margin-bottom:" + (isLast ? "0" : "10") + "px;'>" +
            "<div style='display:flex;justify-content:space-between;align-items:center;" +
            "margin-bottom:5px;gap:8px;'>" +
            "<div style='font-size:12px;font-weight:700;color:#334155;display:flex;" +
            "align-items:center;gap:7px;min-width:0;'>" +
            "<span style='background:" + warna + ";color:#fff;border-radius:50%;" +
            "min-width:20px;width:20px;height:20px;display:inline-flex;align-items:center;" +
            "justify-content:center;font-size:10px;flex:0 0 20px;font-weight:800;'>"
                + nomor + "</span>" +
            "<span style='white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>"
                + esc(label) + "</span></div>" +
            "<div style='font-size:13px;font-weight:900;color:" + warna +
            ";white-space:nowrap;'>" + fmt(jumlah) +
            "<span style='font-size:10px;color:#94a3b8;font-weight:400;margin-left:3px;'>" +
            "(" + pct + "%)</span></div></div>" +
            "<div style='height:8px;background:#f1f5f9;border-radius:4px;overflow:hidden;'>" +
            "<div style='height:100%;width:" + pct + "%;background:" + warna +
            ";border-radius:4px;'></div></div>" +
            (isLast ? ""
                : "<div style='text-align:center;font-size:11px;color:#cbd5e1;" +
                  "line-height:1.2;padding:2px 0;'>&#x25BC;</div>") +
            "</div>";
    }

    /**
     * Membangun grafik radar (spider web) SVG 5 sumbu yang memperlihatkan
     * kesehatan keuangan PMB secara menyeluruh dalam satu tampilan kompak.
     * Sumbu-sumbunya: % tagihan terbayar, % bayar registrasi, % ada DU,
     * % lunas DU, dan % registrasi lunas. Semakin lebar area radar, semakin
     * baik kondisi keuangan. Tidak menggunakan library chart eksternal —
     * murni SVG yang di-generate dari Java.
     *
     * @return HTML string berisi panel + SVG radar chart
     */
    private String buildRadarChart() {
        // Hitung 5 nilai persen (0–100) untuk 5 sumbu
        int v0 = totalTagihan > 0 ? (int) Math.round(totalDibayar  * 100.0 / totalTagihan) : 0;
        int v1 = totalCalon   > 0 ? (int) Math.round(totalBayarReg * 100.0 / totalCalon)   : 0;
        int v2 = totalCalon   > 0 ? (int) Math.round(totalPunyaDU  * 100.0 / totalCalon)   : 0;
        int v3 = totalCalon   > 0 ? (int) Math.round(totalLunas    * 100.0 / totalCalon)   : 0;
        int v4 = totalCalon   > 0 ? (int) Math.round(totalLunasReg * 100.0 / totalCalon)   : 0;
        int[] vals = {v0, v1, v2, v3, v4};

        // Koordinat SVG: cx=210, cy=155, r=100
        // 5 sumbu clockwise dari atas (0°, 72°, 144°, 216°, 288°)
        int cx = 210, cy = 155, rr = 100;
        int[] degs = {0, 72, 144, 216, 288};
        StringBuilder dataPts = new StringBuilder();
        StringBuilder dots    = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            double ang = Math.toRadians(degs[i]);
            int px = cx + (int) Math.round(rr * (vals[i] / 100.0) * Math.sin(ang));
            int py = cy - (int) Math.round(rr * (vals[i] / 100.0) * Math.cos(ang));
            if (i > 0) dataPts.append(" ");
            dataPts.append(px).append(",").append(py);
            dots.append("<circle cx='").append(px).append("' cy='").append(py)
                .append("' r='4' fill='#3b82f6' stroke='#fff' stroke-width='1.5'/>");
        }

        // Grid ring 100%, 75%, 50%, 25% — precomputed (cx=210,cy=155,r=100)
        String g100 = "210,55 305,124 269,236 151,236 115,124";
        String g075 = "210,80 281,132 254,216 166,216 139,132";
        String g050 = "210,105 238,140 239,195 181,195 162,140";
        String g025 = "210,130 234,147 225,175 195,175 186,147";
        // Sumbu dari center ke ujung
        int[][] outer = {{210,55},{305,124},{269,236},{151,236},{115,124}};

        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox='0 0 420 285' xmlns='http://www.w3.org/2000/svg' " +
            "style='width:100%;height:auto;display:block;'>");

        // Grid rings
        String[] gs = {g025, g050, g075, g100};
        for (int i = 0; i < gs.length; i++) {
            svg.append("<polygon points='").append(gs[i])
               .append("' fill='").append(i == 3 ? "#f8fafc" : "none")
               .append("' stroke='#cbd5e1' stroke-width='0.7' stroke-dasharray='3,3'/>");
        }
        // Sumbu
        for (int[] pt : outer) {
            svg.append("<line x1='210' y1='155' x2='").append(pt[0])
               .append("' y2='").append(pt[1])
               .append("' stroke='#e2e8f0' stroke-width='0.8'/>");
        }
        // Data polygon
        svg.append("<polygon points='").append(dataPts)
           .append("' fill='rgba(59,130,246,0.18)' stroke='#3b82f6' " +
                   "stroke-width='2' stroke-linejoin='round'/>");
        svg.append(dots);

        // Label sumbu + nilai
        String[][] lbls = {
            {"210","14","middle","% Tagihan Terbayar",String.valueOf(v0)+"%"},
            {"348","120","start", "% Bayar Reg",       String.valueOf(v1)+"%"},
            {"278","268","middle","% Ada DU",           String.valueOf(v2)+"%"},
            {"142","268","middle","% Lunas DU",         String.valueOf(v3)+"%"},
            {"72", "120","end",   "% Lunas Reg",        String.valueOf(v4)+"%"}
        };
        for (String[] lb : lbls) {
            svg.append("<text x='").append(lb[0]).append("' y='").append(lb[1])
               .append("' text-anchor='").append(lb[2])
               .append("' font-size='9' fill='#475569' font-weight='600'>")
               .append(lb[3]).append("</text>");
            int yn = Integer.parseInt(lb[1]) + 11;
            svg.append("<text x='").append(lb[0]).append("' y='").append(yn)
               .append("' text-anchor='").append(lb[2])
               .append("' font-size='9' fill='#3b82f6' font-weight='800'>")
               .append(lb[4]).append("</text>");
        }
        // Label ring persentase
        svg.append("<text x='215' y='58' font-size='7' fill='#94a3b8'>100%</text>");
        svg.append("<text x='215' y='83' font-size='7' fill='#94a3b8'>75%</text>");
        svg.append("<text x='215' y='108' font-size='7' fill='#94a3b8'>50%</text>");
        svg.append("<text x='215' y='133' font-size='7' fill='#94a3b8'>25%</text>");
        svg.append("</svg>");

        return "<div class='dgk-sect'>" +
            "<div class='dgk-sect-title'>Radar Kesehatan Keuangan</div>" +
            "<div class='dgk-sect-desc'>Lima ukuran keuangan PMB sekaligus — makin lebar makin baik.</div>" +
            "<div style='padding:4px 0;'>" + svg.toString() + "</div></div>";
    }

    // =========================================================================
    // HTML BUILDERS — GRAFIK HORIZONTAL (REUSABLE)
    // =========================================================================

    /**
     * Membangun panel grafik batang horizontal yang dapat dipakai ulang untuk
     * berbagai kelompok data (per prodi, per gelombang, dll.).
     *
     * <p>Format data: setiap {@code Object[]} berisi
     * [0]=nama (String), [1]=jumlah calon (Number),
     * [2]=total tagihan registrasi (Number), [3]=sudah dibayar (Number).</p>
     *
     * @param title        judul panel
     * @param desc         deskripsi singkat untuk end user
     * @param data         list baris data
     * @param colorDibayar CSS color/gradient untuk bar "dibayar"
     * @return HTML string panel lengkap
     */
    private String buildGrafikHorizontalBatang(String title, String desc,
                                               List<Object[]> data,
                                               String colorDibayar) {
        if (data == null || data.isEmpty()) {
            return "<div class='dgk-sect'><div class='dgk-sect-title'>" + esc(title) + "</div>" +
                "<div style='padding:24px;text-align:center;color:#94a3b8;font-size:13px;'>" +
                "Tidak ada data.</div></div>";
        }
        long maxT = 1L;
        for (Object[] r : data) {
            long t = r[2] == null ? 0L : ((Number) r[2]).longValue();
            if (t > maxT) maxT = t;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='dgk-sect'>");
        sb.append("<div class='dgk-sect-title'>").append(esc(title)).append("</div>");
        sb.append("<div class='dgk-sect-desc'>").append(esc(desc)).append("</div>");
        sb.append("<div style='padding:6px 0;'>");
        for (Object[] r : data) {
            String nama = r[0] == null ? "-" : r[0].toString();
            long   jml  = r[1] == null ? 0L : ((Number) r[1]).longValue();
            long   tag  = r[2] == null ? 0L : ((Number) r[2]).longValue();
            long   byr  = r[3] == null ? 0L : ((Number) r[3]).longValue();
            sb.append(buildBarHorizontalRow(nama, jml, tag, byr, maxT, colorDibayar));
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    /**
     * Membangun satu baris batang horizontal (nama + dua bar tagihan/dibayar + info).
     * Method ini dapat dipakai ulang untuk semua jenis grafik horizontal.
     *
     * @param nama          nama kelompok (prodi, gelombang, dll.)
     * @param jumlahCalon   jumlah calon di kelompok ini
     * @param tagihan       total tagihan kelompok ini
     * @param dibayar       total sudah dibayar kelompok ini
     * @param maxTagihan    tagihan terbesar (untuk normalisasi lebar bar)
     * @param colorDibayar  CSS color/gradient bar "dibayar"
     * @return HTML string satu baris
     */
    private String buildBarHorizontalRow(String nama, long jumlahCalon,
                                         long tagihan, long dibayar,
                                         long maxTagihan, String colorDibayar) {
        int wT  = maxTagihan > 0 ? (int) Math.round(tagihan * 100.0 / maxTagihan)  : 0;
        int wB  = maxTagihan > 0 ? (int) Math.round(dibayar * 100.0 / maxTagihan)  : 0;
        int pct = tagihan    > 0 ? (int) Math.round(dibayar * 100.0 / tagihan)     : 0;
        String pctColor = pct >= 80 ? "#16a34a" : pct >= 50 ? "#f59e0b" : "#ef4444";
        return "<div style='margin-bottom:14px;'>" +
            "<div style='display:flex;justify-content:space-between;margin-bottom:3px;" +
            "gap:8px;align-items:baseline;'>" +
            "<span style='font-size:12px;font-weight:700;color:#1e293b;" +
            "min-width:0;word-break:break-word;'>" + esc(nama) + "</span>" +
            "<span style='font-size:10px;color:#64748b;white-space:nowrap;flex-shrink:0;'>" +
            fmt(jumlahCalon) + " calon &bull; " +
            "<span style='color:" + pctColor + ";font-weight:700;'>" + pct + "% terbayar</span>" +
            "</span></div>" +
            "<div style='height:7px;background:#e2e8f0;border-radius:4px;overflow:hidden;" +
            "margin-bottom:3px;'>" +
            "<div style='height:100%;width:" + wT + "%;background:#94a3b8;" +
            "border-radius:4px;'></div></div>" +
            "<div style='height:7px;background:#e2e8f0;border-radius:4px;overflow:hidden;'>" +
            "<div style='height:100%;width:" + wB + "%;background:" + colorDibayar +
            ";border-radius:4px;'></div></div>" +
            "<div style='display:flex;gap:12px;margin-top:3px;'>" +
            "<span style='font-size:10px;color:#64748b;'>&#x25A0; Tagihan: Rp " +
            fmt(tagihan) + "</span>" +
            "<span style='font-size:10px;color:#1d4ed8;'>&#x25A0; Dibayar: Rp " +
            fmt(dibayar) + "</span>" +
            "</div></div>";
    }

    // =========================================================================
    // HTML BUILDERS — GRAFIK BATANG PER TAHUN (REUSABLE)
    // =========================================================================

    /**
     * Membangun panel grafik batang ganda per angkatan tahun.
     * Dipakai untuk registrasi (biru) maupun daftar ulang (hijau) hanya
     * dengan mengganti parameter warna dan label — tanpa duplikasi kode.
     *
     * @param title          judul panel
     * @param desc           deskripsi end user
     * @param data           list [tahun, jumlahCalon, tagihan, dibayar]
     * @param colorDibayar   gradient CSS untuk bar "dibayar"
     * @param labelTagihan   teks legenda bar tagihan
     * @param labelDibayar   teks legenda bar dibayar
     * @return HTML string panel grafik per tahun
     */
    private String buildGrafikPerTahunGeneric(String title, String desc,
                                              List<Object[]> data,
                                              String colorDibayar,
                                              String labelTagihan,
                                              String labelDibayar) {
        if (data == null || data.isEmpty()) {
            return "<div class='dgk-sect'><div class='dgk-sect-title'>" + esc(title) + "</div>" +
                "<div style='padding:24px;text-align:center;color:#94a3b8;font-size:13px;'>" +
                "Tidak ada data tersedia.</div></div>";
        }
        long maxT = 1L;
        for (Object[] r : data) {
            long t = r[2] == null ? 0L : ((Number) r[2]).longValue();
            if (t > maxT) maxT = t;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='dgk-sect'>");
        sb.append("<div class='dgk-sect-title'>").append(esc(title)).append("</div>");
        sb.append("<div class='dgk-sect-desc'>").append(esc(desc)).append("</div>");
        sb.append("<div style='padding:8px 0;'>");
        for (Object[] r : data) {
            int  thn = r[0] == null ? 0  : ((Number) r[0]).intValue();
            long jml = r[1] == null ? 0L : ((Number) r[1]).longValue();
            long tag = r[2] == null ? 0L : ((Number) r[2]).longValue();
            long byr = r[3] == null ? 0L : ((Number) r[3]).longValue();
            int  wT  = (int) Math.round(tag * 100.0 / maxT);
            int  wB  = maxT > 0 ? (int) Math.round(byr * 100.0 / maxT) : 0;
            int  pct = tag  > 0 ? (int) Math.round(byr * 100.0 / tag)  : 0;
            sb.append("<div style='margin-bottom:16px;'>");
            sb.append("<div style='display:flex;justify-content:space-between;margin-bottom:4px;'>");
            sb.append("<span style='font-size:13px;font-weight:700;color:#1e293b;'>")
              .append(thn).append("</span>");
            sb.append("<span style='font-size:11px;color:#64748b;'>")
              .append(fmt(jml)).append(" calon &bull; ").append(pct).append("% terbayar</span>");
            sb.append("</div>");
            sb.append("<div style='height:9px;background:#e2e8f0;border-radius:5px;" +
                "overflow:hidden;margin-bottom:3px;'>");
            sb.append("<div style='height:100%;width:").append(wT)
              .append("%;background:#94a3b8;border-radius:5px;'></div></div>");
            sb.append("<div style='height:9px;background:#e2e8f0;border-radius:5px;overflow:hidden;'>");
            sb.append("<div style='height:100%;width:").append(wB)
              .append("%;background:").append(colorDibayar)
              .append(";border-radius:5px;'></div></div>");
            sb.append("<div style='display:flex;gap:14px;margin-top:4px;'>");
            sb.append("<span style='font-size:10px;color:#64748b;'>&#x25A0; ")
              .append(esc(labelTagihan)).append(": Rp ").append(fmt(tag)).append("</span>");
            sb.append("<span style='font-size:10px;color:#1d4ed8;'>&#x25A0; ")
              .append(esc(labelDibayar)).append(": Rp ").append(fmt(byr)).append("</span>");
            sb.append("</div></div>");
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    // =========================================================================
    // TABEL REKAP + EXCEL DOWNLOAD
    // =========================================================================

    private void buildTabelRekap() {
        Div sect = new Div();
        sect.setSclass("dgk-sect");
        sect.setParent(divDasborContent);

        Div hdr = new Div();
        hdr.setStyle("display:flex;justify-content:space-between;align-items:flex-start;" +
            "flex-wrap:wrap;gap:8px;margin-bottom:12px;");
        hdr.setParent(sect);
        new Html(
            "<div><div class='dgk-sect-title'>Rekap per Angkatan</div>" +
            "<div class='dgk-sect-desc'>Rincian tagihan tiap angkatan — bisa diunduh ke Excel.</div></div>"
        ).setParent(hdr);

        Toolbarbutton btnXls = new Toolbarbutton("Download Excel");
        btnXls.setImage("/img/xls.gif");
        btnXls.setParent(hdr);
        btnXls.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                downloadExcel();
            }
        });

        Grid grid = new Grid();
        grid.setWidth("100%");
        grid.setSclass("dgrid");
        grid.setParent(sect);

        Columns cols = new Columns();
        cols.setParent(grid);
        String[] headers = {"Tahun","Jml Calon","Total Tagihan Reg","Sudah Dibayar","Sisa Belum Lunas","% Terbayar"};
        String[] widths  = {"68px","80px","160px","150px","155px","90px"};
        for (int i = 0; i < headers.length; i++) {
            Column c = new Column(headers[i]);
            c.setWidth(widths[i]);
            c.setParent(cols);
        }

        Rows rows = new Rows();
        rows.setParent(grid);

        if (dataPerTahun == null || dataPerTahun.isEmpty()) {
            Row r = new Row();
            r.setSpans("6");
            r.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data")));
            r.setParent(rows);
        } else {
            long totCln = 0L, totTag = 0L, totByr = 0L;
            for (Object[] rec : dataPerTahun) {
                int  thn  = rec[0] == null ? 0  : ((Number) rec[0]).intValue();
                long jml  = rec[1] == null ? 0L : ((Number) rec[1]).longValue();
                long tag  = rec[2] == null ? 0L : ((Number) rec[2]).longValue();
                long byr  = rec[3] == null ? 0L : ((Number) rec[3]).longValue();
                long sisa = Math.max(0L, tag - byr);
                int  pct  = tag > 0 ? (int) Math.round(byr * 100.0 / tag) : 0;
                String pc = pct >= 80 ? "#16a34a" : pct >= 50 ? "#f59e0b" : "#ef4444";
                totCln += jml; totTag += tag; totByr += byr;
                Row r = new Row();
                r.setParent(rows);
                r.appendChild(new Label(String.valueOf(thn)));
                r.appendChild(new Label(fmt(jml)));
                r.appendChild(new Label("Rp " + fmt(tag)));
                r.appendChild(new Label("Rp " + fmt(byr)));
                r.appendChild(new Label("Rp " + fmt(sisa)));
                r.appendChild(new Html(
                    "<div style='display:flex;align-items:center;gap:5px;'>" +
                    "<div style='flex:1;height:7px;background:#f1f5f9;border-radius:4px;overflow:hidden;'>" +
                    "<div style='height:100%;width:" + pct + "%;background:" + pc + ";border-radius:4px;'>" +
                    "</div></div>" +
                    "<span style='font-size:11px;font-weight:700;color:" + pc +
                    ";min-width:30px;text-align:right;'>" + pct + "%</span></div>"
                ));
            }
            long totSisa = Math.max(0L, totTag - totByr);
            int  totPct  = totTag > 0 ? (int) Math.round(totByr * 100.0 / totTag) : 0;
            Row rFoot = new Row();
            rFoot.setStyle("font-weight:bold;background:#f8fafc;border-top:2px solid #e2e8f0;");
            rFoot.setParent(rows);
            rFoot.appendChild(new Label(ais.common.Common.getBahasaConfig("TOTAL")));
            rFoot.appendChild(new Label(fmt(totCln)));
            rFoot.appendChild(new Label("Rp " + fmt(totTag)));
            rFoot.appendChild(new Label("Rp " + fmt(totByr)));
            rFoot.appendChild(new Label("Rp " + fmt(totSisa)));
            rFoot.appendChild(new Label(totPct + "%"));
        }
    }

    // =========================================================================
    // EXCEL DOWNLOAD (2 sheet: ringkasan per tahun + per prodi)
    // =========================================================================

    /**
     * Menghasilkan file Excel dua lembar:
     * <ul>
     *   <li>Sheet 1 "Rekap per Tahun" — data {@link #dataPerTahun}</li>
     *   <li>Sheet 2 "Per Program Studi" — data {@link #dataPerProdi}</li>
     * </ul>
     * Menggunakan Apache POI XSSF. Jika POI tidak tersedia di classpath,
     * method ini menangkap exception tanpa crash.
     */
    private void downloadExcel() {
        try {
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
                new org.apache.poi.xssf.usermodel.XSSFWorkbook();

            // Sheet 1 — per tahun
            org.apache.poi.ss.usermodel.Sheet sh1 = wb.createSheet("Rekap per Tahun");
            String[] h1 = {"Tahun","Jml Calon","Total Tagihan Reg","Sudah Dibayar","Sisa","% Terbayar"};
            org.apache.poi.ss.usermodel.Row hr1 = sh1.createRow(0);
            for (int i = 0; i < h1.length; i++) hr1.createCell(i).setCellValue(h1[i]);
            int rn = 1;
            long tc = 0L, tt = 0L, tb = 0L;
            if (dataPerTahun != null) {
                for (Object[] rec : dataPerTahun) {
                    int  thn  = rec[0] == null ? 0  : ((Number) rec[0]).intValue();
                    long jml  = rec[1] == null ? 0L : ((Number) rec[1]).longValue();
                    long tag  = rec[2] == null ? 0L : ((Number) rec[2]).longValue();
                    long byr  = rec[3] == null ? 0L : ((Number) rec[3]).longValue();
                    tc += jml; tt += tag; tb += byr;
                    org.apache.poi.ss.usermodel.Row row = sh1.createRow(rn++);
                    row.createCell(0).setCellValue(thn);
                    row.createCell(1).setCellValue(jml);
                    row.createCell(2).setCellValue(tag);
                    row.createCell(3).setCellValue(byr);
                    row.createCell(4).setCellValue(Math.max(0L, tag - byr));
                    row.createCell(5).setCellValue(tag > 0 ? (int) Math.round(byr * 100.0 / tag) + "%" : "0%");
                }
            }
            org.apache.poi.ss.usermodel.Row rTot = sh1.createRow(rn);
            rTot.createCell(0).setCellValue("TOTAL");
            rTot.createCell(1).setCellValue(tc);
            rTot.createCell(2).setCellValue(tt);
            rTot.createCell(3).setCellValue(tb);
            rTot.createCell(4).setCellValue(Math.max(0L, tt - tb));
            rTot.createCell(5).setCellValue(tt > 0 ? (int) Math.round(tb * 100.0 / tt) + "%" : "0%");
            for (int i = 0; i < h1.length; i++) sh1.autoSizeColumn(i);

            // Sheet 2 — per prodi
            org.apache.poi.ss.usermodel.Sheet sh2 = wb.createSheet("Per Program Studi");
            String[] h2 = {"Program Studi","Jml Calon","Total Tagihan Reg","Sudah Dibayar","Sisa","% Terbayar"};
            org.apache.poi.ss.usermodel.Row hr2 = sh2.createRow(0);
            for (int i = 0; i < h2.length; i++) hr2.createCell(i).setCellValue(h2[i]);
            int rn2 = 1;
            if (dataPerProdi != null) {
                for (Object[] rec : dataPerProdi) {
                    String nm  = rec[0] == null ? "-" : rec[0].toString();
                    long   jml = rec[1] == null ? 0L : ((Number) rec[1]).longValue();
                    long   tag = rec[2] == null ? 0L : ((Number) rec[2]).longValue();
                    long   byr = rec[3] == null ? 0L : ((Number) rec[3]).longValue();
                    org.apache.poi.ss.usermodel.Row row = sh2.createRow(rn2++);
                    row.createCell(0).setCellValue(nm);
                    row.createCell(1).setCellValue(jml);
                    row.createCell(2).setCellValue(tag);
                    row.createCell(3).setCellValue(byr);
                    row.createCell(4).setCellValue(Math.max(0L, tag - byr));
                    row.createCell(5).setCellValue(tag > 0 ? (int) Math.round(byr * 100.0 / tag) + "%" : "0%");
                }
            }
            for (int i = 0; i < h2.length; i++) sh2.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            baos.close();
            Filedownload.save(
                new ByteArrayInputStream(baos.toByteArray()),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "Rekap_Keuangan_PMB.xlsx"
            );
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/DasborKeuanganPmbAction.java:1255");
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** Memformat angka panjang dengan pemisah ribuan (mis. 1.234.567). */
    private String fmt(long v) {
        try { return Common.numberFormat.get().format(v); }
        catch (Exception e) { return String.valueOf(v); }
    }

    /** Meng-escape karakter HTML agar aman ditampilkan di konten HTML. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =========================================================================
    // CSS
    // =========================================================================

    /**
     * Membangun blok {@code <style>} CSS untuk seluruh dasbor.
     * Semua class CSS menggunakan prefix {@code dgk-} (Dasbor Keuangan) agar
     * tidak bertabrakan dengan class global ZK atau Bootstrap yang mungkin
     * dipakai di halaman yang sama.
     *
     * <p>Desain responsif menggunakan CSS Grid dengan {@code auto-fill minmax}
     * sehingga kartu dan kolom menyesuaikan lebar layar secara otomatis
     * tanpa JavaScript tambahan.</p>
     *
     * @return HTML string blok &lt;style&gt; lengkap
     */
    private String buildCss() {
        return "<style>" +
            // Header
            ".dgk-header{padding:16px 16px 6px;}" +
            ".dgk-title{font-size:20px;font-weight:900;color:#1e293b;margin:0 0 4px;}" +
            ".dgk-subtitle{font-size:12px;color:#64748b;line-height:1.5;}" +
            // Filter bar
            ".dgk-filter-bar{display:flex;align-items:center;gap:10px;flex-wrap:wrap;" +
              "padding:10px 16px;background:#f8fafc;" +
              "border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;margin-bottom:16px;}" +
            ".dgk-filter-info{font-size:11px;color:#64748b;padding:0 0 10px;}" +
            // Kartu ringkasan — 6 kartu auto-fill
            ".dgk-cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));" +
              "gap:12px;margin-bottom:14px;}" +
            ".dgk-card{background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:16px;" +
              "box-shadow:0 2px 8px rgba(0,0,0,.05);transition:box-shadow .2s;}" +
            ".dgk-card:hover{box-shadow:0 4px 16px rgba(0,0,0,.10);}" +
            ".dgk-card-ico{width:42px;height:42px;border-radius:10px;display:flex;" +
              "align-items:center;justify-content:center;font-size:20px;margin-bottom:10px;}" +
            ".dgk-card-lbl{font-size:10px;text-transform:uppercase;letter-spacing:.5px;" +
              "color:#64748b;font-weight:700;margin-bottom:4px;}" +
            ".dgk-card-val{font-size:16px;font-weight:900;color:#1e293b;" +
              "word-break:break-all;line-height:1.2;}" +
            ".dgk-card-sub{font-size:10px;color:#94a3b8;margin-top:5px;line-height:1.4;}" +
            // Layout 2 kolom
            ".dgk-2col{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:14px;}" +
            // Panel section
            ".dgk-sect{background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:18px;" +
              "margin-bottom:14px;box-shadow:0 2px 8px rgba(0,0,0,.04);}" +
            ".dgk-sect:last-child{margin-bottom:0;}" +
            ".dgk-sect-title{font-size:14px;font-weight:800;color:#1e293b;margin:0 0 3px;}" +
            ".dgk-sect-desc{font-size:11px;color:#94a3b8;margin:0 0 10px;line-height:1.5;}" +
            // Responsive
            "@media(max-width:768px){" +
              ".dgk-2col{grid-template-columns:1fr;}" +
              ".dgk-cards{grid-template-columns:repeat(auto-fill,minmax(150px,1fr));}" +
            "}" +
            "@media(max-width:480px){" +
              ".dgk-cards{grid-template-columns:1fr 1fr;}" +
            "}" +
            "</style>";
    }
}
