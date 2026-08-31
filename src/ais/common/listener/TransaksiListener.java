package ais.common.listener;

import java.util.Date;

import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;

/**
 * Antarmuka callback untuk mengabarkan perubahan status transaksi pada modul rumah sakit
 * (SIRS/{@code ais.database.model.sirs}) di aplikasi AIS, khususnya terkait transaksi
 * pendaftaran pasien yang dapat ditandai "bebas biaya" atau diubah rincian penagihannya
 * (kelas perawatan, nama penanggung jawab, tanggal transaksi, dan keterangan).
 *
 * <p>
 * Pola pemakaiannya serupa dengan {@link ais.common.OnSearchDefaultListener}: sebuah
 * komponen antarmuka (mis. layar edit transaksi/tagihan pasien pada modul SIRS) memegang
 * referensi implementasi interface ini dan memanggil salah satu method-nya ketika pengguna
 * melakukan aksi terkait di layar tersebut. Implementasi konkret biasanya berupa anonymous
 * inner class pada composer/controller ZK modul SIRS yang, setelah menerima notifikasi
 * perubahan, memperbarui data {@link Pendaftaran} terkait di database dan menyegarkan
 * tampilan.
 * </p>
 *
 * <p>
 * Kedua method dideklarasikan {@code throws Exception} secara umum karena implementasi
 * konkretnya biasanya langsung melakukan operasi database (Hibernate) atau validasi bisnis
 * yang dapat gagal, dan pemanggil (kode yang memicu event) bertanggung jawab menangani
 * exception tersebut, biasanya dengan menampilkan pesan kesalahan ke pengguna lewat dialog
 * ZK.
 * </p>
 */
public interface TransaksiListener {

	/**
	 * Dipanggil ketika status "bebas biaya" pada transaksi diubah oleh pengguna, misalnya
	 * lewat sebuah checkbox pada layar transaksi/tagihan pasien.
	 *
	 * @param checked status baru "bebas biaya" (biasanya {@code true} berarti pasien
	 *                dibebaskan dari biaya transaksi, {@code false} berarti tetap dikenakan
	 *                biaya normal); boleh {@code null} tergantung implementasi komponen sumber
	 * @throws Exception diteruskan dari kegagalan pemrosesan pada sisi implementasi (mis.
	 *                    kegagalan menyimpan perubahan ke database)
	 */
	public void onBebas(Boolean checked) throws Exception;

	/**
	 * Dipanggil ketika salah satu rincian transaksi pada pendaftaran pasien berubah, mencakup
	 * status bebas biaya, kelas perawatan, nama penanggung jawab, tanggal transaksi, dan/atau
	 * keterangan tambahan. Method ini membawa seluruh nilai terbaru sekaligus (bukan hanya
	 * nilai yang berubah), sehingga implementasi dapat langsung memakainya untuk memperbarui
	 * record terkait tanpa perlu membaca ulang nilai lama.
	 *
	 * @param bebas             status terbaru "bebas biaya" untuk transaksi ini
	 * @param pendaftaran       record {@link Pendaftaran} (pendaftaran pasien) yang menjadi
	 *                          induk transaksi yang berubah
	 * @param pasien            data {@link Pasien} terkait pendaftaran tersebut
	 * @param nama              nama penanggung jawab/pihak terkait transaksi (mis. nama pasien
	 *                          atau nama penjamin, tergantung konteks pemanggilan)
	 * @param tanggalTransaksi  tanggal transaksi terbaru
	 * @param kelasPerawatan    kelas perawatan ({@link KelasPerawatan}) terbaru yang dipilih
	 *                          untuk transaksi ini, memengaruhi tarif yang berlaku
	 * @param keterangan        catatan/keterangan tambahan terkait perubahan transaksi
	 * @throws Exception diteruskan dari kegagalan pemrosesan pada sisi implementasi (mis.
	 *                    kegagalan validasi atau kegagalan menyimpan perubahan ke database)
	 */
	public void onBerubah(Boolean bebas, Pendaftaran pendaftaran, Pasien pasien, String nama, Date tanggalTransaksi,
			KelasPerawatan kelasPerawatan, String keterangan) throws Exception;

}
