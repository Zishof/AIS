package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

/**
 * Entity Hibernate yang memetakan tabel
 * {@code sekolah.gelombang_punya_parameter_verifikasi_calon_siswa} pada modul <b>PSB/PPDB</b>
 * (penerimaan siswa baru). Satu baris = <b>satu kategori berkas/persyaratan yang wajib diverifikasi
 * pada satu gelombang pendaftaran</b>.
 *
 * <h2>Domain (terverifikasi dari kode, bukan dari nama kelas)</h2>
 * <p>Nama kelasnya panjang dan menyesatkan: kata "punya parameter" membuatnya terbaca seperti tabel
 * penghubung murni antara gelombang dan parameter. Kenyataannya entity ini adalah <b>master
 * kategori</b> yang berdiri sendiri — ia punya {@code judul}, {@code nama}, dan {@code keterangan}
 * sendiri, dan justru menjadi <b>induk</b> bagi baris transaksi
 * {@link CalonSiswaPunyaVerifikasiParameter}.</p>
 *
 * <p>Rantai lengkapnya terdiri dari empat entity:</p>
 * <ol>
 * <li>{@link GelombangPendaftaranPsb} — gelombang pendaftaran (mis. "PPDB 2026 Gelombang 1"),
 * milik satu {@code Sekolah}/{@code Yayasan}.</li>
 * <li><b>Kelas ini</b> — kategori verifikasi yang berlaku pada gelombang tersebut. {@code judul}
 * dipakai sebagai <i>judul blok</i> pada formulir ("Prestasi Akademik"), sedangkan {@code nama}
 * dipakai sebagai <i>label entri</i>: ia muncul pada teks tombol {@code "Tambah " + getNama()},
 * pada judul kolom sub-grid, pada judul dialog input, dan pada pesan validasi
 * {@code getNama() + " harus diisi"}. Jadi {@code nama} sebaiknya berupa kata benda tunggal
 * ("Sertifikat", "Piagam"), bukan kalimat.</li>
 * <li>{@code ParameterVerifikasiCalonSiswa} — <b>daftar nilai tingkat/gradasi</b> (mis. Sekolah /
 * Kabupaten / Provinsi / Nasional). Entity ini adalah master global lintas gelombang; yang
 * menentukan <i>subset mana</i> yang boleh dipilih untuk sebuah kategori adalah relasi
 * {@code @ManyToMany} milik kelas ini — lihat {@link #getParameterVerifikasiCalonSiswas()}. Pada
 * layar, subset itu dirender sebagai combobox berlabel <b>"Tingkat"</b>.</li>
 * <li>{@link CalonSiswaPunyaVerifikasiParameter} — <b>transaksi</b>: satu baris per entri yang
 * didaftarkan seorang calon siswa di bawah satu kategori, menunjuk balik ke baris kelas ini
 * <i>dan</i> ke satu {@code ParameterVerifikasiCalonSiswa} terpilih, plus status
 * {@code verified} dan berkas bukti. Kardinalitasnya <b>N entri per (calon siswa, kategori)</b>,
 * bukan satu.</li>
 * </ol>
 *
 * <p>Karena kategori diikat ke gelombang, dua gelombang yang butuh kategori sama harus memiliki
 * <b>dua baris terpisah</b> di tabel ini (tidak ada mekanisme penyalinan antar gelombang di UI —
 * setiap gelombang baru harus dikonfigurasi ulang dari nol).</p>
 *
 * <h2>Siapa yang membaca dan menulis</h2>
 * <ul>
 * <li><b>Penulis:</b> hanya {@code ais.action.master.sekolah.psb.
 * GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswaAction} (layar master
 * {@code z/x/y/pages/psb/gelombang_punya_parameter_verifikasi_calon_siswa.zul}). Layar itu
 * mewajibkan {@code judul}, {@code nama}, dan gelombang terisi; ia <b>tidak pernah</b> menulis
 * {@code keterangan} — lihat {@link #getKeterangan()}.</li>
 * <li><b>Pembaca:</b> hanya {@code ais.action.master.sekolah.psb.VerifikasiParameterPSBHelper},
 * yang dipanggil dari layar petugas {@code CalonSiswaAction} dan dari keluarga formulir pendaftaran
 * mandiri {@code PPDB1}…{@code PPDB_Simple8}. Helper memuat seluruh kategori milik satu gelombang
 * lalu membangun satu blok verifikasi per kategori.</li>
 * </ul>
 *
 * <h2>Pola arsitektur repo — hasil pemeriksaan pada file ini</h2>
 * <ul>
 * <li><b>Getter penulis-balik ({@code check()}):</b> <b>ADA, dua tempat</b> —
 * {@link #getGelombangPendaftaranPsb()} menugaskan ulang hasil {@code check(...)} ke fieldnya
 * (pola baku repo, bukan bug), dan {@link #toString()} <i>juga</i> menulis ulang field yang sama
 * lewat pemanggilan getter tersebut. Keduanya <b>tidak destruktif</b>: tidak ada data yang
 * dikosongkan.</li>
 * <li><b>Getter destruktif yang mengosongkan data (pola {@code KelasSiswaPSB.getNama()}):</b>
 * <b>TIDAK ADA</b> — {@link #getNama()} dan {@link #getJudul()} murni mengembalikan field apa
 * adanya.</li>
 * <li><b>Getter yang "membalik kontrak" penyimpanan:</b> <b>ADA, satu tempat, dan arahnya
 * TERBALIK dari instance-instance sebelumnya</b>. Di file lain pola ini berupa getter yang
 * menormalkan {@code null} menjadi nilai baku (dan karenanya memicu {@code UPDATE} palsu). Di sini
 * yang terjadi sebaliknya: {@link #getKeterangan()} meng-<i>override</i>
 * {@link ais.database.model.GeneralValueObject#getKeterangan()} — yang menjamin <b>tidak pernah
 * {@code null}</b> — dengan implementasi polos yang <b>bisa mengembalikan {@code null}</b>. Lihat
 * pembahasan dampaknya di method tersebut.</li>
 * <li><b>Penciutan {@code TreeSet}:</b> <b>ADA dan NYATA</b> pada jalur pembuatan kategori baru —
 * lihat uraian rinci di {@link #getParameterVerifikasiCalonSiswas()}. Ini bug kehilangan data
 * senyap, bukan sekadar kuirk urutan.</li>
 * <li><b>Broken access control pada layar pengelola:</b> <b>ADA</b> — lihat bagian di bawah.</li>
 * <li><b>Filter tenant di dalam entity:</b> <b>TIDAK ADA</b>, dan memang tidak diharapkan ada;
 * penyaringan adalah tanggung jawab pemanggil. Masalahnya pemanggil pun tidak menyaring.</li>
 * <li><b>Tombol mutasi massal tanpa gerbang:</b> <b>TIDAK ADA</b> — semua operasi berlaku satu
 * baris.</li>
 * </ul>
 *
 * <h2>Broken access control — TERVERIFIKASI dari kode layar pengelola</h2>
 * <p>{@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswaAction} hanya memanggil
 * {@code Common.doCheckSecurity()} (cek "sudah login") pada {@code doBeforeCompose()} dan
 * <b>tidak memuat satu pun {@code CommonPrivilages.checkPrevilages(...)}</b>. Flag tombolnya
 * di-hard-code:</p>
 * <pre>{@code
 * private boolean edit = true;
 * private boolean delete = true;
 * }</pre>
 * <p>Kontrasnya tajam dengan layar master entity tetangganya,
 * {@code ais.action.master.sekolah.ParameterVerifikasiCalonSiswaAction}, yang <i>sudah</i>
 * melakukannya dengan benar ({@code add.setVisible(checkPrevilages(CREATE))},
 * {@code edit = checkPrevilages(UPDATE)}, {@code delete = checkPrevilages(DELETE)}). Jadi ini bukan
 * konvensi modul yang berbeda, melainkan gerbang yang <b>hilang</b> di satu layar saja.</p>
 * <p>Ditambah lagi, layar ini <b>tidak menyaring tenant sama sekali</b>: combobox gelombang diisi
 * dengan {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} saja — tanpa klausa
 * {@code sekolah}/{@code yayasan}, padahal {@link GelombangPendaftaranPsb} punya kedua kolom itu —
 * dan {@code onSearchDefault()} pun hanya menyaring judul/nama/gelombang. Gabungannya: siapa pun
 * yang bisa membuka menu ini, dengan hak <b>READ saja</b>, dapat menambah, mengubah, dan
 * <b>menghapus</b> definisi kategori verifikasi berkas milik <b>sekolah/yayasan mana pun</b> di
 * seluruh instalasi.</p>
 * <p>Dampak praktisnya bukan sekadar perusakan konfigurasi: menghapus sebuah kategori akan
 * memutus/menghapus baris {@link CalonSiswaPunyaVerifikasiParameter} yang bergantung padanya —
 * yaitu <b>bukti berkas dan status verifikasi calon siswa</b> yang menentukan lolos/tidaknya
 * seorang anak pada seleksi PSB. Menambah kategori baru pada gelombang yang sedang berjalan
 * seketika memunculkan blok wajib-isi baru di formulir pendaftaran publik.</p>
 * <p>Temuan ini <b>satu keluarga</b> dengan broken access control yang sudah tercatat pada
 * {@link CalonSiswaPunyaVerifikasiParameter} (sisi transaksi, lewat
 * {@code VerifikasiParameterPSBHelper} yang juga nol {@code checkPrevilages}). Dengan file ini,
 * <b>kedua sisi</b> rantai verifikasi PSB — master kategori dan transaksi entri — terkonfirmasi
 * tanpa gerbang privilese. Perubahan tetap terekam Envers ({@code @Audited}) dan beratribusi benar
 * lewat {@link #onUpdate()}, jadi ini cacat <i>otorisasi</i>, bukan cacat auditabilitas.</p>
 *
 * <h2>Kuirk dan bug laten lain</h2>
 * <ul>
 * <li><b>Kolom {@code keterangan} mati total.</b> Tidak ada satu pun layar yang menulisnya
 * ({@code onSave()} hanya mengisi judul, nama, gelombang, dan koleksi parameter) maupun
 * membacanya. Kolomnya hanya hidup lewat {@code @Audited}. Konsekuensi baiknya: pembalikan kontrak
 * {@link #getKeterangan()} praktis tidak berdampak <i>saat ini</i> — tetapi akan langsung berdampak
 * begitu ada yang menambahkan input keterangan ke layar.</li>
 * <li><b>Urutan kategori tidak deterministik.</b> Entity ini tidak punya kolom {@code nomorUrut}
 * maupun {@code aktif}, dan {@code VerifikasiParameterPSBHelper} memuat kategori <b>tanpa</b>
 * {@code addOrder(...)}. Urutan blok verifikasi pada formulir PPDB karena itu ditentukan basis data,
 * bisa berubah antar-kueri, dan tidak dapat diatur admin. Tidak adanya {@code aktif} juga berarti
 * <b>tidak ada cara menonaktifkan kategori</b> selain menghapusnya — dengan konsekuensi merusak
 * data transaksi yang dijelaskan di atas.</li>
 * <li><b>Mutasi saat render pada layar master.</b> {@code PilihanGelombangPendaftaranPsbRenderer}
 * memanggil {@code setGelombangPendaftaranPsb(selectedGelombangPendaftaranPsb)} untuk setiap baris
 * yang gelombangnya {@code null}. Object yang diubah masih <i>attached</i> pada session Hibernate
 * yang memuatnya, sehingga perubahan ini berpotensi ikut ter-{@code flush} — sebuah <b>penulisan
 * data sebagai efek samping merender daftar</b>, lengkap dengan revisi Envers yang tidak berasal
 * dari tindakan pengguna.</li>
 * <li><b>NPE laten pada layar master.</b> Baris berikutnya memanggil
 * {@code getGelombangPendaftaranPsb().getNama()} tanpa penjaga {@code null}. Kolom FK-nya tidak
 * dideklarasikan {@code nullable = false}, jadi satu saja baris yatim (impor/SQL langsung, atau
 * dibuat sebelum validasi diperketat) akan melempar {@code NullPointerException} dan merusak render
 * <b>seluruh</b> grid, bukan hanya baris itu — kecuali layar dibuka dengan parameter
 * {@code ?gelombangPendaftaranPsb=<id>} yang mengaktifkan fallback pada butir sebelumnya.</li>
 * <li><b>Tidak ada anotasi {@code @Column} sama sekali</b> pada {@code nama}, {@code judul}, dan
 * {@code keterangan}. Ketiganya memakai pemetaan baku (kolom senama, {@code varchar(255)},
 * {@code nullable}), berbeda dari kerabatnya {@code ParameterVerifikasiCalonSiswa} yang
 * menyatakan {@code nullable = false} pada {@code nama}. Akibatnya kewajiban isi {@code judul}
 * dan {@code nama} <b>hanya ditegakkan di lapisan UI</b>; jalur non-UI mana pun bisa menyimpan
 * baris kosong.</li>
 * <li><b>{@code serialVersionUID} sisa salin-tempel.</b> Nilainya
 * {@code 1463822577548439808L}, hanya berbeda dua digit dari {@code 2463821577548439808L} milik
 * {@code ParameterVerifikasiCalonSiswa} dan {@link CalonSiswaPunyaVerifikasiParameter}. Tidak
 * berdampak fungsional (kesetaraan diperiksa per kelas), tapi menegaskan ketiganya berasal dari
 * satu templat.</li>
 * <li><b>Cascade tanpa {@code REMOVE}.</b> Baik {@code @ManyToOne} ke gelombang maupun
 * {@code @ManyToMany} ke parameter hanya memakai {@code PERSIST} dan {@code MERGE}. Ini benar:
 * menghapus sebuah kategori tidak ikut menghapus gelombang maupun master tingkat — hanya baris
 * tabel penghubungnya.</li>
 * </ul>
 *
 * <h2>Catatan pewarisan</h2>
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa. Hibernate karena
 * itu <b>tidak</b> memetakan properti milik induk. Deklarasi ulang {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi keliru</b>, melainkan
 * keharusan teknis agar keempat kolom itu benar-benar terpetakan. Hal yang sama berlaku untuk
 * {@code nama} dan {@code keterangan}: field di kelas ini <i>membayangi</i> field senama milik
 * induk, dan karena Hibernate memakai <i>property access</i> (anotasi {@code @Id} ada di getter),
 * yang terpetakan adalah field lokal lewat getter/setter yang di-override di sini. Field milik
 * induk tetap {@code null} selamanya — yang penting diingat saat membaca
 * {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)}.</p>
 * <p>Yang tetap diwarisi dan benar-benar dipakai adalah utilitas runtime
 * {@code check(...)} pada {@link #getGelombangPendaftaranPsb()} serta implementasi
 * {@code Comparable}, {@code equals}, dan {@code clone} milik induk.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas &amp; representasi</b> —
 * {@link #GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa()},
 * {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 * <li><b>Relasi</b> — {@link #getGelombangPendaftaranPsb()} beserta setternya (induk gelombang),
 * dan {@link #getParameterVerifikasiCalonSiswas()} beserta setternya (subset "Tingkat" yang
 * boleh dipilih).</li>
 * <li><b>Isi kategori</b> — {@link #getJudul()}/{@link #setJudul(String)},
 * {@link #getNama()}/{@link #setNama(String)},
 * {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * </ol>
 *
 * @see ais.database.model.GeneralValueObject
 * @see CalonSiswaPunyaVerifikasiParameter
 * @see GelombangPendaftaranPsb
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "gelombang_punya_parameter_verifikasi_calon_siswa")



public class GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Nilainya hampir identik dengan milik {@code ParameterVerifikasiCalonSiswa} dan
	 * {@link CalonSiswaPunyaVerifikasiParameter} ({@code 2463821577548439808L}) — sisa salin-tempel
	 * saat ketiga entity PSB diturunkan dari satu templat. Tidak berdampak fungsional karena
	 * kesetaraan {@code serialVersionUID} hanya diperiksa per kelas.</p>
	 */
	private static final long serialVersionUID = 1463822577548439808L;

	/** Kunci utama {@code IDENTITY}; lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna pengubah terakhir (jejak audit); lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna pengubah terakhir (jejak audit); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila baris belum pernah di-{@code UPDATE}
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Nilai {@code null} atau kosong diabaikan diam-diam</b> (method langsung
	 * {@code return}), sehingga jejak audit lama tidak pernah terhapus oleh pemanggil yang lalai.
	 * Umumnya dipanggil {@code AuditTimestampInterceptor} dari {@link #onUpdate()}, bukan oleh kode
	 * layar.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila baris belum pernah di-{@code UPDATE}
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum pernyataan {@code UPDATE}
	 * baris ini dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang
	 * mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna aktif. Ketiga kolom jejak audit karena
	 * itu <b>tidak perlu</b> diisi manual oleh layar/Action.</p>
	 *
	 * <p><b>Hanya berlaku untuk UPDATE.</b> Tidak ada pasangan {@code @PrePersist}, sehingga pada
	 * INSERT pertama {@code oleh}/{@code olehId} tetap {@code null} dan {@code tanggal_dirubah}
	 * mengandalkan nilai awal field di bawah. Praktisnya: <b>kategori verifikasi yang baru dibuat
	 * tidak beratribusi pembuat</b> sampai ada perubahan berikutnya — relevan karena, sebagaimana
	 * dijelaskan pada Javadoc kelas, layar pengelolanya tidak memeriksa privilese sama sekali.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris yang sama dengan hook ini
	 * (gaya sisipan generator di seluruh entity AIS); nilai awalnya diambil dari
	 * {@code ais.ui.util.WaktuUtil.getDate()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor} dari {@link #onUpdate()}, bukan oleh
	 * kode layar. Tidak ada validasi: {@code null} diterima apa adanya dan akan menulis {@code NULL}
	 * ke kolom.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Field-nya diinisialisasi saat object dibuat dengan {@code ais.ui.util.WaktuUtil.getDate()}
	 * — waktu server yang sudah dinormalkan aplikasi, bukan {@code new Date()} mentah. Artinya
	 * baris yang belum pernah di-{@code UPDATE} pun tetap punya stempel waktu, yakni waktu
	 * instance-nya <i>dibuat di JVM</i> (bukan waktu commit).</p>
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null} untuk object yang dibuat
	 *         lewat konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<gelombang>_<nama>"}.
	 *
	 * <p>Menimpa {@link ais.database.model.GeneralValueObject#toString()} (yang berbentuk
	 * {@code "<id>-<nama>"}), sehingga hasilnya menyertakan konteks gelombang. Bagian tengah adalah
	 * hasil {@code toString()} milik {@link GelombangPendaftaranPsb}, bukan sekadar namanya.</p>
	 *
	 * <p><b>Efek samping yang perlu disadari:</b> baris pertamanya memanggil
	 * {@link #getGelombangPendaftaranPsb()} lalu <b>menugaskan hasilnya kembali ke field</b>. Karena
	 * getter itu melewati {@code check(...)}, memanggil {@code toString()} dapat:</p>
	 * <ul>
	 * <li>memicu inisialisasi relasi {@code LAZY} (satu {@code SELECT} tambahan), bahkan</li>
	 * <li><b>membuka session Hibernate baru</b> bila object sudah <i>detached</i> (tahap 3
	 * {@code check()}), dan</li>
	 * <li>mengganti isi field dengan instance kanonik yang berbeda dari sebelumnya.</li>
	 * </ul>
	 * <p>Jadi {@code toString()} di sini <b>bukan operasi murni</b>: jangan menaruhnya di jalur
	 * logging yang panas atau di dalam loop besar. Perhatikan pula bahwa bagian {@code nama} dibaca
	 * <b>langsung dari field</b>, bukan lewat {@link #getNama()} — kebetulan setara karena
	 * {@code getNama()} di sini tidak menormalkan apa pun.</p>
	 *
	 * @return teks {@code "<id>-<gelombang>_<nama>"}; setiap bagian bisa berbunyi {@code "null"}
	 *         bila belum terisi
	 */
	public String toString() {
		gelombangPendaftaranPsb = getGelombangPendaftaranPsb();
		return id + "-" + gelombangPendaftaranPsb + "_" + nama;
	}

	/** Gelombang pendaftaran PSB pemilik kategori ini; lihat {@link #getGelombangPendaftaranPsb()}. */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;

	/**
	 * Label entri kategori — dipakai sebagai teks tombol "Tambah &lt;nama&gt;", judul kolom
	 * sub-grid, dan judul dialog input. Lihat {@link #getNama()}.
	 *
	 * <p>Membayangi field senama milik {@link ais.database.model.GeneralValueObject}; yang
	 * terpetakan Hibernate adalah field ini lewat getter/setter yang di-override di bawah.</p>
	 */
	private String nama;

	/** Judul blok verifikasi pada formulir; lihat {@link #getJudul()}. */
	private String judul;

	/**
	 * Keterangan bebas kategori. <b>Tidak pernah diisi maupun dibaca layar mana pun</b> — lihat
	 * {@link #getKeterangan()}. Membayangi field senama milik
	 * {@link ais.database.model.GeneralValueObject}.
	 */
	private String keterangan;

	/**
	 * Subset nilai "Tingkat" yang boleh dipilih untuk kategori ini; lihat
	 * {@link #getParameterVerifikasiCalonSiswas()}.
	 *
	 * <p>Diinisialisasi sebagai {@link TreeSet} — sumber bug penciutan yang diuraikan pada getter.
	 * {@code TreeSet} ini hanya bertahan selama object masih <i>transient</i>; begitu entity dimuat
	 * dari basis data, Hibernate menggantinya dengan koleksi persistennya sendiri yang bersemantik
	 * hash, bukan terurut.</p>
	 */
	private Set<ParameterVerifikasiCalonSiswa> parameterVerifikasiCalonSiswas = new TreeSet<ParameterVerifikasiCalonSiswa>();

	/**
	 * Mengembalikan himpunan nilai <b>"Tingkat"</b> ({@code ParameterVerifikasiCalonSiswa}) yang
	 * boleh dipilih calon siswa saat menambahkan entri di bawah kategori ini.
	 *
	 * <p>Dipetakan {@code @ManyToMany} lewat tabel penghubung
	 * {@code sekolah.gelombang_verifikasi_calon_siswa_punya_parameter} dengan kolom
	 * {@code gelombang} → baris kelas ini dan {@code parameter} → {@code ParameterVerifikasiCalonSiswa}.
	 * Perhatikan bahwa nama kolom {@code gelombang} <b>menyesatkan</b>: isinya bukan id
	 * {@link GelombangPendaftaranPsb}, melainkan id <i>kategori</i> (id baris kelas ini). Jangan
	 * menulis kueri SQL langsung dengan asumsi sebaliknya.</p>
	 *
	 * <p>Relasi ini <b>tidak punya sisi invers</b> ({@code ParameterVerifikasiCalonSiswa} tidak
	 * menyimpan referensi balik), dan {@code fetch} tidak dinyatakan sehingga berlaku baku JPA
	 * untuk {@code @ManyToMany}, yaitu <b>{@code LAZY}</b>. Karena itu kedua pemanggilnya
	 * memanggil {@code HibernateUtil.currentSession().refresh(...)} lebih dulu sebelum membaca
	 * koleksi ini. Cascade dibatasi {@code PERSIST} + {@code MERGE} — menyimpan kategori tidak
	 * pernah menghapus master tingkat.</p>
	 *
	 * <p><b>Cara pemakaian di layar:</b></p>
	 * <ul>
	 * <li>Layar master merender <i>seluruh</i> {@code ParameterVerifikasiCalonSiswa} yang aktif
	 * sebagai checkbox, dicentang bila {@code contains(...)} pada himpunan ini benar.</li>
	 * <li>{@code VerifikasiParameterPSBHelper} menyalin himpunan ini ke {@code ArrayList},
	 * mengurutkannya dengan {@code Collections.sort(...)}, lalu mengisinya ke combobox "Tingkat"
	 * pada dialog "Tambah". Kategori yang himpunannya <b>kosong</b> menghasilkan combobox kosong,
	 * dan karena "Tingkat" divalidasi wajib, entri baru <b>tidak akan pernah bisa disimpan</b>
	 * untuk kategori seperti itu.</li>
	 * </ul>
	 *
	 * <p><b>BUG — penciutan {@code TreeSet} pada kategori baru (kehilangan data senyap).</b>
	 * Field ini diinisialisasi {@code new TreeSet<>()}, sehingga untuk object yang masih
	 * <i>transient</i> (tombol "Tambah" pada layar master), {@code add()} memakai
	 * {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)}. Kunci pertama
	 * {@code compareTo} adalah {@code nomorUrut}, dan
	 * {@code ParameterVerifikasiCalonSiswa.getNomorUrut()} <b>tidak pernah mengembalikan
	 * {@code null}</b> (mengembalikan {@code 1} sebagai baku). Akibatnya perbandingan
	 * <b>selalu</b> berhenti di kunci pertama, dan dua tingkat yang {@code nomorUrut}-nya sama —
	 * kondisi bawaan bila admin tidak pernah mengisi kolom itu di layar
	 * {@code ParameterVerifikasiCalonSiswaAction} — dianggap <b>duplikat</b> oleh {@code TreeSet}.
	 * Skenario nyatanya:</p>
	 * <ol>
	 * <li>Admin membuat kategori baru, mencentang "Sekolah", "Kabupaten", "Provinsi", "Nasional"
	 * (semuanya {@code nomorUrut} baku).</li>
	 * <li>{@code TreeSet.add()} menerima yang pertama dan <b>menolak diam-diam</b> tiga sisanya;
	 * checkbox tetap tampak tercentang karena tidak ada render ulang.</li>
	 * <li>Kategori tersimpan hanya dengan <b>satu</b> tingkat.</li>
	 * <li>Saat dibuka ulang untuk diedit, koleksi sudah menjadi koleksi persisten Hibernate
	 * (semantik hash, bukan terurut), sehingga {@code contains()} bekerja benar dan hanya satu
	 * checkbox tampak tercentang — mencentang ulang lalu menyimpan kali ini berhasil.</li>
	 * </ol>
	 * <p>Jadi gejalanya "penyimpanan pertama kehilangan pilihan, penyimpanan kedua baik-baik saja"
	 * — sangat mudah disalahartikan sebagai kesalahan pengguna. Perbaikan paling aman adalah
	 * mengganti inisialisasi menjadi {@code LinkedHashSet}/{@code HashSet}; mengisi
	 * {@code nomorUrut} yang unik untuk setiap tingkat hanya menyamarkan gejalanya.</p>
	 *
	 * @return himpunan tingkat yang tertaut; tidak pernah {@code null}, boleh kosong
	 * @see #setParameterVerifikasiCalonSiswas(Set)
	 */
	@ManyToMany(targetEntity = ParameterVerifikasiCalonSiswa.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST })
	@JoinTable(name = "gelombang_verifikasi_calon_siswa_punya_parameter", schema = "sekolah", joinColumns = @JoinColumn(name = "gelombang"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<ParameterVerifikasiCalonSiswa> getParameterVerifikasiCalonSiswas() {
		return parameterVerifikasiCalonSiswas;
	}

	/**
	 * Mengganti seluruh himpunan tingkat yang tertaut pada kategori ini.
	 *
	 * <p>Tanpa validasi maupun penyalinan defensif: referensi koleksi yang diberikan dipakai apa
	 * adanya. Layar master memanfaatkan hal itu — ia mengambil koleksi lewat
	 * {@link #getParameterVerifikasiCalonSiswas()}, memutasinya langsung dari listener checkbox,
	 * lalu memanggil method ini dengan <b>referensi yang sama</b> sebelum menyimpan. Pola itu aman
	 * untuk koleksi persisten Hibernate (tidak menukar instance {@code PersistentSet}), tetapi
	 * mewariskan perilaku {@code TreeSet} untuk entity baru — lihat peringatan penciutan pada
	 * getter.</p>
	 *
	 * <p>Memberi {@code null} akan membuat getter mengembalikan {@code null} dan meruntuhkan
	 * pemanggil yang melakukan iterasi tanpa penjaga; jangan lakukan.</p>
	 *
	 * @param parameterVerifikasiCalonSiswas himpunan tingkat baru
	 */
	public void setParameterVerifikasiCalonSiswas(Set<ParameterVerifikasiCalonSiswa> parameterVerifikasiCalonSiswas) {
		this.parameterVerifikasiCalonSiswas = parameterVerifikasiCalonSiswas;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Menghasilkan object dengan seluruh kolom {@code null} kecuali {@code tanggal_dirubah}
	 * (diisi waktu pembuatan instance) dan {@link #getParameterVerifikasiCalonSiswas()} (himpunan
	 * kosong). Dipakai layar master lewat {@code onAdd()} untuk membuka formulir tambah — dan
	 * itulah jalur yang terkena bug penciutan {@code TreeSet}.</p>
	 */
	public GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dihasilkan basis data ({@code IDENTITY}, kolom {@code insertable = false}), sehingga
	 * bernilai {@code null} sampai object benar-benar tersimpan. Layar master memakai kondisi
	 * {@code getId() == null} untuk membedakan judul dialog "Tambah Parameter Verifikasi" dari
	 * "Ubah Parameter Verifikasi", dan {@link CalonSiswaPunyaVerifikasiParameter} menyimpan nilai
	 * ini sebagai FK.</p>
	 *
	 * <p>Karena id berurutan dan dapat ditebak, jangan menjadikannya rahasia; pengendalian akses
	 * harus dilakukan lewat privilese — yang justru absen pada layar pengelola, lihat Javadoc
	 * kelas.</p>
	 *
	 * @return kunci utama; {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama.
	 *
	 * <p>Disediakan untuk Hibernate; <b>jangan dipanggil kode aplikasi</b> — mengubah id object
	 * yang sudah persisten akan mengacaukan identity map session.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan gelombang pendaftaran PSB tempat kategori ini berlaku.
	 *
	 * <p>Dipetakan {@code @ManyToOne} {@code LAZY} ke kolom {@code gelombang_pendaftaran_psb}
	 * dengan cascade {@code PERSIST} + {@code MERGE}. Sesuai pola baku entity AIS, hasil
	 * {@code check(...)} <b>ditugaskan kembali ke field</b> sebelum dikembalikan: object hasil bisa
	 * merupakan instance lain (kanonik dari {@code EntityIdentityMap}, dari cache, atau hasil
	 * muat-ulang lewat session baru bila object sudah <i>detached</i>). Penulisan balik ini
	 * <b>tidak destruktif</b> — tidak ada data yang dikosongkan.</p>
	 *
	 * <p><b>Bisa mengembalikan {@code null}</b>: kolomnya tidak dideklarasikan
	 * {@code nullable = false}, dan {@code check(null)} meneruskan {@code null} apa adanya.
	 * Kewajiban isi hanya ditegakkan di formulir. Layar master men-dereferensi hasil method ini
	 * tanpa penjaga {@code null} saat merender grid — lihat catatan NPE laten pada Javadoc
	 * kelas.</p>
	 *
	 * <p>Relasi inilah yang menjadikan konfigurasi verifikasi bersifat <b>per gelombang</b>:
	 * {@code VerifikasiParameterPSBHelper} memuat kategori dengan
	 * {@code Restrictions.eq("gelombangPendaftaranPsb", gel)}. Karena
	 * {@link GelombangPendaftaranPsb} yang menyimpan {@code sekolah} dan {@code yayasan}, relasi
	 * ini juga satu-satunya jalur pengikat kategori ke tenant — jalur yang <b>tidak dipakai</b>
	 * oleh layar pengelola untuk menyaring.</p>
	 *
	 * @return gelombang pendaftaran induk, sudah teresolusi dari proxy; boleh {@code null}
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_psb")
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menautkan kategori ini ke sebuah gelombang pendaftaran PSB.
	 *
	 * <p>Tanpa validasi. Dipanggil dari dua tempat: {@code onSave()} pada layar master (nilai dari
	 * combobox atau dari gelombang yang dikunci lewat parameter URL), dan — perlu diwaspadai —
	 * dari <b>renderer baris grid</b>, yang mengisi gelombang untuk baris yang masih {@code null}
	 * saat layar dibuka dengan parameter {@code ?gelombangPendaftaranPsb=<id>}. Pemanggilan kedua
	 * itu terjadi pada object yang masih <i>attached</i>, sehingga sekadar menampilkan daftar
	 * berpotensi menulis perubahan ke basis data beserta revisi Envers-nya.</p>
	 *
	 * @param gelombangPendaftaranPsb gelombang induk; {@code null} diterima tanpa keluhan
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Mengembalikan <b>label entri</b> kategori ini.
	 *
	 * <p>Meng-override {@link ais.database.model.GeneralValueObject#getNama()} agar Hibernate
	 * memetakan field lokal (lihat catatan pewarisan pada Javadoc kelas). Tidak ada normalisasi:
	 * nilai dikembalikan apa adanya, termasuk {@code null} — berbeda dari
	 * {@code ParameterVerifikasiCalonSiswa.getNama()} yang mem-{@code trim}.</p>
	 *
	 * <p>Nilai ini muncul di banyak tempat pada formulir PPDB: teks tombol
	 * {@code "Tambah " + getNama()}, judul kolom sub-grid, judul dialog
	 * {@code "Tambah data " + getNama()}, dan pesan validasi {@code getNama() + " harus diisi"}.
	 * Karena itu isilah dengan kata benda tunggal ("Sertifikat"), bukan kalimat — teks panjang akan
	 * merusak tata letak keempat tempat tersebut sekaligus. Untuk judul blok, pakai
	 * {@link #getJudul()}.</p>
	 *
	 * @return label entri kategori; boleh {@code null} bila baris dibuat di luar jalur UI
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Mengisi label entri kategori.
	 *
	 * <p>Tanpa validasi. Layar master sudah menolak nilai kosong dan mengirimkan hasil
	 * {@code trim()}, tetapi kolomnya sendiri tidak dideklarasikan {@code nullable = false},
	 * sehingga jalur non-UI tetap bisa menyimpan {@code null}.</p>
	 *
	 * @param nama label entri baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan <b>judul blok verifikasi</b> kategori ini.
	 *
	 * <p>Dirender {@code VerifikasiParameterPSBHelper} sebagai teks tebal di atas sub-grid entri
	 * (mis. "Prestasi Akademik"), dan ditampilkan sebagai kolom tersendiri pada grid layar master.
	 * Berbeda peran dari {@link #getNama()}, yang menjadi label per-entri.</p>
	 *
	 * <p><b>Kuirk:</b> baris judul tersebut dibuat dengan {@code setVisible(false)} pada helper,
	 * sehingga pada formulir PPDB judul ini <b>tidak benar-benar tampil</b> — praktisnya nilai
	 * {@code judul} hanya terlihat di layar master. Ia tetap wajib diisi oleh validasi
	 * {@code onSave()}.</p>
	 *
	 * @return judul blok verifikasi; boleh {@code null} bila baris dibuat di luar jalur UI
	 */
	public String getJudul() {
		return judul;
	}

	/**
	 * Mengisi judul blok verifikasi.
	 *
	 * <p>Tanpa validasi; kewajiban isi hanya ditegakkan {@code onSave()} pada layar master.</p>
	 *
	 * @param judul judul blok baru
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Mengembalikan keterangan bebas kategori ini.
	 *
	 * <p><b>Kolom mati.</b> Tidak ada satu pun layar yang mengisinya — {@code onSave()} pada layar
	 * master hanya menulis {@code judul}, {@code nama}, gelombang, dan koleksi parameter — dan
	 * tidak ada satu pun yang membacanya. Nilainya praktis selalu {@code null}. Kolomnya tetap ada
	 * di basis data dan tetap ikut direkam Envers.</p>
	 *
	 * <p><b>Pembalikan kontrak induk.</b> Method ini meng-override
	 * {@link ais.database.model.GeneralValueObject#getKeterangan()}, yang secara eksplisit
	 * menjamin <b>tidak pernah mengembalikan {@code null}</b> (mengubah {@code null} menjadi
	 * {@code ""}). Override di sini mengembalikan field mentah, jadi <b>bisa {@code null}</b> —
	 * arah pembalikan yang berlawanan dengan instance-instance pola serupa di entity lain, yang
	 * biasanya justru menambahkan normalisasi. Dua konsekuensi:</p>
	 * <ul>
	 * <li>Kode yang mengandalkan jaminan induk (mis. langsung memanggil {@code .trim()} atau
	 * {@code .isEmpty()} pada hasil {@code getKeterangan()} sebuah {@code GeneralValueObject})
	 * akan melempar {@code NullPointerException} bila kebetulan menerima instance kelas ini.</li>
	 * <li>Cabang keempat {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)}
	 * — yang menurut Javadoc induk "selalu terpakai" karena keterangan dijamin non-null — di sini
	 * <b>tidak lagi dijamin</b>. Untuk entity ini pengurutan praktis jatuh ke kunci {@code nama}
	 * (kunci {@code nomorUrut} dan {@code nim} milik induk tidak pernah diisi), dan bila
	 * {@code nama} pun {@code null} maka {@code compareTo} mengembalikan {@code 0} — dua baris
	 * berbeda akan dianggap setara oleh {@code TreeSet}/{@code TreeMap} mana pun. Saat ini tidak
	 * ada pemanggil yang menaruh entity ini dalam koleksi terurut, jadi dampaknya laten.</li>
	 * </ul>
	 * <p>Bila kelak keterangan ditambahkan ke layar, pertimbangkan menormalkan hasilnya agar
	 * kontraknya kembali sejalan dengan induk.</p>
	 *
	 * @return keterangan kategori; <b>boleh {@code null}</b>, berbeda dari kontrak induk
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas kategori.
	 *
	 * <p>Tanpa validasi. Tidak pernah dipanggil kode layar mana pun saat ini — lihat
	 * {@link #getKeterangan()}.</p>
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
