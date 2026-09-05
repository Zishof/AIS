package ais.database.model.penelitiandanpengabdian;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;

/**
 * Entitas relasi <b>satu-artikel-ke-banyak-anggota</b> (tabel
 * {@code penelitiandanpengabdian.anggota_artikel}) yang menautkan satu {@link Artikel} ke satu
 * penulis/kontributornya: satu baris di sini mewakili satu pasangan artikel-anggota, sehingga satu
 * artikel dapat memiliki berapa pun baris (multi-penulis). Anggota dapat berupa dosen/pegawai
 * ({@link #tbmuser}) atau mahasiswa ({@link #mahasiswa}) — kedua relasi saling eksklusif, lihat
 * {@link #getTbmuser()}.
 *
 * <p><b>Tidak ada kolom urutan/ordinal penulis</b> pada tabel ini — sama seperti pola yang sudah
 * ditemukan pada {@code library.ItemPunyaPengarang}, semua anggota yang tertaut sebuah artikel
 * berkedudukan setara secara data; tidak ada pembedaan skema "penulis utama" vs "kontributor" di
 * level baris. Urutan tampil (bila ada di UI) berarti ditentukan oleh urutan penyimpanan/urutan
 * baca daftar, bukan oleh nilai kolom eksplisit di entitas ini.</p>
 *
 * <p>Bidang {@link #repoContributorId} menyimpan id kontributor pada sistem repositori eksternal
 * (mis. saat artikel induknya diekspor/didaftarkan ke repositori tersebut), sejalan dengan
 * {@code repoItemId} pada {@link Artikel} dan {@code repoBitstreamId} pada {@link FileArtikel}.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see Artikel
 * @see FileArtikel
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "penelitiandanpengabdian", name = "anggota_artikel")



public class AnggotaArtikel extends GeneralValueObject {
	/** Id kontributor pada sistem repositori eksternal tempat artikel induk diarsipkan. */
	private Long repoContributorId;

	/**
	 * Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463812577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;
	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris relasi ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Menyetel id pengguna yang terakhir mengubah baris relasi ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris relasi ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris relasi ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Interceptor-lah
	 * yang mengisi {@link #oleh}, {@link #olehId}, dan {@link #getTanggal_dirubah()} dari konteks
	 * pengguna aktif. Method sengaja {@code protected} dan tidak boleh dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor},
	 * bukan oleh form.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris relasi ini.
	 *
	 * @return stempel waktu perubahan terakhir, dapat {@code null} bila belum pernah diubah lewat
	 *         jalur yang memasang interceptor audit dan field belum diinisialisasi manual
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat gabungan pengaju dan artikel, dipakai label bawaan komponen ZK dan
	 * penelusuran log. Memanggil {@code toString()} ketiga relasi apa adanya (dapat memicu resolusi
	 * lazy pada proxy Hibernate yang belum diinisialisasi).
	 *
	 * @return {@code "<tbmuser>_<mahasiswa>_<artikel>"}; bagian yang {@code null} tampil sebagai
	 *         teks {@code "null"} bawaan concatenation Java
	 */
	public String toString() {
		return tbmuser + "_" + mahasiswa + "_" + artikel;
	}

	/** Dosen/pegawai anggota artikel; alternatif dari {@link #mahasiswa} — lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Mahasiswa anggota artikel, alternatif dari {@link #tbmuser}. */
	private Mahasiswa mahasiswa;
	/** Keterangan tambahan mengenai peran/kontribusi anggota pada artikel. */
	private String keterangan;
	/** Artikel induk yang ditautkan anggota ini. */
	private Artikel artikel;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public AnggotaArtikel() {
	}
	/**
	 * Mengembalikan id kontributor pada sistem repositori eksternal.
	 *
	 * @return id kontributor repositori, atau {@code null} bila belum diarsipkan
	 */
	@Column(name="repo_contributor_id") public Long getRepoContributorId(){return repoContributorId;}
	/**
	 * Menyetel id kontributor pada sistem repositori eksternal.
	 *
	 * @param v id kontributor repositori baru
	 */
	public void setRepoContributorId(Long v){repoContributorId=v;}

	/**
	 * Mengembalikan kunci utama baris relasi ini.
	 *
	 * @return id baris relasi, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris relasi ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan tambahan mengenai peran/kontribusi anggota pada artikel.
	 *
	 * @return keterangan, dapat {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan tambahan mengenai peran/kontribusi anggota.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan dosen/pegawai anggota artikel. Proxy lazy diresolusi lebih dulu lewat
	 * {@code check()}.
	 *
	 * <p><b>Saling eksklusif dengan {@link #getMahasiswa()}:</b> bila {@link #mahasiswa} terisi,
	 * bidang ini dipaksa {@code null} — satu baris anggota hanya boleh menunjuk salah satu peran
	 * (dosen/pegawai ATAU mahasiswa), tidak keduanya, mengikuti pola yang sama pada
	 * {@link Artikel#getTbmuser()}.</p>
	 *
	 * @return dosen/pegawai anggota, atau {@code null} bila baris ini menunjuk {@link #mahasiswa}
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
	 * Menyetel dosen/pegawai anggota artikel.
	 *
	 * @param tbmuser dosen/pegawai anggota baru; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan mahasiswa anggota artikel, alternatif dari {@link #getTbmuser()}. Dimuat
	 * dengan {@link FetchMode#SELECT}. Berbeda dari {@link #getTbmuser()}, getter ini
	 * <b>tidak</b> memuat proxy lewat {@code check()} dan tidak memiliki logika saling-eksklusif
	 * di sisinya sendiri — pengosongan {@link #tbmuser} saat {@link #mahasiswa} terisi ditegakkan
	 * hanya dari sisi {@link #getTbmuser()}.
	 *
	 * @return mahasiswa anggota, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa anggota artikel.
	 *
	 * @param mahasiswa mahasiswa anggota baru; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan artikel induk yang ditautkan anggota ini. Dimuat dengan
	 * {@link FetchMode#SELECT}. Kolom relasi ini {@code nullable = false} — setiap baris anggota
	 * wajib menunjuk satu artikel.
	 *
	 * @return artikel induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "artikel", nullable = false)
	public Artikel getArtikel() {
		return artikel;
	}

	/**
	 * Menyetel artikel induk yang ditautkan anggota ini.
	 *
	 * @param artikel artikel induk baru
	 */
	public void setArtikel(Artikel artikel) {
		this.artikel = artikel;
	}

}
