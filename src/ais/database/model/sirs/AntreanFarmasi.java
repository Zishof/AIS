package ais.database.model.sirs;

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
 * Antrean penyiapan obat untuk layar publik Instalasi Farmasi.
 *
 * <p>Nama dan nomor rekam medis disimpan lengkap untuk konsol petugas, tetapi
 * API layar publik hanya mengirim versi tersamar. Diagnosis, alamat, nomor
 * telepon, dan aturan pakai sengaja tidak menjadi bagian entity ini.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "antrean_farmasi")
public class AntreanFarmasi extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String JENIS_JADI = "JADI";
	public static final String JENIS_RACIKAN = "RACIKAN";
	public static final String JENIS_CAMPURAN = "CAMPURAN";
	public static final String STATUS_MENUNGGU = "MENUNGGU";
	public static final String STATUS_DISIAPKAN = "DISIAPKAN";
	public static final String STATUS_SIAP = "SIAP";
	public static final String STATUS_SELESAI = "SELESAI";

	private Long id;
	private Long tokoId;
	private Long resepId;
	private String kodeAntrean;
	private String nomorRekamMedis;
	private String namaPasien;
	private String jenis;
	private String status;
	private String loket;
	private String daftarObat;
	private String catatanPublik;
	private Integer urutan;
	private Date tanggalDibuat = new Date();
	private Date tanggal_dirubah = new Date();
	private String oleh;
	private String olehId;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "toko_id", nullable = false)
	public Long getTokoId() { return tokoId; }
	public void setTokoId(Long tokoId) { this.tokoId = tokoId; }

	@Column(name = "resep_id")
	public Long getResepId() { return resepId; }
	public void setResepId(Long resepId) { this.resepId = resepId; }

	@Column(name = "kode_antrean", nullable = false, length = 30)
	public String getKodeAntrean() { return kodeAntrean; }
	public void setKodeAntrean(String kodeAntrean) { this.kodeAntrean = kodeAntrean; }

	@Column(name = "nomor_rekam_medis", length = 80)
	public String getNomorRekamMedis() { return nomorRekamMedis; }
	public void setNomorRekamMedis(String nomorRekamMedis) { this.nomorRekamMedis = nomorRekamMedis; }

	@Column(name = "nama_pasien", nullable = false, length = 160)
	public String getNamaPasien() { return namaPasien; }
	public void setNamaPasien(String namaPasien) { this.namaPasien = namaPasien; }

	@Column(name = "jenis", nullable = false, length = 20)
	public String getJenis() { return jenis; }
	public void setJenis(String jenis) { this.jenis = jenis; }

	@Column(name = "status", nullable = false, length = 20)
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	@Column(name = "loket", length = 40)
	public String getLoket() { return loket; }
	public void setLoket(String loket) { this.loket = loket; }

	@Column(name = "daftar_obat", columnDefinition = "text")
	public String getDaftarObat() { return daftarObat; }
	public void setDaftarObat(String daftarObat) { this.daftarObat = daftarObat; }

	@Column(name = "catatan_publik", length = 240)
	public String getCatatanPublik() { return catatanPublik; }
	public void setCatatanPublik(String catatanPublik) { this.catatanPublik = catatanPublik; }

	@Column(name = "urutan")
	public Integer getUrutan() { return urutan; }
	public void setUrutan(Integer urutan) { this.urutan = urutan; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dibuat", nullable = false)
	public Date getTanggalDibuat() { return tanggalDibuat; }
	public void setTanggalDibuat(Date tanggalDibuat) { this.tanggalDibuat = tanggalDibuat; }

	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh; }

	@Column(name = "oleh_id")
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId; }
}
