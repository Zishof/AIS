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
 * Entity relasi <b>banyak-ke-banyak Ujian &harr; BankSoal</b> pada tabel
 * {@code public.ujian_punya_soal}. Satu baris = satu {@link BankSoal} (kumpulan/butir soal)
 * yang dipasangkan ke satu {@link Ujian}, lengkap dengan {@link #getNomorUrut()} yang
 * menentukan urutan tampil/pengerjaan soal tersebut dalam ujian.
 *
 * <p>Kelas ini murni tabel penghubung: tidak ada field bisnis lain selain dua relasi wajib
 * dan nomor urut. {@link #toString()} punya <b>efek samping</b> memanggil ulang
 * {@link #getUjian()}/{@link #getBankSoal()} (yang masing-masing bisa menugaskan kembali hasil
 * {@code check()} ke field) sebelum merangkai representasi teksnya.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Ujian
 * @see BankSoal
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "ujian_punya_soal")

public class UjianPunyaSoal extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan sejumlah entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2461822577548439808L;
	/** Kunci utama tabel {@code public.ujian_punya_soal} ({@code IDENTITY}). */
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
	 * @return representasi teks berbentuk {@code "<ujian>_<bankSoal>"}. <b>Efek samping:</b>
	 *         memanggil ulang {@link #getUjian()} dan {@link #getBankSoal()} (masing-masing
	 *         menugaskan kembali hasil {@code check()} ke field terkait) sebelum merangkai
	 *         string, alih-alih membaca field secara langsung.
	 */
	public String toString() {
		ujian = getUjian();
		bankSoal = getBankSoal();
		return ujian + "_" + bankSoal;
	}

	/** Ujian yang dipasangkan soal ini. Wajib diisi (kolom {@code NOT NULL}). */
	private Ujian ujian;
	/** Kumpulan/butir soal yang dipasangkan ke ujian. Wajib diisi (kolom {@code NOT NULL}). */
	private BankSoal bankSoal;
	/** Nomor urut tampil soal ini dalam ujian; {@code null} diperlakukan sebagai {@code 0}. */
	private Integer nomorUrut;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public UjianPunyaSoal() {
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

	/** @param ujian ujian yang dipasangkan dengan soal ini. */
	public void setUjian(Ujian ujian) {
		this.ujian = ujian;
	}

	/**
	 * @return {@link Ujian} yang dipasangkan. Referensi dicek lewat {@code check(ujian)}
	 *         sebelum dikembalikan (proxy Hibernate basi diganti entity segar bila perlu).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ujian", nullable = false)
	public Ujian getUjian() {
		ujian = check(ujian);
		return ujian;
	}

	/** @param bankSoal kumpulan/butir soal yang dipasangkan ke ujian. */
	public void setBankSoal(BankSoal bankSoal) {
		this.bankSoal = bankSoal;
	}

	/**
	 * @return {@link BankSoal} yang dipasangkan. Referensi dicek lewat {@code check(bankSoal)}
	 *         sebelum dikembalikan, sama seperti {@link #getUjian()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_soal", nullable = false)
	public BankSoal getBankSoal() {
		bankSoal = check(bankSoal);
		return bankSoal;
	}

	/**
	 * @return nomor urut tampil soal ini dalam ujian. <b>Efek samping:</b> {@code null}
	 *         diganti {@code 0} pada nilai kembalian, tanpa menulis balik ke field.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/** @param nomorUrut nomor urut tampil soal ini; {@code null} akan tampil sebagai 0 saat dibaca. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}
}
