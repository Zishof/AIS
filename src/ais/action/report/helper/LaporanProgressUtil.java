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

    private LaporanProgressUtil() {
    }

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
