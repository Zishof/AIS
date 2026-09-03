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

import org.hibernate.envers.Audited;

/**
 * Entity <b>penghubung</b> antara definisi field kustom generik dan kategori tampilnya pada modul
 * <b>Catatan Administrasi</b>, dipetakan ke tabel {@code public.parameter_tambahan_catatan_siswa}
 * (nama tabel warisan &mdash; lihat "Catatan warisan &amp; pemetaan" di bawah).
 *
 * <h3>Posisi dalam rantai field kustom Catatan Administrasi</h3>
 * <p>AIS mengizinkan setiap institusi menambah pertanyaan/isian sendiri pada formulir Catatan
 * Administrasi tanpa mengubah skema database. Rantainya <b>empat lapis</b>:</p>
 * <ol>
 *   <li>{@link JenisCatatanAdministrasi} &mdash; jenis/template catatan. Lewat relasi
 *   {@code @ManyToMany} {@link JenisCatatanAdministrasi#getKelompokParameterTambahanCatatanAdministrasis()},
 *   admin <b>mencentang</b> kategori mana saja yang ikut muncul untuk jenis catatan tersebut.
 *   Kategori yang tidak dicentang tidak akan pernah dirender, seberapa pun lengkap isinya.</li>
 *   <li>{@link KelompokParameterTambahanCatatanAdministrasi} &mdash; <b>kategori/judul kelompok</b>;
 *   satu baris di sana menjadi satu heading seksi pada formulir isian.</li>
 *   <li><b>Kelas ini</b> &mdash; baris penghubung yang <b>mengadopsi</b> satu
 *   {@link ParameterTambahan} ke dalam satu kategori di atas. Perhatikan: entity ini
 *   <b>hanya membawa pasangan (kategori, parameter)</b> &mdash; tidak ada penyaring cakupan
 *   (fakultas/angkatan/prodi) dan tidak ada penimpa {@code wajibDiisi} seperti pada saudaranya
 *   {@link ParameterTambahanAlumni} maupun {@link ParameterTambahanMahasiswa}.</li>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi</b> field kustom generik: label
 *   ({@code labelInputan}), tipe input ({@code tipeDataInputan}), daftar pilihan
 *   ({@code nilaiDataInputan}), wajib/tidak, perlu lampiran/tidak, nomor urut, bahkan induk
 *   ({@code parent}) untuk field bersarang. Tabel itu dipakai bersama oleh banyak modul
 *   (mahasiswa, alumni, calon mahasiswa, catatan pegawai, pengaduan, angket, &hellip;) &mdash; ia
 *   tidak tahu-menahu soal catatan administrasi.</li>
 * </ol>
 * <p>Karena adopsi terjadi di lapis ini, satu {@link ParameterTambahan} yang sama bisa dipakai
 * berkali-kali (di kategori berbeda, atau oleh modul lain) tanpa digandakan definisinya.</p>
 *
 * <h3>Siapa yang memakai baris-baris ini (terverifikasi dari kode pemanggil)</h3>
 * <ul>
 *   <li><b>Layar admin</b> {@code ais.action.master.ParameterTambahanCatatanAdministrasiAction}
 *   (CRUD). {@code doAfterCompose()} layar tersebut memanggil
 *   {@link KelompokParameterTambahanCatatanAdministrasi#checkCreateDefault()} &mdash; jadi layar
 *   milik entity INILAH pemicu utama auto-seed kategori bawaan pada rantai ini. Layar yang sama
 *   juga menyediakan tab "Manajemen Kelompok"
 *   ({@code /pages/master/kelompok_parameter_tambahan_catatan_administrasi.zul}) dan "Manajemen
 *   Parameter" ({@code /pages/master/parameter_tambahan.zul}) sebagai {@code MyInclude}.</li>
 *   <li><b>Perakit formulir</b>
 *   {@code ais.action.master.helper.ParameterTambahanCatatanAdministrasiListener} &mdash; membangun
 *   baris-baris input ZK per kategori dan memvalidasi isian wajib; dipakai oleh
 *   {@code ais.action.master.CatatanAdministrasiAction}.</li>
 *   <li><b>Penampil ringkas</b> {@code CatatanAdministrasiAction} sendiri (render read-only isian
 *   pada detail catatan).</li>
 *   <li><b>Laporan</b> {@code ais.action.report.format1.akademik.LaporanCatatanAdministrasi}
 *   (menyiapkan {@code map} parameter JasperReports).</li>
 *   <li><b>Broadcast</b> {@code ais.action.master.helper.BroadcastHelper} (menyusun badan
 *   email/notifikasi berisi seluruh isian field kustom).</li>
 * </ul>
 * <p><b>Pola query yang seragam di keempat pembaca di atas</b> &mdash; layak diketahui karena
 * menentukan apa yang benar-benar berpengaruh dari baris entity ini:</p>
 * <pre>
 * session.createCriteria(ParameterTambahanCatatanAdministrasi.class)
 *     .add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi", kel))
 *     .createAlias("parameterTambahan", "parameterTambahan")
 *     .createAlias("kelompokParameterTambahanCatatanAdministrasi", "kelompokParameterTambahanCatatanAdministrasi")
 *     .add(Restrictions.eq("parameterTambahan.aktif", true))
 *     .add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi.aktif", true))
 *     .setProjection(Projections.groupProperty("parameterTambahan.id"))
 * </pre>
 * <p>Perhatikan {@code setProjection(groupProperty("parameterTambahan.id"))}: hasil query
 * <b>bukan</b> baris entity ini, melainkan daftar {@link ParameterTambahan}. Artinya baris
 * penghubung ini berfungsi murni sebagai <i>tabel relasi</i> saat runtime; tidak satu pun
 * property miliknya sendiri (termasuk {@link #getNomorUrut() nomorUrut}) yang ikut terbaca.
 * Pengurutan field di formulir sepenuhnya berasal dari {@code Collections.sort(parameterTambahans)},
 * yaitu {@link ParameterTambahan#compareTo(GeneralValueObject)} atas {@code nomorUrut} milik
 * {@link ParameterTambahan}. Efek samping {@code groupProperty}: bila satu parameter yang sama
 * diadopsi dua kali ke kategori yang sama, ia tetap muncul sekali saja di formulir.</p>
 *
 * <h3>Di mana isian pengguna sesungguhnya disimpan (TERVERIFIKASI dari kode)</h3>
 * <p>Entity ini murni <b>konfigurasi</b>; tidak ada satu pun kolom nilai di sini. Jawaban pengguna
 * disimpan sebagai <b>string terserialisasi</b> pada DUA kolom {@code text} di
 * {@link CatatanAdministrasi} (BUKAN {@code BiodataMahasiswa} seperti pada rantai Alumni/Mahasiswa),
 * ditulis oleh {@link CatatanAdministrasi#populateParameterTambahan(java.util.List)}:</p>
 * <ol>
 *   <li>{@link CatatanAdministrasi#getParameterTambahan()} &mdash; <b>versi berlabel</b>, <b>7 ruas</b>
 *   per baris (bukan 8 seperti varian Alumni/Mahasiswa &mdash; ruas {@code indexKe} tidak ada di
 *   sini); baris dipisah {@code "\n"} dan ruas dipisah {@code "<=>"}:
 *   <pre>
 *   namaKelompok "-&gt;" labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut
 *       &lt;=&gt; idParameterTambahan &lt;=&gt; idKelompok &lt;=&gt; keterangan
 *   </pre>
 *   dibongkar kembali oleh {@link CatatanAdministrasi#ambilDataParameterTambahan()} menjadi daftar
 *   {@code CommonVO} untuk layar tampil dan laporan. Pembongkar itu <b>hanya membaca ruas 1&ndash;5</b>
 *   ({@code lbl}, {@code val}, {@code url}, {@code nomorUrut}, {@code id}); ruas 6
 *   ({@code idKelompok}) dan ruas 7 ({@code keterangan}) ditulis tetapi tidak pernah dibaca kembali
 *   lewat jalur ini;</li>
 *   <li>{@link CatatanAdministrasi#getParameterTambahanInds()} &mdash; <b>versi ber-ID</b>, 4 ruas
 *   per baris:
 *   <pre>
 *   idKelompok "-&gt;" idParameterTambahan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan
 *   </pre>
 *   inilah yang dipakai mesin. Ketiga pembaca runtime (layar detail, laporan, broadcast) memindai
 *   kolom ini dengan {@code split("\n")} lalu {@code split("<=>")} dan mencocokkan ruas pertama
 *   secara {@code equalsIgnoreCase} terhadap kunci gabungan.</li>
 * </ol>
 * <p><b>Kunci penyimpanan.</b> Jawaban dialamatkan oleh pasangan
 * <code>idKelompok + "-&gt;" + idParameterTambahan</code>. String yang sama juga dipakai sebagai
 * penanda {@code jenis} pada {@link ais.database.model.file.LampiranLain#ambil(Long, String)} untuk
 * berkas lampiran (dengan pemilik = {@code catatanAdministrasi.getId()}).
 * <b>ID baris entity ini sendiri tidak pernah ikut disimpan.</b> Akibatnya:</p>
 * <ul>
 *   <li>menghapus lalu membuat ulang baris penghubung ini <b>tidak</b> memutus jawaban lama, selama
 *   pasangan kategori+parameter yang sama dipakai lagi;</li>
 *   <li>sebaliknya, <b>memindahkan</b> sebuah parameter ke kategori lain mengubah kunci, sehingga
 *   seluruh jawaban historis menjadi yatim di dalam kolom {@code text} &mdash; tetap tersimpan,
 *   tetapi tidak pernah terbaca lagi oleh formulir, laporan, maupun broadcast; lampiran yang
 *   tersimpan dengan {@code jenis} lama pun ikut tidak terjangkau;</li>
 *   <li>karena penyimpanannya string, tidak ada foreign key: menghapus {@link ParameterTambahan}
 *   atau kategori tidak membersihkan jawaban lama.</li>
 * </ul>
 * <p><b>Kunci ketiga yang khas modul ini.</b> {@code LaporanCatatanAdministrasi} membentuk pula
 * varian <code>idKelompok + "_" + idParameterTambahan</code> ({@code jenis_id}) sebagai nama
 * parameter JasperReports ({@code map.put(jenis_id, nilai)} dan {@code jenis_id + "_url"} untuk path
 * lampiran) &mdash; garis bawah, bukan panah, karena {@code "-&gt;"} tidak valid sebagai nama
 * parameter laporan. Jadi satu pasangan kategori+parameter punya DUA bentuk kunci yang harus
 * dijaga konsisten bila ada perubahan.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi rantai:</b> {@link #getParameterTambahan()} (definisi field, wajib),
 *   {@link #getKelompokParameterTambahanCatatanAdministrasi()} (kategori/heading, secara pemetaan
 *   opsional).</li>
 *   <li><b>Pengurutan (turunan):</b> {@link #getNomorUrut()} &mdash; bukan nilai mandiri, melainkan
 *   cerminan {@code nomorUrut} milik {@link ParameterTambahan}; lihat catatan write-back di bawah.</li>
 *   <li><b>Konstruktor:</b> {@link #ParameterTambahanCatatanAdministrasi()} (tanpa argumen, syarat
 *   Hibernate).</li>
 * </ul>
 * <p>Kelas ini <b>tidak</b> meng-override {@code compareTo}, {@code toString}, {@code getNama}, atau
 * {@code equals} &mdash; semuanya diwarisi dari {@link GeneralValueObject}. Karena
 * {@link #getNomorUrut()} di sini tidak pernah mengembalikan {@code null} (ada fallback {@code 1}),
 * {@link GeneralValueObject#compareTo(GeneralValueObject)} SELALU berhenti di cabang pertama
 * ({@code nomorUrut}) dan tidak pernah sampai ke fallback {@code getNim()}/{@code getNama()}/
 * {@code getKeterangan()}. Konsekuensinya {@code compareTo} mengembalikan {@code 0} untuk dua baris
 * yang parameternya kebetulan bernomor urut sama &mdash; aman selama hasil query ditampung di
 * {@code List} (kondisi saat ini di seluruh pemanggil), tetapi akan menciutkan data secara senyap
 * bila kelak ada yang menampungnya di {@code TreeSet}.</p>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li><b>Nama tabel dan nama kolom menyesatkan.</b> Entity ini dipetakan ke
 *   {@code public.parameter_tambahan_catatan_siswa} &mdash; sisa salin-tempel dari modul sekolah,
 *   bukan tabel milik modul Siswa. (Tidak ada tabrakan pemetaan: satu-satunya entity lain dengan
 *   nama tabel sama, {@code ais.database.model.asset.ParameterTambahanPerbaikanAsset}, berada di
 *   skema {@code asset}; sedangkan {@code ais.database.model.sekolah.ParameterTambahanCatatanSiswa}
 *   justru dipetakan ke {@code sekolah.parameter_tambahan_alur_sop}.) Senada, kolom FK kategori
 *   bernama {@code kelompok_parameter_tambahan_alur_sop} &mdash; sisa salin-tempel dari modul SOP
 *   &mdash; padahal ia menunjuk tabel {@code kelompok_parameter_tambahan_catatan_administrasi}.
 *   Keduanya berfungsi normal; hanya menyesatkan saat menelusuri database langsung. Dicatat apa
 *   adanya, tidak diubah (mengganti nama tabel/kolom akan memutus data yang sudah ada).</li>
 *   <li>{@link GeneralValueObject} BUKAN {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia
 *   POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan satu pun property induknya</b>. Karena
 *   itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, dan
 *   {@code nomorUrut} di kelas ini <b>bukan bug</b>, melainkan keharusan teknis agar kolom-kolom itu
 *   benar-benar tersimpan.</li>
 *   <li>Anotasi {@code @Id} berada pada <b>getter</b>, sehingga Hibernate memakai <i>property
 *   access</i> untuk seluruh property. Digabung dengan {@code dynamicUpdate = true}, getter yang
 *   menulis balik ke field ({@link #getNomorUrut()}, dan {@code check(...)} pada kedua getter
 *   relasi) dapat mengotori entity dan memicu {@code UPDATE} beserta revisi Envers baru pada baris
 *   yang <b>sekadar dibaca</b>.</li>
 *   <li>Property tanpa {@code @Column} eksplisit ({@code oleh}, {@code olehId},
 *   {@code tanggal_dirubah}, {@code nomorUrut}) memakai nama kolom apa adanya sesuai strategi
 *   penamaan bawaan Hibernate.</li>
 *   <li>Kedua relasi {@code @ManyToOne} memakai {@code cascade = {PERSIST, MERGE}} dan
 *   {@code fetch = LAZY}, jadi menyimpan baris ini bisa ikut mem-{@code persist}/{@code merge}
 *   master yang direferensikan.</li>
 *   <li>Kelas ini TIDAK memiliki field {@code keterangan} sendiri; {@code getKeterangan()},
 *   {@code getNama()}, {@code getNim()}, dan {@link #toString()} sepenuhnya diwarisi dari
 *   {@link GeneralValueObject} dan selalu bernilai bawaan karena tidak dipetakan.</li>
 *   <li>{@code @Audited} (Envers) aktif: setiap perubahan baris konfigurasi ini terekam di tabel
 *   audit, termasuk {@code UPDATE} tak sengaja dari getter write-back di atas.</li>
 * </ul>
 *
 * <h3>Kuirk layar admin yang mempengaruhi data entity ini</h3>
 * <ul>
 *   <li><b>{@code nomorUrut} tidak bisa diisi lewat layar.</b> Dialog "Tambah/Ubah Parameter" hanya
 *   memuat dua baris: Kelompok (combobox {@code readonly}) dan Parameter. {@code onSave()} pun hanya
 *   menulis kedua relasi itu. Satu-satunya jalur yang menyentuh {@code nomorUrut} adalah fitur
 *   unggah data massal ({@code Common.uploadData(this, ParameterTambahanCatatanAdministrasi.class,
 *   contents)} dengan {@code contents} memuat {@code "nomorUrut"}) &mdash; dan nilai itu pun akan
 *   tertimpa lagi oleh {@link #getNomorUrut()} pada pembacaan berikutnya.</li>
 *   <li><b>Kolom pencarian memakai {@code parameterTambahan.nama}, tampilan memakai
 *   {@code labelInputan}.</b> {@code initCriteria()} menyaring dengan
 *   {@code Restrictions.ilike("parameterTambahan.nama", ...)} sementara grid menampilkan
 *   {@code getLabelInputan()}. Aman karena {@link ParameterTambahan#getNama()} menambal dirinya dari
 *   {@code labelInputan} bila kosong, tetapi baris lama yang punya {@code nama} berbeda dari
 *   {@code labelInputan} akan tampak "tidak ketemu" saat dicari dengan teks yang terlihat di layar.</li>
 *   <li><b>Baris tanpa kategori berpotensi NPE di layar daftar.</b> Kolom FK kategori dipetakan
 *   {@code nullable = true}, sedangkan renderer grid memanggil
 *   {@code getKelompokParameterTambahanCatatanAdministrasi().getNama()} tanpa pemeriksaan null.
 *   Jalur UI normal selalu mengisi kategori (divalidasi {@code onSave()}), tetapi baris hasil unggah
 *   massal atau SQL mentah tanpa kategori akan meledak saat baris itu dirender.</li>
 * </ul>
 *
 * <h3>Catatan keamanan (kondisi saat ini, dicatat apa adanya)</h3>
 * <p>{@code ais.action.master.ParameterTambahanCatatanAdministrasiAction} men-<i>hardcode</i>
 * {@code private boolean edit = true;} dan {@code private boolean delete = true;} serta
 * <b>tidak pernah memanggil {@code checkPrevilages}</b> di seluruh berkasnya. Satu-satunya gerbang
 * adalah {@code Common.doCheckSecurity()} di {@code doBeforeCompose()} yang hanya memastikan ada
 * sesi login, bukan hak per-modul. Praktisnya: siapa pun yang dapat membuka layar ini dapat
 * menambah, mengubah, dan menghapus pemetaan field kustom Catatan Administrasi. Karena kunci
 * penyimpanan jawaban adalah pasangan kategori+parameter (lihat di atas), perubahan pemetaan oleh
 * pengguna tak berhak dapat membuat isian historis menjadi yatim. Bandingkan dengan
 * {@code KelompokParameterTambahanCatatanAdministrasiAction} yang pada umumnya memiliki guard yang
 * benar. Tidak diperbaiki di sini &mdash; hanya didokumentasikan.</p>
 *
 * @see ParameterTambahan
 * @see KelompokParameterTambahanCatatanAdministrasi
 * @see JenisCatatanAdministrasi
 * @see CatatanAdministrasi#populateParameterTambahan(java.util.List)
 * @see CatatanAdministrasi#ambilDataParameterTambahan()
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "parameter_tambahan_catatan_siswa")
public class ParameterTambahanCatatanAdministrasi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai tetap ini menjaga kompatibilitas biner instance yang
	 * pernah diserialisasi (mis. saat di-cache atau dikirim antar node) meski struktur field kelas
	 * berubah. Jangan diubah kecuali memang ingin memutus kompatibilitas tersebut secara sengaja.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-increment; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pembuat/pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;
	/** ID/NIP-NIM pengguna pembuat/pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas (ID pengguna) pihak yang terakhir membuat atau mengubah baris ini.
	 *
	 * @return ID pengguna terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna pembuat/pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>mengabaikan</b> nilai {@code null} maupun string
	 * kosong/spasi &mdash; nilai lama dipertahankan. Jejak audit karena itu tidak pernah bisa
	 * dikosongkan lewat setter ini; ia hanya bisa ditimpa dengan nilai baru yang berisi. Perilaku
	 * ini disengaja agar {@code AuditTimestampInterceptor} yang tidak berhasil menentukan pengguna
	 * tidak menghapus jejak sebelumnya.</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim()}
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pembuat/pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong/spasi diabaikan sehingga jejak audit lama tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim()}
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir membuat atau mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini
	 * dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang aktif. Tidak perlu &mdash; dan
	 * tidak boleh &mdash; dipanggil manual dari kode aplikasi.</p>
	 *
	 * <p><b>Efek samping yang perlu diingat:</b> karena getter write-back kelas ini
	 * ({@link #getNomorUrut()}) dapat membuat entity menjadi <i>dirty</i> hanya karena dibaca,
	 * callback ini juga ikut berjalan pada baris yang sebenarnya tidak diubah pengguna &mdash;
	 * menghasilkan revisi Envers dan cap waktu baru yang tidak mencerminkan perubahan nyata.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat (lewat
	 * {@code ais.ui.util.WaktuUtil#getDate()}), sehingga baris baru selalu punya nilai meski
	 * pemanggil tidak mengisinya; selanjutnya diperbarui oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir. Umumnya diisi otomatis oleh {@link #onUpdate()};
	 * pemanggilan manual hanya untuk keperluan migrasi/impor data.
	 *
	 * @param tanggal_dirubah cap waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor Java, tetapi bisa {@code null} untuk baris lama hasil migrasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kategori/heading tempat parameter ini diadopsi; lihat {@link #getKelompokParameterTambahanCatatanAdministrasi()}. */
	private KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi;
	/** Definisi field kustom generik yang diadopsi; lihat {@link #getParameterTambahan()}. */
	private ParameterTambahan parameterTambahan;

	/** Cache nomor urut yang dicerminkan dari {@link ParameterTambahan}; lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut tampil baris ini &mdash; <b>bukan nilai mandiri</b>, melainkan
	 * cerminan {@code nomorUrut} milik {@link ParameterTambahan} yang diadopsi.
	 *
	 * <p><b>Alur:</b> memanggil {@link #getParameterTambahan()} (yang sekaligus me-resolve proxy
	 * lazy lewat {@link GeneralValueObject#check(Object)}), lalu &mdash; bila definisinya ada &mdash;
	 * <b>menimpa field {@link #nomorUrut}</b> dengan {@code parameterTambahan.getNomorUrut()}.
	 * Terakhir mengembalikan nilai tersebut, dengan fallback {@code 1} bila hasilnya {@code null}.</p>
	 *
	 * <p><b>Efek samping (penting).</b> Karena Hibernate memakai <i>property access</i> pada kelas
	 * ini, penimpaan field di atas membuat entity menjadi <i>dirty</i>: sekadar <b>membaca</b> baris
	 * yang nilai {@code nomorUrut}-nya di database berbeda dari nilai di {@link ParameterTambahan}
	 * akan memicu {@code UPDATE} pada flush berikutnya, lengkap dengan revisi Envers dan pembaruan
	 * {@link #getTanggal_dirubah()} yang tidak berasal dari tindakan pengguna. Kondisi ini adalah
	 * kondisi <b>bawaan</b>, karena layar admin tidak pernah mengisi {@code nomorUrut} (lihat catatan
	 * kelas). Perhatikan pula asimetri kecil: fallback {@code 1} hanya memengaruhi nilai kembalian,
	 * TIDAK ditulis balik ke field &mdash; jadi kolom database bisa tetap {@code NULL} sementara
	 * pemanggil selalu menerima angka.</p>
	 *
	 * <p><b>Siapa yang memakai.</b> Tidak ada pembaca runtime di luar kelas ini yang memanggilnya
	 * secara langsung; pengurutan field pada formulir/laporan berasal dari {@code nomorUrut} milik
	 * {@link ParameterTambahan}. Pemakai tak langsung satu-satunya adalah
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} &mdash; dan karena method ini tidak
	 * pernah mengembalikan {@code null}, cabang pembanding lain di sana menjadi kode mati bagi
	 * entity ini.</p>
	 *
	 * @return nomor urut efektif; tidak pernah {@code null} ({@code 1} bila tidak dapat ditentukan)
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menetapkan nomor urut tampil baris ini.
	 *
	 * <p><b>Perhatikan:</b> nilai yang ditetapkan di sini bersifat sementara &mdash;
	 * {@link #getNomorUrut()} akan menimpanya dengan nilai dari {@link ParameterTambahan} pada
	 * pembacaan berikutnya. Satu-satunya pemanggil di aplikasi adalah jalur unggah data massal
	 * ({@code Common.uploadData}); dialog Tambah/Ubah tidak memuat kolom ini.</p>
	 *
	 * @param nomorUrut nomor urut baru; boleh {@code null}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor tanpa argumen. Wajib ada dan harus tetap publik agar Hibernate dapat
	 * meng-instansiasi entity saat memuat baris dari database; juga dipakai layar admin
	 * ({@code onAdd()}) untuk membuat baris baru sebelum kedua relasinya diisi.
	 */
	public ParameterTambahanCatatanAdministrasi() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}/sequence), bukan dikirim aplikasi. Bernilai {@code null} selama objek belum
	 * disimpan &mdash; kondisi inilah yang dipakai layar admin untuk membedakan mode "Tambah
	 * Parameter" dari "Ubah Parameter".</p>
	 *
	 * @return ID baris, atau {@code null} bila belum pernah tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key baris ini. Praktisnya hanya dipakai Hibernate saat memuat baris dan
	 * oleh jalur impor data; kode aplikasi tidak boleh menetapkannya manual.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan definisi field kustom generik yang diadopsi baris ini.
	 *
	 * <p>Inilah sumber seluruh perilaku field di formulir: label, tipe input, daftar pilihan,
	 * wajib/tidak, perlu lampiran/tidak, dan nomor urut. Kolom FK
	 * {@code parameter_tambahan} dipetakan {@code nullable = false}, jadi setiap baris penghubung
	 * pasti punya definisi.</p>
	 *
	 * <p><b>Efek samping:</b> nilai dilewatkan {@link GeneralValueObject#check(Object)} lebih dulu
	 * untuk me-resolve proxy lazy (relasi ini {@code FetchType.LAZY}) dan hasilnya <b>ditulis balik
	 * ke field</b>. Method ini karena itu dapat membuka session Hibernate baru bila objek sudah
	 * detached, dan tidak pernah melempar exception meski resolusi gagal (lihat kontrak
	 * {@code check}).</p>
	 *
	 * <p><b>Dipanggil dari:</b> renderer grid layar admin, {@link #getNomorUrut()}, serta seluruh
	 * pemanggil yang menampilkan/mengurutkan field pada formulir Catatan Administrasi.</p>
	 *
	 * @return definisi field kustom yang diadopsi; secara praktis tidak pernah {@code null} untuk
	 *         baris yang tersimpan lewat jalur normal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menetapkan definisi field kustom yang diadopsi baris ini.
	 *
	 * <p>Diisi {@code onSave()} layar admin dari combobox "Parameter". Mengganti nilainya pada baris
	 * yang sudah ada <b>mengubah kunci penyimpanan jawaban</b>
	 * (<code>idKelompok + "-&gt;" + idParameterTambahan</code>), sehingga isian historis untuk
	 * pasangan lama menjadi yatim &mdash; lihat catatan kelas.</p>
	 *
	 * @param parameterTambahan definisi field kustom; tidak boleh {@code null} pada penyimpanan
	 *                          (kolom {@code nullable = false})
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan kategori/heading tempat parameter ini muncul di formulir.
	 *
	 * <p>Kategori inilah yang dicentang admin pada {@link JenisCatatanAdministrasi}; sebuah baris
	 * penghubung hanya ikut dirender bila kategorinya {@code aktif} DAN dicentang pada jenis catatan
	 * yang sedang dibuka.</p>
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getParameterTambahan()}, nilai di-resolve dengan
	 * {@link GeneralValueObject#check(Object)} dan ditulis balik ke field.</p>
	 *
	 * <p><b>Perhatikan pemetaan:</b> kolom FK bernama {@code kelompok_parameter_tambahan_alur_sop}
	 * (nama warisan modul SOP) dan dipetakan {@code nullable = true}. Renderer grid layar admin
	 * memanggil {@code getNama()} atas hasil method ini tanpa pemeriksaan null, sehingga baris tanpa
	 * kategori (hanya mungkin lewat unggah massal/SQL mentah) akan memicu {@code NullPointerException}
	 * saat dirender.</p>
	 *
	 * @return kategori parameter, atau {@code null} bila baris tidak berkategori
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_alur_sop", nullable = true)
	public KelompokParameterTambahanCatatanAdministrasi getKelompokParameterTambahanCatatanAdministrasi() {
		kelompokParameterTambahanCatatanAdministrasi = check(kelompokParameterTambahanCatatanAdministrasi);
		return kelompokParameterTambahanCatatanAdministrasi;
	}

	/**
	 * Menetapkan kategori/heading tempat parameter ini muncul.
	 *
	 * <p>Diisi {@code onAdd()} layar admin dari combobox PENCARIAN kategori yang sedang aktif (bukan
	 * dari form isian), dan diisi ulang {@code onSave()} dari combobox "Kelompok" pada dialog.
	 * Sebagaimana {@link #setParameterTambahan(ParameterTambahan)}, memindahkan baris ke kategori
	 * lain mengubah kunci penyimpanan jawaban dan meninggalkan isian historis dalam keadaan yatim.</p>
	 *
	 * @param kelompokParameterTambahanCatatanAdministrasi kategori parameter; boleh {@code null}
	 *                                                     secara pemetaan, tetapi divalidasi wajib
	 *                                                     oleh layar admin
	 */
	public void setKelompokParameterTambahanCatatanAdministrasi(
			KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi) {
		this.kelompokParameterTambahanCatatanAdministrasi = kelompokParameterTambahanCatatanAdministrasi;
	}

}
