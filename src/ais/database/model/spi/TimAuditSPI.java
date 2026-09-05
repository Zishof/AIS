package ais.database.model.spi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.CascadeType;
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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * <h2>TimAuditSPI &mdash; Keanggotaan Tim Audit pada Satu Penugasan</h2>
 *
 * <p>
 * Kelas penghubung (relasi many-to-many terstruktur) antara satu {@link PenugasanAuditSPI} dan
 * para pengguna ({@link Tbmuser}) yang ditugaskan sebagai auditor pada penugasan tersebut. Satu
 * baris di sini berarti "pengguna X ditugaskan pada penugasan audit Y dengan peran Z". Kelas ini
 * SENGAJA menggantikan pendekatan kolom teks bebas "Nama Auditor" (yang sempat dipakai di draf
 * awal desain modul ini, meniru pola {@code auditorNama} pada modul SPMI) karena kolom teks bebas
 * tidak bisa direkap/dianalisis secara terstruktur (mis. "berapa kali staf X pernah bertugas
 * sebagai auditor tahun ini", kebutuhan umum untuk menjaga rotasi &amp; independensi auditor), dan
 * rawan salah ketik/tidak konsisten antar penugasan.
 * </p>
 *
 * <h3>Dua peran dalam satu tim: Ketua dan Anggota</h3>
 * <p>
 * {@link #getPeranTim()} membedakan {@link #KETUA_TIM} (memimpin &amp; bertanggung jawab penuh atas
 * penugasan, biasanya satu orang per penugasan) dari {@link #ANGGOTA_TIM} (anggota pendukung,
 * bisa lebih dari satu). Pembedaan ini penting bagi pelaporan SPI ke pimpinan (mis. daftar "siapa
 * saja yang pernah menjadi Ketua Tim" sebagai indikator kematangan/kompetensi staf) dan bagi
 * tanggung jawab dokumen (Ketua Tim yang lazimnya menandatangani laporan hasil audit).
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tim_audit_spi")
public class TimAuditSPI extends GeneralValueObject {

	/** Kode peran "Ketua Tim" &mdash; lihat javadoc kelas bagian "Dua peran dalam satu tim". */
	public static final String KETUA_TIM = "KETUA_TIM";
	/** Kode peran "Anggota Tim" &mdash; lihat javadoc kelas bagian "Dua peran dalam satu tim". */
	public static final String ANGGOTA_TIM = "ANGGOTA_TIM";

	/** Peta kode peran &rarr; label bahasa manusia, sumber tunggal untuk dropdown pilihan peran di layar Setup Penugasan. */
	public static final Map<String, String> PERAN_TIM_DATA = new LinkedHashMap<String, String>();
	static {
		PERAN_TIM_DATA.put(KETUA_TIM, "Ketua Tim");
		PERAN_TIM_DATA.put(ANGGOTA_TIM, "Anggota Tim");
	}

	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field SHADOW dari riwayat Envers
	 * ({@code @Audited} pada kelas ini) &mdash; KEHARUSAN TEKNIS untuk menampilkan "terakhir diubah
	 * oleh siapa" secara murah di layar daftar, bukan duplikasi yang keliru.
	 *
	 * @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan.
	 *
	 * @param olehId ID pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan.
	 *
	 * @param oleh nama pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis Hibernate sebelum UPDATE, mendelegasikan
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk menyegarkan
	 * {@link #getTanggal_dirubah()} secara otomatis.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi manual waktu terakhir baris ini diubah; dalam praktiknya disegarkan otomatis lewat
	 * {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil waktu terakhir baris ini diubah.
	 *
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini untuk log/debug, format
	 * {@code "<nama anggota> (<label peran>)"}, mis. "Budi Santoso (Ketua Tim)".
	 *
	 * @return string gabungan nama anggota dan label perannya; "-" bila anggota belum diisi.
	 */
	public String toString() {
		return (anggota == null ? "-" : anggota.getUserNama()) + " (" + getPeranTimLabel() + ")";
	}

	private PenugasanAuditSPI penugasanAuditSPI;
	private Tbmuser anggota;
	private String peranTim;
	private Boolean aktif;

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public TimAuditSPI() {
	}

	/**
	 * Konstruktor kenyamanan untuk langsung mengaitkan baris keanggotaan tim baru ke satu
	 * penugasan audit, dipakai lazimnya saat menambah anggota tim dari layar detail penugasan.
	 *
	 * @param penugasanAuditSPI penugasan audit yang akan diikuti anggota tim ini.
	 */
	public TimAuditSPI(PenugasanAuditSPI penugasanAuditSPI) {
		this.penugasanAuditSPI = penugasanAuditSPI;
	}

	/**
	 * ID primer baris ini, di-generate otomatis oleh database (strategi {@code IDENTITY}).
	 *
	 * @return ID unik baris ini, atau {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris ini secara manual, terutama saat membangun objek referensi ringan untuk
	 * relasi {@code JoinColumn} tanpa memuat seluruh baris dari database.
	 *
	 * @param id ID baris yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Penugasan audit tempat anggota ini bertugas &mdash; lihat javadoc kelas untuk penjelasan
	 * lengkap mengapa keanggotaan tim direlasikan terstruktur, bukan kolom teks bebas. Relasi
	 * wajib ({@code nullable = false}): setiap baris keanggotaan HARUS terkait satu penugasan.
	 *
	 * @return penugasan audit tempat anggota tim ini bertugas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penugasan_audit_spi", nullable = false)
	public PenugasanAuditSPI getPenugasanAuditSPI() {
		penugasanAuditSPI = check(penugasanAuditSPI);
		return penugasanAuditSPI;
	}

	/**
	 * Mengaitkan baris keanggotaan ini ke satu penugasan audit.
	 *
	 * @param penugasanAuditSPI penugasan audit baru yang diikuti.
	 */
	public void setPenugasanAuditSPI(PenugasanAuditSPI penugasanAuditSPI) {
		this.penugasanAuditSPI = penugasanAuditSPI;
	}

	/**
	 * Pengguna ({@link Tbmuser}) yang menjadi anggota tim pada baris ini &mdash; relasi
	 * terstruktur ke tabel pengguna resmi, BUKAN kolom nama teks bebas, sehingga rekap "siapa
	 * pernah bertugas sebagai auditor" bisa dihitung akurat lewat query, lihat javadoc kelas.
	 * Relasi wajib ({@code nullable = false}).
	 *
	 * @return pengguna yang menjadi anggota tim pada baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota", nullable = false)
	public Tbmuser getAnggota() {
		anggota = check(anggota);
		return anggota;
	}

	/**
	 * Mengisi pengguna yang menjadi anggota tim pada baris ini.
	 *
	 * @param anggota pengguna baru yang ditugaskan sebagai anggota tim.
	 */
	public void setAnggota(Tbmuser anggota) {
		this.anggota = anggota;
	}

	/**
	 * Kode peran anggota ini dalam tim ({@link #KETUA_TIM} atau {@link #ANGGOTA_TIM}) &mdash;
	 * lihat javadoc kelas bagian "Dua peran dalam satu tim". Default {@link #ANGGOTA_TIM} bila
	 * belum diisi.
	 *
	 * @return kode peran; {@link #ANGGOTA_TIM} bila nilai tersimpan {@code null}.
	 */
	@Column(name = "peran_tim", nullable = false, length = 20)
	public String getPeranTim() {
		return peranTim == null ? ANGGOTA_TIM : peranTim;
	}

	/**
	 * Mengisi kode peran anggota ini dalam tim.
	 *
	 * @param peranTim kode peran baru, idealnya salah satu dari {@link #KETUA_TIM}/{@link #ANGGOTA_TIM}.
	 */
	public void setPeranTim(String peranTim) {
		this.peranTim = peranTim;
	}

	/** Label bahasa manusia dari {@link #getPeranTim()}, dipakai langsung oleh tampilan. */
	@Transient
	public String getPeranTimLabel() {
		String label = PERAN_TIM_DATA.get(getPeranTim());
		return label == null ? getPeranTim() : label;
	}

	/**
	 * Status aktif/nonaktif baris keanggotaan ini; nilai {@code null} SENGAJA diperlakukan
	 * sebagai {@code true} (aktif) demi kompatibilitas data lama &mdash; konvensi baku entity
	 * "data master sederhana" di aplikasi ini. Dipakai mis. saat anggota tim dikeluarkan dari
	 * penugasan tanpa menghapus riwayat keanggotaannya.
	 *
	 * @return {@code true} bila keanggotaan ini aktif (termasuk saat nilai tersimpan {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif baris keanggotaan ini.
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
