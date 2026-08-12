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
import ais.database.model.library.Penyedia;

/**
 * Header pembayaran hutang supplier (layar legacy 24-26; event KREDIT register hutang, pola
 * TRAN_HUT.DBF: TGLBAYAR/JUMLAH/KETBAYAR/NOMERBG/NAMABANK/TANGGALBG). Satu pembayaran boleh
 * dialokasikan ke BANYAK faktur ({@link AlokasiPembayaranHutangSupplier}); total alokasi =
 * nominal header dan tiap alokasi tidak melebihi outstanding faktur (divalidasi atomik di
 * helper). {@code kodeUnik} = kunci idempoten (retry jaringan tidak menggandakan pembayaran --
 * pola sama {@code SesiKasKasir.kode}). Event posted TIDAK dihapus -- koreksi = reversal
 * (pembayaran bernilai metode REVERSAL yang mengembalikan outstanding, fase lanjut).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pembayaran_hutang_supplier")
public class PembayaranHutangSupplier extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String METODE_TUNAI = "TUNAI";
	public static final String METODE_TRANSFER = "TRANSFER";
	public static final String METODE_GIRO = "GIRO";
	public static final String METODE_DISCOUNT = "DISCOUNT";
	public static final String METODE_RETUR = "RETUR";

	private Long id;
	private Penyedia supplier;
	private Date tanggal;
	private BigDecimal nominal;
	private String metode;
	private String noBg;
	private String namaBank;
	private Date tanggalBg;
	private String keterangan;
	private String kodeUnik;
	private Tbmuser dibuatOleh;
	// P10 reversal + siklus BG: dokumen posted tidak dihapus -- dibatalkan lewat
	// dokumen pembalik; giro punya status pencairan sendiri.
	private String statusDok;
	private String alasanReversal;
	private Long reversalDari;
	private String statusBg;
	private Date tanggalStatusBg;

	public static final String DOK_AKTIF = "AKTIF";
	public static final String DOK_DIBATALKAN = "DIBATALKAN";
	public static final String DOK_REVERSAL = "REVERSAL";
	public static final String BG_DITERIMA = "DITERIMA";
	public static final String BG_CAIR = "CAIR";
	public static final String BG_TOLAK = "TOLAK";

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PembayaranHutangSupplier() {
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
	@JoinColumn(name = "supplier", nullable = false)
	public Penyedia getSupplier() {
		return supplier;
	}

	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
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

	/** Kunci idempoten dari klien (UUID) -- duplicate retry mengembalikan pembayaran pertama. */
	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/** AKTIF (default) | DIBATALKAN (sudah direversal) | REVERSAL (dokumen pembaliknya). */
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

	/** id dokumen asal yang dibalik (diisi hanya pada baris REVERSAL). */
	@Column(name = "reversal_dari")
	public Long getReversalDari() {
		return reversalDari;
	}

	public void setReversalDari(Long reversalDari) {
		this.reversalDari = reversalDari;
	}

	/** Siklus giro: DITERIMA -> CAIR | TOLAK (null utk metode non-GIRO). */
	@Column(name = "status_bg", length = 20)
	public String getStatusBg() {
		return statusBg;
	}

	public void setStatusBg(String statusBg) {
		this.statusBg = statusBg;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_status_bg")
	public Date getTanggalStatusBg() {
		return tanggalStatusBg;
	}

	public void setTanggalStatusBg(Date tanggalStatusBg) {
		this.tanggalStatusBg = tanggalStatusBg;
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
