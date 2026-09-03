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
 * Master <b>Paket PPDB/PSB</b> — katalog "paket" (jalur) pendaftaran calon siswa baru pada modul
 * sekolah, dipetakan ke tabel {@code sekolah.paket_psb}.
 *
 * <h3>Peran yang TERVERIFIKASI dalam alur PSB/PPDB</h3>
 * Baris entity ini adalah label sebuah <i>jalur/paket pendaftaran</i> (contoh khas: "Reguler",
 * "Prestasi", "Beasiswa", "Boarding"). Perannya dipastikan dari enam sumber independen di dalam
 * repo, bukan dari nama kelasnya:
 * <ol>
 * <li><b>Judul layar</b> — {@code ais.action.master.sekolah.PaketPsbAction#init(PaketPsb)}
 * menyetel judul dialog menjadi <i>"Tambah Paket PPDB"</i> / <i>"Ubah Paket PPDB"</i>, dan
 * label isian formulirnya adalah <i>"Kode Paket"</i>, <i>"Nama Paket"</i>, <i>"Yayasan"</i>,
 * <i>"Sekolah"</i>, <i>"Keterangan"</i>. Jadi istilah baku pada UI adalah <b>Paket PPDB</b>,
 * bukan "paket biaya".</li>
 * <li><b>Pengikatan ke gelombang</b> — {@link PaketPsbPunyaGelombangPendaftaranPsb} adalah tabel
 * silang paket &harr; {@link GelombangPendaftaranPsb}. Pada
 * {@code GelombangPendaftaranPsbAction} isian pengikatnya berlabel <i>"Gelombang pendaftaran ini
 * hanya berlaku untuk paket tertentu"</i> dengan kolom centang <i>"Pilih Paket"</i>. Artinya:
 * satu gelombang boleh dibatasi hanya pada sebagian paket.</li>
 * <li><b>Pilihan calon siswa</b> — {@code CalonSiswaAction} dan SELURUH varian formulir PPDB
 * publik ({@code PPDB1}, {@code PPDB2}, {@code PPDB3}, {@code PPDB_Alumni},
 * {@code PPDB_Simple} … {@code PPDB_Simple8}) menampilkan combobox <i>"Pilih Paket"</i> yang
 * isinya diambil dari paket-paket yang terikat pada gelombang yang sedang dipilih. Baris combo
 * itu <b>disembunyikan</b> bila gelombang tersebut tidak punya pengikatan paket sama sekali, dan
 * <b>wajib diisi</b> bila baris itu tampil. Hasilnya disimpan ke {@code CalonSiswa#paketPsb}.</li>
 * <li><b>Diwariskan ke siswa aktif</b> — {@code Siswa#getPaketPsb()} membaca kolomnya sendiri,
 * dan bila kosong <i>jatuh balik</i> ke {@code ambilCalonSiswa().getPaketPsb()}. Jadi paket yang
 * dipilih saat mendaftar tetap melekat pada rekam siswa setelah diterima.</li>
 * <li><b>Dimensi penargetan biaya</b> — {@code PengaturanBiaya#getPaketPsb()} memakai entity ini
 * sebagai <i>sasaran</i> aturan biaya, dan
 * {@code DetailTagihanCalonSiswaHelper#initCriteria(...)} /
 * {@code DetailTagihanSiswaHelper#initCriteria(...)} menerjemahkannya menjadi
 * {@code Restrictions.eq("paketPsb", …)} saat menyusun daftar penerima tagihan. Inilah alasan
 * paket sering disalahartikan sebagai "bundel biaya": paket bukan berisi biaya, tetapi biaya
 * bisa <b>ditujukan kepada</b> sebuah paket.</li>
 * <li><b>Dimensi analitik</b> — {@code RekapJalurMasukMultiTahunPsb} memakai daftar paket sebagai
 * filter berlabel <i>"Paket:"</i> / <i>"Semua Paket"</i> pada laporan rekap <b>jalur masuk</b>
 * multi-tahun (peminat, diterima, bayar pendaftaran, daftar ulang). Ini konfirmasi paling
 * eksplisit bahwa paket dibaca sebagai <b>jalur masuk</b>.</li>
 * </ol>
 *
 * <h3>Padanan di modul lain</h3>
 * Padanan sisi perguruan tinggi/PMB adalah {@code ais.database.model.Paket} +
 * {@code PaketJurusanPmb} + {@code PaketPunyaProgram}; entity ini adalah versi sekolah yang jauh
 * lebih ringkas (tidak menyimpan kuota, tanggal, biaya, maupun daftar mata pelajaran ujian —
 * semua itu tetap milik {@link GelombangPendaftaranPsb} dan {@code PengaturanBiaya}).
 *
 * <h3>Layar pengelola dan konsekuensi hak akses</h3>
 * Layar CRUD-nya adalah {@code /pages/master/sekolah/paket_psb.zul}
 * ({@code ais.action.master.sekolah.PaketPsbAction}). <b>Halaman itu tidak terdaftar sebagai menu
 * mandiri</b> — satu-satunya jalan masuk adalah tab <i>Paket</i> di dalam layar Gelombang
 * Pendaftaran PSB ({@code GelombangPendaftaranPsbAction#onPaket(Event)} menyisipkannya lewat
 * {@code MyInclude}). Karena {@code CommonPrivilages.checkPrevilages(...)} selalu memakai
 * {@code Common.getCurrentMenu()}, seluruh hak CREATE/UPDATE/DELETE yang berlaku di layar ini
 * sesungguhnya adalah hak pada menu <b>Gelombang Pendaftaran PSB</b>, bukan hak khusus paket.
 * Konsekuensinya: memberi seseorang hak ubah gelombang otomatis memberinya hak ubah dan hapus
 * seluruh master paket.
 *
 * <h3>Cakupan tenant (sekolah/yayasan)</h3>
 * Kedua relasi tenant boleh {@code null}, dan itu <b>bermakna</b>: paket dengan
 * {@code sekolah == null} berlaku untuk semua sekolah — {@code GelombangPendaftaranPsbAction}
 * menyaring daftar paket dengan {@code isNull("sekolah") OR eq("sekolah", s)} (idem untuk
 * yayasan), jadi baris tanpa sekolah sengaja ikut terbawa ke setiap sekolah.
 *
 * <p>Namun layar masternya sendiri <b>tidak</b> memaksakan penyaringan itu:
 * {@code PaketPsbAction#initCriteria(boolean)} hanya menyaring bila combo
 * {@code searchsekolah}/{@code searchyayasan} kebetulan terisi; bila kosong dipakai
 * {@code Restrictions.sqlRestriction("1=1")}. Pengisian combo dikerjakan
 * {@code InitComboUtil.initYayasanDanSekolahDanSemua(...)} yang <b>gagal-terbuka</b>: seluruh
 * badannya dibungkus {@code try/catch} yang menelan exception, dan bila pengguna tidak punya
 * sekolah/yayasan melekat serta tidak ada konteks sekolah aktif maka tidak ada apa pun yang
 * terpilih. Pada kondisi itu daftar menampilkan paket SELURUH sekolah dan yayasan, lengkap
 * dengan tombol Ubah/Hapus per baris. Pola ini identik dengan keluarga temuan fail-open yang
 * sudah tercatat pada audit repo ini; keparahan di sini <b>rendah</b> karena isinya metadata
 * katalog (kode, nama, keterangan) — bukan data pribadi — tetapi permukaan <b>tulis</b> lintas
 * tenant tetap nyata (mengubah nama, menonaktifkan, atau menghapus paket milik sekolah lain).
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum mengubah berkas ini</h3>
 * <ul>
 * <li><b>Field induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * TIDAK memetakan properti apa pun miliknya. Maka {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}, dan juga {@code kode}/{@code nama}/{@code keterangan} <b>harus</b>
 * dideklarasikan ulang di sini agar terpetakan. Ini KEHARUSAN TEKNIS, bukan duplikasi yang bisa
 * "dibersihkan".</li>
 * <li><b>Akibat sampingannya: field induk selamanya kosong.</b> Karena field lokal
 * {@code kode}/{@code nama}/{@code keterangan} membayangi ({@code shadow}) field bernama sama di
 * induk, kode apa pun yang membaca field induk secara langsung (bukan lewat getter) akan
 * mendapat {@code null}. Saat ini aman, karena seluruh method induk — termasuk
 * {@code GeneralValueObject#toString()} dan {@code compareTo(...)} — mengaksesnya lewat getter
 * yang di-override kelas ini.</li>
 * <li><b>Kontrak {@code getKeterangan()} DIBALIK.</b> Lihat {@link #getKeterangan()}.</li>
 * <li><b>{@code getYayasan()} menurunkan ulang nilainya setiap kali dibaca.</b> Lihat
 * {@link #getYayasan()} — nilai yang disetel {@link #setYayasan(Yayasan)} akan ditimpa selama
 * {@code sekolah} terisi.</li>
 * <li><b>Nama paket wajib unik SECARA GLOBAL.</b> {@code PaketPsbAction#checkNamaPaketPsb()}
 * mencari duplikat dengan {@code Restrictions.eq("nama", …)} tanpa menyertakan filter sekolah
 * maupun yayasan. Pada instalasi multi-sekolah, sekolah kedua yang mencoba membuat paket
 * bernama "Reguler" akan ditolak dengan pesan <i>"Nama Paket sudah ada di database"</i> padahal
 * bentrokannya ada di sekolah lain yang tidak boleh ia lihat — sekaligus menjadi oracle
 * keberadaan data tenant lain. Ini perilaku nyata, bukan dugaan.</li>
 * </ul>
 *
 * <h3>Verifikasi pola arsitektur berulang milik repo ini</h3>
 * <ul>
 * <li><b>Getter destruktif/write-back</b> — <b>ADA, dua tingkat.</b> {@link #getSekolah()}
 * menulis balik hasil {@code check(...)} (ringan, sekadar de-proxy), sedangkan
 * {@link #getYayasan()} benar-benar <i>menurunkan ulang</i> isi field {@code yayasan} dari
 * {@code sekolah.getYayasan()} pada setiap pembacaan.</li>
 * <li><b>{@code getKeterangan()} membalik kontrak induk</b> — <b>ADA.</b> Induk menjamin tidak
 * pernah {@code null}; override di sini mengembalikan nilai mentah sehingga bisa {@code null}.
 * Lihat {@link #getKeterangan()}.</li>
 * <li><b>{@code compareTo()} dipangkas</b> — <b>TIDAK ADA.</b> Kelas ini tidak meng-override
 * {@code compareTo}/{@code equals}/{@code hashCode}; seluruhnya diwarisi apa adanya. Yang ada
 * hanyalah efek tidak langsung dari poin sebelumnya (cabang {@code keterangan} pada
 * {@code compareTo} induk kehilangan jaminan non-null).</li>
 * <li><b>Penciutan {@code TreeSet}</b> — <b>TIDAK ADA.</b> Tidak ada koleksi apa pun di entity
 * ini, dan tidak ditemukan pemanggil yang memasukkan {@code PaketPsb} ke dalam
 * {@code TreeSet}/{@code TreeMap}; seluruh pemanggil memakai {@code List} atau
 * {@code HashMap} berkunci {@code Long id}.</li>
 * <li><b>Fail-open cakupan tenant sekolah/yayasan</b> — <b>ADA</b> pada layar masternya (lihat
 * bagian "Cakupan tenant" di atas), dengan keparahan rendah.</li>
 * <li><b>Broken access control</b> — <b>ADA dalam bentuk pewarisan menu</b> (lihat bagian "Layar
 * pengelola"): hak pada menu Gelombang Pendaftaran PSB otomatis menjadi hak penuh atas master
 * paket, karena halaman paket tidak punya menu sendiri.</li>
 * <li><b>Kolom {@code aktif} tak pernah ditulis layar master</b> — <b>TIDAK ADA (kontra-contoh
 * positif).</b> Formulir tambah/ubah memang tidak pernah menyentuh {@code aktif}, TETAPI
 * {@link #getAktif()} mengembalikan {@code true} saat field {@code null} dan Hibernate memakai
 * akses properti, sehingga baris baru tetap masuk dengan {@code aktif = true}. Bandingkan dengan
 * cacat kembar pada {@code JenisCatatanSiswa}/{@code JenisNilaiSiswa} yang membuat baris baru
 * tidak pernah muncul di combobox.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit warisan</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 * constructor {@link #PaketPsb()}.</li>
 * <li><b>Atribut deskriptif</b> — {@link #getKode()}/{@link #setKode(String)},
 * {@link #getNama()}/{@link #setNama(String)},
 * {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * <li><b>Saklar tampil</b> — {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 * <li><b>Relasi tenant</b> — {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 * {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 * </ul>
 * Tidak ada method bisnis, query statis, maupun helper perhitungan di kelas ini; seluruh logika
 * pemilihan/penyaringan paket berada di Action dan helper yang disebut di atas.
 *
 * <h3>Persistensi</h3>
 * {@code @Entity} dengan {@code dynamicInsert}/{@code dynamicUpdate} (hanya kolom yang benar-benar
 * berubah yang ikut pada INSERT/UPDATE) dan {@code @Audited} (Envers merekam setiap revisi ke
 * tabel bayangan). Karena {@code @Id} dipasang pada <i>getter</i>, Hibernate memakai <b>akses
 * properti</b>: seluruh normalisasi di dalam getter (trim, default {@code ""}/{@code true},
 * penurunan ulang yayasan) ikut menentukan nilai yang benar-benar ditulis ke basis data.
 *
 * @see GeneralValueObject
 * @see PaketPsbPunyaGelombangPendaftaranPsb
 * @see GelombangPendaftaranPsb
 * @see CalonSiswa
 * @see PengaturanBiaya
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "paket_psb")
public class PaketPsb extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai {@code 2463821577548439808L} adalah nilai <i>template</i> yang dipakai ulang oleh
	 * ratusan entity lain di repo ini (hasil salin-tempel berkas generator). Tidak berbahaya,
	 * karena mekanisme serialisasi Java tetap memeriksa nama kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, dipetakan ke kolom {@code id}. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>validasi non-trivial</b>: argumen
	 * {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b> (method langsung
	 * {@code return} tanpa menyentuh field).
	 *
	 * <p>Akibatnya jejak audit hanya bisa diisi atau ditimpa, tidak pernah bisa dikosongkan
	 * kembali lewat setter ini. Perilaku sama persis dengan {@link #setOleh(String)} dan dengan
	 * pola yang dipakai seluruh entity turunan {@link GeneralValueObject}.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan <b>validasi non-trivial</b> yang sama
	 * dengan {@link #setOlehId(String)}: argumen {@code null} atau kosong/spasi diabaikan
	 * diam-diam sehingga jejak audit tidak dapat dihapus.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
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
	 * UPDATE dikirim, dan meneruskan objek ini ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna sesi aktif serta memperbarui
	 * {@code tanggal_dirubah}.
	 *
	 * <p><b>Hanya UPDATE.</b> Tidak ada {@code @PrePersist}, sehingga baris yang baru dibuat
	 * mengandalkan nilai awal field {@code tanggal_dirubah} (disetel
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat) dan tidak pernah mendapat
	 * {@code oleh}/{@code olehId} dari jalur ini.</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} — hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya.</p>
	 *
	 * <p>Perlu disadari bahwa satu klik centang "Aktif" pada layar daftar (lihat
	 * {@link #setAktif(Boolean)}) sudah cukup untuk memicu jalur ini, menimpa jejak audit, dan
	 * menciptakan satu revisi Envers baru.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi: nilainya diurus {@link #onUpdate()} lewat
	 * {@code AuditTimestampInterceptor}, dan diisi Hibernate saat hidrasi baris.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini, dipetakan sebagai kolom
	 * {@code TIMESTAMP}.
	 *
	 * <p>Untuk baris yang belum pernah di-UPDATE nilainya adalah waktu objek Java ini dibuat,
	 * bukan waktu penyimpanan pertama — perbedaan yang biasanya tidak kasat mata karena keduanya
	 * hanya berselisih beberapa milidetik pada alur simpan normal.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         constructor Java
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: {@code id + "-" + nama}, mis. {@code "12-Reguler"}.
	 *
	 * <p><b>Berbeda dari induk.</b> {@code GeneralValueObject#toString()} menghasilkan
	 * {@code "kode - nama"}; override ini menukar kode dengan id dan menghilangkan spasi di
	 * sekitar tanda hubung. Untuk baris yang belum tersimpan hasilnya diawali {@code "null-"},
	 * dan bila {@code nama} belum diisi hasilnya berakhir dengan teks {@code "null"}.</p>
	 *
	 * <p>Perhatikan bahwa method ini membaca <b>field</b> {@code nama} secara langsung, bukan
	 * {@link #getNama()}, sehingga spasi di ujung nama TIDAK dipangkas di sini.</p>
	 *
	 * <p>Pemakaian nyata terbatas pada log/diagnostik dan pada
	 * {@code PaketPsbPunyaGelombangPendaftaranPsb#toString()} yang merangkainya menjadi
	 * {@code paket_gelombang}. Combobox dan grid tidak memakainya — keduanya menampilkan
	 * {@link #getNama()} secara eksplisit.</p>
	 *
	 * @return gabungan id dan nama paket
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas paket (kolom {@code kode}). Lihat {@link #getKode()}. */
	private String kode;

	/** Nama paket, kolom wajib {@code nama}. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas paket (kolom {@code keterangan}, boleh kosong). Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Sekolah pemilik paket; {@code null} berarti berlaku untuk semua sekolah. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik paket; diturunkan ulang dari {@code sekolah} bila ada. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Saklar tampil paket; {@code null} diperlakukan sebagai aktif. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Constructor default tanpa argumen.
	 *
	 * <p>WAJIB ada karena Hibernate membutuhkannya untuk membuat instance saat hidrasi baris dari
	 * basis data. Dipakai juga oleh {@code PaketPsbAction#onAdd(Event)} untuk menyiapkan objek
	 * kosong bagi dialog "Tambah Paket PPDB". Seluruh field dibiarkan {@code null} kecuali
	 * {@code tanggal_dirubah} yang langsung terisi waktu saat ini.</p>
	 */
	public PaketPsb() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolom {@code id} bertipe {@code IDENTITY} (auto-increment sisi basis data) dan ditandai
	 * {@code insertable = false} sehingga nilainya tidak pernah dikirim pada INSERT melainkan
	 * dibaca kembali setelahnya. Nilainya berurutan dan mudah ditebak — relevan bila suatu saat
	 * ada endpoint yang menerima id paket dari parameter.</p>
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
	 * Menyetel kunci utama baris. Tanpa validasi.
	 *
	 * @param id nilai kunci utama yang baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas paket, <b>dinormalisasi</b>: {@code null} maupun string kosong
	 * dikembalikan sebagai {@code ""}, selebihnya dipangkas spasi ujungnya.
	 *
	 * <p><b>Berbeda dari induk</b>, yang mengembalikan nilai mentah (bisa {@code null}). Karena
	 * Hibernate memakai akses properti pada entity ini, normalisasi tersebut ikut menentukan nilai
	 * yang ditulis ke kolom {@code kode}: baris baru yang kodenya dikosongkan pengguna tersimpan
	 * sebagai string kosong, bukan {@code NULL}.</p>
	 *
	 * <p>Ditampilkan sebagai kolom pertama grid daftar paket dan dapat dicari lewat isian "Kode"
	 * ({@code Restrictions.ilike("kode", …, ANYWHERE)}). Tidak ada pemeriksaan keunikan kode sama
	 * sekali — yang diperiksa {@code PaketPsbAction} hanyalah keunikan {@link #getNama()}.</p>
	 *
	 * @return kode paket yang sudah dipangkas, atau {@code ""} bila belum diisi; tidak pernah
	 *         {@code null}
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	/**
	 * Menyetel kode ringkas paket. Tanpa validasi, tanpa pemangkasan, dan tanpa pemeriksaan
	 * duplikat.
	 *
	 * <p>Satu-satunya penulis nyata adalah {@code PaketPsbAction#onSave(Event)} yang meneruskan
	 * isi {@code Textbox} "Kode Paket" apa adanya.</p>
	 *
	 * @param kode kode paket yang baru; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama paket dengan spasi ujung dipangkas ({@code null} tetap {@code null}).
	 *
	 * <p>Kolom {@code nama} dideklarasikan {@code nullable = false} sepanjang 255 karakter, dan
	 * {@code PaketPsbAction#onSave(Event)} menolak menyimpan bila isian kosong ("Nama Paket harus
	 * diisi"). Nilai inilah yang tampil sebagai label pilihan paket pada formulir PPDB, pada
	 * daftar centang "Pilih Paket" di layar gelombang, pada filter "Paket:" laporan rekap jalur
	 * masuk, dan pada tautan revisi Envers di grid master.</p>
	 *
	 * <p><b>Nama wajib unik secara global</b> — lihat pembahasan
	 * {@code PaketPsbAction#checkNamaPaketPsb()} pada Javadoc kelas: pemeriksaan duplikat tidak
	 * menyertakan filter sekolah/yayasan, sehingga dua sekolah berbeda tidak dapat memakai nama
	 * paket yang sama.</p>
	 *
	 * <p>Karena Hibernate memakai akses properti, pemangkasan di sini juga berlaku pada nilai yang
	 * ditulis ke basis data.</p>
	 *
	 * @return nama paket yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama paket. Tanpa validasi dan tanpa pemangkasan — pemeriksaan wajib-isi serta
	 * pemeriksaan duplikat dilakukan di {@code PaketPsbAction#onSave(Event)} sebelum setter ini
	 * dipanggil, bukan di sini.
	 *
	 * @param nama nama paket yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas paket <b>apa adanya</b> — termasuk {@code null}.
	 *
	 * <p><b>PENTING — override ini MEMBALIK kontrak induk.</b>
	 * {@code GeneralValueObject#getKeterangan()} secara eksplisit menjamin tidak pernah
	 * mengembalikan {@code null} (mengubahnya menjadi {@code ""}); override di sini menghapus
	 * jaminan tersebut. Dua akibat yang sudah ditelusuri:</p>
	 * <ol>
	 * <li><b>{@code compareTo(...)} induk kehilangan cabang terakhirnya.</b> Urutan alami
	 * {@link GeneralValueObject} adalah {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama}
	 * &rarr; {@code keterangan}. Javadoc induk menyatakan cabang {@code keterangan}
	 * <i>selalu</i> memenuhi syarat karena tidak pernah {@code null} — untuk {@code PaketPsb}
	 * pernyataan itu tidak lagi berlaku, sehingga dua paket tanpa nama akan dianggap setara
	 * (nilai {@code 0}) alih-alih diurutkan menurut keterangan. Dampak praktis kecil: seluruh
	 * paket wajib bernama, dan pengurutan yang dipakai layar adalah
	 * {@code Order.asc("nama")} sisi basis data.</li>
	 * <li><b>Pemanggil harus siap menerima {@code null}.</b> Seluruh pemanggil yang ada sudah
	 * aman — {@code PaketPsbRenderer} membungkusnya dalam {@code Label} ZK (menerima
	 * {@code null}), dan combobox PPDB memakainya sebagai teks deskripsi lewat helper reflektif
	 * yang null-safe. Tidak ditemukan NPE aktual; catatan ini untuk pemanggil BARU.</li>
	 * </ol>
	 *
	 * <p>Isi keterangan ikut terekspos ke pendaftar anonim pada formulir PPDB publik (dipakai
	 * sebagai deskripsi item combo "Pilih Paket"), jadi jangan menaruh catatan internal di sana.</p>
	 *
	 * @return keterangan paket, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas paket. Tanpa validasi; nilai {@code null} diterima dan akan
	 * terbaca kembali sebagai {@code null} (bukan {@code ""}) lewat {@link #getKeterangan()}.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan saklar tampil paket, dengan <b>default aman</b>: field {@code null}
	 * diperlakukan sebagai {@code true}.
	 *
	 * <p>Tidak ada anotasi {@code @Column}, jadi terpetakan ke kolom {@code aktif} dengan
	 * konvensi nama bawaan.</p>
	 *
	 * <p><b>Mengapa default ini penting.</b> Formulir tambah/ubah paket TIDAK PERNAH menyentuh
	 * {@code aktif} — saklarnya hanya ada sebagai centang per baris di grid daftar. Karena
	 * Hibernate memakai akses properti dan {@code dynamicInsert} membaca nilai lewat getter ini,
	 * baris baru tetap masuk dengan {@code aktif = true} sehingga langsung tampil di semua daftar.
	 * Ini kontras dengan cacat yang tercatat pada {@code JenisCatatanSiswa}/{@code JenisNilaiSiswa},
	 * di mana ketiadaan penulisan {@code aktif} membuat baris baru tidak pernah muncul.</p>
	 *
	 * <p><b>Konsistensi filter.</b> Penyaring daftar memakai
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} — cocok dengan default getter
	 * ini — dan {@code GelombangPendaftaranPsbAction} memakai pola yang sama saat menyusun daftar
	 * centang paket. Sebaliknya, combobox paket pada {@code CalonSiswaAction} dan seluruh formulir
	 * PPDB <b>tidak</b> menyaring {@code aktif} sama sekali (kriterianya murni
	 * {@code Restrictions.in("id", …)} dari tabel silang gelombang). Konsekuensi nyata:
	 * menonaktifkan sebuah paket menghilangkannya dari layar administrasi, tetapi paket itu TETAP
	 * bisa dipilih pendaftar selama pengikatannya ke gelombang belum dilepas.</p>
	 *
	 * @return {@code true} bila paket aktif atau belum pernah disetel; {@code false} hanya bila
	 *         eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel saklar tampil paket. Tanpa validasi.
	 *
	 * <p>Satu-satunya penulis adalah centang "Aktif" pada grid daftar
	 * ({@code PaketPsbAction.PaketPsbRenderer}), yang langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate(paketPsb)} tanpa dialog konfirmasi. Centang itu dinonaktifkan
	 * bila pengguna tidak punya hak UPDATE — tetapi hak UPDATE yang diperiksa adalah hak pada menu
	 * <b>Gelombang Pendaftaran PSB</b>, karena halaman paket hanya hidup sebagai tab di dalamnya.</p>
	 *
	 * <p>Efek samping tidak langsung: penyimpanan itu memicu {@link #onUpdate()}, menimpa
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah}, dan mencatat satu revisi Envers baru.</p>
	 *
	 * @param aktif nilai saklar baru; {@code null} akan terbaca kembali sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan sekolah pemilik paket ({@code null} = paket berlaku untuk semua sekolah).
	 *
	 * <p><b>Getter dengan efek samping (write-back).</b> Hasil {@code check(sekolah)} ditulis
	 * kembali ke field sebelum dikembalikan. {@code check(...)} milik {@link GeneralValueObject}
	 * meresolusi proxy lazy Hibernate dan, bila memungkinkan, menukarnya dengan instance kanonik
	 * dari {@code EntityIdentityMap}. Efeknya: pembacaan pertama atas relasi ini <b>membatalkan
	 * kemalasan</b> {@code FetchType.LAZY} yang dideklarasikan di bawah, dan instance yang
	 * dikembalikan bisa berbeda object dari yang tadi disetel.</p>
	 *
	 * <p>Relasi {@code @ManyToOne} ke kolom {@code sekolah_id} dengan cascade
	 * {@code PERSIST}/{@code MERGE} — menyimpan paket ikut menyimpan sekolah yang menempel padanya.</p>
	 *
	 * <p>Nilai ini menentukan cakupan paket: {@code GelombangPendaftaranPsbAction} menyaring daftar
	 * paket dengan {@code isNull("sekolah") OR eq("sekolah", sekolahTerpilih)}, dan
	 * {@code PengaturanBiayaAction} memakai pola yang sama untuk combo "Pilih Paket". Dengan kata
	 * lain baris ber-{@code sekolah} {@code null} sengaja dibagi ke seluruh sekolah.</p>
	 *
	 * @return sekolah pemilik yang sudah teresolusi, atau {@code null} bila paket berlaku umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik paket, dengan <b>validasi non-trivial</b>: instance yang
	 * {@code null} <i>atau</i> yang belum punya {@code id} (objek transien) disimpan sebagai
	 * {@code null}.
	 *
	 * <p>Tujuannya mencegah Hibernate ikut menyimpan sekolah baru lewat cascade
	 * {@code PERSIST}/{@code MERGE}. Konsekuensinya: menyetel sekolah yang belum tersimpan tidak
	 * menghasilkan error apa pun — paket hanya diam-diam menjadi paket lintas sekolah.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id akan menghasilkan
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik paket — <b>diturunkan ulang dari {@link #getSekolah()} setiap
	 * kali dipanggil</b>.
	 *
	 * <p><b>Getter destruktif.</b> Urutan kerjanya: (1) panggil {@link #getSekolah()} (yang sendiri
	 * menulis balik hasil de-proxy); (2) bila sekolah terisi, <b>timpa</b> field {@code yayasan}
	 * dengan {@code sekolah.getYayasan()}; (3) resolusi proxy lewat {@code check(...)} lalu
	 * kembalikan.</p>
	 *
	 * <p>Akibat yang perlu diketahui:</p>
	 * <ul>
	 * <li>Selama {@code sekolah} terisi, nilai apa pun yang disetel lewat
	 * {@link #setYayasan(Yayasan)} akan <b>hilang</b> pada pembacaan berikutnya. Combo "Yayasan"
	 * pada dialog Tambah/Ubah Paket praktis hanya dekoratif untuk paket yang punya sekolah:
	 * {@code PaketPsbAction#onSave(Event)} memang menyetelnya, tetapi karena Hibernate membaca
	 * nilai kolom lewat getter ini saat flush, yang tersimpan tetap yayasan milik sekolah.</li>
	 * <li>Bila {@code sekolah} {@code null} (paket lintas sekolah), nilai yang disetel
	 * dipertahankan — sehingga paket umum masih bisa dibatasi ke satu yayasan.</li>
	 * <li>Bila data lama menyimpan pasangan sekolah/yayasan yang tidak konsisten, penyimpanan
	 * berikutnya atas baris tersebut akan memperbaikinya diam-diam sekaligus menghasilkan satu
	 * revisi Envers yang seolah-olah "diubah pengguna".</li>
	 * <li>Relasi ini dideklarasikan {@code FetchType.LAZY}, tetapi getter ini membaca
	 * {@code sekolah.getYayasan()} sehingga dua relasi sekaligus ikut termuat pada pembacaan
	 * pertama.</li>
	 * </ul>
	 *
	 * <p>Relasi {@code @ManyToOne} ke kolom {@code yayasan_id} dengan cascade
	 * {@code PERSIST}/{@code MERGE}.</p>
	 *
	 * @return yayasan pemilik yang sudah teresolusi, atau {@code null} bila tidak dapat diturunkan
	 *         maupun disetel
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
	 * Menyetel yayasan pemilik paket, dengan <b>validasi non-trivial</b> yang sama dengan
	 * {@link #setSekolah(Sekolah)}: instance {@code null} atau yang belum punya {@code id}
	 * disimpan sebagai {@code null}, demi mencegah cascade menyimpan yayasan baru.
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini hanya bertahan selama {@link #getSekolah()}
	 * bernilai {@code null}. Lihat {@link #getYayasan()} untuk penjelasan lengkap penurunan
	 * ulangnya.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id akan menghasilkan
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

}
