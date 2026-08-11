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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Penerimaan piutang customer / collection (layar legacy 34-36) -- cermin AP
 * {@link PembayaranHutangSupplier}: satu penerimaan boleh melunasi banyak faktur lewat
 * {@link AlokasiPenerimaanPiutangCustomer} (full/partial/multi-invoice, Matriks layar 34);
 * {@code kodeUnik} idempoten (retry offline outbox P7 aman); koreksi = dokumen pembalik
 * (REVERSAL menyusul), bukan hapus.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "penerimaan_piutang_customer")
public class PenerimaanPiutangCustomer extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String METODE_TUNAI = "TUNAI";
	public static final String METODE_TRANSFER = "TRANSFER";
	public static final String METODE_GIRO = "GIRO";
	public static final String METODE_DISCOUNT = "DISCOUNT";
	public static final String METODE_RETUR = "RETUR";

	private Long id;
	private String nomor;
	private AnggotaKoperasi customer;
	private SalesInventory sales;
	private Date tanggal;
	private BigDecimal nominal;
	private String metode;
	private String noBg;
	private String namaBank;
	private Date tanggalBg;
	private String keterangan;
	private String kodeUnik;
	private Tbmuser dibuatOleh;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PenerimaanPiutangCustomer() {
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

	/** Nomor kwitansi (teks) -- diisi pasca-insert dari id ({@code KWT-{id 6 digit}}). */
	@Column(name = "nomor", length = 60, unique = true)
	public String getNomor() {
		return nomor;
	}

	public void setNomor(String nomor) {
		this.nomor = nomor;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer", nullable = false)
	public AnggotaKoperasi getCustomer() {
		return customer;
	}

	public void setCustomer(AnggotaKoperasi customer) {
		this.customer = customer;
	}

	/** Sales penagih (nullable -- penerimaan langsung di kantor tanpa sales lapangan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales")
	public SalesInventory getSales() {
		return sales;
	}

	public void setSales(SalesInventory sales) {
		this.sales = sales;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(name = "nominal", precision = 19, scale = 2)
	public BigDecimal getNominal() {
		return nominal == null ? BigDecimal.ZERO : nominal;
	}

	public void setNominal(BigDecimal nominal) {
		this.nominal = nominal;
	}

	@Column(name = "metode", length = 20)
	public String getMetode() {
		return metode == null || metode.trim().isEmpty() ? METODE_TUNAI : metode;
	}

	public void setMetode(String metode) {
		this.metode = metode;
	}

	@Column(name = "no_bg", length = 60)
	public String getNoBg() {
		return noBg;
	}

	public void setNoBg(String noBg) {
		this.noBg = noBg;
	}

	@Column(name = "nama_bank", length = 100)
	public String getNamaBank() {
		return namaBank;
	}

	public void setNamaBank(String namaBank) {
		this.namaBank = namaBank;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_bg")
	public Date getTanggalBg() {
		return tanggalBg;
	}

	public void setTanggalBg(Date tanggalBg) {
		this.tanggalBg = tanggalBg;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Kunci idempoten dari klien (UUID) -- duplicate retry mengembalikan penerimaan pertama. */
	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh")
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
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
