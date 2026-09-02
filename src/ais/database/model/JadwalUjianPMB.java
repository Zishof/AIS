package ais.database.model;

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
import org.json.JSONObject;

/**
 * Entity <b>sesi ujian PMB daring</b> (tabel {@code public.jadwal_ujian_pmb}).
 *
 * <p>Satu baris di sini adalah satu <i>sesi</i> ujian seleksi calon mahasiswa yang dikerjakan
 * secara daring: kapan sesi dibuka ({@link #getWaktuMulai()}–{@link #getWaktuSampai()}), untuk
 * paket/prodi mana ({@link #getPaket()}), untuk ruang PMB mana saja
 * ({@link #getBerlakuUntukSemuaRuangan()} / {@link #getRuanganYgIkut()}), dan syarat apa yang
 * harus dipenuhi peserta ({@link #getPesertaUjianHarusPunyaNomorUjian()},
 * {@link #getPesertaUjianHarusTelahUjian()}). Induknya adalah {@link UjianPMB}, yaitu
 * <i>gelaran</i> ujian (tanggal + lokasi + gelombang pendaftaran).</p>
 *
 * <h3>Bukan turunan langsung {@code GeneralValueObject}</h3>
 * <p>Berbeda dengan {@link UjianPMB} maupun {@link RuangPMB} yang mewarisi
 * {@link GeneralValueObject} secara langsung, kelas ini duduk di keluarga <b>unit
 * pembelajaran</b>:</p>
 * <pre>
 * GeneralValueObject
 *   └─ {@link ais.database.model.sop.DataSop}      (jejak SOP/disposisi)
 *        └─ {@link VoKunci}                         (kontrak {@code getDikunci()}/{@code setDikunci()})
 *             └─ {@link VOPembelajaran}             (mesin pertemuan, e-learning, Google Classroom)
 *                  └─ <b>JadwalUjianPMB</b>
 * </pre>
 * <p>Konsekuensinya besar dan tidak terlihat dari isi berkas ini: seluruh mesin
 * {@code ambilPertemuan()}, {@code ambilPertemuanList()}, {@code populatePertemuan()},
 * {@code removePertemuan()}, {@code ambilJumlahPertemuanStatistik()}, {@code infoSimple()},
 * dan sinkronisasi Google Classroom diwarisi cuma-cuma dari {@link VOPembelajaran}. Itulah
 * sebabnya sebuah "jadwal ujian PMB" bisa diperlakukan persis seperti sebuah
 * {@link Perkuliahan} oleh layar e-learning, absensi, dan dasbor timeline.</p>
 *
 * <h3>Konfirmasi rantai relasi yang disebut di {@link UjianPMB}</h3>
 * <p>Javadoc {@link UjianPMB} menyebut cabang <code>UjianPMB → JadwalUjianPMB → Pertemuan →
 * PertemuanPunyaUjian → Ujian → BankSoal</code>. Dibaca dari kode kelas ini, rantai itu
 * <b>benar, tetapi hanya mata rantai pertamanya yang berupa relasi Hibernate biasa</b>:</p>
 * <ul>
 *   <li><code>UjianPMB → JadwalUjianPMB</code>: nyata dan dipetakan, lewat
 *   {@link #getUjianPMB()} ({@code @ManyToOne}, kolom {@code ujian_pmb}, {@code nullable = false}).
 *   Arah sebaliknya (koleksi {@code @OneToMany} di {@link UjianPMB}) tidak ada — pencarian
 *   selalu berbentuk kriteria atas {@code JadwalUjianPMB} yang di-{@code createAlias("ujianPMB", …)},
 *   seperti pada {@code TampilanUjianCalonMahasiswa}.</li>
 *   <li><code>JadwalUjianPMB → Pertemuan</code>: <b>tidak ada satu pun properti {@code Pertemuan}
 *   di kelas ini.</b> Relasinya semata-mata terbalik: {@link Pertemuan} yang memegang FK
 *   {@code jadwal_ujian_pmb} ({@code Pertemuan.getJadwalUjianPMB()}). Penelusuran maju dilakukan
 *   lewat {@code ambilPertemuanList()} warisan {@link VOPembelajaran}, yang <b>bukan query
 *   Hibernate biasa</b> melainkan membaca berkas JSON pendamping di disk
 *   ({@code Common.getFileLocation(this, "pertemuan_" + id)}) berisi peta id pertemuan, lalu
 *   memuat entity-nya satu per satu dari cache/DB dan menambal id yang belum ada lewat satu
 *   {@code Criteria} tambahan pada session yang dibuka-tutup sendiri. Bila berkas JSON itu hilang
 *   atau tidak sinkron, daftar pertemuan bisa tampak kosong padahal barisnya ada di DB — di sinilah
 *   {@code RecoveryPertemuanHelper} bermain.</li>
 *   <li><code>Pertemuan → PertemuanPunyaUjian → Ujian → BankSoal</code>: berada di luar berkas ini
 *   dan tetap seperti yang didokumentasikan pada {@link Pertemuan} dan {@link Ujian}. Kelas ini
 *   memang <b>tidak memuat soal, bobot, durasi, maupun nilai ambang</b> sama sekali —
 *   sama seperti {@link UjianPMB}.</li>
 * </ul>
 *
 * <h3>Bagaimana peserta sebuah sesi ditentukan</h3>
 * <p>Tidak ada relasi ke {@link BiodataCalonMahasiswa} di kelas ini. Daftar peserta dihitung ulang
 * setiap kali dibutuhkan oleh {@code AbsensiHelper.populate…}/{@code HasilUjianMahasiswaHelper},
 * dengan urutan cabang berikut (yang pertama cocok dipakai, sisanya diabaikan):</p>
 * <ol>
 *   <li>{@link #getPesertaUjianHarusTelahUjian()} bernilai {@code true} → peserta = calon mahasiswa
 *   yang <i>sudah</i> punya {@code HasilUjianMahasiswa} pada pertemuan tersebut (dipakai untuk sesi
 *   lanjutan/gelombang kedua yang hanya boleh diikuti yang sudah ujian tahap sebelumnya).</li>
 *   <li>{@link #getRuanganYgIkut()} tidak kosong → peserta = penghuni {@link RuangPaketPMB} pada
 *   ruang-ruang yang terdaftar.</li>
 *   <li>selain itu → peserta = seluruh {@link BiodataCalonMahasiswa} aktif pada
 *   {@code getUjianPMB().getGelombangPendaftaran()}, disaring {@link #getPaket()} bila diisi.</li>
 * </ol>
 * <p>{@link #getPesertaUjianHarusPunyaNomorUjian()} bekerja sebagai filter tambahan di semua
 * cabang: bila {@code true}, calon yang {@code noUjian}-nya kosong/{@code null} dibuang.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Bayangan field audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}. Lihat catatan
 *   arsitektur di bawah.</li>
 *   <li><b>Identitas &amp; keterangan</b> — {@link #getNama()}, {@link #getKeterangan()},
 *   {@link #toString()}, {@link #getAktif()}.</li>
 *   <li><b>Kedudukan dalam PMB</b> — {@link #getUjianPMB()}, {@link #getPaket()}.</li>
 *   <li><b>Rentang waktu</b> — {@link #getWaktuMulai()}, {@link #getWaktuSampai()}. Keduanya
 *   getter yang menulis balik (lihat di bawah).</li>
 *   <li><b>Cakupan ruang &amp; syarat peserta</b> — {@link #getBerlakuUntukSemuaRuangan()},
 *   {@link #getRuanganYgIkut()}, {@link #getPesertaUjianHarusPunyaNomorUjian()},
 *   {@link #getPesertaUjianHarusTelahUjian()}.</li>
 *   <li><b>Kewajiban kontrak kelas induk</b> — {@link #getDikunci()} (dari {@link VoKunci}),
 *   {@link #getCourse()}, {@link #getUrutkanotomatis()},
 *   {@link #ambilJumlahDetailperkuliahanLangsung()} (dari {@link VOPembelajaran}).</li>
 * </ol>
 *
 * <h3>Catatan arsitektur: field audit yang dideklarasikan ulang</h3>
 * <p>{@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} tampak "duplikat" dari
 * milik {@link GeneralValueObject}. Itu <b>bukan bug melainkan keharusan teknis</b>:
 * {@code GeneralValueObject} adalah POJO abstrak biasa — bukan {@code @Entity} maupun
 * {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti apa pun dari induk.
 * Setiap entity konkret wajib mendeklarasikan dan menganotasi sendiri field-field tersebut agar
 * kolomnya terbentuk. Penjelasan lengkap pola ini ada di
 * {@link ais.database.model.GeneralValueObject}.</p>
 *
 * <h3>Getter yang menulis balik (pola khas AIS)</h3>
 * <p>Tiga getter di kelas ini <b>mengubah state object saat dibaca</b>. Karena entity ini
 * {@code dynamicUpdate} dan biasanya masih <i>attached</i> pada session ZK, perubahan itu bisa
 * ikut ter-{@code flush} ke database tanpa ada satu pun operator yang menekan tombol simpan:</p>
 * <ul>
 *   <li>{@link #getWaktuMulai()} dan {@link #getWaktuSampai()} mengisi field dengan waktu server
 *   saat ini bila masih {@code null} (kolomnya {@code nullable = false}, jadi ini semacam
 *   penyelamat dari pelanggaran constraint — tetapi berarti sebuah baris lama yang waktunya belum
 *   pernah diisi akan "menjadi hari ini" begitu dibaca).</li>
 *   <li>{@link #getRuanganYgIkut()} menormalkan format CSV <i>dan</i> <b>mengosongkan daftar ruang
 *   secara permanen</b> bila {@link #getBerlakuUntukSemuaRuangan()} bernilai {@code true} —
 *   termasuk untuk baris lama yang kolom {@code berlakuUntukSemuaRuangan}-nya masih {@code null},
 *   karena getter itu memberi nilai bawaan {@code true}. Rinciannya di Javadoc method tersebut.</li>
 * </ul>
 * <p>Selain itu, {@link #getUjianPMB()}, {@link #getPaket()}, dan {@link #getDikunci()} memakai
 * pola standar {@code field = check(field)}: relasi lazy diresolusi dan hasilnya ditugaskan
 * kembali ke field karena instance yang dikembalikan bisa berbeda dari proxy semula. Method
 * {@code check()} tersebut dapat <b>membuka dan menutup session Hibernate sendiri</b> pada tahap
 * terakhirnya; tidak ada getter di kelas ini yang menutup session milik pemanggil.</p>
 *
 * <h3>Persistensi</h3>
 * <p>Entity ini {@code @Audited} (Envers, tabel riwayat {@code jadwal_ujian_pmb_AUD}) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate}. Beberapa properti sengaja tidak diberi
 * {@code @Column} sehingga nama kolomnya mengikuti strategi penamaan bawaan Hibernate
 * ({@code berlakuUntukSemuaRuangan}, {@code ruanganYgIkut}, {@code aktif}, {@code urutkanotomatis},
 * {@code pesertaUjianHarusPunyaNomorUjian}, {@code pesertaUjianHarusTelahUjian}).</p>
 *
 * @see UjianPMB
 * @see VOPembelajaran
 * @see Pertemuan
 * @see RuangPMB
 * @see Paket
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jadwal_ujian_pmb")
public class JadwalUjianPMB extends VOPembelajaran {

	/**
	 * Nomor versi serialisasi. Nilainya persis sama dengan milik {@link Ujian} dan {@link UjianPMB}
	 * (sisa salin-tempel saat kelas ini dibuat); tidak berdampak karena {@code serialVersionUID}
	 * hanya dibandingkan antar-versi kelas yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code public.jadwal_ujian_pmb.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris ini (bayangan field audit).
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna yang mengubah baris ini.
	 *
	 * <p><b>Menolak diam-diam</b> nilai {@code null} maupun string kosong/spasi: dalam kasus itu
	 * nilai lama dipertahankan dan tidak ada pesan kesalahan. Disengaja agar jejak audit yang
	 * sudah terisi tidak terhapus oleh proses batch yang tidak punya konteks pengguna.</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong ditolak diam-diam
	 * sehingga nilai lama tetap bertahan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini (bayangan field audit).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum setiap {@code UPDATE} baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna aktif dan memperbarui {@link #getTanggal_dirubah()}.
	 * Tidak pernah dipanggil manual dari kode aplikasi — Hibernate yang memanggilnya. Perhatikan
	 * bahwa {@code @PreUpdate} tidak berlaku pada {@code INSERT}, sehingga nilai awal
	 * {@code tanggal_dirubah} datang dari inisialisasi field, bukan dari method ini.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat object dibuat sehingga baris
	 * baru pun sudah punya nilai sebelum {@link #onUpdate()} sempat berjalan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Wakil teks untuk log dan combobox sederhana, berbentuk {@code id-nama}.
	 *
	 * <p>Membaca field {@code nama} <b>langsung</b>, bukan lewat {@link #getNama()}, sehingga
	 * spasi di ujung nama tidak dipangkas dan hasilnya bisa sedikit berbeda dari yang tampil di
	 * layar. Untuk teks yang ramah pengguna pada konteks pembelajaran, pakai {@code infoSimple()}
	 * warisan {@link VOPembelajaran} — yang untuk kelas ini juga mengembalikan {@link #getNama()}.</p>
	 *
	 * @return {@code id + "-" + nama}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama sesi ujian, wajib diisi; lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas yang ditampilkan di grid jadwal; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Gelaran ujian PMB induk (wajib); lihat {@link #getUjianPMB()}. */
	private UjianPMB ujianPMB;
	/** Paket/prodi pilihan yang disasar sesi ini; {@code null} berarti semua paket. Lihat {@link #getPaket()}. */
	private Paket paket;

	/** Awal rentang waktu sesi; lihat {@link #getWaktuMulai()}. */
	private Date waktuMulai;
	/** Akhir rentang waktu sesi; lihat {@link #getWaktuSampai()}. */
	private Date waktuSampai;
	/** Syarat "peserta harus sudah punya nomor ujian"; lihat {@link #getPesertaUjianHarusPunyaNomorUjian()}. */
	private Boolean pesertaUjianHarusPunyaNomorUjian;
	/** Syarat "peserta harus sudah pernah ujian"; lihat {@link #getPesertaUjianHarusTelahUjian()}. */
	private Boolean pesertaUjianHarusTelahUjian;
	/** Penanda sesi berlaku untuk seluruh ruang PMB; lihat {@link #getBerlakuUntukSemuaRuangan()}. */
	private Boolean berlakuUntukSemuaRuangan;

	/**
	 * Daftar id {@link RuangPMB} yang ikut sesi ini dalam bentuk CSV berpagar koma
	 * ({@code ",3,7,12,"}); lihat {@link #getRuanganYgIkut()}.
	 */
	private String ruanganYgIkut;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate dan layar {@code JadwalUjianPMBAction}
	 * saat menambah data baru. Tidak mengisi satu pun properti: nilai bawaan justru muncul
	 * belakangan dari getter-getter yang menulis balik (lihat Javadoc kelas).
	 */
	public JadwalUjianPMB() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} memakai {@code IDENTITY} dan ditandai {@code insertable = false} sehingga
	 * nilainya sepenuhnya ditentukan sequence basis data. Selain sebagai identitas Hibernate, id
	 * ini juga dipakai sebagai bagian nama berkas cache pertemuan
	 * ({@code "pertemuan_" + id}) oleh mesin {@link VOPembelajaran}.</p>
	 *
	 * @return kunci utama, atau {@code null} bila entity belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Hanya untuk Hibernate dan proses salin data.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama sesi ujian, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Nama inilah yang muncul sebagai judul unit pembelajaran di layar e-learning calon
	 * mahasiswa dan sebagai hasil {@code infoSimple()}/{@code infoSangatSimple()} warisan
	 * {@link VOPembelajaran}.</p>
	 *
	 * @return nama sesi terpangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama sesi ujian. Disimpan apa adanya (pemangkasan terjadi di getter).
	 *
	 * @param nama nama sesi; kolomnya {@code nullable = false} sehingga {@code null} akan ditolak
	 *             basis data saat flush
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas sesi ini.
	 *
	 * @return keterangan, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas sesi ini.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan gelaran ujian PMB induk sesi ini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}) dan {@code LAZY}. Lewat induk inilah gelombang
	 * pendaftaran peserta ditemukan: {@code getUjianPMB().getGelombangPendaftaran()} dipakai
	 * {@code AbsensiHelper}, {@code HasilUjianMahasiswaHelper}, dan
	 * {@code TampilanUjianCalonMahasiswa} untuk menyaring calon mahasiswa, serta oleh
	 * {@link Pertemuan} untuk menentukan tahun akademik dan jenis semester sebuah pertemuan ujian
	 * PMB.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check()} sehingga proxy lazy diresolusi dan hasilnya
	 * ditugaskan kembali ke field — instance yang dikembalikan bisa berbeda object dari proxy
	 * semula. Bila entity sudah <i>detached</i>, {@code check()} dapat membuka session Hibernate
	 * baru dan menutupnya sendiri.</p>
	 *
	 * @return gelaran ujian PMB induk; secara praktis tidak pernah {@code null} untuk baris yang
	 *         tersimpan
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ujian_pmb", nullable = false)
	public UjianPMB getUjianPMB() {
		ujianPMB = check(ujianPMB);
		return ujianPMB;
	}

	/**
	 * Menetapkan gelaran ujian PMB induk sesi ini.
	 *
	 * @param ujianPMB gelaran ujian induk; wajib diisi sebelum baris disimpan
	 */
	public void setUjianPMB(UjianPMB ujianPMB) {
		this.ujianPMB = ujianPMB;
	}

	/** Pengguna yang mengunci sesi ini; lihat {@link #getDikunci()}. */
	private Tbmuser dikunci;

	/**
	 * Mengembalikan pengguna yang "mengunci" sesi ini, bila ada.
	 *
	 * <p>Properti ini ada semata-mata untuk memenuhi kontrak abstrak {@link VoKunci}, yang di
	 * entity lain (mis. {@link Perkuliahan} atau data kehadiran pegawai) dipakai sebagai penanda
	 * "data sudah dikunci, kontrol edit disembunyikan". <b>Untuk {@code JadwalUjianPMB} properti
	 * ini praktis dorman</b>: tidak ada satu pun layar atau helper PMB
	 * ({@code JadwalUjianPMBAction}, {@code AktifitasJadwalUjianPMBHelper},
	 * {@code PenjadwalanUjianPMBHelper}) yang memanggil {@link #setDikunci(Tbmuser)} maupun
	 * membaca nilainya, sehingga kolom {@code dikunci} selalu {@code null} pada praktiknya.
	 * Dicatat apa adanya, bukan untuk diperbaiki di sini.</p>
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getUjianPMB()} — {@code check()} meresolusi
	 * proxy lazy dan menugaskan hasilnya kembali ke field.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} (nilai yang sesungguhnya selalu terjadi)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menetapkan pengguna yang mengunci sesi ini.
	 *
	 * @param dikunci pengguna pengunci, atau {@code null} untuk membuka kunci
	 * @see #getDikunci()
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Mengembalikan awal rentang waktu sesi ujian.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila field masih {@code null}, method ini mengisinya
	 * dengan waktu server saat ini ({@code WaktuUtil.getDate()}) dan mengembalikan nilai itu —
	 * bukan {@code null}. Kolomnya {@code nullable = false}, jadi perilaku ini melindungi dari
	 * pelanggaran constraint saat menyimpan baris baru yang formulirnya belum diisi. Sisi buruknya:
	 * karena entity biasanya masih <i>attached</i> dan memakai {@code dynamicUpdate}, membaca baris
	 * lama yang waktunya belum pernah terisi bisa membuat "sekarang" ikut ter-{@code flush} ke
	 * database tanpa aksi simpan eksplisit dari pengguna.</p>
	 *
	 * <p>Nilai ini dipakai sebagai jam mulai bawaan tiap {@link Pertemuan} ujian yang dibuat
	 * {@code PenjadwalanUjianPMBHelper} dan {@code AktifitasJadwalUjianPMBHelper}, dan juga
	 * sebagai tanggal pertemuan pertama pada auto-provisioning sesi ujian daring.</p>
	 *
	 * @return awal rentang waktu sesi; tidak pernah {@code null}
	 */
	@Column(nullable = false)
	public Date getWaktuMulai() {
		if (waktuMulai == null) {
			waktuMulai = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuMulai;
	}

	/**
	 * Menetapkan awal rentang waktu sesi ujian.
	 *
	 * @param waktuMulai awal rentang waktu; {@code null} akan "disembuhkan" menjadi waktu sekarang
	 *                   pada pembacaan berikutnya lewat {@link #getWaktuMulai()}
	 */
	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengembalikan akhir rentang waktu sesi ujian.
	 *
	 * <p><b>Getter yang menulis balik</b>, persis seperti {@link #getWaktuMulai()}: field yang
	 * masih {@code null} diisi waktu server saat ini. Perhatikan bahwa tidak ada validasi
	 * {@code waktuSampai >= waktuMulai} di mana pun pada entity ini — urutan waktu sepenuhnya
	 * tanggung jawab layar {@code JadwalUjianPMBAction}.</p>
	 *
	 * <p>Dipakai sebagai jam selesai bawaan pertemuan ujian, dan ikut dirangkai ke teks judul
	 * agenda ({@code "…, Waktu : hh:mm s.d hh:mm"}) oleh {@code PenjadwalanUjianPMBHelper}.</p>
	 *
	 * @return akhir rentang waktu sesi; tidak pernah {@code null}
	 */
	@Column(nullable = false)
	public Date getWaktuSampai() {
		if (waktuSampai == null) {
			waktuSampai = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuSampai;
	}

	/**
	 * Menetapkan akhir rentang waktu sesi ujian.
	 *
	 * @param waktuSampai akhir rentang waktu; {@code null} akan "disembuhkan" menjadi waktu
	 *                    sekarang pada pembacaan berikutnya lewat {@link #getWaktuSampai()}
	 */
	public void setWaktuSampai(Date waktuSampai) {
		this.waktuSampai = waktuSampai;
	}

	/**
	 * Mengembalikan paket/prodi pilihan yang disasar sesi ini.
	 *
	 * <p>Relasi opsional ({@code nullable = true}). Semantik nilainya:</p>
	 * <ul>
	 *   <li>{@code null} — sesi berlaku untuk <b>semua paket</b>. Di grid
	 *   {@code JadwalUjianPMBAction} kolomnya ditampilkan sebagai teks {@code "Semua"}.</li>
	 *   <li>terisi — hanya calon mahasiswa dengan paket yang sama yang masuk daftar peserta
	 *   (lihat cabang ketiga penentuan peserta pada Javadoc kelas), dan hanya mereka yang melihat
	 *   sesi ini di layar {@code TampilanUjianCalonMahasiswa}.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> {@code check()} meresolusi proxy lazy dan menugaskan hasilnya kembali
	 * ke field.</p>
	 *
	 * @return paket yang disasar, atau {@code null} bila sesi berlaku untuk semua paket
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket", nullable = true)
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	/**
	 * Menetapkan paket/prodi pilihan yang disasar sesi ini.
	 *
	 * @param paket paket sasaran, atau {@code null} agar sesi berlaku untuk semua paket
	 */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

	/**
	 * Jumlah peserta "langsung" unit pembelajaran ini — <b>selalu {@code 0}</b> untuk sesi ujian
	 * PMB.
	 *
	 * <p>Implementasi kontrak abstrak {@link VOPembelajaran#ambilJumlahDetailperkuliahanLangsung()}
	 * yang masih berupa stub bawaan generator ({@code TODO Auto-generated method stub}). Pada
	 * {@link Perkuliahan} method ini menghitung baris KRS, tetapi peserta sesi ujian PMB tidak
	 * tersimpan sebagai "detail perkuliahan" melainkan dihitung ulang dari gelombang/ruang
	 * (lihat Javadoc kelas), sehingga tidak ada angka yang bisa dikembalikan secara langsung.</p>
	 *
	 * <p><b>Akibat yang terlihat:</b> setiap pemanggil generik yang memakai angka ini untuk
	 * menampilkan jumlah peserta atau menghitung tinggi baris — {@code TampilanELearningAction},
	 * {@code CommonUiFactoryHelper}, {@code ElearningApiUtil} — akan menampilkan
	 * <b>0 peserta</b> untuk kartu/sel jadwal ujian PMB. Dicatat sebagai kuirk, bukan diperbaiki
	 * di sini.</p>
	 *
	 * @return selalu {@code 0}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 0;
	}

	/** Cuplikan JSON Google Classroom untuk sesi ini; lihat {@link #getCourse()}. */
	private String course;
	/** Penanda sesi aktif/tampil; lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Penanda pengurutan pertemuan otomatis; lihat {@link #getUrutkanotomatis()}. */
	private Boolean urutkanotomatis;

	/**
	 * Mengembalikan cuplikan JSON <i>course</i> Google Classroom yang terkait sesi ini.
	 *
	 * <p>Implementasi kontrak {@link VOPembelajaran#getCourse()}. Kolomnya bertipe {@code text}
	 * dan menyimpan hasil {@code Course.toPrettyString()} dari Google Classroom API, ditulis oleh
	 * {@code ClassRoomUtil} setiap kali sesi disinkronkan.</p>
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null} atau string kosong</b>: bila field kosong,
	 * yang dikembalikan adalah objek JSON kosong {@code "{}"}. Ini penting karena
	 * {@code ClassRoomUtil} langsung memanggil {@code getCourse().equals(pretty)} untuk memutuskan
	 * perlu-tidaknya {@code UPDATE} — tanpa jaminan non-null di sini, pemanggil itu akan
	 * {@code NullPointerException} pada sinkronisasi pertama. Berbeda dengan getter lain di kelas
	 * ini, nilai bawaan tersebut <b>tidak</b> ditulis balik ke field.</p>
	 *
	 * @return JSON course Google Classroom, atau {@code "{}"} bila belum pernah disinkronkan
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Menyimpan cuplikan JSON <i>course</i> Google Classroom. Dipanggil {@code ClassRoomUtil}
	 * setelah membuat atau memperbarui course di sisi Google.
	 *
	 * @param course JSON course; boleh {@code null} (pembacaan berikutnya akan mengembalikan
	 *               {@code "{}"})
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Menyatakan apakah sesi ini berlaku untuk <b>seluruh</b> ruang PMB.
	 *
	 * <p><b>Nilai bawaan {@code true}</b> bila kolomnya masih {@code null} — artinya baris lama
	 * yang dibuat sebelum fitur pembatasan ruang ada otomatis dianggap berlaku untuk semua ruang.
	 * Konsekuensi penting: karena {@link #getRuanganYgIkut()} mengosongkan daftar ruang setiap kali
	 * method ini bernilai {@code true}, baris lama tersebut <b>tidak pernah bisa</b> menahan daftar
	 * ruang sampai kolom {@code berlakuUntukSemuaRuangan}-nya dituliskan {@code false} secara
	 * eksplisit.</p>
	 *
	 * <p>Di sisi pencarian, {@code TampilanUjianCalonMahasiswa} menerjemahkannya menjadi kriteria
	 * "{@code berlakuUntukSemuaRuangan} null ATAU true ATAU {@code ruanganYgIkut} memuat id ruang
	 * calon" — jadi penanganan {@code null} di kueri sudah sejalan dengan nilai bawaan getter ini.</p>
	 *
	 * @return {@code true} bila sesi berlaku untuk semua ruang (termasuk saat kolom masih
	 *         {@code null}), {@code false} bila dibatasi {@link #getRuanganYgIkut()}
	 */
	public Boolean getBerlakuUntukSemuaRuangan() {
		return berlakuUntukSemuaRuangan == null ? true : berlakuUntukSemuaRuangan;
	}

	/**
	 * Menetapkan apakah sesi berlaku untuk seluruh ruang PMB.
	 *
	 * <p>Dipanggil dari kotak centang "Berlaku Untuk Semua Ruangan" di
	 * {@code JadwalUjianPMBAction}. Menyetelnya ke {@code true} akan membuat
	 * {@link #getRuanganYgIkut()} mengosongkan daftar ruang pada pembacaan berikutnya.</p>
	 *
	 * @param berlakuUntukSemuaRuangan penanda cakupan ruang; {@code null} diperlakukan sebagai
	 *                                 {@code true} saat dibaca
	 */
	public void setBerlakuUntukSemuaRuangan(Boolean berlakuUntukSemuaRuangan) {
		this.berlakuUntukSemuaRuangan = berlakuUntukSemuaRuangan;
	}

	/**
	 * Mengembalikan daftar id {@link RuangPMB} yang ikut sesi ini, dalam bentuk CSV berpagar koma.
	 *
	 * <h4>Format keluaran</h4>
	 * <p>{@code ",3,7,12,"} — selalu diawali dan diakhiri koma bila tidak kosong, atau string
	 * kosong {@code ""} bila tidak ada pembatasan. Pagar koma itulah yang membuat pencocokan
	 * anggota bisa ditulis sesederhana {@code getRuanganYgIkut().contains("," + id + ",")}
	 * (dipakai {@code JadwalUjianPMBAction}) dan
	 * {@code Restrictions.ilike("ruanganYgIkut", "," + id + ",", ANYWHERE)}
	 * (dipakai {@code TampilanUjianCalonMahasiswa}) tanpa risiko id {@code 1} cocok dengan
	 * {@code 12}. <b>Tidak pernah mengembalikan {@code null}.</b></p>
	 *
	 * <h4>Getter yang menulis balik — dan bisa menghapus data</h4>
	 * <p>Method ini bukan pembacaan murni. Ia menugaskan hasil normalisasi kembali ke field
	 * {@code ruanganYgIkut}, sehingga pada entity yang masih <i>attached</i> perubahannya dapat
	 * ikut ter-{@code flush} ke basis data. Dua akibat yang perlu diketahui:</p>
	 * <ol>
	 *   <li><b>Normalisasi format.</b> Nilai apa pun dipagari koma lalu koma ganda diciutkan
	 *   (tiga kali {@code replaceAll(",,", ",")} berturut-turut, cukup untuk merapikan sampai
	 *   sekitar delapan koma beruntun). Nilai yang semula {@code "3,,7"} menjadi {@code ",3,7,"}
	 *   dan tersimpan dalam bentuk itu.</li>
	 *   <li><b>Penghapusan daftar ruang.</b> Bila {@link #getBerlakuUntukSemuaRuangan()} bernilai
	 *   {@code true} — termasuk kasus kolomnya masih {@code null} — field dikosongkan menjadi
	 *   {@code ""}. Jadi mencentang "berlaku untuk semua ruangan" lalu menyimpan akan
	 *   <b>menghilangkan pilihan ruang yang sudah dibuat operator secara permanen</b>; mengembalikan
	 *   centang itu tidak memulihkan daftarnya. Perilaku ini konsisten dengan
	 *   {@code JadwalUjianPMBAction.save()} yang juga membersihkan daftar ruang, tetapi terjadi
	 *   diam-diam pada setiap pembacaan, bukan hanya saat menyimpan.</li>
	 * </ol>
	 *
	 * <h4>Cabang mati</h4>
	 * <p>Tiga pemeriksaan {@code equals(",")}, {@code equals(",,")}, {@code equals(",,,")} di badan
	 * method sudah dijalankan <i>setelah</i> rangkaian {@code replaceAll(",,", ",")}, sehingga dua
	 * yang terakhir tidak akan pernah benar. Demikian pula {@code ruanganYgIkut == null} pada baris
	 * {@code return}: ekspresi ternary di awal method selalu menghasilkan nilai non-null. Sisa
	 * kode defensif, tidak berbahaya.</p>
	 *
	 * <h4>Catatan keamanan</h4>
	 * <p>Nilai kembalian method ini disambung <b>mentah</b> ke dalam SQL oleh beberapa pemanggil,
	 * mis. {@code Restrictions.sqlRestriction("ruang_pmb in (-1" + …getRuanganYgIkut() + "-1)")} di
	 * {@code AbsensiHelper}, {@code HasilUjianMahasiswaHelper}, {@code DashboardTimelinePertemuan},
	 * dan {@code JadwalUjianPMBAction}. Method ini <b>tidak memvalidasi bahwa isinya numerik</b> —
	 * ia hanya merapikan koma. Dalam praktiknya isi kolom hanya pernah ditulis dari id kotak
	 * centang {@link RuangPMB}, sehingga ini injeksi tingkat-dua yang butuh akses tulis ke basis
	 * data lebih dulu (risiko rendah, sekeluarga dengan temuan serupa di modul rekap). Dicatat,
	 * tidak diperbaiki di sini.</p>
	 *
	 * @return CSV berpagar koma berisi id ruang PMB, atau {@code ""} bila sesi berlaku untuk semua
	 *         ruang atau belum ada ruang yang dipilih
	 */
	public String getRuanganYgIkut() {
		ruanganYgIkut = (ruanganYgIkut == null || ruanganYgIkut.trim().equalsIgnoreCase(",") ? ""
				: "," + ruanganYgIkut.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (ruanganYgIkut.equals(",")) {
			ruanganYgIkut = "";
		} else if (ruanganYgIkut.equals(",,")) {
			ruanganYgIkut = "";
		} else if (ruanganYgIkut.equals(",,,")) {
			ruanganYgIkut = "";
		}
		if (getBerlakuUntukSemuaRuangan()) {
			ruanganYgIkut = "";
		}
		return ruanganYgIkut == null ? "" : ruanganYgIkut.trim();
	}

	/**
	 * Menetapkan daftar id ruang PMB yang ikut sesi ini.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b> tanpa normalisasi maupun validasi — perapian format
	 * baru terjadi saat dibaca lewat {@link #getRuanganYgIkut()}. Pemanggil di
	 * {@code JadwalUjianPMBAction} membangun nilainya dengan pola "baca nilai lama, buang
	 * {@code ",id,"} bila ada, sambung kembali bila kotak centang aktif", sehingga urutan
	 * baca-tulis di layar itu bergantung pada normalisasi getter.</p>
	 *
	 * @param ruanganYgIkut CSV id ruang; sebaiknya berpagar koma, boleh {@code null}
	 */
	public void setRuanganYgIkut(String ruanganYgIkut) {
		this.ruanganYgIkut = ruanganYgIkut;
	}

	/**
	 * Menyatakan apakah sesi ini aktif (tampil dan dapat diikuti).
	 *
	 * <p><b>Nilai bawaan {@code true}</b> bila kolomnya masih {@code null}, sehingga baris lama
	 * ikut dianggap aktif. Kriteria pencarian di {@code TampilanUjianCalonMahasiswa} menuliskan
	 * hal yang sama secara eksplisit ({@code aktif = true} ATAU {@code aktif IS NULL}), jadi
	 * perilaku kueri dan getter sudah sejalan.</p>
	 *
	 * <p>Di grid {@code JadwalUjianPMBAction} nilai ini terikat pada kotak centang "Aktif" yang
	 * langsung menyimpan perubahan lewat {@code Common.refreshSaveOrUpdate} begitu diklik.</p>
	 *
	 * @return {@code true} bila sesi aktif (termasuk saat kolom masih {@code null})
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif sesi ini.
	 *
	 * @param aktif {@code false} untuk menyembunyikan sesi dari peserta; {@code null} diperlakukan
	 *              sebagai {@code true} saat dibaca
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Menyatakan apakah daftar pertemuan sesi ini diurutkan otomatis berdasarkan tanggal.
	 *
	 * <p>Implementasi kontrak {@link VOPembelajaran#getUrutkanotomatis()}. Nilainya dibaca oleh
	 * {@code VOPembelajaran.masukkanPertemuanLocal(…)} untuk memilih kunci pengurutan peta
	 * pertemuan: {@code true} (bawaan bila kolom {@code null}) mengurutkan berdasarkan
	 * {@code Pertemuan.getTanggal()}, sedangkan {@code false} mengurutkan berdasarkan
	 * {@code Pertemuan.getPertemuanKe()} yang di-<i>pad</i> menjadi empat digit.</p>
	 *
	 * <p>Catatan: layar PMB tidak menyediakan kendali untuk properti ini, sehingga dalam praktiknya
	 * sesi ujian PMB selalu terurut menurut tanggal.</p>
	 *
	 * @return {@code true} bila pertemuan diurutkan otomatis menurut tanggal (termasuk saat kolom
	 *         masih {@code null})
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Menetapkan mode pengurutan daftar pertemuan sesi ini.
	 *
	 * @param urutkanotomatis {@code false} untuk mengurutkan menurut nomor pertemuan alih-alih
	 *                        tanggal; {@code null} diperlakukan sebagai {@code true} saat dibaca
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	/**
	 * Menyatakan apakah peserta sesi ini wajib sudah memiliki nomor ujian.
	 *
	 * <p><b>Nilai bawaan {@code false}</b> bila kolomnya masih {@code null} — jadi tanpa
	 * penyetelan eksplisit, syarat ini mati dan calon yang belum bernomor ujian tetap ikut.</p>
	 *
	 * <p>Bekerja sebagai <i>filter tambahan</i> pada semua cabang penentuan peserta di
	 * {@code AbsensiHelper} dan {@code HasilUjianMahasiswaHelper}: bila {@code true}, kriteria
	 * {@code noUjian <> '' AND noUjian IS NOT NULL} ditambahkan ke pencarian
	 * {@link BiodataCalonMahasiswa}; bila {@code false}, yang ditambahkan adalah pembatas kosong
	 * ({@code sqlRestriction("true")}) sehingga tidak ada penyaringan.</p>
	 *
	 * @return {@code true} bila hanya calon bernomor ujian yang boleh ikut
	 */
	public Boolean getPesertaUjianHarusPunyaNomorUjian() {
		return pesertaUjianHarusPunyaNomorUjian == null ? false : pesertaUjianHarusPunyaNomorUjian;
	}

	/**
	 * Menetapkan syarat "peserta harus sudah punya nomor ujian".
	 *
	 * <p>Terikat pada kotak centang bernama sama di {@code JadwalUjianPMBAction} dan disimpan saat
	 * tombol simpan ditekan.</p>
	 *
	 * @param pesertaUjianHarusPunyaNomorUjian penanda syarat; {@code null} diperlakukan sebagai
	 *                                         {@code false} saat dibaca
	 */
	public void setPesertaUjianHarusPunyaNomorUjian(Boolean pesertaUjianHarusPunyaNomorUjian) {
		this.pesertaUjianHarusPunyaNomorUjian = pesertaUjianHarusPunyaNomorUjian;
	}

	/**
	 * Menyatakan apakah peserta sesi ini wajib sudah pernah mengerjakan ujian pada pertemuan yang
	 * bersangkutan.
	 *
	 * <p><b>Nilai bawaan {@code false}</b> bila kolomnya masih {@code null}.</p>
	 *
	 * <p>Berbeda dengan {@link #getPesertaUjianHarusPunyaNomorUjian()} yang hanya menambah filter,
	 * penanda ini <b>mengganti seluruh sumber daftar peserta</b>: bila {@code true},
	 * {@code AbsensiHelper} tidak lagi mengambil peserta dari gelombang/ruang, melainkan dari
	 * {@code HasilUjianMahasiswa} yang {@code mulaiPada} dan {@code keyhasil}-nya sudah terisi pada
	 * pertemuan tersebut. Pola ini dipakai untuk sesi lanjutan/ujian tahap kedua yang hanya boleh
	 * diikuti calon yang sudah menyelesaikan tahap sebelumnya. Karena cabang ini diperiksa paling
	 * awal, mengaktifkannya membuat {@link #getRuanganYgIkut()} dan {@link #getPaket()} tidak
	 * berpengaruh pada daftar peserta.</p>
	 *
	 * @return {@code true} bila hanya calon yang sudah pernah ujian yang masuk daftar peserta
	 */
	public Boolean getPesertaUjianHarusTelahUjian() {
		return pesertaUjianHarusTelahUjian == null ? false : pesertaUjianHarusTelahUjian;
	}

	/**
	 * Menetapkan syarat "peserta harus sudah pernah ujian".
	 *
	 * <p>Terikat pada kotak centang bernama sama di {@code JadwalUjianPMBAction}.</p>
	 *
	 * @param pesertaUjianHarusTelahUjian penanda syarat; {@code null} diperlakukan sebagai
	 *                                    {@code false} saat dibaca
	 * @see #getPesertaUjianHarusTelahUjian()
	 */
	public void setPesertaUjianHarusTelahUjian(Boolean pesertaUjianHarusTelahUjian) {
		this.pesertaUjianHarusTelahUjian = pesertaUjianHarusTelahUjian;
	}
}
