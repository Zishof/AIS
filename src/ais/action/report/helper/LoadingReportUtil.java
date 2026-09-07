package ais.action.report.helper;
import ais.common.PesanFormalHelper;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

/**
 * Helper kecil untuk mencegah indikator loading laporan ZK 5.5 menggantung.
 * Semua method dibuat defensif agar error UI tidak menghentikan proses laporan.
 */
public final class LoadingReportUtil {

    /**
     * Kunci atribut yang disimpan pada {@link Label} untuk menandai bahwa proses
     * laporan sudah selesai. Diset oleh {@link #selesai(Label)} dan dibaca oleh
     * {@link #isSelesai(Label)}.
     */
    private static final String STATUS_SELESAI = "__LAPORAN_SELESAI__";

    /**
     * Kunci atribut yang disimpan pada {@link Label} untuk menandai bahwa proses
     * laporan berakhir dengan error. Diset oleh {@link #gagal(Label, Throwable)}
     * dan dibaca oleh {@link #isError(Label)}.
     */
    private static final String STATUS_ERROR = "__LAPORAN_ERROR__";

    /**
     * Konstruktor privat. Kelas ini hanya berisi method statis (utility class)
     * sehingga tidak dimaksudkan untuk diinstansiasi.
     */
    private LoadingReportUtil() {
    }

    /**
     * Menampilkan atau menghapus indikator busy ZK berdasarkan status dan isi
     * {@code label} pemantau progres laporan.
     * <p>
     * Jika {@code label} sudah bertanda selesai ({@link #isSelesai(Label)}) atau
     * error ({@link #isError(Label)}), atau isinya kosong, indikator busy akan
     * dihapus lewat {@link #clearBusy()}. Selain itu, teks {@code label} saat ini
     * ditampilkan sebagai pesan busy lewat {@link #showBusyText(String)}.
     *
     * @param label komponen ZK {@link Label} yang menyimpan teks progres dan
     *              atribut status laporan; aman dipanggil dengan {@code null}
     *              (diperlakukan sebagai selesai).
     */
    public static void showBusy(final Label label) {
        if (isSelesai(label) || isError(label)) {
            clearBusy();
            return;
        }
        String teks = getValue(label);
        if (teks == null || teks.trim().length() == 0) {
            clearBusy();
            return;
        }
        showBusyText(teks);
    }

    /**
     * Menampilkan overlay busy ZK berisi {@code teks}, atau menghapusnya bila
     * {@code teks} kosong/null.
     * <p>
     * Method ini defensif terhadap konteks eksekusi ZK yang tidak aktif:
     * {@link Clients#showBusy(String)}/{@link Clients#clearBusy()} membutuhkan
     * {@link Executions#getCurrent()} tidak null karena keduanya mengirim
     * sinyal ke client lewat {@code Clients.response()}. Bila dipanggil dari
     * thread background (mis. proses laporan yang berjalan di luar siklus
     * request ZK) tanpa konteks aktif, panggilan akan menyebabkan
     * {@code NullPointerException} karena tidak ada desktop/client untuk
     * dikirimi sinyal; pada kondisi itu method ini langsung kembali tanpa
     * melakukan apa pun. Error lain saat memanggil Clients dicatat lewat
     * {@link ais.common.ErrorAuditUtil#record} dan ditampilkan sebagai pesan
     * gagal formal, namun tidak dilempar ulang (fail-silent) agar proses
     * laporan tidak terhenti hanya karena indikator UI gagal diperbarui.
     *
     * @param teks pesan busy yang ditampilkan; {@code null} atau string kosong
     *             (setelah di-trim) akan menghapus overlay busy.
     */
    public static void showBusyText(final String teks) {
        // Clients.showBusy/clearBusy butuh ZK Execution/Desktop aktif di thread
        // saat ini. Dipanggil dari thread background (proses laporan) tanpa
        // konteks ZK akan NPE di dalam Clients.response(); tidak ada client
        // untuk disinyal, jadi lewati saja bila tidak ada eksekusi aktif.
        if (Executions.getCurrent() == null) {
            return;
        }
        try {
            if (teks == null || teks.trim().length() == 0) {
                Clients.clearBusy();
            } else {
                Clients.showBusy(teks);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LoadingReportUtil.java:39");
        	PesanFormalHelper.tampilkanGagalException("pemrosesan Loading Report Util", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
        		new String[] {
        			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
        			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
        			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
        		});
        }
    }

    /**
     * Menghapus overlay busy ZK ({@link Clients#clearBusy()}) bila konteks
     * eksekusi ZK sedang aktif ({@link Executions#getCurrent()} tidak null).
     * <p>
     * Sama seperti {@link #showBusyText(String)}, method ini fail-silent:
     * bila dipanggil tanpa konteks ZK aktif, langsung kembali tanpa efek;
     * exception lain saat memanggil Clients dicatat lewat
     * {@link ais.common.ErrorAuditUtil#record} dan ditampilkan sebagai pesan
     * gagal formal tanpa dilempar ulang.
     */
    public static void clearBusy() {
        if (Executions.getCurrent() == null) {
            return;
        }
        try {
            Clients.clearBusy();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LoadingReportUtil.java:46");
        	PesanFormalHelper.tampilkanGagalException("pemrosesan Loading Report Util", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
        		new String[] {
        			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
        			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
        			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
        		});
        }
    }

    /**
     * Menandai {@code label} progres laporan sebagai selesai: menyetel atribut
     * {@link #STATUS_SELESAI}, mengosongkan nilai teksnya, dan
     * menyembunyikannya, lalu memanggil {@link #clearBusy()} untuk menghapus
     * overlay busy ZK.
     * <p>
     * Seluruh operasi terhadap {@code label} dibungkus try/catch; error dicatat
     * lewat {@link ais.common.ErrorAuditUtil#record} dan ditampilkan sebagai
     * pesan gagal formal tanpa dilempar ulang, sehingga kegagalan memperbarui
     * UI tidak menghentikan alur pemanggil.
     *
     * @param label komponen ZK {@link Label} yang diperbarui; aman dipanggil
     *              dengan {@code null} (tidak ada operasi yang dilakukan pada
     *              label, tetapi {@link #clearBusy()} tetap dipanggil).
     */
    public static void selesai(final Label label) {
        try {
            if (label != null) {
                label.setAttribute(STATUS_SELESAI, Boolean.TRUE);
                label.setValue("");
                label.setVisible(false);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LoadingReportUtil.java:57");
        	PesanFormalHelper.tampilkanGagalException("pemrosesan Loading Report Util", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
        		new String[] {
        			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
        			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
        			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
        		});
        }
        clearBusy();
    }

    /**
     * Menandai {@code label} progres laporan sebagai gagal: menyetel atribut
     * {@link #STATUS_ERROR}, mengisi teks {@code label} dengan pesan
     * {@code error} (atau teks default "Proses laporan gagal." bila
     * {@code error} null atau pesannya kosong), membuat {@code label} terlihat,
     * lalu memanggil {@link #clearBusy()}.
     * <p>
     * Sama seperti {@link #selesai(Label)}, operasi terhadap {@code label}
     * dibungkus try/catch fail-silent dengan pencatatan lewat
     * {@link ais.common.ErrorAuditUtil#record} dan pesan gagal formal ke
     * pengguna.
     *
     * @param label komponen ZK {@link Label} yang diperbarui; aman dipanggil
     *              dengan {@code null} (tidak ada operasi pada label, tetapi
     *              {@link #clearBusy()} tetap dipanggil).
     * @param error exception/penyebab kegagalan proses laporan; boleh
     *              {@code null}, dalam hal ini dipakai pesan default.
     */
    public static void gagal(final Label label, final Throwable error) {
        try {
            if (label != null) {
                label.setAttribute(STATUS_ERROR, Boolean.TRUE);
                String pesan = error == null ? "Proses laporan gagal." : error.getMessage();
                label.setValue(pesan == null || pesan.trim().length() == 0 ? "Proses laporan gagal." : pesan);
                label.setVisible(true);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LoadingReportUtil.java:70");
        	PesanFormalHelper.tampilkanGagalException("pemrosesan Loading Report Util", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
        		new String[] {
        			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
        			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
        			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
        		});
        }
        clearBusy();
    }

    /**
     * Menentukan apakah {@code label} progres laporan berstatus selesai:
     * bernilai {@code true} jika {@code label} {@code null}, jika atribut
     * {@link #STATUS_SELESAI} bernilai {@link Boolean#TRUE}, atau jika teks
     * {@code label} kosong/null (setelah di-trim). Pengecekan atribut/nilai
     * dibungkus try/catch; exception apa pun (mis. komponen sudah di-detach)
     * membuat method ini mengembalikan {@code true} secara fail-safe.
     *
     * @param label komponen ZK {@link Label} yang diperiksa; boleh {@code null}.
     * @return {@code true} bila laporan dianggap selesai (termasuk saat
     *         {@code label} null atau kosong), {@code false} jika masih dalam
     *         proses.
     */
    public static boolean isSelesai(final Label label) {
        if (label == null) {
            return true;
        }
        try {
            Object selesai = label.getAttribute(STATUS_SELESAI);
            if (Boolean.TRUE.equals(selesai)) {
                return true;
            }
            String teks = label.getValue();
            return teks == null || teks.trim().length() == 0;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Menentukan apakah {@code label} progres laporan berstatus error:
     * bernilai {@code true} jika atribut {@link #STATUS_ERROR} bernilai
     * {@link Boolean#TRUE}, atau jika teks {@code label} (setelah di-trim)
     * sama dengan {@code "Error"} tanpa membedakan huruf besar/kecil.
     * Pengecekan dibungkus try/catch; exception apa pun membuat method ini
     * mengembalikan {@code false} secara fail-safe (tidak dianggap error).
     *
     * @param label komponen ZK {@link Label} yang diperiksa; bernilai
     *              {@code false} bila {@code null}.
     * @return {@code true} bila laporan dianggap gagal/error,
     *         {@code false} bila tidak (termasuk saat {@code label} null).
     */
    public static boolean isError(final Label label) {
        if (label == null) {
            return false;
        }
        try {
            Object error = label.getAttribute(STATUS_ERROR);
            if (Boolean.TRUE.equals(error)) {
                return true;
            }
            String teks = label.getValue();
            return teks != null && "Error".equalsIgnoreCase(teks.trim());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mengambil teks {@code label} secara aman.
     *
     * @param label komponen ZK {@link Label} sumber teks; boleh {@code null}.
     * @return nilai teks {@code label}, atau string kosong bila {@code label}
     *         {@code null} atau terjadi exception saat mengaksesnya.
     */
    public static String getValue(final Label label) {
        try {
            return label == null ? "" : label.getValue();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Menghentikan lalu melepas (detach) {@code timer} dari desktop ZK.
     * <p>
     * Kedua operasi ({@link Timer#stop()} dan {@link Timer#detach()}) masing-
     * masing dibungkus try/catch terpisah agar kegagalan pada satu operasi
     * (mis. timer sudah berhenti) tidak menghalangi operasi berikutnya;
     * error dicatat lewat {@link ais.common.ErrorAuditUtil#record} dan
     * ditampilkan sebagai pesan gagal formal tanpa dilempar ulang.
     *
     * @param timer komponen ZK {@link Timer} yang dihentikan dan dilepas;
     *              method langsung kembali tanpa efek bila {@code null}.
     */
    public static void stopAndDetach(final Timer timer) {
        if (timer == null) {
            return;
        }
        try {
            timer.stop();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LoadingReportUtil.java:121");
        	PesanFormalHelper.tampilkanGagalException("pemrosesan Loading Report Util", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
        		new String[] {
        			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
        			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
        			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
        		});
        }
        try {
            timer.detach();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LoadingReportUtil.java:125");
        	PesanFormalHelper.tampilkanGagalException("pemrosesan Loading Report Util", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
        		new String[] {
        			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
        			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
        			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
        		});
        }
    }
}
