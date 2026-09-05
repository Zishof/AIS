package ais.database.model.kpi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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



import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.satuan_kpi} — master satuan ukur nilai KPI,
 * misalnya "Persen", "Jumlah", atau "Skor 1-5".
 *
 * <p>Sama seperti {@link ais.database.model.kpi.KategoriKpi}, kelas ini adalah master/referensi
 * sederhana (kode, nama, keterangan, flag aktif) dengan tambahan satu relasi opsional ke
 * {@link ais.database.model.ParameterTambahan} untuk menampung atribut dinamis per satuan yang
 * tidak punya kolom tetap di skema tabel ini. Field-field audit shadow
 * ({@link #getOleh() oleh}/{@link #getOlehId() olehId}/{@link #getTanggal_dirubah() tanggal_dirubah})
 * mengikuti pola berulang yang sama di seluruh entity KPI: redundan secara sengaja terhadap
 * riwayat Envers ({@link org.hibernate.envers.Audited @Audited}), bukan bug.</p>
 *
 * @see ais.database.model.kpi.KategoriKpi
 * @see ais.database.model.ParameterTambahan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "satuan_kpi")



public class SatuanKpi extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id pengguna (username/kode) yang terakhir menyimpan baris ini, sebagaimana
	 *         tercatat pada field audit shadow {@link #oleh}/{@link #olehId}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang menyimpan baris ini. Nilai {@code null} atau string kosong/spasi
	 * diabaikan (guard fail-safe) sehingga nilai audit yang sudah tersimpan tidak pernah
	 * tertimpa oleh input kosong.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang menyimpan baris ini. Nilai {@code null} atau string kosong/spasi
	 * diabaikan (guard fail-safe), sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir menyimpan baris ini (field audit shadow, lihat
	 *         dokumentasi kelas).
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate sebelum setiap
	 * {@code UPDATE} untuk memperbarui {@link #tanggal_dirubah} via
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir baris ini; biasanya diisi otomatis oleh
	 *                        {@link #onUpdate()}, bukan dipanggil manual
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini; default saat konstruksi adalah waktu sekarang. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas {@code "<id>-<nama>"} untuk keperluan log/debug. */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;

	private String nama;
	private String keterangan;
	private ParameterTambahan parameterTambahan;
	private Boolean aktif;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public SatuanKpi() {
	}

	/** @return id baris (primary key, auto increment identity). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; biasanya tidak perlu diset manual karena kolom bersifat identity. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return kode satuan KPI, di-trim; string kosong ({@code ""}) bila belum diisi. */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/** @param kode kode satuan KPI (belum di-trim/divalidasi saat disimpan). */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama satuan KPI, di-trim; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama satuan KPI (kolom wajib/{@code NOT NULL} di database). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan satuan KPI; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi tambahan satuan KPI. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif satuan ini. Bila belum pernah diset ({@code null}) dianggap
	 *         <strong>aktif</strong> — pola "default aktif" yang konsisten dipakai di seluruh
	 *         entity master KPI pada paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif satuan KPI ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@link ParameterTambahan} berisi atribut dinamis tambahan untuk satuan ini, boleh
	 *         {@code null} bila tidak dipakai. Sebelum dikembalikan, referensi diresolusi via
	 *         {@link GeneralValueObject#check(Object)} untuk memastikan proxy lazy Hibernate yang
	 *         sudah terdeteksi/di-cache tidak meledak (lazy-init exception) saat diakses di luar
	 *         sesi Hibernate — pola cache L1/L3 yang sama dipakai di banyak relasi
	 *         {@code @ManyToOne} lain pada codebase ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = true)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/** @param parameterTambahan atribut dinamis tambahan untuk satuan KPI ini. */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

}
