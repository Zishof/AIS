package ais.common;

import java.util.Collection;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.zkoss.zul.Rows;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasiDetail;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.ui.util.MyMessageboxConfig;

/**
 * Kelas utilitas statis untuk memproses pembayaran tunai anggota koperasi pada modul koperasi
 * AIS ({@code ais.database.model.koperasi}), mencakup dua skenario yang dapat terjadi sekaligus
 * dalam satu transaksi pembayaran: (1) pelunasan/angsuran atas satu atau lebih rincian
 * transaksi pinjaman koperasi yang sudah ada ({@link TransaksiKoperasiDetail}, dipilih lewat
 * centang pada layar antarmuka), dan (2) penambahan setoran/topup tabungan anggota
 * ({@code deposit}) yang tidak terkait transaksi pinjaman mana pun.
 *
 * <p>
 * Kelas ini hanya memiliki satu method publik, {@link #onSave(AnggotaKoperasi, Collection,
 * Double, String, CaraPembayaranKoperasi, Rows, Date)}, yang dipanggil dari layar (composer ZK)
 * kasir/loket pembayaran koperasi saat petugas menekan tombol simpan pembayaran tunai. Alur
 * kerjanya:
 * </p>
 * <ol>
 * <li>Menghitung total nilai tagihan dari {@code rowsDetailBiaya} (rincian biaya yang
 * ditampilkan di grid ZK, dijumlahkan lewat
 * {@link PembayaranAnggotaKoperasi#chekDetail(Rows)}) ditambah nilai {@code deposit} (topup
 * tabungan). Bila totalnya nol atau nyaris nol (kurang dari {@code 0.1}), penyimpanan dibatalkan
 * dan pesan kegagalan formal ditampilkan lewat {@link PesanFormalHelper#tampilkanGagal} — tidak
 * ada satu pun record yang dibuat.</li>
 * <li>Memvalidasi bahwa {@code anggotaKoperasi} (pemilik pembayaran) sudah dipilih dan memiliki
 * id; bila tidak, penyimpanan juga dibatalkan dengan pesan formal serupa.</li>
 * <li>Menyusun teks {@code keterangan} otomatis yang merangkum angsuran ke berapa saja yang
 * dibayar (dari nomor urut {@link TransaksiKoperasiDetail#getKe()}) dan/atau nilai topup
 * tabungan yang disertakan.</li>
 * <li>Dalam satu transaksi Hibernate, memuat ulang {@link AnggotaKoperasi} secara terkelola
 * (managed) dari sesi native untuk memastikan entitas tidak "basi" (mis. sudah dihapus pihak
 * lain), lalu membuat dan menyimpan record induk {@link PembayaranAnggotaKoperasi} (tanggal,
 * cara pembayaran, nominal total, nilai deposit, validator/petugas).</li>
 * <li>Untuk setiap {@link TransaksiKoperasiDetail} yang dicentang dan belum pernah dibayar
 * (dicek lewat {@code getPembayaranAnggotaKoperasiDetail() == null} pada versi terkelola dari
 * database, sebagai penjagaan terhadap pembayaran ganda), dibuat baris rincian
 * {@link PembayaranAnggotaKoperasiDetail} dengan nominal gabungan margin+pokok, lalu detail
 * transaksi ditandai sudah terbayar dengan menautkannya ke rincian pembayaran baru tersebut.</li>
 * <li>Transaksi di-commit; bila terjadi exception di titik mana pun setelah transaksi dimulai,
 * dilakukan rollback eksplisit (hanya bila transaksi masih aktif) sebelum exception dilempar
 * ulang ke pemanggil, dan sesi Hibernate selalu dibersihkan (clear/disconnect/close) di blok
 * {@code finally} apa pun hasilnya.</li>
 * </ol>
 *
 * <p>
 * Penanganan galat pada kelas ini mengikuti dua pola berbeda secara sengaja: kegagalan validasi
 * bisnis (tagihan nol, anggota belum dipilih) ditangani secara "lunak" — pesan formal ditampilkan
 * ke pengguna dan method mengembalikan {@code null} tanpa exception — sedangkan kegagalan teknis
 * (anggota tidak ditemukan di database saat transaksi berjalan, kegagalan Hibernate lainnya)
 * ditangani secara "keras" lewat exception yang dilempar ke pemanggil setelah rollback, sehingga
 * pemanggil (layar ZK) bertanggung jawab menampilkan pesan galat generik.
 * </p>
 */
public class TunaiAnggotaKoperasiCommon {

    /**
     * Memproses dan menyimpan satu transaksi pembayaran tunai anggota koperasi, mencakup
     * pelunasan/angsuran rincian pinjaman yang dipilih ({@code tag}) dan/atau penambahan
     * setoran tabungan ({@code deposit}), dalam satu transaksi database. Lihat javadoc kelas
     * untuk uraian lengkap alur kerja, aturan validasi, dan strategi penanganan galat.
     *
     * @param anggotaKoperasi        anggota koperasi pemilik pembayaran; wajib sudah memiliki
     *                               id (tersimpan di database) atau method mengembalikan
     *                               {@code null} dengan pesan kegagalan formal
     * @param tag                    kumpulan {@link TransaksiKoperasiDetail} yang dicentang
     *                               pengguna untuk dilunasi/diangsur pada pembayaran ini, boleh
     *                               {@code null}/kosong bila pembayaran hanya berupa topup
     *                               tabungan
     * @param deposit                nilai tambahan setoran/topup tabungan anggota, boleh
     *                               {@code null} (diperlakukan sebagai {@code 0.0})
     * @param validator              nama/identitas petugas yang memvalidasi pembayaran, boleh
     *                               {@code null} (disimpan sebagai string kosong)
     * @param caraPembayaranKoperasi metode pembayaran yang dipilih (mis. tunai, transfer)
     * @param rowsDetailBiaya        komponen grid ZK berisi rincian biaya yang dicentang,
     *                               dipakai untuk menghitung total tagihan lewat
     *                               {@link PembayaranAnggotaKoperasi#chekDetail(Rows)}; boleh
     *                               {@code null} bila tidak ada rincian biaya tambahan
     * @param tanggalTransaski       tanggal transaksi/pembayaran; bila {@code null}, dipakai
     *                               tanggal saat ini dari {@link ais.ui.util.WaktuUtil#getDate()}
     * @return record {@link PembayaranAnggotaKoperasi} yang berhasil disimpan, atau
     *         {@code null} bila validasi awal gagal (total tagihan nol atau anggota belum
     *         dipilih) dan pesan kegagalan formal sudah ditampilkan ke pengguna
     * @throws Exception dilempar ulang setelah rollback bila terjadi kegagalan teknis saat
     *                    proses penyimpanan berlangsung, mis. data anggota koperasi tidak
     *                    ditemukan/sudah dihapus di database ({@link IllegalArgumentException})
     *                    atau kegagalan Hibernate lainnya
     */
    public static PembayaranAnggotaKoperasi onSave(AnggotaKoperasi anggotaKoperasi,
            Collection<TransaksiKoperasiDetail> tag, Double deposit, String validator,
            CaraPembayaranKoperasi caraPembayaranKoperasi, Rows rowsDetailBiaya, Date tanggalTransaski)
            throws Exception {

        Double nilaiTagihan = rowsDetailBiaya == null ? 0.0 : PembayaranAnggotaKoperasi.chekDetail(rowsDetailBiaya);
        double nilaiDeposit = deposit == null ? 0.0 : deposit.doubleValue();
        double totalBayar = (nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue()) + nilaiDeposit;

        if (totalBayar <= 0.1) {
            PesanFormalHelper.tampilkanGagal("pembayaran tunai anggota koperasi",
                    "Belum ada transaksi yang dipilih dan tidak ada nilai tabungan/topup yang diisi, sehingga "
                            + "total yang harus dibayarkan bernilai nol.",
                    new String[] {
                            "Silakan centang minimal satu transaksi pada daftar rincian, atau",
                            "Isi nilai tabungan/topup yang ingin dibayarkan sebelum menyimpan." });
            return null;
        }

        if (anggotaKoperasi == null || anggotaKoperasi.getId() == null) {
            PesanFormalHelper.tampilkanGagal("pembayaran tunai anggota koperasi",
                    "Data Anggota Koperasi belum dipilih, sehingga sistem tidak dapat menentukan pemilik "
                            + "transaksi pembayaran ini.",
                    new String[] {
                            "Silakan pilih terlebih dahulu Anggota Koperasi yang bersangkutan.",
                            "Ulangi proses penyimpanan pembayaran setelah data anggota terisi." });
            return null;
        }

        StringBuilder keterangan = new StringBuilder("angsuran:");
        if (tag != null) {
            for (TransaksiKoperasiDetail transaksiKoperasiDetail : tag) {
                if (transaksiKoperasiDetail != null) {
                    keterangan.append(" ke-").append(transaksiKoperasiDetail.getKe()).append(",");
                }
            }
        }
        if (nilaiDeposit > 0.1) {
            keterangan.append(" topup tabungan ").append(Common.numberFormat.get().format(nilaiDeposit)).append(",");
        }

        Session session = null;
        Transaction tx = null;
        PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            AnggotaKoperasi anggotaManaged = (AnggotaKoperasi) session.get(AnggotaKoperasi.class,
                    anggotaKoperasi.getId());
            if (anggotaManaged == null) {
                throw new IllegalArgumentException("Data anggota koperasi tidak ditemukan atau sudah dihapus.");
            }

            pembayaranAnggotaKoperasi = new PembayaranAnggotaKoperasi();
            pembayaranAnggotaKoperasi.setAnggotaKoperasi(anggotaManaged);
            pembayaranAnggotaKoperasi.setKoperasi(anggotaManaged.getKoperasi());
            pembayaranAnggotaKoperasi.setTanggal(tanggalTransaski == null ? ais.ui.util.WaktuUtil.getDate()
                    : tanggalTransaski);
            pembayaranAnggotaKoperasi.setTanggalBayar(tanggalTransaski == null ? ais.ui.util.WaktuUtil.getDate()
                    : tanggalTransaski);
            pembayaranAnggotaKoperasi.setKeterangan(keterangan.toString());
            pembayaranAnggotaKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasi);
            pembayaranAnggotaKoperasi.setNominal(totalBayar);
            pembayaranAnggotaKoperasi.setTambahanDeposit(nilaiDeposit);
            pembayaranAnggotaKoperasi.setValidator(validator == null ? "" : validator);
            session.save(pembayaranAnggotaKoperasi);

            if (tag != null) {
                for (TransaksiKoperasiDetail selectedDetail : tag) {
                    if (selectedDetail == null || selectedDetail.getId() == null) {
                        continue;
                    }
                    TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) session.get(
                            TransaksiKoperasiDetail.class, selectedDetail.getId());
                    if (detail == null || detail.getPembayaranAnggotaKoperasiDetail() != null) {
                        continue;
                    }
                    Double nominal = new Double(safeDouble(detail.getMargin()) + safeDouble(detail.getPokok()));
                    PembayaranAnggotaKoperasiDetail pembayaranDetail = new PembayaranAnggotaKoperasiDetail();
                    pembayaranDetail.setNominal(nominal);
                    pembayaranDetail.setNominalManual(nominal);
                    pembayaranDetail.setPembayaranAnggotaKoperasi(pembayaranAnggotaKoperasi);
                    pembayaranDetail.setTransaksiKoperasiDetail(detail);
                    session.save(pembayaranDetail);
                    detail.setPembayaranAnggotaKoperasiDetail(pembayaranDetail);
                    session.update(detail);
                }
            }

            tx.commit();
            return pembayaranAnggotaKoperasi;
        } catch (Exception e) {
            try {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:115");
            }
            throw e;
        } finally {
            if (session != null) {
                try {
                    session.clear();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:122");
                }
                try {
                    session.disconnect();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:126");
                }
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:132");
                }
            }
            try {
                HibernateUtil.closeSession();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:137");
            }
        }
    }

    /** Mengembalikan nilai {@code double} dari {@link Double}, atau {@code 0.0} bila {@code null}. */
    private static double safeDouble(Double value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
