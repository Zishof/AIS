package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>penjurusan sekolah</b> — katalog jurusan/peminatan yang dapat dipilih atau
 * ditetapkan untuk seorang siswa/calon siswa (mis. "IPA", "IPS", "Bahasa" untuk SMA/MA, atau
 * kompetensi keahlian untuk SMK seperti "TKJ", "Akuntansi", "Multimedia"). Dipetakan ke tabel
 * {@code sekolah.penjurusan_sekolah}.
 *
 * <h3>Domain terverifikasi</h3>
 * <p>Isi entity ini <b>tidak</b> ditebak dari namanya; peran berikut diverifikasi dari kode
 * pemanggil nyata:</p>
 * <ol>
 *   <li><b>Katalog global, bukan milik satu sekolah.</b> Entity ini <b>tidak punya kolom FK
 *   {@code sekolah} sama sekali</b>. Keterkaitan ke sekolah dibuat dari sisi seberang lewat
 *   relasi {@code @ManyToMany} {@code Sekolah.penjurusanSekolahs} (tabel gabung
 *   {@code sekolah.sekolah_punya_penjurusan}, kolom {@code sekolah}/{@code penjurusan}) yang
 *   disunting sebagai daftar checkbox pada layar Sekolah
 *   ({@code ais.action.master.sekolah.SekolahAction}). Jadi satu baris "IPA" DIPAKAI BERSAMA
 *   oleh seluruh sekolah/yayasan dalam satu instalasi — lihat catatan lintas-tenant di bawah.</li>
 *   <li><b>Pilihan jurusan pada formulir PPDB/PSB.</b> Sepuluh varian formulir pendaftaran
 *   ({@code ais.action.master.sekolah.psb.form.PPDB1}, {@code PPDB2}, {@code PPDB_Alumni},
 *   {@code PPDB_Simple} … {@code PPDB_Simple6}) mengisi combobox "Penjurusan" dari
 *   {@code Sekolah.getPenjurusanSekolahs()} dan menyaringnya dengan
 *   {@code o.getAktif() &amp;&amp; o.getTampilkanDiPpdb()}. Combobox hanya muncul bila sekolah
 *   mencentang {@code Sekolah.penjurusanBolehDipilihSaatPsb}.</li>
 *   <li><b>Gerbang batas umur pendaftaran.</b> Empat field ({@link #getDibatasiUmur()},
 *   {@link #getUmurminimal()}, {@link #getUmurmaksimal()}, {@link #getUmurDihitungTanggal()})
 *   menjadi syarat validasi tanggal lahir calon siswa pada formulir PPDB. Ini gerbang KEDUA yang
 *   berdiri sendiri di samping gerbang umur milik {@code GelombangPendaftaranPsb} (field
 *   bernama sama persis di sana) — keduanya dievaluasi berurutan dan keduanya harus lolos.</li>
 *   <li><b>Dimensi penentu tarif keuangan.</b> {@code PengaturanBiaya.penjurusanSekolah}
 *   membuat satu paket biaya berlaku hanya untuk jurusan tertentu; pencocokan tagihan
 *   ({@code PengaturanBiaya.terapkanFilterPembayaran}, {@code Tagihan}, {@code TagihanUtil},
 *   {@code PembayaranOnline}) membandingkan jurusan siswa dengan jurusan pada pengaturan biaya.
 *   Mengubah/menonaktifkan satu baris di sini karenanya BERDAMPAK UANG, bukan sekadar label.</li>
 *   <li><b>Atribut identitas siswa.</b> {@code Siswa.penjurusanSekolah} dan
 *   {@code CalonSiswa.penjurusanSekolah} (kolom {@code penjurusan_sekolah_id}) menyimpan jurusan
 *   yang berlaku; nilainya dicetak di rapor ({@code LaporanRaporSiswa}), laporan PSB
 *   ({@code CommonReportPsb}), pengumuman akademis, banbox pencarian siswa, dan REST
 *   {@code PsbCalonApi}.</li>
 *   <li><b>Dimensi rekap.</b> {@code RekapJalurMasukMultiTahunPsb} memakai
 *   {@code FROM PenjurusanSekolah ORDER BY nama ASC} (tanpa filter apa pun) sebagai kolom
 *   laporan multi-tahun.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Jejak audit (deklarasi ulang dari induk):</b> {@link #getOleh()},
 *   {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, callback
 *   {@code onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)} dan dua konstruktor.</li>
 *   <li><b>Deskriptif:</b> {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 *   <li><b>Sakelar tampil:</b> {@link #getAktif()}/{@link #setAktif(Boolean)},
 *   {@link #getTampilkanDiPpdb()}/{@link #setTampilkanDiPpdb(Boolean)}.</li>
 *   <li><b>Aturan batas umur:</b> {@link #getDibatasiUmur()}/{@link #setDibatasiUmur(Boolean)},
 *   {@link #getUmurminimal()}/{@link #setUmurminimal(Integer)},
 *   {@link #getUmurmaksimal()}/{@link #setUmurmaksimal(Integer)},
 *   {@link #getUmurDihitungTanggal()}/{@link #setUmurDihitungTanggal(Date)}.</li>
 * </ul>
 * <p>Tidak ada method bisnis, method utilitas statis, maupun {@code toString()}/{@code equals()}
 * lokal: seluruh logika penggunaan berada di action/helper pemanggil, sedangkan identitas dan
 * pengurutan diwarisi dari {@link ais.database.model.GeneralValueObject}.</p>
 *
 * <h3>Mengapa {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} dideklarasikan ulang</h3>
 * <p><b>Ini bukan duplikasi yang keliru, melainkan keharusan teknis.</b>
 * {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa — ia <b>bukan</b>
 * {@code @Entity} dan <b>bukan</b> {@code @MappedSuperclass}, sehingga Hibernate sama sekali
 * tidak memetakan properti milik induk. Setiap entity turunan wajib mendeklarasikan sendiri
 * kolom identitas dan kolom jejak auditnya agar kolom tersebut benar-benar ada di tabel.</p>
 *
 * <h3>Perilaku non-obvious yang perlu diketahui</h3>
 * <ul>
 *   <li><b>Getter yang "memalsukan" nilai default.</b> Empat getter tidak mengembalikan isi
 *   kolom apa adanya melainkan nilai pengganti saat kolom {@code NULL}:
 *   {@link #getAktif()} → {@code true}, {@link #getTampilkanDiPpdb()} → {@code true},
 *   {@link #getUmurminimal()} → {@code 0}, {@link #getUmurmaksimal()} → {@code 27},
 *   {@link #getDibatasiUmur()} → {@code false}. Akibatnya baris yang baru disimpan lewat
 *   {@code PenjurusanSekolahAction.onSave()} — yang TIDAK pernah menulis {@code aktif} maupun
 *   {@code tampilkanDiPpdb} — tetap tampil aktif dan tetap muncul di PPDB meski kedua kolom di
 *   basis data masih {@code NULL}. Query SQL pendampingnya konsisten dengan kebiasaan ini
 *   ({@code aktif IS NULL OR aktif = true} baik di {@code PenjurusanSekolahAction.initCriteria()}
 *   maupun di {@code SekolahAction}), sehingga di sini <b>tidak</b> terjadi divergensi
 *   checkbox-vs-SQL seperti pada {@code JenisNilaiSiswa}/{@code JenisCatatanSiswa}.</li>
 *   <li><b>Konsekuensi buruk dari default {@code 27} — konfigurasi yang mati.</b> Karena
 *   {@link #getUmurmaksimal()} TIDAK PERNAH mengembalikan {@code null}, penjaga
 *   {@code if (penjurusanSekolah.getUmurmaksimal() == null) { … }} di
 *   {@code PenjurusanSekolahAction.init(…)} tidak pernah bernilai benar. Nilai konfigurasi
 *   {@code nilai_umur_calon_siswa_dibatasi} yang dibaca tepat di atasnya karenanya
 *   <b>tidak pernah dipakai</b> — batas 27 tahun yang tertanam di getter selalu menang. Hal yang
 *   sama berlaku pada {@code GelombangPendaftaranPsbAction} (getter kembarnya juga memberi
 *   default {@code 27}), padahal layar Konfigurasi menyebut kedua action itu sebagai pemakainya.
 *   Ini varian "penjaga null menjaga hal yang tidak pernah terjadi". Berbahaya diperbaiki
 *   sembarangan: {@code Common.getKonfigurasi} menuliskan nilai default ke basis data saat kunci
 *   belum ada, sehingga instalasi lama sudah terlanjur memiliki baris konfigurasi tersebut.</li>
 *   <li><b>{@link #getKeterangan()} membalik kontrak induk.</b> Versi induk dijamin tidak pernah
 *   mengembalikan {@code null} (mengembalikan {@code ""}); override di sini mengembalikan field
 *   mentah sehingga <b>bisa</b> {@code null}. Efeknya pada
 *   {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)}: cabang terakhir
 *   {@code keterangan} yang menurut dokumentasi induk "selalu terpakai" justru dapat gugur untuk
 *   entity ini. Praktis tidak berdampak karena {@link #getNama()} sudah menyelesaikan
 *   perbandingan lebih dulu, tetapi pemanggil yang merangkai string wajib menyiapkan
 *   {@code null}.</li>
 *   <li><b>{@code TreeSet} pada sisi {@code Sekolah}.</b> {@code Sekolah.penjurusanSekolahs}
 *   adalah {@code TreeSet}, sehingga keanggotaan ditentukan {@code compareTo} (berbasis
 *   {@code nama}), bukan {@code equals} (berbasis {@code id}). Risiko penciutan senyap
 *   ("dua baris berbeda dianggap satu") <b>tertutup selama nama tetap unik</b> — dan memang
 *   {@code PenjurusanSekolahAction.checkNamaPenjurusanSekolah()} menolak nama duplikat. Perlu
 *   diwaspadai satu celah: tombol impor massal {@code Common.uploadData(…)} pada layar yang sama
 *   tidak melewati validasi tersebut, sehingga duplikat nama hasil impor akan membuat sekolah
 *   kehilangan salah satu jurusan dari daftar checkbox tanpa pesan apa pun.</li>
 *   <li><b>Nama wajib unik secara GLOBAL.</b> {@code checkNamaPenjurusanSekolah()} tidak
 *   menyaring per sekolah/yayasan. Berbeda dengan {@code PaketPsb} (di mana keunikan global
 *   merupakan cacat), di sini keunikan global memang <b>konsisten dengan desain</b>: katalognya
 *   sendiri global dan dipakai bersama lintas sekolah.</li>
 *   <li><b>Tidak ada auto-seed.</b> {@code InitData} memang menyebut kelas ini, tetapi
 *   {@code InitDataHelper.initData(…)} hanya melakukan <i>preload</i> seluruh baris ke cache
 *   memori aplikasi saat bootstrap — bukan penyemaian data awal. Instalasi baru dimulai tanpa
 *   satu pun penjurusan; jalur "penjurusan {@code null}" karenanya merupakan jalur yang paling
 *   sering ditempuh dan harus selalu ditangani pemanggil.</li>
 *   <li><b>Cache preload bersifat app-wide.</b> Karena preload di atas tidak dipartisi per
 *   tenant, seluruh baris penjurusan seluruh sekolah berada dalam satu cache proses. Ini
 *   konsisten dengan sifat katalog global entity ini, tetapi memperkuat dampak setiap perubahan
 *   (lihat butir berikutnya).</li>
 *   <li><b>Perubahan berdampak lintas sekolah/yayasan.</b> Layar master
 *   {@code PenjurusanSekolahAction} bergerbang benar ({@code Common.doCheckSecurity()} pada
 *   {@code doBeforeCompose}, plus {@code CommonPrivilages.checkPrevilages} untuk
 *   CREATE/UPDATE/DELETE) — jadi <b>tidak</b> ditemukan pola broken access control seperti pada
 *   sejumlah layar sekolah lain. Namun karena tabelnya global, satu pengguna dengan hak UPDATE
 *   di satu sekolah dapat mengganti nama, menonaktifkan, atau mencabut jurusan dari PPDB untuk
 *   SELURUH sekolah dan yayasan di instalasi yang sama — termasuk memutus pencocokan tarif pada
 *   {@code PengaturanBiaya}. Ini konsekuensi model data, bukan bug otorisasi.</li>
 *   <li><b>Setiap centang memicu revisi Envers.</b> Kelas ber-{@code @Audited} dan kedua
 *   checkbox pada grid ({@code Aktif}, {@code PPDB}) langsung memanggil
 *   {@code Common.refreshSaveOrUpdate(…)} pada event {@code onCheck}, sehingga satu klik
 *   menghasilkan satu revisi audit dan menimpa {@code oleh}/{@code tanggal_dirubah}.</li>
 * </ul>
 *
 * <h3>Kuirk pemanggil yang perlu diketahui saat mengubah entity ini</h3>
 * <ul>
 *   <li><b>Gerbang umur bisa terlewat sepenuhnya.</b> Pada formulir PPDB, blok validasi umur
 *   milik penjurusan hanya berjalan bila combobox penjurusan punya item terpilih. Bila gelombang
 *   pendaftaran sudah menetapkan jurusannya sendiri
 *   ({@code GelombangPendaftaranPsb.penjurusanSekolah != null}), formulir hanya menampilkan
 *   {@code Label} dan combobox tidak pernah diisi — sehingga {@link #getDibatasiUmur()} dan
 *   kawan-kawannya TIDAK PERNAH dievaluasi. Batas umur yang disetel pada jurusan diam-diam tidak
 *   berlaku pada jalur pendaftaran tersebut (hanya batas umur milik gelombang yang tersisa).
 *   Dua varian formulir, {@code PPDB3} dan {@code PPDB_Simple8}, bahkan sama sekali tidak
 *   mengenal penjurusan maupun gerbang umurnya.</li>
 *   <li><b>Semantik "siswa tanpa jurusan" berbeda-beda antar mesin tagihan.</b> Untuk siswa yang
 *   {@code penjurusanSekolah}-nya {@code null}: {@code PengaturanBiaya.buatCriteriaPenjurusan}
 *   memakai {@code 1=1} (semua tarif ikut, termasuk tarif khusus jurusan);
 *   {@code TagihanUtil} baris ~1628 justru membatasi ke {@code penjurusanSekolah IS NULL}
 *   (hanya tarif umum); {@code TagihanUtil} baris ~1736 kembali memakai {@code true} (semua);
 *   sedangkan {@code Tagihan} menolak tarif berjurusan hanya bila sekolah mencentang
 *   {@code penjurusanWajibDipilih}. Akibatnya nominal yang dihitung untuk siswa yang sama dapat
 *   berbeda tergantung layar/jalur yang memanggilnya. Ini pola "dua mesin, dua himpunan aturan"
 *   yang sudah dikenal pada modul diskon.</li>
 *   <li><b>Getter fallback berantai di sisi siswa.</b> {@code Siswa.getPenjurusanSekolah()} dan
 *   {@code CalonSiswa.getPenjurusanSekolah()} menulis balik field-nya sendiri dari sumber lain
 *   (calon siswa ↔ siswa ↔ gelombang) — getter destruktif/write-back. Bila membutuhkan nilai
 *   mentah tanpa fallback, gunakan {@code CalonSiswa.ambilPenjurusanSekolah()}. Entity ini
 *   sendiri <b>bebas</b> dari pola tersebut: seluruh getter di sini murni baca.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.Sekolah
 * @see ais.database.model.sekolah.PengaturanBiaya
 * @see ais.database.model.sekolah.GelombangPendaftaranPsb
 * @see ais.action.master.sekolah.PenjurusanSekolahAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "penjurusan_sekolah", schema = "sekolah")
public class PenjurusanSekolah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar objek yang sudah diserialisasi (mis. ke dalam
	 * session ZK atau cache) tetap kompatibel meski struktur field berubah.
	 */
	private static final long serialVersionUID = 2662544030302108496L;

	/**
	 * Kunci primer baris penjurusan. Dideklarasikan ulang di sini karena kelas induk bukan
	 * {@code @MappedSuperclass}; lihat catatan pada Javadoc kelas.
	 */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interceptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interceptor audit. */
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
	 * {@code null} atau berisi spasi saja diabaikan diam-diam (method langsung {@code return}
	 * tanpa menulis apa pun). Tujuannya agar jejak audit yang sudah ada tidak dapat dihapus atau
	 * dikosongkan oleh pemanggil yang lalai.
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
	 * <p>Perlu disadari bahwa satu klik centang "Aktif" atau "PPDB" pada grid daftar (lihat
	 * {@link #setAktif(Boolean)} dan {@link #setTampilkanDiPpdb(Boolean)}) sudah cukup untuk
	 * memicu jalur ini, menimpa jejak audit, dan menciptakan satu revisi Envers baru.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Normalnya tidak dipanggil kode aplikasi — nilai diisi
	 * saat objek dibuat dan diperbarui otomatis oleh {@code onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor karena field diinisialisasi {@code ais.ui.util.WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama jurusan/peminatan, mis. "IPA", "IPS", "Bahasa", atau nama kompetensi keahlian SMK.
	 * Kolom {@code NOT NULL}; wajib unik secara global (divalidasi layar master, bukan constraint
	 * basis data). Menjadi label combobox di formulir PPDB dan teks yang dicetak di rapor.
	 */
	private String nama;

	/**
	 * Keterangan bebas. Ditampilkan sebagai kolom grid pada layar master dan sebagai
	 * {@code description} (baris kedua) tiap item combobox penjurusan di formulir PPDB.
	 */
	private String keterangan;

	/**
	 * Sakelar aktif. {@code NULL} diperlakukan sebagai aktif oleh {@link #getAktif()} maupun oleh
	 * seluruh query pendampingnya.
	 */
	private Boolean aktif;

	/**
	 * Sakelar "tampilkan di PPDB". Menentukan apakah jurusan ini muncul sebagai pilihan pada
	 * formulir pendaftaran; disaring di sisi Java (bukan SQL) oleh formulir PPDB dan beberapa
	 * layar pengelola PSB. {@code NULL} diperlakukan sebagai tampil.
	 */
	private Boolean tampilkanDiPpdb;

	/**
	 * Sakelar "dibatasi umur". Bila {@code true}, tiga field umur di bawah menjadi syarat
	 * validasi tanggal lahir calon siswa pada formulir PPDB. {@code NULL} berarti tidak dibatasi.
	 */
	private Boolean dibatasiUmur;

	/** Umur maksimal (tahun penuh) calon siswa yang boleh mendaftar pada jurusan ini. */
	private Integer umurmaksimal;

	/** Umur minimal (tahun penuh) calon siswa yang boleh mendaftar pada jurusan ini. */
	private Integer umurminimal;

	/**
	 * Tanggal acuan penghitungan umur. Bila {@code null}, umur dihitung terhadap tanggal saat
	 * calon siswa mengisi formulir (lihat {@link #getUmurDihitungTanggal()}).
	 */
	private Date umurDihitungTanggal;

	/**
	 * Konstruktor kosong wajib bagi Hibernate dan bagi layar master saat menekan tombol "Tambah"
	 * ({@code PenjurusanSekolahAction.onAdd(…)}). Seluruh field dibiarkan {@code null} kecuali
	 * {@code tanggal_dirubah} yang langsung diisi waktu saat ini.
	 */
	public PenjurusanSekolah() {
	}

	/**
	 * Konstruktor ringkas berisi kolom wajib saja. Warisan pembangkitan hbm2java; tidak ditemukan
	 * pemanggil di dalam kode aplikasi — praktis hanya berguna untuk pengujian atau pembuatan
	 * objek sementara.
	 *
	 * @param id   kunci primer yang ingin dipasang (perhatikan kolom {@code id} tidak
	 *             {@code insertable}, jadi nilai ini tidak akan dikirim saat INSERT)
	 * @param nama nama jurusan
	 */
	public PenjurusanSekolah(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Strategi {@code IDENTITY} membuat nilainya berurutan dan mudah ditebak; kolom sengaja
	 * dideklarasikan {@code insertable = false} sehingga basis data yang menentukan nilainya saat
	 * INSERT. Layar master memakai {@code getId() == null} sebagai penanda "baris baru" untuk
	 * memilih judul dialog serta melewatkan {@code session.load(…)} pada {@code onSave()}.</p>
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
	 * Menyetel kunci primer. Hanya dipakai Hibernate dan konstruktor; kode aplikasi tidak boleh
	 * mengubah id baris yang sudah tersimpan.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jurusan.
	 *
	 * <p>Ini <b>override</b> atas {@code getNama()} milik kelas induk dan membaca field lokal
	 * {@link #nama}, bukan field induk. Karena
	 * {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)} memanggil
	 * {@code getNama()} secara virtual, nilai inilah yang menentukan urutan dan — pada
	 * {@code TreeSet Sekolah.penjurusanSekolahs} — juga menentukan keanggotaan himpunan.</p>
	 *
	 * <p>Getter ini murni baca: tidak ada penulisan balik maupun efek samping (kontras dengan
	 * {@code getNama()} destruktif yang pernah ditemukan pada {@code KelasSiswaPSB}).</p>
	 *
	 * @return nama jurusan; secara praktis tidak pernah {@code null} untuk baris tersimpan karena
	 *         kolom {@code NOT NULL} dan layar master menolak nama kosong
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama jurusan.
	 *
	 * <p><b>Efek samping tidak langsung:</b> mengubah nama berarti mengubah label yang sudah
	 * terpakai di seluruh sekolah pada instalasi ini (katalog global), dan — karena keanggotaan
	 * {@code TreeSet} di {@code Sekolah} ditentukan {@code compareTo} berbasis nama — dapat
	 * mengubah posisi/keanggotaan objek pada koleksi yang sedang hidup di memori.</p>
	 *
	 * @param nama nama jurusan; tidak divalidasi di sini (validasi wajib-isi dan keunikan
	 *             dilakukan {@code PenjurusanSekolahAction.onSave()})
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas.
	 *
	 * <p><b>Membalik kontrak induk.</b> {@code getKeterangan()} pada
	 * {@link ais.database.model.GeneralValueObject} dijamin tidak pernah mengembalikan
	 * {@code null} (mengembalikan {@code ""}); override ini mengembalikan field mentah sehingga
	 * {@code null} mungkin terjadi. Pemanggil yang merangkai string harus menyiapkan {@code null},
	 * dan cabang terakhir {@code keterangan} pada {@code compareTo} kelas induk dapat gugur untuk
	 * entity ini.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan teks keterangan; {@code null} dan string kosong diterima apa adanya
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif, dengan <b>default {@code true} bila kolom {@code NULL}</b>.
	 *
	 * <p>Ini penting karena {@code PenjurusanSekolahAction.onSave()} tidak pernah menulis kolom
	 * {@code aktif}: setiap jurusan yang baru dibuat menyimpan {@code NULL} namun tetap tampil
	 * aktif. Query pendampingnya konsisten dengan kebiasaan tersebut — baik
	 * {@code PenjurusanSekolahAction.initCriteria()} maupun {@code SekolahAction} memakai
	 * {@code aktif IS NULL OR aktif = true} — sehingga di sini tidak terjadi divergensi
	 * "checkbox terlihat tercentang tapi baris tak pernah muncul di daftar".</p>
	 *
	 * @return {@code true} bila jurusan aktif atau kolom masih {@code NULL}; {@code false} hanya
	 *         bila secara eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif.
	 *
	 * <p>Dipanggil dari event {@code onCheck} checkbox "Aktif" pada grid layar master, yang
	 * langsung diikuti {@code Common.refreshSaveOrUpdate(…)} — jadi satu klik langsung tersimpan,
	 * memicu {@code onUpdate()}, dan menciptakan satu revisi Envers. Checkbox dinonaktifkan bagi
	 * pengguna tanpa hak UPDATE.</p>
	 *
	 * <p><b>Dampak luas:</b> menonaktifkan satu jurusan menghilangkannya dari daftar checkbox di
	 * layar Sekolah dan dari combobox PPDB untuk SEMUA sekolah pada instalasi ini, karena
	 * katalognya global.</p>
	 *
	 * @param aktif {@code true} aktif, {@code false} nonaktif, {@code null} diperlakukan aktif
	 *              saat dibaca kembali
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan apakah jurusan ini boleh ditampilkan pada formulir PPDB, dengan
	 * <b>default {@code true} bila kolom {@code NULL}</b>.
	 *
	 * <p>Penyaringannya dilakukan di sisi Java setelah data dimuat — pola
	 * {@code if (o.getAktif() && o.getTampilkanDiPpdb())} — bukan lewat SQL, sehingga default
	 * pada getter inilah yang benar-benar menentukan hasilnya. Pemakainya: sepuluh varian
	 * formulir {@code PPDB*}, serta layar {@code CalonSiswaAction},
	 * {@code GelombangPendaftaranPsbAction}, dan {@code KelompokPendaftaranPsbAction}.</p>
	 *
	 * @return {@code true} bila jurusan boleh muncul di PPDB atau kolom masih {@code NULL}
	 */
	public Boolean getTampilkanDiPpdb() {
		return tampilkanDiPpdb == null ? true : tampilkanDiPpdb;
	}

	/**
	 * Menyetel apakah jurusan ini ditampilkan pada formulir PPDB.
	 *
	 * <p>Sama seperti {@link #setAktif(Boolean)}: dipanggil dari event {@code onCheck} checkbox
	 * "PPDB" pada grid dan langsung disimpan lewat {@code Common.refreshSaveOrUpdate(…)}.
	 * Mematikannya menyembunyikan jurusan dari pendaftar baru <b>tanpa</b> memengaruhi siswa yang
	 * sudah terlanjur memakainya maupun pencocokan tarif {@code PengaturanBiaya}.</p>
	 *
	 * @param tampilkanDiPpdb {@code true} tampil, {@code false} sembunyi, {@code null}
	 *                        diperlakukan tampil saat dibaca kembali
	 */
	public void setTampilkanDiPpdb(Boolean tampilkanDiPpdb) {
		this.tampilkanDiPpdb = tampilkanDiPpdb;
	}

	/**
	 * Mengembalikan batas umur minimal calon siswa, dengan <b>default {@code 0} bila kolom
	 * {@code NULL}</b> (artinya: tanpa batas bawah).
	 *
	 * <p>Dipakai formulir PPDB hanya bila {@link #getDibatasiUmur()} bernilai {@code true}; nilai
	 * dibandingkan dengan selisih tahun penuh antara tanggal lahir dan tanggal acuan
	 * ({@link #getUmurDihitungTanggal()} atau tanggal hari ini). Calon dengan umur
	 * <b>kurang dari</b> nilai ini ditolak beserta pesan peringatan.</p>
	 *
	 * @return batas umur minimal dalam tahun; {@code 0} bila belum disetel
	 */
	public Integer getUmurminimal() {
		return umurminimal == null ? 0 : umurminimal;
	}

	/**
	 * Menyetel batas umur minimal calon siswa.
	 *
	 * <p>Ditulis {@code PenjurusanSekolahAction.onSave()} dari {@code Intbox} "Umur Minimal".
	 * Perhatikan bahwa nilainya disimpan <b>walaupun</b> {@code dibatasiUmur} tidak dicentang —
	 * kolom tetap terisi tetapi tidak dievaluasi.</p>
	 *
	 * @param umurminimal batas umur minimal dalam tahun; {@code null} berarti tanpa batas bawah
	 */
	public void setUmurminimal(Integer umurminimal) {
		this.umurminimal = umurminimal;
	}

	/**
	 * Mengembalikan batas umur maksimal calon siswa, dengan <b>default {@code 27} bila kolom
	 * {@code NULL}</b>.
	 *
	 * <p><b>Efek samping penting dari default ini.</b> Karena method tidak pernah mengembalikan
	 * {@code null}, penjaga {@code if (…getUmurmaksimal() == null)} di
	 * {@code PenjurusanSekolahAction.init(…)} tidak pernah terpenuhi. Akibatnya nilai konfigurasi
	 * {@code nilai_umur_calon_siswa_dibatasi} yang dibaca tepat sebelum penjaga tersebut
	 * <b>tidak pernah dipakai</b>, dan angka 27 yang tertanam di sini selalu menang. Pola serupa
	 * terjadi pada {@code GelombangPendaftaranPsb.getUmurmaksimal()}, sehingga konfigurasi
	 * tersebut praktis mati di KEDUA tempat yang diklaim memakainya pada layar Konfigurasi.
	 * Angka 27 sendiri jelas warisan modul perguruan tinggi (batas umur calon mahasiswa) dan
	 * tidak masuk akal sebagai batas bawaan untuk jenjang sekolah.</p>
	 *
	 * <p>Calon dengan umur <b>lebih dari</b> nilai ini ditolak formulir PPDB beserta pesan
	 * peringatan.</p>
	 *
	 * @return batas umur maksimal dalam tahun; {@code 27} bila belum disetel
	 */
	public Integer getUmurmaksimal() {
		return umurmaksimal == null ? 27 : umurmaksimal;
	}

	/**
	 * Menyetel batas umur maksimal calon siswa.
	 *
	 * <p>Ditulis {@code PenjurusanSekolahAction.onSave()} dari {@code Intbox} "Umur Maksimal".
	 * Menyetel {@code null} tidak berarti "tanpa batas atas" melainkan mengembalikan nilai baca
	 * ke default {@code 27} — lihat {@link #getUmurmaksimal()}.</p>
	 *
	 * @param umurmaksimal batas umur maksimal dalam tahun
	 */
	public void setUmurmaksimal(Integer umurmaksimal) {
		this.umurmaksimal = umurmaksimal;
	}

	/**
	 * Mengembalikan apakah pendaftaran pada jurusan ini dibatasi umur, dengan <b>default
	 * {@code false} bila kolom {@code NULL}</b> — jadi instalasi lama yang belum pernah menyentuh
	 * kolom ini otomatis tidak menerapkan batas umur (aman secara fungsional).
	 *
	 * <p>Berfungsi sebagai gerbang tunggal bagi ketiga field umur lainnya: bila {@code false},
	 * formulir PPDB melewati seluruh blok validasi umur dan menyembunyikan ketiga isian umur pada
	 * layar master.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> gerbang ini hanya dievaluasi bila combobox penjurusan
	 * pada formulir PPDB benar-benar punya item terpilih. Bila gelombang pendaftaran sudah
	 * menetapkan jurusannya sendiri, combobox tidak pernah diisi sehingga batas umur pada jurusan
	 * <b>tidak pernah diperiksa</b> pada jalur pendaftaran tersebut.</p>
	 *
	 * @return {@code true} bila batas umur diberlakukan
	 */
	public Boolean getDibatasiUmur() {
		return dibatasiUmur == null ? false : dibatasiUmur;
	}

	/**
	 * Menyetel apakah pendaftaran pada jurusan ini dibatasi umur.
	 *
	 * <p>Ditulis {@code PenjurusanSekolahAction.onSave()} dari checkbox "Dibatasi Umur", yang
	 * pada layar master juga mengendalikan tampil/sembunyinya ketiga isian umur secara langsung
	 * lewat listener {@code onClick}.</p>
	 *
	 * @param dibatasiUmur {@code true} untuk memberlakukan batas umur; {@code null} diperlakukan
	 *                     {@code false} saat dibaca kembali
	 */
	public void setDibatasiUmur(Boolean dibatasiUmur) {
		this.dibatasiUmur = dibatasiUmur;
	}

	/**
	 * Mengembalikan tanggal acuan penghitungan umur (presisi {@code DATE}, tanpa jam).
	 *
	 * <p>Berbeda dengan tiga field umur lainnya, getter ini <b>tidak</b> memberi nilai pengganti:
	 * {@code null} dikembalikan apa adanya dan memang bermakna. Formulir PPDB menerjemahkannya
	 * sebagai "umur dihitung saat calon siswa melakukan pendaftaran" (memakai
	 * {@code ais.ui.util.WaktuUtil.getDate()}); layar master menegaskan hal ini lewat petunjuk
	 * "Kosongkan tanggal apabila umur dihitung saat melakukan pendaftaran".</p>
	 *
	 * <p>Mengisinya membuat batas umur menjadi <b>tetap</b> terhadap satu tanggal patokan (mis.
	 * 1 Juli awal tahun ajaran), sehingga hasil validasi tidak bergeser selama masa pendaftaran
	 * berlangsung.</p>
	 *
	 * @return tanggal acuan, atau {@code null} bila umur dihitung terhadap tanggal pendaftaran
	 */
	@Temporal(TemporalType.DATE)
	public Date getUmurDihitungTanggal() {
		return umurDihitungTanggal;
	}

	/**
	 * Menyetel tanggal acuan penghitungan umur.
	 *
	 * <p>Ditulis {@code PenjurusanSekolahAction.onSave()} dari {@code MyDatebox} "Umur dihitung
	 * saat tanggal". Mengosongkannya ({@code null}) mengembalikan perilaku ke penghitungan
	 * relatif terhadap tanggal pendaftaran.</p>
	 *
	 * @param umurDihitungTanggal tanggal patokan, atau {@code null} untuk memakai tanggal
	 *                            pendaftaran
	 */
	public void setUmurDihitungTanggal(Date umurDihitungTanggal) {
		this.umurDihitungTanggal = umurDihitungTanggal;
	}
}
