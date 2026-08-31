package ais.common.inventory.accountspayable;

import java.util.Date;

/**
 * Kontrak (port) untuk memeriksa apakah suatu periode akuntansi sudah "dikunci" (ditutup) bagi
 * tenant tertentu pada modul hutang usaha (accounts payable) di dalam domain inventori AIS.
 *
 * <p>
 * Sesuai pola arsitektur port/adapter (hexagonal architecture), antarmuka ini berperan sebagai
 * batas abstraksi antara logika domain/servis yang memvalidasi apakah sebuah transaksi
 * (mis. penerimaan barang, pencatatan tagihan pemasok, jurnal penyesuaian) boleh diposting pada
 * tanggal tertentu, dengan mekanisme konkret penyimpanan status kunci periode (mis. tabel
 * konfigurasi periode akuntansi di database, layanan penutupan buku eksternal, atau cache).
 * Dengan mengandalkan antarmuka ini alih-alih implementasi konkret, kode pemanggil (mis. servis
 * validasi posting) tidak perlu tahu DARI MANA status kunci periode diperoleh — implementasi
 * sesungguhnya (adapter) disuntikkan terpisah, sehingga aturan "periode terkunci = transaksi baru
 * ditolak" dapat diuji dan diganti sumber datanya tanpa mengubah kode domain.
 * </p>
 *
 * <p>
 * Setiap tenant (unit/instalasi/perusahaan yang dilayani dalam satu database multi-tenant AIS)
 * dapat memiliki periode terkunci yang berbeda-beda, sehingga {@code tenantId} selalu menjadi
 * bagian dari kunci pemeriksaan bersama tanggal posting yang diuji.
 * </p>
 */
public interface PeriodLockPort {
	/**
	 * Memeriksa apakah tanggal posting yang diberikan jatuh pada periode akuntansi yang sudah
	 * dikunci (ditutup) untuk tenant tertentu, sehingga transaksi baru pada tanggal tersebut
	 * seharusnya ditolak oleh pemanggil.
	 *
	 * @param tenantId    id tenant/unit yang periodenya diperiksa
	 * @param postingDate tanggal posting transaksi yang hendak divalidasi
	 * @return {@code true} bila periode yang mencakup {@code postingDate} sudah terkunci untuk
	 *         {@code tenantId} tersebut (transaksi baru semestinya ditolak); {@code false} bila
	 *         periode masih terbuka
	 */
	boolean isLocked(long tenantId, Date postingDate);
}
