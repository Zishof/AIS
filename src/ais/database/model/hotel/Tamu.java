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
 * Tamu hotel. SENGAJA ber-scope per {@link PropertiHotel} (isolasi fail-closed antar pemilik --
 * MitraInap adalah platform banyak pemilik independen); profil tamu lintas-properti milik satu
 * pemilik adalah keputusan fase berikutnya, BUKAN default diam-diam.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_tamu")
public class Tamu extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439824L;

	private Long id;
	private PropertiHotel properti;
	private String nama;
	private String jenisIdentitas;
	private String noIdentitas;
	private String telp;
	private String email;
	private String alamat;
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

	/** Representasi ringkas untuk log/debug: {@code id-nama}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (nama == null ? "" : nama);
	}

	/** @return id unik tamu (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id tamu (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Properti hotel pemilik data tamu ini -- isolasi fail-closed antar pemilik (lihat javadoc
	 * kelas); satu baris Tamu hanya kelihatan/terpakai dalam lingkup satu properti.
	 * @return properti hotel; getter mengembalikan proxy lazy yang sudah diresolusi lewat
	 *         {@link #check(Object)}, boleh {@code null} jika data belum dimuat/tidak valid.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	/** @param properti properti hotel pemilik tamu; wajib diisi (kolom NOT NULL). */
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	/** @return nama tamu, di-trim; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() { return nama == null ? null : nama.trim(); }
	/** @param nama nama tamu (kolom NOT NULL, jangan simpan {@code null}). */
	public void setNama(String nama) { this.nama = nama; }

	/** @return jenis identitas tamu (mis. KTP/Paspor/SIM), bebas format; boleh {@code null}. */
	@Column(name = "jenis_identitas", nullable = true, length = 24)
	public String getJenisIdentitas() { return jenisIdentitas; }
	/** @param jenisIdentitas jenis identitas tamu. */
	public void setJenisIdentitas(String jenisIdentitas) { this.jenisIdentitas = jenisIdentitas; }

	/** @return nomor identitas tamu sesuai {@link #getJenisIdentitas()}; boleh {@code null}. */
	@Column(name = "no_identitas", nullable = true, length = 100)
	public String getNoIdentitas() { return noIdentitas; }
	/** @param noIdentitas nomor identitas tamu. */
	public void setNoIdentitas(String noIdentitas) { this.noIdentitas = noIdentitas; }

	/** @return nomor telepon tamu; boleh {@code null}. */
	@Column(name = "telp", nullable = true, length = 50)
	public String getTelp() { return telp; }
	/** @param telp nomor telepon tamu. */
	public void setTelp(String telp) { this.telp = telp; }

	/** @return alamat email tamu; boleh {@code null}. */
	@Column(name = "email", nullable = true, length = 255)
	public String getEmail() { return email; }
	/** @param email alamat email tamu. */
	public void setEmail(String email) { this.email = email; }

	/** @return alamat tempat tinggal tamu; boleh {@code null}. */
	@Column(name = "alamat", nullable = true)
	public String getAlamat() { return alamat; }
	/** @param alamat alamat tempat tinggal tamu. */
	public void setAlamat(String alamat) { this.alamat = alamat; }

	/** @return catatan/keterangan bebas tentang tamu; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	/** @param keterangan catatan bebas tentang tamu. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Flag aktif/nonaktif profil tamu (soft-disable, bukan hapus baris). {@code null} tersimpan
	 * diperlakukan sebagai aktif (default aman untuk data lama sebelum kolom ini ada).
	 * @return {@code true} jika tamu aktif; default {@code true} bila kolom {@code null}.
	 */
	public Boolean getAktif() { return aktif == null ? true : aktif; }
	/** @param aktif status aktif/nonaktif tamu. */
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
