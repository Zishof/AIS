package ais.database.model.kpi;

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.ui.util.WaktuUtil;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.masa_kpi} — master periode/masa penilaian KPI
 * (mis. per bulan, triwulan, atau tahun akademik) yang membingkai jendela waktu penilaian
 * kinerja pegawai.
 *
 * <p>Kelas ini membawa <em>tiga pasang jendela tanggal</em> yang tujuannya berbeda-beda:</p>
 * <ul>
 *   <li>{@link #getMulai() mulai}/{@link #getSampai() sampai} — rentang periode KPI itu sendiri
 *       (mis. tanggal 1 s.d. akhir bulan yang dinilai). Tidak punya default; boleh {@code null}.</li>
 *   <li>{@link #getMulaitarget() mulaitarget}/{@link #getSampaitarget() sampaitarget} — rentang
 *       target/tenggat terkait periode ini (mis. jendela pengisian target sebelum periode
 *       berjalan). Juga tidak punya default; boleh {@code null}.</li>
 *   <li>{@link #getBerlakumulai() berlakumulai}/{@link #getBerlakusampai() berlakusampai} —
 *       jendela validitas baris master ini sendiri (kapan baris ini "berlaku" dipakai sistem).
 *       <strong>Berbeda dari dua pasang di atas</strong>, pasangan ini punya default implisit:
 *       jika belum diisi, {@code berlakumulai} jatuh ke "kemarin" dan {@code berlakusampai}
 *       jatuh ke "besok" ({@link ais.ui.util.WaktuUtil#kemarin()}/{@link ais.ui.util.WaktuUtil#besok()}),
 *       sehingga baris yang belum pernah diisi kolom validitasnya otomatis tampak "berlaku hari
 *       ini" tanpa perlu migrasi data historis. Ini pola default fail-open yang sama dipakai di
 *       entity master lain pada codebase — bukan bug, tetapi berarti kode pemanggil yang
 *       memfilter berdasarkan tanggal berlaku harus menggunakan getter ini (bukan field mentah)
 *       agar konsisten.</li>
 * </ul>
 *
 * <p>Field-field audit shadow ({@link #getOleh() oleh}/{@link #getOlehId() olehId}/
 * {@link #getTanggal_dirubah() tanggal_dirubah}) mengikuti pola berulang yang sama di seluruh
 * entity KPI: redundan secara sengaja terhadap riwayat Envers
 * ({@link org.hibernate.envers.Audited @Audited}), bukan bug.</p>
 *
 * @see ais.database.model.kpi.KategoriKpi
 * @see ais.ui.util.WaktuUtil#kemarin()
 * @see ais.ui.util.WaktuUtil#besok()
 * @see ais.common.Common#getCurrentTahunAkademik()
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "masa_kpi")
public class MasaKpi extends GeneralValueObject {

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
	private String ta;
	private String nama;
	private String keterangan;
	private Boolean aktif;
	private Date sampai;
	private Date mulai;

	private Date mulaitarget;
	private Date sampaitarget;

	private Date berlakusampai;
	private Date berlakumulai;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public MasaKpi() {
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

	/** @return kode masa KPI, di-trim; string kosong ({@code ""}) bila belum diisi atau kosong. */
	public String getKode() {
		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	/** @param kode kode masa KPI (belum di-trim/divalidasi saat disimpan). */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama masa KPI, di-trim; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama masa KPI (kolom wajib/{@code NOT NULL} di database). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan masa KPI ini; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi tambahan masa KPI ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif masa KPI ini. Bila belum pernah diset ({@code null}) dianggap
	 *         <strong>aktif</strong> — pola "default aktif" yang konsisten dipakai di seluruh
	 *         entity master KPI pada paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif masa KPI ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return tanggal mulai periode KPI ini; boleh {@code null} bila belum diisi. */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/** @param mulai tanggal mulai periode KPI ini. */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/** @return tanggal akhir periode KPI ini; boleh {@code null} bila belum diisi. */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/** @param sampai tanggal akhir periode KPI ini. */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * @return tanggal mulai validitas baris master ini. Bila belum pernah diset ({@code null}),
	 *         jatuh ke "kemarin" ({@link ais.ui.util.WaktuUtil#kemarin()}) — lihat catatan
	 *         jendela validitas pada dokumentasi kelas — sehingga baris tanpa nilai eksplisit
	 *         tampak sudah berlaku sejak sebelum hari ini.
	 */
	@Temporal(TemporalType.DATE)
	public Date getBerlakumulai() {
		return berlakumulai == null ? WaktuUtil.kemarin() : berlakumulai;
	}

	/** @param berlakumulai tanggal mulai validitas baris master ini. */
	public void setBerlakumulai(Date berlakumulai) {
		this.berlakumulai = berlakumulai;
	}

	/**
	 * @return tanggal akhir validitas baris master ini. Bila belum pernah diset ({@code null}),
	 *         jatuh ke "besok" ({@link ais.ui.util.WaktuUtil#besok()}) — lihat catatan jendela
	 *         validitas pada dokumentasi kelas — sehingga baris tanpa nilai eksplisit tampak
	 *         masih berlaku sampai setelah hari ini.
	 */
	@Temporal(TemporalType.DATE)
	public Date getBerlakusampai() {
		return berlakusampai == null ? WaktuUtil.besok() : berlakusampai;
	}

	/** @param berlakusampai tanggal akhir validitas baris master ini. */
	public void setBerlakusampai(Date berlakusampai) {
		this.berlakusampai = berlakusampai;
	}

	/**
	 * @return kode tahun akademik yang berlaku untuk masa KPI ini. Bila belum pernah diset
	 *         ({@code null}), jatuh ke tahun akademik berjalan via
	 *         {@link ais.common.Common#getCurrentTahunAkademik()}.
	 */
	public String getTa() {
		return ta == null ? Common.getCurrentTahunAkademik() : ta;
	}

	/** @param ta kode tahun akademik untuk masa KPI ini. */
	public void setTa(String ta) {
		this.ta = ta;
	}

	/** @return tanggal mulai jendela target/tenggat terkait masa KPI ini; boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	public Date getMulaitarget() {
		return mulaitarget;
	}

	/** @param mulaitarget tanggal mulai jendela target/tenggat terkait masa KPI ini. */
	public void setMulaitarget(Date mulaitarget) {
		this.mulaitarget = mulaitarget;
	}

	/** @return tanggal akhir jendela target/tenggat terkait masa KPI ini; boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	public Date getSampaitarget() {
		return sampaitarget;
	}

	/** @param sampaitarget tanggal akhir jendela target/tenggat terkait masa KPI ini. */
	public void setSampaitarget(Date sampaitarget) {
		this.sampaitarget = sampaitarget;
	}
}
