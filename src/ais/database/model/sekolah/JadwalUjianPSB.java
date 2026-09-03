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
import org.json.JSONObject;

import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;

/**
 * Entity <b>jadwal materi ujian Penerimaan Siswa Baru (PSB/PPDB)</b> — satu baris mewakili SATU
 * materi/sesi ujian masuk (label layar panitia: <i>"Materi Ujian"</i>) yang tergantung pada satu
 * paket {@link UjianPSB}, dengan rentang waktu {@link #getWaktuMulai()}–{@link #getWaktuSampai()}.
 * Dipetakan ke tabel {@code sekolah.jadwal_ujian_psb}, di-audit Envers ({@code @Audited}), dan
 * memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga hanya kolom yang benar-benar
 * berubah yang ikut dalam pernyataan SQL.
 *
 * <p><b>Peran dalam alur PSB (TERVERIFIKASI dari kode pemanggil, bukan dugaan dari nama).</b>
 * Baris entity ini dikelola panitia lewat layar {@code /pages/psb/jadwal_ujian_psb.zul}
 * ({@code ais.action.master.sekolah.JadwalUjianPSBAction}), yang pada instalasi normal dibuka
 * sebagai salah satu tab pada layar gelombang pendaftaran
 * ({@code GelombangPendaftaranPsbAction} menyisipkannya lewat
 * {@code MyInclude("/pages/psb/jadwal_ujian_psb.zul?gelombangPendaftaranPsb=<id>")}). Entity ini
 * BUKAN sekadar baris kalender: ia adalah salah satu "induk pembelajaran" (turunan
 * {@link VOPembelajaran}) yang benar-benar melahirkan baris
 * {@code ais.database.model.Pertemuan} lewat FK {@code pertemuan.jadwal_ujian_psb}. Rantai
 * lengkapnya:
 * <ol>
 * <li>Panitia membuat baris {@code JadwalUjianPSB} (materi ujian + waktu + paket
 * {@link UjianPSB}).</li>
 * <li>Membuka baris pada grid akan memuat tab detail
 * {@code ais.action.master.sekolah.helper.AktifitasJadwalUjianPSBHelper}; helper ini membuat
 * OTOMATIS satu {@code Pertemuan} berstatus {@code ConstantValues.UJIAN_ONLINE} bila jadwal ini
 * belum punya pertemuan sama sekali (lihat "Hal non-obvious" di bawah).</li>
 * <li>Panitia menyusun daftar pertemuan lewat dialog "Agenda Jadwal Ujian PSB"
 * ({@code ais.action.master.sekolah.helper.PenjadwalanUjianPSBHelper}), lalu menempelkan paket
 * soal ke pertemuan tersebut sebagai {@code ais.database.model.PertemuanPunyaUjian}.</li>
 * <li>Calon siswa mengerjakan ujian lewat {@code ais.action.master.sekolah.psb.TampilanUjianCalonSiswa}
 * (tombol "IKUT UJIAN" di formulir {@code CalonSiswaAction}, tombol "Ikut Ujian Sekarang" di
 * {@code TampilanPengumumanAkademisAction}) atau lewat portal PPDB
 * ({@code WEB-INF/baru/modul/ppdb/_ikut_ujian_online.jsp}). Ketiganya menemukan pertemuan yang
 * boleh dikerjakan dengan pola query yang SAMA: cari {@code RuangGelombangPendaftaranPsbPSB}
 * milik calon siswa → ambil {@code ruangPSB.ujianPSB} → cari {@code JadwalUjianPSB} dengan
 * {@code ujianPSB} itu DAN ({@code gelombangPendaftaranPsb} = gelombang calon siswa ATAU
 * {@code gelombangPendaftaranPsb IS NULL}) → ambil {@code PertemuanPunyaUjian} dari
 * pertemuan-pertemuan jadwal tersebut.</li>
 * </ol>
 * Karena pola query di titik (4) memakai {@code OR ... IS NULL}, membiarkan
 * {@link #getGelombangPendaftaranPsb() gelombang} kosong berarti materi ujian ini berlaku untuk
 * SEMUA gelombang yang memakai paket {@link UjianPSB} yang sama — inilah arti label "Semua" pada
 * kolom Gelombang di grid panitia.
 *
 * <p><b>Kelas kembar: {@code JadwalPertemuanPSB}.</b> {@code ais.database.model.sekolah.JadwalPertemuanPSB}
 * adalah hasil salin-tempel dari kelas ini (atau sebaliknya): sama-sama turunan
 * {@link VOPembelajaran}, susunan field dan urutan method-nya nyaris identik, dan
 * {@code serialVersionUID}-nya PERSIS SAMA ({@code 2463821577548439808L}). Nilai yang sama itu
 * bahkan dipakai pula oleh {@link UjianPSB} — jadi keluarga salin-tempel ini minimal beranggota
 * tiga kelas. Keduanya dipasang berdampingan sebagai dua tab pada layar gelombang pendaftaran.
 * Perbedaan yang nyata dan penting:
 * <ul>
 * <li>Kelas ini punya relasi wajib {@link #getUjianPSB()} ({@code nullable = false}) ke paket
 * ujian; kembarannya tidak mengenal {@link UjianPSB} sama sekali.</li>
 * <li>Kembarannya punya {@code kuota}, {@code bolehDipilihSendiriOlehCalonSiswa}, dan override
 * {@code getAktif()}; kelas ini TIDAK punya satu pun dari ketiganya. Konsekuensi praktis: materi
 * ujian tidak bisa dinonaktifkan lewat kolom {@code aktif} (kolomnya tidak dipetakan pada kelas
 * ini) — satu-satunya cara menyembunyikannya adalah menghapus barisnya atau menghapus pertemuan
 * di bawahnya.</li>
 * <li>Kembarannya dipilih sendiri oleh pendaftar dan disimpan di sisi calon siswa
 * ({@code CalonSiswa.jadwalPertemuanPSB}); kelas ini TIDAK pernah dipilih pendaftar — penugasan
 * ujian mengalir lewat alokasi ruang ({@code RuangGelombangPendaftaranPsbPSB} →
 * {@link RuangPSB} → {@link UjianPSB}).</li>
 * <li>Pada kembarannya mesin agenda {@link VOPembelajaran} berstatus LATEN (helper agenda hanya
 * di-instansiasi, tidak pernah dipanggil). Pada kelas ini mesin itu AKTIF dan dipakai
 * sungguhan.</li>
 * <li>{@link #getGelombangPendaftaranPsb()} di kelas ini <b>menulis balik</b> nilai turunan dari
 * {@link #getUjianPSB()}; getter kembarannya hanya me-resolve proxy. Lihat catatan pada method
 * tersebut — ini beda perilaku yang paling mudah terlewat saat menyalin pola antar kembar.</li>
 * </ul>
 *
 * <p><b>Warisan {@link VOPembelajaran}.</b> Rantai pewarisannya
 * {@code VOPembelajaran → VoKunci → ais.database.model.sop.DataSop →}
 * {@link ais.database.model.GeneralValueObject}. Dari {@link VOPembelajaran} kelas ini memperoleh
 * mesin agenda pertemuan bersama yang dipakai juga oleh {@code Perkuliahan},
 * {@code JadwalPelajaran}, {@code KelompokKkn}, dan lain-lain: {@code ambilPertemuan()},
 * {@code ambilPertemuanList()}, {@code belum()} (invalidasi cache), {@code reInitPertemuan()},
 * {@code infoSimple()}, integrasi absensi dan kelas daring. Karena itu kelas ini WAJIB
 * mengimplementasikan tiga anggota abstrak induk: {@link #getCourse()}/{@link #setCourse(String)},
 * {@link #getUrutkanotomatis()}/{@link #setUrutkanotomatis(Boolean)}, dan
 * {@link #ambilJumlahDetailperkuliahanLangsung()}.
 *
 * <p><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 * bug.</b> {@link ais.database.model.GeneralValueObject} — dan seluruh kelas perantara di rantai
 * di atas — adalah POJO abstrak biasa, BUKAN {@code @Entity} maupun {@code @MappedSuperclass}.
 * Hibernate karena itu tidak memetakan satu pun properti kelas induk, sehingga setiap entity
 * konkret HARUS mendeklarasikan ulang kolom identitas dan kolom auditnya sendiri. Menghapus
 * deklarasi ulang itu akan menghilangkan kolomnya dari pemetaan, bukan merapikan kode.
 *
 * <p><b>Pengelompokan anggota kelas ini.</b>
 * <ol>
 * <li><i>Identitas &amp; audit</i> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 * <li><i>Isi materi ujian</i> — {@link #getNama()} (materi ujian, wajib) dan
 * {@link #getKeterangan()} (catatan bebas, ikut dirender di kolom terakhir grid panitia).</li>
 * <li><i>Waktu</i> — {@link #getWaktuMulai()} dan {@link #getWaktuSampai()}, keduanya
 * {@code nullable = false} dan keduanya getter yang menulis balik.</li>
 * <li><i>Relasi</i> — {@link #getUjianPSB()} (wajib),
 * {@link #getGelombangPendaftaranPsb()} (opsional, nilainya diturunkan dari paket ujian), dan
 * {@link #getDikunci()}.</li>
 * <li><i>Kontrak {@link VOPembelajaran}</i> — {@link #getCourse()},
 * {@link #getUrutkanotomatis()}, {@link #ambilJumlahDetailperkuliahanLangsung()}.</li>
 * </ol>
 *
 * <p><b>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini.</b>
 * <ul>
 * <li><b>Getter yang menulis balik.</b> {@link #getWaktuMulai()}, {@link #getWaktuSampai()}, dan
 * {@link #getGelombangPendaftaranPsb()} bukan getter murni: ketiganya MENGISI/MENIMPA field lebih
 * dahulu lalu mengembalikannya. Karena pemetaan memakai <i>property access</i> (anotasi
 * {@code @Id} berada di getter), sekadar merender baris lama sudah cukup membuat nilai hasil
 * "coalesce"/turunan itu ikut tertulis pada dirty-checking berikutnya. Bandingkan dengan
 * {@link #getCourse()} dan {@link #getUrutkanotomatis()} yang mengembalikan nilai default TANPA
 * menulis balik.</li>
 * <li><b>Membuka baris di grid bisa MENULIS ke basis data.</b> {@code AktifitasJadwalUjianPSBHelper.initDetail}
 * membuat dan {@code session.save()} satu {@code Pertemuan} baru begitu tab detail dibuka untuk
 * jadwal yang belum punya pertemuan, asalkan pengguna yang login bukan mahasiswa/siswa/calon
 * siswa. Aksi yang secara UI terasa "hanya melihat" karena itu punya efek samping persisten dan
 * ikut tercatat Envers.</li>
 * <li><b>Tidak ada kolom tenant.</b> Entity ini tidak punya {@code sekolah} maupun
 * {@code yayasan}; tenant hanya bisa diturunkan lewat {@link #getGelombangPendaftaranPsb()} yang
 * boleh {@code null}. {@code JadwalUjianPSBAction.initCriteria()} juga tidak menyaring tenant sama
 * sekali, sehingga pada instalasi multi-sekolah daftar materi ujian bersifat global.</li>
 * <li><b>{@link #getDikunci() dikunci} adalah kolom mati untuk entity ini.</b> Kolom pengunci
 * bertipe {@link Tbmuser} diwarisi dari pola {@code Perkuliahan}, tetapi tidak ada satu pun kode
 * yang membaca atau mengisinya untuk {@code JadwalUjianPSB}. Kolomnya tetap ada di tabel dan tetap
 * ikut diaudit Envers.</li>
 * <li><b>Nilai ujian calon siswa dapat dibaca lewat jalur PPDB tanpa sesi.</b>
 * {@code WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp} — dijangkau lewat dispatcher
 * {@code /psb?hanya_tampil_jsp=true&p=psb&s=_ikut_ujian_online_service&action=fetch_exams&id=<id>}
 * — memakai parameter {@code id} calon siswa mentah tanpa pemeriksaan sesi/kepemilikan, lalu
 * mengembalikan jadwal ujian yang berasal dari entity ini beserta jumlah percobaan dan NILAI yang
 * sudah diperoleh calon siswa tersebut. Ini bukan temuan kategori baru: cacat struktural
 * dispatcher PPDB dan pola {@code request.getParameter("id")} tanpa gerbang sudah tercatat pada
 * audit berjalan (lihat pula catatan sejenis di
 * {@link RuangGelombangPendaftaranPsbPSB}).</li>
 * </ul>
 *
 * @see VOPembelajaran
 * @see ais.database.model.GeneralValueObject
 * @see UjianPSB
 * @see GelombangPendaftaranPsb
 * @see JadwalPertemuanPSB
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jadwal_ujian_psb")

public class JadwalUjianPSB extends VOPembelajaran {

	/**
	 * Nomor versi serialisasi. Nilainya PERSIS SAMA dengan milik {@link JadwalPertemuanPSB} dan
	 * {@link UjianPSB} ({@code 2463821577548439808L}) karena ketiga kelas lahir
	 * dari salin-tempel; kesamaan ini tidak berbahaya (serialisasi Java memakai nama kelas sebagai
	 * bagian identitas) tetapi menjadi penanda kuat bahwa perubahan pada satu kelas biasanya perlu
	 * ditinjau juga pada kembarannya.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data ({@code sekolah.jadwal_ujian_psb.id}), dihasilkan oleh sequence/identity DB. */
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
	 * DIABAIKAN diam-diam (method langsung {@code return} tanpa mengubah apa pun). Kolom audit ini
	 * karena itu hanya bisa maju, tidak bisa dikosongkan kembali lewat setter — disengaja agar
	 * penyimpanan tanpa konteks pengguna tidak menghapus jejak audit yang sudah ada.</p>
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
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah},
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang dibuat lewat
	 *         konstruktor karena field-nya sudah diinisialisasi ke waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<materi ujian>"}.
	 *
	 * <p>Membaca field {@link #nama} secara LANGSUNG (bukan lewat {@link #getNama()}), sehingga
	 * nilainya TIDAK di-{@code trim} — berbeda dari teks yang dirender grid panitia. Dipakai antara
	 * lain sebagai label item combobox dan pada log.</p>
	 *
	 * @return gabungan id dan materi ujian; salah satu bagiannya bisa berbunyi {@code "null"} bila
	 *         baris belum tersimpan atau materi ujian belum diisi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Materi/topik ujian; kolom {@code nama}, wajib diisi, dilabeli "Materi Ujian" di layar panitia. */
	private String nama;
	/** Catatan bebas untuk panitia; kolom {@code keterangan}, opsional. */
	private String keterangan;

	/** Paket ujian masuk pemilik materi ini; kolom FK {@code ujian_psb}, wajib. */
	private UjianPSB ujianPSB;
	/** Gelombang pendaftaran pemilik; kolom FK {@code gelombang_pendaftaran_psb}, opsional dan nilainya diturunkan dari {@link #ujianPSB}. */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;

	/** Awal rentang waktu materi ujian ini; kolom {@code waktumulai}, wajib. */
	private Date waktuMulai;
	/** Akhir rentang waktu materi ujian ini; kolom {@code waktusampai}, wajib. */
	private Date waktuSampai;

	/** Pengguna pengunci baris; kolom FK {@code dikunci}. Kolom mati untuk entity ini (tidak ada kode yang mengisinya). */
	private Tbmuser dikunci;

	/**
	 * Mengembalikan pengguna yang "mengunci" baris ini (kolom FK {@code dikunci}).
	 *
	 * <p><b>Efek samping:</b> memanggil {@code GeneralValueObject.check(...)} lalu MENIMPA field
	 * dengan hasilnya. {@code check()} me-resolve proxy lazy Hibernate lewat identity map/cache
	 * repo, jadi getter ini bisa memicu query dan mengganti referensi objek yang dipegang
	 * instance.</p>
	 * <p><b>Status pemakaian:</b> tidak ada satu pun kode yang membaca atau mengisi kolom ini untuk
	 * {@code JadwalUjianPSB} — mekanisme kunci diwarisi dari pola {@code Perkuliahan} tanpa pernah
	 * dipakai di alur PSB. Kolomnya tetap dipetakan dan tetap ikut diaudit Envers.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} (kondisi normal untuk entity ini)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menetapkan pengguna pengunci baris ini.
	 *
	 * @param dikunci pengguna pengunci; boleh {@code null}
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA dan dipakai langsung oleh
	 * {@code JadwalUjianPSBAction.onAdd()} untuk membuat baris kosong pada dialog "Tambah Jadwal
	 * Ujian".
	 *
	 * <p>Tidak mengisi apa pun secara eksplisit; satu-satunya inisialisasi terjadi pada field
	 * {@code tanggal_dirubah} (waktu server). Nilai default {@link #getWaktuMulai()}/
	 * {@link #getWaktuSampai()} baru muncul saat getternya dipanggil pertama kali.</p>
	 */
	public JadwalUjianPSB() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Anotasi {@code @Id} berada di getter, sehingga mode akses seluruh entity adalah
	 * <b>PROPERTY</b>: Hibernate membaca anotasi pada getter dan mengabaikan anotasi pada field
	 * maupun setter. Kolomnya {@code insertable = false} karena nilainya dihasilkan strategi
	 * {@code IDENTITY} oleh basis data.</p>
	 *
	 * @return id baris, atau {@code null} bila instance belum pernah disimpan
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
	 * @param id kunci utama; umumnya diisi Hibernate setelah {@code save()}
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan materi/topik ujian (kolom {@code nama}, wajib, maksimum 255 karakter).
	 *
	 * <p>Nilainya di-{@code trim} saat dibaca, tetapi TIDAK di-{@code trim} saat disimpan (lihat
	 * {@link #setNama(String)}), sehingga spasi di ujung tetap tersimpan di basis data dan hanya
	 * hilang di tampilan. Perbedaan itu penting bagi pencarian: filter "Materi Ujian" pada layar
	 * panitia memakai {@code ilike ... ANYWHERE} terhadap kolom mentah, bukan terhadap nilai hasil
	 * {@code trim} ini.</p>
	 * <p>Dipakai sebagai judul baris grid panitia, judul tab agenda
	 * ({@code "Agenda " + getNama()}), topik {@code Pertemuan} yang dibuat otomatis, dan label
	 * revisi pada {@code RevisiHelper}.</p>
	 *
	 * @return materi ujian tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan materi/topik ujian.
	 *
	 * <p>Menyimpan nilai APA ADANYA (tanpa {@code trim}). Layar panitia sudah menolak nilai kosong
	 * sebelum memanggil setter ini ("Materi ujian harus diisi"), tetapi validasi itu ada di
	 * Action, bukan di entity — pemanggil lain (importir, migrasi) wajib memvalidasi sendiri karena
	 * kolomnya {@code nullable = false}.</p>
	 *
	 * @param nama materi ujian
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas panitia (kolom {@code keterangan}, opsional, tipe teks panjang).
	 *
	 * <p>Dirender apa adanya pada kolom terakhir grid layar panitia. Berbeda dari
	 * {@link #getNama()}, nilainya tidak di-{@code trim} dan tidak pernah dinormalisasi.</p>
	 *
	 * @return catatan panitia, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas panitia.
	 *
	 * @param keterangan catatan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan paket {@link UjianPSB} pemilik materi ujian ini (kolom FK {@code ujian_psb},
	 * {@code nullable = false}).
	 *
	 * <p>Relasi ini adalah poros seluruh alur ujian PSB: alokasi ruang calon siswa
	 * ({@code RuangGelombangPendaftaranPsbPSB} → {@link RuangPSB}) menunjuk sebuah
	 * {@link UjianPSB}, lalu semua {@code JadwalUjianPSB} dengan {@link UjianPSB} itulah yang
	 * pertemuannya boleh dikerjakan calon siswa bersangkutan.</p>
	 * <p><b>Catatan pemetaan:</b> memakai {@code @Fetch(FetchMode.SELECT)} tanpa
	 * {@code fetch = LAZY} — berbeda dari {@link #getDikunci()} dan
	 * {@link #getGelombangPendaftaranPsb()} yang keduanya lazy. Getter ini juga TIDAK memanggil
	 * {@code check(...)}, jadi ia mengembalikan referensi apa adanya tanpa resolusi proxy
	 * tambahan.</p>
	 *
	 * @return paket ujian pemilik; secara skema tidak boleh {@code null}, namun instance yang belum
	 *         diisi (mis. baris baru pada dialog "Tambah") tetap mengembalikan {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "ujian_psb", nullable = false)
	public UjianPSB getUjianPSB() {
		return ujianPSB;
	}

	/**
	 * Menetapkan paket ujian pemilik materi ini.
	 *
	 * <p><b>Efek berantai yang mudah terlewat:</b> mengubah nilai ini otomatis mengubah pula hasil
	 * {@link #getGelombangPendaftaranPsb()} pada pembacaan berikutnya, karena getter tersebut
	 * menurunkan gelombang dari paket ujian bila paketnya tidak {@code null}. Menyetel paket ujian
	 * karena itu dapat memindahkan materi ujian ini ke gelombang lain secara implisit.</p>
	 *
	 * @param ujianPSB paket ujian; wajib terisi sebelum baris disimpan
	 */
	public void setUjianPSB(UjianPSB ujianPSB) {
		this.ujianPSB = ujianPSB;
	}

	/**
	 * Mengembalikan awal rentang waktu materi ujian ini (kolom {@code waktuMulai},
	 * {@code nullable = false}).
	 *
	 * <p><b>Efek samping / perilaku non-obvious:</b> getter ini BUKAN getter murni. Bila field
	 * masih {@code null} ia MENGISINYA dengan waktu server ({@code ais.ui.util.WaktuUtil.getDate()})
	 * lalu mengembalikan nilai itu. Karena mode akses entity adalah PROPERTY, sekadar merender
	 * baris lama yang kolomnya {@code NULL} sudah cukup membuat waktu server tertulis ke basis data
	 * pada dirty-checking berikutnya. Perilaku ini juga yang membuat dialog "Tambah Jadwal Ujian"
	 * langsung menampilkan tanggal hari ini pada kedua datebox-nya.</p>
	 * <p>Nilai ini dipakai sebagai kunci pengurutan daftar panitia ({@code addOrder(asc("waktuMulai"))}),
	 * sebagai {@code tanggal} dan {@code waktuMulai} {@code Pertemuan} yang dibuat otomatis, serta
	 * ikut membentuk judul dialog agenda lewat {@code infoSimple()}.</p>
	 *
	 * @return awal rentang waktu; tidak pernah {@code null} setelah getter ini dipanggil
	 */
	@Column(nullable = false)
	public Date getWaktuMulai() {
		if (waktuMulai == null) {
			waktuMulai = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuMulai;
	}

	/**
	 * Menetapkan awal rentang waktu materi ujian ini.
	 *
	 * <p>Tidak ada validasi bahwa nilainya lebih awal dari {@link #getWaktuSampai()}; layar panitia
	 * pun hanya memeriksa bahwa kedua datebox terisi, tidak memeriksa urutannya.</p>
	 *
	 * @param waktuMulai awal rentang waktu; menyetel {@code null} akan "diperbaiki" menjadi waktu
	 *                   server pada pembacaan berikutnya (lihat {@link #getWaktuMulai()})
	 */
	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengembalikan akhir rentang waktu materi ujian ini (kolom {@code waktuSampai},
	 * {@code nullable = false}).
	 *
	 * <p><b>Efek samping / perilaku non-obvious:</b> identik dengan {@link #getWaktuMulai()} —
	 * bila field masih {@code null} getter ini MENGISINYA dengan waktu server lalu
	 * mengembalikannya, sehingga nilai hasil "coalesce" itu bisa ikut tertulis ke basis data hanya
	 * karena baris dibaca.</p>
	 * <p>Dipakai sebagai {@code waktuSelesai} {@code Pertemuan} yang dibuat otomatis dan ikut
	 * membentuk judul dialog agenda.</p>
	 *
	 * @return akhir rentang waktu; tidak pernah {@code null} setelah getter ini dipanggil
	 */
	@Column(nullable = false)
	public Date getWaktuSampai() {
		if (waktuSampai == null) {
			waktuSampai = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuSampai;
	}

	/**
	 * Menetapkan akhir rentang waktu materi ujian ini.
	 *
	 * @param waktuSampai akhir rentang waktu; menyetel {@code null} akan "diperbaiki" menjadi waktu
	 *                    server pada pembacaan berikutnya (lihat {@link #getWaktuSampai()})
	 */
	public void setWaktuSampai(Date waktuSampai) {
		this.waktuSampai = waktuSampai;
	}

	/**
	 * Mengembalikan gelombang pendaftaran PSB pemilik materi ujian ini (kolom FK
	 * {@code gelombang_pendaftaran_psb}, {@code nullable = true}).
	 *
	 * <p><b>Efek samping / perilaku non-obvious — beda perilaku terbesar dari kelas kembar
	 * {@link JadwalPertemuanPSB}.</b> Getter ini bukan getter murni dan bukan sekadar resolver
	 * proxy:</p>
	 * <ul>
	 * <li>Bila {@link #getUjianPSB() ujianPSB} TIDAK {@code null}, field
	 * {@code gelombangPendaftaranPsb} DITIMPA dengan {@code ujianPSB.getGelombangPendaftaranPsb()}.
	 * Nilai yang dipilih panitia sendiri pada dialog "Tambah/Ubah Jadwal Ujian" karena itu tidak
	 * bertahan begitu paket ujiannya punya gelombang sendiri — gelombang paket ujian selalu
	 * menang.</li>
	 * <li>Bila {@code ujianPSB} {@code null}, field hanya di-resolve lewat
	 * {@code GeneralValueObject.check(...)} (lazy proxy/identity map).</li>
	 * </ul>
	 * <p>Karena mode akses entity adalah PROPERTY, nilai hasil timpa itu ikut tertulis ke kolom
	 * pada dirty-checking berikutnya, sehingga kolom di basis data lambat laun konvergen ke
	 * gelombang milik paket ujian. Sebelum konvergensi terjadi, kolom dan getter bisa berbeda nilai
	 * — dan ini berarti sesuatu, karena {@code JadwalUjianPSBAction.initCriteria()} serta seluruh
	 * jalur calon siswa menyaring berdasarkan KOLOM ({@code Restrictions.eq("gelombangPendaftaranPsb", ...)}),
	 * bukan berdasarkan nilai getter. Sebuah baris karena itu bisa tampil "milik gelombang A" di
	 * layar namun tetap tersaring sebagai gelombang B (atau tidak tersaring sama sekali bila
	 * kolomnya masih {@code NULL}).</p>
	 * <p>Kolom {@code NULL} bermakna "berlaku untuk semua gelombang": grid panitia merendernya
	 * sebagai teks "Semua", dan seluruh query calon siswa memakai
	 * {@code OR gelombangPendaftaranPsb IS NULL}. Karena entity ini tidak punya kolom
	 * {@code sekolah}/{@code yayasan}, relasi inilah satu-satunya jalur penentu tenant — dan jalur
	 * itu putus persis pada baris "Semua".</p>
	 *
	 * @return gelombang pendaftaran pemilik (diturunkan dari paket ujian bila ada), atau
	 *         {@code null} yang berarti "berlaku untuk semua gelombang"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_psb", nullable = true)
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		if (ujianPSB != null) {
			gelombangPendaftaranPsb = ujianPSB.getGelombangPendaftaranPsb();
		} else {
			gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		}
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menetapkan gelombang pendaftaran PSB pemilik materi ujian ini.
	 *
	 * <p><b>Perlu diketahui:</b> nilai yang disetel di sini bersifat sementara selama
	 * {@link #getUjianPSB()} tidak {@code null} — pembacaan berikutnya lewat
	 * {@link #getGelombangPendaftaranPsb()} akan menimpanya dengan gelombang milik paket ujian.
	 * Nilai yang disetel hanya bertahan pada baris yang paket ujiannya belum terisi.</p>
	 *
	 * @param gelombangPendaftaranPsb gelombang pendaftaran; {@code null} berarti "berlaku untuk
	 *                                semua gelombang"
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Implementasi kontrak abstrak {@link VOPembelajaran}: jumlah "detail perkuliahan langsung"
	 * yang dianggap dimiliki induk pembelajaran ini.
	 *
	 * <p>Selalu mengembalikan {@code 1} (nilai tetap, bukan hasil query) — sama seperti pada kelas
	 * kembar {@link JadwalPertemuanPSB}. Konsep "detail perkuliahan" (kelas paralel pada
	 * {@code Perkuliahan}) tidak relevan untuk jadwal ujian PSB, sehingga nilai 1 hanya berfungsi
	 * sebagai pembagi/penyebut netral pada perhitungan statistik pertemuan milik kelas induk.</p>
	 *
	 * @return selalu {@code 1}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	/** Konfigurasi kelas daring dalam bentuk teks JSON; kolom {@code course} bertipe {@code text}. */
	private String course;
	/** Mode pengurutan agenda pertemuan; kolom {@code urutkanotomatis}. */
	private Boolean urutkanotomatis;

	/**
	 * Implementasi kontrak abstrak {@link VOPembelajaran}: konfigurasi kelas daring milik jadwal
	 * ujian ini, disimpan sebagai teks JSON pada kolom {@code course} ({@code columnDefinition = "text"}).
	 *
	 * <p>Isinya dikelola {@code ais.common.classroom.ClassRoomUtil} (tombol kelas daring pada
	 * toolbar agenda) — antara lain identitas ruang/meeting pada penyedia kelas daring yang
	 * dikonfigurasi instalasi.</p>
	 * <p><b>Perilaku non-obvious:</b> bila kolomnya {@code null} atau berisi string kosong, getter
	 * mengembalikan objek JSON kosong ({@code "{}"}) sehingga pemanggil selalu memperoleh JSON yang
	 * dapat di-parse. Berbeda dari {@link #getWaktuMulai()}/{@link #getGelombangPendaftaranPsb()},
	 * nilai default ini TIDAK ditulis balik ke field — kolom di basis data tetap {@code NULL}.</p>
	 *
	 * @return teks JSON konfigurasi kelas daring; tidak pernah {@code null}, minimal {@code "{}"}
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Menetapkan konfigurasi kelas daring (teks JSON) untuk jadwal ujian ini.
	 *
	 * <p>Menyimpan nilai apa adanya — tidak ada validasi bahwa isinya JSON yang sah. Nilai tidak
	 * sah baru akan menimbulkan kesalahan di pemakainya ({@code ClassRoomUtil}), bukan di sini.</p>
	 *
	 * @param course teks JSON konfigurasi kelas daring; boleh {@code null}/kosong (akan dibaca
	 *               sebagai {@code "{}"})
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Implementasi kontrak abstrak {@link VOPembelajaran}: menandai apakah agenda pertemuan milik
	 * jadwal ujian ini diurutkan otomatis (kolom {@code urutkanotomatis}).
	 *
	 * <p><b>Perilaku non-obvious:</b> nilai {@code null} dibaca sebagai {@code true} — pengurutan
	 * otomatis adalah perilaku bawaan untuk baris yang belum pernah menyetel kolom ini. Seperti
	 * {@link #getCourse()} dan berbeda dari {@link #getWaktuMulai()}, nilai default itu TIDAK
	 * ditulis balik ke field, sehingga kolomnya tetap {@code NULL} di basis data.</p>
	 *
	 * @return {@code true} bila agenda diurutkan otomatis (termasuk saat kolomnya {@code NULL});
	 *         {@code false} hanya bila kolomnya benar-benar berisi {@code false}
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Menetapkan mode pengurutan otomatis agenda pertemuan.
	 *
	 * @param urutkanotomatis {@code true}/{@code null} berarti diurutkan otomatis, {@code false}
	 *                        berarti urutan manual
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}
}
