package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;

/**
 * Entity <b>master kantin sekolah</b>, dipetakan ke tabel {@code sekolah.kantin}. Satu baris
 * mewakili satu gerai/kantin milik sebuah sekolah: namanya ({@link #getNama()}), alamat/lokasinya
 * ({@link #getAlamat()}), cakupan kepemilikannya ({@link #getSekolah()} dan {@link #getYayasan()}),
 * akun buku besar tempat penerimaannya dijurnal ({@link #getAkun()}), serta satu flag wewenang
 * pelaporan ({@link #getBolehMelihatLaporanKantinLain()}).
 *
 * <p>Strukturnya sangat ramping — <b>tidak ada</b> {@code kode}, {@code aktif}, {@code keterangan},
 * {@code nomorUrut}, jam operasional, kontak, maupun koleksi anak. Hanya {@code nama} yang
 * {@code nullable = false}; seluruh kolom lain boleh kosong. Tidak ada method bisnis, tidak ada
 * query statis, tidak ada {@code toString()} sendiri (dipakai warisan
 * {@link GeneralValueObject#toString()}, yang untuk entity ini menghasilkan nama kantin apa adanya
 * karena {@code getKode()} selalu {@code null} di sini).
 *
 * <h2>PENTING: entity ini YATIM — verifikasi independen 3 Sep 2026</h2>
 * <p>Penelusuran ulang seluruh pohon sumber (bukan sekadar mengutip catatan
 * {@link PembelianSiswa}) mengonfirmasi bahwa <b>tidak ada satu pun pemanggil runtime</b> untuk
 * tipe ini:</p>
 * <ul>
 *   <li>Nol berkas Java yang menulis {@code import ais.database.model.sekolah.Kantin;} dan nol
 *       pemakaian nama berkualifikasi penuh {@code ais.database.model.sekolah.Kantin} di kode
 *       Java mana pun.</li>
 *   <li>Nol {@code new Kantin(...)}, nol {@code session.get/load(Kantin.class, ...)}, nol
 *       {@code createCriteria(Kantin.class)}, nol HQL {@code from Kantin}, nol kelas
 *       {@code Action}, halaman ZUL, atau JSP yang menargetkannya.</li>
 *   <li>Satu-satunya penyebut di lapis Java adalah tetangga sepaketnya
 *       {@link PembelianSiswa#getKantin()} — dan {@link PembelianSiswa} sendiri sudah terverifikasi
 *       YATIM TOTAL (lihat Javadoc kelas berkas tersebut). Jadi rantainya yatim seluruhnya, bukan
 *       "entity mati yang masih dirujuk entity hidup".</li>
 *   <li>Kelasnya <b>tetap</b> terdaftar di {@code hibernate.cfg.xml} (baris 2309), sehingga
 *       Hibernate tetap memetakan, memvalidasi, dan (pada mode auto-DDL) menurunkan tabel
 *       {@code sekolah.kantin} berikut tabel audit Envers {@code sekolah.kantin_AUD}. Tabelnya
 *       ada; isinya tidak pernah ditulis lewat jalur aplikasi.</li>
 * </ul>
 *
 * <h2>Pembaca SQL satu-satunya pun sudah dimatikan</h2>
 * <p>Satu-satunya artefak di seluruh repositori yang benar-benar membaca tabel
 * {@code sekolah.kantin} adalah laporan Jasper
 * {@code webapp/report/sekolah/pembayaran/laporan_saldo_rinci.jrxml}, lewat
 * {@code left join kantin c on (c.id = b.kantin_id)} untuk memunculkan nama kantin pada rincian
 * mutasi saldo siswa. Laporan itu <b>tidak dapat dibuka dari UI</b>: satu-satunya tempat yang
 * pernah mendaftarkannya,
 * {@code ais.action.report.format1.sekolah.LaporanSaldoSiswa} baris 284-288, seluruhnya
 * dikomentari. Tab "saldo rinci" tidak pernah dibuat, sehingga bahkan pada tingkat SQL pun tabel
 * ini tidak punya pembaca aktif.
 *
 * <h2>Jebakan penamaan — modul kantin yang HIDUP memakai entity lain</h2>
 * <p>Aplikasi ini punya modul kantin/koperasi yang sangat aktif, tetapi <b>tidak satu pun</b>
 * bagiannya menyentuh entity ini. Padanan hidup untuk setiap konsep di kelas ini:</p>
 * <ul>
 *   <li><b>Master gerai</b> &rarr; {@code ais.database.model.inventory.Toko} (tabel
 *       {@code koperasi.toko}). Inilah yang dipakai {@code PosKantinAction},
 *       {@code PengaturanKantinAction} (menu "Toko" &rarr; {@code /pages/master/inventory/toko.zul}),
 *       {@code DashboardKantinAction}, {@code LaporanKantinUtil}, dan seluruh API
 *       {@code ais.action.servlet.api.KantinHelper}.</li>
 *   <li><b>Transaksi belanja</b> &rarr; {@code ais.database.model.inventory.Pembelian} (tabel
 *       {@code koperasi.pembelian}, kolom {@code waktu}/{@code harga_jual}, relasi
 *       {@code Pembelian.getToko()}). Menu "Belanja Siswa", laporan "Laporan Belanja Siswa", dan
 *       route API {@code pembelian_siswa} semuanya bermuara ke sana — <b>bukan</b> ke
 *       {@link PembelianSiswa} yang berpasangan dengan kelas ini.</li>
 *   <li><b>Flag "boleh melihat laporan gerai lain"</b> &rarr; {@code Toko.bolehMelihatTokolain}
 *       (kolom {@code boleh_melihat_tokolain}). Field {@link #getBolehMelihatLaporanKantinLain()}
 *       di kelas ini adalah versi mati dari gagasan yang sama; namanya unik di seluruh repositori
 *       (nol pembaca).</li>
 *   <li><b>Pemetaan akun buku besar</b> &rarr; {@code Toko.akunKas}/{@code akunPiutang}/
 *       {@code akunModalAwal}/{@code akunLabaDitahan}, dibaca
 *       {@code ais.action.master.koperasi.helper.AkunKantinUtil.akunToko(...)}. Relasi
 *       {@link #getAkun()} di sini hanya satu akun tunggal tanpa peran, dan tidak pernah dibaca
 *       siapa pun.</li>
 *   <li>Entity ber-akhiran "Kantin" lain yang HIDUP dan sama sekali tidak berhubungan dengan kelas
 *       ini: {@code koperasi.MejaKantin}, {@code koperasi.PembatalanTransaksiKantin},
 *       {@code inventory.ProduksiKantin}.</li>
 * </ul>
 *
 * <h2>Servlet {@code ais.action.servlet.Kantin} — tabrakan nama, bukan pemakai</h2>
 * <p>Ada berkas {@code ais/action/servlet/Kantin.java} dengan nama kelas sederhana yang sama.
 * Pemeriksaan sepintas (di luar cakupan audit mendalam): servlet itu <b>tidak menyentuh entity ini
 * sama sekali</b> — nol import model, badan {@code process()}-nya hanya
 * {@code request.getRequestDispatcher("/WEB-INF/baru/modul/kantin/landing_page.jsp").forward(...)}.
 * Ia adalah turunan template generator "CheckISBN" yang sama dengan servlet kiosk yang diaudit
 * batch 44 (komentar kelasnya masih berbunyi "Servlet implementation class CheckISBN"). Namanya
 * merujuk modul kantin JSP {@code /WEB-INF/baru/modul/kantin/}, yang bekerja di atas
 * {@code inventory}/{@code koperasi} — bukan {@code sekolah.kantin}.
 *
 * <h2>Risiko keamanan saat ini</h2>
 * <p>Praktis nihil, dengan alasan yang sama seperti {@link PembelianSiswa}: tidak ada endpoint,
 * layar, atau API yang membaca maupun menulis tabel ini, dan isinya selalu kosong. Satu catatan
 * untuk masa depan: manifest generator CRUD generik
 * ({@code webapp/WEB-INF/generic-crud/manifests/general_value_object_inventory.csv} baris 1155)
 * mendaftarkan kelas ini sebagai kandidat {@code ELIGIBLE_METADATA_FIRST} dengan status
 * <i>enabled = False</i> ("tetap default disabled sampai verifikasi Hibernate/menu/scope"). Bila
 * suatu saat flag itu dinyalakan tanpa menambahkan filter cakupan, layar CRUD otomatis akan
 * terlahir tanpa gerbang {@code sekolah}/{@code yayasan} — pola persis yang berulang di seluruh
 * temuan {@code task_58f74860}. Perlakukan penyalaan flag itu sebagai perubahan berisiko.
 *
 * <h2>Kuirk yang tidak kentara</h2>
 * <ul>
 *   <li>{@link #getYayasan()} <b>destruktif</b>: ia menimpa field {@code yayasan} dengan
 *       {@code getSekolah().getYayasan()} setiap kali dibaca. Karena Hibernate memakai property
 *       access, nilai hasil timpa itulah yang ikut ter-flush pada penyimpanan berikutnya —
 *       kolom {@code yayasan_id} secara efektif tidak bisa berbeda dari yayasan sekolahnya.</li>
 *   <li>{@link #setOleh(String)} dan {@link #setOlehId(String)} <b>menolak diam-diam</b> nilai
 *       {@code null}/kosong (langsung {@code return}), sehingga jejak pengubah terakhir tidak
 *       pernah bisa dikosongkan lagi setelah sekali terisi.</li>
 *   <li>{@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)} mengubah argumen menjadi
 *       {@code null} bila {@code getId()}-nya {@code null} — object transien yang belum disimpan
 *       hilang tanpa peringatan.</li>
 *   <li>Konstruktor {@link #Kantin(long, String)} menyetel {@code id} secara manual padahal
 *       strateginya {@link javax.persistence.GenerationType#IDENTITY}; memakainya lalu
 *       menyimpannya membuat Hibernate memperlakukan instance sebagai <i>detached</i>. Ini
 *       konstruktor bawaan hbm2java, bukan jalur pemakaian yang dimaksudkan.</li>
 *   <li>{@code nama} di kelas ini <b>membayangi</b> field {@code nama} milik
 *       {@link GeneralValueObject} berikut getter/setter-nya. Ini bukan bug melainkan konsekuensi
 *       teknis (lihat bagian di bawah), dan aman karena semua kode induk — termasuk
 *       {@link GeneralValueObject#compareTo(GeneralValueObject)} dan
 *       {@link GeneralValueObject#toString()} — mengakses nilainya lewat {@code getNama()} yang
 *       polimorfik, bukan lewat field langsung.</li>
 * </ul>
 *
 * <h2>Catatan warisan {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} adalah POJO abstrak biasa: ia <b>bukan</b> {@code @Entity} dan
 * <b>bukan</b> {@code @MappedSuperclass}, sehingga Hibernate tidak memetakan satu pun properti
 * induknya. Karena itu deklarasi ulang {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, dan {@link #getNama()} di kelas ini <b>bukan duplikasi keliru</b>
 * melainkan keharusan teknis: tanpa deklarasi ulang beranotasi, kolom-kolom tersebut tidak akan
 * ada di tabel. Yang tetap diwarisi dan berfungsi adalah perilaku non-persisten induk
 * ({@code check()}/{@code chek()} untuk resolusi proxy lazy, {@code equals}/{@code hashCode}
 * berbasis id, {@code compareTo}, {@code toString()}, dan helper statis lainnya).
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, dan callback {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b> — {@link #getId()} ({@code IDENTITY}, {@code insertable = false}).</li>
 *   <li><b>Cakupan kepemilikan</b> — {@link #getSekolah()} dan {@link #getYayasan()}.</li>
 *   <li><b>Atribut deskriptif</b> — {@link #getNama()}, {@link #getAlamat()}.</li>
 *   <li><b>Wewenang</b> — {@link #getBolehMelihatLaporanKantinLain()}.</li>
 *   <li><b>Integrasi akuntansi</b> — {@link #getAkun()}.</li>
 * </ol>
 *
 * @see ais.database.model.sekolah.PembelianSiswa
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.akunting.Akun
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "kantin", schema = "sekolah")
public class Kantin extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Dibutuhkan karena {@link GeneralValueObject} mengimplementasikan
	 * {@link java.io.Serializable}; nilainya dipatok agar instance yang sudah pernah
	 * diserialisasi (mis. ke sesi ZK yang dipertukarkan antar node) tetap kompatibel walau
	 * struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2675123286068270401L;

	/**
	 * Kunci utama baris, dipetakan ke kolom {@code sekolah.kantin.id}. Diisi database
	 * ({@code IDENTITY}), bukan aplikasi.
	 */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini; diisi
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}. Deklarasi ulang milik induk yang
	 * tidak terpetakan.
	 */
	private String oleh;

	/**
	 * Identitas (id pengguna) pengubah terakhir baris ini, pasangan dari {@link #oleh}. Deklarasi
	 * ulang milik induk yang tidak terpetakan.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, <b>dengan penolakan diam-diam</b>: bila
	 * {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung
	 * {@code return} tanpa mengubah apa pun dan tanpa melempar exception.
	 *
	 * <p>Akibatnya nilai yang sudah pernah terisi tidak dapat dikosongkan kembali lewat setter
	 * ini. Perilaku ini disengaja agar interceptor audit tidak menghapus jejak yang sudah ada
	 * ketika konteks pengguna kebetulan tidak tersedia (mis. proses batch/penjadwal).</p>
	 *
	 * @param olehId id pengguna pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penolakan diam-diam yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null} atau kosong diabaikan begitu saja.
	 *
	 * @param oleh nama pengguna pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate}: dijalankan Hibernate tepat sebelum
	 * pernyataan {@code UPDATE} baris ini dikirim ke database.
	 *
	 * <p>Isinya mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang memperbarui
	 * {@link #tanggal_dirubah} serta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif. Merupakan implementasi dari satu-satunya method {@code abstract} milik
	 * {@link GeneralValueObject}, sehingga setiap entity turunan wajib menyediakannya.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah state instance ini sesaat sebelum flush. Tidak pernah
	 * dipanggil langsung oleh kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini. Diinisialisasi ke waktu <b>pembuatan object</b>
	 * lewat {@code ais.ui.util.WaktuUtil.getDate()} (zona waktu aplikasi, bukan
	 * {@code new Date()} polos), lalu diperbarui {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance baru karena field
	 *         diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Akun buku besar ({@code akunting.akun}) yang dikaitkan dengan kantin ini. Tidak pernah
	 * dibaca kode mana pun; padanan hidupnya adalah empat relasi berperan di
	 * {@code inventory.Toko} ({@code akunKas}, {@code akunPiutang}, {@code akunModalAwal},
	 * {@code akunLabaDitahan}).
	 */
	private Akun akun;

	/** Sekolah pemilik kantin ini; kolom {@code sekolah_id}, boleh {@code null}. */
	private Sekolah sekolah;

	/**
	 * Yayasan pemilik kantin ini; kolom {@code yayasan_id}. Nilainya selalu diturunkan ulang dari
	 * {@link #sekolah} saat dibaca — lihat {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/** Alamat/lokasi kantin, kolom {@code alamat} bertipe {@code text} (bebas panjang). */
	private String alamat;

	/**
	 * Nama kantin, kolom {@code nama} dan satu-satunya kolom {@code nullable = false} di tabel
	 * ini. Membayangi field bernama sama milik {@link GeneralValueObject} (yang tidak terpetakan).
	 */
	private String nama;

	/**
	 * Flag wewenang: apakah pengelola kantin ini boleh melihat laporan kantin lain. Kode mati —
	 * nol pembaca di seluruh repositori. Padanan hidupnya {@code Toko.bolehMelihatTokolain}.
	 */
	private Boolean bolehMelihatLaporanKantinLain;

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate membutuhkannya untuk membuat
	 * instance kosong saat menghidrasi baris hasil query.
	 */
	public Kantin() {
	}

	/**
	 * Constructor "kolom wajib" bawaan hbm2java: mengisi kunci utama dan satu-satunya kolom
	 * {@code nullable = false}.
	 *
	 * <p><b>Hati-hati:</b> {@link #id} memakai strategi
	 * {@link javax.persistence.GenerationType#IDENTITY}, jadi menyetelnya sendiri lalu menyimpan
	 * instance ini membuat Hibernate memperlakukannya sebagai object <i>detached</i> (memicu
	 * {@code merge}/{@code update}, bukan {@code insert}). Konstruktor ini dibuat otomatis oleh
	 * generator dan tidak dipanggil dari mana pun di aplikasi.</p>
	 *
	 * @param id kunci utama baris; bertipe primitif {@code long} sehingga otomatis di-<i>box</i>
	 *           ke {@link Long}
	 * @param nama nama kantin
	 */
	public Kantin(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Anotasi {@code insertable = false} berarti kolom {@code id} sengaja tidak disertakan pada
	 * pernyataan {@code INSERT}: nilainya sepenuhnya ditentukan urutan/identitas database dan
	 * dibaca kembali oleh Hibernate setelah baris tersimpan.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Tanpa validasi.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik kantin ini, setelah proxy lazy-nya diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Pola {@code sekolah = check(sekolah); return this.sekolah;} adalah idiom standar seluruh
	 * entity AIS: {@code check()} berusaha menginisialisasi proxy Hibernate yang mungkin sudah
	 * <i>detached</i> (lewat cache in-memory, session aktif, lalu session baru) dan mengembalikan
	 * argumennya apa adanya bila semua upaya gagal. Hasilnya ditulis balik ke field agar
	 * pemanggilan berikutnya tidak mengulang resolusi.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila kolom {@code sekolah_id} kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik kantin ini, dengan <b>penyaringan object transien</b>: bila
	 * argumennya {@code null} <i>atau</i> {@link Sekolah#getId()}-nya {@code null} (belum pernah
	 * disimpan), field disetel ke {@code null} alih-alih menyimpan referensinya.
	 *
	 * <p>Idiom ini mencegah {@code TransientObjectException} saat flush, tetapi juga berarti
	 * penetapan sekolah yang belum tersimpan <b>hilang tanpa peringatan</b>.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object tanpa id menghasilkan {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik kantin ini — <b>getter destruktif</b>.
	 *
	 * <p>Berbeda dari getter relasi biasa, method ini tidak sekadar membaca: ia memanggil
	 * {@link #getSekolah()} lebih dulu dan, bila sekolahnya ada, <b>menimpa</b> field
	 * {@link #yayasan} dengan {@code sekolah.getYayasan()}. Nilai yayasan yang tersimpan di
	 * kolom {@code yayasan_id} karena itu selalu didenormalisasi ulang dari sekolahnya setiap
	 * kali baris dibaca.</p>
	 *
	 * <p><b>Efek samping:</b> karena Hibernate memetakan entity ini lewat <i>property access</i>
	 * (anotasi berada pada getter), nilai hasil timpa itulah yang ikut ter-flush pada
	 * penyimpanan berikutnya dalam session yang sama. Konsekuensinya {@code yayasan_id} secara
	 * efektif tidak bisa dibuat berbeda dari yayasan sekolahnya, dan nilai yayasan yang sudah
	 * terisi akan <b>dipertahankan</b> (bukan dikosongkan) bila sekolahnya {@code null} — karena
	 * blok {@code if} dilewati. Setelah penimpaan, hasilnya masih dilewatkan
	 * {@link GeneralValueObject#check(Object)} untuk resolusi proxy lazy.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila tidak dapat diturunkan maupun dibaca dari
	 *         kolomnya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik kantin ini, dengan penyaringan object transien yang sama seperti
	 * {@link #setSekolah(Sekolah)}.
	 *
	 * <p>Perlu diingat bahwa nilai yang disetel di sini akan <b>ditimpa</b> pada pembacaan
	 * berikutnya lewat {@link #getYayasan()} selama {@link #sekolah} terisi.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object tanpa id menghasilkan {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan alamat/lokasi kantin.
	 *
	 * @return alamat kantin, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat", columnDefinition = "text")
	public String getAlamat() {
		return this.alamat;
	}

	/**
	 * Menyetel alamat/lokasi kantin. Tanpa validasi maupun pembatasan panjang (kolomnya bertipe
	 * {@code text}).
	 *
	 * @param alamat alamat baru
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan nama kantin.
	 *
	 * <p>Meng-<i>override</i> {@link GeneralValueObject#getNama()} — sebuah keharusan teknis,
	 * karena hanya deklarasi di kelas ini yang beranotasi {@code @Column} dan karenanya
	 * dipetakan Hibernate. Override ini juga yang membuat urutan alami
	 * ({@link GeneralValueObject#compareTo(GeneralValueObject)}) dan
	 * {@link GeneralValueObject#toString()} membaca nilai yang benar secara polimorfik.</p>
	 *
	 * @return nama kantin; secara skema tidak boleh {@code null} pada baris tersimpan, tetapi
	 *         bisa {@code null} pada instance yang belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama kantin. Tanpa validasi — kolomnya {@code nullable = false}, sehingga nilai
	 * {@code null} baru akan ditolak database saat flush, bukan di sini.
	 *
	 * @param nama nama kantin baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan flag wewenang "boleh melihat laporan kantin lain".
	 *
	 * <p><b>Kode mati:</b> tidak ada satu pun pembaca flag ini di seluruh repositori (nama
	 * properti maupun nama kolomnya unik untuk berkas ini). Gagasan yang sama diimplementasikan
	 * secara hidup oleh {@code inventory.Toko.bolehMelihatTokolain}.</p>
	 *
	 * @return {@code TRUE}/{@code FALSE} sesuai isi kolom, atau {@code null} bila belum pernah
	 *         diisi — perhatikan tipe pembungkusnya, sehingga pemakaian langsung dalam kondisi
	 *         {@code if} berisiko {@code NullPointerException}
	 */
	@Column(name = "boleh_melihat_laporan_kantin_lain")
	public Boolean getBolehMelihatLaporanKantinLain() {
		return this.bolehMelihatLaporanKantinLain;
	}

	/**
	 * Menyetel flag wewenang "boleh melihat laporan kantin lain". Tanpa validasi; nilai
	 * {@code null} diterima dan disimpan sebagai {@code NULL}.
	 *
	 * @param bolehMelihatLaporanKantinLain nilai flag baru
	 */
	public void setBolehMelihatLaporanKantinLain(Boolean bolehMelihatLaporanKantinLain) {
		this.bolehMelihatLaporanKantinLain = bolehMelihatLaporanKantinLain;
	}

	/**
	 * Mengembalikan akun buku besar yang dikaitkan dengan kantin ini, setelah proxy lazy-nya
	 * diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Relasinya opsional ({@code nullable = true}) dan tanpa peran: tidak ada penanda apakah
	 * akun ini kas, piutang, pendapatan, atau modal. Karena entity ini yatim, tidak ada rutin
	 * penjurnalan yang pernah membacanya — bandingkan dengan
	 * {@code ais.action.master.koperasi.helper.AkunKantinUtil.akunToko(...)} yang memilih akun
	 * berdasarkan jenis dari {@code inventory.Toko}.</p>
	 *
	 * <p>Berbeda gaya (tetapi tidak berbeda perilaku) dari getter relasi lain di kelas ini:
	 * mengembalikan {@code akun}, bukan {@code this.akun}.</p>
	 *
	 * @return akun terkait, atau {@code null} bila kolom {@code akun_id} kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_id", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel akun buku besar terkait.
	 *
	 * <p>Berbeda dari {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}, setter ini
	 * <b>tidak</b> menyaring object transien: {@link Akun} tanpa id akan tersimpan apa adanya di
	 * field dan berpotensi memicu {@code TransientObjectException} saat flush (diredam sebagian
	 * oleh {@link CascadeType#PERSIST} pada relasinya).</p>
	 *
	 * @param akun akun buku besar baru; boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

}
