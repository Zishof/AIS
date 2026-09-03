package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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
 * Entity MASTER butir apresiasi siswa pada modul sekolah — satu baris tabel
 * {@code sekolah.apresiasi} mewakili SATU jenis/butir apresiasi yang dapat diberikan kepada seorang
 * siswa (mis. "Juara 1 Olimpiade Matematika Tingkat Kota", "Kehadiran 100% satu semester", "Aktif
 * dalam kegiatan sosial"), lengkap dengan BOBOT KREDIT-nya pada field {@link #getKredit()}.
 *
 * <p>Kelas ini adalah DAFTAR BUTIR, BUKAN catatan pemberian apresiasi kepada siswa tertentu.
 * Catatan per siswa disimpan di entity transaksi terpisah
 * {@link ais.database.model.sekolah.ApresiasiSiswa} (tabel {@code sekolah.apresiasi_siswa}), yang
 * memiliki relasi {@code siswa}, {@code waktu}, {@code ta} (tahun ajaran), serta relasi
 * {@code @ManyToMany} balik ke entity ini. Konsekuensinya: mengubah nama atau bobot sebuah baris di
 * sini berdampak RETROAKTIF ke seluruh catatan siswa yang sudah pernah merujuknya, karena catatan
 * itu menyimpan FK — bukan salinan nilai.</p>
 *
 * <h3>Posisi dalam rantai apresiasi (4 lapis)</h3>
 * Modul apresiasi sekolah tersusun berlapis, dan entity ini berada di lapis paling dasar:
 * <ol>
 * <li><b>{@code Apresiasi}</b> (kelas ini, tabel {@code sekolah.apresiasi}) — master BUTIR apresiasi
 * + bobot {@link #getKredit() kredit}.</li>
 * <li><b>{@link ais.database.model.sekolah.Penghargaan}</b> (tabel {@code sekolah.penghargaan}) —
 * master JENIS penghargaan/hadiah yang menyertai + bobot {@code poin}. Perannya sejajar (bukan
 * turunan) dengan kelas ini.</li>
 * <li><b>{@link ais.database.model.sekolah.ApresiasiDanPenghargaan}</b> (tabel
 * {@code sekolah.apresiasi_dan_penghargaan}) — PAKET bernama yang menggabungkan sekumpulan
 * {@code Apresiasi} dan sekumpulan {@code Penghargaan} lewat dua tabel penghubung
 * {@code @ManyToMany}. Paket inilah yang dipilih petugas saat mencatat kejadian, bukan baris entity
 * ini secara langsung. Perhatikan: meski namanya terdengar seperti transaksi, lapis ini MASIH
 * master.</li>
 * <li><b>{@link ais.database.model.sekolah.ApresiasiSiswa}</b> (tabel
 * {@code sekolah.apresiasi_siswa}) — barulah ini entity TRANSAKSI: siswa X menerima paket Y pada
 * waktu Z, dengan salinan pilihan butir {@code Apresiasi}/{@code Penghargaan} yang benar-benar
 * dicentang untuk kejadian itu.</li>
 * </ol>
 * Struktur ini adalah CERMINAN PERSIS sisi tata tertib: {@link ais.database.model.sekolah.Pelanggaran}
 * (kredit) + {@code Hukuman} (poin) → {@code PelanggaranDanHukuman} → {@code PelanggaranSiswa}.
 * Kedua sisi bahkan berbagi {@code serialVersionUID} yang sama — lihat catatan pada
 * {@link #serialVersionUID}.
 *
 * <h3>Siapa yang benar-benar membaca {@code kredit}</h3>
 * Nilai {@link #getKredit()} TIDAK diagregasi di dalam entity ini. Berdasarkan penelusuran seluruh
 * kode sumber, satu-satunya pembaca runtime-nya adalah
 * {@code ais.action.report.format1.sekolah.LaporanApresiasiSiswa}, pada dua tempat yang isinya
 * kembar (jalur laporan rekap periode dan jalur cetak satu nota): keduanya menelusuri
 * {@code apresiasiSiswa.getApresiasis()} lalu menjumlahkan {@code getKredit()} setiap anggotanya ke
 * parameter laporan {@code "kredit"} — berdampingan dengan parameter {@code "point"} yang
 * dijumlahkan dari {@code Penghargaan.getPoin()}.
 *
 * <p><b>Perbedaan penting dari sisi pelanggaran:</b> rapor siswa
 * ({@code LaporanRaporSiswa.masukkanPoin(...)}) memang membaca {@code ApresiasiSiswa}, TETAPI hanya
 * menyusuri {@code getPenghargaans()} dan menjumlahkan {@code Penghargaan.getPoin()}. Koleksi
 * {@code getApresiasis()} — dan karenanya {@code kredit} entity ini — tidak pernah disentuh di
 * jalur rapor sama sekali. Jadi bobot kredit hanya muncul di laporan apresiasi tersendiri, tidak di
 * rapor.</p>
 *
 * <h3>Cakupan multi-tenant</h3>
 * Hanya DUA field cakupan yang disediakan: {@link #getSekolah()} (unit sekolah pemilik butir) dan
 * {@link #getYayasan()} (badan penyelenggara, nilainya derivatif). <b>Berbeda dari
 * {@link ais.database.model.sekolah.Pelanggaran} yang strukturnya nyaris identik, entity ini TIDAK
 * memiliki field {@code perguruanTinggi}</b> — sehingga tidak ada getter pengisi-otomatis tenant
 * terluar yang "mengklaim" baris ber-kolom {@code NULL} atas nama sesi pembaca. Absennya field itu
 * sekaligus berarti tenant terluar tidak dapat dipakai sebagai sumbu penyaringan di modul ini.
 *
 * <p>Perlu dicatat bahwa TIDAK SATU PUN pembaca runtime yang ditemukan menyaring daftar master ini
 * berdasarkan cakupan tersebut secara otomatis — lihat catatan pada {@link #getSekolah()}.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit warisan</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}, dua konstruktor.</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 * {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 * <li><b>Isi butir</b> — {@link #getNama()}, {@link #getKeterangan()}, {@link #getKredit()},
 * {@link #getAktif()} beserta setter-nya.</li>
 * </ul>
 * Entity ini TIDAK memiliki satu pun method bisnis, query/finder statis, {@code equals}/
 * {@code hashCode}, maupun {@code compareTo()} sendiri. Seluruh isinya adalah getter/setter properti
 * dan satu callback JPA.
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 * <li><b>Komentar hbm2java di atas anotasi keliru</b> — teks aslinya berbunyi "JenisGuru generated
 * by hbm2java", sisa salin-tempel generator. Kelas ini tidak ada hubungannya dengan jenis guru.
 * Kekeliruan yang sama muncul di {@link ais.database.model.sekolah.Pelanggaran},
 * {@code PelanggaranDanHukuman}, dan {@code PelanggaranSiswa}.</li>
 * <li><b>{@code serialVersionUID} kembar lintas modul</b> — nilai {@code -7490758846785025664L}
 * DIPAKAI ULANG persis oleh keluarga pelanggaran. Tidak berbahaya (serialisasi Java tetap memeriksa
 * nama kelas), tetapi menegaskan asal-usul salin-tempel berkas ini.</li>
 * <li><b>Field warisan yang dideklarasikan ULANG bukan bug</b> — {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} sudah ada di induk
 * {@link ais.database.model.GeneralValueObject}, namun induk itu BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass} — hanya POJO abstrak biasa. Hibernate TIDAK memetakan properti induk,
 * sehingga setiap entity turunan HARUS mendeklarasikan ulang keempatnya agar tersimpan. Ini
 * keharusan teknis, jangan "dirapikan".</li>
 * <li><b>Dua getter melakukan MUTASI saat dibaca</b> — {@link #getSekolah()} menulis balik hasil
 * resolusi proxy, dan {@link #getYayasan()} bahkan MENIMPA nilai kolomnya dengan yayasan turunan
 * dari sekolah. Karena entity beranotasi {@code dynamicUpdate}, pembacaan pada entity yang masih
 * ter-attach ke session dapat memicu {@code UPDATE} kolom bersangkutan saat flush. Rinciannya ada
 * pada Javadoc masing-masing getter.</li>
 * <li><b>{@link #getKeterangan()} TIDAK membalik kontrak</b> — berbeda dari sejumlah entity lain di
 * repo ini yang getter {@code keterangan}-nya mengembalikan sesuatu selain isi kolomnya (atau tidak
 * punya field-nya sama sekali), di sini kolom {@code keterangan} nyata ada dan dikembalikan apa
 * adanya.</li>
 * <li><b>Pengurutan mewarisi {@code compareTo} induk</b> —
 * {@link ais.database.model.GeneralValueObject} mengimplementasikan {@code Comparable} dengan urutan
 * kunci {@code nomorUrut} → {@code nim} → {@code nama} → {@code keterangan}. Entity ini tidak punya
 * {@code nomorUrut} maupun {@code nim}, jadi kunci efektifnya LANGSUNG jatuh ke {@code nama}.
 * Konsekuensi penciutan {@code TreeSet} yang menyertainya dijelaskan pada {@link #getNama()}.</li>
 * <li><b>TIDAK di-preload ke cache tingkat aplikasi</b> — berbeda dari
 * {@link ais.database.model.sekolah.Pelanggaran}/{@code Hukuman}/{@code PelanggaranDanHukuman} yang
 * terdaftar pada {@code ais.common.InitData.initClasses(...)}, kelas ini TIDAK ada dalam daftar
 * mana pun di {@code InitData}. Isinya selalu dibaca langsung dari basis data lewat
 * {@code Criteria} setiap layar dibuka. Ini justru menghindarkan modul apresiasi dari amplifier
 * cache app-wide yang tercatat pada modul pelanggaran, dengan ongkos satu query tambahan per
 * pembukaan layar.</li>
 * </ul>
 *
 * <h3>Layar, impor/ekspor, dan audit</h3>
 * Layar CRUD-nya {@code ais.action.master.sekolah.ApresiasiAction}, dengan kolom ekspor/impor Excel
 * {@code {"id", "nama", "kredit", "sekolah", "keterangan", "aktif"}} (tombol unggah hanya muncul
 * bagi pengguna yang memegang hak {@code CREATE}, {@code UPDATE}, DAN {@code DELETE} sekaligus).
 * Baris master ini juga muncul sebagai daftar checkbox pada layar paket
 * {@code ApresiasiDanPenghargaanAction} dan pada dialog transaksi {@code ApresiasiSiswaAction}.
 * Entity beranotasi {@link Audited} sehingga setiap perubahan direkam Hibernate Envers ke tabel
 * bayangan, dan layar daftar menampilkan tombol revisi lewat
 * {@code RevisiHelper.createNewRevisi(...)}.
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.ApresiasiDanPenghargaan
 * @see ais.database.model.sekolah.ApresiasiSiswa
 * @see ais.database.model.sekolah.Penghargaan
 * @see ais.database.model.sekolah.Pelanggaran
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "apresiasi", schema = "sekolah")
public class Apresiasi extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini kebetulan IDENTIK dengan milik
	 * {@link ais.database.model.sekolah.Pelanggaran}, {@code PelanggaranDanHukuman}, dan
	 * {@code PelanggaranSiswa} (sisa salin-tempel generator); jangan diubah karena instance entity
	 * ikut diserialisasi ke dalam state desktop ZK.
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Kunci utama, dibangkitkan basis data ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila baris belum pernah diubah lewat interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Setter ini SENGAJA tidak simetris: nilai {@code null} atau string kosong/spasi DIABAIKAN
	 * diam-diam sehingga nilai lama tetap bertahan. Efeknya, jejak audit tidak pernah bisa
	 * dikosongkan kembali setelah terisi — termasuk saat Hibernate menghidrasi ulang baris yang
	 * kolomnya {@code NULL} ke instance yang sudah memegang nilai sebelumnya.</p>
	 *
	 * <p>Pemanggil normalnya adalah {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}, bukan kode layar.</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
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
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	 * <p>Efek samping: mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna sesi aktif dan menyegarkan
	 * {@link #getTanggal_dirubah()}. TIDAK dipanggil pada {@code INSERT} (tidak ada
	 * {@code @PrePersist}), sehingga baris baru mengandalkan nilai awal field
	 * {@link #tanggal_dirubah} serta pengisian {@code oleh} dari jalur lain.</p>
	 *
	 * <p>Perlu disadari bahwa satu klik centang "Aktif" pada layar daftar (lihat
	 * {@link #setAktif(Boolean)}) sudah cukup untuk memicu jalur ini dan menimpa jejak audit baris
	 * master.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat instance dibuat sehingga baris
	 * yang baru di-{@code INSERT} pun sudah punya stempel waktu meski {@link #onUpdate()} belum
	 * pernah berjalan.
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
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} pada instance yang dibuat
	 *         lewat konstruktor, tetapi bisa {@code null} bila kolom di basis data kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Unit sekolah pemilik butir apresiasi ini. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Badan penyelenggara pemilik butir ini. Lihat {@link #getYayasan()} — nilainya derivatif. */
	private Yayasan yayasan;
	/** Penjelasan bebas atas butir apresiasi. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Nama/judul butir apresiasi — kolom WAJIB dan kunci pengurutan efektif entity ini. */
	private String nama;

	/** Penanda aktif/nonaktif. Lihat {@link #getAktif()} — {@code null} berarti AKTIF. */
	private Boolean aktif;
	/** Bobot kredit apresiasi. Lihat {@link #getKredit()} untuk perilaku {@code null}. */
	private Double kredit;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA, sekaligus dipakai
	 * {@code ApresiasiAction.onAdd(...)} untuk menyiapkan formulir "Tambah Apresiasi".
	 *
	 * <p>Perhatikan bahwa {@link #tanggal_dirubah} sudah terisi waktu server di sini, sedangkan
	 * {@link #kredit} dan {@link #aktif} sengaja dibiarkan {@code null} — keduanya punya nilai
	 * bawaan yang dihitung di getter masing-masing.</p>
	 */
	public Apresiasi() {
	}

	/**
	 * Konstruktor ringkas berisi kolom {@code NOT NULL} saja, sisa bawaan generator hbm2java.
	 *
	 * <p>Tidak ditemukan pemanggil di dalam kode aplikasi; disediakan untuk pembuatan instance
	 * ringan pada kode uji atau skrip migrasi. Konstruktor ini MENGISI {@link #id} secara langsung,
	 * jadi instance hasilnya berperilaku seperti entity detached (baris yang sudah ada), bukan baris
	 * baru yang menunggu {@code IDENTITY}.</p>
	 *
	 * @param id   kunci utama baris yang sudah ada
	 * @param nama nama butir apresiasi
	 */
	public Apresiasi(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan basis data melalui
	 * strategi {@code IDENTITY} (kolom serial/auto-increment) — sehingga ID-nya berurutan dan mudah
	 * ditebak dari luar.</p>
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
	 * Menyetel kunci utama. Umumnya hanya dipanggil Hibernate saat menghidrasi baris; kode layar
	 * memakainya secara tidak langsung lewat {@code session.load(Apresiasi.class, id)}.
	 *
	 * @param id kunci utama; {@code null} menandakan entity baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan unit sekolah pemilik butir apresiasi ini (kolom FK {@code sekolah_id}).
	 *
	 * <p>Relasi dipetakan {@code LAZY}, sehingga nilai mentahnya bisa berupa proxy Hibernate yang
	 * belum terinisialisasi — bahkan proxy basi bila session-nya sudah ditutup. Karena itu nilai
	 * dilewatkan lebih dulu ke {@code GeneralValueObject.check(...)} yang meresolusi proxy tersebut
	 * (lewat {@code EntityIdentityMap}/cache atau query ulang) dan menuliskan hasilnya kembali ke
	 * field {@link #sekolah}.</p>
	 *
	 * <p><b>Efek samping:</b> pembacaan mengganti isi field dengan instance kanonik. Ini disengaja
	 * agar semua pemegang referensi melihat objek yang sama, tetapi berarti getter ini TIDAK bebas
	 * mutasi.</p>
	 *
	 * <p><b>Catatan cakupan:</b> kolom ini WAJIB diisi dari layar ({@code ApresiasiAction.onSave}
	 * menolak simpan bila combobox "Sekolah" kosong), namun tidak ada satu pun pembaca yang
	 * menyaringnya secara otomatis. Filter sekolah/yayasan pada layar daftar bersifat OPSIONAL —
	 * bila pengguna tidak memilih apa pun, kriterianya berubah menjadi {@code 1=1} sehingga seluruh
	 * butir apresiasi milik SEMUA sekolah dan SEMUA yayasan ikut tampil. Daftar checkbox pada layar
	 * paket {@code ApresiasiDanPenghargaanAction} bahkan tidak punya sumbu filter cakupan sama
	 * sekali (hanya menyaring {@link #getAktif() aktif}). Perilaku ini identik dengan sisi
	 * pelanggaran.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} pada baris lama/hasil impor yang kolomnya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik butir apresiasi.
	 *
	 * <p>Nilai yang belum tersimpan (ID masih {@code null}) DINORMALKAN menjadi {@code null} agar
	 * cascade {@code PERSIST} tidak diam-diam membuat baris {@code Sekolah} baru. Pola yang sama
	 * dipakai {@link #setYayasan(Yayasan)}. Dipanggil dari {@code ApresiasiAction.onSave(...)}
	 * dengan nilai dari combobox "Sekolah" yang sudah divalidasi wajib isi.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau instance tanpa ID diperlakukan sebagai
	 *                "tanpa cakupan sekolah"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan badan penyelenggara (yayasan) pemilik butir apresiasi ini (kolom FK
	 * {@code yayasan_id}).
	 *
	 * <p><b>Getter ini DESTRUKTIF dan nilainya DERIVATIF.</b> Alur kerjanya: baca
	 * {@link #getSekolah()} lebih dulu; bila sekolah terisi, field {@link #yayasan} DITIMPA dengan
	 * {@code sekolah.getYayasan()} — mengabaikan apa pun yang tersimpan di kolom
	 * {@code yayasan_id}. Baru setelah itu nilainya diresolusi lewat {@code check(...)}.</p>
	 *
	 * <p><b>Konsekuensi yang mudah mengejutkan:</b> pada baris yang punya {@code sekolah_id}, isi
	 * kolom {@code yayasan_id} di basis data efektif TIDAK PERNAH dipakai untuk membaca; dan karena
	 * entity beranotasi {@code dynamicUpdate}, sekadar membaca properti ini pada entity yang masih
	 * ter-attach dapat membuat Hibernate menuliskan yayasan turunan tersebut ke basis data saat
	 * flush berikutnya — menimpa isian yayasan yang mungkin dipilih pengguna di layar. Pada praktik
	 * layar ini dampaknya kecil karena combobox "Yayasan" memang di-{@code setReadonly(true)} dan
	 * selalu mengikuti sekolah, tetapi jalur impor Excel dan skrip migrasi tidak melalui layar itu.
	 * Kuirk yang sama sudah tercatat pada {@link ais.database.model.sekolah.Pelanggaran} dan
	 * keluarga {@code KelompokParameterTambahan*}.</p>
	 *
	 * <p>Efek samping tambahan: memanggil {@link #getSekolah()} yang juga menulis balik field
	 * {@link #sekolah}, dan dapat memicu pemuatan proxy {@code Sekolah}.</p>
	 *
	 * @return yayasan pemilik — diturunkan dari sekolah bila sekolah terisi, atau {@code null}
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
	 * Menyetel yayasan pemilik butir apresiasi.
	 *
	 * <p>Sama seperti {@link #setSekolah(Sekolah)}, instance tanpa ID dinormalkan menjadi
	 * {@code null}. Perhatikan bahwa nilai yang disetel di sini akan DITIMPA lagi oleh
	 * {@link #getYayasan()} pada pembacaan berikutnya bila {@link #sekolah} terisi — jadi setter ini
	 * praktis hanya berpengaruh pada baris yang tidak punya sekolah.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau instance tanpa ID berarti tanpa cakupan
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan penjelasan bebas atas butir apresiasi (kolom {@code keterangan}).
	 *
	 * <p>Getter ini polos: mengembalikan isi kolomnya apa adanya, tanpa nilai bawaan maupun
	 * substitusi dari properti lain. Di layar daftar {@code ApresiasiAction} nilainya ditampilkan
	 * langsung sebagai {@code Label} (termasuk {@code null}, yang dirender sebagai teks kosong oleh
	 * ZK), dan ikut serta dalam ekspor/impor Excel.</p>
	 *
	 * <p>Nilai ini juga menjadi kunci pengurutan KEEMPAT (terakhir) pada {@code compareTo} warisan
	 * {@link ais.database.model.GeneralValueObject} — praktis tidak pernah terpakai karena
	 * {@link #getNama()} sudah menyelesaikan perbandingan lebih dulu, kecuali bila {@code nama}
	 * kedua baris sama-sama {@code null}.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel penjelasan bebas atas butir apresiasi.
	 *
	 * <p>Diisi dari {@code Textbox} "Keterangan" (3 baris) pada dialog Tambah/Ubah di
	 * {@code ApresiasiAction}. Tidak ada validasi panjang maupun wajib isi.</p>
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama/judul butir apresiasi (kolom {@code nama}, {@code NOT NULL}).
	 *
	 * <p>Ini satu-satunya kolom yang divalidasi wajib isi oleh layar
	 * ({@code ApresiasiAction.onSave(...)} menolak simpan bila kosong setelah di-{@code trim}),
	 * sekaligus label yang muncul pada checkbox pemilihan di layar paket
	 * {@code ApresiasiDanPenghargaanAction} dan pada dialog transaksi
	 * {@code ApresiasiSiswaAction}.</p>
	 *
	 * <p><b>Kuirk pengurutan yang perlu diwaspadai:</b> {@code compareTo} yang diwarisi dari
	 * {@link ais.database.model.GeneralValueObject} jatuh ke perbandingan {@code nama} untuk entity
	 * ini (karena {@code nomorUrut} dan {@code nim} selalu {@code null} di sini), dan mengembalikan
	 * {@code 0} untuk dua nama yang sama persis. Kolom ini TIDAK punya batasan {@code UNIQUE}, dan
	 * daftar pemilihannya tidak disaring per sekolah — jadi dua sekolah dalam satu yayasan sangat
	 * mungkin sama-sama punya butir "Juara Kelas". Begitu koleksi semacam itu dibungkus
	 * {@code TreeSet} — yang terjadi di DUA tempat:
	 * {@code ApresiasiSiswaAction} ({@code new TreeSet<Apresiasi>(apresiasiSiswa.getApresiasis())})
	 * dan {@code ApresiasiDanPenghargaanAction}
	 * ({@code new TreeSet<Apresiasi>(apresiasiDanPenghargaan.getApresiasis())}) — baris kedua LENYAP
	 * dari daftar tampilan tanpa pesan apa pun. Total kredit pada
	 * {@code LaporanApresiasiSiswa} tetap menghitung keduanya (laporan menyusuri {@code Set} asli,
	 * bukan {@code TreeSet}), sehingga angka di layar dan di laporan bisa berbeda.</p>
	 *
	 * @return nama butir apresiasi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama/judul butir apresiasi.
	 *
	 * <p>Karena catatan apresiasi siswa merujuk baris ini lewat FK (bukan menyalin namanya),
	 * mengubah nama di sini otomatis mengubah tampilan SELURUH catatan lama yang merujuknya,
	 * termasuk laporan apresiasi yang dicetak ulang untuk periode yang sudah lewat.</p>
	 *
	 * @param nama nama butir apresiasi; tidak boleh {@code null} pada saat simpan (kolom
	 *             {@code NOT NULL}) — validasi wajib-isinya ada di layar, bukan di sini
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan penanda apakah butir apresiasi masih dipakai.
	 *
	 * <p><b>Perhatikan default-nya:</b> {@code null} dibaca sebagai {@code true}. Jadi baris lama
	 * (atau hasil impor Excel) yang kolomnya belum pernah diisi otomatis dianggap AKTIF, dan query
	 * penyaring di aplikasi konsisten menuliskannya sebagai
	 * {@code isNull("aktif") OR eq("aktif", true)} — persis seperti pada daftar checkbox di
	 * {@code ApresiasiDanPenghargaanAction}. Kolom ini tidak beranotasi {@code @Column} sehingga
	 * memakai nama kolom bawaan {@code aktif}.</p>
	 *
	 * @return {@code true} bila aktif (termasuk saat nilai tersimpan {@code null}), {@code false}
	 *         bila dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif/nonaktif butir apresiasi.
	 *
	 * <p>Dipanggil langsung dari listener {@code onCheck} checkbox "Aktif" di layar daftar
	 * {@code ApresiasiAction}, yang segera menyusulnya dengan
	 * {@code Common.refreshSaveOrUpdate(apresiasi)} — jadi satu klik centang langsung tersimpan
	 * tanpa dialog konfirmasi, sekaligus memicu {@link #onUpdate()}. Checkbox tersebut
	 * di-{@code setDisabled(true)} bila pengguna tidak memegang hak {@code UPDATE}.</p>
	 *
	 * <p>Menonaktifkan sebuah butir hanya menyembunyikannya dari daftar pilihan pada layar paket;
	 * paket {@link ais.database.model.sekolah.ApresiasiDanPenghargaan} dan catatan
	 * {@link ais.database.model.sekolah.ApresiasiSiswa} yang SUDAH terlanjur merujuknya TETAP
	 * menampilkan dan menghitung butir itu — termasuk pada laporan yang dicetak setelahnya.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menonaktifkan; {@code null}
	 *              diperlakukan sebagai aktif saat dibaca kembali
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan BOBOT KREDIT apresiasi — jumlah "kredit" yang diperoleh siswa setiap kali butir
	 * ini diberikan kepadanya.
	 *
	 * <p><b>Perhatikan default-nya:</b> nilai {@code null} dibaca sebagai {@code 0.0}. Karena
	 * penjumlahan di {@code LaporanApresiasiSiswa} melakukan {@code kredit += apresiasi.getKredit()}
	 * tanpa pemeriksaan tambahan, butir yang bobotnya belum pernah diisi ikut masuk perhitungan
	 * sebagai nol — sama sekali tidak terbedakan dari butir yang memang sengaja bernilai nol. Kolom
	 * ini tidak beranotasi {@code @Column} sehingga memakai nama kolom bawaan {@code kredit}.</p>
	 *
	 * <p>Nilai ini berpasangan dengan {@code Penghargaan.getPoin()}: laporan apresiasi siswa
	 * menyajikan keduanya berdampingan sebagai parameter {@code "kredit"} (total bobot apresiasi)
	 * dan {@code "point"} (total bobot penghargaan). Tidak ada ambang batas, akumulasi, maupun
	 * konversi otomatis yang dihitung di dalam entity ini — penafsiran totalnya sepenuhnya urusan
	 * laporan dan kebijakan sekolah.</p>
	 *
	 * <p><b>Jangkauan pembacanya lebih sempit dari yang diduga:</b> hanya
	 * {@code LaporanApresiasiSiswa} (dua jalur: rekap periode dan cetak satu nota) yang membaca
	 * nilai ini. Rapor siswa ({@code LaporanRaporSiswa}) TIDAK membacanya sama sekali — bagian
	 * apresiasi pada rapor hanya menjumlahkan {@code Penghargaan.getPoin()}. Layar daftar
	 * {@code ApresiasiAction} menampilkannya lewat {@code Common.numberFormat}, dan dasbor apresiasi
	 * hanya menghitung CACAH kejadian, bukan bobotnya.</p>
	 *
	 * @return bobot kredit apresiasi; {@code 0.0} bila belum diisi
	 */
	public Double getKredit() {
		return kredit == null ? 0.0 : kredit;
	}

	/**
	 * Menyetel bobot kredit apresiasi.
	 *
	 * <p>Diisi dari {@code MyDoublebox} "Kredit" pada dialog Tambah/Ubah di
	 * {@code ApresiasiAction}, dan dari kolom {@code kredit} pada impor Excel. Tidak ada validasi
	 * rentang: nilai negatif pun diterima dan akan MENGURANGI total kredit pada laporan — yang
	 * secara semantik janggal untuk sebuah apresiasi.</p>
	 *
	 * <p>Mengubah bobot berlaku RETROAKTIF — laporan menghitung ulang dari master setiap kali
	 * dicetak, sehingga total kredit catatan apresiasi lama ikut berubah tanpa jejak pada catatan
	 * itu sendiri (jejaknya hanya ada di tabel Envers milik entity ini).</p>
	 *
	 * @param kredit bobot kredit; boleh {@code null} (dibaca sebagai {@code 0.0})
	 */
	public void setKredit(Double kredit) {
		this.kredit = kredit;
	}
}
