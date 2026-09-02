package ais.database.model;

// Generated Apr 12, 2010 11:30:55 AM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Entity <b>gelombang pendaftaran sidang tugas akhir (skripsi)</b> — tabel
 * {@code public.gelombang_pendaftaran_sidang_tugas_akhir}.
 *
 * <p><b>Apa yang sebenarnya disimpan class ini.</b> Satu baris = <b>satu jendela pendaftaran</b>
 * sidang tugas akhir yang dibuka petugas: nama gelombang ({@link #getNama()}), rentang tanggal
 * <i>pendaftaran</i> dibuka ({@link #getMulai()} … {@link #getSampai()}), <b>kuota</b> peserta
 * ({@link #getKuota()}), cakupan {@link Fakultas}/{@link Jurusan}/{@link #getProgram() program}/
 * {@link #getTahunAkademik() tahun akademik}, serta dua saklar tampil
 * ({@link #getAktif()}, {@link #getTetapTampilDiAdmin()}). Peserta <b>tidak</b> disimpan di sini;
 * arah relasinya terbalik — {@link Skripsi} yang menunjuk gelombang ini lewat properti
 * {@code Skripsi.gelombangPendaftaranSidangTugasAkhir} (kolom
 * {@code skripsi.gelombang_pendaftaran_sidang_tugas_akhir}). Diverifikasi dari kode: class ini
 * <b>tidak memiliki satu pun</b> properti {@code Mahasiswa}, {@code Dosen}, maupun {@code Ruang}.
 *
 * <h3>Perbandingan dengan {@link JadwalSidangTugasAkhir} — BEDA, bukan duplikat</h3>
 *
 * <p>Nama kedua class sangat mirip dan keduanya sama-sama menyimpan "satu gelombang", tetapi
 * pemeriksaan kode menunjukkan keduanya adalah <b>dua sumbu yang berbeda dan saling melengkapi</b>
 * pada alur tugas akhir yang sama:
 *
 * <table border="1" summary="Perbandingan dua entity gelombang">
 *   <tr><th>&nbsp;</th><th>{@code GelombangPendaftaranSidangTugasAkhir} (class ini)</th>
 *       <th>{@link JadwalSidangTugasAkhir}</th></tr>
 *   <tr><td>Tabel</td><td>{@code gelombang_pendaftaran_sidang_tugas_akhir}</td>
 *       <td>{@code jadwal_sidang_tugas_akhir}</td></tr>
 *   <tr><td>Menjawab</td><td>KAPAN mahasiswa boleh <b>mendaftar</b> sidang, dan BERAPA BANYAK</td>
 *       <td>KAPAN &amp; DI MANA sidang <b>dilaksanakan</b></td></tr>
 *   <tr><td>Properti khas</td><td>{@code kuota}, {@code aktif}, {@code tetapTampilDiAdmin}</td>
 *       <td>{@code ruangSidang}, {@code jadwalRinci} (+3 method bisnis pengurai agenda)</td></tr>
 *   <tr><td>Yang TIDAK dipunya</td><td>tidak ada ruang, tidak ada agenda rinci</td>
 *       <td>tidak ada kuota, tidak ada saklar aktif</td></tr>
 *   <tr><td>Dipakai {@link Skripsi} untuk</td>
 *       <td>penanda pendaftaran + pengelompokan laporan; tidak menyalin nilai apa pun ke
 *           {@code Skripsi}</td>
 *       <td>sumber <b>default</b> {@code Skripsi.tanggalSidang} dan {@code Skripsi.ruangSidang},
 *           sekaligus penanda "sidang disetujui"</td></tr>
 *   <tr><td>Layar master</td><td>{@code ais.action.master.GelombangPendaftaranSidangTugasAkhirAction}
 *       / {@code pages/master/gelombang_pendaftaran_sidang_tugas_akhir.zul}</td>
 *       <td>{@code ais.action.master.JadwalSidangTugasAkhirAction}</td></tr>
 * </table>
 *
 * <p>Bukti bahwa keduanya hidup berdampingan (bukan salah satu peninggalan): {@link Skripsi}
 * mendeklarasikan <b>dua</b> relasi {@code @ManyToOne} sekaligus —
 * {@code Skripsi.jadwalSidangTugasAkhir} dan
 * {@code Skripsi.gelombangPendaftaranSidangTugasAkhir} — dan {@code SkripsiAction} mengisi
 * keduanya pada satu form yang sama. Sebelas properti bersama
 * ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, {@code nama}, {@code mulai},
 * {@code sampai}, {@code jurusan}, {@code fakultas}, {@code program}, {@code tahunAkademik},
 * {@code keterangan}) memang identik karena ketiga entity gelombang tugas akhir
 * (class ini, {@link JadwalSidangTugasAkhir}, {@link JadwalSeminarTugasAkhir}) berasal dari
 * <b>satu cetakan salin-tempel</b> — bahkan nilai {@code serialVersionUID} ketiganya sama persis
 * ({@code -8842945307087672400L}) dan komentar generatornya sama-sama salah (lihat "Kuirk warisan
 * generator" di bawah).
 *
 * <h3>Posisi dalam alur tugas akhir</h3>
 * <ol>
 *   <li>Petugas prodi membuka gelombang pendaftaran — <b>baris class ini</b> — lewat layar
 *       <i>Gelombang Pendaftaran Sidang Tugas Akhir</i>;</li>
 *   <li>pada layar Skripsi, combobox "Gelombang Pendaftaran" diisi oleh
 *       {@code SkripsiAction.muatGelombangPendaftaranSidang(Mahasiswa)} dengan gelombang yang
 *       lolos <b>seluruh</b> penyaring: tahun akademik terpilih, {@code aktif}, kecocokan
 *       program/fakultas/jurusan mahasiswa, dan rentang tanggal hari ini (lihat
 *       {@link #getTetapTampilDiAdmin()} untuk pengecualiannya);</li>
 *   <li>{@code SkripsiAction.checkSyarat()} <b>menolak simpan</b> bila combobox itu kosong, dan
 *       menolak pula bila {@link #getKuota()} sudah terpenuhi (lihat {@link #getKuota()});</li>
 *   <li>gelombang tersimpan dipakai sebagai sumbu pengelompokan pada
 *       {@code LaporanGelombangSidang}, {@code LaporanRekapitulasiGelombangSidang},
 *       {@code LaporanRekapitulasiJudisium}, dan pada JSON
 *       {@code ais.action.master.resources.KelulusanResource}.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota class</h3>
 * <ul>
 *   <li><i>Jejak audit</i> — {@link #getOleh()}/{@link #getOlehId()},
 *       {@link #getTanggal_dirubah()} dan kait {@link #onUpdate()}; dideklarasikan ulang di sini
 *       karena {@link GeneralValueObject} bukan {@code @MappedSuperclass} (lihat catatan di
 *       bawah);</li>
 *   <li><i>Identitas &amp; deskripsi</i> — {@link #getId()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #toString()};</li>
 *   <li><i>Rentang pendaftaran</i> — {@link #getMulai()}, {@link #getSampai()};</li>
 *   <li><i>Cakupan/penyaring</i> — {@link #getFakultas()}, {@link #getJurusan()},
 *       {@link #getProgram()}, {@link #getTahunAkademik()};</li>
 *   <li><i>Pembatas &amp; saklar tampil</i> — {@link #getKuota()}, {@link #getAktif()},
 *       {@link #getTetapTampilDiAdmin()}.</li>
 * </ul>
 *
 * <p>Class ini <b>murni entity</b>: tidak ada method bisnis, tidak ada method utilitas/query
 * statis, tidak ada konstanta selain {@code serialVersionUID}. Seluruh logika pemakaian berada di
 * {@code SkripsiAction}, {@code GelombangPendaftaranSidangTugasAkhirAction}, dan kelas laporan.
 *
 * <h3>Pola berulang: hasil verifikasi langsung pada file ini</h3>
 * <ul>
 *   <li><b>Getter yang menutup sesi Hibernate — TIDAK ADA.</b> File ini tidak mengimpor
 *       {@code Session}, {@code HibernateUtil}, maupun {@code Criteria}; tidak ada satu pun
 *       akses basis data di dalamnya.</li>
 *   <li><b>Getter destruktif (menghapus/meng-{@code null}-kan nilai) — TIDAK ADA.</b> Tidak ada
 *       getter yang menyetel field menjadi {@code null} atau membuang data.</li>
 *   <li><b>Getter yang menulis balik ke field (dan berpotensi ke basis data) — ADA, tepat tiga:</b>
 *       {@link #getNama()} (mengisi nama default), {@link #getMulai()} (mengisi tanggal hari ini),
 *       dan {@link #getSampai()} (mengisi {@code mulai} + 6 bulan). Karena class ini memakai
 *       {@code dynamicUpdate = true}, membaca ketiganya pada instance yang masih <i>managed</i>
 *       dapat memicu {@code UPDATE} saat {@code flush} tanpa ada aksi simpan dari pengguna —
 *       dan perubahan itu akan ikut tercatat di riwayat Envers ({@code @Audited}).</li>
 *   <li><b>Asimetri penting.</b> Lima getter lain juga punya nilai default, tetapi
 *       <b>tidak</b> menulis balik: {@link #getTahunAkademik()} (default: tahun akademik berjalan),
 *       {@link #getProgram()} ({@code null} bila kosong), {@link #getKuota()} (default 1000),
 *       {@link #getAktif()} (default {@code true}), dan {@link #getTetapTampilDiAdmin()}
 *       (default {@code false}). Konsekuensinya: <b>nilai default hanya hidup di memori Java, kolom
 *       basis data tetap {@code NULL}</b>. Setiap query Criteria/HQL yang menyaring kolom-kolom itu
 *       harus menuliskan sendiri toleransi {@code NULL} — dan tidak semua pemakai melakukannya
 *       (lihat {@link #getAktif()} dan {@link #getTahunAkademik()}).</li>
 * </ul>
 *
 * <h3>Catatan {@code GeneralValueObject}</h3>
 * <p>Induk {@link GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity}
 * maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti induk. Karena itu
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b>
 * dideklarasikan ulang di sini; itu keharusan teknis, bukan duplikasi yang keliru. Berbeda dengan
 * {@link JadwalSidangTugasAkhir}, class ini bahkan tidak memakai utilitas
 * {@link GeneralValueObject#check(Object)} sama sekali — pewarisannya praktis hanya untuk
 * {@code Serializable} dan keseragaman tipe.
 *
 * <h3>Kuirk warisan generator</h3>
 * <p>Komentar Javadoc asli hasil {@code hbm2java} pada file ini berbunyi
 * "<i>JamPerkuliahan generated by hbm2java</i>" — <b>salah</b>, sisa salin-tempel dari entity
 * {@link JamPerkuliahan} yang sama sekali tidak berhubungan dengan sidang tugas akhir. Komentar
 * itu digantikan oleh Javadoc ini; fakta kekeliruannya dicatat di sini agar jejaknya tidak hilang.
 * Kekeliruan sejenis masih tersisa di berkas layarnya: judul popup tambah/ubah pada
 * {@code pages/master/gelombang_pendaftaran_sidang_tugas_akhir.zul} tertulis
 * "Tambah Masa Perkuliahan", dan tombol cetak di layar memberi judul jendela laporan
 * "Laporan Jadwal Sidang".
 *
 * @see Skripsi
 * @see JadwalSidangTugasAkhir
 * @see JadwalSeminarTugasAkhir
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "gelombang_pendaftaran_sidang_tugas_akhir")

public class GelombangPendaftaranSidangTugasAkhir extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance yang tersimpan di sesi HTTP/ZK tetap
	 * dapat dibaca setelah class dikompilasi ulang.
	 *
	 * <p>Nilainya <b>sama persis</b> dengan {@link JadwalSidangTugasAkhir} dan
	 * {@link JadwalSeminarTugasAkhir} — bukti bahwa ketiganya lahir dari satu salin-tempel.
	 * Praktik ini tidak berbahaya di sini (ketiganya class berbeda sehingga tidak pernah saling
	 * dideserialisasi), tetapi menghilangkan manfaat {@code serialVersionUID} sebagai penanda
	 * versi per class.
	 */
	private static final long serialVersionUID = -8842945307087672400L;

	/**
	 * Kunci utama (kolom {@code id}, {@code IDENTITY}). Bernilai {@code null} selama baris belum
	 * pernah disimpan — dipakai layar sebagai penanda "tambah" vs "ubah".
	 */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
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
	 * yang sama oleh generator sehingga tidak dapat diberi Javadoc tersendiri. Nilai awalnya
	 * adalah waktu server saat objek dibuat ({@code ais.ui.util.WaktuUtil.getDate()}), bukan
	 * waktu JVM mentah.
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
	 * <p>Tiga kuirk yang perlu diketahui: (1) method ini membaca <b>field mentah</b>, bukan getter,
	 * sehingga tidak memicu pengisian nilai default seperti {@link #getMulai()}/{@link #getSampai()}
	 * dan dapat menghasilkan potongan {@code "null"}; (2) potongan tanggal memakai
	 * {@code Date.toString()} bawaan Java (format Inggris) sehingga tidak layak dipakai sebagai
	 * label layar — label layar memakai {@link #getNama()}; (3) potongan jurusan memakai
	 * {@code Jurusan.toString()}, jadi memanggil {@code toString()} pada entity yang terlepas dari
	 * sesi dapat memicu inisialisasi proksi.
	 *
	 * @return gabungan tanggal mulai, tanggal sampai, dan jurusan dipisah garis bawah
	 */
	public String toString() {
		return mulai + "_" + sampai + "_" + jurusan;
	}

	/**
	 * Nama/judul gelombang, mis. "Gelombang I Pendaftaran Sidang Ganjil 2025/2026".
	 * Label layar: "Nama". Wajib diisi — {@code onSave()} pada layar master menolak nilai kosong.
	 */
	private String nama;

	/**
	 * Tanggal <b>pendaftaran dibuka</b>; diinisialisasi ke waktu server saat objek dibuat.
	 * Label layar: "Mulai".
	 */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Tanggal <b>pendaftaran ditutup</b>; diinisialisasi ke waktu server saat objek dibuat.
	 * Label layar: "Sampai".
	 */
	private Date sampai = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Batas jumlah skripsi yang boleh tertaut ke gelombang ini. {@code null} berarti "belum
	 * diisi" dan dibaca sebagai 1000 oleh {@link #getKuota()}. Label layar: "Kuota".
	 */
	private Integer kuota;

	/**
	 * Program studi/jurusan yang dicakup gelombang ini; {@code null} berarti <b>berlaku untuk
	 * semua jurusan</b>. Label layar: "Prodi".
	 */
	private Jurusan jurusan;

	/**
	 * Fakultas yang dicakup gelombang ini; {@code null} berarti <b>berlaku untuk semua fakultas</b>.
	 * Label layar: "Fakultas".
	 */
	private Fakultas fakultas;

	/**
	 * Program penyelenggaraan (mis. Reguler, Karyawan) yang dicakup; {@code null}/kosong berarti
	 * <b>berlaku untuk semua program</b>. Label layar: "Program".
	 */
	private String program;

	/**
	 * Tahun akademik gelombang ini, format mengikuti {@code Common.generateTahunAjaranDanSemua}.
	 * Wajib dipilih di layar master. Label layar: "Tahun Akademik".
	 */
	private String tahunAkademik;

	/** Catatan bebas untuk gelombang ini. Label layar: "Keterangan". */
	private String keterangan;

	/**
	 * Saklar aktif/nonaktif gelombang. {@code null} dibaca sebagai {@code true} oleh
	 * {@link #getAktif()}. Label layar: kolom centang "Aktif" pada baris grid — <b>bukan</b> pada
	 * form tambah/ubah.
	 */
	private Boolean aktif;

	/**
	 * Saklar "tetap tampilkan gelombang ini kepada operator meski rentang tanggalnya sudah lewat
	 * atau belum tiba". {@code null} dibaca sebagai {@code false} oleh
	 * {@link #getTetapTampilDiAdmin()}. Label layar: kolom centang "Tetap Tampil Di Admin" pada
	 * baris grid.
	 */
	private Boolean tetapTampilDiAdmin;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Objek baru sudah membawa
	 * {@code mulai}, {@code sampai}, dan {@code tanggal_dirubah} berisi waktu server; sisa
	 * properti dibiarkan {@code null} dan barulah bernilai default saat dibaca lewat getter
	 * masing-masing.
	 */
	public GelombangPendaftaranSidangTugasAkhir() {

	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan dengan {@code insertable = false} karena nilainya dihasilkan basis data
	 * ({@code IDENTITY}/{@code serial}), sehingga tidak pernah ikut pada pernyataan {@code INSERT}.
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
	 * Mengisi kunci utama. Hanya dipakai Hibernate dan kode yang membangun objek tiruan; layar
	 * tidak pernah memanggilnya langsung.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama gelombang, dengan pengisian default bila kosong.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila {@code nama} masih {@code null} atau hanya berisi
	 * spasi, method ini <b>mengisi field</b> dengan kalimat
	 * {@code "Gelombang pendaftaran sidang skripsi / tugas akhir tahun akademik " +}
	 * {@link #getTahunAkademik()} lalu mengembalikannya. Karena {@code nama} adalah properti
	 * terpetakan dan class memakai {@code dynamicUpdate = true}, sekadar <i>menampilkan</i> baris
	 * gelombang lama yang namanya kosong dapat menghasilkan {@code UPDATE} ke basis data pada saat
	 * {@code flush} — beserta satu revisi Envers.
	 *
	 * <p>Efek turunan yang perlu disadari: bila {@code tahunAkademik} juga masih {@code null},
	 * {@link #getTahunAkademik()} memasok <b>tahun akademik yang sedang berjalan saat itu</b>,
	 * sehingga nama yang "tertanam" bergantung pada kapan baris itu kebetulan pertama kali dibaca.
	 *
	 * <p>Dipanggil dari: renderer grid layar master (lewat {@code RevisiHelper.createNewRevisi}),
	 * combobox gelombang di {@code SkripsiAction}, pesan penolakan kuota, laporan
	 * {@code LaporanGelombangSidang}/{@code LaporanRekapitulasiGelombangSidang}/
	 * {@code LaporanRekapitulasiJudisium}, dan JSON {@code KelulusanResource}.
	 *
	 * @return nama gelombang; tidak pernah {@code null}
	 */
	public String getNama() {
		if (nama == null || nama.trim().isEmpty()) {
			nama = "Gelombang pendaftaran sidang skripsi / tugas akhir tahun akademik " + getTahunAkademik();
		}
		return nama;
	}

	/**
	 * Mengisi nama gelombang apa adanya (tanpa {@code trim}, tanpa penolakan nilai kosong —
	 * validasi "wajib isi" ada di layar, bukan di sini).
	 *
	 * @param nama nama gelombang
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan jurusan/prodi yang dicakup gelombang ini.
	 *
	 * <p>Relasi {@code @ManyToOne} ke kolom {@code jurusan} yang <b>boleh {@code null}</b>;
	 * {@code null} bermakna "berlaku untuk semua jurusan" — itulah sebabnya penyaring di
	 * {@code SkripsiAction} selalu berbentuk {@code isNull("jurusan") OR eq("jurusan", …)}.
	 * {@code FetchMode.SELECT} berarti jurusan diambil lewat query terpisah, bukan {@code JOIN}.
	 * Cascade {@code PERSIST}/{@code MERGE} hanya merambatkan penyimpanan; menghapus gelombang
	 * tidak menghapus jurusan.
	 *
	 * @return jurusan cakupan, atau {@code null} bila berlaku untuk semua jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Mengisi jurusan cakupan.
	 *
	 * @param jurusan jurusan cakupan; {@code null} berarti berlaku untuk semua jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan fakultas yang dicakup gelombang ini.
	 *
	 * <p>Sama seperti {@link #getJurusan()}: kolom boleh {@code null} dengan makna "berlaku untuk
	 * semua fakultas", {@code FetchMode.SELECT}, cascade hanya {@code PERSIST}/{@code MERGE}.
	 *
	 * <p>Catatan konsistensi: tidak ada validasi apa pun yang memastikan {@code fakultas} cocok
	 * dengan {@code jurusan.getFakultas()} — layar master hanya <i>memandu</i> pemilihan, sehingga
	 * pasangan yang tidak konsisten tetap dapat tersimpan dan membuat gelombang tak pernah lolos
	 * penyaring mana pun.
	 *
	 * @return fakultas cakupan, atau {@code null} bila berlaku untuk semua fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

	/**
	 * Mengisi fakultas cakupan.
	 *
	 * @param fakultas fakultas cakupan; {@code null} berarti berlaku untuk semua fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan keterangan bebas gelombang ini, apa adanya.
	 *
	 * @return keterangan, atau {@code null} bila belum pernah diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal pendaftaran dibuka, dengan pengisian default bila kosong.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila {@code mulai} bernilai {@code null}, field diisi
	 * dengan waktu server ({@code WaktuUtil.getDate()}) lalu dikembalikan. Objek yang baru dibuat
	 * lewat konstruktor tidak pernah masuk cabang ini karena field sudah diinisialisasi; cabang
	 * ini hanya tersentuh oleh baris lama yang kolomnya {@code NULL} di basis data — dan pada
	 * kasus itu pembacaan biasa dapat memicu {@code UPDATE} (lihat catatan pola di Javadoc kelas).
	 *
	 * <p>Dipetakan {@code TemporalType.DATE}: <b>hanya tanggal, tanpa jam</b>. Penyaring
	 * "hari ini masih dalam rentang" di {@code SkripsiAction} membandingkan kolom ini dengan
	 * {@code WaktuUtil.getDate()} memakai {@code le}, sehingga gelombang terbuka sejak awal hari.
	 *
	 * @return tanggal pendaftaran dibuka; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		if (mulai == null) {
			mulai = ais.ui.util.WaktuUtil.getDate();
		}
		return mulai;
	}

	/**
	 * Mengisi tanggal pendaftaran dibuka.
	 *
	 * <p>Tidak ada pemeriksaan {@code mulai <= sampai} di sini maupun di {@code onSave()} layar
	 * master, sehingga rentang terbalik dapat tersimpan dan menghasilkan gelombang yang tidak
	 * pernah muncul pada combobox mahasiswa.
	 *
	 * @param mulai tanggal pendaftaran dibuka; boleh {@code null} (akan diisi ulang saat dibaca)
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal pendaftaran ditutup, dengan pengisian default bila kosong.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila {@code sampai} bernilai {@code null}, method ini
	 * menghitung default <b>{@link #getMulai()} + 6 bulan</b> lewat {@code Calendar} milik
	 * {@code WaktuUtil} lalu <b>menyimpannya ke field</b>. Perhatikan bahwa perhitungan memakai
	 * {@code calendar.set(MONTH, get(MONTH) + 6)}: dalam mode {@code Calendar} lenient (bawaan),
	 * nilai bulan &gt; 11 digulirkan ke tahun berikutnya, jadi hasilnya benar — tetapi bila suatu
	 * saat kalender disetel non-lenient, ekspresi ini akan melempar
	 * {@code IllegalArgumentException}. Efek samping tambahan: memanggil {@link #getMulai()} di
	 * dalamnya berarti getter ini dapat ikut menulis balik {@code mulai}.
	 *
	 * <p>Dipetakan {@code TemporalType.DATE} — hanya tanggal, tanpa jam. Karena penyaring di
	 * {@code SkripsiAction} memakai {@code ge("sampai", hariIni)}, gelombang masih terbuka
	 * sepanjang hari terakhir.
	 *
	 * @return tanggal pendaftaran ditutup; tidak pernah {@code null}
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
	 * Mengisi tanggal pendaftaran ditutup.
	 *
	 * @param sampai tanggal pendaftaran ditutup; boleh {@code null} (akan dihitung ulang saat
	 *        dibaca, lihat {@link #getSampai()})
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan tahun akademik gelombang ini, atau <b>tahun akademik yang sedang berjalan</b>
	 * bila kolomnya masih {@code null}.
	 *
	 * <p><b>Nilai default TIDAK ditulis balik</b> ke field maupun ke basis data — berbeda dari
	 * {@link #getNama()}/{@link #getMulai()}/{@link #getSampai()}. Akibatnya ada celah semantik
	 * yang layak diketahui: penyaring gelombang di {@code SkripsiAction} dan {@code initCriteria()}
	 * layar master membandingkan <b>kolom</b> {@code tahunAkademik} dengan
	 * {@code Restrictions.eq(...)}; baris ber-{@code NULL} tidak pernah cocok, padahal getter ini
	 * mengklaim baris tersebut bertahun-akademik berjalan. Gelombang seperti itu praktis tidak
	 * pernah dapat dipilih meski tampak wajar di grid.
	 *
	 * <p>Sumber nilai berjalan: {@code Common.getCurrentTahunAkademik()} (yang mendelegasikan ke
	 * {@code CommonCurrentSessionHelper}), sehingga hasil method ini <b>bergantung pada konteks
	 * sesi/waktu</b> dan dapat berbeda antar pemanggilan.
	 *
	 * @return tahun akademik gelombang, atau tahun akademik berjalan bila belum diisi
	 */
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik gelombang. Layar master mewajibkan pilihan ini sebelum menyimpan.
	 *
	 * @param tahunAkademik tahun akademik
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan program penyelenggaraan yang dicakup, dinormalkan: {@code null} atau string
	 * yang hanya berisi spasi dikembalikan sebagai {@code null}, selebihnya dikembalikan
	 * ter-{@code trim}.
	 *
	 * <p><b>Normalisasi ini hanya di memori, tidak ditulis balik.</b> Bila kolom
	 * {@code program} di basis data berisi string kosong atau spasi, getter melaporkan
	 * "berlaku untuk semua program", tetapi penyaring
	 * {@code isNull("program") OR eq("program", programMahasiswa)} di
	 * {@code SkripsiAction.muatGelombangPendaftaranSidang} bekerja pada kolomnya dan
	 * <b>tidak</b> menganggapnya {@code NULL} — gelombang tersebut tidak akan pernah muncul untuk
	 * mahasiswa mana pun. {@code onSave()} layar master menyimpan {@code null} (bukan string
	 * kosong) untuk pilihan "semua", jadi kasus ini hanya muncul pada data lama/impor.
	 *
	 * @return nama program, atau {@code null} bila berlaku untuk semua program
	 */
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	/**
	 * Mengisi program penyelenggaraan apa adanya (tanpa normalisasi — normalisasi terjadi saat
	 * dibaca, lihat {@link #getProgram()}).
	 *
	 * @param program nama program; {@code null} berarti berlaku untuk semua program
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan kuota peserta gelombang ini, atau <b>1000</b> bila kolomnya masih
	 * {@code null}. Nilai default tidak ditulis balik ke field/basis data.
	 *
	 * <p><b>Inilah satu-satunya pembatas jumlah peserta di seluruh alur.</b> Penegakannya berada
	 * di {@code SkripsiAction.checkSyarat()}: sebelum menyimpan skripsi, jumlah baris
	 * {@link Skripsi} yang menunjuk gelombang terpilih dihitung
	 * ({@code Projections.rowCount()}), lalu simpan ditolak bila
	 * {@code getKuota() <= jumlah}. Tiga catatan penting soal penegakan itu:
	 * <ul>
	 *   <li>pemeriksaan <b>dilewati</b> bila skripsi sudah tertaut ke gelombang yang sama — jadi
	 *       menyunting ulang skripsi lama tidak pernah terganjal kuota, bahkan ketika kuota sudah
	 *       diturunkan di bawah jumlah peserta yang ada;</li>
	 *   <li>hitung-lalu-simpan tidak berada dalam kunci apa pun, sehingga dua penyimpanan
	 *       bersamaan dapat sama-sama lolos dan melewati kuota (kondisi balapan);</li>
	 *   <li>hitungan mencakup <b>semua</b> skripsi yang menunjuk gelombang, tanpa memandang status
	 *       atau pembatalan.</li>
	 * </ul>
	 *
	 * @return kuota peserta; tidak pernah {@code null}
	 */
	public Integer getKuota() {
		return kuota == null ? 1000 : kuota;
	}

	/**
	 * Mengisi kuota peserta.
	 *
	 * <p>Layar master mengambil nilai dari {@code Intbox} tanpa validasi, sehingga {@code null}
	 * (kolom dikosongkan) maupun angka nol/negatif dapat tersimpan. {@code null} akan dibaca
	 * sebagai 1000 oleh {@link #getKuota()}, sedangkan nilai &le; 0 menutup gelombang untuk
	 * pendaftar baru.
	 *
	 * @param kuota kuota peserta; boleh {@code null}
	 */
	public void setKuota(Integer kuota) {
		this.kuota = kuota;
	}

	/**
	 * Mengembalikan saklar aktif gelombang; {@code null} dibaca sebagai <b>{@code true}</b>
	 * (gelombang baru dianggap aktif). Nilai default tidak ditulis balik.
	 *
	 * <p><b>Saklar ini benar-benar ditegakkan</b> — berbeda dari beberapa flag "aktif" lain di
	 * repo ini yang hanya menyembunyikan baris di UI. Penegakannya ada di
	 * {@code SkripsiAction.muatGelombangPendaftaranSidang}: gelombang hanya masuk combobox bila
	 * {@code aktif IS NULL OR aktif = true}, dan {@code checkSyarat()} menolak simpan bila
	 * combobox itu kosong. Namun penegakan tersebut terjadi <b>saat memuat pilihan</b>, bukan saat
	 * menyimpan: menonaktifkan gelombang tidak memutus skripsi yang terlanjur tertaut, dan tidak
	 * ada pemeriksaan ulang atas gelombang yang sudah tersimpan pada baris skripsi.
	 *
	 * <p>Ketidakseragaman yang perlu diketahui: {@code LaporanRekapitulasiJudisium} memuat daftar
	 * gelombang dengan {@code Restrictions.eq("aktif", true)} <b>tanpa</b> toleransi {@code NULL},
	 * sehingga gelombang yang belum pernah disentuh centang "Aktif" (kolom masih {@code NULL},
	 * tetapi getter ini melaporkannya aktif) <b>tidak muncul</b> di filter laporan itu, padahal
	 * muncul di layar master dan di combobox Skripsi.
	 *
	 * @return {@code true} bila gelombang aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi saklar aktif gelombang.
	 *
	 * <p>Satu-satunya pemanggil di layar adalah centang "Aktif" pada <b>baris grid</b> layar
	 * master, yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate}. Form tambah/ubah
	 * tidak memuat kolom ini sama sekali, sehingga gelombang yang baru dibuat selalu berkolom
	 * {@code NULL} (dan dibaca sebagai aktif).
	 *
	 * @param aktif status aktif; {@code null} akan dibaca sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan saklar "tetap tampil di admin"; {@code null} dibaca sebagai
	 * <b>{@code false}</b>. Nilai default tidak ditulis balik.
	 *
	 * <p><b>Arti sebenarnya (diverifikasi dari
	 * {@code SkripsiAction.muatGelombangPendaftaranSidang}):</b> saklar ini adalah
	 * <b>pembebasan dari penyaring rentang tanggal</b>, dan hanya berlaku bagi pengguna yang
	 * <i>bukan</i> mahasiswa dan <i>bukan</i> siswa. Bentuk penyaringnya:
	 * <pre>
	 * (penggunaAdalahPetugas ? eq("tetapTampilDiAdmin", true) : sqlRestriction("false"))
	 *   OR (mulai &lt;= hariIni AND sampai &gt;= hariIni)
	 * </pre>
	 * Jadi bagi mahasiswa/siswa cabang pertama selalu bernilai salah sehingga rentang tanggal
	 * mutlak; bagi petugas, mencentang saklar ini membuat gelombang tetap dapat dipilih meski
	 * tanggalnya sudah lewat atau belum tiba — mekanisme resmi untuk pendaftaran susulan.
	 * Saklar ini <b>tidak</b> membuka data siapa pun; ia hanya melebarkan daftar pilihan
	 * gelombang.
	 *
	 * @return {@code true} bila gelombang tetap ditawarkan kepada petugas di luar rentang tanggal;
	 *         tidak pernah {@code null}
	 */
	public Boolean getTetapTampilDiAdmin() {
		return tetapTampilDiAdmin == null ? false : tetapTampilDiAdmin;
	}

	/**
	 * Mengisi saklar "tetap tampil di admin".
	 *
	 * <p>Seperti {@link #setAktif(Boolean)}, satu-satunya pemanggil di layar adalah centang pada
	 * baris grid layar master yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate};
	 * form tambah/ubah tidak memuatnya.
	 *
	 * @param tetapTampilDiAdmin status pembebasan rentang tanggal bagi petugas; {@code null} akan
	 *        dibaca sebagai {@code false}
	 */
	public void setTetapTampilDiAdmin(Boolean tetapTampilDiAdmin) {
		this.tetapTampilDiAdmin = tetapTampilDiAdmin;
	}

}
