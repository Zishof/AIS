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
 * Entity master <b>kategori/kelompok "Form Tambahan" untuk Calon Mahasiswa (PMB)</b>, dipetakan ke
 * tabel {@code public.kelompok_parameter_tambahan_calon_mahasiswa}.
 *
 * <p>Baris di tabel ini <b>bukan</b> field kustom itu sendiri, melainkan <i>judul seksi</i> yang
 * mengelompokkan field-field kustom pada formulir biodata calon mahasiswa. Rantai datanya:</p>
 *
 * <pre>
 *   ParameterTambahan          (definisi 1 field kustom: label, tipe, pilihan, aktif)
 *        &uarr; dipakai oleh
 *   ParameterTambahanPaket     (penempatan field pada paket/gelombang tertentu)
 *        &rarr; FK kelompok_parameter_tambahan_calon_mahasiswa
 *   KelompokParameterTambahanCalonMahasiswa   &lt;&mdash; KELAS INI (judul seksi)
 * </pre>
 *
 * <p>Jadi relasi kepemilikan berada di sisi lain: {@code ParameterTambahanPaket} yang memegang
 * {@code @ManyToOne} ke kelas ini lewat kolom {@code kelompok_parameter_tambahan_calon_mahasiswa}
 * (lihat {@code ParameterTambahanPaket#getKelompokParameterTambahanCalonMahasiswa()}). Kelas ini
 * sendiri <b>tidak punya satu pun properti relasi</b> &mdash; nol {@code @ManyToOne}, nol
 * {@code @OneToMany}, nol koleksi. Semua properti bertipe skalar.</p>
 *
 * <p>Relasi kedua bersifat {@code @ManyToMany} dan juga dimiliki pihak lawan:
 * {@code GelombangPendaftaran#getKelompokParameterTambahanCalonMahasiswas()} lewat tabel
 * penghubung {@code gelombang_kelompok_parameter}. Relasi itu memungkinkan satu gelombang
 * pendaftaran memilih daftar seksi form tambahan secara eksplisit (lihat bagian "Dua jalur
 * pemilihan" di bawah).</p>
 *
 * <h3>Peran di layar PMB</h3>
 * <ul>
 *   <li><b>Layar admin "Manajemen Kelompok"</b> &mdash;
 *       {@code ais.action.master.KelompokParameterTambahanCalonMahasiswaAction} +
 *       {@code /pages/master/kelompok_parameter_tambahan_calon_mahasiswa.zul}, tampil sebagai
 *       salah satu tab di dalam layar "Parameter Tambahan Paket"
 *       ({@code ParameterTambahanPaketAction#onManajemenKelompok(Event)}).</li>
 *   <li><b>Formulir calon mahasiswa</b> &mdash;
 *       {@code ais.action.master.pmb.ParameterTambahanListener} merender seksi + field-nya, baik
 *       pada form pendaftaran publik maupun pada form biodata setelah calon login.</li>
 *   <li><b>Dasbor rekap</b> &mdash; {@code DashboardRekapParameterTambahanMahasiswa} dan
 *       {@code DashboardRekapParameterTambahanMahasiswaBaru} memakai kelompok sebagai sumbu
 *       pengelompokan rekap jawaban.</li>
 *   <li><b>Layar lain</b> &mdash; {@code KelompokCalonMahasiswaAction},
 *       {@code GelombangPendaftaranAction} (checkbox pemilihan per gelombang),
 *       {@code TbmuserAction}, {@code TampilanPengumumanAkademisAction}, dan
 *       {@code BiodataCalonMahasiswa#populateParameterTambahan(...)}.</li>
 * </ul>
 *
 * <h3>Auto-seed baris default ({@link #checkCreateDefault()})</h3>
 * <p>Kelas ini memakai pola "kelompok default otomatis" yang sama dengan seluruh keluarga
 * {@code KelompokParameterTambahan*} di codebase (Alumni, Mahasiswa, CatatanPegawai,
 * CatatanAdministrasi, Pengaduan, Pengajuan, CalonSiswa, dan seterusnya): method statis
 * {@link #checkCreateDefault()} memastikan SELALU ada satu baris ber-{@code defaultData = true},
 * dan membuatnya bila belum ada. Satu-satunya pemanggil di codebase adalah
 * {@code ais.action.master.ParameterTambahanPaketAction#doAfterCompose(Component)}, yang langsung
 * menyusulkan SQL mentah untuk mengadopsi setiap baris {@code parameter_tambahan_paket} yatim
 * (kolom FK masih {@code NULL}) ke kelompok default tersebut. Detail dan konsekuensinya
 * didokumentasikan di {@link #checkCreateDefault()}.</p>
 *
 * <h3>Dua jalur pemilihan seksi pada formulir (non-obvious)</h3>
 * <p>{@code ParameterTambahanListener} memutuskan seksi mana yang dirender lewat DUA jalur yang
 * saling eksklusif:</p>
 * <ol>
 *   <li><b>Gelombang punya daftar spesifik</b> (koleksi {@code @ManyToMany} gelombang tidak
 *       kosong) &rarr; dipakai HANYA daftar itu, dan flag
 *       {@link #getTampilDiFormPendaftaran()}/{@link #getTampilDiFormSetelahLogin()}
 *       <b>diabaikan sama sekali</b> (asumsinya admin sudah memilih eksplisit).</li>
 *   <li><b>Gelombang tidak memilih apa pun</b> &rarr; seksi dikumpulkan lewat {@code Criteria} atas
 *       {@code ParameterTambahanPaket} dengan syarat {@code parameterTambahan.aktif = true} DAN
 *       {@code kelompokParameterTambahanCalonMahasiswa.aktif = true}, lalu barulah kedua flag
 *       {@code tampilDiForm*} menyaring hasilnya.</li>
 * </ol>
 * <p>Hasil kedua jalur diurutkan dengan {@code Collections.sort(...)}, yaitu memakai
 * {@link #compareTo(GeneralValueObject)} kelas ini (murni {@code nomorUrut}).</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, callback
 *       {@link #onUpdate()}.</li>
 *   <li><b>Identitas &amp; label</b> &mdash; {@link #getId()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Konfigurasi tampil</b> &mdash; {@link #getAktif()}, {@link #getNomorUrut()},
 *       {@link #getTampilDiFormPendaftaran()}, {@link #getTampilDiFormSetelahLogin()}.</li>
 *   <li><b>Penanda baris sistem</b> &mdash; {@link #getDefaultData()}.</li>
 *   <li><b>Utilitas statis</b> &mdash; {@link #checkCreateDefault()}.</li>
 *   <li><b>Kontrak nilai</b> &mdash; {@link #compareTo(GeneralValueObject)}.</li>
 * </ul>
 *
 * <h3>Catatan pemetaan yang mudah salah paham</h3>
 * <ul>
 *   <li><b>Field induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} BUKAN
 *       {@code @Entity} maupun {@code @MappedSuperclass} &mdash; hanya POJO abstrak biasa &mdash;
 *       sehingga Hibernate tidak memetakan properti milik induk. Deklarasi ulang {@code id},
 *       {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, {@code nama}, {@code keterangan},
 *       dan {@code nomorUrut} di kelas ini <b>bukan bug</b>, melainkan keharusan teknis agar
 *       kolom-kolom itu benar-benar terpetakan. Efek sampingnya: field induk yang senama
 *       ter-<i>shadow</i> dan selamanya bernilai {@code null}; semua kode wajib lewat getter
 *       (yang di-override di sini), bukan membaca field induk langsung.</li>
 *   <li><b>Property access.</b> Anotasi JPA dipasang pada getter ({@code @Id} di
 *       {@link #getId()}), jadi Hibernate memakai <i>property access</i>: setiap getter dipanggil
 *       Hibernate saat memuat, dirty-check, dan flush. Getter yang mengubah field karena itu ikut
 *       mengubah baris di database &mdash; lihat {@link #getDefaultData()}, {@link #getAktif()},
 *       {@link #getNomorUrut()}.</li>
 *   <li><b>Nama kolom implisit.</b> Hanya {@code id}, {@code nama}, dan {@code keterangan} yang
 *       punya {@code @Column} eksplisit. Properti {@code defaultData}, {@code aktif},
 *       {@code nomorUrut}, {@code tampilDiFormPendaftaran}, dan {@code tampilDiFormSetelahLogin}
 *       memakai penamaan default Hibernate (nama properti apa adanya, yang di PostgreSQL menjadi
 *       identifier huruf kecil: {@code defaultdata}, {@code nomorurut},
 *       {@code tampildiformpendaftaran}, {@code tampildiformsetelahlogin}).</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate} aktif</b> ({@code @org.hibernate.annotations.Entity}),
 *       sehingga hanya kolom yang benar-benar berubah yang ikut ditulis &mdash; inilah yang membuat
 *       write-back diam-diam dari getter di atas benar-benar sampai ke tabel.</li>
 *   <li><b>{@code @Audited}</b>: setiap perubahan direkam Hibernate Envers ke tabel revisi. Artinya
 *       write-back tak sengaja dari getter juga melahirkan <i>revisi audit palsu</i> yang seolah
 *       dilakukan pengguna.</li>
 *   <li><b>Komentar generator menyesatkan.</b> Javadoc bawaan pada kelas ini semula berbunyi
 *       "Bank generated by hbm2java" &mdash; sisa salin-tempel {@code hbm2java} yang tidak ada
 *       kaitannya dengan bank; sudah diganti oleh dokumentasi ini.</li>
 *   <li><b>{@code serialVersionUID} kembar.</b> Nilainya identik dengan milik
 *       {@code KelompokParameterTambahanAlumni} (dan beberapa saudara lain) karena file-file ini
 *       lahir dari salinan yang sama. Tidak berbahaya (kelasnya berbeda), tapi menjelaskan
 *       kemiripan struktur antar berkas.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see ParameterTambahanPaket
 * @see GelombangPendaftaran
 * @see ais.action.master.KelompokParameterTambahanCalonMahasiswaAction
 * @see ais.action.master.ParameterTambahanPaketAction
 * @see ais.action.master.pmb.ParameterTambahanListener
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_calon_mahasiswa")
public class KelompokParameterTambahanCalonMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap ini sengaja tidak pernah diubah agar instance yang
	 * tersimpan di session ZK/HTTP lama tetap dapat dibaca setelah kelas dikompilasi ulang.
	 *
	 * <p>Nilainya kebetulan sama persis dengan milik {@code KelompokParameterTambahanAlumni} dan
	 * beberapa saudara {@code KelompokParameterTambahan*} lain &mdash; jejak bahwa berkas-berkas
	 * itu dibuat dengan menyalin satu template yang sama.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci primer baris ini ({@code IDENTITY}/sequence PostgreSQL). Dideklarasikan ulang karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate; lihat {@link #getId()}.
	 */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis lewat {@link #onUpdate()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi otomatis lewat {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir, dengan <b>guard menolak nilai kosong</b>: bila
	 * argumen {@code null} atau hanya berisi spasi, method langsung {@code return} dan nilai lama
	 * dipertahankan.
	 *
	 * <p>Konsekuensinya jejak audit bersifat "sekali terisi tidak bisa dikosongkan" &mdash; nilai
	 * lama tidak akan pernah tertimpa oleh proses yang tidak membawa konteks pengguna (mis. job
	 * batch atau {@link #checkCreateDefault()} yang berjalan tanpa pengguna login).</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa efek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir, dengan guard menolak nilai kosong yang sama
	 * persis seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa efek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> mengubah state objek di tengah siklus flush. Jangan dipanggil manual
	 * dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini. Baris default hasil {@link #checkCreateDefault()} karena itu lahir tanpa jejak
	 * {@code oleh}/{@code olehId} sama sekali &mdash; kolom-kolom itu baru terisi saat admin
	 * pertama kali menyunting baris tersebut.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul. Karena diinisialisasi di
	 * deklarasi, setiap instance baru &mdash; termasuk yang dibuat
	 * {@link #KelompokParameterTambahanCalonMahasiswa()} &mdash; sudah membawa timestamp saat
	 * objek dibentuk, bukan saat disimpan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini TIDAK punya
	 * guard nilai kosong: memberi {@code null} benar-benar mengosongkan kolomnya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance yang baru dibentuk
	 *         di JVM, tetapi bisa {@code null} untuk baris lama yang kolomnya memang kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@code id}/{@code nama} LANGSUNG, bukan lewat getter, sehingga
	 * {@code nama} tidak ter-{@code trim} seperti pada {@link #getNama()} dan sebuah proxy
	 * Hibernate yang belum terinisialisasi akan menghasilkan {@code "null-null"}. Untuk baris yang
	 * belum tersimpan hasilnya berupa {@code "null-<nama>"}.</p>
	 *
	 * <p>Dipakai antara lain oleh {@code Common.insertCombo(...)} dan cetakan {@code println}
	 * debug di {@code GelombangPendaftaranAction}.</p>
	 *
	 * @return gabungan id dan nama kelompok
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama/judul seksi form tambahan sebagaimana ditampilkan ke calon mahasiswa (mis.
	 * {@code "VII. Form Tambahan"}). Wajib diisi dan diperlakukan unik oleh layar admin.
	 */
	private String nama;

	/** Keterangan bebas untuk admin; ikut dicetak sebagai kolom kedua pada grid layar admin. */
	private String keterangan;

	/**
	 * Penanda bahwa baris ini adalah kelompok bawaan sistem hasil {@link #checkCreateDefault()}.
	 * Baris ber-{@code true} tidak bisa dihapus dari layar admin (tombol hapusnya disembunyikan).
	 */
	private Boolean defaultData;

	/**
	 * Penanda kelompok masih dipakai. Menjadi syarat {@code Criteria} di
	 * {@code ParameterTambahanListener} pada jalur "gelombang tanpa daftar spesifik".
	 */
	private Boolean aktif;

	/** Nomor urut tampil seksi pada formulir; satu-satunya kunci {@link #compareTo(GeneralValueObject)}. */
	private Integer nomorUrut;

	/** Izin tampil pada form pendaftaran publik (sebelum calon punya akun/login). */
	private Boolean tampilDiFormPendaftaran;

	/** Izin tampil pada form biodata setelah calon mahasiswa login. */
	private Boolean tampilDiFormSetelahLogin;

	/**
	 * Memastikan tabel ini SELALU memiliki satu kelompok bawaan sistem, dan membuatnya bila belum
	 * ada. Pola yang sama dipakai seluruh keluarga {@code KelompokParameterTambahan*}.
	 *
	 * <p>Alurnya:</p>
	 * <ol>
	 *   <li>Membuka {@code HibernateUtil.currentNativeSession()} (session native yang dibagi
	 *       sepanjang thread, di luar session ZK biasa).</li>
	 *   <li>Mencari baris pertama dengan {@code defaultData = true}
	 *       ({@code setMaxResults(1).uniqueResult()}).</li>
	 *   <li>Bila tidak ada, membuat baris baru dengan {@code defaultData = true},
	 *       {@code nama} dan {@code keterangan} sama-sama diisi {@code "VII. Form Tambahan"},
	 *       lalu menyimpannya dalam transaksi eksplisit
	 *       ({@code begin()} &hellip; {@code save()} &hellip; {@code commit()}).</li>
	 *   <li>Menutup session lewat {@code HibernateUtil.closeSession()} &mdash; <b>selalu</b>,
	 *       baik baris baru dibuat maupun tidak.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping penting:</b></p>
	 * <ul>
	 *   <li><b>Menutup session Hibernate milik thread.</b> Objek yang dikembalikan sudah
	 *       <i>detached</i>; membaca properti lazy setelah pemanggilan ini akan gagal. Pemanggil
	 *       harus meminta session baru ({@code HibernateUtil.currentSession()}) untuk pekerjaan
	 *       berikutnya &mdash; persis yang dilakukan {@code ParameterTambahanPaketAction}.</li>
	 *   <li><b>Menulis ke database dari jalur "pembacaan" layar.</b> Method ini dipanggil dari
	 *       {@code doAfterCompose(...)}, jadi sekadar membuka layar bisa menghasilkan
	 *       {@code INSERT} baru berikut revisi Envers-nya.</li>
	 *   <li><b>Nilai kolom lain diisi oleh getter, bukan oleh method ini.</b> Saat {@code save()},
	 *       Hibernate membaca properti lewat getter, sehingga baris default lahir dengan
	 *       {@code aktif = true} ({@link #getAktif()}), {@code nomorUrut = 1}
	 *       ({@link #getNomorUrut()}), {@code tampilDiFormPendaftaran = false}
	 *       ({@link #getTampilDiFormPendaftaran()}), dan {@code tampilDiFormSetelahLogin = true}
	 *       ({@link #getTampilDiFormSetelahLogin()}).</li>
	 *   <li><b>Kuirk yang layak diketahui admin:</b> karena {@code tampilDiFormPendaftaran} lahir
	 *       {@code false}, kelompok default ini &mdash; beserta semua field tambahan yang diadopsi
	 *       ke dalamnya oleh pemanggil (lihat di bawah) &mdash; TIDAK muncul di form pendaftaran
	 *       publik sampai admin mencentang "Tampil Di Form Pendaftaran" pada layar Manajemen
	 *       Kelompok. Di form setelah login ia langsung muncul.</li>
	 * </ul>
	 *
	 * <p><b>Konsumen nyata:</b> satu-satunya pemanggil di seluruh codebase adalah
	 * {@code ais.action.master.ParameterTambahanPaketAction#doAfterCompose(Component)} (layar
	 * "Parameter Tambahan Paket"). Di sana hasilnya langsung dipakai untuk menjalankan SQL mentah:</p>
	 * <pre>
	 *   update parameter_tambahan_paket
	 *      set kelompok_parameter_tambahan_calon_mahasiswa = &lt;id default&gt;
	 *    where kelompok_parameter_tambahan_calon_mahasiswa is null;
	 * </pre>
	 * <p>yakni <b>migrasi data yang dijalankan ulang setiap kali layar dibuka</b>: seluruh field
	 * tambahan yang belum punya kelompok diadopsi ke kelompok default. Perhatikan bahwa jalur SQL
	 * mentah ini melewati Hibernate/Envers, jadi perpindahan tersebut tidak terekam di tabel
	 * revisi. Id yang disisipkan berasal dari {@link #getId()} (bertipe {@code Long}), bukan dari
	 * masukan pengguna, sehingga tidak membuka celah injeksi.</p>
	 *
	 * <p>Berbeda dari beberapa saudaranya (mis. {@code KelompokParameterTambahanProdukKoperasi}),
	 * varian calon mahasiswa ini <b>tidak</b> ikut dipanggil dari
	 * {@code ais.common.InitData#reloadDefaults()}.</p>
	 *
	 * @return baris kelompok bawaan sistem yang sudah dipastikan ada; objek dalam keadaan
	 *         <i>detached</i> karena session sudah ditutup
	 */
	public static KelompokParameterTambahanCalonMahasiswa checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa = (KelompokParameterTambahanCalonMahasiswa) session
				.createCriteria(KelompokParameterTambahanCalonMahasiswa.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCalonMahasiswa == null) {
			kelompokParameterTambahanCalonMahasiswa = new KelompokParameterTambahanCalonMahasiswa();
			kelompokParameterTambahanCalonMahasiswa.setDefaultData(true);
			kelompokParameterTambahanCalonMahasiswa.setNama("VII. Form Tambahan");
			kelompokParameterTambahanCalonMahasiswa.setKeterangan("VII. Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCalonMahasiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCalonMahasiswa;
	}

	/**
	 * Konstruktor tanpa argumen. Diperlukan Hibernate untuk instansiasi saat memuat baris, dan
	 * dipakai layar admin ({@code KelompokParameterTambahanCalonMahasiswaAction#onAdd(Event)})
	 * serta {@link #checkCreateDefault()} untuk membuat kelompok baru.
	 *
	 * <p>Seluruh properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah terisi
	 * dari inisialisasi field. Nilai efektif untuk {@code aktif}, {@code nomorUrut},
	 * {@code defaultData}, dan kedua flag {@code tampilDiForm*} datang dari getter masing-masing,
	 * bukan dari konstruktor ini.</p>
	 */
	public KelompokParameterTambahanCalonMahasiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Anotasi {@code @Id} berada di sini, sehingga seluruh entity memakai <i>property access</i>
	 * Hibernate. Kolomnya {@code insertable = false} karena nilainya dihasilkan database
	 * ({@code IDENTITY}/sequence).</p>
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
	 * Menyetel kunci primer. Praktis hanya dipanggil Hibernate; kode aplikasi tidak boleh mengubah
	 * id baris yang sudah tersimpan.
	 *
	 * @param id kunci primer baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/judul seksi, sudah di-{@code trim} dari spasi di kedua ujung.
	 *
	 * <p><b>Pemangkasan ini hanya berlaku pada nilai kembalian</b> &mdash; field aslinya tidak
	 * diubah, jadi getter ini tidak menulis balik apa pun ke database. Namun karena Hibernate
	 * memakai property access, nilai yang <i>tersimpan</i> ke kolom {@code nama} adalah versi yang
	 * sudah ter-{@code trim}. Nilai {@code null} diteruskan apa adanya, sehingga pemanggil tetap
	 * harus siap menerima {@code null} (bandingkan dengan {@link #toString()} yang membaca field
	 * mentah).</p>
	 *
	 * @return nama kelompok tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama/judul seksi. Tidak melakukan validasi apa pun di sini; pemeriksaan "tidak boleh
	 * kosong" dan "tidak boleh duplikat" dilakukan layar admin
	 * ({@code KelompokParameterTambahanCalonMahasiswaAction#onSave(Event)} dan
	 * {@code checkNamaKelompokParameterTambahanCalonMahasiswa()}), bukan oleh entity.
	 *
	 * @param nama nama kelompok
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kelompok ini, <b>apa adanya termasuk {@code null}</b>.
	 *
	 * <p><b>Perhatian &mdash; membalik kontrak kelas induk.</b>
	 * {@code GeneralValueObject#getKeterangan()} menjamin tidak pernah mengembalikan {@code null}
	 * (memetakan {@code null} menjadi {@code ""}); override di sini meniadakan jaminan itu. Kode
	 * yang menerima objek sebagai {@link GeneralValueObject} dan mengandalkan kontrak induk bisa
	 * ber-NPE saat objeknya ternyata bertipe kelas ini. Pengaruh praktisnya juga sampai ke
	 * pengurutan bawaan induk (cabang {@code keterangan} pada
	 * {@code GeneralValueObject#compareTo(GeneralValueObject)}) &mdash; meski di kelas ini cabang
	 * itu tidak pernah tercapai karena {@link #compareTo(GeneralValueObject)} di-override penuh.</p>
	 *
	 * @return keterangan kelompok, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas kelompok ini.
	 *
	 * @param keterangan keterangan kelompok; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "kelompok bawaan sistem", dengan default {@code false}.
	 *
	 * <p><b>Getter mutatif:</b> bila field masih {@code null}, method ini <b>menulis {@code false}
	 * ke field</b> sebelum mengembalikannya &mdash; bukan sekadar mengembalikan nilai pengganti.
	 * Karena Hibernate memakai property access + {@code dynamicUpdate}, pemanggilan getter ini
	 * pada objek yang masih terikat session akan membuat baris menjadi "kotor" dan kolom
	 * {@code defaultdata} ikut ter-{@code UPDATE} ke {@code false} pada flush berikutnya, lengkap
	 * dengan revisi Envers-nya. Dampaknya di sini kecil (nilai yang ditulis sama dengan makna
	 * {@code null}) dan bersifat <i>self-healing</i>, tetapi tetap menghasilkan tulisan dan revisi
	 * audit yang tidak pernah diminta pengguna.</p>
	 *
	 * <p>Dipakai layar admin untuk menyembunyikan tombol hapus pada baris bawaan sistem, dan oleh
	 * {@link #checkCreateDefault()} lewat kriteria {@code defaultData = true}. Catat bahwa
	 * kriteria tersebut berjalan di sisi SQL: baris yang kolomnya masih {@code NULL} di database
	 * tidak akan cocok, berapa pun nilai yang dikembalikan getter ini di JVM.</p>
	 *
	 * @return {@code true} bila baris ini kelompok bawaan sistem; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda "kelompok bawaan sistem". Hanya dipanggil {@link #checkCreateDefault()};
	 * tidak ada kendali UI untuk properti ini.
	 *
	 * @param defaultData {@code true} untuk menandai baris bawaan sistem
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan status aktif kelompok, dengan default {@code true}.
	 *
	 * <p><b>Getter mutatif</b> dengan mekanisme sama seperti {@link #getDefaultData()}: nilai
	 * {@code null} ditulis balik menjadi {@code true} ke field, dan lewat property access +
	 * {@code dynamicUpdate} berpotensi ter-{@code UPDATE} ke kolom {@code aktif} berikut revisi
	 * Envers.</p>
	 *
	 * <p><b>Ketidakcocokan Java vs SQL yang perlu diwaspadai:</b> jalur penyaringan sesungguhnya di
	 * {@code ParameterTambahanListener} memakai
	 * {@code Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true)}, yaitu
	 * perbandingan di sisi database. Baris yang kolom {@code aktif}-nya masih {@code NULL}
	 * <b>tidak</b> lolos filter itu, walaupun getter ini menyatakan {@code true} bagi kode Java.
	 * Efek write-back di atas yang justru "menyembuhkan" ketidakcocokan tersebut: cukup baris itu
	 * pernah termuat dan ter-flush dalam satu session aktif (mis. saat layar Manajemen Kelompok
	 * dibuka), kolomnya terisi {@code true} dan mulai lolos filter. Sampai saat itu terjadi, seksi
	 * yang bersangkutan tidak pernah dirender di formulir calon mahasiswa.</p>
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
	 * Menyetel status aktif kelompok. Dipanggil dari checkbox "Aktif" pada grid layar admin, yang
	 * langsung menyimpan lewat {@code Common.refreshSaveOrUpdate(...)}.
	 *
	 * @param aktif {@code true} bila kelompok masih dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil seksi, dengan default {@code 1}.
	 *
	 * <p><b>Getter mutatif</b>, seperti {@link #getAktif()} dan {@link #getDefaultData()}: nilai
	 * {@code null} ditulis balik ke field sebagai {@code 1} dan berpotensi menghasilkan
	 * {@code UPDATE} kolom {@code nomorurut} berikut revisi Envers pada flush berikutnya.</p>
	 *
	 * <p><b>Kuirk penulisan:</b> baris {@code return} memakai ternary
	 * {@code nomorUrut == null ? 1 : nomorUrut} padahal blok {@code if} tepat di atasnya sudah
	 * memastikan field tidak lagi {@code null}. Cabang {@code null} itu <b>mati total</b> dan
	 * ternary-nya redundan &mdash; sisa penulisan yang tidak dirapikan, bukan pengaman tambahan.
	 * Dibiarkan apa adanya di sini karena tugas dokumentasi tidak boleh mengubah logika.</p>
	 *
	 * <p>Nilai ini adalah satu-satunya kunci {@link #compareTo(GeneralValueObject)}, sehingga
	 * menentukan urutan seksi pada formulir calon mahasiswa. Karena getter menjamin bukan
	 * {@code null}, {@code compareTo} kelas ini bebas dari NPE.</p>
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
	 * Menyetel nomor urut tampil seksi. Dipanggil dari {@code Intbox} pada grid layar admin.
	 *
	 * <p>Tidak ada validasi keunikan: beberapa kelompok boleh memiliki nomor urut sama, dan
	 * urutan relatif di antara mereka menjadi tidak deterministik karena
	 * {@link #compareTo(GeneralValueObject)} tidak punya kunci cadangan.</p>
	 *
	 * @param nomorUrut nomor urut tampil; {@code null} akan dipulihkan menjadi {@code 1} oleh
	 *                  {@link #getNomorUrut()}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan dua kelompok <b>murni berdasarkan {@link #getNomorUrut()}</b>.
	 *
	 * <p>Override penuh terhadap {@code GeneralValueObject#compareTo(GeneralValueObject)} yang
	 * berjenjang ({@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr;
	 * {@code keterangan}): di sini tidak ada kunci cadangan sama sekali, jadi dua kelompok dengan
	 * nomor urut sama selalu dianggap sederajat ({@code 0}) dan urutannya bergantung pada
	 * stabilitas algoritma pengurutan pemanggil.</p>
	 *
	 * <p><b>Efek samping tak langsung:</b> karena memanggil {@link #getNomorUrut()} pada KEDUA
	 * objek, sekadar mengurutkan daftar dapat menulis {@code 1} ke field kedua objek dan
	 * memicu {@code UPDATE} pada flush berikutnya (lihat {@link #getNomorUrut()}).</p>
	 *
	 * <p><b>Batasan tipe:</b> argumen di-<i>cast</i> tanpa pemeriksaan ke
	 * {@code KelompokParameterTambahanCalonMahasiswa}, sehingga membandingkannya dengan
	 * {@link GeneralValueObject} jenis lain melempar {@code ClassCastException}. Pemakaian nyata
	 * ({@code Collections.sort(...)} atas daftar homogen di
	 * {@code ParameterTambahanListener#renderParameterTambahan(...)}) aman dari masalah ini.</p>
	 *
	 * @param arg0 kelompok pembanding; harus bertipe
	 *             {@code KelompokParameterTambahanCalonMahasiswa}
	 * @return nilai negatif/nol/positif sesuai perbandingan nomor urut
	 * @throws ClassCastException bila {@code arg0} bukan kelas ini
	 * @throws NullPointerException bila {@code arg0} {@code null}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCalonMahasiswa) arg0).getNomorUrut());
	}

	/**
	 * Mengembalikan izin tampil seksi pada <b>form pendaftaran publik</b> (calon belum punya akun),
	 * dengan default {@code false}.
	 *
	 * <p>Berbeda dari {@link #getAktif()}/{@link #getNomorUrut()}/{@link #getDefaultData()},
	 * getter ini <b>TIDAK mutatif</b>: nilai pengganti hanya dipakai pada kembalian lewat ternary,
	 * field aslinya dibiarkan {@code null}. Jadi tidak ada write-back maupun revisi Envers dari
	 * sini.</p>
	 *
	 * <p>Konsekuensi default {@code false}: kelompok yang belum pernah dikonfigurasi &mdash;
	 * termasuk kelompok bawaan hasil {@link #checkCreateDefault()} &mdash; tidak akan muncul di
	 * form pendaftaran publik sampai admin mencentangnya di layar Manajemen Kelompok. Flag ini
	 * dievaluasi {@code ParameterTambahanListener} HANYA pada jalur "gelombang tanpa daftar
	 * spesifik"; bila gelombang sudah memilih kelompok secara eksplisit lewat
	 * {@code GelombangPendaftaran#getKelompokParameterTambahanCalonMahasiswas()}, flag ini
	 * diabaikan seluruhnya.</p>
	 *
	 * @return {@code true} bila seksi boleh tampil di form pendaftaran publik; tidak pernah
	 *         {@code null}
	 */
	public Boolean getTampilDiFormPendaftaran() {
		return tampilDiFormPendaftaran == null ? false : tampilDiFormPendaftaran;
	}

	/**
	 * Menyetel izin tampil seksi pada form pendaftaran publik. Dipanggil dari checkbox "Tampil Di
	 * Form Pendaftaran" pada grid layar admin, yang langsung menyimpan perubahannya.
	 *
	 * @param tampilDiFormPendaftaran {@code true} bila seksi boleh tampil sebelum calon login
	 */
	public void setTampilDiFormPendaftaran(Boolean tampilDiFormPendaftaran) {
		this.tampilDiFormPendaftaran = tampilDiFormPendaftaran;
	}

	/**
	 * Mengembalikan izin tampil seksi pada <b>form biodata setelah calon mahasiswa login</b>,
	 * dengan default {@code true}.
	 *
	 * <p>Sama seperti {@link #getTampilDiFormPendaftaran()}, getter ini TIDAK mutatif. Perhatikan
	 * <b>asimetri default</b> antara keduanya: seksi baru secara bawaan tersembunyi di form
	 * pendaftaran publik tetapi terlihat di form setelah login &mdash; sikap "aman secara bawaan"
	 * agar field kustom tidak bocor ke halaman pendaftaran terbuka sebelum admin memutuskannya.</p>
	 *
	 * <p>Flag ini juga hanya dievaluasi pada jalur "gelombang tanpa daftar spesifik" di
	 * {@code ParameterTambahanListener}.</p>
	 *
	 * @return {@code true} bila seksi boleh tampil setelah calon login; tidak pernah {@code null}
	 */
	public Boolean getTampilDiFormSetelahLogin() {
		return tampilDiFormSetelahLogin == null ? true : tampilDiFormSetelahLogin;
	}

	/**
	 * Menyetel izin tampil seksi pada form biodata setelah calon mahasiswa login. Dipanggil dari
	 * checkbox "Tampil Di Form Setelah Login" pada grid layar admin.
	 *
	 * @param tampilDiFormSetelahLogin {@code true} bila seksi boleh tampil setelah calon login
	 */
	public void setTampilDiFormSetelahLogin(Boolean tampilDiFormSetelahLogin) {
		this.tampilDiFormSetelahLogin = tampilDiFormSetelahLogin;
	}
}
