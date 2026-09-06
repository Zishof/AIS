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
 * Entity relasi <b>kelompok matakuliah &harr; matakuliah</b> pada tabel
 * {@code public.kelompok_matakuliah_punya_matakuliah}. Baris ini menyatakan satu
 * {@link Matakuliah} tertentu menjadi anggota satu {@link KelompokMatakuliah} (mis. kelompok
 * "MKWU"/"Peminatan"); relasi banyak-ke-banyak dimodelkan lewat tabel penghubung ini, bukan
 * {@code @ManyToMany} langsung.
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see KelompokMatakuliah
 * @see Matakuliah
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_matakuliah_punya_matakuliah")

public class KelompokMatakuliahPunyaMatakuliah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.kelompok_matakuliah_punya_matakuliah} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan diam-diam
	 * (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, masukan
	 * kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi ulang
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk dipanggil
	 * langsung dari kode aplikasi.
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

	/**
	 * @return representasi teks berbentuk {@code "<kelompokMatakuliah> - <matakuliah>"}. Sebagai
	 *         efek samping, method ini juga meresolusi kedua relasi lazy lewat pemanggilan
	 *         {@link #getKelompokMatakuliah()}/{@link #getMatakuliah()} dan menuliskannya kembali
	 *         ke field, sehingga proxy yang belum terinisialisasi ikut ter-"check" saat
	 *         {@code toString()} dipanggil (mis. lewat log/debug).
	 */
	public String toString() {
		kelompokMatakuliah = getKelompokMatakuliah();
		matakuliah = getMatakuliah();
		return kelompokMatakuliah + " - " + matakuliah;
	}

	/** Kelompok matakuliah pemilik relasi ini; relasi lazy, di-"check" sebelum dikembalikan. */
	private KelompokMatakuliah kelompokMatakuliah;
	/** Matakuliah anggota kelompok di atas; relasi lazy, di-"check" sebelum dikembalikan. */
	private Matakuliah matakuliah;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public KelompokMatakuliahPunyaMatakuliah() {
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

	/**
	 * @return {@link KelompokMatakuliah} pemilik relasi ini; relasi {@code @ManyToOne} lazy,
	 *         diresolusi lewat {@link GeneralValueObject#check(Object)} sebelum dikembalikan agar
	 *         aman terhadap proxy yang sudah detached.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_matakuliah", nullable = false)
	public KelompokMatakuliah getKelompokMatakuliah() {
		kelompokMatakuliah = check(kelompokMatakuliah);
		return kelompokMatakuliah;
	}

	/** @param kelompokMatakuliah kelompok matakuliah pemilik relasi ini; wajib diisi. */
	public void setKelompokMatakuliah(KelompokMatakuliah kelompokMatakuliah) {
		this.kelompokMatakuliah = kelompokMatakuliah;
	}

	/**
	 * @return {@link Matakuliah} anggota kelompok di atas; relasi {@code @ManyToOne} lazy,
	 *         diresolusi lewat {@link GeneralValueObject#check(Object)} sebelum dikembalikan agar
	 *         aman terhadap proxy yang sudah detached.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah", nullable = false)
	public Matakuliah getMatakuliah() {
		matakuliah = check(matakuliah);
		return matakuliah;
	}

	/** @param matakuliah matakuliah anggota kelompok; wajib diisi. */
	public void setMatakuliah(Matakuliah matakuliah) {
		this.matakuliah = matakuliah;
	}

}
