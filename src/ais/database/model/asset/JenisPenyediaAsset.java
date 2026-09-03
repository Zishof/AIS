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
 * Master data <b>jenis penyedia asset</b> (tabel {@code asset.jenis_penyedia_asset}) — klasifikasi
 * <i>bentuk badan usaha</i> bagi {@link PenyediaAsset}, yaitu vendor/penyedia barang dan jasa yang
 * dipakai modul pengadaan aset tetap.
 *
 * <h2>Peran dalam klaster vendor</h2>
 * <p>Bersama {@link KategoriPenyediaAsset} (peran vendor dalam rantai pasok, mis. "Pedagang
 * Langsung") dan {@link StatusPenyediaAsset} (kedudukan organisatoris, "Pusat"/"Cabang"), entitas
 * ini membentuk tiga sumbu klasifikasi vendor. Perbedaan maksudnya:</p>
 * <ul>
 *   <li><b>Jenis</b> (kelas ini) menjawab "vendor ini berbadan hukum apa?" — nilai bawaan yang
 *       disemai adalah {@code "Perusahaan Swasta Umum"}, dan operator dapat menambah baris lain
 *       seperti CV, Firma, Koperasi, BUMN, atau perorangan lewat
 *       {@code JenisPenyediaAssetAction};</li>
 *   <li><b>Kategori</b> menjawab "vendor ini berperan sebagai apa?";</li>
 *   <li><b>Status</b> menjawab "kantor pusat atau cabang?".</li>
 * </ul>
 *
 * <h2>Sifat: klasifikasi deskriptif, bukan aturan</h2>
 * <p>Sama seperti dua saudaranya, jenis penyedia <b>tidak menjadi syarat transaksi</b>. Seluruh
 * pemakaiannya di basis kode bersifat tampilan, pencarian, dan pelaporan:</p>
 * <ul>
 *   <li>{@code PenyediaAssetAction} menampilkannya sebagai kolom daftar vendor dan menyediakan
 *       combo penyaring pencarian;</li>
 *   <li>{@code AmbilDataPenyediaAssetBanbox} — pemilih vendor yang dipakai layar-layar pengadaan —
 *       menampilkan kolom jenis pada grid pilihannya dan mengizinkan penyaringan teks atasnya,
 *       tetapi tidak pernah menolak vendor berdasarkan jenis;</li>
 *   <li>{@code DasboardVendor} memakainya sebagai sumbu pengelompokan statistik
 *       ({@code select j.nama, count(p.id) … group by j.nama}) dan menghitung
 *       {@code count(distinct p.jenisPenyediaAsset.id)};</li>
 *   <li>{@code CommonReportHelper} mencetak namanya ke laporan;</li>
 *   <li>{@code ParameterTambahanAstract} merujuk kelas ini sebagai salah satu tipe sumber data
 *       yang boleh dipakai parameter tambahan dinamis.</li>
 * </ul>
 * <p>Tidak ada aturan bisnis — mis. batas nilai pengadaan menurut bentuk badan usaha, atau
 * kewajiban dokumen yang berbeda antara perorangan dan perseroan — yang diturunkan dari kolom ini.
 * Bila kebijakan seperti itu dibutuhkan, ia harus ditambahkan di lapisan {@code Action} pengadaan,
 * bukan diasumsikan sudah ada di sini.</p>
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
 * {@code check(...)} pada {@code PenyediaAsset.getJenisPenyediaAsset()} umumnya dilayani dari
 * memori.</p>
 *
 * @see PenyediaAsset
 * @see KategoriPenyediaAsset
 * @see StatusPenyediaAsset
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_penyedia_asset")
public class JenisPenyediaAsset extends GeneralValueObject {

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
	 * Baris rujukan berbagi untuk jenis bawaan <b>"Perusahaan Swasta Umum"</b>.
	 *
	 * <p>Dipakai {@code PenyediaAsset.getJenisPenyediaAsset()} sebagai nilai bawaan: setiap vendor
	 * yang sudah tersimpan ({@code getId() != null}) namun kolom jenisnya masih {@code null} akan
	 * dianggap berjenis ini. Karena itu kolom jenis pada praktiknya tidak pernah tampil kosong di
	 * layar, walaupun di basis data nilainya bisa saja {@code null}.</p>
	 * <p><b>Peringatan siklus hidup:</b> bidang statis ini menyimpan instance entitas Hibernate
	 * yang ditebus (atau dibuat) sekali oleh {@link #reloadDefault()} pada sesi yang segera
	 * ditutup, sehingga objeknya <b>terlepas</b> dari sesi. Jangan menyuntingnya atau menyimpannya
	 * kembali lewat referensi statis ini, dan jangan mengandalkan pemuatan relasi malas darinya.
	 * Bidang bernilai {@code null} sampai {@link #reloadDefault()} berhasil, dan tetap {@code null}
	 * bila penyemaian gagal — pemanggil harus siap menghadapi {@code null}.</p>
	 */
	public static JenisPenyediaAsset PERUSAHAAN_UMUM = null;

	/**
	 * Menjamin baris jenis bawaan "Perusahaan Swasta Umum" tersedia di tabel
	 * {@code asset.jenis_penyedia_asset}, lalu menyimpan rujukannya pada konstanta statis
	 * {@link #PERUSAHAAN_UMUM}.
	 *
	 * <p><b>Kapan dipanggil.</b> Method ini bagian dari rangkaian penyemaian master data yang
	 * dijalankan {@code InitData} pada saat aplikasi dimulai. Ia dirancang idempoten: pemanggilan
	 * berulang hanya menebus baris yang sudah ada tanpa membuat duplikat, sehingga aman dipanggil
	 * setiap kali aplikasi dijalankan ulang.</p>
	 *
	 * <p><b>Cara kerja.</b> Method membuka sesi Hibernate native lewat
	 * {@code HibernateUtil.currentNativeSession()}, lalu menjalankan pola cari-atau-buat:</p>
	 * <ol>
	 *   <li>Mencari baris dengan {@code Restrictions.ilike("nama", "Perusahaan Swasta Umum",
	 *       MatchMode.ANYWHERE)}. Perhatikan pemilihan {@code MatchMode.ANYWHERE} — bukan
	 *       {@code EXACT} seperti pada {@link StatusPenyediaAsset#reloadDefault()}. Pencocokan
	 *       longgar ini memiliki dua sisi. Sisi baiknya, bila operator mengganti nama baris bawaan
	 *       menjadi mis. "Perusahaan Swasta Umum (PT)", penyemaian berikutnya tetap menemukan
	 *       baris lama dan tidak membuat duplikat. Sisi buruknya, baris lain yang kebetulan
	 *       memuat frasa tersebut sebagai bagian namanya akan "membajak" konstanta
	 *       {@link #PERUSAHAAN_UMUM}; karena hasil dibatasi satu baris tanpa pengurutan eksplisit,
	 *       baris mana yang terpilih bergantung pada urutan yang dikembalikan basis data dan
	 *       karenanya tidak dijamin stabil antar-restart;</li>
	 *   <li>{@code setMaxResults(1).uniqueResult()} membatasi hasil ke satu baris. Pembatasan ini
	 *       penting karena tabel tidak memiliki batasan unik pada kolom {@code nama}: tanpa
	 *       pembatasan tersebut, adanya lebih dari satu baris yang cocok akan membuat
	 *       {@code uniqueResult()} melempar {@code NonUniqueResultException} dan menggagalkan
	 *       penyemaian. Dengan pembatasan ini, baris duplikat hanya tertutupi secara diam-diam —
	 *       duplikatnya tetap ada di tabel dan tetap dapat dirujuk vendor lain, sehingga laporan
	 *       yang mengelompokkan menurut jenis bisa memperlihatkan dua kelompok yang secara bisnis
	 *       sama;</li>
	 *   <li>Bila hasilnya {@code null}, dibuat instance baru dengan {@code kode = "001"},
	 *       {@code nama} dan {@code keterangan} berisi "Perusahaan Swasta Umum", lalu disimpan di
	 *       dalam transaksi tersendiri ({@code begin()}/{@code save()}/{@code commit()}). Berbeda
	 *       dari {@link StatusPenyediaAsset#reloadDefault()} dan
	 *       {@link KategoriPenyediaAsset#reloadDefault()} yang tidak menyemai kode, kelas ini
	 *       mengisi {@code kode} sehingga {@link #getKode()} baris bawaannya bernilai "001";</li>
	 *   <li>Instance hasil temu atau hasil buat disimpan ke {@link #PERUSAHAAN_UMUM}.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan galat.</b> Seluruh badan method dibungkus {@code try/catch} yang menelan
	 * setiap {@code Exception}, mencetak jejak tumpukan, dan mencatatkannya ke
	 * {@code ErrorAuditUtil}. Method <b>tidak pernah melempar</b> ke pemanggil — disengaja agar
	 * satu master data yang gagal disemai tidak menggagalkan seluruh inisialisasi aplikasi.
	 * Konsekuensinya kegagalan bersifat senyap bagi pemanggil: {@link #PERUSAHAAN_UMUM} akan tetap
	 * {@code null} dan hanya jejak di log yang menandai masalah. Tidak ada {@code rollback()}
	 * eksplisit di blok {@code catch}; pembersihan diserahkan ke
	 * {@code HibernateUtil.closeSession()} pada blok {@code finally}, yang selalu dijalankan
	 * termasuk pada jalur sukses.</p>
	 *
	 * <p><b>Interaksi dengan getter bawaan pada {@link PenyediaAsset}.</b> Perlu disadari bahwa
	 * {@code PenyediaAsset.getJenisPenyediaAsset()} menyetel bidang instansnya sendiri ke
	 * {@link #PERUSAHAAN_UMUM} ketika vendor sudah tersimpan namun jenisnya {@code null} — yaitu
	 * getter yang memutasi state objek (pola "getter destruktif" yang berulang di basis kode ini).
	 * Bila objek vendor tersebut kemudian ikut ter-<i>flush</i> Hibernate, nilai bawaan itu dapat
	 * ikut tertulis ke basis data tanpa tindakan eksplisit dari pengguna. Karena
	 * {@link #PERUSAHAAN_UMUM} adalah instance terlepas, penulisan seperti itu bergantung pada
	 * keberhasilan {@code check(...)}/cache {@code ConstantValues} untuk memetakannya kembali ke
	 * baris yang benar.</p>
	 *
	 * <p><b>Catatan bagi penyunting.</b> Jangan menghapus baris bawaan lewat layar CRUD tanpa
	 * memindahkan vendor yang menunjuknya; penghapusan akan meninggalkan rujukan menggantung, dan
	 * penyemaian berikutnya akan membuat baris baru dengan id berbeda sehingga riwayat pelaporan
	 * terpecah.</p>
	 *
	 * @see PenyediaAsset#getJenisPenyediaAsset()
	 * @see KategoriPenyediaAsset#reloadDefault()
	 * @see StatusPenyediaAsset#reloadDefault()
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		try {
			PERUSAHAAN_UMUM = (JenisPenyediaAsset) session.createCriteria(JenisPenyediaAsset.class)

					.add(Restrictions.ilike("nama", "Perusahaan Swasta Umum", MatchMode.ANYWHERE))

					.setMaxResults(1).uniqueResult();
			if (PERUSAHAAN_UMUM == null) {
				PERUSAHAAN_UMUM = new JenisPenyediaAsset();
				PERUSAHAAN_UMUM.setKode("001");
				PERUSAHAAN_UMUM.setNama("Perusahaan Swasta Umum");
				PERUSAHAAN_UMUM.setKeterangan("Perusahaan Swasta Umum");
				session.getTransaction().begin();
				session.save(PERUSAHAAN_UMUM);
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/asset/JenisPenyediaAsset.java:97");
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/** Kode ringkas jenis; baris bawaan disemai dengan nilai {@code "001"} oleh {@link #reloadDefault()}. */
	private String kode;

	/** Nama jenis badan usaha yang ditampilkan; menjadi kunci pencocokan longgar pada {@link #reloadDefault()}. */
	private String nama;
	/** Keterangan bebas; disemai sama dengan {@link #nama} untuk baris bawaan. */
	private String keterangan;
	/** Flag aktif master data; lihat {@link #getAktif()} mengenai keterbatasan penegakannya. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Dipakai pula oleh {@link #reloadDefault()} dan oleh form CRUD
	 * {@code JenisPenyediaAssetAction} saat membuat baris baru.</p>
	 */
	public JenisPenyediaAsset() {
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
	 * Mengembalikan kode jenis dalam bentuk yang aman untuk ditampilkan.
	 *
	 * <p>Mengganti {@code null} dengan string kosong dan memangkas spasi tepi sehingga pemanggil
	 * UI tidak perlu memeriksa {@code null}.</p>
	 *
	 * @return kode yang sudah dipangkas, atau string kosong bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode jenis.
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
	 * Mengembalikan nama jenis setelah dipangkas spasi tepinya.
	 *
	 * <p>Berbeda dari {@link #getKode()}, method ini <b>mempertahankan</b> {@code null} apa adanya.
	 * Pemanggil UI karena itu tetap perlu berjaga terhadap {@code null} walau kolomnya
	 * dideklarasikan {@code nullable = false} pada tingkat basis data.</p>
	 *
	 * @return nama jenis yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis.
	 *
	 * <p>Perhatikan bahwa {@link #reloadDefault()} mencocokkan baris bawaan berdasarkan nama
	 * dengan {@code MatchMode.ANYWHERE}: mengganti nama baris menjadi teks yang tetap memuat frasa
	 * "Perusahaan Swasta Umum" akan membuat baris tersebut tetap dianggap sebagai baris bawaan
	 * pada penyemaian berikutnya.</p>
	 *
	 * @param nama nama jenis badan usaha
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk jenis ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi (tidak dinormalkan menjadi string
	 *         kosong)
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas untuk jenis ini.
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
	 * <p><b>Penegakan lemah.</b> Menonaktifkan sebuah baris jenis <b>tidak</b> memutus atau
	 * menandai vendor yang sudah terlanjur menunjuknya: vendor tersebut tetap dapat dipilih di
	 * {@code AmbilDataPenyediaAssetBanbox} (yang hanya menyaring flag {@code aktif} milik vendor,
	 * bukan milik jenisnya), namanya tetap tercetak di laporan, dan tetap ikut terhitung pada
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
