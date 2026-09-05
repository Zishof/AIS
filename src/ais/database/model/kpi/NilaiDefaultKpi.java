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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.nilai_default_kpi} — nilai bawaan/default KPI
 * per pegawai untuk suatu tahun akademik ({@link #getTa() ta}), dipakai sebagai titik awal
 * sebelum penilaian aktual diisi.
 *
 * <p>Berbeda dari {@link ais.database.model.kpi.KategoriKpi} dan
 * {@link ais.database.model.kpi.SatuanKpi} yang murni master global, entity ini mengikat nilai
 * ({@link #getNilai() nilai}) ke satu {@link ais.database.model.Pegawai pegawai} tertentu untuk
 * satu tahun akademik tertentu.</p>
 *
 * <p><strong>Perhatian pola getter destruktif:</strong> {@link #getNama()} bukan getter murni —
 * setiap kali dipanggil dan {@link #getPegawai()} tidak {@code null}, field {@link #nama} ditimpa
 * ("write") dengan nama pegawai yang bersangkutan sebagai efek samping dari pemanggilan getter.
 * Ini adalah pola berulang yang sudah dikenal di codebase ini (bukan bug baru), tetapi tetap
 * berarti nilai {@link #nama} yang tersimpan di kolom database bisa jadi stale/salah bila
 * relasi pegawai berubah setelah baris ini pernah dibaca sebelum disimpan ulang — nilai kolom
 * {@code nama} efektif hanya konsisten setelah entity di-{@code save} lagi pasca pemanggilan
 * getter ini.</p>
 *
 * <p>Field-field audit shadow ({@link #getOleh() oleh}/{@link #getOlehId() olehId}/
 * {@link #getTanggal_dirubah() tanggal_dirubah}) mengikuti pola berulang yang sama di seluruh
 * entity KPI: redundan secara sengaja terhadap riwayat Envers
 * ({@link org.hibernate.envers.Audited @Audited}), bukan bug.</p>
 *
 * @see ais.database.model.Pegawai
 * @see ais.common.Common#getCurrentTahunAkademik()
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nilai_default_kpi")
public class NilaiDefaultKpi extends GeneralValueObject {

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

	private String ta;
	private Double nilai;
	private Pegawai pegawai;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public NilaiDefaultKpi() {
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

	/**
	 * @return nama tampilan baris ini. <strong>Efek samping:</strong> bila
	 *         {@link #getPegawai()} tidak {@code null}, field {@link #nama} ditimpa terlebih
	 *         dahulu dengan nama pegawai tersebut sebelum di-trim dan dikembalikan — lihat
	 *         catatan pola getter destruktif pada dokumentasi kelas.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getPegawai() != null) {
			nama = getPegawai().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama tampilan baris ini; akan ditimpa lagi oleh {@link #getNama()} pada
	 *             pemanggilan berikutnya selama {@link #pegawai} tidak {@code null}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan nilai default KPI ini; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi tambahan nilai default KPI ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif baris ini. Bila belum pernah diset ({@code null}) dianggap
	 *         <strong>aktif</strong> — pola "default aktif" yang konsisten dipakai di seluruh
	 *         entity master KPI pada paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif baris nilai default KPI ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return kode tahun akademik yang berlaku untuk nilai default ini. Bila belum pernah diset
	 *         ({@code null}), jatuh ke tahun akademik berjalan via
	 *         {@link ais.common.Common#getCurrentTahunAkademik()} — sehingga baris lama yang
	 *         belum mengisi kolom {@code ta} otomatis mengikuti tahun akademik "sekarang", bukan
	 *         tahun akademik saat baris tersebut sebetulnya dibuat.
	 */
	public String getTa() {
		return ta == null ? Common.getCurrentTahunAkademik() : ta;
	}

	/** @param ta kode tahun akademik untuk nilai default KPI ini. */
	public void setTa(String ta) {
		this.ta = ta;
	}

	/**
	 * @return {@link Pegawai} pemilik nilai default KPI ini. Sebelum dikembalikan, referensi
	 *         diresolusi via {@link GeneralValueObject#check(Object)} agar proxy lazy Hibernate
	 *         yang sudah terdeteksi/di-cache tidak meledak (lazy-init exception) saat diakses di
	 *         luar sesi Hibernate.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/** @param pegawai pegawai pemilik nilai default KPI ini. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/** @return nilai default KPI; {@code 0.0} bila belum pernah diset. */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/** @param nilai nilai default KPI untuk pegawai dan tahun akademik ini. */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

}
