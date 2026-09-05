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
 * Progres penyelesaian satu {@link MateriKursus} oleh satu peserta yang sudah enroll
 * (ditandai lewat {@link PesertaPunyaProdukKursus}). Satu baris per (enrollment, materi) --
 * dicari selalu lewat pasangan {@link #getPesertaPunyaProdukKursus()} +
 * {@link #getMateriKursus()}, bukan lewat id sendiri, sehingga baris ini di-upsert
 * (buat-jika-belum-ada / perbarui-jika-sudah-ada) oleh pemanggil, bukan pernah dibuat dobel
 * untuk satu (enrollment, materi) yang sama.
 * <p>
 * <b>Verifikasi integritas "selesai" (dicek terhadap pemanggil nyata di
 * webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp):</b> ada TIGA jalur berbeda yang bisa
 * menuliskan {@link #getSelesai()} = {@code true} untuk satu (enrollment, materi), dengan
 * tingkat verifikasi yang SANGAT TIMPANG antar tipe konten {@link MateriKursus}:
 * <ol>
 * <li><b>aksi {@code heartbeat_progress}</b> (untuk materi {@link MateriKursus#VIDEO}) --
 * menandai selesai HANYA setelah {@link #getPersentase()} tonton mencapai {@code >= 90}, dan
 * persentase itu dihitung server-side dari {@link #getDurasiDitonton()} dibagi
 * {@link MateriKursus#getDurasiMenit()} (durasi resmi tersimpan di server), BUKAN dari
 * persentase yang dikirim klien. Detik posisi putar yang dilaporkan klien juga di-clamp ke
 * {@code [0, durasiMenit*60 + toleransi]} sebelum dipakai. Jalur ini AMAN terhadap pemalsuan
 * parameter mentah.</li>
 * <li><b>aksi {@code selesai_kuis}</b> (untuk materi {@link MateriKursus#QUIZ}) -- menandai
 * selesai hanya jika {@link PercobaanKuisKursus#getLulus()} bernilai {@code true} (atau materi
 * tidak membatasi jumlah percobaan). Nilai kelulusan itu sendiri dihitung server-side dari
 * {@code recomputePercobaan()} berdasarkan skor per jawaban yang tersimpan di
 * {@link JawabanPercobaanKuisKursus} -- lihat catatan integritas skor kuis di Javadoc kelas
 * {@link JawabanPercobaanKuisKursus}. Jalur ini juga AMAN dari pemalsuan langsung.</li>
 * <li><b>aksi {@code tandai_selesai}</b> -- HANYA memvalidasi bahwa
 * {@code enrollmentId}+{@code materiKursusId} yang dikirim klien memang milik peserta yang
 * login dan enrollment berstatus TERBELI, LALU LANGSUNG men-set {@code selesai = true} TANPA
 * peduli {@link MateriKursus#getTipeKonten()} materi tersebut. Ini berarti aksi yang sama bisa
 * dipanggil langsung (mis. lewat HTTP POST manual, bukan lewat tombol UI resmi) untuk materi
 * bertipe {@link MateriKursus#QUIZ} atau {@link MateriKursus#TUGAS} TANPA peserta pernah lulus
 * kuisnya (lihat butir 2) ataupun pernah mengumpulkan tugasnya lewat
 * {@link PengumpulanTugasKursus} -- memalsukan penanda "selesai" persis seperti kekhawatiran
 * IDOR/broken-access-control klasik pada modul progress/completion. Karena
 * {@code cekDanTerbitkanSertifikat()} (penerbit {@link ais.database.model.kursus.SertifikatKursus})
 * HANYA memeriksa {@link #getSelesai()} tiap materi (bukan status kelulusan kuis atau status
 * pengumpulan tugas), peserta yang mengetahui pola ini berpotensi memperoleh sertifikat
 * kelulusan penuh tanpa benar-benar mengerjakan materi kuis/tugas apa pun. TEMUAN INI
 * DILAPORKAN sebagai task terpisah untuk perbaikan di {@code _kursus_service.jsp} (di luar
 * cakupan berkas model ini) -- lihat task hasil {@code spawn_task} terkait.</li>
 * </ol>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "progress_materi_kursus")
public class ProgressMateriKursus extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil id pengguna pembuat/pengubah terakhir baris ini. Field audit "shadow" -- diisi
	 * lewat {@link ais.database.hibernate.AuditTimestampInterceptor}, bukan bagian dari data
	 * bisnis progres materi.
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
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} secara otomatis
	 * sesaat sebelum UPDATE dieksekusi. Method audit "shadow", keharusan teknis infrastruktur,
	 * bukan logika bisnis progres.
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
	 * Representasi ringkas untuk log/debug: {@code kode + " - " + nama}. Karena
	 * {@link #getKode()} auto-generate saat pertama dipanggil dan {@link #getNama()}
	 * bergantung pada relasi yang mungkin belum ter-load, hasil {@code toString()} bisa berbeda
	 * tergantung state lazy-loading objek saat dipanggil.
	 *
	 * @return string gabungan kode dan nama progres.
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	private MateriKursus materiKursus;
	private Boolean selesai;
	private Date waktuSelesai;
	private Date waktuMulai;
	private Date waktuTerakhir;
	private Integer detikVideoTerakhir;
	private Integer durasiDitonton;
	private Integer persentase;
	private Integer jumlahAkses;

	/** Konstruktor default -- dibutuhkan Hibernate untuk instansiasi lewat reflection. */
	public ProgressMateriKursus() {
	}

	/**
	 * Mengambil id unik (primary key) baris progres ini, di-generate DB lewat
	 * strategi {@code IDENTITY}.
	 *
	 * @return id progres, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id progres. Dipanggil Hibernate saat memuat entity dari DB.
	 *
	 * @param id id progres.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas untuk baris progres ini (jarang dipakai -- pemanggil di
	 * {@code _kursus_service.jsp} tidak pernah mengisi kolom ini).
	 *
	 * @return keterangan progres, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/deskripsi bebas. Lihat {@link #getKeterangan()} untuk detail perilaku getter.
	 *
	 * @param keterangan nilai baru untuk keterangan/deskripsi bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil kode unik baris progres ini, auto-generate sekali via
	 * {@link ais.common.BarcodeCommon#generateCode()} pada panggilan pertama jika belum ada
	 * (lazy-init pada getter, bukan pada konstruktor) -- pola yang sama dipakai
	 * {@link PercobaanKuisKursus#getKode()} dan {@link PengumpulanTugasKursus#getKode()}.
	 * Dijaga {@code unique} di DB.
	 *
	 * @return kode unik progres, tidak pernah {@code null} setelah getter ini dipanggil.
	 */
	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	/**
	 * Mengisi kode unik baris ini. Lihat {@link #getKode()} untuk detail perilaku getter.
	 *
	 * @param kode nilai baru untuk kode unik baris ini.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil nama tampilan progres, dibentuk on-the-fly dari nama peserta ({@code enrollment
	 * -> pesertaKursus}) digabung nama materi -- BUKAN kolom yang murni disimpan apa adanya:
	 * setiap panggilan getter ini menimpa ulang field {@link #nama} bila kedua relasi
	 * ({@link #getMateriKursus()} dan {@link #getPesertaPunyaProdukKursus()}) berhasil di-load,
	 * sehingga nilai yang di-set manual lewat {@link #setNama(String)} bisa "hilang" tertimpa
	 * begitu getter dipanggil ulang setelah relasi ter-load. Pola yang sama dipakai
	 * {@link PercobaanKuisKursus#getNama()} dan {@link PengumpulanTugasKursus#getNama()}.
	 *
	 * @return nama gabungan "nama peserta - nama materi", atau nilai {@link #nama} apa adanya
	 *         (mungkin {@code null}) jika salah satu relasi belum bisa di-resolve.
	 */
	public String getNama() {
		if (getMateriKursus() != null && getPesertaPunyaProdukKursus() != null
				&& getPesertaPunyaProdukKursus().getPesertaKursus() != null) {
			nama = getPesertaPunyaProdukKursus().getPesertaKursus().getNama() + " - " + materiKursus.getNama();
		}
		return nama;
	}

	/**
	 * Mengisi nama tampilan. Lihat {@link #getNama()} untuk detail perilaku getter.
	 *
	 * @param nama nilai baru untuk nama tampilan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil enrollment ({@link PesertaPunyaProdukKursus}) pemilik progres ini -- bersama
	 * {@link #getMateriKursus()} membentuk kunci logis "satu baris per (enrollment, materi)"
	 * yang dipakai semua query pencarian progres di {@code _kursus_service.jsp}.
	 *
	 * @return enrollment pemilik progres; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_punya_produk_kursus", nullable = false)
	public PesertaPunyaProdukKursus getPesertaPunyaProdukKursus() {
		pesertaPunyaProdukKursus = check(pesertaPunyaProdukKursus);
		return pesertaPunyaProdukKursus;
	}

	/**
	 * Mengisi enrollment pemilik progres ini. Tidak ada verifikasi kepemilikan di setter ini --
	 * pengecekan "enrollment ini benar-benar milik peserta yang login" ditegakkan di pemanggil
	 * (aksi tandai_selesai/heartbeat_progress/dsb pada {@code _kursus_service.jsp}) sebelum
	 * setter ini dipanggil.
	 *
	 * @param pesertaPunyaProdukKursus enrollment pemilik progres.
	 */
	public void setPesertaPunyaProdukKursus(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
	}

	/**
	 * Mengambil {@link MateriKursus} yang progres penyelesaiannya dilacak baris ini.
	 *
	 * @return materi terkait; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "materi_kursus", nullable = false)
	public MateriKursus getMateriKursus() {
		materiKursus = check(materiKursus);
		return materiKursus;
	}

	/**
	 * Mengisi materi kursus terkait. Lihat {@link #getMateriKursus()} untuk detail perilaku getter.
	 *
	 * @param materiKursus nilai baru untuk materi kursus terkait.
	 */
	public void setMateriKursus(MateriKursus materiKursus) {
		this.materiKursus = materiKursus;
	}

	/**
	 * Mengambil penanda apakah peserta sudah menyelesaikan materi ini. Field paling kritis dari
	 * entity ini: dipakai sebagai satu-satunya syarat gerbang penerbitan sertifikat
	 * ({@code cekDanTerbitkanSertifikat()} mengecek {@code getSelesai()} setiap materi milik
	 * kursus). LIHAT catatan integritas lengkap di Javadoc kelas ini mengenai tiga jalur
	 * berbeda (heartbeat video, kelulusan kuis, dan aksi {@code tandai_selesai} langsung) yang
	 * bisa menuliskan field ini -- jalur ketiga TIDAK memverifikasi tipe konten materi maupun
	 * bukti pengerjaan nyata.
	 *
	 * @return {@code true} jika sudah selesai; default {@code false} bila belum diisi.
	 */
	public Boolean getSelesai() {
		return selesai == null ? false : selesai;
	}

	/**
	 * Mengisi penanda selesai. Setter murni (tidak ada validasi urutan/prasyarat di sini) --
	 * lihat catatan integritas di Javadoc kelas untuk pembahasan risiko pemanggil yang tidak
	 * berhati-hati.
	 *
	 * @param selesai penanda selesai baru.
	 */
	public void setSelesai(Boolean selesai) {
		this.selesai = selesai;
	}

	/**
	 * Mengambil waktu materi ini ditandai selesai.
	 *
	 * @return waktu selesai, atau {@code null} jika belum ditandai selesai (atau ditandai belum
	 *         selesai lagi, mis. setelah {@code hapus_pengumpulan_tugas} membatalkan pengumpulan
	 *         tugas yang sebelumnya menandai materi ini selesai).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuSelesai() {
		return waktuSelesai;
	}

	/**
	 * Mengisi waktu selesai. Lihat {@link #getWaktuSelesai()} untuk detail perilaku getter.
	 *
	 * @param waktuSelesai nilai baru untuk waktu selesai.
	 */
	public void setWaktuSelesai(Date waktuSelesai) {
		this.waktuSelesai = waktuSelesai;
	}

	/*
	 * Kolom BARU pada tabel LAMA yang sudah ber-@Audited (progress_materi_kursus).
	 * Sesuai kesepakatan sebelumnya utk MateriKursus.ujian dkk, kolom-kolom ini
	 * SENGAJA murni diserahkan ke Hibernate hbm2ddl=update (tanpa self-heal manual).
	 * Konsekuensi: new_audit.progress_materi_kursus__audit TIDAK otomatis dapat kolom
	 * ini -- simpan ProgressMateriKursus bisa gagal sampai dijalankan manual satu kali:
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN waktumulai timestamp;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN waktuterakhir timestamp;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN detikvideoterakhir integer;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN durasiditonton integer;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN persentase integer;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN jumlahakses integer;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN waktumulai timestamp;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN waktuterakhir timestamp;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN detikvideoterakhir integer;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN durasiditonton integer;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN persentase integer;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN jumlahakses integer;
	 */
	/**
	 * Mengambil waktu pertama kali peserta membuka/mengakses materi ini. Diisi sekali (hanya
	 * jika sebelumnya {@code null}) oleh aksi {@code mulai_lihat_materi}/{@code heartbeat_progress}
	 * pada {@code _kursus_service.jsp}, tidak pernah ditimpa ulang setelah terisi.
	 *
	 * @return waktu pertama akses, atau {@code null} jika belum pernah diakses.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuMulai() {
		return waktuMulai;
	}

	/**
	 * Mengisi waktu mulai. Lihat {@link #getWaktuMulai()} untuk detail perilaku getter.
	 *
	 * @param waktuMulai nilai baru untuk waktu mulai.
	 */
	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengambil waktu akses terakhir kali peserta membuka materi ini -- diperbarui setiap
	 * pemanggilan aksi {@code mulai_lihat_materi}/{@code heartbeat_progress}, berbeda dari
	 * {@link #getWaktuMulai()} yang hanya diisi sekali.
	 *
	 * @return waktu akses terakhir, atau {@code null} jika belum pernah diakses.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuTerakhir() {
		return waktuTerakhir;
	}

	/**
	 * Mengisi waktu akses terakhir. Lihat {@link #getWaktuTerakhir()} untuk detail perilaku getter.
	 *
	 * @param waktuTerakhir nilai baru untuk waktu akses terakhir.
	 */
	public void setWaktuTerakhir(Date waktuTerakhir) {
		this.waktuTerakhir = waktuTerakhir;
	}

	/**
	 * Mengambil posisi detik terakhir video yang dilaporkan (untuk keperluan "lanjutkan
	 * menonton"). Nilai yang disimpan di sini sudah di-clamp server-side terhadap
	 * {@link MateriKursus#getDurasiMenit()} oleh pemanggil sebelum di-set -- lihat catatan
	 * integritas di Javadoc kelas.
	 *
	 * @return detik posisi putar terakhir, default 0 bila belum diisi.
	 */
	public Integer getDetikVideoTerakhir() {
		return detikVideoTerakhir == null ? 0 : detikVideoTerakhir;
	}

	/**
	 * Mengisi posisi detik video terakhir dilaporkan. Lihat {@link #getDetikVideoTerakhir()} untuk detail perilaku getter.
	 *
	 * @param detikVideoTerakhir nilai baru untuk posisi detik video terakhir dilaporkan.
	 */
	public void setDetikVideoTerakhir(Integer detikVideoTerakhir) {
		this.detikVideoTerakhir = detikVideoTerakhir;
	}

	/**
	 * Mengambil total durasi tontonan tertinggi yang pernah tercatat (dalam detik) -- pemanggil
	 * HANYA memperbarui nilai ini bila detik yang dilaporkan lebih besar dari nilai tersimpan
	 * ({@code if (detikDilaporkan > prog.getDurasiDitonton()) prog.setDurasiDitonton(...)}), jadi
	 * field ini adalah "rekor tertinggi", bukan posisi putar saat ini (bandingkan dengan
	 * {@link #getDetikVideoTerakhir()}). Dipakai sebagai basis perhitungan
	 * {@link #getPersentase()} dan durasi belajar total di sertifikat.
	 *
	 * @return durasi tontonan tertinggi dalam detik, default 0 bila belum diisi.
	 */
	public Integer getDurasiDitonton() {
		return durasiDitonton == null ? 0 : durasiDitonton;
	}

	/**
	 * Mengisi total durasi tontonan tertinggi (detik). Lihat {@link #getDurasiDitonton()} untuk detail perilaku getter.
	 *
	 * @param durasiDitonton nilai baru untuk total durasi tontonan tertinggi (detik).
	 */
	public void setDurasiDitonton(Integer durasiDitonton) {
		this.durasiDitonton = durasiDitonton;
	}

	/**
	 * Mengambil persentase progres tontonan/pembelajaran (0-100), dijaga tetap dalam rentang
	 * valid di getter ini sendiri (di-clamp {@code [0, 100]}) sebagai pertahanan lapis kedua di
	 * atas clamp yang sudah dilakukan pemanggil saat menghitung persentase dari
	 * {@link #getDurasiDitonton()} vs {@link MateriKursus#getDurasiMenit()}. Nilai ini yang
	 * dijadikan ambang {@code >= 90} untuk menandai materi video otomatis selesai.
	 *
	 * @return persentase progres, sudah di-clamp ke rentang [0, 100]; default 0 bila belum diisi.
	 */
	public Integer getPersentase() {
		if (persentase == null) return 0;
		if (persentase > 100) return 100;
		if (persentase < 0) return 0;
		return persentase;
	}

	/**
	 * Mengisi persentase progres mentah (belum di-clamp -- clamping terjadi di
	 * {@link #getPersentase()}, bukan di setter ini).
	 *
	 * @param persentase persentase progres baru.
	 */
	public void setPersentase(Integer persentase) {
		this.persentase = persentase;
	}

	/**
	 * Mengambil jumlah berapa kali peserta membuka/mengakses materi ini, bertambah satu setiap
	 * kali aksi {@code mulai_lihat_materi} dipanggil.
	 *
	 * @return jumlah akses, default 0 bila belum diisi.
	 */
	public Integer getJumlahAkses() {
		return jumlahAkses == null ? 0 : jumlahAkses;
	}

	/**
	 * Mengisi jumlah akses/pembukaan materi. Lihat {@link #getJumlahAkses()} untuk detail perilaku getter.
	 *
	 * @param jumlahAkses nilai baru untuk jumlah akses/pembukaan materi.
	 */
	public void setJumlahAkses(Integer jumlahAkses) {
		this.jumlahAkses = jumlahAkses;
	}

}
