package ais.common.inventory.accountspayable;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Kontrak (port) untuk menjurnal peristiwa akuntansi yang berasal dari modul <b>accounts
 * payable</b> (utang usaha) di {@code ais.common.inventory.accountspayable} ke buku besar/jurnal
 * akunting AIS. Pola ini mengikuti gaya "port" ala arsitektur heksagonal: modul accounts payable
 * tidak bergantung langsung pada implementasi konkret penjurnalan (mis. tabel jurnal Hibernate,
 * layanan akunting tertentu), melainkan hanya pada antarmuka ini, sehingga implementasi
 * penjurnalan dapat diganti atau diuji secara terisolasi (mis. dengan implementasi tiruan/mock
 * pada pengujian unit) tanpa menyentuh logika bisnis accounts payable.
 *
 * <p>
 * Kedua method di antarmuka ini dirancang berpasangan untuk mendukung <b>idempotensi</b> —
 * properti krusial pada operasi keuangan yang berpotensi dipicu ulang (retry jaringan, klik ganda
 * pengguna, pemrosesan ulang antrean pesan): {@link #alreadyPosted(String, String, String)}
 * dipanggil lebih dahulu oleh pemanggil untuk memeriksa apakah suatu peristiwa sumber tertentu
 * SUDAH pernah dijurnal, sebelum {@link #post(long, String, String, String, BigDecimal, Date,
 * String)} benar-benar dieksekusi — mencegah baris jurnal dobel untuk transaksi keuangan yang
 * sama. Identitas satu peristiwa ditentukan oleh kombinasi {@code sourceType} (jenis dokumen
 * sumber, mis. "INVOICE"/"PAYMENT"), {@code sourceId} (id dokumen sumber), dan {@code eventType}
 * (jenis peristiwa pada dokumen tersebut, mis. "CREATED"/"PAID"/"REVERSED").
 * </p>
 *
 * <p>
 * Karena kelas ini murni antarmuka tanpa implementasi maupun state, tidak ada logika penjagaan
 * konkurensi atau transaksi database yang dapat didokumentasikan di sini — kontrak thread-safety
 * dan atomisitas (apakah pengecekan {@link #alreadyPosted} dan {@link #post} berjalan atomik
 * terhadap race condition antar-thread/antar-proses) sepenuhnya menjadi tanggung jawab kelas
 * implementasi konkret yang mewujudkan antarmuka ini.
 * </p>
 */
public interface JournalPostingPort {
	/**
	 * Memeriksa apakah peristiwa sumber dengan kombinasi {@code sourceType}+{@code sourceId}+
	 * {@code eventType} yang diberikan SUDAH pernah dijurnal sebelumnya. Dipanggil sebagai
	 * pengaman idempotensi sebelum {@link #post} untuk mencegah penjurnalan dobel atas transaksi
	 * keuangan yang sama.
	 *
	 * @param sourceType jenis dokumen sumber (mis. "INVOICE", "PAYMENT")
	 * @param sourceId   id dokumen sumber
	 * @param eventType  jenis peristiwa pada dokumen sumber (mis. "CREATED", "PAID")
	 * @return {@code true} bila peristiwa ini sudah pernah dijurnal sebelumnya
	 */
	boolean alreadyPosted(String sourceType, String sourceId, String eventType);

	/**
	 * Menjurnal satu peristiwa akuntansi ke buku besar untuk tenant {@code tenantId}.
	 * Pemanggil bertanggung jawab memastikan peristiwa ini belum pernah dijurnal (lewat
	 * {@link #alreadyPosted}) sebelum memanggil method ini.
	 *
	 * @param tenantId       identitas tenant/instalasi pemilik transaksi
	 * @param sourceType     jenis dokumen sumber (mis. "INVOICE", "PAYMENT")
	 * @param sourceId       id dokumen sumber
	 * @param eventType      jenis peristiwa pada dokumen sumber
	 * @param amount         nominal transaksi yang dijurnal
	 * @param postingDate    tanggal efektif pembukuan jurnal
	 * @param idempotencyKey kunci idempotensi tambahan (mis. dipakai implementasi untuk
	 *                       constraint unik di database) guna memperkuat jaminan anti-dobel di
	 *                       luar pengecekan {@link #alreadyPosted}
	 */
	void post(long tenantId, String sourceType, String sourceId, String eventType,
			BigDecimal amount, Date postingDate, String idempotencyKey);
}
