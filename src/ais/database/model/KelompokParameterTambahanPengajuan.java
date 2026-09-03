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
 * Entity master <b>kategori (seksi) field kustom</b> untuk modul <i>Pengajuan</i> &mdash; tabel
 * {@code public.kelompok_parameter_tambahan_pengajuan}.
 *
 * <p>Satu baris di sini adalah satu <b>judul seksi</b> pada formulir permohonan; bukan field-nya
 * sendiri. Field/isian sesungguhnya hidup di {@link ais.database.model.ParameterTambahan} dan
 * dipasang ke seksi lewat baris penghubung {@link ais.database.model.ParameterTambahanPengajuan}.
 * Baris kategori inilah yang tercetak sebagai label tebal pemisah antar-kelompok isian di layar
 * permohonan.</p>
 *
 * <h3>Domain "Pengajuan" yang mana? (terverifikasi dari pemanggil nyata)</h3>
 * <p>Keluarga {@code KelompokParameterTambahan*} punya banyak varian yang namanya mirip. Varian
 * <b>ini</b> melayani permohonan/pengajuan layanan yang diajukan <b>peserta didik</b>, yaitu:</p>
 * <ul>
 *   <li>{@link ais.database.model.PengajuanMahasiswa} (tabel {@code pengajuan_mahasiswa},
 *   perguruan tinggi) &mdash; dirender {@code ais.action.master.PengajuanMahasiswaAction};</li>
 *   <li>{@link ais.database.model.sekolah.PengajuanSiswa} (tabel {@code pengajuan_siswa}, modul
 *   sekolah) &mdash; dirender {@code ais.action.master.sekolah.PengajuanSiswaAction}.</li>
 * </ul>
 * <p>Keduanya turunan {@code DataSop} dan <b>berbagi satu master</b>
 * {@link ais.database.model.JenisPengajuan} (tabel {@code jenis_pengajuan}), yang menjadi
 * satu-satunya pemilik relasi ke entity ini. Jadi domainnya <b>bukan</b> "umum" dan <b>bukan</b>
 * pegawai: permohonan pegawai memakai keluarga terpisah
 * {@link ais.database.model.KelompokParameterTambahanPengajuanPegawai} (tabel
 * {@code kelompok_parameter_tambahan_pengajuan_pegawai}), dan pengajuan transaksi payroll memakai
 * {@link ais.database.model.payroll.KelompokParameterTambahanPengajuanTransaksiPegawai}.</p>
 *
 * <h3>Rantai konfigurasi &mdash; 4 lapis</h3>
 * <ol>
 *   <li>{@link ais.database.model.ParameterTambahan} &mdash; definisi field kustom (label, tipe
 *   input, pilihan, bendera {@code aktif}).</li>
 *   <li>{@link ais.database.model.ParameterTambahanPengajuan} &mdash; baris penghubung yang
 *   menempelkan sebuah {@code ParameterTambahan} ke <b>kategori ini</b> (kolom FK
 *   {@code kelompok_parameter_tambahan_pengajuan}) beserta {@code nomorUrut} field di dalam
 *   seksi.</li>
 *   <li><b>Kelas ini</b> &mdash; kategori/seksi, dengan bendera {@link #getAktif()} dan
 *   {@link #getNomorUrut()} sebagai urutan seksi.</li>
 *   <li>{@link ais.database.model.JenisPengajuan} &mdash; lapis terakhir: kategori baru
 *   <b>tidak otomatis</b> muncul di formulir. Admin harus mencentang kategori itu per jenis
 *   permohonan pada layar "Jenis Pengajuan"; centangnya disimpan di tabel penghubung
 *   {@code jenis_pengajuan_has_parameter}
 *   ({@code JenisPengajuan.getKelompokParameterTambahanPengajuans()}, {@code @ManyToMany}).</li>
 * </ol>
 * <p>Struktur 4 lapis ini identik dengan varian {@code KelompokParameterTambahanCatatanPegawai}/
 * {@code KelompokParameterTambahanCatatanMahasiswa}, dan berbeda dari
 * {@link ais.database.model.KelompokParameterTambahanAlumni} yang hanya 3 lapis (tanpa lapis
 * "jenis" yang harus dicentang).</p>
 *
 * <h3>Auto-seed lewat {@link #checkCreateDefault()}</h3>
 * <p>Baris penghubung {@code ParameterTambahanPengajuan} mewajibkan sebuah kategori. Agar instalasi
 * baru tidak pernah menemui dropdown kategori yang kosong, {@link #checkCreateDefault()} membuat
 * sendiri satu baris bernama {@code "Form Tambahan"} ({@code defaultData = true}) dan
 * meng-{@code commit}-nya langsung ke DB.</p>
 * <p><b>Pemanggil nyata (satu-satunya di codebase):</b>
 * {@code ais.action.master.ParameterTambahanPengajuanAction.doAfterCompose(Component)} &mdash;
 * layar master "Parameter Tambahan Pengajuan", dipanggil sebelum combobox kategori diisi. Perhatikan
 * <b>perbedaan penting dari varian Alumni</b>: di sini <b>tidak ada</b> mekanisme seed kedua.
 * {@code KelompokParameterTambahanPengajuanAction} (layar master kategori) sama sekali tidak
 * memanggil {@code checkCreateDefault()} maupun menanam baris bawaan sendiri, sehingga bug
 * "dua kategori bawaan berbeda tergantung urutan klik admin" yang tercatat di
 * {@link ais.database.model.KelompokParameterTambahanAlumni} <b>tidak berlaku</b> untuk varian
 * ini.</p>
 *
 * <h3>Layar pengelola</h3>
 * <p>{@code ais.action.master.KelompokParameterTambahanPengajuanAction} +
 * {@code /pages/master/kelompok_parameter_tambahan_pengajuan.zul}. Layar ini tidak berdiri sendiri
 * di menu: ia disisipkan sebagai tab "Manajemen Kelompok" di dalam layar
 * {@code ParameterTambahanPengajuanAction} lewat {@code MyInclude}.</p>
 *
 * <h3>Catatan warisan &mdash; mengapa field dideklarasikan ulang</h3>
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}, tetapi base class itu
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa.
 * Hibernate karena itu <b>tidak</b> memetakan properti induknya sama sekali. Deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, {@code nama}, {@code keterangan}
 * di kelas ini <b>bukan duplikasi keliru</b>, melainkan keharusan teknis agar kolom-kolom itu benar
 * terpetakan. Konsekuensi turunannya: field bernama sama di base class tetap ada dan selalu
 * {@code null} (mis. {@code getNim()} pada base). Lihat
 * {@link ais.database.model.GeneralValueObject} untuk pembahasan lengkap.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 *   {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *   {@link #setTanggal_dirubah(Date)}, callback {@code onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}, {@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Isi bisnis:</b> {@link #getNama()}, {@link #setNama(String)},
 *   {@link #getKeterangan()}, {@link #setKeterangan(String)}.</li>
 *   <li><b>Bendera &amp; urutan:</b> {@link #getDefaultData()}, {@link #setDefaultData(Boolean)},
 *   {@link #getAktif()}, {@link #setAktif(Boolean)}, {@link #getNomorUrut()},
 *   {@link #setNomorUrut(Integer)}.</li>
 *   <li><b>Pengurutan:</b> {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Utilitas statis:</b> {@link #checkCreateDefault()}.</li>
 *   <li><b>Konstruksi:</b> {@link #KelompokParameterTambahanPengajuan()}.</li>
 * </ul>
 *
 * <h3>Kuirk &amp; jebakan yang sudah terverifikasi (jangan "dirapikan" tanpa uji)</h3>
 * <ul>
 *   <li><b>{@link #getKeterangan()} MEMBALIK kontrak base class.</b>
 *   {@code GeneralValueObject.getKeterangan()} menormalkan {@code null} menjadi {@code ""} dan
 *   menjanjikan hasil non-null; override di kelas ini mengembalikan field <b>mentah</b>, jadi bisa
 *   {@code null}. Pemanggil wajib memeriksa sendiri.</li>
 *   <li><b>{@link #compareTo(GeneralValueObject)} DIPANGKAS.</b> Hanya satu baris pembandingan
 *   {@code nomorUrut}, tanpa {@code instanceof} dan tanpa rantai fallback
 *   {@code nim}/{@code nama}/{@code keterangan} milik base class &mdash; berbeda dari
 *   {@link ais.database.model.KelompokParameterTambahanAlumni} yang memakai bentuk lengkap.
 *   Detail risiko di Javadoc method tersebut.</li>
 *   <li><b>Getter yang mengotori state.</b> {@link #getDefaultData()}, {@link #getAktif()}, dan
 *   {@link #getNomorUrut()} menulis nilai default ke field saat menemukan {@code null}. Pada
 *   instance ter-{@code attach}, sekadar <i>membaca</i> baris bisa memicu {@code UPDATE} dan
 *   revisi Envers palsu (kelas memakai {@code dynamicUpdate = true} + akses properti).</li>
 *   <li><b>Kolom {@code nomorUrut} tidak pernah diisi form Tambah/Ubah.</b> Dialog
 *   {@code KelompokParameterTambahanPengajuanAction.init(...)} hanya memuat "Nama Kelompok" dan
 *   "Keterangan". Satu-satunya jalur pengisian adalah {@code Intbox} di grid daftar, sehingga pada
 *   pemakaian normal <b>semua</b> kategori punya {@code nomorUrut = 1}. Akibatnya lihat catatan
 *   {@code TreeSet} di {@link #compareTo(GeneralValueObject)}.</li>
 * </ul>
 *
 * <h3>Catatan keamanan (bukan bug entity ini, tapi menyangkut data yang dikelolanya)</h3>
 * <ul>
 *   <li>Pada renderer grid {@code KelompokParameterTambahanPengajuanAction}, checkbox "Aktif"
 *   dijaga ({@code setDisabled(!edit)}) dan tombol Ubah/Hapus dijaga ({@code setVisible(edit)}/
 *   {@code setVisible(delete)}), tetapi {@code Intbox} nomor urut <b>tidak dijaga sama sekali</b>.
 *   Pengguna dengan hak READ saja bisa mengubah dan menyimpan urutan seksi formulir permohonan.</li>
 *   <li>Layar induknya, {@code ParameterTambahanPengajuanAction}, meng-hardcode
 *   {@code edit = true} dan {@code delete = true} dan tidak memanggil {@code CommonPrivilages}
 *   sama sekali.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.KelompokParameterTambahanAlumni
 * @see ais.database.model.KelompokParameterTambahanPengajuanPegawai
 * @see ais.database.model.ParameterTambahanPengajuan
 * @see ais.database.model.JenisPengajuan
 * @see ais.database.model.PengajuanMahasiswa
 * @see ais.database.model.sekolah.PengajuanSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_pengajuan")
public class KelompokParameterTambahanPengajuan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p><b>Kuirk:</b> nilainya <b>sama persis</b> dengan milik
	 * {@link ais.database.model.ParameterTambahanPengajuan} dan beberapa entity lain di keluarga
	 * ini &mdash; sisa salin-tempel generator. Tidak berbahaya selama kedua kelas tidak pernah
	 * saling menggantikan dalam satu aliran serialisasi (dan memang tidak).</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris kategori; di-generate PostgreSQL. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris; diisi callback {@code onUpdate()}. */
	private String oleh;

	/** Id/username pengguna terakhir yang mengubah baris; diisi callback {@code onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
	 *
	 * @return id/username pengubah terakhir, atau {@code null} bila baris belum pernah di-{@code UPDATE}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Kuirk penting:</b> nilai {@code null}, string kosong, atau hanya spasi
	 * <b>diabaikan diam-diam</b> (method langsung {@code return} tanpa menyentuh field), sehingga
	 * nilai lama bertahan. Ini disengaja agar jejak audit tidak terhapus oleh proses batch/seed yang
	 * berjalan tanpa konteks pengguna login &mdash; tetapi juga berarti jejak audit
	 * <b>tidak bisa dikosongkan</b> lewat setter ini.</p>
	 *
	 * @param olehId id/username pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
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
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati callback
	 * ini. Karena itu baris hasil auto-seed {@link #checkCreateDefault()} masuk <b>tanpa jejak</b>
	 * {@code oleh}/{@code olehId} sama sekali.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #getDefaultData()}, {@link #getAktif()}, dan
	 * {@link #getNomorUrut()} dapat mengotori field saat baris sekadar dibaca, sehingga callback ini
	 * bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta pengguna mana pun</b> &mdash;
	 * jejak audit lalu mencatat pengguna yang kebetulan sedang membuka layar Pengajuan.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * <p>Pada alur normal hanya dipanggil {@code AuditTimestampInterceptor} dari callback
	 * {@code onUpdate()}; kode bisnis tidak perlu memanggilnya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Field diinisialisasi jam aplikasi saat objek dibuat, jadi nilainya <b>tidak pernah</b>
	 * {@code null} untuk objek yang lahir di JVM ini. Untuk baris yang di-{@code load} dari DB,
	 * nilainya berasal dari kolom {@code tanggal_dirubah} dan bisa {@code null} pada baris lama
	 * (mis. hasil auto-seed yang belum pernah di-{@code UPDATE}).</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai kombo/daftar ZK yang belum menyetel {@code itemRenderer} sendiri, dan muncul pada
	 * log/pesan error. Membaca field {@code nama} <b>langsung</b> (bukan lewat {@link #getNama()}),
	 * jadi tidak ada {@code trim()} dan tidak ada normalisasi {@code null} &mdash; baris yang
	 * namanya belum diisi tercetak sebagai {@code "12-null"}.</p>
	 *
	 * @return string {@code id + "-" + nama}; tidak pernah {@code null}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kategori/seksi form; wajib diisi (kolom {@code NOT NULL}). Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas kategori; boleh {@code null}. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Penanda kategori bawaan sistem hasil {@link #checkCreateDefault()}. */
	private Boolean defaultData;

	/** Bendera aktif; kategori non-aktif disaring saat form permohonan dirakit. */
	private Boolean aktif;

	/** Urutan tampil seksi pada formulir; satu-satunya kunci {@link #compareTo(GeneralValueObject)}. */
	private Integer nomorUrut;

	/**
	 * Memastikan tabel ini <b>selalu</b> punya satu kategori bawaan, dan mengembalikannya.
	 *
	 * <p>Mencari baris pertama ber-{@code defaultData = true}. Bila tidak ada, membuat baris baru
	 * dengan {@code nama} dan {@code keterangan} sama-sama {@code "Form Tambahan"},
	 * {@code defaultData = true}, lalu menyimpannya dalam transaksi tersendiri.</p>
	 *
	 * <h4>Efek samping</h4>
	 * <ul>
	 *   <li><b>Menulis ke database</b> ({@code INSERT} + {@code commit}) &mdash; ini bukan method
	 *   baca. Jangan dipanggil dari jalur yang seharusnya read-only.</li>
	 *   <li>Memakai {@code HibernateUtil.currentNativeSession()} dan <b>selalu</b> menutup session
	 *   itu di akhir lewat {@code HibernateUtil.closeSession()}, termasuk pada kasus "sudah ada"
	 *   ketika tidak ada apa pun yang ditulis. Objek yang dikembalikan karena itu berada dalam
	 *   keadaan <b>detached</b>: mengaksesnya lebih lanjut tidak akan memicu lazy loading, dan
	 *   perubahan padanya tidak akan ter-flush.</li>
	 *   <li>Karena baris baru masuk lewat {@code INSERT}, callback {@code @PreUpdate}
	 *   ({@code onUpdate()}) <b>tidak</b> berjalan &mdash; kategori bawaan lahir tanpa
	 *   {@code oleh}/{@code olehId}.</li>
	 * </ul>
	 *
	 * <h4>Kuirk</h4>
	 * <ul>
	 *   <li><b>Tidak ada penanganan error:</b> tidak ada {@code try}/{@code rollback}. Bila
	 *   {@code commit} gagal, exception merambat ke pemanggil dan session tidak pernah ditutup
	 *   (baris {@code closeSession()} tidak tercapai).</li>
	 *   <li><b>Tidak ada penguncian/keunikan di level DB:</b> dua request bersamaan pada instalasi
	 *   kosong dapat membuat dua baris {@code "Form Tambahan"} sekaligus. Query pencarian memakai
	 *   {@code setMaxResults(1)} sehingga duplikat itu tidak akan pernah memicu error, hanya
	 *   diam-diam terabaikan.</li>
	 *   <li><b>Nama variabel lokal berbunyi {@code kelompokParameterTambahanCatatanAdministrasi}</b>
	 *   &mdash; sisa salin-tempel dari varian
	 *   {@code KelompokParameterTambahanCatatanAdministrasi}; tidak berpengaruh pada perilaku.</li>
	 *   <li>{@code nomorUrut} dan {@code aktif} <b>tidak</b> disetel eksplisit, jadi kolomnya masuk
	 *   {@code NULL} ke DB (kelas memakai {@code dynamicInsert = true}). Nilai efektifnya baru
	 *   muncul saat dibaca lewat {@link #getNomorUrut()}/{@link #getAktif()}, yang sekaligus
	 *   menulis default ke field &mdash; lihat catatan getter destruktif di dokumentasi kelas.</li>
	 * </ul>
	 *
	 * <h4>Pemanggil</h4>
	 * <p><b>Satu-satunya pemanggil di codebase</b> adalah
	 * {@code ais.action.master.ParameterTambahanPengajuanAction.doAfterCompose(Component)} &mdash;
	 * layar master "Parameter Tambahan Pengajuan". Dipanggil tepat sebelum
	 * {@code Common.insertCombo(...)} mengisi combobox kategori, supaya dropdown tidak pernah kosong
	 * pada instalasi baru dan tombol "Tambah" parameter selalu punya kategori penampung. Berbeda
	 * dari varian Alumni, layar master kategorinya sendiri
	 * ({@code KelompokParameterTambahanPengajuanAction}) <b>tidak</b> memanggil method ini dan tidak
	 * punya mekanisme seed sendiri, jadi hanya ada satu jalur pembuatan kategori bawaan.</p>
	 *
	 * @return baris kategori bawaan (yang ditemukan atau yang baru saja dibuat); tidak pernah
	 *         {@code null}, tetapi dalam keadaan <b>detached</b>
	 */
	public static KelompokParameterTambahanPengajuan checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanPengajuan kelompokParameterTambahanCatatanAdministrasi = (KelompokParameterTambahanPengajuan) session
				.createCriteria(KelompokParameterTambahanPengajuan.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanAdministrasi == null) {
			kelompokParameterTambahanCatatanAdministrasi = new KelompokParameterTambahanPengajuan();
			kelompokParameterTambahanCatatanAdministrasi.setDefaultData(true);
			kelompokParameterTambahanCatatanAdministrasi.setNama("Form Tambahan");
			kelompokParameterTambahanCatatanAdministrasi.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCatatanAdministrasi);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCatatanAdministrasi;
	}

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate/ZK data-binding; seluruh field dibiarkan
	 * pada nilai awalnya ({@code null}, kecuali {@code tanggal_dirubah} yang diisi jam aplikasi).
	 *
	 * <p>Dipakai langsung oleh {@link #checkCreateDefault()} dan oleh tombol "Tambah" pada layar
	 * {@code KelompokParameterTambahanPengajuanAction.onAdd(Event)}.</p>
	 */
	public KelompokParameterTambahanPengajuan() {
	}

	/**
	 * Mengembalikan primary key baris kategori.
	 *
	 * <p>Kolom {@code id} bersifat {@code insertable = false} &mdash; nilainya dihasilkan sepenuhnya
	 * oleh sequence/identity PostgreSQL saat {@code INSERT}, jadi menyetelnya sebelum simpan tidak
	 * berpengaruh.</p>
	 *
	 * <p>Nilai ini ikut membentuk kunci komposit isian permohonan: {@code PengajuanMahasiswaAction}
	 * dan {@code PengajuanSiswaAction} menyusun string {@code "<idKelompok>-><idParameter>"} untuk
	 * mencocokkan jawaban yang tersimpan di kolom teks {@code parameterTambahanInds}. Konsekuensinya
	 * <b>menghapus lalu membuat ulang</b> sebuah kategori akan memutus seluruh jawaban historis yang
	 * merujuk id lama, karena pencocokannya berbasis id.</p>
	 *
	 * @return primary key, atau {@code null} untuk objek yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; dipakai jalur load/binding, bukan kode bisnis.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kategori/seksi, sudah di-{@code trim()}.
	 *
	 * <p>Nilai ini yang tercetak sebagai label tebal pemisah seksi pada formulir permohonan
	 * ({@code ParameterTambahanPengajuanListener}) dan sebagai teks item pada combobox kategori di
	 * layar "Parameter Tambahan Pengajuan".</p>
	 *
	 * <p><b>Catatan:</b> {@code trim()} hanya dilakukan saat <i>membaca</i>;
	 * {@link #setNama(String)} menyimpan apa adanya. Jadi nilai di DB bisa mengandung spasi tepi
	 * yang tidak terlihat di UI &mdash; hal ini relevan karena pemeriksaan duplikat nama di
	 * {@code KelompokParameterTambahanPengajuanAction.checkNamaKelompokParameterTambahanPengajuan()}
	 * memakai {@code Restrictions.eq("nama", ...)} (perbandingan persis atas kolom mentah, bukan
	 * hasil {@code trim()}), sehingga {@code "Data Diri"} dan {@code "Data Diri "} dianggap dua nama
	 * berbeda dan lolos validasi keunikan.</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori. Tanpa validasi dan tanpa {@code trim()}.
	 *
	 * <p>Validasi "wajib diisi" dan "belum terdaftar" dilakukan di lapisan UI
	 * ({@code KelompokParameterTambahanPengajuanAction.onSave(Event)}), bukan di sini; jalur
	 * non-UI (impor, {@link #checkCreateDefault()}) bisa menulis nilai apa pun.</p>
	 *
	 * @param nama nama kategori baru; kolomnya {@code NOT NULL} di DB sehingga {@code null} akan
	 *             ditolak database saat flush
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kategori, <b>mentah</b>.
	 *
	 * <p><b>Perhatian &mdash; override ini MEMBALIK kontrak base class.</b>
	 * {@link ais.database.model.GeneralValueObject#getKeterangan()} menormalkan {@code null} menjadi
	 * string kosong dan secara eksplisit menjanjikan hasil non-null. Override di sini mengembalikan
	 * isi field apa adanya, sehingga hasilnya <b>bisa {@code null}</b> (kolomnya memang
	 * {@code nullable = true}).</p>
	 *
	 * <p>Dampak nyata yang sudah teramati: renderer grid
	 * {@code KelompokParameterTambahanPengajuanAction.KelompokParameterTambahanPengajuanRenderer}
	 * membuat {@code new Label(getKeterangan())} tanpa penjagaan {@code null}. ZK menerima
	 * {@code null} sebagai label kosong sehingga tidak ada NPE di jalur itu, tetapi pemanggil lain
	 * yang mengasumsikan kontrak base class (mis. merangkai string atau memanggil
	 * {@code .trim()}/{@code .isEmpty()}) akan pecah. Selalu periksa {@code null} sendiri.</p>
	 *
	 * @return keterangan kategori, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas kategori. Tanpa validasi; {@code null} diterima dan akan terbaca
	 * kembali sebagai {@code null} (bukan {@code ""}) &mdash; lihat {@link #getKeterangan()}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "kategori bawaan sistem", dengan default {@code false}.
	 *
	 * <p>Dipakai dua tempat: sebagai kriteria pencarian di {@link #checkCreateDefault()}
	 * ({@code defaultData = true}), dan di renderer grid untuk <b>menyembunyikan tombol Hapus</b>
	 * ({@code setVisible(delete && !getDefaultData())}) sehingga kategori bawaan tidak bisa dihapus
	 * lewat UI. Kategori bawaan tetap bisa di-<i>ubah</i> namanya, dan kalau namanya diubah ia tetap
	 * berperan sebagai kategori bawaan karena pencocokannya lewat bendera ini, bukan nama.</p>
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method ini <b>menulis</b> {@code false}
	 * ke field sebelum mengembalikannya. Pada instance yang masih ter-{@code attach} ke session
	 * Hibernate, hal ini membuat objek menjadi <i>dirty</i> dan memicu {@code UPDATE} pada flush
	 * berikutnya &mdash; lengkap dengan revisi Envers baru yang mencatat pengguna yang kebetulan
	 * sedang membuka layar. Perilaku ini konsisten di seluruh keluarga
	 * {@code KelompokParameterTambahan*}.</p>
	 *
	 * @return {@code true} bila baris ini kategori bawaan sistem; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda "kategori bawaan sistem". Tanpa validasi.
	 *
	 * <p>Di codebase hanya {@link #checkCreateDefault()} yang menyetelnya ke {@code true}; tidak ada
	 * layar yang mengeksposnya ke admin, jadi bendera ini praktis hanya bisa berubah lewat
	 * intervensi DB langsung.</p>
	 *
	 * @param defaultData nilai penanda baru
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan bendera aktif, dengan default {@code true}.
	 *
	 * <p>Kategori non-aktif disaring saat formulir permohonan dirakit: query di
	 * {@code ais.action.master.helper.ParameterTambahanPengajuanListener} menambahkan
	 * {@code Restrictions.eq("kelompokParameterTambahanPengajuan.aktif", true)}, sehingga seluruh
	 * field di bawah kategori itu berhenti muncul di form Tambah/Ubah permohonan. Data lama
	 * <b>tidak</b> dihapus &mdash; hanya berhenti ditampilkan.</p>
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method ini <b>menulis</b> {@code true}
	 * ke field sebelum mengembalikannya (lihat catatan yang sama pada {@link #getDefaultData()}).
	 * Karena {@link #checkCreateDefault()} tidak pernah menyetel kolom ini, kategori bawaan hasil
	 * auto-seed selalu melewati jalur penulisan default ini pada pembacaan pertamanya.</p>
	 *
	 * <p><b>Default "menyala"</b> ini adalah kebalikan asimetri "aman secara bawaan" yang tercatat
	 * pada varian {@code KelompokParameterTambahanCalonMahasiswa}: kategori baru di sini langsung
	 * ikut tampil begitu dicentang di layar "Jenis Pengajuan".</p>
	 *
	 * @return {@code true} bila kategori aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel bendera aktif. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" pada renderer grid layar master,
	 * yang langsung menyusulkan {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan
	 * seketika tanpa tombol Simpan. Checkbox tersebut <b>dijaga</b> hak akses
	 * ({@code setDisabled(!edit)} dengan {@code edit = checkPrevilages(UPDATE)}).</p>
	 *
	 * @param aktif nilai bendera baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil seksi, dengan default {@code 1}.
	 *
	 * <p>Ini satu-satunya kunci pengurutan yang dipakai {@link #compareTo(GeneralValueObject)}, dan
	 * kunci pertama pada {@code @OrderBy("nomorUrut asc, nama asc")} milik relasi
	 * {@code JenisPengajuan.getKelompokParameterTambahanPengajuans()}.</p>
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method ini <b>menulis</b> {@code 1} ke
	 * field sebelum mengembalikannya &mdash; sekali lagi berpotensi memicu {@code UPDATE} dan revisi
	 * Envers dari sekadar operasi baca.</p>
	 *
	 * <p><b>Kuirk:</b> penjagaan {@code null} dilakukan <b>dua kali</b>. Blok {@code if} di atas
	 * sudah menjamin field tidak {@code null}, sehingga ekspresi ternary
	 * {@code nomorUrut == null ? 1 : nomorUrut} pada baris {@code return} adalah kode mati yang
	 * cabang {@code null}-nya tidak akan pernah terpilih. Tidak berbahaya, hanya sisa penambalan
	 * berlapis.</p>
	 *
	 * <p><b>Realitas pemakaian:</b> dialog Tambah/Ubah kategori hanya menyediakan isian "Nama
	 * Kelompok" dan "Keterangan" &mdash; tidak ada isian nomor urut. Satu-satunya cara mengisinya
	 * adalah {@code Intbox} pada grid daftar. Pada instalasi yang admin-nya belum pernah menyentuh
	 * kolom itu, <b>seluruh</b> kategori bernilai {@code 1}; lihat akibatnya di
	 * {@link #compareTo(GeneralValueObject)}.</p>
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
	 * Menyetel nomor urut tampil seksi. Tanpa validasi (nilai negatif maupun duplikat diterima).
	 *
	 * <p>Dipanggil dari listener {@code onChange} milik {@code Intbox} pada renderer grid layar
	 * master, yang langsung menyusulkan {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan
	 * tersimpan seketika.</p>
	 *
	 * <p><b>Catatan keamanan:</b> berbeda dari checkbox "Aktif" di baris yang sama, {@code Intbox}
	 * itu <b>tidak</b> diberi penjagaan hak akses apa pun (tidak ada {@code setDisabled(!edit)}
	 * maupun {@code setReadonly}). Pengguna yang hanya punya hak READ atas layar ini tetap bisa
	 * mengubah dan menyimpan urutan seksi formulir permohonan untuk seluruh pengguna. Pola yang sama
	 * juga ditemukan pada beberapa varian lain keluarga {@code KelompokParameterTambahan*}.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} diterima dan akan terbaca kembali sebagai
	 *                  {@code 1} lewat {@link #getNomorUrut()}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan dua kategori <b>semata-mata</b> berdasarkan {@link #getNomorUrut()}.
	 *
	 * <p><b>Bentuk yang DIPANGKAS.</b> Berbeda dari
	 * {@link ais.database.model.KelompokParameterTambahanAlumni#compareTo(GeneralValueObject)} yang
	 * memakai bentuk lengkap (cek {@code instanceof} lebih dulu, lalu rantai fallback
	 * {@code nim} &rarr; {@code nama} &rarr; {@code keterangan} di dalam {@code try}/{@code catch}
	 * dan {@code return 0} sebagai jaring pengaman), implementasi di sini hanya satu baris tanpa
	 * pemeriksaan tipe dan tanpa fallback apa pun.</p>
	 *
	 * <p><b>Konsekuensi 1 &mdash; {@code ClassCastException}.</b> Parameternya bertipe
	 * {@link GeneralValueObject}, tetapi langsung di-<i>cast</i> ke
	 * {@code KelompokParameterTambahanPengajuan}. Memasukkan entity ini ke dalam koleksi terurut
	 * yang bercampur tipe lain (atau memanggil {@code compareTo} dengan argumen tipe lain) akan
	 * melempar {@code ClassCastException}, bukan menghasilkan urutan sembarang. Pada praktiknya
	 * seluruh pemakaian di codebase homogen ({@code TreeSet} yang isinya hanya kategori pengajuan),
	 * jadi risikonya laten.</p>
	 *
	 * <p><b>Konsekuensi 2 &mdash; penciutan senyap di {@code TreeSet}.</b> Karena
	 * {@code compareTo} mengembalikan {@code 0} untuk dua kategori ber-{@code nomorUrut} sama
	 * (padahal {@code equals()} warisan base class membandingkan {@code id}), {@code TreeSet}
	 * memperlakukan keduanya sebagai <b>satu elemen</b> dan hanya menyimpan yang pertama masuk.
	 * Ini bukan kasus langka: {@code nomorUrut} tidak pernah diisi dialog Tambah/Ubah, sehingga
	 * kondisi <b>bawaan</b> adalah semua kategori bernilai {@code 1}. Titik terdampak yang sudah
	 * dipastikan:</p>
	 * <ul>
	 *   <li>{@code ais.action.master.PengajuanMahasiswaAction} &mdash; membangun
	 *   {@code new TreeSet<KelompokParameterTambahanPengajuan>()} dari koleksi milik
	 *   {@code JenisPengajuan} sebelum menyerahkannya ke
	 *   {@code ParameterTambahanPengajuanListener};</li>
	 *   <li>{@code ais.action.master.sekolah.PengajuanSiswaAction} &mdash; salinan kata-per-kata
	 *   dari yang di atas.</li>
	 * </ul>
	 * <p>Pada kedua layar itu, sebuah jenis permohonan dengan beberapa kategori aktif hanya akan
	 * menampilkan <b>satu</b> seksi form; seksi lain lenyap tanpa error apa pun. Perhatikan bahwa
	 * relasi {@code @ManyToMany}-nya sendiri memakai {@code @OrderBy} (koleksi Hibernate berbasis
	 * urutan SQL, bukan {@code TreeSet}) sehingga penciutan ini <b>khusus</b> terjadi pada dua
	 * penyalinan manual di atas, bukan pada relasi entity-nya.</p>
	 *
	 * <p>Karena {@link #getNomorUrut()} bersifat menulis default, memanggil {@code compareTo} pada
	 * dua instance ter-{@code attach} yang {@code nomorUrut}-nya masih {@code null} dapat
	 * mengotori keduanya sekaligus &mdash; mengurutkan daftar pun bisa menghasilkan {@code UPDATE}.
	 * </p>
	 *
	 * @param arg0 kategori pembanding; <b>harus</b> instance
	 *             {@code KelompokParameterTambahanPengajuan}, jika tidak akan melempar
	 *             {@code ClassCastException}. {@code null} akan melempar
	 *             {@code NullPointerException}.
	 * @return bilangan negatif/nol/positif sesuai perbandingan {@code nomorUrut} kedua kategori
	 * @throws ClassCastException bila {@code arg0} bukan {@code KelompokParameterTambahanPengajuan}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanPengajuan) arg0).getNomorUrut());
	}
}
