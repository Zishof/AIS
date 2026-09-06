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

import org.hibernate.envers.Audited;

/**
 * Entity <b>jalur samping keanggotaan PKL</b> pada tabel {@code public.mahasiswa_dapat_pkl}.
 * Sesuai Javadoc kelas {@link Pkl} (&sect;"Jalur samping"), kelas ini mengaitkan mahasiswa
 * <b>langsung</b> ke suatu program {@link Pkl} <b>tanpa lewat penempatan kelompok</b>
 * ({@code ais.database.model.pkl.KelompokPkl}) &mdash; dipakai layar {@code PklAction} lewat
 * {@code ais.action.master.helper.PklHelper} (grid tambah/hapus peserta langsung) dan
 * {@code AmbilDataMahasiswaPklHelper}/{@code AmbilDataMahasiswaSeleksiPklHelper}.
 *
 * <p><b>Bukan entity pendaftaran, dan bukan legacy.</b> Pendaftaran mahasiswa (dengan status
 * seleksi {@code terima}, tanggal daftar, dsb.) disimpan terpisah di
 * {@link ais.database.model.pkl.MahasiswaDaftarPkl} (paket {@code pkl}). Kelas ini tidak
 * menyimpan status seleksi apa pun &mdash; semata relasi {@link #getPkl()}/{@link #getMahasiswa()}
 * (keduanya wajib diisi, sama seperti kembarannya {@link MahasiswaDapatKkn} pada modul KKN)
 * plus {@link #getKeterangan()} bebas.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Pkl
 * @see ais.database.model.pkl.MahasiswaDaftarPkl
 * @see MahasiswaDapatKkn
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mahasiswa_dapat_pkl")

public class MahasiswaDapatPkl extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.mahasiswa_dapat_pkl} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan
	 * diam-diam (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * masukan kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi
	 * ulang {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk
	 * dipanggil langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #getKeterangan()} apa adanya — representasi teks ringkas baris ini. */
	public String toString() {
		return keterangan;
	}

	/** Program PKL yang diikuti mahasiswa ini. Wajib diisi (kolom {@code NOT NULL}). */
	private Pkl pkl;
	/** Mahasiswa yang dikaitkan ke program PKL ini lewat jalur samping. Wajib diisi (kolom {@code NOT NULL}). */
	private Mahasiswa mahasiswa;
	/** Catatan/keterangan bebas keanggotaan ini; boleh {@code null}. */
	private String keterangan;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public MahasiswaDapatPkl() {
	}

	/**
	 * @return kunci utama baris ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return keterangan bebas keanggotaan ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk keanggotaan ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param pkl program PKL yang diikuti mahasiswa ini. */
	public void setPkl(Pkl pkl) {
		this.pkl = pkl;
	}

	/**
	 * @return program {@link Pkl} yang diikuti. Referensi dicek lewat {@code check(pkl)}
	 *         sebelum dikembalikan (proxy Hibernate basi diganti entity segar bila perlu).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pkl", nullable = false)
	public Pkl getPkl() {
		pkl = check(pkl);
		return pkl;
	}

	/** @param mahasiswa mahasiswa yang dikaitkan ke program PKL ini. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return mahasiswa peserta jalur samping ini. Referensi dicek lewat
	 *         {@code check(mahasiswa)} sebelum dikembalikan, sama seperti {@link #getPkl()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

}
