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
 * Dokumen induk (header) distribusi/pengiriman barang antar lokasi, dengan detail pengangkutan
 * pihak ketiga (kurir, resi, biaya angkut) yang tidak dimiliki mekanisme transfer lain di paket ini.
 *
 * <p><b>Klaster header-baris-event.</b> Kelas ini adalah akar dari klaster tiga tabel: baris item
 * per produk ada di {@link DistribusiDokumenBaris} ({@code document_id} menunjuk ke {@link #getId()}
 * kelas ini), dan riwayat perubahan status ada di {@link DistribusiDokumenEvent}. Pola header-baris-log
 * ini SAMA PERSIS dengan klaster {@code ProduksiDokumen}/{@code ProduksiDokumenBaris}/{@code
 * ProduksiDokumenEvent} di paket yang sama (didokumentasikan terpisah), hanya beda domain -- klaster
 * itu untuk hasil produksi, klaster ini untuk pengiriman/distribusi barang antar toko/gudang.</p>
 *
 * <p><b>Dua pasang "toko" dengan makna berbeda.</b> {@link #getTokoId()} adalah toko PEMILIK/pembuat
 * dokumen ini -- dipakai sebagai lingkup penomoran ({@code document_no} unik per {@code (toko_id,
 * document_type)}) dan lingkup idempotensi ({@link #getClientMutationId()} unik per {@code toko_id}).
 * {@link #getOriginTokoId()}/{@link #getDestinationTokoId()} adalah ENDPOINT rute transfer yang
 * sesungguhnya (opsional, kolom {@code Long} polos tanpa anotasi relasi/FK) -- toko pemilik dokumen
 * tidak harus sama dengan salah satu endpoint ini, meski pada praktiknya kemungkinan besar begitu.
 * {@link #getOriginName()}/{@link #getDestinationName()} adalah label teks bebas terpisah (mis. untuk
 * lokasi non-toko seperti gudang eksternal) yang TIDAK divalidasi konsisten dengan id toko di atas.</p>
 *
 * <p><b>Bukan turunan {@code GeneralValueObject}.</b> Berbeda dari kebanyakan entity lain di paket
 * ini (mis. {@link MutasiStokToko}, {@code AmbangStokGudang}), kelas ini implementasi langsung
 * {@link Serializable} -- TIDAK mewarisi {@code ais.database.model.GeneralValueObject}, tidak
 * ber-{@code @Audited} envers, dan tidak punya hook {@code @PreUpdate} yang menstempel ulang field
 * audit secara otomatis. Akibatnya {@link #getUpdatedAt()}/{@link #getUpdatedBy()} HARUS di-set manual
 * oleh kode pemanggil pada setiap perubahan -- tidak ada mekanisme di model ini yang menjaminnya
 * tersinkron dengan waktu {@code UPDATE} sesungguhnya (lihat javadoc {@link #getUpdatedAt()}).</p>
 *
 * <p><b>Status klaster: seluruhnya belum tersambung ke kode aplikasi (dormant).</b> Penelusuran
 * menyeluruh atas seluruh sumber di WC ini TIDAK menemukan satu pun listener/service/action/helper
 * yang membuat, membaca, mengubah, atau menghapus baris {@code DistribusiDokumen}/{@link
 * DistribusiDokumenBaris}/{@link DistribusiDokumenEvent}/{@link DistribusiPostingStok}. Satu-satunya
 * jejak keempat kelas ini di luar dirinya sendiri adalah pendaftaran mapping Hibernate di {@code
 * hibernate.cfg.xml} dan satu referensi javadoc di {@code MutasiStokProduksi} (perbandingan pola
 * idempotensi oleh sesi dokumentasi paralel, bukan pemakaian kode). Tabel {@code koperasi
 * .distribution_document} beserta tiga tabel anaknya karena itu SECARA PRAKTIS adalah skema yatim --
 * dipetakan penuh dan siap dipakai Hibernate, tapi tidak ada satu pun jalur kode yang benar-benar
 * menulis atau membaca isinya di WC ini. Ini konsisten dengan pola "entity tidur/yatim" yang sudah
 * berulang kali ditemukan di audit dokumentasi paket ini (bukan temuan baru yang perlu task terpisah).</p>
 *
 * <p><b>Relasi dengan {@link MutasiStokToko} -- TIDAK ADA di kode, hanya kemiripan nama field.</b>
 * {@link MutasiStokToko} adalah ledger transfer antar-outlet yang AKTIF dipakai fitur "Mutasi Stok
 * Antar Outlet" (satu baris menunjuk dua sisi produk/toko sekaligus, penjaga keseimbangan struktural
 * lewat satu kolom qty -- lihat javadoc kelas itu). Klaster distribusi di sini punya field {@code
 * legacyMutationId} pada {@link DistribusiPostingStok} yang NAMANYA mengisyaratkan pointer ke baris
 * ledger "legacy" -- kandidat paling masuk akal adalah {@code MutasiStokToko.id} karena itu satu-
 * satunya mekanisme transfer sebaris-dua-sisi yang sebanding di paket ini -- tapi kolom itu {@code
 * Long} polos tanpa {@code @ManyToOne}/{@code @JoinColumn}, dan karena tidak ada kode yang mengisinya,
 * hubungan ini murni spekulatif dari penamaan, bukan relasi yang benar-benar ditegakkan atau bahkan
 * pernah dipakai. Lihat javadoc {@link DistribusiPostingStok#getLegacyMutationId()} untuk detail.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "distribution_document", uniqueConstraints = {
	@UniqueConstraint(columnNames = { "toko_id", "document_type", "document_no" }),
	@UniqueConstraint(columnNames = { "toko_id", "client_mutation_id" }) })
public class DistribusiDokumen implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Primary key baris dokumen. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Toko PEMILIK/pembuat dokumen ini -- lingkup penomoran {@link #getDocumentNo()} dan lingkup idempotensi {@link #getClientMutationId()}. Lihat javadoc kelas untuk perbedaannya dengan {@link #originTokoId}/{@link #destinationTokoId}. */
	private Long tokoId;
	/** Jenis/tipe dokumen distribusi, teks bebas (tidak ada enum) -- bagian dari kunci unik bersama {@link #tokoId} dan {@link #documentNo}. */
	private String documentType;
	/** Nomor dokumen, unik per {@code (tokoId, documentType)}. Penomorannya (format, urutan) sepenuhnya tanggung jawab kode pemanggil -- tidak ada di model ini. */
	private String documentNo;
	/** Status alur kerja dokumen, default {@code "DRAFT"}. Teks bebas, bukan enum -- transisi status idealnya dicatat sebagai baris {@link DistribusiDokumenEvent}, tapi tidak ada mekanisme di model ini yang menegakkan itu. */
	private String status = "DRAFT";
	/** Nomor referensi eksternal (mis. dari sistem/pihak lain), opsional. */
	private String referenceNo;
	/** Label teks bebas lokasi asal (mis. nama gudang/toko), terpisah dari {@link #originTokoId} dan tidak divalidasi konsisten dengannya. */
	private String originName;
	/** Label teks bebas lokasi tujuan, terpisah dari {@link #destinationTokoId} dan tidak divalidasi konsisten dengannya. */
	private String destinationName;
	/** Id toko asal transfer (endpoint rute), kolom {@code Long} polos tanpa anotasi relasi/FK -- opsional. Lihat javadoc kelas untuk perbedaannya dari {@link #tokoId}. */
	private Long originTokoId;
	/** Id toko tujuan transfer (endpoint rute), kolom {@code Long} polos tanpa anotasi relasi/FK -- opsional. */
	private Long destinationTokoId;
	/** Nama perusahaan/jasa kurir pengangkut, opsional. */
	private String carrierName;
	/** Nomor resi/pelacakan pengiriman dari kurir, opsional. */
	private String trackingNo;
	/** Nama penerima barang di lokasi tujuan, opsional. */
	private String receiverName;
	/** URL/path berkas bukti pengiriman (mis. foto tanda terima), opsional, panjang hingga 1000 karakter. */
	private String proofUrl;
	/** Nomor invoice/tagihan biaya angkut dari kurir, opsional. */
	private String freightInvoiceNo;
	/** Nominal biaya angkut/ongkos kirim, opsional. */
	private BigDecimal freightAmount;
	/** Tanggal invoice biaya angkut, opsional. */
	private Date freightInvoiceDate;
	/** Tanggal/waktu rencana pengiriman, opsional. */
	private Date plannedAt;
	/** Tanggal/waktu pengiriman sesungguhnya terjadi, opsional. */
	private Date actualAt;
	/** Catatan bebas teks untuk dokumen ini, opsional. */
	private String notes;
	/** Kunci idempotensi yang disuplai klien, unik per {@link #tokoId} -- lihat javadoc {@link #getClientMutationId()}. */
	private String clientMutationId;
	/** Userid/nama pembuat dokumen, opsional, tidak ber-FK. */
	private String createdBy;
	/** Waktu pembuatan baris, diinisialisasi ke waktu konstruksi objek Java saat instansiasi. */
	private Date createdAt = new Date();
	/** Userid/nama yang terakhir mengubah dokumen, opsional, tidak ber-FK. */
	private String updatedBy;
	/** Waktu terakhir diubah -- lihat javadoc {@link #getUpdatedAt()}, TIDAK di-refresh otomatis oleh model ini. */
	private Date updatedAt = new Date();
	/** Penghitung bernama "version" tapi TIDAK ber-anotasi {@code @javax.persistence.Version} -- lihat javadoc {@link #getVersion()}. */
	private Long version = Long.valueOf(0L);

	/**
	 * Primary key baris dokumen ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long id) { this.id = id; }

	/**
	 * Toko pemilik/pembuat dokumen ini, wajib diisi. Bersama {@link #getDocumentType()}/{@link
	 * #getDocumentNo()} membentuk kunci unik penomoran dokumen, dan bersama {@link
	 * #getClientMutationId()} membentuk kunci unik idempotensi klien. Kolom {@code Long} polos tanpa
	 * relasi entity -- pemanggil bertanggung jawab memastikan nilainya benar-benar id {@code Toko}
	 * yang valid, tidak ada validasi/FK di level database maupun model.
	 * @return id toko pemilik dokumen.
	 */
	@Column(name = "toko_id", nullable = false)
	public Long getTokoId() { return tokoId; } public void setTokoId(Long tokoId) { this.tokoId = tokoId; }

	/**
	 * Jenis/tipe dokumen distribusi, wajib diisi, teks bebas hingga 50 karakter (bukan enum -- tidak
	 * ada validasi nilai yang diperbolehkan di level model). Bagian dari kunci unik penomoran bersama
	 * {@link #getTokoId()} dan {@link #getDocumentNo()}.
	 * @return jenis dokumen.
	 */
	@Column(name = "document_type", nullable = false, length = 50)
	public String getDocumentType() { return documentType; } public void setDocumentType(String value) { documentType = value; }

	/**
	 * Nomor dokumen, wajib diisi, hingga 80 karakter, unik per {@code (tokoId, documentType)}.
	 * Pembentukan/urutan nomor sepenuhnya tanggung jawab kode pemanggil.
	 * @return nomor dokumen.
	 */
	@Column(name = "document_no", nullable = false, length = 80)
	public String getDocumentNo() { return documentNo; } public void setDocumentNo(String value) { documentNo = value; }

	/**
	 * Status alur kerja dokumen ini, wajib diisi, teks bebas hingga 30 karakter, default {@code
	 * "DRAFT"}. Bukan enum -- tidak ada mesin status yang menegakkan transisi valid di level model;
	 * idealnya setiap perubahan nilai ini disertai satu baris {@link DistribusiDokumenEvent} (pola
	 * {@code fromStatus}/{@code toStatus}) tapi tidak ada apa pun di kelas ini yang menjamin
	 * kedisiplinan itu -- sepenuhnya tanggung jawab kode pemanggil (yang, per catatan status dormant
	 * di javadoc kelas, saat ini tidak eksis).
	 * @return status dokumen saat ini, tidak pernah {@code null} pada objek baru (default {@code "DRAFT"}).
	 */
	@Column(name = "status", nullable = false, length = 30)
	public String getStatus() { return status; } public void setStatus(String value) { status = value; }

	/** @return nomor referensi eksternal dokumen ini, atau {@code null} bila tidak diisi. */
	@Column(name = "reference_no", length = 120)
	public String getReferenceNo() { return referenceNo; } public void setReferenceNo(String value) { referenceNo = value; }

	/**
	 * Label teks bebas lokasi asal (mis. nama gudang/toko/mitra), opsional, hingga 180 karakter.
	 * Murni informatif -- tidak divalidasi konsisten dengan {@link #getOriginTokoId()}.
	 * @return nama lokasi asal, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "origin_name", length = 180)
	public String getOriginName() { return originName; } public void setOriginName(String value) { originName = value; }

	/**
	 * Label teks bebas lokasi tujuan, opsional, hingga 180 karakter. Murni informatif -- tidak
	 * divalidasi konsisten dengan {@link #getDestinationTokoId()}.
	 * @return nama lokasi tujuan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "destination_name", length = 180)
	public String getDestinationName() { return destinationName; } public void setDestinationName(String value) { destinationName = value; }

	/**
	 * Id toko asal transfer (endpoint rute sesungguhnya), opsional. Kolom {@code Long} polos tanpa
	 * anotasi relasi/FK -- berbeda dari {@link #getTokoId()} (toko pemilik dokumen) dan dari desain
	 * relasi entity penuh {@code @ManyToOne} yang dipakai {@link MutasiStokToko#getTokoAsal()} untuk
	 * konsep serupa. Karena tanpa FK, integritas referensial nilai ini sepenuhnya bergantung disiplin
	 * kode pemanggil.
	 * @return id toko asal, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "origin_toko_id")
	public Long getOriginTokoId() { return originTokoId; } public void setOriginTokoId(Long value) { originTokoId = value; }

	/**
	 * Id toko tujuan transfer (endpoint rute sesungguhnya), opsional. Sama seperti {@link
	 * #getOriginTokoId()}, kolom {@code Long} polos tanpa FK.
	 * @return id toko tujuan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "destination_toko_id")
	public Long getDestinationTokoId() { return destinationTokoId; } public void setDestinationTokoId(Long value) { destinationTokoId = value; }

	/** @return nama kurir/jasa pengangkut, atau {@code null} bila tidak diisi. */
	@Column(name = "carrier_name", length = 180)
	public String getCarrierName() { return carrierName; } public void setCarrierName(String value) { carrierName = value; }

	/** @return nomor resi/pelacakan pengiriman, atau {@code null} bila tidak diisi. */
	@Column(name = "tracking_no", length = 120)
	public String getTrackingNo() { return trackingNo; } public void setTrackingNo(String value) { trackingNo = value; }

	/** @return nama penerima barang di lokasi tujuan, atau {@code null} bila tidak diisi. */
	@Column(name = "receiver_name", length = 180)
	public String getReceiverName() { return receiverName; } public void setReceiverName(String value) { receiverName = value; }

	/** @return URL/path bukti pengiriman (mis. foto tanda terima), atau {@code null} bila tidak diisi. */
	@Column(name = "proof_url", length = 1000)
	public String getProofUrl() { return proofUrl; } public void setProofUrl(String value) { proofUrl = value; }

	/** @return nomor invoice biaya angkut dari kurir, atau {@code null} bila tidak diisi. */
	@Column(name = "freight_invoice_no", length = 120)
	public String getFreightInvoiceNo() { return freightInvoiceNo; } public void setFreightInvoiceNo(String value) { freightInvoiceNo = value; }

	/** @return nominal biaya angkut/ongkos kirim, atau {@code null} bila tidak diisi (tidak dinormalisasi ke nol seperti field {@code BigDecimal} lain di paket ini). */
	@Column(name = "freight_amount", precision = 19, scale = 2)
	public BigDecimal getFreightAmount() { return freightAmount; } public void setFreightAmount(BigDecimal value) { freightAmount = value; }

	/** @return tanggal invoice biaya angkut, atau {@code null} bila tidak diisi. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "freight_invoice_date")
	public Date getFreightInvoiceDate() { return freightInvoiceDate; } public void setFreightInvoiceDate(Date value) { freightInvoiceDate = value; }

	/** @return tanggal/waktu rencana pengiriman, atau {@code null} bila tidak diisi. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "planned_at")
	public Date getPlannedAt() { return plannedAt; } public void setPlannedAt(Date value) { plannedAt = value; }

	/** @return tanggal/waktu pengiriman sesungguhnya, atau {@code null} bila belum/tidak diisi. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "actual_at")
	public Date getActualAt() { return actualAt; } public void setActualAt(Date value) { actualAt = value; }

	/** @return catatan bebas teks dokumen ini, atau {@code null} bila tidak diisi. */
	@Column(name = "notes", columnDefinition = "text")
	public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }

	/**
	 * Kunci idempotensi yang disuplai klien (mis. UUID yang dibuat sisi klien sebelum mengirim
	 * permintaan pembuatan dokumen), opsional, unik per {@link #getTokoId()}. Memungkinkan klien
	 * mengulang permintaan yang sama (mis. akibat retry jaringan) tanpa risiko membuat dokumen
	 * duplikat -- pemanggil cukup mengecek apakah baris dengan {@code (tokoId, clientMutationId)}
	 * yang sama sudah ada sebelum insert baru. Constraint unik ditegakkan database, bukan model ini.
	 * @return kunci idempotensi klien, atau {@code null} bila tidak dipakai.
	 */
	@Column(name = "client_mutation_id", length = 100)
	public String getClientMutationId() { return clientMutationId; } public void setClientMutationId(String value) { clientMutationId = value; }

	/** @return userid/nama pembuat dokumen, atau {@code null} bila tidak diisi. */
	@Column(name = "created_by", length = 100)
	public String getCreatedBy() { return createdBy; } public void setCreatedBy(String value) { createdBy = value; }

	/**
	 * Waktu pembuatan baris ini, wajib diisi. Diinisialisasi ke waktu konstruksi objek Java (bukan
	 * waktu commit transaksi) -- berbeda dari pola {@code @CreationTimestamp}/interceptor yang dipakai
	 * kelas lain di paket ini, nilai ini murni default field Java biasa dan bisa ditimpa manual oleh
	 * setter sebelum disimpan.
	 * @return waktu pembuatan baris.
	 */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; } public void setCreatedAt(Date value) { createdAt = value; }

	/** @return userid/nama yang terakhir mengubah dokumen, atau {@code null} bila tidak diisi. */
	@Column(name = "updated_by", length = 100)
	public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String value) { updatedBy = value; }

	/**
	 * Waktu terakhir dokumen ini diubah, wajib diisi. Diinisialisasi ke waktu konstruksi objek sama
	 * seperti {@link #getCreatedAt()} saat objek baru dibuat, TAPI berbeda dari pola {@code
	 * tanggal_dirubah} + hook {@code @PreUpdate} yang dipakai kelas-kelas lain di paket ini (mis.
	 * {@link MutasiStokToko#getTanggal_dirubah()}), kelas ini TIDAK punya hook siklus hidup apa pun --
	 * nilai ini TIDAK di-refresh otomatis oleh Hibernate pada setiap {@code UPDATE}. Kode pemanggil
	 * yang melakukan perubahan HARUS men-set field ini secara eksplisit lewat {@link
	 * #setUpdatedAt(Date)}; bila lupa, kolom akan diam-diam tetap membawa waktu pembuatan baris
	 * meski isinya sudah berubah berkali-kali.
	 * @return waktu terakhir diubah (tidak dijamin akurat tanpa disiplin kode pemanggil).
	 */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "updated_at", nullable = false)
	public Date getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Date value) { updatedAt = value; }

	/**
	 * Penghitung bernama "version", wajib diisi, default {@code 0}. Penamaannya mengisyaratkan
	 * optimistic locking, TAPI kolom ini HANYA ber-anotasi {@code @Column} biasa -- BUKAN {@code
	 * @javax.persistence.Version}. Akibatnya Hibernate TIDAK secara otomatis menaikkan nilai ini atau
	 * memeriksanya terhadap kondisi balapan ({@code StaleObjectStateException}) saat update; bila
	 * dipakai untuk mendeteksi update konkuren, kode pemanggil harus menaikkan dan membandingkan
	 * nilainya SENDIRI secara manual pada setiap operasi tulis -- tidak ada jaminan struktural dari
	 * model ini bahwa dua pembaruan konkuren tidak akan saling menimpa.
	 * @return nilai penghitung versi saat ini, tidak pernah {@code null} pada objek baru (default {@code 0}).
	 */
	@Column(name = "version", nullable = false)
	public Long getVersion() { return version; } public void setVersion(Long value) { version = value; }
}
