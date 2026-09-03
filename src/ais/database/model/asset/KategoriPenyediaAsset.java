package ais.database.model.asset;

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

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Master data <b>kategori penyedia asset</b> (tabel {@code asset.kategori_penyedia_asset}) —
 * klasifikasi <i>peran vendor dalam rantai pasok</i> bagi {@link PenyediaAsset}, yaitu
 * vendor/penyedia barang dan jasa yang dipakai modul pengadaan aset tetap.
 *
 * <h2>Peran dalam klaster vendor</h2>
 * <p>Entitas ini adalah satu dari tiga sumbu klasifikasi vendor, dan ketiganya sering tertukar
 * karena namanya mirip:</p>
 * <ul>
 *   <li>{@link JenisPenyediaAsset} — bentuk badan usaha ("Perusahaan Swasta Umum", CV, koperasi,
 *       perorangan);</li>
 *   <li><b>{@code KategoriPenyediaAsset}</b> (kelas ini) — peran vendor dalam rantai pasok. Nilai
 *       bawaan yang disemai adalah {@code "Pedagang Langsung"}, dan operator dapat menambah baris
 *       lain seperti distributor, agen tunggal, pabrikan, atau penyedia jasa lewat
 *       {@code KategoriPenyediaAssetAction};</li>
 *   <li>{@link StatusPenyediaAsset} — kedudukan organisatoris ("Pusat"/"Cabang").</li>
 * </ul>
 *
 * <h2>Sifat: klasifikasi deskriptif, bukan aturan</h2>
 * <p>Seperti dua saudaranya, kategori penyedia <b>tidak menjadi syarat transaksi</b>. Seluruh
 * pemakaiannya di basis kode bersifat tampilan, pencarian, dan pelaporan:</p>
 * <ul>
 *   <li>{@code PenyediaAssetAction} menampilkannya sebagai kolom daftar vendor dan menyediakan
 *       combo penyaring pencarian;</li>
 *   <li>{@code AmbilDataPenyediaAssetBanbox} — pemilih vendor yang dipakai layar-layar pengadaan —
 *       menampilkan kolom kategori pada grid pilihannya dan mengizinkan penyaringan teks atasnya,
 *       tetapi tidak pernah menolak vendor berdasarkan kategori;</li>
 *   <li>{@code DasboardVendor} memakainya sebagai sumbu pengelompokan statistik
 *       ({@code select k.nama, count(p.id) … group by k.nama}) dan menghitung
 *       {@code count(distinct p.kategoriPenyediaAsset.id)};</li>
 *   <li>{@code CommonReportHelper} mencetak namanya ke laporan.</li>
 * </ul>
 * <p>Tidak ada aturan bisnis yang diturunkan dari kolom ini — tidak ada, misalnya, kewajiban
 * dokumen legal yang berbeda antara pabrikan dan pedagang, atau pembatasan cara pengadaan menurut
 * kategori. Bila kebijakan semacam itu diperlukan, ia harus ditambahkan di lapisan {@code Action}
 * pengadaan; jangan mengasumsikan penegakannya sudah tersedia di sini.</p>
 *
 * <h2>Struktur dan pemuatan</h2>
 * <p>Kelas mengikuti cetakan master data ringan AIS: kunci utama {@code IDENTITY}, trio
 * {@code kode}/{@code nama}/{@code keterangan}, satu flag {@link #getAktif()}, dan bidang audit
 * bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang wajib ada agar
 * {@code AuditTimestampInterceptor} dapat mengisinya lewat {@link #onUpdate()}. Entitas dianotasi
 * {@link Audited} sehingga riwayat perubahannya terekam Envers, serta memakai
 * {@code dynamicInsert}/{@code dynamicUpdate}.</p>
 * <p>{@link #reloadDefault()} dipanggil {@code InitData} saat aplikasi dimulai, dan kelas ini
 * termasuk daftar kelas yang di-cache {@code ConstantValues} sehingga resolusi lewat
 * {@code check(...)} pada {@code PenyediaAsset.getKategoriPenyediaAsset()} umumnya dilayani dari
 * memori.</p>
 *
 * @see PenyediaAsset
 * @see JenisPenyediaAsset
 * @see StatusPenyediaAsset
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "kategori_penyedia_asset")
public class KategoriPenyediaAsset extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai warisan cetakan hbm2java yang dipakai bersama hampir seluruh entitas master data
	 * paket ini. Kesamaan nilai antar kelas tidak berbahaya karena verifikasi dilakukan per kelas,
	 * namun jangan mengubahnya tanpa alasan sebab objek entitas dapat ikut diserialisasi bersama
	 * sesi/desktop ZK.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan. Ini disengaja: bidang audit bayangan hanya
	 * boleh diisi {@code AuditTimestampInterceptor}, dan penulisan kosong dari jalur salin/klon
	 * objek tidak boleh menghapus jejak audit yang sudah ada.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan agar jejak
	 * audit tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui.
	 *
	 * <p>Interceptor-lah yang mengisi {@link #oleh}, {@link #olehId}, dan
	 * {@link #getTanggal_dirubah()} dari konteks pengguna aktif. Method sengaja {@code protected}
	 * dan tidak boleh dipanggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan diperbarui interceptor audit. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor}, bukan oleh form.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berformat {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai untuk penelusuran log dan sebagai label bawaan sejumlah komponen ZK. Method ini
	 * membaca bidang {@link #nama} langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * melakukan {@code trim()}.</p>
	 *
	 * @return gabungan id dan nama; bagian id bernilai {@code "null"} bila baris belum tersimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Baris rujukan berbagi untuk kategori bawaan <b>"Pedagang Langsung"</b>.
	 *
	 * <p>Dipakai {@code PenyediaAsset.getKategoriPenyediaAsset()} sebagai nilai bawaan: setiap
	 * vendor yang sudah tersimpan ({@code getId() != null}) namun kolom kategorinya masih
	 * {@code null} akan dianggap berkategori ini. Karena itu kolom kategori pada praktiknya tidak
	 * pernah tampil kosong di layar, walaupun di basis data nilainya bisa saja {@code null}.</p>
	 * <p><b>Peringatan siklus hidup:</b> bidang statis ini menyimpan instance entitas Hibernate
	 * yang ditebus (atau dibuat) sekali oleh {@link #reloadDefault()} pada sesi yang segera
	 * ditutup, sehingga objeknya <b>terlepas</b> dari sesi. Jangan menyuntingnya atau menyimpannya
	 * kembali lewat referensi statis ini, dan jangan mengandalkan pemuatan relasi malas darinya.
	 * Bidang bernilai {@code null} sampai {@link #reloadDefault()} berhasil, dan tetap {@code null}
	 * bila penyemaian gagal — pemanggil harus siap menghadapi {@code null}.</p>
	 */
	public static KategoriPenyediaAsset PEDAGANG_LANGSUNG = null;

	/**
	 * Menjamin baris kategori bawaan "Pedagang Langsung" tersedia di tabel
	 * {@code asset.kategori_penyedia_asset}, lalu menyimpan rujukannya pada konstanta statis
	 * {@link #PEDAGANG_LANGSUNG}.
	 *
	 * <p><b>Kapan dipanggil.</b> Method ini bagian dari rangkaian penyemaian master data yang
	 * dijalankan {@code InitData} pada saat aplikasi dimulai. Ia dirancang idempoten: pemanggilan
	 * berulang hanya menebus baris yang sudah ada tanpa membuat duplikat, sehingga aman dipanggil
	 * setiap kali aplikasi dijalankan ulang.</p>
	 *
	 * <p><b>Cara kerja.</b> Method membuka sesi Hibernate native lewat
	 * {@code HibernateUtil.currentNativeSession()}, lalu menjalankan pola cari-atau-buat:</p>
	 * <ol>
	 *   <li>Mencari baris dengan {@code Restrictions.ilike("nama", "Pedagang Langsung",
	 *       MatchMode.ANYWHERE)}. Sama seperti {@link JenisPenyediaAsset#reloadDefault()} — dan
	 *       berbeda dari {@link StatusPenyediaAsset#reloadDefault()} yang memakai {@code EXACT} —
	 *       pencocokan di sini longgar. Sisi baiknya, mengganti nama baris bawaan menjadi mis.
	 *       "Pedagang Langsung / Retail" tetap membuatnya dikenali pada penyemaian berikutnya
	 *       sehingga tidak muncul duplikat. Sisi buruknya, baris lain yang memuat frasa tersebut
	 *       dapat "membajak" konstanta {@link #PEDAGANG_LANGSUNG}; karena hasil dibatasi satu baris
	 *       tanpa pengurutan eksplisit, baris mana yang terpilih bergantung pada urutan yang
	 *       dikembalikan basis data dan tidak dijamin stabil antar-restart;</li>
	 *   <li>{@code setMaxResults(1).uniqueResult()} membatasi hasil ke satu baris. Pembatasan ini
	 *       penting karena tabel tidak memiliki batasan unik pada kolom {@code nama}: tanpa
	 *       pembatasan tersebut, adanya lebih dari satu baris yang cocok akan membuat
	 *       {@code uniqueResult()} melempar {@code NonUniqueResultException} dan menggagalkan
	 *       penyemaian. Dengan pembatasan ini, baris duplikat hanya tertutupi secara diam-diam —
	 *       duplikatnya tetap ada di tabel dan tetap dapat dirujuk vendor lain, sehingga
	 *       pengelompokan pada {@code DasboardVendor} maupun laporan bisa memperlihatkan dua
	 *       kelompok yang secara bisnis sama;</li>
	 *   <li>Bila hasilnya {@code null}, dibuat instance baru dengan {@code nama} dan
	 *       {@code keterangan} berisi "Pedagang Langsung", lalu disimpan di dalam transaksi
	 *       tersendiri ({@code begin()}/{@code save()}/{@code commit()}). Perhatikan bahwa
	 *       {@code kode} <b>tidak</b> diisi — berbeda dari {@link JenisPenyediaAsset} yang menyemai
	 *       {@code kode = "001"} — sehingga {@link #getKode()} baris bawaan kelas ini akan
	 *       mengembalikan string kosong;</li>
	 *   <li>Instance hasil temu atau hasil buat disimpan ke {@link #PEDAGANG_LANGSUNG}.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan galat.</b> Seluruh badan method dibungkus {@code try/catch} yang menelan
	 * setiap {@code Exception}, mencetak jejak tumpukan, dan mencatatkannya ke
	 * {@code ErrorAuditUtil}. Method <b>tidak pernah melempar</b> ke pemanggil — disengaja agar
	 * satu master data yang gagal disemai tidak menggagalkan seluruh inisialisasi aplikasi.
	 * Konsekuensinya kegagalan bersifat senyap bagi pemanggil: {@link #PEDAGANG_LANGSUNG} akan
	 * tetap {@code null} dan hanya jejak di log yang menandai masalah. Tidak ada {@code rollback()}
	 * eksplisit di blok {@code catch}; pembersihan diserahkan ke
	 * {@code HibernateUtil.closeSession()} pada blok {@code finally}, yang selalu dijalankan
	 * termasuk pada jalur sukses.</p>
	 *
	 * <p><b>Interaksi dengan getter bawaan pada {@link PenyediaAsset}.</b>
	 * {@code PenyediaAsset.getKategoriPenyediaAsset()} menyetel bidang instansnya sendiri ke
	 * {@link #PEDAGANG_LANGSUNG} ketika vendor sudah tersimpan namun kategorinya {@code null} —
	 * yaitu getter yang memutasi state (pola "getter destruktif" yang berulang di basis kode ini).
	 * Bila objek vendor tersebut kemudian ikut ter-<i>flush</i> Hibernate, nilai bawaan itu dapat
	 * ikut tertulis ke basis data tanpa tindakan eksplisit dari pengguna. Karena
	 * {@link #PEDAGANG_LANGSUNG} adalah instance terlepas, penulisan seperti itu bergantung pada
	 * keberhasilan {@code check(...)}/cache {@code ConstantValues} untuk memetakannya kembali ke
	 * baris yang benar. Akibat lanjutan yang perlu disadari saat membaca laporan: kategori
	 * "Pedagang Langsung" akan tampak mendominasi populasi vendor bukan karena data lapangan
	 * memang demikian, melainkan karena ia nilai bawaan bagi setiap vendor yang kategorinya belum
	 * pernah diisi.</p>
	 *
	 * <p><b>Catatan bagi penyunting.</b> Jangan menghapus baris bawaan lewat layar CRUD tanpa
	 * memindahkan vendor yang menunjuknya; penghapusan akan meninggalkan rujukan menggantung, dan
	 * penyemaian berikutnya membuat baris baru dengan id berbeda sehingga riwayat pelaporan
	 * terpecah.</p>
	 *
	 * @see PenyediaAsset#getKategoriPenyediaAsset()
	 * @see JenisPenyediaAsset#reloadDefault()
	 * @see StatusPenyediaAsset#reloadDefault()
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		try {
			PEDAGANG_LANGSUNG = (KategoriPenyediaAsset) session.createCriteria(KategoriPenyediaAsset.class)

					.add(Restrictions.ilike("nama", "Pedagang Langsung", MatchMode.ANYWHERE))

					.setMaxResults(1).uniqueResult();
			if (PEDAGANG_LANGSUNG == null) {
				PEDAGANG_LANGSUNG = new KategoriPenyediaAsset();
				PEDAGANG_LANGSUNG.setNama("Pedagang Langsung");
				PEDAGANG_LANGSUNG.setKeterangan("Pedagang Langsung");
				session.getTransaction().begin();
				session.save(PEDAGANG_LANGSUNG);
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/asset/KategoriPenyediaAsset.java:96");
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/** Kode ringkas kategori; tidak disemai oleh {@link #reloadDefault()} sehingga kosong untuk baris bawaan. */
	private String kode;

	/** Nama kategori yang ditampilkan; menjadi kunci pencocokan longgar pada {@link #reloadDefault()}. */
	private String nama;
	/** Keterangan bebas; disemai sama dengan {@link #nama} untuk baris bawaan. */
	private String keterangan;
	/** Flag aktif master data; lihat {@link #getAktif()} mengenai keterbatasan penegakannya. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Dipakai pula oleh {@link #reloadDefault()} dan oleh form CRUD
	 * {@code KategoriPenyediaAssetAction} saat membuat baris baru.</p>
	 */
	public KategoriPenyediaAsset() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah disimpan
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
	 * <p>Hanya untuk kebutuhan Hibernate dan penyalinan objek; jangan menyetel id secara manual
	 * pada baris yang akan disimpan sebagai data baru.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode kategori dalam bentuk yang aman untuk ditampilkan.
	 *
	 * <p>Mengganti {@code null} dengan string kosong dan memangkas spasi tepi sehingga pemanggil
	 * UI tidak perlu memeriksa {@code null}. Untuk baris bawaan yang disemai
	 * {@link #reloadDefault()}, method ini selalu mengembalikan string kosong karena kolom
	 * {@code kode} memang tidak diisi saat penyemaian.</p>
	 *
	 * @return kode yang sudah dipangkas, atau string kosong bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode kategori.
	 *
	 * <p>Tidak ada penjaminan keunikan pada tingkat entitas maupun basis data; keunikan kode
	 * sepenuhnya bergantung pada disiplin operator dan validasi di layar CRUD.</p>
	 *
	 * @param kode kode ringkas; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama kategori setelah dipangkas spasi tepinya.
	 *
	 * <p>Berbeda dari {@link #getKode()}, method ini <b>mempertahankan</b> {@code null} apa adanya.
	 * Pemanggil UI karena itu tetap perlu berjaga terhadap {@code null} walau kolomnya
	 * dideklarasikan {@code nullable = false} pada tingkat basis data.</p>
	 *
	 * @return nama kategori yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori.
	 *
	 * <p>Perhatikan bahwa {@link #reloadDefault()} mencocokkan baris bawaan berdasarkan nama
	 * dengan {@code MatchMode.ANYWHERE}: mengganti nama baris menjadi teks yang tetap memuat frasa
	 * "Pedagang Langsung" akan membuat baris tersebut tetap dianggap sebagai baris bawaan pada
	 * penyemaian berikutnya.</p>
	 *
	 * @param nama nama kategori
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk kategori ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi (tidak dinormalkan menjadi string
	 *         kosong)
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas untuk kategori ini.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan flag aktif baris master data ini, dengan {@code null} diperlakukan sebagai
	 * {@code true}.
	 *
	 * <p><b>Default membuka, bukan menutup.</b> Baris lama yang kolom {@code aktif}-nya masih
	 * {@code null} — termasuk baris bawaan yang disemai {@link #reloadDefault()}, yang memang tidak
	 * menyetelnya — akan dianggap aktif. Pola {@code null == true} ini konsisten di seluruh master
	 * data AIS.</p>
	 * <p><b>Penegakan lemah.</b> Menonaktifkan sebuah baris kategori <b>tidak</b> memutus atau
	 * menandai vendor yang sudah terlanjur menunjuknya: vendor tersebut tetap dapat dipilih di
	 * {@code AmbilDataPenyediaAssetBanbox} (yang hanya menyaring flag {@code aktif} milik vendor,
	 * bukan milik kategorinya), namanya tetap tercetak di laporan, dan tetap ikut terhitung pada
	 * pengelompokan {@code DasboardVendor}. Efek penonaktifan terbatas pada penyaringan daftar
	 * pilihan di layar CRUD yang bersangkutan.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} bila dinonaktifkan
	 *         secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel flag aktif baris master data ini.
	 *
	 * <p>Menyetel {@code null} mengembalikan perilaku bawaan {@link #getAktif()} menjadi
	 * {@code true}.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menonaktifkan,
	 *              {@code null} untuk mengembalikan ke bawaan (dianggap aktif)
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
