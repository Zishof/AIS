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

/**
 * Sesi Nota Sales -- SATU realisasi untuk satu SPJ (layar legacy 40-42, ERD &sect;3.5).
 * Status: {@code NOT_STARTED -> ACTIVE -> RETURNED -> RECONCILING -> CLOSED}
 * (+SUSPENDED exception). CLOSED wajib approval Pemilik/Admin. Total-total di sini adalah
 * SNAPSHOT hasil rekonsiliasi saat tutup (sumber kebenaran tetap ledger
 * {@link NotaSalesKas}/biaya/penerimaan -- tidak pernah hanya agregat).
 *
 * <p>Rumus (ERD &sect;4): {@code HASIL_BERSIH = TOTAL_PIUTANG_DIBAYAR - TOTAL_BIAYA -
 * TOTAL_PEMBAYARAN_AKTUAL_PEMBELIAN}; {@code KAS_SEHARUSNYA = UANG_MUKA +
 * PENERIMAAN_TUNAI + PENJUALAN_TUNAI + REFUND - BIAYA_TUNAI - PEMBAYARAN_PEMBELIAN_TUNAI -
 * SETORAN}; {@code SELISIH_KAS = KAS_AKTUAL - KAS_SEHARUSNYA}. Dua rumus BERBEDA, dua-duanya
 * ditampilkan.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "nota_sales_session")
public class NotaSalesSession extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_NOT_STARTED = "NOT_STARTED";
	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_RETURNED = "RETURNED";
	public static final String STATUS_RECONCILING = "RECONCILING";
	public static final String STATUS_CLOSED = "CLOSED";
	public static final String STATUS_SUSPENDED = "SUSPENDED";

	private Long id;
	private String nomor;
	private SuratPerintahSalesJalan spj;
	private String status;
	private Date waktuMulai;
	private Date waktuKembali;
	private Date waktuTutup;
	private BigDecimal saldoKasAwal;
	private BigDecimal totalPenerimaanTunai;
	private BigDecimal totalPenerimaanNonTunai;
	private BigDecimal totalBiaya;
	private BigDecimal totalPembayaranPembelian;
	private BigDecimal totalSetoran;
	private BigDecimal kasFisikAktual;
	private BigDecimal selisihKas;
	private String catatanPenutupan;
	private Tbmuser disetujuiOleh;
	private Long version;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public NotaSalesSession() {
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
	@JoinColumn(name = "spj", nullable = false, unique = true)
	public SuratPerintahSalesJalan getSpj() {
		return spj;
	}

	public void setSpj(SuratPerintahSalesJalan spj) {
		this.spj = spj;
	}

	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_NOT_STARTED : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_mulai")
	public Date getWaktuMulai() {
		return waktuMulai;
	}

	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_kembali")
	public Date getWaktuKembali() {
		return waktuKembali;
	}

	public void setWaktuKembali(Date waktuKembali) {
		this.waktuKembali = waktuKembali;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_tutup")
	public Date getWaktuTutup() {
		return waktuTutup;
	}

	public void setWaktuTutup(Date waktuTutup) {
		this.waktuTutup = waktuTutup;
	}

	@Column(name = "saldo_kas_awal", precision = 19, scale = 2)
	public BigDecimal getSaldoKasAwal() {
		return saldoKasAwal == null ? BigDecimal.ZERO : saldoKasAwal;
	}

	public void setSaldoKasAwal(BigDecimal saldoKasAwal) {
		this.saldoKasAwal = saldoKasAwal;
	}

	@Column(name = "total_penerimaan_tunai", precision = 19, scale = 2)
	public BigDecimal getTotalPenerimaanTunai() {
		return totalPenerimaanTunai == null ? BigDecimal.ZERO : totalPenerimaanTunai;
	}

	public void setTotalPenerimaanTunai(BigDecimal totalPenerimaanTunai) {
		this.totalPenerimaanTunai = totalPenerimaanTunai;
	}

	@Column(name = "total_penerimaan_non_tunai", precision = 19, scale = 2)
	public BigDecimal getTotalPenerimaanNonTunai() {
		return totalPenerimaanNonTunai == null ? BigDecimal.ZERO : totalPenerimaanNonTunai;
	}

	public void setTotalPenerimaanNonTunai(BigDecimal totalPenerimaanNonTunai) {
		this.totalPenerimaanNonTunai = totalPenerimaanNonTunai;
	}

	@Column(name = "total_biaya", precision = 19, scale = 2)
	public BigDecimal getTotalBiaya() {
		return totalBiaya == null ? BigDecimal.ZERO : totalBiaya;
	}

	public void setTotalBiaya(BigDecimal totalBiaya) {
		this.totalBiaya = totalBiaya;
	}

	@Column(name = "total_pembayaran_pembelian", precision = 19, scale = 2)
	public BigDecimal getTotalPembayaranPembelian() {
		return totalPembayaranPembelian == null ? BigDecimal.ZERO : totalPembayaranPembelian;
	}

	public void setTotalPembayaranPembelian(BigDecimal totalPembayaranPembelian) {
		this.totalPembayaranPembelian = totalPembayaranPembelian;
	}

	@Column(name = "total_setoran", precision = 19, scale = 2)
	public BigDecimal getTotalSetoran() {
		return totalSetoran == null ? BigDecimal.ZERO : totalSetoran;
	}

	public void setTotalSetoran(BigDecimal totalSetoran) {
		this.totalSetoran = totalSetoran;
	}

	@Column(name = "kas_fisik_aktual", precision = 19, scale = 2)
	public BigDecimal getKasFisikAktual() {
		return kasFisikAktual;
	}

	public void setKasFisikAktual(BigDecimal kasFisikAktual) {
		this.kasFisikAktual = kasFisikAktual;
	}

	@Column(name = "selisih_kas", precision = 19, scale = 2)
	public BigDecimal getSelisihKas() {
		return selisihKas;
	}

	public void setSelisihKas(BigDecimal selisihKas) {
		this.selisihKas = selisihKas;
	}

	@Column(name = "catatan_penutupan", columnDefinition = "text")
	public String getCatatanPenutupan() {
		return catatanPenutupan;
	}

	public void setCatatanPenutupan(String catatanPenutupan) {
		this.catatanPenutupan = catatanPenutupan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh")
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
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
