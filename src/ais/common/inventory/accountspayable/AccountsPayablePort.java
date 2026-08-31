package ais.common.inventory.accountspayable;

/**
 * Port (antarmuka batas) persistensi untuk domain Accounts Payable (AP) — kontrak penyimpanan yang
 * dipakai oleh {@link AccountsPayableService} tanpa layanan itu perlu tahu implementasi konkret
 * (Hibernate, JDBC langsung, mock pengujian, dsb.) yang sesungguhnya menyentuh basis data. Pola ini
 * mengikuti gaya hexagonal architecture/ports-and-adapters: {@link AccountsPayableService} adalah
 * inti domain yang murni berisi aturan bisnis (validasi status invoice, toleransi 3-way match,
 * perhitungan saldo terbuka), sedangkan implementasi konkret dari port ini (adapter) yang
 * bertanggung jawab menerjemahkan operasi domain menjadi query/DML sesungguhnya. Pemisahan ini
 * memudahkan pengujian {@link AccountsPayableService} secara terisolasi (memakai implementasi palsu
 * dari port ini) tanpa memerlukan koneksi database sungguhan, sekaligus membuka kemungkinan
 * mengganti mekanisme penyimpanan di kemudian hari tanpa mengubah logika bisnis.
 *
 * <p>
 * Seluruh metode di sini dipanggil dari {@link AccountsPayableService} pada titik-titik kunci alur
 * AP: pendaftaran invoice vendor baru ({@link #vendorInvoiceExists}/{@link #saveInvoice}), hasil
 * pencocokan tiga arah PO-Penerimaan-Invoice ({@link #saveMatch}), alokasi pembayaran dan penerapan
 * credit note yang keduanya memakai kunci idempotensi untuk mencegah duplikasi transaksi finansial
 * bila permintaan yang sama terkirim ulang ({@link #idempotencyKeyExists}, {@link #savePayment},
 * {@link #saveCreditNote}), serta pembaruan status/saldo invoice di berbagai tahap siklus hidupnya
 * ({@link #updateInvoice}). Implementasi konkret bertanggung jawab penuh atas transaksionalitas
 * (commit/rollback) setiap operasi tulis; kontrak ini sendiri tidak mengasumsikan mekanisme
 * transaksi tertentu.
 * </p>
 */
public interface AccountsPayablePort {
	/** Mengecek apakah nomor invoice vendor tertentu sudah pernah terdaftar untuk kombinasi tenant dan vendor yang sama (mencegah pendaftaran invoice duplikat). */
	boolean vendorInvoiceExists(long tenantId, long vendorId, String vendorInvoiceNumber);
	/** Mengecek apakah sebuah kunci idempotensi sudah pernah dipakai, dipakai sebelum mengalokasikan pembayaran atau menerapkan credit note agar operasi yang terkirim ulang tidak diproses dua kali. */
	boolean idempotencyKeyExists(String idempotencyKey);
	/** Menyimpan invoice vendor baru ke penyimpanan persisten. */
	void saveInvoice(VendorInvoice invoice);
	/** Menyimpan hasil pencocokan tiga arah (3-way match) untuk sebuah invoice. */
	void saveMatch(String invoiceId, ThreeWayMatchResult result);
	/** Menyimpan satu alokasi pembayaran terhadap invoice. */
	void savePayment(PaymentAllocation allocation);
	/** Menyimpan credit note yang diterapkan terhadap invoice. */
	void saveCreditNote(CreditNote creditNote);
	/** Memperbarui data invoice yang sudah ada (mis. status, saldo terbuka) di penyimpanan persisten. */
	void updateInvoice(VendorInvoice invoice);
}
