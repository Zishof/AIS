package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

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
 * Baris penghubung (<i>link row</i>) antara satu {@link JenisPenilaian} dan satu
 * {@link GrupPenilaian} pada rantai konfigurasi penilaian siswa modul Sekolah. Dipetakan ke tabel
 * <b>{@code sekolah.detail_jenis_penilaian_grup}</b>, di-audit Envers ({@code @Audited}) dan
 * memakai {@code dynamicInsert}/{@code dynamicUpdate}.
 *
 * <h2>Posisi dalam rantai penilaian (terverifikasi dari kode)</h2>
 *
 * <p>Rantai konfigurasi penilaian siswa tersusun dari lima tabel master dan tiga tabel penghubung.
 * Arah relasi di bawah diverifikasi langsung dari nama kolom {@code @JoinColumn} masing-masing
 * entity, bukan diasumsikan:</p>
 *
 * <pre>
 *   JenisPenilaian                        (master; tabel sekolah.jenis_penilaian, punya sekolah_id/yayasan_id)
 *        &darr;  <b>DetailJenisPenilaian &mdash; KELAS INI</b>
 *                                         (kolom: jenis_penilaian_id, grup_penilaian_siswa_id)
 *   GrupPenilaian                         (master; pemilik formula, jenisNilaiHuruf, khususTingkat/Semester,
 *                                          tampilDirekap, nilaiBolehDinputOlehGuru)
 *        &darr;  DetailGrupPenilaian       (kolom: grup_penilaian_id, grup_kategori_item_penilaian_siswa)
 *   GrupKategoriItemPenilaianSiswa        (master; punya kode/nama/formula/khususTingkat)
 *        &darr;  DetailGrupKategoriItemPenilaianSiswa
 *                                         (kolom: grup_kategori_item_penilaian_siswa, kategori_item_penilaian_siswa)
 *   KategoriItemPenilaianSiswa            (master)
 *        &darr;  (FK langsung, bukan tabel penghubung)
 *   JenisItemPenilaianSiswa               (butir nilai terkecil; punya kode yang dipakai formula)
 * </pre>
 *
 * <p>Kelas ini adalah <b>simpul penghubung PERTAMA</b> (tertinggi) rantai tersebut: ia menentukan
 * grup penilaian mana saja yang membentuk sebuah "Jenis Penilaian". Jenis Penilaian sendiri adalah
 * profil yang dipasang pada {@code Matapelajaran} dan/atau {@code KurikulumSekolah}, sehingga
 * mematikan satu baris di sini <b>langsung menghapus satu kolom/grup nilai dari formulir input
 * nilai, rapor, rekap, dan API untuk seluruh mata pelajaran yang memakai jenis penilaian
 * tersebut</b> &mdash; dampaknya lebih luas daripada simpul-simpul di bawahnya.</p>
 *
 * <h2>Bentuk relasi</h2>
 *
 * <ul>
 *   <li>Kedua relasi {@code @ManyToOne} <b>LAZY</b> dan {@code nullable = false} &mdash; sebuah
 *       baris detail selalu memiliki induk jenis penilaian dan target grup penilaian.</li>
 *   <li>{@code cascade = {PERSIST, MERGE}} pada kedua sisi: menyimpan baris detail dapat ikut
 *       menyimpan master di ujungnya. Tidak ada {@code REMOVE}, jadi menghapus baris detail tidak
 *       pernah menghapus master.</li>
 *   <li>Tidak ada koleksi balik ({@code @OneToMany}) di {@link JenisPenilaian} maupun di
 *       {@link GrupPenilaian}. Seluruh navigasi dilakukan lewat {@code Criteria} eksplisit di sisi
 *       pemanggil.</li>
 *   <li>Relasi <b>bukan</b> unik: tidak ada {@code unique constraint} pada pasangan
 *       {@code (jenis_penilaian_id, grup_penilaian_siswa_id)}. Duplikat memang terbentuk secara
 *       normal (lihat bagian "duplikasi baris" di bawah); pembaca menetralkannya dengan
 *       {@code Projections.groupProperty(...)}, dan layar master menetralkannya dengan
 *       {@code containsKey} saat memuat peta pilihan.</li>
 *   <li>Nama kolom FK kedua adalah {@code grup_penilaian_siswa_id} &mdash; perhatikan sisipan
 *       {@code _siswa} yang <b>tidak</b> ada pada nama tabel target ({@code sekolah.grup_penilaian})
 *       maupun pada FK sejenis di {@link DetailGrupPenilaian} ({@code grup_penilaian_id}).
 *       Penamaan warisan; jangan dipakai sebagai petunjuk bahwa kolom ini menunjuk tabel lain.</li>
 *   <li>Entity ini <b>tidak</b> punya kolom tenant ({@code sekolah}/{@code yayasan}) sendiri.
 *       Cakupan tenant diwarisi dari {@link JenisPenilaian#getSekolah()} (kolom {@code sekolah_id}
 *       yang {@code nullable = false}). Perhatikan bahwa {@link GrupPenilaian} <b>juga</b> punya
 *       kolom tenant sendiri, sehingga satu baris penghubung bisa menjembatani dua tenant yang
 *       berbeda; lihat catatan fail-open di bawah.</li>
 *   <li>Dua FK masuk menunjuk tabel ini &mdash; {@code KurikulumPunyaJenisNilai.detail_jenis_penilaian_id}
 *       dan {@code ais.database.model.TugasKelompok} &mdash; tetapi getter/setter keduanya tidak
 *       pernah dipanggil dari mana pun di kode aplikasi. Relasi yatim.</li>
 * </ul>
 *
 * <h2>Siapa yang menulis baris ini</h2>
 *
 * <p>Tidak ada layar master tersendiri untuk entity ini. Satu-satunya penulis adalah
 * {@code ais.action.master.sekolah.JenisPenilaianAction} lewat daftar checkbox <i>"Pilih Grup
 * Penilaian"</i> di jendela Tambah/Ubah Jenis Penilaian. Alur simpannya
 * ({@code JenisPenilaianAction#onSave(Event)}, <b>penting, lihat bagian "bom waktu" di bawah</b>):</p>
 *
 * <ol>
 *   <li>Validasi Nama/Yayasan/Sekolah, simpan/ubah {@link JenisPenilaian} lalu {@code flush()}.</li>
 *   <li>Muat baris {@code DetailJenisPenilaian} milik jenis penilaian itu yang masih
 *       <b>aktif</b> ({@code isNull("aktif") OR eq("aktif", true)}) dan set {@code aktif = false}
 *       satu per satu, masing-masing di-{@code flush()}.</li>
 *   <li>Untuk setiap entri peta {@code selectedJenisItemPenilaianSiswa} (isi peta = pilihan yang
 *       berlaku saat itu), set {@code aktif = true}, set induk jenis penilaian, simpan,
 *       {@code flush()}.</li>
 * </ol>
 *
 * <p>Konsekuensi yang perlu diketahui: (a) baris <b>tidak pernah dihapus fisik</b> &mdash;
 * {@code aktif} adalah <i>soft delete</i>; (b) setiap penyimpanan satu Jenis Penilaian menghasilkan
 * sampai <b>2&times;N revisi Envers</b> pada tabel ini (N = jumlah baris detail aktif) plus
 * penulisan ulang {@code oleh}/{@code tanggal_dirubah}, jadi riwayat audit tabel ini berisik dan
 * tidak bisa dipakai untuk menyimpulkan "kapan pemetaan benar-benar berubah"; (c) karena baris lama
 * dipakai ulang (bukan dibuat baru) untuk grup yang tetap tercentang, {@code id} baris detail stabil
 * lintas penyuntingan.</p>
 *
 * <p><b>Perbedaan halus dari {@link DetailGrupPenilaian}:</b> langkah "matikan semua" di sini
 * disaring {@code aktif} true/NULL, sedangkan pada {@code GrupPenilaianAction} langkah yang sama
 * memuat <b>seluruh</b> baris tanpa filter. Efek akhirnya sama (baris yang sudah mati tetap mati),
 * hanya jumlah revisi Envers yang lebih sedikit di sini.</p>
 *
 * <h2>Duplikasi baris tanpa unique constraint</h2>
 *
 * <p>Peta pilihan hanya diisi dari baris yang <b>aktif</b>. Karena itu, bila sebuah grup penilaian
 * pernah dilepas (baris jadi {@code aktif=false}) lalu dicentang kembali, layar tidak menemukan
 * baris lama di peta dan membuat {@code new DetailJenisPenilaian()}. Hasilnya: dua baris fisik
 * dengan pasangan {@code (jenis_penilaian_id, grup_penilaian_siswa_id)} yang sama &mdash; satu
 * {@code aktif=false} warisan, satu {@code aktif=true} yang baru. Tabel tidak punya unique
 * constraint sehingga basis data menerimanya, dan seluruh pembaca memakai
 * {@code Projections.groupProperty("grupPenilaian.id")} sehingga duplikat tidak terlihat di UI.
 * Yang membengkak hanyalah tabel dan tabel auditnya. Pola identik dengan yang tercatat pada
 * {@link DetailGrupPenilaian} dan {@link DetailGrupKategoriItemPenilaianSiswa}.</p>
 *
 * <h2>Siapa yang membaca baris ini</h2>
 *
 * <p>Dua belas titik baca runtime ditemukan, dan &mdash; berbeda dengan simpul di bawahnya &mdash;
 * <b>semuanya konsisten</b>: {@code Criteria} atas {@code DetailJenisPenilaian}, filter
 * {@code aktif} toleran-NULL ({@code isNull("aktif") OR eq("aktif", true)}), penyaringan
 * {@code eq("jenisPenilaian", ...)}, lalu
 * {@code setProjection(Projections.groupProperty("grupPenilaian.id"))} sehingga yang dikembalikan
 * adalah daftar {@link GrupPenilaian}, bukan baris detailnya:</p>
 *
 * <ul>
 *   <li>{@code ais.action.master.sekolah.PenilaianSiswaAction} &mdash; sinkronisasi/formulir nilai
 *       per kelas.</li>
 *   <li>{@code ais.action.master.sekolah.helper.DetailPenilaianSiswaHelper} dan
 *       {@code DetailPenilaianLesSiswaHelper} &mdash; tab detail nilai (reguler dan les); keduanya
 *       menambah {@code isNotNull("grupPenilaian.id")}.</li>
 *   <li>{@code ais.action.master.sekolah.helper.PertemuanPunyaUjianSiswaHelper} &mdash; nilai ujian
 *       per pertemuan.</li>
 *   <li>{@code ais.action.master.sekolah.helper.TampilStudiSiswaHelper} &mdash; tampilan hasil studi.</li>
 *   <li>{@code ais.action.master.helper.TugasMandiriHelper} dan {@code TugasKelompokHelper} &mdash;
 *       penilaian tugas.</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanRaporSiswa} (dua tempat) &mdash; cetak rapor.</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanRekapTotalNilai} (dua tempat) &mdash; rekap
 *       total nilai; <b>satu-satunya</b> pembaca yang menambah filter
 *       {@code grupPenilaian.tampilDirekap} lewat {@code createAlias("grupPenilaian", ...)}.</li>
 *   <li>{@code ais.action.servlet.api.ElearningApiUtil} (dua tempat) dan
 *       {@code ais.action.servlet.api.NilaiSiswaApi} &mdash; API mobile/e-learning.</li>
 * </ul>
 *
 * <p>Selain itu {@code JenisPenilaianAction} sendiri membaca baris ini di tiga tempat: renderer
 * grid (menampilkan daftar bernomor nama grup penilaian per baris master), listener pengisi peta
 * pilihan, dan langkah "matikan semua" pada {@code onSave}.</p>
 *
 * <p><b>Satu pembaca rusak permanen.</b>
 * {@code ais.common.CommonUiFactoryHelper#getDetailJenisPenilaians(JadwalPelajaran)} (dipanggil
 * ulang oleh {@code ais.common.Common#getDetailJenisPenilaians(JadwalPelajaran)}) menyusun
 * {@code createAlias("jenisItemPenilaianSiswa", "jenisItemPenilaianSiswa")} dan
 * {@code addOrder(Order.desc("jenisItemPenilaianSiswa.nomorUrut"))} padahal kelas ini
 * <b>tidak punya</b> properti {@code jenisItemPenilaianSiswa} sama sekali. Hibernate melempar
 * {@code QueryException} pada setiap pemanggilan, exception ditelan {@code catch} di method itu, dan
 * method <b>selalu</b> mengembalikan list kosong. Untungnya tidak ada satu pun pemanggil di kode
 * aplikasi, jadi ini kode mati &mdash; tetapi merupakan "bom waktu": siapa pun yang memakai helper
 * itu di masa depan akan menerima "tidak ada grup penilaian" tanpa pesan kesalahan apa pun.</p>
 *
 * <h2>Bom waktu {@code aktif} &mdash; hasil verifikasi dari sudut pandang entity ini</h2>
 *
 * <p>Pola yang ditemukan pada {@link DetailGrupPenilaian} dan
 * {@link DetailGrupKategoriItemPenilaianSiswa} (batch 54) <b>diverifikasi ulang di simpul ini</b>.
 * Hasilnya: <b>satu dari tiga varian ADA dan identik, dua varian lain TIDAK ADA di sini</b>.
 * Rinciannya:</p>
 *
 * <ol>
 *   <li><b>Varian waktu/timing &mdash; ADA, persis sama, dan ini yang paling berbahaya.</b> Peta
 *       {@code selectedJenisItemPenilaianSiswa} dibuat <b>kosong</b> di {@code init(...)}, dan baru
 *       diisi ketika listener {@code ubahJenisPenialain} dijalankan. Listener itu dipasang ke
 *       {@code onChange} kombo Yayasan/Sekolah <b>dan</b> dijadwalkan sekali lewat
 *       {@code Common.createDefaultTimer(...)} &rarr; {@code CommonTimerHelper} &rarr; ZK
 *       {@code Timer} sekali jalan dengan interval <b>50&nbsp;ms</b> ditemani overlay
 *       {@code Clients.showBusy(...)}. Bila tombol <i>Simpan</i> sempat ditekan sebelum timer itu
 *       berjalan &mdash; atau listener gagal di tengah jalan, misalnya karena
 *       {@code session.refresh(jenisPenilaian)} melempar &mdash; maka langkah "matikan semua" tetap
 *       berjalan sedangkan langkah "hidupkan yang tercentang" mengiterasi peta kosong (peta tidak
 *       pernah {@code null}, sehingga penjaga {@code != null} tidak menolong).
 *       <b>Seluruh pemetaan Jenis Penilaian &rarr; Grup Penilaian lenyap sekaligus, permanen, dan
 *       tanpa pesan apa pun.</b> Karena entity ini simpul teratas rantai, akibatnya bukan satu
 *       kolom nilai yang hilang melainkan <b>seluruh isi rapor</b> untuk setiap mata pelajaran yang
 *       memakai jenis penilaian tersebut. Overlay busy memperkecil peluang, tetapi tidak
 *       menghilangkannya (overlay dibersihkan lebih dulu oleh {@code safeClearBusy()}, dan jendela
 *       modal tetap menerima klik bila timer gagal terpasang &mdash; jalur {@code runFallback}
 *       dipakai saat tidak ada eksekusi ZK).</li>
 *   <li><b>Varian "master dinonaktifkan" &mdash; TIDAK ADA di sini.</b> Ini perbedaan nyata dari
 *       {@code GrupPenilaianAction}. Query pengisi peta pada {@code JenisPenilaianAction} hanya
 *       menyaring {@code DetailJenisPenilaian.aktif} dan {@code jenisPenilaian}; ia <b>tidak</b>
 *       ikut menyaring {@code grupPenilaian.aktif} (bandingkan: {@code GrupPenilaianAction}
 *       menambahkan {@code isNull/eq} atas {@code grupKategoriItemPenilaianSiswa.aktif} pada query
 *       yang setara). Akibatnya baris yang grup penilaiannya sudah dinonaktifkan <b>tetap masuk ke
 *       peta</b> dan tetap dihidupkan kembali saat simpan &mdash; pemetaannya <b>tidak</b> hilang
 *       diam-diam. Verifikasi ini penting: memperbaiki {@code GrupPenilaianAction} dengan cara
 *       "menyamakan dengan tetangganya" harus menyamakannya ke bentuk <b>ini</b>, bukan
 *       sebaliknya.</li>
 *   <li><b>Varian "grup hantu lintas sekolah" &mdash; ADA sebagian, dengan tanda berlawanan.</b>
 *       Daftar checkbox disaring {@code isNull(sekolah) OR eq(sekolah, s)} (dan serupa untuk
 *       yayasan), sehingga grup penilaian milik sekolah lain tidak pernah muncul sebagai checkbox.
 *       Namun karena peta tetap memuat barisnya (butir 2), baris itu <b>tidak</b> dimatikan
 *       melainkan dipertahankan aktif selamanya. Konsekuensinya bukan kehilangan data, melainkan
 *       kebalikannya: <b>tidak ada layar mana pun yang bisa menampilkan, apalagi melepaskan, baris
 *       semacam itu secara terkendali</b>. Grup penilaian sekolah lain akan terus muncul di rapor
 *       sekolah ini sampai seseorang menyuntingnya langsung di basis data. Bentuk ini sama dengan
 *       "kategori hantu" yang tercatat pada {@link KategoriItemPenilaianSiswa} (batch 51).</li>
 * </ol>
 *
 * <p>Ketiganya adalah bug integritas data, bukan kerentanan akses. Perbaikan yang aman untuk butir
 * 1 tidak boleh sekadar menambah penjaga {@code isEmpty()} pada peta &mdash; peta yang memang
 * sengaja dikosongkan pengguna (semua checkbox dilepas) tidak boleh dibedakan dari peta yang belum
 * sempat diisi timer; yang benar adalah menandai secara eksplisit bahwa listener sudah pernah
 * berjalan, atau membangun daftar checkbox secara sinkron di {@code init(...)}.</p>
 *
 * <h2>Catatan cakupan tenant dan hak akses</h2>
 *
 * <ul>
 *   <li><b>Fail-open cakupan tenant (ringan).</b> Pada listener pengisi daftar checkbox, bila kombo
 *       Sekolah (atau Yayasan) belum terpilih, filter diganti
 *       {@code Restrictions.sqlRestriction("1=1")} &mdash; seluruh Grup Penilaian <b>semua
 *       sekolah</b> ikut terdaftar dan bisa dicentang, sehingga terbentuk pemetaan lintas tenant
 *       yang (lihat butir 3 di atas) tidak pernah bisa dilepas lagi lewat UI. Dampaknya terbatas
 *       pada metadata konfigurasi penilaian (bukan PII), dan {@code onSave} tetap menolak bila
 *       Sekolah/Yayasan kosong, tetapi polanya sama dengan keluarga temuan fail-open yang sudah
 *       tercatat pada audit luas &mdash; memperkuat, bukan temuan baru.</li>
 *   <li><b>Pewarisan hak lewat menu induk &mdash; layar ini adalah SUMBER-nya, bukan korbannya.</b>
 *       {@code /pages/master/sekolah/jenis_penilaian.zul} adalah satu-satunya entri menu nyata
 *       (id&nbsp;881229 pada {@code MenuSnapshotData}, didaftarkan {@code MenuInitializer}) dalam
 *       keluarga ini. {@code JenisPenilaianAction} menyisipkan <b>tujuh</b> layar master lain
 *       sebagai tab lewat {@code MyInclude}, dan <b>tidak satu pun</b> dari ketujuhnya punya entri
 *       menu sendiri: {@code jenis_item_penilaian_siswa.zul}, {@code nilai_huruf_sekolah.zul},
 *       {@code jenis_nilai_huruf.zul}, {@code grup_kategori_item_penilaian_siswa.zul},
 *       {@code grup_penilaian.zul}, {@code kategori_item_penilaian_siswa.zul}, dan
 *       {@code /pages/master/konstanta.zul}. Karena
 *       {@code CommonPrivilages.checkPrevilages(...)} selalu menguji {@code Common.getCurrentMenu()},
 *       hak CREATE/UPDATE/DELETE yang dipakai ketujuh layar itu sesungguhnya adalah hak menu
 *       <i>Jenis Penilaian</i>. Yang perlu digarisbawahi adalah tab terakhir: <b>{@code konstanta.zul}
 *       bukan layar modul Sekolah</b>, melainkan master {@code Konstanta} tingkat aplikasi &mdash;
 *       memberi seseorang hak ubah katalog jenis penilaian sekolah dengan sendirinya memberinya hak
 *       ubah konstanta global instalasi. Ini varian baru (kebocoran menuju layar konfigurasi
 *       sistem) dari pola yang sudah tercatat; memperkuat temuan yang ada, bukan mekanisme
 *       baru.</li>
 *   <li><b>Sisi positif:</b> {@code JenisPenilaianAction} sendiri termasuk layar master yang
 *       digerbangi dengan benar &mdash; {@code doBeforeCompose} memanggil
 *       {@code Common.doCheckSecurity()}, tombol Tambah digerbangi CREATE, checkbox Aktif di grid
 *       di-{@code setDisabled(!edit)}, tombol Ubah/Hapus lewat
 *       {@code Common.copyEditDeleteButtons(edit, delete, ...)}, dan tombol unggah massal menuntut
 *       CREATE&amp;UPDATE&amp;DELETE sekaligus. Tidak ditemukan tombol mutasi massal tanpa gerbang
 *       maupun jalur JSP pra-otentikasi yang menyentuh tabel ini.</li>
 *   <li><b>Tidak ada</b> getter yang menulis balik data bisnis secara destruktif di kelas ini.
 *       Penugasan {@code x = check(x)} pada kedua getter relasi hanyalah resolusi proxy lazy
 *       standar (lihat {@link GeneralValueObject#check(Object)}), bukan mutasi data.</li>
 * </ul>
 *
 * <h2>Kolom/anggota yang praktis mati</h2>
 *
 * <ul>
 *   <li>{@link #getKeterangan()} &mdash; tidak pernah diisi maupun dibaca oleh siapa pun. Kotak
 *       "Keterangan" di layar Jenis Penilaian milik {@link JenisPenilaian#getKeterangan()}, bukan
 *       milik baris detail ini. Kolom yatim sejak awal.</li>
 *   <li>{@link #getNomorUrut()} &mdash; selalu mengembalikan {@code 1}; lihat Javadoc method-nya.</li>
 *   <li>FK masuk {@code KurikulumPunyaJenisNilai.detail_jenis_penilaian_id} dan
 *       {@code TugasKelompok.getDetailJenisPenilaian()} &mdash; keduanya tidak pernah disetel
 *       maupun dibaca dari kode aplikasi.</li>
 * </ul>
 *
 * <h2>Pendaftaran kelas di infrastruktur</h2>
 *
 * <ul>
 *   <li>Terdaftar di {@code hibernate.cfg.xml} sebagai kelas terpetakan.</li>
 *   <li>Terdaftar di {@code ais.common.InitData} sehingga ikut dimuat-awal ke cache
 *       {@code ConstantValues} saat aplikasi start.</li>
 *   <li>Terdaftar di {@code ais.common.DataUtil.CLASS_JANGAN_DIBERSIHKAN} &mdash; baris tabel ini
 *       <b>dikecualikan</b> dari rutin pembersihan data massal, tepat karena kehilangannya akan
 *       merusak seluruh konfigurasi rapor.</li>
 * </ul>
 *
 * <h2>Catatan warisan {@code GeneralValueObject}</h2>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti apa pun miliknya. Karena itu
 * deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini
 * <b>bukan duplikasi keliru melainkan keharusan teknis</b>: tanpa deklarasi ulang, kolom-kolom
 * tersebut tidak akan pernah ada di tabel. Pola ini seragam di seluruh entity AIS.</p>
 *
 * <p>Catatan kecil: {@code serialVersionUID} kelas ini identik dengan milik
 * {@link DetailGrupPenilaian} ({@code -9157912161411433979L}), sebagaimana
 * {@link JenisPenilaian} identik dengan {@link GrupPenilaian}. Itu jejak salin-tempel saat kedua
 * pasangan kelas dibuat; tidak berbahaya karena serialisasi Java memeriksa nama kelas lebih dulu,
 * tetapi jangan dijadikan petunjuk kekerabatan skema.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ol>
 *   <li><b>Jejak audit ringan</b> (deklarasi ulang dari induk): {@link #getOleh()},
 *       {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #setId(Long)}.</li>
 *   <li><b>Relasi rantai penilaian</b>: {@link #getJenisPenilaian()},
 *       {@link #setJenisPenilaian(JenisPenilaian)}, {@link #getGrupPenilaian()},
 *       {@link #setGrupPenilaian(GrupPenilaian)}.</li>
 *   <li><b>Atribut</b>: {@link #getAktif()}, {@link #setAktif(Boolean)}, {@link #getKeterangan()},
 *       {@link #setKeterangan(String)}.</li>
 *   <li><b>Turunan</b>: {@link #getNomorUrut()}.</li>
 * </ol>
 *
 * @see JenisPenilaian
 * @see GrupPenilaian
 * @see DetailGrupPenilaian
 * @see GrupKategoriItemPenilaianSiswa
 * @see DetailGrupKategoriItemPenilaianSiswa
 * @see KategoriItemPenilaianSiswa
 * @see JenisItemPenilaianSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "detail_jenis_penilaian_grup", schema = "sekolah")
public class DetailJenisPenilaian extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap; jangan diubah tanpa alasan kuat karena object entity ikut
	 * diserialisasi ke sesi/desktop ZK dan ke cache {@code ConstantValues}.
	 *
	 * <p>Nilainya kebetulan sama persis dengan milik {@link DetailGrupPenilaian} (jejak
	 * salin-tempel); lihat catatan di Javadoc kelas.</p>
	 */
	private static final long serialVersionUID = -9157912161411433979L;
	/** Kunci utama {@code sekolah.detail_jenis_penilaian_grup.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini (kolom {@code olehId}).
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau string kosong/spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa menyentuh field). Jadi nilai lama tidak pernah bisa
	 * dikosongkan lewat setter ini. Pola ini disengaja agar jejak audit tidak terhapus oleh pemuatan
	 * ulang yang belum mengetahui pengguna aktif.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit tidak pernah terhapus lewat setter.</p>
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini (kolom {@code oleh}).
	 *
	 * @return nama pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum {@code UPDATE}: meneruskan object ini ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(...)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} diperbarui dari sesi pengguna aktif.
	 *
	 * <p>Karena {@code JenisPenilaianAction.onSave(...)} menyimpan ulang seluruh baris detail aktif
	 * milik satu jenis penilaian dua kali (mematikan lalu menghidupkan), kait ini ikut terpanggil
	 * berkali-kali per aksi simpan tunggal &mdash; itulah sebab {@code tanggal_dirubah} pada tabel
	 * ini bergerak walau pemetaannya tidak benar-benar berubah.</p>
	 *
	 * <p>Baris ini sengaja ditulis rapat dengan deklarasi field {@code tanggal_dirubah} karena
	 * mengikuti bentuk seragam seluruh entity AIS; jangan dirapikan tanpa menyapu semua entity.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; biasanya diisi otomatis oleh
	 * {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah},
	 * {@code TIMESTAMP}). Default saat object dibuat adalah waktu server
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), jadi nilainya tidak pernah {@code null} untuk object
	 * baru.
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Induk jenis penilaian (sisi "kiri" penghubung); lihat {@link #getJenisPenilaian()}. */
	private JenisPenilaian jenisPenilaian;
	/** Grup penilaian yang ditunjuk (sisi "kanan"); lihat {@link #getGrupPenilaian()}. */
	private GrupPenilaian grupPenilaian;
	/** Keterangan bebas; kolom yatim, lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda pemetaan hidup/mati (soft delete); lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/JPA. Object hasil konstruktor ini belum punya
	 * induk maupun target, sehingga <b>belum</b> memenuhi {@code nullable = false} pada kedua FK;
	 * pemanggil wajib mengisi {@link #setJenisPenilaian(JenisPenilaian)} dan
	 * {@link #setGrupPenilaian(GrupPenilaian)} sebelum menyimpan.
	 *
	 * <p>Satu-satunya pemakaian di kode aplikasi ada di {@code JenisPenilaianAction}, saat merender
	 * checkbox untuk grup penilaian yang belum pernah dipetakan (atau yang pemetaannya sedang
	 * nonaktif &mdash; lihat bagian "duplikasi baris" pada Javadoc kelas). {@code grupPenilaian}
	 * langsung disetel saat render, sedangkan {@code jenisPenilaian} baru disetel saat simpan.</p>
	 */
	public DetailJenisPenilaian() {
	}

	/**
	 * Mengembalikan kunci utama baris penghubung ini (kolom {@code id}, IDENTITY, tidak ikut
	 * di-{@code INSERT} karena dibangkitkan basis data).
	 *
	 * @return id baris, atau {@code null} untuk object yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Tanpa validasi; normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris penghubung ini (kolom {@code keterangan}).
	 *
	 * <p><b>Kolom yatim:</b> penelusuran seluruh kode tidak menemukan satu pun penulis maupun
	 * pembaca. Layar Jenis Penilaian punya kotak "Keterangan", tetapi itu milik
	 * {@link JenisPenilaian#getKeterangan()}, bukan milik baris detail ini. Nilainya akan selalu
	 * {@code null} kecuali diisi langsung lewat basis data.</p>
	 *
	 * @return keterangan, praktis selalu {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi. Tidak dipanggil dari mana pun di kode aplikasi.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda hidup/mati pemetaan ini, dengan <b>default toleran-NULL</b>:
	 * {@code null} dianggap {@code true}.
	 *
	 * <p>Default itu penting dan konsisten dengan sisi SQL: seluruh pembaca memakai
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, sehingga baris warisan yang kolom
	 * {@code aktif}-nya masih NULL tetap terpakai. Berbeda dengan beberapa entity master lain di
	 * modul ini, di sini <b>tidak ada</b> divergensi antara getter dan SQL &mdash; kolomnya juga
	 * benar-benar ditulis oleh layar master, bukan hanya dibaca.</p>
	 *
	 * <p><b>Peringatan integritas data:</b> kolom inilah yang dimatikan-lalu-dihidupkan pada setiap
	 * penyimpanan {@link JenisPenilaian}. Bila peta pilihan layar belum sempat terisi (timer 50&nbsp;ms
	 * belum berjalan), langkah "hidupkan kembali" tidak menemukan apa pun dan seluruh pemetaan jenis
	 * penilaian itu mati sekaligus. Rinciannya ada di Javadoc kelas, bagian "Bom waktu
	 * {@code aktif}".</p>
	 *
	 * @return {@code true} bila pemetaan aktif (termasuk saat kolom masih NULL), {@code false} bila
	 *         pemetaan sudah dimatikan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda hidup/mati pemetaan. Tanpa validasi.
	 *
	 * <p>Dipanggil dari {@code JenisPenilaianAction.onSave(...)} pada dua tahap berurutan:
	 * {@code false} untuk semua baris aktif milik jenis penilaian yang sedang disimpan, lalu
	 * {@code true} untuk baris yang ada di peta pilihan. Tidak ada pemanggil lain di seluruh kode
	 * aplikasi.</p>
	 *
	 * @param aktif nilai baru; {@code null} akan dibaca kembali sebagai {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan {@link JenisPenilaian} induk pemetaan ini (kolom {@code jenis_penilaian_id},
	 * wajib terisi), setelah proxy lazy diselesaikan lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Hasil {@code check(...)} ditugaskan kembali ke field &mdash; itu resolusi proxy standar agar
	 * object tetap terpakai walau sesi Hibernate yang memuatnya sudah tertutup, bukan mutasi data
	 * bisnis.</p>
	 *
	 * <p>Relasi ini adalah penentu tenant utama baris ini: sekolah/yayasan pemilik pemetaan dibaca
	 * dari {@link JenisPenilaian#getSekolah()}/{@link JenisPenilaian#getYayasan()}. Perhatikan bahwa
	 * {@link GrupPenilaian} di sisi seberang punya kolom tenant sendiri yang bisa berbeda; kombinasi
	 * itu tidak pernah divalidasi (lihat catatan fail-open pada Javadoc kelas).</p>
	 *
	 * <p>Seluruh pembaca runtime menyaring baris lewat properti ini
	 * ({@code Restrictions.eq("jenisPenilaian", ...)}), jadi inilah kunci akses utama tabel.</p>
	 *
	 * @return jenis penilaian induk; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penilaian_id", nullable = false)
	public JenisPenilaian getJenisPenilaian() {
		jenisPenilaian = check(jenisPenilaian);
		return jenisPenilaian;
	}

	/**
	 * Menyetel jenis penilaian induk. Tanpa validasi &mdash; {@code null} maupun object tanpa id
	 * <b>tidak</b> ditolak, sehingga kesalahan baru terdeteksi saat {@code INSERT}/{@code UPDATE}
	 * melanggar {@code NOT NULL}.
	 *
	 * <p>Dipanggil dari {@code JenisPenilaianAction.onSave(...)} pada tahap menghidupkan kembali
	 * baris terpilih. Untuk baris baru inilah satu-satunya saat induk diisi (saat render checkbox
	 * hanya {@link #setGrupPenilaian(GrupPenilaian)} yang dipanggil), jadi memindahkan pemanggilan
	 * ini akan langsung menghasilkan pelanggaran {@code NOT NULL}.</p>
	 *
	 * @param jenisPenilaian jenis penilaian induk yang baru
	 */
	public void setJenisPenilaian(JenisPenilaian jenisPenilaian) {
		this.jenisPenilaian = jenisPenilaian;
	}

	/**
	 * Mengembalikan {@link GrupPenilaian} yang dipetakan oleh baris ini (kolom
	 * {@code grup_penilaian_siswa_id}, wajib terisi), setelah proxy lazy diselesaikan lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Inilah nilai yang sesungguhnya dipanen seluruh pembaca runtime &mdash; mereka
	 * memproyeksikan {@code grupPenilaian.id} lalu memuat masternya, sehingga baris detail sendiri
	 * hampir tidak pernah dihidrasi sebagai object. Dua pengecualian: renderer grid
	 * {@code JenisPenilaianAction.JenisPenilaianRenderer} (menampilkan daftar bernomor
	 * {@code getGrupPenilaian().getNama()}) dan listener pengisi peta pilihan.</p>
	 *
	 * <p>Perhatikan bahwa <b>tidak ada</b> pembaca runtime yang memeriksa
	 * {@code getGrupPenilaian().getAktif()}; status aktif grup penilaian hanya berpengaruh pada
	 * daftar checkbox layar master. Berbeda dengan {@link DetailGrupPenilaian}, asimetri itu di sini
	 * <b>tidak</b> menjadi bom waktu karena query pengisi peta pilihan juga tidak menyaringnya
	 * &mdash; lihat butir 2 pada bagian "Bom waktu {@code aktif}" di Javadoc kelas.</p>
	 *
	 * @return grup penilaian yang dipetakan; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_penilaian_siswa_id", nullable = false)
	public GrupPenilaian getGrupPenilaian() {
		grupPenilaian = check(grupPenilaian);
		return grupPenilaian;
	}

	/**
	 * Menyetel grup penilaian yang dipetakan. Tanpa validasi.
	 *
	 * <p>Dipanggil {@code JenisPenilaianAction} saat merender setiap checkbox &mdash; termasuk untuk
	 * baris yang sudah ada di basis data (nilainya disetel ulang ke object yang sama, karena kunci
	 * peta pilihan justru id grup penilaian itu), jadi pemanggilan itu tidak mengotori data.</p>
	 *
	 * @param grupPenilaian grup penilaian target yang baru
	 */
	public void setGrupPenilaian(GrupPenilaian grupPenilaian) {
		this.grupPenilaian = grupPenilaian;
	}

	/**
	 * Mengembalikan nomor urut tampil baris ini dengan cara <b>meneruskan</b> nomor urut milik grup
	 * penilaian yang ditunjuk, menimpa implementasi {@link GeneralValueObject#getNomorUrut()}.
	 *
	 * <p><b>Praktis selalu mengembalikan {@code 1}.</b> Alasannya: {@link GrupPenilaian}
	 * <b>tidak</b> mendeklarasikan properti {@code nomorUrut} sendiri; ia hanya mewarisi field POJO
	 * dari {@link GeneralValueObject}, yang bukan {@code @Entity}/{@code @MappedSuperclass} sehingga
	 * field itu tidak pernah dipetakan ke kolom mana pun dan selalu bernilai {@code null} untuk
	 * object hasil muat dari basis data. Nilai {@code null} itu lalu diubah menjadi {@code 1} oleh
	 * baris terakhir method ini. Nilai {@code 0} hanya mungkin muncul bila FK grup penilaian belum
	 * terisi sama sekali (object baru yang belum disetel).</p>
	 *
	 * <p>Konsekuensi lanjutan: {@link GeneralValueObject#compareTo(GeneralValueObject)} memakai
	 * {@code getNomorUrut()} sebagai kunci urut <b>pertama</b> dan berhenti di situ bila kedua sisi
	 * tidak {@code null}. Karena method ini tidak pernah mengembalikan {@code null}, membandingkan
	 * dua {@code DetailJenisPenilaian} selalu menghasilkan {@code 0} dan
	 * {@code Collections.sort(...)} atas daftar baris detail tidak melakukan apa-apa. Tidak ada kode
	 * yang bergantung pada hal itu &mdash; pengurutan yang nyata dilakukan atas daftar
	 * {@link GrupPenilaian} hasil proyeksi, dan {@code GrupPenilaian} meng-override
	 * {@code compareTo} sehingga memakai {@code nama} lebih dulu; layar master mengurutkan lewat SQL
	 * ({@code addOrder(Order.asc("grupPenilaian.nama"))}).</p>
	 *
	 * <p><b>Dua kuirk teknis yang perlu diketahui bila method ini akan disentuh:</b></p>
	 * <ol>
	 *   <li>Method membaca <b>field</b> {@code grupPenilaian} secara langsung, bukan lewat
	 *       {@link #getGrupPenilaian()}, sehingga melewati {@link GeneralValueObject#check(Object)}.
	 *       Pada object yang sudah lepas dari sesi Hibernate, jalur getter akan memulihkan proxy
	 *       sedangkan jalur ini bisa gagal dengan {@code LazyInitializationException}.</li>
	 *   <li>Karena kelas ini memakai akses properti dan pasangan getter/setter {@code nomorUrut}
	 *       lengkap terbentuk (getter di sini, setter diwarisi), Hibernate memperlakukannya sebagai
	 *       properti persisten &mdash; dengan {@code hbm2ddl.auto=update} kolom {@code nomorUrut}
	 *       akan dibuat pada tabel dan diaudit Envers, padahal isinya hanyalah nilai turunan yang
	 *       tidak pernah dibaca kembali (setter menulis field induk, sedangkan getter ini
	 *       mengabaikannya sama sekali). Bentuk yang sama terdapat pada {@link DetailGrupPenilaian}
	 *       dan {@link DetailGrupKategoriItemPenilaianSiswa}, jadi ini kuirk keluarga tabel
	 *       penghubung, bukan kekhususan berkas ini.</li>
	 * </ol>
	 *
	 * @return nomor urut grup penilaian yang ditunjuk; {@code 1} bila nomor urutnya {@code null}
	 *         (kasus normal), atau {@code 0} bila FK grup penilaian belum terisi
	 */
	public Integer getNomorUrut() {
		Integer nomorUrut = 0;
		if (grupPenilaian != null) {
			nomorUrut = grupPenilaian.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}


}
