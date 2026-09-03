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
 * <h2>Jenis biaya sekolah &mdash; simpul PALING ATAS rantai penagihan (billing) modul sekolah</h2>
 *
 * <p>Entity ini memetakan tabel <code>sekolah.jenis_biaya_sekolah</code>. Berbeda dari
 * kebanyakan tabel master di paket ini yang hanya berisi <em>label</em>, baris di sini
 * adalah <b>berkas konfigurasi perilaku</b>: satu baris tidak sekadar memberi nama
 * "SPP Bulanan", "Uang Pendaftaran", atau "Daftar Ulang", melainkan menentukan
 * <b>mesin tagihan mana yang dijalankan</b>, <b>siapa yang ditagih</b> (siswa atau calon
 * siswa), <b>kapan penagihan mulai</b>, <b>boleh tidaknya angsuran bebas</b>, serta
 * <b>kanal bank mana</b> yang dipakai untuk pembayarannya.</p>
 *
 * <h3>Posisi dalam rantai billing (terverifikasi dari kode)</h3>
 * <p>Urutan pemakaian nyata di modul sekolah adalah sebagai berikut &mdash; entity ini
 * berada di ujung paling hulu, dan setiap tingkat di bawahnya membacanya kembali lewat
 * <code>getPengaturanBiaya().getJenisBiayaSekolah()</code>:</p>
 * <ol>
 *   <li><b>JenisBiayaSekolah</b> (kelas ini) &mdash; kategori/aturan main pembayaran.</li>
 *   <li>{@link ais.database.model.sekolah.PengaturanBiaya} &mdash; satu paket aturan biaya
 *       (tahun ajaran, kelas, jurusan, gelombang, dsb.) yang mengacu ke satu jenis biaya.
 *       Sasaran pesertanya dapat dipersempit oleh
 *       {@link ais.database.model.sekolah.PengaturanBiayaPunyaSiswa} bila pengaturan itu
 *       ditandai <code>khususBuatSiswaTertentu</code>; kelas whitelist tersebut memutuskan
 *       apakah baris yang dicocokkan adalah siswa atau calon siswa persis dengan membaca
 *       properti {@link #getGunakanCalonSiswa()} milik kelas ini.</li>
 *   <li><code>PengaturanBiayaItemBiaya</code> &mdash; komponen biaya apa saja yang masuk
 *       ke dalam pengaturan tersebut, menunjuk ke katalog
 *       {@link ais.database.model.sekolah.ItemBiayaSekolah} (mis. "SPP", "Uang Gedung",
 *       "Seragam"). Katalog item biaya itulah yang menyimpan sifat per-komponen seperti
 *       jumlah angsuran atau keseragaman angsuran.</li>
 *   <li>{@link ais.database.model.sekolah.NominalBiaya} &mdash; materialisasi tarif menjadi
 *       kewajiban rupiah per siswa/periode.</li>
 *   <li>{@link ais.database.model.sekolah.Tagihan} &mdash; baris tagihan per periode,
 *       lalu <code>PembayaranSiswa</code>/<code>PembayaranSiswaDetail</code> saat dibayar.</li>
 * </ol>
 *
 * <h3>Bagaimana properti di sini benar-benar mengubah perilaku sistem</h3>
 * <ul>
 *   <li><b>{@link #getPeriode()}</b> memilih generator tagihan di
 *       <code>ais.action.master.sekolah.helper.TagihanUtil</code> dan
 *       <code>TagihanUtilCalonSiswa</code>: nilai <code>"Bulanan"</code> memanggil
 *       <code>doGenerateTagihanBulanan(...)</code> (deret tagihan per bulan sampai
 *       <code>PengaturanBiaya.getBulanSampai()</code>), nilai <code>"Tahunan"</code>
 *       memanggil <code>doGenerateTagihanTahunan(...)</code>, dan nilai lain apa pun
 *       (termasuk <code>"Harian"</code> serta default <code>"Insidentil"</code>) jatuh ke
 *       <code>doGenerateTagihanInsendentil(...)</code>. Jadi kolom teks ini adalah
 *       <em>saklar percabangan</em>, bukan keterangan.</li>
 *   <li><b>{@link #getGunakanCalonSiswa()}</b> menentukan populasi yang ditagih. Nilai
 *       <code>true</code> mengarahkan seluruh alur ke <code>CalonSiswa</code> (PPDB),
 *       <code>false</code> ke <code>Siswa</code> aktif. Bendera ini dibaca di banyak
 *       tempat sekaligus: <code>PembayaranOnline</code>, <code>PembayaranSiswaAction</code>,
 *       <code>PengaturanBiayaAction</code>, <code>CommonReportHelper</code>, servlet
 *       <code>/Api</code> (<code>TagihanSiswa</code>, <code>PsbCalonApi</code>), sampai
 *       ke pembentukan <b>kunci identitas tagihan</b> di
 *       {@link ais.database.model.sekolah.Tagihan} (prefiks <code>"c-&lt;id&gt;-"</code>
 *       untuk calon siswa versus <code>"s-&lt;id&gt;-"</code> untuk siswa).</li>
 *   <li><b>{@link #getGunakanLes()}</b>, <b>{@link #getGelombangTertentu()}</b>, dan
 *       <b>{@link #getPaketTertentu()}</b> mengubah bentuk form
 *       <code>PengaturanBiayaAction</code>: menyembunyikan/menampilkan pemilih kelas,
 *       tahun angkatan, status awal siswa, kelas les, gelombang PSB, dan paket PSB.
 *       Dua yang terakhir bahkan <em>menghapus</em> nilai FK terkait &mdash; lihat
 *       peringatan di bawah.</li>
 *   <li><b>{@link #getMulaiDitagihDiBulan()}</b> menimpa konfigurasi global instalasi
 *       <code>bulan_mulai_tagihan</code> (default 8 = Agustus, awal tahun ajaran) khusus
 *       untuk jenis biaya ini; lihat <code>TagihanUtil.getBulanMulai(JenisBiayaSekolah)</code>.</li>
 *   <li><b>{@link #getUntukBulan()}</b>/<b>{@link #getUntukTahun()}</b> mengunci satu
 *       periode tertentu untuk tagihan non-bulanan (dipakai lewat
 *       <code>PembayaranSiswa.convert(tahun, bulan)</code>); dikosongkan berarti
 *       "berlaku untuk semua bulan/tahun".</li>
 *   <li><b>{@link #getBolehAngsurBerapapun()}</b> melonggarkan validasi nominal setoran
 *       pada layar kasir dan pada REST <code>/Api TagihanSiswa</code>.</li>
 *   <li><b>{@link #getPilihanItemBiayaTerakumulasiBulanan()}</b> mengubah cara
 *       <code>PembayaranOnline</code> dan <code>/Api TagihanSiswa</code> menggabungkan
 *       item biaya menjadi satu baris bayar bulanan.</li>
 *   <li><b>{@link #getKanalPembayaran()}</b> menunjuk kanal bank/gateway yang dipakai;
 *       bila <code>null</code>, <code>SmartlinkChannelWindow</code> jatuh ke kanal default
 *       milik {@link ais.database.model.sekolah.Sekolah}.</li>
 * </ul>
 *
 * <h3>&#9888; PERINGATAN: mengubah konfigurasi ini pada instalasi berjalan bersifat merusak</h3>
 * <p>Karena baris ini adalah aturan main dan bukan metadata, mengubahnya di tengah tahun
 * ajaran punya efek yang tidak dapat dibatalkan. Yang terverifikasi dari kode:</p>
 * <ol>
 *   <li><b>Membalik <code>gunakanCalonSiswa</code> mengubah kunci identitas tagihan.</b>
 *       {@link ais.database.model.sekolah.Tagihan} menyusun kuncinya dengan prefiks
 *       <code>"c-"</code> atau <code>"s-"</code> berdasarkan bendera ini. Tagihan lama
 *       yang sudah terlanjur dibuat tidak ikut berubah, sehingga sinkronisasi berikutnya
 *       memandangnya sebagai baris asing dan dapat menghasilkan tagihan ganda/yatim.
 *       <code>JenisBiayaSekolahAction</code> memang membekukan kontrol ini lewat
 *       <code>Common.freezeGanti(yayasan, sekolah, gunakanCalonSiswa)</code> &mdash;
 *       <b>tetapi hanya bila sudah ada tagihan yang BENAR-BENAR DIBAYAR</b>
 *       (<code>Tagihan.pembayaranSiswaDetail is not null</code>). Selama seluruh tagihan
 *       masih berstatus belum dibayar, bendera ini tetap bebas dibalik walau ribuan baris
 *       tagihan sudah tergenerasi.</li>
 *   <li><b>Melepas centang <code>gelombangTertentu</code> atau <code>paketTertentu</code>
 *       menghapus FK di baris anak secara permanen.</b>
 *       {@link ais.database.model.sekolah.PengaturanBiaya}<code>.getGelombangPendaftaranPsb()</code>
 *       dan <code>getPaketPsb()</code> adalah getter destruktif: keduanya menyetel field
 *       menjadi <code>null</code> saat bendera di kelas ini <code>false</code>. Karena
 *       Hibernate memakai <em>property access</em> dan entity tersebut memakai
 *       <code>dynamicUpdate</code>, sekadar <b>membaca</b> baris pengaturan biaya setelah
 *       bendera dilepas sudah cukup untuk menulis <code>NULL</code> ke kolom
 *       <code>current_gelombang_pendaftaran_psb_id</code>/<code>paket_psb</code> pada
 *       flush berikutnya. Mencentang kembali benderanya <b>tidak</b> memulihkan nilai
 *       lama.</li>
 *   <li><b><code>periode</code> tidak dibekukan sama sekali.</b> Mengubah "Insidentil"
 *       menjadi "Bulanan" (atau sebaliknya) pada jenis biaya yang sudah dipakai akan
 *       memindahkan pembuatan tagihan ke generator yang sama sekali berbeda, sementara
 *       tagihan lama tetap ada dengan pola periode gaya lama.</li>
 *   <li><b>{@link #getBolehAngsurBerapapun()} sendiri adalah getter destruktif</b> &mdash;
 *       lihat catatan pada method tersebut.</li>
 * </ol>
 *
 * <h3>&#9888; Catatan keamanan / kontrol akses</h3>
 * <ul>
 *   <li><b>Hak akses layar pengelola: BENAR.</b>
 *       <code>ais.action.master.sekolah.JenisBiayaSekolahAction</code> memanggil
 *       <code>Common.doCheckSecurity()</code> pada <code>doBeforeCompose</code> dan
 *       menggerbangi tombol Tambah/Ubah/Hapus dengan
 *       <code>CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)</code>. Tombol unggah
 *       massal pun digerbangi ketat (<code>add.isVisible() &amp;&amp; edit &amp;&amp; delete</code>).
 *       Ini contoh POSITIF dibanding beberapa layar keuangan tetangganya.</li>
 *   <li><b>Pewarisan hak lewat menu induk &mdash; instance paling berbahaya sejauh ini.</b>
 *       <code>doAfterCompose</code> menyisipkan tab kedua
 *       <code>/pages/master/sekolah/kanal_pembayaran.zul</code> ke dalam halaman yang sama.
 *       <code>CommonPrivilages.checkPrevilages</code> memutuskan hak berdasarkan
 *       <code>Common.getCurrentMenu()</code> (menu halaman yang sedang dibuka), bukan
 *       berdasarkan ZUL yang disisipkan. Akibatnya siapa pun yang diberi hak CRUD atas
 *       menu "Jenis Biaya Sekolah" otomatis memperoleh hak CRUD atas
 *       {@link ais.database.model.sekolah.KanalPembayaran} &mdash; tabel yang menyimpan
 *       <b>kredensial gateway pembayaran hidup</b> (merchant id dan password BNI/BSI, API
 *       key dan token Flip/Finpay, serta API key, encryption key, dan HMAC key OnlineBMT,
 *       username/password eSmartlink). Ini menaikkan hak dari "boleh mengatur kategori
 *       biaya" menjadi "boleh membaca dan mengganti kredensial bank sekolah".</li>
 *   <li><b>Fail-open cakupan tenant pada layar daftar.</b> <code>initCriteria()</code> di
 *       Action tidak memasang penyaring dasar {@link ais.database.model.sekolah.Sekolah}
 *       maupun {@link ais.database.model.sekolah.Yayasan} milik pengguna; penyaringan
 *       sepenuhnya bergantung pada dua combobox pencarian yang berlabel "semua" saat
 *       konteks sekolah sesi kosong. Tombol Ubah/Hapus pada renderer hanya bergantung pada
 *       hak peran, bukan pada kepemilikan baris, sehingga baris milik sekolah/yayasan lain
 *       ikut dapat disunting.</li>
 * </ul>
 *
 * <h3>Catatan pemetaan Hibernate</h3>
 * <p>Kelas ini <code>extends</code> {@link ais.database.model.GeneralValueObject}, yang
 * <b>bukan</b> <code>@Entity</code> maupun <code>@MappedSuperclass</code> melainkan POJO
 * abstrak biasa. Hibernate karena itu <b>tidak</b> memetakan properti apa pun milik induk,
 * sehingga <code>id</code>, <code>oleh</code>, <code>olehId</code>, dan
 * <code>tanggal_dirubah</code> <b>wajib</b> dideklarasikan ulang di sini &mdash; itu
 * keharusan teknis, bukan duplikasi yang keliru.</p>
 * <p>Strategi penamaan kolom instalasi ini adalah
 * <code>ais.database.hibernate.MyNamingStrategy</code>, turunan
 * <code>DefaultNamingStrategy</code>, yang memakai <b>nama properti apa adanya</b> tanpa
 * konversi ke snake_case. Akibatnya kelas ini memiliki dua gaya kolom yang bercampur:
 * properti dengan <code>@Column</code> eksplisit memakai snake_case
 * (<code>untuk_bulan</code>, <code>untuk_tahun</code>), sedangkan properti tanpa
 * <code>@Column</code> (<code>gunakanCalonSiswa</code>, <code>gunakanLes</code>,
 * <code>gelombangTertentu</code>, <code>paketTertentu</code>,
 * <code>pilihanItemBiayaTerakumulasiBulanan</code>, <code>bolehAngsurBerapapun</code>,
 * <code>mulaiDitagihDiBulan</code>, <code>keterangan</code>) tetap camelCase di database.
 * Semuanya tetap terpetakan &mdash; tidak ada yang <code>@Transient</code>.</p>
 * <p>Entity ditandai <code>@Audited</code> (Envers), <code>dynamicInsert</code>, dan
 * <code>dynamicUpdate</code>. Kombinasi <em>property access</em> + <code>dynamicUpdate</code>
 * itulah yang membuat getter ber-efek-samping di kelas ini dan di kelas anaknya benar-benar
 * menulis ke database, bukan sekadar mengubah objek dalam memori.</p>
 *
 * <h3>Kuirk yang terverifikasi</h3>
 * <ul>
 *   <li><b>Nilai <code>"Tahunan"</code> tidak dapat dibuat lewat UI mana pun.</b> Combobox
 *       Periode di <code>JenisBiayaSekolahAction</code> hanya menawarkan
 *       <code>{"Bulanan", "Harian", "Insidentil"}</code>, dan tidak ada satu pun pemanggilan
 *       <code>setPeriode("...")</code> dengan literal di seluruh repo. Padahal cabang
 *       <code>"Tahunan"</code> diperiksa belasan kali di <code>TagihanUtil</code> dan
 *       <code>TagihanUtilCalonSiswa</code> (termasuk generator
 *       <code>doGenerateTagihanTahunan</code>). Seluruh cabang itu praktis kode mati
 *       kecuali barisnya dimasukkan lewat SQL mentah atau migrasi.</li>
 *   <li><b><code>"Harian"</code> tidak punya generator sendiri.</b> Nilai ini ditawarkan
 *       di UI dan diperiksa khusus di beberapa layar tampilan
 *       (<code>DetailTagihanSiswaHelper</code>, <code>DetailTagihanCalonSiswaHelper</code>),
 *       tetapi pada percabangan generator ia jatuh ke cabang <code>else</code> yang sama
 *       dengan "Insidentil".</li>
 *   <li><b>Kolom <code>tagihanPerSiswaBerbeda</code> sudah dinonaktifkan</b> (field, getter,
 *       dan setter dikomentari) namun <b>namanya masih terdaftar</b> di array
 *       <code>contents</code> yang dipakai <code>Common.cetakData(...)</code> dan
 *       <code>Common.uploadData(...)</code> pada Action &mdash; kolom hantu pada ekspor
 *       Excel dan pada pemetaan unggah massal.</li>
 *   <li><b>Panjang <code>kode</code> hanya 3 karakter</b> (dan textbox pada form pun
 *       dibatasi <code>setMaxlength(3)</code>), tanpa batasan unik di tingkat database
 *       maupun validasi duplikat di <code>onSave()</code>.</li>
 *   <li>Komentar generator asli <code>"JenisBiaya generated by hbm2java"</code> menyebut
 *       nama kelas <em>tanpa</em> akhiran "Sekolah" &mdash; jejak bahwa kelas ini
 *       diturunkan dari model perguruan tinggi lalu diganti namanya.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.PengaturanBiaya
 * @see ais.database.model.sekolah.PengaturanBiayaPunyaSiswa
 * @see ais.database.model.sekolah.ItemBiayaSekolah
 * @see ais.database.model.sekolah.NominalBiaya
 * @see ais.database.model.sekolah.Tagihan
 * @see ais.database.model.sekolah.KanalPembayaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "jenis_biaya_sekolah", schema = "sekolah")
public class JenisBiayaSekolah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Diwarisi dari {@link ais.database.model.GeneralValueObject}
	 * yang mengimplementasikan {@code Serializable}; nilainya dipatok agar sesi ZK yang
	 * dipasifkan/diaktifkan kembali tidak menolak instance lama.
	 */
	private static final long serialVersionUID = -5690688328230009605L;
	/**
	 * Kunci utama. Dideklarasikan ulang karena {@link ais.database.model.GeneralValueObject}
	 * bukan {@code @MappedSuperclass} sehingga field induk tidak dipetakan Hibernate.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang menyentuh baris ini; diisi oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}. Dideklarasikan ulang atas
	 * alasan pemetaan yang sama dengan {@link #id}.
	 */
	private String oleh;
	/**
	 * Identitas (id) pengguna terakhir yang menyentuh baris ini, pendamping {@link #oleh}.
	 * Dideklarasikan ulang atas alasan pemetaan yang sama dengan {@link #id}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah disentuh
	 *         melalui interceptor audit.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun
	 * string kosong/spasi &mdash; nilai lama dipertahankan. Ini disengaja agar jejak audit
	 * tidak terhapus oleh alur penyimpanan yang kebetulan tidak membawa konteks pengguna
	 * (mis. job latar atau pemanggilan servlet). Konsekuensinya, kolom ini
	 * <b>tidak dapat dikosongkan kembali</b> lewat setter.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}
	 * atau string kosong/spasi ditolak diam-diam sehingga jejak audit lama tetap utuh.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA sebelum {@code UPDATE}, sekaligus deklarasi field stempel waktu.
	 *
	 * <p>Dua hal berbeda sengaja ditulis pada satu baris fisik oleh generator/penyunting
	 * terdahulu &mdash; <b>jangan dipisah tanpa alasan</b> karena banyak berkas model lain
	 * memakai bentuk yang sama persis:</p>
	 * <ul>
	 *   <li><code>onUpdate()</code> &mdash; dipanggil otomatis oleh provider JPA tepat
	 *       sebelum pernyataan {@code UPDATE} dikirim, meneruskan instance ini ke
	 *       {@code AuditTimestampInterceptor.ubah(this)} yang mengisi {@link #oleh},
	 *       {@link #olehId}, dan stempel waktu dari konteks pengguna aktif.</li>
	 *   <li><code>tanggal_dirubah</code> &mdash; field stempel waktu, diinisialisasi ke
	 *       waktu server saat objek dibuat lewat {@code ais.ui.util.WaktuUtil.getDate()}
	 *       (bukan {@code new Date()}, agar mengikuti zona waktu/offset instalasi).</li>
	 * </ul>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil manual; nilainya diisi oleh
	 * {@code AuditTimestampInterceptor} melalui kait {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Tanpa {@code @Column}, sehingga nama kolom
	 * mengikuti nama properti apa adanya (<code>tanggal_dirubah</code>) sesuai
	 * {@code MyNamingStrategy}.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat
	 *         lewat konstruktor karena field diinisialisasi ke waktu server.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik konfigurasi jenis biaya ini (kolom tenant tingkat sekolah). */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik; selalu diturunkan ulang dari {@link #sekolah} pada
	 * {@link #getYayasan()}, jadi bukan nilai yang berdiri sendiri.
	 */
	private Yayasan yayasan;
	/** Penanda aktif/tidak; baris tidak aktif disembunyikan dari pencarian default. */
	private Boolean aktif;
	/** Kode singkat jenis biaya, maksimal 3 karakter, wajib diisi, tanpa jaminan unik. */
	private String kode;
	/** Nama jenis biaya yang tampil ke pengguna (mis. "SPP Bulanan"), wajib diisi. */
	private String nama;
	/**
	 * Siklus penagihan &mdash; <b>saklar percabangan generator tagihan</b>, bukan label.
	 * Nilai yang dapat dibuat lewat UI: "Bulanan", "Harian", "Insidentil". Lihat
	 * {@link #getPeriode()}.
	 */
	private String periode;
//	private Boolean tagihanPerSiswaBerbeda;
	/**
	 * Bila {@code true}, item-item biaya digabung menjadi satu baris pembayaran bulanan
	 * terakumulasi pada layar pembayaran online dan REST tagihan.
	 */
	private Boolean pilihanItemBiayaTerakumulasiBulanan;
	/**
	 * <b>Penentu populasi tagihan</b>: {@code true} berarti jenis biaya ini menagih
	 * {@code CalonSiswa} (PPDB), {@code false} berarti menagih {@code Siswa} aktif.
	 * Dibaca antara lain oleh {@link ais.database.model.sekolah.PengaturanBiayaPunyaSiswa}
	 * untuk memutuskan sisi mana dari whitelist peserta yang berlaku.
	 */
	private Boolean gunakanCalonSiswa;
	/** Bila {@code true}, pengaturan biaya diarahkan ke kelas les/kursus/privat. */
	private Boolean gunakanLes;
	/** Bila {@code true}, pengaturan biaya dibatasi pada satu gelombang pendaftaran PSB. */
	private Boolean gelombangTertentu;
	/** Bila {@code true}, pengaturan biaya dibatasi pada satu paket PSB. */
	private Boolean paketTertentu;
	/** Bulan spesifik yang dituju untuk tagihan non-bulanan; {@code null} = semua bulan. */
	private Integer untukBulan;
	/** Tahun spesifik yang dituju untuk tagihan non-bulanan; {@code null} = semua tahun. */
	private Integer untukTahun;
	/** Keterangan bebas untuk operator; tidak dipakai logika bisnis mana pun. */
	private String keterangan;
	/**
	 * Bulan awal penagihan untuk periode "Bulanan"; menimpa konfigurasi global
	 * <code>bulan_mulai_tagihan</code> ({@code null} = ikuti konfigurasi global).
	 */
	private Integer mulaiDitagihDiBulan;
	/** Bila {@code true}, kasir boleh menerima setoran dengan nominal bebas (angsuran parsial). */
	private Boolean bolehAngsurBerapapun;
	/**
	 * Kanal bank/gateway pembayaran khusus untuk jenis biaya ini; {@code null} berarti
	 * mengikuti kanal default milik {@link #sekolah}.
	 */
	private KanalPembayaran kanalPembayaran;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JavaBeans.
	 *
	 * <p>Seluruh properti dibiarkan {@code null}; nilai efektifnya baru muncul lewat
	 * getter yang melakukan <em>coalesce</em> (mis. {@link #getPeriode()} yang jatuh ke
	 * "Insidentil" dan {@link #getAktif()} yang jatuh ke {@code true}).</p>
	 */
	public JenisBiayaSekolah() {
	}

	/**
	 * Konstruktor ringkas untuk kolom-kolom yang wajib ada.
	 *
	 * <p>Dihasilkan oleh hbm2java. Tidak ada pemanggil di dalam repo saat ini; disediakan
	 * untuk pembuatan instance secara programatik/uji.</p>
	 *
	 * <p><b>Perhatian:</b> konstruktor ini menyetel {@link #id} secara langsung, padahal
	 * kolom id memakai {@code GenerationType.IDENTITY} dan ditandai
	 * {@code insertable = false}. Objek yang dibuat lewat sini karena itu diperlakukan
	 * sebagai entity yang <em>sudah ada</em>, bukan baris baru.</p>
	 *
	 * @param id   kunci utama baris yang sudah ada di database.
	 * @param kode kode singkat jenis biaya (maksimal 3 karakter).
	 * @param nama nama jenis biaya yang ditampilkan.
	 */
	public JenisBiayaSekolah(long id, String kode, String nama) {
		this.id = id;
		this.kode = kode;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dibuat oleh database ({@code IDENTITY}) dan ditandai
	 * {@code insertable = false} sehingga tidak pernah ikut pada pernyataan {@code INSERT}.
	 * Nilai {@code null} menandakan objek baru yang belum disimpan &mdash; dipakai
	 * {@code JenisBiayaSekolahAction} untuk membedakan judul dialog "Tambah" versus
	 * "Ubah", dan untuk memutuskan perlu tidaknya pembekuan kontrol.</p>
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
	 * Menyetel kunci utama baris ini.
	 *
	 * <p>Praktis hanya dipakai Hibernate saat memuat/menyimpan; kode aplikasi sebaiknya
	 * tidak memanggilnya.</p>
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik konfigurasi jenis biaya ini.
	 *
	 * <p><b>Efek samping (write-back):</b> getter memanggil
	 * {@code check(sekolah)} milik {@link ais.database.model.GeneralValueObject} dan
	 * <b>menugaskan ulang hasilnya ke field</b>. {@code check()} menyelesaikan proxy malas
	 * yang sudah terlepas dari session dan/atau mengembalikan instance kanonik dari
	 * {@code EntityIdentityMap}, sehingga referensi yang tersimpan bisa berganti objek
	 * setelah pemanggilan ini. Ini pola seragam di seluruh model AIS, bukan kekhususan
	 * kelas ini &mdash; namun berarti getter ini <b>tidak bebas efek samping</b>.</p>
	 *
	 * <p>Relasi bersifat {@code LAZY} dengan kaskade {@code PERSIST}/{@code MERGE};
	 * kolom FK-nya <code>sekolah_id</code>.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila baris bersifat lintas sekolah/belum
	 *         diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik konfigurasi jenis biaya ini.
	 *
	 * <p><b>Non-obvious:</b> objek {@link ais.database.model.sekolah.Sekolah} yang
	 * ber-{@code id} {@code null} diperlakukan sama dengan {@code null}. Ini penting untuk
	 * combobox ZK yang memakai baris pilihan semu ("== semua ==") bernilai objek kosong
	 * &mdash; tanpa normalisasi ini Hibernate akan mencoba menyimpan entity transien.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id disimpan sebagai
	 *                {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik konfigurasi jenis biaya ini.
	 *
	 * <p><b>Efek samping (write-back &amp; derivasi):</b> getter ini melakukan lebih
	 * banyak hal daripada sekadar membaca. Ia:</p>
	 * <ol>
	 *   <li>memanggil {@link #getSekolah()} (yang sendirinya melakukan write-back),</li>
	 *   <li><b>menimpa</b> field {@link #yayasan} dengan {@code sekolah.getYayasan()}
	 *       bila sekolah terisi &mdash; nilai yayasan yang tersimpan di kolom
	 *       <code>yayasan_id</code> diabaikan dan digantikan hasil turunan, lalu</li>
	 *   <li>menormalkan hasilnya lewat {@code check(...)}.</li>
	 * </ol>
	 * <p>Karena Hibernate memakai <em>property access</em> dan entity ini
	 * {@code dynamicUpdate}, sekadar membaca properti ini pada baris yang tidak konsisten
	 * (yayasan tersimpan berbeda dari yayasan sekolahnya) sudah cukup untuk <b>menulis
	 * koreksi permanen</b> ke database pada flush berikutnya. Sifat ini pada dasarnya
	 * bersifat memperbaiki-diri, tetapi tetap berarti kolom <code>yayasan_id</code> tidak
	 * dapat dipakai untuk menyimpan nilai yang berbeda dari yayasan sekolahnya.</p>
	 *
	 * @return yayasan pemilik (diturunkan dari sekolah bila ada), atau {@code null}.
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
	 * Menyetel yayasan pemilik konfigurasi jenis biaya ini.
	 *
	 * <p><b>Non-obvious:</b> objek ber-{@code id} {@code null} dinormalkan menjadi
	 * {@code null} (alasan sama dengan {@link #setSekolah(Sekolah)}). Perlu diingat pula
	 * bahwa nilai yang disetel di sini akan <b>ditimpa</b> oleh {@link #getYayasan()}
	 * apabila {@link #getSekolah()} tidak {@code null}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id disimpan sebagai
	 *                {@code null}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan status aktif jenis biaya ini.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} di database dibaca sebagai {@code true}
	 * (<em>coalesce</em>, tanpa write-back ke field). Karena filter pencarian default di
	 * {@code JenisBiayaSekolahAction.initCriteria()} juga memakai
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, baris berkolom
	 * {@code NULL} tetap muncul di daftar &mdash; kedua sisi konsisten di layar ini.</p>
	 *
	 * @return {@code true} bila jenis biaya aktif atau kolomnya belum pernah diisi;
	 *         {@code false} bila dinonaktifkan secara eksplisit.
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return this.aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif jenis biaya ini.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" pada baris grid daftar, yang langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan seketika tanpa
	 * membuka dialog. Checkbox tersebut dinonaktifkan bila pengguna tidak memegang hak
	 * {@code UPDATE}.</p>
	 *
	 * @param aktif status aktif; {@code null} akan dibaca kembali sebagai {@code true}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kode singkat jenis biaya.
	 *
	 * <p>Kolom wajib diisi dan <b>dibatasi 3 karakter</b> ({@code length = 3}); textbox
	 * pada form pun memakai {@code setMaxlength(3)}. Dipakai sebagai kunci pengurutan
	 * utama pada daftar. Tidak ada batasan unik di tingkat database maupun pemeriksaan
	 * duplikat pada {@code onSave()}, sehingga dua jenis biaya berkode sama dapat
	 * berdampingan.</p>
	 *
	 * @return kode jenis biaya, atau {@code null} untuk objek yang belum diisi.
	 */
	@Column(name = "kode", nullable = false, length = 3)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode singkat jenis biaya.
	 *
	 * @param kode kode jenis biaya; nilai lebih dari 3 karakter akan ditolak database.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis biaya yang ditampilkan ke pengguna.
	 *
	 * <p>Kolom wajib diisi. Dipakai sebagai label pada grid daftar, sebagai judul entri
	 * riwayat revisi ({@code RevisiHelper.createNewRevisi}), serta sebagai kunci pengurutan
	 * kedua setelah {@link #getKode()}.</p>
	 *
	 * @return nama jenis biaya, atau {@code null} untuk objek yang belum diisi.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama jenis biaya yang ditampilkan.
	 *
	 * @param nama nama jenis biaya; divalidasi tidak kosong oleh {@code onSave()} pada
	 *             layar pengelola.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan siklus penagihan &mdash; <b>saklar yang memilih generator tagihan</b>.
	 *
	 * <p>Nilai ini bukan sekadar keterangan. Pada
	 * {@code ais.action.master.sekolah.helper.TagihanUtil} dan
	 * {@code TagihanUtilCalonSiswa} nilainya menentukan mesin mana yang dijalankan:</p>
	 * <ul>
	 *   <li><code>"Bulanan"</code> &rarr; <code>doGenerateTagihanBulanan(...)</code>,
	 *       membangkitkan deret tagihan per bulan mulai
	 *       {@link #getMulaiDitagihDiBulan()} sampai <code>PengaturanBiaya.getBulanSampai()</code>;</li>
	 *   <li><code>"Tahunan"</code> &rarr; <code>doGenerateTagihanTahunan(...)</code>;</li>
	 *   <li>nilai lain apa pun &rarr; <code>doGenerateTagihanInsendentil(...)</code>.</li>
	 * </ul>
	 * <p>Nilai ini juga mengubah tampilan form pengelola (untuk "Bulanan", isian
	 * bulan/tahun dan "boleh angsur berapapun" disembunyikan sementara "ditagih mulai
	 * bulan" ditampilkan) dan dipakai belasan kali di layar rincian tagihan, rekap
	 * pembayaran, serta pembayaran online.</p>
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} di-<em>coalesce</em> menjadi
	 * <code>"Insidentil"</code> (tanpa write-back), sehingga baris lama yang kolomnya
	 * kosong otomatis diperlakukan sebagai tagihan insidentil.</p>
	 *
	 * <p><b>Kuirk:</b> combobox pada layar pengelola hanya menawarkan
	 * <code>{"Bulanan", "Harian", "Insidentil"}</code> dan tidak ada
	 * {@code setPeriode("Tahunan")} di mana pun, sehingga seluruh cabang
	 * <code>"Tahunan"</code> praktis kode mati. Sebaliknya <code>"Harian"</code> dapat
	 * dipilih namun tidak punya generator sendiri &mdash; ia jatuh ke cabang insidentil,
	 * dan hanya diperlakukan khusus pada beberapa layar tampilan rincian tagihan.</p>
	 *
	 * @return nilai periode, atau <code>"Insidentil"</code> bila kolom {@code null}.
	 */
	@Column(name = "periode", length = 10)
	public String getPeriode() {
		return this.periode == null ? "Insidentil" : periode;
	}

	/**
	 * Menyetel siklus penagihan.
	 *
	 * <p><b>Perhatian:</b> tidak ada mekanisme pembekuan untuk properti ini pada layar
	 * pengelola (berbeda dari {@link #setGunakanCalonSiswa(Boolean)} yang dibekukan bila
	 * sudah ada pembayaran). Mengubah nilainya pada jenis biaya yang sudah dipakai akan
	 * memindahkan pembuatan tagihan berikutnya ke generator yang berbeda, sementara
	 * tagihan lama tetap tersimpan dengan pola periode gaya lama.</p>
	 *
	 * @param periode nilai periode; nilai yang dapat dipilih dari UI hanya "Bulanan",
	 *                "Harian", dan "Insidentil". Kolom dibatasi 10 karakter.
	 */
	public void setPeriode(String periode) {
		this.periode = periode;
	}

//	@Column(name = "tagihan_per_siswa_berbeda")
//	public Boolean getTagihanPerSiswaBerbeda() {
//		return this.tagihanPerSiswaBerbeda == null ? true : tagihanPerSiswaBerbeda;
//	}
//
//	public void setTagihanPerSiswaBerbeda(Boolean tagihanPerSiswaBerbeda) {
//		this.tagihanPerSiswaBerbeda = tagihanPerSiswaBerbeda;
//	}

	/**
	 * Mengembalikan bulan spesifik yang dituju oleh tagihan non-bulanan.
	 *
	 * <p>Dipasangkan dengan {@link #getUntukTahun()} dan diubah menjadi kunci periode
	 * lewat <code>PembayaranSiswa.convert(tahun, bulan)</code> pada
	 * {@code TagihanUtil} serta pada penetapan bulan/tahun
	 * {@link ais.database.model.sekolah.Tagihan} di <code>PembayaranOnline</code>.
	 * Isian ini hanya ditampilkan pada form bila periode <b>bukan</b> "Bulanan".</p>
	 *
	 * @return nomor bulan (1&ndash;12), atau {@code null} yang berarti "berlaku untuk
	 *         semua bulan" (ditampilkan sebagai "Semua" pada grid).
	 */
	@Column(name = "untuk_bulan")
	public Integer getUntukBulan() {
		return this.untukBulan;
	}

	/**
	 * Menyetel bulan spesifik yang dituju oleh tagihan non-bulanan.
	 *
	 * @param untukBulan nomor bulan; {@code null} berarti berlaku untuk semua bulan.
	 */
	public void setUntukBulan(Integer untukBulan) {
		this.untukBulan = untukBulan;
	}

	/**
	 * Mengembalikan tahun spesifik yang dituju oleh tagihan non-bulanan.
	 *
	 * <p>Pendamping {@link #getUntukBulan()}; lihat penjelasan di sana. Nilai ini juga
	 * dipakai <code>PembayaranSiswaAction</code> untuk memilih otomatis combobox tahun
	 * pada layar kasir.</p>
	 *
	 * @return tahun (format 4 digit), atau {@code null} yang berarti "berlaku untuk semua
	 *         tahun" (ditampilkan sebagai "Semua" pada grid).
	 */
	@Column(name = "untuk_tahun")
	public Integer getUntukTahun() {
		return this.untukTahun;
	}

	/**
	 * Menyetel tahun spesifik yang dituju oleh tagihan non-bulanan.
	 *
	 * @param untukTahun tahun; {@code null} berarti berlaku untuk semua tahun.
	 */
	public void setUntukTahun(Integer untukTahun) {
		this.untukTahun = untukTahun;
	}

	/**
	 * Mengembalikan penentu populasi yang ditagih: calon siswa atau siswa aktif.
	 *
	 * <p><b>Ini properti paling berpengaruh di kelas ini.</b> Nilai {@code true} berarti
	 * seluruh alur biaya diarahkan ke {@code CalonSiswa} (jalur PPDB); {@code false}
	 * berarti ke {@code Siswa} aktif. Titik-titik yang membacanya, terverifikasi dari
	 * kode:</p>
	 * <ul>
	 *   <li>{@link ais.database.model.sekolah.PengaturanBiayaPunyaSiswa} &mdash; memutuskan
	 *       apakah whitelist peserta pengaturan biaya dicocokkan terhadap siswa atau calon
	 *       siswa;</li>
	 *   <li>{@link ais.database.model.sekolah.Tagihan} &mdash; menyusun <b>kunci identitas
	 *       tagihan</b> dengan prefiks <code>"c-&lt;idCalonSiswa&gt;-"</code> atau
	 *       <code>"s-&lt;idSiswa&gt;-"</code>, dan menyaring kecocokan gelombang PSB;</li>
	 *   <li>{@link ais.database.model.sekolah.PengaturanBiaya}<code>.kirimTemplate(...)</code>
	 *       &mdash; memilih daftar penerima notifikasi tagihan;</li>
	 *   <li><code>PembayaranOnline</code>, <code>PembayaranSiswaAction</code>,
	 *       <code>PengaturanBiayaAction</code>, <code>TampilanPengumumanAkademisAction</code>,
	 *       <code>CommonReportHelper</code>, <code>NewUiPemOnlineController</code>, serta
	 *       servlet <code>/Api</code> (<code>TagihanSiswa</code>, <code>PsbCalonApi</code>).</li>
	 * </ul>
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} di-<em>coalesce</em> menjadi {@code false}
	 * (tanpa write-back), sehingga baris lama tanpa nilai dianggap menagih siswa aktif.
	 * Perlu dicatat bahwa sebagian pemanggil memakai
	 * {@code Boolean.TRUE.equals(...)}/{@code Boolean.FALSE.equals(...)} terhadap hasil
	 * getter ini &mdash; aman karena getter tidak pernah mengembalikan {@code null}.</p>
	 *
	 * <p><b>Tanpa {@code @Column}</b>, sehingga nama kolomnya mengikuti nama properti apa
	 * adanya (<code>gunakanCalonSiswa</code>, camelCase) sesuai {@code MyNamingStrategy}.</p>
	 *
	 * @return {@code true} bila jenis biaya ini menagih calon siswa; {@code false} bila
	 *         menagih siswa aktif (termasuk saat kolom {@code null}).
	 */
	public Boolean getGunakanCalonSiswa() {
		return gunakanCalonSiswa == null ? false : gunakanCalonSiswa;
	}

	/**
	 * Menyetel penentu populasi yang ditagih.
	 *
	 * <p><b>&#9888; Mengubah nilai ini pada instalasi berjalan berpotensi merusak data.</b>
	 * Kunci identitas tagihan di {@link ais.database.model.sekolah.Tagihan} ikut berubah
	 * (prefiks <code>"c-"</code> versus <code>"s-"</code>), sehingga tagihan yang sudah
	 * terlanjur dibuat menjadi tidak dikenali oleh sinkronisasi berikutnya dan dapat
	 * menghasilkan tagihan ganda atau yatim.</p>
	 * <p>{@code JenisBiayaSekolahAction} memasang penjagaan parsial: pada dialog Ubah,
	 * kontrol yayasan, sekolah, dan checkbox ini <b>dibekukan</b>
	 * ({@code Common.freezeGanti}) bila terdapat minimal satu
	 * {@link ais.database.model.sekolah.Tagihan} yang sudah punya
	 * {@code pembayaranSiswaDetail}. Penjagaan itu <b>tidak</b> berlaku selama seluruh
	 * tagihan masih berstatus belum dibayar, walau jumlahnya sudah ribuan baris.</p>
	 *
	 * @param gunakanCalonSiswa {@code true} untuk menagih calon siswa, {@code false} untuk
	 *                          siswa aktif.
	 */
	public void setGunakanCalonSiswa(Boolean gunakanCalonSiswa) {
		this.gunakanCalonSiswa = gunakanCalonSiswa;
	}

	/**
	 * Mengembalikan bulan awal penagihan untuk periode "Bulanan".
	 *
	 * <p>Dibaca oleh {@code TagihanUtil.getBulanMulai(JenisBiayaSekolah)}, yang mula-mula
	 * mengambil konfigurasi global instalasi <code>bulan_mulai_tagihan</code> (default
	 * {@code 8}, yakni Agustus sebagai awal tahun ajaran) lalu <b>menimpanya</b> dengan
	 * nilai properti ini bila tidak {@code null}. Juga dipakai langsung oleh
	 * <code>PengaturanBiayaAction</code> dan <code>DetailTagihanCalonSiswaHelper</code>.</p>
	 *
	 * <p>Isian ini hanya ditampilkan pada form bila periode bernilai "Bulanan"; pada grid
	 * daftar nilai {@code null} ditampilkan sebagai "Default".</p>
	 *
	 * <p><b>Tanpa {@code @Column}</b> &mdash; nama kolom mengikuti nama properti apa adanya
	 * (<code>mulaiDitagihDiBulan</code>).</p>
	 *
	 * @return nomor bulan awal penagihan, atau {@code null} untuk mengikuti konfigurasi
	 *         global.
	 */
	public Integer getMulaiDitagihDiBulan() {
		return mulaiDitagihDiBulan;
	}

	/**
	 * Menyetel bulan awal penagihan untuk periode "Bulanan".
	 *
	 * @param mulaiDitagihDiBulan nomor bulan; {@code null} berarti memakai konfigurasi
	 *                            global <code>bulan_mulai_tagihan</code>.
	 */
	public void setMulaiDitagihDiBulan(Integer mulaiDitagihDiBulan) {
		this.mulaiDitagihDiBulan = mulaiDitagihDiBulan;
	}

	/**
	 * Mengembalikan keterangan bebas untuk operator.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} di-<em>coalesce</em> menjadi string kosong
	 * (tanpa write-back). Ini penting karena grid daftar merangkai hasilnya langsung
	 * dengan teks lain ({@code getKeterangan() + "(jml pemb. ...)"}) &mdash; tanpa
	 * coalesce, layar akan menampilkan kata "null".</p>
	 *
	 * <p>Berbeda dari beberapa entity master lain di paket ini, properti {@code keterangan}
	 * <b>dideklarasikan di kelas ini sendiri</b> (bukan diwarisi dari
	 * {@link ais.database.model.GeneralValueObject}), sehingga benar-benar dipetakan dan
	 * tersimpan. Tanpa {@code @Column}, nama kolomnya <code>keterangan</code> apa adanya.</p>
	 *
	 * <p>Tidak ada logika bisnis yang membaca isi properti ini &mdash; murni catatan untuk
	 * manusia.</p>
	 *
	 * @return keterangan, atau string kosong bila belum diisi. Tidak pernah {@code null}.
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel keterangan bebas untuk operator.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan izin menerima setoran dengan nominal bebas (angsuran parsial).
	 *
	 * <p>Bila {@code true}, layar kasir (<code>PembayaranSiswaAction</code>), layar rincian
	 * tagihan siswa maupun calon siswa, rekap pembayaran, pembayaran online, dan REST
	 * <code>/Api TagihanSiswa</code> melonggarkan validasi nominal sehingga siswa boleh
	 * membayar sebagian dari tagihan.</p>
	 *
	 * <p><b>&#9888; Getter destruktif (write-back).</b> Sebelum mengembalikan nilai, method
	 * ini <b>menulis {@code false} ke field</b> apabila {@link #getPeriode()} bernilai
	 * "Bulanan" (perbandingan tanpa memperhatikan besar-kecil huruf). Karena Hibernate
	 * memakai <em>property access</em> dan entity ini {@code dynamicUpdate}, sekadar
	 * <b>membaca</b> properti ini pada baris yang sedang dikelola session sudah cukup untuk
	 * menyimpan {@code false} secara permanen pada flush berikutnya. Konsekuensinya:
	 * mengubah periode sebuah jenis biaya menjadi "Bulanan" lalu mengembalikannya ke
	 * "Insidentil" <b>tidak memulihkan</b> pengaturan "boleh angsur berapapun" yang semula
	 * bernilai {@code true} &mdash; nilai aslinya sudah hilang. Ini varian dari pola getter
	 * destruktif yang sama dengan {@code ItemBiayaSekolah.getKelamin()} dan
	 * {@code NominalBiaya.getNominal()}.</p>
	 * <p>Secara logika bisnis pemaksaan tersebut memang masuk akal (tagihan bulanan
	 * dipecah per bulan sehingga angsuran bebas tidak relevan), dan form pengelola pun
	 * menyembunyikan checkbox-nya saat periode "Bulanan". Yang bermasalah adalah
	 * <em>caranya</em>: seharusnya dinormalkan saat menyimpan, bukan saat membaca.</p>
	 *
	 * <p>Nilai {@code null} di-<em>coalesce</em> menjadi {@code false}.</p>
	 *
	 * @return {@code true} bila setoran nominal bebas diizinkan; selalu {@code false} bila
	 *         periode "Bulanan".
	 */
	public Boolean getBolehAngsurBerapapun() {

		if (getPeriode().equalsIgnoreCase("Bulanan")) {
			bolehAngsurBerapapun = false;
		}

		return bolehAngsurBerapapun == null ? false : bolehAngsurBerapapun;
	}

	/**
	 * Menyetel izin menerima setoran dengan nominal bebas.
	 *
	 * <p>Perlu diingat bahwa nilai {@code true} yang disetel di sini akan dibatalkan oleh
	 * {@link #getBolehAngsurBerapapun()} selama {@link #getPeriode()} bernilai "Bulanan".</p>
	 *
	 * @param bolehAngsurBerapapun {@code true} untuk mengizinkan angsuran bebas.
	 */
	public void setBolehAngsurBerapapun(Boolean bolehAngsurBerapapun) {
		this.bolehAngsurBerapapun = bolehAngsurBerapapun;
	}

	/**
	 * Mengembalikan penanda bahwa jenis biaya ini diperuntukkan bagi kelas les/kursus/privat.
	 *
	 * <p>Dibaca <code>PengaturanBiayaAction</code> untuk mengganti bentuk form pengaturan
	 * biaya: bila {@code true}, pemilih kelas siswa, tahun angkatan, dan status awal siswa
	 * disembunyikan sementara pemilih {@code KelasLesSiswa} ditampilkan (dan sebaliknya).
	 * Juga dipakai pada alur pembangkitan tagihan les di Action yang sama.</p>
	 *
	 * <p>Nilai {@code null} di-<em>coalesce</em> menjadi {@code false} (tanpa write-back).
	 * Tanpa {@code @Column} &mdash; nama kolom <code>gunakanLes</code> apa adanya.</p>
	 *
	 * @return {@code true} bila jenis biaya ini untuk kelas les/kursus/privat.
	 */
	public Boolean getGunakanLes() {
		return gunakanLes == null ? false : gunakanLes;
	}

	/**
	 * Menyetel penanda peruntukan kelas les/kursus/privat.
	 *
	 * @param gunakanLes {@code true} bila jenis biaya ini untuk kelas les.
	 */
	public void setGunakanLes(Boolean gunakanLes) {
		this.gunakanLes = gunakanLes;
	}

	/**
	 * Mengembalikan penanda bahwa jenis biaya ini dibatasi pada satu gelombang pendaftaran PSB.
	 *
	 * <p>Bila {@code true}, <code>PengaturanBiayaAction</code> menampilkan pemilih gelombang
	 * pendaftaran pada form pengaturan biaya.</p>
	 *
	 * <p><b>&#9888; Bendera ini mengendalikan getter destruktif di kelas anak.</b>
	 * {@link ais.database.model.sekolah.PengaturanBiaya}<code>.getGelombangPendaftaranPsb()</code>
	 * <b>menyetel field-nya menjadi {@code null}</b> setiap kali bendera ini bernilai
	 * {@code false}. Karena entity tersebut memakai property access dan
	 * {@code dynamicUpdate}, melepas centang di sini lalu sekadar membuka layar pengaturan
	 * biaya sudah cukup untuk <b>menghapus permanen</b> kolom
	 * <code>current_gelombang_pendaftaran_psb_id</code> pada seluruh baris pengaturan biaya
	 * yang memakai jenis biaya ini. Mencentang kembali tidak memulihkannya.</p>
	 *
	 * <p>Nilai {@code null} di-<em>coalesce</em> menjadi {@code false}. Tanpa
	 * {@code @Column} &mdash; nama kolom <code>gelombangTertentu</code> apa adanya.</p>
	 *
	 * @return {@code true} bila jenis biaya ini khusus untuk gelombang tertentu.
	 */
	public Boolean getGelombangTertentu() {
		return gelombangTertentu == null ? false : gelombangTertentu;
	}

	/**
	 * Menyetel penanda pembatasan pada satu gelombang pendaftaran PSB.
	 *
	 * <p>Lihat peringatan penghapusan data pada {@link #getGelombangTertentu()} sebelum
	 * melepas centang ini pada instalasi berjalan.</p>
	 *
	 * @param gelombangTertentu {@code true} bila jenis biaya ini khusus gelombang tertentu.
	 */
	public void setGelombangTertentu(Boolean gelombangTertentu) {
		this.gelombangTertentu = gelombangTertentu;
	}

	/**
	 * Mengembalikan penanda bahwa jenis biaya ini dibatasi pada satu paket PSB.
	 *
	 * <p>Bila {@code true}, <code>PengaturanBiayaAction</code> menampilkan pemilih paket
	 * pada form pengaturan biaya.</p>
	 *
	 * <p><b>&#9888; Sama seperti {@link #getGelombangTertentu()}, bendera ini mengendalikan
	 * getter destruktif di kelas anak.</b>
	 * {@link ais.database.model.sekolah.PengaturanBiaya}<code>.getPaketPsb()</code>
	 * menyetel field-nya menjadi {@code null} setiap kali bendera ini {@code false},
	 * sehingga melepas centang di sini menghapus permanen kolom <code>paket_psb</code> pada
	 * baris-baris pengaturan biaya terkait.</p>
	 *
	 * <p>Nilai {@code null} di-<em>coalesce</em> menjadi {@code false}. Tanpa
	 * {@code @Column} &mdash; nama kolom <code>paketTertentu</code> apa adanya.</p>
	 *
	 * @return {@code true} bila jenis biaya ini khusus untuk paket tertentu.
	 */
	public Boolean getPaketTertentu() {
		return paketTertentu == null ? false : paketTertentu;
	}

	/**
	 * Menyetel penanda pembatasan pada satu paket PSB.
	 *
	 * <p>Lihat peringatan penghapusan data pada {@link #getPaketTertentu()} sebelum melepas
	 * centang ini pada instalasi berjalan.</p>
	 *
	 * @param paketTertentu {@code true} bila jenis biaya ini khusus paket tertentu.
	 */
	public void setPaketTertentu(Boolean paketTertentu) {
		this.paketTertentu = paketTertentu;
	}

	/**
	 * Mengembalikan penanda bahwa pilihan item biaya diakumulasikan menjadi satu
	 * pembayaran bulanan.
	 *
	 * <p>Dibaca oleh <code>ais.action.master.sekolah.helper.PembayaranOnline</code> (enam
	 * titik) dan servlet REST <code>ais.action.servlet.api.TagihanSiswa</code> (tiga titik)
	 * untuk memutuskan apakah beberapa {@link ais.database.model.sekolah.ItemBiayaSekolah}
	 * dalam satu bulan digabung menjadi satu baris bayar terakumulasi, atau ditampilkan
	 * dan dibayar terpisah per item.</p>
	 *
	 * <p>Nilai {@code null} di-<em>coalesce</em> menjadi {@code false} (tanpa write-back).
	 * Tanpa {@code @Column} &mdash; nama kolom
	 * <code>pilihanItemBiayaTerakumulasiBulanan</code> apa adanya.</p>
	 *
	 * @return {@code true} bila item biaya diakumulasikan per bulan pada layar pembayaran.
	 */
	public Boolean getPilihanItemBiayaTerakumulasiBulanan() {
		return pilihanItemBiayaTerakumulasiBulanan == null ? false : pilihanItemBiayaTerakumulasiBulanan;
	}

	/**
	 * Menyetel penanda akumulasi pilihan item biaya menjadi satu pembayaran bulanan.
	 *
	 * @param pilihanItemBiayaTerakumulasiBulanan {@code true} untuk mengakumulasikan item
	 *                                            biaya per bulan.
	 */
	public void setPilihanItemBiayaTerakumulasiBulanan(Boolean pilihanItemBiayaTerakumulasiBulanan) {
		this.pilihanItemBiayaTerakumulasiBulanan = pilihanItemBiayaTerakumulasiBulanan;
	}

	/**
	 * Mengembalikan kanal bank/gateway pembayaran khusus untuk jenis biaya ini.
	 *
	 * <p>Dibaca <code>ais.action.master.helper.util.SmartlinkChannelWindow</code> saat
	 * membangun daftar kanal pembayaran untuk sebuah
	 * {@link ais.database.model.sekolah.Tagihan}: bila jenis biaya punya kanal sendiri,
	 * kanal itulah yang dipakai; bila {@code null}, alur jatuh ke kanal default milik
	 * {@link ais.database.model.sekolah.Sekolah}. Pada form pengelola, isi combobox kanal
	 * dibatasi pada kanal aktif milik sekolah terpilih ditambah kanal global
	 * ({@code sekolah is null}), dengan opsi "Ikuti Kanal Pembayaran Default" untuk nilai
	 * {@code null}.</p>
	 *
	 * <p><b>Efek samping (write-back):</b> memanggil {@code check(kanalPembayaran)} dan
	 * menugaskan ulang hasilnya ke field &mdash; lihat penjelasan pada
	 * {@link #getSekolah()}.</p>
	 *
	 * <p><b>&#9888; Catatan keamanan:</b> {@link ais.database.model.sekolah.KanalPembayaran}
	 * menyimpan kredensial gateway pembayaran hidup (merchant id/password BNI dan BSI, API
	 * key dan token Flip/Finpay, API key, encryption key, dan HMAC key OnlineBMT, serta
	 * username/password eSmartlink). Layar pengelolaannya disisipkan sebagai tab kedua di
	 * halaman jenis biaya ini, sehingga hak akses atas menu "Jenis Biaya Sekolah" ikut
	 * membuka hak CRUD atas kredensial tersebut &mdash; lihat catatan pewarisan hak pada
	 * dokumentasi kelas.</p>
	 *
	 * <p>Relasi {@code LAZY} dengan kaskade {@code PERSIST}/{@code MERGE}; kolom FK-nya
	 * <code>kanal_pembayaran</code>.</p>
	 *
	 * @return kanal pembayaran khusus, atau {@code null} untuk mengikuti kanal default
	 *         sekolah.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kanal_pembayaran")
	public KanalPembayaran getKanalPembayaran() {
		kanalPembayaran = check(kanalPembayaran);
		return kanalPembayaran;
	}

	/**
	 * Menyetel kanal bank/gateway pembayaran khusus untuk jenis biaya ini.
	 *
	 * <p><b>Non-obvious:</b> berbeda dari {@link #setSekolah(Sekolah)} dan
	 * {@link #setYayasan(Yayasan)}, setter ini <b>tidak</b> menormalkan objek ber-{@code id}
	 * {@code null} menjadi {@code null}. Pemanggil (mis. {@code onSave()} pada layar
	 * pengelola) karena itu harus memastikan sendiri bahwa baris pilihan semu "Ikuti Kanal
	 * Pembayaran Default" diteruskan sebagai {@code null}.</p>
	 *
	 * @param kanalPembayaran kanal pembayaran; {@code null} berarti mengikuti kanal default
	 *                        sekolah.
	 */
	public void setKanalPembayaran(KanalPembayaran kanalPembayaran) {
		this.kanalPembayaran = kanalPembayaran;
	}

}
