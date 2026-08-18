package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.common.CommonDashboardHtmlHelper;

/**
 * Penjalan tahapan pemuatan dasbor dengan <b>progress bar yang benar-benar sinkron</b>
 * dengan pekerjaan nyata di server.
 *
 * <h3>Masalah pola lama</h3>
 * Pola lama memanggil beberapa {@code progress.update(...)} di dalam <i>satu</i> event ZK.
 * ZK hanya mengirim perubahan UI ke browser <i>setelah</i> event handler selesai, sehingga
 * progress bar tidak bergerak bertahap — ia "melompat" dari awal langsung ke 100% saat
 * semua query sudah selesai. Inilah penyebab loading terlihat "jalan duluan" / tidak sinkron.
 *
 * <h3>Solusi</h3>
 * Setiap tahap dijalankan di dalam <i>event-nya sendiri</i> yang dirantai memakai
 * {@link Events#echoEvent(String, Component, Object)}. Karena event handler berakhir setelah
 * tiap tahap, ZK <i>flush</i> tampilan progress bar ke browser sebelum tahap berikutnya mulai.
 * Akibatnya persentase hanya naik <i>setelah</i> pekerjaan tahap tersebut benar-benar selesai —
 * bar tidak pernah mendahului data.
 *
 * <h3>Pemakaian</h3>
 * <pre>
 *   new DashboardProgress(this, "Memuat Dasbor Penggajian")
 *       .step("Menghitung ringkasan gaji", new DashboardProgress.Step() {
 *           public void run() throws Exception { loadGaji(); }
 *       })
 *       .step("Menghitung transaksi pegawai", new DashboardProgress.Step() {
 *           public void run() throws Exception { loadTransaksi(); }
 *       })
 *       .onDone(new DashboardProgress.Done() {
 *           public void run() throws Exception { render(); }
 *       })
 *       .start();
 * </pre>
 *
 * Host (komponen tempat bar dirender) akan dibersihkan saat {@link #start()} dipanggil.
 */
public final class DashboardProgress {

    /** Satu tahap pemuatan (biasanya satu blok query/agregasi). */
    public interface Step {
        void run() throws Exception;
    }

    /** Dipanggil sekali setelah semua tahap selesai — tempat merender isi dasbor. */
    public interface Done {
        void run() throws Exception;
    }

    private static final String EVENT = "onDashboardProgressTick";

    private final Component host;
    private final String title;
    private final List<String> labels = new ArrayList<String>();
    private final List<Step> steps = new ArrayList<Step>();
    private Done done;

    private Html bar;
    private int index;

    public DashboardProgress(Component host, String title) {
        this.host = host;
        this.title = title == null ? "Memuat data" : title;
    }

    /** Tambahkan satu tahap berlabel {@code label}. Mengembalikan {@code this} untuk chaining. */
    public DashboardProgress step(String label, Step step) {
        if (step != null) {
            labels.add(label == null ? "Memuat data" : label);
            steps.add(step);
        }
        return this;
    }

    /** Tetapkan aksi render akhir. */
    public DashboardProgress onDone(Done d) {
        this.done = d;
        return this;
    }

    /** Mulai pemuatan bertahap. Aman dipanggil dari constructor komponen dasbor. */
    public void start() {
        if (host == null) {
            return;
        }
        Common.clear(host);
        index = 0;

        if (steps.isEmpty()) {
            finish();
            return;
        }

        // Render bar awal: menampilkan tahap pertama yang akan dikerjakan.
        bar = new Html(CommonDashboardHtmlHelper.progressBar(
                startPercent(), title, "Sedang menyiapkan: " + labels.get(0)));
        bar.setParent(host);

        final EventListener[] holder = new EventListener[1];
        holder[0] = new EventListener() {
            public void onEvent(Event event) throws Exception {
                tick(holder[0]);
            }
        };
        host.addEventListener(EVENT, holder[0]);
        // Echo: event ini diproses pada round-trip berikutnya, setelah bar awal ter-render di browser.
        Events.echoEvent(EVENT, host, null);
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private void tick(EventListener self) {
        try {
            // Jalankan pekerjaan tahap saat ini (bar di browser sedang menampilkan label tahap ini).
            steps.get(index).run();
            index++;

            if (index < steps.size()) {
                // Pekerjaan tahap selesai → naikkan bar & tampilkan label tahap berikutnya,
                // lalu echo agar ZK flush bar sebelum tahap berikutnya dieksekusi.
                int percent = (int) Math.round(index * 90.0 / steps.size());
                updateBar(percent, "Sedang menyiapkan: " + labels.get(index));
                Events.echoEvent(EVENT, host, null);
            } else {
                // Semua tahap selesai.
                host.removeEventListener(EVENT, self);
                updateBar(100, "Selesai. Menampilkan dasbor…");
                finish();
            }
        } catch (Exception e) {
            try {
                host.removeEventListener(EVENT, self);
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/ui/util/DashboardProgress.java:144");
            }
            Common.tampilErrorJikaAdmin(e);
            if (bar != null) {
                bar.setContent(CommonDashboardHtmlHelper.errorState(title, e));
            }
        }
    }

    private void finish() {
        try {
            if (bar != null) {
                bar.detach();
                bar = null;
            }
            if (done != null) {
                done.run();
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private int startPercent() {
        // Tahap pertama belum selesai → tampilkan sedikit progress agar bar terlihat hidup.
        return steps.size() <= 1 ? 5 : Math.max(3, (int) Math.round(2.0));
    }

    private void updateBar(int percent, String detail) {
        if (bar != null) {
            bar.setContent(CommonDashboardHtmlHelper.progressBar(percent, title, detail));
        }
    }
}
