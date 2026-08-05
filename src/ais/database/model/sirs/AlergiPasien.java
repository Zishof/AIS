package ais.database.model.sirs;

// Blueprint Integrasi SIRS — Fase 2 (Fondasi data / keselamatan klinis). Entitas BARU, aditif.

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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Riwayat alergi / intoleransi pasien — analog "AllergyIntolerance" pada FHIR. Menutup gap
 * keselamatan klinis (C1 pada laporan analisis Fase 1): sebelumnya {@link Pasien} tidak memiliki
 * field alergi apa pun, sehingga peresepan tidak dapat memeriksa kontra-indikasi.
 *
 * <p>Setiap baris mencatat satu substansi alergen: kategori (obat/makanan/lingkungan), substansi
 * (teks bebas, opsional ditautkan ke {@link ItemMedis} bila alergi obat yang ada di katalog),
 * reaksi, tingkat keparahan, status klinis, tanggal pencatatan, dan pencatat.</p>
 *
 * <p><b>Keselamatan:</b> status klinis memakai {@code statusKlinis} (AKTIF/INAKTIF/RESOLVED) —
 * penonaktifan alergi dilakukan dengan MENGUBAH status, BUKAN menghapus baris. Envers menyimpan
 * seluruh riwayat perubahan (tanpa @NotAudited). Ini konsisten dengan prinsip blueprint: jangan
 * menghapus fisik / menghilangkan data alergi secara diam-diam.</p>
 *
 * <p><b>Skema:</b> tabel {@code sirs.alergi_pasien}; tabel audit dibuat otomatis oleh
 * {@code hbm2ddl.auto=update} + Envers di {@code new_audit.alergi_pasien__audit}. Kompatibel
 * Java 1.6/1.7 dan Hibernate 3.6.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "alergi_pasien")
public class AlergiPasien extends GeneralValueObject {

	// Kategori alergen
	public static final String KATEGORI_OBAT = "OBAT";
	public static final String KATEGORI_MAKANAN = "MAKANAN";
	public static final String KATEGORI_LINGKUNGAN = "LINGKUNGAN";
	public static final String KATEGORI_LAINNYA = "LAINNYA";

	// Tingkat keparahan
	public static final String KEPARAHAN_RINGAN = "RINGAN";
	public static final String KEPARAHAN_SEDANG = "SEDANG";
	public static final String KEPARAHAN_BERAT = "BERAT";

	// Status klinis
	public static final String STATUS_AKTIF = "AKTIF";
	public static final String STATUS_INAKTIF = "INAKTIF";
	public static final String STATUS_RESOLVED = "RESOLVED";

	private static final long serialVersionUID = 4820100719000000022L;

	private Long id;
	private String olehId;
	private String oleh;
	private Date tanggal_dirubah = new Date();

	private Pasien pasien;
	private ItemMedis itemMedis; // opsional: bila alergi obat yang ada di katalog

	private String kategori;
	private String substansi;
	private String reaksi;
	private String keparahan;
	private String statusKlinis = STATUS_AKTIF;
	private Date tanggalCatat = new Date();
	private String pencatat;
	private String keterangan;

	public AlergiPasien() {
	}

	public String toString() {
		return substansi == null ? "" : substansi;
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

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pasien", nullable = true)
	public Pasien getPasien() {
		pasien = check(pasien);
		return pasien;
	}

	public void setPasien(Pasien pasien) {
		this.pasien = pasien;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_medis", nullable = true)
	public ItemMedis getItemMedis() {
		itemMedis = check(itemMedis);
		return itemMedis;
	}

	public void setItemMedis(ItemMedis itemMedis) {
		this.itemMedis = itemMedis;
	}

	@Column(name = "kategori", nullable = true, length = 30)
	public String getKategori() {
		return kategori;
	}

	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	@Column(name = "substansi", nullable = true, length = 100)
	public String getSubstansi() {
		return substansi;
	}

	public void setSubstansi(String substansi) {
		this.substansi = substansi;
	}

	@Column(name = "reaksi", nullable = true)
	public String getReaksi() {
		return reaksi;
	}

	public void setReaksi(String reaksi) {
		this.reaksi = reaksi;
	}

	@Column(name = "keparahan", nullable = true, length = 20)
	public String getKeparahan() {
		return keparahan;
	}

	public void setKeparahan(String keparahan) {
		this.keparahan = keparahan;
	}

	@Column(name = "status_klinis", nullable = true, length = 20)
	public String getStatusKlinis() {
		return (statusKlinis == null || statusKlinis.trim().isEmpty()) ? STATUS_AKTIF : statusKlinis;
	}

	public void setStatusKlinis(String statusKlinis) {
		this.statusKlinis = statusKlinis;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_catat", nullable = true)
	public Date getTanggalCatat() {
		return tanggalCatat;
	}

	public void setTanggalCatat(Date tanggalCatat) {
		this.tanggalCatat = tanggalCatat;
	}

	@Column(name = "pencatat", nullable = true, length = 60)
	public String getPencatat() {
		return pencatat;
	}

	public void setPencatat(String pencatat) {
		this.pencatat = pencatat;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Derived, bukan kolom DB — WAJIB @Transient agar Hibernate tidak mencari setter "aktif". */
	@Transient
	public boolean isAktif() {
		return STATUS_AKTIF.equals(getStatusKlinis());
	} 
}
