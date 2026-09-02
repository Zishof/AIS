package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.obe.CapaianPembelajaranLulusan;

/**
 * Entity Hibernate untuk <b>penempatan satu mata kuliah di dalam satu kurikulum</b>, pada tabel
 * {@code public.kurikulum_punya_matakuliah}. Satu baris = satu pasangan
 * ({@link Kurikulum}, {@link Matakuliah}) beserta atribut penempatannya: semester keberapa mata
 * kuliah itu ditawarkan, tahap mana, wajib/pilihan-nya, sampai seluruh berkas RPS/OBE mata kuliah
 * itu <i>khusus untuk kurikulum tersebut</i>.
 *
 * <h3>Peran: join table dengan atribut sendiri</h3>
 * <p>Relasi kurikulum &harr; mata kuliah bersifat banyak-ke-banyak (satu mata kuliah bisa dipakai
 * banyak kurikulum, satu kurikulum berisi banyak mata kuliah), sehingga tidak bisa direpresentasikan
 * sebagai field di {@link Matakuliah} maupun di {@link Kurikulum}. Kelas ini adalah <i>join
 * table</i>-nya — tetapi bukan join table polos: ia membawa <b>puluhan atribut sendiri</b> yang
 * hanya masuk akal untuk pasangan kurikulum+mata kuliah tertentu:</p>
 * <ul>
 * <li>penempatan akademik: {@code semester}, {@code tahap}, {@code inti}, {@code institusional},
 * {@code aktif}, {@code terdapatTugas};</li>
 * <li>berkas RPS/OBE: {@code rincian} (agenda mingguan dalam JSON), {@code cplBobot},
 * {@code komponenPenilaian}, {@code teknikPerCpmk}, {@code rubrikPenilaian},
 * {@code pemetaanSoalUts}/{@code pemetaanSoalUas}, {@code minimalKetercapaian},
 * {@code nilaiMenggunakanCpmk};</li>
 * <li>berkas administratif: {@code tanggalPenyusunan}, {@code pengembangRps}, {@code koordinator},
 * {@code dosen}, {@code mitraPengembang}, {@code pustaka}, {@code pustakaPendukung},
 * {@code mkPrasyarat}, {@code catatan}, {@code dikunci}.</li>
 * </ul>
 * <p>Konsekuensi desain ini: <b>RPS milik kurikulum, bukan milik mata kuliah</b>. Mata kuliah yang
 * sama di kurikulum 2018 dan kurikulum 2023 punya dua baris di sini, dengan dua RPS yang bisa
 * berbeda total. Menyalin kurikulum berarti menyalin baris-baris ini beserta seluruh isi RPS-nya
 * (lihat {@code PenjadwalanHelper} yang menyalin satu per satu field OBE dari baris lama ke baris
 * baru).</p>
 *
 * <h3>Hubungan dengan entity lain</h3>
 * <ul>
 * <li>{@link Kurikulum} ({@code kurikulum}) dan {@link Matakuliah} ({@code matakuliah}) — dua sisi
 * relasinya. Keduanya dipetakan {@code nullable = true}, jadi <b>secara skema baris yatim mungkin
 * saja ada</b>; beberapa method di kelas ini (terutama {@link #populateRinci(JSONObject)})
 * mengasumsikan {@code matakuliah} tidak null dan akan melempar {@code NullPointerException} bila
 * asumsinya meleset.</li>
 * <li>{@code indukMatakuliah} — foreign key ke <b>tabel ini sendiri</b>, membentuk hierarki mata
 * kuliah induk &rarr; modul/sub mata kuliah di dalam satu kurikulum
 * (lihat {@link #getIndukMatakuliah()}).</li>
 * <li>{@link Tbmuser} ({@code dikunci}) — penanda siapa mengunci baris RPS ini dari penyuntingan
 * lebih lanjut.</li>
 * <li>{@link ais.database.model.obe.CapaianPembelajaranLulusan} — tidak disimpan sebagai relasi,
 * melainkan diambil ulang lewat query di {@link #populateRinci(JSONObject)} berdasarkan CSV id CPL
 * milik {@link Matakuliah#getCapaianPembelajaranLulusan()}.</li>
 * </ul>
 *
 * <h3>Nilai turunan yang ditulis balik saat dibaca</h3>
 * <p>Seperti kebanyakan entity AIS, beberapa getter di kelas ini <b>bukan getter murni</b>: mereka
 * menghitung ulang lalu <b>menimpa field</b>, sehingga nilai baru ikut ter-<i>flush</i> ke database
 * pada transaksi berikutnya meski pemanggil tidak pernah memanggil {@code set...}. Ringkasnya:</p>
 * <ul>
 * <li>{@link #getFeeder()} menyusun ulang id Feeder dari {@code kurikulum.feeder + "-" +
 * matakuliah.feeder} dan menimpa kolomnya;</li>
 * <li>{@link #getDeskripsiPembelajaran()} dan {@link #getCapaianPembelajaranProdi()}
 * <b>mewarisi isi dari {@link Matakuliah}</b> bila kolom di baris ini masih kosong — dan warisan
 * itu langsung dimaterialisasi menjadi data milik baris ini (setelah itu perubahan di
 * {@code Matakuliah} tidak lagi ikut terbawa);</li>
 * <li>{@link #getJumlahPertemuanPerkuliahanDefault()} mengisi sendiri kolomnya dari konfigurasi
 * global {@code jumlah_pertemuan_perkuliahan_default};</li>
 * <li>{@link #getPustaka()}, {@link #getPustakaPendukung()}, {@link #getDosen()}, dan
 * {@link #getMkPrasyarat()} menormalkan CSV-nya (buang koma ganda, buang duplikat) dan menimpa
 * field — dengan efek samping <b>urutan elemen hilang</b>, karena deduplikasi memakai
 * {@link HashSet};</li>
 * <li>{@link #toString()} pun menulis balik field {@code kurikulum} dan {@code matakuliah}, karena
 * memanggil getter relasinya.</li>
 * </ul>
 * <p>Akibat praktisnya: <b>jangan pernah membandingkan kolom-kolom itu di klausa
 * {@code Restrictions}/HQL dengan asumsi isinya sudah mutakhir</b>, dan jangan heran bila membuka
 * layar RPS saja sudah menghasilkan {@code UPDATE} di log Hibernate.</p>
 *
 * <h3>Format kolom CSV</h3>
 * <p>Empat kolom teks menyimpan daftar id dalam bentuk <b>CSV yang dibungkus koma</b>
 * ({@code ",12,45,"}) — persis pola yang dipakai {@link Matakuliah#getCapaianPembelajaranLulusan()}
 * — supaya uji keanggotaan cukup dengan {@code contains(",id,")}:</p>
 * <ul>
 * <li>{@code dosen} &rarr; id {@link Dosen} pengampu/pengembang RPS;</li>
 * <li>{@code mkPrasyarat} &rarr; id {@link Matakuliah} (bukan id baris kelas ini) yang menjadi
 * prasyarat;</li>
 * <li>{@code pustaka} dan {@code pustakaPendukung} &rarr; id
 * {@link ais.database.model.obe.ReferensiLulusan} (pustaka utama dan pendukung).</li>
 * </ul>
 * <p>Untuk daftar kosong, getter-getter itu mengembalikan {@code ",,"} (dua koma), bukan string
 * kosong — pemanggil yang menghitung panjang harus sadar akan hal ini.</p>
 *
 * <h3>Berkas RPS/OBE berbasis teks bebas</h3>
 * <p>Selain {@code rincian} yang berupa JSON terstruktur, sejumlah kolom OBE disimpan sebagai
 * <b>teks berformat konvensi</b> (bukan tabel anak, bukan JSON), sehingga penguraiannya ada di
 * pemanggil, bukan di sini:</p>
 * <ul>
 * <li>{@code cplBobot} &mdash; {@code "CPL-2:15,CPL-4:45,CPL-9:40"};</li>
 * <li>{@code komponenPenilaian} &mdash; {@code "Kuis:10,Tugas:10,Keaktifan:10,UTS:30,UAS:40"};</li>
 * <li>{@code teknikPerCpmk} &mdash; satu baris per CPMK,
 * {@code "CPMK-1:Kuis,UTS\nCPMK-2:Tugas,UAS,Unjuk Kerja"};</li>
 * <li>{@code pemetaanSoalUts}/{@code pemetaanSoalUas} &mdash; satu baris per sub-CPMK,
 * {@code "Sub CPMK 1.1|PG 1\nSub CPMK 2.1|PG 8,12"};</li>
 * <li>{@code rubrikPenilaian} &mdash; baris berawalan {@code '#'} adalah judul sub-rubrik, baris
 * lain berbentuk {@code "Aspek|Bobot%|Skor4|Skor3|Skor2|Skor1"}.</li>
 * </ul>
 * <p>Karena tidak ada validasi format di lapisan entity, satu-satunya penjaga bentuknya adalah UI
 * ({@code RpsObeAction}, {@code NilaiObeAction}) dan importir/eksportir Excel
 * ({@code RpsObeExcelHelper}). Data yang masuk lewat jalur lain (impor, patch SQL) bisa saja tidak
 * terbaca tanpa pesan kesalahan apa pun.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 * <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook
 * {@code @PreUpdate} {@link #onUpdate()}.</li>
 * <li><b>Identitas &amp; relasi</b> — {@link #getId()}, {@link #getKurikulum()},
 * {@link #getMatakuliah()}, {@link #getIndukMatakuliah()}, {@link #getDikunci()},
 * {@link #toString()}.</li>
 * <li><b>Penempatan akademik</b> — {@link #getSemester()}, {@link #getTahap()},
 * {@link #getInti()}, {@link #getInstitusional()}, {@link #getAktif()},
 * {@link #getTerdapatTugas()}, {@link #getJumlahPertemuanPerkuliahanDefault()}.</li>
 * <li><b>Berkas RPS/OBE</b> — {@link #getRincian()}, {@link #populateRinci(JSONObject)},
 * {@link #ambilRinci(JSONObject, int)}, plus getter/setter kolom teks OBE di bagian akhir kelas.</li>
 * <li><b>Kolom CSV berpembungkus koma</b> — {@link #getDosen()}, {@link #getMkPrasyarat()},
 * {@link #getPustaka()}, {@link #getPustakaPendukung()}.</li>
 * </ol>
 *
 * <h3>Catatan lain</h3>
 * <p>Kelas ini {@code @Audited} (Hibernate Envers), jadi setiap perubahan — termasuk perubahan yang
 * dipicu diam-diam oleh getter di atas — menghasilkan baris revisi tambahan di tabel audit.
 * Pemetaannya {@code dynamicInsert}/{@code dynamicUpdate}, sehingga hanya kolom yang benar-benar
 * berubah yang ikut di-{@code UPDATE}.</p>
 * <p>Kontrak umum id, {@code equals}, {@code hashCode}, {@code compareTo}, dan terutama helper
 * {@link GeneralValueObject#check(Object)} yang dipakai di semua getter relasi dijelaskan lengkap
 * di kelas induk — jangan diulang di sini.</p>
 *
 * @see GeneralValueObject
 * @see Kurikulum
 * @see Matakuliah
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kurikulum_punya_matakuliah")
public class KurikulumPunyaMatakuliah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Catatan: nilainya <b>identik</b> dengan {@code serialVersionUID} milik
	 * {@link Kurikulum} — jejak salin-tempel saat kelas ini dibuat, bukan sesuatu yang bermakna.
	 * Tidak berdampak fungsional (serialVersionUID hanya dicocokkan per kelas), tapi jangan
	 * dipakai sebagai penanda identitas kelas.</p>
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
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> —
	 * field lama dipertahankan. Jadi kolom ini tidak bisa dikosongkan lewat setter; ini disengaja
	 * agar interceptor audit tidak menghapus jejak yang sudah ada saat menyimpan entity yang
	 * konteks penggunanya tidak diketahui (mis. proses batch).</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong tidak berpengaruh apa pun
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong tidak berpengaruh apa pun
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini (kolom jejak audit).
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mengisi kolom jejak audit tepat sebelum baris ini di-{@code
	 * UPDATE}.
	 *
	 * <p>Mendelegasikan seluruh pekerjaannya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} dari konteks pengguna aktif.
	 * Dipanggil oleh penyedia JPA, <b>tidak pernah</b> dipanggil langsung dari kode aplikasi.</p>
	 *
	 * <p>Perhatikan tidak ada pasangan {@code @PrePersist}: saat baris <i>baru</i> disisipkan,
	 * {@code tanggal_dirubah} hanya berisi nilai inisialisasi field
	 * ({@code WaktuUtil.getDate()} pada saat object dibuat di JVM), sedangkan {@code oleh}/
	 * {@code olehId} bisa saja tetap kosong bila pemanggil tidak mengisinya.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis lewat {@link #onUpdate()}; setel manual hanya untuk migrasi data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan; sudah terisi waktu pembuatan object bila baris belum
	 *         pernah di-{@code UPDATE}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris ini: {@code id-kurikulum-matakuliah-semester-tahap}.
	 *
	 * <p><b>Awas, method ini punya efek samping.</b> Dua baris pertamanya memanggil
	 * {@link #getKurikulum()} dan {@link #getMatakuliah()}, yang lewat
	 * {@link GeneralValueObject#check(Object)} bisa memicu <i>lazy initialization</i>, pencarian di
	 * cache, bahkan pembukaan session Hibernate baru untuk memuat ulang object yang sudah
	 * <i>detached</i> — lalu menulis hasilnya kembali ke field {@code kurikulum} dan
	 * {@code matakuliah}. Artinya sekadar mencatat object ini ke log (atau menampilkannya di
	 * debugger) dapat menyentuh database. Bagian {@code kurikulum}/{@code matakuliah} pada teks
	 * hasil juga memanggil {@code toString()} kedua entity itu, sehingga rantai efek sampingnya
	 * berlanjut.</p>
	 *
	 * @return teks {@code id-kurikulum-matakuliah-semester-tahap}; bagian yang kosong tampil
	 *         sebagai {@code null}
	 */
	public String toString() {
		kurikulum = getKurikulum();
		matakuliah = getMatakuliah();
		return id + "-" + kurikulum + "-" + matakuliah + "-" + semester + "-" + tahap;
	}

	private Kurikulum kurikulum;
	private Matakuliah matakuliah;
	private Integer semester;
	private Date tanggalDitambahkan = ais.ui.util.WaktuUtil.getDate();
	private Integer tahap;
	private Integer jumlahPertemuanPerkuliahanDefault;
	private String feeder;
	private String deskripsiPembelajaran;
	private String catatan;
	private String pustaka;
	private String pustakaPendukung;
	private String mitraPengembang;
	private String mkPrasyarat;
	private String capaianPembelajaranProdi;

	private Boolean aktif;

	private Boolean inti;
	private Boolean institusional;
	private Boolean terdapatTugas;

	private KurikulumPunyaMatakuliah indukMatakuliah;

	private Date tanggalPenyusunan;
	private String pengembangRps;
	private String koordinator;
	private String dosen;
	private String rincian;
	private Tbmuser dikunci;
	private Double minimalKetercapaian;
	private Boolean nilaiMenggunakanCpmk;
	// Gap #1: bobot CPL per MK — format: "CPL-2:15,CPL-4:45,CPL-9:40"
	private String cplBobot;
	// Gap #2: pemetaan soal ujian — format per baris: "Sub CPMK 1.1|PG 1\nSub CPMK 2.1|PG 8,12"
	private String pemetaanSoalUts;
	private String pemetaanSoalUas;
	// Gap #3: komponen penilaian — format: "Kuis:10,Tugas:10,Keaktifan:10,UTS:30,UAS:40"
	private String komponenPenilaian;
	// teknik penilaian per CPMK — format tiap baris: "CPMK-1:Kuis,UTS\nCPMK-2:Tugas,UAS,Unjuk Kerja"
	private String teknikPerCpmk;
	// Rubrik Penilaian (mis. rubrik presentasi/laporan) — teks bebas per baris:
	// baris berawalan '#' = judul sub-rubrik (mis. "#Rubrik Materi Presentasi (40%)"),
	// baris lain = "Aspek|Bobot%|Skor4|Skor3|Skor2|Skor1".
	private String rubrikPenilaian;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Membuat baris kosong; {@code tanggalDitambahkan} dan {@code tanggal_dirubah} sudah terisi
	 * lewat inisialisasi field. Pemanggil wajib mengisi sendiri {@code kurikulum},
	 * {@code matakuliah}, dan {@code semester} sebelum menyimpan — {@code semester} dipetakan
	 * {@code nullable = false}.</p>
	 */
	public KurikulumPunyaMatakuliah() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id baris; {@code null} bila belum tersimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini.
	 *
	 * @param id primary key; normalnya diisi Hibernate, bukan kode aplikasi
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menyetel kurikulum pemilik penempatan ini.
	 *
	 * @param kurikulum kurikulum induk; boleh {@code null} secara skema, tetapi baris tanpa
	 *                  kurikulum praktis tidak akan pernah muncul di layar mana pun
	 */
	public void setKurikulum(Kurikulum kurikulum) {
		this.kurikulum = kurikulum;
	}

	/**
	 * Mengembalikan kurikulum pemilik penempatan ini.
	 *
	 * <p>Relasi {@code LAZY}; getter memakai pola standar {@link GeneralValueObject#check(Object)}
	 * lebih dulu, sehingga aman dipanggil pada entity yang sudah <i>detached</i>. Hasil
	 * {@code check()} ditugaskan kembali ke field karena instance yang dikembalikan bisa berbeda
	 * dari proxy semula.</p>
	 *
	 * @return kurikulum induk, atau {@code null} bila kolomnya memang kosong
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kurikulum", nullable = true)
	public Kurikulum getKurikulum() {
		kurikulum = check(kurikulum);
		return kurikulum;
	}

	/**
	 * Menyetel mata kuliah yang ditempatkan pada kurikulum ini.
	 *
	 * @param matakuliah definisi mata kuliah; boleh {@code null} secara skema, tetapi sejumlah
	 *                   method di kelas ini (mis. {@link #populateRinci(JSONObject)}) akan
	 *                   melempar {@code NullPointerException} bila dibiarkan kosong
	 */
	public void setMatakuliah(Matakuliah matakuliah) {
		this.matakuliah = matakuliah;
	}

	/**
	 * Mengembalikan definisi mata kuliah yang ditempatkan di kurikulum ini.
	 *
	 * <p>Sumber kode, nama, dan bobot SKS — baris ini sengaja tidak menyalinnya. Pola resolusi
	 * lazy sama dengan {@link #getKurikulum()}.</p>
	 *
	 * @return mata kuliah terkait, atau {@code null} bila kolomnya kosong
	 * @see Matakuliah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah", nullable = true)
	public Matakuliah getMatakuliah() {
		matakuliah = check(matakuliah);
		return matakuliah;
	}

	/**
	 * Menyetel waktu mata kuliah ini dimasukkan ke kurikulum.
	 *
	 * @param tanggalDitambahkan stempel waktu penambahan; kolomnya {@code nullable = false},
	 *                           jadi jangan diisi {@code null}
	 */
	public void setTanggalDitambahkan(Date tanggalDitambahkan) {
		this.tanggalDitambahkan = tanggalDitambahkan;
	}

	/**
	 * Mengembalikan waktu mata kuliah ini dimasukkan ke kurikulum.
	 *
	 * <p>Sudah terisi otomatis saat object dibuat (inisialisasi field), sehingga baris baru selalu
	 * punya nilai meski pemanggil tidak menyetelnya.</p>
	 *
	 * @return stempel waktu penambahan; tidak pernah {@code null} untuk object yang dibuat lewat
	 *         konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditambahkan", nullable = false)
	public Date getTanggalDitambahkan() {
		return tanggalDitambahkan;
	}

	/**
	 * Menyetel semester penempatan mata kuliah ini.
	 *
	 * @param semester nomor semester (1..n) tempat mata kuliah ditawarkan; kolomnya
	 *                 {@code nullable = false}
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan semester keberapa mata kuliah ini ditawarkan dalam kurikulum.
	 *
	 * <p>Inilah atribut inti yang membedakan kelas ini dari join table biasa: nomor semester
	 * paket/kurikulum (bukan semester berjalan mahasiswa). Dipakai untuk menyusun paket KRS,
	 * cetakan struktur kurikulum, dan validasi pengambilan mata kuliah di luar paket.</p>
	 *
	 * <p>Getter murni tanpa normalisasi: kolomnya {@code nullable = false} di pemetaan, tetapi
	 * data lama yang masuk lewat jalur lain tetap bisa mengembalikan {@code null} — beberapa
	 * pemanggil memang memeriksanya.</p>
	 *
	 * @return nomor semester penempatan
	 */
	@Column(name = "semester", nullable = false)
	public Integer getSemester() {
		return semester;
	}

	/**
	 * Mengembalikan id Feeder PDDikti untuk penempatan ini.
	 *
	 * <p><b>Getter dengan efek samping.</b> Bila {@link #getKurikulum()} dan
	 * {@link #getMatakuliah()} sama-sama berhasil diresolusi, field {@code feeder}
	 * <b>ditimpa</b> dengan gabungan {@code kurikulum.feeder + "-" + matakuliah.feeder}; nilai
	 * yang tersimpan di database diabaikan dan akan ikut ter-{@code UPDATE} pada flush
	 * berikutnya. Jadi id Feeder baris ini selalu turunan dari id Feeder kedua induknya, bukan
	 * identitas mandiri.</p>
	 *
	 * <p>Perhatikan bentuk hasilnya tetap {@code "x-y"} walau salah satu bagian kosong — mis.
	 * {@code "null-null"} bila kedua induk belum punya id Feeder. Pemanggil yang mengirim nilai
	 * ini ke web service Feeder sebaiknya memeriksa isinya lebih dulu.</p>
	 *
	 * @return id Feeder gabungan yang sudah di-trim, atau {@code null} bila kosong seluruhnya
	 */
	public String getFeeder() {
		if (getKurikulum() != null && getMatakuliah() != null) {
			feeder = getKurikulum().getFeeder() + "-" + getMatakuliah().getFeeder();
		}
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menyetel id Feeder penempatan ini.
	 *
	 * <p>Nyaris tidak berguna dipanggil dari kode aplikasi: {@link #getFeeder()} akan menimpanya
	 * lagi begitu kedua relasi induk bisa diresolusi.</p>
	 *
	 * @param feeder id Feeder; boleh {@code null}
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Mengembalikan tahap kurikulum tempat mata kuliah ini berada.
	 *
	 * <p>Fitur opsional yang hanya aktif bila {@code ConstantValues.aktifkanTahapanKurikulum}
	 * bernilai {@code true}; dipakai institusi yang membagi kurikulum menjadi beberapa tahap
	 * (mis. tahap akademik dan tahap profesi pada program kedokteran/kesehatan). Bila fitur mati,
	 * kolom ini dibiarkan {@code null} dan tidak ditampilkan di layar mana pun.</p>
	 *
	 * @return nomor tahap, atau {@code null} bila tidak dipakai
	 */
	public Integer getTahap() {
		return tahap;
	}

	/**
	 * Menyetel tahap kurikulum tempat mata kuliah ini berada.
	 *
	 * @param tahap nomor tahap; {@code null} berarti tanpa tahap
	 */
	public void setTahap(Integer tahap) {
		this.tahap = tahap;
	}

	/**
	 * Mengembalikan deskripsi pembelajaran mata kuliah ini <i>untuk kurikulum ini</i>.
	 *
	 * <p><b>Getter dengan efek samping — pewarisan yang dimaterialisasi.</b> Bila kolom di baris
	 * ini masih kosong dan {@link Matakuliah#getDeskripsiPembelajaran()} berisi sesuatu, isi milik
	 * mata kuliah <b>disalin ke field baris ini</b>, sehingga akan tersimpan ke database pada
	 * flush berikutnya. Sejak saat itu baris ini punya salinannya sendiri: <b>perubahan deskripsi
	 * di {@link Matakuliah} tidak lagi ikut terbawa</b>. Perilaku ini disengaja (RPS milik
	 * kurikulum, bukan milik mata kuliah), tapi berarti sekadar membuka layar RPS sudah cukup
	 * untuk "membekukan" warisan itu.</p>
	 *
	 * <p>Baris pertama juga menulis balik field {@code matakuliah} lewat {@link #getMatakuliah()}.
	 * Seluruh badan pewarisan dibungkus {@code try/catch} sehingga {@code matakuliah} yang
	 * {@code null} tidak meledak, hanya membuat pewarisan dilewati.</p>
	 *
	 * @return deskripsi pembelajaran ter-trim; string kosong bila belum terisi dan tidak ada yang
	 *         bisa diwarisi, tidak pernah {@code null}
	 * @see Matakuliah#getDeskripsiPembelajaran()
	 */
	@Column(columnDefinition = "text")
	public String getDeskripsiPembelajaran() {
		matakuliah = getMatakuliah();
		try {

			if ((deskripsiPembelajaran == null || deskripsiPembelajaran.trim().isEmpty())
					&& !matakuliah.getDeskripsiPembelajaran().isEmpty()) {
				deskripsiPembelajaran = matakuliah.getDeskripsiPembelajaran();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KurikulumPunyaMatakuliah.java:229");
			// TODO: handle exception
		}

		return deskripsiPembelajaran == null ? "" : deskripsiPembelajaran.trim();
	}

	/**
	 * Menyetel deskripsi pembelajaran khusus kurikulum ini.
	 *
	 * @param deskripsiPembelajaran teks deskripsi; {@code null}/kosong berarti kembali mewarisi
	 *                              dari {@link Matakuliah} pada pembacaan berikutnya
	 */
	public void setDeskripsiPembelajaran(String deskripsiPembelajaran) {
		this.deskripsiPembelajaran = deskripsiPembelajaran;
	}

	/**
	 * Mengembalikan capaian pembelajaran tingkat prodi untuk penempatan ini.
	 *
	 * <p>Mekanismenya identik dengan {@link #getDeskripsiPembelajaran()}: bila kosong, isi
	 * diwarisi dari {@link Matakuliah#getCapaianPembelajaranProdi()} dan <b>dimaterialisasi</b>
	 * menjadi milik baris ini (ikut tersimpan pada flush berikutnya, dan setelah itu tidak lagi
	 * mengikuti perubahan di mata kuliah).</p>
	 *
	 * <p>Ini teks bebas, bukan daftar id — jangan tertukar dengan CSV id CPL pada
	 * {@link Matakuliah#getCapaianPembelajaranLulusan()} yang dipakai
	 * {@link #populateRinci(JSONObject)}.</p>
	 *
	 * @return capaian pembelajaran prodi ter-trim; string kosong bila tidak ada, tidak pernah
	 *         {@code null}
	 * @see #getDeskripsiPembelajaran()
	 */
	@Column(columnDefinition = "text")
	public String getCapaianPembelajaranProdi() {
		matakuliah = getMatakuliah();
		try {
			if ((capaianPembelajaranProdi == null || capaianPembelajaranProdi.trim().isEmpty())
					&& !matakuliah.getCapaianPembelajaranProdi().isEmpty()) {
				capaianPembelajaranProdi = matakuliah.getCapaianPembelajaranProdi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KurikulumPunyaMatakuliah.java:248");
			// TODO: handle exception
		}

		return capaianPembelajaranProdi == null ? "" : capaianPembelajaranProdi.trim();
	}

	/**
	 * Menyetel capaian pembelajaran tingkat prodi khusus kurikulum ini.
	 *
	 * @param capaianPembelajaranProdi teks capaian; {@code null}/kosong berarti kembali mewarisi
	 *                                 dari {@link Matakuliah}
	 */
	public void setCapaianPembelajaranProdi(String capaianPembelajaranProdi) {
		this.capaianPembelajaranProdi = capaianPembelajaranProdi;
	}

	/**
	 * Mengembalikan jumlah pertemuan perkuliahan bawaan untuk mata kuliah ini di kurikulum ini.
	 *
	 * <p><b>Getter dengan efek samping — pengisian malas dari konfigurasi global.</b> Bila kolomnya
	 * masih {@code null}, nilainya diambil dari konfigurasi
	 * {@code jumlah_pertemuan_perkuliahan_default} (bawaan {@code 16}) lewat
	 * {@link Common#getKonfigurasi(String, String)}, lalu <b>ditulis ke field</b> sehingga ikut
	 * tersimpan ke database pada flush berikutnya. Dua konsekuensi yang gampang mengejutkan:</p>
	 * <ul>
	 * <li>Baris yang tadinya "mengikuti konfigurasi global" berubah menjadi "punya angka sendiri"
	 * hanya karena pernah dibaca. Mengubah konfigurasi global setelah itu tidak lagi berpengaruh
	 * pada baris tersebut.</li>
	 * <li>{@link Common#getKonfigurasi(String, String)} sendiri <b>menuliskan nilai bawaan ke tabel
	 * konfigurasi</b> bila kuncinya belum ada — jadi pembacaan pertama juga bisa menambah baris
	 * konfigurasi baru di database.</li>
	 * </ul>
	 * <p>Kegagalan parsing angka ditelan diam-diam; nilai {@code 16} tetap dipakai.</p>
	 *
	 * @return jumlah pertemuan bawaan; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	public Integer getJumlahPertemuanPerkuliahanDefault() {
		if (jumlahPertemuanPerkuliahanDefault == null) {
			int jumlahPertemuanDefault = 16;
			try {
				jumlahPertemuanDefault = Integer
						.parseInt(Common.getKonfigurasi("jumlah_pertemuan_perkuliahan_default", "16").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KurikulumPunyaMatakuliah.java:265");

			}
			jumlahPertemuanPerkuliahanDefault = jumlahPertemuanDefault;
		}
		return jumlahPertemuanPerkuliahanDefault;
	}

	/**
	 * Menyetel jumlah pertemuan perkuliahan bawaan.
	 *
	 * @param jumlahPertemuanPerkuliahanDefault jumlah pertemuan; {@code null} mengembalikan baris
	 *                                          ini ke perilaku "ambil dari konfigurasi global"
	 *                                          sampai getter dipanggil lagi
	 */
	public void setJumlahPertemuanPerkuliahanDefault(Integer jumlahPertemuanPerkuliahanDefault) {
		this.jumlahPertemuanPerkuliahanDefault = jumlahPertemuanPerkuliahanDefault;
	}

	/**
	 * Mengembalikan baris induk bila mata kuliah ini adalah modul/sub mata kuliah.
	 *
	 * <p>Foreign key <b>ke tabel ini sendiri</b> ({@code induk_matakuliah}), membentuk hierarki
	 * satu tingkat di dalam satu kurikulum: mata kuliah besar sebagai induk, modul-modulnya sebagai
	 * anak. Diisi dari layar pemilihan mata kuliah kurikulum
	 * ({@code AmbilDataMatakuliahKurikulumHelper}, {@code DetailSemesterKurikulumHelper}).</p>
	 *
	 * <p>Tidak ada penjagaan siklus di lapisan entity: secara teknis baris bisa saja menunjuk
	 * dirinya sendiri atau membentuk lingkaran. Pemanggil yang menelusuri hierarki ke atas
	 * sebaiknya membatasi kedalaman. Resolusi lazy memakai pola {@link #getKurikulum()}.</p>
	 *
	 * @return baris induk, atau {@code null} bila mata kuliah ini bukan modul
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "induk_matakuliah", nullable = true)
	public KurikulumPunyaMatakuliah getIndukMatakuliah() {
		indukMatakuliah = check(indukMatakuliah);
		return indukMatakuliah;
	}

	/**
	 * Menyetel baris induk untuk mata kuliah modul.
	 *
	 * @param indukMatakuliah penempatan mata kuliah induk; {@code null} berarti bukan modul
	 */
	public void setIndukMatakuliah(KurikulumPunyaMatakuliah indukMatakuliah) {
		this.indukMatakuliah = indukMatakuliah;
	}

	/**
	 * Menyatakan apakah mata kuliah ini termasuk <b>kurikulum inti</b> (nasional).
	 *
	 * @return {@code true} bila mata kuliah inti; <b>{@code false}</b> bila kolomnya masih
	 *         {@code null} (bawaan). Tidak pernah {@code null}
	 * @see #getInstitusional()
	 */
	public Boolean getInti() {
		return inti == null ? false : inti;
	}

	/**
	 * Menyetel penanda mata kuliah kurikulum inti.
	 *
	 * @param inti {@code true} bila inti; {@code null} dibaca sebagai {@code false}
	 */
	public void setInti(Boolean inti) {
		this.inti = inti;
	}

	/**
	 * Menyatakan apakah mata kuliah ini termasuk <b>kurikulum institusional</b> (muatan lokal
	 * perguruan tinggi).
	 *
	 * <p>Perhatikan bawaannya berlawanan dengan {@link #getInti()}: baris baru yang belum diisi
	 * apa pun akan terbaca sebagai institusional dan bukan inti.</p>
	 *
	 * @return {@code true} bila institusional; <b>{@code true}</b> pula bila kolomnya masih
	 *         {@code null} (bawaan). Tidak pernah {@code null}
	 */
	public Boolean getInstitusional() {
		return institusional == null ? true : institusional;
	}

	/**
	 * Menyetel penanda mata kuliah kurikulum institusional.
	 *
	 * @param institusional {@code true} bila institusional; {@code null} dibaca sebagai
	 *                      {@code true}
	 */
	public void setInstitusional(Boolean institusional) {
		this.institusional = institusional;
	}

	/**
	 * Menyatakan apakah mata kuliah ini memuat komponen tugas.
	 *
	 * <p>Dipakai layar penilaian untuk memutuskan apakah kolom nilai tugas ditampilkan dan
	 * diperhitungkan.</p>
	 *
	 * @return {@code true} bila ada tugas; {@code true} pula sebagai bawaan saat kolom masih
	 *         {@code null}
	 */
	public Boolean getTerdapatTugas() {
		return terdapatTugas == null ? true : terdapatTugas;
	}

	/**
	 * Menyetel penanda adanya komponen tugas.
	 *
	 * @param terdapatTugas {@code true} bila ada tugas; {@code null} dibaca sebagai {@code true}
	 */
	public void setTerdapatTugas(Boolean terdapatTugas) {
		this.terdapatTugas = terdapatTugas;
	}

	/**
	 * Menyatakan apakah penempatan ini masih berlaku.
	 *
	 * <p>Penonaktifan dipakai sebagai pengganti penghapusan baris: mata kuliah yang dicabut dari
	 * kurikulum tetap disimpan agar KRS/transkrip angkatan lama yang menunjuk ke sini tidak putus
	 * relasinya.</p>
	 *
	 * @return {@code true} bila aktif; {@code true} pula sebagai bawaan saat kolom masih
	 *         {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif penempatan ini.
	 *
	 * @param aktif {@code true} bila masih dipakai; {@code null} dibaca sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tanggal penyusunan RPS (hanya tanggal, tanpa jam).
	 *
	 * <p>Data administratif untuk kop cetakan RPS; tidak dipakai logika apa pun.</p>
	 *
	 * @return tanggal penyusunan RPS, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalPenyusunan() {
		return tanggalPenyusunan;
	}

	/**
	 * Menyetel tanggal penyusunan RPS.
	 *
	 * @param tanggalPenyusunan tanggal penyusunan; boleh {@code null}
	 */
	public void setTanggalPenyusunan(Date tanggalPenyusunan) {
		this.tanggalPenyusunan = tanggalPenyusunan;
	}

	/**
	 * Mengembalikan nama pengembang/penyusun RPS (teks bebas, kolom {@code text}).
	 *
	 * <p>Teks bebas yang diketik pengguna, bukan relasi ke {@link Dosen} — bedakan dari
	 * {@link #getDosen()} yang berisi CSV id dosen. Normalisasi hanya pada nilai kembali, field
	 * tidak diubah.</p>
	 *
	 * @return nama pengembang RPS; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getPengembangRps() {
		return pengembangRps == null ? "" : pengembangRps;
	}

	/**
	 * Menyetel nama pengembang/penyusun RPS.
	 *
	 * @param pengembangRps nama penyusun; boleh {@code null}
	 */
	public void setPengembangRps(String pengembangRps) {
		this.pengembangRps = pengembangRps;
	}

	/**
	 * Mengembalikan nama koordinator mata kuliah (teks bebas, kolom {@code text}).
	 *
	 * <p>Sama seperti {@link #getPengembangRps()}: teks bebas untuk kop RPS, bukan relasi.</p>
	 *
	 * @return nama koordinator; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKoordinator() {
		return koordinator == null ? "" : koordinator;
	}

	/**
	 * Menyetel nama koordinator mata kuliah.
	 *
	 * @param koordinator nama koordinator; boleh {@code null}
	 */
	public void setKoordinator(String koordinator) {
		this.koordinator = koordinator;
	}

	/**
	 * Mengembalikan daftar id pustaka utama sebagai CSV berpembungkus koma.
	 *
	 * <p>Isinya id {@link ais.database.model.obe.ReferensiLulusan} (master referensi/pustaka), bukan
	 * judul buku; penerjemahan id &rarr; judul dilakukan pemanggil, mis.
	 * {@code RpsObeAction.ambilReferensiList(String)}.</p>
	 *
	 * <p><b>Getter dengan efek samping.</b> Algoritmanya sama persis dengan
	 * {@link Matakuliah#getCapaianPembelajaranLulusan()} dan dipakai berulang di kelas ini
	 * ({@link #getPustakaPendukung()}, {@link #getDosen()}, {@link #getMkPrasyarat()}):</p>
	 * <ol>
	 * <li>bungkus isi dengan koma di kedua ujung, lalu rapatkan koma ganda tiga kali berturut-turut;</li>
	 * <li>bila hasilnya hanya berisi koma, jadikan string kosong;</li>
	 * <li>pecah dengan {@code ","}, masukkan ke {@link HashSet} untuk membuang duplikat, lalu
	 * gabungkan kembali;</li>
	 * <li>tulis hasilnya <b>kembali ke field</b> (tanpa pembungkus koma), dan kembalikan versi
	 * yang dibungkus koma.</li>
	 * </ol>
	 * <p>Tiga akibat yang perlu diingat:</p>
	 * <ul>
	 * <li><b>Urutan pustaka hilang</b> — {@link HashSet} tidak menjaga urutan, sehingga urutan
	 * daftar pustaka di cetakan RPS bisa berubah-ubah setiap kali disimpan ulang.</li>
	 * <li><b>Isi field &ne; nilai kembali</b> — field menyimpan bentuk tanpa pembungkus
	 * ({@code "12,45"}), nilai kembali memakai pembungkus ({@code ",12,45,"}). Pencarian dengan
	 * {@code Restrictions.like("pustaka", "%,12,%")} karena itu tidak dapat diandalkan.</li>
	 * <li>Daftar kosong dikembalikan sebagai {@code ",,"}, bukan string kosong.</li>
	 * </ul>
	 * <p>Beberapa cabang di badan method ini <b>tidak pernah tercapai</b> (pemeriksaan
	 * {@code ",,"}/{@code ",,,"}/{@code ",,,,"} setelah koma ganda sudah dirapatkan, dan
	 * pemeriksaan {@code null} setelah field pasti terisi) — sisa salin-tempel; dibiarkan apa
	 * adanya karena tidak berbahaya.</p>
	 *
	 * @return CSV id pustaka utama berpembungkus koma; {@code ",,"} bila kosong, tidak pernah
	 *         {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getPustaka() {
		pustaka = (pustaka == null || pustaka.trim().equalsIgnoreCase(",") ? "" : "," + pustaka.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (pustaka.equals(",")) {
			pustaka = "";
		} else if (pustaka.equals(",,")) {
			pustaka = "";
		} else if (pustaka.equals(",,,")) {
			pustaka = "";
		} else if (pustaka.equals(",,,,")) {
			pustaka = "";
		}
		if (pustaka != null && !pustaka.trim().isEmpty()) {
			Set<String> strings = new HashSet<String>(Arrays.asList(pustaka.split(",")));
			pustaka = "";
			for (String s : strings) {
				if (!s.trim().isEmpty()) {
					pustaka += pustaka.isEmpty() ? s : "," + s;
				}
			}
		}
		return pustaka == null ? "" : "," + pustaka.trim() + ",";
	}

	/**
	 * Menyetel CSV id pustaka utama.
	 *
	 * @param pustaka CSV id referensi, dengan atau tanpa pembungkus koma; akan dinormalkan pada
	 *                pembacaan berikutnya
	 */
	public void setPustaka(String pustaka) {
		this.pustaka = pustaka;
	}

	/**
	 * Mengembalikan daftar id pustaka pendukung sebagai CSV berpembungkus koma.
	 *
	 * <p>Isi, algoritma normalisasi, efek samping penulisan balik, hilangnya urutan, dan bentuk
	 * {@code ",,"} untuk daftar kosong sama persis dengan {@link #getPustaka()} — bedanya hanya
	 * kolom yang dipakai.</p>
	 *
	 * @return CSV id pustaka pendukung berpembungkus koma; {@code ",,"} bila kosong
	 * @see #getPustaka()
	 */
	@Column(columnDefinition = "text")
	public String getPustakaPendukung() {
		pustakaPendukung = (pustakaPendukung == null || pustakaPendukung.trim().equalsIgnoreCase(",") ? ""
				: "," + pustakaPendukung.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (pustakaPendukung.equals(",")) {
			pustakaPendukung = "";
		} else if (pustakaPendukung.equals(",,")) {
			pustakaPendukung = "";
		} else if (pustakaPendukung.equals(",,,")) {
			pustakaPendukung = "";
		} else if (pustakaPendukung.equals(",,,,")) {
			pustakaPendukung = "";
		}
		if (pustakaPendukung != null && !pustakaPendukung.trim().isEmpty()) {
			Set<String> strings = new HashSet<String>(Arrays.asList(pustakaPendukung.split(",")));
			pustakaPendukung = "";
			for (String s : strings) {
				if (!s.trim().isEmpty()) {
					pustakaPendukung += pustakaPendukung.isEmpty() ? s : "," + s;
				}
			}
		}
		return pustakaPendukung == null ? "" : "," + pustakaPendukung.trim() + ",";
	}

	/**
	 * Menyetel CSV id pustaka pendukung.
	 *
	 * @param pustakaPendukung CSV id referensi; akan dinormalkan pada pembacaan berikutnya
	 */
	public void setPustakaPendukung(String pustakaPendukung) {
		this.pustakaPendukung = pustakaPendukung;
	}

	/**
	 * Mengembalikan daftar id {@link Dosen} pengampu/penyusun RPS sebagai CSV berpembungkus koma.
	 *
	 * <p>Ini <b>daftar id</b>, berbeda dari {@link #getPengembangRps()} dan
	 * {@link #getKoordinator()} yang berupa nama teks bebas. Layar RPS mengisinya lewat dialog
	 * pemilihan dosen dan mengujinya dengan {@code contains("," + id + ",")}.</p>
	 *
	 * <p>Algoritma normalisasi, efek samping penulisan balik, hilangnya urutan, dan bentuk
	 * {@code ",,"} untuk daftar kosong sama persis dengan {@link #getPustaka()}.</p>
	 *
	 * @return CSV id dosen berpembungkus koma; {@code ",,"} bila kosong
	 * @see #getPustaka()
	 */
	@Column(name = "dosen", nullable = true, columnDefinition = "text")
	public String getDosen() {

		dosen = (dosen == null || dosen.trim().equalsIgnoreCase(",") ? "" : "," + dosen.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (dosen.equals(",")) {
			dosen = "";
		} else if (dosen.equals(",,")) {
			dosen = "";
		} else if (dosen.equals(",,,")) {
			dosen = "";
		} else if (dosen.equals(",,,,")) {
			dosen = "";
		}
		if (dosen != null && !dosen.trim().isEmpty()) {
			Set<String> strings = new HashSet<String>(Arrays.asList(dosen.split(",")));
			dosen = "";
			for (String s : strings) {
				if (!s.trim().isEmpty()) {
					dosen += dosen.isEmpty() ? s : "," + s;
				}
			}
		}
		return dosen == null ? "" : "," + dosen.trim() + ",";
	}

	/**
	 * Menyetel CSV id dosen pengampu/penyusun RPS.
	 *
	 * @param dosen CSV id {@link Dosen}; akan dinormalkan pada pembacaan berikutnya
	 */
	public void setDosen(String dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan nama mitra pengembang RPS (teks bebas, kolom {@code text}).
	 *
	 * <p>Diisi bila RPS disusun bersama mitra industri/institusi lain (relevan untuk borang
	 * akreditasi). Bebas efek samping.</p>
	 *
	 * @return nama mitra pengembang; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getMitraPengembang() {
		return mitraPengembang == null ? "" : mitraPengembang;
	}

	/**
	 * Menyetel nama mitra pengembang RPS.
	 *
	 * @param mitraPengembang nama mitra; boleh {@code null}
	 */
	public void setMitraPengembang(String mitraPengembang) {
		this.mitraPengembang = mitraPengembang;
	}

	/**
	 * Mengembalikan daftar mata kuliah prasyarat sebagai CSV id berpembungkus koma.
	 *
	 * <p><b>Perhatikan tipe id-nya:</b> yang disimpan adalah id {@link Matakuliah} (definisi mata
	 * kuliah), <b>bukan</b> id baris {@code KurikulumPunyaMatakuliah}. Artinya prasyarat dinyatakan
	 * pada tingkat mata kuliah, sehingga tetap terbaca walau mata kuliah prasyaratnya tidak
	 * ditempatkan di kurikulum yang sama.</p>
	 *
	 * <p>Inilah tempat relasi prasyarat disimpan — {@link Matakuliah} sendiri tidak menyimpan
	 * daftar prasyarat sebagai field. Konsekuensinya prasyarat bisa berbeda antar kurikulum untuk
	 * mata kuliah yang sama.</p>
	 *
	 * <p>Algoritma normalisasi, efek samping penulisan balik, hilangnya urutan, dan bentuk
	 * {@code ",,"} untuk daftar kosong sama persis dengan {@link #getPustaka()}.</p>
	 *
	 * @return CSV id mata kuliah prasyarat berpembungkus koma; {@code ",,"} bila kosong
	 * @see #getPustaka()
	 */
	@Column(columnDefinition = "text")
	public String getMkPrasyarat() {
		mkPrasyarat = (mkPrasyarat == null || mkPrasyarat.trim().equalsIgnoreCase(",") ? ""
				: "," + mkPrasyarat.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (mkPrasyarat.equals(",")) {
			mkPrasyarat = "";
		} else if (mkPrasyarat.equals(",,")) {
			mkPrasyarat = "";
		} else if (mkPrasyarat.equals(",,,")) {
			mkPrasyarat = "";
		} else if (mkPrasyarat.equals(",,,,")) {
			mkPrasyarat = "";
		}
		if (mkPrasyarat != null && !mkPrasyarat.trim().isEmpty()) {
			Set<String> strings = new HashSet<String>(Arrays.asList(mkPrasyarat.split(",")));
			mkPrasyarat = "";
			for (String s : strings) {
				if (!s.trim().isEmpty()) {
					mkPrasyarat += mkPrasyarat.isEmpty() ? s : "," + s;
				}
			}
		}
		return mkPrasyarat == null ? "" : "," + mkPrasyarat.trim() + ",";
	}

	/**
	 * Menyetel CSV id mata kuliah prasyarat.
	 *
	 * @param mkPrasyarat CSV id {@link Matakuliah}; akan dinormalkan pada pembacaan berikutnya
	 */
	public void setMkPrasyarat(String mkPrasyarat) {
		this.mkPrasyarat = mkPrasyarat;
	}

	/**
	 * Nilai bawaan kolom {@code rincian}: teks JSON object kosong ({@code "{}"}).
	 *
	 * <p>Namanya {@code ARRAY} tetapi isinya JSON <i>object</i>, bukan array — penamaan yang
	 * menyesatkan tapi konsisten dengan cara {@code rincian} dipakai: strukturnya memang map
	 * berkunci teks, dibaca dengan {@code new JSONObject(getRincian())}.</p>
	 */
	private final static String ARRAY = new JSONObject().toString();

	/**
	 * Mengembalikan agenda mingguan RPS dalam bentuk teks JSON mentah.
	 *
	 * <p>Strukturnya adalah JSON object yang setiap nilainya mendeskripsikan satu baris RPS
	 * (rentang minggu, sub-CPMK, indikator, materi, metode, bobot). Kunci luar berupa id baris
	 * yang dihasilkan UI dan tidak punya makna urutan — pengurutan dilakukan
	 * {@link #populateRinci(JSONObject)} berdasarkan {@code mulaiMingguKe}.</p>
	 *
	 * <p>Kolom ini <b>tidak divalidasi</b> di lapisan entity: isinya bebas selama masih JSON yang
	 * bisa diurai pemanggil. Bebas efek samping — field tidak ditulis balik.</p>
	 *
	 * @return teks JSON agenda RPS; {@code "{}"} bila belum ada isi, tidak pernah {@code null}
	 * @see #populateRinci(JSONObject)
	 */
	@Column(columnDefinition = "text")
	public String getRincian() {
		return rincian == null || rincian.trim().isEmpty() ? ARRAY : rincian;
	}

	/**
	 * Menyetel agenda mingguan RPS dalam bentuk teks JSON mentah.
	 *
	 * @param rincian teks JSON; {@code null}/kosong akan dibaca sebagai {@code "{}"}
	 */
	public void setRincian(String rincian) {
		this.rincian = rincian;
	}

	/**
	 * Mencari satu baris RPS yang rentang minggunya memuat pertemuan ke-{@code mingguke}.
	 *
	 * <p>Membangun ulang seluruh agenda lewat {@link #populateRinci(JSONObject)}, lalu menelusuri
	 * hasilnya secara berurutan dan mengembalikan map pertama yang memenuhi
	 * {@code mulaiMingguKe <= mingguke <= sampaiMingguKe}. Bentuk map yang dikembalikan sama
	 * dengan elemen keluaran {@code populateRinci}: berisi kunci {@code "jsonObject"} (baris RPS
	 * mentah), {@code "subCpmk"}, dan {@code "capaianPembelajaranLulusanData"}.</p>
	 *
	 * <p><b>Biaya:</b> method ini <b>tidak murah</b> — setiap pemanggilan menjalankan ulang seluruh
	 * {@code populateRinci}, termasuk membuka session Hibernate baru dan melakukan query CPL.
	 * Memanggilnya di dalam perulangan per pertemuan (pola yang muncul di
	 * {@code DashboardTimelinePertemuan}) berarti satu query per minggu. Bila butuh banyak minggu
	 * sekaligus, panggil {@code populateRinci} sekali lalu telusuri sendiri hasilnya.</p>
	 *
	 * <p>Seluruh kesalahan per baris ditelan (hanya dicetak ke {@code stderr} dan dicatat audit),
	 * sehingga baris RPS yang rusak formatnya dilewati tanpa menggagalkan pencarian.</p>
	 *
	 * @param jsonArraykurikulumPunyaMatakuliah agenda RPS terurai, biasanya
	 *                                          {@code new JSONObject(getRincian())}
	 * @param mingguke                          nomor pertemuan/minggu yang dicari (berbasis 1)
	 * @return map baris RPS yang cocok, atau {@code null} bila tidak ada rentang yang memuat
	 *         minggu itu
	 * @see #populateRinci(JSONObject)
	 */
	@SuppressWarnings("rawtypes")
	public Map ambilRinci(JSONObject jsonArraykurikulumPunyaMatakuliah, int mingguke) {
		TreeMap<Integer, Map> maps = populateRinci(jsonArraykurikulumPunyaMatakuliah);
		for (Map map : maps.values()) {
			try {
				JSONObject jsonObject = (JSONObject) map.get("jsonObject");
				JSONObject subCpmk = (JSONObject) map.get("subCpmk");
				CapaianPembelajaranLulusan capaianPembelajaranLulusanData = (CapaianPembelajaranLulusan) map
						.get("capaianPembelajaranLulusanData");
				if (subCpmk != null && capaianPembelajaranLulusanData != null) {
					int mulaiMingguKe = jsonObject.getInt("mulaiMingguKe");
					int sampaiMingguKe = jsonObject.getInt("sampaiMingguKe");

					if (mingguke >= mulaiMingguKe && mingguke <= sampaiMingguKe) {
						return map;
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/KurikulumPunyaMatakuliah.java:507");
			}
		}
		return null;
	}

	/**
	 * Menyusun agenda RPS terurut per minggu, lengkap dengan data CPL/sub-CPMK yang sudah
	 * dicocokkan.
	 *
	 * <p>Ini <b>method bisnis utama</b> kelas ini dan satu-satunya yang menyentuh database secara
	 * langsung. Dipakai oleh hampir semua layar OBE: {@code RpsObeAction} (penyuntingan &amp;
	 * cetak RPS), {@code NilaiObeAction} (perhitungan ketercapaian CPL),
	 * {@code PenjadwalanHelper}, dan dasbor {@code DashboardTimelinePertemuan}.</p>
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 * <li><b>Saring &amp; urutkan baris RPS.</b> Setiap nilai di {@code jsonArray...} diambil
	 * sebagai satu baris RPS; baris yang tidak punya {@code mulaiMingguKe}, {@code sampaiMingguKe},
	 * atau {@code sub_cpmk} dibuang. Kunci luarnya disimpan kembali ke dalam baris sebagai
	 * {@code "keyData"} agar pemanggil bisa menulis balik ke JSON aslinya. Pengurutan dilakukan
	 * dengan membangun kunci numerik {@code mulaiMingguKe} &times; 100000 + nomor urut
	 * kemunculan, sehingga baris terurut per minggu dan urutan aslinya tetap terjaga untuk
	 * minggu yang sama.</li>
	 * <li><b>Kumpulkan id CPL.</b> Diambil dari
	 * {@link Matakuliah#getCapaianPembelajaranLulusan()} — CSV id berpembungkus koma milik
	 * <i>mata kuliah</i>, bukan milik baris ini.</li>
	 * <li><b>Muat CPL dari database.</b> Query {@code Criteria} atas
	 * {@link ais.database.model.obe.CapaianPembelajaranLulusan}, dibatasi pada id yang terkumpul
	 * dan hanya yang aktif ({@code aktif is null or aktif = true}), diurutkan kode lalu nama. Bila
	 * tidak ada id sama sekali, dipakai {@code Restrictions.sqlRestriction("false")} sehingga
	 * query pasti mengembalikan daftar kosong (bukan seluruh tabel).</li>
	 * <li><b>Cocokkan tiap baris RPS dengan CPL/sub-CPMK-nya.</b>
	 *   <ul>
	 *   <li>Nilai {@code sub_cpmk} berupa {@code "-1"} atau kosong berarti baris "bebas" (mis.
	 *   minggu UTS/UAS): dipasangkan dengan {@link CapaianPembelajaranLulusan} <b>kosong hasil
	 *   {@code new}</b> — bukan entity dari database — agar pemanggil tetap mendapat object
	 *   non-null.</li>
	 *   <li>Bila {@link #getNilaiMenggunakanCpmk()} bernilai {@code true}, {@code sub_cpmk}
	 *   dianggap langsung berisi id CPL dan dicocokkan apa adanya.</li>
	 *   <li>Bila tidak, pencocokan dilakukan ke dalam {@code formula} milik tiap CPL (sebuah
	 *   {@link JSONArray}): kunci baris disusun sebagai {@code key + "_" + idCpl} pada sisi CPL
	 *   dan {@code sub_cpmk (+ "_" + idCpl bila belum mengandung "_")} pada sisi baris RPS, lalu
	 *   dibandingkan tanpa membedakan besar-kecil huruf. Kompatibilitas dua bentuk kunci ini
	 *   ada karena data lama menyimpan {@code sub_cpmk} tanpa akhiran id CPL.</li>
	 *   </ul>
	 * </li>
	 * </ol>
	 *
	 * <h4>Session Hibernate</h4>
	 * <p>Method ini <b>membuka session-nya sendiri</b> lewat
	 * {@code HibernateUtil.getSessionFactory().openSession()} dan <b>selalu menutupnya di
	 * {@code finally}</b> ({@code clear} &rarr; {@code disconnect} &rarr; {@code close}, masing-masing
	 * dibungkus {@code try} sendiri). Alasannya tercatat di komentar dalam kode: method ini juga
	 * dipanggil dari <i>background thread</i> dasbor e-learning yang tidak punya transaksi aktif,
	 * sehingga {@code currentSession()} akan gagal dengan <i>"createCriteria is not valid without
	 * active transaction"</i>. Session milik pemanggil tidak pernah disentuh, jadi aman dipanggil
	 * dari konteks mana pun — tetapi berarti entity CPL yang dikembalikan sudah
	 * <b>detached</b> begitu method selesai.</p>
	 *
	 * <h4>Jebakan yang perlu diketahui</h4>
	 * <ul>
	 * <li><b>Map hasil dikunci {@code mulaiMingguKe}.</b> Bila dua baris RPS mulai pada minggu yang
	 * sama, hanya yang terakhir diproses yang bertahan — yang lain <b>hilang diam-diam</b> dari
	 * hasil, meskipun pengurutan di langkah 1 sudah membedakan keduanya.</li>
	 * <li><b>{@code getMatakuliah()} tidak diperiksa null.</b> Baris tanpa mata kuliah (kolomnya
	 * dipetakan {@code nullable = true}) akan membuat method ini melempar
	 * {@code NullPointerException}, dan pemanggilan itu berada di luar semua {@code try}.</li>
	 * <li><b>Kegagalan query CPL ditelan.</b> Bila query gagal, daftar CPL tetap kosong dan
	 * seluruh baris ber-sub-CPMK akan dianggap tak cocok sehingga <b>tidak muncul</b> di hasil —
	 * RPS tampak kosong tanpa pesan kesalahan di layar.</li>
	 * <li>Baris dengan sub-CPMK yang tidak ditemukan padanannya juga dibuang tanpa pesan.</li>
	 * </ul>
	 *
	 * @param jsonArraykurikulumPunyaMatakuliah agenda RPS terurai, biasanya
	 *                                          {@code new JSONObject(getRincian())}
	 * @return map terurut dari {@code mulaiMingguKe} ke map berisi kunci {@code "jsonObject"}
	 *         (baris RPS), {@code "subCpmk"} (baris RPS atau elemen formula CPL yang cocok), dan
	 *         {@code "capaianPembelajaranLulusanData"} (entity CPL, atau instance kosong untuk
	 *         baris bebas); kosong bila tidak ada baris yang lolos
	 * @see #ambilRinci(JSONObject, int)
	 * @see #getNilaiMenggunakanCpmk()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TreeMap<Integer, Map> populateRinci(JSONObject jsonArraykurikulumPunyaMatakuliah) {
		TreeMap<Integer, Map> maps = new TreeMap<Integer, Map>();

		TreeMap<Integer, JSONObject> treeMap = new TreeMap<Integer, JSONObject>();
		Iterator<String> keys = jsonArraykurikulumPunyaMatakuliah.keys();

		int index = 0;
		while (keys.hasNext()) {
			try {
				String keyData = keys.next();
				JSONObject jsonObject = jsonArraykurikulumPunyaMatakuliah.getJSONObject(keyData);
				if (!jsonObject.isNull("mulaiMingguKe") && !jsonObject.isNull("sampaiMingguKe")
						&& !jsonObject.isNull("sub_cpmk")) {
					jsonObject.put("keyData", keyData);
					String sss = "0000000" + (index++);
					String ss = jsonObject.get("mulaiMingguKe") + "" + sss.substring(sss.length() - 5);
					Integer indexData = Integer.parseInt(ss);
//					System.out.println("ss -> " + ss + ", indexData -> " + indexData);
					treeMap.put(indexData, jsonObject);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/KurikulumPunyaMatakuliah.java:535");
			}
		}

		Set<Long> longs = new HashSet<Long>();
		for (String d : getMatakuliah().getCapaianPembelajaranLulusan().split(",")) {
			if (!d.trim().isEmpty()) {
				try {
					longs.add(Long.parseLong(d.trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KurikulumPunyaMatakuliah.java:544");
					// TODO: handle exception
				}
			}
		}
		/* currentSession() butuh transaksi aktif (ThreadLocalSessionContext)
		 * dan method ini juga dipanggil dari background thread dasbor
		 * e-learning tanpa transaksi -> "createCriteria is not valid
		 * without active transaction". Pakai session lokal sendiri yang
		 * SELALU ditutup di finally; thread-local milik pemanggil tidak
		 * pernah disentuh sehingga aman dari semua konteks. */
		List<CapaianPembelajaranLulusan> capaianPembelajaranLulusans = new java.util.ArrayList<CapaianPembelajaranLulusan>();
		org.hibernate.Session sessionLokal = null;
		try {
			sessionLokal = HibernateUtil.getSessionFactory().openSession();
			capaianPembelajaranLulusans = ConstantValues.simpleList(
					sessionLokal.createCriteria(CapaianPembelajaranLulusan.class)
							.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("id", longs))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					CapaianPembelajaranLulusan.class);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/KurikulumPunyaMatakuliah.java:567");
		} finally {
			if (sessionLokal != null) {
				try {
					sessionLokal.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KurikulumPunyaMatakuliah.java:572");
				}
				try {
					sessionLokal.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KurikulumPunyaMatakuliah.java:576");
				}
				try {
					if (sessionLokal.isOpen()) {
						sessionLokal.close();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KurikulumPunyaMatakuliah.java:582");
				}
			}
		}
//		System.out.println("treeMap -> " + treeMap);
		for (JSONObject jsonObject : treeMap.values()) {
			try {

				Integer mulaiMingguKe = jsonObject.getInt("mulaiMingguKe");

				String _scVal = jsonObject.isNull("sub_cpmk") ? "" : (jsonObject.get("sub_cpmk") + "").trim();
				if (_scVal.equals("-1") || _scVal.isEmpty()) {
					Map map = new HashMap();
					map.put("capaianPembelajaranLulusanData", new CapaianPembelajaranLulusan());
					map.put("subCpmk", jsonObject);
					map.put("jsonObject", jsonObject);
					maps.put(mulaiMingguKe, map);
				} else {

					CapaianPembelajaranLulusan capaianPembelajaranLulusanData = null;
					JSONObject subCpmk = null;
					for (CapaianPembelajaranLulusan capaianPembelajaranLulusan : capaianPembelajaranLulusans) {

						if (getNilaiMenggunakanCpmk()) {
							if (jsonObject.get("sub_cpmk").toString()
									.equalsIgnoreCase(capaianPembelajaranLulusan.getId().toString())) {
								subCpmk = jsonObject;
								capaianPembelajaranLulusanData = capaianPembelajaranLulusan;
								break;
							}
						} else {

							JSONArray array = new JSONArray(capaianPembelajaranLulusan.getFormula());
							for (int i = 0; i < array.length(); i++) {
								try {
									JSONObject jsonObjectD = array.getJSONObject(i);

									if (jsonObjectD.isNull("key")) {
										continue;
									}
									String k1 = (jsonObjectD.get("key").toString() + "_"
											+ capaianPembelajaranLulusan.getId());
									String k2 = jsonObject.get("sub_cpmk").toString()
											+ (jsonObject.get("sub_cpmk").toString().contains("_") ? ""
													: "_" + capaianPembelajaranLulusan.getId());

									if (!k1.equalsIgnoreCase(k2)) {
										continue;
									}
									subCpmk = jsonObjectD;
									capaianPembelajaranLulusanData = capaianPembelajaranLulusan;
									break;
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/KurikulumPunyaMatakuliah.java:634");
								}
							}

							if (subCpmk != null && capaianPembelajaranLulusanData != null) {
								break;
							}
						}
					}

					if (subCpmk != null && capaianPembelajaranLulusanData != null) {
						Map map = new HashMap();
						map.put("capaianPembelajaranLulusanData", capaianPembelajaranLulusanData);
						map.put("subCpmk", subCpmk);
						map.put("jsonObject", jsonObject);
						maps.put(mulaiMingguKe, map);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/KurikulumPunyaMatakuliah.java:653");
			}
		}
		return maps;
	}

	/**
	 * Mengembalikan catatan bebas untuk RPS ini (kolom {@code text}).
	 *
	 * <p>Ikut diekspor ke berkas Excel RPS ({@code RpsObeExcelHelper}) dan dipakai sebagai konteks
	 * pada fitur bantuan AI penyusunan RPS. Bebas efek samping.</p>
	 *
	 * @return catatan; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getCatatan() {
		return catatan == null ? "" : catatan;
	}

	/**
	 * Menyetel catatan bebas RPS.
	 *
	 * @param catatan teks catatan; boleh {@code null}
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Mengembalikan pengguna yang mengunci baris RPS ini.
	 *
	 * <p>Kolom ini dipakai sebagai <b>penanda kunci</b>, bukan sekadar jejak: selama tidak
	 * {@code null}, layar RPS dan dasbor menganggap baris terkunci dan menonaktifkan
	 * penyuntingannya (lihat lencana kunci di {@code DashboardTimelinePertemuan}). Melepas kunci
	 * dilakukan dengan menyetelnya kembali ke {@code null}.</p>
	 *
	 * <p>Pola resolusi lazy sama dengan {@link #getKurikulum()}.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris ini tidak terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Mengunci atau membuka kunci baris RPS ini.
	 *
	 * @param dikunci pengguna yang mengunci; {@code null} berarti membuka kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Mengembalikan ambang minimal ketercapaian CPL/CPMK untuk mata kuliah ini, dalam persen.
	 *
	 * <p>Dipakai perhitungan OBE untuk memutuskan apakah seorang mahasiswa (atau satu kelas)
	 * dianggap mencapai capaian pembelajaran: nilai &ge; ambang ini berarti tercapai.</p>
	 *
	 * <p><b>Catatan kejanggalan:</b> getter ini sudah menjamin nilai kembali tidak pernah
	 * {@code null} (bawaan {@code 50}), namun para pemanggil masih memasang bawaan mereka
	 * sendiri yang <b>berbeda-beda</b> — {@code 60.0} di {@code RekapHasilTugasPerTugasDanUjianObe}
	 * dan sebagian {@code NilaiObeAction}, {@code 75.0} di tempat lain, {@code 0.0} di jalur
	 * perhitungan {@code safeDouble(...)}. Kode bawaan itu praktis mati (tak pernah terpakai
	 * lewat getter ini), tapi menyesatkan saat dibaca dan bisa hidup lagi bila kelak ada jalur
	 * yang membaca field-nya langsung. Dicatat apa adanya, tidak diubah.</p>
	 *
	 * @return ambang minimal ketercapaian dalam persen; {@code 50} bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	public Double getMinimalKetercapaian() {
		return minimalKetercapaian == null ? 50 : minimalKetercapaian;
	}

	/**
	 * Menyetel ambang minimal ketercapaian CPL/CPMK dalam persen.
	 *
	 * @param minimalKetercapaian ambang 0..100; {@code null} berarti kembali memakai bawaan
	 *                            {@code 50}
	 */
	public void setMinimalKetercapaian(Double minimalKetercapaian) {
		this.minimalKetercapaian = minimalKetercapaian;
	}

	/**
	 * Menyatakan cara sub-CPMK pada agenda RPS dipetakan ke capaian pembelajaran.
	 *
	 * <p>Menentukan cabang pencocokan di {@link #populateRinci(JSONObject)}:</p>
	 * <ul>
	 * <li>{@code true} — nilai {@code sub_cpmk} pada baris RPS berisi <b>id CPMK/CPL secara
	 * langsung</b>, dicocokkan apa adanya dengan {@code id} entity CPL;</li>
	 * <li>{@code false} (bawaan) — {@code sub_cpmk} berisi kunci di dalam {@code formula} milik
	 * CPL, dicocokkan lewat penyusunan kunci gabungan {@code key_idCpl}.</li>
	 * </ul>
	 * <p>Karena itu mengubah tanda ini pada kurikulum yang RPS-nya sudah terisi akan membuat
	 * seluruh baris RPS gagal dicocokkan dan <b>hilang dari tampilan</b> sampai isinya
	 * disesuaikan.</p>
	 *
	 * @return {@code true} bila penilaian memakai CPMK langsung; {@code false} sebagai bawaan
	 */
	public Boolean getNilaiMenggunakanCpmk() {
		return nilaiMenggunakanCpmk == null ? false : nilaiMenggunakanCpmk;
	}

	/**
	 * Menyetel cara sub-CPMK dipetakan ke capaian pembelajaran.
	 *
	 * @param nilaiMenggunakanCpmk {@code true} bila memakai id CPMK langsung; {@code null} dibaca
	 *                             sebagai {@code false}
	 * @see #getNilaiMenggunakanCpmk()
	 */
	public void setNilaiMenggunakanCpmk(Boolean nilaiMenggunakanCpmk) {
		this.nilaiMenggunakanCpmk = nilaiMenggunakanCpmk;
	}

	/**
	 * Mengembalikan bobot tiap CPL untuk mata kuliah ini, sebagai teks berformat konvensi.
	 *
	 * <p>Format: {@code "CPL-2:15,CPL-4:45,CPL-9:40"} — pasangan {@code kode:bobot} dipisah koma,
	 * bobot dalam persen. Tidak ada validasi apa pun di sini (tidak dijamin berjumlah 100, tidak
	 * dijamin kodenya ada); penguraian dilakukan pemanggil, mis.
	 * {@code DasboardObeElearningHelper}. Bebas efek samping.</p>
	 *
	 * @return teks bobot CPL; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getCplBobot() {
		return cplBobot == null ? "" : cplBobot;
	}

	/**
	 * Menyetel bobot tiap CPL untuk mata kuliah ini.
	 *
	 * @param cplBobot teks berformat {@code "CPL-2:15,CPL-4:45"}; boleh {@code null}
	 */
	public void setCplBobot(String cplBobot) {
		this.cplBobot = cplBobot;
	}

	/**
	 * Mengembalikan pemetaan butir soal UTS ke sub-CPMK, sebagai teks berformat konvensi.
	 *
	 * <p>Format satu baris per sub-CPMK: {@code "Sub CPMK 1.1|PG 1"}, dipisah baris baru. Dipakai
	 * untuk lampiran RPS dan analisis butir soal. Bebas efek samping, tanpa validasi.</p>
	 *
	 * @return teks pemetaan soal UTS; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getPemetaanSoalUts() {
		return pemetaanSoalUts == null ? "" : pemetaanSoalUts;
	}

	/**
	 * Menyetel pemetaan butir soal UTS ke sub-CPMK.
	 *
	 * @param pemetaanSoalUts teks pemetaan per baris; boleh {@code null}
	 */
	public void setPemetaanSoalUts(String pemetaanSoalUts) {
		this.pemetaanSoalUts = pemetaanSoalUts;
	}

	/**
	 * Mengembalikan pemetaan butir soal UAS ke sub-CPMK.
	 *
	 * <p>Format dan perlakuannya sama persis dengan {@link #getPemetaanSoalUts()}.</p>
	 *
	 * @return teks pemetaan soal UAS; string kosong bila belum diisi, tidak pernah {@code null}
	 * @see #getPemetaanSoalUts()
	 */
	@Column(columnDefinition = "text")
	public String getPemetaanSoalUas() {
		return pemetaanSoalUas == null ? "" : pemetaanSoalUas;
	}

	/**
	 * Menyetel pemetaan butir soal UAS ke sub-CPMK.
	 *
	 * @param pemetaanSoalUas teks pemetaan per baris; boleh {@code null}
	 */
	public void setPemetaanSoalUas(String pemetaanSoalUas) {
		this.pemetaanSoalUas = pemetaanSoalUas;
	}

	/**
	 * Mengembalikan komposisi komponen penilaian mata kuliah ini, sebagai teks berformat konvensi.
	 *
	 * <p>Format: {@code "Kuis:10,Tugas:10,Keaktifan:10,UTS:30,UAS:40"} — pasangan
	 * {@code nama:bobot} dipisah koma, bobot dalam persen. Bersifat dokumentatif untuk RPS;
	 * <b>bukan</b> sumber bobot yang dipakai mesin penilaian (bobot yang benar-benar dihitung ada
	 * di {@code FormatNilai}/{@code PembombotanNilai}), jadi keduanya bisa saja tidak sinkron.
	 * Tanpa validasi, bebas efek samping.</p>
	 *
	 * @return teks komponen penilaian; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKomponenPenilaian() {
		return komponenPenilaian == null ? "" : komponenPenilaian;
	}

	/**
	 * Menyetel komposisi komponen penilaian.
	 *
	 * @param komponenPenilaian teks berformat {@code "Kuis:10,UTS:30"}; boleh {@code null}
	 */
	public void setKomponenPenilaian(String komponenPenilaian) {
		this.komponenPenilaian = komponenPenilaian;
	}

	/**
	 * Mengembalikan teknik penilaian per CPMK, sebagai teks berformat konvensi.
	 *
	 * <p>Format satu baris per CPMK: {@code "CPMK-1:Kuis,UTS"} — nama CPMK, titik dua, lalu daftar
	 * teknik dipisah koma; antarbaris dipisah baris baru. Diurai baris demi baris oleh
	 * {@code NilaiObeAction} untuk menyusun tabel rencana asesmen. Bebas efek samping.</p>
	 *
	 * @return teks teknik per CPMK; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getTeknikPerCpmk() {
		return teknikPerCpmk == null ? "" : teknikPerCpmk;
	}

	/**
	 * Menyetel teknik penilaian per CPMK.
	 *
	 * @param teknikPerCpmk teks berformat {@code "CPMK-1:Kuis,UTS"} per baris; boleh {@code null}
	 */
	public void setTeknikPerCpmk(String teknikPerCpmk) {
		this.teknikPerCpmk = teknikPerCpmk;
	}

	/**
	 * Mengembalikan rubrik penilaian mata kuliah ini, sebagai teks berformat konvensi.
	 *
	 * <p>Format per baris: baris yang diawali {@code '#'} adalah judul sub-rubrik (mis.
	 * {@code "#Rubrik Materi Presentasi (40%)"}), baris lainnya berbentuk
	 * {@code "Aspek|Bobot%|Skor4|Skor3|Skor2|Skor1"}. Dipakai untuk lampiran RPS dan ekspor Excel.
	 * Bebas efek samping, tanpa validasi.</p>
	 *
	 * @return teks rubrik penilaian; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getRubrikPenilaian() {
		return rubrikPenilaian == null ? "" : rubrikPenilaian;
	}

	/**
	 * Menyetel rubrik penilaian mata kuliah ini.
	 *
	 * @param rubrikPenilaian teks rubrik per baris; boleh {@code null}
	 */
	public void setRubrikPenilaian(String rubrikPenilaian) {
		this.rubrikPenilaian = rubrikPenilaian;
	}
}
