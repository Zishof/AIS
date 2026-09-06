package ais.database.model;

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

/**
 * Entity master data <b>widget dashboard</b> pada tabel {@code public.dashboard}. Satu baris
 * mendaftarkan satu widget/komponen dashboard yang bisa dipilih operator untuk ditampilkan di
 * beranda, dikaitkan ke peran lewat {@link RoleHasDashboard}. Kolom {@link #getClazz()}
 * menyimpan <b>nama kelas Java lengkap</b> (mis. controller/window ZK) dari widget tersebut —
 * {@code RoleHasDashboard.getDashboard()} membacanya lewat {@code getOriginDashboard().getClazz()}
 * untuk menentukan komponen mana yang dirender bagi suatu peran.
 *
 * <p>Kelas ini murni tabel referensi terkonfigurasi admin: {@link #getNama()} (label tampil),
 * {@link #getClazz()} (nama kelas target), dan {@link #getKeterangan()}. Tidak ada sakelar
 * {@code aktif}/{@code nomorUrut}. Karena {@link #getClazz()} adalah nama kelas bebas teks
 * tanpa validasi di level entity, kebenarannya (apakah kelas tersebut benar ada) sepenuhnya
 * bergantung pada input yang dimasukkan admin lewat layar konfigurasi.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see RoleHasDashboard
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "dashboard")

public class Dashboard extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.dashboard} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan
	 * diam-diam (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * masukan kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi
	 * ulang {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk
	 * dipanggil langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return nama widget dashboard ini apa adanya — representasi teks ringkas baris ini. */
	public String toString() {
		return nama;
	}

	/** Nama/label tampil widget dashboard ini; wajib diisi. */
	private String nama;
	/** Nama kelas Java lengkap komponen yang merender widget ini; lihat {@link #getClazz()}. */
	private String clazz;
	/** Catatan/keterangan bebas tentang widget ini; boleh {@code null}. */
	private String keterangan;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public Dashboard() {
	}

	/**
	 * @return kunci utama baris ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama/label widget, di-trim; {@code null} bila belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama/label widget; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas widget ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk widget ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return nama kelas Java lengkap yang merender widget ini, apa adanya (tanpa validasi
	 *         bahwa kelas tersebut benar ada). Dibaca {@code RoleHasDashboard.getDashboard()}
	 *         untuk menentukan komponen dashboard bagi suatu peran.
	 */
	public String getClazz() {
		return clazz;
	}

	/** @param clazz nama kelas Java lengkap komponen yang merender widget ini. */
	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

}
