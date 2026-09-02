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
 * Entity master <b>jenis tabungan</b> (tabel {@code public.jenis_tabungan}) &mdash; katalog
 * "kantong" tempat saldo titipan/deposit mahasiswa maupun siswa disimpan.
 *
 * <p>Satu baris di sini menjawab pertanyaan <i>"uang titipan ini masuk ke pos apa?"</i>. Setiap
 * baris {@link ais.database.model.Deposit} (setoran/penarikan saldo) menunjuk ke satu jenis
 * tabungan lewat kolom {@code jenis_tabungan}, dan &mdash; bila modul akuntansi diaktifkan
 * &mdash; jenis tabungan itulah yang membawa {@link Akun} buku besar tujuan jurnalnya. Jadi
 * mengubah satu baris di master ini berpotensi mengubah <b>ke akun mana</b> jurnal seluruh
 * transaksi tabungan berikutnya diposting.</p>
 *
 * <h2>Peringatan nama: tiga hal berbeda yang mirip namanya</h2>
 * <p>Nama "jenis tabungan" beredar di tiga tempat dengan arti yang <b>tidak sama</b>. Jangan
 * tertukar:</p>
 * <table border="1" summary="Perbandingan tiga makna 'jenis tabungan'">
 *   <tr><th>Tempat</th><th>Tipe sebenarnya</th><th>Artinya</th></tr>
 *   <tr>
 *     <td><b>Kelas ini</b> ({@code JenisTabungan})</td>
 *     <td>{@code JenisTabungan}</td>
 *     <td>Master pos tabungan. Dirujuk {@link ais.database.model.Deposit#getJenisTabungan()}
 *         dan {@link JenisPembayaran#getJenisTabungan()}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code CicilanPembayaran.jenisTabungan}</td>
 *     <td>{@link JenisPembayaran} (bukan kelas ini!)</td>
 *     <td>Cara membayar cicilan memakai saldo tabungan &mdash; sebuah <i>jenis pembayaran</i>
 *         yang kebetulan diberi nama field {@code jenisTabungan}. Lihat
 *         {@code ais.database.model.CicilanPembayaran#getJenisTabungan()}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code JenisPembayaran.jenisTabungan}</td>
 *     <td>{@code JenisTabungan} (kelas ini)</td>
 *     <td>Relasi opsional: pos tabungan mana yang didebit bila jenis pembayaran itu memotong
 *         saldo tabungan.</td>
 *   </tr>
 * </table>
 * <p>Akibat praktisnya terlihat di {@code ais.database.model.akunting.GrupTransaksi
 * .ambilAkunPembayaran(...)}: variabel lokal di sana bernama {@code jenisTabungan} tetapi
 * bertipe {@code JenisPembayaran}, sehingga {@code getAkun()} yang dipanggil di sana
 * <b>bukan</b> {@link #getAkun()} milik kelas ini.</p>
 *
 * <h2>Baris "default" dan cache statis</h2>
 * <p>Tepat satu baris diharapkan bertanda {@link #getDefaultTabungan() defaultTabungan = true}.
 * Baris itu dipungut ke field statis {@link #DEFAULT_JENIS_TABUNGAN} oleh {@link #reloadDefault()}
 * dan dipakai sebagai <b>fallback</b> oleh {@link ais.database.model.Deposit#getJenisTabungan()}
 * ketika sebuah baris deposit tidak menyebut jenis tabungan apa pun. Perhatikan bahwa tidak ada
 * constraint unik yang menjamin hanya satu baris ber-{@code true}; bila ada dua,
 * {@code setMaxResults(1)} tanpa {@code addOrder(...)} membuat baris mana yang menang bergantung
 * pada urutan yang dikembalikan database.</p>
 *
 * <h2>Auto-seed saat startup (efek samping tulis pada jalur "baca")</h2>
 * <p>{@link #reloadDefault()} <b>tidak hanya membaca</b>: bila tak ada baris default, ia
 * <i>menulis</i> satu baris baru ke database (kode {@code "001"}, nama
 * {@code "Titipan Dana Studi"}) di dalam transaksinya sendiri. Method ini dipanggil dari
 * {@code ais.common.InitData.reloadDefaults()} saat aplikasi naik, sehingga instalasi baru selalu
 * punya minimal satu pos tabungan. Konsekuensinya: pada database yang sudah berisi data,
 * menghapus/menonaktifkan penanda default akan memunculkan baris "Titipan Dana Studi" secara
 * otomatis pada restart berikutnya, dan kode {@code "001"} bisa bentrok dengan baris lain karena
 * pemeriksaan duplikat kode hanya ada di layar (lihat bagian berikut).</p>
 *
 * <h2>Siapa yang menulis tabel ini</h2>
 * <ul>
 *   <li><b>Layar ZK</b> {@code /pages/master/jenis_tabungan.zul} +
 *       {@code ais.action.master.JenisTabunganAction} &mdash; CRUD lengkap, dengan validasi
 *       keunikan {@code kode} dan {@code nama} yang <b>hanya ada di layer Action</b>
 *       ({@code checkKode()}/{@code checkNama()}), bukan di entity maupun skema.</li>
 *   <li><b>Startup</b> {@code InitData.reloadDefaults()} &mdash; auto-seed di atas.</li>
 *   <li><b>API e-Kantin</b> {@code ais.action.servlet.api.KantinHelper} dan
 *       {@code PenyesuaianSaldoHelper} &mdash; keduanya <i>membaca</i> master ini untuk menempel
 *       pada {@link ais.database.model.Deposit} yang mereka buat; keduanya memakai pola "cari
 *       yang cocok, kalau tidak ada ambil yang pertama" sehingga sensitif terhadap urutan data.</li>
 * </ul>
 * <p>Karena validasi keunikan tinggal di Action, jalur non-ZK mana pun (API di atas, atau
 * endpoint CRUD reflektif generik {@code /Data}) dapat menciptakan kode/nama duplikat tanpa
 * halangan.</p>
 *
 * <h2>Catatan kontrol akses (hasil pemeriksaan, bukan perbaikan)</h2>
 * <p>Halaman {@code /pages/master/jenis_tabungan.zul} <b>tidak</b> termasuk dalam whitelist
 * {@code CommonPrivilages.MUST_CHECKED} (12 halaman), sehingga panggilan
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose} praktis tidak memeriksa apa pun
 * untuk layar ini. <b>Namun</b> &mdash; berbeda dari banyak layar lain &mdash;
 * {@code JenisTabunganAction.doAfterCompose} melakukan pemeriksaan eksplisit sendiri
 * ({@code checkPrevilages(READ)} + {@code goLogoff()}, tombol tambah/ubah/hapus digerbangi
 * {@code CREATE}/{@code UPDATE}/{@code DELETE}, dan tombol hapus tambahan dibatasi user
 * {@code root}). Jadi layar ini <i>benar-benar</i> terjaga.</p>
 * <p>Kehalusan yang perlu diketahui: layar ini tidak berdiri sendiri di menu &mdash; satu-satunya
 * pemuatnya adalah tab "Jenis Deposit" di dalam {@code ais.action.master.DepositAction}
 * ({@code loadIncludeOnce(tabJenisDeposit, "/pages/master/jenis_tabungan.zul")}). Karena
 * {@code Common.getCurrentMenu()} mengambil atribut session {@code "currentMenu"} (menu terakhir
 * yang diklik), hak akses yang diperiksa saat layar ini tampil adalah hak akses <b>menu Deposit</b>,
 * bukan hak akses menu tersendiri. Artinya siapa pun yang punya {@code CREATE}/{@code UPDATE} di
 * layar Deposit dengan sendirinya boleh mengubah master jenis tabungan, termasuk mengganti
 * {@link #getAkun() akun buku besar} tujuan jurnalnya. Ini bukan celah "fail-open", melainkan
 * granularitas izin yang menempel pada halaman induk &mdash; catat saat mengaudit siapa yang boleh
 * mengarahkan jurnal tabungan.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; label</b>: {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Flag</b>: {@link #getAktif()} (tampil/tidak di pencarian),
 *       {@link #getDefaultTabungan()} (penanda baris default).</li>
 *   <li><b>Relasi</b>: {@link #getAkun()} &mdash; satu-satunya relasi keluar, ke buku besar.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Utilitas statis</b>: {@link #DEFAULT_JENIS_TABUNGAN}, {@link #reloadDefault()}.</li>
 * </ul>
 *
 * <h2>Kenapa {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} dideklarasikan ulang</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti apa pun miliknya.
 * Mendeklarasikan ulang keempat anggota itu di sini <b>bukan duplikasi yang keliru</b>, melainkan
 * keharusan teknis agar kolomnya benar-benar terpetakan. Yang diwarisi dari induk adalah
 * <i>perilaku</i>-nya, terutama {@code check(...)} untuk resolusi proxy lazy yang dipakai
 * {@link #getAkun()}.</p>
 *
 * <h2>Verifikasi pola berulang pada file ini</h2>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field</b>: <b>YA</b> &mdash; {@link #getAkun()}
 *       menugaskan hasil {@code check(akun)} kembali ke field {@code akun}. Getter lain
 *       ({@code getKode}, {@code getNama}, {@code getAktif}, {@code getDefaultTabungan})
 *       hanya menormalkan nilai yang dikembalikan <b>tanpa</b> menyentuh field.</li>
 *   <li><b>Getter yang menutup session Hibernate</b>: <b>TIDAK ADA</b> getter yang melakukannya;
 *       tetapi {@link #reloadDefault()} (method statis, bukan getter) <b>menutup session milik
 *       thread pemanggil</b> lewat {@code HibernateUtil.closeSession()}.</li>
 *   <li><b>Getter destruktif</b> (menghapus/mengosongkan data sebagai efek samping):
 *       <b>TIDAK ADA</b> di file ini.</li>
 *   <li><b>Setter yang menolak nilai</b>: <b>YA</b> &mdash; {@link #setOleh(String)} dan
 *       {@link #setOlehId(String)} mengabaikan {@code null}/string kosong secara senyap.</li>
 * </ul>
 *
 * <p><b>Catatan pemetaan:</b> {@code kode}, {@code aktif}, {@code defaultTabungan}, {@code oleh},
 * {@code olehId} dan {@code tanggal_dirubah} tidak memakai {@code @Column}, sehingga jatuh ke
 * {@code ais.database.hibernate.MyNamingStrategy} (turunan {@code DefaultNamingStrategy}: nama
 * kolom = nama properti apa adanya, tanpa konversi camelCase &rarr; snake_case). Query HQL/Criteria
 * tetap memakai nama properti, jadi hal ini hanya relevan saat menulis SQL native.</p>
 *
 * <p>Kelas ini ber-{@link Audited}, sehingga setiap perubahan tersimpan di tabel revisi Envers dan
 * dapat ditelusuri lewat {@code ais.action.master.helper.RevisiHelper} (tombol revisi di kolom
 * pertama grid layar).</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.Deposit
 * @see ais.database.model.JenisPembayaran
 * @see ais.database.model.akunting.Akun
 * @see ais.action.master.JenisTabunganAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jenis_tabungan")
public class JenisTabungan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya berasal dari generator hbm2java dan <b>tidak boleh diubah</b>
	 * selama struktur kelas masih kompatibel &mdash; instance jenis tabungan ikut tersimpan di
	 * session ZK/HTTP yang dapat diserialisasi antar restart atau antar node.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci primer (kolom {@code id}, IDENTITY). {@code null} selama entity belum pernah disimpan;
	 * dipakai layar untuk membedakan mode "Tambah" dan "Ubah".
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (jejak audit ringan, kolom {@code oleh}).
	 * Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor}.
	 */
	private String oleh;

	/**
	 * ID pengguna terakhir yang mengubah baris ini (kolom {@code olehId}), pendamping {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Penanda "ini pos tabungan default". Boleh {@code null} pada data lama; {@link #getDefaultTabungan()}
	 * memperlakukan {@code null} sebagai {@code false}.
	 *
	 * <p>Baris ber-{@code true} inilah yang dipungut {@link #reloadDefault()} ke
	 * {@link #DEFAULT_JENIS_TABUNGAN} dan menjadi fallback bagi deposit tanpa jenis tabungan
	 * eksplisit.</p>
	 */
	private Boolean defaultTabungan;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Perilaku non-obvious:</b> nilai {@code null} atau string kosong/hanya spasi
	 * <b>diabaikan secara senyap</b> &mdash; field lama dipertahankan. Konsekuensinya jejak audit
	 * tidak bisa dikosongkan lagi setelah pernah terisi, dan pemanggil yang bermaksud "reset"
	 * tidak akan mendapat kesalahan apa pun. Pola ini seragam di seluruh keluarga entity yang
	 * mewarisi {@link GeneralValueObject}.</p>
	 *
	 * @param olehId ID pengguna; {@code null}/kosong akan diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Perilaku non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan senyap sehingga nama pengubah tidak dapat dihapus.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong akan diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	 * Callback JPA {@code @PreUpdate}: mencatat jejak audit tepat sebelum {@code UPDATE} dikirim.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang
	 * mengisi {@link #setOleh(String)}, {@link #setOlehId(String)} dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> provider JPA/Hibernate saja &mdash; jangan dipanggil manual.
	 * Perhatikan bahwa hanya {@code @PreUpdate} yang dipasang; pada {@code INSERT} pertama nilai
	 * audit bergantung pada apa yang diisi pemanggil (atau interceptor global), bukan pada method
	 * ini.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir (kolom {@code tanggal_dirubah}).
	 *
	 * <p>Diinisialisasi ke waktu server saat instance dibuat lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()}; untuk entity yang dimuat dari database nilai ini
	 * langsung ditimpa Hibernate dengan isi kolomnya. Diperbarui {@link #onUpdate()} pada setiap
	 * {@code UPDATE}.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan oleh
	 * kode aplikasi. Berbeda dari {@link #setOleh(String)}, setter ini menerima {@code null}
	 * apa adanya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir (presisi {@code TIMESTAMP}), atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai antara lain oleh komponen ZK yang menampilkan objek langsung (combobox/listitem)
	 * dan oleh log. <b>Catatan:</b> method ini membaca field {@code nama} <i>secara langsung</i>,
	 * bukan lewat {@link #getNama()}, sehingga nilainya <b>tidak di-{@code trim()}</b> dan spasi
	 * berlebih di data akan ikut tampil. Untuk entity yang belum disimpan hasilnya diawali
	 * {@code "null-"}.</p>
	 *
	 * @return string {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Cache statis berisi baris jenis tabungan yang bertanda default.
	 *
	 * <p><b>Sifat:</b> satu nilai untuk seluruh JVM (bukan per pengguna/per session), diisi
	 * {@link #reloadDefault()} dan bernilai {@code null} sebelum pemanggilan pertama. Pembacanya
	 * yang utama adalah {@link ais.database.model.Deposit#getJenisTabungan()}, yang memakainya
	 * sebagai <b>fallback</b> ketika baris deposit tidak menunjuk jenis tabungan apa pun.</p>
	 *
	 * <p><b>Hal yang perlu diwaspadai:</b></p>
	 * <ul>
	 *   <li>Objek ini <b>detached</b>: {@link #reloadDefault()} menutup session yang memuatnya,
	 *       sehingga membaca relasi lazy-nya (mis. {@link #getAkun()}) akan menempuh jalur
	 *       reload {@code check(...)} milik {@link GeneralValueObject}, bukan session asal.</li>
	 *   <li>Karena instance detached ini dapat ikut ditugaskan ke field sebuah {@code Deposit}
	 *       yang persistent, cascade {@code PERSIST}/{@code MERGE} pada penyimpanan deposit dapat
	 *       menyentuhnya kembali &mdash; itulah alasan objek ini harus selalu berupa baris yang
	 *       sudah ada di database (punya {@code id}), bukan objek baru.</li>
	 *   <li>Field non-{@code volatile} dan ditulis dari thread startup maupun thread request;
	 *       tidak ada sinkronisasi.</li>
	 * </ul>
	 *
	 * @see #reloadDefault()
	 */
	public static JenisTabungan DEFAULT_JENIS_TABUNGAN = null;

	/**
	 * Menyegarkan cache {@link #DEFAULT_JENIS_TABUNGAN} &mdash; dan <b>membuat baris default bila
	 * belum ada</b>.
	 *
	 * <p><b>Alur:</b></p>
	 * <ol>
	 *   <li>Membuka/mengambil session native milik thread ({@code HibernateUtil.currentNativeSession()}).</li>
	 *   <li>Mencari satu baris ber-{@code defaultTabungan = true} dan menyimpannya ke
	 *       {@link #DEFAULT_JENIS_TABUNGAN}. Tanpa {@code addOrder(...)}, bila ada lebih dari satu
	 *       baris bertanda default maka baris yang terpilih bergantung pada urutan database.</li>
	 *   <li><b>Bila tidak ada sama sekali</b>: membangun baris baru (kode {@code "001"}, nama dan
	 *       keterangan {@code "Titipan Dana Studi"}, {@code aktif = true},
	 *       {@code defaultTabungan = true}), membuka transaksi, menyimpannya, lalu commit.</li>
	 *   <li>Menutup session thread ({@code HibernateUtil.closeSession()}).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang harus disadari pemanggil:</b></p>
	 * <ul>
	 *   <li><b>Menulis ke database pada method yang namanya terdengar seperti operasi baca.</b>
	 *       Pada instalasi baru ini fitur (menjamin selalu ada satu pos tabungan); pada database
	 *       berisi data ini berarti baris "Titipan Dana Studi" akan muncul kembali sendiri setiap
	 *       kali penanda default hilang. Kode {@code "001"} yang di-hardcode tidak diperiksa
	 *       keunikannya, sehingga bisa berbenturan dengan baris lain yang sudah memakai kode itu
	 *       &mdash; validasi keunikan hanya ada di layar, bukan di skema.</li>
	 *   <li><b>Menutup session Hibernate milik thread pemanggil.</b> Setiap entity yang masih
	 *       dipegang pemanggil menjadi detached begitu method ini selesai. Di
	 *       {@code JenisTabunganAction} method ini sengaja dipanggil <i>setelah</i>
	 *       {@code session.flush()} pada simpan/hapus/centang-default, jadi urutannya aman &mdash;
	 *       pemanggil baru wajib mengikuti urutan yang sama.</li>
	 *   <li>{@code session.getTransaction().begin()} dipanggil tanpa memeriksa apakah sudah ada
	 *       transaksi aktif pada session tersebut; jalur pemanggil yang sudah membuka transaksi
	 *       sendiri berisiko gagal di titik ini.</li>
	 * </ul>
	 *
	 * <p><b>Dipanggil dari:</b> {@code ais.common.InitData.reloadDefaults()} saat aplikasi naik,
	 * serta dari {@code ais.action.master.JenisTabunganAction} setiap kali baris master disimpan,
	 * dihapus, atau checkbox "Default" diubah &mdash; supaya cache statis tidak basi.</p>
	 *
	 * @see #DEFAULT_JENIS_TABUNGAN
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		DEFAULT_JENIS_TABUNGAN = (JenisTabungan) session.createCriteria(JenisTabungan.class)
				.add(Restrictions.eq("defaultTabungan", true)).setMaxResults(1).uniqueResult();
		if (DEFAULT_JENIS_TABUNGAN == null) {
			DEFAULT_JENIS_TABUNGAN = new JenisTabungan();
			DEFAULT_JENIS_TABUNGAN.setKode("001");
			DEFAULT_JENIS_TABUNGAN.setAktif(true);
			DEFAULT_JENIS_TABUNGAN.setDefaultTabungan(true);
			DEFAULT_JENIS_TABUNGAN.setNama("Titipan Dana Studi");
			DEFAULT_JENIS_TABUNGAN.setKeterangan("Titipan Dana Studi");
			session.getTransaction().begin();
			session.save(DEFAULT_JENIS_TABUNGAN);
			session.getTransaction().commit();
		}
		HibernateUtil.closeSession();
	}

	/**
	 * Kode singkat jenis tabungan (mis. {@code "001"}), kolom {@code kode}.
	 *
	 * <p>Keunikannya <b>tidak</b> dijamin skema maupun entity &mdash; hanya divalidasi
	 * {@code JenisTabunganAction.checkKode()} di layar.</p>
	 */
	private String kode;

	/**
	 * Akun buku besar tujuan jurnal untuk transaksi tabungan pada pos ini (kolom {@code akun}).
	 * Relasi lazy dan opsional; hanya relevan bila konfigurasi
	 * {@code integrasi_modul_akuntansi} aktif.
	 */
	private Akun akun;

	/** Nama jenis tabungan yang tampil di layar dan laporan (kolom {@code nama}, wajib isi). */
	private String nama;

	/** Keterangan bebas untuk baris ini (kolom {@code keterangan}, opsional). */
	private String keterangan;

	/**
	 * Penanda baris masih dipakai. Boleh {@code null} pada data lama;
	 * {@link #getAktif()} memperlakukan {@code null} sebagai {@code true}.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen &mdash; wajib ada untuk Hibernate dan dipakai layar saat menekan
	 * tombol "Tambah". Semua properti dibiarkan {@code null} kecuali {@link #tanggal_dirubah}
	 * yang terisi waktu server melalui inisialisasi field.
	 */
	public JenisTabungan() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Kolom di-{@code IDENTITY} dan ditandai {@code insertable = false}: nilainya sepenuhnya
	 * ditentukan sequence database dan tidak pernah ikut dikirim pada {@code INSERT}.</p>
	 *
	 * @return ID baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer. Dipakai Hibernate saat memuat/menyimpan; kode aplikasi sebaiknya
	 * tidak memanggilnya langsung.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode jenis tabungan dalam bentuk sudah di-{@code trim()}.
	 *
	 * <p><b>Perhatikan asimetri dengan {@link #getNama()}:</b> getter ini mengubah {@code null}
	 * menjadi string kosong {@code ""}, sedangkan {@code getNama()} tetap mengembalikan
	 * {@code null}. Pemanggil yang ingin membedakan "belum diisi" dari "diisi kosong" tidak bisa
	 * mengandalkan getter ini. Nilai field aslinya <b>tidak</b> diubah oleh pemanggilan ini.</p>
	 *
	 * @return kode tanpa spasi tepi; {@code ""} bila kode {@code null}, tidak pernah {@code null}
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menetapkan kode jenis tabungan. Nilai disimpan apa adanya (tanpa {@code trim()} maupun
	 * pemeriksaan duplikat); keunikan divalidasi terpisah di layar.
	 *
	 * @param kode kode jenis tabungan; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis tabungan dalam bentuk sudah di-{@code trim()}.
	 *
	 * <p>Berbeda dari {@link #getKode()}, {@code null} dikembalikan apa adanya. Field aslinya tidak
	 * diubah oleh pemanggilan ini.</p>
	 *
	 * @return nama tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama jenis tabungan.
	 *
	 * <p>Kolom dipetakan {@code nullable = false}, jadi menyimpan entity dengan nama {@code null}
	 * akan ditolak database. Layar mencegahnya lebih dahulu lewat validasi wajib-isi dan
	 * pemeriksaan duplikat {@code checkNama()}.</p>
	 *
	 * @param nama nama jenis tabungan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * <p>Dikembalikan apa adanya &mdash; tanpa {@code trim()}, berbeda dari {@link #getNama()}
	 * dan {@link #getKode()}.</p>
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris ini.
	 *
	 * @param keterangan keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris ini, dengan {@code null} dianggap <b>aktif</b>.
	 *
	 * <p><b>Non-obvious:</b> default "anggap aktif" ini membuat baris lama yang kolom
	 * {@code aktif}-nya masih {@code null} tetap tampil dan tetap dapat dipilih. Perilakunya
	 * konsisten dengan filter pencarian di {@code JenisTabunganAction.initCriteria(...)} yang
	 * memakai {@code isNull("aktif") OR eq("aktif", true)}. Field aslinya tidak ditulis ulang,
	 * jadi nilai {@code null} tetap {@code null} di database sampai ada yang menyimpannya
	 * eksplisit lewat checkbox "Aktif" di grid.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} bila dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif baris ini.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" di grid layar, yang langsung menyimpan perubahannya
	 * ({@code Common.refreshSaveOrUpdate}). Menonaktifkan baris hanya menyembunyikannya dari
	 * pencarian default &mdash; baris deposit yang sudah menunjuk ke sini tetap utuh.</p>
	 *
	 * @param aktif status aktif; {@code null} akan dibaca sebagai aktif oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda "pos tabungan default", dengan {@code null} dianggap {@code false}.
	 *
	 * <p>Kebalikan dari {@link #getAktif()}: di sini {@code null} berarti <b>bukan</b> default,
	 * sehingga data lama tidak sengaja terpilih menjadi baris default. Field aslinya tidak
	 * ditulis ulang.</p>
	 *
	 * @return {@code true} hanya bila kolom benar-benar berisi {@code true}
	 */
	public Boolean getDefaultTabungan() {
		return defaultTabungan == null ? false : defaultTabungan;
	}

	/**
	 * Menetapkan penanda "pos tabungan default".
	 *
	 * <p><b>Dampak lebih luas dari sekadar satu kolom:</b> baris bertanda inilah yang dipungut
	 * {@link #reloadDefault()} ke {@link #DEFAULT_JENIS_TABUNGAN}, yang selanjutnya dipakai
	 * {@link ais.database.model.Deposit#getJenisTabungan()} sebagai fallback bagi setiap deposit
	 * tanpa jenis tabungan eksplisit. Mengubah penanda ini karenanya menggeser pos tujuan
	 * transaksi tabungan yang tidak menyebut posnya sendiri.</p>
	 *
	 * <p>Tidak ada mekanisme yang mematikan penanda pada baris lain, sehingga beberapa baris bisa
	 * bertanda default sekaligus; layar (checkbox "Default" di grid) memanggil
	 * {@link #reloadDefault()} setelah menyimpan agar cache tetap sinkron, tetapi tidak melakukan
	 * pembersihan penanda ganda.</p>
	 *
	 * @param defaultTabungan penanda default; boleh {@code null} (dibaca sebagai {@code false})
	 */
	public void setDefaultTabungan(Boolean defaultTabungan) {
		this.defaultTabungan = defaultTabungan;
	}

	/**
	 * Mengembalikan akun buku besar tujuan jurnal untuk pos tabungan ini.
	 *
	 * <p><b>Efek samping (pola berulang, terverifikasi di file ini):</b> hasil
	 * {@code check(akun)} milik {@link GeneralValueObject} <b>ditugaskan kembali ke field</b>
	 * {@code akun}. {@code check(...)} meresolusi proxy lazy secara bertahap (flag {@code initData},
	 * cache in-memory, session aktif, lalu reload lewat session baru sebagai penyelamat terakhir)
	 * dan mengembalikan argumennya apa adanya bila keempatnya gagal &mdash; jadi getter ini tidak
	 * pernah melempar {@code LazyInitializationException}, tetapi bisa mengembalikan proxy yang
	 * belum terinisialisasi ketika entity ini sudah detached. Getter ini <b>tidak</b> menutup
	 * session dan <b>tidak</b> menghapus data.</p>
	 *
	 * <p>Relasi bersifat opsional dan hanya ditampilkan/diwajibkan di layar bila konfigurasi
	 * {@code integrasi_modul_akuntansi} aktif. Cascade {@code PERSIST}/{@code MERGE} berarti
	 * menyimpan jenis tabungan ikut menyentuh akun yang tertaut.</p>
	 *
	 * @return akun buku besar tertaut, atau {@code null} bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun buku besar tujuan jurnal untuk pos tabungan ini.
	 *
	 * <p>Dipanggil layar saat menyimpan (nilai diambil dari komponen pencari akun
	 * {@code AmbilDataAkunBanbox}). Karena akun inilah yang dipakai modul akuntansi untuk
	 * menentukan sisi jurnal transaksi tabungan, perubahannya berlaku untuk seluruh transaksi
	 * berikutnya pada pos ini &mdash; jurnal yang sudah terbentuk tidak ikut berubah.</p>
	 *
	 * @param akun akun buku besar; boleh {@code null} bila integrasi akuntansi tidak dipakai
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}
}
