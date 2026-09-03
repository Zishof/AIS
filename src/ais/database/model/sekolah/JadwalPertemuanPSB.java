package ais.database.model.sekolah;

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
import org.json.JSONObject;

import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;

/**
 * Entity <b>slot jadwal pertemuan tatap muka Penerimaan Siswa Baru (PSB/PPDB)</b> — satu baris
 * mewakili SATU sesi pertemuan antara panitia sekolah dengan calon siswa beserta orang tuanya,
 * lengkap dengan materi/topik pertemuan, rentang waktu, dan kuota peserta. Dipetakan ke tabel
 * {@code sekolah.jadwal_pertemuan_psb}, di-audit Envers ({@code @Audited}), dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate} sehingga hanya kolom yang benar-benar berubah yang
 * ikut dalam pernyataan SQL.
 *
 * <p><b>Peran dalam alur PSB (TERVERIFIKASI dari kode pemanggil).</b> Baris entity ini dikelola
 * panitia lewat layar {@code /pages/psb/jadwal_pertemuan_psb.zul}
 * ({@code ais.action.master.sekolah.JadwalPertemuanPSBAction}) dan dikonsumsi calon siswa pada dua
 * formulir pendaftaran: {@code ais.action.master.sekolah.CalonSiswaAction} (biodata calon siswa)
 * dan {@code ais.action.master.sekolah.psb.form.PPDB_Simple6} (formulir pendaftaran ringkas). Pada
 * kedua formulir itu label yang dilihat pendaftar berbunyi <i>"Jadwal Pertemuan Siswa / Orang
 * Tua"</i>, sedangkan pada layar panitia kolom {@link #getNama() nama} diberi label <i>"Materi
 * Pertemuan"</i>. Calon siswa memilih SATU slot; pilihan itu disimpan di sisi calon siswa sebagai
 * {@code CalonSiswa.jadwalPertemuanPSB} (relasi many-to-one dari {@link CalonSiswa}, FK
 * {@code jadwal_pertemuan_psb}) — entity ini TIDAK menyimpan koleksi peserta, jadi "siapa saja
 * yang terdaftar" selalu dihitung lewat query balik ke {@link CalonSiswa}.
 *
 * <p><b>Hubungan dengan entity keluarga PSB lain — hasil verifikasi eksplisit.</b>
 * <ul>
 * <li>{@code GelombangPendaftaranPsb} — satu-satunya relasi keluar entity ini
 * ({@link #getGelombangPendaftaranPsb()}), dan sifatnya <b>opsional</b> ({@code nullable = true});
 * layar panitia menampilkan "Semua" bila kosong. Karena entity ini TIDAK punya kolom
 * {@code sekolah}/{@code yayasan} sendiri, gelombang inilah satu-satunya jalur penentu tenant —
 * dan jalur itu putus begitu gelombang dibiarkan kosong (lihat "Hal non-obvious" di bawah).</li>
 * <li>{@code JadwalUjianPSB} — <b>kelas kembar</b> entity ini: sama-sama turunan
 * {@link VOPembelajaran}, struktur field nyaris identik, bahkan {@code serialVersionUID}-nya
 * PERSIS SAMA ({@code 2463821577548439808L}) karena hasil salin-tempel. Bedanya: {@code
 * JadwalUjianPSB} menunjuk {@code UjianPSB} (paket soal ujian masuk) dan tidak mengenal kuota
 * maupun pemilihan mandiri, sedangkan entity ini justru berpusat pada
 * {@link #getKuota() kuota} + {@link #getBolehDipilihSendiriOlehCalonSiswa() pemilihan mandiri}.
 * Keduanya dipasang berdampingan sebagai dua tab pada layar gelombang pendaftaran.</li>
 * <li>{@code InterviewCalonSiswa}/{@code InterviewPunyaCalonSiswa} — <b>TIDAK berelasi sama
 * sekali</b> dengan entity ini (diverifikasi: nol referensi silang di kedua arah). Wawancara PSB
 * adalah mekanisme TERPISAH dengan entity sendiri yang jauh lebih kaya (punya kolom tenant
 * {@code sekolah}/{@code yayasan} sendiri, pewawancara {@code Pegawai}, tautan Zoom/BBB/Skype/WA,
 * dan {@code kapasitasRuangan}). Jangan mencampur keduanya: "pertemuan" di sini adalah sesi
 * temu-muka siswa+orang tua (sosialisasi/pembekalan), bukan sesi wawancara seleksi.</li>
 * <li>{@code RuangPSB}/{@code RuangGelombangPendaftaranPsbPSB} — tidak dirujuk entity ini; slot
 * pertemuan ini tidak mengenal ruangan.</li>
 * </ul>
 *
 * <p><b>Warisan {@link VOPembelajaran}.</b> Entity ini bukan sekadar baris katalog: dengan menjadi
 * turunan {@link VOPembelajaran} (rantai
 * {@code VOPembelajaran → VoKunci → ais.database.model.sop.DataSop →}
 * {@link ais.database.model.GeneralValueObject}) ia menjadi salah satu "induk pembelajaran" yang
 * boleh memiliki daftar {@code ais.database.model.Pertemuan}. Kelas induk menyediakan mesin agenda
 * pertemuan bersama ({@code ambilPertemuan()}, {@code belum()}, cache pertemuan, integrasi absensi,
 * kelas virtual) yang dipakai bersama {@code Perkuliahan}, {@code JadwalPelajaran},
 * {@code KelompokKkn}, {@code KelompokPkl}, {@code JadwalUjianPSB}, dan lain-lain. Karena itu kelas
 * ini WAJIB mengimplementasikan tiga anggota abstrak induk:
 * {@link #getCourse()}/{@link #setCourse(String)} (konfigurasi kelas virtual dalam bentuk JSON),
 * {@link #getUrutkanotomatis()}/{@link #setUrutkanotomatis(Boolean)} (mode pengurutan agenda), dan
 * {@link #ambilJumlahDetailperkuliahanLangsung()}.
 *
 * <p><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 * bug.</b> {@link ais.database.model.GeneralValueObject} — dan seluruh kelas perantara di rantai di
 * atas — adalah POJO abstrak biasa, BUKAN {@code @Entity} maupun {@code @MappedSuperclass}
 * (diverifikasi: nol anotasi {@code @MappedSuperclass} di sepanjang rantai). Hibernate karena itu
 * tidak memetakan satu pun properti kelas induk, sehingga setiap entity konkret HARUS
 * mendeklarasikan ulang kolom identitas dan kolom audit miliknya sendiri. Menghapus deklarasi
 * ulang itu akan menghilangkan kolomnya dari pemetaan, bukan merapikan kode.
 *
 * <p><b>Pengelompokan anggota kelas ini.</b>
 * <ol>
 * <li><i>Identitas &amp; audit</i> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 * <li><i>Isi pertemuan</i> — {@link #getNama()} (materi/topik, wajib) dan
 * {@link #getKeterangan()} (catatan bebas yang ikut ditampilkan ke calon siswa).</li>
 * <li><i>Waktu</i> — {@link #getWaktuMulai()} dan {@link #getWaktuSampai()}, keduanya
 * {@code nullable = false}.</li>
 * <li><i>Kapasitas &amp; keterbukaan</i> — {@link #getKuota()} dan
 * {@link #getBolehDipilihSendiriOlehCalonSiswa()}: dua kolom yang menentukan apakah slot muncul di
 * daftar pilihan pendaftar dan berapa banyak yang boleh mengisinya.</li>
 * <li><i>Relasi</i> — {@link #getGelombangPendaftaranPsb()} (opsional) dan
 * {@link #getDikunci()}.</li>
 * <li><i>Kontrak {@link VOPembelajaran}</i> — {@link #getCourse()},
 * {@link #getUrutkanotomatis()}, {@link #ambilJumlahDetailperkuliahanLangsung()}.</li>
 * <li><i>Status</i> — {@link #getAktif()}.</li>
 * </ol>
 *
 * <p><b>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini.</b>
 * <ul>
 * <li><b>Getter yang menulis balik.</b> {@link #getWaktuMulai()}, {@link #getWaktuSampai()}, dan
 * {@link #getAktif()} bukan getter murni: bila nilainya {@code null} mereka MENGISI field terlebih
 * dahulu lalu mengembalikannya. Karena pemetaan memakai <i>property access</i> (anotasi
 * {@code @Id} berada di getter), sekadar merender baris lama yang kolomnya {@code NULL} sudah
 * cukup membuat nilai hasil "coalesce" itu ikut tertulis saat dirty-checking berikutnya.
 * Bandingkan dengan {@link #getCourse()} dan {@link #getKuota()} yang mengembalikan nilai default
 * TANPA menulis balik — perbedaan yang gampang terlewat saat menyalin pola dari satu getter ke
 * getter lain.</li>
 * <li><b>Tidak ada kolom tenant.</b> Entity ini tidak punya {@code sekolah} maupun {@code yayasan}.
 * Query daftar pada layar panitia ({@code JadwalPertemuanPSBAction.initCriteria}) juga tidak
 * menyaring tenant sama sekali, sehingga pada instalasi multi-sekolah daftar slot bersifat global.
 * Konsumen yang menurunkan tenant dari entity ini melakukannya lewat
 * {@code getGelombangPendaftaranPsb().getSekolah()} — pola yang rapuh karena relasi itu boleh
 * {@code null} (lihat catatan pada {@link #getGelombangPendaftaranPsb()}).</li>
 * <li><b>{@link #getDikunci() dikunci} adalah kolom mati untuk entity ini.</b> Kolom pengunci
 * (bertipe {@link Tbmuser}) diwarisi dari pola {@code Perkuliahan}/{@code JadwalUjianPSB} tempat ia
 * benar-benar dipakai untuk mengunci nilai, tetapi TIDAK ADA satu pun kode yang membaca atau
 * mengisinya untuk {@code JadwalPertemuanPSB}. Kolomnya tetap ada di tabel dan tetap ikut diaudit
 * Envers.</li>
 * <li><b>Mesin agenda pertemuan praktis belum tersambung.</b> Dua helper yang membangun agenda
 * {@code Pertemuan} untuk entity ini
 * ({@code ais.action.master.sekolah.helper.AktifitasJadwalPertemuanPSBHelper} dan
 * {@code ais.action.master.sekolah.helper.PenjadwalanPertemuanPSBHelper}) hanya di-instansiasi
 * sebagai field yang tidak pernah dipanggil di {@code JadwalPertemuanPSBAction}, sehingga pada
 * kondisi kode saat ini slot pertemuan PSB tidak pernah memperoleh baris {@code Pertemuan} lewat
 * UI. Konsekuensinya seluruh kontrak {@link VOPembelajaran} pada kelas ini
 * ({@link #getCourse()}, {@link #getUrutkanotomatis()},
 * {@link #ambilJumlahDetailperkuliahanLangsung()}) berstatus laten.</li>
 * </ul>
 *
 * @see VOPembelajaran
 * @see ais.database.model.GeneralValueObject
 * @see CalonSiswa
 * @see GelombangPendaftaranPsb
 * @see JadwalUjianPSB
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jadwal_pertemuan_psb")
public class JadwalPertemuanPSB extends VOPembelajaran {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data ({@code sekolah.jadwal_pertemuan_psb.id}), dihasilkan oleh sequence/identity DB. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini; diisi otomatis oleh interceptor audit. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini; diisi otomatis oleh interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini (kolom audit {@code olehid}).
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah disimpan lewat
	 *         interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna penyimpan terakhir.
	 *
	 * <p><b>Efek samping / perilaku non-obvious:</b> nilai {@code null} atau string kosong
	 * DIABAIKAN diam-diam (method langsung {@code return} tanpa mengubah apa pun). Artinya kolom
	 * audit ini hanya bisa maju, tidak bisa dikosongkan kembali lewat setter. Perilaku ini sengaja
	 * dipakai agar percobaan penyimpanan tanpa konteks pengguna tidak menghapus jejak audit yang
	 * sudah ada.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong tidak berpengaruh
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna penyimpan terakhir.
	 *
	 * <p><b>Efek samping / perilaku non-obvious:</b> sama seperti {@link #setOlehId(String)},
	 * nilai {@code null} atau string kosong DIABAIKAN diam-diam sehingga jejak audit yang sudah ada
	 * tidak pernah terhapus.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong tidak berpengaruh
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini (kolom audit {@code oleh}).
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang menyerahkan pengisian kolom audit
	 * ({@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}) kepada
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} setiap kali baris ini diperbarui.
	 *
	 * <p><b>Catatan gaya kode:</b> deklarasi field {@code tanggal_dirubah} sengaja ditempel pada
	 * baris yang sama oleh generator kode repo ini. Field tersebut diinisialisasi ke waktu server
	 * saat instance dibuat ({@code ais.ui.util.WaktuUtil.getDate()}), sehingga baris baru selalu
	 * punya stempel waktu meski belum sempat melewati interceptor. Jangan memecah baris ini tanpa
	 * alasan kuat — pola identik dipakai di seluruh entity repo.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; umumnya diisi otomatis oleh interceptor audit
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code timestamp}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang dibuat lewat
	 *         konstruktor karena field-nya diinisialisasi ke waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Perilaku non-obvious:</b> method ini membaca FIELD {@code nama} secara langsung, bukan
	 * lewat {@link #getNama()}, sehingga nilainya TIDAK di-{@code trim} seperti pada getter resmi.
	 * Untuk instance yang belum disimpan hasilnya berbentuk {@code "null-null"}. Dipakai antara lain
	 * oleh komponen ZK yang menampilkan objek apa adanya; untuk label yang dilihat pengguna,
	 * gunakan {@link #getNama()}.</p>
	 *
	 * @return gabungan id dan materi pertemuan yang dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Materi/topik pertemuan; kolom WAJIB, dilabeli "Materi Pertemuan" pada layar panitia. */
	private String nama;
	/** Catatan bebas pertemuan; ikut ditampilkan kepada calon siswa yang sudah memilih slot ini. */
	private String keterangan;
	/** Batas jumlah calon siswa yang boleh memilih slot ini; {@code null} diperlakukan sebagai 10. */
	private Integer kuota;
	/** Menentukan apakah slot ini muncul di daftar pilihan mandiri calon siswa pada formulir PPDB. */
	private Boolean bolehDipilihSendiriOlehCalonSiswa;
	/** Gelombang pendaftaran pemilik slot ini; OPSIONAL — {@code null} berarti "Semua" gelombang. */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;

	/** Waktu mulai sesi pertemuan (kolom wajib). */
	private Date waktuMulai;
	/** Waktu berakhir sesi pertemuan (kolom wajib); dipakai juga sebagai penyaring slot kedaluwarsa. */
	private Date waktuSampai;

	/** Pengguna pengunci baris — diwarisi dari pola {@code Perkuliahan}, TIDAK dipakai kode mana pun untuk entity ini. */
	private Tbmuser dikunci;

	/**
	 * Mengembalikan pengguna yang "mengunci" slot pertemuan ini (kolom FK {@code dikunci}).
	 *
	 * <p><b>Status pemakaian (TERVERIFIKASI):</b> tidak ada satu pun kode di repo yang membaca atau
	 * mengisi properti ini untuk {@code JadwalPertemuanPSB}. Kolom dan pemetaannya ada karena
	 * disalin dari kerabatnya ({@code Perkuliahan}, {@code JadwalUjianPSB}) tempat pola
	 * "{@code dikunci != null} berarti data terkunci dan kontrol edit disembunyikan" memang aktif.
	 * Perlakukan sebagai kolom laten: aman dibaca, tetapi jangan berasumsi ada mekanisme kunci yang
	 * benar-benar berjalan untuk jadwal pertemuan PSB.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} sehingga proxy lazy Hibernate diresolusi
	 * menjadi instance nyata dan field lokal ditimpa hasil resolusi tersebut.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} (nilai normal untuk entity ini)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menetapkan pengguna pengunci slot pertemuan ini.
	 *
	 * @param dikunci pengguna pengunci; boleh {@code null}
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate dan dipakai layar panitia saat menekan
	 * tombol "Tambah". Seluruh kolom dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang
	 * langsung terisi waktu server; nilai default kolom lain baru muncul saat getter-nya dipanggil
	 * (lihat {@link #getWaktuMulai()}, {@link #getKuota()},
	 * {@link #getBolehDipilihSendiriOlehCalonSiswa()}, {@link #getAktif()}).
	 */
	public JadwalPertemuanPSB() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dihasilkan basis data
	 * ({@link javax.persistence.GenerationType#IDENTITY}). Id dipakai antara lain sebagai parameter
	 * URL saat layar panitia menyisipkan daftar calon siswa pemilih slot ini
	 * ({@code calon_siswa.zul?jadwalPertemuanPSB=<id>}).</p>
	 *
	 * @return id baris, atau {@code null} untuk instance yang belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris ini. Hanya untuk keperluan Hibernate/penyalinan data; kode
	 * aplikasi tidak boleh mengarang id sendiri.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan materi/topik pertemuan — kolom WAJIB ({@code nullable = false}, tipe
	 * {@code text}) yang menjadi identitas slot di mata pengguna.
	 *
	 * <p>Dilabeli <i>"Materi Pertemuan"</i> pada layar panitia dan dipakai sebagai teks pilihan pada
	 * combobox calon siswa, judul dialog agenda, serta label riwayat revisi. Nilai dikembalikan
	 * dalam bentuk sudah di-{@code trim}; {@code null} tetap dikembalikan sebagai {@code null}
	 * (bukan string kosong) — berbeda dari {@link #getKeterangan()}.</p>
	 *
	 * @return materi pertemuan tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan materi/topik pertemuan. Validasi "harus diisi" dilakukan di lapisan UI
	 * ({@code JadwalPertemuanPSBAction.onSave}), bukan di sini.
	 *
	 * @param nama materi pertemuan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas pertemuan (kolom {@code text}, opsional).
	 *
	 * <p><b>Perilaku non-obvious:</b> mengembalikan string KOSONG (bukan {@code null}) bila kolom
	 * belum diisi. Pemanggil memang mengandalkan kontrak ini —
	 * {@code CalonSiswaAction} langsung memanggil {@code getKeterangan().isEmpty()} tanpa
	 * pemeriksaan {@code null} untuk memutuskan apakah baris keterangan ditampilkan kepada calon
	 * siswa yang sudah terkunci pada satu slot. Mengubah nilai kembalian menjadi {@code null} akan
	 * langsung menimbulkan {@code NullPointerException} di layar biodata calon siswa.</p>
	 *
	 * <p>Berbeda dari beberapa entity katalog lain di modul ini, properti {@code keterangan} di sini
	 * BENAR-BENAR dipetakan ke kolom sendiri, sehingga isinya bertahan antar-request.</p>
	 *
	 * @return keterangan pertemuan, atau string kosong bila belum diisi (tidak pernah {@code null})
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * Menetapkan catatan bebas pertemuan.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan waktu mulai sesi pertemuan (kolom WAJIB, {@code nullable = false}).
	 *
	 * <p><b>Efek samping — getter yang menulis balik:</b> bila field masih {@code null}, method ini
	 * MENGISINYA dengan waktu server saat itu ({@code ais.ui.util.WaktuUtil.getDate()}) sebelum
	 * mengembalikan nilainya. Untuk instance baru efek ini berguna (form "Tambah Jadwal Pertemuan"
	 * langsung terisi waktu sekarang), tetapi untuk baris yang sudah tersimpan dan kolomnya
	 * {@code NULL} — misalnya hasil migrasi atau {@code INSERT} SQL mentah — sekadar MEMBACA baris
	 * itu sudah mengotori entity sehingga dirty-checking Hibernate berpotensi menuliskan waktu
	 * "sekarang" ke basis data. Jangan memanggil getter ini di jalur yang seharusnya murni baca
	 * tanpa menyadari konsekuensi tersebut.</p>
	 *
	 * <p>Dipakai untuk mengurutkan daftar slot (urut naik) baik di layar panitia maupun di combobox
	 * pilihan calon siswa.</p>
	 *
	 * @return waktu mulai; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	@Column(nullable = false)
	public Date getWaktuMulai() {
		if (waktuMulai == null) {
			waktuMulai = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuMulai;
	}

	/**
	 * Menetapkan waktu mulai sesi pertemuan.
	 *
	 * @param waktuMulai waktu mulai; validasi "harus diisi" ditegakkan di lapisan UI
	 */
	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengembalikan waktu berakhir sesi pertemuan (kolom WAJIB, {@code nullable = false}).
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getWaktuMulai()} — mengisi field dengan waktu
	 * server bila masih {@code null} sebelum mengembalikannya.</p>
	 *
	 * <p><b>Peran bisnis:</b> kolom ini adalah penyaring kedaluwarsa slot. Formulir pendaftaran
	 * ({@code CalonSiswaAction} dan {@code PPDB_Simple6}) hanya menampilkan slot dengan
	 * {@code waktuSampai &gt; sekarang}, dan layar panitia menyediakan centang "Tampilkan hanya
	 * belum terlewat" yang memakai perbandingan yang sama. Jadi memundurkan nilai ini menyembunyikan
	 * slot dari pendaftar tanpa perlu menonaktifkannya lewat {@link #getAktif()}.</p>
	 *
	 * @return waktu berakhir; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	@Column(nullable = false)
	public Date getWaktuSampai() {
		if (waktuSampai == null) {
			waktuSampai = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuSampai;
	}

	/**
	 * Menetapkan waktu berakhir sesi pertemuan.
	 *
	 * @param waktuSampai waktu berakhir; validasi "harus diisi" ditegakkan di lapisan UI
	 */
	public void setWaktuSampai(Date waktuSampai) {
		this.waktuSampai = waktuSampai;
	}

	/**
	 * Mengembalikan gelombang pendaftaran PSB pemilik slot pertemuan ini.
	 *
	 * <p><b>Relasi OPSIONAL.</b> Kolom FK {@code gelombang_pendaftaran_psb} dipetakan
	 * {@code nullable = true} dan layar panitia secara eksplisit menampilkan teks "Semua" bila
	 * kosong. Pada sisi pendaftar, slot tanpa gelombang justru TIDAK PERNAH muncul: combobox
	 * memakai pembanding kesamaan terhadap gelombang yang sedang dipilih, sehingga baris
	 * ber-gelombang {@code null} tidak pernah cocok. Dengan kata lain "Semua" pada layar panitia
	 * dan perilaku nyata pada formulir pendaftaran tidak sejalan.</p>
	 *
	 * <p><b>Konsekuensi tenant.</b> Karena entity ini tidak punya kolom {@code sekolah}/
	 * {@code yayasan} sendiri, relasi inilah satu-satunya sumber informasi tenant. Kode yang
	 * menurunkan tenant dari sini — misalnya {@code ais.action.master.helper.HasilUjianHelper} yang
	 * memanggil {@code getGelombangPendaftaranPsb().getSekolah()} — melakukannya TANPA pemeriksaan
	 * {@code null}, padahal relasi ini sah bernilai {@code null}. Perlakukan setiap dereferensi
	 * berantai atas properti ini sebagai jalur yang harus dijaga {@code null}-nya.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy dan menimpa field
	 * lokal dengan hasilnya.</p>
	 *
	 * @return gelombang pendaftaran pemilik slot, atau {@code null} bila slot tidak diikat ke
	 *         gelombang mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_psb", nullable = true)
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menetapkan gelombang pendaftaran PSB pemilik slot pertemuan ini.
	 *
	 * @param gelombangPendaftaranPsb gelombang pendaftaran; boleh {@code null} ("Semua" di layar
	 *                                panitia, tetapi berarti tak terpilih di formulir pendaftaran)
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: jumlah "detail perkuliahan langsung" yang
	 * dianggap dimiliki induk pembelajaran ini.
	 *
	 * <p><b>Perilaku sebenarnya:</b> selalu mengembalikan {@code 1} (badan method masih berupa stub
	 * hasil pembuatan otomatis IDE, ditandai komentar {@code TODO}). Untuk kerabatnya seperti
	 * {@code Perkuliahan} angka ini dihitung dari jumlah pertemuan tatap muka; di sini nilainya
	 * konstan karena slot pertemuan PSB hanya mewakili satu sesi. Nilai ini dipakai mesin agenda
	 * kelas induk, yang untuk entity ini praktis belum tersambung ke UI mana pun.</p>
	 *
	 * @return selalu {@code 1}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	/** Konfigurasi kelas virtual dalam bentuk teks JSON; diisi {@code ais.common.classroom.ClassRoomUtil}. */
	private String course;
	/** Mode pengurutan agenda pertemuan milik kelas induk; {@code null} diperlakukan sebagai {@code true}. */
	private Boolean urutkanotomatis;
	/** Penanda baris aktif; {@code null} diperlakukan sebagai {@code true} dan DITULIS BALIK oleh getter. */
	private Boolean aktif;

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: konfigurasi ruang kelas virtual milik slot
	 * pertemuan ini, disimpan sebagai teks JSON pada kolom bertipe {@code text}.
	 *
	 * <p>Isinya ditulis {@code ais.common.classroom.ClassRoomUtil} ketika pengguna membuat
	 * pertemuan daring (tautan konferensi, identitas ruang, dan sejenisnya) untuk induk
	 * pembelajaran mana pun. Untuk {@code JadwalPertemuanPSB} jalur itu hanya dapat dicapai lewat
	 * tombol ruang kelas virtual pada helper agenda yang saat ini tidak terpasang di UI, sehingga
	 * kolom ini pada praktiknya selalu berisi objek JSON kosong.</p>
	 *
	 * <p><b>Perilaku non-obvious:</b> bila kolom {@code null} atau hanya berisi spasi, method
	 * mengembalikan representasi objek JSON KOSONG ({@code "{}"}) agar pemanggil selalu menerima
	 * JSON yang sah dan tidak perlu berjaga terhadap {@code null}. Berbeda dari
	 * {@link #getWaktuMulai()} dan {@link #getAktif()}, nilai default ini TIDAK ditulis balik ke
	 * field sehingga getter ini tidak mengotori entity.</p>
	 *
	 * @return teks JSON konfigurasi kelas virtual; tidak pernah {@code null} maupun kosong
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Menetapkan konfigurasi ruang kelas virtual (teks JSON) untuk slot pertemuan ini.
	 *
	 * @param course teks JSON konfigurasi; boleh {@code null}/kosong (getter akan menggantinya
	 *               dengan objek JSON kosong)
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Mengembalikan kuota peserta slot pertemuan ini — batas jumlah calon siswa yang boleh memilih
	 * slot yang sama.
	 *
	 * <p><b>Perilaku non-obvious:</b> bila kolom {@code null}, method mengembalikan {@code 10}
	 * sebagai default. Nilai default itu TIDAK ditulis balik ke field, jadi kolom di basis data
	 * tetap {@code NULL} sampai panitia menyimpan angkanya sendiri lewat layar. Akibatnya slot yang
	 * dibuat tanpa mengisi kuota diam-diam berperilaku seolah berkuota 10 — bukan "tanpa batas".</p>
	 *
	 * <p><b>Cara kuota ditegakkan (di luar kelas ini).</b> Entity ini tidak menyimpan daftar
	 * peserta; formulir pendaftaran menghitung sendiri jumlah {@link CalonSiswa} lain yang sudah
	 * memilih slot tersebut, lalu membandingkannya dengan nilai kembalian method ini untuk
	 * memutuskan apakah tombol simpan/daftar ditampilkan dan apakah peringatan "kuota penuh"
	 * dimunculkan. Perhitungan itu dijalankan saat pilihan combobox BERUBAH, bukan saat penyimpanan,
	 * sehingga kuota bersifat penjaga tampilan, bukan batasan yang ditegakkan di lapisan data.</p>
	 *
	 * @return kuota peserta; {@code 10} bila kolom belum diisi
	 */
	public Integer getKuota() {
		return kuota == null ? 10 : kuota;
	}

	/**
	 * Menetapkan kuota peserta slot pertemuan ini.
	 *
	 * @param kuota batas jumlah peserta; {@code null} akan dibaca sebagai {@code 10} oleh
	 *              {@link #getKuota()}
	 */
	public void setKuota(Integer kuota) {
		this.kuota = kuota;
	}

	/**
	 * Menyatakan apakah slot ini boleh dipilih sendiri oleh calon siswa pada formulir pendaftaran.
	 *
	 * <p><b>Peran bisnis:</b> inilah saklar yang menentukan visibilitas slot bagi pendaftar. Query
	 * combobox pada {@code CalonSiswaAction} maupun {@code PPDB_Simple6} menyaring dengan kesamaan
	 * ketat terhadap nilai {@code true}, digabung dengan syarat slot masih aktif, belum terlewat
	 * ({@link #getWaktuSampai()} melewati waktu sekarang), dan gelombangnya cocok. Slot bernilai
	 * {@code false} hanya bisa ditetapkan panitia dari layar administrasi.</p>
	 *
	 * <p><b>Perilaku non-obvious:</b> getter mengembalikan {@code true} bila kolom {@code null},
	 * TANPA menulis balik ke field. Karena query pendaftar memakai perbandingan kesamaan langsung
	 * ke kolom (bukan lewat getter ini), baris yang kolomnya benar-benar {@code NULL} di basis data
	 * — misalnya hasil migrasi atau {@code INSERT} SQL mentah — akan terlihat "boleh dipilih" di
	 * layar panitia tetapi TIDAK PERNAH muncul di daftar pilihan calon siswa. Perbedaan antara
	 * default di lapisan Java dan penyaringan di lapisan SQL ini adalah sumber kebingungan yang
	 * mudah terlewat.</p>
	 *
	 * @return {@code true} bila slot terbuka untuk dipilih sendiri oleh calon siswa (juga saat
	 *         kolom belum pernah diisi), {@code false} bila hanya panitia yang boleh menetapkannya
	 */
	public Boolean getBolehDipilihSendiriOlehCalonSiswa() {
		return bolehDipilihSendiriOlehCalonSiswa == null ? true : bolehDipilihSendiriOlehCalonSiswa;
	}

	/**
	 * Menetapkan apakah slot ini boleh dipilih sendiri oleh calon siswa.
	 *
	 * @param bolehDipilihSendiriOlehCalonSiswa {@code true} agar slot muncul di daftar pilihan
	 *                                          pendaftar; {@code null} dibaca sebagai {@code true}
	 *                                          oleh getter tetapi TIDAK cocok pada penyaringan SQL
	 */
	public void setBolehDipilihSendiriOlehCalonSiswa(Boolean bolehDipilihSendiriOlehCalonSiswa) {
		this.bolehDipilihSendiriOlehCalonSiswa = bolehDipilihSendiriOlehCalonSiswa;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: mode pengurutan daftar pertemuan milik induk
	 * pembelajaran ini.
	 *
	 * <p>Kelas induk dan helper penjadwalan bersama memakai nilai ini untuk memilih kunci urut
	 * agenda — {@code true} berarti urut menurut tanggal (otomatis), {@code false} berarti urut
	 * menurut nomor urut pertemuan yang ditetapkan manual. Badan method masih berupa stub hasil
	 * pembuatan otomatis IDE (komentar {@code TODO}) yang mengembalikan {@code true} bila kolom
	 * {@code null}, tanpa menulis balik. Karena mesin agenda untuk entity ini praktis belum
	 * tersambung ke UI, nilainya belum pernah benar-benar dipakai untuk jadwal pertemuan PSB.</p>
	 *
	 * @return {@code true} bila agenda diurutkan otomatis (juga saat kolom belum diisi)
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Menetapkan mode pengurutan daftar pertemuan.
	 *
	 * @param urutkanotomatis {@code true} untuk urut otomatis menurut tanggal; boleh {@code null}
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	/**
	 * Mengembalikan penanda aktif slot pertemuan ini.
	 *
	 * <p><b>Peran bisnis:</b> slot non-aktif disembunyikan dari daftar panitia (centang "Tampilkan
	 * hanya yang aktif", menyala secara baku) dan dari combobox pilihan calon siswa. Nilai ini juga
	 * dibaca lapisan penyajian lain — {@code ais.action.servlet.api.PsbCalonApi} dan
	 * {@code ais.action.master.TampilanPengumumanAkademisAction} hanya menampilkan detail jadwal
	 * pertemuan kepada calon siswa bila slotnya masih aktif — sehingga menonaktifkan slot juga
	 * menghilangkan informasi jadwal dari pengumuman dan API pendaftar.</p>
	 *
	 * <p><b>Efek samping — getter yang menulis balik:</b> berbeda dari
	 * {@link #getBolehDipilihSendiriOlehCalonSiswa()} dan {@link #getUrutkanotomatis()} yang hanya
	 * mengembalikan default, method ini MENGISI field dengan {@code true} bila masih {@code null}.
	 * Karena pemetaan memakai <i>property access</i>, membaca baris lama yang kolomnya {@code NULL}
	 * dapat membuat nilai {@code true} ikut tertulis pada penyimpanan berikutnya. Perlu diperhatikan
	 * bahwa query penyaring di layar dan formulir tetap menuliskan syarat "kolom {@code NULL} ATAU
	 * bernilai {@code true}", jadi lapisan SQL sudah bertoleransi terhadap {@code NULL} tanpa
	 * bergantung pada penulisan balik ini.</p>
	 *
	 * @return {@code true} bila slot aktif (juga saat kolom belum pernah diisi)
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan penanda aktif slot pertemuan ini.
	 *
	 * <p>Dipanggil langsung dari renderer baris layar panitia setiap kali centang "Aktif" diubah,
	 * lalu disusul penyimpanan segera — perubahan berlaku tanpa menekan tombol Simpan.</p>
	 *
	 * @param aktif {@code true} agar slot tetap tampil di daftar panitia, pilihan pendaftar,
	 *              pengumuman, dan API calon siswa
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
