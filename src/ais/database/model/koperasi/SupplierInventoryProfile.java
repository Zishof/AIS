package ais.database.model.koperasi;

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
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.library.Penyedia;

/**
 * Profil supplier varian "eBisnis Inventory &amp; Sales" (layar legacy 01-03 "Data Supplier",
 * SUPPLIER.DBF) -- EXTENSION di atas {@link Penyedia} existing, BUKAN kolom baru di Penyedia
 * (aturan ERD &sect;1.2: jangan menambah kolom ke entity existing bila semantiknya modul lain).
 * Field legacy yang TIDAK ada di Penyedia hidup di sini: termin (SYARAT_BYR, dasar jatuh
 * tempo hutang), wilayah, rekening bank (REKRUPIAH/ATASNAMA/NAMABANK/ALMBANK), dan status
 * aktif (master berhistori DINONAKTIFKAN, tidak dihapus fisik).
 *
 * <p>Satu Penyedia maksimal SATU profil (invariant dijaga di helper simpan, bukan unique
 * constraint DB -- supaya data impor legacy yang kotor bisa masuk dulu lalu dibereskan).
 * Saldo hutang supplier TIDAK disimpan di sini -- selalu dihitung dari ledger pembelian/
 * pembayaran (baca-saja, layar 22-27 fase P3).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "supplier_inventory_profile")
public class SupplierInventoryProfile extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Penyedia penyedia;
	private Integer terminHari;
	private String wilayah;
	private String noRekening;
	private String atasNama;
	private String bank;
	private String alamatBank;
	private Boolean aktif;
	private Long version;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SupplierInventoryProfile() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penyedia", nullable = false)
	public Penyedia getPenyedia() {
		return penyedia;
	}

	public void setPenyedia(Penyedia penyedia) {
		this.penyedia = penyedia;
	}

	/** Termin pembayaran (hari) -- SYARAT_BYR legacy; dasar perhitungan jatuh tempo hutang. */
	@Column(name = "termin_hari")
	public Integer getTerminHari() {
		return terminHari == null ? Integer.valueOf(0) : terminHari;
	}

	public void setTerminHari(Integer terminHari) {
		this.terminHari = terminHari;
	}

	@Column(name = "wilayah")
	public String getWilayah() {
		return wilayah;
	}

	public void setWilayah(String wilayah) {
		this.wilayah = wilayah;
	}

	@Column(name = "no_rekening", length = 60)
	public String getNoRekening() {
		return noRekening;
	}

	public void setNoRekening(String noRekening) {
		this.noRekening = noRekening;
	}

	@Column(name = "atas_nama")
	public String getAtasNama() {
		return atasNama;
	}

	public void setAtasNama(String atasNama) {
		this.atasNama = atasNama;
	}

	@Column(name = "bank", length = 100)
	public String getBank() {
		return bank;
	}

	public void setBank(String bank) {
		this.bank = bank;
	}

	@Column(name = "alamat_bank", columnDefinition = "text")
	public String getAlamatBank() {
		return alamatBank;
	}

	public void setAlamatBank(String alamatBank) {
		this.alamatBank = alamatBank;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

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
