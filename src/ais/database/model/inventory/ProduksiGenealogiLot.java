package ais.database.model.inventory;
import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable; import java.math.BigDecimal; import java.util.Date;
import javax.persistence.Column; import javax.persistence.Entity; import javax.persistence.GeneratedValue; import javax.persistence.Id; import javax.persistence.Table; import javax.persistence.Temporal; import javax.persistence.TemporalType; import javax.persistence.UniqueConstraint;
/**
 * Entitas Hibernate untuk tabel {@code koperasi.production_lot_genealogy}, merepresentasikan
 * satu tautan penelusuran (genealogi) lot pada modul manufaktur/produksi koperasi: berapa
 * banyak ({@link #getAllocatedQty()}) dari satu lot bahan baku masuk ({@link #getInputLotNo()},
 * merujuk baris {@link ProduksiDokumenBaris} lewat {@link #getInputLineId()}) dialokasikan
 * untuk menghasilkan satu lot hasil produksi keluar ({@link #getOutputLotNo()}, merujuk baris
 * lewat {@link #getOutputLineId()}), dalam konteks satu {@link ProduksiDokumen} yang sama
 * ({@link #getDocumentId()}).
 * <p>
 * Karena satu dokumen produksi dapat mencampur beberapa lot bahan baku menjadi beberapa lot
 * hasil (relasi banyak-ke-banyak), tabel penghubung ini diperlukan untuk menjawab pertanyaan
 * penelusuran dua arah: dari lot hasil, lot bahan baku mana saja yang menyusunnya (dan berapa
 * porsinya); serta dari lot bahan baku, ke lot hasil mana saja ia tersebar. Kombinasi
 * ({@code document_id}, {@code input_line_id}, {@code output_line_id}) bersifat unik per baris.
 */
@Entity @Table(schema="koperasi",name="production_lot_genealogy",uniqueConstraints=@UniqueConstraint(columnNames={"document_id","input_line_id","output_line_id"}))
public class ProduksiGenealogiLot implements Serializable {
	private static final long serialVersionUID=1L; private Long id; private Long documentId; private Long inputLineId; private Long outputLineId; private String inputLotNo; private String outputLotNo; private BigDecimal allocatedQty=BigDecimal.ZERO; private Date createdAt=new Date();
	/** Primary key baris tautan genealogi ini; digenerasi database ({@code IDENTITY}), {@code null} sebelum baris pertama kali disimpan. Setter normalnya hanya dipanggil Hibernate saat memuat baris dari DB. */
	@Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",unique=true,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
	/** Id {@link ProduksiDokumen} yang memuat kedua baris (input dan output) yang ditautkan oleh baris genealogi ini -- referensi id polos, tanpa relasi Hibernate terpetakan. Bagian dari kunci unik {@code (document_id, input_line_id, output_line_id)}. */
	@Column(name="document_id",nullable=false) public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
	/** Id baris {@link ProduksiDokumenBaris} bahan baku MASUK (sumber alokasi) -- referensi id polos, tanpa relasi Hibernate terpetakan; pemanggil harus memuatnya sendiri lewat {@code session.get(ProduksiDokumenBaris.class, id)} bila butuh detail baris (item, qty, lot). Bagian dari kunci unik. */
	@Column(name="input_line_id",nullable=false) public Long getInputLineId(){return inputLineId;} public void setInputLineId(Long v){inputLineId=v;}
	/** Id baris {@link ProduksiDokumenBaris} hasil produksi KELUAR (tujuan alokasi) -- referensi id polos, sama gaya dengan {@link #getInputLineId()}. Bagian dari kunci unik. */
	@Column(name="output_line_id",nullable=false) public Long getOutputLineId(){return outputLineId;} public void setOutputLineId(Long v){outputLineId=v;}
	/** Nomor lot bahan baku sumber, disalin apa adanya dari {@code ProduksiDokumenBaris.lotNo} baris input pada saat baris genealogi ini ditulis -- murni informatif/pencarian, TIDAK disinkronkan ulang otomatis bila nomor lot baris input diubah belakangan. Opsional. */
	@Column(name="input_lot_no",length=120) public String getInputLotNo(){return inputLotNo;} public void setInputLotNo(String v){inputLotNo=v;}
	/** Nomor lot hasil produksi tujuan, disalin dari {@code ProduksiDokumenBaris.lotNo} baris output pada saat baris genealogi ini ditulis -- sama sifatnya (salinan beku, bukan live) dengan {@link #getInputLotNo()}. Opsional. */
	@Column(name="output_lot_no",length=120) public String getOutputLotNo(){return outputLotNo;} public void setOutputLotNo(String v){outputLotNo=v;}
	/**
	 * Kuantitas dari lot bahan baku {@link #getInputLineId()} yang dialokasikan/dianggap menyusun
	 * lot hasil {@link #getOutputLineId()} pada baris genealogi ini. Wajib diisi, default
	 * {@link BigDecimal#ZERO}, presisi {@code precision=19, scale=4}.
	 *
	 * <p><b>Penjaga keseimbangan (TIDAK ditegakkan oleh model/database).</b> Secara konsep, untuk
	 * satu baris input tertentu, jumlah {@code allocatedQty} pada seluruh baris genealogi yang
	 * menunjuknya SEHARUSNYA tidak melebihi {@code ProduksiDokumenBaris.qty} baris input tersebut
	 * (tidak mengalokasikan lebih banyak bahan baku daripada yang benar-benar dikonsumsi baris
	 * itu) -- dan simetrinya berlaku untuk baris output. Kelas ini, {@link ProduksiDokumenBaris},
	 * maupun skema database TIDAK memiliki constraint atau trigger yang menegakkan invarian
	 * tersebut: tidak ada {@code CHECK}/agregat penjaga, tidak ada validasi di setter. Penelusuran
	 * kode penulis ({@code ProduksiApiHelper.simpanGenealogi}) menunjukkan baris genealogi ditulis
	 * langsung dari payload JSON permintaan {@code simpan} tanpa validasi silang terhadap qty baris
	 * yang ditunjuk -- pemanggil (klien/staf yang mengisi form genealogi) bertanggung jawab penuh
	 * menjaga penjumlahan qty tetap masuk akal. Ini konsisten dengan pola "soft-check aplikasi,
	 * bukan hard-guard data" yang sudah tercatat berulang pada domain produksi/finansial AIS (lihat
	 * javadoc {@link MutasiStokProduksi#getQtyMasuk()} untuk pembahasan pola serupa) -- dicatat di
	 * sini sebagai referensi audit, bukan temuan baru yang genuinely berbeda.</p>
	 *
	 * @return kuantitas teralokasi pada baris genealogi ini, tidak pernah {@code null} (default
	 *         {@link BigDecimal#ZERO}).
	 */
	@Column(name="allocated_qty",nullable=false,precision=19,scale=4) public BigDecimal getAllocatedQty(){return allocatedQty;} public void setAllocatedQty(BigDecimal v){allocatedQty=v;}
	/** Waktu baris tautan genealogi ini dicatat. Wajib diisi, default waktu konstruksi objek Java ({@code new Date()}) -- dibekukan sekali saat instance dibuat, bukan dihitung ulang tiap getter dipanggil. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
