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
 * Master data <b>status penyedia asset</b> (tabel {@code asset.status_penyedia_asset}) — salah satu
 * dari tiga tabel referensi yang mengklasifikasikan {@link PenyediaAsset} (vendor/penyedia barang
 * dan jasa untuk pengadaan aset tetap), bersama {@link JenisPenyediaAsset} dan
 * {@link KategoriPenyediaAsset}.
 *
 * <h2>PENTING: "status" di sini BUKAN status siklus hidup vendor</h2>
 * <p>Nama kelas ini sangat mudah disalahartikan. Pembaca yang datang dari modul pengadaan lain
 * biasanya menduga entitas ini menyimpan status kelayakan vendor — mis. "Aktif", "Diblokir",
 * "Blacklist", "Masa Sanksi", "Dalam Evaluasi" — dan bahwa ada gerbang yang menolak vendor
 * berstatus buruk saat transaksi pengadaan baru dibuat. <b>Dugaan itu tidak benar untuk kelas
 * ini.</b></p>
 * <p>Nilai yang di-<i>seed</i> oleh {@link #reloadDefault()} hanya dua, dan keduanya bersifat
 * <b>geografis/organisatoris</b>, bukan disipliner:</p>
 * <ul>
 *   <li>{@code "Pusat"} — vendor merupakan kantor pusat/perusahaan induk;</li>
 *   <li>{@code "Cabang"} — vendor merupakan kantor cabang/perwakilan.</li>
 * </ul>
 * <p>Artinya kolom ini menjawab pertanyaan "badan usaha ini pusat atau cabang?", bukan "boleh
 * tidak vendor ini dipakai?". Operator memang bebas menambah baris lain lewat
 * {@code StatusPenyediaAssetAction} (mis. mengetik baris bernama "Blacklist"), tetapi menambah
 * baris <b>tidak</b> menciptakan perilaku apa pun: lihat bagian berikut.</p>
 *
 * <h2>Status ini murni deskriptif — tidak ada gerbang yang menegakkannya</h2>
 * <p>Penelusuran seluruh pemakaian {@code StatusPenyediaAsset} di basis kode menunjukkan bahwa
 * nilai status <b>tidak pernah</b> dibaca sebagai syarat, prasyarat, atau larangan. Yang ada hanya
 * pemakaian tampilan dan pelaporan:</p>
 * <ul>
 *   <li>{@code PenyediaAssetAction} menampilkannya sebagai satu {@code Label} kolom daftar vendor,
 *       memakainya sebagai kolom pengurutan/pencarian ({@code isiSatuFilterCombo(searchStatus, …)}
 *       dan {@code criteria.add(Restrictions.eq("statusPenyediaAsset", stsVal))}) — yaitu filter
 *       tampilan yang dipilih sendiri oleh pengguna, bukan pembatasan;</li>
 *   <li>{@code CommonReportHelper} dan {@code TampilanPengumumanAkademisAction} mencetak
 *       {@code getNama()}-nya ke laporan/keluaran teks;</li>
 *   <li>{@code DasboardVendor} memakainya sebagai sumbu pengelompokan statistik
 *       ({@code select s.nama, count(p.id) from PenyediaAsset p left join p.statusPenyediaAsset s
 *       group by s.nama}).</li>
 * </ul>
 * <p>Tidak ada satu pun titik pembuatan transaksi pengadaan — permintaan, pemesanan (PO),
 * penerimaan, perjanjian kerja sama, maupun ketiga jenis pembayaran — yang membaca status ini,
 * apalagi menolak vendor berdasarkan nilainya. Ini adalah pola berulang "gerbang deskriptif" yang
 * juga muncul pada kebijakan retur di modul lain: entitas berperan sebagai label yang dilaporkan,
 * bukan aturan yang ditegakkan.</p>
 * <p>Lebih jauh lagi, editor untuk kolom ini pada form vendor <b>sudah dinonaktifkan</b>: blok
 * {@code Radiogroup statusPenyediaAsset} beserta pemasangan {@code Common.insertRadio(...)},
 * validasinya, dan {@code penyediaAsset.setStatusPenyediaAsset(...)} seluruhnya dikomentari di
 * {@code PenyediaAssetAction}. Konsekuensinya, dalam pemakaian normal nilai kolom ini <b>tidak
 * dapat diubah dari UI</b> dan setiap vendor tersimpan akan memperoleh nilai bawaan
 * {@link #PUSAT} melalui getter {@code PenyediaAsset.getStatusPenyediaAsset()} yang memaksa
 * default bila kolomnya masih {@code null}. Praktisnya, kolom ini hampir selalu bernilai "Pusat"
 * untuk seluruh populasi vendor sehingga daya diskriminasinya pada laporan dan dasbor mendekati
 * nol.</p>
 *
 * <h2>Lalu di mana gerbang vendor yang sebenarnya?</h2>
 * <p>Pembatasan pemakaian vendor pada AIS dijalankan oleh dua mekanisme lain, bukan oleh kelas
 * ini:</p>
 * <ol>
 *   <li><b>Flag {@code aktif} pada {@link PenyediaAsset}</b> — {@code PenyediaAsset.getAktif()}
 *       menurunkan nilainya dari alur persetujuan SOP ({@code DisposisiSop}): vendor yang
 *       disposisinya tidak aktif atau berakhir pada langkah penolakan akan menjadi
 *       {@code false}. Pemilih vendor {@code AmbilDataPenyediaAssetBanbox} menyaring dengan
 *       {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} pada kedua jalurnya
 *       (pencarian daftar dan penebusan berdasarkan kode), sehingga vendor nonaktif/ditolak tidak
 *       muncul dan tidak dapat dipilih untuk transaksi baru;</li>
 *   <li><b>Alur persetujuan SOP</b> pada pendaftaran vendor itu sendiri — {@link PenyediaAsset}
 *       mewarisi {@code DataSop}, sehingga vendor baru harus melewati disposisi sebelum dianggap
 *       sah.</li>
 * </ol>
 * <p>Karena itu, bila kebutuhan bisnis adalah "vendor blacklist tidak boleh dipakai", jalur yang
 * benar adalah menonaktifkan flag {@code aktif} vendor tersebut (atau menolaknya lewat SOP),
 * <b>bukan</b> membuat baris status bernama "Blacklist" di tabel ini — baris seperti itu akan
 * tampil di laporan namun sama sekali tidak menghalangi transaksi.</p>
 *
 * <h2>Struktur dan pemuatan</h2>
 * <p>Struktur kelas mengikuti cetakan master data ringan AIS: kunci utama {@code IDENTITY},
 * pasangan {@code kode}/{@code nama}/{@code keterangan}, satu flag {@link #getAktif()}, ditambah
 * bidang audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang wajib ada agar
 * {@code AuditTimestampInterceptor} dapat bekerja (lihat {@link #onUpdate()}). Entitas
 * dianotasi {@link Audited} sehingga seluruh perubahan terekam pada tabel revisi Envers, serta
 * memakai {@code dynamicInsert}/{@code dynamicUpdate} agar hanya kolom yang berubah yang
 * dikirim ke basis data.</p>
 * <p>Pemuatan awal dilakukan {@link #reloadDefault()}, yang dipanggil dari {@code InitData}
 * bersama seluruh master data lain saat aplikasi dijalankan. Kelas ini juga terdaftar pada daftar
 * kelas yang di-cache {@code ConstantValues} di {@code InitData}, sehingga resolusi lewat
 * {@code check(...)} pada getter {@code PenyediaAsset.getStatusPenyediaAsset()} umumnya dilayani
 * dari cache memori, bukan dari basis data.</p>
 *
 * @see PenyediaAsset
 * @see JenisPenyediaAsset
 * @see KategoriPenyediaAsset
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "status_penyedia_asset")
public class StatusPenyediaAsset extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya sengaja disamakan dengan hampir seluruh entitas master data lain di paket
	 * {@code ais.database.model.asset} (nilai warisan dari cetakan hbm2java). Kesamaan nilai ini
	 * tidak berbahaya karena Java memverifikasi {@code serialVersionUID} per kelas, bukan lintas
	 * kelas; namun jangan mengubahnya tanpa alasan, sebab objek entitas dapat tersimpan pada
	 * sesi/desktop ZK yang diserialisasi.</p>
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
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b> — nilai lama dipertahankan. Perilaku ini disengaja: bidang audit
	 * bayangan hanya boleh diisi oleh {@code AuditTimestampInterceptor}, dan penulisan kosong dari
	 * jalur salin/klon objek tidak boleh menghapus jejak audit yang sudah ada.</p>
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
	 * audit tidak terhapus oleh penulisan kosong.</p>
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
	 * {@link #getTanggal_dirubah()} dari konteks pengguna aktif. Method ini sengaja
	 * {@code protected} dan tidak boleh dipanggil manual dari kode aplikasi.</p>
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
	 * <p>Dipakai untuk penelusuran log dan sebagai label bawaan pada beberapa komponen ZK.
	 * Perhatikan bahwa method ini membaca bidang {@link #nama} secara langsung, bukan lewat
	 * {@link #getNama()}, sehingga tidak melakukan {@code trim()}.</p>
	 *
	 * @return gabungan id dan nama; bagian id bernilai {@code "null"} bila baris belum tersimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Baris rujukan berbagi untuk status <b>"Pusat"</b>, dipakai sebagai nilai bawaan oleh
	 * {@code PenyediaAsset.getStatusPenyediaAsset()} ketika kolom vendor masih {@code null}.
	 *
	 * <p><b>Peringatan siklus hidup:</b> ini adalah bidang statis yang menyimpan instance entitas
	 * Hibernate. Instance tersebut ditebus (atau dibuat) sekali oleh {@link #reloadDefault()} pada
	 * sesi Hibernate yang segera ditutup, sehingga objek yang tersimpan di sini <b>terlepas</b>
	 * dari sesi. Jangan menyunting atau menyimpannya kembali lewat referensi statis ini, dan
	 * jangan mengandalkan pemuatan relasi malas darinya; gunakan {@code check(...)} atau muat
	 * ulang dari sesi aktif bila objek dibutuhkan dalam keadaan terpasang.</p>
	 * <p>Bernilai {@code null} sampai {@link #reloadDefault()} berhasil dijalankan, dan tetap
	 * {@code null} bila pemuatan gagal (mis. basis data belum siap) — pemanggil yang mendereferensi
	 * bidang ini secara langsung harus siap menghadapi {@code null}.</p>
	 */
	public static StatusPenyediaAsset PUSAT = null;
	/**
	 * Baris rujukan berbagi untuk status <b>"Cabang"</b>.
	 *
	 * <p>Disiapkan {@link #reloadDefault()} berdampingan dengan {@link #PUSAT}, namun — berbeda
	 * dengan {@link #PUSAT} — tidak dipakai sebagai nilai bawaan oleh entitas mana pun. Dalam basis
	 * kode saat ini konstanta ini tidak dirujuk dari luar kelas: ia hanya menjamin baris "Cabang"
	 * tersedia di tabel supaya operator dapat memilihnya (seandainya editor status pada form
	 * vendor diaktifkan kembali) dan supaya pengelompokan dasbor memiliki kategori kedua.</p>
	 * <p>Peringatan siklus hidup instance terlepas pada {@link #PUSAT} berlaku sama di sini.</p>
	 */
	public static StatusPenyediaAsset CABANG = null;

	/**
	 * Menjamin dua baris status bawaan — "Pusat" dan "Cabang" — tersedia di tabel
	 * {@code asset.status_penyedia_asset}, lalu menyimpan rujukannya pada konstanta statis
	 * {@link #PUSAT} dan {@link #CABANG}.
	 *
	 * <p><b>Kapan dipanggil.</b> Method ini bagian dari rangkaian penyemaian master data yang
	 * dijalankan {@code InitData} saat aplikasi dimulai ({@code StatusPenyediaAsset.reloadDefault()}
	 * dipanggil berurutan bersama puluhan {@code reloadDefault()} lain). Ia dirancang idempoten:
	 * aman dipanggil berulang kali, dan pemanggilan kedua hanya akan menebus baris yang sudah ada
	 * tanpa membuat duplikat.</p>
	 *
	 * <p><b>Cara kerja.</b> Method membuka sesi Hibernate native lewat
	 * {@code HibernateUtil.currentNativeSession()}, kemudian untuk masing-masing dari dua nilai
	 * bawaan menjalankan pola cari-atau-buat yang sama:</p>
	 * <ol>
	 *   <li>Mencari baris yang namanya cocok dengan {@code Restrictions.ilike("nama", "Pusat",
	 *       MatchMode.EXACT)} — perbandingan <i>case-insensitive</i> namun <b>cocok persis</b>
	 *       (bukan {@code ANYWHERE}), sehingga baris bernama "Kantor Pusat" atau "Pusat Jakarta"
	 *       <b>tidak</b> akan dianggap sebagai baris bawaan dan method akan menambah baris "Pusat"
	 *       yang baru di sampingnya. Ketelitian ini berbeda dari {@link JenisPenyediaAsset} dan
	 *       {@link KategoriPenyediaAsset} yang memakai {@code MatchMode.ANYWHERE}, sehingga
	 *       kelas ini lebih rentan menghasilkan baris yang tampak mirip bila operator sempat
	 *       mengubah nama baris bawaan;</li>
	 *   <li>{@code setMaxResults(1).uniqueResult()} membatasi hasil ke satu baris. Pembatasan ini
	 *       penting: tabel tidak memiliki batasan unik pada kolom {@code nama}, sehingga bila
	 *       terdapat lebih dari satu baris "Pusat" (mis. akibat penyuntingan manual atau migrasi
	 *       data), tanpa {@code setMaxResults(1)} pemanggilan {@code uniqueResult()} akan melempar
	 *       {@code NonUniqueResultException}. Dengan pembatasan ini, baris duplikat hanya
	 *       "tertutupi" secara diam-diam — baris pertama menurut urutan basis data yang dipakai,
	 *       dan duplikatnya tetap ada di tabel serta tetap dapat dirujuk vendor lain;</li>
	 *   <li>Bila hasilnya {@code null}, dibuat instance baru dengan {@code nama} dan
	 *       {@code keterangan} yang sama, lalu disimpan di dalam transaksi tersendiri
	 *       ({@code begin()}/{@code save()}/{@code commit()}). Perhatikan bahwa {@code kode}
	 *       <b>tidak</b> diisi pada kelas ini — berbeda dari {@link JenisPenyediaAsset} yang
	 *       menyemai {@code kode = "001"} — sehingga {@link #getKode()} akan mengembalikan
	 *       string kosong untuk kedua baris bawaan;</li>
	 *   <li>Instance hasil temu atau hasil buat disimpan ke konstanta statis yang bersangkutan.</li>
	 * </ol>
	 *
	 * <p><b>Transaksi per baris.</b> Setiap penyimpanan memakai transaksi sendiri, bukan satu
	 * transaksi menyeluruh. Akibatnya penyemaian bisa berhenti separuh jalan: bila pembuatan
	 * "Cabang" gagal setelah "Pusat" berhasil di-commit, baris "Pusat" tetap ada. Karena method
	 * ini idempoten, kondisi separuh jalan tersebut akan dilengkapi pada pemanggilan berikutnya
	 * saat aplikasi dijalankan ulang.</p>
	 *
	 * <p><b>Penanganan galat.</b> Seluruh badan method dibungkus {@code try/catch} yang menelan
	 * setiap {@code Exception}, mencetak jejak tumpukan, dan mencatatkannya ke
	 * {@code ErrorAuditUtil}. Method <b>tidak pernah melempar</b> ke pemanggil. Ini disengaja agar
	 * satu master data yang gagal disemai tidak menggagalkan seluruh proses inisialisasi aplikasi,
	 * tetapi berarti kegagalan bersifat senyap dari sudut pandang pemanggil: {@link #PUSAT} dan
	 * {@link #CABANG} akan tetap {@code null} dan hanya jejak di log yang menandai masalah.
	 * Perhatikan pula bahwa {@code catch} berada di luar transaksi per baris dan tidak melakukan
	 * {@code rollback()} eksplisit; pembersihan sesi diserahkan ke {@code HibernateUtil.closeSession()}
	 * pada blok {@code finally}, yang selalu dijalankan.</p>
	 *
	 * <p><b>Dampak fungsional yang perlu disadari.</b> Karena editor status pada form vendor
	 * dinonaktifkan (lihat catatan pada Javadoc kelas), praktisnya method inilah satu-satunya
	 * penulis kolom status untuk populasi vendor: setiap vendor yang belum punya status akan
	 * memperoleh {@link #PUSAT} dari getter {@code PenyediaAsset.getStatusPenyediaAsset()}.
	 * Baris "Cabang" yang disemai di sini nyaris tidak pernah terpakai. Kondisi ini membuat
	 * distribusi status pada {@code DasboardVendor} praktis selalu memperlihatkan seluruh vendor
	 * berada di satu kelompok.</p>
	 *
	 * <p><b>Catatan bagi penyunting.</b> Jangan mengubah teks nama bawaan ("Pusat"/"Cabang") tanpa
	 * memigrasi data yang sudah ada: kecocokan {@code EXACT} pada method ini akan gagal menemukan
	 * baris lama dan menambahkan baris baru, sementara vendor lama tetap menunjuk baris lama —
	 * hasilnya adalah dua status yang secara bisnis sama namun terpisah di laporan.</p>
	 *
	 * @see PenyediaAsset#getStatusPenyediaAsset()
	 * @see JenisPenyediaAsset#reloadDefault()
	 * @see KategoriPenyediaAsset#reloadDefault()
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		try {
			PUSAT = (StatusPenyediaAsset) session.createCriteria(StatusPenyediaAsset.class)

					.add(Restrictions.ilike("nama", "Pusat", MatchMode.EXACT))

					.setMaxResults(1).uniqueResult();
			if (PUSAT == null) {
				PUSAT = new StatusPenyediaAsset();
				PUSAT.setNama("Pusat");
				PUSAT.setKeterangan("Pusat");
				session.getTransaction().begin();
				session.save(PUSAT);
				session.getTransaction().commit();
			}

			CABANG = (StatusPenyediaAsset) session.createCriteria(StatusPenyediaAsset.class)

					.add(Restrictions.ilike("nama", "Cabang", MatchMode.EXACT))

					.setMaxResults(1).uniqueResult();
			if (CABANG == null) {
				CABANG = new StatusPenyediaAsset();
				CABANG.setNama("Cabang");
				CABANG.setKeterangan("Cabang");
				session.getTransaction().begin();
				session.save(CABANG);
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/asset/StatusPenyediaAsset.java:111");
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/** Kode ringkas status; tidak disemai oleh {@link #reloadDefault()} sehingga kosong untuk baris bawaan. */
	private String kode;

	/** Nama status yang ditampilkan ("Pusat"/"Cabang"); menjadi kunci pencocokan pada {@link #reloadDefault()}. */
	private String nama;
	/** Keterangan bebas; disemai sama dengan {@link #nama} untuk baris bawaan. */
	private String keterangan;
	/** Flag aktif master data; lihat {@link #getAktif()} mengenai keterbatasan penegakannya. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Dipakai pula oleh {@link #reloadDefault()} dan oleh form CRUD
	 * {@code StatusPenyediaAssetAction} saat membuat baris baru.</p>
	 */
	public StatusPenyediaAsset() {
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
	 * Mengembalikan kode status dalam bentuk yang aman untuk ditampilkan.
	 *
	 * <p>Mengganti {@code null} dengan string kosong dan memangkas spasi tepi, sehingga pemanggil
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
	 * Menyetel kode status.
	 *
	 * @param kode kode ringkas; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama status setelah dipangkas spasi tepinya.
	 *
	 * <p>Berbeda dari {@link #getKode()}, method ini <b>mempertahankan</b> {@code null} apa adanya
	 * dan hanya memangkas bila nilainya ada. Pemanggil UI karena itu tetap perlu berjaga terhadap
	 * {@code null}, walau kolomnya dideklarasikan {@code nullable = false} pada tingkat basis
	 * data.</p>
	 *
	 * @return nama status yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama status.
	 *
	 * <p>Perhatikan bahwa {@link #reloadDefault()} mencocokkan baris bawaan berdasarkan nama
	 * dengan {@code MatchMode.EXACT}; mengganti nama baris bawaan akan membuat penyemaian
	 * berikutnya membuat baris baru alih-alih menemukan baris lama.</p>
	 *
	 * @param nama nama status
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk status ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi (tidak dinormalkan menjadi string
	 *         kosong)
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas untuk status ini.
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
	 * {@code null} (mis. hasil migrasi sebelum kolom ini ada, atau baris yang disemai
	 * {@link #reloadDefault()} yang memang tidak menyetelnya) akan dianggap aktif. Pola
	 * {@code null == true} ini konsisten di seluruh master data AIS.</p>
	 * <p><b>Penegakan lemah.</b> Sebagaimana dijelaskan pada Javadoc kelas, nilai status penyedia
	 * tidak pernah menjadi syarat transaksi; flag ini pun demikian. Menonaktifkan sebuah baris
	 * status <b>tidak</b> membatalkan atau memutus vendor yang sudah terlanjur menunjuk baris
	 * tersebut — vendor tetap dapat dipakai, dan nama status yang nonaktif tetap tercetak di
	 * laporan serta tetap dihitung oleh {@code DasboardVendor}. Efeknya terbatas pada penyaringan
	 * daftar pilihan di layar CRUD yang bersangkutan.</p>
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
