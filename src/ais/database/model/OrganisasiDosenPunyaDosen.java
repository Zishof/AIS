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

import org.hibernate.envers.Audited;

/**
 * Entity penghubung <b>keanggotaan seorang dosen pada satu organisasi profesi/keilmuan</b>
 * &mdash; tabel {@code public.organisasi_dosen_punya_dosen}.
 *
 * <p>Baris di sini menjawab pertanyaan "dosen X tercatat sebagai anggota organisasi Y, dengan
 * jabatan apa, sejak kapan sampai kapan, sudah disetujui atau belum". Ini bukan tabel penghubung
 * murni (bukan sekadar pasangan dua foreign key): setiap baris membawa atribut sendiri &mdash;
 * jabatan dalam organisasi, rentang periode, keterangan bebas, tahun keanggotaan, identitas
 * pengaju, dan penanda persetujuan &mdash; sehingga satu dosen bisa punya beberapa baris untuk
 * organisasi yang sama pada periode/jabatan berbeda.</p>
 *
 * <h2>Posisi dalam keluarga entity</h2>
 * <ul>
 *   <li>{@link OrganisasiDosen} &mdash; master organisasi (kode, nama, nama Inggris, level lewat
 *       {@code LevelOrganisasiDosen}, serta cakupan fakultas/jurusan).</li>
 *   <li>{@link JabatanOrganisasiDosen} &mdash; master jabatan/peran di dalam organisasi
 *       (opsional; boleh {@code null}).</li>
 *   <li>{@link Dosen} &mdash; pemilik keanggotaan.</li>
 *   <li>{@link Tbmuser} &mdash; akun yang mengajukan/mencatat baris ini (opsional; lihat catatan
 *       penting pada {@link #getTbmuser()}).</li>
 * </ul>
 * <p>Entity ini adalah padanan dosen dari {@code OrganisasiIntraKampusPunyaMahasiswa} pada sisi
 * mahasiswa, dan bersaudara dengan {@code KegiatanKedosenanPunyaDosen}, {@code PrestasiDosen},
 * serta {@code PenghargaanDosen} yang memakai pola portofolio dosen yang sama persis (indeks JSON
 * per dosen, alur pengajuan-persetujuan, lampiran SK).</p>
 *
 * <h2>Dari mana baris ini dibuat/diubah</h2>
 * <ol>
 *   <li><b>Sisi organisasi (staf/admin)</b> &mdash;
 *       {@code ais.action.master.helper.OrganisasiDosenPunyaDosenHelper}, dibuka sebagai panel
 *       detail baris organisasi di {@code ais.action.master.OrganisasiDosenAction}. Panel ini
 *       menyunting periode/jabatan/keterangan secara inline, menyetujui baris, mengunggah SK,
 *       dan menyediakan tombol <i>Bersihkan</i> yang menghapus massal seluruh baris BELUM
 *       disetujui milik organisasi tersebut lewat SQL native.</li>
 *   <li><b>Sisi dosen (layanan mandiri)</b> &mdash;
 *       {@code ais.action.master.helper.DosenPunyaOrganisasiDosenHelper}, dipanggil dari halaman
 *       profil dosen ({@code ais.action.master.helper.profile.ProfileDosen}). Dosen mengajukan
 *       keanggotaannya sendiri; hak sunting hanya terbuka bagi pemilik baris ATAU atasan
 *       langsungnya, dan hanya selama {@link #getPersetujuan()} masih {@code false}.</li>
 *   <li><b>Penugasan massal</b> &mdash;
 *       {@code ais.action.master.helper.AmbilDataDosenForOrganisasiDosenHelper} ("Ambil Dosen"),
 *       mendaftarkan sekumpulan dosen sekaligus ke satu organisasi.</li>
 *   <li><b>Impor Excel</b> &mdash; {@code OrganisasiDosenAction} mencocokkan dosen lewat NIDN,
 *       lalu <i>upsert</i> berdasarkan pasangan (dosen, organisasi). Impor ini juga mengisi
 *       {@link #setOleh(String)}, {@link #setTbmuser(Tbmuser)}, dan
 *       {@link #setDiubahDari(String)}.</li>
 * </ol>
 *
 * <h2>Siapa yang membaca</h2>
 * <ul>
 *   <li>{@code ais.action.master.sapto.LaporanProfileDosen_A_4_5_5} &mdash; butir borang
 *       akreditasi A.4.5.5 (keanggotaan dosen dalam organisasi profesi). Hanya baris dengan
 *       {@code persetujuan = true} yang ikut dilaporkan.</li>
 *   <li>{@code ais.action.master.dashboard.admin.DashboardOrganisasiDosenUmum} dan
 *       {@code DasborPerguruanTinggiTerpadu} &mdash; rekap jumlah keanggotaan per organisasi/
 *       jabatan/tahun. Keduanya memfilter memakai properti {@link #getTahun() tahun}, sehingga
 *       baris tanpa {@link #getMulai() mulai} tidak pernah muncul di dasbor (lihat
 *       {@link #getTahun()}).</li>
 *   <li>{@code ProfileDosen}/{@code ProfileUiHelper} &mdash; kartu "Organisasi Dosen" pada profil.</li>
 * </ul>
 *
 * <h2>Indeks JSON per dosen &amp; cache</h2>
 * <p>Selain disimpan di tabelnya sendiri, ID setiap baris dicatat pada berkas indeks JSON milik
 * dosen ({@code Dosen#ambilLokasiOrganisasiDosenPunyaDosen()} dan kawan-kawannya). Indeks itu
 * dipelihara otomatis oleh {@code ais.database.hibernate.AuditListener}: pada
 * <i>insert</i>/<i>update</i> memanggil {@code Dosen#populateOrganisasiDosenPunyaDosen(...)},
 * pada <i>delete</i> memanggil {@code Dosen#removeOrganisasiDosenPunyaDosen(...)}. Kelas ini juga
 * terdaftar pada {@code ais.common.DataUtil.CLASS_IZINKAN}, artinya instance-nya boleh di-cache
 * MapDB dan bisa dibaca kembali lewat {@code GeneralValueObject.ambilData(...)} tanpa menyentuh
 * database.</p>
 *
 * <h2>Lampiran SK</h2>
 * <p>Surat Keputusan/Surat Keterangan tidak disimpan di entity ini, melainkan pada
 * {@code LampiranLain} yang dikaitkan lewat pasangan ({@link #getId() id},
 * {@code OrganisasiDosenPunyaDosen.class.getName()}). Karena itu penghapusan baris di sini tidak
 * otomatis menghapus berkas lampirannya.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()},
 *       {@link #getDiubahDari()}/{@link #setDiubahDari(String)}.</li>
 *   <li><b>Identitas</b> &mdash; {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Relasi</b> &mdash; {@link #getOrganisasiDosen()}, {@link #getDosen()},
 *       {@link #getJabatanOrganisasiDosen()}, {@link #getTbmuser()} beserta setter-nya. SEMUA
 *       getter relasi memanggil {@code check(...)} dan MENULIS BALIK hasilnya ke field.</li>
 *   <li><b>Atribut keanggotaan</b> &mdash; {@link #getMulai()}, {@link #getSampai()},
 *       {@link #getKeterangan()}, {@link #getPersetujuan()}, {@link #getTahun()}.</li>
 * </ul>
 * <p>Tidak ada method bisnis/query statis di kelas ini; seluruh query hidup di Helper/Action
 * pemanggil.</p>
 *
 * <h2>Hal non-obvious yang WAJIB diketahui sebelum menyunting</h2>
 * <ol>
 *   <li><b>Pemetaan berbasis properti.</b> Anotasi JPA dipasang pada <i>getter</i> ({@code @Id}
 *       ada di {@link #getId()}), sehingga Hibernate membaca nilai lewat getter, termasuk saat
 *       <i>flush</i>. Konsekuensinya, efek samping di dalam getter ikut menentukan apa yang
 *       benar-benar tertulis ke kolom &mdash; lihat {@link #getTahun()} dan
 *       {@link #getTbmuser()}.</li>
 *   <li><b>{@link GeneralValueObject} bukan {@code @Entity}/{@code @MappedSuperclass}</b>,
 *       melainkan POJO abstrak biasa. Hibernate TIDAK memetakan properti induknya, karena itu
 *       {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sengaja
 *       <b>dideklarasikan ulang</b> di kelas ini. Itu KEHARUSAN TEKNIS, bukan duplikasi yang
 *       perlu "dibersihkan".</li>
 *   <li><b>Tidak ada {@code nama}/{@code kode} terpetakan.</b> Kelas ini tidak menimpa
 *       {@code getNama()}/{@code getKode()} milik base class, jadi nilai keduanya tidak pernah
 *       tersimpan/terbaca dari database. Kode generik yang mengandalkan {@code getNama()} akan
 *       mendapat nilai dari base class (biasanya {@code null}); layar-layar yang ada karena itu
 *       selalu memasok label sendiri, misalnya {@code organisasiDosen.getNama()}.</li>
 *   <li><b>Nama kolom mengikuti nama properti apa adanya</b>, karena
 *       {@code ais.database.hibernate.MyNamingStrategy} adalah turunan
 *       {@code DefaultNamingStrategy} (tanpa konversi ke {@code snake_case}). Properti tanpa
 *       {@code @Column} seperti {@code diubahDari} karena itu memakai kolom bernama
 *       {@code diubahDari} persis.</li>
 *   <li><b>Seluruh perubahan terekam Envers</b> ({@code @Audited}), jadi nilai lama tetap
 *       tersimpan di tabel {@code _aud} meskipun baris aslinya dihapus.</li>
 * </ol>
 *
 * <p><b>Catatan asal-usul:</b> komentar generator asli pada kelas ini berbunyi "Bank generated by
 * hbm2java" &mdash; salah salin dari {@link Bank}, sumber yang komentarnya terbawa ke puluhan
 * entity lain di paket ini. Kelas ini sama sekali tidak berhubungan dengan modul bank.</p>
 *
 * @see OrganisasiDosen
 * @see JabatanOrganisasiDosen
 * @see Dosen
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "organisasi_dosen_punya_dosen")

public class OrganisasiDosenPunyaDosen extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya identik dengan puluhan entity lain di paket ini karena semuanya
	 * hasil salin-tempel dari berkas yang sama &mdash; jangan dijadikan penanda identitas kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris keanggotaan; dibangkitkan database ({@code IDENTITY}). */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit). */
	private String oleh;

	/** ID pengguna terakhir yang mengubah baris ini (jejak audit). */
	private String olehId;

	/**
	 * ID pengguna terakhir yang mengubah baris ini &mdash; diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}, sehingga properti warisan tidak dipetakan Hibernate.</p>
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa menulis apa pun). Ini disengaja agar jejak audit yang
	 * sudah ada tidak terhapus oleh pemanggil yang meneruskan nilai kosong.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir. Dipanggil {@code AuditTimestampInterceptor},
	 * dan juga secara eksplisit oleh alur impor Excel {@code OrganisasiDosenAction}.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang di kelas ini karena
	 * alasan yang sama dengan {@link #getOlehId()}.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini. Untuk baris yang dibuat lewat "Ambil Dosen" atau impor Excel, jejak audit
	 * pertama hanya terisi bila pemanggil mengisinya sendiri (impor Excel memang melakukannya).</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja diletakkan pada baris fisik yang sama
	 * dalam kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor}
	 * lewat {@link #onUpdate()}, bukan oleh kode layar.
	 *
	 * <p>Berbeda dengan {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini TIDAK
	 * menyaring {@code null} &mdash; nilai {@code null} akan benar-benar menghapus stempel
	 * waktu.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini, disimpan sebagai {@code TIMESTAMP}. Dideklarasikan
	 * ulang di kelas ini karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris keanggotaan dalam bentuk "&lt;organisasi&gt; - &lt;dosen&gt;",
	 * dipakai antara lain sebagai teks progres pada ekspor Excel di {@code OrganisasiDosenAction}.
	 *
	 * <p><b>Kuirk penting:</b> method ini membaca <b>field</b> {@code organisasiDosen} dan
	 * {@code dosen} secara langsung, BUKAN lewat {@link #getOrganisasiDosen()}/
	 * {@link #getDosen()}. Artinya {@code check(...)} tidak dijalankan, sehingga untuk objek yang
	 * relasinya masih berupa proxy lazy dan sesinya sudah tertutup, hasilnya bisa berupa teks
	 * proxy Hibernate atau memicu {@code LazyInitializationException} &mdash; berbeda dari
	 * kebanyakan pembacaan lain di kelas ini yang selalu aman lewat getter.</p>
	 *
	 * <p>Nilai {@code null} pada salah satu sisi akan tercetak sebagai teks {@code "null"} karena
	 * memakai perangkaian string biasa.</p>
	 *
	 * @return teks gabungan organisasi dan dosen
	 */
	public String toString() {
		return organisasiDosen + " - " + dosen;
	}

	/** Organisasi profesi/keilmuan yang diikuti; wajib terisi ({@code nullable = false}). */
	private OrganisasiDosen organisasiDosen;

	/** Dosen pemilik keanggotaan ini; wajib terisi ({@code nullable = false}). */
	private Dosen dosen;

	/** Nama kelas/layar asal perubahan terakhir, dipakai sebagai jejak sumber data. */
	private String diubahDari;

	/** Akun pengguna yang mengajukan/mencatat baris ini; opsional. Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;

	/** Jabatan/peran dosen di dalam organisasi tersebut; opsional. */
	private JabatanOrganisasiDosen jabatanOrganisasiDosen;

	/** Tanggal awal keanggotaan; juga menjadi sumber nilai {@link #getTahun() tahun}. */
	private Date mulai;

	/** Tanggal akhir keanggotaan; boleh {@code null} untuk keanggotaan yang masih berjalan. */
	private Date sampai;

	/** Keterangan bebas mengenai keanggotaan (kolom bertipe {@code text}). */
	private String keterangan;

	/** Penanda apakah keanggotaan sudah disetujui pihak berwenang; mengunci penyuntingan. */
	private Boolean persetujuan;

	/** Tahun keanggotaan; TURUNAN dari {@link #mulai}, lihat {@link #getTahun()}. */
	private Integer tahun;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate untuk instansiasi entity, sekaligus
	 * dipakai kode layar saat membuat baris keanggotaan baru (misalnya alur impor Excel dan
	 * "Ambil Dosen" ketika pasangan dosen&ndash;organisasi belum ada).
	 */
	public OrganisasiDosenPunyaDosen() {
	}

	/**
	 * Primary key baris keanggotaan.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}). ID ini juga menjadi kunci pada indeks JSON per dosen dan kunci
	 * pencarian lampiran SK di {@code LampiranLain}.</p>
	 *
	 * @return ID baris, atau {@code null} untuk objek yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Praktis hanya dipanggil Hibernate; kode aplikasi membiarkan
	 * database yang mengisinya.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Organisasi profesi/keilmuan yang diikuti dosen pada baris ini (kolom
	 * {@code organisasi_dosen}, wajib terisi).
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(...)} milik {@link GeneralValueObject} untuk
	 * meresolusi proxy lazy, lalu <b>menulis balik hasilnya ke field</b>. Getter ini karena itu
	 * tidak <i>read-only</i>: setelah dipanggil sekali, field menyimpan objek yang sudah
	 * terinisialisasi sehingga pemanggilan berikutnya murah. Tidak ada penulisan ke database yang
	 * dipicu langsung oleh getter ini.</p>
	 *
	 * @return organisasi terkait; secara praktik tidak pernah {@code null} untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "organisasi_dosen", nullable = false)
	public OrganisasiDosen getOrganisasiDosen() {
		organisasiDosen = check(organisasiDosen);
		return organisasiDosen;
	}

	/**
	 * Menetapkan organisasi yang diikuti.
	 *
	 * <p>Relasi memakai {@code cascade = PERSIST, MERGE}, jadi menyimpan baris keanggotaan ikut
	 * menyimpan objek organisasi yang belum tersimpan.</p>
	 *
	 * @param organisasiDosen organisasi terkait
	 */
	public void setOrganisasiDosen(OrganisasiDosen organisasiDosen) {
		this.organisasiDosen = organisasiDosen;
	}

	/**
	 * Dosen pemilik keanggotaan ini (kolom {@code dosen}, wajib terisi).
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getOrganisasiDosen()} &mdash;
	 * {@code check(...)} lalu tulis balik ke field.</p>
	 *
	 * <p>Getter ini juga dipanggil {@code AuditListener} setiap kali baris disimpan atau dihapus,
	 * untuk memelihara indeks JSON keanggotaan organisasi milik dosen tersebut.</p>
	 *
	 * @return dosen pemilik; secara praktik tidak pernah {@code null} untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menetapkan dosen pemilik keanggotaan.
	 *
	 * @param dosen dosen pemilik
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Akun pengguna yang mengajukan/mencatat baris ini (kolom {@code tbmuser}, opsional).
	 *
	 * <p><b>Getter ini TIDAK mengembalikan isi field apa adanya</b> &mdash; nilainya
	 * disembunyikan menjadi {@code null} bila akun tersebut merupakan <i>dosen biasa</i>, yaitu
	 * ketika ketiga syarat berikut terpenuhi:</p>
	 * <ol>
	 *   <li>{@code tbmuser != null};</li>
	 *   <li>{@code tbmuser.ambilDosen() != null} &mdash; perhatikan bahwa
	 *       {@code Tbmuser#ambilDosen()} sendiri mengembalikan {@code null} untuk role yang boleh
	 *       melihat data pegawai lain, sehingga syarat ini praktis berarti "akun dosen yang hanya
	 *       boleh melihat datanya sendiri";</li>
	 *   <li>{@code tbmuser.hakAkses().getRoleId()} sama dengan {@code "dosen"} (tanpa
	 *       memperhatikan besar-kecil huruf).</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi yang mudah terlewat:</b> karena entity ini dipetakan berbasis properti
	 * (anotasi ada di getter), Hibernate membaca nilai kolom {@code tbmuser} justru lewat method
	 * ini saat <i>insert</i>/<i>update</i>. Untuk baris yang diajukan sendiri oleh dosen lewat
	 * layar profil, nilai yang tersimpan ke kolom karena itu adalah {@code null} &mdash;
	 * identitas pengaju tidak tercatat pada relasi ini, dan jejak yang tersisa hanya field audit
	 * {@code oleh}/{@code olehId} yang ikut berubah setiap kali baris disunting siapa pun. Baris
	 * yang dibuat staf/admin (misalnya impor Excel) tidak terpengaruh karena role-nya bukan
	 * {@code "dosen"}.</p>
	 *
	 * <p><b>Risiko NPE:</b> {@code hakAkses()} maupun {@code getRoleId()} bisa mengembalikan
	 * {@code null} tanpa penjagaan di sini; bila itu terjadi, exception muncul di tengah siklus
	 * flush Hibernate, bukan di kode pemanggil.</p>
	 *
	 * <p><b>Efek samping tambahan:</b> {@code check(...)} tetap dijalankan dan hasilnya ditulis
	 * balik ke field, jadi field bisa terisi objek terinisialisasi meskipun nilai kembaliannya
	 * {@code null}.</p>
	 *
	 * @return akun pengaju, atau {@code null} bila belum diisi atau bila akun tersebut adalah
	 *         akun dosen biasa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen") ? null : tbmuser;
	}

	/**
	 * Menetapkan akun pengguna pengaju/pencatat baris ini. Diisi antara lain oleh alur impor
	 * Excel {@code OrganisasiDosenAction} dengan akun staf yang menjalankan impor.
	 *
	 * <p>Perhatikan bahwa nilai yang disimpan di sini belum tentu ikut tertulis ke database
	 * &mdash; lihat penyaringan pada {@link #getTbmuser()}.</p>
	 *
	 * @param tbmuser akun pengaju; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Nama kelas/layar asal perubahan terakhir baris ini, misalnya
	 * {@code "OrganisasiDosenAction"} untuk baris hasil impor Excel.
	 *
	 * <p>Properti ini tanpa {@code @Column}, sehingga memakai penamaan default
	 * {@code MyNamingStrategy} &mdash; kolom {@code diubahDari} apa adanya. Nilainya murni jejak
	 * informasi; tidak ada logika yang bercabang berdasarkan isinya.</p>
	 *
	 * @return nama sumber perubahan, atau {@code null} bila tidak pernah diisi
	 */
	public String getDiubahDari() {
		return diubahDari;
	}

	/**
	 * Menetapkan nama kelas/layar asal perubahan terakhir.
	 *
	 * @param diubahDari nama sumber perubahan; boleh {@code null}
	 */
	public void setDiubahDari(String diubahDari) {
		this.diubahDari = diubahDari;
	}

	/**
	 * Jabatan/peran dosen di dalam organisasi tersebut (kolom
	 * {@code jabatan_organisasi_dosen}, opsional).
	 *
	 * <p><b>Efek samping:</b> {@code check(...)} lalu tulis balik ke field, sama seperti getter
	 * relasi lainnya.</p>
	 *
	 * <p>Nilai ini dipakai sebagai dimensi pengelompokan pada dasbor rekap organisasi dosen.</p>
	 *
	 * <p><b>Kuirk yang perlu diwaspadai:</b> laporan borang
	 * {@code ais.action.master.sapto.LaporanProfileDosen_A_4_5_5} menentukan kolom
	 * Internasional/Nasional/Lokal dengan membandingkan <b>nama jabatan</b> di sini terhadap teks
	 * {@code "Internasional"}/{@code "Nasional"}, dan jatuh ke "Lokal" untuk selain itu.
	 * Cakupan/level organisasi sebenarnya tersimpan pada {@code OrganisasiDosen#getLevelOrganisasiDosen()},
	 * sedangkan master {@link JabatanOrganisasiDosen} berisi peran (Ketua, Sekretaris, Anggota,
	 * dan seterusnya) &mdash; sehingga selama master jabatan diisi sesuai maknanya, hampir semua
	 * baris akan terlaporkan sebagai "Lokal". Dicatat apa adanya di sini; perbaikannya bukan di
	 * entity ini.</p>
	 *
	 * @return jabatan dalam organisasi, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_organisasi_dosen", nullable = true)
	public JabatanOrganisasiDosen getJabatanOrganisasiDosen() {
		jabatanOrganisasiDosen = check(jabatanOrganisasiDosen);
		return jabatanOrganisasiDosen;
	}

	/**
	 * Menetapkan jabatan dosen dalam organisasi.
	 *
	 * @param jabatanOrganisasiDosen jabatan dalam organisasi; boleh {@code null}
	 */
	public void setJabatanOrganisasiDosen(JabatanOrganisasiDosen jabatanOrganisasiDosen) {
		this.jabatanOrganisasiDosen = jabatanOrganisasiDosen;
	}

	/**
	 * Tanggal awal keanggotaan, disimpan sebagai {@code DATE} (tanpa komponen jam).
	 *
	 * <p>Selain ditampilkan apa adanya di layar dan ekspor Excel, tanggal ini menjadi
	 * <b>satu-satunya sumber</b> nilai {@link #getTahun() tahun} yang dipakai memfilter dasbor
	 * &mdash; lihat {@link #getTahun()}.</p>
	 *
	 * @return tanggal awal keanggotaan, atau {@code null} bila tidak diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Menetapkan tanggal awal keanggotaan.
	 *
	 * <p>Perubahan nilai ini otomatis mengubah {@link #getTahun() tahun} pada pembacaan
	 * berikutnya, termasuk saat Hibernate melakukan flush.</p>
	 *
	 * @param mulai tanggal awal keanggotaan; boleh {@code null}
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Tanggal akhir keanggotaan, disimpan sebagai {@code DATE}. Dibiarkan {@code null} untuk
	 * keanggotaan yang masih berjalan; layar menampilkannya sebagai sel kosong.
	 *
	 * <p>Tidak ada validasi apa pun di entity ini yang menjamin {@code sampai} berada setelah
	 * {@link #getMulai() mulai}, maupun yang mencegah dua baris dengan periode tumpang tindih
	 * untuk pasangan dosen&ndash;organisasi yang sama.</p>
	 *
	 * @return tanggal akhir keanggotaan, atau {@code null} bila masih berjalan/tidak diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Menetapkan tanggal akhir keanggotaan.
	 *
	 * @param sampai tanggal akhir keanggotaan; boleh {@code null}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Keterangan bebas mengenai keanggotaan (kolom bertipe {@code text}, tanpa batas panjang
	 * praktis).
	 *
	 * <p>Diisi pengguna lewat kotak teks di kedua layar keanggotaan dan ikut diekspor ke Excel.
	 * Isinya tidak pernah dipakai untuk pengambilan keputusan program.</p>
	 *
	 * <p><b>Catatan kontrak:</b> berbeda dari sebagian entity lain yang menjamin nilai non-null,
	 * getter ini mengembalikan isi field apa adanya dan bisa {@code null}. Pemanggil yang
	 * merangkainya ke label ZK perlu menyiapkan penanganan {@code null} sendiri.</p>
	 *
	 * @return keterangan keanggotaan, atau {@code null} bila tidak diisi
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan keanggotaan.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status persetujuan keanggotaan &mdash; penanda paling penting pada entity ini.
	 *
	 * <p><b>Normalisasi:</b> bila field masih {@code null} (baris lama atau baris baru yang
	 * belum pernah disetujui), method mengembalikan {@code false}. Getter ini karena itu
	 * <b>tidak pernah mengembalikan {@code null}</b>, sehingga pemeriksaan bergaya
	 * {@code getPersetujuan() == null || getPersetujuan()} yang tersebar di kedua Helper
	 * sebenarnya cabang mati untuk sisi {@code null}-nya. Perhatikan juga bahwa normalisasi ini
	 * hanya berlaku pada nilai kembalian; kolom di database tetap bisa berisi {@code NULL}
	 * sehingga query SQL langsung harus menulis {@code (persetujuan is null or persetujuan =
	 * false)} &mdash; persis yang dilakukan tombol "Bersihkan".</p>
	 *
	 * <p><b>Dampak nilai ini:</b></p>
	 * <ul>
	 *   <li>Selama {@code false}, seluruh kontrol sunting (periode, jabatan, keterangan) dan
	 *       tombol hapus terbuka bagi pemilik/atasan langsung; begitu {@code true}, semuanya
	 *       dikunci lewat {@code setDisabled(...)}/{@code setVisible(...)}.</li>
	 *   <li>Hanya baris {@code true} yang masuk laporan borang akreditasi
	 *       {@code LaporanProfileDosen_A_4_5_5}.</li>
	 *   <li>Tombol "Bersihkan" pada panel organisasi menghapus massal seluruh baris yang BELUM
	 *       disetujui untuk organisasi tersebut.</li>
	 * </ul>
	 *
	 * <p><b>Catatan pengunciannya bersifat tampilan.</b> Penguncian di atas hanya menonaktifkan
	 * komponen ZK; entity ini sendiri tidak menolak perubahan pada baris yang sudah disetujui,
	 * dan checkbox persetujuan di panel sisi organisasi tidak dibungkus pemeriksaan
	 * {@code CommonPrivilages.UPDATE} (hanya tombol hapus yang memeriksa
	 * {@code CommonPrivilages.DELETE}).</p>
	 *
	 * @return {@code true} bila keanggotaan sudah disetujui; {@code false} bila belum atau bila
	 *         nilainya belum pernah diisi
	 */
	public Boolean getPersetujuan() {
		return persetujuan == null ? false : persetujuan;
	}

	/**
	 * Menetapkan status persetujuan keanggotaan. Dipanggil dari checkbox "Setujui" pada kedua
	 * layar keanggotaan dan dari kolom persetujuan pada impor Excel.
	 *
	 * @param persetujuan {@code true} bila disetujui; {@code null} diperlakukan sama dengan
	 *                    {@code false} saat dibaca kembali
	 */
	public void setPersetujuan(Boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Tahun keanggotaan &mdash; nilai TURUNAN, bukan isian mandiri.
	 *
	 * <p><b>Efek samping (pola getter yang menulis balik ke field):</b> setiap kali dipanggil,
	 * bila {@link #getMulai() mulai} terisi, method menghitung ulang tahunnya memakai kalender
	 * aplikasi ({@code ais.ui.util.WaktuUtil.getCalendar()}) dan <b>menimpa field
	 * {@code tahun}</b>. Apa pun yang pernah ditetapkan lewat {@link #setTahun(Integer)} akan
	 * hilang begitu {@code mulai} tidak {@code null}.</p>
	 *
	 * <p>Karena entity dipetakan berbasis properti, method inilah yang dibaca Hibernate saat
	 * flush &mdash; sehingga kolom {@code tahun} di database praktis <b>selalu</b> sinkron dengan
	 * tahun dari {@code mulai}, dan sebuah operasi baca biasa dapat berujung pada
	 * {@code UPDATE} bila nilai lamanya berbeda.</p>
	 *
	 * <p><b>Konsekuensi bila {@code mulai} kosong:</b> nilai lama dikembalikan apa adanya, dan
	 * untuk baris yang dibuat tanpa tanggal mulai (misalnya lewat "Ambil Dosen") nilainya tetap
	 * {@code null}. Baris seperti itu <b>tidak akan pernah muncul</b> di
	 * {@code DashboardOrganisasiDosenUmum} maupun {@code DasborPerguruanTinggiTerpadu}, karena
	 * keduanya menyaring dengan {@code Restrictions.between("tahun", ...)}/
	 * {@code Restrictions.eq("tahun", ...)}; hal yang sama berlaku untuk filter tahun di
	 * {@code DosenPunyaOrganisasiDosenHelper}.</p>
	 *
	 * @return tahun keanggotaan (tahun dari {@code mulai} bila tersedia), atau {@code null} bila
	 *         {@code mulai} kosong dan nilai tahun belum pernah tersimpan
	 */
	public Integer getTahun() {
		if (mulai != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(mulai);
			tahun = calendar.get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun keanggotaan.
	 *
	 * <p>Praktis tidak berguna dipanggil kode aplikasi: nilai yang ditetapkan di sini akan
	 * ditimpa {@link #getTahun()} pada pembacaan berikutnya selama {@link #getMulai() mulai}
	 * terisi. Setter ini pada dasarnya hanya ada agar Hibernate dapat memuat nilai kolom
	 * {@code tahun} dari database.</p>
	 *
	 * @param tahun tahun keanggotaan; boleh {@code null}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}
}
