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
 * Sertifikat kelulusan/kelengkapan sebuah ProdukKursus, diterbitkan otomatis begitu semua
 * MateriKursus milik satu enrollment (PesertaPunyaProdukKursus) sudah selesai. Tabel BARU
 * (bukan kolom baru di tabel lama) sehingga hbm2ddl=update cukup membuat tabel utama + tabel
 * bayangan audit sekaligus, tanpa perlu self-heal manual.
 *
 * Kolom bawaan "kode" (GeneralValueObject, auto BarcodeCommon.generateCode()) dipakai sebagai
 * kode verifikasi publik -- reuse pola yang sama persis dengan entity kursus lain, bukan kolom baru.
 *
 * <h3>Mekanisme penerbitan -- terverifikasi server-side, bukan idempoten client-triggered</h3>
 * <p>
 * Entity ini murni model data; tidak ada method {@code terbitkan()}/{@code buat()} di kelas ini.
 * Satu-satunya jalur pembuatan baris baru adalah {@code cekDanTerbitkanSertifikat(Session,
 * PesertaPunyaProdukKursus)} di {@code WEB-INF/baru/modul/kursus/_kursus_service.jsp}, dipanggil
 * dari SETIAP endpoint yang menandai satu {@code MateriKursus} selesai ({@code tandai_selesai},
 * {@code upload_tugas}, {@code selesai_kuis}). Method itu:
 * <ol>
 * <li>menolak jika {@code enr} null atau statusnya bukan {@code PesertaPunyaProdukKursus.TERBELI}
 * (harus benar-benar sudah membeli, bukan sekadar enrollment kosong);</li>
 * <li>menolak (return tanpa efek) jika sudah ada {@link SertifikatKursus} untuk enrollment
 * tersebut -- idempoten, tidak bisa diterbitkan dobel lewat pemanggilan berulang;</li>
 * <li>mengumpulkan SEMUA {@code MateriKursus} dari SEMUA {@code SeksiKursus} milik
 * {@code ProdukKursus} terkait, lalu untuk tiap materi mengambil
 * {@code ProgressMateriKursus} milik enrollment ini -- jika progress tidak ada ATAU
 * {@code getSelesai()} bukan {@code true}, method langsung {@code return} tanpa membuat
 * sertifikat. Artinya penerbitan mensyaratkan progress "selesai" TERSIMPAN DI DB untuk setiap
 * materi, bukan sekadar parameter request yang dikirim client pada panggilan terakhir.</li>
 * </ol>
 * Dengan kata lain tidak ada jalur yang memicu penerbitan tanpa verifikasi kelulusan lengkap di
 * server: parameter request hanya menentukan MATERI MANA yang ditandai selesai pada panggilan itu
 * (dan itu pun lewat entity/id yang divalidasi, bukan boolean "sudahLulus" dari client), sedangkan
 * keputusan "apakah SEMUA materi sudah selesai" dihitung ulang dari tabel {@code ProgressMateriKursus}
 * setiap kali. {@link #getNomorSertifikat()} baru diisi SETELAH baris di-{@code save()} (memakai
 * {@link #getId()} yang sudah ter-generate), jadi nomor formal maupun kode verifikasi tidak pernah
 * dapat dipilih oleh pemanggil.
 * </p>
 *
 * <h3>Nomor formal vs kode verifikasi -- dua identifier dengan tingkat prediktabilitas berbeda</h3>
 * <p>
 * {@link #getNomorSertifikat()} (mis. {@code "SERT/2026/000123"}) dibentuk dari tahun berjalan +
 * {@link #getId()} (id numerik urut database) yang di-zero-pad -- SEPENUHNYA DAPAT DITEBAK/dihitung
 * dari sertifikat lain yang diterbitkan berdekatan (pola runtutan id/timestamp yang sama dengan
 * temuan {@code task_a1e32ff3} di modul lain). Ini TIDAK berbahaya di sini karena nomor ini hanya
 * label tampilan (dicetak di sertifikat, ditampilkan di halaman verifikasi) -- bukan kunci yang
 * dipakai untuk MENCARI/otentikasi sertifikat.
 * </p>
 * <p>
 * Pencarian publik ({@code VerifikasiSertifikatKursusServlet}, endpoint
 * {@code /VerifikasiSertifikatKursus?kode=...}) memakai {@link #getKode()} (kolom bawaan
 * {@code GeneralValueObject}, diisi lazy oleh {@code BarcodeCommon.generateCode()} -- kombinasi
 * epoch milidetik + counter in-memory, di-hex-encode). Kode ini jauh lebih sulit ditebak daripada
 * {@link #getNomorSertifikat()} karena bergantung waktu-persis pembuatan, TAPI tetap mengikuti pola
 * generator yang sama dengan {@code task_a1e32ff3} (bukan token acak kriptografis) -- CATATAN,
 * bukan task baru, karena polanya sudah tercatat luas di paket lain.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sertifikat_kursus")
public class SertifikatKursus extends GeneralValueObject {

	/**
	 * Nilai {@link #getStatus()} untuk sertifikat yang masih berlaku dan lolos verifikasi publik.
	 * Ini adalah status default ketika {@link #status} belum pernah diisi -- lihat {@link #getStatus()}.
	 */
	public final static String AKTIF = "Aktif";

	/**
	 * Nilai {@link #getStatus()} untuk sertifikat yang telah dicabut oleh admin lewat
	 * {@code ais.action.master.kursus.SertifikatKursusAction} (checkbox "Aktif (tidak dicabut)").
	 * Sertifikat dengan status ini TETAP dapat ditemukan oleh
	 * {@code ais.action.servlet.VerifikasiSertifikatKursusServlet} lewat {@link #getKode()} --
	 * halaman verifikasi publik menampilkan {@link #getStatus()} apa adanya, bukan menyembunyikan
	 * baris yang dicabut, sehingga pemeriksa dapat melihat status "Dicabut" secara eksplisit.
	 */
	public final static String DICABUT = "Dicabut";

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;

	/** Field audit shadow: nama pengubah terakhir, ditulis oleh {@code AuditTimestampInterceptor}. */
	private String oleh;

	/** Field audit shadow: id pengubah terakhir, ditulis oleh {@code AuditTimestampInterceptor}. */
	private String olehId;

	/**
	 * @return id (bukan nama) pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh
	 *         {@code AuditTimestampInterceptor}, bukan oleh alur bisnis penerbitan sertifikat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengubah terakhir. Nilai kosong/hanya-spasi diabaikan (dead assignment) agar
	 * baris audit sebelumnya tidak tertimpa nilai kosong saat interceptor dipanggil ulang.
	 *
	 * @param olehId id pengguna pengubah; {@code null} atau string kosong tidak melakukan apa-apa.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengubah terakhir. Nilai kosong/hanya-spasi diabaikan (dead assignment),
	 * simetris dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau string kosong tidak melakukan apa-apa.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang terakhir mengubah baris ini, diisi otomatis oleh interceptor audit. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} (dan field audit terkait)
	 * lewat {@code AuditTimestampInterceptor} setiap kali baris ini di-UPDATE.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu perubahan terakhir; default saat instance dibuat, ditimpa {@link #onUpdate()} saat UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir untuk ditimpa langsung (jarang dipanggil manual). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas "{@link #getKode() kode} - {@link #getNama() nama}". */
	public String toString() {
		return kode + " - " + nama;
	}

	/** Kode verifikasi publik unik; lihat {@link #getKode()} untuk mekanisme pengisiannya. */
	private String kode;

	/** Label tampilan gabungan nama peserta + nama kursus; lihat {@link #getNama()}. */
	private String nama;
	private String keterangan;

	/** Enrollment (pembelian kursus) yang diselesaikan peserta hingga menerbitkan sertifikat ini. */
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;

	/** Nomor formal cetak, mis. {@code "SERT/2026/000123"}; lihat catatan prediktabilitas di javadoc kelas. */
	private String nomorSertifikat;
	private Date tanggalTerbit;
	private Double nilaiAkhir;
	private Integer durasiBelajarMenit;

	/** Status siklus hidup: {@link #AKTIF} atau {@link #DICABUT}; lihat {@link #getStatus()} untuk default. */
	private String status;

	public SertifikatKursus() {
	}

	/** @return id baris, auto-generated (identity) oleh database. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; normalnya tidak diisi manual karena kolom bertanda {@code insertable = false}. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return keterangan bebas, boleh kosong. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return kode verifikasi publik. Diisi lazy pada pemanggilan pertama lewat
	 *         {@code BarcodeCommon.generateCode()} (epoch milidetik + counter in-memory, di-hex-encode)
	 *         jika belum ada -- lihat catatan prediktabilitas di javadoc kelas. Inilah kunci pencarian
	 *         yang dipakai {@code VerifikasiSertifikatKursusServlet}, BUKAN {@link #getId()} atau
	 *         {@link #getNomorSertifikat()}.
	 */
	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	/** @param kode kode verifikasi publik untuk ditetapkan langsung (jarang dipakai manual). */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return label tampilan "{nama peserta} - {nama kursus}", dihitung ulang setiap pemanggilan dari
	 *         {@link #getPesertaPunyaProdukKursus()} selama relasi tersebut dan
	 *         {@code PesertaKursus}/{@code ProdukKursus} miliknya tidak null; jika tidak, mengembalikan
	 *         nilai {@link #nama} yang tersimpan sebelumnya (bisa {@code null}) tanpa menghitung ulang.
	 */
	public String getNama() {
		if (getPesertaPunyaProdukKursus() != null && getPesertaPunyaProdukKursus().getPesertaKursus() != null
				&& getPesertaPunyaProdukKursus().getProdukKursus() != null) {
			nama = getPesertaPunyaProdukKursus().getPesertaKursus().getNama() + " - "
					+ getPesertaPunyaProdukKursus().getProdukKursus().getNama();
		}
		return nama;
	}

	/** @param nama nama tampilan untuk ditimpa langsung (akan dihitung ulang oleh {@link #getNama()} bila relasi tersedia). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return enrollment (pembelian kursus) yang diselesaikan hingga sertifikat ini terbit. Dipakai
	 *         {@code VerifikasiSertifikatKursusServlet} untuk menampilkan nama peserta/kursus/instruktur.
	 *         Direfresh lewat {@link GeneralValueObject#check(Object)} agar konsisten dengan identity map
	 *         session, sesuai pola entity lain di paket ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_punya_produk_kursus", nullable = false)
	public PesertaPunyaProdukKursus getPesertaPunyaProdukKursus() {
		pesertaPunyaProdukKursus = check(pesertaPunyaProdukKursus);
		return pesertaPunyaProdukKursus;
	}

	/** @param pesertaPunyaProdukKursus enrollment pemilik sertifikat ini; wajib diisi (kolom {@code nullable = false}). */
	public void setPesertaPunyaProdukKursus(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
	}

	/**
	 * @return nomor cetak formal (mis. {@code "SERT/2026/000123"}), diisi oleh
	 *         {@code cekDanTerbitkanSertifikat()} SETELAH baris di-{@code save()} memakai
	 *         {@link #getId()} yang sudah ter-generate -- {@code null} sebelum baris pernah disimpan.
	 *         Dapat ditebak dari sertifikat lain (lihat javadoc kelas); jangan dipakai sebagai token
	 *         rahasia, hanya sebagai label tampilan.
	 */
	@Column(unique = true)
	public String getNomorSertifikat() {
		return nomorSertifikat;
	}

	/** @param nomorSertifikat nomor cetak formal untuk ditetapkan langsung. */
	public void setNomorSertifikat(String nomorSertifikat) {
		this.nomorSertifikat = nomorSertifikat;
	}

	/** @return waktu penerbitan; default "sekarang" (bukan tersimpan) bila field belum pernah diisi. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalTerbit() {
		return tanggalTerbit == null ? new Date() : tanggalTerbit;
	}

	/** @param tanggalTerbit waktu penerbitan sertifikat. */
	public void setTanggalTerbit(Date tanggalTerbit) {
		this.tanggalTerbit = tanggalTerbit;
	}

	/**
	 * @return rata-rata nilai kuis terbaik dari seluruh materi bertipe {@code QUIZ} pada enrollment ini,
	 *         atau {@code null} jika tidak ada materi kuis (dihitung sekali oleh
	 *         {@code cekDanTerbitkanSertifikat()} saat penerbitan, tidak dihitung ulang setelahnya).
	 */
	public Double getNilaiAkhir() {
		return nilaiAkhir;
	}

	/** @param nilaiAkhir nilai akhir untuk ditetapkan langsung. */
	public void setNilaiAkhir(Double nilaiAkhir) {
		this.nilaiAkhir = nilaiAkhir;
	}

	/** @return total durasi tontonan (menit) seluruh materi pada enrollment ini; {@code 0} bila belum diisi. */
	public Integer getDurasiBelajarMenit() {
		return durasiBelajarMenit == null ? 0 : durasiBelajarMenit;
	}

	/** @param durasiBelajarMenit total durasi belajar (menit) untuk ditetapkan langsung. */
	public void setDurasiBelajarMenit(Integer durasiBelajarMenit) {
		this.durasiBelajarMenit = durasiBelajarMenit;
	}

	/**
	 * @return status siklus hidup sertifikat: {@link #AKTIF} atau {@link #DICABUT}. Default
	 *         {@link #AKTIF} bila field belum pernah diisi atau kosong -- sertifikat yang baru
	 *         diterbitkan ({@code cekDanTerbitkanSertifikat()} menetapkan {@link #AKTIF} eksplisit)
	 *         maupun baris lama tanpa status tersimpan sama-sama dianggap aktif/berlaku.
	 */
	public String getStatus() {
		return status == null || status.isEmpty() ? AKTIF : status;
	}

	/** @param status status siklus hidup baru; gunakan konstanta {@link #AKTIF}/{@link #DICABUT}. */
	public void setStatus(String status) {
		this.status = status;
	}

}
