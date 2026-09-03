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
 * Master <b>kelompok jam pelajaran</b> pada modul sekolah — tabel
 * {@code sekolah.kelompok_jam_pelajaran}.
 *
 * <h3>Peran domain (TERVERIFIKASI)</h3>
 * <p>Entity ini adalah <b>label pengelompok</b> untuk baris {@link JamPelajaran}. Sebuah
 * {@code JamPelajaran} (satu slot waktu: "Jam ke-1", 07.00–07.45, sekian JP) boleh menunjuk ke
 * satu kelompok lewat kolom FK {@code kelompok_jam_pelajaran_id} yang <b>nullable</b>. Contoh
 * pemakaian nyata: memisahkan pola jam "Reguler" dari "Ramadhan", "Kelas Pagi" dari "Kelas Siang",
 * atau jam Sekolah Dasar dari jam Sekolah Menengah pada satu instalasi.</p>
 *
 * <p><b>Arah relasi hanya satu arah.</b> Kelas ini <b>tidak</b> memiliki koleksi
 * {@code Set&lt;JamPelajaran&gt;}; anggota kelompok hanya dapat ditemukan dengan mengkueri
 * {@code JamPelajaran} berdasarkan properti {@code kelompokJamPelajaran}. Konsekuensi praktisnya:
 * menghapus sebuah kelompok tidak melihat siapa saja yang masih menunjuk ke sana, dan tidak ada
 * mekanisme kaskade apa pun dari sisi ini.</p>
 *
 * <p><b>Bukan unit penilaian, bukan unit penjadwalan.</b> Kelompok tidak pernah menjadi bagian
 * dari kunci sebuah jadwal. {@code JadwalPelajaran} menyimpan hingga dua belas slot jam
 * ({@code jamPelajaran} … {@code jamPelajaran12}) yang masing-masing menunjuk
 * {@code JamPelajaran}; kelompok baru muncul secara <i>tidak langsung</i> ketika laporan menelusuri
 * {@code jadwalPelajaran.jamPelajaranN.kelompokJamPelajaran}. Jadi kelompok murni dimensi
 * pengelompokan untuk tampilan/cetak, bukan data transaksional.</p>
 *
 * <h3>Siapa yang memakai</h3>
 * <ul>
 * <li><b>Layar master</b> {@code ais.action.master.sekolah.KelompokJamPelajaranAction} +
 * {@code /pages/master/sekolah/kelompok_jam_pelajaran.zul} — CRUD penuh, grid berkolom Nama
 * Kelompok / Sekolah / Keterangan / Urut / Aktif.</li>
 * <li><b>Layar Jam Pelajaran</b> {@code ais.action.master.sekolah.JamPelajaranAction} — combobox
 * "Kelompok Jam Pelajaran" pada formulir jam pelajaran, diisi
 * {@code Common.insertCombo(..., KelompokJamPelajaran.class, ...)} dengan tapis
 * <i>sekolah cocok atau NULL</i> DAN <i>aktif true atau NULL</i>. Layar yang sama juga
 * <b>menyisipkan ulang seluruh layar master ini</b> sebagai tab kedua bernama "Kelompok"
 * (lihat bagian Hak akses).</li>
 * <li><b>Laporan jadwal</b> {@code ais.action.report.format1.sekolah.LaporanJadwalPelajaran} —
 * satu-satunya pembaca "bisnis" yang sesungguhnya: kelompok dipakai untuk memecah cetak jadwal
 * pelajaran menjadi blok terpisah, dan tiga parameter JasperReports diturunkan darinya
 * ({@code kelompokJamPelajaran}, {@code kelompokJamPelajaran.nama},
 * {@code kelompokJamPelajaran.keterangan}).</li>
 * <li><b>Pramuat cache</b> {@code ais.common.InitData} — kelasnya terdaftar pada
 * {@code initClasses(...)} sehingga seluruh barisnya dimuat ke cache aplikasi saat startup.
 * Ini murni pramuat; <b>tidak ada auto-seed</b> — instalasi baru mulai tanpa satu kelompok pun,
 * dan seluruh {@code JamPelajaran} berjalan dengan {@code kelompok_jam_pelajaran_id = NULL}
 * (sah, karena FK-nya memang nullable).</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 * <ol>
 * <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Field-nya dideklarasikan ulang di sini
 * (lihat catatan tentang {@link GeneralValueObject} di bawah).</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()} dan {@link #getYayasan()}; yayasan diturunkan
 * otomatis dari sekolah pada setiap pembacaan.</li>
 * <li><b>Atribut deskriptif</b> — {@link #getNama()}, {@link #getKeterangan()},
 * {@link #getNomorUrut()}, {@link #getAktif()}.</li>
 * <li><b>Utilitas</b> — {@link #toString()} dan dua konstruktor.</li>
 * </ol>
 * <p>Tidak ada method bisnis, tidak ada query statis, dan tidak ada koleksi apa pun di kelas ini.
 * Seluruh perilaku non-trivial terkonsentrasi pada tiga getter: {@link #getSekolah()},
 * {@link #getYayasan()}, dan {@link #getNomorUrut()}.</p>
 *
 * <h3>Catatan penting tentang {@link GeneralValueObject}</h3>
 * <p>Induknya <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — hanya POJO abstrak
 * biasa, sehingga Hibernate <b>tidak</b> memetakan satu pun properti induk. Karena itu field
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan
 * ulang di setiap entity turunan. Duplikasi ini <b>bukan bug</b>, melainkan keharusan teknis; jangan
 * "dirapikan" dengan memindahkannya ke induk. Yang tetap diwarisi adalah helper statis, terutama
 * {@code check(...)} yang dipakai getter relasi di kelas ini.</p>
 *
 * <h3>Pola arsitektur berulang — hasil verifikasi pada berkas ini</h3>
 * <ul>
 * <li><b>Getter dengan efek tulis balik (write-back)</b> — <b>ADA, dua buah</b>.
 * {@link #getYayasan()} menurunkan ulang yayasan dari sekolah pada setiap pembacaan, dan
 * {@link #getNomorUrut()} menulis nilai bawaan {@code 1} ke field bila kolomnya masih
 * {@code NULL}. Karena keduanya adalah property accessor yang dibaca Hibernate saat
 * <i>dirty checking</i>, pembacaan biasa dapat berubah menjadi UPDATE nyata plus satu revisi
 * Envers baru. Rinciannya di Javadoc masing-masing method.</li>
 * <li><b>Getter destruktif yang mengosongkan data</b> (pola {@code KelasSiswaPSB.getNama()}) —
 * <b>TIDAK ADA</b>. {@link #getNama()} murni mengembalikan field.</li>
 * <li><b>{@code getKeterangan()} yang membalik kontraknya</b> — <b>TIDAK ADA</b> di sini;
 * {@link #getKeterangan()} adalah getter polos.</li>
 * <li><b>Penciutan {@code TreeSet}</b> — <b>TIDAK RELEVAN</b>: kelas ini tidak memiliki koleksi
 * apa pun.</li>
 * <li><b>Cakupan tenant fail-open</b> — <b>TIDAK persis fail-open, melainkan varian "nol
 * filter"</b>. Lihat bagian di bawah.</li>
 * <li><b>Pewarisan hak lewat menu induk</b> — <b>ADA</b>. Lihat bagian di bawah.</li>
 * </ul>
 *
 * <h3>Cakupan tenant dan hak akses</h3>
 * <p><b>Nol filter tenant pada layar master.</b> {@code KelompokJamPelajaranAction.initCriteria()}
 * hanya menambahkan pembatas untuk nilai yang <i>dipilih pengguna</i> di kotak pencarian; bila
 * combobox Yayasan/Sekolah dibiarkan kosong, pembatasnya menjadi {@code Restrictions.sqlRestriction("1=1")}.
 * Tidak ada pembatas bawaan ke sekolah/yayasan milik pengguna. Artinya siapa pun yang punya hak
 * BACA menu ini melihat — dan dengan hak UBAH/HAPUS dapat mengubah atau menghapus — kelompok jam
 * pelajaran milik <b>seluruh sekolah dan yayasan</b> dalam satu instalasi. Isinya metadata jadwal
 * (nama, keterangan, urutan), bukan data pribadi, sehingga dampak kerahasiaannya rendah; dampak
 * <b>integritasnya</b> tidak nol — menghapus kelompok milik sekolah lain langsung mengubah cara
 * laporan jadwal sekolah tersebut tercetak. Ini mekanisme yang sama dengan yang sudah tercatat
 * pada {@code RuangPSB}/{@code KelasSiswaPSB}/{@code SiswaAction}: bukan tapis yang gagal terbuka,
 * melainkan tapis yang memang tidak pernah ditulis.</p>
 *
 * <p><b>Pewarisan hak lewat menu induk (instance ke-4 pola ini).</b> Layar Jam Pelajaran
 * menyisipkan berkas ZUL layar master ini apa adanya sebagai tab kedua berjudul "Kelompok"
 * ({@code JamPelajaranAction.onMasa(...)} membuat
 * {@code MyInclude("/pages/master/sekolah/kelompok_jam_pelajaran.zul")}). Sisipan itu berbagi
 * halaman ZK yang sama, sedangkan {@code CommonPrivilages.checkPrevilages(...)} menentukan hak
 * dari {@code Common.getCurrentMenu()} — yaitu menu <b>Jam Pelajaran</b>, bukan menu Kelompok Jam
 * Pelajaran. Jadi hak TAMBAH/UBAH/HAPUS pada master kelompok sesungguhnya diberikan oleh hak menu
 * Jam Pelajaran: peran yang sengaja tidak diberi menu Kelompok tetap memperoleh CRUD penuh atasnya
 * lewat tab tersebut. Mekanismenya identik dengan {@code PaketPsb} (batch 50),
 * {@code KategoriItemPenilaianSiswa} dan {@code SubMatapelajaran} (batch 51).</p>
 *
 * <p>Di luar dua hal itu, layar master ini termasuk yang <b>bergerbang benar</b>: ada
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose}, tombol Tambah dibatasi
 * {@code CREATE}, tombol Ubah/Hapus dibatasi {@code UPDATE}/{@code DELETE}, dan tombol unggah
 * massal menuntut ketiganya sekaligus. Tombol "Download" (ekspor Excel) tidak bergerbang, tetapi
 * kolom yang diekspor hanya {@code id}, {@code nama}, {@code sekolah}, {@code keterangan},
 * {@code induk}, {@code nomorUrut} — tidak memuat data pribadi.</p>
 *
 * <h3>Kuirk dan bug yang terverifikasi di sekitar entity ini</h3>
 * <ul>
 * <li><b>Komentar generator salah nama.</b> Blok komentar asli berkas ini berbunyi
 * "JenisPenilaian generated by hbm2java" — nama kelas yang sama sekali berbeda. Ini contoh lain
 * dari komentar hbm2java yang tersalin lintas berkas (lihat temuan {@code JenisGuru}, batch 51):
 * komentar generator di modul ini <b>tidak dapat dipercaya</b> sebagai petunjuk domain.</li>
 * <li><b>Judul dialog salah modul.</b> {@code KelompokJamPelajaranAction.init(...)} memberi judul
 * jendela "Tambah kelompok matapelajaran"/"Ubah kelompok matapelajaran" — sisa salin-tempel dari
 * layar {@code KelompokMatapelajaran}. Murni kosmetik, tetapi menyesatkan pengguna karena kedua
 * master itu benar-benar ada dan berbeda.</li>
 * <li><b>Kolom ekspor {@code induk} tidak ada pada entity ini.</b> Daftar kolom ekspor/unggah di
 * layar master memuat {@code "induk"}, properti yang hanya dimiliki {@code KelompokMatapelajaran}
 * (self-FK berjenjang). Tidak meledak: pembacaan properti akhirnya jatuh ke refleksi yang
 * mengembalikan {@code null} secara senyap, sehingga hasilnya sekadar satu kolom yang <b>selalu
 * kosong</b> di berkas Excel. Sisa salin-tempel yang sama dengan poin sebelumnya.</li>
 * <li><b>Kotak centang filter "Tampilkan hanya yang aktif" mati total.</b> Berkas ZUL
 * mendeklarasikan {@code <checkbox id="searchaktif" checked="true" forward="onClick=onSearchDefault"/>},
 * tetapi {@code KelompokJamPelajaranAction} tidak memiliki field bernama {@code searchaktif}
 * (jadi tidak pernah di-autowire) dan {@code initCriteria()} tidak pernah menyentuh kolom
 * {@code aktif}. Mengklik centang itu memang memicu pencarian ulang, namun hasilnya tidak pernah
 * berubah: kelompok yang sudah dinonaktifkan tetap tampil di grid meski centang menyala. Filter
 * ini sudah tampak menyala sejak halaman dibuka, sehingga wajar disangka bekerja.</li>
 * <li><b>{@code nomorUrut} tidak pernah dipakai untuk mengurutkan apa pun.</b> Lihat
 * {@link #getNomorUrut()}.</li>
 * <li><b>Laporan jadwal: dua slot terakhir tidak ikut diseleksi.</b> Lihat bagian berikutnya.</li>
 * </ul>
 *
 * <h3>Catatan khusus {@code LaporanJadwalPelajaran}</h3>
 * <p>Dua hal berikut ditemukan saat memverifikasi peran domain kelas ini dan berguna bila suatu
 * saat ada keluhan "jadwal tercetak kurang" atau "jadwal tercetak dobel":</p>
 * <ol>
 * <li><b>Slot 11 dan 12 tidak masuk kriteria seleksi.</b> Kriteria pemilihan baris
 * {@code JadwalPelajaran} membangun {@code OR} atas kelompok pada slot {@code jamPelajaran}
 * sampai {@code jamPelajaran10} saja (dan hanya sepuluh alias itu yang dibuat), sementara
 * perulangan panen datanya membaca slot {@code jamPelajaran} sampai {@code jamPelajaran12}.
 * Akibatnya baris jadwal yang <i>hanya</i> memakai slot ke-11/ke-12 tidak pernah terpilih dan
 * isinya tidak pernah tercetak; slot ke-11/ke-12 hanya ikut tercetak bila baris yang sama
 * kebetulan juga terpilih lewat salah satu slot 1–10.</li>
 * <li><b>Baris yang menyentuh dua kelompok dipanen berulang.</b> Perulangan luar berjalan per
 * kelompok, tetapi perulangan panen di dalamnya selalu membaca <b>seluruh</b> slot dan
 * mengelompokkannya menurut kelompok milik slot itu sendiri — bukan menurut kelompok yang sedang
 * diiterasi. Satu baris {@code JadwalPelajaran} yang slot-slotnya menunjuk dua kelompok berbeda
 * akan terpilih pada kedua putaran dan seluruh slotnya ikut dipanen dua kali.</li>
 * </ol>
 * <p>Selain itu, bila combobox Sekolah pada layar laporan dibiarkan kosong, daftar kelompok yang
 * dipakai laporan ditarik <b>lintas sekolah</b> ({@code Restrictions.sqlRestriction("true")}) —
 * varian yang sama dengan "nol filter tenant" di atas.</p>
 *
 * <h3>Persistensi</h3>
 * <p>Ber-{@code @Audited} (Hibernate Envers menyimpan riwayat perubahan ke skema audit) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate}, sehingga hanya kolom yang benar-benar berubah yang
 * ikut dalam pernyataan SQL. Id memakai strategi {@code IDENTITY} — berurutan dan mudah ditebak,
 * jadi jangan pernah menjadikan id sebagai satu-satunya pembatas akses. Tidak ada
 * {@code unique constraint} pada {@code nama}: dua kelompok bernama sama pada sekolah yang sama
 * tetap dapat dibuat, dan {@code onSave} layar master pun tidak memeriksa duplikasi.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see JamPelajaran
 * @see JadwalPelajaran
 * @see Sekolah
 * @see Yayasan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "kelompok_jam_pelajaran", schema = "sekolah")
public class KelompokJamPelajaran extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan sejak berkas dibuat; jangan diubah karena instance
	 * entity ikut diserialisasi ke sesi ZK dan ke cache aplikasi.
	 */
	private static final long serialVersionUID = -8817799955174105108L;
	/**
	 * Kunci utama baris, dibangkitkan basis data ({@code IDENTITY}). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 * Dideklarasikan ulang karena induknya tidak dipetakan Hibernate.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh} dan diisi
	 * dari sumber yang sama. Dideklarasikan ulang karena induknya tidak dipetakan Hibernate.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila baris belum pernah melewati UPDATE
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>validasi non-trivial</b>: argumen
	 * {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b> sehingga nilai lama
	 * tetap bertahan. Jejak audit karenanya tidak dapat dikosongkan lewat setter ini.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan <b>validasi non-trivial</b> yang sama dengan
	 * {@link #setOlehId(String)}: argumen {@code null} atau kosong/spasi diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila belum pernah terisi
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
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat di JVM) dan tidak pernah mendapat
	 * {@code oleh}/{@code olehId} dari jalur ini.</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} — hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya.</p>
	 *
	 * <p><b>Pemicu yang mudah terlewat.</b> Selain penyimpanan formulir, satu klik centang "Aktif"
	 * atau satu perubahan angka pada kolom "Urut" di grid daftar sudah cukup untuk memicu jalur ini
	 * (renderer layar master memanggil {@code Common.refreshSaveOrUpdate(...)} langsung dari
	 * event {@code onCheck}/{@code onChange}). Bahkan pembacaan biasa pun dapat memicunya, karena
	 * {@link #getYayasan()} dan {@link #getNomorUrut()} menulis balik ke field — lihat Javadoc
	 * kedua method itu.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; nilai {@code null} diterima apa adanya.
	 *
	 * <p>Dalam pemakaian normal setter ini tidak perlu dipanggil kode aplikasi — pengisiannya
	 * dikerjakan {@link #onUpdate()} pada setiap UPDATE.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * <p>Tidak pernah {@code null} untuk objek yang dibuat lewat konstruktor Java (field-nya
	 * diinisialisasi {@code WaktuUtil.getDate()}), tetapi bisa {@code null} untuk baris lama yang
	 * dimuat dari basis data sebelum kolom ini terisi.</p>
	 *
	 * @return waktu perubahan terakhir, atau {@code null} bila kolomnya kosong di basis data
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Sekolah pemilik kelompok ini — dimensi tenant utama. Boleh {@code null} (kelompok "global"
	 * yang tetap ikut tampil pada combobox sekolah mana pun, lihat tapis
	 * {@code isNull("sekolah") OR eq("sekolah", s)} di {@code JamPelajaranAction}).
	 */
	private Sekolah sekolah;
	/**
	 * Saklar aktif/non-aktif. Kolomnya boleh {@code NULL}; seluruh pembaca memperlakukan
	 * {@code NULL} sebagai aktif. Lihat {@link #getAktif()}.
	 */
	private Boolean aktif;
	/**
	 * Yayasan pemilik. <b>Nilai turunan</b>, bukan masukan bebas: {@link #getYayasan()} menimpanya
	 * dari {@code sekolah.getYayasan()} setiap kali dibaca.
	 */
	private Yayasan yayasan;

	/**
	 * Nama kelompok — satu-satunya kolom {@code nullable = false} dan satu-satunya yang divalidasi
	 * wajib isi oleh layar master. Dipakai sebagai label combobox, kunci pengurutan seluruh
	 * pembaca, dan sebagian dari kunci pengelompokan di laporan jadwal. Tidak unik.
	 */
	private String nama;
	/**
	 * Keterangan bebas. Ikut diturunkan ke JasperReports sebagai parameter
	 * {@code kelompokJamPelajaran.keterangan}, jadi isinya bisa muncul pada jadwal tercetak.
	 */
	private String keterangan;
	/**
	 * Nomor urut tampilan. Dapat disunting langsung di grid layar master, namun
	 * <b>tidak pernah dipakai untuk mengurutkan apa pun</b> — lihat {@link #getNomorUrut()}.
	 */
	private Integer nomorUrut;

	/**
	 * Representasi teks objek: mengembalikan {@link #getNama()} apa adanya.
	 *
	 * <p><b>Dapat mengembalikan {@code null}</b> untuk objek baru yang namanya belum disetel —
	 * pemanggil yang merangkai string sebaiknya lewat {@code String.valueOf(...)}. Pemakaian nyata
	 * di kode ini aman: combobox diisi {@code Common.insertCombo} dengan properti eksplisit
	 * {@code "nama"}/{@code "sekolah"}, bukan lewat {@code toString()}.</p>
	 *
	 * @return nama kelompok, atau {@code null} bila belum terisi
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi saat memuat baris.
	 * Dipakai juga oleh layar master ketika pengguna menekan tombol "Tambah"
	 * ({@code KelompokJamPelajaranAction.onAdd(...)} membuat {@code new KelompokJamPelajaran()}).
	 * Seluruh field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang terisi waktu
	 * pembuatan objek.
	 */
	public KelompokJamPelajaran() {
	}

	/**
	 * Konstruktor lengkap warisan hbm2java untuk kolom-kolom wajib.
	 *
	 * <p><b>Tidak dipakai di mana pun</b> pada basis kode saat ini (satu-satunya instansiasi manual
	 * memakai konstruktor kosong). Dipertahankan karena menghapusnya akan memutus kompatibilitas
	 * biner bagi kode luar yang mungkin masih memanggilnya.</p>
	 *
	 * <p>Perhatikan {@code sekolah} melewati penjaga yang sama dengan {@link #setSekolah(Sekolah)}:
	 * objek sekolah yang belum tersimpan (id masih {@code null}) <b>dibuang menjadi {@code null}</b>
	 * alih-alih disimpan sebagai referensi transien. Ini mencegah Hibernate mencoba menyimpan
	 * kaskade objek yang belum berid, dengan konsekuensi kepemilikan tenant hilang diam-diam bila
	 * pemanggil mengira sekolahnya ikut terpasang.</p>
	 *
	 * @param id      nilai kunci utama yang ingin dipasang (tipe primitif, jadi tidak boleh
	 *                {@code null})
	 * @param sekolah sekolah pemilik; diabaikan menjadi {@code null} bila argumennya {@code null}
	 *                atau id-nya masih {@code null}
	 * @param nama    nama kelompok
	 */
	public KelompokJamPelajaran(long id, Sekolah sekolah, String nama) {
		this.id = id;
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolomnya {@code insertable = false} — nilainya sepenuhnya dibangkitkan basis data lewat
	 * {@code IDENTITY}, sehingga id yang disetel manual sebelum INSERT tidak akan ikut terkirim.
	 * Karena berurutan dan mudah ditebak, id ini <b>tidak boleh</b> dijadikan satu-satunya pembatas
	 * akses pada jalur apa pun.</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum pernah disimpan
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
	 * <p>Praktis hanya dipakai Hibernate sendiri; kode aplikasi tidak perlu memanggilnya karena
	 * id dibangkitkan basis data.</p>
	 *
	 * @param id nilai kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik kelompok ini, setelah diresolusi dari proxy lazy lewat
	 * {@code GeneralValueObject.check(...)}.
	 *
	 * <p><b>Efek samping ringan.</b> Hasil {@code check(...)} ditulis balik ke field
	 * {@code sekolah}, jadi objek yang dikembalikan bisa berupa instance <i>berbeda</i> dari yang
	 * tersimpan sebelumnya (instance kanonik dari cache identitas entity). Efek ini bersifat
	 * mengganti-referensi, bukan mengubah nilai kolom, sehingga tidak memicu UPDATE dengan
	 * sendirinya. {@code check(...)} tidak pernah melempar exception dan tidak pernah mengembalikan
	 * {@code null} untuk argumen non-null — kegagalan resolusi bersifat senyap.</p>
	 *
	 * <p>Relasi {@code LAZY} dengan kaskade {@code PERSIST}/{@code MERGE}: menyimpan kelompok ikut
	 * menyimpan/menggabungkan objek sekolah yang terpasang, tetapi <b>tidak</b> menghapusnya.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} untuk kelompok "global" tanpa sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan <b>penjaga non-trivial</b>: argumen {@code null} <i>atau</i>
	 * objek sekolah yang id-nya masih {@code null} (belum tersimpan) sama-sama disimpan sebagai
	 * {@code null}.
	 *
	 * <p>Tujuannya menghindari kaskade {@code PERSIST} ke objek transien. Konsekuensinya: memasang
	 * sekolah yang belum tersimpan <b>tidak menghasilkan kesalahan apa pun</b>, kelompoknya hanya
	 * berakhir tanpa pemilik. Layar master memanggil setter ini dengan objek hasil pilihan combobox
	 * yang selalu sudah berid, jadi jalur normal tidak terdampak.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik — <b>bukan getter polos</b>: nilainya diturunkan ulang dari
	 * sekolah pada setiap pembacaan.
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memanggil {@link #getSekolah()} (sehingga proxy sekolah ikut teresolusi) dan menyimpan
	 *       hasilnya kembali ke field {@code sekolah}.</li>
	 *   <li>Bila sekolahnya tidak {@code null}, field {@code yayasan} <b>ditimpa</b> dengan
	 *       {@code sekolah.getYayasan()}. Nilai yayasan yang disetel pengguna diabaikan sepenuhnya
	 *       selama sekolahnya terisi.</li>
	 *   <li>Hasilnya diresolusi lagi lewat {@code check(...)} lalu dikembalikan.</li>
	 * </ol>
	 * Bila {@code sekolah} bernilai {@code null}, nilai {@code yayasan} yang sudah ada dibiarkan
	 * apa adanya — jadi kelompok tanpa sekolah tetap dapat memiliki yayasan sendiri.</p>
	 *
	 * <p><b>Efek samping yang perlu disadari.</b> Karena Hibernate memakai <i>property access</i>
	 * (anotasi dipasang pada getter), method inilah yang dibaca saat <i>dirty checking</i>.
	 * Artinya bila kolom {@code yayasan_id} di basis data pernah menyimpang dari yayasan milik
	 * sekolahnya, sekadar memuat lalu mem-flush baris ini sudah cukup untuk memperbaikinya di
	 * basis data — sekaligus menghasilkan UPDATE nyata, revisi Envers baru, dan pembaruan
	 * {@code oleh}/{@code tanggal_dirubah} lewat {@link #onUpdate()}, tanpa ada pengguna yang
	 * benar-benar menyunting apa pun. Perilaku ini disengaja dan konsisten dengan entity sekolah
	 * lainnya: yayasan diperlakukan sebagai turunan sekolah, bukan masukan independen.</p>
	 *
	 * <p>Penugasan {@code sekolah = getSekolah();} di awal method sebenarnya redundan (getter-nya
	 * sudah menulis ke field yang sama) namun tidak berbahaya.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila sekolah maupun yayasan tidak terpasang
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
	 * Menyetel yayasan pemilik, dengan penjaga yang sama seperti {@link #setSekolah(Sekolah)}:
	 * argumen {@code null} atau objek yang id-nya masih {@code null} disimpan sebagai {@code null}.
	 *
	 * <p><b>Nilainya mudah tertimpa.</b> Selama {@code sekolah} terisi, {@link #getYayasan()} akan
	 * menurunkan ulang yayasan dari sekolah dan mengabaikan apa pun yang disetel di sini. Layar
	 * master tetap memanggil setter ini dari combobox Yayasan, tetapi combobox tersebut
	 * {@code readonly} dan dipasangkan dengan combobox Sekolah, jadi hasil akhirnya sama.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan nama kelompok apa adanya — getter polos, tanpa efek samping.
	 *
	 * <p>Perlu ditegaskan karena beberapa entity sekerabat memiliki {@code getNama()} yang
	 * <i>destruktif</i> (menimpa nama menjadi kosong saat dirender); di kelas ini pola itu
	 * <b>tidak ada</b>.</p>
	 *
	 * <p>Kolomnya {@code nullable = false}, sehingga baris yang tersimpan selalu punya nama —
	 * kecuali objek baru yang belum melewati validasi layar master.</p>
	 *
	 * @return nama kelompok
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama kelompok. Tanpa validasi maupun pemangkasan spasi.
	 *
	 * <p>Kewajiban isi dan keunikan bukan urusan setter ini: {@code onSave} layar master hanya
	 * menolak nama yang kosong/spasi saja, dan <b>tidak</b> memeriksa duplikasi — dua kelompok
	 * bernama sama pada sekolah yang sama tetap bisa dibuat, dan keduanya akan tampil identik pada
	 * combobox Jam Pelajaran.</p>
	 *
	 * @param nama nama kelompok
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan status aktif kelompok, dengan <b>bawaan aman</b>: kolom {@code NULL}
	 * diperlakukan sebagai {@code true}.
	 *
	 * <p><b>Tidak menulis balik.</b> Berbeda dari {@link #getNomorUrut()}, method ini hanya
	 * mengembalikan nilai pengganti tanpa menyentuh field, sehingga pembacaan tidak pernah
	 * mengubah data.</p>
	 *
	 * <p><b>Konsisten dengan pembacanya di SQL.</b> Kedua pembaca yang benar-benar menyaring
	 * berdasarkan kolom ini — combobox {@code JamPelajaranAction} dan daftar kelompok di
	 * {@code LaporanJadwalPelajaran} — memakai {@code isNull("aktif") OR eq("aktif", true)},
	 * yaitu aturan yang sama persis. Jadi <b>tidak ada</b> divergensi "kelompok hantu" seperti
	 * yang pernah ditemukan pada entity lain: baris {@code NULL} tampil di layar maupun di kueri.</p>
	 *
	 * <p><b>Yang dipengaruhi saklar ini:</b> hanya <i>penawaran</i> kelompok pada formulir Jam
	 * Pelajaran dan pemblokan laporan jadwal. Menonaktifkan kelompok <b>tidak</b> melepas
	 * {@code JamPelajaran} yang sudah terlanjur menunjuk ke sana; baris jam pelajaran itu tetap
	 * membawa FK-nya, tetapi bloknya berhenti muncul di laporan jadwal.</p>
	 *
	 * <p><b>Catatan layar master:</b> centang "Aktif" pada grid daftar menulis kolom ini langsung
	 * lewat {@link #setAktif(Boolean)} + {@code Common.refreshSaveOrUpdate(...)}, sementara kotak
	 * centang filter "Tampilkan hanya yang aktif" di kartu pencarian tidak berfungsi sama sekali
	 * (lihat catatan kuirk pada Javadoc kelas).</p>
	 *
	 * @return {@code true} bila kelompok aktif atau kolomnya masih {@code NULL}; {@code false}
	 *         hanya bila memang dinonaktifkan secara eksplisit. Tidak pernah {@code null}.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif kelompok. Tanpa validasi; {@code null} diterima dan kemudian dibaca
	 * sebagai aktif oleh {@link #getAktif()}.
	 *
	 * <p>Dipanggil dari event {@code onCheck} centang "Aktif" di grid layar master, yang langsung
	 * diikuti penyimpanan — jadi satu klik pada grid sudah menghasilkan UPDATE, revisi Envers, dan
	 * pembaruan jejak audit.</p>
	 *
	 * @param aktif status aktif baru; {@code null} berarti kembali ke perilaku bawaan (aktif)
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan keterangan bebas kelompok apa adanya — getter polos, tanpa efek samping dan
	 * tanpa nilai pengganti.
	 *
	 * <p>Ditegaskan karena pada beberapa entity lain di repo ini {@code getKeterangan()} membalik
	 * kontraknya (mengembalikan sesuatu selain field yang senama); di kelas ini <b>tidak</b>.</p>
	 *
	 * <p>Dapat bernilai {@code null}. Kedua pemakaian nyatanya aman terhadap itu: grid layar master
	 * membungkusnya dengan {@code Label} ZK (yang memperlakukan {@code null} sebagai teks kosong),
	 * dan {@code LaporanJadwalPelajaran} menormalkannya menjadi string kosong sebelum diserahkan
	 * ke JasperReports sebagai parameter {@code kelompokJamPelajaran.keterangan}.</p>
	 *
	 * @return keterangan kelompok, atau {@code null} bila tidak diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas kelompok. Tanpa validasi maupun pembatasan panjang di sisi Java.
	 *
	 * <p>Diisi dari textbox tiga baris pada formulir layar master; isinya dapat ikut tercetak pada
	 * laporan jadwal, jadi jangan diperlakukan sebagai catatan internal yang tak terlihat.</p>
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nomor urut tampilan, dengan bawaan {@code 1} — <b>dan menulis bawaan itu balik
	 * ke field</b>.
	 *
	 * <p><b>Efek samping.</b> Bila {@code nomorUrut} masih {@code null}, method ini menyetelnya
	 * menjadi {@code 1} sebelum mengembalikannya. Karena Hibernate memakai <i>property access</i>,
	 * getter inilah yang dibaca saat <i>dirty checking</i>: sekadar memuat lalu mem-flush baris
	 * yang kolom {@code nomor_urut}-nya masih {@code NULL} akan menghasilkan UPDATE nyata ke
	 * {@code 1}, satu revisi Envers baru, dan pembaruan {@code oleh}/{@code tanggal_dirubah} lewat
	 * {@link #onUpdate()} — tanpa ada pengguna yang menyunting apa pun. Sesudah flush pertama itu
	 * kolomnya tidak lagi {@code NULL}, jadi gejalanya hanya muncul sekali per baris dan mudah
	 * terlewat saat menelusuri riwayat audit yang "berubah sendiri".</p>
	 *
	 * <p><b>Ternary yang tidak pernah berperan.</b> Ekspresi {@code nomorUrut == null ? 1 : nomorUrut}
	 * pada baris {@code return} sudah pasti mengambil cabang kedua, karena blok {@code if} tepat di
	 * atasnya memastikan field-nya tidak {@code null}. Sisa penulisan bertahap; tidak berbahaya,
	 * tetapi jangan dibaca sebagai penjaga tambahan.</p>
	 *
	 * <p><b>Nilai yang tidak pernah dipakai untuk mengurutkan.</b> Kolom "Urut" dapat disunting
	 * langsung di grid layar master (setiap perubahan angka langsung disimpan), namun tidak satu
	 * pun pembaca memakainya sebagai kunci pengurutan: layar master mengurutkan
	 * {@code Order.asc("nama")}, combobox di {@code JamPelajaranAction} diisi lewat
	 * {@code Common.insertCombo} berdasarkan properti {@code nama}/{@code sekolah}, dan
	 * {@code LaporanJadwalPelajaran} juga memakai {@code Order.asc("nama")}. Satu-satunya tempat
	 * lain nilai ini muncul adalah kolom ekspor Excel. Praktisnya kolom ini <b>yatim fungsional</b>:
	 * mengubahnya tidak menggeser urutan apa pun di layar maupun di cetakan.</p>
	 *
	 * @return nomor urut tampilan; tidak pernah {@code null} (minimal {@code 1})
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampilan. Tanpa validasi — nilai {@code null}, nol, maupun negatif
	 * diterima apa adanya (dan {@code null} akan dinormalkan menjadi {@code 1} pada pembacaan
	 * berikutnya oleh {@link #getNomorUrut()}).
	 *
	 * <p>Dipanggil dari event {@code onChange} kotak angka kolom "Urut" di grid layar master, yang
	 * langsung diikuti penyimpanan. Ingat bahwa nilainya tidak memengaruhi urutan apa pun — lihat
	 * {@link #getNomorUrut()}.</p>
	 *
	 * @param nomorUrut nomor urut tampilan; boleh {@code null}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

}
