package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.KategoriPenghargaan;
import ais.database.model.Perkuliahan;

/**
 * Entity TRANSAKSI pengajuan penghargaan/karya siswa pada modul sekolah &mdash; satu baris tabel
 * {@code sekolah.penghargaan_siswa} mewakili SATU capaian yang diajukan atas nama seorang
 * {@link Siswa} (paten, HaKI, kejuaraan tingkat nasional/internasional, dsb.), lengkap dengan nomor
 * sertifikat, bukti unggahan, dan status persetujuannya.
 *
 * <p>Layar yang memakainya adalah {@code /pages/master/sekolah/penghargaan_siswa.zul}, tab pertama
 * berjudul <b>"Karya Siswa"</b>, dikendalikan {@code ais.action.master.sekolah.PenghargaanSiswaAction}
 * (987 baris). Label formulirnya menyebut kolom {@link #getNama() nama} sebagai <b>"Nama Karya"</b>
 * dan {@link #getTanggal() tanggal} sebagai "Tanggal Pendaftaran Karya", sementara pesan validasi di
 * {@code onSave()} menyebutnya "Nama Kejuaraan"/"Tanggal Mulai Kejuaraan". Ketidakkonsistenan
 * istilah itu ada di kode aslinya, bukan salah baca: satu tabel dipakai untuk dua pengertian
 * (karya/kekayaan intelektual dan kejuaraan) sekaligus.</p>
 *
 * <h3>BUKAN kerabat {@code Penghargaan} &mdash; verifikasi eksplisit</h3>
 * Nama kelas ini menyesatkan. Meskipun berada di paket yang sama dan berselisih satu kata dengan
 * {@code ais.database.model.sekolah.Penghargaan}, <b>keduanya tidak punya hubungan apa pun</b> di
 * tingkat kode maupun basis data:
 * <ul>
 *   <li>{@code Penghargaan} (tabel {@code sekolah.penghargaan}) adalah <b>master butir</b> pemegang
 *       kolom {@code poin}, lapis dasar rantai apresiasi empat tingkat
 *       {@code Apresiasi}/{@code Penghargaan} &rarr; {@code ApresiasiDanPenghargaan} &rarr;
 *       {@code ApresiasiSiswa}. Ia dirujuk lewat dua tabel jembatan {@code @ManyToMany} dan tidak
 *       pernah menyimpan siapa penerimanya.</li>
 *   <li>Kelas ini (tabel {@code sekolah.penghargaan_siswa}) adalah <b>baris transaksi mandiri</b>:
 *       ia memegang FK {@code siswa} langsung, punya alur status persetujuannya sendiri, dan
 *       <b>tidak memiliki kolom poin/kredit sama sekali</b>. Tidak ada satu pun FK, koleksi, atau
 *       tabel jembatan yang menghubungkannya ke {@code Apresiasi}, {@code ApresiasiDanPenghargaan},
 *       {@code ApresiasiSiswa}, maupun {@code Penghargaan}.</li>
 * </ul>
 * Kerabat struktural yang sebenarnya adalah {@code ais.database.model.PenghargaanMahasiswa} dan
 * {@code ais.database.model.PenghargaanDosen} di modul perguruan tinggi &mdash; sama-sama baris
 * pengajuan berkolom {@code kategori_penghargaan}/{@code capaian}/{@code no_sk}/{@code feeder},
 * dan sama-sama memakai katalog global {@link ais.database.model.KategoriPenghargaan}. Nilai seed
 * yang ditulis layar ini ("Paten", "HaKI", "Nasional / Internasional") memperjelas asalnya: ini
 * salinan modul pelaporan kekayaan intelektual gaya feeder PDDIKTI yang dipasang ulang di sisi
 * sekolah.
 *
 * <h3>Hasil verifikasi bug {@code totalPointPenghargaan} (permanen {@code 0.0}): NEGATIF</h3>
 * Bug variabel lokal {@code totalPointPenghargaan} yang tidak pernah diakumulasi di
 * {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} (baris 1224 dan 1251, kembar
 * {@code totalPointHukuman}) <b>sama sekali tidak berkaitan dengan entity ini</b>. Loop yang rusak
 * itu berjalan di atas koleksi {@code ApresiasiSiswa} dan menjumlahkan
 * {@code Penghargaan.getPoin()} ke variabel {@code pointPenghargaan} (per kejadian) serta
 * {@code totalPointKegiatan} (lintas kejadian), tetapi lupa menambah
 * {@code totalPointPenghargaan} yang justru dikirim ke template rapor. Entity ini tidak pernah
 * disebut di {@code LaporanRaporSiswa}, tidak punya kolom poin, dan &mdash; sesuai penelusuran
 * seluruh sumber &mdash; <b>hanya dirujuk oleh dua berkas Java</b>: dirinya sendiri dan
 * {@code PenghargaanSiswaAction}. Jadi tidak ada kolom di kelas ini yang "lupa dijumlahkan"; bug
 * rapor itu murni milik rantai apresiasi.
 *
 * <h3>Alur status persetujuan</h3>
 * Empat konstanta {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES}, {@link #DISETUJUI}, dan
 * {@link #DITOLAK} adalah satu-satunya nilai sah kolom {@code status}; keempatnya dirakit menjadi
 * {@code Combobox} baik di panel pencarian maupun di setiap baris grid. Nilai {@code DISETUJUI}
 * bersifat mengunci secara visual: begitu dipilih, grup tombol Ubah/Hapus baris itu disembunyikan.
 * Perlu dicatat bahwa penguncian ini <b>hanya penyembunyian tombol di sisi tampilan</b> &mdash;
 * tidak ada satu pun pemeriksaan status di jalur simpan/hapus, sehingga baris "Disetujui" tetap
 * dapat diubah atau dihapus lewat jalur lain (lihat bagian keamanan).
 *
 * <h3>Kolom bisnis</h3>
 * <ul>
 *   <li>{@code nama} ("Nama Karya") &mdash; satu-satunya kolom {@code nullable=false} selain
 *       {@code siswa}; dipangkas saat dibaca ({@link #getNama()}).</li>
 *   <li>{@code namaen} &mdash; judul karya dalam bahasa Inggris, kolom terpisah dan opsional.</li>
 *   <li>{@code siswa} &mdash; FK wajib ke {@link Siswa}, pemilik capaian.</li>
 *   <li>{@code kategori_penghargaan} &mdash; FK ke katalog global
 *       {@link ais.database.model.KategoriPenghargaan} (label layar: "Bentuk Penghargaan").</li>
 *   <li>{@code tanggal}/{@code tanggalselesai} &mdash; rentang waktu capaian; keduanya wajib diisi
 *       menurut validasi {@code onSave()} meski di skema tetap {@code nullable}.</li>
 *   <li>{@code nomorsertifikat}, {@code capaian}, {@code url} &mdash; identitas dan bukti capaian;
 *       {@code capaian} dan {@code nomorsertifikat} wajib menurut validasi layar.</li>
 *   <li>{@code yayasan}/{@code sekolah} &mdash; penanda cakupan tenant, keduanya
 *       {@code nullable=true}; {@code null} ditampilkan sebagai "Semua". Lihat catatan fail-open di
 *       bawah.</li>
 *   <li>{@code tahunAkademik}/{@code jenisSemester}/{@code tahun} &mdash; periode akademik. Ketiga
 *       getter-nya <b>menulis balik ke field</b>, lihat "Getter write-back" di bawah.</li>
 *   <li>{@code alamat} ("Lokasi / Alamat"), {@code noSk}, {@code tglSk} &mdash; metadata SK
 *       penetapan penghargaan.</li>
 *   <li>{@code feeder} &mdash; kolom pelaporan ke feeder eksternal. <b>Kode mati:</b> pencarian
 *       menyeluruh atas seluruh sumber tidak menemukan satu pun pemanggil
 *       {@link #getFeeder()}/{@link #setFeeder(String)} di luar kelas ini, dan kolom itu tidak ada
 *       di daftar {@code contents} untuk cetak/unggah Excel. Lihat "Fitur setengah jadi".</li>
 *   <li>{@code status} &mdash; alur persetujuan di atas.</li>
 *   <li>{@code keterangan} &mdash; teks bebas.</li>
 * </ul>
 * Lampiran (scan/foto sertifikat) TIDAK disimpan sebagai kolom entity ini, melainkan sebagai baris
 * {@code LampiranLain} yang menyimpan {@code ref = id} baris ini dan nama kelas ini sebagai
 * penanda jenis. Konsekuensinya lampiran <b>tidak ikut terhapus</b> saat baris ini dihapus, dan
 * relasi itu tidak terlihat sama sekali dari deklarasi kelas.
 *
 * <h3>Getter write-back (dipersistensikan diam-diam)</h3>
 * Tiga getter di kelas ini bukan pembaca murni; ketiganya menugaskan nilai ke field yang
 * <b>dipetakan Hibernate</b>. Karena Hibernate melakukan dirty-checking berdasarkan pembacaan
 * properti, nilai yang "hanya" diisi saat render dapat ikut ter-{@code UPDATE} pada flush
 * berikutnya &mdash; termasuk ter-audit Envers sebagai revisi baru meski tidak ada pengguna yang
 * menyunting apa pun:
 * <ul>
 *   <li>{@link #getTahunAkademik()} mengisi {@code tahunAkademik} yang {@code null} dengan
 *       {@code Common.getCurrentTahunAkademik()}. Nilai itu <b>bergantung pada siapa yang sedang
 *       login dan kapan</b> (helper-nya membaca {@code Common.getCurrentUser()} lalu
 *       {@code RencanaTahunAkademik} milik konteks pengguna itu). Baris lama bertahun-akademik
 *       kosong karena itu dicap dengan tahun ajaran <b>pembacanya</b>, bukan tahun kejadiannya.</li>
 *   <li>{@link #getJenisSemester()} berperilaku sama dengan {@code Common.isNowSemensterGanjil()}
 *       sebagai sumber nilai.</li>
 *   <li>{@link #getTahun()} lebih merusak: ia <b>menimpa</b> {@code tahun} yang sudah terisi setiap
 *       kali {@code tahunAkademik} tidak {@code null}, bukan sekadar mengisi yang kosong. Nilai
 *       {@code tahun} yang dimasukkan lewat jalur lain (unggahan Excel, endpoint generik
 *       {@code /Data}) akan hilang tanpa jejak begitu baris itu dibaca.</li>
 * </ul>
 *
 * <h3>Cakupan tenant dan penyaring orang tua (fail-open)</h3>
 * {@code PenghargaanSiswaAction.initCriteria()} memakai pola yang sudah berulang kali dicatat di
 * modul sekolah:
 * {@code if (tbmuser.getOrangTua() != null && !ambilAnakSiswa().isEmpty()) criteria.add(in("siswa.id", ...))}.
 * {@code ais.database.model.OrangTua.ambilAnakSiswa()} mengembalikan <b>daftar kosong</b> pada
 * semua kasus tepi &mdash; {@code id} masih {@code null}, kolom JSON {@code anak} kosong atau
 * rusak (exception ditelan blok {@code catch} bertanda {@code auto-audit(empty-catch)}), atau tidak
 * ada kunci berawalan {@code "siswa"}. Karena daftar kosong membuat seluruh syarat dilewati,
 * <b>daftar kosong berarti "tanpa filter" alias melihat SEMUA baris seluruh instalasi</b>, bukan
 * "nol baris". Arah kegagalan yang benar adalah sebaliknya.
 * <p>Penyaring yayasan/sekolah pada layar yang sama juga tidak pernah dipaksakan: keduanya hanya
 * dipasang bila pengguna sendiri yang memilihnya di combobox, dan justru <b>dilewati</b>
 * ({@code 1=1}) untuk akun bertipe siswa. Tidak ada satu pun batas tenant otomatis di kriteria
 * ini.</p>
 * <p><b>Verifikasi NEGATIF amplifier cache L3.</b> Berbeda dari {@code PelanggaranSiswa}, entity ini
 * tidak punya dasbor Java sendiri: seluruh sumber hanya menyebutnya di dua berkas, dan tidak satu
 * pun memanggil {@code loadDataWithCache()}. Jadi hasil query lintas-siswa di sini <b>tidak</b>
 * masuk cache L3 app-wide. Amplifier yang berlaku untuk entity ini bentuknya lain, yaitu endpoint
 * generik {@code /Data} pada paragraf berikut.</p>
 *
 * <h3>Catatan keamanan (broken access control)</h3>
 * Temuan berikut berasal dari pembacaan kode, dicatat di sini karena berdampak langsung pada data
 * tabel ini. Semuanya menguatkan task audit yang sudah ada
 * ({@code task_5e93a600}, {@code task_493423ef}, {@code task_9b7ff647}) dan tidak menuntut task
 * baru.
 * <ol>
 *   <li><b>Nol {@code checkPrevilages} pada 987 baris Action.</b> {@code PenghargaanSiswaAction}
 *       tidak memanggil {@code CommonPrivilages.checkPrevilages} satu kali pun. Gerbang tombol
 *       "Tambah" hanyalah {@code add.setVisible(tbmuser != null)}, dan tombol Ubah/Hapus per baris
 *       hanya dipagari status "Disetujui". Artinya <b>hak BACA menu ini setara hak tulis penuh</b>.
 *       </li>
 *   <li><b>Combobox status tanpa gerbang.</b> Pengubah status &mdash; termasuk meresmikan sebuah
 *       karya menjadi {@code "Disetujui"} &mdash; dirender untuk setiap pengguna yang bukan berada
 *       dalam konteks siswa tertentu, tanpa cek hak apa pun, dan langsung memanggil
 *       {@code Common.refreshUpdate()}. Alur persetujuan yang seharusnya milik petugas dapat
 *       dijalankan siapa saja yang bisa membuka layar.</li>
 *   <li><b>IDOR lewat parameter URL {@code ?siswa=}.</b> {@code doAfterCompose()} mengambil
 *       {@code execution.getParameter("siswa")} dan memuat {@link Siswa} dengan id itu
 *       <b>tanpa cek kepemilikan</b>; hanya bila parameter absen barulah sistem jatuh ke
 *       {@code tbmuser.getSiswa()}. Akun siswa maupun wali cukup mengganti angka pada URL untuk
 *       melihat dan menyunting karya siswa lain. Ini instance yang sama persis dengan temuan
 *       {@code PrestasiSiswa}.</li>
 *   <li><b>Parameter {@code ?penghargaan=} melewati SELURUH penyaring.</b> Baris yang ditunjuk
 *       parameter itu dimuat lewat {@code GeneralValueObject.ambilData(...)} lalu
 *       <b>disisipkan paling atas daftar hasil</b> di {@code onSearchDefault()}, di luar
 *       {@code Criteria}. Penyaring siswa, penyaring anak untuk wali murid, dan penyaring
 *       tenant tidak berlaku untuknya sama sekali.</li>
 *   <li><b>Unggahan Excel menyertakan kolom {@code "id"}.</b> Daftar {@code contents} yang
 *       diserahkan ke {@code Common.uploadData(...)} diawali {@code "id"}, sehingga satu berkas
 *       unggahan dapat <b>menimpa baris mana pun berdasarkan id</b>, bukan hanya menambah baris
 *       baru. Tombolnya sendiri hanya digerbangi visibilitas tombol Tambah.</li>
 *   <li><b>Endpoint generik {@code /Data} ({@code ais.action.servlet.Data}).</b> Layar baru
 *       {@code WEB-INF/baru/modul/prestasi/karya/_karya_siswa.jsp} mengoperasikan tabel ini
 *       sepenuhnya lewat endpoint itu: {@code action=daftar} dengan klausa {@code where1} yang
 *       <b>disusun di sisi klien</b> (termasuk konkatenasi kata kunci pencarian mentah ke dalam
 *       {@code ILIKE}), {@code action=load}/{@code simpanDataRinci}/{@code hapusDataRinci} dengan
 *       {@code id} bebas dari klien. Servlet-nya hanya memastikan pemanggil sudah login &mdash;
 *       tidak ada {@code checkPrevilages}, tidak ada cek kepemilikan, tidak ada batas tenant.
 *       Muatan simpan bahkan memuat field {@code status}, sehingga akun siswa dapat
 *       <b>menyetujui karyanya sendiri</b> tanpa melewati petugas. Tombol Ubah/Hapus di JSP itu
 *       juga dirender tanpa memakai flag {@code edit}/{@code delete} yang sudah dihitung di
 *       bagian atas halaman &mdash; flag {@code add} dipakai, dua lainnya tidak.</li>
 *   <li><b>Dasbor menyusun SQL di sisi klien.</b>
 *       {@code WEB-INF/baru/modul/prestasi/karya/_dashboard_karya_siswa.jsp} menempelkan potongan
 *       {@code baseWhere} ke dalam variabel JavaScript, merangkai tiga pernyataan
 *       {@code SELECT ... FROM sekolah.penghargaan_siswa ...} di browser, lalu mengirimnya ke
 *       {@code /Data} dengan {@code action=sql}. Pembatas satu-satunya
 *       ({@code AND p.siswa = <id>}, itu pun hanya bila yang login memang seorang siswa) berada
 *       <b>di dalam teks SQL yang dikirim klien</b>, jadi bukan pembatas sama sekali; untuk akun
 *       wali murid dan guru bahkan tidak ada pembatas apa pun. Lapis pertahanan
 *       {@code SqlSecurityGuard} baru aktif bila pemilik instalasi menyalakannya.</li>
 *   <li><b>Auto-seed pada jalur baca.</b> {@code doAfterCompose()} menghitung baris
 *       {@link ais.database.model.KategoriPenghargaan}; bila nol, ia langsung
 *       {@code session.save()} tiga baris ("Paten", "HaKI", "Nasional / Internasional"). Sekadar
 *       <b>membuka</b> layar ini menulis ke katalog global lintas modul, tanpa gerbang hak dan
 *       tanpa penanda tenant.</li>
 * </ol>
 *
 * <h3>Fitur setengah jadi dan penyaring hantu</h3>
 * <ul>
 *   <li><b>Tombol "Krm ke feeder" tidak pernah tampil.</b> {@code buttonTagihan} dibuat di dalam
 *       renderer dan visibilitasnya diatur dari listener status, tetapi <b>tidak pernah
 *       di-{@code setParent}</b> maupun dimasukkan ke daftar tombol aksi. {@code Hbox} pendamping
 *       yang seharusnya menampungnya memang dipasang ke baris, namun tetap kosong. Bersama
 *       kolom {@code feeder} yang nol pemanggil, ini berarti <b>jalur pengiriman ke feeder tidak
 *       pernah selesai dibuat</b> &mdash; kolomnya ada, tombolnya ada di kode, hasilnya tidak ada
 *       di layar.</li>
 *   <li><b>Penyaring "Penyelenggara" hantu.</b> Berkas ZUL mendeklarasikan
 *       {@code <textbox id="searchpenyelenggara">}, tetapi Action tidak punya field bernama itu dan
 *       {@code initCriteria()} tidak pernah membacanya. Entity ini pun tidak punya kolom
 *       penyelenggara. Kotak isian itu tampil, dapat diketik, lalu <b>diabaikan diam-diam</b>.
 *       Judul kolom grid "Tempat/Penyelenggara/Smt" senasib: yang benar-benar dirender di kolom itu
 *       adalah tanggal dan tahun akademik/semester.</li>
 *   <li><b>Validasi lampiran yang tidak simetris.</b> {@code onSave()} mewajibkan bukti sertifikat,
 *       tetapi untuk baris yang sudah tersimpan ia memeriksa {@code LampiranLain.ambil(...)} dan
 *       untuk baris baru memeriksa variabel instance {@code lainSiswa}. Bila pencarian lampiran itu
 *       melempar exception, blok {@code catch} me-rollback transaksi streaming lalu
 *       <b>membiarkan alur simpan berjalan terus</b> &mdash; penyimpanan tetap terjadi tanpa bukti
 *       yang katanya wajib.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit manual:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Isi capaian:</b> {@link #getNama()}, {@link #getNamaEn()}, {@link #getCapaian()},
 *       {@link #getUrl()}, {@link #getNomorSertifikat()}, {@link #getKeterangan()},
 *       {@link #getAlamat()} beserta setter masing-masing.</li>
 *   <li><b>Waktu:</b> {@link #getTanggal()}, {@link #getTanggalSelesai()}, {@link #getTglSk()}.</li>
 *   <li><b>Periode akademik (getter write-back):</b> {@link #getTahunAkademik()},
 *       {@link #getJenisSemester()}, {@link #getTahun()}.</li>
 *   <li><b>Relasi:</b> {@link #getSiswa()}, {@link #getKategoriPenghargaan()},
 *       {@link #getYayasan()}, {@link #getSekolah()}.</li>
 *   <li><b>Alur kerja:</b> {@link #getStatus()}/{@link #setStatus(String)} dan empat konstanta
 *       statusnya.</li>
 *   <li><b>Integrasi luar:</b> {@link #getFeeder()}, {@link #getNoSk()}.</li>
 * </ul>
 *
 * <h3>Catatan pemetaan</h3>
 * Kelas ini memperpanjang {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa. Hibernate karena
 * itu tidak memetakan properti apa pun milik induk. Deklarasi ULANG {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} di sini <b>bukan duplikasi keliru, melainkan
 * keharusan teknis</b> agar keempatnya benar-benar tersimpan. Anotasi {@code @Audited} membuat
 * setiap perubahan tercatat Envers &mdash; termasuk perubahan tak sengaja dari getter write-back di
 * atas. {@code dynamicInsert}/{@code dynamicUpdate} membuat pernyataan SQL hanya memuat kolom yang
 * benar-benar berubah.
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.KategoriPenghargaan
 * @see Siswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "penghargaan_siswa")
public class PenghargaanSiswa extends GeneralValueObject {

	/**
	 * Status awal sebuah pengajuan: sudah tercatat, belum disentuh petugas.
	 *
	 * <p>Ini juga nilai yang dikembalikan {@link #getStatus()} untuk baris yang kolom
	 * {@code status}-nya {@code null} atau kosong, sehingga baris lama hasil migrasi otomatis
	 * terbaca sebagai "Belum diproses" tanpa perlu pembaruan data.</p>
	 */
	public static final String BELUM_DIPROSES = "Belum diproses";
	/** Status antara: berkas sedang ditelaah petugas. Tidak mengunci tombol aksi apa pun. */
	public static final String SEDANG_DIPROSES = "Sedang diproses";
	/**
	 * Status akhir positif: capaian diakui.
	 *
	 * <p>Satu-satunya nilai yang berpengaruh pada tampilan &mdash; grup tombol Ubah/Hapus baris
	 * disembunyikan begitu status ini terpilih. Penguncian tersebut murni visual dan tidak
	 * diberlakukan ulang di jalur simpan/hapus.</p>
	 */
	public static final String DISETUJUI = "Disetujui";
	/** Status akhir negatif: pengajuan ditolak. Baris tetap dapat diubah dan dihapus. */
	public static final String DITOLAK = "Ditolak";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code sekolah.penghargaan_siswa.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini (jejak audit manual).
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna penyimpan terakhir, <b>mengabaikan nilai kosong</b>.
	 *
	 * <p>Bila {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung
	 * kembali tanpa mengubah apa pun. Jadi jejak audit yang sudah terisi tidak bisa dikosongkan
	 * lewat setter ini &mdash; perilaku sengaja, agar penyimpanan oleh proses tanpa konteks
	 * pengguna tidak menghapus nama penyimpan sebelumnya.</p>
	 *
	 * @param olehId id pengguna; nilai {@code null} atau kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna penyimpan terakhir, <b>mengabaikan nilai kosong</b>.
	 *
	 * <p>Berperilaku sama dengan {@link #setOlehId(String)}: {@code null} atau string kosong
	 * tidak menimpa nilai yang sudah ada.</p>
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini (jejak audit manual).
	 *
	 * @return nama pengguna penyimpan terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback {@code @PreUpdate} yang memperbarui cap waktu audit, dan &mdash; pada baris yang
	 * sama &mdash; deklarasi field {@code tanggal_dirubah} itu sendiri.
	 *
	 * <p>{@code onUpdate()} diserahkan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} dan dipanggil Hibernate
	 * tepat sebelum setiap {@code UPDATE}. Karena getter periode akademik di kelas ini menulis
	 * balik ke field yang dipetakan (lihat Javadoc kelas), callback ini dapat ikut terpicu pada
	 * baris yang sebenarnya tidak disunting siapa pun.</p>
	 *
	 * <p>Field {@code tanggal_dirubah} diinisialisasi ke waktu saat object dibuat lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()}, sehingga baris baru sudah bercap waktu meskipun
	 * belum pernah di-{@code UPDATE}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi; nilainya diisi otomatis oleh {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk object baru karena
	 *         field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<nama karya>"}.
	 *
	 * <p>Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga
	 * spasi di awal/akhir judul karya ikut tercetak apa adanya. Untuk baris yang belum tersimpan,
	 * bagian id berbunyi {@code "null"}.</p>
	 *
	 * @return gabungan id dan nama karya
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Judul karya/kejuaraan ("Nama Karya"); wajib. Lihat {@link #getNama()}. */
	private String nama;
	/** Tanggal mulai capaian ("Tanggal Pendaftaran Karya"). Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Tanggal selesai capaian. Lihat {@link #getTanggalSelesai()}. */
	private Date tanggalSelesai;
	/** Nomor sertifikat penghargaan; wajib menurut validasi layar. */
	private String nomorSertifikat;
	/** Status persetujuan; salah satu dari empat konstanta kelas ini. */
	private String status;
	/** Keterangan bebas. */
	private String keterangan;
	/** Siswa penerima; FK wajib. Lihat {@link #getSiswa()}. */
	private Siswa siswa;

	/** Yayasan cakupan; {@code null} berarti "Semua". Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Sekolah cakupan; {@code null} berarti "Semua". Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

	/** Bentuk penghargaan dari katalog global. Lihat {@link #getKategoriPenghargaan()}. */
	private KategoriPenghargaan kategoriPenghargaan;
	/** Uraian capaian; wajib menurut validasi layar. Lihat {@link #getCapaian()}. */
	private String capaian;
	/** Tautan bukti/publikasi. Lihat {@link #getUrl()}. */
	private String url;
	/** Tahun capaian; <b>ditimpa</b> oleh {@link #getTahun()} dari {@code tahunAkademik}. */
	private Integer tahun;

	/** Tahun ajaran, format {@code "2025/2026"}; diisi otomatis oleh {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Semester ganjil/genap; diisi otomatis oleh {@link #getJenisSemester()}. */
	private String jenisSemester;
	/** Judul karya dalam bahasa Inggris (kolom {@code namaen}). */
	private String namaEn;
	/** Penanda pelaporan feeder; nol pemanggil di seluruh sumber. Lihat {@link #getFeeder()}. */
	private String feeder;
	/** Lokasi/alamat penyelenggaraan. Lihat {@link #getAlamat()}. */
	private String alamat;
	/** Nomor SK penetapan penghargaan. */
	private String noSk;
	/** Tanggal SK penetapan penghargaan. */
	private Date tglSk;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate dan dipakai layar untuk membuat pengajuan baru
	 * ({@code onAdd()} memanggil {@code init(new PenghargaanSiswa())}).
	 *
	 * <p>Seluruh field dibiarkan {@code null} kecuali {@code tanggal_dirubah}, yang langsung berisi
	 * waktu saat ini. Perhatikan bahwa membaca {@link #getTahunAkademik()},
	 * {@link #getJenisSemester()}, atau {@link #getStatus()} pada object yang baru dibuat sudah
	 * menghasilkan nilai default &mdash; dua yang pertama bahkan menuliskannya ke field.</p>
	 */
	public PenghargaanSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} bersifat {@code IDENTITY} dan ditandai {@code insertable = false},
	 * jadi nilainya sepenuhnya ditentukan basis data dan baru terisi setelah {@code INSERT}.
	 * Nilai ini juga dipakai sebagai {@code ref} oleh baris {@code LampiranLain} yang menyimpan
	 * scan sertifikat, sehingga id yang sama muncul di tabel lampiran tanpa FK formal.</p>
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
	 * Menetapkan kunci utama.
	 *
	 * <p>Hanya dipakai Hibernate dan jalur pemuatan generik; jangan disetel manual pada baris yang
	 * sudah dikelola sesi.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul karya/kejuaraan, <b>sudah dipangkas</b> spasi awal-akhir.
	 *
	 * <p>Kolom {@code nama} bertipe {@code text} dan {@code nullable = false}, sehingga
	 * penyimpanan tanpa judul akan ditolak basis data; layar memvalidasinya lebih dulu dengan
	 * pesan "Nama Kejuaraan harus diisi". Pemangkasan hanya terjadi saat dibaca &mdash; nilai
	 * mentah dengan spasi tetap tersimpan di basis data, dan {@link #toString()} memakai nilai
	 * mentah itu.</p>
	 *
	 * @return judul karya yang sudah dipangkas, atau {@code null} bila field mentahnya {@code null}
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan judul karya/kejuaraan apa adanya (tanpa pemangkasan).
	 *
	 * @param nama judul karya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas pengajuan.
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas pengajuan.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal mulai capaian (label layar "Tanggal Pendaftaran Karya").
	 *
	 * <p>Kolom ini pula yang dipakai penyaring rentang "Tgl. Mulai"/"Tgl. Sampai" di layar
	 * pencarian &mdash; keduanya membandingkan kolom yang sama ini, bukan
	 * {@code tanggal} berpasangan dengan {@link #getTanggalSelesai()}.</p>
	 *
	 * @return tanggal mulai, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menetapkan tanggal mulai capaian.
	 *
	 * @param tanggal tanggal mulai
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan nomor sertifikat penghargaan.
	 *
	 * <p>Wajib diisi menurut validasi {@code onSave()} ("Nomor sertifikat kejuaraan harus diisi"),
	 * tetapi kolomnya sendiri tidak dibatasi {@code nullable = false} dan tidak berindeks unik:
	 * nomor sertifikat yang sama dapat dipakai berkali-kali, dan jalur non-layar (unggahan Excel,
	 * endpoint {@code /Data}) dapat menyimpannya kosong.</p>
	 *
	 * @return nomor sertifikat, atau {@code null}
	 */
	public String getNomorSertifikat() {
		return nomorSertifikat;
	}

	/**
	 * Menetapkan nomor sertifikat penghargaan.
	 *
	 * @param nomorSertifikat nomor sertifikat
	 */
	public void setNomorSertifikat(String nomorSertifikat) {
		this.nomorSertifikat = nomorSertifikat;
	}

	/**
	 * Mengembalikan status persetujuan, dengan {@link #BELUM_DIPROSES} sebagai default.
	 *
	 * <p>Bila kolom {@code status} bernilai {@code null} atau hanya berisi spasi, method
	 * mengembalikan {@link #BELUM_DIPROSES} <b>tanpa menuliskannya ke field</b> &mdash; berbeda
	 * dari {@link #getTahunAkademik()} dan {@link #getJenisSemester()} yang menulis balik. Default
	 * ini bersifat murni baca, jadi aman dipanggil dari renderer.</p>
	 *
	 * <p>Nilai kembalian dipakai langsung untuk perbandingan {@code equals(DISETUJUI)} di renderer
	 * yang menentukan apakah grup tombol Ubah/Hapus baris ditampilkan; karena tidak pernah
	 * {@code null}, perbandingan itu aman dari {@code NullPointerException}.</p>
	 *
	 * @return salah satu dari {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES},
	 *         {@link #DISETUJUI}, {@link #DITOLAK}, atau nilai lain apa pun yang sempat ditulis
	 *         jalur non-layar; tidak pernah {@code null} maupun kosong
	 */
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? BELUM_DIPROSES : status;
	}

	/**
	 * Menetapkan status persetujuan.
	 *
	 * <p>Tidak ada validasi bahwa nilainya termasuk salah satu dari empat konstanta kelas ini.
	 * Layar ZK hanya menawarkan keempatnya lewat {@code Combobox}, tetapi jalur unggahan Excel dan
	 * endpoint generik {@code /Data} dapat menuliskan teks bebas &mdash; yang kemudian tidak akan
	 * cocok dengan penyaring status mana pun sehingga baris tersebut praktis hilang dari hasil
	 * pencarian berstatus.</p>
	 *
	 * @param status status persetujuan
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan siswa penerima penghargaan.
	 *
	 * <p>FK {@code siswa} adalah <b>satu-satunya penanda kepemilikan</b> baris ini dan bersifat
	 * {@code nullable = false}. Layar pencarian menyaring lewat {@code createAlias("siswa",
	 * "siswa")}, dan seluruh pembatasan cakupan (siswa yang login, anak-anak wali murid, sekolah,
	 * yayasan) dijalankan melalui alias itu &mdash; termasuk pembatasan yang gagal-terbuka seperti
	 * dijelaskan di Javadoc kelas.</p>
	 *
	 * <p>Relasi memakai {@code CascadeType.PERSIST}/{@code MERGE} dan {@code FetchMode.SELECT},
	 * jadi setiap baris memicu query terpisah untuk mengambil siswanya. Renderer memanggil
	 * {@code getSiswa().getNim()} dan {@code getSiswa().getNama()} tanpa penjagaan {@code null};
	 * baris yatim akibat penghapusan siswa lewat SQL langsung akan menggagalkan render seluruh
	 * grid.</p>
	 *
	 * @return siswa penerima
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		return siswa;
	}

	/**
	 * Menetapkan siswa penerima penghargaan.
	 *
	 * <p><b>Tidak ada cek kepemilikan di sini maupun di pemanggilnya.</b> {@code onSave()} mengambil
	 * siswa dari atribut bandbox, yang pada layar dapat berasal dari parameter URL {@code ?siswa=}
	 * &mdash; lihat catatan IDOR pada Javadoc kelas.</p>
	 *
	 * @param siswa siswa penerima
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan yayasan cakupan pengajuan.
	 *
	 * @return yayasan, atau {@code null} yang di layar ditampilkan sebagai "Semua"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		return yayasan;
	}

	/**
	 * Menetapkan yayasan cakupan, <b>menormalkan object tanpa id menjadi {@code null}</b>.
	 *
	 * <p>Bila {@code yayasan} bernilai {@code null} atau ber-{@code id} {@code null} (mis. object
	 * baru yang belum tersimpan, atau item "Semua" pada combobox), field diisi {@code null}. Ini
	 * mencegah Hibernate mencoba meng-{@code cascade}-persist tenant baru dari layar pengajuan
	 * penghargaan.</p>
	 *
	 * @param yayasan yayasan cakupan; {@code null} atau object tanpa id menghasilkan {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan sekolah cakupan pengajuan.
	 *
	 * <p>Perhatikan bahwa kolom ini <b>bukan</b> sumber penyaringan tenant pada pencarian: layar
	 * menyaring lewat {@code siswa.sekolah}, bukan lewat kolom ini. Keduanya dapat berbeda, dan
	 * tidak ada validasi yang memastikan sekolah di sini sama dengan sekolah siswanya.</p>
	 *
	 * @return sekolah, atau {@code null} yang di layar ditampilkan sebagai "Semua"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		return sekolah;
	}

	/**
	 * Menetapkan sekolah cakupan, <b>menormalkan object tanpa id menjadi {@code null}</b>.
	 *
	 * <p>Berperilaku sama dengan {@link #setYayasan(Yayasan)}.</p>
	 *
	 * @param sekolah sekolah cakupan; {@code null} atau object tanpa id menghasilkan {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan bentuk penghargaan dari katalog global
	 * {@link ais.database.model.KategoriPenghargaan}.
	 *
	 * <p>Katalog itu <b>tidak bertenant</b> dan dibagi dengan modul perguruan tinggi
	 * ({@code PenghargaanMahasiswa}, {@code PenghargaanDosen}). Membuka layar penghargaan siswa
	 * pada instalasi yang katalognya masih kosong akan <b>menulis tiga baris seed</b> ("Paten",
	 * "HaKI", "Nasional / Internasional") &mdash; lihat catatan auto-seed pada Javadoc kelas.</p>
	 *
	 * @return kategori penghargaan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kategori_penghargaan", nullable = true)
	public KategoriPenghargaan getKategoriPenghargaan() {
		return kategoriPenghargaan;
	}

	/**
	 * Menetapkan bentuk penghargaan.
	 *
	 * @param kategoriPenghargaan baris katalog kategori
	 */
	public void setKategoriPenghargaan(KategoriPenghargaan kategoriPenghargaan) {
		this.kategoriPenghargaan = kategoriPenghargaan;
	}

	/**
	 * Mengembalikan uraian capaian, dengan <b>string kosong</b> sebagai pengganti {@code null}.
	 *
	 * <p>Default kosong ini murni baca (tidak ditulis balik ke field), dan ada agar renderer dapat
	 * memasang nilainya langsung ke {@code Label} tanpa penjagaan {@code null}. Efek sampingnya:
	 * kode pemanggil tidak dapat membedakan "belum diisi" dari "diisi string kosong" lewat getter
	 * ini.</p>
	 *
	 * @return uraian capaian, atau string kosong bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getCapaian() {
		return capaian == null ? "" : capaian;
	}

	/**
	 * Menetapkan uraian capaian.
	 *
	 * @param capaian uraian capaian
	 */
	public void setCapaian(String capaian) {
		this.capaian = capaian;
	}

	/**
	 * Mengembalikan tautan bukti/publikasi, dengan <b>string kosong</b> sebagai pengganti
	 * {@code null}.
	 *
	 * <p>Berperilaku sama dengan {@link #getCapaian()}. Nilainya ditampilkan apa adanya sebagai
	 * teks di grid ("Link: ..."), tidak diubah menjadi tautan yang dapat diklik dan tidak
	 * divalidasi sebagai URL.</p>
	 *
	 * @return tautan, atau string kosong bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getUrl() {
		return url == null ? "" : url;
	}

	/**
	 * Menetapkan tautan bukti/publikasi.
	 *
	 * @param url tautan
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Mengembalikan tanggal selesai capaian.
	 *
	 * <p>Wajib diisi menurut validasi {@code onSave()} ("Tanggal Selesai Kejuaraan harus diisi"),
	 * namun tidak ada pemeriksaan bahwa nilainya tidak mendahului {@link #getTanggal()}. Kolom ini
	 * hanya dipakai untuk ditampilkan ("Tanggal: X s.d Y"); tidak ada penyaring yang membacanya.</p>
	 *
	 * @return tanggal selesai, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Menetapkan tanggal selesai capaian.
	 *
	 * @param tanggalSelesai tanggal selesai
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Mengembalikan tahun capaian, <b>menurunkannya ulang dari {@code tahunAkademik} dan menimpa
	 * nilai tersimpan</b>.
	 *
	 * <p><b>Getter destruktif.</b> Setiap kali {@code tahunAkademik} tidak {@code null}, method ini
	 * mengurai bagian sebelum tanda garis miring ({@code "2025/2026"} &rarr; {@code 2025}) lalu
	 * <b>menugaskannya ke field {@code tahun}</b> &mdash; bukan hanya mengisi yang kosong,
	 * melainkan menimpa nilai yang sudah ada. Karena {@code tahun} adalah properti terpetakan,
	 * penimpaan itu ikut terbawa {@code UPDATE} pada flush berikutnya dan tercatat sebagai revisi
	 * Envers, meski tidak ada yang menyunting baris tersebut.</p>
	 *
	 * <p>Dampak nyatanya terbatas pada jalur non-layar: pada layar ZK, {@code Intbox} tahun memang
	 * {@code readonly} dan selalu diisi ulang dari combobox tahun akademik, jadi kedua nilai
	 * konsisten. Sebaliknya nilai {@code tahun} yang masuk lewat unggahan Excel (kolom
	 * {@code "tahun"} termasuk dalam daftar {@code contents}) atau lewat endpoint {@code /Data}
	 * akan <b>hilang tanpa peringatan</b> begitu baris itu dibaca.</p>
	 *
	 * <p>Penguraian yang gagal (format tahun akademik tidak berisi angka di depan garis miring)
	 * ditelan blok {@code catch} bertanda {@code auto-audit(empty-catch)}; nilai {@code tahun}
	 * sebelumnya dibiarkan apa adanya dan tidak ada pesan kesalahan yang sampai ke pengguna.</p>
	 *
	 * @return tahun capaian hasil penurunan, atau nilai tersimpan bila {@code tahunAkademik}
	 *         {@code null} atau penguraian gagal
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			try {
				tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PenghargaanSiswa.java:241");

			}
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun capaian.
	 *
	 * <p>Nilai yang disetel di sini <b>tidak awet</b>: pembacaan {@link #getTahun()} berikutnya
	 * akan menimpanya selama {@code tahunAkademik} terisi.</p>
	 *
	 * @param tahun tahun capaian
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun ajaran, <b>mengisinya dari konteks pengguna yang sedang login bila masih
	 * kosong</b>.
	 *
	 * <p><b>Getter write-back yang bergantung konteks.</b> Bila {@code tahunAkademik} bernilai
	 * {@code null}, method menugaskan hasil {@code Common.getCurrentTahunAkademik()} ke field.
	 * Helper itu membaca {@code Common.getCurrentUser()} lalu {@code RencanaTahunAkademik} milik
	 * konteks pengguna tersebut; bila tidak ada rencana yang cocok ia jatuh ke perhitungan
	 * kalender (bulan lebih dari Juni &rarr; {@code "tahun/tahun+1"}). Artinya nilai yang tertulis
	 * <b>bergantung pada siapa yang membaca baris itu dan kapan</b>, bukan pada kapan capaiannya
	 * terjadi.</p>
	 *
	 * <p>Karena {@code tahunAkademik} terpetakan, pengisian itu dapat ikut ter-{@code UPDATE} dan
	 * ter-audit Envers. Efek berantainya: {@link #getTahun()} kemudian menurunkan {@code tahun}
	 * dari nilai baru ini, sehingga satu kali render dapat mengubah dua kolom sekaligus pada baris
	 * lama yang periodenya kosong.</p>
	 *
	 * @return tahun ajaran format {@code "2025/2026"}; tidak pernah {@code null} setelah pemanggilan
	 *         pertama
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun ajaran.
	 *
	 * <p>Menyetel {@code null} secara eksplisit tidak akan bertahan: pembacaan berikutnya lewat
	 * {@link #getTahunAkademik()} langsung mengisinya kembali dari konteks pengguna.</p>
	 *
	 * @param tahunAkademik tahun ajaran, format {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenis semester, <b>mengisinya dari waktu berjalan bila masih kosong</b>.
	 *
	 * <p><b>Getter write-back.</b> Bila {@code jenisSemester} {@code null}, field diisi
	 * {@link ais.database.model.Perkuliahan#GANJIL} atau
	 * {@link ais.database.model.Perkuliahan#GENAP} berdasarkan
	 * {@code Common.isNowSemensterGanjil()} &mdash; sekali lagi berdasarkan <b>saat pembacaan</b>,
	 * bukan saat kejadian. Sama seperti {@link #getTahunAkademik()}, penulisan itu dapat
	 * dipersistensikan tanpa ada yang menyunting baris.</p>
	 *
	 * <p>Catatan konsistensi: nilai ini tidak pernah diselaraskan dengan {@link #getTanggal()}.
	 * Sebuah capaian bulan Februari yang barisnya baru dibuka pertama kali pada bulan September
	 * akan tercatat sebagai semester Ganjil.</p>
	 *
	 * @return {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}; tidak pernah {@code null}
	 *         setelah pemanggilan pertama
	 */
	public String getJenisSemester() {
		if (jenisSemester == null) {
			jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return jenisSemester;
	}

	/**
	 * Menetapkan jenis semester.
	 *
	 * @param jenisSemester {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan judul karya dalam bahasa Inggris (kolom {@code namaen}).
	 *
	 * <p>Berbeda dari {@link #getNama()}, nilainya tidak dipangkas dan tidak wajib. Renderer
	 * memasangnya ke {@code Label} tanpa penjagaan {@code null}.</p>
	 *
	 * @return judul berbahasa Inggris, atau {@code null}
	 */
	@Column(name = "namaen", columnDefinition = "text")
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menetapkan judul karya dalam bahasa Inggris.
	 *
	 * @param namaEn judul berbahasa Inggris
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan penanda pelaporan feeder, <b>menormalkan string kosong menjadi {@code null}</b>.
	 *
	 * <p><b>Kode mati.</b> Penelusuran seluruh sumber tidak menemukan satu pun pemanggil method ini
	 * maupun {@link #setFeeder(String)} di luar kelas ini; kolom {@code feeder} juga tidak masuk
	 * daftar {@code contents} untuk cetak/unggah Excel. Tombol "Krm ke feeder" yang seharusnya
	 * mengisinya memang dibuat di renderer, tetapi tidak pernah dipasang ke komponen induk mana
	 * pun sehingga tidak pernah muncul di layar. Jadi kolom ini selalu {@code null} pada instalasi
	 * yang hanya memakai jalur normal.</p>
	 *
	 * <p>Normalisasi di sini bersifat baca saja (tidak ditulis balik), sehingga string kosong yang
	 * terlanjur tersimpan tetap ada di basis data meski getter melaporkannya {@code null}.</p>
	 *
	 * @return penanda feeder yang sudah dipangkas, atau {@code null} bila kosong
	 */
	@Column(columnDefinition = "text")
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menetapkan penanda pelaporan feeder.
	 *
	 * <p>Nol pemanggil di seluruh sumber &mdash; lihat {@link #getFeeder()}.</p>
	 *
	 * @param feeder penanda feeder
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Mengembalikan lokasi/alamat penyelenggaraan (label layar "Lokasi / Alamat").
	 *
	 * <p>Jangan tertukar dengan penyaring "Penyelenggara" di layar pencarian: penyaring itu tidak
	 * terhubung ke kolom mana pun dan tidak berfungsi (lihat Javadoc kelas). Kolom ini pun tidak
	 * pernah ditampilkan di grid, hanya di formulir tambah/ubah.</p>
	 *
	 * @return lokasi/alamat, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menetapkan lokasi/alamat penyelenggaraan.
	 *
	 * @param alamat lokasi/alamat
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan nomor SK penetapan penghargaan.
	 *
	 * <p>Bersama {@link #getTglSk()} merupakan metadata legal pengesahan. Keduanya opsional, tidak
	 * divalidasi, tidak ditampilkan di grid, dan tidak dipakai penyaring mana pun &mdash; murni
	 * arsip pada formulir.</p>
	 *
	 * @return nomor SK, atau {@code null}
	 */
	public String getNoSk() {
		return noSk;
	}

	/**
	 * Menetapkan nomor SK penetapan penghargaan.
	 *
	 * @param noSk nomor SK
	 */
	public void setNoSk(String noSk) {
		this.noSk = noSk;
	}

	/**
	 * Mengembalikan tanggal SK penetapan penghargaan.
	 *
	 * @return tanggal SK, atau {@code null}
	 * @see #getNoSk()
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	/**
	 * Menetapkan tanggal SK penetapan penghargaan.
	 *
	 * @param tglSk tanggal SK
	 */
	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}
}
