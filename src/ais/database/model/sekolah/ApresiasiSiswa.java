package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.ui.util.WaktuUtil;

/**
 * Entity TRANSAKSI "apresiasi yang diterima seorang siswa" pada modul sekolah
 * (tabel {@code sekolah.apresiasi_siswa}).
 *
 * <h2>Apa ini SEBENARNYA (hasil verifikasi kode, bukan tebakan dari nama kelas)</h2>
 * <p>Kelas ini <b>berbeda</b> dari dua kerabat dekatnya yang namanya mirip, dan perbedaannya
 * mendasar — bukan sekadar variasi penamaan:</p>
 * <ul>
 *   <li>{@link Apresiasi} — <b>master butir</b> apresiasi (satu baris = satu butir yang bisa
 *       diapresiasi, punya kolom {@code kredit}). Tidak punya relasi ke siswa sama sekali.</li>
 *   <li>{@link Penghargaan} — <b>master butir</b> bentuk penghargaan (punya kolom {@code poin}).
 *       Juga tanpa relasi ke siswa.</li>
 *   <li>{@link ApresiasiDanPenghargaan} — <b>master paket/template</b>: memetakan sekumpulan
 *       butir {@code Apresiasi} ke sekumpulan butir {@code Penghargaan}. Masih tanpa relasi ke
 *       siswa, tanpa tanggal, tanpa angka.</li>
 *   <li><i>kelas ini</i> — <b>baris transaksi</b>: satu {@link Siswa} tertentu, pada satu
 *       {@link #getWaktu() tanggal/jam} tertentu, dalam satu {@link #getTa() tahun ajaran},
 *       menerima satu paket ({@link #getApresiasiDanPenghargaan()}) beserta
 *       <b>salinan pilihan</b> butir mana saja dari paket itu yang benar-benar diberikan
 *       ({@link #getApresiasis()} dan {@link #getPenghargaans()}).</li>
 * </ul>
 *
 * <p>Jadi rantai empat lapis keluarga "sisi positif" berjalan seperti ini, dan kelas ini
 * menempati <b>lapis ke-3</b>:</p>
 * <ol>
 *   <li>Master butir — {@link Apresiasi} ({@code kredit}) dan {@link Penghargaan}
 *       ({@code poin}).</li>
 *   <li>Master paket — {@link ApresiasiDanPenghargaan} (daftar pilihan yang boleh dicentang).</li>
 *   <li><b>Transaksi per siswa — kelas ini.</b></li>
 *   <li>Pelaporan — {@code LaporanApresiasiSiswa} (kartu apresiasi per kejadian/per periode) dan
 *       {@code LaporanRaporSiswa} (blok apresiasi + penjumlahan {@code poin} di rapor).</li>
 * </ol>
 *
 * <p>Rantai ini adalah cermin positif dari rantai disiplin
 * {@code Pelanggaran}/{@code Hukuman} &rarr; {@code PelanggaranDanHukuman} &rarr;
 * {@code PelanggaranSiswa}; kelas ini adalah padanan langsung {@code PelanggaranSiswa}.
 * {@code LaporanRaporSiswa} memang memuat kedua sisi berdampingan dalam satu method
 * {@code masukkanPoin(...)}.</p>
 *
 * <h2>Dua koleksi {@code @ManyToMany}: mengapa disalin, bukan diturunkan</h2>
 * <p>Paket ({@link ApresiasiDanPenghargaan}) sudah memuat daftar butir. Namun baris transaksi
 * <b>tidak</b> memakai daftar paket apa adanya: ia menyimpan <b>subset yang dipilih petugas</b>
 * ke dua tabel jembatan miliknya sendiri, {@code sekolah.apresiasi_siswa_has_apresiasi} dan
 * {@code sekolah.apresiasi_siswa_has_penghargaan}. Alurnya di
 * {@code ApresiasiSiswaAction}: memilih paket di combobox memicu
 * {@code loadApresiasi(...)}/{@code loadPenghargaan(...)} yang merender satu checkbox per butir
 * <i>milik paket</i>, dengan status tercentang diambil dari koleksi <i>milik baris ini</i>.
 * Dampak yang perlu disadari: <b>mengubah isi paket di kemudian hari tidak mengubah transaksi
 * lama</b> (bagus, riwayat tetap utuh), tetapi butir yang dihapus dari master masih akan muncul
 * pada transaksi lama karena kedua relasi hanya ber-{@code cascade = MERGE} tanpa
 * {@code REMOVE}.</p>
 *
 * <h2>Siapa yang memakai baris ini</h2>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.ApresiasiSiswaAction} — layar CRUD utama
 *       ({@code /WEB-INF/z/x/y/pages/master/sekolah/apresiasi_siswa.zul}): grid, dialog
 *       tambah/ubah, ekspor Excel, unggah Excel, dan tombol cetak per baris.</li>
 *   <li>{@code ais.action.master.apresiasi.DasbordApresiasi} (dipasang dengan
 *       {@code Lingkup.SISWA} pada tab "Dasbor" layar yang sama) — agregasi jumlah per bulan,
 *       per jenis paket, per hari dalam minggu, plus tabel catatan terbaru.</li>
 *   <li>{@code ais.action.master.dashboard.admin.DasboardSiswa} — kartu "total apresiasi",
 *       tren bulanan, dan peringkat jenis apresiasi terbanyak.</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanApresiasiSiswa} — dua jalur:
 *       {@code generateParameter(ApresiasiSiswa)} mencetak <b>satu</b> kartu apresiasi
 *       (template {@code sekolah/kartu_apresiasi}), sedangkan {@code generateParameter()}
 *       tanpa argumen mencetak rekap satu rentang tanggal.</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanRaporSiswa} — menyisipkan blok apresiasi
 *       ke rapor, tetapi hanya bila {@code JenisRaporSiswa.getAmbilApresiasiSiswa()} bernilai
 *       {@code true} (lihat catatan bug pada bagian "Kuirk" di bawah).</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, callback {@link #onUpdate()},
 *       plus anotasi {@link Audited} (Envers menulis tabel riwayat tiap perubahan).</li>
 *   <li><b>Subjek transaksi</b> — {@link #getSiswa()} (satu-satunya relasi wajib selain paket).</li>
 *   <li><b>Cakupan tenant (turunan)</b> — {@link #getSekolah()}, {@link #getYayasan()};
 *       keduanya <b>diturunkan dari siswa</b>, bukan diisi manual.</li>
 *   <li><b>Isi transaksi</b> — {@link #getApresiasiDanPenghargaan()} (paket),
 *       {@link #getApresiasis()}, {@link #getPenghargaans()} (subset terpilih).</li>
 *   <li><b>Atribut kejadian</b> — {@link #getWaktu()}, {@link #getTa()},
 *       {@link #getKeterangan()}, {@link #getAktif()}.</li>
 *   <li><b>Label turunan</b> — {@link #getNama()} (bukan input pengguna; lihat peringatannya).</li>
 *   <li><b>Konstruktor</b> — {@link #ApresiasiSiswa()} (wajib untuk Hibernate) dan
 *       {@link #ApresiasiSiswa(long, String)} (peninggalan hbm2java, tidak dipakai kode
 *       aplikasi).</li>
 * </ul>
 *
 * <h2>Catatan pemetaan yang tidak kentara</h2>
 * <ul>
 *   <li><b>Field {@code oleh}/{@code olehId}/{@code tanggal_dirubah}/{@code id} sengaja
 *       dideklarasikan ULANG di kelas ini, dan itu BUKAN duplikasi yang bisa dihapus.</b>
 *       {@link GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity}
 *       maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti induknya
 *       sama sekali. Tanpa deklarasi ulang di sini, keempat kolom itu tidak akan pernah
 *       tersimpan. Lihat {@link GeneralValueObject} untuk uraian lengkapnya.</li>
 *   <li>Akses Hibernate di sini adalah <b>property access</b> (anotasi menempel pada getter),
 *       jadi setiap getter yang menulis balik ke field ikut menentukan apa yang di-{@code flush}
 *       ke database. Ada <b>tiga</b> getter semacam itu di kelas ini:
 *       {@link #getSekolah()}, {@link #getYayasan()}, dan — yang paling berdampak —
 *       {@link #getNama()}.</li>
 *   <li>{@code aktif} dan {@code ta} <b>tidak</b> beranotasi {@code @Column}, sehingga jatuh ke
 *       nama kolom bawaan {@code aktif} dan {@code ta}. Keduanya tetap dipetakan (bukan
 *       {@code @Transient}).</li>
 *   <li>{@code dynamicInsert}/{@code dynamicUpdate} aktif: Hibernate hanya menuliskan kolom yang
 *       benar-benar berubah. Ini <b>tidak</b> menetralkan efek getter penulis-balik, karena
 *       dirty-check justru membandingkan nilai hasil getter tersebut.</li>
 *   <li>{@code @OrderBy("nama asc")} pada kedua koleksi hanya menambahkan {@code ORDER BY} pada
 *       SQL pemuatan; karena tipe field-nya {@link HashSet}, urutan itu <b>tidak terjaga</b> di
 *       memori. Karena itu {@code ApresiasiSiswaAction} membungkus ulang keduanya dengan
 *       {@code new TreeSet&lt;&gt;(...)} saat merender — dan {@code TreeSet} memakai
 *       {@code compareTo()} sebagai identitas, sehingga <b>butir yang kunci urutnya sama akan
 *       saling menciutkan</b> (pola yang sama sudah dicatat pada beberapa entity lain).</li>
 * </ul>
 *
 * <h2>Kuirk dan bug yang sudah terverifikasi di sekitar entity ini</h2>
 * <ul>
 *   <li><b>{@link #getNama()} adalah getter destruktif.</b> Setiap kali dibaca, ia menimpa field
 *       {@code nama} dengan gabungan {@code siswa + "_" + paket + "_" + waktu}. Konsekuensinya
 *       dijelaskan pada dokumentasi method itu — termasuk kolom grid berlabel "Jenis" yang
 *       sebenarnya <b>mengurutkan berdasarkan id siswa</b>.</li>
 *   <li><b>Filter "Tampilkan hanya yang aktif" pada layar utama tidak berfungsi.</b>
 *       {@code apresiasi_siswa.zul} mendeklarasikan {@code <checkbox id="searchaktif" ...>},
 *       tetapi {@code ApresiasiSiswaAction} <b>tidak punya field bernama {@code searchaktif}</b>
 *       dan {@code initCriteria(...)} tidak pernah menyaring kolom {@code aktif}. Centang
 *       tersebut hanya memicu pencarian ulang tanpa mengubah hasil: baris non-aktif tetap
 *       tampil di grid. (Laporan {@code LaporanApresiasiSiswa} <i>memang</i> menyaring
 *       {@code aktif}, sehingga grid dan laporan bisa menampilkan jumlah baris yang berbeda —
 *       inilah gejala yang mungkin dilaporkan pengguna.)</li>
 *   <li><b>Bug salin-tempel di {@code LaporanRaporSiswa}.</b> Pada salah satu dari dua jalur
 *       cetak rapor, pemuatan daftar {@code ApresiasiSiswa} digerbangi
 *       {@code jenisRaporSiswa.getAmbilPelanggaranSiswa()} — bukan
 *       {@code getAmbilApresiasiSiswa()} seperti pada jalur satunya. Efeknya: sekolah yang
 *       mengaktifkan blok "Apresiasi" tetapi tidak mengaktifkan "Pelanggaran" akan mencetak
 *       blok apresiasi <b>kosong</b> lewat jalur tersebut, tanpa pesan kesalahan apa pun.</li>
 *   <li><b>Cakupan tenant pada laporan bersifat fail-open.</b> Di
 *       {@code LaporanApresiasiSiswa.generateParameter()}, ketiga filter siswa/yayasan/sekolah
 *       memakai bentuk {@code x == null ? Restrictions.sqlRestriction("true") : ...} — bila
 *       tidak ada yang dipilih, query mengembalikan apresiasi <b>seluruh instalasi</b> pada
 *       rentang tanggal bawaan (2 tahun ke belakang).</li>
 * </ul>
 *
 * <h2>Kontrol akses: yang perlu diketahui pemelihara</h2>
 * <p>Bagian ini murni catatan hasil pembacaan kode pemakai; tidak ada logika di kelas ini yang
 * menegakkan hak akses.</p>
 * <ul>
 *   <li><b>Penyaring "hanya anak saya" bersifat fail-open.</b>
 *       {@code ApresiasiSiswaAction.initCriteria(...)} memasang pembatas
 *       {@code Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa())} hanya bila
 *       daftar anak <b>tidak kosong</b>. Akun wali murid yang belum/gagal tertaut ke anak mana
 *       pun karena itu tidak mendapat pembatas sama sekali dan melihat seluruh baris apresiasi
 *       instalasi. Bentuk kode yang persis sama juga dipakai
 *       {@code AmbilDataSiswaBanbox} (bandbox pemilih siswa).</li>
 *   <li><b>Dasbor memperkuat kebocoran tersebut.</b>
 *       {@code DasbordApresiasi.loadDataWithCache()} menghitung {@code isPersonal} dari
 *       {@code user.getSiswa()}/{@code getMahasiswa()}/{@code ambilGuru()}/{@code ambilDosen()}/
 *       {@code ambilPegawai()} — <b>peran orang tua tidak termasuk</b>. Akun wali murid karena
 *       itu dianggap "tidak personal", sehingga (a) {@code muatApresiasiSiswa(d, null)} berjalan
 *       tanpa penyaring siswa apa pun dan menarik sampai {@code MAX_ROWS = 600} baris apresiasi
 *       lintas siswa/sekolah, dan (b) hasilnya disimpan ke cache <b>L3 se-aplikasi</b> dengan
 *       kunci tanpa id pengguna, sehingga ikut tersaji ke pengguna lain yang membuka dasbor
 *       yang sama.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> {@code apresiasi_siswa.zul} menyisipkan
 *       {@code apresiasi_dan_penghargaan.zul}, {@code apresiasi.zul}, dan {@code penghargaan.zul}
 *       sebagai tab. Karena {@code CommonPrivilages.checkPrevilages(...)} menilai hak dari menu
 *       yang sedang aktif, hak CRUD atas menu "Apresiasi Siswa" otomatis membuka CRUD ketiga
 *       katalog master tersebut tanpa menu tersendiri.</li>
 *   <li><b>Sisi positif</b> (patut dipertahankan saat refactor): tombol Tambah digerbangi
 *       {@code CREATE}, tombol unggah Excel digerbangi {@code CREATE &amp;&amp; UPDATE &amp;&amp;
 *       DELETE} sekaligus, checkbox "Aktif" per baris digerbangi {@code UPDATE}, tombol
 *       salin/ubah/hapus lewat {@code Common.copyEditDeleteButtons(edit, delete, ...)}, dan link
 *       Revisi lewat {@code RevisiHelper.bolehLihatRevisi()}. Yang <b>tidak</b> digerbangi
 *       adalah tombol unduh Excel ({@code Common.cetakData}) dan tombol cetak kartu per baris —
 *       keduanya jalur baca saja, tetapi tetap mengekspor data yang lolos
 *       {@code initCriteria(...)} yang fail-open di atas.</li>
 * </ul>
 *
 * @see ApresiasiDanPenghargaan
 * @see Apresiasi
 * @see Penghargaan
 * @see Siswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "apresiasi_siswa", schema = "sekolah")
public class ApresiasiSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya dibekukan agar instance yang tersimpan di session ZK/HTTP tetap dapat dibaca
	 * setelah kelas ini dikompilasi ulang. Jangan diubah hanya karena menambah field.</p>
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Kunci primer baris ({@code sekolah.apresiasi_siswa.id}); lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir; diisi otomatis, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi otomatis, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> nilai {@code null} atau string kosong/blanko
	 * <b>diabaikan diam-diam</b> (method langsung {@code return} tanpa menulis apa pun).
	 * Jejak audit karena itu tidak bisa dikosongkan setelah pernah terisi — hanya bisa
	 * ditimpa dengan nilai lain yang tidak kosong. Ini konvensi seragam seluruh entity AIS,
	 * bukan kekhususan kelas ini.</p>
	 *
	 * <p>Dipanggil dari {@code AuditTimestampInterceptor.ubah(this)} melalui
	 * {@link #onUpdate()}, bukan dari layar.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/blanko diabaikan tanpa efek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Perilaku "abaikan nilai kosong" persis sama dengan {@link #setOlehId(String)} — lihat
	 * penjelasan di sana.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/blanko diabaikan tanpa efek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} + deklarasi field {@code tanggal_dirubah} — keduanya
	 * berbagi satu baris fisik karena bentukan generator; jangan dipisah tanpa alasan kuat
	 * agar diff terhadap berkas sekerabat tetap mudah dibaca.
	 *
	 * <p><b>Callback:</b> Hibernate memanggil {@code onUpdate()} TEPAT SEBELUM setiap
	 * {@code UPDATE} baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna aktif. <b>Jangan dipanggil
	 * manual</b> — ini titik masuk milik provider persistence.</p>
	 *
	 * <p>Perlu diingat entity ini punya <b>tiga</b> getter yang menulis balik
	 * ({@link #getSekolah()}, {@link #getYayasan()}, {@link #getNama()}), sehingga
	 * {@code UPDATE} — dan karenanya callback ini, plus satu revisi Envers baru — bisa terpicu
	 * pada alur yang secara logis hanya "membaca" baris apresiasi.</p>
	 *
	 * <p><b>Field:</b> {@code tanggal_dirubah} diinisialisasi ke waktu server saat object dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), sehingga baris baru sudah punya stempel waktu
	 * walaupun belum pernah melewati {@code @PreUpdate}. Tidak ada callback {@code @PrePersist}
	 * yang setara di kelas ini.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini
	 * <b>menerima {@code null}</b> apa adanya. Umumnya dipanggil oleh
	 * {@code AuditTimestampInterceptor}, bukan oleh layar.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#TIMESTAMP} (tanggal + jam).
	 * <b>Jangan dikelirukan dengan {@link #getWaktu()}</b>: yang ini adalah metadata teknis
	 * "kapan baris disunting", sedangkan {@code waktu} adalah data bisnis "kapan apresiasi
	 * diberikan kepada siswa".</p>
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk object yang
	 *         baru dibuat lewat konstruktor karena field-nya sudah diinisialisasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik baris; <b>turunan</b> dari siswa, lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik baris; <b>turunan</b> dari sekolah, lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Siswa penerima apresiasi (relasi wajib); lihat {@link #getSiswa()}. */
	private Siswa siswa;
	/** Paket/template apresiasi yang dipakai (relasi wajib); lihat {@link #getApresiasiDanPenghargaan()}. */
	private ApresiasiDanPenghargaan apresiasiDanPenghargaan;
	/** Tanggal dan jam pemberian apresiasi (data bisnis); lihat {@link #getWaktu()}. */
	private Date waktu;
	/** Catatan bebas petugas; satu-satunya kolom teks yang benar-benar diinput pengguna. */
	private String keterangan;
	/** Label turunan hasil rangkaian siswa+paket+waktu; <b>bukan input</b>, lihat {@link #getNama()}. */
	private String nama;
	/** Tahun ajaran kejadian (misal {@code "2025/2026"}); lihat {@link #getTa()}. */
	private String ta;
	/** Penanda baris masih berlaku; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Subset butir {@link Penghargaan} yang benar-benar diberikan; lihat {@link #getPenghargaans()}. */
	private Set<Penghargaan> penghargaans = new HashSet<Penghargaan>();

	/**
	 * Mengembalikan <b>bentuk penghargaan yang benar-benar diberikan</b> pada kejadian ini.
	 *
	 * <p>Ini adalah <b>subset</b> dari {@code getApresiasiDanPenghargaan().getPenghargaans()}:
	 * paket menentukan daftar pilihan, sedangkan koleksi ini menyimpan apa yang dicentang
	 * petugas. Disimpan di tabel jembatan
	 * {@code sekolah.apresiasi_siswa_has_penghargaan(apresiasi_siswa, penghargaan)}.</p>
	 *
	 * <p><b>Dipakai oleh:</b> {@code ApresiasiSiswaAction.loadPenghargaan(...)} (status centang
	 * di dialog, sekaligus <b>koleksi hidup</b> yang langsung dimutasi oleh listener
	 * {@code onClick} tiap checkbox), renderer grid (kolom "Penghargaan"), serta
	 * {@code LaporanApresiasiSiswa} dan {@code LaporanRaporSiswa} yang menjumlahkan
	 * {@code Penghargaan.getPoin()} sebagai total poin penghargaan siswa.</p>
	 *
	 * <p><b>Catatan {@code cascade = MERGE} saja:</b> menyimpan transaksi tidak pernah membuat
	 * atau menghapus baris master {@link Penghargaan} — hanya baris jembatan. Itu perilaku yang
	 * memang diinginkan untuk relasi transaksi&rarr;master.</p>
	 *
	 * <p><b>Catatan urutan:</b> {@code @OrderBy("nama asc")} hanya berlaku pada SQL pemuatan;
	 * karena field-nya {@link HashSet}, urutan hilang di memori. Renderer membungkusnya dengan
	 * {@code TreeSet}, yang berarti dua {@code Penghargaan} dengan kunci {@code compareTo()}
	 * sama akan saling menciutkan pada tampilan.</p>
	 *
	 * @return koleksi penghargaan terpilih; tidak pernah {@code null} (default {@link HashSet}
	 *         kosong), tetapi bisa kosong bila petugas tidak mencentang apa pun
	 */
	@ManyToMany(targetEntity = Penghargaan.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "apresiasi_siswa_has_penghargaan", schema = "sekolah", joinColumns = @JoinColumn(name = "apresiasi_siswa"), inverseJoinColumns = @JoinColumn(name = "penghargaan"))
	public Set<Penghargaan> getPenghargaans() {
		return penghargaans;
	}

	/**
	 * Mengganti seluruh isi koleksi penghargaan terpilih.
	 *
	 * <p><b>Efek samping saat {@code flush}:</b> Hibernate menyelaraskan tabel jembatan dengan
	 * isi baru — baris jembatan yang hilang dari koleksi akan {@code DELETE}. Karena
	 * {@code ApresiasiSiswaAction.onSave(...)} meneruskan koleksi hidup yang sudah dimutasi
	 * checkbox, menyetel referensi baru di sini setara dengan "simpan hasil pencentangan".</p>
	 *
	 * @param penghargaans koleksi penghargaan terpilih yang baru; menyetel {@code null} akan
	 *                     membuat getter mengembalikan {@code null} (setter tidak menjaga nilai
	 *                     default), jadi pemanggil sebaiknya mengirim koleksi kosong
	 */
	public void setPenghargaans(Set<Penghargaan> penghargaans) {
		this.penghargaans = penghargaans;
	}

	/** Subset butir {@link Apresiasi} yang benar-benar diberikan; lihat {@link #getApresiasis()}. */
	private Set<Apresiasi> apresiasis = new HashSet<Apresiasi>();

	/**
	 * Mengembalikan <b>butir apresiasi yang benar-benar diberikan</b> pada kejadian ini.
	 *
	 * <p>Sepenuhnya sejajar dengan {@link #getPenghargaans()}, hanya berbeda master dan tabel
	 * jembatannya: {@code sekolah.apresiasi_siswa_has_apresiasi(apresiasi_siswa, apresiasi)}.
	 * Daftar pilihannya berasal dari {@code getApresiasiDanPenghargaan().getApresiasis()}.</p>
	 *
	 * <p><b>Dipakai oleh:</b> {@code ApresiasiSiswaAction.loadApresiasi(...)} (koleksi hidup
	 * untuk checkbox), renderer grid (kolom "Apresiasi"), dan {@code LaporanApresiasiSiswa}
	 * yang menjumlahkan {@code Apresiasi.getKredit()}. Perhatikan pembagian angkanya:
	 * <b>kredit</b> berasal dari sisi {@code Apresiasi}, <b>poin</b> dari sisi
	 * {@code Penghargaan} — keduanya dijumlahkan terpisah.</p>
	 *
	 * @return koleksi butir apresiasi terpilih; tidak pernah {@code null} secara default, boleh
	 *         kosong
	 */
	@ManyToMany(targetEntity = Apresiasi.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "apresiasi_siswa_has_apresiasi", schema = "sekolah", joinColumns = @JoinColumn(name = "apresiasi_siswa"), inverseJoinColumns = @JoinColumn(name = "apresiasi"))
	public Set<Apresiasi> getApresiasis() {
		return apresiasis;
	}

	/**
	 * Mengganti seluruh isi koleksi butir apresiasi terpilih.
	 *
	 * <p>Efek {@code flush}-nya sama dengan {@link #setPenghargaans(Set)} — lihat penjelasan di
	 * sana.</p>
	 *
	 * @param apresiasis koleksi butir apresiasi terpilih yang baru
	 */
	public void setApresiasis(Set<Apresiasi> apresiasis) {
		this.apresiasis = apresiasis;
	}

	/**
	 * Konstruktor tanpa argumen.
	 *
	 * <p><b>Wajib ada</b> karena Hibernate membutuhkannya untuk menghidrasi entity dari hasil
	 * query, dan karena {@code ApresiasiSiswaAction.onAdd(...)} membuat baris kosong lewat
	 * {@code new ApresiasiSiswa()}. Kedua koleksi sudah terisi {@link HashSet} kosong, sehingga
	 * dialog tambah bisa langsung memakainya tanpa cek {@code null}.</p>
	 */
	public ApresiasiSiswa() {
	}

	/**
	 * Konstruktor peninggalan hbm2java untuk kolom-kolom {@code nullable = false}.
	 *
	 * <p><b>Tidak dipakai kode aplikasi</b> — dan memang sebaiknya tidak dipakai, karena
	 * argumen {@code nama} akan segera <b>ditimpa</b> oleh {@link #getNama()} pada pembacaan
	 * pertama. Dipertahankan hanya agar berkas tetap sebangun dengan hasil generator.</p>
	 *
	 * @param id   kunci primer baris
	 * @param nama label baris — praktis tidak berpengaruh, lihat peringatan di atas
	 */
	public ApresiasiSiswa(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci primer baris apresiasi ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) sehingga {@code insertable = false}: nilai
	 * yang disetel manual sebelum {@code INSERT} akan diabaikan. Bernilai {@code null} selama
	 * object belum pernah disimpan — {@code ApresiasiSiswaAction} memanfaatkan itu untuk
	 * membedakan "Tambah" dari "Ubah" (judul dialog dan keputusan {@code session.load(...)}).</p>
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
	 * Menyetel kunci primer baris.
	 *
	 * <p>Praktis hanya dipakai Hibernate saat hidrasi. Menyetelnya manual pada object baru
	 * tidak membuat {@code INSERT} memakai nilai itu (lihat {@link #getId()}), tetapi
	 * <b>bisa</b> mengubah baris lain bila object kemudian di-{@code merge} — inilah mekanisme
	 * yang membuat kolom {@code "id"} pada unggahan Excel berbahaya bila tombol unggahnya tidak
	 * digerbangi. Pada layar ini tombol unggah <i>sudah</i> digerbangi
	 * {@code CREATE &amp;&amp; UPDATE &amp;&amp; DELETE}.</p>
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan siswa penerima apresiasi.
	 *
	 * <p>Ini adalah <b>subjek</b> transaksi dan satu-satunya relasi yang membuat kelas ini
	 * berbeda dari kerabat masternya. Kolom {@code siswa_id} bersifat {@code nullable = false},
	 * jadi baris tanpa siswa tidak boleh ada; {@code onSave(...)} pada layar memvalidasinya
	 * lebih dulu dengan pesan "Siswa harus diisi".</p>
	 *
	 * <p>Relasi {@code LAZY}, karena itu getter memanggil
	 * {@link GeneralValueObject#check(Object)} untuk meresolusi proxy sebelum mengembalikannya
	 * — supaya pemakaian di luar session (renderer, laporan, dasbor) tidak melempar
	 * {@code LazyInitializationException}. Method ini <b>tidak</b> menulis ke kolom apa pun;
	 * yang ditulis hanya field {@code siswa} lokal dengan object hasil resolusi yang setara.</p>
	 *
	 * <p><b>Dipanggil dari:</b> renderer grid (foto, nomor induk, nama, nama sekolah),
	 * {@link #getSekolah()}, {@link #getNama()}, {@code LaporanApresiasiSiswa},
	 * {@code LaporanRaporSiswa} (pencocokan per siswa), dan {@code DasbordApresiasi}.</p>
	 *
	 * @return siswa penerima apresiasi; secara skema tidak pernah {@code null} untuk baris yang
	 *         sudah tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return this.siswa;
	}

	/**
	 * Menyetel siswa penerima apresiasi.
	 *
	 * <p>Dipanggil {@code ApresiasiSiswaAction.onSave(...)} dari atribut {@code "siswa"} milik
	 * {@code AmbilDataSiswaBanbox}. Karena {@code cascade} mencakup {@code PERSIST} dan
	 * {@code MERGE}, menyimpan baris apresiasi ikut men-{@code merge} object siswa yang
	 * dilampirkan — jangan mengirim instance {@link Siswa} hasil modifikasi lokal yang belum
	 * diinginkan tersimpan.</p>
	 *
	 * @param siswa siswa penerima; menyetel {@code null} akan gagal saat {@code INSERT} karena
	 *              kolomnya {@code nullable = false}
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan sekolah pemilik baris — <b>diturunkan dari siswa, bukan diinput</b>.
	 *
	 * <p><b>Getter penulis-balik.</b> Setiap pembacaan menjalankan {@code sekolah =
	 * getSiswa().getSekolah()} bila siswanya ada, lalu meresolusi proxy dengan
	 * {@link GeneralValueObject#check(Object)}. Karena pemetaan Hibernate di kelas ini memakai
	 * <i>property access</i>, nilai hasil turunan itulah yang ikut ter-{@code flush} ke kolom
	 * {@code sekolah_id}. Konsekuensinya:</p>
	 * <ul>
	 *   <li>Kolom {@code sekolah_id} tidak pernah perlu diisi layar — cukup pilih siswanya.</li>
	 *   <li>Memindahkan siswa ke sekolah lain akan <b>menulis ulang</b> {@code sekolah_id}
	 *       seluruh baris apresiasi lamanya begitu baris itu tersentuh, sehingga riwayat
	 *       apresiasi "ikut pindah" alih-alih tetap tercatat di sekolah asal.</li>
	 *   <li>Baris lama yang belum pernah tersentuh setelah kolom ini ada bisa masih
	 *       {@code NULL} di database. Filter "Sekolah" pada layar pencarian menyaring kolom
	 *       tersebut, jadi baris ber-{@code sekolah_id} {@code NULL} akan <b>hilang dari
	 *       hasil</b> — bukan berarti datanya tidak ada.</li>
	 * </ul>
	 *
	 * @return sekolah pemilik, atau {@code null} bila siswa maupun sekolahnya belum dapat
	 *         diresolusi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		siswa = getSiswa();
		if (siswa != null) {
			sekolah = siswa.getSekolah();
		}
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris.
	 *
	 * <p><b>Perilaku non-obvious:</b> object {@link Sekolah} yang belum punya id
	 * ({@code getId() == null}) <b>dibuang</b> dan field disetel {@code null}. Ini mencegah
	 * {@code cascade = PERSIST} tanpa sengaja menyisipkan baris master sekolah baru dari objek
	 * setengah jadi milik komponen UI.</p>
	 *
	 * <p>Perlu diingat nilai apa pun yang disetel di sini akan <b>ditimpa</b> pada pembacaan
	 * berikutnya oleh {@link #getSekolah()} selama siswanya terisi.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau instance tanpa id menghasilkan
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris — <b>diturunkan dari sekolah</b>.
	 *
	 * <p>Getter penulis-balik lapis kedua: memanggil {@link #getSekolah()} (yang sendirinya
	 * menurunkan dari {@link #getSiswa()}), lalu mengambil {@code sekolah.getYayasan()} dan
	 * meresolusi proxy-nya. Jadi satu pembacaan {@code getYayasan()} berpotensi memicu
	 * <b>tiga</b> resolusi lazy berantai — perhatikan ini pada perulangan besar seperti ekspor
	 * Excel dan laporan rekap.</p>
	 *
	 * <p>Semua catatan dampak pada {@link #getSekolah()} berlaku sama di sini, termasuk soal
	 * baris lama yang masih {@code NULL} dan filter "Yayasan" pada layar pencarian.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila rantai siswa&rarr;sekolah&rarr;yayasan
	 *         terputus
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
	 * Menyetel yayasan pemilik baris.
	 *
	 * <p>Perilaku "buang object tanpa id" persis sama dengan {@link #setSekolah(Sekolah)} —
	 * lihat penjelasan di sana. Nilainya juga akan ditimpa oleh {@link #getYayasan()} pada
	 * pembacaan berikutnya.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau instance tanpa id menghasilkan
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan catatan bebas petugas atas kejadian apresiasi ini.
	 *
	 * <p>Ini praktis satu-satunya kolom teks yang benar-benar diisi manusia pada baris ini
	 * (kolom {@code nama} adalah turunan). Diisi lewat {@code Textbox} 3 baris pada dialog
	 * tambah/ubah, ditampilkan pada kolom "Keterangan" grid, ikut diekspor ke Excel, dan
	 * ditumpangkan ke kartu apresiasi maupun blok apresiasi di rapor.</p>
	 *
	 * @return keterangan, atau {@code null}/kosong bila tidak diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas petugas.
	 *
	 * @param keterangan teks keterangan; boleh {@code null} atau kosong
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan label baris — <b>PERINGATAN: getter ini destruktif dan menghitung ulang
	 * nilainya setiap kali dipanggil.</b>
	 *
	 * <p>Alih-alih sekadar membaca field, method ini menimpa {@code nama} dengan rangkaian:</p>
	 * <pre>
	 *   nama = getSiswa() + "_" + getApresiasiDanPenghargaan() + "_" + getWaktu();
	 * </pre>
	 * <p>Karena rangkaian string memakai {@code toString()} tiap operand, hasilnya berbentuk
	 * {@code "&lt;idSiswa&gt;-&lt;nomorInduk&gt;-&lt;namaSiswa&gt;_&lt;kode&gt; -
	 * &lt;namaPaket&gt;_&lt;Date.toString()&gt;"} —
	 * lihat {@code Siswa.toString()} dan {@code GeneralValueObject.toString()}. Nilai apa pun
	 * yang disetel lewat {@link #setNama(String)} (termasuk dari unggahan Excel) karena itu
	 * <b>tidak bertahan</b>: pembacaan berikutnya menggantinya.</p>
	 *
	 * <p><b>Konsekuensi yang perlu diketahui pemelihara:</b></p>
	 * <ul>
	 *   <li>Kolom {@code nama} dipetakan {@code nullable = false}, sehingga wajib terisi —
	 *       inilah alasan getter turunan ini ada. Sisi buruknya, karena property access
	 *       dipakai, <b>nilai baru ikut ter-{@code flush}</b> pada dirty-check.</li>
	 *   <li>Isinya ikut berubah bila nama siswa, nomor induk, atau nama paket diubah di master
	 *       — riwayat lama tertulis ulang begitu barisnya tersentuh, dan karena kelas ini
	 *       {@link Audited}, tiap penulisan ulang melahirkan revisi Envers baru.</li>
	 *   <li>{@link #getWaktu()} mengembalikan waktu server sekarang bila {@code waktu} masih
	 *       {@code null}, sehingga baris yang belum punya tanggal menghasilkan {@code nama}
	 *       yang <b>berbeda pada setiap pembacaan</b>.</li>
	 *   <li>Kolom grid berlabel <b>"Jenis"</b> pada {@code apresiasi_siswa.zul} memakai
	 *       {@code sort="auto(nama)"}, padahal yang ditampilkan di sel itu adalah nama paket.
	 *       Karena {@code nama} diawali id siswa, mengeklik header "Jenis" sebenarnya
	 *       <b>mengurutkan berdasarkan id siswa</b>, bukan berdasarkan jenis apresiasi.</li>
	 *   <li>Kolom {@code "nama"} tetap disertakan dalam daftar kolom ekspor/impor Excel
	 *       ({@code contents} pada {@code ApresiasiSiswaAction}); mengisinya pada berkas
	 *       unggahan tidak ada efeknya.</li>
	 * </ul>
	 *
	 * @return label turunan baris ini; secara praktis tidak pernah {@code null} selama relasi
	 *         siswa/paket dapat diresolusi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		nama = getSiswa() + "_" + getApresiasiDanPenghargaan() + "_" + getWaktu();
		return this.nama;
	}

	/**
	 * Menyetel label baris.
	 *
	 * <p><b>Praktis tidak berguna:</b> nilai yang disetel di sini akan ditimpa pada pembacaan
	 * {@link #getNama()} berikutnya. Dipertahankan karena Hibernate membutuhkan pasangan
	 * setter untuk property yang dipetakan, dan karena mekanisme impor Excel generik memanggil
	 * setter berdasarkan nama kolom.</p>
	 *
	 * @param nama label baris; akan ditimpa oleh nilai turunan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan penanda apakah baris apresiasi ini masih berlaku.
	 *
	 * <p><b>Default aman:</b> {@code null} diperlakukan sebagai {@code true}, sehingga baris
	 * lama yang dibuat sebelum kolom ini ada tetap dianggap aktif.</p>
	 *
	 * <p><b>Siapa yang benar-benar menghormati flag ini:</b> hanya
	 * {@code LaporanApresiasiSiswa} (menyaring {@code aktif IS NULL OR aktif = true}). Grid
	 * layar utama dan {@code DasbordApresiasi} <b>tidak</b> menyaringnya sama sekali — lihat
	 * catatan "filter Tampilkan hanya yang aktif tidak berfungsi" pada dokumentasi kelas.
	 * Karena itu jumlah baris di grid dan di laporan bisa berbeda secara sah.</p>
	 *
	 * <p>Checkbox "Aktif" per baris di grid digerbangi hak {@code UPDATE} dan langsung
	 * menyimpan lewat {@code Common.refreshSaveOrUpdate(...)} tanpa tombol Simpan.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} bila
	 *         dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda berlaku/tidaknya baris ini.
	 *
	 * @param aktif {@code true} untuk aktif, {@code false} untuk nonaktif; {@code null} akan
	 *              dibaca kembali sebagai {@code true} oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan paket apresiasi &amp; penghargaan yang dipakai pada kejadian ini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}) ke master lapis 2. Paket inilah yang
	 * menentukan <b>daftar pilihan</b> butir yang boleh dicentang — bukan yang benar-benar
	 * diberikan; yang diberikan tersimpan di {@link #getApresiasis()} dan
	 * {@link #getPenghargaans()}.</p>
	 *
	 * <p>Relasi {@code LAZY} dengan resolusi proxy lewat
	 * {@link GeneralValueObject#check(Object)}. Layar memvalidasi keberadaannya sebelum simpan
	 * dengan pesan "Jenis apresiasi harus diisi".</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@link #getNama()}, renderer grid (label link Revisi),
	 * {@code DasbordApresiasi} (kategori "jenis" pada agregasi), dan
	 * {@code LaporanRaporSiswa} (label baris apresiasi di rapor).</p>
	 *
	 * @return paket apresiasi &amp; penghargaan; secara skema tidak {@code null} untuk baris
	 *         tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "apresiasi_dan_penghargaan", nullable = false)
	public ApresiasiDanPenghargaan getApresiasiDanPenghargaan() {
		apresiasiDanPenghargaan = check(apresiasiDanPenghargaan);
		return apresiasiDanPenghargaan;
	}

	/**
	 * Menyetel paket apresiasi &amp; penghargaan yang dipakai.
	 *
	 * <p>Diisi {@code onSave(...)} dari combobox "Jenis Apresiasi". Perlu diperhatikan bahwa
	 * mengganti paket pada baris yang sudah ada <b>tidak</b> otomatis membersihkan butir
	 * terpilih dari paket lama — koleksi {@link #getApresiasis()}/{@link #getPenghargaans()}
	 * tetap berisi apa adanya sampai petugas mengubah pencentangannya. Akibatnya sebuah baris
	 * bisa merujuk butir yang tidak ada dalam daftar pilihan paket barunya.</p>
	 *
	 * @param apresiasiDanPenghargaan paket yang dipakai; wajib tidak {@code null} agar
	 *                                {@code INSERT} berhasil
	 */
	public void setApresiasiDanPenghargaan(ApresiasiDanPenghargaan apresiasiDanPenghargaan) {
		this.apresiasiDanPenghargaan = apresiasiDanPenghargaan;
	}

	/**
	 * Mengembalikan tanggal dan jam pemberian apresiasi (data bisnis).
	 *
	 * <p><b>Default non-obvious:</b> bila field {@code waktu} masih {@code null}, method
	 * mengembalikan <b>waktu server saat ini</b> ({@code WaktuUtil.getDate()}) alih-alih
	 * {@code null}. Nilai pengganti itu <b>tidak</b> dituliskan ke field, sehingga:</p>
	 * <ul>
	 *   <li>Pembacaan berulang pada baris tanpa {@code waktu} menghasilkan nilai yang
	 *       berbeda-beda (dan ikut membuat {@link #getNama()} berubah-ubah).</li>
	 *   <li>Kolom database tetap {@code NULL} sampai {@link #setWaktu(Date)} dipanggil. Alur
	 *       normal aman: dialog tambah menginisialisasi {@code MyDatebox} dari getter ini dan
	 *       menyimpannya kembali, sehingga baris baru selalu punya waktu konkret.</li>
	 *   <li>{@code Restrictions.between("waktu", ...)} pada laporan bekerja atas kolom
	 *       database, bukan atas nilai pengganti ini — baris ber-{@code waktu} {@code NULL}
	 *       tidak akan pernah masuk rentang tanggal mana pun.</li>
	 * </ul>
	 *
	 * <p>Dipetakan {@link TemporalType#TIMESTAMP} tanpa {@code @Column} eksplisit (kolom
	 * {@code waktu}). Dipakai sebagai kunci pengurutan utama grid
	 * ({@code Order.desc("waktu")}), sumbu tren bulanan/harian dasbor, dan tanggal cetak kartu
	 * apresiasi.</p>
	 *
	 * @return tanggal/jam pemberian apresiasi; tidak pernah {@code null} (jatuh ke waktu
	 *         sekarang)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menyetel tanggal dan jam pemberian apresiasi.
	 *
	 * @param waktu tanggal/jam kejadian; menyetel {@code null} mengembalikan perilaku
	 *              "jatuh ke waktu sekarang" pada {@link #getWaktu()}
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan tahun ajaran kejadian (misal {@code "2025/2026"}).
	 *
	 * <p><b>Default non-obvious yang sejajar dengan {@link #getWaktu()}:</b> bila {@code ta}
	 * masih {@code null}, method mengembalikan {@code Common.getCurrentTahunAkademik()} —
	 * tahun akademik <b>aktif saat ini</b>, bukan tahun akademik saat apresiasi diberikan.
	 * Nilai pengganti tidak dituliskan ke field, jadi kolom database tetap {@code NULL}.</p>
	 *
	 * <p>Ini penting untuk pelaporan: {@code LaporanRaporSiswa} menyaring dengan
	 * {@code Restrictions.eq("ta", ta)} atas kolom database. Baris warisan ber-{@code ta}
	 * {@code NULL} karena itu <b>tidak akan pernah muncul di rapor tahun mana pun</b>,
	 * meskipun grid menampilkannya dengan label tahun ajaran berjalan. Perbedaan inilah yang
	 * biasanya muncul sebagai keluhan "apresiasinya ada di layar tapi tidak tercetak di
	 * rapor".</p>
	 *
	 * <p>Alur normal aman: combobox "Tahun Ajaran" pada dialog diinisialisasi dari getter ini
	 * dan disimpan kembali oleh {@code onSave(...)}.</p>
	 *
	 * @return tahun ajaran kejadian, atau tahun akademik berjalan bila belum pernah disetel
	 */
	public String getTa() {
		return ta == null ? Common.getCurrentTahunAkademik() : ta;
	}

	/**
	 * Menyetel tahun ajaran kejadian.
	 *
	 * @param ta tahun ajaran dalam format yang dipakai {@code Common.generateTahunAjaran(...)}
	 *           (misal {@code "2025/2026"}); {@code null} mengaktifkan kembali perilaku default
	 *           pada {@link #getTa()}
	 */
	public void setTa(String ta) {
		this.ta = ta;
	}

}
