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
import org.json.JSONObject;

import ais.common.Common;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>Grup Pertemuan</b> — satu <i>sesi konsultasi terjadwal</i> yang dibuka oleh seorang
 * {@link Dosen} pada satu tanggal &amp; rentang jam tertentu, lalu diikuti oleh sekumpulan
 * {@link Mahasiswa}. Tabel: {@code public.grup_pertemuan}, {@code @Audited} (Envers),
 * {@code dynamicInsert}/{@code dynamicUpdate}.
 *
 * <h2>Apa yang sebenarnya dikelompokkan</h2>
 * Meskipun namanya "grup pertemuan", entity ini <b>bukan</b> koleksi
 * {@link Pertemuan} dan <b>tidak memiliki satu pun field koleksi</b>. Yang dikelompokkan adalah
 * <i>peserta</i>: satu baris {@code GrupPertemuan} adalah <b>undangan/agenda konsultasi</b>, dan
 * setiap mahasiswa yang diikutkan dicatat sebagai satu baris entity penghubung
 * {@link PertemuanPunyaGrupPertemuan} yang menautkan tiga hal sekaligus:
 * <ul>
 *   <li>{@code grupPertemuan} → baris ini (agenda konsultasinya);</li>
 *   <li>{@code mahasiswa} → peserta;</li>
 *   <li>{@code pertemuan} → satu baris {@link Pertemuan} milik <b>konteks akademik mahasiswa itu
 *       sendiri</b> ({@link KrsMahasiswa}, {@link MahasiswaRequestTugasAkhir}, atau
 *       {@link Skripsi}) — bukan milik grup ini.</li>
 * </ul>
 * Jadi arah relasinya <b>terbalik dari dugaan biasa</b>: {@link Pertemuan} tidak menunjuk ke
 * {@code GrupPertemuan}, melainkan ke {@link PertemuanPunyaGrupPertemuan}
 * ({@code Pertemuan.getPertemuanPunyaGrupPertemuan()}). Konsekuensinya, satu sesi konsultasi yang
 * diikuti 20 mahasiswa akan menghasilkan 20 baris {@link Pertemuan} terpisah (masing-masing
 * menempel pada KRS/skripsi/TA mahasiswa yang bersangkutan) yang "diikat" secara logis oleh satu
 * baris {@code GrupPertemuan}. Pengikatan itu dipakai untuk menyeragamkan nomor pertemuan, jam,
 * ruang, catatan, presensi, serta berkas/audio/video sesi.
 *
 * <h2>Empat jenis konsultasi</h2>
 * Kolom {@code jenis} (wajib, lihat {@link #getJenis()}) hanya boleh diisi salah satu dari empat
 * konstanta di kelas ini, dan nilai itulah yang menentukan <b>dari populasi mana daftar mahasiswa
 * calon peserta diambil</b> (lihat {@code GrupPertemuanAction#loadMahasiswa} dan
 * {@code GrupPertemuanAction#saveDetail}):
 * <ul>
 *   <li>{@link #KRS_MAHASISWA} — konsultasi dosen Pembimbing Akademik; peserta dicari dari
 *       {@link Mahasiswa} yang kolom {@code dosen}-nya (dosen PA) sama dengan {@link #getDosen()},
 *       dan {@link Pertemuan}-nya digantung pada {@link KrsMahasiswa} semester berjalan (dibuat
 *       lewat {@code Common.singkronkanKrsMahasiswa}).</li>
 *   <li>{@link #BIMBINGAN} — bimbingan Tesis/Skripsi/Tugas Akhir; peserta dicari dari
 *       {@link MahasiswaRequestTugasAkhir} berstatus bukan
 *       {@code MahasiswaRequestTugasAkhir.GAGAL_STATUS} yang salah satu dari
 *       {@code dosen1..dosen5}-nya adalah dosen ini.</li>
 *   <li>{@link #SIDANG} — konsultasi revisi pasca sidang; peserta dicari dari {@link Skripsi} yang
 *       {@code pembimbing}/{@code pembimbing3}/{@code ketuaSidang}/{@code penguji1..penguji4}-nya
 *       adalah dosen ini.</li>
 *   <li>{@link #LAINNYA} — konsultasi umum; peserta bebas, diambil dari seluruh {@link Mahasiswa}
 *       aktif yang lolos filter fakultas/jurusan/program/angkatan/kelas.</li>
 * </ul>
 * Di UI, nilai {@code jenis} juga menyembunyikan/menampilkan tab pendukung (Sejarah KRS untuk
 * {@link #KRS_MAHASISWA}, Sidang Skripsi untuk {@link #SIDANG}, Bimbingan Skripsi untuk
 * {@link #BIMBINGAN}).
 *
 * <h2>Posisi dalam hierarki</h2>
 * {@code GrupPertemuan} → {@link VOPembelajaran} → {@link VoKunci} →
 * {@code ais.database.model.sop.DataSop} → {@link GeneralValueObject}. Karena
 * {@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * hanya POJO abstrak — Hibernate tidak memetakan properti apa pun dari induk. Itulah sebabnya
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>harus</b>
 * dideklarasikan ulang di sini; pengulangan tersebut <b>keharusan teknis, bukan duplikasi yang
 * perlu "dibersihkan"</b>. Lihat {@link GeneralValueObject} untuk penjelasan lengkap mekanisme
 * {@code check()}, cache {@code ambilData}/{@code masukkanData}, dan pola getter di paket ini.
 *
 * <h2>Kuirk penting: mesin pertemuan warisan tidak aktif di sini</h2>
 * Dari {@link VOPembelajaran}, kelas ini mewarisi seluruh "mesin pertemuan" —
 * {@code ambilPertemuan()}, {@code populatePertemuan()}, {@code reInitPertemuan(Session)},
 * {@code reInitTugas(Session)}, {@code reInitUjian(Session)}, {@code ambilJumlahPertemuan()}, dan
 * seterusnya. <b>Semua method itu praktis mati untuk {@code GrupPertemuan}</b>: rantai
 * {@code instanceof} di dalam {@link VOPembelajaran} hanya mengenali 14 subtipe
 * ({@link Perkuliahan}, {@link KrsMahasiswa}, {@link Skripsi},
 * {@link PertemuanPunyaGrupPertemuan}, dst.) dan <b>tidak ada cabang untuk
 * {@code GrupPertemuan}</b>, sehingga kueri jatuh ke {@code Restrictions.sqlRestriction("false")}
 * dan selalu mengembalikan himpunan kosong. Yang terdaftar sebagai subtipe sah adalah entity
 * penghubung {@link PertemuanPunyaGrupPertemuan}, bukan grupnya. Penelusuran seluruh pohon sumber
 * memang tidak menemukan satu pun pemanggilan {@code ambilPertemuan()}/{@code reInit*()} pada
 * instance {@code GrupPertemuan}, jadi warisan ini tidak menimbulkan kerusakan — tetapi jangan
 * berasumsi method-method tersebut akan bekerja bila dipanggil.
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Jejak audit (deklarasi ulang wajib):</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *       {@link #getNama()}, {@link #getJenis()}.</li>
 *   <li><b>Penjadwalan sesi:</b> {@link #getTanggal()}, {@link #getWaktuMulai()},
 *       {@link #getWaktuSelesai()}, {@link #getRuang()}, {@link #getPertemuanKe()},
 *       {@link #getDosen()}, {@link #getDosenPengganti()}.</li>
 *   <li><b>Konteks akademik:</b> {@link #getTahunAkademik()}, {@link #getJenisSemester()},
 *       {@link #getTahun()}, {@link #getSemesterPendek()}.</li>
 *   <li><b>Penyaring calon peserta:</b> {@link #getFakultas()}, {@link #getJurusan()},
 *       {@link #getProgram()}, {@link #getTahunAngkatan()}, {@link #getKelas()}.</li>
 *   <li><b>Isi/administrasi:</b> {@link #getKeterangan()}, {@link #getCatatan()},
 *       {@link #getAktif()}, {@link #getDikunci()},
 *       {@link #getJenisLayananKepadaMahasiswa()}.</li>
 *   <li><b>Kontrak e-learning ({@code @Override} dari {@link VOPembelajaran}):</b>
 *       {@link #getCourse()}, {@link #getUrutkanotomatis()},
 *       {@link #ambilJumlahDetailperkuliahanLangsung()}.</li>
 * </ul>
 *
 * <h2>Getter yang menulis balik ke field (waspadai)</h2>
 * Sesuai pola umum paket ini, beberapa getter <b>tidak murni</b> — memanggilnya mengubah state
 * objek, dan karena entity ini {@code dynamicUpdate} + dikelola Hibernate, perubahan itu bisa ikut
 * ter-<i>flush</i> ke basis data pada akhir transaksi meski pemanggil hanya bermaksud "membaca":
 * <ul>
 *   <li>{@link #getFakultas()} — menimpa field {@code jurusan} <i>dan</i> {@code fakultas};
 *       fakultas selalu diturunkan dari jurusan bila jurusan terisi, sehingga
 *       {@link #setFakultas(Fakultas)} efektif diabaikan pada kasus itu.</li>
 *   <li>{@link #getTahunAkademik()} — mengisi field dengan tahun akademik berjalan bila kosong.</li>
 *   <li>{@link #getJenisSemester()} — mengisi field Ganjil/Genap dari {@link #getTanggal()} bila
 *       kosong.</li>
 *   <li>{@link #getTahun()} — mengisi field {@code tahun} hasil parse {@code tahunAkademik}.</li>
 *   <li>{@link #getDikunci()}, {@link #getJurusan()}, {@link #getRuang()}, {@link #getDosen()},
 *       {@link #getJenisLayananKepadaMahasiswa()} — memanggil {@code check()} lalu menugaskan
 *       hasilnya kembali ke field (resolusi proxy lazy, bukan perubahan nilai bisnis).</li>
 * </ul>
 * Sebaliknya {@link #getTanggal()}, {@link #getAktif()}, {@link #getPertemuanKe()},
 * {@link #getWaktuMulai()}, {@link #getWaktuSelesai()}, {@link #getKelas()},
 * {@link #getTahunAngkatan()}, {@link #getNama()}, {@link #getCourse()}, dan
 * {@link #getUrutkanotomatis()} hanya menghitung nilai pengganti untuk <i>dikembalikan</i> tanpa
 * menyentuh field — sehingga nilai {@code null} tetap tersimpan {@code null} di basis data.
 * <b>Tidak ada</b> getter di kelas ini yang membuka atau menutup {@link org.hibernate.Session}
 * sendiri (berbeda dengan {@code ambilPertemuan()} warisan di {@link VOPembelajaran}).
 *
 * <h2>Pemakai utama</h2>
 * {@code ais.action.master.GrupPertemuanAction} (CRUD + pemilihan peserta),
 * {@code AktifitasGrupPertemuanHelper} (agenda &amp; penyeragaman nomor pertemuan),
 * {@code PenjadwalanGrupPertemuanHelper}, {@code AbsensiGrupPertemuanHelper} (presensi + dosen
 * pengganti), {@code GrupPertemuanHelper} (jendela "Manajemen Konsultasi": presensi, catatan,
 * file, audio, video), {@code AmbilDataMahasiswaForGrupPertemuanDosenPaHelper},
 * {@code KonsultasiAction} (sisi mahasiswa), serta laporan
 * {@code LaporanLayananKepadaMahasiswa_A_3_1_8}.
 *
 * @see PertemuanPunyaGrupPertemuan
 * @see Pertemuan
 * @see VOPembelajaran
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "grup_pertemuan")

public class GrupPertemuan extends VOPembelajaran {

	/**
	 * Nilai {@code jenis} untuk konsultasi dengan dosen Pembimbing Akademik. Peserta diambil dari
	 * {@link Mahasiswa} yang dosen PA-nya adalah {@link #getDosen()}, dan {@link Pertemuan} peserta
	 * digantung pada {@link KrsMahasiswa} semester berjalan.
	 *
	 * <p><b>Catatan:</b> nilai konstanta ini adalah teks yang ditampilkan di UI sekaligus nilai
	 * yang disimpan apa adanya ke kolom {@code jenis}. Mengubah teksnya akan memutus pencocokan
	 * {@code equals(...)} terhadap baris lama di basis data.
	 */
	public static final String KRS_MAHASISWA = "Konsultasi Dosen PA";

	/**
	 * Nilai {@code jenis} untuk bimbingan Tesis/Skripsi/Tugas Akhir. Peserta diambil dari
	 * {@link MahasiswaRequestTugasAkhir} aktif yang salah satu pembimbingnya
	 * ({@code dosen1}..{@code dosen5}) adalah {@link #getDosen()}.
	 *
	 * @see #KRS_MAHASISWA untuk catatan soal nilai literal yang tersimpan di basis data
	 */
	public static final String BIMBINGAN = "Konsultasi Bimbingan Thesis/Skripsi/Tugas AKhir";

	/**
	 * Nilai {@code jenis} untuk konsultasi revisi pasca sidang. Peserta diambil dari
	 * {@link Skripsi} yang pembimbing/ketua sidang/pengujinya adalah {@link #getDosen()}.
	 *
	 * @see #KRS_MAHASISWA untuk catatan soal nilai literal yang tersimpan di basis data
	 */
	public static final String SIDANG = "Konsultasi Revisi Thesis/Skripsi/Tugas AKhir";

	/**
	 * Nilai {@code jenis} untuk konsultasi umum di luar tiga kategori lainnya. Peserta bebas: semua
	 * {@link Mahasiswa} aktif yang lolos penyaring fakultas/jurusan/program/angkatan/kelas.
	 *
	 * @see #KRS_MAHASISWA untuk catatan soal nilai literal yang tersimpan di basis data
	 */
	public static final String LAINNYA = "Konsultasi Umum";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code grup_pertemuan}; dideklarasikan ulang karena induk tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit). */
	private String oleh;
	/** Id/username pengguna terakhir yang mengubah baris ini (jejak audit). */
	private String olehId;

	/**
	 * Mengembalikan id/username pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id/username pengguna penyunting. Mengikuti pola audit standar repo ini: nilai
	 * {@code null} atau kosong <b>diabaikan diam-diam</b> sehingga jejak audit lama tidak terhapus
	 * oleh proses yang kebetulan tidak membawa konteks pengguna (mis. job terjadwal).
	 *
	 * @param olehId id pengguna penyunting; {@code null}/kosong = tidak ada perubahan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan agar jejak audit tidak tertimpa nilai hampa.
	 *
	 * @param oleh nama pengguna penyunting; {@code null}/kosong = tidak ada perubahan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama pengguna penyunting, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait lifecycle JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum
	 * pernyataan {@code UPDATE} dijalankan, dan mendelegasikan pemutakhiran stempel waktu serta
	 * identitas penyunting ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p><b>Efek samping:</b> mengubah {@link #getTanggal_dirubah()} (dan
	 * {@code oleh}/{@code olehId} bila konteks pengguna tersedia). <b>Jangan dipanggil manual</b> —
	 * ini kontrak container persistence, bukan API bisnis.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu penyuntingan terakhir; diinisialisasi ke waktu sekarang agar baris baru pun
	 * punya nilai, lalu diperbarui oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu penyuntingan terakhir secara eksplisit (umumnya hanya dipakai oleh
	 * {@link ais.database.hibernate.AuditTimestampInterceptor} dan proses migrasi/impor data).
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu penyuntingan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir (tidak pernah {@code null} untuk objek yang baru dibuat)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berformat {@code "<id>-<nama>"}, dipakai pada log, combobox, dan
	 * pesan kesalahan.
	 *
	 * <p><b>Perhatikan:</b> method ini membaca <b>field</b> {@code id} dan {@code nama} langsung
	 * (bukan lewat getter), jadi tidak melakukan {@code trim()} pada nama dan tetap aman dipanggil
	 * atas objek yang belum tersimpan (menghasilkan {@code "null-..."}).
	 *
	 * @return gabungan id dan nama grup pertemuan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Judul sesi konsultasi; wajib terisi (kolom {@code nama} {@code NOT NULL}). */
	private String nama;
	/** Keterangan bebas mengenai sesi konsultasi. */
	private String keterangan;
	/** Catatan konsultasi (teks panjang) yang diisi dosen lewat jendela "Manajemen Konsultasi". */
	private String catatan;
	/** Tanggal pelaksanaan sesi; menjadi tanggal {@link Pertemuan} yang dibuat untuk tiap peserta. */
	private Date tanggal;
	/** Jam mulai sesi dalam format teks {@code HH:mm}; default string kosong, bukan {@code null}. */
	private String waktuMulai = "";
	/** Jam selesai sesi dalam format teks {@code HH:mm}; default string kosong, bukan {@code null}. */
	private String waktuSelesai = "";
	/** Fakultas penyaring calon peserta; selalu diturunkan ulang dari {@link #jurusan} oleh {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Jurusan/program studi penyaring calon peserta. */
	private Jurusan jurusan;
	/** Program (S1/S2/D3/...) penyaring calon peserta. */
	private String program;
	/** Daftar tahun angkatan penyaring calon peserta, dipisah koma (mis. {@code "2024,2023,2022"}). */
	private String tahunAngkatan;
	/** Penanda sesi masih berlaku; {@code null} diperlakukan sebagai {@code true}. */
	private Boolean aktif;
	/** Nomor urut pertemuan yang diseragamkan ke seluruh {@link Pertemuan} peserta; default 1. */
	private Integer pertemuanKe = 1;
	/** Ruang tempat konsultasi berlangsung. */
	private Ruang ruang;
	/** Tahun akademik pelaksanaan (mis. {@code "2025/2026"}). */
	private String tahunAkademik;
	/** Jenis semester pelaksanaan: {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}. */
	private String jenisSemester;
	/** Dosen pemilik/pembuka sesi konsultasi; menentukan populasi calon peserta. */
	private Dosen dosen;
	/** Id (bukan relasi) dosen pengganti bila dosen utama berhalangan; lihat {@link #getDosenPengganti()}. */
	private Long dosenPengganti;
	/** Salah satu dari {@link #KRS_MAHASISWA}, {@link #BIMBINGAN}, {@link #SIDANG}, {@link #LAINNYA}. */
	private String jenis;
	/** Klasifikasi layanan kepada mahasiswa untuk kebutuhan pelaporan akreditasi. */
	private JenisLayananKepadaMahasiswa jenisLayananKepadaMahasiswa;
	/** Daftar kelas penyaring calon peserta, dipisah koma. */
	private String kelas;
	/** Penanda semester pendek; disalin dari konteks halaman pemanggil. */
	private Integer semesterPendek;
	/** Tahun (angka) hasil parse bagian pertama {@link #tahunAkademik}; lihat {@link #getTahun()}. */
	private Integer tahun;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi lewat refleksi, sekaligus
	 * dipakai UI saat menekan tombol "Baru" pada halaman Grup Pertemuan.
	 *
	 * <p>Nilai awal yang sudah tertanam di deklarasi field tetap berlaku: {@code waktuMulai} dan
	 * {@code waktuSelesai} = string kosong, {@code pertemuanKe} = 1, {@code tanggal_dirubah} =
	 * waktu sekarang. Sisanya {@code null} dan akan diisi lewat setter atau dihitung oleh getter
	 * berdefault (lihat {@link #getTahunAkademik()}, {@link #getJenisSemester()},
	 * {@link #getTanggal()}, {@link #getTahunAngkatan()}).
	 */
	public GrupPertemuan() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} ditandai {@code insertable = false} karena nilainya dibangkitkan basis
	 * data ({@code IDENTITY}/sequence), bukan dikirim aplikasi.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Dipakai Hibernate setelah {@code INSERT} dan oleh kode yang menyusun
	 * objek referensi ringan (hanya id) untuk keperluan kueri.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** Pengguna yang sedang "mengunci" baris ini (mekanisme kunci penyuntingan dari {@link VoKunci}). */
	private Tbmuser dikunci;

	/**
	 * Mengembalikan pengguna yang sedang memegang kunci penyuntingan atas baris ini. Implementasi
	 * kontrak {@link VoKunci#getDikunci()}; selama field terisi, UI menolak/memperingatkan
	 * penyuntingan bersamaan oleh pengguna lain.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditugaskan kembali ke field {@code dikunci}
	 * — resolusi proxy lazy Hibernate menjadi objek nyata, lihat {@link GeneralValueObject} bagian
	 * mekanisme {@code check()}.
	 *
	 * @return pemegang kunci, atau {@code null} bila baris tidak sedang dikunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menetapkan (atau melepas, dengan {@code null}) pemegang kunci penyuntingan baris ini.
	 *
	 * @param dikunci pengguna pemegang kunci; {@code null} untuk membuka kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Mengembalikan judul sesi konsultasi, sudah di-{@code trim()}.
	 *
	 * <p>Nama ini muncul sebagai judul tab di halaman Konsultasi sisi mahasiswa dan menjadi awalan
	 * nama entity penghubung ({@link PertemuanPunyaGrupPertemuan#getNama()} menyusun
	 * {@code "<nama grup>-<id pertemuan>"}). Kolomnya {@code NOT NULL} sepanjang 255 karakter,
	 * sehingga menyimpan baris tanpa nama akan gagal di tingkat basis data.
	 *
	 * <p>{@code trim()} hanya diterapkan pada nilai kembalian; field aslinya tidak diubah.
	 *
	 * @return judul sesi tanpa spasi di tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan judul sesi konsultasi. Wajib diisi sebelum penyimpanan (kolom {@code NOT NULL}).
	 *
	 * @param nama judul sesi, maksimal 255 karakter
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas mengenai sesi konsultasi (ditampilkan pada grid dan formulir).
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas sesi konsultasi.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jurusan/program studi yang dipakai sebagai <b>penyaring calon peserta</b> saat
	 * memilih mahasiswa untuk sesi ini. Bila {@code null}, penyaringan jurusan tidak diterapkan
	 * (semua jurusan).
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditugaskan kembali ke field (resolusi proxy
	 * lazy). Nilai ini juga menjadi sumber {@link #getFakultas()}.
	 *
	 * @return jurusan penyaring, atau {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan jurusan penyaring calon peserta.
	 *
	 * <p><b>Perhatikan:</b> mengubah jurusan secara tidak langsung mengubah pula fakultas, karena
	 * {@link #getFakultas()} selalu menghitung ulang fakultas dari jurusan yang terisi.
	 *
	 * @param jurusan jurusan penyaring; {@code null} berarti semua jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan daftar tahun angkatan penyaring calon peserta, dalam bentuk teks dipisah koma
	 * (mis. {@code "2026,2025,2024,2023,2022,2021"}).
	 *
	 * <p><b>Default dinamis:</b> bila field belum pernah diisi, method menyusun daftar
	 * <b>enam tahun terakhir</b> dari tahun berjalan ({@code Y, Y-1, ... Y-5}) memakai
	 * {@link WaktuUtil#getCalendar()}. Nilai default ini <b>tidak</b> ditulis balik ke field,
	 * sehingga baris tetap menyimpan {@code NULL} di basis data dan daftar akan bergeser sendiri
	 * setiap pergantian tahun — perilaku disengaja agar sesi lama tidak "membekukan" rentang
	 * angkatan.
	 *
	 * <p>Nilai ini dipecah per koma oleh {@code GrupPertemuanAction} lalu dipakai pada
	 * {@code Restrictions.in("tahunangkatan", ...)}.
	 *
	 * @return daftar tahun angkatan dipisah koma; tidak pernah {@code null}
	 */
	@Column(name = "tahun_angkatan")
	public String getTahunAngkatan() {
		return tahunAngkatan == null
				? WaktuUtil.getCalendar().get(Calendar.YEAR) + "," + (WaktuUtil.getCalendar().get(Calendar.YEAR) - 1)
						+ "," + (WaktuUtil.getCalendar().get(Calendar.YEAR) - 2) + ","
						+ (WaktuUtil.getCalendar().get(Calendar.YEAR) - 3) + ","
						+ (WaktuUtil.getCalendar().get(Calendar.YEAR) - 4) + ","
						+ (WaktuUtil.getCalendar().get(Calendar.YEAR) - 5)
				: tahunAngkatan;
	}

	/**
	 * Menetapkan daftar tahun angkatan penyaring calon peserta.
	 *
	 * @param tahunAngkatan tahun angkatan dipisah koma; {@code null} mengembalikan perilaku default
	 *                      enam tahun terakhir (lihat {@link #getTahunAngkatan()})
	 */
	public void setTahunAngkatan(String tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * Mengembalikan fakultas penyaring calon peserta, <b>diturunkan dari {@link #getJurusan()}</b>.
	 *
	 * <p><b>Efek samping ganda (non-obvious):</b> method ini memanggil {@link #getJurusan()} — yang
	 * menulis balik field {@code jurusan} — lalu, bila jurusan terisi, <b>menimpa field
	 * {@code fakultas}</b> dengan {@code jurusan.getFakultas()}. Artinya nilai yang pernah dipasang
	 * lewat {@link #setFakultas(Fakultas)} akan hilang begitu jurusan tidak {@code null}; fakultas
	 * hanya bertahan mandiri bila jurusan kosong. Karena entity ini {@code dynamicUpdate} dan
	 * dikelola Hibernate, penimpaan tersebut dapat ikut ter-<i>flush</i> ke kolom
	 * {@code fakultas} walau pemanggil hanya bermaksud membaca.
	 *
	 * @return fakultas penyaring, atau {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas")
	public Fakultas getFakultas() {
		jurusan = getJurusan();
		if (jurusan != null) {
			fakultas = jurusan.getFakultas();
		}
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menetapkan fakultas penyaring calon peserta.
	 *
	 * <p><b>Perhatikan:</b> nilai ini hanya bertahan selama {@link #getJurusan()} bernilai
	 * {@code null}; bila jurusan terisi, {@link #getFakultas()} akan menimpanya dengan fakultas
	 * milik jurusan tersebut.
	 *
	 * @param fakultas fakultas penyaring; boleh {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan status keaktifan sesi konsultasi, dengan {@code null} <b>diperlakukan sebagai
	 * {@code true}</b> — baris lama yang belum punya nilai tetap dianggap aktif.
	 *
	 * <p>Nilai default ini tidak ditulis balik ke field, jadi kolomnya tetap {@code NULL} di basis
	 * data sampai pengguna mengubah centang "Aktif" pada formulir.
	 *
	 * @return {@code true} bila sesi aktif (termasuk saat field masih {@code null})
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status keaktifan sesi konsultasi.
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code null}/{@code true} berarti aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan sesi konsultasi, dengan <b>tanggal hari ini sebagai
	 * default</b> bila field masih {@code null}.
	 *
	 * <p>Nilai ini menjadi acuan penting di beberapa tempat: {@code GrupPertemuanAction#saveDetail}
	 * memakainya untuk mencari/membuat {@link Pertemuan} tiap peserta ({@code setMulai}/
	 * {@code setSelesai} serta pencocokan {@code Restrictions.eq("tanggal", ...)}), dan
	 * {@link #getJenisSemester()} memakainya untuk menentukan Ganjil/Genap.
	 *
	 * <p>Default tidak ditulis balik ke field, sehingga kolom tetap {@code NULL} sampai
	 * {@link #setTanggal(Date)} dipanggil. Konsekuensinya, sesi yang belum disimpan dengan tanggal
	 * eksplisit akan "berpindah" mengikuti hari saat dibaca.
	 *
	 * @return tanggal pelaksanaan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menetapkan tanggal pelaksanaan sesi konsultasi.
	 *
	 * @param tanggal tanggal pelaksanaan; {@code null} mengembalikan perilaku default "hari ini"
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan kode program (mis. S1/S2/D3) penyaring calon peserta.
	 *
	 * @return kode program, atau {@code null} bila tidak dibatasi
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menetapkan kode program penyaring calon peserta.
	 *
	 * @param program kode program; {@code null} berarti semua program
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan jam mulai sesi dalam bentuk teks (format {@code HH:mm}, diurai memakai
	 * {@code Common.timeFormat2} saat pembuatan {@link Pertemuan} peserta).
	 *
	 * <p>Mengembalikan string kosong (bukan {@code null}) bila belum diisi, sehingga pemanggil
	 * aman melakukan {@code trim()}/{@code isEmpty()} tanpa cek {@code null}.
	 *
	 * @return jam mulai; tidak pernah {@code null}
	 */
	public String getWaktuMulai() {
		return waktuMulai == null ? "" : waktuMulai;
	}

	/**
	 * Menetapkan jam mulai sesi.
	 *
	 * @param waktuMulai jam mulai berformat {@code HH:mm}; boleh {@code null}/kosong
	 */
	public void setWaktuMulai(String waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengembalikan jam selesai sesi dalam bentuk teks (format {@code HH:mm}).
	 *
	 * <p>Mengembalikan string kosong (bukan {@code null}) bila belum diisi.
	 *
	 * @return jam selesai; tidak pernah {@code null}
	 */
	public String getWaktuSelesai() {
		return waktuSelesai == null ? "" : waktuSelesai;
	}

	/**
	 * Menetapkan jam selesai sesi.
	 *
	 * @param waktuSelesai jam selesai berformat {@code HH:mm}; boleh {@code null}/kosong
	 */
	public void setWaktuSelesai(String waktuSelesai) {
		this.waktuSelesai = waktuSelesai;
	}

	/**
	 * Mengembalikan nomor urut pertemuan yang akan diseragamkan ke seluruh {@link Pertemuan}
	 * peserta sesi ini.
	 *
	 * <p>{@code AktifitasGrupPertemuanHelper} membaca nilai ini lalu, untuk setiap
	 * {@link PertemuanPunyaGrupPertemuan} pada grup, memperbarui
	 * {@code pertemuan.setPertemuanKe(...)} bila belum sama — sehingga konsultasi ke-3 tercatat
	 * sebagai pertemuan ke-3 di semua konteks peserta.
	 *
	 * <p><b>Kuirk:</b> default kembalian saat field {@code null} adalah <b>0</b>, padahal nilai
	 * awal field pada objek baru adalah <b>1</b>. Selisih ini hanya terlihat pada baris lama hasil
	 * migrasi yang kolomnya {@code NULL}.
	 *
	 * @return nomor pertemuan; {@code 0} bila field {@code null}
	 */
	public Integer getPertemuanKe() {
		return pertemuanKe == null ? 0 : pertemuanKe;
	}

	/**
	 * Menetapkan nomor urut pertemuan sesi ini.
	 *
	 * @param pertemuanKe nomor pertemuan; boleh {@code null}
	 */
	public void setPertemuanKe(Integer pertemuanKe) {
		this.pertemuanKe = pertemuanKe;
	}

	/**
	 * Mengembalikan ruang tempat sesi konsultasi berlangsung.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditugaskan kembali ke field (resolusi proxy
	 * lazy).
	 *
	 * @return ruang konsultasi, atau {@code null} bila tidak ditentukan (mis. daring)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang")
	public Ruang getRuang() {
		ruang = check(ruang);
		return ruang;
	}

	/**
	 * Menetapkan ruang tempat sesi konsultasi.
	 *
	 * @param ruang ruang konsultasi; boleh {@code null}
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengembalikan tahun akademik pelaksanaan sesi (mis. {@code "2025/2026"}).
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method <b>mengisinya</b> dengan tahun
	 * akademik berjalan dari {@link Common#getCurrentTahunAkademik()} — jadi pemanggilan "baca"
	 * pertama sekaligus memutasi objek, dan pada entity terkelola perubahan itu dapat ikut
	 * ter-<i>flush</i> ke basis data. Perilaku ini disengaja agar sesi yang dibuat lewat jalur
	 * non-UI tetap punya konteks tahun akademik.
	 *
	 * <p>Nilai ini dipakai {@code GrupPertemuanAction#saveDetail} untuk mencari
	 * {@link Skripsi}/{@link MahasiswaRequestTugasAkhir} peserta pada tahun akademik yang sama, dan
	 * menjadi sumber {@link #getTahun()}.
	 *
	 * @return tahun akademik pelaksanaan; tidak {@code null} setelah pemanggilan ini
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik pelaksanaan sesi.
	 *
	 * @param tahunAkademik tahun akademik berformat {@code "YYYY/YYYY"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenis semester pelaksanaan: {@link Perkuliahan#GANJIL} atau
	 * {@link Perkuliahan#GENAP}.
	 *
	 * <p><b>Efek samping:</b> bila field {@code null}/kosong, method <b>mengisinya</b> dengan hasil
	 * {@code Common.isNowSemensterGanjil(getTanggal())} — perhatikan bahwa penentuan memakai
	 * {@link #getTanggal()}, yang sendirinya berdefault "hari ini" bila tanggal belum diisi.
	 * Seperti {@link #getTahunAkademik()}, mutasi ini dapat ikut tersimpan ke basis data.
	 *
	 * <p>Nilai ini dipakai untuk memilih semester ganjil/genap saat mencocokkan
	 * {@link Skripsi}/{@link MahasiswaRequestTugasAkhir}/{@link KrsMahasiswa} peserta
	 * ({@code semester % 2}).
	 *
	 * @return {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}; tidak pernah {@code null}
	 */
	public String getJenisSemester() {
		if (jenisSemester == null || jenisSemester.isEmpty()) {
			jenisSemester = Common.isNowSemensterGanjil(getTanggal()) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return jenisSemester;
	}

	/**
	 * Menetapkan jenis semester pelaksanaan sesi.
	 *
	 * @param jenisSemester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan dosen pemilik sesi konsultasi. Relasi ini <b>bukan sekadar informasi</b>: dosen
	 * inilah yang menentukan populasi calon peserta pada ketiga jenis terarah
	 * ({@link #KRS_MAHASISWA} lewat kolom dosen PA mahasiswa, {@link #BIMBINGAN} lewat
	 * {@code dosen1..dosen5} pada {@link MahasiswaRequestTugasAkhir}, {@link #SIDANG} lewat
	 * pembimbing/ketua sidang/penguji pada {@link Skripsi}).
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditugaskan kembali ke field (resolusi proxy
	 * lazy).
	 *
	 * @return dosen pemilik sesi, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen")
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menetapkan dosen pemilik sesi konsultasi.
	 *
	 * @param dosen dosen pemilik; menentukan populasi calon peserta (lihat {@link #getDosen()})
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan jenis konsultasi — salah satu dari {@link #KRS_MAHASISWA}, {@link #BIMBINGAN},
	 * {@link #SIDANG}, atau {@link #LAINNYA}.
	 *
	 * <p>Kolomnya {@code NOT NULL}, namun getter ini <b>tidak</b> menyediakan nilai default:
	 * memanggil {@code getJenis().equals(...)} pada objek yang belum diisi (mis. hasil
	 * {@code new GrupPertemuan()}) akan melempar {@link NullPointerException}. Beberapa pemanggil
	 * di {@code GrupPertemuanAction} dan {@code AmbilDataMahasiswaForGrupPertemuanDosenPaHelper}
	 * memang melakukan pola itu tanpa penjagaan {@code null}, dengan asumsi objek selalu berasal
	 * dari basis data.
	 *
	 * @return jenis konsultasi, atau {@code null} pada objek baru yang belum diisi
	 */
	@Column(nullable = false)
	public String getJenis() {
		return jenis;
	}

	/**
	 * Menetapkan jenis konsultasi. Nilainya wajib salah satu konstanta kelas ini — tidak ada
	 * validasi di sisi entity, penjaminannya ada di combobox {@code GrupPertemuanAction}.
	 *
	 * @param jenis {@link #KRS_MAHASISWA}, {@link #BIMBINGAN}, {@link #SIDANG}, atau
	 *              {@link #LAINNYA}
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan penanda semester pendek yang disalin dari konteks halaman saat sesi disimpan
	 * ({@code GrupPertemuanAction} menyalin field bernama sama miliknya sendiri).
	 *
	 * @return penanda semester pendek, atau {@code null} bila sesi bukan bagian semester pendek
	 */
	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	/**
	 * Menetapkan penanda semester pendek.
	 *
	 * @param semesterPendek penanda semester pendek; boleh {@code null}
	 */
	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	/**
	 * Mengembalikan catatan konsultasi — teks panjang ({@code columnDefinition = "text"}) yang
	 * diisi dosen melalui tab "Catatan Konsultasi" pada jendela Manajemen Konsultasi
	 * ({@code GrupPertemuanHelper}).
	 *
	 * @return catatan konsultasi, atau {@code null} bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	/**
	 * Menetapkan catatan konsultasi.
	 *
	 * @param catatan teks catatan bebas; boleh {@code null}
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Mengembalikan <b>id</b> dosen pengganti bila dosen utama berhalangan hadir.
	 *
	 * <p><b>Non-obvious:</b> ini disimpan sebagai {@link Long} biasa, <b>bukan</b> relasi
	 * {@code @ManyToOne} ke {@link Dosen} seperti {@link #getDosen()}. Pemanggil harus memuat
	 * objeknya sendiri — pola yang dipakai {@code AbsensiGrupPertemuanHelper} adalah
	 * {@code createCriteria(Dosen.class).add(Restrictions.idEq(grupPertemuan.getDosenPengganti()))}.
	 * Karena bukan foreign key terpetakan, tidak ada jaminan integritas referensial dari sisi ORM:
	 * id dosen yang sudah dihapus tetap bisa tersimpan di kolom ini.
	 *
	 * @return id dosen pengganti, atau {@code null} bila dosen utama yang hadir
	 */
	public Long getDosenPengganti() {
		return dosenPengganti;
	}

	/**
	 * Menetapkan id dosen pengganti. Dipanggil dari {@code AbsensiGrupPertemuanHelper} ketika
	 * operator memilih dosen pengganti pada layar presensi.
	 *
	 * @param dosenPengganti id {@link Dosen} pengganti; {@code null} untuk menghapus penggantian
	 */
	public void setDosenPengganti(Long dosenPengganti) {
		this.dosenPengganti = dosenPengganti;
	}

	/**
	 * Mengembalikan klasifikasi layanan kepada mahasiswa untuk sesi ini. Dipakai pelaporan
	 * akreditasi — {@code LaporanLayananKepadaMahasiswa_A_3_1_8} menghitung frekuensi
	 * {@code GrupPertemuan} beserta jumlah mahasiswa terlayani per jenis layanan.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditugaskan kembali ke field (resolusi proxy
	 * lazy).
	 *
	 * @return jenis layanan kepada mahasiswa, atau {@code null} bila belum diklasifikasikan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_layanan_kepada_mahasiswa")
	public JenisLayananKepadaMahasiswa getJenisLayananKepadaMahasiswa() {
		jenisLayananKepadaMahasiswa = check(jenisLayananKepadaMahasiswa);
		return jenisLayananKepadaMahasiswa;
	}

	/**
	 * Menetapkan klasifikasi layanan kepada mahasiswa untuk sesi ini.
	 *
	 * @param jenisLayananKepadaMahasiswa jenis layanan; boleh {@code null}
	 */
	public void setJenisLayananKepadaMahasiswa(JenisLayananKepadaMahasiswa jenisLayananKepadaMahasiswa) {
		this.jenisLayananKepadaMahasiswa = jenisLayananKepadaMahasiswa;
	}

	/**
	 * Mengembalikan daftar kelas penyaring calon peserta (dipisah koma), sudah di-{@code trim()}
	 * dan tidak pernah {@code null}.
	 *
	 * <p>{@code GrupPertemuanAction} memecah nilai ini per koma lalu menyusun potongan SQL
	 * {@code mahasiswa in (select aa.id from mahasiswa aa where aa.kelas in ('A','B'))} yang
	 * dipasang lewat {@code Restrictions.sqlRestriction(...)}. Bila kosong, penyaring kelas tidak
	 * diterapkan sama sekali.
	 *
	 * @return daftar kelas dipisah koma; string kosong bila tidak dibatasi
	 */
	public String getKelas() {
		return kelas == null ? "" : kelas.trim();
	}

	/**
	 * Menetapkan daftar kelas penyaring calon peserta.
	 *
	 * @param kelas nama-nama kelas dipisah koma; {@code null}/kosong berarti semua kelas
	 */
	public void setKelas(String kelas) {
		this.kelas = kelas;
	}

	/**
	 * Mengembalikan tahun (angka) pelaksanaan sesi, hasil parse bagian pertama
	 * {@code tahunAkademik} ({@code "2025/2026"} → {@code 2025}).
	 *
	 * <p><b>Efek samping:</b> hasil parse ditulis ke field {@code tahun}, sehingga pemanggilan
	 * "baca" ini memutasi objek dan berpotensi ikut ter-<i>flush</i> ke kolom {@code tahun}.
	 *
	 * <p><b>Kuirk urutan pemanggilan:</b> method membaca <b>field</b> {@code tahunAkademik} secara
	 * langsung, bukan lewat {@link #getTahunAkademik()}. Akibatnya, pada objek yang tahun
	 * akademiknya belum pernah diisi maupun dibaca, cabang parse dilewati dan method mengembalikan
	 * nilai {@code tahun} apa adanya (biasanya {@code null}) — sementara memanggil
	 * {@link #getTahunAkademik()} lebih dulu akan membuat method ini menghasilkan tahun berjalan.
	 *
	 * <p>Kegagalan parse (format tak terduga) ditelan diam-diam dan hanya dicatat ke
	 * {@code ErrorAuditUtil}; nilai {@code tahun} sebelumnya dipertahankan.
	 *
	 * @return tahun pelaksanaan, atau {@code null} bila tahun akademik belum terisi/tidak terurai
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			try {
				tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GrupPertemuan.java:352");

			}
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun (angka) pelaksanaan sesi secara eksplisit.
	 *
	 * <p><b>Perhatikan:</b> nilai ini akan ditimpa oleh {@link #getTahun()} pada pemanggilan
	 * berikutnya bila {@code tahunAkademik} terisi dan terurai dengan benar.
	 *
	 * @param tahun tahun pelaksanaan; boleh {@code null}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan jumlah {@code Detailperkuliahan} yang menempel <b>langsung</b> pada objek
	 * pembelajaran ini. Implementasi kontrak abstrak
	 * {@link VOPembelajaran#ambilJumlahDetailperkuliahanLangsung()}.
	 *
	 * <p>Selalu {@code 0}: sesi konsultasi tidak punya peserta langsung sama sekali — pesertanya
	 * tercatat satu per satu pada {@link PertemuanPunyaGrupPertemuan}, yang implementasinya
	 * mengembalikan {@code 1} per baris. Jadi jumlah peserta sebuah grup dihitung dengan menghitung
	 * baris {@link PertemuanPunyaGrupPertemuan} yang menunjuk ke grup ini, bukan lewat method ini.
	 *
	 * @return selalu {@code 0}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 0;
	}

	/** Konfigurasi tampilan e-learning dalam bentuk teks JSON; lihat {@link #getCourse()}. */
	private String course;
	/** Penanda apakah nomor pertemuan disusun otomatis menurut tanggal; lihat {@link #getUrutkanotomatis()}. */
	private Boolean urutkanotomatis;

	/**
	 * Mengembalikan konfigurasi tampilan e-learning sesi ini sebagai teks JSON. Implementasi
	 * kontrak abstrak {@link VOPembelajaran#getCourse()}, dipetakan ke kolom bertipe {@code text}.
	 *
	 * <p>Bila field {@code null} atau hanya berisi spasi, method mengembalikan objek JSON kosong
	 * ({@code "{}"}) alih-alih {@code null}, sehingga pemanggil selalu bisa langsung membungkusnya
	 * dengan {@code new JSONObject(...)} tanpa penjagaan tambahan. Nilai default ini <b>tidak</b>
	 * ditulis balik ke field.
	 *
	 * @return teks JSON konfigurasi tampilan; tidak pernah {@code null}
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Menetapkan konfigurasi tampilan e-learning sesi ini. Implementasi kontrak abstrak
	 * {@link VOPembelajaran#setCourse(String)}.
	 *
	 * @param course teks JSON konfigurasi; {@code null}/kosong mengembalikan perilaku default
	 *               {@code "{}"} pada {@link #getCourse()}
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Mengembalikan penanda apakah nomor pertemuan disusun otomatis berdasarkan tanggal (bukan
	 * berdasarkan kolom {@code pertemuanKe}). Implementasi kontrak abstrak
	 * {@link VOPembelajaran#getUrutkanotomatis()}; {@code null} diperlakukan sebagai {@code true}.
	 *
	 * <p>Di {@link VOPembelajaran}, nilai ini memilih kunci pengurutan pada
	 * {@code masukkanPertemuanLocal(...)} dan arah {@code Order.asc(...)} pada
	 * {@code reInitPertemuan(Session)}/{@code reInitTugas(Session)}. Namun untuk
	 * {@code GrupPertemuan} jalur-jalur tersebut tidak pernah aktif (lihat catatan "mesin pertemuan
	 * warisan tidak aktif" pada dokumentasi kelas), sehingga secara praktis nilai ini hanya
	 * memenuhi kontrak induk.
	 *
	 * @return {@code true} bila pengurutan otomatis (termasuk saat field {@code null})
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Menetapkan penanda pengurutan otomatis nomor pertemuan. Implementasi kontrak abstrak
	 * {@link VOPembelajaran#setUrutkanotomatis(Boolean)}.
	 *
	 * @param urutkanotomatis {@code false} untuk memakai {@code pertemuanKe} manual;
	 *                        {@code null}/{@code true} berarti otomatis
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

}
