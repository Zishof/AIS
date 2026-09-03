package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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



import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Baris <b>keikutsertaan seorang siswa pada satu kegiatan kesiswaan</b> — tabel penghubung
 * (<i>join entity</i>) {@code sekolah.kegiatan_kesiswaan_punya_siswa} antara
 * {@link KegiatanKesiswaan} (kegiatan/lomba/kepanitiaan) dan {@link Siswa} (peserta). Satu baris
 * = satu peserta pada satu kegiatan, ditambah atribut khas peserta tersebut: peran/capaian
 * ({@code jabatanKegiatanKesiswaan}), tingkat/skala ({@code skalaKegiatanKesiswaan}), rentang
 * tanggal keikutsertaan ({@code mulai}/{@code sampai}), catatan bebas ({@code keterangan}), dan
 * status persetujuan pembina/petugas ({@code persetujuan}).
 *
 * <p><b>Posisi dalam rantai kegiatan kesiswaan</b> (semua sudah didokumentasikan terpisah):</p>
 * <ol>
 *   <li>{@code KelompokKegiatanKesiswaan} — tingkat 1 hierarki katalog (Utama/Penunjang).</li>
 *   <li>{@link DetailKelompokKegiatanKesiswaan} — tingkat 2; memiliki daftar jabatan dan skala
 *   yang <b>boleh dipilih</b> untuk kegiatan di bawahnya (relasi many-to-many ke katalog).</li>
 *   <li>{@link KegiatanKesiswaan} — kegiatan konkret (nama, tempat, tanggal, sekolah/yayasan,
 *   pembina, status pengajuan, sertifikat). Menyimpan <b>nilai bawaan</b> jabatan/skala/tanggal.</li>
 *   <li><b>Kelas ini</b> — baris peserta. Nilai jabatan/skala/tanggal di sini <i>menimpa</i>
 *   bawaan kegiatan bila diisi, dan <i>mewarisi</i> bawaan kegiatan bila kosong (lihat
 *   {@link #getJabatanKegiatanKesiswaan()}, {@link #getSkalaKegiatanKesiswaan()},
 *   {@link #getMulai()}, {@link #getSampai()}).</li>
 *   <li>{@link NilaiKegiatanKesiswaan} — rubrik/nilai yang dihitung dari keikutsertaan.</li>
 * </ol>
 *
 * <p>Katalog yang dirujuk baris ini adalah {@link ais.database.model.sekolah.JabatanKegiatanKesiswaan}
 * (satu kolom yang mencampur peran kepanitiaan, format lomba, dan capaian juara — mis. "Peserta",
 * "Panitia", "Juara I") dan {@link ais.database.model.sekolah.SkalaKegiatanKesiswaan} (tingkat
 * penyelenggaraan/durasi — mis. "Sekolah", "Kabupaten", "Nasional"). Keduanya tidak dipilih bebas
 * dari seluruh katalog: daftar pilihan pada layar peserta dibatasi oleh koleksi many-to-many milik
 * {@link DetailKelompokKegiatanKesiswaan} induk kegiatan.</p>
 *
 * <p><b>Pengelompokan method:</b></p>
 * <ul>
 *   <li><i>Jejak audit ringan</i> — {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)}, {@link #getTanggal_dirubah()}/
 *   {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}, {@link #getDiubahDari()}.</li>
 *   <li><i>Identitas</i> — {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><i>Relasi wajib</i> — {@link #getKegiatanKesiswaan()}, {@link #getSiswa()}.</li>
 *   <li><i>Relasi opsional</i> — {@link #getTbmuser()} (akun pendaftar),
 *   {@link #getJabatanKegiatanKesiswaan()}, {@link #getSkalaKegiatanKesiswaan()}.</li>
 *   <li><i>Atribut keikutsertaan</i> — {@link #getMulai()}, {@link #getSampai()},
 *   {@link #getKeterangan()}, {@link #getPersetujuan()}.</li>
 * </ul>
 *
 * <p><b>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini:</b></p>
 * <ol>
 *   <li><b>Getter di kelas ini tidak murni membaca.</b> {@link #getJabatanKegiatanKesiswaan()},
 *   {@link #getSkalaKegiatanKesiswaan()} dan {@link #getPersetujuan()} <b>menulis balik</b> ke
 *   field. Karena Hibernate memakai <i>property access</i> (anotasi berada pada getter) dan
 *   entity dipetakan {@code dynamicUpdate = true}, nilai hasil tulis-balik itulah yang ikut
 *   ter-{@code flush} ke database begitu baris tersentuh di dalam session yang masih terbuka.
 *   Membaca baris ini <b>bisa mengubah isinya secara permanen</b> — lihat peringatan rinci di
 *   masing-masing getter.</li>
 *   <li><b>{@code mulai}/{@code sampai} sering NULL di database</b> meskipun getter-nya selalu
 *   mengembalikan tanggal (fallback ke {@link KegiatanKesiswaan}). Fallback itu <b>tidak</b>
 *   ditulis balik, sehingga query yang menyaring langsung ke kolom (bukan lewat getter)
 *   <b>menjatuhkan</b> baris-baris tersebut. Ini bukan teori: dashboard rekap menyaring
 *   {@code mulai} dua kali (Criteria dan native SQL) — lihat bagian "Jangkauan dashboard".</li>
 *   <li><b>{@code persetujuan} bukan milik baris ini sepenuhnya.</b> Nilainya dipaksa
 *   {@code false} oleh getter bila kegiatan induk belum/tidak berstatus
 *   {@link PrestasiSiswa#DISETUJUI}, dan dipaksa {@code true} secara massal oleh
 *   {@code KegiatanKesiswaanAction} untuk SELURUH peserta ketika kegiatan disetujui.</li>
 *   <li><b>{@link #getTbmuser()} menyembunyikan pendaftar bila pendaftarnya siswa.</b> Kolom di
 *   database tetap terisi; hanya getter yang mengembalikan {@code null}. Jangan pakai getter ini
 *   sebagai bukti "tidak ada yang mendaftarkan".</li>
 * </ol>
 *
 * <p><b>Jangkauan dashboard rekap (VERIFIKASI batch 59/60-61).</b> Tabel entity inilah yang
 * menjadi tabel fakta ({@code from sekolah.kegiatan_kesiswaan_punya_siswa aaa}) pada
 * {@code ais.action.master.dashboard.helper.DashboardRekapKegiatanKesiswaan}, yaitu tempat SQL
 * injection lewat nama baris katalog ditemukan (nama {@code JabatanKegiatanKesiswaan}/
 * {@code SkalaKegiatanKesiswaan} disambung mentah menjadi alias kolom native SQL). Kolom yang
 * di-{@code group}/dihitung adalah kolom FK <b>milik entity ini</b>
 * ({@code aaa.jabatan_kegiatan_kesiswaan}, {@code aaa.skala_kegiatan_kesiswaan}) untuk varian
 * "Berdasar Jabatan"/"Berdasar Skala". Jadi entity ini <b>terjangkau</b> kerentanan tersebut
 * sebagai korban (baris-barisnya yang dibaca dan diekspor ke XLSX), walaupun payload berasal dari
 * kelas katalog. Selain itu filter {@code between} atas {@code mulai} dipasang pada dua tempat
 * sekaligus, sehingga peserta ber-{@code mulai} NULL tidak pernah muncul di rekap manapun.</p>
 *
 * <p><b>Pemakai utama</b> (verifikasi kode, bukan dugaan):</p>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.helper.KegiatanKesiswaanPunyaSiswaHelper} — panel
 *   "Daftar siswa yang mengikuti ..." di dalam baris {@code KegiatanKesiswaanAction}; edit inline
 *   jabatan/skala/tanggal/keterangan, checkbox "Setujui", tombol Hapus, "Ambil Siswa",
 *   "Bersihkan", Download/Upload Excel, cetak sertifikat per peserta.</li>
 *   <li>{@code ais.action.master.helper.SiswaPunyaKegiatanKesiswaanHelper} — sisi siswa
 *   (tab "Kegiatan" pada biodata siswa); edit hanya untuk baris milik siswa yang login dan hanya
 *   selama belum disetujui.</li>
 *   <li>{@code ais.action.master.sekolah.helper.AmbilDataSiswaForKegiatanKesiswaanHelper} —
 *   dialog pendaftaran massal peserta.</li>
 *   <li>{@code ais.action.master.sekolah.KegiatanKesiswaanAction} — pembuatan baris otomatis untuk
 *   siswa pengaju, unggah/unduh Excel persetujuan, persetujuan massal.</li>
 *   <li>{@code ais.action.master.SertifikatAction#cetakSertifikat(KegiatanKesiswaanPunyaSiswa)} —
 *   cetak sertifikat peserta dari template JRXML kegiatan.</li>
 *   <li>{@code ais.action.master.dashboard.helper.DashboardRekapKegiatanKesiswaan} dan keempat
 *   turunannya (Berdasar Jabatan/Skala/Kelompok/Detail Kelompok).</li>
 * </ul>
 *
 * <p><b>Lampiran.</b> Bukti keikutsertaan disimpan di {@code LampiranLain} dengan
 * {@code jenis = "ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa"} dan {@code ref = id}
 * baris ini. Karena keterkaitan itu hanya berupa pasangan nilai (bukan FK), penghapusan baris —
 * terutama lewat native SQL tombol "Bersihkan" — meninggalkan lampiran yatim.</p>
 *
 * <p><b>Audit.</b> Kelas ber-{@code @Audited} (Envers), sehingga perubahan lewat session Hibernate
 * masuk ke tabel revisi dan tampil pada tombol Revisi tiap baris. Operasi native SQL massal
 * (mis. "Bersihkan") <b>tidak</b> tercatat di Envers.</p>
 *
 * <p><b>Catatan pewarisan dari {@link ais.database.model.GeneralValueObject}.</b> Kelas induk
 * adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * sehingga Hibernate tidak memetakan properti miliknya. Karena itu {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} <b>sengaja dideklarasikan ulang</b> di kelas ini;
 * itu keharusan teknis pemetaan, bukan duplikasi yang perlu "dibersihkan". Perilaku bersama yang
 * tetap diwarisi antara lain {@link ais.database.model.GeneralValueObject#check(Object)},
 * {@link ais.database.model.GeneralValueObject#equals(Object)}, dan
 * {@link ais.database.model.GeneralValueObject#compareTo(ais.database.model.GeneralValueObject)}.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see KegiatanKesiswaan
 * @see Siswa
 * @see JabatanKegiatanKesiswaan
 * @see SkalaKegiatanKesiswaan
 * @see DetailKelompokKegiatanKesiswaan
 * @see NilaiKegiatanKesiswaan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kegiatan_kesiswaan_punya_siswa")



public class KegiatanKesiswaanPunyaSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai ini <b>sama persis</b> dengan milik {@link KegiatanKesiswaan} dan beberapa entity
	 * lain di paket ini — sisa salin-tempel kerangka kelas, bukan penanda kompatibilitas yang
	 * dihitung. Tidak berpengaruh pada pemetaan Hibernate; jangan diubah tanpa alasan karena
	 * instance entity ikut diserialisasi ke dalam session ZK (desktop/state komponen).</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code sekolah.kegiatan_kesiswaan_punya_siswa.id}; dideklarasikan ulang karena induk tidak dipetakan Hibernate. */
	private Long id;
	/** Nama/ID pengguna terakhir yang menyentuh baris ini (jejak audit ringan, diisi pemanggil). */
	private String oleh;
	/** Identitas tambahan pengguna penyunting; dipakai berdampingan dengan {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan identitas tambahan pengguna penyunting terakhir apa adanya (boleh {@code null}).
	 *
	 * @return isi kolom {@code oleh_id}, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas tambahan pengguna penyunting, dengan <b>penjagaan tulis-kosong</b>.
	 *
	 * <p>Bila {@code olehId} {@code null} atau hanya berisi spasi, method <b>langsung keluar</b>
	 * tanpa mengubah apa pun — nilai lama dipertahankan. Konsekuensinya jejak audit tidak dapat
	 * dikosongkan lewat setter ini, dan pemanggil yang mengira berhasil "membersihkan" kolom akan
	 * salah duga. Perilaku ini sengaja seragam di seluruh entity turunan
	 * {@link ais.database.model.GeneralValueObject}.</p>
	 *
	 * @param olehId identitas pengguna; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama/ID pengguna penyunting terakhir, dengan penjagaan tulis-kosong yang sama
	 * seperti {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * <p>Dipanggil dari alur pendaftaran peserta
	 * ({@code AmbilDataSiswaForKegiatanKesiswaanHelper#save()},
	 * {@code KegiatanKesiswaanAction}) dengan {@code Tbmuser#getUserId()} pengguna aktif.</p>
	 *
	 * @param oleh identitas pengguna; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/ID pengguna penyunting terakhir apa adanya (boleh {@code null}).
	 *
	 * @return isi kolom {@code oleh}, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: meneruskan entity ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} agar stempel waktu
	 * perubahan diperbarui otomatis tepat sebelum {@code UPDATE} dikirim ke database.
	 *
	 * <p>Ini implementasi wajib dari satu-satunya method {@code abstract} milik
	 * {@link ais.database.model.GeneralValueObject}. Method tidak dipanggil manual dari kode
	 * aplikasi — hanya oleh provider persistence.</p>
	 *
	 * <p><b>Perhatian pembaca:</b> pada baris fisik yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil#getDate()}). Penulisan berdempet ini adalah gaya bawaan
	 * generator kode di repo, jadi jangan dipisah tanpa alasan; keduanya bekerja berpasangan —
	 * field menyediakan nilai awal untuk baris baru, callback ini menyegarkannya saat baris lama
	 * diubah.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara eksplisit.
	 *
	 * <p>Tidak ada penjagaan {@code null} di sini (berbeda dari {@link #setOleh(String)}), sehingga
	 * memanggilnya dengan {@code null} benar-benar mengosongkan kolom. Dalam alur normal setter ini
	 * tidak perlu dipanggil: {@link #onUpdate()} sudah mengurusnya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima dan menimpa nilai lama
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#TIMESTAMP} (tanggal + jam), berbeda dari
	 * {@link #getMulai()}/{@link #getSampai()} yang hanya menyimpan tanggal.</p>
	 *
	 * @return waktu perubahan terakhir; untuk objek yang baru dibuat berisi waktu pembuatan objek,
	 *         bukan waktu simpan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris: {@code "<kegiatan> - <siswa>"}.
	 *
	 * <p><b>Bukan method murni.</b> Baris pertamanya memanggil {@link #getSiswa()} dan
	 * <b>menugaskan hasilnya kembali</b> ke field {@code siswa}; artinya {@code toString()} ikut
	 * memicu inisialisasi proxy lazy {@link Siswa} <i>dan</i> menormalkan field lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}. Memanggil {@code toString()} di
	 * luar session Hibernate yang terbuka dapat melempar {@code LazyInitializationException}.</p>
	 *
	 * <p>Perhatikan asimetri yang mudah terlewat: bagian {@code siswa} melewati getter (ter-{@code check}),
	 * sedangkan bagian {@code kegiatanKesiswaan} memakai <b>field mentah</b> — jadi bila kegiatan masih
	 * berupa proxy yang belum diinisialisasi, penggabungan String di sini yang akan memicu
	 * pemuatannya.</p>
	 *
	 * @return gabungan {@code toString()} kegiatan dan siswa dipisah {@code " - "}
	 */
	public String toString() {
		siswa = getSiswa();
		return kegiatanKesiswaan + " - " + siswa;
	}

	/** Kegiatan yang diikuti (FK {@code kegiatan_kesiswaan}, wajib). Sumber nilai bawaan jabatan/skala/tanggal. */
	private KegiatanKesiswaan kegiatanKesiswaan;
	/** Siswa peserta (FK {@code siswa}, wajib). */
	private Siswa siswa;
	/** Nama kelas Action/Helper asal perubahan (mis. {@code "SiswaAction"}, {@code "KegiatanKesiswaanAction"}); jejak provenance bebas teks, tanpa relasi. */
	private String diubahDari;

	/** Akun pengguna yang mendaftarkan peserta ini (FK {@code tbmuser}, opsional); disembunyikan getter bila akunnya milik siswa. */
	private Tbmuser tbmuser;
	/** Peran/capaian peserta pada kegiatan ini (FK opsional); kosong berarti mengikuti bawaan kegiatan. */
	private JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan;
	/** Tingkat/skala keikutsertaan peserta ini (FK opsional); kosong berarti mengikuti bawaan kegiatan. */
	private SkalaKegiatanKesiswaan skalaKegiatanKesiswaan;

	/** Catatan bebas per peserta (kolom {@code text}); diedit inline pada grid peserta. */
	private String keterangan;
	/** Tanggal mulai keikutsertaan peserta ini; sering NULL dan digantikan tanggal kegiatan oleh getter. */
	private Date mulai;
	/** Tanggal selesai keikutsertaan peserta ini; sering NULL dan digantikan tanggal kegiatan oleh getter. */
	private Date sampai;
	/** Status persetujuan pembina/petugas atas keikutsertaan ini; menentukan hak edit/hapus dan kelayakan cetak sertifikat. */
	private Boolean persetujuan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Juga dipakai langsung oleh kode aplikasi ketika peserta baru didaftarkan
	 * ({@code AmbilDataSiswaForKegiatanKesiswaanHelper#save()}, unggah Excel dan pembuatan baris
	 * otomatis di {@code KegiatanKesiswaanAction}). Semua relasi wajib
	 * ({@link #setKegiatanKesiswaan(KegiatanKesiswaan)}, {@link #setSiswa(Siswa)}) harus diisi
	 * pemanggil sebelum disimpan.</p>
	 */
	public KegiatanKesiswaanPunyaSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris keikutsertaan.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@link javax.persistence.GenerationType#IDENTITY}). Nilai ini juga dipakai sebagai
	 * {@code ref} lampiran bukti kegiatan di {@code LampiranLain}, sehingga id yang sudah terbit
	 * tidak boleh dipakai ulang untuk baris lain.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama secara manual.
	 *
	 * <p>Dalam alur normal tidak dipanggil (id dibangkitkan database). Berguna hanya pada
	 * skenario penyalinan/detach; menyetelnya pada entity yang sudah persisten akan membuat
	 * Hibernate memperlakukannya sebagai baris berbeda.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kegiatan yang diikuti peserta ini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}), lazy. Sebelum dikembalikan, nilai dilewatkan
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan <b>ditulis balik</b> ke
	 * field — pola seragam di repo ini untuk menormalkan proxy/instance yang tidak lagi valid.</p>
	 *
	 * <p>Objek ini adalah sumber semua nilai bawaan baris: {@link #getJabatanKegiatanKesiswaan()},
	 * {@link #getSkalaKegiatanKesiswaan()}, {@link #getMulai()}, {@link #getSampai()} dan
	 * {@link #getPersetujuan()} semuanya membacanya.</p>
	 *
	 * @return kegiatan kesiswaan induk; secara teori tidak pernah {@code null} untuk baris
	 *         persisten, namun bisa {@code null} pada objek yang belum lengkap diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kegiatan_kesiswaan", nullable = false)
	public KegiatanKesiswaan getKegiatanKesiswaan() {
		kegiatanKesiswaan = check(kegiatanKesiswaan);
		return kegiatanKesiswaan;
	}

	/**
	 * Menyetel kegiatan yang diikuti peserta ini.
	 *
	 * <p>Wajib diisi sebelum penyimpanan. Mengubahnya setelah baris tersimpan berarti memindahkan
	 * peserta ke kegiatan lain — nilai {@code jabatanKegiatanKesiswaan}/{@code skalaKegiatanKesiswaan}
	 * yang sudah terlanjur ditulis balik dari kegiatan lama <b>tidak</b> ikut disesuaikan, sehingga
	 * baris dapat merujuk katalog yang tidak tersedia pada detail kelompok kegiatan baru.</p>
	 *
	 * @param kegiatanKesiswaan kegiatan induk
	 */
	public void setKegiatanKesiswaan(KegiatanKesiswaan kegiatanKesiswaan) {
		this.kegiatanKesiswaan = kegiatanKesiswaan;
	}

	/**
	 * Mengembalikan siswa peserta.
	 *
	 * <p>Relasi wajib ({@code nullable = false}), lazy, dinormalkan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis balik ke field.</p>
	 *
	 * <p>Dari objek inilah seluruh layar peserta mengambil NIS/NIM, nama, foto, tahun masuk, dan
	 * sekolah; juga dipakai {@code SertifikatAction#cetakSertifikat} untuk QR code sertifikat.</p>
	 *
	 * @return siswa peserta
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa peserta. Wajib diisi sebelum penyimpanan.
	 *
	 * <p>Keunikan pasangan (kegiatan, siswa) <b>tidak</b> dijamin skema — pemanggil yang
	 * mendaftarkan peserta selalu mencari baris yang sudah ada lebih dulu
	 * ({@code Restrictions.eq("siswa", ...)} + {@code Restrictions.eq("kegiatanKesiswaan", ...)})
	 * sebelum membuat baris baru. Kode baru harus mengikuti pola itu agar tidak menghasilkan
	 * peserta ganda.</p>
	 *
	 * @param siswa siswa peserta
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan akun pengguna yang mendaftarkan peserta ini — <b>kecuali bila akun itu milik
	 * seorang siswa, yang dalam hal itu method mengembalikan {@code null}</b>.
	 *
	 * <p>Urutannya: field dinormalkan {@link ais.database.model.GeneralValueObject#check(Object)}
	 * dan ditulis balik (jadi getter ini ikut menginisialisasi proxy lazy), lalu hasilnya disaring
	 * dengan {@code tbmuser.getSiswa() != null ? null : tbmuser}.</p>
	 *
	 * <p><b>Efeknya:</b> pendaftaran mandiri oleh siswa (mis. siswa mengajukan kegiatan lewat
	 * {@code KegiatanKesiswaanAction}, yang otomatis membuatkan baris peserta untuk dirinya)
	 * tampak "tanpa pendaftar" di seluruh layar dan ekspor. Yang disembunyikan hanyalah tampilan:
	 * kolom {@code tbmuser} di database tetap terisi, dan penyaringan langsung ke kolom (native
	 * SQL, Criteria) tetap melihatnya. Jangan menyimpulkan "tidak ada yang mendaftarkan" dari
	 * hasil {@code null} getter ini, dan jangan memakainya sebagai kontrol akses.</p>
	 *
	 * <p>Berbeda dari getter destruktif lain di kelas ini, penyaringan ini <b>tidak</b> menulis
	 * {@code null} ke field, sehingga tidak merusak data saat baris di-{@code flush}.</p>
	 *
	 * @return akun pendaftar bila akun tersebut bukan akun siswa; {@code null} bila kolom kosong
	 *         <i>atau</i> pendaftarnya seorang siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser != null && tbmuser.getSiswa() != null ? null : tbmuser;
	}

	/**
	 * Menyetel akun pengguna yang mendaftarkan peserta ini.
	 *
	 * <p>Diisi dengan {@code Common.getCurrentUser()} oleh seluruh alur pendaftaran
	 * ({@code AmbilDataSiswaForKegiatanKesiswaanHelper#save()}, unggah Excel, dan pembuatan baris
	 * otomatis untuk siswa pengaju di {@code KegiatanKesiswaanAction}).</p>
	 *
	 * <p>Perlu diingat pasangan setter/getter ini <b>tidak simetris</b>: nilai yang disetel di sini
	 * belum tentu bisa dibaca kembali lewat {@link #getTbmuser()} (lihat penyaringan di sana).</p>
	 *
	 * @param tbmuser akun pendaftar; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan penanda asal perubahan baris — nama sederhana kelas Action/Helper yang terakhir
	 * membuat atau mengubahnya (mis. {@code "SiswaAction"}, {@code "KegiatanKesiswaanAction"}).
	 *
	 * <p>Sekadar teks bebas untuk penelusuran; tidak dipetakan ke relasi apa pun dan tidak
	 * divalidasi. Perhatikan bahwa {@code AmbilDataSiswaForKegiatanKesiswaanHelper} mengisinya
	 * dengan {@code SiswaAction.class.getSimpleName()} walaupun pendaftaran dilakukan dari layar
	 * kegiatan — jadi nilainya tidak selalu menunjuk layar yang sebenarnya dipakai.</p>
	 *
	 * @return nama kelas asal perubahan, atau {@code null} bila tidak diisi
	 */
	public String getDiubahDari() {
		return diubahDari;
	}

	/**
	 * Menyetel penanda asal perubahan baris.
	 *
	 * @param diubahDari nama sederhana kelas Action/Helper pemanggil; bebas, boleh {@code null}
	 */
	public void setDiubahDari(String diubahDari) {
		this.diubahDari = diubahDari;
	}

	/**
	 * Mengembalikan peran/capaian peserta ({@link JabatanKegiatanKesiswaan}), dengan
	 * <b>pewarisan otomatis dari kegiatan induk</b>.
	 *
	 * <p>Bila field masih kosong sementara {@code kegiatanKesiswaan} tersedia, nilai bawaan
	 * kegiatan ({@link KegiatanKesiswaan#getJabatanKegiatanKesiswaan()}) <b>disalin ke field ini</b>,
	 * lalu dinormalkan {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis balik
	 * sekali lagi.</p>
	 *
	 * <p><b>Efek samping yang harus disadari:</b> penyalinan itu bukan sekadar nilai kembalian.
	 * Karena Hibernate memakai property access dan entity ini {@code dynamicUpdate}, sekadar
	 * <i>menampilkan</i> daftar peserta sudah cukup untuk membuat nilai bawaan kegiatan
	 * <b>termaterialisasi permanen</b> ke kolom {@code jabatan_kegiatan_kesiswaan} baris ini pada
	 * {@code flush} berikutnya. Sesudah itu, mengubah jabatan bawaan di tingkat kegiatan tidak lagi
	 * mempengaruhi peserta lama — mereka "membeku" pada nilai yang tersalin, tanpa layar peninjauan
	 * dan tanpa jejak niat pengguna (revisi Envers akan mencatatnya seolah petugas yang mengubah).</p>
	 *
	 * <p>Perhatikan pula cabang pewarisan membaca <b>field mentah</b> {@code kegiatanKesiswaan},
	 * bukan {@link #getKegiatanKesiswaan()}. Untuk entity yang dimuat Hibernate hal ini aman
	 * (field berisi proxy non-null), tetapi pada objek yang dirakit manual dan belum diisi
	 * kegiatannya, pewarisan diam-diam dilewati.</p>
	 *
	 * <p>Daftar pilihan pada UI dibatasi koleksi milik {@link DetailKelompokKegiatanKesiswaan}
	 * induk kegiatan, bukan seluruh isi katalog.</p>
	 *
	 * @return jabatan/peran peserta; {@code null} bila baris maupun kegiatan induknya tidak
	 *         menetapkan jabatan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_kegiatan_kesiswaan", nullable = true)
	public JabatanKegiatanKesiswaan getJabatanKegiatanKesiswaan() {
		if (kegiatanKesiswaan != null && jabatanKegiatanKesiswaan == null) {
			jabatanKegiatanKesiswaan = kegiatanKesiswaan.getJabatanKegiatanKesiswaan();
		}
		jabatanKegiatanKesiswaan = check(jabatanKegiatanKesiswaan);
		return jabatanKegiatanKesiswaan;
	}

	/**
	 * Menyetel peran/capaian peserta.
	 *
	 * <p>Dipanggil dari listener {@code onChange} combobox pada kedua layar peserta, yang langsung
	 * menyusul dengan {@code Common.refreshUpdate(...)} — <b>tidak ada tombol Simpan</b>: memilih
	 * item combobox sudah menulis ke database. Nilai {@code null} (pilihan dikosongkan) diterima,
	 * tetapi akan langsung "diisi ulang" oleh pewarisan di
	 * {@link #getJabatanKegiatanKesiswaan()} pada pembacaan berikutnya.</p>
	 *
	 * @param jabatanKegiatanKesiswaan jabatan/peran peserta; boleh {@code null}
	 */
	public void setJabatanKegiatanKesiswaan(JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan) {
		this.jabatanKegiatanKesiswaan = jabatanKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan catatan bebas per peserta apa adanya.
	 *
	 * <p>Dipetakan {@code columnDefinition = "text"} sehingga panjangnya tidak dibatasi. Diedit
	 * inline lewat {@code MyTextbox} dua baris pada grid peserta dan ikut terbawa ke ekspor Excel
	 * "Download"/"Download Persetujuan Siswa".</p>
	 *
	 * <p>Berbeda dari {@link ais.database.model.GeneralValueObject#getKeterangan()} yang
	 * mengembalikan {@code ""} untuk nilai kosong, override ini mengembalikan {@code null} apa
	 * adanya. Konsekuensinya cabang {@code keterangan} pada
	 * {@link ais.database.model.GeneralValueObject#compareTo(ais.database.model.GeneralValueObject)}
	 * bisa dilewati untuk entity ini.</p>
	 *
	 * @return catatan bebas, atau {@code null} bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas per peserta.
	 *
	 * <p>Sama seperti combobox jabatan/skala, perubahan pada textbox keterangan langsung disimpan
	 * lewat {@code Common.refreshUpdate(...)} tanpa tombol Simpan. Tidak ada pembersihan/escaping
	 * di sini; nilai dipakai apa adanya oleh renderer dan ekspor Excel.</p>
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status persetujuan keikutsertaan, <b>dipaksa mengikuti status kegiatan
	 * induk</b> dan tidak pernah {@code null}.
	 *
	 * <p>Aturannya dua lapis:</p>
	 * <ol>
	 *   <li>Bila kegiatan induk ada dan statusnya <b>bukan</b> {@link PrestasiSiswa#DISETUJUI},
	 *   field {@code persetujuan} <b>ditimpa {@code false}</b> — bukan sekadar dilaporkan
	 *   {@code false}, tetapi benar-benar ditulis ke field.</li>
	 *   <li>Nilai {@code null} dilaporkan sebagai {@code false} (coalesce), sehingga pemanggil
	 *   dapat meng-unbox hasilnya tanpa memeriksa {@code null} — dan memang seluruh layar
	 *   memakainya langsung sebagai {@code boolean}.</li>
	 * </ol>
	 *
	 * <p><b>PERINGATAN — getter destruktif.</b> Lapis pertama adalah penulisan permanen: cukup
	 * dengan membuka daftar peserta sebuah kegiatan yang statusnya dikembalikan dari "Disetujui"
	 * ke "Sedang diproses"/"Ditolak", seluruh persetujuan peserta yang sudah pernah diberikan
	 * petugas akan menjadi {@code false} dan ikut tersimpan pada {@code flush} berikutnya.
	 * Menyetujui ulang kegiatan <b>tidak</b> memulihkan nilai lama satu per satu; yang terjadi
	 * adalah persetujuan massal ({@code KegiatanKesiswaanAction} menyetel {@code true} untuk semua
	 * peserta saat kegiatan disetujui), sehingga peserta yang sengaja <i>tidak</i> disetujui
	 * petugas ikut ter-{@code true}. Hasil bersihnya: keputusan per-peserta bisa hilang tanpa jejak
	 * niat pengguna.</p>
	 *
	 * <p>Nilai ini bukan sekadar tampilan — ia mengunci form (combobox/datebox/textbox
	 * di-{@code disable}), menyembunyikan tombol Hapus, menjadi syarat munculnya tombol cetak
	 * Sertifikat, dan menjadi filter {@code where ... and aaa.persetujuan} pada dashboard rekap.</p>
	 *
	 * @return {@code true} bila keikutsertaan disetujui; {@code false} bila belum, ditolak, atau
	 *         kegiatan induknya belum berstatus Disetujui
	 */
	public Boolean getPersetujuan() {
		if (kegiatanKesiswaan != null && !kegiatanKesiswaan.getStatus().equals(PrestasiSiswa.DISETUJUI)) {
			persetujuan = false;
		}
		return persetujuan == null ? false : persetujuan;
	}

	/**
	 * Menyetel status persetujuan keikutsertaan.
	 *
	 * <p>Pemanggil yang tercatat: checkbox "Setujui" pada grid peserta (langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)}), kolom persetujuan pada unggah Excel, dan
	 * penyetelan massal {@code true} untuk seluruh peserta ketika kegiatan induk disetujui di
	 * {@code KegiatanKesiswaanAction}.</p>
	 *
	 * <p>Nilai yang disetel di sini <b>tidak permanen</b> selama kegiatan induk belum berstatus
	 * Disetujui: pembacaan berikutnya lewat {@link #getPersetujuan()} akan menimpanya kembali
	 * menjadi {@code false}.</p>
	 *
	 * @param persetujuan {@code true} untuk menyetujui; {@code null} diperlakukan sebagai
	 *                    {@code false} saat dibaca
	 */
	public void setPersetujuan(Boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Mengembalikan tingkat/skala keikutsertaan ({@link SkalaKegiatanKesiswaan}), dengan
	 * <b>pewarisan otomatis dari kegiatan induk</b>.
	 *
	 * <p>Mekanismenya identik dengan {@link #getJabatanKegiatanKesiswaan()}: bila field kosong dan
	 * kegiatan tersedia, {@link KegiatanKesiswaan#getSkalaKegiatanKesiswaan()} disalin ke field ini
	 * lalu dinormalkan dan ditulis balik. Semua peringatan di sana berlaku sama — pembacaan biasa
	 * dapat memateralisasi nilai bawaan secara permanen ke kolom
	 * {@code skala_kegiatan_kesiswaan} baris ini.</p>
	 *
	 * <p>Kolom inilah yang di-{@code group} dashboard varian "Berdasar Skala"
	 * ({@code aaa.skala_kegiatan_kesiswaan}), sehingga materialisasi tersebut ikut mengubah angka
	 * rekap untuk peserta lama.</p>
	 *
	 * @return skala keikutsertaan; {@code null} bila baris maupun kegiatan induknya tidak
	 *         menetapkan skala
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skala_kegiatan_kesiswaan", nullable = true)
	public SkalaKegiatanKesiswaan getSkalaKegiatanKesiswaan() {
		if (kegiatanKesiswaan != null && skalaKegiatanKesiswaan == null) {
			skalaKegiatanKesiswaan = kegiatanKesiswaan.getSkalaKegiatanKesiswaan();
		}
		skalaKegiatanKesiswaan = check(skalaKegiatanKesiswaan);
		return skalaKegiatanKesiswaan;
	}

	/**
	 * Menyetel tingkat/skala keikutsertaan.
	 *
	 * <p>Dipanggil dari listener {@code onChange} combobox skala pada kedua layar peserta dan
	 * langsung disimpan tanpa tombol Simpan. Sama seperti jabatan, mengosongkan pilihan tidak
	 * bertahan: {@link #getSkalaKegiatanKesiswaan()} akan mengisinya kembali dari bawaan
	 * kegiatan.</p>
	 *
	 * @param skalaKegiatanKesiswaan skala keikutsertaan; boleh {@code null}
	 */
	public void setSkalaKegiatanKesiswaan(SkalaKegiatanKesiswaan skalaKegiatanKesiswaan) {
		this.skalaKegiatanKesiswaan = skalaKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan tanggal mulai keikutsertaan, dengan <b>fallback ke tanggal mulai kegiatan</b>
	 * bila kolom baris ini kosong.
	 *
	 * <p>Dipetakan {@link TemporalType#DATE} (tanpa jam). Berbeda dari getter jabatan/skala,
	 * fallback di sini <b>tidak</b> ditulis balik ke field — nilai hanya dikembalikan.</p>
	 *
	 * <p><b>Konsekuensi penting (terverifikasi di kode).</b> Karena kolom tetap NULL di database
	 * sementara UI selalu menampilkan tanggal, setiap query yang menyaring langsung ke kolom akan
	 * menjatuhkan baris tersebut tanpa peringatan. Ini terjadi pada
	 * {@code DashboardRekapKegiatanKesiswaan} di dua tempat sekaligus: filter Criteria
	 * {@code Restrictions.between("mulai", ...)} yang menentukan kolom apa saja yang muncul, dan
	 * klausa native {@code and aaa.mulai between date(...) and date(...)} yang menentukan angkanya.
	 * Peserta yang tanggalnya "diwarisi" dari kegiatan karena itu <b>tidak pernah terhitung</b> di
	 * keempat dashboard rekap kegiatan kesiswaan, walaupun di layar tampak punya tanggal.</p>
	 *
	 * <p>Peserta yang dibuat lewat {@code AmbilDataSiswaForKegiatanKesiswaanHelper} maupun lewat
	 * pengajuan kegiatan oleh siswa memang tidak pernah diisi tanggalnya, sehingga kondisi ini
	 * bukan kasus tepi.</p>
	 *
	 * @return tanggal mulai baris ini bila diisi; jika tidak, tanggal mulai kegiatan induk; atau
	 *         {@code null} bila keduanya kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai == null ? (kegiatanKesiswaan == null ? null : kegiatanKesiswaan.getMulai()) : mulai;
	}

	/**
	 * Menyetel tanggal mulai keikutsertaan peserta ini.
	 *
	 * <p>Diisi dari {@code MyDatebox} pada grid peserta (langsung tersimpan lewat
	 * {@code Common.refreshUpdate(...)}) dan dari kolom 5 berkas unggah Excel di
	 * {@code KegiatanKesiswaanAction}. Mengisinya adalah satu-satunya cara membuat baris terlihat
	 * di dashboard rekap (lihat {@link #getMulai()}).</p>
	 *
	 * @param mulai tanggal mulai; {@code null} berarti mengikuti tanggal kegiatan saat dibaca —
	 *              tetapi kolom di database tetap NULL
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Menyetel tanggal selesai keikutsertaan peserta ini.
	 *
	 * <p>Sumber pengisian sama dengan {@link #setMulai(Date)}: datebox pada grid peserta dan
	 * kolom 6 berkas unggah Excel. Tidak ada validasi bahwa {@code sampai} berada setelah
	 * {@code mulai}, dan tidak ada validasi bahwa rentang peserta berada di dalam rentang
	 * kegiatan.</p>
	 *
	 * @param sampai tanggal selesai; {@code null} berarti mengikuti tanggal kegiatan saat dibaca
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan tanggal selesai keikutsertaan, dengan <b>fallback ke tanggal selesai
	 * kegiatan</b> bila kolom baris ini kosong.
	 *
	 * <p>Dipetakan {@link TemporalType#DATE}; fallback tidak ditulis balik, persis seperti
	 * {@link #getMulai()}. Kolom {@code sampai} tidak dipakai sebagai filter oleh dashboard rekap
	 * (yang menyaring hanya {@code mulai}), sehingga dampak NULL-nya sebatas tampilan dan ekspor.</p>
	 *
	 * @return tanggal selesai baris ini bila diisi; jika tidak, tanggal selesai kegiatan induk;
	 *         atau {@code null} bila keduanya kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai == null ? (kegiatanKesiswaan == null ? null : kegiatanKesiswaan.getSampai()) : sampai;
	}

}
