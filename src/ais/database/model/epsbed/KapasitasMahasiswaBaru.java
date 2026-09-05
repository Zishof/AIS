package ais.database.model.epsbed;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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




import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;



import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;

/**
 * Entity JPA/Hibernate untuk tabel {@code epsbed.kapasitas_mahasiswa_baru}: mencatat kapasitas daya
 * tampung mahasiswa baru satu program studi ({@link Jurusan}) untuk satu tahun akademik/semester,
 * sesuai format pelaporan EPSBED (Evaluasi Program Studi Berbasis Evaluasi Diri) ke Dikti/Kemdikbud —
 * pendahulu format feeder/PDDIKTI yang lebih baru, masih dipakai perguruan tinggi yang belum/tidak
 * bermigrasi penuh serta oleh laporan akreditasi internal (lihat pemakai di {@code ais.action.master.epsbed},
 * {@code ais.action.master.pmb}, {@code ais.action.master.sapto}, dan {@code ais.action.report.std9}).
 * Satu baris merepresentasikan satu kombinasi prodi-tahun akademik, memuat target dan realisasi jumlah
 * mahasiswa baru (pendaftar, lulus seleksi, daftar ulang, mundur, pindahan), rentang tanggal serta jumlah
 * minggu perkuliahan untuk semester ganjil dan genap, metode penyelenggaraan hari kuliah, dan data
 * semester pendek (SP) opsional.
 *
 * <p><b>Turunan otomatis (fallback saat pembacaan):</b> beberapa getter mengisi nilai default —
 * bahkan mengubah state field di memori sebagai efek samping pembacaan — bila nilai belum pernah
 * diset secara eksplisit: {@link #getNama()} menyusun ulang nama dari {@link Jurusan#getNama()} +
 * tahun akademik, {@link #getTahunAkademik()} jatuh ke tahun akademik berjalan, {@link #getGanjilGenap()}
 * jatuh ke semester berjalan, dan {@link #getTahun()} mem-parse tahun dari tahun akademik dengan fallback
 * ke tahun kalender saat ini bila parsing gagal. Lihat javadoc masing-masing method untuk rincian.</p>
 *
 * <p><b>Field audit shadow:</b> {@code oleh}/{@code olehId} (pencatat perubahan) dan
 * {@code tanggal_dirubah} (di-refresh oleh {@link #onUpdate()} melalui
 * {@code ais.database.hibernate.AuditTimestampInterceptor} pada setiap {@code @PreUpdate}) adalah
 * kolom audit yang ditulis oleh infrastruktur persistence, bukan bug — pola ini konsisten dengan
 * entity lain sekeluarga di paket {@code ais.database.model.epsbed}. Kelas mewarisi perilaku umum,
 * validasi, dan lifecycle dari {@link GeneralValueObject}; anotasi {@code @Audited} membuat setiap
 * perubahan baris ini turut tercatat oleh Hibernate Envers.</p>
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
@Table(schema = "epsbed", name = "kapasitas_mahasiswa_baru")
public class KapasitasMahasiswaBaru extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris kapasitas mahasiswa baru; dibangkitkan otomatis oleh database (identity). */
	private Long id;
	/** Nama/identitas pencatat perubahan terakhir; kolom audit yang diisi oleh lapisan pemanggil, bukan Hibernate. */
	private String oleh;
	/** Id pencatat perubahan terakhir; pasangan dari {@link #oleh}, kolom audit yang diisi oleh lapisan pemanggil. */
	private String olehId;

	/**
	 * Mengembalikan id pencatat perubahan terakhir baris ini.
	 *
	 * @return id pencatat ({@link #olehId}), atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pencatat perubahan terakhir. Nilai kosong/blank diabaikan (metode langsung
	 * {@code return} tanpa mengubah state) sehingga id pencatat yang sudah tersimpan tidak pernah
	 * ditimpa oleh nilai kosong yang tidak sengaja terkirim dari lapisan pemanggil.
	 *
	 * @param olehId id pencatat baru; diabaikan jika {@code null} atau string kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pencatat perubahan terakhir. Nilai kosong/blank diabaikan (metode langsung
	 * {@code return} tanpa mengubah state), simetris dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pencatat baru; diabaikan jika {@code null} atau string kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pencatat perubahan terakhir baris ini.
	 *
	 * @return nama pencatat ({@link #oleh}), atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback lifecycle JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat
	 * sebelum baris ini di-{@code UPDATE} ke database, lalu mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} ke waktu saat ini. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir secara eksplisit. Biasanya nilai ini diperbarui otomatis
	 * oleh {@link #onUpdate()}; setter ini dipakai bila lapisan pemanggil perlu menimpa nilainya
	 * secara manual (mis. saat migrasi/backfill data).
	 *
	 * @param tanggal_dirubah cap waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini. Diinisialisasi ke waktu pembuatan objek
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) dan diperbarui otomatis oleh {@link #onUpdate()} pada
	 * setiap update.
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
	 * @return nilai field {@link #nama} (bukan {@link #getNama()} — tidak memicu turunan otomatis
	 *         dari {@link Jurusan}; bisa {@code null} jika nama belum pernah dihitung/diset).
	 */
	public String toString() {
		return nama;
	}

	/** Nama tampilan baris ini; lihat {@link #getNama()} untuk aturan turunan otomatisnya. */
	private String nama;
	/** Program studi/jurusan yang kapasitasnya dicatat oleh baris ini. */
	private Jurusan jurusan;
	/** Tahun akademik (format {@code "YYYY/YYYY"}); lihat {@link #getTahunAkademik()} untuk fallback-nya. */
	private String tahunAkademik;
	/** Tahun kalender hasil parsing {@link #tahunAkademik}; lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Kode semester ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}); lihat {@link #getGanjilGenap()}. */
	private String ganjilGenap;
	/** Target jumlah mahasiswa baru yang hendak diterima prodi ini pada tahun akademik bersangkutan. */
	private Integer jumlahTargetMahasiswaBaru;
	/** Jumlah calon mahasiswa yang mendaftar (mengikuti seleksi) pada prodi/tahun akademik ini. */
	private Integer jumlahPendaftar;
	/** Jumlah pendaftar yang dinyatakan lulus seleksi penerimaan. */
	private Integer jumlahLulus;
	/** Jumlah mahasiswa baru yang benar-benar melakukan daftar ulang (registrasi) setelah lulus seleksi. */
	private Integer jumlahDaftarUlang;
	/** Jumlah mahasiswa baru yang mengundurkan diri sebelum/sesudah daftar ulang. */
	private Integer jumlahMundur;
	/** Jumlah mahasiswa yang diterima melalui jalur pindahan (bukan seleksi mahasiswa baru reguler). */
	private Integer jumlahPindahan;
	/** Angkatan ke berapa kelompok mahasiswa baru ini bagi prodi bersangkutan. */
	private Integer angkatanKe;
	/** Tanggal mulai perkuliahan semester ganjil untuk tahun akademik ini. */
	private Date awalPerkuliahanGanjil;
	/** Tanggal berakhir perkuliahan semester ganjil untuk tahun akademik ini. */
	private Date akhirPerkuliahanGanjil;
	/** Jumlah minggu efektif perkuliahan semester ganjil. */
	private Integer jumlahMingguKuliahGanjil;
	/** Tanggal mulai perkuliahan semester genap untuk tahun akademik ini. */
	private Date awalPerkuliahanGenap;
	/** Tanggal berakhir perkuliahan semester genap untuk tahun akademik ini. */
	private Date akhirPerkuliahanGenap;
	/** Jumlah minggu efektif perkuliahan semester genap. */
	private Integer jumlahMingguKuliahGenap;
	/** Deskripsi metode/pola hari perkuliahan reguler (mis. kode hari kuliah dalam seminggu). */
	private String metodeHariPerkuliahan;
	/** Deskripsi metode/pola hari perkuliahan untuk kelas ekstensi, terpisah dari kelas reguler. */
	private String metodeHariPerkuliahanEkstensi;
	/** Penanda (kode ya/tidak) apakah prodi ini menyelenggarakan semester pendek (SP). */
	private String adaSP;
	/** Jumlah semester pendek (SP) yang diselenggarakan, bila {@link #adaSP} menyatakan ada. */
	private Integer jumlahSP;
	/** Deskripsi metode pelaksanaan semester pendek (SP), mis. tatap muka/daring/kombinasi. */
	private String metodePelaksanaanSP;

	/** Konstruktor default yang dibutuhkan Hibernate/JPA untuk instansiasi entity via refleksi. */
	public KapasitasMahasiswaBaru() {
	}

	/**
	 * Konstruktor kenyamanan untuk membuat baris kapasitas baru yang langsung terkait ke satu prodi.
	 *
	 * @param jurusan program studi yang kapasitasnya akan dicatat.
	 */
	public KapasitasMahasiswaBaru(Jurusan jurusan) {
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
	 * normalnya diisi oleh Hibernate dari identity generator database, bukan oleh kode aplikasi.
	 *
	 * @param id nilai id baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama tampilan baris ini, dengan turunan otomatis sebagai efek samping pembacaan:
	 * bila relasi {@link #jurusan} sudah diisi, field {@link #nama} ditulis ulang (dimutasi in-place,
	 * bukan sekadar dibaca) menjadi gabungan {@code Jurusan.getNama() + "-" + getTahunAkademik()}
	 * sebelum dikembalikan — sehingga nama selalu mengikuti nama jurusan dan tahun akademik terkini
	 * meski field {@code nama} tersimpan di database sudah berbeda (nilai lama akan tertimpa pada
	 * pembacaan berikutnya bila entity ini kemudian di-persist ulang). Ini adalah pola "getter
	 * destruktif" yang sudah dikenal berulang pada model {@code hbm2java} lawas di paket ini: memanggil
	 * getter murni untuk keperluan baca saja tetap dapat mengubah state yang akan ikut tersimpan pada
	 * flush/commit Hibernate berikutnya, karena dirty-checking Hibernate mendeteksi field yang berubah
	 * meskipun perubahan itu terjadi di dalam getter. Bila {@link #jurusan} bernilai {@code null}
	 * (belum diset atau lazy-load belum terjadi), nilai {@link #nama} yang sudah tersimpan dipakai apa
	 * adanya. Hasil akhirnya selalu di-trim sebelum dikembalikan; string kosong setelah trim tetap
	 * dikembalikan sebagai string kosong (bukan {@code null}), sedangkan nilai {@code nama} yang belum
	 * pernah diset sama sekali (dan tanpa relasi jurusan) mengembalikan {@code null}.
	 *
	 * @return nama tampilan baris ini: {@code "<nama jurusan>-<tahun akademik>"} bila relasi jurusan
	 *         tersedia, atau nilai {@link #nama} tersimpan (di-trim) sebagai fallback.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (jurusan != null) {
			nama = jurusan.getNama() + "-" + getTahunAkademik();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama tampilan baris ini secara langsung. Nilai ini dapat ditimpa kembali pada
	 * pembacaan berikutnya oleh {@link #getNama()} selama relasi {@link #jurusan} masih terisi.
	 *
	 * @param nama nama tampilan baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan program studi/jurusan yang kapasitasnya dicatat baris ini.
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
	 * Menetapkan program studi/jurusan yang kapasitasnya dicatat baris ini. Nilai ini turut
	 * memengaruhi hasil {@link #getNama()} pada pembacaan berikutnya.
	 *
	 * @param jurusan relasi jurusan baru.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan tahun akademik baris ini (format umum {@code "YYYY/YYYY"}), dengan fallback
	 * otomatis: bila {@link #tahunAkademik} belum pernah diset ({@code null}), field ini diisi
	 * (dimutasi sebagai efek samping pembacaan, sama seperti {@link #getNama()}) dengan hasil
	 * {@link Common#getCurrentTahunAkademik()} — tahun akademik berjalan menurut kalender akademik
	 * aplikasi saat ini — sebelum dikembalikan. Sekali nilai ini terisi (baik lewat fallback maupun
	 * lewat {@link #setTahunAkademik(String)}), pemanggilan berikutnya selalu mengembalikan nilai yang
	 * sudah tersimpan tanpa menghitung ulang, sehingga baris yang dibuat lebih awal tidak diam-diam
	 * mengikuti pergantian tahun akademik berjalan pada pembacaan-pembacaan setelahnya. Nilai ini juga
	 * dipakai oleh {@link #getNama()} untuk menyusun nama tampilan dan oleh {@link #getTahun()} sebagai
	 * sumber parsing tahun kalender.
	 *
	 * @return tahun akademik baris ini; tidak pernah {@code null} setelah pemanggilan pertama karena
	 *         fallback ke tahun akademik berjalan.
	 */
	@Column(name = "tahun_akademik")
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik baris ini secara eksplisit, menonaktifkan fallback otomatis pada
	 * {@link #getTahunAkademik()} untuk pemanggilan berikutnya.
	 *
	 * @param tahunAkademik tahun akademik baru (format {@code "YYYY/YYYY"}).
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan kode semester (ganjil/genap) baris ini, dengan fallback otomatis: bila
	 * {@link #ganjilGenap} belum pernah diset ({@code null}), field ini diisi (dimutasi sebagai efek
	 * samping pembacaan) berdasarkan semester berjalan menurut {@link Common#isNowSemensterGanjil()} —
	 * {@link Perkuliahan#GANJIL} bila saat ini semester ganjil, {@link Perkuliahan#GENAP} bila
	 * sebaliknya — sebelum dikembalikan. Sama seperti {@link #getTahunAkademik()}, sekali terisi nilai
	 * ini tidak dihitung ulang pada pemanggilan berikutnya, sehingga baris yang dibuat pada semester
	 * ganjil tidak diam-diam berubah label saat aplikasi dipakai lagi pada semester genap.
	 *
	 * @return kode semester baris ini; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	@Column(name = "ganjil_genap")
	public String getGanjilGenap() {
		if (ganjilGenap == null) {
			ganjilGenap = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return ganjilGenap;
	}

	/**
	 * Menetapkan kode semester baris ini secara eksplisit, menonaktifkan fallback otomatis pada
	 * {@link #getGanjilGenap()} untuk pemanggilan berikutnya.
	 *
	 * @param ganjilGenap kode semester baru, umumnya {@link Perkuliahan#GANJIL} atau
	 *                    {@link Perkuliahan#GENAP}.
	 */
	public void setGanjilGenap(String ganjilGenap) {
		this.ganjilGenap = ganjilGenap;
	}

	/**
	 * Mengembalikan target jumlah mahasiswa baru yang hendak diterima prodi ini.
	 *
	 * @return target jumlah mahasiswa baru; {@code 0} bila belum pernah diset (tidak pernah
	 *         {@code null}).
	 */
	@Column(name = "jumlah_target_mahasiswa_baru")
	public Integer getJumlahTargetMahasiswaBaru() {
		return jumlahTargetMahasiswaBaru == null ? 0 : jumlahTargetMahasiswaBaru;
	}

	/**
	 * Menetapkan target jumlah mahasiswa baru prodi ini.
	 *
	 * @param jumlahTargetMahasiswaBaru target baru; {@code null} diperbolehkan dan akan dibaca
	 *                                  sebagai {@code 0} oleh {@link #getJumlahTargetMahasiswaBaru()}.
	 */
	public void setJumlahTargetMahasiswaBaru(Integer jumlahTargetMahasiswaBaru) {
		this.jumlahTargetMahasiswaBaru = jumlahTargetMahasiswaBaru;
	}

	/**
	 * Mengembalikan jumlah calon mahasiswa yang mendaftar (mengikuti seleksi) pada prodi/tahun
	 * akademik ini. Berbeda dari sebagian besar counter lain di kelas ini, getter ini tidak menerapkan
	 * fallback ke {@code 0} — nilai {@code null} dikembalikan apa adanya bila belum pernah diset.
	 *
	 * @return jumlah pendaftar, bisa {@code null}.
	 */
	@Column(name = "jumlah_pendaftar")
	public Integer getJumlahPendaftar() {
		return jumlahPendaftar;
	}

	/**
	 * Menetapkan jumlah calon mahasiswa yang mendaftar pada prodi/tahun akademik ini.
	 *
	 * @param jumlahPendaftar jumlah pendaftar baru.
	 */
	public void setJumlahPendaftar(Integer jumlahPendaftar) {
		this.jumlahPendaftar = jumlahPendaftar;
	}

	/**
	 * Mengembalikan jumlah pendaftar yang dinyatakan lulus seleksi penerimaan.
	 *
	 * @return jumlah lulus seleksi; {@code 0} bila belum pernah diset (tidak pernah {@code null}).
	 */
	@Column(name = "jumlah_lulus")
	public Integer getJumlahLulus() {
		if (jumlahLulus == null) {
			jumlahLulus = 0;
		}
		return jumlahLulus;
	}

	/**
	 * Menetapkan jumlah pendaftar yang dinyatakan lulus seleksi penerimaan.
	 *
	 * @param jumlahLulus jumlah lulus baru.
	 */
	public void setJumlahLulus(Integer jumlahLulus) {
		this.jumlahLulus = jumlahLulus;
	}

	/**
	 * Mengembalikan jumlah mahasiswa baru yang benar-benar melakukan daftar ulang (registrasi)
	 * setelah lulus seleksi.
	 *
	 * @return jumlah daftar ulang; {@code 0} bila belum pernah diset (tidak pernah {@code null}).
	 */
	@Column(name = "jumlah_daftar_ulang")
	public Integer getJumlahDaftarUlang() {
		if (jumlahDaftarUlang == null) {
			jumlahDaftarUlang = 0;
		}
		return jumlahDaftarUlang;
	}

	/**
	 * Menetapkan jumlah mahasiswa baru yang melakukan daftar ulang.
	 *
	 * @param jumlahDaftarUlang jumlah daftar ulang baru.
	 */
	public void setJumlahDaftarUlang(Integer jumlahDaftarUlang) {
		this.jumlahDaftarUlang = jumlahDaftarUlang;
	}

	/**
	 * Mengembalikan jumlah mahasiswa baru yang mengundurkan diri.
	 *
	 * @return jumlah mundur; {@code 0} bila belum pernah diset (tidak pernah {@code null}).
	 */
	@Column(name = "jumlah_mundur")
	public Integer getJumlahMundur() {
		if (jumlahMundur == null) {
			jumlahMundur = 0;
		}
		return jumlahMundur;
	}

	/**
	 * Menetapkan jumlah mahasiswa baru yang mengundurkan diri.
	 *
	 * @param jumlahMundur jumlah mundur baru.
	 */
	public void setJumlahMundur(Integer jumlahMundur) {
		this.jumlahMundur = jumlahMundur;
	}

	/**
	 * Mengembalikan jumlah mahasiswa yang diterima melalui jalur pindahan.
	 *
	 * @return jumlah pindahan; {@code 0} bila belum pernah diset (tidak pernah {@code null}).
	 */
	@Column(name = "jumlah_pindahan")
	public Integer getJumlahPindahan() {
		if (jumlahPindahan == null) {
			jumlahPindahan = 0;
		}
		return jumlahPindahan;
	}

	/**
	 * Menetapkan jumlah mahasiswa yang diterima melalui jalur pindahan.
	 *
	 * @param jumlahPindahan jumlah pindahan baru.
	 */
	public void setJumlahPindahan(Integer jumlahPindahan) {
		this.jumlahPindahan = jumlahPindahan;
	}

	/**
	 * Mengembalikan tanggal mulai perkuliahan semester ganjil pada tahun akademik ini.
	 *
	 * @return tanggal awal perkuliahan ganjil, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "awal_perkuliahan_ganjil")
	public Date getAwalPerkuliahanGanjil() {

		return awalPerkuliahanGanjil;
	}

	/**
	 * Menetapkan tanggal mulai perkuliahan semester ganjil.
	 *
	 * @param awalPerkuliahanGanjil tanggal awal baru.
	 */
	public void setAwalPerkuliahanGanjil(Date awalPerkuliahanGanjil) {
		this.awalPerkuliahanGanjil = awalPerkuliahanGanjil;
	}

	/**
	 * Mengembalikan tanggal berakhir perkuliahan semester ganjil pada tahun akademik ini.
	 *
	 * @return tanggal akhir perkuliahan ganjil, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "akhir_perkuliahan_ganjil")
	public Date getAkhirPerkuliahanGanjil() {
		return akhirPerkuliahanGanjil;
	}

	/**
	 * Menetapkan tanggal berakhir perkuliahan semester ganjil.
	 *
	 * @param akhirPerkuliahanGanjil tanggal akhir baru.
	 */
	public void setAkhirPerkuliahanGanjil(Date akhirPerkuliahanGanjil) {
		this.akhirPerkuliahanGanjil = akhirPerkuliahanGanjil;
	}

	/**
	 * Mengembalikan jumlah minggu efektif perkuliahan semester ganjil.
	 *
	 * @return jumlah minggu kuliah ganjil; {@code 0} bila belum pernah diset (tidak pernah
	 *         {@code null}).
	 */
	@Column(name = "jumlah_minggu_ganjil")
	public Integer getJumlahMingguKuliahGanjil() {
		if (jumlahMingguKuliahGanjil == null) {
			jumlahMingguKuliahGanjil = 0;
		}
		return jumlahMingguKuliahGanjil;
	}

	/**
	 * Menetapkan jumlah minggu efektif perkuliahan semester ganjil.
	 *
	 * @param jumlahMingguKuliahGanjil jumlah minggu baru.
	 */
	public void setJumlahMingguKuliahGanjil(Integer jumlahMingguKuliahGanjil) {
		this.jumlahMingguKuliahGanjil = jumlahMingguKuliahGanjil;
	}

	/**
	 * Mengembalikan tanggal mulai perkuliahan semester genap pada tahun akademik ini.
	 *
	 * @return tanggal awal perkuliahan genap, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "awal_perkuliahan_genap")
	public Date getAwalPerkuliahanGenap() {
		return awalPerkuliahanGenap;
	}

	/**
	 * Menetapkan tanggal mulai perkuliahan semester genap.
	 *
	 * @param awalPerkuliahanGenap tanggal awal baru.
	 */
	public void setAwalPerkuliahanGenap(Date awalPerkuliahanGenap) {
		this.awalPerkuliahanGenap = awalPerkuliahanGenap;
	}

	/**
	 * Mengembalikan tanggal berakhir perkuliahan semester genap pada tahun akademik ini.
	 *
	 * @return tanggal akhir perkuliahan genap, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "akhir_perkuliahan_genap")
	public Date getAkhirPerkuliahanGenap() {
		return akhirPerkuliahanGenap;
	}

	/**
	 * Menetapkan tanggal berakhir perkuliahan semester genap.
	 *
	 * @param akhirPerkuliahanGenap tanggal akhir baru.
	 */
	public void setAkhirPerkuliahanGenap(Date akhirPerkuliahanGenap) {
		this.akhirPerkuliahanGenap = akhirPerkuliahanGenap;
	}

	/**
	 * Mengembalikan jumlah minggu efektif perkuliahan semester genap. Berbeda dari
	 * {@link #getJumlahMingguKuliahGanjil()}, getter ini tidak menerapkan fallback ke {@code 0} —
	 * nilai {@code null} dikembalikan apa adanya bila belum pernah diset.
	 *
	 * @return jumlah minggu kuliah genap, bisa {@code null}.
	 */
	@Column(name = "jumlah_minggu_genap")
	public Integer getJumlahMingguKuliahGenap() {
		return jumlahMingguKuliahGenap;
	}

	/**
	 * Menetapkan jumlah minggu efektif perkuliahan semester genap.
	 *
	 * @param jumlahMingguKuliahGenap jumlah minggu baru.
	 */
	public void setJumlahMingguKuliahGenap(Integer jumlahMingguKuliahGenap) {
		this.jumlahMingguKuliahGenap = jumlahMingguKuliahGenap;
	}

	/**
	 * Mengembalikan deskripsi metode/pola hari perkuliahan reguler.
	 *
	 * @return metode hari perkuliahan, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "metode_hari_perkuliahan")
	public String getMetodeHariPerkuliahan() {
		return metodeHariPerkuliahan;
	}

	/**
	 * Menetapkan deskripsi metode/pola hari perkuliahan reguler.
	 *
	 * @param metodeHariPerkuliahan metode baru.
	 */
	public void setMetodeHariPerkuliahan(String metodeHariPerkuliahan) {
		this.metodeHariPerkuliahan = metodeHariPerkuliahan;
	}

	/**
	 * Mengembalikan deskripsi metode/pola hari perkuliahan untuk kelas ekstensi.
	 *
	 * @return metode hari perkuliahan ekstensi, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "metode_hari_perkuliahan_ekstensi")
	public String getMetodeHariPerkuliahanEkstensi() {
		return metodeHariPerkuliahanEkstensi;
	}

	/**
	 * Menetapkan deskripsi metode/pola hari perkuliahan untuk kelas ekstensi.
	 *
	 * @param metodeHariPerkuliahanEkstensi metode baru.
	 */
	public void setMetodeHariPerkuliahanEkstensi(String metodeHariPerkuliahanEkstensi) {
		this.metodeHariPerkuliahanEkstensi = metodeHariPerkuliahanEkstensi;
	}

	/**
	 * Mengembalikan penanda apakah prodi ini menyelenggarakan semester pendek (SP).
	 *
	 * @return kode ada/tidaknya SP, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "ada_sp")
	public String getAdaSP() {
		return adaSP;
	}

	/**
	 * Menetapkan penanda apakah prodi ini menyelenggarakan semester pendek (SP).
	 *
	 * @param adaSP kode baru.
	 */
	public void setAdaSP(String adaSP) {
		this.adaSP = adaSP;
	}

	/**
	 * Mengembalikan jumlah semester pendek (SP) yang diselenggarakan.
	 *
	 * @return jumlah SP; {@code 0} bila belum pernah diset (tidak pernah {@code null}).
	 */
	@Column(name = "jumlah_sp")
	public Integer getJumlahSP() {
		if (jumlahSP == null) {
			jumlahSP = 0;
		}
		return jumlahSP;
	}

	/**
	 * Menetapkan jumlah semester pendek (SP) yang diselenggarakan.
	 *
	 * @param jumlahSP jumlah SP baru.
	 */
	public void setJumlahSP(Integer jumlahSP) {
		this.jumlahSP = jumlahSP;
	}

	/**
	 * Mengembalikan deskripsi metode pelaksanaan semester pendek (SP).
	 *
	 * @return metode pelaksanaan SP, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "metode_pelaksanaan_sp")
	public String getMetodePelaksanaanSP() {
		return metodePelaksanaanSP;
	}

	/**
	 * Menetapkan deskripsi metode pelaksanaan semester pendek (SP).
	 *
	 * @param metodePelaksanaanSP metode baru.
	 */
	public void setMetodePelaksanaanSP(String metodePelaksanaanSP) {
		this.metodePelaksanaanSP = metodePelaksanaanSP;
	}

	/**
	 * Mengembalikan angkatan ke berapa kelompok mahasiswa baru ini bagi prodi bersangkutan. Tidak
	 * dipetakan sebagai kolom JPA ({@code @Column}) — nilai ini murni state in-memory, tidak
	 * dipersist ke tabel {@code kapasitas_mahasiswa_baru}.
	 *
	 * @return angkatan ke-, bisa {@code null} bila belum diisi.
	 */
	public Integer getAngkatanKe() {
		return angkatanKe;
	}

	/**
	 * Menetapkan angkatan ke berapa kelompok mahasiswa baru ini. Lihat catatan di
	 * {@link #getAngkatanKe()} soal field ini yang tidak dipersist.
	 *
	 * @param angkatanKe angkatan ke- baru.
	 */
	public void setAngkatanKe(Integer angkatanKe) {
		this.angkatanKe = angkatanKe;
	}

	/**
	 * Mengembalikan tahun kalender (mis. {@code 2026}) hasil parsing dari {@link #getTahunAkademik()},
	 * dengan dua lapis fallback: pertama, field {@link #tahun} selalu dihitung ulang (dimutasi sebagai
	 * efek samping pembacaan, sama seperti {@link #getNama()}/{@link #getTahunAkademik()}/
	 * {@link #getGanjilGenap()}) dengan mem-parse token pertama sebelum {@code "/"} pada string tahun
	 * akademik — mis. {@code "2026/2027"} menghasilkan {@code 2026} — menggunakan
	 * {@link Integer#parseInt(String)} setelah di-{@code trim}; kedua, bila parsing gagal karena
	 * alasan apa pun (format tahun akademik tidak sesuai pola {@code "YYYY/YYYY"}, string kosong, atau
	 * exception lain), exception tersebut ditelan (empty-catch) dan hanya direkam lewat
	 * {@code ais.common.ErrorAuditUtil.record(Exception, String)} untuk keperluan audit — pola
	 * penanganan silent-catch yang seragam di seluruh entity {@code hbm2java} lawas pada paket ini,
	 * bukan bug yang baru ditemukan di sini. Setelah percobaan parsing (berhasil atau gagal), bila
	 * {@link #tahun} masih {@code null} — baik karena baris {@code catch} tereksekusi maupun karena
	 * parsing "berhasil" tanpa error namun tetap tidak mengisi variabel (kasus yang secara praktis
	 * tidak terjadi dengan {@code parseInt}, namun dijaga eksplisit oleh kode) — nilai jatuh ke tahun
	 * kalender berjalan dari {@code ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)}. Karena
	 * field ini dihitung ulang setiap pembacaan (tidak seperti {@link #tahunAkademik}/
	 * {@link #ganjilGenap} yang hanya dihitung sekali), nilai {@link #getTahun()} akan selalu
	 * konsisten dengan {@link #getTahunAkademik()} terkini, termasuk bila tahun akademik diubah lewat
	 * {@link #setTahunAkademik(String)} setelah objek ini dibuat.
	 *
	 * @return tahun kalender hasil parsing tahun akademik, atau tahun kalender berjalan sebagai
	 *         fallback bila parsing gagal; tidak pernah {@code null}.
	 */
	public Integer getTahun() {
		try {
			tahun = Integer.parseInt(StringUtils.split(getTahunAkademik(), "/")[0].trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/epsbed/KapasitasMahasiswaBaru.java:356");
			// Common.tampilErrorJikaAdmin(e);
		}
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun kalender secara eksplisit. Nilai ini akan ditimpa kembali pada pembacaan
	 * berikutnya oleh {@link #getTahun()}, yang selalu menghitung ulang dari {@link #getTahunAkademik()}
	 * terlebih dahulu — sehingga setter ini pada praktiknya hanya berpengaruh sesaat sebelum
	 * {@link #getTahun()} dipanggil lagi.
	 *
	 * @param tahun tahun kalender baru.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

}
