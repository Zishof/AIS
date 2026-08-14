package ais.database.model.library;

// Detail per-vendor untuk pengajuan Seleksi Vendor.
// Setiap baris = satu vendor (berelasi ke Penyedia) + data manual + 9 skor kriteria (1..5).

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Detail vendor pada Seleksi Vendor.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "seleksi_vendor_detail")
public class SeleksiVendorDetail extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439811L;

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
		return namaVendor;
	}

	private SeleksiVendor seleksiVendor;
	private Penyedia penyedia;
	private Integer urutan;         // Vendor I/II/III -> 1,2,3

	// Section A - data vendor (manual, boleh override dari Penyedia)
	private String namaVendor;
	private String alamatKontak;
	private String jenisBarangJasa;
	private String picVendor;

	// Section B - 9 skor kriteria (1..5)
	private Integer nilaiHarga;
	private Integer nilaiSpesifikasi;
	private Integer nilaiKetersediaan;
	private Integer nilaiKejelasan;
	private Integer nilaiLegalitas;
	private Integer nilaiPengalaman;
	private Integer nilaiResponsif;
	private Integer nilaiPembayaran;
	private Integer nilaiReputasi;

	public SeleksiVendorDetail() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "seleksi_vendor", nullable = true)
	public SeleksiVendor getSeleksiVendor() {
		return seleksiVendor;
	}

	public void setSeleksiVendor(SeleksiVendor seleksiVendor) {
		this.seleksiVendor = seleksiVendor;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyedia", nullable = true)
	public Penyedia getPenyedia() {
		penyedia = check(penyedia);
		return penyedia;
	}

	public void setPenyedia(Penyedia penyedia) {
		this.penyedia = penyedia;
	}

	@Column(name = "urutan")
	public Integer getUrutan() {
		return urutan == null ? 0 : urutan;
	}

	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}

	@Column(name = "nama_vendor", length = 255)
	public String getNamaVendor() {
		if ((namaVendor == null || namaVendor.trim().isEmpty()) && getPenyedia() != null) {
			return getPenyedia().getNama();
		}
		return namaVendor;
	}

	public void setNamaVendor(String namaVendor) {
		this.namaVendor = namaVendor;
	}

	@Column(name = "alamat_kontak")
	public String getAlamatKontak() {
		if ((alamatKontak == null || alamatKontak.trim().isEmpty()) && getPenyedia() != null) {
			String a = getPenyedia().getAlamat();
			String t = getPenyedia().getTelp();
			String gabung = (a == null ? "" : a) + (t == null || t.trim().isEmpty() ? "" : " / " + t);
			return gabung.trim().isEmpty() ? null : gabung.trim();
		}
		return alamatKontak;
	}

	public void setAlamatKontak(String alamatKontak) {
		this.alamatKontak = alamatKontak;
	}

	@Column(name = "jenis_barang_jasa")
	public String getJenisBarangJasa() {
		return jenisBarangJasa;
	}

	public void setJenisBarangJasa(String jenisBarangJasa) {
		this.jenisBarangJasa = jenisBarangJasa;
	}

	@Column(name = "pic_vendor")
	public String getPicVendor() {
		if ((picVendor == null || picVendor.trim().isEmpty()) && getPenyedia() != null) {
			return getPenyedia().getKontak();
		}
		return picVendor;
	}

	public void setPicVendor(String picVendor) {
		this.picVendor = picVendor;
	}

	@Column(name = "nilai_harga") public Integer getNilaiHarga() { return nilaiHarga; }
	public void setNilaiHarga(Integer v) { this.nilaiHarga = v; }
	@Column(name = "nilai_spesifikasi") public Integer getNilaiSpesifikasi() { return nilaiSpesifikasi; }
	public void setNilaiSpesifikasi(Integer v) { this.nilaiSpesifikasi = v; }
	@Column(name = "nilai_ketersediaan") public Integer getNilaiKetersediaan() { return nilaiKetersediaan; }
	public void setNilaiKetersediaan(Integer v) { this.nilaiKetersediaan = v; }
	@Column(name = "nilai_kejelasan") public Integer getNilaiKejelasan() { return nilaiKejelasan; }
	public void setNilaiKejelasan(Integer v) { this.nilaiKejelasan = v; }
	@Column(name = "nilai_legalitas") public Integer getNilaiLegalitas() { return nilaiLegalitas; }
	public void setNilaiLegalitas(Integer v) { this.nilaiLegalitas = v; }
	@Column(name = "nilai_pengalaman") public Integer getNilaiPengalaman() { return nilaiPengalaman; }
	public void setNilaiPengalaman(Integer v) { this.nilaiPengalaman = v; }
	@Column(name = "nilai_responsif") public Integer getNilaiResponsif() { return nilaiResponsif; }
	public void setNilaiResponsif(Integer v) { this.nilaiResponsif = v; }
	@Column(name = "nilai_pembayaran") public Integer getNilaiPembayaran() { return nilaiPembayaran; }
	public void setNilaiPembayaran(Integer v) { this.nilaiPembayaran = v; }
	@Column(name = "nilai_reputasi") public Integer getNilaiReputasi() { return nilaiReputasi; }
	public void setNilaiReputasi(Integer v) { this.nilaiReputasi = v; }

	private static int n(Integer v) { return v == null ? 0 : v.intValue(); }

	/** Jumlah skor mentah (maks 45). */
	@Transient
	public Integer getTotalNilai() {
		return n(nilaiHarga) + n(nilaiSpesifikasi) + n(nilaiKetersediaan) + n(nilaiKejelasan)
				+ n(nilaiLegalitas) + n(nilaiPengalaman) + n(nilaiResponsif) + n(nilaiPembayaran)
				+ n(nilaiReputasi);
	}

	/** Skor tertimbang best-practice = Sigma(nilai_i * bobot_i) / 5, skala 0..100. */
	@Transient
	public Double getSkorTertimbang() {
		SeleksiVendor h = getSeleksiVendor();
		if (h == null) {
			// tanpa header: rata-rata sederhana skala 0..100
			return getTotalNilai() * 100.0 / 45.0;
		}
		double total = n(nilaiHarga) * h.getBobotHarga() + n(nilaiSpesifikasi) * h.getBobotSpesifikasi()
				+ n(nilaiKetersediaan) * h.getBobotKetersediaan() + n(nilaiKejelasan) * h.getBobotKejelasan()
				+ n(nilaiLegalitas) * h.getBobotLegalitas() + n(nilaiPengalaman) * h.getBobotPengalaman()
				+ n(nilaiResponsif) * h.getBobotResponsif() + n(nilaiPembayaran) * h.getBobotPembayaran()
				+ n(nilaiReputasi) * h.getBobotReputasi();
		// bobot dalam persen (total ~100), nilai maks 5 -> bagi 5 untuk skala 0..100
		return Math.round((total / 5.0) * 100.0) / 100.0;
	}

}
