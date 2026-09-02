package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Entity Hibernate untuk <b>kurikulum</b> pada tabel {@code public.kurikulum} — satu baris mewakili
 * satu kurikulum yang berlaku pada satu {@link Jurusan} + {@link Program} tertentu, misalnya
 * "Kurikulum 2018 S1 Teknik Informatika" atau "Kurikulum 2023 D3 Akuntansi".
 *
 * <h3>Peran dalam domain akademik</h3>
 * <p>Kurikulum adalah <b>simpul pusat perencanaan akademik</b>. Ia tidak menyimpan daftar mata
 * kuliahnya sendiri; daftar itu ada di join table {@link KurikulumPunyaMatakuliah} (satu baris per
 * pasangan kurikulum+mata kuliah, lengkap dengan semester penempatan, wajib/pilihan, dan seluruh
 * berkas RPS/OBE). Dari sisi runtime, {@link Perkuliahan} (kelas yang dibuka pada satu semester)
 * menunjuk ke kurikulum, dan lewat rantai itulah aturan kurikulum ikut menentukan:</p>
 * <ul>
 * <li><b>siapa yang boleh mengambil</b> kelas suatu kurikulum — lihat
 * {@link #bolehAmbil(Mahasiswa)}, dipanggil dari {@code AmbilDataPerkuliahanHelper},
 * {@code AmbilDataPerkuliahanNonPaketHelper}, {@code ElearningApiUtil}, dan
 * {@link Detailperkuliahan};</li>
 * <li><b>apakah penilaian memakai skema OBE</b> (Outcome Based Education) atau skema lama — lihat
 * {@link #apakahObe(String, String)}, dipanggil dari puluhan titik di {@code AktifitasPerkuliahan
 * Helper}, {@code DetailUjianHelper}, {@code HasilUjianMahasiswaHelper}, {@code FormatPenilaian
 * Helper}, sampai dasbor {@code DashboardTimelinePertemuan};</li>
 * <li><b>syarat kelulusan</b> dalam bentuk jumlah SKS wajib/pilihan/lulus, dipakai layar
 * {@code KurikulumAction} dan diekspor ke Feeder (PDDikti).</li>
 * </ul>
 *
 * <h3>Relasi</h3>
 * <ul>
 * <li>{@link Jurusan} ({@code jurusan}, kolom {@code jurusan}) — {@code @ManyToOne} LAZY,
 * {@code nullable = true}. Pemilik kurikulum. Getter-nya melewati
 * {@link GeneralValueObject#check(Object)}.</li>
 * <li>{@link Program} ({@code program}, kolom {@code program}) — {@code @ManyToOne} dengan
 * {@code FetchMode.SELECT}, {@code nullable = true}. Jenjang/program pendidikan (S1, D3, ...).
 * Berbeda dengan {@code jurusan}, getter-nya <b>tidak</b> memanggil {@code check()} — lihat
 * {@link #getProgram()}.</li>
 * <li>{@link KurikulumPunyaMatakuliah} — sisi "banyak" dari kurikulum. <b>Tidak dipetakan sebagai
 * koleksi di kelas ini</b>; selalu diambil lewat query eksplisit dari sisi join table. Ini
 * disengaja: satu kurikulum bisa berisi ratusan baris berukuran besar (isi RPS), sehingga koleksi
 * {@code @OneToMany} akan sangat mahal.</li>
 * <li>{@link Perkuliahan}, {@link Detailperkuliahan} — pengguna hilir; menunjuk ke kurikulum, bukan
 * sebaliknya.</li>
 * </ul>
 *
 * <h3>Field jejak audit yang dideklarasikan ulang — keharusan teknis</h3>
 * <p>Kelas ini mendeklarasikan ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} meskipun {@link GeneralValueObject} sudah punya konsep yang sama. Ini
 * <b>bukan duplikasi yang keliru</b>: {@link GeneralValueObject} adalah POJO abstrak biasa — bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate <b>tidak memetakan properti
 * apa pun dari kelas induk</b>. Setiap entity konkret wajib mendeklarasikan sendiri kolom-kolom
 * tersebut agar ikut tersimpan. Pola ini seragam di seluruh {@code ais.database.model}.</p>
 *
 * <h3>Getter yang menulis balik ke field (dan karenanya ke database)</h3>
 * <p>Ini kuirk paling penting di kelas ini. Sebagian besar getter <b>bukan getter murni</b>: bila
 * field-nya masih {@code null} mereka mengisi nilai default lalu <b>menyimpannya ke field</b>.
 * Karena instance yang masih <i>attached</i> ke {@code Session} Hibernate akan dicek kotor
 * (<i>dirty check</i>) saat flush, sekadar <b>membaca</b> kurikulum bisa menghasilkan
 * {@code UPDATE} dan — karena kelas ini {@code @Audited} — sebuah baris revisi Envers baru.
 * Daftar lengkapnya:</p>
 * <ul>
 * <li>{@link #getTahun()} &rarr; tahun berjalan dari {@code WaktuUtil};</li>
 * <li>{@link #getNama()} &rarr; nama rakitan "{@code <program> <jurusan> thn <tahun> - ID: <id>}";</li>
 * <li>{@link #getTahunAkademik()} &rarr; "{@code <tahun>/<tahun+1>}";</li>
 * <li>{@link #getJenisSemester()} &rarr; {@link Perkuliahan#GANJIL};</li>
 * <li>{@link #getJumlahAturanSksWajib()}, {@link #getJumlahAturanSksPilihan()},
 * {@link #getJumlahAturanSksLulus()} &rarr; {@code 0};</li>
 * <li>{@link #getFeeders()} &rarr; string kosong;</li>
 * <li>{@link #getNamaAsli()} &rarr; string kosong;</li>
 * <li>{@link #getJurusan()} &rarr; hasil {@code check()} (bisa instance lain);</li>
 * <li>{@link #getTaObe()} &rarr; <b>selalu</b> menghitung ulang dari {@code tahunAkademikObe} +
 * {@code semesterObe} dan menimpa field, bukan hanya saat {@code null}.</li>
 * </ul>
 * <p>Sebaliknya, getter berikut <b>tidak</b> menulis balik — mereka hanya menormalkan nilai
 * kembalian: {@link #getAktif()}, {@link #getObe()},
 * {@link #getNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan()}, dan {@link #getFeeder()}.
 * Konsekuensinya, kolom {@code aktif}/{@code obe} di database boleh saja tetap {@code NULL} sambil
 * getter-nya mengembalikan {@code true}/{@code false}; query Criteria di {@code KurikulumAction}
 * karena itu selalu ditulis {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} — meniru
 * pembacaan {@code null == true} milik {@link #getAktif()}. <b>Jangan menulis filter
 * {@code eq("aktif", true)} saja</b>, kurikulum lama akan hilang dari hasil.</p>
 * <p>Tidak ada satu pun method di kelas ini yang membuka atau menutup {@code Session} Hibernate
 * secara langsung (kelas ini bahkan tidak meng-import {@code HibernateUtil}). Satu-satunya akses
 * database implisit terjadi di dalam {@link GeneralValueObject#check(Object)} yang dipakai
 * {@link #getJurusan()}, dan penutupan sesi di sana sudah ditangani kelas induk.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 * <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas &amp; relasi</b> — {@link #Kurikulum()}, {@link #getId()}, {@link #getNama()},
 * {@link #getNamaAsli()}, {@link #getKeterangan()}, {@link #getJurusan()}, {@link #getProgram()},
 * {@link #toString()}.</li>
 * <li><b>Masa berlaku</b> — {@link #getTahun()}, {@link #getTahunAkademik()},
 * {@link #getJenisSemester()}, {@link #getAktif()}.</li>
 * <li><b>Aturan pengambilan per angkatan</b> — {@link #getTahunAngkatanMulai()},
 * {@link #getTahunAngkatanSampai()},
 * {@link #getNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan()}, dan method bisnisnya
 * {@link #bolehAmbil(Mahasiswa)}.</li>
 * <li><b>Aturan kelulusan</b> — {@link #getJumlahAturanSksWajib()},
 * {@link #getJumlahAturanSksPilihan()}, {@link #getJumlahAturanSksLulus()}.</li>
 * <li><b>Ambang OBE</b> — {@link #getObe()}, {@link #getTahunAkademikObe()},
 * {@link #getSemesterObe()}, {@link #getTaObe()}, dan method bisnisnya
 * {@link #apakahObe(String, String)}.</li>
 * <li><b>Integrasi Feeder/PDDikti</b> — {@link #getFeeder()}, {@link #getFeeders()}.</li>
 * </ol>
 *
 * <h3>Catatan pemetaan</h3>
 * <p>Hanya sebagian getter yang diberi {@code @Column} eksplisit ({@code id}, {@code keterangan},
 * {@code tahun}, {@code nama}, {@code feeders}); sisanya mengandalkan penamaan kolom default
 * Hibernate dari nama properti. Kelas ini {@code dynamicInsert}/{@code dynamicUpdate}, jadi hanya
 * kolom yang benar-benar berubah yang ikut ditulis — yang meredam, tapi tidak menghapus, efek
 * getter penulis-balik di atas.</p>
 * <p>Kontrak umum {@code equals}, {@code hashCode}, {@code compareTo}, cache, dan terutama
 * {@link GeneralValueObject#check(Object)} dijelaskan lengkap di kelas induk — jangan diulang di
 * sini.</p>
 *
 * @see GeneralValueObject
 * @see KurikulumPunyaMatakuliah
 * @see Jurusan
 * @see Perkuliahan
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kurikulum")
public class Kurikulum extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Catatan: nilai ini <b>identik</b> dengan {@code serialVersionUID} milik
	 * {@link KurikulumPunyaMatakuliah}, {@link BeasiswaPunyaItemBiayaTambahan}, dan
	 * {@link UjianPunyaSoal} — total empat kelas memakai angka yang sama. Temuan salin-tempel yang
	 * dicatat dari sisi {@link KurikulumPunyaMatakuliah} dengan ini <b>terkonfirmasi</b>, dan
	 * ternyata cakupannya lebih luas dari dua kelas. Tidak berdampak fungsional —
	 * {@code serialVersionUID} hanya dicocokkan per kelas, bukan lintas kelas — tapi jangan sekali
	 * pun dipakai sebagai penanda identitas kelas.</p>
	 */
	private static final long serialVersionUID = 2461822577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini (kolom jejak audit).
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p>Setter ini <b>sengaja mengabaikan</b> nilai {@code null} maupun string kosong/spasi:
	 * jejak audit yang sudah ada tidak boleh terhapus oleh pemanggil yang kebetulan tidak tahu
	 * siapa penggunanya. Akibatnya kolom ini tidak pernah bisa dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit lama tetap utuh.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini
	 * dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari konteks pengguna aktif dan memperbarui
	 * {@link #getTanggal_dirubah()}. Karena hook ini terikat pada {@code @PreUpdate} saja (bukan
	 * {@code @PrePersist}), baris yang baru pertama kali di-{@code INSERT} mengandalkan nilai awal
	 * field dan setter yang dipanggil layar penyimpan.</p>
	 *
	 * <p>Jangan panggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh {@link #onUpdate()}; panggilan manual hanya dipakai importir
	 * yang ingin mempertahankan waktu asal data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * <p>Field-nya diinisialisasi ke waktu server saat object dibuat ({@code WaktuUtil.getDate()}),
	 * sehingga tidak pernah {@code null} untuk instance baru.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks kurikulum dalam bentuk "{@code <id>-<nama>}".
	 *
	 * <p><b>Perhatian:</b> karena memanggil {@link #getNama()}, method ini ikut memicu perakitan
	 * nama otomatis beserta penulisan-baliknya ke field bila {@code nama} masih kosong. Memanggil
	 * {@code toString()} pada instance <i>attached</i> — misalnya di dalam string log — karena itu
	 * bisa menghasilkan {@code UPDATE} pada flush berikutnya.</p>
	 *
	 * @return "{@code <id>-<nama>}"; bagian id berbunyi "{@code null}" untuk kurikulum yang belum
	 *         tersimpan
	 */
	public String toString() {
		return getId() + "-" + getNama();
	}

	private String nama;
	private String namaAsli;
	private Integer tahun;
	private Jurusan jurusan;
	private String keterangan;
	private Program program;

	private String tahunAkademik;
	private String jenisSemester;

	private Integer jumlahAturanSksWajib;
	private Integer jumlahAturanSksPilihan;
	private Integer jumlahAturanSksLulus;

	private String feeder;
	private String feeders;

	private Integer tahunAngkatanMulai;
	private Integer tahunAngkatanSampai;

	private Boolean nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan;

	private Boolean aktif;
	private Boolean obe;

	private Integer taObe;
	private String semesterObe;
	private String tahunAkademikObe;

	/**
	 * Menentukan apakah seorang mahasiswa <b>boleh mengambil</b> mata kuliah dari kurikulum ini,
	 * berdasarkan tahun angkatannya.
	 *
	 * <p>Aturannya memakai rentang {@link #getTahunAngkatanMulai()} &ndash;
	 * {@link #getTahunAngkatanSampai()} yang keduanya opsional, sehingga ada empat kemungkinan:</p>
	 * <ol>
	 * <li>kedua batas terisi &rarr; angkatan harus berada di dalam rentang (inklusif di kedua
	 * ujung);</li>
	 * <li>hanya batas bawah terisi &rarr; angkatan harus &ge; batas bawah;</li>
	 * <li>hanya batas atas terisi &rarr; angkatan harus &le; batas atas;</li>
	 * <li>kedua batas kosong &rarr; kurikulum berlaku untuk semua angkatan.</li>
	 * </ol>
	 *
	 * <p><b>Kebijakan gagal-terbuka:</b> method ini mengembalikan {@code true} juga ketika
	 * {@code mahasiswa} bernilai {@code null} atau tahun angkatannya belum terisi. Jadi
	 * "tidak diketahui" diperlakukan sama dengan "boleh" — data mahasiswa yang tidak lengkap tidak
	 * akan memblokir KRS. Kalau pemanggil butuh perilaku sebaliknya, ia harus mengecek sendiri
	 * kelengkapan data sebelum memanggil.</p>
	 *
	 * <p>Dipanggil dari penyaring pilihan kelas ({@code AmbilDataPerkuliahanHelper},
	 * {@code AmbilDataPerkuliahanNonPaketHelper}, {@code ElearningApiUtil}), dari laporan
	 * {@code LaporanKurikulumMahasiswa}, dan — dipasangkan dengan
	 * {@link #getNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan()} — dari
	 * {@code Detailperkuliahan} untuk <b>mencabut persetujuan KRS yang terlanjur diberikan</b>.</p>
	 *
	 * @param mahasiswa mahasiswa yang hendak mengambil; boleh {@code null}
	 * @return {@code true} bila boleh mengambil (termasuk saat data tidak cukup untuk menolak)
	 * @see #getTahunAngkatanMulai()
	 * @see #getTahunAngkatanSampai()
	 * @see #getNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan()
	 */
	public boolean bolehAmbil(Mahasiswa mahasiswa) {
		if (mahasiswa != null && mahasiswa.getTahunangkatan() != null) {
			if (getTahunAngkatanMulai() != null && getTahunAngkatanSampai() != null) {
				return mahasiswa.getTahunangkatan() >= getTahunAngkatanMulai()
						&& mahasiswa.getTahunangkatan() <= getTahunAngkatanSampai();
			} else if (getTahunAngkatanMulai() != null) {
				return mahasiswa.getTahunangkatan() >= getTahunAngkatanMulai();
			} else if (getTahunAngkatanSampai() != null) {
				return mahasiswa.getTahunangkatan() <= getTahunAngkatanSampai();
			}
		}
		return true;
	}

	/**
	 * Konstruktor tanpa argumen — wajib ada untuk Hibernate dan untuk pembuatan kurikulum baru dari
	 * layar {@code KurikulumAction} maupun dari importir Feeder.
	 *
	 * <p>Tidak mengisi apa pun kecuali {@code tanggal_dirubah} (lewat inisialisasi field). Nilai
	 * default field lain baru terbentuk saat getter-nya dipanggil pertama kali — lihat catatan
	 * "getter yang menulis balik" di dokumentasi kelas.</p>
	 */
	public Kurikulum() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} dan {@code insertable = false}, jadi nilainya ditentukan
	 * sequence PostgreSQL saat {@code INSERT} dan baru terisi setelah flush.</p>
	 *
	 * @return id kurikulum; {@code null} untuk instance yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Hanya dipakai Hibernate dan kode migrasi/impor; jangan diubah untuk
	 * baris yang sudah tersimpan.
	 *
	 * @param id id kurikulum
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas kurikulum (catatan SK penetapan, dasar hukum, dan sejenisnya).
	 *
	 * @return keterangan; boleh {@code null}
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas kurikulum.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi tahun berlaku kurikulum.
	 *
	 * @param tahun tahun berlaku, mis. {@code 2023}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun berlaku kurikulum — inilah angka yang biasa dipakai menamai kurikulum
	 * ("Kurikulum 2018", "Kurikulum 2023").
	 *
	 * <p><b>Getter penulis-balik:</b> bila field masih {@code null}, method ini mengisinya dengan
	 * <b>tahun berjalan</b> dari {@code WaktuUtil.getCalendar()} lalu menyimpannya ke field. Untuk
	 * instance yang masih <i>attached</i>, nilai itu ikut ter-{@code UPDATE} pada flush berikutnya.
	 * Efek sampingnya halus tapi nyata: kurikulum lama yang kolom {@code tahun}-nya kosong akan
	 * "berpindah" ke tahun saat pertama kali layarnya dibuka, bukan ke tahun aslinya.</p>
	 *
	 * <p>Nilai ini juga menjadi basis {@link #getTahunAkademik()} dan {@link #getNama()}, jadi
	 * pengisian otomatis di sini merambat ke keduanya.</p>
	 *
	 * @return tahun berlaku; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	@Column(name = "tahun")
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Mengisi jurusan pemilik kurikulum.
	 *
	 * @param jurusan jurusan pemilik; boleh {@code null}
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan {@link Jurusan} pemilik kurikulum.
	 *
	 * <p>Relasi ini {@code LAZY}, jadi getter memakai pola standar entity AIS: hasil
	 * {@link GeneralValueObject#check(Object)} <b>ditugaskan kembali ke field</b> supaya proxy lazy
	 * yang sudah <i>detached</i> tergantikan instance yang benar-benar bisa dipakai. Penjelasan
	 * lengkap mekanismenya (identity map, cache, reload lewat session baru, penutupan session) ada
	 * di kelas induk.</p>
	 *
	 * <p>Kolomnya {@code nullable = true}, sehingga kurikulum tanpa jurusan secara skema mungkin
	 * ada — dan {@link #getNama()} memang bergantung pada hal itu (nama otomatis tidak dirakit bila
	 * jurusan kosong).</p>
	 *
	 * @return jurusan pemilik; {@code null} bila memang belum ditentukan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Mengembalikan nama tampil kurikulum, <b>merakitnya sendiri bila masih kosong</b>.
	 *
	 * <p>Bila {@code nama} masih {@code null}/kosong <i>dan</i> jurusan sudah terisi, nama dirakit
	 * dengan pola:</p>
	 * <pre>{@code <nama program> <nama jurusan> thn <tahun>[ - ID: <id>]}</pre>
	 * <p>lalu <b>disimpan ke field</b> (getter penulis-balik — pada instance <i>attached</i> hasilnya
	 * ikut tersimpan permanen ke database). Karena hanya dirakit sekali, nama yang sudah terbentuk
	 * <b>tidak akan pernah menyesuaikan diri</b> bila program, jurusan, atau tahun kemudian
	 * berubah.</p>
	 *
	 * <p>Tiga kuirk yang perlu diketahui:</p>
	 * <ol>
	 * <li><b>Pemotongan mengambil bagian belakang.</b> Bila hasil rakitan lebih dari 60 karakter,
	 * yang disimpan adalah <b>59 karakter TERAKHIR</b> ({@code substring(nama.length() - 59)}) —
	 * jadi yang terbuang justru nama program dan jurusan di depan, menyisakan potongan seperti
	 * "{@code ...knik Informatika thn 2023 - ID: 412}". Ambang 60 ini juga tidak sejalan dengan
	 * {@code @Column(length = 255)} pada kolomnya; besar dugaan warisan lebar kolom lama.</li>
	 * <li><b>Membaca field {@code jurusan} langsung, bukan {@link #getJurusan()}</b>, sehingga
	 * {@code check()} dilewati. Pada instance <i>detached</i> yang jurusannya masih berupa proxy
	 * lazy, {@code jurusan.getNama()} di baris berikutnya bisa melempar
	 * {@code LazyInitializationException}. Perhatikan kontrasnya dengan {@link #getProgram()} yang
	 * dipanggil lewat getter.</li>
	 * <li><b>Bagian "- ID:" hanya muncul bila id sudah ada.</b> Kurikulum yang namanya dirakit
	 * sebelum {@code INSERT} pertama akan selamanya kehilangan penanda id itu.</li>
	 * </ol>
	 *
	 * @return nama tampil kurikulum; {@code null} bila nama kosong dan jurusan juga belum terisi
	 * @see #getNamaAsli()
	 */
	@Column(name = "nama", length = 255)
	public String getNama() {
		if (nama == null || nama.trim().isEmpty()) {
			if (jurusan != null) {
				nama = (getProgram() == null ? "" : getProgram().getNama()) + " " + jurusan.getNama() + " thn "
						+ getTahun() + (id == null ? "" : " - ID: " + getId());
				nama = nama.length() > 60 ? nama.substring(nama.length() - 59) : nama;
			}
		}
		return nama;
	}

	/**
	 * Mengisi nama tampil kurikulum secara eksplisit, mematikan perakitan otomatis di
	 * {@link #getNama()}.
	 *
	 * @param nama nama kurikulum
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan {@link Program} (jenjang pendidikan: S1, D3, Profesi, ...) kurikulum ini.
	 *
	 * <p><b>Berbeda dengan {@link #getJurusan()}, getter ini TIDAK memanggil
	 * {@link GeneralValueObject#check(Object)}.</b> Relasi ini memang tidak diberi
	 * {@code FetchType.LAZY} melainkan {@code @Fetch(FetchMode.SELECT)} sehingga umumnya sudah
	 * termuat, tetapi pada instance yang sudah lepas dari session ketidakkonsistenan ini membuat
	 * perilakunya berbeda dengan relasi jurusan. Bila pemanggil butuh jaminan resolusi, panggil
	 * sendiri {@code check(kurikulum.getProgram())}.</p>
	 *
	 * @return program/jenjang kurikulum; boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "program", nullable = true)
	public Program getProgram() {
		return program;
	}

	/**
	 * Mengisi program/jenjang kurikulum.
	 *
	 * @param program program pendidikan; boleh {@code null}
	 */
	public void setProgram(Program program) {
		this.program = program;
	}

	/**
	 * Mengembalikan <b>id kurikulum pada sistem Feeder/PDDikti</b> (UUID dari layanan Feeder),
	 * dalam bentuk sudah di-{@code trim}.
	 *
	 * <p>Bukan getter penulis-balik: normalisasi hanya berlaku pada nilai kembalian, field tetap
	 * seperti aslinya. String kosong/spasi dinormalkan menjadi {@code null} sehingga pemanggil cukup
	 * mengecek {@code null} saja — meski demikian {@code FeederExporter} dan {@code FeederJSONExport}
	 * tetap menulis pengecekan ganda {@code null || trim().isEmpty()}.</p>
	 *
	 * <p>Dipakai sebagai kunci sinkronisasi dua arah: {@code FeederExporter}/
	 * {@code FeederExporterGenerator} mengirimnya sebagai {@code id_kurikulum},
	 * {@code FeederImporter} memakainya untuk mencocokkan baris lokal dengan baris Feeder.</p>
	 *
	 * @return id Feeder yang sudah dirapikan, atau {@code null} bila belum tersinkron
	 * @see #getFeeders()
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Mengisi id kurikulum pada sistem Feeder/PDDikti.
	 *
	 * @param feeder id Feeder; boleh {@code null}
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Mengembalikan tahun akademik berlakunya kurikulum dalam format "{@code 2023/2024}".
	 *
	 * <p><b>Getter penulis-balik:</b> bila masih {@code null}, dirakit dari {@link #getTahun()}
	 * sebagai "{@code <tahun>/<tahun+1>}" lalu disimpan ke field. Perhatikan rantainya — bila
	 * {@code tahun} juga masih kosong, {@link #getTahun()} lebih dulu mengisi dirinya dengan tahun
	 * berjalan, sehingga satu pemanggilan bisa memutasi <b>dua</b> kolom sekaligus.</p>
	 *
	 * <p>Dipasangkan dengan {@link #getJenisSemester()} pada label pemilih kurikulum di banyak layar
	 * ({@code KurikulumAction}, {@code PenjadwalanUtil}, {@code AmbilDataKurikulumBanbox}, ...).</p>
	 *
	 * @return tahun akademik "{@code awal/akhir}"; tidak pernah {@code null} setelah pemanggilan
	 *         pertama
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = getTahun() + "/" + (getTahun() + 1);
		}
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik berlakunya kurikulum.
	 *
	 * @param tahunAkademik tahun akademik format "{@code 2023/2024}"
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenis semester mulai berlakunya kurikulum
	 * ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}/{@link Perkuliahan#SP}).
	 *
	 * <p><b>Getter penulis-balik:</b> bila masih {@code null}, diisi {@link Perkuliahan#GANJIL} lalu
	 * disimpan ke field. Perlu diingat nilainya adalah <b>string berbahasa Indonesia</b>
	 * ("Ganjil"/"Genap"/"Semester Pendek"), bukan enum atau angka — perbandingan di seluruh
	 * codebase dilakukan dengan {@code equals} terhadap konstanta {@link Perkuliahan}, jangan
	 * ditulis literal.</p>
	 *
	 * @return jenis semester; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	public String getJenisSemester() {
		if (jenisSemester == null) {
			jenisSemester = Perkuliahan.GANJIL;
		}
		return jenisSemester;
	}

	/**
	 * Mengisi jenis semester mulai berlakunya kurikulum.
	 *
	 * @param jenisSemester salah satu konstanta {@link Perkuliahan#GANJIL},
	 *                      {@link Perkuliahan#GENAP}, atau {@link Perkuliahan#SP}
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan aturan <b>jumlah SKS mata kuliah wajib</b> yang harus ditempuh pada kurikulum
	 * ini.
	 *
	 * <p><b>Getter penulis-balik:</b> {@code null} diganti {@code 0} dan disimpan ke field.
	 * Konsekuensinya "belum diisi" tidak bisa dibedakan dari "sengaja nol" begitu layar kurikulum
	 * sekali dibuka.</p>
	 *
	 * <p>Ditampilkan di ringkasan aturan {@code KurikulumAction} dan diekspor ke Feeder sebagai
	 * {@code jumlah_sks_wajib}. Kelas ini <b>tidak</b> memvalidasi bahwa wajib + pilihan = lulus;
	 * ketiganya angka bebas yang diisi operator.</p>
	 *
	 * @return jumlah SKS wajib; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	public Integer getJumlahAturanSksWajib() {
		if (jumlahAturanSksWajib == null) {
			jumlahAturanSksWajib = 0;
		}
		return jumlahAturanSksWajib;
	}

	/**
	 * Mengisi aturan jumlah SKS mata kuliah wajib.
	 *
	 * @param jumlahAturanSksWajib jumlah SKS wajib
	 */
	public void setJumlahAturanSksWajib(Integer jumlahAturanSksWajib) {
		this.jumlahAturanSksWajib = jumlahAturanSksWajib;
	}

	/**
	 * Mengembalikan aturan <b>jumlah SKS mata kuliah pilihan</b> yang harus ditempuh pada kurikulum
	 * ini.
	 *
	 * <p><b>Getter penulis-balik</b> dengan default {@code 0} — sama persis dengan
	 * {@link #getJumlahAturanSksWajib()}. Diekspor ke Feeder sebagai {@code jumlah_sks_pilihan}.</p>
	 *
	 * @return jumlah SKS pilihan; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	public Integer getJumlahAturanSksPilihan() {
		if (jumlahAturanSksPilihan == null) {
			jumlahAturanSksPilihan = 0;
		}
		return jumlahAturanSksPilihan;
	}

	/**
	 * Mengisi aturan jumlah SKS mata kuliah pilihan.
	 *
	 * @param jumlahAturanSksPilihan jumlah SKS pilihan
	 */
	public void setJumlahAturanSksPilihan(Integer jumlahAturanSksPilihan) {
		this.jumlahAturanSksPilihan = jumlahAturanSksPilihan;
	}

	/**
	 * Mengembalikan aturan <b>total SKS untuk dinyatakan lulus</b> pada kurikulum ini.
	 *
	 * <p><b>Getter penulis-balik</b> dengan default {@code 0}. Diekspor ke Feeder sebagai
	 * {@code jumlah_sks_lulus}. Perlu dicatat bahwa angka ini hanya deklaratif di entity — kelulusan
	 * mahasiswa dihitung di lapisan lain (helper transkrip/yudisium), bukan di sini.</p>
	 *
	 * @return total SKS kelulusan; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	public Integer getJumlahAturanSksLulus() {
		if (jumlahAturanSksLulus == null) {
			jumlahAturanSksLulus = 0;
		}
		return jumlahAturanSksLulus;
	}

	/**
	 * Mengisi aturan total SKS kelulusan.
	 *
	 * @param jumlahAturanSksLulus total SKS kelulusan
	 */
	public void setJumlahAturanSksLulus(Integer jumlahAturanSksLulus) {
		this.jumlahAturanSksLulus = jumlahAturanSksLulus;
	}

	/**
	 * Mengembalikan <b>riwayat id Feeder</b> kurikulum ini — daftar id lama yang pernah dipakai,
	 * dipisahkan titik koma.
	 *
	 * <p>Berbeda dengan {@link #getFeeder()} yang menyimpan satu id aktif, kolom ini bertipe
	 * {@code text} dan menampung akumulasi. {@code FeederJSONImport} menambah entri dengan pola
	 * {@code setFeeders(kurikulum.getFeeder() + ";" + existing.getFeeders())} — <b>menumpuk di depan
	 * tanpa deduplikasi dan tanpa batas panjang</b>, jadi impor berulang bisa membuat isinya
	 * membengkak dan mengandung id yang sama berkali-kali. Tidak ada method di kelas ini yang
	 * mengurainya kembali; penguraian (bila perlu) menjadi urusan pemanggil.</p>
	 *
	 * <p><b>Getter penulis-balik:</b> {@code null} diganti string kosong dan disimpan ke field.</p>
	 *
	 * @return riwayat id Feeder terpisah titik koma; string kosong bila belum ada, tidak pernah
	 *         {@code null}
	 * @see #getFeeder()
	 */
	@Column(columnDefinition = "text")
	public String getFeeders() {
		if (feeders == null) {
			feeders = "";
		}
		return feeders;
	}

	/**
	 * Mengisi riwayat id Feeder.
	 *
	 * @param feeders daftar id Feeder terpisah titik koma
	 */
	public void setFeeders(String feeders) {
		this.feeders = feeders;
	}

	/**
	 * Mengembalikan <b>nama asli kurikulum menurut sumber data eksternal</b> (Feeder/PDDikti),
	 * terpisah dari {@link #getNama()} yang bisa dirakit ulang atau dipotong sistem.
	 *
	 * <p>Diisi importir: {@code FeederJSONImport} menyalin field {@code nama_kurikulum} dari respons
	 * Feeder apa adanya, dan {@code ImporKrsFeeder} merangkai nama untuk kurikulum yang dibuat
	 * otomatis lalu menyalinnya juga ke {@code nama}. Di sisi UI, nilai ini dipakai sebagai
	 * <i>tooltip</i>/deskripsi item combo di banyak layar pemilih kurikulum
	 * ({@code PenjadwalanUtil}, {@code TemplatePerkuliahanDetailAction}, {@code LaporanKurikulum},
	 * ...) sehingga operator tetap melihat nama utuh meski {@link #getNama()} sudah terpotong.</p>
	 *
	 * <p><b>Getter penulis-balik:</b> {@code null} diganti string kosong dan disimpan ke field.</p>
	 *
	 * @return nama asli dari sumber eksternal; string kosong bila tidak ada, tidak pernah
	 *         {@code null}
	 * @see #getNama()
	 */
	public String getNamaAsli() {
		if (namaAsli == null) {
			namaAsli = "";
		}
		return namaAsli;
	}

	/**
	 * Mengisi nama asli kurikulum menurut sumber data eksternal.
	 *
	 * @param namaAsli nama asli dari Feeder atau berkas impor
	 */
	public void setNamaAsli(String namaAsli) {
		this.namaAsli = namaAsli;
	}

	/**
	 * Menyatakan apakah kurikulum ini masih <b>aktif</b> (boleh dipakai membuka kelas baru).
	 *
	 * <p><b>Default terbuka:</b> nilai {@code null} dibaca sebagai {@code true}. Ini <b>bukan</b>
	 * getter penulis-balik — kolom di database tetap {@code NULL}. Karena itu setiap query yang
	 * menyaring kurikulum aktif harus meniru pembacaan tersebut:</p>
	 * <pre>{@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))}</pre>
	 * <p>Pola itulah yang dipakai konsisten di {@code KurikulumAction} dan {@code ImporKrsFeeder}.
	 * Menulis {@code Restrictions.eq("aktif", true)} saja akan <b>menyembunyikan seluruh kurikulum
	 * lama</b> yang kolomnya belum pernah diisi.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah ditentukan; {@code false} hanya bila
	 *         dinonaktifkan eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengaktifkan/menonaktifkan kurikulum.
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code null} berarti kembali ke perilaku
	 *              default "aktif"
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan batas <b>bawah</b> tahun angkatan mahasiswa yang boleh memakai kurikulum ini.
	 *
	 * <p>Getter murni: {@code null} berarti "tanpa batas bawah" dan dibiarkan apa adanya — makna
	 * {@code null} di sini <b>signifikan</b>, jangan dinormalkan menjadi 0.</p>
	 *
	 * @return tahun angkatan terkecil yang diizinkan, atau {@code null} bila tidak dibatasi
	 * @see #bolehAmbil(Mahasiswa)
	 */
	public Integer getTahunAngkatanMulai() {
		return tahunAngkatanMulai;
	}

	/**
	 * Mengisi batas bawah tahun angkatan pengguna kurikulum ini.
	 *
	 * @param tahunAngkatanMulai tahun angkatan terkecil; {@code null} berarti tanpa batas bawah
	 */
	public void setTahunAngkatanMulai(Integer tahunAngkatanMulai) {
		this.tahunAngkatanMulai = tahunAngkatanMulai;
	}

	/**
	 * Mengembalikan batas <b>atas</b> tahun angkatan mahasiswa yang boleh memakai kurikulum ini.
	 *
	 * <p>Getter murni; {@code null} berarti "tanpa batas atas".</p>
	 *
	 * @return tahun angkatan terbesar yang diizinkan, atau {@code null} bila tidak dibatasi
	 * @see #bolehAmbil(Mahasiswa)
	 */
	public Integer getTahunAngkatanSampai() {
		return tahunAngkatanSampai;
	}

	/**
	 * Mengisi batas atas tahun angkatan pengguna kurikulum ini.
	 *
	 * @param tahunAngkatanSampai tahun angkatan terbesar; {@code null} berarti tanpa batas atas
	 */
	public void setTahunAngkatanSampai(Integer tahunAngkatanSampai) {
		this.tahunAngkatanSampai = tahunAngkatanSampai;
	}

	/**
	 * Menyatakan apakah KRS yang <b>terlanjur</b> diambil mahasiswa di luar rentang tahun angkatan
	 * harus <b>dicabut persetujuannya</b>.
	 *
	 * <p>Flag ini melengkapi {@link #bolehAmbil(Mahasiswa)}: {@code bolehAmbil} hanya menyaring
	 * pilihan <i>sebelum</i> KRS diambil, sedangkan flag ini bekerja <i>setelahnya</i>. Di
	 * {@code Detailperkuliahan}, kombinasi
	 * "{@code getNonAktifkan...() == true} <b>dan</b> {@code !kurikulum.bolehAmbil(mahasiswa)}"
	 * memaksa status persetujuan baris KRS turun menjadi {@code BELUM_DISETUJUI} — jadi mengaktifkan
	 * opsi ini bisa <b>membatalkan KRS yang sudah disetujui secara massal dan retroaktif</b>. Perlu
	 * kehati-hatian saat mengubahnya dari layar {@code KurikulumAction}.</p>
	 *
	 * <p>Default {@code false} bila kolom masih {@code null} (getter murni, tidak menulis balik).
	 * Perhatikan bahwa default-nya <b>berlawanan arah</b> dengan {@link #getAktif()}: yang ini
	 * gagal-tertutup (tidak mencabut apa pun), yang itu gagal-terbuka.</p>
	 *
	 * @return {@code true} bila pencabutan retroaktif diaktifkan
	 * @see #bolehAmbil(Mahasiswa)
	 */
	public Boolean getNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan() {
		return nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan == null ? false
				: nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan;
	}

	/**
	 * Mengaktifkan/menonaktifkan pencabutan retroaktif KRS di luar rentang tahun angkatan.
	 *
	 * @param nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan {@code true} untuk mencabut
	 *                                                                persetujuan KRS yang tidak
	 *                                                                sesuai rentang angkatan
	 */
	public void setNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan(
			Boolean nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan) {
		this.nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan = nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan;
	}

	/**
	 * Menyatakan apakah kurikulum ini <b>berbasis OBE</b> (Outcome Based Education).
	 *
	 * <p>Ini sakelar utamanya saja; kapan OBE mulai berlaku ditentukan pasangan
	 * {@link #getTahunAkademikObe()} + {@link #getSemesterObe()}. Untuk keputusan nyata
	 * per-perkuliahan, <b>jangan memakai method ini langsung</b> — pakai
	 * {@link #apakahObe(String, String)} yang sudah memperhitungkan ambang waktunya.</p>
	 *
	 * <p>Getter murni dengan default {@code false} bila kolom {@code null}.</p>
	 *
	 * @return {@code true} bila kurikulum ditandai OBE
	 * @see #apakahObe(String, String)
	 */
	public Boolean getObe() {
		return obe == null ? false : obe;
	}

	/**
	 * Menentukan apakah <b>penilaian OBE berlaku</b> untuk satu semester perkuliahan tertentu.
	 *
	 * <p>Inilah gerbang tunggal yang dipakai puluhan titik di lapisan aksi/helper untuk memilih
	 * antara skema penilaian OBE (CPMK, rubrik, pemetaan soal) dan skema lama. Alurnya:</p>
	 * <ol>
	 * <li>bila {@link #getObe()} {@code false} &rarr; langsung {@code false};</li>
	 * <li>argumen dikodekan menjadi <b>kode semester numerik</b> {@code id_smt}: tahun awal dari
	 * {@code tahunAkademik} (potongan sebelum "/") disambung satu digit semester —
	 * {@link Perkuliahan#GENAP} &rarr; {@code "2"}, {@link Perkuliahan#SP} &rarr; {@code "3"},
	 * <b>selain itu</b> &rarr; {@code "1"}. Jadi "2023/2024" + "Genap" menjadi {@code 20232};</li>
	 * <li>hasilnya dibandingkan dengan ambang {@link #getTaObe()} yang dikodekan dengan formula
	 * sama dari {@code tahunAkademikObe} + {@code semesterObe};</li>
	 * <li>OBE berlaku bila kode semester perkuliahan <b>&ge;</b> ambang.</li>
	 * </ol>
	 *
	 * <p>Beberapa hal yang perlu diketahui:</p>
	 * <ul>
	 * <li><b>Digit semester bersifat "sisanya Ganjil".</b> Nilai apa pun yang bukan
	 * {@code "Genap"}/{@code "Semester Pendek"} — termasuk salah ketik — dipetakan ke {@code "1"}.
	 * Tidak ada validasi.</li>
	 * <li><b>Argumen kosong &rarr; kode kecil.</b> {@code tahunAkademik} kosong menghasilkan awalan
	 * {@code "0"}, sehingga kodenya jatuh jauh di bawah ambang mana pun dan hasilnya {@code false}
	 * — gagal-tertutup (aman: skema lama yang dipakai).</li>
	 * <li><b>Kegagalan parsing ditelan.</b> Dua blok {@code catch} (bertanda
	 * {@code auto-audit(empty-catch)}) hanya merekam ke {@code ErrorAuditUtil}; bila
	 * {@code Integer.parseInt} gagal, {@code ta} tetap {@code null} lalu dipaksa {@code 0} — lagi-lagi
	 * jatuh ke {@code false}.</li>
	 * <li><b>Memanggil {@link #getTaObe()} berarti memicu penulisan-balik.</b> Lihat peringatan di
	 * getter tersebut; jadi method yang tampak hanya "membaca" ini bisa mengotori entity.</li>
	 * </ul>
	 *
	 * @param tahunAkademik tahun akademik perkuliahan yang dinilai, format "{@code 2023/2024}";
	 *                      boleh {@code null}/kosong
	 * @param semester      jenis semester perkuliahan; bandingkan dengan konstanta
	 *                      {@link Perkuliahan}; boleh {@code null}/kosong
	 * @return {@code true} bila semester tersebut sudah masuk masa berlaku OBE kurikulum ini
	 * @see #getObe()
	 * @see #getTaObe()
	 */
	public Boolean apakahObe(String tahunAkademik, String semester) {
		Integer ta = null;
		try {
			if (!getObe()) {
				return false;
			}
			String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0"
					: tahunAkademik.split("/")[0])
					+ (semester == null || semester.trim().isEmpty() ? "0"
							: semester.equals(Perkuliahan.GENAP) ? "2" : semester.equals(Perkuliahan.SP) ? "3" : "1");

			try {
				ta = Integer.parseInt(id_smt.trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kurikulum.java:338");

			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kurikulum.java:342");
			// TODO: handle exception
		}
		if (ta == null) {
			ta = 0;
		}
		return ta >= getTaObe();
	}

	/**
	 * Menandai kurikulum ini sebagai berbasis OBE atau tidak.
	 *
	 * @param obe {@code true} bila memakai skema OBE
	 */
	public void setObe(Boolean obe) {
		this.obe = obe;
	}

	/**
	 * Mengembalikan <b>ambang mulai berlakunya OBE</b> dalam bentuk kode semester numerik
	 * (mis. {@code 20232} untuk "2023/2024" semester Genap).
	 *
	 * <p><b>Getter penulis-balik yang paling agresif di kelas ini:</b> berbeda dengan getter lain
	 * yang hanya mengisi saat {@code null}, method ini <b>selalu menghitung ulang</b> dari
	 * {@link #getTahunAkademikObe()} + {@link #getSemesterObe()} dan <b>selalu menimpa</b> field
	 * {@code taObe}. Dua akibatnya:</p>
	 * <ol>
	 * <li>{@link #setTaObe(Integer)} praktis <b>tidak berpengaruh</b> — nilai yang dipasang akan
	 * ditimpa pada pembacaan berikutnya. Sumber kebenaran yang sesungguhnya adalah pasangan
	 * {@code tahunAkademikObe}/{@code semesterObe}, bukan kolom ini;</li>
	 * <li>karena {@link #apakahObe(String, String)} memanggilnya di setiap evaluasi, dan
	 * {@code apakahObe} sendiri dipanggil di dalam perulangan dasbor/penilaian, entity kurikulum
	 * yang <i>attached</i> bisa berulang kali ditandai kotor. Kolomnya sendiri praktis
	 * <b>redundan</b> — nilai turunan yang disimpan.</li>
	 * </ol>
	 *
	 * <p>Formula pengkodeannya identik dengan yang dipakai {@link #apakahObe(String, String)}, dan
	 * kegagalan {@code Integer.parseInt} ditelan blok {@code catch} bertanda
	 * {@code auto-audit(empty-catch)} sehingga nilai lama bertahan; bila akhirnya masih {@code null}
	 * dipaksa {@code 0} — artinya <b>OBE berlaku sejak kapan pun</b> ketika ambangnya tidak bisa
	 * diuraikan. Perhatikan asimetrinya: kegagalan di sisi ambang gagal-<i>terbuka</i>, sedangkan
	 * kegagalan di sisi semester yang dinilai gagal-<i>tertutup</i>.</p>
	 *
	 * @return kode semester ambang OBE; {@code 0} bila belum/tidak dapat ditentukan
	 * @see #apakahObe(String, String)
	 */
	public Integer getTaObe() {
		String id_smt = (getTahunAkademikObe() == null || getTahunAkademikObe().trim().isEmpty() ? "0"
				: getTahunAkademikObe().split("/")[0])
				+ (getSemesterObe() == null || getSemesterObe().trim().isEmpty() ? "0"
						: getSemesterObe().equals(Perkuliahan.GENAP) ? "2"
								: getSemesterObe().equals(Perkuliahan.SP) ? "3" : "1");
		try {
			taObe = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Kurikulum.java:363");

		}
		if (taObe == null) {
			taObe = 0;
		}
		return taObe;
	}

	/**
	 * Mengisi kode semester ambang OBE.
	 *
	 * <p><b>Praktis tidak berguna:</b> {@link #getTaObe()} selalu menghitung ulang nilainya dari
	 * {@code tahunAkademikObe}/{@code semesterObe} dan menimpa apa pun yang dipasang di sini. Untuk
	 * mengubah ambang OBE, isi {@link #setTahunAkademikObe(String)} dan
	 * {@link #setSemesterObe(String)}. Setter ini tetap ada karena dibutuhkan Hibernate saat memuat
	 * baris dari database.</p>
	 *
	 * @param taObe kode semester ambang; akan ditimpa pada pembacaan berikutnya
	 * @see #getTaObe()
	 */
	public void setTaObe(Integer taObe) {
		this.taObe = taObe;
	}

	/**
	 * Mengembalikan jenis semester mulai berlakunya OBE.
	 *
	 * <p>Getter murni — {@code null} dibiarkan, dan penanganannya diserahkan ke
	 * {@link #getTaObe()} yang memetakan kosong menjadi digit {@code "0"}. Nilainya diharapkan
	 * berupa konstanta {@link Perkuliahan}.</p>
	 *
	 * @return jenis semester mulai OBE; boleh {@code null}
	 * @see #getTaObe()
	 */
	public String getSemesterObe() {
		return semesterObe;
	}

	/**
	 * Mengisi jenis semester mulai berlakunya OBE.
	 *
	 * @param semesterObe konstanta {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}/
	 *                    {@link Perkuliahan#SP}
	 */
	public void setSemesterObe(String semesterObe) {
		this.semesterObe = semesterObe;
	}

	/**
	 * Mengembalikan tahun akademik mulai berlakunya OBE, format "{@code 2023/2024}".
	 *
	 * <p>Getter murni — tidak seperti {@link #getTahunAkademik()}, method ini <b>tidak</b> merakit
	 * nilai default dari {@link #getTahun()}. Kekosongan di sini bermakna "ambang belum ditentukan"
	 * dan diproses {@link #getTaObe()} menjadi ambang {@code 0}.</p>
	 *
	 * @return tahun akademik mulai OBE; boleh {@code null}
	 * @see #getTaObe()
	 */
	public String getTahunAkademikObe() {
		return tahunAkademikObe;
	}

	/**
	 * Mengisi tahun akademik mulai berlakunya OBE.
	 *
	 * @param tahunAkademikObe tahun akademik format "{@code 2023/2024}"
	 */
	public void setTahunAkademikObe(String tahunAkademikObe) {
		this.tahunAkademikObe = tahunAkademikObe;
	}

}
