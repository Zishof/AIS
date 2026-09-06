package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.akunting.Akun;

/**
 * Entity master data <b>pemetaan jenis input pembayaran &rarr; akun debet</b> pada tabel
 * {@code public.input_pembayaran_punya_akun}. Menautkan satu {@link #getJenis() jenis input
 * pembayaran} (kategori metode pembayaran tetap, mis. "Tunai"/"Transfer"/"Kartu", dipilih lewat
 * combobox tetap pada {@code ItemPembayaranPunyaAkunAction}, bukan teks bebas) dengan satu
 * {@link Akun} pada bagan akun (chart of accounts) yang akan didebet secara otomatis saat
 * pembayaran dengan jenis tersebut diposting ke jurnal akuntansi.
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Akun
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "input_pembayaran_punya_akun")

public class InputPembayaranPunyaAkun extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.input_pembayaran_punya_akun} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan diam-diam
	 * (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, masukan
	 * kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi ulang
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk dipanggil
	 * langsung dari kode aplikasi.
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

	/** @return {@link #getKeterangan() keterangan} baris ini apa adanya. */
	public String toString() {
		return keterangan;
	}

	/** Kode/nama jenis input pembayaran (dipilih dari daftar tetap, bukan teks bebas); wajib diisi. */
	private String jenis;
	/** Catatan/keterangan bebas untuk pemetaan ini. */
	private String keterangan;
	/** Akun debet pada bagan akun yang dipakai saat pembayaran jenis ini diposting; boleh kosong. */
	private Akun akunDebet;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public InputPembayaranPunyaAkun() {
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

	/** @return keterangan bebas pemetaan ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk pemetaan ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return jenis input pembayaran apa adanya, tanpa normalisasi. */
	public String getJenis() {
		return jenis;
	}

	/** @param jenis kode/nama jenis input pembayaran; disimpan apa adanya. */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * @return {@link Akun} debet yang dipakai saat pembayaran jenis ini diposting, atau
	 *         {@code null} bila belum diisi; relasi {@code @ManyToOne} eager (bukan
	 *         {@code FetchType.LAZY}) dengan strategi {@code FetchMode.SELECT}, sehingga sudah
	 *         terinisialisasi saat baris ini dimuat dan tidak dipanggilkan {@code check()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "akun_debet", nullable = true)
	public Akun getAkunDebet() {
		return akunDebet;
	}

	/** @param akunDebet akun debet pada bagan akun untuk jenis pembayaran ini; boleh {@code null}. */
	public void setAkunDebet(Akun akunDebet) {
		this.akunDebet = akunDebet;
	}

}
