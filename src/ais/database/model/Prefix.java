package ais.database.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity master data <b>prefix kode mata kuliah</b> pada tabel {@code public.prefix} (mis.
 * "TI", "SI", "MTK" sebagai awalan kode mata kuliah). Dipakai layar {@code PrefixAction} untuk
 * mengelola daftar prefix, dan dikaitkan opsional ke {@link Matakuliah#getPrefix()} lewat
 * combobox pada {@code MatakuliahAction} — kemunculan kolom ini di layar mata kuliah digerbangi
 * konfigurasi {@code tampil_prefix_matakuliah}.
 *
 * <p>Kelas ini murni tabel referensi: hanya {@link #getNamaPrefix()} dan
 * {@link #getKeterangan()} sebagai data bisnis, tanpa sakelar {@code aktif} maupun
 * {@code nomorUrut}. Perhatikan penamaan kolom database {@code prefix} yang dipetakan ke
 * properti Java {@link #getNamaPrefix()} (bukan {@code getPrefix()}), dan
 * {@link #toString()} yang mengembalikan {@link #getKeterangan()} — <b>bukan</b>
 * {@link #getNamaPrefix()} seperti kebanyakan master data sejenis.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Matakuliah
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "prefix")
public class Prefix extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 */
	private static final long serialVersionUID = 665223576015880477L;

	/** Kunci utama tabel {@code public.prefix} ({@code IDENTITY}). */
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

	/**
	 * @return {@link #getKeterangan()} apa adanya — representasi teks baris ini. Perhatikan
	 *         ini <b>bukan</b> {@link #getNamaPrefix()}, berbeda dari kebanyakan master data
	 *         sejenis yang memakai nama sebagai representasi teks.
	 */
	public String toString() {
		return keterangan;
	}

	/** Kode prefix mata kuliah (mis. "TI"); dipetakan ke kolom database {@code prefix}. */
	private String namaPrefix;
	/** Catatan/keterangan bebas tentang prefix ini; boleh {@code null}. */
	private String keterangan;

	/**
	 * @return kunci utama baris ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Long getId() {
		return id;
	}

	/** @param id kunci utama baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode prefix mata kuliah apa adanya (tanpa trimming). Properti bernama
	 *         {@code namaPrefix} ini dipetakan ke kolom database {@code prefix}.
	 */
	@Column(name = "prefix", nullable = false)
	public String getNamaPrefix() {
		return namaPrefix;
	}

	/** @param namaPrefix kode prefix mata kuliah; disimpan apa adanya. */
	public void setNamaPrefix(String namaPrefix) {
		this.namaPrefix = namaPrefix;
	}

	/** @return keterangan bebas prefix ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk prefix ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
