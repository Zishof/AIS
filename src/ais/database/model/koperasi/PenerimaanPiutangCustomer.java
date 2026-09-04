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

/**
 * Penerimaan piutang customer / collection (layar legacy 34-36) -- cermin AP
 * {@link PembayaranHutangSupplier}: satu penerimaan boleh melunasi banyak faktur lewat
 * {@link AlokasiPenerimaanPiutangCustomer} (full/partial/multi-invoice, Matriks layar 34);
 * {@code kodeUnik} idempoten (retry offline outbox P7 aman); koreksi = dokumen pembalik
 * (REVERSAL menyusul), bukan hapus.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "penerimaan_piutang_customer")
public class PenerimaanPiutangCustomer extends GeneralValueObject {

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
	/** Penerimaan berupa potongan/diskon (non-kas) yang mengurangi outstanding faktur. */
	public static final String METODE_DISCOUNT = "DISCOUNT";
	/** Penerimaan berupa retur barang (non-kas) yang mengurangi outstanding faktur. */
	public static final String METODE_RETUR = "RETUR";

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Nomor kwitansi (teks), diisi pasca-insert dari {@link #id}. Lihat {@link #getNomor()}. */
	private String nomor;
	/** Anggota koperasi (pelanggan) yang membayar lewat penerimaan ini. Wajib diisi. Lihat
	 * {@link #getCustomer()}. */
	private AnggotaKoperasi customer;
	/** Sales penagih (nullable -- penerimaan langsung di kantor tanpa sales lapangan). Lihat
	 * {@link #getSales()}. */
	private SalesInventory sales;
	/** Tanggal penerimaan. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Total nominal penerimaan ini, yang lalu dipecah ke satu/banyak baris
	 * {@link AlokasiPenerimaanPiutangCustomer} (SUM alokasi = nominal ini, ditegakkan helper).
	 * Lihat {@link #getNominal()}. */
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
	/** Catatan bebas ttg penerimaan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Kunci idempoten dari klien (UUID) -- retry offline outbox aman. Lihat
	 * {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** User yang mencatat penerimaan ini. Lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Sesi Nota Sales tempat penerimaan ini terjadi (nullable). Lihat {@link #getSesi()}. */
	private NotaSalesSession sesi;
	// P10 reversal + siklus BG (pola sama PembayaranHutangSupplier).
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

	/** Nilai default/normal {@link #statusDok} -- penerimaan berlaku, ikut mengurangi outstanding
	 * faktur teralokasi. */
	public static final String DOK_AKTIF = "AKTIF";
	/** Penerimaan sudah direversal -- dokumen asal TIDAK dihapus, hanya ditandai; pasangannya
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
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris penerimaan ini TERAKHIR diubah,
	 * dengan menuliskan waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum setiap {@code UPDATE} (mis. saat {@link #statusDok}/{@link
	 * #statusBg} diubah), tidak pernah dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi --
	 * dokumen ini normalnya dibentuk oleh helper collection (mis. {@code si_collection_create})
	 * yang sekaligus membuat baris {@link AlokasiPenerimaanPiutangCustomer} sesuai invariant
	 * keseimbangan (lihat Javadoc kelas). */
	public PenerimaanPiutangCustomer() {
	}

	/**
	 * PK identity baris penerimaan ini. {@code null} sebelum entity di-{@code save}/{@code flush}
	 * ke Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}); nilai ini yang
	 * lalu dipakai membentuk {@link #getNomor()}.
	 *
	 * @return id baris penerimaan, atau {@code null} bila belum tersimpan.
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
	 * @param id id baris penerimaan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** Nomor kwitansi (teks) -- diisi pasca-insert dari id ({@code KWT-{id 6 digit}}).
	 *
	 * @return nomor kwitansi, atau {@code null} sebelum diisi pasca-insert.
	 */
	@Column(name = "nomor", length = 60, unique = true)
	public String getNomor() {
		return nomor;
	}

	/**
	 * Menetapkan nomor kwitansi. Normalnya diisi otomatis oleh helper posting pasca-insert (dari
	 * {@link #getId()}); setter ini tidak memvalidasi keunikan sendiri (mengandalkan constraint
	 * {@code unique = true} pada kolomnya di DB).
	 *
	 * @param nomor nomor kwitansi baru.
	 */
	public void setNomor(String nomor) {
		this.nomor = nomor;
	}

	/**
	 * Anggota koperasi (pelanggan) yang membayar lewat penerimaan ini. {@code nullable = false}.
	 * Relasi {@code LAZY}: mengakses field pada objek yang dikembalikan di luar sesi Hibernate
	 * yang masih terbuka akan melempar {@code LazyInitializationException}.
	 *
	 * @return customer pembayar.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer", nullable = false)
	public AnggotaKoperasi getCustomer() {
		customer = check(customer);
		return customer;
	}

	/**
	 * Menetapkan customer pembayar penerimaan ini.
	 *
	 * @param customer anggota koperasi (pelanggan) terkait.
	 */
	public void setCustomer(AnggotaKoperasi customer) {
		this.customer = customer;
	}

	/** Sales penagih (nullable -- penerimaan langsung di kantor tanpa sales lapangan).
	 *
	 * @return sales penagih, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales")
	public SalesInventory getSales() {
		sales = check(sales);
		return sales;
	}

	/**
	 * Menetapkan sales penagih.
	 *
	 * @param sales sales inventory penagih, boleh {@code null}.
	 */
	public void setSales(SalesInventory sales) {
		this.sales = sales;
	}

	/**
	 * Tanggal penerimaan. Getter null-safe: mengembalikan waktu SEKARANG bila kolom belum diisi
	 * (mis. objek baru yang belum di-{@code set}), BUKAN {@code null}.
	 *
	 * @return tanggal penerimaan, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menetapkan tanggal penerimaan.
	 *
	 * @param tanggal tanggal penerimaan baru.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Total nominal penerimaan ini. Getter null-safe: mengembalikan {@link BigDecimal#ZERO} bila
	 * kolom NULL di DB. Nilai ini yang menjadi batas atas SUM baris {@link
	 * AlokasiPenerimaanPiutangCustomer} pada penjagaan keseimbangan helper collection (lihat
	 * Javadoc kelas) -- entity ini sendiri TIDAK menegakkan penjagaan itu.
	 *
	 * @return nominal penerimaan, tidak pernah {@code null}.
	 */
	@Column(name = "nominal", precision = 19, scale = 2)
	public BigDecimal getNominal() {
		return nominal == null ? BigDecimal.ZERO : nominal;
	}

	/**
	 * Menetapkan nominal penerimaan. Tidak divalidasi di sini -- validasi "nominal &gt; 0" dan
	 * "SUM alokasi = nominal ini" dilakukan helper collection sebelum entity ini dibangun.
	 *
	 * @param nominal nominal penerimaan baru.
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
	 * Catatan bebas ttg penerimaan ini.
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

	/** Kunci idempoten dari klien (UUID) -- duplicate retry mengembalikan penerimaan pertama.
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
	 * User yang mencatat penerimaan ini (relasi FK ke {@link Tbmuser}, berbeda dari pasangan
	 * shadow {@link #getOleh()}/{@link #getOlehId()} yang bukan FK).
	 *
	 * @return user pencatat, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh")
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
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

	/** Sesi Nota Sales tempat penerimaan ini terjadi (nullable -- penerimaan kantor tanpa
	 *  sesi lapangan). Dipakai laporan sesi P5 (tertagih per sesi + kas COLLECTION_CASH).
	 *
	 * @return sesi nota sales terkait, atau {@code null} bila penerimaan bukan dari sesi lapangan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sesi")
	public NotaSalesSession getSesi() {
		sesi = check(sesi);
		return sesi;
	}

	/**
	 * Menetapkan sesi nota sales terkait.
	 *
	 * @param sesi sesi nota sales, boleh {@code null}.
	 */
	public void setSesi(NotaSalesSession sesi) {
		this.sesi = sesi;
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
	 * Nama petugas yang mencatat baris penerimaan ini (jejak audit tampilan, bukan FK).
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
	 * ID/username petugas yang mencatat baris penerimaan ini, pasangan {@link #getOleh()}.
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
	 * Waktu baris penerimaan ini dicatat. Getter null-safe: mengembalikan waktu SEKARANG bila
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
	 * Timestamp perubahan terakhir baris penerimaan ini, diisi otomatis oleh {@link #onUpdate()}
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
	 * Penanda jurnal. Jurnal penerimaan piutang: debet Kas/Bank, kredit Piutang. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal penerimaan piutang ini (debet Kas/Bank, kredit Piutang).
	 *
	 * @return riwayat posting, atau {@code null} bila baris ini belum pernah diposting.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
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

}
