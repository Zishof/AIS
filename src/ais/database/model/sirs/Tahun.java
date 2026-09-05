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
 * Entitas katalog master tahun pada schema {@code sirs} (tabel
 * {@code tahun}), dimaksudkan sebagai daftar tahun (mis. "2024", "2025",
 * "2026") untuk keperluan filter/pilihan periode.
 *
 * <p>
 * <b>ENTITAS YATIM (dormant) — TIDAK DIPAKAI DI MANA PUN dalam kode
 * Java.</b> Penelusuran seluruh basis kode ({@code ais.action.*},
 * {@code ais.common.*}, {@code ais.database.model.*}) hanya menemukan
 * SATU referensi ke kelas ini: pendaftaran mapping Hibernate di
 * {@code hibernate.cfg.xml} ({@code <mapping class="ais.database.model.
 * sirs.Tahun" />}). Tidak ada Action CRUD (tidak ada
 * {@code TahunAction} seperti pola penamaan katalog {@code sirs} lain,
 * mis. {@code SatkerAction}/{@code KomunitasAction}), tidak ada helper
 * pemilihan data (banbox), tidak ada import dari Action/Helper/laporan
 * mana pun yang merujuk kelas ini. Baris di tabel {@code sirs.tahun}
 * (bila ada) hanya bisa diisi lewat SQL manual atau Generic CRUD
 * generik, bukan lewat layar master khusus. Ini pola entitas yatim yang
 * berulang ditemukan di paket model AIS lain — dipetakan Hibernate
 * namun tidak pernah benar-benar dipakai aplikasi.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "tahun")
public class Tahun extends GeneralValueObject {

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
	 * Representasi ringkas baris tahun ini untuk keperluan tampilan/log.
	 *
	 * @return nilai tahun sebagai string (mis. {@code "2026"}), atau
	 *         {@code "null"} bila field {@link #tahun} belum diisi
	 *         (akibat konkatenasi {@code tahun + ""}).
	 */
	public String toString() {
		return tahun + "";
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

	private String tahun;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public Tahun() {
	}

	/**
	 * Primary key baris tahun, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris tahun ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris tahun.
	 *
	 * @param id ID baris tahun.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nilai tahun. PERHATIKAN: nama method {@code getNama()}
	 * TIDAK sesuai dengan nama field/kolom sebenarnya ({@link #tahun},
	 * kolom {@code tahun}) — penamaan {@code Nama}/{@code nama} di sini
	 * hanyalah konvensi warisan generator hbm2java, bukan indikasi ada
	 * kolom {@code nama} terpisah dari {@code tahun}.
	 *
	 * @return nilai tahun (mis. {@code "2026"}).
	 */
	@Column(name = "tahun", nullable = false)
	public String getNama() {
		return this.tahun;
	}

	/**
	 * Menetapkan nilai tahun. Lihat catatan penamaan pada
	 * {@link #getNama()}.
	 *
	 * @param tahun nilai tahun.
	 */
	public void setNama(String tahun) {
		this.tahun = tahun;
	}

}
