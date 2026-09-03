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
 * Entity master <b>cabang/bidang prestasi siswa</b> — tabel
 * <code>sekolah.cabang_prestasi_siswa</code>.
 *
 * <p><b>Domain TERVERIFIKASI dari kode, bukan dugaan.</b> Inilah entity yang benar-benar memegang
 * pembagian <i>bidang lomba</i>: pemetaan keras di {@link #getKode()} dan penyemaian otomatis di
 * {@code ais.action.master.sekolah.PrestasiSiswaAction.init()} sama-sama menyebut empat baris baku
 * <code>Seni</code>, <code>Olah Raga</code>, <code>Kejuaraan Ilmiah</code>, dan
 * <code>Lain-Lain</code>. Jangan tertukar dengan entity saudaranya
 * {@link ais.database.model.sekolah.KategoriPrestasiSiswa} yang — meski bernama "kategori" — justru
 * mencatat <i>tingkat</i> kejuaraan (Internasional/Nasional/Regional/Kab-Kota/Kecamatan/
 * Kampus-Sekolah/Lain-Lain). Satu baris {@link ais.database.model.sekolah.PrestasiSiswa} memakai
 * KEDUA katalog itu lewat dua FK terpisah dan menjawab dua pertanyaan berbeda: "di bidang apa"
 * (kelas ini) dan "seberapa tinggi tingkatnya" (kelas kategori).</p>
 *
 * <p><b>Asal-usul angka pada {@code kode}.</b> Nilai 2/3/1/9 bukan angka acak dan bukan pula nomor
 * urut: itu <code>id_jenis_prestasi</code> milik PDDikti/Neo Feeder. Jejaknya masih utuh di kembaran
 * perguruan tinggi {@link ais.database.model.CabangPrestasiMahasiswa} — kelas itu memakai pemetaan
 * yang identik <i>persis</i>, ditambah kolom <code>feeder</code> yang benar-benar dikirim sebagai
 * <code>id_jenis_prestasi</code> oleh {@code ais.action.master.feeder.util.FeederExporter} dan
 * diselaraskan dua arah oleh {@code CabangPrestasiMahasiswaAction}. Versi sekolah ini adalah hasil
 * <i>porting</i> dari modul PT (kembar lainnya: {@link ais.database.model.sekolah.CabangPrestasiGuru},
 * {@code CabangPrestasiDosen}, {@code CabangPrestasiPegawai}) TANPA membawa kolom <code>feeder</code>,
 * dan modul sekolah tidak punya integrasi Feeder sama sekali. Konsekuensinya: di modul sekolah
 * {@code kode} praktis hanya kolom tampilan — satu {@code Label} di grid master, satu kotak isian di
 * formulir master, dan satu kolom pada ekspor/unggah massal — tidak pernah dipakai untuk keputusan
 * bisnis, perhitungan, pelaporan, maupun pertukaran data.</p>
 *
 * <p><b>Struktur.</b> Entity ini sangat ramping — hanya {@code id}, {@code kode}, {@code nama},
 * {@code keterangan}, ditambah jejak audit warisan ({@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}). Tidak ada koleksi balik ke {@link ais.database.model.sekolah.PrestasiSiswa}
 * (relasi satu arah dari sisi transaksi), tidak ada FK induk/anak, tidak ada kolom {@code aktif}
 * maupun {@code nomorUrut}, dan — ini penting — <b>tidak ada kolom {@code sekolah} maupun
 * {@code yayasan}</b>. Tabel ini memang katalog GLOBAL satu instalasi, dipakai bersama oleh seluruh
 * sekolah dan yayasan. Karena itu ketiadaan filter tenant pada
 * {@code CabangPrestasiSiswaAction.initCriteria(boolean)} bukan kebocoran: memang tidak ada dimensi
 * tenant yang bisa disaring. Efek sampingnya bersifat fungsional, bukan keamanan: validasi
 * {@code checkNamaCabangPrestasiSiswa()} menuntut nama unik SECARA GLOBAL, sehingga dua sekolah
 * dalam satu instalasi tidak bisa punya baris "Seni" masing-masing.</p>
 *
 * <p><b>Pengelompokan anggota.</b></p>
 * <ul>
 * <li><i>Identitas &amp; kunci</i> — {@link #getId()}/{@link #setId(Long)}, {@link #serialVersionUID}.</li>
 * <li><i>Jejak audit warisan</i> — {@link #getOleh()}/{@link #setOleh(String)},
 *     {@link #getOlehId()}/{@link #setOlehId(String)},
 *     {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback {@link #onUpdate()}.</li>
 * <li><i>Muatan bisnis</i> — {@link #getKode()}/{@link #setKode(String)},
 *     {@link #getNama()}/{@link #setNama(String)}, {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * <li><i>Infrastruktur</i> — konstruktor {@link #CabangPrestasiSiswa()} dan {@link #toString()}.</li>
 * </ul>
 *
 * <p><b>Hal non-obvious yang WAJIB diketahui sebelum menyunting kelas ini.</b></p>
 * <ol>
 * <li><b>{@link #getKode()} adalah getter DESTRUKTIF (menulis balik ke field).</b> Getter ini bukan
 *     pembaca murni: bila {@code nama} cocok salah satu dari empat label baku, ia MENIMPA field
 *     {@code kode}. Karena pemetaan Hibernate kelas ini berbasis <i>property access</i> (anotasi
 *     {@code @Id} berada di getter) dan {@code getKode()} tidak diberi {@code @Transient},
 *     nilai hasil timpaan itu ikut ditulis ke kolom <code>kode</code> pada flush berikutnya.
 *     Pola yang sama persis ada di {@link ais.database.model.sekolah.KategoriPrestasiSiswa} dan di
 *     seluruh kembaran PT/guru/pegawai. Rinciannya di Javadoc method tersebut.</li>
 * <li><b>{@code nama} adalah kolom yang benar-benar dipakai seluruh sistem, termasuk yang DICETAK di
 *     rapor.</b> {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} memasukkan
 *     {@code getCabangPrestasiSiswa().getNama()} ke parameter laporan bernama {@code "jenis"} —
 *     jadi nama cabanglah yang muncul pada lembar rapor siswa, bukan nama kategori/tingkat.
 *     Dasbor dan rekap pun mengelompokkan berdasarkan nama. Mengganti nama satu baris otomatis
 *     mengganti label seluruh riwayat prestasi yang menunjuk baris itu.</li>
 * <li><b>Baris baku disemai otomatis dari layar TRANSAKSI, bukan dari layar master.</b> Penyemaian
 *     ada di {@code PrestasiSiswaAction.init()} dan berjalan sebagai efek samping saat layar Prestasi
 *     Siswa dibuka. Ada dua blok terpisah dengan penjaga berbeda: blok pertama ("Seni", "Olah Raga",
 *     "Kejuaraan Ilmiah") hanya jalan bila tabel BENAR-BENAR KOSONG, blok kedua ("Lain-Lain") jalan
 *     bila baris bernama "Lain-Lain" belum ada. Akibatnya, sekali seseorang menghapus "Seni" pada
 *     instalasi yang sudah berisi baris lain, baris itu TIDAK akan pernah dipulihkan otomatis.</li>
 * <li><b>Kewajiban isi ada di formulir, bukan di skema.</b> FK
 *     {@code PrestasiSiswa.cabang_prestasi_siswa} bersifat {@code nullable = true}, tetapi formulir
 *     transaksi memaksa "Cabang Kejuaraan harus diisi". Baris lama, impor massal, dan penulis lain
 *     tetap bisa menghasilkan prestasi tanpa cabang — karena itu setiap pembaca hilir
 *     ({@code DasbordPrestasi}, {@code LaporanRaporSiswa}) memakai penjaga null sendiri.</li>
 * <li><b>Warisan {@link ais.database.model.GeneralValueObject} bukan {@code @MappedSuperclass}.</b>
 *     Kelas induk adalah POJO abstrak biasa sehingga Hibernate TIDAK memetakan propertinya. Karena
 *     itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sengaja
 *     DIDEKLARASIKAN ULANG di sini — ini keharusan teknis pemetaan, bukan duplikasi yang bisa
 *     dibersihkan. Jangan menghapusnya.</li>
 * <li><b>{@code @Audited} (Hibernate Envers) aktif.</b> Setiap penulisan — termasuk penulisan tak
 *     sengaja akibat butir 1 — menghasilkan satu revisi baru di tabel audit. Grid master memang
 *     menampilkan riwayat itu lewat {@code RevisiHelper.createNewRevisi(...)}.</li>
 * <li><b>Komentar generator "Bank generated by hbm2java" pada versi lama adalah salah salin.</b>
 *     Kelas ini tidak ada hubungannya dengan entity Bank; string yang sama tersalin ke belasan berkas
 *     lain di repositori (sumber aslinya {@code JenisGuru}). Komentar itu digantikan Javadoc ini.</li>
 * </ol>
 *
 * <p><b>Siapa yang menulis baris ini.</b> Satu-satunya layar CRUD adalah
 * {@code ais.action.master.sekolah.CabangPrestasiSiswaAction} (berkas layar
 * <code>/pages/master/sekolah/cabang_prestasi_siswa.zul</code>), ditambah jalur unggah massal
 * {@code Common.uploadData(...)} pada layar yang sama, dan penyemaian otomatis
 * {@code PrestasiSiswaAction.init()} yang dijelaskan di atas. Berkas
 * <code>/WEB-INF/new/sekolah/uiux/cabang_prestasi_siswa.jsp</code> beserta
 * <code>services/cabang_prestasi_siswa_service.jsp</code> hanyalah scaffold metadata UI baru — tidak
 * mengakses data sama sekali.</p>
 *
 * <p><b>Catatan hak akses (pewarisan hak lewat menu induk).</b> Layar master ini sendiri bergerbang
 * benar: {@code doBeforeCompose()} memanggil {@code Common.doCheckSecurity()}, tombol Tambah diuji
 * {@code CommonPrivilages.CREATE}, tombol Ubah/Hapus diuji {@code UPDATE}/{@code DELETE}, dan tombol
 * unggah massal baru muncul bila ketiganya terpenuhi. Namun layar yang sama juga disisipkan sebagai
 * TAB di dalam layar transaksi Prestasi Siswa
 * ({@code ais.action.master.sekolah.PrestasiSiswaAction.onCabangPrestasiSiswa(...)} menyisipkan
 * <code>cabang_prestasi_siswa.zul</code> lewat {@code MyInclude}). Saat diakses lewat jalur itu,
 * {@code checkPrevilages()} mengevaluasi hak menu INDUK (Prestasi Siswa), bukan hak menu master
 * Cabang Prestasi Siswa — pola "pewarisan hak lewat menu induk" yang sudah berulang di modul lain,
 * dan di sini bahkan berpasangan dengan tab kembarnya untuk
 * {@link ais.database.model.sekolah.KategoriPrestasiSiswa}. Satu-satunya penyaring pada jalur tab
 * adalah visibilitas tab itu sendiri, yang hanya disembunyikan bagi akun siswa dan guru.</p>
 *
 * <p><b>Catatan lingkup data pada pemakainya.</b> Kelas ini hanya katalog label dan tidak memuat data
 * pribadi, tetapi transaksi yang memakainya berada di alur yang perlu diwaspadai: filter "hanya anak
 * saya" pada {@code PrestasiSiswaAction.initCriteria(boolean)} bersifat <i>fail-open</i> (hanya
 * dipasang bila daftar anak tidak kosong), gerbang tombol ubah/hapus pada layar transaksi itu
 * dikomentari total, dan rekap lintas sekolah pada
 * {@code ais.action.master.dashboard.helper.DashboardRekapPrestasiSiswa} (dipakai lewat
 * {@code DashboardRekapPrestasiSiswaBerdasarCabang}) tidak menyaring tenant kecuali pengguna sendiri
 * memilih yayasan/sekolah di combobox. Lihat catatan pada
 * {@link ais.database.model.sekolah.PrestasiSiswa} bila menelusuri hal itu.</p>
 *
 * <p><b>Catatan khusus rekap per cabang.</b> {@code DashboardRekapPrestasiSiswa} merangkai
 * <i>native SQL</i> dan menyisipkan {@link #getNama()} setiap baris katalog ini apa adanya ke dalam
 * alias kolom berkutip ganda (<code>... end) as "&lt;nama&gt; Laki-laki"</code>). Nama cabang tidak
 * pernah di-escape dan layar master tidak melarang karakter apa pun, sehingga nama yang mengandung
 * tanda kutip ganda akan memutus pernyataan SQL yang dijalankan pengguna LAIN saat membuka tab rekap.
 * Jangan menambah pemakaian nama katalog ke dalam SQL yang dirangkai string; bila menyentuh helper
 * itu, pindahkan ke alias tetap ({@code kolom_1}, {@code kolom_2}, ...) dengan label diatur di lapis
 * tampilan.</p>
 *
 * <p><b>Bug yang sudah diketahui pada pemakai (bukan pada kelas ini).</b>
 * <code>/WEB-INF/baru/modul/prestasi/catatan.jsp</code> menembak
 * <code>public.cabang_prestasi_siswa</code> dan <code>public.prestasi_siswa</code> padahal tabel
 * sekolah berada di skema <code>sekolah</code> — salah salin dari cabang mahasiswa pada berkas yang
 * sama. Berkas dasbor tetangganya (<code>_dashboard_prestasi_siswa.jsp</code>) memakai skema yang
 * benar, jadi perbaikannya sepele; dicatat di sini agar tidak dicari-cari sebagai masalah pemetaan
 * entity.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.PrestasiSiswa
 * @see ais.database.model.sekolah.KategoriPrestasiSiswa
 * @see ais.database.model.CabangPrestasiMahasiswa
 * @see ais.database.model.sekolah.CabangPrestasiGuru
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "cabang_prestasi_siswa")



public class CabangPrestasiSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya sengaja dipertahankan apa adanya (dan kebetulan sama dengan milik
	 * {@link ais.database.model.sekolah.KategoriPrestasiSiswa} serta
	 * {@link ais.database.model.sekolah.PrestasiSiswa} karena ketiga berkas lahir dari salinan
	 * generator yang sama). Instance entity ikut terserialisasi saat ZK menyimpan state komponen ke
	 * dalam session, sehingga mengubah nilai ini dapat mematahkan session lama yang masih hidup.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama tabel <code>sekolah.cabang_prestasi_siswa</code>.
	 *
	 * <p>Dideklarasikan ulang karena {@link ais.database.model.GeneralValueObject} bukan
	 * {@code @MappedSuperclass}. Dipetakan lewat {@link #getId()}.</p>
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna yang terakhir menyimpan baris ini (jejak audit warisan).
	 *
	 * <p>Diisi otomatis oleh lapisan penyimpanan bersama, bukan oleh formulir master.</p>
	 */
	private String oleh;
	/**
	 * Identitas login (user id) pengguna yang terakhir menyimpan baris ini (jejak audit warisan).
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas login pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return user id penyimpan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas login pengguna penyimpan terakhir.
	 *
	 * <p><b>Perhatikan penjaga di awal method:</b> nilai {@code null} atau string kosong/spasi
	 * DIABAIKAN diam-diam — nilai lama dipertahankan. Ini kontrak keluarga
	 * {@link ais.database.model.GeneralValueObject}: jejak audit tidak boleh terhapus oleh
	 * pemanggil yang kebetulan tidak punya konteks pengguna (misalnya proses batch atau penyemaian
	 * otomatis dari {@code PrestasiSiswaAction.init()}).</p>
	 *
	 * @param olehId user id penyimpan; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama tampilan pengguna penyimpan terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan agar jejak
	 * audit yang sudah ada tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna penyimpan; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui stempel waktu audit tepat sebelum UPDATE dikirim.
	 *
	 * <p>Delegasi ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setTanggal_dirubah(Date)} (dan jejak pengguna bila konteksnya tersedia). Dipanggil oleh
	 * Hibernate, bukan oleh kode aplikasi — jangan memanggilnya manual.</p>
	 *
	 * <p><b>Catatan penting:</b> callback ini juga ikut berjalan pada UPDATE yang lahir dari efek
	 * samping {@link #getKode()} (lihat butir "getter destruktif" pada Javadoc kelas), sehingga
	 * baris dapat tampak "baru dirubah" padahal tidak ada seorang pun yang menyuntingnya.</p>
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field {@code tanggal_dirubah} beserta
	 * nilai awalnya ({@code ais.ui.util.WaktuUtil.getDate()}) — bentuk padat ini dipertahankan apa
	 * adanya agar diff tetap bersih; jangan dipecah tanpa alasan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan
	 * oleh formulir. Tidak ada penjaga null di sini (berbeda dari {@link #setOleh(String)}), jadi
	 * pemanggil yang mengirim {@code null} akan benar-benar mengosongkan kolom.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Nilai awalnya diisi saat objek dibuat sehingga baris
	 * baru pun sudah membawa waktu, bukan {@code null}.</p>
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas dalam bentuk <code>id-nama</code>.
	 *
	 * <p>Dipakai antara lain sebagai label bawaan ZK ketika instance ini menjadi nilai
	 * {@code Comboitem} pada combobox "Cabang *" di layar transaksi Prestasi Siswa, dan sebagai teks
	 * pembanding pada beberapa utilitas umum.</p>
	 *
	 * <p><b>Perhatikan:</b> method ini membaca field {@code nama} SECARA LANGSUNG, bukan lewat
	 * {@link #getNama()}, sehingga hasilnya TIDAK di-trim — berbeda dengan nilai yang dilihat
	 * Hibernate dan layar. Untuk baris yang namanya tersimpan dengan spasi di ujung, keduanya bisa
	 * berbeda. Karena {@code nama} adalah field biasa (bukan asosiasi lazy), method ini aman
	 * dipanggil pada objek detached.</p>
	 *
	 * @return gabungan id dan nama, misalnya {@code "2-Olah Raga"}.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode cabang/bidang prestasi.
	 *
	 * <p>Kolom bebas isi dari sisi formulir, TETAPI untuk empat nama baku nilainya ditimpa oleh
	 * {@link #getKode()} setiap kali dibaca. Angka yang dipakai adalah
	 * <code>id_jenis_prestasi</code> PDDikti/Neo Feeder yang diwarisi dari modul perguruan tinggi;
	 * di modul sekolah tidak ada konsumen fungsional selain satu label di grid master, satu kotak
	 * isian di formulir, dan satu kolom pada cetak/unggah massal.</p>
	 */
	private String kode;

	/**
	 * Nama cabang/bidang prestasi — inilah kolom yang benar-benar dipakai seluruh sistem.
	 *
	 * <p>Wajib diisi ({@code nullable = false}), maksimal 255 karakter, dan divalidasi unik secara
	 * GLOBAL oleh layar master. Nilai inilah yang muncul di combobox formulir Prestasi Siswa, di
	 * potongan agregasi {@code DasbordPrestasi} (termasuk radar "Bidang / Cabang Prestasi" dan chip
	 * "Cabang andalan"), di rekap SQL berbasis {@code GROUP BY}, serta — yang paling kasatmata bagi
	 * orang tua — pada <b>lembar rapor siswa</b> melalui parameter {@code "jenis"} di
	 * {@code LaporanRaporSiswa}. Karena pengelompokan laporan memakai NAMA (bukan id), mengubah nama
	 * sebuah baris otomatis mengubah label seluruh riwayat prestasi yang menunjuknya, dan dua baris
	 * berbeda yang kebetulan bernama sama akan menyatu di laporan.</p>
	 */
	private String nama;
	/**
	 * Keterangan bebas untuk cabang prestasi ini.
	 *
	 * <p>Hanya ditampilkan di grid dan formulir layar master; tidak pernah dipakai untuk keputusan
	 * bisnis apa pun.</p>
	 */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen.
	 *
	 * <p>Dibutuhkan Hibernate untuk membentuk instance saat memuat baris, dipakai layar master
	 * ({@code onAdd(...)}) untuk menyiapkan formulir "Tambah Cabang Prestasi Siswa", dan dipakai
	 * penyemaian otomatis {@code PrestasiSiswaAction.init()} untuk membuat empat baris baku. Seluruh
	 * field bisnis dibiarkan {@code null}; hanya {@code tanggal_dirubah} yang punya nilai awal.</p>
	 */
	public CabangPrestasiSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dihasilkan basis data ({@code IDENTITY}) sehingga kolomnya {@code insertable = false}:
	 * nilai yang di-set manual sebelum penyimpanan pertama akan diabaikan pada INSERT. Layar master
	 * memakai {@code getId() == null} sebagai penanda mode "tambah" versus "ubah", dan rekap dasbor
	 * menyisipkan id ini apa adanya ke dalam ekspresi {@code case when} pada SQL-nya.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama.
	 *
	 * <p>Dipakai kerangka kerja (Hibernate, jalur unggah massal, dan pencarian objek terpilih), bukan
	 * oleh formulir. Menetapkan id pada objek yang sudah tersimpan akan membuat Hibernate
	 * memperlakukannya sebagai baris lain.</p>
	 *
	 * @param id kunci utama baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode cabang/bidang prestasi — <b>getter dengan efek samping (write-back), bukan
	 * pembaca murni</b>.
	 *
	 * <p><b>Perilaku.</b> Bila {@code nama} tidak {@code null} dan cocok (tanpa membedakan besar
	 * kecil huruf) dengan salah satu dari empat label baku, field {@code kode} DITIMPA lebih dulu,
	 * baru dikembalikan:</p>
	 * <table summary="Pemetaan nama ke kode">
	 * <tr><td>Seni</td><td>&#8594; "2"</td></tr>
	 * <tr><td>Olah Raga</td><td>&#8594; "3"</td></tr>
	 * <tr><td>Kejuaraan Ilmiah</td><td>&#8594; "1"</td></tr>
	 * <tr><td>Lain-Lain</td><td>&#8594; "9"</td></tr>
	 * </table>
	 * <p>Perhatikan bahwa urutan angkanya TIDAK mengikuti urutan penulisan cabang (Seni mendapat 2,
	 * Olah Raga 3, Kejuaraan Ilmiah 1) — jangan "merapikannya", angka itu kode
	 * <code>id_jenis_prestasi</code> PDDikti, bukan nomor urut. Untuk nama lain (termasuk ejaan yang
	 * sedikit berbeda seperti "Olahraga" tanpa spasi atau "Karya Ilmiah") tidak ada cabang yang cocok
	 * dan nilai simpanan dikembalikan apa adanya — termasuk nilai baku LAMA yang tertinggal bila
	 * sebuah baris pernah bernama baku lalu diganti namanya.</p>
	 *
	 * <p><b>Efek samping yang WAJIB diperhitungkan.</b> Pemetaan Hibernate kelas ini berbasis
	 * <i>property access</i> (anotasi {@code @Id} ada di getter) dan method ini tidak diberi
	 * {@code @Transient}, sehingga ia dipakai Hibernate untuk membaca nilai kolom
	 * <code>kode</code>. Akibatnya:</p>
	 * <ul>
	 * <li>Nilai yang diketik petugas pada kotak "Kode Cabang Prestasi" akan TERBUANG DIAM-DIAM bila
	 *     nama barisnya salah satu dari empat label baku — layar menyimpan nilai ketikan lewat
	 *     {@link #setKode(String)}, tetapi saat flush Hibernate membaca ulang lewat method ini dan
	 *     memperoleh angka bakunya. Kotak isian itu praktis hanya berguna untuk nama non-baku.</li>
	 * <li>Sekadar MEMBUKA daftar master sudah cukup untuk memicu penulisan: perender baris
	 *     ({@code CabangPrestasiSiswaRenderer.render(...)}) memanggil {@code getKode()} untuk membuat
	 *     label, objeknya masih terikat session, dan pemeriksaan kotor Hibernate melihat field
	 *     berubah. Karena kelas ini {@code @Audited}, setiap kejadian seperti itu juga melahirkan
	 *     revisi Envers baru tanpa ada yang menyunting apa pun.</li>
	 * <li>Penyemaian otomatis di {@code PrestasiSiswaAction.init()} bergantung pada perilaku ini:
	 *     tiga baris pertama disimpan HANYA dengan {@code setNama(...)} tanpa kode sama sekali, dan
	 *     kolom {@code kode}-nya baru terisi karena write-back di sini. Hanya baris "Lain-Lain" yang
	 *     kodenya di-set eksplisit — dan itu pun redundan.</li>
	 * </ul>
	 *
	 * <p><b>Mengapa dampaknya tetap kecil hari ini.</b> Di modul sekolah tidak ada satu pun pembaca
	 * fungsional {@code kode} selain label grid, isian formulir master, dan kolom cetak/unggah:
	 * laporan, dasbor, dan rekap semuanya mengelompokkan berdasarkan {@code nama}. Nilai yang
	 * tertimpa pun selalu sama dengan nilai kanoniknya, jadi tidak ada data yang benar-benar hilang
	 * selama empat nama baku itu dipertahankan. Yang tersisa adalah kebisingan audit dan kotak isian
	 * yang menyesatkan. Perilaku ini berbeda di modul perguruan tinggi, tempat
	 * {@link ais.database.model.CabangPrestasiMahasiswa} memakai kolom terpisah {@code feeder} untuk
	 * pertukaran data resmi.</p>
	 *
	 * @return kode cabang prestasi; kanonik untuk empat nama baku, nilai tersimpan untuk nama lain,
	 *         atau {@code null} bila belum pernah diisi dan namanya bukan salah satu label baku.
	 */
	public String getKode() {
		if (nama != null) {
			if (nama.equalsIgnoreCase("Seni")) {
				kode = "2";
			} else if (nama.equalsIgnoreCase("Olah Raga")) {
				kode = "3";
			} else if (nama.equalsIgnoreCase("Kejuaraan Ilmiah")) {
				kode = "1";
			} else if (nama.equalsIgnoreCase("Lain-Lain")) {
				kode = "9";
			}
		}
		return kode;
	}

	/**
	 * Menetapkan kode cabang/bidang prestasi.
	 *
	 * <p>Setter polos tanpa validasi. Dipanggil dari {@code onSave(...)} layar master dengan nilai
	 * kotak isian yang sudah di-trim, dari jalur unggah massal, dan sekali dari penyemaian otomatis
	 * ({@code setKode("9")} untuk baris "Lain-Lain").</p>
	 *
	 * <p><b>Perhatikan:</b> nilai yang ditetapkan di sini tidak dijamin bertahan — lihat penjelasan
	 * write-back pada {@link #getKode()}. Untuk baris bernama salah satu dari empat label baku,
	 * apa pun yang di-set di sini akan tergantikan sebelum tersimpan.</p>
	 *
	 * @param kode kode baru; boleh {@code null} atau kosong.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama cabang/bidang prestasi, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Kolom wajib isi, panjang maksimal 255 karakter. Pemangkasan dilakukan saat BACA, sementara
	 * {@link #setNama(String)} menyimpan apa adanya — asimetri yang berarti nilai di basis data bisa
	 * saja masih membawa spasi. Konsekuensi praktisnya: pemeriksaan duplikat pada layar master
	 * memakai perbandingan persis {@code Restrictions.eq("nama", ...)} terhadap nilai ketikan yang
	 * sudah di-trim, sehingga baris lama yang tersimpan dengan spasi di ujung tidak akan terdeteksi
	 * sebagai duplikat.</p>
	 *
	 * <p><b>Hati-hati bila memakai nilai ini untuk merangkai SQL.</b> Nilai kembalian tidak
	 * di-escape dan layar master tidak melarang karakter apa pun; {@code DashboardRekapPrestasiSiswa}
	 * sudah terlanjur menyisipkannya ke dalam alias kolom berkutip ganda (lihat catatan pada Javadoc
	 * kelas). Pemanggil baru wajib memakai parameter terikat atau alias tetap.</p>
	 *
	 * @return nama cabang prestasi yang sudah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama cabang/bidang prestasi.
	 *
	 * <p>Setter polos: nilai disimpan apa adanya, TANPA trim (pemangkasan baru terjadi di
	 * {@link #getNama()}) dan tanpa pemeriksaan panjang maupun keunikan. Kewajiban isi dan keunikan
	 * global ditegakkan di layar master, bukan di sini — jalur penulisan lain (unggah massal,
	 * penyemaian otomatis, kode pemanggil baru) tidak otomatis mewarisi validasi tersebut.</p>
	 *
	 * <p><b>Dampak lanjutan:</b> mengganti nama sebuah baris otomatis mengganti label seluruh
	 * riwayat prestasi yang menunjuk baris ini — termasuk yang tercetak di rapor siswa — karena
	 * laporan dan rekap mengelompokkan berdasarkan nama. Mengganti nama menjadi/dari salah satu
	 * empat label baku juga mengubah nilai {@link #getKode()} yang tersimpan pada penyimpanan
	 * berikutnya; mengganti nama KELUAR dari label baku tidak mengembalikan {@code kode} ke
	 * {@code null}, nilai baku lama tertinggal apa adanya.</p>
	 *
	 * @param nama nama cabang prestasi baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * <p>Boleh {@code null} ({@code nullable = true}) dan dikembalikan apa adanya — tidak di-trim,
	 * tidak dinormalkan menjadi string kosong. Pemanggil yang merangkainya ke dalam teks harus
	 * menyiapkan penjaga null sendiri; perender grid master memang menyerahkannya langsung ke
	 * {@code Label} sehingga kolom Keterangan tampil kosong untuk baris yang tidak mengisinya.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas.
	 *
	 * <p>Setter polos tanpa validasi maupun penjaga null. Dipanggil dari {@code onSave(...)} layar
	 * master dan jalur unggah massal.</p>
	 *
	 * @param keterangan keterangan baru; boleh {@code null} atau kosong.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
