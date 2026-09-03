package ais.database.model.sekolah;

// Dibuat mengikuti pola KelompokStatusKeluarMahasiswa, namun untuk Siswa (tabel berbeda).

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;

/**
 * <h3>Kelompok Status Keluar Siswa</h3>
 *
 * <p>Entitas master (tabel {@code sekolah.kelompok_status_keluar_siswa}) yang berperan sebagai
 * <b>wadah/batch berlabel</b> untuk memberi satu status keluar yang sama — misalnya Lulus,
 * Dikeluarkan, Mengundurkan Diri, Pindah — kepada banyak siswa sekaligus dalam satu tahun akademik
 * dan semester, lengkap dengan tanggal lulus/keluar dan keterangan. Keanggotaan disimpan pada sisi
 * siswa: kolom {@code kelompok_status_keluar_siswa} pada tabel {@code sekolah.siswa}, dipetakan oleh
 * {@link Siswa#getKelompokStatusKeluarSiswa()}. Karena relasi itu {@code @ManyToOne} dari sisi
 * {@link Siswa}, <b>satu siswa hanya boleh berada di satu kelompok pada satu waktu</b>: memasukkan
 * siswa ke kelompok baru secara diam-diam mengeluarkannya dari kelompok lamanya (tidak ada
 * peringatan apa pun di layar).</p>
 *
 * <h4>Peran nyata yang terverifikasi dari kode</h4>
 * <p>Pembacaan menyeluruh atas seluruh referensi entitas ini di basis kode (hanya lima berkas:
 * berkas ini sendiri, {@link Siswa}, {@code KelompokStatusKeluarSiswaAction},
 * {@code helper/KelompokStatusKeluarSiswaDetailAction}, serta tab pemanggil di
 * {@code SiswaAction}) memperlihatkan bahwa kelompok ini <b>murni label pengelompokan</b>:</p>
 * <ul>
 *   <li>Nilai {@link #getStatusKeluar()} milik kelompok <b>tidak pernah disalin</b> ke properti
 *       {@code Siswa.statusKeluar}. Satu-satunya penulis {@code Siswa.setStatusKeluar(...)} di
 *       seluruh basis kode adalah formulir biodata per-siswa di {@code SiswaAction}. Jadi
 *       memasukkan seorang siswa ke kelompok "Lulus" <b>tidak</b> membuat status keluar siswa itu
 *       menjadi Lulus.</li>
 *   <li>{@link #getTanggalLulus()} milik kelompok juga tidak disalin otomatis ke
 *       {@code Siswa.tanggalLulus}; tanggal per siswa diketik manual satu per satu pada panel
 *       detail. Teks bantuan di formulir ("Kosongkan tanggal lulus jika siswa dalam kelompok ini
 *       mempunyai tanggal lulus yang berbeda") menyiratkan adanya penurunan nilai otomatis yang
 *       sesungguhnya <b>tidak pernah diimplementasikan</b>.</li>
 *   <li>Tidak ada laporan, dasbor, ekspor, maupun query SQL di luar layar pemeliharaannya yang
 *       membaca kolom {@code kelompok_status_keluar_siswa}. Dasbor kelulusan
 *       ({@code DasboardKelulusanSiswa}) menyaring memakai {@link StatusKeluarSiswa} langsung pada
 *       {@link Siswa}, bukan lewat kelompok ini.</li>
 * </ul>
 * <p>Konsekuensi praktis bagi pembaca kode di masa depan: mengubah keanggotaan kelompok <b>tidak
 * mengubah status akademik siswa mana pun</b>. Efek tulis yang benar-benar terjadi dari panel
 * detail hanyalah pada kolom {@code siswa.kelompok_status_keluar_siswa} dan — bila operator
 * mengetiknya — {@code siswa.tanggal_lulus}.</p>
 *
 * <h4>Struktur anggota berkas ini</h4>
 * <ul>
 *   <li><b>Jejak audit ringan</b> — {@link #getOleh()}/{@link #getOlehId()} (identitas pengubah
 *       terakhir, dengan setter yang menolak nilai kosong) dan
 *       {@link #getTanggal_dirubah()} yang disegarkan callback {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b> — {@link #getId()} ({@code IDENTITY}, berurutan) dan
 *       {@link #toString()}.</li>
 *   <li><b>Atribut deskriptif</b> — {@link #getNama()} (wajib diisi, divalidasi di layar) dan
 *       {@link #getKeterangan()}.</li>
 *   <li><b>Klasifikasi</b> — {@link #getStatusKeluar()} menuju {@link StatusKeluarSiswa}
 *       ({@code NOT NULL} di skema).</li>
 *   <li><b>Periode &amp; tanggal</b> — {@link #getTahunAkademik()} dan {@link #getSemester()},
 *       keduanya {@code NOT NULL} dengan nilai bawaan yang <b>dihitung di dalam getter</b>, serta
 *       {@link #getTanggalLulus()}.</li>
 * </ul>
 * <p>Tidak ada method bisnis, query statis, maupun koleksi turunan pada entitas ini — seluruh
 * logika (pencarian, penyaringan, penambahan/pengeluaran anggota) berada di kelas action.</p>
 *
 * <h4>Hal non-obvious</h4>
 * <ul>
 *   <li><b>Getter dengan nilai bawaan tersembunyi.</b> {@link #getTahunAkademik()} dan
 *       {@link #getSemester()} mengembalikan nilai yang dihitung dari konteks sesi ketika field-nya
 *       masih {@code null}. Karena Hibernate memakai <i>property access</i> (anotasi ada di
 *       getter), nilai bawaan itulah yang tertulis ke kolom {@code NOT NULL} saat penyimpanan
 *       pertama, meskipun pemanggil tidak pernah memanggil setter-nya. Berbeda dari beberapa getter
 *       "destruktif" di modul lain, kedua getter ini <b>tidak menulis balik</b> ke field — nilai
 *       bawaan dihitung ulang setiap pemanggilan selama field masih {@code null}.</li>
 *   <li><b>Kosakata perguruan tinggi pada modul sekolah.</b> {@link #getSemester()} memakai
 *       konstanta {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP} dan helper
 *       {@code Common.isNowSemensterGanjil()} (nama method memang salah eja di sumbernya) —
 *       peninggalan asal-usul berkas ini sebagai salinan versi Mahasiswa.</li>
 *   <li><b>Tidak di-audit dengan sengaja.</b> Entitas ini tidak memakai {@code @Audited} agar tidak
 *       memerlukan tabel {@code _aud} tambahan (aplikasi berjalan dengan
 *       {@code hbm2ddl.auto=none}); relasi dari {@link Siswa} — yang justru {@code @Audited} —
 *       ditandai {@code @NotAudited} supaya envers tidak mempersoalkannya. Efek sampingnya:
 *       <b>perpindahan siswa masuk/keluar kelompok tidak meninggalkan jejak revisi apa pun</b>,
 *       sementara perubahan {@code tanggalLulus} pada siswa yang sama tetap terekam sebagai revisi
 *       {@link Siswa}.</li>
 *   <li><b>Jejak salin-tempel.</b> {@link #serialVersionUID} bernilai
 *       {@code 2463821577548439809L}, yaitu persis nilai milik {@link StatusKeluarSiswa}
 *       ({@code ...808L}) ditambah satu — berkas ini digandakan dari tetangganya lalu angka
 *       terakhirnya dinaikkan manual.</li>
 *   <li><b>{@link #getNama()} tanpa {@code @Column}.</b> Satu-satunya properti persisten yang tidak
 *       diberi anotasi kolom eksplisit; Hibernate memetakannya ke kolom bawaan {@code nama}.</li>
 * </ul>
 *
 * <h4>Perbedaan dengan versi Mahasiswa</h4>
 * <p>Skema tabel berada di schema {@code sekolah} dan jenis statusnya mengacu ke
 * {@link StatusKeluarSiswa} (bukan {@code StatusKeluar} milik Mahasiswa). Padanan penuhnya adalah
 * {@code ais.database.model.KelompokStatusKeluarMahasiswa}; panel detail versi sekolah jauh lebih
 * ringkas (5 kolom identitas + tanggal) dibanding versi Mahasiswa yang menampilkan belasan kolom
 * dokumen kelulusan.</p>
 *
 * <h4>Peringatan otorisasi pada layar pengelolanya</h4>
 * <p><b>Bukan cacat entitas ini</b>, tetapi wajib diketahui siapa pun yang menyentuh alur ini:
 * panel detail {@code ais.action.master.sekolah.helper.KelompokStatusKeluarSiswaDetailAction}
 * <b>sama sekali tidak memanggil {@code CommonPrivilages.checkPrevilages(...)}</b> (kelas itu
 * bahkan tidak meng-import {@code CommonPrivilages}). Layar induk
 * {@code KelompokStatusKeluarSiswaAction} menghitung hak {@code UPDATE}/{@code DELETE} dan
 * memakainya hanya untuk tombol baris grid utama, sedangkan tombol "Ambil Data Siswa",
 * "Hapus Semua" (hingga 5.000 baris), tombol "Hapus" per baris, dan kotak tanggal
 * lulus/keluar yang bisa disunting langsung di dalam panel detail <b>selalu aktif untuk siapa pun
 * yang dapat membuka layarnya, termasuk pengguna berhak baca saja</b>. Pemilih siswa
 * {@code AmbilDataSiswaBanyak} juga tidak menyaring sekolah/yayasan pengguna (penyaring sekolah dan
 * yayasan di sana adalah combobox pilihan pengguna, bukan pembatas ruang lingkup) sehingga daftar
 * calon anggota mencakup seluruh instalasi. Detail lengkap dicatat pada dokumentasi audit
 * proyek.</p>
 *
 * @see Siswa#getKelompokStatusKeluarSiswa()
 * @see StatusKeluarSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "sekolah", name = "kelompok_status_keluar_siswa")
public class KelompokStatusKeluarSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya persis {@code serialVersionUID} milik
	 * {@link StatusKeluarSiswa} ditambah satu, jejak bahwa berkas ini digandakan dari tetangganya.
	 * Jangan diubah agar sesi ZK/HTTP yang sudah ter-serialisasi tetap dapat dipulihkan.
	 */
	private static final long serialVersionUID = 2463821577548439809L;
	/** Kunci utama, diisi basis data melalui kolom {@code IDENTITY}. Lihat {@link #getId()}. */
	private Long id;
	/** Nama tampil pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Pengenal (id) pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan pengenal (id) pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Properti ini dideklarasikan ulang di sini — bukan diwarisi — karena
	 * {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa, bukan
	 * {@code @Entity}/{@code @MappedSuperclass}, sehingga Hibernate tidak memetakan properti
	 * induknya. Pengulangan ini adalah keharusan teknis, bukan duplikasi yang keliru.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diubah lewat
	 *         alur yang mengisinya.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel pengenal (id) pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> nilai {@code null}, string kosong, atau string yang hanya
	 * berisi spasi <b>diabaikan diam-diam</b> — nilai lama dipertahankan. Pola ini dipakai seragam
	 * di seluruh entitas repo agar jejak pengubah terakhir tidak terhapus oleh pemanggil yang
	 * kebetulan tidak memiliki konteks pengguna (misalnya proses batch atau impor).</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampil pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/hanya-spasi diabaikan
	 * diam-diam sehingga nilai lama tidak tertimpa.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistensi tepat sebelum perintah
	 * {@code UPDATE} dikirim ke basis data.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang menyegarkan
	 * {@link #getTanggal_dirubah()} serta {@link #getOleh()}/{@link #getOlehId()} dari konteks
	 * pengguna aktif. <b>Tidak dipanggil pada penyimpanan pertama</b> ({@code INSERT}) — pada kasus
	 * itu nilai {@code tanggal_dirubah} berasal dari inisialisasi field.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi state entitas terkelola. Jangan dipanggil manual dari kode
	 * aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat sehingga baris
	 * baru pun selalu memiliki nilai; sesudah itu disegarkan oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah cap waktu yang dikehendaki. Umumnya diisi otomatis oleh
	 *                        {@link #onUpdate()}; pengisian manual hanya dipakai oleh alur
	 *                        impor/migrasi yang ingin mempertahankan cap waktu asal.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir (presisi {@code TIMESTAMP}, hingga detik/milidetik).
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor karena field-nya sudah diinisialisasi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<statusKeluar>"}.
	 *
	 * <p><b>Efek samping yang perlu diketahui:</b> method ini memanggil {@link #getStatusKeluar()},
	 * yang berarti ia dapat <b>memicu inisialisasi proxy lazy</b> ke {@link StatusKeluarSiswa} —
	 * dan karenanya query basis data — hanya karena objek ini dicetak (misalnya di log atau di
	 * pesan galat). Di luar sesi Hibernate yang aktif hal ini bisa memunculkan
	 * {@code LazyInitializationException}. Penugasan {@code statusKeluar = getStatusKeluar()}
	 * bersifat redundan karena getter-nya sudah menulis balik ke field yang sama.</p>
	 *
	 * <p>Bagian status dirender lewat {@code String.valueOf} implisit atas entitas
	 * {@link StatusKeluarSiswa}, bukan namanya — sehingga keluarannya adalah
	 * {@code toString()} milik entitas itu, bukan label yang ramah pengguna. Untuk tampilan layar,
	 * gunakan {@code getStatusKeluar().getNama()} sebagaimana dilakukan renderer grid.</p>
	 *
	 * @return gabungan id dan status keluar, mis. {@code "12-3-Lulus"}.
	 */
	public String toString() {
		statusKeluar = getStatusKeluar();
		return id + "-" + statusKeluar;
	}

	/** Nama kelompok yang diketik operator; wajib diisi (divalidasi di layar). Lihat {@link #getNama()}. */
	private String nama;
	/** Catatan bebas tentang kelompok ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Jenis status keluar yang diwakili kelompok ini. Lihat {@link #getStatusKeluar()}. */
	private StatusKeluarSiswa statusKeluar;
	/** Tanggal lulus/keluar tingkat kelompok. Lihat {@link #getTanggalLulus()}. */
	private Date tanggalLulus;
	/** Tahun akademik berlakunya kelompok. Lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Semester berlakunya kelompok ({@code Ganjil}/{@code Genap}). Lihat {@link #getSemester()}. */
	private String semester;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi entitas, dan dipakai
	 * layar {@code KelompokStatusKeluarSiswaAction} saat operator menekan tombol "Tambah".
	 *
	 * <p>Seluruh properti dibiarkan {@code null} kecuali {@link #getTanggal_dirubah()} yang sudah
	 * terisi lewat inisialisasi field. Perlu dicatat bahwa {@link #getTahunAkademik()} dan
	 * {@link #getSemester()} akan tetap mengembalikan nilai (bukan {@code null}) meski belum pernah
	 * disetel — lihat dokumentasi masing-masing getter.</p>
	 */
	public KelompokStatusKeluarSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dihasilkan basis data dengan strategi {@code IDENTITY} sehingga <b>berurutan dan mudah
	 * ditebak</b>; kolomnya {@code insertable = false} karena nilainya tidak pernah dikirim
	 * aplikasi. Nilai {@code null} menandakan objek baru yang belum pernah disimpan — layar induk
	 * memakai tepat pemeriksaan ini untuk memilih judul "Tambah" atau "Ubah".</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Disediakan untuk kebutuhan kerangka kerja (Hibernate, deserialisasi, konstruksi referensi
	 * ringan). Jangan dipakai untuk mengubah identitas baris yang sudah tersimpan.</p>
	 *
	 * @param id kunci utama yang dikehendaki.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kelompok, yaitu label yang diketik operator untuk membedakan satu batch
	 * status keluar dari batch lain (mis. "Lulusan Angkatan 2025 IPA").
	 *
	 * <p>Satu-satunya properti persisten pada kelas ini yang tidak diberi anotasi {@code @Column}
	 * eksplisit; Hibernate memetakannya ke kolom bawaan bernama {@code nama}. Dipakai sebagai kunci
	 * pengurutan dan pencarian di {@code KelompokStatusKeluarSiswaAction.initCriteria(boolean)},
	 * dan ditampilkan pada judul panel detail ("Daftar siswa yang masuk kelompok ...").</p>
	 *
	 * @return nama kelompok, atau {@code null} untuk objek baru yang belum diisi.
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama kelompok.
	 *
	 * <p>Wajib terisi: layar induk menolak penyimpanan dengan pesan "Nama Kelompok Status Keluar
	 * Siswa harus diisi" bila kosong. Namun validasi itu hanya ada di lapisan tampilan — kolomnya
	 * sendiri tidak {@code NOT NULL}, sehingga alur non-UI (impor massal) dapat menyimpan nama
	 * kosong.</p>
	 *
	 * @param nama nama kelompok.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas mengenai kelompok ini.
	 *
	 * <p>Bersifat opsional ({@code nullable = true}) dan hanya ditampilkan sebagai kolom terakhir
	 * grid layar induk. Tidak dipakai untuk logika apa pun.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan catatan tambahan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jenis status keluar yang diwakili kelompok ini (Lulus, Pindah, Dikeluarkan, dan
	 * seterusnya).
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke {@link StatusKeluarSiswa} melalui kolom
	 * {@code status_keluar_siswa} yang {@code NOT NULL} di skema — meskipun tidak ada
	 * {@code @NotNull} di sisi Java, penyimpanan tanpa status akan gagal di tingkat basis data.
	 * Layar induk mencegahnya lebih awal dengan pesan "Status Keluar harus diisi".</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} lalu <b>menulis balik hasilnya ke field</b>.
	 * Helper itu menormalkan proxy lazy menjadi instance kanonik (satu objek Java per id di seluruh
	 * JVM) sehingga perubahan skalar pada entitas bersangkutan langsung terlihat oleh semua
	 * pemegang referensi. Konsekuensinya, pemanggilan getter ini dapat memicu query basis data dan
	 * bukan operasi baca murni.</p>
	 *
	 * <p><b>Penting:</b> nilai ini <b>tidak pernah</b> disalin ke {@code Siswa.statusKeluar} oleh
	 * alur mana pun — lihat catatan peran nyata pada dokumentasi kelas.</p>
	 *
	 * @return jenis status keluar; secara praktis tidak pernah {@code null} untuk baris tersimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_keluar_siswa", nullable = false)
	public StatusKeluarSiswa getStatusKeluar() {
		statusKeluar = check(statusKeluar);
		return statusKeluar;
	}

	/**
	 * Menyetel jenis status keluar kelompok.
	 *
	 * <p>Karena relasi memakai {@code CascadeType.PERSIST} dan {@code MERGE}, menyimpan kelompok ini
	 * juga akan mem-persist/merge objek {@link StatusKeluarSiswa} yang belum terkelola. Dalam alur
	 * normal nilainya selalu berupa entitas yang sudah ada (dipilih dari combobox), sehingga cascade
	 * tersebut tidak pernah membuat baris master baru.</p>
	 *
	 * @param statusKeluar jenis status keluar; kolomnya {@code NOT NULL} sehingga {@code null} akan
	 *                     menyebabkan kegagalan saat flush.
	 */
	public void setStatusKeluar(StatusKeluarSiswa statusKeluar) {
		this.statusKeluar = statusKeluar;
	}

	/**
	 * Mengembalikan tahun akademik berlakunya kelompok ini.
	 *
	 * <p><b>Nilai bawaan tersembunyi:</b> bila field masih {@code null}, getter mengembalikan
	 * {@code Common.getCurrentTahunAkademik()} — tahun akademik aktif menurut konteks
	 * pengguna/sesi. Karena Hibernate memakai <i>property access</i>, nilai bawaan itulah yang
	 * ditulis ke kolom {@code tahun_akademik} yang {@code NOT NULL} saat penyimpanan pertama,
	 * walaupun operator tidak pernah menyentuh comboboxnya.</p>
	 *
	 * <p>Berbeda dari beberapa getter "destruktif" di modul lain, method ini <b>tidak</b> menulis
	 * balik ke field: selama field {@code null}, nilai bawaan dihitung ulang pada setiap
	 * pemanggilan. Artinya, satu objek yang belum tersimpan dapat melaporkan tahun akademik berbeda
	 * bila konteks sesi berubah di antara dua pemanggilan.</p>
	 *
	 * @return tahun akademik yang tersimpan, atau tahun akademik aktif bila belum pernah disetel.
	 */
	@Column(name = "tahun_akademik", nullable = false)
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik berlakunya kelompok.
	 *
	 * <p>Diisi layar induk dari combobox hasil {@code Common.generateTahunAjaran(...)}. Menyetel
	 * {@code null} mengembalikan properti ke perilaku nilai bawaan yang dijelaskan pada
	 * {@link #getTahunAkademik()}.</p>
	 *
	 * @param tahunAkademik tahun akademik, mis. {@code "2025/2026"}.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan semester berlakunya kelompok ini.
	 *
	 * <p><b>Nilai bawaan tersembunyi:</b> bila field masih {@code null}, getter menghitungnya dari
	 * {@code Common.isNowSemensterGanjil()} (nama method memang salah eja di sumbernya) dan
	 * mengembalikan {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}. Helper tersebut
	 * mengambil semester dari {@code RencanaTahunAkademik} yang aktif untuk pengguna saat ini, dan
	 * bila tidak ada, jatuh ke perkiraan berbasis bulan kalender (Juni–Desember dianggap Ganjil).
	 * Helper itu tidak pernah mengembalikan {@code null}, sehingga tidak ada risiko
	 * {@code NullPointerException} dari auto-unboxing di sini.</p>
	 *
	 * <p>Seperti {@link #getTahunAkademik()}, nilai bawaan <b>tidak</b> ditulis balik ke field, dan
	 * nilainya yang mengisi kolom {@code semester} yang {@code NOT NULL} pada penyimpanan pertama.
	 * Pemakaian konstanta {@link Perkuliahan} (kosakata perguruan tinggi) pada entitas sekolah
	 * adalah peninggalan asal-usul berkas ini sebagai salinan versi Mahasiswa.</p>
	 *
	 * @return semester yang tersimpan, atau semester berjalan bila belum pernah disetel.
	 */
	@Column(name = "semester", nullable = false)
	public String getSemester() {
		return semester == null ? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP) : semester;
	}

	/**
	 * Menyetel semester berlakunya kelompok.
	 *
	 * <p>Diisi layar induk dari combobox berisi tepat dua pilihan, {@link Perkuliahan#GANJIL} dan
	 * {@link Perkuliahan#GENAP}. Nilai di luar kedua konstanta itu tidak divalidasi di lapisan mana
	 * pun.</p>
	 *
	 * @param semester nama semester; sebaiknya salah satu konstanta {@link Perkuliahan}.
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tanggal lulus/keluar tingkat kelompok.
	 *
	 * <p>Disimpan sebagai {@code DATE} (tanpa komponen jam). Bersifat opsional: teks bantuan di
	 * formulir menganjurkan mengosongkannya bila anggota kelompok memiliki tanggal berbeda-beda.</p>
	 *
	 * <p><b>Penting — tidak diturunkan otomatis.</b> Tidak ada satu pun alur yang menyalin nilai ini
	 * ke {@code Siswa.tanggalLulus}; tanggal per siswa harus diketik satu per satu pada kolom
	 * "Tgl.Lulus/Keluar" di panel detail. Nilai di sini murni informatif, ditampilkan sebagai satu
	 * kolom grid layar induk ("Tidak Ditentukan" bila {@code null}).</p>
	 *
	 * @return tanggal lulus/keluar kelompok, atau {@code null} bila tidak ditentukan.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalLulus() {
		return tanggalLulus;
	}

	/**
	 * Menyetel tanggal lulus/keluar tingkat kelompok.
	 *
	 * @param tanggalLulus tanggal lulus/keluar; boleh {@code null} bila anggota kelompok memiliki
	 *                     tanggal masing-masing.
	 */
	public void setTanggalLulus(Date tanggalLulus) {
		this.tanggalLulus = tanggalLulus;
	}

}
