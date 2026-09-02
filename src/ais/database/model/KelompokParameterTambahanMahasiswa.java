package ais.database.model;

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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;

/**
 * Master <b>kategori/kelompok</b> untuk "Form Tambahan" (field kustom) data mahasiswa —
 * tabel {@code public.kelompok_parameter_tambahan_mahasiswa}.
 *
 * <p>Entity ini <b>bukan</b> definisi field-nya sendiri, melainkan hanya JUDUL SEKSI tempat
 * field-field kustom dikelompokkan pada formulir biodata mahasiswa. Rantai datanya tiga
 * lapis:</p>
 * <ol>
 *   <li>{@link ParameterTambahan} — definisi field generik (label, tipe inputan, wajib
 *   lampiran, dst.), dipakai bersama oleh banyak modul;</li>
 *   <li>{@link ParameterTambahanMahasiswa} — tabel penghubung yang memasang sebuah
 *   {@code ParameterTambahan} ke modul mahasiswa, lengkap dengan penyaring tahun angkatan;
 *   di sinilah kolom FK {@code kelompokParameterTambahanMahasiswa} berada;</li>
 *   <li><b>kelas ini</b> — kelompok/seksi tempat field-field tadi ditampilkan berurutan.</li>
 * </ol>
 *
 * <p><b>Nilai isian mahasiswa TIDAK disimpan di sini maupun di tabel anak.</b> Hasil
 * pengisian formulir ditulis sebagai teks gabungan multi-baris ke dua kolom {@code text}
 * milik {@link BiodataMahasiswa} ({@code parameterTambahan} versi berlabel dan
 * {@code parameterTambahanInds} versi ber-ID) lewat
 * {@link BiodataMahasiswa#populateParameterTambahan(java.util.List)}. Id kelompok ini muncul
 * di sana sebagai bagian kiri kunci {@code "<idKelompok>-&gt;<idParameter>"}, kunci yang sama
 * yang dipakai untuk menautkan berkas {@link LampiranLain} per field. Akibatnya menghapus
 * atau mengganti id baris kelompok akan memutus tautan lampiran dan membuat kolom teks lama
 * tidak lagi bisa dipetakan balik.</p>
 *
 * <p><b>Auto-seed baris default.</b> {@link #checkCreateDefault()} memastikan selalu ada satu
 * baris bertanda {@code defaultData = true} bernama {@code "Form Tambahan"}. Satu-satunya
 * pemanggil nyata di codebase adalah
 * {@code ais.action.master.ParameterTambahanMahasiswaAction.doAfterCompose(Component)} —
 * layar "Parameter Tambahan Mahasiswa" — yang memanggilnya sebelum mengisi combobox kelompok,
 * supaya combobox itu tidak pernah kosong pada instalasi baru. Layar pemeliharaan kelompok
 * sendiri ({@code KelompokParameterTambahanMahasiswaAction}) TIDAK memanggilnya.</p>
 *
 * <p><b>Konsumen utama</b> (semua memakai pola query yang sama: {@code createCriteria} pada
 * {@link ParameterTambahanMahasiswa} + {@code createAlias} + {@code groupProperty} sehingga
 * yang keluar adalah daftar kelompok yang benar-benar punya field aktif):</p>
 * <ul>
 *   <li>{@code ais.action.master.helper.ParameterTambahanMahasiswaListener} — merender seksi
 *   formulir biodata mahasiswa, satu {@link #getNama()} per judul seksi;</li>
 *   <li>{@code ais.action.master.dashboard.helper.DashboardRekapParameterTambahanMahasiswa} —
 *   penyaring rekap dasbor;</li>
 *   <li>{@code ais.action.master.ParameterTambahanMahasiswaAction} — combobox pemilihan
 *   kelompok saat mendefinisikan field;</li>
 *   <li>{@code ais.action.master.KelompokParameterTambahanMahasiswaAction} — layar CRUD
 *   kelompok itu sendiri;</li>
 *   <li>{@code ais.common.InitData} — kelas ini terdaftar pada salah satu panggilan
 *   {@code initClasses(...)}, jadi seluruh barisnya ikut di-preload ke cache memori saat
 *   bootstrap aplikasi.</li>
 * </ul>
 *
 * <p><b>Filter {@code aktif} hanya ditegakkan di sisi pembaca.</b> Baik listener biodata
 * maupun dasbor menambahkan {@code Restrictions.eq("aktif", true)}. Kolomnya sendiri tidak
 * pernah diisi oleh {@code onSave()} layar kelompok (layar itu hanya menulis {@code nama} dan
 * {@code keterangan}); nilainya baru terisi karena {@link #getAktif()} menambal {@code null}
 * menjadi {@code true} saat Hibernate membacanya — lihat catatan pada getter tersebut.</p>
 *
 * <p><b>Susunan anggota:</b> (a) jejak audit manual {@code oleh}/{@code olehId}/
 * {@code tanggal_dirubah} beserta {@link #onUpdate()}; (b) kolom bisnis {@code nama},
 * {@code keterangan}, {@code defaultData}, {@code aktif}, {@code nomorUrut}; (c) satu utility
 * statis {@link #checkCreateDefault()}; (d) {@link #compareTo(GeneralValueObject)} sebagai
 * satu-satunya logika non-trivial selain (c).</p>
 *
 * <p><b>Catatan pemetaan yang mudah disalahpahami.</b> {@link GeneralValueObject} BUKAN
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * tidak memetakan satu pun property induknya. Karena itu deklarasi ULANG {@code id},
 * {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, {@code nama}, {@code keterangan}, dan
 * {@code nomorUrut} di kelas ini <b>bukan bug</b>, melainkan keharusan teknis agar kolom-kolom
 * tersebut benar-benar tersimpan. Efek sampingnya: field kelas ini <i>membayangi</i>
 * (<i>shadow</i>) field bernama sama di induk, dan property induk yang TIDAK dideklarasikan
 * ulang di sini — terutama {@code nim} dan {@code kode} — selamanya bernilai {@code null}
 * untuk entity ini.</p>
 *
 * <p><b>Akses property, bukan field.</b> {@code @Id} dipasang pada {@link #getId()} sehingga
 * Hibernate memakai getter untuk membaca seluruh property. Digabung dengan
 * {@code dynamicUpdate = true}, getter penambal default ({@link #getDefaultData()},
 * {@link #getAktif()}, {@link #getNomorUrut()}) dapat memicu {@code UPDATE} beserta revisi
 * Envers baru pada baris yang sekadar terbaca. Property tanpa {@code @Column} eksplisit
 * ({@code defaultData}, {@code aktif}, {@code nomorUrut}) memakai nama kolom apa adanya sesuai
 * {@code ais.database.hibernate.MyNamingStrategy} (turunan {@code DefaultNamingStrategy}).</p>
 *
 * <p><b>Kembaran dekat.</b> Kelas ini identik kata-per-kata dengan
 * {@link KelompokParameterTambahanAlumni} kecuali nama tabel/tipe dan satu kolom tambahan
 * ({@code digunakanUntukPenggunaAlumni}) yang hanya dimiliki versi alumni; keduanya bahkan
 * berbagi nilai {@code serialVersionUID} yang sama persis. Anggota keluarga lain:
 * {@code KelompokParameterTambahanCalonMahasiswa}, {@code KelompokParameterTambahanPertemuan},
 * {@code KelompokParameterTambahanCalonSiswa}, {@code ais.database.model.lkp
 * .KelompokParameterTambahanKegiatan}, dan seterusnya.</p>
 *
 * @see ParameterTambahanMahasiswa
 * @see ParameterTambahan
 * @see BiodataMahasiswa#populateParameterTambahan(java.util.List)
 * @see KelompokParameterTambahanAlumni
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_mahasiswa")

public class KelompokParameterTambahanMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya identik dengan milik {@link KelompokParameterTambahanAlumni}
	 * — sisa penggandaan berkas, tidak berdampak karena kedua tipe tidak pernah saling
	 * dideserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code id}, IDENTITY; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi interceptor audit, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah tersentuh interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah, dengan <b>penjagaan nilai kosong</b>: bila argumen
	 * {@code null} atau hanya berisi spasi, method langsung kembali dan nilai lama
	 * <b>dipertahankan</b>. Jadi jejak audit tidak pernah bisa dikosongkan lewat setter ini.
	 *
	 * @param olehId id pengguna baru; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah, dengan penjagaan nilai kosong yang sama seperti
	 * {@link #setOlehId(String)}: {@code null}/spasi diabaikan dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna baru; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyerahkan pembaruan stempel audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(Object)} tepat sebelum
	 * Hibernate mengeksekusi {@code UPDATE}. Implementasi wajib dari method {@code abstract}
	 * satu-satunya milik {@link GeneralValueObject}; tidak pernah dipanggil kode aplikasi.
	 *
	 * <p>Perhatikan bahwa hanya {@code @PreUpdate} yang dipasang — pada {@code INSERT} pertama
	 * stempel {@code oleh} tidak diisi lewat jalur ini.</p>
	 *
	 * <p>Pada baris yang sama juga dideklarasikan field {@code tanggal_dirubah}, yang
	 * diinisialisasi ke waktu server saat object dibuat ({@code WaktuUtil.getDate()}) sehingga
	 * baris baru selalu punya nilai walau interceptor belum pernah berjalan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; biasanya dipanggil interceptor audit,
	 * bukan kode layar.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan; tidak pernah {@code null} untuk object yang baru dibuat
	 *         di JVM karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks {@code "<id>-<nama>"}, dipakai antara lain sebagai label item combobox
	 * kelompok pada layar parameter tambahan.
	 *
	 * <p>Berbeda dengan {@link GeneralValueObject#toString()} yang memakai {@code kode}, dan
	 * berbeda pula dengan {@link #getNama()}: nilai {@code nama} dibaca <b>langsung dari
	 * field</b> sehingga <b>tidak</b> di-{@code trim()}. Untuk baris yang belum tersimpan
	 * hasilnya diawali {@code "null-"}.</p>
	 *
	 * @return gabungan id dan nama, dipisahkan tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kelompok; judul seksi pada formulir. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas kelompok. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda baris default hasil auto-seed {@link #checkCreateDefault()}. */
	private Boolean defaultData;
	/** Penanda kelompok masih dipakai; disaring pembaca formulir/dasbor. */
	private Boolean aktif;
	/** Nomor urut tampil seksi; satu-satunya kunci urut antar sesama kelompok. */
	private Integer nomorUrut;

	/**
	 * Memastikan tabel ini selalu punya satu kelompok default, dan mengembalikannya.
	 *
	 * <p>Alur: mencari baris pertama dengan {@code defaultData = true} pada session native yang
	 * sedang aktif. Bila tidak ada, sebuah baris baru dibuat dengan {@code defaultData = true},
	 * {@code nama} dan {@code keterangan} sama-sama {@code "Form Tambahan"}, lalu disimpan di
	 * dalam transaksi tersendiri ({@code begin()} … {@code commit()}).</p>
	 *
	 * <p><b>Efek samping yang harus disadari pemanggil:</b></p>
	 * <ul>
	 *   <li><b>Menutup session Hibernate</b> lewat {@code HibernateUtil.closeSession()} —
	 *   SELALU, bukan hanya ketika baris baru dibuat. Object yang dikembalikan karenanya sudah
	 *   <i>detached</i>, dan kode setelahnya akan mendapat session baru saat memanggil
	 *   {@code currentSession()}. Karena itu pemanggilan yang ada diletakkan paling awal pada
	 *   {@code doAfterCompose()} layar, sebelum query lain dijalankan.</li>
	 *   <li>Baris hasil seed adalah baris yang tombol hapusnya disembunyikan layar CRUD kelompok
	 *   (lihat {@link #getDefaultData()}), jadi ia praktis permanen.</li>
	 *   <li>{@code aktif} dan {@code nomorUrut} tidak diisi eksplisit; nilainya bergantung pada
	 *   penambalan getter saat Hibernate menulis baris ({@code true} dan {@code 1}).</li>
	 *   <li>Tidak ada penguncian: dua request bersamaan pada instalasi kosong berpotensi
	 *   membuat dua baris default. Kueri pencarian memakai {@code setMaxResults(1)} sehingga
	 *   duplikat semacam itu tidak menimbulkan error, hanya baris yatim.</li>
	 * </ul>
	 *
	 * <p>Pemanggil nyata satu-satunya:
	 * {@code ais.action.master.ParameterTambahanMahasiswaAction.doAfterCompose(Component)}.</p>
	 *
	 * @return baris kelompok default (yang sudah ada maupun yang baru saja dibuat), dalam
	 *         keadaan <i>detached</i> karena session sudah ditutup
	 */
	public static KelompokParameterTambahanMahasiswa checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanMahasiswa kelompokParameterTambahanMahasiswa = (KelompokParameterTambahanMahasiswa) session
				.createCriteria(KelompokParameterTambahanMahasiswa.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanMahasiswa == null) {
			kelompokParameterTambahanMahasiswa = new KelompokParameterTambahanMahasiswa();
			kelompokParameterTambahanMahasiswa.setDefaultData(true);
			kelompokParameterTambahanMahasiswa.setNama("Form Tambahan");
			kelompokParameterTambahanMahasiswa.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanMahasiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanMahasiswa;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate. Seluruh field dibiarkan {@code null}
	 * kecuali {@code tanggal_dirubah} yang terisi waktu server.
	 */
	public KelompokParameterTambahanMahasiswa() {
	}

	/**
	 * Mengembalikan primary key baris ini. Nilainya ikut menyusun kunci
	 * {@code "<idKelompok>-&gt;<idParameter>"} yang dipakai {@link BiodataMahasiswa} untuk
	 * menyimpan isian dan menautkan {@link LampiranLain}.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya diisi Hibernate.
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kelompok, sudah di-{@code trim()}.
	 *
	 * <p>Pemangkasan dilakukan <b>saat baca saja</b> — nilai di field/DB tetap apa adanya, jadi
	 * pencarian {@code Restrictions.eq("nama", ...)} pada
	 * {@code KelompokParameterTambahanMahasiswaAction.checkNamaKelompokParameterTambahanMahasiswa()}
	 * membandingkan nilai mentah, bukan hasil {@code trim()} ini.</p>
	 *
	 * @return nama kelompok tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kelompok. Tanpa validasi maupun pemangkasan spasi; keunikan nama hanya
	 * dijaga di layar CRUD, bukan oleh constraint database.
	 *
	 * @param nama nama kelompok baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan kelompok <b>apa adanya</b>.
	 *
	 * <p><b>Membalik kontrak kelas induk.</b> {@link GeneralValueObject#getKeterangan()}
	 * menjanjikan tidak pernah {@code null} (mengembalikan {@code ""}); override ini
	 * mengembalikan nilai field mentah sehingga {@code null} MUNGKIN. Konsekuensinya cabang
	 * {@code keterangan} pada {@link #compareTo(GeneralValueObject)} di kelas ini bisa gagal
	 * memenuhi syarat non-null, dan pemanggil yang mengandalkan janji induk (mis. merangkai
	 * string tanpa pemeriksaan) berisiko NPE.</p>
	 *
	 * @return keterangan kelompok, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan kelompok. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "baris default" — {@code true} hanya untuk baris hasil auto-seed
	 * {@link #checkCreateDefault()}. Layar CRUD kelompok memakainya untuk MENYEMBUNYIKAN tombol
	 * hapus agar kelompok default tidak bisa dibuang.
	 *
	 * <p><b>Getter menulis balik.</b> Bila field masih {@code null}, method menyimpan
	 * {@code false} ke field sebelum mengembalikannya. Karena entity ini memakai akses property
	 * + {@code dynamicUpdate}, pembacaan pada baris lama bernilai {@code NULL} dapat berubah
	 * menjadi {@code UPDATE} nyata (plus satu revisi Envers) walau tidak ada niat mengedit.
	 * Sifatnya <i>self-healing</i>: nilai yang ditulis sama dengan nilai yang dianggap benar,
	 * dan hanya terjadi sekali per baris.</p>
	 *
	 * @return {@code true} bila ini kelompok default; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda baris default. Tanpa validasi; hanya dipanggil
	 * {@link #checkCreateDefault()} — tidak ada layar yang mengeksposnya.
	 *
	 * @param defaultData penanda baru
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan status aktif kelompok, dengan <b>default {@code true}</b> bila belum diisi.
	 *
	 * <p>Kelompok non-aktif disaring keluar dari formulir biodata mahasiswa dan dari rekap
	 * dasbor ({@code Restrictions.eq("aktif", true)}), sehingga seluruh field kustom di
	 * bawahnya ikut hilang dari layar meski definisinya masih ada.</p>
	 *
	 * <p><b>Getter menulis balik</b>, dengan mekanisme dan konsekuensi yang sama seperti
	 * {@link #getDefaultData()}: {@code null} ditambal menjadi {@code true} di field, yang pada
	 * akses property dapat memicu {@code UPDATE}. Justru penambalan inilah satu-satunya jalan
	 * kolom {@code aktif} terisi untuk baris baru, karena {@code onSave()} layar CRUD kelompok
	 * hanya menulis {@code nama} dan {@code keterangan}. Baris yang terlanjur {@code NULL} di
	 * database tetap tak terlihat oleh kedua query pembaca sampai baris itu kebetulan dimuat
	 * dan tersimpan ulang.</p>
	 *
	 * @return {@code true} bila kelompok masih dipakai; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif kelompok. Tanpa validasi. Dipanggil dari checkbox "Aktif" di grid
	 * layar CRUD kelompok, yang langsung menyimpan perubahannya.
	 *
	 * @param aktif status baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil seksi, dengan <b>default {@code 1}</b> bila belum diisi.
	 * Inilah satu-satunya kunci pengurutan antar sesama kelompok pada
	 * {@link #compareTo(GeneralValueObject)}, jadi nilai ini yang menentukan susunan seksi di
	 * formulir biodata mahasiswa maupun di rekap dasbor.
	 *
	 * <p><b>Getter menulis balik</b>, sama seperti {@link #getAktif()}: {@code null} ditambal
	 * menjadi {@code 1} di field. Karena penambalan sudah dilakukan di dalam blok {@code if},
	 * ternary {@code nomorUrut == null ? 1 : nomorUrut} pada baris {@code return} <b>tidak
	 * pernah</b> mengambil cabang kirinya — sisa penulisan defensif berganda, bukan logika
	 * aktif.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil. Tanpa validasi (nilai negatif maupun duplikat diterima).
	 * Dipanggil dari kotak angka di grid layar CRUD kelompok, yang menyimpan perubahannya
	 * seketika.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Urutan alami kelompok parameter tambahan mahasiswa. Menimpa
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} dengan dua perilaku berbeda:
	 *
	 * <ol>
	 *   <li><b>Sesama {@code KelompokParameterTambahanMahasiswa}</b> — dibandingkan
	 *   MURNI berdasarkan {@link #getNomorUrut()}. Karena getter itu tidak pernah
	 *   mengembalikan {@code null}, cabang ini selalu menghasilkan keputusan; tidak ada
	 *   pemecah imbang, sehingga dua kelompok bernomor sama dianggap setara dan urutan
	 *   relatifnya bergantung pada stabilitas {@code Collections.sort} serta urutan hasil
	 *   query. Inilah urutan yang dipakai
	 *   {@code ParameterTambahanMahasiswaListener} dan
	 *   {@code DashboardRekapParameterTambahanMahasiswa} saat menyusun seksi formulir.</li>
	 *   <li><b>Terhadap tipe lain</b> — jatuh ke rantai kunci gaya induk: {@code nim}, lalu
	 *   {@code nama}, lalu {@code keterangan}. Cabang {@code nim} di sini <b>mati</b>: property
	 *   {@code nim} milik {@link GeneralValueObject} tidak pernah diisi untuk entity ini.
	 *   Cabang {@code keterangan} juga tidak lagi dijamin terpakai seperti pada induk, karena
	 *   {@link #getKeterangan()} boleh mengembalikan {@code null}. Bila tak satu pun kunci
	 *   memenuhi syarat, hasilnya {@code 0}.</li>
	 * </ol>
	 *
	 * <p>Exception apa pun ditelan dan hanya dicatat ke audit error, lalu method mengembalikan
	 * {@code 0}. Perlu diingat {@code compareTo} di sini tidak konsisten dengan
	 * {@code equals}, jadi jangan memakainya sebagai kunci {@code TreeSet}/{@code TreeMap}:
	 * kelompok bernomor urut sama akan saling menghilangkan.</p>
	 *
	 * @param arg0 entity pembanding; boleh bertipe lain
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} bila tidak ada
	 *         kunci pembanding yang tersedia atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof KelompokParameterTambahanMahasiswa) {
			KelompokParameterTambahanMahasiswa s = (KelompokParameterTambahanMahasiswa) arg0;
			return getNomorUrut().compareTo(s.getNomorUrut());
		} else {
			try {
				if (getNim() != null && arg0.getNim() != null) {
					return getNim().compareTo(arg0.getNim());
				} else if (getNama() != null && arg0.getNama() != null) {
					return getNama().compareTo(arg0.getNama());
				} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
					return getKeterangan().compareTo(arg0.getKeterangan());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KelompokParameterTambahanMahasiswa.java:179");

			}

			return 0;
		}
	}

}
