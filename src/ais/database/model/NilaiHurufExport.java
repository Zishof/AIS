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
 * Entity bantu <b>ekspor nilai huruf ke Feeder/PDDIKTI per jurusan</b> pada tabel
 * {@code public.nilai_huruf_export}. Menautkan satu baris {@link NilaiHuruf} (definisi
 * konversi angka&harr;huruf) dengan satu {@link Jurusan} beserta {@link #getFeeder() kode Feeder}
 * yang sesuai, karena skema kode nilai huruf pada Feeder/PDDIKTI Neo-Feeder bisa berbeda
 * per-jurusan walau {@code NilaiHuruf}-nya sama. Jangan tertukar dengan {@link NilaiHuruf}
 * (definisi konversi nilai itu sendiri) maupun {@link JenisNilaiHurufMatakuliah} (nama skema,
 * bukan pemetaan ekspor).
 *
 * <p>Kelas ini tidak meng-override {@link #toString()}; representasi teksnya memakai bawaan
 * {@link GeneralValueObject#toString()} ({@code "kode - nama"}), padahal kelas ini tidak memetakan
 * {@code kode}/{@code nama} sebagai kolom sendiri — dalam praktiknya akan tampil sebagai
 * {@code "null"} bila dipakai pada komponen ZK yang mengandalkan representasi tersebut.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see NilaiHuruf
 * @see Jurusan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nilai_huruf_export")

public class NilaiHurufExport extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.nilai_huruf_export} ({@code IDENTITY}). */
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

	/** Jurusan tempat pemetaan kode Feeder ini berlaku; relasi eager (lihat catatan kelas). */
	private Jurusan jurusan;
	/** Definisi nilai huruf yang dipetakan; relasi eager (lihat catatan kelas). */
	private NilaiHuruf nilaiHuruf;

	/** Kode nilai huruf sesuai skema Feeder/PDDIKTI untuk pasangan jurusan-nilai huruf ini. */
	private String feeder;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public NilaiHurufExport() {
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

	/** @return kode Feeder, di-trim; {@code null} bila kosong/belum pernah diisi. */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/** @param feeder kode nilai huruf sesuai skema Feeder/PDDIKTI; disimpan apa adanya. */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * @return {@link Jurusan} tempat pemetaan kode Feeder ini berlaku; relasi {@code @ManyToOne}
	 *         eager (bukan {@code FetchType.LAZY}) dengan strategi {@code FetchMode.SELECT},
	 *         sehingga sudah terinisialisasi saat baris ini dimuat dan tidak dipanggilkan
	 *         {@code check()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = false)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/** @param jurusan jurusan tempat pemetaan kode Feeder ini berlaku; wajib diisi. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return {@link NilaiHuruf} yang dipetakan; relasi {@code @ManyToOne} eager (bukan
	 *         {@code FetchType.LAZY}) dengan strategi {@code FetchMode.SELECT}, sehingga sudah
	 *         terinisialisasi saat baris ini dimuat dan tidak dipanggilkan {@code check()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "nilai_huruf", nullable = false)
	public NilaiHuruf getNilaiHuruf() {
		return nilaiHuruf;
	}

	/** @param nilaiHuruf definisi nilai huruf yang dipetakan pada jurusan ini; wajib diisi. */
	public void setNilaiHuruf(NilaiHuruf nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

}
