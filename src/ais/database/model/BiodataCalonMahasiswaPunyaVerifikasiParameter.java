package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate yang memetakan tabel
 * {@code public.biodata_calon_mahasiswa_punya_verifikasi_parameter} pada modul
 * penerimaan mahasiswa baru. Merupakan tabel penghubung (junction) yang
 * mencatat hasil verifikasi satu {@link ParameterVerifikasiCalonMahasiswa}
 * (mis. item cek berkas/syarat pendaftaran) untuk satu {@link
 * BiodataCalonMahasiswa} (data calon mahasiswa) yang mendaftar lewat satu
 * {@link PaketPunyaParameterVerifikasiCalonMahasiswa} (paket parameter
 * verifikasi yang berlaku, mis. per jalur/gelombang seleksi) — flag
 * {@code verified} menandai apakah parameter tersebut sudah dinyatakan
 * terverifikasi, dan {@code keterangan} menyimpan catatan petugas verifikasi.
 *
 * <p>
 * {@code nama} otomatis diisi dari nama {@link ParameterVerifikasiCalonMahasiswa}
 * terkait bila belum diisi manual — lihat {@link #ambilNamaParameterDefault()},
 * yang juga dipanggil pada {@code @PrePersist}/{@code @PreUpdate} untuk
 * menormalkan field sebelum disimpan. Diaudit lewat Hibernate Envers
 * ({@code @Audited}).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_calon_mahasiswa_punya_verifikasi_parameter")
public class BiodataCalonMahasiswaPunyaVerifikasiParameter extends GeneralValueObject {

	/** Penanda versi serialisasi Java. Tidak berdampak fungsional. */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama, lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna pengubah terakhir, lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna pengubah terakhir, lihat {@link #getOlehId()}. */
	private String olehId;

	/** Stempel waktu perubahan terakhir, lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Calon mahasiswa yang berkasnya diverifikasi, lihat {@link #getBiodataCalonMahasiswa()}. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	/** Paket parameter verifikasi yang berlaku, lihat {@link #getPaketPunyaParameterVerifikasiCalonMahasiswa()}. */
	private PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa;

	/** Item/master parameter yang diverifikasi, lihat {@link #getParameterVerifikasiCalonMahasiswa()}. */
	private ParameterVerifikasiCalonMahasiswa parameterVerifikasiCalonMahasiswa;

	/** Nama parameter (disalin/di-default dari master), lihat {@link #getNama()}. */
	private String nama;

	/** Catatan petugas verifikasi, lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Flag hasil verifikasi, lihat {@link #getVerified()}. */
	private Boolean verified;

	/** Konstruktor tanpa argumen yang dibutuhkan Hibernate/JPA. */
	public BiodataCalonMahasiswaPunyaVerifikasiParameter() {
	}

	/**
	 * Kunci utama baris (kolom {@code id}, {@code IDENTITY} &mdash; dibangkitkan database).
	 *
	 * @return id baris; {@code null} selama object belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menetapkan kunci utama. Praktis hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Id pengguna pengubah terakhir.
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila belum pernah tercatat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir. Setter penjaga: nilai {@code null}/kosong diabaikan
	 * diam-diam sehingga jejak audit yang sudah terisi tidak dapat dihapus lewat setter ini.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong.
	 */
	public void setOlehId(String olehId) {
		if (isBlank(olehId)) {
			return;
		}
		this.olehId = olehId.trim();
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila belum pernah tercatat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir. Berperilaku sama dengan
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong.
	 */
	public void setOleh(String oleh) {
		if (isBlank(oleh)) {
			return;
		}
		this.oleh = oleh.trim();
	}

	/**
	 * Hook JPA {@code @PrePersist} yang menormalkan field ({@link #normalize()}) sebelum baris ini
	 * pertama kali di-{@code INSERT} &mdash; memastikan {@link #getVerified()} dan {@link #getNama()}
	 * sudah punya nilai yang konsisten walau belum diisi manual. Dipanggil Hibernate, tidak pernah
	 * dipanggil manual.
	 */
	@javax.persistence.PrePersist
	protected void onPersist() {
		normalize();
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang menyegarkan metadata audit (lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}) dan menormalkan field
	 * ({@link #normalize()}) sebelum baris ini di-{@code UPDATE}. Dipanggil Hibernate saja.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
		normalize();
	}

	/**
	 * Menormalkan state sebelum persist/update: {@code verified} {@code null} dikoersi ke
	 * {@code Boolean.FALSE}, {@code nama} kosong diisi lewat {@link #ambilNamaParameterDefault()}
	 * (atau di-{@code trim()} bila sudah terisi), dan {@code keterangan} di-{@code trim()} bila
	 * tidak {@code null}. Dipanggil dari {@link #onPersist()} dan {@link #onUpdate()}.
	 */
	private void normalize() {
		if (verified == null) {
			verified = Boolean.FALSE;
		}
		if (isBlank(nama)) {
			nama = ambilNamaParameterDefault();
		} else {
			nama = nama.trim();
		}
		if (keterangan != null) {
			keterangan = keterangan.trim();
		}
	}

	/**
	 * Mengambil nama default untuk kolom {@code nama} bila belum diisi manual, disalin dari nama
	 * {@link #getParameterVerifikasiCalonMahasiswa()} terkait. Kegagalan resolusi relasi (mis. baris
	 * master sudah terhapus) ditangkap diam-diam dan dicatat lewat
	 * {@code ais.common.ErrorAuditUtil.record(...)}, dengan fallback ke label generik.
	 *
	 * @return nama parameter dari master terkait; {@code "Parameter Verifikasi"} bila relasi kosong
	 *         atau gagal diresolusi.
	 */
	private String ambilNamaParameterDefault() {
		try {
			if (getParameterVerifikasiCalonMahasiswa() != null && !isBlank(getParameterVerifikasiCalonMahasiswa().getNama())) {
				return getParameterVerifikasiCalonMahasiswa().getNama().trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswaPunyaVerifikasiParameter.java:107");
		}
		return "Parameter Verifikasi";
	}

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Tidak menjaga terhadap {@code null}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu terakhir baris ini diubah.
	 *
	 * @return waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Calon mahasiswa yang berkas pendaftarannya sedang diverifikasi &mdash; relasi
	 * {@code @ManyToOne} lazy ke kolom {@code biodata_calon_mahasiswa} ({@code nullable = true})
	 * dengan cascade {@code PERSIST}/{@code MERGE}.
	 *
	 * <p><b>Efek samping (resolusi proxy lazy).</b> Getter menuliskan kembali hasil
	 * {@link GeneralValueObject#check(Object)} ke field-nya; ini resolusi proxy, bukan mutasi bisnis,
	 * namun dapat memicu akses cache/database pada pemanggilan pertama.</p>
	 *
	 * @return calon mahasiswa terkait, sudah ter-resolve dari proxy lazy; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/**
	 * Menetapkan calon mahasiswa yang berkasnya diverifikasi.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa terkait; boleh {@code null}.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Paket parameter verifikasi yang berlaku bagi baris ini (mis. per jalur/gelombang seleksi)
	 * &mdash; relasi {@code @ManyToOne} lazy ke kolom
	 * {@code paket_punya_parameter_verifikasi_calon_mahasiswa} ({@code nullable = true}) dengan
	 * cascade {@code PERSIST}/{@code MERGE}.
	 *
	 * <p><b>Efek samping (resolusi proxy lazy)</b> sama seperti
	 * {@link #getBiodataCalonMahasiswa()}.</p>
	 *
	 * @return paket parameter verifikasi terkait, sudah ter-resolve dari proxy lazy; boleh
	 *         {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket_punya_parameter_verifikasi_calon_mahasiswa", nullable = true)
	public PaketPunyaParameterVerifikasiCalonMahasiswa getPaketPunyaParameterVerifikasiCalonMahasiswa() {
		paketPunyaParameterVerifikasiCalonMahasiswa = check(paketPunyaParameterVerifikasiCalonMahasiswa);
		return paketPunyaParameterVerifikasiCalonMahasiswa;
	}

	/**
	 * Menetapkan paket parameter verifikasi yang berlaku bagi baris ini.
	 *
	 * @param paketPunyaParameterVerifikasiCalonMahasiswa paket parameter verifikasi; boleh
	 *                                                     {@code null}.
	 */
	public void setPaketPunyaParameterVerifikasiCalonMahasiswa(
			PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa) {
		this.paketPunyaParameterVerifikasiCalonMahasiswa = paketPunyaParameterVerifikasiCalonMahasiswa;
	}

	/**
	 * Catatan bebas petugas verifikasi (kolom bertipe {@code text}). Dikembalikan sudah
	 * di-{@code trim()} dan tidak pernah {@code null} (kosong bila belum diisi).
	 *
	 * @return keterangan; string kosong bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menetapkan catatan bebas petugas verifikasi.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}, akan di-{@code trim()} saat
	 *                   persist/update lewat {@link #normalize()}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama parameter (kolom bertipe {@code text}). Bila belum diisi manual, dihitung on-the-fly
	 * lewat {@link #ambilNamaParameterDefault()} (disalin dari nama
	 * {@link #getParameterVerifikasiCalonMahasiswa()} terkait) &mdash; nilai default ini juga
	 * dituliskan kembali ke field saat {@link #normalize()} berjalan pada {@code @PrePersist}/
	 * {@code @PreUpdate}.
	 *
	 * @return nama parameter; tidak pernah {@code null}/kosong.
	 */
	@Column(columnDefinition = "text")
	public String getNama() {
		return isBlank(nama) ? ambilNamaParameterDefault() : nama.trim();
	}

	/**
	 * Menetapkan nama parameter secara manual. Bila dibiarkan kosong, {@link #getNama()}/
	 * {@link #normalize()} akan mengisinya dari master {@link ParameterVerifikasiCalonMahasiswa}
	 * terkait.
	 *
	 * @param nama nama parameter; boleh {@code null}/kosong.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Item/master parameter verifikasi ({@link ParameterVerifikasiCalonMahasiswa}) yang diperiksa
	 * pada baris ini &mdash; relasi {@code @ManyToOne} lazy ke kolom
	 * {@code parameter_verifikasi_calon_mahasiswa} ({@code nullable = true}) dengan cascade
	 * {@code PERSIST}/{@code MERGE}.
	 *
	 * <p><b>Efek samping (resolusi proxy lazy)</b> sama seperti
	 * {@link #getBiodataCalonMahasiswa()}; hasilnya juga dipakai {@link #ambilNamaParameterDefault()}
	 * untuk mengisi {@link #getNama()} default.</p>
	 *
	 * @return item master parameter verifikasi terkait, sudah ter-resolve dari proxy lazy; boleh
	 *         {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_verifikasi_calon_mahasiswa", nullable = true)
	public ParameterVerifikasiCalonMahasiswa getParameterVerifikasiCalonMahasiswa() {
		parameterVerifikasiCalonMahasiswa = check(parameterVerifikasiCalonMahasiswa);
		return parameterVerifikasiCalonMahasiswa;
	}

	/**
	 * Menetapkan item/master parameter verifikasi yang diperiksa pada baris ini.
	 *
	 * @param parameterVerifikasiCalonMahasiswa item master parameter verifikasi; boleh {@code null}.
	 */
	public void setParameterVerifikasiCalonMahasiswa(
			ParameterVerifikasiCalonMahasiswa parameterVerifikasiCalonMahasiswa) {
		this.parameterVerifikasiCalonMahasiswa = parameterVerifikasiCalonMahasiswa;
	}

	/**
	 * Flag hasil verifikasi: apakah parameter ini sudah dinyatakan terverifikasi oleh petugas.
	 * Getter berpola <i>default-false</i>: {@code null} dibaca sebagai belum terverifikasi.
	 *
	 * @return {@code true} bila sudah diverifikasi; {@code false} bila belum atau belum diisi.
	 */
	public Boolean getVerified() {
		return verified == null ? Boolean.FALSE : verified;
	}

	/**
	 * Menetapkan flag hasil verifikasi.
	 *
	 * @param verified {@code true} bila sudah diverifikasi; boleh {@code null} (dibaca sebagai
	 *                 {@code false}).
	 */
	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

	/**
	 * Representasi teks baris ini &mdash; nama parameter (lihat {@link #getNama()}, yang dapat
	 * memicu resolusi default bila belum diisi manual).
	 *
	 * @return nama parameter.
	 */
	public String toString() {
		return getNama();
	}

	/**
	 * Uji apakah sebuah string {@code null} atau hanya berisi spasi.
	 *
	 * @param value string yang diuji.
	 * @return {@code true} bila {@code null} atau kosong setelah di-{@code trim()}.
	 */
	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
