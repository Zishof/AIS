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
 * <h2>ProduksiDokumen — dokumen induk (header) modul manufaktur/produksi koperasi.</h2>
 *
 * <p>Entitas Hibernate untuk tabel {@code koperasi.production_document}, dipetakan lewat mapping
 * anotasi murni (bukan DDL manual) sehingga skema tercipta otomatis via {@code hbm2ddl.auto=update}
 * saat bootstrap server (lihat {@code ProduksiApiHelper.tabelProduksiTersedia}, yang memeriksa
 * eksistensi tabel ini sebelum mengizinkan aksi apa pun agar kegagalan "tabel belum ada" muncul
 * sebagai pesan edukatif, bukan {@code SQLGrammarException} mentah). Satu baris merepresentasikan
 * SATU dokumen dari SEMBILAN jenis yang dikelola {@code ProduksiApiHelper} (dibedakan lewat
 * {@link #getDocumentType()}, kode singkat 2-6 huruf):</p>
 * <ul>
 *   <li><b>BOM</b> (Bill of Material) — resep/susunan komponen suatu produk jadi. Baris-barisnya
 *       ({@link ProduksiDokumenBaris}) berisi baris {@code OUTPUT} (produk jadi yang dihasilkan,
 *       biasanya satu) dan baris komponen (bahan baku yang dibutuhkan). TIDAK menggerakkan stok
 *       sendiri — murni definisi resep yang dirujuk WO/UNBUILD lewat {@link #getBomId()}.</li>
 *   <li><b>WO</b> (Work Order) — perintah kerja produksi memakai satu BOM (opsional, WO manual boleh
 *       tanpa BOM). Siklus status {@code DRAFT → RELEASED → IN_PROGRESS → COMPLETED/CANCELLED}: saat
 *       {@code RELEASED}, komponen BOM (di-skala rasio {@link #getPlannedQty()} terhadap qty baris
 *       OUTPUT BOM) DIKUNCI sebagai {@link ReservasiStokProduksi} dan kekurangan stok dilaporkan
 *       (opsional diajukan pembelian antar-gudang bila toko punya Gudang Pemasok). WO TIDAK
 *       menggerakkan stok sendiri — konsumsi bahan riilnya terjadi lewat dokumen ISSUE terpisah
 *       ber-{@link #getReferenceNo()} = nomor WO ini.</li>
 *   <li><b>ISSUE</b> — pengeluaran bahan baku dari gudang ke lantai produksi. Menggerakkan stok
 *       KELUAR saat {@code POSTED}; bila ber-referensi WO, mengurangi {@code qtySisa} reservasi
 *       komponen WO terkait ({@code ProduksiApiHelper.sesuaikanReservasiIssue}).</li>
 *   <li><b>RETURN</b> — pengembalian bahan baku yang tidak jadi dipakai. Menggerakkan stok MASUK
 *       saat {@code POSTED} (arah kebalikan ISSUE, jenis dokumen berbeda).</li>
 *   <li><b>OUTPUT</b> — hasil produksi jadi masuk gudang. Menggerakkan stok MASUK saat
 *       {@code POSTED}; bila baris-barisnya memuat produk ber-{@code flag perlu_qc}, otomatis
 *       menerbitkan dokumen QC dan mengkarantina batch ber-lot sama
 *       ({@code ProduksiApiHelper.buatQcAlertJikaPerlu}, Fase E).</li>
 *   <li><b>WASTE</b> — bahan/produk yang dibuang/rusak/basi selama produksi. Menggerakkan stok
 *       KELUAR saat {@code POSTED} (arah sama dengan ISSUE).</li>
 *   <li><b>COST</b> — pencatatan biaya tambahan (mis. koreksi biaya tenaga kerja/overhead) TANPA
 *       efek stok — satu-satunya jenis selain BOM/WO yang tidak masuk {@code jenisStok()}.</li>
 *   <li><b>UNBUILD</b> (Fase D) — membongkar produk jadi kembali menjadi komponennya (kebalikan
 *       WO/OUTPUT): SATU dokumen memuat baris {@code OUTPUT} (produk jadi yang KELUAR/dibongkar)
 *       DAN baris komponen (yang MASUK kembali ke stok) sekaligus — arah efek stok ditentukan
 *       PER BARIS (lewat {@code lineType}), bukan per dokumen seperti jenis lain
 *       ({@code ProduksiApiHelper.arahMasukBaris}). Komponennya bisa diprefill otomatis dari BOM
 *       produk (ter-skala rasio qty) lewat {@code isiKomponenUnbuildDariBom}.</li>
 *   <li><b>QC</b> (Quality Alert, Fase E) — diterbitkan OTOMATIS oleh sistem (bukan input manual)
 *       saat OUTPUT memuat produk ber-QC; baris-barisnya menyalin baris OUTPUT terkait
 *       (ber-{@code stockAffecting=false}, murni catatan). Ditutup lewat disposisi
 *       (REWORK → draf WO baru; UNBUILD/SCRAP → dokumen turunan DRAFT; RELEASE → tanpa turunan),
 *       lihat {@code ProduksiApiHelper.terapkanDisposisiQc}.</li>
 * </ul>
 *
 * <p><b>Identitas &amp; keunikan.</b> Kombinasi ({@link #getTokoId()}, {@link #getDocumentType()},
 * {@link #getDocumentNo()}) unik per baris — satu toko tidak bisa punya dua dokumen jenis sama
 * bernomor sama. Kombinasi ({@link #getTokoId()}, {@link #getClientMutationId()}) juga unik dan
 * dipakai sebagai kunci idempotensi sisi klien: {@code ProduksiApiHelper.simpan} mencari dokumen
 * existing lewat {@code clientMutationId} sebelum membuat baru, sehingga retry simpan dari klien
 * (mis. koneksi terputus setelah commit) tidak menggandakan dokumen.</p>
 *
 * <p><b>Siklus edit &amp; status.</b> Dokumen hanya bisa diedit penuh (ubah baris/genealogi) selagi
 * {@link #getStatus()} masih {@code DRAFT} — begitu berpindah status, {@code ProduksiApiHelper.simpan}
 * menolaknya. Transisi status divalidasi per jenis dokumen oleh {@code ProduksiApiHelper.transisi}
 * (mis. BOM: {@code DRAFT→ACTIVE/CANCELLED}, {@code ACTIVE→RETIRED}; WO: rantai
 * {@code DRAFT→RELEASED→IN_PROGRESS→COMPLETED}, {@code CANCELLED} dari mana saja sebelum
 * {@code COMPLETED}; jenis lain: {@code DRAFT→POSTED→REVERSED}). Setiap transisi dicatat sebagai
 * satu baris {@link ProduksiDokumenEvent} (audit trail append-only, tidak ada relasi terpetakan ke
 * kelas ini — hanya id mentah).</p>
 *
 * <p><b>Biaya.</b> {@link #getMaterialCost()}/{@link #getLaborCost()}/{@link #getOverheadCost()}
 * diisi MANUAL oleh pemanggil (form/API) — TIDAK dihitung otomatis dari baris ISSUE/qty yang
 * sesungguhnya diposting; {@link #getTotalCost()}/{@link #getUnitCost()} sendiri DIHITUNG ULANG
 * ({@code ProduksiApiHelper.hitungBiaya}, dipanggil tiap {@code simpan}/transisi ke
 * {@code COMPLETED}/{@code POSTED}) sebagai penjumlahan tiga komponen manual dan pembagian
 * terhadap {@link #getActualQty()} (atau {@link #getPlannedQty()} bila actual masih nol). <b>Tidak
 * ada penjaga keseimbangan bahan-hasil di level model/database</b>: tidak ada constraint atau
 * validasi silang yang memastikan total qty ISSUE/WASTE (bahan keluar, memperhitungkan
 * rendemen/susut BOM) sebenarnya sesuai dengan qty OUTPUT (produk jadi dihasilkan) untuk WO yang
 * sama — WO dapat berpindah ke {@code COMPLETED} tanpa dokumen OUTPUT terkait pernah dibuat/
 * diposting sama sekali, dan {@link #getActualQty()}/biaya dokumen adalah input bebas pemanggil.
 * Ini konsisten dengan pola soft-check aplikasi (bukan hard-guard data) yang sudah tercatat
 * berulang pada ledger/dokumen produksi di paket ini (lihat javadoc
 * {@link MutasiStokProduksi#getQtyMasuk()}) — dicatat di sini sebagai referensi audit atas
 * permintaan eksplisit pemeriksaan penjaga keseimbangan, bukan sebagai temuan baru yang genuinely
 * berbeda dari pola yang sudah diketahui.</p>
 *
 * @see ProduksiDokumenBaris
 * @see ProduksiDokumenEvent
 * @see ProduksiGenealogiLot
 * @see ReservasiStokProduksi
 * @see MutasiStokProduksi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "production_document",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = { "toko_id", "document_type", "document_no" }),
		@UniqueConstraint(columnNames = { "toko_id", "client_mutation_id" })
	})
public class ProduksiDokumen implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long tokoId;
	private String documentType;
	private String documentNo;
	private String status = "DRAFT";
	private String referenceNo;
	private Long bomId;
	private BigDecimal plannedQty = BigDecimal.ZERO;
	private BigDecimal actualQty = BigDecimal.ZERO;
	private String uom;
	private BigDecimal materialCost = BigDecimal.ZERO;
	private BigDecimal laborCost = BigDecimal.ZERO;
	private BigDecimal overheadCost = BigDecimal.ZERO;
	private BigDecimal totalCost = BigDecimal.ZERO;
	private BigDecimal unitCost = BigDecimal.ZERO;
	private Date plannedAt;
	private Date actualAt;
	private String notes;
	private String clientMutationId;
	private String createdBy;
	private Date createdAt = new Date();
	private String updatedBy;
	private Date updatedAt = new Date();
	private Long version = Long.valueOf(0L);

	/** Primary key dokumen ini; digenerasi database ({@code IDENTITY}), {@code null} sebelum dokumen pertama kali disimpan. Setter normalnya hanya dipanggil Hibernate saat memuat baris dari DB. */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	/** Id toko pemilik dokumen ini -- filter tenant wajib pada seluruh kueri {@code ProduksiApiHelper} (daftar/detail/simpan selalu memeriksa {@code tokoId} cocok dengan toko aktor). Bagian dari kunci unik {@code (toko_id, document_type, document_no)} dan {@code (toko_id, client_mutation_id)}. */
	@Column(name = "toko_id", nullable = false)
	public Long getTokoId() { return tokoId; }
	public void setTokoId(Long tokoId) { this.tokoId = tokoId; }
	/** Jenis dokumen: salah satu dari {@code BOM/WO/ISSUE/RETURN/OUTPUT/WASTE/COST/UNBUILD/QC} -- lihat javadoc kelas untuk peran masing-masing. Ditentukan sekali saat dokumen dibuat ({@code ProduksiApiHelper.simpan} menolak mengubah jenis dokumen existing). Bagian dari kunci unik {@code (toko_id, document_type, document_no)}. */
	@Column(name = "document_type", nullable = false, length = 50)
	public String getDocumentType() { return documentType; }
	public void setDocumentType(String documentType) { this.documentType = documentType; }
	/** Nomor dokumen, unik per (toko, jenis). Default {@code <KODE_JENIS>-<epoch_millis>} bila tidak diisi eksplisit saat dibuat ({@code ProduksiApiHelper.simpan}); untuk dokumen WO/QC/UNBUILD/WASTE turunan otomatis, formatnya mengikuti pola pemicunya (mis. {@code WO-AUTO-<millis>-<produk>}, {@code QC-<nomorOutput>}). Bagian dari kunci unik {@code (toko_id, document_type, document_no)}. */
	@Column(name = "document_no", nullable = false, length = 80)
	public String getDocumentNo() { return documentNo; }
	public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
	/** Status siklus hidup dokumen, default {@code "DRAFT"}. Nilai dan transisi yang diizinkan berbeda per {@link #getDocumentType()} -- lihat javadoc kelas bagian "Siklus edit & status". Setter TIDAK memvalidasi transisi (validasi ada di {@code ProduksiApiHelper.transisi}, dipanggil sebelum {@code setStatus}). */
	@Column(name = "status", nullable = false, length = 30)
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	/** Rujukan bebas teks ke dokumen terkait, maknanya berbeda per jenis: untuk WO, dipakai draf-otomatis sebagai kunci idempotensi pembuatan ({@code buatWoDrafOtomatis}, mis. kode ambang stok atau {@code "QC:<id>:<produk>"} dari disposisi rework); untuk ISSUE, diisi nomor dokumen WO yang dirujuk sehingga {@code sesuaikanReservasiIssue} tahu reservasi komponen mana yang harus disesuaikan; untuk QC/turunan disposisi, nomor dokumen OUTPUT/QC pemicunya. Opsional. */
	@Column(name = "reference_no", length = 120)
	public String getReferenceNo() { return referenceNo; }
	public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
	/** Id {@link ProduksiDokumen} bertipe BOM yang menjadi resep dokumen ini (relevan untuk WO dan UNBUILD; referensi id polos, tanpa relasi Hibernate terpetakan) -- dipakai untuk menghitung rasio skala komponen terhadap {@link #getPlannedQty()}/qty baris. Opsional: WO manual boleh tanpa BOM (komponen diisi manual, tanpa reservasi otomatis saat rilis). */
	@Column(name = "bom_id")
	public Long getBomId() { return bomId; }
	public void setBomId(Long bomId) { this.bomId = bomId; }
	/** Kuantitas yang DIRENCANAKAN (target), default {@link BigDecimal#ZERO}, presisi {@code precision=19, scale=4}. Untuk WO, dipakai sebagai basis rasio skala komponen BOM saat rilis ({@code reservasiSaatRilis}) dan sebagai pembagi {@code hitungBiaya} bila {@link #getActualQty()} masih nol. */
	@Column(name = "planned_qty", precision = 19, scale = 4)
	public BigDecimal getPlannedQty() { return plannedQty; }
	public void setPlannedQty(BigDecimal plannedQty) { this.plannedQty = plannedQty; }
	/** Kuantitas AKTUAL (realisasi), default {@link BigDecimal#ZERO}, presisi sama dengan {@link #getPlannedQty()}. Diisi manual oleh pemanggil -- TIDAK dihitung otomatis dari qty baris OUTPUT yang benar-benar diposting (lihat javadoc kelas bagian "Biaya" soal ketiadaan penjaga keseimbangan). Dipakai sebagai pembagi utama {@code hitungBiaya} bila bernilai {@literal >} 0. */
	@Column(name = "actual_qty", precision = 19, scale = 4)
	public BigDecimal getActualQty() { return actualQty; }
	public void setActualQty(BigDecimal actualQty) { this.actualQty = actualQty; }
	/** Satuan unit kuantitas dokumen (mis. kg, pcs, liter) -- teks bebas, opsional. Untuk WO draf otomatis, disalin dari nama satuan produk target ({@code koperasi.satuan_produk}) bila tersedia. */
	@Column(name = "uom", length = 30)
	public String getUom() { return uom; }
	public void setUom(String uom) { this.uom = uom; }
	/** Komponen biaya bahan baku, default {@link BigDecimal#ZERO}, presisi {@code precision=19, scale=2}. Diisi MANUAL oleh pemanggil (form/API) -- lihat javadoc kelas bagian "Biaya". Salah satu dari tiga komponen yang dijumlahkan {@code hitungBiaya} menjadi {@link #getTotalCost()}. */
	@Column(name = "material_cost", precision = 19, scale = 2)
	public BigDecimal getMaterialCost() { return materialCost; }
	public void setMaterialCost(BigDecimal materialCost) { this.materialCost = materialCost; }
	/** Komponen biaya tenaga kerja, default {@link BigDecimal#ZERO}, presisi sama {@link #getMaterialCost()}. Diisi manual, salah satu komponen {@link #getTotalCost()}. */
	@Column(name = "labor_cost", precision = 19, scale = 2)
	public BigDecimal getLaborCost() { return laborCost; }
	public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }
	/** Komponen biaya overhead, default {@link BigDecimal#ZERO}, presisi sama {@link #getMaterialCost()}. Diisi manual, salah satu komponen {@link #getTotalCost()}. */
	@Column(name = "overhead_cost", precision = 19, scale = 2)
	public BigDecimal getOverheadCost() { return overheadCost; }
	public void setOverheadCost(BigDecimal overheadCost) { this.overheadCost = overheadCost; }
	/** Total biaya = {@link #getMaterialCost()} + {@link #getLaborCost()} + {@link #getOverheadCost()}, default {@link BigDecimal#ZERO}. DIHITUNG ULANG otomatis oleh {@code ProduksiApiHelper.hitungBiaya} tiap kali dokumen disimpan atau berpindah status ke {@code COMPLETED}/{@code POSTED} -- setter ada untuk keperluan Hibernate/mapping, TIDAK dimaksudkan diisi manual langsung oleh pemanggil normal. */
	@Column(name = "total_cost", precision = 19, scale = 2)
	public BigDecimal getTotalCost() { return totalCost; }
	public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
	/** Biaya per unit = {@link #getTotalCost()} dibagi {@link #getActualQty()} (atau {@link #getPlannedQty()} bila actual masih nol; hasil {@link BigDecimal#ZERO} bila kedua qty nol), pembulatan {@code HALF_UP} 4 desimal. DIHITUNG ULANG otomatis bersamaan {@link #getTotalCost()} -- sama, setter bukan untuk diisi manual. */
	@Column(name = "unit_cost", precision = 19, scale = 4)
	public BigDecimal getUnitCost() { return unitCost; }
	public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
	/** Tanggal/waktu rencana pelaksanaan dokumen (mis. tanggal WO dijadwalkan). Opsional; untuk WO draf otomatis diisi waktu pembuatan draf. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "planned_at")
	public Date getPlannedAt() { return plannedAt; }
	public void setPlannedAt(Date plannedAt) { this.plannedAt = plannedAt; }
	/** Tanggal/waktu realisasi aktual. Diisi otomatis ke waktu saat itu oleh {@code ProduksiApiHelper.ubahStatus} ketika status berpindah ke {@code COMPLETED}/{@code POSTED}; {@code null} sebelum dokumen mencapai status tersebut. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "actual_at")
	public Date getActualAt() { return actualAt; }
	public void setActualAt(Date actualAt) { this.actualAt = actualAt; }
	/** Catatan bebas dokumen -- juga dipakai sistem untuk menambahkan pesan otomatis (mis. peringatan "BELUM ADA BOM AKTIF" pada WO draf otomatis, ringkasan kekurangan komponen saat rilis WO, atau tag {@code [Disposisi: ...]} pada QC yang sudah didisposisi) dengan cara MENAMBAHKAN teks ke nilai lama, bukan menimpa. */
	@Column(name = "notes", columnDefinition = "text")
	public String getNotes() { return notes; }
	public void setNotes(String notes) { this.notes = notes; }
	/** Kunci idempotensi sisi klien untuk operasi simpan, opsional tapi unik bersama {@link #getTokoId()} bila diisi -- lihat javadoc kelas bagian "Identitas & keunikan". Dokumen yang dibuat tanpa nilai ini (klien lama/tanpa dukungan retry-aman) tidak ikut dicari lewat jalur idempotensi, hanya lewat {@code id} eksplisit. */
	@Column(name = "client_mutation_id", length = 100)
	public String getClientMutationId() { return clientMutationId; }
	public void setClientMutationId(String clientMutationId) { this.clientMutationId = clientMutationId; }
	/** Identitas (userid) pembuat dokumen -- diisi sekali saat dokumen dibuat, TIDAK berubah lagi setelahnya (berbeda dari {@link #getUpdatedBy()} yang diperbarui tiap simpan/transisi). {@code "SYSTEM"} untuk dokumen yang diterbitkan otomatis (WO draf otomatis, QC alert, turunan disposisi). */
	@Column(name = "created_by", length = 100)
	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
	/** Waktu dokumen pertama kali dibuat. Wajib diisi, default waktu instansiasi objek Java ({@code new Date()}) -- dibekukan sekali, TIDAK diperbarui lagi setelah dokumen pertama disimpan. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
	/** Identitas (userid) yang terakhir menyimpan/mengubah status dokumen -- diperbarui tiap {@code ProduksiApiHelper.simpan}/{@code ubahStatus} berhasil, berbeda dari {@link #getCreatedBy()} yang beku. */
	@Column(name = "updated_by", length = 100)
	public String getUpdatedBy() { return updatedBy; }
	public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
	/** Waktu dokumen terakhir diubah. Wajib diisi, default waktu instansiasi objek Java ({@code new Date()}); diperbarui manual oleh pemanggil ({@code ProduksiApiHelper}) tiap simpan/transisi status -- BUKAN lewat hook {@code @PreUpdate} otomatis (kelas ini tidak punya hook semacam itu, berbeda dari entity ber-{@code tanggal_dirubah} lain di paket ini). Dipakai sebagai kunci urutan default pada daftar dokumen ({@code order by updatedAt desc}). */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "updated_at", nullable = false)
	public Date getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
	/** Nomor versi untuk optimistic locking implisit (kolom polos, BUKAN dianotasi {@code @Version} JPA -- Hibernate TIDAK memeriksa/menaikkan kolom ini secara otomatis pada tiap update; nilainya statis {@code 0} sepanjang siklus hidup dokumen kecuali diubah manual). Default {@code Long.valueOf(0L)}. */
	@Column(name = "version", nullable = false)
	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }
}
