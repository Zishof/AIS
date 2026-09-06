package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Menyimpan isi panduan (Bantuan) yang <b>telah dimodifikasi</b> dari dalam aplikasi.
 *
 * <p>Model penyimpanan panduan: bila sebuah panduan pernah disunting oleh administrator,
 * isinya disimpan pada tabel ini (kolom {@code isi}) dengan kunci = nama berkas panduan
 * ({@code kunci}). Saat memuat panduan, aplikasi mengutamakan isi dari tabel ini; bila
 * tidak ada barisnya, aplikasi jatuh ke berkas bawaan {@code WEB-INF/bantuan/<kunci>.html}.
 * Tombol <b>Reset</b> menghapus baris di tabel ini sehingga panduan kembali memakai berkas
 * HTML bawaan.</p>
 *
 * <p>Tabel dibuat otomatis oleh Hibernate ({@code hbm2ddl.auto=update}) saat aplikasi mulai.
 * Kolom {@code isi} bertipe {@code text} agar mampu menampung panduan yang panjang.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "bantuan", uniqueConstraints = @UniqueConstraint(columnNames = "kunci"))
public class Bantuan extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String kunci;
	private String isi;
	private Date waktuUbah;
	private String oleh;
	private String olehId;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public Bantuan() {
	}

	/**
	 * <i>Callback</i> JPA {@link javax.persistence.PreUpdate} yang memperbarui
	 * {@link #getWaktuUbah()} (lewat {@code AuditTimestampInterceptor}, sama seperti pola audit
	 * di entity lain paket ini) pada setiap UPDATE baris.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Mengembalikan primary key baris panduan.
	 *
	 * @return id baris; {@code null} selama objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel primary key baris panduan. Hanya relevan bagi Hibernate saat mengisi objek dari
	 * hasil query, karena kolom dipetakan {@code insertable = false}.
	 *
	 * @param id nilai primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** Kunci panduan = nama berkas zul/halaman tanpa ekstensi (mis. "agama", "pos"). */
	@Column(name = "kunci", nullable = false, length = 190)
	public String getKunci() {
		return kunci;
	}

	public void setKunci(String kunci) {
		this.kunci = kunci == null ? null : kunci.trim().toLowerCase();
	}

	/** Isi panduan dalam HTML. Tipe {@code text} untuk menampung panduan panjang. */
	@Column(name = "isi", columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	/**
	 * Menyetel isi panduan HTML.
	 *
	 * @param isi isi panduan baru; boleh {@code null}
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * Mengembalikan waktu terakhir panduan ini disunting administrator (presisi TIMESTAMP).
	 *
	 * @return waktu penyuntingan terakhir; boleh {@code null} bila belum pernah disunting manual
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_ubah")
	public Date getWaktuUbah() {
		return waktuUbah;
	}

	/**
	 * Menyetel waktu penyuntingan terakhir.
	 *
	 * @param waktuUbah waktu baru; boleh {@code null}
	 */
	public void setWaktuUbah(Date waktuUbah) {
		this.waktuUbah = waktuUbah;
	}

	/**
	 * Mengembalikan identitas administrator yang terakhir menyunting panduan ini.
	 *
	 * @return nama/identitas penyunting; boleh {@code null}
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel identitas administrator penyunting.
	 *
	 * @param oleh nama/identitas penyunting; boleh {@code null}
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id administrator yang terakhir menyunting panduan ini, pendamping
	 * {@link #getOleh()}.
	 *
	 * @return id penyunting; boleh {@code null}
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id administrator penyunting.
	 *
	 * @param olehId id penyunting; boleh {@code null}
	 */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/** Representasi teks baris, berupa id digabung {@link #kunci}. */
	public String toString() {
		return id + "-" + kunci;
	}
}
