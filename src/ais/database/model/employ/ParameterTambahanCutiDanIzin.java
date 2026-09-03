package ais.database.model.employ;

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
 * Entitas Hibernate baris keanggotaan satu {@link ParameterTambahan} (definisi custom field
 * generik AIS) di dalam satu {@link KelompokParameterTambahanCutiDanIzin} (kelompok/tab parameter
 * tambahan untuk pengajuan cuti &amp; izin) — dipetakan ke tabel
 * {@code employ.parameter_tambahan_pengajuan_pegawai}. Baris entitas ini sendiri TIDAK menyimpan
 * nilai isian pengguna; ia hanya mendefinisikan "parameter apa saja ada di kelompok mana", sesuai
 * pola parameter-tambahan generik AIS (bandingkan dengan pasangan sejenis di paket
 * {@code payroll}, mis. {@code ParameterTambahanPengajuanTransaksiPegawai} dan
 * {@code ParameterTambahanGajiPegawai}). Nilai isian pengguna yang sesungguhnya disimpan
 * terserialisasi pada entitas dokumen (mis. {@code parameterTambahanInds} milik
 * {@link ais.database.model.payroll.CutiDanIzin}), dirujuk lewat kombinasi
 * {@link #getKelompokParameterTambahanCutiDanIzin()} dan {@link #getParameterTambahan()}.
 *
 * @see KelompokParameterTambahanCutiDanIzin
 * @see JenisCutiDanIzin
 * @see ais.database.model.payroll.CutiDanIzin
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "parameter_tambahan_pengajuan_pegawai")
public class ParameterTambahanCutiDanIzin extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris ini, di-generate database (IDENTITY). */
	private Long id;
	/** Nama pengguna audit terakhir yang mengubah baris ini. */
	private String oleh;
	/** Id pengguna audit terakhir yang mengubah baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/** @return {@link #olehId} — id pengguna audit terakhir yang mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Meng-set {@link #olehId}; dilewati (no-op) bila {@code olehId} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param olehId id pengguna yang melakukan perubahan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Meng-set {@link #oleh}; dilewati (no-op) bila {@code oleh} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param oleh nama pengguna yang melakukan perubahan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return {@link #oleh} — nama pengguna audit terakhir yang mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE; mendelegasikan pembaruan {@link #tanggal_dirubah} ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak dipanggil manual
	 * dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah nilai timestamp audit baru; dipanggil manual maupun otomatis oleh {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return {@link #tanggal_dirubah} — timestamp terakhir baris ini diubah; nilai awal saat konstruksi objek adalah waktu sekarang. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kelompok parameter tambahan yang menaungi parameter ini. */
	private KelompokParameterTambahanCutiDanIzin kelompokParameterTambahanCutiDanIzin;
	/** Definisi parameter tambahan generik AIS yang menjadi rujukan baris keanggotaan ini. */
	private ParameterTambahan parameterTambahan;

	/** Cache; nilai efektif selalu dihitung ulang di {@link #getNomorUrut()} dari {@link #parameterTambahan}. */
	private Integer nomorUrut;

	/**
	 * @return nomor urut tampilan baris ini — SELALU dihitung ulang dari
	 *         {@link #getParameterTambahan()}{@code .getNomorUrut()} bila
	 *         {@link #parameterTambahan} terisi (field {@link #nomorUrut} instance hanya dipakai
	 *         sebagai fallback/cache terakhir); {@code 1} bila keduanya kosong.
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * @param nomorUrut nilai cache/fallback lokal; TIDAK menggantikan nomor urut sesungguhnya,
	 *                   karena {@link #getNomorUrut()} akan menghitung ulang &amp; menimpa field
	 *                   ini dari {@link #parameterTambahan} pada pemanggilan berikutnya bila
	 *                   {@code parameterTambahan} terisi.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field. */
	public ParameterTambahanCutiDanIzin() {
	}

	/** @return {@link #id} — primary key baris ini. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru; normalnya di-generate database, jarang di-set manual dari kode aplikasi. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return {@link #parameterTambahan} — definisi parameter tambahan generik yang dirujuk baris ini, dilewatkan {@link #check(Object)} (pola lazy-init standar {@code GeneralValueObject}). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/** @param parameterTambahan definisi parameter tambahan generik baru. */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * @return {@link #kelompokParameterTambahanCutiDanIzin} — kelompok yang menaungi parameter ini,
	 *         dilewatkan {@link #check(Object)} (pola lazy-init standar {@code GeneralValueObject}).
	 *         Kolom join {@code kelompok_parameter_tambahan_pengajuan_pegawai} — perhatikan nama
	 *         kolom ini SAMA PERSIS dengan nama tabel {@link KelompokParameterTambahanCutiDanIzin}
	 *         (lihat catatan skema pada Javadoc kelas tersebut), jangan tertukar dengan
	 *         nama-nama tabel/kolom parameter tambahan modul lain (mis. pengajuan-transaksi,
	 *         gaji-pegawai) yang polanya mirip.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_pengajuan_pegawai", nullable = true)
	public KelompokParameterTambahanCutiDanIzin getKelompokParameterTambahanCutiDanIzin() {
		kelompokParameterTambahanCutiDanIzin = check(kelompokParameterTambahanCutiDanIzin);
		return kelompokParameterTambahanCutiDanIzin;
	}

	/** @param kelompokParameterTambahanCutiDanIzin kelompok parameter tambahan baru. */
	public void setKelompokParameterTambahanCutiDanIzin(
			KelompokParameterTambahanCutiDanIzin kelompokParameterTambahanCutiDanIzin) {
		this.kelompokParameterTambahanCutiDanIzin = kelompokParameterTambahanCutiDanIzin;
	}

}
