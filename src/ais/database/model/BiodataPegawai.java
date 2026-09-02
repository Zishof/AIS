package ais.database.model;

// Generated Apr 23, 2010 12:45:00 AM by Hibernate Tools 3.2.4.CR1

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

/**
 * Entity biodata rinci (data pribadi) seorang pegawai — tabel {@code public.biodata_pegawai}.
 *
 * <h3>Peran dan pembagian tanggung jawab dengan {@link Pegawai}</h3>
 * <p>AIS memisahkan data seorang karyawan menjadi dua tabel dengan alasan yang sama seperti
 * pasangan {@link Mahasiswa}/{@link BiodataMahasiswa}:</p>
 * <ul>
 *   <li>{@link Pegawai} — data <b>kepegawaian</b>: NIP, satuan kerja, jabatan, golongan, status
 *   aktif, relasi ke {@link Dosen}, dan seterusnya. Inilah entity yang dirujuk dari mana-mana
 *   (penggajian, presensi, hak akses) dan yang jumlah barisnya wajib tetap ramping.</li>
 *   <li>{@code BiodataPegawai} (kelas ini) — data <b>pribadi</b> yang jarang dibaca dan hanya
 *   muncul pada formulir biodata: alamat rumah, nama/pekerjaan orang tua, tinggi &amp; berat badan,
 *   telepon rumah dan HP, SIM, kendaraan, riwayat organisasi, hobi, minat seni, kemampuan bahasa,
 *   riwayat pendidikan SD sampai S3, golongan darah, status nikah, kewarganegaraan, dan agama.</li>
 * </ul>
 * <p>Relasinya satu arah dari sisi ini: {@code BiodataPegawai} memegang {@code @ManyToOne} ke
 * {@link Pegawai} lewat kolom {@code pegawai} yang {@code nullable = false}. Tidak ada
 * {@code @OneToOne} balik di {@link Pegawai}; kode pemanggil (lihat
 * {@code ais.action.master.BiodataPegawaiAction} dan
 * {@code ais.action.master.BiodataPegawaiAccountAction}) mencarinya sendiri lewat
 * {@code Criteria} atas kolom {@code pegawai}. Secara skema tidak ada yang mencegah <i>lebih dari
 * satu</i> baris biodata untuk pegawai yang sama, karena itu form biodata mengambil baris
 * ber-ID terbesar.</p>
 *
 * <h3>Hal paling non-obvious: seluruh getter membayangi nilainya dari {@link BiodataDosen}</h3>
 * <p><b>Hampir setiap getter di kelas ini bukan getter murni.</b> Semuanya berbentuk:</p>
 * <pre>{@code
 * public String getNamaAyah() {
 *     if (ambilBiodataDosen() != null) {
 *         namaAyah = ambilBiodataDosen().getNamaAyah();   // menimpa field sendiri!
 *     }
 *     return this.namaAyah;
 * }
 * }</pre>
 * <p>Artinya: bila pegawai yang bersangkutan <b>juga terdaftar sebagai {@link Dosen}</b>, maka
 * {@link BiodataDosen} diperlakukan sebagai sumber kebenaran dan isi baris
 * {@code biodata_pegawai} <b>ditimpa di memori</b> setiap kali getter dibaca. Konsekuensinya:</p>
 * <ol>
 *   <li>Nilai yang baru saja diset lewat setter bisa "hilang" pada pembacaan berikutnya untuk
 *   pegawai yang merangkap dosen — setter menulis field, getter langsung menimpanya lagi.</li>
 *   <li>Karena entity ini dipetakan {@code dynamicUpdate = true} dan penimpaan terjadi pada field
 *   object yang bisa sedang <i>managed</i> oleh {@link org.hibernate.Session}, sekadar
 *   <b>membaca</b> biodata dapat memicu {@code UPDATE} pada tabel {@code biodata_pegawai} saat
 *   flush — pembacaan yang tidak kasat mata berubah menjadi penulisan.</li>
 *   <li>Untuk pegawai non-dosen ({@code ambilBiodataDosen()} mengembalikan {@code null}) semua
 *   getter berperilaku normal dan mengembalikan isi baris {@code biodata_pegawai} apa adanya.</li>
 * </ol>
 * <p>Pintu masuk semua ini adalah satu method privat: {@link #ambilBiodataDosen()}. Baca
 * Javadoc-nya sebelum menyentuh getter mana pun di kelas ini — di situ dijelaskan bahwa
 * pemanggilan tersebut dapat <b>menulis baris {@code biodata_dosen} baru ke database</b> dan
 * <b>menutup session Hibernate milik thread pemanggil</b>.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan {@link #onUpdate()}.
 *   Perhatikan: keempat properti ini <b>membayangi (shadow)</b> properti bernama sama di
 *   {@link GeneralValueObject} — lihat bagian berikutnya.</li>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)} (juga membayangi
 *   {@code GeneralValueObject#getId()}), {@link #toString()}.</li>
 *   <li><b>Relasi</b> — {@link #getPegawai()}/{@link #setPegawai(Pegawai)} dan helper privat
 *   {@link #ambilBiodataDosen()}.</li>
 *   <li><b>Data pribadi &amp; keluarga</b> — alamat, nama/pekerjaan ayah &amp; ibu, golongan darah,
 *   status nikah, kewarganegaraan, agama, tinggi/berat badan.</li>
 *   <li><b>Kontak &amp; dokumen</b> — telepon rumah, HP, surat izin mengemudi, kendaraan.</li>
 *   <li><b>Organisasi, hobi, bahasa</b> — pernah memimpin organisasi, nama organisasi, hobi, minat
 *   seni, kemampuan bahasa 1..3.</li>
 *   <li><b>Riwayat pendidikan</b> — SD, SMP, SMA (nama + alamat masing-masing) dan S1/S2/S3 (nama +
 *   alamat), ditambah lima slot keahlian.</li>
 * </ul>
 * <p>Tidak ada method bisnis, query statis, maupun helper UI di kelas ini; seluruh isinya adalah
 * pasangan getter/setter properti Hibernate ditambah satu helper privat. Seluruh logika formulir
 * ada di lapisan {@code ais.action.master.*}.</p>
 *
 * <h3>Properti yang membayangi {@link GeneralValueObject}</h3>
 * <p>{@link GeneralValueObject} sudah mendeklarasikan {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} lengkap dengan getter/setter-nya. Kelas ini <b>mendeklarasikan ulang
 * keempatnya</b> sebagai field privat sendiri dan menimpa (override) getter/setter-nya. Efeknya:
 * yang dibaca/ditulis adalah salinan milik kelas ini, sedangkan salinan di induk tetap ada tapi
 * tidak pernah terisi. Ini konsisten dengan entity biodata lain ({@link BiodataMahasiswa},
 * {@link BiodataCalonMahasiswa}) — bukan kekhasan file ini — namun tetap perlu diingat karena
 * method induk yang membaca field induk secara langsung (bukan lewat getter) akan melihat
 * {@code null}. Kontrak {@code equals}/{@code compareTo}/{@code check()} sendiri tetap memakai
 * {@code getId()} sehingga tidak terpengaruh.</p>
 *
 * <h3>Kuirk yang ditemukan saat pendokumentasian (belum diperbaiki)</h3>
 * <ul>
 *   <li>{@link #getSuratIzinMengemudi()} menyalin dari {@code BiodataDosen#getHp()}, bukan dari
 *   nomor SIM — hampir pasti salah tempel.</li>
 *   <li>Anotasi {@code @Column(name = "alamat_asal_s2")} terpasang pada <b>setter</b>
 *   {@link #setAlamatAsalS2(String)}, bukan pada getter-nya. Entity ini memakai <i>property
 *   access</i> (anotasi {@code @Id} ada di {@link #getId()}), jadi Hibernate hanya membaca anotasi
 *   dari getter dan anotasi pada setter itu diabaikan.</li>
 *   <li>Komentar kelas hasil generator masih menyebut "BiodataMahasiswa"; sudah digantikan oleh
 *   dokumentasi ini.</li>
 *   <li>Nama properti {@code keahliah1} (bukan {@code keahlian1}) salah eja sejak awal dan sudah
 *   telanjur dipakai lapisan UI, sehingga tidak bisa diganti tanpa menyentuh pemanggil.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see Pegawai
 * @see BiodataDosen
 * @see BiodataMahasiswa
 * @see ais.action.master.BiodataPegawaiAction
 * @see ais.action.master.BiodataPegawaiAccountAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_pegawai")
public class BiodataPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Entity ini ikut diserialkan saat disimpan di session HTTP/ZK atau
	 * cache, jadi nilainya jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 1995121656124539247L;
	/** Kunci utama tabel {@code biodata_pegawai}; membayangi {@code GeneralValueObject#id}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; membayangi {@code GeneralValueObject#oleh}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini; membayangi {@code GeneralValueObject#olehId}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris biodata ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> —
	 * jejak audit yang sudah ada tidak bisa dihapus lewat setter ini. Perilaku ini sengaja, agar
	 * proses batch yang lupa mengisi identitas tidak menghapus jejak sebelumnya.</p>
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila {@code null} atau kosong
	 * @see GeneralValueObject#setOlehId(String)
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong diabaikan diam-diam sehingga jejak audit lama tidak tertimpa.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 * @see GeneralValueObject#setOleh(String)
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris biodata ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 * @see GeneralValueObject#getOleh()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Implementasi konkret dari method abstrak {@code GeneralValueObject#onUpdate()}: mendelegasikan
	 * ke {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna yang sedang login dan memutakhirkan
	 * {@code tanggal_dirubah}.</p>
	 *
	 * <p>Dipanggil oleh Hibernate, bukan oleh kode aplikasi.</p>
	 *
	 * @see GeneralValueObject#onUpdate()
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir; membayangi {@code GeneralValueObject#tanggal_dirubah}.
	 * Diinisialisasi ke waktu server saat object dibuat sehingga baris baru pun selalu berstempel.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Biasanya diisi otomatis oleh {@link #onUpdate()},
	 * jadi pemanggilan manual hanya untuk migrasi/perbaikan data.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris biodata ini.
	 *
	 * @return waktu perubahan terakhir lengkap dengan jam ({@code TIMESTAMP})
	 * @see GeneralValueObject#getTanggal_dirubah()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks biodata ini, yaitu representasi teks {@link Pegawai} pemiliknya.
	 *
	 * <p><b>Non-obvious:</b> method ini membaca <b>field</b> {@code pegawai} secara langsung, bukan
	 * lewat {@link #getPegawai()}, sehingga tidak ada resolusi proxy lazy. Untuk entity yang
	 * ter-<i>detach</i> hasilnya bisa berupa {@code "null"} walau kolom relasinya sebetulnya
	 * terisi. Penggabungan dengan {@code ""} juga membuat {@code null} berubah menjadi teks
	 * {@code "null"} alih-alih string kosong.</p>
	 *
	 * @return teks pegawai pemilik biodata, atau {@code "null"} bila relasi belum teresolusi
	 */
	public String toString() {
		return pegawai + "";
	}

	/** Pegawai pemilik biodata ini — kolom {@code pegawai}, wajib terisi. */
	private Pegawai pegawai;
	/** Alamat tempat tinggal. */
	private String alamat;
	/** Nama ayah kandung. */
	private String namaAyah;
	/** Pekerjaan ayah, disimpan sebagai teks (bukan relasi seperti di {@link BiodataDosen}). */
	private String pekerjaanAyah;
	/** Nama ibu kandung. */
	private String namaIbu;
	/** Pekerjaan ibu, disimpan sebagai teks (bukan relasi seperti di {@link BiodataDosen}). */
	private String pekerjaanIbu;
	/** Tinggi badan dalam sentimeter. */
	private Integer tinggiBadan;
	/** Penanda pernah menetap di luar negeri (0/1). */
	private Integer pernahMenetapDiLuarNegeri;
	/** Berat badan dalam kilogram. */
	private Integer beratBadan;
	/** Nomor telepon rumah. */
	private String teleponRumah;
	/** Nomor telepon seluler. */
	private String hp;
	/** Nomor/golongan surat izin mengemudi. */
	private String suratIzinMengemudi;
	/**
	 * Kendaraan yang dipakai berangkat kerja/kuliah. Nama properti warisan dari formulir biodata
	 * mahasiswa yang dipakai ulang untuk pegawai.
	 */
	private String kendaraanKuliah;
	/** Penanda pernah memimpin organisasi (0/1). */
	private Integer pernahMemimpinOrganisasi;
	/** Nama organisasi yang pernah diikuti/dipimpin. */
	private String namaOrganisasi;
	/** Hobi. */
	private String hobi;
	/** Minat seni. */
	private String minatSeni;
	/** Kemampuan bahasa slot ke-1. */
	private String kemampuanBahasa1;
	/** Kemampuan bahasa slot ke-2. */
	private String kemampuanBahasa2;
	/** Kemampuan bahasa slot ke-3. */
	private String kemampuanBahasa3;
	/** Nama perguruan tinggi asal jenjang S1. */
	private String asalS1;
	/** Alamat perguruan tinggi asal jenjang S1. */
	private String alamatAsalS1;
	/** Nama perguruan tinggi asal jenjang S2. */
	private String asalS2;
	/** Alamat perguruan tinggi asal jenjang S2. */
	private String alamatAsalS2;
	/** Nama perguruan tinggi asal jenjang S3. */
	private String asalS3;
	/** Alamat perguruan tinggi asal jenjang S3. */
	private String alamatAsalS3;
	/** Keahlian slot ke-1. Ejaan "keahliah" adalah salah ketik lama yang telanjur dipakai. */
	private String keahliah1;
	/** Keahlian slot ke-2. */
	private String keahlian2;
	/** Keahlian slot ke-3. */
	private String keahlian3;
	/** Keahlian slot ke-4. */
	private String keahlian4;
	/** Keahlian slot ke-5. */
	private String keahlian5;
	/** Nama SMA/sederajat asal. */
	private String asalSma;
	/** Alamat SMA/sederajat asal. */
	private String alamatAsalSma;
	/** Nama SMP/sederajat asal. */
	private String asalSmp;
	/** Alamat SMP/sederajat asal. */
	private String alamatAsalSmp;
	/** Nama SD/sederajat asal. */
	private String asalSd;
	/** Alamat SD/sederajat asal. */
	private String alamatAsalSd;
	/** Golongan darah. */
	private String golonganDarah;
	/** Status pernikahan sebagai kode angka. */
	private Integer statusNikah;
	/** Kewarganegaraan. */
	private String kewarganegaraan;
	/** Agama, disimpan sebagai teks (bukan relasi {@link Agama} seperti di {@link BiodataDosen}). */
	private String agama;

	/**
	 * Cache per-instance hasil {@link #ambilBiodataDosen()}. Bukan properti Hibernate (tidak punya
	 * getter publik), jadi tidak ikut dipetakan ke kolom mana pun.
	 */
	private BiodataDosen biodataDosen = null;

	/**
	 * Mencari biodata dosen milik pegawai ini — <b>method paling berbahaya di kelas ini</b>.
	 *
	 * <p>Dipanggil di awal hampir setiap getter kelas ini. Bila pegawai pemilik biodata juga
	 * terdaftar sebagai {@link Dosen}, hasilnya dipakai untuk <b>menimpa field lokal</b> sehingga
	 * {@link BiodataDosen} menjadi sumber kebenaran dan baris {@code biodata_pegawai} sekadar
	 * bayangannya. Bila pegawai bukan dosen, method mengembalikan {@code null} dan seluruh getter
	 * berperilaku sebagai getter biasa.</p>
	 *
	 * <p>Hasil disimpan di field {@link #biodataDosen} sehingga rantai pencarian hanya berjalan
	 * sekali per instance. Rantainya: {@link #getPegawai()} → {@code Pegawai#getDosen()} →
	 * {@code Dosen#ambilBiodata()}.</p>
	 *
	 * <h4>Efek samping yang wajib diketahui</h4>
	 * <ol>
	 *   <li><b>Menulis baris baru ke database.</b> {@code Dosen#ambilBiodata()} adalah bentuk
	 *   singkat dari {@code ambilBiodata(true)}; bila dosen tersebut belum punya baris
	 *   {@code biodata_dosen}, method itu membuka transaksi sendiri dan <b>menyimpan baris
	 *   {@code BiodataDosen} kosong yang baru</b>. Jadi sekadar membaca {@code getHobi()} pada
	 *   pegawai yang merangkap dosen dapat menghasilkan {@code INSERT} ke tabel lain.</li>
	 *   <li><b>Menutup session Hibernate milik thread pemanggil.</b> {@code Dosen#ambilBiodata()}
	 *   memakai {@code HibernateUtil.currentNativeSession()} lalu memanggil
	 *   {@code HibernateUtil.closeSession()} di akhir cabang pencarian maupun cabang penyimpanan.
	 *   Kode pemanggil yang masih memegang session yang sama akan menemukannya sudah tertutup —
	 *   sumber klasik {@code LazyInitializationException} beruntun di layar biodata.</li>
	 *   <li><b>Menimpa field sendiri.</b> Penimpaan terjadi pada object yang mungkin sedang
	 *   <i>managed</i>; dengan {@code dynamicUpdate = true} hal ini bisa berujung {@code UPDATE}
	 *   {@code biodata_pegawai} saat flush.</li>
	 * </ol>
	 *
	 * <p>{@code Dosen#ambilBiodata()} memasang penjaga anti-rekursi berbasis {@code ThreadLocal},
	 * jadi siklus getter → {@code ambilBiodata()} → auto-flush → getter tidak lagi berujung
	 * {@code StackOverflowError} seperti dulu.</p>
	 *
	 * @return biodata dosen milik pegawai ini, atau {@code null} bila pegawai belum terisi atau
	 *         bukan seorang dosen
	 * @see Dosen#ambilBiodata()
	 * @see BiodataDosen
	 */
	private BiodataDosen ambilBiodataDosen() {

		if (biodataDosen == null) {
			if (getPegawai() != null && getPegawai().getDosen() != null) {
				biodataDosen = getPegawai().getDosen().ambilBiodata();
			}
		}
		return biodataDosen;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Semua properti diisi lewat setter.
	 */
	public BiodataPegawai() {
	}

	/**
	 * Mengembalikan kunci utama baris biodata ini.
	 *
	 * <p>Membayangi {@code GeneralValueObject#getId()}. Kolomnya {@code insertable = false} karena
	 * nilai dibangkitkan oleh sequence/identity database.</p>
	 *
	 * @return ID baris, atau {@code null} bila belum pernah disimpan
	 * @see GeneralValueObject#getId()
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama baris biodata ini. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan alamat tempat tinggal pegawai.
	 *
	 * <p><b>Efek samping:</b> bila pegawai merangkap dosen, nilai diambil dari
	 * {@code BiodataDosen#getAlamat()} dan field lokal ditimpa — lihat
	 * {@link #ambilBiodataDosen()} untuk daftar lengkap efek sampingnya (kemungkinan
	 * {@code INSERT} biodata dosen baru dan penutupan session pemanggil).</p>
	 *
	 * @return alamat tempat tinggal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat")
	public String getAlamat() {
		if (ambilBiodataDosen() != null) {
			alamat = ambilBiodataDosen().getAlamat();
		}
		return this.alamat;
	}

	/**
	 * Mengisi alamat tempat tinggal.
	 *
	 * <p>Untuk pegawai yang merangkap dosen nilai ini akan tertimpa lagi pada pembacaan berikutnya
	 * oleh {@link #getAlamat()}.</p>
	 *
	 * @param alamat alamat tempat tinggal
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan nama ayah kandung.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@code BiodataDosen#getNamaAyah()} bila pegawai
	 * merangkap dosen — lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nama ayah, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_ayah", length = 100)
	public String getNamaAyah() {
		if (ambilBiodataDosen() != null) {
			namaAyah = ambilBiodataDosen().getNamaAyah();
		}
		return this.namaAyah;
	}

	/**
	 * Mengisi nama ayah kandung.
	 *
	 * @param namaAyah nama ayah
	 */
	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	/**
	 * Mengembalikan pekerjaan ayah sebagai teks.
	 *
	 * <p><b>Perbedaan tipe dengan {@link BiodataDosen}:</b> di sini pekerjaan disimpan sebagai
	 * {@code String}, sedangkan {@code BiodataDosen#getPekerjaanAyah()} mengembalikan entity
	 * {@code PekerjaanOrangTua}. Karena itu penimpaan dilakukan lewat {@code getNama()}.</p>
	 *
	 * <p><b>Penanganan galat:</b> seluruh blok penimpaan dibungkus {@code try/catch} karena
	 * instance {@link BiodataDosen} yang dipakai bisa berupa instance kanonik/bersama yang
	 * proxy-nya terikat ke {@link org.hibernate.Session} lain yang sudah tertutup —
	 * {@code LazyInitializationException} di sini ditelan (dicatat lewat
	 * {@code ErrorAuditUtil}) dan nilai lokal dipertahankan sebagai fallback, agar getter tidak
	 * meledak di tengah render formulir.</p>
	 *
	 * <p><b>Efek samping:</b> lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return pekerjaan ayah, atau {@code null} bila belum diisi
	 */
	@Column(name = "pekerjaan_ayah", length = 150)
	public String getPekerjaanAyah() {
		try {
			// FIX LazyInitializationException: ambilBiodataDosen()/getPekerjaanAyah() bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (ambilBiodataDosen() != null && ambilBiodataDosen().getPekerjaanAyah() != null) {
				pekerjaanAyah = ambilBiodataDosen().getPekerjaanAyah().getNama();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/BiodataPegawai.java:getPekerjaanAyah-lazy");
		}
		return this.pekerjaanAyah;
	}

	/**
	 * Mengisi pekerjaan ayah sebagai teks bebas.
	 *
	 * @param pekerjaanAyah pekerjaan ayah
	 */
	public void setPekerjaanAyah(String pekerjaanAyah) {
		this.pekerjaanAyah = pekerjaanAyah;
	}

	/**
	 * Mengembalikan nama ibu kandung.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@code BiodataDosen#getNamaIbu()} bila pegawai merangkap
	 * dosen — lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nama ibu, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_ibu", length = 100)
	public String getNamaIbu() {
		if (ambilBiodataDosen() != null) {
			namaIbu = ambilBiodataDosen().getNamaIbu();
		}
		return this.namaIbu;
	}

	/**
	 * Mengisi nama ibu kandung.
	 *
	 * @param namaIbu nama ibu
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Mengembalikan pekerjaan ibu sebagai teks.
	 *
	 * <p>Perilakunya identik dengan {@link #getPekerjaanAyah()}: nilai diambil dari entity
	 * {@code PekerjaanOrangTua} milik {@link BiodataDosen} lewat {@code getNama()}, dan seluruh
	 * blok dibungkus {@code try/catch} agar {@code LazyInitializationException} dari proxy yang
	 * session-nya sudah tertutup tidak menggagalkan render formulir.</p>
	 *
	 * @return pekerjaan ibu, atau {@code null} bila belum diisi
	 */
	@Column(name = "pekerjaan_ibu", length = 150)
	public String getPekerjaanIbu() {
		try {
			// FIX LazyInitializationException: ambilBiodataDosen()/getPekerjaanIbu() bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (ambilBiodataDosen() != null && ambilBiodataDosen().getPekerjaanIbu() != null) {
				pekerjaanIbu = ambilBiodataDosen().getPekerjaanIbu().getNama();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/BiodataPegawai.java:getPekerjaanIbu-lazy");
		}
		return this.pekerjaanIbu;
	}

	/**
	 * Mengisi pekerjaan ibu sebagai teks bebas.
	 *
	 * @param pekerjaanIbu pekerjaan ibu
	 */
	public void setPekerjaanIbu(String pekerjaanIbu) {
		this.pekerjaanIbu = pekerjaanIbu;
	}

	/**
	 * Mengembalikan tinggi badan dalam sentimeter.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return tinggi badan (cm), atau {@code null} bila belum diisi
	 */
	@Column(name = "tinggi_badan")
	public Integer getTinggiBadan() {
		if (ambilBiodataDosen() != null) {
			tinggiBadan = ambilBiodataDosen().getTinggiBadan();
		}
		return this.tinggiBadan;
	}

	/**
	 * Mengisi tinggi badan dalam sentimeter.
	 *
	 * @param tinggiBadan tinggi badan (cm)
	 */
	public void setTinggiBadan(Integer tinggiBadan) {
		this.tinggiBadan = tinggiBadan;
	}

	/**
	 * Mengembalikan penanda pernah menetap di luar negeri.
	 *
	 * <p>Disimpan sebagai angka (konvensi 0 = tidak, 1 = ya) alih-alih boolean, mengikuti bentuk
	 * kolom lama.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return penanda pernah menetap di luar negeri, atau {@code null} bila belum diisi
	 */
	@Column(name = "pernah_menetap_di_luar_negeri")
	public Integer getPernahMenetapDiLuarNegeri() {
		if (ambilBiodataDosen() != null) {
			pernahMenetapDiLuarNegeri = ambilBiodataDosen().getPernahMenetapDiLuarNegeri();
		}
		return this.pernahMenetapDiLuarNegeri;
	}

	/**
	 * Mengisi penanda pernah menetap di luar negeri.
	 *
	 * @param pernahMenetapDiLuarNegeri 0 = tidak, 1 = ya
	 */
	public void setPernahMenetapDiLuarNegeri(Integer pernahMenetapDiLuarNegeri) {
		this.pernahMenetapDiLuarNegeri = pernahMenetapDiLuarNegeri;
	}

	/**
	 * Mengembalikan berat badan dalam kilogram.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return berat badan (kg), atau {@code null} bila belum diisi
	 */
	@Column(name = "berat_badan")
	public Integer getBeratBadan() {
		if (ambilBiodataDosen() != null) {
			beratBadan = ambilBiodataDosen().getBeratBadan();
		}
		return this.beratBadan;
	}

	/**
	 * Mengisi berat badan dalam kilogram.
	 *
	 * @param beratBadan berat badan (kg)
	 */
	public void setBeratBadan(Integer beratBadan) {
		this.beratBadan = beratBadan;
	}

	/**
	 * Mengembalikan nomor telepon rumah.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nomor telepon rumah, atau {@code null} bila belum diisi
	 */
	@Column(name = "telepon_rumah", length = 20)
	public String getTeleponRumah() {
		if (ambilBiodataDosen() != null) {
			teleponRumah = ambilBiodataDosen().getTeleponRumah();
		}
		return this.teleponRumah;
	}

	/**
	 * Mengisi nomor telepon rumah.
	 *
	 * @param teleponRumah nomor telepon rumah
	 */
	public void setTeleponRumah(String teleponRumah) {
		this.teleponRumah = teleponRumah;
	}

	/**
	 * Mengembalikan nomor telepon seluler.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nomor HP, atau {@code null} bila belum diisi
	 */
	@Column(name = "hp", length = 20)
	public String getHp() {
		if (ambilBiodataDosen() != null) {
			hp = ambilBiodataDosen().getHp();
		}
		return this.hp;
	}

	/**
	 * Mengisi nomor telepon seluler.
	 *
	 * @param hp nomor HP
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Mengembalikan data surat izin mengemudi (SIM).
	 *
	 * <p><b>BUG yang sengaja tidak diperbaiki di sini (hanya dicatat):</b> untuk pegawai yang
	 * merangkap dosen, field {@code suratIzinMengemudi} ditimpa dengan
	 * {@code ambilBiodataDosen().getHp()} — <b>nomor HP</b>, bukan data SIM. Hampir pasti salah
	 * tempel dari {@link #getHp()} di atasnya. Akibatnya kolom SIM pada layar biodata pegawai-dosen
	 * menampilkan nomor HP, dan bila baris kemudian ikut ter-flush, nilai keliru itu bisa
	 * tersimpan permanen ke kolom {@code surat_izin_mengemudi}. Perbaikannya semestinya memanggil
	 * {@code ambilBiodataDosen().getSuratIzinMengemudi()}, tetapi perubahan perilaku semacam itu
	 * berada di luar lingkup pekerjaan dokumentasi ini.</p>
	 *
	 * @return data SIM — atau, untuk pegawai yang merangkap dosen, nomor HP (lihat catatan bug)
	 */
	@Column(name = "surat_izin_mengemudi", length = 50)
	public String getSuratIzinMengemudi() {
		if (ambilBiodataDosen() != null) {
			suratIzinMengemudi = ambilBiodataDosen().getHp();
		}
		return this.suratIzinMengemudi;
	}

	/**
	 * Mengisi data surat izin mengemudi.
	 *
	 * @param suratIzinMengemudi nomor/golongan SIM
	 */
	public void setSuratIzinMengemudi(String suratIzinMengemudi) {
		this.suratIzinMengemudi = suratIzinMengemudi;
	}

	/**
	 * Mengembalikan kendaraan yang dipakai berangkat kerja.
	 *
	 * <p>Nama properti ({@code kendaraanKuliah}) dan kolom ({@code kendaraan_kuliah}) merupakan
	 * warisan formulir biodata mahasiswa yang dipakai ulang untuk pegawai.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return jenis kendaraan, atau {@code null} bila belum diisi
	 */
	@Column(name = "kendaraan_kuliah", length = 50)
	public String getKendaraanKuliah() {
		if (ambilBiodataDosen() != null) {
			kendaraanKuliah = ambilBiodataDosen().getKendaraanKuliah();
		}
		return this.kendaraanKuliah;
	}

	/**
	 * Mengisi kendaraan yang dipakai berangkat kerja.
	 *
	 * @param kendaraanKuliah jenis kendaraan
	 */
	public void setKendaraanKuliah(String kendaraanKuliah) {
		this.kendaraanKuliah = kendaraanKuliah;
	}

	/**
	 * Mengembalikan penanda pernah memimpin organisasi (0 = tidak, 1 = ya).
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return penanda pernah memimpin organisasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "pernah_memimpin_organisasi")
	public Integer getPernahMemimpinOrganisasi() {
		if (ambilBiodataDosen() != null) {
			pernahMemimpinOrganisasi = ambilBiodataDosen().getPernahMemimpinOrganisasi();
		}
		return this.pernahMemimpinOrganisasi;
	}

	/**
	 * Mengisi penanda pernah memimpin organisasi.
	 *
	 * @param pernahMemimpinOrganisasi 0 = tidak, 1 = ya
	 */
	public void setPernahMemimpinOrganisasi(Integer pernahMemimpinOrganisasi) {
		this.pernahMemimpinOrganisasi = pernahMemimpinOrganisasi;
	}

	/**
	 * Mengembalikan nama organisasi yang pernah diikuti atau dipimpin.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nama organisasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_organisasi", length = 50)
	public String getNamaOrganisasi() {
		if (ambilBiodataDosen() != null) {
			namaOrganisasi = ambilBiodataDosen().getNamaOrganisasi();
		}
		return this.namaOrganisasi;
	}

	/**
	 * Mengisi nama organisasi yang pernah diikuti atau dipimpin.
	 *
	 * @param namaOrganisasi nama organisasi
	 */
	public void setNamaOrganisasi(String namaOrganisasi) {
		this.namaOrganisasi = namaOrganisasi;
	}

	/**
	 * Mengembalikan hobi pegawai.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return hobi, atau {@code null} bila belum diisi
	 */
	@Column(name = "hobi")
	public String getHobi() {
		if (ambilBiodataDosen() != null) {
			hobi = ambilBiodataDosen().getHobi();
		}
		return this.hobi;
	}

	/**
	 * Mengisi hobi pegawai.
	 *
	 * @param hobi hobi
	 */
	public void setHobi(String hobi) {
		this.hobi = hobi;
	}

	/**
	 * Mengembalikan minat seni pegawai.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return minat seni, atau {@code null} bila belum diisi
	 */
	@Column(name = "minat_seni")
	public String getMinatSeni() {
		if (ambilBiodataDosen() != null) {
			minatSeni = ambilBiodataDosen().getMinatSeni();
		}
		return this.minatSeni;
	}

	/**
	 * Mengisi minat seni pegawai.
	 *
	 * @param minatSeni minat seni
	 */
	public void setMinatSeni(String minatSeni) {
		this.minatSeni = minatSeni;
	}

	/**
	 * Mengembalikan kemampuan bahasa slot ke-1.
	 *
	 * <p>Kemampuan bahasa disimpan sebagai tiga kolom terpisah, bukan tabel anak, sehingga jumlah
	 * bahasa yang bisa dicatat dibatasi tiga.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return kemampuan bahasa ke-1, atau {@code null} bila belum diisi
	 */
	@Column(name = "kemampuan_bahasa1", length = 50)
	public String getKemampuanBahasa1() {
		if (ambilBiodataDosen() != null) {
			kemampuanBahasa1 = ambilBiodataDosen().getKemampuanBahasa1();
		}
		return this.kemampuanBahasa1;
	}

	/**
	 * Mengisi kemampuan bahasa slot ke-1.
	 *
	 * @param kemampuanBahasa1 kemampuan bahasa ke-1
	 */
	public void setKemampuanBahasa1(String kemampuanBahasa1) {
		this.kemampuanBahasa1 = kemampuanBahasa1;
	}

	/**
	 * Mengembalikan kemampuan bahasa slot ke-2.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return kemampuan bahasa ke-2, atau {@code null} bila belum diisi
	 */
	@Column(name = "kemampuan_bahasa2", length = 50)
	public String getKemampuanBahasa2() {
		if (ambilBiodataDosen() != null) {
			kemampuanBahasa2 = ambilBiodataDosen().getKemampuanBahasa2();
		}
		return this.kemampuanBahasa2;
	}

	/**
	 * Mengisi kemampuan bahasa slot ke-2.
	 *
	 * @param kemampuanBahasa2 kemampuan bahasa ke-2
	 */
	public void setKemampuanBahasa2(String kemampuanBahasa2) {
		this.kemampuanBahasa2 = kemampuanBahasa2;
	}

	/**
	 * Mengembalikan kemampuan bahasa slot ke-3.
	 *
	 * <p>Panjang kolomnya 255 karakter, berbeda dari dua slot sebelumnya yang hanya 50 —
	 * kemungkinan besar tidak disengaja, tetapi sudah menjadi bentuk skema yang berjalan.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return kemampuan bahasa ke-3, atau {@code null} bila belum diisi
	 */
	@Column(name = "kemampuan_bahasa3", length = 255)
	public String getKemampuanBahasa3() {
		if (ambilBiodataDosen() != null) {
			kemampuanBahasa3 = ambilBiodataDosen().getKemampuanBahasa3();
		}
		return this.kemampuanBahasa3;
	}

	/**
	 * Mengisi kemampuan bahasa slot ke-3.
	 *
	 * @param kemampuanBahasa3 kemampuan bahasa ke-3
	 */
	public void setKemampuanBahasa3(String kemampuanBahasa3) {
		this.kemampuanBahasa3 = kemampuanBahasa3;
	}

	/**
	 * Mengembalikan nama SMA/sederajat asal, sudah dibersihkan.
	 *
	 * <p><b>Perbedaan penting dengan getter lain:</b> nilai balik <b>tidak pernah {@code null}</b> —
	 * bila field kosong, method mengembalikan string kosong. Selain itu isinya di-{@code trim()}
	 * dan tanda petik tunggal maupun ganda dibuang. Pembersihan ini ada karena nama sekolah kerap
	 * ditempelkan langsung ke dalam string SQL/laporan di lapisan pemanggil, sehingga tanda petik
	 * bisa merusak kueri atau tampilan.</p>
	 *
	 * <p><b>Catatan:</b> pembersihan hanya berlaku pada nilai yang dikembalikan; field internal
	 * (dan karenanya isi kolom database) tetap menyimpan teks aslinya.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nama SMA asal tanpa tanda petik; string kosong bila belum diisi
	 */
	@Column(name = "asal_sma", length = 255)
	public String getAsalSma() {

		if (ambilBiodataDosen() != null) {
			asalSma = ambilBiodataDosen().getAsalSma();
		}

		return this.asalSma == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSma.trim(), "'", ""), "\"", "");
	}

	/**
	 * Mengisi nama SMA/sederajat asal. Nilai disimpan apa adanya; pembersihan tanda petik baru
	 * dilakukan saat dibaca oleh {@link #getAsalSma()}.
	 *
	 * @param asalSma nama SMA asal
	 */
	public void setAsalSma(String asalSma) {
		this.asalSma = asalSma;
	}

	/**
	 * Mengembalikan alamat SMA/sederajat asal.
	 *
	 * <p>Berbeda dari {@link #getAsalSma()}, alamat tidak dibersihkan dari tanda petik dan bisa
	 * bernilai {@code null}.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return alamat SMA asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_sma")
	public String getAlamatAsalSma() {

		if (ambilBiodataDosen() != null) {
			alamatAsalSma = ambilBiodataDosen().getAlamatAsalSma();
		}

		return this.alamatAsalSma;
	}

	/**
	 * Mengisi alamat SMA/sederajat asal.
	 *
	 * @param alamatAsalSma alamat SMA asal
	 */
	public void setAlamatAsalSma(String alamatAsalSma) {
		this.alamatAsalSma = alamatAsalSma;
	}

	/**
	 * Mengembalikan nama SMP/sederajat asal, sudah dibersihkan.
	 *
	 * <p>Perlakuannya identik dengan {@link #getAsalSma()}: hasil di-{@code trim()}, tanda petik
	 * tunggal dan ganda dibuang, dan nilai balik tidak pernah {@code null} (string kosong bila
	 * belum diisi). Perhatikan bahwa kolomnya hanya 50 karakter, jauh lebih pendek daripada
	 * {@code asal_sma} yang 255.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nama SMP asal tanpa tanda petik; string kosong bila belum diisi
	 */
	@Column(name = "asal_smp", length = 50)
	public String getAsalSmp() {

		if (ambilBiodataDosen() != null) {
			asalSmp = ambilBiodataDosen().getAsalSmp();
		}

		return this.asalSmp == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSmp.trim(), "'", ""), "\"", "");
	}

	/**
	 * Mengisi nama SMP/sederajat asal. Nilai disimpan apa adanya.
	 *
	 * @param asalSmp nama SMP asal
	 */
	public void setAsalSmp(String asalSmp) {
		this.asalSmp = asalSmp;
	}

	/**
	 * Mengembalikan alamat SMP/sederajat asal, tanpa pembersihan tanda petik.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return alamat SMP asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_smp")
	public String getAlamatAsalSmp() {

		if (ambilBiodataDosen() != null) {
			alamatAsalSmp = ambilBiodataDosen().getAlamatAsalSmp();
		}

		return this.alamatAsalSmp;
	}

	/**
	 * Mengisi alamat SMP/sederajat asal.
	 *
	 * @param alamatAsalSmp alamat SMP asal
	 */
	public void setAlamatAsalSmp(String alamatAsalSmp) {
		this.alamatAsalSmp = alamatAsalSmp;
	}

	/**
	 * Mengembalikan nama SD/sederajat asal, sudah dibersihkan.
	 *
	 * <p>Perlakuannya identik dengan {@link #getAsalSma()} dan {@link #getAsalSmp()}: di-{@code trim()},
	 * tanda petik dibuang, dan tidak pernah mengembalikan {@code null}.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nama SD asal tanpa tanda petik; string kosong bila belum diisi
	 */
	@Column(name = "asal_sd", length = 50)
	public String getAsalSd() {

		if (ambilBiodataDosen() != null) {
			asalSd = ambilBiodataDosen().getAsalSd();
		}

		return this.asalSd == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSd.trim(), "'", ""), "\"", "");
	}

	/**
	 * Mengisi nama SD/sederajat asal. Nilai disimpan apa adanya.
	 *
	 * @param asalSd nama SD asal
	 */
	public void setAsalSd(String asalSd) {
		this.asalSd = asalSd;
	}

	/**
	 * Mengembalikan alamat SD/sederajat asal, tanpa pembersihan tanda petik.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return alamat SD asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_sd")
	public String getAlamatAsalSd() {

		if (ambilBiodataDosen() != null) {
			alamatAsalSd = ambilBiodataDosen().getAlamatAsalSd();
		}

		return this.alamatAsalSd;
	}

	/**
	 * Mengisi alamat SD/sederajat asal.
	 *
	 * @param alamatAsalSd alamat SD asal
	 */
	public void setAlamatAsalSd(String alamatAsalSd) {
		this.alamatAsalSd = alamatAsalSd;
	}

	/**
	 * Mengembalikan golongan darah.
	 *
	 * <p>Disimpan sebagai teks bebas sepanjang maksimal 10 karakter, tanpa validasi daftar nilai;
	 * pembatasan pilihan (A/B/AB/O) ada di lapisan UI.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return golongan darah, atau {@code null} bila belum diisi
	 */
	@Column(name = "golongan_darah", length = 10)
	public String getGolonganDarah() {

		if (ambilBiodataDosen() != null) {
			golonganDarah = ambilBiodataDosen().getGolonganDarah();
		}

		return this.golonganDarah;
	}

	/**
	 * Mengisi golongan darah.
	 *
	 * @param golonganDarah golongan darah
	 */
	public void setGolonganDarah(String golonganDarah) {
		this.golonganDarah = golonganDarah;
	}

	/**
	 * Mengembalikan status pernikahan sebagai kode angka.
	 *
	 * <p>Pemetaan kode ke label (belum menikah/menikah/duda/janda dan seterusnya) tidak disimpan di
	 * entity ini, melainkan ditentukan oleh daftar pilihan di formulir biodata.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return kode status nikah, atau {@code null} bila belum diisi
	 */
	@Column(name = "status_nikah")
	public Integer getStatusNikah() {

		if (ambilBiodataDosen() != null) {
			statusNikah = ambilBiodataDosen().getStatusNikah();
		}

		return this.statusNikah;
	}

	/**
	 * Mengisi status pernikahan sebagai kode angka.
	 *
	 * @param statusNikah kode status nikah
	 */
	public void setStatusNikah(Integer statusNikah) {
		this.statusNikah = statusNikah;
	}

	/**
	 * Mengembalikan kewarganegaraan.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return kewarganegaraan, atau {@code null} bila belum diisi
	 */
	@Column(name = "kewarganegaraan", length = 10)
	public String getKewarganegaraan() {

		if (ambilBiodataDosen() != null) {
			kewarganegaraan = ambilBiodataDosen().getKewarganegaraan();
		}

		return this.kewarganegaraan;
	}

	/**
	 * Mengisi kewarganegaraan.
	 *
	 * @param kewarganegaraan kewarganegaraan
	 */
	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	/**
	 * Mengembalikan agama sebagai teks.
	 *
	 * <p><b>Perbedaan tipe dengan {@link BiodataDosen}:</b> di sini agama berupa {@code String},
	 * sedangkan {@code BiodataDosen#getAgama()} mengembalikan entity {@code Agama} — karena itu
	 * penimpaan dilakukan lewat {@code getNama()}.</p>
	 *
	 * <p><b>Penanganan galat:</b> seperti {@link #getPekerjaanAyah()}, blok penimpaan dibungkus
	 * {@code try/catch} agar {@code LazyInitializationException} dari proxy {@code Agama} yang
	 * session-nya sudah tertutup tidak menggagalkan getter; nilai lokal dipertahankan sebagai
	 * fallback dan galatnya dicatat lewat {@code ErrorAuditUtil}.</p>
	 *
	 * <p><b>Catatan historis:</b> rantai {@code getAgama()} → {@code ambilBiodata()} → query →
	 * auto-flush → {@code getAgama()} pernah menyebabkan {@code StackOverflowError}; penjaga
	 * {@code ThreadLocal} di {@code Dosen#ambilBiodata(boolean)} yang memutusnya.</p>
	 *
	 * @return nama agama, atau {@code null} bila belum diisi
	 */
	@Column(name = "agama", length = 20)
	public String getAgama() {

		try {
			// FIX LazyInitializationException: ambilBiodataDosen()/getAgama() bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (ambilBiodataDosen() != null && ambilBiodataDosen().getAgama() != null) {
				agama = ambilBiodataDosen().getAgama().getNama();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/BiodataPegawai.java:getAgama-lazy");
		}

		return this.agama;
	}

	/**
	 * Mengisi agama sebagai teks bebas.
	 *
	 * @param agama nama agama
	 */
	public void setAgama(String agama) {
		this.agama = agama;
	}

	/**
	 * Mengisi pegawai pemilik biodata ini.
	 *
	 * <p>Wajib diisi sebelum baris disimpan — kolom {@code pegawai} dideklarasikan
	 * {@code nullable = false}.</p>
	 *
	 * <p><b>Perhatian:</b> mengganti pegawai setelah beberapa getter sempat dipanggil tidak akan
	 * mengosongkan cache {@link #biodataDosen}, sehingga penimpaan bisa terus memakai biodata dosen
	 * milik pegawai yang lama.</p>
	 *
	 * @param pegawai pegawai pemilik biodata
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan pegawai pemilik biodata ini.
	 *
	 * <p>Mengikuti pola getter relasi standar AIS: hasilnya dilewatkan
	 * {@link GeneralValueObject#check(Object)} lebih dulu agar proxy lazy yang mungkin sudah
	 * ter-<i>detach</i> teresolusi (dari {@code EntityIdentityMap}, cache, session yang tersedia,
	 * atau reload lewat session baru) dan hasilnya ditugaskan kembali ke field karena bisa berupa
	 * instance yang berbeda.</p>
	 *
	 * <p>Relasi dipetakan {@code LAZY} dengan cascade {@code PERSIST} dan {@code MERGE} — menyimpan
	 * biodata ikut menyimpan pegawai yang belum tersimpan, tetapi menghapus biodata tidak menghapus
	 * pegawai.</p>
	 *
	 * @return pegawai pemilik biodata, atau {@code null} bila belum diisi
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Mengisi nama perguruan tinggi asal jenjang S1.
	 *
	 * @param asalS1 nama perguruan tinggi S1
	 */
	public void setAsalS1(String asalS1) {
		this.asalS1 = asalS1;
	}

	/**
	 * Mengembalikan nama perguruan tinggi asal jenjang S1.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}. Berbeda dari {@link #getAsalSma()}, riwayat pendidikan
	 * tinggi tidak dibersihkan dari tanda petik dan bisa bernilai {@code null}.</p>
	 *
	 * @return nama perguruan tinggi S1, atau {@code null} bila belum diisi
	 */
	@Column(name = "asal_s1", length = 100)
	public String getAsalS1() {

		if (ambilBiodataDosen() != null) {
			asalS1 = ambilBiodataDosen().getAsalS1();
		}

		return asalS1;
	}

	/**
	 * Mengisi alamat perguruan tinggi asal jenjang S1.
	 *
	 * @param alamatAsalS1 alamat perguruan tinggi S1
	 */
	public void setAlamatAsalS1(String alamatAsalS1) {
		this.alamatAsalS1 = alamatAsalS1;
	}

	/**
	 * Mengembalikan alamat perguruan tinggi asal jenjang S1.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return alamat perguruan tinggi S1, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_s1", length = 255)
	public String getAlamatAsalS1() {

		if (ambilBiodataDosen() != null) {
			alamatAsalS1 = ambilBiodataDosen().getAlamatAsalS1();
		}

		return alamatAsalS1;
	}

	/**
	 * Mengisi nama perguruan tinggi asal jenjang S2.
	 *
	 * @param asalS2 nama perguruan tinggi S2
	 */
	public void setAsalS2(String asalS2) {
		this.asalS2 = asalS2;
	}

	/**
	 * Mengembalikan nama perguruan tinggi asal jenjang S2.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nama perguruan tinggi S2, atau {@code null} bila belum diisi
	 */
	@Column(name = "asal_s2", length = 100)
	public String getAsalS2() {

		if (ambilBiodataDosen() != null) {
			asalS2 = ambilBiodataDosen().getAsalS2();
		}

		return asalS2;
	}

	/**
	 * Mengisi alamat perguruan tinggi asal jenjang S2.
	 *
	 * <p><b>Kuirk pemetaan yang sengaja hanya dicatat, tidak diperbaiki:</b> anotasi
	 * {@code @Column(name = "alamat_asal_s2", length = 255)} terpasang pada <b>setter ini</b>,
	 * bukan pada {@link #getAlamatAsalS2()} seperti seluruh properti lain di kelas ini. Karena
	 * entity ini memakai <i>property access</i> (anotasi {@code @Id} berada di {@link #getId()}),
	 * Hibernate hanya membaca anotasi dari getter dan mengabaikan anotasi di setter. Akibatnya
	 * properti {@code alamatAsalS2} tidak punya {@code @Column} efektif dan nama kolomnya jatuh ke
	 * strategi penamaan bawaan (nama properti apa adanya), bukan {@code alamat_asal_s2}. Karena
	 * konfigurasi memakai {@code hbm2ddl.auto=update}, kolom tambahan itu kemungkinan besar dibuat
	 * otomatis, sehingga nilai yang ditulis lewat setter ini tidak mendarat di kolom
	 * {@code alamat_asal_s2} yang dipakai laporan/kueri lain. Memindahkan anotasi ke getter adalah
	 * perubahan skema, bukan perubahan dokumentasi, jadi tidak dilakukan di sini.</p>
	 *
	 * @param alamatAsalS2 alamat perguruan tinggi S2
	 */
	@Column(name = "alamat_asal_s2", length = 255)
	public void setAlamatAsalS2(String alamatAsalS2) {
		this.alamatAsalS2 = alamatAsalS2;
	}

	/**
	 * Mengembalikan alamat perguruan tinggi asal jenjang S2.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * <p><b>Catatan pemetaan:</b> getter ini tidak beranotasi {@code @Column}; anotasinya salah
	 * tempat di {@link #setAlamatAsalS2(String)}. Lihat penjelasan lengkapnya di sana.</p>
	 *
	 * @return alamat perguruan tinggi S2, atau {@code null} bila belum diisi
	 */
	public String getAlamatAsalS2() {

		if (ambilBiodataDosen() != null) {
			alamatAsalS2 = ambilBiodataDosen().getAlamatAsalS2();
		}

		return alamatAsalS2;
	}

	/**
	 * Mengisi nama perguruan tinggi asal jenjang S3.
	 *
	 * @param asalS3 nama perguruan tinggi S3
	 */
	public void setAsalS3(String asalS3) {
		this.asalS3 = asalS3;
	}

	/**
	 * Mengembalikan nama perguruan tinggi asal jenjang S3.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return nama perguruan tinggi S3, atau {@code null} bila belum diisi
	 */
	@Column(name = "asal_s3", length = 100)
	public String getAsalS3() {

		if (ambilBiodataDosen() != null) {
			asalS3 = ambilBiodataDosen().getAsalS3();
		}

		return asalS3;
	}

	/**
	 * Mengisi alamat perguruan tinggi asal jenjang S3.
	 *
	 * @param alamatAsalS3 alamat perguruan tinggi S3
	 */
	public void setAlamatAsalS3(String alamatAsalS3) {
		this.alamatAsalS3 = alamatAsalS3;
	}

	/**
	 * Mengembalikan alamat perguruan tinggi asal jenjang S3.
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return alamat perguruan tinggi S3, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_s3", length = 255)
	public String getAlamatAsalS3() {

		if (ambilBiodataDosen() != null) {
			alamatAsalS3 = ambilBiodataDosen().getAlamatAsalS3();
		}

		return alamatAsalS3;
	}

	/**
	 * Mengisi keahlian slot ke-1.
	 *
	 * <p>Ejaan {@code Keahliah} (bukan {@code Keahlian}) adalah salah ketik lama yang sudah
	 * telanjur dipakai lapisan UI dan nama kolom database, jadi dibiarkan.</p>
	 *
	 * @param keahliah1 keahlian ke-1
	 */
	public void setKeahliah1(String keahliah1) {
		this.keahliah1 = keahliah1;
	}

	/**
	 * Mengembalikan keahlian slot ke-1.
	 *
	 * <p>Kelima slot keahlian dipetakan ke kolom {@code keahliah1}..{@code keahliah5} — perhatikan
	 * bahwa hanya slot pertama yang nama method-nya ikut salah eja ({@code getKeahliah1} vs
	 * {@code getKeahlian2}..{@code getKeahlian5}), sedangkan nama kolomnya konsisten salah eja
	 * untuk kelimanya.</p>
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return keahlian ke-1, atau {@code null} bila belum diisi
	 */
	@Column(name = "keahliah1", length = 100)
	public String getKeahliah1() {

		if (ambilBiodataDosen() != null) {
			keahliah1 = ambilBiodataDosen().getKeahliah1();
		}

		return keahliah1;
	}

	/**
	 * Mengisi keahlian slot ke-2.
	 *
	 * @param keahlian2 keahlian ke-2
	 */
	public void setKeahlian2(String keahlian2) {
		this.keahlian2 = keahlian2;
	}

	/**
	 * Mengembalikan keahlian slot ke-2 (kolom {@code keahliah2}).
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return keahlian ke-2, atau {@code null} bila belum diisi
	 */
	@Column(name = "keahliah2", length = 100)
	public String getKeahlian2() {

		if (ambilBiodataDosen() != null) {
			keahlian2 = ambilBiodataDosen().getKeahlian2();
		}

		return keahlian2;
	}

	/**
	 * Mengisi keahlian slot ke-3.
	 *
	 * @param keahlian3 keahlian ke-3
	 */
	public void setKeahlian3(String keahlian3) {
		this.keahlian3 = keahlian3;
	}

	/**
	 * Mengembalikan keahlian slot ke-3 (kolom {@code keahliah3}).
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return keahlian ke-3, atau {@code null} bila belum diisi
	 */
	@Column(name = "keahliah3", length = 100)
	public String getKeahlian3() {

		if (ambilBiodataDosen() != null) {
			keahlian3 = ambilBiodataDosen().getKeahlian3();
		}

		return keahlian3;
	}

	/**
	 * Mengisi keahlian slot ke-4.
	 *
	 * @param keahlian4 keahlian ke-4
	 */
	public void setKeahlian4(String keahlian4) {
		this.keahlian4 = keahlian4;
	}

	/**
	 * Mengembalikan keahlian slot ke-4 (kolom {@code keahliah4}).
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return keahlian ke-4, atau {@code null} bila belum diisi
	 */
	@Column(name = "keahliah4", length = 100)
	public String getKeahlian4() {

		if (ambilBiodataDosen() != null) {
			keahlian4 = ambilBiodataDosen().getKeahlian4();
		}

		return keahlian4;
	}

	/**
	 * Mengisi keahlian slot ke-5.
	 *
	 * @param keahlian5 keahlian ke-5
	 */
	public void setKeahlian5(String keahlian5) {
		this.keahlian5 = keahlian5;
	}

	/**
	 * Mengembalikan keahlian slot ke-5 (kolom {@code keahliah5}).
	 *
	 * <p><b>Efek samping:</b> ditimpa dari {@link BiodataDosen} bila pegawai merangkap dosen —
	 * lihat {@link #ambilBiodataDosen()}.</p>
	 *
	 * @return keahlian ke-5, atau {@code null} bila belum diisi
	 */
	@Column(name = "keahliah5", length = 100)
	public String getKeahlian5() {

		if (ambilBiodataDosen() != null) {
			keahlian5 = ambilBiodataDosen().getKeahlian5();
		}

		return keahlian5;
	}

}
