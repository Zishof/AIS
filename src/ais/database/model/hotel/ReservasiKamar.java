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
 * Reservasi/booking kamar. Transisi status DIVALIDASI SERVER ({@code HotelApiHelper}), jangan
 * percaya klien: BOOKED -&gt; CONFIRMED -&gt; CHECKED_IN, atau BOOKED/CONFIRMED -&gt; CANCELLED/NO_SHOW.
 * {@link #getHargaPerMalam()} adalah SNAPSHOT dari {@link TipeKamar#getHargaDasar()} saat booking
 * dibuat -- perubahan harga master tidak mengubah reservasi berjalan (pola snapshot yang sama
 * dengan cost_snapshot POS).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_reservasi")
public class ReservasiKamar extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439825L;

	/** Reservasi baru dibuat, belum dikonfirmasi (status awal default). */
	public static final String STATUS_BOOKED = "BOOKED";
	/** Reservasi dikonfirmasi (mis. setelah pembayaran/DP tercatat) via aksi {@code approve}. */
	public static final String STATUS_CONFIRMED = "CONFIRMED";
	/** Tamu sudah check-in atas reservasi ini; kamar sudah ditetapkan di {@link #getKamar()}. */
	public static final String STATUS_CHECKED_IN = "CHECKED_IN";
	/** Reservasi dibatalkan (oleh tamu/staf) sebelum check-in. */
	public static final String STATUS_CANCELLED = "CANCELLED";
	/** Tamu tidak datang sampai batas waktu; reservasi ditutup tanpa check-in. */
	public static final String STATUS_NO_SHOW = "NO_SHOW";

	private Long id;
	private PropertiHotel properti;
	private Tamu tamu;
	private TipeKamar tipeKamar;
	private Kamar kamar;
	private String kode;
	private Date tanggalCheckin;
	private Date tanggalCheckout;
	private Integer jumlahTamu;
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

	/** Representasi ringkas untuk log/debug: {@code id-kode}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (kode == null ? "" : kode);
	}

	/** @return id unik reservasi (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id reservasi (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Properti hotel pemilik reservasi ini -- root scope tenant vertikal hotel.
	 * @return properti hotel; getter mengembalikan proxy lazy yang sudah diresolusi lewat
	 *         {@link #check(Object)}, boleh {@code null} jika data belum dimuat/tidak valid.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	/** @param properti properti hotel pemilik reservasi; wajib diisi (kolom NOT NULL). */
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	/**
	 * Tamu yang memesan.
	 * @return tamu pemesan; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tamu", nullable = false)
	public Tamu getTamu() { tamu = check(tamu); return tamu; }
	/** @param tamu tamu pemesan; wajib diisi (kolom NOT NULL). */
	public void setTamu(Tamu tamu) { this.tamu = tamu; }

	/**
	 * Tipe kamar yang dipesan (bukan kamar fisik spesifik -- lihat {@link #getKamar()}).
	 * @return tipe kamar; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_kamar", nullable = false)
	public TipeKamar getTipeKamar() { tipeKamar = check(tipeKamar); return tipeKamar; }
	/** @param tipeKamar tipe kamar yang dipesan; wajib diisi (kolom NOT NULL). */
	public void setTipeKamar(TipeKamar tipeKamar) { this.tipeKamar = tipeKamar; }

	/**
	 * Kamar spesifik opsional saat booking; boleh baru ditetapkan saat check-in.
	 * CATATAN ARSITEKTUR: tidak ada gerbang di level reservasi yang mencegah dua reservasi
	 * overlap tanggal untuk {@link #getTipeKamar()} yang sama melebihi jumlah kamar fisik
	 * tersedia (tidak ada hitung kapasitas/inventori saat {@code reservasiBuat}) -- kamar
	 * fisik baru benar-benar dikunci di {@code HotelApiHelper.checkin} lewat gerbang
	 * {@link Kamar#getStatusHunian()} harus {@code VACANT}; sampai saat itu overbooking di
	 * level tipe kamar mengandalkan resolusi manual staf, bukan penolakan otomatis sistem.
	 * @return kamar fisik yang ditetapkan; getter diresolusi lewat {@link #check(Object)},
	 *         {@code null} sebelum kamar ditetapkan.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar", nullable = true)
	public Kamar getKamar() { kamar = check(kamar); return kamar; }
	/** @param kamar kamar fisik yang ditetapkan ke reservasi ini. */
	public void setKamar(Kamar kamar) { this.kamar = kamar; }

	/** @return kode unik reservasi (mis. {@code RSV-<epoch>}); dipakai juga sebagai referensi posting folio. */
	@Column(name = "kode", nullable = false, length = 64)
	public String getKode() { return kode; }
	/** @param kode kode unik reservasi (kolom NOT NULL). */
	public void setKode(String kode) { this.kode = kode; }

	/** @return tanggal rencana check-in (tanpa komponen waktu, {@link TemporalType#DATE}). */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_checkin", nullable = false)
	public Date getTanggalCheckin() { return tanggalCheckin; }
	/** @param tanggalCheckin tanggal rencana check-in (kolom NOT NULL). */
	public void setTanggalCheckin(Date tanggalCheckin) { this.tanggalCheckin = tanggalCheckin; }

	/**
	 * @return tanggal rencana check-out (tanpa komponen waktu, {@link TemporalType#DATE});
	 *         server memvalidasi ini harus setelah {@link #getTanggalCheckin()} saat dibuat.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_checkout", nullable = false)
	public Date getTanggalCheckout() { return tanggalCheckout; }
	/** @param tanggalCheckout tanggal rencana check-out (kolom NOT NULL). */
	public void setTanggalCheckout(Date tanggalCheckout) { this.tanggalCheckout = tanggalCheckout; }

	/** @return jumlah tamu yang akan menginap; boleh {@code null}. */
	@Column(name = "jumlah_tamu", nullable = true)
	public Integer getJumlahTamu() { return jumlahTamu; }
	/** @param jumlahTamu jumlah tamu yang akan menginap. */
	public void setJumlahTamu(Integer jumlahTamu) { this.jumlahTamu = jumlahTamu; }

	/**
	 * SNAPSHOT dari {@link TipeKamar#getHargaDasar()} saat booking dibuat -- perubahan harga
	 * master tidak mengubah reservasi berjalan (pola snapshot yang sama dengan cost_snapshot POS).
	 * @return harga per malam yang berlaku untuk reservasi ini; boleh {@code null}.
	 */
	@Column(name = "harga_per_malam", nullable = true)
	public Double getHargaPerMalam() { return hargaPerMalam; }
	/** @param hargaPerMalam harga per malam snapshot untuk reservasi ini. */
	public void setHargaPerMalam(Double hargaPerMalam) { this.hargaPerMalam = hargaPerMalam; }

	/**
	 * Status siklus hidup reservasi; transisi DIVALIDASI SERVER (lihat javadoc kelas), jangan
	 * percaya nilai dari klien.
	 * @return status reservasi; {@code null} tersimpan diperlakukan sebagai {@link #STATUS_BOOKED}.
	 */
	@Column(name = "status", nullable = false, length = 24)
	public String getStatus() { return status == null ? STATUS_BOOKED : status; }
	/** @param status status reservasi baru; gunakan konstanta {@code STATUS_*}. */
	public void setStatus(String status) { this.status = status; }

	/** @return catatan bebas tentang reservasi (mis. permintaan khusus, alasan pembatalan); boleh {@code null}. */
	@Column(name = "catatan", nullable = true)
	public String getCatatan() { return catatan; }
	/** @param catatan catatan bebas tentang reservasi. */
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
