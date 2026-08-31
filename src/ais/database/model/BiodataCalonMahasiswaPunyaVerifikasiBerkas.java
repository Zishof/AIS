package ais.database.model;

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
 * Entitas Hibernate: baris tautan many-to-many-like antara {@link BiodataCalonMahasiswa} (calon
 * mahasiswa pendaftar) dan {@link VerifikasiKelengkapanCalonMahasiswa} (item verifikasi berkas
 * yang wajib dipenuhi, mis. "Ijazah", "KTP Orang Tua") — dipetakan ke tabel
 * {@code public.biodata_calon_mahasiswa_punya_verifikasi_berkas}. Satu baris = status verifikasi
 * satu jenis berkas untuk satu calon mahasiswa: sudah diunggah ({@link #uploaded}) dan/atau sudah
 * diverifikasi petugas ({@link #verified}), berikut nama berkas dan keterangan bebas.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_calon_mahasiswa_punya_verifikasi_berkas")
public class BiodataCalonMahasiswaPunyaVerifikasiBerkas extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Calon mahasiswa pemilik berkas ini. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Item/jenis verifikasi kelengkapan yang harus dipenuhi oleh berkas ini. */
	private VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa;
	/** Apakah berkas sudah diverifikasi/disetujui petugas; dinormalisasi ke {@code false} (bukan {@code null}) sebelum simpan. */
	private Boolean verified;
	/** Apakah berkas sudah diunggah oleh calon mahasiswa; dinormalisasi ke {@code false} (bukan {@code null}) sebelum simpan. */
	private Boolean uploaded;
	/** Nama berkas yang diunggah. */
	private String namaFile;
	private String keterangan;

	public BiodataCalonMahasiswaPunyaVerifikasiBerkas() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (isBlank(olehId)) {
			return;
		}
		this.olehId = olehId.trim();
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (isBlank(oleh)) {
			return;
		}
		this.oleh = oleh.trim();
	}

	@javax.persistence.PrePersist
	protected void onPersist() {
		normalize();
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
		normalize();
	}

	/** Dipanggil dari {@link #onPersist()}/{@link #onUpdate()}: mengisi {@link #verified}/{@link #uploaded} dengan {@code false} bila {@code null}, dan men-trim {@link #namaFile}/{@link #keterangan}. */
	private void normalize() {
		if (verified == null) {
			verified = Boolean.FALSE;
		}
		if (uploaded == null) {
			uploaded = Boolean.FALSE;
		}
		if (namaFile != null) {
			namaFile = namaFile.trim();
		}
		if (keterangan != null) {
			keterangan = keterangan.trim();
		}
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "verifikasi_kelengkapan_calon_mahasiswa", nullable = true)
	public VerifikasiKelengkapanCalonMahasiswa getVerifikasiKelengkapanCalonMahasiswa() {
		verifikasiKelengkapanCalonMahasiswa = check(verifikasiKelengkapanCalonMahasiswa);
		return verifikasiKelengkapanCalonMahasiswa;
	}

	public void setVerifikasiKelengkapanCalonMahasiswa(
			VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa) {
		this.verifikasiKelengkapanCalonMahasiswa = verifikasiKelengkapanCalonMahasiswa;
	}

	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Boolean getVerified() {
		return verified == null ? Boolean.FALSE : verified;
	}

	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

	public Boolean getUploaded() {
		return uploaded == null ? Boolean.FALSE : uploaded;
	}

	public void setUploaded(Boolean uploaded) {
		this.uploaded = uploaded;
	}

	public String getNamaFile() {
		return namaFile == null ? "" : namaFile.trim();
	}

	public void setNamaFile(String namaFile) {
		this.namaFile = namaFile;
	}

	public String toString() {
		try {
			String namaBerkas = getVerifikasiKelengkapanCalonMahasiswa() == null ? "" : getVerifikasiKelengkapanCalonMahasiswa().getNama();
			String namaCalon = getBiodataCalonMahasiswa() == null ? "" : getBiodataCalonMahasiswa().getNama();
			return (namaCalon + " - " + namaBerkas).trim();
		} catch (Exception e) {
			return getNamaFile();
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
