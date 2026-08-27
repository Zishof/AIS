package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;


import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.UIUtil;
import ais.ui.util.WaktuUtil;

/**
 * Utilitas kecil untuk modul kehadiran/presensi.
 * Dibuat reusable agar pola tanggal, angka, dan penutupan session konsisten di semua laporan/proses.
 */
public final class KehadiranPresensiUtil {

    public static final int DEFAULT_PARALLEL_BATCH_SIZE = 20;

    public static final String LABEL_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH = "Abaikan kehadiran jika hari tidak terpilih";
    public static final String KETERANGAN_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH = "Jika pilihan ini diaktifkan, sistem hanya akan memproses tanggal yang harinya dicentang pada daftar Hari Aktif. Kehadiran yang sudah tercatat pada hari yang tidak dicentang tetap disimpan di data asli, tetapi tidak ikut dihitung dan tidak ditampilkan pada hasil proses/laporan ini. Jika pilihan ini tidak diaktifkan, hari yang tidak dicentang tetap dapat ikut terbaca apabila pada tanggal tersebut terdapat data kehadiran masuk, sehingga perilaku laporan tetap sama seperti sebelumnya.";

    private KehadiranPresensiUtil() {
    }

    public static void rollbackQuietly(Transaction tx) {
        if (tx != null) {
            try {
                if (tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KehadiranPresensiUtil.java:39");
            }
        }
    }

    public static void closeNativeSession(Session session) {
        closeSession(session, true, true);
    }

    public static void closeOpenSession(Session session) {
        closeSession(session, true, true);
    }

    private static void closeSession(Session session, boolean clear, boolean disconnect) {
        if (session == null) {
            return;
        }
		try {
			if (!session.isOpen()) {
				return;
			}
		} catch (Exception e) {
			return;
		}
        if (clear) {
            try {
                session.clear();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KehadiranPresensiUtil.java:59");
            }
        }
        if (disconnect) {
            try {
                if (session.isConnected()) {
                    session.disconnect();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KehadiranPresensiUtil.java:67");
            }
        }
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KehadiranPresensiUtil.java:74");
        }
    }

    public static Date[] normalisasiRentangTanggal(Date mulai, Date sampai) {
        Date now = WaktuUtil.getDate();
        if (mulai == null && sampai == null) {
            mulai = now;
            sampai = now;
        } else if (mulai == null) {
            mulai = sampai;
        } else if (sampai == null) {
            sampai = mulai;
        }

        if (mulai.after(sampai)) {
            Date tmp = mulai;
            mulai = sampai;
            sampai = tmp;
        }
        return new Date[] { awalHari(mulai), akhirHari(sampai) };
    }

    public static Date awalHari(Date date) {
        Calendar calendar = WaktuUtil.getCalendar();
        calendar.setTime(date == null ? WaktuUtil.getDate() : date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date akhirHari(Date date) {
        Calendar calendar = WaktuUtil.getCalendar();
        calendar.setTime(date == null ? WaktuUtil.getDate() : date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }


    public static MyCheckboxConfig buatCheckboxAbaikanKehadiranHariTidakTerpilih(Rows rows) {
        MyCheckboxConfig checkbox = new MyCheckboxConfig(LABEL_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH);
        checkbox.setChecked(false);
        checkbox.setTooltiptext(KETERANGAN_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH);

        if (rows != null) {
            MyFormRow row = new MyFormRow();
            row.setParent(rows);
            row.appendChild(new MyLabelConfig(""));
            row.appendChild(checkbox);

            MyFormRow rowKeterangan = new MyFormRow();
            rowKeterangan.setParent(rows);
            rowKeterangan.appendChild(new MyLabelConfig("Keterangan"));
            MyLabelConfig labelKeterangan = new MyLabelConfig(KETERANGAN_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH);
            labelKeterangan.setStyle("font-size:11px;color:#555;white-space:normal;line-height:16px;");
            rowKeterangan.appendChild(labelKeterangan);
        }
        return checkbox;
    }

    public static boolean isChecked(Checkbox checkbox) {
        return checkbox != null && checkbox.isChecked();
    }

    public static boolean isHariDipilih(Checkbox[] haris, int hari) {
        int indexHari = hari - 1;
        return indexHari >= 0 && haris != null && indexHari < haris.length
                && haris[indexHari] != null && haris[indexHari].isChecked();
    }

    public static boolean harusLewatiTanggalKarenaHariTidakDipilih(Checkbox[] haris, int hari, boolean adaHadir,
            Checkbox abaikanKehadiranJikaHariTidakTerpilih) {
        return harusLewatiTanggalKarenaHariTidakDipilih(haris, hari, adaHadir,
                isChecked(abaikanKehadiranJikaHariTidakTerpilih));
    }

    public static boolean harusLewatiTanggalKarenaHariTidakDipilih(Checkbox[] haris, int hari, boolean adaHadir,
            boolean abaikanKehadiranJikaHariTidakTerpilih) {
        if (isHariDipilih(haris, hari)) {
            return false;
        }
        return !adaHadir || abaikanKehadiranJikaHariTidakTerpilih;
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long toLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double toDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
