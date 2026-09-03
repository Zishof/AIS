package ais.database.model.payroll;

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
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;

/**
 * Katalog master <b>&quot;Cara Pembayaran Gaji&quot;</b> &mdash; daftar cara/kanal yang dipakai
 * organisasi untuk membayarkan gaji pegawai (Tunai, Transfer Bank BNI, Transfer Bank Syariah,
 * dan seterusnya), sekaligus <b>pemetaan masing-masing cara itu ke satu akun buku besar</b>
 * ({@link Akun}) yang akan <b>dikredit</b> saat dokumen gaji diposting ke jurnal.
 *
 * <p>Tabel fisiknya <code>payroll.cara_pembayaran_gaji</code>. Baris di sini <b>bukan</b> dokumen
 * dan tidak pernah memuat nominal: ia murni referensi yang dirujuk oleh header dokumen gaji
 * {@link ais.database.model.payroll.PembayaranGaji} lewat properti
 * {@code caraPembayaranGaji} (kolom <code>cara_pembayaran_gaji</code>).</p>
 *
 * <h2>Peran di dalam alur penggajian (terverifikasi dari kode)</h2>
 *
 * <ol>
 *   <li><b>Pemilihan saat membuat dokumen gaji.</b> Empat layar ZK mengisi combo &quot;Cara
 *       Bayar&quot; dari kelas ini: {@code PembayaranGajiAction}, {@code BayarGajiPegawaiAction},
 *       {@code PostingTransaksiPembayaranGajiAction}, dan
 *       {@code PostingTransaksiPenggajianAction}. Keempatnya memakai <b>filter yang identik</b>
 *       (salin-tempel): baris harus <code>aktif</code> (atau <code>aktif IS NULL</code>),
 *       <b>harus punya <code>akun</code></b> (<code>Restrictions.isNotNull(&quot;akun&quot;)</code>),
 *       dan satuan kerjanya harus <code>NULL</code> atau sama dengan satuan kerja konteks
 *       (pegawai yang dibayar, atau {@code Common.getSatuanKerja()}).</li>
 *   <li><b>Sumber akun kredit saat posting.</b> {@code PostingTransaksiPembayaranGajiAction} dan
 *       {@code PostingTransaksiPenggajianAction} membaca
 *       {@code pembayaranGaji.getCaraPembayaranGaji().getAkun()} dan memasukkannya sebagai
 *       <b>kaki kredit</b> jurnal gaji (kas/bank yang berkurang). Baris ini karena itu ikut
 *       menentukan <i>akun buku besar resmi</i> yang tersentuh, bukan sekadar label.</li>
 *   <li><b>Sumber data surat perintah transfer.</b>
 *       {@code ais.database.model.akunting.StandingInstruction} menarik bank sumber, atas nama,
 *       dan nomor rekening dari <code>getAkun().getBank()</code> /
 *       <code>getAkun().getAtasNama()</code> / <code>getAkun().getNoRek()</code>, dan menyusun
 *       nama batch dari <code>getNama()</code>. Isi surat perintah transfer gaji dengan demikian
 *       dibaca <b>hidup</b> dari katalog ini (bukan snapshot): mengubah rekening pada
 *       {@link Akun} yang dirujuk akan mengubah surat perintah untuk dokumen gaji yang sudah
 *       lama dibuat.</li>
 *   <li><b>Penyemaian awal.</b> {@code InitData} mendaftarkan kelas ini ke {@code initClasses(...)}
 *       dan memanggil {@link #reloadDefault()} pada saat startup.</li>
 * </ol>
 *
 * <h2>Perbandingan dengan {@link ais.database.model.akunting.CaraPembayaranTransfer}</h2>
 *
 * <p>Kedua kelas adalah <b>kembar salin-tempel</b>: {@code serialVersionUID}-nya <b>identik</b>
 * (<code>2463821577548439808L</code>), urutan field sama persis, dan pola
 * {@code DEFAULT_JENIS_PEMBAYARAN} + {@link #reloadDefault()} (termasuk baris bawaan
 * &quot;Tunai&quot;/&quot;Bayar Tunai&quot;) sama kata demi kata. Kesamaan
 * {@code serialVersionUID} ini pola yang sama dengan {@code Pertangungjawaban} vs
 * {@code PertangungjawabanKasBesar}. Perbedaan yang benar-benar ada:</p>
 * <ul>
 *   <li>{@code CaraPembayaranTransfer} punya <b>dua</b> relasi akun (<code>akun</code> dan
 *       <code>akunTransitori</code>, untuk mekanisme transitori dua langkah); kelas ini hanya
 *       punya <code>akun</code> &mdash; pembayaran gaji tidak mengenal akun transitori;</li>
 *   <li>layar master {@code CaraPembayaranTransfer} adalah <i>tab</i> di dalam layar
 *       &quot;Daftar Pengajuan Transfer&quot; sehingga mewarisi hak menu induk; layar master kelas
 *       ini adalah <b>menu tersendiri</b> (lihat catatan menu ganda di bawah);</li>
 *   <li>pada {@code CaraPembayaranTransfer}, mencentang &quot;Default&quot; ikut <b>mematikan
 *       default baris lain</b>; di sini <b>tidak</b> &mdash; lihat &quot;Bendera default tidak
 *       eksklusif&quot;.</li>
 * </ul>
 *
 * <h2>Hal non-obvious yang perlu diketahui sebelum menyunting</h2>
 *
 * <h3>1. Bendera default tidak eksklusif, dan pemilihnya sewenang-wenang</h3>
 *
 * <p>Tidak ada indeks unik, tidak ada penjaga aplikasi, dan renderer grid
 * {@code CaraPembayaranGajiAction.CaraPembayaranGajiRenderer} hanya melakukan
 * {@code setDefaultPembayaran(...)} lalu {@code Common.refreshSaveOrUpdate(...)} pada baris yang
 * dicentang &mdash; baris lain tidak disentuh. <b>Beberapa baris bisa berstatus default sekaligus.</b>
 * Semua pembaca default kemudian memakai <code>setMaxResults(1).uniqueResult()</code> pada criteria
 * <b>tanpa <code>Order</code></b>, sehingga baris mana yang terpilih ditentukan urutan yang
 * dikembalikan basis data. Akibat praktisnya: akun kas/bank yang dikredit pada jurnal gaji bisa
 * berpindah diam-diam antar-pemuatan halaman tanpa satu pun data diubah pengguna.</p>
 *
 * <h3>2. {@link #reloadDefault()} menulis ke basis data, dan baris yang ditulisnya tidak terpakai</h3>
 *
 * <p>{@link #reloadDefault()} bukan sekadar pemuat cache: bila tidak ada baris berbendera default,
 * ia <b>menyimpan baris baru</b> &quot;Tunai&quot; ke tabel. Baris hasil semai itu punya
 * <code>akun == null</code> dan <code>satuanKerja == null</code>, padahal <b>seluruh</b> combo
 * konsumen mensyaratkan <code>akun IS NOT NULL</code> &mdash; jadi baris bawaan tersebut
 * <b>tidak pernah muncul sebagai pilihan di layar manapun</b>. Ia hanya menempati tabel dan
 * membuat pemanggilan {@link #reloadDefault()} berikutnya berhenti menyemai.</p>
 *
 * <h3>3. Cache statis {@link #DEFAULT_JENIS_PEMBAYARAN} tidak punya pembaca</h3>
 *
 * <p>Penelusuran menyeluruh atas repo menemukan pembacaan
 * <code>DEFAULT_JENIS_PEMBAYARAN</code> hanya untuk {@code JenisPembayaran} dan
 * {@code CaraPembayaranTransfer}; <b>tidak ada satu pun</b> pembaca untuk field statis milik kelas
 * ini di luar {@link #reloadDefault()} sendiri. Field ini efektif <i>write-only</i>. Semua layar
 * konsumen justru mengulang query defaultnya sendiri, masing-masing dengan filter satuan kerja
 * (yang tidak dipunyai {@link #reloadDefault()}) &mdash; sehingga cache statis, andai dipakai,
 * akan membocorkan baris default milik satuan kerja lain ke seluruh JVM.</p>
 *
 * <h3>4. Cakupan satuan kerja: dua jalur, dua semantik, satu fail-open</h3>
 *
 * <p>Kolom <code>satuan_kerja</code> adalah <b>satu-satunya</b> sumbu tenant kelas ini (tidak ada
 * <code>yayasan</code>/<code>sekolah</code>/<code>program</code>). Dua jalur membacanya berbeda:</p>
 * <ul>
 *   <li><b>ZK</b> ({@code CaraPembayaranGajiAction.initCriteria()}): baris ber-<code>satuanKerja</code>
 *       <code>NULL</code> selalu ikut terlihat (katalog global), dan bila
 *       {@code SekolahUtil.ambilSatuanKerjas()} mengembalikan himpunan <b>kosong</b> &mdash; yang
 *       terjadi ketika pengguna tidak punya daftar satuan kerja <i>dan</i> tidak punya yayasan
 *       &mdash; restriksi jatuh ke <code>sqlRestriction(&quot;1=1&quot;)</code> sehingga
 *       <b>SELURUH baris seluruh tenant</b> tampil dan dapat disunting. Ini instans lain dari pola
 *       fail-open cakupan yang sudah berulang kali dicatat di inisiatif ini;</li>
 *   <li><b>Generic CRUD v2</b> ({@code GenericCrudAutoEntityAdapter}): <code>applyScope()</code>
 *       memasang <code>Restrictions.eq(&quot;satuanKerja&quot;, user.getSatuanKerja())</code>
 *       &mdash; kesetaraan, <b>bukan</b> &quot;atau NULL&quot;. Baris katalog global karena itu
 *       <b>tidak terlihat sama sekali</b> di New UI meski terlihat di ZK. Dan bila
 *       <code>user.getSatuanKerja()</code> bernilai <code>null</code>, {@code addScope()} keluar
 *       lebih awal sehingga <b>tidak ada restriksi apa pun</b> yang terpasang &mdash; kembali
 *       fail-open lintas tenant.</li>
 * </ul>
 *
 * <h3>5. Keterjangkauan Generic CRUD v2 dan permukaan REST</h3>
 *
 * <p>Entity ini <b>terjangkau</b> New UI Generic CRUD v2: berkas
 * <code>WEB-INF/new/payroll/uiux/cara_pembayaran_gaji.jsp</code> dan
 * <code>.../services/cara_pembayaran_gaji_service.jsp</code> mendeklarasikan
 * <code>nuiEntityCandidates = {&quot;CaraPembayaranGaji&quot;}</code> dan
 * <code>nuiSourceClass = &quot;CaraPembayaranGajiAction&quot;</code>. Karena
 * {@code CaraPembayaranGajiAction} punya konstruktor default, field komponen ZK,
 * {@code boolean onSave(Event)}, dan {@code init(GeneralValueObject)}, definisi yang dibangun
 * {@code GenericCrudAutoDefinitionFactory} lolos {@code GenericCrudExistingActionInvoker.supports()}
 * sehingga mode-nya <b>FULL CRUD</b> (nama kelas juga tidak tertangkap satu pun
 * <code>BLOCKED_CLASS_TOKENS</code> &mdash; token <code>payment</code>/<code>bank</code> tidak
 * cocok dengan &quot;pembayaran&quot; maupun &quot;payroll&quot;). Selain itu entity ini terdaftar
 * di <i>admin model browser</i> ({@code listAdministrativeModels()} yang mengenumerasi seluruh
 * subclass {@link GeneralValueObject} terpetakan).</p>
 *
 * <p><b>Verifikasi negatif yang menenangkan:</b> katalog ini <b>tidak</b> termasuk tujuh master
 * keuangan yang dilayani {@code MasterKeuanganApiHelper} (yang berisi {@code JenisUangMuka},
 * {@code JenisKasKecil}, {@code JenisKasBesar}, {@code JenisReimbursement},
 * {@code JenisPengeluaran}, {@code KategoriBiayaSales}, dan &mdash; justru &mdash;
 * {@code CaraPembayaranTransfer}). Pola fail-open <code>bolehAksi()</code> peran-null yang
 * terkonfirmasi di sepuluh helper API <b>tidak menjangkau</b> kelas ini; tidak ada permukaan REST
 * &quot;PosApi&quot;/ApiHelper untuk cara pembayaran gaji sama sekali.</p>
 *
 * <h3>6. Checkbox &quot;Aktif&quot;/&quot;Default&quot; di grid tanpa gerbang hak</h3>
 *
 * <p>Pada baris grid yang sama, tombol Ubah/Hapus dipagari
 * ({@code Common.copyEditDeleteButtons(edit, delete, ...)} dengan <code>edit</code>/<code>delete</code>
 * dari {@code CommonPrivilages.checkPrevilages(UPDATE/DELETE)}), tetapi <b>kedua checkbox
 * menyimpan langsung ke basis data tanpa pemeriksaan hak apa pun</b>. Pengguna berhak
 * <i>baca saja</i> tetap dapat menonaktifkan seluruh cara pembayaran (melumpuhkan penggajian)
 * atau memindahkan bendera default (mengganti akun kas/bank yang dikredit jurnal gaji). Pola
 * identik dengan yang sudah dicatat pada {@code JenisKasBesar} dan
 * {@code CaraPembayaranTransfer}.</p>
 *
 * <h3>7. Dua menu berbeda menunjuk satu layar yang sama</h3>
 *
 * <p>{@code MenuSnapshotData} memetakan <b>dua</b> entri menu ke
 * <code>/pages/master/payroll/cara_pembayaran_gaji.zul</code>: &quot;Cara Pembayaran Gaji&quot;
 * (id 94173) dan &quot;Cara Pembayaran Absensi&quot; (id 892658). Karena
 * {@code CommonPrivilages.checkPrevilages()} membaca atribut sesi <code>currentMenu</code>, hak
 * CREATE/UPDATE/DELETE yang berlaku atas katalog ini <b>berbeda tergantung menu mana yang dipakai
 * masuk</b> &mdash; memberi hak pada salah satu menu berarti memberi hak atas data yang sama.</p>
 *
 * <h3>8. Hubungan dengan bug write-back {@code PembayaranGaji.getSatuanKerja()}</h3>
 *
 * <p>Perlu ditegaskan karena mudah tertukar: <b>getter di kelas ini tidak destruktif</b>.
 * {@link #getAkun()} dan {@link #getSatuanKerja()} hanya memanggil
 * {@code check(...)} milik {@link GeneralValueObject} untuk meresolusi proxy lazy, lalu
 * menugaskan kembali hasilnya ke field yang sama &mdash; nilai tidak berubah. Yang destruktif
 * adalah {@code PembayaranGaji.getSatuanKerja()}, yang <b>menimpa</b> satuan kerja dokumen dengan
 * <code>getCaraPembayaranGaji().getSatuanKerja()</code> setiap kali dibaca. Kelas ini adalah
 * <b>sumber</b> nilai penimpa itu, bukan pelakunya. Konsekuensi yang perlu diingat saat menyunting
 * baris di sini: mengubah <code>satuanKerja</code> satu baris katalog akan <b>memindahkan seluruh
 * dokumen gaji yang memakainya ke satuan kerja lain</b>, diam-diam, pada pembacaan berikutnya
 * (pemetaan <i>property access</i> + <code>dynamicUpdate</code> membuat penimpaan itu ikut
 * tersimpan saat sesi di-flush). Layar master di sini tidak membatasi pilihan satuan kerja ke
 * subpohon milik pengguna, sehingga pemindahan itu dapat diarahkan ke tenant mana pun.</p>
 *
 * <h2>Catatan pemetaan Hibernate</h2>
 *
 * <p>Kelas memakai <b>akses properti</b> (anotasi JPA ada di getter), <code>dynamicInsert</code> +
 * <code>dynamicUpdate</code>, dan <code>@Audited</code> (Envers &mdash; setiap versi baris
 * digandakan ke <code>cara_pembayaran_gaji_aud</code>, dan tabel revisi itulah yang dibaca tombol
 * kode di grid lewat {@code RevisiHelper.createNewRevisi}). Hanya sebagian getter yang
 * beranotasi <code>@Column</code>/<code>@JoinColumn</code> ({@code id}, {@code nama},
 * {@code keterangan}, {@code akun}, {@code satuanKerja}); sisanya ({@code kode},
 * {@code deskripsi}, {@code defaultPembayaran}, {@code aktif}) tetap dipetakan dengan nama kolom
 * bawaan karena tidak ada <code>@Transient</code>.</p>
 *
 * <p><b>Efek samping akses properti pada getter bersubstitusi.</b> Karena Hibernate membaca nilai
 * lewat getter, tiga getter di bawah menormalkan nilai secara <b>permanen</b> pada flush
 * berikutnya: {@link #getNama()} me-<code>trim()</code> spasi, {@link #getAktif()} mengubah
 * <code>NULL</code> menjadi <code>true</code>, dan {@link #getDefaultPembayaran()} mengubah
 * <code>NULL</code> menjadi <code>false</code>. Ketiganya searah dengan semantik query konsumen
 * (<code>aktif IS NULL OR aktif = true</code>), jadi normalisasi ini tidak merusak &mdash; tetapi
 * ia berarti membuka satu baris untuk dilihat saja sudah cukup untuk menghasilkan baris baru di
 * tabel revisi Envers.</p>
 *
 * <p><b>{@link GeneralValueObject} bukan {@code @Entity} maupun {@code @MappedSuperclass}</b>
 * &mdash; ia POJO abstrak biasa dan Hibernate tidak memetakan properti apa pun darinya. Karena itu
 * field jejak audit ({@code oleh}, {@code olehId}, {@code tanggal_dirubah}) yang juga muncul di
 * banyak entity lain <b>harus</b> dideklarasikan ulang di sini; pengulangan itu keharusan teknis,
 * bukan duplikasi yang perlu &quot;dibersihkan&quot;. Yang diwarisi dari induk adalah
 * <i>perilaku</i>, terutama {@code check(...)} yang dipakai getter relasi untuk meresolusi proxy
 * lazy.</p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 *
 * <ul>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas &amp; label</b>: {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *       {@link #toString()}.</li>
 *   <li><b>Keterangan</b>: {@link #getKeterangan()}, {@link #getDeskripsi()}.</li>
 *   <li><b>Pemetaan akun jurnal</b>: {@link #getAkun()}.</li>
 *   <li><b>Bendera</b>: {@link #getAktif()}, {@link #getDefaultPembayaran()}.</li>
 *   <li><b>Cakupan organisasi</b>: {@link #getSatuanKerja()}.</li>
 *   <li><b>Cache statis + penyemaian</b>: {@link #DEFAULT_JENIS_PEMBAYARAN},
 *       {@link #reloadDefault()}.</li>
 * </ul>
 *
 * @see ais.database.model.payroll.PembayaranGaji
 * @see ais.database.model.akunting.CaraPembayaranTransfer
 * @see Akun
 * @see ais.database.model.rab.SatuanKerja
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "cara_pembayaran_gaji")
public class CaraPembayaranGaji extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya <b>identik</b> dengan
	 * {@code ais.database.model.akunting.CaraPembayaranTransfer} &mdash; jejak bahwa kedua kelas
	 * berasal dari satu salinan yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel <code>payroll.cara_pembayaran_gaji</code> (IDENTITY, diisi basis data). */
	private Long id;
	/** Nama pengguna terakhir yang menyunting baris ini; diisi interseptor audit. */
	private String oleh;
	/** Id pengguna terakhir yang menyunting baris ini; diisi interseptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyunting terakhir.
	 *
	 * <p><b>Perhatian:</b> argumen {@code null} atau string kosong/spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return}), sehingga nilai lama dipertahankan. Jejak audit karena itu
	 * tidak pernah bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyunting terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, argumen {@code null} atau
	 * kosong diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama pengguna penyunting terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-UPDATE, dan
	 * mendelegasikan pengisian jejak audit ({@code oleh}, {@code olehId}, {@code tanggal_dirubah})
	 * ke {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Tidak dipanggil manual dari kode aplikasi mana pun. Perhatikan bahwa kait ini
	 * <b>tidak</b> berjalan pada INSERT, hanya pada UPDATE.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris yang sama (gaya berkas
	 * asli); nilai awalnya adalah waktu server saat object dibuat, lewat
	 * {@code WaktuUtil.getDate()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir (tidak pernah {@code null} untuk object baru)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini, yaitu <b>{@link #getNama() nama} apa adanya</b>.
	 *
	 * <p>Dipakai komponen ZK (combo &quot;Cara Bayar&quot; lewat {@code Common.insertCombo(...)}
	 * dengan properti label {@code "nama"}) dan pesan log. Membaca field {@code nama} secara
	 * langsung &mdash; <b>bukan</b> lewat {@link #getNama()} &mdash; sehingga hasilnya
	 * <b>tidak</b> di-{@code trim()} dan bisa berupa {@code null} untuk baris yang namanya belum
	 * diisi.</p>
	 *
	 * @return nama cara pembayaran, atau {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Kode singkat cara pembayaran; dijaga unik oleh {@code CaraPembayaranGajiAction.checkKode()}, bukan oleh constraint basis data. */
	private String kode;

	/** Nama cara pembayaran (mis. &quot;Tunai&quot;, &quot;Transfer BNI&quot;); wajib diisi, dijaga unik di lapisan Action. */
	private String nama;
	/** Keterangan bebas; tidak ditampilkan di grid maupun form layar master. */
	private String keterangan;
	/** Akun buku besar yang <b>dikredit</b> saat dokumen gaji yang memakai cara ini diposting. */
	private Akun akun;
	/** Deskripsi bebas; ditampilkan sebagai kolom grid dan textarea pada form master. */
	private String deskripsi;
	/** Bendera &quot;cara pembayaran bawaan&quot;; <b>tidak</b> ditegakkan eksklusif (lihat Javadoc kelas). */
	private Boolean defaultPembayaran;
	/** Bendera aktif; baris non-aktif disaring dari seluruh combo konsumen. */
	private Boolean aktif;
	/** Satuan kerja pemilik baris; {@code null} berarti katalog global (terlihat semua satuan kerja di jalur ZK). */
	private SatuanKerja satuanKerja;

	/**
	 * Cache statis baris berbendera default, diisi {@link #reloadDefault()} saat startup.
	 *
	 * <p><b>Tidak punya pembaca.</b> Penelusuran menyeluruh repo hanya menemukan pembacaan
	 * <code>DEFAULT_JENIS_PEMBAYARAN</code> milik {@code JenisPembayaran} dan
	 * {@code CaraPembayaranTransfer}; field ini efektif <i>write-only</i>. Andai kelak dipakai, ia
	 * berbahaya: {@link #reloadDefault()} memilih baris <b>tanpa filter satuan kerja</b>, sehingga
	 * satu baris milik satu tenant akan dibagikan ke seluruh JVM.</p>
	 *
	 * <p>Bersifat {@code public} dan <b>mutable</b> &mdash; kode mana pun dapat menimpanya.</p>
	 *
	 * @see #reloadDefault()
	 */
	public static CaraPembayaranGaji DEFAULT_JENIS_PEMBAYARAN = null;

	/**
	 * Memuat ulang cache {@link #DEFAULT_JENIS_PEMBAYARAN} dan &mdash; bila belum ada baris
	 * berbendera default sama sekali &mdash; <b>menyemai satu baris baru ke basis data</b>.
	 *
	 * <p>Dipanggil dari {@code InitData.reloadDefaults()} pada startup aplikasi (di dalam
	 * {@code executor.submit(...)}, jadi berjalan di thread terpisah dari thread request).</p>
	 *
	 * <p><b>Alur:</b></p>
	 * <ol>
	 *   <li>ambil sesi native lewat {@code HibernateUtil.currentNativeSession()};</li>
	 *   <li>cari baris pertama dengan <code>defaultPembayaran = true</code>
	 *       (<code>setMaxResults(1)</code>, <b>tanpa <code>Order</code></b> &mdash; bila ada lebih
	 *       dari satu baris default, yang terpilih ditentukan basis data);</li>
	 *   <li>bila tidak ada, buat baris baru bernama &quot;Tunai&quot; berdeskripsi
	 *       &quot;Bayar Tunai&quot; dengan <code>defaultPembayaran = true</code>, lalu
	 *       {@code begin()} &ndash; {@code save()} &ndash; {@code commit()};</li>
	 *   <li>tutup sesi dengan {@code HibernateUtil.closeSession()}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping &amp; kuirk (terverifikasi):</b></p>
	 * <ul>
	 *   <li><b>Baris semaian tidak pernah terpakai.</b> Ia dibuat tanpa {@code akun} dan tanpa
	 *       {@code satuanKerja}, sedangkan seluruh combo konsumen mensyaratkan
	 *       <code>akun IS NOT NULL</code>. Baris itu hanya menghuni tabel dan mencegah penyemaian
	 *       berikutnya.</li>
	 *   <li><b>Pemilihan tanpa filter tenant.</b> Berbeda dari empat layar konsumen yang selalu
	 *       menyaring <code>satuanKerja IS NULL OR satuanKerja = &lt;konteks&gt;</code>, method ini
	 *       memungut baris default <b>milik satuan kerja mana pun</b>.</li>
	 *   <li><b>{@code closeSession()} tanpa syarat.</b> Bila method ini dipanggil dari konteks yang
	 *       sudah memegang sesi (mis. event ZK), sesi milik pemanggil ikut tertutup &mdash; pola
	 *       pelanggaran kontrak {@code HibernateUtil} yang sama sudah dicatat pada layar
	 *       Kas Besar/Kas Kecil/Uang Muka. Pada jalur startup {@code InitData} hal ini tidak
	 *       terasa karena tidak ada sesi lain yang aktif.</li>
	 *   <li><b>Tanpa penanganan exception.</b> Kegagalan query atau commit akan merambat keluar;
	 *       pada thread executor {@code InitData}, kegagalan itu tidak menghentikan startup tetapi
	 *       meninggalkan cache tetap {@code null}.</li>
	 * </ul>
	 *
	 * @see #DEFAULT_JENIS_PEMBAYARAN
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		DEFAULT_JENIS_PEMBAYARAN = (CaraPembayaranGaji) session.createCriteria(CaraPembayaranGaji.class)
				.add(Restrictions.eq("defaultPembayaran", true)).setMaxResults(1).uniqueResult();

		if (DEFAULT_JENIS_PEMBAYARAN == null) {
			DEFAULT_JENIS_PEMBAYARAN = new CaraPembayaranGaji();
			DEFAULT_JENIS_PEMBAYARAN.setNama("Tunai");
			DEFAULT_JENIS_PEMBAYARAN.setDeskripsi("Bayar Tunai");
			DEFAULT_JENIS_PEMBAYARAN.setDefaultPembayaran(true);
			session.getTransaction().begin();
			session.save(DEFAULT_JENIS_PEMBAYARAN);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
	}

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate, {@code CaraPembayaranGajiAction.onAdd()},
	 * dan {@code GenericCrudAutoEntityAdapter.createNew()}.
	 *
	 * <p>Keberadaannya ikut menentukan apakah entity ini boleh dibuat lewat Generic CRUD v2
	 * ({@code hasDefaultConstructor(entityClass)} pada {@code supportsCreate}).</p>
	 */
	public CaraPembayaranGaji() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data (strategi IDENTITY) dan <code>insertable = false</code>, jadi
	 * bernilai {@code null} sampai baris benar-benar tersimpan. Nilainya <b>tidak</b> di-namespace
	 * per tenant &mdash; urutan IDENTITY global untuk seluruh instalasi.</p>
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
	 * Menyetel kunci utama baris ini. Hanya dipakai Hibernate saat hidrasi.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode singkat cara pembayaran apa adanya (tanpa {@code trim()}).
	 *
	 * <p>Dipakai grid master sebagai label tombol revisi Envers dan sebagai kunci pengurutan
	 * ({@code Order.asc("kode")}). Keunikannya hanya dijaga
	 * {@code CaraPembayaranGajiAction.checkKode()} di lapisan aplikasi &mdash; tidak ada constraint
	 * basis data, sehingga jalur tulis lain (Generic CRUD v2, impor) dapat menghasilkan kode
	 * kembar.</p>
	 *
	 * @return kode cara pembayaran, atau {@code null}
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode singkat cara pembayaran.
	 *
	 * @param kode kode cara pembayaran
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama cara pembayaran, sudah di-{@code trim()}.
	 *
	 * <p>Karena pemetaan memakai <i>property access</i>, hasil {@code trim()} inilah yang dibaca
	 * Hibernate saat flush &mdash; spasi di ujung nama akan <b>terhapus permanen</b> di basis data
	 * pada penyimpanan berikutnya. Bandingkan dengan {@link #toString()} yang membaca field mentah
	 * tanpa {@code trim()}.</p>
	 *
	 * <p>Kolom dideklarasikan <code>nullable = false</code>; keunikan nama dijaga
	 * {@code CaraPembayaranGajiAction.checkNama()}, bukan basis data.</p>
	 *
	 * @return nama cara pembayaran tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama cara pembayaran.
	 *
	 * @param nama nama cara pembayaran
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * <p>Berbeda dari {@link #getDeskripsi()}, kolom ini <b>tidak</b> muncul di form maupun grid
	 * layar master ZK &mdash; hanya terisi lewat jalur lain (Generic CRUD v2, impor, atau data
	 * lama).</p>
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan keterangan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan akun buku besar yang dipetakan ke cara pembayaran ini.
	 *
	 * <p><b>Inilah properti terpenting kelas ini.</b> Akun yang dikembalikan dipakai sebagai
	 * <b>kaki kredit</b> jurnal gaji oleh {@code PostingTransaksiPembayaranGajiAction} dan
	 * {@code PostingTransaksiPenggajianAction} (kas/bank yang berkurang saat gaji dibayarkan), dan
	 * dipakai {@code StandingInstruction} sebagai sumber bank, atas nama, serta nomor rekening
	 * surat perintah transfer. Karena pembacaan itu <b>hidup</b> (bukan snapshot saat dokumen
	 * disimpan), mengubah pemetaan akun di sini akan mengubah akun yang dijurnal untuk dokumen gaji
	 * yang belum diposting <i>dan</i> mengubah isi surat perintah transfer dokumen lama yang
	 * dicetak ulang.</p>
	 *
	 * <p>Relasi <code>@ManyToOne</code> lazy dengan cascade {@code PERSIST}/{@code MERGE}. Baris
	 * pemanggilan {@code check(akun)} adalah resolusi proxy lazi standar {@link GeneralValueObject}
	 * &mdash; hasilnya ditugaskan kembali ke field yang sama dan <b>tidak mengubah nilai</b>.</p>
	 *
	 * <p>Kolom <code>nullable = true</code>, tetapi seluruh combo konsumen menyaring
	 * <code>akun IS NOT NULL</code> &mdash; baris tanpa akun tidak akan pernah dapat dipilih pada
	 * dokumen gaji.</p>
	 *
	 * @return akun buku besar yang dikredit, atau {@code null} bila belum dipetakan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel akun buku besar yang dipetakan ke cara pembayaran ini.
	 *
	 * <p>Dipanggil {@code CaraPembayaranGajiAction.onSave()} dari nilai banbox pemilih akun.
	 * Perubahan berlaku surut bagi seluruh dokumen gaji yang merujuk baris ini (lihat
	 * {@link #getAkun()}).</p>
	 *
	 * @param akun akun buku besar yang akan dikredit
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan deskripsi bebas baris ini.
	 *
	 * <p>Ditampilkan sebagai kolom kedua grid master dan textarea lima baris pada form.</p>
	 *
	 * @return deskripsi, atau {@code null}
	 */
	public String getDeskripsi() {
		return deskripsi;
	}

	/**
	 * Menyetel deskripsi bebas.
	 *
	 * @param deskripsi deskripsi cara pembayaran
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengembalikan bendera &quot;cara pembayaran bawaan&quot;, dengan {@code null} disubstitusi
	 * menjadi {@code false}.
	 *
	 * <p>Karena pemetaan <i>property access</i>, substitusi itu <b>ikut tersimpan</b>: baris yang
	 * kolomnya {@code NULL} akan tertulis {@code false} pada flush berikutnya. Arahnya searah
	 * dengan semantik query konsumen, jadi tidak merusak.</p>
	 *
	 * <p><b>Tidak eksklusif.</b> Tidak ada indeks unik maupun penjaga aplikasi yang mematikan
	 * bendera baris lain saat satu baris dijadikan default (berbeda dari
	 * {@code CaraPembayaranTransfer}). Semua pembaca default
	 * ({@link #reloadDefault()}, {@code PembayaranGajiAction}, {@code BayarGajiPegawaiAction},
	 * {@code PostingTransaksiPenggajianAction}) memakai
	 * <code>setMaxResults(1).uniqueResult()</code> tanpa {@code Order}, sehingga bila ada lebih
	 * dari satu default, baris terpilih &mdash; dan karenanya akun kas/bank yang dikredit jurnal
	 * gaji &mdash; ditentukan urutan yang kebetulan dikembalikan basis data.</p>
	 *
	 * @return {@code true} bila baris ini ditandai sebagai cara pembayaran bawaan
	 */
	public Boolean getDefaultPembayaran() {
		return defaultPembayaran == null ? false : defaultPembayaran;
	}

	/**
	 * Menyetel bendera &quot;cara pembayaran bawaan&quot;.
	 *
	 * <p>Dipanggil dari checkbox &quot;Default&quot; di grid master &mdash; dan checkbox itu
	 * menyimpan langsung lewat {@code Common.refreshSaveOrUpdate(...)} <b>tanpa pemeriksaan hak
	 * apa pun</b>, berbeda dari tombol Ubah/Hapus di baris yang sama yang dipagari
	 * {@code CommonPrivilages}. Method ini <b>tidak</b> mematikan bendera baris lain.</p>
	 *
	 * @param defaultPembayaran {@code true} untuk menandai baris ini sebagai bawaan
	 */
	public void setDefaultPembayaran(Boolean defaultPembayaran) {
		this.defaultPembayaran = defaultPembayaran;
	}

	/**
	 * Mengembalikan bendera aktif, dengan {@code null} disubstitusi menjadi {@code true}
	 * (&quot;anggap aktif&quot;).
	 *
	 * <p>Substitusi ini konsisten dengan filter yang dipakai seluruh konsumen
	 * (<code>aktif IS NULL OR aktif = true</code>), dan &mdash; karena pemetaan
	 * <i>property access</i> &mdash; ikut menormalkan kolom {@code NULL} menjadi {@code true} pada
	 * flush berikutnya.</p>
	 *
	 * <p>Menonaktifkan seluruh baris katalog secara efektif <b>melumpuhkan pembuatan dokumen
	 * gaji</b>, karena combo &quot;Cara Bayar&quot; akan kosong di keempat layar konsumen.</p>
	 *
	 * @return {@code true} bila cara pembayaran ini boleh dipilih pada dokumen gaji baru
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel bendera aktif.
	 *
	 * <p>Dipanggil dari checkbox &quot;Aktif&quot; di grid master, yang menyimpan langsung lewat
	 * {@code Common.refreshSaveOrUpdate(...)} <b>tanpa pemeriksaan hak apa pun</b> (lihat catatan
	 * pada {@link #setDefaultPembayaran(Boolean)}). Bendera ini juga menjadi dasar kemampuan
	 * &quot;hapus lunak&quot; Generic CRUD v2, yang mengaktifkan dirinya sendiri semata karena
	 * properti bernama {@code aktif} terpetakan sebagai boolean.</p>
	 *
	 * @param aktif {@code true} bila cara pembayaran boleh dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan satuan kerja (unit organisasi/tenant) pemilik baris katalog ini.
	 *
	 * <p><b>Getter ini tidak destruktif.</b> Pemanggilan {@code check(satuanKerja)} hanyalah
	 * resolusi proxy lazi standar {@link GeneralValueObject}: hasilnya ditugaskan kembali ke field
	 * yang sama dan nilainya tidak berubah. Jangan tertukar dengan
	 * {@code PembayaranGaji.getSatuanKerja()}, yang <b>menimpa</b> satuan kerja dokumen gaji dengan
	 * nilai yang dikembalikan method ini.</p>
	 *
	 * <p><b>Dampak hilir yang perlu disadari.</b> Karena penimpaan di sisi
	 * {@link ais.database.model.payroll.PembayaranGaji} itu, nilai properti ini secara efektif
	 * <b>menentukan tenant dari setiap dokumen gaji yang memakai baris katalog ini</b>. Mengubahnya
	 * memindahkan seluruh dokumen tersebut ke satuan kerja lain, diam-diam, pada pembacaan
	 * berikutnya. Layar master tidak membatasi pilihan ke subpohon satuan kerja milik pengguna.</p>
	 *
	 * <p><b>Perlakuan cakupan.</b> Ini satu-satunya sumbu tenant kelas ini. Jalur ZK memperlakukan
	 * {@code null} sebagai &quot;katalog global&quot; (selalu terlihat), sedangkan Generic CRUD v2
	 * memasang <code>Restrictions.eq(&quot;satuanKerja&quot;, ...)</code> sehingga baris global
	 * justru tersembunyi di New UI. Pada <b>kedua</b> jalur, pengguna yang satuan kerjanya tidak
	 * dapat ditentukan berakhir tanpa restriksi sama sekali (fail-open) &mdash; lihat Javadoc
	 * kelas.</p>
	 *
	 * @return satuan kerja pemilik baris, atau {@code null} untuk katalog global
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik baris katalog ini.
	 *
	 * <p>Dipanggil {@code CaraPembayaranGajiAction.onSave()} dari banbox pemilih satuan kerja
	 * (mode multi-level, tanpa pembatasan ke subpohon pengguna). Perhatikan dampak hilirnya pada
	 * tenant dokumen gaji yang dijelaskan di {@link #getSatuanKerja()}.</p>
	 *
	 * @param satuanKerja unit organisasi pemilik baris, atau {@code null} untuk katalog global
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
