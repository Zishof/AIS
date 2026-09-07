package ais.action.report.helper;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Label;

import ais.common.Common;

/**
 * Utilitas progress ringan untuk laporan lama berbasis ZK 5.5.
 * Aman dipanggil dari background thread karena memakai Executions.activate/deactivate.
 */
public final class LaporanProgressUtil {

    /**
     * Konstruktor privat. Kelas ini hanya berisi method statis (utility class)
     * sehingga tidak dimaksudkan untuk diinstansiasi.
     */
    private LaporanProgressUtil() {
    }

    /**
     * Memperbarui teks progres {@code label} dengan persentase penyelesaian
     * proses laporan yang berjalan di {@code desktop}.
     * <p>
     * Method ini aman dipanggil dari thread background (di luar siklus
     * request ZK) karena mengaktifkan konteks eksekusi lewat
     * {@link Executions#activate(Desktop)} sebelum mengubah komponen UI, dan
     * selalu melepasnya kembali lewat {@link Executions#deactivate(Desktop)}
     * di blok {@code finally}. Tidak melakukan apa pun bila {@code desktop}
     * atau {@code label} {@code null}, atau bila {@code label} sudah bertanda
     * selesai ({@link LoadingReportUtil#isSelesai(Label)}) maupun error
     * ({@link LoadingReportUtil#isError(Label)}). Persentase dihitung sebagai
     * {@code posisi/total * 100}, dibulatkan turun ke 100% bila melampauinya,
     * dan dianggap 100% bila {@code total <= 0}. Bila {@link Executions#activate}
     * gagal (mis. desktop sudah tidak aktif karena window ditutup pengguna),
     * exception dicatat lewat {@link ais.common.ErrorAuditUtil#record} dan
     * diabaikan agar proses laporan tetap berjalan.
     *
     * @param desktop desktop ZK tempat komponen {@code label} berada; method
     *                tidak melakukan apa pun bila {@code null}.
     * @param label   komponen {@link Label} yang menampilkan teks progres;
     *                method tidak melakukan apa pun bila {@code null}.
     * @param teks    deskripsi tahap proses yang sedang berjalan; bila
     *                {@code null}, dipakai teks default "Memproses data".
     * @param posisi  posisi/langkah saat ini dalam proses (pembilang persen).
     * @param total   total langkah proses (penyebut persen); bila {@code <= 0}
     *                persentase dianggap 100%.
     */
    public static void update(final Desktop desktop, final Label label, final String teks,
            final int posisi, final int total) {
        if (desktop == null || label == null || LoadingReportUtil.isSelesai(label) || LoadingReportUtil.isError(label)) {
            return;
        }
        try {
            Executions.activate(desktop);
            try {
                double persen = total <= 0 ? 100.0 : ((posisi * 100.0) / total);
                if (persen > 100.0) {
                    persen = 100.0;
                }
                label.setVisible(true);
                label.setValue((teks == null ? "Memproses data" : teks) + " ("
                        + Common.numberFormat.get().format(persen) + "%)");
            } finally {
                Executions.deactivate(desktop);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LaporanProgressUtil.java:36");
            // Desktop dapat berubah tidak aktif saat user menutup window. Abaikan agar proses laporan tetap berjalan.
        }
    }

    /**
     * Menandai proses laporan pada {@code desktop}/{@code label} sebagai
     * selesai, dengan mengaktifkan konteks eksekusi ZK terlebih dahulu (sama
     * seperti {@link #update(Desktop, Label, String, int, int)}) lalu
     * mendelegasikan ke {@link LoadingReportUtil#selesai(Label)}.
     * <p>
     * Tidak melakukan apa pun bila {@code desktop} atau {@code label}
     * {@code null}. Bila {@link Executions#activate(Desktop)} gagal (mis.
     * desktop sudah tidak aktif), method jatuh kembali ke
     * {@link LoadingReportUtil#clearBusy()} agar overlay busy tetap terhapus.
     *
     * @param desktop desktop ZK tempat komponen {@code label} berada.
     * @param label   komponen {@link Label} progres yang ditandai selesai.
     */
    public static void selesai(final Desktop desktop, final Label label) {
        if (desktop == null || label == null) {
            return;
        }
        try {
            Executions.activate(desktop);
            try {
                LoadingReportUtil.selesai(label);
            } finally {
                Executions.deactivate(desktop);
            }
        } catch (Exception e) {
            LoadingReportUtil.clearBusy();
        }
    }

    /**
     * Menandai proses laporan pada {@code desktop}/{@code label} sebagai
     * gagal, dengan mengaktifkan konteks eksekusi ZK terlebih dahulu (sama
     * seperti {@link #update(Desktop, Label, String, int, int)}) lalu
     * mendelegasikan ke {@link LoadingReportUtil#gagal(Label, Throwable)}.
     * <p>
     * Bila {@code desktop} atau {@code label} {@code null}, atau bila
     * {@link Executions#activate(Desktop)} gagal (mis. desktop sudah tidak
     * aktif), method jatuh kembali ke memanggil
     * {@link LoadingReportUtil#clearBusy()} saja agar overlay busy tetap
     * terhapus meski status error tidak dapat ditulis ke {@code label}.
     *
     * @param desktop desktop ZK tempat komponen {@code label} berada.
     * @param label   komponen {@link Label} progres yang ditandai gagal.
     * @param error   exception/penyebab kegagalan proses laporan; boleh
     *                {@code null}.
     */
    public static void gagal(final Desktop desktop, final Label label, final Throwable error) {
        if (desktop == null || label == null) {
            LoadingReportUtil.clearBusy();
            return;
        }
        try {
            Executions.activate(desktop);
            try {
                LoadingReportUtil.gagal(label, error);
            } finally {
                Executions.deactivate(desktop);
            }
        } catch (Exception e) {
            LoadingReportUtil.clearBusy();
        }
    }
}
