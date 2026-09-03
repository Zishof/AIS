package ais.database.model.employ;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;



/**
 * Entitas Hibernate katalog jenis peraturan kepegawaian AIS — dipetakan ke tabel
 * {@code employ.jenis_peraturan}. Baris entitas ini adalah kategori/pengelompokan untuk
 * {@link Peraturan} (mis. "Tata Tertib Kerja", "Kode Etik") — dirujuk oleh
 * {@link Peraturan#getJenisPeraturan()}. Entitas ini sendiri tidak menyimpan isi peraturan, hanya
 * label kategorinya beserta {@link #getKey() kunci} turunan yang dihasilkan dari {@link #nama}.
 *
 * @see Peraturan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "jenis_peraturan")



public class JenisPeraturan extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris ini, di-generate database (IDENTITY). */
	private Long id;
	/** Nama pengguna audit terakhir yang mengubah baris ini. */
	private String oleh;
	/** Id pengguna audit terakhir yang mengubah baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/** @return {@link #olehId} — id pengguna audit terakhir yang mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Meng-set {@link #olehId}; dilewati (no-op) bila {@code olehId} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param olehId id pengguna yang melakukan perubahan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Meng-set {@link #oleh}; dilewati (no-op) bila {@code oleh} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param oleh nama pengguna yang melakukan perubahan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return {@link #oleh} — nama pengguna audit terakhir yang mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE; mendelegasikan pembaruan {@link #tanggal_dirubah} ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak dipanggil manual
	 * dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah nilai timestamp audit baru; dipanggil manual maupun otomatis oleh {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return {@link #tanggal_dirubah} — timestamp terakhir baris ini diubah; nilai awal saat konstruksi objek adalah waktu sekarang. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi teks baris ini: {@link #nama} apa adanya (bukan {@link #id}, berbeda dengan beberapa entitas lain di paket ini). */
	public String toString() {
		return nama;
	}

	/** Nama jenis peraturan (mis. "Tata Tertib Kerja"). */
	private String nama;
	/** Keterangan/deskripsi bebas untuk jenis peraturan ini, boleh {@code null}. */
	private String keterangan;
	/** Cache; nilai efektif selalu dihitung ulang di {@link #getKey()} dari {@link #nama} (slug huruf kecil dipisah underscore). */
	private String key;

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field. */
	public JenisPeraturan() {
	}

	/** @return {@link #id} — primary key baris ini. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru; normalnya di-generate database, jarang di-set manual dari kode aplikasi. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return {@link #nama} yang sudah di-trim; {@code null} bila {@link #nama} {@code null}. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama jenis peraturan baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return {@link #keterangan} — keterangan/deskripsi bebas jenis peraturan ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi bebas baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return kunci slug yang dihasilkan dari {@link #nama} — SELALU dihitung ulang &amp; menimpa
	 *         field {@link #key} sebagai efek samping setiap kali dipanggil BILA {@link #nama}
	 *         terisi (huruf kecil semua, spasi diganti {@code "_"}); bila {@link #nama} kosong/
	 *         {@code null}, nilai {@link #key} lama (jika ada) dikembalikan apa adanya tanpa
	 *         dihitung ulang. Getter ini BUKAN getter murni — lihat catatan serupa pada entitas
	 *         lain di paket ini (mis. {@link JenisCutiDanIzin#getJenisPengguna()}).
	 */
	public String getKey() {
		boolean ada = nama != null && !nama.trim().equals("");
		if (ada) {
			key = nama.trim().toLowerCase().replaceAll(" ", "_");
		}
//		System.out.println("key = " + key + ", nama = " + nama + ", ada = " + ada);
		return key;
	}

	/** @param key nilai kunci slug awal/cache lokal; TIDAK bertahan sebagai nilai akhir, karena {@link #getKey()} akan menghitung ulang &amp; menimpa field ini dari {@link #nama} pada pemanggilan berikutnya bila {@code nama} terisi. */
	public void setKey(String key) {
		this.key = key;
	}

}
