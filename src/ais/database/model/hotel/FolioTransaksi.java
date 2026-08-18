package ais.database.model.hotel;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

/**
 * Baris transaksi folio -- APPEND-ONLY. Konvensi tanda pada {@link #getJumlah()}:
 * beban (ROOM_CHARGE/POS_CHARGE/ADJUSTMENT positif) menambah tagihan; PAYMENT disimpan NEGATIF.
 * Saldo folio = SUM(jumlah); checkout hanya boleh saat saldo &lt;= 0 (ditegakkan server).
 * Room-charge dari POS outlet (fase berikutnya) WAJIB memakai idempotency key lewat
 * {@link #getReferensi()} agar retry tidak menggandakan beban.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_folio_transaksi")
public class FolioTransaksi extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439828L;

	public static final String JENIS_ROOM_CHARGE = "ROOM_CHARGE";
	public static final String JENIS_POS_CHARGE = "POS_CHARGE";
	public static final String JENIS_PAYMENT = "PAYMENT";
	public static final String JENIS_ADJUSTMENT = "ADJUSTMENT";

	private Long id;
	private Folio folio;
	private String jenis;
	private String keterangan;
	private Double jumlah;
	private String referensi;
	private Date waktu;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (jenis == null ? "" : jenis);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "folio", nullable = false)
	public Folio getFolio() { return folio; }
	public void setFolio(Folio folio) { this.folio = folio; }

	@Column(name = "jenis", nullable = false, length = 24)
	public String getJenis() { return jenis; }
	public void setJenis(String jenis) { this.jenis = jenis; }

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/** Signed: beban positif, PAYMENT negatif. Saldo folio = SUM kolom ini. */
	@Column(name = "jumlah", nullable = false)
	public Double getJumlah() { return jumlah; }
	public void setJumlah(Double jumlah) { this.jumlah = jumlah; }

	/** Referensi eksternal / idempotency key (mis. id sale POS) -- kunci anti-duplikasi charge. */
	@Column(name = "referensi", nullable = true, length = 160)
	public String getReferensi() { return referensi; }
	public void setReferensi(String referensi) { this.referensi = referensi; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
