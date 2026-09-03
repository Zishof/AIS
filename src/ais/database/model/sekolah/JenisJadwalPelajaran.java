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
 * Master <b>jenis jadwal pelajaran</b> pada modul sekolah — tabel
 * {@code sekolah.jenis_jadwal_pelajaran}.
 *
 * <h3>Peran domain (TERVERIFIKASI)</h3>
 * <p>Entity ini adalah <b>katalog kategori jadwal</b>: label bebas yang membedakan ragam jadwal
 * yang berlaku di sebuah sekolah. Panduan pengguna resmi berkas
 * {@code /WEB-INF/bantuan/jenis_jadwal_pelajaran.html} menyebut contohnya secara eksplisit —
 * "jadwal reguler, jadwal ujian, jadwal kegiatan khusus, atau jadwal pada masa tertentu"
 * (mis. Ramadhan). Isinya hanya nama, keterangan, pemilik tenant, dan saklar aktif; tidak ada
 * satu pun aturan waktu, hari, atau kalender yang tersimpan di sini.</p>
 *
 * <p><b>Koreksi penting terhadap nama kelasnya.</b> Meski bernama "JenisJadwalPelajaran",
 * satu-satunya kolom di seluruh basis data yang menunjuk ke tabel ini adalah
 * {@code sekolah.jam_pelajaran.jenis_jadwal_pelajaran_id} — properti
 * {@code JamPelajaran.jenisJadwalPelajaran}, dan FK itu <b>WAJIB</b> ({@code nullable = false}).
 * Entity {@code JadwalPelajaran} sendiri <b>tidak memiliki relasi apa pun</b> ke kelas ini.
 * Jadi yang sesungguhnya dikategorikan adalah <b>slot jam</b> ("Jam ke-1", 07.00–07.45), bukan
 * baris jadwal. Menu aplikasi konsisten dengan kenyataan itu: label menunya
 * <b>"Jenis Jam Pelajaran"</b> ({@code ais.common.MenuInitializer} id 865429), begitu pula label
 * formulir pada layar Jam Pelajaran ("Jenis Jam Pelajaran&nbsp;*"). Penamaan di aplikasi memang
 * tidak seragam: judul halaman dan kolom grid memakai "Jenis Jadwal Pelajaran", judul dialog
 * memakai "Tambah/Ubah Jenis Jadwal", sedangkan pesan validasi pada layar Jam Pelajaran memakai
 * "Jenis Jadwal harus diisi" — ketiganya menunjuk entity yang sama.</p>
 *
 * <p><b>Konsekuensi praktis dari letak FK tersebut.</b> Sebuah baris {@code JadwalPelajaran}
 * menyimpan hingga dua belas rujukan slot jam ({@code jamPelajaran} … {@code jamPelajaran12});
 * jenis jadwal baru terlihat secara <i>tidak langsung</i> lewat rantai
 * {@code jadwalPelajaran.jamPelajaranN.jenisJadwalPelajaran}. Karena itu tidak ada mekanisme
 * apa pun yang mencegah satu baris jadwal mencampur slot dari dua jenis berbeda (mis. sebagian
 * slot "Reguler", sebagian "Ujian"), dan tidak ada satu pun kueri yang menyaring
 * {@code JadwalPelajaran} berdasarkan jenis. Model "jadwal reguler dipisahkan dari jadwal ujian"
 * yang dijanjikan panduan pengguna hanya terwujud sejauh administrator disiplin membuat set
 * {@code JamPelajaran} terpisah per jenis.</p>
 *
 * <h3>Kerabat terdekat</h3>
 * <p>Bersama {@link KelompokJamPelajaran} kelas ini membentuk <b>dua dimensi pengelompokan</b>
 * yang independen atas {@link JamPelajaran}: jenis jadwal bersifat WAJIB dan menjawab
 * "slot ini untuk keperluan apa" (Reguler/Ujian/Ramadhan), sedangkan kelompok jam bersifat
 * OPSIONAL ({@code kelompok_jam_pelajaran_id} nullable) dan menjawab "slot ini masuk blok cetak
 * mana". Keduanya diisi dari formulir yang sama di layar Jam Pelajaran, dengan tapis combobox
 * yang identik bentuknya. Bedanya: kelompok jam dibaca laporan cetak jadwal
 * ({@code LaporanJadwalPelajaran}) untuk memecah blok, sedangkan jenis jadwal <b>tidak dibaca
 * satu pun laporan</b> — pembaca non-master satu-satunya adalah dasbor dan helper timetable
 * (lihat daftar di bawah).</p>
 *
 * <h3>Siapa yang memakai</h3>
 * <ul>
 * <li><b>Layar master</b> {@code ais.action.master.sekolah.JenisJadwalPelajaranAction} +
 * {@code /pages/master/sekolah/jenis_jadwal_pelajaran.zul} — CRUD penuh; grid berkolom
 * Nama Jenis Jadwal Pelajaran / Sekolah / Keterangan / Aktif, formulir berisi Nama, Yayasan,
 * Sekolah, dan Keterangan (tanpa kotak centang Aktif — lihat catatan pada {@link #getAktif()}).</li>
 * <li><b>Layar Jam Pelajaran</b> {@code ais.action.master.sekolah.JamPelajaranAction} — combobox
 * wajib "Jenis Jam Pelajaran", diisi
 * {@code Common.insertCombo(..., JenisJadwalPelajaran.class, ...)} dengan tapis
 * <i>sekolah cocok ATAU sekolah NULL</i> DAN <i>aktif true ATAU aktif NULL</i>. Label item
 * combo digabung dari {@code nama} + {@code sekolah}.</li>
 * <li><b>Layar Jadwal Pelajaran &amp; Guru Mengajar</b>
 * ({@code JadwalPelajaranAction}, {@code GuruMengajarAction}) — memakai kelas ini <b>hanya
 * sebagai potongan label</b>: nama jenis ikut dirangkai ke dalam teks item combobox
 * {@code JamPelajaran} ({@code new String[]{"nama","mulaiS","sampaiS","jenisJadwalPelajaran"}}),
 * masing-masing untuk 12 dan 25 slot. Tidak ada penyaringan maupun penyimpanan nilai jenis di
 * kedua layar itu.</li>
 * <li><b>Dasbor</b> {@code ais.action.master.dashboard.admin.DasboardJadwalPelajaran} — tabel
 * ringkas "Top Jenis Jadwal/Jam", yaitu jumlah {@code JamPelajaran} aktif dikelompokkan menurut
 * {@code jenis.nama}.</li>
 * <li><b>Helper timetable</b>
 * {@code ais.action.master.sekolah.helper.TimetableJadwalPelajaranWindow} — tombol "buat waktu
 * default" membuat 10 slot {@code JamPelajaran} sekaligus dan harus mengisi FK wajib ini; jenis
 * yang dipakai dipilih otomatis oleh {@code jenisDefault(Session, Sekolah)} (lihat catatan bug
 * lintas-tenant di bawah).</li>
 * <li><b>Pramuat cache</b> {@code ais.common.InitData} — kelasnya terdaftar pada
 * {@code initClasses(...)} sehingga barisnya dimuat ke cache aplikasi saat startup. Ini murni
 * pramuat; <b>tidak ada auto-seed</b>.</li>
 * </ul>
 *
 * <p><b>Akibat "tidak ada auto-seed" + FK wajib.</b> Instalasi baru dimulai tanpa satu pun jenis
 * jadwal, sementara {@code JamPelajaran.jenis_jadwal_pelajaran_id} tidak boleh {@code NULL}.
 * Selama tabel ini kosong, <b>tidak satu pun jam pelajaran dapat dibuat</b> — baik lewat layar
 * Jam Pelajaran (validasi "Jenis Jadwal harus diisi") maupun lewat helper timetable (yang
 * menampilkan pesan "Belum ada 'Jenis Jadwal Pelajaran'. Buat satu dahulu di master data").
 * Master ini karenanya merupakan prasyarat keras seluruh rantai penjadwalan sekolah, bukan
 * pelengkap opsional.</p>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 * <ol>
 * <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Field-nya dideklarasikan ulang di sini
 * (lihat catatan tentang {@link GeneralValueObject} di bawah).</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()} dan {@link #getYayasan()}; yayasan
 * diturunkan ulang dari sekolah pada setiap pembacaan.</li>
 * <li><b>Atribut deskriptif</b> — {@link #getNama()}, {@link #getKeterangan()},
 * {@link #getAktif()}.</li>
 * <li><b>Utilitas</b> — dua konstruktor. Kelas ini <b>tidak</b> memiliki {@code toString()},
 * tidak memiliki koleksi, tidak memiliki method bisnis, dan tidak memiliki query statis.</li>
 * </ol>
 * <p>Seluruh perilaku non-trivial terkonsentrasi pada empat accessor: {@link #getSekolah()},
 * {@link #getYayasan()}, {@link #getAktif()}, dan pasangan setter
 * {@link #setOleh(String)}/{@link #setOlehId(String)}.</p>
 *
 * <h3>Catatan penting tentang {@link GeneralValueObject}</h3>
 * <p>Induknya <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — hanya POJO abstrak
 * biasa, sehingga Hibernate <b>tidak</b> memetakan satu pun properti induk. Karena itu field
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b>
 * dideklarasikan ulang di setiap entity turunan. Duplikasi ini <b>bukan bug</b>, melainkan
 * keharusan teknis; jangan "dirapikan" dengan memindahkannya ke induk. Yang tetap diwarisi
 * adalah helper statis, terutama {@code check(...)} yang dipakai kedua getter relasi di sini.</p>
 *
 * <h3>Pola arsitektur berulang — hasil verifikasi pada berkas ini</h3>
 * <ul>
 * <li><b>Getter dengan efek tulis balik (write-back)</b> — <b>ADA, satu buah</b>:
 * {@link #getYayasan()} menurunkan ulang yayasan dari sekolah pada setiap pembacaan dan
 * menyimpannya ke field. Karena Hibernate membaca entity ini lewat property access, pembacaan
 * biasa dapat berubah menjadi UPDATE nyata plus revisi Envers baru. {@link #getSekolah()}
 * juga menulis ke field, tetapi hanya menukar proxy lazy dengan instance teresolusi
 * (baris logis yang sama), sehingga tidak mengubah nilai kolom.</li>
 * <li><b>Getter destruktif yang mengosongkan data</b> (pola {@code KelasSiswaPSB.getNama()}) —
 * <b>TIDAK ADA</b>. {@link #getNama()} murni mengembalikan field.</li>
 * <li><b>{@code getKeterangan()} yang membalik kontraknya</b> — <b>TIDAK ADA</b>;
 * {@link #getKeterangan()} adalah getter polos.</li>
 * <li><b>Penciutan {@code TreeSet}</b> — <b>TIDAK RELEVAN</b>: kelas ini tidak punya koleksi.</li>
 * <li><b>"Kolom aktif tak pernah ditulis layar master"</b> — <b>ADA secara struktur, TETAPI
 * TIDAK BERAKIBAT</b>. {@code JenisJadwalPelajaranAction.onSave()} memang tidak pernah memanggil
 * {@link #setAktif(Boolean)} (formulirnya tidak punya kotak centang Aktif), sehingga baris baru
 * selalu tersimpan dengan {@code aktif = NULL}. Berbeda dengan {@code JenisCatatanSiswa},
 * {@code JenisNilaiSiswa}, dan {@code JenisLaporanJadwalSekolah}, <b>kedua</b> pembaca entity ini
 * menulis tapisnya secara toleran-NULL
 * ({@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))}) dan
 * {@link #getAktif()} memetakan {@code NULL} menjadi {@code true}. Jadi baris baru langsung
 * terpakai, dan bug "harus ditekan dua kali" TIDAK muncul di sini. Berkas ini karena itu
 * berguna sebagai <b>contoh pembanding yang benar</b> saat ketiga berkas lain diperbaiki.</li>
 * <li><b>Pewarisan hak lewat menu induk</b> — <b>TIDAK ADA</b>. Berkas ZUL ini tidak pernah
 * di-{@code MyInclude} dari layar mana pun; ia memiliki entri menunya sendiri
 * ("Jenis Jam Pelajaran", id 865429, induk 5700 "Setup"), sehingga
 * {@code CommonPrivilages.checkPrevilages(...)} menguji hak menu yang benar.</li>
 * <li><b>{@code Intbox} nomor urut tanpa gerbang</b> (temuan pada
 * {@code JenisAktiftasHarianDefault}) — <b>TIDAK RELEVAN</b>: kelas ini tidak punya kolom nomor
 * urut sama sekali, dan satu-satunya komponen yang dapat diubah dari grid — kotak centang
 * Aktif — <b>digerbangi dengan benar</b> ({@code checkbox.setDisabled(!edit)}).</li>
 * </ul>
 *
 * <h3>Hak akses dan cakupan tenant</h3>
 * <p><b>Gerbang privilese: BENAR.</b> Layar master memanggil {@code Common.doCheckSecurity()} di
 * {@code doBeforeCompose}, menyembunyikan tombol Tambah tanpa hak {@code CREATE}, menonaktifkan
 * kotak centang Aktif dan tombol Ubah/Hapus tanpa hak {@code UPDATE}/{@code DELETE}, serta
 * menampilkan tombol unggah massal hanya bila ketiga hak dimiliki sekaligus. Ini salah satu layar
 * master yang paling rapi gerbangnya sejauh audit berjalan.</p>
 *
 * <p><b>Nol filter tenant pada pencarian.</b> {@code initCriteria()} hanya menambahkan pembatas
 * untuk nilai yang <i>dipilih pengguna</i> pada combobox Yayasan/Sekolah; bila dibiarkan kosong,
 * pembatasnya menjadi {@code Restrictions.sqlRestriction("1=1")}. Tidak ada pembatas bawaan ke
 * sekolah/yayasan milik pengguna, sehingga pemegang hak BACA menu ini melihat — dan dengan hak
 * UBAH/HAPUS dapat mengubah atau menghapus — jenis jadwal milik <b>seluruh sekolah dan yayasan</b>
 * pada satu instalasi. Isinya metadata penjadwalan (nama, keterangan), bukan data pribadi,
 * sehingga dampak kerahasiaannya rendah; dampak <b>integritasnya</b> tidak nol — menghapus jenis
 * milik sekolah lain memutus FK wajib seluruh {@code JamPelajaran} sekolah tersebut. Mekanismenya
 * sama persis dengan yang sudah tercatat pada {@code RuangPSB}/{@code KelasSiswaPSB}/
 * {@code KelompokJamPelajaran}: bukan tapis yang gagal terbuka, melainkan tapis yang memang tidak
 * pernah ditulis. Dasbor {@code DasboardJadwalPelajaran} menambah varian fail-open yang sudah
 * dikenal ({@code applySekolahFilter} langsung {@code return} bila {@code currentSekolah == null},
 * yaitu saat pengguna memilih "Semua Sekolah").</p>
 *
 * <h3>Kuirk dan jebakan yang perlu diketahui</h3>
 * <ol>
 * <li><b>Baris global tidak dapat dibuat lewat UI, tetapi dapat dirusak lewat UI.</b> Tapis
 * combobox di layar Jam Pelajaran menerima baris dengan {@code sekolah = NULL} sebagai "berlaku
 * untuk semua sekolah", namun {@code onSave()} layar master <b>mewajibkan</b> Yayasan dan Sekolah
 * terisi. Akibatnya baris global hanya bisa lahir dari impor/skrip — dan begitu seseorang membuka
 * lalu menyimpan baris global itu dari layar master, baris tersebut <b>berubah permanen menjadi
 * milik satu sekolah</b> dan lenyap dari combobox seluruh sekolah lain. Pola yang sama sudah
 * tercatat pada {@code JenisMateriHarianDefault}.</li>
 * <li><b>Yayasan pada formulir praktis tidak berarti.</b> {@code onSave()} menyimpan yayasan
 * pilihan pengguna, tetapi {@link #getYayasan()} menimpanya dengan
 * {@code sekolah.getYayasan()} pada pembacaan berikutnya. Selama sekolah terisi (dan itu wajib),
 * nilai combo Yayasan tidak pernah bertahan bila berbeda.</li>
 * <li><b>{@code keterangan} tanpa {@code @Column(length = ...)}.</b> Kolomnya dibuat
 * {@code varchar(255)} bawaan JPA, sementara formulir menyediakan {@code Textbox} tiga baris
 * tanpa {@code maxlength}. Keterangan panjang gagal disimpan dengan galat "value too long".
 * Bandingkan {@code JamPelajaran.keterangan} yang eksplisit {@code length = 10000}.</li>
 * <li><b>Pemilihan jenis default lintas tenant pada helper timetable.</b>
 * {@code TimetableJadwalPelajaranWindow.jenisDefault(Session, Sekolah)} mencari jenis milik
 * sekolah yang bersangkutan; bila tidak ada, tahap kedua mengambil
 * {@code createCriteria(JenisJadwalPelajaran.class).setMaxResults(1).uniqueResult()}
 * <b>tanpa tapis apa pun</b>. Jam pelajaran sekolah A karena itu dapat lahir menunjuk jenis
 * jadwal milik sekolah B, dan sejak saat itu tidak pernah muncul pada combobox sekolah A
 * (tapisnya menuntut sekolah cocok atau NULL) sehingga tampak "kosong" saat diedit.</li>
 * <li><b>Komentar generator yang menyesatkan.</b> Baris "generated by hbm2java" yang dulu berdiri
 * sebagai Javadoc kelas ini <b>ikut tersalin</b> ke {@code KompetensiDasarMatapelajaran.java},
 * yang sampai kini memuat teks "JenisJadwalPelajaran generated by hbm2java" untuk kelas yang
 * sama sekali berbeda. Jejak salin-tempel serupa sudah tercatat pada {@code JenisGuru}.</li>
 * </ol>
 *
 * @see JamPelajaran
 * @see KelompokJamPelajaran
 * @see JadwalPelajaran
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "jenis_jadwal_pelajaran", schema = "sekolah")
public class JenisJadwalPelajaran extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya harus tetap agar sesi ZK dan cache aplikasi yang sudah
	 * berisi objek versi lama tidak menolak instance baru dengan
	 * {@code InvalidClassException} — entity ini ikut diserialisasi ke sesi ZK (dipegang
	 * {@code JenisJadwalPelajaranAction}) maupun ke cache pramuat {@code InitData}.
	 */
	private static final long serialVersionUID = 5179120903470452362L;
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
	 * Menyetel nama pengguna pengubah terakhir, dengan <b>validasi non-trivial</b> yang sama
	 * dengan {@link #setOlehId(String)}: argumen {@code null} atau kosong/spasi diabaikan
	 * diam-diam.
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
	 * {@code oleh}/{@code olehId} dari jalur ini. Pada entity ini efeknya kentara: baris hasil
	 * "Tambah" selalu berkolom {@code oleh}/{@code olehid} kosong sampai pertama kali diubah.</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} — hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya.
	 * Field {@code tanggal_dirubah} sendiri adalah stempel waktu perubahan terakhir, juga
	 * dideklarasikan ulang karena {@link GeneralValueObject} tidak dipetakan Hibernate.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Normalnya diisi otomatis lewat
	 * {@link #onUpdate()}; pemanggilan manual hanya dipakai jalur impor/migrasi data.
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini. Dipetakan sebagai
	 * {@code TIMESTAMP} (tanggal sekaligus jam).
	 *
	 * @return waktu perubahan terakhir; untuk objek yang baru dibuat berisi waktu pembuatan
	 *         objek di JVM, bukan {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Sekolah pemilik baris ini (kolom {@code sekolah_id}, nullable di basis data). {@code NULL}
	 * berarti "berlaku untuk semua sekolah" bagi combobox layar Jam Pelajaran, tetapi layar
	 * master tidak pernah dapat menghasilkan nilai {@code NULL} — lihat catatan kuirk pada
	 * Javadoc kelas.
	 */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik baris ini (kolom {@code yayasan_id}). Bersifat <b>turunan</b>: nilainya
	 * dihitung ulang dari {@link #sekolah} setiap kali {@link #getYayasan()} dipanggil.
	 */
	private Yayasan yayasan;
	/**
	 * Nama jenis jadwal seperti yang diketik administrator, mis. "Reguler", "Ujian", "Ramadhan".
	 * Kolom {@code nama} bersifat {@code NOT NULL} dan menjadi label item combobox di layar Jam
	 * Pelajaran serta label pengelompok pada dasbor.
	 */
	private String nama;
	/**
	 * Keterangan bebas. Tidak pernah dibaca logika bisnis mana pun; hanya ditampilkan pada kolom
	 * grid layar master. Tidak memiliki {@code @Column(length = ...)} sehingga kolomnya
	 * {@code varchar(255)} bawaan.
	 */
	private String keterangan;
	/**
	 * Saklar aktif. {@code NULL} diperlakukan sebagai <b>aktif</b> oleh {@link #getAktif()}
	 * maupun oleh kedua kueri yang membaca entity ini; formulir tambah/ubah tidak pernah
	 * menulis field ini, hanya kotak centang pada grid yang menulisnya.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA sekaligus dipakai layar master saat
	 * menekan tombol "Tambah" ({@code JenisJadwalPelajaranAction.onAdd(...)}). Seluruh properti
	 * dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang langsung diisi waktu sekarang
	 * oleh inisialisasi field.
	 */
	public JenisJadwalPelajaran() {
	}

	/**
	 * Konstruktor peringkas untuk membuat instance dengan identitas dan nama sekaligus.
	 *
	 * <p><b>Tidak pernah dipakai.</b> Penelusuran seluruh sumber ({@code src/}, mirror
	 * {@code java/}, dan berkas {@code .zul}/{@code .jsp}) tidak menemukan satu pun pemanggil —
	 * sisa bawaan generator hbm2java. Perhatikan pula bahwa konstruktor ini <b>tidak</b> mengisi
	 * {@link #sekolah}, sehingga objek hasilnya tidak akan lolos validasi
	 * {@code JenisJadwalPelajaranAction.onSave()} bila dipakai apa adanya.</p>
	 *
	 * @param id   nilai kunci utama yang ingin dilekatkan (tipe primitif, jadi tidak boleh
	 *             {@code null})
	 * @param nama nama jenis jadwal
	 */
	public JenisJadwalPelajaran(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolomnya ditandai {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@code IDENTITY}); Hibernate tidak menyertakannya pada pernyataan INSERT. Nilai
	 * {@code null} karenanya menjadi penanda "baris belum tersimpan", dan layar master memakai
	 * tepat penanda itu untuk memilih judul dialog "Tambah Jenis Jadwal" atau "Ubah Jenis
	 * Jadwal" serta untuk memutuskan perlu-tidaknya {@code session.load(...)} sebelum menyimpan.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipakai Hibernate saat memuat/menyimpan baris; kode aplikasi
	 * sebaiknya tidak memanggilnya.
	 *
	 * @param id nilai kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini, sekaligus <b>meresolusi proxy lazy</b> lewat
	 * {@code GeneralValueObject.check(...)}.
	 *
	 * <p><b>Mengapa perlu.</b> Relasi ini {@code FetchType.LAZY}, sehingga di luar sesi Hibernate
	 * yang membuatnya nilai field bisa berupa proxy yang meledak
	 * ({@code LazyInitializationException}). {@code check(...)} berusaha menggantinya dengan
	 * instance nyata melalui cache identitas, sesi yang masih tersedia, atau — sebagai upaya
	 * terakhir — sesi baru; bila semuanya gagal, argumen dikembalikan apa adanya tanpa melempar
	 * exception.</p>
	 *
	 * <p><b>Efek samping.</b> Hasil resolusi <b>ditulis kembali</b> ke field {@link #sekolah}.
	 * Yang berubah hanyalah <i>instance</i>-nya, bukan baris yang ditunjuk, sehingga nilai kolom
	 * {@code sekolah_id} tidak berubah dan tidak ada revisi Envers yang lahir karenanya. Yang
	 * tetap perlu diingat: pembacaan getter ini dapat memicu kueri basis data.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila baris ini berlaku global
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan <b>normalisasi non-trivial</b>: objek {@code Sekolah} yang
	 * belum punya id (transient, mis. hasil pilihan combobox kosong) diperlakukan sama dengan
	 * {@code null} agar Hibernate tidak mencoba mem-{@code persist} baris sekolah baru lewat
	 * {@code CascadeType.PERSIST}.
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini, dengan <b>menurunkannya ulang dari sekolah pada
	 * setiap pembacaan</b>.
	 *
	 * <p><b>Cara kerja.</b> Method memanggil {@link #getSekolah()} lebih dulu (sehingga ikut
	 * meresolusi proxy sekolah); bila sekolah tidak {@code null}, field {@link #yayasan} ditimpa
	 * dengan {@code sekolah.getYayasan()}. Setelah itu hasilnya dilewatkan
	 * {@code GeneralValueObject.check(...)} untuk meresolusi proxy yayasan.</p>
	 *
	 * <p><b>Efek samping yang sesungguhnya (write-back).</b> Ini bukan getter murni: nilai kolom
	 * {@code yayasan_id} dapat <b>berubah</b> hanya karena baris dibaca. Karena entity ini
	 * dipetakan lewat property access, Hibernate memanggil getter ini saat <i>dirty checking</i>;
	 * bila yayasan tersimpan berbeda dari yayasan milik sekolahnya, pembacaan biasa (mis.
	 * merender satu halaman grid) berubah menjadi UPDATE nyata plus satu revisi Envers baru.
	 * Konsekuensi praktisnya: pilihan combobox "Yayasan" pada formulir tidak pernah bertahan bila
	 * berbeda dari yayasan sekolah yang dipilih — dan karena Sekolah wajib diisi, combobox itu
	 * praktis hanya hiasan.</p>
	 *
	 * <p>Bila {@link #getSekolah()} mengembalikan {@code null}, nilai yayasan yang sudah ada
	 * dipertahankan apa adanya (hanya diresolusi), sehingga baris global hasil impor tetap dapat
	 * menyimpan yayasan sendiri.</p>
	 *
	 * @return yayasan pemilik hasil penurunan dari sekolah, atau nilai tersimpan bila sekolah
	 *         {@code null}; dapat {@code null}
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
	 * Menyetel yayasan pemilik, dengan normalisasi yang sama seperti
	 * {@link #setSekolah(Sekolah)}: objek tanpa id diperlakukan sebagai {@code null}.
	 *
	 * <p><b>Perhatikan:</b> nilai yang disetel di sini <b>tidak dijamin bertahan</b> — lihat
	 * penjelasan write-back pada {@link #getYayasan()}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan nama jenis jadwal. Getter polos tanpa efek samping.
	 *
	 * <p>Kolomnya {@code NOT NULL} dan diisi lewat validasi wajib "Nama Jenis Jadwal harus diisi"
	 * di layar master. Tidak ada batasan keunikan pada tingkat basis data maupun aplikasi,
	 * sehingga dua jenis bernama sama — baik pada sekolah yang sama maupun berbeda — dapat
	 * berdampingan dan tampil identik di combobox layar Jam Pelajaran (label combo memang
	 * merangkai nama + sekolah, tetapi tidak menyaring duplikat).</p>
	 *
	 * @return nama jenis jadwal; tidak pernah {@code null} untuk baris yang sudah tersimpan
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama jenis jadwal.
	 *
	 * @param nama nama baru; tidak divalidasi di sini (validasi wajib-isi ada di layar master)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas. Getter polos — <b>tidak</b> membalik kontraknya seperti
	 * beberapa entity lain di repo ini, dan tidak pernah menyusun teks turunan.
	 *
	 * <p>Nilainya hanya dipakai untuk ditampilkan pada kolom "Keterangan" grid layar master dan
	 * ikut pada ekspor cetak. Tidak ada logika bisnis yang membacanya.</p>
	 *
	 * @return keterangan, dapat {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan teks keterangan; perhatikan kolomnya {@code varchar(255)} bawaan JPA
	 *                   sehingga teks lebih panjang gagal disimpan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif dengan <b>bawaan {@code true} untuk nilai {@code NULL}</b>.
	 *
	 * <p><b>Mengapa penting.</b> Formulir tambah/ubah layar master tidak memiliki kotak centang
	 * Aktif, sehingga {@code onSave()} tidak pernah memanggil {@link #setAktif(Boolean)} dan
	 * setiap baris baru lahir dengan kolom {@code aktif = NULL}. Pemetaan {@code NULL → true} di
	 * sini membuat baris baru langsung tampak aktif di grid, dan kedua kueri yang membaca entity
	 * ini menuliskan tapisnya secara toleran-NULL
	 * ({@code isNull("aktif") OR eq("aktif", true)}) sehingga baris baru juga langsung muncul di
	 * combobox layar Jam Pelajaran. Inilah sebabnya bug "kotak centang harus ditekan dua kali"
	 * yang tercatat pada {@code JenisCatatanSiswa}/{@code JenisNilaiSiswa}/
	 * {@code JenisLaporanJadwalSekolah} <b>tidak</b> terjadi di sini.</p>
	 *
	 * <p><b>Bukan write-back.</b> Nilai bawaan dikembalikan langsung tanpa ditulis ke field,
	 * sehingga membaca getter ini tidak pernah mengubah baris maupun melahirkan revisi Envers.
	 * Kolom di basis data tetap {@code NULL} sampai seseorang menekan kotak centang Aktif pada
	 * grid.</p>
	 *
	 * <p><b>Jangan dipakai untuk membangun kueri.</b> Karena nilai {@code NULL} tersembunyi di
	 * balik getter ini, setiap kueri baru yang menyaring jenis aktif harus tetap memakai bentuk
	 * toleran-NULL; {@code Restrictions.eq("aktif", true)} polos akan menyembunyikan hampir
	 * seluruh baris yang pernah dibuat lewat layar master.</p>
	 *
	 * @return {@code true} bila baris aktif atau kolomnya masih {@code NULL}; {@code false} hanya
	 *         bila secara eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif.
	 *
	 * <p>Satu-satunya pemanggil di aplikasi adalah pendengar {@code onCheck} kotak centang
	 * "Aktif" pada grid layar master, yang langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan seketika tanpa tombol
	 * Simpan. Kotak centang itu dinonaktifkan bagi pengguna tanpa hak {@code UPDATE}.</p>
	 *
	 * @param aktif status baru; {@code null} berarti kembali ke keadaan "belum pernah disetel"
	 *              yang oleh {@link #getAktif()} dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
