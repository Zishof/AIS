package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Master <b>kategori (rentang) penghasilan orang tua siswa</b> &mdash; tabel
 * {@code sekolah.penghasilan_orang_tua_siswa}.
 *
 * <h3>Peran sebenarnya: kamus rentang, BUKAN data penghasilan per keluarga</h3>
 *
 * <p>Nama kelas ini mudah disalahpahami. Baris di tabel ini <b>tidak</b> memuat penghasilan
 * keluarga siapa pun; ia hanya mendefinisikan <i>pilihan kategori</i> yang nanti dipilih di
 * formulir biodata, mis. &laquo;Rp. 1.000.000 &ndash; Rp. 1.999.999&raquo; dengan
 * {@link #getBatasBawah()} {@code = 1000000} dan {@link #getBatasAtas()} {@code = 1999999}.
 * Seluruh isi tabel adalah <b>label dan angka rentang</b>, tanpa satu pun kolom yang menunjuk
 * siswa, orang tua, kelas, sekolah, atau yayasan.</p>
 *
 * <p>Data sosial-ekonomi yang sesungguhnya sensitif &mdash; yaitu <i>siswa mana masuk rentang
 * yang mana</i> &mdash; tersimpan sebagai kolom foreign key di entity <b>lain</b>:</p>
 * <ul>
 *   <li>{@link Siswa#getPenghasilanAyah()}, {@link Siswa#getPenghasilanIbu()},
 *   {@link Siswa#getPenghasilanWali()};</li>
 *   <li>{@link CalonSiswa#getPenghasilanAyah()}, {@link CalonSiswa#getPenghasilanIbu()},
 *   {@link CalonSiswa#getPenghasilanWali()}.</li>
 * </ul>
 * <p>Konsekuensi penting bagi audit privasi: mengunci kelas ini <b>tidak</b> melindungi data
 * sosial-ekonomi keluarga, dan membocorkan kelas ini <b>tidak</b> membocorkannya &mdash; jangkauan
 * kebocoran ditentukan oleh gerbang akses di {@code Siswa}/{@code CalonSiswa}. Lihat butir
 * &laquo;Catatan akses&raquo; di bawah.</p>
 *
 * <h3>Siapa yang memakai kelas ini</h3>
 *
 * <ol>
 *   <li><b>Layar master</b> {@code ais.action.master.sekolah.PenghasilanOrangTuaSiswaAction}
 *   (ZUL {@code pages/master/sekolah/penghasilan_orang_tua_siswa.zul}) &mdash; satu-satunya
 *   penulis: tambah/ubah/hapus rentang, dengan validasi nama tidak boleh kosong dan tidak boleh
 *   duplikat.</li>
 *   <li><b>Combobox biodata</b> lewat {@code Common.insertCombo(..., "nama",
 *   PenghasilanOrangTuaSiswa.class)} di {@code SiswaAction} (3 kali: ayah/ibu/wali),
 *   {@code CalonSiswaAction} (3 kali), serta formulir pendaftaran online
 *   {@code psb.form.PPDB1} dan {@code psb.form.PPDB2} (3 kali masing-masing).</li>
 *   <li><b>Preload memori</b>: {@code InitData} mendaftarkan kelas ini ke
 *   {@code InitDataHelper.initData(Class)}. Ini <b>hanya</b> memuat baris yang sudah ada ke cache
 *   {@code MemoryCacheUtil}; tidak ada satu pun baris bawaan yang disemai (lihat butir 1 di
 *   bawah).</li>
 *   <li><b>Layar CRUD reflektif</b> {@code DynamicJspCrudGenerator.generate(
 *   PenghasilanOrangTuaSiswa.class)} yang dirender oleh
 *   {@code WEB-INF/baru/modul/pagesmastersekolahpenghasilanorangtuasiswazul/index.jsp}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Tabel ini mulai KOSONG &mdash; tidak ada rentang bawaan.</b> Kembaran sisi perguruan
 *   tinggi {@link ais.database.model.Penghasilan} disemai enam rentang standar Feeder
 *   (&laquo;Kurang dari Rp. 500,000&raquo; &hellip; &laquo;Lebih dari Rp. 20,000,000&raquo;,
 *   {@code feeder} 11&ndash;16) oleh {@code InitDataHelper}. Blok penyemaian itu <b>hanya</b>
 *   menyentuh kelas PT; tidak ada padanannya untuk kelas sekolah. Jadi sampai seorang admin
 *   mengetikkan rentang lewat layar master, seluruh combobox &laquo;Penghasilan
 *   Ayah/Ibu/Wali&raquo; di biodata siswa, biodata calon siswa, dan formulir PPDB <b>tampil
 *   kosong</b>.</li>
 *   <li><b>{@link #getFeeder()} adalah kolom mati.</b> Ia disalin dari kelas PT (tempat
 *   {@code feeder} dipakai sebagai kunci idempoten penyemaian dan padanan kode Feeder Dikti),
 *   tetapi di sisi sekolah tidak ada satu pun penulis: layar master tidak menampilkannya,
 *   penyemaian tidak ada, dan tidak ada importer Feeder untuk jenjang sekolah. Nilainya selalu
 *   {@code null}.</li>
 *   <li><b>{@link #getNama()} di sini POLOS &mdash; berbeda tajam dari kembaran PT.</b> Pada
 *   {@link ais.database.model.Penghasilan} getter {@code nama} bersifat <i>destruktif</i>: ia
 *   membangkitkan ulang nama dari batas bawah/atas dan menuliskannya balik ke database, sehingga
 *   label yang diketik pengguna tidak pernah bertahan. Di kelas ini {@code getNama()} hanya
 *   melakukan {@code trim()} dan tidak menyentuh field, jadi label bebas yang diketik admin
 *   (mis. &laquo;Tidak berpenghasilan&raquo;) <b>bertahan apa adanya</b>. Jangan menyamakan
 *   perilaku kedua kelas hanya karena strukturnya kembar.</li>
 *   <li><b>{@link #getBatasBawah()} dan {@link #getBatasAtas()} TETAP menulis balik.</b> Keduanya
 *   mengganti {@code null} menjadi {@code 0.0} <i>pada field</i>, bukan sekadar pada nilai
 *   kembalian. Karena pemetaan kelas ini memakai <i>property access</i> (anotasi menempel pada
 *   getter), Hibernate membaca nilai baru itu saat dirty-check &mdash; sehingga sekadar
 *   <b>membaca</b> batas sebuah baris di dalam sesi terkelola yang ter-flush akan
 *   <b>menuliskan 0 ke database</b>. Jalur nyata yang memicunya adalah layar master:
 *   {@code PenghasilanOrangTuaSiswaRenderer} memanggil kedua getter untuk setiap baris pada
 *   session {@code HibernateUtil.currentSession()}. Jalur combobox tidak terdampak karena
 *   {@code CommonComboInsertHelper} memakai session terpisah yang di-{@code clear()} sebelum
 *   ditutup.</li>
 *   <li><b>Rentang terbuka teratas akan tersortir paling depan.</b> {@link #compareTo(
 *   GeneralValueObject)} mengurutkan berdasarkan {@code batasAtas}, dan combobox biodata
 *   memakai {@code Collections.sort(list)} yang memanggil {@code compareTo} itu. Bila admin
 *   menyandikan &laquo;Lebih dari Rp. 20.000.000&raquo; dengan {@code batasAtas = 0} (persis
 *   konvensi data awal sisi PT), kategori <i>tertinggi</i> akan muncul di urutan <i>paling
 *   rendah</i>. Tidak ada validasi yang mencegahnya.</li>
 *   <li><b>Cabang <i>fallback</i> di {@link #compareTo(GeneralValueObject)} adalah kode mati.</b>
 *   Karena {@link #getBatasAtas()} tidak pernah mengembalikan {@code null} (butir 4), perbandingan
 *   antar dua {@code PenghasilanOrangTuaSiswa} selalu berhenti di cabang pertama. Cabang
 *   {@code nomorUrut}/{@code nim}/{@code nama}/{@code keterangan} hanya terjangkau bila
 *   {@code arg0} bertipe lain &mdash; tetapi dalam kasus itu cast {@code (PenghasilanOrangTuaSiswa)
 *   arg0} sudah melempar {@code ClassCastException} yang ditelan {@code catch} sehingga method
 *   mengembalikan {@code 0}.</li>
 *   <li><b>{@link #toString()} membaca field mentah, bukan getter.</b> Ia mencetak
 *   {@code "<id>-<nama>"} langsung dari field {@code nama} sehingga <b>tidak</b> ikut
 *   {@code trim()} seperti {@link #getNama()}. Untuk nama yang berspasi di ujung, keduanya memberi
 *   jawaban berbeda.</li>
 *   <li><b>Validasi keunikan nama di layar master bersifat <i>case-sensitive</i> dan sadar
 *   spasi.</b> {@code checkNamaPenghasilanOrangTuaSiswa()} memakai {@code Restrictions.eq("nama",
 *   nilai.trim())}. &laquo;Rendah&raquo; dan &laquo;rendah&raquo; dianggap dua nama berbeda, dan
 *   dua rentang angka yang identik persis (duplikat sesungguhnya) lolos tanpa peringatan selama
 *   labelnya berbeda.</li>
 *   <li><b>Tidak ada kolom {@code aktif}.</b> Berbeda dari saudaranya
 *   {@code PendidikanOrangTuaSiswa} &mdash; yang di layar biodata dipanggil dengan
 *   {@code Restrictions.eq("aktif", true)} &mdash; kelas ini tidak punya penanda aktif sama
 *   sekali, sehingga combobox-nya dipanggil tanpa filter. Rentang yang sudah tidak dipakai
 *   <b>tidak bisa dipensiunkan</b>; satu-satunya cara menyembunyikannya adalah menghapus barisnya,
 *   dan penghapusan akan ditolak bila masih direferensikan siswa/calon siswa (ditangkap sebagai
 *   pesan &laquo;berelasi dengan data lainnya&raquo; di layar master).</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash;
 *   bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate sama sekali
 *   tidak memetakan properti kelas induk. Setiap entity turunan wajib mendeklarasikan sendiri
 *   kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi tidak
 *   ada {@code @PrePersist}, jadi {@code oleh}/{@code olehId} hanya terisi saat baris di-UPDATE,
 *   bukan saat dibuat.</li>
 *   <li><b>{@code serialVersionUID} identik dengan milik {@link ais.database.model.Penghasilan}</b>
 *   &mdash; sisa salin-tempel saat kelas sekolah dibuat dari kelas PT. Tidak berpengaruh apa pun
 *   karena serialisasi Java selalu dilakukan per kelas.</li>
 * </ol>
 *
 * <h3>Catatan akses (hasil penelusuran, bukan klaim umum)</h3>
 *
 * <p><b>Layar master kelas ini bergerbang benar.</b>
 * {@code PenghasilanOrangTuaSiswaAction.doBeforeCompose} memanggil
 * {@code Common.doCheckSecurity()}, {@code doAfterCompose} menolak sesi tanpa {@code usersTemp}
 * atau tanpa {@code CommonPrivilages.READ}, dan tombol Tambah/Ubah/Hapus masing-masing bergantung
 * pada {@code CREATE}/{@code UPDATE}/{@code DELETE}. Tidak ada penyaringan
 * sekolah/yayasan/orang tua di {@code initCriteria()} &mdash; tetapi di sini itu <b>bukan</b>
 * cacat: tabel ini memang kamus global tanpa kolom kepemilikan apa pun, dan isinya bukan data
 * pribadi. Khususnya, {@code PenghasilanOrangTuaSiswaAction} <b>tidak memanggil</b>
 * {@code OrangTua.ambilAnakSiswa()} sama sekali, jadi pola <i>fail-open</i> orang tua yang
 * berulang di modul sekolah tidak berlaku untuk kelas ini.</p>
 *
 * <p><b>Daftar rentang ini memang terbaca sebelum login</b> &mdash; formulir pendaftaran online
 * {@code PPDB1}/{@code PPDB2} tidak punya gerbang keamanan (memang dirancang publik) dan mengisi
 * combobox penghasilan dari tabel ini. Yang terekspos hanya label dan angka rentang, bukan data
 * keluarga siapa pun.</p>
 *
 * <p><b>Titik yang benar-benar perlu diawasi ada di hilir, bukan di kelas ini:</b> layar
 * {@code SiswaAction} &mdash; yang menampilkan dan menyunting {@code penghasilanAyah}/
 * {@code penghasilanIbu}/{@code penghasilanWali} bersama NIK dan nomor telepon orang tua &mdash;
 * menyusun {@code initCriteria()} <b>tanpa</b> pembatas sekolah/yayasan wajib (kolom
 * {@code sekolah}/{@code yayasan} hanya tersedia sebagai filter pencarian opsional yang dipilih
 * pengguna) dan tanpa pembatas {@code orangTua}. Lihat javadoc {@code Siswa} untuk pembahasan
 * lengkapnya; jangan menyimpulkan dari kelas ini bahwa data sosial-ekonomi keluarga sudah
 * terlindungi.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.Penghasilan
 * @see PendidikanOrangTuaSiswa
 * @see PekerjaanOrtuSiswa
 * @see Siswa#getPenghasilanAyah()
 * @see CalonSiswa#getPenghasilanAyah()
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "penghasilan_orang_tua_siswa")
public class PenghasilanOrangTuaSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sama persis dengan milik
	 * {@link ais.database.model.Penghasilan} &mdash; sisa salin-tempel saat kelas sekolah dibuat
	 * dari kelas perguruan tinggi. Tidak berdampak apa pun karena serialisasi selalu dilakukan
	 * per kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama {@code sekolah.penghasilan_orang_tua_siswa.id}, {@code IDENTITY} (di-generate database). */
	private Long id;

	/** Nama pengguna terakhir yang meng-UPDATE baris ini; diisi {@link #onUpdate()}. */
	private String oleh;

	/** Id pengguna terakhir yang meng-UPDATE baris ini; diisi {@link #onUpdate()}. */
	private String olehId;

	/** @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. Nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> &mdash; nilai lama dipertahankan, bukan dikosongkan. Perilaku ini
	 * disengaja agar jejak audit yang sudah ada tidak terhapus oleh pemanggil yang kebetulan
	 * mengirim nilai kosong.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi pembuat baris tidak tercatat di
	 * kolom-kolom ini (lihat javadoc kelas). Pada baris deklarasi yang sama juga dideklarasikan
	 * field {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.
	 *
	 * <p>Perlu dibaca bersama kuirk {@link #getBatasBawah()}/{@link #getBatasAtas()}: karena
	 * membaca batas yang masih {@code null} sudah membuat baris menjadi <i>dirty</i>, UPDATE yang
	 * tidak diniatkan siapa pun tetap dapat memicu callback ini dan mengganti
	 * {@code oleh}/{@code tanggal_dirubah}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir baris ini */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi TIMESTAMP) */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca <b>field</b> {@code nama} secara langsung, bukan {@link #getNama()}, sehingga
	 * hasilnya <b>tidak</b> ikut di-{@code trim()}. Untuk nama yang berspasi di ujung,
	 * {@code toString()} dan {@link #getNama()} memberi jawaban berbeda untuk objek yang sama.</p>
	 *
	 * @return gabungan id dan nama, mis. {@code "3-Rp. 1.000.000 - Rp. 1.999.999"}; bagian id
	 *         berbunyi {@code "null"} untuk objek yang belum tersimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Label kategori yang tampil di combobox biodata, kolom {@code nama} (wajib isi, maks. 255 karakter). */
	private String nama;

	/** Catatan bebas admin, kolom {@code keterangan} (boleh {@code null}, tanpa batas panjang). */
	private String keterangan;

	/** Batas bawah rentang penghasilan dalam rupiah; lihat {@link #getBatasBawah()} soal penulisan balik {@code 0.0}. */
	private Double batasBawah;

	/** Batas atas rentang penghasilan dalam rupiah; menjadi kunci urutan di {@link #compareTo(GeneralValueObject)}. */
	private Double batasAtas;

	/**
	 * Padanan kode Feeder untuk rentang ini &mdash; <b>kolom mati di sisi sekolah</b>. Disalin dari
	 * {@link ais.database.model.Penghasilan} (tempat nilai 11&ndash;16 dipakai sebagai kunci
	 * idempoten penyemaian), tetapi di modul sekolah tidak ada penyemaian, tidak ada importer
	 * Feeder, dan layar master tidak menampilkan ruas ini &mdash; sehingga nilainya selalu
	 * {@code null}.
	 */
	private Long feeder;

	/**
	 * Urutan alami: <b>menaik menurut {@link #getBatasAtas()}</b>.
	 *
	 * <p>Dipakai lewat {@code Collections.sort(list)} di {@code CommonComboInsertHelper} saat
	 * mengisi combobox &laquo;Penghasilan Ayah/Ibu/Wali&raquo; pada {@code SiswaAction},
	 * {@code CalonSiswaAction}, {@code PPDB1}, dan {@code PPDB2}.</p>
	 *
	 * <p><b>Cabang fallback adalah kode mati.</b> Karena {@link #getBatasAtas()} tidak pernah
	 * mengembalikan {@code null} (ia mengganti {@code null} menjadi {@code 0.0}), perbandingan
	 * antar dua {@code PenghasilanOrangTuaSiswa} selalu berhenti di cabang pertama. Cabang
	 * {@code nomorUrut}/{@code nim}/{@code nama}/{@code keterangan} hanya terjangkau bila
	 * {@code arg0} bertipe lain &mdash; namun dalam kasus itu cast di cabang pertama sudah melempar
	 * {@code ClassCastException} yang ditangkap dan membuat method mengembalikan {@code 0}.</p>
	 *
	 * <p><b>Jebakan urutan.</b> Rentang terbuka teratas yang disandikan dengan
	 * {@code batasAtas = 0} (konvensi data awal sisi perguruan tinggi untuk &laquo;Lebih dari
	 * Rp. 20.000.000&raquo;) akan tersortir <i>paling depan</i>, bukan paling belakang. Tidak ada
	 * validasi di layar master yang mencegah admin memakai konvensi itu.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getBatasAtas()} di dalam method ini dapat menulis
	 * {@code 0.0} ke field bila sebelumnya {@code null} &mdash; jadi mengurutkan koleksi entity
	 * terkelola ikut memicu kuirk penulisan balik yang dibahas di javadoc kelas.</p>
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch} yang mencatat exception ke
	 * {@code ErrorAuditUtil} lalu mengembalikan {@code 0} (dianggap setara), sehingga
	 * pengurutan tidak pernah melempar ke pemanggil.</p>
	 *
	 * @param arg0 objek pembanding; diharapkan bertipe {@code PenghasilanOrangTuaSiswa}
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} bila tidak ada
	 *         pasangan ruas yang dapat dibandingkan atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (getBatasAtas() != null && ((PenghasilanOrangTuaSiswa) arg0).getBatasAtas() != null) {
				return getBatasAtas().compareTo(((PenghasilanOrangTuaSiswa) arg0).getBatasAtas());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PenghasilanOrangTuaSiswa.java:93");

		}

		return 0;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate. Dipakai juga oleh
	 * {@code PenghasilanOrangTuaSiswaAction.onAdd(Event)} untuk menyiapkan formulir rentang baru.
	 * Seluruh ruas dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang langsung diisi
	 * waktu server.
	 */
	public PenghasilanOrangTuaSiswa() {
	}

	/** @return kunci utama baris ini, atau {@code null} bila belum pernah disimpan */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama; normalnya hanya disetel oleh Hibernate */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Label kategori yang tampil di grid master dan di combobox biodata.
	 *
	 * <p>Berbeda dari kembaran perguruan tinggi {@code Penghasilan.getNama()} yang membangkitkan
	 * ulang nama dari batas bawah/atas dan menuliskannya balik ke database, getter ini
	 * <b>tidak destruktif</b>: ia hanya me-{@code trim()} nilai kembalian dan tidak menyentuh
	 * field, sehingga label bebas yang diketik admin bertahan apa adanya.</p>
	 *
	 * @return nama kategori tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel label kategori. Nilai disimpan apa adanya (tanpa {@code trim()}); pemangkasan baru
	 * terjadi saat dibaca lewat {@link #getNama()}. Dipanggil dari
	 * {@code PenghasilanOrangTuaSiswaAction.onSave(Event)} setelah validasi wajib isi dan validasi
	 * duplikat nama.
	 *
	 * @param nama label kategori, mis. {@code "Rp. 1.000.000 - Rp. 1.999.999"}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan bebas admin untuk rentang ini, atau {@code null} bila kosong */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan bebas; boleh {@code null} */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return padanan kode Feeder &mdash; <b>selalu {@code null}</b> di modul sekolah karena tidak
	 *         ada penulis sama sekali (lihat javadoc field {@link #feeder})
	 */
	public Long getFeeder() {
		return feeder;
	}

	/**
	 * Menyetel padanan kode Feeder. Tidak ada pemanggil di seluruh basis kode sisi sekolah;
	 * disediakan hanya demi kesejajaran dengan {@link ais.database.model.Penghasilan}.
	 *
	 * @param feeder kode Feeder
	 */
	public void setFeeder(Long feeder) {
		this.feeder = feeder;
	}

	/**
	 * Batas bawah rentang penghasilan dalam rupiah.
	 *
	 * <p><b>Efek samping &mdash; getter ini MENULIS.</b> Bila field masih {@code null}, ia
	 * mengganti isinya menjadi {@code 0.0} <i>pada field</i>, bukan hanya pada nilai kembalian.
	 * Karena pemetaan kelas ini memakai <i>property access</i>, nilai baru itulah yang dibaca
	 * Hibernate saat dirty-check &mdash; sehingga membaca baris terkelola di sesi yang ter-flush
	 * akan menuliskan {@code 0} ke database dan memicu {@link #onUpdate()}. Jalur nyata yang
	 * memicunya adalah renderer grid layar master. Jalur combobox biodata aman karena memakai
	 * session terpisah yang di-{@code clear()} sebelum ditutup.</p>
	 *
	 * @return batas bawah rentang; tidak pernah {@code null} (nilai {@code null} diganti
	 *         {@code 0.0})
	 */
	public Double getBatasBawah() {
		if (batasBawah == null) {
			batasBawah = 0.0;
		}
		return batasBawah;
	}

	/**
	 * Menyetel batas bawah rentang. Dipanggil dari
	 * {@code PenghasilanOrangTuaSiswaAction.onSave(Event)} dengan nilai
	 * {@code MyDoublebox batasBawah}. Tidak ada validasi bahwa batas bawah &le; batas atas.
	 *
	 * @param batasBawah batas bawah dalam rupiah
	 */
	public void setBatasBawah(Double batasBawah) {
		this.batasBawah = batasBawah;
	}

	/**
	 * Batas atas rentang penghasilan dalam rupiah &mdash; sekaligus kunci urutan alami kelas ini
	 * ({@link #compareTo(GeneralValueObject)}).
	 *
	 * <p><b>Efek samping &mdash; getter ini MENULIS.</b> Sama persis dengan
	 * {@link #getBatasBawah()}: {@code null} diganti {@code 0.0} pada field dan ikut ter-flush.
	 * Karena {@code compareTo} memanggil getter ini, sekadar <i>mengurutkan</i> koleksi entity
	 * terkelola sudah cukup untuk memicunya.</p>
	 *
	 * @return batas atas rentang; tidak pernah {@code null} (nilai {@code null} diganti
	 *         {@code 0.0})
	 */
	public Double getBatasAtas() {
		if (batasAtas == null) {
			batasAtas = 0.0;
		}
		return batasAtas;
	}

	/**
	 * Menyetel batas atas rentang. Dipanggil dari
	 * {@code PenghasilanOrangTuaSiswaAction.onSave(Event)} dengan nilai
	 * {@code MyDoublebox batasAtas}. Perhatikan jebakan urutan yang dibahas di
	 * {@link #compareTo(GeneralValueObject)}: menyandikan rentang terbuka teratas dengan
	 * {@code 0} akan membuatnya tampil paling depan di combobox.
	 *
	 * @param batasAtas batas atas dalam rupiah
	 */
	public void setBatasAtas(Double batasAtas) {
		this.batasAtas = batasAtas;
	}

}
