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
 * Kamar fisik per {@link PropertiHotel}. {@link #getStatusHunian()} adalah PROYEKSI operasional
 * (VACANT/OCCUPIED/DIRTY/OUT_OF_ORDER) -- sumber kebenaran hunian sesungguhnya adalah
 * MenginapTamu/reservasi (fase berikutnya); kolom ini dipelihara servis, bukan diedit bebas klien.
 * <p>
 * Nama entity WAJIB "HotelKamar" (bukan default "Kamar") -- auto-import Hibernate menolak dua
 * entity bernama sama dan {@code ais.database.model.sirs.Kamar} sudah memakai "Kamar"; tanpa ini
 * SessionFactory gagal build di deployment yang memuat sirs (ecampus). HQL lama "from Kamar"
 * tetap menunjuk sirs; akses entity ini via Criteria/class ({@code HotelApiHelper} sudah begitu).
 */
@Entity(name = "HotelKamar")
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_kamar")
public class Kamar extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439823L;

	/** Kamar kosong, siap dijual/di-check-in. Nilai default proyeksi bila kolom {@code null}. */
	public static final String HUNIAN_VACANT = "VACANT";
	/** Kamar sedang dihuni tamu (di-set atomik oleh {@code HotelApiHelper.checkin}). */
	public static final String HUNIAN_OCCUPIED = "OCCUPIED";
	/** Kamar kosong tapi belum dibersihkan pasca checkout; belum siap dijual. */
	public static final String HUNIAN_DIRTY = "DIRTY";
	/** Kamar tidak bisa dijual sementara (rusak/maintenance). */
	public static final String HUNIAN_OUT_OF_ORDER = "OUT_OF_ORDER";

	private Long id;
	private PropertiHotel properti;
	private TipeKamar tipeKamar;
	private String nomor;
	private Integer lantai;
	private String statusHunian;
	private String keterangan;
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

	/** Representasi ringkas untuk log/debug: {@code id-nomor}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (nomor == null ? "" : nomor);
	}

	/** @return id unik kamar (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id kamar (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Properti hotel pemilik kamar fisik ini -- root scope tenant vertikal hotel.
	 * @return properti hotel; getter mengembalikan proxy lazy yang sudah diresolusi lewat
	 *         {@link #check(Object)}, boleh {@code null} jika data belum dimuat/tidak valid.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	/** @param properti properti hotel pemilik kamar; wajib diisi (kolom NOT NULL). */
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	/**
	 * Tipe/kelas kamar ini (menentukan harga dasar &amp; kapasitas default).
	 * @return tipe kamar; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_kamar", nullable = false)
	public TipeKamar getTipeKamar() { tipeKamar = check(tipeKamar); return tipeKamar; }
	/** @param tipeKamar tipe kamar; wajib diisi (kolom NOT NULL). */
	public void setTipeKamar(TipeKamar tipeKamar) { this.tipeKamar = tipeKamar; }

	/** @return nomor kamar, di-trim; {@code null} bila belum diisi. */
	@Column(name = "nomor", nullable = false, length = 50)
	public String getNomor() { return nomor == null ? null : nomor.trim(); }
	/** @param nomor nomor kamar (kolom NOT NULL, jangan simpan {@code null}). */
	public void setNomor(String nomor) { this.nomor = nomor; }

	/** @return nomor lantai fisik kamar; boleh {@code null}. */
	@Column(name = "lantai", nullable = true)
	public Integer getLantai() { return lantai; }
	/** @param lantai nomor lantai fisik kamar. */
	public void setLantai(Integer lantai) { this.lantai = lantai; }

	/**
	 * Status hunian operasional -- PROYEKSI yang dipelihara servis (lihat javadoc kelas),
	 * bukan kolom bebas-edit klien: {@code HotelApiHelper.checkin}/{@code checkout} yang
	 * memindahkannya antara {@link #HUNIAN_VACANT} dan {@link #HUNIAN_OCCUPIED}.
	 * @return status hunian; {@code null} tersimpan diperlakukan sebagai {@link #HUNIAN_VACANT}.
	 */
	@Column(name = "status_hunian", nullable = true, length = 24)
	public String getStatusHunian() { return statusHunian == null ? HUNIAN_VACANT : statusHunian; }
	/** @param statusHunian status hunian baru; gunakan konstanta {@code HUNIAN_*}. */
	public void setStatusHunian(String statusHunian) { this.statusHunian = statusHunian; }

	/** @return catatan/keterangan bebas tentang kamar; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	/** @param keterangan catatan bebas tentang kamar. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Flag aktif/nonaktif kamar (soft-disable, bukan hapus baris) -- kamar nonaktif tidak
	 * boleh di-check-in ({@code HotelApiHelper.checkin} menolaknya secara eksplisit).
	 * @return {@code true} jika kamar aktif; default {@code true} bila kolom {@code null}.
	 */
	public Boolean getAktif() { return aktif == null ? true : aktif; }
	/** @param aktif status aktif/nonaktif kamar. */
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
