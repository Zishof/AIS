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
 * Dipakai lintas layar/laporan presensi karyawan harian (mis. {@code KehadiranPegawaiAction},
 * {@code ProsesAbsensiPegawai}, serta berbagai {@code LaporanAbsensiPegawai*}/{@code LaporanRekapitulasiAbsen}/
 * {@code LaporanCutiPegawai}/{@code LaporanLembur} di paket {@code ais.action.report.format1.payroll} dan
 * {@code ais.action.report.format1.akademik}) yang membaca data dari model kehadiran seperti
 * {@code StatuskehadiranKaryawanHarian} dan {@code Statusabsensi}; kelas ini sendiri tidak mengakses
 * model tersebut secara langsung, murni menyediakan helper tanggal/angka/session/UI generik.
 */
public final class KehadiranPresensiUtil {

    /**
     * Ukuran batch default untuk proses paralel (mis. pemrosesan absensi per kelompok pegawai)
     * agar pola ukuran batch konsisten di berbagai proses yang memakai utilitas ini.
     */
    public static final int DEFAULT_PARALLEL_BATCH_SIZE = 20;

    /** Label checkbox opsi "abaikan kehadiran jika hari tidak terpilih" pada form proses/laporan. */
    public static final String LABEL_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH = "Abaikan kehadiran jika hari tidak terpilih";
    /**
     * Teks keterangan (tooltip) lengkap untuk {@link #LABEL_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH}:
     * menjelaskan bahwa saat diaktifkan, sistem hanya memproses tanggal yang harinya dicentang pada
     * daftar Hari Aktif; kehadiran pada hari yang tidak dicentang tetap tersimpan di data asli tetapi
     * tidak ikut dihitung/ditampilkan pada hasil proses/laporan ini. Saat tidak diaktifkan, hari yang
     * tidak dicentang tetap dapat ikut terbaca bila pada tanggal tersebut ada data kehadiran masuk
     * (perilaku default/lama, demi kompatibilitas mundur).
     */
    public static final String KETERANGAN_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH = "Jika pilihan ini diaktifkan, sistem hanya akan memproses tanggal yang harinya dicentang pada daftar Hari Aktif. Kehadiran yang sudah tercatat pada hari yang tidak dicentang tetap disimpan di data asli, tetapi tidak ikut dihitung dan tidak ditampilkan pada hasil proses/laporan ini. Jika pilihan ini tidak diaktifkan, hari yang tidak dicentang tetap dapat ikut terbaca apabila pada tanggal tersebut terdapat data kehadiran masuk, sehingga perilaku laporan tetap sama seperti sebelumnya.";

    /**
     * Konstruktor privat: kelas ini hanya berisi konstanta dan method statis, tidak boleh
     * diinstansiasi.
     */
    private KehadiranPresensiUtil() {
    }

    /**
     * Melakukan rollback transaksi Hibernate secara aman: hanya dijalankan bila {@code tx} tidak
     * {@code null} dan masih aktif, dan kegagalan rollback ditangkap serta dicatat lewat
     * {@link ais.common.ErrorAuditUtil#record} alih-alih dilempar ke pemanggil (berguna di blok
     * {@code finally}/penanganan error tempat exception baru tidak diinginkan).
     *
     * @param tx transaksi yang akan di-rollback; boleh {@code null}.
     */
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

    /**
     * Menutup sesi Hibernate "native" (bersih + putus koneksi sebelum ditutup). Saat ini
     * berperilaku identik dengan {@link #closeOpenSession(Session)}; dipertahankan sebagai method
     * terpisah agar niat pemanggil (menutup sesi native vs. sesi terbuka biasa) tetap terbaca di
     * lokasi pemanggilan meski implementasinya sama.
     *
     * @param session sesi Hibernate yang akan ditutup; boleh {@code null}.
     */
    public static void closeNativeSession(Session session) {
        closeSession(session, true, true);
    }

    /**
     * Menutup sesi Hibernate yang sedang terbuka (bersih + putus koneksi sebelum ditutup). Saat ini
     * berperilaku identik dengan {@link #closeNativeSession(Session)}; lihat catatan pada method
     * tersebut.
     *
     * @param session sesi Hibernate yang akan ditutup; boleh {@code null}.
     */
    public static void closeOpenSession(Session session) {
        closeSession(session, true, true);
    }

    /**
     * Menutup sesi Hibernate dengan urutan aman: berhenti lebih awal bila {@code session}
     * {@code null} atau sudah tidak terbuka; bila {@code clear} true, membersihkan cache first-level
     * sesi ({@link Session#clear()}); bila {@code disconnect} true dan sesi masih terhubung, memutus
     * koneksi JDBC ({@link Session#disconnect()}) sebelum akhirnya menutup sesi
     * ({@link Session#close()}). Setiap langkah dibungkus try/catch independen dan kegagalannya
     * dicatat lewat {@link ais.common.ErrorAuditUtil#record} agar satu langkah yang gagal tidak
     * menghalangi langkah penutupan berikutnya.
     *
     * @param session sesi Hibernate yang akan ditutup; boleh {@code null}.
     * @param clear {@code true} untuk membersihkan cache first-level sebelum menutup.
     * @param disconnect {@code true} untuk memutus koneksi JDBC sebelum menutup.
     */
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

    /**
     * Menormalkan sepasang tanggal mulai/sampai menjadi rentang satu hari penuh yang valid:
     * bila keduanya {@code null}, dipakai hari ini ({@link WaktuUtil#getDate()}) untuk keduanya;
     * bila hanya salah satu {@code null}, disamakan dengan yang lain; bila {@code mulai} jatuh
     * setelah {@code sampai}, keduanya ditukar. Hasil akhir selalu berupa {@code [awal hari mulai,
     * akhir hari sampai]} (lihat {@link #awalHari(Date)}/{@link #akhirHari(Date)}), sehingga rentang
     * mencakup keseluruhan hari pertama sampai keseluruhan hari terakhir.
     *
     * @param mulai tanggal mulai; boleh {@code null}.
     * @param sampai tanggal sampai; boleh {@code null}.
     * @return array dua elemen {@code [awalHari, akhirHari]}, tidak pernah {@code null} dan
     *         elemennya tidak pernah {@code null}.
     */
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

    /**
     * Mengembalikan awal hari (00:00:00.000) dari sebuah tanggal, memakai zona waktu/kalender
     * {@link WaktuUtil#getCalendar()}.
     *
     * @param date tanggal sumber; {@code null} berarti hari ini ({@link WaktuUtil#getDate()}).
     * @return tanggal pada jam 00:00:00.000 di hari yang sama.
     */
    public static Date awalHari(Date date) {
        Calendar calendar = WaktuUtil.getCalendar();
        calendar.setTime(date == null ? WaktuUtil.getDate() : date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * Mengembalikan akhir hari (23:59:59.999) dari sebuah tanggal, memakai zona waktu/kalender
     * {@link WaktuUtil#getCalendar()}.
     *
     * @param date tanggal sumber; {@code null} berarti hari ini ({@link WaktuUtil#getDate()}).
     * @return tanggal pada jam 23:59:59.999 di hari yang sama.
     */
    public static Date akhirHari(Date date) {
        Calendar calendar = WaktuUtil.getCalendar();
        calendar.setTime(date == null ? WaktuUtil.getDate() : date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }


    /**
     * Membentuk checkbox opsi {@link #LABEL_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH} (tidak dicentang
     * secara default) lengkap dengan tooltip keterangannya. Bila {@code rows} tidak {@code null},
     * checkbox dan baris keterangan (teks kecil penjelasan lengkap) langsung ditambahkan sebagai
     * baris-baris {@link MyFormRow} baru ke {@code rows}; bila {@code null}, checkbox tetap dibentuk
     * dan dikembalikan tanpa ditambahkan ke UI mana pun (pemanggil bebas menaruhnya sendiri).
     *
     * @param rows kontainer baris form tujuan; boleh {@code null}.
     * @return checkbox yang dibentuk, tidak pernah {@code null}.
     */
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

    /**
     * Membaca status centang sebuah checkbox secara aman terhadap {@code null}.
     *
     * @param checkbox checkbox yang diperiksa; boleh {@code null}.
     * @return {@code true} hanya bila {@code checkbox} tidak {@code null} dan sedang dicentang.
     */
    public static boolean isChecked(Checkbox checkbox) {
        return checkbox != null && checkbox.isChecked();
    }

    /**
     * Memeriksa apakah sebuah hari (bernomor 1-7, sesuai urutan larik {@code haris}) sedang
     * dicentang pada daftar Hari Aktif.
     *
     * @param haris larik checkbox hari aktif, terindeks 0 untuk hari ke-1, dst.; boleh {@code null}.
     * @param hari nomor hari (1-based) yang diperiksa.
     * @return {@code true} bila {@code hari} berada dalam rentang valid larik dan checkbox pada
     *         indeks tersebut ada serta dicentang; {@code false} bila {@code haris} {@code null},
     *         indeks di luar jangkauan, atau checkbox tidak dicentang/tidak ada.
     */
    public static boolean isHariDipilih(Checkbox[] haris, int hari) {
        int indexHari = hari - 1;
        return indexHari >= 0 && haris != null && indexHari < haris.length
                && haris[indexHari] != null && haris[indexHari].isChecked();
    }

    /**
     * Overload yang menerima checkbox opsi "abaikan" secara langsung; lihat
     * {@link #harusLewatiTanggalKarenaHariTidakDipilih(Checkbox[], int, boolean, boolean)} untuk
     * aturan lengkapnya. Status centang checkbox dibaca lewat {@link #isChecked(Checkbox)} sehingga
     * {@code null} aman diperlakukan sebagai tidak dicentang.
     *
     * @param haris larik checkbox hari aktif.
     * @param hari nomor hari (1-based) yang diperiksa.
     * @param adaHadir {@code true} bila pada tanggal ini terdapat data kehadiran masuk.
     * @param abaikanKehadiranJikaHariTidakTerpilih checkbox opsi "abaikan"; boleh {@code null}.
     * @return {@code true} bila tanggal ini harus dilewati dari proses/laporan.
     */
    public static boolean harusLewatiTanggalKarenaHariTidakDipilih(Checkbox[] haris, int hari, boolean adaHadir,
            Checkbox abaikanKehadiranJikaHariTidakTerpilih) {
        return harusLewatiTanggalKarenaHariTidakDipilih(haris, hari, adaHadir,
                isChecked(abaikanKehadiranJikaHariTidakTerpilih));
    }

    /**
     * Menentukan apakah sebuah tanggal harus dilewati dari proses/laporan kehadiran karena harinya
     * tidak dicentang pada daftar Hari Aktif. Bila hari tersebut dicentang ({@link #isHariDipilih}),
     * tanggal tidak pernah dilewati. Bila hari tidak dicentang: tanggal dilewati jika tidak ada data
     * kehadiran masuk ({@code !adaHadir}), atau jika opsi
     * {@link #KETERANGAN_ABAIKAN_KEHADIRAN_HARI_TIDAK_TERPILIH} diaktifkan (memaksa lewati meski ada
     * data kehadiran masuk pada hari yang tidak dicentang) &mdash; sebaliknya (ada data kehadiran dan
     * opsi abaikan tidak aktif) tanggal tetap diproses demi kompatibilitas mundur.
     *
     * @param haris larik checkbox hari aktif.
     * @param hari nomor hari (1-based) yang diperiksa.
     * @param adaHadir {@code true} bila pada tanggal ini terdapat data kehadiran masuk.
     * @param abaikanKehadiranJikaHariTidakTerpilih {@code true} bila opsi "abaikan kehadiran jika
     *        hari tidak terpilih" sedang aktif.
     * @return {@code true} bila tanggal ini harus dilewati dari proses/laporan.
     */
    public static boolean harusLewatiTanggalKarenaHariTidakDipilih(Checkbox[] haris, int hari, boolean adaHadir,
            boolean abaikanKehadiranJikaHariTidakTerpilih) {
        if (isHariDipilih(haris, hari)) {
            return false;
        }
        return !adaHadir || abaikanKehadiranJikaHariTidakTerpilih;
    }

    /**
     * Memangkas spasi di awal/akhir string, memperlakukan {@code null} sebagai string kosong.
     *
     * @param value nilai mentah; boleh {@code null}.
     * @return {@code value.trim()}, atau {@code ""} bila {@code value} {@code null}.
     */
    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Mengonversi sebuah nilai ke {@code int} secara toleran: nilai {@link Number} dikonversi
     * langsung, nilai lain dicoba diparse dari {@link Object#toString()}; kegagalan apa pun
     * (termasuk {@code value == null}) menghasilkan {@code defaultValue}, tidak pernah melempar
     * exception ke pemanggil.
     *
     * @param value nilai mentah; boleh {@code null} atau tipe apa pun.
     * @param defaultValue nilai yang dikembalikan bila {@code value} {@code null} atau tidak dapat
     *        dikonversi.
     * @return hasil konversi, atau {@code defaultValue}.
     */
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

    /**
     * Mengonversi sebuah nilai ke {@code long} secara toleran; lihat {@link #toInt(Object, int)}
     * untuk aturan konversi dan penanganan kegagalan yang sama.
     *
     * @param value nilai mentah; boleh {@code null} atau tipe apa pun.
     * @param defaultValue nilai yang dikembalikan bila {@code value} {@code null} atau tidak dapat
     *        dikonversi.
     * @return hasil konversi, atau {@code defaultValue}.
     */
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

    /**
     * Mengonversi sebuah nilai ke {@code double} secara toleran; lihat {@link #toInt(Object, int)}
     * untuk aturan konversi dan penanganan kegagalan yang sama.
     *
     * @param value nilai mentah; boleh {@code null} atau tipe apa pun.
     * @param defaultValue nilai yang dikembalikan bila {@code value} {@code null} atau tidak dapat
     *        dikonversi.
     * @return hasil konversi, atau {@code defaultValue}.
     */
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
