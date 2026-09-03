package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

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

import ais.database.model.GeneralValueObject;

/**
 * <h2>SurveyKepuasanPos — Rating Kepuasan Pembeli dari Layar Kedua POS.</h2>
 *
 * <p>
 * Entity BARU untuk menampung rating kepuasan (1-5) yang diisi PEMBELI sendiri lewat "Layar
 * Pelanggan" (layar kedua POS dual-monitor) setelah transaksi selesai. Dengan pendaftaran di
 * {@code hibernate.cfg.xml}, tabel {@code koperasi.survey_kepuasan_pos} DAN tabel audit Envers
 * {@code new_audit.survey_kepuasan_pos__audit} otomatis dibuat (hbm2ddl=update) saat RESTART --
 * entity BARU (bukan kolom baru pada entity lama), jadi tak butuh ALTER manual di InitIndex.java.
 * </p>
 *
 * <p>
 * Penamaan kolom mengikuti aturan proyek: field tanpa @Column ter-<i>fold</i> menjadi huruf kecil
 * tanpa underscore (mis. {@code olehId}→{@code olehid}). Kompatibel Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul POS layar kedua)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@org.hibernate.envers.Audited
@Table(schema = "koperasi", name = "survey_kepuasan_pos")
public class SurveyKepuasanPos extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Primary key baris survey. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Toko/kantin tempat transaksi POS yang disurvey berlangsung -- lihat javadoc {@link #getToko()}. */
	private Toko toko;
	/** Nilai rating 1-5 yang diisi pembeli lewat Layar Pelanggan -- lihat javadoc {@link #getRating()} untuk catatan validasi. */
	private Integer rating;
	/** Catatan/komentar bebas teks opsional dari pembeli yang menyertai rating. */
	private String catatan;
	/** Waktu pengisian survey -- lihat javadoc {@link #getWaktu()} untuk perilaku default. */
	private Date waktu;
	/** Userid/nama kasir/petugas yang sesinya sedang berjalan saat survey diisi (BUKAN identitas pembeli -- lihat javadoc kelas, pembeli mengisi lewat Layar Pelanggan tanpa login). */
	private String oleh;
	/** Id user terkait {@link #oleh}. */
	private String olehId;

	/**
	 * Hook lifecycle Hibernate dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris survey
	 * ini (dipicu anotasi {@link javax.persistence.PreUpdate}, BUKAN dipanggil manual). Mendelegasikan
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menstempel ulang
	 * {@link #tanggal_dirubah}. Murni hook siklus hidup entity -- tidak memvalidasi {@link #rating}
	 * (mis. memastikan nilainya di rentang 1-5); validasi semacam itu, bila ada, berada di lapisan
	 * pemanggil (layar Layar Pelanggan / API POS).
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu terakhir baris survey ini diubah -- field audit shadow diisi otomatis oleh
	 * {@link #onUpdate()} pada tiap {@code UPDATE}, terpisah dari riwayat versi penuh envers
	 * ({@code @Audited}). Inisialisasi default memakai waktu konstruksi objek Java, bukan waktu commit
	 * transaksi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via refleksi saat memuat baris dari database; kode aplikasi yang mencatat survey baru dari Layar Pelanggan juga memakainya lalu mengisi field lewat setter. */
	public SurveyKepuasanPos() {
	}

	/**
	 * Primary key baris survey ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Toko/kantin tempat transaksi POS yang disurvey berlangsung. Relasi {@code LAZY}; getter
	 * memanggil {@code check(toko)} milik {@link GeneralValueObject} yang menormalisasi proxy/nilai
	 * kosong sebelum dikembalikan. Dipakai untuk memisahkan/memfilter rekap kepuasan per toko pada
	 * dasbor multi-toko.
	 * @return toko terkait survey ini (bisa proxy lazy, dinormalisasi via {@code check()}), atau
	 *         {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/** @param toko toko/kantin tempat transaksi POS yang disurvey berlangsung. */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Nilai rating kepuasan (dimaksudkan 1-5, sesuai deskripsi bintang di Layar Pelanggan) yang diisi
	 * PEMBELI sendiri, bukan kasir.
	 *
	 * <p><b>Tidak ada validasi rentang di level model ini.</b> Field ini murni {@code Integer} tanpa
	 * anotasi {@code @Column} bertingkat (kolom di-fold otomatis menjadi {@code rating}), tanpa
	 * {@code @Min}/{@code @Max} atau {@code CHECK constraint} yang tampak di sini -- getter maupun
	 * setter tidak memaksa nilai berada di rentang 1-5. Bila lapisan pemanggil (UI Layar Pelanggan
	 * atau endpoint API yang menerimanya) tidak memvalidasi input sebelum {@code save}, nilai di luar
	 * rentang (mis. 0, negatif, atau lebih dari 5) bisa tersimpan tanpa penolakan, yang berpotensi
	 * mendistorsi rata-rata/statistik kepuasan pada dasbor rekap bila terjadi.</p>
	 *
	 * @return rating yang diisi pembeli, atau {@code null} bila belum diisi/entity baru.
	 */
	public Integer getRating() {
		return rating;
	}

	/** @param rating nilai rating kepuasan (dimaksudkan 1-5); tidak divalidasi rentangnya di setter ini. */
	public void setRating(Integer rating) {
		this.rating = rating;
	}

	/**
	 * Catatan/komentar bebas teks opsional dari pembeli yang menyertai rating pada Layar Pelanggan.
	 * @return catatan, atau {@code null} bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	/** @param catatan catatan/komentar bebas teks dari pembeli. */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Waktu survey diisi. Berbeda dari kebanyakan field tanggal di klaster ini yang membiarkan
	 * {@code null} apa adanya, getter ini SELALU mengembalikan nilai non-{@code null}: bila field
	 * mentah {@code null}, dikembalikan {@code ais.ui.util.WaktuUtil.getDate()} -- yaitu waktu SAAT
	 * GETTER DIPANGGIL, bukan waktu survey benar-benar diisi. Efek praktisnya: memanggil getter ini
	 * berulang kali pada entity yang belum pernah men-set {@link #waktu} secara eksplisit akan
	 * mengembalikan nilai yang BERBEDA setiap kali (bukan nilai stabil) -- pemanggil yang butuh waktu
	 * pengisian yang konsisten harus memastikan {@link #setWaktu(Date)} dipanggil eksplisit sebelum
	 * {@code save}, jangan mengandalkan nilai default getter ini untuk keperluan audit presisi.
	 * @return waktu survey diisi, atau waktu saat ini bila belum pernah di-{@code set}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/** @param waktu waktu survey diisi. */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Userid/nama sesi yang sedang berjalan saat baris survey ini dicatat -- BUKAN identitas pembeli
	 * (pembeli mengisi lewat Layar Pelanggan tanpa login/identitas, lihat javadoc kelas), melainkan
	 * konteks sesi kasir/perangkat POS yang aktif.
	 * @return userid/nama sesi pencatat, atau {@code null} bila tidak diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/** @param oleh userid/nama sesi yang sedang berjalan saat baris survey ini dicatat. Berbeda dari pola guard di kelas lain klaster ini, setter ini menerima nilai {@code null}/kosong apa adanya tanpa diabaikan. */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Id user terkait {@link #getOleh()}.
	 * @return id user pencatat, atau {@code null} bila tidak diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/** @param olehId id user terkait {@link #getOleh()}. Setter ini menerima nilai {@code null}/kosong apa adanya (tidak ada guard silent-ignore seperti pada beberapa model lain klaster ini). */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu terakhir baris survey ini diubah, diisi otomatis oleh {@link #onUpdate()} pada
	 * tiap {@code UPDATE}.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @param tanggal_dirubah waktu terakhir diubah. Normalnya TIDAK diisi manual oleh kode aplikasi --
	 *                        dikelola otomatis oleh {@link #onUpdate()}; setter ini ada untuk kebutuhan
	 *                        Hibernate (property accessor) dan skenario migrasi/backfill data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
