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
 * Surat Perintah Sales Jalan / SPJ (layar legacy 39, ERD &sect;3.2) -- pusat assignment
 * barang dibawa ({@link SpjSalesBarang}) dan nota/invoice dibawa ({@link SpjSalesNota})
 * untuk satu keberangkatan sales. State machine (ERD &sect;6):
 * {@code DRAFT -> SUBMITTED -> APPROVED -> ACTIVE -> RETURNED -> RECONCILING -> CLOSED};
 * {@code DRAFT/SUBMITTED -> CANCELLED}; {@code APPROVED -> CANCELLED} hanya sebelum
 * berangkat dan wajib beralasan. Realisasinya = satu {@link NotaSalesSession} (dibuat
 * saat mulai jalan). Nomor {@code SPJ-{toko}-{id 6 digit}} pasca-insert (tanpa MAX+1).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "surat_perintah_sales_jalan")
public class SuratPerintahSalesJalan extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_DRAFT = "DRAFT";
	public static final String STATUS_SUBMITTED = "SUBMITTED";
	public static final String STATUS_APPROVED = "APPROVED";
	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_RETURNED = "RETURNED";
	public static final String STATUS_RECONCILING = "RECONCILING";
	public static final String STATUS_CLOSED = "CLOSED";
	public static final String STATUS_CANCELLED = "CANCELLED";

	private Long id;
	private String nomor;
	private Toko toko;
	private SalesInventory sales;
	private Date tanggalBerangkatRencana;
	private Date tanggalMulaiAktual;
	private Date tanggalKembaliAktual;
	private String rute;
	private String kendaraan;
	private BigDecimal uangMukaOperasional;
	private String catatan;
	private String status;
	private String alasanBatal;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private String kodeUnik;
	private Long version;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SuratPerintahSalesJalan() {
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
	@JoinColumn(name = "sales", nullable = false)
	public SalesInventory getSales() {
		return sales;
	}

	public void setSales(SalesInventory sales) {
		this.sales = sales;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_berangkat_rencana", nullable = false)
	public Date getTanggalBerangkatRencana() {
		return tanggalBerangkatRencana == null ? ais.ui.util.WaktuUtil.getDate() : tanggalBerangkatRencana;
	}

	public void setTanggalBerangkatRencana(Date tanggalBerangkatRencana) {
		this.tanggalBerangkatRencana = tanggalBerangkatRencana;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_mulai_aktual")
	public Date getTanggalMulaiAktual() {
		return tanggalMulaiAktual;
	}

	public void setTanggalMulaiAktual(Date tanggalMulaiAktual) {
		this.tanggalMulaiAktual = tanggalMulaiAktual;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_kembali_aktual")
	public Date getTanggalKembaliAktual() {
		return tanggalKembaliAktual;
	}

	public void setTanggalKembaliAktual(Date tanggalKembaliAktual) {
		this.tanggalKembaliAktual = tanggalKembaliAktual;
	}

	@Column(name = "rute", columnDefinition = "text")
	public String getRute() {
		return rute;
	}

	public void setRute(String rute) {
		this.rute = rute;
	}

	@Column(name = "kendaraan", length = 100)
	public String getKendaraan() {
		return kendaraan;
	}

	public void setKendaraan(String kendaraan) {
		this.kendaraan = kendaraan;
	}

	/** Kas awal operasional (OPENING_ADVANCE ledger kas sesi saat mulai jalan). */
	@Column(name = "uang_muka_operasional", precision = 19, scale = 2)
	public BigDecimal getUangMukaOperasional() {
		return uangMukaOperasional == null ? BigDecimal.ZERO : uangMukaOperasional;
	}

	public void setUangMukaOperasional(BigDecimal uangMukaOperasional) {
		this.uangMukaOperasional = uangMukaOperasional;
	}

	@Column(name = "catatan", columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_DRAFT : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "alasan_batal", columnDefinition = "text")
	public String getAlasanBatal() {
		return alasanBatal;
	}

	public void setAlasanBatal(String alasanBatal) {
		this.alasanBatal = alasanBatal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh")
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh")
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
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
