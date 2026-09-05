package ais.database.model.recruitment;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.gelombang_pendaftaran_pegawai}: gelombang
 * (periode) pendaftaran rekrutmen calon pegawai/dosen/guru non-mahasiswa di modul {@code
 * ais.database.model.recruitment}. Satu baris mewakili satu "batch" lowongan yang dibuka pada
 * rentang tanggal tertentu ({@link #getMulai()} s/d {@link #getSampai()}), dengan teks lowongan
 * (kualifikasi, tanggung jawab, fasilitas, disclaimer) yang ditampilkan ke publik pada halaman
 * pendaftaran, serta parameter tampilan form tambahan saat registrasi/login calon pegawai.
 *
 * <p><b>Relasi utama:</b></p>
 * <ul>
 * <li>{@link #getSatuanKerja()} — unit kerja/instansi pemilik gelombang (opsional; scoping
 * multi-tenant bila diisi). {@code nullable = true} pada kolom FK berarti gelombang bisa dibuat
 * tanpa satuan kerja spesifik (berlaku lintas satker), jadi kode pemanggil yang menyaring data per
 * satuan kerja wajib menangani kasus {@code null} secara eksplisit, bukan mengasumsikan selalu
 * terisi.</li>
 * <li>{@link #getVerifikasiKelengkapanCalonPegawais()} — relasi many-to-many ke {@link
 * VerifikasiKelengkapanCalonPegawai} lewat tabel pivot {@code gelombang_punya_verifikasi_pegawai};
 * menentukan daftar item verifikasi kelengkapan berkas yang wajib dipenuhi calon pegawai yang
 * mendaftar pada gelombang ini.</li>
 * <li>Entity anak yang mereferensikan gelombang ini (tidak dideklarasikan di sini, hanya sisi
 * pemilik FK): {@link UjianPegawai}, {@link KelompokPendaftaranPegawai}, {@link RuangPegawai}, dan
 * {@link GelombangPendaftaranPegawaiPunyaParameterVerifikasiCalonPegawai}.</li>
 * </ul>
 *
 * <p><b>Konstanta {@link #PEGAWAI}, {@link #DOSEN}, {@link #GURU}</b> adalah nilai kanonik untuk
 * field {@link #getJenis()} yang membedakan jalur/formulir pendaftaran (pegawai umum & tendik,
 * dosen, atau guru); dipakai sebagai pembanding string biasa, bukan enum JPA, sehingga tidak ada
 * validasi tingkat kolom yang mencegah nilai lain tersimpan di database.</p>
 *
 * <p><b>Banyak getter teks lowongan ({@link #getFungsiKerja()}, {@link #getJurusan()}, {@link
 * #getLulusan()}, {@link #getPersyaratan()}, {@link #getTanggungJawab()}, {@link #getDisclaimer()},
 * {@link #getPengalaman()}, {@link #getFasilitas()}) mengembalikan teks placeholder Bahasa
 * Indonesia yang cukup panjang bila field bersangkutan masih {@code null}</b> — bukan string
 * kosong. Ini pola "default informatif": halaman lowongan publik tetap menampilkan contoh teks
 * yang masuk akal alih-alih kosong, tapi efek sampingnya method-method ini tidak murni (nilai
 * kembalian bergantung state field, bukan konstanta), dan pemanggil yang ingin tahu "apakah admin
 * sudah mengisi field ini" tidak bisa membedakannya lewat getter — harus memeriksa field mentah
 * atau DB langsung.</p>
 *
 * <p>Diaudit oleh Hibernate Envers ({@code @Audited}); setiap INSERT/UPDATE/DELETE pada baris
 * entity ini tercatat ke tabel revisi historis terpisah. Anotasi {@code dynamicInsert}/{@code
 * dynamicUpdate} membuat Hibernate hanya menyertakan kolom yang benar-benar berubah pada statement
 * SQL, mengurangi lebar statement untuk entity dengan banyak kolom teks seperti ini.</p>
 *
 * @see GeneralValueObject
 * @see UjianPegawai
 * @see KelompokPendaftaranPegawai
 * @see VerifikasiKelengkapanCalonPegawai
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "gelombang_pendaftaran_pegawai", schema = "public")
public class GelombangPendaftaranPegawai extends GeneralValueObject {

	/**
	 * Nilai kanonik {@link #getJenis()} untuk gelombang pendaftaran pegawai umum dan tenaga
	 * kependidikan (tendik). Juga dipakai sebagai fallback default oleh {@link #getJenis()} bila
	 * field {@code jenis} belum diisi atau kosong.
	 */
	public static final String PEGAWAI = "Pegawai Umum dan Tendik";
	/**
	 * Nilai kanonik {@link #getJenis()} untuk gelombang pendaftaran khusus dosen.
	 */
	public static final String DOSEN = "Dosen";
	/**
	 * Nilai kanonik {@link #getJenis()} untuk gelombang pendaftaran khusus guru.
	 */
	public static final String GURU = "Guru";

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} lintas deployment;
	 * hanya berubah bila struktur field yang memengaruhi serialisasi berubah secara tidak
	 * kompatibel.
	 */
	private static final long serialVersionUID = -4835727221706810019L;
	/**
	 * Primary key baris ini pada tabel {@code gelombang_pendaftaran_pegawai}, dihasilkan otomatis
	 * oleh database ({@code IDENTITY}). Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna (username/nama akun) yang terakhir membuat/mengubah baris ini. Field
	 * audit yang tidak dipetakan sebagai kolom JPA ({@code @Column} tidak ada) — persistensinya
	 * bergantung mekanisme lain (mis. mapping XML lama/kolom implisit), bukan anotasi di kelas ini.
	 * Lihat {@link #getOleh()}/{@link #setOleh(String)}.
	 */
	private String oleh;
	/**
	 * Identitas (ID) pengguna yang terakhir membuat/mengubah baris ini, pasangan dari {@link
	 * #oleh}. Lihat {@link #getOlehId()}/{@link #setOlehId(String)}.
	 */
	private String olehId;

	/**
	 * Mengambil ID pengguna (bukan nama tampilan) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return ID pengguna audit, atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset ID pengguna audit ({@link #olehId}). Menolak (no-op) nilai {@code null} atau string
	 * kosong/berisi hanya whitespace — begitu field ini pernah terisi nilai valid, pemanggilan
	 * berikutnya dengan nilai kosong tidak akan menghapusnya. Perilaku ini konsisten dengan pola
	 * "audit shadow field" yang berulang di banyak entity AIS: guard ini mencegah proses lain
	 * (mis. save parsial) secara tidak sengaja mengosongkan jejak siapa yang terakhir mengubah data.
	 *
	 * @param olehId ID pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengeset nama tampilan pengguna audit ({@link #oleh}). Guard yang sama seperti {@link
	 * #setOlehId(String)}: nilai {@code null}/kosong diabaikan agar jejak "oleh" tidak pernah
	 * tertimpa kosong.
	 *
	 * @param oleh nama pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama tampilan pengguna yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nama pengguna audit, atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat sebelum
	 * statement UPDATE dikirim untuk baris ini. Mendelegasikan ke {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang bertanggung jawab
	 * memperbarui field timestamp audit (termasuk {@link #tanggal_dirubah}) secara konsisten
	 * lintas seluruh entity yang memakai pola ini. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Timestamp terakhir baris ini diubah. Diinisialisasi ke waktu saat ini ({@link
	 * ais.ui.util.WaktuUtil#getDate()}) pada saat object dikonstruksi (bukan pada saat pertama kali
	 * disimpan), lalu diperbarui lagi oleh {@link #onUpdate()} setiap kali UPDATE terjadi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengeset timestamp perubahan terakhir secara manual. Umumnya tidak perlu dipanggil dari kode
	 * aplikasi karena {@link #onUpdate()} sudah menjaga nilai ini otomatis pada setiap UPDATE.
	 *
	 * @param tanggal_dirubah timestamp baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null} karena field diinisialisasi
	 * saat konstruksi object.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Tanggal mulai gelombang pendaftaran dibuka. Lihat {@link #getMulai()}.
	 */
	private Date mulai;
	/**
	 * Nama/judul gelombang pendaftaran, ditampilkan ke publik. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Tanggal gelombang pendaftaran ditutup. Lihat {@link #getSampai()}.
	 */
	private Date sampai;
	/**
	 * Teks informasi bebas (HTML/rich text) tentang gelombang ini yang ditampilkan ke calon
	 * pendaftar. Lihat {@link #getInformasi()}.
	 */
	private String informasi;
	/**
	 * Penanda apakah gelombang ini aktif (ditampilkan/dibuka untuk pendaftaran). Lihat {@link
	 * #getAktif()} — default {@code true} bila belum pernah diset.
	 */
	private Boolean aktif;
	/**
	 * Catatan/keterangan internal tentang gelombang ini. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;
	/**
	 * Penanda apakah form parameter tambahan ditampilkan saat proses registrasi awal calon
	 * pegawai. Lihat {@link #getTampilFormTambahanSaatRegistrasi()} — default {@code false}.
	 */
	private Boolean tampilFormTambahanSaatRegistrasi;
	/**
	 * Penanda apakah form parameter tambahan ditampilkan saat calon pegawai login (setelah
	 * registrasi awal). Lihat {@link #getTampilFormTambahanSaatLoginCalonPegawai()} — default
	 * {@code true}.
	 */
	private Boolean tampilFormTambahanSaatLoginCalonPegawai;
	/**
	 * Jenis/kategori gelombang: salah satu dari {@link #PEGAWAI}, {@link #DOSEN}, {@link #GURU},
	 * atau nilai bebas lain (tidak divalidasi terhadap enum). Lihat {@link #getJenis()}.
	 */
	private String jenis;

	/**
	 * Deskripsi fungsi kerja/jabatan pada lowongan gelombang ini. Lihat {@link #getFungsiKerja()}.
	 */
	private String fungsiKerja;
//	private String jenjangKarir;
	/**
	 * Syarat pengalaman kerja minimal untuk lowongan gelombang ini. Lihat {@link
	 * #getPengalaman()}.
	 */
	private String pengalaman;
	/**
	 * Deskripsi fasilitas yang ditawarkan pada lowongan gelombang ini. Lihat {@link
	 * #getFasilitas()}.
	 */
	private String fasilitas;
	/**
	 * Syarat jurusan pendidikan untuk lowongan gelombang ini. Lihat {@link #getJurusan()}.
	 */
	private String jurusan;
	/**
	 * Syarat jenjang lulusan pendidikan untuk lowongan gelombang ini (mis. "Sarjana/S1"). Lihat
	 * {@link #getLulusan()}.
	 */
	private String lulusan;
	/**
	 * Teks persyaratan lengkap pelamar untuk lowongan gelombang ini. Lihat {@link
	 * #getPersyaratan()}.
	 */
	private String persyaratan;
	/**
	 * Teks tanggung jawab/uraian tugas jabatan pada lowongan gelombang ini. Lihat {@link
	 * #getTanggungJawab()}.
	 */
	private String tanggungJawab;
	/**
	 * Teks disclaimer yang ditampilkan pada lowongan gelombang ini (mis. peringatan anti-pungutan
	 * biaya). Lihat {@link #getDisclaimer()}.
	 */
	private String disclaimer;

	/**
	 * Himpunan item verifikasi kelengkapan berkas yang wajib dipenuhi calon pegawai pada gelombang
	 * ini, dimuat lewat relasi many-to-many {@code gelombang_punya_verifikasi_pegawai}. Diinisialisasi
	 * ke {@link TreeSet} kosong (tidak pernah {@code null}) supaya iterasi/penambahan aman tanpa
	 * pengecekan null di pemanggil; urutan iterasi mengikuti {@link Comparable} pada {@link
	 * VerifikasiKelengkapanCalonPegawai}. Lihat {@link #getVerifikasiKelengkapanCalonPegawais()}.
	 */
	private Set<VerifikasiKelengkapanCalonPegawai> verifikasiKelengkapanCalonPegawais = new TreeSet<VerifikasiKelengkapanCalonPegawai>();
	/**
	 * Unit kerja/instansi pemilik gelombang ini (opsional). Lihat {@link #getSatuanKerja()}.
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Mengambil himpunan item verifikasi kelengkapan berkas yang wajib dipenuhi pada gelombang
	 * ini. Relasi many-to-many dengan {@code cascade = MERGE} lewat tabel pivot {@code
	 * gelombang_punya_verifikasi_pegawai} (kolom {@code gelombang}/{@code verifikasi}); tidak ada
	 * {@code CascadeType.PERSIST} maupun {@code REMOVE}, jadi entity {@link
	 * VerifikasiKelengkapanCalonPegawai} baru harus sudah dipersist terpisah sebelum ditambahkan ke
	 * set ini, dan menghapus dari set ini hanya melepas baris pivot — tidak menghapus entity
	 * verifikasi itu sendiri.
	 *
	 * @return set (tidak pernah {@code null}) item verifikasi kelengkapan berkas untuk gelombang
	 * ini.
	 */
	@ManyToMany(targetEntity = VerifikasiKelengkapanCalonPegawai.class, cascade = { CascadeType.MERGE })
	@JoinTable(name = "gelombang_punya_verifikasi_pegawai", joinColumns = @JoinColumn(name = "gelombang"), inverseJoinColumns = @JoinColumn(name = "verifikasi"), schema = "public")
	public Set<VerifikasiKelengkapanCalonPegawai> getVerifikasiKelengkapanCalonPegawais() {
		return verifikasiKelengkapanCalonPegawais;
	}

	/**
	 * Mengganti seluruh himpunan item verifikasi kelengkapan berkas untuk gelombang ini.
	 *
	 * @param verifikasiKelengkapanCalonPegawais set pengganti; boleh {@code null} secara teknis,
	 * tetapi pemanggil sebaiknya selalu menyediakan set (kosong bila perlu) agar getter tetap
	 * konsisten dengan kontrak "tidak pernah null" pada inisialisasi field.
	 */
	public void setVerifikasiKelengkapanCalonPegawais(
			Set<VerifikasiKelengkapanCalonPegawai> verifikasiKelengkapanCalonPegawais) {
		this.verifikasiKelengkapanCalonPegawais = verifikasiKelengkapanCalonPegawais;
	}

	/**
	 * Konstruktor kosong yang disyaratkan Hibernate/JPA untuk instansiasi entity lewat refleksi
	 * (mis. saat memuat baris dari database). Tidak untuk dipanggil langsung oleh kode aplikasi
	 * yang ingin membuat gelombang baru dengan data lengkap — gunakan {@link #GelombangPendaftaranPegawai(long,
	 * Date, String, Date)} atau setter individual.
	 */
	public GelombangPendaftaranPegawai() {
	}

	/**
	 * Konstruktor kenyamanan untuk membuat instance dengan field wajib (non-null di kolom
	 * database) langsung terisi: {@link #id}, {@link #mulai}, {@link #nama}, {@link #sampai}.
	 * Field lain (informasi lowongan, flag tampilan, satuan kerja, dsb.) tetap harus diset
	 * terpisah lewat setter masing-masing sebelum entity dipersist bila dibutuhkan.
	 *
	 * @param id primary key; diteruskan apa adanya ke {@link #setId(Long)} (autoboxing
	 * {@code long} → {@link Long}) — perlu diperhatikan bahwa kolom {@code id} sebenarnya
	 * {@code insertable = false} (di-generate database), jadi nilai yang diset di sini akan
	 * diabaikan Hibernate pada saat INSERT dan hanya relevan bila entity dipakai sebagai
	 * representasi in-memory sebelum benar-benar dipersist.
	 * @param mulai tanggal mulai gelombang.
	 * @param nama nama/judul gelombang.
	 * @param sampai tanggal akhir gelombang.
	 */
	public GelombangPendaftaranPegawai(long id, Date mulai, String nama, Date sampai) {
		this.id = id;
		this.mulai = mulai;
		this.nama = nama;
		this.sampai = sampai;
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID gelombang, atau {@code null} untuk instance transient yang belum dipersist.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengeset ID secara manual. Kolom {@code id} dipetakan {@code insertable = false} (nilai
	 * dihasilkan database via {@code IDENTITY}), sehingga pemanggilan ini tidak memengaruhi
	 * statement INSERT — hanya berguna untuk mengisi representasi in-memory (mis. saat membangun
	 * object hasil query manual) atau untuk operasi UPDATE/DELETE berbasis ID yang sudah diketahui.
	 *
	 * @param id ID yang akan diset pada object ini.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil tanggal mulai gelombang pendaftaran dibuka.
	 *
	 * @return tanggal mulai; kolom wajib diisi ({@code nullable = false}) pada database, tapi tidak
	 * ada validasi null di level Java sebelum persist.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "mulai", nullable = false, length = 13)
	public Date getMulai() {
		return this.mulai;
	}

	/**
	 * Mengeset {@link #mulai}.
	 *
	 * @param mulai nilai baru untuk {@link #mulai}.
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengambil nama/judul gelombang pendaftaran.
	 *
	 * @return nama gelombang, ditampilkan ke publik pada halaman lowongan.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Mengeset {@link #nama}.
	 *
	 * @param nama nilai baru untuk {@link #nama}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil tanggal gelombang pendaftaran ditutup.
	 *
	 * @return tanggal akhir; kolom wajib diisi ({@code nullable = false}) pada database.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "sampai", nullable = false, length = 13)
	public Date getSampai() {
		return this.sampai;
	}

	/**
	 * Mengeset {@link #sampai}.
	 *
	 * @param sampai nilai baru untuk {@link #sampai}.
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengambil teks informasi bebas (dipetakan sebagai kolom {@code text}, tanpa batas panjang
	 * praktis) tentang gelombang ini.
	 *
	 * @return teks informasi, atau {@code null} bila belum diisi.
	 */
	@Column(name = "informasi", columnDefinition = "text")
	public String getInformasi() {
		return this.informasi;
	}

	/**
	 * Mengeset {@link #informasi}.
	 *
	 * @param informasi nilai baru untuk {@link #informasi}.
	 */
	public void setInformasi(String informasi) {
		this.informasi = informasi;
	}

	/**
	 * Mengambil status aktif gelombang ini. Tidak dipetakan dengan {@code @Column} eksplisit
	 * (kolom disimpulkan dari nama getter oleh Hibernate secara implisit).
	 *
	 * @return {@code true} bila field {@link #aktif} belum pernah diset ({@code null}) — gelombang
	 * baru dianggap aktif secara default — atau nilai eksplisit yang tersimpan.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengeset {@link #aktif}.
	 *
	 * @param aktif nilai baru untuk {@link #aktif}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil catatan/keterangan internal gelombang ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi (tidak ada default informatif seperti
	 * getter teks lowongan lain di kelas ini).
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengeset {@link #keterangan}.
	 *
	 * @param keterangan nilai baru untuk {@link #keterangan}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil (dan bila perlu menormalkan) penanda apakah form parameter tambahan ditampilkan
	 * saat proses registrasi awal calon pegawai. <b>Efek samping:</b> bila field masih {@code
	 * null}, method ini menulis {@code false} ke field sebelum mengembalikannya — bukan getter
	 * murni, melainkan pola "normalisasi malas" (lazy default) yang juga mengubah state object di
	 * memori (dan akan ikut tersimpan bila entity kemudian di-flush).
	 *
	 * @return {@code false} sebagai default bila belum pernah diset, atau nilai eksplisit yang
	 * tersimpan.
	 */
	public Boolean getTampilFormTambahanSaatRegistrasi() {
		if (tampilFormTambahanSaatRegistrasi == null) {
			tampilFormTambahanSaatRegistrasi = false;
		}
		return tampilFormTambahanSaatRegistrasi;
	}

	/**
	 * Mengeset {@link #tampilFormTambahanSaatRegistrasi}.
	 *
	 * @param tampilFormTambahanSaatRegistrasi nilai baru untuk {@link #tampilFormTambahanSaatRegistrasi}.
	 */
	public void setTampilFormTambahanSaatRegistrasi(Boolean tampilFormTambahanSaatRegistrasi) {
		this.tampilFormTambahanSaatRegistrasi = tampilFormTambahanSaatRegistrasi;
	}

	/**
	 * Mengambil (dan bila perlu menormalkan) penanda apakah form parameter tambahan ditampilkan
	 * saat calon pegawai login. Sama seperti {@link #getTampilFormTambahanSaatRegistrasi()},
	 * getter ini menulis default ke field bila masih {@code null} — tetapi defaultnya berlawanan
	 * ({@code true}), sehingga dua flag "tampil form tambahan" ini punya polaritas default yang
	 * berbeda (satu default mati, satu default hidup) meski namanya paralel; perlu diperhatikan
	 * saat membaca/mengubah keduanya bersamaan agar tidak tertukar asumsi.
	 *
	 * @return {@code true} sebagai default bila belum pernah diset, atau nilai eksplisit yang
	 * tersimpan.
	 */
	public Boolean getTampilFormTambahanSaatLoginCalonPegawai() {
		if (tampilFormTambahanSaatLoginCalonPegawai == null) {
			tampilFormTambahanSaatLoginCalonPegawai = true;
		}
		return tampilFormTambahanSaatLoginCalonPegawai;
	}

	/**
	 * Mengeset {@link #tampilFormTambahanSaatLoginCalonPegawai}.
	 *
	 * @param tampilFormTambahanSaatLoginCalonPegawai nilai baru untuk {@link #tampilFormTambahanSaatLoginCalonPegawai}.
	 */
	public void setTampilFormTambahanSaatLoginCalonPegawai(Boolean tampilFormTambahanSaatLoginCalonPegawai) {
		this.tampilFormTambahanSaatLoginCalonPegawai = tampilFormTambahanSaatLoginCalonPegawai;
	}

	/**
	 * Mengambil jenis/kategori gelombang pendaftaran.
	 *
	 * @return nilai field {@link #jenis} apa adanya bila terisi non-kosong; jika {@code null} atau
	 * string kosong, mengembalikan {@link #PEGAWAI} sebagai default. Nilai yang dikembalikan tidak
	 * divalidasi terhadap {@link #PEGAWAI}/{@link #DOSEN}/{@link #GURU} — kode pemanggil yang
	 * membedakan alur berdasarkan jenis (mis. formulir pendaftaran dosen vs pegawai umum) harus
	 * menangani kemungkinan nilai string bebas lain yang tidak dikenal.
	 */
	public String getJenis() {
		return jenis == null || jenis.isEmpty() ? PEGAWAI : jenis;
	}

	/**
	 * Mengeset {@link #jenis}.
	 *
	 * @param jenis nilai baru untuk {@link #jenis}.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengambil unit kerja/instansi pemilik gelombang ini, dengan resolusi proxy lazy lewat {@link
	 * GeneralValueObject#check(Object)}. Relasi {@code @ManyToOne} dengan {@code fetch = LAZY} dan
	 * kolom FK {@code satuan_kerja} yang {@code nullable = true} — gelombang tanpa satuan kerja
	 * (nilai {@code null}) adalah kondisi valid dan didukung, bukan data yang belum lengkap.
	 * Efeknya, kode yang melakukan scoping data per satuan kerja (mis. filter dashboard/laporan
	 * agar admin satu unit kerja hanya melihat gelombang miliknya) harus secara eksplisit
	 * memutuskan bagaimana memperlakukan gelombang dengan {@code satuanKerja == null} — apakah
	 * dianggap "milik semua satker" (visible ke semua) atau "tidak ada satker" (disembunyikan);
	 * kelas ini sendiri tidak memaksakan kebijakan tersebut, sehingga risiko kebocoran data lintas
	 * satker bergantung sepenuhnya pada disiplin query di lapisan action/service pemanggil.
	 *
	 * @return {@link SatuanKerja} pemilik gelombang setelah resolusi proxy lazy, atau {@code null}
	 * bila gelombang tidak terikat satuan kerja tertentu.
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengeset {@link #satuanKerja}.
	 *
	 * @param satuanKerja nilai baru untuk {@link #satuanKerja}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengambil deskripsi fungsi kerja/jabatan pada lowongan gelombang ini (kolom {@code text}).
	 *
	 * @return teks fungsi kerja yang tersimpan, atau placeholder {@code "Sebagai .."} bila field
	 * masih {@code null} — placeholder ini murni untuk tampilan, bukan indikasi data valid.
	 */
	@Column(columnDefinition = "text")
	public String getFungsiKerja() {
		return fungsiKerja == null ? "Sebagai .." : fungsiKerja;
	}

	/**
	 * Mengeset {@link #fungsiKerja}.
	 *
	 * @param fungsiKerja nilai baru untuk {@link #fungsiKerja}.
	 */
	public void setFungsiKerja(String fungsiKerja) {
		this.fungsiKerja = fungsiKerja;
	}

//	@Column(columnDefinition = "text")
//	public String getJenjangKarir() {
//		return jenjangKarir == null ? "Diutamakan berpengalaan di bidang nya" : jenjangKarir;
//	}
//
//	public void setJenjangKarir(String jenjangKarir) {
//		this.jenjangKarir = jenjangKarir;
//	}

	/**
	 * Mengambil syarat jurusan pendidikan pada lowongan gelombang ini (kolom {@code text}).
	 *
	 * @return teks jurusan yang tersimpan, atau placeholder {@code "Semua Pendidikan"} bila field
	 * masih {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getJurusan() {
		return jurusan == null ? "Semua Pendidikan" : jurusan;
	}

	/**
	 * Mengeset {@link #jurusan}.
	 *
	 * @param jurusan nilai baru untuk {@link #jurusan}.
	 */
	public void setJurusan(String jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengambil syarat jenjang lulusan pendidikan pada lowongan gelombang ini (kolom {@code
	 * text}).
	 *
	 * @return teks lulusan yang tersimpan, atau placeholder {@code "Sarjana/S1"} bila field masih
	 * {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getLulusan() {
		return lulusan == null ? "Sarjana/S1" : lulusan;
	}

	/**
	 * Mengeset {@link #lulusan}.
	 *
	 * @param lulusan nilai baru untuk {@link #lulusan}.
	 */
	public void setLulusan(String lulusan) {
		this.lulusan = lulusan;
	}

	/**
	 * Mengambil teks persyaratan lengkap pelamar pada lowongan gelombang ini (kolom {@code text}).
	 * Ini salah satu getter dengan placeholder default terpanjang di kelas ini: bila field {@link
	 * #persyaratan} masih {@code null}, method mengembalikan teks contoh persyaratan multi-baris
	 * (rentang usia, jenjang pendidikan minimal beserta IPK, pengalaman kerja, kesediaan
	 * penempatan/perjalanan dinas, serta sikap kerja yang diharapkan) yang dipisahkan {@code
	 * "\r\n"} literal (CRLF eksplisit di source, bukan bergantung platform line separator) —
	 * penting diperhatikan bila teks ini dibandingkan atau diproses ulang oleh kode lain yang
	 * mengasumsikan pemisah baris tertentu. Placeholder ini murni contoh tampilan untuk
	 * memudahkan admin melihat format yang diharapkan sebelum mengisi data sebenarnya; tidak
	 * disimpan ke database sampai admin benar-benar memanggil {@link #setPersyaratan(String)}
	 * dengan nilai eksplisit (getter tidak menulis balik ke field seperti pola beberapa getter
	 * flag boolean lain di file ini — placeholder di sini murni dihitung ulang setiap pemanggilan,
	 * tidak pernah dipersist secara tidak sengaja).
	 *
	 * @return teks persyaratan yang tersimpan, atau teks placeholder multi-baris bila field masih
	 * {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getPersyaratan() {
		return persyaratan == null
				? "Usia 25 s/d 38 tahun\r\n" + "Pendidikan minimal S2 segala bidang pendidikan dengan IPK min. 4,00\r\n"
						+ "Pengalaman dibidang yang sama minimal 2 Tahun\r\n"
						+ "Bersedia ditempatkan di daerah Bandung\r\n"
						+ "Mampu bekerja sesuai visi dan misi sekolah, tegas, jujur, dan sopan\r\n"
						+ "Bersedia melakukan perjalanan dinas keluar kota\r\n" + "Mampu bekerja dibawah tekanan"
				: persyaratan;
	}

	/**
	 * Mengeset {@link #persyaratan}.
	 *
	 * @param persyaratan nilai baru untuk {@link #persyaratan}.
	 */
	public void setPersyaratan(String persyaratan) {
		this.persyaratan = persyaratan;
	}

	/**
	 * Mengambil teks tanggung jawab/uraian tugas jabatan pada lowongan gelombang ini (kolom
	 * {@code text}). Sama seperti {@link #getPersyaratan()}, method ini mengembalikan teks
	 * placeholder multi-baris (mencakup perumusan visi, misi, tujuan sekolah, penyusunan RKS/RKAS,
	 * serta program induksi) yang dipisahkan {@code "\r\n"} literal bila field {@link
	 * #tanggungJawab} masih {@code null}. Placeholder ini secara eksplisit bernuansa jabatan
	 * kepala sekolah — mengindikasikan gelombang pendaftaran modul ini pada awalnya dirancang
	 * (atau paling sering dipakai) untuk rekrutmen jabatan struktural sekolah, meski entity secara
	 * umum bisa dipakai untuk jenis lowongan apa pun lewat {@link #getJenis()}.
	 *
	 * @return teks tanggung jawab yang tersimpan, atau teks placeholder multi-baris bertema kepala
	 * sekolah bila field masih {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getTanggungJawab() {
		return tanggungJawab == null
				? "Merumuskan, menetapkan, dan mengembangkan visi sekolah.\r\n"
						+ "Merumuskan, menetapkan, dan mengembangkan misi sekolah.\r\n"
						+ "Merumuskan, menetapkan, dan mengembangkan tujuan sekolah.\r\n"
						+ "Membuat Rencana Kerja Sekolah (RKS) dan Rencana Kegiatan dan Anggaran Sekolah (RKAS).\r\n"
						+ "Membuat perencanaan program induksi."
				: tanggungJawab;
	}

	/**
	 * Mengeset {@link #tanggungJawab}.
	 *
	 * @param tanggungJawab nilai baru untuk {@link #tanggungJawab}.
	 */
	public void setTanggungJawab(String tanggungJawab) {
		this.tanggungJawab = tanggungJawab;
	}

	/**
	 * Mengambil teks disclaimer yang ditampilkan pada lowongan gelombang ini (kolom {@code text}).
	 *
	 * @return teks disclaimer yang tersimpan, atau placeholder {@code "melamar pekerjaan di sini
	 * tidak dipungut biaya"} bila field masih {@code null} — placeholder ini secara khusus
	 * mengingatkan bahwa proses lamaran tidak berbayar, kemungkinan untuk mengantisipasi
	 * penipuan/pungli yang mengatasnamakan proses rekrutmen.
	 */
	@Column(columnDefinition = "text")
	public String getDisclaimer() {
		return disclaimer == null ? "melamar pekerjaan di sini tidak dipungut biaya" : disclaimer;
	}

	/**
	 * Mengeset {@link #disclaimer}.
	 *
	 * @param disclaimer nilai baru untuk {@link #disclaimer}.
	 */
	public void setDisclaimer(String disclaimer) {
		this.disclaimer = disclaimer;
	}

	/**
	 * Mengambil syarat pengalaman kerja minimal pada lowongan gelombang ini (kolom {@code text}).
	 *
	 * @return teks pengalaman yang tersimpan, atau placeholder {@code "Setidaknya 2 Tahun"} bila
	 * field masih {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getPengalaman() {
		return pengalaman == null ? "Setidaknya 2 Tahun" : pengalaman;
	}

	/**
	 * Mengeset {@link #pengalaman}.
	 *
	 * @param pengalaman nilai baru untuk {@link #pengalaman}.
	 */
	public void setPengalaman(String pengalaman) {
		this.pengalaman = pengalaman;
	}

	/**
	 * Mengambil deskripsi fasilitas yang ditawarkan pada lowongan gelombang ini (kolom {@code
	 * text}).
	 *
	 * @return teks fasilitas yang tersimpan, atau placeholder {@code "BPJS Kesehatan, BPJS
	 * Ketenagakerjaan"} bila field masih {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getFasilitas() {
		return fasilitas == null ? "BPJS Kesehatan, BPJS Ketenagakerjaan" : fasilitas;
	}

	/**
	 * Mengeset {@link #fasilitas}.
	 *
	 * @param fasilitas nilai baru untuk {@link #fasilitas}.
	 */
	public void setFasilitas(String fasilitas) {
		this.fasilitas = fasilitas;
	}
}
