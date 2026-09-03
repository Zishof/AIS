package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Satu <b>baris keanggotaan</b> seorang {@link Siswa} pada satu {@link OrganisasiSiswa} (OSIS,
 * Pramuka, ekstrakurikuler, dan sejenisnya). Kelas ini adalah entity penghubung (join entity)
 * many-to-many bermuatan: selain menautkan siswa ke organisasi, baris ini juga menyimpan
 * <i>jabatan</i> yang diemban, <i>periode</i> keanggotaan, keterangan bebas, identitas pengaju,
 * dan bendera <i>persetujuan</i> operator.
 *
 * <p>Tabel fisiknya {@code sekolah.organisasi_siswa_punya_siswa}. Lampiran Surat Keputusan (SK) /
 * Surat Keterangan tidak disimpan sebagai kolom di sini melainkan lewat
 * {@code ais.database.model.LampiranLain} yang menyimpan pasangan
 * ({@code id} baris ini, nama kelas ini) &mdash; lihat
 * {@code LampiranLain.ambil(id, OrganisasiSiswaPunyaSiswa.class.getName())} pada jalur ekspor
 * Excel {@code OrganisasiSiswaAction}.</p>
 *
 * <h2>Posisi dalam model domain</h2>
 * <ul>
 *   <li>{@link OrganisasiSiswa} &mdash; sisi organisasi. Wadah/katalog organisasi (punya
 *       {@code kode}, {@code nama}, {@code namaEn}, {@code yayasan}, {@code sekolah},
 *       {@code keterangan}). Kolom {@code organisasi_siswa} di sini {@code nullable = false}.</li>
 *   <li>{@link Siswa} &mdash; sisi siswa. Kolom {@code siswa} juga {@code nullable = false},
 *       dipetakan {@link FetchType#LAZY}.</li>
 *   <li>{@link JabatanOrganisasiSiswa} &mdash; katalog jabatan kepengurusan (Ketua/Sekretaris/
 *       Anggota dan seterusnya; teks bebas tanpa seed bawaan). Relasinya <b>opsional</b>
 *       ({@code nullable = true}) dan dideklarasikan <b>di sisi ini</b>, bukan di sisi
 *       {@link OrganisasiSiswa}. Perlu ditekankan karena mudah keliru: jabatan melekat pada
 *       <i>keanggotaan</i>, bukan pada organisasi.</li>
 *   <li>{@link Tbmuser} &mdash; akun yang mengajukan/mendaftarkan baris. Perhatikan perilaku
 *       {@link #getTbmuser()} di bawah; nilainya <b>tidak</b> dapat dipercaya sebagai jejak
 *       pengaju.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ul>
 *   <li><b>Identitas &amp; jejak audit ringan</b>: {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #getDiubahDari()},
 *       {@link #onUpdate()}.</li>
 *   <li><b>Relasi</b>: {@link #getOrganisasiSiswa()}, {@link #getSiswa()},
 *       {@link #getJabatanOrganisasiSiswa()}, {@link #getTbmuser()}.</li>
 *   <li><b>Muatan keanggotaan</b>: {@link #getMulai()}, {@link #getSampai()},
 *       {@link #getKeterangan()}, {@link #getTahun()}.</li>
 *   <li><b>Alur bisnis</b>: {@link #getPersetujuan()} &mdash; satu-satunya gerbang bisnis nyata
 *       pada entity ini.</li>
 *   <li><b>Penyajian</b>: {@link #toString()}.</li>
 * </ul>
 *
 * <h2>Catatan {@code GeneralValueObject} (penting, bukan bug)</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan satu pun properti yang
 * dideklarasikan di sana. Karena itu {@link #id}, {@link #oleh}, {@link #olehId}, dan
 * {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di kelas ini agar ikut tersimpan;
 * pengulangan itu <b>keharusan teknis, bukan duplikasi yang perlu "dirapikan"</b>. Menghapusnya
 * akan menghilangkan kolom-kolom tersebut dari pemetaan.</p>
 * <p>Berbeda dari banyak entity saudaranya, kelas ini <b>juga</b> mendeklarasikan ulang
 * {@link #diubahDari} sehingga properti itu ikut dipetakan dan benar-benar tersimpan &mdash;
 * diisi {@code OrganisasiSiswaAction} (impor Excel) dan kedua helper "Ambil Data" dengan nama kelas
 * layar sumber. Kontras dengan {@link OrganisasiSiswa} yang <i>tidak</i> mendeklarasikannya dan
 * karena itu selalu kehilangan nilai {@code diubahDari} setelah dimuat ulang.</p>
 *
 * <h2>Catatan Envers &amp; dynamic insert/update</h2>
 * <p>Kelas ditandai {@link Audited}, jadi setiap perubahan baris tersalin ke skema revisi
 * ({@code new_audit}) dan dapat ditelusuri lewat
 * {@code RevisiHelper.createNewRevisi(OrganisasiSiswaPunyaSiswa.class, ...)} yang dipasang pada
 * kolom NIM/nama organisasi di kedua panel ZK. Perlu diingat bahwa Envers hanya menangkap
 * perubahan yang melewati session Hibernate: tombol "Bersihkan" pada
 * {@code OrganisasiSiswaPunyaSiswaHelper} memakai {@code createSQLQuery(...)} DELETE massal
 * sehingga <b>tidak</b> terekam Envers (lihat catatan keamanan di bawah &mdash; tombol itu memang
 * sedang gagal-tertutup karena menyebut nama tabel versi PT).</p>
 * <p>{@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menuliskan kolom yang
 * benar-benar berubah &mdash; relevan untuk memahami efek getter yang menulis balik
 * ({@link #getTahun()}) dan getter yang mengembalikan {@code null} ({@link #getTbmuser()}).</p>
 *
 * <h2>Layar dan jalur tulis yang menyentuh baris ini (hasil penelusuran kode)</h2>
 * <ol>
 *   <li>{@code ais.action.master.sekolah.helper.OrganisasiSiswaPunyaSiswaHelper} &mdash; panel
 *       "daftar pengurus/anggota" dari <b>sisi organisasi</b>, dibuka lewat baris grid pada layar
 *       master {@code OrganisasiSiswaAction}. Di sini {@code mulai}/{@code sampai}/{@code keterangan}
 *       dan kombo jabatan tersimpan <b>langsung pada event {@code onChange}</b> lewat
 *       {@code Common.refreshUpdate} (tanpa tombol Simpan), dan checkbox "Setujui" menulis
 *       {@link #setPersetujuan(Boolean)} lalu {@code Common.refreshSaveOrUpdate}.</li>
 *   <li>{@code ais.action.master.sekolah.helper.SiswaPunyaOrganisasiSiswaHelper} &mdash; panel
 *       dari <b>sisi biodata siswa</b> (tab "Organisasi" pada dasbor kesiswaan). Ini jalur tempat
 *       siswa menyunting keanggotaannya sendiri.</li>
 *   <li>{@code ais.action.master.sekolah.helper.AmbilDataSiswaForOrganisasiSiswaHelper} dan
 *       {@code AmbilDataOrganisasiForOrganisasiSiswaHelper} &mdash; dua pemetik massal (dari sisi
 *       organisasi dan dari sisi siswa). Keduanya memakai pola cari-dulu-baru-buat
 *       ({@code Restrictions.eq("siswa", ...)} + {@code Restrictions.eq("organisasiSiswa", ...)})
 *       sehingga pasangan siswa+organisasi tidak terduplikasi, lalu mengisi {@link #setOleh(String)},
 *       {@link #setTbmuser(Tbmuser)}, dan {@link #setDiubahDari(String)}.</li>
 *   <li>{@code ais.action.master.sekolah.OrganisasiSiswaAction} &mdash; impor/ekspor Excel massal
 *       (kolom: NIM, Mulai, Sampai, Jabatan, Keterangan, SK, Persetujuan). Jalur ini
 *       satu-satunya yang menulis {@link #setPersetujuan(Boolean)} secara massal, langsung dari
 *       sel spreadsheet.</li>
 *   <li>{@code webapp/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_siswa.jsp} &mdash;
 *       layar UI baru (non-ZK). Membaca dan menulis baris ini lewat endpoint reflektif generik
 *       {@code /Data} ({@code action=daftar}/{@code simpanDataRinci}/{@code hapusDataRinci}).
 *       Lihat catatan keamanan &amp; bug di bawah: jalur ini <b>tidak</b> melewati satu pun
 *       gerbang hak akses milik entity/layar ZK.</li>
 *   <li>{@code ais.action.master.dashboard.admin.DasboardSiswa} &mdash; agregasi baca-saja
 *       ("Total Anggota Organisasi", "Anggota Disetujui", "Top Organisasi", daftar rinci
 *       ORGANISASI). Memfilter periode lewat kolom {@link #tahun} &mdash; lihat
 *       {@link #getTahun()}.</li>
 * </ol>
 *
 * <h2>Alur persetujuan &mdash; hasil verifikasi</h2>
 * <p>{@link #getPersetujuan()} adalah gerbang bisnis: selama {@code false}, baris dianggap
 * <i>pengajuan</i> dan masih dapat disunting/dihapus; begitu {@code true}, seluruh isian
 * (tanggal, keterangan, kombo jabatan) langsung ter-{@code disable} dan tombol Hapus milik siswa
 * disembunyikan. Yang perlu dicatat:</p>
 * <ul>
 *   <li><b>Siswa memang boleh mendeklarasikan jabatannya sendiri.</b> Pada
 *       {@code SiswaPunyaOrganisasiSiswaHelper}, kombo jabatan diisi
 *       {@code Common.insertCombo(combobox, "nama", JabatanOrganisasiSiswa.class)} &mdash; seluruh
 *       isi katalog jabatan, <b>tanpa filter apa pun</b>, termasuk "Ketua". Tidak ada validasi
 *       keunikan (dua siswa bisa sama-sama memilih "Ketua" pada organisasi yang sama) dan tidak ada
 *       pembatasan berdasarkan sekolah/yayasan siswa. Yang menahan penyalahgunaan <b>bukan</b>
 *       validasi data, melainkan semata-mata bendera {@link #persetujuan}: nilai yang dipilih
 *       siswa langsung tersimpan ke database (event {@code onChange} &rarr;
 *       {@code Common.refreshUpdate}) dan sudah tampil sebagai "Ketua" di seluruh layar/laporan
 *       yang tidak menyaring {@code persetujuan}, termasuk panel daftar pengurus dari sisi
 *       organisasi. Hanya kartu ringkasan "Anggota Disetujui" pada {@code DasboardSiswa} yang
 *       benar-benar menyaring dengan {@code persetujuan = TRUE}.</li>
 *   <li><b>Gerbang kepemilikan pada sisi siswa BENAR</b> (contoh positif):
 *       {@code bolehEdit = tbmuser != null && tbmuser.getSiswa() != null &&
 *       tbmuser.getSiswa().getId().equals(baris.getSiswa().getId()) && !baris.getPersetujuan()}
 *       &mdash; harus akun siswa, harus siswa <i>pemilik baris</i>, dan baris belum disetujui.
 *       Berbeda dari bug salin-tempel {@code getMahasiswa()} yang ditemukan pada beberapa layar
 *       sekolah lain.</li>
 *   <li><b>Namun cabang "Setujui" pada helper sisi siswa adalah kode mati.</b> Checkbox "Setujui"
 *       di {@code SiswaPunyaOrganisasiSiswaHelper} berada di dalam blok {@code if (bolehEdit)} dan
 *       dijaga syarat {@code if (tbmuser.getSiswa() == null)}. Padahal {@code bolehEdit} sendiri
 *       sudah mensyaratkan {@code tbmuser.getSiswa() != null}; kedua syarat itu saling meniadakan,
 *       sehingga checkbox tersebut <b>tidak pernah dirender</b>. Operator/guru yang membuka tab
 *       organisasi seorang siswa lewat {@code DashboardKegiatanKesiswaan} mendapat
 *       {@code bolehEdit == false} sehingga jatuh ke cabang baca-saja. Konsekuensinya: dari layar
 *       biodata siswa, persetujuan <b>tidak dapat diberikan sama sekali</b> &mdash; satu-satunya
 *       jalan adalah panel dari sisi organisasi atau impor Excel. Beberapa baris di sekitarnya
 *       juga tidak pernah berpengaruh ({@code toolbar.setVisible(!getPersetujuan())} dan
 *       {@code setDisabled(getPersetujuan())} di dalam blok yang sudah memastikan
 *       {@code !getPersetujuan()}).</li>
 *   <li><b>Pemeriksaan {@code getPersetujuan() == null} pada label "Ya"/"Belum" juga kode mati</b>,
 *       karena {@link #getPersetujuan()} sudah meng-<i>coalesce</i> {@code null} menjadi
 *       {@code false}. Tidak berbahaya, tetapi menyesatkan pembaca: seandainya getter itu suatu
 *       saat diubah agar mengembalikan {@code null}, label akan menampilkan "Ya" untuk baris yang
 *       justru belum pernah disetujui.</li>
 * </ul>
 *
 * <h2>Catatan keamanan (hasil audit, TIDAK diperbaiki di sini)</h2>
 *
 * <h3>1. SQL injection {@code OrganisasiSiswaAction#initCriteria} &mdash; tabel INI-lah sasarannya</h3>
 * <p>Filter "Nama Siswa" ({@code searchnamamhs}) dan "NIS Siswa" ({@code searchnim}) pada layar
 * master organisasi disisipkan <b>mentah</b> ke {@code Restrictions.sqlRestriction(...)}. Subquery
 * yang dibangun menyasar persis tabel entity ini:</p>
 * <pre>
 * this_.id in (select sekolah.organisasi_siswa
 *              from sekolah.organisasi_siswa_punya_siswa a
 *              inner join siswa b on (a.siswa = b.id)
 *              where sekolah.organisasi_siswa is not null
 *                and b.nama ilike '%&lt;INPUT&gt;%' and b.nim ilike '%&lt;INPUT&gt;%'
 *              group by sekolah.organisasi_siswa)
 * </pre>
 * <p><b>Verifikasi "bug lain" yang menghalangi eksploitasi &mdash; TERKONFIRMASI dari sisi entity
 * ini.</b> Nama <i>kolom</i> {@code organisasi_siswa} (kolom FK milik tabel entity ini, lihat
 * {@link #getOrganisasiSiswa()}) ikut terkena find/replace penambahan skema sehingga tertulis
 * {@code sekolah.organisasi_siswa} di <b>tiga</b> tempat: daftar {@code SELECT}, klausa
 * {@code WHERE}, dan {@code GROUP BY}. Di dalam subquery itu satu-satunya alias yang ada adalah
 * {@code a} (untuk {@code sekolah.organisasi_siswa_punya_siswa}) dan {@code b} (untuk
 * {@code siswa}); tidak ada tabel/alias bernama {@code sekolah}. PostgreSQL menolak referensi
 * {@code x.y} yang {@code x}-nya bukan range variable dengan galat
 * <i>"missing FROM-clause entry for table sekolah"</i> &mdash; jadi kueri <b>selalu</b> gagal dan
 * filter tidak pernah mengembalikan hasil. Bentuk yang dimaksud penulis jelas
 * {@code a.organisasi_siswa}.</p>
 * <p><b>Mengapa ini benar-benar menutup eksploitasi (bukan sekadar mempersulit):</b> kemunculan
 * {@code sekolah.organisasi_siswa} yang pertama berada di daftar {@code SELECT}, yaitu
 * <b>sebelum</b> titik penyisipan input. Payload apa pun &mdash; termasuk yang menutup tanda kutip
 * lalu mengomentari sisa kueri dengan {@code --} &mdash; hanya dapat menghapus bagian
 * <i>sesudah</i> dirinya, sehingga referensi rusak di {@code SELECT} tetap ada dan kueri tetap
 * ditolak parser sebelum satu baris pun dibaca. Inilah "bom waktu" yang dimaksud catatan audit
 * sebelumnya: <b>begitu seseorang memperbaiki prefiks skema tersebut tanpa sekaligus mem-bind
 * parameter, SQL injection langsung hidup.</b></p>
 * <p><b>Temuan tambahan (baru).</b> Cabang kedua pada method yang sama, filter "Guru", <b>juga</b>
 * rusak tetapi karena sebab yang berbeda: SQL-nya menyebut {@code b.guru} pada tabel
 * {@code siswa}, padahal pada {@link Siswa} kedua properti guru ({@code guruPembina} dan
 * {@code guruBk}) ditandai {@code @Transient} &mdash; keduanya diturunkan dari kelas, bukan kolom.
 * Kolom {@code guru} tidak ada dalam pemetaan, sehingga cabang ini pun selalu gagal.
 * Kegagalannya bersifat <b>senyap</b> untuk pengguna non-admin: hitungan paging dibungkus
 * {@code try/catch} yang hanya memanggil {@code Common.tampilErrorJikaAdmin(e)}.</p>
 * <p>Bug serupa muncul pula pada {@code OrganisasiSiswaPunyaSiswaHelper#initCriteria}, yang
 * menyaring dengan {@code Restrictions.eq("siswa.guruPembina", guru.getId())}. Karena
 * {@code guruPembina} {@code @Transient}, Hibernate melempar
 * {@code could not resolve property: guruPembina} &mdash; filter "Guru" pada panel daftar anggota
 * juga tidak pernah berfungsi.</p>
 *
 * <h3>2. Jalur tulis reflektif generik {@code /Data} tanpa gerbang (UI baru)</h3>
 * <p>{@code _tab_organisasi_siswa.jsp} menambah/menghapus anggota lewat
 * {@code POST /Data} dengan {@code action=simpanDataRinci}/{@code hapusDataRinci} dan
 * {@code class} berisi nama kelas <b>ini</b>. Rantainya bermuara di
 * {@code ElearningApiUtil.prosesSimpan}/{@code prosesHapus}, yang hanya punya gerbang CRUD
 * ter-hardcode untuk <b>dua</b> kelas modul koperasi/aset; untuk kelas lain &mdash; termasuk entity
 * ini &mdash; tidak ada pemeriksaan {@code CommonPrivilages} maupun kepemilikan sama sekali.
 * Ditambah lagi, {@code ais.action.servlet.Data} hanya mewajibkan login secara keras untuk
 * {@code action=update_data}/{@code update_file_data}; untuk aksi lain, penanda
 * {@code tanpaLogin=true} yang <b>dikirim klien</b> melewati pemeriksaan login. Artinya
 * penambahan, pengubahan, dan penghapusan baris keanggotaan pada tabel ini dapat dilakukan tanpa
 * hak akses layar terkait.</p>
 * <p>Pada jalur baca ({@code action=daftar}), parameter {@code whereN} dari klien diteruskan apa
 * adanya ke {@code Restrictions.sqlRestriction(...)} oleh
 * {@code ais.action.servlet.api.DaftarDataService} &mdash; sink SQL injection generik yang, tidak
 * seperti bug skema di atas, <b>tidak</b> terhalang apa pun. JSP ini bahkan merangkai sendiri
 * {@code where1 = "(nis ILIKE '%" + val + "%' OR ...)"} dari ketikan pengguna. Temuan ini bersifat
 * arsitektural (endpoint bersama), bukan khusus entity ini.</p>
 *
 * <h3>3. Tombol tanpa gerbang pada panel sisi organisasi</h3>
 * <p>Pada {@code OrganisasiSiswaPunyaSiswaHelper} hanya tombol Hapus per baris yang dijaga
 * ({@code delete = CommonPrivilages.checkPrevilages(DELETE)}) dan tombol Upload yang dijaga
 * ({@code Common.getApakahAdmin() || getApakahAdminLain()}). <b>Tidak</b> dijaga: penyuntingan
 * inline seluruh kolom (tersimpan pada {@code onChange}), <b>checkbox "Setujui"</b> &mdash;
 * gerbang bisnis yang meresmikan jabatan &mdash; serta tombol "Ambil Siswa" (penambahan massal)
 * dan "Bersihkan" (penghapusan massal). Jadi hak <b>BACA</b> layar sudah cukup untuk meresmikan
 * seorang siswa sebagai, misalnya, Ketua OSIS. "Bersihkan" untungnya masih gagal-tertutup karena
 * SQL-nya menyebut tabel versi PT {@code organisasi_intra_kampus_punya_siswa} yang tidak ada
 * &mdash; risiko nol untuk saat ini, sampai seseorang "memperbaiki" nama tabelnya tanpa menambah
 * gerbang.</p>
 *
 * <h3>4. Cakupan tenant</h3>
 * <p>Entity ini tidak punya kolom {@code sekolah}/{@code yayasan} sendiri; cakupannya diturunkan
 * dari {@link #getSiswa()} dan {@link #getOrganisasiSiswa()}. {@code SiswaPunyaOrganisasiSiswaHelper#initCriteria}
 * tidak menambahkan filter tenant apa pun &mdash; aman selama helper selalu terikat pada satu
 * siswa atau satu organisasi (kondisi yang berlaku pada seluruh pemanggil saat ini), tetapi tidak
 * ada pertahanan berlapis bila kelak ada pemanggil yang membiarkan keduanya {@code null}.</p>
 *
 * <h2>Kuirk &amp; bug non-keamanan yang terverifikasi</h2>
 * <ul>
 *   <li><b>UI baru menulis properti yang tidak ada.</b> Form "Tambah Siswa" pada
 *       {@code _tab_organisasi_siswa.jsp} mengirim {@code {organisasiSiswa, siswa, jabatan,
 *       periode}}. Entity ini tidak punya properti {@code jabatan} maupun {@code periode} (yang
 *       ada {@link #jabatanOrganisasiSiswa}, {@link #mulai}, {@link #sampai}).
 *       {@code ElearningApiUtil.simpanProperty} melakukan iterasi atas
 *       {@code ClassMetadata.getPropertyNames()} &mdash; properti JSON yang tidak dikenal
 *       <b>diabaikan diam-diam, tanpa peringatan</b>. Akibatnya jabatan dan periode yang diketik
 *       operator pada layar baru tidak pernah tersimpan, dan kolom "Jabatan"/"Periode" pada tabel
 *       anggota di layar yang sama selalu menampilkan "-".</li>
 *   <li><b>{@link #getTahun()} adalah getter yang menulis balik.</b> Setiap pembacaan menghitung
 *       ulang {@link #tahun} dari {@link #mulai}; karena Hibernate memakai <i>property access</i>
 *       (anotasi berada pada getter), nilai hasil hitung itulah yang ikut ter-INSERT/UPDATE.
 *       Efek praktis: {@link #setTahun(Integer)} hanya "menempel" bila {@link #mulai} {@code null};
 *       selebihnya nilai manual apa pun akan tertimpa tahun dari tanggal mulai.</li>
 *   <li><b>Baris tanpa tanggal mulai lenyap dari dasbor.</b> {@code DasboardSiswa.applyPeriodFilter}
 *       memilih kolom pertama yang tersedia, dan untuk kelas ini yang terpilih adalah
 *       {@code tahun} ({@code Restrictions.ge/le}). Baris yang dibuat lewat UI baru tidak pernah
 *       mengisi {@link #mulai} (form-nya mengirim {@code periode} yang diabaikan), sehingga
 *       {@link #tahun} tetap {@code null} dan baris tersebut <b>tidak pernah</b> ikut terhitung
 *       pada kartu maupun daftar rinci dasbor kesiswaan.</li>
 *   <li><b>{@link #getTbmuser()} merusak datanya sendiri.</b> Getter ini mengembalikan
 *       {@code null} setiap kali akun pengaju adalah akun siswa. Karena Hibernate membaca nilai
 *       lewat getter, {@code null} itulah yang dibandingkan dengan snapshot dan yang dituliskan
 *       saat flush &mdash; kolom {@code tbmuser} praktis <b>tidak pernah terisi</b> untuk baris
 *       yang diajukan siswa, dan baris lama yang sudah terisi akan <b>dikosongkan permanen</b>
 *       begitu tersentuh sesi yang mem-flush. Jejak "siapa yang mengajukan" karenanya hanya andal
 *       lewat {@link #oleh}/{@link #olehId} (teks) dan riwayat Envers.</li>
 *   <li><b>{@link #toString()} membaca field langsung, bukan getter.</b> {@link #siswa} dipetakan
 *       {@link FetchType#LAZY}, sedangkan {@code toString()} merangkainya tanpa melewati
 *       {@code check(...)}. Pada object yang sudah <i>detached</i> hal ini dapat memicu
 *       {@code LazyInitializationException} &mdash; relevan karena {@code toString()} dipakai pada
 *       label progres ekspor Excel {@code OrganisasiSiswaAction}. Juga: yang dirangkai adalah
 *       {@link OrganisasiSiswa} dan {@link Siswa}, <b>bukan</b> jabatan, sehingga representasi
 *       teksnya tidak membedakan dua baris siswa yang sama pada organisasi yang sama.</li>
 *   <li><b>Tidak ada batasan keunikan.</b> Pasangan (siswa, organisasi) tidak dijamin unik di
 *       tingkat basis data. Keunikan hanya "dijaga" oleh pola cari-dulu-baru-buat pada keempat
 *       jalur ZK/Excel; jalur {@code /Data} tidak melakukannya sama sekali, jadi anggota ganda
 *       dapat tercipta dari layar UI baru.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see OrganisasiSiswa
 * @see JabatanOrganisasiSiswa
 * @see Siswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "organisasi_siswa_punya_siswa")
public class OrganisasiSiswaPunyaSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Dipertahankan apa adanya karena instance entity ini ikut tersimpan
	 * pada atribut komponen/desktop ZK; mengubahnya dapat memutus sesi lama yang sedang berjalan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris keanggotaan. Dideklarasikan ulang di sini karena {@link GeneralValueObject}
	 * bukan {@code @MappedSuperclass}; lihat catatan pada Javadoc kelas.
	 */
	private Long id;
	/**
	 * Jejak teks "diubah oleh" (biasanya {@code Tbmuser.getUserId()}). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} bukan {@code @MappedSuperclass}.
	 */
	private String oleh;
	/**
	 * Jejak teks id pengubah. Dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}.
	 */
	private String olehId;

	/** @return jejak id pengubah terakhir, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel jejak id pengubah. <b>Sengaja mengabaikan</b> nilai {@code null} atau kosong
	 * (early-return) supaya jejak lama tidak terhapus oleh pemanggil yang tidak membawa identitas.
	 *
	 * @param olehId id pengubah; nilai {@code null}/kosong/berisi spasi saja tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel jejak "diubah oleh". Sama seperti {@link #setOlehId(String)}, nilai {@code null}
	 * atau kosong <b>diabaikan</b> agar jejak lama tetap utuh.
	 *
	 * @param oleh identitas pengubah (umumnya {@code Tbmuser.getUserId()}); {@code null}/kosong
	 *             tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return identitas pengubah terakhir, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: mendelegasikan pembaruan stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate
	 * menjalankan UPDATE. Implementasi wajib dari method {@code abstract} milik
	 * {@link GeneralValueObject}.
	 *
	 * <p>Baris fisik yang sama juga mendeklarasikan field {@code tanggal_dirubah}, yang
	 * diinisialisasi ke {@code ais.ui.util.WaktuUtil.getDate()} sehingga baris baru sudah punya
	 * stempel waktu sebelum sempat di-UPDATE. Format satu baris ini peninggalan transformator
	 * massal; jangan dipecah tanpa alasan agar diff tetap bersih.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Biasanya tidak dipanggil kode aplikasi secara
	 * langsung; diisi {@link #onUpdate()} lewat interceptor audit.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu (tanggal+jam) perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: {@code "<organisasi> - <siswa>"}.
	 *
	 * <p>Dipakai pada label progres ekspor Excel {@code OrganisasiSiswaAction}. Dua catatan
	 * penting: (1) method ini membaca <b>field</b> {@link #siswa} secara langsung, bukan
	 * {@link #getSiswa()}, sehingga tidak melewati resolusi proxy {@code check(...)} dan dapat
	 * memicu {@code LazyInitializationException} pada object yang sudah detached; (2) jabatan tidak
	 * ikut dirangkai, jadi teksnya tidak membedakan dua baris keanggotaan siswa yang sama pada
	 * organisasi yang sama.</p>
	 *
	 * @return gabungan organisasi dan siswa, dipisah {@code " - "}
	 */
	public String toString() {
		return organisasiSiswa + " - " + siswa;
	}

	/** Organisasi yang diikuti (sisi "banyak" ke {@link OrganisasiSiswa}); kolom wajib. */
	private OrganisasiSiswa organisasiSiswa;
	/** Siswa anggota (sisi "banyak" ke {@link Siswa}); kolom wajib, dipetakan lazy. */
	private Siswa siswa;
	/**
	 * Nama kelas layar yang terakhir menulis baris ini (mis. {@code "OrganisasiSiswaAction"},
	 * {@code "SiswaAction"}). Berbeda dari kebanyakan entity saudara, properti ini
	 * <b>dideklarasikan ulang</b> di sini sehingga benar-benar dipetakan dan tersimpan.
	 */
	private String diubahDari;

	/**
	 * Akun pengaju keanggotaan. Perhatikan {@link #getTbmuser()} &mdash; nilainya tidak dapat
	 * dipercaya sebagai jejak pengaju untuk akun siswa.
	 */
	private Tbmuser tbmuser;
	/** Jabatan yang diemban pada organisasi tersebut; opsional. */
	private JabatanOrganisasiSiswa jabatanOrganisasiSiswa;
	/** Tanggal mulai menjabat/menjadi anggota; juga sumber perhitungan {@link #tahun}. */
	private Date mulai;
	/** Tanggal akhir keanggotaan; {@code null} berarti masih berjalan. */
	private Date sampai;
	/** Keterangan bebas (kolom {@code text}). */
	private String keterangan;

	/**
	 * Tahun keanggotaan, dipakai sebagai filter periode oleh dasbor kesiswaan. Diturunkan otomatis
	 * dari {@link #mulai} setiap kali {@link #getTahun()} dipanggil.
	 */
	private Integer tahun;

	/**
	 * Bendera persetujuan operator. {@code false}/{@code null} = masih berupa pengajuan dan boleh
	 * disunting siswa pemiliknya; {@code true} = resmi dan dibekukan.
	 */
	private Boolean persetujuan;

	/** Konstruktor tanpa argumen yang dibutuhkan Hibernate dan seluruh jalur pembuatan baris baru. */
	public OrganisasiSiswaPunyaSiswa() {
	}

	/**
	 * @return kunci utama baris keanggotaan. Kolom {@code insertable = false} karena nilainya
	 *         dihasilkan basis data ({@link javax.persistence.GenerationType#IDENTITY}).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipanggil Hibernate saat memuat/menyimpan; kode aplikasi tidak
	 * seharusnya menyetelnya sendiri.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return organisasi yang diikuti. Kolom FK {@code organisasi_siswa} (wajib), diambil dengan
	 *         {@link FetchMode#SELECT} sehingga tidak ikut dalam JOIN kueri induk. Kolom inilah
	 *         yang secara keliru ditulis {@code sekolah.organisasi_siswa} pada subquery
	 *         {@code OrganisasiSiswaAction#initCriteria} &mdash; lihat catatan keamanan pada
	 *         Javadoc kelas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "organisasi_siswa", nullable = false)
	public OrganisasiSiswa getOrganisasiSiswa() {
		return organisasiSiswa;
	}

	/**
	 * Menyetel organisasi yang diikuti. Wajib diisi sebelum simpan (kolom {@code nullable = false}).
	 *
	 * @param organisasiSiswa organisasi tujuan
	 */
	public void setOrganisasiSiswa(OrganisasiSiswa organisasiSiswa) {
		this.organisasiSiswa = organisasiSiswa;
	}

	/**
	 * @return siswa anggota. Relasi ini {@link FetchType#LAZY}, karena itu getter memanggil
	 *         {@code check(...)} milik {@link GeneralValueObject} untuk meresolusi proxy sebelum
	 *         mengembalikannya (cache &rarr; session aktif &rarr; muat ulang lewat session baru).
	 *         Efek samping yang disengaja: field {@link #siswa} ikut ditimpa hasil resolusi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa anggota. Wajib diisi sebelum simpan (kolom {@code nullable = false}).
	 *
	 * @param siswa siswa yang menjadi anggota
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan akun pengaju keanggotaan &mdash; <b>tetapi hanya bila akun itu bukan akun
	 * siswa</b>. Setelah meresolusi proxy lazy lewat {@code check(...)}, getter mengembalikan
	 * {@code null} ketika {@code tbmuser.getSiswa() != null}.
	 *
	 * <p><b>Efek samping yang perlu diwaspadai:</b> Hibernate membaca nilai properti lewat getter
	 * ini (property access). Karena itu {@code null} yang dikembalikan bukan sekadar penyaringan
	 * tampilan &mdash; nilai itulah yang dibandingkan dengan snapshot dan yang dituliskan saat
	 * flush. Konsekuensinya kolom {@code tbmuser} tidak pernah terisi untuk baris yang diajukan
	 * akun siswa, dan baris lama yang sudah terisi akan dikosongkan permanen begitu tersentuh
	 * sesi yang mem-flush. Untuk menelusuri pengaju, andalkan {@link #getOleh()}/
	 * {@link #getOlehId()} atau riwayat Envers, bukan kolom ini.</p>
	 *
	 * @return akun pengaju non-siswa, atau {@code null} bila belum diisi maupun bila pengajunya
	 *         berupa akun siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser != null && tbmuser.getSiswa() != null ? null : tbmuser;
	}

	/**
	 * Menyetel akun pengaju. Diisi oleh {@code AmbilDataSiswaForOrganisasiSiswaHelper},
	 * {@code AmbilDataOrganisasiForOrganisasiSiswaHelper}, dan impor Excel
	 * {@code OrganisasiSiswaAction} dengan akun yang sedang login.
	 *
	 * @param tbmuser akun pengaju
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * @return nama kelas layar yang terakhir menulis baris ini (mis. {@code "OrganisasiSiswaAction"}
	 *         atau {@code "SiswaAction"}), atau {@code null} untuk baris yang dibuat lewat jalur
	 *         yang tidak mengisinya (antara lain endpoint reflektif {@code /Data}).
	 */
	public String getDiubahDari() {
		return diubahDari;
	}

	/**
	 * Menyetel penanda layar asal perubahan.
	 *
	 * @param diubahDari umumnya {@code SomeAction.class.getSimpleName()}
	 */
	public void setDiubahDari(String diubahDari) {
		this.diubahDari = diubahDari;
	}

	/**
	 * @return jabatan yang diemban pada organisasi ini, atau {@code null} bila anggota biasa/belum
	 *         ditentukan. Kolom FK {@code jabatan_organisasi_siswa} bersifat opsional dan diambil
	 *         dengan {@link FetchMode#SELECT}. Nilai dipilih lewat combobox yang diisi seluruh isi
	 *         katalog {@link JabatanOrganisasiSiswa} tanpa filter, baik oleh operator maupun oleh
	 *         siswa pemilik baris &mdash; lihat bagian alur persetujuan pada Javadoc kelas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_organisasi_siswa", nullable = true)
	public JabatanOrganisasiSiswa getJabatanOrganisasiSiswa() {
		return jabatanOrganisasiSiswa;
	}

	/**
	 * Menyetel jabatan yang diemban.
	 *
	 * <p>Dipanggil dari event {@code onChange} combobox pada kedua panel ZK &mdash; perubahan
	 * langsung dilanjutkan ke {@code Common.refreshUpdate} tanpa tombol Simpan &mdash; serta dari
	 * impor Excel {@code OrganisasiSiswaAction}. Pada jalur Excel, sel yang tidak cocok dengan
	 * baris katalog mana pun menghasilkan {@code null} secara diam-diam.</p>
	 *
	 * @param jabatanOrganisasiSiswa jabatan baru, boleh {@code null}
	 */
	public void setJabatanOrganisasiSiswa(JabatanOrganisasiSiswa jabatanOrganisasiSiswa) {
		this.jabatanOrganisasiSiswa = jabatanOrganisasiSiswa;
	}

	/**
	 * @return tanggal mulai keanggotaan/jabatan (presisi tanggal, tanpa jam), atau {@code null}
	 *         bila tidak diisi. Selain ditampilkan, nilai ini menjadi sumber perhitungan
	 *         {@link #getTahun()}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Menyetel tanggal mulai keanggotaan. Mengubahnya secara tidak langsung juga mengubah
	 * {@link #getTahun()} pada pembacaan berikutnya.
	 *
	 * @param mulai tanggal mulai, boleh {@code null}
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * @return tanggal akhir keanggotaan (presisi tanggal), atau {@code null} bila keanggotaan masih
	 *         berjalan. Tidak ada validasi bahwa {@code sampai} berada setelah {@link #getMulai()}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Menyetel tanggal akhir keanggotaan.
	 *
	 * @param sampai tanggal akhir, boleh {@code null} untuk keanggotaan yang masih berjalan
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * @return keterangan bebas mengenai keanggotaan ini, atau {@code null}. Dipetakan sebagai
	 *         kolom {@code text} sehingga tidak dibatasi panjang.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Pada kedua panel ZK, isian ini tersimpan langsung pada event
	 * {@code onChange} textbox.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Bendera persetujuan operator atas pengajuan keanggotaan ini, dengan {@code null} dianggap
	 * <b>belum disetujui</b>.
	 *
	 * <p>Nilai {@code true} membekukan baris: pada kedua panel ZK seluruh isian (tanggal,
	 * keterangan, kombo jabatan) ter-{@code disable} dan toolbar hapus milik siswa disembunyikan.
	 * Nilai {@code false} berarti baris masih berstatus pengajuan dan boleh disunting siswa
	 * pemiliknya sendiri (gerbang {@code bolehEdit} pada
	 * {@code SiswaPunyaOrganisasiSiswaHelper}).</p>
	 *
	 * <p>Karena getter meng-<i>coalesce</i> {@code null} menjadi {@code false} dan Hibernate
	 * memakai property access, baris yang disimpan lewat jalur normal selalu menuliskan
	 * {@code false} alih-alih {@code null}. Pemeriksaan {@code getPersetujuan() == null} yang masih
	 * ada di beberapa renderer karenanya merupakan kode mati.</p>
	 *
	 * @return {@code true} bila keanggotaan sudah disetujui; {@code false} bila belum atau belum
	 *         pernah diisi
	 */
	public Boolean getPersetujuan() {
		return persetujuan == null ? false : persetujuan;
	}

	/**
	 * Menyetel bendera persetujuan.
	 *
	 * <p>Dipanggil dari checkbox "Setujui" pada {@code OrganisasiSiswaPunyaSiswaHelper} (diikuti
	 * {@code Common.refreshSaveOrUpdate}) dan dari kolom "Persetujuan" pada impor Excel
	 * {@code OrganisasiSiswaAction}. Checkbox serupa pada
	 * {@code SiswaPunyaOrganisasiSiswaHelper} berada di cabang yang tidak pernah tercapai, jadi
	 * layar biodata siswa tidak dapat memberi persetujuan &mdash; lihat Javadoc kelas.</p>
	 *
	 * <p><b>Tidak ada pemeriksaan hak akses</b> pada pemanggil checkbox tersebut; ini gerbang
	 * bisnis yang meresmikan jabatan seorang siswa, namun hak BACA layar sudah cukup untuk
	 * mengubahnya.</p>
	 *
	 * @param persetujuan {@code true} untuk menyetujui dan membekukan baris, {@code false} untuk
	 *                    mengembalikannya ke status pengajuan
	 */
	public void setPersetujuan(Boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Mengembalikan tahun keanggotaan, <b>dihitung ulang dari {@link #getMulai()} setiap kali
	 * dipanggil</b> bila tanggal mulai terisi.
	 *
	 * <p><b>Getter yang menulis balik.</b> Method ini bukan pembaca murni: ia menimpa field
	 * {@link #tahun} dengan {@code Calendar.YEAR} dari {@link #mulai} (kalender diambil dari
	 * {@code ais.ui.util.WaktuUtil.getCalendar()} agar zona waktu konsisten). Karena Hibernate
	 * membaca nilai properti lewat getter, hasil perhitungan itulah yang ikut ter-INSERT/UPDATE ke
	 * kolom {@code tahun}. Akibatnya {@link #setTahun(Integer)} hanya berpengaruh selama
	 * {@link #mulai} masih {@code null}.</p>
	 *
	 * <p><b>Mengapa penting:</b> {@code DasboardSiswa.applyPeriodFilter} menyaring periode entity
	 * ini memakai kolom {@code tahun} ({@code >=} dan {@code <=} rentang tahun terpilih). Baris
	 * yang dibuat tanpa tanggal mulai &mdash; termasuk seluruh baris dari form UI baru
	 * {@code _tab_organisasi_siswa.jsp}, yang mengirim properti {@code periode} yang tidak ada
	 * alih-alih {@code mulai} &mdash; akan berkolom {@code tahun} {@code null} dan tidak pernah
	 * ikut terhitung pada dasbor kesiswaan. Filter tahun pada konstruktor
	 * {@code SiswaPunyaOrganisasiSiswaHelper} juga memakai kolom yang sama.</p>
	 *
	 * @return tahun dari tanggal mulai bila tersedia; selain itu nilai yang terakhir disetel
	 *         (dapat {@code null})
	 */
	public Integer getTahun() {
		if (mulai != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(mulai);
			tahun = calendar.get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun keanggotaan secara manual.
	 *
	 * <p>Perhatikan bahwa nilai ini akan <b>tertimpa</b> oleh {@link #getTahun()} pada pembacaan
	 * berikutnya bila {@link #getMulai()} tidak {@code null}. Praktisnya setter ini hanya berguna
	 * untuk baris tanpa tanggal mulai.</p>
	 *
	 * @param tahun tahun keanggotaan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}
}
