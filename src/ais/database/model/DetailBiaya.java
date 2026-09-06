package ais.database.model;

// Generated Apr 18, 2010 11:30:58 PM by Hibernate Tools 3.2.4.CR1

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
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;

/**
 * Master <b>rincian komponen biaya</b> &mdash; satu baris menyimpan <i>berapa nominal</i>
 * sebuah {@link ItemBiaya} untuk satu kombinasi sasaran tertentu. Inilah tabel tempat
 * seluruh angka rupiah pada tagihan mahasiswa berasal.
 *
 * <h3>Posisi dalam mesin penagihan</h3>
 * <ul>
 *   <li>{@link JenisKegiatan} &mdash; katalog/aturan: jenis tagihan apa, semester berapa
 *       saja, boleh diangsur atau tidak, ada denda atau tidak. Tanpa nominal.</li>
 *   <li><b>{@code DetailBiaya}</b> (kelas ini) &mdash; master nominal.</li>
 *   <li>{@link Kegiatan} &mdash; header tagihan per mahasiswa per semester;
 *       {@link DetailKegiatan} baris rinciannya, yang menyalin nominal dari sini ke
 *       kolom {@code biaya} milik mahasiswa yang bersangkutan.</li>
 * </ul>
 *
 * <h3>MASTER BERSAMA: satu baris melayani banyak mahasiswa</h3>
 * <p>{@code DetailBiaya} <b>bukan</b> data per mahasiswa. Identitas logisnya adalah
 * kombinasi belasan dimensi penyaring &mdash; lihat {@link #key()} dan {@link #genKey}:
 * jurusan, item biaya, program, semester, tahun akademik, angkatan, semester mulai belajar,
 * status mahasiswa, status awal, paket, gelombang pendaftaran, jenis kegiatan, jenis
 * seleksi, kelas, jenis tinggal, ditambah tiga nilai tambahan bebas. Setiap mahasiswa yang
 * cocok dengan kombinasi itu <b>berbagi baris {@code DetailBiaya} yang sama</b>.</p>
 *
 * <p>Konsekuensinya penting untuk dipahami sebelum menyentuh kelas ini: apa pun yang
 * mengubah sebuah baris {@code DetailBiaya} berpotensi mengubah tagihan <b>seluruh</b>
 * mahasiswa yang termasuk kombinasi tersebut, bukan satu orang.</p>
 *
 * <h3>PERINGATAN: getter yang menulis balik ke kolom nominal</h3>
 * <p>Kelas ini memakai <i>property access</i> Hibernate (anotasi berada di getter),
 * sehingga Hibernate memanggil getter pada setiap {@code dirty check}/{@code flush}.
 * Beberapa getter di sini tidak sekadar membaca, melainkan <b>menghitung ulang lalu
 * menugaskan hasilnya ke field yang dipetakan ke kolom</b>. Yang paling berdampak:</p>
 * <ul>
 *   <li>{@link #getNilaiBiaya()} &mdash; menghitung ulang nominal dari
 *       {@link DetailSettingBiaya}/{@link SettingBiayaDetail} lalu menulisnya ke field
 *       {@code nilaiBiaya}, yang dipetakan ke kolom {@code nilai_biaya}. Ini yang
 *       dimaksud catatan pada {@link CicilanPembayaran#getKegiatan()} tentang
 *       &quot;master bersama yang bisa berubah/tersimpan lewat getter&quot;.</li>
 *   <li>{@link #getStatusMahasiswa()} dan {@link #getStatusAwalMahasiswa()} &mdash;
 *       menimpa/mengisi <b>foreign key</b>, yang berarti mengubah <i>kombinasi sasaran</i>
 *       baris ini, bukan sekadar nilainya.</li>
 *   <li>{@link #getJenjang()}, {@link #getNama()}, {@link #getKelamin()},
 *       {@link #getMulaiBelajarDiSemester()}, {@link #getTunggakanLalu()} &mdash;
 *       menulis balik ke property yang juga dipetakan ke kolom.</li>
 * </ul>
 * <p>Sebaliknya {@link #getKeterangan()}, {@link #getNilaiBiayaBaru()},
 * {@link #getDefaultTanggalTagihan()}, {@link #getDefaultTanggalDeadline()}, dan
 * {@link #getInfoDenda()} bertanda {@code @Transient} &mdash; mereka juga menulis balik ke
 * field, tetapi field-nya tidak dipetakan ke kolom sehingga tidak sampai ke database.
 * Rincian per method didokumentasikan pada masing-masing getter.</p>
 *
 * <p>Karena kelas ini {@code @Audited}, setiap penulisan tak sengaja seperti itu juga
 * menghasilkan revisi Envers &mdash; jejak audit yang mencatat &quot;perubahan&quot;
 * padahal tidak ada seorang pun yang mengubahnya.</p>
 *
 * <h3>Dua mesin total yang berbeda</h3>
 * <p>Perlu diketahui bahwa nominal akhir sebuah baris tagihan dapat dihitung lewat dua
 * jalur yang <b>tidak setara</b>:</p>
 * <ol>
 *   <li>{@link #hitungTotal(DetailKegiatan)} dan {@link #hitungTotalKegiatan(Kegiatan)}
 *       di kelas ini &mdash; ringkas, dipakai jalur virtual account/gateway bank.</li>
 *   <li>{@link Kegiatan#ambilJumlahTagihan(Kegiatan, DetailBiaya, boolean)} dan
 *       saudara-saudaranya &mdash; jauh lebih lengkap: menghormati nominal terkunci,
 *       penanda {@code bukanTagihan}, potongan/diskon, item berpenghitungan perkalian,
 *       skor parameter tambahan, dan pembalikan tanda untuk item pengurang.</li>
 * </ol>
 * <p>Lihat catatan pada {@link #hitungTotal(DetailKegiatan)} mengenai perbedaan hasil
 * yang dapat timbul di antara keduanya.</p>
 *
 * <p>DetailBiaya generated by hbm2java</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "detail_biaya")
public class DetailBiaya extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1930910520143688237L;
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
	 * Setter audit <b>satu arah</b>: masukan {@code null} atau kosong diabaikan diam-diam,
	 * sehingga nilai lama tidak pernah dapat dikosongkan kembali.
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
	 * <p>Karena beberapa getter kelas ini menulis balik ke kolom (lihat javadoc kelas),
	 * callback ini ikut berjalan pada &quot;update palsu&quot; yang dipicu semata-mata oleh
	 * pembacaan entity di dalam session yang terbuka.</p>
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
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi {@code id - itemBiaya-nilaiBiaya-detailSettingBiaya} untuk log dan
	 * komponen ZK.
	 *
	 * <p><b>Waspadai efek sampingnya.</b> Berbeda dari {@link JenisKegiatan#toString()} yang
	 * sengaja membaca field mentah, method ini memanggil {@link #getItemBiaya()} dan
	 * {@link #getDetailSettingBiaya()} &mdash; keduanya memanggil {@code check(...)} yang
	 * dapat memicu pemuatan relasi lazy. Pada entity yang sudah <i>detached</i> hal itu
	 * berpotensi melempar {@code LazyInitializationException} dari dalam {@code toString()},
	 * yang notabene sering dipanggil justru saat mencatat error. Nilai {@code nilaiBiaya}
	 * sendiri dibaca dari field mentah, sehingga tidak memicu perhitungan ulang destruktif
	 * {@link #getNilaiBiaya()}.</p>
	 *
	 * @return representasi ringkas rincian biaya
	 */
	public String toString() {
		itemBiaya = getItemBiaya();
		detailSettingBiaya = getDetailSettingBiaya();
		return id + " - " + itemBiaya + "-" + nilaiBiaya
				+ (detailSettingBiaya == null ? "" : "-" + detailSettingBiaya.toString());
	}

	private String tahunAkademik;
	private JenisKegiatan jenisKegiatan;
	private ItemBiaya itemBiaya;
	private String wnaAtauWni;
	private JenisSeleksi jenisSeleksi;
	private Paket paket;
	private GelombangPendaftaran gelombangPendaftaran;
	private String program;
	private Fakultas fakultas;
	private Jurusan jurusan;
	private Jenjang jenjang;
	private Integer semester;
	private Integer angkatan;
	private Double nilaiBiaya;
	private Double nilaiBiayaBaru;
	private Double tunggakanLalu;
	private String nama;
	private StatusMahasiswa statusMahasiswa;
	private Boolean merupakanPembayaran = false;
	private String bahasa;
	private String mulaiBelajarDiSemester = Perkuliahan.GANJIL;
	private StatusAwalMahasiswa statusAwalMahasiswa;
	private Integer bayarKe;
	private JenisTinggalMahasiswa jenisTinggalMahasiswa;
	private Kelas kelas;
	private DetailSettingBiaya detailSettingBiaya;
	private SettingBiayaDetail settingBiayaDetail;

	private String nilaiTambahan1;
	private String nilaiTambahan2;
	private String nilaiTambahan3;
	private SettingBiaya settingBiaya;
	private String keterangan;
	private Date defaultTanggalTagihan;
	private Date defaultTanggalDeadline;
	private Boolean aktif;

	/** Konstruktor kosong wajib bagi Hibernate/JPA dan bagi form CRUD generik. */
	public DetailBiaya() {
	}

	/**
	 * Konstruktor pintasan berisi <b>hanya</b> primary key &mdash; instance {@code TRANSIENT}
	 * yang seluruh field lainnya {@code null}.
	 *
	 * <p>Jangan memakainya sebagai nilai relasi yang akan disimpan; pakai
	 * {@link #muatRefAman(Session, Long)} yang justru dibuat untuk menggantikan pola ini.
	 * Lihat javadoc method tersebut mengenai pelanggaran foreign key yang pernah terjadi.</p>
	 *
	 * @param id primary key rincian biaya
	 */
	public DetailBiaya(Long id) {
		this.id = id;
	}

	/**
	 * Muat referensi DetailBiaya berdasarkan id secara NULL-SAFE untuk dipakai sebagai
	 * relasi FK pada entity yang akan disimpan (mis. CicilanPembayaran.detailBiaya).
	 *
	 * Mengganti pola lama "new DetailBiaya(id)" (instance TRANSIENT yang hanya berisi id):
	 * id hasil parse (mis. dari idPemBul / data gateway pembayaran) bisa TIDAK ADA di tabel
	 * detail_biaya sehingga INSERT melanggar foreign key constraint. Di sini di-load dari DB
	 * (lookup primary key murah, bisa kena 2nd-level cache &amp; cache session level-1 saat
	 * dipanggil berulang). Bila id null / session tidak ada / data tidak ditemukan -&gt; null
	 * (kolom detail_biaya nullable). Entity yang dikembalikan ter-asosiasi dengan session yang
	 * sama dengan yang akan menyimpan, sehingga aman dari "object referenced by another session".
	 */
	public static DetailBiaya muatRefAman(Session session, Long id) {
		if (id == null || session == null) {
			return null;
		}
		try {
			return (DetailBiaya) session.get(DetailBiaya.class, id);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Membangun <b>kunci identitas logis</b> baris ini &mdash; sidik jari kombinasi sasaran
	 * yang menentukan mahasiswa mana saja yang memperoleh nominal ini.
	 *
	 * <p>Dipakai untuk mengelompokkan/mencocokkan rincian biaya di memori (mis. saat form
	 * setting biaya menyandingkan konfigurasi lama dengan yang baru, atau saat mendeteksi
	 * duplikat) tanpa harus menempuh perbandingan belasan kolom satu per satu. Pekerjaan
	 * penyusunan string diserahkan ke {@link #genKey} yang statis.</p>
	 *
	 * <p><b>Memanggil sepuluh getter relasi lebih dulu.</b> Sebelum menyusun kunci, method
	 * ini menugaskan hasil {@link #getJurusan()}, {@link #getItemBiaya()},
	 * {@link #getStatusMahasiswa()}, {@link #getStatusAwalMahasiswa()}, {@link #getPaket()},
	 * {@link #getGelombangPendaftaran()}, {@link #getJenisKegiatan()},
	 * {@link #getJenisSeleksi()}, {@link #getKelas()}, dan
	 * {@link #getJenisTinggalMahasiswa()} ke field masing-masing. Sebagian besar hanya
	 * memulihkan proxy lewat {@code check(...)} sehingga tidak berbahaya, <b>tetapi dua di
	 * antaranya destruktif secara nyata</b>: {@link #getStatusMahasiswa()} menimpa FK dengan
	 * status milik {@link SettingBiaya} induk, dan {@link #getStatusAwalMahasiswa()} mengisi
	 * FK kosong dengan {@code ConstantValues.BARU}. Jadi sekadar meminta kunci sebuah
	 * {@code DetailBiaya} di dalam session terbuka dapat <i>mengubah kombinasi sasaran baris
	 * itu sendiri</i> &mdash; dan karenanya mengubah kunci yang baru saja dihitung.</p>
	 *
	 * <p>Perhatikan pula bahwa {@code program}, {@code semester}, {@code tahunAkademik},
	 * {@code angkatan}, {@code mulaiBelajarDiSemester}, dan ketiga {@code nilaiTambahan}
	 * dibaca sebagai <b>field mentah</b>, bukan lewat getter-nya. Untuk
	 * {@code mulaiBelajarDiSemester} hal ini berarti kunci dapat memuat {@code null},
	 * sedangkan {@link #getMulaiBelajarDiSemester()} akan mengembalikan
	 * {@link Perkuliahan#GANJIL} &mdash; dua baris yang secara efektif identik dapat
	 * menghasilkan kunci berbeda bila salah satunya kolomnya masih {@code NULL}.</p>
	 *
	 * @return string kunci identitas logis baris ini
	 */
	public String key() {

		jurusan = getJurusan();
		itemBiaya = getItemBiaya();
		statusMahasiswa = getStatusMahasiswa();
		statusAwalMahasiswa = getStatusAwalMahasiswa();
		paket = getPaket();
		gelombangPendaftaran = getGelombangPendaftaran();
		jenisKegiatan = getJenisKegiatan();
		jenisSeleksi = getJenisSeleksi();
		kelas = getKelas();
		jenisTinggalMahasiswa = getJenisTinggalMahasiswa();

		return DetailBiaya.genKey(jurusan, itemBiaya, program, semester, tahunAkademik, angkatan,
				mulaiBelajarDiSemester, statusMahasiswa, statusAwalMahasiswa, paket, gelombangPendaftaran,
				jenisKegiatan, jenisSeleksi, kelas, jenisTinggalMahasiswa, nilaiTambahan1, nilaiTambahan2,
				nilaiTambahan3);
	}

	/**
	 * Menyusun string kunci identitas logis dari komponen-komponennya. Versi statis dari
	 * {@link #key()} sehingga dapat dipanggil untuk kombinasi hipotetis yang belum berwujud
	 * entity &mdash; mis. saat memeriksa apakah konfigurasi baru akan berbenturan dengan
	 * baris yang sudah ada.
	 *
	 * <p>Setiap relasi diwakili id-nya, dengan {@code 0L} sebagai pengganti bila relasi atau
	 * id-nya {@code null}, sehingga &quot;tidak dibatasi&quot; punya representasi yang stabil.
	 * Komponen dirangkai dengan pemisah tanda hubung.</p>
	 *
	 * <p><b>Parameter {@code jenisSeleksi} diterima tetapi TIDAK PERNAH DIPAKAI.</b> Periksa
	 * badan method: string kunci dirangkai dari jurusan, item biaya, program, semester, tahun
	 * ajaran, angkatan, semester mulai belajar, status mahasiswa, status awal, paket,
	 * gelombang pendaftaran, jenis kegiatan, kelas, jenis tinggal, dan ketiga nilai tambahan
	 * &mdash; jenis seleksi tidak muncul sama sekali. Akibatnya dua rincian biaya yang
	 * identik dalam segala hal <i>kecuali</i> jenis seleksinya menghasilkan kunci yang
	 * <b>sama persis</b>, dan setiap pemakaian kunci ini untuk pengelompokan atau deteksi
	 * duplikat akan menganggap keduanya satu baris. Perlu diketahui bahwa
	 * {@code NewDetailBiayaExcelAction} memakai kunci ini justru untuk mencocokkan baris
	 * pada impor/ekspor Excel rincian biaya, sehingga skenario &quot;biaya berbeda untuk
	 * jalur seleksi berbeda&quot; berisiko saling menimpa atau terlewat di jalur itu.
	 * Perhatikan pula bahwa {@code jenisSeleksi} <i>ikut</i> diperhitungkan sebagai penyaring
	 * di tempat lain, mis. pada rute diskon {@link Kegiatan#hitungDiskon}, sehingga
	 * ketidakhadirannya di sini adalah ketidakseragaman, bukan keputusan desain yang
	 * konsisten.</p>
	 *
	 * <p>Pemisah tanda hubung juga dipakai polos tanpa peng-escape-an, sehingga nilai
	 * tambahan yang memuat tanda hubung dapat menggeser batas antar komponen dan membuat dua
	 * kombinasi berbeda menghasilkan kunci yang sama.</p>
	 *
	 * @param jurusan               program studi sasaran; {@code null} = tidak dibatasi
	 * @param itemBiaya             item biaya yang dinominalkan
	 * @param program               program (reguler/karyawan/dsb.)
	 * @param semester              semester sasaran
	 * @param tahunAjaran           tahun akademik sasaran
	 * @param angkatan              tahun angkatan sasaran
	 * @param mulaiBelajar          semester mulai belajar (ganjil/genap)
	 * @param statusMahasiswa       status mahasiswa sasaran
	 * @param statusAwalMahasiswa   status awal mahasiswa sasaran
	 * @param paket                 paket biaya sasaran
	 * @param gelombangPendaftaran  gelombang pendaftaran sasaran
	 * @param jenisKegiatan         jenis kegiatan sasaran
	 * @param jenisSeleksi          jenis seleksi sasaran &mdash; <b>tidak ikut membentuk kunci</b>
	 * @param kelas                 kelas sasaran
	 * @param jenisTinggalMahasiswa jenis tinggal mahasiswa sasaran
	 * @param nilaiTambahan1        nilai penyaring tambahan ke-1
	 * @param nilaiTambahan2        nilai penyaring tambahan ke-2
	 * @param nilaiTambahan3        nilai penyaring tambahan ke-3
	 * @return string kunci identitas logis
	 */
	public static String genKey(Jurusan jurusan, ItemBiaya itemBiaya, String program, Integer semester,
			String tahunAjaran, Integer angkatan, String mulaiBelajar, StatusMahasiswa statusMahasiswa,
			StatusAwalMahasiswa statusAwalMahasiswa, Paket paket, GelombangPendaftaran gelombangPendaftaran,
			JenisKegiatan jenisKegiatan, JenisSeleksi jenisSeleksi, Kelas kelas,
			JenisTinggalMahasiswa jenisTinggalMahasiswa, String nilaiTambahan1, String nilaiTambahan2,
			String nilaiTambahan3) {
		String key = (jurusan == null || jurusan.getId() == null ? 0L : jurusan.getId()) + "-" + (itemBiaya == null || itemBiaya.getId() == null ? 0L : itemBiaya.getId()) + "-"
				+ program + "-" + semester + "-" + tahunAjaran + "-" + angkatan + "-" + mulaiBelajar + "-"
				+ (statusMahasiswa == null || statusMahasiswa.getId() == null ? 0L : statusMahasiswa.getId()) + "-"
				+ (statusAwalMahasiswa == null || statusAwalMahasiswa.getId() == null ? 0L : statusAwalMahasiswa.getId()) + "-"
				+ (paket == null || paket.getId() == null ? 0L : paket.getId()) + "-"
				+ (gelombangPendaftaran == null || gelombangPendaftaran.getId() == null ? 0L : gelombangPendaftaran.getId()) + "-"
				+ (jenisKegiatan == null || jenisKegiatan.getId() == null ? 0L : jenisKegiatan.getId()) + "-" + (kelas == null || kelas.getId() == null ? 0L : kelas.getId())
				+ "-" + (jenisTinggalMahasiswa == null || jenisTinggalMahasiswa.getId() == null ? 0L : jenisTinggalMahasiswa.getId()) + "-"
				+ (nilaiTambahan1 == null ? "" : nilaiTambahan1) + "-" + (nilaiTambahan2 == null ? "" : nilaiTambahan2)
				+ "-" + (nilaiTambahan3 == null ? "" : nilaiTambahan3);
		return key;
	}

	/**
	 * Konstruktor pintasan berisi hanya tahun akademik; dipakai sebagai wadah sementara saat
	 * menyiapkan rincian biaya untuk satu tahun akademik tertentu.
	 *
	 * @param tahunAkademik tahun akademik sasaran
	 */
	public DetailBiaya(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Menghitung nominal baris biaya ini menurut mesin total <b>ringkas</b> milik kelas ini.
	 *
	 * <h4>Rumusnya</h4>
	 * <p>Tiga sumber nilai dicoba berurutan:</p>
	 * <ol>
	 *   <li>Bila {@link #getTunggakanLalu()} melebihi {@code 0.01}, nilai itulah yang dipakai
	 *       &mdash; baris ini sedang mewakili tunggakan semester lalu, dan nominal masternya
	 *       tidak relevan.</li>
	 *   <li>Bila {@code detailKegiatan} diberikan, dipakai {@link DetailKegiatan#getBiaya()}
	 *       &mdash; yaitu nominal yang sudah <i>dibekukan</i> untuk mahasiswa tertentu.</li>
	 *   <li>Selain itu dipakai {@link #getNilaiBiayaBaru()} bila ada, jika tidak
	 *       {@link #getNilaiBiaya()} &mdash; nominal master bersama.</li>
	 * </ol>
	 *
	 * <h4>PERBEDAAN PENTING DENGAN MESIN TOTAL YANG LAIN</h4>
	 * <p>Hasil method ini <b>tidak setara</b> dengan
	 * {@link Kegiatan#ambilJumlahTagihan(Kegiatan, DetailBiaya, boolean)}, yang merupakan
	 * mesin total lengkap dan dipakai jalur tampilan tagihan mahasiswa. Method di sini
	 * <b>melewatkan seluruh</b> hal berikut:</p>
	 * <ul>
	 *   <li><b>Potongan/diskon.</b> Tidak ada pengurangan
	 *       {@code jumlah - hitungDiskon(...)}. Nominal yang dikembalikan adalah nilai BRUTO,
	 *       sedangkan mesin lengkap mengembalikan nilai NETO.</li>
	 *   <li><b>Nominal terkunci.</b> Tidak berkonsultasi dengan
	 *       {@link Kegiatan#ambilNominalTagihanTerkunci}, sehingga koreksi manual bernomor
	 *       yang sudah disetujui petugas diabaikan.</li>
	 *   <li><b>Penanda {@code bukanTagihan}.</b> Tidak memeriksa
	 *       {@link DetailKegiatan#getBukanTagihan()}, sehingga baris yang seharusnya
	 *       bernilai nol tetap menyumbang nominal.</li>
	 *   <li><b>Item berpenghitungan perkalian.</b> Tidak memicu
	 *       {@link #updateKeterangan(Mahasiswa, Integer)}, sehingga untuk item yang bernilai
	 *       &quot;(50.000) x N SKS&quot; dan {@code nilaiBiayaBaru}-nya belum terhitung, yang
	 *       terpakai adalah nilai <b>per unit</b> &mdash; angka yang di layar justru dicoret.</li>
	 *   <li><b>Pembalikan tanda.</b> Tidak menerapkan
	 *       {@code ItemBiaya.DIKALI_NILAI_MINUS}, sehingga item pengurang dikembalikan
	 *       sebagai bilangan positif.</li>
	 * </ul>
	 *
	 * <p>Perbedaan ini bermakna karena {@link #hitungTotalKegiatan(Kegiatan, Session)}
	 * &mdash; yang bermuara ke sini &mdash; dipakai luas oleh servlet gateway bank dan
	 * pembangkit virtual account untuk menentukan <b>nominal yang harus dibayar</b>. Nominal
	 * di lembar tagihan (mesin lengkap, neto) dengan nominal pada virtual account (mesin ini,
	 * bruto) karenanya dapat berbeda untuk baris yang sama. Sebagian selisih itu tertutup
	 * karena pemanggil di jalur bank umumnya mengoper {@link DetailKegiatan} yang
	 * {@code biaya}-nya sudah berisi hasil perhitungan mesin lengkap, sehingga cabang (2) di
	 * atas yang terpakai; selisih baru muncul ketika {@link DetailKegiatan} belum terbentuk
	 * atau belum dihitung ulang, dan pemanggilan jatuh ke cabang (3).</p>
	 *
	 * @param detailKegiatan baris rincian milik mahasiswa yang nominalnya sudah dibekukan;
	 *                       boleh {@code null} untuk memakai nominal master
	 * @return nominal baris ini (bruto, tanpa potongan)
	 */
	public Double hitungTotal(DetailKegiatan detailKegiatan) {
		Double total = getTunggakanLalu() > 0.01 ? getTunggakanLalu()
				: (detailKegiatan == null ? (getNilaiBiayaBaru() == null ? getNilaiBiaya() : getNilaiBiayaBaru())
						: detailKegiatan.getBiaya());
		return total;
	}

	/**
	 * Nominal baris ini menurut nominal <b>master</b> semata, tanpa konteks mahasiswa mana pun.
	 * Pintasan untuk {@code hitungTotal(null)}.
	 *
	 * <p>Karena tidak ada {@link DetailKegiatan}, hasilnya selalu bruto dan selalu mengikuti
	 * nilai master terkini &mdash; termasuk seluruh keterbatasan yang diuraikan pada
	 * {@link #hitungTotal(DetailKegiatan)}. Perlu diingat pula bahwa jalur ini memanggil
	 * {@link #getNilaiBiaya()} yang destruktif.</p>
	 *
	 * @return nominal master baris ini (bruto)
	 */
	public Double hitungTotal() {
		return hitungTotal(null);
	}

	/**
	 * Nominal baris ini untuk sebuah {@link Kegiatan} tertentu, dengan session diambil
	 * sendiri dari {@link HibernateUtil#currentNativeSession()}.
	 *
	 * <p>Bila {@code kegiatan} {@code null} atau belum tersimpan, langsung jatuh ke
	 * {@link #hitungTotal()} (nominal master). Kegagalan apa pun ditangani dengan
	 * {@code Common.tampilErrorJikaAdmin(e)} lalu tetap mengembalikan
	 * {@link #hitungTotal()} &mdash; sikap <i>fail-soft</i>: gangguan basis data
	 * menghasilkan nominal master, bukan pembatalan transaksi. Untuk jalur pembayaran hal
	 * ini berarti nominal yang ditagihkan dapat menyimpang dari nominal khusus mahasiswa
	 * tanpa pesan galat yang terlihat pengguna biasa.</p>
	 *
	 * <p>Session yang diambil dilepas kembali lewat
	 * {@code KegiatanPersistenceHelper.closeOpenedSession(session)} di {@code finally};
	 * helper itulah yang memutuskan apakah session tersebut memang milik method ini dan
	 * karenanya boleh ditutup.</p>
	 *
	 * @param kegiatan header tagihan sebagai konteks; boleh {@code null}
	 * @return nominal baris ini untuk kegiatan tersebut
	 */
	public Double hitungTotalKegiatan(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return hitungTotal();
		}

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return hitungTotalKegiatan(kegiatan, session);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return hitungTotal();
		} finally {
			ais.action.master.helper.KegiatanPersistenceHelper.closeOpenedSession(session);
		}
	}

	/**
	 * Nominal baris ini untuk sebuah {@link Kegiatan} dengan session yang dioper pemanggil
	 * &mdash; varian yang dipakai servlet gateway bank agar seluruh pembacaan berada pada
	 * satu session/transaksi yang sama.
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>{@code kegiatan} kosong/belum tersimpan &rarr; {@link #hitungTotal()}.</li>
	 *   <li>Session tidak siap (lihat {@link #isSessionSiapDipakai(Session)}) &rarr;
	 *       dilimpahkan ke {@link #hitungTotalKegiatan(Kegiatan)} yang membuka sessionnya
	 *       sendiri.</li>
	 *   <li>Cari satu {@link DetailKegiatan} yang menautkan baris biaya ini dengan kegiatan
	 *       tersebut, lalu serahkan ke {@link #hitungTotal(DetailKegiatan)}.</li>
	 * </ol>
	 *
	 * <p><b>Pencarian memakai {@code setMaxResults(1)} atas kriteria tanpa pengurutan.</b>
	 * Pilihan ini menghindari {@code NonUniqueResultException} ketika proses hitung ulang
	 * meninggalkan lebih dari satu {@link DetailKegiatan} untuk pasangan yang sama &mdash;
	 * masalah yang memang terdokumentasi pada {@link Kegiatan#ambilByKodeUnik(String, Session)}.
	 * Bedanya, di sana pemilihannya dibuat deterministik dengan {@code order by id desc}
	 * (mengambil hasil hitung ulang terbaru), sedangkan di sini tanpa {@code addOrder}
	 * sehingga baris mana yang terambil bergantung pada urutan yang kebetulan dikembalikan
	 * basis data. Bila duplikat itu berbeda nominal, nominal yang dipakai jalur pembayaran
	 * dapat berubah-ubah antar pemanggilan.</p>
	 *
	 * <p>Kriteria disusun atas {@code detailBiaya.id} bila entity ini sudah tersimpan, dan
	 * atas objeknya sendiri bila belum &mdash; menghindari perbandingan terhadap {@code id}
	 * yang masih {@code null}. Kegagalan query ditangani <i>fail-soft</i> menjadi
	 * {@link #hitungTotal()}.</p>
	 *
	 * @param kegiatan header tagihan sebagai konteks; boleh {@code null}
	 * @param session  session Hibernate milik pemanggil; boleh {@code null}/tertutup
	 * @return nominal baris ini untuk kegiatan tersebut
	 */
	public Double hitungTotalKegiatan(Kegiatan kegiatan, Session session) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return hitungTotal();
		}

		if (!isSessionSiapDipakai(session)) {
			return hitungTotalKegiatan(kegiatan);
		}

		DetailKegiatan detailKegiatan = null;
		try {
			org.hibernate.Criteria criteria = session.createCriteria(DetailKegiatan.class);
			if (getId() != null) {
				criteria.add(Restrictions.eq("detailBiaya.id", getId()));
			} else {
				criteria.add(Restrictions.eq("detailBiaya", this));
			}
			criteria.add(Restrictions.eq("kegiatan.id", kegiatan.getId()));
			detailKegiatan = (DetailKegiatan) criteria.setMaxResults(1).uniqueResult();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return hitungTotal();
		}

		return hitungTotal(detailKegiatan);
	}

	/**
	 * Memeriksa apakah sebuah session masih layak dipakai untuk query.
	 *
	 * <p>Pemeriksaan {@code session.isOpen()} sendiri dapat melempar pada proxy yang sudah
	 * terputus, sehingga seluruhnya dibungkus {@code try/catch} yang mengembalikan
	 * {@code false}. Sikapnya <i>fail-closed</i> dan tepat: bila keadaan session tidak dapat
	 * dipastikan, ia diperlakukan sebagai tidak siap dan pemanggil membuka session sendiri
	 * &mdash; lebih baik daripada melempar &quot;Session is closed!&quot; di tengah
	 * perhitungan tagihan.
	 *
	 * @param session session yang diperiksa; boleh {@code null}
	 * @return {@code true} bila session tidak {@code null} dan masih terbuka
	 */
	private boolean isSessionSiapDipakai(Session session) {
		try {
			return session != null && session.isOpen();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Primary key {@code detail_biaya.id}, dihasilkan database ({@code IDENTITY}) sehingga
	 * {@code insertable = false}.
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
	 * @param id primary key rincian biaya
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Tahun akademik sasaran (mis. {@code "2025/2026"}); salah satu dimensi penyaring yang
	 * menentukan mahasiswa mana yang memperoleh nominal ini. {@code null} berarti tidak
	 * dibatasi tahun akademik tertentu.
	 *
	 * <p>Getter murni &mdash; mengembalikan field apa adanya tanpa menulis balik.</p>
	 *
	 * @return tahun akademik sasaran; {@code null} bila tidak dibatasi
	 */
	@Column(name = "tahun_akademik", nullable = true, length = 20)
	public String getTahunAkademik() {
		return this.tahunAkademik;
	}

	/**
	 * Setter tahun akademik sasaran.
	 *
	 * @param tahunAkademik tahun akademik; {@code null} berarti tidak dibatasi
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * <b>Item biaya</b> yang dinominalkan baris ini &mdash; relasi terpenting kelas ini,
	 * karena {@link ItemBiaya} yang menentukan nama komponen biaya, cara penghitungannya
	 * (tetap, dikali SKS/matakuliah, tunggakan semester lalu, atau pengurang bertanda
	 * negatif), serta konfigurasi dendanya sendiri.
	 *
	 * <p>Hampir seluruh mesin penagihan mensyaratkan relasi ini terisi: baik
	 * {@link Kegiatan#ambilJumlahTagihan(Kegiatan, DetailBiaya, boolean)} maupun
	 * {@link Kegiatan#ambilSatuDetailKegiatan(DetailBiaya, boolean, Session)} langsung
	 * mengembalikan nol/{@code null} bila {@code getItemBiaya()} kosong.</p>
	 *
	 * <p>Getter relasi lazy standar: {@code check(...)} memulihkan proxy yang mungkin sudah
	 * terputus. Ia menulis balik ke field, tetapi hanya mengganti proxy dengan objek setara
	 * sehingga nilai foreign key tidak berubah &mdash; bukan getter destruktif dalam arti
	 * yang dimaksud pada javadoc kelas.</p>
	 *
	 * @return item biaya; {@code null} bila baris belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		itemBiaya = check(itemBiaya);
		return this.itemBiaya;
	}

	/**
	 * Setter item biaya.
	 *
	 * @param itemBiaya item biaya yang dinominalkan baris ini
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Penyaring kewarganegaraan sasaran (WNA atau WNI) &mdash; memungkinkan nominal berbeda
	 * bagi mahasiswa asing. {@code null} berarti tidak dibatasi.
	 *
	 * <p>Getter murni. Perhatikan bahwa dimensi ini <b>tidak</b> ikut membentuk
	 * {@link #key()}, sehingga dua baris yang hanya berbeda kewarganegaraan sasaran akan
	 * berkunci sama &mdash; keterbatasan yang sama dengan {@code jenisSeleksi}; lihat
	 * {@link #genKey}.</p>
	 *
	 * @return penyaring kewarganegaraan; {@code null} bila tidak dibatasi
	 */
	@Column(name = "wna_atau_wni", length = 20)
	public String getWnaAtauWni() {
		return this.wnaAtauWni;
	}

	/**
	 * Setter penyaring kewarganegaraan.
	 *
	 * @param wnaAtauWni penyaring kewarganegaraan; {@code null} berarti tidak dibatasi
	 */
	public void setWnaAtauWni(String wnaAtauWni) {
		this.wnaAtauWni = wnaAtauWni;
	}

	/**
	 * Penyaring jenis seleksi sasaran (jalur masuk mahasiswa). {@code null} berarti tidak
	 * dibatasi jalur tertentu.
	 *
	 * <p>Getter relasi lazy standar dengan {@code check(...)}. Perhatikan bahwa dimensi ini
	 * <b>tidak ikut membentuk kunci</b> pada {@link #genKey} walaupun diterima sebagai
	 * parameter di sana &mdash; lihat catatan pada method tersebut.</p>
	 *
	 * @return jenis seleksi sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_seleksi", nullable = true)
	public JenisSeleksi getJenisSeleksi() {
		jenisSeleksi = check(jenisSeleksi);
		return this.jenisSeleksi;
	}

	/**
	 * Setter penyaring jenis seleksi.
	 *
	 * @param jenisSeleksi jenis seleksi sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setJenisSeleksi(JenisSeleksi jenisSeleksi) {
		this.jenisSeleksi = jenisSeleksi;
	}

	/**
	 * Penyaring program sasaran (mis. reguler, karyawan, internasional). {@code null} berarti
	 * tidak dibatasi. Getter murni.
	 *
	 * @return program sasaran; {@code null} bila tidak dibatasi
	 */
	@Column(name = "program")
	public String getProgram() {
		return this.program;
	}

	/**
	 * Setter penyaring program.
	 *
	 * @param program program sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Setter penyaring program studi. Perhatikan bahwa mengubah nilai ini juga mengubah
	 * jenjang yang dilaporkan {@link #getJenjang()}, karena getter itu menurunkan jenjang
	 * dari program studi.
	 *
	 * @param jurusan program studi sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Penyaring <b>program studi</b> sasaran. {@code null} berarti nominal berlaku untuk
	 * seluruh program studi.
	 *
	 * <p>Dimensi yang paling banyak dikonsultasikan: selain menjadi bagian {@link #key()},
	 * ia menentukan nominal per-prodi lewat
	 * {@link DetailSettingBiaya#ambilDefaultBiaya(Jurusan)} pada {@link #getNilaiBiaya()},
	 * keterangan per-prodi pada {@link #getKeterangan()}, tanggal tagihan/deadline per-prodi
	 * pada {@link #getDefaultTanggalTagihan()} dan {@link #getDefaultTanggalDeadline()},
	 * serta besaran denda per-prodi pada {@link #checkDenda} dan
	 * {@link #checkDendaCicilan}.</p>
	 *
	 * <p>Getter relasi lazy standar dengan {@code check(...)}.</p>
	 *
	 * @return program studi sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Setter penyaring fakultas.
	 *
	 * @param fakultas fakultas sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Penyaring fakultas sasaran. {@code null} berarti tidak dibatasi.
	 *
	 * <p>Getter relasi lazy standar. Seperti {@code wnaAtauWni} dan {@code jenisSeleksi},
	 * dimensi ini <b>tidak ikut membentuk</b> {@link #key()} &mdash; penyaringan berbasis
	 * fakultas pada praktiknya sudah tercakup oleh {@link #getJurusan()}, karena setiap
	 * program studi bernaung di bawah satu fakultas.</p>
	 *
	 * @return fakultas sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Jenjang sasaran (S1, S2, D3, dan seterusnya).
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; menimpa FOREIGN KEY.</b> Bila {@link #getJurusan()}
	 * terisi, field {@code jenjang} <b>ditimpa</b> dengan {@code jurusan.getJenjang()} tanpa
	 * syarat, lalu dikembalikan. Karena property ini dipetakan ke kolom
	 * {@code detail_biaya.jenjang}, sekadar membaca entity di dalam session terbuka akan
	 * menuliskan foreign key hasil turunan itu ke database, beserta satu revisi Envers.</p>
	 *
	 * <p>Secara semantik penurunan itu masuk akal &mdash; jenjang memang melekat pada program
	 * studi, dan menyimpannya membuat penyaringan berbasis jenjang dapat dilakukan dengan
	 * satu kolom tanpa {@code JOIN}. Namun konsekuensinya kolom {@code jenjang} <b>bukan
	 * data mandiri</b>: nilai apa pun yang diisi operator lewat {@link #setJenjang(Jenjang)}
	 * akan hilang pada pembacaan berikutnya selama {@code jurusan} terisi, dan mengubah
	 * jenjang sebuah {@link Jurusan} akan merambat ke seluruh baris {@code DetailBiaya}
	 * yang menunjuk program studi itu. Kemandirian kolom ini hanya berlaku ketika
	 * {@code jurusan} kosong (nominal berlaku lintas prodi), dan pada kasus itu
	 * {@code check(...)} sekadar memulihkan proxy.</p>
	 *
	 * @return jenjang sasaran; {@code null} bila tidak dapat diturunkan maupun diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		jurusan = getJurusan();
		if (jurusan != null) {
			jenjang = jurusan.getJenjang();
		}
		jenjang = check(jenjang);
		return this.jenjang;
	}

	/**
	 * Setter jenjang sasaran. Perhatikan bahwa nilai yang diisi di sini akan <b>ditimpa</b>
	 * oleh {@link #getJenjang()} pada pembacaan berikutnya bila {@link #getJurusan()} terisi.
	 *
	 * @param jenjang jenjang sasaran
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Penyaring semester sasaran. {@code null} berarti nominal berlaku di semua semester.
	 *
	 * <p>Getter murni. Perhatikan bahwa badan method sengaja hanya berisi {@code return}
	 * &mdash; tanpa penjaga ternary, tanpa penurunan nilai &mdash; sehingga {@code null}
	 * benar-benar diteruskan ke pemanggil sebagai &quot;tidak dibatasi&quot;, bukan
	 * dikonversi menjadi {@code 0} yang justru berarti &quot;semester nol&quot;.</p>
	 *
	 * @return semester sasaran; {@code null} bila tidak dibatasi
	 */
	@Column(name = "semester")
	public Integer getSemester() {

		return this.semester;
	}

	/**
	 * Setter penyaring semester.
	 *
	 * @param semester semester sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Penyaring tahun angkatan sasaran &mdash; memungkinkan nominal berbeda bagi angkatan
	 * berbeda, yang merupakan pola lazim ketika biaya kuliah dinaikkan hanya untuk mahasiswa
	 * baru. {@code null} berarti berlaku untuk semua angkatan. Getter murni.
	 *
	 * @return tahun angkatan sasaran; {@code null} bila tidak dibatasi
	 */
	@Column(name = "angkatan")
	public Integer getAngkatan() {
		return this.angkatan;
	}

	/**
	 * Setter penyaring tahun angkatan.
	 *
	 * @param angkatan tahun angkatan sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setAngkatan(Integer angkatan) {
		this.angkatan = angkatan;
	}

	/**
	 * <b>Nominal master</b> komponen biaya ini &mdash; angka rupiah yang menjadi asal-usul
	 * hampir seluruh tagihan mahasiswa pada AIS.
	 *
	 * <h4>INI GETTER DESTRUKTIF YANG PALING BERDAMPAK DI KELAS INI</h4>
	 * <p>Method ini bukan pembaca. Ia <b>menghitung ulang</b> nominal dari hierarki setting
	 * biaya, lalu <b>menugaskan hasilnya ke field {@code nilaiBiaya}</b> &mdash; field yang
	 * dipetakan ke kolom {@code detail_biaya.nilai_biaya} melalui anotasi
	 * {@code @Column} di atas. Karena kelas ini memakai <i>property access</i>, Hibernate
	 * memanggil getter ini pada setiap {@code dirty check}/{@code flush}. Rangkaian akibatnya:
	 * <b>membaca</b> sebuah {@code DetailBiaya} di dalam session yang terbuka dapat
	 * menghasilkan {@code UPDATE detail_biaya SET nilai_biaya = ...} yang tidak pernah
	 * diminta siapa pun, ditambah satu revisi Envers ({@code @Audited}) yang mencatatnya
	 * sebagai perubahan berpelaku dan berwaktu.</p>
	 *
	 * <p>Inilah rujukan konkret dari catatan yang selama ini tercantum pada
	 * {@link CicilanPembayaran#getKegiatan()} dan {@link DetailKegiatan#getBiaya()} bahwa
	 * {@code DetailBiaya} adalah &quot;master bersama yang dapat berubah dan tersimpan lewat
	 * getter&quot;. Yang membuatnya berdampak luas adalah sifat {@code DetailBiaya} sebagai
	 * <b>master bersama</b>: satu baris melayani semua mahasiswa yang cocok dengan kombinasi
	 * penyaring di {@link #key()}. Jadi penulisan tak sengaja itu bukan mengubah nominal satu
	 * orang, melainkan nominal seluruh kelompok sasaran.</p>
	 *
	 * <h4>Tiga cabang penurunan nominal</h4>
	 * <p>Dievaluasi berurutan, yang pertama cocok itulah yang menang:</p>
	 * <ol>
	 *   <li><b>Per program studi.</b> Bila {@link #getJurusan()} terisi dan
	 *       {@link SettingBiaya#getTampilkanPerProdi()} menyala, nominal diambil dari
	 *       {@link DetailSettingBiaya#ambilDefaultBiaya(Jurusan)} &mdash; tarif khusus prodi
	 *       tersebut.</li>
	 *   <li><b>Per item biaya dari peta JSON.</b> Bila {@link #getSettingBiayaDetail()} dan
	 *       {@link #getItemBiaya()} keduanya tersimpan, kolom
	 *       {@link SettingBiayaDetail#getBiayas()} diurai sebagai objek JSON dan nominal
	 *       diambil dari kunci berupa id item biaya; bila kunci itu tidak ada, dipakai
	 *       {@link DetailSettingBiaya#getDefaultBiaya()}.</li>
	 *   <li><b>Biaya default.</b> Bila {@link SettingBiaya#getGunakanBiayaDefault()} menyala,
	 *       dipakai {@link DetailSettingBiaya#getDefaultBiaya()}.</li>
	 * </ol>
	 * <p>Bila tidak satu pun cocok, nilai yang tersimpan di field dipertahankan. Pada akhir
	 * method, {@code null} dinormalkan menjadi {@code 0.0} &mdash; juga dengan penulisan ke
	 * field, sehingga baris yang {@code nilai_biaya}-nya {@code NULL} akan tertulis menjadi
	 * {@code 0} pada pembacaan pertama.</p>
	 *
	 * <h4>Perisai untuk entity yang sudah terputus</h4>
	 * <p>Nilai kolom yang tersimpan disalin lebih dulu ke {@code nilaiTersimpan}. Bila
	 * penurunan di atas melempar {@link org.hibernate.LazyInitializationException} atau
	 * {@link org.hibernate.SessionException} &mdash; yang terjadi ketika relasi lazi menunjuk
	 * session yang sudah ditutup, lazim pada pemrosesan asinkron &mdash; field dikembalikan
	 * ke {@code nilaiTersimpan}. Ini penting dan benar: tanpa pemulihan itu, sebuah entity
	 * yang kebetulan dibaca dalam keadaan terputus akan menuliskan {@code 0.0} menimpa
	 * nominal yang sah. Exception jenis lain hanya dicatat lewat
	 * {@code ErrorAuditUtil.record(...)} dan nilai apa pun yang terlanjur tertulis di field
	 * <b>tidak</b> dipulihkan &mdash; celah sempit yang tersisa dari perisai ini.</p>
	 *
	 * <h4>Yang perlu diperhatikan pemanggil</h4>
	 * <ul>
	 *   <li><b>Nominal ini bersifat hidup, bukan snapshot.</b> Mengubah
	 *       {@link DetailSettingBiaya} atau {@link SettingBiayaDetail} induk akan mengubah
	 *       nilai yang dikembalikan di sini untuk seluruh baris yang menurunkannya &mdash;
	 *       termasuk baris yang tagihannya sudah terbit. Mekanisme yang disediakan untuk
	 *       membekukan nominal adalah {@link DetailKegiatan#getBiaya()} (per mahasiswa) dan
	 *       {@link Kegiatan#simpanNominalTagihanTerkunci} (koreksi manual bernomor lengkap
	 *       dengan alasan dan pelaku), bukan kolom ini.</li>
	 *   <li><b>Bukan nominal final.</b> Untuk item biaya berpenghitungan perkalian, nilai di
	 *       sini adalah harga <b>per unit</b>; hasil perkaliannya ada di
	 *       {@link #getNilaiBiayaBaru()}. Potongan/diskon juga belum dikurangkan. Nominal
	 *       final sebuah baris tagihan dihitung
	 *       {@link Kegiatan#ambilJumlahTagihan(Kegiatan, DetailBiaya, boolean)}.</li>
	 *   <li><b>Hindari memanggilnya sekadar untuk menampilkan.</b> Pada laporan atau daftar
	 *       yang menelusuri banyak {@code DetailBiaya} di dalam satu session, setiap
	 *       pemanggilan berpotensi menambah satu {@code UPDATE} dan satu revisi audit.</li>
	 * </ul>
	 *
	 * @return nominal master baris ini; tidak pernah {@code null} (dinormalkan ke {@code 0.0})
	 */
	@Column(name = "nilai_biaya", precision = 15)
	public Double getNilaiBiaya() {
		/*
		 * Hibernate juga memanggil getter ini saat flush. Pada proses async, relasi
		 * lazy dapat masih menunjuk session lama yang sudah ditutup. Dalam keadaan
		 * itu nilai kolom yang tersimpan tetap sah; perhitungan dinamis hanya boleh
		 * dijalankan ketika seluruh relasi masih dapat dibaca.
		 */
		Double nilaiTersimpan = nilaiBiaya;
		try {
			itemBiaya = getItemBiaya();
			settingBiayaDetail = getSettingBiayaDetail();
			settingBiaya = getSettingBiaya();
			detailSettingBiaya = getDetailSettingBiaya();
			jurusan = getJurusan();
			if (jurusan != null && jurusan.getId() != null && settingBiaya != null
					&& settingBiaya.getTampilkanPerProdi() && detailSettingBiaya != null) {
				nilaiBiaya = detailSettingBiaya.ambilDefaultBiaya(jurusan);
			} else if (settingBiayaDetail != null && settingBiayaDetail.getId() != null
					&& itemBiaya != null && itemBiaya.getId() != null && detailSettingBiaya != null) {
				JSONObject jsonObject = new JSONObject(settingBiayaDetail.getBiayas());
				nilaiBiaya = jsonObject.isNull(itemBiaya.getId().toString()) ? detailSettingBiaya.getDefaultBiaya()
						: jsonObject.getDouble(itemBiaya.getId().toString());
			} else if (detailSettingBiaya != null && settingBiaya != null
					&& settingBiaya.getGunakanBiayaDefault()) {
				nilaiBiaya = detailSettingBiaya.getDefaultBiaya();
			}
		} catch (org.hibernate.LazyInitializationException e) {
			// Entity detached: pertahankan nilai biaya yang sudah tersimpan.
			nilaiBiaya = nilaiTersimpan;
		} catch (org.hibernate.SessionException e) {
			// Proxy terhubung ke session tertutup; perlakukan sama seperti detached.
			nilaiBiaya = nilaiTersimpan;
		} catch (Exception e) {
			e.printStackTrace();
			ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/DetailBiaya.java:getNilaiBiaya");
		}
		if (nilaiBiaya == null) {
			nilaiBiaya = 0.0;
		}
		return this.nilaiBiaya;
	}

	/**
	 * Setter nominal master baris ini.
	 *
	 * <p><b>Nilai yang diisi di sini tidak dijamin bertahan.</b> {@link #getNilaiBiaya()}
	 * menghitung ulang nominal dari {@link DetailSettingBiaya}/{@link SettingBiayaDetail}
	 * setiap kali dipanggil dan menimpa field ini, sehingga nominal yang ditetapkan langsung
	 * lewat setter hanya bertahan selama induk setting biaya tidak memenuhi salah satu dari
	 * tiga cabang penurunan di getter tersebut. Untuk menetapkan nominal yang benar-benar
	 * tetap bagi seorang mahasiswa, mekanisme yang disediakan adalah nominal terkunci pada
	 * {@link Kegiatan#simpanNominalTagihanTerkunci}, bukan setter ini.</p>
	 *
	 * @param nilaiBiaya nominal master
	 */
	public void setNilaiBiaya(Double nilaiBiaya) {
		this.nilaiBiaya = nilaiBiaya;
	}

	/**
	 * Setter nama tampilan baris biaya.
	 *
	 * <p>Nilai yang diisi di sini <b>tidak bertahan</b> selama {@link #getItemBiaya()}
	 * terisi, karena {@link #getNama()} selalu menurunkan ulang nama dari item biaya dan
	 * menimpanya. Lihat javadoc getter tersebut.</p>
	 *
	 * @param nama nama tampilan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Nama tampilan baris biaya sebagaimana muncul di lembar tagihan dan kuitansi mahasiswa.
	 *
	 * <p>Diturunkan dari {@link ItemBiaya#getNama()}, dengan imbuhan {@code " ke-N"} bila
	 * setting biaya induknya menetapkan pembayaran bertahap
	 * ({@link SettingBiaya#getJumlahPembayaran()} lebih dari satu) &mdash; sehingga mahasiswa
	 * melihat mis. &quot;SPP ke-1&quot;, &quot;SPP ke-2&quot;. Nomor tahapnya berasal dari
	 * {@link #getBayarKe()}.</p>
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Kedua cabang menugaskan hasilnya ke field {@code nama},
	 * yang dipetakan ke kolom {@code detail_biaya.nama} ({@code nullable = false}). Membaca
	 * entity di dalam session terbuka karenanya dapat menuliskan nama turunan ke database
	 * beserta revisi Envers. Dampaknya jauh lebih ringan daripada
	 * {@link #getNilaiBiaya()} karena yang tertulis hanyalah label, bukan angka rupiah;
	 * namun berarti kolom {@code nama} bukan data mandiri &mdash; mengganti nama sebuah
	 * {@link ItemBiaya} akan merambat ke seluruh baris {@code DetailBiaya} yang memakainya.</p>
	 *
	 * <p>Perhatikan pula bahwa method ini memanggil {@link #getSettingBiayaDetail()} beserta
	 * rantai {@code getSettingBiaya()} di dalamnya tanpa pembungkus {@code try/catch},
	 * berbeda dari kehati-hatian yang ditunjukkan {@link #getNilaiBiaya()} terhadap entity
	 * terputus. Pada entity <i>detached</i>, pemanggilan ini dapat melempar
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return nama tampilan baris biaya; {@code null} bila item biaya kosong dan field belum
	 *         pernah diisi
	 */
	@Column(name = "nama", length = 255, nullable = false)
	public String getNama() {
		if (getItemBiaya() != null && getSettingBiayaDetail() != null
				&& getSettingBiayaDetail().getSettingBiaya() != null
				&& getSettingBiayaDetail().getSettingBiaya().getJumlahPembayaran() > 1) {
			nama = getItemBiaya().getNama() + " ke-" + getBayarKe();
		} else if (getItemBiaya() != null) {
			nama = getItemBiaya().getNama();
		}
		return nama;
	}

	/**
	 * Setter penyaring jenis kegiatan.
	 *
	 * @param jenisKegiatan jenis kegiatan sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * Penyaring {@link JenisKegiatan} sasaran &mdash; menentukan jenis tagihan mana yang
	 * memanggil nominal baris ini. {@code null} berarti tidak dibatasi jenis kegiatan
	 * tertentu.
	 *
	 * <p>Relasi inilah yang menghubungkan katalog aturan ({@link JenisKegiatan}) dengan master
	 * nominal (kelas ini). Ia juga dipakai {@link #checkDenda} sebagai sumber konfigurasi
	 * denda tingkat jenis kegiatan &mdash; perhatikan bahwa {@link #checkDendaCicilan}
	 * justru <b>tidak</b> membacanya dari sini melainkan dari {@link JadwalPembayaran} yang
	 * dioper pemanggil; lihat catatan pada method tersebut.</p>
	 *
	 * <p>Getter relasi lazy standar dengan {@code check(...)}.</p>
	 *
	 * @return jenis kegiatan sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		jenisKegiatan = check(jenisKegiatan);
		return jenisKegiatan;
	}

	/**
	 * Mengurutkan rincian biaya berdasarkan {@link ItemBiaya#getKode()}.
	 *
	 * <p><b>Urutannya terbalik.</b> Perbandingan dilakukan sebagai
	 * {@code o.itemBiaya.getKode().compareTo(itemBiaya.getKode())} &mdash; argumen sebagai
	 * penerima dan {@code this} sebagai pembanding, kebalikan dari konvensi
	 * {@link Comparable}. Akibatnya daftar rincian biaya tersusun <b>menurun</b> menurut kode
	 * item biaya. Karena seluruh pemanggil memakai pengurutan ini secara konsisten, hasilnya
	 * stabil; namun perlu diketahui saat membandingkan dengan
	 * {@link JenisKegiatan#compareTo(GeneralValueObject)} yang menaik.</p>
	 *
	 * <p>Seperti kerabatnya, seluruh perbandingan dibungkus {@code try/catch} yang
	 * mengembalikan {@code 0} pada kegagalan apa pun &mdash; termasuk ketika salah satu kode
	 * item biaya {@code null} sehingga {@code .trim()} melempar
	 * {@code NullPointerException}. Perhatikan bahwa pemeriksaan {@code != null} di sana
	 * dilakukan atas hasil {@code getKode().trim()}, bukan atas {@code getKode()} itu
	 * sendiri, sehingga pemeriksaan tersebut tidak pernah benar-benar mencegah NPE &mdash;
	 * yang menahan hanyalah {@code try/catch}-nya. Nilai {@code 0} berarti &quot;dianggap
	 * sama&quot;, sehingga elemen bermasalah menempati urutan yang tidak deterministik.</p>
	 *
	 * <p>Method ini juga menugaskan {@code itemBiaya = getItemBiaya()} pada {@code this}
	 * <b>dan</b> pada objek pembanding ({@code o.itemBiaya = o.getItemBiaya()}); keduanya
	 * hanya memulihkan proxy lewat {@code check(...)} sehingga tidak mengubah foreign key.</p>
	 *
	 * @param arg0 objek pembanding
	 * @return hasil perbandingan kode item biaya secara terbalik; {@code 0} bila tidak dapat
	 *         dibandingkan
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		itemBiaya = getItemBiaya();
		if (arg0 instanceof DetailBiaya) {
			try {
				DetailBiaya o = (DetailBiaya) arg0;
				o.itemBiaya = o.getItemBiaya();
				if (o.itemBiaya != null && itemBiaya != null && o.itemBiaya.getKode().trim() != null
						&& itemBiaya.getKode().trim() != null) {
					return o.itemBiaya.getKode().trim().compareTo(itemBiaya.getKode().trim());
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/DetailBiaya.java:454");
			}
		}
		return 0;
	}

	/**
	 * Setter penyaring status mahasiswa.
	 *
	 * <p>Nilai yang diisi di sini <b>akan ditimpa</b> oleh {@link #getStatusMahasiswa()}
	 * pada pembacaan berikutnya bila {@link #getSettingBiaya()} induknya memiliki status
	 * mahasiswa sendiri. Lihat javadoc getter tersebut.</p>
	 *
	 * @param statusMahasiswa status mahasiswa sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setStatusMahasiswa(StatusMahasiswa statusMahasiswa) {
		this.statusMahasiswa = statusMahasiswa;
	}

	/**
	 * Penyaring status mahasiswa sasaran (aktif, cuti, dan seterusnya).
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; menimpa FOREIGN KEY, dan karenanya mengubah SASARAN
	 * baris ini.</b> Bila {@link #getSettingBiaya()} terisi dan induk itu memiliki status
	 * mahasiswa sendiri, field {@code statusMahasiswa} ditimpa dengan nilai milik induk lalu
	 * dikembalikan. Property ini dipetakan ke kolom {@code detail_biaya.status_mahasiswa},
	 * sehingga penulisannya sampai ke database pada {@code flush} berikutnya.</p>
	 *
	 * <p>Perbedaannya dengan getter destruktif lain di kelas ini perlu digarisbawahi: yang
	 * ditimpa bukan sekadar label atau angka, melainkan <b>salah satu dimensi penyaring</b>
	 * yang menentukan mahasiswa mana yang memperoleh nominal baris ini &mdash; dimensi yang
	 * ikut membentuk {@link #key()}. Menimpanya berarti diam-diam <b>memindahkan baris
	 * nominal ini ke kelompok sasaran yang lain</b>. Bila operator sengaja menyetel status
	 * mahasiswa berbeda dari induknya (mis. satu rincian khusus mahasiswa cuti di bawah
	 * setting biaya bagi mahasiswa aktif), penyetelan itu tidak akan bertahan.</p>
	 *
	 * <p>Perhatikan pula urutannya: {@code check(...)} dipanggil lebih dulu, lalu penimpaan
	 * dilakukan tanpa memeriksa apakah field sudah terisi &mdash; jadi ini penimpaan tanpa
	 * syarat, bukan pengisian nilai kosong (<i>auto-seed</i>) seperti
	 * {@link #getStatusAwalMahasiswa()}.</p>
	 *
	 * @return status mahasiswa sasaran; {@code null} bila tidak dibatasi dan induk pun tidak
	 *         menentukannya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_mahasiswa", nullable = true)
	public StatusMahasiswa getStatusMahasiswa() {
		statusMahasiswa = check(statusMahasiswa);

		if (getSettingBiaya() != null && getSettingBiaya().getStatusMahasiswa() != null) {
			statusMahasiswa = getSettingBiaya().getStatusMahasiswa();
		}

		return statusMahasiswa;
	}

	/**
	 * Penanda bahwa baris ini mewakili sebuah <b>pembayaran</b> dan bukan tagihan &mdash;
	 * dipakai untuk komponen yang mengurangi jumlah terutang alih-alih menambahnya.
	 *
	 * <p>Getter murni yang mengembalikan field apa adanya. Perhatikan bahwa ia
	 * <b>tidak memasang penjaga ternary</b>, sehingga dapat mengembalikan {@code null} untuk
	 * baris lama yang kolomnya belum terisi &mdash; walaupun field-nya diberi nilai awal
	 * {@code false} pada deklarasi, nilai awal itu ditimpa {@code null} dari database saat
	 * Hibernate memuat baris tersebut. Pemanggil yang melakukan auto-unboxing ke
	 * {@code boolean} perlu berjaga terhadap {@code NullPointerException}.</p>
	 *
	 * @return {@code true}/{@code false}/{@code null} sesuai isi kolom
	 */
	@Column(name = "merupakan_pembayaran")
	public Boolean getMerupakanPembayaran() {
		return merupakanPembayaran;
	}

	/**
	 * Setter penanda pembayaran.
	 *
	 * @param merupakanPembayaran {@code true} bila baris ini mewakili pembayaran
	 */
	public void setMerupakanPembayaran(Boolean merupakanPembayaran) {
		this.merupakanPembayaran = merupakanPembayaran;
	}

	/**
	 * Penyaring bahasa pengantar sasaran &mdash; memungkinkan nominal berbeda bagi kelas
	 * berbahasa asing.
	 *
	 * <p>Getter murni tanpa anotasi pemetaan eksplisit, sehingga Hibernate memetakannya ke
	 * kolom bernama {@code bahasa} dengan pengaturan bawaan. Seperti {@code wnaAtauWni} dan
	 * {@code fakultas}, dimensi ini <b>tidak ikut membentuk</b> {@link #key()}.</p>
	 *
	 * @return bahasa pengantar sasaran; {@code null} bila tidak dibatasi
	 */
	public String getBahasa() {
		return bahasa;
	}

	/**
	 * Setter penyaring bahasa pengantar.
	 *
	 * @param bahasa bahasa pengantar sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setBahasa(String bahasa) {
		this.bahasa = bahasa;
	}

	/**
	 * Penyaring semester mulai belajar ({@link Perkuliahan#GANJIL} atau
	 * {@link Perkuliahan#GENAP}) &mdash; membedakan nominal bagi mahasiswa yang memulai
	 * kuliah di semester genap, yang penomoran semesternya bergeser.
	 *
	 * <p><b>GETTER DESTRUKTIF ringan (auto-seed literal).</b> Bila field {@code null},
	 * diisi {@link Perkuliahan#GANJIL} lalu ditulis balik. Karena property ini dipetakan ke
	 * kolom, baris yang kolomnya {@code NULL} akan tertulis menjadi {@code "Ganjil"} pada
	 * pembacaan pertama di dalam session terbuka.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #key()} membaca dimensi ini sebagai <b>field mentah</b>,
	 * bukan lewat getter ini. Selama auto-seed belum berjalan, kunci memuat {@code null};
	 * sesudahnya memuat {@code "Ganjil"}. Dua baris yang secara efektif identik karena itu
	 * dapat menghasilkan kunci berbeda bergantung pada apakah salah satunya kebetulan sudah
	 * pernah dibaca lewat getter ini.</p>
	 *
	 * @return semester mulai belajar sasaran; tidak pernah {@code null}
	 */
	@Column(name = "mulai_belajar_di_semester")
	public String getMulaiBelajarDiSemester() {
		if (mulaiBelajarDiSemester == null) {
			mulaiBelajarDiSemester = Perkuliahan.GANJIL;
		}
		return mulaiBelajarDiSemester;
	}

	/**
	 * Setter penyaring semester mulai belajar.
	 *
	 * @param mulaiBelajarDiSemester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 */
	public void setMulaiBelajarDiSemester(String mulaiBelajarDiSemester) {
		this.mulaiBelajarDiSemester = mulaiBelajarDiSemester;
	}

	/**
	 * Penyaring {@link StatusAwalMahasiswa} sasaran (baru, pindahan, alih jenjang, dan
	 * seterusnya).
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; mengisi FOREIGN KEY yang kosong (auto-seed).</b> Bila
	 * setelah {@code check(...)} field masih {@code null}, ia diisi
	 * {@code ConstantValues.BARU} lalu dikembalikan. Property ini dipetakan ke kolom
	 * {@code detail_biaya.status_awal_mahasiswa}, sehingga pengisian itu sampai ke database.</p>
	 *
	 * <p><b>Perubahan makna yang halus namun berdampak.</b> Pada kolom penyaring, {@code NULL}
	 * lazimnya berarti &quot;tidak dibatasi &mdash; berlaku untuk semua status awal&quot;,
	 * sedangkan {@code BARU} berarti &quot;hanya untuk mahasiswa baru&quot;. Auto-seed ini
	 * karenanya <b>mempersempit sasaran</b> baris nominal: rincian biaya yang semula berlaku
	 * bagi seluruh mahasiswa berubah menjadi hanya bagi mahasiswa berstatus awal baru, tanpa
	 * ada yang mengubahnya lewat antarmuka. Mahasiswa pindahan atau alih jenjang dapat
	 * kehilangan kecocokan dengan baris tersebut sesudahnya.</p>
	 *
	 * <p>Berbeda dari {@link #getStatusMahasiswa()} yang menimpa tanpa syarat, penulisan di
	 * sini hanya terjadi saat field kosong; nilai yang sudah diisi operator tetap dihormati.
	 * Dimensi ini ikut membentuk {@link #key()}, sehingga auto-seed juga mengubah kunci
	 * identitas logis baris.</p>
	 *
	 * @return status awal mahasiswa sasaran; tidak pernah {@code null} setelah auto-seed
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		if (statusAwalMahasiswa == null) {
			statusAwalMahasiswa = ConstantValues.BARU;
		}
		return statusAwalMahasiswa;
	}

	/**
	 * Setter penyaring status awal mahasiswa.
	 *
	 * @param statusAwalMahasiswa status awal sasaran; {@code null} akan diisi otomatis
	 *                            menjadi {@code BARU} oleh {@link #getStatusAwalMahasiswa()}
	 */
	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	/**
	 * Keterangan tambahan yang menyertai baris biaya pada lembar tagihan &mdash; mis.
	 * rincian &quot;(50.000) x 20 SKS&quot; untuk item berpenghitungan perkalian, atau
	 * catatan khusus prodi.
	 *
	 * <p><b>Bertanda {@code @Transient}</b>, sehingga meskipun method ini menugaskan hasilnya
	 * ke field {@code keterangan}, tulisan itu <b>tidak pernah sampai ke database</b>.
	 * Sifatnya nilai turunan yang hidup di memori saja &mdash; berbeda dari
	 * {@link #getNilaiBiaya()} atau {@link #getNama()} yang penulisannya persisten. Inilah
	 * pembeda yang penting saat menilai getter mana di kelas ini yang benar-benar berbahaya.</p>
	 *
	 * <h4>Penurunan nilai</h4>
	 * <p>Hanya berjalan bila field masih kosong. Dimulai dari {@link ItemBiaya#getNama()},
	 * lalu ditambahi keterangan dari hierarki setting biaya:</p>
	 * <ul>
	 *   <li>Bila {@link SettingBiaya#getTampilkanPerProdi()} menyala dan
	 *       {@link #getJurusan()} terisi, ditambahkan
	 *       {@link DetailSettingBiaya#ambilDefaultKeteranganTagihan(Jurusan)}.</li>
	 *   <li>Bila tidak, dan {@link SettingBiaya#getGunakanBiayaDefault()} menyala,
	 *       ditambahkan {@link DetailSettingBiaya#getDefaultKeterangan()}.</li>
	 * </ul>
	 *
	 * <p>Keterangan inilah yang diperbarui {@link #updateKeterangan(Mahasiswa, Integer)}
	 * ketika item biaya berpenghitungan perkalian dihitung untuk seorang mahasiswa &mdash;
	 * proses yang sekaligus mengisi {@link #getNilaiBiayaBaru()}.</p>
	 *
	 * @return keterangan baris biaya; {@code null} bila tidak ada sumber yang dapat diturunkan
	 */
	@Transient
	public String getKeterangan() {
		if (keterangan == null || keterangan.trim().isEmpty()) {
			itemBiaya = getItemBiaya();
			if (itemBiaya != null) {
				keterangan = itemBiaya.getNama();
			}

			detailSettingBiaya = getDetailSettingBiaya();
			jurusan = getJurusan();

			if (jurusan != null && jurusan.getId() != null && detailSettingBiaya != null
					&& detailSettingBiaya.getSettingBiaya() != null
					&& detailSettingBiaya.getSettingBiaya().getTampilkanPerProdi()) {

				String s = detailSettingBiaya.ambilDefaultKeteranganTagihan(jurusan);
				if (s != null && !s.trim().isEmpty()) {
					keterangan += ", " + s;
				}
			} else if (detailSettingBiaya != null && detailSettingBiaya.getSettingBiaya() != null
					&& detailSettingBiaya.getSettingBiaya().getGunakanBiayaDefault()
					&& !detailSettingBiaya.getDefaultKeterangan().trim().isEmpty()) {
				keterangan += ", " + detailSettingBiaya.getDefaultKeterangan();
			}
		}

		return keterangan;
	}

	/**
	 * Setter keterangan baris biaya. Masukan {@code null} diabaikan &mdash; setter
	 * <b>satu arah</b> yang tidak dapat mengosongkan keterangan yang sudah ada.
	 *
	 * <p>Blok besar yang dikomentari di dalam method ini adalah sisa penelusuran lama: dahulu
	 * sebuah {@code Exception} sengaja dilempar-dan-ditangkap hanya untuk mencetak
	 * <i>stack trace</i> ketika keterangan diubah menjadi nilai berbeda
	 * ({@code "Who called me?"}), demi menemukan pemanggil yang menimpa keterangan secara tak
	 * terduga. Kode itu sudah tidak aktif dan tidak perlu dihidupkan kembali; ia ditinggalkan
	 * sebagai jejak bahwa penulisan keterangan pernah menjadi sumber masalah.</p>
	 *
	 * @param keterangan keterangan baru; diabaikan bila {@code null}
	 */
	public void setKeterangan(String keterangan) {

		if (keterangan == null) {
			return;
		}

//		if (this.keterangan != null && keterangan != null
//				&& !keterangan.trim().equalsIgnoreCase(this.keterangan.trim())) {
//			try {
//				throw new Exception("Who called me?");
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailBiaya.java:558");
//				e.printStackTrace();
//			}
//		}

		this.keterangan = keterangan;
	}

	/**
	 * Menghitung ulang keterangan <b>dan nominal hasil perkalian</b> baris ini untuk seorang
	 * mahasiswa pada semester tertentu, dengan mendelegasikannya ke
	 * {@link ais.action.master.helper.PembayaranNominalModifikasiHelper#updateKeterangan}.
	 *
	 * <p>Meskipun namanya menyebut &quot;keterangan&quot;, method ini adalah pintu masuk
	 * perhitungan nominal untuk item biaya berpenghitungan perkalian &mdash; mis.
	 * &quot;Rp50.000 x jumlah SKS yang diambil&quot;. Helper tersebut mengisi
	 * {@link #getNilaiBiayaBaru()} dengan hasil perkaliannya sekaligus menyusun teks
	 * keterangan yang menjelaskan perhitungan itu kepada mahasiswa. Karena itu
	 * {@link Kegiatan#ambilJumlahTagihan(DetailKegiatan, Kegiatan, DetailBiaya, boolean)}
	 * memanggilnya lebih dulu ketika mendapati {@code nilaiBiayaBaru} masih {@code null}
	 * pada item berpenghitungan &mdash; tanpa itu, yang terpakai adalah harga per unit yang
	 * di layar justru ditampilkan tercoret.</p>
	 *
	 * <p>Perhatikan bahwa penamaan ini menyembunyikan efek sampingnya: pemanggil yang mengira
	 * hanya memperbarui teks sebenarnya juga mengubah nominal yang akan dipakai perhitungan
	 * tagihan berikutnya. Baik {@code keterangan} maupun {@code nilaiBiayaBaru} bertanda
	 * {@code @Transient}, sehingga perubahannya tinggal di memori dan tidak menulis ke
	 * {@code detail_biaya}.</p>
	 *
	 * @param mahasiswa mahasiswa yang menjadi konteks perhitungan
	 * @param semester  semester yang menjadi konteks perhitungan
	 */
	public void updateKeterangan(Mahasiswa mahasiswa, Integer semester) {
		ais.action.master.helper.PembayaranNominalModifikasiHelper.updateKeterangan(this, mahasiswa, semester);
	}

	/**
	 * Status aktif baris rincian biaya; {@code null} dibaca sebagai {@code true}.
	 *
	 * <p>Getter murni (ternary saja, tanpa menulis balik ke field) &mdash; sikap yang benar
	 * dan patut dicatat, karena banyak kerabatnya di kelas ini justru destruktif.</p>
	 *
	 * <p><b>Bawaannya {@code true}</b>: baris yang kolom {@code aktif}-nya {@code NULL}
	 * &mdash; termasuk seluruh data lama dari sebelum kolom ini ada &mdash; dianggap aktif
	 * dan ikut menagih. Ini menjaga kompatibilitas data lama, tetapi berarti penonaktifan
	 * sebuah rincian biaya harus dilakukan dengan menyimpan {@code false} secara eksplisit;
	 * mengosongkan kolomnya justru menghidupkannya kembali.</p>
	 *
	 * @return {@code true} bila rincian biaya masih berlaku; tidak pernah {@code null}
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Setter status aktif rincian biaya.
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code null} berarti aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

//	@Column(name = "nilai_biaya_baru", precision = 15, insertable = false, updatable = false)
	@Transient
	/**
	 * Nominal <b>hasil perhitungan</b> untuk item biaya berpenghitungan perkalian &mdash;
	 * mis. hasil dari &quot;Rp50.000 x 20 SKS&quot;, sedangkan {@link #getNilaiBiaya()}
	 * menyimpan harga per unitnya.
	 *
	 * <p>Bertanda {@code @Transient} (perhatikan anotasi {@code @Column} yang sengaja
	 * dikomentari tepat di atasnya): nilai ini hidup di memori saja dan tidak pernah
	 * tersimpan. Ia diisi {@link #updateKeterangan(Mahasiswa, Integer)}, yang harus dipanggil
	 * lebih dulu agar nilainya tersedia &mdash; itulah sebabnya
	 * {@link Kegiatan#ambilJumlahTagihan(DetailKegiatan, Kegiatan, DetailBiaya, boolean)}
	 * memicu pemanggilan tersebut saat mendapati nilai ini masih {@code null}.</p>
	 *
	 * <p>Getter murni. Nilai {@code null} bermakna &quot;belum dihitung&quot; dan bukan
	 * &quot;nol&quot;; seluruh pemanggil memperlakukannya sebagai penanda untuk jatuh kembali
	 * ke {@link #getNilaiBiaya()}.</p>
	 *
	 * @return nominal hasil perkalian; {@code null} bila belum dihitung
	 */
	public Double getNilaiBiayaBaru() {
		return nilaiBiayaBaru;
	}

	/**
	 * Setter nominal hasil perkalian (hanya di memori; tidak tersimpan).
	 *
	 * @param nilaiBiayaBaru nominal hasil perhitungan; {@code null} berarti belum dihitung
	 */
	public void setNilaiBiayaBaru(Double nilaiBiayaBaru) {
		this.nilaiBiayaBaru = nilaiBiayaBaru;
	}

	/**
	 * Nominal tunggakan semester sebelumnya yang dibawa ke baris ini &mdash; dipakai oleh
	 * item biaya berpenghitungan {@code ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU}, yang menagihkan
	 * sisa utang lama sebagai satu komponen tersendiri.
	 *
	 * <p><b>GETTER DESTRUKTIF ringan.</b> Bila field {@code null}, diisi {@code 0.0} lalu
	 * ditulis balik. Property ini tidak beranotasi eksplisit sehingga Hibernate memetakannya
	 * ke kolom dengan pengaturan bawaan &mdash; artinya penulisan itu persisten. Dampaknya
	 * kecil (mengubah {@code NULL} menjadi {@code 0}, yang secara semantik setara), tetapi
	 * tetap dapat memicu {@code UPDATE} dan revisi Envers pada pembacaan pertama.</p>
	 *
	 * <p>Nilai ini punya <b>prioritas tertinggi</b> pada {@link #hitungTotal(DetailKegiatan)}:
	 * bila melebihi {@code 0.01}, ia mengalahkan nominal master maupun nominal yang sudah
	 * dibekukan pada {@link DetailKegiatan}. Ambang {@code 0.01} dipakai alih-alih
	 * perbandingan dengan nol untuk menghindari masalah presisi bilangan pecahan.</p>
	 *
	 * @return nominal tunggakan semester lalu; tidak pernah {@code null}
	 */
	public Double getTunggakanLalu() {
		if (tunggakanLalu == null) {
			tunggakanLalu = 0.0;
		}
		return tunggakanLalu;
	}

	/**
	 * Setter nominal tunggakan semester lalu.
	 *
	 * @param tunggakanLalu nominal tunggakan; {@code null} akan diisi {@code 0.0} oleh getter
	 */
	public void setTunggakanLalu(Double tunggakanLalu) {
		this.tunggakanLalu = tunggakanLalu;
	}

	/**
	 * Penyaring {@link Paket} biaya sasaran &mdash; memungkinkan satu program studi menawarkan
	 * beberapa paket biaya berbeda. {@code null} berarti tidak dibatasi paket tertentu.
	 * Ikut membentuk {@link #key()}. Getter relasi lazy standar dengan {@code check(...)}.
	 *
	 * @return paket biaya sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket", nullable = true)
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	/**
	 * Setter penyaring paket biaya.
	 *
	 * @param paket paket sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

	/**
	 * Penyaring {@link JenisTinggalMahasiswa} sasaran (mis. tinggal di asrama atau di luar)
	 * &mdash; memungkinkan komponen biaya yang bergantung pada tempat tinggal mahasiswa.
	 * {@code null} berarti tidak dibatasi. Ikut membentuk {@link #key()}. Getter relasi lazy
	 * standar dengan {@code check(...)}.
	 *
	 * @return jenis tinggal sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_tinggal_mahasiswa", nullable = true)
	public JenisTinggalMahasiswa getJenisTinggalMahasiswa() {
		jenisTinggalMahasiswa = check(jenisTinggalMahasiswa);
		return jenisTinggalMahasiswa;
	}

	/**
	 * Setter penyaring jenis tinggal mahasiswa.
	 *
	 * @param jenisTinggalMahasiswa jenis tinggal sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setJenisTinggalMahasiswa(JenisTinggalMahasiswa jenisTinggalMahasiswa) {
		this.jenisTinggalMahasiswa = jenisTinggalMahasiswa;
	}

	/**
	 * Penyaring {@link Kelas} sasaran (mis. pagi, sore, internasional). {@code null} berarti
	 * tidak dibatasi. Ikut membentuk {@link #key()}. Getter relasi lazy standar dengan
	 * {@code check(...)}.
	 *
	 * @return kelas sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas", nullable = true)
	public Kelas getKelas() {
		kelas = check(kelas);
		return kelas;
	}

	/**
	 * Setter penyaring kelas.
	 *
	 * @param kelas kelas sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setKelas(Kelas kelas) {
		this.kelas = kelas;
	}

	/**
	 * Nilai penyaring tambahan bebas ke-1 &mdash; salah satu dari tiga slot serbaguna yang
	 * memungkinkan instalasi menambah dimensi penyaring sendiri tanpa mengubah skema tabel.
	 *
	 * <p>Ketiganya ikut membentuk {@link #key()} dan dirangkai apa adanya di sana; perhatikan
	 * catatan pada {@link #genKey} bahwa nilai yang memuat tanda hubung dapat menggeser batas
	 * antar komponen kunci. Getter murni.</p>
	 *
	 * @return nilai penyaring tambahan ke-1; {@code null} bila tidak dipakai
	 */
	public String getNilaiTambahan1() {
		return nilaiTambahan1;
	}

	/**
	 * Setter nilai penyaring tambahan ke-1.
	 *
	 * @param nilaiTambahan1 nilai penyaring; {@code null} berarti tidak dipakai
	 */
	public void setNilaiTambahan1(String nilaiTambahan1) {
		this.nilaiTambahan1 = nilaiTambahan1;
	}

	/**
	 * Nilai penyaring tambahan bebas ke-2. Lihat {@link #getNilaiTambahan1()}.
	 *
	 * @return nilai penyaring tambahan ke-2; {@code null} bila tidak dipakai
	 */
	public String getNilaiTambahan2() {
		return nilaiTambahan2;
	}

	/**
	 * Setter nilai penyaring tambahan ke-2.
	 *
	 * @param nilaiTambahan2 nilai penyaring; {@code null} berarti tidak dipakai
	 */
	public void setNilaiTambahan2(String nilaiTambahan2) {
		this.nilaiTambahan2 = nilaiTambahan2;
	}

	/**
	 * Nilai penyaring tambahan bebas ke-3. Lihat {@link #getNilaiTambahan1()}.
	 *
	 * @return nilai penyaring tambahan ke-3; {@code null} bila tidak dipakai
	 */
	public String getNilaiTambahan3() {
		return nilaiTambahan3;
	}

	/**
	 * Setter nilai penyaring tambahan ke-3.
	 *
	 * @param nilaiTambahan3 nilai penyaring; {@code null} berarti tidak dipakai
	 */
	public void setNilaiTambahan3(String nilaiTambahan3) {
		this.nilaiTambahan3 = nilaiTambahan3;
	}

	/**
	 * Penyaring {@link GelombangPendaftaran} sasaran &mdash; memungkinkan biaya berbeda per
	 * gelombang pendaftaran, pola lazim untuk memberi insentif pendaftar awal. {@code null}
	 * berarti tidak dibatasi. Ikut membentuk {@link #key()}.
	 *
	 * <p>Perhatikan bahwa gelombang pendaftaran juga menjadi salah satu <b>rute diskon</b>
	 * pada {@link Kegiatan#hitungDiskon} lewat
	 * {@code gelombangPendaftaran.getJenisDiskonMahasiswa()}; jadi selisih biaya antar
	 * gelombang dapat diwujudkan lewat dua mekanisme berbeda &mdash; nominal berbeda di sini,
	 * atau potongan pada mesin diskon. Keduanya dapat aktif bersamaan.</p>
	 *
	 * @return gelombang pendaftaran sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran", nullable = true)
	public GelombangPendaftaran getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		return gelombangPendaftaran;
	}

	/**
	 * Setter penyaring gelombang pendaftaran.
	 *
	 * @param gelombangPendaftaran gelombang sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setGelombangPendaftaran(GelombangPendaftaran gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	/**
	 * Tanggal terbit tagihan bawaan bagi baris ini, diturunkan dari hierarki setting biaya.
	 *
	 * <p>Bertanda {@code @Transient}: meskipun method ini menugaskan hasilnya ke field
	 * {@code defaultTanggalTagihan}, penulisan itu tidak sampai ke database. Nilainya
	 * diturunkan per program studi lewat
	 * {@link DetailSettingBiaya#ambilDefaultTanggalTagihan(Jurusan)} bila
	 * {@link SettingBiaya#getTampilkanPerProdi()} menyala, atau dari
	 * {@link DetailSettingBiaya#getDefaultTanggalTagihan()} bila setting memakai biaya
	 * default.</p>
	 *
	 * <p>Bila tidak satu pun cabang cocok, nilai field sebelumnya dipertahankan &mdash;
	 * termasuk {@code null}. Berbeda dari {@link #getNilaiBiaya()}, tidak ada pembungkus
	 * {@code try/catch} di sini, sehingga entity yang sudah terputus dapat melempar
	 * {@code LazyInitializationException} dari getter ini.</p>
	 *
	 * @return tanggal terbit tagihan bawaan; {@code null} bila tidak dapat diturunkan
	 */
	@Transient
	public Date getDefaultTanggalTagihan() {
		detailSettingBiaya = getDetailSettingBiaya();
		jurusan = getJurusan();

		if (jurusan != null && jurusan.getId() != null && detailSettingBiaya != null
				&& detailSettingBiaya.getSettingBiaya() != null
				&& detailSettingBiaya.getSettingBiaya().getTampilkanPerProdi()) {
			defaultTanggalTagihan = detailSettingBiaya.ambilDefaultTanggalTagihan(jurusan);
		} else if (detailSettingBiaya != null && detailSettingBiaya.getSettingBiaya() != null
				&& detailSettingBiaya.getSettingBiaya().getGunakanBiayaDefault()) {
			defaultTanggalTagihan = detailSettingBiaya.getDefaultTanggalTagihan();
		}

		return defaultTanggalTagihan;
	}

	/**
	 * Setter tanggal terbit tagihan bawaan (hanya di memori; tidak tersimpan).
	 *
	 * @param defaultTanggalTagihan tanggal terbit tagihan
	 */
	public void setDefaultTanggalTagihan(Date defaultTanggalTagihan) {
		this.defaultTanggalTagihan = defaultTanggalTagihan;
	}

	/**
	 * {@link DetailSettingBiaya} induk &mdash; sumber utama nominal, keterangan, tanggal
	 * tagihan, dan tenggat bawaan bagi baris ini.
	 *
	 * <p>Relasi inilah yang membuat {@link #getNilaiBiaya()} bersifat &quot;hidup&quot;:
	 * seluruh cabang penurunan nominal di sana bermuara ke sini. Mengubah satu
	 * {@link DetailSettingBiaya} karena itu merambat ke setiap {@code DetailBiaya} yang
	 * menunjuknya, dan dari situ ke tagihan setiap mahasiswa yang cocok.</p>
	 *
	 * <p>Getter relasi lazy standar dengan {@code check(...)}.</p>
	 *
	 * @return rincian setting biaya induk; {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "detail_setting_biaya", nullable = true)
	public DetailSettingBiaya getDetailSettingBiaya() {
		detailSettingBiaya = check(detailSettingBiaya);
		return detailSettingBiaya;
	}

	/**
	 * Setter rincian setting biaya induk.
	 *
	 * @param detailSettingBiaya rincian setting biaya induk
	 */
	public void setDetailSettingBiaya(DetailSettingBiaya detailSettingBiaya) {
		this.detailSettingBiaya = detailSettingBiaya;
	}

	/**
	 * {@link SettingBiayaDetail} induk &mdash; jalur alternatif penurunan nominal yang
	 * menyimpan biaya per item dalam satu kolom JSON ({@link SettingBiayaDetail#getBiayas()})
	 * alih-alih satu baris per item.
	 *
	 * <p>Dipakai cabang kedua {@link #getNilaiBiaya()}: id {@link ItemBiaya} baris ini
	 * dijadikan kunci pencarian di dalam JSON tersebut. Getter relasi lazy standar dengan
	 * {@code check(...)}.</p>
	 *
	 * @return setting biaya detail induk; {@code null} bila tidak memakai jalur ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "setting_biaya_detail", nullable = true)
	public SettingBiayaDetail getSettingBiayaDetail() {
		settingBiayaDetail = check(settingBiayaDetail);
		return settingBiayaDetail;
	}

	/**
	 * Setter setting biaya detail induk.
	 *
	 * @param settingBiayaDetail setting biaya detail induk
	 */
	public void setSettingBiayaDetail(SettingBiayaDetail settingBiayaDetail) {
		this.settingBiayaDetail = settingBiayaDetail;
	}

	/**
	 * Nomor tahap pembayaran baris ini ketika setting biaya menetapkan pembayaran bertahap
	 * &mdash; mis. {@code 1} untuk &quot;SPP ke-1&quot;, {@code 2} untuk &quot;SPP ke-2&quot;.
	 * {@code null} dibaca sebagai {@code 1}.
	 *
	 * <p>Getter murni (ternary saja, tanpa menulis balik ke field). Nomor ini dipakai
	 * {@link #getNama()} untuk menyusun label tahap, dan menjadi salah satu komponen kunci
	 * unik {@link DetailKegiatan} lewat {@code DetailKegiatan.kodeUnik(...)} &mdash; sehingga
	 * ia ikut menentukan baris rincian tagihan mana yang dianggap sama.</p>
	 *
	 * @return nomor tahap pembayaran; tidak pernah {@code null}
	 */
	public Integer getBayarKe() {
		return bayarKe == null ? 1 : bayarKe;
	}

	/**
	 * Setter nomor tahap pembayaran.
	 *
	 * @param bayarKe nomor tahap; {@code null} dibaca sebagai {@code 1}
	 */
	public void setBayarKe(Integer bayarKe) {
		this.bayarKe = bayarKe;
	}

	/**
	 * {@link SettingBiaya} induk lewat foreign key langsung {@code detail_biaya.setting_biaya}.
	 *
	 * <p>Merupakan salah satu dari <b>tiga</b> jalur menuju setting biaya induk yang sama;
	 * lihat {@link #getSettingBiayaEfektif()} yang memilih di antaranya secara kanonis. Data
	 * lama boleh jadi belum mengisi foreign key ini, sehingga jangan mengandalkannya secara
	 * langsung untuk mencapai induk &mdash; pakai {@code getSettingBiayaEfektif()}.</p>
	 *
	 * <p>Getter relasi lazy standar dengan {@code check(...)}. Perhatikan bahwa relasi ini
	 * juga menjadi sumber penimpaan pada {@link #getStatusMahasiswa()} dan
	 * {@link #getKelamin()}.</p>
	 *
	 * @return setting biaya induk lewat FK langsung; {@code null} pada data lama
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "setting_biaya", nullable = true)
	public SettingBiaya getSettingBiaya() {
		settingBiaya = check(settingBiaya);
		return settingBiaya;
	}

	/**
	 * Setter setting biaya induk (foreign key langsung).
	 *
	 * @param settingBiaya setting biaya induk
	 */
	public void setSettingBiaya(SettingBiaya settingBiaya) {
		this.settingBiaya = settingBiaya;
	}

	/**
	 * Mengembalikan induk setting biaya secara kanonis. Data baru menyimpan ketiga
	 * jalur relasi dengan induk yang sama; urutan ini juga menjaga kompatibilitas
	 * data lama yang belum mempunyai FK {@code detail_biaya.setting_biaya}.
	 */
	@Transient
	public SettingBiaya getSettingBiayaEfektif() {
		try {
			SettingBiayaDetail individual = getSettingBiayaDetail();
			if (individual != null && individual.getSettingBiaya() != null) {
				return individual.getSettingBiaya();
			}
		} catch (Exception ignored) {
			// Proxy lama dapat terputus; lanjutkan ke jalur template/rincian langsung.
		}
		try {
			DetailSettingBiaya rincian = getDetailSettingBiaya();
			if (rincian != null && rincian.getSettingBiaya() != null) {
				return rincian.getSettingBiaya();
			}
		} catch (Exception ignored) {
			// Lanjutkan ke FK langsung.
		}
		return getSettingBiaya();
	}

	/**
	 * Menambahkan <b>denda keterlambatan</b> ke sebuah nominal pembayaran, bila tanggal bayar
	 * melewati tenggat. Ini mesin denda untuk pembayaran <b>sekaligus</b> (non-angsuran);
	 * pasangannya untuk angsuran adalah
	 * {@link #checkDendaCicilan(CicilanPembayaran, JadwalPembayaran, PengaturanPembayaranBulanan)}.
	 *
	 * <h4>Menentukan tenggat</h4>
	 * <p>Dicoba berurutan: {@link PengaturanPembayaranBulanan#getDeadline()}; lalu
	 * {@link JadwalPembayaran#getEndDate()} bila jenis kegiatan pada jadwal mengaktifkan denda
	 * atau jadwal itu khusus untuk satu NIM; terakhir
	 * {@link #getDefaultTanggalDeadline()} milik setting biaya. Tenggat {@code null} berarti
	 * tidak ada denda.</p>
	 *
	 * <h4>Menentukan besaran</h4>
	 * <p>Bermula dari {@link ItemBiaya#getDefaultProsentaseDenda()}, lalu dapat digantikan:</p>
	 * <ul>
	 *   <li>bila {@link JenisKegiatan#getDendaDibuatPerProdi()} menyala, dari peta JSON
	 *       {@link JenisKegiatan#getDendaPerProdi()} menurut id program studi &mdash; prodi
	 *       yang tidak tercantum memperoleh {@code 0.0}, bukan nilai bawaan;</li>
	 *   <li>bila {@link JenisKegiatan#getDendaJikaTerlambat()} menyala, dari
	 *       {@link JenisKegiatan#getDefaultProsentaseDenda()}.</li>
	 * </ul>
	 *
	 * <h4>Menghitung</h4>
	 * <p>Denda dikenakan bila item biaya <b>atau</b> jenis kegiatan mengaktifkan denda,
	 * besarannya lebih dari nol, dan tenggat ada. Keterlambatan ditentukan dengan
	 * membandingkan tanggal <i>sesudah</i> memastikan keduanya bukan hari yang sama
	 * ({@code dateFormat83}) &mdash; jadi membayar pada hari tenggat tidak dianggap
	 * terlambat. Nilai denda dihitung sebagai persen dari nominal atau sebagai rupiah tetap
	 * menurut {@code getNilaiDendaDalamPersen()}, lalu dikalikan jumlah kelipatan
	 * keterlambatan.</p>
	 *
	 * <h4>Hal yang perlu diperhatikan</h4>
	 * <p><b>Pembagian bilangan bulat membuat keterlambatan pendek bebas denda.</b> Jumlah
	 * kelipatan dihitung {@code (Common.getBetweenTwoDates(deadline, tanggalBayar) - 1) /
	 * kelipatan}. Bila {@code kelipatan} bernilai 7, keterlambatan 1&ndash;7 hari menghasilkan
	 * pembagi {@code 0}, sehingga {@code d = d * 0} dan dendanya <b>nol</b> &mdash; bukan satu
	 * kali denda sebagaimana mungkin diharapkan operator. Denda baru muncul pada hari ke-8.
	 * Sebaliknya bila {@code kelipatan} bernilai {@code 0} (bawaan), pengalian dilewati
	 * seluruhnya dan denda dikenakan tepat satu kali.</p>
	 *
	 * <p><b>Peta denda per prodi memakai field {@code jurusan} mentah.</b> Baris
	 * {@code jurusan.getId()} di dalam blok per-prodi membaca field, bukan
	 * {@link #getJurusan()}, sehingga pada entity yang proxy-nya belum dipulihkan atau yang
	 * memang tidak dibatasi prodi ({@code jurusan} {@code null}) akan terjadi
	 * {@code NullPointerException}. Exception itu tertangkap {@code try/catch} di sekitarnya
	 * dan hanya dicatat, sehingga akibatnya adalah denda tetap memakai besaran bawaan dari
	 * item biaya &mdash; gagal secara diam-diam, bukan gagal terlihat.</p>
	 *
	 * <p><b>{@code infoDenda} disetel sebagai efek samping.</b> Method ini menulis penjelasan
	 * denda ke field {@code infoDenda} (bertanda {@code @Transient}, jadi tidak masuk
	 * database) yang harus dibaca pemanggil lewat {@link #getInfoDenda()} sesudahnya. Field
	 * itu dikosongkan di awal setiap pemanggilan, sehingga instance {@code DetailBiaya} yang
	 * sama tidak dapat dipakai menghitung dua denda secara bersamaan &mdash; kelas ini tidak
	 * aman untuk pemakaian lintas thread. Penjelasan yang sama juga dicetak ke
	 * {@code System.out}.</p>
	 *
	 * <p><b>Parameter {@code old} tidak dipakai.</b> Parameter bertipe {@link JenisKegiatan}
	 * bernama {@code old} diterima tetapi tidak pernah dirujuk di badan method; jenis kegiatan
	 * yang benar-benar dipakai diambil dari {@link #getJenisKegiatan()} milik entity ini.</p>
	 *
	 * @param nominalModifikasi           nominal sebelum denda
	 * @param tanggalBayar                tanggal pembayaran yang diperiksa
	 * @param jadwalPembayaran            jadwal pembayaran yang berlaku; boleh {@code null}
	 * @param old                         <b>tidak dipakai</b>
	 * @param pengaturanPembayaranBulanan pengaturan bulanan sumber tenggat; boleh {@code null}
	 * @return nominal sesudah ditambah denda; sama dengan masukan bila tidak ada denda
	 */
	public Double checkDenda(Double nominalModifikasi, Date tanggalBayar, JadwalPembayaran jadwalPembayaran,
			JenisKegiatan old, PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {

		JenisKegiatan jenisKegiatan = this.getJenisKegiatan();

		infoDenda = "";
		Date deadline = pengaturanPembayaranBulanan == null ? null : pengaturanPembayaranBulanan.getDeadline();
		if (jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan().getDendaJikaTerlambat()) {
			deadline = jadwalPembayaran.getEndDate();
		} else if (jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null) {
			deadline = jadwalPembayaran.getEndDate();
		}
		// Gunakan deadline dari SettingBiaya sebagai fallback jika belum diisi dari JadwalPembayaran
		if (deadline == null && this.getDefaultTanggalDeadline() != null) {
			deadline = this.getDefaultTanggalDeadline();
		}

		Double nilaiDenda = this.getItemBiaya() != null ? this.getItemBiaya().getDefaultProsentaseDenda() : 0.0;

		if (jenisKegiatan != null && jenisKegiatan.getDendaDibuatPerProdi()) {
			try {
				JSONObject dendaPerProdi = new JSONObject(jenisKegiatan.getDendaPerProdi());
				nilaiDenda = dendaPerProdi.isNull(jurusan.getId().toString()) ? 0.0
						: dendaPerProdi.getDouble(jurusan.getId().toString());

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/DetailBiaya.java:756");
			}
		}

		else if (jenisKegiatan != null && jenisKegiatan.getDendaJikaTerlambat()) {
			nilaiDenda = jenisKegiatan.getDefaultProsentaseDenda();
		}

		boolean before = deadline != null && tanggalBayar != null
				? (deadline.before(tanggalBayar)
						&& !Common.dateFormat83.get().format(deadline).equals(Common.dateFormat83.get().format(tanggalBayar)))
				: false;

//		System.out.println("checkDenda -> deadline " + (deadline == null ? "" : Common.dateFormat1.get().format(deadline))
//				+ ", tanggalBayar " + (tanggalBayar == null ? "" : Common.dateFormat1.get().format(tanggalBayar))
//				+ ", before " + before + " nilaiDenda " + nilaiDenda + " nominalModifikasi " + nominalModifikasi);

		if (

		(

		(this.getItemBiaya() != null && this.getItemBiaya().getDendaJikaTerlambat())

				||

				(jenisKegiatan != null && jenisKegiatan.getDendaJikaTerlambat())

		)

				&& nilaiDenda > 0.0 && deadline != null) {

			if (before) {

				Double d = 0.0;

				Integer kelipatan = this.getItemBiaya().getDendaAkanBerlipatTerlambaHari();
				Integer maksimal = this.getItemBiaya().getMaksimalBerlipatTerlambaHari();
				Boolean dalamPersen = this.getItemBiaya().getNilaiDendaDalamPersen();

				if (jenisKegiatan != null && jenisKegiatan.getDendaJikaTerlambat()) {
					kelipatan = jenisKegiatan.getDendaAkanBerlipatTerlambaHari();
					maksimal = jenisKegiatan.getMaksimalBerlipatTerlambaHari();
					dalamPersen = jenisKegiatan.getNilaiDendaDalamPersen();
				}

				if (dalamPersen) {
					d = (nilaiDenda * nominalModifikasi) / 100.0;
				} else {
					d = nilaiDenda;
				}

				int terlambathari = Common.getBetweenTwoDates(deadline, tanggalBayar) - 1;

				if (kelipatan > 0) {
					int jumlahKali = terlambathari / kelipatan;

					if (maksimal > 0 && jumlahKali > maksimal) {
						jumlahKali = maksimal;
					}

					d = d * jumlahKali;
				}

				infoDenda = " Penambahan denda senilai " + Common.numberFormat.get().format(nilaiDenda) + " "
						+ (dalamPersen ? "%" : "") + " karena terlambat " + terlambathari + " hari ("
						+ Common.dateFormat1.get().format(tanggalBayar) + ") senilai " + Common.numberFormat.get().format(d) + ".";

				System.out.println(infoDenda);

				nominalModifikasi += d;
			}
		}
		return nominalModifikasi;
	}

	/**
	 * Menghitung <b>denda keterlambatan untuk satu angsuran/cicilan</b>, dan bila ada,
	 * menuliskannya langsung ke {@link CicilanPembayaran} yang bersangkutan.
	 *
	 * <p>Berbeda dari {@link #checkDenda} yang hanya mengembalikan nominal, method ini
	 * <b>memutasi</b> objek cicilan: {@code setDenda(d)}, {@code setNilaiAsli(nominal)},
	 * {@code setNilai(nominal + d)}, dan menambahkan penjelasan denda ke keterangannya.
	 * Nilai kembaliannya adalah besaran denda saja, bukan nominal total.</p>
	 *
	 * <h4>Pembatalan denda</h4>
	 * <p>Di awal, denda dianggap nol bila id {@link PengaturanPembayaranBulanan} terkait
	 * tercantum pada daftar {@link Kegiatan#getPembatalanDenda()} milik header tagihan
	 * &mdash; mekanisme pembebasan denda per baris yang bersifat dapat dibatalkan kembali.</p>
	 *
	 * <h4>PERBEDAAN PENTING DARI {@link #checkDenda}: konfigurasi denda tingkat jenis
	 * kegiatan praktis tidak terpakai di sini</h4>
	 * <p>Dua hal bergabung menghasilkan keadaan ini:</p>
	 * <ol>
	 *   <li><b>Sumber jenis kegiatannya berbeda.</b> {@link #checkDenda} membaca
	 *       {@link #getJenisKegiatan()} milik entity ini, sedangkan method ini hanya membaca
	 *       {@code jadwalPembayaran.getJenisKegiatan()}. Setiap cabang yang menyangkut
	 *       konfigurasi denda jenis kegiatan di sini dijaga {@code jadwalPembayaran != null}.
	 *       Padahal satu-satunya pemanggil produksi &mdash;
	 *       {@link ais.database.hibernate.AuditListener} pada {@code onPostInsert}, lewat
	 *       {@link PengaturanPembayaranBulanan#checkDendaCicilan(CicilanPembayaran, JadwalPembayaran)}
	 *       &mdash; mengoper {@code null} untuk jadwal tersebut. Akibatnya seluruh cabang itu
	 *       tidak pernah berjalan, dan denda angsuran <b>hanya</b> mengikuti konfigurasi
	 *       tingkat {@link ItemBiaya}. Sebuah jenis kegiatan yang dikonfigurasi berdenda
	 *       karena itu mengenakan denda pada pembayaran sekaligus, tetapi tidak pada
	 *       angsuran.</li>
	 *   <li><b>Flag yang diperiksa berbeda.</b> Di ketiga tempat yang relevan (pemilihan
	 *       besaran, gerbang utama, dan penggantian kelipatan/batas/format), method ini
	 *       memeriksa {@link JenisKegiatan#getNilaiDendaDalamPersen()}, sedangkan
	 *       {@link #checkDenda} memeriksa {@link JenisKegiatan#getDendaJikaTerlambat()}.
	 *       Keduanya bukan sinonim: yang pertama sekadar menyatakan SATUAN denda dan
	 *       berbawaan {@code true}, yang kedua adalah saklar ADA/TIDAKNYA denda dan berbawaan
	 *       {@code false}. Seandainya {@code jadwalPembayaran} suatu saat diisi, cabang-cabang
	 *       itu akan salah dalam dua arah: mengenakan denda pada jenis kegiatan yang saklarnya
	 *       mati (karena bawaan {@code true}), sekaligus mengabaikan denda pada jenis kegiatan
	 *       yang dendanya berupa nominal tetap.</li>
	 * </ol>
	 *
	 * <h4>Penanganan galat</h4>
	 * <p>Seluruh perhitungan sesudah pemeriksaan pembatalan dibungkus {@code try/catch} yang
	 * memperlakukan kegagalan apa pun sebagai &quot;tidak ada denda tambahan&quot;
	 * ({@code d} tetap {@code 0.0}). Ini disengaja: method ini berjalan dari dalam listener
	 * {@code onPostInsert}, sehingga exception yang lolos akan menggagalkan proses simpan
	 * keranjang pembayaran. Konsekuensinya, kegagalan menghitung denda tidak pernah terlihat
	 * pengguna &mdash; hanya tercatat lewat {@code ErrorAuditUtil}.</p>
	 *
	 * <p>Seperti {@link #checkDenda}, blok denda per prodi di sini juga membaca field
	 * {@code jurusan} mentah sehingga rawan {@code NullPointerException} yang tertangkap
	 * diam-diam, dan {@code infoDenda} disetel sebagai efek samping.</p>
	 *
	 * @param cicilanPembayaran           angsuran yang diperiksa; <b>dimutasi</b> bila ada denda
	 * @param jadwalPembayaran            jadwal pembayaran; {@code null} dari jalur produksi,
	 *                                    yang menonaktifkan seluruh cabang konfigurasi jenis
	 *                                    kegiatan
	 * @param pengaturanPembayaranBulanan pengaturan bulanan sumber tenggat dan nominal
	 * @return besaran denda; {@code 0.0} bila tidak ada denda atau perhitungan gagal
	 */
	public Double checkDendaCicilan(CicilanPembayaran cicilanPembayaran, JadwalPembayaran jadwalPembayaran,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {

		infoDenda = "";
		try {
			boolean batal = cicilanPembayaran != null && cicilanPembayaran.getKegiatan() != null
					&& cicilanPembayaran.getPengaturanPembayaranBulanan() != null
					&& StringUtils.contains(cicilanPembayaran.getKegiatan().getPembatalanDenda(),
							"," + cicilanPembayaran.getPengaturanPembayaranBulanan().getId() + ",");
			if (batal) {
				return 0.0;
			}
		} catch (Exception e) {
			// Data cicilan belum lengkap saat listener audit berjalan; lanjut hitung normal.
		}
		// Dipanggil dari PengaturanPembayaranBulanan.checkDendaCicilan <- AuditListener.onPostInsert
		// (setiap insert entity terkait). NPE tak tertangkap di sini akan mengganggu proses
		// simpanKeranjang (WizardPembayaranMhsHelper). Bungkus sisa perhitungan denda dengan
		// try-catch defensif: kegagalan hitung denda berarti "tidak ada denda tambahan" (d=0.0),
		// bukan membatalkan insert.
		Double d = 0.0;
		try {
		Date deadline = pengaturanPembayaranBulanan == null ? getDefaultTanggalDeadline()
				: pengaturanPembayaranBulanan.getDeadline();
		// jadwalPembayaran.getJenisKegiatan() bisa null (tak selalu diisi) — guard sebelum
		// dereference agar tidak NPE di jalur listener AuditListener.onPostInsert.
		if (jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan() != null
				&& jadwalPembayaran.getJenisKegiatan().getDendaJikaTerlambat()) {
			deadline = jadwalPembayaran.getEndDate();
		} else if (jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null) {
			deadline = jadwalPembayaran.getEndDate();
		}

		Double nilaiDenda = this.getItemBiaya() != null ? this.getItemBiaya().getDefaultProsentaseDenda() : 0.0;

		if (jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan() != null
				&& jadwalPembayaran.getJenisKegiatan().getDendaDibuatPerProdi()) {
			try {
				JSONObject dendaPerProdi = new JSONObject(jadwalPembayaran.getJenisKegiatan().getDendaPerProdi());
				nilaiDenda = dendaPerProdi.isNull(jurusan.getId().toString()) ? 0.0
						: dendaPerProdi.getDouble(jurusan.getId().toString());

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/DetailBiaya.java:862");
			}
		}

		else if ((jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan() != null
				&& jadwalPembayaran.getJenisKegiatan().getNilaiDendaDalamPersen())) {
			nilaiDenda = jadwalPembayaran.getJenisKegiatan().getDefaultProsentaseDenda();
		}

		if (this.getItemBiaya() != null
				&& (this.getItemBiaya().getDendaJikaTerlambat()
						|| (jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan() != null
								&& jadwalPembayaran.getJenisKegiatan().getNilaiDendaDalamPersen()))
				&& nilaiDenda > 0.0 && deadline != null && cicilanPembayaran.getKegiatan() != null
				&& cicilanPembayaran.getTanggal() != null) {
			Date tanggalBayar = cicilanPembayaran.getTanggal();

			boolean before = deadline != null && tanggalBayar != null
					? (deadline.before(tanggalBayar)
							&& !Common.dateFormat83.get().format(deadline).equals(Common.dateFormat83.get().format(tanggalBayar)))
					: false;

			if (before) {

				Integer kelipatan = this.getItemBiaya().getDendaAkanBerlipatTerlambaHari();
				Integer maksimal = this.getItemBiaya().getMaksimalBerlipatTerlambaHari();

				Boolean dalamPersen = this.getItemBiaya().getNilaiDendaDalamPersen();

				if ((jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan() != null
						&& jadwalPembayaran.getJenisKegiatan().getNilaiDendaDalamPersen())) {
					kelipatan = jadwalPembayaran.getJenisKegiatan().getDendaAkanBerlipatTerlambaHari();
					maksimal = jadwalPembayaran.getJenisKegiatan().getMaksimalBerlipatTerlambaHari();
					dalamPersen = jadwalPembayaran.getJenisKegiatan().getNilaiDendaDalamPersen();
				}

				Double nominalModifikasi = 0.0;

				if (pengaturanPembayaranBulanan != null) {
					nominalModifikasi = cicilanPembayaran.getKegiatan().getMahasiswa() == null
							? pengaturanPembayaranBulanan.getNominal()
							: pengaturanPembayaranBulanan.ambilNominalModifikasi(
									cicilanPembayaran.getKegiatan().getMahasiswa(),
									cicilanPembayaran.getKegiatan().getSemster());
				} else {

					updateKeterangan(cicilanPembayaran.getKegiatan().getMahasiswa(),
							cicilanPembayaran.getKegiatan().getSemster());

					nominalModifikasi = getNilaiBiayaBaru() == null ? getNilaiBiaya() : getNilaiBiayaBaru();
				}

				if (dalamPersen) {
					d = (nilaiDenda * nominalModifikasi) / 100.0;
				} else {
					d = nilaiDenda;
				}

				int terlambathari = Common.getBetweenTwoDates(deadline, tanggalBayar) - 1;

				if (kelipatan > 0) {
					int jumlahKali = terlambathari / kelipatan;

					if (maksimal > 0 && jumlahKali > maksimal) {
						jumlahKali = maksimal;
					}

					d = d * jumlahKali;
				}

				if (d > 0.01) {

					infoDenda = " Penambahan denda senilai " + Common.numberFormat.get().format(nilaiDenda) + " "
							+ (dalamPersen ? "%" : "") + " karena terlambat " + terlambathari + " hari senilai "
							+ Common.numberFormat.get().format(d) + ".";

					cicilanPembayaran.setDenda(d);
					cicilanPembayaran.setNilaiAsli(nominalModifikasi);
					cicilanPembayaran.setNilai(nominalModifikasi + d);
					String keterangan = cicilanPembayaran.getKeterangan();
					keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, infoDenda, "") + infoDenda;
					cicilanPembayaran.setKeterangan(keterangan.trim());

					System.out.println("cicilan " + cicilanPembayaran + ". " + infoDenda);
				}
			}

		}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailBiaya.java:checkDendaCicilan-lazy");
			// kegagalan hitung denda dianggap "tidak ada denda tambahan"; d tetap 0.0
		}
		return d;
	}

	/**
	 * Penjelasan denda hasil perhitungan terakhir {@link #checkDenda} atau
	 * {@link #checkDendaCicilan} &mdash; bertanda {@code @Transient} pada getter-nya sehingga
	 * tidak tersimpan. Dikosongkan di awal setiap perhitungan; lihat catatan keamanan thread
	 * pada {@link #checkDenda}.
	 */
	private String infoDenda;
	private String kelamin;
	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswa;

	/**
	 * Penjelasan denda dari perhitungan terakhir, mis. &quot;Penambahan denda senilai 2 %
	 * karena terlambat 10 hari&quot;. Dipakai antarmuka untuk menerangkan kepada mahasiswa
	 * mengapa nominalnya bertambah.
	 *
	 * <p>Getter murni yang menormalkan {@code null} menjadi string kosong tanpa menulis balik.
	 * Nilainya hanya sahih tepat setelah {@link #checkDenda} atau {@link #checkDendaCicilan}
	 * dipanggil pada instance yang sama.</p>
	 *
	 * @return penjelasan denda; string kosong bila belum ada perhitungan
	 */
	@Transient
	public String getInfoDenda() {
		return infoDenda == null ? "" : infoDenda;
	}

	/**
	 * Setter penjelasan denda (hanya di memori; tidak tersimpan).
	 *
	 * @param infoDenda penjelasan denda
	 */
	public void setInfoDenda(String infoDenda) {
		this.infoDenda = infoDenda;
	}

	/**
	 * Penyaring jenis kelamin sasaran &mdash; memungkinkan komponen biaya yang hanya berlaku
	 * bagi mahasiswa laki-laki atau perempuan, mis. biaya asrama.
	 *
	 * <p><b>GETTER DESTRUKTIF dengan dua penulisan.</b> Property ini tidak beranotasi
	 * eksplisit sehingga dipetakan ke kolom dengan pengaturan bawaan, dan kedua penulisan
	 * di bawah ini persisten:</p>
	 * <ol>
	 *   <li><b>Normalisasi menghapus data.</b> Nilai yang bukan persis {@code "Laki-laki"}
	 *       atau {@code "Perempuan"} <b>diubah menjadi {@code null}</b>. Perbandingannya
	 *       peka huruf besar/kecil dan tanpa {@code trim()}, sehingga isian yang hanya
	 *       berbeda kapitalisasi atau berimbuhan spasi &mdash; mis. dari impor Excel
	 *       &mdash; akan terhapus secara diam-diam, mengubah baris dari &quot;khusus satu
	 *       jenis kelamin&quot; menjadi &quot;berlaku untuk semua&quot;.</li>
	 *   <li><b>Penimpaan dari induk.</b> Bila {@link #getSettingBiaya()} terisi, field
	 *       ditimpa dengan {@link SettingBiaya#getKelamin()} tanpa syarat &mdash; termasuk
	 *       bila nilai induk itu sendiri {@code null}. Jadi setelan jenis kelamin pada baris
	 *       ini tidak pernah bertahan selama FK setting biaya terisi; pola yang sama dengan
	 *       {@link #getStatusMahasiswa()}.</li>
	 * </ol>
	 *
	 * <p>Seperti {@code statusMahasiswa}, yang ditulis di sini adalah sebuah <b>dimensi
	 * penyaring</b>, sehingga penulisannya mengubah kelompok sasaran baris nominal. Berbeda
	 * dari {@code statusMahasiswa}, dimensi ini tidak ikut membentuk {@link #key()}.</p>
	 *
	 * @return penyaring jenis kelamin; {@code null} bila tidak dibatasi
	 */
	public String getKelamin() {
		if (kelamin != null && !(kelamin.equals("Laki-laki") || kelamin.equals("Perempuan"))) {
			kelamin = null;
		}

		settingBiaya = getSettingBiaya();
		if (settingBiaya != null) {
			kelamin = settingBiaya.getKelamin();
		}
		return kelamin;
	}

	/**
	 * Setter penyaring jenis kelamin. Nilai yang diisi di sini akan dinormalkan dan dapat
	 * ditimpa oleh {@link #getKelamin()} pada pembacaan berikutnya.
	 *
	 * @param kelamin {@code "Laki-laki"}, {@code "Perempuan"}, atau {@code null}
	 */
	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	/**
	 * Penyaring {@link AfiliasiCalonMahasiswa} sasaran &mdash; memungkinkan biaya khusus bagi
	 * pendaftar dari afiliasi tertentu (mis. sekolah mitra atau instansi kerja sama).
	 * {@code null} berarti tidak dibatasi.
	 *
	 * <p>Getter relasi lazy standar dengan {@code check(...)}. Seperti {@code wnaAtauWni},
	 * {@code bahasa}, {@code fakultas}, {@code kelamin}, dan {@code jenisSeleksi}, dimensi
	 * ini <b>tidak ikut membentuk</b> {@link #key()} &mdash; tampaknya kunci itu memang hanya
	 * mencakup dimensi yang sudah ada sejak awal, dan tidak diperbarui seiring bertambahnya
	 * penyaring baru.</p>
	 *
	 * @return afiliasi calon mahasiswa sasaran; {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "afiliasi_calon_mahasiswa", nullable = true)
	public AfiliasiCalonMahasiswa getAfiliasiCalonMahasiswa() {
		afiliasiCalonMahasiswa = check(afiliasiCalonMahasiswa);
		return afiliasiCalonMahasiswa;
	}

	/**
	 * Setter penyaring afiliasi calon mahasiswa.
	 *
	 * @param afiliasiCalonMahasiswa afiliasi sasaran; {@code null} berarti tidak dibatasi
	 */
	public void setAfiliasiCalonMahasiswa(AfiliasiCalonMahasiswa afiliasiCalonMahasiswa) {
		this.afiliasiCalonMahasiswa = afiliasiCalonMahasiswa;
	}

	/**
	 * <b>Tenggat pembayaran</b> bawaan bagi baris ini, diturunkan dari hierarki setting biaya
	 * &mdash; tanggal yang menjadi acuan perhitungan denda keterlambatan.
	 *
	 * <p>Bertanda {@code @Transient}, sehingga penulisan ke field tidak sampai ke database.
	 * Penurunannya sejajar dengan {@link #getDefaultTanggalTagihan()}: per program studi lewat
	 * {@link DetailSettingBiaya#ambilDefaultTanggalDeadline(Jurusan)} bila
	 * {@link SettingBiaya#getTampilkanPerProdi()} menyala, atau dari
	 * {@link DetailSettingBiaya#getDefaultTanggalDeadline()} bila setting memakai biaya
	 * default; bila tidak satu pun cocok, nilai sebelumnya dipertahankan.</p>
	 *
	 * <p>Menjadi tenggat <b>cadangan terakhir</b> pada {@link #checkDenda} (setelah
	 * {@link PengaturanPembayaranBulanan} dan {@link JadwalPembayaran}), tetapi justru menjadi
	 * tenggat <b>pertama dan satu-satunya</b> pada {@link #checkDendaCicilan} ketika
	 * {@code pengaturanPembayaranBulanan} kosong. Tenggat {@code null} berarti tidak ada denda
	 * sama sekali &mdash; kedua mesin denda mensyaratkan tenggat terisi sebelum menghitung.</p>
	 *
	 * @return tenggat pembayaran bawaan; {@code null} bila tidak dapat diturunkan
	 */
	@Transient
	public Date getDefaultTanggalDeadline() {
		detailSettingBiaya = getDetailSettingBiaya();
		jurusan = getJurusan();

		if (jurusan != null && jurusan.getId() != null && detailSettingBiaya != null
				&& detailSettingBiaya.getSettingBiaya() != null
				&& detailSettingBiaya.getSettingBiaya().getTampilkanPerProdi()) {
			defaultTanggalDeadline = detailSettingBiaya.ambilDefaultTanggalDeadline(jurusan);
		} else if (detailSettingBiaya != null && detailSettingBiaya.getSettingBiaya() != null
				&& detailSettingBiaya.getSettingBiaya().getGunakanBiayaDefault()) {
			defaultTanggalDeadline = detailSettingBiaya.getDefaultTanggalDeadline();
		}

		return defaultTanggalDeadline;
	}

	/**
	 * Setter tenggat pembayaran bawaan (hanya di memori; tidak tersimpan).
	 *
	 * @param defaultTanggalDeadline tenggat pembayaran
	 */
	public void setDefaultTanggalDeadline(Date defaultTanggalDeadline) {
		this.defaultTanggalDeadline = defaultTanggalDeadline;
	}
}
