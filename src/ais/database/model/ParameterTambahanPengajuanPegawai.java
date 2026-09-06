package ais.database.model;

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

/**
 * Entity <b>pendaftaran parameter tambahan untuk pengajuan pegawai</b> pada tabel
 * {@code public.parameter_tambahan_pengajuan_pegawai}. Satu baris menyatakan bahwa satu
 * {@link ParameterTambahan} (definisi isian dinamis) berlaku untuk form
 * {@link PengajuanPegawai}, opsional dikelompokkan lewat
 * {@link #getKelompokParameterTambahanPengajuanPegawai()} — konfigurasi ini dibaca layar
 * {@code ParameterTambahanPengajuanPegawaiAction} dan dirender secara dinamis oleh
 * {@code ParameterTambahanPengajuanPegawaiListener} pada form pengajuan pegawai.
 *
 * <p><b>Pola namespace jenis/ref sudah memakai mekanisme terpusat yang benar.</b> Berbeda dari
 * pola tabrakan jenis-namespace lampiran yang pernah ditemukan pada sejumlah entity lain
 * (task_484d4bd0), {@code ParameterTambahanPengajuanPegawaiListener} membangun kunci
 * "jenis" lampiran/nilai parameter tambahan lewat pemanggilan terpusat
 * {@code LampiranLain.resolveJenisParameterTambahan(PengajuanPegawai.class,
 * pengajuanPegawai.getId(), kelompokParameterTambahanPengajuanPegawai.getId() + "-&gt;" +
 * parameterTambahan.getId())} — bukan menyusun string kunci secara manual — sehingga modul ini
 * sudah konsisten dengan perbaikan namespace yang sudah diterapkan di ~90 titik lain dan tidak
 * memerlukan penambalan ulang.</p>
 *
 * <p><b>{@link #getNomorUrut()} adalah getter-mutasi turunan</b>: setiap dipanggil, ia membaca
 * ulang {@link #getParameterTambahan()} dan <b>menimpa</b> field {@code nomorUrut} miliknya
 * sendiri dengan {@code parameterTambahan.getNomorUrut()} bila relasi tersebut tidak
 * {@code null} — nilai yang pernah diisi lewat {@link #setNomorUrut(Integer)} secara langsung
 * karena itu tidak pernah bertahan setelah getter ini dipanggil sekali. Baru bila
 * {@link #getParameterTambahan()} mengembalikan {@code null} dan field {@code nomorUrut} juga
 * masih {@code null}, getter jatuh ke bawaan {@code 1}.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut. Kelas ini tidak meng-override {@code toString()} (memakai bawaan {@code Object}).</p>
 *
 * @see GeneralValueObject
 * @see ParameterTambahan
 * @see PengajuanPegawai
 * @see KelompokParameterTambahanPengajuanPegawai
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "parameter_tambahan_pengajuan_pegawai")
public class ParameterTambahanPengajuanPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.parameter_tambahan_pengajuan_pegawai} ({@code IDENTITY}). */
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

	/** Kelompok opsional tempat parameter tambahan ini ditampilkan; boleh {@code null}. */
	private KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai;
	/** Definisi isian dinamis yang berlaku untuk form pengajuan pegawai ini. Wajib diisi (kolom {@code NOT NULL}). */
	private ParameterTambahan parameterTambahan;

	/** Nomor urut tampil; lihat catatan efek samping pada {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * @return nomor urut tampil parameter tambahan ini. <b>Efek samping ganda:</b> method ini
	 *         terlebih dulu memanggil {@link #getParameterTambahan()} (yang sendiri bisa
	 *         menugaskan kembali hasil {@code check()} ke field {@code parameterTambahan}),
	 *         lalu — bila hasilnya tidak {@code null} — <b>menimpa</b> field
	 *         {@code nomorUrut} milik entity ini dengan {@code parameterTambahan.getNomorUrut()}.
	 *         Nilai yang pernah diset langsung lewat {@link #setNomorUrut(Integer)} karena itu
	 *         hilang begitu getter ini dipanggil selama relasi {@code parameterTambahan}
	 *         terisi. Hanya jatuh ke bawaan {@code 1} bila relasi maupun field keduanya
	 *         {@code null}.
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * @param nomorUrut nomor urut tampil; perhatikan nilai ini akan ditimpa oleh
	 *                   {@link #getNomorUrut()} pada pemanggilan berikutnya selama
	 *                   {@link #getParameterTambahan()} tidak {@code null}.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public ParameterTambahanPengajuanPegawai() {
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

	/**
	 * @return definisi {@link ParameterTambahan} yang berlaku untuk form ini. Referensi dicek
	 *         lewat {@code check(parameterTambahan)} sebelum dikembalikan (proxy Hibernate
	 *         basi diganti entity segar bila perlu).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/** @param parameterTambahan definisi isian dinamis yang berlaku untuk form ini. */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * @return kelompok opsional tempat parameter tambahan ini ditampilkan, atau {@code null}
	 *         bila tidak dikelompokkan. Referensi dicek lewat {@code check(...)} sebelum
	 *         dikembalikan, sama seperti {@link #getParameterTambahan()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_pengajuan_pegawai", nullable = true)
	public KelompokParameterTambahanPengajuanPegawai getKelompokParameterTambahanPengajuanPegawai() {
		kelompokParameterTambahanPengajuanPegawai = check(kelompokParameterTambahanPengajuanPegawai);
		return kelompokParameterTambahanPengajuanPegawai;
	}

	/** @param kelompokParameterTambahanPengajuanPegawai kelompok opsional tempat parameter tambahan ini ditampilkan. */
	public void setKelompokParameterTambahanPengajuanPegawai(
			KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai) {
		this.kelompokParameterTambahanPengajuanPegawai = kelompokParameterTambahanPengajuanPegawai;
	}

}
