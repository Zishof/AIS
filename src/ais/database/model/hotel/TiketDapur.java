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
 * Tiket dapur (kitchen ticket) satu nota POS outlet -- LANGKAH 5 MitraInap. Satu
 * {@link ais.database.model.koperasi.PembelianAnggotaKoperasi} maksimal SATU tiket
 * (kolom pembelian unik = idempotensi pembuatan via retry outbox). Transisi status
 * DIVALIDASI SERVER ({@code HotelApiHelper.kitchenTicketUpdate}, jangan percaya klien):
 * QUEUED -&gt; PREPARING -&gt; READY -&gt; SERVED, QUEUED/PREPARING -&gt; CANCELLED --
 * referensi urutan: KITCHEN_TRANSITIONS pos-hospitality.service.ts versi Node (yang di
 * sana justru TIDAK memvalidasi transisi; kesenjangan itu yang ditutup di sini).
 * Timestamp per-fase diisi SEKALI (pola COALESCE Node): tidak ditimpa saat status maju.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_tiket_dapur")
public class TiketDapur extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439829L;

	/** Tiket baru dibuat, menunggu dikerjakan dapur. Status awal default. */
	public static final String STATUS_QUEUED = "QUEUED";
	/** Dapur sedang menyiapkan pesanan. */
	public static final String STATUS_PREPARING = "PREPARING";
	/** Pesanan siap disajikan/diambil. */
	public static final String STATUS_READY = "READY";
	/** Pesanan sudah disajikan ke tamu -- status akhir alur normal. */
	public static final String STATUS_SERVED = "SERVED";
	/** Tiket dibatalkan -- hanya boleh dari QUEUED/PREPARING (lihat javadoc kelas). */
	public static final String STATUS_CANCELLED = "CANCELLED";

	private Long id;
	private PropertiHotel properti;
	private ais.database.model.koperasi.PembelianAnggotaKoperasi pembelian;
	private String status;
	private String catatan;
	private Date mulaiPada;
	private Date siapPada;
	private Date disajikanPada;
	private Date dibatalkanPada;
	private String oleh;
	private String olehId;

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pencatatan jejak audit ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali tiket
	 * ini di-UPDATE (mis. transisi status). Dipanggil otomatis oleh provider JPA.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Timestamp shadow untuk audit trail; diinisialisasi ke waktu sekarang saat entity
	 * dibuat di memori -- KEHARUSAN TEKNIS pola audit timestamp di seluruh model, bukan bug.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Representasi ringkas untuk log/debug: {@code id-status}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + getStatus();
	}

	/** @return id unik tiket dapur (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id tiket (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Nullable: nota outlet tanpa konteks properti (mis. toko campuran) tetap boleh bertiket.
	 * @return properti hotel terkait tiket ini, atau {@code null} bila nota tanpa konteks properti;
	 *         getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = true)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	/** @param properti properti hotel terkait tiket ini (opsional). */
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	/**
	 * Nota pembelian POS koperasi yang memicu tiket ini -- relasi unik (satu nota maksimal satu
	 * tiket) berfungsi sebagai kunci idempotensi pembuatan tiket lewat retry outbox.
	 * @return nota pembelian anggota koperasi terkait; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pembelian", nullable = false, unique = true)
	public ais.database.model.koperasi.PembelianAnggotaKoperasi getPembelian() { pembelian = check(pembelian); return pembelian; }
	/** @param pembelian nota pembelian anggota koperasi yang memicu tiket ini; wajib diisi (kolom NOT NULL, unik). */
	public void setPembelian(ais.database.model.koperasi.PembelianAnggotaKoperasi pembelian) { this.pembelian = pembelian; }

	/**
	 * Status siklus hidup tiket: salah satu konstanta {@code STATUS_*}. Transisi divalidasi
	 * server ({@code HotelApiHelper.kitchenTicketUpdate}), lihat javadoc kelas untuk urutan
	 * transisi yang diizinkan.
	 * @return status tiket; {@code null} tersimpan diperlakukan sebagai {@link #STATUS_QUEUED}.
	 */
	@Column(name = "status", nullable = false, length = 24)
	public String getStatus() { return status == null ? STATUS_QUEUED : status; }
	/** @param status status tiket baru; gunakan konstanta {@code STATUS_*}. */
	public void setStatus(String status) { this.status = status; }

	/** @return catatan bebas untuk tiket ini (mis. permintaan khusus dapur), atau {@code null}. */
	@Column(name = "catatan", nullable = true)
	public String getCatatan() { return catatan; }
	/** @param catatan catatan bebas untuk tiket ini. */
	public void setCatatan(String catatan) { this.catatan = catatan; }

	/**
	 * Timestamp fase PREPARING dimulai. Diisi SEKALI (pola COALESCE): tidak ditimpa saat
	 * status maju lagi -- lihat javadoc kelas.
	 * @return waktu tiket mulai dikerjakan dapur, atau {@code null} bila belum masuk fase ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "mulai_pada", nullable = true)
	public Date getMulaiPada() { return mulaiPada; }
	/** @param mulaiPada waktu tiket mulai dikerjakan dapur. */
	public void setMulaiPada(Date mulaiPada) { this.mulaiPada = mulaiPada; }

	/**
	 * Timestamp fase READY dimulai. Diisi SEKALI (pola COALESCE): tidak ditimpa saat status
	 * maju lagi -- lihat javadoc kelas.
	 * @return waktu tiket siap disajikan, atau {@code null} bila belum masuk fase ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "siap_pada", nullable = true)
	public Date getSiapPada() { return siapPada; }
	/** @param siapPada waktu tiket siap disajikan. */
	public void setSiapPada(Date siapPada) { this.siapPada = siapPada; }

	/**
	 * Timestamp fase SERVED dimulai. Diisi SEKALI (pola COALESCE): tidak ditimpa saat status
	 * maju lagi -- lihat javadoc kelas.
	 * @return waktu tiket disajikan ke tamu, atau {@code null} bila belum masuk fase ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "disajikan_pada", nullable = true)
	public Date getDisajikanPada() { return disajikanPada; }
	/** @param disajikanPada waktu tiket disajikan ke tamu. */
	public void setDisajikanPada(Date disajikanPada) { this.disajikanPada = disajikanPada; }

	/**
	 * Timestamp tiket dibatalkan. Hanya diisi bila transisi ke {@link #STATUS_CANCELLED}
	 * terjadi (dari QUEUED/PREPARING).
	 * @return waktu tiket dibatalkan, atau {@code null} bila tidak pernah dibatalkan.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "dibatalkan_pada", nullable = true)
	public Date getDibatalkanPada() { return dibatalkanPada; }
	/** @param dibatalkanPada waktu tiket dibatalkan. */
	public void setDibatalkanPada(Date dibatalkanPada) { this.dibatalkanPada = dibatalkanPada; }

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
