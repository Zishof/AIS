package ais.action.master.dashboard.admin;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;

/**
 * Dashboard ringan untuk merangkum tab "Info & Materi" e-Learning.
 *
 * Catatan desain:
 * 1. Query tabel utama tetap memakai Common.ambilSql(sql).
 * 2. Query tabel file/blob yang berada di database streaming memakai
 *    Common.ambilSqlStreaming(sql), terutama:
 *    - pertemuan_file_content
 *    - video_pertemuan
 *    - audio_pertemuan
 *    - tugas_file_content
 *    - lampiran_lain
 * 3. Tidak memakai tabel cache/index absensi pertemuan.
 * 4. Kompatibel Java 1.7 dan ZKoss 5.5.
 */
public class DasborInfoDanMateri extends Window implements Serializable {

    private static final long serialVersionUID = 202606010058L;
    private static final int CHUNK_SIZE = 700;
    private static final int DETAIL_LIMIT = 600;
    private static final ThreadLocal<DecimalFormat> NUMBER_FORMAT = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("#,##0");
        }
    };

    private final Tbmuser tbmuser;
    private Vbox mainContainer;
    private Label statusLabel;
    private String tahunAkademik;
    private DashboardScope currentScope;
    private List<StatItem> currentStats;

    public DasborInfoDanMateri() {
        this(Common.getCurrentUser());
    }

    public DasborInfoDanMateri(Tbmuser user) {
        this.tbmuser = user;
        this.tahunAkademik = Common.getCurrentTahunAkademik();
        initWindow();
    }

    public static DasborInfoDanMateri display(Component parent, Tbmuser user) {
        DasborInfoDanMateri dashboard = new DasborInfoDanMateri(user);
        dashboard.setParent(parent);
        return dashboard;
    }

    private void initWindow() {
        setTitle("");
        setBorder("none");
        setWidth("100%");
        setHeight("100%");

        Borderlayout layout = new Borderlayout();
        layout.setParent(this);
        layout.setWidth("100%");
        layout.setHeight("100%");

        North north = new North();
        north.setParent(layout);
        north.setHeight("54px");
        north.setBorder("none");
        north.setSplittable(false);
        north.setCollapsible(false);

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(north);
        toolbar.setWidth("100%");
        toolbar.setStyle("padding:8px 10px; min-height:42px; background:#f8fafc; border-bottom:1px solid #e5e7eb;");

        Label title = new Label(ais.common.Common.getBahasaConfig("Dasbor Info & Materi"));
        title.setStyle("font-weight:bold; font-size:14px; color:#0f172a; margin-right:12px;");
        title.setParent(toolbar);

        statusLabel = new Label(ais.common.ElearningConfigUtil.text("elearning_desc_info_materi", "Merangkum file, video, audio, tugas, dan materi agar bahan belajar lebih mudah ditemukan."));
        statusLabel.setStyle("font-size:11px; color:#64748b;");
        statusLabel.setParent(toolbar);

        Space space = new Space();
        space.setWidth("12px");
        space.setParent(toolbar);

        Toolbarbutton refresh = new Toolbarbutton("Muat Ulang");
        refresh.setImage("/img/svg/refresh-cw.svg");
        refresh.setTooltiptext("Muat ulang ringkasan Info & Materi");
        refresh.setStyle("font-size:12px;");
        refresh.setParent(toolbar);
        refresh.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                reloadAsync();
            }
        });

        try {
            setContentStyle("overflow:hidden;");
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasborInfoDanMateri.java:135");
        }

        Center center = new Center();
        center.setParent(layout);
        ais.ui.util.ZkCompat.setFlex(center, true);
        center.setBorder("none");
        center.setAutoscroll(false);
        center.setStyle("overflow:hidden; background:#f1f5f9;");

        Div scrollPane = new Div();
        scrollPane.setParent(center);
        scrollPane.setWidth("100%");
        scrollPane.setHeight("100%");
        scrollPane.setStyle("height:100%; max-height:100%; overflow-y:auto; overflow-x:hidden; background:#f1f5f9; box-sizing:border-box;");

        mainContainer = new Vbox();
        mainContainer.setParent(scrollPane);
        mainContainer.setWidth("100%");
        mainContainer.setStyle("background:#f1f5f9; min-height:100%; padding:10px 10px 80px 10px; box-sizing:border-box;");

        reloadAsync();
    }

    private void reloadAsync() {
        Common.clear(mainContainer);
        renderLoading("Menyiapkan ringkasan Info & Materi...", 10);
        if (statusLabel != null) {
            statusLabel.setValue("Menyiapkan data...");
        }

        final Timer timer = new Timer(180);
        timer.setRepeats(false);
        timer.setParent(this);
        timer.addEventListener("onTimer", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                try {
                    timer.stop();
                    timer.detach();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasborInfoDanMateri.java:175");
                }
                loadDashboard();
            }
        });
        timer.start();
    }

    private void loadDashboard() {
        long start = System.currentTimeMillis();
        try {
            Common.clear(mainContainer);
            renderLoading("Mengambil daftar pertemuan dan relasi pembelajaran...", 20);

            DashboardScope scope = resolveScope();
            updateStatus("Menganalisis " + scope.pertemuanIds.size() + " pertemuan aktif...");

            List<StatItem> stats = new ArrayList<StatItem>();
            addStats(stats, scope);

            long elapsed = System.currentTimeMillis() - start;
            currentScope = scope;
            currentStats = stats;
            Common.clear(mainContainer);
            renderDashboard(scope, stats, elapsed);
            updateStatus("Selesai memuat " + stats.size() + " kelompok data dalam " + elapsed + " ms");
        } catch (Exception e) {
            Common.clear(mainContainer);
            renderError(e);
            updateStatus("Gagal memuat data: " + safe(e.getMessage()));
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void addStats(List<StatItem> stats, DashboardScope scope) {
        updateStatus("Menghitung pengumuman, ujian, tugas, dan tugas kelompok...");
        stats.add(new StatItem("Info", "Pengumuman Perkuliahan", countPengumuman(scope), "TampilanPengumumanPerkuliahanAction", false));
        stats.add(new StatItem("Ujian", "Ujian", countByPertemuanMain("pertemuan_punya_ujian", "pertemuan", scope.pertemuanIds), "RekapitulasiUjianHelper", false));
        stats.add(new StatItem("Tugas", "Tugas", countTugas(scope), "RekapitulasiTugasHelper", false));
        stats.add(new StatItem("Tgs.Kel", "Tugas Kelompok", countByPertemuanMain("tugas_kelompok", "pertemuan", scope.pertemuanIds), "RekapitulasiTugasKelompokHelper", false));

        updateStatus("Menghitung materi, video, audio, dan lampiran tugas dari database streaming...");
        stats.add(new StatItem("Materi", "Materi / File Pertemuan", countByPertemuanStreaming("pertemuan_file_content", "pertemuan", scope.pertemuanIds), "RekapitulasiMateriHelper", true));
        stats.add(new StatItem("Video", "Video Pertemuan", countByPertemuanStreaming("video_pertemuan", "pertemuan", scope.pertemuanIds), "RekapitulasiVideoHelper", true));
        stats.add(new StatItem("Audio", "Audio Pertemuan", countByPertemuanStreaming("audio_pertemuan", "pertemuan", scope.pertemuanIds), "RekapitulasiAudioHelper", true));
        stats.add(new StatItem("Lamp.Tugas", "Lampiran Tugas", countLampiranTugas(scope), "TugasFileContent", true));

        updateStatus("Menghitung buku referensi, diktat, dan artikel...");
        stats.add(new StatItem("Buku", "Buku Referensi", countBukuReferensi(scope), "PerkuliahanPunyaItemHelper", false));
        stats.add(new StatItem("Diktat", "Buku Bahan Ajar / Diktat", countBukuAjar(scope), "BukuBahanAjarHelper", false));
        stats.add(new StatItem("Artikel", "Artikel", countArtikel(scope), "DataPunyaArtikelHelper", false));
    }

    private DashboardScope resolveScope() {
        DashboardScope scope = new DashboardScope();
        scope.tahunAkademik = tahunAkademik;
        scope.sekolah = safeGetSekolah();

        String wherePertemuan = buildWherePertemuan(scope.sekolah);
        String sqlPertemuan = "select p.id from pertemuan p where " + wherePertemuan + " order by p.tanggal desc, p.id desc";
        scope.pertemuanIds = toLongList(Common.ambilSql(sqlPertemuan));

        scope.perkuliahanIds = loadDistinctMain("select p.perkuliahan from pertemuan p where p.perkuliahan is not null and "
                + wherePertemuan + " group by p.perkuliahan");
        scope.jadwalPelajaranIds = loadDistinctMain("select p.jadwal_pelajaran from pertemuan p where p.jadwal_pelajaran is not null and "
                + wherePertemuan + " group by p.jadwal_pelajaran");
        scope.matakuliahIds = loadMatakuliahIds(scope.perkuliahanIds);
        return scope;
    }

    private String buildWherePertemuan(Sekolah sekolah) {
        StringBuilder where = new StringBuilder();
        where.append(" (p.aktif = true or p.aktif is null) ");
        if (tahunAkademik != null && !tahunAkademik.trim().isEmpty()) {
            where.append(" and p.ta = \'").append(sqlEscape(tahunAkademik)).append("\' ");
        }

        if (tbmuser == null || tbmuser.getUserId() == null) {
            where.append(" and 1=0 ");
            return where.toString();
        }

        Mahasiswa mahasiswa = null;
        Dosen dosen = null;
        Siswa siswa = null;
        Guru guru = null;
        try {
            mahasiswa = tbmuser.getMahasiswa();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasborInfoDanMateri.java:263");
        }
        try {
            dosen = tbmuser.ambilDosen();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasborInfoDanMateri.java:267");
        }
        try {
            siswa = tbmuser.getSiswa();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasborInfoDanMateri.java:271");
        }
        try {
            guru = tbmuser.ambilGuru();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasborInfoDanMateri.java:275");
        }

        if (mahasiswa != null && mahasiswa.getId() != null) {
            where.append(" and p.perkuliahan in (select a.perkuliahan from detailperkuliahan a where a.mahasiswa=")
                    .append(mahasiswa.getId()).append(" and a.perkuliahan is not null group by a.perkuliahan) ");
            return where.toString();
        }

        if (dosen != null && dosen.getId() != null) {
            where.append(" and p.perkuliahan in (select pk.id from perkuliahan pk where ")
                    .append(buildDosenSql("pk", dosen.getId())).append(") ");
            return where.toString();
        }

        if (siswa != null && siswa.getId() != null) {
            where.append(" and p.jadwal_pelajaran in (select jp.id from sekolah.jadwal_pelajaran jp where ")
                    .append(" jp.kelas_id in (select kps.kelas_id from sekolah.kelas_punya_siswa kps where kps.siswa_id=")
                    .append(siswa.getId()).append(" and kps.kelas_id is not null and (kps.aktif=true or kps.aktif is null)) ")
                    .append(" or jp.kelas_les_siswa in (select klps.kelas_id from sekolah.kelas_les_punya_siswa klps where klps.siswa_id=")
                    .append(siswa.getId()).append(" and klps.kelas_id is not null and (klps.aktif=true or klps.aktif is null)) ")
                    .append(") ");
            return where.toString();
        }

        if (guru != null && guru.getId() != null) {
            where.append(" and p.jadwal_pelajaran in (select jp.id from sekolah.jadwal_pelajaran jp where ")
                    .append(buildGuruSql("jp", guru.getId())).append(") ");
            return where.toString();
        }

        if (sekolah != null && sekolah.getId() != null) {
            where.append(" and p.jadwal_pelajaran in (select jp.id from sekolah.jadwal_pelajaran jp where jp.sekolah_id=")
                    .append(sekolah.getId()).append(") ");
            return where.toString();
        }

        if (!Common.getApakahAdmin()) {
            where.append(" and 1=0 ");
        }
        return where.toString();
    }

    private String buildDosenSql(String alias, Long dosenId) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            if (i > 1) {
                sb.append(" or ");
            }
            sb.append(alias).append(".dosen").append(i).append("=").append(dosenId);
        }
        return sb.toString();
    }

    private String buildGuruSql(String alias, Long guruId) {
        StringBuilder sb = new StringBuilder();
        sb.append(alias).append(".guru_id=").append(guruId);
        for (int i = 2; i <= 12; i++) {
            sb.append(" or ").append(alias).append(".guru").append(i).append("_id=").append(guruId);
        }
        return sb.toString();
    }

    private Sekolah safeGetSekolah() {
        try {
            return SekolahUtil.getSekolah();
        } catch (Exception e) {
            return null;
        }
    }

    private List<Long> loadDistinctMain(String sql) {
        try {
            return toLongList(Common.ambilSql(sql));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Long>();
        }
    }

    private List<Long> loadMatakuliahIds(List<Long> perkuliahanIds) {
        final List<Long> result = new ArrayList<Long>();
        if (isEmpty(perkuliahanIds)) {
            return result;
        }
        executeForChunks(perkuliahanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sql = "select matakuliah from perkuliahan where id in (" + inSql
                        + ") and matakuliah is not null group by matakuliah";
                result.addAll(toLongList(Common.ambilSql(sql)));
            }
        });
        return result;
    }

    private long countPengumuman(final DashboardScope scope) {
        if (isEmpty(scope.perkuliahanIds)) {
            return 0L;
        }
        final long[] total = new long[] { 0L };
        executeForChunks(scope.perkuliahanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sql = "select count(id) from pengumuman_perkuliahan where (aktif=true or aktif is null) and perkuliahan in ("
                        + inSql + ")";
                total[0] += firstLong(Common.ambilSql(sql));
            }
        });
        return total[0];
    }

    private long countTugas(final DashboardScope scope) {
        if (isEmpty(scope.pertemuanIds)) {
            return 0L;
        }
        final long[] total = new long[] { 0L };
        executeForChunks(scope.pertemuanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sqlPertemuan = "select count(id) from pertemuan where id in (" + inSql
                        + ") and judultugas is not null and trim(judultugas) <> ''";
                String sqlTugas = "select count(id) from tugas_pertemuan where pertemuan in (" + inSql
                        + ") and judultugas is not null and trim(judultugas) <> ''";
                total[0] += firstLong(Common.ambilSql(sqlPertemuan));
                total[0] += firstLong(Common.ambilSql(sqlTugas));
            }
        });
        return total[0];
    }

    private long countLampiranTugas(final DashboardScope scope) {
        if (isEmpty(scope.pertemuanIds)) {
            return 0L;
        }
        if (!columnExistsStreaming("tugas_file_content", "pertemuan")) {
            return 0L;
        }
        final long[] total = new long[] { 0L };
        executeForChunks(scope.pertemuanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sql = "select count(id) from tugas_file_content where pertemuan in (" + inSql + ")";
                total[0] += firstLongStreamingSafe(sql);
            }
        });
        return total[0];
    }

    private long countBukuReferensi(final DashboardScope scope) {
        if (isEmpty(scope.perkuliahanIds)) {
            return 0L;
        }
        final long[] total = new long[] { 0L };
        executeForChunks(scope.perkuliahanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sql = "select count(id) from perkuliahan_punya_item where perkuliahan in (" + inSql + ")";
                total[0] += firstLong(Common.ambilSql(sql));
            }
        });
        return total[0];
    }

    private long countBukuAjar(final DashboardScope scope) {
        if (isEmpty(scope.matakuliahIds)) {
            return 0L;
        }
        final long[] total = new long[] { 0L };
        executeForChunks(scope.matakuliahIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sql = "select count(id) from matakuliah_punya_buku_bahan_ajar where matakuliah in (" + inSql + ")";
                total[0] += firstLong(Common.ambilSql(sql));
            }
        });
        return total[0];
    }

    private long countArtikel(final DashboardScope scope) {
        long total = 0L;
        if (!isEmpty(scope.perkuliahanIds)) {
            final long[] sub = new long[] { 0L };
            executeForChunks(scope.perkuliahanIds, new ChunkExecutor() {
                @Override
                public void execute(String inSql) {
                    String sql = "select count(id) from data_punya_artikel where perkuliahan in (" + inSql + ")";
                    sub[0] += firstLong(Common.ambilSql(sql));
                }
            });
            total += sub[0];
        }
        if (!isEmpty(scope.jadwalPelajaranIds)) {
            final long[] sub = new long[] { 0L };
            executeForChunks(scope.jadwalPelajaranIds, new ChunkExecutor() {
                @Override
                public void execute(String inSql) {
                    String sql = "select count(id) from data_punya_artikel where jadwal_pelajaran in (" + inSql + ")";
                    sub[0] += firstLong(Common.ambilSql(sql));
                }
            });
            total += sub[0];
        }
        return total;
    }

    private long countByPertemuanMain(final String tableName, final String fieldName, List<Long> ids) {
        if (isEmpty(ids)) {
            return 0L;
        }
        final long[] total = new long[] { 0L };
        executeForChunks(ids, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sql = "select count(id) from " + tableName + " where " + fieldName + " in (" + inSql + ")";
                total[0] += firstLong(Common.ambilSql(sql));
            }
        });
        return total[0];
    }

    private long countByPertemuanStreaming(final String tableName, final String fieldName, List<Long> ids) {
        if (isEmpty(ids)) {
            return 0L;
        }
        final long[] total = new long[] { 0L };
        executeForChunks(ids, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sql = "select count(id) from " + tableName + " where " + fieldName + " in (" + inSql + ")";
                total[0] += firstLongStreamingSafe(sql);
            }
        });
        return total[0];
    }

    private long firstLongStreamingSafe(String sql) {
        try {
            return firstLong(Common.ambilSqlStreaming(sql));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return 0L;
        }
    }

    private boolean columnExistsStreaming(String tableName, String columnName) {
        try {
            String sql = "select count(*) from information_schema.columns where table_name='" + sqlEscape(tableName)
                    + "' and column_name='" + sqlEscape(columnName) + "'";
            return firstLong(Common.ambilSqlStreaming(sql)) > 0L;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return false;
        }
    }

    private void executeForChunks(List<Long> ids, ChunkExecutor executor) {
        if (ids == null || ids.isEmpty() || executor == null) {
            return;
        }
        StringBuilder in = new StringBuilder();
        int count = 0;
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            if (in.length() > 0) {
                in.append(',');
            }
            in.append(id.longValue());
            count++;
            if (count >= CHUNK_SIZE) {
                executor.execute(in.toString());
                in.setLength(0);
                count = 0;
            }
        }
        if (in.length() > 0) {
            executor.execute(in.toString());
        }
    }

    private void renderLoading(String message, int progress) {
        Html html = new Html("<div style=\"background:#fff; border:1px solid #e5e7eb; border-radius:14px; padding:18px; box-shadow:0 4px 16px rgba(15,23,42,.06);\">"
                + "<div style=\"font-weight:700; color:#0f172a; font-size:14px; margin-bottom:8px;\">" + escapeHtml(message) + "</div>"
                + "<div style=\"height:10px; border-radius:999px; background:#e5e7eb; overflow:hidden;\">"
                + "<div style=\"width:" + progress + "%; height:10px; border-radius:999px; background:linear-gradient(90deg,#0ea5e9,#22c55e);\"></div>"
                + "</div><div style=\"font-size:11px; color:#64748b; margin-top:8px;\">Mohon tunggu, sistem sedang menyusun ringkasan ringan dari beberapa sumber data.</div></div>");
        html.setParent(mainContainer);
    }

    private void renderDashboard(final DashboardScope scope, final List<StatItem> stats, long elapsed) {
        currentScope = scope;
        currentStats = stats;
        buildHeroComponent(scope, stats, elapsed).setParent(mainContainer);
        buildCardsComponent(stats).setParent(mainContainer);
        buildChartsComponent(stats).setParent(mainContainer);
        buildGrid(stats).setParent(mainContainer);
    }

    private Component buildHeroComponent(final DashboardScope scope, final List<StatItem> stats, long elapsed) {
        Div hero = new Div();
        hero.setWidth("100%");
        hero.setStyle("background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#fff; border-radius:18px; padding:16px; margin-bottom:10px; box-shadow:0 10px 28px rgba(15,23,42,.18); box-sizing:border-box; cursor:pointer;");
        hero.setTooltiptext("Klik untuk melihat ringkasan seluruh data Info & Materi");
        hero.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showOverviewPopup();
            }
        });

        Html title = new Html("<div style=\"font-size:20px; font-weight:800; margin-bottom:4px; line-height:1.25;\">Dasbor Info &amp; Materi e-Learning</div>"
                + "<div style=\"font-size:12px; opacity:.90; line-height:1.55; max-width:980px;\">Klik judul, angka, card, bar grafik, atau baris grid untuk membuka rincian data. Tahun akademik: <b>"
                + escapeHtml(scope.tahunAkademik) + "</b>. Waktu proses: <b>" + elapsed + " ms</b>.</div>");
        title.setParent(hero);

        Div chips = new Div();
        chips.setParent(hero);
        chips.setWidth("100%");
        chips.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(138px,1fr)); gap:8px; margin-top:12px;");
        createScopeChip(chips, "Pertemuan", scope.pertemuanIds.size(), "pertemuan");
        createScopeChip(chips, "Perkuliahan", scope.perkuliahanIds.size(), "perkuliahan");
        createScopeChip(chips, "Jadwal Pelajaran", scope.jadwalPelajaranIds.size(), "jadwal");
        createScopeChip(chips, "Total Konten", total(stats), "total");
        return hero;
    }

    private void createScopeChip(Component parent, final String label, final long value, final String type) {
        Div chip = new Div();
        chip.setParent(parent);
        chip.setStyle("padding:9px 11px; border-radius:14px; background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.24); cursor:pointer; min-height:42px; box-sizing:border-box;");
        chip.setTooltiptext("Klik untuk melihat rincian " + label);
        chip.appendChild(new Html("<div style=\"font-size:11px; opacity:.85; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">" + escapeHtml(label) + "</div>"
                + "<div style=\"font-size:18px; font-weight:800; line-height:1.2;\">" + NUMBER_FORMAT.get().format(value) + "</div>"));
        chip.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showScopeDetailPopup(label, type);
            }
        });
    }

    private Component buildCardsComponent(final List<StatItem> stats) {
        Div wrap = new Div();
        wrap.setWidth("100%");
        wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(138px,1fr)); gap:10px; margin-bottom:10px;");
        for (int i = 0; i < stats.size(); i++) {
            final StatItem s = stats.get(i);
            Div card = new Div();
            card.setParent(wrap);
            card.setTooltiptext("Klik untuk melihat rincian " + s.label);
            card.setStyle("background:#fff; border:1px solid #e5e7eb; border-radius:16px; padding:13px; box-shadow:0 5px 18px rgba(15,23,42,.07); cursor:pointer; min-height:92px; box-sizing:border-box;");
            card.appendChild(new Html("<div style=\"font-size:11px; color:#64748b; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">"
                    + escapeHtml(s.label) + "</div>"
                    + "<div style=\"font-size:25px; font-weight:800; color:#0f172a; margin-top:4px; line-height:1.15;\">"
                    + NUMBER_FORMAT.get().format(s.total) + "</div>"
                    + "<div style=\"font-size:10.5px; color:" + (s.streaming ? "#0369a1" : "#64748b") + "; margin-top:5px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">"
                    + (s.streaming ? "Database file/streaming" : "Database utama") + "</div>"));
            card.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    showStatDetailPopup(s);
                }
            });
        }
        return wrap;
    }

    private Component buildChartsComponent(final List<StatItem> stats) {
        long max = max(stats);
        if (max <= 0) {
            max = 1;
        }

        Div outer = new Div();
        outer.setWidth("100%");
        outer.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(300px,1fr)); gap:10px; margin-bottom:10px; align-items:stretch;");

        Div dist = new Div();
        dist.setParent(outer);
        dist.setStyle("background:#fff; border:1px solid #e5e7eb; border-radius:16px; padding:14px; box-shadow:0 5px 18px rgba(15,23,42,.07); cursor:pointer; box-sizing:border-box; min-height:145px;");
        dist.setTooltiptext("Klik untuk melihat distribusi sumber data");
        dist.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showOverviewPopup();
            }
        });
        long totalUtama = 0L;
        long totalStreaming = 0L;
        for (int i = 0; i < stats.size(); i++) {
            if (stats.get(i).streaming) {
                totalStreaming += stats.get(i).total;
            } else {
                totalUtama += stats.get(i).total;
            }
        }
        long all = totalUtama + totalStreaming;
        int pctUtama = all <= 0 ? 0 : (int) Math.round((totalUtama * 100.0) / all);
        int pctStreaming = all <= 0 ? 0 : 100 - pctUtama;
        dist.appendChild(new Html("<div style=\"font-size:14px; font-weight:800; color:#0f172a; margin-bottom:10px;\">Distribusi Sumber Data</div>"
                + "<div style=\"height:20px; border-radius:999px; overflow:hidden; background:#e5e7eb; display:flex;\">"
                + "<div style=\"width:" + pctUtama + "% ; background:#0ea5e9;\"></div>"
                + "<div style=\"width:" + pctStreaming + "% ; background:#22c55e;\"></div></div>"
                + "<div style=\"display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; margin-top:12px;\">"
                + "<div style=\"background:#f0f9ff; border:1px solid #bae6fd; border-radius:12px; padding:9px;\"><div style=\"font-size:10px; color:#0369a1; font-weight:700;\">Database Utama</div><div style=\"font-size:20px; font-weight:800; color:#0f172a;\">" + NUMBER_FORMAT.get().format(totalUtama) + "</div></div>"
                + "<div style=\"background:#f0fdf4; border:1px solid #bbf7d0; border-radius:12px; padding:9px;\"><div style=\"font-size:10px; color:#15803d; font-weight:700;\">File/Streaming</div><div style=\"font-size:20px; font-weight:800; color:#0f172a;\">" + NUMBER_FORMAT.get().format(totalStreaming) + "</div></div>"
                + "</div>"));

        Div spider = new Div();
        spider.setParent(outer);
        spider.setStyle("background:#fff; border:1px solid #e5e7eb; border-radius:16px; padding:14px; box-shadow:0 5px 18px rgba(15,23,42,.07); cursor:pointer; box-sizing:border-box; min-height:145px;");
        spider.setTooltiptext("Klik untuk melihat indeks aktivitas materi");
        spider.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showOverviewPopup();
            }
        });
        spider.appendChild(new Html("<div style=\"font-size:14px; font-weight:800; color:#0f172a; margin-bottom:8px;\">Indeks Aktivitas Materi</div>" + buildMiniSpider(stats, max)));

        Div trend = new Div();
        trend.setParent(outer);
        trend.setStyle("grid-column:1/-1; background:#fff; border:1px solid #e5e7eb; border-radius:16px; padding:14px; box-shadow:0 5px 18px rgba(15,23,42,.07); box-sizing:border-box;");
        trend.appendChild(new Html("<div style=\"font-size:14px; font-weight:800; color:#0f172a; margin-bottom:3px;\">Trend Ringkasan Konten</div>"
                + "<div style=\"font-size:11px; color:#64748b; margin-bottom:10px;\">Bar disusun responsif agar tidak terlalu panjang di desktop dan tetap rapi di mobile.</div>"));

        Div trendGrid = new Div();
        trendGrid.setParent(trend);
        trendGrid.setWidth("100%");
        trendGrid.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:8px;");

        for (int i = 0; i < stats.size(); i++) {
            final StatItem s = stats.get(i);
            int pct = (int) Math.round((s.total * 100.0) / max);
            if (pct < 2 && s.total > 0) {
                pct = 2;
            }
            createTrendItem(trendGrid, s, pct);
        }
        return outer;
    }

    private void createTrendItem(Component parent, final StatItem s, int pct) {
        if (pct < 0) {
            pct = 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        Div item = new Div();
        item.setParent(parent);
        item.setTooltiptext("Klik untuk melihat rincian " + s.label);
        item.setStyle("background:#f8fafc; border:1px solid #e2e8f0; border-radius:13px; padding:9px; cursor:pointer; box-sizing:border-box; min-height:74px;");

        Div top = new Div();
        top.setParent(item);
        top.setStyle("display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:7px;");
        Html label = new Html("<div style=\"font-size:11px; color:#334155; font-weight:700; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">" + escapeHtml(s.kode) + "</div>");
        label.setParent(top);
        Html number = new Html("<div style=\"font-size:12px; font-weight:800; color:#0f172a; white-space:nowrap;\">" + NUMBER_FORMAT.get().format(s.total) + "</div>");
        number.setParent(top);

        Div bar = new Div();
        bar.setParent(item);
        bar.setStyle("height:12px; background:#e5e7eb; border-radius:999px; overflow:hidden; width:100%;");
        Div fill = new Div();
        fill.setParent(bar);
        fill.setStyle("width:" + pct + "%; height:12px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); border-radius:999px;");

        Html desc = new Html("<div style=\"font-size:10px; color:#64748b; margin-top:6px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">" + escapeHtml(s.label) + "</div>");
        desc.setParent(item);

        item.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showStatDetailPopup(s);
            }
        });
    }

    private String buildHeroHtml(DashboardScope scope, List<StatItem> stats, long elapsed) {
        long total = total(stats);
        return "<div style=\"background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#fff; border-radius:16px; padding:18px; margin-bottom:10px; box-shadow:0 8px 24px rgba(15,23,42,.14);\">"
                + "<div style=\"font-size:20px; font-weight:800; margin-bottom:4px;\">Dasbor Info & Materi e-Learning</div>"
                + "<div style=\"font-size:12px; opacity:.88; line-height:1.6;\">Ringkasan pengumuman, ujian, tugas, tugas kelompok, materi, video, audio, buku, diktat, dan artikel. "
                + "Tahun akademik: <b>" + escapeHtml(scope.tahunAkademik) + "</b>. Waktu proses: <b>" + elapsed + " ms</b>.</div>"
                + "<div style=\"display:flex; gap:10px; margin-top:12px; flex-wrap:wrap;\">"
                + heroChip("Pertemuan", scope.pertemuanIds.size())
                + heroChip("Perkuliahan", scope.perkuliahanIds.size())
                + heroChip("Jadwal Pelajaran", scope.jadwalPelajaranIds.size())
                + heroChip("Total Konten", total)
                + "</div></div>";
    }

    private String heroChip(String label, long value) {
        return "<span style=\"display:inline-block; padding:8px 11px; border-radius:999px; background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.22); font-size:12px;\">"
                + escapeHtml(label) + ": <b>" + NUMBER_FORMAT.get().format(value) + "</b></span>";
    }

    private String buildCardsHtml(List<StatItem> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"display:grid; grid-template-columns:repeat(auto-fit,minmax(165px,1fr)); gap:10px; margin-bottom:10px;\">");
        for (int i = 0; i < stats.size(); i++) {
            StatItem s = stats.get(i);
            sb.append("<div style=\"background:#fff; border:1px solid #e5e7eb; border-radius:14px; padding:13px; box-shadow:0 4px 14px rgba(15,23,42,.06);\">")
                    .append("<div style=\"font-size:11px; color:#64748b; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">")
                    .append(escapeHtml(s.label)).append("</div>")
                    .append("<div style=\"font-size:24px; font-weight:800; color:#0f172a; margin-top:4px;\">")
                    .append(NUMBER_FORMAT.get().format(s.total)).append("</div>")
                    .append("<div style=\"font-size:10px; color:").append(s.streaming ? "#0369a1" : "#64748b")
                    .append("; margin-top:4px;\">").append(s.streaming ? "Database file/streaming" : "Database utama")
                    .append("</div></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildChartsHtml(List<StatItem> stats) {
        long max = max(stats);
        if (max <= 0) {
            max = 1;
        }
        StringBuilder bar = new StringBuilder();
        bar.append("<div style=\"background:#fff; border:1px solid #e5e7eb; border-radius:14px; padding:14px; margin-bottom:10px; box-shadow:0 4px 14px rgba(15,23,42,.06);\">")
                .append("<div style=\"font-size:14px; font-weight:800; color:#0f172a; margin-bottom:10px;\">Trend Ringkasan Konten</div>");
        for (int i = 0; i < stats.size(); i++) {
            StatItem s = stats.get(i);
            int pct = (int) Math.round((s.total * 100.0) / max);
            if (pct < 2 && s.total > 0) {
                pct = 2;
            }
            bar.append("<div style=\"display:flex; align-items:center; gap:8px; margin:8px 0;\">")
                    .append("<div style=\"width:120px; font-size:11px; color:#334155; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">")
                    .append(escapeHtml(s.kode)).append("</div>")
                    .append("<div style=\"flex:1; height:12px; background:#e5e7eb; border-radius:999px; overflow:hidden;\">")
                    .append("<div style=\"width:").append(pct).append("%; height:12px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); border-radius:999px;\"></div>")
                    .append("</div><div style=\"width:70px; text-align:right; font-size:11px; font-weight:700; color:#0f172a;\">")
                    .append(NUMBER_FORMAT.get().format(s.total)).append("</div></div>");
        }
        bar.append("</div>");

        StringBuilder dist = new StringBuilder();
        long totalUtama = 0L;
        long totalStreaming = 0L;
        for (int i = 0; i < stats.size(); i++) {
            if (stats.get(i).streaming) {
                totalStreaming += stats.get(i).total;
            } else {
                totalUtama += stats.get(i).total;
            }
        }
        long all = totalUtama + totalStreaming;
        int pctUtama = all <= 0 ? 0 : (int) Math.round((totalUtama * 100.0) / all);
        int pctStreaming = 100 - pctUtama;
        dist.append("<div style=\"display:grid; grid-template-columns:repeat(auto-fit,minmax(240px,1fr)); gap:10px; margin-bottom:10px;\">")
                .append("<div style=\"background:#fff; border:1px solid #e5e7eb; border-radius:14px; padding:14px; box-shadow:0 4px 14px rgba(15,23,42,.06);\">")
                .append("<div style=\"font-size:14px; font-weight:800; color:#0f172a; margin-bottom:12px;\">Distribusi Sumber Data</div>")
                .append("<div style=\"height:16px; border-radius:999px; overflow:hidden; background:#e5e7eb; display:flex;\">")
                .append("<div style=\"width:").append(pctUtama).append("%; background:#0ea5e9;\"></div>")
                .append("<div style=\"width:").append(pctStreaming).append("%; background:#22c55e;\"></div>")
                .append("</div><div style=\"display:flex; justify-content:space-between; font-size:11px; color:#64748b; margin-top:9px;\">")
                .append("<span>Utama: <b>").append(NUMBER_FORMAT.get().format(totalUtama)).append("</b></span>")
                .append("<span>File/Streaming: <b>").append(NUMBER_FORMAT.get().format(totalStreaming)).append("</b></span>")
                .append("</div></div>")
                .append("<div style=\"background:#fff; border:1px solid #e5e7eb; border-radius:14px; padding:14px; box-shadow:0 4px 14px rgba(15,23,42,.06);\">")
                .append("<div style=\"font-size:14px; font-weight:800; color:#0f172a; margin-bottom:8px;\">Indeks Aktivitas Materi</div>")
                .append(buildMiniSpider(stats, max))
                .append("</div></div>");
        return dist.toString() + bar.toString();
    }

    private String buildMiniSpider(List<StatItem> stats, long max) {
        int limit = Math.min(8, stats.size());
        if (limit == 0) {
            return "<div style=\"font-size:12px;color:#64748b;\">Belum ada data.</div>";
        }
        StringBuilder labels = new StringBuilder();
        labels.append("<div style=\"display:grid; grid-template-columns:repeat(2,1fr); gap:6px; margin-top:6px;\">");
        for (int i = 0; i < limit; i++) {
            StatItem s = stats.get(i);
            int pct = (int) Math.round((s.total * 100.0) / (max <= 0 ? 1 : max));
            labels.append("<div style=\"font-size:11px; color:#334155;\"><span style=\"display:inline-block;width:8px;height:8px;border-radius:99px;background:#0ea5e9;margin-right:5px;\"></span>")
                    .append(escapeHtml(s.kode)).append(": <b>").append(pct).append("%</b></div>");
        }
        labels.append("</div>");
        return "<div style=\"height:120px; display:flex; align-items:center; justify-content:center; background:radial-gradient(circle,#e0f2fe 0,#e0f2fe 32%,#f8fafc 33%,#f8fafc 100%); border-radius:12px; border:1px dashed #bae6fd;\">"
                + "<div style=\"text-align:center; color:#0f172a;\"><div style=\"font-size:26px; font-weight:800;\">" + NUMBER_FORMAT.get().format(total(stats))
                + "</div><div style=\"font-size:11px; color:#64748b;\">total konten</div></div></div>" + labels.toString();
    }

    private Component buildGrid(List<StatItem> stats) {
        Div wrap = new Div();
        wrap.setWidth("100%");
        wrap.setStyle("background:#fff; border:1px solid #e5e7eb; border-radius:14px; padding:10px; box-sizing:border-box; box-shadow:0 4px 14px rgba(15,23,42,.06);");

        Label title = new Label(ais.common.Common.getBahasaConfig("Rekap Grid Ringan"));
        title.setStyle("font-weight:bold; font-size:14px; color:#0f172a; margin-bottom:8px; display:block; cursor:pointer;");
        title.setTooltiptext("Klik untuk melihat ringkasan seluruh data");
        title.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                showOverviewPopup();
            }
        });
        title.setParent(wrap);

        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setParent(wrap);
        grid.setStyle("border:0;");

        Columns cols = new Columns();
        cols.setParent(grid);
        MyColumnConfig c = new MyColumnConfig();
        c.setLabel("No");
        c.setWidth("45px");
        c.setParent(cols);
        c = new MyColumnConfig();
        c.setLabel("Kelompok Data");
        c.setParent(cols);
        c = new MyColumnConfig();
        c.setLabel("Jumlah");
        c.setWidth("110px");
        c.setParent(cols);
        c = new MyColumnConfig();
        c.setLabel("Sumber Modul");
        c.setWidth("230px");
        c.setParent(cols);
        c = new MyColumnConfig();
        c.setLabel("Database");
        c.setWidth("130px");
        c.setParent(cols);

        Rows rows = new Rows();
        rows.setParent(grid);
        for (int i = 0; i < stats.size(); i++) {
            final StatItem s = stats.get(i);
            Row row = new Row();
            row.setParent(rows);
            row.setValign("top");
            row.setStyle("cursor:pointer;");
            row.setTooltiptext("Klik untuk melihat rincian " + s.label);
            row.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    showStatDetailPopup(s);
                }
            });
            row.appendChild(new Label(String.valueOf(i + 1)));
            Label label = new Label(s.label);
            label.setStyle("font-weight:bold; color:#0f172a;");
            row.appendChild(label);
            Label jumlah = new Label(NUMBER_FORMAT.get().format(s.total));
            jumlah.setStyle("font-weight:bold; color:#0f172a;");
            row.appendChild(jumlah);
            row.appendChild(new Label(s.source));
            row.appendChild(new Label(s.streaming ? "File/Streaming" : "Utama"));
        }
        return wrap;
    }

    private void showOverviewPopup() {
        List<DetailItem> rows = new ArrayList<DetailItem>();
        if (currentStats != null) {
            for (int i = 0; i < currentStats.size(); i++) {
                StatItem s = currentStats.get(i);
                rows.add(new DetailItem(s.kode, s.label, NUMBER_FORMAT.get().format(s.total) + " data", s.source,
                        s.streaming ? "File/Streaming" : "Utama"));
            }
        }
        if (currentScope != null) {
            rows.add(new DetailItem("Scope", "Pertemuan", NUMBER_FORMAT.get().format(currentScope.pertemuanIds.size()) + " data",
                    "Pertemuan aktif", "Utama"));
            rows.add(new DetailItem("Scope", "Perkuliahan", NUMBER_FORMAT.get().format(currentScope.perkuliahanIds.size()) + " data",
                    "Relasi perkuliahan", "Utama"));
            rows.add(new DetailItem("Scope", "Jadwal Pelajaran", NUMBER_FORMAT.get().format(currentScope.jadwalPelajaranIds.size()) + " data",
                    "Relasi jadwal pelajaran", "Utama"));
        }
        renderDetailWindow("Ringkasan Dasbor Info & Materi", rows, "Klik card/baris lain untuk melihat rincian per kelompok data.");
    }

    private void showScopeDetailPopup(String label, String type) {
        List<DetailItem> rows = new ArrayList<DetailItem>();
        if (currentScope == null) {
            renderDetailWindow(label, rows, "Scope belum tersedia.");
            return;
        }
        if ("pertemuan".equalsIgnoreCase(type)) {
            appendPertemuanScopeDetails(rows, currentScope.pertemuanIds);
        } else if ("perkuliahan".equalsIgnoreCase(type)) {
            appendPerkuliahanScopeDetails(rows, currentScope.perkuliahanIds);
        } else if ("jadwal".equalsIgnoreCase(type)) {
            appendJadwalPelajaranScopeDetails(rows, currentScope.jadwalPelajaranIds);
        } else {
            showOverviewPopup();
            return;
        }
        renderDetailWindow("Detail " + label, rows, buildLimitInfo(rows));
    }

    private void showStatDetailPopup(StatItem item) {
        List<DetailItem> rows = loadDetailRows(item);
        renderDetailWindow("Detail " + (item == null ? "Data" : item.label), rows, buildLimitInfo(rows));
    }

    private String buildLimitInfo(List<DetailItem> rows) {
        if (rows != null && rows.size() >= DETAIL_LIMIT) {
            return "Data dibatasi maksimal " + DETAIL_LIMIT + " baris agar popup tetap ringan. Gunakan tab utama masing-masing untuk melihat data lengkap.";
        }
        return "Popup ini menampilkan rincian cepat. Untuk pengelolaan penuh, gunakan tab/module sumber terkait.";
    }

    private void renderDetailWindow(String title, List<DetailItem> details, String note) {
        Window window = new Window(title == null ? "Detail" : title, "normal", true);
        window.setWidth(Common.isMobile() ? "96%" : "920px");
        window.setHeight(Common.isMobile() ? "86%" : "78%");
        window.setClosable(true);
        window.setSizable(true);
        window.setMaximizable(true);
        window.setPosition("center");
        try {
            window.setParent(getPage() == null ? this : getPage().getFirstRoot());
        } catch (Exception e) {
            window.setParent(this);
        }

        Borderlayout layout = new Borderlayout();
        layout.setParent(window);
        layout.setWidth("100%");
        layout.setHeight("100%");

        North north = new North();
        north.setParent(layout);
        north.setHeight("72px");
        north.setBorder("none");
        Div head = new Div();
        head.setParent(north);
        head.setStyle("background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:white; padding:12px 14px; box-sizing:border-box; height:100%;");
        long jumlah = details == null ? 0L : details.size();
        head.appendChild(new Html("<div style=\"font-size:16px; font-weight:800; line-height:1.25;\">" + escapeHtml(title) + "</div>"
                + "<div style=\"font-size:11px; opacity:.88; margin-top:4px;\">" + NUMBER_FORMAT.get().format(jumlah)
                + " baris ditampilkan</div>"));

        Center center = new Center();
        center.setParent(layout);
        ais.ui.util.ZkCompat.setFlex(center, true);
        center.setAutoscroll(true);
        center.setBorder("none");

        Div body = new Div();
        body.setParent(center);
        body.setWidth("100%");
        body.setStyle("padding:10px; box-sizing:border-box; background:#f8fafc; min-height:100%;");

        if (details == null || details.isEmpty()) {
            Html empty = new Html("<div style=\"background:#fff; border:1px dashed #cbd5e1; border-radius:14px; padding:16px; color:#64748b; text-align:center;\">Belum ada rincian data untuk kelompok ini.</div>");
            empty.setParent(body);
        } else {
            buildDetailGrid(details).setParent(body);
        }

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        south.setParent(layout);
        south.setHeight("42px");
        south.setBorder("none");
        Html footer = new Html("<div style=\"padding:9px 12px; font-size:11px; color:#64748b; background:#fff; border-top:1px solid #e5e7eb; box-sizing:border-box; height:100%;\">"
                + escapeHtml(note == null ? "" : note) + "</div>");
        footer.setParent(south);

        try {
            window.doModal();
        } catch (Exception e) {
            window.setVisible(true);
        }
    }

    private Component buildDetailGrid(List<DetailItem> details) {
        Div wrap = new Div();
        wrap.setWidth("100%");
        wrap.setStyle("background:#fff; border:1px solid #e5e7eb; border-radius:14px; padding:8px; box-sizing:border-box; box-shadow:0 4px 14px rgba(15,23,42,.05);");

        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setParent(wrap);
        grid.setStyle("border:0;");

        Columns cols = new Columns();
        cols.setParent(grid);
        MyColumnConfig c = new MyColumnConfig();
        c.setLabel("No");
        c.setWidth("45px");
        c.setParent(cols);
        c = new MyColumnConfig();
        c.setLabel("Jenis");
        c.setWidth(Common.isMobile() ? "90px" : "130px");
        c.setParent(cols);
        c = new MyColumnConfig();
        c.setLabel("Judul / Informasi");
        c.setParent(cols);
        c = new MyColumnConfig();
        c.setLabel("Relasi");
        c.setWidth(Common.isMobile() ? "120px" : "220px");
        c.setParent(cols);
        c = new MyColumnConfig();
        c.setLabel("DB");
        c.setWidth("90px");
        c.setParent(cols);

        Rows rows = new Rows();
        rows.setParent(grid);
        for (int i = 0; i < details.size(); i++) {
            DetailItem d = details.get(i);
            Row row = new Row();
            row.setValign("top");
            row.setStyle(i % 2 == 0 ? "background:#fff;" : "background:#f8fafc;");
            row.setParent(rows);
            row.appendChild(new Label(String.valueOf(i + 1)));
            row.appendChild(new Label(safe(d.jenis)));
            Label info = new Label(safe(d.info));
            info.setStyle("font-weight:bold; color:#0f172a;");
            row.appendChild(info);
            row.appendChild(new Label(safe(d.relasi)));
            row.appendChild(new Label(safe(d.database)));
        }
        return wrap;
    }

    private List<DetailItem> loadDetailRows(StatItem item) {
        List<DetailItem> rows = new ArrayList<DetailItem>();
        if (item == null || currentScope == null) {
            return rows;
        }
        if ("Info".equalsIgnoreCase(item.kode)) {
            appendPengumumanDetails(rows, currentScope);
        } else if ("Ujian".equalsIgnoreCase(item.kode)) {
            appendUjianDetails(rows, currentScope);
        } else if ("Tugas".equalsIgnoreCase(item.kode)) {
            appendTugasDetails(rows, currentScope);
        } else if ("Tgs.Kel".equalsIgnoreCase(item.kode)) {
            appendTugasKelompokDetails(rows, currentScope);
        } else if ("Materi".equalsIgnoreCase(item.kode)) {
            appendStreamingPertemuanDetails(rows, currentScope, "pertemuan_file_content", "Materi", "File materi pertemuan");
        } else if ("Video".equalsIgnoreCase(item.kode)) {
            appendStreamingPertemuanDetails(rows, currentScope, "video_pertemuan", "Video", "Video pertemuan");
        } else if ("Audio".equalsIgnoreCase(item.kode)) {
            appendStreamingPertemuanDetails(rows, currentScope, "audio_pertemuan", "Audio", "Audio pertemuan");
        } else if ("Lamp.Tugas".equalsIgnoreCase(item.kode)) {
            appendStreamingPertemuanDetails(rows, currentScope, "tugas_file_content", "Lampiran Tugas", "Lampiran tugas");
        } else if ("Buku".equalsIgnoreCase(item.kode)) {
            appendBukuReferensiDetails(rows, currentScope);
        } else if ("Diktat".equalsIgnoreCase(item.kode)) {
            appendBukuAjarDetails(rows, currentScope);
        } else if ("Artikel".equalsIgnoreCase(item.kode)) {
            appendArtikelDetails(rows, currentScope);
        }
        return rows;
    }

    private void appendPertemuanScopeDetails(final List<DetailItem> rows, final List<Long> ids) {
        if (isEmpty(ids)) {
            return;
        }
        executeForChunks(ids, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select id, pertemuan_ke, tanggal, topik, perkuliahan, jadwal_pelajaran from pertemuan where id in (" + inSql
                        + ") order by tanggal desc, id desc limit " + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    String info = "Pertemuan #" + formatObj(val(a, 0)) + " ke-" + formatObj(val(a, 1)) + " - " + formatObj(val(a, 3));
                    String relasi = "Tgl " + formatObj(val(a, 2)) + ", Perkuliahan " + formatObj(val(a, 4)) + ", Jadwal " + formatObj(val(a, 5));
                    rows.add(new DetailItem("Pertemuan", info, relasi, "pertemuan", "Utama"));
                }
            }
        });
    }

    private void appendPerkuliahanScopeDetails(final List<DetailItem> rows, final List<Long> ids) {
        if (isEmpty(ids)) {
            return;
        }
        executeForChunks(ids, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select p.id, m.kode, m.nama, p.kelas, p.tahun_ajaran from perkuliahan p left join matakuliah m on p.matakuliah=m.id where p.id in ("
                        + inSql + ") order by p.id desc limit " + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    rows.add(new DetailItem("Perkuliahan", formatObj(val(a, 1)) + " - " + formatObj(val(a, 2)),
                            "ID " + formatObj(val(a, 0)) + ", Kelas " + formatObj(val(a, 3)) + ", TA " + formatObj(val(a, 4)), "perkuliahan", "Utama"));
                }
            }
        });
    }

    private void appendJadwalPelajaranScopeDetails(final List<DetailItem> rows, final List<Long> ids) {
        if (isEmpty(ids)) {
            return;
        }
        executeForChunks(ids, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select jp.id, jp.tahun_ajaran, jp.semester, jp.hari, jp.waktumulai from sekolah.jadwal_pelajaran jp where jp.id in ("
                        + inSql + ") order by jp.id desc limit " + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    rows.add(new DetailItem("Jadwal", "Jadwal Pelajaran ID " + formatObj(val(a, 0)),
                            "TA " + formatObj(val(a, 1)) + ", SMT " + formatObj(val(a, 2)) + ", " + formatObj(val(a, 3)) + " " + formatObj(val(a, 4)), "jadwal_pelajaran", "Utama"));
                }
            }
        });
    }

    private void appendPengumumanDetails(final List<DetailItem> rows, final DashboardScope scope) {
        if (isEmpty(scope.perkuliahanIds)) {
            return;
        }
        executeForChunks(scope.perkuliahanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select id, judul, perkuliahan from pengumuman_perkuliahan where (aktif=true or aktif is null) and perkuliahan in ("
                        + inSql + ") order by id desc limit " + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    rows.add(new DetailItem("Info", formatObj(val(a, 1)), "Perkuliahan " + formatObj(val(a, 2)) + ", ID " + formatObj(val(a, 0)),
                            "pengumuman_perkuliahan", "Utama"));
                }
            }
        });
    }

    private void appendUjianDetails(final List<DetailItem> rows, final DashboardScope scope) {
        if (isEmpty(scope.pertemuanIds)) {
            return;
        }
        executeForChunks(scope.pertemuanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select x.id, x.pertemuan, x.ujian, p.pertemuan_ke, p.tanggal, p.topik from pertemuan_punya_ujian x left join pertemuan p on x.pertemuan=p.id where x.pertemuan in ("
                        + inSql + ") order by x.id desc limit " + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    rows.add(new DetailItem("Ujian", "Ujian " + formatObj(val(a, 2)), buildPertemuanText(a, 1, 3, 4, 5),
                            "pertemuan_punya_ujian", "Utama"));
                }
            }
        });
    }

    private void appendTugasDetails(final List<DetailItem> rows, final DashboardScope scope) {
        if (isEmpty(scope.pertemuanIds)) {
            return;
        }
        executeForChunks(scope.pertemuanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql1 = "select p.id, p.pertemuan_ke, p.tanggal, p.topik, p.judultugas from pertemuan p where p.id in (" + inSql
                        + ") and p.judultugas is not null and trim(p.judultugas) <> '' order by p.id desc limit " + (DETAIL_LIMIT - rows.size());
                List data1 = safeAmbilSql(sql1, false);
                for (int i = 0; data1 != null && i < data1.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data1.get(i));
                    rows.add(new DetailItem("Tugas", formatObj(val(a, 4)), buildPertemuanText(a, 0, 1, 2, 3), "pertemuan.judultugas", "Utama"));
                }
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql2 = "select x.id, x.pertemuan, x.judultugas, p.pertemuan_ke, p.tanggal, p.topik from tugas_pertemuan x left join pertemuan p on x.pertemuan=p.id where x.pertemuan in ("
                        + inSql + ") and x.judultugas is not null and trim(x.judultugas) <> '' order by x.id desc limit " + (DETAIL_LIMIT - rows.size());
                List data2 = safeAmbilSql(sql2, false);
                for (int i = 0; data2 != null && i < data2.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data2.get(i));
                    rows.add(new DetailItem("Tugas", formatObj(val(a, 2)), buildPertemuanText(a, 1, 3, 4, 5), "tugas_pertemuan", "Utama"));
                }
            }
        });
    }

    private void appendTugasKelompokDetails(final List<DetailItem> rows, final DashboardScope scope) {
        if (isEmpty(scope.pertemuanIds)) {
            return;
        }
        executeForChunks(scope.pertemuanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select x.id, x.pertemuan, p.pertemuan_ke, p.tanggal, p.topik from tugas_kelompok x left join pertemuan p on x.pertemuan=p.id where x.pertemuan in ("
                        + inSql + ") order by x.id desc limit " + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    rows.add(new DetailItem("Tugas Kelompok", "Tugas Kelompok ID " + formatObj(val(a, 0)),
                            buildPertemuanText(a, 1, 2, 3, 4), "tugas_kelompok", "Utama"));
                }
            }
        });
    }

    private void appendStreamingPertemuanDetails(final List<DetailItem> rows, final DashboardScope scope, final String tableName,
            final String jenis, final String label) {
        if (isEmpty(scope.pertemuanIds)) {
            return;
        }
        if (!columnExistsStreaming(tableName, "pertemuan")) {
            return;
        }
        final Map<Long, String> mapPertemuan = buildPertemuanLabelMap(scope.pertemuanIds);
        executeForChunks(scope.pertemuanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select id, pertemuan from " + tableName + " where pertemuan in (" + inSql + ") order by id desc limit "
                        + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, true);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    Long pid = toLongObject(val(a, 1));
                    String relasi = pid == null ? "Pertemuan -" : safe(mapPertemuan.get(pid));
                    rows.add(new DetailItem(jenis, label + " ID " + formatObj(val(a, 0)), relasi, tableName, "File/Streaming"));
                }
            }
        });
    }

    private void appendBukuReferensiDetails(final List<DetailItem> rows, final DashboardScope scope) {
        if (isEmpty(scope.perkuliahanIds)) {
            return;
        }
        executeForChunks(scope.perkuliahanIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select id, perkuliahan, item from perkuliahan_punya_item where perkuliahan in (" + inSql + ") order by id desc limit "
                        + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    rows.add(new DetailItem("Buku", "Item/Buku ID " + formatObj(val(a, 2)),
                            "Perkuliahan " + formatObj(val(a, 1)) + ", Relasi ID " + formatObj(val(a, 0)), "perkuliahan_punya_item", "Utama"));
                }
            }
        });
    }

    private void appendBukuAjarDetails(final List<DetailItem> rows, final DashboardScope scope) {
        if (isEmpty(scope.matakuliahIds)) {
            return;
        }
        executeForChunks(scope.matakuliahIds, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                if (rows.size() >= DETAIL_LIMIT) {
                    return;
                }
                String sql = "select id, matakuliah, buku_bahan_ajar from matakuliah_punya_buku_bahan_ajar where matakuliah in (" + inSql
                        + ") order by id desc limit " + (DETAIL_LIMIT - rows.size());
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
                    Object[] a = toArray(data.get(i));
                    rows.add(new DetailItem("Diktat", "Buku Bahan Ajar ID " + formatObj(val(a, 2)),
                            "Matakuliah " + formatObj(val(a, 1)) + ", Relasi ID " + formatObj(val(a, 0)), "matakuliah_punya_buku_bahan_ajar", "Utama"));
                }
            }
        });
    }

    private void appendArtikelDetails(final List<DetailItem> rows, final DashboardScope scope) {
        if (!isEmpty(scope.perkuliahanIds)) {
            executeForChunks(scope.perkuliahanIds, new ChunkExecutor() {
                @Override
                public void execute(String inSql) {
                    if (rows.size() >= DETAIL_LIMIT) {
                        return;
                    }
                    String sql = "select id, perkuliahan, artikel from data_punya_artikel where perkuliahan in (" + inSql
                            + ") order by id desc limit " + (DETAIL_LIMIT - rows.size());
                    appendArtikelSqlRows(rows, sql, "Perkuliahan");
                }
            });
        }
        if (!isEmpty(scope.jadwalPelajaranIds) && rows.size() < DETAIL_LIMIT) {
            executeForChunks(scope.jadwalPelajaranIds, new ChunkExecutor() {
                @Override
                public void execute(String inSql) {
                    if (rows.size() >= DETAIL_LIMIT) {
                        return;
                    }
                    String sql = "select id, jadwal_pelajaran, artikel from data_punya_artikel where jadwal_pelajaran in (" + inSql
                            + ") order by id desc limit " + (DETAIL_LIMIT - rows.size());
                    appendArtikelSqlRows(rows, sql, "Jadwal Pelajaran");
                }
            });
        }
    }

    private void appendArtikelSqlRows(List<DetailItem> rows, String sql, String relasiNama) {
        List data = safeAmbilSql(sql, false);
        for (int i = 0; data != null && i < data.size() && rows.size() < DETAIL_LIMIT; i++) {
            Object[] a = toArray(data.get(i));
            rows.add(new DetailItem("Artikel", "Artikel ID " + formatObj(val(a, 2)),
                    relasiNama + " " + formatObj(val(a, 1)) + ", Relasi ID " + formatObj(val(a, 0)), "data_punya_artikel", "Utama"));
        }
    }

    private Map<Long, String> buildPertemuanLabelMap(final List<Long> ids) {
        final Map<Long, String> map = new HashMap<Long, String>();
        if (isEmpty(ids)) {
            return map;
        }
        executeForChunks(ids, new ChunkExecutor() {
            @Override
            public void execute(String inSql) {
                String sql = "select id, pertemuan_ke, tanggal, topik from pertemuan where id in (" + inSql + ")";
                List data = safeAmbilSql(sql, false);
                for (int i = 0; data != null && i < data.size(); i++) {
                    Object[] a = toArray(data.get(i));
                    Long id = toLongObject(val(a, 0));
                    if (id != null) {
                        map.put(id, buildPertemuanText(a, 0, 1, 2, 3));
                    }
                }
            }
        });
        return map;
    }

    private String buildPertemuanText(Object[] a, int idxId, int idxKe, int idxTanggal, int idxTopik) {
        return "Pertemuan " + formatObj(val(a, idxId)) + ", Ke-" + formatObj(val(a, idxKe)) + ", "
                + formatObj(val(a, idxTanggal)) + ", " + formatObj(val(a, idxTopik));
    }

    private List safeAmbilSql(String sql, boolean streaming) {
        try {
            return streaming ? Common.ambilSqlStreaming(sql) : Common.ambilSql(sql);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList();
        }
    }

    private Object[] toArray(Object obj) {
        if (obj instanceof Object[]) {
            return (Object[]) obj;
        }
        return new Object[] { obj };
    }

    private Object val(Object[] arr, int idx) {
        return arr == null || idx < 0 || idx >= arr.length ? null : arr[idx];
    }

    private String formatObj(Object obj) {
        if (obj == null) {
            return "";
        }
        if (obj instanceof Date) {
            try {
                return Common.dateFormat6.get().format((Date) obj);
            } catch (Exception e) {
                return obj.toString();
            }
        }
        return obj.toString();
    }

    private void renderError(Exception e) {
        Html html = new Html("<div style=\"background:#fff1f2; border:1px solid #fecdd3; color:#9f1239; border-radius:14px; padding:15px;\">"
                + "<div style=\"font-weight:800; font-size:14px; margin-bottom:6px;\">Gagal memuat Dasbor Info & Materi</div>"
                + "<div style=\"font-size:12px; line-height:1.5;\">" + escapeHtml(e == null ? "" : e.getMessage()) + "</div></div>");
        html.setParent(mainContainer);
    }

    private void updateStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setValue(text == null ? "" : text);
        }
    }

    private boolean isEmpty(List<Long> ids) {
        return ids == null || ids.isEmpty();
    }

    private long total(List<StatItem> stats) {
        long total = 0L;
        if (stats != null) {
            for (int i = 0; i < stats.size(); i++) {
                total += stats.get(i).total;
            }
        }
        return total;
    }

    private long max(List<StatItem> stats) {
        long max = 0L;
        if (stats != null) {
            for (int i = 0; i < stats.size(); i++) {
                if (stats.get(i).total > max) {
                    max = stats.get(i).total;
                }
            }
        }
        return max;
    }

    private long firstLong(List data) {
        if (data == null || data.isEmpty()) {
            return 0L;
        }
        Object obj = data.get(0);
        if (obj instanceof Object[]) {
            Object[] arr = (Object[]) obj;
            return arr.length == 0 ? 0L : toLong(arr[0]);
        }
        return toLong(obj);
    }

    private List<Long> toLongList(List data) {
        List<Long> result = new ArrayList<Long>();
        if (data == null) {
            return result;
        }
        for (int i = 0; i < data.size(); i++) {
            Object obj = data.get(i);
            if (obj instanceof Object[]) {
                Object[] arr = (Object[]) obj;
                if (arr.length > 0) {
                    Long val = toLongObject(arr[0]);
                    if (val != null && !result.contains(val)) {
                        result.add(val);
                    }
                }
            } else {
                Long val = toLongObject(obj);
                if (val != null && !result.contains(val)) {
                    result.add(val);
                }
            }
        }
        return result;
    }

    private long toLong(Object obj) {
        Long val = toLongObject(obj);
        return val == null ? 0L : val.longValue();
    }

    private Long toLongObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return Long.valueOf(((Number) obj).longValue());
        }
        try {
            String s = obj.toString().trim();
            if (s.length() == 0) {
                return null;
            }
            return Long.valueOf(Long.parseLong(s));
        } catch (Exception e) {
            return null;
        }
    }

    private String sqlEscape(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static interface ChunkExecutor {
        void execute(String inSql);
    }

    private static class DashboardScope {
        String tahunAkademik;
        Sekolah sekolah;
        List<Long> pertemuanIds = new ArrayList<Long>();
        List<Long> perkuliahanIds = new ArrayList<Long>();
        List<Long> jadwalPelajaranIds = new ArrayList<Long>();
        List<Long> matakuliahIds = new ArrayList<Long>();
    }

    private static class StatItem {
        String kode;
        String label;
        long total;
        String source;
        boolean streaming;

        StatItem(String kode, String label, long total, String source, boolean streaming) {
            this.kode = kode;
            this.label = label;
            this.total = total;
            this.source = source;
            this.streaming = streaming;
        }
    }

    private static class DetailItem {
        String jenis;
        String info;
        String relasi;
        String source;
        String database;

        DetailItem(String jenis, String info, String relasi, String source, String database) {
            this.jenis = jenis;
            this.info = info;
            this.relasi = relasi;
            this.source = source;
            this.database = database;
        }
    }
}
