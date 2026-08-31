package ais.common.inventory.accountspayable;

/**
 * Objek nilai (value object) yang tidak dapat diubah ({@code final}, seluruh bidang {@code final})
 * untuk membawa hasil satu operasi pada modul <b>accounts payable</b> (utang usaha) kembali ke
 * pemanggil, tanpa bergantung pada exception untuk kasus "sudah pernah diproses" — pola umum pada
 * operasi keuangan yang harus idempoten (aman dijalankan ulang tanpa efek samping ganda, mis. saat
 * permintaan yang sama datang dua kali akibat retry jaringan atau klik ganda pengguna).
 *
 * <p>
 * Tiga informasi yang dibawa objek ini saling melengkapi untuk membedakan tiga kondisi hasil
 * operasi: (1) berhasil normal ({@code successful=true, idempotentReplay=false}), (2) berhasil
 * karena permintaan yang identik memang sudah pernah diproses sebelumnya dan hasil sebelumnya
 * dikembalikan lagi tanpa mengulang efek samping ({@code successful=true, idempotentReplay=true}
 * — lihat juga {@link JournalPostingPort} untuk mekanisme pengecekan idempotensi terkait), dan
 * (3) gagal ({@code successful=false}, dengan {@code message} berisi alasan kegagalan untuk
 * ditampilkan/dicatat).
 * </p>
 *
 * <p>
 * Sebagai objek nilai murni tanpa perilaku (hanya konstruktor dan getter), kelas ini aman dibagi
 * antar-thread setelah dibuat (immutable) dan tidak menyimpan referensi ke sumber daya eksternal
 * apa pun (koneksi database, sesi Hibernate, dsb.).
 * </p>
 */
public final class AccountsPayableOperationResult {
	/** {@code true} bila operasi dianggap berhasil (baik dieksekusi baru maupun sebagai replay idempoten). */
	private final boolean successful;
	/** {@code true} bila hasil ini adalah pengulangan (replay) dari operasi identik yang sudah pernah berhasil diproses sebelumnya, bukan eksekusi baru. */
	private final boolean idempotentReplay;
	/** Pesan penjelas hasil operasi — alasan kegagalan bila {@link #successful} {@code false}, atau keterangan tambahan lain. */
	private final String message;

	/**
	 * Membentuk hasil operasi accounts payable dengan ketiga komponen hasilnya sekaligus.
	 *
	 * @param successful       status keberhasilan operasi
	 * @param idempotentReplay apakah hasil ini adalah replay dari operasi identik sebelumnya
	 * @param message          pesan penjelas (alasan gagal atau keterangan tambahan)
	 */
	public AccountsPayableOperationResult(boolean successful, boolean idempotentReplay, String message) {
		this.successful = successful; this.idempotentReplay = idempotentReplay; this.message = message;
	}
	/** @return {@code true} bila operasi berhasil (baru dieksekusi maupun replay idempoten). */
	public boolean isSuccessful() { return successful; }
	/** @return {@code true} bila hasil ini adalah replay dari operasi identik yang sudah pernah berhasil sebelumnya. */
	public boolean isIdempotentReplay() { return idempotentReplay; }
	/** @return pesan penjelas hasil operasi (alasan gagal, atau keterangan tambahan). */
	public String getMessage() { return message; }
}
