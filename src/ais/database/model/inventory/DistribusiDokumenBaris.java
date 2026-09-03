package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Baris item barang pada satu {@link DistribusiDokumen} -- anak dari klaster header-baris-event
 * yang dibahas lengkap di javadoc kelas induknya (lihat {@link DistribusiDokumen} untuk konteks
 * arsitektur klaster, status dormant, dan perbandingan dengan {@link MutasiStokToko}).
 *
 * <p><b>Pemetaan produk asal/tujuan berbasis id polos, bukan relasi entity.</b> {@link
 * #getSourceProductId()}/{@link #getDestinationProductId()} menyimpan id produk pada masing-masing
 * sisi transfer (mis. karena tiap toko punya baris {@link Produk} terpisah untuk "barang yang sama",
 * pola yang sama seperti dibahas di javadoc {@link MutasiStokToko#getProdukAsal()}). BERBEDA dari
 * {@code MutasiStokToko} yang memakai relasi {@code @ManyToOne} penuh ke {@link Produk} (proxy lazy,
 * bisa dinavigasi langsung), kedua field di sini hanyalah kolom {@code Long} polos tanpa anotasi
 * relasi/FK apa pun -- memuat baris produk terkait memerlukan query terpisah oleh kode pemanggil,
 * dan tidak ada jaminan referensial dari model ini bahwa id yang tersimpan benar-benar menunjuk baris
 * {@link Produk} yang valid dan masih ada. {@link #getItemId()}/{@link #getItemCode()}/{@link
 * #getItemName()} tampak sebagai identitas/label barang yang lebih generik (mungkin dimaksudkan
 * independen dari model {@link Produk} internal, mis. untuk kebutuhan integrasi luar) -- hubungan
 * persisnya dengan {@link #getSourceProductId()}/{@link #getDestinationProductId()} tidak didefinisikan
 * di model ini dan, karena tidak ada kode pemanggil aktif (lihat javadoc {@link DistribusiDokumen}),
 * tidak bisa diverifikasi dari jalur pemakaian nyata.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "distribution_document_line", uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "line_no" }))
public class DistribusiDokumenBaris implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Primary key baris ini. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Id {@link DistribusiDokumen} induk baris ini -- kolom {@code Long} polos tanpa relasi {@code @ManyToOne}, bukan navigasi objek langsung ke header. */
	private Long documentId;
	/** Nomor urut baris dalam satu dokumen, bersama {@link #documentId} membentuk kunci unik {@code (document_id, line_no)}. */
	private Integer lineNo;
	/** Id barang, teridentifikasi terpisah dari {@link #sourceProductId}/{@link #destinationProductId} -- lihat javadoc kelas untuk pembahasan hubungan yang tidak terdefinisi di model ini. */
	private Long itemId;
	/** Kode barang (mis. SKU), opsional, denormalisasi teks bebas. */
	private String itemCode;
	/** Nama barang, wajib diisi, denormalisasi teks bebas. */
	private String itemName;
	/** Kuantitas barang pada baris ini, default {@code BigDecimal.ZERO}. Satu angka berlaku untuk kedua sisi transfer, mirip pola {@link MutasiStokToko#getQty()} -- lihat javadoc {@link #getQty()}. */
	private BigDecimal qty = BigDecimal.ZERO;
	/** Satuan unit ukur barang (mis. "PCS", "KG"), opsional, teks bebas. */
	private String uom;
	/** Catatan bebas teks untuk baris ini, opsional. */
	private String notes;
	/** Id baris {@link Produk} milik toko ASAL, kolom {@code Long} polos tanpa relasi/FK -- lihat javadoc kelas untuk perbandingan dengan {@link MutasiStokToko#getProdukAsal()}. */
	private Long sourceProductId;
	/** Id baris {@link Produk} milik toko TUJUAN, kolom {@code Long} polos tanpa relasi/FK -- lihat javadoc kelas. */
	private Long destinationProductId;

	/**
	 * Primary key baris ini. Digenerasi database via strategi {@code IDENTITY}; {@code null} pada
	 * objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param value id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long value) { id = value; }

	/**
	 * Id {@link DistribusiDokumen} induk baris ini, wajib diisi. Kolom {@code Long} polos tanpa
	 * relasi {@code @ManyToOne} -- memuat header dokumen terkait memerlukan query terpisah oleh
	 * kode pemanggil (mis. {@code load(DistribusiDokumen.class, documentId)}), tidak ada navigasi
	 * objek langsung dari baris ini ke induknya.
	 * @return id dokumen induk.
	 */
	@Column(name = "document_id", nullable = false)
	public Long getDocumentId() { return documentId; } public void setDocumentId(Long value) { documentId = value; }

	/**
	 * Nomor urut baris ini dalam dokumen induknya, wajib diisi. Bersama {@link #getDocumentId()}
	 * membentuk kunci unik {@code (document_id, line_no)} -- mencegah dua baris dengan nomor urut
	 * sama pada dokumen yang sama, ditegakkan constraint database.
	 * @return nomor urut baris.
	 */
	@Column(name = "line_no", nullable = false)
	public Integer getLineNo() { return lineNo; } public void setLineNo(Integer value) { lineNo = value; }

	/** @return id barang pada baris ini, atau {@code null} bila tidak diisi. */
	@Column(name = "item_id") public Long getItemId() { return itemId; } public void setItemId(Long value) { itemId = value; }

	/** @return kode barang (mis. SKU), atau {@code null} bila tidak diisi. */
	@Column(name = "item_code", length = 100) public String getItemCode() { return itemCode; } public void setItemCode(String value) { itemCode = value; }

	/** @return nama barang pada baris ini, wajib diisi (tidak boleh {@code null} pada baris yang tersimpan). */
	@Column(name = "item_name", nullable = false, length = 255) public String getItemName() { return itemName; } public void setItemName(String value) { itemName = value; }

	/**
	 * Kuantitas barang pada baris ini, wajib diisi, default {@code BigDecimal.ZERO}. Satu angka yang
	 * sama dipahami berlaku untuk pengurangan stok sisi {@link #getSourceProductId()} dan penambahan
	 * stok sisi {@link #getDestinationProductId()} -- pola satu-angka-dua-sisi yang sama seperti
	 * {@link MutasiStokToko#getQty()}, TAPI tidak seperti kelas itu, getter di sini TIDAK menormalisasi
	 * {@code null} secara eksplisit di kode (nilai default hanya berlaku dari inisialisasi field Java
	 * saat objek baru dibuat, bukan dari logika getter) -- baris yang dimuat dari database dengan
	 * kolom {@code qty} bernilai {@code NULL} tetap bisa mengembalikan {@code null} dari getter ini,
	 * berbeda dari jaminan "tidak pernah {@code null}" pada {@code MutasiStokToko.getQty()}.
	 * @return kuantitas barang pada baris ini.
	 */
	@Column(name = "qty", nullable = false, precision = 24, scale = 6) public BigDecimal getQty() { return qty; } public void setQty(BigDecimal value) { qty = value; }

	/** @return satuan unit ukur barang, atau {@code null} bila tidak diisi. */
	@Column(name = "uom", length = 50) public String getUom() { return uom; } public void setUom(String value) { uom = value; }

	/** @return catatan bebas teks baris ini, atau {@code null} bila tidak diisi. */
	@Column(name = "notes", columnDefinition = "text") public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }

	/**
	 * Id baris {@link Produk} milik toko ASAL transfer -- sisi yang stoknya seharusnya BERKURANG,
	 * mengikuti pola dua-sisi yang sama seperti {@link MutasiStokToko#getProdukAsal()}. Kolom {@code
	 * Long} polos tanpa relasi/FK, opsional (tidak {@code nullable = false} di anotasi, berbeda dari
	 * {@code MutasiStokToko} yang mewajibkan relasinya).
	 * @return id produk sisi asal, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "source_product_id") public Long getSourceProductId() { return sourceProductId; } public void setSourceProductId(Long value) { sourceProductId = value; }

	/**
	 * Id baris {@link Produk} milik toko TUJUAN transfer -- sisi yang stoknya seharusnya BERTAMBAH.
	 * Sama seperti {@link #getSourceProductId()}, kolom {@code Long} polos tanpa relasi/FK, opsional.
	 * @return id produk sisi tujuan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "destination_product_id") public Long getDestinationProductId() { return destinationProductId; } public void setDestinationProductId(Long value) { destinationProductId = value; }
}
