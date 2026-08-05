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

    private static final String STATUS_SELESAI = "__LAPORAN_SELESAI__";
    private static final String STATUS_ERROR = "__LAPORAN_ERROR__";

    private LoadingReportUtil() {
    }

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

    public static String getValue(final Label label) {
        try {
            return label == null ? "" : label.getValue();
        } catch (Exception e) {
            return "";
        }
    }

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
