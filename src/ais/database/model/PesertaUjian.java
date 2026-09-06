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

/**
 * Entity <b>peserta ujian</b> pada tabel {@code public.peserta_ujian}. Satu baris mengaitkan
 * satu peserta — {@link #getBiodataCalonMahasiswa()} (calon mahasiswa, jalur PMB/ujian saringan
 * masuk) <b>atau</b> {@link #getMahasiswa()} (mahasiswa terdaftar) — ke satu sesi/jadwal
 * {@link #getPertemuan()} tempat ujian berlangsung.
 *
 * <p><b>Ketiga relasi sama-sama {@code nullable = true}</b>, berbeda dari kebanyakan entity
 * penghubung di paket ini yang mewajibkan relasinya. Tidak ada constraint di level Java yang
 * memastikan setidaknya satu dari {@code biodataCalonMahasiswa}/{@code mahasiswa} terisi;
 * pemanggil bertanggung jawab mengisi kombinasi yang sesuai jalur ujian (PMB vs. ujian
 * reguler mahasiswa aktif).</p>
 *
 * <p>Ketiga getter relasi memakai {@code @Fetch(FetchMode.SELECT)} dan <b>tidak</b> memanggil
 * {@code check()} seperti kebanyakan entity lain di paket ini — proxy Hibernate dikembalikan
 * apa adanya tanpa penyegaran.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Pertemuan
 * @see BiodataCalonMahasiswa
 * @see Mahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "peserta_ujian")

public class PesertaUjian extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.peserta_ujian} ({@code IDENTITY}). */
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

	/** @return representasi teks berbentuk {@code "<id>-<biodataCalonMahasiswa>"}. */
	public String toString() {
		return id + "-" + biodataCalonMahasiswa;
	}

	/** Sesi/jadwal ujian yang diikuti peserta ini. Boleh {@code null}. */
	private Pertemuan pertemuan;
	/** Calon mahasiswa peserta (jalur PMB/ujian saringan masuk). Boleh {@code null}. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Mahasiswa terdaftar peserta (jalur ujian reguler). Boleh {@code null}. */
	private Mahasiswa mahasiswa;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public PesertaUjian() {
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
	 * @return calon mahasiswa peserta, atau {@code null} bila peserta berasal dari jalur
	 *         mahasiswa terdaftar ({@link #getMahasiswa()}). Dikembalikan apa adanya tanpa
	 *         {@code check()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa;
	}

	/** @param biodataCalonMahasiswa calon mahasiswa peserta ujian ini. */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * @return sesi/jadwal {@link Pertemuan} ujian yang diikuti, atau {@code null} bila belum
	 *         diisi. Dikembalikan apa adanya tanpa {@code check()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertemuan", nullable = true)
	public Pertemuan getPertemuan() {
		return pertemuan;
	}

	/** @param pertemuan sesi/jadwal ujian yang diikuti peserta ini. */
	public void setPertemuan(Pertemuan pertemuan) {
		this.pertemuan = pertemuan;
	}

	/**
	 * @return mahasiswa terdaftar peserta, atau {@code null} bila peserta berasal dari jalur
	 *         calon mahasiswa ({@link #getBiodataCalonMahasiswa()}). Dikembalikan apa adanya
	 *         tanpa {@code check()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/** @param mahasiswa mahasiswa terdaftar peserta ujian ini. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

}
