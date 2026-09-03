package ais.database.model.akunting;

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




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;

/**
 * <h3>KelompokLaporanPunyaAkun &mdash; jembatan "baris laporan &harr; akun buku besar"</h3>
 *
 * <p>Entity ini memetakan tabel <b>{@code akunting.kelompok_laporan_punya_akun}</b>. Ia adalah
 * <b>tabel jembatan (join table) many-to-many</b> yang menempelkan satu
 * {@link ais.database.model.akunting.Akun} (satu baris bagan akun / Chart of Accounts) ke satu
 * {@link ais.database.model.akunting.KelompokLaporan} (satu <i>baris cetak</i> pada laporan
 * keuangan), ditambah satu angka {@code nomorUrut} untuk mengatur urutan tampil akun di dalam
 * kelompok tersebut. Tidak ada kolom lain: <b>tidak ada nominal, tidak ada tanggal berlaku, tidak
 * ada bendera aktif/nonaktif, dan tidak ada kolom tenant</b> (sekolah/yayasan/satuan kerja) &mdash;
 * murni pasangan (akun, kelompok) beserta urutannya.</p>
 *
 * <h3>Posisi dalam rantai laporan keuangan</h3>
 *
 * <p>Struktur laporan keuangan berbasis jurnal di AIS disusun berjenjang. Entity ini adalah
 * <b>daun terakhir</b> rantai tersebut &mdash; titik tempat struktur laporan akhirnya bertemu
 * dengan angka:</p>
 *
 * <pre>
 *   JenisLaporan            (Neraca / Rugi Laba / Arus Kas ...)
 *        &darr;
 *   KelompokLaporan         (satu BARIS cetak; memegang DUA FK ortogonal:
 *                            jenis_laporan + master_grup_laporan)
 *        &darr;  &nwarr; MasterGrupLaporan (seksi/blok cetak: ASET LANCAR, BEBAN OPERASIONAL, ...)
 *        &darr;
 *   KelompokLaporanPunyaAkun   &lt;&mdash; ENTITY INI (pasangan akun &harr; baris, + nomorUrut)
 *        &darr;
 *   Akun                    (bagan akun berhierarki; saldonya dihitung dari
 *                            akunting.transaksi / akunting.grup_transaksi)
 * </pre>
 *
 * <p>Perhatikan bahwa {@code MasterGrupLaporan} <b>bukan</b> induk langsung entity ini: ia
 * digantung dari {@code KelompokLaporan} sebagai sumbu pengelompokan kedua (lihat
 * {@link ais.database.model.akunting.MasterGrupLaporan}). Entity ini hanya mengenal
 * {@code KelompokLaporan} dan {@code Akun}.</p>
 *
 * <h3>Konsekuensi bisnis: baris yang tidak dipetakan = hilang dari laporan</h3>
 *
 * <p>Seluruh mesin laporan keuangan berbasis jurnal melakukan <b>INNER JOIN</b> ke tabel ini.
 * Artinya sebuah akun yang punya mutasi jurnal tetapi <b>belum</b> punya satu pun baris di sini
 * <b>tidak akan muncul sama sekali</b> di Neraca, Laba Rugi, dashboard akuntansi, maupun jurnal
 * penutup &mdash; angkanya menguap tanpa peringatan apa pun. Inilah alasan keberadaan
 * {@code PemetaanAkunHelper} (pemetaan otomatis dari bagan akun) dan halaman diagnosa
 * {@code webapp/WEB-INF/baru/modul/kantin/cek_pemetaan_akun.jsp}.</p>
 *
 * <h3>Pembaca terverifikasi (konsumen)</h3>
 *
 * <ul>
 *   <li><b>{@code ais.action.master.koperasi.helper.LaporanKeuanganCoaHelper}</b> &mdash; penyusun
 *       laporan keuangan HTML. Query HQL
 *       {@code where kp.kelompokLaporan.jenisLaporan.id = :jid order by kp.nomorUrut}, hasil
 *       di-<i>bucket</i> per {@code KelompokLaporan}; tiap baris menjumlahkan
 *       {@code saldo * akun.getDebetCredit()}.</li>
 *   <li><b>{@code ais.action.servlet.api.TutupBukuHelper}</b> &mdash; <b>penulis jurnal penutup
 *       sungguhan</b>. Memilih akun nominal lewat SQL
 *       {@code join akunting.kelompok_laporan_punya_akun b on b.akun = d.id} lalu memposting
 *       selisihnya ke akun Laba Ditahan. Struktur pemetaan di sini <b>langsung menentukan angka
 *       yang dijurnal</b>, bukan sekadar tampilan.</li>
 *   <li><b>{@code ais.action.master.dashboard.helper.DashboardAkuntingHelper}</b>,
 *       <b>{@code DashboardAkuntingTahunHelper}</b>, <b>{@code DasboardAkunting}</b>,
 *       <b>{@code DasboardAkuntansi}</b>, dan JSP
 *       {@code baru/modul/akuntansi/_monitor_akunting_service.jsp} &mdash; agregat 12 bulan,
 *       semuanya memakai pola {@code inner join ... on (a.akun = b.akun)}.</li>
 *   <li><b>{@code ais.action.master.koperasi.helper.LaporanKantinUtil}</b> &mdash; 7 titik query
 *       (uji {@code exists}/{@code not exists}) untuk menandai akun kantin yang belum terpetakan.</li>
 * </ul>
 *
 * <h3>Penulis terverifikasi (dan gerbang haknya)</h3>
 *
 * <ol>
 *   <li><b>{@code KelompokLaporanDanDetailAction}</b> (layar ZK "Setup Laporan",
 *       {@code /pages/master/akunting/kelompok_laporan_dan_detail.zul}) &mdash; panel detail
 *       <i>expand-able</i> per kelompok. Digerbangi {@code CommonPrivilages.checkPrevilages}
 *       (READ/CREATE/UPDATE/DELETE). Juga menyediakan <b>impor Excel</b> yang mengenali baris
 *       lama lewat pasangan (akun, kelompokLaporan).</li>
 *   <li><b>{@code KelompokLaporanPunyaAkunAction}</b> (layar ZK mandiri) &mdash; CRUD satu baris
 *       dengan combo Kelompok Laporan bebas. Digerbangi {@code CommonPrivilages} juga.</li>
 *   <li><b>{@code KelompokLaporanAction} / {@code KelompokLaporanDanDetailAction}, fitur
 *       "copy dari"</b> &mdash; saat sebuah {@code KelompokLaporan} baru disimpan dengan
 *       {@code copyDari} terisi, SELURUH baris entity ini milik kelompok sumber diduplikasi ke
 *       kelompok baru (dilewati bila pasangan yang sama sudah ada di kelompok tujuan).</li>
 *   <li><b>{@code ais.action.servlet.api.PemetaanAkunHelper}</b> (REST, aksi
 *       {@code pemetaan_akun_usulan} / {@code pemetaan_akun_terapkan} lewat {@code PosApi})
 *       &mdash; pemetaan massal otomatis: menelusuri rantai induk tiap akun, membuat
 *       {@code KelompokLaporan} bila belum ada, lalu menyimpan baris entity ini. Hanya menambah,
 *       tidak pernah menghapus/memindah.</li>
 *   <li><b>{@code webapp/WEB-INF/baru/modul/kantin/cek_pemetaan_akun.jsp}</b> &mdash; tombol
 *       "petakan cepat" pada halaman diagnosa; satu-satunya penulis yang memeriksa duplikat
 *       lewat {@code select count(*) ... where akun=:a and kelompok_laporan=:k}.</li>
 *   <li><b>{@code ais.action.master.akunting.helper.AmbilDataBanyakListAkun}</b> &mdash; layar
 *       grid centang massal. <b>Terverifikasi YATIM</b>: nol pemanggil di seluruh repo (Java,
 *       ZUL, maupun JSP), jadi jalurnya laten, bukan hidup. Lihat catatan "Kuirk" di bawah.</li>
 * </ol>
 *
 * <h3>PENTING &mdash; satu akun BISA masuk lebih dari satu kelompok laporan (DIPERBAIKI SEBAGIAN)</h3>
 *
 * <p><b>Status sebelum perbaikan (diverifikasi dari kode, masih berlaku untuk data historis):</b>
 * entity ini <b>tidak</b> mendeklarasikan {@code @UniqueConstraint} atas pasangan (akun,
 * kelompok_laporan), dan tidak ada pemeriksaan apa pun di setter/getter. Jalur impor Excel dan
 * fitur "copy dari" hanya memeriksa duplikat <b>di dalam kelompok tujuan</b> &mdash; akun yang
 * sama tetap boleh muncul di kelompok lain pada jenis laporan yang sama. Fitur "copy dari" bahkan
 * merupakan <b>sumber duplikasi lintas kelompok</b>: seluruh isi kelompok sumber disalin ke
 * kelompok baru sehingga tiap akun sumber bisa terdaftar di dua kelompok sekaligus.</p>
 *
 * <p><b>Status setelah perbaikan (migrasi {@code webapp/sql/
 * migrasi_dedup_kelompok_laporan_punya_akun_20260904.sql}):</b></p>
 * <ul>
 *   <li>{@code KelompokLaporanPunyaAkunAction.onSave} dan
 *       {@code KelompokLaporanDanDetailAction.KelompokLaporanPunyaAkunAction.onSave} kini menolak
 *       simpan (pesan menyebutkan nama kelompok yang sudah memuat akun tersebut) bila akun sudah
 *       terpetakan ke kelompok LAIN pada <i>jenis laporan yang sama</i> &mdash; lewat method privat
 *       {@code cariBentrokJenisLaporan}, dibandingkan bukan hanya per pasangan (akun,
 *       kelompok_laporan) tetapi per (akun, jenis_laporan), karena itulah unit yang benar-benar
 *       dipakai konsumen di bawah (TutupBukuHelper, dashboard) untuk memutuskan "akun ini masuk
 *       Rugi Laba atau tidak".</li>
 *   <li>Fitur "copy dari" pada kedua Action kini melewati (skip) akun sumber yang akan bentrok
 *       dengan pemetaan lain pada jenis laporan yang sama, dan melaporkan jumlah yang dilewati
 *       lewat messagebox informasi &mdash; bukan lagi menduplikasi tanpa syarat.</li>
 *   <li><b>Trigger DB</b> {@code akunting.trg_cegah_duplikasi_kelompok_laporan_punya_akun}
 *       menegakkan aturan yang sama pada level basis data untuk jalur yang TIDAK melalui kedua
 *       {@code onSave} di atas: impor Excel, {@code cek_pemetaan_akun.jsp},
 *       {@code PemetaanAkunHelper}, dan penulis mana pun di masa depan. Ditambah indeks unik
 *       {@code uq_kelompok_laporan_punya_akun_pasangan} atas pasangan (akun, kelompok_laporan)
 *       persis sebagai rem kedua yang lebih murah.</li>
 *   <li><b>Belum dibersihkan:</b> baris duplikat yang SUDAH ada di data sebelum migrasi ini TIDAK
 *       disentuh (trigger hanya menolak baris baru/ubahan, bukan retroaktif). Lihat AUDIT 1-3 di
 *       akhir skrip migrasi untuk mengukur dan meninjau data historis sebelum membersihkannya.</li>
 * </ul>
 *
 * <p><b>Dampaknya terhadap angka (tetap berlaku untuk duplikat historis yang belum dibersihkan).</b>
 * Karena semua konsumen melakukan INNER JOIN pada {@code akun}, satu akun yang terdaftar di dua
 * kelompok pada <i>jenis laporan yang sama</i> membuat baris jurnalnya <b>terhitung dua kali</b>:</p>
 * <ul>
 *   <li>{@code LaporanKeuanganCoaHelper} menghasilkan dua baris cetak yang masing-masing memuat
 *       saldo penuh akun tersebut &mdash; subtotal grup dan total laporan menggelembung.</li>
 *   <li>Dashboard 12 bulan ({@code DashboardAkuntingHelper} dkk.) menggandakan baris
 *       {@code akunting.transaksi} pada join sehingga {@code sum(debet-kredit)} ikut berlipat.</li>
 *   <li><b>Paling berat:</b> {@code TutupBukuHelper} <i>dulu</i> menggunakan pola join yang sama
 *       untuk memilih akun nominal dan besaran yang ditutup, sehingga duplikasi pemetaan berarti
 *       jurnal penutup yang benar-benar diposting ke buku besar memuat nominal berlipat. Query itu
 *       sudah ditulis ulang memakai {@code EXISTS} (bukan {@code JOIN}) sehingga TIDAK LAGI
 *       menggandakan baris berapa pun jumlah pemetaan yang cocok, terlepas dari status data
 *       historis. Konsumen lain ({@code LaporanKeuanganCoaHelper}, dashboard) belum diubah pola
 *       query-nya &mdash; perlindungannya bergantung pada penjaga tulis di atas.</li>
 * </ul>
 *
 * <p>Rem parsial lama di {@code PemetaanAkunHelper} (melewati akun yang sudah terpetakan ke
 * kelompok <i>aktif</i> mana pun, {@code SELECT DISTINCT p.akun ...}) tetap ada dan kini
 * bertumpuk dengan trigger DB di atas.</p>
 *
 * <h3>Cakupan tenant: TIDAK ADA sama sekali</h3>
 *
 * <p>Entity ini tidak memiliki kolom sekolah/yayasan/satuan kerja, dan tidak satu pun konsumen
 * menyaringnya per tenant &mdash; <b>struktur pemetaan bersifat global untuk seluruh instalasi</b>.
 * Penyaringan tenant baru muncul satu tingkat lebih jauh, pada {@code grup_transaksi.satuan_kerja}
 * milik jurnal yang diagregasi. Ini bukan pola "fail-open kondisional" seperti pada beberapa
 * entity dokumen keuangan lain, melainkan ketiadaan dimensi tenant secara desain: satu operator
 * mengubah pemetaan, seluruh tenant berubah laporannya.</p>
 *
 * <p>Fail-open cakupan yang sesungguhnya berada di pemakainya:
 * {@code TutupBukuHelper.satkerId(...)} mengembalikan {@code -1} baik saat satuan kerja kantin
 * tidak dikonfigurasi maupun saat resolusinya melempar exception, dan {@code -1} diterjemahkan
 * SQL-nya menjadi <b>"semua satuan kerja"</b> ({@code :satker = -1 or a1.satuan_kerja = :satker}).
 * Jadi kegagalan konfigurasi memperlebar cakupan jurnal penutup, bukan mempersempitnya.</p>
 *
 * <h3>Gerbang hak akses pada jalur REST (kaitan pola fail-open yang sudah dilacak)</h3>
 *
 * <ul>
 *   <li>{@code TutupBukuHelper.bolehAksiMenu(...)} mengulang persis pola fail-open yang sudah
 *       dilacak sebagai {@code task_66986071}: {@code peran == null} &rarr; {@code return true}
 *       ("kompatibilitas akun lama"). Di sini gerbang itu menjaga aksi
 *       {@code tutup_buku_posting}, yaitu penulisan jurnal penutup ke buku besar.</li>
 *   <li>{@code PemetaanAkunHelper} lebih longgar lagi: {@code proses(...)} menerima
 *       {@code Tbmuser} tetapi <b>tidak pernah memakainya</b> &mdash; tidak ada
 *       {@code bolehAksi}, tidak ada cek admin, dan awalan {@code pemetaan_akun_} tidak terdaftar
 *       di {@code EbisnisMenuKatalog} sehingga tidak ada kunci menu yang bisa dimatikan admin.
 *       Aksi {@code pemetaan_akun_terapkan} membuat {@code KelompokLaporan} baru dan menulis
 *       baris entity ini secara massal.</li>
 *   <li>{@code cek_pemetaan_akun.jsp} hanya menolak pengunjung yang belum login
 *       ({@code uCP == null} &rarr; 401); tidak ada pemeriksaan peran/menu, dan aksinya dipicu
 *       oleh parameter request biasa ({@code ?aksi=petakan&amp;akunId=..&amp;kelompokId=..}).</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 *
 * <ul>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #setId(Long)}.</li>
 *   <li><b>Jejak audit ringan</b> (pola seragam seluruh model AIS): {@link #getOleh()},
 *       {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, dan hook
 *       {@link #onUpdate()}. Ditambah {@code @Audited} (Hibernate Envers) yang menggandakan
 *       setiap versi baris ke tabel {@code kelompok_laporan_punya_akun_aud}.</li>
 *   <li><b>Relasi</b>: {@link #getAkun()}/{@link #setAkun(Akun)} dan
 *       {@link #getKelompokLaporan()}/{@link #setKelompokLaporan(KelompokLaporan)}. Keduanya
 *       {@code @JoinColumn(nullable = false)} &mdash; wajib di tingkat DDL.</li>
 *   <li><b>Penyajian</b>: {@link #getNomorUrut()}/{@link #setNomorUrut(Integer)} dan
 *       {@link #compareTo(GeneralValueObject)}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious / kuirk</h3>
 *
 * <ol>
 *   <li><b>Komentar kelas asli salah.</b> Javadoc bawaan hbm2java berbunyi
 *       "Bank generated by hbm2java" &mdash; sisa salin-tempel dari entity {@code Bank}, tidak ada
 *       hubungannya dengan kelas ini. Diganti oleh dokumentasi ini.</li>
 *   <li><b>{@code serialVersionUID} bukan penanda identitas.</b> Nilai
 *       {@code 2463821577548439808L} dipakai bersama oleh <b>967 kelas model</b> di repo ini
 *       (termasuk {@code KelompokLaporan} sendiri) &mdash; artefak generator, jangan dipakai untuk
 *       menyimpulkan kekerabatan antar-entity.</li>
 *   <li><b>Asimetri {@code check()} pada dua getter relasi.</b> {@link #getAkun()} memanggil
 *       {@code check(...)} dan <i>menulis balik</i> hasilnya ke field, sedangkan
 *       {@link #getKelompokLaporan()} tidak. Ini konsisten dengan strategi fetch-nya:
 *       {@code akun} dipetakan {@code FetchType.LAZY} (berpotensi proxy),
 *       {@code kelompokLaporan} dibiarkan pada default {@code EAGER} + {@code FetchMode.SELECT}
 *       sehingga sudah terisi objek nyata. Penulisan-balik di {@code getAkun()} bersifat
 *       <b>resolusi proxy, bukan destruktif</b>: nilainya tetap entity ber-ID sama (tahap
 *       {@code EntityIdentityMap} / reload), sehingga <b>tidak</b> memindahkan atribusi akun
 *       seperti {@code Transaksi.getAkun()} yang menimpa dengan {@code akunOver}. Meski begitu,
 *       kombinasi {@code dynamicUpdate = true} + akses properti berarti instance hasil resolusi
 *       itulah yang akan diperiksa Hibernate saat flush.</li>
 *   <li><b>{@code getNomorUrut()} menyembunyikan {@code NULL} sebagai {@code 0}</b>, tetapi
 *       pengurutan di lapisan query bekerja pada kolom mentah. Baris yang lahir tanpa nomor urut
 *       (impor Excel tanpa kolom urut, fitur "copy dari" yang <b>tidak</b> menyalin
 *       {@code nomorUrut}, {@code PemetaanAkunHelper}, dan JSP diagnosa) tersimpan {@code NULL};
 *       di Java ia terlihat sebagai {@code 0} dan mengurut paling depan, sementara di
 *       PostgreSQL {@code order by nomorurut} menempatkan {@code NULL} paling belakang secara
 *       default. Urutan akun dalam satu baris laporan karena itu bisa berbeda antara layar ZK dan
 *       laporan cetak.</li>
 *   <li><b>Layar centang massal {@code AmbilDataBanyakListAkun} yatim namun berbahaya bila
 *       dihidupkan.</b> Renderer-nya menghitung baris dengan {@code Restrictions.eq("akun", akun)}
 *       <b>tanpa</b> menyaring kelompok laporan, dan jalur "uncheck"-nya menghapus baris hasil
 *       {@code setMaxResults(1)} tanpa filter kelompok dan tanpa {@code addOrder} &mdash; mencabut
 *       centang di kelompok A dapat menghapus pemetaan milik kelompok B. Ditambah lagi kelas itu
 *       <b>tidak memanggil {@code CommonPrivilages} sama sekali</b> (pola "checkbox grid tanpa
 *       gerbang"). Karena nol pemanggil, ini risiko laten, bukan celah aktif.</li>
 *   <li><b>{@code check()} tidak pernah melempar exception dan tidak pernah mengembalikan
 *       {@code null}</b> untuk argumen non-null; kegagalan resolusi bersifat senyap. Bila
 *       {@link #getAkun()} mengembalikan proxy yang meledak di pemanggil, curigai tahap reload
 *       pada {@link ais.database.model.GeneralValueObject#check(Object)}. Semua konsumen laporan
 *       memang membungkus pembacaan ini dengan {@code try/catch} yang melewati baris bermasalah
 *       diam-diam &mdash; akun yang gagal resolusi hilang dari laporan tanpa jejak di UI.</li>
 * </ol>
 *
 * <h3>Catatan pewarisan dari {@code GeneralValueObject}</h3>
 *
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu
 * pun properti induknya. Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan bug melainkan keharusan teknis</b>: tanpa
 * pengulangan itu kolom-kolom tersebut tidak akan pernah dipetakan. Yang benar-benar diwarisi
 * hanyalah utilitas non-persisten seperti {@code check(...)}, {@code getNama()},
 * {@code getKeterangan()}, dan {@code getNim()}.</p>
 *
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.akunting.KelompokLaporan
 * @see ais.database.model.akunting.MasterGrupLaporan
 * @see ais.database.model.akunting.JenisLaporan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "kelompok_laporan_punya_akun")
public class KelompokLaporanPunyaAkun extends GeneralValueObject {

	/**
	 * Versi serialisasi Java bawaan generator.
	 *
	 * <p>Nilai ini <b>dipakai bersama oleh 967 kelas model lain</b> di repo (termasuk
	 * {@link KelompokLaporan}), jadi kesamaannya sama sekali bukan indikasi bahwa dua kelas
	 * merupakan salinan satu sama lain. Jangan diubah: instance entity ini ikut tersimpan pada
	 * sesi ZK yang diserialisasi.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code akunting.kelompok_laporan_punya_akun} (IDENTITY, diisi database). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi interceptor audit, bukan oleh UI. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris pemetaan ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah melalui interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi &mdash; nilai lama dipertahankan. Pola ini seragam di seluruh model AIS dan
	 * bertujuan agar jejak audit tidak terhapus oleh pemanggil yang mengirim nilai kosong; efek
	 * sampingnya, kolom ini tidak dapat dikosongkan kembali lewat setter.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong <b>diabaikan tanpa pesan</b> sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris pemetaan ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} sekaligus deklarasi field stempel waktu perubahan.
	 *
	 * <p><b>Tujuan:</b> tepat sebelum Hibernate menerbitkan {@code UPDATE} atas baris pemetaan
	 * ini, {@code AuditTimestampInterceptor.ubah(this)} dipanggil untuk mengisi
	 * {@link #oleh}/{@link #olehId} dari konteks pengguna aktif dan menyegarkan
	 * {@code tanggal_dirubah}.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi tiga field audit pada instance ini. Tidak menyentuh
	 * {@link #akun}, {@link #kelompokLaporan}, maupun {@link #nomorUrut}.</p>
	 *
	 * <p><b>Non-obvious:</b> deklarasi field {@code tanggal_dirubah} sengaja ditempel pada baris
	 * yang sama dengan method ini (gaya generator repo). Nilai awalnya <b>bukan</b> {@code null}
	 * melainkan {@code ais.ui.util.WaktuUtil.getDate()} pada saat objek Java dibuat &mdash; jadi
	 * baris yang baru di-{@code new} sudah membawa stempel waktu meskipun belum pernah disimpan.
	 * Perlu diingat pula bahwa hook ini <b>hanya</b> berjalan pada {@code UPDATE}, bukan pada
	 * {@code INSERT}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi; pengisiannya diserahkan ke {@link #onUpdate()}.
	 * Berbeda dari setter {@code oleh}/{@code olehId}, setter ini <b>menerima</b> {@code null}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris pemetaan ini.
	 *
	 * @return stempel waktu (presisi {@code TIMESTAMP}); praktis tidak pernah {@code null} karena
	 *         field-nya sudah diinisialisasi saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Akun buku besar yang dipetakan ke baris laporan ini (FK {@code akun}, wajib).
	 *
	 * <p>Dipetakan {@code LAZY}; getter-nya me-resolve proxy lewat {@code check(...)}.</p>
	 */
	private Akun akun;
	/**
	 * Baris laporan (kelompok) tempat {@link #akun} ditampilkan (FK {@code kelompok_laporan},
	 * wajib). Dipetakan {@code EAGER} + {@code FetchMode.SELECT}.
	 */
	private KelompokLaporan kelompokLaporan;
	/**
	 * Nomor urut tampil akun di dalam kelompok laporan. Boleh {@code NULL} di database; lihat
	 * {@link #getNomorUrut()} untuk perbedaan perlakuan {@code NULL} antara Java dan SQL.
	 */
	private Integer nomorUrut;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Seluruh field relasi dibiarkan {@code null}; kedua FK wajib diisi lewat
	 * {@link #setAkun(Akun)} dan {@link #setKelompokLaporan(KelompokLaporan)} sebelum
	 * disimpan, karena keduanya {@code nullable = false} di tingkat kolom.</p>
	 */
	public KelompokLaporanPunyaAkun() {
	}

	/**
	 * Mengembalikan kunci utama baris pemetaan ini.
	 *
	 * <p>Kolomnya {@code insertable = false} karena diisi oleh strategi {@code IDENTITY} di sisi
	 * database. Nilai {@code null} berarti baris belum pernah disimpan &mdash; itulah pembeda mode
	 * "tambah" dan "ubah" pada seluruh Action ZK yang mengelola entity ini.</p>
	 *
	 * @return id baris, atau {@code null} bila objek masih transient
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama baris pemetaan ini.
	 *
	 * <p>Dipakai kerangka kerja (Hibernate, helper CRUD generik). Kode aplikasi sebaiknya tidak
	 * mengubah id baris yang sudah tersimpan.</p>
	 *
	 * @param id kunci utama; {@code null} untuk baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan akun buku besar yang dipetakan ke baris laporan ini.
	 *
	 * <p><b>Efek samping:</b> murni penugasan field, tanpa validasi. <b>Tidak ada</b> pemeriksaan
	 * apakah pasangan (akun, kelompokLaporan) sudah pernah ada &mdash; lihat bagian "satu akun
	 * BISA masuk lebih dari satu kelompok laporan" pada dokumentasi kelas: duplikasi yang terjadi
	 * di sini berujung pada nominal berlipat di laporan dan di jurnal penutup.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code KelompokLaporanPunyaAkunAction.onSave},
	 * {@code KelompokLaporanDanDetailAction} (form detail, impor Excel, dan fitur "copy dari"),
	 * {@code KelompokLaporanAction} (fitur "copy dari"), {@code PemetaanAkunHelper},
	 * {@code AmbilDataBanyakListAkun}, serta {@code cek_pemetaan_akun.jsp}.</p>
	 *
	 * @param akun akun buku besar; wajib non-{@code null} sebelum penyimpanan karena kolomnya
	 *             {@code nullable = false}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan akun buku besar yang dipetakan ke baris laporan ini.
	 *
	 * <p><b>Tujuan:</b> menyediakan akses yang aman terhadap relasi {@code LAZY} ini. Karena
	 * relasinya {@code FetchType.LAZY}, Hibernate biasanya mengisi field dengan <i>proxy</i> yang
	 * akan melempar {@code LazyInitializationException} bila diakses setelah sesi ditutup.
	 * Panggilan {@code check(...)} dari {@link ais.database.model.GeneralValueObject} melakukan
	 * de-proxy berjenjang (cache identitas &rarr; sesi aktif &rarr; reload sesi baru).</p>
	 *
	 * <p><b>Efek samping (penulisan-balik):</b> hasil {@code check(...)} <b>ditugaskan kembali ke
	 * field</b> {@code akun}, jadi getter ini memutasi state objek. Mutasinya bersifat
	 * <i>resolusi</i>, bukan penggantian makna: yang dikembalikan tetap entity dengan id yang
	 * sama, sehingga <b>tidak</b> memindahkan atribusi akun sebagaimana kuirk
	 * {@code Transaksi.getAkun()} yang menimpa dengan {@code akunOver}. Meski demikian, karena
	 * kelas ini {@code dynamicUpdate = true} dan Hibernate memakai akses properti, instance hasil
	 * resolusi itulah yang diperiksa saat flush.</p>
	 *
	 * <p><b>Kontrak kegagalan:</b> {@code check(...)} tidak pernah melempar exception dan tidak
	 * pernah mengembalikan {@code null} untuk argumen non-null; bila seluruh tahap resolusi gagal,
	 * proxy dikembalikan apa adanya dan barulah pemanggil yang meledak. Seluruh konsumen laporan
	 * membungkus pembacaan ini dengan {@code try/catch} yang <b>melewati baris bermasalah secara
	 * diam-diam</b>, sehingga akun yang gagal di-resolve hilang dari laporan tanpa peringatan.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code LaporanKeuanganCoaHelper.susun} (untuk mengambil
	 * {@code kode}/{@code nama}/{@code debetCredit} dan mengalikan saldo), renderer grid ZK
	 * ({@code KelompokLaporanPunyaAkunRenderer} di dua Action), fitur "copy dari", dan pemeriksaan
	 * duplikat pada impor Excel.</p>
	 *
	 * @return akun buku besar yang sudah teresolusi; secara praktik selalu non-{@code null} untuk
	 *         baris yang tersimpan karena kolomnya {@code nullable = false}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = false)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan baris laporan (kelompok) tempat akun ini ditampilkan.
	 *
	 * <p><b>Efek samping:</b> penugasan field murni, tanpa validasi dan tanpa cek duplikat.
	 * Memindahkan sebuah pemetaan ke kelompok lain berarti <b>memindahkan angka akun tersebut ke
	 * baris cetak lain</b> pada Neraca/Laba Rugi &mdash; dan, bila kelompok tujuan berada di jenis
	 * laporan berbeda, memindahkannya ke laporan yang berbeda sama sekali. Perubahan berlaku
	 * seketika dan retroaktif untuk seluruh periode, karena laporan dihitung ulang dari jurnal
	 * setiap kali dicetak (tidak ada snapshot).</p>
	 *
	 * <p><b>Dipanggil dari:</b> sumber yang sama dengan {@link #setAkun(Akun)}. Pada
	 * {@code KelompokLaporanDanDetailAction} nilainya diambil dari konteks panel detail (kelompok
	 * yang sedang dibuka), sedangkan pada {@code KelompokLaporanPunyaAkunAction} dari combo yang
	 * bebas dipilih operator.</p>
	 *
	 * @param kelompokLaporan baris laporan tujuan; wajib non-{@code null} sebelum penyimpanan
	 */
	public void setKelompokLaporan(KelompokLaporan kelompokLaporan) {
		this.kelompokLaporan = kelompokLaporan;
	}

	/**
	 * Mengembalikan baris laporan (kelompok) tempat akun ini ditampilkan.
	 *
	 * <p><b>Non-obvious &mdash; asimetri dengan {@link #getAkun()}:</b> getter ini <b>tidak</b>
	 * memanggil {@code check(...)} dan karenanya tidak memutasi state. Itu aman karena relasi ini
	 * dibiarkan pada default {@code @ManyToOne} yaitu {@code EAGER}, dipertegas
	 * {@code @Fetch(FetchMode.SELECT)} yang memuatnya lewat SELECT terpisah alih-alih ikut dalam
	 * outer join &mdash; sehingga field praktis selalu berisi objek nyata, bukan proxy. Bila suatu
	 * saat strategi fetch diubah menjadi {@code LAZY}, getter ini <b>harus</b> ditambahi
	 * {@code check(...)} seperti saudaranya.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code LaporanKeuanganCoaHelper.ambilPemetaan} (untuk
	 * mengelompokkan hasil query per id kelompok) dan renderer grid ZK.</p>
	 *
	 * @return kelompok laporan pemilik baris ini; secara praktik non-{@code null} untuk baris
	 *         tersimpan karena kolomnya {@code nullable = false}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kelompok_laporan", nullable = false)
	public KelompokLaporan getKelompokLaporan() {
		return kelompokLaporan;
	}

	/**
	 * Mengembalikan nomor urut tampil akun di dalam kelompok laporan, dengan {@code NULL}
	 * disubstitusi menjadi {@code 0}.
	 *
	 * <p><b>Tujuan:</b> membuat {@link #compareTo(GeneralValueObject)} dan renderer grid bebas
	 * dari pemeriksaan {@code null}.</p>
	 *
	 * <p><b>Non-obvious &mdash; beda urutan Java vs SQL:</b> substitusi ini hanya berlaku di
	 * memori. Query yang mengurutkan langsung di database tidak melihatnya:</p>
	 * <ul>
	 *   <li>{@code KelompokLaporanDanDetailAction.initCriteria} memakai
	 *       {@code addOrder(Order.asc("nomorUrut"))}, dan pada PostgreSQL {@code ORDER BY ... ASC}
	 *       menempatkan {@code NULL} di <b>akhir</b>;</li>
	 *   <li>{@code LaporanKeuanganCoaHelper} memakai {@code order by kp.nomorUrut} dengan
	 *       perilaku yang sama;</li>
	 *   <li>di Java, baris {@code NULL} yang sama dinilai {@code 0} sehingga mengurut paling
	 *       <b>depan</b>.</li>
	 * </ul>
	 * <p>Baris ber-{@code NULL} bukan kasus langka: fitur "copy dari" tidak menyalin nomor urut,
	 * dan {@code PemetaanAkunHelper}, {@code AmbilDataBanyakListAkun}, serta
	 * {@code cek_pemetaan_akun.jsp} tidak pernah mengisinya. Impor Excel hanya mengisi bila nilai
	 * di kolom kedua lebih besar dari nol.</p>
	 *
	 * <p><b>Efek samping:</b> tidak ada &mdash; getter ini tidak menulis balik ke field, jadi
	 * baris yang {@code NULL} di database tetap {@code NULL} setelah dibaca.</p>
	 *
	 * @return nomor urut tampil, atau {@code 0} bila belum pernah diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Mengisi nomor urut tampil akun di dalam kelompok laporan.
	 *
	 * <p>Menerima {@code null} (mengembalikan baris ke keadaan "belum diurutkan"). Tidak ada
	 * validasi keunikan nomor urut dalam satu kelompok; nomor kembar dibiarkan dan urutan akhir
	 * ditentukan kriteria pengurutan kedua ({@code Order.desc("id")} pada layar detail).</p>
	 *
	 * @param nomorUrut nomor urut baru; boleh {@code null}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan dua objek untuk keperluan pengurutan koleksi di memori.
	 *
	 * <p><b>Tujuan:</b> implementasi {@code Comparable} yang diwajibkan
	 * {@link ais.database.model.GeneralValueObject}, dipakai saat daftar pemetaan diurutkan di
	 * sisi Java (mis. {@code SimpleListModel} yang diurutkan sebelum dirender).</p>
	 *
	 * <p><b>Cara kerja &mdash; dua cabang:</b></p>
	 * <ol>
	 *   <li><b>Argumen sejenis</b> ({@code instanceof KelompokLaporanPunyaAkun}): dibandingkan
	 *       berdasarkan {@link #getNomorUrut()}. Karena getter itu mengganti {@code NULL} menjadi
	 *       {@code 0}, seluruh baris tanpa nomor urut dinilai setara dan mengumpul di depan.</li>
	 *   <li><b>Argumen jenis lain</b>: jatuh ke perbandingan generik warisan induk &mdash;
	 *       {@code getNim()}, lalu {@code getNama()}, lalu {@code getKeterangan()} &mdash; yang
	 *       pada entity ini <b>tidak</b> di-<i>override</i>, sehingga hasilnya mengikuti
	 *       implementasi bawaan {@code GeneralValueObject}. Cabang ini praktis tidak pernah
	 *       terpakai pada alur normal.</li>
	 * </ol>
	 *
	 * <p><b>Non-obvious:</b> cabang kedua dibungkus {@code try/catch} yang <b>menelan seluruh
	 * exception</b> (dicatat ke {@code ErrorAuditUtil}) lalu mengembalikan {@code 0} &mdash;
	 * artinya kegagalan perbandingan diterjemahkan sebagai "sama", bukan sebagai error. Perlu
	 * diingat pula bahwa perbandingan ini <b>tidak konsisten dengan {@code equals}</b>: dua
	 * pemetaan berbeda dengan nomor urut sama menghasilkan {@code 0}, sehingga jangan dipakai
	 * sebagai comparator untuk {@code TreeSet}/{@code TreeMap}.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getNomorUrut()} (tidak memutasi apa pun); cabang
	 * kedua dapat memicu pembacaan properti warisan.</p>
	 *
	 * @param arg0 objek pembanding; boleh berupa entity jenis lain
	 * @return bilangan negatif/nol/positif sesuai kontrak {@code Comparable}; {@code 0} juga
	 *         dikembalikan bila perbandingan generik gagal atau tidak menemukan atribut yang bisa
	 *         dibandingkan
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof KelompokLaporanPunyaAkun) {
			KelompokLaporanPunyaAkun s = (KelompokLaporanPunyaAkun) arg0;
			return getNomorUrut().compareTo(s.getNomorUrut());
		} else {
			try {
				if (getNim() != null && arg0.getNim() != null) {
					return getNim().compareTo(arg0.getNim());
				} else if (getNama() != null && arg0.getNama() != null) {
					return getNama().compareTo(arg0.getNama());
				} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
					return getKeterangan().compareTo(arg0.getKeterangan());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/KelompokLaporanPunyaAkun.java:141");

			}

			return 0;
		}
	}

}
