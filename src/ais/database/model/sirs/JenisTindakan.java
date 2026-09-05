package ais.database.model.sirs;

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
 * Entitas katalog master jenis tindakan medis pada schema {@code sirs}
 * (tabel {@code jenis_tindakan}) — mis. "Kunjungan Dokter", "Jasa Racik",
 * kategori tindakan lain. Dipakai sebagai relasi {@code jenisTindakan}
 * dari {@link ais.database.model.sirs.Tindakan} untuk mengelompokkan
 * katalog tindakan/layanan.
 *
 * <p>
 * Dua nilai spesifik dipakai sebagai penanda semantik lewat konstanta
 * {@link #JASA_RACIK} dan {@link #KUNJUNGAN_DOKTER}, keduanya diresolusi
 * lewat lookup-atau-buat BERBASIS NAMA (bukan id hardcode) di
 * {@code InitSirs}/{@code CommonSirs} saat startup, lalu dicache di
 * {@code ConstantValues.KUNJUNGAN_DOKTER} dan dipakai untuk query
 * {@code Restrictions.eq("jenisTindakan", ...)} di beberapa Action
 * (mis. {@code DiagnosaPenyakitRawatInapAction}). Baris "Kunjungan
 * Dokter" ini juga menjadi anchor bagi {@code ConstantValues.KUNJUNGAN_RUTIN}
 * (entitas {@code Tindakan} terpisah yang menunjuk balik ke jenis ini).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "jenis_tindakan")
public class JenisTindakan extends GeneralValueObject {

	/** Nilai {@link #getNama()} penanda jenis tindakan "jasa racik" obat. */
	public static final String JASA_RACIK = "JASA RACIK";
	/** Nilai {@link #getNama()} penanda jenis tindakan "kunjungan dokter". */
	public static final String KUNJUNGAN_DOKTER = "Kunjungan Dokter";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
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
	 * Representasi ringkas jenis tindakan ini untuk keperluan
	 * tampilan/log.
	 *
	 * @return nama jenis tindakan.
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
	public JenisTindakan() {
	}

	/**
	 * Konstruktor pintasan untuk membuat referensi jenis tindakan hanya
	 * dari ID-nya (mis. untuk dipakai sebagai proxy relasi tanpa memuat
	 * seluruh baris).
	 *
	 * @param id ID jenis tindakan yang sudah ada.
	 */
	public JenisTindakan(Long id) {
		this.id = id;
	}

	/**
	 * Primary key baris jenis tindakan, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik jenis tindakan ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID jenis tindakan.
	 *
	 * @param id ID jenis tindakan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama jenis tindakan.
	 *
	 * <p>
	 * {@code null} otomatis dibaca DAN ditulis-balik ke field
	 * {@link #nama} sebagai string kosong {@code ""} lewat lazy-init —
	 * berbeda dari kebanyakan katalog {@code sirs} lain yang membiarkan
	 * nama tetap {@code null} bila belum diisi. Efeknya, kolom
	 * {@code nama} tidak akan pernah persis {@code null} lagi setelah
	 * getter ini dipanggil sekali pada baris yang bersangkutan.
	 * </p>
	 *
	 * @return nama jenis tindakan; string kosong (bukan {@code null})
	 *         bila belum pernah diisi.
	 */
	@Column(name = "nama", nullable = true, length = 250)
	public String getNama() {
		if (nama == null) {
			nama = "";
		}
		return this.nama;
	}

	/**
	 * Menetapkan nama jenis tindakan.
	 *
	 * @param nama nama jenis tindakan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan bebas jenis tindakan ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas jenis tindakan ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
