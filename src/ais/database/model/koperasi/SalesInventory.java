package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/**
 * Master Sales / Penjual Keliling (varian "eBisnis Inventory &amp; Sales", layar legacy 07
 * "Data Sales atau Penjual Keliling" -- lihat docs/pos-inventory-sales di repo zishof-platform,
 * ERD_DAN_SPESIFIKASI_DATA_NOTA_SALES_JAVA_AIS.md &sect;3.1).
 *
 * <p>Menghubungkan akun login existing ({@link Tbmuser}) dgn profil sales per {@link Toko} --
 * BUKAN pengganti {@code Pedagang}: user Sales boleh TIDAK punya baris Pedagang sama sekali
 * (dia bukan kasir toko), dan resolver konteks
 * ({@code ais.action.servlet.api.EbisnisActorContextResolver}) TIDAK pernah menyimpulkan
 * "tanpa Pedagang = admin" -- itulah alasan entity ini lahir di fase P1 (fondasi RBAC), bukan
 * baru di P2 (CRUD Master Sales).</p>
 *
 * <p>{@code kode} = kode sales legacy DBF (2 karakter, dipertahankan sbg TEKS termasuk nol di
 * depan -- rekonsiliasi arsip); {@code nomorPerkiraan} = "No. Perkiraan" legacy (akun COA per
 * sales) -- nullable sampai mapping COA disetujui UAT (uat-required.md #1), TIDAK dikarang.
 * Uang ({@code targetBulanan}/{@code limitPenagihan}) memakai {@link BigDecimal} sesuai
 * larangan &sect;2 PERINTAH_MASTER (bukan {@code Double} spt entity lama). Sales berhistori
 * DINONAKTIFKAN ({@code aktif=false}), tidak pernah dihapus fisik.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sales_inventory")
public class SalesInventory extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String kode;
	private String nama;
	private Tbmuser tbmuser;
	private Toko toko;
	private String nomorPerkiraan;
	private String area;
	private String telepon;
	private String alamat;
	private BigDecimal targetBulanan;
	private BigDecimal limitPenagihan;
	private Boolean aktif;
	private Long version;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SalesInventory() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** Kode sales legacy (teks, nol di depan dipertahankan) -- unik per toko (dicek di helper simpan). */
	@Column(name = "kode", length = 30)
	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama")
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Akun login sales -- nullable: master sales boleh dibuat dulu tanpa akun (legacy DBF tidak
	 * punya akun per sales), dipasangkan belakangan. Satu akun aktif maksimal SATU profil sales
	 * per toko (invariant dicek di helper simpan, bukan constraint DB, supaya data legacy yang
	 * kotor tetap bisa diimpor lalu dibereskan lewat exception queue).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser_id", nullable = true)
	public Tbmuser getTbmuser() {
		return tbmuser;
	}

	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/** "No. Perkiraan" legacy (akun COA sales) -- nullable sampai mapping COA disetujui (UAT_REQUIRED). */
	@Column(name = "nomor_perkiraan", length = 50)
	public String getNomorPerkiraan() {
		return nomorPerkiraan;
	}

	public void setNomorPerkiraan(String nomorPerkiraan) {
		this.nomorPerkiraan = nomorPerkiraan;
	}

	@Column(name = "area")
	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	@Column(name = "telepon", length = 50)
	public String getTelepon() {
		return telepon;
	}

	public void setTelepon(String telepon) {
		this.telepon = telepon;
	}

	@Column(name = "alamat", columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	@Column(name = "target_bulanan", precision = 19, scale = 2)
	public BigDecimal getTargetBulanan() {
		return targetBulanan == null ? BigDecimal.ZERO : targetBulanan;
	}

	public void setTargetBulanan(BigDecimal targetBulanan) {
		this.targetBulanan = targetBulanan;
	}

	@Column(name = "limit_penagihan", precision = 19, scale = 2)
	public BigDecimal getLimitPenagihan() {
		return limitPenagihan == null ? BigDecimal.ZERO : limitPenagihan;
	}

	public void setLimitPenagihan(BigDecimal limitPenagihan) {
		this.limitPenagihan = limitPenagihan;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** Optimistic locking (pola sama {@code Kegiatan.version}) -- master ini bisa diedit dari banyak perangkat. */
	@Version
	@Column(name = "version")
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
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

	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
