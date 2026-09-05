package ais.database.model.penelitiandanpengabdian;

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




import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate/JPA (tabel {@code penelitiandanpengabdian.pengumuman_penelitian})
 * yang merepresentasikan satu pengumuman/berita papan informasi pada modul
 * Penelitian dan Pengabdian Masyarakat (mis. pengumuman jadwal hibah, hasil
 * seleksi proposal, dsb). Dikelola lewat
 * {@code ais.action.master.penelitiandanpengabdian.PengumumanPenelitianAction}
 * (lihat javadoc kelas tersebut untuk alur simpan &amp; notifikasi email
 * lengkap) dan ditampilkan dengan dua sub-bagian lewat
 * {@code ais.action.master.helper.DetailPengumumanPenelitianHelper}: thread
 * diskusi/komentar ({@link ais.database.model.DiskusiPengumumanPenelitian},
 * relasi FK {@code pengumumanPenelitian} pada sisi anak) dan lampiran file
 * ({@link ais.database.model.file.LampiranPengumumanPenelitian}, FK serupa;
 * kelas lampiran tersebut SUDAH selesai didokumentasikan pada paket
 * {@code ais.database.model.file}, tidak diedit ulang di sini).
 * <p>
 * Jangkauan tayang diatur lewat rentang tanggal {@link #getTanggal()}
 * (mulai) sampai {@link #getSampai()} (akhir), status {@link #getAktif()}
 * (default aktif bila belum diset — SAMA seperti pola di
 * {@link ais.database.model.penelitiandanpengabdian.JurnalPenelitian#getAktif()},
 * BUKAN pola {@code getAktif()} terbalik yang ditemukan pada
 * {@code InformasiPerpustakaan}/{@code ItemPunyaTerbit} di sesi sebelumnya —
 * di sini {@code true} tetap berarti aktif apa adanya), dan
 * {@link #getBolehDiberiKomentar()} yang mengizinkan/menutup thread diskusi
 * (default mengizinkan bila belum diset, pola lazy-default yang sama).
 * CATATAN: entitas ini sendiri hanya menyimpan flag-flag tersebut sebagai
 * data; penegakan gerbang tayang/publikasi berdasarkan rentang tanggal &amp;
 * {@code aktif} (mis. pada layar publik) ada di lapisan pemanggil
 * (Action/servlet), bukan di kelas ini.
 * <p>
 * Daftar penerima notifikasi email disimpan di {@link #getKorespondensi()}
 * sebagai satu string userId dipisah koma — pola identik dengan
 * {@link ais.database.model.penelitiandanpengabdian.JurnalPenelitian#getKorespondensi()}.
 * Diaudit lewat Hibernate Envers ({@code @Audited}).
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "penelitiandanpengabdian", name = "pengumuman_penelitian")



public class PengumumanPenelitian extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 * Nilainya sama persis dengan konstanta di banyak entitas lain hasil
	 * hbm2java pada generasi kode yang sama (mis.
	 * {@link ais.database.model.penelitiandanpengabdian.JurnalPenelitian}) —
	 * kebetulan template generator, bukan indikasi relasi/warisan.
	 */
	private static final long serialVersionUID = 2463822571548439808L;
	private Long id;

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat
	 * sebelum UPDATE, mendelegasikan pencatatan waktu ubah ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}. Field {@link #tanggal_dirubah}
	 * sendiri diinisialisasi eager lewat {@link ais.ui.util.WaktuUtil#getDate()}
	 * saat objek dibuat — field audit shadow standar di banyak entitas AIS,
	 * KEHARUSAN TEKNIS untuk penjejakan waktu ubah, bukan bug.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel timestamp terakhir diubah secara manual. Umumnya tidak perlu
	 * dipanggil langsung oleh kode aplikasi karena {@link #onUpdate()} sudah
	 * mengisinya otomatis pada setiap UPDATE lewat Hibernate.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp terakhir kali record ini diubah (kolom
	 * {@code @Temporal(TIMESTAMP)}), diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string entitas ini untuk keperluan tampilan ringkas —
	 * mengembalikan {@link #catatan} (isi pengumuman) apa adanya, BUKAN
	 * {@link #judul} seperti kebiasaan umum entitas lain di paket ini (mis.
	 * {@link ais.database.model.penelitiandanpengabdian.JurnalPenelitian#toString()}
	 * mengembalikan judul) — perhatikan perbedaan ini bila kelas ini dipakai
	 * pada komponen UI yang mengandalkan {@code toString()} untuk label
	 * ringkas (isinya bisa berupa HTML CKEditor yang panjang, bukan judul
	 * singkat). Tidak ada null-check: mengembalikan {@code null} literal
	 * bila catatan belum diisi.
	 *
	 * @return isi catatan/pengumuman, atau {@code null} jika belum diisi
	 */
	public String toString() {
		return catatan;
	}

	/** Lihat javadoc {@link #getJudul()}. */
	private String judul;
	/** Lihat javadoc {@link #getCatatan()}. */
	private String catatan;
	/** Lihat javadoc {@link #getOleh()}. */
	private String oleh;
	/** Lihat javadoc {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan userId pembuat/pengelola pengumuman ini, terpisah dari
	 * nama tampilan {@link #getOleh()}.
	 *
	 * @return userId pembuat, atau {@code null} jika belum diset
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel {@link #olehId}. Menolak (no-op, silent) nilai {@code null}
	 * atau string kosong/whitespace — begitu terisi valid, tidak akan
	 * tertimpa kosong oleh pemanggilan berikutnya (pola berulang yang sama
	 * pada pasangan {@code oleh}/{@code olehId} di banyak entitas lain).
	 *
	 * @param olehId userId pembuat; diabaikan jika {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Lihat javadoc {@link #getTanggal()}. */
	private Date tanggal;
	/** Lihat javadoc {@link #getSampai()}. */
	private Date sampai;
	/** Lihat javadoc {@link #getAktif()}. */
	private Boolean aktif;
	/** Lihat javadoc {@link #getBolehDiberiKomentar()}. */
	private Boolean bolehDiberiKomentar;
	/** Lihat javadoc {@link #getKorespondensi()}. */
	private String korespondensi;

	/** Konstruktor default (dibutuhkan Hibernate); tidak menyetel field apa pun. */
	public PengumumanPenelitian() {
	}

	/**
	 * Primary key auto-increment (identity) tabel {@code pengumuman_penelitian}.
	 * Anotasi {@code insertable = false} berarti kolom ini tidak pernah
	 * disertakan Hibernate pada statement INSERT eksplisit — nilainya
	 * sepenuhnya diserahkan ke mekanisme {@code IDENTITY} pada database.
	 *
	 * @return id primary key, {@code null} untuk instance yang belum
	 *         disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel id. Dipakai Hibernate sendiri saat memuat entitas dari DB;
	 * jarang perlu dipanggil manual oleh kode aplikasi karena kolomnya
	 * {@code insertable = false} (lihat {@link #getId()}).
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Judul pengumuman, kolom wajib diisi ({@code nullable = false}) dengan
	 * tipe {@code text} (tanpa batas panjang eksplisit, berbeda dari
	 * {@link ais.database.model.penelitiandanpengabdian.JurnalPenelitian#getJudul()}
	 * yang dibatasi 1000 karakter).
	 *
	 * @return judul pengumuman
	 */
	@Column(name = "judul", nullable = false, columnDefinition="text")
	public String getJudul() {
		return this.judul;
	}

	/**
	 * Menyetel judul pengumuman.
	 *
	 * @param judul judul baru
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Isi lengkap pengumuman, kolom wajib diisi ({@code nullable = false})
	 * bertipe {@code text}. Diisi lewat editor kaya-teks (CKEditor) di UI
	 * pengelolaan ({@code PengumumanPenelitianAction}), sehingga nilainya
	 * berupa markup HTML, bukan teks polos — dirender sebagai HTML pada
	 * layar detail (lihat pemakaian {@code ais.ui.util.MyHtml} pada helper
	 * tampilan terkait). Juga dikembalikan oleh {@link #toString()}.
	 *
	 * @return isi/catatan pengumuman (HTML)
	 */
	@Column(name = "catatan", nullable = false, columnDefinition="text")
	public String getCatatan() {
		return this.catatan;
	}

	/**
	 * Menyetel isi/catatan pengumuman (HTML dari editor kaya-teks).
	 *
	 * @param catatan isi baru
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Menyetel nama tampilan pembuat pengumuman ({@link #oleh}). Menolak
	 * (no-op, silent) nilai {@code null} atau string kosong/whitespace,
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama tampilan pembuat; diabaikan jika {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan (dan pada praktiknya juga menyertakan
	 * peran/role, lihat {@code PengumumanPenelitianAction.onSave}) pembuat
	 * pengumuman ini.
	 *
	 * @return nama tampilan pembuat, atau {@code null} jika belum diset
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel tanggal mulai tayang pengumuman.
	 *
	 * @param tanggal tanggal mulai baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Tanggal mulai berlaku/tayang pengumuman ini (kolom {@code @Temporal(DATE)},
	 * tanpa komponen waktu). Bersama {@link #getSampai()} membentuk rentang
	 * tayang pengumuman; pengecekan apakah tanggal saat ini berada dalam
	 * rentang ini dilakukan oleh lapisan pemanggil, bukan oleh entitas ini.
	 *
	 * @return tanggal mulai tayang, boleh {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menyetel tanggal akhir tayang pengumuman.
	 *
	 * @param sampai tanggal akhir baru
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Tanggal akhir berlaku/tayang pengumuman ini (kolom {@code @Temporal(DATE)}).
	 * Lihat catatan rentang tayang pada javadoc {@link #getTanggal()}.
	 *
	 * @return tanggal akhir tayang, boleh {@code null} bila belum diisi
	 *         (mis. berarti tayang tanpa batas akhir, tergantung interpretasi
	 *         pemanggil)
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "sampai")
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Status aktif/nonaktif pengumuman ini. Lazy-default: jika belum pernah
	 * diset ({@code null}, mis. data lama), method ini MENGISI field
	 * {@link #aktif} dengan {@code true} pada pemanggilan pertama (efek
	 * samping pada getter) sekaligus mengembalikannya — pola "default aktif"
	 * yang arahnya normal, identik dengan
	 * {@link ais.database.model.penelitiandanpengabdian.JurnalPenelitian#getAktif()}.
	 * BUKAN pola {@code getAktif()} terbalik (logika dibalik) yang ditemukan
	 * pada {@code InformasiPerpustakaan}/{@code ItemPunyaTerbit} di sesi
	 * javadoc sebelumnya — di sini {@code true} tetap konsisten berarti aktif.
	 *
	 * @return {@code true} jika pengumuman aktif (default bila belum diset),
	 *         {@code false} jika dinonaktifkan eksplisit
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif pengumuman secara eksplisit.
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk
	 *              menonaktifkan, {@code null} untuk kembali ke default
	 *              (akan dianggap aktif lagi oleh {@link #getAktif()})
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengizinkan/menutup thread diskusi-komentar
	 * ({@link ais.database.model.DiskusiPengumumanPenelitian}) pada
	 * pengumuman ini. Lazy-default: jika belum pernah diset, method ini
	 * MENGISI field {@link #bolehDiberiKomentar} dengan {@code true} pada
	 * pemanggilan pertama (efek samping pada getter, sama seperti
	 * {@link #getAktif()}) — default MENGIZINKAN komentar bila belum diset.
	 * Arah logikanya normal (tidak terbalik): {@code true} = boleh
	 * dikomentari, {@code false} = ditutup.
	 *
	 * @return {@code true} jika thread diskusi dibuka (default bila belum
	 *         diset), {@code false} jika ditutup
	 */
	public Boolean getBolehDiberiKomentar() {
		if (bolehDiberiKomentar == null) {
			bolehDiberiKomentar = true;
		}
		return bolehDiberiKomentar;
	}

	/**
	 * Menyetel izin komentar pada pengumuman ini secara eksplisit.
	 *
	 * @param bolehDiberiKomentar {@code true} untuk mengizinkan komentar,
	 *                            {@code false} untuk menutup, {@code null}
	 *                            untuk kembali ke default (dianggap
	 *                            mengizinkan lagi oleh
	 *                            {@link #getBolehDiberiKomentar()})
	 */
	public void setBolehDiberiKomentar(Boolean bolehDiberiKomentar) {
		this.bolehDiberiKomentar = bolehDiberiKomentar;
	}

	/**
	 * Daftar userId penerima notifikasi email pengumuman ini, disimpan
	 * sebagai SATU string dengan userId dipisah koma (parsing/pengiriman
	 * dilakukan oleh {@code PengumumanPenelitianAction.kirimEmailKeKorespondensi}
	 * dan {@code .kirimEmail}) — bukan relasi many-to-many terstruktur, pola
	 * identik dengan
	 * {@link ais.database.model.penelitiandanpengabdian.JurnalPenelitian#getKorespondensi()}.
	 * Kolom string maks. 1000 karakter (berbeda dari {@code JurnalPenelitian}
	 * yang bertipe {@code text} tanpa batas), boleh null di DB tapi getter
	 * ini menormalkan {@code null} menjadi string kosong (lazy-default
	 * dengan efek samping tulis-balik ke field, seperti {@link #getAktif()}).
	 * Menurut javadoc {@code PengumumanPenelitianAction}, bila kosong saat
	 * disimpan akan di-default ke userId pembuat pengumuman sendiri.
	 *
	 * @return string userId korespondensi dipisah koma, tidak pernah
	 *         {@code null} (string kosong bila belum diisi)
	 */
	@Column(name = "korespondensi", nullable = true, length = 1000)
	public String getKorespondensi() {
		if (korespondensi == null) {
			korespondensi = "";
		}
		return korespondensi;
	}

	/**
	 * Menyetel daftar userId korespondensi (format string dipisah koma,
	 * lihat {@link #getKorespondensi()}).
	 *
	 * @param korespondensi string userId dipisah koma
	 */
	public void setKorespondensi(String korespondensi) {
		this.korespondensi = korespondensi;
	}
}
