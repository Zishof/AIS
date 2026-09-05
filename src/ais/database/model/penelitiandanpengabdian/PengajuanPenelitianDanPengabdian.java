package ais.database.model.penelitiandanpengabdian;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Model entitas <b>proposal pengajuan</b> penelitian atau pengabdian kepada masyarakat oleh
 * dosen/pegawai ({@link Tbmuser}) atau mahasiswa ({@link Mahasiswa}), terhadap satu skema
 * {@link PenelitianDanPengabdian} yang dibuka. Ini adalah entitas akar (aggregate root) untuk
 * seluruh modul {@code ais.database.model.penelitiandanpengabdian}: anggota tim
 * ({@link AnggotaPengajuanPenelitianDanPengabdian}), lampiran berkas proposal
 * ({@link FilePengajuanPenelitianDanPengabdian}), dan seluruh pengajuan laporan tahap
 * ({@link PengajuanTahapanPelaporanPenelitianDanPengabdian}) merujuk balik ke satu baris kelas ini.
 *
 * <h2>Dua mekanisme status yang berbeda pada modul yang sama</h2>
 * <p>
 * Kelas ini extends {@link DataSop} (bukan {@link ais.database.model.GeneralValueObject} langsung
 * seperti entitas lain di paket ini) sehingga persetujuannya <b>ditautkan ke alur disposisi SOP
 * generik</b> ({@code ais.action.master.sop}, entitas {@code DisposisiSop}/{@code DisposisiAlurSop}
 * /{@code AlurSop}): {@link #getStatus()} dan {@link #getDisetujuiOleh()} <i>menderivasi</i> nilainya
 * dari {@link #getDisposisiSop()}{@code .getDisposisiSetuju()} setiap kali dipanggil — proposal baru
 * benar-benar berstatus {@link #DISETUJUI} bila ada {@code DisposisiAlurSop} yang menandai "selesai"
 * pada rute SOP yang bersangkutan, yang pembuatannya (lewat {@code ProsesDisposisiSopService} atau
 * jalur ZK {@code DisposisiAlurSopAction.onSave}) tunduk pada aturan routing/otorisasi SOP itu
 * sendiri. Ini <b>berbeda arsitektur</b> dari {@link PengajuanTahapanPelaporanPenelitianDanPengabdian}
 * di paket yang sama, yang statusnya sekadar string bebas tanpa keterkaitan workflow apa pun (lihat
 * javadoc kelas tersebut untuk gerbang persetujuan UI-only yang sudah dikonfirmasi di sana).
 * </p>
 *
 * <p>
 * <b>Namun demikian, jalur SOP di atas bukan satu-satunya cara status kelas ini berubah.</b>
 * Tombol admin <b>"Setujui Semua"</b> pada
 * {@code PengajuanPenelitianDanPengabdianHelper} (visible hanya bila
 * {@code Common.getApakahAdmin()} saat render toolbar) memanggil {@link #setStatus(String)} secara
 * langsung dengan {@link #DISETUJUI} — lewat sesi Hibernate native, di luar alur
 * {@code DisposisiSop} — untuk <b>seluruh baris hasil pencarian aktif</b> ({@code initCriteria()}),
 * sekaligus menyapu semua {@link PengajuanTahapanPelaporanPenelitianDanPengabdian} terkait ke status
 * yang sama. Karena {@link #getStatus()} hanya menimpa status ke {@link #DISETUJUI} ketika
 * {@link #getDisetujuiOleh()} bukan {@code null} (dan sebaliknya membiarkan nilai field
 * {@link #status} yang tersimpan apa adanya bila {@link #getDisetujuiOleh()} {@code null}), tulisan
 * langsung oleh tombol ini <b>tidak pernah ditimpa balik</b> oleh derivasi SOP — proposal akan
 * tampil "Disetujui" secara permanen tanpa pernah melewati satu langkah disposisi pun. Pemeriksaan
 * otorisasi ({@code getApakahAdmin()}) untuk tombol ini <b>hanya dievaluasi saat render toolbar</b>,
 * tidak diulang di dalam listener {@code onClick} yang benar-benar mengeksekusi pembaruan massal
 * tersebut — pola yang sama dengan gerbang persetujuan UI-only yang sudah dikonfirmasi pada domain
 * lain di sistem ini (kepegawaian, dua alur persuratan), kini dengan cakupan dampak yang lebih luas
 * karena satu klik dapat menyetujui banyak proposal dan laporan tahap sekaligus.
 * </p>
 *
 * @see DataSop
 * @see AnggotaPengajuanPenelitianDanPengabdian
 * @see FilePengajuanPenelitianDanPengabdian
 * @see PengajuanTahapanPelaporanPenelitianDanPengabdian
 * @see PenelitianDanPengabdian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "penelitiandanpengabdian", name = "pengajuan_penelitian_dan_pengabdian")
public class PengajuanPenelitianDanPengabdian extends DataSop {

	/** Status awal: proposal sudah dibuat tetapi belum masuk proses disposisi/tinjauan apa pun. Nilai default {@link #getStatus()} bila field belum pernah diatur. */
	public static final String BELUM_DIPROSES = "Belum Diproses";
	/** Status transisi: proposal sedang dalam proses disposisi/tinjauan. */
	public static final String SEDANG_DIPROSES = "Sedang Diproses";
	/** Status akhir: proposal ditolak. */
	public static final String DITOLAK = "Ditolak";
	/**
	 * Status akhir: proposal disetujui. Dikembalikan oleh {@link #getStatus()} setiap kali
	 * {@link #getDisetujuiOleh()} tidak {@code null} (derivasi dari alur disposisi SOP), atau bila
	 * field {@link #status} pernah ditulis langsung ke nilai ini lewat {@link #setStatus(String)}
	 * (lihat catatan arsitektur pada javadoc kelas mengenai tombol admin "Setujui Semua").
	 */
	public static final String DISETUJUI = "Disetujui";

	/** Tahap pengajuan proposal awal — nilai default {@link #getTahapPengajuan()} bila field belum diisi. Dipakai pula sebagai acuan default oleh {@link TahapanPelaporanPenelitianDanPengabdian#getTahapPengajuan()}. */
	public static final String TAHAP_PROPOSAL = "Proposal";
	/** Tahap pengajuan: pengumpulan data lapangan/penyebaran kuesioner. */
	public static final String TAHAP_PENGUMPULAN_DATA = "Pengumpulan Data / Sebar Kuesioner";
	/** Tahap pengajuan: analisis data yang telah terkumpul. */
	public static final String TAHAP_ANALISIS_DATA = "Analisis Data";
	/** Tahap pengajuan: penyusunan/penyerahan laporan akhir. */
	public static final String TAHAP_LAPORAN_AKHIR = "Laporan Akhir";

	/**
	 * Membentuk nomor registrasi tampilan untuk proposal ini: {@link #id} diratakan-kanan (padding
	 * nol di kiri) menjadi tepat 7 digit.
	 *
	 * <p>
	 * <b>Catatan risiko:</b> implementasi menempelkan 19 karakter {@code '0'} di depan representasi
	 * teks {@link #id}, lalu mengambil 7 karakter <i>terakhir</i> dari hasilnya
	 * ({@code no.substring(no.length() - 7)}). Ini berfungsi selama {@code id} tidak melebihi 7
	 * digit (di bawah 10 juta); begitu id proposal mencapai 8 digit, digit pertama id akan
	 * terpotong dari nomor registrasi yang ditampilkan (bukan {@code ArrayIndexOutOfBoundsException},
	 * karena panjang string selalu jauh melebihi 7). Bila {@link #id} {@code null} (proposal belum
	 * tersimpan), {@code "0000000000000000000null"} yang diproses, menghasilkan nomor registrasi
	 * {@code "0000nul"} — bukan exception, tetapi nilai yang tidak bermakna.
	 * </p>
	 *
	 * @return nomor registrasi 7 digit hasil padding {@link #id}, atau nilai tidak bermakna bila
	 *         {@link #id} {@code null}
	 */
	public String noreg() {
		String no = "0000000000000000000" + id;
		no = no.substring(no.length() - 7);
		return no;
	}

	/**
	 * Versi kelas untuk kebutuhan serialisasi ({@link java.io.Serializable}). Nilai ini identik
	 * dengan {@code serialVersionUID} pada beberapa entitas lain di paket ini — sisa pola
	 * salin-tempel hbm2java, tidak bermakna khusus.
	 */
	private static final long serialVersionUID = 2463812577548439808L;
	/** Primary key baris proposal, auto-increment ({@code IDENTITY}) pada kolom {@code id}. */
	private Long id;
	/** Field audit legacy: nama pengguna yang melakukan perubahan terakhir (bebas format, isi manual). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Field audit legacy: id/username pengguna yang melakukan perubahan terakhir. Lihat {@link #getOlehId()}. */
	private String olehId;

	/** @return id/username pengguna yang tercatat melakukan perubahan terakhir pada baris ini (field audit legacy, tidak dipetakan sebagai kolom entitas). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pencatat perubahan terakhir. Nilai {@code null} atau string kosong/spasi
	 * diabaikan (tidak menimpa nilai yang sudah tersimpan).
	 *
	 * @param olehId id/username pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pencatat perubahan terakhir. Nilai {@code null} atau string kosong/spasi
	 * diabaikan, dengan alasan yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang tercatat melakukan perubahan terakhir pada baris ini (field audit legacy, tidak dipetakan sebagai kolom entitas). */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence sesaat sebelum
	 * setiap {@code UPDATE} baris ini dieksekusi, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} ke waktu saat ini. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal pengajuan proposal ini ditampilkan ke pengguna. Default waktu pembuatan objek; lihat {@link #getTanggal()} untuk perilaku fallback berantai bila kosong. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengatur cap waktu perubahan terakhir secara manual. Dalam alur normal field ini diperbarui
	 * otomatis lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return cap waktu perubahan terakhir baris ini; diinisialisasi ke waktu pembuatan objek dan diperbarui otomatis oleh {@link #onUpdate()} pada setiap {@code UPDATE}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi teks ringkas proposal ini untuk keperluan log/debug: id, id pengguna
	 *         pengaju ({@code tbmuser}, kosong bila tidak ada/error), NIM pengaju
	 *         ({@code mahasiswa}, kosong bila tidak ada/error), judul skema
	 *         {@link #penelitianDanPengabdian} (kosong bila tidak ada/error), dan {@link #status},
	 *         digabung dengan pemisah {@code "-"} dan {@code "_"}. Setiap bagian dibungkus
	 *         {@code try/catch(Throwable)} sendiri-sendiri agar kegagalan lazy-loading satu relasi
	 *         (mis. sesi Hibernate sudah tertutup) tidak menggagalkan seluruh pembentukan string.
	 */
	public String toString() {
		String tbmuserId = "";
		String mahasiswaId = "";
		String skema = "";
		try {
			tbmuserId = tbmuser == null ? "" : tbmuser.getUserId();
		} catch (Throwable t) {
			tbmuserId = "";
		}
		try {
			mahasiswaId = mahasiswa == null ? "" : mahasiswa.getNim();
		} catch (Throwable t) {
			mahasiswaId = "";
		}
		try {
			skema = penelitianDanPengabdian == null ? "" : penelitianDanPengabdian.getJudul();
		} catch (Throwable t) {
			skema = "";
		}
		return id + "-" + tbmuserId + "_" + mahasiswaId + "_" + skema + "_" + status;
	}

	/** Dosen/pegawai pengaju proposal, bila pengaju seorang {@link Tbmuser} (bukan mahasiswa). Lihat {@link #getTbmuser()} untuk perilaku getter yang saling meniadakan dengan {@link #mahasiswa}. */
	private Tbmuser tbmuser;
	/** Mahasiswa pengaju proposal, bila pengaju seorang {@link Mahasiswa} (bukan dosen/pegawai). Mengisi field ini membuat {@link #getTbmuser()} mengosongkan {@link #tbmuser} pada pemanggilan berikutnya. */
	private Mahasiswa mahasiswa;
	/** Cache tampilan identitas pengaju ("NIM Nama" atau "Nama (userId)"), dibangun ulang setiap {@link #getOlehPenguna()} dipanggil — field ini tidak pernah dibaca sebelum ditimpa. */
	private String olehPenguna;
	/** Daftar anggota tim sebagai teks bebas dipisah koma (username/NIM), sumber untuk membangun ulang baris {@link AnggotaPengajuanPenelitianDanPengabdian} saat proposal disimpan. Lihat {@link #getAnggota()}. */
	private String anggota;
	/** Judul proposal. Lihat {@link #getJudul()}. */
	private String judul;
	/** Kata kunci proposal, wajib diisi menurut validasi form (bukan validasi entitas). Lihat {@link #getKeyword()}. */
	private String keyword;
	/** Tujuan/rumusan masalah proposal, diisi lewat editor kaya (CKEditor). Lihat {@link #getTujuan()}. */
	private String tujuan;
	/** Jumlah dana yang diajukan. Lihat {@link #getJumlahDana()} untuk perilaku default {@code 0.0}. */
	private Double jumlahDana;
	/** Abstrak/keterangan proposal, wajib diisi menurut validasi form. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** URL publik unduhan berkas proposal ({@link FilePengajuanPenelitianDanPengabdian} terkait), dibentuk setelah berkas tersimpan. Lihat {@link #getPathUrl()}. */
	private String pathUrl;
	/** Skema penelitian/pengabdian yang dituju proposal ini (FK wajib). */
	private PenelitianDanPengabdian penelitianDanPengabdian;
	/** Tahap pengajuan proposal saat ini ({@link #TAHAP_PROPOSAL} dkk.). Lihat {@link #getTahapPengajuan()} untuk default. */
	private String tahapPengajuan;

	/** Kode unik turunan (bukan diisi pengguna) yang menggabungkan id proposal, id skema, dan identitas pengaju — dibangun ulang setiap {@link #getKodeUnik()} dipanggil. Dipetakan {@code unique = true} di basis data. */
	private String kodeUnik;

	/** Himpunan sumber dana yang dicentang untuk proposal ini (relasi many-to-many lewat tabel penghubung {@code pengajuan_has_sumber_dana}). Lihat {@link #getSumberDanaPenelitianDanPengabdianes()}. */
	private Set<SumberDanaPenelitianDanPengabdian> sumberDanaPenelitianDanPengabdianes = new HashSet<SumberDanaPenelitianDanPengabdian>();

	/** @return himpunan {@link SumberDanaPenelitianDanPengabdian} yang tercatat untuk proposal ini; tidak pernah {@code null} (diinisialisasi {@link HashSet} kosong bila belum diisi). */
	@ManyToMany(targetEntity = SumberDanaPenelitianDanPengabdian.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinTable(schema = "penelitiandanpengabdian", name = "pengajuan_has_sumber_dana", joinColumns = @JoinColumn(name = "pengajuan"), inverseJoinColumns = @JoinColumn(name = "sumber_dana"))
	public Set<SumberDanaPenelitianDanPengabdian> getSumberDanaPenelitianDanPengabdianes() {
		return sumberDanaPenelitianDanPengabdianes;
	}

	/**
	 * Mengganti seluruh himpunan sumber dana proposal ini. Pemanggil biasa (lihat
	 * {@code PengajuanPenelitianDanPengabdianHelper.onSave}) mengosongkan lalu mengisi ulang
	 * himpunan ini dari checkbox yang dicentang pada form setiap kali disimpan, bukan menambah
	 * secara inkremental.
	 *
	 * @param sumberDanaPenelitianDanPengabdianes himpunan sumber dana pengganti
	 */
	public void setSumberDanaPenelitianDanPengabdianes(
			Set<SumberDanaPenelitianDanPengabdian> sumberDanaPenelitianDanPengabdianes) {
		this.sumberDanaPenelitianDanPengabdianes = sumberDanaPenelitianDanPengabdianes;
	}

	/** Status pengajuan mentah yang tersimpan di basis data ({@link #BELUM_DIPROSES}/{@link #SEDANG_DIPROSES}/{@link #DITOLAK}/{@link #DISETUJUI}). Lihat {@link #getStatus()} untuk bagaimana nilai ini berinteraksi dengan derivasi dari {@link #disposisiSop}. */
	private String status;
	/** Lama pengerjaan yang diajukan (mis. "1 semester", "6 bulan"), teks bebas. Lihat {@link #getMasaPenugasan()} untuk default. */
	private String masaPenugasan;
	/** Daftar editor dan kontributor proposal, teks bebas. Lihat {@link #getEditorDanKontributor()}. */
	private String editorDanKontributor;
	/** Cache waktu persetujuan, diturunkan ulang dari {@link #disposisiSop} setiap {@link #getSetujuiTanggal()} dipanggil — nilai field ini sendiri tidak otoritatif. */
	private Date setujuiTanggal;
	/** Cache dosen/pegawai penyetuju, diturunkan ulang dari {@link #disposisiSop} setiap {@link #getDisetujuiOleh()} dipanggil — nilai field ini sendiri tidak otoritatif (lihat catatan arsitektur pada javadoc kelas). */
	private Tbmuser disetujiOleh;
	/** Cache dosen/pegawai pengaju disposisi (langkah START SOP), diturunkan ulang dari {@link #disposisiSop} setiap {@link #getDiajukanOleh()} dipanggil selama {@link #disposisiSop} tersedia. */
	private Tbmuser diajukanOleh;
	/** Konteks disposisi SOP yang menaungi proposal ini; sumber kebenaran untuk {@link #getStatus()}, {@link #getDisetujuiOleh()}, {@link #getSetujuiTanggal()}, dan {@link #getDiajukanOleh()}. Lihat {@link #setDisposisiSop(DisposisiSop)} untuk aturan "sekali terisi valid, tidak bisa ditimpa/dikosongkan lagi". */
	private DisposisiSop disposisiSop;

	/** Konstruktor default (wajib untuk entitas Hibernate/JPA); seluruh field diisi lewat setter. */
	public PengajuanPenelitianDanPengabdian() {
	}

	/** @return primary key baris proposal ini, atau {@code null} bila belum tersimpan. Dipakai juga oleh {@link #noreg()} dan {@link #getKodeUnik()}. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengatur id baris ini secara manual. Karena kolom {@code id} dipetakan
	 * {@code insertable = false} (nilai dihasilkan basis data lewat {@code IDENTITY}), pengaturan
	 * manual di sini hanya berguna untuk menandai objek yang mewakili baris yang sudah ada.
	 *
	 * @param id primary key yang ingin diasosiasikan ke objek ini
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return abstrak/keterangan proposal, di-trim; string kosong (bukan {@code null}) bila belum diisi. */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan.trim();
	}

	/** @param keterangan abstrak/keterangan baru untuk proposal ini; boleh {@code null}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param penelitianDanPengabdian skema penelitian/pengabdian tujuan proposal ini. */
	public void setPenelitianDanPengabdian(PenelitianDanPengabdian penelitianDanPengabdian) {
		this.penelitianDanPengabdian = penelitianDanPengabdian;
	}

	/** @return lama pengerjaan yang diajukan, di-trim; {@code "1 semester"} dipakai sebagai default bila field belum diisi atau kosong setelah di-trim. */
	public String getMasaPenugasan() {
		return masaPenugasan == null || masaPenugasan.trim().isEmpty() ? "1 semester" : masaPenugasan.trim();
	}

	/** @param masaPenugasan lama pengerjaan baru yang diajukan. */
	public void setMasaPenugasan(String masaPenugasan) {
		this.masaPenugasan = masaPenugasan;
	}

	/** @return skema penelitian/pengabdian yang dituju proposal ini (FK wajib, tidak pernah {@code null} pada baris tersimpan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penelitian_dan_pengabdian", nullable = false)
	public PenelitianDanPengabdian getPenelitianDanPengabdian() {
		return penelitianDanPengabdian;
	}

	/**
	 * <b>Getter destruktif:</b> selain mengembalikan nilai, method ini <b>menulis ulang state</b> —
	 * bila {@link #mahasiswa} sudah terisi (bukan {@code null}), field {@link #tbmuser} langsung
	 * di-{@code null}-kan sebelum dikembalikan, sebagai penegakan aturan "pengaju adalah dosen ATAU
	 * mahasiswa, tidak pernah keduanya" (pola yang sama seperti
	 * {@link AnggotaPengajuanPenelitianDanPengabdian#getTbmuser()}). Efek ini permanen pada instance
	 * yang sedang dipegang. Nilai hasil juga melewati {@code check()} (helper {@link DataSop}) untuk
	 * menahan referensi ke baris {@link Tbmuser} yang sudah tidak ada/tidak valid.
	 *
	 * @return dosen/pegawai pengaju proposal, atau {@code null} bila pengaju mahasiswa atau tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (mahasiswa != null) {
			tbmuser = null;
		}
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Mengatur dosen/pegawai pengaju secara langsung, tanpa mengosongkan {@link #mahasiswa}.
	 * Pemanggil bertanggung jawab memastikan hanya satu dari {@code tbmuser}/{@code mahasiswa} yang
	 * bermakna untuk proposal ini — pengosongan silang hanya terjadi lewat {@link #getTbmuser()}.
	 *
	 * @param tbmuser dosen/pegawai yang mengajukan proposal; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * @return mahasiswa pengaju proposal, atau {@code null} bila pengaju dosen/pegawai atau tidak
	 *         diisi. Getter ini juga melewati {@code check()} (menahan referensi ke baris
	 *         {@link Mahasiswa} yang sudah tidak ada/tidak valid), tetapi <b>tidak</b> mengosongkan
	 *         {@link #tbmuser} — pengosongan silang hanya terjadi lewat {@link #getTbmuser()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Mengatur mahasiswa pengaju. Mengisi field ini dengan nilai bukan-{@code null} akan membuat
	 * pemanggilan {@link #getTbmuser()} berikutnya mengosongkan {@link #tbmuser}.
	 *
	 * @param mahasiswa mahasiswa yang mengajukan proposal; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Status pengajuan proposal ini, yang <b>menderivasi</b> nilainya dari alur disposisi SOP setiap
	 * kali dipanggil: bila {@link #getDisetujuiOleh()} tidak {@code null} (ada
	 * {@code DisposisiAlurSop} yang menandai "selesai/setuju" pada {@link #disposisiSop}), field
	 * {@link #status} ditulis-ulang paksa menjadi {@link #DISETUJUI} — mengabaikan apa pun nilai
	 * yang tersimpan sebelumnya, termasuk {@link #DITOLAK}. Sebaliknya, ketika
	 * {@link #getDisetujuiOleh()} {@code null}, method ini <b>hanya</b> menormalisasi {@code null}
	 * menjadi {@link #BELUM_DIPROSES} — nilai apa pun yang sudah tersimpan pada {@link #status}
	 * (termasuk {@link #DISETUJUI} yang ditulis langsung lewat {@link #setStatus(String)} tanpa
	 * pernah melalui disposisi SOP, mis. oleh tombol admin "Setujui Semua"; lihat javadoc kelas)
	 * dikembalikan apa adanya, tanpa dikoreksi. Dengan kata lain: derivasi SOP dapat MENAIKKAN status
	 * ke Disetujui, tetapi tidak pernah MENURUNKANNYA kembali bila field mentah sudah terlanjur
	 * berisi Disetujui dari jalur lain.
	 *
	 * @return status proposal saat ini; lihat catatan derivasi di atas untuk kapan nilai ini benar-benar berasal dari alur disposisi SOP dan kapan hanya dari field mentah
	 */
	public String getStatus() {

		if (getDisetujuiOleh() != null) {
			status = DISETUJUI;
		} else {
			if (status == null) {
				status = BELUM_DIPROSES;
			}
		}
		return status;
	}

	/**
	 * Mengatur status pengajuan secara langsung, tanpa validasi nilai maupun keterkaitan ke
	 * {@link #disposisiSop}. Lihat javadoc {@link #getStatus()} untuk bagaimana nilai yang ditulis
	 * di sini berinteraksi (atau tidak) dengan derivasi dari alur disposisi SOP pada pembacaan
	 * berikutnya.
	 *
	 * @param status status baru, idealnya salah satu dari {@link #BELUM_DIPROSES}/{@link #SEDANG_DIPROSES}/{@link #DITOLAK}/{@link #DISETUJUI}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return URL publik unduhan berkas proposal terbaru, atau {@code null} bila belum ada berkas yang diunggah/URL belum dibentuk. */
	@Column(name = "path_url", length = 1000)
	public String getPathUrl() {
		return pathUrl;
	}

	/** @param pathUrl URL publik unduhan berkas proposal terbaru. */
	public void setPathUrl(String pathUrl) {
		this.pathUrl = pathUrl;
	}

	/** @return daftar anggota tim sebagai teks bebas dipisah koma (username/NIM); string kosong (bukan {@code null}, dan ditulis-balik ke field) bila belum diisi. */
	@Column(name = "anggota", columnDefinition = "text")
	public String getAnggota() {
		if (anggota == null) {
			anggota = "";
		}
		return anggota;
	}

	/** @param anggota daftar anggota tim baru sebagai teks bebas dipisah koma (username/NIM). */
	public void setAnggota(String anggota) {
		this.anggota = anggota;
	}

	/** @return jumlah dana yang diajukan; {@code 0.0} dipakai sebagai default (dan ditulis-balik ke field) bila belum diisi — tidak pernah {@code null}. */
	public Double getJumlahDana() {
		if (jumlahDana == null) {
			jumlahDana = 0.0;
		}
		return jumlahDana;
	}

	/** @param jumlahDana jumlah dana baru yang diajukan. */
	public void setJumlahDana(Double jumlahDana) {
		this.jumlahDana = jumlahDana;
	}

	/** @return tujuan/rumusan masalah proposal, apa adanya (boleh {@code null}). */
	@Column(name = "tujuan", columnDefinition = "text")
	public String getTujuan() {
		return tujuan;
	}

	/** @param tujuan tujuan/rumusan masalah baru untuk proposal ini. */
	public void setTujuan(String tujuan) {
		this.tujuan = tujuan;
	}

	/** @return judul proposal, apa adanya (boleh {@code null}). */
	@Column(name = "judul", columnDefinition = "text")
	public String getJudul() {
		return judul;
	}

	/** @param judul judul baru untuk proposal ini. */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Membangun (dan menyimpan ke field {@link #olehPenguna}, meski field itu tidak pernah dibaca
	 * ulang sebelum ditimpa) representasi tampilan identitas pengaju: "{@code NIM Nama}" bila
	 * pengaju mahasiswa, atau "{@code Nama (userId)}" bila pengaju dosen/pegawai. Memanggil
	 * {@link #getMahasiswa()}/{@link #getTbmuser()} sehingga ikut memicu efek samping
	 * getter-destruktif keduanya (lihat javadoc masing-masing).
	 *
	 * @return teks identitas pengaju siap tampil, atau string kosong bila tidak ada pengaju yang valid
	 */
	public String getOlehPenguna() {
		olehPenguna = "";
		if (getMahasiswa() != null) {
			olehPenguna = (getMahasiswa().getNim() + " " + getMahasiswa().getNama());
		} else if (getTbmuser() != null) {
			olehPenguna = (getTbmuser().getUserNama() + " (" + getTbmuser().getUserId() + ")");
		}
		return olehPenguna;
	}

	/** @param olehPenguna teks identitas pengaju; akan ditimpa ulang pada pemanggilan {@link #getOlehPenguna()} berikutnya, sehingga setter ini tidak berefek jangka panjang. */
	public void setOlehPenguna(String olehPenguna) {
		this.olehPenguna = olehPenguna;
	}

	/**
	 * Getter dengan fallback berantai: bila {@link #tanggal} kosong, dicoba diisi dari
	 * {@link #getTanggal_dirubah()} (cap waktu perubahan terakhir); bila masih kosong (baris baru
	 * yang belum pernah di-{@code UPDATE}), diisi dengan waktu saat ini. Nilai hasil fallback ditulis
	 * balik ke field {@link #tanggal}.
	 *
	 * @return tanggal pengajuan proposal ini; tidak pernah {@code null}
	 */
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = getTanggal_dirubah();
		}
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/** @param tanggal tanggal pengajuan baru untuk proposal ini. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** @return tahap pengajuan proposal saat ini; {@link #TAHAP_PROPOSAL} dipakai sebagai default bila field belum diisi atau kosong setelah di-trim. */
	public String getTahapPengajuan() {
		return tahapPengajuan == null || tahapPengajuan.trim().isEmpty() ? TAHAP_PROPOSAL : tahapPengajuan;
	}

	/** @param tahapPengajuan tahap pengajuan baru untuk proposal ini. */
	public void setTahapPengajuan(String tahapPengajuan) {
		this.tahapPengajuan = tahapPengajuan;
	}

	/** @return kata kunci proposal, di-trim; string kosong (bukan {@code null}) bila belum diisi. */
	public String getKeyword() {
		return keyword == null ? "" : keyword.trim();
	}

	/** @param keyword kata kunci baru untuk proposal ini. */
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	/**
	 * Membangun (dan menyimpan ke field {@link #kodeUnik}) kode unik turunan yang menggabungkan id
	 * proposal, id skema {@link #penelitianDanPengabdian}, dan identitas pengaju
	 * ({@code "usr_" + userId} atau {@code "mhs_" + idMahasiswa}) — format:
	 * {@code "<id>_<idSkema>-usr_<userId>"} atau {@code "<id>_<idSkema>-mhs_<idMahasiswa>"}. Bila
	 * {@link #penelitianDanPengabdian} belum diisi, atau kedua {@link #tbmuser}/{@link #mahasiswa}
	 * kosong, method ini menuliskan {@code null} ke field lalu mengembalikan {@code null}. Kolom ini
	 * dipetakan {@code unique = true}, sehingga dua proposal dengan skema dan pengaju yang identik
	 * akan gagal disimpan bersamaan (constraint violation) — mencegah pengajuan ganda oleh pengaju
	 * yang sama untuk skema yang sama.
	 *
	 * @return kode unik proposal ini, atau {@code null} bila data pembentuknya belum lengkap
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (penelitianDanPengabdian != null && (tbmuser != null || mahasiswa != null)) {
			kodeUnik = getId() + "_" + penelitianDanPengabdian.getId() + "-"
					+ (tbmuser != null ? "usr_" + tbmuser.getUserId()
							: mahasiswa != null ? "mhs_" + mahasiswa.getId() : "");
		} else {
			kodeUnik = null;
		}
		return kodeUnik;
	}

	/**
	 * Mengatur kode unik secara manual. Karena {@link #getKodeUnik()} selalu membangun ulang nilai
	 * ini dari field lain setiap dipanggil, pengaturan manual di sini hanya bertahan sampai
	 * pemanggilan {@link #getKodeUnik()} berikutnya.
	 *
	 * @param kodeUnik kode unik yang ingin diasosiasikan sementara ke objek ini
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/** @return daftar editor dan kontributor proposal, di-trim; string kosong (bukan {@code null}) bila belum diisi. */
	@Column(columnDefinition = "text")
	public String getEditorDanKontributor() {
		return editorDanKontributor == null ? "" : editorDanKontributor.trim();
	}

	/** @param editorDanKontributor daftar editor dan kontributor baru untuk proposal ini. */
	public void setEditorDanKontributor(String editorDanKontributor) {
		this.editorDanKontributor = editorDanKontributor;
	}

	/**
	 * Getter turunan sekaligus cache tertulis: menurunkan dosen/pegawai penyetuju dari
	 * {@link #disposisiSop}{@code .getDisposisiSetuju().getDiajukanOleh()} bila tersedia (langkah
	 * disposisi yang menandai "selesai/setuju" sudah ada dan diajukan oleh seseorang), menuliskannya
	 * ke field {@link #disetujiOleh}. Bila {@link #disposisiSop} tersedia tetapi belum ada langkah
	 * "setuju" (atau langkah tersebut ada namun tanpa pengaju), field {@link #disetujiOleh}
	 * <b>dikosongkan paksa</b> ke {@code null} — mencegah nilai basi (stale) bertahan dari state
	 * sebelumnya. Namun bila {@link #disposisiSop} itu sendiri {@code null} (proposal belum/tidak
	 * pernah masuk alur disposisi SOP), tidak ada cabang di atas yang tereksekusi sehingga nilai
	 * {@link #disetujiOleh} yang sudah ada (mis. hasil {@code load} dari basis data, atau diisi
	 * manual lewat {@link #setDisetujuiOleh(Tbmuser)}) dikembalikan apa adanya, hanya melewati
	 * {@code check()} (menahan referensi ke baris {@link Tbmuser} yang sudah tidak ada/tidak valid).
	 * Inilah dasar dari {@link #getStatus()}: proposal dianggap {@link #DISETUJUI} persis ketika
	 * method ini mengembalikan bukan-{@code null}.
	 *
	 * @return dosen/pegawai yang menyetujui proposal ini (secara langsung atau via disposisi SOP), atau {@code null} bila belum disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetuji_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujiOleh = check(disetujiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujiOleh = null;
		}

		return disetujiOleh;
	}

	/**
	 * Mengatur dosen/pegawai penyetuju secara langsung. Berguna terutama saat
	 * {@link #disposisiSop} belum diisi/tidak dipakai (proposal lama sebelum alur SOP diterapkan,
	 * atau jalur import data) — begitu {@link #disposisiSop} terisi dan memiliki langkah "setuju",
	 * nilai yang diatur di sini akan ditimpa oleh {@link #getDisetujuiOleh()} pada pemanggilan
	 * berikutnya.
	 *
	 * @param disetujiOleh dosen/pegawai penyetuju yang ingin diatur langsung
	 */
	public void setDisetujuiOleh(Tbmuser disetujiOleh) {
		this.disetujiOleh = disetujiOleh;
	}

	/**
	 * Getter turunan sekaligus cache tertulis untuk waktu persetujuan, mengikuti logika yang sama
	 * persis dengan {@link #getDisetujuiOleh()}: diisi dari
	 * {@link #disposisiSop}{@code .getDisposisiSetuju().getWaktu()} bila langkah "setuju" pada
	 * disposisi SOP tersedia, dikosongkan paksa bila {@link #disposisiSop} ada tetapi belum/tidak
	 * ada langkah "setuju", atau dikembalikan apa adanya bila {@link #disposisiSop} {@code null}.
	 *
	 * @return waktu persetujuan proposal ini, atau {@code null} bila belum disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSetujuiTanggal() {

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			setujuiTanggal = getDisposisiSop().getDisposisiSetuju().getWaktu();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			setujuiTanggal = null;
		}

		return setujuiTanggal;
	}

	/**
	 * Mengatur waktu persetujuan secara langsung. Sama seperti {@link #setDisetujuiOleh(Tbmuser)},
	 * nilai ini akan ditimpa oleh {@link #getSetujuiTanggal()} begitu {@link #disposisiSop} terisi
	 * dan memiliki langkah "setuju".
	 *
	 * @param setujuiTanggal waktu persetujuan yang ingin diatur langsung
	 */
	public void setSetujuiTanggal(Date setujuiTanggal) {
		this.setujuiTanggal = setujuiTanggal;
	}

	/**
	 * Getter turunan sekaligus cache tertulis untuk pengaju disposisi: diisi dari
	 * {@link #disposisiSop}{@code .getDisposisiStart().getDiajukanOleh()} bila langkah awal (START)
	 * disposisi SOP tersedia dan memiliki pengaju. Berbeda dengan {@link #getDisetujuiOleh()}/
	 * {@link #getSetujuiTanggal()}, method ini <b>tidak</b> mengosongkan paksa {@link #diajukanOleh}
	 * ketika langkah START belum ada — nilai yang sudah tersimpan (mis. diisi manual lewat
	 * {@link #setDiajukanOleh(Tbmuser)} saat form pertama kali dibuat, lihat
	 * {@code PengajuanPenelitianDanPengabdianHelper.form}) tetap dipertahankan apa adanya sampai
	 * disposisi SOP benar-benar memberikan nilai pengganti.
	 *
	 * @return dosen/pegawai yang mengajukan proposal ini (langkah START disposisi, atau nilai yang diatur manual sebelum disposisi dibuat), atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Tbmuser getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			diajukanOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}

		return diajukanOleh;
	}

	/**
	 * Mengatur dosen/pegawai pengaju secara langsung. Dipakai antara lain saat form proposal
	 * pertama kali dibuka untuk dosen yang login (sebelum {@link #disposisiSop} tentu sudah ada) —
	 * lihat {@code PengajuanPenelitianDanPengabdianHelper.form}.
	 *
	 * @param diajukanOleh dosen/pegawai pengaju yang ingin diatur langsung
	 */
	public void setDiajukanOleh(Tbmuser diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	/** @return konteks disposisi SOP yang menaungi proposal ini, atau {@code null} bila proposal belum/tidak pernah masuk alur disposisi SOP. Melewati {@code check()} untuk menahan referensi ke baris {@code DisposisiSop} yang sudah tidak ada/tidak valid. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Mengatur konteks disposisi SOP untuk proposal ini — dengan <b>penjagaan satu-arah</b>: bila
	 * {@code disposisiSop} yang diberikan {@code null} atau belum tersimpan ({@code getId() == null}),
	 * method ini langsung {@code return} tanpa melakukan apa pun (baris kedua yang tampak
	 * "membandingkan" kondisi yang sama tidak pernah tereksekusi dengan {@code disposisiSop} yang
	 * lolos guard pertama). Efek praktisnya: setelah {@link #disposisiSop} pernah terisi dengan
	 * disposisi yang valid (punya id), <b>tidak ada cara mengosongkannya kembali ke {@code null}
	 * atau menggantinya ke disposisi yang belum tersimpan</b> lewat setter ini — hanya penggantian
	 * ke disposisi valid lain yang mungkin (dan itu pun tidak pernah menolak penggantian karena
	 * kondisi kedua selalu bernilai true untuk disposisi valid). Penjagaan ini mencegah
	 * penghapusan/pemutusan tautan proposal dari riwayat disposisinya secara tidak sengaja.
	 *
	 * @param disposisiSop disposisi SOP baru untuk proposal ini; diabaikan sepenuhnya bila {@code null} atau belum tersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}
}
