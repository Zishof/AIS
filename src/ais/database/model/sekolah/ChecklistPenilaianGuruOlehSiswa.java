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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk tabel {@code public.checklist_penilaian_guru_oleh_siswa}, yaitu baris
 * <b>transaksi</b> (hasil pengisian angket), <b>bukan</b> master butir pertanyaan. Satu baris =
 * satu jawaban yang diberikan seorang {@link #getSiswa()} terhadap seorang {@link #getGuru()}
 * atas <b>satu butir</b> checklist penilaian guru ({@link #getChecklistPenilaianGuru()}), pada
 * {@link #getTahunAkademik()}/{@link #getSemester()}, dengan konteks opsional satu
 * {@link #getJadwalPelajaran()} (mata pelajaran/kelas tempat guru itu mengajar). Skor tersimpan
 * pada {@link #getNilai()} memakai skala konstanta kelas ini: {@link #SANGAT_BAIK} (1) sampai
 * {@link #BURUK} (5) — perhatikan bahwa <b>angka kecil berarti lebih baik</b>. Kolom
 * {@link #getKeterangan()} menampung komentar bebas siswa untuk butir tersebut.
 *
 * <h2>Posisi dalam keluarga angket guru (jenjang sekolah)</h2>
 * <p>Master butir pertanyaannya adalah {@code ChecklistPenilaianGuru} (tabel
 * {@code sekolah.checklist_penilaian_guru}), yang sendirinya bernaung di bawah
 * {@link GrupChecklistPenilaianGuru} (kelompok/aspek penilaian). Entity ini adalah padanan
 * jenjang sekolah dari {@link ais.database.model.ChecklistPenilaianDosenOlehMahasiswa} pada
 * jenjang perguruan tinggi — struktur field, konstanta skala, dan pola relasinya nyaris identik
 * kata-per-kata, termasuk nilai {@link #serialVersionUID} yang sama persis (warisan salin-tempel
 * generator, bukan makna semantik).</p>
 *
 * <h2>Status pemakaian: skema LAMA yang sudah tidak ditulis maupun dibaca kode</h2>
 * <p>Penting bagi pembaca kode: berdasarkan penelusuran seluruh pohon sumber, <b>tidak ada satu
 * pun kelas Java lain yang merujuk tipe ini</b> — tidak ada Action, helper, DAO, laporan, maupun
 * API yang membaca atau menyimpannya. Yang tetap hidup hanyalah pendaftarannya di
 * {@code hibernate.cfg.xml} (sehingga tabel dan tabel audit Envers-nya tetap dibuat/dipetakan)
 * dan sebuah halaman CRUD generik hasil generator di
 * {@code WEB-INF/baru/modul/pagesmastersekolahchecklistpenilaianguruolehsiswazul/index.jsp}.</p>
 * <p>Jalur angket guru yang benar-benar berjalan saat ini memakai entity
 * {@link ChecklistBaruPenilaianGuruOlehSiswa}, yang menyimpan <b>seluruh</b> jawaban satu siswa
 * atas satu guru pada satu jadwal <b>terpadatkan dalam satu kolom teks</b>
 * ({@code "DATA<idButir>;<nilai><>keterangan"} dipisah {@code "___"}) alih-alih satu baris per
 * butir seperti di sini. Relasi {@link #getChecklistBaruPenilaianGuruOlehSiswa()} adalah
 * jembatan/penanda migrasi dari baris lama ini ke baris rekap versi baru tersebut.</p>
 * <p>Konsekuensi praktis: tabel ini pada instalasi baru diperkirakan selalu kosong, dan
 * menambahkan pembaca/penulis baru untuk tipe ini justru akan memecah data angket ke dua skema
 * yang tidak saling tahu. Kode baru sebaiknya memakai {@link ChecklistBaruPenilaianGuruOlehSiswa}.</p>
 *
 * <h2>Anonimitas responden</h2>
 * <p>Perlu disadari saat memakai/melaporkan data ini: kolom {@code siswa} dideklarasikan
 * {@code nullable = false}, sehingga identitas siswa penilai <b>selalu</b> tersimpan dan
 * tergandeng langsung ke skor yang ia berikan kepada guru tertentu. Tidak ada field penanda
 * anonim, penyamaran, maupun agregasi pada level penyimpanan — anonimitas angket, bila
 * dikehendaki, sepenuhnya bergantung pada lapisan tampilan/laporan yang membacanya. Hal yang
 * sama berlaku pada skema penggantinya, {@link ChecklistBaruPenilaianGuruOlehSiswa}, yang juga
 * menyimpan relasi {@code siswa} wajib.</p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ul>
 *   <li><b>Konstanta skala nilai</b>: {@link #SANGAT_BAIK}, {@link #BAIK}, {@link #CUKUP},
 *       {@link #KURANG_BAIK}, {@link #BURUK}.</li>
 *   <li><b>Jejak audit ringan</b> (dideklarasikan ulang dari base class, lihat catatan di bawah):
 *       {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@link #onUpdate()}.</li>
 *   <li><b>Identitas baris</b>: {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Isi penilaian</b>: {@link #getNilai()}, {@link #getKeterangan()}.</li>
 *   <li><b>Konteks periode</b>: {@link #getTahunAkademik()}, {@link #getSemester()}.</li>
 *   <li><b>Relasi</b>: {@link #getSiswa()} (penilai), {@link #getGuru()} (yang dinilai),
 *       {@link #getChecklistPenilaianGuru()} (butir yang dijawab),
 *       {@link #getJadwalPelajaran()} (konteks mengajar, opsional),
 *       {@link #getChecklistBaruPenilaianGuruOlehSiswa()} (jembatan ke skema baru, opsional).</li>
 *   <li><b>Lain-lain</b>: konstruktor kosong {@link #ChecklistPenilaianGuruOlehSiswa()} dan
 *       {@link #toString()}.</li>
 * </ul>
 *
 * <h2>Hal-hal non-obvious</h2>
 * <ul>
 *   <li><b>Skema tabel berbeda dari kerabatnya.</b> Tabel ini berada di schema {@code public},
 *       sedangkan {@code checklist_penilaian_guru},
 *       {@code checklist_baru_penilaian_guru_oleh_siswa}, dan master jenjang sekolah lain berada
 *       di schema {@code sekolah}. Ini konsisten dengan padanan jenjang PT-nya (yang juga di
 *       {@code public}) dan merupakan sisa penempatan lama, bukan salah ketik yang aman
 *       diperbaiki begitu saja: mengubahnya memindahkan tabel.</li>
 *   <li><b>Field {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 *       dideklarasikan ulang di kelas ini padahal ada di {@link GeneralValueObject}.</b> Itu
 *       <b>bukan</b> duplikasi keliru melainkan keharusan teknis: {@link GeneralValueObject}
 *       adalah POJO abstrak biasa, bukan {@code @Entity} maupun
 *       {@code @MappedSuperclass}, sehingga Hibernate tidak memetakan properti milik induk.
 *       Tanpa deklarasi ulang di sini, keempat kolom tersebut tidak akan pernah dipetakan.</li>
 *   <li><b>Pemanggilan {@code check(...)} tidak simetris.</b> {@link #getSiswa()},
 *       {@link #getGuru()}, dan {@link #getChecklistPenilaianGuru()} meresolusi proxy lazy lewat
 *       {@link GeneralValueObject#check(Object)}, sedangkan {@link #getJadwalPelajaran()} dan
 *       {@link #getChecklistBaruPenilaianGuruOlehSiswa()} tidak. Perbedaannya beralasan: dua
 *       relasi terakhir memakai {@code @ManyToOne} tanpa {@code fetch = LAZY} (jadi eager)
 *       ditambah {@link Fetch}{@code (}{@link FetchMode#SELECT}{@code )}, sehingga sudah berupa
 *       object nyata saat getter dipanggil.</li>
 *   <li><b>{@link #toString()} dapat mengembalikan {@code null}</b> karena membaca field
 *       {@code keterangan} yang {@code nullable} secara langsung tanpa penjagaan. Padanan
 *       jenjang PT-nya ({@link ais.database.model.ChecklistPenilaianDosenOlehMahasiswa#toString()})
 *       mengembalikan string kosong pada kasus yang sama — penyimpangan kecil yang membuat
 *       pemakaian tipe ini di komponen UI (label, combobox, {@code Listcell}) berpotensi
 *       memunculkan teks {@code "null"} atau {@code NullPointerException} pada pemanggil yang
 *       merantai method {@code String}.</li>
 *   <li><b>Konstanta {@link #SANGAT_BAIK}..{@link #BURUK} tidak dipakai kode mana pun</b> saat
 *       ini (konsekuensi status yatim di atas); nilai skor pada jalur aktif diparsing sebagai
 *       {@code Integer} mentah dari kolom teks {@link ChecklistBaruPenilaianGuruOlehSiswa}.</li>
 *   <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} menolak nilai kosong secara
 *       senyap</b>, sehingga jejak "diubah oleh" tidak pernah bisa dikosongkan kembali setelah
 *       terisi.</li>
 * </ul>
 *
 * <p>Perubahan (create/update/delete) tercatat historisnya lewat anotasi {@link Audited}
 * (Hibernate Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat
 * callback {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.</p>
 *
 * @see ChecklistBaruPenilaianGuruOlehSiswa
 * @see GrupChecklistPenilaianGuru
 * @see ais.database.model.ChecklistPenilaianDosenOlehMahasiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "checklist_penilaian_guru_oleh_siswa")
public class ChecklistPenilaianGuruOlehSiswa extends GeneralValueObject {

	/** Skor "sangat baik" (nilai terbaik) pada skala {@link #getNilai()}; angka kecil = lebih baik. */
	public static final Integer SANGAT_BAIK = 1;
	/** Skor "baik" pada skala {@link #getNilai()}. */
	public static final Integer BAIK = 2;
	/** Skor "cukup" pada skala {@link #getNilai()}. */
	public static final Integer CUKUP = 3;
	/** Skor "kurang baik" pada skala {@link #getNilai()}. */
	public static final Integer KURANG_BAIK = 4;
	/** Skor "buruk" (nilai terendah) pada skala {@link #getNilai()}. */
	public static final Integer BURUK = 5;

	/**
	 * Penanda versi serialisasi Java. Nilainya sama persis dengan sejumlah entity angket lain
	 * (mis. {@link ChecklistBaruPenilaianGuruOlehSiswa} dan padanan jenjang PT-nya) karena
	 * disalin dari cetakan generator {@code hbm2java}; kesamaan itu tidak punya arti khusus.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, di-generate database ({@code IDENTITY}); lihat {@link #getId()}. */
	private Long id;
	/** Nama/label pengguna terakhir yang menyimpan baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Identitas (id/username) pengguna terakhir yang menyimpan baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas (id/username) pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return identitas penyimpan terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna penyimpan terakhir. Nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan secara senyap</b> (nilai lama dipertahankan), sehingga field ini tidak dapat
	 * dikosongkan kembali setelah terisi.
	 *
	 * @param olehId identitas pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama/label pengguna penyimpan terakhir. Sama seperti {@link #setOlehId(String)},
	 * nilai {@code null} atau kosong/spasi <b>diabaikan secara senyap</b>.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/label pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate}: dipanggil Hibernate tepat sebelum
	 * {@code UPDATE} baris ini dikirim ke database, dan mendelegasikan pembaruan stempel waktu
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} (mengisi
	 * {@link #setTanggal_dirubah(Date)} beserta {@code oleh}/{@code olehId} bila konteks
	 * pengguna tersedia).
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja diletakkan pada baris yang sama oleh
	 * generator; nilai awalnya adalah waktu server saat object dibuat
	 * ({@link ais.ui.util.WaktuUtil#getDate()}), sehingga baris yang baru di-{@code INSERT} pun
	 * sudah punya stempel waktu meski callback ini belum pernah berjalan.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan; biasanya diisi otomatis oleh {@link #onUpdate()}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (kolom {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object baru karena
	 *         diinisialisasi ke waktu pembuatan object
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini, yaitu isi komentar bebas {@link #getKeterangan()} apa adanya.
	 * <p><b>Perhatian:</b> membaca field secara langsung tanpa penjagaan, sehingga method ini
	 * <b>dapat mengembalikan {@code null}</b> bila kolom {@code keterangan} (yang memang
	 * {@code nullable}) belum diisi — berbeda dari padanan jenjang PT-nya yang mengembalikan
	 * string kosong. Pemanggil di lapisan UI sebaiknya tidak mengandalkan hasilnya bukan-null.
	 *
	 * @return isi keterangan/komentar, bisa {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/** Siswa yang <b>memberikan</b> penilaian (responden); kolom wajib, lihat {@link #getSiswa()}. */
	private Siswa siswa;
	/** Tahun akademik/tahun ajaran pengisian angket, mis. {@code "2025/2026"}; lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Semester pengisian angket; lihat {@link #getSemester()}. */
	private Integer semester;
	/** Skor yang diberikan untuk butir ini; lihat konstanta {@link #SANGAT_BAIK}..{@link #BURUK}. */
	private Integer nilai;
	/** Guru yang <b>dinilai</b>; kolom wajib, lihat {@link #getGuru()}. */
	private Guru guru;
	/** Konteks jadwal pelajaran (mata pelajaran/kelas) penilaian ini, opsional; lihat {@link #getJadwalPelajaran()}. */
	private JadwalPelajaran jadwalPelajaran;
	/** Butir/pertanyaan master yang dijawab oleh baris ini; lihat {@link #getChecklistPenilaianGuru()}. */
	private ChecklistPenilaianGuru checklistPenilaianGuru;
	/** Komentar bebas siswa untuk butir ini, opsional; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Jembatan ke baris rekap skema baru, opsional; lihat {@link #getChecklistBaruPenilaianGuruOlehSiswa()}. */
	private ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi lewat refleksi. Seluruh
	 * field dibiarkan pada nilai bawaannya, kecuali {@code tanggal_dirubah} yang langsung diisi
	 * waktu server saat object dibuat.
	 */
	public ChecklistPenilaianGuruOlehSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini. Kolom dipetakan {@code insertable = false} karena
	 * nilainya di-generate database dengan strategi {@code IDENTITY} (sekuens/serial), jadi
	 * bernilai {@code null} sampai baris benar-benar tersimpan.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate; kode aplikasi tidak
	 * perlu menyetelnya sendiri.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan komentar/keterangan bebas yang ditulis siswa untuk butir penilaian ini.
	 *
	 * @return isi keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel komentar/keterangan bebas untuk butir penilaian ini.
	 *
	 * @param keterangan isi komentar; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan siswa yang memberikan penilaian ini (responden angket), setelah meresolusi
	 * proxy lazy lewat {@link GeneralValueObject#check(Object)} dan menuliskan hasilnya kembali
	 * ke field agar resolusi tidak berulang.
	 * <p>Kolom {@code siswa} bersifat wajib ({@code nullable = false}): identitas responden
	 * selalu terekam bersama skor yang ia berikan — lihat bagian "Anonimitas responden" pada
	 * javadoc kelas.
	 *
	 * @return siswa penilai; secara skema tidak pernah {@code null} untuk baris tersimpan yang
	 *         relasinya berhasil diresolusi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa penilai (responden) baris ini.
	 *
	 * @param siswa siswa yang mengisi angket
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan guru yang dinilai pada baris ini, setelah meresolusi proxy lazy lewat
	 * {@link GeneralValueObject#check(Object)} dan menuliskan hasilnya kembali ke field.
	 * Kolom {@code guru} bersifat wajib ({@code nullable = false}).
	 *
	 * @return guru yang dinilai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = false)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menyetel guru yang dinilai pada baris ini.
	 *
	 * @param guru guru sasaran penilaian
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan butir/pertanyaan master yang dijawab oleh baris ini (satu baris = satu
	 * butir), setelah meresolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} dan
	 * menuliskan hasilnya kembali ke field. Kolom {@code checklist_penilaian_guru} bersifat
	 * wajib ({@code nullable = false}).
	 * <p>Butir master tersebut ({@code ChecklistPenilaianGuru}) menyimpan teks pertanyaan,
	 * bobot, opsi jawaban, dan kelompok/aspeknya ({@link GrupChecklistPenilaianGuru}).
	 *
	 * @return butir checklist yang dijawab
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_penilaian_guru", nullable = false)
	public ChecklistPenilaianGuru getChecklistPenilaianGuru() {
		checklistPenilaianGuru = check(checklistPenilaianGuru);
		return checklistPenilaianGuru;
	}

	/**
	 * Menyetel butir/pertanyaan master yang dijawab baris ini.
	 *
	 * @param checklistPenilaianGuru butir checklist penilaian guru
	 */
	public void setChecklistPenilaianGuru(ChecklistPenilaianGuru checklistPenilaianGuru) {
		this.checklistPenilaianGuru = checklistPenilaianGuru;
	}

	/**
	 * Mengembalikan tahun akademik/tahun ajaran saat angket ini diisi (kolom wajib), mengikuti
	 * format tahun ajaran yang dipakai seluruh aplikasi, mis. {@code "2025/2026"}. Disimpan
	 * sebagai teks, bukan relasi, sehingga pemfilteran periode dilakukan dengan pembandingan
	 * string persis.
	 *
	 * @return tahun akademik pengisian
	 */
	@Column(name = "tahun_akademik", nullable = false)
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik/tahun ajaran pengisian angket.
	 *
	 * @param tahunAkademik tahun ajaran, mis. {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan semester saat angket ini diisi (kolom wajib). Pada keluarga angket, nilai
	 * ganjil/genap semester dipakai untuk mencocokkan baris dengan periode berjalan.
	 *
	 * @return nomor semester pengisian
	 */
	@Column(name = "semester", nullable = false)
	public Integer getSemester() {
		return semester;
	}

	/**
	 * Menyetel semester pengisian angket.
	 *
	 * @param semester nomor semester
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan skor yang diberikan siswa untuk butir ini (kolom wajib), memakai skala
	 * konstanta kelas ini: {@link #SANGAT_BAIK} (1) sampai {@link #BURUK} (5) — <b>angka lebih
	 * kecil berarti penilaian lebih baik</b>. Tidak ada validasi rentang di sini; nilai di luar
	 * 1..5 tetap dapat tersimpan bila penulisnya tidak memvalidasi.
	 *
	 * @return skor penilaian
	 */
	@Column(name = "nilai", nullable = false)
	public Integer getNilai() {

		return nilai;
	}

	/**
	 * Menyetel skor penilaian untuk butir ini.
	 *
	 * @param nilai skor pada skala {@link #SANGAT_BAIK}..{@link #BURUK}
	 */
	public void setNilai(Integer nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan jadwal pelajaran (mata pelajaran/kelas) yang menjadi konteks penilaian ini;
	 * opsional ({@code nullable = true}), sehingga bisa {@code null} untuk penilaian guru yang
	 * tidak dikaitkan ke satu jadwal tertentu.
	 * <p>Berbeda dari {@link #getSiswa()}/{@link #getGuru()}, getter ini <b>tidak</b> memanggil
	 * {@link GeneralValueObject#check(Object)}: relasi ini dipetakan {@code @ManyToOne} tanpa
	 * {@code fetch = LAZY} (jadi eager) dengan {@link Fetch}{@code (}{@link FetchMode#SELECT}
	 * {@code )}, sehingga sudah berupa object nyata — bukan proxy — saat getter dipanggil.
	 *
	 * @return jadwal pelajaran konteks penilaian, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jadwal_pelajaran", nullable = true)
	public JadwalPelajaran getJadwalPelajaran() {
		return jadwalPelajaran;
	}

	/**
	 * Menyetel jadwal pelajaran yang menjadi konteks penilaian ini.
	 *
	 * @param jadwalPelajaran jadwal pelajaran; boleh {@code null}
	 */
	public void setJadwalPelajaran(JadwalPelajaran jadwalPelajaran) {
		this.jadwalPelajaran = jadwalPelajaran;
	}

	/**
	 * Mengembalikan baris rekap angket skema baru yang berpasangan dengan baris lama ini
	 * ({@code nullable = true}). Kolom ini berperan sebagai <b>jembatan/penanda migrasi</b>: satu
	 * baris {@link ChecklistBaruPenilaianGuruOlehSiswa} merangkum seluruh jawaban satu siswa
	 * atas satu guru pada satu jadwal dalam satu kolom teks terpadatkan, menggantikan sekumpulan
	 * baris per-butir seperti baris ini.
	 * <p>Sama seperti {@link #getJadwalPelajaran()}, getter ini tidak memanggil
	 * {@link GeneralValueObject#check(Object)} karena relasinya eager dengan
	 * {@link FetchMode#SELECT}.
	 *
	 * @return baris rekap skema baru yang bersesuaian, atau {@code null} bila baris ini belum
	 *         dipetakan ke skema baru
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "checklist_baru_penilaian_guru_oleh_siswa", nullable = true)
	public ChecklistBaruPenilaianGuruOlehSiswa getChecklistBaruPenilaianGuruOlehSiswa() {
		return checklistBaruPenilaianGuruOlehSiswa;
	}

	/**
	 * Menyetel baris rekap angket skema baru yang berpasangan dengan baris lama ini.
	 *
	 * @param checklistBaruPenilaianGuruOlehSiswa baris rekap skema baru; boleh {@code null}
	 */
	public void setChecklistBaruPenilaianGuruOlehSiswa(
			ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa) {
		this.checklistBaruPenilaianGuruOlehSiswa = checklistBaruPenilaianGuruOlehSiswa;
	}

}
