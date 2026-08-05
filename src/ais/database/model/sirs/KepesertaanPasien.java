package ais.database.model.sirs;

// Blueprint Integrasi SIRS — Fase 2 (Fondasi data). Entitas BARU, aditif.

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

import ais.database.model.GeneralValueObject;

/**
 * Kepesertaan / jaminan pasien terhadap seorang payer ({@link Asuransi}) — analog "Coverage"
 * pada FHIR. Menyimpan detail eligibilitas yang TIDAK dapat ditampung oleh {@code Pasien.asuransi}
 * atau {@code Pendaftaran.asuransi} saja: nomor kepesertaan, kelas hak rawat, faskes tingkat 1,
 * jenis peserta, masa berlaku, urutan penjamin (untuk COB/Coordination of Benefits), serta
 * status & sumber verifikasi.
 *
 * <p><b>Kompatibilitas:</b> entitas ini ADITIF. {@link Asuransi} TETAP master payer tunggal dan
 * {@code Pendaftaran.asuransi} / {@code Pasien.asuransi} TETAP dipakai apa adanya. Kepesertaan ini
 * memperkaya (bukan menggantikan) data payer, dan diisi bertahap. Baris kepesertaan boleh bersifat
 * level-pasien (field {@code pendaftaran} null) atau ditangkap saat sebuah encounter tertentu
 * (field {@code pendaftaran} terisi, sekadar jejak dari mana data direkam).</p>
 *
 * <p><b>Skema:</b> tabel {@code sirs.kepesertaan_pasien}; tabel audit dibuat otomatis oleh
 * {@code hbm2ddl.auto=update} + Envers di {@code new_audit.kepesertaan_pasien__audit} (entitas
 * baru → tabel utama dan audit dibuat sekaligus saat startup). @Audited penuh, tanpa @NotAudited.</p>
 *
 * <p>Semua relasi memakai pola {@code @ManyToOne(cascade={PERSIST,MERGE}, fetch=LAZY)} yang seragam
 * dengan seluruh model SIRS (child-owned FK, tanpa @OneToMany/REMOVE). Kompatibel Java 1.6/1.7 dan
 * Hibernate 3.6.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "kepesertaan_pasien")
public class KepesertaanPasien extends GeneralValueObject {

	// Jenis peserta (contoh nilai BPJS/JKN — bebas diperluas dari master di masa depan)
	public static final String PESERTA_PBI = "PBI"; // Penerima Bantuan Iuran
	public static final String PESERTA_PPU = "PPU"; // Pekerja Penerima Upah
	public static final String PESERTA_PBPU = "PBPU"; // Pekerja Bukan Penerima Upah
	public static final String PESERTA_BP = "BP"; // Bukan Pekerja

	// Sumber verifikasi eligibilitas
	public static final String VERIF_MANUAL = "MANUAL";
	public static final String VERIF_VCLAIM = "VCLAIM";
	public static final String VERIF_PCARE = "PCARE";

	private static final long serialVersionUID = 4820100719000000021L;

	private Long id;
	private String olehId;
	private String oleh;
	private Date tanggal_dirubah = new Date();

	private Pasien pasien;
	private Asuransi asuransi;
	private Pendaftaran pendaftaran;

	private String nomorKepesertaan;
	private String jenisPeserta;
	private String kelasHak;
	private String faskesTk1Kode;
	private String faskesTk1Nama;
	private Date mulaiBerlaku;
	private Date akhirBerlaku;
	private Boolean statusAktif = Boolean.TRUE;
	private Integer urutanPenjamin; // 1 = penjamin utama, 2 = penjamin kedua (COB)
	private Boolean cob = Boolean.FALSE; // Coordination of Benefits
	private String sumberVerifikasi;
	private Date tanggalVerifikasi;
	private String keterangan;

	public KepesertaanPasien() {
	}

	public String toString() {
		String payer = asuransi == null ? "" : asuransi.getNama();
		String no = nomorKepesertaan == null ? "" : nomorKepesertaan;
		return (payer + " " + no).trim();
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
	@JoinColumn(name = "asuransi", nullable = true)
	public Asuransi getAsuransi() {
		asuransi = check(asuransi);
		return asuransi;
	}

	public void setAsuransi(Asuransi asuransi) {
		this.asuransi = asuransi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftaran", nullable = true)
	public Pendaftaran getPendaftaran() {
		pendaftaran = check(pendaftaran);
		return pendaftaran;
	}

	public void setPendaftaran(Pendaftaran pendaftaran) {
		this.pendaftaran = pendaftaran;
	}

	@Column(name = "nomor_kepesertaan", nullable = true, length = 30)
	public String getNomorKepesertaan() {
		return nomorKepesertaan;
	}

	public void setNomorKepesertaan(String nomorKepesertaan) {
		this.nomorKepesertaan = nomorKepesertaan;
	}

	@Column(name = "jenis_peserta", nullable = true, length = 40)
	public String getJenisPeserta() {
		return jenisPeserta;
	}

	public void setJenisPeserta(String jenisPeserta) {
		this.jenisPeserta = jenisPeserta;
	}

	@Column(name = "kelas_hak", nullable = true, length = 15)
	public String getKelasHak() {
		return kelasHak;
	}

	public void setKelasHak(String kelasHak) {
		this.kelasHak = kelasHak;
	}

	@Column(name = "faskes_tk1_kode", nullable = true, length = 30)
	public String getFaskesTk1Kode() {
		return faskesTk1Kode;
	}

	public void setFaskesTk1Kode(String faskesTk1Kode) {
		this.faskesTk1Kode = faskesTk1Kode;
	}

	@Column(name = "faskes_tk1_nama", nullable = true, length = 100)
	public String getFaskesTk1Nama() {
		return faskesTk1Nama;
	}

	public void setFaskesTk1Nama(String faskesTk1Nama) {
		this.faskesTk1Nama = faskesTk1Nama;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "mulai_berlaku", nullable = true)
	public Date getMulaiBerlaku() {
		return mulaiBerlaku;
	}

	public void setMulaiBerlaku(Date mulaiBerlaku) {
		this.mulaiBerlaku = mulaiBerlaku;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "akhir_berlaku", nullable = true)
	public Date getAkhirBerlaku() {
		return akhirBerlaku;
	}

	public void setAkhirBerlaku(Date akhirBerlaku) {
		this.akhirBerlaku = akhirBerlaku;
	}

	@Column(name = "status_aktif", nullable = true)
	public Boolean getStatusAktif() {
		return statusAktif == null ? Boolean.TRUE : statusAktif;
	}

	public void setStatusAktif(Boolean statusAktif) {
		this.statusAktif = statusAktif;
	}

	@Column(name = "urutan_penjamin", nullable = true)
	public Integer getUrutanPenjamin() {
		return urutanPenjamin;
	}

	public void setUrutanPenjamin(Integer urutanPenjamin) {
		this.urutanPenjamin = urutanPenjamin;
	}

	@Column(name = "cob", nullable = true)
	public Boolean getCob() {
		return cob == null ? Boolean.FALSE : cob;
	}

	public void setCob(Boolean cob) {
		this.cob = cob;
	}

	@Column(name = "sumber_verifikasi", nullable = true, length = 30)
	public String getSumberVerifikasi() {
		return sumberVerifikasi;
	}

	public void setSumberVerifikasi(String sumberVerifikasi) {
		this.sumberVerifikasi = sumberVerifikasi;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_verifikasi", nullable = true)
	public Date getTanggalVerifikasi() {
		return tanggalVerifikasi;
	}

	public void setTanggalVerifikasi(Date tanggalVerifikasi) {
		this.tanggalVerifikasi = tanggalVerifikasi;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** True bila kepesertaan aktif & (bila diisi) tanggal sekarang berada dalam masa berlaku. */
	public boolean berlakuPada(Date tanggal) {
		if (!getStatusAktif().booleanValue()) {
			return false;
		}
		if (tanggal == null) {
			return true;
		}
		if (mulaiBerlaku != null && tanggal.before(mulaiBerlaku)) {
			return false;
		}
		if (akhirBerlaku != null && tanggal.after(akhirBerlaku)) {
			return false;
		}
		return true;
	}
}
