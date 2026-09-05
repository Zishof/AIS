package ais.database.model.sirs;

// Generated Apr 12, 2010 1:48:52 AM by Hibernate Tools 3.2.4.CR1

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
 * Entitas lookup kelas item pada schema {@code sirs} (tabel
 * {@code kelas_item}). Dimensi klasifikasi OPSIONAL untuk
 * {@link ItemMedis}, dihubungkan lewat relasi
 * {@link ItemMedis#getKelasItem()} — merepresentasikan klasifikasi
 * katalog item (mis. tingkat/mutu/kategori mutu item) yang murni bersifat
 * administratif untuk katalog.
 *
 * <p>
 * PENTING: "kelas" di sini TIDAK ADA HUBUNGANNYA dengan {@code KelasPerawatan}
 * (kelas rawat inap pasien seperti VIP/Kelas 1/2/3) yang dipakai di
 * {@link HargaJualItem#getKelasPerawatan()} untuk menentukan harga jual per
 * kelas rawat — dua entitas "kelas" yang sama sekali berbeda konteks dan
 * tabelnya, meski namanya mirip. Sudah diverifikasi dari kode: kelas ini
 * tidak punya relasi apapun ke {@code KelasPerawatan}.
 * </p>
 *
 * <p>
 * Sama seperti {@link KelompokItem} dan {@link GenerikItem}, kelas ini
 * adalah tabel lookup DATAR (hanya id/nama/keterangan) tanpa relasi ke
 * dimensi klasifikasi item medis lain.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "kelas_item")
public class KelasItem extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -3088213612931036389L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas kelas item ini untuk tampilan/log.
	 *
	 * @return nama kelas item.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String nama;
	private String keterangan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public KelasItem() {
	}

	/**
	 * Primary key baris kelas item, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris kelas item ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris kelas item.
	 *
	 * @param id ID baris kelas item.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nama kelas item.
	 *
	 * @param nama nama kelas item.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil nama kelas item.
	 *
	 * @return nama kelas item.
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan keterangan bebas baris kelas item ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil keterangan bebas baris kelas item ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

}
