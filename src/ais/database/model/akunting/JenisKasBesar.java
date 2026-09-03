package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Map;

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
import org.hibernate.criterion.Order;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;

/**
 * Katalog master <b>Jenis Kas Besar</b> &mdash; pemetaan dari sebuah kategori kas besar
 * (mis. &quot;Kas Besar Yayasan&quot;, &quot;Kas Besar Unit A&quot;) ke <b>dua akun buku besar</b>
 * yang dipakai mesin posting saat dokumen kas besar dan LPJ kas besar dijurnal.
 * Tabel: <code>public.jenis_kas_besar</code> (perhatikan: schema <code>public</code>, bukan
 * <code>akunting</code>, meskipun kelasnya berada di paket <code>akunting</code> dan dokumen
 * yang memakainya tersimpan di <code>akunting.kas_besar</code>).
 *
 * <p>Di layar, katalog ini muncul sebagai menu <i>&quot;Akun Kas Besar&quot;</i>
 * (<code>MenuSnapshotData</code> id <code>115564</code>, jalur
 * <code>/pages/master/akunting/jenis_kas_besar.zul</code>) di bawah induk
 * <i>&quot;Uang Muka dan Kas Kecil&quot;</i>. Nama menunya sengaja disebut &quot;Akun&quot; dan
 * bukan &quot;Jenis&quot; &mdash; itu memang inti perannya: yang disimpan di sini bukan saldo,
 * bukan pagu, melainkan <b>pemetaan ke akun</b>.
 *
 * <h3>Peran dalam mesin akuntansi</h3>
 *
 * <p>Entity ini adalah <b>satu-satunya sumber akun</b> bagi seluruh siklus kas besar. Tidak ada
 * satu pun layar posting yang menentukan akunnya sendiri; semuanya menelusuri
 * <code>dokumen.getKasBesar().getJenisKasBesar().getAkunXxx()</code>. Dua kolom akun yang tersedia
 * dipakai sebagai berikut (TERVERIFIKASI dari kode pemanggil dan dari label layar, bukan ditebak
 * dari nama kolom):</p>
 *
 * <ul>
 *   <li>{@link #getAkun()} &mdash; label layar <i>&quot;Akun Sumber Kas Besar *&quot;</i>, wajib
 *       diisi di layar ZK. Ini <b>akun KREDIT</b> (asal dana) pada setiap jurnal kas besar:
 *       {@code PostingKasBesarAction} memakainya sebagai {@code akunKredit} di keempat jalur
 *       postingnya, dan {@code DraftJurnalRingkasanUtil} memakai pasangan yang sama untuk pratinjau
 *       draft jurnal. Nilai akun ini juga menjadi <b>sumber data rekening bank</b> pengirim di
 *       {@code DaftarPengajuanTransfer}: bank, nomor rekening, dan nama pemilik rekening pengirim
 *       diambil dari {@code jenisKasBesar.getAkun().getBank()/.getNoRek()/.getAtasNama()}.
 *       Jadi salah memilih akun di sini bukan hanya menggeser jurnal, tetapi juga menggeser
 *       <b>rekening yang dipakai membayar</b>.</li>
 *   <li>{@link #getAkunPenerima()} &mdash; label layar <i>&quot;Akun Penerima Kas Besar *&quot;</i>,
 *       wajib diisi di layar ZK. Perannya berganti tergantung tahap siklus:
 *       <ul>
 *         <li>saat <b>pencairan kas besar</b> ({@code PostingKasBesarAction}) ia menjadi
 *             <b>akun DEBET</b> &mdash; kas besar diakui sebagai kas/aset yang dipegang unit;</li>
 *         <li>saat <b>LPJ kas besar</b> ({@code PostingPertangungjawabanKasBesarAction}) ia menjadi
 *             <b>akun KREDIT pokok</b> &mdash; kas yang tadi diakui dilepas kembali seiring belanja
 *             diakui. Lihat {@link ais.database.model.akunting.PertangungjawabanKasBesar}.</li>
 *       </ul>
 *       Pasangan debet/kredit inilah yang membuat siklus kas besar tertutup.</li>
 * </ul>
 *
 * <p>Ada satu jalur khusus: bila dokumen {@link KasBesar} ditandai diambil dari kas kecil
 * (<i>pengisian ulang</i>), {@code PostingKasBesarAction} memakai akun {@code JenisKasKecil}
 * sebagai debet dan akun katalog ini ({@link #getAkun()}) sebagai kredit &mdash;
 * {@link #getAkunPenerima()} <b>tidak dipakai</b> pada jalur itu.</p>
 *
 * <p>Konsekuensinya: <b>mengubah satu baris katalog ini mengubah akun buku besar</b> setiap dokumen
 * kas besar dan LPJ kas besar <i>yang belum diposting</i> yang menunjuk baris tersebut &mdash;
 * mesin posting membaca akun secara <i>live</i> saat tombol Posting ditekan; tidak ada potret akun
 * yang disimpan di dokumen. Pola retroaktif yang sama sudah dicatat pada
 * {@link ais.database.model.akunting.JenisUangMuka} dan pada tarif di
 * {@link ais.database.model.akunting.Pajak}.</p>
 *
 * <h3>Hubungan ke dokumen</h3>
 *
 * <p>Tidak ada koleksi anak di kelas ini; arah relasinya selalu dari dokumen ke katalog:</p>
 * <ul>
 *   <li>{@link KasBesar} menunjuk katalog ini lewat kolom FK <code>kas_besar.jenis_kas_besar</code>.
 *       Getter di sana ({@code KasBesar.getJenisKasBesar()}) <b>berefek samping</b>: bila jenis
 *       masih kosong sementara satuan kerja sudah terisi, ia memanggil
 *       {@link #ambilDefault(SatuanKerja)} dan menulis hasilnya ke field &mdash; nilai bawaan itu
 *       ikut tersimpan ke kolom FK pada flush berikutnya. Jadi sekadar <i>membaca</i> dokumen kas
 *       besar dapat menetapkan jenisnya secara permanen.</li>
 *   <li>{@link ais.database.model.akunting.PertangungjawabanKasBesar} (LPJ kas besar)
 *       <b>tidak</b> punya FK sendiri ke katalog ini. Ia menjangkaunya secara tidak langsung lewat
 *       dokumen hulunya: <code>pertangungjawabanKasBesar.getKasBesar().getJenisKasBesar()
 *       .getAkunPenerima()</code> &mdash; itulah akun kredit pokok jurnal LPJ. Rantai tiga langkah
 *       ini berarti LPJ kas besar <b>mewarisi</b> akun dari dokumen kas besarnya; mengubah jenis di
 *       dokumen kas besar setelah LPJ dibuat (selama LPJ belum diposting) ikut mengubah jurnal LPJ.</li>
 *   <li>{@link DaftarPengajuanTransfer} membaca katalog ini lewat <b>dua</b> jalur sekaligus
 *       (langsung dari {@code getKasBesar()}, atau lewat {@code getPertangungjawabanKasBesar()
 *       .getKasBesar()}) untuk menentukan rekening pengirim &mdash; lihat catatan pada
 *       {@link #getAkun()}.</li>
 * </ul>
 *
 * <h3>Klarifikasi penting: tidak ada pagu, tidak ada saldo</h3>
 *
 * <p>Entity ini <b>TIDAK memiliki kolom pagu, plafon, kuota, maupun saldo</b> &mdash; sudah
 * diperiksa lapangan demi lapangan. Katalog ini <b>tidak menegakkan batas nominal apa pun</b>:
 * tidak ada plafon per jenis, tidak ada akumulasi terpakai, tidak ada penolakan bila pengajuan kas
 * besar melampaui suatu ambang. Kesimpulan yang sama sudah dicatat untuk
 * {@link ais.database.model.akunting.JenisUangMuka}.</p>
 *
 * <p>Yang mudah menyesatkan adalah {@link #getTanggal()}, yang di layar ZK diberi label
 * <i>&quot;Tanggal Saldo Awal *&quot;</i>. <b>Tidak ada saldo awal untuk ditanggali.</b> Kelas
 * kembarnya {@code JenisKasKecil} memang punya sepasang kolom <code>saldo_awal</code> +
 * <code>tanggal</code>, dan saat kelas ini disalin dari sana kolom <code>saldo_awal</code>
 * ditinggalkan sementara <code>tanggal</code> beserta labelnya ikut terbawa. Hasilnya kolom
 * <code>tanggal</code> di sini adalah <b>sisa salin-tempel</b>: hanya ditulis dan dibaca oleh layar
 * masternya sendiri ({@code JenisKasBesarAction} &mdash; kolom grid dan datebox form), dan
 * <b>nol pembaca</b> di seluruh mesin posting, laporan, maupun REST. Jalur REST bahkan tidak pernah
 * mengisinya sama sekali.</p>
 *
 * <h3>Struktur data</h3>
 *
 * <p>Selain dua kolom akun di atas: {@link #getKode()} (kode bebas, dipakai untuk pengurutan grid
 * dan sebagai kunci layar revisi), {@link #getNama()} (wajib, <code>nullable = false</code>),
 * {@link #getKeterangan()}, {@link #getSatuanKerja()} (unit pemilik &mdash; lihat catatan cakupan
 * di bawah), {@link #getTanggal()} (lihat paragraf sebelumnya), {@link #getAktif()}, dan
 * {@link #getDefaultData()} (penanda &quot;jenis bawaan&quot; unit, dibaca
 * {@link #ambilDefault(SatuanKerja)}). Kolom jejak audit {@link #getOleh()} /
 * {@link #getOlehId()} / {@link #getTanggal_dirubah()} diisi oleh {@link #onUpdate()} lewat
 * {@code AuditTimestampInterceptor}.</p>
 *
 * <p>Kelas ini <code>extends</code> {@link ais.database.model.GeneralValueObject}. Base class itu
 * <b>bukan</b> <code>@Entity</code> maupun <code>@MappedSuperclass</code> &mdash; hanya POJO
 * abstrak biasa &mdash; sehingga Hibernate tidak memetakan properti apa pun miliknya. Karena itu
 * <code>id</code>, <code>oleh</code>, <code>olehId</code>, dan <code>tanggal_dirubah</code>
 * <b>wajib dideklarasikan ulang di sini</b>; pengulangan itu keharusan teknis, bukan duplikasi yang
 * perlu dirapikan. Yang diwarisi dan benar-benar dipakai adalah util statis
 * {@link ais.database.model.GeneralValueObject#check(Object)} (de-proxy lazy) yang dipanggil
 * seluruh getter relasi di kelas ini.</p>
 *
 * <p>Pemetaan memakai <b>property access</b> (anotasi berada di getter), dengan
 * <code>dynamicInsert</code>/<code>dynamicUpdate</code> aktif dan <code>@Audited</code> (Envers)
 * sehingga setiap versi baris digandakan ke tabel revisi <code>jenis_kas_besar_aud</code> &mdash;
 * itulah yang menyalakan tombol riwayat pada kolom Kode di grid master
 * ({@code RevisiHelper.createNewRevisi}). Kombinasi property access + Envers berarti nilai yang
 * <i>dikembalikan getter</i>-lah yang tersimpan dan terarsip, bukan nilai mentah field &mdash;
 * relevan untuk {@link #getKode()}/{@link #getNama()} yang melakukan <code>trim()</code> dan untuk
 * getter relasi yang menulis balik hasil de-proxy ke fieldnya.</p>
 *
 * <h3>Pengelompokan method</h3>
 *
 * <ol>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Cache/bawaan statis</b> &mdash; {@link #DEFAULT_JENIS_KAS_BESAR},
 *       {@link #reloadDefault()}, {@link #ambilDefault(SatuanKerja)}.</li>
 *   <li><b>Identitas &amp; deskripsi</b> &mdash; {@link #getId()}, {@link #getKode()},
 *       {@link #getNama()}, {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Akun buku besar</b> &mdash; {@link #getAkun()}, {@link #getAkunPenerima()} beserta
 *       setter-nya.</li>
 *   <li><b>Cakupan, status &amp; periode</b> &mdash; {@link #getSatuanKerja()},
 *       {@link #getAktif()}, {@link #getDefaultData()}, {@link #getTanggal()} beserta
 *       setter-nya.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Cakupan satuan kerja bersifat fail-open.</b> Setiap konsumen ZK memfilter dengan pola
 *       <code>Restrictions.or(Restrictions.isNull(&quot;satuanKerja&quot;), &hellip;)</code> &mdash;
 *       baik {@code JenisKasBesarAction.initCriteria} maupun combo jenis di {@code KasBesarAction}.
 *       Artinya baris yang <code>satuanKerja</code>-nya <code>null</code> <b>selalu terlihat dan
 *       selalu dapat dipilih oleh semua unit</b>, termasuk saat pengguna sudah dipagari ke unitnya
 *       sendiri. Jalur REST lebih longgar lagi: {@code MasterKeuanganApiHelper.daftar}
 *       (<code>SELECT &hellip; FROM public.jenis_kas_besar m WHERE 1 = 1</code>) dan
 *       {@code KasBesarApiHelper.opsi} membaca tabel ini <b>tanpa klausa satuan kerja sama
 *       sekali</b>.</li>
 *   <li><b>Baris baru dari layar ZK lahir dengan <code>aktif = null</code> dan
 *       <code>defaultData = null</code>.</b> {@code JenisKasBesarAction.onSave} menyetel satuan
 *       kerja, dua akun, tanggal, kode, nama, dan keterangan &mdash; tetapi <b>tidak pernah</b>
 *       memanggil {@link #setAktif(Boolean)} maupun {@link #setDefaultData(Boolean)}. Tiga konsumen
 *       menafsirkan <code>null</code> secara BERBEDA: {@link #getAktif()} dan filter layar master
 *       membacanya sebagai <i>aktif</i> (<code>isNull OR eq(true)</code>), REST memakai
 *       <code>COALESCE(aktif,true)</code> (juga aktif), tetapi combo jenis pada formulir dokumen
 *       kas besar ({@code KasBesarAction}) memakai <code>Restrictions.eq(&quot;aktif&quot;, true)</code>
 *       <b>ketat</b> sehingga baris <code>aktif = null</code> <b>tidak pernah muncul di
 *       dropdown</b>. Efek yang terlihat pengguna: jenis baru tampak &quot;Aktif&nbsp;&#10003;&quot;
 *       di layar master tetapi tidak bisa dipilih saat membuat dokumen kas besar, sampai seseorang
 *       menoggle checkbox Aktif di grid (yang menulis nilai boolean sungguhan). Apakah gejala ini
 *       muncul di suatu instalasi bergantung pada ada tidaknya <code>DEFAULT true</code> pada kolom
 *       di DDL &mdash; <code>dynamicInsert</code> membuat kolom null dihilangkan dari INSERT
 *       sehingga DEFAULT database sempat berlaku.</li>
 *   <li><b>Tidak ada jaminan keunikan &quot;bawaan&quot;.</b> Tidak ada constraint maupun kode yang
 *       mencegah dua baris ber-<code>defaultData = true</code> pada satuan kerja yang sama;
 *       {@link #ambilDefault(SatuanKerja)} mengambil yang pertama ditemui saat iterasi {@code Map}
 *       cache, sehingga akun jurnal yang terpilih untuk dokumen kas besar baru bisa <b>berbeda antar
 *       restart</b>. Checkbox &quot;Default&quot; di grid master juga menyimpan langsung per baris
 *       tanpa mematikan penanda bawaan pada baris lain.</li>
 *   <li><b>Checkbox Aktif dan Default di grid master tidak dipagari hak akses.</b> Di
 *       {@code JenisKasBesarAction.JenisKasBesarRenderer.render(...)} tombol Ubah/Hapus dibungkus
 *       {@code Common.copyEditDeleteButtons(edit, delete, &hellip;)} sehingga mengikuti hak UPDATE
 *       dan DELETE, tetapi kedua checkbox ditambahkan <b>tanpa syarat</b> dan listener
 *       {@code onCheck}-nya langsung memanggil {@code Common.refreshSaveOrUpdate}. Pengguna yang
 *       hanya berhak READ atas halaman ini karenanya tetap dapat <b>memindahkan penanda
 *       &quot;Default&quot;</b> &mdash; yaitu memindahkan akun buku besar yang akan dipakai seluruh
 *       dokumen kas besar berikutnya di unit tersebut &mdash; atau <b>menonaktifkan</b> jenis yang
 *       sedang dipakai. Ini perubahan bermuatan finansial lewat gerbang baca saja.</li>
 *   <li><b>{@link #reloadDefault()} bukan sekadar memuat cache</b> &mdash; ia menulis ke database
 *       (menyemai satu baris bila tabel kosong) dan <b>menutup sesi Hibernate thread saat ini</b>,
 *       padahal satu-satunya pemanggilnya berada di tengah request ZK. Lihat Javadoc method
 *       tersebut; ini butir paling berisiko di kelas ini.</li>
 *   <li><b>{@link #DEFAULT_JENIS_KAS_BESAR} adalah cache mati</b> &mdash; ditulis, tidak pernah
 *       dibaca siapa pun. Lihat Javadoc fieldnya.</li>
 *   <li><b>Berbeda dari saudara kembarnya, katalog ini tidak dipramuat penuh saat start.</b>
 *       {@code InitData} memang mendaftarkan kelas ini ke pramuat {@code MemoryCacheUtil}
 *       (sehingga {@link #ambilDefault(SatuanKerja)} punya isi), tetapi daftar
 *       {@code InitData.reloadDefaults()} <b>hanya</b> memanggil {@code JenisKasKecil.reloadDefault()}
 *       dan {@code JenisUangMuka.reloadDefault()} &mdash; <b>tidak</b> {@link #reloadDefault()}.
 *       Jadi penyemaian baris bawaan &quot;001 / Kas Besar&quot; tidak pernah terjadi saat startup,
 *       melainkan baru mungkin terjadi pada penyimpanan pertama lewat layar master.</li>
 *   <li><b>Tiga jalur tulis, bukan satu.</b> Selain layar ZK
 *       {@code /WEB-INF/z/x/y/pages/master/akunting/jenis_kas_besar.zul}, katalog ini dapat
 *       dibuat/diubah/dihapus lewat (a) REST {@code PosApi} aksi <code>master_keuangan_*</code> yang
 *       ditangani {@code MasterKeuanganApiHelper} dengan tipe <code>jenis_kas_besar</code>, dan
 *       (b) CRUD generik {@code DynamicJspCrudGenerator.generate(JenisKasBesar.class)} lewat
 *       {@code /WEB-INF/baru/modul/pagesmasterakuntingjeniskasbesarzul/index.jsp}. Penjaga di jalur
 *       REST, <code>bolehAksi()</code>, <b>fail-open</b>: bila {@code Tbmuser.hakAkses()}
 *       mengembalikan <code>null</code> (pengguna tanpa peran) fungsi itu langsung
 *       <code>return true</code> untuk create/update/delete, dan aksi
 *       <code>master_keuangan_daftar</code> tidak memeriksa hak baca sama sekali. Lihat catatan
 *       keamanan pada {@link #setAkunPenerima(Akun)}.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Halaman ZUL yang sama disisipkan sebagai tab di
 *       dalam layar dokumen Kas Besar ({@code KasBesarAction.onKasBesar} membuat
 *       {@code MyInclude("/pages/master/akunting/jenis_kas_besar.zul")}). Karena
 *       {@code CommonPrivilages.checkPrevilages} membaca atribut sesi <code>currentMenu</code> yang
 *       tidak di-resolve ulang untuk halaman ter-include, hak yang berlaku di tab itu adalah hak
 *       atas menu <i>Kas Besar</i>, bukan atas menu <i>Akun Kas Besar</i>. Siapa pun yang boleh
 *       membuka layar dokumen kas besar praktis memperoleh CRUD atas katalog akun ini. Halaman ini
 *       tetap punya entri menunya sendiri, jadi pola ini menambah jalur, bukan menggantikannya.</li>
 *   <li><b>Perubahan lewat REST tidak menyegarkan cache.</b> {@code MasterKeuanganApiHelper.simpan}
 *       tidak memanggil {@link #reloadDefault()} dan tidak menyentuh {@code MemoryCacheUtil},
 *       sehingga {@link #ambilDefault(SatuanKerja)} dapat terus mengembalikan akun lama sampai
 *       restart berikutnya. Jalur REST juga tidak pernah menulis {@link #setTanggal(Date)} maupun
 *       {@link #setDefaultData(Boolean)}.</li>
 * </ol>
 *
 * <h3>Silsilah salin-tempel</h3>
 *
 * <p>Kelas ini satu cetakan hbm2java dengan {@code JenisKasKecil} dan
 * {@link ais.database.model.akunting.JenisUangMuka}: {@code serialVersionUID}-nya <b>identik</b>
 * ({@code 2463821577548439808L}) &mdash; nilai yang sama juga dipakai {@link KasBesar},
 * {@code KasKecil}, {@link ais.database.model.akunting.Pertangungjawaban}, dan
 * {@link ais.database.model.akunting.PertangungjawabanKasBesar}. Yang membedakan ketiga katalog itu
 * hanya jumlah dan makna kolom akunnya (kas besar: <code>akun</code> + <code>akun_penerima</code>;
 * kas kecil: <code>akun</code> + <code>akun_penutup_kas_kecil</code> + <code>saldo_awal</code>;
 * uang muka: <code>akun</code> + <code>akun_kelebihan</code> + <code>akun_sponsor</code>).
 * Jejak salinan masih terlihat di badan {@link #ambilDefault(SatuanKerja)}, yang variabel
 * perulangannya bernama <code>jenisUangMuka</code>.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.akunting.KasBesar
 * @see ais.database.model.akunting.PertangungjawabanKasBesar
 * @see ais.database.model.akunting.JenisUangMuka
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.rab.SatuanKerja
 * @see ais.action.master.akunting.JenisKasBesarAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_kas_besar")
public class JenisKasBesar extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Entity ini melintasi batas serialisasi karena disimpan di sesi ZK dan di cache in-memory
	 * {@code MemoryCacheUtil}. Nilai ini <b>tidak boleh diubah</b> selama perubahan struktur masih
	 * kompatibel, agar objek yang sudah terlanjur diserialisasi tetap dapat dibaca kembali.</p>
	 *
	 * <p>Nilainya sama persis dengan yang dipakai {@code JenisKasKecil},
	 * {@link ais.database.model.akunting.JenisUangMuka}, {@link KasBesar}, dan
	 * {@link ais.database.model.akunting.PertangungjawabanKasBesar} &mdash; jejak bahwa seluruh
	 * kelas itu lahir dari satu cetakan hbm2java yang sama, bukan tanda bahwa mereka sekerabat
	 * secara pewarisan.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama, di-generate database (kolom <code>id</code>). Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir kali mengubah baris katalog ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()} melalui
	 * {@code AuditTimestampInterceptor.ubah(this)} pada setiap UPDATE. Karena kelas ini tidak
	 * memiliki hook <code>@PrePersist</code>, kolom ini <b>kosong pada baris yang baru dibuat</b>
	 * dan baru terisi pada perubahan berikutnya.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau <code>null</code> bila baris belum pernah diubah
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam.</b> Argumen <code>null</code> atau yang hanya
	 * berisi spasi diabaikan &mdash; nilai lama dipertahankan dan tidak ada exception yang
	 * dilempar. Jejak audit karenanya hanya bisa ditimpa oleh identitas baru yang valid, tidak bisa
	 * dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: argumen <code>null</code> atau berisi spasi saja
	 * diabaikan tanpa error, sehingga jejak audit tidak dapat dihapus lewat setter.</p>
	 *
	 * @param oleh nama pengguna pengubah; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir kali mengubah baris katalog ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()}. Kosong pada baris yang baru dibuat (tidak ada
	 * hook <code>@PrePersist</code> di kelas ini).</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau <code>null</code>
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA <code>@PreUpdate</code>: menstempel jejak audit tepat sebelum UPDATE dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari identitas pengguna sesi berjalan. Dipanggil oleh <b>provider JPA</b>, bukan oleh kode
	 * aplikasi &mdash; jangan memanggilnya langsung.</p>
	 *
	 * <p><b>Hanya berlaku untuk UPDATE.</b> Tidak ada pasangan <code>@PrePersist</code>, sehingga
	 * baris yang baru dibuat tidak berstempel siapa pembuatnya; kolom <code>oleh</code>/
	 * <code>oleh_id</code> baru terisi pada perubahan berikutnya. Bila jalur penulisnya bukan
	 * request pengguna (mis. REST tanpa sesi ZK, atau penyemaian di {@link #reloadDefault()}),
	 * interceptor dapat tidak menemukan identitas dan setter yang menolak nilai kosong membuat
	 * jejak lama bertahan apa adanya.</p>
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field <code>tanggal_dirubah</code>
	 * &mdash; diinisialisasi ke waktu sekarang ({@code WaktuUtil.getDate()}) saat objek dibuat,
	 * sehingga instance baru selalu punya timestamp meski belum pernah disimpan. Susunan satu baris
	 * ini adalah gaya generator, bukan sesuatu yang bermakna.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Setter polos; normalnya diisi {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini (kolom <code>tanggal_dirubah</code>, presisi TIMESTAMP).
	 *
	 * <p>Berbeda dari kolom audit lainnya, field ini <b>tidak pernah null</b>: nilai awalnya
	 * ditetapkan pada deklarasi field ke waktu pembuatan objek Java.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris katalog dalam format <code>id-nama</code>.
	 *
	 * <p>Dipakai antara lain oleh komponen pemilih akun/jenis di layar ZK dan oleh log. Perhatikan
	 * bahwa yang dirangkai adalah <b>field</b> <code>nama</code> secara langsung (bukan
	 * {@link #getNama()}), sehingga hasilnya tidak di-<code>trim()</code>, dan bahwa yang
	 * ditampilkan di depan adalah <b>id numerik</b> &mdash; bukan {@link #getKode()} seperti yang
	 * biasa diharapkan pengguna. Untuk baris yang belum tersimpan hasilnya berawalan
	 * <code>null-</code>.</p>
	 *
	 * @return gabungan <code>id + "-" + nama</code>
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Cache statis &quot;jenis kas besar bawaan&quot; global.
	 *
	 * <p><b>Cache mati.</b> Field ini hanya <i>ditulis</i> oleh {@link #reloadDefault()} dan
	 * <b>tidak pernah dibaca</b> oleh satu pun kelas di repositori ini &mdash; sudah diperiksa.
	 * Pemilihan jenis bawaan yang sungguh dipakai dilakukan per satuan kerja lewat
	 * {@link #ambilDefault(SatuanKerja)}, yang membaca cache {@code MemoryCacheUtil} dan sama sekali
	 * tidak menyentuh field ini.</p>
	 *
	 * <p>Karena bersifat <code>public static</code> dan mutable, field ini juga menyimpan satu
	 * entity Hibernate melintasi request dan melintasi thread tanpa sinkronisasi. Entity yang
	 * ditahan di sana sudah lepas dari sesi yang membuatnya (sesi ditutup di akhir
	 * {@link #reloadDefault()}), jadi seandainya suatu saat ada yang membacanya, relasi lazy-nya
	 * hanya akan hidup lewat {@link ais.database.model.GeneralValueObject#check(Object)}.</p>
	 */
	public static JenisKasBesar DEFAULT_JENIS_KAS_BESAR = null;

	/**
	 * Memuat ulang penanda jenis kas besar bawaan global dan, bila tabel masih kosong,
	 * <b>menyemai satu baris ke database</b>.
	 *
	 * <p><b>Cara kerja.</b> Mengambil sesi native thread saat ini
	 * ({@code HibernateUtil.currentNativeSession()}), mengambil <b>satu</b> baris
	 * {@code JenisKasBesar} dengan <code>id</code> terkecil, dan menyimpannya di
	 * {@link #DEFAULT_JENIS_KAS_BESAR}. Bila tidak ada baris sama sekali, sebuah instance baru
	 * dibuat dengan kode <code>&quot;001&quot;</code>, nama dan keterangan
	 * <code>&quot;Kas Besar&quot;</code>, {@code aktif = true}, lalu <b>disimpan dalam transaksi
	 * yang dibuka dan di-commit di dalam method ini</b>. Terakhir
	 * {@code HibernateUtil.closeSession()} dipanggil tanpa syarat.</p>
	 *
	 * <p><b>Baris semaian itu tidak siap pakai.</b> Ia lahir tanpa {@link #getAkun()}, tanpa
	 * {@link #getAkunPenerima()}, tanpa {@link #getSatuanKerja()}, dan dengan
	 * {@code defaultData = null}. Konsekuensinya baris tersebut tidak akan pernah dikembalikan
	 * {@link #ambilDefault(SatuanKerja)} (yang mensyaratkan <code>defaultData = true</code>), dan
	 * dokumen kas besar yang terlanjur memakainya tidak dapat dijurnal karena akunnya kosong.
	 * Nilainya semata sebagai baris pengisi agar layar tidak kosong.</p>
	 *
	 * <p><b>Kapan dipanggil.</b> Hanya dari satu tempat di seluruh repositori:
	 * {@code JenisKasBesarAction.onSave(...)}, tepat setelah {@code Common.refreshSaveOrUpdate} dan
	 * {@code session.flush()}. Berbeda dari saudara kembarnya, {@code InitData.reloadDefaults()}
	 * <b>tidak</b> memanggil method ini saat aplikasi start (di sana hanya ada
	 * {@code JenisKasKecil.reloadDefault()} dan {@code JenisUangMuka.reloadDefault()}). Jalur REST
	 * {@code MasterKeuanganApiHelper} juga tidak memanggilnya. Karena satu-satunya pemanggil selalu
	 * baru saja menyimpan sebuah baris, cabang penyemaian di sini praktis tidak pernah menyala.</p>
	 *
	 * <p><b>Efek samping yang perlu diwaspadai &mdash; penutupan sesi di tengah request ZK.</b>
	 * Javadoc {@code HibernateUtil.currentNativeSession()} menyatakan tegas bahwa method itu
	 * <i>&quot;JANGAN dipakai di konteks request ZK &mdash; di situ pakai currentSession() dan
	 * JANGAN tutup manual&quot;</i>, sedangkan {@code closeSession()} melakukan
	 * <code>clear()</code> + <b>rollback</b> + <code>disconnect()</code> + <code>close()</code>.
	 * Method ini melanggar kontrak itu: satu-satunya pemanggilnya berada persis di tengah request
	 * ZK, dan pada konfigurasi di mana sesi ZK dan sesi native thread merupakan objek yang sama
	 * (ZK 9/10 CE, yaitu saat {@code currentSession()} jatuh ke {@code currentNativeSession()}),
	 * penutupan di sini membatalkan transaksi request yang dibuka Open-Session-In-View &mdash;
	 * termasuk penyimpanan yang baru saja di-<code>flush()</code> namun belum di-commit. Bila suatu
	 * saat muncul laporan &quot;data tersimpan tapi hilang setelah refresh&quot; atau
	 * {@code SessionException: Session is closed!} pada layar ini, mulailah dari sini. Jangan
	 * menambah pemanggil baru dari dalam request ZK.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		DEFAULT_JENIS_KAS_BESAR = (JenisKasBesar) session.createCriteria(JenisKasBesar.class).setMaxResults(1)
				.addOrder(Order.asc("id")).uniqueResult();
		if (DEFAULT_JENIS_KAS_BESAR == null) {
			DEFAULT_JENIS_KAS_BESAR = new JenisKasBesar();
			DEFAULT_JENIS_KAS_BESAR.setKode("001");
			DEFAULT_JENIS_KAS_BESAR.setAktif(true);
			DEFAULT_JENIS_KAS_BESAR.setNama("Kas Besar");
			DEFAULT_JENIS_KAS_BESAR.setKeterangan("Kas Besar");
			session.getTransaction().begin();
			session.save(DEFAULT_JENIS_KAS_BESAR);
			session.getTransaction().commit();
		}
		HibernateUtil.closeSession();
	}

	/**
	 * Mencari jenis kas besar <b>bawaan</b> untuk sebuah satuan kerja, dari cache in-memory.
	 *
	 * <p><b>Tujuan.</b> Memberi jawaban atas pertanyaan &quot;akun apa yang dipakai bila pengguna
	 * tidak memilih jenis kas besar?&quot;. Inilah yang membuat dokumen {@link KasBesar} tetap bisa
	 * dijurnal meski pembuatnya tidak menyentuh combo Jenis.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengambil seluruh baris {@code JenisKasBesar} dari cache
	 * {@code MemoryCacheUtil} lewat
	 * {@link ais.common.ConstantValues#ambilBerdasarClass(Class)} (dipramuat saat start oleh
	 * {@code InitData.initClasses(...)} &mdash; <b>bukan</b> query database, jadi method ini tidak
	 * membuka sesi maupun transaksi), lalu memilih baris pertama yang memenuhi:</p>
	 * <ul>
	 *   <li>bila <code>satuanKerja</code> argumen <code>null</code> <b>atau</b> id-nya
	 *       <code>null</code> &mdash; baris yang {@link #getSatuanKerja()}-nya <code>null</code>
	 *       <b>dan</b> {@link #getDefaultData()}-nya <code>true</code> (jenis bawaan global);</li>
	 *   <li>selain itu &mdash; baris yang satuan kerjanya ber-id sama dengan argumen <b>dan</b>
	 *       {@link #getDefaultData()}-nya <code>true</code>. Perhatikan: pada cabang ini baris
	 *       bawaan global (<code>satuanKerja = null</code>) <b>tidak</b> ikut dipertimbangkan,
	 *       sehingga unit yang belum punya jenis bawaan sendiri mendapat <code>null</code>, bukan
	 *       jatuh ke bawaan global.</li>
	 * </ul>
	 *
	 * <p><b>Hal yang perlu diketahui.</b>
	 * (1) <b>Urutan iterasi {@code Map} tidak dijamin</b> dan tidak ada constraint yang mencegah
	 * dua baris bawaan pada satuan kerja yang sama &mdash; bila itu terjadi, akun jurnal yang
	 * terpilih dapat berbeda antar restart.
	 * (2) Cache dibaca apa adanya: perubahan lewat jalur REST tidak menyegarkannya, sehingga
	 * hasilnya bisa basi sampai restart berikutnya.
	 * (3) Baris yang <code>aktif = false</code> <b>tidak disaring</b> di sini &mdash; jenis yang
	 * sudah dinonaktifkan tetap dapat terpilih sebagai bawaan.
	 * (4) Ekspresi <code>getSatuanKerja().getId().equals(...)</code> akan melempar
	 * {@code NullPointerException} bila ada baris cache yang satuan kerjanya tersimpan tanpa id.
	 * (5) Variabel perulangan bernama <code>jenisUangMuka</code> &mdash; sisa salin-tempel dari
	 * {@link ais.database.model.akunting.JenisUangMuka}, tanpa makna.</p>
	 *
	 * <p><b>Siapa yang memanggil.</b> Satu pemanggil di seluruh repositori:
	 * {@code KasBesar.getJenisKasBesar()}. Karena getter itu menulis hasilnya kembali ke field,
	 * nilai yang dikembalikan method ini <b>ikut tersimpan permanen</b> ke kolom
	 * <code>kas_besar.jenis_kas_besar</code> pada flush berikutnya.</p>
	 *
	 * @param satuanKerja unit yang jenis bawaannya dicari; boleh <code>null</code> (atau ber-id
	 *                    <code>null</code>) untuk mencari jenis bawaan global
	 * @return jenis kas besar bawaan yang cocok, atau <code>null</code> bila tidak ada
	 */
	@SuppressWarnings("unchecked")
	public static JenisKasBesar ambilDefault(SatuanKerja satuanKerja) {

		Map<Long, JenisKasBesar> mapJenisKasBesar = ConstantValues.ambilBerdasarClass(JenisKasBesar.class);

		if (satuanKerja == null || satuanKerja.getId() == null) {
			for (JenisKasBesar jenisUangMuka : mapJenisKasBesar.values()) {
				if (jenisUangMuka.getSatuanKerja() == null && jenisUangMuka.getDefaultData()) {
					return jenisUangMuka;
				}
			}
		} else {
			for (JenisKasBesar jenisUangMuka : mapJenisKasBesar.values()) {
				if (jenisUangMuka.getSatuanKerja() != null
						&& jenisUangMuka.getSatuanKerja().getId().equals(satuanKerja.getId())
						&& jenisUangMuka.getDefaultData()) {
					return jenisUangMuka;
				}
			}
		}

		return null;
	}

	/** Kode bebas katalog; dipakai pengurutan grid dan kunci layar revisi. Lihat {@link #getKode()}. */
	private String kode;

	/** Akun sumber (kredit) kas besar, sekaligus sumber rekening pengirim. Lihat {@link #getAkun()}. */
	private Akun akun;

	/** Akun penerima kas besar &mdash; debet saat pencairan, kredit saat LPJ. Lihat {@link #getAkunPenerima()}. */
	private Akun akunPenerima;

	/** Nama jenis kas besar; wajib. Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Unit pemilik katalog; <code>null</code> berarti terlihat oleh semua unit. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/** Tanggal berlabel &quot;Saldo Awal&quot; yang tidak punya saldo &mdash; sisa salin-tempel. Lihat {@link #getTanggal()}. */
	private Date tanggal;

	/** Penanda baris masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Penanda &quot;jenis bawaan&quot; unit. Lihat {@link #getDefaultData()}. */
	private Boolean defaultData;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Menghasilkan instance kosong: seluruh kolom <code>null</code> kecuali
	 * <code>tanggal_dirubah</code> yang diinisialisasi ke waktu sekarang pada deklarasi fieldnya.
	 * Perhatikan bahwa instance baru berarti {@link #getAktif()} mengembalikan <code>true</code>
	 * dan {@link #getTanggal()} mengembalikan hari ini (keduanya nilai pengganti dari getter),
	 * sementara kolom di database tetap <code>null</code> &mdash; lihat catatan nomor 2 pada
	 * Javadoc kelas.</p>
	 */
	public JenisKasBesar() {
	}

	/**
	 * Kunci utama baris katalog (kolom <code>id</code>, IDENTITY).
	 *
	 * <p><code>insertable = false</code>: nilainya sepenuhnya ditentukan database dan baru terisi
	 * setelah INSERT ter-flush. Dipakai sebagai kunci cache {@code MemoryCacheUtil} yang dibaca
	 * {@link #ambilDefault(SatuanKerja)}, dan sebagai pengurut pada {@link #reloadDefault()}.</p>
	 *
	 * @return id baris, atau <code>null</code> bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Setter polos; normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode katalog, sudah di-<code>trim()</code>.
	 *
	 * <p><b>Tidak pernah mengembalikan <code>null</code></b> &mdash; kolom kosong menjadi string
	 * kosong. Perilaku ini berbeda dari {@link #getNama()} yang tetap mengembalikan
	 * <code>null</code>, jadi jangan menganggap kedua getter itu setara saat menulis penjaga
	 * null.</p>
	 *
	 * <p>Karena pemetaan memakai property access, nilai hasil <code>trim()</code> inilah yang
	 * ditulis ke kolom dan diarsipkan Envers &mdash; spasi di ujung yang diketik pengguna tidak
	 * pernah sampai ke database. Kode ini dipakai sebagai pengurutan grid master
	 * ({@code Order.asc("kode")}), sebagai kunci tampilan widget revisi, dan ikut menjadi label
	 * combo jenis pada formulir dokumen kas besar.</p>
	 *
	 * <p>Tidak ada constraint keunikan pada kolom ini; dua baris berkode sama diizinkan.</p>
	 *
	 * @return kode katalog yang sudah di-trim, atau string kosong bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode katalog. Setter polos (tanpa trim &mdash; pemangkasan terjadi di getter).
	 *
	 * @param kode kode katalog; boleh <code>null</code>
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama jenis kas besar, sudah di-<code>trim()</code>.
	 *
	 * <p>Kolomnya <code>nullable = false</code> di tingkat pemetaan, dan layar ZK menolak simpan
	 * bila nama kosong; jalur REST {@code MasterKeuanganApiHelper.simpan} juga menolak nama kosong.
	 * Meski begitu getter ini tetap dapat mengembalikan <code>null</code> untuk instance yang belum
	 * diisi (mis. objek baru dari tombol Tambah).</p>
	 *
	 * <p>Nama inilah yang tampil di grid master, di combo jenis pada formulir kas besar, dan di
	 * dasbor {@code MonitorKasBesarDashboard}.</p>
	 *
	 * @return nama katalog yang sudah di-trim, atau <code>null</code> bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis kas besar. Setter polos.
	 *
	 * @param nama nama katalog
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas atas baris katalog (kolom <code>keterangan</code>, opsional).
	 *
	 * <p>Dikembalikan apa adanya tanpa <code>trim()</code>. Ditampilkan sebagai kolom tersendiri di
	 * grid master dan ikut menjadi bagian label combo jenis pada formulir dokumen kas besar; tidak
	 * dipakai logika bisnis apa pun.</p>
	 *
	 * @return keterangan, atau <code>null</code>
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Setter polos.
	 *
	 * @param keterangan keterangan; boleh <code>null</code>
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Penanda baris katalog masih boleh dipakai.
	 *
	 * <p><b>Menganggap <code>null</code> sebagai aktif</b> &mdash; nilai pengganti ini
	 * <b>tidak</b> ditulis balik ke field, jadi kolomnya tetap <code>NULL</code> di database
	 * sementara layar memperlihatkan centang Aktif.</p>
	 *
	 * <p><b>Penafsiran <code>null</code> berbeda-beda antar konsumen</b>, dan ini sumber keluhan
	 * yang nyata: filter grid master memakai <code>isNull OR eq(true)</code> (aktif), REST memakai
	 * <code>COALESCE(aktif,true)</code> (aktif), tetapi combo jenis pada formulir dokumen kas besar
	 * ({@code KasBesarAction}) memakai <code>Restrictions.eq("aktif", true)</code> yang
	 * <b>ketat</b>. Karena {@code JenisKasBesarAction.onSave} tidak pernah memanggil
	 * {@link #setAktif(Boolean)}, jenis yang baru dibuat lewat layar master dapat tampak aktif di
	 * layar master namun tidak muncul di dropdown dokumen sampai checkbox Aktif di grid ditoggle.
	 * Lihat catatan nomor 2 pada Javadoc kelas.</p>
	 *
	 * @return <code>true</code> bila aktif atau belum pernah ditentukan; <code>false</code> hanya
	 *         bila secara eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif. Setter polos.
	 *
	 * <p>Dipanggil dari checkbox &quot;Aktif&quot; di grid master (yang langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate} <b>tanpa memeriksa hak UPDATE</b> &mdash; lihat catatan
	 * nomor 4 pada Javadoc kelas), dari jalur REST {@code MasterKeuanganApiHelper.simpan}, dan dari
	 * {@link #reloadDefault()}. Formulir tambah/ubah di layar master <b>tidak</b> memanggilnya.</p>
	 *
	 * @param aktif status aktif; <code>null</code> diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * <b>Akun sumber kas besar</b> (kolom <code>akun</code>) &mdash; label layar
	 * <i>&quot;Akun Sumber Kas Besar *&quot;</i>.
	 *
	 * <p><b>Peran di jurnal.</b> Ini akun <b>KREDIT</b> pada setiap jurnal pencairan kas besar:
	 * dana keluar dari sini. {@code PostingKasBesarAction} memakainya sebagai {@code akunKredit} di
	 * seluruh cabang postingnya, dan {@code DraftJurnalRingkasanUtil} memakainya untuk pratinjau
	 * draft jurnal yang sama. Pada jalur pengisian ulang kas kecil, akun ini tetap menjadi kredit
	 * sementara debetnya diambil dari {@code JenisKasKecil}.</p>
	 *
	 * <p><b>Peran non-akuntansi yang mudah terlewat.</b> Akun ini juga menjadi sumber
	 * <b>identitas rekening pengirim</b> di {@link DaftarPengajuanTransfer}: bank, nomor rekening,
	 * dan nama pemilik rekening diambil dari {@code getAkun().getBank()},
	 * {@code getAkun().getNoRek()}, dan {@code getAkun().getAtasNama()} &mdash; baik lewat dokumen
	 * {@link KasBesar} langsung maupun lewat
	 * {@link ais.database.model.akunting.PertangungjawabanKasBesar}. Mengganti akun di sini karena
	 * alasan pembukuan berarti ikut mengganti rekening yang dipakai membayar.</p>
	 *
	 * <p><b>Efek samping getter.</b> Memanggil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan <b>menulis hasilnya kembali
	 * ke field</b> &mdash; instance yang dikembalikan bisa berbeda dari proxy semula (kanonik dari
	 * {@code EntityIdentityMap}, dari cache, atau hasil reload). Itu keharusan pola de-proxy, bukan
	 * kesalahan; yang perlu diingat hanyalah bahwa membaca getter ini mengubah isi field. Berbeda
	 * dari beberapa entity lain di paket ini, getter ini <b>tidak</b> mengganti akun dengan akun
	 * lain (tidak ada pola <i>write-back</i> destruktif semacam {@code Transaksi.getAkun()}).</p>
	 *
	 * <p>Relasi lazy, cascade {@code PERSIST}/{@code MERGE}, kolom <code>nullable = true</code> di
	 * tingkat pemetaan &mdash; kewajiban pengisian hanya ditegakkan layar ZK. Jalur REST
	 * mengizinkan akun kosong dengan sengaja (agar admin dapat melengkapi bertahap) dan menandai
	 * barisnya sebagai <code>akunLengkap = false</code>. Dokumen yang memakai jenis tanpa akun
	 * <b>dilewati diam-diam</b> oleh mesin posting.</p>
	 *
	 * @return akun sumber, atau <code>null</code> bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel akun sumber kas besar. Setter polos.
	 *
	 * <p><b>Berdampak finansial.</b> Karena mesin posting membaca akun secara <i>live</i>, nilai
	 * yang disetel di sini menentukan akun kredit seluruh dokumen kas besar yang menunjuk baris ini
	 * dan <b>belum</b> diposting, sekaligus rekening bank pengirim pada pengajuan transfer.</p>
	 *
	 * <p>Pemanggil: formulir layar master ({@code JenisKasBesarAction.onSave}, dengan validasi
	 * wajib) dan jalur REST {@code MasterKeuanganApiHelper.simpan} lewat parameter
	 * <code>akunId</code> (tanpa validasi wajib).</p>
	 *
	 * @param akun akun sumber; boleh <code>null</code>
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Unit kerja pemilik baris katalog (kolom <code>satuan_kerja</code>, opsional).
	 *
	 * <p><b>Ini pembatas cakupan yang fail-open, bukan pemagar yang tegas.</b> Baris dengan
	 * <code>satuanKerja = null</code> terlihat dan dapat dipilih oleh <b>semua</b> unit, karena
	 * setiap konsumen ZK memfilter dengan pola
	 * <code>Restrictions.or(Restrictions.isNull("satuanKerja"), &hellip;)</code>. Jalur REST
	 * ({@code MasterKeuanganApiHelper.daftar}, {@code KasBesarApiHelper.opsi}) tidak memfilter
	 * satuan kerja sama sekali, sehingga seluruh katalog lintas unit terbaca lewat API.</p>
	 *
	 * <p>Nilai ini juga menjadi kunci pencarian {@link #ambilDefault(SatuanKerja)}. Di layar ZK
	 * pengisiannya wajib dan otomatis terisi dari konteks pengguna ({@code Common.getSatuanKerja()})
	 * untuk baris baru; jalur REST membolehkannya kosong.</p>
	 *
	 * <p><b>Efek samping getter:</b> de-proxy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dengan penulisan balik ke field.
	 * Relasi lazy, cascade {@code PERSIST}/{@code MERGE}.</p>
	 *
	 * @return unit pemilik, atau <code>null</code> bila berlaku untuk semua unit
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel unit kerja pemilik. Setter polos.
	 *
	 * <p>Menyetel <code>null</code> berarti menjadikan baris ini berlaku untuk seluruh unit &mdash;
	 * lihat catatan fail-open pada {@link #getSatuanKerja()}.</p>
	 *
	 * @param satuanKerja unit pemilik; boleh <code>null</code>
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Tanggal katalog (kolom <code>tanggal</code>, TIMESTAMP), berlabel
	 * <i>&quot;Tanggal Saldo Awal *&quot;</i> di layar master.
	 *
	 * <p><b>Labelnya menyesatkan: kelas ini tidak punya kolom saldo awal.</b> Sepasang kolom
	 * <code>saldo_awal</code> + <code>tanggal</code> ada di {@code JenisKasKecil}; saat kelas ini
	 * disalin dari sana hanya <code>tanggal</code> yang ikut terbawa. Nilainya <b>tidak dibaca oleh
	 * siapa pun</b> selain layar masternya sendiri ({@code JenisKasBesarAction}: satu kolom grid
	 * dan satu datebox pada formulir) &mdash; nol pembaca di mesin posting, laporan, dasbor, maupun
	 * REST, dan jalur REST tidak pernah mengisinya.</p>
	 *
	 * <p><b>Mengembalikan hari ini bila kosong</b>, dan nilai pengganti itu <b>tidak</b> ditulis
	 * balik ke field. Akibatnya kolom dapat tetap <code>NULL</code> di database sementara grid
	 * selalu memperlihatkan tanggal hari ini &mdash; dan dua pembacaan pada hari berbeda
	 * menghasilkan tampilan berbeda untuk baris yang sama. Karena pemetaan memakai property access,
	 * nilai pengganti itu juga akan ikut <b>tersimpan</b> begitu baris tersebut disimpan ulang lewat
	 * jalur apa pun (termasuk toggle checkbox Aktif/Default di grid), sehingga kolom perlahan
	 * terisi tanggal penyimpanan, bukan tanggal yang bermakna.</p>
	 *
	 * @return tanggal katalog, atau tanggal hari ini bila kolomnya kosong (tidak pernah
	 *         <code>null</code>)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	/**
	 * Menyetel tanggal katalog. Setter polos.
	 *
	 * <p>Satu-satunya pemanggil adalah formulir layar master
	 * ({@code JenisKasBesarAction.onSave} dari datebox &quot;Tanggal Saldo Awal&quot;). Jalur REST
	 * tidak pernah memanggilnya. Nilainya tidak dipakai logika bisnis apa pun &mdash; lihat
	 * {@link #getTanggal()}.</p>
	 *
	 * @param tanggal tanggal katalog; boleh <code>null</code>
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * <b>Akun penerima kas besar</b> (kolom <code>akun_penerima</code>) &mdash; label layar
	 * <i>&quot;Akun Penerima Kas Besar *&quot;</i>.
	 *
	 * <p><b>Peran di jurnal berganti sesuai tahap siklus</b> (TERVERIFIKASI dari kode posting):</p>
	 * <ul>
	 *   <li><b>Pencairan kas besar</b> &mdash; {@code PostingKasBesarAction} menambahkannya ke
	 *       daftar <b>akun DEBET</b> (kas besar diakui sebagai kas yang dipegang unit), berpasangan
	 *       dengan {@link #getAkun()} sebagai kredit. Bila kolom ini kosong, layar posting
	 *       menampilkan pesan &quot;Transaksi tidak valid&quot; dan dokumen tidak dapat
	 *       dijurnal.</li>
	 *   <li><b>Pertanggungjawaban (LPJ) kas besar</b> &mdash;
	 *       {@code PostingPertangungjawabanKasBesarAction} memakainya sebagai <b>akun KREDIT
	 *       pokok</b>, dijangkau lewat rantai
	 *       <code>pertangungjawabanKasBesar.getKasBesar().getJenisKasBesar().getAkunPenerima()</code>.
	 *       Sisi debetnya justru <b>tidak</b> berasal dari katalog ini melainkan dari kunci
	 *       <code>workspace</code> pada JSON {@code formula} dokumen kas besar hulunya. Lihat
	 *       {@link ais.database.model.akunting.PertangungjawabanKasBesar}.</li>
	 *   <li><b>Pengisian ulang dari kas kecil</b> &mdash; kolom ini <b>tidak dipakai</b>; debet
	 *       diambil dari akun {@code JenisKasKecil}.</li>
	 * </ul>
	 *
	 * <p>Karena LPJ kas besar tidak menyimpan potret akun sendiri, <b>mengubah kolom ini mengubah
	 * kredit pokok setiap LPJ yang belum diposting</b> yang bermuara ke baris katalog ini.</p>
	 *
	 * <p><b>Efek samping getter:</b> de-proxy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dengan penulisan balik ke field
	 * (bukan penggantian akun). Relasi lazy, cascade {@code PERSIST}/{@code MERGE}, kolom
	 * <code>nullable = true</code> di pemetaan &mdash; kewajiban hanya ditegakkan layar ZK.</p>
	 *
	 * @return akun penerima, atau <code>null</code> bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_penerima", nullable = true)
	public Akun getAkunPenerima() {
		akunPenerima = check(akunPenerima);
		return akunPenerima;
	}

	/**
	 * Menyetel akun penerima kas besar. Setter polos.
	 *
	 * <p><b>Berdampak finansial.</b> Nilai di sini menentukan akun debet jurnal pencairan kas besar
	 * dan akun kredit pokok jurnal LPJ kas besar untuk semua dokumen terkait yang belum diposting.
	 * Tidak ada validasi tipe akun (bagan akun tidak dibatasi ke kelompok kas/bank), sehingga akun
	 * apa pun dari {@link Akun} dapat dipasang di sini.</p>
	 *
	 * <p><b>Catatan keamanan &mdash; jalur tulis yang tidak setara.</b> Selain formulir layar master
	 * (yang mewajibkan pengisian dan berada di balik hak menu), setter ini dijangkau oleh REST
	 * {@code MasterKeuanganApiHelper.simpan} tipe <code>jenis_kas_besar</code> lewat parameter
	 * <code>akunKeduaId</code>. Penjaga di sana, <code>bolehAksi()</code>, <b>fail-open</b>: bila
	 * {@code Tbmuser.hakAkses()} mengembalikan <code>null</code> (pengguna tanpa peran) fungsi itu
	 * langsung <code>return true</code> untuk create/update/delete alih-alih menolak, dan aksi
	 * <code>master_keuangan_daftar</code> tidak memeriksa hak baca sama sekali. Karena kolom inilah
	 * yang menentukan akun jurnal kas besar dan LPJ-nya, ini bukan sekadar celah master data.</p>
	 *
	 * @param akunPenerima akun penerima; boleh <code>null</code>
	 */
	public void setAkunPenerima(Akun akunPenerima) {
		this.akunPenerima = akunPenerima;
	}

	/**
	 * Penanda bahwa baris ini adalah <b>jenis bawaan</b> bagi satuan kerjanya.
	 *
	 * <p><b>Menganggap <code>null</code> sebagai <code>false</code></b> (kebalikan dari
	 * {@link #getAktif()}), dan nilai pengganti itu tidak ditulis balik ke field. Karena
	 * {@code JenisKasBesarAction.onSave} tidak pernah memanggil {@link #setDefaultData(Boolean)},
	 * baris yang baru dibuat lewat formulir <b>tidak pernah</b> menjadi bawaan sampai checkbox
	 * &quot;Default&quot; di grid ditoggle.</p>
	 *
	 * <p>Satu-satunya pembaca yang bermakna adalah {@link #ambilDefault(SatuanKerja)}, yang
	 * hasilnya dipakai {@code KasBesar.getJenisKasBesar()} untuk mengisi jenis dokumen yang masih
	 * kosong &mdash; dan penulisannya bersifat permanen. Jadi penanda ini menentukan <b>akun buku
	 * besar mana yang dipakai dokumen kas besar yang pembuatnya tidak memilih jenis</b>. Tidak ada
	 * constraint yang mencegah dua baris bawaan pada satu satuan kerja; bila itu terjadi, yang
	 * terpilih adalah yang pertama ditemui saat iterasi cache.</p>
	 *
	 * @return <code>true</code> bila baris ini jenis bawaan unitnya; <code>false</code> bila tidak
	 *         atau belum ditentukan
	 */
	public Boolean getDefaultData() {
		return defaultData == null ? false : defaultData;
	}

	/**
	 * Menyetel penanda jenis bawaan. Setter polos &mdash; <b>tidak</b> mematikan penanda pada baris
	 * lain di satuan kerja yang sama.
	 *
	 * <p>Satu-satunya pemanggil di aplikasi adalah listener {@code onCheck} checkbox
	 * &quot;Default&quot; pada grid master, yang langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate} <b>tanpa memeriksa hak UPDATE</b> (berbeda dari tombol
	 * Ubah/Hapus di baris yang sama, yang dipagari). Karena penanda ini menentukan akun jurnal
	 * dokumen kas besar berikutnya, perubahan bermuatan finansial ini terbuka bagi pengguna yang
	 * hanya berhak membaca halaman. Formulir tambah/ubah maupun jalur REST tidak pernah menyentuh
	 * kolom ini.</p>
	 *
	 * @param defaultData penanda jenis bawaan; <code>null</code> diperlakukan sebagai
	 *                    <code>false</code> oleh {@link #getDefaultData()}
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}
}
