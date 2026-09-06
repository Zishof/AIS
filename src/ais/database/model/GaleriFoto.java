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
 * Entity <b>album galeri foto</b> (tabel {@code public.galeri_foto}) — satu baris mewakili satu album
 * (mis. "Wisuda 2024", "Ospek 2024") dengan periode tampil ({@link #getMulai()}/{@link #getSampai()})
 * dan {@link #getJenis()} yang menandai konteks album ({@link #ALUMNI}, {@link #SPMB}, {@link #PSB}).
 * Foto-foto isi album disimpan terpisah pada {@link GaleriFotoImage} yang menunjuk balik ke baris ini
 * (relasi one-to-many, sudah didokumentasikan pada batch sebelumnya di paket {@code file}).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "galeri_foto")
public class GaleriFoto extends GeneralValueObject {

	/** Penanda {@link #getJenis()}: album terkait aktivitas alumni. */
	public static final Integer ALUMNI = 1;
	/** Penanda {@link #getJenis()}: album terkait SPMB (seleksi penerimaan mahasiswa baru). */
	public static final Integer SPMB = 2;
	/** Penanda {@link #getJenis()}: album terkait PSB (penerimaan siswa baru). */
	public static final Integer PSB = 3;

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #mulai_dirubah} setiap kali baris ini
	 * di-update.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	private Date mulai_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param mulai_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setMulai_dirubah(Date mulai_dirubah) {
		this.mulai_dirubah = mulai_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai_dirubah() {
		return mulai_dirubah;
	}

	/**
	 * @return representasi ringkas "{id}-{nama}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String nama;
	private String keterangan;
	private Date mulai;
	private Date sampai;

	private Integer jenis;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public GaleriFoto() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama/judul album, di-trim saat dibaca.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama/judul album.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan/deskripsi album.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan/deskripsi album.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return penanda jenis/konteks album — salah satu {@link #ALUMNI}, {@link #SPMB}, {@link #PSB},
	 *         atau {@code null} bila album umum/tidak berkonteks khusus.
	 */
	public Integer getJenis() {
		return jenis;
	}

	/**
	 * @param jenis penanda jenis/konteks album.
	 */
	public void setJenis(Integer jenis) {
		this.jenis = jenis;
	}

	/**
	 * @return tanggal mulai album ini ditampilkan.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * @param mulai tanggal mulai album ini ditampilkan.
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * @return tanggal berakhirnya album ini ditampilkan.
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * @param sampai tanggal berakhirnya album ini ditampilkan.
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

}
