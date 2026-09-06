package ais.database.model;

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
import org.hibernate.envers.Audited;

import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

//import org.zkforge.fckez.MyCkEditor;

/**
 * Model data untuk satu KOMENTAR/BALASAN pada thread diskusi sebuah {@link PengumumanAkademis}.
 * Mendukung penulis dari ENAM jenis akun berbeda (mahasiswa, siswa, calon siswa, calon mahasiswa,
 * dosen, guru) sekaligus akun staf generik {@link Tbmuser} sebagai cadangan -- pola "siapa
 * penulisnya" yang sama seperti mekanisme {@code Diskusi} generik lain di AIS (lihat
 * {@code task_493423ef} soal anonimitas), meski kelas ini bukan bagian dari hierarki generik itu
 * melainkan implementasi KHUSUS untuk pengumuman akademis. Mendukung balasan berjenjang lewat
 * {@link #getParent()} (self-reference).
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code Date
 * tanggal_dirubah}, {@code String catatan}, {@code PengumumanAkademis pengumumanAkademis}, {@code Tbmuser
 * tbmuser}, {@code Mahasiswa mahasiswa}, {@code Dosen dosen}, {@code Siswa siswa}, {@code Guru guru}, {@code
 * CalonSiswa calonSiswa}, {@code DiskusiPengumumanAkademis parent}; pemetaan persistence: tabel
 * {@code public.diskusi_pengumuman_akademis}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()},
 * {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getCatatan()}, {@code getPengumumanAkademis()},
 * {@code getParent()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code
 * onUpdate()}, {@code setTanggal_dirubah()}, {@code setCatatan()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>{@link #getOleh()} adalah RESOLVER penulis, bukan accessor sederhana</b> -- setiap pemanggilan
 * membaca ULANG {@link #getMahasiswa()}/{@link #getSiswa()}/{@link #getCalonSiswa()}/{@link
 * #getBiodataCalonMahasiswa()}/{@link #getTbmuser()} secara berurutan (prioritas dalam urutan itu), lalu
 * MENIMPA field {@link #oleh} dengan nama entitas pertama yang ditemukan tidak {@code null}. Bila
 * {@link #getTbmuser()} yang dipakai, nama diambil dari dosen/pegawai terkait tbmuser itu, atau username-nya
 * sebagai cadangan terakhir. Nilai literal {@code "external_update"} (penanda internal proses sinkronisasi)
 * disaring menjadi string kosong sebelum dikembalikan. {@link #setOleh(String)} karena itu HANYA berlaku
 * sebagai fallback -- nilai apa pun yang diset lewatnya akan tertimpa selama salah satu relasi penulis di
 * atas terisi.</p>
 * <p><b>{@link #getJudul()} SELALU mengembalikan {@code "-"}</b> -- getter ini menimpa field {@link #judul}
 * dengan literal {@code "-"} pada SETIAP pemanggilan tanpa syarat, sehingga kolom judul di database menjadi
 * TIDAK PERNAH terbaca kembali walau diisi lewat {@link #setJudul(String)}; thread diskusi ini efektif tidak
 * bertajuk individual (konsisten dengan {@link #toString()} yang memakai {@link #catatan}, bukan judul).</p>
 * <p><b>{@link #getTbmuser()} menolak dirinya sendiri</b> bila salah satu dari
 * mahasiswa/biodataCalonMahasiswa/siswa/calonSiswa terisi -- constraint "penulis harus SATU jenis akun
 * saja" ditegakkan di level getter, bukan lewat validasi/constraint database.</p>
 * <p><b>Efek samping:</b> selain accessor state, {@link #getCatatan()} memanggil {@code filterTidakBoleh}
 * (penyaring kata terlarang) pada setiap pembacaan. Persistence, transaksi, otorisasi, dan pemuatan relasi
 * lazy tetap menjadi tanggung jawab DAO/service dengan session aktif; jangan menaruh query duplikat pada
 * model.</p>
 *
 * @see GeneralValueObject
 * @see PengumumanAkademis pengumuman yang menaungi thread diskusi ini
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "diskusi_pengumuman_akademis")
public class DiskusiPengumumanAkademis extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 2463822577541439808L;
	/** Primary key entity (kolom {@code id}, identity/auto-increment). */
	private Long id;

	/**
	 * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk debug/log: isi {@link #getCatatan()} apa adanya. */
	public String toString() {
		return catatan;
	}

	/** Judul komentar; SELALU tertimpa {@code "-"} oleh {@link #getJudul()}, lihat catatan class. */
	private String judul;
	/** Isi komentar/balasan (disaring kata terlarang saat dibaca, lihat {@link #getCatatan()}). */
	private String catatan;
	/** Nama penulis; MENIMPA dirinya sendiri lewat {@link #getOleh()}, lihat catatan class. */
	private String oleh;
	/** Id pengguna penulis; tidak dipakai resolver {@link #getOleh()}, murni penanda tambahan. */
	private String olehId;

	/** @return id pengguna penulis (bila terisi), boleh {@code null}. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penulis. Nilai {@code null}/kosong diabaikan diam-diam.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Nama pengguna dalam bentuk lain (mis. login/username), terpisah dari {@link #oleh}. */
	private String pengguna;
	/** Waktu komentar dibuat/diubah, boleh {@code null}. */
	private Date tanggal;
	/** Pengumuman akademis yang menaungi thread diskusi ini (wajib). */
	private PengumumanAkademis pengumumanAkademis;
	/** Akun staf generik penulis, dipakai bila bukan mahasiswa/siswa/calon siswa/calon mahasiswa. */
	private Tbmuser tbmuser;
	/** Mahasiswa penulis (bila penulis adalah mahasiswa). */
	private Mahasiswa mahasiswa;
	/** Komentar induk bila baris ini adalah balasan (self-reference), boleh {@code null}. */
	private DiskusiPengumumanAkademis parent;

	/** Dosen penulis (di-resolve dari {@link #tbmuser} bila rolenya "dosen"), lihat {@link #getDosen()}. */
	private Dosen dosen;
	/** Calon mahasiswa penulis (bila penulis adalah calon mahasiswa). */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Siswa penulis (bila penulis adalah siswa). */
	private Siswa siswa;
	/** Guru penulis (di-resolve dari {@link #tbmuser} bila terkait guru), lihat {@link #getGuru()}. */
	private Guru guru;
	/** Calon siswa penulis (bila penulis adalah calon siswa). */
	private CalonSiswa calonSiswa;

	/** Konstruktor kosong, dipakai Hibernate. */
	public DiskusiPengumumanAkademis() {
		// MyCkEditor
	}

	/** @return primary key entity, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return isi komentar setelah disaring lewat {@code filterTidakBoleh} (penyaring kata
	 *         terlarang) -- field {@link #catatan} DITIMPA dengan hasil saringan pada setiap
	 *         pemanggilan.
	 */
	@Column(name = "catatan", nullable = false, length = 3000)
	public String getCatatan() {
		catatan = filterTidakBoleh(catatan);
		return this.catatan;
	}

	/** @param catatan isi komentar baru; akan disaring ulang pada pemanggilan {@link #getCatatan()} berikutnya. */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Menyetel nama penulis secara langsung -- HANYA berlaku sebagai fallback, karena
	 * {@link #getOleh()} akan MENIMPA nilai ini bila salah satu relasi penulis (mahasiswa/
	 * siswa/calon siswa/calon mahasiswa/tbmuser) terisi. Nilai {@code null}/kosong diabaikan
	 * diam-diam.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama penulis, di-RESOLVE ULANG setiap pemanggilan dari relasi penulis dengan
	 *         urutan prioritas: {@link #getMahasiswa()}, {@link #getSiswa()}, {@link
	 *         #getCalonSiswa()}, {@link #getBiodataCalonMahasiswa()}, lalu {@link #getTbmuser()}
	 *         (nama dosen/pegawai terkait, atau username sebagai cadangan terakhir). Nilai
	 *         literal {@code "external_update"} (penanda sinkronisasi internal) disaring menjadi
	 *         string kosong. Lihat catatan lengkap pada javadoc class.
	 */
	@Column(name = "oleh")
	public String getOleh() {
		mahasiswa = getMahasiswa();
		siswa = getSiswa();
		calonSiswa = getCalonSiswa();
		biodataCalonMahasiswa = getBiodataCalonMahasiswa();
		tbmuser = getTbmuser();
		if (mahasiswa != null) {
			oleh = mahasiswa.getNama();
		} else if (siswa != null) {
			oleh = siswa.getNama();
		} else if (calonSiswa != null) {
			oleh = calonSiswa.getNama();
		} else if (biodataCalonMahasiswa != null) {
			oleh = biodataCalonMahasiswa.getNama();
		} else if (tbmuser != null) {
			if (tbmuser.ambilDosen() != null) {
				oleh = tbmuser.ambilDosen().getNama();
			} else if (tbmuser.ambilPegawai() != null) {
				oleh = tbmuser.ambilPegawai().getNama();
			} else {
				oleh = tbmuser.getUserNama();
			}
		}
		return oleh == null || oleh.trim().equalsIgnoreCase("external_update") ? "" : oleh.trim();
	}

	/** @param tanggal waktu komentar yang baru. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** @return waktu komentar dibuat/diubah, boleh {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal;
	}

	/** @param pengumumanAkademis pengumuman akademis yang menaungi thread ini, baru. */
	public void setPengumumanAkademis(PengumumanAkademis pengumumanAkademis) {
		this.pengumumanAkademis = pengumumanAkademis;
	}

	/** @return pengumuman akademis yang menaungi thread ini (wajib); dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengumuman_akademis", nullable = false)
	public PengumumanAkademis getPengumumanAkademis() {
		pengumumanAkademis = check(pengumumanAkademis);
		return pengumumanAkademis;
	}

	/** @param judul judul komentar baru; TIDAK PERNAH terbaca kembali, lihat catatan {@link #getJudul()}. */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * @return SELALU {@code "-"} -- field {@link #judul} ditimpa literal ini pada setiap
	 *         pemanggilan tanpa syarat, terlepas dari apa yang pernah diset lewat
	 *         {@link #setJudul(String)}. Lihat catatan lengkap pada javadoc class.
	 */
	@Column(name = "judul", nullable = false, length = 500)
	public String getJudul() {
		judul = "-";
		return judul;
	}

	/** @return nama pengguna (login/username) penulis, string kosong (bukan {@code null}) bila belum diisi. */
	public String getPengguna() {
		return pengguna == null ? "" : pengguna.trim();
	}

	/** @param pengguna nama pengguna (login/username) baru. */
	public void setPengguna(String pengguna) {
		this.pengguna = pengguna;
	}

	/**
	 * @return akun staf generik penulis, atau {@code null} bila penulis sebenarnya adalah
	 *         mahasiswa/calon mahasiswa/siswa/calon siswa -- getter ini SENGAJA mengosongkan
	 *         {@link #tbmuser} bila salah satu relasi peserta didik itu terisi, menegakkan
	 *         "penulis harus satu jenis akun saja" di level getter.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (mahasiswa != null || biodataCalonMahasiswa != null || siswa != null || calonSiswa != null) {
			tbmuser = null;
		} else {
			tbmuser = check(tbmuser);
		}
		return tbmuser;
	}

	/** @param tbmuser akun staf generik penulis yang baru. */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/** @return mahasiswa penulis, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param mahasiswa mahasiswa penulis yang baru. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return calon mahasiswa penulis, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/** @param biodataCalonMahasiswa calon mahasiswa penulis yang baru. */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/** @return komentar induk (self-reference) bila baris ini balasan, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "parent", nullable = true)
	public DiskusiPengumumanAkademis getParent() {
		return parent;
	}

	/** @param parent komentar induk yang baru. */
	public void setParent(DiskusiPengumumanAkademis parent) {
		this.parent = parent;
	}

	/** @param dosen dosen penulis yang baru; bisa tertimpa lagi oleh {@link #getDosen()}. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * @return dosen penulis. Bila {@link #getTbmuser()} tidak {@code null} DAN punya relasi dosen
	 *         DAN role aktifnya persis {@code "dosen"} (case-insensitive), field ini DITIMPA
	 *         dengan dosen tersebut sebelum di-resolve lewat {@link GeneralValueObject#check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		tbmuser = getTbmuser();
		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			dosen = tbmuser.ambilDosen();
		}
		dosen = check(dosen);
		return dosen;
	}

	/** @return siswa penulis, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/** @param siswa siswa penulis yang baru. */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * @return guru penulis. Bila {@link #tbmuser} (state field, BUKAN hasil {@link
	 *         #getTbmuser()}) tidak {@code null} dan punya relasi guru, field ini DITIMPA dengan
	 *         guru tersebut sebelum di-resolve lewat {@link GeneralValueObject#check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		if (tbmuser != null && tbmuser.ambilGuru() != null) {
			guru = tbmuser.ambilGuru();
		}
		guru = check(guru);
		return guru;
	}

	/** @param guru guru penulis yang baru. */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/** @return calon siswa penulis, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/** @param calonSiswa calon siswa penulis yang baru. */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}
}
