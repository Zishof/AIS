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

	private Long id;
	private String oleh;
	private String olehId;

	private KelompokPkl kelompokPkl;
	private Siswa siswa;
	private String keterangan;
	private String hasil;
	private Double totalNilai;
	private String nilaiHuruf;
	private Boolean lulus;
	private Boolean diterima;
	private String detailNilai = "";

	public SiswaDapatKelompokPkl() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		kelompokPkl = getKelompokPkl();
		siswa = getSiswa();
		return kelompokPkl + "-" + siswa;
	}

	public void setKelompokPkl(KelompokPkl kelompokPkl) {
		this.kelompokPkl = kelompokPkl;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_pkl", nullable = false)
	public KelompokPkl getKelompokPkl() {
		kelompokPkl = check(kelompokPkl);
		return kelompokPkl;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(columnDefinition = "text")
	public String getHasil() {
		return hasil == null ? "" : hasil.trim();
	}

	public void setHasil(String hasil) {
		this.hasil = hasil;
	}

	public Double getTotalNilai() {
		return totalNilai == null ? 0.0 : totalNilai;
	}

	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	public String getNilaiHuruf() {
		return this.nilaiHuruf == null ? null : this.nilaiHuruf.trim();
	}

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

	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	public Boolean getDiterima() {
		return diterima == null ? false : diterima;
	}

	public void setDiterima(Boolean diterima) {
		this.diterima = diterima;
	}

	@Column(columnDefinition = "text")
	public String getDetailNilai() {
		return detailNilai == null ? "" : detailNilai.trim();
	}

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
