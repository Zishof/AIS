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
 * Kontrak pemilik kamar (model kondotel: tiap kamar dimiliki investor, operator
 * memungut komisi persen dari pendapatan kamar) -- LANGKAH 5 MitraInap, padanan
 * {@code hospitality_owner_contract} versi Node ({@code ownerContract()}
 * hospitality-longstay.service.ts). Pendapatan pemilik dihitung server saat
 * generate {@link LaporanPemilik} -- bukan angka kiriman klien.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_kontrak_pemilik")
public class KontrakPemilik extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439830L;

	private Long id;
	private PropertiHotel properti;
	private Kamar kamar;
	private String namaPemilik;
	private String referensiPemilik;
	private Double persenKomisi;
	private Date berlakuDari;
	private Date berlakuSampai;
	private Boolean aktif;
	private String oleh;
	private String olehId;

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pencatatan jejak audit ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris
	 * kontrak ini di-UPDATE (mis. ubah komisi, nonaktifkan). Dipanggil otomatis oleh provider
	 * JPA, bukan kode aplikasi.
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

	/** Representasi ringkas untuk log/debug: {@code id-namaPemilik}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (namaPemilik == null ? "" : namaPemilik);
	}

	/** @return id unik kontrak pemilik (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id kontrak (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Properti/cabang hotel tempat kamar kontrak ini berada -- root scope tenant vertikal hotel.
	 * @return properti hotel; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	/** @param properti properti hotel tempat kamar berada; wajib diisi (kolom NOT NULL). */
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	/**
	 * Kamar spesifik yang dimiliki investor sesuai kontrak ini (model kondotel: satu kamar =
	 * satu unit investasi).
	 * @return kamar terkait kontrak; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar", nullable = false)
	public Kamar getKamar() { kamar = check(kamar); return kamar; }
	/** @param kamar kamar yang dimiliki investor sesuai kontrak ini; wajib diisi (kolom NOT NULL). */
	public void setKamar(Kamar kamar) { this.kamar = kamar; }

	/**
	 * Nama pemilik/investor kamar. Field bebas teks (bukan FK ke entity user/anggota) --
	 * pemilik properti hotel adalah pihak eksternal, tidak selalu punya akun sistem.
	 * @return nama pemilik kamar.
	 */
	@Column(name = "nama_pemilik", nullable = false, length = 255)
	public String getNamaPemilik() { return namaPemilik; }
	/** @param namaPemilik nama pemilik/investor kamar; wajib diisi (kolom NOT NULL). */
	public void setNamaPemilik(String namaPemilik) { this.namaPemilik = namaPemilik; }

	/**
	 * Referensi bebas (no. KTP/NPWP/kode investor) -- opsional.
	 * @return referensi identitas pemilik, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "referensi_pemilik", nullable = true, length = 100)
	public String getReferensiPemilik() { return referensiPemilik; }
	/** @param referensiPemilik referensi identitas pemilik (opsional). */
	public void setReferensiPemilik(String referensiPemilik) { this.referensiPemilik = referensiPemilik; }

	/**
	 * Komisi OPERATOR dalam persen (0..100) dari pendapatan kamar; sisa = hak pemilik.
	 * Dipakai server saat {@code HotelApiHelper.laporanPemilikGenerate}: {@code komisi = kotor *
	 * persenKomisi / 100}, {@code bersih = kotor - komisi - biaya} -- terverifikasi konsisten
	 * dengan deskripsi field ini (operator memungut komisi, sisanya hak pemilik).
	 * @return persentase komisi operator (0..100).
	 */
	@Column(name = "persen_komisi", nullable = false)
	public Double getPersenKomisi() { return persenKomisi; }
	/** @param persenKomisi persentase komisi operator; harus 0..100 (divalidasi di action simpan). */
	public void setPersenKomisi(Double persenKomisi) { this.persenKomisi = persenKomisi; }

	/** @return tanggal mulai berlaku kontrak (inklusif). */
	@Temporal(TemporalType.DATE)
	@Column(name = "berlaku_dari", nullable = false)
	public Date getBerlakuDari() { return berlakuDari; }
	/** @param berlakuDari tanggal mulai berlaku kontrak; wajib diisi (kolom NOT NULL). */
	public void setBerlakuDari(Date berlakuDari) { this.berlakuDari = berlakuDari; }

	/** @return tanggal akhir berlaku kontrak (inklusif); {@code null} berarti tanpa batas akhir. */
	@Temporal(TemporalType.DATE)
	@Column(name = "berlaku_sampai", nullable = true)
	public Date getBerlakuSampai() { return berlakuSampai; }
	/** @param berlakuSampai tanggal akhir berlaku kontrak, atau {@code null} untuk tanpa batas. */
	public void setBerlakuSampai(Date berlakuSampai) { this.berlakuSampai = berlakuSampai; }

	/**
	 * Flag aktif/nonaktif kontrak.
	 * @return status aktif; {@code null} tersimpan diperlakukan sebagai {@code TRUE} (default aman).
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	/** @param aktif status aktif kontrak baru. */
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
