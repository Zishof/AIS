package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.library.Penyedia;

/**
 * Header pembayaran hutang supplier (layar legacy 24-26; event KREDIT register hutang, pola
 * TRAN_HUT.DBF: TGLBAYAR/JUMLAH/KETBAYAR/NOMERBG/NAMABANK/TANGGALBG). Satu pembayaran boleh
 * dialokasikan ke BANYAK faktur ({@link AlokasiPembayaranHutangSupplier}); total alokasi =
 * nominal header dan tiap alokasi tidak melebihi outstanding faktur (divalidasi atomik di
 * helper). {@code kodeUnik} = kunci idempoten (retry jaringan tidak menggandakan pembayaran --
 * pola sama {@code SesiKasKasir.kode}). Event posted TIDAK dihapus -- koreksi = reversal
 * (pembayaran bernilai metode REVERSAL yang mengembalikan outstanding, fase lanjut).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pembayaran_hutang_supplier")
public class PembayaranHutangSupplier extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}, entity ini tidak dikirim lewat Java serialization jarak jauh. */
	private static final long serialVersionUID = 1L;

	/** Pembayaran tunai. Nilai default {@link #metode} bila kolom NULL/kosong. */
	public static final String METODE_TUNAI = "TUNAI";
	/** Pembayaran transfer bank. */
	public static final String METODE_TRANSFER = "TRANSFER";
	/** Pembayaran giro/bilyet giro -- satu-satunya metode yang mengaktifkan siklus
	 * {@link #getStatusBg()}/{@link #getTanggalStatusBg()}. */
	public static final String METODE_GIRO = "GIRO";
	/** Pembayaran berupa potongan/diskon (non-kas) yang mengurangi outstanding faktur. */
	public static final String METODE_DISCOUNT = "DISCOUNT";
	/** Pembayaran berupa retur barang (non-kas) yang mengurangi outstanding faktur. */
	public static final String METODE_RETUR = "RETUR";

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Penyedia (supplier) yang menerima pembayaran ini. Wajib diisi. Lihat
	 * {@link #getSupplier()}. */
	private Penyedia supplier;
	/** Tanggal pembayaran. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Total nominal pembayaran ini, yang lalu dipecah ke satu/banyak baris {@link
	 * AlokasiPembayaranHutangSupplier} (SUM alokasi = nominal ini, ditegakkan helper). Lihat
	 * {@link #getNominal()}. */
	private BigDecimal nominal;
	/** Metode pembayaran; salah satu konstanta {@code METODE_*}. Lihat {@link #getMetode()}. */
	private String metode;
	/** Nomor bilyet giro (hanya relevan utk {@link #METODE_GIRO}). Lihat {@link #getNoBg()}. */
	private String noBg;
	/** Nama bank penerbit giro (hanya relevan utk {@link #METODE_GIRO}). Lihat
	 * {@link #getNamaBank()}. */
	private String namaBank;
	/** Tanggal jatuh tempo giro (hanya relevan utk {@link #METODE_GIRO}). Lihat
	 * {@link #getTanggalBg()}. */
	private Date tanggalBg;
	/** Catatan bebas ttg pembayaran ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Kunci idempoten dari klien (UUID) -- retry jaringan tidak menggandakan pembayaran. Lihat
	 * {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** User yang mencatat pembayaran ini. Lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	// P10 reversal + siklus BG: dokumen posted tidak dihapus -- dibatalkan lewat
	// dokumen pembalik; giro punya status pencairan sendiri.
	/** Status dokumen: {@link #DOK_AKTIF} (default) | {@link #DOK_DIBATALKAN} | {@link
	 * #DOK_REVERSAL}. Lihat {@link #getStatusDok()}. */
	private String statusDok;
	/** Alasan reversal, diisi pada baris {@link #DOK_REVERSAL} maupun pada baris asal yang
	 * ditandai {@link #DOK_DIBATALKAN}. Lihat {@link #getAlasanReversal()}. */
	private String alasanReversal;
	/** id dokumen asal yang dibalik, diisi hanya pada baris {@link #DOK_REVERSAL}. Lihat
	 * {@link #getReversalDari()}. */
	private Long reversalDari;
	/** Status siklus giro: {@link #BG_DITERIMA} -&gt; {@link #BG_CAIR}/{@link #BG_TOLAK} (null
	 * utk metode non-{@link #METODE_GIRO}). Lihat {@link #getStatusBg()}. */
	private String statusBg;
	/** Tanggal status giro terakhir diubah (cair/tolak). Lihat {@link #getTanggalStatusBg()}. */
	private Date tanggalStatusBg;

	/** Nilai default/normal {@link #statusDok} -- pembayaran berlaku, ikut mengurangi outstanding
	 * faktur teralokasi. */
	public static final String DOK_AKTIF = "AKTIF";
	/** Pembayaran sudah direversal -- dokumen asal TIDAK dihapus, hanya ditandai; pasangannya
	 * adalah satu baris baru berstatus {@link #DOK_REVERSAL} yang membalik nilainya. */
	public static final String DOK_DIBATALKAN = "DIBATALKAN";
	/** Dokumen pembalik hasil reversal; {@link #reversalDari} menunjuk id dokumen asal. */
	public static final String DOK_REVERSAL = "REVERSAL";
	/** Giro baru diterima, belum dicairkan/ditolak bank. Nilai awal {@link #statusBg} utk
	 * {@link #METODE_GIRO}. */
	public static final String BG_DITERIMA = "DITERIMA";
	/** Giro sudah cair di bank. */
	public static final String BG_CAIR = "CAIR";
	/** Giro ditolak bank (dana tidak cair). */
	public static final String BG_TOLAK = "TOLAK";

	/** Nama petugas yang mencatat baris ini (jejak audit tampilan, bukan FK). Lihat
	 * {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang mencatat baris ini (jejak audit, pasangan {@link #oleh}). Lihat
	 * {@link #getOlehId()}. */
	private String olehId;
	/** Waktu baris ini dicatat. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris pembayaran ini TERAKHIR diubah,
	 * dengan menuliskan waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum setiap {@code UPDATE} (mis. saat {@link #statusDok}/{@link
	 * #statusBg} diubah), tidak pernah dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi --
	 * dokumen ini normalnya dibentuk oleh {@code SalesInventoryPayableHelper} yang sekaligus
	 * membuat baris {@link AlokasiPembayaranHutangSupplier} sesuai invariant keseimbangan (lihat
	 * Javadoc kelas). */
	public PembayaranHutangSupplier() {
	}

	/**
	 * PK identity baris pembayaran ini. {@code null} sebelum entity di-{@code save}/{@code
	 * flush} ke Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}).
	 *
	 * @return id baris pembayaran, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak
	 * perlu memanggil ini secara eksplisit.
	 *
	 * @param id id baris pembayaran.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Penyedia (supplier) yang menerima pembayaran ini. {@code nullable = false}. Relasi
	 * {@code LAZY}: mengakses field pada objek yang dikembalikan di luar sesi Hibernate yang
	 * masih terbuka akan melempar {@code LazyInitializationException}.
	 *
	 * @return supplier penerima pembayaran.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier", nullable = false)
	public Penyedia getSupplier() {
		return supplier;
	}

	/**
	 * Menetapkan supplier penerima pembayaran.
	 *
	 * @param supplier penyedia terkait.
	 */
	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	/**
	 * Tanggal pembayaran. Getter null-safe: mengembalikan waktu SEKARANG bila kolom belum diisi
	 * (mis. objek baru yang belum di-{@code set}), BUKAN {@code null}.
	 *
	 * @return tanggal pembayaran, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menetapkan tanggal pembayaran.
	 *
	 * @param tanggal tanggal pembayaran baru.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Total nominal pembayaran ini. Getter null-safe: mengembalikan {@link BigDecimal#ZERO} bila
	 * kolom NULL di DB. Nilai ini yang menjadi batas atas SUM baris {@link
	 * AlokasiPembayaranHutangSupplier} pada penjagaan keseimbangan {@code
	 * SalesInventoryPayableHelper} (lihat Javadoc kelas) -- entity ini sendiri TIDAK menegakkan
	 * penjagaan itu.
	 *
	 * @return nominal pembayaran, tidak pernah {@code null}.
	 */
	@Column(name = "nominal", precision = 19, scale = 2)
	public BigDecimal getNominal() {
		return nominal == null ? BigDecimal.ZERO : nominal;
	}

	/**
	 * Menetapkan nominal pembayaran. Tidak divalidasi di sini -- validasi "nominal &gt; 0" dan
	 * "SUM alokasi = nominal ini" dilakukan {@code SalesInventoryPayableHelper} sebelum entity
	 * ini dibangun.
	 *
	 * @param nominal nominal pembayaran baru.
	 */
	public void setNominal(BigDecimal nominal) {
		this.nominal = nominal;
	}

	/**
	 * Metode pembayaran. Getter null-safe: mengembalikan {@link #METODE_TUNAI} bila kolom
	 * NULL/kosong di DB.
	 *
	 * @return metode pembayaran, tidak pernah {@code null}/kosong.
	 */
	@Column(name = "metode", length = 20)
	public String getMetode() {
		return metode == null || metode.trim().isEmpty() ? METODE_TUNAI : metode;
	}

	/**
	 * Menetapkan metode pembayaran. Tidak memvalidasi nilai terhadap konstanta {@code METODE_*}
	 * -- pemanggil bertanggung jawab memakai nilai yang benar (mis. mengisi {@link #getNoBg()}
	 * dkk. hanya utk {@link #METODE_GIRO}).
	 *
	 * @param metode metode pembayaran baru.
	 */
	public void setMetode(String metode) {
		this.metode = metode;
	}

	/**
	 * Nomor bilyet giro (hanya relevan utk {@link #METODE_GIRO}).
	 *
	 * @return nomor giro, atau {@code null} bila metode bukan giro/tidak diisi.
	 */
	@Column(name = "no_bg", length = 60)
	public String getNoBg() {
		return noBg;
	}

	/**
	 * Menetapkan nomor giro.
	 *
	 * @param noBg nomor giro baru.
	 */
	public void setNoBg(String noBg) {
		this.noBg = noBg;
	}

	/**
	 * Nama bank penerbit giro (hanya relevan utk {@link #METODE_GIRO}).
	 *
	 * @return nama bank, atau {@code null} bila metode bukan giro/tidak diisi.
	 */
	@Column(name = "nama_bank", length = 100)
	public String getNamaBank() {
		return namaBank;
	}

	/**
	 * Menetapkan nama bank penerbit giro.
	 *
	 * @param namaBank nama bank baru.
	 */
	public void setNamaBank(String namaBank) {
		this.namaBank = namaBank;
	}

	/**
	 * Tanggal jatuh tempo giro (hanya relevan utk {@link #METODE_GIRO}).
	 *
	 * @return tanggal giro, atau {@code null} bila metode bukan giro/tidak diisi.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_bg")
	public Date getTanggalBg() {
		return tanggalBg;
	}

	/**
	 * Menetapkan tanggal jatuh tempo giro.
	 *
	 * @param tanggalBg tanggal giro baru.
	 */
	public void setTanggalBg(Date tanggalBg) {
		this.tanggalBg = tanggalBg;
	}

	/**
	 * Catatan bebas ttg pembayaran ini.
	 *
	 * @return keterangan, atau {@code null}/kosong bila tidak diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan.
	 *
	 * @param keterangan catatan bebas baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Kunci idempoten dari klien (UUID) -- duplicate retry mengembalikan pembayaran pertama.
	 *
	 * @return kode unik idempoten, atau {@code null} bila tidak dipakai jalur idempoten.
	 */
	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	/**
	 * Menetapkan kode unik idempoten. Keunikannya ditegakkan constraint DB
	 * ({@code unique = true}), bukan dicek manual di setter ini.
	 *
	 * @param kodeUnik kode unik baru.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * User yang mencatat pembayaran ini (relasi FK ke {@link Tbmuser}, berbeda dari pasangan
	 * shadow {@link #getOleh()}/{@link #getOlehId()} yang bukan FK).
	 *
	 * @return user pencatat, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	/**
	 * Menetapkan user pencatat.
	 *
	 * @param dibuatOleh user pencatat baru.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/** AKTIF (default) | DIBATALKAN (sudah direversal) | REVERSAL (dokumen pembaliknya).
	 *
	 * @return status dokumen, tidak pernah {@code null}/kosong (default {@link #DOK_AKTIF}).
	 */
	@Column(name = "status_dok", length = 20)
	public String getStatusDok() {
		return statusDok == null || statusDok.trim().isEmpty() ? DOK_AKTIF : statusDok;
	}

	/**
	 * Menetapkan status dokumen. Tidak memvalidasi nilai terhadap konstanta {@code DOK_*} --
	 * pemanggil (jalur reversal) bertanggung jawab memakai nilai yang benar dan mengisi
	 * {@link #setReversalDari(Long)} secara konsisten.
	 *
	 * @param statusDok status dokumen baru.
	 */
	public void setStatusDok(String statusDok) {
		this.statusDok = statusDok;
	}

	/**
	 * Alasan reversal/pembatalan.
	 *
	 * @return alasan reversal, atau {@code null} bila dokumen belum pernah direversal.
	 */
	@Column(name = "alasan_reversal", columnDefinition = "text")
	public String getAlasanReversal() {
		return alasanReversal;
	}

	/**
	 * Menetapkan alasan reversal.
	 *
	 * @param alasanReversal alasan reversal baru.
	 */
	public void setAlasanReversal(String alasanReversal) {
		this.alasanReversal = alasanReversal;
	}

	/** id dokumen asal yang dibalik (diisi hanya pada baris REVERSAL).
	 *
	 * @return id dokumen asal, atau {@code null} bila baris ini bukan baris {@link #DOK_REVERSAL}.
	 */
	@Column(name = "reversal_dari")
	public Long getReversalDari() {
		return reversalDari;
	}

	/**
	 * Menetapkan id dokumen asal yang dibalik.
	 *
	 * @param reversalDari id dokumen asal.
	 */
	public void setReversalDari(Long reversalDari) {
		this.reversalDari = reversalDari;
	}

	/** Siklus giro: DITERIMA -> CAIR | TOLAK (null utk metode non-GIRO).
	 *
	 * @return status siklus giro, atau {@code null} bila metode bukan {@link #METODE_GIRO}.
	 */
	@Column(name = "status_bg", length = 20)
	public String getStatusBg() {
		return statusBg;
	}

	/**
	 * Menetapkan status siklus giro. Tidak memvalidasi transisi (mis. tidak mencegah
	 * {@link #BG_CAIR} langsung ke {@link #BG_DITERIMA}) -- disiplin transisi ada di lapisan
	 * pemanggil.
	 *
	 * @param statusBg status siklus giro baru.
	 */
	public void setStatusBg(String statusBg) {
		this.statusBg = statusBg;
	}

	/**
	 * Tanggal status giro terakhir diubah (cair/tolak).
	 *
	 * @return tanggal perubahan status giro, atau {@code null} bila belum pernah berubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_status_bg")
	public Date getTanggalStatusBg() {
		return tanggalStatusBg;
	}

	/**
	 * Menetapkan tanggal perubahan status giro.
	 *
	 * @param tanggalStatusBg tanggal perubahan status giro baru.
	 */
	public void setTanggalStatusBg(Date tanggalStatusBg) {
		this.tanggalStatusBg = tanggalStatusBg;
	}

	/**
	 * Nama petugas yang mencatat baris pembayaran ini (jejak audit tampilan, bukan FK).
	 *
	 * @return nama pencatat, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan {@link #oleh}. Guard null/blank: nilai {@code null} atau string kosong/spasi
	 * DIABAIKAN (early return) -- field yang sudah terisi TIDAK PERNAH ditimpa balik ke kosong
	 * oleh pemanggilan setter ini dengan argumen kosong. Pola guard yg sama dipakai
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pencatat; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * ID/username petugas yang mencatat baris pembayaran ini, pasangan {@link #getOleh()}.
	 *
	 * @return id/username pencatat, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank sama seperti {@link #setOleh(String)}: nilai
	 * {@code null}/kosong/spasi diabaikan, nilai lama dipertahankan.
	 *
	 * @param olehId id/username pencatat; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Waktu baris pembayaran ini dicatat. Getter null-safe: mengembalikan waktu SEKARANG bila
	 * kolom belum diisi, BUKAN {@code null} -- catatan yang sama seperti {@link #getTanggal()}.
	 *
	 * @return waktu pencatatan, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu pencatatan baris ini.
	 *
	 * @param waktu waktu pencatatan baru.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Timestamp perubahan terakhir baris pembayaran ini, diisi otomatis oleh {@link #onUpdate()}
	 * setiap kali Hibernate melakukan {@code UPDATE}. Nilai awal (sebelum ada update apa pun)
	 * adalah waktu instansiasi objek Java, BUKAN waktu insert DB sesungguhnya.
	 *
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}; memanggil setter ini eksplisit dari kode aplikasi
	 * akan ditimpa lagi oleh callback tsb pada {@code UPDATE} berikutnya.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Penanda jurnal. Jurnal pembayaran hutang: debet Utang Supplier, kredit Kas/Bank. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal pembayaran hutang ini (debet Utang Supplier, kredit Kas/Bank).
	 *
	 * @return riwayat posting, atau {@code null} bila baris ini belum pernah diposting.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan riwayat posting jurnal. Dipanggil mesin posting saat menjurnalkan baris ini;
	 * kode aplikasi normal tidak perlu memanggilnya langsung.
	 *
	 * @param postingHistory riwayat posting baru.
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}


	/**
	 * Tautan ke Daftar Pengajuan Transfer (DPC) -- membuat pembayaran hutang supplier toko ikut
	 * muncul di menu Pembayaran Transfer keuangan, sejajar dengan pembayaran pengadaan aset.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.DaftarPengajuanTransfer daftarPengajuanTransfer;

	/**
	 * Pengajuan transfer (DPC) tempat pembayaran ini diajukan/diproses, bila alur pembayarannya
	 * lewat menu Pembayaran Transfer keuangan (bukan dibayar langsung dari kas toko).
	 *
	 * @return pengajuan transfer terkait, atau {@code null} bila tidak lewat jalur DPC.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public ais.database.model.akunting.DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menetapkan pengajuan transfer (DPC) terkait. Dipanggil {@code
	 * DaftarPengajuanTransfer.simpanPembayaranHutangSupplier}; kode aplikasi normal tidak perlu
	 * memanggilnya langsung.
	 *
	 * @param daftarPengajuanTransfer pengajuan transfer baru.
	 */
	public void setDaftarPengajuanTransfer(ais.database.model.akunting.DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

}
