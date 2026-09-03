package ais.action.master;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Html;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ErrorLog;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk error log. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code int DASHBOARD_LIMIT}, {@code int
 * TREND_DAYS}, {@code int EXPORT_CHUNK_SIZE_DEFAULT}, {@code int EXPORT_LIMIT_DEFAULT}, {@code Paging paging},
 * {@code MyGrid grid}, {@code Html dashboardHtml}, {@code Html progressHtml}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initDefaultTanggal()}, {@code initCriteria()}, {@code
 * initCriteria()}, {@code buatSumberLaporanError()}); pembacaan/pencarian ({@code getFilterTanggalMulai()},
 * {@code getFilterTanggalSampaiEksklusif()}, {@code downloadFilteredErrors()}, {@code
 * isHanyaTampilkanErrorBerbeda()}, {@code getDistinctScanLimit()}, {@code getDistinctChunkSize()}); mutasi data
 * ({@code updatePagingSafely()}); penghapusan/pembatalan ({@code confirmDeleteAll()}, {@code deleteAllLogs()});
 * pelaporan/ekspor ({@code buildFilteredErrorReport()}, {@code buildErrorFingerprint()}, {@code
 * normalizeFingerprintLine()}, {@code addIfFingerprintBaru()}); operasi domain lain ({@code
 * appendToolbarButtons()}, {@code rollbackQuietly()}, {@code copyErrorLogToClipboard()}, {@code
 * buildCopyText()}, {@code copyFilteredAiPromptToClipboard()}, {@code countFilteredRowsLong()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class ErrorLogAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

    private static final long serialVersionUID = -5779730267402400328L;
    private static final int DASHBOARD_LIMIT = 1000;
    private static final int TREND_DAYS = 14;
    private static final int EXPORT_CHUNK_SIZE_DEFAULT = 100;
    private static final int EXPORT_LIMIT_DEFAULT = 0;


    private Paging paging;
    private MyGrid grid;
    private Html dashboardHtml;
    private Html progressHtml;
    private Component rootComponent;

    private Textbox searchnama;
    private Textbox searchnamaTidak;
    private MyDatebox start;
    private MyDatebox end;
    private Checkbox hanyaTampilkanErrorBerbeda;

    private MyToolbarbuttonConfig find;

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        this.rootComponent = comp;
        Common.initLaguage();

        initDefaultTanggal();

        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(null);
            }
        });

        if (rootComponent != null) {
            rootComponent.addEventListener("onLoadErrorLogs", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    doSearchDefault(event);
                }
            });
        }

        appendToolbarButtons();
        onSearchDefault(null);
    }

    /** Default filter tanggal: 1 bulan terakhir sampai besok. */
    private void initDefaultTanggal() {
        try {
            if (start != null) {
                if (start != null) start.setReadonly(true);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MONTH, -1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (start != null) start.setValue(calendar.getTime());
            }
            if (end != null) {
                if (end != null) end.setReadonly(true);
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (end != null) end.setValue(calendar.getTime());
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private Date getFilterTanggalMulai() {
        try {
            return start == null ? null : start.getValue();
        } catch (Exception e) {
            return null;
        }
    }

    /** Tanggal sampai bersifat inklusif: dipakai sebagai batas < (sampai + 1 hari). */
    private Date getFilterTanggalSampaiEksklusif() {
        try {
            Date value = end == null ? null : end.getValue();
            if (value == null) {
                return null;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(value);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.DATE, 1);
            return calendar.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private void appendToolbarButtons() {
        if (find == null || find.getParent() == null) {
            return;
        }

        // Laporan modern (Cetak + Ekspor Excel + progress + grafik HTML/CSS) via mesin reuse DashboardReportKit.
        ais.action.master.helper.DashboardReportKit.pasangTombol(find.getParent(), self, buatSumberLaporanError());

        String[] contents = new String[] { "id", "tanggal_dirubah", "olehId", "oleh", "keterangan" };
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ErrorLog.class, this, contents);
        cetakToolbarbutton.setLabel("Download Data Standar");
        cetakToolbarbutton.setTooltiptext("Download standar disembunyikan agar semua download mengikuti filter aktif.");
        cetakToolbarbutton.setVisible(false);
        find.getParent().appendChild(cetakToolbarbutton);

        hanyaTampilkanErrorBerbeda = new Checkbox("Hanya Tampilkan Error yang berbeda");
        hanyaTampilkanErrorBerbeda.setTooltiptext("Jika aktif, error dengan exception/stack trace yang mirip hanya ditampilkan satu kali.");
        hanyaTampilkanErrorBerbeda.setStyle("font-size:11px;font-weight:bold;margin-left:8px;margin-right:8px;color:#0f172a;");
        hanyaTampilkanErrorBerbeda.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (paging != null) {
                    paging.setActivePage(0);
                }
                onSearchDefault(event);
            }
        });
        find.getParent().appendChild(hanyaTampilkanErrorBerbeda);

        MyToolbarbuttonConfig downloadSemua = new MyToolbarbuttonConfig("Download Error Sesuai Filter", "/img/svg/download.svg");
        downloadSemua.setTooltiptext("Download semua catatan error yang sesuai filter pencarian saat ini dalam format teks");
        downloadSemua.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                downloadFilteredErrors(false);
            }
        });
        find.getParent().appendChild(downloadSemua);

        MyToolbarbuttonConfig downloadAi = new MyToolbarbuttonConfig("Download Instruksi AI Semua Error", "/img/svg/download.svg");
        downloadAi.setTooltiptext("Download prompt lengkap untuk AI agar semua error sesuai filter dianalisa dan diperbaiki sekaligus");
        downloadAi.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                downloadFilteredErrors(true);
            }
        });
        find.getParent().appendChild(downloadAi);

        MyToolbarbuttonConfig copyAi = new MyToolbarbuttonConfig("Copy Instruksi AI Semua", "/img/svg/copy.svg");
        copyAi.setTooltiptext("Salin prompt AI berisi semua error sesuai filter ke clipboard");
        copyAi.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                copyFilteredAiPromptToClipboard();
            }
        });
        find.getParent().appendChild(copyAi);

        MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Muat Ulang", "/img/svg/refresh.svg");
        refresh.setTooltiptext("Muat ulang daftar dan dasbor error");
        refresh.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(arg0);
            }
        });
        find.getParent().appendChild(refresh);

        MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
        hapus.setTooltiptext("Hapus seluruh catatan error yang tersimpan di database");
        hapus.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                confirmDeleteAll();
            }
        });
        find.getParent().appendChild(hapus);
    }

    private void confirmDeleteAll() {
        try {
            MyMessageboxConfig.show("Mohon perhatian Bapak/Ibu, seluruh catatan kesalahan (error log) yang tersimpan di basis data akan dihapus secara permanen dan tidak dapat dikembalikan lagi. Apakah Bapak/Ibu yakin ingin melanjutkan penghapusan seluruh data kesalahan ini?", "Konfirmasi",
                    Messagebox.YES | Messagebox.NO, MyMessageboxConfig.QUESTION, new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            if ("onYes".equals(event.getName())) {
                                deleteAllLogs();
                                onSearchDefault(event);
                            }
                        }
                    });
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void deleteAllLogs() {
        Session session = null;
        Transaction transaction = null;
        boolean suksesTruncate = false;
        try {
            showProgress(20, "Menghapus ErrorLog", "Mengosongkan tabel error_log dengan TRUNCATE agar proses lebih cepat.");
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.createSQLQuery("TRUNCATE TABLE error_log RESTART IDENTITY").executeUpdate();
            transaction.commit();
            suksesTruncate = true;
            // Log dihapus dari DB -> kosongkan penanda dedup in-memory agar setiap lokasi error
            // (kelas+baris, termasuk error JSP) boleh dicatat lagi 1x.
            ais.common.ErrorAuditUtil.resetDedup();
            try {
                MyMessageboxConfig.show("Seluruh catatan kesalahan berhasil dihapus dengan cepat menggunakan metode TRUNCATE. Basis data kini telah bersih dari seluruh data kesalahan sebelumnya.");
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:276");
            }
        } catch (Exception e) {
            rollbackQuietly(transaction);
            closeSessionQuietly(session);
            session = null;
            transaction = null;
            try {
                showProgress(55, "TRUNCATE gagal", "Mencoba fallback DELETE karena database menolak TRUNCATE.");
                session = HibernateUtil.getSessionFactory().openSession();
                transaction = session.beginTransaction();
                session.createSQLQuery("delete from error_log").executeUpdate();
                transaction.commit();
                // Log dihapus (fallback DELETE) -> reset dedup agar lokasi error boleh tercatat lagi.
                ais.common.ErrorAuditUtil.resetDedup();
                try {
                    MyMessageboxConfig.show("Seluruh catatan kesalahan berhasil dihapus. Proses TRUNCATE tidak dapat dijalankan, sehingga sistem secara otomatis menggunakan metode DELETE sebagai cadangan. Tidak diperlukan tindakan tambahan dari Bapak/Ibu.");
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:291");
                }
            } catch (Exception ex) {
                rollbackQuietly(transaction);
                Common.tampilErrorJikaAdmin(ex);
            }
        } finally {
            closeSessionQuietly(session);
            hideProgress();
        }
    }

    private void rollbackQuietly(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        try {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:311");
        }
    }

    /**
     * Renderer lokal untuk layar/komponen {@link ErrorLogAction}. Kelas ini menerjemahkan satu item data menjadi
     * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link ErrorLogAction} dan dapat mengakses state
     * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see ErrorLogAction
     */
    class ErrorLogRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object data) throws Exception {
            row.setValign("top");
            final ErrorLog errorLog = (ErrorLog) data;
            String waktu = errorLog.getTanggal_dirubah() == null ? "-" : dateTimeFormat.format(errorLog.getTanggal_dirubah());
            String kategori = errorLog.getKategoriSederhana();
            String ringkasan = errorLog.getRingkasan();

            StringBuilder left = new StringBuilder();
            left.append("<div style='padding:10px 8px;'>");
            left.append("<div style='font-weight:800;color:#0f172a;font-size:13px;'>").append(escapeHtml(waktu)).append("</div>");
            left.append("<div style='margin-top:6px;display:inline-block;padding:3px 8px;border-radius:999px;background:#eef2ff;color:#3730a3;font-size:11px;font-weight:700;'>")
                    .append(escapeHtml(kategori)).append("</div>");
            left.append("<div style='margin-top:8px;color:#64748b;font-size:11px;'>ID: ").append(errorLog.getId()).append("</div>");
            if (errorLog.getOlehId() != null && errorLog.getOlehId().trim().length() > 0) {
                left.append("<div style='margin-top:4px;color:#64748b;font-size:11px;'>User: ")
                        .append(escapeHtml(errorLog.getOlehId())).append("</div>");
            }
            left.append("</div>");
            new MyHtml(left.toString()).setParent(row);

            Vbox rightBox = new Vbox();
            rightBox.setParent(row);
            rightBox.setWidth("100%");
            rightBox.setStyle("padding:10px 8px;box-sizing:border-box;gap:6px;");

            Hbox copyToolbar = new Hbox();
            copyToolbar.setParent(rightBox);
            copyToolbar.setWidth("100%");
            copyToolbar.setStyle("gap:6px;flex-wrap:wrap;align-items:center;margin-bottom:6px;");

            MyToolbarbuttonConfig copyRingkasan = new MyToolbarbuttonConfig("Copy Ringkasan", "/img/svg/copy.svg");
            copyRingkasan.setTooltiptext("Salin ringkasan error singkat ke clipboard");
            copyRingkasan.setStyle("font-size:11px;font-weight:bold;border-radius:8px;padding:4px 8px;background:#eff6ff;color:#1d4ed8;border:1px solid #bfdbfe;");
            copyRingkasan.setParent(copyToolbar);
            copyRingkasan.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    copyErrorLogToClipboard(errorLog, false);
                }
            });

            MyToolbarbuttonConfig copyLengkap = new MyToolbarbuttonConfig("Copy Lengkap untuk AI", "/img/svg/copy.svg");
            copyLengkap.setTooltiptext("Salin detail lengkap error, termasuk waktu, kategori, user, dan stack trace");
            copyLengkap.setStyle("font-size:11px;font-weight:bold;border-radius:8px;padding:4px 8px;background:#0f172a;color:#ffffff;border:1px solid #0f172a;");
            copyLengkap.setParent(copyToolbar);
            copyLengkap.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    copyErrorLogToClipboard(errorLog, true);
                }
            });

            StringBuilder right = new StringBuilder();
            right.append("<div style='width:100%;box-sizing:border-box;'>");
            right.append("<div style='font-weight:700;color:#1e293b;margin-bottom:8px;'>")
                    .append(escapeHtml(ringkasan)).append("</div>");
            right.append("<pre style='white-space:pre-wrap;word-break:break-word;background:#0f172a;color:#e2e8f0;border-radius:10px;padding:12px;line-height:1.45;font-size:11px;max-height:260px;overflow:auto;box-sizing:border-box;'>")
                    .append(escapeHtml(errorLog.getKeterangan())).append("</pre>");
            right.append("</div>");
            new MyHtml(right.toString()).setParent(rightBox);
        }
    }

    private void copyErrorLogToClipboard(ErrorLog errorLog, boolean full) {
        try {
            String text = buildCopyText(errorLog, full);
            Clients.evalJavaScript(buildCopyToClipboardScript(text));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private String buildCopyText(ErrorLog errorLog, boolean full) {
        if (errorLog == null) {
            return "";
        }
        String waktu = errorLog.getTanggal_dirubah() == null ? "-" : dateTimeFormat.format(errorLog.getTanggal_dirubah());
        StringBuilder text = new StringBuilder(4096);
        text.append("Tolong analisa penyebab error berikut dan berikan solusi perbaikannya.\n");
        text.append("Pertahankan Java 1.7, ZKoss 5.5. Terapkan perbaikan LANGSUNG ke file kode yang sudah ada di repository (edit in-place) sesuai package /java/ais.* masing-masing — JANGAN membuat file ZIP baru.\n\n");
        text.append("=== ERROR LOG ECAMPUS ===\n");
        text.append("ID        : ").append(errorLog.getId()).append('\n');
        text.append("Waktu     : ").append(waktu).append('\n');
        text.append("Kategori  : ").append(nullToEmpty(errorLog.getKategoriSederhana())).append('\n');
        text.append("Ringkasan : ").append(nullToEmpty(errorLog.getRingkasan())).append('\n');
        if (errorLog.getOlehId() != null && errorLog.getOlehId().trim().length() > 0) {
            text.append("Oleh ID   : ").append(errorLog.getOlehId()).append('\n');
        }
        if (errorLog.getOleh() != null && errorLog.getOleh().trim().length() > 0) {
            text.append("Oleh      : ").append(errorLog.getOleh()).append('\n');
        }
        if (full) {
            text.append("\n=== DETAIL / STACK TRACE ===\n");
            text.append(nullToEmpty(errorLog.getKeterangan()));
        }
        return text.toString();
    }

    private void downloadFilteredErrors(boolean aiMode) {
        try {
            showProgress(10, "Menyiapkan download", "Mengumpulkan error sesuai filter yang sedang aktif.");
            String report = buildFilteredErrorReport(aiMode);
            String prefix = aiMode ? "prompt_ai_semua_error_" : "error_log_filter_";
            String fileName = prefix + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
            showProgress(90, "Membuat file", "File teks sedang disiapkan untuk diunduh.");
            Filedownload.save(report.getBytes("UTF-8"), "text/plain;charset=UTF-8", fileName);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            hideProgress();
        }
    }

    private void copyFilteredAiPromptToClipboard() {
        try {
            showProgress(10, "Menyiapkan teks AI", "Mengumpulkan error sesuai filter yang sedang aktif.");
            String report = buildFilteredErrorReport(true);
            showProgress(90, "Menyalin", "Teks instruksi AI sedang disalin ke clipboard.");
            Clients.evalJavaScript(buildCopyToClipboardScript(report));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            hideProgress();
        }
    }

    @SuppressWarnings("unchecked")
    private String buildFilteredErrorReport(boolean aiMode) {
        StringBuilder text = new StringBuilder(65536);
        String filterAda = getFilterAda();
        String filterTidak = getFilterTidak();
        boolean distinctMode = isHanyaTampilkanErrorBerbeda();
        int limit = getKonfigurasiInt("errorlog_export_limit", EXPORT_LIMIT_DEFAULT);
        int chunkSize = getKonfigurasiInt("errorlog_export_chunk_size", EXPORT_CHUNK_SIZE_DEFAULT);
        if (chunkSize <= 0) {
            chunkSize = EXPORT_CHUNK_SIZE_DEFAULT;
        }
        if (chunkSize > 1000) {
            chunkSize = 1000;
        }

        Long totalFiltered = distinctMode ? Long.valueOf(countDistinctFilteredRows()) : countFilteredRowsLong();

        if (aiMode) {
            appendAiInstructionHeader(text, totalFiltered, filterAda, filterTidak, limit);
            if (distinctMode) {
                text.append("Mode error berbeda: AKTIF. Error dengan fingerprint/stack trace mirip hanya disertakan satu kali.\n");
            }
        } else {
            text.append("DOWNLOAD ERROR LOG ECAMPUS SESUAI FILTER\n");
            text.append("Dibuat pada : ").append(dateTimeFormat.format(new Date())).append('\n');
            text.append("Filter ada  : ").append(filterAda.length() == 0 ? "-" : filterAda).append('\n');
            text.append("Filter tanpa: ").append(filterTidak.length() == 0 ? "-" : filterTidak).append('\n');
            text.append("Periode     : ").append(buildInfoPeriode()).append('\n');
            text.append("Hanya error berbeda: ").append(distinctMode ? "YA" : "TIDAK").append('\n');
            text.append("Total sesuai filter: ").append(totalFiltered).append('\n');
            if (limit > 0) {
                text.append("Batas download: ").append(limit).append(" catatan terbaru\n");
            }
            text.append("\n");
        }

        int offset = 0;
        int nomor = 1;
        int processed = 0;
        int scanned = 0;
        int scanLimit = distinctMode ? getDistinctScanLimit() : 0;
        Map<String, Boolean> fingerprints = distinctMode ? new HashMap<String, Boolean>() : null;
        boolean selesai = false;
        while (!selesai) {
            int maxThisChunk = chunkSize;
            if (!distinctMode && limit > 0) {
                int remaining = limit - processed;
                if (remaining <= 0) {
                    break;
                }
                maxThisChunk = remaining < chunkSize ? remaining : chunkSize;
            }
            if (distinctMode && scanLimit > 0 && scanned + maxThisChunk > scanLimit) {
                maxThisChunk = scanLimit - scanned;
            }
            if (maxThisChunk <= 0) {
                break;
            }
            Session session = null;
            List<ErrorLog> logs = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                logs = initCriteria(session, true).setFirstResult(offset).setMaxResults(maxThisChunk).list();
                if (logs == null || logs.isEmpty()) {
                    break;
                }
                for (int i = 0; i < logs.size(); i++) {
                    ErrorLog log = logs.get(i);
                    if (log == null) {
                        continue;
                    }
                    if (distinctMode && !addIfFingerprintBaru(fingerprints, log)) {
                        continue;
                    }
                    appendErrorBlock(text, log, nomor, aiMode);
                    nomor++;
                    processed++;
                    if (limit > 0 && processed >= limit) {
                        selesai = true;
                        break;
                    }
                }
                offset += logs.size();
                scanned += logs.size();
                int percent = totalFiltered == null || totalFiltered.longValue() <= 0L ? 50
                        : (int) Math.min(85L, Math.round((processed * 80.0) / totalFiltered.longValue()) + 10L);
                showProgress(percent, "Mengumpulkan error", "Sudah memproses " + processed + " dari " + totalFiltered
                        + (distinctMode ? " error berbeda." : " catatan."));
                if (logs.size() < maxThisChunk || (distinctMode && scanLimit > 0 && scanned >= scanLimit)) {
                    break;
                }
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
                break;
            } finally {
                closeSessionQuietly(session);
            }
        }

        if (processed == 0) {
            text.append("Tidak ada catatan error yang sesuai dengan filter saat ini.\n");
        }
        if (aiMode) {
            text.append("\n=== AKHIR DATA ERROR ===\n");
            text.append("Terapkan seluruh perbaikan LANGSUNG ke file kode yang sudah ada di repository (edit in-place), JANGAN membuat file ZIP baru sebagai hasil.\n");
        }
        return text.toString();
    }

    private Long countFilteredRowsLong() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = initCriteria(session, false);
            criteria.setProjection(Projections.rowCount());
            Object value = criteria.uniqueResult();
            if (value instanceof Long) {
                return (Long) value;
            }
            if (value instanceof Number) {
                return Long.valueOf(((Number) value).longValue());
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeSessionQuietly(session);
        }
        return Long.valueOf(0);
    }

    private void appendAiInstructionHeader(StringBuilder text, Long totalFiltered, String filterAda, String filterTidak, int limit) {
        text.append("Tolong analisa dan perbaiki SEMUA error ECAMPUS berikut sekaligus, jangan satu per satu.\n\n");
        text.append("Tugas yang harus dilakukan:\n");
        text.append("1. Kelompokkan error yang penyebabnya sama agar perbaikan tidak berulang.\n");
        text.append("2. Temukan akar masalah dari stack trace, bukan hanya baris terakhir.\n");
        text.append("3. Pertahankan semua fungsi lama tetap berjalan. Jangan matikan logic dan jangan hanya memberi komentar tanpa perbaikan.\n");
        text.append("4. Jika ada openSession() atau currentNativeSession(), tutup di finally dengan clear/disconnect/close. Jika memakai currentSession(), jangan ditutup manual.\n");
        text.append("5. Tetap kompatibel Java 1.7 dan gaya Java 1.6: jangan memakai lambda, try-with-resources, diamond operator, Stream API, atau fitur Java 8+.\n");
        text.append("6. Jika ada file ZUL/JSP/CSS yang perlu diperbaiki, susun sesuai folder webapp yang benar.\n");
        text.append("7. Terapkan perbaikan LANGSUNG ke file kode yang sudah ada di repository (edit in-place) sesuai package /java/ais.* masing-masing. JANGAN membuat file ZIP baru sebagai hasil pekerjaan.\n");
        text.append("8. Sertakan laporan singkat isi perubahan dan alasan perbaikannya.\n\n");
        text.append("Filter ada  : ").append(filterAda.length() == 0 ? "-" : filterAda).append('\n');
        text.append("Filter tanpa: ").append(filterTidak.length() == 0 ? "-" : filterTidak).append('\n');
        text.append("Periode     : ").append(buildInfoPeriode()).append('\n');
        text.append("Total sesuai filter: ").append(totalFiltered).append('\n');
        if (limit > 0) {
            text.append("Catatan yang disertakan dibatasi konfigurasi errorlog_export_limit: ").append(limit).append(" catatan terbaru.\n");
        }
        text.append("\n=== DATA ERROR LOG ECAMPUS ===\n");
    }

    private void appendErrorBlock(StringBuilder text, ErrorLog errorLog, int nomor, boolean aiMode) {
        String waktu = errorLog.getTanggal_dirubah() == null ? "-" : dateTimeFormat.format(errorLog.getTanggal_dirubah());
        text.append("\n------------------------------------------------------------\n");
        text.append(aiMode ? "ERROR KE-" : "CATATAN KE-").append(nomor).append('\n');
        text.append("ID        : ").append(errorLog.getId()).append('\n');
        text.append("Waktu     : ").append(waktu).append('\n');
        text.append("Kategori  : ").append(nullToEmpty(errorLog.getKategoriSederhana())).append('\n');
        text.append("Ringkasan : ").append(nullToEmpty(errorLog.getRingkasan())).append('\n');
        if (errorLog.getOlehId() != null && errorLog.getOlehId().trim().length() > 0) {
            text.append("Oleh ID   : ").append(errorLog.getOlehId()).append('\n');
        }
        if (errorLog.getOleh() != null && errorLog.getOleh().trim().length() > 0) {
            text.append("Oleh      : ").append(errorLog.getOleh()).append('\n');
        }
        text.append("\nDETAIL / STACK TRACE:\n");
        text.append(nullToEmpty(errorLog.getKeterangan())).append('\n');
    }

    private String buildCopyToClipboardScript(String text) {
        String escaped = escapeJavaScriptString(text);
        StringBuilder js = new StringBuilder(escaped.length() + 1800);
        js.append("(function(){");
        js.append("var text=\"").append(escaped).append("\";");
        js.append("function toast(msg){try{");
        js.append("var d=document.createElement('div');");
        js.append("d.style.cssText='position:fixed;right:18px;bottom:18px;z-index:999999;background:#0f172a;color:#fff;padding:10px 14px;border-radius:12px;font:12px Arial,Helvetica,sans-serif;box-shadow:0 14px 32px rgba(15,23,42,.28);max-width:360px;';");
        js.append("d.innerHTML=msg;document.body.appendChild(d);setTimeout(function(){try{document.body.removeChild(d);}catch(e){}},1800);");
        js.append("}catch(e){}}");
        js.append("function fallback(){var ta=document.createElement('textarea');ta.value=text;ta.setAttribute('readonly','readonly');ta.style.position='fixed';ta.style.left='-9999px';ta.style.top='0';document.body.appendChild(ta);ta.focus();ta.select();try{var ok=document.execCommand('copy');toast(ok?'Error berhasil disalin ke clipboard':'Silakan salin manual dari kotak yang muncul');if(!ok){window.prompt('Copy error:',text);}}catch(e){window.prompt('Copy error:',text);}try{document.body.removeChild(ta);}catch(e){}}");
        js.append("if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(text).then(function(){toast('Error berhasil disalin ke clipboard');},function(){fallback();});}else{fallback();}");
        js.append("})();");
        return js.toString();
    }

    private String escapeJavaScriptString(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 32);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c < 32) {
                String hex = Integer.toHexString(c);
                sb.append("\\u");
                for (int j = hex.length(); j < 4; j++) {
                    sb.append('0');
                }
                sb.append(hex);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String buildInfoPeriode() {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        Date mulai = getFilterTanggalMulai();
        Date sampai = end == null ? null : end.getValue();
        if (mulai == null && sampai == null) {
            return "semua tanggal";
        }
        return (mulai == null ? "awal" : format.format(mulai)) + " s/d "
                + (sampai == null ? "sekarang" : format.format(sampai));
    }


    private boolean isHanyaTampilkanErrorBerbeda() {
        try {
            return hanyaTampilkanErrorBerbeda != null && hanyaTampilkanErrorBerbeda.isChecked();
        } catch (Exception e) {
            return false;
        }
    }

    private int getDistinctScanLimit() {
        int limit = getKonfigurasiInt("errorlog_distinct_scan_limit", 5000);
        return limit < 0 ? 5000 : limit;
    }

    private int getDistinctChunkSize() {
        int chunk = getKonfigurasiInt("errorlog_distinct_chunk_size", 200);
        if (chunk <= 0) {
            chunk = 200;
        }
        return chunk > 1000 ? 1000 : chunk;
    }

    private String getFingerprint(ErrorLog errorLog) {
        if (errorLog == null) {
            return "";
        }
        try {
            return errorLog.getFingerprint();
        } catch (Exception e) {
            return buildErrorFingerprint(errorLog.getKeterangan());
        }
    }

    private String buildErrorFingerprint(String value) {
        if (value == null) {
            return "";
        }
        String text = value.toLowerCase();
        text = text.replace('\r', '\n');
        String[] lines = text.split("\\n");
        StringBuilder key = new StringBuilder(2048);
        boolean adaException = false;
        int stackCount = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.indexOf(" at ") == 0 || line.indexOf("at ") == 0 || line.indexOf("caused by:") == 0
                    || line.indexOf("exception") >= 0 || line.indexOf("error") >= 0) {
                line = normalizeFingerprintLine(line);
                if (line.indexOf("exception") >= 0 || line.indexOf("error") >= 0 || line.indexOf("caused by:") == 0) {
                    adaException = true;
                }
                if (line.indexOf("at ais.") == 0 || line.indexOf("at org.") == 0 || line.indexOf("at com.") == 0
                        || line.indexOf("at javax.") == 0 || line.indexOf("at java.") == 0) {
                    stackCount++;
                }
                key.append(line).append('\n');
                if (stackCount >= 12 && adaException) {
                    break;
                }
            }
        }
        if (key.length() == 0) {
            key.append(normalizeFingerprintLine(text));
        }
        String result = key.toString().trim();
        return result.length() > 3500 ? result.substring(0, 3500) : result;
    }

    private String normalizeFingerprintLine(String line) {
        if (line == null) {
            return "";
        }
        String value = line.toLowerCase().trim();
        value = value.replaceAll("\\$\\$_javassist_[0-9]+", "\\$\\$_javassist");
        value = value.replaceAll("\\(.*?java:[0-9]+\\)", "(.java:*)");
        value = value.replaceAll("line [0-9]+", "line *");
        value = value.replaceAll("id[=: ]+[0-9]+", "id=*");
        value = value.replaceAll("\\b[0-9]{2,}\\b", "#");
        value = value.replaceAll("\\s+", " ");
        return value;
    }

    private boolean addIfFingerprintBaru(Map<String, Boolean> fingerprints, ErrorLog log) {
        if (fingerprints == null || log == null) {
            return false;
        }
        String key = getFingerprint(log);
        if (key == null || key.trim().length() == 0) {
            key = "id:" + log.getId();
        }
        if (fingerprints.containsKey(key)) {
            return false;
        }
        fingerprints.put(key, Boolean.TRUE);
        return true;
    }

    @SuppressWarnings("unchecked")
    private int countDistinctFilteredRows() {
        Map<String, Boolean> fingerprints = new HashMap<String, Boolean>();
        int offset = 0;
        int scanned = 0;
        int scanLimit = getDistinctScanLimit();
        int chunkSize = getDistinctChunkSize();
        while (true) {
            int maxThisChunk = chunkSize;
            if (scanLimit > 0 && scanned + maxThisChunk > scanLimit) {
                maxThisChunk = scanLimit - scanned;
            }
            if (maxThisChunk <= 0) {
                break;
            }
            Session session = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                List<ErrorLog> logs = initCriteria(session, true).setFirstResult(offset).setMaxResults(maxThisChunk).list();
                if (logs == null || logs.isEmpty()) {
                    break;
                }
                for (int i = 0; i < logs.size(); i++) {
                    addIfFingerprintBaru(fingerprints, logs.get(i));
                }
                offset += logs.size();
                scanned += logs.size();
                if (logs.size() < maxThisChunk || (scanLimit > 0 && scanned >= scanLimit)) {
                    break;
                }
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
                break;
            } finally {
                closeSessionQuietly(session);
            }
        }
        return fingerprints.size();
    }

    @SuppressWarnings("unchecked")
    private List<ErrorLog> loadDistinctCurrentPageLogs(int first, int pageSize, int[] totalHolder) {
        List<ErrorLog> result = new ArrayList<ErrorLog>();
        Map<String, Boolean> fingerprints = new HashMap<String, Boolean>();
        int offset = 0;
        int scanned = 0;
        int uniqueIndex = 0;
        int scanLimit = getDistinctScanLimit();
        int chunkSize = getDistinctChunkSize();
        while (true) {
            int maxThisChunk = chunkSize;
            if (scanLimit > 0 && scanned + maxThisChunk > scanLimit) {
                maxThisChunk = scanLimit - scanned;
            }
            if (maxThisChunk <= 0) {
                break;
            }
            Session session = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                List<ErrorLog> logs = initCriteria(session, true).setFirstResult(offset).setMaxResults(maxThisChunk).list();
                if (logs == null || logs.isEmpty()) {
                    break;
                }
                for (int i = 0; i < logs.size(); i++) {
                    ErrorLog log = logs.get(i);
                    if (!addIfFingerprintBaru(fingerprints, log)) {
                        continue;
                    }
                    if (uniqueIndex >= first && result.size() < pageSize) {
                        result.add(log);
                    }
                    uniqueIndex++;
                }
                offset += logs.size();
                scanned += logs.size();
                if (logs.size() < maxThisChunk || (scanLimit > 0 && scanned >= scanLimit)) {
                    break;
                }
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
                break;
            } finally {
                closeSessionQuietly(session);
            }
        }
        if (totalHolder != null && totalHolder.length > 0) {
            totalHolder[0] = uniqueIndex;
        }
        return result;
    }

    public Criteria initCriteria(boolean order) {
        return initCriteria(HibernateUtil.currentSession(), order);
    }

    private Criteria initCriteria(Session session, boolean order) {
        Criteria criteria = session.createCriteria(ErrorLog.class);

        String ada = getFilterAda();
        String tidak = getFilterTidak();

        if (ada.length() > 0) {
            criteria.add(Restrictions.ilike("keterangan", ada, MatchMode.ANYWHERE));
        }
        if (tidak.length() > 0) {
            criteria.add(Restrictions.not(Restrictions.ilike("keterangan", tidak, MatchMode.ANYWHERE)));
        }

        Date tanggalMulai = getFilterTanggalMulai();
        if (tanggalMulai != null) {
            criteria.add(Restrictions.ge("tanggal_dirubah", tanggalMulai));
        }
        Date tanggalSampai = getFilterTanggalSampaiEksklusif();
        if (tanggalSampai != null) {
            criteria.add(Restrictions.lt("tanggal_dirubah", tanggalSampai));
        }
        if (order) {
            criteria.addOrder(Order.desc("id"));
        }
        return criteria;
    }

    private String getFilterAda() {
        return searchnama == null || searchnama.getValue() == null ? "" : searchnama.getValue().trim();
    }

    private String getFilterTidak() {
        return searchnamaTidak == null || searchnamaTidak.getValue() == null ? "" : searchnamaTidak.getValue().trim();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void onSearchDefault(Event event) {
        try {
            showProgress(5, "Menyiapkan data", "Filter sedang dibaca dan proses pemuatan akan dimulai.");
            if (rootComponent != null) {
                Events.echoEvent("onLoadErrorLogs", rootComponent, null);
            } else {
                doSearchDefault(event);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            showDashboardError(e);
            hideProgress();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void doSearchDefault(Event event) {
        try {
            showProgress(20, "Membaca ringkasan", "Dasbor error sedang dihitung.");
            refreshDashboard();

            showProgress(55, "Menghitung halaman", "Jumlah error sesuai filter sedang dihitung.");
            int totalRows = countFilteredRows();
            updatePagingSafely(totalRows);

            showProgress(75, "Mengambil daftar", "Data error halaman aktif sedang dimuat.");
            List<ErrorLog> errorLogs = loadCurrentPageLogs();

            showProgress(95, "Menampilkan data", "Tabel error sedang disusun.");
            if (grid != null) {
                ListModel model = new SimpleListModel(errorLogs);
                grid.setRowRenderer(new ErrorLogRenderer());
                grid.setModelCheckMobile(model);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            showDashboardError(e);
        } finally {
            hideProgress();
        }
    }

    @SuppressWarnings("unchecked")
    private int countFilteredRows() {
        if (isHanyaTampilkanErrorBerbeda()) {
            return countDistinctFilteredRows();
        }
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = initCriteria(session, false);
            criteria.setProjection(Projections.rowCount());
            Object value = criteria.uniqueResult();
            long total = 0L;
            if (value instanceof Long) {
                total = ((Long) value).longValue();
            } else if (value instanceof Number) {
                total = ((Number) value).longValue();
            }
            return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return 0;
        } finally {
            closeSessionQuietly(session);
        }
    }

    private void updatePagingSafely(int totalRows) {
        if (paging == null) {
            return;
        }
        try {
            paging.setPageSize(Common.ROWS_COUNT_ON_PAGE);
            paging.setTotalSize(totalRows < 0 ? 0 : totalRows);
            if (paging.getActivePage() < 0) {
                paging.setActivePage(0);
            }
            int maxPage = Common.ROWS_COUNT_ON_PAGE <= 0 ? 0 : ((totalRows + Common.ROWS_COUNT_ON_PAGE - 1) / Common.ROWS_COUNT_ON_PAGE) - 1;
            if (maxPage < 0) {
                maxPage = 0;
            }
            if (paging.getActivePage() > maxPage) {
                paging.setActivePage(maxPage);
            }
            paging.setVisible(totalRows > Common.ROWS_COUNT_ON_PAGE);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ErrorLog> loadCurrentPageLogs() {
        int activePage = paging == null ? 0 : paging.getActivePage();
        int first = Common.ROWS_COUNT_ON_PAGE * (activePage < 0 ? 0 : activePage);
        if (isHanyaTampilkanErrorBerbeda()) {
            int[] totalHolder = new int[] { 0 };
            List<ErrorLog> resultDistinct = loadDistinctCurrentPageLogs(first, Common.ROWS_COUNT_ON_PAGE, totalHolder);
            updatePagingSafely(totalHolder[0]);
            return resultDistinct;
        }
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<ErrorLog> result = initCriteria(session, true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                    .setFirstResult(first).list();
            return result == null ? new ArrayList<ErrorLog>() : result;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<ErrorLog>();
        } finally {
            closeSessionQuietly(session);
        }
    }

    private void showProgress(int percent, String title, String detail) {
        if (progressHtml == null) {
            try {
                Clients.showBusy((title == null ? "Memuat data" : title) + " - " + (detail == null ? "" : detail));
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:1029");
            }
            return;
        }
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        StringBuilder html = new StringBuilder(2200);
        html.append("<div style='margin:10px;padding:12px 14px;border-radius:16px;background:#0f172a;color:#fff;box-shadow:0 14px 35px rgba(15,23,42,.22);font-family:Arial,Helvetica,sans-serif;'>");
        html.append("<div style='display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:8px;'>");
        html.append("<div style='font-size:13px;font-weight:800;'>").append(escapeHtml(title)).append("</div>");
        html.append("<div style='font-size:12px;font-weight:800;color:#bfdbfe;'>").append(percent).append("%</div>");
        html.append("</div>");
        html.append("<div style='height:10px;border-radius:999px;background:rgba(255,255,255,.16);overflow:hidden;'>");
        html.append("<div style='height:10px;width:").append(percent).append("%;border-radius:999px;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));transition:width .25s ease;'></div>");
        html.append("</div>");
        html.append("<div style='margin-top:8px;font-size:12px;color:#dbeafe;line-height:1.45;'>").append(escapeHtml(detail)).append("</div>");
        html.append("</div>");
        progressHtml.setVisible(true);
        progressHtml.setContent(html.toString());
    }

    private void hideProgress() {
        if (progressHtml == null) {
            try {
                Clients.clearBusy();
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:1058");
            }
            return;
        }
        try {
            progressHtml.setContent("");
            progressHtml.setVisible(false);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:1065");
        }
    }

    private int getKonfigurasiInt(String key, int defaultValue) {
        try {
            String value = Common.getKonfigurasi(key, String.valueOf(defaultValue)).getNilai();
            return Integer.parseInt(value == null ? String.valueOf(defaultValue) : value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Menyusun klausa WHERE SQL (rentang tanggal) untuk laporan kesalahan, mengikuti filter tanggal
     * di layar ({@code start}/{@code end}); bila kosong dipakai 30 hari terakhir. Memakai
     * {@code Common.databaseDateFormat} agar format tanggal konsisten dengan basis data.
     */
    private String whereLaporanError() {
        java.util.Date m = start == null ? null : start.getValue();
        java.util.Date s = end == null ? null : end.getValue();
        if (s == null) {
            s = ais.ui.util.WaktuUtil.getDate();
        }
        if (m == null) {
            java.util.Calendar c = ais.ui.util.WaktuUtil.getCalendar();
            c.setTime(s);
            c.add(java.util.Calendar.DATE, -30);
            m = c.getTime();
        }
        java.text.SimpleDateFormat f = Common.databaseDateFormat.get();
        return " where date(tanggal_dirubah) between date('" + f.format(m) + "') and date('" + f.format(s) + "') ";
    }

    /**
     * Deskripsi laporan "Ringkasan Kesalahan Sistem" untuk mesin
     * {@link ais.action.master.helper.DashboardReportKit}. Semua query hanya mengambil ringkasan/agregat
     * dan potongan pesan ({@code left(keterangan, N)}) — TIDAK memuat kolom TEXT penuh — demi hemat
     * memori. Berisi: KPI ringkasan, tren harian (grafik garis), kesalahan tersering (batang), dan
     * daftar kesalahan terbaru (tabel).
     */
    private ais.action.master.helper.DashboardReportKit.SumberLaporan buatSumberLaporanError() {
        return new ais.action.master.helper.DashboardReportKit.SumberLaporan() {
            @Override
            public String judul() {
                return "Ringkasan Kesalahan Sistem";
            }

            @Override
            public String subjudul() {
                return "";
            }

            @Override
            public String deskripsi() {
                return "Melihat seberapa sering sistem mengalami gangguan dan jenis gangguan yang paling banyak terjadi.";
            }

            @Override
            public java.util.List<ais.action.master.helper.DashboardReportKit.Bagian> bagian() {
                final String w = whereLaporanError();
                java.util.List<ais.action.master.helper.DashboardReportKit.Bagian> b =
                        new java.util.ArrayList<ais.action.master.helper.DashboardReportKit.Bagian>();

                b.add(ais.action.master.helper.DashboardReportKit.kpi("Ringkasan",
                        "Angka penting kesalahan pada rentang tanggal terpilih.",
                        new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
                            @Override
                            public java.util.List<Object[]> ambil() {
                                java.util.List<Object[]> r = new java.util.ArrayList<Object[]>();
                                java.util.List<Object[]> q = ais.action.master.helper.DashboardReportKit.sql(
                                        "select count(*), count(distinct left(keterangan,80)), "
                                        + "sum(case when date(tanggal_dirubah)=current_date then 1 else 0 end), "
                                        + "count(distinct date(tanggal_dirubah)) from error_log" + w);
                                if (!q.isEmpty()) {
                                    Object[] x = q.get(0);
                                    long total = ais.action.master.helper.DashboardReportKit.L(x[0]);
                                    long jenis = ais.action.master.helper.DashboardReportKit.L(x[1]);
                                    long hariIni = ais.action.master.helper.DashboardReportKit.L(x[2]);
                                    long hari = ais.action.master.helper.DashboardReportKit.L(x[3]);
                                    long rata = hari > 0 ? Math.round((double) total / hari) : total;
                                    r.add(new Object[] { "Total Kesalahan", ais.action.master.helper.DashboardReportKit.fmt(total), "sepanjang periode" });
                                    r.add(new Object[] { "Jenis Berbeda", ais.action.master.helper.DashboardReportKit.fmt(jenis), "ragam pesan kesalahan" });
                                    r.add(new Object[] { "Hari Ini", ais.action.master.helper.DashboardReportKit.fmt(hariIni), "kesalahan tercatat hari ini" });
                                    r.add(new Object[] { "Rata-rata / Hari", ais.action.master.helper.DashboardReportKit.fmt(rata), "pada hari yang ada kesalahan" });
                                }
                                return r;
                            }
                        }));

                b.add(ais.action.master.helper.DashboardReportKit.garis("Tren Kesalahan Harian",
                        "Naik-turun jumlah kesalahan tiap hari; lonjakan menandai hari bermasalah.",
                        new String[] { "Tanggal", "Jumlah" },
                        new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
                            @Override
                            public java.util.List<Object[]> ambil() {
                                return ais.action.master.helper.DashboardReportKit.sql(
                                        "select to_char(tanggal_dirubah,'YYYY-MM-DD'), count(*) from error_log" + w
                                        + " group by 1 order by 1");
                            }
                        }));

                b.add(ais.action.master.helper.DashboardReportKit.batang("Kesalahan Paling Sering",
                        "Pesan kesalahan yang paling banyak muncul; jadikan prioritas perbaikan.",
                        new String[] { "Pesan (ringkas)", "Jumlah" },
                        new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
                            @Override
                            public java.util.List<Object[]> ambil() {
                                return ais.action.master.helper.DashboardReportKit.sql(
                                        "select left(keterangan,70), count(*) from error_log" + w
                                        + " group by 1 order by 2 desc limit 12");
                            }
                        }));

                b.add(ais.action.master.helper.DashboardReportKit.tabel("Kesalahan Terbaru",
                        "Daftar kejadian kesalahan paling akhir beserta waktunya.",
                        new String[] { "Waktu", "Pesan (ringkas)" },
                        new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
                            @Override
                            public java.util.List<Object[]> ambil() {
                                return ais.action.master.helper.DashboardReportKit.sql(
                                        "select to_char(tanggal_dirubah,'YYYY-MM-DD HH24:MI'), left(keterangan,160) from error_log" + w
                                        + " order by tanggal_dirubah desc limit 50");
                            }
                        }));
                return b;
            }
        };
    }

    private void closeSessionQuietly(Session session) {
        if (session == null) {
            return;
        }
        try {
            if (session.isOpen()) {
                try {
                    session.clear();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:1203");
                }
                try {
                    session.disconnect();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:1207");
                }
                try {
                    session.close();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:1211");
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:1214");
        }
    }

    private void refreshDashboard() {
        if (dashboardHtml == null) {
            return;
        }
        try {
            DashboardData data = loadDashboardData();
            dashboardHtml.setContent(buildDashboardHtml(data));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            showDashboardError(e);
        }
    }

    private void showDashboardError(Exception e) {
        if (dashboardHtml == null) {
            return;
        }
        try {
            StringBuilder html = new StringBuilder();
            html.append("<div style='font-family:Arial,Helvetica,sans-serif;margin:14px;padding:16px;border-radius:14px;background:#fff1f2;border:1px solid #fecdd3;color:#9f1239;'>");
            html.append("<div style='font-weight:800;font-size:15px;margin-bottom:6px;'>Data error belum bisa ditampilkan.</div>");
            html.append("<div style='font-size:12px;line-height:1.5;'>Terjadi kendala saat membaca ErrorLog. Detailnya sudah dicatat ke log sistem. Coba muat ulang halaman atau periksa stack trace terbaru.</div>");
            if (e != null) {
                html.append("<pre style='white-space:pre-wrap;margin-top:10px;font-size:11px;'>").append(escapeHtml(e.getClass().getName() + ": " + e.getMessage())).append("</pre>");
            }
            html.append("</div>");
            dashboardHtml.setContent(html.toString());
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/ErrorLogAction.java:1245");
        }
    }

    @SuppressWarnings("unchecked")
    private DashboardData loadDashboardData() {
        DashboardData data = new DashboardData();
        data.total = count(null);
        data.today = count(startOfDay(0));
        data.last7Days = count(startOfDay(-6));

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<ErrorLog> logs = initCriteria(session, true).setMaxResults(getDashboardLimit()).list();
            data.logs = logs == null ? new ArrayList<ErrorLog>() : logs;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            data.logs = new ArrayList<ErrorLog>();
        } finally {
            closeSessionQuietly(session);
        }

        initTrend(data);
        for (int i = 0; i < data.logs.size(); i++) {
            ErrorLog logData = data.logs.get(i);
            String keterangan = logData == null ? "" : logData.getKeterangan();
            String kategori = categorize(keterangan);
            addCount(data.categoryCounts, kategori, 1);
            addCount(data.sourceCounts, findSource(keterangan), 1);
            addCount(data.hourCounts, hourKey(logData == null ? null : logData.getTanggal_dirubah()), 1);

            if (logData != null && logData.getTanggal_dirubah() != null) {
                String key = dayFormat.format(logData.getTanggal_dirubah());
                if (data.trendCounts.containsKey(key)) {
                    data.trendCounts.put(key, Integer.valueOf(data.trendCounts.get(key).intValue() + 1));
                }
            }
        }
        data.topCategory = topKey(data.categoryCounts);
        return data;
    }

    private int getDashboardLimit() {
        int limit = getKonfigurasiInt("errorlog_dashboard_limit", DASHBOARD_LIMIT);
        if (limit <= 0) {
            return DASHBOARD_LIMIT;
        }
        return limit > 3000 ? 3000 : limit;
    }

    private void initTrend(DashboardData data) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -(TREND_DAYS - 1));
        for (int i = 0; i < TREND_DAYS; i++) {
            data.trendCounts.put(dayFormat.format(calendar.getTime()), Integer.valueOf(0));
            calendar.add(Calendar.DATE, 1);
        }
    }

    private Long count(Date since) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = initCriteria(session, false);
            if (since != null) {
                criteria.add(Restrictions.ge("tanggal_dirubah", since));
            }
            criteria.setProjection(Projections.rowCount());
            Object value = criteria.uniqueResult();
            if (value instanceof Long) {
                return (Long) value;
            }
            if (value instanceof Number) {
                return Long.valueOf(((Number) value).longValue());
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeSessionQuietly(session);
        }
        return Long.valueOf(0);
    }

    private Date startOfDay(int addDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, addDays);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String buildDashboardHtml(DashboardData data) {
        StringBuilder html = new StringBuilder(20000);
        html.append("<style>");
        html.append(".elog{font-family:Arial,Helvetica,sans-serif;color:#0f172a;padding:14px;background:#f8fafc;}");
        html.append(".elog-title{font-size:22px;font-weight:900;margin:0 0 6px;color:#0f172a;}");
        html.append(".elog-sub{font-size:13px;color:#64748b;margin-bottom:14px;line-height:1.5;}");
        html.append(".elog-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin-bottom:14px;}");
        html.append(".elog-card{background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 10px 25px rgba(15,23,42,.06);}");
        html.append(".elog-num{font-size:26px;font-weight:900;color:#1d4ed8;margin-top:8px;}");
        html.append(".elog-label{font-size:12px;font-weight:800;color:#475569;text-transform:uppercase;letter-spacing:.04em;}");
        html.append(".elog-help{font-size:12px;color:#64748b;line-height:1.45;margin-top:8px;}");
        html.append(".elog-panels{display:grid;grid-template-columns:1.3fr 1fr;gap:12px;margin-bottom:14px;}");
        html.append(".elog-panel{background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 10px 25px rgba(15,23,42,.05);}");
        html.append(".elog-panel h3{font-size:15px;margin:0 0 5px;color:#0f172a;}");
        html.append(".elog-desc{font-size:12px;color:#64748b;line-height:1.5;margin-bottom:12px;}");
        html.append(".bar-row{display:grid;grid-template-columns:54px 1fr 42px;align-items:center;gap:8px;margin:8px 0;font-size:11px;color:#475569;}");
        html.append(".bar-bg{height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;}");
        html.append(".bar-bg b{display:block;height:12px;border-radius:999px;background:linear-gradient(90deg,#60a5fa,#2563eb);}");
        html.append(".pill{display:inline-block;margin:4px 4px 0 0;padding:6px 9px;border-radius:999px;background:#eef2ff;color:#3730a3;font-size:11px;font-weight:700;}");
        html.append(".mini{font-size:11px;color:#64748b;}");
        html.append("@media(max-width:900px){.elog-grid,.elog-panels{grid-template-columns:1fr;}}");
        html.append("</style>");

        html.append("<div class='elog'>");
        html.append("<div class='elog-title'>Dasbor Error Sistem</div>");
        html.append("<div class='elog-sub'>Catatan ini membantu melihat bagian sistem yang paling sering bermasalah, kapan error biasanya muncul, dan area mana yang perlu diperiksa lebih dulu.</div>");

        html.append("<div class='elog-grid'>");
        appendCard(html, "Total Error", formatNumber(data.total), "Jumlah semua catatan error yang tersimpan di database.");
        appendCard(html, "Hari Ini", formatNumber(data.today), "Memudahkan tim melihat apakah hari ini sedang banyak gangguan.");
        appendCard(html, "7 Hari", formatNumber(data.last7Days), "Menunjukkan banyaknya gangguan selama satu minggu terakhir.");
        appendCard(html, "Paling Sering", escapeHtml(data.topCategory), "Jenis masalah yang paling banyak muncul pada data yang sedang ditampilkan.");
        html.append("</div>");

		html.append(buildSmartErrorAnalysis(data));

        html.append("<div class='elog-panels'>");
        appendTrendPanel(html, data);
        appendCategoryPanel(html, data);
        html.append("</div>");

        html.append("<div class='elog-panels'>");
        appendSpiderPanel(html, data);
        appendSourcePanel(html, data);
        html.append("</div>");

        html.append("<div class='elog-panels'>");
        appendHourPanel(html, data);
        appendLatestPanel(html, data);
        html.append("</div>");

        html.append("<div class='mini'>Ringkasan memakai maksimal ").append(DASHBOARD_LIMIT)
                .append(" catatan terbaru agar halaman tetap ringan. Gunakan kotak pencarian untuk mempersempit data yang ingin dianalisis.</div>");
        html.append("</div>");
        return html.toString();
    }

	private String buildSmartErrorAnalysis(DashboardData data) {
		List<String> temuan = new ArrayList<String>();
		List<String> penyebab = new ArrayList<String>();
		List<String> tindakan = new ArrayList<String>();
		long rataHarian = data.last7Days == null ? 0L : Math.round(data.last7Days.longValue() / 7.0);
		int topCount = getValue(data.categoryCounts, data.topCategory);
		int sampel = data.logs == null ? 0 : data.logs.size();
		double dominasi = sampel > 0 ? topCount * 100.0 / sampel : 0.0;
		long hariIni = data.today == null ? 0L : data.today.longValue();
		String status;
		String ringkasan;
		if (data.total == null || data.total.longValue() == 0L) {
			status = "BELUM ADA DATA";
			ringkasan = "Belum ada kesalahan yang dapat dianalisis pada periode ini.";
		} else if (hariIni >= 100L || (hariIni >= 20L && hariIni > Math.max(1L, rataHarian) * 3L)
				|| getValue(data.categoryCounts, "Database") >= 50) {
			status = "KRITIS";
			ringkasan = "Volume atau konsentrasi kesalahan menunjukkan gangguan berulang yang perlu ditangani segera.";
		} else if (hariIni > Math.max(3L, rataHarian * 2L) || data.last7Days.longValue() > 0L) {
			status = "PERLU PERHATIAN";
			ringkasan = "Masih terdapat kesalahan aktif; prioritas pemeriksaan diarahkan ke kategori dan sumber yang paling dominan.";
		} else {
			status = "NORMAL";
			ringkasan = "Tidak terlihat lonjakan kesalahan baru, namun histori tetap tersedia untuk evaluasi berkala.";
		}

		temuan.add(hariIni + " error tercatat hari ini; rata-rata tujuh hari sekitar " + rataHarian + " error per hari.");
		temuan.add("Kategori dominan adalah " + data.topCategory + " sebanyak " + topCount
				+ " dari " + sampel + " sampel terbaru (" + new java.text.DecimalFormat("0.0").format(dominasi) + "%).");
		temuan.add("Sumber yang paling sering muncul: " + topKey(data.sourceCounts) + ".");

		String kategori = data.topCategory == null ? "" : data.topCategory.toLowerCase();
		if (kategori.indexOf("database") >= 0) penyebab.add("Koneksi database, query lambat, constraint, transaksi gagal, atau session yang tidak selesai dengan benar.");
		else if (kategori.indexOf("data") >= 0 || kategori.indexOf("format") >= 0) penyebab.add("Data wajib kosong, format input tidak sesuai, relasi lama terputus, atau konversi nilai gagal.");
		else if (kategori.indexOf("file") >= 0 || kategori.indexOf("jaringan") >= 0) penyebab.add("File tidak tersedia, izin akses, timeout, host eksternal, atau koneksi jaringan tidak stabil.");
		else if (kategori.indexOf("tampilan") >= 0 || kategori.indexOf("halaman") >= 0) penyebab.add("State komponen tidak lengkap, data null saat render, atau event halaman berjalan pada urutan yang tidak diharapkan.");
		else penyebab.add("Akar masalah perlu ditentukan dari stack trace lengkap pada kategori dan sumber dominan.");
		if (dominasi >= 50.0) penyebab.add("Satu akar masalah kemungkinan menghasilkan banyak catatan berulang; angka total belum tentu mewakili banyak masalah berbeda.");

		tindakan.add("Aktifkan 'Hanya Tampilkan Error yang berbeda' untuk memisahkan akar masalah dari pengulangan log.");
		tindakan.add("Buka error terbaru dari sumber " + topKey(data.sourceCounts) + " dan telusuri frame aplikasi pertama pada stack trace.");
		if (hariIni > Math.max(3L, rataHarian * 2L)) tindakan.add("Bandingkan jam lonjakan dengan tab Performa Sistem, Pemakaian Memori, dan Catatan Upload.");
		tindakan.add("Setelah perbaikan, muat ulang dan pastikan kategori dominan tidak bertambah pada periode pemantauan berikutnya.");
		return DashboardUiKit.smartAnalysis(status, ringkasan, temuan, penyebab, tindakan);
	}

    private void appendCard(StringBuilder html, String label, String value, String help) {
        html.append("<div class='elog-card'>");
        html.append("<div class='elog-label'>").append(label).append("</div>");
        html.append("<div class='elog-num'>").append(value).append("</div>");
        html.append("<div class='elog-help'>").append(help).append("</div>");
        html.append("</div>");
    }

    private void appendTrendPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='elog-panel'><h3>Tren Harian</h3>");
        html.append("<div class='elog-desc'>Melihat hari yang paling sering muncul gangguan sehingga pemeriksaan bisa diarahkan ke waktu tersebut.</div>");
        int max = maxValue(data.trendCounts);
        for (Map.Entry<String, Integer> entry : data.trendCounts.entrySet()) {
            appendBar(html, entry.getKey(), entry.getValue().intValue(), max);
        }
        html.append("</div>");
    }

    private void appendCategoryPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='elog-panel'><h3>Jenis Masalah</h3>");
        html.append("<div class='elog-desc'>Memisahkan error menjadi kelompok sederhana agar mudah dipahami tanpa membaca stack trace panjang.</div>");
        appendBarsFromMap(html, data.categoryCounts, 8);
        html.append("</div>");
    }

    private void appendSourcePanel(StringBuilder html, DashboardData data) {
        html.append("<div class='elog-panel'><h3>Sumber Error</h3>");
        html.append("<div class='elog-desc'>Menunjukkan class atau bagian program yang paling sering muncul dalam catatan error.</div>");
        appendBarsFromMap(html, data.sourceCounts, 8);
        html.append("</div>");
    }

    private void appendHourPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='elog-panel'><h3>Jam Rawan</h3>");
        html.append("<div class='elog-desc'>Membantu melihat waktu kerja yang paling sering mengalami gangguan.</div>");
        appendBarsFromMap(html, data.hourCounts, 8);
        html.append("</div>");
    }

    private void appendLatestPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='elog-panel'><h3>Error Terbaru</h3>");
        html.append("<div class='elog-desc'>Daftar singkat error terakhir agar kejadian terbaru cepat terlihat.</div>");
        int limit = Math.min(6, data.logs.size());
        for (int i = 0; i < limit; i++) {
            ErrorLog log = data.logs.get(i);
            html.append("<div style='border-bottom:1px solid #e2e8f0;padding:8px 0;'>");
            html.append("<div style='font-weight:800;font-size:12px;color:#0f172a;'>")
                    .append(log.getTanggal_dirubah() == null ? "-" : dateTimeFormat.format(log.getTanggal_dirubah()))
                    .append("</div>");
            html.append("<div class='mini'>").append(escapeHtml(log.getRingkasan())).append("</div>");
            html.append("</div>");
        }
        if (limit == 0) {
            html.append("<div class='mini'>Belum ada catatan error.</div>");
        }
        html.append("</div>");
    }

    private void appendSpiderPanel(StringBuilder html, DashboardData data) {
        String[] labels = new String[] { "Data", "Database", "Tampilan", "Halaman", "File/Jaringan" };
        int[] values = new int[] { sumCategories(data.categoryCounts, new String[] { "Data", "Format Angka", "Data Tidak Lengkap" }),
                getValue(data.categoryCounts, "Database"), getValue(data.categoryCounts, "Tampilan"),
                getValue(data.categoryCounts, "Halaman"),
                sumCategories(data.categoryCounts, new String[] { "File", "Jaringan" }) };
        int max = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }

        html.append("<div class='elog-panel'><h3>Peta Risiko</h3>");
        html.append("<div class='elog-desc'>Bentuk radar memperlihatkan area masalah yang paling menonjol agar prioritas perbaikan lebih mudah dipilih.</div>");
        html.append("<svg viewBox='0 0 240 240' style='width:100%;max-width:360px;display:block;margin:auto;'>");
        html.append("<circle cx='120' cy='120' r='36' fill='none' stroke='#e2e8f0'/>");
        html.append("<circle cx='120' cy='120' r='72' fill='none' stroke='#e2e8f0'/>");
        html.append("<circle cx='120' cy='120' r='102' fill='none' stroke='#cbd5e1'/>");
        for (int i = 0; i < labels.length; i++) {
            double angle = -Math.PI / 2 + (2 * Math.PI * i / labels.length);
            int x = (int) Math.round(120 + Math.cos(angle) * 102);
            int y = (int) Math.round(120 + Math.sin(angle) * 102);
            int lx = (int) Math.round(120 + Math.cos(angle) * 116);
            int ly = (int) Math.round(120 + Math.sin(angle) * 116);
            html.append("<line x1='120' y1='120' x2='").append(x).append("' y2='").append(y)
                    .append("' stroke='#e2e8f0'/>");
            html.append("<text x='").append(lx).append("' y='").append(ly)
                    .append("' text-anchor='middle' font-size='10' fill='#475569'>").append(labels[i]).append("</text>");
        }
        html.append("<polygon points='").append(buildRadarPoints(values, max)).append("' fill='rgba(37,99,235,.25)' stroke='#2563eb' stroke-width='3'/>");
        html.append("</svg>");
        for (int i = 0; i < labels.length; i++) {
            html.append("<span class='pill'>").append(labels[i]).append(": ").append(values[i]).append("</span>");
        }
        html.append("</div>");
    }

    private String buildRadarPoints(int[] values, int max) {
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            double angle = -Math.PI / 2 + (2 * Math.PI * i / values.length);
            double radius = 102.0 * values[i] / (max <= 0 ? 1 : max);
            int x = (int) Math.round(120 + Math.cos(angle) * radius);
            int y = (int) Math.round(120 + Math.sin(angle) * radius);
            if (points.length() > 0) {
                points.append(' ');
            }
            points.append(x).append(',').append(y);
        }
        return points.toString();
    }

    private void appendBarsFromMap(StringBuilder html, Map<String, Integer> map, int limit) {
        List<Map.Entry<String, Integer>> entries = sortedEntries(map);
        int max = 1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getValue().intValue() > max) {
                max = entries.get(i).getValue().intValue();
            }
        }
        int count = Math.min(limit, entries.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            appendBar(html, entry.getKey(), entry.getValue().intValue(), max);
        }
        if (count == 0) {
            html.append("<div class='mini'>Belum ada data untuk ditampilkan.</div>");
        }
    }

    private void appendBar(StringBuilder html, String label, int value, int max) {
        int width = max <= 0 ? 0 : (int) Math.round((value * 100.0) / max);
        if (width < 4 && value > 0) {
            width = 4;
        }
        html.append("<div class='bar-row'><span>").append(escapeHtml(label)).append("</span><div class='bar-bg'><b style='width:")
                .append(width).append("%'></b></div><em>").append(value).append("</em></div>");
    }

    private String categorize(String text) {
        String value = text == null ? "" : text.toLowerCase();
        if (value.indexOf("numberformatexception") >= 0) {
            return "Format Angka";
        }
        if (value.indexOf("arrayindex") >= 0) {
            return "Data Tidak Lengkap";
        }
        if (value.indexOf("nullpointer") >= 0) {
            return "Data";
        }
        if (value.indexOf("hibernate") >= 0 || value.indexOf("sql") >= 0 || value.indexOf("database") >= 0) {
            return "Database";
        }
        if (value.indexOf("zkoss") >= 0 || value.indexOf("zul") >= 0) {
            return "Tampilan";
        }
        if (value.indexOf("servlet") >= 0 || value.indexOf("jsp") >= 0 || value.indexOf("request uri") >= 0) {
            return "Halaman";
        }
        if (value.indexOf("connectexception") >= 0 || value.indexOf("socket") >= 0 || value.indexOf("timeout") >= 0) {
            return "Jaringan";
        }
        if (value.indexOf("ioexception") >= 0 || value.indexOf("filenotfound") >= 0 || value.indexOf(" file") >= 0) {
            return "File";
        }
        return "Lainnya";
    }

    private String findSource(String text) {
        if (text == null) {
            return "Tidak diketahui";
        }
        String[] lines = text.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.startsWith("at ais.")) {
                int start = 3;
                int end = line.indexOf('(');
                String value = end > start ? line.substring(start, end) : line.substring(start);
                int lastDot = value.lastIndexOf('.');
                if (lastDot > 0) {
                    int prevDot = value.lastIndexOf('.', lastDot - 1);
                    if (prevDot > 0) {
                        value = value.substring(prevDot + 1);
                    }
                }
                return abbreviate(value, 42);
            }
        }
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.startsWith("Request URI:")) {
                return abbreviate(line.substring("Request URI:".length()).trim(), 42);
            }
        }
        return "Tidak diketahui";
    }

    private String hourKey(Date date) {
        if (date == null) {
            return "Tidak diketahui";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return (hour < 10 ? "0" + hour : String.valueOf(hour)) + ":00";
    }

    private void addCount(Map<String, Integer> map, String key, int value) {
        String safeKey = key == null || key.trim().length() == 0 ? "Tidak diketahui" : key.trim();
        Integer old = map.get(safeKey);
        map.put(safeKey, Integer.valueOf((old == null ? 0 : old.intValue()) + value));
    }

    private String topKey(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> entries = sortedEntries(map);
        return entries.isEmpty() ? "Belum ada" : entries.get(0).getKey();
    }

    private List<Map.Entry<String, Integer>> sortedEntries(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        return entries;
    }

    private int maxValue(Map<String, Integer> map) {
        int max = 1;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue().intValue() > max) {
                max = entry.getValue().intValue();
            }
        }
        return max;
    }

    private int getValue(Map<String, Integer> map, String key) {
        Integer value = map.get(key);
        return value == null ? 0 : value.intValue();
    }

    private int sumCategories(Map<String, Integer> map, String[] keys) {
        int total = 0;
        for (int i = 0; i < keys.length; i++) {
            total += getValue(map, keys[i]);
        }
        return total;
    }

    private String formatNumber(Long value) {
        return Common.numberFormat1.get().format(value == null ? Long.valueOf(0) : value);
    }

    private String abbreviate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Pembawa data/helper lokal milik {@link ErrorLogAction} untuk dashboard data. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link ErrorLogAction}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long total}, {@code Long today},
     * {@code Long last7Days}, {@code String topCategory}, {@code List logs}, {@code Map trendCounts}, {@code Map
     * categoryCounts}, {@code Map sourceCounts}. Aturan bisnis bersama tetap berada pada kelas induk atau service
     * yang dipanggilnya.</p>
     *
     * @see ErrorLogAction
     */
    private static class DashboardData {
        Long total = Long.valueOf(0);
        Long today = Long.valueOf(0);
        Long last7Days = Long.valueOf(0);
        String topCategory = "Belum ada";
        List<ErrorLog> logs = new ArrayList<ErrorLog>();
        Map<String, Integer> trendCounts = new LinkedHashMap<String, Integer>();
        Map<String, Integer> categoryCounts = new HashMap<String, Integer>();
        Map<String, Integer> sourceCounts = new HashMap<String, Integer>();
        Map<String, Integer> hourCounts = new HashMap<String, Integer>();
    }
}
