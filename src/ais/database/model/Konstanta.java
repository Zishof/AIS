package ais.database.model;

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

/**
 * Entity master data <b>konstanta formula global</b> pada tabel {@code public.konstanta}. Setiap
 * baris adalah satu pasangan token&rarr;nilai yang dipakai sebagai <i>placeholder</i> substitusi
 * teks di dalam string rumus/formula (payroll golongan, KPI, masa kerja, dsb.) sebelum
 * dievaluasi lewat {@code exp4j}: {@link #getKode() kode} adalah token yang dicari di dalam teks
 * rumus (dikelilingi spasi, mis. {@code " TOKEN "}), dan {@link #getKeterangan() keterangan}
 * adalah nilai numerik (disimpan sebagai teks) yang menggantikannya. Lihat pemakaiannya di
 * {@code ais.action.master.employ.helper.MasaKerjaUtil} (dan util formula sejenis) lewat
 * {@code ConstantValues.ambilBerdasarClass(Konstanta.class)}.
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "konstanta")

public class Konstanta extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.konstanta} ({@code IDENTITY}). */
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

	/** @return representasi teks berbentuk {@code "<id>-<nama>"}, dipakai label combobox ZK. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Token substitusi konstanta ini di dalam teks rumus; lihat {@link #getKode()}. */
	private String kode;

	/** Nama deskriptif konstanta ini (untuk keperluan tampilan/pencarian); wajib diisi. */
	private String nama;
	/**
	 * Nilai konstanta, disimpan sebagai teks meski dipakai sebagai angka pada substitusi rumus;
	 * lihat {@link #getKeterangan()} untuk perilaku default.
	 */
	private String keterangan;
	/** Status aktif/nonaktif; lihat {@link #getAktif()} untuk perilaku default. */
	private Boolean aktif;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public Konstanta() {
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
	 * @return token substitusi ini setelah dibuang seluruh tanda baca kecuali {@code _} dan
	 *         {@code -}, lalu di-trim; string kosong (bukan {@code null}) bila belum pernah diisi.
	 *         Token inilah yang dicocokkan (dikelilingi spasi) di dalam teks rumus untuk diganti
	 *         dengan {@link #getKeterangan()}.
	 */
	public String getKode() {
		return kode == null ? "" : kode.replaceAll("[\\p{Punct}&&[^_-]]+", "").trim();
	}

	/** @param kode token substitusi konstanta ini; disimpan apa adanya, penyaringan tanda baca terjadi di {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama deskriptif konstanta ini, di-trim; {@code null} bila belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama deskriptif konstanta ini; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return nilai konstanta ini (di-trim) yang akan menggantikan {@link #getKode() token} pada
	 *         teks rumus; default {@code "0"} bila belum pernah diisi sehingga substitusi rumus
	 *         tetap menghasilkan ekspresi numerik valid, bukan {@code null}/teks kosong.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "0" : this.keterangan.trim();
	}

	/** @param keterangan nilai konstanta ini; disimpan apa adanya, trimming/default terjadi di {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif konstanta; default {@code true} bila belum pernah diisi (flag aktif
	 *         satu-arah — baris lama tanpa nilai eksplisit dianggap aktif, nilai default tidak
	 *         ditulis balik ke field). Hanya konstanta aktif yang disubstitusikan ke rumus (lihat
	 *         pemanggil di {@code MasaKerjaUtil}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif konstanta ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
