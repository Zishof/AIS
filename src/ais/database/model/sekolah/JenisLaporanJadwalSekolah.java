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

import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>Jenis Laporan Jadwal Pelajaran</b> milik modul sekolah
 * (tabel {@code sekolah.jenis_laporan_jadwal_sekolah}).
 *
 * <h3>Domain (terverifikasi dari kode pemanggil)</h3>
 * <p>Baris pada tabel ini <b>bukan</b> kategori/klasifikasi jadwal pelajaran, melainkan
 * <b>profil varian template cetak JasperReports</b> untuk laporan Jadwal Pelajaran. Satu baris
 * = satu "bentuk cetakan" jadwal (mis. "Jadwal Per Kelas", "Jadwal Per Guru", "Jadwal Format
 * Dinas") yang dapat dipilih pengguna di layar laporan.</p>
 *
 * <p>Verifikasi label UI: dialog tambah/ubah pada
 * {@code ais.action.master.sekolah.JenisLaporanJadwalSekolahAction} berjudul <i>"Tambah/Ubah
 * Laporan Jadwal Pelajaran"</i> dengan medan wajib <i>"Nama Laporan Jadwal Pelajaran"</i> dan
 * medan berkas <i>"File Laporan (jrxml atau jasper)"</i>; layar laporan
 * {@code ais.action.report.format1.sekolah.LaporanJadwalPelajaran} menampilkan combo wajib
 * <i>"Jenis Laporan *"</i> yang diisi dari kelas ini.</p>
 *
 * <h3>PENTING: entity ini TIDAK menyimpan path berkas {@code .jrxml}</h3>
 * <p>Berbeda dari dugaan yang wajar, <b>tidak ada</b> kolom path/berkas di kelas ini. Berkas
 * template disimpan terpisah pada {@code ais.database.model.file.LampiranLain} dengan pasangan
 * kunci logis:</p>
 * <ul>
 *   <li>{@code ref} = {@link #getId()} baris ini, dan</li>
 *   <li>{@code jenis} = {@link ais.database.model.file.LampiranLain#FILE_JRXML_LAYOUT_JENIS_JADWAL}
 *       ({@code "File jrxml jenis jadwal"}).</li>
 * </ul>
 * <p>Jadi kelas ini berperan sebagai <b>label + jangkar (anchor) id</b> tempat lampiran template
 * digantungkan, bukan pemilik path-nya. Konsekuensi praktis: menghapus baris di sini tidak
 * otomatis menghapus berkas lampirannya (tidak ada kaskade ke {@code LampiranLain}), dan
 * sebaliknya lampiran dapat hilang/diganti tanpa mengubah baris ini sama sekali.</p>
 *
 * <h3>Alur pemakaian (dari master sampai cetak)</h3>
 * <ol>
 *   <li>Admin membuat baris di layar master (menu/berkas ZUL
 *       {@code /pages/master/sekolah/jenis_laporan_jadwal_sekolah.zul}) lalu mengunggah berkas
 *       {@code .jrxml}/{@code .jasper} lewat blok lampiran pada dialog yang sama.</li>
 *   <li>Di layar {@code LaporanJadwalPelajaran}, combo <i>"Jenis Laporan"</i> diisi lewat
 *       {@code Common.insertCombo(..., new String[]{"nama", "kode"}, "keterangan",
 *       JenisLaporanJadwalSekolah.class, and(eq("sekolah", s), eq("aktif", true)))} —
 *       artinya {@link #getNama()} dan {@link #getKode()} menjadi <i>label</i> item combo,
 *       {@link #getKeterangan()} menjadi <i>deskripsi</i> item, dan penyaringan dilakukan
 *       terhadap {@link #getSekolah()} + kolom {@code aktif}.</li>
 *   <li>Saat mencetak, {@code LampiranLain.ambil(id, FILE_JRXML_LAYOUT_JENIS_JADWAL)} dipakai
 *       untuk mendapatkan berkas fisik; path absolutnya dijejalkan ke parameter laporan
 *       {@code "nama_laporan"} (dipakai untuk SEMUA format ekspor: PDF/XLS/DOCX/PPTX) dan
 *       dikompilasi oleh {@code Report.generateCompileFileReport(...)}.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)} ({@code IDENTITY}, berurutan),
 *       {@link #toString()}.</li>
 *   <li><b>Jejak audit (deklarasi ulang wajib)</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan pengait
 *       {@code onUpdate()}.</li>
 *   <li><b>Atribut bisnis</b> — {@link #getKode()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #getAktif()} beserta setter-nya.</li>
 *   <li><b>Relasi tenant</b> — {@link #getSekolah()} dan {@link #getYayasan()} beserta
 *       setter-nya.</li>
 * </ul>
 *
 * <h3>Hal non-obvious (mudah salah paham)</h3>
 * <ul>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *       bug.</b> {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa — bukan
 *       {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate <b>tidak</b>
 *       memetakan properti kelas induk. Mendeklarasikan ulang properti tersebut di sini adalah
 *       <b>keharusan teknis</b> agar kolomnya benar-benar tersimpan.</li>
 *   <li><b>Getter relasi menulis balik ke field.</b> {@link #getSekolah()} menugaskan hasil
 *       {@code check(...)} kembali ke field, dan {@link #getYayasan()} bahkan
 *       <b>menimpa</b> {@code yayasan} dengan {@code sekolah.getYayasan()} setiap kali dibaca.
 *       Karena entity ini memakai <i>property access</i>, getter inilah yang dipanggil Hibernate
 *       saat dirty-check — konsekuensinya lihat catatan pada {@link #getYayasan()}.</li>
 *   <li><b>Normalisasi string tidak konsisten.</b> {@link #getKode()} mengembalikan {@code ""}
 *       untuk {@code null}, {@link #getNama()} mengembalikan {@code null} untuk {@code null},
 *       dan {@link #getKeterangan()} mengembalikan nilai mentah tanpa {@code trim()}. Pemanggil
 *       tidak boleh mengandaikan ketiganya berperilaku sama.</li>
 *   <li><b>Kolom {@code kode} praktis selalu kosong.</b> Tidak ada satu pun layar yang memanggil
 *       {@link #setKode(String)}; lihat catatan pada {@link #getKode()}.</li>
 *   <li><b>Kolom {@code aktif} tidak pernah ditulis saat pembuatan baris baru</b> — ini bug
 *       fungsional nyata yang membuat jenis laporan baru tak pernah muncul di layar cetak.
 *       Rinciannya didokumentasikan pada {@link #getAktif()}.</li>
 * </ul>
 *
 * <h3>Catatan hak akses (hasil audit, bukan anjuran perubahan kode)</h3>
 * <ul>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Layar master ini juga disematkan sebagai tab
 *       <i>"Jenis Jadwal Pelajaran"</i> di dalam layar laporan {@code LaporanJadwalPelajaran}
 *       (lewat {@code MyInclude("/pages/master/sekolah/jenis_laporan_jadwal_sekolah.zul")}).
 *       Karena {@code CommonPrivilages.checkPrevilages(...)} menyelesaikan hak terhadap
 *       {@code Common.getCurrentMenu()} — yaitu menu <i>laporan</i> saat disematkan, bukan menu
 *       master — hak CREATE/UPDATE/DELETE pada menu Laporan Jadwal Pelajaran otomatis menjadi
 *       hak penuh CRUD atas master ini. Tab tersebut hanya dirakit untuk akun yang bukan siswa
 *       dan bukan guru, tetapi itu penyaring peran kasar, bukan pemeriksaan hak akses.</li>
 *   <li><b>Cakupan tenant tidak ditegakkan di sisi server.</b> {@code initCriteria()} pada layar
 *       master hanya menambahkan penyaring dari nilai combo pencarian; bila combo kosong yang
 *       dipakai adalah {@code Restrictions.sqlRestriction("1=1")}. Pengisian combo sendiri
 *       bersifat <i>fail-open</i>: bila konteks sekolah aktif kosong dan pengguna tidak terikat
 *       sekolah/yayasan, seluruh yayasan/sekolah dimuat sebagai pilihan.</li>
 *   <li><b>Amplifier "unggah ulang template {@code .jrxml}".</b> {@code onSave()} memuat ulang
 *       baris berdasarkan id tanpa memverifikasi ulang kepemilikan sekolah, dan blok lampiran
 *       ({@code LampiranLain.createDownloadUploadFileLain}) memang terdokumentasi tidak
 *       memeriksa hak akses sama sekali. Karena JasperReports mengeksekusi ekspresi Java
 *       sisi-server saat kompilasi/render, mengganti berkas template adalah operasi berdampak
 *       eksekusi kode, bukan sekadar mengubah tata letak.</li>
 * </ul>
 *
 * <p><b>Catatan generator:</b> komentar asli <i>"Bank generated by hbm2java"</i> adalah sisa
 * salin-tempel dari {@code ais.database.model.Bank} (Apr 2010) dan tidak menggambarkan kelas
 * ini; komentar tersebut digantikan oleh dokumentasi ini.</p>
 *
 * @see ais.action.master.sekolah.JenisLaporanJadwalSekolahAction
 * @see ais.action.report.format1.sekolah.LaporanJadwalPelajaran
 * @see ais.database.model.file.LampiranLain#FILE_JRXML_LAYOUT_JENIS_JADWAL
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.JadwalPelajaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_laporan_jadwal_sekolah")
public class JenisLaporanJadwalSekolah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance yang tersimpan di session ZK/HTTP
	 * tetap dapat dibaca setelah kelas dikompilasi ulang. Jangan diubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama basis data ({@code IDENTITY}, berurutan). Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} bukan superclass yang dipetakan Hibernate.
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna terakhir yang mengubah baris ini (jejak audit ringan). Diisi oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh layar.
	 */
	private String oleh;
	/**
	 * Identitas (login id) pengguna terakhir yang mengubah baris ini. Pendamping {@link #oleh},
	 * diisi oleh interceptor audit yang sama.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * @return login id pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Efek samping non-obvious:</b> nilai {@code null} maupun string kosong/spasi
	 * <b>ditolak diam-diam</b> (method langsung {@code return}), sehingga memanggilnya dengan
	 * nilai kosong <i>tidak</i> menghapus jejak audit yang sudah ada.</p>
	 *
	 * @param olehId login id pengubah; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pengguna pengubah terakhir.
	 *
	 * <p><b>Efek samping non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong ditolak diam-diam sehingga jejak audit lama tidak terhapus.</p>
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Pengait JPA {@code @PreUpdate}: dijalankan Hibernate tepat sebelum {@code UPDATE} baris ini
	 * dikirim ke basis data, lalu mendelegasikan pengisian jejak audit
	 * ({@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p><b>Catatan tata letak:</b> pada baris kode yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah} — stempel waktu perubahan terakhir, diinisialisasi ke waktu server
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru sudah punya nilai sebelum
	 * sempat disimpan. Penggabungan dua deklarasi dalam satu baris adalah pola penyisipan
	 * otomatis lintas-entity di repositori ini; jangan dirapikan tanpa menyapu seluruh entity.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field instance; tidak boleh dipanggil manual dari kode
	 * aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh {@link #onUpdate()}; pemanggilan manual hanya dipakai pada
	 * jalur impor/migrasi data yang ingin mempertahankan waktu asli.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berformat {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@link #nama} secara <b>langsung</b> (bukan lewat {@link #getNama()}),
	 * sehingga hasilnya tidak di-{@code trim()} dan dapat berisi literal {@code "null-null"}
	 * untuk objek baru yang belum diisi. Dipakai untuk log/debug, bukan untuk tampilan
	 * pengguna — label combo dan grid memakai {@link #getNama()}.</p>
	 *
	 * @return teks gabungan id dan nama.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode pendek jenis laporan. Dipakai sebagai bagian kedua label item combo di layar laporan
	 * ({@code new String[]{"nama", "kode"}}). Lihat catatan pada {@link #getKode()}: tidak ada
	 * layar yang mengisinya.
	 */
	private String kode;

	/** Nama jenis laporan jadwal (wajib, tidak boleh {@code NULL} di basis data). */
	private String nama;
	/** Sekolah pemilik baris ini — dimensi tenant utama dan penyaring combo di layar laporan. */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik baris ini. Bersifat turunan: {@link #getYayasan()} selalu menyelaraskannya
	 * ulang dari {@link #sekolah} bila sekolah terisi.
	 */
	private Yayasan yayasan;
	/** Keterangan bebas; ditampilkan sebagai deskripsi item combo dan kolom grid master. */
	private String keterangan;
	/**
	 * Saklar aktif/nonaktif. Perhatikan perbedaan penting antara nilai kolom
	 * ({@code NULL}/{@code true}/{@code false}) dan hasil {@link #getAktif()} — lihat catatan
	 * di getter tersebut.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi entity, dan
	 * dipakai layar master saat menekan tombol "Tambah" ({@code init(new
	 * JenisLaporanJadwalSekolah())}). Tidak mengisi nilai baku apa pun selain
	 * {@link #tanggal_dirubah} yang diinisialisasi pada deklarasi field.
	 */
	public JenisLaporanJadwalSekolah() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Selain sebagai identitas basis data, nilai ini juga merupakan <b>kunci acuan
	 * ({@code ref}) berkas template</b> pada {@code LampiranLain} — lihat dokumentasi kelas.
	 * Strategi {@code IDENTITY} berarti id berurutan dan mudah ditebak.</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate setelah {@code INSERT}
	 * atau oleh jalur impor data.
	 *
	 * @param id kunci utama baru; boleh {@code null} untuk objek baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode pendek jenis laporan dalam bentuk sudah dipangkas spasi.
	 *
	 * <p><b>Kuirk terverifikasi:</b> tidak ada satu pun layar di basis kode ini yang memanggil
	 * {@link #setKode(String)} — dialog tambah/ubah pada layar master hanya menyediakan medan
	 * Nama, Yayasan, Sekolah, Keterangan, dan berkas lampiran. Akibatnya kolom {@code kode}
	 * praktis selalu {@code NULL} dan label item combo di layar laporan (yang dirakit dari
	 * {@code {"nama", "kode"}}) efektif hanya berisi namanya saja. Satu-satunya jalur yang
	 * berpotensi mengisinya adalah impor massal Excel, dan kolom tersebut pun tidak termasuk
	 * daftar kolom yang dipetakan layar master.</p>
	 *
	 * <p>Berbeda dari {@link #getNama()}, getter ini mengembalikan string kosong (bukan
	 * {@code null}) ketika nilainya belum diisi.</p>
	 *
	 * @return kode terpangkas, atau {@code ""} bila belum diisi (tidak pernah {@code null}).
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode pendek jenis laporan.
	 *
	 * @param kode kode baru; boleh {@code null}.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis laporan jadwal dalam bentuk sudah dipangkas spasi.
	 *
	 * <p>Ini adalah label utama entity: dipakai sebagai teks kolom pertama grid master (lewat
	 * {@code RevisiHelper.createNewRevisi(...)} sehingga sekaligus menjadi tautan riwayat
	 * revisi Envers) dan sebagai label item combo <i>"Jenis Laporan"</i> di layar cetak.</p>
	 *
	 * <p><b>Kuirk:</b> mengembalikan {@code null} (bukan {@code ""}) bila nilainya belum diisi —
	 * kebalikan perilaku {@link #getKode()}.</p>
	 *
	 * @return nama terpangkas, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis laporan jadwal.
	 *
	 * <p>Dipanggil {@code onSave()} layar master setelah validasi "Nama Jenis Laporan Jadwal
	 * harus diisi". Tidak ada normalisasi maupun uji keunikan di sini — dua baris dengan nama
	 * identik pada sekolah yang sama dapat dibuat dan akan tampil sebagai dua item combo yang
	 * tak terbedakan di layar cetak.</p>
	 *
	 * @param nama nama baru; nilai mentah dari kotak teks (belum dipangkas).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas jenis laporan ini.
	 *
	 * <p><b>Berbeda dari {@link #getNama()} dan {@link #getKode()}, getter ini mengembalikan
	 * nilai mentah</b> — tanpa {@code trim()} dan tanpa penggantian {@code null}. Pemanggil
	 * harus siap menerima {@code null}; layar master memang meneruskannya langsung ke
	 * {@code new Label(...)}, dan layar laporan memakainya sebagai deskripsi item combo.</p>
	 *
	 * @return keterangan apa adanya, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}/kosong.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif dengan <b>anggapan optimistis</b>: nilai kolom {@code NULL}
	 * diperlakukan sebagai {@code true}.
	 *
	 * <h4>BUG FUNGSIONAL TERVERIFIKASI — jenis laporan baru tidak pernah muncul di layar cetak</h4>
	 * <p>Anggapan optimistis di atas hanya berlaku <b>di dalam Java</b>. Layar laporan menyaring
	 * pilihan combo dengan {@code Restrictions.eq("aktif", true)}, yaitu perbandingan
	 * <b>di sisi SQL</b>, dan di SQL {@code NULL = true} tidak pernah bernilai benar. Sementara
	 * itu {@code onSave()} layar master <b>tidak pernah</b> memanggil {@link #setAktif(Boolean)};
	 * dengan {@code dynamicInsert = true}, kolom {@code aktif} bahkan tidak ikut disertakan pada
	 * pernyataan {@code INSERT} sehingga tetap {@code NULL}.</p>
	 * <p>Akibatnya, jenis laporan yang baru dibuat:</p>
	 * <ul>
	 *   <li>tampak <b>tercentang "Aktif"</b> di grid master (karena renderer memanggil getter ini
	 *       yang memetakan {@code NULL} menjadi {@code true}), tetapi</li>
	 *   <li><b>tidak pernah muncul</b> pada combo "Jenis Laporan" di layar cetak — sehingga
	 *       templat {@code .jrxml} yang sudah diunggah tidak dapat dipakai sama sekali.</li>
	 * </ul>
	 * <p>Satu-satunya jalur pemulihan lewat UI adalah menekan checkbox "Aktif" di grid master
	 * <b>dua kali</b> (mati lalu hidup lagi): klik pertama menulis {@code false}, klik kedua
	 * menulis {@code true} sungguhan lewat {@code Common.refreshSaveOrUpdate(...)}. Jalur impor
	 * massal Excel juga dapat mengisinya karena {@code "aktif"} termasuk kolom yang dipetakan.
	 * Pola ini kembar dengan temuan pada {@code JenisCatatanSiswa} dan {@code JenisNilaiSiswa}.</p>
	 *
	 * @return {@code true} bila kolom bernilai {@code true} atau {@code NULL}; {@code false}
	 *         hanya bila kolom benar-benar bernilai {@code false}.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif.
	 *
	 * <p>Satu-satunya pemanggil di layar master adalah listener {@code onCheck} checkbox "Aktif"
	 * pada grid, yang langsung diikuti {@code Common.refreshSaveOrUpdate(...)} sehingga
	 * perubahan tersimpan seketika tanpa membuka dialog. Checkbox itu dinonaktifkan bila
	 * pengguna tidak memiliki hak UPDATE.</p>
	 *
	 * @param aktif status baru; boleh {@code null} (diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()}, tetapi <b>tidak</b> oleh penyaring SQL layar cetak).
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini (dimensi tenant utama).
	 *
	 * <p><b>Getter dengan efek samping:</b> memanggil
	 * {@link GeneralValueObject#check(Object) check(...)} lalu <b>menugaskan kembali</b> hasilnya
	 * ke field {@code sekolah}. Tujuannya menyelesaikan proxy lazy Hibernate menjadi objek nyata
	 * agar pemanggil tidak meledak di luar session; efek sampingnya adalah field instance dapat
	 * berubah menjadi objek berbeda (kanonik) hanya karena dibaca.</p>
	 *
	 * <p>Dipakai layar master untuk kolom "Sekolah" pada grid dan untuk memilih ulang combo
	 * Sekolah saat dialog ubah dibuka, serta oleh layar cetak sebagai penyaring daftar jenis
	 * laporan yang boleh dipilih.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila baris tidak terikat sekolah mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris ini.
	 *
	 * <p><b>Perilaku non-obvious:</b> objek {@code Sekolah} yang belum tersimpan (id
	 * {@code null}) diperlakukan sama dengan {@code null} dan <b>tidak</b> disimpan ke field.
	 * Ini mencegah Hibernate mencoba mengaitkan baris ke entity transien lewat kaskade
	 * {@code PERSIST}/{@code MERGE}, tetapi juga berarti pemanggil bisa "menyetel" nilai yang
	 * senyap tidak berlaku.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id akan menghasilkan
	 *                {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini, <b>diselaraskan ulang dari sekolah setiap kali
	 * dibaca</b>.
	 *
	 * <p><b>Getter destruktif — perhatikan urutan kerjanya:</b></p>
	 * <ol>
	 *   <li>memanggil {@link #getSekolah()} (yang sendirinya menulis balik field {@code sekolah});</li>
	 *   <li>bila sekolah terisi, field {@code yayasan} <b>ditimpa</b> dengan
	 *       {@code sekolah.getYayasan()} — nilai yang sebelumnya disetel lewat
	 *       {@link #setYayasan(Yayasan)} hilang tanpa peringatan;</li>
	 *   <li>hasilnya dilewatkan {@code check(...)} dan ditugaskan kembali ke field.</li>
	 * </ol>
	 * <p>Konsekuensi praktis: kolom {@code yayasan_id} <b>tidak pernah</b> dapat menyimpang dari
	 * yayasan induk sekolahnya selama {@code sekolah_id} terisi, sehingga {@code setYayasan}
	 * pada {@code onSave()} layar master efektif hanya berlaku untuk baris yang sekolahnya
	 * kosong. Karena entity memakai <i>property access</i>, getter ini juga dipanggil Hibernate
	 * saat dirty-check, sehingga penyelarasan tersebut dapat menghasilkan {@code UPDATE} nyata
	 * (dan revisi Envers baru) semata-mata karena objek dibaca. Perilakunya konsisten dengan
	 * entity master modul sekolah lain dan bukan bug — tetapi jangan pernah menganggap
	 * {@code yayasan} sebagai nilai yang dapat diisi bebas.</p>
	 *
	 * @return yayasan pemilik (turunan dari sekolah bila ada), atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik baris ini.
	 *
	 * <p>Seperti {@link #setSekolah(Sekolah)}, objek tanpa id diperlakukan sebagai {@code null}.
	 * Perlu diingat nilai yang disetel di sini akan <b>ditimpa</b> oleh {@link #getYayasan()}
	 * pada pembacaan berikutnya bila {@link #getSekolah()} tidak {@code null}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id menghasilkan {@code null}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

}
