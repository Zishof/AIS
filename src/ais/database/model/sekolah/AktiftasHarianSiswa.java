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
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * <h1>AktiftasHarianSiswa &mdash; baris transaksi jurnal aktivitas harian siswa
 * (Buku Penghubung Digital)</h1>
 *
 * <p>Entity ini memetakan tabel <code>sekolah.aktiftas_harian_siswa</code> dan merupakan
 * <b>baris transaksi</b> &mdash; bukan katalog/master. Satu baris mewakili <b>satu siswa pada satu
 * tanggal</b>: rekapitulasi kegiatan harian anak yang diisi guru/pembina, ditambah dua kanal pesan
 * dua arah antara sekolah dan keluarga. Di layar pengguna, kumpulan baris ini disajikan sebagai
 * "Buku Penghubung" (satu baris = satu halaman buku), kalender bulanan per siswa, dasbor rekap,
 * dan halaman tanggapan orang tua.</p>
 *
 * <p><b>Catatan ejaan.</b> Nama kelas, nama tabel, dan nama kolom memakai ejaan
 * <i>Aktiftas</i>/<i>aktifitas</i> yang tidak konsisten satu sama lain (kelas
 * {@code AktiftasHarianSiswa}, properti {@code aktifitas}, action pemakai bernama campur
 * {@code AktiftasHarianSiswaAction} dan {@code DaftarAktifitasHarianSiswaAction}). Semuanya
 * merujuk objek yang sama; jangan "diperbaiki" tanpa migrasi skema.</p>
 *
 * <h2>Struktur kolom</h2>
 * <ul>
 *   <li><b>Identitas baris:</b> {@code id} (IDENTITY) dan {@code kode} (unik, non-null) &mdash;
 *       kode <i>diturunkan ulang</i> setiap kali dibaca, lihat {@link #getKode()}.</li>
 *   <li><b>Subjek dan waktu:</b> {@code siswa} (FK wajib ke {@link Siswa}) dan {@code tanggal}
 *       (DATE, wajib). Pasangan keduanya adalah kunci logis baris ini; keunikannya dijaga
 *       <i>hanya</i> oleh pemeriksaan aplikasi di layar utama, bukan oleh constraint DB.</li>
 *   <li><b>Denormalisasi:</b> {@code nama} (non-null) &mdash; bukan judul kegiatan, melainkan
 *       salinan nama siswa yang di-<i>refresh</i> pada setiap pembacaan, lihat
 *       {@link #getNama()}.</li>
 *   <li><b>Isi jurnal (dua kolom komposit JSON):</b> {@code aktifitas} dan {@code materi}
 *       bertipe <code>text</code>, masing-masing menyimpan satu dokumen JSON &mdash; lihat
 *       bagian berikutnya.</li>
 *   <li><b>Kanal pesan:</b> {@code pesan_pembina} (guru/pembina &rarr; keluarga) dan
 *       {@code pesan_orang_tua} (keluarga &rarr; sekolah), keduanya <code>text</code> bebas.</li>
 *   <li><b>Penanggung jawab:</b> {@code pembina1}, {@code pembina2}, {@code pembina3} &mdash;
 *       ketiganya FK opsional ke {@link Tbmuser} (akun, <b>bukan</b> entity {@code Guru}).</li>
 *   <li><b>Pelengkap:</b> {@code keterangan} (<code>text</code> bebas) dan {@code aktif}
 *       (penanda tampil/tidak, tidak dipetakan ke kolom &mdash; lihat {@link #getAktif()}).</li>
 *   <li><b>Jejak audit:</b> {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, ditambah
 *       {@code @Audited} (Envers) sehingga setiap versi baris tersimpan di tabel riwayat.</li>
 * </ul>
 *
 * <h2>Dua kolom komposit JSON &mdash; struktur TERVERIFIKASI</h2>
 * <p>Kolom {@code aktifitas} dan {@code materi} memakai bentuk yang sama persis: satu objek JSON
 * yang kuncinya adalah <b>nomor urut baris sebagai string</b> ("1", "2", "3", &hellip;) dan nilainya
 * objek dengan tepat dua field, {@code nama} dan {@code nilai}. Bentuk ini ditulis oleh
 * {@code AktiftasHarianSiswaAction.onSave()} dan dibaca ulang oleh seluruh layar pemakai:</p>
 * <pre>
 * aktifitas : {"1":{"nama":"Shalat Jamaah","nilai":"YA"},
 *              "2":{"nama":"Membaca Al-Quran","nilai":"TIDAK"},
 *              "3":{"nama":"Olahraga","nilai":""}}
 *
 * materi    : {"1":{"nama":"Tahfidz","nilai":"Sangat Baik"},
 *              "2":{"nama":"Hadits","nilai":"B"}}
 * </pre>
 * <p>Perbedaan keduanya hanya pada <i>domain nilai</i>: pada {@code aktifitas} nilai berasal dari
 * sepasang radio sehingga hanya bisa <code>"YA"</code>, <code>"TIDAK"</code>, atau
 * <code>""</code> (belum dipilih); pada {@code materi} nilai adalah teks bebas yang dalam praktik
 * diisi huruf mutu atau predikat ("A"/"B"/"C"/"D", "Sangat Baik", "Baik", "Cukup", "Kurang",
 * "Lancar") &mdash; daftar itulah yang diberi warna oleh
 * {@code CatatanOrangTuaAktiftasHarianAction.nilaiWarna()}. Baris dengan {@code nama} kosong
 * dilewati oleh perenderan dan agregasi, tetapi tetap ikut tersimpan.</p>
 * <p><b>Kuirk urutan.</b> Kunci JSON hanyalah nomor urut saat disimpan. Karena
 * {@link org.json.JSONObject} tidak menjamin urutan iterasi, urutan baris yang tampil kembali saat
 * baris ini dibuka ulang <i>tidak dijamin sama</i> dengan urutan saat diisi &mdash; dan begitu
 * disimpan ulang, nomor urut ditulis ulang mengikuti urutan tampil yang baru.</p>
 *
 * <h2>Hubungan ke katalog default: SALINAN NILAI, bukan FK</h2>
 * <p>Nama-nama butir pada kedua kolom JSON di atas berasal dari dua katalog master
 * {@link ais.database.model.sekolah.JenisAktiftasHarianDefault} (untuk {@code aktifitas}) dan
 * {@link ais.database.model.sekolah.JenisMateriHarianDefault} (untuk {@code materi}). Diverifikasi
 * dari sisi entity transaksi ini sendiri: <b>tidak ada satu pun {@code @JoinColumn} maupun properti
 * relasi ke kedua katalog itu</b> &mdash; satu-satunya relasi keluar entity ini adalah {@code siswa}
 * dan {@code pembina1..3}.</p>
 * <p>Katalog hanya dipakai sekali, yaitu ketika formulir dibuka untuk baris <b>baru</b>: layar
 * mengambil baris katalog yang {@code aktif}, terurut {@code nomorUrut} lalu {@code nama}, dan
 * menyalin kolom {@code nama}-nya menjadi teks awal pada petak isian. Sejak tombol Simpan ditekan,
 * teks itu <b>dibekukan</b> ke dalam JSON dan hidup terpisah dari katalognya. Konsekuensinya:</p>
 * <ul>
 *   <li>Mengganti nama, menonaktifkan, atau menghapus baris katalog <b>tidak mengubah</b> jurnal
 *       yang sudah tersimpan &mdash; riwayat lama tetap terbaca apa adanya (perilaku yang memang
 *       diinginkan untuk dokumen historis).</li>
 *   <li>Sebaliknya, tidak ada integritas referensial sama sekali: guru boleh mengetik nama butir
 *       bebas lewat tombol "Tambah Baris", sehingga agregasi dasbor yang mengelompokkan per
 *       {@code nama} akan memecah "Shalat Jamaah" dan "Sholat Jamaah" menjadi dua kategori
 *       berbeda.</li>
 *   <li>Penyaringan katalog per sekolah dilakukan di layar (baris katalog dengan {@code sekolah}
 *       NULL dianggap berlaku umum), bukan di entity ini &mdash; setelah disalin, jejak asal butir
 *       hilang seluruhnya.</li>
 * </ul>
 *
 * <h2>Jalur baca dan tulis</h2>
 * <ol>
 *   <li><b>{@code AktiftasHarianSiswaAction}</b> &mdash; layar utama
 *       (<code>/pages/master/sekolah/aktiftas_harian_siswa.zul</code>, satu-satunya menu untuk
 *       seluruh modul ini). Menyediakan daftar, formulir popup tambah/ubah, dan tombol unduh
 *       Excel. Formulir inilah yang menyusun kedua dokumen JSON.</li>
 *   <li><b>{@code DaftarAktifitasHarianSiswaAction}</b> &mdash; tab "Daftar Siswa": memilih siswa,
 *       menampilkan kalender bulanan yang menandai tanggal yang sudah berjurnal, lalu memanggil
 *       {@code AktiftasHarianSiswaAction.showPopupForm(...)} untuk tanggal yang diklik.</li>
 *   <li><b>{@code DashboardAktifitasHarianSiswaAction}</b> &mdash; tab dasbor rekap lintas siswa
 *       dengan penyaring yayasan/sekolah dan rentang tanggal.</li>
 *   <li><b>{@code BukuPenghubungSiswa}</b> &mdash; tab "Buku Penghubung": merender tiap baris
 *       sebagai satu halaman buku (aktivitas, materi, pesan pembina, kotak pesan orang tua yang
 *       bisa langsung disimpan).</li>
 *   <li><b>{@code CatatanOrangTuaAktiftasHarianAction}</b> + {@code CatatanOrangTuaServlet}
 *       &mdash; halaman ringkasan bulanan untuk orang tua (statistik, donat, tren, spider,
 *       heatmap) berikut kotak komentar harian. <b>Halaman ini dirancang tanpa login</b>; lihat
 *       catatan keamanan.</li>
 *   <li><b>{@code AktifitasHarianSiswaApi}</b> &mdash; layanan JSON untuk aplikasi mobile:
 *       {@code daftar}, {@code detail}, {@code simpan}, {@code pesanOrangTua},
 *       {@code pesanPembina}.</li>
 * </ol>
 * <p>Layar CRUD generik <i>tidak</i> dinyalakan untuk entity ini: {@code GenericCrudAkademikOverrides}
 * menahannya sebagai READ_ONLY dengan alasan yang tepat &mdash; formulir generik akan menampilkan
 * kedua kolom komposit sebagai teks bebas dan menghasilkan baris kosong pada layar yang justru ada
 * untuk mengisinya. Ini adalah contoh POSITIF pengendalian, bukan celah.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit warisan:</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}, {@link #setId(Long)}, {@link #getKode()},
 *       {@link #setKode(String)}, {@link #toString()}.</li>
 *   <li><b>Subjek dan waktu:</b> {@link #getSiswa()}, {@link #setSiswa(Siswa)},
 *       {@link #getTanggal()}, {@link #setTanggal(Date)}, {@link #getNama()},
 *       {@link #setNama(String)}.</li>
 *   <li><b>Isi jurnal:</b> {@link #getAktifitas()}, {@link #setAktifitas(String)},
 *       {@link #getMateri()}, {@link #setMateri(String)}, {@link #getKeterangan()},
 *       {@link #setKeterangan(String)}.</li>
 *   <li><b>Kanal pesan:</b> {@link #getPesanPembina()}, {@link #setPesanPembina(String)},
 *       {@link #getPesanOrangTua()}, {@link #setPesanOrangTua(String)}.</li>
 *   <li><b>Penanggung jawab:</b> {@link #getPembina1()}, {@link #setPembina1(Tbmuser)},
 *       {@link #getPembina2()}, {@link #setPembina2(Tbmuser)}, {@link #getPembina3()},
 *       {@link #setPembina3(Tbmuser)}.</li>
 *   <li><b>Penanda tampil:</b> {@link #getAktif()}, {@link #setAktif(Boolean)}.</li>
 * </ul>
 *
 * <h2>Hal non-obvious dan kuirk</h2>
 * <ol>
 *   <li><b>{@link #getKode()} adalah getter destruktif.</b> Setiap pembacaan menimpa field
 *       {@code kode} dengan <code>idSiswa + "_" + ddMMyy</code>. Karena pemetaan berbasis
 *       <i>property access</i>, nilai itulah yang ditulis ke kolom unik saat flush. Akibatnya
 *       {@link #setKode(String)} praktis mati: {@code AktiftasHarianSiswaAction.onSave()} menyusun
 *       kode bergaya <code>nomorInduk + "-" + yyyyMMdd</code>, tetapi nilai itu selalu tergantikan
 *       sebelum sempat tersimpan.</li>
 *   <li><b>{@link #getNama()} juga getter destruktif.</b> Kolom {@code nama} bukan judul kegiatan
 *       melainkan salinan nama siswa yang di-refresh pada setiap pembacaan. Ini membuat parameter
 *       <code>nama</code> pada {@code AktifitasHarianSiswaApi.simpan()} &mdash; yang
 *       didokumentasikan di sana sebagai "judul/nama kegiatan" dan divalidasi wajib &mdash; tidak
 *       pernah benar-benar tersimpan; respons {@code detail}/{@code daftar} mengembalikan nama
 *       siswa, bukan judul yang dikirim klien.</li>
 *   <li><b>Asimetri denormalisasi.</b> Nama siswa selalu ikut berubah bila biodata siswa diubah,
 *       sedangkan nama butir aktivitas/materi justru dibekukan. Dua kebijakan berlawanan hidup di
 *       satu baris yang sama.</li>
 *   <li><b>{@link #getTanggal()} punya fallback tanpa write-back.</b> Bila {@code tanggal} masih
 *       null, getter mengembalikan tanggal hari ini tetapi <i>tidak</i> menuliskannya ke field.
 *       Karena {@link #getKode()} memanggilnya, baris tanpa tanggal tetap memperoleh kode
 *       berbasis hari ini, sementara kolom {@code tanggal} yang {@code nullable = false} akan
 *       ditolak DB.</li>
 *   <li><b>{@code aktif} tidak dipetakan.</b> Tidak ada {@code @Column} pada
 *       {@link #getAktif()} maupun {@code @Transient}; nilainya mengikuti penamaan default.
 *       Getter meng-coalesce {@code null} menjadi {@code true}. Satu-satunya penulis nilai ini
 *       adalah checkbox "Aktif" pada grid layar utama.</li>
 *   <li><b>Keunikan siswa+tanggal hanya dijaga aplikasi.</b> Pemeriksaan "sudah ada" dilakukan
 *       oleh {@code onSave()} pada layar utama saja; jalur mobile {@code simpan()} dan jalur
 *       kalender tidak melakukannya, sehingga baris kembar untuk satu siswa pada satu tanggal
 *       tetap mungkin terbentuk lewat API.</li>
 *   <li><b>{@link #toString()} memakai field {@code nama} langsung</b>, bukan {@link #getNama()},
 *       sehingga bebas efek samping &mdash; berbeda dari kebiasaan sebagian entity lain di paket
 *       ini.</li>
 *   <li><b>Impor {@link org.json.JSONObject} tidak terpakai</b> di kelas ini. Kedua kolom JSON
 *       diperlakukan sebagai {@link String} murni di sisi entity; seluruh penguraian dan
 *       penyusunan JSON terjadi di layar dan API. Sisa impor ini tidak berbahaya, tetapi
 *       menunjukkan bahwa sempat ada niat memindahkan logika JSON ke entity.</li>
 * </ol>
 *
 * <h2>Catatan keamanan dan privasi</h2>
 * <p>Baris ini menyimpan data anak yang bersifat sensitif: butir aktivitas bawaan pada instalasi
 * baru adalah "Shalat Jamaah", "Membaca Al-Quran", "Membantu Orang Tua", dan "Olahraga", dan butir
 * materi bawaannya "Tahfidz", "Hadits", "Bahasa Arab", "Fiqih". Artinya kolom {@code aktifitas}
 * secara rutin memuat catatan pelaksanaan ibadah harian per anak per hari, ditambah pesan bebas
 * dua arah antara guru dan keluarga. Hasil audit atas seluruh jalur pemakainya:</p>
 * <ul>
 *   <li><b>Halaman publik tanpa login (baca DAN tulis).</b> {@code CatatanOrangTuaServlet}
 *       terdaftar di <code>web.xml</code> pada URL <code>/AktiftasHarianSiswa?siswa={id}</code>
 *       tanpa gerbang autentikasi apa pun; parameter {@code siswa} adalah id numerik berurutan
 *       yang langsung disimpan ke HTTP session, lalu halaman ZUL merender ringkasan bulanan anak
 *       tersebut <b>dan</b> menyediakan tombol "Simpan Komentar" yang menulis
 *       {@code pesan_orang_tua} tanpa memeriksa siapa penulisnya. Menaikkan/menurunkan angka pada
 *       URL cukup untuk berpindah anak.</li>
 *   <li><b>Cakupan tenant fail-open total.</b> {@code AktiftasHarianSiswaAction.initCriteria()}
 *       tidak memasang penyaring sekolah maupun yayasan sama sekali, dan dasbornya berjalan pada
 *       mode "Semua Sekolah" secara bawaan. Akun guru sekolah A melihat jurnal siswa sekolah B
 *       pada instalasi multi-sekolah yang sama.</li>
 *   <li><b>Pola {@code ambilAnakSiswa()} fail-open (lihat {@code task_5e93a600}).</b> Penyaring
 *       untuk akun orang tua ditulis
 *       <code>if (!kids.isEmpty()) criteria.add(Restrictions.in("siswa.id", kids))</code>.
 *       {@code OrangTua.ambilAnakSiswa()} mengembalikan daftar kosong bila kolom JSON
 *       {@code anak} null, kosong, atau rusak (exception ditelan) &mdash; dan daftar kosong
 *       berarti <b>tidak ada pembatasan sama sekali</b>, sehingga orang tua yang datanya belum
 *       lengkap melihat jurnal seluruh anak di seluruh instalasi, bukan nol baris.</li>
 *   <li><b>Tombol unduh Excel tanpa gerbang.</b> {@code doAfterCompose()} menyembunyikan tombol
 *       Tambah bagi non-guru, tetapi {@code Common.appendDownloadButton(...)} menempelkan tombol
 *       unduh ke induk tombol itu tanpa menyalin {@code isVisible()} dan tanpa memeriksa hak.
 *       Kolom yang diekspor mencakup NIS, nama siswa, kedua dokumen JSON, kedua kanal pesan, dan
 *       nama ketiga pembina &mdash; digabung dengan dua butir sebelumnya, hak BACA saja sudah
 *       cukup untuk mengunduh biodata dan catatan ibadah seluruh anak di instalasi.</li>
 *   <li><b>IDOR pada layanan mobile.</b> {@code AktifitasHarianSiswaApi.detail()} mengambil baris
 *       berdasarkan {@code id} tanpa pemeriksaan kepemilikan apa pun, dan
 *       {@code resolveSiswa(...)} mendahulukan parameter {@code siswa} dari klien di atas siswa
 *       milik akun &mdash; sehingga token siswa/orang tua mana pun dapat membaca jurnal anak lain
 *       lewat {@code daftar} maupun {@code detail}. Pada sisi tulis, {@code simpan()} tidak
 *       memeriksa peran guru sama sekali, dan {@code pesanPembina} sama sekali tidak memeriksa
 *       kepemilikan (pemeriksaan yang ada hanya berlaku untuk cabang {@code pesanOrangTua}, itu
 *       pun hanya bila {@code tbmuser.getSiswa() != null} sehingga akun bertipe orang tua
 *       melewatinya). Klaim pada Javadoc kelas API bahwa "akun tidak dapat mengintip data siswa
 *       lain" tidak terpenuhi oleh kodenya.</li>
 *   <li><b>Pewarisan hak menu.</b> Hanya ada satu entri menu untuk seluruh modul ini. Kedua
 *       katalog {@link ais.database.model.sekolah.JenisAktiftasHarianDefault} dan
 *       {@link ais.database.model.sekolah.JenisMateriHarianDefault} tidak punya menu sendiri;
 *       CRUD-nya menumpang tab pada layar ini, sehingga hak atas "Aktifitas Harian Siswa"
 *       otomatis memberi hak mengubah kedua katalog tersebut.</li>
 *   <li><b>Checkbox "Aktif" pada grid</b> hanya bergerbang hak UPDATE, tanpa memeriksa peran guru
 *       maupun kepemilikan baris, dan menyimpan langsung lewat
 *       {@code Common.refreshSaveOrUpdate(...)} tanpa konfirmasi.</li>
 *   <li><b>Bug fungsional terkait.</b> Pada {@code initCriteria()}, {@code isOrtu} bernilai benar
 *       juga untuk akun bertipe siswa ({@code user.getSiswa() != null}), padahal cabangnya
 *       memanggil {@code getOrangTua().ambilAnakSiswa()} &mdash; akun siswa murni memicu
 *       {@link NullPointerException} sehingga layar daftar gagal dimuat baginya.</li>
 * </ul>
 * <p>Seluruh temuan di atas memperkuat task audit luas yang sudah ada
 * ({@code task_5e93a600} untuk cakupan siswa/tenant fail-open, {@code task_493423ef} untuk
 * endpoint tanpa autentikasi, {@code task_9b7ff647} untuk layar tanpa {@code checkPrevilages}).
 * Kelas ini hanya mendokumentasikannya; tidak ada perubahan logika yang dilakukan.</p>
 *
 * <h2>Catatan pewarisan {@link ais.database.model.GeneralValueObject}</h2>
 * <p>Kelas induk {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} dan
 * <b>bukan</b> {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak
 * memetakan properti apa pun miliknya. Karena itu deklarasi ulang {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} beserta getter/setter-nya di kelas ini
 * <b>bukan duplikasi yang keliru, melainkan keharusan teknis</b>: tanpa deklarasi ulang, keempat
 * kolom itu tidak akan dipetakan sama sekali. Pola yang sama berlaku di seluruh entity paket ini.
 * Yang tetap diwarisi dan dipakai di sini adalah utilitas non-persisten, terutama
 * {@code check(...)} yang dipanggil oleh keempat getter relasi.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.JenisAktiftasHarianDefault
 * @see ais.database.model.sekolah.JenisMateriHarianDefault
 * @see Siswa
 * @see Tbmuser
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "aktiftas_harian_siswa")
public class AktiftasHarianSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya dibangkitkan generator dan harus dipertahankan
	 * agar baris yang tersimpan di sesi/klaster lama tetap dapat dideserialisasi setelah kelas
	 * ini diubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, dibangkitkan DB (IDENTITY). Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang menyimpan baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang menyimpan baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini (jejak audit ringan yang
	 * disimpan langsung pada baris, di samping riwayat Envers).
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah diberi jejak audit.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna penyimpan baris ini.
	 *
	 * <p><b>Non-obvious:</b> setter ini <i>menolak diam-diam</i> nilai {@code null} maupun string
	 * kosong/spasi &mdash; field lama dipertahankan. Jejak audit karena itu hanya dapat diisi atau
	 * ditimpa, tidak pernah dapat dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna penyimpan baris ini.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong diabaikan tanpa pesan sehingga nama lama tetap bertahan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA yang dijalankan tepat sebelum setiap UPDATE atas baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang memperbarui
	 * {@code tanggal_dirubah} serta {@code oleh}/{@code olehId} dari konteks pengguna aktif.
	 * <b>Efek samping:</b> mengubah state object ini. Callback hanya berjalan pada UPDATE, bukan
	 * INSERT, dan tidak berjalan untuk operasi massal HQL/SQL native &mdash; pembaruan massal
	 * karena itu meninggalkan jejak audit yang tidak berubah.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat object dibuat sehingga
	 * baris baru selalu punya nilai, lalu diperbarui {@link #onUpdate()} pada setiap UPDATE.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah cap waktu baru; biasanya diisi oleh interceptor audit, bukan oleh
	 *                        kode layar.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini (presisi TIMESTAMP).
	 *
	 * @return cap waktu perubahan; tidak pernah {@code null} untuk object yang dibuat lewat
	 *         konstruktor kelas ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk <code>id-nama</code>.
	 *
	 * <p><b>Non-obvious:</b> method ini membaca field {@code nama} secara langsung, bukan lewat
	 * {@link #getNama()}. Karena itu ia <i>tidak</i> memicu efek samping penyegaran nama siswa dan
	 * tidak berisiko memicu inisialisasi lazy atas relasi {@code siswa}. Pada object yang baru
	 * dimuat, nilai yang tampil adalah nama siswa sebagaimana tersimpan di kolom; pada object baru
	 * yang belum disimpan, bagian nama akan {@code null}.</p>
	 *
	 * @return string <code>id-nama</code> untuk keperluan log dan debug.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode unik baris. Diturunkan ulang setiap kali dibaca &mdash; lihat {@link #getKode()}.
	 */
	private String kode;

	/** Siswa pemilik jurnal harian ini (wajib). Lihat {@link #getSiswa()}. */
	private Siswa siswa;

	/** Akun pembina/guru penanggung jawab pertama (opsional). Lihat {@link #getPembina1()}. */
	private Tbmuser pembina1;

	/** Akun pembina/guru penanggung jawab kedua (opsional). Lihat {@link #getPembina2()}. */
	private Tbmuser pembina2;

	/** Akun pembina/guru penanggung jawab ketiga (opsional). Lihat {@link #getPembina3()}. */
	private Tbmuser pembina3;

	/** Tanggal jurnal (tanpa komponen jam). Lihat {@link #getTanggal()}. */
	private Date tanggal;

	/**
	 * Salinan nama siswa, bukan judul kegiatan. Disegarkan pada setiap pembacaan &mdash; lihat
	 * {@link #getNama()}.
	 */
	private String nama;

	/** Keterangan umum bebas untuk hari itu. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Pesan guru/pembina kepada siswa dan keluarga. Lihat {@link #getPesanPembina()}. */
	private String pesanPembina;

	/** Tanggapan orang tua/wali kepada sekolah. Lihat {@link #getPesanOrangTua()}. */
	private String pesanOrangTua;

	/**
	 * Dokumen JSON daftar aktivitas harian beserta capaian YA/TIDAK. Lihat
	 * {@link #getAktifitas()} dan uraian struktur pada Javadoc kelas.
	 */
	private String aktifitas;

	/**
	 * Dokumen JSON daftar materi beserta penilaiannya. Lihat {@link #getMateri()} dan uraian
	 * struktur pada Javadoc kelas.
	 */
	private String materi;

	/** Penanda baris masih ditampilkan. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan dipakai layar untuk membuat baris
	 * jurnal kosong sebelum formulir diisi.
	 *
	 * <p>Seluruh properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah terisi
	 * waktu server lewat inisialisasi field.</p>
	 */
	public AktiftasHarianSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolom {@code id} dipetakan {@code insertable = false} karena nilainya dibangkitkan DB
	 * (IDENTITY); nilai {@code null} menandakan baris belum pernah disimpan, dan beberapa layar
	 * memakai fakta itu untuk membedakan mode "tambah" dari "ubah".</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris.
	 *
	 * @param id id baris; umumnya hanya diisi Hibernate seusai INSERT.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode unik baris, <b>selalu dihitung ulang</b> dari siswa dan tanggal dengan
	 * pola <code>idSiswa + "_" + ddMMyy</code>.
	 *
	 * <p><b>Getter destruktif.</b> Perhitungan tidak sekadar dikembalikan, melainkan ditimpakan
	 * ke field {@code kode}. Karena pemetaan entity ini memakai <i>property access</i>, Hibernate
	 * membaca nilai lewat getter ini saat flush sehingga nilai hasil perhitungan itulah yang
	 * benar-benar ditulis ke kolom unik. Konsekuensinya {@link #setKode(String)} tidak pernah
	 * berpengaruh pada apa yang tersimpan &mdash; termasuk kode bergaya
	 * <code>nomorInduk + "-" + yyyyMMdd</code> yang disusun
	 * {@code AktiftasHarianSiswaAction.onSave()} untuk baris baru.</p>
	 *
	 * <p><b>Efek samping lain:</b> memanggil {@link #getSiswa()} (dapat memicu inisialisasi lazy
	 * atau pencarian cache lewat {@code check(...)}) dan {@link #getTanggal()} (yang jatuh ke
	 * tanggal hari ini bila {@code tanggal} masih null). Pada object detached tanpa session,
	 * pemanggilan ini dapat gagal.</p>
	 *
	 * @return kode unik yang sudah di-trim, atau {@code null} bila siswa belum diisi atau kode
	 *         yang terbentuk kosong. Kolom aslinya {@code nullable = false}, sehingga hasil
	 *         {@code null} berarti baris tersebut memang belum layak disimpan.
	 */
	@Column(unique = true, nullable = false)
	public String getKode() {
		kode = getSiswa() == null ? null : getSiswa().getId() + "_" + Common.dateFormat85.get().format(getTanggal()); // date
																												// format
																												// ddMMyy
		return kode == null || kode.isEmpty() ? null : kode.trim();
	}

	/**
	 * Menetapkan kode baris.
	 *
	 * <p><b>Perhatian:</b> nilai yang diberikan di sini bersifat sementara. Pembacaan berikutnya
	 * lewat {@link #getKode()} akan menimpanya dengan kode turunan, dan karena Hibernate membaca
	 * lewat getter, nilai dari setter ini tidak pernah tersimpan ke DB. Setter dipertahankan
	 * semata agar kontrak JavaBean lengkap dan pemanggil lama tetap kompilasi.</p>
	 *
	 * @param kode kode yang diusulkan; efektif diabaikan.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan isi kolom {@code nama}, yaitu <b>nama siswa</b> pemilik jurnal &mdash; bukan
	 * judul kegiatan.
	 *
	 * <p><b>Getter destruktif.</b> Setiap pembacaan menimpa field {@code nama} dengan
	 * {@code getSiswa().getNamaSiswa()} sehingga kolom denormalisasi ini selalu tersinkron dengan
	 * biodata siswa saat baris berikutnya di-flush. Ini berlawanan arah dengan kebijakan pada
	 * {@link #getAktifitas()}/{@link #getMateri()} yang justru membekukan nama butir katalog.</p>
	 *
	 * <p><b>Dampak pada API mobile:</b> {@code AktifitasHarianSiswaApi.simpan()} mewajibkan
	 * parameter <code>nama</code> dan memanggil {@link #setNama(String)}, tetapi nilai itu selalu
	 * tergantikan sebelum tersimpan. Respons {@code daftar}/{@code detail} karena itu selalu
	 * mengembalikan nama siswa pada field <code>nama</code>, bukan judul yang dikirim klien.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getSiswa()}. Bila siswa {@code null}, field
	 * ditimpa {@code null} padahal kolomnya {@code nullable = false}; {@code Siswa.getNamaSiswa()}
	 * sendiri mengembalikan string kosong (bukan {@code null}) bila nama siswa belum diisi.</p>
	 *
	 * @return nama siswa yang sudah di-trim, atau {@code null} bila relasi {@code siswa} belum
	 *         terisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		nama = getSiswa() == null ? null : getSiswa().getNamaSiswa();
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan isi kolom {@code nama}.
	 *
	 * <p><b>Perhatian:</b> seperti {@link #setKode(String)}, nilai ini tidak bertahan &mdash;
	 * {@link #getNama()} menimpanya dengan nama siswa pada pembacaan berikutnya.</p>
	 *
	 * @param nama nilai yang diusulkan; efektif diabaikan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan umum hari itu &mdash; catatan bebas di luar daftar aktivitas dan
	 * materi, ditampilkan paling atas pada blok detail grid layar utama dengan label "Keterangan".
	 *
	 * <p>Getter murni tanpa efek samping. Kolomnya bertipe <code>text</code> sehingga panjangnya
	 * tidak dibatasi.</p>
	 *
	 * <p><b>Perhatian keluaran:</b> nilai dirender ke HTML lewat komponen {@code Html} pada grid
	 * layar utama <i>tanpa escaping</i>; pada halaman Buku Penghubung dan Catatan Orang Tua nilai
	 * ini di-escape lebih dahulu. Perbedaan perlakuan itu ada di sisi layar, bukan di entity.</p>
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan umum hari itu.
	 *
	 * @param keterangan teks bebas; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda apakah baris masih ditampilkan, dengan {@code null} dianggap
	 * {@code true}.
	 *
	 * <p><b>Non-obvious &mdash; properti ini tidak diberi {@code @Column} maupun
	 * {@code @Transient}</b>, sehingga Hibernate memetakannya ke kolom bernama default
	 * ({@code aktif}). Karena getter meng-coalesce {@code null} menjadi {@code true} dan pemetaan
	 * memakai property access, nilai {@code true} itulah yang ikut ditulis saat baris disimpan
	 * lewat alur normal; baris yang masuk lewat SQL mentah atau migrasi dapat tetap
	 * {@code NULL} di DB.</p>
	 *
	 * <p>Satu-satunya penulis nilai ini adalah checkbox "Aktif" pada grid
	 * {@code AktiftasHarianSiswaAction}, yang menyimpan seketika tanpa konfirmasi. Perlu dicatat
	 * bahwa renderer grid membaca {@code getAktif() == null ? false : getAktif()} &mdash;
	 * pemeriksaan {@code null} di sana adalah kode mati karena getter ini tidak pernah
	 * mengembalikan {@code null}.</p>
	 *
	 * <p><b>Perhatian:</b> tidak ada satu pun query pemakai yang menyaring berdasarkan
	 * {@code aktif}. Menonaktifkan baris karena itu hanya mengubah tampilan checkbox, tidak
	 * menyembunyikan jurnal dari daftar, dasbor, buku penghubung, halaman orang tua, maupun
	 * layanan mobile.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} bila sengaja
	 *         dinonaktifkan.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan penanda aktif.
	 *
	 * @param aktif {@code true}/{@code false}; nilai {@code null} akan dibaca kembali sebagai
	 *              {@code true} oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tanggal jurnal, dengan fallback ke tanggal hari ini bila belum diisi.
	 *
	 * <p>Bersama {@link #getSiswa()}, nilai ini membentuk kunci logis baris: layar utama menolak
	 * menyimpan baris baru bila sudah ada baris lain untuk pasangan siswa dan tanggal yang sama.
	 * Kolomnya {@code TemporalType.DATE} sehingga komponen jam tidak disimpan.</p>
	 *
	 * <p><b>Non-obvious &mdash; fallback tanpa write-back.</b> Berbeda dari {@link #getKode()} dan
	 * {@link #getNama()}, getter ini <i>tidak</i> menuliskan hasil fallback ke field. Akibatnya
	 * baris yang tanggalnya belum diisi tetap menghasilkan kode berbasis hari ini lewat
	 * {@link #getKode()}, sementara kolom {@code tanggal} yang {@code nullable = false} akan
	 * ditolak DB saat INSERT. Nilai fallback juga bergeser mengikuti hari sistem selama object
	 * masih hidup di memori.</p>
	 *
	 * @return tanggal jurnal, atau tanggal hari ini bila field masih {@code null}; tidak pernah
	 *         mengembalikan {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menetapkan tanggal jurnal.
	 *
	 * @param tanggal tanggal hari yang dijurnalkan; komponen jam diabaikan oleh pemetaan DATE.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan siswa pemilik jurnal harian ini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}) dan satu-satunya penentu "milik siapa" baris ini.
	 * Dimuat {@code LAZY} dengan {@code CascadeType.PERSIST} dan {@code MERGE} &mdash; perhatikan
	 * bahwa cascade MERGE berarti perubahan pada object {@link Siswa} yang menempel pada baris ini
	 * ikut terbawa saat baris di-merge.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(siswa)} warisan
	 * {@link ais.database.model.GeneralValueObject} lalu menuliskan hasilnya kembali ke field.
	 * Utilitas itu menukar proxy yang belum terinisialisasi dengan instance kanonis dari cache
	 * identitas bila tersedia, sehingga perubahan skalar pada siswa yang sama langsung terlihat
	 * oleh semua pemegang referensi. Penulisan balik ini menyegarkan referensi, bukan menghapus
	 * data, jadi berbeda sifatnya dari getter destruktif {@link #getKode()}/{@link #getNama()}.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@link #getKode()}, {@link #getNama()}, seluruh renderer layar,
	 * dan pemetaan JSON pada layanan mobile.</p>
	 *
	 * @return siswa pemilik baris, atau {@code null} pada baris baru yang belum dipilih siswanya.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan siswa pemilik jurnal harian ini.
	 *
	 * <p>Pada layar utama, pemilihan siswa dikunci setelah baris tersimpan sehingga jurnal tidak
	 * dapat dipindahkan ke anak lain lewat UI. Pengunciannya bersifat tampilan; jalur mobile
	 * {@code AktifitasHarianSiswaApi.simpan()} masih dapat mengubah relasi ini pada baris yang
	 * sudah ada bila parameter <code>siswa</code> dikirim.</p>
	 *
	 * @param siswa siswa pemilik baris; wajib terisi sebelum baris disimpan.
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan akun pembina/guru penanggung jawab pertama.
	 *
	 * <p>Bertipe {@link Tbmuser} (akun aplikasi), <b>bukan</b> entity {@code Guru} &mdash; pola
	 * yang sama dengan {@code PembinaSiswa}. Konsekuensinya pemilih pembina pada formulir tidak
	 * dibatasi sekolah atau yayasan mana pun, dan akun non-guru pun dapat tercatat sebagai
	 * pembina.</p>
	 *
	 * <p>Pada formulir, pembina pertama diisi otomatis dengan akun guru yang sedang login untuk
	 * baris baru lalu dikunci, sehingga kolom ini berfungsi sebagai penanda "siapa yang menulis
	 * jurnal ini". Relasi opsional dan {@code LAZY}.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(pembina1)} dan menuliskan hasilnya kembali ke
	 * field (penyegaran referensi, bukan penghapusan data).</p>
	 *
	 * @return akun pembina pertama, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembina1")
	public Tbmuser getPembina1() {
		pembina1 = check(pembina1);
		return pembina1;
	}

	/**
	 * Menetapkan akun pembina/guru penanggung jawab pertama.
	 *
	 * @param pembina1 akun pembina; boleh {@code null}.
	 */
	public void setPembina1(Tbmuser pembina1) {
		this.pembina1 = pembina1;
	}

	/**
	 * Mengembalikan akun pembina/guru penanggung jawab kedua.
	 *
	 * <p>Kembaran {@link #getPembina1()} dengan seluruh sifat yang sama (tipe {@link Tbmuser},
	 * opsional, {@code LAZY}, memanggil {@code check(...)} dengan penulisan balik). Berbeda dari
	 * pembina pertama, kolom ini tidak pernah diisi otomatis dan hanya dapat diubah oleh pengguna
	 * berperan guru.</p>
	 *
	 * @return akun pembina kedua, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembina2")
	public Tbmuser getPembina2() {
		pembina2 = check(pembina2);
		return pembina2;
	}

	/**
	 * Menetapkan akun pembina/guru penanggung jawab kedua.
	 *
	 * @param pembina2 akun pembina; boleh {@code null}.
	 */
	public void setPembina2(Tbmuser pembina2) {
		this.pembina2 = pembina2;
	}

	/**
	 * Mengembalikan akun pembina/guru penanggung jawab ketiga.
	 *
	 * <p>Kembaran {@link #getPembina2()}. Ketiga kolom pembina adalah daftar tetap berukuran tiga,
	 * bukan relasi satu-ke-banyak &mdash; tidak ada cara menambah pembina keempat tanpa mengubah
	 * skema.</p>
	 *
	 * @return akun pembina ketiga, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembina3")
	public Tbmuser getPembina3() {
		pembina3 = check(pembina3);
		return pembina3;
	}

	/**
	 * Menetapkan akun pembina/guru penanggung jawab ketiga.
	 *
	 * @param pembina3 akun pembina; boleh {@code null}.
	 */
	public void setPembina3(Tbmuser pembina3) {
		this.pembina3 = pembina3;
	}

	/**
	 * Mengembalikan dokumen JSON daftar aktivitas harian beserta capaiannya, apa adanya sebagai
	 * {@link String}.
	 *
	 * <p>Struktur: objek JSON bernomor urut, tiap nilainya objek {@code {"nama": ..., "nilai": ...}}
	 * dengan {@code nilai} terbatas pada <code>"YA"</code>, <code>"TIDAK"</code>, atau
	 * <code>""</code>. Uraian lengkap beserta contohnya ada pada Javadoc kelas.</p>
	 *
	 * <p>Nama butir merupakan <b>salinan nilai</b> dari katalog
	 * {@link ais.database.model.sekolah.JenisAktiftasHarianDefault} yang dibekukan saat penyimpanan
	 * pertama, sehingga perubahan katalog tidak menyentuh jurnal lama. Entity ini tidak menguraikan
	 * maupun memvalidasi JSON &mdash; seluruh penguraian dilakukan pemakainya, masing-masing dengan
	 * blok {@code try/catch} yang menelan JSON rusak secara diam-diam sehingga blok aktivitas
	 * hilang dari tampilan tanpa pesan apa pun.</p>
	 *
	 * <p>Getter murni tanpa efek samping.</p>
	 *
	 * @return dokumen JSON aktivitas, atau {@code null} bila belum pernah diisi. String kosong
	 *         dan {@code "{}"} sama-sama diperlakukan sebagai "tidak ada aktivitas" oleh pemakai.
	 */
	@Column(name = "aktifitas", nullable = true, columnDefinition = "text")
	public String getAktifitas() {
		return aktifitas;
	}

	/**
	 * Menetapkan dokumen JSON daftar aktivitas harian.
	 *
	 * <p><b>Tidak ada validasi bentuk di sini.</b> Pemanggil bertanggung jawab menyusun struktur
	 * yang benar. Jalur resmi adalah {@code AktiftasHarianSiswaAction.onSave()} yang membangunnya
	 * dari petak radio; jalur mobile {@code AktifitasHarianSiswaApi.simpan()} meneruskan string
	 * mentah dari klien tanpa pemeriksaan sama sekali, sehingga teks sembarang dapat masuk dan
	 * membuat blok aktivitas hilang dari seluruh layar.</p>
	 *
	 * @param aktifitas dokumen JSON aktivitas; boleh {@code null}.
	 */
	public void setAktifitas(String aktifitas) {
		this.aktifitas = aktifitas;
	}

	/**
	 * Mengembalikan dokumen JSON daftar materi beserta penilaiannya, apa adanya sebagai
	 * {@link String}.
	 *
	 * <p>Strukturnya identik dengan {@link #getAktifitas()}, hanya domain {@code nilai}-nya yang
	 * berbeda: di sini nilai adalah teks bebas yang dalam praktik diisi huruf mutu atau predikat.
	 * Nama butir merupakan salinan nilai dari katalog
	 * {@link ais.database.model.sekolah.JenisMateriHarianDefault}, dibekukan dengan cara dan
	 * konsekuensi yang sama.</p>
	 *
	 * <p>Getter murni tanpa efek samping.</p>
	 *
	 * @return dokumen JSON materi, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "materi", nullable = true, columnDefinition = "text")
	public String getMateri() {
		return materi;
	}

	/**
	 * Menetapkan dokumen JSON daftar materi.
	 *
	 * <p>Sama seperti {@link #setAktifitas(String)}, tidak ada validasi bentuk pada entity dan
	 * jalur mobile meneruskan string mentah dari klien.</p>
	 *
	 * @param materi dokumen JSON materi; boleh {@code null}.
	 */
	public void setMateri(String materi) {
		this.materi = materi;
	}

	/**
	 * Mengembalikan pesan dari guru/pembina kepada siswa dan keluarganya untuk hari itu.
	 *
	 * <p>Kanal satu arah sekolah &rarr; keluarga, berpasangan dengan {@link #getPesanOrangTua()}
	 * membentuk komunikasi dua arah pada satu baris jurnal. Ditampilkan pada Buku Penghubung,
	 * halaman Catatan Orang Tua, dan layanan mobile (yang juga memancarkan penanda
	 * <code>adaPesanPembina</code> agar aplikasi dapat menandai entri yang perlu ditanggapi).</p>
	 *
	 * <p>Getter murni tanpa efek samping. Kolom bertipe <code>text</code>.</p>
	 *
	 * <p><b>Catatan keamanan:</b> jalur tulisnya lewat
	 * {@code AktifitasHarianSiswaApi.pesanPembina()} tidak memeriksa peran maupun kepemilikan
	 * sama sekali, sehingga token akun apa pun dapat menuliskan "pesan guru" pada jurnal siswa
	 * mana pun.</p>
	 *
	 * @return pesan pembina, atau {@code null} bila belum diisi.
	 */
	@Column(name = "pesan_pembina", nullable = true, columnDefinition = "text")
	public String getPesanPembina() {
		return pesanPembina;
	}

	/**
	 * Menetapkan pesan dari guru/pembina.
	 *
	 * <p>Dipanggil dari formulir layar utama saat menyimpan, dan dari
	 * {@code AktifitasHarianSiswaApi.simpan()} serta
	 * {@code AktifitasHarianSiswaApi.pesanPembina()}. Penulisan bersifat menimpa penuh &mdash;
	 * tidak ada riwayat pesan per hari selain riwayat versi baris yang direkam Envers.</p>
	 *
	 * @param pesanPembina teks pesan; boleh {@code null} atau kosong.
	 */
	public void setPesanPembina(String pesanPembina) {
		this.pesanPembina = pesanPembina;
	}

	/**
	 * Mengembalikan tanggapan orang tua/wali atas jurnal hari itu.
	 *
	 * <p>Kanal keluarga &rarr; sekolah. Dipakai juga sebagai indikator "belum ditanggapi" pada
	 * agregasi halaman Catatan Orang Tua dan sebagai penanda <code>adaPesanOrangTua</code> pada
	 * layanan mobile.</p>
	 *
	 * <p>Getter murni tanpa efek samping. Kolom bertipe <code>text</code>.</p>
	 *
	 * <p><b>Catatan keamanan:</b> properti inilah satu-satunya yang dapat ditulis dari halaman
	 * publik tanpa login {@code /AktiftasHarianSiswa?siswa={id}}. Baik
	 * {@code CatatanOrangTuaAktiftasHarianAction.savePesanOrangTua()} maupun
	 * {@code BukuPenghubungSiswa.simpanPesanOrtu()} menyimpan berdasarkan id baris saja, tanpa
	 * memeriksa siapa penulisnya.</p>
	 *
	 * @return tanggapan orang tua, atau {@code null} bila belum diisi.
	 */
	@Column(name = "pesan_orang_tua", nullable = true, columnDefinition = "text")
	public String getPesanOrangTua() {
		return pesanOrangTua;
	}

	/**
	 * Menetapkan tanggapan orang tua/wali.
	 *
	 * <p>Dipanggil dari formulir layar utama, kotak komentar Buku Penghubung, kotak komentar
	 * halaman publik Catatan Orang Tua, dan {@code AktifitasHarianSiswaApi.pesanOrangTua()}.
	 * Penulisan bersifat menimpa penuh; komentar sebelumnya hanya dapat ditelusuri lewat riwayat
	 * Envers.</p>
	 *
	 * @param pesanOrangTua teks tanggapan; boleh {@code null} atau kosong.
	 */
	public void setPesanOrangTua(String pesanOrangTua) {
		this.pesanOrangTua = pesanOrangTua;
	}

}
