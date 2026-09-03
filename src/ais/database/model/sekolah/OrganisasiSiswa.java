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
 * Entity <b>MASTER organisasi kesiswaan</b> &mdash; tabel {@code sekolah.organisasi_siswa}.
 *
 * <p>Satu baris mewakili satu wadah organisasi/kegiatan siswa di dalam sekolah: OSIS, MPK,
 * ekstrakurikuler (Pramuka, PMR, Paskibra, Rohis, klub olahraga/seni), dan sejenisnya. Domain ini
 * <b>terverifikasi dari kode</b>, bukan diduga dari nama kelas: Javadoc kelas saudara
 * {@code ais.action.master.sekolah.helper.SiswaPunyaOrganisasiSiswaHelper} menyebut relasi ini
 * sebagai keanggotaan siswa pada organisasi "mis. OSIS, ekstrakurikuler", layar masternya
 * ({@code /pages/master/sekolah/organisasi_siswa.zul}) berlabel "Organisasi" + tab "Jabatan", dan
 * dasbor {@code DashboardKegiatanKesiswaan} memakainya sebagai sumber blok kegiatan kesiswaan.</p>
 *
 * <p>Baris ini hanya menyimpan <i>identitas organisasi</i>: {@link #getKode() kode} (dibangkitkan
 * otomatis dari id), {@link #getNama() nama Indonesia}, {@link #getNamaEn() nama Inggris},
 * {@link #getKeterangan() keterangan}, serta cakupan tenant {@link #getYayasan()} /
 * {@link #getSekolah()}. <b>Siapa</b> siswa yang menjadi anggota, dengan jabatan apa, sejak kapan
 * sampai kapan, dan apakah pengajuannya sudah disetujui &mdash; semuanya disimpan pada entity
 * penghubung {@link OrganisasiSiswaPunyaSiswa}, bukan di sini.</p>
 *
 * <h2>Perbandingan dengan {@link ais.database.model.OrganisasiIntraKampus}</h2>
 * <p>Kelas ini adalah <b>padanan versi sekolah</b> dari
 * {@link ais.database.model.OrganisasiIntraKampus} (versi perguruan tinggi). Keduanya lahir dari
 * cetakan generator yang sama; perbandingannya dilakukan langsung atas kode kedua berkas:</p>
 * <ul>
 *   <li><b>SAMA persis</b>: blok jejak audit ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *       beserta {@link #onUpdate()} pada satu baris fisik yang sama), {@link #getId()},
 *       {@link #getNama()} (wajib isi + unik), {@link #getKeterangan()}, {@link #getKode()}
 *       (kata-per-kata, termasuk seluruh kuirk pembangkitan otomatis + tulis balik),
 *       {@link #getNamaEn()}, {@link #toString()}, serta anotasi kelas
 *       ({@code dynamicInsert}/{@code dynamicUpdate} + {@link Audited}). Bahkan
 *       {@code serialVersionUID}-nya bernilai <b>identik</b>
 *       ({@code 2463821577548439808L}).</li>
 *   <li><b>BERBEDA &mdash; sumbu cakupan.</b> Versi PT memakai
 *       {@code fakultas}/{@code jurusan}; kelas ini memakai {@link #getYayasan()} /
 *       {@link #getSekolah()}. Semantiknya sama: {@code null} berarti "Semua" (lihat
 *       {@link #getSekolah()}). Perbedaan penting: pasangan {@code yayasan}+{@code sekolah}
 *       adalah <b>sumbu multi-tenant</b> aplikasi ini, sehingga kelalaian menyaringnya berdampak
 *       lintas-institusi &mdash; bukan sekadar lintas-fakultas seperti pada versi PT.</li>
 *   <li><b>TIDAK ADA di sini &mdash; syarat akademik keanggotaan.</b> Versi PT punya tiga properti
 *       {@code minimalIpk}/{@code minimalSks}/{@code minimalSkkm} yang benar-benar ditegakkan saat
 *       mahasiswa mendaftar mandiri. Kelas ini <b>tidak punya padanan apa pun</b>: tidak ada field
 *       {@code minimal*}, dan {@code AmbilDataOrganisasiForOrganisasiSiswaHelper#save()}
 *       (pendaftaran mandiri siswa) tidak pernah memanggil pemeriksaan syarat apa pun sebelum
 *       membuat baris keanggotaan. Konsekuensi menguntungkan: <b>temuan "fail-open pada
 *       pemeriksaan syarat" yang tercatat pada versi PT tidak punya padanan di sini</b> &mdash;
 *       karena memang tidak ada syarat yang diperiksa sama sekali.</li>
 *   <li><b>TIDAK ADA di sini &mdash; tingkat/level organisasi</b>, sama seperti versi PT (tidak ada
 *       kelas {@code LevelOrganisasiSiswa} di paket ini). Wajar secara domain: organisasi
 *       kesiswaan menurut definisinya bertingkat lokal-sekolah.</li>
 * </ul>
 * <p>Sama seperti versi PT, kelas ini maupun {@link JabatanOrganisasiSiswa} <b>tidak</b> terdaftar
 * sebagai entity yang di-<i>preload</i> ke cache in-memory oleh {@code ais.common.InitData} (nol
 * kemunculan). Konsekuensinya {@code check(...)} pada {@link #getYayasan()}/{@link #getSekolah()}
 * lebih sering benar-benar menyentuh session/database.</p>
 *
 * <h2>Posisi dalam keluarga entity</h2>
 * <ul>
 *   <li>{@link OrganisasiSiswaPunyaSiswa} &mdash; keanggotaan seorang {@link Siswa} pada satu
 *       organisasi; satu-satunya entity yang menunjuk balik ke sini lewat properti
 *       {@code organisasiSiswa} (kolom {@code organisasi_siswa} pada tabel
 *       {@code sekolah.organisasi_siswa_punya_siswa}). Menyimpan
 *       {@code jabatanOrganisasiSiswa}, {@code mulai}/{@code sampai}, {@code persetujuan},
 *       {@code keterangan}, {@code tbmuser} (identitas pengaju), dan lampiran SK lewat
 *       {@code LampiranLain}.</li>
 *   <li>{@link JabatanOrganisasiSiswa} &mdash; master <b>jabatan siswa di dalam organisasi</b>
 *       (Ketua/Pengurus/Anggota). Dirujuk oleh {@link OrganisasiSiswaPunyaSiswa}, <b>bukan</b> oleh
 *       entity ini. Master itu di-<i>include</i> sebagai tab kedua pada layar yang sama.</li>
 *   <li>{@link Yayasan} / {@link Sekolah} &mdash; cakupan organisasi; keduanya boleh {@code null}
 *       yang berarti "Semua" (lihat {@link #getSekolah()}).</li>
 * </ul>
 *
 * <h2>Dari mana baris ini dibuat/diubah</h2>
 * <ol>
 *   <li><b>Layar master</b> &mdash; {@code ais.action.master.sekolah.OrganisasiSiswaAction}
 *       ({@code /pages/master/sekolah/organisasi_siswa.zul}). Menyediakan CRUD
 *       (Tambah/Ubah/Hapus), pencarian per kode/nama/yayasan/sekolah serta per nama+NIS siswa
 *       anggotanya dan per Guru BK-nya, panel detail keanggotaan
 *       ({@code OrganisasiSiswaPunyaSiswaHelper}), dan tab bawaan untuk master
 *       {@link JabatanOrganisasiSiswa}.</li>
 *   <li><b>Impor Excel per-organisasi</b> &mdash; {@code OrganisasiSiswaAction#onUploadData}.
 *       Setiap <i>sheet</i> pada berkas {@code .xlsx} dicocokkan ke satu organisasi
 *       <b>berdasarkan {@link #getKode() kode}</b> (nama sheet = kode). Bila tidak ketemu,
 *       organisasi baru dibuat otomatis dengan {@code nama} dan {@code keterangan} = nama sheet
 *       &mdash; <b>tanpa {@code yayasan}/{@code sekolah}</b>, sehingga lahir berlingkup "Semua"
 *       (lihat kuirk pada {@link #getKode()} dan {@link #getSekolah()}). Isi sheet (kolom NIS)
 *       kemudian dipakai membuat baris {@link OrganisasiSiswaPunyaSiswa}.</li>
 *   <li><b>Impor/ekspor generik</b> &mdash; {@code Common.uploadData}/{@code Common.cetakData}
 *       dengan daftar kolom {@code id, nama, namaEn, yayasan, sekolah, keterangan}. Perhatikan
 *       {@link #getKode() kode} <b>tidak</b> termasuk kolom yang diekspor/diimpor &mdash; sama
 *       seperti versi PT &mdash; padahal justru kode itulah kunci pencocokan sheet pada impor
 *       per-organisasi di atas.</li>
 * </ol>
 * <p><b>Tidak ada auto-seed.</b> Nol kemunculan {@code OrganisasiSiswa} pada
 * {@code ais/common/InitData*}: seluruh isi tabel ini diketik/diimpor admin. Tidak ada baris
 * bawaan OSIS/Pramuka yang tercipta sendiri saat instalasi.</p>
 *
 * <h2>Siapa yang membaca baris ini</h2>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.helper.AmbilDataOrganisasiForOrganisasiSiswaHelper}
 *       &mdash; <b>pendaftaran mandiri siswa</b>: grid berisi organisasi yang dapat dipilih.
 *       Filternya memakai pola {@code isNull(sekolah) OR eq(sekolah, pilihan)} sehingga organisasi
 *       berlingkup "Semua" selalu ikut muncul.</li>
 *   <li>{@code ais.action.master.sekolah.helper.OrganisasiSiswaPunyaSiswaHelper} &mdash; panel
 *       detail daftar anggota pada layar master (arah organisasi &rarr; siswa).</li>
 *   <li>{@code ais.action.master.sekolah.helper.SiswaPunyaOrganisasiSiswaHelper} &mdash; arah
 *       sebaliknya (siswa &rarr; organisasi), dipakai halaman profil dan dasbor.</li>
 *   <li>{@code ais.action.master.dashboard.admin.DasboardSiswa} &mdash; kartu "Total Organisasi"
 *       lewat {@code countGeneric(OrganisasiSiswa.class, false)}, dan daftar kolom detail
 *       {@code organisasiSiswa, jabatanOrganisasiSiswa, siswa.namaSiswa, mulai, sampai,
 *       persetujuan}.</li>
 *   <li>{@code ais.action.master.dashboard.admin.DashboardKegiatanKesiswaan} &mdash; agregasi
 *       kegiatan kesiswaan per organisasi.</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanOrganisasiSiswa} (cetakan per siswa, juga
 *       dipanggil {@code CommonReportHelper#onCetakOrganisasiSiswa}) dan
 *       {@code LaporanPerOrganisasiSiswa} (cetakan per organisasi, dibuka dari tombol
 *       "Organisasi Siswa" pada toolbar layar master).</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ul>
 *   <li><b>Jejak audit</b> (dideklarasikan ulang dari base class, lihat catatan di bawah):
 *       {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()} beserta
 *       setter-nya, dan callback {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *       {@link #getNamaEn()}, {@link #toString()}.</li>
 *   <li><b>Cakupan tenant</b>: {@link #getYayasan()}, {@link #getSekolah()}.</li>
 *   <li><b>Deskriptif</b>: {@link #getKeterangan()}.</li>
 * </ul>
 * <p><b>Tidak ada method utilitas, query statis, konstanta domain, maupun logika bisnis lain di
 * kelas ini.</b> Satu-satunya logika non-trivial ada pada {@link #getKode()} (pembangkitan kode +
 * tulis balik) dan pada empat setter yang menolak nilai kosong/tanpa id
 * ({@link #setOleh(String)}, {@link #setOlehId(String)}, {@link #setYayasan(Yayasan)},
 * {@link #setSekolah(Sekolah)}).</p>
 *
 * <h2>Verifikasi pola berulang keluarga entity ini</h2>
 * <p>Diperiksa langsung dari kode kelas ini, bukan diasumsikan dari entity lain:</p>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field/DB</b>: <b>ADA satu</b> &mdash; {@link #getKode()}
 *       membangkitkan kode dari {@link #getId() id} lalu <b>menyimpannya ke field {@code kode}</b>.
 *       Karena {@code kode} adalah properti terpetakan Hibernate (getter tanpa anotasi &rarr;
 *       pemetaan implisit ke kolom {@code kode}), sekadar me-render daftar organisasi (renderer
 *       memanggil {@code getKode()} per baris) atau menjalankan tombol "Download Data Siswa"
 *       (memanggil {@code getKode()} sebagai nama sheet) sudah cukup untuk memicu {@code UPDATE}
 *       saat flush &mdash; termasuk satu revisi Envers baru per baris.</li>
 *   <li><b>Getter destruktif</b> (getter yang mengosongkan data seperti
 *       {@code Komentar#getTbmuser()}): <b>TIDAK ADA</b> di kelas ini &mdash; semua relasi murni
 *       baca. Pola itu justru muncul pada entity penghubungnya; lihat "Catatan keamanan".</li>
 *   <li><b>Getter yang menutup session Hibernate</b>: <b>TIDAK ADA</b>. Nol pemanggilan
 *       {@code HibernateUtil.closeSession()} di kelas ini.</li>
 *   <li><b>{@code getKeterangan()} yang membalik kontrak</b> (mengembalikan nama/kode alih-alih
 *       keterangan, seperti pada sejumlah entity penghubung): <b>TIDAK ADA</b> &mdash;
 *       {@link #getKeterangan()} di sini benar-benar mengembalikan field {@code keterangan} apa
 *       adanya, tanpa fallback maupun tulis balik.</li>
 *   <li><b>{@code getNama()} yang membangkitkan ulang label</b> (pola {@code Kota}/
 *       {@code Penghasilan}): <b>TIDAK ADA</b> &mdash; {@link #getNama()} hanya
 *       me-{@code trim()} nilai yang dikembalikan, tanpa menulis balik ke field.</li>
 *   <li><b>Konsistensi {@code check()}</b>: <b>simetris</b> &mdash; kedua relasi yang ada
 *       ({@link #getYayasan()}, {@link #getSekolah()}) sama-sama memakai {@code check(...)} dengan
 *       {@code FetchType.LAZY}, persis seperti pasangan {@code fakultas}/{@code jurusan} pada
 *       versi PT. Tidak ada asimetri seperti pada {@code OrganisasiDosen}.</li>
 *   <li><b>Bug penciutan {@code TreeSet}</b> (baris hilang senyap karena {@code compareTo} yang
 *       menyamakan dua baris berbeda): <b>TIDAK BERLAKU</b> &mdash; kelas ini tidak punya
 *       {@code compareTo}, tidak punya koleksi apa pun, dan seluruh konsumennya memakai
 *       {@code List}.</li>
 * </ul>
 *
 * <h2>Catatan {@code GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
 * &mdash; ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti apa pun yang
 * dideklarasikan di sana. Karena itu field {@link #id}, {@link #oleh}, {@link #olehId}, dan
 * {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di kelas ini agar ikut tersimpan;
 * pengulangan tersebut <b>keharusan teknis, bukan bug</b>. Konsekuensi lain: properti warisan yang
 * <i>tidak</i> dideklarasikan ulang (misalnya {@code diubahDari}) tidak tersimpan ke database dan
 * akan kembali {@code null} setelah baris dimuat ulang &mdash; perhatikan bahwa entity penghubung
 * {@link OrganisasiSiswaPunyaSiswa} justru <i>mendeklarasikan</i> {@code diubahDari} (diisi
 * {@code OrganisasiSiswaAction} saat impor Excel), jadi perilakunya berbeda dari kelas ini.</p>
 *
 * <h2>Catatan Envers</h2>
 * <p>Kelas ditandai {@link Audited}, sehingga setiap perubahan baris tersalin ke tabel revisi dan
 * dapat ditelusuri lewat {@code RevisiHelper.createNewRevisi(OrganisasiSiswa.class, ...)} yang
 * dipasang pada kolom "Nama Organisasi" di grid layar master. Kombinasi
 * {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menuliskan kolom yang
 * benar-benar berubah.</p>
 *
 * <h2>Catatan keamanan (hasil audit, tidak diperbaiki di sini)</h2>
 * <p>Ketiga temuan pertama di bawah <b>terverifikasi ulang langsung pada
 * {@code ais/action/master/sekolah/OrganisasiSiswaAction.java}</b> dan merupakan kembaran temuan
 * yang sudah tercatat pada {@link ais.database.model.OrganisasiIntraKampus}:</p>
 * <ul>
 *   <li><b>Inversi hak akses &mdash; TERKONFIRMASI, dan lebih parah dari versi PT.</b> Pada
 *       {@code OrganisasiSiswaAction}, penjagaan tombol Ubah dan Hapus di renderer grid
 *       <b>dikomentari mati</b> ({@code // button.setVisible(edit);} dan
 *       {@code // button.setVisible(delete);}). Berbeda dari versi PT yang setidaknya masih
 *       menyisakan deklarasi terkomentari {@code // private boolean edit = false;}, kelas sekolah
 *       ini <b>tidak mendeklarasikan bendera {@code edit}/{@code delete} sama sekali</b> &mdash;
 *       artinya kode itu tidak akan kompilasi bila komentarnya sekadar dibuka; jejak niat asalnya
 *       sudah hilang seluruhnya. Tidak ada satu pun pemanggilan
 *       {@code CommonPrivilages.checkPrevilages(...)} di seluruh kelas Action ini, dan tombol
 *       Tambah ({@code add}) juga tidak pernah di-{@code setVisible(...)} sehingga default
 *       terlihat &mdash; ikut mengaktifkan kedua tombol impor massal yang bergantung padanya
 *       ({@code upload.setVisible(add != null &amp;&amp; add.isVisible())} dan
 *       {@code uploadData}). Satu-satunya gerbang adalah {@code Common.doCheckSecurity()} yang
 *       hanya memeriksa hak <b>READ tingkat menu</b>. Efeknya: siapa pun yang bisa membuka layar
 *       master dapat menambah, mengubah, menghapus, dan mengimpor massal baris organisasi.
 *       <b>Bukti bahwa ini anomali, bukan gaya arsitektur:</b> kelas saudara
 *       {@link JabatanOrganisasiSiswa}-nya, {@code JabatanOrganisasiSiswaAction} &mdash; yang
 *       di-<i>include</i> sebagai <b>tab kedua pada layar yang sama persis</b> &mdash; memasang
 *       lengkap {@code add.setVisible(checkPrevilages(CREATE))},
 *       {@code edit = checkPrevilages(UPDATE)}, {@code delete = checkPrevilages(DELETE)}, dan
 *       benar-benar memakai keduanya di renderer-nya. Helper detail
 *       {@code OrganisasiSiswaPunyaSiswaHelper} juga menghitung {@code delete} dengan benar,
 *       menegaskan bahwa hanya kelas Action inilah yang bolong.</li>
 *   <li><b>SQL injection pada {@code OrganisasiSiswaAction#initCriteria} &mdash; TERKONFIRMASI.</b>
 *       Nilai kotak pencarian "Nama Siswa" ({@code searchnamamhs}) dan "NIS Siswa"
 *       ({@code searchnim}) disisipkan <b>mentah</b> (tanpa escaping, tanpa parameter bind) ke
 *       dalam string yang diserahkan ke {@code Restrictions.sqlRestriction(...)}. Instance ini
 *       identik dengan yang tercatat pada versi PT. <b>Namun ada nuansa penting yang harus
 *       disampaikan apa adanya:</b> pada varian sekolah, SQL di sekitarnya sendiri <b>rusak</b>
 *       akibat find/replace penambahan skema yang serampangan &mdash; nama <i>kolom</i>
 *       {@code organisasi_siswa} ikut diberi awalan menjadi {@code sekolah.organisasi_siswa} di
 *       daftar {@code SELECT}, klausa {@code WHERE}, dan {@code GROUP BY}, padahal di dalam
 *       subquery itu tidak ada tabel/alias bernama {@code sekolah} (alias yang ada hanya
 *       {@code a} dan {@code b}), sementara tabel {@code siswa} pada {@code inner join} justru
 *       <i>kehilangan</i> awalan skemanya. Karena bagian yang rusak berada <b>sebelum</b> titik
 *       penyisipan, penyerang tidak bisa "memperbaikinya" lewat isian sendiri; kemungkinan besar
 *       cabang ini selalu melempar {@code SQLGrammarException}. Konsekuensinya <b>ganda</b>:
 *       (a) filter "Nama Siswa"/"NIS Siswa"/"Guru BK" pada layar ini praktis <b>tidak berfungsi
 *       sama sekali</b> (bug fungsional nyata), dan (b) celah injeksinya untuk saat ini
 *       kemungkinan tidak dapat dieksploitasi &mdash; tetapi tetap <b>wajib diperbaiki</b>,
 *       karena begitu SQL-nya dibetulkan (perbaikan bug yang sangat mungkin dilakukan orang
 *       berikutnya) celahnya langsung hidup sepenuhnya. Cabang "Guru BK" memakai
 *       {@code dsn.getId()} yang bertipe {@code Long} sehingga aman dari injeksi, tetapi memakai
 *       SQL rusak yang sama.</li>
 *   <li><b>Layar master tidak menyaring per yayasan/sekolah pengguna &mdash; TERKONFIRMASI,
 *       fail-open.</b> {@code initCriteria} hanya memakai nilai combobox pencarian
 *       ({@code searchyayasan}/{@code searchsekolah}); <b>tidak ada</b> pembatasan sisi server
 *       berdasarkan tenant pengguna yang login. Pembatasannya semata-mata kosmetik:
 *       {@code Common.initYayasanDanSekolahDanSemua(...)} hanya memilih lalu me-{@code disable}
 *       combobox, dan hanya bila konteks sekolah/yayasan pengguna berhasil ditemukan. Bila akun
 *       tidak terikat sekolah maupun yayasan (atau berperan admin), kedua combobox tetap
 *       "Semua" &rarr; seluruh organisasi <b>lintas sekolah dan lintas yayasan</b> ikut
 *       terlihat. Tombol "Download Data Siswa" pada layar yang sama karenanya dapat menghasilkan
 *       satu berkas Excel berisi <b>NIS + nama + jabatan + tanggal + tautan berkas SK</b> seluruh
 *       anggota semua organisasi di semua tenant sekaligus (dan tautan SK-nya berupa URL
 *       {@code CommonMedia.getFile(...)} yang dapat dibagikan). Ini instance baru dari pola
 *       fail-open cakupan tenant yang sudah berulang kali tercatat pada modul {@code sekolah}.</li>
 *   <li><b>Impor Excel melewati alur persetujuan dan seluruh gerbang.</b>
 *       {@code OrganisasiSiswaAction#onUploadData} membuat baris
 *       {@link OrganisasiSiswaPunyaSiswa} massal dari kolom NIS, dan mengisi kolom
 *       {@code persetujuan} <b>langsung dari sel berkas</b> &mdash; sehingga keanggotaan dapat
 *       disetujui borongan tanpa melewati layar persetujuan. Sheet yang kodenya tak dikenal juga
 *       membuat baris master baru (jalur CREATE tanpa gerbang; lihat {@link #getKode()}).
 *       Pencarian siswanya hanya berdasarkan {@code nim} tanpa filter sekolah, sehingga satu
 *       berkas dapat menarik siswa dari sekolah/yayasan mana pun ke dalam organisasi mana pun.
 *       Prosesnya berjalan pada {@code Thread} terpisah dengan session Hibernate sendiri, di luar
 *       konteks keamanan request.</li>
 *   <li><b>Aksi massal "Bersihkan" tanpa gerbang &mdash; tetapi RUSAK.</b> Pada
 *       {@code OrganisasiSiswaPunyaSiswaHelper}, tombol "Bersihkan" menjalankan {@code DELETE}
 *       SQL native atas seluruh anggota yang belum disetujui tanpa memeriksa
 *       {@code CommonPrivilages.DELETE} (padahal bendera {@code delete} sudah dihitung dan
 *       dipakai di renderer berkas yang sama), dan bila berjalan akan melewati Envers serta
 *       {@code AuditListener}. <b>Namun SQL-nya menyebut tabel dan kolom milik versi PT</b>
 *       ({@code delete from organisasi_intra_kampus_punya_siswa ... and organisasi_intra_kampus =
 *       ...}) &mdash; hasil find/replace setengah jalan dari {@code mahasiswa} ke {@code siswa}
 *       yang tidak menyentuh {@code intra_kampus}. Tabel itu tidak ada, sehingga tombol ini
 *       selalu gagal dan menampilkan dialog error. Efeknya <b>gagal-tertutup</b>: fitur mati,
 *       risiko nol untuk saat ini &mdash; sampai seseorang "memperbaiki" nama tabelnya tanpa
 *       menambahkan gerbang hak akses. Tombol "Ambil Siswa" di panel yang sama juga tanpa
 *       gerbang.</li>
 *   <li><b>Identitas pengaju keanggotaan.</b> Pada entity penghubung
 *       {@link OrganisasiSiswaPunyaSiswa}, periksa perilaku {@code getTbmuser()} sebelum
 *       mengandalkan kolom pengaju &mdash; keluarga entity ini punya riwayat getter destruktif
 *       yang mengosongkan kolom tersebut saat pengajunya berupa akun siswa/mahasiswa (pola
 *       {@code OrganisasiDosenPunyaDosen#getTbmuser()} dan
 *       {@code OrganisasiIntraKampusPunyaMahasiswa#getTbmuser()}). Kelas <i>ini</i> sendiri tidak
 *       punya relasi {@code tbmuser}.</li>
 * </ul>
 *
 * <h2>Kuirk salin-tempel dari versi PT (bukan celah keamanan)</h2>
 * <p>Layar sekolah ini masih memakai sejumlah teks dan nama peninggalan versi perguruan tinggi;
 * berguna diketahui agar pembaca kode tidak salah mengira ada dua fitur berbeda:</p>
 * <ul>
 *   <li>Judul dialog tambah/ubah pada {@code OrganisasiSiswaAction#init} berbunyi <b>"Tambah
 *       Organisasi Intra Kampus"</b> / <b>"Ubah Organisasi Intra Kampus"</b>.</li>
 *   <li>Label form dan judul kolom grid untuk relasi {@link #getSekolah()} berbunyi
 *       <b>"Prodi"</b>, bukan "Sekolah".</li>
 *   <li>Nama field pencarian di kelas Action masih {@code searchnamamhs}/{@code searchnim}
 *       ("mhs"/"nim" = mahasiswa/NIM) meski label ZUL-nya sudah "Nama Siswa"/"NIS Siswa"; hal
 *       yang sama berlaku untuk variabel {@code criterionGuruPa} ("PA" = Pembimbing Akademik).</li>
 *   <li>Pesan progres impor memakai kata gabungan mentah "organisasiSiswa".</li>
 * </ul>
 *
 * <h2>Kuirk penghitungan dasbor</h2>
 * <p>Bukan celah keamanan, tetapi berdampak ke angka yang dilaporkan:
 * {@code DasboardSiswa#countGeneric} menambahkan {@code Restrictions.eq("sekolah",
 * currentSekolah)} bila konteks sekolah aktif. Karena {@code sekolah} boleh {@code null} yang
 * berarti "Semua" (lihat {@link #getSekolah()}), <b>organisasi berlingkup "Semua" tidak pernah
 * ikut terhitung</b> pada kartu "Total Organisasi" milik sebuah sekolah &mdash; padahal
 * organisasi yang dibuat otomatis oleh impor Excel selalu lahir berlingkup "Semua". Sebaliknya
 * bila konteks sekolah kosong, penghitungnya menjumlah seluruh tenant.</p>
 *
 * <h2>Kuirk komentar generator</h2>
 * <p>Komentar aslinya berbunyi <i>"Bank generated by hbm2java"</i> &mdash; artefak salin-tempel
 * dari {@code ais.database.model.Bank}, sumber yang sama yang dibajak puluhan entity lain di
 * paket ini. Komentar itu digantikan Javadoc di atas; tidak ada hubungan apa pun antara entity
 * ini dan {@code Bank}.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.OrganisasiIntraKampus
 * @see OrganisasiSiswaPunyaSiswa
 * @see JabatanOrganisasiSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "organisasi_siswa")



public class OrganisasiSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>identik</b> dengan
	 * {@link ais.database.model.OrganisasiIntraKampus} dan sejumlah entity master lain hasil
	 * generator {@code hbm2java}; duplikasi ini tidak menimbulkan masalah karena
	 * {@code serialVersionUID} hanya dibandingkan antar versi kelas yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris, kolom {@code id}. Dideklarasikan ulang dari base class (lihat Javadoc kelas). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang dari base class. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang dari base class. */
	private String olehId;

	/**
	 * ID pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 *
	 * <p>Properti ini <b>tidak</b> beranotasi {@code @Column}, sehingga Hibernate memetakannya
	 * secara implisit ke kolom {@code olehid}. Dideklarasikan ulang di kelas ini karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass} (lihat Javadoc kelas).</p>
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna pengubah terakhir, <b>menolak nilai kosong</b>.
	 *
	 * <p><b>Perilaku non-trivial:</b> bila {@code olehId} bernilai {@code null} atau hanya berisi
	 * spasi, method langsung {@code return} tanpa mengubah apa pun &mdash; nilai lama
	 * dipertahankan. Tujuannya menjaga agar jejak audit yang sudah ada tidak terhapus oleh proses
	 * yang berjalan tanpa konteks pengguna (mis. thread impor Excel, job terjadwal, atau
	 * deserialisasi). Konsekuensinya: <b>kolom ini tidak dapat dikosongkan lewat setter</b>.</p>
	 *
	 * @param olehId ID pengguna pengubah; nilai {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir, <b>menolak nilai kosong</b> dengan alasan dan
	 * konsekuensi yang sama persis dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; nilai {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. Dipetakan implisit ke kolom
	 * {@code oleh}. Dideklarasikan ulang di kelas ini karena alasan yang sama dengan
	 * {@link #getOlehId()}.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini, sehingga organisasi yang dibuat otomatis oleh impor Excel (lihat
	 * {@link #getKode()}) tidak punya jejak audit sampai ada penyuntingan berikutnya. Perhatikan
	 * pula bahwa impor Excel berjalan pada thread terpisah tanpa konteks pengguna, sehingga
	 * kedua setter penolak-kosong di atas kemungkinan besar tidak menuliskan apa pun.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja diletakkan pada baris fisik yang sama
	 * dalam kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir baris ini.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini
	 * <b>tidak</b> menolak {@code null} &mdash; nilai {@code null} akan benar-benar mengosongkan
	 * kolom {@code tanggal_dirubah}. Normalnya hanya dipanggil
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini, kolom {@code tanggal_dirubah} bertipe
	 * {@code TIMESTAMP}.
	 *
	 * <p>Field-nya diinisialisasi ke waktu pembuatan objek Java (lihat {@link #onUpdate()}),
	 * sehingga baris baru pun sudah punya nilai walau {@code @PreUpdate} belum pernah
	 * berjalan.</p>
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null} kecuali sengaja
	 *         dikosongkan lewat {@link #setTanggal_dirubah(Date)}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam format {@code "<id>-<nama>"}, mis.
	 * {@code "12-OSIS"}.
	 *
	 * <p>Membaca field {@code id} dan {@code nama} <b>secara langsung</b> (bukan lewat getter),
	 * sehingga {@code nama} tidak di-{@code trim()} seperti pada {@link #getNama()} dan
	 * pemanggilan ini <b>tidak</b> memicu pembangkitan kode seperti {@link #getKode()}. Untuk
	 * baris yang belum disimpan hasilnya berawalan {@code "null-"}.</p>
	 *
	 * <p>Dipakai antara lain sebagai label item combobox pada layar-layar yang memilih
	 * organisasi.</p>
	 *
	 * @return gabungan id dan nama dipisah tanda hubung; tidak pernah {@code null}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama organisasi (bahasa Indonesia). Kolom {@code nama}: wajib isi dan UNIK se-tabel. */
	private String nama;
	/** Keterangan bebas. Kolom {@code keterangan}, opsional. */
	private String keterangan;
	/**
	 * Kode organisasi 5 digit. Dipetakan implisit ke kolom {@code kode} (getter-nya tanpa
	 * anotasi), dan <b>ditulis balik</b> oleh {@link #getKode()} bila masih kosong.
	 */
	private String kode;
	/** Cakupan yayasan; {@code null} berarti "Semua". Kolom FK {@code yayasan}. */
	private Yayasan yayasan;
	/** Cakupan sekolah; {@code null} berarti "Semua". Kolom FK {@code sekolah}. */
	private Sekolah sekolah;

	
	/** Nama organisasi dalam bahasa Inggris. Kolom {@code namaen}, opsional. */
	private String namaEn;

	/**
	 * Konstruktor tanpa argumen &mdash; satu-satunya konstruktor kelas ini.
	 *
	 * <p>Wajib ada dan wajib publik agar Hibernate dapat meng-instansiasi baris hasil query.
	 * Dipakai juga oleh {@code OrganisasiSiswaAction#onAdd} (form tambah) dan
	 * {@code OrganisasiSiswaAction#onUploadData} (pembuatan organisasi baru dari nama sheet).
	 * Seluruh properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang langsung
	 * terisi waktu sekarang (lihat {@link #onUpdate()}).</p>
	 */
	public OrganisasiSiswa() {
	}

	/**
	 * Primary key baris, kolom {@code id}.
	 *
	 * <p>Dibangkitkan database dengan strategi {@link javax.persistence.GenerationType#IDENTITY}
	 * (sequence/serial PostgreSQL), karena itu {@code insertable = false}. Nilainya berurutan dan
	 * dapat ditebak &mdash; perhatikan bahwa {@link #getKode()} menurunkan kode organisasi
	 * langsung dari id ini.</p>
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
	 * Menetapkan primary key. Praktis hanya dipakai Hibernate saat memuat baris; kode aplikasi
	 * tidak boleh memanggilnya untuk baris yang sudah tersimpan.
	 *
	 * @param id id baris; boleh {@code null} untuk objek baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama organisasi dalam bahasa Indonesia &mdash; kolom {@code nama}, <b>wajib isi</b>
	 * ({@code nullable = false}), panjang maksimum 255, dan <b>UNIK se-tabel</b>
	 * ({@code unique = true}).
	 *
	 * <p><b>Perhatikan konsekuensi keunikan global:</b> batasan unik itu <b>tidak</b> memandang
	 * cakupan {@link #getYayasan()}/{@link #getSekolah()}, sehingga dua sekolah berbeda pada
	 * instalasi multi-tenant yang sama <b>tidak bisa</b> masing-masing punya organisasi bernama
	 * "OSIS" atau "Pramuka". Validasi sisi aplikasi
	 * ({@code OrganisasiSiswaAction#checkNamaOrganisasiSiswa}) juga mencocokkan nama tanpa filter
	 * tenant apa pun, dan pesan yang ditampilkan &mdash; "Nama Organisasi sudah ada di database"
	 * &mdash; membocorkan keberadaan data milik tenant lain kepada operator sekolah mana pun.</p>
	 *
	 * <p><b>Perilaku getter:</b> nilai yang dikembalikan di-{@code trim()}, <b>tanpa</b> ditulis
	 * balik ke field &mdash; jadi getter ini tidak menimbulkan {@code UPDATE} tak terduga
	 * (berbeda dari {@link #getKode()}). Efek sampingnya justru pada perbandingan: nilai
	 * ber-spasi ekstra di database akan <i>tampak</i> sudah rapi walau isi kolomnya tidak.</p>
	 *
	 * @return nama organisasi tanpa spasi di tepi, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama organisasi. Disimpan apa adanya &mdash; <b>tidak</b> di-{@code trim()}
	 * (pemangkasan hanya terjadi saat dibaca lewat {@link #getNama()}).
	 *
	 * <p>Dipanggil dari form tambah/ubah ({@code OrganisasiSiswaAction#onSave}), dari impor Excel
	 * per-organisasi (diisi nama sheet), dan dari impor generik {@code Common.uploadData}. Tidak
	 * ada validasi keunikan di sini; validasi itu ada di lapis Action dan di batasan kolom
	 * database.</p>
	 *
	 * @param nama nama organisasi; secara skema wajib non-{@code null} saat disimpan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tentang organisasi &mdash; kolom {@code keterangan}, opsional.
	 *
	 * <p><b>Pola "getKeterangan() membalik kontrak" TIDAK berlaku di sini</b>: method ini benar-
	 * benar mengembalikan field {@code keterangan} apa adanya, tanpa {@code trim()}, tanpa
	 * fallback ke nama/kode, dan tanpa tulis balik.</p>
	 *
	 * <p>Dirender sebagai satu kolom penuh pada grid layar master dan ikut pada ekspor/impor
	 * generik. Pada impor Excel per-organisasi, organisasi yang tercipta otomatis mendapat
	 * keterangan = nama sheet (yaitu sama dengan namanya).</p>
	 *
	 * @return keterangan organisasi, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan organisasi. Disimpan apa adanya, boleh {@code null}.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode organisasi 5 digit berpadding nol &mdash; <b>getter dengan efek samping</b>.
	 *
	 * <p><b>Logika:</b> bila baris sudah punya {@link #getId() id} <i>dan</i> field {@code kode}
	 * masih {@code null}/kosong, kode dibangkitkan dari id dengan cara menempelkan sepuluh angka
	 * nol di depan id lalu mengambil <b>5 karakter terakhir</b> &mdash; id {@code 7} menjadi
	 * {@code "00007"}. Hasilnya <b>ditulis balik ke field {@code kode}</b>, lalu dikembalikan.</p>
	 *
	 * <p><b>Efek samping yang penting dipahami.</b> Getter ini tidak beranotasi {@code @Column},
	 * tetapi karena keluarga entity ini memakai <i>property access</i>, Hibernate tetap
	 * memetakannya secara implisit ke kolom {@code kode}. Artinya nilai hasil pembangkitan itu
	 * ikut terbaca saat pemeriksaan <i>dirty</i> dan <b>dituliskan ke database pada flush
	 * berikutnya</b>. Cukup dengan me-render grid layar master (renderer memanggil
	 * {@code getKode()} untuk setiap baris) atau menekan "Download Data Siswa" (kode dipakai
	 * sebagai nama sheet), sejumlah baris bisa ter-{@code UPDATE} beserta satu revisi Envers
	 * baru masing-masing &mdash; walaupun pengguna tidak menyunting apa pun.</p>
	 *
	 * <p><b>Kuirk terkait:</b> {@code kode} <b>tidak</b> termasuk daftar kolom ekspor/impor
	 * generik ({@code id, nama, namaEn, yayasan, sekolah, keterangan}) dan tidak ada satu pun
	 * kotak isian di form tambah/ubah untuk mengisinya &mdash; padahal justru kode inilah kunci
	 * pencocokan sheet pada {@code OrganisasiSiswaAction#onUploadData}. Praktisnya kode selalu
	 * merupakan turunan id, sehingga berkas impor per-organisasi harus dibuat dengan nama sheet
	 * berupa nomor 5 digit, bukan nama organisasi yang mudah dibaca. Kolom "Kode" pada grid dan
	 * kotak pencarian {@code searchkode} bekerja atas nilai turunan ini.</p>
	 *
	 * @return kode organisasi (5 digit berpadding nol), atau {@code null} bila baris belum punya
	 *         id dan kode belum pernah diisi
	 */
	public String getKode() {
		if (id != null && (kode == null || kode.trim().isEmpty())) {
			String k = "0000000000" + id;
			kode = k.substring(k.length() - 5);
		}
		return kode;
	}

	/**
	 * Menetapkan kode organisasi secara eksplisit. Praktis hanya dipakai Hibernate saat memuat
	 * baris; tidak ada layar yang memanggilnya (lihat kuirk pada {@link #getKode()}).
	 *
	 * <p>Mengisi nilai non-kosong di sini mematikan pembangkitan otomatis pada
	 * {@link #getKode()}.</p>
	 *
	 * @param kode kode organisasi; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Yayasan yang menaungi organisasi ini &mdash; relasi {@code @ManyToOne} lazy ke kolom FK
	 * {@code yayasan}, <b>opsional</b>.
	 *
	 * <p>{@code null} berarti <b>"Semua"</b>: organisasi berlaku lintas yayasan. Renderer grid
	 * menampilkan teks literal "Semua" untuk nilai {@code null}, dan pemilih organisasi pada
	 * pendaftaran mandiri siswa memakai pola {@code isNull(yayasan) OR eq(yayasan, pilihan)}
	 * sehingga baris "Semua" selalu ikut muncul.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} <b>ditulis balik</b> ke field
	 * {@code yayasan}. Method {@link GeneralValueObject#check(Object)} me-resolusi proxy lazy
	 * dengan mencoba beberapa sumber berurutan ({@code EntityIdentityMap}, cache in-memory,
	 * session aktif) dan mengembalikan argumennya apa adanya bila semuanya gagal &mdash; jadi
	 * getter ini tidak pernah mengubah <i>identitas</i> baris yang dirujuk, hanya menukar
	 * referensi proxy dengan objek yang sudah terinisialisasi. Karena {@link Yayasan} tidak
	 * di-<i>preload</i> ke cache bersama entity ini, pemanggilan getter ini lebih sering
	 * benar-benar menyentuh session/database.</p>
	 *
	 * <p><b>Cascade:</b> {@code PERSIST} dan {@code MERGE} &mdash; menyimpan organisasi ikut
	 * menyimpan objek {@link Yayasan} yang belum terkelola. {@code REMOVE} sengaja tidak ada.</p>
	 *
	 * @return yayasan penaung, atau {@code null} yang bermakna "Semua yayasan"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan yayasan penaung, dengan <b>normalisasi objek "kosong" menjadi {@code null}</b>.
	 *
	 * <p><b>Perilaku non-trivial:</b> bila argumennya {@code null} <i>atau</i> objek
	 * {@link Yayasan} yang {@code getId()}-nya masih {@code null} (mis. instance baru hasil
	 * {@code new Yayasan()} atau item combobox pembatas "Semua"), field disetel ke {@code null}.
	 * Ini mencegah Hibernate mencoba meng-{@code INSERT} yayasan kosong lewat
	 * {@code CascadeType.PERSIST}. Konsekuensinya <b>menetapkan yayasan yang belum tersimpan
	 * tidak akan pernah berhasil</b> &mdash; simpan yayasannya lebih dulu.</p>
	 *
	 * <p>Dipanggil dari {@code OrganisasiSiswaAction#onSave} (nilai combobox) dan dari
	 * {@code OrganisasiSiswaAction#init} yang mengisi otomatis yayasan pengguna saat form tambah
	 * dibuka.</p>
	 *
	 * @param yayasan yayasan penaung; {@code null} atau objek tanpa id diperlakukan sebagai
	 *                "Semua"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Sekolah pemilik organisasi ini &mdash; relasi {@code @ManyToOne} lazy ke kolom FK
	 * {@code sekolah}, <b>opsional</b>.
	 *
	 * <p>{@code null} berarti <b>"Semua"</b>, sama seperti {@link #getYayasan()}; renderer grid
	 * pun menampilkan "Semua" untuk nilai {@code null}. Perhatikan bahwa label form dan judul
	 * kolom grid untuk relasi ini masih tertulis <b>"Prodi"</b> &mdash; peninggalan salin-tempel
	 * dari versi perguruan tinggi.</p>
	 *
	 * <p><b>Efek samping {@code check(...)} sama persis dengan {@link #getYayasan()}</b>: hasilnya
	 * ditulis balik ke field, tanpa mengubah identitas baris yang dirujuk. Kedua relasi kelas ini
	 * konsisten memakai {@code check(...)} (tidak ada asimetri).</p>
	 *
	 * <p><b>Konsekuensi cakupan yang perlu diwaspadai.</b> Organisasi yang dibuat otomatis oleh
	 * {@code OrganisasiSiswaAction#onUploadData} lahir dengan {@code sekolah} dan {@code yayasan}
	 * = {@code null}, yakni <b>terlihat oleh seluruh tenant</b>. Sebaliknya, dasbor
	 * {@code DasboardSiswa#countGeneric} menyaring {@code eq("sekolah", currentSekolah)} sehingga
	 * baris berlingkup "Semua" justru <b>tidak pernah ikut terhitung</b> pada kartu jumlah
	 * organisasi milik sebuah sekolah &mdash; dua perilaku yang saling bertolak belakang atas
	 * nilai {@code null} yang sama.</p>
	 *
	 * <p><b>Cascade:</b> {@code PERSIST} dan {@code MERGE}; {@code REMOVE} sengaja tidak ada.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} yang bermakna "Semua sekolah"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik, dengan <b>normalisasi objek "kosong" menjadi {@code null}</b>
	 * &mdash; aturan, alasan, dan konsekuensinya sama persis dengan
	 * {@link #setYayasan(Yayasan)}.
	 *
	 * <p>Dipanggil dari {@code OrganisasiSiswaAction#onSave}. Pada form tambah/ubah, combobox
	 * sekolah dikunci ({@code setDisabled(true)}) bila pengguna terikat pada satu sekolah
	 * tertentu &mdash; pengunciannya bersifat kosmetik di sisi UI, bukan validasi sisi server
	 * (lihat "Catatan keamanan" pada Javadoc kelas).</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id diperlakukan sebagai
	 *                "Semua"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	

	/**
	 * Nama organisasi dalam bahasa Inggris &mdash; kolom {@code namaen}, opsional.
	 *
	 * <p>Berbeda dari {@link #getNama()}, properti ini <b>tidak</b> unik, <b>tidak</b> wajib isi,
	 * dan <b>tidak</b> di-{@code trim()}. Dipakai pada tampilan dwibahasa: renderer grid
	 * menempatkannya sebagai baris kedua di bawah nama Indonesia, dan nilainya ikut pada
	 * ekspor/impor generik. Tidak pernah dipakai sebagai kunci pencarian.</p>
	 *
	 * @return nama organisasi dalam bahasa Inggris, atau {@code null} bila tidak diisi
	 */
	@Column(name = "namaen")
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menetapkan nama organisasi dalam bahasa Inggris. Disimpan apa adanya, boleh {@code null}.
	 *
	 * @param namaEn nama dalam bahasa Inggris; boleh {@code null}
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn; 
	}
}
