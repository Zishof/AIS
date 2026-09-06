package ais.database.model;

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

import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.Siswa;

/**
 * <h2>SiswaDapatKelompokPkl &mdash; Keanggotaan Siswa dalam Kelompok PKL (Praktik Kerja Lapangan)</h2>
 *
 * <p><b>Untuk apa (bahasa sederhana):</b> menyimpan catatan bahwa seorang <b>siswa</b> tergabung ke
 * dalam sebuah <b>kelompok PKL</b>. Satu baris tabel ini berarti "siswa X menjadi anggota kelompok
 * PKL Y", lengkap dengan keterangan, hasil/laporan, serta nilai akhir PKL-nya. Entitas ini adalah
 * pasangan sekolah dari {@link MahasiswaDapatKelompokPkl} (yang khusus untuk mahasiswa): keduanya
 * menunjuk ke {@link KelompokPkl} yang sama sehingga <b>mesin PKL yang sudah ada dipakai ulang</b>
 * (tanpa menggandakan model) &mdash; hanya jenis pesertanya yang berbeda (siswa, bukan mahasiswa).
 * Sebuah {@code KelompokPkl} dianggap "PKL untuk siswa" ketika field sekolahnya terisi (lihat
 * {@code KelompokPkl.getSekolah()}); pada kelompok seperti itulah baris {@code SiswaDapatKelompokPkl}
 * dibuat.</p>
 *
 * <h3>Relasi &amp; kolom</h3>
 * <ul>
 *   <li>{@link #getKelompokPkl()} &rarr; kolom {@code kelompok_pkl} (wajib): kelompok PKL yang diikuti.</li>
 *   <li>{@link #getSiswa()} &rarr; kolom {@code siswa} (wajib): siswa yang menjadi anggota.</li>
 *   <li>{@link #getKeterangan()}, {@link #getHasil()}: catatan bebas &amp; ringkasan hasil/laporan.</li>
 *   <li>{@link #getTotalNilai()}, {@link #getNilaiHuruf()}, {@link #getLulus()}: nilai akhir PKL siswa.</li>
 *   <li>{@link #getDiterima()}: penanda siswa sudah resmi diterima/ditempatkan di kelompok tersebut.</li>
 *   <li>{@link #getDetailNilai()}: rincian nilai per komponen (format teks) untuk kebutuhan penilaian.</li>
 * </ul>
 *
 * <h3>Integrasi e-learning</h3>
 * <p>Entitas ini mengimplementasikan {@link VOPesertaPembelajaran} sehingga dapat diperlakukan
 * seragam oleh mesin pembelajaran/e-learning: {@link #ambilVOPembelajaran()} mengembalikan
 * {@link KelompokPkl} sebagai objek pembelajaran (sama seperti perkuliahan atau jadwal pelajaran).
 * Dengan begitu, kelompok PKL milik seorang siswa dapat ditampilkan sebagai "kelas" di e-learning
 * siswa &mdash; persis seperti PKL pada e-learning mahasiswa &mdash; tanpa membuat alur baru.</p>
 *
 * <h3>Skema basis data</h3>
 * <p>Dipetakan ke tabel {@code public.siswa_dapat_kelompok_pkl}. Karena aplikasi memakai
 * {@code hbm2ddl=update}, tabel ini dibuat otomatis saat pertama kali dijalankan (tidak perlu SQL
 * manual). Entitas diaudit ({@link Audited}) dan memakai {@code dynamicInsert/dynamicUpdate} agar
 * hanya kolom yang berubah yang ikut disimpan &mdash; hemat operasi basis data. Kolom bertipe teks
 * panjang ({@code hasil}, {@code detailNilai}) memakai {@code columnDefinition = "text"} agar tidak
 * terbatas 255 karakter.</p>
 *
 * <h3>Audit &amp; keamanan</h3>
 * <p>Field {@code oleh}/{@code olehId} mencatat siapa yang terakhir menyimpan (setter mengabaikan
 * nilai kosong agar jejak lama tidak tertimpa nilai hampa), dan {@code tanggal_dirubah} diperbarui
 * otomatis lewat {@code AuditTimestampInterceptor} pada {@code @PreUpdate}. Seluruh relasi memakai
 * pemuatan malas ({@code FetchType.LAZY}) dan pembungkus {@code check(...)} dari
 * {@link GeneralValueObject} agar aman terhadap proxy Hibernate yang belum terinisialisasi.</p>
 *
 * @author eCampus
 * @see MahasiswaDapatKelompokPkl
 * @see KelompokPkl
 * @see VOPesertaPembelajaran
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "siswa_dapat_kelompok_pkl")
public class SiswaDapatKelompokPkl extends GeneralValueObject implements VOPesertaPembelajaran {

	private static final long serialVersionUID = 2463821577548439810L;

	/** Primary key baris kepesertaan, dihasilkan basis data (strategi {@code IDENTITY}). */
	private Long id;
	/** Jejak "siapa yang menulis baris ini", diisi otomatis lewat {@link #setOleh(String)}. */
	private String oleh;
	/** Pendamping {@link #oleh}. Lihat {@link #getOlehId()}. */
	private String olehId;

	/** Kelompok PKL yang diikuti siswa. Kolom FK wajib {@code kelompok_pkl}. */
	private KelompokPkl kelompokPkl;
	/** Siswa anggota kelompok PKL. Kolom FK wajib {@code siswa}. */
	private Siswa siswa;
	/** Catatan bebas kepesertaan. */
	private String keterangan;
	/** Ringkasan hasil/laporan PKL siswa. Lihat {@link #getHasil()}. */
	private String hasil;
	/** Total nilai akhir PKL. Lihat {@link #getTotalNilai()}. */
	private Double totalNilai;
	/** Nilai huruf akhir PKL. Lihat {@link #getNilaiHuruf()}. */
	private String nilaiHuruf;
	/** Status kelulusan PKL. Lihat {@link #getLulus()}. */
	private Boolean lulus;
	/** Penanda siswa sudah resmi diterima/ditempatkan di kelompok. Lihat {@link #getDiterima()}. */
	private Boolean diterima;
	/** Rincian nilai per komponen (teks bebas). Lihat {@link #getDetailNilai()}. */
	private String detailNilai = "";

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SiswaDapatKelompokPkl() {
	}

	/**
	 * Mengembalikan primary key baris kepesertaan.
	 *
	 * <p>Perhatikan {@code insertable = false}: nilai id sepenuhnya dihasilkan basis data
	 * (strategi {@code IDENTITY}), sehingga memanggil {@link #setId(Long)} sebelum menyimpan
	 * tidak memaksakan id tertentu.</p>
	 *
	 * @return id baris; {@code null} selama objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris kepesertaan. Hanya relevan bagi Hibernate saat mengisi objek
	 * dari hasil query, karena kolom dipetakan {@code insertable = false} (lihat {@link #getId()}).
	 *
	 * @param id nilai primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan rantai jejak pemanggil yang tersimpan bersama {@link #oleh}, diisi otomatis
	 * oleh {@code AuditTimestampInterceptor}.
	 *
	 * @return jejak pemanggil; boleh {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel jejak pemanggil {@link #getOlehId()}. Setter ini <b>mengabaikan diam-diam</b>
	 * nilai {@code null}/kosong sehingga jejak lama tidak dapat ditimpa nilai hampa.
	 *
	 * @param olehId jejak pemanggil baru; {@code null}/kosong diabaikan tanpa peringatan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel identitas pelaku penyimpanan baris ini. Nilai {@code null}/kosong diabaikan
	 * diam-diam agar jejak lama tidak tertimpa nilai hampa.
	 *
	 * @param oleh nama/identitas pelaku; {@code null}/kosong diabaikan tanpa peringatan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan identitas pelaku yang terakhir menyimpan baris ini.
	 *
	 * @return nama/identitas pelaku; boleh {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * <i>Callback</i> JPA {@link javax.persistence.PreUpdate} yang memperbarui
	 * {@link #getTanggal_dirubah()}/{@link #getOleh()}/{@link #getOlehId()} lewat
	 * {@code AuditTimestampInterceptor} pada setiap UPDATE baris.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu terakhir baris diubah, diinisialisasi ke waktu server saat objek dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya diurus otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi TIMESTAMP).
	 *
	 * @return waktu terakhir baris disimpan/diubah
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris, berupa {@link #getKelompokPkl()} digabung {@link #getSiswa()}.
	 * <b>Method ini bermutasi</b> field {@link #kelompokPkl} dan {@link #siswa} (memanggil ulang
	 * getter masing-masing yang menormalkan proxy Hibernate).
	 */
	public String toString() {
		kelompokPkl = getKelompokPkl();
		siswa = getSiswa();
		return kelompokPkl + "-" + siswa;
	}

	/**
	 * Menyetel kelompok PKL yang diikuti.
	 *
	 * @param kelompokPkl kelompok PKL baru
	 */
	public void setKelompokPkl(KelompokPkl kelompokPkl) {
		this.kelompokPkl = kelompokPkl;
	}

	/**
	 * Mengembalikan kelompok PKL yang diikuti siswa ini. Relasi wajib ({@code nullable = false}),
	 * diambil malas ({@code FetchType.LAZY}) dan dinormalkan lewat {@code check(...)} dari
	 * {@link GeneralValueObject} agar aman terhadap proxy Hibernate yang belum terinisialisasi.
	 *
	 * @return kelompok PKL terkait; tidak seharusnya {@code null} pada baris yang valid
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_pkl", nullable = false)
	public KelompokPkl getKelompokPkl() {
		kelompokPkl = check(kelompokPkl);
		return kelompokPkl;
	}

	/**
	 * Menyetel siswa anggota kelompok PKL.
	 *
	 * @param siswa siswa baru
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan siswa anggota kelompok PKL. Relasi wajib ({@code nullable = false}), diambil
	 * malas ({@code FetchType.LAZY}).
	 *
	 * @return siswa terkait; tidak seharusnya {@code null} pada baris yang valid
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Mengembalikan catatan bebas kepesertaan apa adanya (tanpa <i>trim</i>).
	 *
	 * @return keterangan; boleh {@code null}
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas kepesertaan.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan ringkasan hasil/laporan PKL siswa, dinormalisasi ke string kosong (bukan
	 * {@code null}) bila belum diisi, dengan spasi tepi dipangkas.
	 *
	 * @return hasil/laporan yang sudah di-<i>trim</i>; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getHasil() {
		return hasil == null ? "" : hasil.trim();
	}

	/**
	 * Menyetel ringkasan hasil/laporan PKL.
	 *
	 * @param hasil hasil/laporan baru; boleh {@code null}
	 */
	public void setHasil(String hasil) {
		this.hasil = hasil;
	}

	/**
	 * Mengembalikan total nilai akhir PKL siswa, dinormalisasi ke {@code 0.0} bila belum diisi.
	 *
	 * @return total nilai; tidak pernah {@code null}
	 */
	public Double getTotalNilai() {
		return totalNilai == null ? 0.0 : totalNilai;
	}

	/**
	 * Menyetel total nilai akhir PKL.
	 *
	 * @param totalNilai total nilai baru; {@code null} berarti kembali ke bawaan ({@code 0.0})
	 */
	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	/**
	 * Mengembalikan nilai huruf akhir PKL, dengan spasi tepi dipangkas. Dipakai
	 * {@link #getLulus()} untuk menyimpulkan status kelulusan bila belum ditetapkan eksplisit.
	 *
	 * @return nilai huruf yang sudah di-<i>trim</i>; {@code null} bila belum diisi
	 */
	public String getNilaiHuruf() {
		return this.nilaiHuruf == null ? null : this.nilaiHuruf.trim();
	}

	/**
	 * Menyetel nilai huruf akhir PKL.
	 *
	 * @param nilaiHuruf nilai huruf baru; boleh {@code null}
	 */
	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	/**
	 * Status kelulusan PKL siswa. Bila belum ditetapkan eksplisit, disimpulkan dari nilai huruf:
	 * huruf kosong / mengandung D, E, atau T dianggap belum lulus; selain itu lulus. Meniru logika
	 * {@link MahasiswaDapatKelompokPkl#getLulus()} agar konsisten antar jenjang.
	 */
	public Boolean getLulus() {
		nilaiHuruf = getNilaiHuruf();
		if (lulus == null && nilaiHuruf != null) {
			if (nilaiHuruf.isEmpty() || nilaiHuruf.toUpperCase().contains("D") || nilaiHuruf.toUpperCase().contains("E")
					|| nilaiHuruf.toUpperCase().contains("T")) {
				lulus = false;
			} else {
				lulus = true;
			}
		} else if (lulus == null) {
			lulus = true;
		}
		if (nilaiHuruf == null) {
			lulus = false;
		}
		return lulus;
	}

	/**
	 * Menyetel status kelulusan PKL secara eksplisit, mem-<i>bypass</i> penyimpulan otomatis dari
	 * {@link #getNilaiHuruf()}.
	 *
	 * @param lulus nilai eksplisit baru; {@code null} mengembalikan getter ke logika penyimpulan
	 */
	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	/**
	 * Mengembalikan penanda siswa sudah resmi diterima/ditempatkan di kelompok PKL. Default
	 * {@code false} bila belum disetel eksplisit.
	 *
	 * @return {@code true} bila sudah diterima
	 */
	public Boolean getDiterima() {
		return diterima == null ? false : diterima;
	}

	/**
	 * Menyetel penanda penerimaan/penempatan siswa di kelompok.
	 *
	 * @param diterima nilai baru; {@code null} berarti kembali ke bawaan ({@code false})
	 */
	public void setDiterima(Boolean diterima) {
		this.diterima = diterima;
	}

	/**
	 * Mengembalikan rincian nilai per komponen (format teks bebas), dinormalisasi ke string
	 * kosong (bukan {@code null}) bila belum diisi, dengan spasi tepi dipangkas.
	 *
	 * @return rincian nilai yang sudah di-<i>trim</i>; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getDetailNilai() {
		return detailNilai == null ? "" : detailNilai.trim();
	}

	/**
	 * Menyetel rincian nilai per komponen.
	 *
	 * @param detailNilai rincian nilai baru; boleh {@code null}
	 */
	public void setDetailNilai(String detailNilai) {
		this.detailNilai = detailNilai;
	}

	/**
	 * Mengembalikan {@link KelompokPkl} sebagai objek pembelajaran agar kelompok PKL siswa dapat
	 * diperlakukan seragam oleh mesin e-learning (sama seperti perkuliahan / jadwal pelajaran).
	 */
	@Override
	public VOPembelajaran ambilVOPembelajaran() {
		return getKelompokPkl();
	}
}
