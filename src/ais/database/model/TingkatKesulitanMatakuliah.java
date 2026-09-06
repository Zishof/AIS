package ais.database.model;

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

/**
 * Entity master data <b>tingkat kesulitan matakuliah</b> pada tabel
 * {@code public.tingkat_kesulitan_matakuliah}. Mengklasifikasikan tingkat kesulitan sebuah
 * {@code Matakuliah} (mis. "Mudah"/"Sedang"/"Sulit") lewat pasangan {@link #getKode() kode} dan
 * {@link #getKeterangan() keterangan}; dipakai sebagai daftar pilihan pada layar master
 * matakuliah.
 *
 * <p>Kelas ini murni tabel referensi kecil: hanya {@code kode} dan {@code keterangan} sebagai
 * data bisnis, tanpa sakelar {@code aktif}/{@code nomorUrut} seperti sejumlah master data sejenis
 * lain di paket ini.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "tingkat_kesulitan_matakuliah")

public class TingkatKesulitanMatakuliah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.tingkat_kesulitan_matakuliah} ({@code IDENTITY}). */
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

	/** @return representasi teks berbentuk {@code "<kode>_<keterangan>"}. */
	public String toString() {
		return kode + "_" + keterangan;
	}

	/** Kode ringkas tingkat kesulitan (mis. "M", "S"); wajib diisi. */
	private String kode;
	/** Nama/keterangan tingkat kesulitan (mis. "Mudah", "Sulit"); wajib diisi. */
	private String keterangan;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public TingkatKesulitanMatakuliah() {
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

	/** @param kode kode ringkas tingkat kesulitan; disimpan apa adanya, tanpa normalisasi. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return kode ringkas tingkat kesulitan apa adanya, tanpa normalisasi. */
	@Column(name = "kode", nullable = false)
	public String getKode() {
		return kode;
	}

	/** @return nama/keterangan tingkat kesulitan apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan", nullable = false)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan nama/keterangan tingkat kesulitan. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
