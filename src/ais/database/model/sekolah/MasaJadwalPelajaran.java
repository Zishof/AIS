package ais.database.model.sekolah;

// Generated Apr 12, 2010 11:30:55 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.VoKunci;
import ais.database.model.sop.DisposisiSop;

/**
 * Entity <b>Masa Pembelajaran</b> (tabel {@code sekolah.masa_jadwal_pelajaran}): satu
 * <i>rentang tanggal</i> ({@link #getMulai()} … {@link #getSampai()}) untuk sebuah
 * tahun ajaran + semester pada satu sekolah/yayasan, yang menjadi payung berlakunya
 * sekumpulan {@link JadwalPelajaran}.
 *
 * <p>Nama kelas ini masih memakai komentar generator lama ("JamJadwalPelajaran generated
 * by hbm2java" pada versi sebelumnya) dan nama tabelnya menyebut "jadwal pelajaran",
 * tetapi label yang dilihat pengguna di seluruh layar adalah <b>"Masa Pembelajaran"</b>
 * (lihat {@code MasaJadwalPelajaranAction.init(...)}, {@code JadwalPelajaranAction} baris
 * label "Masa Pembelajaran *", dan {@code KelasLesSiswaAction}).</p>
 *
 * <h2>Bukan "jenis", melainkan "rentang waktu"</h2>
 * <p>Entity ini <b>bukan</b> padanan atau duplikat dari
 * {@link ais.database.model.sekolah.JenisJadwalPelajaran}. Keduanya berdiri pada dimensi
 * yang berbeda dan tidak punya relasi satu sama lain:</p>
 * <ul>
 *   <li><i>Jenis</i> jadwal pelajaran menjawab <b>"jadwal macam apa"</b> — kategori/varian
 *   satu baris jadwal (dipakai antara lain sebagai atribut {@link JamPelajaran}); ia tidak
 *   punya kolom tanggal sama sekali.</li>
 *   <li><i>Masa</i> jadwal pelajaran — kelas ini — menjawab <b>"berlaku dari kapan sampai
 *   kapan"</b>. Kolom intinya adalah sepasang tanggal plus penanda tahun ajaran dan
 *   semester. Tidak ada kolom yang mengategorikan sifat jadwal.</li>
 * </ul>
 * <p>Konsekuensinya, satu instalasi lazim memiliki beberapa baris masa untuk tahun ajaran
 * yang sama (mis. potongan awal semester, periode khusus, sisa semester), dan setiap baris
 * {@link JadwalPelajaran} menunjuk tepat satu di antaranya lewat kolom
 * {@code masa_jadwal_pelajaran}.</p>
 *
 * <h2>Peran nyata di alur aplikasi (terverifikasi dari kode pemanggil)</h2>
 * <ol>
 *   <li><b>Kombobox wajib di layar jadwal.</b> {@code JadwalPelajaranAction} dan
 *   {@code KelasLesSiswaAction} mengisi kombobox "Masa Pembelajaran *" lewat
 *   {@code Common.insertCombo(..., MasaJadwalPelajaran.class, ...)} dengan penyaring
 *   tahun ajaran + semester + sekolah + {@link #getAktif()}. Penyimpanan jadwal ditolak
 *   bila kombobox ini kosong.</li>
 *   <li><b>Batas atas pembangkitan pertemuan.</b> {@code PenjadwalanSiswaHelper} menolak
 *   berjalan bila jadwal belum punya masa ("Masa pembelajaran belum dipilih di menu jadwal
 *   pelajaran"), lalu memakai {@link #getSampai()} sebagai <i>kondisi henti</i> perulangan
 *   pembuatan baris pertemuan. Mengubah tanggal {@code sampai} secara langsung mengubah
 *   berapa banyak pertemuan yang terbentuk.</li>
 *   <li><b>Gerbang penguncian nilai.</b> {@code DetailPenilaianSiswaHelper} menyembunyikan
 *   seluruh perangkat input/unggah nilai bila
 *   {@code jadwalPelajaran.getMasaJadwalPelajaran().getDikunci() != null}. Jadi mengunci
 *   satu baris masa membekukan pengisian nilai bagi <b>semua</b> jadwal yang bernaung di
 *   bawahnya.</li>
 *   <li><b>Sumber tanggal pada laporan.</b> {@code LaporanJadwalPelajaran} menyuntikkan
 *   {@link #getMulai()}/{@link #getSampai()} sebagai parameter {@code masa_mulai} dan
 *   {@code masa_sampai} dalam belasan format tanggal.</li>
 *   <li><b>Penulis balik tanggal jadwal.</b> {@code JadwalPelajaran.getTanggalMulaiJadwalPelajaran()}
 *   menimpa tanggal mulai jadwal dengan {@link #getMulai()} setiap kali dibaca, bila masa
 *   sudah terisi.</li>
 *   <li><b>Teks tampilan.</b> {@code JadwalDisplayHelper} memakai {@link #getNama()} sebagai
 *   keterangan masa pada tampilan jadwal.</li>
 * </ol>
 *
 * <h2>Sumber data: cache global, bukan query per pemakaian</h2>
 * <p>Kelas ini termasuk daftar pra-muat {@code InitData.initClasses(...)} sehingga seluruh
 * isinya berada di {@code MemoryCacheUtil} (peta {@code ConcurrentHashMap} satu JVM) dan
 * dapat diambil lewat {@code ConstantValues.ambilBerdasarClass(MasaJadwalPelajaran.class)}.
 * Kelas ini juga terdaftar pada {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}, jadi entrinya
 * tidak dibuang oleh pembersih cache berkala. Dua sifat itu penting untuk memahami
 * catatan {@link #getDefaultData()} di bawah.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()},
 *   {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *   {@link #toString()}.</li>
 *   <li><b>Penamaan:</b> {@link #getNama()}, {@link #getNamaSmt()},
 *   {@link #getKeterangan()}.</li>
 *   <li><b>Rentang waktu:</b> {@link #getMulai()}, {@link #getSampai()}.</li>
 *   <li><b>Penanda periode akademik:</b> {@link #getTahunAjaran()},
 *   {@link #getSemester()}.</li>
 *   <li><b>Cakupan organisasi:</b> {@link #getSekolah()}, {@link #getYayasan()},
 *   {@link #getProgram()}.</li>
 *   <li><b>Penanda perilaku:</b> {@link #getAktif()}, {@link #getDefaultData()}.</li>
 *   <li><b>Kontrol alur:</b> {@link #getDikunci()} (kontrak {@link VoKunci}),
 *   {@link #getDisposisiSop()} (kontrak {@code DataSop}).</li>
 * </ul>
 *
 * <h2>Hal non-obvious yang wajib diketahui sebelum menyunting</h2>
 * <ul>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   bukan bug.</b> {@link ais.database.model.GeneralValueObject} (induk terjauh, lewat
 *   {@code DataSop} dan {@link VoKunci}) adalah POJO abstrak biasa — bukan
 *   {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan
 *   properti induknya. Setiap entity konkret <b>harus</b> mendeklarasikan sendiri keempat
 *   anggota tersebut agar terpetakan. Menghapusnya akan merusak pemetaan.</li>
 *   <li><b>Akses properti (property access).</b> Karena {@code @Id} dipasang pada
 *   {@link #getId()}, Hibernate membaca/menulis lewat <i>getter</i>. Semua getter di kelas
 *   ini otomatis menjadi kolom kecuali ditandai lain — termasuk {@link #getNamaSmt()} yang
 *   sebenarnya nilai turunan dari {@link #getSemester()}.</li>
 *   <li><b>Enam getter menulis balik ke field.</b> {@link #getNama()}, {@link #getMulai()},
 *   {@link #getSampai()}, {@link #getTahunAjaran()}, {@link #getSemester()} dan
 *   {@link #getNamaSmt()} mengisi/menimpa field saat dibaca. Digabung dengan property
 *   access dan {@code dynamicUpdate}, sekadar merender satu baris di grid dapat memicu
 *   {@code UPDATE} yang tidak pernah diminta pengguna, sekaligus menjalankan
 *   {@link #onUpdate()} sehingga jejak "diubah oleh" ikut tertimpa. {@link #getNamaSmt()}
 *   adalah yang paling agresif: ia menimpa <b>tanpa syarat</b> pada setiap pembacaan.</li>
 *   <li><b>{@code sekolah}/{@code yayasan} bernilai {@code null} berarti "berlaku untuk
 *   semua".</b> Penyaring kombobox di layar jadwal ditulis sebagai
 *   {@code isNull("sekolah") OR eq("sekolah", s)}. Karena {@link #setSekolah(Sekolah)} dan
 *   {@link #setYayasan(Yayasan)} mengubah objek ber-id {@code null} menjadi {@code null},
 *   menyetel tenant dari objek transient (mis. hasil {@code SekolahUtil.getYayasan()} pada
 *   instalasi/thread yang gagal meresolusi tenant) diam-diam melebarkan cakupan baris ini
 *   ke seluruh instalasi, bukan mempersempitnya.</li>
 *   <li><b>Kolom {@code program} tidak pernah ditulis.</b> Tidak ada satu pun pemanggil
 *   {@link #setProgram(String)} di luar kelas ini, sedangkan
 *   {@code MasaJadwalPelajaranAction.initCriteria()} menyediakan penyaring pencarian
 *   "Program" berupa {@code Restrictions.eq("program", ...)}. Akibatnya penyaring itu
 *   selalu mengembalikan daftar kosong.</li>
 *   <li><b>Layar masternya disisipkan sebagai tab layar lain.</b>
 *   {@code JadwalPelajaranAction.onMasa()} dan {@code KelasLesSiswaAction.onMasa()}
 *   menyisipkan {@code /pages/master/sekolah/masa_jadwal_pelajaran.zul} lewat
 *   {@code MyInclude}. Gerbang {@code CommonPrivilages.checkPrevilages(...)} di dalam
 *   {@code MasaJadwalPelajaranAction} dievaluasi terhadap menu yang sedang aktif, yaitu
 *   menu induk ("Jadwal Pelajaran" atau "Kelas Les Siswa") — jadi hak pada menu induk
 *   otomatis menjadi hak CRUD atas seluruh data masa pembelajaran.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see VoKunci
 * @see JadwalPelajaran
 * @see KelasLesSiswa
 * @see ais.database.model.sekolah.JenisJadwalPelajaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "masa_jadwal_pelajaran")
public class MasaJadwalPelajaran extends VoKunci {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8842945307087672400L;
	/** Kunci utama tabel {@code sekolah.masa_jadwal_pelajaran}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna penyunting terakhir; diisi {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna penyunting terakhir; diisi {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyunting terakhir.
	 *
	 * <p>Nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> sehingga jejak
	 * audit yang sudah ada tidak bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna penyunting; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyunting terakhir. Sama seperti {@link #setOlehId(String)},
	 * nilai {@code null} atau string kosong <b>diabaikan diam-diam</b>.
	 *
	 * @param oleh nama penyunting; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris masa ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat siapa dan kapan baris masa diubah dengan
	 * mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p><b>Efek samping:</b> mengisi {@link #getOleh()}, {@link #getOlehId()} dan
	 * {@link #getTanggal_dirubah()} tepat sebelum {@code UPDATE} dikirim. Karena entity ini
	 * memakai <i>property access</i> dan enam getter-nya menulis balik ke field (lihat
	 * Javadoc kelas), callback ini ikut berjalan pada perubahan yang tidak pernah diminta
	 * pengguna — mis. saat baris sekadar dirender di grid layar Masa Pembelajaran.</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> deklarasi field {@code tanggal_dirubah} sengaja berada
	 * pada baris yang sama dengan method ini (hasil penyisipan otomatis). Nilai awalnya
	 * diambil dari {@code WaktuUtil.getDate()} agar baris baru selalu punya stempel waktu
	 * meski belum pernah di-{@code UPDATE}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi; pengisian normal dilakukan
	 * {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru
	 *         dibuat karena field-nya diinisialisasi saat deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris masa: {@code id-nama-mulai-sampai-sekolah}.
	 *
	 * <p><b>Bukan method baca murni.</b> Baris pertamanya menugaskan ulang
	 * {@code sekolah = getSekolah()}, sehingga pemanggilan {@code toString()} ikut memicu
	 * resolusi proxy lazy {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject}. Pada objek yang sudah detached hal ini
	 * dapat membuka session baru; pada objek yang masih terkelola, nilai hasil resolusi
	 * tertulis ke field. Hindari memanggilnya di jalur log yang sangat panas.</p>
	 *
	 * <p>Field {@code nama}, {@code mulai} dan {@code sampai} dibaca <b>mentah</b> (bukan
	 * lewat getter), jadi keluarannya bisa memuat {@code null} untuk baris yang belum
	 * pernah dibaca lewat getter pengisi otomatis.</p>
	 *
	 * @return gabungan id, nama, tanggal mulai, tanggal sampai dan sekolah
	 */
	public String toString() {
		sekolah = getSekolah();
		return id + "-" + nama + "-" + mulai + "-" + sampai + "-" + sekolah;
	}

	/** Nama masa yang tampil di kombobox dan laporan; lihat {@link #getNama()}. */
	private String nama;
	/** Tanggal awal berlakunya masa; lihat {@link #getMulai()}. */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal akhir berlakunya masa; lihat {@link #getSampai()}. */
	private Date sampai = ais.ui.util.WaktuUtil.getDate();
	/** Nama program penyelenggaraan; lihat catatan pada {@link #getProgram()}. */
	private String program;
	/** Sekolah pemilik; {@code null} berarti berlaku untuk semua sekolah. */
	private Sekolah sekolah;
	/** Yayasan pemilik; {@code null} berarti berlaku untuk semua yayasan. */
	private Yayasan yayasan;
	/** Tahun ajaran penanda periode, mis. {@code "2025/2026"}. */
	private String tahunAjaran;

	/** Keterangan bebas yang ditampilkan pada grid master. */
	private String keterangan;
	/** Penanda masa masih boleh dipakai; lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Penanda masa bawaan untuk pengisian otomatis; lihat {@link #getDefaultData()}. */
	private Boolean defaultData;
	/** Nomor semester (ganjil/genap ditentukan dari sisa bagi 2). */
	private Integer semester;
	/** Nama semester turunan; ditimpa tanpa syarat oleh {@link #getNamaSmt()}. */
	private String namaSmt;

	/** Pengguna yang mengunci baris ini; lihat {@link #getDikunci()}. */
	private Tbmuser dikunci;
	/** Disposisi SOP terkait; lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/ZK.
	 *
	 * <p>Objek baru sudah membawa {@code mulai}, {@code sampai} dan {@code tanggal_dirubah}
	 * berisi waktu saat ini karena ketiga field itu diinisialisasi pada deklarasinya.</p>
	 */
	public MasaJadwalPelajaran() {

	}

	/**
	 * Mengembalikan kunci utama baris masa.
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
	 * Menyetel kunci utama baris masa.
	 *
	 * @param id nilai kunci utama; {@code null} menandai objek belum tersimpan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama masa pembelajaran yang tampil di kombobox jadwal, grid master dan
	 * tampilan jadwal ({@code JadwalDisplayHelper}).
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila {@code nama} masih {@code null} atau hanya
	 * berisi spasi, nama dibangkitkan sebagai {@code "Masa " + tahun ajaran + " " +
	 * Ganjil/Genap} lalu <b>ditulis ke field</b>. Karena entity ini memakai property access,
	 * nama hasil bangkitan itu akan ikut tersimpan pada {@code UPDATE} berikutnya walaupun
	 * pengguna tidak pernah mengetiknya.</p>
	 *
	 * <p>Penentuan ganjil/genap memakai {@link #getSemester()} — sehingga pembacaan
	 * {@code getNama()} pada baris yang semesternya masih kosong ikut memicu pengisian
	 * otomatis semester (lihat {@link #getSemester()}), dan {@link #getTahunAjaran()} juga
	 * dapat terisi otomatis di sini.</p>
	 *
	 * @return nama masa; tidak pernah {@code null} setelah pemanggilan ini
	 */
	public String getNama() {
		if (nama == null || nama.trim().isEmpty()) {
			nama = "Masa " + getTahunAjaran() + " "
					+ (getSemester() % 2 == 0 ? JadwalPelajaran.GENAP : JadwalPelajaran.GANJIL);
		}
		return nama;
	}

	/**
	 * Menyetel nama masa pembelajaran.
	 *
	 * <p>Dipanggil {@code MasaJadwalPelajaranAction.onSave()} dari isian "Masa Pembelajaran *"
	 * yang divalidasi tidak boleh kosong.</p>
	 *
	 * @param nama nama masa; nilai kosong akan dibangkitkan ulang oleh {@link #getNama()}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan sekolah pemilik masa ini.
	 *
	 * <p>Nilai {@code null} bermakna <b>"berlaku untuk semua sekolah"</b>, bukan "belum
	 * diisi": penyaring kombobox pada layar jadwal ditulis sebagai
	 * {@code isNull("sekolah") OR eq("sekolah", sekolahAktif)}, dan grid master menampilkan
	 * teks "Semua" untuk baris seperti itu.</p>
	 *
	 * <p><b>Menulis balik.</b> Hasil {@code check(...)} (resolusi proxy lazi milik
	 * {@link ais.database.model.GeneralValueObject}) ditugaskan kembali ke field sebelum
	 * dikembalikan.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila masa berlaku lintas sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel sekolah pemilik masa ini.
	 *
	 * <p><b>Perilaku non-obvious:</b> objek {@link Sekolah} yang ber-id {@code null} (objek
	 * transient, mis. hasil resolusi tenant yang gagal) <b>disimpan sebagai {@code null}</b>.
	 * Karena {@code null} pada kolom ini berarti "semua sekolah", kegagalan resolusi tenant
	 * di sini <i>melebarkan</i> cakupan baris, bukan mempersempitnya.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek ber-id {@code null} membuat
	 *        masa berlaku untuk semua sekolah
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik masa ini.
	 *
	 * <p>Sama seperti {@link #getSekolah()}: {@code null} berarti "berlaku untuk semua
	 * yayasan" (grid master menampilkan "Semua"), dan hasil {@code check(...)} ditulis balik
	 * ke field.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila masa berlaku lintas yayasan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menyetel yayasan pemilik masa ini.
	 *
	 * <p>Objek {@link Yayasan} ber-id {@code null} disimpan sebagai {@code null} — dengan
	 * konsekuensi pelebaran cakupan yang sama seperti dijelaskan pada
	 * {@link #setSekolah(Sekolah)}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek ber-id {@code null} membuat
	 *        masa berlaku untuk semua yayasan
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas masa ini.
	 *
	 * <p>Ditampilkan pada kolom terakhir grid master dan dipakai sebagai penyaring pencarian
	 * {@code ilike ANYWHERE} di {@code MasaJadwalPelajaranAction.initCriteria()}.</p>
	 *
	 * @return keterangan, atau {@code null} bila kosong
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas masa ini.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal awal berlakunya masa.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila field {@code null}, diisi tanggal hari ini
	 * ({@code WaktuUtil.getDate()}) dan nilai itu tertulis ke field — sehingga baris dengan
	 * kolom {@code mulai} kosong akan memperoleh tanggal hari ini semata-mata karena
	 * dibaca.</p>
	 *
	 * <p><b>Dampak hilir:</b> {@code JadwalPelajaran.getTanggalMulaiJadwalPelajaran()}
	 * menimpa tanggal mulai jadwal dengan nilai ini, dan {@code LaporanJadwalPelajaran}
	 * mencetaknya sebagai parameter {@code masa_mulai}.</p>
	 *
	 * @return tanggal awal masa; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		if (mulai == null) {
			mulai = ais.ui.util.WaktuUtil.getDate();
		}
		return mulai;
	}

	/**
	 * Menyetel tanggal awal berlakunya masa.
	 *
	 * @param mulai tanggal awal; {@code null} akan diisi hari ini oleh {@link #getMulai()}
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal akhir berlakunya masa.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila field {@code null}, tanggal akhir dihitung
	 * sebagai {@link #getMulai()} + 6 bulan lalu <b>ditulis ke field</b>. Perhatikan bahwa
	 * pemanggilan {@link #getMulai()} di dalamnya sendiri bisa mengisi tanggal awal dengan
	 * hari ini, jadi satu pembacaan dapat mengisi dua kolom sekaligus.</p>
	 *
	 * <p><b>Kuirk perhitungan:</b> penambahan dilakukan dengan
	 * {@code calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6)}, bukan
	 * {@code calendar.add(...)}. Pada {@link Calendar} lenient hasilnya tetap berpindah tahun
	 * dengan benar, tetapi tanggal dalam bulan tidak dinormalkan: mulai tanggal 31 pada bulan
	 * yang tujuannya lebih pendek akan bergeser melewati akhir bulan (mis. 31 Agustus →
	 * awal Maret, bukan akhir Februari).</p>
	 *
	 * <p><b>Dampak hilir:</b> {@code PenjadwalanSiswaHelper} memakai nilai ini sebagai
	 * kondisi henti perulangan pembangkitan baris pertemuan — memperpendek atau
	 * memperpanjang tanggal ini langsung mengubah jumlah pertemuan yang dibuat.</p>
	 *
	 * @return tanggal akhir masa; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		if (sampai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getMulai());
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
			sampai = calendar.getTime();
		}
		return sampai;
	}

	/**
	 * Menyetel tanggal akhir berlakunya masa.
	 *
	 * @param sampai tanggal akhir; {@code null} akan dihitung ulang oleh
	 *        {@link #getSampai()} sebagai enam bulan setelah tanggal mulai
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan tahun ajaran penanda periode, mis. {@code "2025/2026"}.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila field {@code null}, diisi
	 * {@code Common.getCurrentTahunAkademik()} dan nilainya tertulis ke field.</p>
	 *
	 * <p>Kolom ini adalah salah satu dari dua kunci pencocokan yang dipakai
	 * {@code JadwalPelajaran.getMasaJadwalPelajaran()} saat memilih masa bawaan secara
	 * otomatis (yang satu lagi {@link #getSemester()}), dan juga salah satu kunci pada
	 * perintah SQL native yang menyeragamkan penanda {@link #getDefaultData()} di layar
	 * master.</p>
	 *
	 * @return tahun ajaran; tidak pernah {@code null} setelah pemanggilan ini
	 */
	public String getTahunAjaran() {
		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return tahunAjaran;
	}

	/**
	 * Menyetel tahun ajaran penanda periode.
	 *
	 * @param tahunAjaran tahun ajaran, mis. {@code "2025/2026"}; {@code null} akan diisi
	 *        tahun akademik berjalan oleh {@link #getTahunAjaran()}
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Mengembalikan nomor semester masa ini.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila field {@code null}, diisi {@code 1} saat
	 * {@code Common.isNowSemensterGanjil()} bernilai {@code true} dan {@code 2} bila tidak,
	 * lalu ditulis ke field. Ganjil/genap ditentukan dari sisa bagi 2 sehingga nomor lain
	 * (mis. 3, 4) tetap valid dan tetap terpetakan ke Ganjil/Genap.</p>
	 *
	 * <p>Dipakai bersama {@link #getTahunAjaran()} sebagai kunci pencocokan masa bawaan di
	 * {@code JadwalPelajaran.getMasaJadwalPelajaran()}, sebagai penyaring kombobox pada
	 * layar jadwal, dan sebagai kunci perintah SQL native penyeragam
	 * {@link #getDefaultData()}.</p>
	 *
	 * @return nomor semester; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(name = "semester")
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	/**
	 * Menyetel nomor semester masa ini.
	 *
	 * <p>Diisi {@code MasaJadwalPelajaranAction.onSave()} dari kombobox Ganjil(1)/Genap(2).</p>
	 *
	 * @param semester nomor semester; {@code null} akan diisi semester berjalan oleh
	 *        {@link #getSemester()}
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan nama program penyelenggaraan masa ini.
	 *
	 * <p><b>Kolom mati dalam praktik.</b> Tidak ada pemanggil {@link #setProgram(String)} di
	 * seluruh basis kode selain kelas ini, sehingga kolomnya selalu {@code null}. Sementara
	 * itu {@code MasaJadwalPelajaranAction.initCriteria()} tetap memasang penyaring
	 * pencarian {@code Restrictions.eq("program", ...)} untuk kombobox "Program" di toolbar
	 * pencarian — akibatnya memilih program apa pun pada pencarian akan mengembalikan daftar
	 * kosong, bukan hasil yang tersaring.</p>
	 *
	 * @return nama program; dalam praktik selalu {@code null}
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menyetel nama program penyelenggaraan masa ini.
	 *
	 * <p>Tidak dipanggil dari mana pun saat ini; lihat catatan pada {@link #getProgram()}.</p>
	 *
	 * @param program nama program; boleh {@code null}
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan penanda apakah masa ini masih boleh dipakai.
	 *
	 * <p><b>Default {@code true} bila kolom kosong</b> — baris lama yang belum pernah
	 * disentuh penanda ini dianggap aktif. Penyaring kombobox pada layar jadwal konsisten
	 * dengan itu: {@code isNull("aktif") OR eq("aktif", true)}.</p>
	 *
	 * <p><b>Perlu diperhatikan:</b> pengisian otomatis masa bawaan di
	 * {@code JadwalPelajaran.getMasaJadwalPelajaran()} <b>tidak</b> memeriksa penanda ini —
	 * ia hanya mencocokkan {@link #getDefaultData()}, {@link #getTahunAjaran()} dan
	 * {@link #getSemester()}. Jadi masa yang sengaja dinonaktifkan lewat centang "Aktif" di
	 * grid master tetap bisa terpasang otomatis pada baris jadwal.</p>
	 *
	 * @return {@code true} bila masa masih boleh dipakai
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif masa ini.
	 *
	 * <p>Disetel dari centang "Aktif" pada grid master ({@code MasaJadwalPelajaranRenderer}),
	 * yang langsung diikuti {@code Common.refreshSaveOrUpdate(...)}. Centang tersebut
	 * dinonaktifkan bila pengguna tidak punya hak {@code UPDATE}.</p>
	 *
	 * @param aktif {@code true} bila masa boleh dipakai; {@code null} diperlakukan sebagai
	 *        {@code true} oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda "masa bawaan" untuk pengisian otomatis pada baris jadwal.
	 *
	 * <p><b>Default {@code false} bila kolom kosong</b> — bentuk yang <i>aman</i>: baris yang
	 * belum pernah ditandai tidak akan pernah terpilih otomatis. Penanda ini sendiri memang
	 * ditulis ke basis data (lewat centang "Default" pada grid master), jadi kolom ini bukan
	 * kasus "kolom tak pernah ditulis".</p>
	 *
	 * <p><b>Pemakaian.</b> Satu-satunya pembaca sesungguhnya adalah
	 * {@code JadwalPelajaran.getMasaJadwalPelajaran()}: bila sebuah baris jadwal belum punya
	 * masa, getter tersebut menelusuri seluruh {@code MasaJadwalPelajaran} dari cache
	 * {@code ConstantValues} dan memasang <b>yang pertama ditemukan</b> dengan
	 * {@code getDefaultData()} bernilai {@code true} serta tahun ajaran dan semester yang
	 * cocok, lalu {@code break}.</p>
	 *
	 * <h4>Sifat penelusuran itu yang perlu diketahui</h4>
	 * <ul>
	 *   <li><b>Tanpa penyaring sekolah/yayasan.</b> Penelusuran hanya mencocokkan tahun
	 *   ajaran dan semester atas cache <i>global satu JVM</i>. Berbeda dengan kombobox di
	 *   layar jadwal yang menyaring {@code isNull("sekolah") OR eq("sekolah", sekolahAktif)},
	 *   jalur otomatis ini dapat memasang masa milik sekolah lain pada jadwal sebuah
	 *   sekolah. Karena {@code JadwalPelajaran.getTanggalMulaiJadwalPelajaran()} kemudian
	 *   menimpa tanggal mulai jadwal dari masa tersebut, tanggal milik sekolah lain ikut
	 *   terbawa.</li>
	 *   <li><b>Tidak deterministik bila ada lebih dari satu baris bertanda.</b> Cache
	 *   berbentuk {@code ConcurrentHashMap} ({@code MemoryCacheUtil}) yang urutan iterasi
	 *   nilainya tidak dijamin; kombinasi dengan {@code break} membuat baris mana yang
	 *   terpilih bergantung pada isi peta saat itu.</li>
	 *   <li><b>Penyeragaman "hanya satu default" berjalan di luar Hibernate.</b> Ketika
	 *   centang "Default" ditekan pada grid master, {@code MasaJadwalPelajaranAction}
	 *   menjalankan {@code createSQLQuery("update sekolah.masa_jadwal_pelajaran set
	 *   default_data=false where id != ... and tahunajaran=... and semester=...")}.
	 *   Perintah itu (a) tidak menyaring sekolah/yayasan, sehingga menandai satu masa sebagai
	 *   default juga mencabut penanda default milik sekolah lain pada tahun ajaran dan
	 *   semester yang sama; dan (b) tidak menyentuh cache {@code MemoryCacheUtil}, sehingga
	 *   entity lama di cache masih membawa {@code defaultData = true} sampai cache dimuat
	 *   ulang — pada jendela waktu itu penelusuran di atas bisa memilih baris yang di basis
	 *   data sudah tidak default lagi.</li>
	 * </ul>
	 *
	 * @return {@code true} bila masa ini menjadi pilihan otomatis untuk tahun ajaran dan
	 *         semester yang sama
	 */
	@Column(name = "default_data")
	public Boolean getDefaultData() {
		return defaultData == null ? false : defaultData;
	}

	/**
	 * Menyetel penanda "masa bawaan".
	 *
	 * <p>Disetel dari centang "Default" pada grid master. Setelah setter ini dipanggil,
	 * {@code MasaJadwalPelajaranAction} menyimpan entity lalu menjalankan perintah SQL native
	 * yang mencabut penanda default pada baris lain dengan tahun ajaran dan semester yang
	 * sama; lihat catatan lengkap pada {@link #getDefaultData()}.</p>
	 *
	 * @param defaultData {@code true} untuk menjadikan masa ini pilihan otomatis;
	 *        {@code null} diperlakukan sebagai {@code false} oleh {@link #getDefaultData()}
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan nama semester dalam bentuk teks ("Ganjil"/"Genap").
	 *
	 * <p><b>Nilai turunan yang tetap terpetakan sebagai kolom.</b> Method ini <b>selalu</b>
	 * menghitung ulang dari {@link #getSemester()} dan menimpa field tanpa syarat, jadi
	 * apa pun yang pernah disimpan lewat {@link #setNamaSmt(String)} akan hilang pada
	 * pembacaan berikutnya. Karena entity memakai property access, nilai hasil hitung ulang
	 * itulah yang ikut tertulis saat {@code UPDATE}.</p>
	 *
	 * <p>Dipakai sebagai salah satu kolom label kombobox masa pada layar Jadwal Pelajaran
	 * ({@code new String[] { "nama", "tahunAjaran", "namaSmt" }}) dan pada kolom "Semester"
	 * grid master.</p>
	 *
	 * @return {@code "Ganjil"} untuk semester ganjil, {@code "Genap"} untuk semester genap
	 */
	public String getNamaSmt() {
		namaSmt = getSemester() % 2 == 0 ? JadwalPelajaran.GENAP : JadwalPelajaran.GANJIL;
		return namaSmt;
	}

	/**
	 * Menyetel nama semester dalam bentuk teks.
	 *
	 * <p>Praktis tidak berguna sebagai penyimpan nilai: {@link #getNamaSmt()} menimpanya
	 * pada setiap pembacaan. Setter ini ada untuk memenuhi kontrak JavaBean yang dibutuhkan
	 * Hibernate dan pengikatan komponen ZK.</p>
	 *
	 * @param namaSmt nama semester; akan ditimpa pada pembacaan berikutnya
	 */
	public void setNamaSmt(String namaSmt) {
		this.namaSmt = namaSmt;
	}

	/**
	 * Mengembalikan pengguna yang mengunci baris masa ini (implementasi kontrak
	 * {@link VoKunci}).
	 *
	 * <p><b>Ini gerbang bisnis, bukan sekadar penanda tampilan.</b>
	 * {@code DetailPenilaianSiswaHelper} menyembunyikan seluruh perangkat pengisian dan
	 * unggah nilai bila {@code jadwalPelajaran.getMasaJadwalPelajaran().getDikunci()} tidak
	 * {@code null} — sehingga mengunci satu baris masa membekukan pengisian nilai bagi semua
	 * jadwal yang bernaung padanya. Pada layar master sendiri, tombol "Simpan" disembunyikan
	 * dan formulirnya dibekukan ({@code Common.freezeGanti}) bila baris terkunci.</p>
	 *
	 * <p><b>Menulis balik.</b> Hasil {@code check(...)} ditugaskan kembali ke field sebelum
	 * dikembalikan.</p>
	 *
	 * <p>Tombol Kunci/Buka Kunci dipasang lewat
	 * {@code GeneralValueObject.tampilKunci(...)}; syarat tampilnya adalah akun aktif bukan
	 * siswa dan konfigurasi {@code Common.getApakahAdminBolehKunci()} — bukan hak
	 * {@code UPDATE} pada menu.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris tidak terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menyetel pengguna pengunci baris masa ini (implementasi kontrak {@link VoKunci}).
	 *
	 * <p>Dipanggil {@code GeneralValueObject.tampilKunci(...)}: {@code Common.getCurrentUser()}
	 * saat mengunci dan {@code null} saat membuka kunci, masing-masing diikuti
	 * {@code Common.refreshUpdate(...)}.</p>
	 *
	 * @param dikunci pengguna pengunci; {@code null} membuka kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Mengembalikan disposisi SOP yang menaungi baris masa ini (implementasi kontrak
	 * {@code DataSop}).
	 *
	 * <p><b>Menulis balik.</b> Hasil {@code check(...)} ditugaskan kembali ke field sebelum
	 * dikembalikan.</p>
	 *
	 * @return disposisi SOP terkait, atau {@code null} bila baris tidak berasal dari alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP yang menaungi baris masa ini (implementasi kontrak
	 * {@code DataSop}).
	 *
	 * <p><b>Hanya menerima disposisi yang sudah tersimpan.</b> Argumen {@code null} atau
	 * disposisi ber-id {@code null} diabaikan diam-diam lewat penjaga di awal method,
	 * sehingga kaitan SOP yang sudah ada tidak dapat dilepas lewat setter ini.</p>
	 *
	 * <p><b>Kuirk:</b> ekspresi ternary sesudah penjaga tersebut tidak pernah memilih cabang
	 * pertama — pada titik itu {@code disposisiSop} dipastikan tidak {@code null} dan
	 * ber-id tidak {@code null}, sehingga kondisi {@code (disposisiSop == null ||
	 * disposisiSop.getId() == null)} selalu bernilai {@code false}. Efektifnya method ini
	 * selalu menugaskan argumennya. Bentuk berbelit itu dipertahankan apa adanya karena
	 * dipakai seragam pada banyak entity turunan {@code DataSop}.</p>
	 *
	 * @param disposisiSop disposisi SOP yang sudah tersimpan; {@code null} atau yang ber-id
	 *        {@code null} diabaikan tanpa error
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}
}
