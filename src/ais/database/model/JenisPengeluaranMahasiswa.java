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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;

/**
 * Entity <b>master jenis pengeluaran mahasiswa</b> — tabel
 * {@code public.jenis_pengeluaran_mahasiswa}, {@code @Audited} (Hibernate Envers),
 * {@code dynamicInsert}/{@code dynamicUpdate}. Turunan langsung
 * {@link ais.database.model.GeneralValueObject}.
 *
 * <h3>Peran dalam alur keuangan mahasiswa</h3>
 * <p>Satu baris entity ini adalah <b>satu kategori pengeluaran uang dari kas mahasiswa</b> —
 * yaitu perpindahan uang KELUAR dari saldo/tabungan mahasiswa, kebalikan dari pembayaran
 * (uang masuk). Contoh baku bawaan sistem adalah <i>"Ambil Uang Tunai"</i> (kode {@code 001},
 * lihat {@link #reloadDefault()}), yang mewakili penarikan tunai oleh mahasiswa di loket.</p>
 *
 * <p>Entity ini <b>tidak menyimpan nominal apa pun</b>. Nominal, mahasiswa, tanggal, dan cara
 * pembayarannya ada di entity transaksi {@link PengeluaranMahasiswa}; baris master ini hanya
 * menjadi <i>label</i> sekaligus <i>pemetaan akun akuntansi</i> bagi transaksi tersebut, lewat
 * relasi {@code ManyToOne} {@link PengeluaranMahasiswa#getJenisPengeluaranMahasiswa()}.</p>
 *
 * <h3>Konsumen</h3>
 * <ul>
 *   <li><b>Layar master</b> {@code /pages/master/jenis_pengeluaran_mahasiswa.zul} —
 *       {@code ais.action.master.JenisPengeluaranMahasiswaAction} (CRUD: kode, nama, keterangan,
 *       akun, checkbox "Aktif" dan "Default"). Layar yang sama juga disematkan sebagai tab di
 *       dalam layar {@code pengeluaran_mahasiswa.zul}.</li>
 *   <li><b>Layar transaksi</b> {@code /pages/master/pengeluaran_mahasiswa.zul} —
 *       {@code ais.action.master.PengeluaranMahasiswaAction} menampilkan seluruh baris entity ini
 *       sebagai {@code Combobox} pilihan jenis pengeluaran (wajib dipilih saat menyimpan).</li>
 *   <li><b>Layar posting jurnal</b> {@code /pages/master/posting_pengeluaran_mahasiswa.zul} —
 *       {@code ais.action.master.PostingPengeluaranMahasiswaAction} memakai {@link #getAkun()}
 *       sebagai <b>akun KREDIT</b> jurnal (akun debet diambil dari
 *       {@code JenisPembayaran.getAkun()}) dan memfilter kombo pilihan dengan
 *       {@code Restrictions.eq("aktif", true)}.</li>
 *   <li><b>Bootstrap aplikasi</b> {@code ais.common.InitData} — kelas ini masuk daftar
 *       {@code initClasses(...)} (pembuatan/penyelarasan tabel lewat {@code InitDataHelper}) dan
 *       {@code reloadDefaults()} memanggil {@link #reloadDefault()} di thread executor saat
 *       start-up.</li>
 *   <li><b>UI baru (JSP)</b> {@code /WEB-INF/new/root/services/jenis_pengeluaran_mahasiswa_service.jsp}
 *       — hanya scaffold metadata yang meneruskan ke {@code _shared/services/dispatcher.jsp};
 *       dispatcher itu <i>sudah</i> memasang {@code NewUiRouteGuard.isActionAuthorized(...)} dan
 *       pemeriksaan token CSRF.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; kode</b>: {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *       {@link #getKeterangan()}.</li>
 *   <li><b>Flag perilaku</b>: {@link #getAktif()} (muncul/tidak di pencarian &amp; kombo posting),
 *       {@link #getDefaultPengeluaran()} (baris yang dipakai sebagai nilai bawaan).</li>
 *   <li><b>Pemetaan akuntansi</b>: {@link #getAkun()} — satu-satunya relasi keluar.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Utilitas statis</b>: {@link #DEFAULT_JENIS_PENGELUARAN} dan {@link #reloadDefault()}.</li>
 * </ul>
 *
 * <h3>Mengapa field {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} dideklarasikan ulang</h3>
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan
 * properti milik induk. Karena itu setiap entity turunan <b>wajib</b> mendeklarasikan ulang
 * kolom identitas dan kolom audit di kelasnya sendiri. Pengulangan di bawah adalah keharusan
 * teknis pemetaan, bukan duplikasi yang tertinggal.</p>
 *
 * <h3>Hal yang mengejutkan / perlu diketahui</h3>
 * <ul>
 *   <li><b>Tidak ada yang menjamin "default" itu tunggal.</b> Tidak ada {@code unique constraint}
 *       maupun logika yang mematikan flag {@code defaultPengeluaran} baris lain ketika satu baris
 *       dicentang "Default" di layar master. Bila dua baris tercentang, {@link #reloadDefault()}
 *       memungut salah satunya secara sembarang ({@code setMaxResults(1)} tanpa {@code order by}).</li>
 *   <li><b>Menghilangkan default akan MEMBUAT baris master baru.</b> Bila tidak ada satu pun baris
 *       ber-{@code defaultPengeluaran = true}, {@link #reloadDefault()} langsung meng-{@code INSERT}
 *       baris "001 — Ambil Uang Tunai". Karena layar master memanggil {@link #reloadDefault()}
 *       sesudah setiap simpan/hapus/centang, membatalkan centang "Default" pada satu-satunya baris
 *       default akan diam-diam menambah baris master baru — dan kode {@code "001"} tidak dijaga
 *       unik, sehingga bisa bentrok dengan baris yang sudah ada.</li>
 *   <li><b>{@link #DEFAULT_JENIS_PENGELUARAN} adalah state statis yang dibagi seluruh JVM</b> dan
 *       berstatus <i>detached</i> (session-nya ditutup di akhir {@link #reloadDefault()}). Baris
 *       hasil seed juga tidak mengisi {@link #getOleh()}/{@link #getOlehId()}, sehingga jejak
 *       auditnya kosong.</li>
 *   <li><b>Efek tersembunyi pada entity anak.</b> {@link PengeluaranMahasiswa#getJenisPengeluaranMahasiswa()}
 *       menulis {@link #DEFAULT_JENIS_PENGELUARAN} ke field-nya sendiri saat masih {@code null}
 *       (dan cabang itu <i>tidak</i> melewati {@code check(...)}). Karena
 *       {@link PengeluaranMahasiswa} juga {@code dynamicUpdate}, sekadar membaca transaksi lama
 *       yang jenisnya kosong lalu menyimpannya dapat mem-persist FK ke baris default.</li>
 *   <li><b>Kontrol akses layar master timpang.</b> Tombol ubah dan hapus dijaga
 *       {@code CommonPrivilages.UPDATE}/{@code DELETE}, tetapi checkbox "Aktif" dan "Default" pada
 *       tiap baris grid <b>tidak dijaga hak akses apa pun</b> — lihat catatan pada
 *       {@link #setAktif(Boolean)} dan {@link #setDefaultPengeluaran(Boolean)}.</li>
 *   <li><b>Komentar Javadoc asal hbm2java salah.</b> Berkas ini semula berkomentar
 *       {@code "Bank generated by hbm2java"} — sisa salin-tempel dari entity {@code Bank},
 *       tidak ada hubungannya dengan bank.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see PengeluaranMahasiswa
 * @see ais.database.model.akunting.Akun
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_pengeluaran_mahasiswa")
public class JenisPengeluaranMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan sejak berkas dibuat; jangan diubah karena instance
	 * entity ikut tersimpan pada session ZK/HTTP yang bisa di-serialize.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Primary key tabel {@code jenis_pengeluaran_mahasiswa}. Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate — lihat catatan kelas.
	 */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini; diisi otomatis oleh lapisan audit. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini; diisi otomatis oleh lapisan audit. */
	private String olehId;
	/**
	 * Penanda baris "bawaan". Hanya boleh {@code true} pada satu baris, tetapi keunikan itu tidak
	 * ditegakkan di manapun — lihat {@link #reloadDefault()}.
	 */
	private Boolean defaultPengeluaran;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah disimpan lewat jalur ber-audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau string kosong/spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa menyentuh field). Jadi jejak audit lama tidak pernah
	 * bisa dihapus lewat setter ini; nilai lama tetap bertahan.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Perhatikan:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong/spasi diabaikan diam-diam sehingga nilai lama tidak tertimpa.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat jejak audit (pengguna dan waktu) tepat sebelum
	 * baris ini di-{@code UPDATE}, dengan mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Dipanggil oleh Hibernate, bukan oleh kode aplikasi. Tidak berjalan pada {@code INSERT}
	 * pertama maupun pada operasi SQL native/bulk-update.</p>
	 *
	 * <p>Pada baris yang sama juga dideklarasikan field {@code tanggal_dirubah}, yang diinisialisasi
	 * ke waktu server saat objek dibentuk ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga entity
	 * baru selalu punya stempel waktu meski belum pernah di-update.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (kolom {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibentuk
	 *         karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Perhatikan:</b> memakai <b>field mentah</b> {@code nama}, bukan {@link #getNama()},
	 * sehingga nilainya tidak dipangkas dan bisa berisi {@code "null-null"} untuk objek baru.</p>
	 *
	 * @return gabungan id dan nama, dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Cache statis baris jenis pengeluaran yang ditandai sebagai bawaan
	 * ({@code defaultPengeluaran = true}).
	 *
	 * <p><b>State global lintas pengguna.</b> Field ini dibagi seluruh JVM dan diisi/diganti oleh
	 * {@link #reloadDefault()}. Instance yang tersimpan di sini <b>detached</b> — session Hibernate
	 * yang memuatnya sudah ditutup — sehingga relasi {@code LAZY} {@link #getAkun()} hanya aman
	 * diakses berkat {@code check(...)} milik {@link GeneralValueObject} yang menyelesaikan ulang
	 * proxy pada session aktif.</p>
	 *
	 * <p>Pembaca satu-satunya adalah {@link PengeluaranMahasiswa#getJenisPengeluaranMahasiswa()},
	 * yang memakainya sebagai nilai jatuh-tempo saat transaksi belum punya jenis pengeluaran.</p>
	 */
	public static JenisPengeluaranMahasiswa DEFAULT_JENIS_PENGELUARAN = null;

	/**
	 * Memuat ulang {@link #DEFAULT_JENIS_PENGELUARAN} dari database, dan <b>membuat baris master
	 * baru bila belum ada satu pun yang ditandai default</b>.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Mengambil session native thread ({@code HibernateUtil.currentNativeSession()}).</li>
	 *   <li>Mencari satu baris dengan {@code defaultPengeluaran = true}
	 *       ({@code setMaxResults(1)}, tanpa {@code order by}).</li>
	 *   <li>Bila tidak ada: membentuk objek baru berisi kode {@code "001"}, nama dan keterangan
	 *       {@code "Ambil Uang Tunai"}, {@code aktif = true}, {@code defaultPengeluaran = true},
	 *       lalu <b>menyimpannya</b> dalam transaksi eksplisit
	 *       ({@code begin()} &rarr; {@code save()} &rarr; {@code commit()}).</li>
	 *   <li>Menutup session native thread ({@code HibernateUtil.closeSession()}).</li>
	 * </ol>
	 *
	 * <h4>Efek samping</h4>
	 * <ul>
	 *   <li><b>Menulis ke database</b> (auto-seed). Ini bukan method baca murni: nama
	 *       "reloadDefault" menyembunyikan kemungkinan {@code INSERT} baris master baru.</li>
	 *   <li><b>Mengganti state statis</b> {@link #DEFAULT_JENIS_PENGELUARAN} untuk seluruh JVM.</li>
	 *   <li><b>Menutup session native milik thread.</b> {@code closeSession()} melakukan
	 *       {@code clear + rollback + disconnect + close}. Sesuai cookbook
	 *       {@link ais.database.hibernate.HibernateUtil}, penutupan itu seharusnya dilakukan oleh
	 *       titik-masuk thread yang <i>pertama</i> membuka session, bukan oleh tiap pemanggil.
	 *       Bila method ini dipanggil dari tengah alur lain yang sudah memakai
	 *       {@code currentNativeSession()} (mis. jalur JSP di bawah {@code FilterJSP}), session
	 *       bersama itu ikut tertutup dan perubahan yang belum ter-flush bisa hilang. Pada alur ZK
	 *       (layar master) hal ini tidak terasa karena session ZK adalah session yang berbeda.</li>
	 *   <li>Bila session native thread sudah punya transaksi aktif, {@code getTransaction().begin()}
	 *       akan melempar exception.</li>
	 * </ul>
	 *
	 * <h4>Dipanggil dari</h4>
	 * <ul>
	 *   <li>{@code ais.common.InitData.reloadDefaults()} — sekali saat start-up, di thread executor.</li>
	 *   <li>{@code JenisPengeluaranMahasiswaAction}: sesudah {@code onSave(...)}, sesudah baris
	 *       dihapus, dan sesudah checkbox "Default" dicentang/dibatalkan pada grid.</li>
	 * </ul>
	 *
	 * <p><b>Perhatikan:</b> karena method ini tidak pernah mematikan flag default baris lain,
	 * membatalkan centang "Default" pada satu-satunya baris default akan langsung memicu
	 * pembuatan baris "001 — Ambil Uang Tunai" yang baru pada pemanggilan berikutnya.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		DEFAULT_JENIS_PENGELUARAN = (JenisPengeluaranMahasiswa) session.createCriteria(JenisPengeluaranMahasiswa.class)
				.add(Restrictions.eq("defaultPengeluaran", true)).setMaxResults(1).uniqueResult();
		if (DEFAULT_JENIS_PENGELUARAN == null) {
			DEFAULT_JENIS_PENGELUARAN = new JenisPengeluaranMahasiswa();
			DEFAULT_JENIS_PENGELUARAN.setKode("001");
			DEFAULT_JENIS_PENGELUARAN.setAktif(true);
			DEFAULT_JENIS_PENGELUARAN.setDefaultPengeluaran(true);
			DEFAULT_JENIS_PENGELUARAN.setNama("Ambil Uang Tunai");
			DEFAULT_JENIS_PENGELUARAN.setKeterangan("Ambil Uang Tunai");
			session.getTransaction().begin();
			session.save(DEFAULT_JENIS_PENGELUARAN);
			session.getTransaction().commit();
		}
		HibernateUtil.closeSession();
	}

	/**
	 * Kode singkat jenis pengeluaran (mis. {@code "001"}). Dipakai sebagai kunci pencarian dan
	 * urutan tampil di layar master; keunikannya hanya divalidasi di layar
	 * ({@code JenisPengeluaranMahasiswaAction.checkKode()}), tidak di database.
	 */
	private String kode;
	/** Akun buku besar yang menjadi sisi KREDIT jurnal saat pengeluaran diposting. */
	private Akun akun;
	/** Nama jenis pengeluaran; kolom wajib ({@code nullable = false}). */
	private String nama;
	/** Keterangan bebas; boleh kosong. */
	private String keterangan;
	/**
	 * Penanda aktif. Baris tidak aktif disembunyikan dari pencarian baku layar master dan dari kombo
	 * pilihan pada layar posting.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate. Seluruh field dibiarkan {@code null}
	 * kecuali {@code tanggal_dirubah}, yang diisi waktu server saat objek dibentuk.
	 */
	public JenisPengeluaranMahasiswa() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code GenerationType.IDENTITY}).</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key baris ini. Dipakai Hibernate; kode aplikasi sebaiknya tidak memanggilnya.
	 *
	 * @param id nilai primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode jenis pengeluaran dalam bentuk yang aman untuk ditampilkan.
	 *
	 * <p><b>Perhatikan:</b> mengembalikan string kosong bila field {@code null}, dan memangkas spasi
	 * — tetapi <b>tidak menulis balik</b> ke field, sehingga tidak ada risiko {@code UPDATE} tak
	 * disengaja dari sekadar membaca. Kolom ini tidak diberi {@code @Column}, jadi nama kolomnya
	 * ditentukan {@code MyNamingStrategy}.</p>
	 *
	 * @return kode yang sudah dipangkas, atau {@code ""} bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode jenis pengeluaran.
	 *
	 * <p>Nilai disimpan apa adanya (tanpa {@code trim}); pemangkasan hanya terjadi saat dibaca lewat
	 * {@link #getKode()}. Duplikasi kode hanya dicegah di layar master, bukan oleh constraint.</p>
	 *
	 * @param kode kode baru; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis pengeluaran, dipangkas spasinya.
	 *
	 * <p>Berbeda dengan {@link #getKode()}, {@code null} <b>tidak</b> diubah menjadi {@code ""}.
	 * Tidak ada penulisan balik ke field.</p>
	 *
	 * @return nama yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis pengeluaran.
	 *
	 * <p>Kolom bersifat {@code nullable = false} di database; layar master menolak nama kosong dan
	 * nama yang sudah terpakai ({@code checkNama()}), tetapi tidak ada constraint unik di DB.</p>
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas jenis pengeluaran.
	 *
	 * @return keterangan apa adanya (tanpa pemangkasan), atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas jenis pengeluaran.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris ini.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} diperlakukan sebagai {@code true} — baris lama yang
	 * kolomnya belum pernah diisi tetap dianggap aktif. Pencarian di layar master mengikuti aturan
	 * yang sama ({@code isNull("aktif") OR eq("aktif", true)}). Getter ini tidak menulis balik ke
	 * field, jadi nilai {@code null} di database tetap {@code null}.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} bila sengaja
	 *         dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif baris ini.
	 *
	 * <p>Menonaktifkan baris menyembunyikannya dari pencarian baku layar master dan dari kombo
	 * pilihan layar posting pengeluaran ({@code Restrictions.eq("aktif", true)}), tetapi
	 * <b>tidak</b> memutus transaksi {@link PengeluaranMahasiswa} lama yang sudah terlanjur
	 * memakainya.</p>
	 *
	 * <p><b>Catatan kontrol akses:</b> pada grid layar master, checkbox "Aktif" langsung menyimpan
	 * perubahan ({@code Common.refreshSaveOrUpdate(...)}) <b>tanpa memeriksa hak
	 * {@code CommonPrivilages.UPDATE}</b> — berbeda dengan tombol "Ubah Data" di baris yang sama
	 * yang dijaga hak tersebut.</p>
	 *
	 * @param aktif status aktif baru; {@code null} akan dibaca kembali sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda "baris bawaan".
	 *
	 * <p>Nilai {@code null} diperlakukan sebagai {@code false}. Getter ini tidak menulis balik ke
	 * field.</p>
	 *
	 * @return {@code true} bila baris ini yang dipakai {@link #reloadDefault()} sebagai
	 *         {@link #DEFAULT_JENIS_PENGELUARAN}
	 */
	public Boolean getDefaultPengeluaran() {
		return defaultPengeluaran == null ? false : defaultPengeluaran;
	}

	/**
	 * Menandai atau membatalkan baris ini sebagai jenis pengeluaran bawaan.
	 *
	 * <p><b>Tidak ada logika "hanya satu yang boleh default".</b> Menandai baris ini tidak
	 * membatalkan penandaan baris lain, dan membatalkan penandaan baris terakhir yang default akan
	 * membuat {@link #reloadDefault()} menyisipkan baris master "001 — Ambil Uang Tunai" yang
	 * baru.</p>
	 *
	 * <p><b>Catatan kontrol akses:</b> sama seperti {@link #setAktif(Boolean)}, checkbox "Default"
	 * pada grid layar master menyimpan dan langsung memanggil {@link #reloadDefault()}
	 * <b>tanpa pemeriksaan hak {@code CommonPrivilages.UPDATE}</b>, padahal dampaknya berskala
	 * institusi (mengubah nilai bawaan seluruh transaksi pengeluaran mahasiswa berikutnya).</p>
	 *
	 * @param defaultPengeluaran {@code true} untuk menjadikan baris ini bawaan
	 */
	public void setDefaultPengeluaran(Boolean defaultPengeluaran) {
		this.defaultPengeluaran = defaultPengeluaran;
	}

	/**
	 * Mengembalikan akun buku besar yang dipetakan ke jenis pengeluaran ini.
	 *
	 * <p><b>Pola penulisan balik.</b> Getter ini <b>menugaskan ulang field</b>
	 * ({@code akun = check(akun)}). {@code check(...)} milik {@link GeneralValueObject} mengganti
	 * proxy {@code LAZY} yang sudah tidak bisa diinisialisasi (session tertutup, objek detached)
	 * dengan instance kanonik yang valid. Yang berubah hanyalah <i>identitas objek Java</i>, bukan
	 * nilai foreign key, sehingga meski entity ini {@code dynamicUpdate}, pembacaan biasa tidak
	 * menghasilkan {@code UPDATE}. Justru pola inilah yang membuat
	 * {@link #DEFAULT_JENIS_PENGELUARAN} (yang selalu detached) aman dibaca akunnya.</p>
	 *
	 * <p><b>Pemakaian:</b> {@code PostingPengeluaranMahasiswaAction} memakai nilai ini sebagai akun
	 * KREDIT jurnal pengeluaran. Kolom boleh {@code null}; layar master hanya mewajibkannya bila
	 * konfigurasi {@code integrasi_modul_akuntansi} aktif. Bila {@code null}, baris transaksi
	 * ditampilkan sebagai "pemetaan akun tidak valid" dan dilewati saat posting massal.</p>
	 *
	 * @return akun kredit yang dipetakan, atau {@code null} bila belum dipetakan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Memetakan jenis pengeluaran ini ke sebuah akun buku besar.
	 *
	 * <p>Relasi memakai {@code cascade = PERSIST, MERGE}, sehingga menyimpan baris ini dapat ikut
	 * menyimpan/menggabungkan objek {@link Akun} yang diberikan.</p>
	 *
	 * @param akun akun kredit; boleh {@code null} bila integrasi akuntansi tidak dipakai
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}
}
