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

/**
 * Profil customer varian "eBisnis Inventory &amp; Sales" (layar legacy 04-06 "Data Customer",
 * LANGGANAN.DBF) -- EXTENSION di atas {@link AnggotaKoperasi} existing (identitas: kode 5
 * karakter teks/nama/alamat/telepon TETAP milik AnggotaKoperasi; jangan duplikat). Field
 * khusus distribusi/sales hidup di sini: termin, diskon default, wilayah, sales pembina
 * ({@link SalesInventory}), rekening bank. Member retail POS TIDAK otomatis punya profil ini
 * (aturan ERD &sect;6.2: jangan samakan member retail dgn customer distributor tanpa profil
 * eksplisit).
 *
 * <p>Saldo piutang TIDAK disimpan -- baca-saja dihitung dari ledger existing (belanja
 * ber-cara-bayar {@code masuk_sebagai_hutang} dikurangi {@code pembayaran_hutang}, formula
 * yang sama dgn {@code KantinHelper.mutasiHutangList}).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "customer_inventory_profile")
public class CustomerInventoryProfile extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private AnggotaKoperasi anggotaKoperasi;
	private SalesInventory salesOwner;
	private Integer terminHari;
	private BigDecimal diskonDefaultPersen;
	private String wilayah;
	private String noRekening;
	private String atasNama;
	private String bank;
	private Boolean aktif;
	private Long version;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public CustomerInventoryProfile() {
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
	@JoinColumn(name = "anggota_koperasi", nullable = false)
	public AnggotaKoperasi getAnggotaKoperasi() {
		return anggotaKoperasi;
	}

	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/** Sales pembina customer ini -- snapshot pada faktur historis TIDAK ikut berubah bila diganti. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_owner", nullable = true)
	public SalesInventory getSalesOwner() {
		return salesOwner;
	}

	public void setSalesOwner(SalesInventory salesOwner) {
		this.salesOwner = salesOwner;
	}

	@Column(name = "termin_hari")
	public Integer getTerminHari() {
		return terminHari == null ? Integer.valueOf(0) : terminHari;
	}

	public void setTerminHari(Integer terminHari) {
		this.terminHari = terminHari;
	}

	@Column(name = "diskon_default_persen", precision = 7, scale = 3)
	public BigDecimal getDiskonDefaultPersen() {
		return diskonDefaultPersen == null ? BigDecimal.ZERO : diskonDefaultPersen;
	}

	public void setDiskonDefaultPersen(BigDecimal diskonDefaultPersen) {
		this.diskonDefaultPersen = diskonDefaultPersen;
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
