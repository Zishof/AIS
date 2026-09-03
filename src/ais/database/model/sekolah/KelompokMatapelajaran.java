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

/**
 * Master <b>kelompok mata pelajaran</b> (rumpun mata pelajaran) jenjang SEKOLAH &mdash; tabel
 * {@code sekolah.kelompok_matapelajaran}.
 *
 * <p>Satu baris entity ini mewakili satu <i>rumpun</i> tempat beberapa {@link Matapelajaran}
 * dihimpun untuk keperluan penataan kurikulum sekolah dan pengelompokan blok nilai pada rapor.
 * Contoh isi yang disebut panduan pengguna ({@code WEB-INF/bantuan/kelompok_matapelajaran.html}):
 * rumpun mata pelajaran <i>umum</i>, <i>kejuruan</i>, dan <i>muatan lokal</i>. Struktur data di
 * sini <b>generik</b>: tidak ada satu pun daftar bawaan/auto-seed di repositori, sehingga nama
 * rumpun sepenuhnya diketik admin kurikulum &mdash; skema "Kelompok A/B/C" ala Kurikulum 2013
 * dapat diketik, tetapi <b>bukan</b> sesuatu yang diasumsikan kode.</p>
 *
 * <p><b>Arah relasi:</b> pemiliknya adalah {@link Matapelajaran} lewat
 * {@link Matapelajaran#getKelompokMatapelajaran()} (kolom FK {@code kelompok_matapelajaran_id},
 * {@code nullable = true}). Kelas ini <b>tidak</b> menyimpan koleksi balik ke mata pelajaran; untuk
 * mengambil anggota rumpun, kode selalu meng-query dari sisi {@code Matapelajaran}.</p>
 *
 * <h2>Rumpun bertingkat (self-reference)</h2>
 * <p>Kolom {@code induk} ({@link #getInduk()}) menunjuk ke baris {@code KelompokMatapelajaran} lain,
 * sehingga rumpun dapat disusun <b>berjenjang</b>. Jalur rapor
 * ({@code ais.action.report.format1.sekolah.LaporanRaporSiswa}) memakainya persis begitu:</p>
 * <ol>
 *   <li>mengambil seluruh rumpun <b>akar</b> ({@code induk IS NULL}) milik sekolah terkait,
 *       diurutkan {@code nomorUrut} menaik;</li>
 *   <li>untuk tiap akar memanggil {@code genarateKelompok(...)}; bila tidak ada mata pelajaran yang
 *       langsung menempel pada akar tersebut, method itu mencari <b>anak</b> ({@code induk = akar})
 *       lalu ber-rekursi ke bawah;</li>
 *   <li>nama rumpun yang akhirnya terpakai dikirim ke JasperReports sebagai parameter
 *       {@code "sub_jenis"} &mdash; yaitu <i>judul blok</i> di atas sekelompok baris nilai.</li>
 * </ol>
 * <p>Indeks pendukung pola akses itu dibuat {@code ais.common.InitIndex}:
 * {@code idx_kelompok_matapelajaran_rapor ON sekolah.kelompok_matapelajaran (sekolah_id, induk,
 * aktif, nomorurut)} &mdash; urutan kolomnya sekaligus menjadi bukti bahwa keempat properti itulah
 * yang benar-benar dipakai sebagai penyaring/pengurut di produksi.</p>
 *
 * <h2>Layar &amp; jalur pemakai (terverifikasi)</h2>
 * <ul>
 *   <li><b>Master:</b> {@code ais.action.master.sekolah.KelompokMatapelajaranAction} +
 *       {@code /pages/master/sekolah/kelompok_matapelajaran.zul}. Menu "Kelompok Mata Pelajaran"
 *       (id {@code 83459111}, induk menu {@code 570008}) didaftarkan
 *       {@code ais.common.MenuInitializer} dan {@code ais.common.MenuSnapshotData}, bersebelahan
 *       dengan menu "Mata Pelajaran".</li>
 *   <li><b>Konsumen utama:</b> {@code ais.action.master.sekolah.MatapelajaranAction} &mdash;
 *       combobox berlabel <i>"Kelompok Matapelajaran *"</i> yang <b>wajib</b> diisi saat menyimpan
 *       mata pelajaran (validasi ada di {@code onSave()}).</li>
 *   <li><b>Laporan:</b> {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} (rekursi rumpun
 *       + parameter {@code sub_jenis}) dan
 *       {@code ais.action.report.format1.sekolah.LaporanRekapTotalNilai} (parameter
 *       {@code sub_jenis} per mata pelajaran).</li>
 *   <li><b>Startup/infrastruktur:</b> {@code ais.common.InitData.initClasses(...)},
 *       {@code ais.common.InitIndex} (indeks di atas), {@code ais.common.DataUtil} (kelas ini masuk
 *       daftar {@code CLASS_JANGAN_DIBERSIHKAN} sehingga <b>tidak</b> ikut terhapus pembersihan
 *       data), dan pendaftaran {@code <mapping class="..."/>} di {@code hibernate.cfg.xml}.</li>
 *   <li><b>UI baru (belum aktif):</b> {@code WEB-INF/new/sekolah/uiux/kelompok_matapelajaran.jsp}
 *       dan {@code .../services/kelompok_matapelajaran_service.jsp} masih berupa <i>scaffold</i>
 *       hasil generator ({@code generate_new_jsp_scaffold.py}) yang hanya menyetel atribut request
 *       lalu meneruskan ke dispatcher &mdash; <b>tidak ada akses data</b> di sana.</li>
 * </ul>
 *
 * <h2>Warisan {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan properti induknya sama sekali</b>.
 * Karena itu {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()}, dan
 * {@link #getTanggal_dirubah()} <b>harus</b> dideklarasikan ulang di kelas ini; hal yang sama
 * berlaku untuk field {@code nama}, {@code keterangan}, dan {@code nomorUrut} yang
 * <i>menutupi</i> (shadow) field senama milik induk. Duplikasi tersebut adalah KEHARUSAN TEKNIS,
 * bukan bug &mdash; jangan "dirapikan" dengan menghapusnya.</p>
 *
 * <h2>Kelompok method</h2>
 * <ul>
 *   <li><b>Jejak audit (deklarasi ulang):</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #onUpdate()},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}.</li>
 *   <li><b>Identitas &amp; representasi:</b> {@link #KelompokMatapelajaran()},
 *       {@link #KelompokMatapelajaran(long, Sekolah, String)}, {@link #getId()},
 *       {@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Atribut rumpun:</b> {@link #getNama()}, {@link #setNama(String)},
 *       {@link #getKeterangan()}, {@link #setKeterangan(String)}, {@link #getNomorUrut()},
 *       {@link #setNomorUrut(Integer)}, {@link #getAktif()}, {@link #setAktif(Boolean)}.</li>
 *   <li><b>Terjemahan nama (kolom mati):</b> {@link #getNamaEn()}, {@link #setNamaEn(String)},
 *       {@link #getNamaAr()}, {@link #setNamaAr(String)}, {@link #getNamaCh()},
 *       {@link #setNamaCh(String)}.</li>
 *   <li><b>Hierarki rumpun:</b> {@link #getInduk()}, {@link #setInduk(KelompokMatapelajaran)}.</li>
 *   <li><b>Cakupan multi-tenant:</b> {@link #getSekolah()}, {@link #setSekolah(Sekolah)},
 *       {@link #getYayasan()}, {@link #setYayasan(Yayasan)}.</li>
 * </ul>
 *
 * <h2>Kuirk &amp; catatan penting</h2>
 * <ul>
 *   <li><b>Header hbm2java salah salin.</b> Komentar asli hasil generator berbunyi
 *       <i>"JenisPenilaian generated by hbm2java"</i> &mdash; nama kelas yang disebut adalah
 *       {@link JenisPenilaian}, bukan kelas ini. Sisa salin-tempel, tanpa akibat runtime.</li>
 *   <li><b>{@link #getKeterangan()} MEMBALIK kontrak base class.</b>
 *       {@code GeneralValueObject.getKeterangan()} menormalkan {@code null} menjadi {@code ""} dan
 *       menjanjikan hasil non-null; override di sini mengembalikan field mentah, jadi pemanggil
 *       <b>wajib</b> menyiapkan diri terhadap {@code null}.</li>
 *   <li><b>{@link #getNomorUrut()} tidak pernah {@code null}</b> (memaksa {@code 1}), sehingga
 *       {@code GeneralValueObject.compareTo(...)} yang tidak di-{@code override} di sini <b>selalu
 *       berhenti di cabang pertama</b>. Praktis {@code compareTo} tereduksi menjadi perbandingan
 *       {@code nomorUrut} saja; cabang {@code nim}/{@code nama}/{@code keterangan} MATI. Lihat
 *       uraian lengkap pada {@link #getNomorUrut()}.</li>
 *   <li><b>Getter berefek samping (write-back).</b> {@link #getNomorUrut()} benar-benar
 *       <i>menulis</i> field; {@link #getSekolah()}/{@link #getInduk()} menimpa field dengan hasil
 *       de-proxy {@code check(...)}; {@link #getYayasan()} bahkan menimpa {@code yayasan} dari
 *       {@code getSekolah().getYayasan()}. Karena pemetaan kelas ini memakai <i>property access</i>
 *       (anotasi menempel pada getter), nilai yang dikembalikan getter itulah yang dipakai
 *       dirty-check Hibernate &mdash; termasuk {@link #getAktif()}, {@link #getNamaEn()},
 *       {@link #getNamaAr()}, dan {@link #getNamaCh()} yang tidak menulis field pun tetap membuat
 *       kolomnya ter-{@code UPDATE}. Entity ini {@code @Audited}, jadi revisi Envers bisa lahir
 *       hanya karena barisnya DIBACA.</li>
 *   <li><b>{@code namaEn}/{@code namaAr}/{@code namaCh} adalah kolom mati.</b> Tidak ada satu pun
 *       pembaca maupun penulis di luar berkas ini &mdash; form master tidak punya {@code Textbox}
 *       untuknya, dan daftar properti cetak/impor Excel
 *       ({@code {"id","nama","sekolah","keterangan","induk","nomorUrut"}}) tidak menyebutnya.
 *       Kolomnya tetap terisi diam-diam berkat write-back di atas.</li>
 *   <li><b>Kendali "Tampilkan hanya yang aktif" MATI.</b> Berkas {@code kelompok_matapelajaran.zul}
 *       memuat {@code <checkbox id="searchaktif" label="Tampilkan hanya yang aktif" checked="true"
 *       forward="onClick=onSearchDefault"/>}, tetapi {@code KelompokMatapelajaranAction}
 *       <b>tidak punya field</b> bernama {@code searchaktif} dan {@code initCriteria()} tidak
 *       pernah menambahkan penyaring {@code aktif}. Akibatnya rumpun yang sudah dinonaktifkan tetap
 *       muncul di grid master meski centang itu menyala.</li>
 *   <li><b>Tidak ada penjaga siklus pada {@code induk}.</b> Combobox "Induk" di layar master diisi
 *       dengan seluruh rumpun milik sekolah yang sama <i>termasuk baris yang sedang disunting</i>,
 *       jadi sebuah rumpun bisa dijadikan induk bagi dirinya sendiri. Rekursi
 *       {@code LaporanRaporSiswa.genarateKelompokInternal(...)} tidak menyimpan penanda kunjungan,
 *       sehingga siklus semacam itu berujung rekursi tak berhingga
 *       ({@code StackOverflowError}) saat cetak rapor.</li>
 *   <li><b>NPE nyata di jalur laporan.</b> FK {@code kelompok_matapelajaran_id} pada
 *       {@link Matapelajaran} {@code nullable = true}, dan kewajiban mengisinya hanya ditegakkan di
 *       {@code MatapelajaranAction.onSave()} &mdash; jalur impor Excel
 *       ({@code Common.uploadData(..., Matapelajaran.class, contents)}) melewati validasi itu.
 *       Namun {@code LaporanRaporSiswa} dan {@code LaporanRekapTotalNilai} memanggil
 *       {@code getMatapelajaran().getKelompokMatapelajaran().getNama()} <b>tanpa</b> cek
 *       {@code null} di beberapa titik, padahal di titik lain berkas yang sama justru sudah
 *       memeriksanya &mdash; inkonsistensi yang membuat rapor gagal cetak untuk mata pelajaran
 *       hasil impor tanpa rumpun.</li>
 * </ul>
 *
 * <h2>Catatan keamanan (hasil telusur jalur pemakai)</h2>
 * <p>Layar master dijaga {@code Common.doCheckSecurity()} pada {@code doBeforeCompose()} dan
 * tombol Tambah/Ubah/Hapus mengikuti {@code CommonPrivilages}, jadi akses anonim TIDAK ada.
 * Namun {@code KelompokMatapelajaranAction.initCriteria()} <b>tidak memiliki penyaring tenant
 * sendiri</b>: batas sekolah/yayasan hanya berasal dari dua combobox pencarian yang di-<i>disable</i>
 * di sisi UI oleh {@code InitComboUtil.initYayasanDanSekolahDanSemua(...)}. Bila konteks sekolah
 * dan yayasan pengguna sama-sama kosong, kedua combobox jatuh ke item netral dan kriteria menjadi
 * {@code Restrictions.sqlRestriction("1=1")} &mdash; pola <b>fail-open</b> yang sama dengan keluarga
 * layar master {@code sekolah/} lain. Isi tabel ini bukan data pribadi (hanya nama rumpun mata
 * pelajaran), sehingga dampaknya rendah; catat sebagai konfirmasi tambahan pola tersebut, bukan
 * temuan baru. Yang perlu diperhatikan: daftar properti impor Excel menyertakan kolom {@code "id"},
 * sehingga pengguna yang memegang hak CREATE+UPDATE+DELETE pada menu ini dapat menimpa baris milik
 * sekolah lain lewat berkas unggahan.</p>
 *
 * @see Matapelajaran#getKelompokMatapelajaran()
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "kelompok_matapelajaran", schema = "sekolah")
public class KelompokMatapelajaran extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Dipertahankan apa adanya agar instance yang tersimpan di sesi ZK
	 * atau cache tetap dapat dibaca setelah kelas ini diubah.
	 */
	private static final long serialVersionUID = -8817799955174105108L;
	/** Kunci utama; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor.ubah(this)} lewat callback
	 * {@link #onUpdate()}; tidak ada layar yang mengisinya secara manual.</p>
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah di-{@code UPDATE}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah.
	 *
	 * <p><b>Setter defensif:</b> nilai {@code null} maupun string kosong/spasi <b>diabaikan</b>
	 * (method langsung {@code return}), sehingga jejak audit lama tidak pernah terhapus oleh
	 * konteks pengguna yang gagal diresolusi.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah.
	 *
	 * <p><b>Setter defensif:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong/spasi diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila baris belum pernah di-{@code UPDATE}
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
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini, jadi rumpun yang baru dibuat masuk <b>tanpa jejak</b> {@code oleh}/{@code
	 * olehId} sampai ada penyuntingan pertama.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getNomorUrut()}, {@link #getSekolah()}, {@link #getYayasan()},
	 * {@link #getAktif()}, dan trio {@code getNama*()} dapat membuat baris menjadi "kotor" saat
	 * sekadar DIBACA (lihat catatan write-back pada Javadoc kelas), sehingga callback ini bisa
	 * ikut terpicu pada {@code UPDATE} yang <b>tidak diminta pengguna mana pun</b> &mdash; jejak
	 * audit (dan revisi Envers, karena kelas ini {@code @Audited}) lalu mencatat pengguna yang
	 * kebetulan sedang membuka layar.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilai awalnya diambil dari jam aplikasi {@code ais.ui.util.WaktuUtil.getDate()}
	 * (bukan {@code new Date()}) sehingga tunduk pada penyesuaian waktu global sistem.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; nilai {@code null} diterima.
	 *
	 * <p>Normalnya diisi {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan oleh
	 * kode layar.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Tidak pernah {@code null} untuk instance baru: field-nya diinisialisasi saat objek dibuat
	 * dengan {@code ais.ui.util.WaktuUtil.getDate()}. Dipetakan sebagai
	 * {@code TemporalType.TIMESTAMP} (tanggal + jam).</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik rumpun (cakupan tenant); lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Penanda rumpun masih dipakai; lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Yayasan pemilik rumpun, turunan dari {@link #sekolah}; lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Nama rumpun (wajib, kolom {@code nama}); lihat {@link #getNama()}. */
	private String nama;
	/** Terjemahan Inggris nama rumpun &mdash; kolom mati; lihat {@link #getNamaEn()}. */
	private String namaEn;
	/** Terjemahan Arab nama rumpun &mdash; kolom mati; lihat {@link #getNamaAr()}. */
	private String namaAr;
	/** Keterangan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Rumpun induk (self-reference, kolom {@code induk}); lihat {@link #getInduk()}. */
	private KelompokMatapelajaran induk;
	/** Nomor urut tampil/cetak; lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Terjemahan Mandarin nama rumpun &mdash; kolom mati; lihat {@link #getNamaCh()}. */
	private String namaCh;
	
	/**
	 * Representasi teks baris ini: <b>hanya</b> {@link #getNama()} mentah.
	 *
	 * <p>Meng-{@code override} total {@code GeneralValueObject.toString()} (yang menggabungkan
	 * kode + nama); kolom {@code kode} milik induk memang tidak dipetakan pada entity ini.</p>
	 *
	 * <p><b>Dapat mengembalikan {@code null}</b> &mdash; instance yang baru dibuat lewat
	 * {@link #KelompokMatapelajaran()} (mis. pada {@code KelompokMatapelajaranAction.onAdd()})
	 * belum punya nama. Dipakai antara lain sebagai label item {@code Combobox} pada layar Mata
	 * Pelajaran, jadi nama yang kosong akan tampil sebagai item tanpa teks.</p>
	 *
	 * @return nama rumpun, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Konstruktor kosong wajib Hibernate/JPA; juga dipakai layar master saat menekan tombol
	 * "Tambah" ({@code KelompokMatapelajaranAction.onAdd()}).
	 *
	 * <p>Seluruh properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang diisi jam
	 * aplikasi pada saat inisialisasi field.</p>
	 */
	public KelompokMatapelajaran() {
	}

	/**
	 * Konstruktor "kolom wajib" warisan generator hbm2java.
	 *
	 * <p><b>Tidak dipakai pemanggil mana pun</b> di repositori saat ini; dipertahankan agar
	 * kompatibel dengan kode/skrip lama. Perhatikan bahwa {@code id} yang diberikan di sini
	 * <b>diabaikan Hibernate saat {@code INSERT}</b>, karena kolomnya {@code IDENTITY} dan
	 * dipetakan {@code insertable = false} (lihat {@link #getId()}).</p>
	 *
	 * <p>Argumen {@code sekolah} melewati penyaring yang sama dengan {@link #setSekolah(Sekolah)}:
	 * instance tanpa id dianggap {@code null} (lihat alasannya di sana).</p>
	 *
	 * @param id      id baris; diabaikan pada penyimpanan baru
	 * @param sekolah sekolah pemilik; instance tanpa id disimpan sebagai {@code null}
	 * @param nama    nama rumpun
	 */
	public KelompokMatapelajaran(long id, Sekolah sekolah, String nama) {
		this.id = id;
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data ({@code GenerationType.IDENTITY}) dan dipetakan
	 * {@code insertable = false}, sehingga nilai apa pun yang disetel aplikasi sebelum
	 * {@code INSERT} tidak ikut dikirim. Bernilai {@code null} untuk baris yang belum tersimpan
	 * &mdash; {@code KelompokMatapelajaranAction} memakainya persis begitu untuk membedakan judul
	 * dialog "Tambah" vs "Ubah" dan untuk memutuskan {@code session.load(...)} sebelum menyimpan.</p>
	 *
	 * <p>Id berurutan dan mudah ditebak; lihat catatan keamanan pada Javadoc kelas mengenai jalur
	 * impor Excel yang menyertakan kolom {@code id}.</p>
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
	 * Menyetel kunci utama. Tanpa validasi.
	 *
	 * <p>Praktis hanya dipakai Hibernate dan jalur impor; menyetelnya manual pada baris terkelola
	 * bukan cara yang benar untuk "memindahkan" data.</p>
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik rumpun ini &mdash; batas tenant utama entity ini.
	 *
	 * <p><b>Getter berefek samping:</b> memanggil {@code check(sekolah)} milik
	 * {@link GeneralValueObject} lalu <b>menimpa field</b> {@code sekolah} dengan hasilnya.
	 * {@code check(...)} meresolusi proxy lazy secara berlapis (peta identitas &rarr; cache &rarr;
	 * session aktif &rarr; session baru) dan <b>tidak pernah melempar exception</b>: bila semua
	 * tahap gagal, argumen dikembalikan apa adanya. Konsekuensinya penulisan balik ini dapat
	 * membuat baris dianggap "kotor" dan memicu {@code UPDATE} + revisi Envers meski pengguna
	 * hanya membuka layar.</p>
	 *
	 * <p>Kolom {@code sekolah_id} dipetakan {@code nullable = false}, namun beberapa jalur pembaca
	 * (mis. filter combobox pada {@code MatapelajaranAction}) masih menyiapkan cabang
	 * {@code Restrictions.isNull("sekolah")} &mdash; sisa pola salin-tempel dari entity lain yang
	 * kolomnya memang opsional.</p>
	 *
	 * @return sekolah pemilik; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik rumpun.
	 *
	 * <p><b>Setter defensif:</b> instance {@link Sekolah} yang {@code null} <i>atau</i> yang
	 * id-nya masih {@code null} (belum tersimpan &mdash; mis. item netral "= Sekolah =" pada
	 * combobox) sama-sama disimpan sebagai {@code null}. Ini mencegah Hibernate mencoba melakukan
	 * cascade {@code PERSIST} atas objek setengah jadi, tetapi juga berarti pemanggil <b>tidak</b>
	 * mendapat kesalahan apa pun ketika penyetelan "gagal" &mdash; nilai lama hilang tanpa pesan.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau instance tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik rumpun ini.
	 *
	 * <p><b>Getter DESTRUKTIF &mdash; nilai yang disetel pemanggil bisa dibuang.</b> Alurnya:
	 * ambil {@link #getSekolah()} (yang sendirinya sudah menulis balik field {@code sekolah}),
	 * dan bila hasilnya non-{@code null}, <b>timpa</b> {@code yayasan} dengan
	 * {@code sekolah.getYayasan()}; barulah hasilnya dilewatkan {@code check(...)} dan ditulis
	 * balik sekali lagi. Artinya {@link #setYayasan(Yayasan)} hanya "bertahan" selama sekolahnya
	 * belum terisi &mdash; begitu ada sekolah, yayasan selalu diturunkan darinya.</p>
	 *
	 * <p>Perilaku itu sebenarnya menjaga konsistensi (yayasan tidak mungkin berbeda dari yayasan
	 * sekolahnya), tetapi harganya adalah dua penulisan field pada operasi baca, sehingga baris
	 * dapat menjadi kotor dan memicu {@code UPDATE} + revisi Envers tanpa aksi pengguna.</p>
	 *
	 * <p>Detail kecil: baris pertama menugaskan hasil ke <b>field</b> {@code sekolah} (bukan
	 * variabel lokal), jadi efek sampingnya berlipat &mdash; tidak berbahaya, tetapi menyesatkan
	 * saat dibaca sekilas.</p>
	 *
	 * @return yayasan pemilik, diturunkan dari sekolah bila tersedia; {@code null} bila keduanya
	 *         belum terisi
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
	 * Menyetel yayasan pemilik rumpun.
	 *
	 * <p><b>Setter defensif</b> dengan aturan sama seperti {@link #setSekolah(Sekolah)}: instance
	 * tanpa id disimpan sebagai {@code null}.</p>
	 *
	 * <p><b>Penting:</b> nilai yang disetel di sini akan <b>ditimpa</b> oleh {@link #getYayasan()}
	 * begitu {@link #getSekolah()} mengembalikan sekolah yang punya yayasan. Layar master tetap
	 * memanggilnya ({@code onSave()}) supaya kolom terisi pada baris yang sekolahnya belum
	 * teresolusi.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau instance tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan nama rumpun &mdash; identitas yang dilihat pengguna (kolom "Nama Kelompok"
	 * pada grid master, label item combobox pada layar Mata Pelajaran, dan parameter
	 * {@code "sub_jenis"} pada rapor).
	 *
	 * <p>Meng-{@code override} {@code GeneralValueObject.getNama()} untuk membaca field milik
	 * kelas ini (field induk tidak dipetakan Hibernate). Kolom {@code nama} dipetakan
	 * {@code nullable = false} dan {@code onSave()} pada layar master menolak nama kosong, tetapi
	 * getter ini <b>tetap dapat mengembalikan {@code null}</b> untuk instance yang belum
	 * tersimpan.</p>
	 *
	 * @return nama rumpun, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama rumpun. Tanpa validasi &mdash; pemeriksaan "Nama harus diisi" ada di
	 * {@code KelompokMatapelajaranAction.onSave()}, bukan di sini, sehingga jalur impor Excel
	 * dapat menuliskan nilai kosong.
	 *
	 * @param nama nama rumpun baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan status aktif rumpun, dengan <b>normalisasi</b>: {@code null} dilaporkan sebagai
	 * {@code true}.
	 *
	 * <p>Normalisasi ini penting karena kolom {@code aktif} baru ditambahkan belakangan
	 * ({@code hbm2ddl.auto=update}), sehingga baris lama bernilai {@code NULL} di basis data.
	 * Jalur rapor dan combobox pada {@code MatapelajaranAction} sudah menyesuaikan diri dengan
	 * menulis kriteria {@code or(isNull("aktif"), eq("aktif", true))}.</p>
	 *
	 * <p><b>Kuirk 1 &mdash; satu jalur TIDAK menyesuaikan diri.</b> Combobox "Induk" pada layar
	 * master ini memakai {@code Restrictions.eq("aktif", true)} <i>tanpa</i> cabang
	 * {@code isNull}, sehingga rumpun lama yang kolom {@code aktif}-nya masih {@code NULL} tidak
	 * pernah muncul sebagai calon induk &mdash; padahal getter ini melaporkannya aktif.</p>
	 *
	 * <p><b>Kuirk 2 &mdash; write-back.</b> Karena pemetaan memakai <i>property access</i>,
	 * nilai {@code true} hasil normalisasi inilah yang dibaca dirty-check Hibernate. Baris lama
	 * ber-{@code NULL} otomatis ter-{@code UPDATE} menjadi {@code true} (dan tercatat Envers)
	 * pada flush pertama setelah dibaca &mdash; yang kebetulan juga "menyembuhkan" Kuirk 1.</p>
	 *
	 * <p><b>Kuirk 3 &mdash; grid master mengabaikan status ini.</b> Lihat catatan tentang checkbox
	 * {@code searchaktif} pada Javadoc kelas: rumpun non-aktif tetap terdaftar di layar master.</p>
	 *
	 * @return {@code true} bila rumpun masih dipakai; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif rumpun. Tanpa validasi; {@code null} diterima dan akan terbaca sebagai
	 * {@code true} lewat {@link #getAktif()}.
	 *
	 * <p>Dipanggil dari event {@code onCheck} checkbox "Aktif" pada tiap baris grid master, yang
	 * langsung menyusulinya dengan {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * @param aktif status baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan keterangan bebas rumpun (kolom "Keterangan" pada grid dan form master).
	 *
	 * <p><b>MEMBALIK kontrak base class.</b> {@code GeneralValueObject.getKeterangan()}
	 * menormalkan {@code null} menjadi {@code ""} dan menjanjikan hasil non-null; override di sini
	 * mengembalikan field mentah sehingga <b>bisa {@code null}</b>. Pemanggil wajib bersiap:
	 * {@code KelompokMatapelajaranRenderer} membuat {@code new Label(getKeterangan())} tanpa
	 * penjaga (aman di ZK, tetapi menghasilkan sel kosong), dan cabang {@code keterangan} pada
	 * {@code GeneralValueObject.compareTo(...)} kehilangan jaminan non-null-nya &mdash; meski
	 * cabang itu memang sudah mati di kelas ini (lihat {@link #getNomorUrut()}).</p>
	 *
	 * @return keterangan rumpun, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas rumpun. Tanpa validasi; {@code null} diterima.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan rumpun <b>induk</b> dari rumpun ini &mdash; tulang punggung struktur
	 * berjenjang yang dipakai rapor.
	 *
	 * <p>Bernilai {@code null} untuk rumpun <i>akar</i>. {@code LaporanRaporSiswa} mengambil
	 * seluruh akar ({@code induk IS NULL}) milik satu sekolah, lalu untuk tiap akar yang belum
	 * punya mata pelajaran langsung, mencari anak-anaknya ({@code eq("induk", akar)}) dan
	 * ber-rekursi &mdash; sehingga nama rumpun terdalam-lah yang akhirnya menjadi judul blok nilai
	 * ({@code "sub_jenis"}).</p>
	 *
	 * <p><b>Getter berefek samping:</b> memanggil {@code check(induk)} dan <b>menimpa field</b>
	 * dengan hasil de-proxy, dengan konsekuensi dirty/Envers yang sama seperti
	 * {@link #getSekolah()}.</p>
	 *
	 * <p><b>Tidak ada penjaga siklus.</b> Combobox "Induk" pada layar master menampilkan seluruh
	 * rumpun aktif milik sekolah yang sama <i>termasuk baris yang sedang disunting</i>, dan
	 * rekursi rapor tidak menyimpan penanda kunjungan. Rantai melingkar (A&rarr;A, atau
	 * A&rarr;B&rarr;A) karena itu berujung {@code StackOverflowError} saat cetak rapor, bukan
	 * pesan kesalahan yang ramah.</p>
	 *
	 * @return rumpun induk, atau {@code null} bila rumpun ini berada di tingkat akar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "induk")
	public KelompokMatapelajaran getInduk() {
		induk = check(induk);
		return induk;
	}

	/**
	 * Menyetel rumpun induk.
	 *
	 * <p>Berbeda dari {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}, setter ini
	 * <b>tidak</b> menyaring instance tanpa id &mdash; nilai apa pun (termasuk objek yang belum
	 * tersimpan, bahkan {@code this} sendiri) diterima apa adanya. Tidak ada pemeriksaan siklus di
	 * lapis mana pun; lihat peringatan pada {@link #getInduk()}.</p>
	 *
	 * @param induk rumpun induk baru, atau {@code null} untuk menjadikan rumpun ini akar
	 */
	public void setInduk(KelompokMatapelajaran induk) {
		this.induk = induk;
	}
	
	/**
	 * Mengembalikan nomor urut tampil/cetak rumpun (kolom "Urut" pada grid master, dan
	 * {@code ORDER BY nomorurut} pada pemindaian rumpun akar di rapor).
	 *
	 * <p><b>Getter DESTRUKTIF.</b> Bila field masih {@code null}, method ini <i>menulis</i>
	 * {@code 1} ke field sebelum mengembalikannya &mdash; bukan sekadar menormalkan nilai
	 * kembalian. Baris lama yang kolom {@code nomorurut}-nya {@code NULL} karena itu akan
	 * ter-{@code UPDATE} menjadi {@code 1} (lengkap dengan revisi Envers) pada flush pertama
	 * setelah dibaca, tanpa ada pengguna yang memintanya.</p>
	 *
	 * <p>Ekspresi terakhir ({@code nomorUrut == null ? 1 : nomorUrut}) sudah <b>tidak mungkin</b>
	 * bercabang ke sisi kiri karena {@code if} di atasnya pasti sudah mengisi field &mdash; sisa
	 * penulisan defensif ganda, tanpa akibat.</p>
	 *
	 * <p><b>Akibat pada pengurutan (penting).</b> {@code GeneralValueObject.compareTo(...)} tidak
	 * di-{@code override} di sini dan memakai getter, bukan field. Karena getter ini
	 * <b>tidak pernah</b> mengembalikan {@code null}, cabang pertama {@code compareTo} selalu
	 * terpenuhi dan tiga cabang berikutnya ({@code nim}, {@code nama}, {@code keterangan})
	 * <b>MATI TOTAL</b>. Dua rumpun yang sama-sama bernomor urut bawaan {@code 1} karena itu
	 * dinyatakan setara ({@code 0}) meskipun namanya berbeda. Dampaknya:</p>
	 * <ul>
	 *   <li><b>Combobox</b> &mdash; {@code CommonComboInsertHelper} mengurutkan dengan
	 *       {@code Collections.sort(List)}, jadi hasil "setara" hanya berarti urutan pemuatan dari
	 *       basis data dipertahankan (pengurutan alfabetis yang mungkin diharapkan pengguna tidak
	 *       terjadi). <b>Tidak ada</b> penciutan data.</li>
	 *   <li><b>Grid dan rapor</b> &mdash; keduanya mengurutkan lewat SQL
	 *       ({@code Order.asc("nama")} / {@code Order.asc("nomorUrut")}), jadi tidak terpengaruh
	 *       sama sekali.</li>
	 *   <li><b>{@code TreeSet}/{@code TreeMap}</b> &mdash; tidak ada satu pun konsumen yang memakai
	 *       struktur berbasis {@code compareTo} untuk entity ini, sehingga bug "penciutan senyap"
	 *       yang dikenal di keluarga entity lain <b>TIDAK berlaku</b> di sini. Tetap hindari bila
	 *       menambah kode baru: {@code compareTo} kelas ini tidak konsisten dengan
	 *       {@code equals}.</li>
	 * </ul>
	 *
	 * @return nomor urut rumpun; tidak pernah {@code null} (minimal {@code 1})
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil/cetak. Tanpa validasi; nilai negatif atau ganda diterima
	 * (nomor urut kembar tidak menghilangkan baris &mdash; lihat {@link #getNomorUrut()}).
	 *
	 * <p>Dipanggil dari event {@code onChange} {@code Intbox} pada tiap baris grid master, yang
	 * langsung menyusulinya dengan {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan dipulihkan menjadi {@code 1} pada
	 *                  pembacaan berikutnya
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan terjemahan Inggris nama rumpun, dengan <b>fallback</b> ke {@link #getNama()}
	 * bila belum diisi.
	 *
	 * <p><b>Kolom mati.</b> Tidak ada satu pun pembaca maupun penulis {@code namaEn} untuk entity
	 * ini di luar berkas ini: form master tidak menyediakan isian terjemahan, dan daftar properti
	 * cetak/impor Excel ({@code {"id","nama","sekolah","keterangan","induk","nomorUrut"}}) tidak
	 * menyebutnya. Bandingkan dengan {@link Matapelajaran} yang justru memuat
	 * {@code namaEn}/{@code namaAr}/{@code namaCh} di daftar propertinya &mdash; pola yang tidak
	 * pernah dituntaskan untuk kelas ini.</p>
	 *
	 * <p><b>Efek fallback pada penyimpanan.</b> Karena pemetaan memakai <i>property access</i>,
	 * nilai kembalian getter inilah yang dibaca dirty-check Hibernate. Baris ber-{@code NULL}
	 * karena itu ter-{@code UPDATE} dengan <i>salinan</i> {@code nama} pada flush pertama setelah
	 * dibaca. Setelah itu, "belum diterjemahkan" tidak lagi dapat dibedakan dari "terjemahannya
	 * memang sama".</p>
	 *
	 * @return terjemahan Inggris, atau nama Indonesia bila belum diisi
	 */
	public String getNamaEn() {
		return namaEn == null ? getNama() : namaEn;
	}

	/**
	 * Menyetel terjemahan Inggris nama rumpun. Tanpa validasi; tidak ada layar yang memanggilnya
	 * (lihat {@link #getNamaEn()}).
	 *
	 * @param namaEn terjemahan Inggris baru
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan terjemahan Arab nama rumpun, dengan <b>fallback</b> ke {@link #getNama()} bila
	 * belum diisi.
	 *
	 * <p>Kolom mati dengan karakteristik persis sama seperti {@link #getNamaEn()}, termasuk efek
	 * penulisan balik salinan {@code nama} ke kolom {@code namaar}.</p>
	 *
	 * @return terjemahan Arab, atau nama Indonesia bila belum diisi
	 */
	public String getNamaAr() {
		return namaAr == null ? getNama() : namaAr;
	}

	/**
	 * Menyetel terjemahan Arab nama rumpun. Tanpa validasi; tidak ada layar yang memanggilnya.
	 *
	 * @param namaAr terjemahan Arab baru
	 */
	public void setNamaAr(String namaAr) {
		this.namaAr = namaAr;
	}

	/**
	 * Mengembalikan terjemahan Mandarin nama rumpun, dengan <b>fallback</b> ke {@link #getNama()}
	 * bila belum diisi.
	 *
	 * <p>Kolom mati dengan karakteristik persis sama seperti {@link #getNamaEn()}, termasuk efek
	 * penulisan balik salinan {@code nama} ke kolom {@code namach}.</p>
	 *
	 * @return terjemahan Mandarin, atau nama Indonesia bila belum diisi
	 */
	public String getNamaCh() {
		return namaCh == null ? getNama() : namaCh;
	}

	/**
	 * Menyetel terjemahan Mandarin nama rumpun. Tanpa validasi; tidak ada layar yang memanggilnya.
	 *
	 * @param namaCh terjemahan Mandarin baru
	 */
	public void setNamaCh(String namaCh) {
		this.namaCh = namaCh; 
	}
}
