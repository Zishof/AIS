package ais.database.model.akunting;

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
 * Katalog <b>Grup Akun</b> &mdash; daftar nama kelompok yang dipakai untuk menggolongkan
 * akun buku besar ({@link ais.database.model.akunting.Akun}) menjadi rombongan yang lebih
 * besar, misalnya "Aktiva Lancar", "Kewajiban Jangka Panjang", atau "Pendapatan Usaha".
 * Satu baris tabel {@code akunting.grup_akun} = satu nama kelompok, tidak lebih.
 *
 * <h3>Isi entity ini &mdash; apa adanya (TERVERIFIKASI dari kode)</h3>
 * <p>Seluruh isi kelas ini hanya <b>dua kolom bermakna</b> ditambah tiga kolom jejak audit:</p>
 * <ul>
 *   <li>{@link #getNama()} &mdash; nama kelompok, wajib diisi ({@code nullable = false},
 *   panjang 255);</li>
 *   <li>{@link #getKeterangan()} &mdash; catatan bebas, boleh kosong;</li>
 *   <li>{@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()} &mdash;
 *   jejak audit yang diisi otomatis interceptor, bukan data akuntansi.</li>
 * </ul>
 * <p><b>Koreksi penting terhadap dugaan yang wajar muncul.</b> Kelas ini <b>TIDAK</b>
 * memiliki kolom kategori laporan keuangan (tidak ada enum/kode Aset&ndash;Kewajiban&ndash;
 * Ekuitas&ndash;Pendapatan&ndash;Beban), <b>TIDAK</b> memiliki kolom posisi normal debet/kredit,
 * <b>TIDAK</b> memiliki nomor urut penyajian, dan <b>TIDAK</b> memiliki kolom tenant
 * (sekolah/yayasan/satuan kerja). Nama-nama yang terdengar seperti kategori standar akuntansi
 * ("Aktiva", "Kewajiban", "Modal") hanyalah <b>teks bebas yang diketik operator</b> pada layar
 * master; sistem tidak pernah memaknainya. Klasifikasi akuntansi yang sesungguhnya dipegang
 * kolom lain di {@code Akun}: posisi normal ada di {@code Akun.getDebetCredit()} dan hierarki
 * penyajian ada di {@code Akun.getParent()}.</p>
 *
 * <h3>Struktur: DATAR, tanpa induk sendiri (TERVERIFIKASI &mdash; penting)</h3>
 * <p>Berbeda dengan {@link ais.database.model.akunting.Akun} yang self-referential
 * (punya {@code parent} sehingga membentuk pohon), <b>{@code GrupAkun} sama sekali tidak
 * punya field induk maupun koleksi anak</b>. Katalog ini <b>satu tingkat</b>. Konsekuensinya
 * langsung dan perlu ditegaskan: pewarisan grup yang terjadi di {@code Akun}
 * <b>tidak dapat diperpanjang dari sisi grup</b> &mdash; sebuah grup tidak bisa "mewarisi"
 * dari grup lain, tidak ada rantai grup, dan tidak ada risiko rekursi yang berasal dari
 * kelas ini. Lihat bagian berikut.</p>
 *
 * <h3>Hubungan dengan {@code Akun}: satu arah, dan sisi ini pasif</h3>
 * <p>Relasi dimiliki sepenuhnya oleh {@code Akun}: {@code Akun.getGrupAkun()} dipetakan
 * {@code @ManyToOne(fetch = LAZY, cascade = {PERSIST, MERGE})} ke kolom {@code grup_akun}.
 * Kelas ini <b>tidak menyimpan koleksi {@code Akun}</b> dan tidak punya jalan balik; untuk
 * mengetahui akun apa saja yang memakai sebuah grup, kode harus mengueri {@code Akun} dengan
 * {@code Restrictions.eq("grupAkun", grup)} &mdash; persis yang dilakukan
 * {@code KodeAkunApiHelper.grupAkunHapus} dan seluruh combobox penyaring laporan.</p>
 *
 * <h3>VERIFIKASI ULANG mekanisme tulis-balik, dilihat dari sisi grup</h3>
 * <p>{@code Akun.getGrupAkun()} adalah <b>getter destruktif</b>: bila akun belum punya grup,
 * getter itu mengambil grup induknya lalu <b>menugaskan objek grup itu ke field milik akun
 * anak</b>, dan karena {@code Akun} memakai akses properti, nilai tersebut ikut tertulis
 * permanen ke kolom {@code grup_akun} akun anak pada flush berikutnya. Ditinjau dari sisi
 * {@code GrupAkun} sendiri, yang perlu dicatat:</p>
 * <ul>
 *   <li><b>Objek yang "ditimpakan" adalah instance grup yang sama persis</b>, bukan salinan.
 *   Satu baris {@code GrupAkun} yang semula dirujuk satu akun induk bisa berakhir dirujuk
 *   puluhan akun keturunannya tanpa seorang pun pernah menekan tombol Simpan.</li>
 *   <li><b>Kelas ini tidak menyumbang satu langkah pun pada rekursi.</b> Rekursi
 *   {@code getParent().getGrupAkun()} berjalan <b>sepenuhnya menaiki pohon {@code Akun}</b>
 *   dan berhenti begitu ada leluhur yang kolom {@code grup_akun}-nya terisi. Karena katalog
 *   ini datar, tidak ada kemungkinan "grup dari grup"; risiko {@code StackOverflowError} yang
 *   didokumentasikan di {@code Akun} murni berasal dari siklus {@code parent} antar akun.</li>
 *   <li><b>Dampak nyata bagi data master ini:</b> begitu warisan membeku, mengganti grup pada
 *   akun induk (dari layar Kode Akun) <b>tidak lagi mengubah akun-akun anak</b> yang telanjur
 *   ikut tertulis. Perpindahan sebuah akun ke grup lain karena itu sering harus dikerjakan
 *   satu per satu, dan penghapusan grup lewat REST bisa mendadak tertolak dengan pesan
 *   "masih dipakai N akun" untuk akun-akun yang tidak pernah sengaja dikaitkan ke grup itu.</li>
 * </ul>
 *
 * <h3>Siapa memakai katalog ini (TERVERIFIKASI, 17 berkas)</h3>
 * <ul>
 *   <li><b>Master &amp; CRUD:</b> {@code ais.action.master.akunting.GrupAkunAction} atas
 *   layar {@code /pages/master/akunting/grupakun.zul} (menu "Setup Grup Akun", lihat
 *   {@code MenuSnapshotData}); persistensinya lewat {@code GrupAkunDao}/{@code GrupAkunDaoImpl}
 *   yang keduanya kosong dan hanya mewarisi CRUD generik.</li>
 *   <li><b>Pemakai relasi:</b> {@code AkunAction} (combobox "Grup Akun" pada form akun, kolom
 *   grup pada grid, dan <b>pembuatan otomatis</b> grup saat impor bagan akun dari spreadsheet),
 *   serta {@code KodeAkunApiHelper} pada jalur REST.</li>
 *   <li><b>Penyaring:</b> {@code AmbilDataAkunBanbox} + {@code AkunTreeModel} (bandbox pemilih
 *   akun), {@code DasboardBukuBesar}, dan lima laporan &mdash; {@code LaporanBukuBesar},
 *   {@code LaporanBukuBesarPerTanggal}, {@code LaporanTrialBalance},
 *   {@code LaporanRiwayatTransaksi}, {@code LaporanJurnalHarianSimple}.</li>
 *   <li><b>Data awal:</b> terdaftar pada {@code InitData.initClasses(...)} sehingga isi
 *   katalog ikut dimuat ke cache {@code GeneralValueObject} saat aplikasi dijalankan.</li>
 * </ul>
 * <p><b>Catatan yang meluruskan klaim lama.</b> Javadoc {@code GrupAkunAction} menyebut grup
 * akun sebagai "dasar penyusunan laporan keuangan seperti neraca dan laporan laba rugi".
 * Penelusuran seluruh repo menunjukkan hal itu <b>berlebihan</b>: tidak satu pun laporan
 * neraca/laba rugi merujuk kelas ini. Di semua pemakai yang benar-benar ada, grup akun hanya
 * berperan sebagai <b>dimensi penyaring</b> (satu {@code Restrictions.eq("akun.grupAkun", ...)}
 * yang berubah menjadi {@code sqlRestriction("true")} bila pengguna memilih "Semua"), bukan
 * sebagai penentu struktur, urutan, ataupun subtotal laporan.</p>
 *
 * <h3>Cakupan tenant: GLOBAL, tanpa penyaring apa pun (TERVERIFIKASI)</h3>
 * <p>Tidak ada kolom {@code sekolah}, {@code yayasan}, maupun {@code satuanKerja} pada tabel
 * ini, dan tidak satu pun pemakainya menambahkan penyaring tenant:
 * {@code GrupAkunAction.onSearchDefault} hanya menyaring dengan {@code ilike("nama", ...)},
 * dan {@code KodeAkunApiHelper.grupAkunDaftar} menjalankan
 * {@code SELECT id, nama, keterangan FROM akunting.grup_akun ORDER BY nama} tanpa klausa
 * {@code WHERE} sama sekali. Ini <b>bukan</b> kasus "fail-open kondisional" (penyaring yang
 * bocor ketika suatu nilai {@code null}) melainkan <b>ketiadaan pemisahan tenant secara
 * struktural</b>, seperti pada {@code Closing}. Pada instalasi multi-yayasan, penamaan grup
 * milik satu tenant terlihat dan dapat diubah/dihapus oleh tenant lain yang berhak atas menu
 * ini, dan uji duplikasi nama pun berlaku lintas tenant &mdash; dua yayasan tidak dapat
 * memakai nama grup yang sama.</p>
 *
 * <h3>Gerbang hak akses</h3>
 * <ul>
 *   <li><b>Jalur ZK &mdash; wajar.</b> {@code GrupAkunAction} memanggil
 *   {@code Common.doCheckSecurity()} di {@code doBeforeCompose}, memeriksa sesi dan
 *   {@code CommonPrivilages.READ} di {@code doAfterCompose}, lalu menyembunyikan tombol
 *   Tambah/Ubah/Hapus menurut {@code CREATE}/{@code UPDATE}/{@code DELETE}. Karena
 *   {@code grupakun.zul} adalah <b>halaman menu tersendiri</b> (bukan tab yang menumpang pada
 *   layar lain), haknya dibaca dari menunya sendiri: <b>pola pewarisan hak lewat menu induk
 *   TIDAK berlaku di sini</b> &mdash; verifikasi negatif yang menenangkan.</li>
 *   <li><b>Jalur ZK &mdash; pola kisi tanpa gerbang: TIDAK berlaku.</b> Grid layar ini hanya
 *   berisi label nama dan keterangan; tidak ada checkbox "Aktif"/"Default" yang menulis ke
 *   basis data langsung dari baris grid. Verifikasi negatif kedua.</li>
 *   <li><b>Jalur REST &mdash; PERLU DIPERHATIKAN.</b> {@code kode_akun_grup},
 *   {@code kode_akun_grup_simpan}, dan {@code kode_akun_grup_hapus} dilayani
 *   {@code KodeAkunApiHelper}, yang gerbangnya {@code bolehAksi(...)} <b>mengembalikan
 *   {@code true} ketika peran pengguna tidak terbaca</b> ({@code role == null}) &mdash; pola
 *   fail-open yang sama dengan yang tercatat di sederet helper API akunting lain. Ironisnya
 *   kunci {@code "grup_akun"} <b>sudah didaftarkan</b> pada
 *   {@code EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF}, yaitu niat eksplisit "tertutup secara
 *   bawaan"; niat itu tidak pernah tereksekusi bagi pengguna yang perannya gagal dimuat,
 *   karena keputusan sudah dipotong menjadi {@code true} sebelum katalog menu sempat
 *   dikonsultasikan. Dampaknya terbatas pada data master (menambah/mengganti nama/menghapus
 *   kelompok), tidak menyentuh jurnal.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; penyajian:</b> {@link #getId()}, {@link #setId(Long)},
 *   {@link #toString()};</li>
 *   <li><b>Isi katalog:</b> {@link #getNama()}, {@link #setNama(String)},
 *   {@link #getKeterangan()}, {@link #setKeterangan(String)};</li>
 *   <li><b>Jejak audit otomatis:</b> {@link #getOleh()}, {@link #setOleh(String)},
 *   {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *   {@link #setTanggal_dirubah(java.util.Date)}, serta kait {@code onUpdate()};</li>
 *   <li><b>Konstruksi:</b> {@link #GrupAkun()}.</li>
 * </ul>
 * <p>Tidak ada satu pun method dengan logika bisnis di kelas ini &mdash; seluruh perilaku
 * yang menarik justru berada di pemakainya. Ini disengaja dan sebaiknya dipertahankan.</p>
 *
 * <h3>Hal-hal yang tidak terlihat dari sekilas membaca</h3>
 * <ul>
 *   <li><b>Bukan {@code @Entity}/{@code @MappedSuperclass} pada induknya.</b>
 *   {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa; Hibernate tidak
 *   memetakan properti apa pun darinya. Karena itu {@code id}, {@code oleh}, {@code olehId},
 *   dan {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di sini &mdash; pengulangan
 *   itu keharusan teknis, bukan sisa salin-tempel yang perlu "dirapikan". Yang benar-benar
 *   diwarisi adalah {@code check()}/{@code chek()} (resolusi proxy lazy) dan mesin cache
 *   data awal.</li>
 *   <li><b>Akses properti, bukan field.</b> {@code @Id} dipasang pada {@link #getId()},
 *   sehingga Hibernate membaca setiap nilai lewat getter-nya, termasuk saat dirty-check dan
 *   saat menyimpan. Akibat yang berguna: pemangkasan spasi di {@link #getNama()} ikut berlaku
 *   pada nilai yang <b>ditulis</b> ke kolom.</li>
 *   <li><b>Uji duplikasi bergantung pada pemangkasan itu.</b>
 *   {@code GrupAkunAction.onSave} menyimpan {@code nama.getValue()} <b>tanpa</b> trim, lalu
 *   {@code checkNamaGrupAkun()} mencari dengan {@code Restrictions.eq("nama", nilai.trim())}.
 *   Keduanya cocok <b>hanya karena</b> yang benar-benar mendarat di kolom sudah dipangkas
 *   getter. Bila kelak {@link #getNama()} diubah agar tidak lagi memangkas, penjaga duplikasi
 *   di ZK maupun REST akan bocor tanpa ada yang mengubah kode penjaganya.</li>
 *   <li><b>Uji duplikasi peka huruf besar/kecil di satu sisi, tidak di sisi lain.</b>
 *   {@code GrupAkunAction.checkNamaGrupAkun()} dan {@code KodeAkunApiHelper.grupAkunSimpan}
 *   memakai {@code Restrictions.eq} (peka huruf), sedangkan pencarian grup saat impor bagan
 *   akun di {@code AkunAction} memakai {@code ilike(..., MatchMode.EXACT)} (tidak peka huruf).
 *   Jadi "Aktiva" dan "aktiva" dapat hidup berdampingan sebagai dua baris, lalu impor akan
 *   memungut salah satunya secara sembarang lewat {@code setMaxResults(1)}.</li>
 *   <li><b>Impor bagan akun dapat mengotori katalog ini.</b> Pada {@code AkunAction}, grup
 *   dicari lebih dulu dan <b>dibuat serta di-commit</b> bila tidak ketemu; baru sesudah itu
 *   ada cabang yang membuang hasilnya dan memakai grup akun induk ketika kolom grup di
 *   spreadsheet ternyata kosong. Urutan itu menyisakan satu baris {@code GrupAkun} bernama
 *   string kosong di master &mdash; lolos karena {@code nullable = false} tidak menolak
 *   string kosong.</li>
 *   <li><b>Penghapusan dijaga berbeda di dua jalur.</b> Lewat REST, {@code grupAkunHapus}
 *   lebih dulu menghitung akun pemakai dan menolak dengan pesan jelas. Lewat layar ZK,
 *   {@code Common.refreshDelete} langsung dijalankan dan satu-satunya perlindungan adalah
 *   pelanggaran kunci asing di basis data yang ditangkap sebagai {@code Exception}.</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate} aktif</b>, sehingga hanya kolom yang
 *   benar-benar berubah yang ikut dalam pernyataan SQL.</li>
 *   <li><b>{@code @Audited} (Envers)</b> menggandakan setiap versi baris ke tabel revisi;
 *   riwayat perubahan nama ditampilkan di kolom pertama grid lewat
 *   {@code RevisiHelper.createNewRevisi}. Menghapus sebuah grup tidak menghapus jejak
 *   revisinya.</li>
 *   <li><b>Komentar generator menyesatkan.</b> Baris "{@code Bank generated by hbm2java}"
 *   pada Javadoc lama adalah sisa salin-tempel dari entity {@code Bank}; kelas ini tidak ada
 *   hubungannya dengan bank. Hal serupa terjadi di layar: beberapa pesan validasi
 *   {@code GrupAkunAction} masih menyebut "Agama", dan variabel hasil query di sana masih
 *   bernama {@code agama}/{@code kotaCount}.</li>
 * </ul>
 *
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.dao.akunting.GrupAkunDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "grup_akun")
public class GrupAkun extends GeneralValueObject {

	/**
	 * Versi serial bawaan generator.
	 *
	 * <p>Nilai yang sama persis muncul di ratusan entity lain repo ini; itu <b>bukan</b>
	 * penanda hubungan antar kelas, melainkan konstanta yang disalin generator ke setiap
	 * keluarannya.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris grup akun; {@code null} selama baris belum tersimpan. */
	private Long id;

	/** Nama pengguna terakhir yang menyimpan baris ini; diisi otomatis oleh interceptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang menyimpan baris ini; diisi otomatis oleh interceptor audit. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyimpan baris grup ini.
	 *
	 * <p>Pada jalur REST nilai ini diisi eksplisit oleh {@code KodeAkunApiHelper.grupAkunSimpan}
	 * dari {@code Tbmuser.getUserId()}; pada jalur ZK pengisiannya diserahkan kepada
	 * {@code AuditTimestampInterceptor}.</p>
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah melewati jalur audit.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir.
	 *
	 * <p>Argumen {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b>: method
	 * keluar tanpa mengubah apa pun, sehingga nilai lama tetap bertahan dan jejak audit tidak
	 * dapat dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna; nilai kosong tidak berpengaruh.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks berupa nama grup apa adanya.
	 *
	 * <p>Membaca <b>field</b> {@code nama} secara langsung, bukan lewat {@link #getNama()},
	 * sehingga spasi ujung tidak dipangkas di sini dan hasilnya dapat berupa {@code null}
	 * untuk objek yang baru dibuat. Bentuk ini yang muncul sebagai label pilihan pada setiap
	 * combobox penyaring "Grup Akun" (dibangun {@code Common.insertComboDanSemua}) serta pada
	 * keluaran diagnostik.</p>
	 *
	 * @return nama grup apa adanya, atau {@code null} bila belum diisi.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau spasi saja diabaikan
	 * diam-diam.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong tidak berpengaruh.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang menyimpan baris grup ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA sekaligus deklarasi field waktu perubahan &mdash; keduanya berada
	 * pada satu baris sumber, persis seperti disisipkan oleh perkakas audit repo ini.
	 *
	 * <p>{@code onUpdate()} dipanggil Hibernate tepat sebelum setiap {@code UPDATE} baris ini
	 * dan menyerahkan pengisian jejak audit ({@code oleh}, {@code olehId},
	 * {@code tanggal_dirubah}) kepada
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Field
	 * {@code tanggal_dirubah} sendiri diberi nilai awal waktu pembuatan objek melalui
	 * {@code WaktuUtil.getDate()}, sehingga baris baru pun sudah membawa cap waktu.</p>
	 *
	 * <p><b>Jangan pisahkan baris ini</b> menjadi beberapa baris tanpa alasan: bentuknya
	 * seragam di seluruh entity repo dan dipakai sebagai penanda oleh perkakas penyisip.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir.
	 *
	 * <p>Biasanya tidak dipanggil kode aplikasi; pengisiannya diserahkan ke interceptor audit
	 * pada {@code onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris grup ini.
	 *
	 * @return cap waktu perubahan; tidak pernah {@code null} karena field diberi nilai awal
	 *         saat objek dibuat.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama kelompok akun, mis. "Aktiva Lancar"; wajib diisi dan menjadi <b>identitas
	 * fungsional</b> baris ini &mdash; layar maupun REST menolak nama yang sudah ada, dan
	 * impor bagan akun mencocokkan grup berdasarkan nama ini, bukan id.
	 */
	private String nama;

	/** Catatan bebas tentang kelompok; boleh kosong dan tidak dipakai logika mana pun. */
	private String keterangan;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate dan dipakai layar ZK
	 * ({@code GrupAkunAction.onAdd}), jalur REST ({@code KodeAkunApiHelper.grupAkunSimpan}),
	 * serta impor bagan akun ({@code AkunAction}) untuk membangun baris grup baru sebelum
	 * {@link #setNama(String)} diisi.
	 */
	public GrupAkun() {
	}

	/**
	 * Kunci utama baris grup akun.
	 *
	 * <p>Nilai inilah yang tersimpan pada kolom {@code grup_akun} setiap
	 * {@link ais.database.model.akunting.Akun} yang tergabung ke kelompok ini, dan yang
	 * dikirim sebagai {@code grupAkunId} pada muatan REST penyimpanan akun. Kosong/{@code null}
	 * juga dipakai layar sebagai penanda mode: {@code GrupAkunAction} memilih judul jendela
	 * "Tambah" atau "Ubah" dan memilih {@code save()} atau {@code update()} berdasarkan nilai
	 * ini.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris.
	 *
	 * @param id kunci utama; umumnya diisi Hibernate, bukan kode aplikasi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama kelompok akun, sudah dipangkas spasi ujungnya.
	 *
	 * <p>Karena entity ini memakai akses properti, pemangkasan berlaku juga saat Hibernate
	 * menyimpan &mdash; nilai yang mendarat di kolom adalah versi yang sudah di-{@code trim}.
	 * Sifat itu <b>menopang penjaga duplikasi</b> di {@code GrupAkunAction.checkNamaGrupAkun()}
	 * dan {@code KodeAkunApiHelper.grupAkunSimpan}, yang keduanya membandingkan nilai
	 * masukan hasil {@code trim()} dengan isi kolom; jangan hilangkan pemangkasan ini tanpa
	 * ikut memperbaiki kedua penjaga tersebut.</p>
	 *
	 * <p>Nilai ini pula yang ditampilkan sebagai teks kolom "Grup Akun" pada grid Kode Akun
	 * dan sebagai label tiap pilihan combobox penyaring laporan.</p>
	 *
	 * @return nama kelompok tanpa spasi ujung, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kelompok akun.
	 *
	 * <p>Tidak melakukan validasi apa pun: kekosongan dan duplikasi dijaga pemanggil
	 * ({@code GrupAkunAction.onSave} dan {@code KodeAkunApiHelper.grupAkunSimpan}), sehingga
	 * jalur yang tidak lewat keduanya &mdash; misalnya impor bagan akun &mdash; dapat
	 * menyimpan nama kosong.</p>
	 *
	 * @param nama nama kelompok; disimpan apa adanya dan baru dipangkas saat dibaca kembali
	 *             lewat {@link #getNama()}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas tentang kelompok ini.
	 *
	 * <p>Berbeda dengan {@link #getNama()}, nilai dikembalikan apa adanya tanpa pemangkasan.
	 * Kolomnya {@code nullable} dan tanpa batas panjang eksplisit, dan isinya tidak pernah
	 * dipakai logika mana pun &mdash; hanya ditampilkan sebagai kolom kedua grid master serta
	 * ikut pada balasan REST {@code kode_akun_grup}.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang kelompok ini.
	 *
	 * @param keterangan keterangan; boleh {@code null} atau kosong.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
