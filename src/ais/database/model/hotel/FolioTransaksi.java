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

	/** Beban kamar (posting saat checkout, atau per-malam) -- tanda positif. */
	public static final String JENIS_ROOM_CHARGE = "ROOM_CHARGE";
	/** Beban dari nota POS outlet (mis. restoran/room service) -- tanda positif. */
	public static final String JENIS_POS_CHARGE = "POS_CHARGE";
	/** Pembayaran tamu terhadap saldo folio -- WAJIB disimpan sebagai nilai NEGATIF. */
	public static final String JENIS_PAYMENT = "PAYMENT";
	/** Koreksi manual (append-only: koreksi baris lama = baris ADJUSTMENT baru, bukan edit). */
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

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pencatatan jejak audit ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris
	 * ini di-UPDATE. Perhatikan: baris transaksi folio dirancang APPEND-ONLY -- UPDATE pada
	 * baris yang sudah ada seharusnya tidak terjadi di alur normal (koreksi = baris baru).
	 * Dipanggil otomatis oleh provider JPA, bukan kode aplikasi.
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

	/** Representasi ringkas untuk log/debug: {@code id-jenis}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (jenis == null ? "" : jenis);
	}

	/** @return id unik baris transaksi folio (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id baris transaksi (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Folio induk (header) tempat baris ini menagih -- relasi banyak-ke-satu, satu
	 * {@link Folio} punya banyak {@link FolioTransaksi} (header/detail).
	 * @return folio induk; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "folio", nullable = false)
	public Folio getFolio() { folio = check(folio); return folio; }
	/** @param folio folio induk baris transaksi ini; wajib diisi (kolom NOT NULL). */
	public void setFolio(Folio folio) { this.folio = folio; }

	/**
	 * Jenis baris: salah satu konstanta {@code JENIS_*}. Menentukan tanda yang diharapkan
	 * pada {@link #getJumlah()} (lihat javadoc kelas).
	 * @return jenis baris transaksi.
	 */
	@Column(name = "jenis", nullable = false, length = 24)
	public String getJenis() { return jenis; }
	/** @param jenis jenis baris; gunakan konstanta {@code JENIS_*}. */
	public void setJenis(String jenis) { this.jenis = jenis; }

	/** @return keterangan bebas untuk baris ini (opsional, mis. deskripsi item POS). */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	/** @param keterangan keterangan bebas untuk baris ini. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Signed: beban positif, PAYMENT negatif. Saldo folio = SUM kolom ini.
	 * @return nilai baris (signed sesuai {@link #getJenis()}).
	 */
	@Column(name = "jumlah", nullable = false)
	public Double getJumlah() { return jumlah; }
	/** @param jumlah nilai signed baris; PAYMENT harus dikirim negatif, beban lain positif. */
	public void setJumlah(Double jumlah) { this.jumlah = jumlah; }

	/**
	 * Referensi eksternal / idempotency key (mis. id sale POS) -- kunci anti-duplikasi charge.
	 * @return referensi eksternal baris ini, atau {@code null} bila tidak ada.
	 */
	@Column(name = "referensi", nullable = true, length = 160)
	public String getReferensi() { return referensi; }
	/** @param referensi idempotency key eksternal; disarankan diisi untuk baris hasil retry outbox. */
	public void setReferensi(String referensi) { this.referensi = referensi; }

	/** @return waktu baris transaksi ini dicatat/terjadi. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }
	/** @param waktu waktu baris transaksi. */
	public void setWaktu(Date waktu) { this.waktu = waktu; }

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
