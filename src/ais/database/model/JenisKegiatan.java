package ais.database.model;

// Generated Apr 12, 2010 1:48:52 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.List;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.sekolah.KanalPembayaran;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Master <b>jenis kegiatan</b> &mdash; katalog resmi jenis tagihan/kegiatan yang dapat
 * ditagihkan kepada mahasiswa maupun calon mahasiswa (SPP/registrasi ulang, pendaftaran
 * calon mahasiswa, daftar ulang mahasiswa baru, wisuda, semester pendek, dan seterusnya).
 * Entity ini adalah <i>puncak</i> hierarki penagihan pada AIS; satu barisnya menentukan
 * perilaku seluruh tagihan yang bernaung di bawahnya.
 *
 * <h3>Posisi dalam mesin penagihan</h3>
 * Tiga entity bekerja bersama membentuk satu tagihan:
 * <ol>
 *   <li><b>{@code JenisKegiatan}</b> (kelas ini) &mdash; <i>katalog/aturan</i>. Menjawab
 *       &quot;jenis tagihan apa ini, semester berapa saja ia berlaku, apakah boleh diangsur,
 *       apakah ada denda keterlambatan, dan apakah ia menjadi syarat login/KRS/ujian&quot;.
 *       Tidak menyimpan nominal sama sekali.</li>
 *   <li><b>{@link DetailBiaya}</b> &mdash; <i>master nominal</i>. Satu baris per kombinasi
 *       (prodi, item biaya, program, semester, tahun akademik, angkatan, paket, gelombang,
 *       jenis seleksi, kelas, &hellip;) &mdash; lihat {@link DetailBiaya#key()}. Menyimpan
 *       {@code nilai_biaya}. Satu baris <b>dipakai bersama</b> oleh banyak mahasiswa.</li>
 *   <li><b>{@link Kegiatan}</b> &mdash; <i>header tagihan per orang per semester</i>, dan
 *       {@link DetailKegiatan} sebagai baris-baris rinciannya.</li>
 * </ol>
 * Jadi arah alirannya: {@code JenisKegiatan} menyaring <i>kegiatan mana yang berlaku</i>,
 * {@link DetailBiaya} memasok <i>berapa nominalnya</i>, dan {@link Kegiatan} menampung
 * <i>siapa yang ditagih dan sudah bayar berapa</i>.
 *
 * <h3>Relasi masuk</h3>
 * <ul>
 *   <li>{@link Kegiatan#getJenisKegiatan()} &mdash; setiap header tagihan menunjuk ke sini;
 *       {@link Kegiatan#generateKodeUnik} memakai {@code id}-nya sebagai bagian kunci unik
 *       alami tagihan, sehingga satu mahasiswa hanya boleh punya satu {@link Kegiatan}
 *       per (jenis kegiatan, semester).</li>
 *   <li>{@link DetailBiaya#getJenisKegiatan()} &mdash; menyaring master nominal mana yang
 *       ikut terpanggil untuk jenis kegiatan tertentu.</li>
 *   <li>{@link JadwalPembayaran} &mdash; rentang tanggal berlaku &amp; deadline denda.</li>
 *   <li>{@link KegiatanTemporary} &mdash; staging tagihan sebelum menjadi {@link Kegiatan}.</li>
 * </ul>
 *
 * <h3>Tiga sentinel yang diperlakukan khusus</h3>
 * Sejumlah method di kelas ini mengenali tiga jenis kegiatan bawaan lewat
 * {@link ais.common.ConstantValues} / {@link ais.action.ws.util.ConstantUtil}:
 * {@code PENDAFTARAN_CALON_MAHASISWA}, {@code PENDAFTARAN_MAHASISWA_LAMA} (registrasi ulang),
 * dan {@code PENDAFTARAN_ULANG_MAHASISWA_BARU}. Untuk ketiganya sejumlah flag dipaksa
 * bernilai tertentu tanpa bisa dimatikan operator &mdash; lihat {@link #getAktif()},
 * {@link #getKode()}, {@link #getDigunakanUntukPengecekanKrs()},
 * {@link #getDigunakanSyaratKeaktifan()}, dan {@link #getMaxSmt()}.
 *
 * <h3>PERINGATAN: banyak getter di kelas ini bersifat DESTRUKTIF</h3>
 * Kelas ini memakai <i>property access</i> Hibernate (anotasi ada di getter), sehingga
 * Hibernate memanggil getter-nya pada setiap {@code flush}/{@code dirty check}. Sejumlah
 * getter di bawah ini <b>menulis balik ke field</b>, bukan sekadar membaca:
 * {@link #getKode()}, {@link #getAktif()}, {@link #getDigunakanUntukPengecekanKrs()},
 * {@link #getDigunakanUntukPengecekanUjian()}, {@link #getDigunakanSyaratKeaktifan()},
 * {@link #getMaxSmt()}, {@link #getTagihanJugaUntukAlumni()},
 * {@link #getDendaDibuatPerProdi()}, dan {@link #getNamaBankPembayaran()}. Akibatnya
 * <b>sekadar membaca</b> sebuah {@code JenisKegiatan} di dalam session yang terbuka dapat
 * menandai entity sebagai <i>dirty</i>, memicu {@code UPDATE jenis_kegiatan}, dan
 * menghasilkan revisi Envers ({@code @Audited}) palsu &mdash; revisi yang seolah-olah
 * &quot;diubah orang&quot; padahal tidak ada yang mengubahnya. Ini instance dari pola
 * sistemik <i>getter-mutasi-field</i> yang tersebar di paket {@code ais.database.model}.
 *
 * <p>JenisKegiatan generated by hbm2java</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jenis_kegiatan")
public class JenisKegiatan extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3088613612931036389L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Id pengguna (bukan nama) yang terakhir menyentuh baris ini &mdash; field audit bayangan
	 * yang diisi {@link ais.database.hibernate.AuditTimestampInterceptor}.
	 *
	 * @return id pelaku perubahan terakhir; {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter audit <b>satu arah</b>: masukan {@code null} atau string kosong/spasi diabaikan
	 * diam-diam sehingga nilai lama tidak pernah bisa dikosongkan lagi. Pola yang sama dipakai
	 * pada {@link #setOleh(String)} dan pada hampir seluruh entity {@code ais.database.model}.
	 *
	 * @param olehId id pelaku; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Setter audit <b>satu arah</b> untuk nama pelaku perubahan; masukan {@code null} atau
	 * kosong diabaikan (nilai lama dipertahankan). Lihat {@link #setOlehId(String)}.
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
	 * Urutkan katalog berdasarkan {@link #getNama()} secara alfabetis menaik. Dipakai saat
	 * daftar jenis kegiatan ditampilkan pada combobox/listbox ZK dan pada laporan.
	 *
	 * <p>Perhatikan bahwa pembandingnya dibungkus {@code try/catch} yang mengembalikan
	 * {@code 0} bila terjadi apa pun (mis. {@code arg0} bukan {@code JenisKegiatan} sehingga
	 * {@code arg0.getNama()} melempar, atau relasi lazy sudah terputus). Nilai {@code 0}
	 * berarti &quot;dianggap sama&quot;, sehingga urutan elemen bermasalah menjadi tidak
	 * deterministik &mdash; bukan melempar error yang terlihat.</p>
	 *
	 * @param arg0 objek pembanding
	 * @return hasil {@link String#compareTo(String)} atas nama; {@code 0} bila salah satu nama
	 *         {@code null} atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/JenisKegiatan.java:80");

		}

		return 0;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat stempel waktu/pelaku perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum
	 * {@code UPDATE} dikirim ke database.
	 *
	 * <p>Karena banyak getter kelas ini menulis balik ke field (lihat javadoc kelas), callback
	 * ini juga ikut berjalan pada &quot;update palsu&quot; yang dipicu sekadar oleh pembacaan
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
	 * Stempel waktu perubahan terakhir. Nilai awalnya diisi saat objek dibuat dengan
	 * {@link ais.ui.util.WaktuUtil#getDate()} (waktu server yang dapat digeser konfigurasi),
	 * bukan {@code new Date()} langsung.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama kegiatan/tagihan sebagaimana tampil di layar mahasiswa dan di kuitansi; kolom {@code nama_kegiatan}. Dipetakan DUA KALI &mdash; lihat {@link #getNama()} dan {@link #getNamaKegiatan()}. */
	private String namaKegiatan;
	private String prefixKodePembayaran;
	private String namaBankPembayaran;
	private String keterangan;
	private String penjelasanPembayaran;

	/**
	 * Keterangan bebas jenis kegiatan, dinormalkan menjadi string kosong bila {@code null}
	 * agar aman dipakai langsung pada komponen ZK dan template laporan.
	 *
	 * <p>Getter murni: hanya membaca, tidak menulis balik ke field (bandingkan dengan
	 * {@link #getKode()} atau {@link #getNamaBankPembayaran()} yang destruktif). Karena itu
	 * baris dengan {@code keterangan} {@code NULL} di database tetap {@code NULL} setelah
	 * dibaca &mdash; string kosong hanya bentuk tampilannya.</p>
	 *
	 * @return keterangan; string kosong bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/**
	 * Setter keterangan (tanpa validasi, {@code null} diterima apa adanya).
	 *
	 * @param keterangan keterangan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode pendek jenis kegiatan yang dipakai gateway pembayaran/bank dan laporan.
	 * Dapat diturunkan otomatis dari {@code namaKegiatan} &mdash; lihat {@link #getKode()}.
	 */
	private String kode;
	private Boolean defaultKegiatan = false;
	private Boolean aktif;
	private Boolean defaultPembayaran = false;
	private Boolean digunakanUntukPengecekanKrs;
	private Boolean digunakanUntukPengecekanNilai;
	private Boolean digunakanUntukPengecekanUjian;
	private Boolean digunakanSyaratKeaktifan;
	private Boolean digunakanSyaratLogin;
	private Boolean digunakanSyaratCetakSuratBebasAktif;

	/**
	 * Batas semester berlakunya jenis kegiatan ini.
	 *
	 * <p>Pasangan {@code minSmt}/{@code maxSmt} adalah <b>gerbang keberlakuan</b> yang dibaca
	 * kembali oleh {@link Kegiatan#getAktif()}: sebuah {@link Kegiatan} otomatis dianggap
	 * TIDAK aktif bila semesternya di luar rentang ini. Jadi mengubah nilai di sini
	 * berpengaruh surut terhadap tagihan yang sudah terbentuk, bukan hanya terhadap tagihan
	 * baru. Nilai default 0..30 berarti &quot;berlaku di semua semester&quot;.</p>
	 */
	private Integer minSmt = 0;
	private Integer maxSmt = 30;

	/**
	 * Ambang persentase pelunasan sebagai syarat login, beserta 14 varian per jumlah semester
	 * mundur ({@code persenSyaratLogin1} &hellip; {@code persenSyaratLogin14}).
	 *
	 * <p>Varian bernomor menjawab kebutuhan &quot;semester berjalan boleh menunggak, tapi
	 * semester lalu harus lunas 100%&quot;. Setiap varian yang {@code null} jatuh kembali ke
	 * {@link #getPersenSyaratLogin()} &mdash; lihat {@link #bolehMasuk} yang memilih varian
	 * lewat {@code switch} atas jumlah semester mundur.</p>
	 */
	private Double persenSyaratLogin;
	private KanalPembayaran kanalPembayaran;
	private Boolean bayarHanyaSmtSaatIni;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnya;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi3;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi4;
	private Boolean hanyaBerupaAngsuran;
	private Boolean hanyaBerupaBukanAngsuran;
	private String jenjangAngsuranJson;
	private Boolean tagihanJugaUntukAlumni;

	private Boolean tidakBolehMengangsur;
	private Double persenSyaratLogin1;
	private Double persenSyaratLogin2;
	private Double persenSyaratLogin3;
	private Double persenSyaratLogin4;
	private Double persenSyaratLogin5;
	private Double persenSyaratLogin6;
	private Double persenSyaratLogin7;
	private Double persenSyaratLogin8;
	private Double persenSyaratLogin9;
	private Double persenSyaratLogin10;
	private Double persenSyaratLogin11;
	private Double persenSyaratLogin12;
	private Double persenSyaratLogin13;
	private Double persenSyaratLogin14;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi5;
	// private Set<DetailBiaya> detailBiayas = new HashSet<DetailBiaya>();
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi6;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi7;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi8;

	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi9;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi10;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi11;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi12;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi13;
	private Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi14;

	/**
	 * Konfigurasi <b>denda keterlambatan</b> pada tingkat jenis kegiatan.
	 *
	 * <p>Enam field ini dibaca oleh {@link DetailBiaya#checkDenda} dan
	 * {@link DetailBiaya#checkDendaCicilan}:</p>
	 * <ul>
	 *   <li>{@code dendaJikaTerlambat} &mdash; saklar UTAMA: apakah jenis kegiatan ini
	 *       mengenakan denda sama sekali. Default {@code false}.</li>
	 *   <li>{@code nilaiDendaDalamPersen} &mdash; FORMAT denda: persen dari nominal, atau
	 *       nominal tetap. Default {@code true} (perhatikan: default-nya berlawanan dengan
	 *       saklar utama).</li>
	 *   <li>{@code defaultProsentaseDenda} &mdash; besaran dendanya.</li>
	 *   <li>{@code dendaDibuatPerProdi} + {@code dendaPerProdi} (JSON {@code prodiId:nilai})
	 *       &mdash; besaran denda berbeda per program studi.</li>
	 *   <li>{@code dendaAkanBerlipatTerlambaHari} / {@code maksimalBerlipatTerlambaHari}
	 *       &mdash; denda berlipat setiap N hari, dengan batas kelipatan.</li>
	 * </ul>
	 * <p><b>Catatan penting:</b> {@link DetailBiaya#checkDendaCicilan} hanya sanggup membaca
	 * konfigurasi ini melalui objek {@link JadwalPembayaran} yang dioper pemanggilnya,
	 * sedangkan satu-satunya pemanggil produksi
	 * ({@link ais.database.hibernate.AuditListener} pada {@code onPostInsert}) mengoper
	 * {@code null}. Akibatnya konfigurasi denda di tingkat jenis kegiatan efektif TIDAK
	 * berlaku untuk pembayaran angsuran/cicilan &mdash; hanya denda tingkat
	 * {@link ItemBiaya} yang terpakai di sana.</p>
	 */
	private Boolean dendaJikaTerlambat;
	private Boolean dendaDibuatPerProdi;
	private Boolean nilaiDendaDalamPersen;
	private Integer dendaAkanBerlipatTerlambaHari;
	private Integer maksimalBerlipatTerlambaHari;
	private Double defaultProsentaseDenda;
	private String dendaPerProdi;
	private Boolean untukBayarSP;

	/** Bila {@code true}, baris tagihan bernilai negatif pada jenis kegiatan ini diabaikan saat penjumlahan (bukan dikurangkan). */
	private Boolean abaikanNilaiMinus;

	/**
	 * Representasi ringkas {@code id-kode-namaKegiatan} untuk log dan komponen ZK.
	 *
	 * <p>Sengaja membaca <b>field</b> {@code kode} dan {@code namaKegiatan} secara langsung,
	 * bukan lewat {@link #getKode()}. Ini penting: {@link #getKode()} bersifat destruktif
	 * (menulis balik ke field). Bila {@code toString()} memakai getter itu, sekadar mencetak
	 * objek ke log dapat mengotori entity dan memicu {@code UPDATE}. Konsekuensinya, untuk
	 * baris yang {@code kode}-nya masih {@code null} di database, {@code toString()} mencetak
	 * {@code null} walaupun {@link #getKode()} akan menurunkan kode dari nama kegiatan.</p>
	 *
	 * @return string {@code id-kode-namaKegiatan}
	 */
	public String toString() {
		return id + "-" + kode + "-" + namaKegiatan;
	}

	/** Konstruktor kosong wajib bagi Hibernate/JPA dan bagi form CRUD generik. */
	public JenisKegiatan() {
	}

	/**
	 * Konstruktor pintasan berisi <b>hanya</b> primary key &mdash; menghasilkan instance
	 * {@code TRANSIENT} yang seluruh field lainnya {@code null}.
	 *
	 * <p>Berhati-hatilah memakainya sebagai nilai relasi yang akan disimpan: bila {@code id}
	 * yang dioper ternyata tidak ada di tabel {@code jenis_kegiatan}, {@code INSERT} pemilik
	 * relasi akan melanggar foreign key. Untuk keperluan itu pakai pola muat-aman seperti
	 * {@link DetailBiaya#muatRefAman(org.hibernate.Session, Long)}.</p>
	 *
	 * @param id primary key jenis kegiatan
	 */
	public JenisKegiatan(Long id) {
		this.id = id;
	}

	/**
	 * Konstruktor pintasan berisi hanya nama kegiatan; dipakai saat mencari/menyiapkan
	 * kandidat katalog berdasarkan nama.
	 *
	 * @param namaKegiatan nama kegiatan
	 */
	public JenisKegiatan(String namaKegiatan) {
		this.namaKegiatan = namaKegiatan;
	}

	/**
	 * Primary key {@code jenis_kegiatan.id}, di-{@code generate} oleh database
	 * ({@code IDENTITY}) sehingga {@code insertable = false}.
	 *
	 * <p>Nilai id inilah yang dibandingkan dengan tiga sentinel di
	 * {@link ais.common.ConstantValues} pada {@link #getAktif()} dan {@link #bolehMasuk},
	 * dan yang ikut membentuk kunci unik alami tagihan di
	 * {@link Kegiatan#generateKodeUnik}.</p>
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
	 * @param id primary key jenis kegiatan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama kegiatan versi <b>hanya-baca</b>.
	 *
	 * <p>Perhatikan bahwa kolom {@code nama_kegiatan} dipetakan DUA KALI pada entity ini:
	 * lewat property {@code nama} (method ini, {@code insertable = false, updatable = false})
	 * dan lewat property {@code namaKegiatan} ({@link #getNamaKegiatan()}, yang dapat ditulis).
	 * Keduanya menunjuk field Java yang sama, {@code namaKegiatan}. Pemetaan ganda ini ada
	 * agar kelas ini memenuhi kontrak {@code getNama()} milik {@link GeneralValueObject}
	 * &mdash; yang dipakai renderer/combobox ZK dan {@link #compareTo} &mdash; tanpa membuat
	 * Hibernate menuliskan kolom yang sama dua kali dalam satu {@code INSERT}/{@code UPDATE}
	 * (yang akan ditolak database). Konsekuensinya {@link #setNama(String)} tetap mengubah
	 * nilai in-memory, tetapi perubahan itu hanya tersimpan ke database karena property
	 * {@code namaKegiatan} ikut menuliskannya.</p>
	 *
	 * @return nama kegiatan
	 */
	@Column(name = "nama_kegiatan", nullable = false, insertable = false, updatable = false, length = 100)
	public String getNama() {
		return this.namaKegiatan;
	}

	/**
	 * Setter nama kegiatan lewat property hanya-baca {@code nama}; mengubah field yang sama
	 * dengan {@link #setNamaKegiatan(String)}. Lihat {@link #getNama()} soal pemetaan ganda.
	 *
	 * @param namaKegiatan nama kegiatan
	 */
	public void setNama(String namaKegiatan) {
		this.namaKegiatan = namaKegiatan;
	}

	/**
	 * Nama kegiatan versi <b>dapat ditulis</b> &mdash; inilah property yang benar-benar
	 * menuliskan kolom {@code nama_kegiatan} ke database. Lihat {@link #getNama()} mengenai
	 * pemetaan ganda kolom ini.
	 *
	 * <p>Nilainya juga berperan sebagai <i>penanda semantik</i>: {@link #getKode()},
	 * {@link #getMaxSmt()}, {@link #getDigunakanUntukPengecekanKrs()},
	 * {@link #getDigunakanSyaratKeaktifan()}, dan {@link #getTagihanJugaUntukAlumni()}
	 * semuanya mencocokkan nama ini dengan konstanta di
	 * {@link ais.action.ws.util.ConstantUtil} untuk menentukan perilaku bawaan. Mengganti
	 * nama kegiatan karena itu bukan sekadar perubahan label &mdash; ia dapat mengubah
	 * kode pembayaran, batas semester, dan status &quot;syarat keaktifan&quot; baris ini.</p>
	 *
	 * @return nama kegiatan
	 */
	@Column(name = "nama_kegiatan", nullable = false, length = 100)
	public String getNamaKegiatan() {
		return this.namaKegiatan;
	}

	/**
	 * Setter nama kegiatan (property yang dituliskan ke database).
	 *
	 * @param namaKegiatan nama kegiatan; lihat {@link #getNamaKegiatan()} soal efek sampingnya
	 *                     terhadap kode pembayaran dan flag bawaan
	 */
	public void setNamaKegiatan(String namaKegiatan) {
		this.namaKegiatan = namaKegiatan;
	}

	/**
	 * Setter kode pembayaran secara eksplisit. Nilai yang diisi di sini akan dihormati
	 * {@link #getKode()} (yang hanya menurunkan kode saat field masih kosong).
	 *
	 * @param kode kode pembayaran
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Kode pembayaran jenis kegiatan, dengan <b>penurunan otomatis</b> dari nama kegiatan
	 * bila kolom {@code kode} masih kosong.
	 *
	 * <p>Pemetaan otomatisnya mengikuti konstanta {@link ais.action.ws.util.ConstantUtil}:</p>
	 * <ul>
	 *   <li>{@code PENDAFTARAN_MAHASISWA_LAMA} &rarr; {@code PEMBAYARAN_PENDAFTARAN_ULANG}</li>
	 *   <li>{@code PENDAFTARAN_CALON_MAHASISWA} &rarr;
	 *       {@code PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA}</li>
	 *   <li>{@code PENDAFTARAN_ULANG_MAHASISWA_BARU} &rarr;
	 *       {@code PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU}</li>
	 *   <li>{@code PENDAFTARAN_WISUDA} &rarr; {@code PEMBAYARAN_PENDAFTARAN_WISUDA}</li>
	 * </ul>
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Method ini bukan pembaca murni: pada cabang penurunan
	 * otomatis ia melakukan {@code kode = ...}, yaitu <i>menulis balik ke field</i>. Karena
	 * property {@code kode} ikut dipetakan ke kolom database dan Hibernate memanggil getter
	 * pada setiap {@code dirty check}, konsekuensinya berlapis:</p>
	 * <ol>
	 *   <li>Sekadar <b>membaca</b> jenis kegiatan yang {@code kode}-nya masih {@code NULL}
	 *       di dalam session terbuka akan menandai entity sebagai <i>dirty</i> dan memicu
	 *       {@code UPDATE jenis_kegiatan SET kode = ...} &mdash; suatu penulisan yang tidak
	 *       pernah diminta pengguna mana pun.</li>
	 *   <li>Karena kelas ini {@code @Audited}, Envers akan mencatat <b>revisi baru</b> untuk
	 *       perubahan itu, lengkap dengan pelaku dan waktu, sehingga jejak audit menampilkan
	 *       perubahan yang tidak pernah dilakukan siapa pun.</li>
	 *   <li>Penurunan otomatis bersifat <b>sekali seumur hidup dan tidak dapat dibatalkan
	 *       dari sisi baca</b>: begitu kode tertulis, cabang penurunan tidak pernah berjalan
	 *       lagi karena syaratnya field harus kosong. Untuk mengubahnya operator harus
	 *       memanggil {@link #setKode(String)} secara eksplisit.</li>
	 * </ol>
	 *
	 * <p>Perhatikan pula bahwa pencocokan nama memakai {@link String#equals(Object)} yang
	 * <i>case sensitive</i> dan tanpa {@code trim()}. Nama kegiatan yang berbeda kapitalisasi
	 * atau berimbuhan spasi tidak akan cocok, dan {@code kode} tetap {@code null} &mdash;
	 * kondisi yang perlu diantisipasi pemanggil di jalur gateway pembayaran.</p>
	 *
	 * @return kode pembayaran; {@code null} bila field kosong dan nama kegiatan tidak cocok
	 *         dengan satu pun konstanta bawaan
	 */
	public String getKode() {
		if (kode == null || kode.trim().isEmpty()) {
			if (getNamaKegiatan() != null && getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA)) {
				kode = ConstantUtil.PEMBAYARAN_PENDAFTARAN_ULANG;
			} else if (getNamaKegiatan() != null
					&& getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
				kode = ConstantUtil.PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA;
			} else if (getNamaKegiatan() != null
					&& getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
				kode = ConstantUtil.PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU;
			} else if (getNamaKegiatan() != null && getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_WISUDA)) {
				kode = ConstantUtil.PEMBAYARAN_PENDAFTARAN_WISUDA;
			}
		}
		return kode;
	}

	/**
	 * Setter penanda kegiatan bawaan.
	 *
	 * @param defaultKegiatan {@code true} bila jenis kegiatan ini menjadi pilihan bawaan
	 */
	public void setDefaultKegiatan(Boolean defaultKegiatan) {
		this.defaultKegiatan = defaultKegiatan;
	}

	/**
	 * Penanda bahwa jenis kegiatan ini menjadi pilihan bawaan pada form pembuatan tagihan.
	 *
	 * <p>Berbeda dari kebanyakan flag di kelas ini, getter ini mengembalikan field apa adanya
	 * &mdash; termasuk {@code null} bila kolomnya kosong di database. Pemanggil yang melakukan
	 * auto-unboxing ke {@code boolean} harus berjaga terhadap {@code NullPointerException};
	 * bandingkan dengan {@link #getDefaultPembayaran()} yang sudah memakai penjaga ternary.</p>
	 *
	 * @return {@code true}/{@code false}/{@code null} sesuai isi kolom
	 */
	@Column(name = "default_kegiatan")
	public Boolean getDefaultKegiatan() {
		return defaultKegiatan;
	}

	/**
	 * Status aktif jenis kegiatan &mdash; gerbang yang menentukan apakah katalog ini masih
	 * boleh dipakai membentuk tagihan baru dan ikut terbawa pada
	 * {@code CommonHelperClass.jenisKegiatansAktif}.
	 *
	 * <h4>Perilaku</h4>
	 * <ol>
	 *   <li>Bila field {@code aktif} masih {@code null}, ia <b>diisi</b> {@code true} &mdash;
	 *       jadi jenis kegiatan yang belum pernah dikonfigurasi dianggap aktif.</li>
	 *   <li>Bila {@code id} entity ini sama dengan salah satu dari tiga sentinel di
	 *       {@link ais.common.ConstantValues} &mdash; {@code PENDAFTARAN_CALON_MAHASISWA},
	 *       {@code PENDAFTARAN_MAHASISWA_LAMA}, atau
	 *       {@code PENDAFTARAN_ULANG_MAHASISWA_BARU} &mdash; nilainya <b>dipaksa</b>
	 *       {@code true} berapa pun isi kolomnya.</li>
	 * </ol>
	 *
	 * <h4>GETTER DESTRUKTIF &amp; FLAG SATU ARAH</h4>
	 * <p>Kedua cabang di atas melakukan penugasan {@code aktif = true} ke field, bukan sekadar
	 * mengembalikan nilai. Karena property ini dipetakan ke kolom dan Hibernate memanggil
	 * getter saat {@code flush}, efeknya:</p>
	 * <ul>
	 *   <li>Membaca entity yang {@code aktif}-nya {@code NULL} menulis {@code true} ke
	 *       database tanpa diminta, plus satu revisi Envers palsu.</li>
	 *   <li>Ketiga sentinel menjadi <b>mustahil dinonaktifkan</b> lewat antarmuka mana pun:
	 *       operator boleh saja menghilangkan centang &quot;Aktif&quot; dan menyimpannya,
	 *       tetapi pembacaan berikutnya menuliskan kembali {@code true} ke baris tersebut.
	 *       Ini pola <i>flag satu arah</i> &mdash; nilainya hanya bisa bergerak ke
	 *       {@code true}, tidak pernah kembali. Perilaku ini agaknya disengaja sebagai
	 *       pengaman agar alur pendaftaran/registrasi ulang tidak dapat dimatikan sehingga
	 *       menutup akses seluruh mahasiswa, tetapi ia menjadikan kolom {@code aktif} pada
	 *       ketiga baris itu <b>menyesatkan</b> bila dibaca langsung lewat SQL.</li>
	 * </ul>
	 *
	 * <p>Perhatikan juga bahwa perbandingannya dilakukan atas {@code id}, sehingga pada
	 * instalasi yang konstanta sentinel-nya belum ter-<i>resolve</i> (bernilai {@code null}
	 * saat awal <i>startup</i>) pemaksaan ini tidak berjalan &mdash; nilai kolom dipakai
	 * apa adanya. Setiap perbandingan sudah dijaga {@code != null} sehingga tidak melempar.</p>
	 *
	 * @return {@code true} bila jenis kegiatan aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		JenisKegiatan jenisKegiatan = this;
		if (jenisKegiatan.getId() != null) {
			if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
					&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
					|| (ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null
							&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA.getId()))
					|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
							&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {
				aktif = true;
			}
		}

		return aktif;
	}

	/**
	 * Setter status aktif. Perhatikan bahwa untuk tiga jenis kegiatan sentinel, nilai
	 * {@code false} yang disimpan di sini akan ditimpa kembali menjadi {@code true} oleh
	 * {@link #getAktif()} pada pembacaan berikutnya.
	 *
	 * @param aktif status aktif yang diinginkan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Apakah pelunasan tagihan jenis ini menjadi syarat mahasiswa boleh mengisi KRS.
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Bila {@code namaKegiatan} sama dengan
	 * {@code ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA} (registrasi ulang), field dipaksa
	 * {@code true} dan ditulis balik &mdash; <i>flag satu arah</i> yang tidak dapat
	 * dimatikan operator, sekaligus memicu {@code UPDATE} + revisi Envers palsu saat entity
	 * sekadar dibaca. Logikanya masuk akal (registrasi ulang memang prasyarat KRS), tetapi
	 * cara penerapannya membuat isi kolom di database tidak dapat dipercaya.</p>
	 *
	 * <p>Perhatikan pencocokan di sini memakai {@link String#equalsIgnoreCase(String)} atas
	 * <b>field</b> {@code namaKegiatan} secara langsung &mdash; berbeda dari
	 * {@link #getKode()} dan {@link #getMaxSmt()} yang memakai {@code equals()} case-sensitive
	 * lewat {@link #getNamaKegiatan()}. Ketidakseragaman ini berarti nama kegiatan dengan
	 * kapitalisasi berbeda dapat mengaktifkan flag KRS namun TIDAK memicu penurunan kode
	 * pembayaran maupun batas semester bawaannya.</p>
	 *
	 * @return {@code true} bila menjadi syarat pengisian KRS; tidak pernah {@code null}
	 */
	@Column(name = "digunakan_untuk_pengecekan_krs")
	public Boolean getDigunakanUntukPengecekanKrs() {
		if (namaKegiatan != null && namaKegiatan.equalsIgnoreCase(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA)) {
			digunakanUntukPengecekanKrs = true;
		}
		return digunakanUntukPengecekanKrs == null ? false : digunakanUntukPengecekanKrs;
	}

	/**
	 * Setter syarat KRS. Untuk jenis kegiatan bernama registrasi ulang, nilai {@code false}
	 * akan ditimpa kembali menjadi {@code true} oleh
	 * {@link #getDigunakanUntukPengecekanKrs()}.
	 *
	 * @param digunakanUntukPengecekanKrs status syarat KRS yang diinginkan
	 */
	public void setDigunakanUntukPengecekanKrs(Boolean digunakanUntukPengecekanKrs) {
		this.digunakanUntukPengecekanKrs = digunakanUntukPengecekanKrs;
	}

	/**
	 * Penanda bahwa jenis kegiatan ini menjadi pilihan bawaan pada form pembayaran.
	 * Getter murni dengan penjaga ternary ({@code null} dibaca sebagai {@code false}) tanpa
	 * menulis balik ke field.
	 *
	 * @return {@code true} bila menjadi pilihan bawaan pembayaran; tidak pernah {@code null}
	 */
	@Column(name = "default_pembayaran")
	public Boolean getDefaultPembayaran() {
		return defaultPembayaran == null ? false : defaultPembayaran;
	}

	/**
	 * Setter penanda bawaan pembayaran.
	 *
	 * @param defaultPembayaran status bawaan pembayaran
	 */
	public void setDefaultPembayaran(Boolean defaultPembayaran) {
		this.defaultPembayaran = defaultPembayaran;
	}

	/**
	 * Semester paling awal jenis kegiatan ini berlaku; {@code null} dibaca sebagai {@code 0}
	 * (berlaku sejak semester nol/pendaftaran).
	 *
	 * <p>Getter murni &mdash; hanya ternary, tidak menulis balik ke field. Bandingkan dengan
	 * pasangannya {@link #getMaxSmt()} yang justru destruktif.</p>
	 *
	 * @return batas semester bawah; tidak pernah {@code null}
	 */
	public Integer getMinSmt() {
		return minSmt == null ? 0 : minSmt;
	}

	/**
	 * Setter batas semester bawah.
	 *
	 * @param minSmt semester paling awal jenis kegiatan berlaku
	 */
	public void setMinSmt(Integer minSmt) {
		this.minSmt = minSmt;
	}

	/**
	 * Semester paling akhir jenis kegiatan ini berlaku, dengan nilai bawaan yang bergantung
	 * pada nama kegiatan.
	 *
	 * <h4>Nilai bawaan menurut nama</h4>
	 * <ul>
	 *   <li>{@code PENDAFTARAN_MAHASISWA_LAMA} (registrasi ulang) &rarr; {@code 14}</li>
	 *   <li>{@code PENDAFTARAN_CALON_MAHASISWA} &rarr; {@code 1}</li>
	 *   <li>{@code PENDAFTARAN_ULANG_MAHASISWA_BARU} &rarr; {@code 1}</li>
	 *   <li>{@code PENDAFTARAN_WISUDA} &rarr; {@code 20}</li>
	 *   <li>selain itu &rarr; {@code 30}</li>
	 * </ul>
	 *
	 * <h4>GETTER DESTRUKTIF</h4>
	 * <p>Seluruh cabang di atas menulis balik ke field {@code maxSmt}. Berbeda dari
	 * {@link #getAktif()} atau {@link #getDigunakanUntukPengecekanKrs()}, penulisan di sini
	 * dijaga {@code if (maxSmt == null)} sehingga nilai yang sudah diisi operator TIDAK
	 * ditimpa &mdash; ini bentuk <i>auto-seed</i>, bukan pemaksaan satu arah. Efek sampingnya
	 * tetap ada: baris yang {@code max_smt}-nya {@code NULL} akan tertulis diam-diam ke
	 * database pada pembacaan pertama di dalam session terbuka, lengkap dengan revisi Envers.
	 * Sifat auto-seed ini juga berarti mengubah nama kegiatan TIDAK mengubah batas semester
	 * yang sudah terlanjur tersimpan.</p>
	 *
	 * <h4>Dampak hilir</h4>
	 * <p>Pasangan {@link #getMinSmt()}/{@code getMaxSmt()} dibaca oleh
	 * {@link Kegiatan#getAktif()}: sebuah header tagihan yang semesternya berada di luar
	 * rentang ini otomatis dianggap tidak aktif, yang pada gilirannya mengubah cara
	 * {@link Kegiatan#getKodeunik()} membentuk kunci unik (memakai barcode acak alih-alih
	 * format {@code MHS_*}/{@code CAL_MHS_*}). Method ini juga menjadi gerbang pertama
	 * {@link #bolehMasuk} &mdash; semester di luar rentang langsung diloloskan tanpa
	 * memeriksa tunggakan sama sekali.</p>
	 *
	 * @return batas semester atas; tidak pernah {@code null}
	 */
	public Integer getMaxSmt() {
		if (getNamaKegiatan() != null && getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA)) {
			if (maxSmt == null)
				maxSmt = 14;
		} else if (getNamaKegiatan() != null && getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
			if (maxSmt == null)
				maxSmt = 1;
		} else if (getNamaKegiatan() != null
				&& getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
			if (maxSmt == null)
				maxSmt = 1;
		} else if (getNamaKegiatan() != null && getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_WISUDA)) {
			if (maxSmt == null)
				maxSmt = 20;
		}
		if (maxSmt == null) {
			maxSmt = 30;
		}
		return maxSmt;
	}

	/**
	 * Setter batas semester atas. Nilai eksplisit di sini dihormati {@link #getMaxSmt()}
	 * (yang hanya mengisi bawaan saat field masih {@code null}).
	 *
	 * @param maxSmt semester paling akhir jenis kegiatan berlaku
	 */
	public void setMaxSmt(Integer maxSmt) {
		this.maxSmt = maxSmt;
	}

	/**
	 * Apakah pelunasan tagihan jenis ini menjadi syarat mahasiswa boleh mengikuti ujian.
	 *
	 * <p><b>GETTER DESTRUKTIF (auto-seed).</b> Bila field masih {@code null}, nilainya diisi
	 * dari {@link #getDigunakanUntukPengecekanKrs()} lalu ditulis balik ke field &mdash;
	 * asumsinya &quot;yang menghalangi KRS juga menghalangi ujian&quot;. Karena
	 * {@code getDigunakanUntukPengecekanKrs()} sendiri destruktif dan memaksa {@code true}
	 * untuk registrasi ulang, satu kali pembacaan entity dapat menuliskan DUA kolom sekaligus
	 * ke database beserta revisi Envers untuk keduanya.</p>
	 *
	 * <p>Berbeda dari kerabatnya, getter ini tidak memasang penjaga ternary di akhir; ia
	 * mengembalikan hasil {@code getDigunakanUntukPengecekanKrs()} yang memang tidak pernah
	 * {@code null}, sehingga hasilnya tetap aman dari {@code NullPointerException}.</p>
	 *
	 * @return {@code true} bila menjadi syarat mengikuti ujian; tidak pernah {@code null}
	 */
	public Boolean getDigunakanUntukPengecekanUjian() {
		if (digunakanUntukPengecekanUjian == null) {
			digunakanUntukPengecekanUjian = getDigunakanUntukPengecekanKrs();
		}
		return digunakanUntukPengecekanUjian;
	}

	/**
	 * Setter syarat ujian. Nilai eksplisit di sini dihormati oleh
	 * {@link #getDigunakanUntukPengecekanUjian()} karena auto-seed hanya berjalan saat field
	 * masih {@code null}.
	 *
	 * @param digunakanUntukPengecekanUjian status syarat ujian
	 */
	public void setDigunakanUntukPengecekanUjian(Boolean digunakanUntukPengecekanUjian) {
		this.digunakanUntukPengecekanUjian = digunakanUntukPengecekanUjian;
	}

	/**
	 * Apakah pelunasan tagihan jenis ini menentukan status <b>keaktifan</b> mahasiswa pada
	 * semester berjalan (dipakai laporan status, surat keterangan aktif, dan pelaporan
	 * eksternal).
	 *
	 * <p><b>GETTER DESTRUKTIF dengan dua perilaku berbeda:</b></p>
	 * <ul>
	 *   <li>Untuk {@code PENDAFTARAN_MAHASISWA_LAMA} (registrasi ulang) nilainya
	 *       <b>dipaksa</b> {@code true} tanpa syarat &mdash; <i>flag satu arah</i> yang tidak
	 *       dapat dimatikan operator.</li>
	 *   <li>Untuk {@code PENDAFTARAN_ULANG_MAHASISWA_BARU} nilainya hanya di-<i>auto-seed</i>
	 *       {@code true} saat field masih {@code null}, sehingga pilihan operator dihormati.</li>
	 * </ul>
	 * <p>Perbedaan halus antara kedua cabang ini mudah luput saat membaca sekilas; keduanya
	 * sama-sama menulis balik ke field sehingga sama-sama dapat memicu {@code UPDATE} dan
	 * revisi Envers palsu, tetapi hanya cabang pertama yang menjadikan kolom database
	 * menyesatkan secara permanen.</p>
	 *
	 * @return {@code true} bila menentukan keaktifan; tidak pernah {@code null}
	 */
	public Boolean getDigunakanSyaratKeaktifan() {
		if (getNamaKegiatan() != null && getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA)) {
			digunakanSyaratKeaktifan = true;
		} else if (getNamaKegiatan() != null
				&& getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
			if (digunakanSyaratKeaktifan == null)
				digunakanSyaratKeaktifan = true;
		}
		return digunakanSyaratKeaktifan == null ? false : digunakanSyaratKeaktifan;
	}

	/**
	 * Setter syarat keaktifan. Untuk jenis kegiatan registrasi ulang nilai {@code false}
	 * akan ditimpa kembali menjadi {@code true} pada pembacaan berikutnya.
	 *
	 * @param digunakanSyaratKeaktifan status syarat keaktifan
	 */
	public void setDigunakanSyaratKeaktifan(Boolean digunakanSyaratKeaktifan) {
		this.digunakanSyaratKeaktifan = digunakanSyaratKeaktifan;
	}

	/**
	 * Apakah tunggakan tagihan jenis ini <b>memblokir login</b> mahasiswa ke sistem.
	 *
	 * <p>Getter murni (ternary saja, tanpa menulis balik ke field) &mdash; sikap yang tepat
	 * untuk sebuah gerbang akses. Nilainya menjadi saringan pertama pada
	 * {@link #apakahBoleh(Mahasiswa, int, List)}: jenis kegiatan yang mengembalikan
	 * {@code false} di sini dilewati sepenuhnya, tunggakan berapa pun tidak diperiksa.</p>
	 *
	 * @return {@code true} bila menjadi syarat login; tidak pernah {@code null}
	 */
	public Boolean getDigunakanSyaratLogin() {
		return digunakanSyaratLogin == null ? false : digunakanSyaratLogin;
	}

	/**
	 * Setter penanda syarat login.
	 *
	 * @param digunakanSyaratLogin {@code true} bila tunggakan jenis ini memblokir login
	 */
	public void setDigunakanSyaratLogin(Boolean digunakanSyaratLogin) {
		this.digunakanSyaratLogin = digunakanSyaratLogin;
	}

	/**
	 * Ambang persentase pelunasan dasar (semester berjalan / semester mundur yang tidak punya
	 * varian sendiri) agar mahasiswa boleh login; {@code null} dibaca sebagai {@code 0.0}.
	 *
	 * <p>Nilai ini juga menjadi <i>fallback</i> bagi keempat belas varian bernomor
	 * {@code getPersenSyaratLogin1()} &hellip; {@code getPersenSyaratLogin14()}. Perhatikan
	 * ambang {@code 0.0} berarti pemeriksaan dilewati sepenuhnya: {@link #bolehMasuk} hanya
	 * memeriksa tagihan bila {@code persenSyarat > 0.1}.</p>
	 *
	 * @return ambang persentase; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin() {
		return persenSyaratLogin == null ? 0.0 : persenSyaratLogin;
	}

	/**
	 * Setter ambang persentase pelunasan dasar.
	 *
	 * @param persenSyaratLogin ambang persentase (0&ndash;100)
	 */
	public void setPersenSyaratLogin(Double persenSyaratLogin) {
		this.persenSyaratLogin = persenSyaratLogin;
	}

	/**
	 * Ambang persentase pelunasan untuk tagihan <b>satu semester ke belakang</b>;
	 * jatuh kembali ke {@link #getPersenSyaratLogin()} bila belum diisi.
	 *
	 * @return ambang persentase untuk semester mundur ke-1; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin1() {
		return persenSyaratLogin1 == null ? getPersenSyaratLogin() : persenSyaratLogin1;
	}

	/**
	 * Setter ambang persentase untuk semester mundur ke-1.
	 *
	 * @param persenSyaratLogin1 ambang persentase; {@code null} berarti ikut ambang dasar
	 */
	public void setPersenSyaratLogin1(Double persenSyaratLogin1) {
		this.persenSyaratLogin1 = persenSyaratLogin1;
	}

	/**
	 * Ambang persentase pelunasan untuk tagihan <b>dua semester ke belakang</b>;
	 * jatuh kembali ke {@link #getPersenSyaratLogin()} bila belum diisi.
	 *
	 * @return ambang persentase untuk semester mundur ke-2; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin2() {
		return persenSyaratLogin2 == null ? getPersenSyaratLogin() : persenSyaratLogin2;
	}

	/**
	 * Setter ambang persentase untuk semester mundur ke-2.
	 *
	 * @param persenSyaratLogin2 ambang persentase; {@code null} berarti ikut ambang dasar
	 */
	public void setPersenSyaratLogin2(Double persenSyaratLogin2) {
		this.persenSyaratLogin2 = persenSyaratLogin2;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan: hanya <b>semester berjalan</b> yang diperiksa.
	 *
	 * <p>Merupakan elemen ke-0 dari deret sembilan flag {@code bayarHanyaSmtSaatIni},
	 * {@code ...DanSebelumnya}, {@code ...DanSebelumnyalagi}, {@code ...lagi3} &hellip;
	 * {@code ...lagi8} yang disusun {@link #apakahBoleh(Mahasiswa, int, List)} menjadi array
	 * {@code aturanSmtMundur}. Indeks array berarti &quot;berapa semester mundur dari
	 * semester berjalan&quot;.</p>
	 *
	 * @return {@code true} bila hanya semester berjalan yang diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIni() {
		return bayarHanyaSmtSaatIni == null ? false : bayarHanyaSmtSaatIni;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-0.
	 *
	 * @param bayarHanyaSmtSaatIni status aturan
	 */
	public void setBayarHanyaSmtSaatIni(Boolean bayarHanyaSmtSaatIni) {
		this.bayarHanyaSmtSaatIni = bayarHanyaSmtSaatIni;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan untuk <b>satu semester mundur</b> (elemen ke-1
	 * array {@code aturanSmtMundur} pada {@link #apakahBoleh(Mahasiswa, int, List)}).
	 *
	 * @return {@code true} bila semester mundur ke-1 ikut diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIniDanSebelumnya() {
		return bayarHanyaSmtSaatIniDanSebelumnya == null ? false : bayarHanyaSmtSaatIniDanSebelumnya;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-1.
	 *
	 * @param bayarHanyaSmtSaatIniDanSebelumnya status aturan
	 */
	public void setBayarHanyaSmtSaatIniDanSebelumnya(Boolean bayarHanyaSmtSaatIniDanSebelumnya) {
		this.bayarHanyaSmtSaatIniDanSebelumnya = bayarHanyaSmtSaatIniDanSebelumnya;
	}

	/**
	 * <b>Gerbang login berbasis tunggakan.</b> Memeriksa seluruh jenis kegiatan yang ditandai
	 * sebagai syarat login, lalu mengisi {@code warning} dengan pesan penolakan untuk setiap
	 * semester yang tunggakannya melampaui ambang. Dipanggil dari alur autentikasi mahasiswa.
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li><b>Muat katalog.</b> Bekerja atas cache statis
	 *       {@code CommonHelperClass.jenisKegiatansAktif}. Bila cache masih {@code null},
	 *       {@code Common.reloadJenisKegiatans()} dipanggil; bila setelah itu tetap
	 *       {@code null}, method <b>keluar diam-diam</b>.</li>
	 *   <li><b>Saring.</b> Jenis kegiatan yang {@link #getDigunakanSyaratLogin()}-nya
	 *       {@code false} dilewati.</li>
	 *   <li><b>Tentukan cakupan semester.</b> Sembilan flag {@code bayarHanyaSmt*} disusun
	 *       menjadi array; indeks {@code i} berarti &quot;semester berjalan dikurangi
	 *       {@code i}&quot;. Bila setidaknya satu flag menyala, hanya semester-semester yang
	 *       flag-nya aktif yang diperiksa. Bila tidak ada satu pun yang menyala, sistem
	 *       memeriksa <b>seluruh</b> semester dari {@link #getMinSmt()} sampai semester
	 *       berjalan.</li>
	 *   <li><b>Hormati cuti.</b> Semester yang mahasiswanya punya
	 *       {@link PendaftaranCutiMahasiswa} dengan {@code getPersetujuan()} bernilai benar
	 *       dilewati &mdash; mahasiswa cuti resmi tidak diblokir karena tidak membayar
	 *       semester tersebut.</li>
	 *   <li><b>Periksa.</b> Sisanya diserahkan ke {@link #bolehMasuk}.</li>
	 * </ol>
	 *
	 * <h4>Catatan perilaku yang perlu diketahui</h4>
	 * <p><b>Fail-open pada kegagalan pemuatan katalog.</b> Bila cache jenis kegiatan gagal
	 * dimuat, method langsung {@code return} tanpa mengisi {@code warning} sama sekali
	 * &mdash; pemanggil akan menyimpulkan &quot;tidak ada halangan&quot; dan mengizinkan
	 * login. Untuk sebuah gerbang akses, sikap ini adalah <i>fail-open</i>: gangguan pada
	 * pemuatan katalog membuka akses, bukan menutupnya. Perlu diingat bahwa gerbang ini
	 * bersifat administratif (penagihan), bukan pengendali kerahasiaan data, sehingga
	 * dampaknya adalah kebocoran akses bagi penunggak, bukan kebocoran data.</p>
	 *
	 * <p><b>Cache statis lintas-permintaan.</b> {@code jenisKegiatansAktif} adalah cache
	 * statis tingkat aplikasi. Perubahan konfigurasi jenis kegiatan oleh operator baru
	 * berlaku setelah cache di-<i>reload</i>; sampai saat itu gerbang login masih memakai
	 * aturan lama. Karena elemen di dalamnya adalah entity yang mungkin sudah
	 * <i>detached</i>, pemanggilan getter destruktif atasnya (mis. {@link #getMaxSmt()} lewat
	 * {@link #bolehMasuk}) tidak akan menuliskan apa pun ke database &mdash; efek samping
	 * penulisan hanya terjadi bila entity yang sama juga terikat pada session yang terbuka.</p>
	 *
	 * <p><b>Cuti tanpa persetujuan tetap ditagih.</b> Syaratnya {@code cuti == null ||
	 * !cuti.getPersetujuan()} &mdash; artinya pengajuan cuti yang belum disetujui
	 * diperlakukan sama dengan tidak mengajukan cuti sama sekali. Ini konsisten dengan
	 * kebijakan bahwa cuti baru meniadakan kewajiban setelah disahkan.</p>
	 *
	 * <p><b>Deret sembilan versus empat belas.</b> Array cakupan hanya menyusun sembilan flag
	 * ({@code bayarHanyaSmtSaatIni} sampai {@code ...lagi8}), sedangkan kelas ini menyediakan
	 * flag {@code ...lagi9} sampai {@code ...lagi14} <i>dan</i> ambang persen sampai
	 * {@link #getPersenSyaratLogin14()}. Enam flag cakupan terakhir karena itu tidak pernah
	 * dibaca dari jalur ini &mdash; field tidur (<i>dormant</i>) yang tetap tampil pada form
	 * konfigurasi. Operator yang mencentangnya akan mengira ia mengaktifkan pemeriksaan
	 * semester mundur ke-9 sampai ke-14, padahal tidak ada efeknya; bila tidak ada flag lain
	 * yang menyala, sistem justru jatuh ke cabang &quot;periksa semua semester&quot;.</p>
	 *
	 * @param mahasiswa  mahasiswa yang sedang mencoba masuk
	 * @param currentSmt semester berjalan mahasiswa tersebut
	 * @param warning    daftar keluaran; setiap pelanggaran menambahkan satu pesan
	 * @throws Exception diteruskan dari {@link #bolehMasuk} maupun dari pemuatan katalog
	 */
	public static void apakahBoleh(Mahasiswa mahasiswa, int currentSmt, List<String> warning) throws Exception {
		// 1. Reload data jika null
		if (ais.common.CommonHelperClass.jenisKegiatansAktif == null) {
			Common.reloadJenisKegiatans();
		}

		if (ais.common.CommonHelperClass.jenisKegiatansAktif == null) {
			return; // Cegah NullPointerException jika reload tetap gagal
		}

		// 2. Iterasi langsung tanpa membuat ArrayList sementara (Hemat Memory)
		for (JenisKegiatan syarat : ais.common.CommonHelperClass.jenisKegiatansAktif) {
			if (!syarat.getDigunakanSyaratLogin()) {
				continue; // Skip jika tidak digunakan sebagai syarat login
			}

			// 3. Masukkan aturan boolean ke dalam Array agar bisa di-loop secara dinamis
			boolean[] aturanSmtMundur = { syarat.getBayarHanyaSmtSaatIni(),
					syarat.getBayarHanyaSmtSaatIniDanSebelumnya(), syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi(),
					syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi3(),
					syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi4(),
					syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi5(),
					syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi6(),
					syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi7(),
					syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi8() };

			// Cek apakah ada spesifik semester mundur yang diaktifkan
			boolean adaAturanSpesifik = false;
			for (boolean aturanAktif : aturanSmtMundur) {
				if (aturanAktif) {
					adaAturanSpesifik = true;
					break;
				}
			}

			if (adaAturanSpesifik) {
				// Loop 0 s.d 8 sesuai dengan index boolean array di atas
				for (int i = 0; i < aturanSmtMundur.length; i++) {
					if (aturanSmtMundur[i] && currentSmt > i) {
						int targetSmt = currentSmt - i;
						PendaftaranCutiMahasiswa cuti = mahasiswa.ambilCuti(targetSmt, null, false);

						// Jika tidak cuti atau cuti tidak disetujui, maka cek syarat masuk
						if (cuti == null || !cuti.getPersetujuan()) {
							syarat.bolehMasuk(mahasiswa, targetSmt, i, warning);
						}
					}
				}
			} else {
				// Jika tidak ada aturan spesifik mundur, cek dari minimal semester sampai saat
				// ini
				for (int smtMulai = syarat.getMinSmt(); smtMulai <= currentSmt; smtMulai++) {
					PendaftaranCutiMahasiswa cuti = mahasiswa.ambilCuti(smtMulai, null, false);
					if (cuti == null || !cuti.getPersetujuan()) {
						syarat.bolehMasuk(mahasiswa, smtMulai, 0, warning);
					}
				}
			}
		}
	}

	/**
	 * Memeriksa apakah seorang mahasiswa memenuhi ambang pelunasan jenis kegiatan ini pada
	 * <b>satu</b> semester tertentu. Merupakan inti pemeriksaan yang dipanggil berulang oleh
	 * {@link #apakahBoleh(Mahasiswa, int, List)}.
	 *
	 * <h4>Urutan gerbang</h4>
	 * <ol>
	 *   <li><b>Rentang semester.</b> {@code smtMulai == 0}, atau berada di luar
	 *       {@link #getMinSmt()}&ndash;{@link #getMaxSmt()}, langsung diloloskan. Perhatikan
	 *       bahwa {@link #getMaxSmt()} adalah getter destruktif ber-auto-seed.</li>
	 *   <li><b>Pengecualian semester ganjil/genap.</b> Mahasiswa yang mulai belajar di
	 *       semester {@link Perkuliahan#GENAP} diloloskan pada {@code smtMulai == 1}, karena
	 *       semester 1 bagi mereka tidak pernah benar-benar ada.</li>
	 *   <li><b>Pilih ambang.</b> {@code switch} atas {@code smtMundur} memilih salah satu dari
	 *       {@code getPersenSyaratLogin1()} &hellip; {@code getPersenSyaratLogin14()};
	 *       {@code default} (termasuk {@code smtMundur == 0}) memakai
	 *       {@link #getPersenSyaratLogin()}.</li>
	 *   <li><b>Bypass administratif.</b>
	 *       {@code Common.checkBaypassStatusPembayaranMahasiswa(...)} memberi jalan keluar
	 *       bagi mahasiswa yang statusnya dikecualikan (mis. penerima beasiswa penuh atau
	 *       keringanan yang disetujui).</li>
	 *   <li><b>Ambang nol berarti tidak diperiksa.</b> Seluruh pemeriksaan tagihan hanya
	 *       berjalan bila {@code persenSyarat > 0.1}. Ambang {@code 0} &mdash; yang juga
	 *       merupakan nilai bawaan saat kolomnya {@code NULL} &mdash; menonaktifkan gerbang.</li>
	 *   <li><b>Ambil header tagihan.</b> Untuk jenis kegiatan pendaftaran calon mahasiswa
	 *       atau daftar ulang mahasiswa baru, {@link Kegiatan} dicari lebih dulu lewat
	 *       {@link BiodataCalonMahasiswa} (karena tagihannya melekat pada berkas calon,
	 *       bukan pada NIM). Bila tidak ketemu, pencarian jatuh ke
	 *       {@code mahasiswa.ambilKegiatans(smtMulai, this)}.</li>
	 *   <li><b>Bandingkan.</b> Bila {@link Kegiatan#getPersentase()} di bawah ambang, sebuah
	 *       dialog modal ZK ditampilkan dan pesan peringatan ditambahkan ke {@code warning},
	 *       lalu {@code false} dikembalikan.</li>
	 * </ol>
	 *
	 * <h4>Catatan penting</h4>
	 * <p><b>Tagihan yang tidak ditemukan berarti LOLOS.</b> Bila {@code kegiatan} bernilai
	 * {@code null} &mdash; mahasiswa belum pernah dibuatkan header tagihan untuk semester
	 * tersebut &mdash; perbandingan dilewati dan method mengembalikan {@code true}. Ini
	 * kebijakan <i>fail-open</i> yang disengaja dan memang perlu (mahasiswa tidak boleh
	 * diblokir karena bagian keuangan belum menerbitkan tagihannya), tetapi berarti gerbang
	 * ini <b>tidak bisa dipakai untuk membuktikan</b> bahwa seorang mahasiswa lunas &mdash;
	 * hanya untuk membuktikan bahwa ia tidak tercatat menunggak.</p>
	 *
	 * <p><b>Persentase dihitung ulang dari JSON, bukan dari kolom.</b>
	 * {@link Kegiatan#getPersentase()} menurunkan angkanya dari field {@code tagihan} dan
	 * {@code dibayar}, yang pada gilirannya berasal dari penguraian kolom JSON
	 * {@code tagihans}/{@code bulans}. Jika kedua field itu belum dihitung ulang
	 * ({@link Kegiatan#hitungTagihan()} / {@link Kegiatan#hitungDibayar()}), nilai yang
	 * dibandingkan adalah nilai tersimpan terakhir &mdash; bukan keadaan terkini.</p>
	 *
	 * <p><b>Efek samping antarmuka di dalam method model.</b> Blok besar di tengah method ini
	 * membangun jendela modal ZK ({@link ais.ui.util.MyWindow}) berisi halaman informasi
	 * pembayaran, lengkap dengan tombol yang memanggil {@code Common.goLogoff()}. Menempatkan
	 * kode presentasi di dalam kelas entity berarti method ini <b>tidak dapat dipakai ulang</b>
	 * dari konteks non-ZK (REST, penjadwal, batch) tanpa efek samping. Seluruh blok itu
	 * dibungkus {@code try/catch} yang mencatat error dan melanjutkan, sehingga pada konteks
	 * tanpa halaman ZK aktif ({@code ExecutionsCtrl.getCurrentCtrl()} bernilai {@code null})
	 * pemeriksaan tetap berjalan benar &mdash; hanya dialognya yang tidak muncul. Pesan
	 * peringatan tetap ditambahkan ke {@code warning} di luar blok tersebut, sehingga
	 * pemanggil non-UI tetap menerima hasil yang benar.</p>
	 *
	 * @param mahasiswa mahasiswa yang diperiksa
	 * @param smtMulai  semester yang sedang diperiksa
	 * @param smtMundur jarak semester ke belakang dari semester berjalan; menentukan varian
	 *                  ambang persen mana yang dipakai
	 * @param warning   daftar keluaran; satu pesan ditambahkan bila pemeriksaan gagal
	 * @return {@code true} bila mahasiswa boleh masuk untuk semester ini
	 * @throws Exception diteruskan dari pengambilan data cuti/tagihan
	 */
	public boolean bolehMasuk(Mahasiswa mahasiswa, int smtMulai, int smtMundur, List<String> warning) throws Exception {
		// Validasi batas rentang semester
		if (smtMulai == 0 || getMinSmt() > smtMulai || getMaxSmt() < smtMulai) {
			return true;
		}

		// Pengecualian khusus untuk Genap smt 1
		if (Perkuliahan.GENAP.equalsIgnoreCase(mahasiswa.getSemesterMulai()) && smtMulai == 1) {
			return true;
		}

		// Gunakan Switch-Case untuk menentukan persen syarat agar lebih cepat (O(1))
		// dan rapi
		double persenSyarat = 0.0;
		switch (smtMundur) {
		case 1:
			persenSyarat = this.getPersenSyaratLogin1();
			break;
		case 2:
			persenSyarat = this.getPersenSyaratLogin2();
			break;
		case 3:
			persenSyarat = this.getPersenSyaratLogin3();
			break;
		case 4:
			persenSyarat = this.getPersenSyaratLogin4();
			break;
		case 5:
			persenSyarat = this.getPersenSyaratLogin5();
			break;
		case 6:
			persenSyarat = this.getPersenSyaratLogin6();
			break;
		case 7:
			persenSyarat = this.getPersenSyaratLogin7();
			break;
		case 8:
			persenSyarat = this.getPersenSyaratLogin8();
			break;
		case 9:
			persenSyarat = this.getPersenSyaratLogin9();
			break;
		case 10:
			persenSyarat = this.getPersenSyaratLogin10();
			break;
		case 11:
			persenSyarat = this.getPersenSyaratLogin11();
			break;
		case 12:
			persenSyarat = this.getPersenSyaratLogin12();
			break;
		case 13:
			persenSyarat = this.getPersenSyaratLogin13();
			break;
		case 14:
			persenSyarat = this.getPersenSyaratLogin14();
			break;
		default:
			persenSyarat = this.getPersenSyaratLogin();
			break; // Berlaku untuk index 0 atau tidak terdaftar
		}

		// Jika lolos bypass status, tidak perlu cek tagihan
		if (Common.checkBaypassStatusPembayaranMahasiswa(smtMulai, null, mahasiswa, this)) {
			return true;
		}

		if (persenSyarat > 0.1) {
			Kegiatan kegiatan = null;

			// Deteksi apakah ini Kegiatan Calon Mahasiswa atau Mahasiswa Baru
			boolean isPendaftaranUlangBaru = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
					&& this.getId() != null
					&& this.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId());

			boolean isPendaftaranCalonMhs = ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && this.getId() != null
					&& this.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId());

			// Penentuan Data Kegiatan
			if (mahasiswa.getBiodataCalonMahasiswa() != null && mahasiswa.getBiodataCalonMahasiswaData() != null) {
				if (isPendaftaranUlangBaru || isPendaftaranCalonMhs) {
					kegiatan = mahasiswa.getBiodataCalonMahasiswaData().ambilKegiatans(smtMulai, this);
				}
			}

			// Jika bukan dari pendaftaran calon mhs, ambil dari data mahasiswa aktif
			if (kegiatan == null) {
				kegiatan = mahasiswa.ambilKegiatans(smtMulai, this);
			}

			// Validasi Jika Punya Tunggakan dan Persentase Bayar di Bawah Syarat
			if (kegiatan != null && kegiatan.getPersentase() < persenSyarat) {

				// Blok Pemanggilan UI Modal
				try {
					ExecutionCtrl ctrl = ExecutionsCtrl.getCurrentCtrl();
					if (ctrl != null && ctrl.getCurrentPage() != null) {

						final MyWindow window = new MyWindow("", "none", false);
						ctrl.getCurrentPage().getFirstRoot().appendChild(window);

						org.zkoss.zul.Borderlayout borderlayout = new org.zkoss.zul.Borderlayout();
						borderlayout.setParent(window);

						org.zkoss.zul.Center center = new org.zkoss.zul.Center();
						center.setBorder("none");
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.appendChild(
								new ais.ui.util.MyInclude("/pages/master/informasi_pembayaran_mahasiswa.zul"));

						org.zkoss.zul.South south = new org.zkoss.zul.South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar();
						toolbar.setParent(south);

						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
								Common.goLogoff();
							}
						});
						cancel.setParent(toolbar);

						window.setVisible(true);
						window.setHeight("99%");
						window.setWidth("99%");
						window.onModal();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/JenisKegiatan.java:602"); // Tampilkan error ke server log, bukan ditelan diam-diam
				}

				String textPeringatan = "Mohon maaf, Anda belum dapat mengakses sistem. "
				        + "Syarat minimum pembayaran untuk tagihan \"" + this.getNamaKegiatan() + "\" "
				        + "di Semester " + smtMulai + " adalah " + Common.numberFormat.get().format(persenSyarat) + "%, "
				        + "sedangkan total pembayaran Anda saat ini tercatat sebesar " 
				        + Common.numberFormat.get().format(kegiatan.getPersentase()) + "%.\n\n"
				        + "Untuk informasi dan bantuan lebih lanjut, silakan menghubungi Bagian Keuangan.";

				warning.add(textPeringatan);

				return false;
			}
		}

		return true;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan untuk <b>semester mundur ke-2</b> &mdash;
	 * elemen indeks 2 pada array {@code aturanSmtMundur} yang disusun
	 * {@link #apakahBoleh(Mahasiswa, int, java.util.List)}.
	 *
	 * <p>Getter murni (ternary saja): {@code null} dibaca sebagai {@code false} tanpa
	 * menulis balik ke field.</p>
	 *
	 * @return {@code true} bila semester mundur ke-2 ikut diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-2.
	 *
	 * @param bayarHanyaSmtSaatIniDanSebelumnyalagi status aturan
	 */
	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi = bayarHanyaSmtSaatIniDanSebelumnyalagi;
	}

	/**
	 * Apakah tunggakan jenis kegiatan ini menyembunyikan <b>nilai/KHS</b> mahasiswa.
	 *
	 * <p>Getter murni dengan penjaga ternary &mdash; berbeda dari saudara-saudaranya
	 * {@link #getDigunakanUntukPengecekanKrs()} dan
	 * {@link #getDigunakanUntukPengecekanUjian()} yang keduanya destruktif. Tidak ada
	 * penurunan otomatis di sini: gerbang nilai harus dinyalakan operator secara eksplisit.</p>
	 *
	 * @return {@code true} bila menjadi syarat melihat nilai; tidak pernah {@code null}
	 */
	public Boolean getDigunakanUntukPengecekanNilai() {
		return digunakanUntukPengecekanNilai == null ? false : digunakanUntukPengecekanNilai;
	}

	/**
	 * Setter syarat melihat nilai.
	 *
	 * @param digunakanUntukPengecekanNilai status syarat nilai
	 */
	public void setDigunakanUntukPengecekanNilai(Boolean digunakanUntukPengecekanNilai) {
		this.digunakanUntukPengecekanNilai = digunakanUntukPengecekanNilai;
	}

	/**
	 * Larangan mengangsur: bila {@code true}, tagihan jenis ini harus dilunasi sekaligus
	 * dan tidak boleh dipecah menjadi {@link CicilanPembayaran}.
	 *
	 * <p>Perhatikan bahwa kelas ini menyimpan <b>tiga</b> penanda bermuatan mirip namun
	 * dengan mesin yang berbeda: {@code tidakBolehMengangsur} (larangan sederhana),
	 * {@link #getHanyaBerupaAngsuran()} dan {@link #getHanyaBerupaBukanAngsuran()}
	 * (pasangan yang diolah {@link #modeAngsuranUntukJenjang(Jenjang, Integer, Integer)}
	 * bersama JSON per-jenjang). Ketiganya tidak saling memvalidasi di tingkat entity,
	 * sehingga kombinasi yang bertentangan tetap dapat disimpan operator.</p>
	 *
	 * @return {@code true} bila angsuran dilarang; tidak pernah {@code null}
	 */
	public Boolean getTidakBolehMengangsur() {
		return tidakBolehMengangsur == null ? false : tidakBolehMengangsur;
	}

	/**
	 * Setter larangan mengangsur.
	 *
	 * @param tidakBolehMengangsur {@code true} bila angsuran dilarang
	 */
	public void setTidakBolehMengangsur(Boolean tidakBolehMengangsur) {
		this.tidakBolehMengangsur = tidakBolehMengangsur;
	}

	/**
	 * Apakah tagihan jenis ini tetap diterbitkan bagi mahasiswa yang sudah lulus (alumni).
	 *
	 * <h4>GETTER DESTRUKTIF ber-AUTO-SEED DARI KONFIGURASI GLOBAL</h4>
	 * <p>Bila field masih {@code null}, nilainya diturunkan lalu <b>ditulis balik</b> ke
	 * field melalui dua jalur:</p>
	 * <ol>
	 *   <li>Bila nama kegiatan mengandung kata &quot;wisuda&quot; (dicek dengan
	 *       {@code toLowerCase().contains(...)}, jadi cocok sebagian dan tidak peka huruf
	 *       besar/kecil), nilainya {@code true}. Logikanya: biaya wisuda memang wajar
	 *       ditagihkan justru kepada yang sudah selesai kuliah.</li>
	 *   <li>Selain itu, nilainya dibaca dari konfigurasi global lewat
	 *       {@code retreive("tagihan_juga_untuk_alumni")} dan dibandingkan dengan string
	 *       {@code "true"} secara tidak peka huruf besar/kecil.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi yang perlu diwaspadai.</b> Karena hasilnya ditulis balik ke field
	 * dan property ini dipetakan ke kolom, nilai konfigurasi global itu <i>dibekukan</i>
	 * ke dalam baris {@code jenis_kegiatan} pada pembacaan pertama di dalam session yang
	 * terbuka. Sesudah itu, mengubah konfigurasi global <b>tidak lagi berpengaruh</b>
	 * terhadap jenis kegiatan yang sudah terlanjur terbaca &mdash; sebagian baris akan
	 * mengikuti nilai lama dan sebagian mengikuti nilai baru, bergantung pada baris mana
	 * yang kebetulan pernah dibaca lebih dulu. Ini pola auto-seed yang sama dengan yang
	 * tercatat pada mekanisme konfigurasi AIS lainnya: nilai bawaan tidak tinggal sebagai
	 * bawaan, melainkan tertulis permanen ke data.</p>
	 *
	 * <p>Pencocokan &quot;wisuda&quot; dengan {@code contains} juga lebih longgar daripada
	 * pencocokan konstanta yang dipakai {@link #getKode()} dan {@link #getMaxSmt()};
	 * jenis kegiatan bernama mis. &quot;Denda Wisuda Terlambat&quot; ikut tertangkap.</p>
	 *
	 * @return {@code true} bila alumni tetap ditagih; tidak pernah {@code null} setelah
	 *         penurunan otomatis berjalan
	 */
	public Boolean getTagihanJugaUntukAlumni() {
		if (tagihanJugaUntukAlumni == null) {
			if (getNamaKegiatan() != null && getNamaKegiatan().toLowerCase().contains("wisuda")) {
				tagihanJugaUntukAlumni = true;
			} else {
				String alumni = retreive("tagihan_juga_untuk_alumni");
				if (alumni == null) {
					alumni = "";
				}
				tagihanJugaUntukAlumni = alumni.equalsIgnoreCase("true");
			}

		}
		return tagihanJugaUntukAlumni;
	}

	/**
	 * Setter penanda tagihan untuk alumni. Nilai eksplisit di sini mencegah auto-seed dari
	 * konfigurasi global pada {@link #getTagihanJugaUntukAlumni()}.
	 *
	 * @param tagihanJugaUntukAlumni status penagihan alumni
	 */
	public void setTagihanJugaUntukAlumni(Boolean tagihanJugaUntukAlumni) {
		this.tagihanJugaUntukAlumni = tagihanJugaUntukAlumni;
	}

	/**
	 * Apakah pelunasan jenis kegiatan ini menjadi syarat pencetakan surat keterangan
	 * bebas/aktif.
	 *
	 * <p>Getter murni, tetapi perhatikan bahwa bawaannya <b>{@code true}</b> &mdash;
	 * berlawanan dengan hampir seluruh flag lain di kelas ini yang berbawaan
	 * {@code false}. Artinya setiap jenis kegiatan baru secara bawaan ikut menghalangi
	 * pencetakan surat sampai operator mematikannya secara eksplisit. Sikap ini
	 * <i>fail-closed</i> dan aman, namun mudah mengejutkan karena tidak seragam dengan
	 * flag-flag tetangganya.</p>
	 *
	 * @return {@code true} bila menjadi syarat cetak surat; tidak pernah {@code null}
	 */
	public Boolean getDigunakanSyaratCetakSuratBebasAktif() {
		return digunakanSyaratCetakSuratBebasAktif == null ? true : digunakanSyaratCetakSuratBebasAktif;
	}

	/**
	 * Setter syarat cetak surat bebas/aktif.
	 *
	 * @param digunakanSyaratCetakSuratBebasAktif status syarat
	 */
	public void setDigunakanSyaratCetakSuratBebasAktif(Boolean digunakanSyaratCetakSuratBebasAktif) {
		this.digunakanSyaratCetakSuratBebasAktif = digunakanSyaratCetakSuratBebasAktif;
	}

	/**
	 * Teks penjelasan pembayaran (petunjuk transfer, nomor rekening, catatan) yang
	 * ditampilkan kepada mahasiswa pada halaman tagihan. Disimpan sebagai {@code text}
	 * sehingga tidak dibatasi panjang.
	 *
	 * <p>Getter murni: {@code null} ditampilkan sebagai string kosong tanpa menulis balik
	 * ke field.</p>
	 *
	 * @return penjelasan pembayaran; string kosong bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getPenjelasanPembayaran() {
		return penjelasanPembayaran == null ? "" : penjelasanPembayaran;
	}

	/**
	 * Setter teks penjelasan pembayaran.
	 *
	 * @param penjelasanPembayaran teks penjelasan
	 */
	public void setPenjelasanPembayaran(String penjelasanPembayaran) {
		this.penjelasanPembayaran = penjelasanPembayaran;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan untuk <b>semester mundur ke-3</b> &mdash;
	 * elemen indeks 3 pada array {@code aturanSmtMundur} yang disusun
	 * {@link #apakahBoleh(Mahasiswa, int, java.util.List)}.
	 *
	 * <p>Getter murni (ternary saja): {@code null} dibaca sebagai {@code false} tanpa
	 * menulis balik ke field.</p>
	 *
	 * @return {@code true} bila semester mundur ke-3 ikut diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi3() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi3 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi3;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-3.
	 *
	 * @param bayarHanyaSmtSaatIniDanSebelumnyalagi3 status aturan
	 */
	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi3(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi3) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi3 = bayarHanyaSmtSaatIniDanSebelumnyalagi3;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan untuk <b>semester mundur ke-4</b> &mdash;
	 * elemen indeks 4 pada array {@code aturanSmtMundur} yang disusun
	 * {@link #apakahBoleh(Mahasiswa, int, java.util.List)}.
	 *
	 * <p>Getter murni (ternary saja): {@code null} dibaca sebagai {@code false} tanpa
	 * menulis balik ke field.</p>
	 *
	 * @return {@code true} bila semester mundur ke-4 ikut diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi4() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi4 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi4;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-4.
	 *
	 * @param bayarHanyaSmtSaatIniDanSebelumnyalagi4 status aturan
	 */
	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi4(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi4) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi4 = bayarHanyaSmtSaatIniDanSebelumnyalagi4;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan untuk <b>semester mundur ke-5</b> &mdash;
	 * elemen indeks 5 pada array {@code aturanSmtMundur} yang disusun
	 * {@link #apakahBoleh(Mahasiswa, int, java.util.List)}.
	 *
	 * <p>Getter murni (ternary saja): {@code null} dibaca sebagai {@code false} tanpa
	 * menulis balik ke field.</p>
	 *
	 * @return {@code true} bila semester mundur ke-5 ikut diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi5() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi5 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi5;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-5.
	 *
	 * @param bayarHanyaSmtSaatIniDanSebelumnyalagi5 status aturan
	 */
	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi5(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi5) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi5 = bayarHanyaSmtSaatIniDanSebelumnyalagi5;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan untuk <b>semester mundur ke-6</b> &mdash;
	 * elemen indeks 6 pada array {@code aturanSmtMundur} yang disusun
	 * {@link #apakahBoleh(Mahasiswa, int, java.util.List)}.
	 *
	 * <p>Getter murni (ternary saja): {@code null} dibaca sebagai {@code false} tanpa
	 * menulis balik ke field.</p>
	 *
	 * @return {@code true} bila semester mundur ke-6 ikut diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi6() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi6 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi6;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-6.
	 *
	 * @param bayarHanyaSmtSaatIniDanSebelumnyalagi6 status aturan
	 */
	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi6(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi6) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi6 = bayarHanyaSmtSaatIniDanSebelumnyalagi6;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan untuk <b>semester mundur ke-7</b> &mdash;
	 * elemen indeks 7 pada array {@code aturanSmtMundur} yang disusun
	 * {@link #apakahBoleh(Mahasiswa, int, java.util.List)}.
	 *
	 * <p>Getter murni (ternary saja): {@code null} dibaca sebagai {@code false} tanpa
	 * menulis balik ke field.</p>
	 *
	 * @return {@code true} bila semester mundur ke-7 ikut diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi7() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi7 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi7;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-7.
	 *
	 * @param bayarHanyaSmtSaatIniDanSebelumnyalagi7 status aturan
	 */
	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi7(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi7) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi7 = bayarHanyaSmtSaatIniDanSebelumnyalagi7;
	}

	/**
	 * Aturan cakupan pemeriksaan tunggakan untuk <b>semester mundur ke-8</b> &mdash;
	 * elemen indeks 8 pada array {@code aturanSmtMundur} yang disusun
	 * {@link #apakahBoleh(Mahasiswa, int, java.util.List)}.
	 *
	 * <p>Getter murni (ternary saja): {@code null} dibaca sebagai {@code false} tanpa
	 * menulis balik ke field.</p>
	 *
	 * @return {@code true} bila semester mundur ke-8 ikut diperiksa; tidak pernah {@code null}
	 */
	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi8() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi8 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi8;
	}

	/**
	 * Setter aturan cakupan semester mundur ke-8.
	 *
	 * @param bayarHanyaSmtSaatIniDanSebelumnyalagi8 status aturan
	 */
	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi8(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi8) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi8 = bayarHanyaSmtSaatIniDanSebelumnyalagi8;
	}

	/**
	 * Ambang persentase pelunasan untuk tagihan <b>3 semester ke belakang</b>;
	 * jatuh kembali ke {@link #getPersenSyaratLogin()} bila belum diisi. Dipilih oleh
	 * {@code switch} pada {@link #bolehMasuk} ketika {@code smtMundur == 3}.
	 *
	 * @return ambang persentase untuk semester mundur ke-3; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin3() {
		return persenSyaratLogin3 == null ? getPersenSyaratLogin() : persenSyaratLogin3;
	}

	/**
	 * Setter ambang persentase untuk semester mundur ke-3.
	 *
	 * @param persenSyaratLogin3 ambang persentase; {@code null} berarti ikut ambang dasar
	 */
	public void setPersenSyaratLogin3(Double persenSyaratLogin3) {
		this.persenSyaratLogin3 = persenSyaratLogin3;
	}

	/**
	 * Ambang persentase pelunasan untuk tagihan <b>4 semester ke belakang</b>;
	 * jatuh kembali ke {@link #getPersenSyaratLogin()} bila belum diisi. Dipilih oleh
	 * {@code switch} pada {@link #bolehMasuk} ketika {@code smtMundur == 4}.
	 *
	 * @return ambang persentase untuk semester mundur ke-4; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin4() {
		return persenSyaratLogin4 == null ? getPersenSyaratLogin() : persenSyaratLogin4;
	}

	/**
	 * Setter ambang persentase untuk semester mundur ke-4.
	 *
	 * @param persenSyaratLogin4 ambang persentase; {@code null} berarti ikut ambang dasar
	 */
	public void setPersenSyaratLogin4(Double persenSyaratLogin4) {
		this.persenSyaratLogin4 = persenSyaratLogin4;
	}

	/**
	 * Ambang persentase pelunasan untuk tagihan <b>5 semester ke belakang</b>;
	 * jatuh kembali ke {@link #getPersenSyaratLogin()} bila belum diisi. Dipilih oleh
	 * {@code switch} pada {@link #bolehMasuk} ketika {@code smtMundur == 5}.
	 *
	 * @return ambang persentase untuk semester mundur ke-5; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin5() {
		return persenSyaratLogin5 == null ? getPersenSyaratLogin() : persenSyaratLogin5;
	}

	/**
	 * Setter ambang persentase untuk semester mundur ke-5.
	 *
	 * @param persenSyaratLogin5 ambang persentase; {@code null} berarti ikut ambang dasar
	 */
	public void setPersenSyaratLogin5(Double persenSyaratLogin5) {
		this.persenSyaratLogin5 = persenSyaratLogin5;
	}

	/**
	 * Ambang persentase pelunasan untuk tagihan <b>6 semester ke belakang</b>;
	 * jatuh kembali ke {@link #getPersenSyaratLogin()} bila belum diisi. Dipilih oleh
	 * {@code switch} pada {@link #bolehMasuk} ketika {@code smtMundur == 6}.
	 *
	 * @return ambang persentase untuk semester mundur ke-6; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin6() {
		return persenSyaratLogin6 == null ? getPersenSyaratLogin() : persenSyaratLogin6;
	}

	/**
	 * Setter ambang persentase untuk semester mundur ke-6.
	 *
	 * @param persenSyaratLogin6 ambang persentase; {@code null} berarti ikut ambang dasar
	 */
	public void setPersenSyaratLogin6(Double persenSyaratLogin6) {
		this.persenSyaratLogin6 = persenSyaratLogin6;
	}

	/**
	 * Ambang persentase pelunasan untuk tagihan <b>7 semester ke belakang</b>;
	 * jatuh kembali ke {@link #getPersenSyaratLogin()} bila belum diisi. Dipilih oleh
	 * {@code switch} pada {@link #bolehMasuk} ketika {@code smtMundur == 7}.
	 *
	 * @return ambang persentase untuk semester mundur ke-7; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin7() {
		return persenSyaratLogin7 == null ? getPersenSyaratLogin() : persenSyaratLogin7;
	}

	/**
	 * Setter ambang persentase untuk semester mundur ke-7.
	 *
	 * @param persenSyaratLogin7 ambang persentase; {@code null} berarti ikut ambang dasar
	 */
	public void setPersenSyaratLogin7(Double persenSyaratLogin7) {
		this.persenSyaratLogin7 = persenSyaratLogin7;
	}

	/**
	 * Ambang persentase pelunasan untuk tagihan <b>8 semester ke belakang</b>;
	 * jatuh kembali ke {@link #getPersenSyaratLogin()} bila belum diisi. Dipilih oleh
	 * {@code switch} pada {@link #bolehMasuk} ketika {@code smtMundur == 8}.
	 *
	 * @return ambang persentase untuk semester mundur ke-8; tidak pernah {@code null}
	 */
	public Double getPersenSyaratLogin8() {
		return persenSyaratLogin8 == null ? getPersenSyaratLogin() : persenSyaratLogin8;
	}

	/**
	 * Setter ambang persentase untuk semester mundur ke-8.
	 *
	 * @param persenSyaratLogin8 ambang persentase; {@code null} berarti ikut ambang dasar
	 */
	public void setPersenSyaratLogin8(Double persenSyaratLogin8) {
		this.persenSyaratLogin8 = persenSyaratLogin8;
	}

	/**
	 * <b>Saklar utama denda keterlambatan</b> pada tingkat jenis kegiatan; bawaan
	 * {@code false} (tidak ada denda).
	 *
	 * <p>Dibaca {@link DetailBiaya#checkDenda} sebagai penentu apakah konfigurasi denda
	 * jenis kegiatan (besaran, kelipatan, batas, dan varian per prodi) menggantikan
	 * konfigurasi denda tingkat {@link ItemBiaya}. Juga menjadi penjaga bagi
	 * {@link #getDendaDibuatPerProdi()} yang dipaksa {@code false} saat saklar ini mati.</p>
	 *
	 * <p><b>Perbedaan bawaan yang perlu diperhatikan.</b> Saklar ini berbawaan
	 * {@code false} sedangkan flag FORMAT {@link #getNilaiDendaDalamPersen()} berbawaan
	 * {@code true}. Keduanya sering muncul berdampingan di kode pemanggil, dan
	 * mempertukarkannya menghasilkan perilaku yang berlawanan secara diam-diam &mdash;
	 * lihat catatan pada {@link DetailBiaya#checkDendaCicilan}.</p>
	 *
	 * @return {@code true} bila jenis kegiatan ini mengenakan denda; tidak pernah {@code null}
	 */
	public Boolean getDendaJikaTerlambat() {
		return dendaJikaTerlambat == null ? false : dendaJikaTerlambat;
	}

	/**
	 * Setter saklar utama denda keterlambatan.
	 *
	 * @param dendaJikaTerlambat {@code true} untuk mengaktifkan denda
	 */
	public void setDendaJikaTerlambat(Boolean dendaJikaTerlambat) {
		this.dendaJikaTerlambat = dendaJikaTerlambat;
	}

	/**
	 * Besaran denda keterlambatan tingkat jenis kegiatan; {@code null} dibaca sebagai
	 * {@code 0.0}.
	 *
	 * <p>Satuannya ditentukan {@link #getNilaiDendaDalamPersen()}: persen dari nominal
	 * tagihan, atau rupiah tetap. Meskipun namanya mengandung kata
	 * &quot;prosentase&quot;, nilai ini juga dipakai sebagai nominal tetap ketika flag
	 * format dimatikan &mdash; penamaan yang menyesatkan pembaca kode.</p>
	 *
	 * <p>Nilai {@code 0.0} secara efektif mematikan denda: {@link DetailBiaya#checkDenda}
	 * mensyaratkan {@code nilaiDenda > 0.0} sebelum menghitung apa pun.</p>
	 *
	 * @return besaran denda; tidak pernah {@code null}
	 */
	public Double getDefaultProsentaseDenda() {
		return defaultProsentaseDenda == null ? 0.0 : defaultProsentaseDenda;
	}

	/**
	 * Setter besaran denda keterlambatan.
	 *
	 * @param defaultProsentaseDenda besaran denda (persen atau rupiah, sesuai flag format)
	 */
	public void setDefaultProsentaseDenda(Double defaultProsentaseDenda) {
		this.defaultProsentaseDenda = defaultProsentaseDenda;
	}

	/**
	 * <b>Format</b> denda keterlambatan: {@code true} berarti
	 * {@link #getDefaultProsentaseDenda()} adalah persen dari nominal tagihan,
	 * {@code false} berarti nominal rupiah tetap.
	 *
	 * <p><b>Bawaannya {@code true}</b> &mdash; berlawanan arah dengan saklar utama
	 * {@link #getDendaJikaTerlambat()} yang berbawaan {@code false}. Ini penting karena
	 * flag ini hanyalah penentu SATUAN, bukan penentu ADA/TIDAKNYA denda. Setiap kode
	 * pemanggil yang memakainya sebagai gerbang &quot;apakah jenis kegiatan ini mengenakan
	 * denda&quot; akan salah dalam dua arah sekaligus: menyalakan denda pada jenis kegiatan
	 * yang saklar utamanya mati (karena bawaan {@code true}), sekaligus mengabaikan denda
	 * pada jenis kegiatan yang dendanya berupa nominal tetap. Lihat
	 * {@link DetailBiaya#checkDendaCicilan} yang memakai pola tersebut.</p>
	 *
	 * @return {@code true} bila denda dinyatakan dalam persen; tidak pernah {@code null}
	 */
	public Boolean getNilaiDendaDalamPersen() {
		return nilaiDendaDalamPersen == null ? true : nilaiDendaDalamPersen;
	}

	/**
	 * Setter format denda (persen atau nominal tetap).
	 *
	 * @param nilaiDendaDalamPersen {@code true} untuk persen
	 */
	public void setNilaiDendaDalamPersen(Boolean nilaiDendaDalamPersen) {
		this.nilaiDendaDalamPersen = nilaiDendaDalamPersen;
	}

	/**
	 * Periode <b>pelipatan</b> denda dalam hari: denda dikalikan
	 * {@code jumlahHariTerlambat / nilai ini}. Nilai {@code 0} (juga bawaan saat kolom
	 * {@code NULL}) berarti denda tidak berlipat &mdash; dikenakan sekali saja.
	 *
	 * <p>Perhatikan bahwa perhitungan hilir di {@link DetailBiaya#checkDenda} memakai
	 * pembagian bilangan bulat atas {@code terlambathari} yang sudah dikurangi satu, dan
	 * hasil pembagian bernilai {@code 0} membuat denda menjadi <b>nol</b> &mdash; bukan
	 * satu kali denda. Jadi dengan pelipatan 7 hari, keterlambatan 1&ndash;7 hari tidak
	 * menghasilkan denda sama sekali.</p>
	 *
	 * @return periode pelipatan dalam hari; tidak pernah {@code null}
	 */
	public Integer getDendaAkanBerlipatTerlambaHari() {
		return dendaAkanBerlipatTerlambaHari == null ? 0 : dendaAkanBerlipatTerlambaHari;
	}

	/**
	 * Setter periode pelipatan denda.
	 *
	 * @param dendaAkanBerlipatTerlambaHari periode dalam hari; {@code 0} = tidak berlipat
	 */
	public void setDendaAkanBerlipatTerlambaHari(Integer dendaAkanBerlipatTerlambaHari) {
		this.dendaAkanBerlipatTerlambaHari = dendaAkanBerlipatTerlambaHari;
	}

	/**
	 * Batas atas jumlah kelipatan denda. Nilai {@code 0} (juga bawaan) berarti
	 * <b>tanpa batas</b> &mdash; denda terus berlipat selama keterlambatan berlanjut.
	 *
	 * <p>Perhatikan bahwa {@code 0} di sini bermakna &quot;tak terbatas&quot;, bukan
	 * &quot;maksimal nol kali&quot;; {@link DetailBiaya#checkDenda} menerapkan batas hanya
	 * bila {@code maksimal > 0}. Bawaan &quot;tanpa batas&quot; ini berarti jenis kegiatan
	 * yang mengaktifkan pelipatan tanpa mengisi batas dapat menghasilkan denda yang tumbuh
	 * tanpa plafon.</p>
	 *
	 * @return batas kelipatan; {@code 0} berarti tanpa batas; tidak pernah {@code null}
	 */
	public Integer getMaksimalBerlipatTerlambaHari() {
		return maksimalBerlipatTerlambaHari == null ? 0 : maksimalBerlipatTerlambaHari;
	}

	/**
	 * Setter batas kelipatan denda.
	 *
	 * @param maksimalBerlipatTerlambaHari batas kelipatan; {@code 0} = tanpa batas
	 */
	public void setMaksimalBerlipatTerlambaHari(Integer maksimalBerlipatTerlambaHari) {
		this.maksimalBerlipatTerlambaHari = maksimalBerlipatTerlambaHari;
	}

	/**
	 * Penanda bahwa tagihan jenis ini <b>harus</b> berupa angsuran/cicilan bulanan.
	 *
	 * <p>Dibaca dari dua tempat dengan makna berbeda: sebagai salah satu dari sepasang
	 * flag masukan {@link #modeAngsuranUntukJenjang(Jenjang, Integer, Integer)}, dan
	 * langsung oleh {@link Kegiatan#hitungTagihan()} sebagai penentu kunci JSON mana pada
	 * kolom {@code tagihans} yang boleh dijumlahkan (hanya kunci ber-garis-bawah, yaitu
	 * baris angsuran). Karena itu mengubah flag ini pada jenis kegiatan yang tagihannya
	 * sudah terbentuk akan <b>mengubah total tagihan</b> yang dihitung ulang, bukan hanya
	 * cara pembayarannya.</p>
	 *
	 * @return {@code true} bila wajib angsuran; tidak pernah {@code null}
	 */
	public Boolean getHanyaBerupaAngsuran() {
		return hanyaBerupaAngsuran == null ? false : hanyaBerupaAngsuran;
	}

	/**
	 * Setter penanda wajib angsuran. Perhatikan efeknya terhadap penjumlahan total pada
	 * {@link Kegiatan#hitungTagihan()}.
	 *
	 * @param hanyaBerupaAngsuran {@code true} bila wajib angsuran
	 */
	public void setHanyaBerupaAngsuran(Boolean hanyaBerupaAngsuran) {
		this.hanyaBerupaAngsuran = hanyaBerupaAngsuran;
	}

	public Double getPersenSyaratLogin9() {
		return persenSyaratLogin9 == null ? getPersenSyaratLogin() : persenSyaratLogin9;
	}

	public void setPersenSyaratLogin9(Double persenSyaratLogin9) {
		this.persenSyaratLogin9 = persenSyaratLogin9;
	}

	public Double getPersenSyaratLogin10() {
		return persenSyaratLogin10 == null ? getPersenSyaratLogin() : persenSyaratLogin10;
	}

	public void setPersenSyaratLogin10(Double persenSyaratLogin10) {
		this.persenSyaratLogin10 = persenSyaratLogin10;
	}

	public Double getPersenSyaratLogin11() {
		return persenSyaratLogin11 == null ? getPersenSyaratLogin() : persenSyaratLogin11;
	}

	public void setPersenSyaratLogin11(Double persenSyaratLogin11) {
		this.persenSyaratLogin11 = persenSyaratLogin11;
	}

	public Double getPersenSyaratLogin12() {
		return persenSyaratLogin12 == null ? getPersenSyaratLogin() : persenSyaratLogin12;
	}

	public void setPersenSyaratLogin12(Double persenSyaratLogin12) {
		this.persenSyaratLogin12 = persenSyaratLogin12;
	}

	public Double getPersenSyaratLogin13() {
		return persenSyaratLogin13 == null ? getPersenSyaratLogin() : persenSyaratLogin13;
	}

	public void setPersenSyaratLogin13(Double persenSyaratLogin13) {
		this.persenSyaratLogin13 = persenSyaratLogin13;
	}

	public Double getPersenSyaratLogin14() {
		return persenSyaratLogin14 == null ? getPersenSyaratLogin() : persenSyaratLogin14;
	}

	public void setPersenSyaratLogin14(Double persenSyaratLogin14) {
		this.persenSyaratLogin14 = persenSyaratLogin14;
	}

	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi9() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi9 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi9;
	}

	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi9(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi9) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi9 = bayarHanyaSmtSaatIniDanSebelumnyalagi9;
	}

	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi10() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi10 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi10;
	}

	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi10(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi10) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi10 = bayarHanyaSmtSaatIniDanSebelumnyalagi10;
	}

	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi11() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi11 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi11;
	}

	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi11(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi11) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi11 = bayarHanyaSmtSaatIniDanSebelumnyalagi11;
	}

	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi12() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi12 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi12;
	}

	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi12(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi12) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi12 = bayarHanyaSmtSaatIniDanSebelumnyalagi12;
	}

	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi13() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi13 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi13;
	}

	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi13(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi13) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi13 = bayarHanyaSmtSaatIniDanSebelumnyalagi13;
	}

	public Boolean getBayarHanyaSmtSaatIniDanSebelumnyalagi14() {
		return bayarHanyaSmtSaatIniDanSebelumnyalagi14 == null ? false : bayarHanyaSmtSaatIniDanSebelumnyalagi14;
	}

	public void setBayarHanyaSmtSaatIniDanSebelumnyalagi14(Boolean bayarHanyaSmtSaatIniDanSebelumnyalagi14) {
		this.bayarHanyaSmtSaatIniDanSebelumnyalagi14 = bayarHanyaSmtSaatIniDanSebelumnyalagi14;
	}

	public Boolean getUntukBayarSP() {
		return untukBayarSP == null ? false : untukBayarSP;
	}

	public void setUntukBayarSP(Boolean untukBayarSP) {
		this.untukBayarSP = untukBayarSP;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kanal_pembayaran", nullable = true)
	public KanalPembayaran getKanalPembayaran() {
		kanalPembayaran = check(kanalPembayaran);
		return kanalPembayaran;
	}

	public void setKanalPembayaran(KanalPembayaran kanalPembayaran) {
		this.kanalPembayaran = kanalPembayaran;
	}

	public Boolean getHanyaBerupaBukanAngsuran() {
		return hanyaBerupaBukanAngsuran == null ? false : hanyaBerupaBukanAngsuran;
	}

	public void setHanyaBerupaBukanAngsuran(Boolean hanyaBerupaBukanAngsuran) {
		this.hanyaBerupaBukanAngsuran = hanyaBerupaBukanAngsuran;
	}

	@javax.persistence.Column(name = "jenjang_angsuran_json")
	public String getJenjangAngsuranJson() {
		return jenjangAngsuranJson;
	}

	public void setJenjangAngsuranJson(String jenjangAngsuranJson) {
		this.jenjangAngsuranJson = jenjangAngsuranJson;
	}

	/**
	 * Periksa apakah jenjang tertentu masuk dalam array bertanda key ("harus" atau "bukan")
	 * di dalam jenjangAngsuranJson. Array kosong/null berarti berlaku untuk semua jenjang.
	 */
	private boolean jenjangDalamArray(String key, Jenjang jenjang) {
		if (jenjangAngsuranJson == null || jenjangAngsuranJson.trim().isEmpty()) return true;
		try {
			JSONArray arr = new JSONObject(jenjangAngsuranJson).optJSONArray(key);
			if (arr == null || arr.length() == 0) return true;
			if (jenjang == null || jenjang.getId() == null) return false;
			String jid = String.valueOf(jenjang.getId());
			for (int i = 0; i < arr.length(); i++) {
				try { if (jid.equals(arr.getString(i))) return true; } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/JenisKegiatan.java:970");}
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/JenisKegiatan.java:972");}
		return false;
	}

	/** Set true untuk mencetak log tiap panggilan modeAngsuranUntukJenjang. */
	public static boolean DEBUG_MODE_ANGSURAN = false;

	/**
	 * Tentukan mode angsuran untuk jenjang tertentu berdasarkan kombinasi flag:
	 * <ul>
	 *   <li>{@link Boolean#TRUE}  → jenjang ini HARUS pakai angsuran (cicilan)</li>
	 *   <li>{@link Boolean#FALSE} → jenjang ini BUKAN angsuran (reguler/sekaligus)</li>
	 *   <li>{@code null}          → tidak ada aturan khusus; ikuti logika default</li>
	 * </ul>
	 * Logika:
	 * <ol>
	 *   <li>Kedua flag aktif (harus &amp;&amp; bukan) → per-jenjang; "bukan" override "harus"</li>
	 *   <li>Hanya harus aktif → jenjang di array "harus" ⇒ TRUE, else null</li>
	 *   <li>Hanya bukan aktif → jenjang di array "bukan" ⇒ FALSE, else null</li>
	 * </ol>
	 */
	public Boolean modeAngsuranUntukJenjang(Jenjang jenjang) {
		return modeAngsuranUntukJenjang(jenjang, null);
	}

	/**
	 * Periksa apakah aturan angsuran kunci tertentu ("harus"/"bukan") berlaku pada
	 * SEMESTER (dan opsional TAHUN ANGKATAN) yang diminta, berdasarkan peta
	 * {@code <key>_smt} di jenjangAngsuranJson — isian field "Berlaku di smt" pada form
	 * Jenis Kegiatan. Format isian (dipisah koma, boleh dicampur):
	 * <ul>
	 *   <li>{@code 1,2,3,5,6} — daftar semester GLOBAL: berlaku di smt tersebut untuk
	 *       semua angkatan (contoh ini: TIDAK berlaku di smt 4).</li>
	 *   <li>{@code 2023:1} — khusus angkatan 2023 semester 1.</li>
	 *   <li>{@code 2023:1,2,3,4,5,8} — angka setelah token {@code TAHUN:SMT} tetap milik
	 *       angkatan tersebut sampai bertemu token ber-titik-dua berikutnya; contoh:
	 *       {@code 2023:1,2,8,2024:1} = angkatan 2023 smt 1,2,8 dan angkatan 2024 smt 1.</li>
	 *   <li>{@code 2023:} (tanpa smt) — angkatan 2023 di SEMUA semester.</li>
	 * </ul>
	 * Prioritas pencocokan: (1) bila tahun angkatan mahasiswa punya entri sendiri, pakai
	 * daftar smt angkatan itu (kosong = semua smt); (2) bila tidak, pakai daftar global;
	 * (3) bila hanya ada entri ber-angkatan dan angkatan mahasiswa tidak terdaftar,
	 * aturan dianggap TIDAK berlaku untuk mahasiswa itu. Kosong/tidak ada isian = berlaku
	 * di semua semester &amp; angkatan (kompatibel data lama). {@code semester == null} =
	 * tanpa konteks semester → cocok; {@code angkatan == null} = tanpa konteks angkatan →
	 * entri ber-angkatan diabaikan (jatuh ke daftar global / toleran).
	 */
	private boolean semesterCocokUntukJenjang(String key, Jenjang jenjang, Integer semester, Integer angkatan) {
		if (semester == null)
			return true;
		if (jenjangAngsuranJson == null || jenjangAngsuranJson.trim().isEmpty())
			return true;
		try {
			org.json.JSONObject peta = new org.json.JSONObject(jenjangAngsuranJson).optJSONObject(key + "_smt");
			if (peta == null)
				return true;
			String daftar = jenjang == null || jenjang.getId() == null ? null
					: peta.optString(String.valueOf(jenjang.getId()), null);
			if (daftar == null || daftar.trim().isEmpty())
				return true;

			java.util.List<Integer> smtGlobal = new java.util.ArrayList<Integer>();
			java.util.Map<Integer, java.util.List<Integer>> smtPerAngkatan =
					new java.util.HashMap<Integer, java.util.List<Integer>>();
			Integer cakupan = null; // null = global; selain itu = tahun angkatan aktif
			for (String token : daftar.split(",")) {
				String t = token.trim();
				if (t.isEmpty())
					continue;
				int posTitikDua = t.indexOf(':');
				if (posTitikDua >= 0) {
					String tahunStr = t.substring(0, posTitikDua).trim();
					String smtStr = t.substring(posTitikDua + 1).trim();
					try {
						cakupan = Integer.valueOf(tahunStr);
					} catch (Exception exTahun) {
						ais.common.ErrorAuditUtil.record(exTahun,
								"semesterCocokUntukJenjang: tahun angkatan tidak valid '" + tahunStr
										+ "' pada jenis kegiatan id=" + getId());
						cakupan = null;
						continue;
					}
					if (!smtPerAngkatan.containsKey(cakupan))
						smtPerAngkatan.put(cakupan, new java.util.ArrayList<Integer>());
					if (!smtStr.isEmpty()) {
						try {
							smtPerAngkatan.get(cakupan).add(Integer.valueOf(smtStr));
						} catch (Exception exSmt) {
							ais.common.ErrorAuditUtil.record(exSmt,
									"semesterCocokUntukJenjang: isian smt tidak valid '" + smtStr
											+ "' pada jenis kegiatan id=" + getId());
						}
					}
				} else {
					Integer nilai;
					try {
						nilai = Integer.valueOf(t);
					} catch (Exception exNum) {
						ais.common.ErrorAuditUtil.record(exNum, "semesterCocokUntukJenjang: isian smt tidak valid '"
								+ t + "' pada jenis kegiatan id=" + getId());
						continue;
					}
					if (cakupan == null)
						smtGlobal.add(nilai);
					else
						smtPerAngkatan.get(cakupan).add(nilai);
				}
			}

			if (angkatan != null && smtPerAngkatan.containsKey(angkatan)) {
				java.util.List<Integer> daftarSmt = smtPerAngkatan.get(angkatan);
				return daftarSmt.isEmpty() || daftarSmt.contains(semester);
			}
			if (!smtGlobal.isEmpty())
				return smtGlobal.contains(semester);
			if (!smtPerAngkatan.isEmpty())
				return angkatan == null; // tanpa konteks angkatan → toleran (perilaku lama)
			return false; // isian ada namun tak satu pun token valid
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "semesterCocokUntukJenjang gagal; jk id=" + getId());
			return true; // toleran: gagal parse → jangan mengubah perilaku lama
		}
	}

	/**
	 * Varian ber-SEMESTER dari {@link #modeAngsuranUntukJenjang(Jenjang)} — jawaban atas
	 * kenyataan bahwa konfigurasi billing dibuat per semester (contoh: S2 smt 1-3 ditagih
	 * bulanan, smt 4 sekali tagih). Aturan per-jenjang hanya dianggap MENGENAI mahasiswa
	 * bila jenjangnya masuk array kunci DAN semester berjalan masuk daftar "Berlaku di
	 * smt" kunci tersebut (kosong = semua semester). Semester di luar daftar menghasilkan
	 * {@code null} (tidak ada aturan) sehingga logika hilir jatuh ke penghitungan nyata
	 * dari database — bukan dipaksa angsuran atau dipaksa reguler.
	 *
	 * @param jenjang  jenjang mahasiswa/calon; {@code null} diperlakukan seperti versi lama
	 * @param semester semester berjalan; {@code null} = tanpa konteks semester (perilaku
	 *                 identik versi satu-argumen)
	 * @return {@link Boolean#TRUE} harus angsuran, {@link Boolean#FALSE} bukan angsuran,
	 *         {@code null} tidak ada aturan untuk kombinasi jenjang+semester ini
	 */
	public Boolean modeAngsuranUntukJenjang(Jenjang jenjang, Integer semester) {
		return modeAngsuranUntukJenjang(jenjang, semester, null);
	}

	/**
	 * Varian ber-SEMESTER dan ber-TAHUN-ANGKATAN — mendukung isian "Berlaku di smt"
	 * berformat campuran {@code 1,2,3} (global) dan {@code 2023:1,2,8,2024:1} (khusus
	 * angkatan; lihat {@link #semesterCocokUntukJenjang}). Aturan per-jenjang hanya
	 * dianggap mengenai mahasiswa bila jenjangnya masuk array kunci DAN kombinasi
	 * semester+angkatan-nya lolos daftar "Berlaku di smt". Kombinasi di luar daftar
	 * menghasilkan {@code null} (tidak ada aturan) sehingga logika hilir jatuh ke
	 * penghitungan nyata dari database.
	 *
	 * @param jenjang  jenjang mahasiswa/calon
	 * @param semester semester berjalan; {@code null} = tanpa konteks semester
	 * @param angkatan tahun angkatan mahasiswa/calon; {@code null} = tanpa konteks
	 *                 angkatan (entri ber-angkatan diabaikan)
	 * @return TRUE harus angsuran, FALSE bukan angsuran, {@code null} tidak ada aturan
	 */
	public Boolean modeAngsuranUntukJenjang(Jenjang jenjang, Integer semester, Integer angkatan) {
		boolean harus = getHanyaBerupaAngsuran();
		boolean bukan = getHanyaBerupaBukanAngsuran();

		String jkLabel = "[JK id=" + getId() + " nama=" + getNama() + "]";
		String jjLabel = "[Jenjang id=" + (jenjang != null ? jenjang.getId() : "null")
				+ " nama=" + (jenjang != null ? jenjang.getNama() : "null") + "]";
		String jsonLabel = "[json=" + getJenjangAngsuranJson() + "]";

		if (!harus && !bukan) {
			if (DEBUG_MODE_ANGSURAN) System.out.println("[DEBUG-ANGSURAN] " + jkLabel + " " + jjLabel
					+ " harus=false bukan=false → null (tidak ada aturan angsuran)");
			return null;
		}

		Boolean hasil;
		String branch;

		boolean kenaBukan = jenjangDalamArray("bukan", jenjang)
				&& semesterCocokUntukJenjang("bukan", jenjang, semester, angkatan);
		boolean kenaHarus = jenjangDalamArray("harus", jenjang)
				&& semesterCocokUntukJenjang("harus", jenjang, semester, angkatan);

		if (harus && bukan) {
			// BRANCH 1: kedua flag aktif → "bukan" override "harus"
			branch = "BRANCH-1(harus&&bukan)";
			if (bukan && kenaBukan) {
				hasil = Boolean.FALSE;
			} else if (kenaHarus) {
				hasil = Boolean.TRUE;
			} else {
				hasil = null;
			}
		} else if (harus) {
			// BRANCH 2: hanya harus aktif
			branch = "BRANCH-2(hanyaHarus)";
			hasil = kenaHarus ? Boolean.TRUE : null;
		} else {
			// BRANCH 3: hanya bukan aktif
			branch = "BRANCH-3(hanyaBukan)";
			hasil = kenaBukan ? Boolean.FALSE : null;
		}

		if (DEBUG_MODE_ANGSURAN) System.out.println("[DEBUG-ANGSURAN] " + jkLabel + " " + jjLabel
				+ " harus=" + harus + " bukan=" + bukan + " " + jsonLabel
				+ " → " + branch + " → hasil=" + hasil);
		return hasil;
	}

	public Boolean getAbaikanNilaiMinus() {
		return abaikanNilaiMinus == null ? false : abaikanNilaiMinus;
	}

	public void setAbaikanNilaiMinus(Boolean abaikanNilaiMinus) {
		this.abaikanNilaiMinus = abaikanNilaiMinus;
	}

	public Boolean getDendaDibuatPerProdi() {
		if (!getDendaJikaTerlambat()) {
			dendaDibuatPerProdi = false;
		}
		return dendaDibuatPerProdi == null ? false : dendaDibuatPerProdi;
	}

	public void setDendaDibuatPerProdi(Boolean dendaDibuatPerProdi) {
		this.dendaDibuatPerProdi = dendaDibuatPerProdi;
	}

	@Column(columnDefinition = "text")
	public String getDendaPerProdi() {
		return dendaPerProdi == null || dendaPerProdi.trim().isEmpty() ? new JSONObject().toString() : dendaPerProdi;
	}

	public void setDendaPerProdi(String dendaPerProdi) {
		this.dendaPerProdi = dendaPerProdi;
	}

	public String getPrefixKodePembayaran() {
		return prefixKodePembayaran == null || prefixKodePembayaran.trim().isEmpty() ? null
				: prefixKodePembayaran.trim();
	}

	public void setPrefixKodePembayaran(String prefixKodePembayaran) {
		this.prefixKodePembayaran = prefixKodePembayaran;
	}

	@Column(columnDefinition = "text")
	public String getNamaBankPembayaran() {
		if (namaBankPembayaran == null) {
			namaBankPembayaran = "";
		}

		namaBankPembayaran = (namaBankPembayaran == null || namaBankPembayaran.trim().equalsIgnoreCase(";") ? ""
				: ";" + namaBankPembayaran.trim() + ";").replaceAll(";;", ";").replaceAll(";;", ";")
				.replaceAll(";;", ";");

		if (namaBankPembayaran.equals(";")) {
			namaBankPembayaran = "";
		} else if (namaBankPembayaran.equals(";;")) {
			namaBankPembayaran = "";
		} else if (namaBankPembayaran.equals(";;;")) {
			namaBankPembayaran = "";
		} else if (namaBankPembayaran.equals(";;;;")) {
			namaBankPembayaran = "";
		}

		return namaBankPembayaran == null ? "" : namaBankPembayaran.trim();
	}

	public void setNamaBankPembayaran(String namaBankPembayaran) {
		this.namaBankPembayaran = namaBankPembayaran;
	}

}