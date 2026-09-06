package ais.database.model.temporary;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;



/**
 * Snapshot IPK/IP mahasiswa per semester, tabel {@code temporary.ipk_mahasiswa} — MESKI berada
 * di skema/paket {@code temporary}, entity ini **AKTIF dipakai luas** (bukan tabel staging
 * sekali-pakai): dibaca oleh dasbor admin
 * ({@code DashboardSKSDanIPKMahasiswa}/{@code DashboardDataNilaiDanIPKMahasiswa}/
 * {@code DashboardDataNilaiIPKMahasiswaPerTahunAngkatan}), layar pemantauan KRS
 * ({@code MonitorKRSMahasiswaAction}), penilaian ({@code PenilaianAction}), dan ekspor pelaporan
 * EPSBED ({@code UpdateDataMahasiswa}/{@code TransaksiStatusMahasiswa}/
 * {@code TransaksiAktivitasKuliahMahasiswa}).
 *
 * <p>Nama paket {@code temporary} di sini menyesatkan — kemungkinan menyiratkan "cache
 * turunan/hasil kalkulasi" (satu baris per mahasiswa PER SEMESTER, dihitung ulang oleh proses
 * batch/laporan dari nilai matakuliah aktual di {@code Mahasiswa}/{@code KrsMahasiswa}, bukan
 * sumber kebenaran utama), bukan berarti "sementara dan boleh dihapus/dorman". Jangan
 * disamakan dengan entity di paket {@code temp} (`AisFlagsData`/`DetailperkuliahanTemp`/
 * `NilaiTemp`) yang TERKONFIRMASI dorman total (nol pemanggil) — paket ini kebalikannya.</p>
 *
 * <p>Karena murni cache turunan, kolom {@link #getIp()}/{@link #getIpk()}/
 * {@link #getSksCurrent()}/{@link #getSksTotal()} bisa MENYIMPANG dari perhitungan nyata bila
 * proses batch yang mengisi ulang baris ini belum/gagal berjalan setelah ada perubahan nilai —
 * entity ini sendiri tidak punya mekanisme apa pun untuk mendeteksi/menandai data basi.</p>
 *
 * @see Mahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "temporary", name = "ipk_mahasiswa")
public class IPKMahasiswa extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** Field audit shadow standar AIS — nama pengguna yang terakhir menulis baris ini (biasanya proses batch, bukan interaksi UI langsung). Dideklarasikan ulang di sini karena induk {@code GeneralValueObject} bukan {@code @MappedSuperclass}. */
	private String oleh;
	/** Field audit shadow standar AIS — ID login pengguna yang terakhir menulis baris ini. */
	private String olehId;
	/** Lihat {@link #olehId}. */
	public String getOlehId() {return olehId;}
	/** Lihat {@link #olehId}. Setter menolak nilai kosong/blank secara diam-diam agar jejak lama tidak tertimpa nilai kosong. */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/** Lihat {@link #getOleh()}. Setter menolak nilai kosong/blank secara diam-diam agar jejak lama tidak tertimpa nilai kosong. */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** Lihat {@link #oleh}. */
	public String getOleh() {
		return oleh;
	}

	/** Callback siklus hidup JPA — menstempel {@link #getTanggal_dirubah()} otomatis sebelum setiap {@code UPDATE}, lewat {@code AuditTimestampInterceptor.ubah(this)}. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/** Field audit shadow standar AIS — stempel waktu perubahan terakhir baris ini. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Lihat {@link #tanggal_dirubah}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** Lihat {@link #tanggal_dirubah}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Representasi teks baris ini — nama mahasiswa (lihat {@link #getNama()}); TIDAK menyertakan semester/tahun akademik meski baris ini bersifat per-periode, jadi dua baris IPK mahasiswa yang sama pada semester berbeda tampil identik di log/debug apa adanya. */
	public String toString() {
		return nama;
	}

	/** Nama tampilan mahasiswa pemilik baris — SALINAN teks bebas (bukan diturunkan dari {@link #getMahasiswa()} saat dibaca), bisa menyimpang dari nama mahasiswa terkini bila nama diubah belakangan tanpa proses batch ini dijalankan ulang. */
	private String nama;
	/** Catatan bebas opsional pada baris snapshot ini. */
	private String keterangan;
	/** Mahasiswa pemilik snapshot IPK ini. */
	private Mahasiswa mahasiswa;
	/** SKS yang sedang ditempuh pada semester snapshot ini diambil (beban studi semester berjalan, bukan kumulatif). */
	private Integer sksCurrent;
	/** Total SKS kumulatif yang sudah ditempuh mahasiswa sampai dengan semester snapshot ini. */
	private Integer sksTotal;
	/** Indeks Prestasi (IP) SATU semester ini saja (bukan kumulatif) — lihat {@link #getIpk()} untuk versi kumulatif. */
	private Double ip;
	/** Nomor urut semester snapshot ini diambil. */
	private Integer semester;
	/** Indeks Prestasi Kumulatif (IPK) sampai dengan semester ini — berbeda dari {@link #getIp()} yang hanya IP semester berjalan. */
	private Double ipk;
	/** Tahun akademik snapshot ini diambil, format teks bebas (bukan FK ke entity tahun akademik master). */
	private String tahunAkademik;


	/** Mahasiswa pemilik snapshot ini. Fetch {@code SELECT} eksplisit (bukan {@code JOIN} default Hibernate) — query terpisah dipicu tiap baris diakses, relevan untuk performa saat menampilkan banyak baris sekaligus di dasbor. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/** Lihat {@link #getMahasiswa()}. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** Lihat {@link #sksCurrent}. */
	@Column(name = "sks_current")
	public Integer getSksCurrent() {
		return sksCurrent;
	}

	/** Lihat {@link #sksCurrent}. */
	public void setSksCurrent(Integer sksCurrent) {
		this.sksCurrent = sksCurrent;
	}

	/** Lihat {@link #sksTotal}. */
	@Column(name = "sks_total")
	public Integer getSksTotal() {
		return sksTotal;
	}

	/** Lihat {@link #sksTotal}. */
	public void setSksTotal(Integer sksTotal) {
		this.sksTotal = sksTotal;
	}

	/** Lihat {@link #ip}. */
	@Column(name = "ip")
	public Double getIp() {
		return ip;
	}

	/** Lihat {@link #ip}. */
	public void setIp(Double ip) {
		this.ip = ip;
	}

	/** Lihat {@link #semester}. */
	@Column(name = "semester")
	public Integer getSemester() {
		return semester;
	}

	/** Lihat {@link #semester}. */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/** Lihat {@link #ipk}. */
	@Column(name = "ipk")
	public Double getIpk() {
		return ipk;
	}

	/** Lihat {@link #ipk}. */
	public void setIpk(Double ipk) {
		this.ipk = ipk;
	}

	/** Konstruktor bawaan JPA/Hibernate — field diisi lewat setter setelah instansiasi. */
	public IPKMahasiswa() {
	}

	/** Primary key auto-increment; lihat {@code @GeneratedValue(strategy = IDENTITY)}. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** Lihat {@link #getId()}. Hanya dipakai Hibernate saat memuat baris (kolom {@code insertable = false}). */
	public void setId(Long id) {
		this.id = id;
	}

	/** Lihat {@link #nama}. Dipangkas spasi tepi (`trim()`) saat dibaca — TIDAK ditulis balik ke field, jadi ini BUKAN getter destruktif seperti pola yang berulang di banyak entity lain. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** Lihat {@link #nama}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Lihat {@link #keterangan}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** Lihat {@link #keterangan}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Lihat {@link #tahunAkademik}. */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/** Lihat {@link #tahunAkademik}. */
	@Column(name="tahun_akademik")
	public String getTahunAkademik() {
		return tahunAkademik;
	}

}
