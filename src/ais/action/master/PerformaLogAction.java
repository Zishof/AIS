package ais.action.master;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.PerformaSnapshotUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerformaLog;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Composer halaman <b>Performa Sistem</b> ({@code performa_log.zul}).
 *
 * <p>
 * Polanya meniru {@link ErrorLogAction}: ada tab Dasbor (ringkasan visual) dan tab Daftar
 * (paginasi catatan). Bedanya, sumber data adalah {@link PerformaLog} yang berisi metrik angka
 * dari {@link PerformaSnapshotUtil}, sehingga dasbor membaca kolom langsung tanpa mengurai teks.
 * </p>
 *
 * <p>
 * Tombol "Rekam Sekarang" memaksa satu snapshot baru, sehingga admin tidak perlu menunggu siklus
 * perekam otomatis. doAfterCompose juga memastikan scheduler perekam berkala berjalan.
 * </p>
 */
public class PerformaLogAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

    private static final long serialVersionUID = -8841220674590113377L;
    private static final int DASHBOARD_LIMIT = 1000;
    private static final int TREND_POINTS = 20;
    private static final int EXPORT_CHUNK_SIZE_DEFAULT = 100;

    private Paging paging;
    private MyGrid grid;
    private Html dashboardHtml;
    private Html progressHtml;
    private Component rootComponent;

    private Textbox searchnama;
    private Textbox searchnamaTidak;
    private MyDatebox start;
    private MyDatebox end;
    private Checkbox hanyaBermasalah;

    private MyToolbarbuttonConfig find;

    private List<PerformaSnapshotUtil.Hitung> histogramTerakhir;
    private String histogramTeksTerakhir;
    private Date histogramWaktu;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private final SimpleDateFormat dayTimeShortFormat = new SimpleDateFormat("dd/MM HH:mm");

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

        try {
            PerformaSnapshotUtil.ensureSchedulerStarted();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(null);
            }
        });

        if (rootComponent != null) {
            rootComponent.addEventListener("onLoadPerformaLogs", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    doSearchDefault(event);
                }
            });
        }

        appendToolbarButtons();
        onSearchDefault(null);
    }

    /** Default filter tanggal: 7 hari terakhir sampai hari ini. */
    private void initDefaultTanggal() {
        try {
            if (start != null) {
                start.setReadonly(true);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -7);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                start.setValue(calendar.getTime());
            }
            if (end != null) {
                end.setReadonly(true);
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                end.setValue(calendar.getTime());
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
        ais.action.master.helper.DashboardReportKit.pasangTombol(find.getParent(), self, buatSumberLaporanPerforma());

        hanyaBermasalah = new Checkbox("Hanya yang bermasalah");
        hanyaBermasalah.setTooltiptext("Jika aktif, hanya menampilkan snapshot berstatus Perhatian atau Kritis; status Normal disembunyikan.");
        hanyaBermasalah.setStyle("font-size:11px;font-weight:bold;margin-left:8px;margin-right:8px;color:#0f172a;");
        hanyaBermasalah.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (paging != null) {
                    paging.setActivePage(0);
                }
                onSearchDefault(event);
            }
        });
        find.getParent().appendChild(hanyaBermasalah);

        MyToolbarbuttonConfig rekam = new MyToolbarbuttonConfig("Rekam Sekarang", "/img/svg/refresh.svg");
        rekam.setTooltiptext("Ambil dan simpan satu snapshot performa JVM saat ini");
        rekam.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                rekamSnapshotSekarang();
            }
        });
        find.getParent().appendChild(rekam);

        MyToolbarbuttonConfig histogram = new MyToolbarbuttonConfig("Ambil Histogram Objek (berat)", "/img/svg/chart-line.svg");
        histogram.setTooltiptext("Ambil daftar class pemakai memori terbesar (setara jmap -histo:live). BERAT: memicu Full GC, jalankan saat investigasi memory leak.");
        histogram.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                konfirmasiHistogram();
            }
        });
        find.getParent().appendChild(histogram);

        MyToolbarbuttonConfig downloadSemua = new MyToolbarbuttonConfig("Download Sesuai Filter", "/img/svg/download.svg");
        downloadSemua.setTooltiptext("Download semua snapshot performa sesuai filter dalam format teks");
        downloadSemua.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                downloadFiltered(false);
            }
        });
        find.getParent().appendChild(downloadSemua);

        MyToolbarbuttonConfig copyAi = new MyToolbarbuttonConfig("Copy Instruksi AI", "/img/svg/copy.svg");
        copyAi.setTooltiptext("Salin prompt AI berisi snapshot performa sesuai filter ke clipboard");
        copyAi.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                copyFilteredAiPromptToClipboard();
            }
        });
        find.getParent().appendChild(copyAi);

        MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Muat Ulang", "/img/svg/refresh.svg");
        refresh.setTooltiptext("Muat ulang daftar dan dasbor performa");
        refresh.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(arg0);
            }
        });
        find.getParent().appendChild(refresh);

        MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
        hapus.setTooltiptext("Hapus seluruh catatan performa yang tersimpan di database");
        hapus.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                confirmDeleteAll();
            }
        });
        find.getParent().appendChild(hapus);
    }

    private void rekamSnapshotSekarang() {
        try {
            showProgress(30, "Merekam performa", "Membaca kondisi thread, memori, dan GC dari JVM.");
            String oleh = "Sistem";
            String olehId = "admin";
            try {
                Tbmuser user = Common.getCurrentUser();
                if (user != null) {
                    if (user.getUserId() != null) {
                        olehId = user.getUserId();
                    }
                    oleh = user.toString();
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:279");
            }
            showProgress(70, "Menyimpan", "Snapshot performa sedang disimpan ke database.");
            Long id = PerformaSnapshotUtil.recordPaksa(oleh, olehId);
            if (id != null) {
                try {
                    MyMessageboxConfig.showFormat(
                            "Rekaman performa sistem berhasil disimpan dengan nomor identitas (ID {V1}). Data terbaru kini telah tersedia pada daftar dan dasbor performa.",
                            "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, id);
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:288");
                }
            } else {
                try {
                    MyMessageboxConfig.show("Mohon maaf, rekaman performa sistem gagal disimpan. Langkah yang dapat dilakukan: (1) periksa berkas log server untuk mengetahui penyebabnya; (2) pastikan koneksi basis data berjalan normal; (3) ulangi proses perekaman, dan apabila masih gagal mohon hubungi administrator sistem.");
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:293");
                }
            }
            onSearchDefault(null);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            hideProgress();
        }
    }

    private void konfirmasiHistogram() {
        try {
            MyMessageboxConfig.show("Mohon perhatian Bapak/Ibu, pengambilan histogram objek akan memicu proses Full GC dan berpotensi membuat aplikasi tertahan sesaat, terutama pada pemakaian memori (heap) yang besar. Sebaiknya proses ini dijalankan hanya saat investigasi kebocoran memori. Apakah Bapak/Ibu yakin ingin melanjutkan?",
                    "Konfirmasi", Messagebox.YES | Messagebox.NO, MyMessageboxConfig.QUESTION, new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            if ("onYes".equals(event.getName())) {
                                ambilHistogram();
                            }
                        }
                    });
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void ambilHistogram() {
        try {
            showProgress(40, "Mengambil histogram objek", "Menjalankan Full GC dan menghitung objek per class. Mohon tunggu.");
            String teks = PerformaSnapshotUtil.ambilHistogramTeks();
            histogramTeksTerakhir = teks;
            histogramTerakhir = PerformaSnapshotUtil.parseHistogram(teks, 40);
            histogramWaktu = new Date();
            showProgress(85, "Menyusun tampilan", "Menampilkan class pemakai memori terbesar.");
            refreshDashboard();
            try {
                MyMessageboxConfig.show("Histogram objek berhasil diambil. Silakan Bapak/Ibu membuka panel \"Histogram Objek (Memory Leak)\" pada dasbor untuk melihat daftar kelas dengan pemakaian memori terbesar.");
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:331");
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            hideProgress();
        }
    }

    private void confirmDeleteAll() {
        try {
            MyMessageboxConfig.show("Mohon perhatian Bapak/Ibu, seluruh catatan performa sistem yang tersimpan di basis data akan dihapus secara permanen dan tidak dapat dikembalikan lagi. Apakah Bapak/Ibu yakin ingin melanjutkan penghapusan seluruh data performa ini?", "Konfirmasi",
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
        try {
            showProgress(20, "Menghapus PerformaLog", "Mengosongkan tabel performa_log dengan TRUNCATE.");
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.createSQLQuery("TRUNCATE TABLE performa_log RESTART IDENTITY").executeUpdate();
            transaction.commit();
            try {
                MyMessageboxConfig.show("Seluruh catatan performa sistem berhasil dihapus dari basis data.");
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:368");
            }
        } catch (Exception e) {
            rollbackQuietly(transaction);
            closeSessionQuietly(session);
            session = null;
            transaction = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                transaction = session.beginTransaction();
                session.createSQLQuery("delete from performa_log").executeUpdate();
                transaction.commit();
                try {
                    MyMessageboxConfig.show("Seluruh catatan performa sistem berhasil dihapus dari basis data menggunakan metode DELETE.");
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:382");
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
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:402");
        }
    }

    class PerformaLogRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object data) throws Exception {
            row.setValign("top");
            final PerformaLog performaLog = (PerformaLog) data;
            String waktu = performaLog.getTanggal_dirubah() == null ? "-"
                    : dateTimeFormat.format(performaLog.getTanggal_dirubah());
            String status = performaLog.getStatusKesehatan();
            String warna = performaLog.getStatusWarna();

            StringBuilder left = new StringBuilder();
            left.append("<div style='padding:10px 8px;'>");
            left.append("<div style='font-weight:800;color:#0f172a;font-size:13px;'>").append(escapeHtml(waktu)).append("</div>");
            left.append("<div style='margin-top:6px;display:inline-block;padding:3px 10px;border-radius:999px;background:")
                    .append(warna).append(";color:#fff;font-size:11px;font-weight:800;'>")
                    .append(escapeHtml(status)).append("</div>");
            left.append("<div style='margin-top:8px;color:#64748b;font-size:11px;'>ID: ").append(performaLog.getId()).append("</div>");
            if (performaLog.getOlehId() != null && performaLog.getOlehId().trim().length() > 0) {
                left.append("<div style='margin-top:4px;color:#64748b;font-size:11px;'>Oleh: ")
                        .append(escapeHtml(performaLog.getOlehId())).append("</div>");
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
            copyRingkasan.setTooltiptext("Salin ringkasan performa singkat ke clipboard");
            copyRingkasan.setStyle("font-size:11px;font-weight:bold;border-radius:8px;padding:4px 8px;background:#eff6ff;color:#1d4ed8;border:1px solid #bfdbfe;");
            copyRingkasan.setParent(copyToolbar);
            copyRingkasan.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    copyToClipboard(buildCopyText(performaLog, false));
                }
            });

            MyToolbarbuttonConfig copyLengkap = new MyToolbarbuttonConfig("Copy Lengkap untuk AI", "/img/svg/copy.svg");
            copyLengkap.setTooltiptext("Salin detail lengkap snapshot performa termasuk cuplikan thread");
            copyLengkap.setStyle("font-size:11px;font-weight:bold;border-radius:8px;padding:4px 8px;background:#0f172a;color:#ffffff;border:1px solid #0f172a;");
            copyLengkap.setParent(copyToolbar);
            copyLengkap.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    copyToClipboard(buildCopyText(performaLog, true));
                }
            });

            StringBuilder right = new StringBuilder();
            right.append("<div style='width:100%;box-sizing:border-box;'>");
            right.append("<div style='display:flex;flex-wrap:wrap;gap:6px;margin-bottom:10px;'>");
            appendChip(right, "Thread", String.valueOf(nz(performaLog.getJumlahThread())));
            appendChip(right, "Menunggu", String.valueOf(performaLog.getJumlahMenunggu()));
            appendChip(right, "BLOCKED", String.valueOf(nz(performaLog.getJumlahBlocked())));
            appendChip(right, "Deadlock", String.valueOf(nz(performaLog.getJumlahDeadlock())));
            int heap = performaLog.getPersenHeap();
            appendChip(right, "Heap", heap < 0 ? "-" : (heap + "%"));
            appendChip(right, "Kelas", String.valueOf(nz(performaLog.getJumlahKelas())));
            appendChip(right, "GC", String.valueOf(nzLong(performaLog.getGcJumlah())));
            right.append("</div>");
            right.append("<pre style='white-space:pre-wrap;word-break:break-word;background:#0f172a;color:#e2e8f0;border-radius:10px;padding:12px;line-height:1.45;font-size:11px;max-height:260px;overflow:auto;box-sizing:border-box;'>")
                    .append(escapeHtml(performaLog.getKeterangan())).append("</pre>");
            right.append("</div>");
            new MyHtml(right.toString()).setParent(rightBox);
        }
    }

    private void appendChip(StringBuilder html, String label, String value) {
        html.append("<span style='display:inline-block;padding:4px 10px;border-radius:999px;background:#eef2ff;color:#3730a3;font-size:11px;font-weight:700;'>")
                .append(escapeHtml(label)).append(": ").append(escapeHtml(value)).append("</span>");
    }

    private String buildCopyText(PerformaLog performaLog, boolean full) {
        if (performaLog == null) {
            return "";
        }
        String waktu = performaLog.getTanggal_dirubah() == null ? "-"
                : dateTimeFormat.format(performaLog.getTanggal_dirubah());
        StringBuilder text = new StringBuilder(4096);
        if (full) {
            text.append("Tolong analisa kondisi performa JVM eCampus berikut dan berikan rekomendasi tindakan untuk admin.\n");
            text.append("Fokuskan pada penyebab thread menunggu/terblokir, potensi deadlock, dan tekanan memori/GC.\n\n");
        }
        text.append("=== SNAPSHOT PERFORMA ECAMPUS ===\n");
        text.append("ID            : ").append(performaLog.getId()).append('\n');
        text.append("Waktu         : ").append(waktu).append('\n');
        text.append("Status        : ").append(performaLog.getStatusKesehatan()).append('\n');
        text.append("Thread        : ").append(nz(performaLog.getJumlahThread()))
                .append(" (puncak ").append(nz(performaLog.getJumlahThreadPuncak()))
                .append(", daemon ").append(nz(performaLog.getJumlahDaemon())).append(")\n");
        text.append("RUNNABLE      : ").append(nz(performaLog.getJumlahRunnable())).append('\n');
        text.append("WAITING       : ").append(nz(performaLog.getJumlahWaiting())).append('\n');
        text.append("TIMED_WAITING : ").append(nz(performaLog.getJumlahTimedWaiting())).append('\n');
        text.append("BLOCKED       : ").append(nz(performaLog.getJumlahBlocked())).append('\n');
        text.append("Deadlock      : ").append(nz(performaLog.getJumlahDeadlock())).append('\n');
        int heap = performaLog.getPersenHeap();
        text.append("Heap          : ").append(PerformaSnapshotUtil.formatBytes(performaLog.getHeapDipakai()))
                .append(" / ").append(PerformaSnapshotUtil.formatBytes(performaLog.getHeapMaksimum()))
                .append(heap < 0 ? "" : (" (" + heap + "%)")).append('\n');
        text.append("GC            : ").append(nzLong(performaLog.getGcJumlah())).append("x, ")
                .append(nzLong(performaLog.getGcWaktuMillis())).append(" ms\n");
        text.append("Beban sistem  : ").append(performaLog.getBebanSistem()).append('\n');
        if (full) {
            text.append("\n=== DETAIL ===\n");
            text.append(nullToEmpty(performaLog.getKeterangan()));
        }
        return text.toString();
    }

    private void copyToClipboard(String text) {
        try {
            Clients.evalJavaScript(buildCopyToClipboardScript(text));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void downloadFiltered(boolean aiMode) {
        try {
            showProgress(10, "Menyiapkan download", "Mengumpulkan snapshot performa sesuai filter.");
            String report = buildFilteredReport(aiMode);
            String prefix = aiMode ? "prompt_ai_performa_" : "performa_log_filter_";
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
            showProgress(10, "Menyiapkan teks AI", "Mengumpulkan snapshot performa sesuai filter.");
            String report = buildFilteredReport(true);
            showProgress(90, "Menyalin", "Teks instruksi AI sedang disalin ke clipboard.");
            Clients.evalJavaScript(buildCopyToClipboardScript(report));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            hideProgress();
        }
    }

    @SuppressWarnings("unchecked")
    private String buildFilteredReport(boolean aiMode) {
        StringBuilder text = new StringBuilder(65536);
        int chunkSize = EXPORT_CHUNK_SIZE_DEFAULT;
        if (aiMode) {
            text.append("Tolong analisa SEMUA snapshot performa eCampus berikut dan rangkum tren serta rekomendasinya.\n");
            text.append("Identifikasi kapan sistem mulai bermasalah (thread menunggu meningkat, deadlock, heap mendekati penuh, GC sering).\n\n");
        } else {
            text.append("DOWNLOAD PERFORMA LOG ECAMPUS SESUAI FILTER\n");
        }
        text.append("Dibuat pada : ").append(dateTimeFormat.format(new Date())).append('\n');
        text.append("Periode     : ").append(buildInfoPeriode()).append('\n');
        text.append("Hanya bermasalah: ").append(isHanyaBermasalah() ? "YA" : "TIDAK").append('\n');
        text.append("Total sesuai filter: ").append(countFilteredRows()).append("\n\n");

        int offset = 0;
        int nomor = 1;
        boolean selesai = false;
        while (!selesai) {
            Session session = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                List<PerformaLog> logs = initCriteria(session, true).setFirstResult(offset)
                        .setMaxResults(chunkSize).list();
                if (logs == null || logs.isEmpty()) {
                    break;
                }
                for (int i = 0; i < logs.size(); i++) {
                    PerformaLog logData = logs.get(i);
                    if (logData == null) {
                        continue;
                    }
                    text.append("\n------------------------------------------------------------\n");
                    text.append(aiMode ? "SNAPSHOT KE-" : "CATATAN KE-").append(nomor).append('\n');
                    text.append(buildCopyText(logData, true)).append('\n');
                    nomor++;
                }
                offset += logs.size();
                if (logs.size() < chunkSize) {
                    selesai = true;
                }
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
                break;
            } finally {
                closeSessionQuietly(session);
            }
        }
        if (nomor == 1) {
            text.append("Tidak ada catatan performa yang sesuai dengan filter saat ini.\n");
        }
        return text.toString();
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
        js.append("function fallback(){var ta=document.createElement('textarea');ta.value=text;ta.setAttribute('readonly','readonly');ta.style.position='fixed';ta.style.left='-9999px';ta.style.top='0';document.body.appendChild(ta);ta.focus();ta.select();try{var ok=document.execCommand('copy');toast(ok?'Performa berhasil disalin ke clipboard':'Silakan salin manual dari kotak yang muncul');if(!ok){window.prompt('Copy:',text);}}catch(e){window.prompt('Copy:',text);}try{document.body.removeChild(ta);}catch(e){}}");
        js.append("if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(text).then(function(){toast('Performa berhasil disalin ke clipboard');},function(){fallback();});}else{fallback();}");
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

    private boolean isHanyaBermasalah() {
        try {
            return hanyaBermasalah != null && hanyaBermasalah.isChecked();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kriteria ini harus selalu sejalan dengan {@link PerformaLog#getStatusKesehatan()}.
     * Satu atau dua thread BLOCKED merupakan kejadian transien dan masih berstatus Normal,
     * sehingga tidak boleh ikut saat pengguna meminta hanya data yang bermasalah.
     */
    private Disjunction buatKriteriaBermasalah() {
        Disjunction masalah = Restrictions.disjunction();
        masalah.add(Restrictions.gt("jumlahDeadlock", Integer.valueOf(0)));
        masalah.add(Restrictions.ge("jumlahBlocked",
                Integer.valueOf(PerformaLog.AMBANG_BLOCKED_PERHATIAN)));
        masalah.add(Restrictions.ge("maxThreadSatuLock",
                Integer.valueOf(PerformaLog.AMBANG_KONTENSI_PERHATIAN)));
        masalah.add(Restrictions.gt("jumlahThread",
                Integer.valueOf(PerformaLog.AMBANG_THREAD_PERHATIAN)));
        masalah.add(Restrictions.sqlRestriction(
                "(COALESCE(heap_maksimum,0)>0 AND COALESCE(heap_dipakai,0)*100"
              + ">=heap_maksimum*" + PerformaLog.AMBANG_MEMORI_PERHATIAN + ")"));
        masalah.add(Restrictions.sqlRestriction(
                "(COALESCE(metaspace_maksimum,0)>0 AND COALESCE(metaspace_dipakai,0)*100"
              + ">=metaspace_maksimum*" + PerformaLog.AMBANG_MEMORI_PERHATIAN + ")"));
        return masalah;
    }

    public Criteria initCriteria(boolean order) {
        return initCriteria(HibernateUtil.currentSession(), order);
    }

    private Criteria initCriteria(Session session, boolean order) {
        Criteria criteria = session.createCriteria(PerformaLog.class);

        String ada = getFilterAda();
        String tidak = getFilterTidak();

        if (ada.length() > 0) {
            criteria.add(Restrictions.ilike("keterangan", ada, MatchMode.ANYWHERE));
        }
        if (tidak.length() > 0) {
            criteria.add(Restrictions.not(Restrictions.ilike("keterangan", tidak, MatchMode.ANYWHERE)));
        }
        if (isHanyaBermasalah()) {
            criteria.add(buatKriteriaBermasalah());
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
                Events.echoEvent("onLoadPerformaLogs", rootComponent, null);
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
            showProgress(20, "Membaca ringkasan", "Dasbor performa sedang dihitung.");
            refreshDashboard();

            showProgress(55, "Menghitung halaman", "Jumlah snapshot sesuai filter sedang dihitung.");
            int totalRows = countFilteredRows();
            updatePagingSafely(totalRows);

            showProgress(75, "Mengambil daftar", "Data performa halaman aktif sedang dimuat.");
            List<PerformaLog> logs = loadCurrentPageLogs();

            showProgress(95, "Menampilkan data", "Tabel performa sedang disusun.");
            if (grid != null) {
                ListModel model = new SimpleListModel(logs);
                grid.setRowRenderer(new PerformaLogRenderer());
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
            int maxPage = Common.ROWS_COUNT_ON_PAGE <= 0 ? 0
                    : ((totalRows + Common.ROWS_COUNT_ON_PAGE - 1) / Common.ROWS_COUNT_ON_PAGE) - 1;
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
    private List<PerformaLog> loadCurrentPageLogs() {
        int activePage = paging == null ? 0 : paging.getActivePage();
        int first = Common.ROWS_COUNT_ON_PAGE * (activePage < 0 ? 0 : activePage);
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<PerformaLog> result = initCriteria(session, true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                    .setFirstResult(first).list();
            return result == null ? new ArrayList<PerformaLog>() : result;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<PerformaLog>();
        } finally {
            closeSessionQuietly(session);
        }
    }

    private void showProgress(int percent, String title, String detail) {
        if (progressHtml == null) {
            try {
                Clients.showBusy((title == null ? "Memuat data" : title) + " - " + (detail == null ? "" : detail));
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:842");
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
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:871");
            }
            return;
        }
        try {
            progressHtml.setContent("");
            progressHtml.setVisible(false);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:878");
        }
    }

    /**
     * Menyusun klausa WHERE SQL (rentang tanggal) untuk laporan performa, mengikuti filter tanggal di
     * layar ({@code start}/{@code end}); bila kosong dipakai 7 hari terakhir (data performa biasanya
     * padat, rentang pendek lebih relevan). Memakai {@code Common.databaseDateFormat}.
     */
    private String whereLaporanPerforma() {
        java.util.Date m = start == null ? null : start.getValue();
        java.util.Date s = end == null ? null : end.getValue();
        if (s == null) {
            s = ais.ui.util.WaktuUtil.getDate();
        }
        if (m == null) {
            java.util.Calendar c = ais.ui.util.WaktuUtil.getCalendar();
            c.setTime(s);
            c.add(java.util.Calendar.DATE, -7);
            m = c.getTime();
        }
        java.text.SimpleDateFormat f = Common.databaseDateFormat.get();
        return " where date(tanggal_dirubah) between date('" + f.format(m) + "') and date('" + f.format(s) + "') ";
    }

    /**
     * Deskripsi laporan "Ringkasan Performa Sistem" untuk mesin
     * {@link ais.action.master.helper.DashboardReportKit}. Query hanya mengambil agregat/kolom ringkas
     * (bukan TEXT penuh) demi hemat memori. Berisi: KPI kondisi puncak, tren jumlah proses (grafik
     * garis), titik rebutan kunci terbanyak (batang), dan rekaman terbaru (tabel). Istilah teknis
     * (thread/blocked/deadlock/lock) disederhanakan agar mudah dipahami pengguna non-IT.
     */
    private ais.action.master.helper.DashboardReportKit.SumberLaporan buatSumberLaporanPerforma() {
        return new ais.action.master.helper.DashboardReportKit.SumberLaporan() {
            @Override
            public String judul() {
                return "Ringkasan Performa Sistem";
            }

            @Override
            public String subjudul() {
                return "";
            }

            @Override
            public String deskripsi() {
                return "Melihat seberapa berat kerja sistem: jumlah proses, kemacetan, dan pemakaian memori dari waktu ke waktu.";
            }

            @Override
            public java.util.List<ais.action.master.helper.DashboardReportKit.Bagian> bagian() {
                final String w = whereLaporanPerforma();
                java.util.List<ais.action.master.helper.DashboardReportKit.Bagian> b =
                        new java.util.ArrayList<ais.action.master.helper.DashboardReportKit.Bagian>();

                b.add(ais.action.master.helper.DashboardReportKit.kpi("Kondisi Puncak",
                        "Angka tertinggi yang pernah tercatat pada rentang tanggal terpilih.",
                        new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
                            @Override
                            public java.util.List<Object[]> ambil() {
                                java.util.List<Object[]> r = new java.util.ArrayList<Object[]>();
                                java.util.List<Object[]> q = ais.action.master.helper.DashboardReportKit.sql(
                                        "select count(*), coalesce(max(jumlah_thread),0), coalesce(max(jumlah_blocked),0), "
                                        + "coalesce(max(jumlah_deadlock),0), coalesce(max(max_thread_satu_lock),0), "
                                        + "coalesce(round(max(heap_dipakai)/1048576.0),0) from performa_log" + w);
                                if (!q.isEmpty()) {
                                    Object[] x = q.get(0);
                                    r.add(new Object[] { "Jumlah Rekaman", ais.action.master.helper.DashboardReportKit.fmt(ais.action.master.helper.DashboardReportKit.L(x[0])), "kali sistem diperiksa" });
                                    r.add(new Object[] { "Proses Tertinggi", ais.action.master.helper.DashboardReportKit.fmt(ais.action.master.helper.DashboardReportKit.L(x[1])), "banyak proses berjalan bersamaan" });
                                    r.add(new Object[] { "Antrean Macet", ais.action.master.helper.DashboardReportKit.fmt(ais.action.master.helper.DashboardReportKit.L(x[2])), "proses menunggu giliran" });
                                    r.add(new Object[] { "Kebuntuan", ais.action.master.helper.DashboardReportKit.fmt(ais.action.master.helper.DashboardReportKit.L(x[3])), "saling menunggu (deadlock)" });
                                    r.add(new Object[] { "Rebutan Kunci", ais.action.master.helper.DashboardReportKit.fmt(ais.action.master.helper.DashboardReportKit.L(x[4])), "banyak proses berebut sumber daya sama" });
                                    r.add(new Object[] { "Memori Puncak", ais.action.master.helper.DashboardReportKit.fmt(ais.action.master.helper.DashboardReportKit.L(x[5])) + " MB", "pemakaian memori tertinggi" });
                                }
                                return r;
                            }
                        }));

                b.add(ais.action.master.helper.DashboardReportKit.garis("Tren Jumlah Proses",
                        "Naik-turun jumlah proses tiap hari; makin tinggi berarti sistem makin sibuk.",
                        new String[] { "Tanggal", "Proses Tertinggi" },
                        new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
                            @Override
                            public java.util.List<Object[]> ambil() {
                                return ais.action.master.helper.DashboardReportKit.sql(
                                        "select to_char(tanggal_dirubah,'YYYY-MM-DD'), coalesce(max(jumlah_thread),0) from performa_log" + w
                                        + " group by 1 order by 1");
                            }
                        }));

                b.add(ais.action.master.helper.DashboardReportKit.batang("Titik Rebutan Kunci Terbanyak",
                        "Bagian program yang paling sering menyebabkan antrean; petunjuk untuk optimasi.",
                        new String[] { "Bagian Program", "Kejadian" },
                        new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
                            @Override
                            public java.util.List<Object[]> ambil() {
                                return ais.action.master.helper.DashboardReportKit.sql(
                                        "select coalesce(nullif(top_class,''),'(tidak ada)'), count(*) from performa_log" + w
                                        + " group by 1 order by 2 desc limit 12");
                            }
                        }));

                b.add(ais.action.master.helper.DashboardReportKit.tabel("Rekaman Terbaru",
                        "Daftar hasil pemeriksaan sistem paling akhir beserta waktunya.",
                        new String[] { "Waktu", "Proses", "Antrean Macet", "Kebuntuan", "Memori (MB)" },
                        new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
                            @Override
                            public java.util.List<Object[]> ambil() {
                                return ais.action.master.helper.DashboardReportKit.sql(
                                        "select to_char(tanggal_dirubah,'YYYY-MM-DD HH24:MI'), coalesce(jumlah_thread,0), "
                                        + "coalesce(jumlah_blocked,0), coalesce(jumlah_deadlock,0), "
                                        + "coalesce(round(heap_dipakai/1048576.0),0) from performa_log" + w
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
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:1006");
                }
                try {
                    session.disconnect();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:1010");
                }
                try {
                    session.close();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:1014");
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:1017");
        }
    }

    private void refreshDashboard() {
        if (dashboardHtml == null) {
            return;
        }
        try {
            DashboardData data = loadDashboardData();
            PerformaSnapshotUtil.Analisa analisa = null;
            try {
                analisa = PerformaSnapshotUtil.analisaLengkap();
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:1030");
                // Analisa langsung tidak boleh menggagalkan dasbor; riwayat tetap tampil.
            }
            dashboardHtml.setContent(buildDashboardHtml(data, analisa));
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
            html.append("<div style='font-weight:800;font-size:15px;margin-bottom:6px;'>Data performa belum bisa ditampilkan.</div>");
            html.append("<div style='font-size:12px;line-height:1.5;'>Terjadi kendala saat membaca PerformaLog. Coba muat ulang halaman atau rekam snapshot baru.</div>");
            if (e != null) {
                html.append("<pre style='white-space:pre-wrap;margin-top:10px;font-size:11px;'>").append(escapeHtml(e.getClass().getName() + ": " + e.getMessage())).append("</pre>");
            }
            html.append("</div>");
            dashboardHtml.setContent(html.toString());
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PerformaLogAction.java:1054");
        }
    }

    @SuppressWarnings("unchecked")
    private DashboardData loadDashboardData() {
        DashboardData data = new DashboardData();
        data.total = count(null);
        data.today = count(startOfDay(0));

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<PerformaLog> logs = initCriteria(session, true).setMaxResults(getDashboardLimit()).list();
            data.logs = logs == null ? new ArrayList<PerformaLog>() : logs;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            data.logs = new ArrayList<PerformaLog>();
        } finally {
            closeSessionQuietly(session);
        }

        for (int i = 0; i < data.logs.size(); i++) {
            PerformaLog logData = data.logs.get(i);
            if (logData == null) {
                continue;
            }
            String status = logData.getStatusKesehatan();
            if ("Kritis".equals(status)) {
                data.kritis++;
            } else if ("Perhatian".equals(status)) {
                data.perhatian++;
            } else {
                data.normal++;
            }
            if (i == 0) {
                data.terkini = logData;
            }
        }
        return data;
    }

    private int getDashboardLimit() {
        return DASHBOARD_LIMIT;
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

    private String buildDashboardHtml(DashboardData data, PerformaSnapshotUtil.Analisa analisa) {
        StringBuilder html = new StringBuilder(48000);
        html.append("<style>");
        html.append(".plog{font-family:Arial,Helvetica,sans-serif;color:#0f172a;padding:14px;background:#f8fafc;}");
        html.append(".plog-title{font-size:22px;font-weight:900;margin:0 0 6px;color:#0f172a;}");
        html.append(".plog-sub{font-size:13px;color:#64748b;margin-bottom:14px;line-height:1.5;}");
        html.append(".plog-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin-bottom:14px;}");
        html.append(".plog-card{background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 10px 25px rgba(15,23,42,.06);}");
        html.append(".plog-num{font-size:24px;font-weight:900;color:#1d4ed8;margin-top:8px;}");
        html.append(".plog-label{font-size:12px;font-weight:800;color:#475569;text-transform:uppercase;letter-spacing:.04em;}");
        html.append(".plog-help{font-size:12px;color:#64748b;line-height:1.45;margin-top:8px;}");
        html.append(".plog-panels{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:14px;}");
        html.append(".plog-full{grid-template-columns:1fr;}");
        html.append(".plog-panel{background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 10px 25px rgba(15,23,42,.05);overflow:auto;}");
        html.append(".plog-panel h3{font-size:15px;margin:0 0 5px;color:#0f172a;}");
        html.append(".plog-desc{font-size:12px;color:#64748b;line-height:1.5;margin-bottom:12px;}");
        html.append(".bar-row{display:grid;grid-template-columns:84px 1fr 52px;align-items:center;gap:8px;margin:8px 0;font-size:11px;color:#475569;}");
        html.append(".bar-bg{height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;}");
        html.append(".bar-bg b{display:block;height:12px;border-radius:999px;background:linear-gradient(90deg,#60a5fa,#2563eb);}");
        html.append(".mini{font-size:11px;color:#64748b;}");
        html.append(".plog-table{width:100%;border-collapse:collapse;font-size:11px;}");
        html.append(".plog-table th{text-align:left;color:#475569;font-weight:800;border-bottom:2px solid #e2e8f0;padding:6px 8px;}");
        html.append(".plog-table td{border-bottom:1px solid #eef2f7;padding:6px 8px;color:#334155;word-break:break-word;}");
        html.append(".plog-table td.num{text-align:right;font-weight:800;color:#0f172a;white-space:nowrap;}");
        html.append(".plog-rec{background:#fff;border:1px solid #e2e8f0;border-left:5px solid #2563eb;border-radius:14px;padding:12px 14px;margin-bottom:14px;}");
        html.append(".plog-rec li{margin:5px 0;font-size:12px;color:#334155;line-height:1.5;}");
        html.append(".code{white-space:pre-wrap;word-break:break-word;background:#0f172a;color:#e2e8f0;border-radius:10px;padding:10px;font-size:11px;line-height:1.45;}");
        html.append(".mono{font-family:Consolas,Menlo,monospace;}");
        html.append("@media(max-width:900px){.plog-grid,.plog-panels{grid-template-columns:1fr;}}");
        html.append("</style>");

        PerformaLog live = analisa == null ? null : analisa.ringkas;

        html.append("<div class='plog'>");
        html.append("<div class='plog-title'>Dasbor Performa Sistem</div>");
        html.append("<div class='plog-sub'>Investigasi kondisi runtime Java secara langsung: class aplikasi yang menahan thread, thread yang menunggu/terblokir, kontensi lock (pool koneksi), tekanan memori (heap/old gen/metaspace), serta histogram objek untuk menelusuri kebocoran memori.</div>");

        html.append("<div class='plog-grid'>");
        appendCard(html, "Total Snapshot", formatNumber(data.total), "Catatan performa tersimpan (riwayat).");
        appendCard(html, "Status Langsung", live == null ? "-" : escapeHtml(live.getStatusKesehatan()),
                live == null ? "Analisa langsung tidak tersedia." : "Hasil pembacaan JVM saat dasbor dibuka.");
        appendCard(html, "Thread Aktif", live == null ? "-" : (nz(live.getJumlahThread()) + " / nunggu " + live.getJumlahMenunggu()),
                "Total thread dan yang sedang menunggu/terblokir.");
        appendCard(html, "Heap / Old / Meta", live == null ? "-" : (persenStr(live.getPersenHeap()) + " / "
                + persenStr(live.getPersenOldGen()) + " / " + persenStr(live.getPersenMetaspace())),
                "Pemakaian heap, old gen, dan metaspace saat ini.");
        html.append("</div>");

        appendRekomendasi(html, analisa);

        html.append("<div class='plog-title' style='font-size:18px;'>Investigasi Langsung</div>");

        html.append("<div class='plog-panels'>");
        appendMemoryPoolPanel(html, analisa);
        appendTerkiniPanel(html, live);
        html.append("</div>");

        html.append("<div class='plog-panels'>");
        appendHitungPanel(html, "Class Aplikasi Terbanyak di Stack",
                "Class ais.* yang muncul di paling banyak thread - kandidat kode lambat/macet atau dipanggil berlebihan.",
                analisa == null ? null : analisa.topKelasAis, "thread");
        appendHitungPanel(html, "Grup Thread",
                "Thread dikelompokkan dari pola namanya. Angka besar pada satu grup = thread dibuat tak terkendali.",
                analisa == null ? null : analisa.grupThread, "thread");
        html.append("</div>");

        html.append("<div class='plog-panels'>");
        appendHitungPanel(html, "Kontensi Lock",
                "Banyak thread menunggu lock yang sama = bottleneck. Pola c3p0/ResourcePool menandakan pool koneksi DB habis.",
                analisa == null ? null : analisa.kontensiLock, "penunggu");
        appendHitungPanel(html, "Class Terbanyak di Stack (semua)",
                "Termasuk library/framework; membantu melihat lapisan yang menahan thread.",
                analisa == null ? null : analisa.topKelas, "thread");
        html.append("</div>");

        html.append("<div class='plog-panels'>");
        appendStringListPanel(html, "Thread Mencurigakan (RUNNABLE)",
                "Thread yang sedang aktif menjalankan kode aplikasi saat snapshot diambil.",
                analisa == null ? null : analisa.threadMencurigakan,
                "Tidak ada thread aplikasi yang sedang berjalan.");
        appendDeadlockGcPanel(html, analisa);
        html.append("</div>");

        html.append("<div class='plog-panels plog-full'>");
        appendHistogramPanel(html);
        html.append("</div>");

        if (analisa != null && analisa.infoVm != null && analisa.infoVm.length() > 0) {
            html.append("<div class='plog-panels plog-full'><div class='plog-panel'><h3>Info Mesin Virtual</h3>");
            html.append("<pre class='code'>").append(escapeHtml(analisa.infoVm)).append("</pre></div></div>");
        }

        html.append("<div class='plog-title' style='font-size:18px;margin-top:6px;'>Riwayat &amp; Tren</div>");
        html.append("<div class='plog-panels'>");
        appendTrendThreadPanel(html, data);
        appendTrendHeapPanel(html, data);
        html.append("</div>");
        html.append("<div class='plog-panels'>");
        appendStatusPanel(html, data);
        appendLatestPanel(html, data);
        html.append("</div>");
        html.append("<div class='plog-panels plog-full'>");
        appendDeadlockPanel(html, data);
        html.append("</div>");

        html.append("<div class='mini'>Riwayat memakai maksimal ").append(DASHBOARD_LIMIT)
                .append(" snapshot terbaru. Investigasi langsung dibaca real-time tiap dasbor dibuka. Histogram objek bersifat manual karena memicu Full GC.</div>");
        html.append("</div>");
        return html.toString();
    }

    private String persenStr(int persen) {
        return persen < 0 ? "-" : (persen + "%");
    }

    private void appendRekomendasi(StringBuilder html, PerformaSnapshotUtil.Analisa analisa) {
        html.append("<div class='plog-rec'>");
        html.append("<div style='font-weight:900;font-size:14px;margin-bottom:6px;'>Rekomendasi Investigasi</div>");
        html.append("<ul style='margin:0;padding-left:18px;'>");
        if (analisa == null || analisa.rekomendasi == null || analisa.rekomendasi.isEmpty()) {
            html.append("<li>Analisa langsung belum tersedia.</li>");
        } else {
            for (int i = 0; i < analisa.rekomendasi.size(); i++) {
                html.append("<li>").append(escapeHtml(analisa.rekomendasi.get(i))).append("</li>");
            }
        }
        html.append("</ul></div>");
    }

    private void appendMemoryPoolPanel(StringBuilder html, PerformaSnapshotUtil.Analisa analisa) {
        html.append("<div class='plog-panel'><h3>Memori per Area (jstat-like)</h3>");
        html.append("<div class='plog-desc'>Pemakaian tiap area memori: Eden/Survivor (objek baru), Old Gen (objek lama - indikator kebocoran), Metaspace (class), Code Cache.</div>");
        List<PerformaSnapshotUtil.Hitung> pools = analisa == null ? null : analisa.memoryPool;
        if (pools == null || pools.isEmpty()) {
            html.append("<div class='mini'>Data memory pool tidak tersedia.</div></div>");
            return;
        }
        html.append("<table class='plog-table'><tr><th>Area</th><th>Pemakaian</th><th style='width:130px;'>%</th></tr>");
        for (int i = 0; i < pools.size(); i++) {
            PerformaSnapshotUtil.Hitung h = pools.get(i);
            int persen = (int) h.nilai;
            html.append("<tr><td>").append(escapeHtml(h.label)).append("</td>");
            html.append("<td class='mini'>").append(escapeHtml(h.ekstra)).append("</td>");
            html.append("<td><div class='bar-bg'><b style='width:").append(persen < 0 ? 0 : persen)
                    .append("%'></b></div></td></tr>");
        }
        html.append("</table></div>");
    }

    private void appendHitungPanel(StringBuilder html, String judul, String desc,
            List<PerformaSnapshotUtil.Hitung> list, String satuan) {
        html.append("<div class='plog-panel'><h3>").append(escapeHtml(judul)).append("</h3>");
        html.append("<div class='plog-desc'>").append(escapeHtml(desc)).append("</div>");
        if (list == null || list.isEmpty()) {
            html.append("<div class='mini'>Belum ada data.</div></div>");
            return;
        }
        html.append("<table class='plog-table'><tr><th>Nama</th><th class='num'>").append(escapeHtml(satuan))
                .append("</th><th>Keterangan</th></tr>");
        for (int i = 0; i < list.size(); i++) {
            PerformaSnapshotUtil.Hitung h = list.get(i);
            html.append("<tr><td class='mono'>").append(escapeHtml(h.label)).append("</td>");
            html.append("<td class='num'>").append(h.nilai).append("</td>");
            html.append("<td class='mini'>").append(h.ekstra == null ? "" : escapeHtml(h.ekstra)).append("</td></tr>");
        }
        html.append("</table></div>");
    }

    private void appendStringListPanel(StringBuilder html, String judul, String desc, List<String> baris,
            String kosongMsg) {
        html.append("<div class='plog-panel'><h3>").append(escapeHtml(judul)).append("</h3>");
        html.append("<div class='plog-desc'>").append(escapeHtml(desc)).append("</div>");
        if (baris == null || baris.isEmpty()) {
            html.append("<div class='mini'>").append(escapeHtml(kosongMsg)).append("</div></div>");
            return;
        }
        for (int i = 0; i < baris.size(); i++) {
            html.append("<div class='mini mono' style='border-bottom:1px solid #eef2f7;padding:5px 0;'>")
                    .append(escapeHtml(baris.get(i))).append("</div>");
        }
        html.append("</div>");
    }

    private void appendDeadlockGcPanel(StringBuilder html, PerformaSnapshotUtil.Analisa analisa) {
        html.append("<div class='plog-panel'><h3>Deadlock &amp; Garbage Collector</h3>");
        html.append("<div class='plog-desc'>Deadlock harus segera ditangani. Aktivitas GC tinggi (waktu besar) menandakan tekanan memori.</div>");
        List<String> dl = analisa == null ? null : analisa.deadlockDetail;
        if (dl == null || dl.isEmpty()) {
            html.append("<div class='mini' style='color:#16a34a;font-weight:700;'>Tidak ada deadlock terdeteksi.</div>");
        } else {
            html.append("<div style='font-weight:800;color:#b91c1c;font-size:12px;margin-bottom:4px;'>DEADLOCK:</div>");
            for (int i = 0; i < dl.size(); i++) {
                html.append("<div class='mini mono' style='color:#b91c1c;padding:3px 0;'>")
                        .append(escapeHtml(dl.get(i))).append("</div>");
            }
        }
        List<PerformaSnapshotUtil.Hitung> gcs = analisa == null ? null : analisa.gcDetail;
        if (gcs != null && !gcs.isEmpty()) {
            html.append("<table class='plog-table' style='margin-top:10px;'><tr><th>Collector</th><th class='num'>Jumlah</th><th>Waktu</th></tr>");
            for (int i = 0; i < gcs.size(); i++) {
                PerformaSnapshotUtil.Hitung h = gcs.get(i);
                html.append("<tr><td>").append(escapeHtml(h.label)).append("</td><td class='num'>").append(h.nilai)
                        .append("</td><td class='mini'>").append(escapeHtml(h.ekstra)).append("</td></tr>");
            }
            html.append("</table>");
        }
        html.append("</div>");
    }

    private void appendHistogramPanel(StringBuilder html) {
        html.append("<div class='plog-panel'><h3>Histogram Objek (Memory Leak)</h3>");
        html.append("<div class='plog-desc'>Daftar class pemakai memori terbesar (setara jmap -histo:live). Klik tombol \"Ambil Histogram Objek (berat)\" di toolbar untuk mengisi/memperbarui - operasi ini memicu Full GC.</div>");
        if (histogramTerakhir == null || histogramTerakhir.isEmpty()) {
            html.append("<div class='mini'>Belum diambil. Gunakan tombol di toolbar saat ingin mencari penyebab kebocoran memori.</div></div>");
            return;
        }
        html.append("<div class='mini' style='margin-bottom:6px;'>Diambil: ")
                .append(histogramWaktu == null ? "-" : dateTimeFormat.format(histogramWaktu)).append("</div>");
        html.append("<table class='plog-table'><tr><th>#</th><th>Class</th><th class='num'>Ukuran</th><th>Instance</th></tr>");
        for (int i = 0; i < histogramTerakhir.size(); i++) {
            PerformaSnapshotUtil.Hitung h = histogramTerakhir.get(i);
            html.append("<tr><td class='mini'>").append(i + 1).append("</td>");
            html.append("<td class='mono'>").append(escapeHtml(h.label)).append("</td>");
            html.append("<td class='num'>").append(PerformaSnapshotUtil.formatBytes(Long.valueOf(h.nilai))).append("</td>");
            html.append("<td class='mini'>").append(h.ekstra == null ? "" : escapeHtml(h.ekstra)).append("</td></tr>");
        }
        html.append("</table></div>");
    }

    private void appendCard(StringBuilder html, String label, String value, String help) {
        html.append("<div class='plog-card'>");
        html.append("<div class='plog-label'>").append(label).append("</div>");
        html.append("<div class='plog-num'>").append(value).append("</div>");
        html.append("<div class='plog-help'>").append(help).append("</div>");
        html.append("</div>");
    }

    private void appendTerkiniPanel(StringBuilder html, PerformaLog terkini) {
        html.append("<div class='plog-panel'><h3>Kondisi Terkini</h3>");
        html.append("<div class='plog-desc'>Rincian metrik pada snapshot paling baru.</div>");
        if (terkini == null) {
            html.append("<div class='mini'>Belum ada snapshot. Klik \"Rekam Sekarang\" untuk mengambil yang pertama.</div></div>");
            return;
        }
        int maxThread = nz(terkini.getJumlahThread());
        if (maxThread < 1) {
            maxThread = 1;
        }
        appendBar(html, "RUNNABLE", nz(terkini.getJumlahRunnable()), maxThread);
        appendBar(html, "WAITING", nz(terkini.getJumlahWaiting()), maxThread);
        appendBar(html, "TIMED_WAIT", nz(terkini.getJumlahTimedWaiting()), maxThread);
        appendBar(html, "BLOCKED", nz(terkini.getJumlahBlocked()), maxThread);
        int heap = terkini.getPersenHeap();
        appendBar(html, "Heap %", heap < 0 ? 0 : heap, 100);
        html.append("<div class='mini' style='margin-top:8px;'>Thread total ").append(nz(terkini.getJumlahThread()))
                .append(", daemon ").append(nz(terkini.getJumlahDaemon()))
                .append(", deadlock ").append(nz(terkini.getJumlahDeadlock()))
                .append(", GC ").append(nzLong(terkini.getGcJumlah())).append("x.</div>");
        html.append("</div>");
    }

    private void appendStatusPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='plog-panel'><h3>Komposisi Status</h3>");
        html.append("<div class='plog-desc'>Pembagian snapshot berdasarkan status kesehatan.</div>");
        int max = Math.max(data.normal, Math.max(data.perhatian, data.kritis));
        appendBar(html, "Normal", data.normal, max);
        appendBar(html, "Perhatian", data.perhatian, max);
        appendBar(html, "Kritis", data.kritis, max);
        html.append("</div>");
    }

    private void appendTrendThreadPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='plog-panel'><h3>Tren Jumlah Thread</h3>");
        html.append("<div class='plog-desc'>Perubahan jumlah thread pada snapshot terbaru (kiri = paling lama).</div>");
        List<PerformaLog> deret = ambilDeretTerbaru(data.logs, TREND_POINTS);
        int max = 1;
        for (int i = 0; i < deret.size(); i++) {
            int value = nz(deret.get(i).getJumlahThread());
            if (value > max) {
                max = value;
            }
        }
        for (int i = 0; i < deret.size(); i++) {
            PerformaLog logData = deret.get(i);
            appendBar(html, labelWaktu(logData), nz(logData.getJumlahThread()), max);
        }
        if (deret.isEmpty()) {
            html.append("<div class='mini'>Belum ada data.</div>");
        }
        html.append("</div>");
    }

    private void appendTrendHeapPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='plog-panel'><h3>Tren Pemakaian Heap</h3>");
        html.append("<div class='plog-desc'>Persentase heap pada snapshot terbaru.</div>");
        List<PerformaLog> deret = ambilDeretTerbaru(data.logs, TREND_POINTS);
        for (int i = 0; i < deret.size(); i++) {
            PerformaLog logData = deret.get(i);
            int heap = logData.getPersenHeap();
            appendBar(html, labelWaktu(logData), heap < 0 ? 0 : heap, 100);
        }
        if (deret.isEmpty()) {
            html.append("<div class='mini'>Belum ada data.</div>");
        }
        html.append("</div>");
    }

    private void appendLatestPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='plog-panel'><h3>Snapshot Terbaru</h3>");
        html.append("<div class='plog-desc'>Beberapa catatan terakhir agar kondisi terbaru cepat terlihat.</div>");
        int limit = Math.min(6, data.logs.size());
        for (int i = 0; i < limit; i++) {
            PerformaLog logData = data.logs.get(i);
            html.append("<div style='border-bottom:1px solid #e2e8f0;padding:8px 0;'>");
            html.append("<div style='font-weight:800;font-size:12px;color:#0f172a;'>")
                    .append(logData.getTanggal_dirubah() == null ? "-" : dateTimeFormat.format(logData.getTanggal_dirubah()))
                    .append(" <span style='color:").append(logData.getStatusWarna()).append(";'>")
                    .append(escapeHtml(logData.getStatusKesehatan())).append("</span></div>");
            html.append("<div class='mini'>").append(escapeHtml(logData.getRingkasan())).append("</div>");
            html.append("</div>");
        }
        if (limit == 0) {
            html.append("<div class='mini'>Belum ada catatan performa.</div>");
        }
        html.append("</div>");
    }

    private void appendDeadlockPanel(StringBuilder html, DashboardData data) {
        html.append("<div class='plog-panel'><h3>Perlu Perhatian</h3>");
        html.append("<div class='plog-desc'>Snapshot dengan deadlock atau thread terblokir yang patut diperiksa.</div>");
        int jumlah = 0;
        for (int i = 0; i < data.logs.size() && jumlah < 6; i++) {
            PerformaLog logData = data.logs.get(i);
            if (nz(logData.getJumlahDeadlock()) > 0 || nz(logData.getJumlahBlocked()) > 0) {
                html.append("<div style='border-bottom:1px solid #e2e8f0;padding:8px 0;'>");
                html.append("<div style='font-weight:800;font-size:12px;color:#b91c1c;'>")
                        .append(logData.getTanggal_dirubah() == null ? "-" : dateTimeFormat.format(logData.getTanggal_dirubah()))
                        .append("</div>");
                html.append("<div class='mini'>Deadlock ").append(nz(logData.getJumlahDeadlock()))
                        .append(", BLOCKED ").append(nz(logData.getJumlahBlocked()))
                        .append(", menunggu ").append(logData.getJumlahMenunggu()).append(".</div>");
                html.append("</div>");
                jumlah++;
            }
        }
        if (jumlah == 0) {
            html.append("<div class='mini'>Tidak ada deadlock atau thread terblokir pada data ini. Bagus!</div>");
        }
        html.append("</div>");
    }

    private List<PerformaLog> ambilDeretTerbaru(List<PerformaLog> logs, int jumlah) {
        List<PerformaLog> hasil = new ArrayList<PerformaLog>();
        if (logs == null || logs.isEmpty()) {
            return hasil;
        }
        int batas = Math.min(jumlah, logs.size());
        // logs sudah terurut desc (terbaru dulu). Ambil sebagian lalu balik agar kiri = paling lama.
        for (int i = batas - 1; i >= 0; i--) {
            hasil.add(logs.get(i));
        }
        return hasil;
    }

    private String labelWaktu(PerformaLog logData) {
        if (logData == null || logData.getTanggal_dirubah() == null) {
            return "-";
        }
        return dayTimeShortFormat.format(logData.getTanggal_dirubah());
    }

    private void appendBar(StringBuilder html, String label, int value, int max) {
        int width = max <= 0 ? 0 : (int) Math.round((value * 100.0) / max);
        if (width < 4 && value > 0) {
            width = 4;
        }
        html.append("<div class='bar-row'><span>").append(escapeHtml(label)).append("</span><div class='bar-bg'><b style='width:")
                .append(width).append("%'></b></div><em>").append(value).append("</em></div>");
    }

    private String formatNumber(Long value) {
        return Common.numberFormat1.get().format(value == null ? Long.valueOf(0) : value);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private static long nzLong(Long value) {
        return value == null ? 0L : value.longValue();
    }

    private static class DashboardData {
        Long total = Long.valueOf(0);
        Long today = Long.valueOf(0);
        int normal = 0;
        int perhatian = 0;
        int kritis = 0;
        PerformaLog terkini = null;
        List<PerformaLog> logs = new ArrayList<PerformaLog>();
    }
}
