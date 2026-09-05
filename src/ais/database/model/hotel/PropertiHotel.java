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
import ais.database.model.Tbmuser;

/**
 * <h3>Properti hotel/penginapan MitraInap -- root scope vertikal hotel (diskriminator baris).</h3>
 *
 * <p>Analog {@link ais.database.model.inventory.Toko} pada vertikal Kantin/POS: SEMUA tabel hotel
 * lain ber-FK ke properti ini; scoping aktor lewat perluasan pola
 * {@code EbisnisActorContextResolver} (fail-closed), BUKAN skema-per-tenant -- keputusan
 * 2.4/5.1 dokumen handover MitraInap 2026-08-18: mode LEGACY (skema tetap) adalah deployment
 * default dan jalur skema-per-tenant masih "schema-only-v0".</p>
 *
 * <p>Multi-properti (keputusan 5.3): satu {@link #getPemilik() pemilik} boleh menaungi BANYAK
 * baris properti -- relasi many-to-one biasa tanpa constraint unik; picker properti di klien
 * menyusul. Tabel di skema {@code public} berprefix {@code hotel_} (bukan skema baru) supaya
 * {@code hbm2ddl.auto=update} membuat tabelnya tanpa CREATE SCHEMA manual -- seluruh DDL
 * diserahkan ke Hibernate sesuai arahan pemilik produk 2026-08-18.</p>
 *
 * <p><b>CATATAN KEAMANAN (diverifikasi ulang, konsisten dengan {@code task_7b6038ac}/
 * {@code task_90bbdd51}):</b> entity ini TIDAK punya field {@code yayasan}/{@code sekolah}/
 * {@code satuanKerja}/dst. yang dikenali {@code GenericCrudAutoEntityAdapter.scopeBindings()}.
 * Kelas ini juga TIDAK dicegat oleh {@code GenericCrudAutoDefinitionFactory} (bukan token nama
 * terblokir, bukan paket terblokir) -- artinya jika {@code PropertiHotel} pernah dijelajahi lewat
 * Generic CRUD v2 (mis. lewat {@code model_crud_service.jsp} berbekal hak admin apa pun /
 * {@code Common.getApakahAdmin()}), {@code scopeBindings()} akan mengembalikan peta kosong untuk
 * kelas ini sehingga TIDAK ADA pembatas kepemilikan/institusi yang dipasang pada query maupun
 * validasi objek -- setiap satu {@link #getPemilik() pemilik} berpotensi melihat/mengubah properti
 * milik pemilik lain lewat jalur itu. Ini pola gap yang sama seperti paket SIRS dan entity lain
 * tanpa sumbu tenant, BUKAN kerentanan baru yang perlu di-spawn terpisah -- lihat catatan
 * {@code SIRS_BLOCKED_PACKAGE_PREFIX} di {@code GenericCrudAutoDefinitionFactory} untuk pola
 * mitigasinya (blokir per-paket). Jalur resmi hotel ({@code HotelApiHelper} via {@code PosApi})
 * TIDAK terpengaruh -- gate/scoping-nya terpisah dan tidak lewat adapter Generic CRUD ini.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_properti")
public class PropertiHotel extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439821L;

	private Long id;
	private String kode;
	private String nama;
	private String alamat;
	private String kota;
	private String telp;
	private String email;
	private String keterangan;
	private Integer jumlahLantai;
	private Boolean aktif;
	private Tbmuser pemilik;
	private String oleh;
	private String olehId;

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pencatatan jejak audit ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris
	 * properti ini di-UPDATE. Dipanggil otomatis oleh provider JPA, bukan kode aplikasi.
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

	/** Representasi ringkas untuk log/debug: {@code id-nama}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (nama == null ? "" : nama);
	}

	/** @return id unik properti hotel (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id properti (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/** @return kode singkat properti (opsional, tidak divalidasi unik di level entity). */
	@Column(name = "kode", nullable = true, length = 100)
	public String getKode() { return kode; }
	/** @param kode kode singkat properti. */
	public void setKode(String kode) { this.kode = kode; }

	/**
	 * Nama properti/hotel. Getter melakukan {@code trim()} defensif (whitespace pinggir
	 * dihilangkan tiap dibaca, walau nilai tersimpan tidak diubah).
	 * @return nama properti, sudah di-trim; {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() { return nama == null ? null : nama.trim(); }
	/** @param nama nama properti; wajib diisi (kolom NOT NULL). */
	public void setNama(String nama) { this.nama = nama; }

	/** @return alamat properti, atau {@code null} bila belum diisi. */
	@Column(name = "alamat", nullable = true)
	public String getAlamat() { return alamat; }
	/** @param alamat alamat properti. */
	public void setAlamat(String alamat) { this.alamat = alamat; }

	/** @return kota lokasi properti, atau {@code null} bila belum diisi. */
	@Column(name = "kota", nullable = true, length = 100)
	public String getKota() { return kota; }
	/** @param kota kota lokasi properti. */
	public void setKota(String kota) { this.kota = kota; }

	/** @return nomor telepon properti, atau {@code null} bila belum diisi. */
	@Column(name = "telp", nullable = true, length = 50)
	public String getTelp() { return telp; }
	/** @param telp nomor telepon properti. */
	public void setTelp(String telp) { this.telp = telp; }

	/** @return alamat email properti, atau {@code null} bila belum diisi. */
	@Column(name = "email", nullable = true, length = 255)
	public String getEmail() { return email; }
	/** @param email alamat email properti. */
	public void setEmail(String email) { this.email = email; }

	/** @return keterangan bebas properti, atau {@code null} bila belum diisi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	/** @param keterangan keterangan bebas properti. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/** @return jumlah lantai gedung properti, atau {@code null} bila belum diisi. */
	@Column(name = "jumlah_lantai", nullable = true)
	public Integer getJumlahLantai() { return jumlahLantai; }
	/** @param jumlahLantai jumlah lantai gedung properti. */
	public void setJumlahLantai(Integer jumlahLantai) { this.jumlahLantai = jumlahLantai; }

	/**
	 * Flag aktif/nonaktif properti.
	 * @return status aktif; {@code null} tersimpan diperlakukan sebagai {@code true} (default aman).
	 */
	public Boolean getAktif() { return aktif == null ? true : aktif; }
	/** @param aktif status aktif properti baru. */
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	/**
	 * Pemilik/pengurus utama -- nullable; SATU pemilik boleh punya BANYAK properti (5.3).
	 * Ini SATU-SATUNYA field yang secara alami mengaitkan properti ke pengguna/institusi;
	 * relasi many-to-one biasa tanpa constraint unik, dan TIDAK dikenali sebagai sumbu scope
	 * oleh {@code GenericCrudAutoEntityAdapter.scopeBindings()} (lihat catatan keamanan di
	 * javadoc kelas) karena nama propertinya bukan {@code yayasan}/{@code sekolah}/
	 * {@code satuanKerja}/dst.
	 * @return pemilik/pengurus utama properti; getter diresolusi lewat {@link #check(Object)},
	 *         boleh {@code null} bila belum ditetapkan.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pemilik", nullable = true)
	public Tbmuser getPemilik() { pemilik = check(pemilik); return pemilik; }
	/** @param pemilik pemilik/pengurus utama properti (opsional). */
	public void setPemilik(Tbmuser pemilik) { this.pemilik = pemilik; }

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
