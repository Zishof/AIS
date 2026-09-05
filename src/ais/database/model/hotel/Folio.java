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
 * Folio (tagihan berjalan) satu {@link MenginapTamu}. Total TIDAK disimpan sebagai kolom --
 * dihitung dari SUM({@link FolioTransaksi}) supaya tidak pernah drift; baris transaksi
 * append-only (koreksi = baris ADJUSTMENT baru, bukan edit/hapus baris lama).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_folio")
public class Folio extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439827L;

	/** Folio masih berjalan -- transaksi baru boleh ditambahkan, checkout belum dilakukan. */
	public static final String STATUS_OPEN = "OPEN";
	/** Folio sudah ditutup saat checkout (mensyaratkan saldo &lt;= 0); tidak menerima transaksi baru. */
	public static final String STATUS_CLOSED = "CLOSED";

	private Long id;
	private PropertiHotel properti;
	private MenginapTamu menginap;
	private String status;
	private Date dibukaPada;
	private Date ditutupPada;
	private String oleh;
	private String olehId;

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pencatatan jejak audit (siapa/kapan
	 * berubah) ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * setiap kali baris ini di-UPDATE. Dipanggil otomatis oleh provider JPA, bukan kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Timestamp shadow untuk audit trail; diinisialisasi ke waktu sekarang saat entity
	 * dibuat di memori (bukan hanya saat persist) -- KEHARUSAN TEKNIS pola audit timestamp
	 * di seluruh model, bukan bug.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Representasi ringkas untuk log/debug: {@code id-status}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + getStatus();
	}

	/** @return id unik folio (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id folio (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Properti/cabang hotel pemilik folio ini -- root scope tenant vertikal hotel.
	 * @return properti hotel; getter mengembalikan proxy lazy yang sudah diresolusi lewat
	 *         {@link #check(Object)}, boleh {@code null} jika data belum dimuat/tidak valid.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	/** @param properti properti hotel pemilik folio; wajib diisi (kolom NOT NULL). */
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	/**
	 * Masa inap tamu yang ditagih oleh folio ini -- relasi satu {@link MenginapTamu} ke
	 * (biasanya) satu folio aktif, dibuat atomik bersama stay + status kamar saat checkin.
	 * @return masa inap tamu terkait; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menginap", nullable = false)
	public MenginapTamu getMenginap() { menginap = check(menginap); return menginap; }
	/** @param menginap masa inap tamu yang ditagih; wajib diisi (kolom NOT NULL). */
	public void setMenginap(MenginapTamu menginap) { this.menginap = menginap; }

	/**
	 * Status siklus hidup folio: {@link #STATUS_OPEN} (default) atau {@link #STATUS_CLOSED}.
	 * Transisi ke CLOSED ditegakkan server saat checkout, mensyaratkan saldo (SUM
	 * {@link FolioTransaksi#getJumlah()}) &lt;= 0.
	 * @return status folio; {@code null} tersimpan diperlakukan sebagai {@link #STATUS_OPEN}.
	 */
	@Column(name = "status", nullable = false, length = 24)
	public String getStatus() { return status == null ? STATUS_OPEN : status; }
	/** @param status status folio baru; gunakan konstanta {@code STATUS_*}. */
	public void setStatus(String status) { this.status = status; }

	/** @return waktu folio dibuka (biasanya saat checkin). */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "dibuka_pada", nullable = false)
	public Date getDibukaPada() { return dibukaPada; }
	/** @param dibukaPada waktu folio dibuka. */
	public void setDibukaPada(Date dibukaPada) { this.dibukaPada = dibukaPada; }

	/** @return waktu folio ditutup saat checkout; {@code null} selama folio masih OPEN. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "ditutup_pada", nullable = true)
	public Date getDitutupPada() { return ditutupPada; }
	/** @param ditutupPada waktu folio ditutup (checkout). */
	public void setDitutupPada(Date ditutupPada) { this.ditutupPada = ditutupPada; }

	/** @return nama aktor yang terakhir membuat/mengubah baris (kolom audit, bukan FK). */
	public String getOleh() { return oleh; }
	/** @param oleh nama aktor; input kosong/blank diabaikan supaya nilai lama tidak tertimpa. */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	/** @return id aktor yang terakhir membuat/mengubah baris (kolom audit, bukan FK). */
	public String getOlehId() { return olehId; }
	/** @param olehId id aktor; input kosong/blank diabaikan supaya nilai lama tidak tertimpa. */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	/** @return timestamp shadow terakhir baris ini diubah (diisi otomatis lewat {@link #onUpdate()}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** @param tanggal_dirubah timestamp perubahan; umumnya tidak perlu diset manual. */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
