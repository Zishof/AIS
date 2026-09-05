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
 * Guest stay -- check-in aktif (analog {@code Pendaftaran} di sirs). Dibuat dari reservasi ATAU
 * walk-in ({@link #getReservasi()} nullable). Satu stay membuka tepat satu {@link Folio};
 * checkout menutup keduanya atomik ({@code HotelApiHelper.checkout}).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_menginap")
public class MenginapTamu extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439826L;

	/** Tamu sedang menginap aktif di kamar ini (status awal default saat check-in). */
	public static final String STATUS_IN_HOUSE = "IN_HOUSE";
	/** Tamu sudah check-out; stay ditutup bersama {@link Folio}-nya (atomik). */
	public static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

	private Long id;
	private PropertiHotel properti;
	private ReservasiKamar reservasi;
	private Tamu tamu;
	private Kamar kamar;
	private Date checkinPada;
	private Date checkoutPada;
	private Double hargaPerMalam;
	private String status;
	private String catatan;
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

	/** Representasi ringkas untuk log/debug: {@code id-nama tamu}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (tamu == null ? "" : tamu.getNama());
	}

	/** @return id unik stay/menginap (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id stay (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Properti hotel pemilik stay ini -- root scope tenant vertikal hotel.
	 * @return properti hotel; getter mengembalikan proxy lazy yang sudah diresolusi lewat
	 *         {@link #check(Object)}, boleh {@code null} jika data belum dimuat/tidak valid.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	/** @param properti properti hotel pemilik stay; wajib diisi (kolom NOT NULL). */
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	/**
	 * Reservasi asal stay ini; {@code null} bila stay berasal dari walk-in (tanpa reservasi
	 * sebelumnya) -- lihat javadoc kelas. Reservasi TIDAK otomatis menjadi stay; keduanya
	 * dihubungkan hanya saat {@code HotelApiHelper.checkin} dipanggil secara eksplisit dengan
	 * {@code reservasi_id}, yang saat itu juga mempromosikan status reservasi ke
	 * {@link ReservasiKamar#STATUS_CHECKED_IN} dan menetapkan {@link ReservasiKamar#getKamar()}.
	 * @return reservasi asal, atau {@code null} untuk walk-in; getter diresolusi lewat
	 *         {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservasi", nullable = true)
	public ReservasiKamar getReservasi() { reservasi = check(reservasi); return reservasi; }
	/** @param reservasi reservasi asal stay ini; boleh {@code null} untuk walk-in. */
	public void setReservasi(ReservasiKamar reservasi) { this.reservasi = reservasi; }

	/**
	 * Tamu yang menginap.
	 * @return tamu; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tamu", nullable = false)
	public Tamu getTamu() { tamu = check(tamu); return tamu; }
	/** @param tamu tamu yang menginap; wajib diisi (kolom NOT NULL). */
	public void setTamu(Tamu tamu) { this.tamu = tamu; }

	/**
	 * Kamar fisik yang dihuni. Ditetapkan atomik bersama pembuatan stay ini dan perubahan
	 * {@link Kamar#getStatusHunian()} menjadi {@link Kamar#HUNIAN_OCCUPIED} di
	 * {@code HotelApiHelper.checkin} -- gerbang riil pencegah dua stay aktif pada kamar yang
	 * sama adalah syarat kamar berstatus {@link Kamar#HUNIAN_VACANT} sebelum check-in, BUKAN
	 * pengecekan tumpang tindih tanggal (stay tidak punya rentang tanggal terjadwal seperti
	 * reservasi, hanya waktu check-in/checkout aktual).
	 * @return kamar yang dihuni; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar", nullable = false)
	public Kamar getKamar() { kamar = check(kamar); return kamar; }
	/** @param kamar kamar yang dihuni; wajib diisi (kolom NOT NULL). */
	public void setKamar(Kamar kamar) { this.kamar = kamar; }

	/** @return waktu check-in aktual (timestamp, bukan hanya tanggal). */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "checkin_pada", nullable = false)
	public Date getCheckinPada() { return checkinPada; }
	/** @param checkinPada waktu check-in aktual (kolom NOT NULL). */
	public void setCheckinPada(Date checkinPada) { this.checkinPada = checkinPada; }

	/** @return waktu check-out aktual; {@code null} selama tamu masih {@link #STATUS_IN_HOUSE}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "checkout_pada", nullable = true)
	public Date getCheckoutPada() { return checkoutPada; }
	/** @param checkoutPada waktu check-out aktual. */
	public void setCheckoutPada(Date checkoutPada) { this.checkoutPada = checkoutPada; }

	/**
	 * Snapshot harga per malam saat check-in (dari reservasi atau tipe kamar saat itu).
	 * @return harga per malam yang berlaku untuk stay ini; boleh {@code null}.
	 */
	@Column(name = "harga_per_malam", nullable = true)
	public Double getHargaPerMalam() { return hargaPerMalam; }
	/** @param hargaPerMalam harga per malam snapshot untuk stay ini. */
	public void setHargaPerMalam(Double hargaPerMalam) { this.hargaPerMalam = hargaPerMalam; }

	/**
	 * Status siklus hidup stay; transisi ke {@link #STATUS_CHECKED_OUT} ditegakkan server
	 * saat checkout (menutup {@link Folio} terkait secara atomik, mensyaratkan saldo folio
	 * &lt;= 0).
	 * @return status stay; {@code null} tersimpan diperlakukan sebagai {@link #STATUS_IN_HOUSE}.
	 */
	@Column(name = "status", nullable = false, length = 24)
	public String getStatus() { return status == null ? STATUS_IN_HOUSE : status; }
	/** @param status status stay baru; gunakan konstanta {@code STATUS_*}. */
	public void setStatus(String status) { this.status = status; }

	/** @return catatan bebas tentang stay ini; boleh {@code null}. */
	@Column(name = "catatan", nullable = true)
	public String getCatatan() { return catatan; }
	/** @param catatan catatan bebas tentang stay ini. */
	public void setCatatan(String catatan) { this.catatan = catatan; }

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
