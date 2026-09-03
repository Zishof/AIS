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
 * Katalog master <b>Jenis Uang Muka</b> &mdash; pemetaan dari sebuah kategori panjar
 * (mis. &quot;Uang Muka Perjalanan Dinas&quot;, &quot;Uang Muka Kegiatan&quot;) ke
 * <b>akun-akun buku besar</b> yang akan dipakai mesin posting saat dokumen panjar dijurnal.
 * Tabel: <code>public.jenis_uang_muka</code>.
 *
 * <h3>Peran dalam mesin akuntansi</h3>
 *
 * <p>Entity ini adalah <b>satu-satunya sumber akun</b> bagi seluruh siklus panjar. Tidak ada
 * satu pun layar posting yang menentukan akun sendiri; semuanya menelusuri
 * <code>dokumen.getUangMuka().getJenisUangMuka().getAkunXxx()</code>. Tiga kolom akun yang
 * disediakan dipakai sebagai berikut (TERVERIFIKASI dari kode pemanggil, bukan dari nama kolom):</p>
 *
 * <ul>
 *   <li>{@link #getAkun()} &mdash; label layar <i>&quot;Akun Penerima Uang Muka&quot;</i>, wajib.
 *       Dipakai sebagai <b>akun DEBET</b> jurnal pencairan panjar di
 *       {@code PostingUangMukaAction} dan {@code PostingDanaTalanganAction} (akun piutang
 *       kepada penerima panjar), lalu sebagai <b>akun KREDIT</b> saat panjar
 *       ditutup/dipertanggungjawabkan di {@code PostingPertangungjawabanAction} dan
 *       {@code PostingPertangungjawabanPengembalianAction}, serta oleh
 *       {@code PostingPertangungjawabanPajakAction} untuk potongan pajak atas LPJ.</li>
 *   <li>{@link #getAkunKelebihan()} &mdash; label layar <i>&quot;Akun Pengembalian Uang Muka&quot;</i>,
 *       wajib. Akun lawan saat sisa dana dikembalikan / kelebihan panjar disetorkan kembali
 *       (kredit di jurnal talangan, debet di jurnal pengembalian).</li>
 *   <li>{@link #getAkunSponsor()} &mdash; label layar <i>&quot;Akun Sponsor Uang Muka&quot;</i>,
 *       opsional. Hanya dipakai {@code PostingPertangungjawabanAction} untuk kaki jurnal
 *       kegiatan bersponsor; bila kosong, kaki tersebut tidak terbentuk.</li>
 * </ul>
 *
 * <p>Konsekuensinya: <b>mengubah satu baris katalog ini mengubah akun buku besar</b> setiap
 * dokumen panjar yang belum diposting yang menunjuk baris tersebut &mdash; mesin posting membaca
 * akun secara <i>live</i> saat tombol Posting ditekan, tidak ada potret akun yang disimpan di
 * dokumen. Pola retroaktif yang sama sudah dicatat untuk tarif pajak pada
 * {@link ais.database.model.akunting.Pajak}.</p>
 *
 * <h3>Hubungan ke {@link ais.database.model.akunting.UangMuka}</h3>
 *
 * <p>{@link ais.database.model.akunting.UangMuka} menunjuk katalog ini lewat kolom FK
 * <code>uang_muka.jenis_uang_muka</code>. Getternya berefek samping bertingkat: bila dokumen
 * ditalangi {@link ais.database.model.akunting.DanaTalangan} yang sudah disetujui, jenis di
 * dokumen <b>ditimpa</b> dari dana talangan; bila tetap kosong dan dokumen punya satuan kerja,
 * dipakai jenis bawaan unit lewat {@link #ambilDefault(SatuanKerja)}. Layar persetujuan panjar
 * menolak dokumen tanpa jenis (&quot;Akun Penerima belum dipilih&quot;) karena tanpa baris
 * katalog ini jurnal tidak dapat dibentuk.</p>
 *
 * <p><b>Klarifikasi penting soal &quot;saldo&quot; dan &quot;pagu&quot;.</b> Entity ini
 * <b>TIDAK memiliki kolom pagu, plafon, kuota, maupun saldo</b> &mdash; sudah diverifikasi
 * lapangan demi lapangan. Kolom <code>saldo</code> berada di
 * {@link ais.database.model.akunting.UangMuka}, bukan di sini, dan diisi oleh
 * {@code JenisUangMukaAction.hitungSaldo(...)} yang menghitung
 * <code>Workspace.getHargaTotal() - &Sigma;PenggunaanAnggaran</code>, yaitu <b>pagu mata anggaran
 * (RAB)</b>, sama sekali bukan pagu per jenis uang muka. Nama kelas
 * {@code JenisUangMukaAction} yang menampung perhitungan itu adalah kebetulan historis
 * (layar panjar yang pertama membutuhkannya), bukan tanda bahwa katalog ini ikut membatasi
 * nominal. <b>Katalog ini tidak menegakkan batas nominal apa pun</b>: tidak ada plafon per jenis,
 * tidak ada akumulasi terpakai, tidak ada penolakan bila panjar melampaui suatu ambang. Satu-satunya
 * pembatas anggaran di jalur panjar adalah saldo {@code Workspace}, dan itu pun hanya ditegakkan
 * bila konfigurasi <code>saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran</code> menyala.</p>
 *
 * <h3>Struktur data</h3>
 *
 * <p>Selain tiga kolom akun di atas: {@link #getKode()} (kode bebas, dipakai untuk pengurutan
 * dan kunci layar revisi), {@link #getNama()} (wajib, <code>nullable = false</code>),
 * {@link #getKeterangan()}, {@link #getSatuanKerja()} (unit pemilik &mdash; lihat catatan
 * cakupan di bawah), {@link #getAktif()}, dan {@link #getDefaultData()} (penanda &quot;jenis
 * bawaan&quot; unit, dibaca {@link #ambilDefault(SatuanKerja)}). Kolom jejak audit
 * {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} diisi oleh
 * {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}.</p>
 *
 * <p>Kelas ini <code>extends</code> {@link ais.database.model.GeneralValueObject}. Base class itu
 * <b>bukan</b> <code>@Entity</code> maupun <code>@MappedSuperclass</code> &mdash; hanya POJO
 * abstrak biasa &mdash; sehingga Hibernate tidak memetakan properti apa pun miliknya. Karena itu
 * <code>id</code>, <code>oleh</code>, <code>olehId</code>, dan <code>tanggal_dirubah</code>
 * <b>wajib dideklarasikan ulang di sini</b>; pengulangan itu keharusan teknis, bukan duplikasi
 * yang perlu dirapikan. Yang diwarisi dan dipakai adalah util statis
 * {@link ais.database.model.GeneralValueObject#check(Object)} (de-proxy lazy) yang dipanggil
 * seluruh getter relasi.</p>
 *
 * <p>Pemetaan memakai <b>property access</b> (anotasi berada di getter), dengan
 * <code>dynamicInsert</code>/<code>dynamicUpdate</code> aktif dan <code>@Audited</code>
 * (Envers) sehingga setiap versi baris digandakan ke tabel revisi <code>jenis_uang_muka_aud</code>.
 * Kombinasi property access + Envers berarti nilai yang <i>dikembalikan getter</i>-lah yang
 * tersimpan dan terarsip, bukan nilai mentah field &mdash; relevan untuk
 * {@link #getKode()}/{@link #getNama()} yang melakukan <code>trim()</code> dan untuk getter relasi
 * yang menulis balik hasil de-proxy ke fieldnya.</p>
 *
 * <h3>Pengelompokan method</h3>
 *
 * <ol>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Cache/bawaan statis</b> &mdash; {@link #DEFAULT_JENIS_KAS_KECIL},
 *       {@link #reloadDefault()}, {@link #ambilDefault(SatuanKerja)}.</li>
 *   <li><b>Identitas &amp; deskripsi</b> &mdash; {@link #getId()}, {@link #getKode()},
 *       {@link #getNama()}, {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Akun buku besar</b> &mdash; {@link #getAkun()}, {@link #getAkunKelebihan()},
 *       {@link #getAkunSponsor()} beserta setter-nya.</li>
 *   <li><b>Cakupan &amp; status</b> &mdash; {@link #getSatuanKerja()}, {@link #getAktif()},
 *       {@link #getDefaultData()} beserta setter-nya.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Cakupan satuan kerja bersifat fail-open.</b> Setiap konsumen memfilter dengan pola
 *       <code>Restrictions.or(Restrictions.isNull(&quot;satuanKerja&quot;), &hellip;)</code>
 *       &mdash; baik {@code JenisUangMukaAction.initCriteria} maupun combo jenis di
 *       {@code UangMukaAction}. Artinya baris yang <code>satuanKerja</code>-nya <code>null</code>
 *       <b>selalu terlihat dan selalu dapat dipilih oleh semua unit</b>, termasuk saat pengguna
 *       sudah dipagari ke unitnya sendiri. Jalur REST lebih longgar lagi: {@code UangMukaApiHelper.opsi}
 *       dan {@code MasterKeuanganApiHelper.daftar} membaca tabel ini <b>tanpa klausa satuan kerja
 *       sama sekali</b>.</li>
 *   <li><b>Baris baru dari layar ZK lahir dengan <code>aktif = null</code>.</b>
 *       {@code JenisUangMukaAction.onSave} menyetel satuan kerja, tiga akun, kode, nama, dan
 *       keterangan &mdash; tetapi <b>tidak pernah</b> memanggil {@link #setAktif(Boolean)} maupun
 *       {@link #setDefaultData(Boolean)}. Tiga konsumen menafsirkan <code>null</code> secara
 *       BERBEDA: {@link #getAktif()} dan layar master membacanya sebagai <i>aktif</i>
 *       (<code>isNull OR eq(true)</code>), REST memakai <code>COALESCE(aktif,true)</code> (juga
 *       aktif), tetapi combo jenis di formulir uang muka memakai
 *       <code>Restrictions.eq(&quot;aktif&quot;, true)</code> <b>ketat</b> sehingga baris
 *       <code>aktif = null</code> <b>tidak pernah muncul di dropdown</b>. Efek yang terlihat
 *       pengguna: jenis baru tampak &quot;Aktif&nbsp;&#10003;&quot; di layar master tetapi tidak bisa
 *       dipilih di dokumen panjar, sampai seseorang menoggle checkbox Aktif di grid (yang menulis
 *       nilai boolean sungguhan). Apakah gejala ini muncul di suatu instalasi bergantung pada ada
 *       tidaknya <code>DEFAULT true</code> pada kolom di DDL &mdash; <code>dynamicInsert</code>
 *       membuat kolom null dihilangkan dari INSERT sehingga DEFAULT database sempat berlaku.</li>
 *   <li><b>Tidak ada jaminan keunikan &quot;bawaan&quot;.</b> Tidak ada constraint maupun kode
 *       yang mencegah dua baris ber-<code>defaultData = true</code> pada satuan kerja yang sama;
 *       {@link #ambilDefault(SatuanKerja)} mengambil yang pertama ditemui saat iterasi
 *       {@code Map} cache, sehingga akun jurnal yang terpilih bisa <b>berbeda antar restart</b>.
 *       Checkbox &quot;Default&quot; di grid master juga menyimpan langsung per baris tanpa
 *       mematikan penanda bawaan pada baris lain.</li>
 *   <li><b>{@link #reloadDefault()} bukan sekadar memuat cache</b> &mdash; ia menulis ke database
 *       (menyemai satu baris bila tabel kosong) dan menutup sesi Hibernate thread saat ini.
 *       Lihat Javadoc method tersebut.</li>
 *   <li><b>{@link #DEFAULT_JENIS_KAS_KECIL} adalah cache mati</b> &mdash; ditulis, tidak pernah
 *       dibaca siapa pun. Namanya pun sisa salin-tempel dari
 *       {@code JenisKasKecil}. Lihat Javadoc fieldnya.</li>
 *   <li><b>Jalur tulis kedua tanpa gerbang menu.</b> Selain layar ZK
 *       {@code /WEB-INF/z/x/y/pages/master/akunting/jenis_uang_muka.zul} (halaman berdiri sendiri
 *       dengan menu sendiri &mdash; pola &quot;pewarisan hak lewat menu induk&quot; TIDAK berlaku
 *       di sini), katalog ini dapat dibuat/diubah/dihapus lewat REST
 *       {@code PosApi} aksi <code>master_keuangan_*</code> yang ditangani
 *       {@code MasterKeuanganApiHelper}. Penjaga di sana, <code>bolehAksi()</code>,
 *       <b>fail-open</b>: bila {@code Tbmuser.hakAkses()} mengembalikan <code>null</code>
 *       (pengguna tanpa peran) fungsi itu langsung <code>return true</code> untuk
 *       create/update/delete, dan aksi <code>master_keuangan_daftar</code> tidak memeriksa hak
 *       baca sama sekali. Lihat catatan keamanan pada {@link #setAkun(Akun)}.</li>
 * </ol>
 *
 * <h3>Siklus hidup di runtime</h3>
 *
 * <p>Saat aplikasi start, {@code InitData} mendaftarkan kelas ini ke pramuat
 * {@code MemoryCacheUtil} (dibaca {@link #ambilDefault(SatuanKerja)} lewat
 * {@link ais.common.ConstantValues#ambilBerdasarClass(Class)}) lalu memanggil
 * {@link #reloadDefault()}. Setiap penyimpanan lewat layar ZK memanggil
 * {@link #reloadDefault()} lagi; jalur REST <b>tidak</b> memanggilnya, sehingga cache in-memory
 * bisa basi setelah perubahan lewat REST sampai restart berikutnya.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.akunting.UangMuka
 * @see ais.database.model.akunting.DanaTalangan
 * @see ais.database.model.akunting.Pertangungjawaban
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.rab.SatuanKerja
 * @see ais.action.master.akunting.JenisUangMukaAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_uang_muka")
public class JenisUangMuka extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Entity ini melintasi batas serialisasi karena disimpan di sesi ZK dan di cache
	 * in-memory {@code MemoryCacheUtil}. Nilai ini <b>tidak boleh diubah</b> selama perubahan
	 * struktur masih kompatibel, agar objek yang sudah terlanjur diserialisasi tetap dapat
	 * dibaca kembali.</p>
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
	 * <p><b>Menolak nilai kosong secara diam-diam.</b> Argumen <code>null</code> atau yang
	 * hanya berisi spasi diabaikan &mdash; nilai lama dipertahankan dan tidak ada exception yang
	 * dilempar. Jejak audit karenanya hanya bisa ditimpa oleh identitas baru yang valid, tidak
	 * bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: argumen <code>null</code> atau berisi spasi
	 * saja diabaikan tanpa error, sehingga jejak audit tidak dapat dihapus lewat setter.</p>
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
	 * <p><b>Tujuan.</b> Mengisi {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan
	 * {@link #setOlehId(String)} dengan waktu server dan identitas pengguna aktif, tanpa
	 * membebani setiap layar untuk melakukannya sendiri.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan sepenuhnya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Interceptor tersebut
	 * lebih dulu bertanya ke {@code AuditTrailHelper.peekUpdateDecision(...)}; bila UPDATE
	 * dinilai tidak membawa perubahan bisnis, penstempelan <b>dilewati</b> sehingga
	 * <code>tanggal_dirubah</code> tidak bergerak karena flush kosong. Identitas diambil dari
	 * sesi web ZK/JSP, atau dari atribut request {@code ATTR_PENGGUNA_POS} bila permintaan datang
	 * lewat {@code PosApi}; bila keduanya tidak ada, terekam sebagai <code>external_update</code>.</p>
	 *
	 * <p><b>Efek samping.</b> Memodifikasi tiga properti entity ini di dalam siklus flush
	 * Hibernate. Tidak menyentuh database secara langsung dan tidak melempar exception.</p>
	 *
	 * <p><b>Kapan dipanggil.</b> Hanya oleh provider JPA/Hibernate, otomatis, pada setiap UPDATE
	 * baris ini. Tidak pernah dipanggil dari kode aplikasi. <b>Tidak</b> berjalan pada INSERT
	 * &mdash; tidak ada <code>@PrePersist</code> pasangannya.</p>
	 *
	 * <p><b>Catatan tentang deklarasi field pada baris yang sama.</b> Field
	 * <code>tanggal_dirubah</code> sengaja dideklarasikan menempel di baris method ini (pola
	 * penyisipan massal di repo ini) dan diinisialisasi ke {@code WaktuUtil.getDate()} saat objek
	 * dibuat, sehingga baris baru sudah punya stempel waktu meski hook ini belum pernah jalan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil otomatis dari {@link #onUpdate()}; pemanggilan manual hanya wajar saat
	 * migrasi/impor data yang ingin mempertahankan waktu asli.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh <code>null</code>
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris katalog ini (presisi TIMESTAMP).
	 *
	 * <p>Diinisialisasi ke waktu pembuatan objek dan diperbarui oleh {@link #onUpdate()}.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: <code>id + &quot;-&quot; + nama</code>.
	 *
	 * <p><b>Kapan terlihat pengguna.</b> Dipakai komponen ZK generik (mis. isi combo/banbox yang
	 * tidak menyebut properti tampilan secara eksplisit) dan pesan diagnostik. Layar master dan
	 * combo jenis di formulir uang muka menampilkan <code>kode</code>/<code>nama</code> secara
	 * eksplisit sehingga tidak melewati method ini.</p>
	 *
	 * <p><b>Kasus tepi.</b> Membaca field <code>id</code> dan <code>nama</code> secara langsung,
	 * bukan lewat getternya, sehingga tidak melakukan <code>trim()</code> dan menghasilkan
	 * <code>&quot;null-null&quot;</code> untuk instance yang benar-benar kosong.</p>
	 *
	 * @return gabungan id dan nama, dipisahkan tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Cache statis satu-baris hasil {@link #reloadDefault()} &mdash; <b>tidak dipakai siapa pun.</b>
	 *
	 * <p><b>Status TERVERIFIKASI: cache mati (write-only).</b> Penelusuran seluruh repo menemukan
	 * referensi ke field ini <b>hanya di dalam {@link #reloadDefault()} itu sendiri</b>. Tidak ada
	 * satu pun Action, Helper, JSP, atau ZUL yang membacanya. Resolusi jenis bawaan yang
	 * sesungguhnya dipakai produksi berjalan lewat {@link #ambilDefault(SatuanKerja)} yang membaca
	 * cache {@code MemoryCacheUtil}, bukan lewat field ini.</p>
	 *
	 * <p><b>Nama yang menyesatkan.</b> <code>DEFAULT_JENIS_KAS_KECIL</code> adalah sisa
	 * salin-tempel dari {@code JenisKasKecil} yang punya field bernama sama; di kelas ini nama
	 * tersebut tidak ada hubungannya dengan kas kecil.</p>
	 *
	 * <p><b>Risiko bila kelak dipakai.</b> Field ini <code>public static</code> dan mutable,
	 * berisi satu instance tunggal untuk seluruh JVM &mdash; artinya lintas seluruh satuan kerja
	 * dan lintas thread. {@link #reloadDefault()} mengisinya dengan baris ber-<code>id</code>
	 * TERKECIL di seluruh tabel tanpa memandang satuan kerja maupun penanda
	 * {@link #getDefaultData()}. Jangan menjadikannya sumber akun jurnal.</p>
	 */
	public static JenisUangMuka DEFAULT_JENIS_KAS_KECIL = null;

	/**
	 * Menyegarkan {@link #DEFAULT_JENIS_KAS_KECIL} dan &mdash; bila tabel katalog masih kosong
	 * &mdash; <b>menyemai satu baris ke database</b>.
	 *
	 * <p><b>Tujuan yang tersirat dari nama</b> adalah memuat ulang cache bawaan. Yang benar-benar
	 * dikerjakan lebih luas dari itu dan perlu dipahami sebelum memanggilnya:</p>
	 *
	 * <ol>
	 *   <li>Mengambil sesi lewat {@code HibernateUtil.currentNativeSession()} lalu mengambil
	 *       <b>satu</b> baris {@code JenisUangMuka} dengan <code>id</code> terkecil
	 *       (<code>addOrder(asc(&quot;id&quot;))</code>, <code>setMaxResults(1)</code>) &mdash;
	 *       tanpa filter satuan kerja, tanpa filter {@link #getAktif()}, tanpa memandang
	 *       {@link #getDefaultData()}.</li>
	 *   <li>Bila hasilnya <code>null</code> (tabel kosong), <b>membuat dan menyimpan baris baru</b>
	 *       dengan <code>kode = &quot;001&quot;</code>, <code>aktif = true</code>,
	 *       <code>nama = keterangan = &quot;Uang Muka&quot;</code>, di dalam transaksi
	 *       <code>begin()</code>/<code>commit()</code> eksplisit pada sesi tersebut.</li>
	 *   <li>Menutup sesi: <code>disconnect()</code> + <code>close()</code> bila masih terbuka,
	 *       lalu {@code HibernateUtil.closeSession()} untuk melepas sesi ThreadLocal.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang perlu diwaspadai.</b></p>
	 * <ul>
	 *   <li><b>Menulis ke database dari sebuah method bernama &quot;reload&quot;.</b> Baris semaian
	 *       lahir <b>tanpa {@link #getAkun()}, tanpa {@link #getAkunKelebihan()}, tanpa
	 *       {@link #getSatuanKerja()}, dan tanpa {@link #getDefaultData()}</b>. Baris seperti itu
	 *       tidak akan pernah terpilih oleh {@link #ambilDefault(SatuanKerja)} (yang mensyaratkan
	 *       <code>defaultData</code>), tetapi <b>bisa dipilih manual</b> di combo formulir uang
	 *       muka karena <code>satuanKerja</code>-nya null (fail-open cakupan). Dokumen yang memakai
	 *       baris ini akan <b>gagal terjurnal</b> saat diposting karena akun debetnya null.</li>
	 *   <li><b>Menutup sesi Hibernate milik pemanggil.</b> {@code currentNativeSession()} sendiri
	 *       memuat pengingat eksplisit &quot;JANGAN dipakai di konteks request ZK&quot;, namun
	 *       {@code JenisUangMukaAction.onSave} memanggil method ini persis di konteks itu, tepat
	 *       setelah <code>session.flush()</code>. Akibatnya entity yang baru disimpan menjadi
	 *       <i>detached</i> dan operasi berikutnya (mis. muat ulang grid) berjalan di atas sesi
	 *       baru. Berfungsi, tetapi rapuh &mdash; jangan menambah pekerjaan apa pun di antara
	 *       penyimpanan dan pemanggilan method ini.</li>
	 *   <li><b>Transaksi eksplisit pada sesi bersama.</b> Karena <code>begin()</code>/
	 *       <code>commit()</code> dijalankan pada sesi ThreadLocal yang sama dengan pekerjaan lain,
	 *       perubahan lain yang masih menggantung di sesi tersebut ikut ter-commit bersama baris
	 *       semaian.</li>
	 * </ul>
	 *
	 * <p><b>Kapan/dari mana dipanggil (TERVERIFIKASI).</b> Dua tempat saja: {@code InitData}
	 * pada rangkaian <code>reloadDefaults</code> saat aplikasi start, dan
	 * {@code JenisUangMukaAction.onSave} setelah setiap simpan lewat layar master ZK. Jalur REST
	 * {@code MasterKeuanganApiHelper.simpan} <b>tidak</b> memanggilnya.</p>
	 *
	 * <p><b>Penanganan error.</b> Tidak ada. Exception dari query maupun dari
	 * <code>commit()</code> merambat ke pemanggil, dan bila itu terjadi sesi tidak sempat ditutup
	 * karena tidak ada blok <code>finally</code>.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		DEFAULT_JENIS_KAS_KECIL = (JenisUangMuka) session.createCriteria(JenisUangMuka.class).setMaxResults(1)
				.addOrder(Order.asc("id")).uniqueResult();
		if (DEFAULT_JENIS_KAS_KECIL == null) {
			DEFAULT_JENIS_KAS_KECIL = new JenisUangMuka();
			DEFAULT_JENIS_KAS_KECIL.setKode("001");
			DEFAULT_JENIS_KAS_KECIL.setAktif(true);
			DEFAULT_JENIS_KAS_KECIL.setNama("Uang Muka");
			DEFAULT_JENIS_KAS_KECIL.setKeterangan("Uang Muka");
			session.getTransaction().begin();
			session.save(DEFAULT_JENIS_KAS_KECIL);
			session.getTransaction().commit();
		}
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	/**
	 * Mencari <b>jenis uang muka bawaan</b> untuk suatu satuan kerja dari cache in-memory.
	 *
	 * <p><b>Tujuan.</b> Menyediakan akun jurnal fallback bagi dokumen panjar yang jenisnya belum
	 * dipilih pengguna, sehingga dokumen tetap dapat diposting tanpa intervensi manual.</p>
	 *
	 * <p><b>Cara kerja.</b> Membaca seluruh isi katalog dari cache
	 * {@link ais.common.ConstantValues#ambilBerdasarClass(Class)} (di belakangnya
	 * {@code MemoryCacheUtil}, dipramuat oleh {@code InitData}) lalu menelusuri nilainya:</p>
	 * <ul>
	 *   <li>bila <code>satuanKerja</code> argumen <code>null</code> <b>atau</b> id-nya
	 *       <code>null</code> &rarr; dicari baris yang {@link #getSatuanKerja()}-nya
	 *       <code>null</code> <b>dan</b> {@link #getDefaultData()}-nya <code>true</code>;</li>
	 *   <li>selain itu &rarr; dicari baris yang {@link #getSatuanKerja()}-nya tidak null,
	 *       id-nya sama persis dengan argumen, <b>dan</b> {@link #getDefaultData()}-nya
	 *       <code>true</code>.</li>
	 * </ul>
	 * <p>Baris pertama yang cocok langsung dikembalikan.</p>
	 *
	 * <p><b>Hal non-obvious.</b></p>
	 * <ul>
	 *   <li><b>Tidak mewarisi ke induk hierarki.</b> Pencocokan satuan kerja memakai kesamaan id
	 *       persis, bukan penelusuran pohon. Unit anak yang tidak punya bawaan sendiri
	 *       <b>tidak</b> mewarisi bawaan unit induknya dan akan menerima <code>null</code>.</li>
	 *   <li><b>Tidak jatuh balik ke bawaan global.</b> Bila argumen berisi satuan kerja, baris
	 *       bawaan yang <code>satuanKerja</code>-nya null tidak pernah dipertimbangkan.</li>
	 *   <li><b>Urutan non-deterministik.</b> Iterasi atas {@code Map} cache tidak berurutan; bila
	 *       ada lebih dari satu baris bertanda bawaan pada unit yang sama (tidak ada yang
	 *       mencegahnya, lihat Javadoc kelas), akun jurnal yang terpilih dapat berubah antar
	 *       restart aplikasi.</li>
	 *   <li><b>Gagal senyap saat cache kosong.</b> {@code ambilBerdasarClass} mengembalikan
	 *       {@code EMPTY_MAP} bila cache belum/gagal terisi &mdash; hasilnya <code>null</code>,
	 *       tanpa fallback query ke database dan tanpa log.</li>
	 * </ul>
	 *
	 * <p><b>Kapan/dari mana dipanggil (TERVERIFIKASI).</b> Dua pemanggil, keduanya di dalam getter
	 * entity dokumen: {@code UangMuka.getJenisUangMuka()} dan
	 * {@code DanaTalangan.getJenisUangMuka()}. Karena keduanya getter, pemanggilan terjadi saat
	 * baris dirender maupun saat mesin posting membaca dokumen.</p>
	 *
	 * <p><b>Efek samping.</b> Tidak ada tulisan ke database. Instance yang dikembalikan berasal
	 * dari cache bersama &mdash; jangan dimutasi oleh pemanggil.</p>
	 *
	 * @param satuanKerja unit yang bawaannya dicari; <code>null</code> (atau ber-id null) berarti
	 *                    mencari bawaan global (baris tanpa satuan kerja)
	 * @return jenis uang muka bawaan yang cocok, atau <code>null</code> bila unit tersebut belum
	 *         punya bawaan atau cache belum terisi
	 */
	@SuppressWarnings("unchecked")
	public static JenisUangMuka ambilDefault(SatuanKerja satuanKerja) {

		Map<Long, JenisUangMuka> mapJenisUangMuka = ConstantValues.ambilBerdasarClass(JenisUangMuka.class);

		if (satuanKerja == null || satuanKerja.getId() == null) {
			for (JenisUangMuka jenisUangMuka : mapJenisUangMuka.values()) {
				if (jenisUangMuka.getSatuanKerja() == null && jenisUangMuka.getDefaultData()) {
					return jenisUangMuka;
				}
			}
		} else {
			for (JenisUangMuka jenisUangMuka : mapJenisUangMuka.values()) {
				if (jenisUangMuka.getSatuanKerja() != null
						&& jenisUangMuka.getSatuanKerja().getId().equals(satuanKerja.getId())
						&& jenisUangMuka.getDefaultData()) {
					return jenisUangMuka;
				}
			}
		}

		return null;
	}

	/** Kode katalog bebas isi. Lihat {@link #getKode()}. */
	private String kode;

	/** Akun penerima uang muka &mdash; sumber akun debet jurnal pencairan. Lihat {@link #getAkun()}. */
	private Akun akun;

	/** Akun pengembalian/kelebihan uang muka. Lihat {@link #getAkunKelebihan()}. */
	private Akun akunKelebihan;

	/** Akun sponsor (opsional), dipakai jurnal LPJ kegiatan bersponsor. Lihat {@link #getAkunSponsor()}. */
	private Akun akunSponsor;

	/** Nama jenis uang muka; wajib. Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Unit pemilik katalog; <code>null</code> berarti berlaku untuk semua unit. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

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
	 * (dari nilai default getter) sementara kolom di database tetap <code>null</code> &mdash;
	 * lihat catatan nomor 2 pada Javadoc kelas.</p>
	 */
	public JenisUangMuka() {
	}

	/**
	 * Kunci utama baris katalog (kolom <code>id</code>, IDENTITY, tidak ikut dalam INSERT).
	 *
	 * @return id baris, atau <code>null</code> bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya dipanggil Hibernate; kode aplikasi tidak boleh menyetelnya
	 * sendiri.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode katalog, <b>sudah di-<code>trim()</code> dan tidak pernah <code>null</code></b>.
	 *
	 * <p>Bebas isi (tidak ada validasi format maupun keunikan). Dipakai layar master untuk
	 * pengurutan hasil pencarian (<code>Order.asc(&quot;kode&quot;)</code>), sebagai kunci layar
	 * revisi Envers ({@code RevisiHelper.createNewRevisi}), dan sebagai bagian teks item combo
	 * jenis di formulir uang muka.</p>
	 *
	 * <p><b>Non-obvious:</b> karena pemetaan memakai property access, nilai <b>hasil trim</b>
	 * inilah yang ditulis Hibernate ke database dan diarsipkan Envers &mdash; kode
	 * <code>null</code> akan tersimpan sebagai string kosong pada UPDATE berikutnya.</p>
	 *
	 * @return kode katalog yang sudah dirapikan, atau string kosong bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode katalog apa adanya (tanpa trim, tanpa validasi keunikan).
	 *
	 * @param kode kode katalog; boleh <code>null</code>
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama jenis uang muka yang tampil di layar dan di combo pemilihan.
	 *
	 * <p>Kolom wajib di tingkat database (<code>nullable = false</code>) dan divalidasi tidak
	 * kosong baik oleh {@code JenisUangMukaAction.onSave} maupun oleh
	 * {@code MasterKeuanganApiHelper.simpan}. Hasilnya di-<code>trim()</code>, dan karena property
	 * access nilai hasil trim itulah yang tersimpan.</p>
	 *
	 * @return nama jenis uang muka yang sudah dirapikan, atau <code>null</code> bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis uang muka.
	 *
	 * @param nama nama jenis; wajib tidak kosong agar dapat disimpan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk baris katalog.
	 *
	 * <p>Ditampilkan sebagai kolom pada grid master dan sebagai teks deskripsi item combo jenis di
	 * formulir uang muka. Tidak dipakai logika bisnis mana pun.</p>
	 *
	 * @return keterangan apa adanya (tanpa trim), atau <code>null</code>
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan teks keterangan; boleh <code>null</code>
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Penanda baris katalog masih dipakai, dengan <code>null</code> diperlakukan sebagai
	 * <b>aktif</b>.
	 *
	 * <p><b>Peringatan: tiga konsumen menafsirkan kolom ini berbeda.</b> Getter ini dan filter
	 * layar master ({@code isNull OR eq(true)}) serta REST ({@code COALESCE(aktif,true)})
	 * menganggap <code>null</code> aktif, tetapi combo jenis di formulir uang muka memakai
	 * {@code Restrictions.eq("aktif", true)} yang ketat sehingga baris <code>aktif = null</code>
	 * tidak pernah muncul di dropdown. Karena {@code JenisUangMukaAction.onSave} tidak pernah
	 * memanggil {@link #setAktif(Boolean)}, baris yang dibuat lewat layar master berpotensi lahir
	 * dengan <code>null</code> (tergantung ada tidaknya DEFAULT kolom di DDL, sebab
	 * <code>dynamicInsert</code> menghilangkan kolom null dari INSERT). Lihat catatan nomor 2 pada
	 * Javadoc kelas.</p>
	 *
	 * @return <code>true</code> bila baris aktif atau statusnya belum pernah diset;
	 *         <code>false</code> hanya bila dinonaktifkan secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris katalog.
	 *
	 * <p>Dipanggil dari checkbox &quot;Aktif&quot; di grid master (langsung menyimpan tanpa tombol
	 * simpan terpisah), dari {@code MasterKeuanganApiHelper.simpan}, dan dari
	 * {@link #reloadDefault()} saat menyemai baris pertama. <b>Tidak</b> dipanggil oleh
	 * {@code JenisUangMukaAction.onSave}.</p>
	 *
	 * <p>Menonaktifkan baris tidak berpengaruh apa pun pada dokumen panjar yang sudah menunjuknya:
	 * mesin posting tetap membaca akun dari baris nonaktif.</p>
	 *
	 * @param aktif status aktif; <code>null</code> berarti dianggap aktif oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * <b>Akun penerima uang muka</b> &mdash; sumber akun buku besar utama bagi seluruh siklus
	 * panjar (kolom FK <code>akun</code>).
	 *
	 * <p><b>Peran di mesin jurnal (TERVERIFIKASI dari kode pemanggil).</b></p>
	 * <ul>
	 *   <li><b>Akun DEBET</b> jurnal pencairan panjar di {@code PostingUangMukaAction} dan
	 *       {@code PostingDanaTalanganAction} &mdash; mencatat piutang kepada penerima panjar.</li>
	 *   <li><b>Akun KREDIT</b> saat panjar ditutup di {@code PostingPertangungjawabanAction},
	 *       {@code PostingPertangungjawabanPengembalianAction}, dan
	 *       {@code PostingPertangungjawabanPajakAction} &mdash; menghapus piutang tersebut.</li>
	 * </ul>
	 * <p>Karena akun dibaca <i>live</i> saat posting dan tidak dipotret ke dokumen, mengganti akun
	 * di sini mengubah akun jurnal seluruh dokumen yang belum diposting. Bila kosong, dokumen
	 * gagal terjurnal &mdash; {@code UangMukaApiHelper} bahkan menolak persetujuan dengan pesan
	 * &quot;Akun Penerima (Jenis Uang Muka) belum dipilih&quot;.</p>
	 *
	 * <p><b>Getter menulis balik ke fieldnya.</b> Hasil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} di-assign kembali ke
	 * <code>akun</code>. Ini de-proxy lazy Hibernate: proxy diganti instance kanonik dari
	 * {@code EntityIdentityMap}/cache atau dimuat ulang dari database. <b>Bersifat jinak</b>
	 * &mdash; berbeda dari getter destruktif {@code Transaksi.getAkun()} yang menimpa akun dengan
	 * nilai <i>berbeda</i> ({@code akunOver}); di sini identitas FK tidak berubah sehingga
	 * pembacaan tidak menggeser atribusi akun. Kegagalan resolusi bersifat senyap: bila
	 * <code>check</code> tidak bisa meresolusi, proxy dikembalikan apa adanya.</p>
	 *
	 * @return akun penerima uang muka, atau <code>null</code> bila belum dikonfigurasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel akun penerima uang muka.
	 *
	 * <p><b>Titik ubah paling sensitif di kelas ini.</b> Setter ini memindahkan akun buku besar
	 * yang akan dipakai seluruh dokumen panjar yang menunjuk baris ini dan belum diposting.
	 * Tidak ada validasi tipe akun (debet/kredit), tidak ada pemeriksaan apakah akun berada di
	 * satuan kerja yang sama, dan tidak ada jejak &quot;akun lama&quot; selain arsip Envers.</p>
	 *
	 * <p><b>Dua jalur tulis, gerbang berbeda.</b> (1) {@code JenisUangMukaAction.onSave} &mdash;
	 * layar ZK berdiri sendiri dengan menu dan hak CREATE/UPDATE/DELETE-nya sendiri; validasi
	 * mewajibkan akun terisi. (2) {@code MasterKeuanganApiHelper.simpan} lewat REST
	 * {@code PosApi} aksi <code>master_keuangan_simpan</code> &mdash; penjaganya
	 * <code>bolehAksi()</code> <b>fail-open</b>: bila {@code Tbmuser.hakAkses()} bernilai
	 * <code>null</code> fungsi itu <code>return true</code> untuk create/update/delete, jalur ini
	 * <b>mengizinkan akun kosong</b> (&quot;supaya admin dapat melengkapi bertahap&quot;),
	 * menerima <code>satuanKerjaId</code> apa pun tanpa cek cakupan, dan tidak memanggil
	 * {@link #reloadDefault()} sehingga cache in-memory menjadi basi.</p>
	 *
	 * @param akun akun buku besar penerima uang muka; boleh <code>null</code>, tetapi dokumen yang
	 *             memakainya tidak akan dapat dijurnal
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Unit pemilik baris katalog (kolom FK <code>satuan_kerja</code>).
	 *
	 * <p><b>Cakupan bersifat fail-open.</b> Nilai <code>null</code> berarti baris berlaku untuk
	 * <b>semua</b> unit: baik {@code JenisUangMukaAction.initCriteria} maupun combo jenis di
	 * {@code UangMukaAction} membungkus filternya dengan
	 * <code>Restrictions.or(Restrictions.isNull(&quot;satuanKerja&quot;), &hellip;)</code>,
	 * sehingga baris tanpa unit selalu ikut terlihat dan dapat dipilih meski pengguna sudah
	 * dipagari ke unitnya. Jalur REST ({@code UangMukaApiHelper.opsi},
	 * {@code MasterKeuanganApiHelper.daftar}) membaca tabel ini tanpa klausa satuan kerja sama
	 * sekali. Baris semaian dari {@link #reloadDefault()} termasuk kategori ini karena tidak
	 * menyetel satuan kerja.</p>
	 *
	 * <p>Pemilihan di layar master memakai hierarki: unit yang dipilih beserta seluruh turunannya
	 * ({@code SatuanKerjaTreeModel.getChildsSet}). Namun {@link #ambilDefault(SatuanKerja)} tidak
	 * ikut menelusuri hierarki &mdash; lihat Javadoc method tersebut.</p>
	 *
	 * <p>Getter melakukan de-proxy lewat {@link ais.database.model.GeneralValueObject#check(Object)}
	 * dan menulis hasilnya kembali ke field; sifatnya jinak, sama seperti {@link #getAkun()}.</p>
	 *
	 * @return unit pemilik, atau <code>null</code> bila katalog berlaku lintas unit
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel unit pemilik baris katalog.
	 *
	 * <p>Layar master mewajibkan unit dipilih (&quot;Satuan Kerja *&quot;) dan mengisinya otomatis
	 * dari {@code Common.getSatuanKerja()} bila kosong. Jalur REST tidak mewajibkannya dan tidak
	 * memeriksa apakah unit yang dikirim memang milik pemanggil &mdash; menyetel
	 * <code>null</code> lewat jalur itu menjadikan katalog terlihat lintas unit.</p>
	 *
	 * @param satuanKerja unit pemilik; <code>null</code> berarti berlaku untuk semua unit
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * <b>Akun pengembalian/kelebihan uang muka</b> (kolom FK <code>akun_kelebihan</code>), label
	 * layar &quot;Akun Pengembalian Uang Muka&quot;.
	 *
	 * <p><b>Peran di mesin jurnal (TERVERIFIKASI).</b> Merupakan lawan dari {@link #getAkun()}
	 * pada sisi pengembalian dana:</p>
	 * <ul>
	 *   <li>{@code PostingDanaTalanganAction} memakainya sebagai <b>akun KREDIT</b> jurnal dana
	 *       talangan (pasangan debet {@link #getAkun()});</li>
	 *   <li>{@code PostingUangMukaAction} memakainya sebagai akun lawan ketika panjar ditalangi
	 *       dana talangan;</li>
	 *   <li>{@code PostingPertangungjawabanPengembalianAction} memakainya sebagai <b>akun
	 *       DEBET</b> saat sisa panjar disetorkan kembali;</li>
	 *   <li>{@code PostingPertangungjawabanAction} memakainya pada kaki jurnal LPJ.</li>
	 * </ul>
	 *
	 * <p>Wajib diisi di layar master maupun ditandai <code>wajibUntukJurnal</code> di REST; bila
	 * kosong, jurnal pengembalian tidak dapat dibentuk. Getter melakukan de-proxy jinak dan
	 * menulis hasilnya kembali ke field, sama seperti {@link #getAkun()}.</p>
	 *
	 * @return akun pengembalian uang muka, atau <code>null</code> bila belum dikonfigurasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_kelebihan", nullable = true)
	public Akun getAkunKelebihan() {
		akunKelebihan = check(akunKelebihan);
		return akunKelebihan;
	}

	/**
	 * Menyetel akun pengembalian/kelebihan uang muka.
	 *
	 * <p>Berlaku peringatan yang sama dengan {@link #setAkun(Akun)}: perubahan berdampak
	 * retroaktif pada seluruh dokumen yang belum diposting, tidak ada validasi tipe akun maupun
	 * cakupan unit, dan jalur REST dapat menulisnya lewat penjaga yang fail-open.</p>
	 *
	 * @param akunKelebihan akun buku besar pengembalian; boleh <code>null</code>
	 */
	public void setAkunKelebihan(Akun akunKelebihan) {
		this.akunKelebihan = akunKelebihan;
	}

	/**
	 * <b>Akun sponsor uang muka</b> (kolom FK <code>akun_sponsor</code>), opsional.
	 *
	 * <p><b>Peran di mesin jurnal (TERVERIFIKASI).</b> Satu-satunya konsumen adalah
	 * {@code PostingPertangungjawabanAction}, yang memakainya untuk kaki jurnal LPJ kegiatan
	 * bersponsor. Tidak dipakai jalur pencairan panjar, dana talangan, pengembalian, maupun pajak.
	 * Bila kosong, kaki jurnal sponsor tidak terbentuk &mdash; karena itu satu-satunya kolom akun
	 * yang ditandai tidak wajib (<code>wajibUntukJurnal = false</code>) baik di layar master
	 * maupun di REST.</p>
	 *
	 * <p>Getter melakukan de-proxy jinak dan menulis hasilnya kembali ke field, sama seperti
	 * {@link #getAkun()}.</p>
	 *
	 * @return akun sponsor, atau <code>null</code> bila jenis ini tidak memakai skema sponsor
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_sponsor", nullable = true)
	public Akun getAkunSponsor() {
		akunSponsor = check(akunSponsor);
		return akunSponsor;
	}

	/**
	 * Menyetel akun sponsor uang muka.
	 *
	 * @param akunSponsor akun buku besar sponsor; boleh <code>null</code> (kaki jurnal sponsor
	 *                    tidak akan dibentuk)
	 */
	public void setAkunSponsor(Akun akunSponsor) {
		this.akunSponsor = akunSponsor;
	}

	/**
	 * Penanda bahwa baris ini adalah <b>jenis bawaan</b> bagi satuan kerjanya (kolom
	 * <code>defaultdata</code>), dengan <code>null</code> diperlakukan sebagai
	 * <code>false</code>.
	 *
	 * <p><b>Dampak.</b> Hanya baris bertanda ini yang dapat dipilih otomatis oleh
	 * {@link #ambilDefault(SatuanKerja)}, yang dipanggil dari getter
	 * {@code UangMuka.getJenisUangMuka()} dan {@code DanaTalangan.getJenisUangMuka()}. Artinya
	 * baris ini menentukan akun buku besar dokumen panjar yang jenisnya <b>tidak dipilih siapa
	 * pun</b>.</p>
	 *
	 * <p><b>Non-obvious.</b> Tidak ada constraint keunikan per satuan kerja dan tidak ada kode
	 * yang mematikan penanda ini pada baris lain saat satu baris ditandai; bila ada lebih dari
	 * satu bawaan pada unit yang sama, pemilihan menjadi non-deterministik (iterasi
	 * {@code Map} cache). Penanda ini juga tidak pernah diset oleh
	 * {@code JenisUangMukaAction.onSave} &mdash; hanya lewat checkbox &quot;Default&quot; di grid
	 * master yang menyimpan langsung per baris.</p>
	 *
	 * @return <code>true</code> bila baris ini bawaan unitnya, <code>false</code> bila tidak atau
	 *         belum pernah diset
	 */
	public Boolean getDefaultData() {
		return defaultData == null ? false : defaultData;
	}

	/**
	 * Menyetel penanda jenis bawaan.
	 *
	 * <p>Tidak melakukan apa pun terhadap baris lain: menandai satu baris sebagai bawaan
	 * <b>tidak</b> mencabut penanda bawaan dari baris lain pada satuan kerja yang sama.</p>
	 *
	 * @param defaultData <code>true</code> untuk menjadikan baris ini bawaan unitnya;
	 *                    <code>null</code> dianggap <code>false</code> oleh
	 *                    {@link #getDefaultData()}
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

}
