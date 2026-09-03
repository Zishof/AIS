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
 * Entity MASTER "Jenis Nilai Siswa" pada modul sekolah — satu baris tabel
 * {@code sekolah.jenis_nilai_siswa} mewakili SATU <b>varian/profil cetak</b> untuk laporan rekap
 * nilai satu kelas, lengkap dengan berkas template JasperReports ({@code .jrxml}/{@code .jasper})
 * yang diunggah khusus untuk varian itu.
 *
 * <h3>Domain sebenarnya — BUKAN kategori penilaian rapor</h3>
 * Nama kelas ini sangat mudah disalahartikan. Berdasarkan penelusuran SELURUH pembacanya di kode
 * sumber, entity ini <b>BUKAN</b>:
 * <ul>
 * <li><b>bukan</b> kategori nilai rapor Kurikulum 2013 (Pengetahuan / Keterampilan / Sikap);</li>
 * <li><b>bukan</b> jenis ulangan (UH / UTS / UAS / Tugas);</li>
 * <li><b>bukan</b> skala/bobot nilai.</li>
 * </ul>
 * Ketiga hal di atas dimodelkan oleh keluarga entity yang SAMA SEKALI BERBEDA dan tidak punya
 * relasi apa pun ke kelas ini: {@link ais.database.model.sekolah.JenisPenilaian} (dirujuk dari
 * {@code Matapelajaran} dan {@link ais.database.model.sekolah.KurikulumSekolah}),
 * {@code DetailJenisPenilaian}, {@code GrupPenilaian} (beserta {@code formula}-nya),
 * {@code DetailGrupPenilaian}, dan {@code GrupKategoriItemPenilaianSiswa}. Nilai siswa yang
 * sebenarnya diambil lewat {@code KelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(...)}, sama sekali
 * tidak melalui entity ini.
 *
 * <p>Yang sesungguhnya diwakili entity ini adalah <b>bentuk keluaran laporan</b>: petugas
 * mendefinisikan beberapa "Jenis Nilai" (mis. satu untuk rekap wali kelas, satu untuk arsip
 * kurikulum, satu untuk format dinas), mengunggah template {@code .jrxml} yang berbeda pada
 * masing-masing, lalu operator laporan tinggal memilih salah satunya di layar
 * <b>Laporan Rekap Total Nilai</b>. Jadi kelas ini lebih dekat ke "profil template cetak" daripada
 * ke "jenis nilai" dalam arti pedagogis.</p>
 *
 * <h3>Satu-satunya pembaca runtime</h3>
 * Di luar layar master-nya sendiri, HANYA ADA SATU kelas yang membaca entity ini:
 * {@code ais.action.report.format1.sekolah.LaporanRekapTotalNilai}. Alurnya:
 * <ol>
 * <li>Setelah pengguna memilih Kelas, combo "Jenis Nilai" diisi lewat
 * {@code Common.insertComboDanSemua(...)} dengan kriteria keras:
 * {@code (kurikulumSekolah IS NULL OR kurikulumSekolah = kelas.kurikulumSekolah)
 * AND sekolah = kelas.sekolah AND aktif = true}, ditambah satu item kosong berlabel
 * <i>"= Tanpa Jenis Penilaian ="</i>.</li>
 * <li>Bila pengguna memilih item kosong ({@code null}), laporan memakai jalur BAWAAN: pratinjau
 * spreadsheet ({@code MySpreadsheet}) plus tombol "Ambil File" → {@code rekap_nilai.xlsx}.</li>
 * <li>Bila pengguna memilih sebuah {@code JenisNilaiSiswa}, laporan mencari lampiran
 * {@link ais.database.model.file.LampiranLain} berjenis
 * {@code LampiranLain.FILE_JRXML_LAYOUT_JENIS_NILAI} ({@code "File jrxml jenis nilai"}) dengan
 * {@code ref = }{@link #getId()}. Bila ADA, seluruh pratinjau bawaan (grid, centang Total/Max/Min,
 * tombol Ambil File) DISEMBUNYIKAN dan laporan dirender penuh oleh JasperReports memakai berkas
 * itu, dengan tombol ekspor PDF/XLS/DOCX/PPTX. Bila TIDAK ADA, {@code onCetak(...)} berhenti dengan
 * pesan <i>"File template jenis nilai siswa belum diupload"</i>.</li>
 * <li>{@link #getBerdasarkanMk()} menentukan ORIENTASI baris datasource yang diserahkan ke template
 * tersebut — lihat dokumentasi method itu.</li>
 * </ol>
 *
 * <h3>Layar pengelola (dua titik masuk)</h3>
 * <ul>
 * <li>Menu master tersendiri: {@code WEB-INF/z/x/y/pages/master/sekolah/jenis_nilai_siswa.zul} yang
 * di-{@code apply} oleh {@code ais.action.master.sekolah.JenisNilaiSiswaAction} (kolom grid: Nama
 * Jenis Nilai, Sekolah, Kurikulum, Keterangan, Aktif, Per Matapelajaran).</li>
 * <li>Tab tertanam: {@code ais.action.master.sekolah.PenilaianSiswaAction.onJenisNilai(...)}
 * menyisipkan {@code .zul} yang SAMA lewat {@code MyInclude} ke dalam layar Penilaian Siswa,
 * sehingga layar ini juga muncul sebagai tab di modul lain.</li>
 * </ul>
 * Selain formulir, layar master menyediakan dua kanal tulis massal bawaan {@code Common}:
 * {@code Common.cetakData(...)} (ekspor) dan {@code Common.uploadData(...)} (impor spreadsheet)
 * dengan kolom {@code id, nama, sekolah, kurikulumSekolah, keterangan, aktif} — perhatikan
 * {@code aktif} ADA di jalur impor tetapi TIDAK ADA di formulir (lihat {@link #getAktif()}).
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit warisan</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}, konstruktor tanpa argumen,
 * {@link #toString()}.</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 * {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 * <li><b>Isi profil</b> — {@link #getKode()}, {@link #getNama()}, {@link #getKeterangan()},
 * {@link #getAktif()} beserta setter-nya.</li>
 * <li><b>Perilaku laporan</b> — {@link #getBerdasarkanMk()}/{@link #setBerdasarkanMk(Boolean)},
 * {@link #getKurikulumSekolah()}/{@link #setKurikulumSekolah(KurikulumSekolah)}.</li>
 * </ul>
 * Entity ini TIDAK memiliki satu pun method bisnis, query/finder statis, {@code equals}/
 * {@code hashCode}, maupun {@code compareTo()} sendiri; seluruh isinya getter/setter properti plus
 * satu callback JPA dan satu {@code toString()}.
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 * <li><b>Komentar hbm2java di atas anotasi keliru</b> — teks aslinya berbunyi "Bank generated by
 * hbm2java", sisa salin-tempel generator dari entity perbankan. Kelas ini tidak ada hubungannya
 * dengan bank. Kekeliruan sejenis tersebar di banyak entity {@code sekolah}.</li>
 * <li><b>Field warisan yang dideklarasikan ULANG bukan bug</b> — {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} sudah ada di induk
 * {@link ais.database.model.GeneralValueObject}, namun induk itu BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass} — hanya POJO abstrak biasa. Hibernate TIDAK memetakan properti induk,
 * sehingga setiap entity turunan HARUS mendeklarasikan ulang keempatnya agar tersimpan. Ini
 * keharusan teknis; jangan "dirapikan".</li>
 * <li><b>Tiga getter melakukan MUTASI saat dibaca</b> — {@link #getSekolah()} dan
 * {@link #getKurikulumSekolah()} menulis balik hasil resolusi proxy
 * {@link ais.database.model.GeneralValueObject#check(Object)}, sedangkan {@link #getYayasan()}
 * lebih jauh lagi: ia MENIMPA nilai kolomnya dengan yayasan turunan dari {@code sekolah}. Pada
 * instance terkelola, mutasi itu ikut ter-{@code flush} — sekadar MEMBACA baris bisa menghasilkan
 * {@code UPDATE} sekaligus satu revisi Envers baru.</li>
 * <li><b>Tiga kontrak {@code null} yang berbeda dalam satu kelas</b> — {@link #getKode()}
 * mengembalikan {@code ""} untuk {@code null}, {@link #getNama()} mengembalikan {@code null} apa
 * adanya (hanya di-{@code trim} bila ada isinya), dan {@link #getKeterangan()} mengembalikan nilai
 * mentah tanpa {@code trim} sama sekali. Pemanggil tidak boleh menganggap ketiganya seragam.</li>
 * <li><b>Kolom {@code kode} praktis mati</b> — tidak ada satu pun layar/impor yang mengisinya
 * ({@code setKode} tidak pernah dipanggil di luar Hibernate), padahal laporan memakainya sebagai
 * properti tampilan kedua pada combo. Lihat {@link #getKode()}.</li>
 * <li><b>{@code aktif} tidak pernah ditulis oleh formulir simpan</b> — akibatnya baris yang baru
 * dibuat lewat tombol "Tambah" berpotensi tidak pernah muncul di combo laporan. Ini bug fungsional
 * nyata; rinciannya di {@link #getAktif()}.</li>
 * <li><b>Filter "Tampilkan hanya yang aktif" di layar master adalah kendali mati</b> — berkas
 * {@code jenis_nilai_siswa.zul} mendeklarasikan {@code <checkbox id="searchaktif" ...
 * checked="true"/>}, tetapi {@code JenisNilaiSiswaAction} tidak punya field bernama
 * {@code searchaktif} (jadi tidak pernah di-autowire) dan {@code initCriteria(...)}-nya tidak
 * pernah menyaring kolom {@code aktif}. Mencentang/melepasnya hanya memicu pencarian ulang tanpa
 * efek penyaringan apa pun.</li>
 * <li><b>{@code @Audited} + {@code hbm2ddl.auto=update}</b> — entity ini direkam Envers ke schema
 * {@code new_audit}. Sesuai catatan di {@code hibernate.cfg.xml}, penambahan kolom baru ke kelas
 * ini WAJIB diikuti {@code ALTER TABLE} manual pada tabel {@code new_audit.*__audit}-nya; bila
 * tidak, {@code INSERT} audit gagal, transaksi di-{@code rollback}, dan data pengguna TIDAK
 * tersimpan sama sekali.</li>
 * <li><b>{@code dynamicInsert}/{@code dynamicUpdate}</b> — Hibernate hanya menyertakan kolom yang
 * benar-benar berubah/terisi. Kolom bernilai {@code null} (mis. {@code aktif} pada baris baru)
 * tidak ikut dalam {@code INSERT}, sehingga nilainya bergantung pada {@code DEFAULT} kolom di
 * basis data — dan kolom yang dibuat otomatis oleh {@code hbm2ddl} tidak punya {@code DEFAULT}.</li>
 * </ul>
 *
 * <h3>Pola arsitektur repo yang diperiksa pada berkas ini</h3>
 * <ul>
 * <li><b>Getter write-back / destruktif — ADA.</b> {@link #getYayasan()} adalah varian destruktif
 * (menimpa kolom dengan nilai turunan), {@link #getSekolah()} dan
 * {@link #getKurikulumSekolah()} varian resolusi-proxy yang lebih jinak.</li>
 * <li><b>{@code getKeterangan()} membalik kontrak — TIDAK ADA.</b> Di kelas ini
 * {@link #getKeterangan()} benar-benar hanya mengembalikan field {@code keterangan}.</li>
 * <li><b>{@code compareTo()} dipangkas — TIDAK ADA.</b> Kelas ini tidak mengimplementasikan
 * {@code Comparable} dan tidak punya {@code compareTo()} sendiri.</li>
 * <li><b>Penciutan {@code TreeSet} — TIDAK ADA di berkas ini</b> (kelas ini tidak punya koleksi
 * sama sekali). Catatan bertetangga: pembacanya, {@code LaporanRekapTotalNilai}, memang memakai
 * {@code TreeSet<Double>} untuk menghitung rangking — dua siswa dengan total identik menempati
 * rangking yang sama karena nilai kembar menyatu di dalam himpunan. Itu perilaku kelas lain, bukan
 * kelas ini.</li>
 * <li><b>Fail-open cakupan tenant sekolah/yayasan — ADA di layar pengelolanya, bukan di entity.</b>
 * {@code JenisNilaiSiswaAction.initCriteria(...)} tidak memasang satu pun batasan tenant miliknya
 * sendiri: bila combo Yayasan/Sekolah tidak terpilih, kriterianya berubah menjadi
 * {@code Restrictions.sqlRestriction("1=1")}. Penyaringan sepenuhnya bergantung pada combo yang
 * diisi {@code InitComboUtil.initYayasanDanSekolahDanSemua(...)}, dan seluruh badan method itu
 * dibungkus {@code try/catch} penelan-galat — sekali ada exception (atau sekadar tidak ada konteks
 * sekolah/yayasan aktif), daftar terbuka lintas sekolah/yayasan. Lihat {@link #getSekolah()}.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.KurikulumSekolah
 * @see ais.database.model.file.LampiranLain
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_nilai_siswa")
public class JenisNilaiSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan generator dan dipakai apa adanya sejak berkas
	 * dibuat; jangan diubah tanpa alasan kuat karena instance kelas ini ikut tersimpan di sesi ZK
	 * (mis. sebagai {@code value} sebuah {@code Comboitem}) dan dapat diserialisasi saat sesi
	 * dipindahkan/di-passivate.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer baris ({@code IDENTITY}); lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris; lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}, bukan oleh layar.</p>
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} maupun string kosong DIABAIKAN diam-diam (method
	 * langsung {@code return} tanpa menyentuh field). Konsekuensinya jejak audit yang sudah ada
	 * tidak pernah bisa dikosongkan lewat setter ini — perilaku sengaja, seragam di seluruh entity
	 * turunan {@link GeneralValueObject}.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit lama tidak pernah terhapus.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence TEPAT SEBELUM pernyataan
	 * {@code UPDATE} dikirim ke basis data.
	 *
	 * <p><b>Efek samping:</b> mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #getOleh()}/{@link #getOlehId()} dari pengguna sesi aktif dan menyegarkan
	 * {@link #getTanggal_dirubah()}. TIDAK ada {@code @PrePersist}, sehingga baris baru mengandalkan
	 * nilai awal field {@link #tanggal_dirubah} dan pengisian {@code oleh} dari jalur lain.</p>
	 *
	 * <p>Perlu disadari bahwa satu klik centang "Aktif" atau "Per Matapelajaran" pada grid daftar
	 * sudah memicu {@code Common.refreshSaveOrUpdate(...)}, jadi jalur ini — beserta satu revisi
	 * Envers baru — berjalan hanya karena pengguna mengubah sebuah kotak centang.</p>
	 *
	 * <p>Perhatikan bahwa deklarasi field {@link #tanggal_dirubah} sengaja berbagi baris dengan
	 * method ini persis seperti pada seluruh entity turunan lain (hasil penyuntingan massal); itu
	 * bukan kekeliruan sintaks.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server ({@code ais.ui.util.WaktuUtil}) saat
	 * instance dibuat, sehingga baris yang baru di-{@code INSERT} pun sudah punya stempel waktu
	 * meski {@link #onUpdate()} belum pernah berjalan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini menerima
	 * {@code null} apa adanya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan terakhir; terisi otomatis pada instance yang dibuat lewat
	 *         konstruktor, tetapi bisa {@code null} bila kolom di basis data kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Dua kehalusan:</b> (1) method ini membaca FIELD {@code nama} secara langsung, BUKAN
	 * {@link #getNama()}, sehingga hasilnya TIDAK di-{@code trim}; (2) pada instance yang belum
	 * disimpan {@code id} masih {@code null} sehingga keluarannya berupa {@code "null-..."}.</p>
	 *
	 * <p>Nilai ini tidak dipakai combo laporan (combo memakai properti {@code nama}/{@code kode}
	 * lewat {@code Common.insertComboDanSemua}), jadi dampaknya terbatas pada log dan debugging.</p>
	 *
	 * @return gabungan id dan nama, dipisahkan tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat profil; praktis tidak pernah terisi — lihat {@link #getKode()}. */
	private String kode;

	/** Nama profil yang ditampilkan di grid dan combo laporan; lihat {@link #getNama()}. */
	private String nama;

	/** Unit sekolah pemilik baris; lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

	/** Badan penyelenggara; nilainya derivatif dari {@code sekolah} — lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Keterangan bebas; juga dipakai sebagai baris deskripsi combo — lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Saklar tampil di combo laporan; lihat peringatan di {@link #getAktif()}. */
	private Boolean aktif;

	/** Saklar orientasi baris datasource laporan; lihat {@link #getBerdasarkanMk()}. */
	private Boolean berdasarkanMk;

	/** Kurikulum yang mengikat profil ini (opsional); lihat {@link #getKurikulumSekolah()}. */
	private KurikulumSekolah kurikulumSekolah;

	/**
	 * Konstruktor tanpa argumen — satu-satunya yang tersedia.
	 *
	 * <p>Diperlukan Hibernate untuk membentuk instance saat memuat baris, dan dipakai langsung oleh
	 * {@code JenisNilaiSiswaAction.onAdd(...)} untuk menyiapkan formulir "Tambah Jenis Nilai Siswa".
	 * Semua field kecuali {@link #tanggal_dirubah} dibiarkan {@code null}.</p>
	 */
	public JenisNilaiSiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Kolom dipetakan dengan {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@code IDENTITY}), sehingga id BERURUTAN dan dapat ditebak. Id ini juga dipakai sebagai
	 * {@code ref} pada {@link ais.database.model.file.LampiranLain} untuk mengaitkan berkas
	 * {@code .jrxml} dan gambar pendukung ke profil ini.</p>
	 *
	 * @return id baris, atau {@code null} untuk instance yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris.
	 *
	 * <p>Hanya untuk dipakai Hibernate maupun kode yang sengaja membentuk referensi ringan; jangan
	 * dipakai untuk "memindahkan" data ke id lain.</p>
	 *
	 * @param id kunci primer baru; boleh {@code null} untuk instance baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode singkat profil, dengan spasi tepi dibuang.
	 *
	 * <p><b>Kolom mati dalam praktik.</b> Tidak ada satu pun layar, impor spreadsheet, atau API yang
	 * memanggil {@link #setKode(String)}; formulir "Tambah/Ubah Jenis Nilai Siswa" bahkan tidak
	 * punya kotak isian untuk kode. Padahal {@code LaporanRekapTotalNilai} mendaftarkan combo-nya
	 * dengan properti tampilan {@code {"nama", "kode"}} — sehingga label item combo secara efektif
	 * hanya berisi nama.</p>
	 *
	 * <p>Berbeda dari {@link #getNama()} dan {@link #getKeterangan()}, getter ini TIDAK PERNAH
	 * mengembalikan {@code null}: nilai {@code null} dinormalkan menjadi string kosong.</p>
	 *
	 * @return kode yang sudah di-{@code trim}, atau {@code ""} bila kolom kosong
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode singkat profil.
	 *
	 * <p>Nilai disimpan apa adanya (tanpa {@code trim}); pemangkasan baru terjadi saat dibaca lewat
	 * {@link #getKode()}.</p>
	 *
	 * @param kode kode baru; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama profil, dengan spasi tepi dibuang bila ada isinya.
	 *
	 * <p>Inilah label yang muncul di kolom "Nama Jenis Nilai" pada grid master (lewat
	 * {@code RevisiHelper.createNewRevisi(...)}) dan menjadi teks item combo "Jenis Nilai" pada
	 * layar Laporan Rekap Total Nilai.</p>
	 *
	 * <p><b>Kontrak {@code null} berbeda dari {@link #getKode()}</b>: {@code null} dikembalikan apa
	 * adanya, tidak dinormalkan menjadi {@code ""}.</p>
	 *
	 * @return nama yang sudah di-{@code trim}, atau {@code null} bila kolom kosong
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama profil.
	 *
	 * <p>Dipanggil {@code JenisNilaiSiswaAction.onSave(...)} setelah validasi "Nama Jenis Sekolah
	 * harus diisi" (teks pesan itu sendiri sisa salin-tempel dari layar jenis sekolah). Nilai
	 * disimpan apa adanya; pemangkasan terjadi saat dibaca.</p>
	 *
	 * <p>Kolom dipetakan {@code nullable = false}, jadi menyetel {@code null} akan gagal saat
	 * {@code flush}, bukan saat setter dipanggil.</p>
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas profil ini.
	 *
	 * <p>Ditampilkan pada kolom "Keterangan" grid master dan dipakai sebagai baris deskripsi item
	 * combo di {@code LaporanRekapTotalNilai} (argumen ketiga
	 * {@code Common.insertComboDanSemua(...)}).</p>
	 *
	 * <p><b>Berbeda dari {@link #getNama()} dan {@link #getKode()}, getter ini mengembalikan nilai
	 * MENTAH</b> — tanpa {@code trim} dan tanpa normalisasi {@code null}. Kolom "Keterangan" pada
	 * grid karenanya dapat menerima {@code null} (ZK {@code Label} menampilkannya sebagai kosong).
	 * Getter ini juga TIDAK membalik kontrak apa pun: benar-benar hanya mengembalikan field.</p>
	 *
	 * @return keterangan apa adanya, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas profil ini.
	 *
	 * <p>Diisi dari {@code Textbox} tiga baris pada formulir tambah/ubah.</p>
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan saklar aktif profil, dengan {@code null} dianggap {@code true}.
	 *
	 * <p><b>PERINGATAN — sumber bug fungsional nyata.</b> Normalisasi {@code null → true} hanya
	 * berlaku DI DALAM JAVA. Di sisi basis data, {@code LaporanRekapTotalNilai} menyaring combo
	 * "Jenis Nilai" dengan {@code Restrictions.eq("aktif", true)}, yang diterjemahkan menjadi
	 * {@code aktif = true} — dan di SQL {@code NULL = true} bernilai {@code UNKNOWN}, sehingga baris
	 * ber-{@code aktif} {@code NULL} TIDAK IKUT TERAMBIL.</p>
	 *
	 * <p>Masalahnya, {@code JenisNilaiSiswaAction.onSave(...)} TIDAK PERNAH memanggil
	 * {@link #setAktif(Boolean)}; formulir tambah/ubah pun tidak punya kotak centang "Aktif".
	 * Dengan {@code dynamicInsert = true}, kolom {@code aktif} yang masih {@code null} tidak
	 * disertakan dalam {@code INSERT}, dan kolom yang dibangun otomatis oleh {@code hbm2ddl.auto=
	 * update} tidak memiliki {@code DEFAULT}. Hasil akhirnya: <b>profil yang baru dibuat lewat
	 * tombol "Tambah" tidak pernah muncul di combo laporan</b>, padahal grid master menampilkan
	 * kotak centang "Aktif" dalam keadaan TERCENTANG (karena getter ini menormalkan {@code null}).
	 * Ketidaksesuaian ini sangat menyesatkan bagi operator.</p>
	 *
	 * <p>Dua jalur yang benar-benar menuliskan nilai konkret: kotak centang "Aktif" pada grid
	 * daftar (melepas lalu mencentangnya kembali akan menulis {@code true} sungguhan) dan impor
	 * spreadsheet {@code Common.uploadData(...)} yang memang menyertakan kolom {@code aktif}.</p>
	 *
	 * <p>Pola yang sama pernah tercatat pada {@code JenisCatatanSiswa} (batch 45).</p>
	 *
	 * @return {@code true} bila profil dianggap aktif; {@code null} dilaporkan sebagai {@code true}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel saklar aktif profil.
	 *
	 * <p>Satu-satunya pemanggil di layar adalah listener {@code onCheck} kotak centang "Aktif" pada
	 * grid daftar, yang langsung menyusulnya dengan {@code Common.refreshSaveOrUpdate(...)} —
	 * artinya perubahan TERSIMPAN SEKETIKA tanpa tombol Simpan dan tanpa konfirmasi, sekaligus
	 * memicu {@link #onUpdate()} serta satu revisi Envers.</p>
	 *
	 * <p>Menyetel {@code null} secara efektif berarti "aktif" di sisi Java tetapi "tidak aktif" di
	 * sisi kueri laporan; lihat {@link #getAktif()}.</p>
	 *
	 * @param aktif nilai baru; boleh {@code null}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan unit sekolah pemilik profil ini, setelah proxy lazy diresolusi.
	 *
	 * <p><b>Efek samping:</b> hasil {@link ais.database.model.GeneralValueObject#check(Object)}
	 * DITULIS BALIK ke field {@code sekolah}. Pada instance terkelola, penulisan itu dapat
	 * terdeteksi mekanisme dirty-checking Hibernate, sehingga membaca pun berpotensi menghasilkan
	 * {@code UPDATE}. Jangan memanggilnya dalam pengulangan panas tanpa alasan.</p>
	 *
	 * <p><b>Peran dalam penyaringan:</b> nilai ini dipakai keras oleh {@code LaporanRekapTotalNilai}
	 * ({@code Restrictions.eq("sekolah", kelasSiswa.getSekolah())}), jadi di jalur laporan cakupan
	 * tenant AMAN dan mengikuti kelas yang dipilih.</p>
	 *
	 * <p><b>Namun di layar master TIDAK demikian.</b> {@code JenisNilaiSiswaAction.initCriteria(...)}
	 * tidak memasang batasan tenant apa pun sendiri: bila combo Yayasan/Sekolah tidak terpilih,
	 * kriterianya menjadi {@code Restrictions.sqlRestriction("1=1")}. Pengisian combo diserahkan ke
	 * {@code InitComboUtil.initYayasanDanSekolahDanSemua(...)} yang seluruh badannya dibungkus
	 * {@code try/catch} penelan-galat — sekali gagal, atau sekadar tidak ada konteks sekolah/yayasan
	 * aktif untuk pengguna, daftar terbuka LINTAS sekolah/yayasan. Karena tombol Ubah/Hapus dan
	 * kotak centang pada grid mengikuti baris yang tampil, pengguna dapat menyunting profil milik
	 * sekolah lain — termasuk MENGUNDUH dan MENGGANTI berkas template {@code .jrxml} milik sekolah
	 * lain (berkas JasperReports memuat ekspresi Java yang dieksekusi di sisi server). Isi entity
	 * ini sendiri bukan data pribadi, tetapi kanal penggantian template itu adalah penguat risiko
	 * yang serupa dengan temuan pada {@code JenisCatatanGuru} (batch 45).</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila kolom kosong / proxy gagal diresolusi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel unit sekolah pemilik profil ini.
	 *
	 * <p><b>Normalisasi diam-diam:</b> {@code null} maupun objek {@code Sekolah} yang {@code id}-nya
	 * masih {@code null} (belum tersimpan) sama-sama disimpan sebagai {@code null}. Tujuannya
	 * mencegah {@code CascadeType.PERSIST} ikut menyisipkan entity sekolah baru secara tak sengaja.
	 * Perhatikan bahwa {@link #setKurikulumSekolah(KurikulumSekolah)} TIDAK melakukan penjagaan
	 * serupa — asimetri yang disengaja atau tidak, tetapi nyata.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau entity tanpa id diperlakukan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan badan penyelenggara (yayasan) profil ini.
	 *
	 * <p><b>Getter DESTRUKTIF — baca dengan hati-hati.</b> Method ini tidak sekadar membaca kolom
	 * {@code yayasan_id}. Urutannya:</p>
	 * <ol>
	 * <li>memanggil {@link #getSekolah()} (yang sudah menulis balik field {@code sekolah});</li>
	 * <li>bila sekolah ada, MENIMPA field {@code yayasan} dengan {@code sekolah.getYayasan()} —
	 * nilai kolom {@code yayasan_id} yang tersimpan di basis data diabaikan sepenuhnya;</li>
	 * <li>meresolusi proxy hasilnya lewat {@code check(...)} dan menulis baliknya lagi.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi praktis:</b> (a) pada instance terkelola, sekadar MEMBACA yayasan dapat
	 * menghasilkan {@code UPDATE} yang menyelaraskan {@code yayasan_id} ke yayasan milik sekolah,
	 * lengkap dengan satu revisi Envers baru; (b) nilai yang di-{@code set} layar lewat
	 * {@link #setYayasan(Yayasan)} praktis TIDAK BERARTI selama {@code sekolah} terisi — inilah
	 * sebabnya combo "Yayasan" pada formulir dibuat {@code readonly} dan disetel otomatis mengikuti
	 * sekolah. Untuk baris tanpa sekolah, nilai kolom asli tetap dipakai.</p>
	 *
	 * @return yayasan turunan dari {@link #getSekolah()} bila ada, selain itu nilai kolom apa adanya
	 *         (boleh {@code null})
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
	 * Menyetel badan penyelenggara profil ini.
	 *
	 * <p>Sama seperti {@link #setSekolah(Sekolah)}, {@code null} maupun entity yang belum punya
	 * {@code id} dinormalkan menjadi {@code null} agar {@code CascadeType.PERSIST} tidak menyisipkan
	 * yayasan baru.</p>
	 *
	 * <p><b>Perhatikan:</b> nilai yang disetel di sini akan DITIMPA pada pembacaan berikutnya bila
	 * {@link #getSekolah()} tidak {@code null} — lihat {@link #getYayasan()}.</p>
	 *
	 * @param yayasan yayasan baru; {@code null} atau entity tanpa id diperlakukan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan saklar ORIENTASI baris datasource laporan, dengan {@code null} dianggap
	 * {@code false}.
	 *
	 * <p>Di grid master saklar ini berlabel <b>"Per Matapelajaran"</b>. Satu-satunya pembaca
	 * runtime-nya adalah {@code LaporanRekapTotalNilai.onCetak(...)}, dan pengaruhnya baru terasa
	 * bila sebuah profil BENAR-BENAR dipilih (item {@code "= Tanpa Jenis Penilaian ="} membuat
	 * cabang ini tidak pernah dievaluasi) DAN profil itu sudah punya berkas {@code .jrxml} —
	 * tanpa berkas, laporan berhenti lebih dulu dengan pesan peringatan.</p>
	 *
	 * <ul>
	 * <li>{@code true} → datasource dibangun BERTINGKAT MATAPELAJARAN: perulangan luar menyusuri
	 * {@code KurikulumPunyaMatapelajaran}, perulangan dalam menyusuri siswa, sehingga dihasilkan
	 * satu baris per pasangan (matapelajaran × siswa). Cocok untuk template yang mengelompokkan
	 * halaman per mata pelajaran.</li>
	 * <li>{@code false} (juga bila {@code null}) → datasource BERTINGKAT SISWA: satu baris per
	 * siswa, dengan seluruh mata pelajaran dijadikan kolom di dalam baris itu. Ini bentuk rekap
	 * klasik.</li>
	 * </ul>
	 *
	 * <p>Karena bentuk datasource-nya berbeda total, sebuah template {@code .jrxml} umumnya hanya
	 * cocok untuk SATU nilai saklar ini; mengubah saklar tanpa mengganti template biasanya
	 * menghasilkan laporan kosong atau kolom yang tidak terisi.</p>
	 *
	 * @return {@code true} bila baris laporan dipecah per mata pelajaran; {@code null} dilaporkan
	 *         sebagai {@code false}
	 */
	public Boolean getBerdasarkanMk() {
		return berdasarkanMk == null ? false : berdasarkanMk;
	}

	/**
	 * Menyetel saklar orientasi baris datasource laporan.
	 *
	 * <p>Seperti {@link #setAktif(Boolean)}, satu-satunya pemanggil di layar adalah listener
	 * {@code onCheck} kotak centang "Per Matapelajaran" pada grid daftar, yang langsung
	 * menyimpannya lewat {@code Common.refreshSaveOrUpdate(...)} — tersimpan seketika, tanpa tombol
	 * Simpan dan tanpa konfirmasi. Formulir tambah/ubah tidak menyediakan kendali untuk field ini,
	 * sehingga profil baru selalu lahir dengan nilai {@code null} (dibaca sebagai {@code false}).</p>
	 *
	 * @param berdasarkanMk nilai baru; boleh {@code null}
	 */
	public void setBerdasarkanMk(Boolean berdasarkanMk) {
		this.berdasarkanMk = berdasarkanMk;
	}

	/**
	 * Mengembalikan kurikulum yang mengikat profil ini, setelah proxy lazy diresolusi.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field
	 * {@code kurikulumSekolah}, sama seperti {@link #getSekolah()}.</p>
	 *
	 * <p><b>Semantik {@code null} penting:</b> pada {@code LaporanRekapTotalNilai}, combo "Jenis
	 * Nilai" disaring dengan
	 * {@code (kurikulumSekolah IS NULL OR kurikulumSekolah = kelas.kurikulumSekolah)}. Artinya
	 * {@code null} BUKAN "belum diisi" melainkan <b>"berlaku untuk semua kurikulum"</b>: profil
	 * tanpa kurikulum muncul untuk kelas mana pun, sedangkan profil dengan kurikulum tertentu hanya
	 * muncul untuk kelas yang memakai kurikulum itu. Mengisi field ini adalah tindakan MEMPERSEMPIT
	 * ketersediaan profil, bukan melengkapinya.</p>
	 *
	 * <p>Nilainya juga ditampilkan pada kolom "Kurikulum" di grid master. Pada formulir, daftar
	 * pilihannya dimuat ulang setiap kali combo Sekolah berubah, dibatasi
	 * {@code aktif = true AND sekolah = <sekolah terpilih>} — jadi profil tidak bisa diikat ke
	 * kurikulum milik sekolah lain lewat jalur formulir normal.</p>
	 *
	 * @return kurikulum pengikat, atau {@code null} yang berarti "berlaku untuk semua kurikulum"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kurikulum_sekolah_id")
	public KurikulumSekolah getKurikulumSekolah() {
		kurikulumSekolah = check(kurikulumSekolah);
		return kurikulumSekolah;
	}

	/**
	 * Menyetel kurikulum pengikat profil ini.
	 *
	 * <p>Dipanggil {@code JenisNilaiSiswaAction.onSave(...)} dari combo "Kurikulum"; bila tidak ada
	 * item terpilih, yang disetel adalah {@code null} — yang berarti "berlaku untuk semua
	 * kurikulum", lihat {@link #getKurikulumSekolah()}.</p>
	 *
	 * <p><b>Asimetri yang perlu disadari:</b> berbeda dari {@link #setSekolah(Sekolah)} dan
	 * {@link #setYayasan(Yayasan)}, setter ini TIDAK menolak entity yang {@code id}-nya masih
	 * {@code null}. Karena relasinya memakai {@code CascadeType.PERSIST}, meneruskan
	 * {@code KurikulumSekolah} yang belum tersimpan akan ikut menyisipkan baris kurikulum baru saat
	 * {@code flush}. Jalur layar yang ada selalu memberi entity hasil pemuatan combo, jadi hal ini
	 * belum pernah terpicu — tetapi pemanggil baru wajib berhati-hati.</p>
	 *
	 * @param kurikulumSekolah kurikulum pengikat; {@code null} berarti berlaku untuk semua kurikulum
	 */
	public void setKurikulumSekolah(KurikulumSekolah kurikulumSekolah) {
		this.kurikulumSekolah = kurikulumSekolah;
	}
}
