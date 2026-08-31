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
 * Biaya sesi sales lapangan (layar legacy 40-41, ERD &sect;3.8) -- kategori configurable
 * ({@link KategoriBiayaSales}), idempoten {@code kodeUnik} (retry offline P7 aman); biaya
 * TUNAI ikut menulis ledger {@link NotaSalesKas} EXPENSE_CASH. Koreksi = reversal, bukan hapus.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "nota_sales_biaya")
public class NotaSalesBiaya extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String METODE_TUNAI = "TUNAI";
	public static final String METODE_TRANSFER = "TRANSFER";

	private Long id;
	private NotaSalesSession sesi;
	private KategoriBiayaSales kategori;
	private Date tanggal;
	private String uraian;
	private BigDecimal nilai;
	private String metode;
	private String penerima;
	private String nomorBukti;
	private String kodeUnik;
	private Tbmuser dibuatOleh;
	// P10 reversal: biaya posted dibatalkan lewat baris pembalik bernilai negatif.
	private String statusDok;
	private String alasanReversal;
	private Long reversalDari;

	public static final String DOK_AKTIF = "AKTIF";
	public static final String DOK_DIBATALKAN = "DIBATALKAN";
	public static final String DOK_REVERSAL = "REVERSAL";

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public NotaSalesBiaya() {
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
	@JoinColumn(name = "sesi", nullable = false)
	public NotaSalesSession getSesi() {
		return sesi;
	}

	public void setSesi(NotaSalesSession sesi) {
		this.sesi = sesi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori", nullable = false)
	public KategoriBiayaSales getKategori() {
		return kategori;
	}

	public void setKategori(KategoriBiayaSales kategori) {
		this.kategori = kategori;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(name = "uraian", columnDefinition = "text")
	public String getUraian() {
		return uraian;
	}

	public void setUraian(String uraian) {
		this.uraian = uraian;
	}

	@Column(name = "nilai", precision = 19, scale = 2)
	public BigDecimal getNilai() {
		return nilai == null ? BigDecimal.ZERO : nilai;
	}

	public void setNilai(BigDecimal nilai) {
		this.nilai = nilai;
	}

	@Column(name = "metode", length = 20)
	public String getMetode() {
		return metode == null || metode.trim().isEmpty() ? METODE_TUNAI : metode;
	}

	public void setMetode(String metode) {
		this.metode = metode;
	}

	@Column(name = "penerima", length = 120)
	public String getPenerima() {
		return penerima;
	}

	public void setPenerima(String penerima) {
		this.penerima = penerima;
	}

	@Column(name = "nomor_bukti", length = 80)
	public String getNomorBukti() {
		return nomorBukti;
	}

	public void setNomorBukti(String nomorBukti) {
		this.nomorBukti = nomorBukti;
	}

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

	/** AKTIF (default) | DIBATALKAN (sudah direversal) | REVERSAL (baris pembalik). */
	@Column(name = "status_dok", length = 20)
	public String getStatusDok() {
		return statusDok == null || statusDok.trim().isEmpty() ? DOK_AKTIF : statusDok;
	}

	public void setStatusDok(String statusDok) {
		this.statusDok = statusDok;
	}

	@Column(name = "alasan_reversal", columnDefinition = "text")
	public String getAlasanReversal() {
		return alasanReversal;
	}

	public void setAlasanReversal(String alasanReversal) {
		this.alasanReversal = alasanReversal;
	}

	@Column(name = "reversal_dari")
	public Long getReversalDari() {
		return reversalDari;
	}

	public void setReversalDari(Long reversalDari) {
		this.reversalDari = reversalDari;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal biaya sesi sales (dok 61 butir E): terisi begitu mesin
	 * {@code PostingBiayaSalesUtil} menjurnalkan biaya ini.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
