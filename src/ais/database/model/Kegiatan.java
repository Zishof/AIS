package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;

/**
 * <b>Header tagihan per mahasiswa per semester</b> &mdash; entity billing paling sentral
 * pada AIS. Satu baris menjawab: <i>siapa</i> ditagih, untuk <i>jenis kegiatan apa</i>, pada
 * <i>semester berapa</i>, <i>berapa totalnya</i>, dan <i>sudah dibayar berapa</i>.
 *
 * <h3>Tiga lapis mesin penagihan</h3>
 * <ol>
 *   <li>{@link JenisKegiatan} &mdash; katalog/aturan (jenis tagihan, rentang semester,
 *       boleh diangsur, ada denda). Tanpa nominal, tanpa orang.</li>
 *   <li>{@link DetailBiaya} &mdash; master nominal bersama; satu baris melayani semua
 *       mahasiswa yang cocok dengan kombinasi penyaringnya.</li>
 *   <li><b>{@code Kegiatan}</b> (kelas ini) &mdash; header per orang per semester;
 *       {@link DetailKegiatan} baris-baris rinciannya.</li>
 * </ol>
 *
 * <h3>Pemilik tagihan: mahasiswa ATAU calon mahasiswa</h3>
 * <p>Sebuah {@code Kegiatan} menunjuk {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}
 * &mdash; keduanya {@code nullable}. Tagihan pendaftaran dan daftar ulang mahasiswa baru
 * melekat pada berkas calon (yang belum punya NIM), sedangkan tagihan semester berjalan
 * melekat pada mahasiswa. Sebagian besar getter di kelas ini karena itu berbentuk
 * &quot;coba mahasiswa dulu, kalau kosong coba calon mahasiswa&quot;. Perhatikan bahwa
 * {@link #getMahasiswa()} dan {@link #getCalonMahasiswa()} saling mengisi: yang pertama
 * dapat menurunkan mahasiswa dari calon, yang kedua dapat menurunkan calon dari mahasiswa
 * untuk dua jenis kegiatan pendaftaran.</p>
 *
 * <h3>PENTING: total TIDAK dijumlahkan dari DetailKegiatan</h3>
 * <p>Ini kejutan arsitektural terbesar kelas ini. Meskipun {@link DetailKegiatan} adalah
 * baris rincian yang menunjuk ke {@code Kegiatan} lewat foreign key, total tagihan
 * <b>tidak</b> dihitung dengan menjumlahkan baris-baris itu. Sebagai gantinya kelas ini
 * menyimpan dua kolom {@code text} berisi JSON:</p>
 * <ul>
 *   <li>{@link #getTagihans()} &mdash; peta nominal tagihan per baris; dijumlahkan
 *       {@link #hitungTagihan()} menjadi field {@code tagihan}.</li>
 *   <li>{@link #getBulans()} &mdash; peta nominal yang sudah dibayar per baris/bulan;
 *       dijumlahkan {@link #hitungDibayar()} menjadi field {@code dibayar}.</li>
 * </ul>
 * <p>Kedua field hasil itulah yang dibaca {@link #getPersentase()},
 * {@link #getApakahLunas()}, dan {@link #getAmountTerhutang()}. Konsekuensinya: nilai yang
 * ditampilkan kepada mahasiswa berasal dari <b>snapshot JSON</b>, bukan dari keadaan
 * terkini tabel {@code detail_kegiatan}. Selama JSON belum dihitung ulang, penambahan atau
 * perubahan {@link DetailKegiatan} tidak tercermin pada total. Dua daftar id pendamping
 * &mdash; {@link #getDetailKegiatans()} dan {@link #getCicilans()} &mdash; menyimpan
 * keanggotaan baris dalam bentuk string {@code ",id:true,"} dengan mekanisme hapus lunak,
 * juga terpisah dari relasi basis data yang sesungguhnya.</p>
 *
 * <h3>Entity kerabat</h3>
 * <ul>
 *   <li>{@link DetailKegiatan} &mdash; baris rincian; menunjuk {@code Kegiatan} dan
 *       {@link DetailBiaya}, serta menyimpan nominal yang sudah <i>dibekukan</i> untuk
 *       mahasiswa ini pada kolom {@code biaya} beserta {@code diskon}-nya.</li>
 *   <li>{@link KegiatanTemporary} &mdash; header <b>staging</b> dengan struktur serupa dan
 *       foreign key {@code kegiatan} yang menunjuk balik ke sini. Dipakai menyusun tagihan
 *       sebelum disahkan menjadi {@code Kegiatan} sesungguhnya; kunci unik
 *       {@link DetailKegiatan#kodeUnik} menerima salah satu dari keduanya, sehingga sebuah
 *       baris rincian dapat bernaung di bawah header staging maupun header final.</li>
 *   <li>{@link CicilanPembayaran} &mdash; angsuran yang dibayarkan terhadap tagihan ini.</li>
 * </ul>
 *
 * <h3>Getter destruktif dan nominal terkunci</h3>
 * <p>Seperti kerabatnya, kelas ini memakai <i>property access</i> sehingga getter dipanggil
 * Hibernate pada setiap {@code dirty check}. Banyak getter di sini menurunkan nilainya dari
 * {@link Mahasiswa}/{@link BiodataCalonMahasiswa} lalu menulisnya balik ke field yang
 * dipetakan ke kolom &mdash; antara lain {@link #getTahunAkademik()}, {@link #getProgram()},
 * {@link #getTanggal()}, {@link #getSemster()}, {@link #getJurusan()},
 * {@link #getTahunAngkatan()}, {@link #getStatusAwalMahasiswa()}, {@link #getKode()},
 * {@link #getAmount()}, {@link #getAmountTerhutang()}, {@link #getLunas()},
 * {@link #getPersentaseLunas()}, {@link #getAktif()}, dan
 * {@link #getPembatalanDenda()}. Pada kelas {@code @Audited} ini, pembacaan biasa karena
 * itu dapat memicu {@code UPDATE} beserta revisi Envers palsu.</p>
 *
 * <p>Pengecualian yang penting adalah {@link #getKodeunik()}, yang sengaja <b>membekukan</b>
 * nilai tersimpan untuk entity yang sudah punya id &mdash; kunci unik alami tagihan tidak
 * boleh berubah karena sudah dirujuk banyak relasi. Untuk nominal, mekanisme pembekuan yang
 * disediakan adalah snapshot JSON {@link #getNominalTagihanKunciJson()} beserta
 * {@link #simpanNominalTagihanTerkunci} yang mewajibkan alasan dan mencatat pelaku.</p>
 *
 * <p>Bank generated by hbm2java</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kegiatan")
public class Kegiatan extends GeneralValueObject {

	/** Nomor versi serialisasi Java; dipertahankan agar objek lama tetap dapat dibaca. */
	private static final long serialVersionUID = 2413822577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Id pengguna yang terakhir menyentuh baris ini &mdash; field audit bayangan yang diisi
	 * {@link ais.database.hibernate.AuditTimestampInterceptor}.
	 *
	 * @return id pelaku perubahan terakhir; {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter audit <b>satu arah</b>: masukan {@code null}/kosong diabaikan diam-diam sehingga
	 * nilai lama tidak pernah dapat dikosongkan kembali.
	 *
	 * @param olehId id pelaku; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	private String tambahanKodeUnik = "";

	/**
	 * Setter audit <b>satu arah</b> untuk nama pelaku; masukan {@code null}/kosong diabaikan.
	 *
	 * @param oleh nama pelaku; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir menyentuh baris ini (field audit bayangan).
	 *
	 * @return nama pelaku perubahan terakhir; {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat stempel waktu/pelaku tepat sebelum
	 * {@code UPDATE} dikirim ke database.
	 *
	 * <p>Karena banyak getter kelas ini menulis balik ke kolom (lihat javadoc kelas), callback
	 * ini ikut berjalan pada &quot;update palsu&quot; yang dipicu semata-mata oleh pembacaan
	 * entity di dalam session terbuka.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setter langsung stempel waktu perubahan terakhir (tanpa validasi).
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir; nilai awalnya diisi saat objek dibuat dengan
	 * {@link ais.ui.util.WaktuUtil#getDate()}.
	 *
	 * <p>Perhatikan bahwa {@link #getTanggal()} &mdash; tanggal tagihan &mdash; justru
	 * mengembalikan nilai field ini, sehingga kedua konsep itu tidak terpisah; lihat catatan
	 * pada getter tersebut.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi {@code id-namaKegiatan nominal} untuk log dan komponen ZK.
	 *
	 * <p><b>Memicu tiga getter yang menulis balik ke field.</b> Method ini menugaskan hasil
	 * {@link #getJenisKegiatan()}, {@link #getMahasiswa()}, dan {@link #getCalonMahasiswa()}
	 * ke field masing-masing sebelum menyusun string. Dua yang terakhir bukan sekadar
	 * pemulihan proxy: keduanya dapat <b>mengisi foreign key yang semula kosong</b> dengan
	 * hasil turunan (mahasiswa dari calon, atau sebaliknya). Jadi mencetak sebuah
	 * {@code Kegiatan} ke log di dalam session yang terbuka dapat menautkan baris tagihan itu
	 * ke pemilik yang sebelumnya tidak tercatat. Nominal dibaca dari field {@code amount}
	 * mentah, sehingga tidak memicu {@link #getAmount()}.</p>
	 *
	 * <p>Bentuk lengkap yang memuat nomor referensi, pemilik, tahun akademik, semester,
	 * program, dan persentase pelunasan masih ada dalam bentuk komentar di bawahnya; ia
	 * ditinggalkan karena terlalu berat untuk dipanggil pada setiap pencatatan log &mdash;
	 * {@code getPersentaseLunas()} sendiri memicu rantai perhitungan tagihan.</p>
	 *
	 * @return representasi ringkas header tagihan
	 */
	public String toString() {
		jenisKegiatan = getJenisKegiatan();
		mahasiswa = getMahasiswa();
		calonMahasiswa = getCalonMahasiswa();

		return id + "-" + (jenisKegiatan == null ? "" : "-" + jenisKegiatan.getNamaKegiatan()) + " "
				+ (amount == null ? "" : Common.numberFormat.get().format(amount));

//		return id + "-" + (refNumber == null ? "" : refNumber) + (mahasiswa == null ? "" : "-" + mahasiswa)
//				+ (calonMahasiswa == null ? "" : "-" + calonMahasiswa) + "-"
//				+ (jenisKegiatan == null ? "" : "-" + jenisKegiatan.getNamaKegiatan()) + tahunAkademik + "-" + semster
//				+ "-" + program + "- Rp." + (amount == null ? "" : Common.numberFormat.get().format(amount)) + "-"
//				+ getPersentaseLunas() + "%";
	}

	private String refNumber;
	private String kodeunik;
	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa calonMahasiswa;
	private JenisKegiatan jenisKegiatan;

	private Integer tahunAngkatan;
	private Jurusan jurusan;

	private String tahunAkademik;
	private String program;
	private Date tanggal;
	private Date tanggalBayarAwal;
	private Date tanggalBayarTerakhir;
	private Integer semster;
	private Integer validated;
	private String validator;
	private Double pengurangan;
	private String keterangan;
	private Double amount;
	private Double denda;

	private StatusMahasiswa statusMahasiswa;
	private StatusAwalMahasiswa statusAwalMahasiswa;
	private String kode;

	private JadwalPembayaran jadwalPembayaran;
	private Double amountTerhutang = 0.0;
	private Boolean lunas = false;
	private Double persentaseLunas = 0.0;

	private Boolean apakahLunas = null;
	private Double persentase = null;

	private Integer bulan;

	private Double jumlahTelahDibayar = 0.0;

	private UploadVirtualAccount uploadVirtualAccount;
	private Boolean kodeUnikLain;
	private String bulans;
	private String tagihans;
	private Boolean aktif;

	private Double dibayar;
	private Double tagihan;
	private String cicilans;
	private String detailKegiatans;
	private String pembatalanDenda;
	private String nominalTagihanKunciJson;

	/** Konstruktor kosong wajib bagi Hibernate/JPA dan bagi form CRUD generik. */
	public Kegiatan() {
	}

	/**
	 * Konstruktor pintasan berisi <b>hanya</b> primary key &mdash; instance {@code TRANSIENT}
	 * yang seluruh field lainnya {@code null}.
	 *
	 * <p>Berhati-hatilah memakainya sebagai nilai relasi yang akan disimpan: bila {@code id}
	 * tidak ada di tabel {@code kegiatan}, {@code INSERT} pemilik relasi melanggar foreign
	 * key. Bandingkan pola muat-aman {@link DetailBiaya#muatRefAman(Session, Long)}.</p>
	 *
	 * @param id primary key header tagihan
	 */
	public Kegiatan(Long id) {
		this.id = id;
	}

	/**
	 * Primary key {@code kegiatan.id}, dihasilkan database ({@code IDENTITY}) sehingga
	 * {@code insertable = false}.
	 *
	 * <p>Nilai ini punya arti khusus di beberapa tempat: {@link #getKodeunik()} memakainya
	 * untuk memutuskan apakah kunci unik alami sudah boleh dibekukan, dan
	 * {@link #ambilByKodeUnik(String, Session)} memakainya sebagai kunci cache statis
	 * {@link #mappingId}.</p>
	 *
	 * @return primary key; {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter primary key.
	 *
	 * @param id primary key header tagihan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * {@link JenisKegiatan} yang ditagihkan header ini &mdash; menentukan jenis tagihan
	 * (registrasi ulang, pendaftaran, wisuda, dan seterusnya) beserta seluruh aturannya:
	 * rentang semester, boleh diangsur atau tidak, dan konfigurasi denda.
	 *
	 * <p>Relasi ini juga ikut membentuk kunci unik alami tagihan lewat
	 * {@link #generateKodeUnik}, sehingga satu mahasiswa hanya boleh punya satu
	 * {@code Kegiatan} per pasangan (jenis kegiatan, semester). Ia dibaca pula oleh
	 * {@link #getSemster()} (yang memaksa semester nol untuk kegiatan pendaftaran calon
	 * mahasiswa), {@link #getAktif()} (yang menonaktifkan header di luar rentang semester),
	 * dan {@link #hitungTagihan()} (yang memakai
	 * {@link JenisKegiatan#getHanyaBerupaAngsuran()} untuk memutuskan kunci JSON mana yang
	 * dijumlahkan).</p>
	 *
	 * <p>Getter relasi lazy standar: {@code check(...)} memulihkan proxy yang mungkin sudah
	 * terputus, tanpa mengubah nilai foreign key.</p>
	 *
	 * @return jenis kegiatan yang ditagihkan; {@code null} bila header belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan")
	public JenisKegiatan getJenisKegiatan() {
		jenisKegiatan = check(jenisKegiatan);
		return this.jenisKegiatan;
	}

	/**
	 * Setter jenis kegiatan. Mengubahnya pada header yang sudah tersimpan tidak mengubah
	 * {@link #getKodeunik()}, karena kunci unik alami sengaja dibekukan setelah entity punya
	 * id.
	 *
	 * @param jenisKegiatan jenis kegiatan yang ditagihkan
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * Setter pemilik tagihan berupa mahasiswa aktif.
	 *
	 * @param mahasiswa pemilik tagihan; {@code null} bila tagihan milik calon mahasiswa
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Pemilik tagihan berupa {@link Mahasiswa} aktif (ber-NIM). Bersama
	 * {@link #getCalonMahasiswa()} membentuk pasangan pemilik yang saling menggantikan.
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; dapat mengisi FOREIGN KEY.</b> Bila
	 * {@link #getCalonMahasiswa()} terisi <i>dan</i> berkas calon itu sudah tertaut ke
	 * seorang mahasiswa ({@code calonMahasiswa.getMahasiswa()}), field {@code mahasiswa}
	 * <b>ditimpa</b> dengan mahasiswa tersebut. Karena property ini dipetakan ke kolom
	 * {@code kegiatan.mahasiswa}, penimpaan itu tersimpan pada {@code flush} berikutnya.</p>
	 *
	 * <p>Secara semantik ini menjembatani transisi calon menjadi mahasiswa: tagihan
	 * pendaftaran yang semula hanya menunjuk berkas calon otomatis ikut menunjuk NIM begitu
	 * mahasiswanya terbentuk, sehingga tagihan lama muncul di riwayat pembayaran mahasiswa
	 * yang bersangkutan. Yang perlu disadari adalah penimpaannya <b>tanpa syarat</b>: nilai
	 * yang sengaja diisi berbeda tidak akan bertahan selama jalur calon-ke-mahasiswa dapat
	 * ditelusuri. Bila cabang itu tidak berlaku, {@code check(...)} sekadar memulihkan
	 * proxy.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getCalonMahasiswa()} melakukan hal sebaliknya untuk dua
	 * jenis kegiatan pendaftaran, sehingga kedua getter dapat saling mengisi.</p>
	 *
	 * @return pemilik tagihan berupa mahasiswa; {@code null} bila tagihan milik calon
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		if (getCalonMahasiswa() != null && getCalonMahasiswa().getMahasiswa() != null) {
			mahasiswa = getCalonMahasiswa().getMahasiswa();
		} else {
			mahasiswa = check(mahasiswa);
		}

		return mahasiswa;
	}

	/**
	 * Setter tahun akademik. Nilai yang diisi di sini akan ditimpa {@link #getTahunAkademik()}
	 * pada pembacaan berikutnya bila pemilik dan semester tagihan diketahui.
	 *
	 * @param tahunAkademik tahun akademik, mis. {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Tahun akademik tagihan ini, mis. {@code "2025/2026"}.
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; nilai diturunkan ulang setiap kali dibaca.</b> Kolom
	 * {@code tahun_akademik} praktis bukan data mandiri: nilainya dihitung dari tahun
	 * angkatan pemilik dan semester tagihan lewat {@code Common.getTahunAkademik(...)}, lalu
	 * ditulis balik ke field. Tiga cabang dievaluasi berurutan:</p>
	 * <ol>
	 *   <li>Calon mahasiswa dengan semester {@code <= 1} &rarr; langsung memakai
	 *       {@link BiodataCalonMahasiswa#getTahunAkademik()}.</li>
	 *   <li>Mahasiswa &rarr; dihitung dari {@link Mahasiswa#getTahunangkatan()},
	 *       {@link Mahasiswa#getPindahKeKampusIniMasukSemester()}, dan
	 *       {@link Mahasiswa#getSemesterMulai()}; hasilnya diformat {@code "N/N+1"}.</li>
	 *   <li>Calon mahasiswa pada semester lain &rarr; dihitung serupa dari
	 *       {@link BiodataCalonMahasiswa#getTahun()} dengan semester mulai {@code 0}.</li>
	 * </ol>
	 *
	 * <p>Penurunan ini memperhitungkan mahasiswa pindahan dan mahasiswa yang memulai kuliah
	 * di semester genap &mdash; keduanya membuat pemetaan semester ke tahun akademik
	 * bergeser, sehingga menyimpan nilai statis akan salah. Konsekuensinya: memperbaiki data
	 * angkatan atau semester mulai seorang mahasiswa akan <b>menggeser tahun akademik seluruh
	 * tagihan lamanya</b> secara surut, termasuk tagihan yang sudah lunas dan sudah
	 * dilaporkan. Bila tidak satu pun cabang cocok (pemilik atau semester tidak diketahui),
	 * nilai tersimpan dipertahankan.</p>
	 *
	 * @return tahun akademik tagihan; {@code null} bila tak dapat diturunkan maupun tersimpan
	 */
	@Column(name = "tahun_akademik", length = 20)
	public String getTahunAkademik() {
		mahasiswa = getMahasiswa();
		calonMahasiswa = getCalonMahasiswa();
		if (calonMahasiswa != null && semster != null && semster <= 1) {
			tahunAkademik = calonMahasiswa.getTahunAkademik();
		} else if (mahasiswa != null && semster != null) {
			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
			Integer tahunAkademikMulai = Common.getTahunAkademik(semster, tahunAngkatanMhs, semesterMulai,
					mahasiswa.getSemesterMulai());
			tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		} else if (calonMahasiswa != null && semster != null) {
			Integer tahunAngkatanMhs = calonMahasiswa.getTahun();
			Integer semesterMulai = 0;
			Integer tahunAkademikMulai = Common.getTahunAkademik(semster, tahunAngkatanMhs, semesterMulai,
					calonMahasiswa.getSemesterMulai());
			tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		}

		return tahunAkademik;
	}

	/**
	 * Setter program. Nilai yang diisi akan ditimpa {@link #getProgram()} pada pembacaan
	 * berikutnya bila pemilik tagihan diketahui.
	 *
	 * @param program program studi/kelas, mis. reguler atau karyawan
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Program yang berlaku bagi tagihan ini (mis. reguler, karyawan).
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Untuk mahasiswa, nilainya diambil dari
	 * {@link HistoryStatusMahasiswa#ambilProgram} &mdash; yaitu program yang berlaku
	 * <i>pada semester tagihan ini</i>, bukan program mahasiswa saat ini. Ini penting dan
	 * benar: mahasiswa yang pindah dari kelas reguler ke karyawan harus tetap ditagih menurut
	 * program yang berlaku di semester bersangkutan. Untuk calon mahasiswa, nilainya diambil
	 * langsung dari berkas calon. Hasilnya ditulis balik ke field yang dipetakan ke kolom.</p>
	 *
	 * <p>Perhatikan bahwa argumen ketiga {@code ambilProgram} memanggil
	 * {@code getMahasiswa().getProgram()} sekali lagi, padahal {@code mahasiswa} sudah
	 * tersedia di variabel lokal &mdash; pemanggilan ganda yang tidak perlu, dan yang pada
	 * gilirannya kembali memicu getter destruktif {@link #getMahasiswa()}.</p>
	 *
	 * @return program yang berlaku; {@code null} bila pemilik tidak diketahui
	 */
	@Column(name = "program", length = 20)
	public String getProgram() {
		mahasiswa = getMahasiswa();
		calonMahasiswa = getCalonMahasiswa();

		if (mahasiswa != null) {
			program = HistoryStatusMahasiswa.ambilProgram(mahasiswa, getSemster(), getMahasiswa().getProgram());
		} else if (calonMahasiswa != null) {
			program = calonMahasiswa.getProgram();
		}

		return program;
	}

	/**
	 * Setter tanggal tagihan. <b>Nilai ini tidak akan pernah terbaca kembali</b>:
	 * {@link #getTanggal()} selalu menimpanya dengan stempel waktu perubahan terakhir.
	 *
	 * @param tanggal tanggal tagihan
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Tanggal tagihan.
	 *
	 * <p><b>GETTER DESTRUKTIF yang membuat kolom ini kehilangan maknanya sendiri.</b> Badan
	 * method hanya berisi {@code tanggal = getTanggal_dirubah();} sebelum mengembalikan
	 * nilainya &mdash; artinya kolom {@code kegiatan.tanggal} <b>selalu</b> ditimpa dengan
	 * stempel waktu perubahan terakhir baris ini, dan penimpaan itu tersimpan karena property
	 * dipetakan ke kolom.</p>
	 *
	 * <p>Akibatnya {@code tanggal} tidak lagi berarti &quot;tanggal tagihan diterbitkan&quot;
	 * melainkan duplikat dari {@code tanggal_dirubah}. Setiap penyentuhan baris &mdash;
	 * termasuk penyentuhan tak sengaja oleh getter destruktif lain di kelas ini &mdash;
	 * menggeser tanggal tagihan ke waktu sekarang. Kode yang memerlukan tanggal terbit
	 * sesungguhnya sebaiknya memakai {@link DetailBiaya#getDefaultTanggalTagihan()} atau
	 * {@link JadwalPembayaran}, sedangkan riwayat pembayaran tersedia lewat
	 * {@link #getTanggalBayarAwal()} dan {@link #getTanggalBayarTerakhir()} yang justru
	 * merupakan getter murni.</p>
	 *
	 * @return stempel waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		tanggal = getTanggal_dirubah();
		return tanggal;
	}

	/**
	 * Setter semester tagihan (perhatikan ejaan {@code semster} tanpa huruf {@code e},
	 * mengikuti nama kolom bawaan).
	 *
	 * @param semster semester tagihan
	 */
	public void setSemster(Integer semster) {
		this.semster = semster;
	}

	/**
	 * Semester yang ditagihkan header ini. Perhatikan ejaan {@code semster} tanpa huruf
	 * {@code e} &mdash; nama kolom bawaan yang dipertahankan demi kompatibilitas.
	 *
	 * <p><b>GETTER DESTRUKTIF terbatas.</b> Bila jenis kegiatannya adalah
	 * {@code ConstantValues.PENDAFTARAN_CALON_MAHASISWA}, field dipaksa {@code 0} dan ditulis
	 * balik &mdash; pendaftaran calon mahasiswa memang terjadi sebelum semester mana pun
	 * dimulai. Di luar itu nilai tersimpan dipertahankan.</p>
	 *
	 * <p><b>Nilai kembalian dan field dapat berbeda.</b> Baris {@code return} memakai ternary
	 * yang mengubah {@code null} menjadi {@code 0} <i>tanpa</i> menulis balik ke field. Jadi
	 * untuk baris yang kolom {@code semster}-nya {@code NULL}, method ini mengembalikan
	 * {@code 0} sementara field tetap {@code null}. Perbedaan itu terasa pada
	 * {@link #generateKodeUnik}, yang membaca <b>field mentah</b> {@code semster} dan
	 * memperlakukan {@code null} sebagai data prasyarat yang tidak lengkap sehingga
	 * mengembalikan {@code null} &mdash; keadaan yang harus ditangani jalur fallback pada
	 * {@link #getKodeunik()}.</p>
	 *
	 * @return semester tagihan; {@code 0} bila kolomnya kosong; tidak pernah {@code null}
	 */
	@Column(name = "semster", length = 20)
	public Integer getSemster() {
		jenisKegiatan = getJenisKegiatan();
		if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
				&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
			semster = 0;
		}
		return semster == null ? 0 : semster;
	}

	/**
	 * Setter penanda validasi pembayaran.
	 *
	 * @param validated penanda validasi
	 */
	public void setValidated(Integer validated) {
		this.validated = validated;
	}

	/**
	 * Penanda bahwa pembayaran tagihan ini sudah divalidasi petugas, disimpan sebagai
	 * {@link Integer} alih-alih {@code Boolean} (mengikuti bentuk kolom lama).
	 *
	 * <p>Getter murni tanpa penjaga ternary, sehingga dapat mengembalikan {@code null} untuk
	 * baris yang belum pernah divalidasi &mdash; pemanggil perlu berjaga terhadap
	 * {@code NullPointerException} saat auto-unboxing. Pendampingnya adalah
	 * {@link #getValidator()} yang mencatat siapa yang memvalidasi.</p>
	 *
	 * @return penanda validasi; {@code null} bila belum divalidasi
	 */
	@Column(name = "validated")
	public Integer getValidated() {
		return validated;
	}

	/**
	 * Setter pemilik tagihan berupa berkas calon mahasiswa.
	 *
	 * @param calonMahasiswa pemilik tagihan; {@code null} bila tagihan milik mahasiswa aktif
	 */
	public void setCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa) {
		this.calonMahasiswa = calonMahasiswa;
	}

	/**
	 * Pemilik tagihan berupa {@link BiodataCalonMahasiswa} (berkas pendaftar yang belum
	 * ber-NIM). Pasangan dari {@link #getMahasiswa()}.
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; dapat mengisi FOREIGN KEY, kebalikan arah dari
	 * {@link #getMahasiswa()}.</b> Bila {@code calonMahasiswa} kosong tetapi
	 * {@code mahasiswa} terisi, <i>dan</i> jenis kegiatannya adalah salah satu dari
	 * {@code PENDAFTARAN_CALON_MAHASISWA} atau {@code PENDAFTARAN_ULANG_MAHASISWA_BARU},
	 * field diisi dengan {@code mahasiswa.getBiodataCalonMahasiswaData()}. Alasannya: kedua
	 * jenis tagihan itu secara konseptual milik berkas calon, sehingga tautannya dipulihkan
	 * walaupun yang tersimpan hanya NIM.</p>
	 *
	 * <p>Berbeda dari {@link #getMahasiswa()} yang menimpa tanpa syarat, pengisian di sini
	 * hanya terjadi saat field kosong &mdash; bentuk auto-seed. Seluruh blok dibungkus
	 * {@code try/catch} yang mencatat lewat {@code ErrorAuditUtil} dan melanjutkan, karena
	 * {@code getBiodataCalonMahasiswaData()} dapat melempar pada data yang tidak utuh;
	 * kegagalan berarti relasi tetap kosong, bukan pembatalan.</p>
	 *
	 * <p>Perhatikan bahwa method ini memanggil {@link #getJenisKegiatan()} di dalam blok
	 * tersebut, sedangkan {@link #getMahasiswa()} memanggil {@code getCalonMahasiswa()} di
	 * awalnya &mdash; keduanya saling memanggil, tetapi tidak sampai menjadi rekursi tak
	 * berujung karena {@code getCalonMahasiswa()} memakai field {@code mahasiswa} yang sudah
	 * dipulihkan {@code check(...)}, bukan getter-nya.</p>
	 *
	 * @return pemilik tagihan berupa berkas calon; {@code null} bila tagihan milik mahasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getCalonMahasiswa() {
		calonMahasiswa = check(calonMahasiswa);
		mahasiswa = check(mahasiswa);
		try {
			if (calonMahasiswa == null && mahasiswa != null) {
				jenisKegiatan = getJenisKegiatan();
				if ((jenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
						&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
						|| (jenisKegiatan != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
								&& jenisKegiatan.getId()
										.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {
					calonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:305");
			// TODO: handle exception
		}
		return calonMahasiswa;
	}

	/**
	 * Setter nomor referensi pembayaran. Nilai yang diisi di sini dihormati
	 * {@link #getRefNumber()}, yang hanya membangkitkan nomor saat field masih kosong.
	 *
	 * @param refNumber nomor referensi
	 */
	public void setRefNumber(String refNumber) {
		this.refNumber = refNumber;
	}

	/**
	 * Nomor referensi pembayaran yang dipakai gateway bank untuk mengaitkan setoran dengan
	 * tagihan ini.
	 *
	 * <p><b>GETTER DESTRUKTIF ber-auto-seed.</b> Bila field kosong, sebuah nomor dibangkitkan
	 * lalu ditulis balik. Rumusnya adalah waktu sekarang dalam milidetik ditambah NIM (untuk
	 * mahasiswa) atau nomor registrasi (untuk calon). Bila penjumlahan itu gagal &mdash;
	 * lazimnya karena NIM/nomor registrasi memuat huruf sehingga {@code Long.parseLong}
	 * melempar &mdash; dipakai bilangan acak besar ditambah waktu sekarang.</p>
	 *
	 * <p><b>Keunikan tidak dijamin.</b> Tidak ada pemeriksaan tabrakan ke basis data maupun
	 * indeks unik pada kolom ini, sehingga keunikan hanya bersandar pada resolusi milidetik
	 * dan keunikan NIM. Dua tagihan milik mahasiswa yang sama yang dibangkitkan dalam
	 * milidetik yang sama akan menerima nomor identik; pada jalur acak, tabrakan bergantung
	 * pada mutu {@link Math#random()}. Perhatikan pula bahwa kolomnya dibatasi 20 karakter
	 * sedangkan nilai hasil penjumlahan dapat lebih panjang, dan bahwa kedua cabang membaca
	 * <b>field mentah</b> {@code mahasiswa}/{@code calonMahasiswa} alih-alih getter-nya
	 * &mdash; sehingga pada entity yang relasinya belum dipulihkan, keduanya dianggap kosong
	 * dan jalur acak yang terpakai.</p>
	 *
	 * @return nomor referensi pembayaran; tidak pernah {@code null} setelah auto-seed
	 */
	@Column(name = "ref_number", length = 20)
	public String getRefNumber() {
		if (refNumber == null || refNumber.trim().isEmpty()) {
			try {
				if (mahasiswa != null) {

					refNumber = new Long(
							ais.ui.util.WaktuUtil.getDate().getTime() + (Long.parseLong(mahasiswa.getNim())))
							.toString();

				} else if (calonMahasiswa != null) {
					refNumber = new Long(ais.ui.util.WaktuUtil.getDate().getTime()
							+ (Long.parseLong(calonMahasiswa.getNoRegistrasi()))).toString();
				}
			} catch (Exception e) {
				int randomInt = (int) (1000000000000000.0 * Math.random());
				refNumber = (randomInt + ais.ui.util.WaktuUtil.getDate().getTime()) + "";
			}
		}
		return refNumber;
	}

	/**
	 * Setter jadwal pembayaran.
	 *
	 * @param jadwalPembayaran jadwal pembayaran yang berlaku; boleh {@code null}
	 */
	public void setJadwalPembayaran(JadwalPembayaran jadwalPembayaran) {
		this.jadwalPembayaran = jadwalPembayaran;
	}

	/**
	 * {@link JadwalPembayaran} yang berlaku bagi tagihan ini &mdash; menetapkan rentang
	 * tanggal pembayaran dan, lewat {@link JadwalPembayaran#getEndDate()}, tenggat yang
	 * dipakai perhitungan denda.
	 *
	 * <p>Perhatikan bahwa relasi ini tidak pernah dibaca dari sini oleh mesin denda:
	 * {@link DetailBiaya#checkDenda} dan {@link DetailBiaya#checkDendaCicilan} menerima
	 * jadwal sebagai <i>parameter</i> dari pemanggilnya, bukan mengambilnya dari header
	 * tagihan. Untuk jalur angsuran, pemanggil produksinya mengoper {@code null} &mdash;
	 * lihat catatan pada {@code checkDendaCicilan}.</p>
	 *
	 * <p>Getter relasi lazy standar dengan {@code check(...)}.</p>
	 *
	 * @return jadwal pembayaran; {@code null} bila tidak ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pembayaran", nullable = true)
	public JadwalPembayaran getJadwalPembayaran() {
		jadwalPembayaran = check(jadwalPembayaran);
		return jadwalPembayaran;
	}

	/**
	 * Setter nama petugas yang memvalidasi pembayaran.
	 *
	 * @param validator nama/identitas petugas validator
	 */
	public void setValidator(String validator) {
		this.validator = validator;
	}

	/**
	 * Nama petugas yang memvalidasi pembayaran tagihan ini; pendamping
	 * {@link #getValidated()}.
	 *
	 * <p>Getter murni yang menampilkan tanda hubung untuk nilai kosong &mdash; bentuk
	 * tampilan saja, tidak ditulis balik ke field, sehingga kolomnya tetap {@code NULL} di
	 * database. Perhatikan bahwa pemanggil tidak dapat membedakan &quot;belum divalidasi&quot;
	 * dari &quot;divalidasi oleh seseorang bernama tanda hubung&quot; hanya dari nilai
	 * kembalian ini; pakai {@link #getValidated()} untuk memeriksa status validasi.</p>
	 *
	 * @return nama validator; {@code "-"} bila belum ada
	 */
	public String getValidator() {
		return validator == null || validator.trim().isEmpty() ? "-" : validator;
	}

	/**
	 * Setter nilai pengurang tagihan.
	 *
	 * @param pengurangan nominal pengurang
	 */
	public void setPengurangan(Double pengurangan) {
		this.pengurangan = pengurangan;
	}

	/**
	 * Nominal pengurang yang diterapkan pada tagihan ini di luar mekanisme diskon
	 * ({@link JenisDiskonMahasiswa}/{@link DiskonMahasiswa}) &mdash; mis. keringanan yang
	 * dicatat langsung pada header.
	 *
	 * <p><b>GETTER DESTRUKTIF ringan (auto-seed literal).</b> {@code null} diisi {@code 0.0}
	 * lalu ditulis balik ke field yang dipetakan ke kolom, sehingga pembacaan pertama dapat
	 * memicu {@code UPDATE} dan revisi Envers. Secara semantik nilainya setara, tetapi
	 * pola penulisannya sama dengan yang lain di kelas ini.</p>
	 *
	 * <p>Perhatikan bahwa nilai ini <b>tidak</b> ikut diperhitungkan oleh
	 * {@link #hitungTagihan()} maupun {@link #getAmountTerhutang()}; keduanya bekerja atas
	 * snapshot JSON {@link #getTagihans()}. Pengurangan karenanya harus sudah tercermin di
	 * dalam JSON tersebut agar berpengaruh pada total &mdash; kolom ini sendiri bersifat
	 * pencatatan.</p>
	 *
	 * @return nominal pengurang; tidak pernah {@code null}
	 */
	public Double getPengurangan() {
		if (pengurangan == null) {
			pengurangan = 0.0;
		}
		return pengurangan;
	}

	/**
	 * Setter keterangan bebas tagihan.
	 *
	 * <p>Perhatikan bahwa anotasi {@code @Column(columnDefinition = "text")} keliru
	 * ditempatkan pada <b>setter</b> ini, bukan pada {@link #getKeterangan()}. Karena kelas
	 * ini memakai <i>property access</i> (anotasi {@code @Id} berada di getter), Hibernate
	 * hanya membaca anotasi pemetaan dari getter &mdash; anotasi di sini <b>diabaikan</b>.
	 * Akibatnya kolom {@code keterangan} dipetakan dengan pengaturan bawaan
	 * ({@code varchar(255)}), bukan sebagai {@code text}. Keterangan yang lebih panjang dari
	 * itu akan ditolak database saat disimpan.</p>
	 *
	 * @param keterangan keterangan bebas
	 */
	@Column(columnDefinition = "text")
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Keterangan bebas yang menyertai tagihan ini.
	 *
	 * <p>Getter murni yang mengembalikan field apa adanya, termasuk {@code null}. Lihat
	 * catatan pada {@link #setKeterangan(String)} mengenai anotasi {@code @Column} yang
	 * salah tempat sehingga kolom ini tidak dipetakan sebagai {@code text}.</p>
	 *
	 * @return keterangan tagihan; {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Setter nominal tagihan. <b>Nilai ini tidak akan pernah terbaca kembali</b>:
	 * {@link #getAmount()} selalu menimpanya dengan {@link #getDibayar()}.
	 *
	 * @param amount nominal
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * Nominal yang tercatat pada header tagihan.
	 *
	 * <p><b>GETTER DESTRUKTIF, dan namanya menyesatkan.</b> Badan method hanya berisi
	 * {@code amount = getDibayar();} sebelum mengembalikannya &mdash; jadi kolom
	 * {@code amount} <b>tidak</b> menyimpan besarnya tagihan, melainkan selalu ditimpa dengan
	 * jumlah yang sudah <i>dibayar</i>. Penimpaan itu tersimpan karena property dipetakan ke
	 * kolom.</p>
	 *
	 * <p>Ini pasangan dari {@link #getTanggal()}, yang juga membuang makna kolomnya sendiri.
	 * Untuk memperoleh angka yang benar-benar dimaksud, pakai: {@link #getTagihan()} untuk
	 * besarnya tagihan, {@link #getDibayar()} untuk yang sudah dibayar, dan
	 * {@link #getAmountTerhutang()} untuk sisanya. Perlu diketahui pula bahwa
	 * {@link #getDibayar()} sendiri membatasi nilainya agar tidak melebihi tagihan, sehingga
	 * {@code amount} ikut terbatasi.</p>
	 *
	 * @return jumlah yang sudah dibayar (bukan nominal tagihan)
	 */
	public Double getAmount() {
		amount = getDibayar();
		return amount;
	}

	/**
	 * Setter status mahasiswa pada saat tagihan ini berlaku.
	 *
	 * @param statusMahasiswa status mahasiswa; boleh {@code null}
	 */
	public void setStatusMahasiswa(StatusMahasiswa statusMahasiswa) {
		this.statusMahasiswa = statusMahasiswa;
	}

	/**
	 * {@link StatusMahasiswa} yang berlaku bagi pemilik tagihan pada semester ini (aktif,
	 * cuti, dan seterusnya).
	 *
	 * <p>Getter relasi lazy standar dengan {@code check(...)} &mdash; <b>tidak destruktif</b>.
	 * Patut dicatat bahwa ia berbeda dari saudaranya {@link #getStatusAwalMahasiswa()}, yang
	 * justru menurunkan nilainya dari {@link HistoryStatusMahasiswa} dan menulisnya balik.
	 * Status pada header ini karena itu merupakan data yang benar-benar tersimpan, bukan
	 * turunan.</p>
	 *
	 * @return status mahasiswa; {@code null} bila tidak dicatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_mahasiswa", nullable = true)
	public StatusMahasiswa getStatusMahasiswa() {
		statusMahasiswa = check(statusMahasiswa);
		return statusMahasiswa;
	}

	/**
	 * Setter sisa terhutang. <b>Nilai ini tidak akan pernah terbaca kembali</b>:
	 * {@link #getAmountTerhutang()} selalu menghitung ulangnya.
	 *
	 * @param amountTerhutang sisa terhutang
	 */
	public void setAmountTerhutang(Double amountTerhutang) {
		this.amountTerhutang = amountTerhutang;
	}

	/**
	 * Sisa tagihan yang belum dibayar, yaitu {@link #getTagihan()} dikurangi
	 * {@link #getDibayar()}.
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Nilainya dihitung ulang setiap kali dibaca dan ditulis
	 * balik ke field yang dipetakan ke kolom {@code amount_terhutang}, sehingga kolom itu
	 * merupakan nilai turunan yang di-<i>cache</i> ke basis data &mdash; berguna untuk
	 * laporan tunggakan yang membaca langsung lewat SQL, tetapi hanya seakurat pembacaan
	 * terakhirnya.</p>
	 *
	 * <p><b>Sifat perhitungannya.</b> Karena kedua operan berasal dari field {@code tagihan}
	 * dan {@code dibayar} &mdash; yang merupakan hasil penguraian snapshot JSON oleh
	 * {@link #hitungTagihan()}/{@link #hitungDibayar()} &mdash; nilai di sini ikut mewarisi
	 * ketertinggalan snapshot itu terhadap keadaan {@link DetailKegiatan} yang sebenarnya.
	 * Perhatikan pula bahwa {@link #getDibayar()} membatasi diri agar tidak melebihi
	 * {@link #getTagihan()}, sehingga hasilnya <b>tidak pernah negatif</b>: kelebihan bayar
	 * tidak akan tampak sebagai angka minus di sini, melainkan sebagai nol.</p>
	 *
	 * <p>Pengisian awal {@code 0.0} untuk field {@code null} dilakukan sebelum perhitungan,
	 * dan seluruh perhitungan dibungkus {@code try/catch} yang mencatat lewat
	 * {@code ErrorAuditUtil} lalu mempertahankan nilai yang ada &mdash; kegagalan berarti
	 * nilai lama, bukan nol.</p>
	 *
	 * @return sisa terhutang; tidak pernah {@code null} dan tidak pernah negatif
	 */
	@Column(name = "amount_terhutang", nullable = true)
	public Double getAmountTerhutang() {
		if (amountTerhutang == null) {
			amountTerhutang = 0.0;
		}
		try {
			amountTerhutang = getTagihan() - getDibayar();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:407");
			// TODO: handle exception
		}
		return amountTerhutang;
	}

	/**
	 * Setter kunci unik alami tagihan. Nilai eksplisit di sini dihormati
	 * {@link #getKodeunik()} untuk entity yang sudah tersimpan, karena getter itu langsung
	 * mengembalikan nilai tersimpan tanpa menghitung ulang.
	 *
	 * @param kodeunik kunci unik alami
	 */
	public void setKodeunik(String kodeunik) {
		this.kodeunik = kodeunik;
	}

	/**
	 * Membentuk <b>kunci unik alami</b> sebuah tagihan dari komponen-komponennya. Versi
	 * statis sehingga dapat dipakai memeriksa keberadaan tagihan sebelum membuatnya.
	 *
	 * <h4>Format</h4>
	 * <ul>
	 *   <li>Calon mahasiswa: {@code CAL_MHS_<idCalon>-<idJenisKegiatan>} ditambah
	 *       {@code _<semester>} bila semesternya lebih dari 1.</li>
	 *   <li>Mahasiswa: {@code MHS_<idMahasiswa>-<idJenisKegiatan>-<semester>}.</li>
	 *   <li>Selain itu: {@code null}.</li>
	 * </ul>
	 * <p>Sesudahnya {@code tambahanKodeUnik} dan {@code _<bulan>} ditempelkan bila terisi,
	 * memungkinkan beberapa tagihan sejenis berdampingan (mis. tagihan bulanan).</p>
	 *
	 * <h4>Yang menentukan keunikan tagihan</h4>
	 * <p>Kolom {@code kodeunik} bertanda {@code unique = true, nullable = false}, sehingga
	 * rumus di sinilah yang secara efektif menjadi <b>aturan bisnis</b> &quot;satu mahasiswa
	 * hanya boleh punya satu tagihan per jenis kegiatan per semester&quot; &mdash; ditegakkan
	 * oleh indeks unik basis data, bukan oleh pemeriksaan di kode.</p>
	 *
	 * <h4>Hal yang perlu diperhatikan</h4>
	 * <p><b>Calon mahasiswa memperlakukan semester 0 dan 1 sebagai satu.</b> Semester baru
	 * ditempelkan bila {@code > 1}, sehingga tagihan calon di semester {@code null},
	 * {@code 0}, dan {@code 1} menghasilkan kunci yang sama persis. Untuk jenis kegiatan
	 * pendaftaran hal itu memang dikehendaki &mdash; keduanya merujuk peristiwa yang sama,
	 * dan {@link #getSemster()} pun memaksa {@code 0} untuk pendaftaran calon mahasiswa.</p>
	 *
	 * <p><b>Kembalian {@code null} bila prasyarat tidak lengkap.</b> Bila pemilik atau jenis
	 * kegiatan kosong &mdash; atau, pada cabang mahasiswa, bila {@code semster} masih
	 * {@code null} &mdash; hasilnya {@code null}. Perhatikan bahwa {@code null} itu kemudian
	 * masih ditempeli {@code tambahanKodeUnik}/{@code bulan} bila keduanya terisi, sehingga
	 * dapat menghasilkan string harfiah yang diawali {@code "null"}. Karena kolomnya
	 * {@code nullable = false}, {@link #getKodeunik()} memasang jalur cadangan berupa barcode
	 * acak untuk mencegah kegagalan {@code INSERT} yang akan membatalkan seluruh transaksi
	 * pembayaran.</p>
	 *
	 * <p><b>Memakai id, bukan NIM.</b> Kunci disusun dari primary key sehingga tetap stabil
	 * ketika NIM atau nomor registrasi berubah.</p>
	 *
	 * @param mahasiswa        pemilik berupa mahasiswa; boleh {@code null}
	 * @param calonMahasiswa   pemilik berupa berkas calon; boleh {@code null}
	 * @param jenisKegiatan    jenis kegiatan yang ditagihkan
	 * @param semster          semester tagihan
	 * @param tambahanKodeUnik imbuhan pembeda; boleh {@code null}/kosong
	 * @param bulan            bulan tagihan untuk tagihan bulanan; boleh {@code null}
	 * @return kunci unik alami; {@code null} bila prasyaratnya tidak lengkap
	 */
	public static String generateKodeUnik(Mahasiswa mahasiswa, BiodataCalonMahasiswa calonMahasiswa,
			JenisKegiatan jenisKegiatan, Integer semster, String tambahanKodeUnik, Integer bulan) {
		String kodeunik = null;
		if (calonMahasiswa != null && jenisKegiatan != null) {
			kodeunik = "CAL_MHS_" + calonMahasiswa.getId() + "-" + jenisKegiatan.getId()
					+ (semster != null && semster > 1 ? "_" + semster : "");
		} else if (mahasiswa != null && jenisKegiatan != null && semster != null) {
			kodeunik = "MHS_" + mahasiswa.getId() + "-" + jenisKegiatan.getId() + "-" + semster;
		} else {
			kodeunik = null;
		}

		if (tambahanKodeUnik != null && !tambahanKodeUnik.trim().isEmpty()) {
			kodeunik = kodeunik + tambahanKodeUnik;
		}
		if (bulan != null) {
			kodeunik = kodeunik + "_" + bulan;
		}

		return kodeunik;
	}

	@Column(name = "kodeunik", unique = true, nullable = false)
	public String getKodeunik() {
		// kodeunik adalah natural key yang sudah dipakai oleh banyak relasi. Entity lama
		// dapat masih menyimpan mahasiswa DAN calonMahasiswa sekaligus; menghitung ulang
		// getter pada setiap flush dapat mengubah MHS_* menjadi CAL_MHS_* dan menabrak
		// baris kegiatan calon yang memang sudah ada. Bekukan nilai yang sudah tersimpan.
		// Entity baru (id null) tetap memakai generator lama, dan setKodeunik eksplisit
		// pada entity existing tetap dihormati karena nilainya langsung dikembalikan.
		if (id != null && kodeunik != null && kodeunik.trim().length() > 0) {
			return kodeunik;
		}
		if (getKodeUnikLain() || !getAktif()) {
			if (kodeunik == null || kodeunik.trim().length() == 0) {
				kodeunik = Common.getGeneratedBarCode();
			}
		} else {
			kodeunik = Kegiatan.generateKodeUnik(mahasiswa, calonMahasiswa, jenisKegiatan, semster, tambahanKodeUnik,
					bulan);
			// FIX "null value in column kodeunik violates not-null constraint": generateKodeUnik()
			// balik null bila data prasyaratnya tidak lengkap (mis. jenisKegiatan atau semster kosong --
			// terlihat pada VirtualAccountBank hasil restore dari Audit yang datanya bisa tidak utuh).
			// Tanpa fallback ini, INSERT/UPDATE Kegiatan gagal total di level DB, membatalkan SELURUH
			// transaksi pembayaran (H2H bank/manual) dengan pesan error yang membingungkan (tampak
			// sbg TransactionException, bukan akar masalah sesungguhnya). Pakai barcode acak sbg
			// fallback terakhir -- pola yang SAMA seperti jalur kodeUnikLain/!aktif di atas -- supaya
			// data pembayaran tetap tersimpan walau kodeunik-nya tak sedeskriptif format normal.
			if (kodeunik == null) {
				kodeunik = Common.getGeneratedBarCode();
			}
		}
		return kodeunik;
	}

	/**
	 * Imbuhan pembeda yang ditempelkan pada kunci unik alami &mdash; memungkinkan beberapa
	 * tagihan sejenis berdampingan untuk mahasiswa dan semester yang sama, yang tanpa imbuhan
	 * ini akan ditolak indeks unik kolom {@code kodeunik}.
	 *
	 * <p>Getter murni. Field-nya diberi nilai awal string kosong pada deklarasi sehingga
	 * {@link #generateKodeUnik} tidak menempelkan apa pun secara bawaan.</p>
	 *
	 * @return imbuhan pembeda; string kosong bila tidak dipakai
	 */
	@Column(name = "tambahan_kode_unik", nullable = true)
	public String getTambahanKodeUnik() {
		return tambahanKodeUnik;
	}

	/**
	 * Setter imbuhan pembeda kunci unik. Perhatikan bahwa mengubahnya pada tagihan yang sudah
	 * tersimpan tidak mengubah {@link #getKodeunik()}, karena kunci sudah dibekukan.
	 *
	 * @param tambahanKodeUnik imbuhan pembeda
	 */
	public void setTambahanKodeUnik(String tambahanKodeUnik) {
		this.tambahanKodeUnik = tambahanKodeUnik;
	}

	/**
	 * Bulan yang ditagihkan, untuk jenis kegiatan yang menagih per bulan alih-alih per
	 * semester. Ikut ditempelkan pada kunci unik alami lewat {@link #generateKodeUnik}.
	 *
	 * <p>Getter murni yang mengembalikan field apa adanya, termasuk {@code null} &mdash; yang
	 * di sini bermakna &quot;tagihan bukan per bulan&quot;, bukan &quot;bulan nol&quot;.</p>
	 *
	 * @return bulan tagihan; {@code null} bila tagihan tidak bersifat bulanan
	 */
	public Integer getBulan() {
		return bulan;
	}

	/**
	 * Setter bulan tagihan.
	 *
	 * @param bulan bulan tagihan; {@code null} bila tidak bersifat bulanan
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Status lunas tagihan ini.
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Nilainya selalu diambil ulang dari
	 * {@link #getApakahLunas()} lalu ditulis balik ke field {@code lunas} yang dipetakan ke
	 * kolom &mdash; jadi kolom {@code lunas} adalah nilai turunan yang di-<i>cache</i> ke
	 * basis data, bukan penanda mandiri yang dapat disetel petugas. Menyimpan {@code false}
	 * lewat {@link #setLunas(Boolean)} tidak akan bertahan.</p>
	 *
	 * <p>Rantainya: {@code getLunas()} &rarr; {@link #getApakahLunas()} &rarr;
	 * {@link #getPersentase()} &rarr; field {@code tagihan}/{@code dibayar}, yang berasal
	 * dari penguraian snapshot JSON. Jadi status lunas ikut mewarisi ketertinggalan snapshot
	 * itu terhadap keadaan {@link DetailKegiatan} yang sebenarnya.</p>
	 *
	 * @return {@code true} bila persentase pelunasan mencapai 100%
	 */
	public Boolean getLunas() {
		lunas = getApakahLunas();
		return lunas;
	}

	/**
	 * Setter status lunas. <b>Nilai ini tidak akan pernah terbaca kembali</b>:
	 * {@link #getLunas()} selalu menghitung ulangnya dari persentase pelunasan.
	 *
	 * @param lunas status lunas
	 */
	public void setLunas(Boolean lunas) {
		this.lunas = lunas;
	}

	/**
	 * Persentase pelunasan tagihan ini (0&ndash;100).
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Selalu diambil ulang dari {@link #getPersentase()} lalu
	 * ditulis balik ke field yang dipetakan ke kolom &mdash; nilai turunan yang di-cache,
	 * sama seperti {@link #getLunas()}.</p>
	 *
	 * <p>Perhatikan bahwa nilainya <b>dibatasi 100%</b> secara tidak langsung: ia dihitung
	 * dari field {@code dibayar}, dan {@link #getDibayar()} tidak pernah melebihi
	 * {@link #getTagihan()}. Untuk mengetahui pelunasan yang sebenarnya termasuk kelebihan
	 * bayar, pakai {@link #hitungPersentaseLunasAktual()}.</p>
	 *
	 * @return persentase pelunasan; tidak pernah melebihi 100
	 */
	public Double getPersentaseLunas() {
		persentaseLunas = getPersentase();
		return persentaseLunas;
	}

	/**
	 * Menghitung persentase pelunasan <b>yang sebenarnya</b>, termasuk kelebihan bayar,
	 * tanpa mengubah keadaan entity.
	 *
	 * <p>Method ini ada karena jalur biasa membatasi diri: {@link #getDibayar()} memangkas
	 * nilai agar tidak melebihi tagihan, sehingga {@link #getPersentaseLunas()} tidak pernah
	 * melampaui 100% dan kelebihan bayar tidak terlihat. Di sini dipakai
	 * {@link #hitungDibayarAktualTanpaBatas()} yang tidak memangkas.</p>
	 *
	 * <h4>Pola simpan-pulihkan</h4>
	 * <p>Baik {@link #hitungTagihan()} maupun {@link #hitungDibayarAktualTanpaBatas()}
	 * menulis ke field {@code tagihan}/{@code dibayar} sebagai efek samping. Untuk mencegah
	 * perhitungan ini mengotori entity, kedua field disalin lebih dulu ({@code tagihanLama},
	 * {@code dibayarLama}) dan <b>dipulihkan</b> sesudahnya. Ini pola yang benar dan patut
	 * dicontoh &mdash; method ini menjadi satu-satunya penghitung di kelas ini yang benar
	 * benar bebas efek samping terhadap field yang dipetakan ke kolom.</p>
	 *
	 * <p>Perlu dicatat bahwa pemulihannya tidak dibungkus {@code try/finally}: bila salah
	 * satu penghitung melempar, field tidak akan dipulihkan. Keduanya sendiri sudah
	 * menangkap seluruh exception secara internal, sehingga dalam praktik jalur itu tidak
	 * tercapai.</p>
	 *
	 * <h4>Aturan hasil</h4>
	 * <ul>
	 *   <li>Tagihan di bawah {@code 0.01}: {@code 100.0} bila ada pembayaran positif,
	 *       selain itu {@code 0.0} &mdash; tagihan nol tanpa pembayaran bukan transaksi
	 *       lunas.</li>
	 *   <li>Dibayar mencapai tagihan (dengan toleransi {@code 0.01} untuk galat pembulatan
	 *       bilangan pecahan): tepat {@code 100.0}, bukan lebih.</li>
	 *   <li>Selain itu: perbandingan biasa dikali seratus.</li>
	 * </ul>
	 * <p>Perhatikan bahwa cabang kedua membulatkan kelebihan bayar menjadi tepat 100%,
	 * sehingga method ini pun tidak melaporkan angka di atas seratus &mdash; yang
	 * dibedakannya dari jalur biasa adalah bahwa <i>tagihan</i> dihitung ulang dari JSON
	 * terkini, bukan bahwa hasilnya boleh melampaui 100.</p>
	 *
	 * @return persentase pelunasan aktual; antara {@code 0.0} dan {@code 100.0}
	 */
	public Double hitungPersentaseLunasAktual() {
		Double tagihanLama = tagihan;
		Double dibayarLama = dibayar;
		Double tagihanHitung = hitungTagihan();
		Double dibayarHitung = hitungDibayarAktualTanpaBatas();
		tagihan = tagihanLama;
		dibayar = dibayarLama;
		double totalTagihan = tagihanHitung == null ? 0.0 : tagihanHitung.doubleValue();
		double totalDibayar = dibayarHitung == null ? 0.0 : dibayarHitung.doubleValue();
		if (totalTagihan < 0.01) {
			return totalDibayar > 0.01 ? 100.0 : 0.0;
		}
		if (totalDibayar + 0.01 >= totalTagihan) {
			return 100.0;
		}
		return (totalDibayar * 100.0) / totalTagihan;
	}

	/**
	 * Setter persentase pelunasan. <b>Nilai ini tidak akan pernah terbaca kembali</b>:
	 * {@link #getPersentaseLunas()} selalu menghitung ulangnya.
	 *
	 * @param persentaseLunas persentase pelunasan
	 */
	public void setPersentaseLunas(Double persentaseLunas) {
		this.persentaseLunas = persentaseLunas;
	}

	/**
	 * Jumlah yang telah dibayar menurut kolomnya sendiri.
	 *
	 * <p><b>Jangan tertukar dengan {@link #getDibayar()}.</b> Keduanya bernama mirip tetapi
	 * berbeda sumber: {@code getDibayar()} membaca field hasil penguraian snapshot JSON
	 * {@link #getBulans()}, sedangkan kolom di sini adalah nilai yang disetel pemanggil dan
	 * <b>tidak pernah diperbarui</b> oleh mesin perhitungan mana pun di kelas ini. Seluruh
	 * logika lunas, persentase, dan sisa terhutang memakai {@code getDibayar()}, bukan kolom
	 * ini. Karena itu kolom ini praktis merupakan <b>field tidur</b> dari sudut pandang
	 * kelas ini, dan nilainya dapat menyimpang dari kenyataan bila ada jalur luar yang
	 * mengisinya.</p>
	 *
	 * <p>Getter destruktif ringan: {@code null} diisi {@code 0.0} lalu ditulis balik.</p>
	 *
	 * @return jumlah telah dibayar menurut kolom; tidak pernah {@code null}
	 */
	public Double getJumlahTelahDibayar() {
		if (jumlahTelahDibayar == null) {
			jumlahTelahDibayar = 0.0;
		}
		return jumlahTelahDibayar;
	}

	/**
	 * Setter jumlah telah dibayar (kolom mandiri; lihat catatan pada getter-nya).
	 *
	 * @param jumlahTelahDibayar jumlah telah dibayar
	 */
	public void setJumlahTelahDibayar(Double jumlahTelahDibayar) {
		this.jumlahTelahDibayar = jumlahTelahDibayar;
	}

	/**
	 * Berkas {@link UploadVirtualAccount} yang menghasilkan tagihan ini &mdash; menautkan
	 * header ke proses unggah massal virtual account yang membentuknya.
	 *
	 * <p>Getter <b>murni sepenuhnya</b>: tidak memanggil {@code check(...)} seperti relasi
	 * lain di kelas ini, melainkan mengembalikan field apa adanya. Konsekuensinya, pada
	 * entity yang sudah <i>detached</i> pemanggil dapat menerima proxy yang belum
	 * terinisialisasi dan melempar saat dipakai &mdash; risiko yang justru diredam
	 * {@code check(...)} di getter relasi lainnya.</p>
	 *
	 * <p>Perhatikan pula {@code @Fetch(FetchMode.SELECT)} dan tidak adanya
	 * {@code fetch = FetchType.LAZY} pada {@code @ManyToOne}-nya: relasi ini karena itu
	 * bersifat <i>eager</i> dan diambil lewat SELECT terpisah setiap kali sebuah
	 * {@code Kegiatan} dimuat. Pada pemuatan daftar tagihan yang panjang, ini menghasilkan
	 * pola kueri N+1.</p>
	 *
	 * @return berkas unggah virtual account asal; {@code null} bila tagihan tidak berasal
	 *         dari unggahan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "upload_virtual_account", nullable = true)
	public UploadVirtualAccount getUploadVirtualAccount() {
		return uploadVirtualAccount;
	}

	/**
	 * Setter berkas unggah virtual account asal.
	 *
	 * @param uploadVirtualAccount berkas unggah asal; boleh {@code null}
	 */
	public void setUploadVirtualAccount(UploadVirtualAccount uploadVirtualAccount) {
		this.uploadVirtualAccount = uploadVirtualAccount;
	}

	/**
	 * Penanda bahwa tagihan ini memakai kunci unik <b>di luar format baku</b>.
	 *
	 * <p>Bila {@code true}, {@link #getKodeunik()} tidak membentuk kunci
	 * {@code MHS_*}/{@code CAL_MHS_*} melainkan memakai barcode acak
	 * ({@code Common.getGeneratedBarCode()}). Ini melepaskan tagihan dari aturan &quot;satu
	 * per jenis kegiatan per semester&quot; yang ditegakkan indeks unik &mdash; dipakai untuk
	 * tagihan insidental yang memang boleh berulang. Perhatikan bahwa {@link #getAktif()}
	 * yang bernilai {@code false} menghasilkan efek yang sama.</p>
	 *
	 * <p>Getter murni dengan penjaga ternary ({@code null} dibaca sebagai {@code false}),
	 * tanpa menulis balik ke field.</p>
	 *
	 * @return {@code true} bila memakai kunci unik di luar format baku; tidak pernah {@code null}
	 */
	public Boolean getKodeUnikLain() {
		return kodeUnikLain == null ? false : kodeUnikLain;
	}

	/**
	 * Setter penanda kunci unik di luar format baku.
	 *
	 * @param kodeUnikLain status penanda
	 */
	public void setKodeUnikLain(Boolean kodeUnikLain) {
		this.kodeUnikLain = kodeUnikLain;
	}

	/**
	 * Nominal denda keterlambatan yang tercatat pada header tagihan ini; {@code null} dibaca
	 * sebagai {@code 0.0}.
	 *
	 * <p>Getter murni (ternary saja, tanpa menulis balik) &mdash; patut dicatat karena
	 * banyak kerabatnya di kelas ini justru destruktif.</p>
	 *
	 * <p>Perhitungan dendanya sendiri tidak terjadi di sini melainkan di
	 * {@link DetailBiaya#checkDenda} (pembayaran sekaligus) dan
	 * {@link DetailBiaya#checkDendaCicilan} (angsuran, yang menuliskan hasilnya ke
	 * {@link CicilanPembayaran}). Pembebasan denda per baris dicatat pada
	 * {@link #getPembatalanDenda()}. Seperti {@link #getPengurangan()}, nilai di sini
	 * <b>tidak</b> ikut diperhitungkan {@link #hitungTagihan()}, yang bekerja atas snapshot
	 * JSON {@link #getTagihans()}.</p>
	 *
	 * @return nominal denda; tidak pernah {@code null}
	 */
	public Double getDenda() {
		return denda == null ? 0.0 : denda;
	}

	/**
	 * Setter nominal denda pada header tagihan.
	 *
	 * @param denda nominal denda
	 */
	public void setDenda(Double denda) {
		this.denda = denda;
	}

	/**
	 * Mengambil seluruh {@link CicilanPembayaran} (angsuran) yang tercatat terhadap tagihan
	 * ini.
	 *
	 * <p>Pekerjaannya didelegasikan ke pemilik tagihan &mdash;
	 * {@link BiodataCalonMahasiswa#ambilCicilanPembayaran(Kegiatan)} bila tagihan milik calon,
	 * atau {@link Mahasiswa#ambilCicilanPembayaran(Kegiatan)} bila milik mahasiswa. Pola
	 * &quot;calon dulu, baru mahasiswa&quot; ini konsisten dengan seluruh method
	 * {@code ambil*} di kelas ini; urutannya penting karena sebuah tagihan pendaftaran dapat
	 * menunjuk keduanya sekaligus, dan berkas calonlah pemilik yang benar dalam hal itu.</p>
	 *
	 * <p>Bila kedua pemilik kosong, dikembalikan daftar kosong &mdash; bukan {@code null},
	 * sehingga pemanggil dapat langsung melakukan iterasi. Perhatikan bahwa method ini
	 * memicu {@link #getCalonMahasiswa()} dan {@link #getMahasiswa()} yang keduanya dapat
	 * mengisi/menimpa foreign key pemilik.</p>
	 *
	 * @return daftar angsuran; kosong bila tidak ada atau pemilik tak diketahui
	 */
	public List<CicilanPembayaran> ambilCicilan() {
		if (getCalonMahasiswa() != null) {
			return getCalonMahasiswa().ambilCicilanPembayaran(this);
		} else if (getMahasiswa() != null) {
			return getMahasiswa().ambilCicilanPembayaran(this);
		}
		return new ArrayList<CicilanPembayaran>();
	}

	/**
	 * Menghitung total angsuran dan total denda yang tercatat terhadap tagihan ini, sebagai
	 * pasangan nilai.
	 *
	 * <p>Seperti {@link #ambilCicilan()}, pekerjaannya didelegasikan ke pemilik tagihan lewat
	 * {@code hitungTotalCicilanDanDendaPembayaran(this)}. Bila kedua pemilik kosong,
	 * dikembalikan {@code {0.0, 0.0}} &mdash; bukan {@code null}.</p>
	 *
	 * <p><b>Kembalian berupa larik dua elemen tanpa nama.</b> Indeks {@code 0} adalah total
	 * angsuran dan indeks {@code 1} adalah total denda; urutan itu hanya terbaca dari nilai
	 * bawaan pada baris terakhir dan dari implementasi di kelas pemilik, tidak dari tipe
	 * kembaliannya. Pemanggil perlu berhati-hati untuk tidak mempertukarkannya.</p>
	 *
	 * <p>Anotasi {@code @SuppressWarnings({})} pada method ini berdaftar kosong sehingga tidak
	 * menekan peringatan apa pun &mdash; sisa penyuntingan yang tidak berpengaruh.</p>
	 *
	 * @return larik dua elemen: {@code [totalAngsuran, totalDenda]}; tidak pernah {@code null}
	 */
	@SuppressWarnings({})
	public Double[] hitungTotalDanDendaFromCicilan() {
		if (getCalonMahasiswa() != null) {
			return getCalonMahasiswa().hitungTotalCicilanDanDendaPembayaran(this);
		} else if (getMahasiswa() != null) {
			return getMahasiswa().hitungTotalCicilanDanDendaPembayaran(this);
		}
		return new Double[] { 0.0, 0.0 };
	}

	/**
	 * Mengambil seluruh {@link DetailKegiatan} (baris rincian) milik tagihan ini, tanpa
	 * memuat ulang dari basis data. Pintasan untuk {@code ambilDetailKegiatan(false)}.
	 *
	 * @return kumpulan baris rincian; kosong bila tidak ada atau pemilik tak diketahui
	 */
	public Collection<DetailKegiatan> ambilDetailKegiatan() {
		return ambilDetailKegiatan(false);
	}

	/**
	 * Mengambil seluruh {@link DetailKegiatan} milik tagihan ini, dengan pilihan memuat
	 * ulang dari basis data.
	 *
	 * <p>Seperti seluruh method {@code ambil*} di kelas ini, pekerjaannya didelegasikan ke
	 * pemilik tagihan &mdash; berkas calon lebih dulu, baru mahasiswa. Bila kedua pemilik
	 * kosong, dikembalikan daftar kosong, bukan {@code null}.</p>
	 *
	 * <p><b>Perhatikan bahwa ini BUKAN relasi Hibernate.</b> {@code Kegiatan} tidak memetakan
	 * koleksi {@code DetailKegiatan}; pengambilannya dilakukan lewat kueri di kelas pemilik.
	 * Konsekuensinya baris rincian tidak ikut ter-<i>cascade</i> saat header disimpan atau
	 * dihapus, dan jumlah baris yang dikembalikan di sini tidak harus sepadan dengan daftar
	 * id pada {@link #getDetailKegiatans()} maupun dengan snapshot JSON
	 * {@link #getTagihans()} yang menjadi dasar total.</p>
	 *
	 * @param refresh {@code true} untuk memaksa pemuatan ulang dari basis data
	 * @return kumpulan baris rincian; kosong bila tidak ada atau pemilik tak diketahui
	 */
	public Collection<DetailKegiatan> ambilDetailKegiatan(boolean refresh) {
		if (getCalonMahasiswa() != null) {
			return getCalonMahasiswa().ambilDetailKegiatan(this, refresh);
		} else if (getMahasiswa() != null) {
			return getMahasiswa().ambilDetailKegiatan(this, refresh);
		}
		return new ArrayList<DetailKegiatan>();
	}

	/**
	 * Mengambil baris-baris rincian tagihan ini yang berasal dari satu {@link DetailBiaya}
	 * tertentu.
	 *
	 * <p>Berguna ketika satu komponen biaya menghasilkan lebih dari satu baris &mdash; mis.
	 * pembayaran bertahap, di mana tiap tahap punya {@link DetailBiaya#getBayarKe()} sendiri.
	 * Bandingkan dengan {@link #ambilSatuDetailKegiatan(DetailBiaya, boolean)} yang justru
	 * mengambil tepat satu baris lewat kunci unik.</p>
	 *
	 * @param detailBiaya komponen biaya penyaring
	 * @param refresh     {@code true} untuk memaksa pemuatan ulang dari basis data
	 * @return daftar baris rincian; kosong bila tidak ada atau pemilik tak diketahui
	 */
	public List<DetailKegiatan> ambilDetailKegiatan(DetailBiaya detailBiaya, boolean refresh) {
		if (getCalonMahasiswa() != null) {
			return getCalonMahasiswa().ambilDetailKegiatan(this, detailBiaya, refresh);
		} else if (getMahasiswa() != null) {
			return getMahasiswa().ambilDetailKegiatan(this, detailBiaya, refresh);
		}
		return new ArrayList<DetailKegiatan>();
	}

	/**
	 * Mencari kembali sebuah {@link DetailKegiatan} yang <b>setara</b> dengan yang dioper,
	 * berdasarkan kunci uniknya &mdash; dipakai untuk memperoleh instance yang benar-benar
	 * tersimpan dan terikat session, dari sebuah objek yang mungkin lepas atau baru dibentuk.
	 *
	 * <p>Kunci unik disusun {@link DetailKegiatan#kodeUnik} dari salah satu dari dua
	 * kombinasi, sesuai isi objek masukan:</p>
	 * <ul>
	 *   <li>bila {@code detailKegiatan} punya {@link PengaturanPembayaranBulanan} &mdash;
	 *       dari pengaturan itu beserta item biaya dan {@code bayarKe} milik
	 *       {@link DetailBiaya}-nya;</li>
	 *   <li>bila tidak, tetapi punya item biaya sendiri &mdash; dari item biaya itu beserta
	 *       {@code bayarKe} milik {@code DetailBiaya}-nya.</li>
	 * </ul>
	 * <p>Kedua cabang menyertakan {@code this} sebagai header dan
	 * {@link DetailKegiatan#getKegiatanTemporary()} sebagai header staging &mdash; sehingga
	 * baris rincian yang masih bernaung di bawah {@link KegiatanTemporary} tetap dapat
	 * ditemukan.</p>
	 *
	 * <p><b>Objek masukan dikembalikan apa adanya bila tak satu pun cabang cocok.</b> Bila
	 * {@code detailKegiatan} tidak punya pengaturan bulanan maupun item biaya, tidak ada
	 * pencarian yang dilakukan dan objek yang sama dikembalikan &mdash; bukan {@code null}.
	 * Pemanggil karenanya tidak dapat membedakan &quot;ditemukan&quot; dari &quot;tidak
	 * dicari&quot; hanya dari nilai kembaliannya. Perhatikan pula bahwa kedua cabang
	 * mendereferensi {@code detailKegiatan.getDetailBiaya()} tanpa pemeriksaan {@code null};
	 * kolom {@code detail_biaya} memang bertanda {@code nullable = false}, sehingga hal itu
	 * aman untuk baris yang benar-benar tersimpan namun tidak untuk objek yang baru dibentuk
	 * di memori.</p>
	 *
	 * @param detailKegiatan baris rincian acuan; boleh {@code null}
	 * @param session        session Hibernate; boleh {@code null}/tertutup
	 * @return baris rincian tersimpan yang setara; objek masukan bila tidak dicari
	 */
	public DetailKegiatan ambilByKodeUnik(DetailKegiatan detailKegiatan, Session session) {

		if (detailKegiatan != null && detailKegiatan.getPengaturanPembayaranBulanan() != null) {
			String kodeUnik = DetailKegiatan.kodeUnik(detailKegiatan.getPengaturanPembayaranBulanan(),
					detailKegiatan.getDetailBiaya().getItemBiaya(), detailKegiatan.getDetailBiaya().getBayarKe(), this,
					detailKegiatan.getKegiatanTemporary());
			detailKegiatan = ambilByKodeUnik(kodeUnik, session);
		} else if (detailKegiatan != null && detailKegiatan.getItemBiaya() != null) {
			String kodeUnik = DetailKegiatan.kodeUnik(null, detailKegiatan.getItemBiaya(),
					detailKegiatan.getDetailBiaya().getBayarKe(), this, detailKegiatan.getKegiatanTemporary());
			detailKegiatan = ambilByKodeUnik(kodeUnik, session);
		}

		return detailKegiatan;
	}

	/**
	 * Mencari satu {@link DetailKegiatan} berdasarkan kunci uniknya, dengan cache dan
	 * pengelolaan session sendiri. Inti pencarian baris rincian di kelas ini.
	 *
	 * <h4>Pengelolaan session</h4>
	 * <p>Bila session yang dioper {@code null} atau sudah tertutup, method membuka session
	 * <b>dedikasi</b> sendiri dan menutupnya di {@code finally}. Session milik pemanggil
	 * tidak pernah ditutup &mdash; ditandai {@code sessionDibukaSendiri}. Pemeriksaan
	 * {@code isOpen()} pun dibungkus {@code try/catch} karena dapat melempar pada proxy yang
	 * terputus. Kehati-hatian ini menghindari &quot;Session is closed!&quot; yang pernah
	 * muncul ketika pola lama menutup session milik request.</p>
	 *
	 * <h4>Cache statis {@link #mappingId}</h4>
	 * <p>Pencarian pertama-tama menengok peta statis {@code Map<idKegiatan, Map<kodeUnik,
	 * idDetailKegiatan>>}. Bila kunci ditemukan, entity diambil lewat
	 * {@link GeneralValueObject#ambilData}.</p>
	 *
	 * <p><b>Cache ini statis dan tidak pernah dibersihkan.</b> Ia tumbuh sepanjang umur
	 * aplikasi seiring bertambahnya tagihan yang tersentuh, tanpa batas ukuran, tanpa masa
	 * kedaluwarsa, dan tanpa penguncian &mdash; sebuah {@link java.util.HashMap} biasa yang
	 * dibaca dan ditulis banyak thread permintaan sekaligus. Selain berpotensi menahan memori,
	 * penulisan bersamaan pada {@code HashMap} tidak aman. Cache ini juga tidak dibatalkan
	 * ketika baris rincian dihapus atau dibentuk ulang, sehingga dapat menyimpan id yang
	 * sudah tidak ada; jalur pemulihannya adalah {@code ambilData} mengembalikan {@code null}
	 * sehingga pencarian dilanjutkan ke basis data.</p>
	 *
	 * <h4>Pencarian ke basis data</h4>
	 * <p>Bila cache meleset, dilakukan {@code createCriteria} atas {@code kodeUnik}. Sengaja
	 * <b>tidak</b> memakai {@code uniqueResult()} melainkan {@code order by id desc} dengan
	 * {@code setMaxResults(1)}. Alasannya terdokumentasi pada komentar di dalam badan method:
	 * proses hitung ulang tagihan dapat meninggalkan lebih dari satu baris rincian berkunci
	 * sama, dan {@code uniqueResult()} akan melempar sehingga hasilnya {@code null} &mdash;
	 * yang membuat perhitungan tagihan melewatkan diskon dan menghasilkan nilai bruto,
	 * berganti-ganti dengan nilai neto tergantung cache meleset atau tidak. Mengambil baris
	 * terbaru membuat hasilnya deterministik sekaligus mendorong hitung ulang memakai ulang
	 * baris terbaru alih-alih menumpuk duplikat.</p>
	 *
	 * <p>Perhatikan bahwa {@link DetailBiaya#hitungTotalKegiatan(Kegiatan, Session)}
	 * menghadapi masalah duplikat yang sama tetapi <b>tanpa</b> pengurutan, sehingga di sana
	 * baris yang terpilih tidak deterministik.</p>
	 *
	 * <p>Seluruh badan dibungkus {@code try/catch} yang mencetak jejak dan mencatat lewat
	 * {@code ErrorAuditUtil}, lalu mengembalikan {@code null} &mdash; kegagalan pencarian
	 * berarti &quot;tidak ada&quot;, bukan pembatalan.</p>
	 *
	 * @param kodeUnik kunci unik baris rincian yang dicari
	 * @param session  session Hibernate; boleh {@code null}/tertutup
	 * @return baris rincian yang ditemukan; {@code null} bila tidak ada atau terjadi galat
	 */
	public DetailKegiatan ambilByKodeUnik(String kodeUnik, Session session) {

		DetailKegiatan detailKegiatan = null;

		// Pertahanan "Session is closed!": bila session yang dioper pemanggil null/closed, buka session
		// DEDIKASI sendiri (openSession) lalu tutup di finally. Session milik pemanggil TIDAK ditutup di
		// sini — hanya yang kita buka sendiri (flag sessionDibukaSendiri). Menghindari createCriteria di
		// bawah melempar SessionException.
		boolean sessionDibukaSendiri = false;
		try {
			if (session == null || !session.isOpen()) {
				session = HibernateUtil.openSession();
				sessionDibukaSendiri = true;
			}
		} catch (Throwable t) {
			session = HibernateUtil.openSession();
			sessionDibukaSendiri = true;
		}

		try {

			boolean belumada = false;

			Map<String, Long> kegMap = mappingId.get(getId());
			if (kegMap == null) {
				kegMap = new HashMap<String, Long>();
				mappingId.put(getId(), kegMap);
			}
			Long idData = kegMap.get(kodeUnik);
			if (idData != null) {
				belumada = true;
				detailKegiatan = (DetailKegiatan) GeneralValueObject.ambilData(DetailKegiatan.class, idData.toString());
//				System.out.println("ambilSatuDetailKegiatan-> langkah 1 " + detailKegiatan);
			}

			if (detailKegiatan == null) {
				// PENTING: JANGAN pakai uniqueResult() di sini. Recompute tagihan ("Hitung Ulang")
				// bisa MENINGGALKAN >1 DetailKegiatan dengan kodeUnik yang SAMA (duplikat). Bila ada
				// duplikat, uniqueResult() melempar NonUniqueResultException -> tertangkap -> kembali
				// NULL -> ambilJumlahTagihan(null,...) MELEWATI diskon -> tagihan BRUTO. Saat cache
				// 'mappingId' kebetulan hit, hasilnya NETO. Hit/miss bergantian -> tagihan BOLAK-BALIK
				// BENAR(neto)/SALAH(bruto). Ambil DETERMINISTIK: DetailKegiatan TERBARU (id desc) —
				// yaitu yang baru saja di-recompute + diberi diskon — sehingga konsisten & sekaligus
				// membuat recompute MEMAKAI ULANG DK terbaru (tidak menumpuk duplikat baru).
				java.util.List<?> hasilKodeUnik = session.createCriteria(DetailKegiatan.class)
						.add(Restrictions.eq("kodeUnik", kodeUnik))
						.addOrder(org.hibernate.criterion.Order.desc("id")).setMaxResults(1).list();
				if (hasilKodeUnik != null && !hasilKodeUnik.isEmpty()) {
					detailKegiatan = (DetailKegiatan) hasilKodeUnik.get(0);
				}
//				System.out.println("ambilSatuDetailKegiatan-> langkah 2 " + detailKegiatan);
			}

			if (detailKegiatan != null) {
				kegMap.put(kodeUnik, detailKegiatan.getId());
				if (belumada) {
					GeneralValueObject.masukkanData(DetailKegiatan.class, detailKegiatan);
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:657");
		} finally {
			if (sessionDibukaSendiri && session != null) {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
		return detailKegiatan;
	}

	/**
	 * Mengambil satu baris rincian untuk sebuah {@link PengaturanPembayaranBulanan}, dengan
	 * session dedikasi yang dibuka dan ditutup sendiri.
	 *
	 * <p>Sengaja memakai {@link HibernateUtil#openSession()} alih-alih session milik request:
	 * session dedikasi terisolasi sehingga menutupnya tidak meracuni request, dan karena
	 * ditutup di {@code finally} ia juga tidak membocorkan koneksi dari kolam c3p0.</p>
	 *
	 * <p>Kegagalan apa pun dicatat lewat {@code ErrorAuditUtil} dan menghasilkan {@code null}.</p>
	 *
	 * @param pengaturanPembayaranBulanan pengaturan bulanan acuan
	 * @param detailKegiatansTemp         kumpulan baris rincian sementara; diteruskan ke
	 *                                    overload ber-session, yang pada praktiknya tidak
	 *                                    memakainya
	 * @return baris rincian yang ditemukan; {@code null} bila tidak ada atau terjadi galat
	 */
	public DetailKegiatan ambilSatuDetailKegiatan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Collection<DetailKegiatan> detailKegiatansTemp) {
		DetailKegiatan detailKegiatan = null;
		// Pakai session DEDIKASI (openSession) yang WAJIB ditutup di finally (closeSessionQuietly) —
		// BUKAN currentNativeSession milik request. Session dedikasi terisolasi: tidak meracuni request
		// (menutup session request = "Session is closed!" di akses berikutnya) dan tidak bocor pool c3p0
		// karena ditutup sendiri di sini.
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			detailKegiatan = ambilSatuDetailKegiatan(pengaturanPembayaranBulanan, detailKegiatansTemp, session);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:677");
			// diabaikan: kembalikan null bila gagal
		} finally {
			if (session != null) {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
		return detailKegiatan;
	}

	/**
	 * Cache statis pemetaan {@code idKegiatan -> (kodeUnik -> idDetailKegiatan)}, dipakai
	 * {@link #ambilByKodeUnik(String, Session)} untuk menghindari kueri berulang saat sebuah
	 * tagihan dihitung ulang baris demi baris.
	 *
	 * <p><b>Perlu diperlakukan dengan hati-hati.</b> Field ini {@code public static} dan
	 * bersifat non-final dalam arti isinya dapat diubah siapa pun; ia berupa
	 * {@link HashMap} biasa yang dibaca dan ditulis dari banyak thread permintaan tanpa
	 * penguncian, tidak pernah dibersihkan, tidak berbatas ukuran, dan tidak dibatalkan saat
	 * baris rincian berubah atau terhapus. Lihat uraian lengkapnya pada
	 * {@link #ambilByKodeUnik(String, Session)}.</p>
	 */
	public static Map<Long, Map<String, Long>> mappingId = new HashMap<Long, Map<String, Long>>();

	/**
	 * Mengambil satu baris rincian untuk sebuah {@link PengaturanPembayaranBulanan} memakai
	 * session yang dioper pemanggil.
	 *
	 * <p>Kunci unik disusun {@link DetailKegiatan#kodeUnik} dari pengaturan bulanan tersebut
	 * beserta item biaya dan {@code bayarKe} milik {@link DetailBiaya}-nya, dengan
	 * {@code this} sebagai header dan {@code null} sebagai header staging.</p>
	 *
	 * <p><b>Parameter {@code detailKegiatansTemp} tidak dipakai.</b> Kumpulan baris rincian
	 * sementara yang diterima tidak pernah dirujuk di badan method; pencarian selalu menempuh
	 * {@link #ambilByKodeUnik(String, Session)}. Parameter itu tampaknya sisa dari rancangan
	 * lama yang menyaring dari koleksi di memori lebih dulu.</p>
	 *
	 * <p>Perhatikan pula bahwa {@code pengaturanPembayaranBulanan.getDetailBiaya()}
	 * didereferensi tanpa pemeriksaan {@code null}; pengaturan bulanan yang belum tertaut
	 * komponen biaya akan melempar, tertangkap {@code try/catch} di sekitarnya, dan
	 * menghasilkan {@code null}.</p>
	 *
	 * @param pengaturanPembayaranBulanan pengaturan bulanan acuan
	 * @param detailKegiatansTemp         <b>tidak dipakai</b>
	 * @param session                     session Hibernate
	 * @return baris rincian yang ditemukan; {@code null} bila tidak ada atau terjadi galat
	 */
	public DetailKegiatan ambilSatuDetailKegiatan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Collection<DetailKegiatan> detailKegiatansTemp, Session session) {

		DetailKegiatan detailKegiatan = null;

		try {
			String kodeUnik = DetailKegiatan.kodeUnik(pengaturanPembayaranBulanan,
					pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya(),
					pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe(), this, null);

			detailKegiatan = ambilByKodeUnik(kodeUnik, session);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:702");
		}

		return detailKegiatan;
	}

	/**
	 * Mengambil baris-baris rincian tagihan ini yang berasal dari sebuah
	 * {@link PengaturanPembayaranBulanan}, dengan pilihan memuat ulang dari basis data.
	 *
	 * @param pengaturanPembayaranBulanan pengaturan bulanan penyaring
	 * @param refresh                     {@code true} untuk memaksa pemuatan ulang
	 * @return daftar baris rincian; kosong bila tidak ada atau pemilik tak diketahui
	 */
	public List<DetailKegiatan> ambilDetailKegiatan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			boolean refresh) {
		if (getCalonMahasiswa() != null) {
			return getCalonMahasiswa().ambilDetailKegiatan(this, pengaturanPembayaranBulanan, refresh);
		} else if (getMahasiswa() != null) {
			return getMahasiswa().ambilDetailKegiatan(this, pengaturanPembayaranBulanan, refresh);
		}
		return new ArrayList<DetailKegiatan>();
	}

	/**
	 * Mengambil baris-baris rincian tagihan ini untuk sebuah
	 * {@link PengaturanPembayaranBulanan}, dengan kumpulan baris sementara sebagai bahan
	 * pertimbangan bagi kelas pemilik.
	 *
	 * <p>Berbeda dari overload ber-{@code refresh}, di sini pemilik menerima koleksi yang
	 * sudah ada di memori sehingga dapat menghindari kueri ulang &mdash; berguna di dalam
	 * gelung hitung ulang yang sudah memuat seluruh baris rincian sekali di awal.</p>
	 *
	 * @param pengaturanPembayaranBulanan pengaturan bulanan penyaring
	 * @param detailKegiatansTemp         kumpulan baris rincian yang sudah dimuat
	 * @return daftar baris rincian; kosong bila tidak ada atau pemilik tak diketahui
	 */
	public List<DetailKegiatan> ambilDetailKegiatan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Collection<DetailKegiatan> detailKegiatansTemp) {
		if (getCalonMahasiswa() != null) {
			return getCalonMahasiswa().ambilDetailKegiatan(this, pengaturanPembayaranBulanan, detailKegiatansTemp);
		} else if (getMahasiswa() != null) {
			return getMahasiswa().ambilDetailKegiatan(this, pengaturanPembayaranBulanan, detailKegiatansTemp);
		}
		return new ArrayList<DetailKegiatan>();
	}

	/**
	 * Mengambil satu baris rincian untuk sebuah {@link DetailBiaya}, tanpa memuat ulang.
	 * Pintasan untuk {@code ambilSatuDetailKegiatan(detailBiaya, false)}.
	 *
	 * @param detailBiaya komponen biaya acuan
	 * @return baris rincian yang ditemukan; {@code null} bila tidak ada
	 */
	public DetailKegiatan ambilSatuDetailKegiatan(DetailBiaya detailBiaya) {
		return ambilSatuDetailKegiatan(detailBiaya, false);
	}

	/**
	 * Mengambil satu baris rincian untuk sebuah {@link DetailBiaya}, dengan session dedikasi
	 * yang dibuka dan ditutup sendiri.
	 *
	 * <p>Overload inilah yang dipanggil {@link #ambilJumlahTagihan(Kegiatan, DetailBiaya,
	 * boolean)}, sehingga ia berjalan sangat sering &mdash; sekali untuk setiap baris tagihan
	 * yang dihitung. Karena setiap pemanggilan membuka session Hibernate baru, menghitung
	 * ulang seluruh tagihan seorang mahasiswa membuka sebanyak itu pula session; masing-masing
	 * memang ditutup di {@code finally} sehingga tidak bocor, tetapi biayanya nyata pada
	 * pemrosesan massal. Pemanggil yang sudah memegang session sebaiknya memakai
	 * {@link #ambilSatuDetailKegiatan(DetailBiaya, boolean, Session)}.</p>
	 *
	 * @param detailBiaya komponen biaya acuan
	 * @param refresh     {@code true} untuk memaksa pemuatan ulang
	 * @return baris rincian yang ditemukan; {@code null} bila tidak ada atau terjadi galat
	 */
	public DetailKegiatan ambilSatuDetailKegiatan(DetailBiaya detailBiaya, boolean refresh) {
		DetailKegiatan detailKegiatan = null;
		// Pakai session DEDIKASI (openSession) yang ditutup di finally — bukan currentNativeSession
		// milik request. Overload ini dipanggil luas via ambilJumlahTagihan(...); session terisolasi
		// menghindari peracunan request sekaligus dijamin tertutup (tak bocor pool c3p0).
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			detailKegiatan = ambilSatuDetailKegiatan(detailBiaya, refresh, session);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:741");
			// diabaikan
		} finally {
			if (session != null) {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
		return detailKegiatan;
	}

	/**
	 * Mengambil satu baris rincian untuk sebuah {@link DetailBiaya} memakai session yang
	 * dioper pemanggil, tanpa memuat ulang.
	 *
	 * @param detailBiaya komponen biaya acuan
	 * @param session     session Hibernate
	 * @return baris rincian yang ditemukan; {@code null} bila tidak ada
	 */
	public DetailKegiatan ambilSatuDetailKegiatan(DetailBiaya detailBiaya, Session session) {
		return ambilSatuDetailKegiatan(detailBiaya, false, session);
	}

	/**
	 * Mengambil satu baris rincian untuk sebuah {@link DetailBiaya} memakai session yang
	 * dioper pemanggil &mdash; bentuk terlengkap dari keluarga ini.
	 *
	 * <p>Kunci unik disusun {@link DetailKegiatan#kodeUnik} dari item biaya dan
	 * {@code bayarKe} milik komponen biaya, dengan {@code this} sebagai header, serta
	 * {@code null} untuk pengaturan bulanan dan header staging.</p>
	 *
	 * <p>Penjaga di awal mengembalikan {@code null} bila {@code detailBiaya} atau item
	 * biayanya kosong &mdash; ditambahkan karena pemanggil seperti
	 * {@code CommonReportHelper.genSklMap} dapat mengoper elemen yang bukan
	 * {@link DetailBiaya} maupun {@link PengaturanPembayaranBulanan}, yang sebelumnya
	 * menghasilkan {@code NullPointerException} di baris penyusunan kunci.</p>
	 *
	 * <p><b>Parameter {@code refresh} tidak dipakai.</b> Ia diterima demi keseragaman tanda
	 * tangan dengan overload lain, tetapi tidak pernah dirujuk; pencarian selalu menempuh
	 * {@link #ambilByKodeUnik(String, Session)} yang punya kebijakan cache sendiri. Karena
	 * itu mengoper {@code true} tidak melewati cache statis {@link #mappingId} sebagaimana
	 * mungkin diharapkan pemanggil.</p>
	 *
	 * @param detailBiaya komponen biaya acuan; boleh {@code null}
	 * @param refresh     <b>tidak dipakai</b>
	 * @param session     session Hibernate
	 * @return baris rincian yang ditemukan; {@code null} bila tidak ada atau terjadi galat
	 */
	public DetailKegiatan ambilSatuDetailKegiatan(DetailBiaya detailBiaya, boolean refresh, Session session) {
		DetailKegiatan detailKegiatan = null;

		// detailBiaya bisa null (mis. dari CommonReportHelper.genSklMap saat elemen
		// bukan PengaturanPembayaranBulanan maupun DetailBiaya) — guard di sini
		// mencegah NPE saat detailBiaya.getItemBiaya() diakses di bawah.
		if (detailBiaya == null || detailBiaya.getItemBiaya() == null) {
			return null;
		}

		try {
			String kodeUnik = DetailKegiatan.kodeUnik(null, detailBiaya.getItemBiaya(), detailBiaya.getBayarKe(), this,
					null);

			detailKegiatan = ambilByKodeUnik(kodeUnik, session);

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:764");
//			e.printStackTrace();
		}
		return detailKegiatan;
	}

	/**
	 * Menyimpan koreksi nominal {@code biaya} sebuah DetailKegiatan secara TERISOLASI: memakai session
	 * dedikasi {@link HibernateUtil#openSession()} dengan transaksi sendiri, lalu ditutup di
	 * {@code finally}. Ini MENGGANTI pola lama yang menulis lewat {@code currentNativeSession()} lalu
	 * menutupnya ({@code disconnect()+close()+closeSession()}) — pola itu meracuni session milik
	 * request sehingga akses DB berikutnya pada request yang sama melempar "Session is closed!"
	 * (mis. saat menghitung tagihan calon mahasiswa saat login PMB). Memakai HQL bulk-update by id agar
	 * tidak menautkan entity ke dua session (hindari NonUniqueObject) dan tidak menyentuh session request.
	 */
	private static void simpanKoreksiBiayaTerisolasi(DetailKegiatan detailKegiatan, Double biaya) {
		if (detailKegiatan == null || detailKegiatan.getId() == null || biaya == null) {
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.getTransaction().begin();
			session.createQuery("update DetailKegiatan set biaya = :biaya where id = :id")
					.setParameter("biaya", biaya).setLong("id", detailKegiatan.getId()).executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null) {
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:796");
				}
			}
		} finally {
			if (session != null) {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
	}

	public static Double ambilJumlahTagihan(Kegiatan kegiatan, DetailBiaya detailBiaya) {
		return ambilJumlahTagihan(kegiatan, detailBiaya, false);
	}

	public static Double ambilJumlahTagihan(Kegiatan kegiatan, DetailBiaya detailBiaya, boolean refresh) {
		DetailKegiatan detailKegiatan = (detailBiaya == null || kegiatan == null || kegiatan.getId() == null) ? null
				: kegiatan.ambilSatuDetailKegiatan(detailBiaya, refresh);
		return ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, refresh);
	}

	public static Double ambilJumlahTagihan(DetailKegiatan detailKegiatan, Kegiatan kegiatan, DetailBiaya detailBiaya,
			boolean refresh) {
			Double jumlah = 0.0;
			try {
				if (detailBiaya == null || detailBiaya.getItemBiaya() == null) {
					return jumlah;
				}

				if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
					return jumlah;
				}

				Double nominalTerkunci = kegiatan == null ? null
						: kegiatan.ambilNominalTagihanTerkunci(detailBiaya, null, detailKegiatan);
				if (nominalTerkunci != null) {
					return nominalTerkunci;
				}

			// Untuk item dgn penghitungan PERKALIAN (mis. "(50.000) x N matakuliah/SKS"),
			// nilai tagihan yang sah adalah HASIL perkalian (nilaiBiayaBaru), BUKAN nilai
			// per-unit (getNilaiBiaya) yang tampil dicoret. Bila hasil perkalian belum
			// terhitung (nilaiBiayaBaru null), hitung dulu agar perkalian 0 menghasilkan 0
			// (50.000 x 0 = 0) — bukan keliru memakai/menyimpan nilai per-unit yang dicoret.
			if (detailBiaya.getItemBiaya() != null
					&& !detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)
					&& detailBiaya.getNilaiBiayaBaru() == null && kegiatan != null
					&& kegiatan.getMahasiswa() != null) {
				try {
					detailBiaya.updateKeterangan(kegiatan.getMahasiswa(), kegiatan.getSemster());
				} catch (Exception eUpd) { ais.common.ErrorAuditUtil.record(eUpd, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:837");
				}
			}

			Double ni = detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)
					? detailBiaya.getNilaiBiaya()
					: (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru());

			jumlah = detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
					? detailBiaya.getTunggakanLalu()
					: (detailKegiatan != null ? detailKegiatan.getBiaya() : ni);

			try {
				if (detailBiaya != null && detailBiaya.getItemBiaya() != null
						&& detailBiaya.getItemBiaya().getParameterTambahan() != null) {
					if (kegiatan != null && kegiatan.getCalonMahasiswa() != null) {
						jumlah = kegiatan.getCalonMahasiswa()
								.ambilSkor(detailBiaya.getItemBiaya().getParameterTambahan()).doubleValue();
					} else if (kegiatan != null && kegiatan.getMahasiswa() != null) {
						jumlah = kegiatan.getMahasiswa().ambilBiodata()
								.ambilSkor(detailBiaya.getItemBiaya().getParameterTambahan()).doubleValue();
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:862");
			}

			try {
				if ((jumlah == null || jumlah.intValue() == 0) && ni.intValue() != 0) {
					jumlah = ni;
					if (detailKegiatan != null && detailKegiatan.getId() != null
							&& jumlah.intValue() != detailKegiatan.getBiaya().intValue()) {
						detailKegiatan.setBiaya(jumlah);
						simpanKoreksiBiayaTerisolasi(detailKegiatan, jumlah);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:875");
			}

			Double diskon = hitungDiskon(detailKegiatan, kegiatan, detailBiaya, jumlah);

//			System.out.println("jumlah -> " + jumlah + ", diskon -> " + diskon);

			jumlah = jumlah - diskon;

			if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
				jumlah = 0.0;
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:890");
		}

		if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
			jumlah = -Math.abs(jumlah);
		}

		return jumlah;
	}

	public static Double ambilJumlahTagihan(Kegiatan kegiatan, Collection<DetailKegiatan> detailKegiatans,
			Mahasiswa mahasiswa, Integer semester, PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		DetailBiaya detailBiaya = pengaturanPembayaranBulanan == null ? null
				: pengaturanPembayaranBulanan.getDetailBiaya();
		DetailKegiatan detailKegiatan = (kegiatan == null || kegiatan.getId() == null
				|| pengaturanPembayaranBulanan == null) ? null
						: kegiatan.ambilSatuDetailKegiatan(pengaturanPembayaranBulanan, detailKegiatans);

		return ambilJumlahTagihan(detailKegiatan, detailBiaya, kegiatan, mahasiswa, semester,
				pengaturanPembayaranBulanan);
	}

	public static Double ambilJumlahTagihan(DetailKegiatan detailKegiatan, DetailBiaya detailBiaya, Kegiatan kegiatan,
			Mahasiswa mahasiswa, Integer semester, PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		Double jumlah = 0.0;
		try {
			if (detailBiaya == null || detailBiaya.getItemBiaya() == null
					|| pengaturanPembayaranBulanan == null) {
				return jumlah;
			}

			if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
				return jumlah;
			}

			Double nominalTerkunci = kegiatan == null ? null : kegiatan.ambilNominalTagihanTerkunci(
					detailBiaya, pengaturanPembayaranBulanan, detailKegiatan);
			if (nominalTerkunci != null) {
				return nominalTerkunci;
			}

			Double ni = pengaturanPembayaranBulanan.getNominal();
			if (mahasiswa != null && semester != null) {
				ni = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
			}

			jumlah = detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
					? detailBiaya.getTunggakanLalu()
					: (detailKegiatan != null ? detailKegiatan.getBiaya() : ni);

			try {
				if (detailBiaya != null && detailBiaya.getItemBiaya() != null
						&& detailBiaya.getItemBiaya().getParameterTambahan() != null) {
					if (kegiatan != null && kegiatan.getCalonMahasiswa() != null) {
						jumlah = kegiatan.getCalonMahasiswa()
								.ambilSkor(detailBiaya.getItemBiaya().getParameterTambahan()).doubleValue();
					} else if (kegiatan != null && kegiatan.getMahasiswa() != null) {
						jumlah = kegiatan.getMahasiswa().ambilBiodata()
								.ambilSkor(detailBiaya.getItemBiaya().getParameterTambahan()).doubleValue();
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:941");
			}

//			System.out.println("ni -> " + ni + ", jumlah -> " + jumlah);

			if ((jumlah == null || jumlah.intValue() == 0) && ni.intValue() != 0) {
				jumlah = ni;
				if (detailKegiatan != null && detailKegiatan.getId() != null
						&& detailKegiatan.getBiaya().intValue() != jumlah.intValue()) {
					detailKegiatan.setBiaya(jumlah);
					simpanKoreksiBiayaTerisolasi(detailKegiatan, jumlah);
				}
			}

			Double diskon = hitungDiskon(detailKegiatan, kegiatan, detailBiaya, jumlah);
			jumlah = jumlah - diskon;

			if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
				jumlah = 0.0;
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:964");
		}

		if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
			jumlah = -Math.abs(jumlah);
		}

		return jumlah;
	}

	public static Double ambilJumlahTagihan(Kegiatan kegiatan, Collection<DetailKegiatan> detailKegiatans,
			Integer semester, PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		DetailBiaya detailBiaya = pengaturanPembayaranBulanan == null ? null
				: pengaturanPembayaranBulanan.getDetailBiaya();
		DetailKegiatan detailKegiatan = (kegiatan == null || kegiatan.getId() == null
				|| pengaturanPembayaranBulanan == null) ? null
						: kegiatan.ambilSatuDetailKegiatan(pengaturanPembayaranBulanan, detailKegiatans);

		return ambilJumlahTagihan(detailKegiatan, detailBiaya, kegiatan, semester, pengaturanPembayaranBulanan);
	}

	public static Double ambilJumlahTagihan(DetailKegiatan detailKegiatan, DetailBiaya detailBiaya, Kegiatan kegiatan,
			Integer semester, PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
			Double jumlah = 0.0;
			try {
				if (detailBiaya == null || detailBiaya.getItemBiaya() == null
						|| pengaturanPembayaranBulanan == null) {
					return jumlah;
				}

				if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
					return jumlah;
				}

				Double nominalTerkunci = kegiatan == null ? null : kegiatan.ambilNominalTagihanTerkunci(
						detailBiaya, pengaturanPembayaranBulanan, detailKegiatan);
				if (nominalTerkunci != null) {
					return nominalTerkunci;
				}

				Double ni = pengaturanPembayaranBulanan.getNominal();
				if (kegiatan != null && kegiatan.getMahasiswa() != null && semester != null) {
					ni = pengaturanPembayaranBulanan.ambilNominalModifikasi(kegiatan.getMahasiswa(), semester);
				}

			jumlah = detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
					? detailBiaya.getTunggakanLalu()
					: (detailKegiatan != null ? detailKegiatan.getBiaya() : ni);

			try {
				if (detailBiaya != null && detailBiaya.getItemBiaya() != null
						&& detailBiaya.getItemBiaya().getParameterTambahan() != null) {
					if (kegiatan != null && kegiatan.getCalonMahasiswa() != null) {
						jumlah = kegiatan.getCalonMahasiswa()
								.ambilSkor(detailBiaya.getItemBiaya().getParameterTambahan()).doubleValue();
					} else if (kegiatan != null && kegiatan.getMahasiswa() != null) {
						jumlah = kegiatan.getMahasiswa().ambilBiodata()
								.ambilSkor(detailBiaya.getItemBiaya().getParameterTambahan()).doubleValue();
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:1014");
			}

			if ((jumlah == null || jumlah.intValue() == 0) && ni.intValue() != 0) {
				jumlah = ni;
				if (detailKegiatan != null && detailKegiatan.getId() != null) {
					detailKegiatan.setBiaya(jumlah);
					simpanKoreksiBiayaTerisolasi(detailKegiatan, jumlah);
				}
			}
			Double diskon = hitungDiskon(detailKegiatan, kegiatan, detailBiaya, jumlah);
			jumlah = jumlah - diskon;

			if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
				jumlah = 0.0;
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kegiatan.java:1033");
		}

		if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
			jumlah = -Math.abs(jumlah);
		}

		return jumlah;
	}

	/**
	 * Simpan nilai diskon hasil perhitungan jenisDiskonMahasiswa (kelompok mahasiswa /
	 * jenis seleksi) ke DetailKegiatan. TANPA ini diskon hanya dihitung lokal lalu dibuang
	 * sehingga TAGIHAN TIDAK TERPOTONG (pemanggil membaca detailKegiatan.getDiskon()).
	 * Hanya menulis ke DB bila nilai berubah & DetailKegiatan sudah tersimpan (idempoten).
	 */
	private static void simpanDiskonDetailKegiatan(DetailKegiatan detailKegiatan, Double diskon) {
		if (detailKegiatan == null) {
			return;
		}
		Double nilaiDiskon = diskon == null ? 0.0 : diskon;
		Double lama = detailKegiatan.getDiskon() == null ? 0.0 : detailKegiatan.getDiskon();
		if (lama.intValue() == nilaiDiskon.intValue()) {
			return;
		}
		detailKegiatan.setDiskon(nilaiDiskon);
		if (detailKegiatan.getId() == null) {
			return;
		}
		// Persist diskon lewat session DEDIKASI (openSession) yang di-commit sendiri lalu ditutup di
		// finally (closeSessionQuietly). Aman ditutup karena BUKAN session request -> tak mengganggu
		// iterasi berikutnya pada loop recompute. DetailKegiatan tak punya @Version -> tak ada bentrok
		// versi walau objek juga dikelola session request.
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.getTransaction().begin();
			Common.refreshUpdate(session, detailKegiatan);
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null) {
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1078");
				}
			}
			// Diskon in-memory sudah ter-set; kegagalan persist diabaikan.
		} finally {
			if (session != null) {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
	}

	public static Double hitungDiskon(DetailKegiatan detailKegiatan, Kegiatan kegiatan, DetailBiaya detailBiaya,
			Double jumlah) {
		Double diskonTerhitung = 0.0;
		try {
			// detailBiaya bisa null (mis. dipanggil dari CommonReportHelper.genSklMap saat
			// elemen bukan PengaturanPembayaranBulanan maupun DetailBiaya). Hanya cabang
			// pertama di bawah yang mengecek detailBiaya != null; cabang calonMahasiswa/
			// mahasiswa di dalam else-nya langsung men-dereference detailBiaya.getItemBiaya()
			// tanpa jaga-jaga → NPE. Guard di sini agar seluruh diskon dilewati dgn aman
			// (bukan menghitung diskon dari data biaya yang tak diketahui).
			if (detailBiaya == null) {
				return 0.0;
			}
			/* PENYERAGAMAN 21-08-2026: keempat rute diskon di kelas ini semula memakai
			 * cocokTanggalBerlakuUntuk() yang HANYA memeriksa rentang tanggal, sedangkan
			 * DetailKegiatan -- yang menghitung angka untuk baris tagihan yang sama --
			 * memakai cocokUntukKegiatan() yang juga menghormati filter Fakultas, Jurusan,
			 * Program, dan Status Awal. Akibatnya kedua mesin dapat menghasilkan potongan
			 * berbeda untuk tagihan yang sama. Kini keduanya memakai pemeriksaan yang sama. */
			if (kegiatan != null && kegiatan.getMahasiswa() != null && detailBiaya != null
					&& detailBiaya.getItemBiaya() != null && kegiatan.getMahasiswa().getKelompokMahasiswa() != null
					&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtMulai() <= kegiatan.getSemster()
					&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtSampai() >= kegiatan.getSemster()

					&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa() != null
					/* FIX 21-08-2026: rute Kelompok Mahasiswa memeriksa batas SEMESTER tetapi tidak pernah
					 * memeriksa rentang TANGGAL berlaku jenis diskonnya, sehingga promo berbatas waktu di
					 * jalur ini tidak pernah berhenti sendiri. Rute Jenis Seleksi dan Gelombang Pendaftaran
					 * di bawah sudah memeriksanya; pemeriksaan yang sama ditambahkan di sini agar ketiga
					 * rute berperilaku konsisten. */
					&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa()
							.cocokUntukKegiatan(kegiatan, detailBiaya)
					&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
					&& !kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
							.isEmpty()
					&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
							.contains(detailBiaya.getItemBiaya().getId())) {
				Double jumlahDiskon = jumlah;
				Double diskon = 0.0;
				diskon += (kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getBerupaPersen()
						? (jumlahDiskon
								* (kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getDiskon()
										/ 100.0))
						: kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getDiskon());
				diskonTerhitung = diskon;
					simpanDiskonDetailKegiatan(detailKegiatan, diskon);
				jumlahDiskon = jumlahDiskon - diskon;
			} else {

				if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
						&& kegiatan.getCalonMahasiswa().getJenisSeleksi() != null
						&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
						&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
						// FIX 19-08-2026: rute CALON mahasiswa dahulu TIDAK memeriksa Tanggal Mulai/
						// Sampai Berlaku, sehingga promo berbatas waktu tetap memotong setelah lewat.
						&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
								.cocokUntukKegiatan(kegiatan, detailBiaya)
						&& !kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.isEmpty()
						&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.contains(detailBiaya.getItemBiaya().getId())

						&&

						(

						kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
								.getSemesterMulai() == null
								|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
										.getSemesterMulai() != null
										&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
												.getSemesterMulai() <= kegiatan.getSemster())

						)

						&&

						(

						kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
								.getSemesterSampai() == null
								|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
										.getSemesterSampai() != null
										&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
												.getSemesterSampai() >= kegiatan.getSemster())

						)

				) {

					Double jumlahDiskon = jumlah;
					Double diskon = 0.0;
					diskon += (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
							.getBerupaPersen()
									? (jumlahDiskon * (kegiatan.getCalonMahasiswa().getJenisSeleksi()
											.getJenisDiskonMahasiswa().getDiskon() / 100.0))
									: kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getDiskon());
					diskonTerhitung = diskon;
					jumlahDiskon = jumlahDiskon - diskon;
					simpanDiskonDetailKegiatan(detailKegiatan, diskon);

				}

				else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa() != null
						&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
						// FIX 19-08-2026: lihat catatan tanggal berlaku pada rute Jenis Seleksi di atas.
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
								.cocokUntukKegiatan(kegiatan, detailBiaya)
						&& !kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.isEmpty()
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.contains(detailBiaya.getItemBiaya().getId())

						&&

						(

						kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
								.getSemesterMulai() == null
								|| (kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
										.getSemesterMulai() != null
										&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
												.getSemesterMulai() <= kegiatan.getSemster())

						)

						&&

						(

						kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
								.getSemesterSampai() == null
								|| (kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
										.getSemesterSampai() != null
										&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
												.getSemesterSampai() >= kegiatan.getSemster())

						)

				) {

					Double jumlahDiskon = jumlah;
					Double diskon = 0.0;
					diskon += (kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
							.getBerupaPersen()
									? (jumlahDiskon * (kegiatan.getCalonMahasiswa().getGelombangPendaftaran()
											.getJenisDiskonMahasiswa().getDiskon() / 100.0))
									: kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getJenisDiskonMahasiswa()
											.getDiskon());
					diskonTerhitung = diskon;
					jumlahDiskon = jumlahDiskon - diskon;
					simpanDiskonDetailKegiatan(detailKegiatan, diskon);

				}

				else if (kegiatan != null && kegiatan.getMahasiswa() != null
						&& kegiatan.getMahasiswa().getJenisSeleksi() != null
						&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
						&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
						/* FIX 21-08-2026: rute Jenis Seleksi milik mahasiswa AKTIF adalah satu-satunya
						 * rute di kelas ini yang masih melewatkan pemeriksaan Tanggal Mulai/Sampai
						 * Berlaku, sehingga promo berbatas waktu tidak pernah berhenti sendiri di
						 * jalur ini. Disamakan dengan rute Kelompok, Jenis Seleksi calon, dan
						 * Gelombang Pendaftaran. */
						&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
								.cocokUntukKegiatan(kegiatan, detailBiaya)
						&& !kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.isEmpty()
						&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.contains(detailBiaya.getItemBiaya().getId())

						&&

						(

						kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterMulai() == null
								|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
										.getSemesterMulai() != null
										&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
												.getSemesterMulai() <= kegiatan.getSemster())

						)

						&&

						(

						kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterSampai() == null
								|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
										.getSemesterSampai() != null
										&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
												.getSemesterSampai() >= kegiatan.getSemster())

						)

				) {

					Double jumlahDiskon = jumlah;
					Double diskon = 0.0;
					diskon += (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getBerupaPersen()
							? (jumlahDiskon
									* (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getDiskon()
											/ 100.0))
							: kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getDiskon());
					diskonTerhitung = diskon;
						simpanDiskonDetailKegiatan(detailKegiatan, diskon);
					jumlahDiskon = jumlahDiskon - diskon;

				}

				// PROMO GLOBAL (perbaikan 19-08-2026): jenis diskon ber-centang "Berlaku Untuk
				// Semua Mahasiswa" kini benar-benar diterapkan tanpa perlu ditautkan ke Gelombang
				// Pendaftaran / Jenis Seleksi. Ditempatkan SETELAH seluruh rute tautan di atas
				// (tautan eksplisit tetap diprioritaskan) dan SEBELUM blok diskon per-orang.
				// Seluruh filter pada form dihormati: tanggal berlaku, Fakultas (Institusi),
				// Jurusan (Prodi), Program, Status Awal, batas semester, dan item biaya.
				else if (kegiatan != null && !(detailKegiatan != null && detailKegiatan.adaDiskon())
						&& JenisDiskonMahasiswa.cariPromoGlobal(kegiatan, detailBiaya, jumlah) != null) {

					JenisDiskonMahasiswa promoGlobal = JenisDiskonMahasiswa.cariPromoGlobal(kegiatan, detailBiaya,
							jumlah);
					Double jumlahDiskon = jumlah;
					Double diskon = promoGlobal.getBerupaPersen()
							? (jumlahDiskon * (promoGlobal.getDiskon() / 100.0))
							: promoGlobal.getDiskon();
					// Potongan tidak boleh melebihi nominal tagihan baris ini.
					if (diskon > jumlahDiskon) {
						diskon = jumlahDiskon;
					}
					diskonTerhitung = diskon;
					simpanDiskonDetailKegiatan(detailKegiatan, diskon);

				}

				else {

					Session session = null;
					try {
					session = HibernateUtil.openSession();

					List<DiskonMahasiswa> diskonMahasiswaDatas = kegiatan == null || kegiatan.getId() == null
							? new ArrayList<DiskonMahasiswa>()
							: ConstantValues.simpleList(session.createCriteria(DiskonMahasiswa.class)
									.addOrder(Order.asc("id"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

									.add(Restrictions.or(Restrictions.eq("mahasiswa", kegiatan.getMahasiswa()),
											Restrictions.eq("biodataCalonMahasiswa", kegiatan.getCalonMahasiswa())))

									.add(Restrictions.or(Restrictions.eq("itemBiaya", detailBiaya.getItemBiaya()),
											Restrictions.or(Restrictions.eq("itemBiaya2", detailBiaya.getItemBiaya()),
													Restrictions.or(
															Restrictions.eq("itemBiaya3", detailBiaya.getItemBiaya()),
															Restrictions.or(
																	Restrictions.eq("itemBiaya4",
																			detailBiaya.getItemBiaya()),
																	Restrictions.eq("itemBiaya5",
																			detailBiaya.getItemBiaya()))))))

									.add(Restrictions.sqlRestriction(kegiatan.getSemster()
											+ " between this_.semestermulai and this_.semestersampai"))

									, DiskonMahasiswa.class);

					if (detailKegiatan != null) {

						List<Long> sudahAda = new ArrayList<Long>();
						for (DiskonMahasiswa diskonMahasiswa : diskonMahasiswaDatas) {
							sudahAda.add(diskonMahasiswa.getId());
						}

						if (detailKegiatan.getDiskonMahasiswaData() != null
								&& !sudahAda.contains(detailKegiatan.getDiskonMahasiswaData().getId())) {
							diskonMahasiswaDatas.add(detailKegiatan.getDiskonMahasiswaData());
						}
						if (detailKegiatan.getDiskonMahasiswaData2() != null
								&& !sudahAda.contains(detailKegiatan.getDiskonMahasiswaData2().getId())) {
							diskonMahasiswaDatas.add(detailKegiatan.getDiskonMahasiswaData2());
						}
						if (detailKegiatan.getDiskonMahasiswaData3() != null
								&& !sudahAda.contains(detailKegiatan.getDiskonMahasiswaData3().getId())) {
							diskonMahasiswaDatas.add(detailKegiatan.getDiskonMahasiswaData3());
						}
					}

					Double jumlahDiskon = jumlah;
					Double diskon = 0.0;
					for (DiskonMahasiswa diskonMahasiswaData : diskonMahasiswaDatas) {
						// Hitung delta diskon iterasi ini berdasarkan sisa jumlah (bukan akumulatif).
						// Bug lama: jumlahDiskon = jumlahDiskon - diskon (diskon akumulatif) →
						// iterasi ke-2 dst. mengurangi terlalu besar → total diskon LEBIH KECIL.
						double deltaDiskon = diskonMahasiswaData.getJenisDiskonMahasiswa().getBerupaPersen()
								? (jumlahDiskon * (diskonMahasiswaData.getJenisDiskonMahasiswa().getDiskon() / 100.0))
								: diskonMahasiswaData.getJenisDiskonMahasiswa().getDiskon();
						diskon += deltaDiskon;
						jumlahDiskon = jumlahDiskon - deltaDiskon;
					}
					diskonTerhitung = diskon;

					if (detailKegiatan != null) {

						DiskonMahasiswa d1 = diskonMahasiswaDatas.size() > 0 ? diskonMahasiswaDatas.get(0) : null;
						DiskonMahasiswa d2 = diskonMahasiswaDatas.size() > 1 ? diskonMahasiswaDatas.get(1) : null;
						DiskonMahasiswa d3 = diskonMahasiswaDatas.size() > 2 ? diskonMahasiswaDatas.get(2) : null;

						if ((detailKegiatan.getDiskon().intValue() != diskon.intValue())

								||

								(detailKegiatan.getDiskonMahasiswaData() == null && d1 != null) ||

								(detailKegiatan.getDiskonMahasiswaData() != null && d1 == null)

								||

								(detailKegiatan.getDiskonMahasiswaData() != null && d1 != null
										&& !detailKegiatan.getDiskonMahasiswaData().getId().equals(d1.getId()))

								||

								(detailKegiatan.getDiskonMahasiswaData2() == null && d2 != null) ||

								(detailKegiatan.getDiskonMahasiswaData2() != null && d2 == null)

								||

								(detailKegiatan.getDiskonMahasiswaData2() != null && d2 != null
										&& !detailKegiatan.getDiskonMahasiswaData2().getId().equals(d2.getId()))

								||

								(detailKegiatan.getDiskonMahasiswaData3() == null && d3 != null) ||

								(detailKegiatan.getDiskonMahasiswaData3() != null && d3 == null)

								||

								(detailKegiatan.getDiskonMahasiswaData3() != null && d3 != null
										&& !detailKegiatan.getDiskonMahasiswaData3().getId().equals(d3.getId()))

						) {
							detailKegiatan.setDiskon(diskon);
							detailKegiatan.setDiskonMahasiswaData(d1);
							detailKegiatan.setDiskonMahasiswaData2(d2);
							detailKegiatan.setDiskonMahasiswaData3(d3);

							simpanDiskonDenganRetry(detailKegiatan.getId(), diskon, d1, d2, d3);
						}
					}
					} finally {
						if (session != null) {
							HibernateUtil.closeSessionQuietly(session);
						}
				}
			}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1334");
			// TODO: handle exception
		}
		if (detailKegiatan != null && detailKegiatan.getDiskon() != null
				&& detailKegiatan.getDiskon().doubleValue() > diskonTerhitung.doubleValue()) {
			diskonTerhitung = detailKegiatan.getDiskon();
		}
		return diskonTerhitung == null ? 0.0 : diskonTerhitung;
	}

	private static void simpanDiskonDenganRetry(Long detailId, Double diskon, DiskonMahasiswa d1,
			DiskonMahasiswa d2, DiskonMahasiswa d3) throws Exception {
		if (detailId == null) return;
		Exception last = null;
		for (int attempt = 0; attempt < 3; attempt++) {
			Session updateSession = null;
			org.hibernate.Transaction tx = null;
			try {
				updateSession = HibernateUtil.openSession();
				tx = updateSession.beginTransaction();
				DetailKegiatan managed = (DetailKegiatan) updateSession.get(DetailKegiatan.class, detailId);
				if (managed == null) return;
				managed.setDiskon(diskon);
				managed.setDiskonMahasiswaData(d1);
				managed.setDiskonMahasiswaData2(d2);
				managed.setDiskonMahasiswaData3(d3);
				updateSession.flush();
				tx.commit();
				return;
			} catch (Exception error) {
				last = error;
				try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) {
					ais.common.ErrorAuditUtil.record(ignored, "auto-audit src/ais/database/model/Kegiatan.java:diskon-rollback");
				}
				if (!isLockTimeout(error) || attempt == 2) throw error;
				try {
					long jitter = (long) (Math.random() * 250L);
					Thread.sleep(350L * (attempt + 1) + jitter);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw interrupted;
				}
			} finally {
				HibernateUtil.closeSessionQuietly(updateSession);
			}
		}
		if (last != null) throw last;
	}

	private static boolean isLockTimeout(Throwable error) {
		Throwable current = error;
		while (current != null) {
			String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
			if (current instanceof java.sql.SQLException) {
				String state = ((java.sql.SQLException) current).getSQLState();
				if ("55P03".equals(state) || "57014".equals(state) || "40P01".equals(state)) return true;
			}
			if (message.indexOf("lock timeout") >= 0 || message.indexOf("could not obtain lock") >= 0
					|| message.indexOf("statement timeout") >= 0
					|| message.indexOf("canceling statement due to") >= 0
					|| message.indexOf("deadlock detected") >= 0) return true;
			current = current.getCause();
		}
		return false;
	}

	public Boolean getAktif() {

		if (getJenisKegiatan() != null
				&& (jenisKegiatan.getMinSmt() > getSemster() || jenisKegiatan.getMaxSmt() < getSemster())) {
			aktif = false;
		}

		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	private static String jsonObject = new JSONObject().toString();

	/**
	 * Snapshot nominal hasil koreksi manual per baris tagihan. Disimpan sebagai JSON agar nilai yang
	 * telah disetujui petugas tidak kembali mengikuti nilai master yang dapat berubah. Struktur ini
	 * juga menyimpan alasan, pelaku, waktu, dan riwayat perubahan.
	 */
	@Column(name = "nominal_tagihan_kunci_json", nullable = true, columnDefinition = "text")
	public String getNominalTagihanKunciJson() {
		return nominalTagihanKunciJson;
	}

	public void setNominalTagihanKunciJson(String nominalTagihanKunciJson) {
		this.nominalTagihanKunciJson = nominalTagihanKunciJson;
	}

	/** Kunci stabil untuk snapshot nominal, tidak bergantung pada id DetailKegiatan yang bisa dibuat ulang. */
	public static String kodeNominalTagihanTerkunci(DetailBiaya detailBiaya,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, DetailKegiatan detailKegiatan) {
		if (pengaturanPembayaranBulanan != null && pengaturanPembayaranBulanan.getId() != null) {
			return "bulanan:" + pengaturanPembayaranBulanan.getId();
		}
		if (detailBiaya != null && detailBiaya.getId() != null) {
			return "detailBiaya:" + detailBiaya.getId();
		}
		if (detailKegiatan != null && detailKegiatan.getId() != null) {
			return "detailKegiatan:" + detailKegiatan.getId();
		}
		return null;
	}

	/** Mengambil nominal final yang pernah dikunci petugas; {@code null} berarti belum pernah diedit. */
	public Double ambilNominalTagihanTerkunci(DetailBiaya detailBiaya,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, DetailKegiatan detailKegiatan) {
		String kode = kodeNominalTagihanTerkunci(detailBiaya, pengaturanPembayaranBulanan, detailKegiatan);
		if (kode == null || nominalTagihanKunciJson == null || nominalTagihanKunciJson.trim().isEmpty()) {
			return null;
		}
		try {
			JSONObject root = new JSONObject(nominalTagihanKunciJson);
			JSONObject nilai = root.optJSONObject("nilai");
			JSONObject snapshot = nilai == null ? null : nilai.optJSONObject(kode);
			return snapshot == null || !snapshot.has("nominal") ? null
					: Double.valueOf(snapshot.getDouble("nominal"));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"Kegiatan.ambilNominalTagihanTerkunci: JSON snapshot nominal tidak valid");
			return null;
		}
	}

	/**
	 * Menyimpan snapshot nominal final beserta audit perubahan. Alasan wajib divalidasi lagi di model
	 * agar jalur non-UI tidak dapat menyimpan koreksi tanpa pertanggungjawaban.
	 */
	public void simpanNominalTagihanTerkunci(DetailBiaya detailBiaya,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, DetailKegiatan detailKegiatan,
			Double nominalSebelum, Double nominalBaru, String alasan, String userId, String userNama) throws Exception {
		String kode = kodeNominalTagihanTerkunci(detailBiaya, pengaturanPembayaranBulanan, detailKegiatan);
		if (kode == null) {
			throw new IllegalArgumentException("Sumber baris tagihan belum memiliki identitas yang dapat dikunci.");
		}
		if (nominalBaru == null || nominalBaru.isNaN() || nominalBaru.isInfinite()) {
			throw new IllegalArgumentException("Nominal baru tidak valid.");
		}
		if (alasan == null || alasan.trim().isEmpty()) {
			throw new IllegalArgumentException("Alasan perubahan nominal wajib diisi.");
		}

		JSONObject root = nominalTagihanKunciJson == null || nominalTagihanKunciJson.trim().isEmpty()
				? new JSONObject() : new JSONObject(nominalTagihanKunciJson);
		JSONObject nilai = root.optJSONObject("nilai");
		if (nilai == null) {
			nilai = new JSONObject();
			root.put("nilai", nilai);
		}
		JSONArray riwayat = root.optJSONArray("riwayat");
		if (riwayat == null) {
			riwayat = new JSONArray();
			root.put("riwayat", riwayat);
		}

		long waktu = System.currentTimeMillis();
		ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();
		JSONObject snapshot = new JSONObject();
		snapshot.put("nominal", nominalBaru.doubleValue());
		snapshot.put("alasan", alasan.trim());
		snapshot.put("waktuEpoch", waktu);
		if (userId != null) snapshot.put("userId", userId);
		if (userNama != null) snapshot.put("userNama", userNama);
		if (itemBiaya != null && itemBiaya.getId() != null) snapshot.put("itemBiayaId", itemBiaya.getId());
		if (itemBiaya != null && itemBiaya.getNama() != null) snapshot.put("itemBiaya", itemBiaya.getNama());
		if (detailBiaya != null && detailBiaya.getId() != null) snapshot.put("detailBiayaId", detailBiaya.getId());
		if (pengaturanPembayaranBulanan != null && pengaturanPembayaranBulanan.getId() != null) {
			snapshot.put("pengaturanPembayaranBulananId", pengaturanPembayaranBulanan.getId());
		}
		if (detailKegiatan != null && detailKegiatan.getId() != null) {
			snapshot.put("detailKegiatanId", detailKegiatan.getId());
		}
		nilai.put(kode, snapshot);

		JSONObject audit = new JSONObject(snapshot.toString());
		audit.put("kode", kode);
		if (nominalSebelum != null) audit.put("nominalSebelum", nominalSebelum.doubleValue());
		riwayat.put(audit);
		root.put("versi", 1);
		nominalTagihanKunciJson = root.toString();
	}

	@Column(columnDefinition = "text")
	public String getBulans() {
		return bulans == null || bulans.isEmpty() ? jsonObject : bulans;
	}

	public void setBulans(String bulans) {
		this.bulans = bulans;
	}

	public void resetBulans() {
		this.bulans = "";
	}

	@Column(columnDefinition = "text")
	public String getTagihans() {
		return tagihans == null || tagihans.isEmpty() ? jsonObject : tagihans;
	}

	public void setTagihans(String tagihans) {
		this.tagihans = tagihans;
	}

	public void resetTagihans() {
		tagihans = "";
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		if (getMahasiswa() != null) {
			jurusan = getMahasiswa().getJurusan();
		} else if (getCalonMahasiswa() != null) {
			jurusan = getCalonMahasiswa().getProdiLulus();
			if (jurusan == null) {
				jurusan = getCalonMahasiswa().getProdi1();
			}
		} else {
			jurusan = check(jurusan);
		}
		return jurusan;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	public Double getTagihan() {
		if (tagihan == null) {
			return 0.0;
		}
		// Koreksi pengamanan jika ternyata ada yang sudah dibayar tapi tagihan tercatat
		// 0
		if (tagihan.intValue() == 0 && dibayar != null && dibayar.intValue() > 0) {
			return dibayar;
		}
		return tagihan;
	}

	public Double getDibayar() {
		if (dibayar == null) {
			return 0.0;
		}
		// Mencegah nilai dibayar melebihi tagihan secara visual
		if (tagihan != null && dibayar > tagihan) {
			return tagihan;
		}
		return dibayar;
	}

	public void setDibayar(Double dibayar) {
		this.dibayar = dibayar;
	}

	public void setTagihan(Double tagihan) {
		this.tagihan = tagihan;
	}

	@SuppressWarnings("unchecked")
	public Double hitungDibayar() {
		try {
			dibayar = 0.0;
			String blnStr = getBulans();

			if (blnStr != null && !blnStr.trim().isEmpty() && !blnStr.equals("{}")) {
				JSONObject jsonObject = new JSONObject(blnStr);
				Iterator<String> iterator = jsonObject.keys();

				while (iterator.hasNext()) {
					String key = iterator.next();
					// Optimasi: Gunakan optString daripada get() + "" untuk mencegah null string
					String val = jsonObject.optString(key, "").trim();

					// FIX: Menggunakan >= 3 agar kebal terhadap penambahan ID dinamis di masa depan
					if (!val.isEmpty() && !"null".equalsIgnoreCase(val) && key.split("_").length >= 3) {
						try {
							Double v = Double.parseDouble(val);
							if (v > 0.0) {
								dibayar += v;
							}
						} catch (NumberFormatException nfe) { ais.common.ErrorAuditUtil.record(nfe, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1454");
							// Abaikan data corrupt
						}
					}
				}
				jsonObject = null; // Bantu Garbage Collector
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1461");
			// Fail-safe
		}

		if (tagihan != null && dibayar != null && dibayar > tagihan) {
			dibayar = tagihan;
		}

		return dibayar;
	}

	@SuppressWarnings("unchecked")
	public Double hitungDibayarAktualTanpaBatas() {
		Double hasil = null;
		try {
			String blnStr = getBulans();

			if (blnStr != null && !blnStr.trim().isEmpty() && !blnStr.equals("{}")) {
				hasil = 0.0;
				JSONObject jsonObject = new JSONObject(blnStr);
				Iterator<String> iterator = jsonObject.keys();

				while (iterator.hasNext()) {
					String key = iterator.next();
					String val = jsonObject.optString(key, "").trim();

					if (!val.isEmpty() && !"null".equalsIgnoreCase(val) && key.split("_").length >= 3) {
						try {
							Double v = Double.parseDouble(val);
							if (v > 0.0) {
								hasil += v;
							}
						} catch (NumberFormatException nfe) {
							ais.common.ErrorAuditUtil.record(nfe,
									"auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:hitungDibayarAktualTanpaBatas");
						}
					}
				}
				jsonObject = null;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:hitungDibayarAktualTanpaBatas");
		}
		if (hasil == null) {
			hasil = dibayar == null ? 0.0 : dibayar;
		}
		return hasil;
	}

	@SuppressWarnings("unchecked")
	public Double hitungTagihan() {
		try {
			jenisKegiatan = getJenisKegiatan();
			if (jenisKegiatan != null) {
				tagihan = 0.0;
				String tgStr = getTagihans();

				if (tgStr != null && !tgStr.trim().isEmpty() && !tgStr.equals("{}")) {
					JSONObject jsonObject = new JSONObject(tgStr);

					int b = 0;
					int a = 0;
					Iterator<String> iterator = jsonObject.keys();

					// Looping Pertama: Hitung a dan b
					while (iterator.hasNext()) {
						String key = iterator.next();
						String val = jsonObject.optString(key, "").trim();

						if (!val.isEmpty() && !"null".equalsIgnoreCase(val)) {
							if (key.contains("_")) {
								b++;
							} else {
								a++;
							}
						}
					}

					boolean isAngsuran = Boolean.TRUE.equals(jenisKegiatan.getHanyaBerupaAngsuran());
					iterator = jsonObject.keys(); // Reset iterator

					// Looping Kedua: Eksekusi Penjumlahan
					while (iterator.hasNext()) {
						String key = iterator.next();
						String val = jsonObject.optString(key, "").trim();

						if (!val.isEmpty() && !"null".equalsIgnoreCase(val)) {
							boolean validKey = false;

							if (isAngsuran) {
								if (key.contains("_"))
									validKey = true;
							} else {
								if (b > a) {
									if (key.contains("_"))
										validKey = true;
								} else {
									validKey = true;
								}
							}

							if (validKey) {
								try {
									tagihan += Double.parseDouble(val);
								} catch (NumberFormatException nfe) { ais.common.ErrorAuditUtil.record(nfe, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1527");
									// Abaikan string non-angka
								}
							}
						}
					}
					jsonObject = null; // Bantu Garbage Collector
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1536");
			// Fail-safe
		}

		if (tagihan != null && tagihan.intValue() == 0 && dibayar != null && dibayar.intValue() > 0) {
			tagihan = dibayar;
		}

		return tagihan;
	}

	public Boolean getApakahLunas() {

		if (persentase == null) {
			persentase = getPersentase();
		}

		apakahLunas = persentase != null && persentase.intValue() >= 100;

		return apakahLunas;
	}

	public void setApakahLunas(Boolean apakahLunas) {
		this.apakahLunas = apakahLunas;
	}

	public Double getPersentase() {

		if (tagihan == null) {
			tagihan = getTagihan();
		}
		if (dibayar == null) {
			dibayar = getDibayar();
		}

		if (tagihan != null) {

			if (tagihan < 0.01) {
				// 0 tagihan dan 0 pembayaran bukan transaksi lunas. Nilai 100% hanya
				// masuk akal bila memang pernah ada pembayaran positif.
				persentase = dibayar != null && dibayar.doubleValue() > 0.01 ? 100.0 : 0.0;
			} else {
				persentase = (dibayar * 100.0) / tagihan;
			}
		}
		return persentase;
	}

	public void setPersentase(Double persentase) {
		this.persentase = persentase;
	}

	public Integer getTahunAngkatan() {
		if (getMahasiswa() != null) {
			tahunAngkatan = getMahasiswa().getTahunangkatan();
		} else if (getCalonMahasiswa() != null) {
			tahunAngkatan = getCalonMahasiswa().getTahun();
		}
		return tahunAngkatan;
	}

	public void setTahunAngkatan(Integer tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {

		if (getMahasiswa() != null) {
			statusAwalMahasiswa = HistoryStatusMahasiswa.ambilStatusAwal(mahasiswa, getSemster(),
					getMahasiswa().getStatusAwalMahasiswa());
		} else if (getCalonMahasiswa() != null) {
			statusAwalMahasiswa = getCalonMahasiswa().getStatusAwalMahasiswa();
		} else {
			statusAwalMahasiswa = check(statusAwalMahasiswa);
		}

		return statusAwalMahasiswa;
	}

	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	public String getKode() {
		if (getMahasiswa() != null) {
			kode = getMahasiswa().getNim();
		} else if (getCalonMahasiswa() != null) {
			kode = getCalonMahasiswa().getNoRegistrasi();
		}
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "pembatalan_denda", nullable = true, columnDefinition = "text")
	public String getPembatalanDenda() {
		pembatalanDenda = (pembatalanDenda == null || pembatalanDenda.trim().equalsIgnoreCase(",") ? ""
				: "," + pembatalanDenda.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (pembatalanDenda.equals(",")) {
			pembatalanDenda = "";
		} else if (pembatalanDenda.equals(",,")) {
			pembatalanDenda = "";
		} else if (pembatalanDenda.equals(",,,")) {
			pembatalanDenda = "";
		} else if (pembatalanDenda.equals(",,,,")) {
			pembatalanDenda = "";
		}

		return pembatalanDenda == null ? "" : pembatalanDenda.trim();
	}

	public void setPembatalanDenda(String pembatalanDenda) {
		this.pembatalanDenda = pembatalanDenda;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalBayarTerakhir() {
		return tanggalBayarTerakhir;
	}

	public void setTanggalBayarTerakhir(Date tanggalBayarTerakhir) {
		this.tanggalBayarTerakhir = tanggalBayarTerakhir;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalBayarAwal() {
		return tanggalBayarAwal;
	}

	public void setTanggalBayarAwal(Date tanggalBayarAwal) {
		this.tanggalBayarAwal = tanggalBayarAwal;
	}

	// ========================================================================
	// GETTER & SETTER DETAIL KEGIATANS
	// ========================================================================

	@Column(name = "detail_kegiatans", nullable = true, columnDefinition = "text")
	public String getDetailKegiatans() {
		if (detailKegiatans == null || detailKegiatans.replace(",", "").trim().isEmpty()) {
			return "";
		}
		return ("," + detailKegiatans.trim() + ",").replaceAll(",+", ",");
	}

	public void setDetailKegiatans(String detailKegiatans) {
		// Panggil Helper agar data lama tidak hilang, melainkan digabung
		this.detailKegiatans = gabungkanDanPertahankanID(this.detailKegiatans, detailKegiatans, true);
	}

	// ========================================================================
	// TAMBAHAN METHOD (SANGAT PENTING!)
	// ========================================================================

	/**
	 * Karena format string berubah menjadi ID:true/false, method lain di sistem
	 * Anda mungkin error jika mereka mem-parsing dengan Long.parseLong() secara
	 * langsung. Gunakan method ini untuk mengambil list ID yang HANYA AKTIF (true).
	 */
	public java.util.List<Long> ambilCicilansAktifIds() {
		java.util.List<Long> list = new java.util.ArrayList<Long>();
		String data = getCicilans();
		if (data != null && !data.isEmpty()) {
			for (String s : data.split(",")) {
				if (!s.trim().isEmpty()) {
					String[] parts = s.split(":");
					try {
						Long id = Long.parseLong(parts[0].trim());
						boolean aktif = parts.length > 1 ? Boolean.parseBoolean(parts[1].trim()) : true;
						if (aktif)
							list.add(id); // Hanya ambil yang masih true
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1710");
					}
				}
			}
		}
		return list;
	}

	public java.util.List<Long> ambilDetailKegiatansAktifIds() {
		java.util.List<Long> list = new java.util.ArrayList<Long>();
		String data = getDetailKegiatans();
		if (data != null && !data.isEmpty()) {
			for (String s : data.split(",")) {
				if (!s.trim().isEmpty()) {
					String[] parts = s.split(":");
					try {
						Long id = Long.parseLong(parts[0].trim());
						boolean aktif = parts.length > 1 ? Boolean.parseBoolean(parts[1].trim()) : true;
						if (aktif)
							list.add(id); // Hanya ambil yang masih true
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1730");
					}
				}
			}
		}
		return list;
	}

	// ========================================================================
	// HELPER PENGGABUNGAN DATA (SOFT DELETE STRING)
	// ========================================================================
	/**
	 * Helper untuk menggabungkan string ID, mengamankan ID lama agar tidak hilang.
	 * Jika ID dihapus, ia hanya akan diset menjadi false (,ID:false,) apabila
	 * parameter hapus == true.
	 */
	private String gabungkanDanPertahankanID(String dataLama, String dataBaru, boolean hapus) {
		java.util.Map<Long, Boolean> map = new java.util.LinkedHashMap<Long, Boolean>();

		// 1. Ekstrak data lama
		if (dataLama != null && !dataLama.trim().isEmpty()) {
			for (String s : dataLama.split(",")) {
				if (!s.trim().isEmpty()) {
					String[] parts = s.split(":");
					if (parts.length == 0 || !isTokenIdKegiatanValid(parts[0])) {
						continue;
					}
					try {
						Long id = Long.parseLong(parts[0].trim());
						boolean aktif = parts.length > 1 ? Boolean.parseBoolean(parts[1].trim()) : true;
						map.put(id, aktif);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1758");
					}
				}
			}
		}

		// 2. Ekstrak data baru
		java.util.Map<Long, Boolean> newMap = new java.util.LinkedHashMap<Long, Boolean>();
		if (dataBaru != null && !dataBaru.trim().isEmpty()) {
			for (String s : dataBaru.split(",")) {
				if (!s.trim().isEmpty()) {
					String[] parts = s.split(":");
					if (parts.length == 0 || !isTokenIdKegiatanValid(parts[0])) {
						continue;
					}
					try {
						Long id = Long.parseLong(parts[0].trim());
						boolean aktif = parts.length > 1 ? Boolean.parseBoolean(parts[1].trim()) : true;
						newMap.put(id, aktif);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kegiatan.java:1774");
					}
				}
			}
		}

		// 3. Gabungkan Logika (The Core Logic):
		// - Jika ID lama tidak ada di list baru, jadikan FALSE (Soft Delete) HANYA JIKA
		// hapus == true
		// - Jika ID lama ada di list baru, gunakan status dari list baru
		for (java.util.Map.Entry<Long, Boolean> entry : map.entrySet()) {
			Long id = entry.getKey();
			if (!newMap.containsKey(id)) {
				if (hapus) {
					map.put(id, false); // Tandai sebagai terhapus (Soft delete) karena hapus == true
				}
				// Jika hapus == false, tidak melakukan apa-apa. Status asli di dalam 'map' akan
				// tetap dipertahankan.
			} else {
				map.put(id, newMap.get(id)); // Pertahankan atau perbarui status
			}
		}

		// 4. Masukkan ID baru yang belum ada di data lama
		for (java.util.Map.Entry<Long, Boolean> entry : newMap.entrySet()) {
			if (!map.containsKey(entry.getKey())) {
				map.put(entry.getKey(), entry.getValue());
			}
		}

		// 5. Rekonstruksi String hasil gabungan
		StringBuilder sb = new StringBuilder(",");
		for (java.util.Map.Entry<Long, Boolean> entry : map.entrySet()) {
			sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
		}

		String hasil = sb.toString();
		if (hasil.replace(",", "").trim().isEmpty()) {
			return "";
		}
		return hasil;
	}

	private boolean isTokenIdKegiatanValid(String token) {
		return token != null && token.trim().matches("[0-9]+");
	}

	// ========================================================================
	// GETTER & SETTER CICILANS
	// ========================================================================

	@Column(name = "cicilans", nullable = true, columnDefinition = "text")
	public String getCicilans() {
		if (cicilans == null || cicilans.replace(",", "").trim().isEmpty()) {
			return "";
		}
		return ("," + cicilans.trim() + ",").replaceAll(",+", ",");
	}

	public void setCicilans(String cicilans) {
		// OPTIMASI PENTING: Cegah looping komputasi saat Hibernate load dari DB.
		// Jika nilai yang masuk sama persis dengan yang ada, abaikan.
		if (this.cicilans != null && this.cicilans.equals(cicilans)) {
			return;
		}
		this.cicilans = gabungkanDanPertahankanID(this.cicilans, cicilans,
				cicilans != null && cicilans.toLowerCase().contains("false"));
	}

	public void appendCicilan(ais.database.model.CicilanPembayaran cicilanPembayaran) {
		if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
			return;
		}

		String idBaru = cicilanPembayaran.getId() + ":true";

		if (this.cicilans == null || this.cicilans.replace(",", "").trim().isEmpty()) {
			this.cicilans = "," + idBaru + ",";
		} else {
			// Bypass setter untuk menghindari double processing, panggil method helper
			// langsung
			String dataSekarang = this.cicilans;
			String dataGabungan = dataSekarang + "," + idBaru;
			this.cicilans = gabungkanDanPertahankanID(dataSekarang, dataGabungan, false);
		}
	}

	public void appendDetailKegiatan(ais.database.model.DetailKegiatan detailKegiatan) {
		if (detailKegiatan == null || detailKegiatan.getId() == null) {
			return;
		}

		String idBaru = detailKegiatan.getId() + ":true";

		if (this.detailKegiatans == null || this.detailKegiatans.replace(",", "").trim().isEmpty()) {
			this.detailKegiatans = "," + idBaru + ",";
		} else {
			// Mengikuti template: ambil data sekarang, gabungkan dengan ID baru,
			// lalu proses melalui method helper untuk menjaga integritas data.
			String dataSekarang = this.detailKegiatans;
			String dataGabungan = dataSekarang + "," + idBaru;
			this.detailKegiatans = gabungkanDanPertahankanID(dataSekarang, dataGabungan, false);
		}
	}
}
