package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;

/**
 * Master <b>kategori/seksi field kustom ("form tambahan") untuk modul Pengaduan</b>.
 *
 * <p>Satu baris entity ini mewakili satu <i>judul seksi</i> pada formulir pengaduan — misalnya
 * "Form Tambahan" (nilai bawaan), "Data Kronologi", "Bukti Pendukung". Entity ini <b>tidak</b>
 * menyimpan definisi field-nya sendiri; ia hanya menjadi wadah pengelompokan. Definisi field
 * sesungguhnya ada di {@code ParameterTambahan} (label, kode, tipe data), dan pemetaan
 * field&nbsp;&rarr;&nbsp;kategori dilakukan oleh entity penghubung
 * {@code ParameterTambahanPengaduan}.</p>
 *
 * <h3>Rantai konfigurasi (4 lapis, terverifikasi dari kode)</h3>
 * <ol>
 *   <li>{@code ParameterTambahan} — definisi field mentah (label, kode, tipe inputan, {@code aktif}).</li>
 *   <li>{@code ParameterTambahanPengaduan} — mengikat satu {@code ParameterTambahan} ke satu
 *       {@code KelompokParameterTambahanPengaduan} (kelas ini).</li>
 *   <li><b>Kelas ini</b> — kategori/seksi, punya {@link #getAktif() aktif} dan
 *       {@link #getNomorUrut() nomorUrut} sendiri.</li>
 *   <li>{@link JenisPengaduan} — <b>lapis keempat</b>: kategori harus <i>dicentang</i> pada suatu
 *       jenis pengaduan (relasi {@code @ManyToMany} lewat tabel {@code jenis_pengaduan_has_parameter},
 *       lihat {@link JenisPengaduan#getKelompokParameterTambahanPengaduans()}) sebelum seksinya
 *       benar-benar muncul di formulir. Kategori yang aktif tetapi tidak dicentang di jenis manapun
 *       tidak pernah tampil.</li>
 * </ol>
 *
 * <p>Nilai isian pengguna <b>tidak</b> disimpan per-baris relasional, melainkan dipadatkan menjadi
 * satu blob teks multi-baris di {@code Pengaduan.parameterTambahanInds} dengan format
 * {@code <idKelompok>-&gt;<idParameter><=><nilai>} per baris. Karena itu id baris entity ini ikut
 * menjadi bagian <i>kunci</i> data historis: mengganti/menghapus kategori membuat isian lama
 * yatim dan tak lagi terbaca.</p>
 *
 * <h3>Keluarga sejenis</h3>
 * <p>Anggota keluarga {@code KelompokParameterTambahan*} — bandingkan dengan
 * {@link ais.database.model.KelompokParameterTambahanAlumni} yang menjadi rujukan struktur
 * keluarga ini. Perbedaan yang terverifikasi terhadap varian Alumni:</p>
 * <ul>
 *   <li>{@link #compareTo(GeneralValueObject)} di sini <b>versi pendek</b> — hanya membandingkan
 *       {@code nomorUrut}, tanpa rantai fallback {@code nim}/{@code nama}/{@code keterangan}
 *       milik {@link GeneralValueObject#compareTo(GeneralValueObject)}. Lihat catatan pada method
 *       itu untuk konsekuensinya.</li>
 *   <li>Hanya ada <b>satu</b> mekanisme auto-seed di sini ({@link #checkCreateDefault()}), sedangkan
 *       varian Alumni punya dua jalur seed yang saling tidak sadar. Layar master kategori
 *       ({@code KelompokParameterTambahanPengaduanAction}) sengaja <i>tidak</i> memanggil seed,
 *       sehingga anomali "dua kategori bawaan berbeda" milik Alumni tidak terjadi di modul ini.</li>
 *   <li>Entity ini tidak memiliki flag tampil-per-form tambahan (seperti
 *       {@code tampilDiFormPendaftaran} pada varian Calon Mahasiswa); penyaringan tampil
 *       sepenuhnya bergantung pada {@link #getAktif()} + centang di {@link JenisPengaduan}.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Jejak audit manual</b> — {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b> — {@link #getId()}, {@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Atribut kategori</b> — {@link #getNama()}/{@link #setNama(String)},
 *       {@link #getKeterangan()}/{@link #setKeterangan(String)},
 *       {@link #getAktif()}/{@link #setAktif(Boolean)},
 *       {@link #getNomorUrut()}/{@link #setNomorUrut(Integer)}.</li>
 *   <li><b>Penanda data bawaan</b> — {@link #getDefaultData()}/{@link #setDefaultData(Boolean)} dan
 *       pabrik statis {@link #checkCreateDefault()}.</li>
 *   <li><b>Pengurutan</b> — {@link #compareTo(GeneralValueObject)}.</li>
 * </ul>
 *
 * <h3>Catatan teknis pemetaan</h3>
 * <p>Induk {@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti
 * apa pun miliknya. Deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi keliru</b>, melainkan keharusan teknis
 * agar kolom-kolom itu ikut terpetakan. Konsekuensinya, field induk yang tidak dideklarasikan
 * ulang (mis. {@code nim}) selalu bernilai {@code null} pada instance kelas ini.</p>
 *
 * <p>Akses properti memakai <i>property access</i> (anotasi {@code @Id} berada pada getter), dan
 * kelas ditandai {@code dynamicUpdate = true} + {@link Audited}. Artinya getter yang menulis ke
 * field ({@link #getDefaultData()}, {@link #getAktif()}, {@link #getNomorUrut()}) berpotensi
 * memicu {@code UPDATE} dan revisi Envers saat baris kebetulan dibaca dalam sesi aktif — lihat
 * catatan pada masing-masing getter.</p>
 *
 * @see ais.database.model.KelompokParameterTambahanAlumni
 * @see ais.database.model.GeneralValueObject
 * @see JenisPengaduan
 * @see Pengaduan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_pengaduan")
public class KelompokParameterTambahanPengaduan extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilai ini kebetulan sama dengan milik beberapa entity lain di paket ini
	 * (mis. {@link Pengaduan}) — artefak salin-tempel template hbm2java, bukan penanda kompatibilitas
	 * lintas kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama, kolom {@code id} (identity/serial). Dibaca lewat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi interceptor audit, lihat {@link #setOleh(String)}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi interceptor audit, lihat {@link #setOlehId(String)}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah disentuh interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>penjagaan "tolak nilai kosong"</b>: bila
	 * {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung {@code return}
	 * tanpa mengubah apa pun sehingga nilai lama tetap dipertahankan.
	 *
	 * <p>Perilaku ini disengaja agar jejak audit tidak terhapus oleh proses yang menyalin properti
	 * secara massal (mis. {@code BeanUtils.copyProperties}) dari objek yang kolom audit-nya kosong.
	 * Efek sampingnya: kolom ini <b>tidak pernah bisa dikosongkan kembali</b> lewat setter.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan secara diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan diam-diam sehingga nilai lama bertahan.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA: dipanggil Hibernate tepat sebelum {@code UPDATE} dikirim ke
	 * basis data, meneruskan instance ini ke {@code AuditTimestampInterceptor.ubah(...)} yang
	 * mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif.
	 *
	 * <p>Tidak boleh dipanggil manual. Perhatikan bahwa kait ini hanya berjalan pada
	 * <i>update</i>; pengisian awal saat {@code INSERT} mengandalkan nilai awal field
	 * {@code tanggal_dirubah} yang di-<i>inline</i> pada deklarasi di baris yang sama.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; nilai {@code null} diterima apa adanya
	 * (berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)} yang menolak nilai kosong).
	 *
	 * <p>Pemanggil normalnya hanya {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * <p>Nilai awalnya adalah waktu <b>pembuatan objek di JVM</b> ({@code WaktuUtil.getDate()} pada
	 * deklarasi field), bukan waktu simpan. Untuk baris yang dibuat lalu tidak segera disimpan,
	 * selisih keduanya bisa terlihat.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibuat di JVM
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai antara lain oleh komponen ZK yang menampilkan objek langsung dan oleh keluaran
	 * diagnostik. Untuk baris yang belum tersimpan hasilnya berawalan {@code "null-"}. Perhatikan
	 * bahwa {@code nama} dibaca dari <b>field</b>, bukan lewat {@link #getNama()}, sehingga tidak
	 * mengalami {@code trim()}.</p>
	 *
	 * @return gabungan id dan nama kategori
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kategori/seksi yang tampil sebagai judul kelompok di formulir pengaduan; wajib diisi dan unik. */
	private String nama;

	/** Keterangan bebas kategori; ditampilkan sebagai kolom terpisah di layar daftar master. */
	private String keterangan;

	/** Penanda "kategori bawaan sistem"; baris ber-{@code true} tidak boleh dihapus dari layar master. */
	private Boolean defaultData;

	/** Saklar tampil kategori; disaring pada semua query pembangun formulir dan laporan. */
	private Boolean aktif;

	/** Nomor urut tampil seksi pada formulir; juga satu-satunya kunci {@link #compareTo(GeneralValueObject)}. */
	private Integer nomorUrut;

	/**
	 * Memastikan minimal ada <b>satu</b> kategori bawaan di basis data, membuatnya bila belum ada
	 * ("auto-seed").
	 *
	 * <p>Alur: membuka session native lewat
	 * {@code HibernateUtil.currentNativeSession()}, mencari satu baris dengan
	 * {@code defaultData = true}. Bila tidak ditemukan, dibuat baris baru dengan
	 * {@code defaultData = true}, {@code nama = "Form Tambahan"}, dan
	 * {@code keterangan = "Form Tambahan"}, lalu disimpan dalam transaksi tersendiri
	 * ({@code begin()}/{@code save()}/{@code commit()}). Terakhir session ditutup lewat
	 * {@code HibernateUtil.closeSession()} — <b>termasuk pada jalur "sudah ada"</b>.</p>
	 *
	 * <p><b>Pemanggil nyata (terverifikasi, hanya satu):</b>
	 * {@code ais.action.master.ParameterTambahanPengaduanAction#doAfterCompose(Component)} — layar
	 * master "Parameter Tambahan Pengaduan" memanggilnya di awal setiap kali layar dibuka, sebelum
	 * mengisi combo pemilih kategori. Tujuannya agar combo itu tidak pernah kosong sehingga admin
	 * selalu punya minimal satu wadah untuk menempatkan field baru. Layar master kategori sendiri
	 * ({@code KelompokParameterTambahanPengaduanAction}) <b>tidak</b> memanggil method ini.</p>
	 *
	 * <p><b>Catatan efek samping:</b></p>
	 * <ul>
	 *   <li>Menutup session Hibernate milik request yang sedang berjalan. Kode setelahnya yang
	 *       masih memegang objek lazy dari session lama akan mendapat session baru.</li>
	 *   <li>Baris hasil seed lahir dengan {@code aktif} dan {@code nomorUrut} <b>tidak diisi</b>
	 *       (tetap {@code null} di kolom); nilai efektif {@code true}/{@code 1} baru muncul lewat
	 *       getter yang menulis-balik — lihat {@link #getAktif()} dan {@link #getNomorUrut()}.</li>
	 *   <li>Nama variabel lokal di dalam method ini masih menyebut
	 *       {@code kelompokParameterTambahanCatatanAdministrasi} — sisa salin-tempel dari varian
	 *       Catatan Administrasi; tidak berpengaruh pada perilaku.</li>
	 *   <li>Kategori bawaan hasil seed <b>belum tercentang</b> pada {@link JenisPengaduan} manapun,
	 *       sehingga seksinya belum tampil di formulir sampai admin mencentangnya.</li>
	 * </ul>
	 *
	 * @return kategori bawaan yang ditemukan, atau kategori baru yang barusan dibuat; tidak pernah
	 *         {@code null} kecuali penyimpanan gagal dan melempar exception
	 */
	public static KelompokParameterTambahanPengaduan checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanPengaduan kelompokParameterTambahanCatatanAdministrasi = (KelompokParameterTambahanPengaduan) session
				.createCriteria(KelompokParameterTambahanPengaduan.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanAdministrasi == null) {
			kelompokParameterTambahanCatatanAdministrasi = new KelompokParameterTambahanPengaduan();
			kelompokParameterTambahanCatatanAdministrasi.setDefaultData(true);
			kelompokParameterTambahanCatatanAdministrasi.setNama("Form Tambahan");
			kelompokParameterTambahanCatatanAdministrasi.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCatatanAdministrasi);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCatatanAdministrasi;
	}

	/**
	 * Konstruktor tanpa argumen. Diperlukan Hibernate untuk instansiasi lewat refleksi, dan dipakai
	 * layar master saat menekan tombol "Tambah"
	 * ({@code KelompokParameterTambahanPengaduanAction#onAdd(Event)}) serta oleh
	 * {@link #checkCreateDefault()}.
	 */
	public KelompokParameterTambahanPengaduan() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Selain sebagai identitas basis data, nilai ini ikut menjadi bagian kunci teks
	 * {@code "<idKelompok>-><idParameter>"} yang dipakai untuk menyimpan dan membaca isian pengguna
	 * pada blob {@code Pengaduan.parameterTambahanInds} serta untuk menautkan lampiran lewat
	 * {@code LampiranLain}. Karena itu id kategori bersifat <b>bagian dari data historis</b>, bukan
	 * sekadar detail penyimpanan.</p>
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
	 * Menyetel kunci utama. Normalnya hanya dipanggil Hibernate setelah {@code INSERT}; pemanggilan
	 * manual berisiko memutus tautan ke isian historis (lihat {@link #getId()}).
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kategori dengan {@code trim()} otomatis.
	 *
	 * <p>Nilai ini dipakai sebagai judul seksi pada formulir pengaduan
	 * ({@code ParameterTambahanPengaduanListener}), sebagai kolom {@code kelompok} pada ekspor
	 * laporan ({@code LaporanPengaduan}), dan sebagai label centang pada layar Jenis Pengaduan.</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori. Tanpa validasi dan tanpa {@code trim()} — pembersihan spasi baru
	 * terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * <p>Keunikan nama tidak dijamin basis data (tidak ada {@code unique = true}); pemeriksaannya
	 * dilakukan di lapisan UI oleh
	 * {@code KelompokParameterTambahanPengaduanAction#checkNamaKelompokParameterTambahanPengaduan()}
	 * sebelum menyimpan, sehingga duplikat masih mungkin masuk lewat jalur lain.</p>
	 *
	 * @param nama nama kategori baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan kategori <b>apa adanya</b>.
	 *
	 * <p><b>Peringatan — membalik kontrak kelas induk.</b>
	 * {@link GeneralValueObject#getKeterangan()} menjanjikan hasil yang tidak pernah {@code null}
	 * (mengembalikan {@code ""} untuk nilai kosong), sedangkan override ini mengembalikan
	 * {@code null} apa adanya. Pemanggil yang menganggap kontrak induk berlaku bisa terkena
	 * {@code NullPointerException}.</p>
	 *
	 * <p>Dalam praktik, override ini <b>tidak</b> membahayakan pengurutan karena
	 * {@link #compareTo(GeneralValueObject)} di kelas ini sudah dipangkas sehingga cabang
	 * {@code keterangan} milik induk tidak pernah tercapai. Risiko nyatanya ada pada UI: renderer
	 * layar master membungkus nilai ini langsung dalam {@code new Label(...)}, yang menampilkan
	 * teks kosong untuk {@code null} (aman), sementara pemanggil lain yang memanggil method
	 * {@code String} atasnya tidak aman.</p>
	 *
	 * @return keterangan kategori, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan kategori. Tanpa validasi; nilai {@code null} diterima.
	 *
	 * <p>Diisi dari kotak teks "Keterangan" pada dialog Tambah/Ubah layar master dan oleh
	 * {@link #checkCreateDefault()} untuk baris bawaan.</p>
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "kategori bawaan sistem", dengan <b>normalisasi menulis-balik</b>:
	 * bila field masih {@code null}, field diisi {@code false} lebih dulu lalu dikembalikan.
	 *
	 * <p><b>Efek samping:</b> karena kelas memakai <i>property access</i> dan
	 * {@code dynamicUpdate = true} + {@link Audited}, penulisan ke field pada objek yang sedang
	 * terikat session dapat terdeteksi sebagai perubahan kotor dan memicu {@code UPDATE} beserta
	 * revisi Envers "palsu" saat flush — cukup dengan <i>membaca</i> properti ini. Untuk baris yang
	 * dibuat lewat layar master hal ini praktis tidak terjadi karena
	 * {@code KelompokParameterTambahanPengaduanAction} selalu menyetel nilainya secara implisit
	 * lewat renderer yang membaca getter ini.</p>
	 *
	 * <p><b>Konsumen:</b> renderer layar master memakai nilai ini untuk menyembunyikan tombol Hapus
	 * pada baris bawaan ({@code setVisible(delete && !getDefaultData())}), dan
	 * {@link #checkCreateDefault()} memakainya sebagai kriteria pencarian.</p>
	 *
	 * @return {@code true} bila kategori bawaan sistem; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda kategori bawaan sistem.
	 *
	 * <p>Tidak diekspos di layar master — satu-satunya pemanggil nyata adalah
	 * {@link #checkCreateDefault()} yang menyetelnya {@code true} untuk baris hasil seed. Menyetel
	 * {@code true} pada baris lain akan membuat tombol Hapus baris itu ikut hilang.</p>
	 *
	 * @param defaultData penanda baru; {@code null} akan dinormalkan menjadi {@code false} pada
	 *        pembacaan berikutnya lewat {@link #getDefaultData()}
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan saklar tampil kategori, dengan <b>normalisasi menulis-balik</b>: bila field
	 * masih {@code null}, field diisi {@code true} lebih dulu (default "aktif") lalu dikembalikan.
	 *
	 * <p>Efek samping penulisan-balik sama seperti pada {@link #getDefaultData()} — pembacaan pada
	 * objek terikat session berpotensi memicu {@code UPDATE} dan revisi Envers.</p>
	 *
	 * <p><b>Konsumen:</b> seluruh query pembangun formulir dan laporan menyaring
	 * {@code kelompokParameterTambahanPengaduan.aktif = true}
	 * ({@code ParameterTambahanPengaduanListener}, {@code PengaduanAction}, dan
	 * {@code LaporanPengaduan}). Menonaktifkan kategori menyembunyikan seluruh seksi beserta
	 * field-nya dari formulir dan dari ekspor laporan, <b>tanpa</b> menghapus isian yang sudah
	 * tersimpan di blob {@code Pengaduan.parameterTambahanInds} — data lama menjadi tak terlihat
	 * tetapi tetap ada.</p>
	 *
	 * @return {@code true} bila kategori ditampilkan; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel saklar tampil kategori.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" pada renderer grid layar master,
	 * langsung diikuti {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan seketika
	 * tanpa tombol Simpan. Checkbox itu dinonaktifkan untuk pengguna tanpa hak UPDATE
	 * ({@code checkbox.setDisabled(!edit)}).</p>
	 *
	 * @param aktif saklar baru; {@code null} akan dinormalkan menjadi {@code true} pada pembacaan
	 *        berikutnya lewat {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil seksi, dengan <b>normalisasi menulis-balik</b>: bila field
	 * masih {@code null}, field diisi {@code 1} lebih dulu lalu dikembalikan.
	 *
	 * <p>Baris {@code return} mengandung pemeriksaan {@code null} kedua yang sudah tidak mungkin
	 * benar karena blok {@code if} di atasnya pasti sudah mengisi field — sisa penulisan berlapis,
	 * tidak berpengaruh pada perilaku.</p>
	 *
	 * <p><b>Konsekuensi penting.</b> Dialog Tambah/Ubah layar master <b>hanya</b> menyunting
	 * {@code nama} dan {@code keterangan}; nomor urut tidak pernah diisi di sana. Akibatnya setiap
	 * kategori baru efektif bernomor urut {@code 1}, dan karena
	 * {@link #compareTo(GeneralValueObject)} hanya membandingkan nomor urut, dua kategori bernomor
	 * sama dianggap "sama" oleh {@code TreeSet}. {@code PengaduanAction} menyalin koleksi kategori
	 * milik {@link JenisPengaduan} ke dalam {@code TreeSet} sebelum membangun formulir, sehingga
	 * kategori kedua dan seterusnya <b>hilang diam-diam</b> dari formulir selama nomor urutnya
	 * belum dibedakan. Satu-satunya tempat nomor urut bisa diubah adalah kotak angka pada grid
	 * layar master.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}, minimal {@code 1}
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil seksi.
	 *
	 * <p>Dipanggil dari listener {@code onChange} kotak angka ({@code Intbox}) pada renderer grid
	 * layar master, langsung diikuti {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan
	 * tersimpan seketika. Lihat {@link #getNomorUrut()} untuk dampak nomor urut kembar terhadap
	 * tampilnya seksi di formulir.</p>
	 *
	 * @param nomorUrut nomor urut baru; nilai {@code null} akan dinormalkan menjadi {@code 1} pada
	 *        pembacaan berikutnya
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan dua kategori <b>hanya</b> berdasarkan {@link #getNomorUrut()}.
	 *
	 * <p><b>Versi pangkas.</b> Implementasi induk
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} memakai rantai kunci berjenjang
	 * ({@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr; {@code keterangan}); override
	 * ini membuang seluruh fallback tersebut. Karena {@link #getNomorUrut()} tidak pernah
	 * mengembalikan {@code null}, method ini aman dari {@code NullPointerException}, tetapi
	 * <b>mengembalikan {@code 0} untuk setiap pasangan kategori bernomor urut sama</b> meskipun
	 * keduanya baris berbeda.</p>
	 *
	 * <p><b>Akibat pada koleksi terurut:</b> nilai {@code 0} membuat {@code TreeSet}/{@code TreeMap}
	 * memperlakukan kedua kategori sebagai satu elemen dan membuang salah satunya. Ini bukan kasus
	 * langka melainkan kondisi <b>bawaan</b>, karena nomor urut tidak pernah diisi lewat dialog
	 * Tambah/Ubah (lihat {@link #getNomorUrut()}). Jalur yang terdampak: field
	 * {@code JenisPengaduan.kelompokParameterTambahanPengaduans} yang diinisialisasi sebagai
	 * {@code TreeSet}, dan penyalinan eksplisit ke {@code TreeSet} di
	 * {@code PengaduanAction} sebelum formulir dibangun. Perhatikan bahwa relasi
	 * {@code @ManyToMany} pada {@link JenisPengaduan} sendiri sudah membawa
	 * {@code @OrderBy("nomorUrut asc, nama asc")} — pemecah-seri {@code nama} di tingkat SQL itu
	 * justru hilang begitu koleksinya disalin ke {@code TreeSet}.</p>
	 *
	 * <p>Konsisten dengan induk, hasil {@code 0} di sini <b>tidak</b> berarti {@code equals}:
	 * {@link GeneralValueObject#equals(Object)} tetap berbasis {@code id}, sehingga kelas ini
	 * memang tidak konsisten dengan {@code equals} dalam arti {@code Comparable}.</p>
	 *
	 * @param arg0 kategori pembanding; harus bertipe {@code KelompokParameterTambahanPengaduan}
	 *        karena di-<i>cast</i> tanpa pemeriksaan
	 * @return bilangan negatif/nol/positif sesuai perbandingan nomor urut
	 * @throws ClassCastException bila {@code arg0} bukan {@code KelompokParameterTambahanPengaduan}
	 * @throws NullPointerException bila {@code arg0} bernilai {@code null}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanPengaduan) arg0).getNomorUrut());
	}
}
