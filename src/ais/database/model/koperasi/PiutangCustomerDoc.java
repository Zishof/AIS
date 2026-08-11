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
import ais.database.model.inventory.Toko;

/**
 * Dokumen piutang customer / invoice AR (layar legacy 31-33, varian Inventory &amp; Sales) --
 * cermin AP {@link PayableFakturInfo} di sisi penjualan. Lahir dari posting
 * {@link SalesOrderLapangan} ({@code si_sales_order_invoice}) atau entri manual pemilik
 * (faktur lama/migrasi legacy TRAN_PIU.DBF menyusul).
 *
 * <p>OUTSTANDING TIDAK DISIMPAN -- selalu dihitung: {@code totalFaktur - dibayarAwal -
 * SUM(alokasi penerimaan)} (register event, pola sama persis ledger AP P3 -- pelunasan tidak
 * pernah menghapus/menimpa dokumen; filter "lunas" murni visual, layar 33).</p>
 *
 * <p>CATATAN sub-ledger (D-12): piutang POS existing (belanja kasir ber-cara-bayar
 * "masuk sebagai hutang" &minus; pembayaran_hutang) adalah ledger TERPISAH yang sudah punya
 * layar Mutasi Hutang sendiri; saldo customer gabungan = ledger POS + outstanding dokumen ini
 * (keduanya ditampilkan terpisah, tidak dicampur -- tanpa duplikasi pencatatan).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "piutang_customer_doc")
public class PiutangCustomerDoc extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_AKTIF = "AKTIF";
	public static final String STATUS_BATAL = "BATAL";

	private Long id;
	private String nomor;
	private Toko toko;
	private AnggotaKoperasi customer;
	private SalesInventory sales;
	private SalesOrderLapangan salesOrder;
	private Date tanggal;
	private Integer terminHari;
	private Date jatuhTempo;
	private BigDecimal totalFaktur;
	private BigDecimal dibayarAwal;
	private String status;
	private String keterangan;
	private String alasanBatal;
	private String kodeUnik;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PiutangCustomerDoc() {
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

	/** Nomor faktur (teks) -- diisi pasca-insert dari id ({@code INV-{toko}-{id 6 digit}}),
	 *  unik tanpa MAX+1. */
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

	/** Order asal (nullable -- dokumen manual/migrasi tidak punya order). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order")
	public SalesOrderLapangan getSalesOrder() {
		return salesOrder;
	}

	public void setSalesOrder(SalesOrderLapangan salesOrder) {
		this.salesOrder = salesOrder;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(name = "termin_hari")
	public Integer getTerminHari() {
		return terminHari == null ? Integer.valueOf(0) : terminHari;
	}

	public void setTerminHari(Integer terminHari) {
		this.terminHari = terminHari;
	}

	/** Jatuh tempo = tanggal + termin, kolom sendiri (query aging tanpa join, bisa dikoreksi). */
	@Temporal(TemporalType.DATE)
	@Column(name = "jatuh_tempo")
	public Date getJatuhTempo() {
		return jatuhTempo;
	}

	public void setJatuhTempo(Date jatuhTempo) {
		this.jatuhTempo = jatuhTempo;
	}

	@Column(name = "total_faktur", precision = 19, scale = 2)
	public BigDecimal getTotalFaktur() {
		return totalFaktur == null ? BigDecimal.ZERO : totalFaktur;
	}

	public void setTotalFaktur(BigDecimal totalFaktur) {
		this.totalFaktur = totalFaktur;
	}

	/** Dibayar saat faktur terbit (uang muka/tunai sebagian) -- bukan hasil penagihan. */
	@Column(name = "dibayar_awal", precision = 19, scale = 2)
	public BigDecimal getDibayarAwal() {
		return dibayarAwal == null ? BigDecimal.ZERO : dibayarAwal;
	}

	public void setDibayarAwal(BigDecimal dibayarAwal) {
		this.dibayarAwal = dibayarAwal;
	}

	@Column(name = "status", length = 20)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_AKTIF : status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	/** Kunci idempoten create (UUID klien / turunan order) -- retry ganda aman. */
	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
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
