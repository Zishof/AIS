package ais.database.model.kursus;

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

import org.hibernate.envers.Audited;

import ais.common.BarcodeCommon;
import ais.database.model.GeneralValueObject;

/**
 * Satu percobaan (attempt) peserta kursus mengerjakan {@link MateriKursus} bertipe
 * {@link MateriKursus#QUIZ} yang tertaut ke sebuah {@link Ujian} (soal-soalnya di-reuse dari
 * {@link ais.database.model.BankSoal}/{@link ais.database.model.BankSoalDetail}/{@code Ujian}
 * yang sudah ada di sistem CBT akademik -- entity ini HANYA menyimpan riwayat percobaan+skor,
 * bukan definisi soal). Satu peserta bisa punya banyak baris untuk satu materi yang sama
 * (satu per percobaan, dibedakan {@link #getNomorPercobaan()}), dibatasi jumlahnya oleh
 * {@link MateriKursus#getBatasPercobaan()}.
 * <p>
 * <b>Alur hidup satu percobaan</b> (lihat pemanggil di
 * webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp): dibuat oleh aksi {@code mulai_kuis}
 * berstatus {@link #BERLANGSUNG} sekaligus membuat baris {@link JawabanPercobaanKuisKursus}
 * kosong (skor 0) untuk setiap soal yang akan dikerjakan; setiap jawaban diisi satu per satu
 * lewat aksi {@code jawab_soal_kuis}; ditutup lewat aksi {@code selesai_kuis} yang mengubah
 * status ke {@link #SELESAI} dan memicu {@code recomputePercobaan()} untuk menghitung ULANG
 * {@link #getTotalNilai()}/{@link #getJumlahBenar()}/{@link #getLulus()} dari seluruh baris
 * {@link JawabanPercobaanKuisKursus} milik percobaan ini -- lihat catatan integritas skor
 * lengkap di Javadoc kelas {@link JawabanPercobaanKuisKursus}. {@link #getLulus()} di percobaan
 * inilah yang menjadi syarat {@link ProgressMateriKursus#getSelesai()} bernilai {@code true}
 * untuk materi bertipe kuis (lihat catatan di Javadoc {@link ProgressMateriKursus}).
 * <p>
 * Tabel BARU (bukan kolom baru di tabel lama) sehingga {@code hbm2ddl=update} cukup membuat
 * tabel utama + tabel bayangan audit sekaligus, tanpa perlu self-heal manual seperti kolom-kolom
 * baru pada {@link MateriKursus}/{@link ProgressMateriKursus}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "percobaan_kuis_kursus")
public class PercobaanKuisKursus extends GeneralValueObject {

	/** Status percobaan yang sedang dikerjakan peserta, belum ditutup lewat aksi selesai_kuis. */
	public final static String BERLANGSUNG = "Berlangsung";
	/** Status percobaan yang sudah ditutup (peserta menekan "selesai kuis"); nilai final sudah terhitung. */
	public final static String SELESAI = "Selesai";

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil id pengguna pembuat/pengubah terakhir baris ini. Field audit "shadow" -- diisi
	 * lewat {@link ais.database.hibernate.AuditTimestampInterceptor}, bukan bagian dari data
	 * bisnis percobaan kuis.
	 *
	 * @return id pengguna pembuat/pengubah terakhir, atau {@code null} jika belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pembuat/pengubah terakhir. Nilai kosong/blank diabaikan (fail-safe).
	 *
	 * @param olehId id pengguna; diabaikan jika {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pembuat/pengubah terakhir. Nilai kosong/blank diabaikan (fail-safe).
	 *
	 * @param oleh nama pengguna; diabaikan jika {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nama pengguna pembuat/pengubah terakhir, atau {@code null} jika belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} otomatis sesaat
	 * sebelum UPDATE dieksekusi. Method audit "shadow", keharusan teknis infrastruktur.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi tanggal terakhir baris ini diubah. Biasanya diisi otomatis lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah tanggal/waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil tanggal terakhir baris ini diubah.
	 *
	 * @return tanggal/waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug: {@code kode + " - " + nama}.
	 *
	 * @return string gabungan kode dan nama percobaan.
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private MateriKursus materiKursus;
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	private Integer nomorPercobaan;
	private Date waktuMulai;
	private Date waktuSelesai;
	private String status;
	private Double totalNilai;
	private Boolean lulus;
	private Integer jumlahSoal;
	private Integer jumlahBenar;

	/** Konstruktor default -- dibutuhkan Hibernate untuk instansiasi lewat reflection. */
	public PercobaanKuisKursus() {
	}

	/**
	 * Mengambil id unik (primary key) baris percobaan ini, di-generate DB lewat
	 * strategi {@code IDENTITY}.
	 *
	 * @return id percobaan, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas untuk percobaan ini (jarang dipakai -- pemanggil di
	 * {@code _kursus_service.jsp} tidak pernah mengisi kolom ini).
	 *
	 * @return keterangan percobaan, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil kode unik baris percobaan ini, auto-generate sekali via
	 * {@link ais.common.BarcodeCommon#generateCode()} pada panggilan pertama jika belum ada.
	 * Dijaga {@code unique} di DB.
	 *
	 * @return kode unik percobaan, tidak pernah {@code null} setelah getter ini dipanggil.
	 */
	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil nama tampilan percobaan, dibentuk on-the-fly dari nama peserta + nama materi +
	 * nomor percobaan -- BUKAN kolom murni tersimpan; ditimpa ulang setiap getter ini dipanggil
	 * bila kedua relasi ({@link #getMateriKursus()}, {@link #getPesertaPunyaProdukKursus()})
	 * berhasil di-load. Pola sama dengan {@link ProgressMateriKursus#getNama()}.
	 *
	 * @return nama gabungan "nama peserta - nama materi (percobaan N)", atau nilai
	 *         {@link #nama} apa adanya jika relasi belum bisa di-resolve.
	 */
	public String getNama() {
		if (getMateriKursus() != null && getPesertaPunyaProdukKursus() != null
				&& getPesertaPunyaProdukKursus().getPesertaKursus() != null) {
			nama = getPesertaPunyaProdukKursus().getPesertaKursus().getNama() + " - " + materiKursus.getNama()
					+ " (percobaan " + getNomorPercobaan() + ")";
		}
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil {@link MateriKursus} bertipe {@link MateriKursus#QUIZ} yang dikerjakan pada
	 * percobaan ini.
	 *
	 * @return materi kuis terkait; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "materi_kursus", nullable = false)
	public MateriKursus getMateriKursus() {
		materiKursus = check(materiKursus);
		return materiKursus;
	}

	public void setMateriKursus(MateriKursus materiKursus) {
		this.materiKursus = materiKursus;
	}

	/**
	 * Mengambil enrollment ({@link PesertaPunyaProdukKursus}) pemilik percobaan ini.
	 *
	 * @return enrollment pemilik percobaan; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_punya_produk_kursus", nullable = false)
	public PesertaPunyaProdukKursus getPesertaPunyaProdukKursus() {
		pesertaPunyaProdukKursus = check(pesertaPunyaProdukKursus);
		return pesertaPunyaProdukKursus;
	}

	/**
	 * Mengisi enrollment pemilik percobaan ini. Tidak ada verifikasi kepemilikan di setter ini
	 * -- ditegakkan oleh pemanggil (aksi mulai_kuis) sebelum entity dibuat.
	 *
	 * @param pesertaPunyaProdukKursus enrollment pemilik percobaan.
	 */
	public void setPesertaPunyaProdukKursus(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
	}

	/**
	 * Mengambil nomor urut percobaan ini di antara seluruh percobaan peserta yang sama untuk
	 * materi yang sama (1 = percobaan pertama). Dihitung pemanggil dari jumlah percobaan
	 * berstatus {@link #SELESAI} sebelumnya + 1, bukan auto-increment DB.
	 *
	 * @return nomor percobaan, default 1 bila belum diisi.
	 */
	public Integer getNomorPercobaan() {
		return nomorPercobaan == null ? 1 : nomorPercobaan;
	}

	public void setNomorPercobaan(Integer nomorPercobaan) {
		this.nomorPercobaan = nomorPercobaan;
	}

	/**
	 * Mengambil waktu percobaan ini dimulai. Berbeda dari kebanyakan field tanggal lain di
	 * paket ini, getter ini TIDAK mengembalikan {@code null} untuk entity yang belum diisi --
	 * ia fallback ke {@code new Date()} (waktu saat getter dipanggil), sehingga dua panggilan
	 * berturut-turut pada entity baru yang belum di-set bisa mengembalikan nilai yang sedikit
	 * berbeda. Pemanggil di {@code _kursus_service.jsp} selalu meng-set nilai eksplisit lewat
	 * {@link #setWaktuMulai(Date)} sebelum membaca kembali via getter, sehingga perbedaan ini
	 * tidak teramati dalam alur normal.
	 *
	 * @return waktu mulai percobaan, atau waktu saat ini jika belum pernah di-set.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuMulai() {
		return waktuMulai == null ? new Date() : waktuMulai;
	}

	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengambil waktu percobaan ini ditutup (status berubah ke {@link #SELESAI}).
	 *
	 * @return waktu selesai, atau {@code null} jika percobaan masih {@link #BERLANGSUNG}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuSelesai() {
		return waktuSelesai;
	}

	public void setWaktuSelesai(Date waktuSelesai) {
		this.waktuSelesai = waktuSelesai;
	}

	/**
	 * Mengambil status percobaan ({@link #BERLANGSUNG}/{@link #SELESAI}). Default ke
	 * {@link #BERLANGSUNG} bila kolom kosong/null -- fail-safe supaya baris yang belum sempat
	 * diisi statusnya tidak dianggap sudah final.
	 *
	 * @return status percobaan, tidak pernah {@code null}/kosong (fallback {@link #BERLANGSUNG}).
	 */
	public String getStatus() {
		return status == null || status.isEmpty() ? BERLANGSUNG : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil nilai akhir percobaan dalam persen (0-100), dihitung server-side oleh
	 * {@code recomputePercobaan()} sebagai {@code (totalSkorJawaban / totalSkorMaksimalSoal) * 100}
	 * dibulatkan 2 desimal. TIDAK pernah diisi langsung dari input klien -- lihat catatan
	 * integritas skor kuis lengkap di Javadoc {@link JawabanPercobaanKuisKursus}.
	 *
	 * @return nilai akhir persen, default 0.0 bila belum dihitung.
	 */
	public Double getTotalNilai() {
		return totalNilai == null ? 0.0 : totalNilai;
	}

	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	/**
	 * Mengambil penanda kelulusan percobaan ini, dihitung server-side sebagai
	 * {@code totalNilai >= Ujian.getNilaiLulus()} oleh {@code recomputePercobaan()}. Field ini
	 * yang dipakai sebagai syarat materi kuis boleh ditandai selesai di
	 * {@link ProgressMateriKursus} -- lihat catatan integritas di Javadoc kelas itu.
	 *
	 * @return {@code true} jika lulus, {@code false} jika tidak lulus, atau {@code null} jika
	 *         percobaan belum pernah dihitung (mis. masih {@link #BERLANGSUNG}).
	 */
	public Boolean getLulus() {
		return lulus;
	}

	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	/**
	 * Mengambil jumlah total soal yang disajikan pada percobaan ini (bisa berbeda antar
	 * percobaan bila {@link MateriKursus#getJumlahSoalDitampilkan()} membatasi jumlah soal yang
	 * diambil sebagai subset acak dari bank soal).
	 *
	 * @return jumlah soal pada percobaan ini, default 0 bila belum diisi.
	 */
	public Integer getJumlahSoal() {
		return jumlahSoal == null ? 0 : jumlahSoal;
	}

	public void setJumlahSoal(Integer jumlahSoal) {
		this.jumlahSoal = jumlahSoal;
	}

	/**
	 * Mengambil jumlah soal yang dijawab BENAR pada percobaan ini, dihitung server-side oleh
	 * {@code recomputePercobaan()} dari jumlah baris {@link JawabanPercobaanKuisKursus} dengan
	 * {@link JawabanPercobaanKuisKursus#getBenar()} bernilai {@code true}.
	 *
	 * @return jumlah jawaban benar, default 0 bila belum dihitung.
	 */
	public Integer getJumlahBenar() {
		return jumlahBenar == null ? 0 : jumlahBenar;
	}

	public void setJumlahBenar(Integer jumlahBenar) {
		this.jumlahBenar = jumlahBenar;
	}

}
