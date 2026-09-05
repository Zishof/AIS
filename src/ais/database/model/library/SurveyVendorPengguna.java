package ais.database.model.library;

// Pengguna (individu) yang di-assign pada satu survey: penilai (boleh menilai) atau pengamat (lihat saja).
// Akses survey dibatasi hanya ke pengguna yang terdaftar di sini (kecuali bolehLihatSemua).

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Entitas <b>penugasan pengguna</b> (tabel {@code library.survey_vendor_pengguna}) pada satu
 * {@link SurveyVendor} — satu baris menandai bahwa satu {@link Tbmuser} diberi peran
 * {@link #PENILAI} (boleh mengisi penilaian, lihat {@link SurveyVendorPenilaian}) atau
 * {@link #PENGAMAT} (hanya melihat) pada survei tersebut.
 *
 * <h2>Akses survei: ditegakkan lewat peran staf, bukan lewat {@code bolehLihatSemua}</h2>
 * <p>Komentar berkas ini menyatakan niatnya: akses ke satu survei dibatasi hanya untuk pengguna
 * yang terdaftar sebagai baris {@code SurveyVendorPengguna} pada survei tersebut, kecuali
 * {@link #getBolehLihatSemua()} bernilai {@code true}. <b>Namun pembacaan {@code SurveyVendorAction}
 * menunjukkan pengecualian ini tidak seperti itu diimplementasikan.</b>
 * {@code daftarSurvey()} membedakan hanya dua jalur: pengguna dengan privilese staf (flag
 * {@code isStaf}, dari {@code isAdministrator()} atau {@code CommonPrivilages.CREATE}) melihat
 * <b>seluruh</b> survei tanpa syarat apa pun, sedangkan pengguna non-staf hanya melihat survei
 * tempat dirinya terdaftar sebagai {@code SurveyVendorPengguna} — query penyaringnya
 * ({@code Restrictions.eq("pengguna", user)}) sama sekali tidak menyebut {@link #bolehLihatSemua}.
 * Bidang ini <b>dibaca dan ditulis</b> pada form penugasan (checkbox yang menampilkan/menyimpan
 * nilainya), namun tidak pernah dibaca di jalur mana pun yang memfilter visibilitas survei atau
 * penilaian. Ini instance tambahan dari pola flag yang tersimpan dan dapat diedit tetapi tidak
 * benar-benar menggerbangi apa pun — jangan berasumsi mencentang "boleh lihat semua" pada
 * penugasan memberi pengguna non-staf akses ke data di luar surveinya sendiri, dan jangan pula
 * berasumsi mengosongkannya membatasi staf.</p>
 *
 * <h2>Flag notifikasi/progres satu arah</h2>
 * <p>{@link #getSudahNotifikasi()} dan {@link #getSudahMenilai()} adalah penanda progres yang
 * hanya ditulis oleh {@code SurveyVendorAction} pada titik tertentu (setelah notifikasi terkirim,
 * setelah penilaian pengguna ini tersimpan lengkap) — kelas ini sendiri tidak memiliki logika
 * yang mengesetnya otomatis maupun yang membalikkannya bila penilaian terkait dibatalkan/dihapus,
 * sehingga {@code sudahMenilai} yang sudah {@code true} berpotensi menjadi tidak sinkron dengan
 * keberadaan baris {@link SurveyVendorPenilaian} yang sesungguhnya bila baris penilaian dihapus
 * lewat jalur lain (mis. CRUD generik) tanpa turut membalikkan flag ini.</p>
 *
 * @see SurveyVendor
 * @see SurveyVendorPenilaian
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_pengguna")
public class SurveyVendorPengguna extends GeneralValueObject {

	/** Penanda versi serialisasi Java. */
	private static final long serialVersionUID = 7720145511001000004L;

	/** Peran: pengguna boleh mengisi penilaian — bawaan {@link #getPeran()}. */
	public static final String PENILAI = "Penilai";
	/** Peran: pengguna hanya dapat melihat, tidak mengisi penilaian. */
	public static final String PENGAMAT = "Pengamat";

	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris penugasan ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() { return olehId; }
	/**
	 * Menyetel id pengguna yang terakhir mengubah baris penugasan ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) { return; } this.olehId = olehId; }
	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris penugasan ini.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) { return; } this.oleh = oleh; }
	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris penugasan ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() { return oleh; }

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Method sengaja
	 * {@code protected} dan tidak boleh dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris penugasan ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru di memori
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Representasi teks singkat: nama pengguna yang ditugaskan.
	 *
	 * @return nama pengguna ({@code Tbmuser.getUserNama()}), atau string kosong bila
	 *         {@link #pengguna} belum ditautkan
	 */
	public String toString() { return pengguna == null ? "" : pengguna.getUserNama(); }

	/** Header survei tempat pengguna ini ditugaskan. */
	private SurveyVendor surveyVendor;
	/** Pengguna ({@link Tbmuser}) yang ditugaskan sebagai penilai/pengamat. */
	private Tbmuser pengguna;
	/** Peran pengguna pada survei ini; salah satu {@link #PENILAI}/{@link #PENGAMAT} — bawaan {@link #PENILAI}. */
	private String peran;
	/** Penanda "boleh lihat semua"; lihat javadoc kelas — bidang ini tidak dipakai memfilter visibilitas mana pun saat ini. */
	private Boolean bolehLihatSemua;
	/** Penanda notifikasi survei aktif sudah dikirim ke pengguna ini; ditulis {@code SurveyVendorAction.aktifkan(...)}. */
	private Boolean sudahNotifikasi;
	/** Penanda pengguna ini sudah menyelesaikan penilaiannya; ditulis {@code SurveyVendorAction} setelah simpan penilaian, tidak pernah dibalik otomatis. */
	private Boolean sudahMenilai;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SurveyVendorPengguna() {}

	/**
	 * Mengembalikan kunci utama baris penugasan ini.
	 *
	 * @return id, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/**
	 * Menyetel kunci utama baris penugasan ini. Hanya untuk kebutuhan Hibernate/penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Mengembalikan header survei tempat pengguna ini ditugaskan.
	 *
	 * <p>Tidak memanggil {@code check(...)} karena relasi ini tidak dinyatakan {@code LAZY} pada
	 * anotasi (bawaan JPA untuk {@code @ManyToOne} adalah <i>eager</i>).</p>
	 *
	 * @return header survei; boleh {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "survey_vendor", nullable = true)
	public SurveyVendor getSurveyVendor() { return surveyVendor; }
	/**
	 * Menyetel header survei tempat pengguna ini ditugaskan.
	 *
	 * @param v header survei; boleh {@code null}
	 */
	public void setSurveyVendor(SurveyVendor v) { this.surveyVendor = v; }

	/**
	 * Mengembalikan pengguna yang ditugaskan, setelah proksi malasnya diselesaikan
	 * {@code check(...)}.
	 *
	 * @return pengguna yang ditugaskan; boleh {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengguna", nullable = true)
	public Tbmuser getPengguna() { pengguna = check(pengguna); return pengguna; }
	/**
	 * Menyetel pengguna yang ditugaskan.
	 *
	 * @param pengguna pengguna yang ditugaskan; boleh {@code null}
	 */
	public void setPengguna(Tbmuser pengguna) { this.pengguna = pengguna; }

	/**
	 * Mengembalikan peran pengguna pada survei ini, dengan bawaan {@link #PENILAI} bila belum
	 * diisi.
	 *
	 * @return salah satu {@link #PENILAI} atau {@link #PENGAMAT}; tidak pernah {@code null}
	 */
	@Column(name = "peran", length = 30) public String getPeran() { return peran == null ? PENILAI : peran; }
	/**
	 * Menyetel peran pengguna pada survei ini.
	 *
	 * @param peran sebaiknya salah satu {@link #PENILAI}/{@link #PENGAMAT}; boleh {@code null}
	 *              untuk kembali ke bawaan {@link #PENILAI}
	 */
	public void setPeran(String peran) { this.peran = peran; }

	/**
	 * Mengembalikan penanda "boleh lihat semua", dengan {@code null} dinormalkan menjadi
	 * {@code false}.
	 *
	 * <p><b>Perhatian:</b> flag ini ditampilkan dan disimpan lewat form penugasan, namun — lihat
	 * javadoc kelas — tidak dibaca oleh {@code SurveyVendorAction.daftarSurvey()} atau jalur
	 * penyaring lain manapun yang ditemukan. Jangan mengandalkannya sebagai gerbang akses.</p>
	 *
	 * @return {@code true} bila ditandai boleh lihat semua; {@code false} bila tidak/belum diisi
	 */
	@Column(name = "boleh_lihat_semua") public Boolean getBolehLihatSemua() { return bolehLihatSemua != null && bolehLihatSemua; }
	/**
	 * Menyetel penanda "boleh lihat semua".
	 *
	 * @param v penanda baru; boleh {@code null}
	 */
	public void setBolehLihatSemua(Boolean v) { this.bolehLihatSemua = v; }

	/**
	 * Mengembalikan penanda notifikasi survei aktif sudah dikirim ke pengguna ini, dengan
	 * {@code null} dinormalkan menjadi {@code false}.
	 *
	 * <p>Ditulis {@code true} oleh {@code SurveyVendorAction.aktifkan(SurveyVendor)} untuk seluruh
	 * baris penugasan survei yang bersangkutan tepat saat survei diaktifkan; tidak ada logika yang
	 * membalikkannya.</p>
	 *
	 * @return {@code true} bila notifikasi sudah dikirim; {@code false} bila belum/tidak diketahui
	 */
	@Column(name = "sudah_notifikasi") public Boolean getSudahNotifikasi() { return sudahNotifikasi != null && sudahNotifikasi; }
	/**
	 * Menyetel penanda notifikasi survei sudah dikirim.
	 *
	 * @param v penanda baru; boleh {@code null}
	 */
	public void setSudahNotifikasi(Boolean v) { this.sudahNotifikasi = v; }

	/**
	 * Mengembalikan penanda pengguna ini sudah menyelesaikan penilaiannya, dengan {@code null}
	 * dinormalkan menjadi {@code false}.
	 *
	 * <p>Ditulis {@code true} oleh {@code SurveyVendorAction} tepat setelah seluruh sel skor
	 * {@link SurveyVendorPenilaianDetail} pengguna ini tersimpan. Tidak ada logika — baik di kelas
	 * ini maupun yang ditemukan di action-nya — yang membalikkan flag ini bila baris
	 * {@link SurveyVendorPenilaian}/{@link SurveyVendorPenilaianDetail} terkait kemudian
	 * dihapus lewat jalur lain (mis. CRUD generik); flag dapat menjadi tidak sinkron dengan
	 * keberadaan data penilaian yang sesungguhnya.</p>
	 *
	 * @return {@code true} bila sudah menilai; {@code false} bila belum/tidak diketahui
	 */
	@Column(name = "sudah_menilai") public Boolean getSudahMenilai() { return sudahMenilai != null && sudahMenilai; }
	/**
	 * Menyetel penanda pengguna ini sudah menyelesaikan penilaiannya.
	 *
	 * @param v penanda baru; boleh {@code null}
	 */
	public void setSudahMenilai(Boolean v) { this.sudahMenilai = v; }
}
