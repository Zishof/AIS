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
 * Entitas Hibernate untuk tabel {@code koperasi.production_document_line}, merepresentasikan
 * satu baris rincian pada dokumen produksi ({@link ProduksiDokumen}, dirujuk lewat id polos
 * {@link #getDocumentId()}) pada modul manufaktur/produksi koperasi. Kombinasi
 * ({@code document_id}, {@code line_no}) bersifat unik, memberi urutan tetap
 * ({@link #getLineNo()}) pada baris-baris dalam satu dokumen.
 * <p>
 * Satu baris mewakili satu pergerakan barang (bahan baku yang dikonsumsi maupun hasil produksi
 * yang dihasilkan — dibedakan lewat {@link #getLineType()}) untuk satu item ({@link #getItemId()}
 * /{@link #getItemCode()}/{@link #getItemName()}) sejumlah {@link #getQty()} dengan satuan
 * {@link #getUom()}, opsional terikat pada nomor lot tertentu ({@link #getLotNo()}, untuk
 * pelacakan genealogi lot — lihat {@link ProduksiGenealogiLot}) dan biaya per unit
 * ({@link #getUnitCost()}). Flag {@link #getStockAffecting()} menandai apakah baris ini
 * benar-benar memengaruhi saldo stok gudang saat diproses, atau hanya bersifat informatif.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "production_document_line", uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "line_no" }))
public class ProduksiDokumenBaris implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id; private Long documentId; private Integer lineNo; private String lineType;
	private Long itemId; private String itemCode; private String itemName; private BigDecimal qty = BigDecimal.ZERO;
	private String uom; private String lotNo; private BigDecimal unitCost = BigDecimal.ZERO;
	private Boolean stockAffecting = Boolean.FALSE; private String notes;
	/** Primary key baris ini; digenerasi database ({@code IDENTITY}), {@code null} sebelum baris pertama kali disimpan. Setter normalnya hanya dipanggil Hibernate saat memuat baris dari DB. */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false) public Long getId(){return id;} public void setId(Long v){id=v;}
	/** Id {@link ProduksiDokumen} pemilik baris ini -- referensi id polos, tanpa relasi Hibernate terpetakan (pemanggil memuat dokumen induk sendiri via {@code session.get}). Bagian dari kunci unik {@code (document_id, line_no)}. */
	@Column(name="document_id",nullable=false) public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
	/** Nomor urut baris dalam satu dokumen (1, 2, 3, ...), memberi urutan tetap saat ditampilkan/diproses. Bagian dari kunci unik {@code (document_id, line_no)} -- dua baris pada dokumen yang sama tidak boleh berbagi nomor. */
	@Column(name="line_no",nullable=false) public Integer getLineNo(){return lineNo;} public void setLineNo(Integer v){lineNo=v;}
	/** Jenis baris (mis. konsumsi bahan baku vs hasil produksi keluar). */
	@Column(name="line_type",nullable=false,length=30) public String getLineType(){return lineType;} public void setLineType(String v){lineType=v;}
	/** Id {@code koperasi.produk} yang menjadi subjek baris ini -- opsional; baris tanpa {@code itemId} (mis. baris informatif/catatan) TIDAK boleh {@link #getStockAffecting()} bernilai {@code true} (ditolak {@code IllegalArgumentException} saat posting, lihat {@code ProduksiApiHelper.postingStok}). */
	@Column(name="item_id") public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
	/** Kode produk, disalin sebagai teks bebas pada saat baris ditulis (bukan live join ke katalog produk) -- murni tampilan/pencarian, tidak disinkronkan ulang bila kode produk aslinya berubah belakangan. */
	@Column(name="item_code",length=100) public String getItemCode(){return itemCode;} public void setItemCode(String v){itemCode=v;}
	/** Nama item/produk pada baris ini, wajib diisi (default {@code "Item produksi"} bila kosong saat disimpan lewat {@code ProduksiApiHelper.simpanBaris}) -- sama sifat salinan-beku dengan {@link #getItemCode()}. */
	@Column(name="item_name",nullable=false,length=255) public String getItemName(){return itemName;} public void setItemName(String v){itemName=v;}
	/**
	 * Kuantitas baris ini, wajib diisi, default {@link BigDecimal#ZERO}, presisi
	 * {@code precision=19, scale=4}. Untuk baris ber-{@link #getStockAffecting()} {@code true},
	 * qty harus lebih dari nol -- ditolak {@code IllegalArgumentException} saat posting bila
	 * {@code <= 0} (lihat {@code ProduksiApiHelper.postingStok}); baris informatif ({@code
	 * stockAffecting=false}) tidak diperiksa demikian.
	 */
	@Column(name="qty",nullable=false,precision=19,scale=4) public BigDecimal getQty(){return qty;} public void setQty(BigDecimal v){qty=v;}
	/** Satuan unit kuantitas baris ini (mis. kg, pcs, liter) -- teks bebas, opsional. */
	@Column(name="uom",length=30) public String getUom(){return uom;} public void setUom(String v){uom=v;}
	/** Nomor lot terkait baris ini (bahan baku yang dikonsumsi atau hasil produksi yang menghasilkan lot baru), opsional -- dipakai sebagai penanda tekstual saat menautkan baris ini ke {@link ProduksiGenealogiLot} (lewat {@link #getId()} sebagai {@code inputLineId}/{@code outputLineId}, bukan lewat kolom ini). */
	@Column(name="lot_no",length=120) public String getLotNo(){return lotNo;} public void setLotNo(String v){lotNo=v;}
	/** Biaya per unit pada baris ini, default {@link BigDecimal#ZERO}, presisi {@code precision=19, scale=4} -- murni informatif/pelaporan biaya baris; TIDAK diagregasi otomatis ke {@link ProduksiDokumen#getMaterialCost()}/{@link ProduksiDokumen#getTotalCost()} dokumen induk (kolom biaya dokumen diisi manual oleh pemanggil, lihat {@code ProduksiApiHelper.hitungBiaya}). */
	@Column(name="unit_cost",precision=19,scale=4) public BigDecimal getUnitCost(){return unitCost;} public void setUnitCost(BigDecimal v){unitCost=v;}
	/** Menandai apakah baris ini memengaruhi saldo stok gudang saat dokumen diproses, atau hanya informatif. */
	@Column(name="stock_affecting",nullable=false) public Boolean getStockAffecting(){return stockAffecting;} public void setStockAffecting(Boolean v){stockAffecting=v;}
	/** Catatan bebas untuk baris ini, opsional. */
	@Column(name="notes",columnDefinition="text") public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
