package ais.database.model.library;

// Modul Seleksi Vendor (Pemilihan Penilaian Vendor / Pra-Pembelian).
// Header pengajuan SOP: berisi data ringkas, bobot best-practice, ringkasan
// perbandingan, rekomendasi, dan tautan disposisi SOP (mirip UangMuka).

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

import ais.database.model.Tbmuser;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Header pengajuan Seleksi Vendor.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "seleksi_vendor")
public class SeleksiVendor extends DataSop {

	private static final long serialVersionUID = 2463821577548439810L;

	public static final String PENGAJUAN = "Pengajuan";
	public static final String DISETUJU = "Disetujui";
	public static final String DITOLAK = "Ditolak";

	// Rekomendasi (Section D form)
	public static final String REKOM_DIREKOMENDASIKAN = "Direkomendasikan";
	public static final String REKOM_PERTIMBANGAN_ULANG = "Perlu pertimbangan ulang";
	public static final String REKOM_TIDAK = "Tidak direkomendasikan";

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private String kodeUnik;
	private String nama;          // Perihal / judul pengajuan
	private String keterangan;    // Latar belakang / catatan
	private String jenisPengadaan; // Jenis barang/jasa yang diadakan
	private Date tanggal;         // Tanggal pengajuan
	private Boolean aktif;
	private String status;

	private DisposisiSop disposisiSop;
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private Date tanggalPersetujuan;
	private Date tanggalPembuatan;

	// Bobot best-practice per kriteria (persen). Default sama-rata (~11 tiap kriteria).
	private Integer bobotHarga;
	private Integer bobotSpesifikasi;
	private Integer bobotKetersediaan;
	private Integer bobotKejelasan;
	private Integer bobotLegalitas;
	private Integer bobotPengalaman;
	private Integer bobotResponsif;
	private Integer bobotPembayaran;
	private Integer bobotReputasi;

	// Kolom "Ket." per kriteria (Section B)
	private String ketHarga;
	private String ketSpesifikasi;
	private String ketKetersediaan;
	private String ketKejelasan;
	private String ketLegalitas;
	private String ketPengalaman;
	private String ketResponsif;
	private String ketPembayaran;
	private String ketReputasi;

	// Section C - Ringkasan Perbandingan
	private String vendorPembanding1;
	private String vendorPembanding2;
	private String vendorPembanding3;
	private String alasanDipilih;

	// Section D - Rekomendasi
	private String rekomendasi;          // salah satu konstanta REKOM_*
	private Integer rekomendasiNomor;    // vendor nomor yang direkomendasikan (1..n)
	private Penyedia rekomendasiPenyedia;
	private String alasanUtama;

	// Section E - Penilai (dari pengguna disposisi tindak-lanjut)
	private String namaPenilai;
	private String jabatanPenilai;
	private Date tanggalPenilaian;
	private String ttd;

	public SeleksiVendor() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "kode", nullable = true)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "jenis_pengadaan", nullable = true)
	public String getJenisPengadaan() {
		return jenisPengadaan;
	}

	public void setJenisPengadaan(String jenisPengadaan) {
		this.jenisPengadaan = jenisPengadaan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	public Boolean getAktif() {
		if (getStatus().equals(SeleksiVendor.DISETUJU)) {
			aktif = true;
		}
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = disposisiSop;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PEMILIHAN_PENILAIAN_VENDOR_DATA;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}
		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		try {
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}
			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/library/SeleksiVendor.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		} else if (status != null && status.equals(DISETUJU)) {
			status = PENGAJUAN;
		}
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			status = DITOLAK;
		}
		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	// ---- Bobot best-practice (default sama-rata) ----
	@Column(name = "bobot_harga")
	public Integer getBobotHarga() { return bobotHarga == null ? 12 : bobotHarga; }
	public void setBobotHarga(Integer v) { this.bobotHarga = v; }

	@Column(name = "bobot_spesifikasi")
	public Integer getBobotSpesifikasi() { return bobotSpesifikasi == null ? 12 : bobotSpesifikasi; }
	public void setBobotSpesifikasi(Integer v) { this.bobotSpesifikasi = v; }

	@Column(name = "bobot_ketersediaan")
	public Integer getBobotKetersediaan() { return bobotKetersediaan == null ? 11 : bobotKetersediaan; }
	public void setBobotKetersediaan(Integer v) { this.bobotKetersediaan = v; }

	@Column(name = "bobot_kejelasan")
	public Integer getBobotKejelasan() { return bobotKejelasan == null ? 11 : bobotKejelasan; }
	public void setBobotKejelasan(Integer v) { this.bobotKejelasan = v; }

	@Column(name = "bobot_legalitas")
	public Integer getBobotLegalitas() { return bobotLegalitas == null ? 11 : bobotLegalitas; }
	public void setBobotLegalitas(Integer v) { this.bobotLegalitas = v; }

	@Column(name = "bobot_pengalaman")
	public Integer getBobotPengalaman() { return bobotPengalaman == null ? 11 : bobotPengalaman; }
	public void setBobotPengalaman(Integer v) { this.bobotPengalaman = v; }

	@Column(name = "bobot_responsif")
	public Integer getBobotResponsif() { return bobotResponsif == null ? 11 : bobotResponsif; }
	public void setBobotResponsif(Integer v) { this.bobotResponsif = v; }

	@Column(name = "bobot_pembayaran")
	public Integer getBobotPembayaran() { return bobotPembayaran == null ? 11 : bobotPembayaran; }
	public void setBobotPembayaran(Integer v) { this.bobotPembayaran = v; }

	@Column(name = "bobot_reputasi")
	public Integer getBobotReputasi() { return bobotReputasi == null ? 10 : bobotReputasi; }
	public void setBobotReputasi(Integer v) { this.bobotReputasi = v; }

	// ---- Keterangan per kriteria (Section B) ----
	@Column(name = "ket_harga") public String getKetHarga() { return ketHarga; }
	public void setKetHarga(String v) { this.ketHarga = v; }
	@Column(name = "ket_spesifikasi") public String getKetSpesifikasi() { return ketSpesifikasi; }
	public void setKetSpesifikasi(String v) { this.ketSpesifikasi = v; }
	@Column(name = "ket_ketersediaan") public String getKetKetersediaan() { return ketKetersediaan; }
	public void setKetKetersediaan(String v) { this.ketKetersediaan = v; }
	@Column(name = "ket_kejelasan") public String getKetKejelasan() { return ketKejelasan; }
	public void setKetKejelasan(String v) { this.ketKejelasan = v; }
	@Column(name = "ket_legalitas") public String getKetLegalitas() { return ketLegalitas; }
	public void setKetLegalitas(String v) { this.ketLegalitas = v; }
	@Column(name = "ket_pengalaman") public String getKetPengalaman() { return ketPengalaman; }
	public void setKetPengalaman(String v) { this.ketPengalaman = v; }
	@Column(name = "ket_responsif") public String getKetResponsif() { return ketResponsif; }
	public void setKetResponsif(String v) { this.ketResponsif = v; }
	@Column(name = "ket_pembayaran") public String getKetPembayaran() { return ketPembayaran; }
	public void setKetPembayaran(String v) { this.ketPembayaran = v; }
	@Column(name = "ket_reputasi") public String getKetReputasi() { return ketReputasi; }
	public void setKetReputasi(String v) { this.ketReputasi = v; }

	// ---- Section C ----
	@Column(name = "vendor_pembanding1") public String getVendorPembanding1() { return vendorPembanding1; }
	public void setVendorPembanding1(String v) { this.vendorPembanding1 = v; }
	@Column(name = "vendor_pembanding2") public String getVendorPembanding2() { return vendorPembanding2; }
	public void setVendorPembanding2(String v) { this.vendorPembanding2 = v; }
	@Column(name = "vendor_pembanding3") public String getVendorPembanding3() { return vendorPembanding3; }
	public void setVendorPembanding3(String v) { this.vendorPembanding3 = v; }

	@Column(name = "alasan_dipilih") public String getAlasanDipilih() { return alasanDipilih; }
	public void setAlasanDipilih(String v) { this.alasanDipilih = v; }

	// ---- Section D ----
	@Column(name = "rekomendasi") public String getRekomendasi() { return rekomendasi; }
	public void setRekomendasi(String v) { this.rekomendasi = v; }

	@Column(name = "rekomendasi_nomor") public Integer getRekomendasiNomor() { return rekomendasiNomor; }
	public void setRekomendasiNomor(Integer v) { this.rekomendasiNomor = v; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "rekomendasi_penyedia", nullable = true)
	public Penyedia getRekomendasiPenyedia() {
		rekomendasiPenyedia = check(rekomendasiPenyedia);
		return rekomendasiPenyedia;
	}

	public void setRekomendasiPenyedia(Penyedia rekomendasiPenyedia) {
		this.rekomendasiPenyedia = rekomendasiPenyedia;
	}

	@Column(name = "alasan_utama") public String getAlasanUtama() { return alasanUtama; }
	public void setAlasanUtama(String v) { this.alasanUtama = v; }

	// ---- Section E - Penilai ----
	@Column(name = "nama_penilai") public String getNamaPenilai() { return namaPenilai; }
	public void setNamaPenilai(String v) { this.namaPenilai = v; }

	@Column(name = "jabatan_penilai") public String getJabatanPenilai() { return jabatanPenilai; }
	public void setJabatanPenilai(String v) { this.jabatanPenilai = v; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_penilaian")
	public Date getTanggalPenilaian() { return tanggalPenilaian; }
	public void setTanggalPenilaian(Date v) { this.tanggalPenilaian = v; }

	@Column(name = "ttd") public String getTtd() { return ttd; }
	public void setTtd(String v) { this.ttd = v; }

}
