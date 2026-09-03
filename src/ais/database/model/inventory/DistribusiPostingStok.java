package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Penanda idempoten posting stok untuk satu {@code (baris dokumen distribusi, arah)} -- mencegah
 * satu baris {@link DistribusiDokumenBaris} diposting/diterapkan ke stok lebih dari sekali untuk arah
 * yang sama. Bukan bagian dari klaster header-baris-event murni ({@link DistribusiDokumen} &rarr;
 * {@link DistribusiDokumenBaris} &rarr; {@link DistribusiDokumenEvent}), melainkan tabel efek-samping
 * terpisah yang menunjuk balik ke {@link DistribusiDokumen}/{@link DistribusiDokumenBaris} lewat id
 * polos. Lihat javadoc {@link DistribusiDokumen} untuk konteks klaster secara penuh, termasuk catatan
 * status dormant kelas ini (tidak ada kode aplikasi yang benar-benar menulis baris tabel ini).
 *
 * <p><b>Penjaga idempotensi dua-arah, bukan satu-baris-dua-sisi.</b> Berbeda dari {@link
 * MutasiStokToko} yang mencatat SATU baris menunjuk kedua sisi transfer sekaligus (penjaga
 * keseimbangan struktural lewat satu kolom qty -- lihat javadoc {@link
 * MutasiStokToko#getProdukAsal()}), constraint unik {@code (document_id, line_id, direction)} pada
 * kelas ini mengisyaratkan desain DUA baris terpisah per baris dokumen -- satu untuk {@link
 * #getDirection()} keluar (sisi {@link #getSourceProductId()} berkurang) dan satu untuk arah masuk
 * (sisi {@link #getDestinationProductId()} bertambah), masing-masing jadi penanda idempoten sendiri
 * yang mencegah baris yang SAMA diposting ulang untuk arah yang SAMA. Karena kedua baris arah itu
 * independen (tidak ada constraint yang mengikat keduanya harus sama-sama ada atau sama-sama tidak
 * ada), desain ini secara struktural TIDAK memberi jaminan setara "mustahil satu sisi terposting
 * tanpa sisi lain" seperti pada {@code MutasiStokToko} -- kode pemanggil yang harus menjamin kedua
 * arah diposting bersamaan dalam satu transaksi, tapi karena tidak ada kode pemanggil aktif (lihat
 * javadoc {@link DistribusiDokumen}), ini tidak bisa diverifikasi dari jalur pemakaian nyata.</p>
 *
 * <p><b>{@code legacyMutationId} -- pointer bernama tapi tak pernah diisi.</b> Lihat javadoc {@link
 * #getLegacyMutationId()} untuk pembahasan lengkap kolom ini, satu-satunya jejak tekstual di seluruh
 * paket ini yang menghubungkan klaster distribusi dengan konsep ledger "legacy" seperti {@link
 * MutasiStokToko}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "distribution_stock_posting", uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "line_id", "direction" }))
public class DistribusiPostingStok implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Primary key baris penanda posting ini. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Id {@link DistribusiDokumen} yang baris posting ini merujuk, kolom {@code Long} polos tanpa relasi/FK. */
	private Long documentId;
	/** Id {@link DistribusiDokumenBaris} yang diposting, kolom {@code Long} polos tanpa relasi/FK. Bersama {@link #documentId} dan {@link #direction} membentuk kunci unik idempotensi. */
	private Long lineId;
	/** Arah posting ("keluar"/"masuk", nilai persis tidak didefinisikan sbg enum), wajib diisi, hingga 10 karakter -- lihat javadoc kelas untuk desain dua-baris-per-arah. */
	private String direction;
	/** Id pointer ke baris ledger "legacy" (kemungkinan besar {@link MutasiStokToko#getId()}), wajib diisi meski TIDAK ada relasi/FK dan TIDAK ada kode yang mengisinya -- lihat javadoc {@link #getLegacyMutationId()}. */
	private Long legacyMutationId;
	/** Id toko asal untuk baris posting ini, wajib diisi, kolom {@code Long} polos tanpa relasi/FK. */
	private Long sourceTokoId;
	/** Id toko tujuan untuk baris posting ini, wajib diisi, kolom {@code Long} polos tanpa relasi/FK. */
	private Long destinationTokoId;
	/** Id baris {@link Produk} sisi asal, wajib diisi, kolom {@code Long} polos tanpa relasi/FK. */
	private Long sourceProductId;
	/** Id baris {@link Produk} sisi tujuan, wajib diisi, kolom {@code Long} polos tanpa relasi/FK. */
	private Long destinationProductId;
	/** Kuantitas yang diposting pada baris ini, default {@code BigDecimal.ZERO}. */
	private BigDecimal qty = BigDecimal.ZERO;
	/** Userid/nama yang memicu posting ini, opsional, tidak ber-FK. */
	private String createdBy;
	/** Waktu baris penanda posting ini dibuat, diinisialisasi ke waktu konstruksi objek Java. */
	private Date createdAt = new Date();

	/**
	 * Primary key baris penanda posting ini. Digenerasi database via strategi {@code IDENTITY};
	 * {@code null} pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long value) { id = value; }

	/**
	 * Id {@link DistribusiDokumen} yang baris posting ini terapkan, wajib diisi. Kolom {@code Long}
	 * polos tanpa relasi {@code @ManyToOne}.
	 * @return id dokumen terkait.
	 */
	@Column(name = "document_id", nullable = false) public Long getDocumentId() { return documentId; } public void setDocumentId(Long value) { documentId = value; }

	/**
	 * Id {@link DistribusiDokumenBaris} yang diposting oleh baris ini, wajib diisi. Bersama {@link
	 * #getDocumentId()} dan {@link #getDirection()} membentuk kunci unik {@code (document_id,
	 * line_id, direction)} yang menjadi mekanisme anti-posting-ganda: mencoba memposting baris/arah
	 * yang sama dua kali akan gagal pada constraint unik database.
	 * @return id baris dokumen yang diposting.
	 */
	@Column(name = "line_id", nullable = false) public Long getLineId() { return lineId; } public void setLineId(Long value) { lineId = value; }

	/**
	 * Arah posting ini, wajib diisi, hingga 10 karakter (nilai persis, mis. "OUT"/"IN", tidak
	 * didefinisikan sbg enum di model). Lihat javadoc kelas untuk pembahasan desain dua-baris-per-arah
	 * dan bedanya dari penjaga satu-baris-dua-sisi {@link MutasiStokToko}.
	 * @return arah posting baris ini.
	 */
	@Column(name = "direction", nullable = false, length = 10) public String getDirection() { return direction; } public void setDirection(String value) { direction = value; }

	/**
	 * Id yang namanya mengisyaratkan pointer ke baris ledger "legacy" -- kandidat paling masuk akal
	 * berdasarkan penamaan dan kesamaan konsep adalah {@link MutasiStokToko#getId()} (satu-satunya
	 * ledger transfer sebaris-dua-sisi yang sebanding di paket ini), TAPI kolom ini adalah {@code Long}
	 * POLOS tanpa {@code @ManyToOne}/{@code @JoinColumn} apa pun -- tidak ada relasi entity, tidak ada
	 * FK database, dan HUBUNGAN INI TIDAK PERNAH DIVERIFIKASI dari kode nyata karena penelusuran
	 * menyeluruh atas WC ini tidak menemukan satu pun jalur kode yang membuat baris {@code
	 * DistribusiPostingStok} (lihat catatan status dormant di javadoc {@link DistribusiDokumen}).
	 * Kolom ini bersifat {@code nullable = false} -- pada tabel yang benar-benar dipakai, ini akan
	 * memaksa setiap baris posting distribusi membawa pointer ke ledger legacy, mengisyaratkan desain
	 * yang MEMBUTUHKAN sinkronisasi dengan mekanisme lama (kemungkinan {@code MutasiStokToko}) sebagai
	 * bagian dari alur posting -- tapi karena tidak ada kode yang benar-benar mengisinya, ini tetap
	 * spekulasi arsitektural dari penamaan dan constraint, bukan relasi yang bisa diverifikasi dari
	 * jalur pemakaian.
	 * @return id pointer ke ledger legacy (makna dan pengisiannya tidak dapat diverifikasi dari kode nyata).
	 */
	@Column(name = "legacy_mutation_id", nullable = false) public Long getLegacyMutationId() { return legacyMutationId; } public void setLegacyMutationId(Long value) { legacyMutationId = value; }

	/** @return id toko asal untuk baris posting ini, wajib diisi. */
	@Column(name = "source_toko_id", nullable = false) public Long getSourceTokoId() { return sourceTokoId; } public void setSourceTokoId(Long value) { sourceTokoId = value; }

	/** @return id toko tujuan untuk baris posting ini, wajib diisi. */
	@Column(name = "destination_toko_id", nullable = false) public Long getDestinationTokoId() { return destinationTokoId; } public void setDestinationTokoId(Long value) { destinationTokoId = value; }

	/** @return id baris {@link Produk} sisi asal, wajib diisi. */
	@Column(name = "source_product_id", nullable = false) public Long getSourceProductId() { return sourceProductId; } public void setSourceProductId(Long value) { sourceProductId = value; }

	/** @return id baris {@link Produk} sisi tujuan, wajib diisi. */
	@Column(name = "destination_product_id", nullable = false) public Long getDestinationProductId() { return destinationProductId; } public void setDestinationProductId(Long value) { destinationProductId = value; }

	/**
	 * Kuantitas yang diposting pada baris ini, wajib diisi, default {@code BigDecimal.ZERO} (nilai
	 * default hanya berlaku dari inisialisasi field Java saat objek baru dibuat, getter tidak
	 * menormalisasi {@code null} secara eksplisit di kode -- sama seperti catatan pada {@link
	 * DistribusiDokumenBaris#getQty()}).
	 * @return kuantitas yang diposting.
	 */
	@Column(name = "qty", nullable = false, precision = 24, scale = 6) public BigDecimal getQty() { return qty; } public void setQty(BigDecimal value) { qty = value; }

	/** @return userid/nama yang memicu posting ini, atau {@code null} bila tidak diisi. */
	@Column(name = "created_by", length = 100) public String getCreatedBy() { return createdBy; } public void setCreatedBy(String value) { createdBy = value; }

	/**
	 * Waktu baris penanda posting ini dibuat, wajib diisi. Diinisialisasi ke waktu konstruksi objek
	 * Java (bukan waktu commit transaksi).
	 * @return waktu baris ini dibuat.
	 */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; } public void setCreatedAt(Date value) { createdAt = value; }
}
