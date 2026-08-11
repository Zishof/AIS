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
 * Sales Order lapangan (varian "eBisnis Inventory &amp; Sales", layar legacy 30 "Menu
 * Penjualan" -- ERD &sect;3.6). Order BUKAN invoice: piutang baru lahir saat order
 * di-posting jadi {@link PiutangCustomerDoc} (aksi {@code si_sales_order_invoice},
 * status {@link #STATUS_SIAP_TAGIH}) -- sesuai mapping layar 30 "jangan samakan order
 * dengan invoice".
 *
 * <p>Status "Mode Sales Lapangan" persis permintaan mapping: {@link #STATUS_PESAN} &rarr;
 * {@link #STATUS_SIAP_KIRIM} &rarr; {@link #STATUS_TERKIRIM} &rarr; {@link #STATUS_SIAP_TAGIH};
 * ditambah {@link #STATUS_DRAFT} (belum dikonfirmasi), {@link #STATUS_LUNAS} (turunan dari
 * pelunasan piutang), {@link #STATUS_BATAL} (soft-cancel, dokumen tidak pernah dihapus fisik).
 * Transisi divalidasi server ({@code SalesInventoryReceivableHelper.salesOrderStatus}).</p>
 *
 * <p>KEPUTUSAN P4 (D-13, dicatat di docs/pos-inventory-sales/02-decisions.md): TERKIRIM
 * TIDAK menggerakkan stok di fase ini -- movement fisik barang sales dicatat lewat SPJ
 * "barang dibawa" (P5, TRIP-002) supaya tidak dobel-hitung saat kedua fitur digabung.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sales_order_lapangan")
public class SalesOrderLapangan extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_DRAFT = "DRAFT";
	public static final String STATUS_PESAN = "PESAN";
	public static final String STATUS_SIAP_KIRIM = "SIAP_KIRIM";
	public static final String STATUS_TERKIRIM = "TERKIRIM";
	public static final String STATUS_SIAP_TAGIH = "SIAP_TAGIH";
	public static final String STATUS_LUNAS = "LUNAS";
	public static final String STATUS_BATAL = "BATAL";

	private Long id;
	private String nomor;
	private Toko toko;
	private AnggotaKoperasi customer;
	private SalesInventory sales;
	private Date tanggal;
	private String status;
	private BigDecimal total;
	private String keterangan;
	private String alasanBatal;
	private String kodeUnik;
	private Tbmuser dibuatOleh;
	private Long version;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SalesOrderLapangan() {
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

	/** Nomor dokumen (teks) -- diisi SETELAH insert dari id ({@code SO-{toko}-{id 6 digit}}):
	 *  unik tanpa MAX+1 dan tanpa sequence tambahan (larangan ERD &sect;1.6). */
	@Column(name = "nomor", length = 60, unique = true)
	public String getNomor() {
		return nomor;
	}

	public void setNomor(String nomor) {
		this.nomor = nomor;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer", nullable = false)
	public AnggotaKoperasi getCustomer() {
		return customer;
	}

	public void setCustomer(AnggotaKoperasi customer) {
		this.customer = customer;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales")
	public SalesInventory getSales() {
		return sales;
	}

	public void setSales(SalesInventory sales) {
		this.sales = sales;
	}

	/** Tanggal bisnis order (bukan timestamp teknis -- itu {@link #getWaktu()}). */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_DRAFT : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/** Total order (denormal dari &Sigma; subtotal item, di-recompute tiap simpan item --
	 *  untuk list/aging tanpa join; sumber kebenaran tetap item). */
	@Column(name = "total", precision = 19, scale = 2)
	public BigDecimal getTotal() {
		return total == null ? BigDecimal.ZERO : total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "alasan_batal", columnDefinition = "text")
	public String getAlasanBatal() {
		return alasanBatal;
	}

	public void setAlasanBatal(String alasanBatal) {
		this.alasanBatal = alasanBatal;
	}

	/** Kunci idempoten create dari klien (UUID) -- retry ganda mengembalikan order pertama. */
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
