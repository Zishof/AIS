package ais.database.model;

// Generated Apr 12, 2010 11:30:55 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Entity <b>periode/gelombang sidang tugas akhir (skripsi)</b> — tabel
 * {@code public.jadwal_sidang_tugas_akhir}.
 *
 * <p><b>Apa yang sebenarnya disimpan class ini.</b> Meski namanya "jadwal sidang", satu baris
 * entity ini <i>bukan</i> jadwal sidang satu mahasiswa. Satu baris adalah <b>satu gelombang /
 * periode sidang</b> yang dibuka oleh program studi: punya nama ({@link #getNama() nama}),
 * rentang tanggal berlaku ({@link #getMulai() mulai} … {@link #getSampai() sampai}), satu
 * {@link Ruang} sidang default, tahun akademik, dan cakupan
 * {@link Fakultas}/{@link Jurusan}/{@link #getProgram() program}. Peserta sidang tidak disimpan
 * di sini sama sekali — arah relasinya terbalik: {@link Skripsi} yang menunjuk gelombang ini
 * lewat {@code Skripsi.jadwalSidangTugasAkhir}. Karena itu class ini <b>tidak memiliki satu pun
 * properti mahasiswa, dosen pembimbing, maupun dosen penguji</b> (lihat catatan "Pola dosen"
 * di bawah).
 *
 * <p><b>Posisi dalam alur tugas akhir.</b>
 * <ol>
 *   <li>Mahasiswa mengajukan judul lewat {@link MahasiswaRequestTugasAkhir};</li>
 *   <li>pengajuan yang disetujui menjadi {@link Skripsi} (judul, pembimbing, penguji, nilai);</li>
 *   <li>petugas prodi membuka gelombang sidang — <b>baris class ini</b> — melalui layar
 *       <i>Jadwal Sidang Tugas Akhir</i> ({@code ais.action.master.JadwalSidangTugasAkhirAction});</li>
 *   <li>skripsi dijadwalkan dengan memilih gelombang pada Bandbox
 *       {@code ais.action.master.helper.AmbilJadwalSidangTugasAkhirBanbox} di layar Skripsi.
 *       Di AIS, <b>tertautnya sebuah skripsi ke gelombang ini sekaligus berarti "sidang
 *       disetujui"</b> — lihat pemeriksaan pada {@code Skripsi} yang membaca field
 *       {@code jadwalSidangTugasAkhir};</li>
 *   <li>{@code Skripsi} memakai gelombang ini sebagai <b>sumber default</b>: bila tanggal sidang
 *       skripsi kosong, {@link #getMulai()} disalin ke {@code Skripsi.tanggalSidang}; bila ruang
 *       sidang skripsi kosong, {@link #getRuangSidang()} disalin ke {@code Skripsi.ruangSidang};</li>
 *   <li>laporan {@code LaporanSidang} dan {@code LaporanRekapitulasiSidang} dapat dijalankan
 *       langsung untuk satu gelombang (konstruktor yang menerima instance class ini).</li>
 * </ol>
 *
 * <p><b>Pengelompokan anggota class.</b>
 * <ul>
 *   <li><i>Jejak audit</i> — {@link #getOleh()}/{@link #getOlehId()},
 *       {@link #getTanggal_dirubah()} dan kait {@link #onUpdate()}; dideklarasikan ulang di sini
 *       karena {@link GeneralValueObject} bukan {@code @MappedSuperclass} (lihat di bawah);</li>
 *   <li><i>Identitas &amp; deskripsi</i> — {@link #getId()}, {@link #getNama()},
 *       {@link #getKeterangan()};</li>
 *   <li><i>Rentang berlaku</i> — {@link #getMulai()}, {@link #getSampai()};</li>
 *   <li><i>Cakupan/penyaring</i> — {@link #getFakultas()}, {@link #getJurusan()},
 *       {@link #getProgram()}, {@link #getTahunAkademik()};</li>
 *   <li><i>Sarana</i> — {@link #getRuangSidang()};</li>
 *   <li><i>Agenda rinci</i> — {@link #getJadwalRinci()} beserta tiga method bisnis pengelolanya:
 *       {@link #populateJadwal(String, Date, Date, String)}, {@link #hapusJadwal(String)}, dan
 *       {@link #daftarJadwal()}.</li>
 * </ul>
 *
 * <p><b>Agenda rinci disimpan sebagai satu kolom teks, bukan tabel anak.</b> Kolom
 * {@code jadwal_rinci} menampung daftar agenda (mis. "Pendaftaran", "Penyerahan berkas",
 * "Pelaksanaan sidang") dalam bentuk string berserialisasi manual dengan pemisah antar-baris
 * {@code "||"} dan pemisah antar-kolom {@code "<>"}, urutan kolom
 * {@code nama<>mulai<>sampai<>keterangan}, tanggal diformat memakai
 * {@code Common.datetimeFormat1s} (pola {@code ddMMyyHHmmss} — inilah satu-satunya tempat di
 * entity ini yang menyimpan <b>jam</b>). Konsekuensi rancangan ini didokumentasikan pada
 * masing-masing method; yang terpenting: format tersebut rapuh terhadap karakter
 * {@code '|'}, {@code '<'}, {@code '>'} dan terhadap kolom kosong.
 *
 * <p><b>Catatan {@code GeneralValueObject}.</b> Induk {@link GeneralValueObject} adalah POJO
 * abstrak biasa — <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — sehingga
 * Hibernate tidak memetakan properti induk. Karena itu {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di sini; itu keharusan teknis,
 * bukan duplikasi yang keliru. Yang diwarisi dan dipakai adalah utilitas statisnya, khususnya
 * {@link GeneralValueObject#check(Object)}.
 *
 * <p><b>Pola dosen slot 1/2/dst.: TIDAK ADA di class ini.</b> Diverifikasi langsung dari kode —
 * file ini tidak mengimpor maupun mendeklarasikan properti bertipe {@code Dosen}/{@code Mahasiswa}
 * sama sekali; satu-satunya relasi adalah {@link Jurusan}, {@link Fakultas}, dan {@link Ruang}.
 * Jadi risiko tertukarnya slot penguji (seperti yang ditemukan pada {@code FormatNilaiSkripsi})
 * tidak berlaku di sini; slot pembimbing/penguji berada di {@link Skripsi}.
 *
 * <p><b>Peringatan getter yang menulis balik.</b> Class ini memakai {@code dynamicUpdate = true}.
 * Empat getter memodifikasi state saat dibaca — {@link #getNama()} (mengisi nama default),
 * {@link #getMulai()} (mengisi tanggal hari ini), {@link #getSampai()} (mengisi mulai + 6 bulan),
 * dan {@link #getRuangSidang()} (menulis balik hasil {@code check()}). Membaca keempatnya pada
 * instance yang masih <i>managed</i> dapat menghasilkan {@code UPDATE} ke basis data saat
 * {@code flush} tanpa ada aksi simpan dari pengguna.
 *
 * <p><b>Kuirk warisan generator.</b> Komentar Javadoc asli hasil {@code hbm2java} berbunyi
 * "JamPerkuliahan generated by hbm2java" — sisa salin-tempel dari entity lain, tidak ada
 * hubungannya dengan jadwal sidang.
 *
 * @see Skripsi
 * @see MahasiswaRequestTugasAkhir
 * @see Ruang
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jadwal_sidang_tugas_akhir")

public class JadwalSidangTugasAkhir extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance yang tersimpan di sesi HTTP/ZK tetap
	 * dapat dibaca setelah class dikompilasi ulang.
	 */
	private static final long serialVersionUID = -8842945307087672400L;

	/**
	 * Kunci utama (kolom {@code id}, {@code IDENTITY}). Bernilai {@code null} selama entity
	 * belum pernah disimpan — dipakai layar sebagai penanda "tambah" vs "ubah".
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (jejak audit).
	 *
	 * @see #setOleh(String)
	 */
	private String oleh;

	/**
	 * Identitas (username/id) pengguna terakhir yang mengubah baris ini (jejak audit).
	 *
	 * @see #setOlehId(String)
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah, dengan <b>penjagaan anti-timpa</b>: nilai {@code null} atau
	 * yang hanya berisi spasi <b>diabaikan</b> (nilai lama dipertahankan), bukan disimpan.
	 * Pola ini dipakai seragam di seluruh entity AIS agar interceptor audit tidak menghapus
	 * jejak yang sudah ada ketika konteks pengguna kebetulan kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah, dengan penjagaan anti-timpa yang sama seperti
	 * {@link #setOlehId(String)}: {@code null}/kosong diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code UPDATE}
	 * dieksekusi, dan mendelegasikan pengisian jejak audit (pengguna + waktu) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Catatan tata letak: deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris
	 * yang sama oleh generator; nilai awalnya adalah waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan waktu JVM mentah.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Umumnya dipanggil oleh interceptor audit, bukan oleh
	 * kode layar.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (dipetakan sebagai {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek baru karena field
	 *         diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks untuk keperluan log/debug: {@code mulai_sampai_jurusan}.
	 *
	 * <p>Dua kuirk yang perlu diketahui: (1) method ini membaca <b>field mentah</b>, bukan getter,
	 * sehingga tidak memicu pengisian nilai default seperti {@link #getMulai()}/{@link #getSampai()};
	 * (2) potongan tanggal memakai {@code Date.toString()} bawaan Java (format Inggris), dan
	 * potongan jurusan memakai {@code Jurusan.toString()} — jadi keluarannya <b>tidak</b> cocok
	 * dipakai sebagai label di layar. Label di layar memakai {@link #getNama()}.
	 *
	 * @return gabungan tanggal mulai, tanggal sampai, dan jurusan dipisah garis bawah
	 */
	public String toString() {
		return mulai + "_" + sampai + "_" + jurusan;
	}

	/** Nama/judul gelombang sidang, mis. "Sidang Skripsi Gelombang I". Label layar: "Nama Jadwal". */
	private String nama;

	/** Tanggal awal berlakunya gelombang; default waktu server saat objek dibuat. Label layar: "Mulai". */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();

	/** Tanggal akhir berlakunya gelombang; default waktu server saat objek dibuat. Label layar: "Sampai". */
	private Date sampai = ais.ui.util.WaktuUtil.getDate();

	/** Program studi pemilik gelombang (opsional). Label layar: "Prodi". */
	private Jurusan jurusan;

	/** Fakultas pemilik gelombang (opsional); dipakai juga untuk mempersempit pilihan {@link #jurusan}. */
	private Fakultas fakultas;

	/** Kode program pendidikan (mis. reguler/karyawan), diisi dari {@code Common.initPrograms}. */
	private String program;

	/** Tahun akademik berlakunya gelombang, mis. {@code "2025/2026"}; wajib diisi di layar. */
	private String tahunAkademik;

	/** Ruang sidang default gelombang ini; diwarisi {@code Skripsi} bila ruang skripsi kosong. */
	private Ruang ruangSidang;

	/** Keterangan bebas gelombang. */
	private String keterangan;

	/**
	 * Daftar agenda rinci dalam bentuk teks berserialisasi
	 * ({@code nama<>mulai<>sampai<>keterangan}, antar-baris dipisah {@code "||"}).
	 * Jangan diolah langsung — pakai {@link #daftarJadwal()},
	 * {@link #populateJadwal(String, Date, Date, String)}, dan {@link #hapusJadwal(String)}.
	 */
	private String jadwalRinci;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Nilai default field
	 * ({@code mulai}, {@code sampai}, {@code tanggal_dirubah}) diisi pada saat inisialisasi field.
	 */
	public JadwalSidangTugasAkhir() {

	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Nilai {@code null} berarti gelombang belum tersimpan; layar
	 * {@code JadwalSidangTugasAkhirAction} memakai kondisi ini untuk memilih judul jendela
	 * ("Tambah Jadwal" vs "Ubah Jadwal") dan untuk memutuskan apakah perubahan agenda rinci
	 * boleh langsung ditulis ke basis data.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Normalnya diisi Hibernate; pengisian manual hanya dipakai pada
	 * skenario impor/penyalinan data.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama gelombang sidang.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila nama masih {@code null} atau hanya berisi spasi,
	 * method ini <b>mengisi field</b> dengan teks default
	 * {@code "Jadwal sidang skripsi / tugas akhir "} (perhatikan spasi di ujung — ikut tersimpan).
	 * Karena {@code dynamicUpdate = true}, membaca nama pada instance yang masih <i>managed</i>
	 * dapat membuat teks default itu ter-{@code flush} ke kolom {@code nama} walau pengguna tidak
	 * menekan tombol simpan.
	 *
	 * @return nama gelombang; tidak pernah {@code null} setelah pemanggilan ini
	 */
	public String getNama() {
		if (nama == null || nama.trim().isEmpty()) {
			nama = "Jadwal sidang skripsi / tugas akhir ";
		}
		return nama;
	}

	/**
	 * Mengisi nama gelombang sidang.
	 *
	 * @param nama nama gelombang
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan program studi pemilik gelombang.
	 *
	 * <p>Relasi opsional ({@code nullable = true}) — gelombang tingkat fakultas atau universitas
	 * dibiarkan tanpa jurusan. Dipakai sebagai penyaring pada pencarian daftar gelombang dan
	 * pada Bandbox pemilihan jadwal di layar Skripsi. Berbeda dengan {@link #getRuangSidang()},
	 * getter ini <b>tidak</b> memanggil {@code check()}.
	 *
	 * @return jurusan pemilik, atau {@code null} bila gelombang tidak dibatasi per prodi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Mengisi program studi pemilik gelombang.
	 *
	 * @param jurusan jurusan pemilik, boleh {@code null}
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan fakultas pemilik gelombang.
	 *
	 * <p>Relasi opsional ({@code nullable = true}). Di layar, pilihan fakultas menentukan daftar
	 * jurusan yang boleh dipilih; bila entity baru, nilainya diambil dari fakultas pengguna yang
	 * sedang login. Getter ini tidak memanggil {@code check()}.
	 *
	 * @return fakultas pemilik, atau {@code null} bila gelombang tidak dibatasi per fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

	/**
	 * Mengisi fakultas pemilik gelombang.
	 *
	 * @param fakultas fakultas pemilik, boleh {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan keterangan bebas gelombang.
	 *
	 * <p>Dipakai juga sebagai kolom pencarian utama pada daftar gelombang (pencarian
	 * {@code ilike ANYWHERE} atas kolom ini).
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas gelombang.
	 *
	 * @param keterangan keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal awal berlakunya gelombang sidang.
	 *
	 * <p><b>Getter yang menulis balik:</b> bila field masih {@code null}, diisi dengan waktu
	 * server saat ini ({@code WaktuUtil.getDate()}) dan nilai itu ikut tersimpan pada
	 * {@code flush} berikutnya.
	 *
	 * <p>Dipetakan {@code TemporalType.DATE} sehingga <b>bagian jam tidak disimpan</b>. Ini
	 * penting karena {@code Skripsi} menyalin nilai ini ke {@code tanggalSidang} bila tanggal
	 * sidang skripsi kosong — jam sidang per mahasiswa karenanya tidak berasal dari sini,
	 * melainkan dari agenda rinci ({@link #getJadwalRinci()}) atau diisi manual di layar Skripsi.
	 *
	 * @return tanggal mulai; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		if (mulai == null) {
			mulai = ais.ui.util.WaktuUtil.getDate();
		}
		return mulai;
	}

	/**
	 * Mengisi tanggal awal berlakunya gelombang.
	 *
	 * @param mulai tanggal mulai
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal akhir berlakunya gelombang sidang.
	 *
	 * <p><b>Getter yang menulis balik:</b> bila field masih {@code null}, diisi dengan
	 * {@link #getMulai()} <b>ditambah 6 bulan</b> (memakai {@code Calendar} lenient, sehingga
	 * pergantian tahun dan panjang bulan ditangani otomatis) lalu nilai itu ikut tersimpan pada
	 * {@code flush} berikutnya. Perhatikan efek berantai: pemanggilan ini memanggil
	 * {@link #getMulai()}, yang sendiri dapat mengisi {@code mulai} bila masih kosong — jadi
	 * satu kali membaca {@code sampai} berpotensi mengubah <b>dua</b> kolom sekaligus.
	 *
	 * <p>Dipetakan {@code TemporalType.DATE} (tanpa jam), sama seperti {@link #getMulai()}.
	 *
	 * @return tanggal sampai; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		if (sampai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getMulai());
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
			sampai = calendar.getTime();
		}
		return sampai;
	}

	/**
	 * Mengisi tanggal akhir berlakunya gelombang.
	 *
	 * @param sampai tanggal sampai
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan tahun akademik gelombang (mis. {@code "2025/2026"}), sesuai pilihan pada
	 * combobox "Tahun Akademik" di layar. Wajib diisi saat menyimpan (divalidasi di layar,
	 * bukan di entity) dan menjadi salah satu penyaring daftar gelombang.
	 *
	 * <p>Perhatikan bahwa {@code tahunAkademik} dan {@link #getProgram() program} sama-sama
	 * {@code String} tanpa validasi tipe — pastikan memakai getter yang benar saat menyalin nilai.
	 *
	 * @return tahun akademik, atau {@code null} bila belum diisi
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik gelombang.
	 *
	 * @param tahunAkademik tahun akademik, mis. {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengisi ruang sidang default gelombang.
	 *
	 * @param ruangSidang ruang sidang, boleh {@code null}
	 */
	public void setRuangSidang(Ruang ruangSidang) {
		this.ruangSidang = ruangSidang;
	}

	/**
	 * Mengembalikan ruang sidang default gelombang ini.
	 *
	 * <p><b>Getter yang menulis balik referensi.</b> Nilai dilewatkan lebih dulu ke
	 * {@link GeneralValueObject#check(Object)} dan hasilnya <b>disimpan kembali</b> ke field.
	 * {@code check()} bertugas memuat/menyegarkan objek terpisah (<i>detached</i>) atau proxy
	 * yang belum terinisialisasi memakai sesi Hibernate tersendiri, sehingga getter ini aman
	 * dipanggil di luar sesi asal — tetapi juga berarti getter ini dapat memicu query basis data.
	 *
	 * <p>Relasi ini {@code FetchType.LAZY} dan opsional. Nilainya diwarisi oleh {@code Skripsi}:
	 * bila ruang sidang pada skripsi kosong sedangkan gelombangnya sudah menentukan ruang,
	 * {@code Skripsi} menyalin ruang dari sini.
	 *
	 * @return ruang sidang default, atau {@code null} bila belum ditentukan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_sidang", nullable = true)
	public Ruang getRuangSidang() {
		ruangSidang = check(ruangSidang);
		return this.ruangSidang;
	}

	/**
	 * Mengembalikan kode program pendidikan gelombang ini (isi combobox "Program" di layar,
	 * dibangkitkan {@code Common.initPrograms}), mis. kelas reguler vs kelas karyawan.
	 *
	 * <p>Tidak ada anotasi pemetaan eksplisit pada getter ini; Hibernate memakai akses properti
	 * sehingga nilainya dipetakan ke kolom {@code program} secara implisit. Berbeda dengan
	 * {@link #getTahunAkademik()}, nilai ini <b>tidak</b> menjadi penyaring pada pencarian daftar
	 * gelombang — hanya tersimpan dan ditampilkan di formulir.
	 *
	 * @return kode program, atau {@code null} bila tidak dibatasi per program
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Mengisi kode program pendidikan gelombang.
	 *
	 * @param program kode program, boleh {@code null}
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan agenda rinci gelombang dalam bentuk <b>teks mentah berserialisasi</b>
	 * (kolom {@code jadwal_rinci}, tipe {@code text}).
	 *
	 * <p>Nilai dinormalkan saat dibaca: {@code null} menjadi string kosong, selain itu di-{@code trim}.
	 * Karena Hibernate mengakses properti lewat getter ini, <b>nilai yang tersimpan ke basis data
	 * juga sudah ternormalisasi</b> — kolom ini praktis tidak pernah berisi {@code NULL} setelah
	 * sekali disimpan. Normalisasi tersebut <b>tidak</b> ditulis balik ke field, jadi getter ini
	 * tidak bersifat merusak.
	 *
	 * <p>Untuk membaca isinya secara terstruktur gunakan {@link #daftarJadwal()}; jangan mengurai
	 * string ini di luar class.
	 *
	 * @return teks agenda rinci yang sudah di-{@code trim}; string kosong bila belum ada agenda
	 */
	@Column(name = "jadwal_rinci", columnDefinition = "text")
	public String getJadwalRinci() {
		return jadwalRinci == null ? "" : jadwalRinci.trim();
	}

	/**
	 * Mengisi agenda rinci dalam bentuk teks mentah.
	 *
	 * <p>Dipakai layar untuk <b>mengosongkan</b> agenda ({@code setJadwalRinci("")}) sebelum
	 * membangun ulang seluruh baris agenda lewat
	 * {@link #populateJadwal(String, Date, Date, String)}. Hindari memanggilnya dengan teks yang
	 * disusun sendiri, karena format pemisahnya rapuh (lihat
	 * {@link #populateJadwal(String, Date, Date, String)}).
	 *
	 * @param jadwalRinci teks agenda rinci berserialisasi
	 */
	public void setJadwalRinci(String jadwalRinci) {
		this.jadwalRinci = jadwalRinci;
	}

	/**
	 * Menambahkan <b>atau memperbarui</b> satu baris agenda pada {@link #getJadwalRinci()}.
	 *
	 * <p><b>Cara kerja.</b> Baris agenda dibentuk sebagai
	 * {@code nama<>mulai<>sampai<>keterangan} dengan tanggal diformat memakai
	 * {@code Common.datetimeFormat1s} (pola {@code ddMMyyHHmmss}). Seluruh baris yang sudah ada
	 * ditelusuri; baris yang <b>namanya sama</b> (perbandingan tanpa memperhatikan besar-kecil
	 * huruf) <b>digantikan</b> oleh baris baru, sisanya dipertahankan apa adanya. Bila tidak ada
	 * yang cocok, baris baru ditambahkan di akhir. Jadi <b>nama agenda berperan sebagai kunci</b>.
	 *
	 * <p><b>Efek samping.</b> Hasilnya ditulis <b>langsung ke field</b> {@code jadwalRinci}
	 * (bukan lewat {@link #setJadwalRinci(String)}), dan method ini mencetak hasilnya ke
	 * {@code System.out} — sisa kode debug yang masih tertinggal di produksi. Method ini
	 * <b>tidak</b> menyentuh sesi Hibernate; penyimpanan dilakukan pemanggil, mis.
	 * {@code Common.refreshUpdate(...)} pada layar (sehingga saat mengubah gelombang yang sudah
	 * tersimpan, setiap perubahan sel agenda langsung ditulis ke basis data) atau
	 * {@code Common.refreshSaveOrUpdate(...)} pada tombol simpan.
	 *
	 * <p><b>Dipanggil dari.</b> {@code JadwalSidangTugasAkhirAction}: pendengar {@code onChange}
	 * tiap sel baris agenda, dan perakitan ulang seluruh agenda saat penyimpanan formulir.
	 *
	 * <p><b>Batasan format yang perlu diwaspadai</b> (perilaku nyata, bukan dugaan):
	 * <ul>
	 *   <li>{@code nama} dan {@code keterangan} dibersihkan hanya dari urutan literal {@code "||"}
	 *       dan {@code "<>"}. Karakter tunggal {@code '|'}, {@code '<'}, atau {@code '>'}
	 *       <b>lolos</b> — padahal pengurai memakai
	 *       {@code org.apache.commons.lang.StringUtils.split} yang memperlakukan argumen pemisah
	 *       sebagai <b>himpunan karakter</b>, sehingga satu karakter tersebut sudah cukup untuk
	 *       memecah baris/kolom secara tidak sengaja;</li>
	 *   <li>pemisah yang berdempetan dianggap satu, sehingga <b>kolom kosong menghilang</b>: bila
	 *       {@code tanggalMulai} atau {@code nama} kosong, kolom-kolom sesudahnya <b>bergeser</b>
	 *       saat dibaca kembali oleh {@link #daftarJadwal()} dan baris tersebut umumnya gagal
	 *       diurai lalu hilang dari daftar (lihat {@link #daftarJadwal()});</li>
	 *   <li>bila ada beberapa baris lama dengan nama yang sama, <b>semuanya</b> diganti dengan
	 *       baris baru yang identik sehingga menghasilkan duplikat.</li>
	 * </ul>
	 *
	 * @param nama           nama agenda; juga berfungsi sebagai kunci pencocokan baris
	 * @param tanggalMulai   waktu mulai agenda; boleh {@code null} (disimpan sebagai kolom kosong —
	 *                       lihat batasan format di atas)
	 * @param tanggalSampai  waktu selesai agenda; boleh {@code null}
	 * @param keterangan     keterangan agenda
	 */
	public void populateJadwal(String nama, Date tanggalMulai, Date tanggalSampai, String keterangan) {
		String r = "";
		nama = org.apache.commons.lang3.StringUtils
				.replace(org.apache.commons.lang3.StringUtils.replace(nama, "||", " "), "<>", " ").trim();
		keterangan = org.apache.commons.lang3.StringUtils
				.replace(org.apache.commons.lang3.StringUtils.replace(keterangan, "||", " "), "<>", " ").trim();

		// KE-FIX (NullPointerException): tanggalMulai/tanggalSampai bisa null bila Datebox
		// dikosongkan user -- SimpleDateFormat.format(null) melempar NPE via Calendar.setTime(null).
		String gabungan = nama + "<>" + (tanggalMulai == null ? "" : Common.datetimeFormat1s.get().format(tanggalMulai))
				+ "<>" + (tanggalSampai == null ? "" : Common.datetimeFormat1s.get().format(tanggalSampai)) + "<>"
				+ keterangan;

		boolean ada = false;
		String[] spl = StringUtils.split(getJadwalRinci(), "||");
		for (String s : spl) {

			String[] subS = StringUtils.split(s, "<>");
			String n = subS.length > 0 ? subS[0].trim() : "";
			if (n.equalsIgnoreCase(nama)) {
				r += r.isEmpty() ? gabungan : "||" + gabungan;
				ada = true;
			} else {
				r += r.isEmpty() ? s : "||" + s;
			}
		}

		if (!ada) {
			r += r.isEmpty() ? gabungan : "||" + gabungan;
		}

		jadwalRinci = r;
		System.out.println("jadwalRinci -> " + jadwalRinci);
	}

	/**
	 * Menghapus semua baris agenda yang namanya sama dengan {@code nama} dari
	 * {@link #getJadwalRinci()}.
	 *
	 * <p>Perbandingan nama dilakukan tanpa memperhatikan besar-kecil huruf, setelah {@code nama}
	 * dibersihkan dengan aturan yang sama seperti pada
	 * {@link #populateJadwal(String, Date, Date, String)}. Baris lain disusun ulang apa adanya
	 * dan hasilnya ditulis <b>langsung ke field</b> {@code jadwalRinci}; method ini tidak
	 * menyentuh basis data — penyimpanan dilakukan pemanggil ({@code Common.refreshUpdate(...)}
	 * pada layar, setelah pengguna mengonfirmasi dialog "Apakah yakin ingin menghapus data ini ?").
	 *
	 * <p>Bila tidak ada baris yang cocok, isi agenda tidak berubah (kecuali normalisasi
	 * {@code trim} yang dibawa {@link #getJadwalRinci()}). Bila nama yang diberikan kosong,
	 * baris yang kolom namanya juga kosong akan ikut terhapus.
	 *
	 * @param nama nama agenda yang hendak dihapus
	 */
	public void hapusJadwal(String nama) {
		String r = "";
		nama = org.apache.commons.lang3.StringUtils
				.replace(org.apache.commons.lang3.StringUtils.replace(nama, "||", " "), "<>", " ").trim();

		String[] spl = StringUtils.split(getJadwalRinci(), "||");
		for (String s : spl) {

			String[] subS = StringUtils.split(s, "<>");
			String n = subS.length > 0 ? subS[0].trim() : "";
			if (!n.equalsIgnoreCase(nama)) {
				r += r.isEmpty() ? s : "||" + s;
			}
		}

		jadwalRinci = r;
	}

	/**
	 * Mengurai {@link #getJadwalRinci()} menjadi daftar agenda terstruktur untuk ditampilkan di
	 * grid "Nama Acara / Jadwal — Waktu dan Tanggal Mulai — Waktu dan Tanggal Sampai —
	 * Keterangan".
	 *
	 * <p>Setiap elemen daftar adalah larik {@code Object[]} berisi empat unsur, dengan urutan
	 * dan tipe tetap:
	 * <ol start="0">
	 *   <li>{@code [0]} — {@code String} nama agenda;</li>
	 *   <li>{@code [1]} — {@code java.util.Date} waktu mulai, atau {@code null};</li>
	 *   <li>{@code [2]} — {@code java.util.Date} waktu sampai, atau {@code null};</li>
	 *   <li>{@code [3]} — {@code String} keterangan (string kosong bila tidak ada).</li>
	 * </ol>
	 * Layar memakai larik ini dua arah: untuk merender baris grid, dan — lewat atribut baris
	 * {@code "o"} — untuk merakit ulang agenda saat penyimpanan melalui
	 * {@link #populateJadwal(String, Date, Date, String)}.
	 *
	 * <p><b>Penanganan galat &amp; kehilangan data (perilaku nyata).</b> Setiap baris diurai di
	 * dalam {@code try/catch} sendiri: baris yang gagal diurai — misalnya karena kolom tanggal
	 * bergeser akibat kolom kosong atau karena isi mengandung karakter {@code '|'}/{@code '<'}/
	 * {@code '>'} — hanya dicatat ({@code printStackTrace} + {@code ErrorAuditUtil.record}) lalu
	 * <b>dilewati diam-diam</b>. Baris seperti itu tidak muncul di layar, dan karena penyimpanan
	 * membangun ulang agenda dari baris-baris yang tampil, baris tersebut <b>hilang permanen</b>
	 * pada penyimpanan berikutnya.
	 *
	 * <p>Method ini murni membaca ({@code getJadwalRinci()} tidak menulis field), tidak menyentuh
	 * sesi Hibernate, dan aman dipanggil berulang kali. Urutan hasil mengikuti urutan penyimpanan,
	 * <b>bukan</b> urutan kronologis tanggal.
	 *
	 * @return daftar agenda; daftar kosong bila agenda rinci kosong atau seluruh barisnya gagal
	 *         diurai — tidak pernah {@code null}
	 */
	public List<Object[]> daftarJadwal() {
		List<Object[]> list = new ArrayList<Object[]>();
		String[] spl = StringUtils.split(getJadwalRinci(), "||");
		for (String s : spl) {
			try {
				String[] subS = StringUtils.split(s, "<>");
				String n = subS.length > 0 ? subS[0].trim() : "";
				Date tanggalMulai = subS.length > 1 ? Common.datetimeFormat1s.get().parse(subS[1].trim()) : null;
				Date tanggalSampai = subS.length > 2 ? Common.datetimeFormat1s.get().parse(subS[2].trim()) : null;
				String keterangan = subS.length > 3 ? subS[3].trim() : "";
				list.add(new Object[] { n, tanggalMulai, tanggalSampai, keterangan });
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/JadwalSidangTugasAkhir.java:275");
			}
		}
		return list;
	}

}
