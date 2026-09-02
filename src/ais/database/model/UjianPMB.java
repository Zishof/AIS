package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

/**
 * Entity <b>gelaran ujian seleksi PMB</b> (tabel {@code public.ujian_pmb}) — satu baris mewakili
 * SATU peristiwa ujian saringan masuk milik sebuah {@link GelombangPendaftaran}: namanya, lokasi
 * pelaksanaannya, berapa hari ia berlangsung beserta tanggal tiap harinya, dan teks pengumuman
 * yang dicetak pada dokumen calon mahasiswa.
 *
 * <h3>Bukan {@link Ujian} — jangan tertukar</h3>
 * <p>Ada dua entity di paket ini yang namanya mirip tetapi perannya jauh berbeda:</p>
 * <ul>
 *   <li><b>{@link Ujian}</b> ({@code public.ujian}) — master ujian/kuis <i>online</i> untuk
 *   <b>mahasiswa aktif</b>: berisi kumpulan soal ({@link UjianPunyaSoal} → {@link BankSoal}),
 *   jenis koreksi, nilai lulus, tata tertib, sertifikat, dan syarat kelayakan.</li>
 *   <li><b>{@code UjianPMB}</b> (kelas ini) — <i>penyelenggaraan</i> ujian seleksi masuk untuk
 *   <b>calon mahasiswa</b>. Kelas ini <b>tidak memuat satu pun soal, bobot, durasi, maupun skor
 *   kelulusan</b>. Ia murni pembungkus jadwal + tempat + teks informasi. Kalau mencari "nilai
 *   ambang lulus PMB", tempatnya bukan di sini.</li>
 * </ul>
 *
 * <h3>Kedudukan dalam alur PMB</h3>
 * <p>Rantai relasi dari gelombang sampai ke calon mahasiswa berbentuk seperti ini:</p>
 * <pre>
 * GelombangPendaftaran
 *   └─1..N─ UjianPMB            (kelas ini — kapan &amp; di mana ujian digelar)
 *              ├─1..N─ RuangPMB          (ruang/kelas fisik, kapasitas, gedung)
 *              │          └─1..N─ RuangPaketPMB ──N..1── BiodataCalonMahasiswa
 *              └─1..N─ JadwalUjianPMB    (sesi ujian daring per Paket/prodi)
 *                         └─ Pertemuan ─ PertemuanPunyaUjian ─ Ujian ─ BankSoal
 * </pre>
 * <ul>
 *   <li><b>Tidak ada relasi langsung ke {@link BiodataCalonMahasiswa}</b>. Keterkaitan peserta
 *   selalu ditempuh lewat penempatan ruang: {@code RuangPaketPMB → RuangPMB → UjianPMB}. Contoh
 *   penelusurannya ada di {@code CommonReportHelper.genSklMap(BiodataCalonMahasiswa)}, yang
 *   memproyeksikan {@code ruangPMB.ujianPMB.id} lalu memuat ulang entity-nya, dan baru
 *   jatuh-balik ke pencarian berdasarkan gelombang bila calon belum ditempatkan di ruang mana
 *   pun.</li>
 *   <li>{@link JadwalUjianPMB} adalah tempat soal PMB daring sesungguhnya bersandar; ia menunjuk
 *   balik ke {@code UjianPMB} lewat properti {@code ujianPMB} dan dari sanalah
 *   {@code AbsensiHelper} maupun {@code HasilUjianMahasiswaHelper} menemukan gelombang
 *   pendaftaran peserta.</li>
 * </ul>
 *
 * <h3>Pembuatan otomatis oleh gelombang pendaftaran</h3>
 * <p>{@code GelombangPendaftaran.chekKuotaPendaftar()} akan <b>membuat sendiri</b> satu
 * {@code UjianPMB} bawaan (nama {@code "Online"}, lokasi {@code "Lokasi Dimana Saja (Online)"},
 * {@code tampilkanJadwalUjianDiKartuUjian = true}) beserta satu {@link RuangPMB} bawaan
 * berkapasitas 10.000 bila gelombang tersebut belum punya keduanya. Jadi baris tabel ini bisa
 * muncul tanpa pernah ada operator yang membukanya lewat layar {@code UjianPMBAction} — jangan
 * kaget menemukan data yang "tidak pernah diinput siapa pun".</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Bayangan field audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}. Lihat catatan
 *   arsitektur di bawah.</li>
 *   <li><b>Identitas</b> — {@link #getNama()}, {@link #getLokasi()}, {@link #toString()},
 *   {@link #getAktif()}.</li>
 *   <li><b>Jadwal pelaksanaan</b> — {@link #getJumlahHariUjian()} sebagai pengendali, plus
 *   sepuluh pasang {@code getTanggalUjianN()}/{@code setTanggalUjianN(Date)}.</li>
 *   <li><b>Konteks akademik</b> — {@link #getGelombangPendaftaran()} (satu-satunya relasi
 *   sejati), dengan {@link #getTahunAkademik()} dan {@link #getTahun()} sebagai salinan
 *   denormalisasi yang diturunkan darinya.</li>
 *   <li><b>Teks pengumuman</b> — {@link #getKeterangan()},
 *   {@link #getKeteranganSetelahBayar()}, {@link #getKeteranganHeader()},
 *   {@link #getKeteranganSetelahBayarHeader()}, dan sakelar
 *   {@link #getTampilkanJadwalUjianDiKartuUjian()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, query statis, maupun utilitas berkas di kelas ini; seluruh
 * anggotanya konstruktor, {@code toString()}, atau pasangan getter/setter properti.</p>
 *
 * <h3>Catatan arsitektur: field audit dideklarasikan ulang di sini</h3>
 * <p>Kelas ini {@code extends} {@link GeneralValueObject}, tetapi induk tersebut <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * sama sekali tidak memetakan propertinya. Karena itu {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di setiap entity, termasuk di
 * sini. Ini keharusan teknis arsitektur, bukan kelalaian atau duplikasi yang perlu "dirapikan".
 * Kontrak umum method warisan ({@code check}, {@code udah}, {@code ambilData}, dan
 * kawan-kawan) didokumentasikan lengkap di {@link GeneralValueObject}.</p>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Pemetaan berbasis properti, dan tidak ada satu pun {@code @Transient}.</b> Karena
 *   {@code @Id} dipasang pada getter {@link #getId()}, Hibernate membaca SELURUH getter sebagai
 *   kolom — termasuk yang sama sekali tanpa anotasi seperti {@link #getJumlahHariUjian()} atau
 *   {@link #getTanggalUjian5()}. Konsekuensinya besar: <b>apa pun yang dikembalikan getter akan
 *   ikut tersimpan ke database</b> pada flush berikutnya lewat dirty checking, walau pemanggil
 *   hanya bermaksud "membaca". Ini berlaku baik untuk getter yang menugaskan ulang ke field-nya
 *   sendiri maupun untuk getter yang sekadar mengembalikan nilai bawaan tanpa penugasan.</li>
 *   <li><b>Getter yang menulis balik ke field — sudah diverifikasi ada 15</b>:
 *   {@link #getKeterangan()}, {@link #getJumlahHariUjian()}, {@link #getTanggalUjian2()} sampai
 *   {@link #getTanggalUjian10()} (sembilan buah), {@link #getTahunAkademik()},
 *   {@link #getTahun()}, {@link #getGelombangPendaftaran()},
 *   {@link #getTampilkanJadwalUjianDiKartuUjian()}, {@link #getKeteranganSetelahBayar()},
 *   {@link #getKeteranganHeader()}, dan {@link #getKeteranganSetelahBayarHeader()}. Hanya
 *   {@link #getGelombangPendaftaran()} yang penulisannya "netral" (penyeragaman instance hasil
 *   {@link GeneralValueObject#check(Object)}); sisanya benar-benar mengubah nilai data.</li>
 *   <li><b>Getter penghapus data.</b> {@link #getTanggalUjian2()}…{@link #getTanggalUjian10()}
 *   <b>meng-{@code null}-kan field-nya sendiri</b> begitu {@link #getJumlahHariUjian()} lebih
 *   kecil dari nomor harinya. Digabung dengan pemetaan properti di atas, artinya: menurunkan
 *   jumlah hari ujian dari 5 menjadi 1 lalu sekadar <i>menampilkan</i> daftar ujian sudah cukup
 *   untuk <b>menghapus permanen</b> tanggal hari ke-2..ke-5 dari basis data. Perilaku ini
 *   disengaja sebagai "pemangkasan otomatis", tetapi tidak bisa dibatalkan.</li>
 *   <li><b>Getter yang tidak menulis balik namun tetap mengubah nilai tersimpan</b>:
 *   {@link #getNama()} (memangkas spasi), {@link #getLokasi()} (mengganti nilai kosong dengan
 *   {@code "Belum ditentukan"}), dan {@link #getAktif()} (mengganti {@code null} dengan
 *   {@code true}). Field-nya memang tidak disentuh, tetapi karena akses properti, nilai hasil
 *   getter-lah yang dibandingkan dan disimpan Hibernate.</li>
 *   <li><b>Tidak ada getter yang membuka atau menutup sesi Hibernate.</b> Sudah diperiksa
 *   menyeluruh: kelas ini tidak mengimpor {@code Session}/{@code HibernateUtil} dan tidak
 *   menjalankan query apa pun. Satu-satunya sentuhan ke lapisan persistensi adalah
 *   {@code check()} di {@link #getGelombangPendaftaran()}, yang pengelolaan sesinya ditangani
 *   sepenuhnya di dalam {@link GeneralValueObject}.</li>
 *   <li><b>Urutan pemanggilan {@link #getTahunAkademik()} → {@link #getTahun()} penting.</b>
 *   {@code getTahun()} membaca <i>field</i> {@code tahunAkademik} secara langsung, bukan
 *   getter-nya; bila {@code getTahunAkademik()} belum pernah dipanggil pada instance itu, field
 *   tersebut masih berisi nilai lama dari basis data (bisa {@code null}) dan {@code tahun} tidak
 *   diperbarui. Lihat catatan pada method masing-masing.</li>
 *   <li><b>{@code keteranganHeader} dan {@code keteranganSetelahBayarHeader} tanpa pemakai.</b>
 *   Penelusuran seluruh pohon sumber (Java, ZUL, laporan) hanya menemukan getter/setter-nya
 *   sendiri — layar {@code UjianPMBAction} pun tidak menampilkannya. Keduanya tetap kolom
 *   terpetakan yang aktif, sehingga nilai bawaannya tetap ditulis ke basis data pada flush
 *   pertama.</li>
 *   <li><b>Kembaran hampir persis di modul lain.</b> {@code ais.database.model.sekolah.UjianPSB}
 *   (penerimaan siswa baru) dan {@code ais.database.model.recruitment.UjianPegawai}
 *   (rekrutmen pegawai) adalah salinan struktur kelas ini. Perbaikan di sini hampir selalu
 *   perlu ditiru ke sana, dan sebaliknya.</li>
 * </ul>
 *
 * <p>Entity ini {@code @Audited} (Envers, tabel riwayat {@code ujian_pmb_AUD}) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate} sehingga SQL hanya memuat kolom yang benar-benar
 * berubah — properti yang menonjol ketika sebagian besar getter di sini gemar menulis balik.</p>
 *
 * @see GelombangPendaftaran
 * @see RuangPMB
 * @see JadwalUjianPMB
 * @see BiodataCalonMahasiswa
 * @see Ujian
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "ujian_pmb")

public class UjianPMB extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi. Nilainya kebetulan sama persis dengan milik {@link Ujian} dan
	 * {@link JadwalUjianPMB} (sisa salin-tempel saat kelas ini dibuat); tidak berdampak karena
	 * {@code serialVersionUID} hanya dibandingkan antar-versi kelas yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code public.ujian_pmb.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris ini (bayangan field audit).
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna yang mengubah baris ini.
	 *
	 * <p><b>Perhatikan:</b> masukan {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b>
	 * — nilai lama dipertahankan. Jejak audit sengaja dibuat tidak bisa dikosongkan lewat setter
	 * ini.</p>
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh apa pun
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan kosong/{@code null} diabaikan diam-diam
	 * sehingga nilai audit lama tidak bisa terhapus.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh apa pun
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini (bayangan field audit).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} dijalankan,
	 * meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} terisi dari konteks pengguna yang sedang aktif.
	 *
	 * <p>Tidak untuk dipanggil langsung dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan diperbarui
	 * {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (bayangan field audit).
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Label ringkas entity ini, dipakai ZK saat menampilkan {@code UjianPMB} di combobox/bandbox
	 * (mis. pemilihan ujian pada layar {@code RuangPMBAction}) dan pada
	 * {@code RevisiHelper.createNewRevisi(...)}.
	 *
	 * <p><b>Dua kehalusan yang mudah terlewat:</b> method ini membaca <b>field</b> {@code nama}
	 * secara langsung, bukan {@link #getNama()}, sehingga hasilnya <i>tidak</i> dipangkas
	 * spasinya; dan ia <b>dapat mengembalikan {@code null}</b> bila nama belum diisi — berbeda
	 * dari kebiasaan {@code toString()} pada umumnya. Pemanggil yang merangkai string dengan
	 * hasil ini akan mendapat teks {@code "null"}.</p>
	 *
	 * @return nama ujian apa adanya, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Nama gelaran ujian (kolom wajib); lihat {@link #getNama()}. */
	private String nama;
	/** Lokasi/tempat ujian digelar; lihat {@link #getLokasi()}. */
	private String lokasi;
	/**
	 * Banyaknya hari pelaksanaan ujian (1..10), default 1. Bertindak sebagai <b>pengendali</b>
	 * bagi {@code tanggalUjian1}..{@code tanggalUjian10}; lihat {@link #getJumlahHariUjian()}.
	 */
	private Integer jumlahHariUjian = 1;
	/** Tanggal pelaksanaan hari ke-1; satu-satunya tanggal yang tidak pernah dipangkas otomatis. */
	private Date tanggalUjian1;
	/** Tanggal pelaksanaan hari ke-2; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 2. */
	private Date tanggalUjian2;
	/** Tanggal pelaksanaan hari ke-3; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 3. */
	private Date tanggalUjian3;
	/** Tanggal pelaksanaan hari ke-4; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 4. */
	private Date tanggalUjian4;
	/** Tanggal pelaksanaan hari ke-5; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 5. */
	private Date tanggalUjian5;
	/** Tanggal pelaksanaan hari ke-6; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 6. */
	private Date tanggalUjian6;
	/** Tanggal pelaksanaan hari ke-7; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 7. */
	private Date tanggalUjian7;
	/** Tanggal pelaksanaan hari ke-8; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 8. */
	private Date tanggalUjian8;
	/** Tanggal pelaksanaan hari ke-9; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 9. */
	private Date tanggalUjian9;
	/** Tanggal pelaksanaan hari ke-10; di-{@code null}-kan bila {@code jumlahHariUjian} &lt; 10. */
	private Date tanggalUjian10;

	/**
	 * Salinan denormalisasi tahun akademik milik {@link #gelombangPendaftaran}, mis.
	 * {@code "2026/2027"}; lihat {@link #getTahunAkademik()}.
	 */
	private String tahunAkademik;
	/**
	 * Tahun awal hasil urai {@link #tahunAkademik} (mis. {@code 2026}); lihat
	 * {@link #getTahun()}.
	 */
	private Integer tahun;
	/**
	 * Gelombang pendaftaran pemilik gelaran ujian ini — satu-satunya relasi sejati kelas ini;
	 * lihat {@link #getGelombangPendaftaran()}.
	 */
	private GelombangPendaftaran gelombangPendaftaran;
	/**
	 * Sakelar penampilan daftar tanggal ujian pada kartu ujian, default {@code true}; lihat
	 * {@link #getTampilkanJadwalUjianDiKartuUjian()}.
	 */
	private Boolean tampilkanJadwalUjianDiKartuUjian;
	/**
	 * Teks informasi pada <b>kartu pembayaran/registrasi</b> (sebelum calon membayar); lihat
	 * {@link #getKeterangan()}.
	 */
	private String keterangan;
	/** Judul di atas {@link #keterangan}; tanpa pemakai. Lihat {@link #getKeteranganHeader()}. */
	private String keteranganHeader;
	/**
	 * Teks informasi pada <b>kartu ujian</b> (setelah calon membayar); lihat
	 * {@link #getKeteranganSetelahBayar()}.
	 */
	private String keteranganSetelahBayar;
	/**
	 * Judul di atas {@link #keteranganSetelahBayar}; tanpa pemakai. Lihat
	 * {@link #getKeteranganSetelahBayarHeader()}.
	 */
	private String keteranganSetelahBayarHeader;

	/** Penanda baris masih dipakai; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk membentuk instance saat
	 * memuat baris dari basis data.
	 *
	 * <p>Juga dipakai langsung oleh kode aplikasi, mis. {@code UjianPMBAction.onAdd(Event)} saat
	 * operator menekan tombol tambah, dan {@code GelombangPendaftaran.chekKuotaPendaftar()} saat
	 * membentuk gelaran ujian bawaan {@code "Online"}.</p>
	 */
	public UjianPMB() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} bertipe identity di PostgreSQL dan ditandai {@code insertable = false},
	 * jadi nilainya dibangkitkan basis data saat {@code INSERT} dan baru terisi setelah
	 * {@code save}/{@code flush}. Karena {@code @Id} dipasang di getter inilah seluruh kelas ini
	 * memakai <b>akses properti</b> — lihat catatan pada Javadoc kelas.</p>
	 *
	 * @return ID baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris ini.
	 *
	 * <p>Praktis hanya dipanggil Hibernate. Mengubahnya dari kode aplikasi pada entity yang sudah
	 * tersimpan akan mengacaukan identitas baris.</p>
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama gelaran ujian, sudah dipangkas spasi depan-belakangnya.
	 *
	 * <p>Kolom wajib ({@code nullable = false}); layar {@code UjianPMBAction.onSave(Event)}
	 * menolak simpan bila kosong. Perhatikan bahwa hasil pemangkasan ini adalah nilai yang
	 * dibaca Hibernate untuk dirty checking, sehingga spasi berlebih pada data lama akan
	 * <b>ikut terbersihkan permanen</b> pada flush berikutnya. Bandingkan dengan
	 * {@link #toString()} yang membaca field mentahnya.</p>
	 *
	 * @return nama ujian tanpa spasi tepi, atau {@code null} bila field-nya {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama gelaran ujian.
	 *
	 * @param nama nama ujian; dipakai apa adanya tanpa validasi di lapisan entity
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan teks informasi untuk <b>kartu pembayaran/registrasi</b> calon mahasiswa —
	 * pada layar {@code UjianPMBAction} kolom ini berlabel <i>"Informasi ke peserta ujian pada
	 * kartu pembayaran"</i>.
	 *
	 * <p><b>Menulis balik ke field.</b> Bila nilainya {@code null} atau berisi spasi saja, method
	 * ini <b>mengisi field dengan teks bawaan lima langkah</b> (cara membayar di loket, menyimpan
	 * bukti, mencetak struk lewat portal, melengkapi biodata, mencetak kartu ujian) lalu
	 * mengembalikannya. Karena properti ini terpetakan, teks bawaan tersebut ikut ter-{@code
	 * UPDATE} permanen ke kolom {@code keterangan} pada flush berikutnya — sekali terisi, ia
	 * tidak lagi "kosong" dan tidak akan diganti bila teks bawaan di kode kelak diubah.</p>
	 *
	 * <p>Perhatikan pula bahwa teks bawaan memuat kalimat <i>"membayar senilai Rp. ."</i> dengan
	 * nominal yang memang dibiarkan kosong, serta salah ketik yang sudah lama ada
	 * ({@code "2  Bukti"} tanpa titik, {@code "malakukan"}). Keduanya dipertahankan apa adanya
	 * agar data yang sudah telanjur tersimpan di basis data UAT/produksi tetap cocok.</p>
	 *
	 * <p>Pemakainya: renderer daftar {@code UjianPMBAction.UjianPMBRenderer} (lewat
	 * {@code buatInformasiRingkas}, dirender sebagai {@code Label} sehingga aman dari HTML
	 * suntikan), formulir ubah, dan parameter {@code info} pada
	 * {@code CommonReportHelper.genSklMap(...)} untuk Surat Keterangan Lulus.</p>
	 *
	 * @return teks informasi kartu pembayaran; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}

		if (keterangan.trim().isEmpty()) {
			keterangan = "1. Pembayaran registrasi mahasiswa baru dapat dilakukan di loket pembayaran dengan membayar senilai Rp. .\n"
					+ "2  Bukti Pembayaran harap disimpan sebagai bukti pembayaran.\n"
					+ "3. Setelah melakukan pembayaran, struk pembayaran dapat di cetak dengan malakukan Login kembali ke portal penerimaan mahasiswa baru, kemudian Klik Info Pembayaran, masukkan nomor registrasi dan tanggal lahir.\n"
					+ "4. Selanjutnya, tutup menu Info Pembayaran, dan click Tombol Login Calon Mahasiswa untuk melengkapi pengisian Form Biodata Calon Mahasiswa beserta lampiran-lampiran-nya.\n"
					+ "5. Terakhir, Cetak Kartu Ujian.";
		}
		return this.keterangan;
	}

	/**
	 * Menetapkan teks informasi kartu pembayaran.
	 *
	 * <p>Mengisi dengan {@code null} atau string kosong tidak "mengosongkan" kolom secara
	 * permanen: pembacaan berikutnya lewat {@link #getKeterangan()} akan mengisinya kembali
	 * dengan teks bawaan.</p>
	 *
	 * @param keterangan teks informasi; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan banyaknya hari pelaksanaan ujian, dengan {@code null} dianggap {@code 1}.
	 *
	 * <p><b>Menulis balik ke field</b> saat nilainya {@code null}, sehingga baris warisan yang
	 * kolomnya masih {@code NULL} akan permanen menjadi {@code 1} pada flush berikutnya.</p>
	 *
	 * <p>Nilai inilah <b>pengendali</b> seluruh {@code getTanggalUjianN()}: setiap tanggal dengan
	 * nomor hari melebihi angka ini akan dihapus (lihat {@link #getTanggalUjian2()}). Di layar
	 * {@code UjianPMBAction} nilai ini juga mengatur baris formulir tanggal mana yang
	 * ditampilkan; perubahannya memicu {@code rowEventListener} yang menyembunyikan baris
	 * berlebih.</p>
	 *
	 * @return jumlah hari ujian, minimal {@code 1}; tidak pernah {@code null}
	 */
	public Integer getJumlahHariUjian() {
		if (jumlahHariUjian == null) {
			jumlahHariUjian = 1;
		}
		return jumlahHariUjian;
	}

	/**
	 * Menetapkan banyaknya hari pelaksanaan ujian.
	 *
	 * <p><b>Berdampak merusak.</b> Menurunkan angka ini membuat tanggal hari-hari di atasnya
	 * dihapus permanen oleh getter-nya pada pembacaan berikutnya; tidak ada mekanisme
	 * pengembalian. Tidak ada validasi rentang di sini — nilai di luar 1..10 tidak ditolak, hanya
	 * saja tanggal hari ke-11 dan seterusnya memang tidak punya kolom.</p>
	 *
	 * @param jumlahHariUjian jumlah hari; {@code null} akan dinormalkan menjadi {@code 1} oleh
	 *                        {@link #getJumlahHariUjian()}
	 */
	public void setJumlahHariUjian(Integer jumlahHariUjian) {
		this.jumlahHariUjian = jumlahHariUjian;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-1.
	 *
	 * <p>Satu-satunya tanggal yang <b>tidak</b> tunduk pada pemangkasan otomatis
	 * {@link #getJumlahHariUjian()}, karena jumlah hari minimalnya memang 1.</p>
	 *
	 * @return tanggal hari ke-1, atau {@code null} bila belum dijadwalkan
	 */
	public Date getTanggalUjian1() {
		return tanggalUjian1;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-1.
	 *
	 * @param tanggalUjian1 tanggal ujian; boleh {@code null}
	 */
	public void setTanggalUjian1(Date tanggalUjian1) {
		this.tanggalUjian1 = tanggalUjian1;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-2.
	 *
	 * <p><b>Getter penghapus data.</b> Bila {@link #getJumlahHariUjian()} kurang dari 2, field
	 * {@code tanggalUjian2} <b>di-{@code null}-kan</b> lebih dulu. Karena properti ini terpetakan
	 * Hibernate (tidak ada {@code @Transient} di kelas ini), penghapusan tersebut ikut
	 * ter-{@code UPDATE} ke basis data pada flush berikutnya — <b>membaca saja sudah cukup untuk
	 * menghilangkan data secara permanen</b>. Pola identik berlaku pada
	 * {@link #getTanggalUjian3()} sampai {@link #getTanggalUjian10()}.</p>
	 *
	 * @return tanggal hari ke-2, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian2() {
		if (getJumlahHariUjian() < 2) {
			tanggalUjian2 = null;
		}
		return tanggalUjian2;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-2.
	 *
	 * <p>Nilai ini hanya bertahan selama {@link #getJumlahHariUjian()} bernilai minimal 2; lihat
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @param tanggalUjian2 tanggal ujian; boleh {@code null}
	 */
	public void setTanggalUjian2(Date tanggalUjian2) {
		this.tanggalUjian2 = tanggalUjian2;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-3, meng-{@code null}-kan field-nya bila
	 * {@link #getJumlahHariUjian()} kurang dari 3.
	 *
	 * <p>Efek samping penghapusan permanennya dijelaskan lengkap di
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ke-3, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian3() {
		if (getJumlahHariUjian() < 3) {
			tanggalUjian3 = null;
		}
		return tanggalUjian3;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-3.
	 *
	 * @param tanggalUjian3 tanggal ujian; boleh {@code null}
	 * @see #getTanggalUjian3()
	 */
	public void setTanggalUjian3(Date tanggalUjian3) {
		this.tanggalUjian3 = tanggalUjian3;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-4, meng-{@code null}-kan field-nya bila
	 * {@link #getJumlahHariUjian()} kurang dari 4.
	 *
	 * <p>Efek samping penghapusan permanennya dijelaskan lengkap di
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ke-4, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian4() {
		if (getJumlahHariUjian() < 4) {
			tanggalUjian4 = null;
		}
		return tanggalUjian4;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-4.
	 *
	 * @param tanggalUjian4 tanggal ujian; boleh {@code null}
	 * @see #getTanggalUjian4()
	 */
	public void setTanggalUjian4(Date tanggalUjian4) {
		this.tanggalUjian4 = tanggalUjian4;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-5, meng-{@code null}-kan field-nya bila
	 * {@link #getJumlahHariUjian()} kurang dari 5.
	 *
	 * <p>Efek samping penghapusan permanennya dijelaskan lengkap di
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ke-5, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian5() {
		if (getJumlahHariUjian() < 5) {
			tanggalUjian5 = null;
		}
		return tanggalUjian5;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-5.
	 *
	 * @param tanggalUjian5 tanggal ujian; boleh {@code null}
	 * @see #getTanggalUjian5()
	 */
	public void setTanggalUjian5(Date tanggalUjian5) {
		this.tanggalUjian5 = tanggalUjian5;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-6, meng-{@code null}-kan field-nya bila
	 * {@link #getJumlahHariUjian()} kurang dari 6.
	 *
	 * <p>Efek samping penghapusan permanennya dijelaskan lengkap di
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ke-6, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian6() {
		if (getJumlahHariUjian() < 6) {
			tanggalUjian6 = null;
		}
		return tanggalUjian6;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-6.
	 *
	 * @param tanggalUjian6 tanggal ujian; boleh {@code null}
	 * @see #getTanggalUjian6()
	 */
	public void setTanggalUjian6(Date tanggalUjian6) {
		this.tanggalUjian6 = tanggalUjian6;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-7, meng-{@code null}-kan field-nya bila
	 * {@link #getJumlahHariUjian()} kurang dari 7.
	 *
	 * <p>Efek samping penghapusan permanennya dijelaskan lengkap di
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ke-7, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian7() {
		if (getJumlahHariUjian() < 7) {
			tanggalUjian7 = null;
		}
		return tanggalUjian7;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-7.
	 *
	 * @param tanggalUjian7 tanggal ujian; boleh {@code null}
	 * @see #getTanggalUjian7()
	 */
	public void setTanggalUjian7(Date tanggalUjian7) {
		this.tanggalUjian7 = tanggalUjian7;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-8, meng-{@code null}-kan field-nya bila
	 * {@link #getJumlahHariUjian()} kurang dari 8.
	 *
	 * <p>Efek samping penghapusan permanennya dijelaskan lengkap di
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ke-8, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian8() {
		if (getJumlahHariUjian() < 8) {
			tanggalUjian8 = null;
		}
		return tanggalUjian8;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-8.
	 *
	 * @param tanggalUjian8 tanggal ujian; boleh {@code null}
	 * @see #getTanggalUjian8()
	 */
	public void setTanggalUjian8(Date tanggalUjian8) {
		this.tanggalUjian8 = tanggalUjian8;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-9, meng-{@code null}-kan field-nya bila
	 * {@link #getJumlahHariUjian()} kurang dari 9.
	 *
	 * <p>Efek samping penghapusan permanennya dijelaskan lengkap di
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ke-9, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian9() {
		if (getJumlahHariUjian() < 9) {
			tanggalUjian9 = null;
		}
		return tanggalUjian9;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-9.
	 *
	 * @param tanggalUjian9 tanggal ujian; boleh {@code null}
	 * @see #getTanggalUjian9()
	 */
	public void setTanggalUjian9(Date tanggalUjian9) {
		this.tanggalUjian9 = tanggalUjian9;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan ujian hari ke-10, meng-{@code null}-kan field-nya bila
	 * {@link #getJumlahHariUjian()} kurang dari 10.
	 *
	 * <p>Hari ke-10 adalah batas atas yang didukung entity ini; tidak ada kolom untuk hari
	 * berikutnya. Efek samping penghapusan permanennya dijelaskan lengkap di
	 * {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ke-10, atau {@code null} bila belum dijadwalkan atau sudah dipangkas
	 */
	public Date getTanggalUjian10() {
		if (getJumlahHariUjian() < 10) {
			tanggalUjian10 = null;
		}
		return tanggalUjian10;
	}

	/**
	 * Menetapkan tanggal pelaksanaan ujian hari ke-10.
	 *
	 * @param tanggalUjian10 tanggal ujian; boleh {@code null}
	 * @see #getTanggalUjian10()
	 */
	public void setTanggalUjian10(Date tanggalUjian10) {
		this.tanggalUjian10 = tanggalUjian10;
	}

	/**
	 * Mengembalikan tahun akademik gelaran ujian ini, mis. {@code "2026/2027"}.
	 *
	 * <p><b>Menulis balik ke field, sekaligus menyegarkan relasi.</b> Method ini pertama-tama
	 * memanggil {@link #getGelombangPendaftaran()} (yang menjalankan
	 * {@link GeneralValueObject#check(Object)}) dan menugaskan hasilnya ke field
	 * {@code gelombangPendaftaran}; bila gelombangnya ada, field {@code tahunAkademik}
	 * <b>ditimpa</b> dengan nilai milik gelombang tersebut. Kolom {@code tahun_akademik} pada
	 * tabel ini karena itu bersifat <b>salinan denormalisasi</b> yang tersinkron sendiri ke basis
	 * data setiap kali dibaca lalu di-flush — bukan nilai yang boleh diisi manual dan diharapkan
	 * bertahan.</p>
	 *
	 * <p>Kolom {@code tahunAkademik} sengaja tidak lagi disunting dari layar; blok formulir dan
	 * blok {@code onSave} yang dulu mengisinya di {@code UjianPMBAction} sudah dinonaktifkan
	 * (dijadikan komentar), dan pencarian tahun ajaran di layar itu kini menyaring lewat
	 * {@code gelombangPendaftaran.tahunAkademik}, bukan lewat kolom ini.</p>
	 *
	 * <p>Dipakai antara lain oleh {@code UjianPMBAction.UjianPMBRenderer} sebagai kolom pertama
	 * daftar.</p>
	 *
	 * @return tahun akademik dari gelombang pendaftaran, atau nilai tersimpan sebelumnya
	 *         (mungkin {@code null}) bila gelombangnya belum diisi
	 */
	public String getTahunAkademik() {
		gelombangPendaftaran = getGelombangPendaftaran();
		if (gelombangPendaftaran != null) {
			tahunAkademik = gelombangPendaftaran.getTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik.
	 *
	 * <p>Nilai yang diisi lewat setter ini <b>tidak awet</b>: pembacaan berikutnya lewat
	 * {@link #getTahunAkademik()} akan menimpanya dengan tahun akademik milik gelombang
	 * pendaftaran, selama gelombangnya terisi.</p>
	 *
	 * @param tahunAkademik tahun akademik dalam format {@code "YYYY/YYYY"}; boleh {@code null}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan tahun awal dari tahun akademik, mis. {@code 2026} untuk
	 * {@code "2026/2027"}.
	 *
	 * <p><b>Tiga jebakan yang harus disadari:</b></p>
	 * <ol>
	 *   <li><b>Bergantung urutan pemanggilan.</b> Method ini membaca <i>field</i>
	 *   {@code tahunAkademik} langsung, <b>bukan</b> {@link #getTahunAkademik()}. Bila
	 *   {@code getTahunAkademik()} belum pernah dipanggil pada instance ini, field tersebut masih
	 *   berisi nilai lama dari basis data — bisa {@code null}, sehingga {@code tahun} tidak
	 *   diperbarui sama sekali dan nilai lamanya yang dikembalikan.</li>
	 *   <li><b>Menulis balik ke field.</b> Hasil urai ditugaskan ke field {@code tahun}, dan
	 *   karena properti ini terpetakan, ikut tersimpan ke kolom {@code tahun} pada flush
	 *   berikutnya.</li>
	 *   <li><b>Bisa melempar {@link NumberFormatException}.</b> Tidak ada penjagaan bila potongan
	 *   pertama sebelum {@code "/"} bukan angka (mis. nilai penyaring {@code "Semua"} yang
	 *   telanjur tersimpan). Pemanggil tidak diberi jalur pemulihan.</li>
	 * </ol>
	 *
	 * <p>Penelusuran seluruh pohon sumber tidak menemukan satu pun pemanggil {@code getTahun()}
	 * pada {@code UjianPMB} selain Hibernate sendiri saat memetakan kolomnya.</p>
	 *
	 * @return tahun awal tahun akademik, atau {@code null} bila {@code tahunAkademik} maupun
	 *         {@code tahun} belum pernah terisi
	 * @throws NumberFormatException bila bagian sebelum {@code "/"} pada {@code tahunAkademik}
	 *                               bukan bilangan bulat
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun awal tahun akademik.
	 *
	 * <p>Sama seperti {@link #setTahunAkademik(String)}, nilainya tidak awet: begitu
	 * {@link #getTahun()} dipanggil dengan {@code tahunAkademik} terisi, nilai ini ditimpa hasil
	 * urai.</p>
	 *
	 * @param tahun tahun awal; boleh {@code null}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan {@link GelombangPendaftaran} pemilik gelaran ujian ini — satu-satunya relasi
	 * sejati kelas ini, dan sumber seluruh konteks akademik (tahun akademik, perguruan tinggi,
	 * jenis seleksi, kuota).
	 *
	 * <p>Relasi {@code FetchType.LAZY} dengan {@code CascadeType.PERSIST}/{@code MERGE} ke kolom
	 * {@code gelombang_pendaftaran}, dan {@code nullable = true} sehingga baris tanpa gelombang
	 * secara teknis sah — meski layar {@code UjianPMBAction.onSave(Event)} mewajibkannya diisi
	 * untuk data yang dibuat operator.</p>
	 *
	 * <p><b>Menulis balik ke field, tetapi netral.</b> Hasil
	 * {@link GeneralValueObject#check(Object)} ditugaskan ulang ke field. Ini penyeragaman
	 * instance sekaligus penangkal {@code LazyInitializationException} pada proxy yang sudah
	 * terlepas dari sesi; nilai datanya tidak berubah, jadi tidak menimbulkan {@code UPDATE}
	 * yang tak diinginkan. Kontrak lengkap {@code check()} — termasuk sifatnya yang tidak pernah
	 * melempar exception dan tidak pernah mengembalikan {@code null} untuk argumen non-null —
	 * ada di {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return gelombang pendaftaran pemilik, atau {@code null} bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran", nullable = true)
	public GelombangPendaftaran getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		return gelombangPendaftaran;
	}

	/**
	 * Menetapkan gelombang pendaftaran pemilik gelaran ujian ini.
	 *
	 * @param gelombangPendaftaran gelombang pendaftaran; boleh {@code null}
	 */
	public void setGelombangPendaftaran(GelombangPendaftaran gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	/**
	 * Mengembalikan lokasi pelaksanaan ujian, dengan nilai kosong/{@code null} digantikan teks
	 * {@code "Belum ditentukan"}.
	 *
	 * <p><b>Tidak menulis balik ke field</b> — berbeda dari getter teks lain di kelas ini.
	 * Namun karena pemetaan berbasis properti, nilai pengganti itulah yang dibaca Hibernate,
	 * sehingga kolom {@code lokasi} di basis data <b>tetap</b> berisi teks
	 * {@code "Belum ditentukan"} setelah flush berikutnya. Akibat sampingnya: sekali tersimpan,
	 * nilai itu bukan lagi string kosong, sehingga pembacaan berikutnya mengembalikannya apa
	 * adanya dan tidak ada cara membedakan "memang belum diisi" dari "operator sengaja mengetik
	 * kalimat itu".</p>
	 *
	 * <p>Dipakai pada kolom lokasi daftar {@code UjianPMBAction}, formulir ubah, dan parameter
	 * {@code lokasi} laporan Surat Keterangan Lulus di {@code CommonReportHelper.genSklMap(...)}
	 * — yang juga memakai teks pengganti sama saat gelaran ujiannya tidak ditemukan.</p>
	 *
	 * @return lokasi ujian, atau {@code "Belum ditentukan"} bila belum diisi; tidak pernah
	 *         {@code null}
	 */
	public String getLokasi() {
		return lokasi == null || lokasi.isEmpty() ? "Belum ditentukan" : lokasi;
	}

	/**
	 * Menetapkan lokasi pelaksanaan ujian.
	 *
	 * <p>Perhatikan bahwa {@link #getLokasi()} hanya memeriksa {@code isEmpty()}, bukan
	 * {@code trim().isEmpty()}, sehingga masukan berisi spasi saja akan lolos dan dikembalikan
	 * apa adanya.</p>
	 *
	 * @param lokasi lokasi ujian; boleh {@code null}
	 */
	public void setLokasi(String lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan sakelar penampilan daftar tanggal ujian pada kartu ujian calon mahasiswa,
	 * dengan {@code null} dianggap {@code true}.
	 *
	 * <p><b>Menulis balik ke field</b> saat nilainya {@code null}, sehingga baris warisan yang
	 * kolomnya masih {@code NULL} akan permanen menjadi {@code true} pada flush berikutnya —
	 * artinya jadwal ujian <b>tampil secara bawaan</b>.</p>
	 *
	 * <p>Nilainya diteruskan ke berkas laporan sebagai parameter
	 * {@code tampilkanjadwalujiandikartuujian} oleh
	 * {@code CommonReportHelper.genSklMap(BiodataCalonMahasiswa)}. Bila calon mahasiswa tidak
	 * terhubung ke gelaran ujian mana pun, helper tersebut mengirim {@code false} sebagai
	 * gantinya.</p>
	 *
	 * @return {@code true} bila jadwal ujian ditampilkan di kartu; tidak pernah {@code null}
	 */
	public Boolean getTampilkanJadwalUjianDiKartuUjian() {
		if (tampilkanJadwalUjianDiKartuUjian == null) {
			tampilkanJadwalUjianDiKartuUjian = true;
		}
		return tampilkanJadwalUjianDiKartuUjian;
	}

	/**
	 * Menetapkan sakelar penampilan daftar tanggal ujian pada kartu ujian.
	 *
	 * <p>Diisi dari kotak centang layar {@code UjianPMBAction}, dan diisi {@code true} secara
	 * eksplisit oleh {@code GelombangPendaftaran.chekKuotaPendaftar()} saat membentuk gelaran
	 * ujian bawaan {@code "Online"}.</p>
	 *
	 * @param tampilkanJadwalUjianDiKartuUjian {@code true} untuk menampilkan; {@code null} akan
	 *                                         dinormalkan menjadi {@code true} oleh getter-nya
	 */
	public void setTampilkanJadwalUjianDiKartuUjian(Boolean tampilkanJadwalUjianDiKartuUjian) {
		this.tampilkanJadwalUjianDiKartuUjian = tampilkanJadwalUjianDiKartuUjian;
	}

	/**
	 * Mengembalikan teks informasi untuk <b>kartu ujian</b>, yaitu pengumuman yang dilihat calon
	 * mahasiswa <i>setelah</i> pembayaran registrasi terpenuhi — pada layar
	 * {@code UjianPMBAction} kolom ini berlabel <i>"Informasi ke peserta ujian pada kartu
	 * Ujian"</i>.
	 *
	 * <p><b>Menulis balik ke field.</b> Bila nilainya {@code null} atau berisi spasi saja, field
	 * diisi teks bawaan {@code "1. Alat Tuilis\n2. Papan Ujian"} — daftar barang bawaan peserta —
	 * lalu dikembalikan. Sama seperti {@link #getKeterangan()}, teks bawaan itu ikut tersimpan
	 * permanen ke basis data pada flush berikutnya. Salah ketik {@code "Tuilis"} dipertahankan
	 * apa adanya agar cocok dengan data yang sudah telanjur tersimpan.</p>
	 *
	 * <p>Pasangan {@link #getKeteranganSetelahBayarHeader()} yang seharusnya menjadi judul teks
	 * ini tidak pernah dipakai di mana pun.</p>
	 *
	 * @return teks informasi kartu ujian; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganSetelahBayar() {
		if (keteranganSetelahBayar == null) {
			keteranganSetelahBayar = "";
		}
		if (keteranganSetelahBayar.trim().isEmpty()) {
			keteranganSetelahBayar = "1. Alat Tuilis\n2. Papan Ujian";
		}

		return keteranganSetelahBayar;
	}

	/**
	 * Menetapkan teks informasi kartu ujian.
	 *
	 * <p>Mengisi dengan {@code null} atau string kosong tidak mengosongkan kolom secara permanen:
	 * pembacaan berikutnya lewat {@link #getKeteranganSetelahBayar()} akan mengisinya kembali
	 * dengan teks bawaan.</p>
	 *
	 * @param keteranganSetelahBayar teks informasi; boleh {@code null}
	 */
	public void setKeteranganSetelahBayar(String keteranganSetelahBayar) {
		this.keteranganSetelahBayar = keteranganSetelahBayar;
	}

	/**
	 * Mengembalikan judul yang seharusnya dicetak di atas {@link #getKeterangan()} pada kartu
	 * pembayaran/registrasi.
	 *
	 * <p><b>Menulis balik ke field</b>: bila {@code null}, field diisi teks bawaan
	 * {@code "Pastikan bahwa data dibawah ini adalah benar data diri anda."} dan tersimpan
	 * permanen pada flush berikutnya. Perhatikan bedanya dengan {@link #getKeterangan()} —
	 * di sini hanya {@code null} yang dianggap kosong, string berisi spasi saja
	 * <b>tidak</b> diganti.</p>
	 *
	 * <p><b>Tanpa pemakai.</b> Penelusuran seluruh pohon sumber (Java, ZUL, berkas laporan) tidak
	 * menemukan satu pun pemanggil selain getter/setter ini sendiri; layar {@code UjianPMBAction}
	 * tidak menampilkan kolomnya. Kolomnya tetap terpetakan dan tetap ikut ditulis, jadi jangan
	 * dihapus tanpa migrasi skema.</p>
	 *
	 * @return judul teks kartu pembayaran; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganHeader() {
		if (keteranganHeader == null) {
			keteranganHeader = "Pastikan bahwa data dibawah ini adalah benar data diri anda.";
		}
		return keteranganHeader;
	}

	/**
	 * Menetapkan judul teks kartu pembayaran/registrasi.
	 *
	 * <p>Tidak ada pemanggil di seluruh pohon sumber; lihat {@link #getKeteranganHeader()}.</p>
	 *
	 * @param keteranganHeader judul; boleh {@code null}, akan diganti teks bawaan saat dibaca
	 */
	public void setKeteranganHeader(String keteranganHeader) {
		this.keteranganHeader = keteranganHeader;
	}

	/**
	 * Mengembalikan judul yang seharusnya dicetak di atas {@link #getKeteranganSetelahBayar()}
	 * pada kartu ujian.
	 *
	 * <p>Perilakunya identik dengan {@link #getKeteranganHeader()} — termasuk teks bawaan yang
	 * <b>sama persis</b> ({@code "Pastikan bahwa data dibawah ini adalah benar data diri
	 * anda."}), penulisan balik ke field, dan ketiadaan pemakai di seluruh pohon sumber.</p>
	 *
	 * @return judul teks kartu ujian; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganSetelahBayarHeader() {
		if (keteranganSetelahBayarHeader == null) {
			keteranganSetelahBayarHeader = "Pastikan bahwa data dibawah ini adalah benar data diri anda.";
		}
		return keteranganSetelahBayarHeader;
	}

	/**
	 * Menetapkan judul teks kartu ujian.
	 *
	 * <p>Tidak ada pemanggil di seluruh pohon sumber; lihat
	 * {@link #getKeteranganSetelahBayarHeader()}.</p>
	 *
	 * @param keteranganSetelahBayarHeader judul; boleh {@code null}, akan diganti teks bawaan
	 *                                     saat dibaca
	 */
	public void setKeteranganSetelahBayarHeader(String keteranganSetelahBayarHeader) {
		this.keteranganSetelahBayarHeader = keteranganSetelahBayarHeader;
	}

	/**
	 * Mengembalikan penanda baris masih dipakai, dengan {@code null} dianggap {@code true}.
	 *
	 * <p><b>Tidak menulis balik ke field</b> — nilai pengganti dihitung setiap kali dipanggil.
	 * Meski begitu, karena pemetaan berbasis properti, {@code true} itulah yang dilihat Hibernate
	 * dan akan tersimpan ke kolom {@code aktif} pada flush berikutnya. Artinya baris warisan
	 * dengan kolom {@code NULL} akan permanen menjadi {@code true}.</p>
	 *
	 * <p><b>Bukan pola "flag aktif satu-arah"</b> seperti pada entity akunting: di sini tidak ada
	 * logika bisnis apa pun yang menonaktifkan baris sendiri. Nilainya sepenuhnya dikendalikan
	 * operator lewat kotak centang di daftar {@code UjianPMBAction}, yang langsung memanggil
	 * {@code Common.refreshSaveOrUpdate(...)}. Penyaring bawaan layar itu menampilkan baris
	 * dengan {@code aktif IS NULL OR aktif = true}, jadi baris lama yang belum pernah diisi tetap
	 * ikut terlihat.</p>
	 *
	 * @return {@code true} bila baris masih aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan penanda baris masih dipakai.
	 *
	 * @param aktif {@code true} bila aktif; {@code null} akan dibaca sebagai {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
