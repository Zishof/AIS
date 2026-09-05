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

/** Tipe kamar per {@link PropertiHotel} (mis. Standard/Deluxe/Suite) + harga dasar per malam. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_tipe_kamar")
public class TipeKamar extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439822L;

	private Long id;
	private PropertiHotel properti;
	private String kode;
	private String nama;
	private String keterangan;
	private Double hargaDasar;
	private Integer kapasitas;
	private Boolean aktif;
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

	/** Representasi ringkas untuk log/debug: {@code id-nama}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (nama == null ? "" : nama);
	}

	/** @return id unik tipe kamar (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id tipe kamar (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Properti hotel pemilik tipe kamar ini -- root scope tenant vertikal hotel.
	 * @return properti hotel; getter mengembalikan proxy lazy yang sudah diresolusi lewat
	 *         {@link #check(Object)}, boleh {@code null} jika data belum dimuat/tidak valid.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	/** @param properti properti hotel pemilik tipe kamar; wajib diisi (kolom NOT NULL). */
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	/** @return kode singkat tipe kamar (mis. STD/DLX/STE); boleh {@code null}. */
	@Column(name = "kode", nullable = true, length = 100)
	public String getKode() { return kode; }
	/** @param kode kode singkat tipe kamar. */
	public void setKode(String kode) { this.kode = kode; }

	/** @return nama tipe kamar, di-trim; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() { return nama == null ? null : nama.trim(); }
	/** @param nama nama tipe kamar (kolom NOT NULL, jangan simpan {@code null}). */
	public void setNama(String nama) { this.nama = nama; }

	/** @return catatan/keterangan bebas tentang tipe kamar; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	/** @param keterangan catatan bebas tentang tipe kamar. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Harga dasar per malam; harga musiman/promo menyusul fase berikutnya.
	 * @return harga dasar per malam; boleh {@code null} bila belum diisi.
	 */
	@Column(name = "harga_dasar", nullable = true)
	public Double getHargaDasar() { return hargaDasar; }
	/** @param hargaDasar harga dasar per malam yang baru. */
	public void setHargaDasar(Double hargaDasar) { this.hargaDasar = hargaDasar; }

	/** @return kapasitas maksimum tamu per kamar untuk tipe ini; boleh {@code null}. */
	@Column(name = "kapasitas", nullable = true)
	public Integer getKapasitas() { return kapasitas; }
	/** @param kapasitas kapasitas maksimum tamu per kamar. */
	public void setKapasitas(Integer kapasitas) { this.kapasitas = kapasitas; }

	/**
	 * Flag aktif/nonaktif tipe kamar (soft-disable, bukan hapus baris).
	 * @return {@code true} jika tipe kamar aktif; default {@code true} bila kolom {@code null}.
	 */
	public Boolean getAktif() { return aktif == null ? true : aktif; }
	/** @param aktif status aktif/nonaktif tipe kamar. */
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

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
