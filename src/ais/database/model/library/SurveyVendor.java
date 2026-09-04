package ais.database.model.library;

// Header Survey Pemilihan Penilaian Vendor (angket multi-pengguna, pra-pembelian).
// Staf pengadaan setup survey + vendor + kriteria(bobot configurable) + pengguna(penilai);
// tiap pengguna menilai independen; hasil diagregat + staf menentukan pemenang (audit auto vs terpilih).

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
import ais.database.model.Tbmuser;

/**
 * Survey Pemilihan Penilaian Vendor (header).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor")
public class SurveyVendor extends GeneralValueObject {

	private static final long serialVersionUID = 7720145511001000001L;

	public static final String DRAFT = "Draft";
	public static final String AKTIF = "Aktif";
	public static final String SELESAI = "Selesai";

	public static final String REKOM_DIREKOMENDASIKAN = "Direkomendasikan";
	public static final String REKOM_PERTIMBANGAN_ULANG = "Perlu pertimbangan ulang";
	public static final String REKOM_TIDAK = "Tidak direkomendasikan";

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) { return; } this.olehId = olehId; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) { return; } this.oleh = oleh; }
	public String getOleh() { return oleh; }

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	public String toString() { return judul; }

	private String kode;
	private String judul;
	private String jenisBarangJasa;
	private Date tanggal;
	private String keterangan;
	private String status;
	private Tbmuser dibuatOleh;
	private Date tanggalPembuatan;
	private Boolean pakaiQualification;   // P1: gerbang lulus/gagal sebelum scoring (opsional)

	// Ringkasan perbandingan (final oleh staf pengadaan)
	private String vendorPembanding1;
	private String vendorPembanding2;
	private String vendorPembanding3;
	private String alasanDipilih;

	// Rekomendasi & audit trail (P4): pemenang otomatis (skor) vs vendor terpilih staf + alasan
	private String rekomendasi;
	private SurveyVendorVendor vendorTerpilih;
	private String alasanUtama;

	// Penilai akhir (Lampiran 1.2)
	private String namaPenilai;
	private String jabatanPenilai;
	private Date tanggalPenilaian;

	public SurveyVendor() {}

	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "kode", length = 100) public String getKode() { return kode; }
	public void setKode(String kode) { this.kode = kode; }

	@Column(name = "judul", length = 255) public String getJudul() { return judul; }
	public void setJudul(String judul) { this.judul = judul; }

	@Column(name = "jenis_barang_jasa") public String getJenisBarangJasa() { return jenisBarangJasa; }
	public void setJenisBarangJasa(String v) { this.jenisBarangJasa = v; }

	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal") public Date getTanggal() { return tanggal; }
	public void setTanggal(Date tanggal) { this.tanggal = tanggal; }

	@Column(name = "keterangan") public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	@Column(name = "status", length = 30) public String getStatus() { return status == null ? DRAFT : status; }
	public void setStatus(String status) { this.status = status; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() { dibuatOleh = check(dibuatOleh); return dibuatOleh; }
	public void setDibuatOleh(Tbmuser dibuatOleh) { this.dibuatOleh = dibuatOleh; }

	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_pembuatan") public Date getTanggalPembuatan() { return tanggalPembuatan; }
	public void setTanggalPembuatan(Date v) { this.tanggalPembuatan = v; }

	@Column(name = "pakai_qualification") public Boolean getPakaiQualification() { return pakaiQualification != null && pakaiQualification; }
	public void setPakaiQualification(Boolean v) { this.pakaiQualification = v; }

	@Column(name = "vendor_pembanding1") public String getVendorPembanding1() { return vendorPembanding1; }
	public void setVendorPembanding1(String v) { this.vendorPembanding1 = v; }
	@Column(name = "vendor_pembanding2") public String getVendorPembanding2() { return vendorPembanding2; }
	public void setVendorPembanding2(String v) { this.vendorPembanding2 = v; }
	@Column(name = "vendor_pembanding3") public String getVendorPembanding3() { return vendorPembanding3; }
	public void setVendorPembanding3(String v) { this.vendorPembanding3 = v; }
	@Column(name = "alasan_dipilih") public String getAlasanDipilih() { return alasanDipilih; }
	public void setAlasanDipilih(String v) { this.alasanDipilih = v; }

	@Column(name = "rekomendasi", length = 60) public String getRekomendasi() { return rekomendasi; }
	public void setRekomendasi(String v) { this.rekomendasi = v; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "vendor_terpilih", nullable = true)
	public SurveyVendorVendor getVendorTerpilih() { vendorTerpilih = check(vendorTerpilih); return vendorTerpilih; }
	public void setVendorTerpilih(SurveyVendorVendor v) { this.vendorTerpilih = v; }

	@Column(name = "alasan_utama") public String getAlasanUtama() { return alasanUtama; }
	public void setAlasanUtama(String v) { this.alasanUtama = v; }

	@Column(name = "nama_penilai") public String getNamaPenilai() { return namaPenilai; }
	public void setNamaPenilai(String v) { this.namaPenilai = v; }
	@Column(name = "jabatan_penilai") public String getJabatanPenilai() { return jabatanPenilai; }
	public void setJabatanPenilai(String v) { this.jabatanPenilai = v; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_penilaian") public Date getTanggalPenilaian() { return tanggalPenilaian; }
	public void setTanggalPenilaian(Date v) { this.tanggalPenilaian = v; }
}
