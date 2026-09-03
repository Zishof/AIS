package ais.database.model.employ;

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




import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;



/**
 * Entitas Hibernate isi peraturan kepegawaian AIS — dipetakan ke tabel {@code employ.peraturan}.
 * Satu baris merepresentasikan satu dokumen/pasal peraturan (kode, nama, isi teks lengkap, tanggal
 * berlaku), dikelompokkan lewat {@link #getJenisPeraturan()} ke satu {@link JenisPeraturan}. Tidak
 * ditemukan relasi langsung dari entitas ini ke rantai disiplin pegawai ({@link PelanggaranPegawai}/
 * {@link HukumanPegawai}/{@link PelanggaranDanHukumanPegawai}/{@link PendataanPelanggaranPegawai})
 * di paket ini — keterkaitan antara suatu pelanggaran dengan pasal peraturan yang dilanggar (bila
 * ada) dikelola di luar model data ini (mis. lewat teks bebas {@code keterangan} pada entitas
 * disiplin, bukan lewat foreign key).
 *
 * @see JenisPeraturan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "peraturan")



public class Peraturan extends GeneralValueObject {

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

	/** @return representasi teks baris ini: {@link #nama} apa adanya (bukan {@link #id}, berbeda dengan beberapa entitas lain di paket ini). */
	public String toString() {
		return nama;
	}

	/** Kode/nomor peraturan (mis. nomor SK), boleh {@code null}; TIDAK dipetakan lewat {@code @Column} eksplisit (mengandalkan konvensi penamaan default Hibernate). */
	private String kode;
	/** Nama/judul peraturan. */
	private String nama;
	/** Isi teks lengkap peraturan, boleh {@code null}. */
	private String isi;
	/** Kategori/jenis peraturan ini. */
	private JenisPeraturan jenisPeraturan;
	/** Tanggal peraturan ini mulai berlaku, boleh {@code null}. */
	private Date tanggalBerlaku;
	/** Keterangan/catatan bebas untuk peraturan ini, boleh {@code null}. */
	private String keterangan;
	/** Menandai apakah peraturan ini masih aktif/berlaku; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif;

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field. */
	public Peraturan() {
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

	/** @return {@link #nama} yang sudah di-trim; {@code null} bila {@link #nama} {@code null}. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama/judul peraturan baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return {@link #keterangan} — keterangan/catatan bebas peraturan ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/catatan bebas baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return {@link #isi} — isi teks lengkap peraturan ini, boleh {@code null}. */
	@Column(name = "isi", columnDefinition = "text", nullable = true)
	public String getIsi() {
		return isi;
	}

	/** @param isi isi teks lengkap peraturan baru. */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/** @return {@link #kode} apa adanya (tidak di-trim, berbeda dengan {@link #getNama()}); boleh {@code null}. */
	public String getKode() {
		return kode;
	}

	/** @param kode kode/nomor peraturan baru. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return {@link #tanggalBerlaku} — tanggal peraturan ini mulai berlaku, boleh {@code null}. */
	@Column(name = "tanggal_berlaku")
	public Date getTanggalBerlaku() {
		return tanggalBerlaku;
	}

	/** @param tanggalBerlaku tanggal mulai berlaku baru. */
	public void setTanggalBerlaku(Date tanggalBerlaku) {
		this.tanggalBerlaku = tanggalBerlaku;
	}

	/**
	 * @return {@link #jenisPeraturan} — kategori peraturan ini apa adanya (TIDAK dilewatkan
	 *         {@link #check(Object)}, berbeda dengan pola lazy-init pada relasi {@code @ManyToOne}
	 *         milik entitas lain di paket ini, mis. {@link JenisCutiDanIzin#getStatusabsensi()}) —
	 *         proxy Hibernate yang belum ter-inisialisasi bisa langsung dikembalikan apa adanya.
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinColumn(name = "jenis_peraturan")
	public JenisPeraturan getJenisPeraturan() {
		return jenisPeraturan;
	}

	/** @param jenisPeraturan kategori peraturan baru. */
	public void setJenisPeraturan(JenisPeraturan jenisPeraturan) {
		this.jenisPeraturan = jenisPeraturan;
	}

	/** @return {@link #aktif}; {@code true} bila belum pernah di-set ({@code null}) — default aktif. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif baru untuk peraturan ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
