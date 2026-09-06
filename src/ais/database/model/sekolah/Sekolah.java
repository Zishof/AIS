package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.Pegawai;
import ais.database.model.Pendaftar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.VoKunci;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DisposisiSop;

/**
 * <h2>Entity <b>Sekolah</b> — unit tenant utama seluruh sistem AIS</h2>
 *
 * <p>Kelas ini memetakan tabel <code>sekolah.sekolah</code> dan merupakan <b>entity paling
 * sentral</b> dalam model data AIS. Hampir seluruh data operasional (siswa, guru, kelas, jadwal,
 * nilai, tagihan, pembayaran, absensi, surat, laporan) bermuara pada satu baris di tabel ini:
 * pada saat dokumentasi ini ditulis, <b>116 entity di bawah {@code ais/database/model/**}
 * mendeklarasikan relasi {@code private Sekolah sekolah;}</b> secara langsung, dan lebih banyak
 * lagi yang menjangkaunya secara tidak langsung lewat {@code Siswa}, {@code Guru},
 * {@code KelasSiswa}, dan seterusnya. Dengan kata lain: <b>baris Sekolah adalah batas tenant</b>
 * — nilai <code>id</code>-nya yang memisahkan data satu sekolah dari sekolah lain di dalam satu
 * instalasi (satu database, satu aplikasi) yang dipakai bersama oleh banyak sekolah.</p>
 *
 * <h3>Posisi dalam hierarki tenant</h3>
 *
 * <p>Hierarki tenant AIS bertingkat: <b>Pendaftar</b> (calon pelanggan/instansi yang mendaftar ke
 * layanan) &rarr; <b>Yayasan</b> (badan penyelenggara, lihat
 * {@link ais.database.model.sekolah.Yayasan}) &rarr; <b>Sekolah</b> (satuan pendidikan) &rarr;
 * data operasional. Entity ini menyimpan referensi ke <i>kedua</i> tingkat di atasnya:
 * {@link #getYayasan()} dan {@link #getPendaftar()}. Untuk jalur perguruan tinggi ada cabang
 * paralel {@link #getPerguruanTinggi()}, dan untuk instalasi yang memakai modul RAB/kepegawaian
 * ada {@link #getSatuanKerja()}.</p>
 *
 * <p><b>Sifat relasi ke Yayasan — TERVERIFIKASI dari sisi entity ini:</b> kolom FK
 * <code>yayasan_id</code> dideklarasikan sebagai
 * <code>&#64;JoinColumn(name = "yayasan_id")</code> <b>tanpa</b> <code>nullable = false</code>,
 * sehingga <b>relasi ini OPSIONAL</b> — sebuah Sekolah sah berdiri tanpa Yayasan. Hal itu
 * diperkuat oleh {@link #setYayasan(Yayasan)} yang secara eksplisit <b>menormalkan objek
 * ber-<code>id</code> null menjadi <code>null</code> asli</b>. Konsekuensinya penting dan sering
 * disalahpahami: pada level entity, <code>sekolah.getYayasan() == null</code> adalah <b>penjaga
 * yang benar-benar bisa menyala</b>. Ini <b>berbeda</b> dengan
 * {@code SekolahUtil.getYayasan()} (lihat bagian berikut) yang tidak pernah mengembalikan
 * <code>null</code>. Kode yang menyamakan keduanya akan salah.</p>
 *
 * <p>Sebaliknya, relasi ke {@link JenisSekolah} bersifat <b>wajib</b>
 * (<code>&#64;JoinColumn(name = "jenis_sekolah_id", nullable = false)</code>). {@code JenisSekolah}
 * membawa nama jenis beserta {@code Jenjang} — inilah sumber "jenjang" (SD/SMP/SMA/sederajat)
 * bagi sekolah, <b>bukan</b> kolom tersendiri di entity ini.</p>
 *
 * <h3>Kolom penentu domain instalasi (resolusi tenant berbasis nama host)</h3>
 *
 * <p>Entity ini memiliki kolom {@link #getDomain() domain} yang dipetakan ke
 * <code>domain_sekolah</code> dengan <b><code>unique = true</code></b>. Kolom inilah yang
 * menentukan pada nama host mana sebuah sekolah "muncul". Alurnya:
 * {@code SekolahAction.reInitByDomain()} membaca seluruh Sekolah dengan
 * <code>domain IS NOT NULL AND domain &lt;&gt; '' AND aktif = true</code>, memecah nilai kolom
 * dengan {@code Common.pisahDomain()} (dipisah koma — <b>satu Sekolah boleh punya banyak
 * domain</b>, sehingga keunikan di level kolom tidak berarti keunikan di level domain), lalu
 * mengisi peta statis {@code SekolahAction.sekolahByDomain}. Peta itu kemudian dipakai
 * {@code SekolahUtil.getSekolahData(request)} untuk menebak sekolah aktif dari
 * {@code request.getServerName()}.</p>
 *
 * <p><b>PERINGATAN KEAMANAN (terkait audit {@code task_beeb2833}).</b> Pencocokan di
 * {@code SekolahUtil.getSekolahData} <b>tidak</b> membandingkan nama host secara utuh, melainkan
 * <code>serverName.startsWith(domain)</code> lalu <code>serverName.contains(domain)</code>
 * (keduanya <i>lowercase</i>). Pola <b>substring</b> ini berarti sekolah yang mendaftarkan domain
 * pendek/umum akan "menangkap" permintaan untuk host lain yang kebetulan memuat potongan itu.
 * Temuan yang sama sebelumnya dicatat dari sisi {@link ais.database.model.sekolah.Yayasan}
 * ({@code YayasanAction.yayasanByDomain}); dokumentasi ini <b>mengonfirmasi bahwa mekanisme yang
 * identik juga berlaku untuk Sekolah</b> — jadi cakupannya dua kali lebih luas dari yang
 * tercatat semula. Sumber datanya adalah kolom {@link #getDomain() domain} di kelas ini, yang
 * dapat disunting lewat layar master Sekolah.</p>
 *
 * <h3>Pola "fail-open cakupan tenant" — ringkasan untuk pembaca yang tiba di sini lebih dulu</h3>
 *
 * <p>Bila Anda membuka file ini sebagai titik masuk untuk memahami multi-tenancy AIS, inilah
 * pola berulang yang <b>wajib</b> diketahui; pola ini ditemukan puluhan kali sepanjang audit
 * dokumentasi dan hampir selalu berbentuk sama:</p>
 *
 * <ol>
 *   <li><b>Resolver tenant tidak pernah mengembalikan <code>null</code>.</b>
 *       {@code SekolahUtil.getSekolah()} dan {@code SekolahUtil.getSekolahData()} berakhir dengan
 *       <code>return new Sekolah();</code> — sebuah objek <b>kosong ber-<code>id</code>
 *       null</b>, bukan <code>null</code>. Hal yang persis sama berlaku untuk
 *       {@code SekolahUtil.getYayasan()} yang berakhir <code>return new Yayasan();</code>.
 *       Akibatnya <b>penjaga bergaya <code>if (SekolahUtil.getSekolah() == null)</code> tidak
 *       pernah menyala</b>. Verifikasi angka: di seluruh pohon sumber hanya ada 2 pemakaian
 *       penjaga <code>== null</code> semacam itu berbanding 190 pemanggilan
 *       {@code SekolahUtil.getSekolah()} — jadi mayoritas pemanggil memang (dengan benar)
 *       memeriksa <code>getId() == null</code>, tetapi yang beberapa itu tetap mati.</li>
 *   <li><b>Filter Criteria berubah menjadi "izinkan semua" saat tenant tak teridentifikasi.</b>
 *       Bentuk kanoniknya terlihat di {@code SekolahAction.initCriteria()} pada layar master
 *       Sekolah sendiri:
 *       <pre>
 * current == null || current.getId() == null
 *     ? Restrictions.sqlRestriction("true")
 *     : Restrictions.eq("id", current.getId())</pre>
 *       Ketika sekolah aktif tidak dapat diresolusi (mis. user yang perannya tidak terikat ke
 *       sekolah dan nama host tidak cocok domain mana pun), pembatas berubah menjadi
 *       <code>true</code> dan <b>seluruh sekolah di instalasi ikut terdaftar</b>. Varian lain
 *       memakai <code>sqlRestriction("1=1")</code> atau menghilangkan kondisi sama sekali. Pada
 *       layar ini konsekuensinya masih relatif dapat dipertanggungjawabkan (super admin memang
 *       perlu melihat semua), tetapi <b>pola tulisannya persis sama</b> dengan lusinan layar lain
 *       yang seharusnya terkurung per tenant.</li>
 *   <li><b>Kontras "fail-closed" ada di file yang sama.</b> Masih di {@code SekolahAction},
 *       pengisian combobox Kanal Pembayaran memakai
 *       <code>sekolah.getId() == null ? Restrictions.sqlRestriction("false") : …</code> —
 *       <b><code>false</code></b>, bukan <code>true</code>. Jadi penulis kode <i>tahu</i> bentuk
 *       amannya; pemilihan <code>true</code> di tempat lain adalah keputusan per lokasi, bukan
 *       keterbatasan API. Ini argumen kuat bahwa perbaikan menyeluruh layak dilakukan.</li>
 *   <li><b>Objek tenant kosong tetap "aktif".</b> Lihat {@link #getAktif()}: nilai
 *       <code>null</code> dinormalkan menjadi <code>true</code>. Sebuah <code>new Sekolah()</code>
 *       hasil fallback resolver karenanya menjawab "ya, saya aktif" pada setiap pengecekan
 *       status — satu lapis lagi yang membuat kegagalan resolusi tenant tidak terlihat.</li>
 * </ol>
 *
 * <p><b>Hasil pemeriksaan khusus atas kelas ini:</b> {@code Sekolah} sendiri <b>tidak</b> memuat
 * method resolusi tenant apa pun (tidak ada padanan {@code getYayasan()} statis, tidak ada
 * pembaca sesi/HTTP). Satu-satunya method statis di sini —
 * {@link #checkCidDanPassword(Siswa, CalonSiswa)} dan
 * {@link #checkCidDanPasswordBsi(Siswa, CalonSiswa)} — meresolusi <i>kredensial</i>, bukan
 * tenant, meskipun keduanya memakai pola fallback yang secara moral sejenis: bila kredensial
 * milik sekolah kosong, <b>kredensial global instalasi dipakai</b>. Efeknya: transaksi milik
 * sekolah yang belum dikonfigurasi tetap berjalan, tetapi mendarat di merchant instalasi, bukan
 * gagal dengan jelas.</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getDikunci()}. Kelas juga
 *       ber-anotasi {@code @Audited} (Hibernate Envers), sehingga setiap perubahan tersimpan di
 *       tabel revisi.</li>
 *   <li><b>Identitas resmi satuan pendidikan</b> — {@link #getNama()}, {@link #getNss()},
 *       {@link #getNpsn()}, {@link #getJenisSekolah()} (pembawa jenjang),
 *       {@link #getNamaKepalaSekolah()}/{@link #getNipKepalaSekolah()},
 *       {@link #getNamaWakilKepalaSekolah()}/{@link #getNipWakilKepalaSekolah()}.</li>
 *   <li><b>Alamat &amp; kontak</b> — {@link #getAlamat()}, {@link #getRt()}, {@link #getRw()},
 *       {@link #getDusun()}, {@link #getKelurahan()}, {@link #getKecamatan()},
 *       {@link #getKabupatenKota()}, {@link #getPropinsi()}, {@link #getKodePos()},
 *       {@link #getTelp()}, {@link #getFax()}, {@link #getWa()}, {@link #getEmail()}.</li>
 *   <li><b>Tampilan &amp; portal publik</b> — {@link #getDomain()}, {@link #getCss()},
 *       {@link #getPiilhanTampilan()} beserta konstanta {@link #TAMPILAN_DEFAULT},
 *       {@link #TAMPILAN_KLASIK}, {@link #TAMPILAN_BARU}; {@link #getMotto()},
 *       {@link #getDeskripsi()}, {@link #getWebsite()}, {@link #getHeaderppdb()},
 *       {@link #getTanyaWhatsapp()}, {@link #getJawabWhatsappPsb()}.</li>
 *   <li><b>Kebijakan akademik/PSB</b> — {@link #getPenjurusanSekolahs()},
 *       {@link #getPenjurusanBolehDipilihSaatPsb()}, {@link #getPenjurusanWajibDipilih()},
 *       {@link #getSiswaDiizinkanDiPortalYayasan()},
 *       {@link #getGuruHarusPakaiSatuanKerja()}.</li>
 *   <li><b>Pejabat penanda tangan</b> — lima pasang
 *       {@code labelPejabatN}/{@code pegawaiN} ({@link #getLabelPejabat1()} …
 *       {@link #getPegawai5()}) yang dipakai blok tanda tangan pada laporan/surat.</li>
 *   <li><b>Integrasi pembayaran (per sekolah)</b> — BNI ({@link #getBniMerchantId()},
 *       {@link #getBniPassword()}, {@link #getBniGatewayUrl()}), BSI
 *       ({@link #getBsiMerchantId()}, {@link #getBsiUsername()}, {@link #getBsiScretId()},
 *       {@link #getBsiPassword()}, {@link #getBsiGatewayUrl()}), Flip, Finpay, Esmartlink,
 *       Online BMT, BJB Syariah, serta {@link #getKanalPembayaran()}.</li>
 *   <li><b>Berkas cetak</b> — {@link #putFile(Map)}, satu-satunya method "mesin" nyata di kelas
 *       ini: menyuntikkan jalur file kop surat atas/bawah, stempel, dan logo ke dalam peta
 *       parameter JasperReports.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang harus diketahui sebelum menyunting</h3>
 *
 * <ul>
 *   <li><b>Getter yang MENULIS (write-back), sebagian bahkan destruktif.</b> Tujuh getter —
 *       {@link #getAlamat()}, {@link #getEmail()}, {@link #getNama()}, {@link #getNss()},
 *       {@link #getNpsn()}, {@link #getTelp()}, {@link #getMotto()}, dan {@link #getDomain()} —
 *       <b>menugaskan kembali ke field</b> bila nilainya kosong, menyalin dari
 *       {@link #getPendaftar()}. {@link #getEmail()} lebih jauh lagi: ia <b>mengubah nilai yang
 *       sudah ada</b> dengan memangkas tanda petik di awal. Karena kelas ini memakai
 *       <i>property access</i> (anotasi berada di getter) dan <code>dynamicUpdate = true</code>,
 *       <b>proses dirty-checking Hibernate memanggil getter-getter ini</b> — artinya sekadar
 *       <i>membaca</i> objek Sekolah di dalam sebuah transaksi dapat menghasilkan
 *       <code>UPDATE</code> senyap ke database. Untuk {@link #getDomain()} efeknya paling serius:
 *       nilai yang tersalin dari Pendaftar dapat menabrak <code>unique</code> constraint kolom
 *       <code>domain_sekolah</code>, atau justru mengubah pemetaan tenant berbasis host.</li>
 *   <li><b><code>getNss()</code> dan <code>getNpsn()</code> memakai sumber cadangan yang
 *       SAMA</b> ({@code pendaftar.getKode()}), padahal NSS dan NPSN adalah dua identitas resmi
 *       yang berbeda. Pada sekolah yang dibuat dari Pendaftar dan tidak pernah disunting, kedua
 *       kolom akan berisi nilai identik.</li>
 *   <li><b>Setter yang menolak menghapus nilai.</b> {@link #setOleh(String)},
 *       {@link #setOlehId(String)}, dan {@link #setDisposisiSop(DisposisiSop)}
 *       <i>return</i> lebih awal bila argumennya null/kosong — nilai lama dipertahankan. Jadi
 *       field-field itu <b>tidak dapat dikosongkan lewat setter</b>.</li>
 *   <li><b>Kolom tanpa anotasi.</b> Banyak properti (mis. {@code npsn}, {@code kelurahan},
 *       {@code rt}, seluruh blok Flip/Finpay) tidak memakai {@code @Column} sama sekali dan
 *       mengandalkan penamaan bawaan Hibernate. Mengganti nama method-nya berarti mengganti nama
 *       kolom — perubahan yang memutus skema.</li>
 *   <li><b>Field dideklarasikan ulang di kelas ini adalah KEHARUSAN, bukan duplikasi.</b>
 *       {@link ais.database.model.GeneralValueObject} (leluhur lewat
 *       {@link ais.database.model.VoKunci} &rarr; {@code DataSop}) <b>bukan</b> {@code @Entity}
 *       maupun {@code @MappedSuperclass}; Hibernate tidak memetakan propertinya. Karena itu
 *       {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, {@code aktif}, dan
 *       {@code disposisiSop} harus dinyatakan lagi di sini agar terpetakan.</li>
 *   <li><b>Resolusi proxy lazy.</b> Seluruh getter relasi memanggil {@code check(...)} milik
 *       {@link ais.database.model.GeneralValueObject} dan <b>menugaskan hasilnya kembali ke
 *       field</b>. Mekanisme itu (cache &rarr; inisialisasi proxy &rarr; reload lewat session
 *       baru) didokumentasikan lengkap di kelas dasar; jangan menghapus penugasan baliknya.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Layar master Sekolah
 *       (<code>/pages/master/sekolah/sekolah.zul</code>) memang terdaftar sebagai menu sendiri,
 *       tetapi {@code PendaftarAction} <b>menyisipkan layar yang sama sebagai iframe</b> di dalam
 *       tab "Sekolah" pada layar Pendaftar. Karena {@code CommonPrivilages.checkPrevilages(...)}
 *       menilai hak berdasarkan menu yang sedang aktif, seluruh CRUD atas entity tenant ini —
 *       termasuk penyuntingan {@link #getDomain() domain} dan kredensial payment gateway —
 *       <b>diwarisi dari hak atas menu "Pendaftar"</b>. Sebagai efek samping tambahan, membuka
 *       tab itu akan <b>membuat dan menyimpan baris {@link Yayasan} baru</b> bila belum ada
 *       (operasi tulis dipicu oleh tindakan baca).</li>
 *   <li><b>Rahasia dalam kolom biasa.</b> Kredensial BNI/BSI/Flip/Finpay/Esmartlink/Online BMT
 *       disimpan sebagai kolom teks apa adanya (tanpa enkripsi), ikut terekam ke tabel revisi
 *       Envers karena {@code @Audited}, dan sebagian getter-nya dipanggil dari kode yang mencetak
 *       ke {@code System.out} (lihat {@link #checkCidDanPassword(Siswa, CalonSiswa)}).</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.VoKunci
 * @see ais.database.model.sekolah.Yayasan
 * @see ais.database.model.sekolah.JenisSekolah
 * @see ais.database.model.Pendaftar
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "sekolah", schema = "sekolah")
public class Sekolah extends VoKunci {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8010394750682252089L;
	/** Kunci utama tabel <code>sekolah.sekolah</code>; sekaligus <b>pengenal tenant</b>. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini; dapat {@code null} pada baris lama
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> argumen {@code null} atau kosong <b>diabaikan diam-diam</b> (method
	 * langsung {@code return}), sehingga nilai lama dipertahankan. Field ini karenanya tidak dapat
	 * dikosongkan lewat setter — perilaku sengaja agar jejak audit tidak terhapus oleh proses
	 * penyimpanan yang tidak membawa konteks pengguna.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: argumen {@code null}/kosong diabaikan sehingga
	 * nilai lama tidak pernah terhapus.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini; dapat {@code null} pada baris lama
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi stempel waktu dan identitas pengubah tepat sebelum
	 * Hibernate menerbitkan {@code UPDATE}.
	 *
	 * <p>Pekerjaan sesungguhnya didelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Karena dipanggil oleh
	 * penyedia persistence, method ini <b>tidak boleh</b> dipanggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()}), lalu disegarkan oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini (presisi TIMESTAMP)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Jenis satuan pendidikan; <b>pembawa jenjang</b> (SD/SMP/SMA/…). Relasi WAJIB. */
	private JenisSekolah jenisSekolah;
	/** Badan penyelenggara. Relasi <b>OPSIONAL</b> — lihat javadoc kelas. */
	private Yayasan yayasan;
	private String alamat;
	private String email;
	private String motto;
	private String fax;
	private String nama;
	private String namaKepalaSekolah;
	private String nipKepalaSekolah;
	private String namaWakilKepalaSekolah;
	private String nipWakilKepalaSekolah;
	/** Nomor Statistik Sekolah. */
	private String nss;
	/** Nomor Pokok Sekolah Nasional. */
	private String npsn;
	private String wa;
	private String telp;
	private Boolean aktif;
	private Boolean penjurusanBolehDipilihSaatPsb;
	private Boolean penjurusanWajibDipilih;
	private SatuanKerja satuanKerja;
	private Boolean guruHarusPakaiSatuanKerja;
	/**
	 * Nama host tempat sekolah ini dilayani; boleh berisi <b>beberapa</b> domain dipisah koma.
	 * Menentukan resolusi tenant berbasis host — lihat peringatan keamanan di javadoc kelas.
	 */
	private String domain;
	private String css;
	private String deskripsi;
	/** Nama tema tampilan; salah satu dari konstanta {@code TAMPILAN_*} (ejaan asli dipertahankan). */
	private String piilhanTampilan;

	/** Nilai {@link #getPiilhanTampilan()} untuk tema bawaan. */
	public static final String TAMPILAN_DEFAULT = "default";
	/** Nilai {@link #getPiilhanTampilan()} untuk tema lama/klasik. */
	public static final String TAMPILAN_KLASIK = "klasik";
	/** Nilai {@link #getPiilhanTampilan()} untuk tema "New UI". */
	public static final String TAMPILAN_BARU = "baru";
	private DisposisiSop disposisiSop;
	private String rt;
	private String rw;
	private String dusun;
	private String kodePos;
	private String kelurahan;
	private String kecamatan;
	private String kabupatenKota;
	private String propinsi;
	/** Konfigurasi situs publik dalam bentuk JSON (kolom teks), bukan sekadar URL. */
	private String website;

	private String bniMerchantId;
	private String bniPassword;
	private String bniGatewayUrl;

	private String bsiMerchantId;
	private String bsiScretId;
	private String bsiUsername;
	private String bsiPassword;
	private String bsiGatewayUrl;

	/** Penjurusan yang ditawarkan sekolah ini; {@code TreeSet} sehingga urutan/keanggotaan ditentukan {@code compareTo}. */
	private Set<PenjurusanSekolah> penjurusanSekolahs = new TreeSet<PenjurusanSekolah>();
	private String labelPejabat1;
	private String labelPejabat2;
	private String labelPejabat3;
	private String labelPejabat4;
	private String labelPejabat5;
	private Pegawai pegawai1;
	private Pegawai pegawai2;
	private Pegawai pegawai3;
	private Pegawai pegawai4;
	private Pegawai pegawai5;

	private String tanyaWhatsapp;
	private String jawabWhatsappPsb;

	/** Cabang perguruan tinggi dari hierarki tenant (dipakai instalasi kampus). */
	private PerguruanTinggi perguruanTinggi;

	private Boolean aktfkanPembayaranViaFlip;
	private String apiKeyFlip;
	private String tokenFlip;
	private Double biayaAdminFlip;

	private Boolean aktfkanPembayaranViaFinpay;
	private String apiKeyFinpay;
	private String tokenFinpay;
	private Double biayaAdminFinpay;
	private Double biayaAdminBjbSyariah;

	private Boolean aktfkanPembayaranViaEsmartlink;
	/** Sakelar tenant Online BMT; null pada instalasi lama berarti OFF. */
	private Boolean aktfkanPembayaranViaOnlineBmt;
	private String onlineBmtPrefixInvoice;
	private Double onlineBmtBiayaAdministrasi;
	private String onlineBmtKodeMitra;
	private String onlineBmtNamaMitra;
	private String onlineBmtKodeMerchant;
	private String onlineBmtNamaMerchant;
	private String onlineBmtApiKey;
	private String onlineBmtEncryptionKey;
	private String onlineBmtHmacKey;
	private Integer onlineBmtRequestTimeTolerance;
	private String usernameEsmartlink;
	private String passwordEsmartlink;
	private Double biayaAdminEsmartlink;
	private String variableBiayaAdminEsmartlink;

	/** Pendaftar (calon pelanggan) asal sekolah ini; menjadi sumber salin sejumlah getter. */
	private Pendaftar pendaftar;
	private String headerppdb;

	private Boolean siswaDiizinkanDiPortalYayasan;
	private KanalPembayaran kanalPembayaran;
	private Tbmuser dikunci;

	private Boolean aktfkanBjbSyariah;

	/**
	 * Daftar penjurusan (IPA/IPS/Bahasa/kejuruan) yang ditawarkan sekolah ini.
	 *
	 * <p>Dipetakan sebagai {@code @ManyToMany} lewat tabel penghubung
	 * <code>sekolah.sekolah_punya_penjurusan</code> (kolom <code>sekolah</code> dan
	 * <code>penjurusan</code>). Cascade hanya {@code MERGE} — menyimpan Sekolah <b>tidak</b>
	 * membuat baris {@link PenjurusanSekolah} baru.</p>
	 *
	 * <p><b>Kasus tepi:</b> koleksinya {@code TreeSet}, sehingga keanggotaan ditentukan
	 * {@code compareTo} milik {@link ais.database.model.GeneralValueObject} (berbasis nama, bukan
	 * id). Dua penjurusan bernama sama akan saling menggeser walau id-nya berbeda.</p>
	 *
	 * @return himpunan penjurusan yang ditawarkan; tidak pernah {@code null} (default set kosong)
	 */
	@ManyToMany(targetEntity = PenjurusanSekolah.class, cascade = { CascadeType.MERGE })
	@JoinTable(name = "sekolah_punya_penjurusan", joinColumns = @JoinColumn(name = "sekolah"), inverseJoinColumns = @JoinColumn(name = "penjurusan"), schema = "sekolah")
	public Set<PenjurusanSekolah> getPenjurusanSekolahs() {
		return penjurusanSekolahs;
	}

	/**
	 * @param penjurusanSekolahs himpunan penjurusan pengganti; disarankan tetap {@code TreeSet}
	 *                           agar perilaku urutan/keanggotaan tidak berubah
	 */
	public void setPenjurusanSekolahs(Set<PenjurusanSekolah> penjurusanSekolahs) {
		this.penjurusanSekolahs = penjurusanSekolahs;
	}

	/**
	 * @return representasi ringkas <code>"&lt;id&gt;-&lt;nama&gt;"</code>. Membaca field
	 *         <b>langsung</b> (bukan lewat {@link #getNama()}), sehingga tidak memicu salin dari
	 *         Pendaftar dan dapat menampilkan {@code null} pada objek yang belum lengkap —
	 *         termasuk objek kosong hasil fallback resolver tenant, yang akan tercetak sebagai
	 *         <code>"null-null"</code>
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate. */
	public Sekolah() {
	}

	/**
	 * Konstruktor pintasan untuk membentuk referensi ringan ke sebuah sekolah.
	 *
	 * @param id kunci utama sekolah yang dirujuk
	 */
	public Sekolah(Long id) {
		this.id = id;
	}

	/**
	 * @return kunci utama sekaligus <b>pengenal tenant</b>; {@code null} pada objek yang belum
	 *         disimpan <i>atau</i> pada objek kosong hasil fallback
	 *         {@code SekolahUtil.getSekolah()} — pemeriksaan <code>getId() == null</code> adalah
	 *         cara yang benar untuk mendeteksi "tenant tidak teridentifikasi"
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id kunci utama; umumnya hanya diisi Hibernate atau
	 *           {@link #Sekolah(Long) konstruktor referensi}
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Pengguna yang sedang "mengunci" baris ini (mekanisme kunci pengeditan warisan
	 * {@link ais.database.model.VoKunci}).
	 *
	 * <p>Memanggil {@code check(...)} untuk meresolusi proxy lazy dan menugaskan hasilnya kembali
	 * ke field — jangan hilangkan penugasan itu.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris tidak terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * @param dikunci pengguna pengunci; {@code null} untuk melepas kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Disposisi SOP yang melekat pada baris ini (kontrak warisan {@code DataSop}).
	 *
	 * @return disposisi SOP, atau {@code null} bila tidak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyimpan disposisi SOP.
	 *
	 * <p><b>Kasus tepi &amp; kuirk:</b> argumen {@code null} atau yang ber-<code>id</code>
	 * {@code null} ditolak lewat {@code return} awal, sehingga <b>disposisi tidak dapat
	 * dilepas</b> lewat setter. Akibat penjagaan awal itu, ternari di baris berikutnya
	 * <b>tidak pernah</b> memilih cabang kirinya (kondisinya selalu <code>false</code> pada titik
	 * tersebut) — praktis selalu menugaskan {@code disposisiSop}. Ternari itu adalah sisa pola
	 * salin-tempel dan dibiarkan apa adanya; jangan "membetulkan" tanpa menelusuri pemanggilnya.</p>
	 *
	 * @param disposisiSop disposisi SOP baru; diabaikan bila {@code null}/belum tersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Jenis satuan pendidikan — <b>sumber jenjang</b> sekolah ini (jenjang tersimpan di
	 * {@code JenisSekolah.jenjang}, bukan sebagai kolom di entity ini).
	 *
	 * <p>Relasi <b>WAJIB</b>: {@code @JoinColumn(name = "jenis_sekolah_id", nullable = false)}.
	 * Menyimpan Sekolah tanpa jenis akan ditolak database.</p>
	 *
	 * @return jenis sekolah; secara skema tidak boleh {@code null} pada baris tersimpan, namun
	 *         dapat {@code null} pada objek yang belum lengkap (termasuk hasil fallback resolver)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_sekolah_id", nullable = false)
	public JenisSekolah getJenisSekolah() {
		jenisSekolah = check(jenisSekolah);
		return this.jenisSekolah;
	}

	/**
	 * @param jenisSekolah jenis satuan pendidikan; tidak dinormalisasi — nilai {@code null}
	 *                     diterima di memori dan baru ditolak saat penyimpanan
	 */
	public void setJenisSekolah(JenisSekolah jenisSekolah) {
		this.jenisSekolah = jenisSekolah;
	}

	/**
	 * Badan penyelenggara (yayasan) pemilik sekolah ini.
	 *
	 * <p><b>Relasi OPSIONAL</b> — {@code @JoinColumn(name = "yayasan_id")} tanpa
	 * {@code nullable = false}. Berbeda dengan {@code SekolahUtil.getYayasan()} yang tidak pernah
	 * mengembalikan {@code null} (ia mengembalikan {@code new Yayasan()} ber-<code>id</code>
	 * null), getter <b>ini</b> benar-benar dapat mengembalikan {@code null}, dan
	 * {@link #setYayasan(Yayasan)} memastikan hal itu dengan menormalkan objek ber-id null.
	 * Karena itu penjaga <code>sekolah.getYayasan() == null</code> pada level entity
	 * <b>bermakna</b>, sementara penjaga bernama mirip terhadap hasil {@code SekolahUtil}
	 * <b>tidak pernah menyala</b>. Bedakan keduanya dengan cermat saat menambal kode cakupan
	 * tenant.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila sekolah berdiri sendiri
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menetapkan yayasan pemilik.
	 *
	 * <p><b>Normalisasi penting:</b> objek yayasan yang ber-<code>id</code> {@code null} —
	 * misalnya hasil fallback {@code SekolahUtil.getYayasan()} pada permintaan tanpa konteks
	 * tenant, atau baris kosong dari combobox — <b>disimpan sebagai {@code null} asli</b>. Inilah
	 * satu-satunya titik di jalur ini yang mengubah "objek kosong" kembali menjadi {@code null},
	 * dan itu yang membuat penjaga {@code == null} pada relasi entity dapat dipercaya.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek ber-id null berarti "tanpa yayasan"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Alamat jalan sekolah (baris alamat utama; RT/RW/dusun/kelurahan disimpan terpisah).
	 *
	 * <p><b>Getter write-back:</b> bila {@link #getPendaftar()} terisi dan alamat masih kosong,
	 * nilai <b>disalin dari Pendaftar dan ditugaskan ke field</b>. Karena kelas ini memakai
	 * property access, dirty-checking Hibernate memanggil getter ini, sehingga sekadar membaca
	 * objek di dalam transaksi dapat menerbitkan {@code UPDATE} senyap.</p>
	 *
	 * <p>Nilai kembalian selalu sudah di-{@code trim} dan tidak pernah {@code null} (string kosong
	 * bila belum diisi), meskipun kolomnya {@code nullable = false} di database.</p>
	 *
	 * @return alamat sekolah, string kosong bila belum ada
	 */
	@Column(name = "alamat", nullable = false)
	public String getAlamat() {
		if (getPendaftar() != null && (alamat == null || alamat.trim().isEmpty())) {
			alamat = pendaftar.getAlamat();
		}
		return this.alamat == null ? "" : this.alamat.trim();
	}

	/**
	 * @param alamat alamat sekolah; disimpan apa adanya (tanpa trim)
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Alamat surel resmi sekolah.
	 *
	 * <p><b>Getter write-back sekaligus DESTRUKTIF.</b> Dua efek samping terjadi di sini:</p>
	 * <ol>
	 *   <li>bila kosong dan {@link #getPendaftar()} terisi, nilai disalin dari Pendaftar;</li>
	 *   <li>tanda petik tunggal di awal nilai <b>dipangkas berulang</b> (menangani hasil ekspor
	 *       Excel bergaya <code>'email&#64;domain</code>, termasuk petik ganda), dan hasil
	 *       pangkasan itu <b>ditulis kembali ke field</b>.</li>
	 * </ol>
	 * <p>Efek nomor 2 berarti getter ini <b>mengubah data yang sudah ada</b>, bukan hanya mengisi
	 * yang kosong; dikombinasikan dengan dirty-checking, nilai di database ikut berubah pada flush
	 * berikutnya tanpa ada aksi simpan dari pengguna.</p>
	 *
	 * @return surel sekolah yang sudah dibersihkan; string kosong bila belum ada
	 */
	@Column(name = "email")
	public String getEmail() {
		if (getPendaftar() != null && (email == null || email.trim().isEmpty())) {
			email = pendaftar.getEmail();
		}
		// --- LOGIKA BARU: Menghilangkan tanda petik (') di depan ---
		if (email != null) {
			email = email.trim();
			// Menggunakan while agar jika ada double petik (contoh: ''email@web.com)
			// langsung terhapus semua
			while (email.startsWith("'")) {
				email = email.substring(1).trim();
			}
		}
		return this.email == null ? "" : email.trim();
	}

	/**
	 * @param email surel sekolah; disimpan apa adanya — pembersihan tanda petik terjadi di getter
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Nomor faksimile sekolah.
	 *
	 * <p>Nilai <b>dinormalkan saat dibaca</b>: seluruh karakter selain angka dan titik dibuang
	 * (<code>[^\d.]</code>). Jadi <code>"(022) 123-456"</code> dibaca sebagai
	 * <code>"022123456"</code>. Normalisasi ini <b>tidak</b> ditulis kembali ke field, sehingga
	 * nilai di database tetap seperti yang diketik pengguna.</p>
	 *
	 * @return nomor faks berisi angka/titik saja; string kosong bila belum diisi
	 */
	@Column(name = "fax")
	public String getFax() {
		return this.fax == null ? "" : fax.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * @param fax nomor faks; disimpan apa adanya
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}

	/**
	 * Nama resmi sekolah — dipakai di judul portal, kop laporan, dan kunci parameter Jasper pada
	 * {@link #putFile(Map)}.
	 *
	 * <p><b>Getter write-back:</b> bila kosong dan {@link #getPendaftar()} terisi, nama disalin
	 * dari Pendaftar dan ditugaskan ke field (lihat catatan dirty-checking di javadoc kelas).</p>
	 *
	 * @return nama sekolah sudah di-{@code trim}; string kosong bila belum ada
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		if (getPendaftar() != null && (nama == null || nama.trim().isEmpty())) {
			nama = pendaftar.getNama();
		}
		return this.nama == null ? "" : nama.trim();
	}

	/**
	 * @param nama nama resmi sekolah
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return nama kepala sekolah untuk blok tanda tangan laporan/surat; dapat {@code null}
	 */
	@Column(name = "nama_kepala_sekolah")
	public String getNamaKepalaSekolah() {
		return this.namaKepalaSekolah;
	}

	/**
	 * @param namaKepalaSekolah nama kepala sekolah
	 */
	public void setNamaKepalaSekolah(String namaKepalaSekolah) {
		this.namaKepalaSekolah = namaKepalaSekolah;
	}

	/**
	 * Nomor Statistik Sekolah (NSS).
	 *
	 * <p><b>Getter write-back:</b> bila kosong, disalin dari {@code pendaftar.getKode()}.
	 * <b>Kuirk yang perlu diwaspadai:</b> {@link #getNpsn()} memakai <b>sumber cadangan yang
	 * sama persis</b>, sehingga pada sekolah hasil konversi Pendaftar, NSS dan NPSN — dua
	 * identitas resmi yang berbeda — akan berisi nilai identik sampai ada yang menyuntingnya.</p>
	 *
	 * @return NSS; dapat {@code null} bila belum diisi dan tidak ada Pendaftar
	 */
	@Column(name = "nss", nullable = false)
	public String getNss() {
		if (getPendaftar() != null && (nss == null || nss.trim().isEmpty())) {
			nss = pendaftar.getKode();
		}
		return this.nss;
	}

	/**
	 * @param nss Nomor Statistik Sekolah
	 */
	public void setNss(String nss) {
		this.nss = nss;
	}

	/**
	 * Nomor telepon sekolah.
	 *
	 * <p>Menggabungkan dua perilaku: <b>write-back</b> dari {@link #getPendaftar()} bila kosong,
	 * dan <b>normalisasi baca</b> yang membuang karakter selain angka/titik (normalisasi ini
	 * tidak ditulis balik).</p>
	 *
	 * @return nomor telepon berisi angka/titik saja; string kosong bila belum ada
	 */
	@Column(name = "telp")
	public String getTelp() {
		if (getPendaftar() != null && (telp == null || telp.trim().isEmpty())) {
			telp = pendaftar.getTelp();
		}
		return this.telp == null ? "" : telp.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * @param telp nomor telepon; disimpan apa adanya
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Status aktif sekolah.
	 *
	 * <p><b>Penting untuk keamanan/cakupan tenant:</b> nilai {@code null} dinormalkan menjadi
	 * <b>{@code true}</b>. Artinya (a) sekolah lama yang kolomnya belum terisi dianggap aktif, dan
	 * (b) <b>objek kosong hasil fallback</b> {@code SekolahUtil.getSekolah()} — yang seluruh
	 * fieldnya {@code null} — juga menjawab "aktif". Ini menambah satu lapis lagi yang membuat
	 * kegagalan resolusi tenant tidak terlihat oleh pemanggil (lihat ringkasan pola fail-open di
	 * javadoc kelas).</p>
	 *
	 * <p>Blok yang dikomentari di dalam method menandakan pernah ada percobaan menurunkan status
	 * aktif dari Pendaftar; percobaan itu dinonaktifkan dan sengaja dibiarkan sebagai jejak.</p>
	 *
	 * @return {@code true} bila sekolah aktif (default untuk nilai {@code null})
	 */
	public Boolean getAktif() {
//		if (getPendaftar() != null && getPendaftar().getAktif()) {
//			aktif = getPendaftar().getMerupakanSekolah();
//		}
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif; {@code null} akan dibaca kembali sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return NIP kepala sekolah untuk blok tanda tangan; dapat {@code null}
	 */
	public String getNipKepalaSekolah() {
		return nipKepalaSekolah;
	}

	/**
	 * @param nipKepalaSekolah NIP kepala sekolah
	 */
	public void setNipKepalaSekolah(String nipKepalaSekolah) {
		this.nipKepalaSekolah = nipKepalaSekolah;
	}

	/**
	 * Deskripsi/profil singkat sekolah untuk portal publik (kolom bertipe {@code text}).
	 *
	 * @return deskripsi sudah di-{@code trim}; string kosong bila belum ada
	 */
	@Column(columnDefinition = "text")
	public String getDeskripsi() {
		return deskripsi == null ? "" : deskripsi.trim();
	}

	/**
	 * @param deskripsi deskripsi/profil sekolah
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Nama host (domain/subdomain) tempat sekolah ini dilayani — <b>kolom penentu resolusi
	 * tenant</b>.
	 *
	 * <p>Dipetakan ke <code>domain_sekolah</code> bertipe {@code text} dengan
	 * <b>{@code unique = true}</b>. Nilainya boleh memuat <b>beberapa domain sekaligus, dipisah
	 * koma</b>; {@code SekolahAction.reInitByDomain()} memecahnya dengan
	 * {@code Common.pisahDomain()} dan mendaftarkan tiap potongan ke peta
	 * {@code sekolahByDomain}. Karena keunikan berlaku pada <i>seluruh isi kolom</i>, bukan pada
	 * tiap potongan, dua sekolah tetap dapat mendaftarkan domain yang sama selama string
	 * lengkapnya berbeda (validasi tambahan di form hanya membandingkan string utuh).</p>
	 *
	 * <p><b>Getter write-back — risiko tertinggi di kelas ini.</b> Bila kosong dan
	 * {@link #getPendaftar()} terisi, domain <b>disalin dari Pendaftar dan ditugaskan ke
	 * field</b>. Kombinasi dengan dirty-checking Hibernate berarti sekadar merender sebuah layar
	 * dapat menuliskan domain baru ke baris ini, yang berpotensi (a) menabrak {@code unique}
	 * constraint sehingga penyimpanan yang tak terkait ikut gagal, atau (b) <b>memindahkan
	 * pemetaan host</b> ke sekolah ini pada rebuild peta domain berikutnya. Perlu dicatat juga
	 * bahwa {@code reInitByDomain()} menyaring pada <i>kolom</i> (<code>domain IS NOT NULL</code>),
	 * sehingga sekolah yang domainnya hanya "ada" lewat write-back getter tidak akan masuk peta
	 * sampai nilai itu benar-benar tersimpan — sumber ketidakcocokan yang membingungkan.</p>
	 *
	 * <p><b>Catatan keamanan:</b> pencocokan di {@code SekolahUtil.getSekolahData} bersifat
	 * <code>startsWith</code> lalu <code>contains</code> (substring), bukan kesamaan penuh —
	 * lihat peringatan lengkap pada javadoc kelas.</p>
	 *
	 * @return domain sekolah sudah di-{@code trim}, atau <b>{@code null}</b> bila kosong
	 *         (perhatikan: getter ini mengembalikan {@code null}, bukan string kosong seperti
	 *         mayoritas getter teks lain di kelas ini)
	 */
	@Column(columnDefinition = "text", name = "domain_sekolah", unique = true)
	public String getDomain() {
		if (getPendaftar() != null && (domain == null || domain.trim().isEmpty())) {
			domain = pendaftar.getDomain();
		}
		return domain == null || domain.trim().isEmpty() ? null : domain.trim();
	}

	/**
	 * Menetapkan domain sekolah.
	 *
	 * <p>Tidak melakukan normalisasi apa pun (tidak trim, tidak lowercase, tidak memvalidasi
	 * format atau keunikan). Validasi bentrok domain dilakukan di lapisan layar
	 * ({@code SekolahAction}) sebelum penyimpanan; penulisan lewat jalur lain melewatkannya.</p>
	 *
	 * @param domain satu atau beberapa nama host dipisah koma
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * Moto sekolah untuk tampilan portal.
	 *
	 * <p><b>Getter write-back</b> dari {@link #getPendaftar()} bila kosong.</p>
	 *
	 * @return moto sudah di-{@code trim}; string kosong bila belum ada
	 */
	public String getMotto() {
		if (getPendaftar() != null && (motto == null || motto.trim().isEmpty())) {
			motto = pendaftar.getMotto();
		}
		return motto == null ? "" : motto.trim();
	}

	/**
	 * @param motto moto sekolah
	 */
	public void setMotto(String motto) {
		this.motto = motto;
	}

	/**
	 * Nomor WhatsApp sekolah (dipakai tombol chat pada portal/PSB bersama
	 * {@link #getTanyaWhatsapp()} dan {@link #getJawabWhatsappPsb()}).
	 *
	 * <p>Dinormalkan saat dibaca: karakter selain angka/titik dibuang, sehingga
	 * <code>"+62 812-3456"</code> menjadi <code>"628123456"</code>. Normalisasi tidak ditulis
	 * balik ke field.</p>
	 *
	 * @return nomor WhatsApp berisi angka/titik saja; string kosong bila belum ada
	 */
	public String getWa() {
		return wa == null ? "" : wa.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * @param wa nomor WhatsApp; disimpan apa adanya
	 */
	public void setWa(String wa) {
		this.wa = wa;
	}

	/**
	 * Satuan kerja (unit organisasi modul RAB/kepegawaian) yang mewakili sekolah ini.
	 *
	 * <p>Berpasangan dengan {@link #getGuruHarusPakaiSatuanKerja()} yang menentukan apakah data
	 * guru wajib mencantumkan satuan kerja.</p>
	 *
	 * @return satuan kerja terkait, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja_id")
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * @param satuanKerja satuan kerja terkait
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Kebijakan PSB: apakah calon siswa boleh memilih penjurusan saat mendaftar.
	 *
	 * @return {@code true} bila boleh memilih; {@code null} dinormalkan menjadi {@code false}
	 *         (default aman: fitur mati pada sekolah lama)
	 */
	public Boolean getPenjurusanBolehDipilihSaatPsb() {
		return penjurusanBolehDipilihSaatPsb == null ? false : penjurusanBolehDipilihSaatPsb;
	}

	/**
	 * @param penjurusanBolehDipilihSaatPsb kebijakan pemilihan penjurusan pada PSB
	 */
	public void setPenjurusanBolehDipilihSaatPsb(Boolean penjurusanBolehDipilihSaatPsb) {
		this.penjurusanBolehDipilihSaatPsb = penjurusanBolehDipilihSaatPsb;
	}

	/**
	 * Merchant/Client ID BNI khusus sekolah ini (override kredensial global instalasi).
	 *
	 * <p>Nilainya boleh berbentuk <b>peta per tahun ajaran</b>
	 * <code>"2023:XXX;2024:YYY"</code> yang diurai oleh
	 * {@link #checkCidDanPassword(Siswa, CalonSiswa)}.</p>
	 *
	 * @return merchant id BNI sudah di-{@code trim}; <b>string kosong</b> (bukan {@code null})
	 *         bila belum diatur — nilai kosong inilah yang memicu fallback ke konfigurasi global
	 */
	@Column(name = "bni_merchant_id", nullable = true)
	public String getBniMerchantId() {
		return bniMerchantId == null ? "" : bniMerchantId.trim();
	}

	/**
	 * @param bniMerchantId merchant id BNI (boleh berformat peta per tahun)
	 */
	public void setBniMerchantId(String bniMerchantId) {
		this.bniMerchantId = bniMerchantId;
	}

	/**
	 * Kredensial (password/secret) BNI khusus sekolah ini.
	 *
	 * <p>Disimpan sebagai teks biasa tanpa enkripsi dan — karena kelas ber-{@code @Audited} —
	 * ikut terekam ke tabel revisi Envers. Sama seperti merchant id, boleh berformat peta per
	 * tahun ajaran.</p>
	 *
	 * @return password BNI sudah di-{@code trim}; string kosong bila belum diatur
	 */
	@Column(name = "bni_password", nullable = true)
	public String getBniPassword() {
		return bniPassword == null ? "" : bniPassword.trim();
	}

	/**
	 * @param bniPassword kredensial BNI
	 */
	public void setBniPassword(String bniPassword) {
		this.bniPassword = bniPassword;
	}

	/**
	 * @return URL gateway BNI khusus sekolah ini; string kosong berarti memakai URL global
	 */
	@Column(name = "bni_gateway_url", nullable = true)
	public String getBniGatewayUrl() {
		return bniGatewayUrl == null ? "" : bniGatewayUrl.trim();
	}

	/**
	 * @param bniGatewayUrl URL gateway BNI
	 */
	public void setBniGatewayUrl(String bniGatewayUrl) {
		this.bniGatewayUrl = bniGatewayUrl;
	}

	/**
	 * Merchant/Client ID BSI khusus sekolah ini; boleh berformat peta per tahun ajaran seperti
	 * padanan BNI-nya.
	 *
	 * @return merchant id BSI sudah di-{@code trim}; string kosong bila belum diatur
	 */
	@Column(name = "bsi_merchant_id", nullable = true)
	public String getBsiMerchantId() {
		return bsiMerchantId == null ? "" : bsiMerchantId.trim();
	}

	/**
	 * @param bsiMerchantId merchant id BSI
	 */
	public void setBsiMerchantId(String bsiMerchantId) {
		this.bsiMerchantId = bsiMerchantId;
	}

	/**
	 * Kredensial (password/secret) BSI khusus sekolah ini; disimpan sebagai teks biasa dan ikut
	 * terekam Envers.
	 *
	 * @return password BSI sudah di-{@code trim}; string kosong bila belum diatur
	 */
	@Column(name = "bsi_password", nullable = true)
	public String getBsiPassword() {
		return bsiPassword == null ? "" : bsiPassword.trim();
	}

	/**
	 * @param bsiPassword kredensial BSI
	 */
	public void setBsiPassword(String bsiPassword) {
		this.bsiPassword = bsiPassword;
	}

	/**
	 * @return URL gateway BSI khusus sekolah ini; string kosong berarti memakai URL global
	 */
	@Column(name = "bsi_gateway_url", nullable = true)
	public String getBsiGatewayUrl() {
		return bsiGatewayUrl == null ? "" : bsiGatewayUrl.trim();
	}

	/**
	 * @param bsiGatewayUrl URL gateway BSI
	 */
	public void setBsiGatewayUrl(String bsiGatewayUrl) {
		this.bsiGatewayUrl = bsiGatewayUrl;
	}

	/**
	 * Meresolusi pasangan <code>[merchant_id, password]</code> BNI untuk sekolah <b>ini</b> pada
	 * tahun ajaran tertentu.
	 *
	 * <p>Urutan resolusi:</p>
	 * <ol>
	 *   <li>pakai {@link #getBniMerchantId()}/{@link #getBniPassword()} milik sekolah ini bila
	 *       tidak kosong; bila kosong, ambil konfigurasi global
	 *       <code>bni_merchant_id</code>/<code>bni_password</code> lewat
	 *       {@code Common.getKonfigurasi(...)};</li>
	 *   <li>bila {@code tahun} diberikan dan nilai hasil langkah 1 mengandung <code>":"</code>,
	 *       nilai itu diperlakukan sebagai daftar <code>tahun:nilai</code> dipisah
	 *       <code>";"</code> dan dicari entri yang tahunnya cocok;</li>
	 *   <li>bila hasil akhir masih kosong, jatuh kembali ke konfigurasi global sekali lagi.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> {@code Common.getKonfigurasi(kunci, default)} bersifat
	 * <i>auto-seed</i> — bila kunci belum ada di database, nilai default akan <b>ditulis</b> ke
	 * tabel konfigurasi. Jadi memanggil method ini pada instalasi baru dapat membuat baris
	 * konfigurasi <code>bni_merchant_id = "000"</code>.</p>
	 *
	 * <p><b>Kasus tepi:</b> penguraian format tahun dibungkus {@code try/catch} yang hanya
	 * melaporkan lewat {@code Common.tampilErrorJikaAdmin(e)}; format yang salah tidak
	 * menggagalkan transaksi, melainkan menyisakan nilai apa adanya (mis. string
	 * <code>"2024:ABC;…"</code> utuh) yang kemudian dikirim ke gateway.</p>
	 *
	 * <p><b>Status pemakaian:</b> pada pemeriksaan terakhir, <b>tidak ada pemanggil</b> varian
	 * instance ini di seluruh pohon sumber — seluruh jalur BNI memakai varian statis
	 * {@link #checkCidDanPassword(Siswa, CalonSiswa)}. Method ini efektif kode mati; variabel
	 * lokal <code>Sekolah sekolah = this;</code> beserta pemeriksaan
	 * <code>sekolah != null</code> di dalamnya adalah sisa salin-tempel dari varian statis.</p>
	 *
	 * @param tahun tahun ajaran untuk memilih entri pada format <code>tahun:nilai;…</code>;
	 *              boleh {@code null} untuk melewati pemilihan per tahun
	 * @return array dua elemen: indeks 0 = merchant id, indeks 1 = password
	 */
	public String[] checkCidDanPassword(Integer tahun) {

		Sekolah sekolah = this;

		String merchant_id = sekolah != null && !sekolah.getBniMerchantId().isEmpty() ? sekolah.getBniMerchantId()
				: Common.getKonfigurasi("bni_merchant_id", "000").getNilai().trim();
		String Password = sekolah != null && !sekolah.getBniPassword().isEmpty() ? sekolah.getBniPassword()
				: Common.getKonfigurasi("bni_password", "").getNilai().trim();

		if (tahun != null) {
			try {
				if (StringUtils.contains(merchant_id, ":")) {
					for (String s : StringUtils.split(merchant_id, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(tahun);
						if (ketemu) {
							merchant_id = m;
							break;
						}
					}
				}

				if (StringUtils.contains(Password, ":")) {
					for (String s : StringUtils.split(Password, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(tahun);
						if (ketemu) {
							Password = m;
							break;
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (merchant_id == null || merchant_id.trim().isEmpty()) {
			merchant_id = Common.getKonfigurasi("bni_merchant_id", "000").getNilai().trim();
		}
		if (Password == null || Password.trim().isEmpty()) {
			Password = Common.getKonfigurasi("bni_password", "").getNilai().trim();
		}

		return new String[] { merchant_id, Password };
	}

	/**
	 * Meresolusi pasangan <code>[merchant_id, password]</code> BNI untuk seorang siswa atau calon
	 * siswa — <b>jalur yang sesungguhnya dipakai</b> oleh integrasi BNI.
	 *
	 * <p>Sekolah diambil dari {@code siswa.getSekolah()} bila {@code siswa} tidak {@code null},
	 * jika tidak dari {@code calonSiswa.getSekolah()}. Selanjutnya kredensial diresolusi dengan
	 * pola yang sama seperti {@link #checkCidDanPassword(Integer)}: kredensial sekolah bila ada,
	 * kalau tidak kredensial global instalasi; lalu bila nilainya berformat
	 * <code>tahun:nilai;…</code>, entri dipilih berdasarkan {@code getTahunMasuk()} peserta didik.</p>
	 *
	 * <p><b>Konsekuensi cakupan tenant:</b> ini adalah varian "fail-open" versi kredensial —
	 * bila sekolah tidak dapat diresolusi (kedua argumen {@code null}, atau relasi
	 * {@code sekolah} peserta didik kosong), method <b>tidak melempar error</b> melainkan
	 * mengembalikan <b>kredensial global instalasi</b>. Transaksi tetap terbit, tetapi mendarat di
	 * merchant milik penyedia layanan, bukan milik sekolah yang bersangkutan.</p>
	 *
	 * <p><b>Bug dan kuirk yang terverifikasi di badan method:</b></p>
	 * <ul>
	 *   <li>blok {@code if (calonSiswa != null)} <b>menimpa</b> hasil perhitungan sebelumnya.
	 *       Bila {@code siswa} dan {@code calonSiswa} keduanya terisi, sekolah yang dipakai tetap
	 *       milik <b>siswa</b> (karena resolusi sekolah di awal mendahulukan siswa), namun syarat
	 *       {@code siswa != null} pada perhitungan pertama diganti {@code calonSiswa != null} —
	 *       kondisi yang secara logis sudah pasti benar di dalam blok tersebut;</li>
	 *   <li>pada cabang {@code calonSiswa}, seluruh baris {@code System.out.println} mencetak
	 *       <b>{@code siswa}</b> dan <b>{@code siswa.getTahunMasuk()}</b> padahal {@code siswa}
	 *       pada cabang itu {@code null} — <b>berpotensi {@code NullPointerException}</b>.
	 *       Untungnya seluruh blok dibungkus {@code try/catch}, sehingga NPE tersebut "hanya"
	 *       membatalkan pemilihan kredensial per tahun secara diam-diam dan menyisakan nilai
	 *       gabungan <code>tahun:nilai;…</code> apa adanya;</li>
	 *   <li>baris {@code println} tersebut <b>mencetak nilai password ke log server</b>
	 *       (variabel {@code m} pada loop Password) — kebocoran kredensial ke berkas log.</li>
	 * </ul>
	 *
	 * @param siswa      siswa pemilik tagihan; boleh {@code null}
	 * @param calonSiswa calon siswa (PSB) pemilik tagihan; boleh {@code null}
	 * @return array dua elemen: indeks 0 = merchant id, indeks 1 = password; tidak pernah
	 *         {@code null}, tetapi dapat berisi kredensial global bila sekolah tidak teridentifikasi
	 * @see ais.common.BniCommon
	 */
	public static String[] checkCidDanPassword(Siswa siswa, CalonSiswa calonSiswa) {

		Sekolah sekolah = null;
		if (siswa != null) {
			sekolah = siswa.getSekolah();
		} else if (calonSiswa != null) {
			sekolah = calonSiswa.getSekolah();
		}

		String merchant_id = siswa != null && sekolah != null && !sekolah.getBniMerchantId().isEmpty()
				? sekolah.getBniMerchantId()
				: Common.getKonfigurasi("bni_merchant_id", "000").getNilai().trim();
		String Password = siswa != null && sekolah != null && !sekolah.getBniPassword().isEmpty()
				? sekolah.getBniPassword()
				: Common.getKonfigurasi("bni_password", "").getNilai().trim();

		if (calonSiswa != null) {
			merchant_id = calonSiswa != null && sekolah != null && !sekolah.getBniMerchantId().isEmpty()
					? sekolah.getBniMerchantId()
					: Common.getKonfigurasi("bni_merchant_id", "000").getNilai().trim();
			Password = calonSiswa != null && sekolah != null && !sekolah.getBniPassword().isEmpty()
					? sekolah.getBniPassword()
					: Common.getKonfigurasi("bni_password", "").getNilai().trim();
		}

		if (siswa != null && siswa.getTahunMasuk() != null) {
			try {
				if (StringUtils.contains(merchant_id, ":")) {
					for (String s : StringUtils.split(merchant_id, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(siswa.getTahunMasuk());
						System.out.println("chek merchant_id siswa " + siswa + " masuk " + siswa.getTahunMasuk()
								+ " ta " + ta + " m " + m + " ketemu " + ketemu);
						if (ketemu) {
							merchant_id = m;
							break;
						}
					}
				}

				if (StringUtils.contains(Password, ":")) {
					for (String s : StringUtils.split(Password, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(siswa.getTahunMasuk());
						System.out.println("chek Password siswa " + siswa + " masuk " + siswa.getTahunMasuk() + " ta "
								+ ta + " m " + (m == null || m.isEmpty() ? "(kosong)" : "(disamarkan)") + " ketemu " + ketemu);
						if (ketemu) {
							Password = m;
							break;
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else if (calonSiswa != null && calonSiswa.getTahunMasuk() != null) {
			try {
				if (StringUtils.contains(merchant_id, ":")) {
					for (String s : StringUtils.split(merchant_id, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(calonSiswa.getTahunMasuk());
						System.out.println("chek merchant_id calonSiswa " + siswa + " masuk " + siswa.getTahunMasuk()
								+ " ta " + ta + " m " + m + " ketemu " + ketemu);
						if (ketemu) {
							merchant_id = m;
							break;
						}
					}
				}

				if (StringUtils.contains(Password, ":")) {
					for (String s : StringUtils.split(Password, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(calonSiswa.getTahunMasuk());
						System.out.println("chek Password calonSiswa " + siswa + " masuk " + siswa.getTahunMasuk()
								+ " ta " + ta + " m " + (m == null || m.isEmpty() ? "(kosong)" : "(disamarkan)") + " ketemu " + ketemu);
						if (ketemu) {
							Password = m;
							break;
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (merchant_id == null || merchant_id.trim().isEmpty()) {
			merchant_id = Common.getKonfigurasi("bni_merchant_id", "000").getNilai().trim();
		}
		if (Password == null || Password.trim().isEmpty()) {
			Password = Common.getKonfigurasi("bni_password", "").getNilai().trim();
		}

		return new String[] { merchant_id, Password };
	}

	/**
	 * Padanan BSI dari {@link #checkCidDanPassword(Siswa, CalonSiswa)}: meresolusi
	 * <code>[merchant_id, password]</code> BSI untuk seorang siswa atau calon siswa.
	 *
	 * <p>Struktur, urutan fallback (kredensial sekolah &rarr; konfigurasi global
	 * <code>bsi_merchant_id</code>/<code>bsi_password</code>), penguraian format
	 * <code>tahun:nilai;…</code>, <b>maupun seluruh kuirk dan bug yang dijelaskan pada method
	 * BNI</b> (blok {@code calonSiswa} yang menimpa, {@code println} yang mengacu {@code siswa}
	 * null pada cabang calon siswa, dan pencetakan password ke log) berlaku sama persis di sini —
	 * badan method adalah salinan dengan nama kunci konfigurasi yang diganti.</p>
	 *
	 * @param siswa      siswa pemilik tagihan; boleh {@code null}
	 * @param calonSiswa calon siswa (PSB) pemilik tagihan; boleh {@code null}
	 * @return array dua elemen: indeks 0 = merchant id BSI, indeks 1 = password BSI
	 * @see ais.common.BsiCommon
	 */
	public static String[] checkCidDanPasswordBsi(Siswa siswa, CalonSiswa calonSiswa) {

		Sekolah sekolah = null;
		if (siswa != null) {
			sekolah = siswa.getSekolah();
		} else if (calonSiswa != null) {
			sekolah = calonSiswa.getSekolah();
		}

		String merchant_id = siswa != null && sekolah != null && !sekolah.getBsiMerchantId().isEmpty()
				? sekolah.getBsiMerchantId()
				: Common.getKonfigurasi("bsi_merchant_id", "000").getNilai().trim();
		String Password = siswa != null && sekolah != null && !sekolah.getBsiPassword().isEmpty()
				? sekolah.getBsiPassword()
				: Common.getKonfigurasi("bsi_password", "").getNilai().trim();

		if (calonSiswa != null) {
			merchant_id = calonSiswa != null && sekolah != null && !sekolah.getBsiMerchantId().isEmpty()
					? sekolah.getBsiMerchantId()
					: Common.getKonfigurasi("bsi_merchant_id", "000").getNilai().trim();
			Password = calonSiswa != null && sekolah != null && !sekolah.getBsiPassword().isEmpty()
					? sekolah.getBsiPassword()
					: Common.getKonfigurasi("bsi_password", "").getNilai().trim();
		}

		if (siswa != null && siswa.getTahunMasuk() != null) {
			try {
				if (StringUtils.contains(merchant_id, ":")) {
					for (String s : StringUtils.split(merchant_id, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(siswa.getTahunMasuk());
						System.out.println("chek merchant_id siswa " + siswa + " masuk " + siswa.getTahunMasuk()
								+ " ta " + ta + " m " + m + " ketemu " + ketemu);
						if (ketemu) {
							merchant_id = m;
							break;
						}
					}
				}

				if (StringUtils.contains(Password, ":")) {
					for (String s : StringUtils.split(Password, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(siswa.getTahunMasuk());
						System.out.println("chek Password siswa " + siswa + " masuk " + siswa.getTahunMasuk() + " ta "
								+ ta + " m " + (m == null || m.isEmpty() ? "(kosong)" : "(disamarkan)") + " ketemu " + ketemu);
						if (ketemu) {
							Password = m;
							break;
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else if (calonSiswa != null && calonSiswa.getTahunMasuk() != null) {
			try {
				if (StringUtils.contains(merchant_id, ":")) {
					for (String s : StringUtils.split(merchant_id, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(calonSiswa.getTahunMasuk());
						System.out.println("chek merchant_id calonSiswa " + siswa + " masuk " + siswa.getTahunMasuk()
								+ " ta " + ta + " m " + m + " ketemu " + ketemu);
						if (ketemu) {
							merchant_id = m;
							break;
						}
					}
				}

				if (StringUtils.contains(Password, ":")) {
					for (String s : StringUtils.split(Password, ";")) {
						Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
						String m = StringUtils.split(s, ":")[1];
						boolean ketemu = ta.equals(calonSiswa.getTahunMasuk());
						System.out.println("chek Password calonSiswa " + siswa + " masuk " + siswa.getTahunMasuk()
								+ " ta " + ta + " m " + (m == null || m.isEmpty() ? "(kosong)" : "(disamarkan)") + " ketemu " + ketemu);
						if (ketemu) {
							Password = m;
							break;
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (merchant_id == null || merchant_id.trim().isEmpty()) {
			merchant_id = Common.getKonfigurasi("bsi_merchant_id", "000").getNilai().trim();
		}
		if (Password == null || Password.trim().isEmpty()) {
			Password = Common.getKonfigurasi("bsi_password", "").getNilai().trim();
		}

		return new String[] { merchant_id, Password };
	}

	/**
	 * CSS tambahan khusus sekolah ini, disuntikkan ke halaman portal untuk menyesuaikan warna/
	 * tata letak.
	 *
	 * <p><b>Catatan keamanan:</b> isinya dirender apa adanya ke halaman; siapa pun yang dapat
	 * menyunting master Sekolah (termasuk lewat jalur pewarisan hak menu induk yang dijelaskan di
	 * javadoc kelas) dapat menyisipkan CSS sewenang-wenang.</p>
	 *
	 * @return CSS tambahan; string kosong bila belum ada
	 */
	public String getCss() {
		return css == null ? "" : css;
	}

	/**
	 * @param css CSS tambahan untuk portal sekolah
	 */
	public void setCss(String css) {
		this.css = css;
	}

	/**
	 * Tema tampilan yang dipakai sekolah ini.
	 *
	 * <p>Nama method mempertahankan salah ketik asli ("Piilhan"); mengganti namanya berarti
	 * mengganti nama properti Hibernate dan memutus pemetaan di seluruh kode pemanggil.</p>
	 *
	 * @return salah satu dari {@link #TAMPILAN_DEFAULT}, {@link #TAMPILAN_KLASIK},
	 *         {@link #TAMPILAN_BARU}; {@code null} dinormalkan menjadi {@link #TAMPILAN_DEFAULT}
	 */
	@javax.persistence.Column(name = "pilihan_tampilan", length = 30)
	public String getPiilhanTampilan() {
		return piilhanTampilan == null ? TAMPILAN_DEFAULT : piilhanTampilan;
	}

	/**
	 * @param piilhanTampilan nama tema; sebaiknya memakai konstanta {@code TAMPILAN_*} — nilai di
	 *                        luar itu tidak divalidasi dan akan membuat halaman jatuh ke perilaku
	 *                        bawaan lapisan tampilan
	 */
	public void setPiilhanTampilan(String piilhanTampilan) {
		this.piilhanTampilan = piilhanTampilan;
	}

	/**
	 * Nomor Pokok Sekolah Nasional (NPSN).
	 *
	 * <p><b>Getter write-back</b> dari {@code pendaftar.getKode()} — sumber yang <b>sama</b>
	 * dengan {@link #getNss()}; lihat catatan kuirk di sana.</p>
	 *
	 * @return NPSN; dapat {@code null} bila belum diisi dan tidak ada Pendaftar
	 */
	public String getNpsn() {
		if (getPendaftar() != null && (npsn == null || npsn.trim().isEmpty())) {
			npsn = pendaftar.getKode();
		}
		return npsn;
	}

	/**
	 * @param npsn Nomor Pokok Sekolah Nasional
	 */
	public void setNpsn(String npsn) {
		this.npsn = npsn;
	}

	/**
	 * @return kelurahan/desa alamat sekolah; dapat {@code null}
	 */
	public String getKelurahan() {
		return kelurahan;
	}

	/**
	 * @param kelurahan kelurahan/desa alamat sekolah
	 */
	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	/**
	 * @return kecamatan alamat sekolah; dapat {@code null}
	 */
	public String getKecamatan() {
		return kecamatan;
	}

	/**
	 * @param kecamatan kecamatan alamat sekolah
	 */
	public void setKecamatan(String kecamatan) {
		this.kecamatan = kecamatan;
	}

	/**
	 * @return kabupaten/kota alamat sekolah; dapat {@code null}
	 */
	public String getKabupatenKota() {
		return kabupatenKota;
	}

	/**
	 * @param kabupatenKota kabupaten/kota alamat sekolah
	 */
	public void setKabupatenKota(String kabupatenKota) {
		this.kabupatenKota = kabupatenKota;
	}

	/**
	 * @return provinsi alamat sekolah (ejaan properti "propinsi" dipertahankan); dapat {@code null}
	 */
	public String getPropinsi() {
		return propinsi;
	}

	/**
	 * @param propinsi provinsi alamat sekolah
	 */
	public void setPropinsi(String propinsi) {
		this.propinsi = propinsi;
	}

	/**
	 * @return nomor RT alamat sekolah; dapat {@code null}
	 */
	public String getRt() {
		return rt;
	}

	/**
	 * @param rt nomor RT alamat sekolah
	 */
	public void setRt(String rt) {
		this.rt = rt;
	}

	/**
	 * @return nomor RW alamat sekolah; dapat {@code null}
	 */
	public String getRw() {
		return rw;
	}

	/**
	 * @param rw nomor RW alamat sekolah
	 */
	public void setRw(String rw) {
		this.rw = rw;
	}

	/**
	 * @return dusun/kampung alamat sekolah; dapat {@code null}
	 */
	public String getDusun() {
		return dusun;
	}

	/**
	 * @param dusun dusun/kampung alamat sekolah
	 */
	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	/**
	 * @return kode pos alamat sekolah; dapat {@code null}
	 */
	public String getKodePos() {
		return kodePos;
	}

	/**
	 * @param kodePos kode pos alamat sekolah
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/** Konfigurasi website publik sekolah dalam JSON; identitas/kontak tetap berasal dari kolom model. */
	@Column(name = "website", columnDefinition = "text")
	public String getWebsite() {
		return website == null ? "" : website.trim();
	}

	/**
	 * @param website konfigurasi website publik (JSON); disimpan apa adanya tanpa validasi format
	 */
	public void setWebsite(String website) {
		this.website = website;
	}

	/**
	 * Label jabatan penanda tangan ke-1, dipasangkan dengan {@link #getPegawai1()} pada blok tanda
	 * tangan laporan/surat.
	 *
	 * @return label jabatan; {@code null} dinormalkan menjadi <code>"Pejabat I"</code>
	 */
	public String getLabelPejabat1() {
		return labelPejabat1 == null ? "Pejabat I" : labelPejabat1;
	}

	/**
	 * @param labelPejabat1 label jabatan penanda tangan ke-1
	 */
	public void setLabelPejabat1(String labelPejabat1) {
		this.labelPejabat1 = labelPejabat1;
	}

	/**
	 * @return label jabatan penanda tangan ke-2; {@code null} dinormalkan menjadi
	 *         <code>"Pejabat II"</code>
	 */
	public String getLabelPejabat2() {
		return labelPejabat2 == null ? "Pejabat II" : labelPejabat2;
	}

	/**
	 * @param labelPejabat2 label jabatan penanda tangan ke-2
	 */
	public void setLabelPejabat2(String labelPejabat2) {
		this.labelPejabat2 = labelPejabat2;
	}

	/**
	 * @return label jabatan penanda tangan ke-3; {@code null} dinormalkan menjadi
	 *         <code>"Pejabat III"</code>
	 */
	public String getLabelPejabat3() {
		return labelPejabat3 == null ? "Pejabat III" : labelPejabat3;
	}

	/**
	 * @param labelPejabat3 label jabatan penanda tangan ke-3
	 */
	public void setLabelPejabat3(String labelPejabat3) {
		this.labelPejabat3 = labelPejabat3;
	}

	/**
	 * Pegawai penanda tangan ke-1 (berpasangan dengan {@link #getLabelPejabat1()}).
	 *
	 * @return pegawai penanda tangan, atau {@code null} bila belum diatur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai1", nullable = true)
	public Pegawai getPegawai1() {
		pegawai1 = check(pegawai1);
		return pegawai1;
	}

	/**
	 * @param pegawai1 pegawai penanda tangan ke-1
	 */
	public void setPegawai1(Pegawai pegawai1) {
		this.pegawai1 = pegawai1;
	}

	/**
	 * Pegawai penanda tangan ke-2 (berpasangan dengan {@link #getLabelPejabat2()}).
	 *
	 * @return pegawai penanda tangan, atau {@code null} bila belum diatur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai2", nullable = true)
	public Pegawai getPegawai2() {
		pegawai2 = check(pegawai2);
		return pegawai2;
	}

	/**
	 * @param pegawai2 pegawai penanda tangan ke-2
	 */
	public void setPegawai2(Pegawai pegawai2) {
		this.pegawai2 = pegawai2;
	}

	/**
	 * Pegawai penanda tangan ke-3 (berpasangan dengan {@link #getLabelPejabat3()}).
	 *
	 * @return pegawai penanda tangan, atau {@code null} bila belum diatur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai3", nullable = true)
	public Pegawai getPegawai3() {
		pegawai3 = check(pegawai3);
		return pegawai3;
	}

	/**
	 * @param pegawai3 pegawai penanda tangan ke-3
	 */
	public void setPegawai3(Pegawai pegawai3) {
		this.pegawai3 = pegawai3;
	}

	/**
	 * @return label jabatan penanda tangan ke-4; {@code null} dinormalkan menjadi
	 *         <code>"Pejabat IV"</code>
	 */
	public String getLabelPejabat4() {

		return labelPejabat4 == null ? "Pejabat IV" : labelPejabat4;
	}

	/**
	 * @param labelPejabat4 label jabatan penanda tangan ke-4
	 */
	public void setLabelPejabat4(String labelPejabat4) {
		this.labelPejabat4 = labelPejabat4;
	}

	/**
	 * @return label jabatan penanda tangan ke-5; {@code null} dinormalkan menjadi
	 *         <code>"Pejabat V"</code>
	 */
	public String getLabelPejabat5() {
		return labelPejabat5 == null ? "Pejabat V" : labelPejabat5;
	}

	/**
	 * @param labelPejabat5 label jabatan penanda tangan ke-5
	 */
	public void setLabelPejabat5(String labelPejabat5) {
		this.labelPejabat5 = labelPejabat5;
	}

	/**
	 * Pegawai penanda tangan ke-5 (berpasangan dengan {@link #getLabelPejabat5()}).
	 *
	 * <p>Urutan deklarasi di berkas ini menempatkan pasangan ke-5 sebelum ke-4; itu murni
	 * kosmetik dan tidak memengaruhi pemetaan.</p>
	 *
	 * @return pegawai penanda tangan, atau {@code null} bila belum diatur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai5", nullable = true)
	public Pegawai getPegawai5() {
		pegawai5 = check(pegawai5);
		return pegawai5;
	}

	/**
	 * @param pegawai5 pegawai penanda tangan ke-5
	 */
	public void setPegawai5(Pegawai pegawai5) {
		this.pegawai5 = pegawai5;
	}

	/**
	 * Pegawai penanda tangan ke-4 (berpasangan dengan {@link #getLabelPejabat4()}).
	 *
	 * @return pegawai penanda tangan, atau {@code null} bila belum diatur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai4", nullable = true)
	public Pegawai getPegawai4() {
		pegawai4 = check(pegawai4);
		return pegawai4;
	}

	/**
	 * @param pegawai4 pegawai penanda tangan ke-4
	 */
	public void setPegawai4(Pegawai pegawai4) {
		this.pegawai4 = pegawai4;
	}

	/**
	 * Perguruan tinggi induk — cabang hierarki tenant untuk instalasi kampus.
	 *
	 * <p>Sebuah baris Sekolah dapat sekaligus tertaut ke {@link #getYayasan()} dan ke perguruan
	 * tinggi; tidak ada pembatasan yang memaksa hanya salah satu terisi.</p>
	 *
	 * @return perguruan tinggi induk, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi", nullable = true)
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		return perguruanTinggi;
	}

	/**
	 * @param perguruanTinggi perguruan tinggi induk
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * Sapaan pembuka widget WhatsApp pada portal sekolah.
	 *
	 * @return teks sapaan; {@code null} dinormalkan menjadi teks bawaan
	 *         <code>"Salamat Datang, apa yang bisa kami bantu?"</code> (salah ketik "Salamat"
	 *         ada pada nilai default asli dan sengaja tidak diubah agar tampilan tidak berubah
	 *         tanpa permintaan)
	 */
	public String getTanyaWhatsapp() {
		return tanyaWhatsapp == null ? "Salamat Datang, apa yang bisa kami bantu?" : tanyaWhatsapp;
	}

	/**
	 * @param tanyaWhatsapp teks sapaan widget WhatsApp
	 */
	public void setTanyaWhatsapp(String tanyaWhatsapp) {
		this.tanyaWhatsapp = tanyaWhatsapp;
	}

	/**
	 * Teks yang sudah terisi otomatis pada jendela chat WhatsApp ketika calon siswa menekan tombol
	 * tanya-jawab PSB.
	 *
	 * @return teks pesan; {@code null} dinormalkan menjadi kalimat bawaan tentang informasi
	 *         penerimaan siswa baru
	 */
	public String getJawabWhatsappPsb() {
		return jawabWhatsappPsb == null
				? "Saya ingin menanyakan tentang informasi penerimaan siswa baru, apakah Anda bisa membantu?"
				: jawabWhatsappPsb;
	}

	/**
	 * @param jawabWhatsappPsb teks pesan bawaan chat PSB
	 */
	public void setJawabWhatsappPsb(String jawabWhatsappPsb) {
		this.jawabWhatsappPsb = jawabWhatsappPsb;
	}

	/**
	 * Kebijakan PSB: apakah pemilihan penjurusan bersifat wajib.
	 *
	 * <p>Hanya bermakna bila {@link #getPenjurusanBolehDipilihSaatPsb()} juga aktif; entity tidak
	 * memaksa konsistensi keduanya.</p>
	 *
	 * @return {@code true} bila wajib; {@code null} dinormalkan menjadi {@code false}
	 */
	public Boolean getPenjurusanWajibDipilih() {
		return penjurusanWajibDipilih == null ? false : penjurusanWajibDipilih;
	}

	/**
	 * @param penjurusanWajibDipilih kebijakan wajib memilih penjurusan
	 */
	public void setPenjurusanWajibDipilih(Boolean penjurusanWajibDipilih) {
		this.penjurusanWajibDipilih = penjurusanWajibDipilih;
	}

	/**
	 * Sakelar kanal pembayaran Flip untuk sekolah ini.
	 *
	 * @return {@code true} bila kanal Flip aktif; {@code null} dinormalkan menjadi {@code false}
	 *         sehingga penambahan kolom tidak menyalakan kanal pada sekolah lama
	 */
	public Boolean getAktfkanPembayaranViaFlip() {
		return aktfkanPembayaranViaFlip == null ? false : aktfkanPembayaranViaFlip;
	}

	/**
	 * @param aktfkanPembayaranViaFlip sakelar kanal Flip
	 */
	public void setAktfkanPembayaranViaFlip(Boolean aktfkanPembayaranViaFlip) {
		this.aktfkanPembayaranViaFlip = aktfkanPembayaranViaFlip;
	}

	/**
	 * @return API key Flip milik sekolah ini (teks biasa, ikut terekam Envers); string kosong bila
	 *         belum diatur
	 */
	public String getApiKeyFlip() {
		return apiKeyFlip == null ? "" : apiKeyFlip;
	}

	/**
	 * @param apiKeyFlip API key Flip
	 */
	public void setApiKeyFlip(String apiKeyFlip) {
		this.apiKeyFlip = apiKeyFlip;
	}

	/**
	 * @return token Flip milik sekolah ini (teks biasa, ikut terekam Envers); string kosong bila
	 *         belum diatur
	 */
	public String getTokenFlip() {
		return tokenFlip == null ? "" : tokenFlip;
	}

	/**
	 * @param tokenFlip token Flip
	 */
	public void setTokenFlip(String tokenFlip) {
		this.tokenFlip = tokenFlip;
	}

	/**
	 * @return biaya administrasi yang dibebankan pada transaksi Flip; {@code null} dinormalkan
	 *         menjadi {@code 0.0}, sehingga "belum diatur" tidak dapat dibedakan dari "nol"
	 */
	public Double getBiayaAdminFlip() {
		return biayaAdminFlip == null ? 0.0 : biayaAdminFlip;
	}

	/**
	 * @param biayaAdminFlip biaya administrasi transaksi Flip
	 */
	public void setBiayaAdminFlip(Double biayaAdminFlip) {
		this.biayaAdminFlip = biayaAdminFlip;
	}

	/**
	 * Sakelar kanal pembayaran Finpay untuk sekolah ini.
	 *
	 * @return {@code true} bila kanal Finpay aktif; {@code null} dinormalkan menjadi {@code false}
	 */
	public Boolean getAktfkanPembayaranViaFinpay() {
		return aktfkanPembayaranViaFinpay == null ? false : aktfkanPembayaranViaFinpay;
	}

	/**
	 * @param aktfkanPembayaranViaFinpay sakelar kanal Finpay
	 */
	public void setAktfkanPembayaranViaFinpay(Boolean aktfkanPembayaranViaFinpay) {
		this.aktfkanPembayaranViaFinpay = aktfkanPembayaranViaFinpay;
	}

	/**
	 * @return API key Finpay milik sekolah ini (teks biasa, ikut terekam Envers); string kosong
	 *         bila belum diatur
	 */
	public String getApiKeyFinpay() {
		return apiKeyFinpay == null ? "" : apiKeyFinpay;
	}

	/**
	 * @param apiKeyFinpay API key Finpay
	 */
	public void setApiKeyFinpay(String apiKeyFinpay) {
		this.apiKeyFinpay = apiKeyFinpay;
	}

	/**
	 * @return token Finpay milik sekolah ini (teks biasa, ikut terekam Envers); string kosong bila
	 *         belum diatur
	 */
	public String getTokenFinpay() {
		return tokenFinpay == null ? "" : tokenFinpay;
	}

	/**
	 * @param tokenFinpay token Finpay
	 */
	public void setTokenFinpay(String tokenFinpay) {
		this.tokenFinpay = tokenFinpay;
	}

	/**
	 * @return biaya administrasi transaksi Finpay; {@code null} dinormalkan menjadi {@code 0.0}
	 */
	public Double getBiayaAdminFinpay() {
		return biayaAdminFinpay == null ? 0.0 : biayaAdminFinpay;
	}

	/**
	 * @param biayaAdminFinpay biaya administrasi transaksi Finpay
	 */
	public void setBiayaAdminFinpay(Double biayaAdminFinpay) {
		this.biayaAdminFinpay = biayaAdminFinpay;
	}

	/**
	 * @return nama wakil kepala sekolah untuk blok tanda tangan; dapat {@code null}
	 */
	@Column(name = "nama_wakil_kepala_sekolah")
	public String getNamaWakilKepalaSekolah() {
		return namaWakilKepalaSekolah;
	}

	/**
	 * @param namaWakilKepalaSekolah nama wakil kepala sekolah
	 */
	public void setNamaWakilKepalaSekolah(String namaWakilKepalaSekolah) {
		this.namaWakilKepalaSekolah = namaWakilKepalaSekolah;
	}

	/**
	 * @return NIP wakil kepala sekolah untuk blok tanda tangan; dapat {@code null}
	 */
	@Column(name = "nip_wakil_kepala_sekolah")
	public String getNipWakilKepalaSekolah() {
		return nipWakilKepalaSekolah;
	}

	/**
	 * @param nipWakilKepalaSekolah NIP wakil kepala sekolah
	 */
	public void setNipWakilKepalaSekolah(String nipWakilKepalaSekolah) {
		this.nipWakilKepalaSekolah = nipWakilKepalaSekolah;
	}

	/**
	 * Kebijakan kepegawaian: apakah setiap data guru wajib mencantumkan
	 * {@link #getSatuanKerja() satuan kerja}.
	 *
	 * @return {@code true} bila wajib; {@code null} dinormalkan menjadi {@code false}
	 */
	public Boolean getGuruHarusPakaiSatuanKerja() {
		return guruHarusPakaiSatuanKerja == null ? false : guruHarusPakaiSatuanKerja;
	}

	/**
	 * @param guruHarusPakaiSatuanKerja kebijakan wajib satuan kerja pada data guru
	 */
	public void setGuruHarusPakaiSatuanKerja(Boolean guruHarusPakaiSatuanKerja) {
		this.guruHarusPakaiSatuanKerja = guruHarusPakaiSatuanKerja;
	}

	/**
	 * Sakelar kanal pembayaran Esmartlink untuk sekolah ini.
	 *
	 * @return {@code true} bila kanal Esmartlink aktif; {@code null} dinormalkan menjadi
	 *         {@code false}
	 */
	public Boolean getAktfkanPembayaranViaEsmartlink() {
		return aktfkanPembayaranViaEsmartlink == null ? false : aktfkanPembayaranViaEsmartlink;
	}

	/**
	 * @param aktfkanPembayaranViaEsmartlink sakelar kanal Esmartlink
	 */
	public void setAktfkanPembayaranViaEsmartlink(Boolean aktfkanPembayaranViaEsmartlink) {
		this.aktfkanPembayaranViaEsmartlink = aktfkanPembayaranViaEsmartlink;
	}

	/**
	 * Mengaktifkan penerbitan invoice Online BMT pada sekolah ini. Sakelar global
	 * {@code aktifkan_pembayaran_via_online_bmt} tetap harus aktif; getter sengaja
	 * menormalkan null menjadi false agar penambahan kolom tidak menyalakan kanal
	 * pada sekolah lama secara tidak sengaja.
	 *
	 * @return {@code true} bila kanal Online BMT aktif untuk sekolah ini
	 */
	@Column(name = "aktfkan_pembayaran_via_online_bmt")
	public Boolean getAktfkanPembayaranViaOnlineBmt() {
		return aktfkanPembayaranViaOnlineBmt == null ? false : aktfkanPembayaranViaOnlineBmt;
	}

	/**
	 * @param aktfkanPembayaranViaOnlineBmt sakelar kanal Online BMT untuk sekolah ini
	 */
	public void setAktfkanPembayaranViaOnlineBmt(Boolean aktfkanPembayaranViaOnlineBmt) {
		this.aktfkanPembayaranViaOnlineBmt = aktfkanPembayaranViaOnlineBmt;
	}

	/**
	 * Override prefix invoice Online BMT untuk sekolah ini. Kosong berarti memakai
	 * konfigurasi global. Nilai tidak dinormalisasi di entity supaya perbedaan antara
	 * "belum diatur" dan nilai efektif tetap dapat dianalisis oleh resolver.
	 *
	 * @return prefix invoice, atau {@code null}/kosong bila mewarisi konfigurasi global
	 */
	@Column(name = "online_bmt_prefix_invoice", length = 8)
	public String getOnlineBmtPrefixInvoice() { return onlineBmtPrefixInvoice; }
	/** @param value prefix invoice Online BMT; {@code null}/kosong berarti mewarisi global */
	public void setOnlineBmtPrefixInvoice(String value) { this.onlineBmtPrefixInvoice = value; }

	/**
	 * Biaya admin sekolah; null mewarisi global, sedangkan 0 adalah override sah.
	 *
	 * @return biaya administrasi Online BMT khusus sekolah ini, atau {@code null} bila mewarisi
	 */
	@Column(name = "online_bmt_biaya_administrasi")
	public Double getOnlineBmtBiayaAdministrasi() { return onlineBmtBiayaAdministrasi; }
	/** @param value biaya administrasi Online BMT; {@code null} berarti mewarisi global, {@code 0} adalah override sah */
	public void setOnlineBmtBiayaAdministrasi(Double value) { this.onlineBmtBiayaAdministrasi = value; }

	/** @return kode mitra Online BMT sekolah ini; {@code null} berarti mewarisi konfigurasi global */
	@Column(name = "online_bmt_kode_mitra")
	public String getOnlineBmtKodeMitra() { return onlineBmtKodeMitra; }
	/** @param value kode mitra Online BMT */
	public void setOnlineBmtKodeMitra(String value) { this.onlineBmtKodeMitra = value; }

	/** @return nama mitra Online BMT sekolah ini; {@code null} berarti mewarisi konfigurasi global */
	@Column(name = "online_bmt_nama_mitra")
	public String getOnlineBmtNamaMitra() { return onlineBmtNamaMitra; }
	/** @param value nama mitra Online BMT */
	public void setOnlineBmtNamaMitra(String value) { this.onlineBmtNamaMitra = value; }

	/** @return kode merchant Online BMT sekolah ini; {@code null} berarti mewarisi konfigurasi global */
	@Column(name = "online_bmt_kode_merchant")
	public String getOnlineBmtKodeMerchant() { return onlineBmtKodeMerchant; }
	/** @param value kode merchant Online BMT */
	public void setOnlineBmtKodeMerchant(String value) { this.onlineBmtKodeMerchant = value; }

	/** @return nama merchant Online BMT sekolah ini; {@code null} berarti mewarisi konfigurasi global */
	@Column(name = "online_bmt_nama_merchant")
	public String getOnlineBmtNamaMerchant() { return onlineBmtNamaMerchant; }
	/** @param value nama merchant Online BMT */
	public void setOnlineBmtNamaMerchant(String value) { this.onlineBmtNamaMerchant = value; }

	/**
	 * Credential sekolah merupakan satu paket atomik. Ketiganya kosong berarti
	 * mewarisi credential global; override parsial ditolak pada form dan callback.
	 *
	 * @return API key Online BMT sekolah ini (teks biasa, ikut terekam Envers)
	 */
	@Column(name = "online_bmt_api_key", columnDefinition = "text")
	public String getOnlineBmtApiKey() { return onlineBmtApiKey; }
	/** @param value API key Online BMT; bagian dari paket credential yang harus diisi bersama */
	public void setOnlineBmtApiKey(String value) { this.onlineBmtApiKey = value; }

	/** @return kunci enkripsi Online BMT sekolah ini; bagian dari paket credential atomik */
	@Column(name = "online_bmt_encryption_key", columnDefinition = "text")
	public String getOnlineBmtEncryptionKey() { return onlineBmtEncryptionKey; }
	/** @param value kunci enkripsi Online BMT; bagian dari paket credential yang harus diisi bersama */
	public void setOnlineBmtEncryptionKey(String value) { this.onlineBmtEncryptionKey = value; }

	/** @return kunci HMAC Online BMT sekolah ini; bagian dari paket credential atomik */
	@Column(name = "online_bmt_hmac_key", columnDefinition = "text")
	public String getOnlineBmtHmacKey() { return onlineBmtHmacKey; }
	/** @param value kunci HMAC Online BMT; bagian dari paket credential yang harus diisi bersama */
	public void setOnlineBmtHmacKey(String value) { this.onlineBmtHmacKey = value; }

	/**
	 * Toleransi detik; null mewarisi global. Nilai efektif dibatasi 30 sampai 3600.
	 *
	 * @return toleransi selisih waktu permintaan Online BMT dalam detik, atau {@code null} bila
	 *         mewarisi konfigurasi global
	 */
	@Column(name = "online_bmt_request_time_tolerance")
	public Integer getOnlineBmtRequestTimeTolerance() { return onlineBmtRequestTimeTolerance; }
	/** @param value toleransi selisih waktu dalam detik; {@code null} berarti mewarisi global */
	public void setOnlineBmtRequestTimeTolerance(Integer value) { this.onlineBmtRequestTimeTolerance = value; }

	/**
	 * @return username akun Esmartlink milik sekolah ini; dapat {@code null} (tidak dinormalkan,
	 *         berbeda dengan getter kredensial BNI/BSI yang mengembalikan string kosong)
	 */
	public String getUsernameEsmartlink() {
		return usernameEsmartlink;
	}

	/**
	 * @param usernameEsmartlink username akun Esmartlink
	 */
	public void setUsernameEsmartlink(String usernameEsmartlink) {
		this.usernameEsmartlink = usernameEsmartlink;
	}

	/**
	 * Biaya administrasi bawaan transaksi Esmartlink.
	 *
	 * <p>Dipakai sebagai nilai cadangan bila jenis kanal yang dipilih tidak ditemukan pada peta
	 * {@link #getVariableBiayaAdminEsmartlink()}.</p>
	 *
	 * @return biaya administrasi; {@code null} dinormalkan menjadi {@code 0}
	 */
	public Double getBiayaAdminEsmartlink() {
		return biayaAdminEsmartlink == null ? 0 : biayaAdminEsmartlink;
	}

	/**
	 * @param biayaAdminEsmartlink biaya administrasi bawaan Esmartlink
	 */
	public void setBiayaAdminEsmartlink(Double biayaAdminEsmartlink) {
		this.biayaAdminEsmartlink = biayaAdminEsmartlink;
	}

	/**
	 * @return password akun Esmartlink milik sekolah ini (teks biasa, ikut terekam Envers); dapat
	 *         {@code null}
	 */
	public String getPasswordEsmartlink() {
		return passwordEsmartlink;
	}

	/**
	 * @param passwordEsmartlink password akun Esmartlink
	 */
	public void setPasswordEsmartlink(String passwordEsmartlink) {
		this.passwordEsmartlink = passwordEsmartlink;
	}

	/**
	 * Pendaftar (calon pelanggan) yang menjadi asal-usul sekolah ini.
	 *
	 * <p><b>Penting:</b> getter ini adalah <b>pemicu</b> seluruh perilaku write-back di kelas ini.
	 * {@link #getAlamat()}, {@link #getEmail()}, {@link #getNama()}, {@link #getNss()},
	 * {@link #getNpsn()}, {@link #getTelp()}, {@link #getMotto()}, dan {@link #getDomain()}
	 * memanggilnya lebih dulu, dan bila hasilnya bukan {@code null} mereka menyalin nilai dari
	 * Pendaftar ke field masing-masing. Melepas relasi ini (menyetel {@code null}) akan
	 * menghentikan seluruh perilaku salin tersebut.</p>
	 *
	 * @return pendaftar asal, atau {@code null} bila sekolah dibuat langsung tanpa proses
	 *         pendaftaran layanan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar")
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	/**
	 * @param pendaftar pendaftar asal sekolah ini
	 */
	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/**
	 * Secret ID BSI milik sekolah ini (ejaan properti "Scret" adalah salah ketik asli yang sudah
	 * menjadi nama kolom — jangan diubah).
	 *
	 * @return secret id BSI sudah di-{@code trim}; string kosong bila belum diatur
	 */
	public String getBsiScretId() {
		return bsiScretId == null ? "" : bsiScretId.trim();
	}

	/**
	 * @param bsiScretId secret id BSI
	 */
	public void setBsiScretId(String bsiScretId) {
		this.bsiScretId = bsiScretId;
	}

	/**
	 * @return username akun BSI milik sekolah ini sudah di-{@code trim}; string kosong bila belum
	 *         diatur
	 */
	public String getBsiUsername() {
		return bsiUsername == null ? "" : bsiUsername.trim();
	}

	/**
	 * @param bsiUsername username akun BSI
	 */
	public void setBsiUsername(String bsiUsername) {
		this.bsiUsername = bsiUsername;
	}

	/**
	 * Tabel biaya administrasi Esmartlink per jenis kanal pembayaran.
	 *
	 * <p>Formatnya adalah daftar dipisah <code>";"</code> berisi tiga bagian dipisah
	 * <code>":"</code> — <code>KODE:BIAYA:LABEL</code>, misalnya
	 * <code>VA_BNI:2500:BNI</code>. Bila kolom kosong, dikembalikan daftar bawaan yang cukup
	 * panjang (VA BNI/BRI/BCA/BNC/CIMB/Mandiri/Permata/BSI/Danamon serta OTC Alfamart/Indomaret).</p>
	 *
	 * <p><b>Kasus tepi:</b> nilai bawaan itu <b>tidak</b> ditulis ke database — ia hanya muncul
	 * saat pembacaan. Menaikkan biaya bawaan di kode akan langsung mengubah biaya seluruh sekolah
	 * yang belum pernah menyunting kolom ini.</p>
	 *
	 * @return daftar <code>KODE:BIAYA:LABEL</code> dipisah <code>";"</code>; tidak pernah kosong
	 */
	@Column(columnDefinition = "text")
	public String getVariableBiayaAdminEsmartlink() {
		return variableBiayaAdminEsmartlink == null
				? "VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart"
				: variableBiayaAdminEsmartlink.trim();
	}

	/**
	 * @param variableBiayaAdminEsmartlink daftar biaya per kanal berformat
	 *                                     <code>KODE:BIAYA:LABEL;…</code>; format tidak divalidasi
	 *                                     di entity
	 */
	public void setVariableBiayaAdminEsmartlink(String variableBiayaAdminEsmartlink) {
		this.variableBiayaAdminEsmartlink = variableBiayaAdminEsmartlink;
	}

	/**
	 * Potongan HTML kop/header khusus halaman PPDB (PSB) sekolah ini (kolom bertipe {@code text}).
	 *
	 * <p><b>Catatan keamanan:</b> seperti {@link #getCss()}, isinya dirender ke halaman publik apa
	 * adanya sehingga merupakan permukaan penyisipan HTML/skrip bagi siapa pun yang dapat
	 * menyunting master Sekolah.</p>
	 *
	 * @return potongan HTML header PPDB; dapat {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getHeaderppdb() {
		return headerppdb;
	}

	/**
	 * @param headerppdb potongan HTML header halaman PPDB
	 */
	public void setHeaderppdb(String headerppdb) {
		this.headerppdb = headerppdb;
	}

	/**
	 * Kebijakan lintas-tenant: apakah siswa sekolah ini boleh masuk lewat portal
	 * {@link ais.database.model.sekolah.Yayasan yayasan} induk, bukan hanya lewat portal sekolah.
	 *
	 * @return {@code true} bila diizinkan; {@code null} dinormalkan menjadi {@code false}
	 *         (default tertutup — perilaku yang benar untuk sakelar yang memperluas akses)
	 */
	public Boolean getSiswaDiizinkanDiPortalYayasan() {
		return siswaDiizinkanDiPortalYayasan == null ? false : siswaDiizinkanDiPortalYayasan;
	}

	/**
	 * @param siswaDiizinkanDiPortalYayasan kebijakan akses siswa lewat portal yayasan
	 */
	public void setSiswaDiizinkanDiPortalYayasan(Boolean siswaDiizinkanDiPortalYayasan) {
		this.siswaDiizinkanDiPortalYayasan = siswaDiizinkanDiPortalYayasan;
	}

	/**
	 * Kanal pembayaran bawaan sekolah ini (paket konfigurasi kanal yang dapat dipakai bersama).
	 *
	 * <p>Layar master mengisi combobox untuk properti ini dengan pembatas
	 * <code>sekolah IS NULL OR sekolah.id = &lt;id sekolah aktif&gt;</code>, dan — berbeda dengan
	 * kebanyakan tempat lain di codebase — memakai <code>sqlRestriction("false")</code> ketika
	 * sekolah tidak teridentifikasi. Ini adalah salah satu contoh <b>fail-closed</b> yang benar;
	 * lihat pembahasan pola fail-open pada javadoc kelas.</p>
	 *
	 * @return kanal pembayaran bawaan, atau {@code null} bila mengikuti kanal default instalasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kanal_pembayaran")
	public KanalPembayaran getKanalPembayaran() {
		kanalPembayaran = check(kanalPembayaran);
		return kanalPembayaran;
	}

	/**
	 * @param kanalPembayaran kanal pembayaran bawaan sekolah ini
	 */
	public void setKanalPembayaran(KanalPembayaran kanalPembayaran) {
		this.kanalPembayaran = kanalPembayaran;
	}

	/**
	 * Menyuntikkan jalur berkas gambar identitas sekolah (kop atas, kop bawah, stempel, logo) ke
	 * dalam peta parameter laporan JasperReports.
	 *
	 * <p><b>Tujuan.</b> Template JRXML merujuk gambar lewat parameter bernama; method ini yang
	 * mengisi nama-nama parameter itu dengan <i>absolute path</i> berkas yang benar-benar ada di
	 * server. Dipanggil dari {@code SuratUtil} (pembuatan surat/laporan) dan
	 * {@code ManajemenProperty}, berdampingan dengan method senama pada
	 * {@link ais.database.model.sekolah.Yayasan}, {@code PerguruanTinggi}, {@code Fakultas}, dan
	 * {@code Jurusan} — satu laporan bisa menerima gambar dari beberapa tingkat hierarki
	 * sekaligus.</p>
	 *
	 * <p><b>Sumber berkas dan urutan pencarian.</b> Untuk setiap jenis gambar, dicoba dua sumber
	 * secara berurutan:</p>
	 * <ol>
	 *   <li>berkas langsung di folder unggahan lewat
	 *       {@code FileFoto.fileAdaDiFolder(jenis, idSekolah)};</li>
	 *   <li>bila berkas itu tidak ada atau tidak lolos {@code Common.isGambarLaporanValid(...)},
	 *       lampiran dari basis data lewat
	 *       {@code LampiranLain.ambil(false, idSekolah, jenis)} lalu {@code kop.ambilFile()}.</li>
	 * </ol>
	 *
	 * <p><b>Nama parameter yang ditulis.</b> Tiap gambar didaftarkan dengan <i>beberapa</i> kunci
	 * agar template lama maupun baru sama-sama menemukannya:</p>
	 * <ul>
	 *   <li>bersufiks id — <code>KOP_SEKOLAH_&lt;id&gt;</code>,
	 *       <code>KOP_BAWAH_SEKOLAH_&lt;id&gt;</code>,
	 *       <code>STEMPEL_SEKOLAH_&lt;id&gt;</code>, <code>LOGO_SEKOLAH_&lt;id&gt;</code>;</li>
	 *   <li>bersufiks <b>nama sekolah</b> — <code>KOP_SEKOLAH_&lt;nama&gt;</code> dan seterusnya.
	 *       <b>Kasus tepi:</b> karena kuncinya memakai nama, dua sekolah bernama sama dalam satu
	 *       laporan gabungan akan saling menimpa parameternya;</li>
	 *   <li>tanpa sufiks — <code>KOP_SEKOLAH</code>, <code>KOP_BAWAH_SEKOLAH</code>,
	 *       <code>LOGO_SEKOLAH</code>, <code>STEMPEL_SEKOLAH</code>. Kunci polos inilah yang
	 *       dipakai template satu-sekolah; bila {@code putFile} dipanggil untuk lebih dari satu
	 *       sekolah pada peta yang sama, <b>sekolah terakhir menang</b>.</li>
	 * </ul>
	 * <p>Perhatikan bahwa <code>STEMPEL_SEKOLAH</code> polos ditulis di blok pertama (bersamaan
	 * dengan varian bersufiks), sementara <code>KOP_SEKOLAH</code>, <code>KOP_BAWAH_SEKOLAH</code>,
	 * dan <code>LOGO_SEKOLAH</code> polos ditulis di blok terpisah di bagian akhir method —
	 * itulah sebabnya pencarian berkas untuk ketiganya <b>dilakukan dua kali</b> (asimetri yang
	 * disengaja pada kode asli, bukan salah baca).</p>
	 *
	 * <p><b>Efek samping.</b> Method <b>tidak</b> menyalin/menulis berkas; ia hanya menaruh jalur
	 * ke dalam {@code parameters}. Namun {@code LampiranLain.ambil(...)} dan
	 * {@code ambilFile()} dapat mengakses basis data serta mengekstrak lampiran ke berkas
	 * sementara. Kunci yang sudah ada di peta akan ditimpa; kunci untuk gambar yang tidak
	 * ditemukan <b>tidak</b> ditulis sama sekali (template harus tahan terhadap parameter yang
	 * hilang).</p>
	 *
	 * <p><b>Kasus tepi lain:</b> tidak ada penjagaan {@code null} atas {@code parameters} maupun
	 * atas {@link #getId()}. Bila dipanggil pada objek Sekolah kosong hasil fallback resolver
	 * tenant, kunci yang terbentuk akan berbentuk <code>KOP_SEKOLAH_null</code> dan pencarian
	 * berkas pasti gagal — laporan tercetak tanpa kop, tanpa pesan kesalahan.</p>
	 *
	 * @param parameters peta parameter JasperReports yang akan diisi (dimodifikasi di tempat);
	 *                   tipe mentah {@code Map} mengikuti API Jasper yang dipakai
	 * @see ais.database.model.file.LampiranLain
	 * @see ais.database.model.file.FileFoto
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putFile(Map parameters) {
		Sekolah sekolah = this;

		File file = FileFoto.fileAdaDiFolder(LampiranLain.KOP_SEKOLAH, sekolah.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("KOP_SEKOLAH_" + sekolah.getId(), file.getAbsolutePath());
			parameters.put("KOP_SEKOLAH_" + sekolah.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, sekolah.getId(), LampiranLain.KOP_SEKOLAH);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("KOP_SEKOLAH_" + sekolah.getId(), fileKop.getAbsolutePath());
					parameters.put("KOP_SEKOLAH_" + sekolah.getNama(), fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.KOP_BAWAH_SEKOLAH, sekolah.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("KOP_BAWAH_SEKOLAH_" + sekolah.getId(), file.getAbsolutePath());
			parameters.put("KOP_BAWAH_SEKOLAH_" + sekolah.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, sekolah.getId(), LampiranLain.KOP_BAWAH_SEKOLAH);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("KOP_BAWAH_SEKOLAH_" + sekolah.getId(), fileKop.getAbsolutePath());
					parameters.put("KOP_BAWAH_SEKOLAH_" + sekolah.getNama(), fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.STEMPEL_SEKOLAH, sekolah.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("STEMPEL_SEKOLAH", file.getAbsolutePath());
			parameters.put("STEMPEL_SEKOLAH_" + sekolah.getId(), file.getAbsolutePath());
			parameters.put("STEMPEL_SEKOLAH_" + sekolah.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, sekolah.getId(), LampiranLain.STEMPEL_SEKOLAH);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("STEMPEL_SEKOLAH", fileKop.getAbsolutePath());
					parameters.put("STEMPEL_SEKOLAH_" + sekolah.getId(), fileKop.getAbsolutePath());
					parameters.put("STEMPEL_SEKOLAH_" + sekolah.getNama(), fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.LOGO_SEKOLAH, sekolah.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("LOGO_SEKOLAH_" + sekolah.getId(), file.getAbsolutePath());
			parameters.put("LOGO_SEKOLAH_" + sekolah.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, sekolah.getId(), LampiranLain.LOGO_SEKOLAH);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("LOGO_SEKOLAH_" + sekolah.getId(), fileKop.getAbsolutePath());
					parameters.put("LOGO_SEKOLAH_" + sekolah.getNama(), fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.KOP_SEKOLAH, sekolah.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("KOP_SEKOLAH", file.getAbsolutePath());
		} else {

			LampiranLain kop = LampiranLain.ambil(false, sekolah.getId(), LampiranLain.KOP_SEKOLAH);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("KOP_SEKOLAH", fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.KOP_BAWAH_SEKOLAH, sekolah.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("KOP_BAWAH_SEKOLAH", file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, sekolah.getId(), LampiranLain.KOP_BAWAH_SEKOLAH);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("KOP_BAWAH_SEKOLAH", fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.LOGO_SEKOLAH, sekolah.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("LOGO_SEKOLAH", file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, sekolah.getId(), LampiranLain.LOGO_SEKOLAH);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("LOGO_SEKOLAH", fileKop.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Sakelar kanal pembayaran BJB Syariah untuk sekolah ini.
	 *
	 * @return {@code true} bila kanal BJB Syariah aktif; {@code null} dinormalkan menjadi
	 *         {@code false}
	 */
	public Boolean getAktfkanBjbSyariah() {
		return aktfkanBjbSyariah == null ? false : aktfkanBjbSyariah;
	}

	/**
	 * @param aktfkanBjbSyariah sakelar kanal BJB Syariah
	 */
	public void setAktfkanBjbSyariah(Boolean aktfkanBjbSyariah) {
		this.aktfkanBjbSyariah = aktfkanBjbSyariah;
	}

	/**
	 * @return biaya administrasi transaksi BJB Syariah; {@code null} dinormalkan menjadi
	 *         {@code 0.0}
	 */
	public Double getBiayaAdminBjbSyariah() {
		return biayaAdminBjbSyariah == null ? 0.0 : biayaAdminBjbSyariah;
	}

	/**
	 * @param biayaAdminBjbSyariah biaya administrasi transaksi BJB Syariah
	 */
	public void setBiayaAdminBjbSyariah(Double biayaAdminBjbSyariah) {
		this.biayaAdminBjbSyariah = biayaAdminBjbSyariah;
	}
}
