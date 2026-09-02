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

import org.hibernate.envers.Audited;

/**
 * Entity <b>jejak cetak dokumen akademik</b> (tabel {@code public.meta_report}): satu baris =
 * satu dokumen resmi yang pernah dicetak/di-generate, menyimpan <i>salinan teks</i> dari
 * nilai-nilai yang tercetak di dokumen itu (nama, NIM, IPK, yudisium, fakultas, program studi,
 * jumlah matakuliah, penanda tangan, tanggal cetak, jenis dokumen) beserta {@link #getBarcode()
 * kode barcode} yang dicetak di dokumen sebagai kunci pencariannya.
 *
 * <h2>Peran dalam sistem pelaporan</h2>
 * <p><b>Koreksi premis yang mudah keliru:</b> meskipun namanya "MetaReport", entity ini
 * <b>BUKAN</b> registry definisi laporan JasperReports. Tidak ada satu pun kolom di sini yang
 * berisi nama berkas {@code .jrxml}, daftar parameter laporan, apalagi query SQL. Yang disimpan
 * adalah <i>hasil</i> laporan, bukan <i>definisi</i>-nya — sebuah snapshot pipih (semuanya
 * {@code String}) dari apa yang tercetak, supaya nanti bisa dicocokkan ulang dengan dokumen
 * fisik. Definisi laporan sesungguhnya hidup sebagai berkas {@code .jrxml} di
 * {@code webapp/WEB-INF/report/} dan kelas-kelas {@code ais.action.report.*} yang menyusun
 * {@code parameters} Jasper (mis. {@code ais.action.report.format1.akademik.LaporanIjazahAkademik},
 * {@code LaporanRekamanNilai}, {@code LaporanPrestasiMahasiswa}) — bukan di tabel ini.</p>
 *
 * <p>Fungsi yang dituju jelas: <b>verifikasi keaslian dokumen</b>. Pemeriksa (mis. bagian
 * akademik atau instansi yang menerima ijazah/transkrip) memindai barcode pada lembar dokumen,
 * sistem mencari barisnya di sini, lalu menampilkan seluruh nilai yang tersimpan agar bisa
 * dibandingkan dengan yang tercetak. Selisih apa pun (IPK diubah, nama diganti, prodi
 * dipalsukan) langsung terlihat.</p>
 *
 * <h2>Siapa yang membaca, siapa yang menulis</h2>
 * <ul>
 *   <li><b>Pembaca — satu-satunya:</b> {@code ais.action.master.helper.CekMetaReportHelper}
 *   (composer ZK untuk {@code webapp/WEB-INF/z/x/y/pages/master/cek_meta_report.zul}). Method
 *   {@code onCari()} mencari <i>satu</i> baris dengan
 *   {@code Restrictions.ilike("barcode", input.trim(), MatchMode.EXACT)} lalu memasang tiap
 *   nilai ke label layar. Operasi murni baca; tidak ada penulisan.</li>
 *   <li><b>Penulis — TIDAK ADA.</b> Penelusuran seluruh pohon sumber (per 3 Sep 2026)
 *   menunjukkan kata "MetaReport" hanya muncul di entity ini, trio DAO-nya
 *   ({@code MetaReportDao}, {@code MetaReportDaoImpl},
 *   {@code DaoFactory#getMetaReportDao()}/{@code HibernateDaoFactory}) dan
 *   {@code CekMetaReportHelper}. <b>Tidak ada satu baris kode pun yang memanggil
 *   {@code new MetaReport()} lalu menyimpannya</b>, dan tidak ada kelas {@code Laporan*} yang
 *   mencatat barcode dokumen ke sini setelah mencetak. Konsekuensinya: pada basis data yang
 *   diisi murni oleh aplikasi ini, tabel {@code meta_report} tetap kosong dan layar verifikasi
 *   selalu menjawab <i>"Barcode tidak ditemukan / tidak valid"</i>. Baris hanya bisa muncul dari
 *   luar aplikasi (skrip SQL, migrasi, atau modul lain di luar pohon sumber ini).</li>
 *   <li><b>Trio DAO tidak terpakai:</b> {@code getMetaReportDao()} terdaftar di
 *   {@code DaoFactory}/{@code HibernateDaoFactory} tetapi tidak pernah dipanggil dari mana pun —
 *   sisa <i>boilerplate</i> generator DAO, bukan jalur hidup.</li>
 *   <li><b>Jalur reflektif:</b> entity ini terdaftar di {@code hibernate.cfg.xml}
 *   ({@code <mapping class="ais.database.model.MetaReport"/>}), sehingga tetap terjangkau oleh
 *   endpoint generik berbasis nama kelas ({@code /Data}, {@code /Api}) seperti seluruh entity
 *   terpetakan lain, terlepas dari ada/tidaknya layar UI yang aktif.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Identitas &amp; audit warisan</b> — {@link #getId()}, {@link #getOleh()},
 *   {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Snapshot isi dokumen</b> — {@link #getNama()}, {@link #getNim()}, {@link #getIpk()},
 *   {@link #getYudisium()}, {@link #getFakultas()}, {@link #getProdi()},
 *   {@link #getJumlahMk()}, {@link #getPenandaTangan()}, {@link #getTanggalCetak()}.</li>
 *   <li><b>Kunci verifikasi &amp; klasifikasi</b> — {@link #getBarcode()},
 *   {@link #getJenis_report()}.</li>
 *   <li><b>Representasi</b> — {@link #toString()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, tidak ada method query statis, dan tidak ada relasi
 * {@code @ManyToOne}/{@code @OneToMany} sama sekali: kelas ini benar-benar hanya kantong data.
 * Karena tidak ada relasi, tidak ada pula pemanggilan {@code check(...)}/{@code resolveLazy(...)}
 * warisan {@link GeneralValueObject} di sini.</p>
 *
 * <h2>Hal non-obvious yang perlu diketahui sebelum menyunting</h2>
 * <ul>
 *   <li><b>Semua kolom bertipe teks</b>, termasuk yang secara alami numerik/tanggal:
 *   {@code ipk}, {@code jumlah_mk}, dan {@code tgl_cetak} disimpan sebagai {@code varchar(255)}.
 *   Ini konsisten dengan tujuannya (menyimpan <i>persis</i> string yang tercetak, apa adanya,
 *   termasuk format lokal seperti "3,45" atau "12 Agustus 2019"), tapi berarti kolom-kolom ini
 *   tidak bisa dibandingkan/diagregasi secara numerik maupun diurutkan secara kronologis di
 *   level SQL.</li>
 *   <li><b>Hampir semua kolom {@code nullable = false} tanpa nilai default.</b> Hanya
 *   {@code penanda_tangan} yang boleh {@code null}. Karena tidak ada kode penulis, setiap upaya
 *   menyisipkan baris lewat jalur generik (CRUD dinamis, {@code /Data}, skrip) yang membiarkan
 *   salah satu dari {@code nama}/{@code nim}/{@code ipk}/{@code yudisium}/{@code fakultas}/
 *   {@code prodi}/{@code jumlah_mk}/{@code barcode}/{@code tgl_cetak}/{@code jenis_report}
 *   kosong akan gagal di level basis data.</li>
 *   <li><b>{@code barcode} tidak punya {@code unique constraint}</b> padahal pembacanya memakai
 *   {@code uniqueResult()}. Dua dokumen dengan barcode sama (mis. cetak ulang) membuat layar
 *   verifikasi melempar {@code NonUniqueResultException}, bukan menampilkan hasil.</li>
 *   <li><b>Anotasi kelas berpasangan:</b> {@code @Entity} JPA + {@code @org.hibernate.annotations.Entity}
 *   Hibernate lama dengan {@code dynamicInsert}/{@code dynamicUpdate} = {@code true} (SQL hanya
 *   memuat kolom yang benar-benar berubah), dan {@code @Audited} Envers sehingga setiap versi
 *   baris tersalin ke tabel revisi {@code meta_report_aud}. Perhatikan implikasi privasinya:
 *   data yang tersimpan di sini adalah PII lulusan (nama, NIM, IPK, predikat yudisium) dan tetap
 *   ada di tabel audit meski baris aslinya dihapus.</li>
 *   <li><b>Akses properti, bukan field:</b> {@code @Id} dipasang pada {@link #getId()}, jadi
 *   Hibernate membaca/menulis seluruh state lewat getter/setter. Getter apa pun yang mengubah
 *   state karena itu akan ikut terbaca oleh <i>dirty checking</i>. Di kelas ini <b>tidak ada</b>
 *   getter semacam itu (lihat "Verifikasi pola berulang" di bawah).</li>
 *   <li><b>Komentar generator yang salah warisan:</b> Javadoc asli berkas ini berbunyi
 *   "Bank generated by hbm2java" — sisa salin-tempel dari
 *   {@link ais.database.model.Bank}, bukan indikasi hubungan apa pun dengan modul bank. Komentar
 *   itu digantikan oleh dokumentasi ini.</li>
 * </ul>
 *
 * <h2>Verifikasi pola berulang (diperiksa langsung dari kode berkas ini)</h2>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field/DB:</b> <b>TIDAK ADA</b>. {@link #getNama()}
 *   memang memanggil {@code trim()}, tetapi hasilnya dikembalikan <i>tanpa</i> disimpan kembali
 *   ke field, sehingga tidak memicu UPDATE hantu. Semua getter lain mengembalikan field apa
 *   adanya.</li>
 *   <li><b>Getter yang menutup sesi Hibernate:</b> <b>TIDAK ADA</b> — kelas ini tidak menyentuh
 *   {@code HibernateUtil}/{@code Session} sama sekali.</li>
 *   <li><b>Getter destruktif</b> (menghapus/menetralkan data saat dibaca): <b>TIDAK ADA</b>.</li>
 *   <li><b>Setter yang menolak masukan secara senyap:</b> <b>ADA</b> —
 *   {@link #setOleh(String)} dan {@link #setOlehId(String)} mengabaikan {@code null}/string
 *   kosong tanpa pesan apa pun (pola audit standar AIS, lihat method masing-masing).</li>
 * </ul>
 *
 * <h2>Catatan tentang {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * hanya POJO abstrak biasa — sehingga Hibernate tidak memetakan properti apa pun miliknya.
 * Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi keliru melainkan keharusan teknis</b>:
 * tanpa deklarasi ulang tersebut kolom-kolom itu tidak akan terpetakan sama sekali. Hal yang sama
 * berlaku untuk {@code nama} dan {@code nim}: keduanya dideklarasikan ulang di sini (menutupi
 * field privat bernama sama di kelas induk) agar bisa diberi {@code @Column}. Efek samping yang
 * perlu diingat: field {@code nama}/{@code nim} milik induk tetap {@code null} selamanya, dan
 * properti warisan lain yang <b>tidak</b> dideklarasikan ulang — {@code kode},
 * {@code keterangan}, {@code nomorUrut} — memang <b>tidak tersimpan</b> ke tabel ini walau
 * setter-nya tetap bisa dipanggil dari kode.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.action.master.helper.CekMetaReportHelper
 * @see ais.database.dao.MetaReportDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "meta_report")

public class MetaReport extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan generator dan dipertahankan apa adanya:
	 * mengubahnya akan mematahkan deserialisasi objek {@code MetaReport} yang sudah terlanjur
	 * tersimpan di sesi ZK atau cache berkas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris, kolom {@code id} ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;
	/** Nama tampil pengguna terakhir yang meng-UPDATE baris. Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID login pengguna terakhir yang meng-UPDATE baris. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID login (user id) pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()} lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} saat UPDATE. Karena tidak ada callback
	 * {@code @PrePersist}, nilai ini <b>kosong pada baris yang belum pernah di-update</b>: ia
	 * mencatat pengubah, bukan pembuat.</p>
	 *
	 * @return ID login pengubah terakhir, atau {@code null} bila baris belum pernah di-update.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID login pengubah terakhir.
	 *
	 * <p><b>Perhatikan penjaga di awal baris:</b> bila {@code olehId} bernilai {@code null} atau
	 * hanya berisi spasi, method langsung {@code return} dan nilai lama <b>dipertahankan</b> —
	 * penolakan berlangsung senyap, tanpa exception maupun log. Pola ini seragam di seluruh
	 * entity AIS dan bertujuan mencegah jejak audit yang sudah terisi tertimpa nilai kosong oleh
	 * jalur penyimpanan yang tidak membawa konteks pengguna.</p>
	 *
	 * @param olehId ID login pengubah; {@code null}/kosong diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampil pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: masukan {@code null} atau berisi spasi saja
	 * diabaikan secara senyap sehingga nilai audit sebelumnya tidak hilang.</p>
	 *
	 * @param oleh nama tampil pengubah; {@code null}/kosong diabaikan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sepasang dengan {@link #getOlehId()} dan mengikuti keterbatasan yang sama (tidak
	 * mencatat pembuat baris). Karena tidak ada layar CRUD untuk entity ini, dalam praktiknya
	 * kolom ini akan tetap kosong kecuali baris diubah lewat jalur generik/reflektif.</p>
	 *
	 * @return nama tampil pengubah terakhir, atau {@code null} bila belum pernah di-update.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Implementasi wajib atas satu-satunya method {@code abstract} di
	 * {@link GeneralValueObject}. Tidak ada padanan {@code @PrePersist}, jadi <i>pembuat</i>
	 * baris tidak pernah tercatat pada kolom-kolom ini.
	 *
	 * <p>Karena entity ini tidak punya getter yang menulis balik ke field terpetakan, callback
	 * ini hanya berjalan pada perubahan yang memang diniatkan — berbeda dari beberapa entity AIS
	 * lain yang jejak auditnya bisa tercemar oleh UPDATE hasil operasi baca.</p>
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field {@code tanggal_dirubah}, yang
	 * diinisialisasi ke waktu server saat objek dibuat ({@code ais.ui.util.WaktuUtil.getDate()})
	 * sehingga baris baru tetap punya stempel waktu meski belum pernah di-update.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Dipanggil {@code AuditTimestampInterceptor} dari
	 * {@link #onUpdate()}; tidak ada validasi, nilai {@code null} pun diterima.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * tipe {@code TIMESTAMP}).
	 *
	 * <p>Nilainya sudah terisi sejak objek dibangun (waktu server), sehingga tidak bisa dipakai
	 * untuk membedakan "belum pernah diubah" dari "baru saja diubah" — gunakan
	 * {@link #getOlehId()} yang memang {@code null} sebelum UPDATE pertama.</p>
	 *
	 * <p><b>Catatan penting:</b> stempel ini mencatat kapan <i>baris metadata</i> berubah, bukan
	 * kapan dokumennya dicetak. Waktu cetak dokumen ada di {@link #getTanggalCetak()} yang
	 * bertipe teks dan diisi terpisah.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang dibuat lewat
	 *         constructor kelas ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks objek: mengembalikan <b>langsung isi field {@code nama}</b>, bukan format
	 * {@code "kode - nama"} bawaan {@link GeneralValueObject#toString()}.
	 *
	 * <p>Dua konsekuensi yang mudah terlewat:</p>
	 * <ul>
	 *   <li>Method ini membaca field, bukan {@link #getNama()}, sehingga <b>tidak</b> ikut
	 *   di-{@code trim()} — nilai yang tampil bisa berbeda (mengandung spasi tepi) dari yang
	 *   dikembalikan getter-nya.</li>
	 *   <li>Nilainya bisa {@code null}. Berbeda dari implementasi induk yang selalu menghasilkan
	 *   string, method ini mengembalikan {@code null} apa adanya bila {@code nama} belum diisi,
	 *   sehingga pemanggil yang merangkai string bisa mendapat {@code "null"} atau melempar NPE
	 *   pada operasi yang tidak toleran {@code null}.</li>
	 * </ul>
	 *
	 * @return isi field {@code nama} apa adanya, mungkin {@code null}.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Nama pemilik dokumen sebagaimana tercetak. Menutupi field {@code nama} milik
	 * {@link GeneralValueObject} agar bisa dipetakan sebagai kolom. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * NIM pemilik dokumen sebagaimana tercetak. Menutupi field {@code nim} milik
	 * {@link GeneralValueObject}. Disimpan sebagai teks, tanpa FK ke {@code Mahasiswa}.
	 */
	private String nim;
	/** IPK sebagaimana tercetak, sebagai teks (mis. {@code "3,45"}). Lihat {@link #getIpk()}. */
	private String ipk;
	/** Predikat yudisium sebagaimana tercetak (mis. {@code "Cum Laude"}). */
	private String yudisium;
	/** Nama fakultas sebagaimana tercetak; salinan teks, bukan FK ke {@code Fakultas}. */
	private String fakultas;
	/** Nama program studi sebagaimana tercetak; salinan teks, bukan FK ke {@code Jurusan}. */
	private String prodi;
	/** Jumlah matakuliah yang tercetak pada dokumen, sebagai teks. Lihat {@link #getJumlahMk()}. */
	private String jumlahMk;
	/** Kode barcode yang tercetak pada dokumen; satu-satunya kunci pencarian verifikasi. */
	private String barcode;
	/** Nama pejabat penanda tangan dokumen; satu-satunya kolom yang boleh {@code null}. */
	private String penandaTangan;
	/** Tanggal cetak dokumen sebagaimana tercetak, sebagai teks bebas format. */
	private String tanggalCetak;
	/** Jenis dokumen (mis. transkrip/ijazah/rekaman nilai), sebagai teks bebas. */
	private String jenis_report;

	/**
	 * Constructor default tanpa argumen. Wajib ada karena Hibernate membutuhkannya untuk
	 * menghidrasi entity dari hasil query. Tidak menyetel apa pun kecuali inisialisasi
	 * {@code tanggal_dirubah} ke waktu server yang dilakukan pada deklarasi field.
	 */
	public MetaReport() {
	}

	/**
	 * Mengembalikan primary key baris.
	 *
	 * <p>Kolom {@code id} memakai strategi {@code IDENTITY} (nilai dibangkitkan basis data) dan
	 * ditandai {@code insertable = false} sehingga tidak pernah ikut disertakan pada
	 * {@code INSERT} — nilai apa pun yang disetel manual lewat {@link #setId(Long)} sebelum
	 * penyimpanan akan diabaikan. Karena {@code @Id} dipasang di getter ini, seluruh pemetaan
	 * kelas memakai <i>property access</i>.</p>
	 *
	 * @return primary key, atau {@code null} untuk objek yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Umumnya hanya dipanggil Hibernate saat hidrasi; penyetelan manual
	 * tidak berpengaruh pada {@code INSERT} (lihat {@link #getId()}).
	 *
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama pemilik dokumen sebagaimana tercetak, <b>sudah di-{@code trim()}</b>.
	 *
	 * <p>Meng-override {@link GeneralValueObject#getNama()} dan memetakan kolom {@code nama}
	 * ({@code NOT NULL}, panjang 255). Pemangkasan hanya berlaku pada nilai yang dikembalikan —
	 * field maupun isi basis data tidak ikut diubah, sehingga getter ini <b>bukan</b> getter
	 * yang menulis balik. Perhatikan bahwa {@link #toString()} membaca field secara langsung dan
	 * karenanya tidak ikut memangkas.</p>
	 *
	 * @return nama pemilik dokumen tanpa spasi tepi, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama pemilik dokumen. Disimpan apa adanya tanpa pemangkasan maupun validasi;
	 * pemangkasan baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama sebagaimana tercetak di dokumen.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan NIM pemilik dokumen sebagaimana tercetak (kolom {@code nim}, {@code NOT NULL}).
	 *
	 * <p>Meng-override {@link GeneralValueObject#getNim()}. Berbeda dari {@link #getNama()},
	 * nilai dikembalikan apa adanya <b>tanpa {@code trim()}</b> — asimetri yang perlu diingat
	 * bila membandingkan NIM di sini dengan {@code Mahasiswa.nim}. Tidak ada foreign key ke
	 * {@code Mahasiswa}: kaitannya murni berbasis teks, jadi baris di sini tetap utuh (dan tetap
	 * bisa diverifikasi) meski data mahasiswanya kelak dihapus.</p>
	 *
	 * @return NIM sebagaimana tercetak, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nim", nullable = false, length = 255)
	public String getNim() {
		return nim;
	}

	/**
	 * Menyetel NIM pemilik dokumen. Tanpa validasi format maupun pengecekan keberadaan mahasiswa.
	 *
	 * @param nim NIM sebagaimana tercetak di dokumen.
	 */
	public void setNim(String nim) {
		this.nim = nim;
	}

	/**
	 * Mengembalikan IPK yang tercetak di dokumen (kolom {@code ipk}, {@code NOT NULL}).
	 *
	 * <p>Bertipe teks, bukan numerik: yang disimpan adalah string persis seperti yang tercetak,
	 * termasuk pemisah desimal lokal ("3,45") dan jumlah angka di belakang koma. Jangan
	 * mengurutkan atau membandingkan kolom ini secara numerik di level SQL.</p>
	 *
	 * @return IPK sebagai teks, atau {@code null} bila belum diisi.
	 */
	@Column(name = "ipk", nullable = false, length = 255)
	public String getIpk() {
		return ipk;
	}

	/**
	 * Menyetel IPK yang tercetak. Tanpa validasi bahwa isinya berupa angka yang sah.
	 *
	 * @param ipk IPK sebagai teks, sesuai yang tercetak.
	 */
	public void setIpk(String ipk) {
		this.ipk = ipk;
	}

	/**
	 * Mengembalikan predikat yudisium yang tercetak (kolom {@code yudisium}, {@code NOT NULL}),
	 * mis. {@code "Dengan Pujian"}/{@code "Cum Laude"}/{@code "Memuaskan"}. Teks bebas, tidak
	 * terikat master mana pun.
	 *
	 * @return predikat yudisium, atau {@code null} bila belum diisi.
	 */
	@Column(name = "yudisium", nullable = false, length = 255)
	public String getYudisium() {
		return yudisium;
	}

	/**
	 * Menyetel predikat yudisium yang tercetak. Tanpa validasi terhadap daftar predikat yang sah.
	 *
	 * @param yudisium predikat yudisium sesuai yang tercetak.
	 */
	public void setYudisium(String yudisium) {
		this.yudisium = yudisium;
	}

	/**
	 * Mengembalikan nama fakultas yang tercetak (kolom {@code fakultas}, {@code NOT NULL}).
	 *
	 * <p>Salinan teks, bukan relasi: tidak ada FK ke {@code Fakultas}. Ini disengaja untuk
	 * verifikasi dokumen — nama fakultas yang tercetak harus tetap sebagaimana adanya pada saat
	 * pencetakan, walaupun master fakultas kemudian diubah namanya atau digabung.</p>
	 *
	 * @return nama fakultas sebagaimana tercetak, atau {@code null} bila belum diisi.
	 */
	@Column(name = "fakultas", nullable = false, length = 255)
	public String getFakultas() {
		return fakultas;
	}

	/**
	 * Menyetel nama fakultas yang tercetak.
	 *
	 * @param fakultas nama fakultas sesuai yang tercetak.
	 */
	public void setFakultas(String fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan nama program studi yang tercetak (kolom {@code prodi}, {@code NOT NULL}).
	 * Sama seperti {@link #getFakultas()}: salinan teks tanpa FK ke {@code Jurusan}, sengaja
	 * dibekukan pada nilai saat pencetakan.
	 *
	 * @return nama program studi sebagaimana tercetak, atau {@code null} bila belum diisi.
	 */
	@Column(name = "prodi", nullable = false, length = 255)
	public String getProdi() {
		return prodi;
	}

	/**
	 * Menyetel nama program studi yang tercetak.
	 *
	 * @param prodi nama program studi sesuai yang tercetak.
	 */
	public void setProdi(String prodi) {
		this.prodi = prodi;
	}

	/**
	 * Mengembalikan jumlah matakuliah yang tercantum pada dokumen (kolom {@code jumlah_mk},
	 * {@code NOT NULL}), sebagai teks.
	 *
	 * <p>Padanannya di sisi pencetakan adalah parameter Jasper {@code "jumlah_mk"} yang diisi
	 * kelas-kelas laporan akademik ({@code LaporanIjazahAkademik},
	 * {@code LaporanRekamanNilai}, {@code LaporanRekamanNilai2Kolom},
	 * {@code LaporanPrestasiMahasiswa}) dari {@code detailsperkuliahans.size()}. Nilai itu
	 * berguna sebagai pengaman anti-pemalsuan: baris transkrip yang ditambah/dikurangi akan
	 * membuat jumlah pada dokumen tidak lagi cocok dengan yang tersimpan di sini. Perlu dicatat
	 * kelas-kelas laporan tersebut <b>tidak</b> menuliskan nilainya ke entity ini (lihat catatan
	 * "Penulis — TIDAK ADA" pada Javadoc kelas).</p>
	 *
	 * @return jumlah matakuliah sebagai teks, atau {@code null} bila belum diisi.
	 */
	@Column(name = "jumlah_mk", nullable = false, length = 255)
	public String getJumlahMk() {
		return jumlahMk;
	}

	/**
	 * Menyetel jumlah matakuliah yang tercantum pada dokumen. Tanpa validasi numerik.
	 *
	 * @param jumlahMk jumlah matakuliah sebagai teks.
	 */
	public void setJumlahMk(String jumlahMk) {
		this.jumlahMk = jumlahMk;
	}

	/**
	 * Mengembalikan kode barcode yang tercetak pada dokumen (kolom {@code barcode},
	 * {@code NOT NULL}) — <b>satu-satunya kunci pencarian</b> pada layar verifikasi.
	 *
	 * <p>Beberapa hal yang penting diketahui tentang kolom ini:</p>
	 * <ul>
	 *   <li><b>Tidak ada {@code unique constraint}</b> di level pemetaan, padahal pencarinya
	 *   ({@code CekMetaReportHelper.onCari()}) memakai {@code uniqueResult()}. Dua baris dengan
	 *   barcode sama akan membuat layar verifikasi melempar
	 *   {@code NonUniqueResultException}.</li>
	 *   <li><b>Nilai disimpan apa adanya</b> (getter ini tidak memangkas), sementara masukan
	 *   pengguna di layar verifikasi di-{@code trim()} sebelum dicocokkan. Barcode yang
	 *   terlanjur tersimpan dengan spasi tepi karena itu tidak akan pernah cocok.</li>
	 *   <li><b>Pencocokan memakai {@code ilike} dengan {@code MatchMode.EXACT}</b>, artinya
	 *   perbandingan bersifat <i>case-insensitive</i> dan masukan pengguna dipakai sebagai pola
	 *   {@code LIKE} tanpa peng-escape-an karakter wildcard. Masukan yang mengandung {@code %}
	 *   atau {@code _} karena itu ikut diperlakukan sebagai wildcard — bukan SQL injection
	 *   (nilainya tetap dikirim sebagai parameter terikat), tetapi memungkinkan pencocokan
	 *   sebagian tanpa mengetahui barcode lengkapnya.</li>
	 * </ul>
	 *
	 * @return kode barcode dokumen, atau {@code null} bila belum diisi.
	 */
	@Column(name = "barcode", nullable = false, length = 255)
	public String getBarcode() {
		return barcode;
	}

	/**
	 * Menyetel kode barcode dokumen. Tanpa pemangkasan, tanpa validasi format, dan tanpa
	 * pengecekan keunikan — lihat peringatan pada {@link #getBarcode()}.
	 *
	 * @param barcode kode barcode sebagaimana tercetak di dokumen.
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * Mengembalikan nama pejabat penanda tangan dokumen (kolom {@code penanda_tangan}).
	 *
	 * <p><b>Satu-satunya kolom yang boleh {@code null}</b> di entity ini — konsisten dengan
	 * kenyataan bahwa tidak semua jenis dokumen memuat blok tanda tangan. Isi kolom ini sejalan
	 * dengan data pejabat penanda tangan yang dipakai jalur laporan (lihat
	 * {@code ais.database.model.Staff}), tetapi disimpan sebagai teks lepas tanpa FK.</p>
	 *
	 * @return nama penanda tangan, atau {@code null} bila dokumen tidak memuatnya.
	 */
	@Column(name = "penanda_tangan", length = 255)
	public String getPenandaTangan() {
		return penandaTangan;
	}

	/**
	 * Menyetel nama pejabat penanda tangan dokumen. Boleh {@code null}.
	 *
	 * @param penandaTangan nama penanda tangan sesuai yang tercetak.
	 */
	public void setPenandaTangan(String penandaTangan) {
		this.penandaTangan = penandaTangan;
	}

	/**
	 * Mengembalikan tanggal cetak dokumen (kolom {@code tgl_cetak}, {@code NOT NULL}) sebagai
	 * <b>teks</b>, bukan {@code Date}.
	 *
	 * <p>Perhatikan ketidaksesuaian nama: properti Java {@code tanggalCetak} dipetakan ke kolom
	 * {@code tgl_cetak}. Karena bertipe teks bebas format, kolom ini tidak bisa diurutkan secara
	 * kronologis maupun difilter berdasarkan rentang tanggal di level SQL. Untuk stempel waktu
	 * yang benar-benar bertipe tanggal gunakan {@link #getTanggal_dirubah()} — tetapi itu
	 * mencatat perubahan baris metadata, bukan waktu pencetakan dokumen.</p>
	 *
	 * @return tanggal cetak sebagaimana tercetak di dokumen, atau {@code null} bila belum diisi.
	 */
	@Column(name = "tgl_cetak", nullable = false, length = 255)
	public String getTanggalCetak() {
		return tanggalCetak;
	}

	/**
	 * Menyetel tanggal cetak dokumen sebagai teks. Tanpa validasi format tanggal.
	 *
	 * @param tanggalCetak tanggal cetak sesuai yang tercetak di dokumen.
	 */
	public void setTanggalCetak(String tanggalCetak) {
		this.tanggalCetak = tanggalCetak;
	}

	/**
	 * Mengembalikan jenis dokumen yang direkam (kolom {@code jenis_report}, {@code NOT NULL}),
	 * mis. transkrip, ijazah, atau rekaman nilai.
	 *
	 * <p>Teks bebas tanpa daftar nilai yang dibakukan di kode — hanya ditampilkan apa adanya di
	 * layar verifikasi dan tidak pernah dipakai sebagai kriteria filter. Nama method sengaja
	 * mempertahankan gaya {@code snake_case} bawaan generator ({@code getJenis_report}, bukan
	 * {@code getJenisReport}); mengubahnya akan memutus pemetaan properti Hibernate dan
	 * pemanggilnya di {@code CekMetaReportHelper}.</p>
	 *
	 * @return jenis dokumen, atau {@code null} bila belum diisi.
	 */
	@Column(name = "jenis_report", nullable = false, length = 255)
	public String getJenis_report() {
		return jenis_report;
	}

	/**
	 * Menyetel jenis dokumen yang direkam. Tanpa validasi terhadap daftar jenis yang dikenal.
	 *
	 * @param jenis_report jenis dokumen (mis. transkrip/ijazah).
	 */
	public void setJenis_report(String jenis_report) {
		this.jenis_report = jenis_report;
	}

}
