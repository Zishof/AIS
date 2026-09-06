package ais.database.model;

// Generated Apr 12, 2010 1:48:52 AM by Hibernate Tools 3.2.4.CR1

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
 * Entity Hibernate/JPA untuk tabel {@code public.jenis_kegiatan_prasyarat} — aturan
 * <b>prasyarat pelunasan</b> antar {@link JenisKegiatan}: satu baris menyatakan bahwa {@link
 * #getJenisKegiatan()} mensyaratkan {@link #getJenisKegiatanPrasyarat()} (dan opsional dua
 * prasyarat tambahan, {@link #getJenisKegiatanPrasyarat2()}/{@link
 * #getJenisKegiatanPrasyarat3()}) sudah lunas — sebagian ({@link #getProsentaseLunas()}) atau
 * penuh — sejumlah semester tertentu ({@link #getJumlahSemesterHarusLunas()}) sebelum kegiatan
 * yang disyaratkan boleh diproses/ditagihkan.
 *
 * <p>Cakupan aturan ini dapat dipersempit lewat filter Fakultas/Jurusan/Program/Tahun Angkatan/
 * rentang semester ({@link #getMinSmt()}/{@link #getMaxSmt()}), dan/atau dibatasi mulai berlaku
 * pada tahun akademik/semester tertentu ({@link #getTahunAkademikMulai()}/{@link
 * #getJenisSemesterMulai()}, digabung jadi ID pembanding oleh {@link #getTa()} — formula yang
 * sama persis dengan {@link SettingBiaya#getTa()}). Dipakai antara lain oleh {@code
 * DaftarUlangMahasiswaLamaAction} dan {@code TagihanMahasiswa} (servlet API) untuk menegakkan
 * gerbang prasyarat pelunasan sebelum daftar ulang/penagihan baru.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_kegiatan_prasyarat")
public class JenisKegiatanPrasyarat extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = -3088613612931036389L;
	/** Primary key baris {@code jenis_kegiatan_prasyarat}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Keterangan bebas untuk baris aturan prasyarat ini. */
	private String keterangan;

	/**
	 * @return keterangan bebas baris ini; boleh {@code null}.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk baris ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Flag aktif aturan prasyarat ini, kolom implisit {@code aktif}; {@code null} diperlakukan sebagai aktif dan ditulis balik permanen (lihat {@link #getAktif()}). */
	private Boolean aktif;
	/** Flag "periksa juga semester yang sama" untuk pengecekan pelunasan; lihat {@link #getCheckJugaSmtYgSama()}. */
	private Boolean checkJugaSmtYgSama;
	/** Jenis kegiatan yang DISYARATKAN (yang baru boleh diproses setelah prasyarat lunas). */
	private JenisKegiatan jenisKegiatan;
	/** Jenis kegiatan prasyarat utama (wajib) yang harus lunas terlebih dahulu. */
	private JenisKegiatan jenisKegiatanPrasyarat;
	/** Jenis kegiatan prasyarat tambahan ke-2 (opsional). */
	private JenisKegiatan jenisKegiatanPrasyarat2;
	/** Jenis kegiatan prasyarat tambahan ke-3 (opsional). */
	private JenisKegiatan jenisKegiatanPrasyarat3;
	/** Filter Fakultas (Institusi): bila diisi, aturan hanya berlaku untuk mahasiswa di fakultas ini. */
	private Fakultas fakultas;
	/** Filter Jurusan (Prodi): bila diisi, aturan hanya berlaku untuk mahasiswa di jurusan ini. */
	private Jurusan jurusan;
	/** Filter Program (string bebas); lihat {@link #getProgram()}. */
	private String program;
	/** Tahun akademik acuan aturan ini (makna berbeda dari {@link #tahunAkademikMulai}, lihat masing-masing getter). */
	private String tahunAkademik;
	/** Jenis semester acuan aturan ini (bandingkan dengan {@link #jenisSemesterMulai}). */
	private String jenisSemester;

	/** Tahun akademik mulai berlakunya aturan ini; digabung dengan {@link #jenisSemesterMulai} oleh {@link #getTa()}. */
	private String tahunAkademikMulai;
	/** Jenis semester mulai berlakunya aturan ini; digabung dengan {@link #tahunAkademikMulai} oleh {@link #getTa()}. */
	private String jenisSemesterMulai;

	/** Jumlah semester yang wajib sudah lunas (atas prasyarat) sebelum kegiatan disyaratkan boleh diproses. */
	private Integer jumlahSemesterHarusLunas;
	/** Persentase minimum pelunasan prasyarat yang disyaratkan (0-100); default 99.0, lihat {@link #getProsentaseLunas()}. */
	private Double prosentaseLunas;
	/** Filter tahun angkatan mahasiswa (string bebas); lihat {@link #getTahunAngkatan()}. */
	private String tahunAngkatan;

	/** Batas bawah semester cakupan aturan ini; default 0. */
	private Integer minSmt = 0;
	/** Batas atas semester cakupan aturan ini; default 30. */
	private Integer maxSmt = 30;
	/** ID tahun-ajaran+semester gabungan, diturunkan oleh {@link #getTa()}. */
	private Integer ta;

	/**
	 * Representasi ringkas untuk log/debug: {@code "<id>-<jenisKegiatan>-<jenisKegiatanPrasyarat>"}.
	 *
	 * @return string ringkas identitas baris ini
	 */
	public String toString() {
		return id + "-" + jenisKegiatan + "-" + jenisKegiatanPrasyarat;
	}

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public JenisKegiatanPrasyarat() {
	}

	/**
	 * Konstruktor dengan ID langsung — berguna untuk membuat referensi ringan (proxy manual)
	 * tanpa memuat seluruh baris dari database, mis. untuk dipakai sebagai FK pada entity lain.
	 *
	 * @param id primary key yang sudah diketahui
	 */
	public JenisKegiatanPrasyarat(Long id) {
		this.id = id;
	}

	/**
	 * @return primary key baris {@code jenis_kegiatan_prasyarat}; {@code null} sebelum baris
	 *         di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Status aktif aturan prasyarat ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code true} pada pembacaan pertama — bukan sekadar
	 * fallback sesaat seperti kebanyakan getter default lain di kelas ini.</p>
	 *
	 * @return status aktif; {@code true} bila belum diisi (dan setelahnya tersimpan permanen
	 *         sebagai {@code true}).
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return batas bawah semester cakupan aturan ini; {@code 0} bila belum diisi.
	 */
	public Integer getMinSmt() {
		return minSmt == null ? 0 : minSmt;
	}

	/**
	 * @param minSmt batas bawah semester baru.
	 */
	public void setMinSmt(Integer minSmt) {
		this.minSmt = minSmt;
	}

	/**
	 * Batas atas semester cakupan aturan ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 30} pada pembacaan pertama.</p>
	 *
	 * @return batas atas semester; {@code 30} bila belum diisi (dan setelahnya tersimpan
	 *         permanen sebagai {@code 30}).
	 */
	public Integer getMaxSmt() {
		if (maxSmt == null) {
			maxSmt = 30;
		}
		return maxSmt;
	}

	/**
	 * @param maxSmt batas atas semester baru.
	 */
	public void setMaxSmt(Integer maxSmt) {
		this.maxSmt = maxSmt;
	}

	/**
	 * @return jenis kegiatan yang DISYARATKAN oleh aturan ini (yang baru boleh diproses setelah
	 *         prasyarat lunas); tidak memakai {@code check()} untuk resolusi proxy lazy (berbeda
	 *         dari sejumlah entity lain di cluster ini yang konsisten memakainya).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan", nullable = false)
	public JenisKegiatan getJenisKegiatan() {
		return jenisKegiatan;
	}

	/**
	 * @param jenisKegiatan jenis kegiatan yang disyaratkan, baru.
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * @return jenis kegiatan prasyarat UTAMA (wajib) yang harus lunas terlebih dahulu; kolomnya
	 *         {@code nullable = false} sehingga secara skema selalu terisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan_menjadi_prasyarat", nullable = false)
	public JenisKegiatan getJenisKegiatanPrasyarat() {
		return jenisKegiatanPrasyarat;
	}

	/**
	 * @param jenisKegiatanPrasyarat jenis kegiatan prasyarat utama baru.
	 */
	public void setJenisKegiatanPrasyarat(JenisKegiatan jenisKegiatanPrasyarat) {
		this.jenisKegiatanPrasyarat = jenisKegiatanPrasyarat;
	}

	/**
	 * @return tahun akademik acuan aturan ini; boleh {@code null}. Perhatikan bedanya dengan
	 *         {@link #getTahunAkademikMulai()}: field ini tidak dipakai oleh {@link #getTa()}.
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * @param tahunAkademik tahun akademik acuan baru.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * @return jenis semester acuan aturan ini; boleh {@code null}. Perhatikan bedanya dengan
	 *         {@link #getJenisSemesterMulai()}: field ini tidak dipakai oleh {@link #getTa()}.
	 */
	public String getJenisSemester() {
		return jenisSemester;
	}

	/**
	 * @param jenisSemester jenis semester acuan baru.
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * @return jumlah semester yang wajib sudah lunas (atas prasyarat) sebelum kegiatan yang
	 *         disyaratkan boleh diproses; {@code 0} bila belum diisi.
	 */
	public Integer getJumlahSemesterHarusLunas() {
		return jumlahSemesterHarusLunas == null ? 0 : jumlahSemesterHarusLunas;
	}

	/**
	 * @param jumlahSemesterHarusLunas jumlah semester wajib lunas, baru.
	 */
	public void setJumlahSemesterHarusLunas(Integer jumlahSemesterHarusLunas) {
		this.jumlahSemesterHarusLunas = jumlahSemesterHarusLunas;
	}

	/**
	 * @return persentase minimum pelunasan prasyarat yang disyaratkan (0-100); default {@code
	 *         99.0} bila belum diisi.
	 */
	public Double getProsentaseLunas() {
		return prosentaseLunas == null ? 99.0 : prosentaseLunas;
	}

	/**
	 * @param prosentaseLunas persentase minimum pelunasan baru.
	 */
	public void setProsentaseLunas(Double prosentaseLunas) {
		this.prosentaseLunas = prosentaseLunas;
	}

	/**
	 * @return filter Fakultas (Institusi) aturan ini; {@code null} berarti tidak difilter
	 *         berdasarkan fakultas. Tidak memakai {@code check()} untuk resolusi proxy lazy.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

	/**
	 * @param fakultas filter fakultas baru; {@code null} untuk menghapus filter.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return filter Jurusan (Prodi) aturan ini; {@code null} berarti tidak difilter
	 *         berdasarkan jurusan. Tidak memakai {@code check()} untuk resolusi proxy lazy.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * @param jurusan filter jurusan baru; {@code null} untuk menghapus filter.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return filter Program aturan ini, di-{@code trim()}; {@code null} bila kosong/hanya
	 *         spasi ATAU belum diisi (tidak difilter berdasarkan program).
	 */
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	/**
	 * @param program filter program baru.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * @return filter tahun angkatan mahasiswa, di-{@code trim()}; {@code null} bila kosong/hanya
	 *         spasi ATAU belum diisi (tidak difilter berdasarkan angkatan).
	 */
	public String getTahunAngkatan() {
		return tahunAngkatan == null || tahunAngkatan.trim().isEmpty() ? null : tahunAngkatan.trim();
	}

	/**
	 * @param tahunAngkatan filter tahun angkatan baru.
	 */
	public void setTahunAngkatan(String tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * @return {@code true} bila pengecekan pelunasan prasyarat juga harus memeriksa semester
	 *         yang sama (bukan hanya semester-semester sebelumnya); default {@code false} bila
	 *         belum diisi.
	 */
	public Boolean getCheckJugaSmtYgSama() {
		return checkJugaSmtYgSama == null ? false : checkJugaSmtYgSama;
	}

	/**
	 * @param checkJugaSmtYgSama nilai flag baru.
	 */
	public void setCheckJugaSmtYgSama(Boolean checkJugaSmtYgSama) {
		this.checkJugaSmtYgSama = checkJugaSmtYgSama;
	}

	/**
	 * @return jenis kegiatan prasyarat tambahan ke-2 (opsional); {@code null} bila hanya satu
	 *         prasyarat yang berlaku. Tidak memakai {@code check()} untuk resolusi proxy lazy.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan_menjadi_prasyarat_2", nullable = true)
	public JenisKegiatan getJenisKegiatanPrasyarat2() {
		return jenisKegiatanPrasyarat2;
	}

	/**
	 * @param jenisKegiatanPrasyarat2 jenis kegiatan prasyarat ke-2 baru; {@code null} untuk
	 *                                melepas tautan.
	 */
	public void setJenisKegiatanPrasyarat2(JenisKegiatan jenisKegiatanPrasyarat2) {
		this.jenisKegiatanPrasyarat2 = jenisKegiatanPrasyarat2;
	}

	/**
	 * @return jenis kegiatan prasyarat tambahan ke-3 (opsional); {@code null} bila tidak
	 *         berlaku. Tidak memakai {@code check()} untuk resolusi proxy lazy.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan_menjadi_prasyarat_3", nullable = true)
	public JenisKegiatan getJenisKegiatanPrasyarat3() {
		return jenisKegiatanPrasyarat3;
	}

	/**
	 * @param jenisKegiatanPrasyarat3 jenis kegiatan prasyarat ke-3 baru; {@code null} untuk
	 *                                melepas tautan.
	 */
	public void setJenisKegiatanPrasyarat3(JenisKegiatan jenisKegiatanPrasyarat3) {
		this.jenisKegiatanPrasyarat3 = jenisKegiatanPrasyarat3;
	}

	/**
	 * @return tahun akademik mulai berlakunya aturan ini; boleh {@code null}. Dipakai bersama
	 *         {@link #getJenisSemesterMulai()} oleh {@link #getTa()}.
	 */
	public String getTahunAkademikMulai() {
		return tahunAkademikMulai;
	}

	/**
	 * @param tahunAkademikMulai tahun akademik mulai baru.
	 */
	public void setTahunAkademikMulai(String tahunAkademikMulai) {
		this.tahunAkademikMulai = tahunAkademikMulai;
	}

	/**
	 * @return jenis semester mulai berlakunya aturan ini; boleh {@code null}. Dipakai bersama
	 *         {@link #getTahunAkademikMulai()} oleh {@link #getTa()}.
	 */
	public String getJenisSemesterMulai() {
		return jenisSemesterMulai;
	}

	/**
	 * @param jenisSemesterMulai jenis semester mulai baru.
	 */
	public void setJenisSemesterMulai(String jenisSemesterMulai) {
		this.jenisSemesterMulai = jenisSemesterMulai;
	}

	/**
	 * Menurunkan ID tahun-ajaran+semester gabungan dari {@link #getTahunAkademikMulai()} dan
	 * {@link #getJenisSemesterMulai()} — formula dan seluruh perilakunya (termasuk potensi
	 * nilai basi bila parsing gagal, karena {@link #ta} adalah field instance bukan variabel
	 * lokal) identik dengan {@link SettingBiaya#getTa()}; lihat javadoc method itu untuk
	 * penjelasan lengkap.
	 *
	 * @return ID tahun-ajaran+semester gabungan; {@code 0} bila tidak dapat diturunkan sama
	 *         sekali.
	 * @see SettingBiaya#getTa()
	 */
	public Integer getTa() {
		String id_smt = (getTahunAkademikMulai() == null || getTahunAkademikMulai().trim().isEmpty() ? "0"
				: getTahunAkademikMulai().split("/")[0])
				+ (getJenisSemesterMulai() == null || getJenisSemesterMulai().trim().isEmpty() ? "0"
						: getJenisSemesterMulai().equals(Perkuliahan.GENAP) ? "2" : "1");
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/JenisKegiatanPrasyarat.java:301");

		}
		if (ta == null) {
			ta = 0;
		}
		return ta;
	}

	/**
	 * @param ta ID tahun-ajaran+semester gabungan; biasanya tidak perlu diset manual karena
	 *           diturunkan otomatis oleh {@link #getTa()}.
	 */
	public void setTa(Integer ta) {
		this.ta = ta;
	}
}
