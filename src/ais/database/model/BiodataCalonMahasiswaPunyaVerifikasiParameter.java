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
 * Entitas Hibernate yang memetakan tabel
 * {@code public.biodata_calon_mahasiswa_punya_verifikasi_parameter} pada modul
 * penerimaan mahasiswa baru. Merupakan tabel penghubung (junction) yang
 * mencatat hasil verifikasi satu {@link ParameterVerifikasiCalonMahasiswa}
 * (mis. item cek berkas/syarat pendaftaran) untuk satu {@link
 * BiodataCalonMahasiswa} (data calon mahasiswa) yang mendaftar lewat satu
 * {@link PaketPunyaParameterVerifikasiCalonMahasiswa} (paket parameter
 * verifikasi yang berlaku, mis. per jalur/gelombang seleksi) — flag
 * {@code verified} menandai apakah parameter tersebut sudah dinyatakan
 * terverifikasi, dan {@code keterangan} menyimpan catatan petugas verifikasi.
 *
 * <p>
 * {@code nama} otomatis diisi dari nama {@link ParameterVerifikasiCalonMahasiswa}
 * terkait bila belum diisi manual — lihat {@link #ambilNamaParameterDefault()},
 * yang juga dipanggil pada {@code @PrePersist}/{@code @PreUpdate} untuk
 * menormalkan field sebelum disimpan. Diaudit lewat Hibernate Envers
 * ({@code @Audited}).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_calon_mahasiswa_punya_verifikasi_parameter")
public class BiodataCalonMahasiswaPunyaVerifikasiParameter extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa;
	private ParameterVerifikasiCalonMahasiswa parameterVerifikasiCalonMahasiswa;
	private String nama;
	private String keterangan;
	private Boolean verified;

	public BiodataCalonMahasiswaPunyaVerifikasiParameter() {
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

	private void normalize() {
		if (verified == null) {
			verified = Boolean.FALSE;
		}
		if (isBlank(nama)) {
			nama = ambilNamaParameterDefault();
		} else {
			nama = nama.trim();
		}
		if (keterangan != null) {
			keterangan = keterangan.trim();
		}
	}

	private String ambilNamaParameterDefault() {
		try {
			if (getParameterVerifikasiCalonMahasiswa() != null && !isBlank(getParameterVerifikasiCalonMahasiswa().getNama())) {
				return getParameterVerifikasiCalonMahasiswa().getNama().trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswaPunyaVerifikasiParameter.java:107");
		}
		return "Parameter Verifikasi";
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
	@JoinColumn(name = "paket_punya_parameter_verifikasi_calon_mahasiswa", nullable = true)
	public PaketPunyaParameterVerifikasiCalonMahasiswa getPaketPunyaParameterVerifikasiCalonMahasiswa() {
		paketPunyaParameterVerifikasiCalonMahasiswa = check(paketPunyaParameterVerifikasiCalonMahasiswa);
		return paketPunyaParameterVerifikasiCalonMahasiswa;
	}

	public void setPaketPunyaParameterVerifikasiCalonMahasiswa(
			PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa) {
		this.paketPunyaParameterVerifikasiCalonMahasiswa = paketPunyaParameterVerifikasiCalonMahasiswa;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(columnDefinition = "text")
	public String getNama() {
		return isBlank(nama) ? ambilNamaParameterDefault() : nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_verifikasi_calon_mahasiswa", nullable = true)
	public ParameterVerifikasiCalonMahasiswa getParameterVerifikasiCalonMahasiswa() {
		parameterVerifikasiCalonMahasiswa = check(parameterVerifikasiCalonMahasiswa);
		return parameterVerifikasiCalonMahasiswa;
	}

	public void setParameterVerifikasiCalonMahasiswa(
			ParameterVerifikasiCalonMahasiswa parameterVerifikasiCalonMahasiswa) {
		this.parameterVerifikasiCalonMahasiswa = parameterVerifikasiCalonMahasiswa;
	}

	public Boolean getVerified() {
		return verified == null ? Boolean.FALSE : verified;
	}

	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

	public String toString() {
		return getNama();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
