package ais.database.model.epsbed;

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
import ais.database.model.Jurusan;



/**
 * Entity JPA/Hibernate untuk tabel {@code epsbed.fasilitas_akademik_jurusan}: mencatat fasilitas
 * akademik penunjang satu program studi ({@link Jurusan}) untuk pelaporan EPSBED — luas dan jumlah
 * ruang kuliah, ruang laboratorium, ruang dosen tetap, ruang administrasi, luas lahan/kebun percobaan
 * prodi, serta koleksi buku (jumlah judul dan eksemplar) milik prodi tersebut. Dipakai oleh
 * {@code ais.action.master.epsbed.TransaksiFasilitasPenunjangAkademik} dan {@code ais.action.master.JurusanAction}.
 *
 * <p><b>Bukan relasi dengan {@code library.Perpustakaan}:</b> koleksi buku pada entity ini
 * ({@link #jumlahJudulBuku}, {@link #jumlahEksemplarBuku}) adalah kolom numerik biasa, bukan relasi
 * ke entity {@code ais.database.model.library.Perpustakaan} — tidak ada join/foreign key ke modul
 * perpustakaan sama sekali; keduanya adalah dua sumber data yang independen. Angka pada entity ini
 * merepresentasikan koleksi buku tingkat prodi (kemungkinan ruang baca/pojok baca jurusan) untuk
 * keperluan pelaporan EPSBED, sedangkan luas ruang perpustakaan tingkat perguruan tinggi dicatat
 * terpisah pada {@code PerguruanTinggi#getLuasTotalRuangPerpustakaan()} (di luar paket ini) dan modul
 * {@code library.Perpustakaan} mengelola sirkulasi/katalog perpustakaan pusat sebagai domain
 * tersendiri. Tidak ditemukan mekanisme sinkronisasi otomatis antara ketiganya di kode ini.</p>
 *
 * <p><b>Field audit shadow:</b> {@code oleh}/{@code olehId} (pencatat perubahan) dan
 * {@code tanggal_dirubah} (di-refresh oleh {@link #onUpdate()} melalui
 * {@code ais.database.hibernate.AuditTimestampInterceptor} pada setiap {@code @PreUpdate}) adalah
 * kolom audit yang ditulis oleh infrastruktur persistence, konsisten dengan entity lain sekeluarga
 * di paket {@code ais.database.model.epsbed}. Kelas mewarisi perilaku umum, validasi, dan lifecycle
 * dari {@link GeneralValueObject}; anotasi {@code @Audited} membuat setiap perubahan baris ini turut
 * tercatat oleh Hibernate Envers.</p>
 *
 * @see GeneralValueObject
 * @see Jurusan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "epsbed", name = "fasilitas_akademik_jurusan")



public class FasilitasAkademikJurusan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris fasilitas akademik jurusan; dibangkitkan otomatis oleh database (identity). */
	private Long id;
	/** Nama pencatat perubahan terakhir; kolom audit yang diisi oleh lapisan pemanggil, bukan Hibernate. */
	private String oleh;
	/** Id pencatat perubahan terakhir; pasangan dari {@link #oleh}. */
	private String olehId;
	/**
	 * Mengembalikan id pencatat perubahan terakhir baris ini.
	 *
	 * @return id pencatat, atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Menetapkan id pencatat perubahan terakhir. Nilai kosong/blank diabaikan sehingga id pencatat
	 * yang sudah tersimpan tidak pernah ditimpa nilai kosong.
	 *
	 * @param olehId id pencatat baru; diabaikan jika {@code null} atau string kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menetapkan nama pencatat perubahan terakhir. Nilai kosong/blank diabaikan, simetris dengan
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pencatat baru; diabaikan jika {@code null} atau string kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pencatat perubahan terakhir baris ini.
	 *
	 * @return nama pencatat, atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback lifecycle JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence sebelum
	 * baris ini di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir secara eksplisit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini, diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap update.
	 *
	 * @return cap waktu perubahan terakhir, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string baris ini, dipakai widget UI (mis. combobox/listbox) yang menampilkan
	 * entity lewat {@code toString()}.
	 *
	 * @return nilai field {@link #nama} apa adanya (tidak di-trim); bisa {@code null} bila belum
	 *         pernah diset.
	 */
	public String toString() {
		return nama;
	}

	/** Nama tampilan baris ini. Berbeda dari {@code KapasitasMahasiswaBaru}, tidak ada turunan otomatis dari {@link Jurusan} di sini. */
	private String nama;
	/** Program studi/jurusan yang fasilitas akademiknya dicatat oleh baris ini. */
	private Jurusan jurusan;
	/** Luas kebun/lahan percobaan milik prodi ini (dalam meter persegi), relevan untuk prodi rumpun pertanian/peternakan. */
	private Double luasKebunLahanPercobaanProdi;
	/** Luas total ruang kuliah milik/dipakai prodi ini (dalam meter persegi). */
	private Double luasRuangKuliah;
	/** Jumlah ruang kuliah milik/dipakai prodi ini. */
	private Integer jumlahRuangKuliah;
	/** Luas total ruang laboratorium milik/dipakai prodi ini (dalam meter persegi). */
	private Double luasRuangLaboratorium;
	/** Jumlah ruang laboratorium milik/dipakai prodi ini. */
	private Integer jumlahRuangLaboratorium;
	/** Luas total ruang dosen tetap prodi ini (dalam meter persegi). */
	private Double luasTotalRuangDosenTetap;
	/** Luas total ruang administrasi prodi ini (dalam meter persegi). */
	private Double luasTotalRuangAdministrasi;
	/** Jumlah judul buku pada koleksi tingkat prodi ini; tidak berelasi dengan modul {@code library.Perpustakaan}, lihat javadoc class. */
	private Integer jumlahJudulBuku;
	/** Jumlah eksemplar (fisik) buku pada koleksi tingkat prodi ini; lihat javadoc class untuk catatan relasi library. */
	private Integer jumlahEksemplarBuku;

	/** Konstruktor default yang dibutuhkan Hibernate/JPA untuk instansiasi entity via refleksi. */
	public FasilitasAkademikJurusan() {
	}

	/**
	 * Konstruktor kenyamanan untuk membuat baris fasilitas akademik baru yang langsung terkait ke
	 * satu prodi.
	 *
	 * @param jurusan program studi yang fasilitas akademiknya akan dicatat.
	 */
	public FasilitasAkademikJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah dipersist.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key baris ini. Kolom dipetakan {@code insertable = false} sehingga nilai ini
	 * normalnya diisi oleh Hibernate dari identity generator database.
	 *
	 * @param id nilai id baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama tampilan baris ini.
	 *
	 * @return nilai {@link #nama} yang di-trim; {@code null} bila belum pernah diset.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama tampilan baris ini.
	 *
	 * @param nama nama tampilan baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan program studi/jurusan yang fasilitas akademiknya dicatat baris ini.
	 *
	 * @return relasi {@link Jurusan}, di-cascade {@code PERSIST}/{@code MERGE} sehingga entity
	 *         jurusan transient/detached yang diberikan via {@link #setJurusan(Jurusan)} ikut
	 *         dipersist/di-merge bersama baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menetapkan program studi/jurusan yang fasilitas akademiknya dicatat baris ini.
	 *
	 * @param jurusan relasi jurusan baru.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan luas total ruang kuliah prodi ini.
	 *
	 * @return luas ruang kuliah (meter persegi), bisa {@code null} bila belum diisi.
	 */
	@Column(name="luas_ruang_kuliah")
	public Double getLuasRuangKuliah() {
		return luasRuangKuliah;
	}

	/**
	 * Menetapkan luas total ruang kuliah prodi ini.
	 *
	 * @param luasRuangKuliah luas baru (meter persegi).
	 */
	public void setLuasRuangKuliah(Double luasRuangKuliah) {
		this.luasRuangKuliah = luasRuangKuliah;
	}

	/**
	 * Mengembalikan jumlah ruang kuliah prodi ini.
	 *
	 * @return jumlah ruang kuliah, bisa {@code null} bila belum diisi.
	 */
	@Column(name="jumlah_ruang_kuliah")
	public Integer getJumlahRuangKuliah() {
		return jumlahRuangKuliah;
	}

	/**
	 * Menetapkan jumlah ruang kuliah prodi ini.
	 *
	 * @param jumlahRuangKuliah jumlah baru.
	 */
	public void setJumlahRuangKuliah(Integer jumlahRuangKuliah) {
		this.jumlahRuangKuliah = jumlahRuangKuliah;
	}

	/**
	 * Mengembalikan luas total ruang laboratorium prodi ini.
	 *
	 * @return luas ruang laboratorium (meter persegi), bisa {@code null} bila belum diisi.
	 */
	@Column(name="luas_ruang_laboratorium")
	public Double getLuasRuangLaboratorium() {
		return luasRuangLaboratorium;
	}

	/**
	 * Menetapkan luas total ruang laboratorium prodi ini.
	 *
	 * @param luasRuangLaboratorium luas baru (meter persegi).
	 */
	public void setLuasRuangLaboratorium(Double luasRuangLaboratorium) {
		this.luasRuangLaboratorium = luasRuangLaboratorium;
	}
	/**
	 * Mengembalikan jumlah ruang laboratorium prodi ini.
	 *
	 * @return jumlah ruang laboratorium, bisa {@code null} bila belum diisi.
	 */
	@Column(name="jumlah_ruang_laboratorium")
	public Integer getJumlahRuangLaboratorium() {
		return jumlahRuangLaboratorium;
	}

	/**
	 * Menetapkan jumlah ruang laboratorium prodi ini.
	 *
	 * @param jumlahRuangLaboratorium jumlah baru.
	 */
	public void setJumlahRuangLaboratorium(Integer jumlahRuangLaboratorium) {
		this.jumlahRuangLaboratorium = jumlahRuangLaboratorium;
	}
	/**
	 * Mengembalikan luas total ruang dosen tetap prodi ini.
	 *
	 * @return luas ruang dosen tetap (meter persegi), bisa {@code null} bila belum diisi.
	 */
	@Column(name="luas_total_ruang_dosen_tetap")
	public Double getLuasTotalRuangDosenTetap() {
		return luasTotalRuangDosenTetap;
	}

	/**
	 * Menetapkan luas total ruang dosen tetap prodi ini.
	 *
	 * @param luasTotalRuangDosenTetap luas baru (meter persegi).
	 */
	public void setLuasTotalRuangDosenTetap(Double luasTotalRuangDosenTetap) {
		this.luasTotalRuangDosenTetap = luasTotalRuangDosenTetap;
	}
	/**
	 * Mengembalikan luas total ruang administrasi prodi ini.
	 *
	 * @return luas ruang administrasi (meter persegi), bisa {@code null} bila belum diisi.
	 */
	@Column(name="luas_total_ruang_administrasi")
	public Double getLuasTotalRuangAdministrasi() {
		return luasTotalRuangAdministrasi;
	}

	/**
	 * Menetapkan luas total ruang administrasi prodi ini.
	 *
	 * @param luasTotalRuangAdministrasi luas baru (meter persegi).
	 */
	public void setLuasTotalRuangAdministrasi(Double luasTotalRuangAdministrasi) {
		this.luasTotalRuangAdministrasi = luasTotalRuangAdministrasi;
	}
	/**
	 * Mengembalikan jumlah judul buku pada koleksi tingkat prodi ini.
	 *
	 * @return jumlah judul buku, bisa {@code null} bila belum diisi.
	 */
	@Column(name="jumlah_judul_buku")
	public Integer getJumlahJudulBuku() {
		return jumlahJudulBuku;
	}

	/**
	 * Menetapkan jumlah judul buku pada koleksi tingkat prodi ini.
	 *
	 * @param jumlahJudulBuku jumlah baru.
	 */
	public void setJumlahJudulBuku(Integer jumlahJudulBuku) {
		this.jumlahJudulBuku = jumlahJudulBuku;
	}
	/**
	 * Mengembalikan jumlah eksemplar (fisik) buku pada koleksi tingkat prodi ini.
	 *
	 * @return jumlah eksemplar buku, bisa {@code null} bila belum diisi.
	 */
	@Column(name="jumlah_eksemplar_buku")
	public Integer getJumlahEksemplarBuku() {
		return jumlahEksemplarBuku;
	}

	/**
	 * Menetapkan jumlah eksemplar (fisik) buku pada koleksi tingkat prodi ini.
	 *
	 * @param jumlahEksemplarBuku jumlah baru.
	 */
	public void setJumlahEksemplarBuku(Integer jumlahEksemplarBuku) {
		this.jumlahEksemplarBuku = jumlahEksemplarBuku;
	}

	/**
	 * Menetapkan luas kebun/lahan percobaan milik prodi ini.
	 *
	 * @param luasKebunLahanPercobaanProdi luas baru (meter persegi).
	 */
	public void setLuasKebunLahanPercobaanProdi(
			Double luasKebunLahanPercobaanProdi) {
		this.luasKebunLahanPercobaanProdi = luasKebunLahanPercobaanProdi;
	}
	/**
	 * Mengembalikan luas kebun/lahan percobaan milik prodi ini.
	 *
	 * @return luas kebun/lahan percobaan (meter persegi), bisa {@code null} bila belum diisi.
	 */
	@Column(name="luas_kebun_lahan_percobaan_prodi")
	public Double getLuasKebunLahanPercobaanProdi() {
		return luasKebunLahanPercobaanProdi;
	}


}
