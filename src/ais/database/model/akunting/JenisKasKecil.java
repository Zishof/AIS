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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;

/**
 * Katalog <b>Jenis Kas Kecil</b> (<i>petty cash</i>) &mdash; satu baris tabel
 * <code>public.jenis_kas_kecil</code>, mewakili satu &quot;dompet&quot; kas kecil milik sebuah
 * satuan kerja: akun buku besarnya, akun penutupnya, saldo awal beserta tanggalnya, dan penanda
 * aktif. Judul layarnya adalah <i>&quot;Tambah/Ubah Jenis Kas Kecil&quot;</i>
 * ({@code JenisKasKecilAction}), sedangkan menu yang membukanya bernama
 * <i>&quot;Saldo Awal Kas Kecil&quot;</i> &mdash; dua nama untuk data yang sama, dan nama menu itu
 * lebih jujur menggambarkan perannya (lihat bagian &quot;Master yang ikut dijurnal&quot;).
 *
 * <h3>Peran dalam mesin akuntansi</h3>
 *
 * <p>Entity ini punya <b>dua peran sekaligus</b>, dan itulah hal pertama yang harus dipahami
 * sebelum menyentuhnya:</p>
 *
 * <ol>
 *   <li><b>Data master</b> &mdash; sumber akun jurnal bagi seluruh siklus kas kecil. Tidak ada
 *       satu pun layar posting kas kecil yang menentukan akunnya sendiri; semuanya menelusuri
 *       <code>dokumen.getKasKecil().getJenisKasKecil().getAkun()</code> atau varian rantainya.</li>
 *   <li><b>Dokumen yang ikut dijurnal</b> &mdash; baris katalog ini <i>sendiri</i> diposting ke
 *       buku besar sebagai jurnal penyerahan saldo awal, memakai kolom
 *       {@link #getPostingHistory()} sebagai cap posting dan {@link #getSaldoAwal()} sebagai
 *       nominalnya. Master yang ikut menjadi dokumen adalah pola yang tidak lazim di paket ini dan
 *       menjelaskan kehadiran field {@link #getDaftarPengajuanTransfer()} dan
 *       {@link #getTanggalTransaksi()} yang biasanya hanya dimiliki entity transaksi.</li>
 * </ol>
 *
 * <p><b>Pemakaian dua kolom akun (TERVERIFIKASI dari kode pemanggil, bukan dari nama kolom):</b></p>
 *
 * <ul>
 *   <li>{@link #getAkun()} &mdash; label layar <i>&quot;Akun Kas Kecil *&quot;</i>, wajib diisi.
 *       Dipakai sebagai <b>akun DEBET</b> pada jurnal saldo awal
 *       ({@code PostingJenisKasKecilAction}), pada jurnal penggantian/pengisian ulang
 *       ({@code PostingPenggantianKasKecilAction}, {@code PostingKasBesarAction} untuk kas kecil
 *       yang didanai kas besar), dan sebagai <b>akun KREDIT</b> pada jurnal pemakaian kas kecil
 *       ({@code PostingKasKecilAction} &mdash; dompet berkurang, akun biaya dari
 *       {@code KasKecil.formula} yang didebet). Akun ini juga menjadi sumber data bank tujuan
 *       transfer: {@code DaftarPengajuanTransfer} membaca
 *       <code>akun.getBank()/getNoRek()/getAtasNama()</code> dari sini.</li>
 *   <li>{@link #getAkunPenutupKasKecil()} &mdash; label layar <i>&quot;Akun Penutup Kas
 *       Kecil&quot;</i>, opsional. <b>Hanya</b> dipakai {@code PostingKasKecilAction} ketika
 *       pengajuan kas kecil ditandai {@code merupakanPenutupanKasKecil}: ditambahkan satu kaki
 *       DEBET tambahan sebesar sisa dompet, yaitu jurnal penutupan/pengembalian dompet. Bila kolom
 *       ini kosong, kaki tersebut <b>tidak terbentuk</b> dan penutupan berjalan tanpa
 *       menihilkan akun kas kecil &mdash; tanpa pesan galat apa pun.</li>
 * </ul>
 *
 * <p>Konsekuensinya sama seperti pada {@link ais.database.model.akunting.JenisUangMuka}:
 * <b>mengubah satu baris katalog ini mengubah akun buku besar</b> setiap dokumen kas kecil yang
 * belum diposting yang menunjuk baris tersebut, karena mesin posting membaca akun secara
 * <i>live</i> saat tombol Posting ditekan &mdash; tidak ada potret akun yang disimpan di dokumen.</p>
 *
 * <h3>Hubungan ke {@link ais.database.model.akunting.KasKecil} (terverifikasi)</h3>
 *
 * <p>{@link ais.database.model.akunting.KasKecil} (dokumen <i>pengajuan</i> pemakaian kas kecil)
 * menunjuk katalog ini lewat kolom FK <code>akunting.kas_kecil.jenis_kas_kecil</code>; sisi
 * balikannya <b>tidak</b> dipetakan di kelas ini (tidak ada koleksi <code>kasKecils</code>), jadi
 * relasinya satu arah dari dokumen ke katalog. Empat kaitan yang benar-benar dipakai:</p>
 *
 * <ul>
 *   <li><b>Saldo dompet.</b> {@code JenisKasKecilAction.hitungSaldo(bukanId, jenisKasKecil,
 *       tanggal)} menghitung <code>{@link #getSaldoAwal()} &minus; &Sigma;KasKecil.nilai</code>
 *       untuk seluruh pengajuan pada jenis ini yang <i>belum diganti</i>
 *       (<code>penggantianKasKecil.tanggalPersetujuan IS NULL</code>), masih aktif
 *       (<code>aktif IS NULL OR aktif = true</code>), dan tanggal pengajuannya tidak melewati
 *       tanggal acuan. Jadi <b>saldo tidak disimpan di mana pun</b> &mdash; ia selalu dihitung
 *       ulang, dan {@link #getSaldoAwal()} adalah satu-satunya angka permanen dompet ini.</li>
 *   <li><b>Satuan kerja.</b> {@code KasKecil.getSatuanKerja()} dan
 *       {@code GrupTransaksi.getSatuanKerja()} menurunkan unit dari
 *       {@link #getSatuanKerja()} bila dokumen tidak menyebutkannya sendiri.</li>
 *   <li><b>Akun jurnal.</b> Seluruh kaki jurnal kas kecil, penggantian kas kecil, dan kas besar
 *       yang menyentuh kas kecil membaca {@link #getAkun()} /
 *       {@link #getAkunPenutupKasKecil()} dari sini (lihat daftar di atas).</li>
 *   <li><b>Kunci jurnal.</b> {@code GrupTransaksi} memiliki kolom FK <code>jenis_kas_kecil</code>
 *       sendiri; {@code GrupTransaksi.ambilUnik()} menyusun kunci
 *       <code>JenisKasKecil.class.getName() + &quot;_&quot; + id</code> sehingga jurnal saldo awal
 *       tiap jenis tidak bertabrakan dengan dokumen lain.</li>
 * </ul>
 *
 * <p><b>Jalur tulis dari {@code GrupTransaksi}.</b> {@code GrupTransaksi.simpanNamaRincianKasKecil}
 * menulis rincian ke {@code KasKecil.formula} &mdash; itu menyentuh <b>dokumen</b>
 * {@code KasKecil}, bukan katalog ini. Katalog ini tidak pernah ditulis dari jalur jurnal;
 * satu-satunya kolomnya yang disentuh mesin posting adalah {@link #setPostingHistory(PostingHistory)}.</p>
 *
 * <h3>Master yang ikut dijurnal: alur saldo awal</h3>
 *
 * <ol>
 *   <li>Petugas keuangan membuat baris katalog di layar <i>Jenis Kas Kecil</i> dan mengisi saldo
 *       awal beserta tanggalnya.</li>
 *   <li>{@code JenisKasKecilAction.onSave} menyalakan timer sekali-tembak yang memanggil
 *       {@code DaftarPengajuanTransfer.simpanJenisKasKecil(this)} bila
 *       {@link #getDaftarPengajuanTransfer()} masih kosong. Method itu membuat satu baris antrean
 *       pembayaran bernama <code>&quot;Saldo awal &lt;nama&gt;&quot;</code> berkode buatan
 *       <code>&quot;SKK-&lt;id&gt;&quot;</code> (katalog ini tidak punya kode dokumen sendiri),
 *       bernominal {@link #getSaldoAwal()}, lalu <b>menulis balik</b> baris antrean itu ke
 *       {@link #setDaftarPengajuanTransfer(DaftarPengajuanTransfer)}. Penyapu terjadwal
 *       {@code SinkronDaftarPengajuanTransferHelper} melakukan hal yang sama untuk baris lama yang
 *       terlewat.</li>
 *   <li>Setelah dana benar-benar ditransfer (proses transfer / transitori), layar
 *       <i>Jurnal Saldo Awal Kas Kecil</i> ({@code PostingJenisKasKecilAction}, juga terjangkau
 *       lewat REST {@code DraftJurnalApiHelper}) menjurnal: <b>DEBET</b> {@link #getAkun()},
 *       <b>KREDIT</b> akun kas/bank atau akun transitori dari
 *       {@code daftarPengajuanTransfer.prosesTransfer.caraPembayaranTransfer}, sebesar
 *       {@link #getSaldoAwal()}, bertanggal {@link #getTanggal()}. Cap {@link #getPostingHistory()}
 *       lalu ditulis ke baris ini.</li>
 *   <li>&quot;Batalkan Posting&quot; menihilkan {@link #setPostingHistory(PostingHistory)} dan
 *       menghapus baris <code>akunting.grup_transaksi</code> yang menunjuk jenis ini lewat SQL
 *       langsung.</li>
 * </ol>
 *
 * <p><b>Konsekuensi yang mudah terlewat:</b> nominal jurnal saldo awal dibaca <i>live</i> dari
 * {@link #getSaldoAwal()}, sedangkan tidak ada satu pun penjaga yang mencegah kolom itu diubah
 * setelah baris ini diposting. Mengubah saldo awal pada baris yang sudah bercap posting membuat
 * angka master dan angka di buku besar berbeda, tanpa peringatan apa pun; hanya siklus
 * batalkan&rarr;posting-ulang yang menyelaraskannya kembali. Perhitungan saldo berjalan
 * ({@code hitungSaldo}) juga langsung ikut berubah secara retroaktif.</p>
 *
 * <h3>Struktur data</h3>
 *
 * <p>Selain dua kolom akun di atas: {@link #getKode()} (kode bebas, dipakai untuk pengurutan dan
 * sebagai kunci layar revisi), {@link #getNama()} (wajib, <code>nullable = false</code>),
 * {@link #getKeterangan()}, {@link #getSatuanKerja()} (unit pemilik &mdash; lihat catatan cakupan
 * di bawah), {@link #getSaldoAwal()}, {@link #getTanggal()} (tanggal saldo awal, dipakai sebagai
 * tanggal jurnal), {@link #getAktif()}, {@link #getPostingHistory()} (cap posting),
 * {@link #getDaftarPengajuanTransfer()} (baris antrean pembayaran), dan
 * {@link #getTanggalTransaksi()} (tanggal realisasi transfer, diturunkan). Kolom jejak audit
 * {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} diisi oleh
 * {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}.</p>
 *
 * <p><b>Yang TIDAK ada di entity ini</b> &mdash; sudah diverifikasi lapangan demi lapangan, karena
 * sering diduga ada: tidak ada kolom <b>pemegang kas</b>/kasir (identitas hanya tersimpan di
 * dokumen {@code KasKecil} lewat alur SOP), tidak ada <b>plafon/pagu/batas replenishment</b>,
 * tidak ada <b>ambang minimum pengisian ulang</b>, tidak ada akumulasi terpakai, dan tidak ada
 * kolom saldo berjalan. Katalog ini <b>tidak menegakkan batas nominal apa pun</b>: saldo dihitung
 * ulang di lapisan action, dan kecukupannya hanya diperiksa oleh layar pengajuan
 * ({@code KasKecilAction}, {@code PenggantianKasKecilAction}), bukan oleh entity ini. Tidak ada
 * pula kolom <b>sekolah</b> maupun <b>yayasan</b> &mdash; satu-satunya sumbu cakupan adalah
 * {@link #getSatuanKerja()}, dan itu pun opsional.</p>
 *
 * <p>Kelas ini <code>extends</code> {@link ais.database.model.GeneralValueObject}. Base class itu
 * <b>bukan</b> <code>@Entity</code> maupun <code>@MappedSuperclass</code> &mdash; hanya POJO
 * abstrak biasa &mdash; sehingga Hibernate tidak memetakan properti apa pun miliknya. Karena itu
 * <code>id</code>, <code>oleh</code>, <code>olehId</code>, dan <code>tanggal_dirubah</code>
 * <b>wajib dideklarasikan ulang di sini</b>; pengulangan itu keharusan teknis, bukan duplikasi
 * yang perlu dirapikan. Yang diwarisi dan benar-benar dipakai adalah util statis
 * {@link ais.database.model.GeneralValueObject#check(Object)} (de-proxy lazy) yang dipanggil tiga
 * getter relasi di sini.</p>
 *
 * <p>Pemetaan memakai <b>property access</b> (anotasi berada di getter), dengan
 * <code>dynamicInsert</code>/<code>dynamicUpdate</code> aktif dan <code>@Audited</code> (Envers)
 * sehingga setiap versi baris digandakan ke tabel revisi <code>jenis_kas_kecil_aud</code> yang
 * ditayangkan tombol revisi pada grid master. Kombinasi property access + Envers berarti nilai
 * yang <i>dikembalikan getter</i>-lah yang tersimpan dan terarsip, bukan nilai mentah field
 * &mdash; relevan untuk {@link #getKode()}/{@link #getNama()} yang melakukan <code>trim()</code>,
 * untuk {@link #getAktif()}/{@link #getSaldoAwal()}/{@link #getTanggal()} yang mengganti
 * <code>null</code> dengan nilai bawaan, dan untuk getter relasi serta
 * {@link #getTanggalTransaksi()} yang menulis balik ke fieldnya.</p>
 *
 * <p><b>Nama kolom.</b> Beberapa properti sengaja tidak dianotasi <code>@Column</code>
 * (<code>kode</code>, <code>aktif</code>, <code>saldoAwal</code>, <code>tanggal</code>,
 * <code>tanggal_dirubah</code>, <code>oleh</code>, <code>olehId</code>). Strategi penamaan proyek
 * adalah {@code ais.database.hibernate.MyNamingStrategy}, turunan
 * {@code org.hibernate.cfg.DefaultNamingStrategy}, yang memakai nama properti <b>apa adanya</b>
 * tanpa mengubah camelCase menjadi <i>snake_case</i>. Jadi kolom fisik untuk
 * <code>saldoAwal</code> adalah <code>saldoawal</code> (PostgreSQL melipat identifier tak berkutip
 * menjadi huruf kecil), bukan <code>saldo_awal</code> &mdash; penting saat menulis SQL native atau
 * skrip migrasi. Bandingkan dengan {@link #getTanggalTransaksi()} yang <i>dianotasi</i>
 * <code>@Column(name = &quot;tanggal_transaksi&quot;)</code> sehingga memang ber-underscore.</p>
 *
 * <h3>Pengelompokan method</h3>
 *
 * <ol>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Cache/bawaan statis</b> &mdash; {@link #DEFAULT_JENIS_KAS_KECIL},
 *       {@link #reloadDefault()}. Keduanya punya kejutan; lihat Javadoc masing-masing.</li>
 *   <li><b>Identitas &amp; deskripsi</b> &mdash; {@link #JenisKasKecil()}, {@link #getId()},
 *       {@link #getKode()}, {@link #getNama()}, {@link #getKeterangan()}, {@link #toString()}
 *       beserta setter-nya.</li>
 *   <li><b>Akun buku besar</b> &mdash; {@link #getAkun()}, {@link #getAkunPenutupKasKecil()}
 *       beserta setter-nya.</li>
 *   <li><b>Nilai &amp; waktu</b> &mdash; {@link #getSaldoAwal()}, {@link #getTanggal()},
 *       {@link #getTanggalTransaksi()} beserta setter-nya.</li>
 *   <li><b>Cakupan &amp; status</b> &mdash; {@link #getSatuanKerja()}, {@link #getAktif()}
 *       beserta setter-nya.</li>
 *   <li><b>Kaitan alur pembayaran &amp; posting</b> &mdash; {@link #getDaftarPengajuanTransfer()},
 *       {@link #getPostingHistory()} beserta setter-nya.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>{@link #reloadDefault()} bukan sekadar memuat cache.</b> Bila tabel kosong, method itu
 *       <b>menulis satu baris ke database</b> (kode <code>&quot;001&quot;</code>, nama
 *       <i>&quot;Kas Kecil&quot;</i>) tanpa akun, tanpa satuan kerja, dan tanpa saldo awal &mdash;
 *       lalu <b>menutup sesi Hibernate</b> thread saat ini. Ia dipanggil saat aplikasi start
 *       ({@code InitData}) <i>dan</i> setiap kali layar master menyimpan. Baris semaian itu
 *       bersatuan-kerja <code>null</code>, sehingga terlihat oleh semua unit, dan berakun
 *       <code>null</code>, sehingga dokumen yang memakainya <b>dilewati diam-diam</b> oleh mesin
 *       posting. Lihat Javadoc method tersebut.</li>
 *   <li><b>{@link #DEFAULT_JENIS_KAS_KECIL} adalah cache mati</b> &mdash; ditulis
 *       {@link #reloadDefault()}, tidak pernah dibaca siapa pun di seluruh repo. Satu-satunya efek
 *       nyata dari {@link #reloadDefault()} adalah penyemaian baris dan penutupan sesi di atas.</li>
 *   <li><b>Cakupan satuan kerja bersifat fail-open.</b> {@code JenisKasKecilAction.initCriteria}
 *       memfilter dengan pola
 *       <code>Restrictions.or(Restrictions.isNull(&quot;satuanKerja&quot;), &hellip;)</code>,
 *       sehingga baris ber-<code>satuanKerja = null</code> <b>selalu terlihat dan selalu dapat
 *       dipilih semua unit</b>. Lebih jauh, bila
 *       {@code SekolahUtil.ambilSatuanKerjas()} mengembalikan himpunan <b>kosong</b> (pengguna
 *       tanpa daftar satuan kerja pada perannya <i>dan</i> tanpa yayasan yang dapat di-resolve),
 *       kriteria jatuh ke <code>sqlRestriction(&quot;1=1&quot;)</code> &mdash; seluruh baris
 *       lintas unit ditampilkan, bukan nol baris. Jalur REST lebih longgar lagi:
 *       {@code MasterKeuanganApiHelper.daftar} membaca <code>public.jenis_kas_kecil</code> dengan
 *       <code>WHERE 1 = 1</code>, <b>tanpa klausa satuan kerja sama sekali</b> dan tanpa
 *       pemeriksaan hak baca.</li>
 *   <li><b>Jalur tulis kedua tanpa gerbang menu (REST).</b> Selain layar ZK
 *       <code>/WEB-INF/z/x/y/pages/master/akunting/jenis_kas_kecil.zul</code>, katalog ini dapat
 *       dibuat/diubah/dihapus lewat {@code PosApi} aksi <code>master_keuangan_*</code> yang
 *       ditangani {@code MasterKeuanganApiHelper}. Tipe <code>&quot;jenis_kas_kecil&quot;</code>
 *       adalah <b>salah satu dari tujuh master keuangan</b> yang dijangkau helper itu
 *       (TERVERIFIKASI di {@code tipeSah()} dan di cabang <code>simpan()</code>), dan lewat jalur
 *       itu {@link #setAkun(Akun)}, {@link #setAkunPenutupKasKecil(Akun)},
 *       {@link #setSatuanKerja(SatuanKerja)}, {@link #setNama(String)}, {@link #setKode(String)},
 *       {@link #setKeterangan(String)}, serta {@link #setAktif(Boolean)} semuanya dapat ditulis.
 *       Penjaganya, <code>bolehAksi()</code>, <b>fail-open</b>: bila {@code Tbmuser.hakAkses()}
 *       mengembalikan <code>null</code> (pengguna tanpa peran) fungsi itu langsung
 *       <code>return true</code> untuk create/update/delete alih-alih menolak. Lihat catatan
 *       keamanan pada {@link #setAkun(Akun)}.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Layar master ini punya menu sendiri
 *       (&quot;Saldo Awal Kas Kecil&quot;, snapshot menu <code>1444590561</code>), tetapi ZUL yang
 *       sama juga <b>disisipkan sebagai tab</b> di halaman dokumen Kas Kecil
 *       ({@code KasKecilAction.onKasKecil} membangun {@code MyInclude} ke
 *       <code>/pages/master/akunting/jenis_kas_kecil.zul</code>), dan layar posting saldo awal
 *       disisipkan ke halaman gabungan <code>uang_muka_dan_kas_kecil.zul</code>. Untuk halaman
 *       ter-<i>include</i>, {@code CommonPrivilages.checkPrevilages()} membaca atribut sesi
 *       <code>currentMenu</code> yang tidak di-resolve ulang &mdash; sehingga hak CREATE/UPDATE/
 *       DELETE atas katalog akun jurnal ini efektif diwarisi dari menu <i>dokumen</i> Kas Kecil,
 *       bukan dari menu masternya sendiri. Ini instance ke sekian dari pola yang sama yang sudah
 *       tercatat pada {@code JenisTransaksi}, {@code Akun}, dan {@code GelombangPendaftaranPsb}.</li>
 *   <li><b>Getter yang menulis balik ke field.</b> {@link #getAkun()},
 *       {@link #getAkunPenutupKasKecil()}, dan {@link #getSatuanKerja()} menugaskan ulang hasil
 *       {@code check(...)} ke fieldnya; {@link #getTanggalTransaksi()} menulis ulang kolom
 *       terpetakan <code>tanggal_transaksi</code> pada <b>setiap</b> pembacaan. Karena pemetaan
 *       property-access + <code>dynamicUpdate</code>, penulisan itu ikut ter-<i>flush</i> bila
 *       entity berada dalam session yang dirty-check &mdash; sekadar <b>membaca</b> daftar bisa
 *       menerbitkan UPDATE. Rincian dan batasannya ada di Javadoc masing-masing getter.</li>
 *   <li><b>Kembaran salin-tempel.</b> {@link #serialVersionUID} kelas ini
 *       (<code>2463821577548439808L</code>) <b>identik</b> dengan milik
 *       {@link ais.database.model.akunting.JenisUangMuka}; kelas itu bahkan masih menyimpan field
 *       statis bernama <code>DEFAULT_JENIS_KAS_KECIL</code> sebagai sisa penyalinan. Kelas
 *       inilah yang asli. Jangan menyimpulkan kekerabatan tipe dari nilai
 *       <code>serialVersionUID</code> di paket ini.</li>
 * </ol>
 *
 * <h3>Siklus hidup di runtime</h3>
 *
 * <p>Saat aplikasi start, {@code InitData} mendaftarkan kelas ini ke pramuat
 * {@code MemoryCacheUtil} lalu memanggil {@link #reloadDefault()}. Setiap penyimpanan lewat layar
 * ZK memanggil {@link #reloadDefault()} lagi; jalur REST <b>tidak</b> memanggilnya. Pembacaan
 * relasi di sini dilayani {@link ais.database.model.GeneralValueObject#check(Object)} yang
 * mengambil dari {@code EntityIdentityMap}/cache lebih dulu dan, sebagai penyelamat terakhir,
 * membuka sesi sendiri untuk memuat ulang objek detached.</p>
 *
 * @see ais.database.model.akunting.KasKecil
 * @see ais.database.model.akunting.PenggantianKasKecil
 * @see ais.database.model.akunting.DaftarPengajuanTransfer
 * @see ais.database.model.akunting.PostingHistory
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.akunting.JenisUangMuka
 * @see ais.database.model.rab.SatuanKerja
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_kas_kecil")
public class JenisKasKecil extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Entity ini melintasi batas serialisasi karena disimpan di sesi ZK (field publik
	 * {@code JenisKasKecilAction.jenisKasKecil}) dan di cache in-memory {@code MemoryCacheUtil}.
	 * Nilai ini <b>tidak boleh diubah</b> selama perubahan struktur masih kompatibel, agar objek
	 * yang sudah terlanjur diserialisasi tetap dapat dibaca kembali.</p>
	 *
	 * <p><b>Catatan:</b> nilai yang sama persis juga dipakai
	 * {@link ais.database.model.akunting.JenisUangMuka} &mdash; sisa salin-tempel, bukan tanda
	 * kekerabatan tipe.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama, di-generate database (kolom <code>id</code>). Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir kali mengubah baris katalog ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}, bukan
	 * oleh layar. Kosong pada baris yang baru dibuat karena tidak ada hook
	 * <code>@PrePersist</code> di kelas ini.</p>
	 *
	 * <p>Tanpa <code>@Column</code>, sehingga nama kolomnya mengikuti nama properti apa adanya
	 * (<code>olehid</code> setelah pelipatan huruf oleh PostgreSQL).</p>
	 *
	 * @return id pengguna pengubah terakhir, atau <code>null</code> bila baris belum pernah
	 *         di-UPDATE lewat sesi yang teridentifikasi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Setter berlogika:</b> argumen <code>null</code> atau berisi spasi saja
	 * <b>diabaikan diam-diam</b> (method langsung <code>return</code> tanpa menyentuh field),
	 * sehingga jejak audit yang sudah ada tidak dapat dihapus lewat setter ini &mdash; termasuk
	 * secara tidak sengaja oleh pengikat data yang mengirim string kosong.</p>
	 *
	 * @param olehId id pengguna pengubah; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: argumen <code>null</code> atau berisi spasi saja
	 * diabaikan tanpa error, sehingga jejak audit tidak dapat dihapus lewat setter.</p>
	 *
	 * @param oleh nama pengguna pengubah; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir kali mengubah baris katalog ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()}. Kosong pada baris yang baru dibuat (tidak ada
	 * hook <code>@PrePersist</code> di kelas ini).</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau <code>null</code>
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA <code>@PreUpdate</code>: menstempel jejak audit tepat sebelum UPDATE dieksekusi.
	 *
	 * <p><b>Tujuan.</b> Menjamin <code>oleh</code>, <code>olehId</code>, dan
	 * <code>tanggal_dirubah</code> selalu terisi pada setiap perubahan baris katalog ini, tanpa
	 * membebani setiap layar untuk melakukannya sendiri.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan sepenuhnya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Interceptor tersebut
	 * lebih dulu bertanya ke {@code AuditTrailHelper.peekUpdateDecision(...)}; bila UPDATE dinilai
	 * tidak membawa perubahan bisnis, penstempelan <b>dilewati</b> sehingga
	 * <code>tanggal_dirubah</code> tidak bergerak karena flush kosong. Identitas diambil dari sesi
	 * web ZK/JSP, atau dari atribut request {@code ATTR_PENGGUNA_POS} bila permintaan datang lewat
	 * {@code PosApi}; bila keduanya tidak ada, terekam sebagai <code>external_update</code>.</p>
	 *
	 * <p><b>Efek samping.</b> Memodifikasi tiga properti entity ini di dalam siklus flush
	 * Hibernate. Tidak menyentuh database secara langsung dan tidak melempar exception.</p>
	 *
	 * <p><b>Kapan dipanggil.</b> Hanya oleh provider JPA/Hibernate, otomatis, pada setiap UPDATE
	 * baris ini &mdash; termasuk UPDATE yang lahir dari getter penulis-balik seperti
	 * {@link #getTanggalTransaksi()}. Tidak pernah dipanggil dari kode aplikasi. <b>Tidak</b>
	 * berjalan pada INSERT: tidak ada <code>@PrePersist</code> pasangannya.</p>
	 *
	 * <p><b>Catatan tentang deklarasi field pada baris yang sama.</b> Field
	 * <code>tanggal_dirubah</code> sengaja dideklarasikan menempel di baris method ini (pola
	 * penyisipan massal di repo ini) dan diinisialisasi ke {@code WaktuUtil.getDate()} saat objek
	 * dibuat, sehingga baris baru sudah punya stempel waktu meski hook ini belum pernah jalan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Penugasan polos tanpa validasi &mdash; berbeda dari {@link #setOleh(String)}/
	 * {@link #setOlehId(String)}, nilai <code>null</code> di sini <b>diterima</b> dan akan
	 * mengosongkan stempel. Dalam praktik hanya dipanggil {@code AuditTimestampInterceptor}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh <code>null</code>
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris katalog ini.
	 *
	 * <p>Sudah terisi sejak objek dibuat (inisialisasi field ke {@code WaktuUtil.getDate()}),
	 * lalu diperbarui {@link #onUpdate()} pada setiap UPDATE yang dinilai membawa perubahan
	 * bisnis. Tanpa <code>@Column</code>: nama kolomnya mengikuti nama properti apa adanya.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah <code>null</code> untuk objek yang baru
	 *         dibuat di memori, tetapi bisa <code>null</code> bila dibaca dari baris lama yang
	 *         kolomnya kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris katalog dalam bentuk <code>&quot;&lt;id&gt;-&lt;nama&gt;&quot;</code>.
	 *
	 * <p><b>Kuirk yang perlu disadari:</b> method ini membaca <b>field</b> <code>nama</code>
	 * mentah, bukan {@link #getNama()}, sehingga hasilnya tidak ter-<code>trim()</code> dan bisa
	 * berbentuk <code>&quot;12-null&quot;</code> untuk baris tanpa nama. Untuk baris yang belum
	 * tersimpan, <code>id</code> juga masih <code>null</code> sehingga hasilnya
	 * <code>&quot;null-...&quot;</code>.</p>
	 *
	 * <p><b>Kapan terlihat pengguna.</b> Dipakai komponen ZK yang menampilkan objek apa adanya
	 * (mis. isi banbox/combo sebelum renderer khusus mengambil alih) dan pada keluaran diagnostik.
	 * Layar master sendiri tidak memakainya &mdash; renderernya menyusun kolom satu per satu.</p>
	 *
	 * @return gabungan id dan nama mentah, dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Cache statis &quot;jenis kas kecil bawaan&quot; &mdash; <b>ditulis tetapi tidak pernah
	 * dibaca</b>.
	 *
	 * <p>Diisi {@link #reloadDefault()} dengan baris ber-<code>id</code> terkecil. Penelusuran
	 * seluruh repo menunjukkan <b>nol pembaca</b>: tidak ada satu pun Action, Helper, laporan,
	 * atau JSP yang mengambil nilai field ini. Nilai gunanya karena itu nihil; yang benar-benar
	 * berdampak dari {@link #reloadDefault()} adalah <i>efek sampingnya</i> (penyemaian baris ke
	 * database dan penutupan sesi Hibernate), bukan isian field ini.</p>
	 *
	 * <p><b>Bahaya yang melekat:</b> field ini <code>public static</code> dan <b>tidak</b>
	 * <code>final</code>, memegang entity Hibernate yang <i>detached</i> setelah sesi
	 * penyemainya ditutup. Bila kelak ada kode yang mulai membacanya, akses lintas-thread ke satu
	 * objek entity bersama dan potensi {@code LazyInitializationException} pada relasinya harus
	 * ditangani lebih dulu (mis. lewat {@link ais.database.model.GeneralValueObject#check(Object)}).
	 * Kembarannya di {@link ais.database.model.akunting.JenisUangMuka} bahkan masih memakai
	 * <i>nama</i> yang sama karena disalin dari sini.</p>
	 *
	 * @see #reloadDefault()
	 */
	public static JenisKasKecil DEFAULT_JENIS_KAS_KECIL = null;

	/**
	 * Memuat ulang cache {@link #DEFAULT_JENIS_KAS_KECIL} dan &mdash; ini bagian pentingnya &mdash;
	 * <b>menyemai satu baris katalog ke database bila tabelnya masih kosong</b>.
	 *
	 * <p><b>Tujuan.</b> Menjamin instalasi baru selalu punya minimal satu jenis kas kecil sehingga
	 * layar dokumen Kas Kecil tidak menghadapkan pengguna pada dropdown kosong.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Mengambil sesi <i>native</i> ThreadLocal lewat
	 *       {@link ais.database.hibernate.HibernateUtil#currentNativeSession()}.</li>
	 *   <li>Mengambil satu baris <code>ORDER BY id ASC LIMIT 1</code> dan menyimpannya ke
	 *       {@link #DEFAULT_JENIS_KAS_KECIL}.</li>
	 *   <li>Bila tidak ada baris sama sekali, membuat objek baru berisi kode
	 *       <code>&quot;001&quot;</code>, aktif <code>true</code>, nama dan keterangan
	 *       <i>&quot;Kas Kecil&quot;</i>, lalu <code>save</code>-nya dalam transaksi
	 *       eksplisit.</li>
	 *   <li>Menutup sesi ThreadLocal lewat {@code HibernateUtil.closeSession()}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Efek samping &mdash; empat hal yang harus disadari.</b></p>
	 * <ol>
	 *   <li><b>Menulis ke database.</b> Ini bukan method &quot;muat cache&quot; yang bersih; ia
	 *       melakukan INSERT. Karena itu ia tidak boleh dipanggil dari jalur baca-saja atau dari
	 *       laporan.</li>
	 *   <li><b>Baris semaian lahir tidak lengkap.</b> {@link #setAkun(Akun)},
	 *       {@link #setAkunPenutupKasKecil(Akun)}, {@link #setSatuanKerja(SatuanKerja)},
	 *       {@link #setSaldoAwal(Double)}, dan {@link #setTanggal(Date)} <b>tidak</b> dipanggil.
	 *       Akibatnya: (a) tanpa akun, dokumen kas kecil yang memakainya <b>dilewati diam-diam</b>
	 *       oleh mesin posting karena setiap layar posting mensyaratkan
	 *       <code>akunDebet != null &amp;&amp; akunKredit != null</code>; (b) tanpa satuan kerja,
	 *       baris ini lolos filter <code>isNull(&quot;satuanKerja&quot;)</code> di semua layar,
	 *       jadi <b>terlihat dan dapat dipilih oleh seluruh unit</b>; (c) tanpa saldo awal,
	 *       {@code hitungSaldo} selalu memulai dari nol.</li>
	 *   <li><b>Menutup sesi Hibernate thread saat ini.</b> {@code HibernateUtil.closeSession()}
	 *       melepas dan menutup sesi ThreadLocal. {@code currentNativeSession()} sendiri
	 *       mengingatkan agar <b>tidak dipakai di konteks request ZK</b>, namun
	 *       {@code JenisKasKecilAction.onSave} tetap memanggil method ini di tengah request
	 *       setelah menyimpan &mdash; pola yang sudah lama berjalan dan sengaja dibiarkan, tetapi
	 *       perlu diketahui bila muncul gejala sesi tertutup pada langkah berikutnya.</li>
	 *   <li><b>Transaksi tidak dibungkus try/finally.</b> Bila <code>save</code> gagal,
	 *       <code>commit()</code> tidak tercapai dan <code>closeSession()</code> di baris terakhir
	 *       juga tidak tercapai &mdash; exception merambat ke pemanggil dengan sesi masih
	 *       menggantung.</li>
	 * </ol>
	 *
	 * <p><b>Kapan dipanggil.</b> Dua tempat: {@code InitData.reloadDefaults()} saat aplikasi start,
	 * dan {@code JenisKasKecilAction.onSave} setiap kali baris katalog disimpan lewat layar ZK.
	 * Jalur REST {@code MasterKeuanganApiHelper} <b>tidak</b> memanggilnya, jadi
	 * {@link #DEFAULT_JENIS_KAS_KECIL} bisa basi setelah perubahan lewat REST &mdash; tanpa akibat
	 * praktis, karena tidak ada yang membacanya.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		DEFAULT_JENIS_KAS_KECIL = (JenisKasKecil) session.createCriteria(JenisKasKecil.class).setMaxResults(1)
				.addOrder(Order.asc("id")).uniqueResult();
		if (DEFAULT_JENIS_KAS_KECIL == null) {
			DEFAULT_JENIS_KAS_KECIL = new JenisKasKecil();
			DEFAULT_JENIS_KAS_KECIL.setKode("001");
			DEFAULT_JENIS_KAS_KECIL.setAktif(true);
			DEFAULT_JENIS_KAS_KECIL.setNama("Kas Kecil");
			DEFAULT_JENIS_KAS_KECIL.setKeterangan("Kas Kecil");
			session.getTransaction().begin();
			session.save(DEFAULT_JENIS_KAS_KECIL);
			session.getTransaction().commit();
		}
		HibernateUtil.closeSession();
	}

	/** Kode bebas katalog; dipakai untuk pengurutan dan kunci layar revisi. Lihat {@link #getKode()}. */
	private String kode;

	/** Akun buku besar kas kecil (dompet). Lihat {@link #getAkun()}. */
	private Akun akun;

	/** Akun lawan saat dompet ditutup/dinihilkan. Lihat {@link #getAkunPenutupKasKecil()}. */
	private Akun akunPenutupKasKecil;

	/** Nama jenis kas kecil; wajib di level kolom. Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Unit pemilik dompet; opsional dan berperilaku fail-open. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/** Nominal dana awal dompet, dasar seluruh perhitungan saldo. Lihat {@link #getSaldoAwal()}. */
	private Double saldoAwal;

	/** Tanggal saldo awal; dipakai sebagai tanggal jurnal. Lihat {@link #getTanggal()}. */
	private Date tanggal;

	/** Penanda aktif; <code>null</code> diperlakukan sebagai aktif. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Cap posting jurnal saldo awal. Lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;

	/** Baris antrean pembayaran saldo awal. Lihat {@link #getDaftarPengajuanTransfer()}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;

	/** Tanggal realisasi transfer, diturunkan saat dibaca. Lihat {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi;

	/**
	 * Konstruktor tanpa argumen.
	 *
	 * <p>Wajib ada untuk Hibernate (instansiasi entity saat memuat baris) dan dipakai kode
	 * aplikasi saat menambah data baru ({@code JenisKasKecilAction.onAdd},
	 * {@code MasterKeuanganApiHelper.simpan}, serta {@link #reloadDefault()}). Tidak menyetel
	 * nilai bawaan apa pun kecuali inisialisasi field <code>tanggal_dirubah</code> yang
	 * dideklarasikan bersama {@link #onUpdate()}.</p>
	 */
	public JenisKasKecil() {
	}

	/**
	 * Kunci utama baris katalog.
	 *
	 * <p>Dipetakan <code>IDENTITY</code> dengan <code>insertable = false</code>, sehingga nilainya
	 * sepenuhnya ditentukan sequence database dan tidak dapat dipaksakan dari aplikasi. Nilainya
	 * ikut menyusun kunci jurnal {@code GrupTransaksi.ambilUnik()}
	 * (<code>ais.database.model.akunting.JenisKasKecil_&lt;id&gt;</code>) dan kode buatan
	 * <code>&quot;SKK-&lt;id&gt;&quot;</code> pada {@code DaftarPengajuanTransfer.getKode()}.</p>
	 *
	 * @return id baris; <code>null</code> untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Penugasan polos.
	 *
	 * <p>Praktisnya hanya dipanggil Hibernate saat memuat/menyimpan baris; kode aplikasi tidak
	 * boleh menyetel id sendiri karena kolomnya <code>insertable = false</code>.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode katalog, sudah ter-<code>trim()</code>.
	 *
	 * <p><b>Getter berlogika ringan:</b> mengembalikan string kosong (bukan <code>null</code>)
	 * bila field kosong, sehingga pemanggil tidak perlu menjaga NPE. Nilai mentah field
	 * <b>tidak</b> diubah &mdash; berbeda dari getter relasi di bawah, method ini tidak menulis
	 * balik.</p>
	 *
	 * <p><b>Kapan dipakai.</b> Pengurutan daftar master (<code>ORDER BY kode ASC</code>),
	 * pencarian <code>ILIKE</code> di layar master, kunci tombol revisi Envers pada grid, dan
	 * keterangan jurnal saldo awal (<i>&quot;Saldo awal kas kecil &quot;&lt;kode&gt;&quot;
	 * &hellip;&quot;</i>). Tidak ada jaminan keunikan: tidak ada <code>unique</code> constraint
	 * maupun validasi di layar, sehingga dua baris berkode sama diperbolehkan.</p>
	 *
	 * <p>Karena pemetaan property-access, nilai <b>hasil trim inilah</b> yang tersimpan dan
	 * terarsip Envers, bukan nilai mentah yang disetel layar.</p>
	 *
	 * @return kode yang sudah dirapikan; string kosong bila belum diisi, tidak pernah
	 *         <code>null</code>
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode katalog. Penugasan polos tanpa trim maupun validasi keunikan.
	 *
	 * @param kode kode baru; boleh <code>null</code> atau kosong (layar master tidak
	 *             mewajibkannya)
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama jenis kas kecil, sudah ter-<code>trim()</code>.
	 *
	 * <p><b>Perhatikan perbedaan dengan {@link #getKode()}:</b> di sini <code>null</code>
	 * dikembalikan apa adanya sebagai <code>null</code> (bukan string kosong), sehingga pemanggil
	 * tetap harus berjaga &mdash; dan itulah sebab {@link #toString()} bisa menghasilkan
	 * <code>&quot;12-null&quot;</code>.</p>
	 *
	 * <p>Kolomnya dipetakan <code>nullable = false</code> sehingga baris tanpa nama ditolak
	 * database; layar master juga memvalidasi nama tidak boleh kosong, begitu pula
	 * {@code MasterKeuanganApiHelper.simpan}. Nama ini muncul di keterangan jurnal saldo awal, di
	 * nama baris antrean pembayaran (<i>&quot;Saldo awal &lt;nama&gt;&quot;</i>), di grid master,
	 * dan di dasbor monitor kas kecil.</p>
	 *
	 * @return nama yang sudah dirapikan, atau <code>null</code> bila field kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis kas kecil. Penugasan polos tanpa trim maupun validasi.
	 *
	 * <p>Validasi &quot;nama wajib diisi&quot; berada di pemanggil
	 * ({@code JenisKasKecilAction.onSave} dan {@code MasterKeuanganApiHelper.simpan}), bukan di
	 * sini; entity ini hanya dijaga oleh constraint <code>nullable = false</code> pada kolom.</p>
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk baris katalog.
	 *
	 * <p>Getter polos: nilai dikembalikan apa adanya, tanpa trim dan tanpa normalisasi
	 * <code>null</code>. Hanya ditampilkan (grid master, daftar REST) dan ikut dicari pada
	 * pencarian REST; tidak dipakai logika apa pun.</p>
	 *
	 * @return keterangan, atau <code>null</code>
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Penugasan polos.
	 *
	 * @param keterangan keterangan baru; boleh <code>null</code>
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Penanda apakah baris katalog masih dipakai.
	 *
	 * <p><b>Getter berlogika:</b> <code>null</code> diperlakukan sebagai <b><code>true</code></b>.
	 * Ini penting karena layar master ZK <b>tidak pernah</b> memanggil
	 * {@link #setAktif(Boolean)} saat menyimpan formulir &mdash; baris baru dari layar lahir
	 * dengan <code>aktif = null</code> di memori, dan karena <code>dynamicInsert</code> aktif,
	 * kolomnya bahkan tidak ikut disertakan dalam INSERT sehingga nilai DEFAULT database (bila
	 * ada) yang berlaku.</p>
	 *
	 * <p><b>Penafsiran <code>null</code> konsisten di seluruh konsumen yang diperiksa</b> (berbeda
	 * dari {@link ais.database.model.akunting.JenisUangMuka} yang penafsirannya bercabang): filter
	 * layar master memakai <code>isNull OR eq(true)</code>, {@code MasterKeuanganApiHelper}
	 * memakai <code>COALESCE(aktif, true)</code>, dan getter ini mengembalikan <code>true</code>.
	 * Jadi baris tanpa nilai eksplisit tetap terlihat dan terpilih di mana-mana.</p>
	 *
	 * <p>Nilai boolean sungguhan baru tertulis bila pengguna menoggle checkbox &quot;Aktif&quot;
	 * pada grid master (yang menyimpan langsung per baris) atau lewat jalur REST.</p>
	 *
	 * @return <code>true</code> bila aktif atau belum ditentukan; <code>false</code> hanya bila
	 *         dinonaktifkan secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif. Penugasan polos; <code>null</code> diterima dan berarti
	 * &quot;belum ditentukan&quot; (dibaca sebagai aktif oleh {@link #getAktif()}).
	 *
	 * <p>Menonaktifkan baris <b>tidak</b> memutus dokumen yang sudah menunjuknya: relasi
	 * {@code KasKecil.jenisKasKecil} tetap utuh, jurnal yang sudah terbentuk tetap ada, dan
	 * {@code hitungSaldo} tetap menghitung dompet ini. Efeknya murni menyembunyikan baris dari
	 * daftar dan dropdown pemilihan.</p>
	 *
	 * @param aktif penanda aktif baru; boleh <code>null</code>
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Akun buku besar kas kecil (&quot;dompet&quot;-nya) &mdash; label layar
	 * <i>&quot;Akun Kas Kecil *&quot;</i>, wajib diisi.
	 *
	 * <p><b>Peran di jurnal (terverifikasi dari layar posting):</b></p>
	 * <ul>
	 *   <li><b>DEBET</b> pada jurnal saldo awal ({@code PostingJenisKasKecilAction}), jurnal
	 *       penggantian/pengisian ulang ({@code PostingPenggantianKasKecilAction}), dan jurnal kas
	 *       besar yang mendanai kas kecil ({@code PostingKasBesarAction}).</li>
	 *   <li><b>KREDIT</b> pada jurnal pemakaian kas kecil ({@code PostingKasKecilAction}) &mdash;
	 *       dompet berkurang, akun biaya dari {@code KasKecil.formula} yang didebet.</li>
	 * </ul>
	 *
	 * <p>Akun ini juga menjadi sumber informasi rekening tujuan transfer: {@code
	 * DaftarPengajuanTransfer} membaca <code>akun.getBank()</code>, <code>getNoRek()</code>, dan
	 * <code>getAtasNama()</code> dari sini untuk baris antrean pembayaran saldo awal maupun
	 * penggantian kas kecil.</p>
	 *
	 * <p><b>Getter menulis balik ke field.</b> Hasil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} ditugaskan kembali ke
	 * <code>akun</code> sebelum dikembalikan. {@code check(...)} me-resolve proxy lazy yang mungkin
	 * sudah <i>detached</i> &mdash; mengambil dari {@code EntityIdentityMap}/cache bila ada, atau
	 * memuat ulang lewat sesi sendiri sebagai penyelamat terakhir &mdash; dan mengembalikan
	 * instance <b>ber-id sama</b>, sehingga nilai bisnisnya tidak berubah. Yang berubah adalah
	 * <i>referensi objek</i>: karena pemetaan property-access dengan <code>dynamicUpdate</code>,
	 * penggantian referensi itu dapat membuat Hibernate menganggap properti ini kotor dan
	 * menerbitkan UPDATE pada flush berikutnya. Jangan menambah logika pemilihan akun (mis. pola
	 * <code>akunOver</code> pada {@code Transaksi}) di sini &mdash; di posisi ini, penulisan balik
	 * akan memindahkan atribusi akun buku besar hanya karena baris dibaca.</p>
	 *
	 * <p><b>Bila kosong:</b> tidak ada exception. Setiap layar posting mensyaratkan
	 * <code>akunDebet != null &amp;&amp; akunKredit != null</code>, sehingga dokumen yang memakai
	 * jenis tanpa akun <b>dilewati diam-diam</b> &mdash; tidak terjurnal, tanpa pesan galat di
	 * dokumennya. {@code MasterKeuanganApiHelper.daftar} menandai kondisi ini lewat bendera
	 * <code>akunLengkap</code> agar admin melihatnya lebih dulu.</p>
	 *
	 * @return akun kas kecil yang sudah di-resolve, atau <code>null</code> bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel akun buku besar kas kecil. Penugasan polos tanpa validasi.
	 *
	 * <p><b>Dampak akuntansi.</b> Mengubah nilai ini <b>mengubah akun buku besar seluruh dokumen
	 * kas kecil yang belum diposting</b> yang menunjuk baris katalog ini, karena mesin posting
	 * membaca akun secara <i>live</i> saat tombol Posting ditekan &mdash; tidak ada potret akun
	 * yang disimpan di dokumen. Jurnal yang sudah terbentuk tidak ikut berubah, sehingga satu
	 * dompet bisa berakhir tersebar di dua akun buku besar bila kolom ini diubah di tengah
	 * periode.</p>
	 *
	 * <p><b>Catatan keamanan (broken access control).</b> Selain layar ZK, setter ini terjangkau
	 * dari REST {@code PosApi} aksi <code>master_keuangan_simpan</code> yang ditangani
	 * {@code MasterKeuanganApiHelper} untuk tipe <code>&quot;jenis_kas_kecil&quot;</code> &mdash;
	 * salah satu dari tujuh master keuangan yang dilayani helper itu. Penjaganya,
	 * <code>bolehAksi()</code>, <b>fail-open</b>: bila {@code Tbmuser.hakAkses()} mengembalikan
	 * <code>null</code>, fungsi itu <code>return true</code> untuk create/update/delete alih-alih
	 * menolak. Artinya pengguna terautentikasi tanpa peran dapat mengarahkan ulang akun buku besar
	 * kas kecil lewat REST. Sudah dilacak sebagai temuan audit yang berlaku untuk seluruh keluarga
	 * master keuangan; jangan buat catatan terpisah untuk entity ini.</p>
	 *
	 * @param akun akun kas kecil baru; boleh <code>null</code>, dengan akibat dokumen tidak
	 *             terjurnal (lihat {@link #getAkun()})
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Unit kerja pemilik dompet kas kecil &mdash; label layar <i>&quot;Satuan Kerja *&quot;</i>.
	 *
	 * <p>Dipakai sebagai sumbu cakupan pada daftar master, dan sebagai <b>satuan kerja jurnal</b>
	 * cadangan: {@code GrupTransaksi.getSatuanKerja()} dan {@code KasKecil.getSatuanKerja()}
	 * menurunkan unit dari sini bila dokumen tidak menyebutkannya sendiri, begitu pula
	 * {@code DaftarPengajuanTransfer.getSatuanKerja()} untuk baris antrean saldo awal.</p>
	 *
	 * <p><b>Getter menulis balik ke field</b> lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}; berlaku catatan yang sama
	 * seperti pada {@link #getAkun()}.</p>
	 *
	 * <p><b>Cakupan bersifat fail-open &mdash; ini kolom yang paling mudah disalahpahami.</b>
	 * Meski layar menandai field ini wajib, kolomnya <code>nullable = true</code> dan jalur REST
	 * membolehkan kosong. Baris ber-<code>satuanKerja = null</code> lolos klausa
	 * <code>Restrictions.or(Restrictions.isNull(&quot;satuanKerja&quot;), &hellip;)</code> di
	 * {@code JenisKasKecilAction.initCriteria} sehingga <b>terlihat dan dapat dipilih oleh semua
	 * unit</b>. Lebih jauh, bila {@code SekolahUtil.ambilSatuanKerjas()} mengembalikan himpunan
	 * kosong &mdash; pengguna tanpa daftar satuan kerja pada perannya dan tanpa yayasan yang dapat
	 * di-resolve &mdash; kriteria jatuh ke <code>sqlRestriction(&quot;1=1&quot;)</code>, yaitu
	 * seluruh baris lintas unit, bukan nol baris. Jalur REST
	 * {@code MasterKeuanganApiHelper.daftar} tidak memfilter satuan kerja sama sekali. Entity ini
	 * juga tidak punya kolom sekolah maupun yayasan, jadi tidak ada sumbu cakupan kedua yang bisa
	 * menyelamatkan.</p>
	 *
	 * @return satuan kerja pemilik yang sudah di-resolve, atau <code>null</code> untuk katalog
	 *         berlaku-global
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel unit kerja pemilik. Penugasan polos tanpa validasi.
	 *
	 * <p>Menyetel <code>null</code> mengubah baris ini menjadi katalog berlaku-global bagi seluruh
	 * unit &mdash; lihat penjelasan fail-open pada {@link #getSatuanKerja()}. {@link #reloadDefault()}
	 * tidak pernah memanggil setter ini, sehingga baris semaian otomatis selalu global.</p>
	 *
	 * @param satuanKerja unit pemilik baru; boleh <code>null</code>
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Nominal dana awal dompet kas kecil &mdash; label layar
	 * <i>&quot;Saldo Awal Kas Kecil *&quot;</i>, wajib diisi di layar ZK.
	 *
	 * <p><b>Getter berlogika ringan:</b> <code>null</code> dikembalikan sebagai <code>0.0</code>
	 * sehingga pemanggil dapat langsung berhitung. Field mentah tidak diubah.</p>
	 *
	 * <p><b>Ini satu-satunya angka permanen milik dompet.</b> Tiga konsumen membacanya:</p>
	 * <ol>
	 *   <li>{@code JenisKasKecilAction.hitungSaldo(...)} sebagai titik awal
	 *       <code>saldoAwal &minus; &Sigma;KasKecil.nilai</code> untuk pengajuan yang belum
	 *       diganti dan masih aktif &mdash; saldo berjalan <b>tidak pernah disimpan</b>, selalu
	 *       dihitung ulang;</li>
	 *   <li>{@code PostingJenisKasKecilAction} sebagai <b>nominal jurnal</b> saldo awal;</li>
	 *   <li>{@code DaftarPengajuanTransfer.getNominal()} sebagai nominal baris antrean pembayaran
	 *       saldo awal.</li>
	 * </ol>
	 *
	 * <p><b>Kuirk arah jurnal.</b> {@code PostingJenisKasKecilAction} membalik posisi debet dan
	 * kredit bila nilainya <code>&lt;= 0.1</code> (cabang <code>else</code> memanggil
	 * {@code saveTransaksi} dengan urutan akun tertukar) dan tetap memakai nominal aslinya. Saldo
	 * awal nol atau negatif karena itu <b>tetap menerbitkan baris jurnal</b>, hanya terbalik
	 * arahnya. Tidak ada validasi yang menolak nilai negatif di layar maupun di REST.</p>
	 *
	 * <p><b>Tidak ada penjaga pasca-posting.</b> Kolom ini tetap dapat diubah setelah baris
	 * bercap {@link #getPostingHistory()}; nilai master dan angka di buku besar lalu berbeda tanpa
	 * peringatan, dan hasil {@code hitungSaldo} ikut berubah secara retroaktif. Tanpa
	 * <code>@Column</code>, nama kolom fisiknya <code>saldoawal</code> (bukan
	 * <code>saldo_awal</code>) &mdash; lihat catatan penamaan di Javadoc kelas.</p>
	 *
	 * @return saldo awal dompet; <code>0.0</code> bila belum diisi, tidak pernah <code>null</code>
	 */
	public Double getSaldoAwal() {
		return saldoAwal == null ? 0.0 : saldoAwal;
	}

	/**
	 * Menyetel nominal dana awal dompet. Penugasan polos tanpa validasi tanda maupun batas.
	 *
	 * <p>Layar ZK hanya memastikan field tidak kosong; nilai nol dan negatif diterima. Jalur REST
	 * {@code MasterKeuanganApiHelper} <b>tidak</b> menyentuh kolom ini sama sekali, sehingga saldo
	 * awal hanya dapat diubah dari layar ZK.</p>
	 *
	 * @param saldoAwal nominal saldo awal baru; boleh <code>null</code> (dibaca sebagai
	 *                  <code>0.0</code>)
	 */
	public void setSaldoAwal(Double saldoAwal) {
		this.saldoAwal = saldoAwal;
	}

	/**
	 * Tanggal berlakunya saldo awal &mdash; label layar
	 * <i>&quot;Tanggal Saldo Awal *&quot;</i> (datebox readonly, hanya lewat pemilih tanggal).
	 *
	 * <p><b>Getter berlogika ringan:</b> bila field kosong dikembalikan
	 * <code>new Date()</code>, yaitu <b>waktu saat pembacaan</b>. Field mentah tidak diubah,
	 * sehingga nilai kembalian bisa berbeda pada dua pembacaan berturut-turut untuk baris yang
	 * kolom tanggalnya kosong &mdash; hindari mengandalkannya untuk perbandingan yang harus
	 * stabil.</p>
	 *
	 * <p><b>Dipakai sebagai tanggal jurnal.</b> {@code PostingJenisKasKecilAction} meneruskan
	 * nilai ini ke {@code CommonAkunting.saveTransaksi} sebagai tanggal transaksi jurnal saldo
	 * awal, dan {@code DaftarPengajuanTransfer.getWaktu()} memakainya sebagai waktu baris antrean
	 * pembayaran. Karena itu tanggal ini menentukan <b>periode buku</b> tempat saldo awal
	 * mendarat.</p>
	 *
	 * @return tanggal saldo awal; waktu sekarang bila kolomnya kosong, tidak pernah
	 *         <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	/**
	 * Menyetel tanggal saldo awal. Penugasan polos; <code>null</code> diterima (dibaca sebagai
	 * waktu sekarang oleh {@link #getTanggal()}).
	 *
	 * <p>Mengubah tanggal ini setelah baris diposting <b>tidak</b> memindahkan jurnal yang sudah
	 * terbentuk; hanya siklus batalkan&rarr;posting-ulang yang melakukannya. Nilai ini juga
	 * menjadi tanggal acuan cadangan bagi {@link #getTanggalTransaksi()}.</p>
	 *
	 * @param tanggal tanggal saldo awal baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Cap posting jurnal saldo awal &mdash; penanda bahwa baris katalog ini sudah masuk buku
	 * besar.
	 *
	 * <p>Getter polos (tanpa <code>check(...)</code>, tanpa penulisan balik); relasi ini dipetakan
	 * <code>@Fetch(FetchMode.SELECT)</code> tanpa <code>fetch = LAZY</code> eksplisit sehingga
	 * dimuat lewat SELECT terpisah.</p>
	 *
	 * <p><b>Cara membacanya.</b> <code>null</code> berarti <i>belum diposting</i>; bukan
	 * <code>null</code> berarti sudah. Layar <i>Jurnal Saldo Awal Kas Kecil</i> memilih baris yang
	 * akan diposting persis dengan <code>Restrictions.isNull(&quot;postingHistory&quot;)</code>,
	 * dan &quot;Batalkan Posting&quot; menihilkannya kembali sambil menghapus baris
	 * <code>akunting.grup_transaksi</code> yang menunjuk jenis ini. Sesuai temuan pada
	 * {@link ais.database.model.akunting.PostingHistory}, satu baris cap dapat dipakai bersama
	 * banyak dokumen dalam satu batch posting, dan kolom <code>posting</code> di dalamnya
	 * <b>bukan</b> penanda &quot;sudah dijurnal&quot; &mdash; keberadaan cap inilah penandanya.</p>
	 *
	 * @return cap posting, atau <code>null</code> bila saldo awal belum dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel (atau menihilkan) cap posting jurnal saldo awal. Penugasan polos.
	 *
	 * <p><b>Dipanggil dari:</b> {@code PostingJenisKasKecilAction} &mdash; diisi setelah
	 * {@code CommonAkunting.saveTransaksi} berhasil membentuk jurnal, dan dinihilkan pada
	 * &quot;Batalkan Posting&quot; (baik per baris maupun massal) tepat sebelum baris
	 * {@code GrupTransaksi} terkait dihapus lewat SQL native. Jalur REST
	 * {@code DraftJurnalApiHelper} memanggil method statis posting/batal yang sama.</p>
	 *
	 * <p><b>Peringatan integritas.</b> Menihilkan cap ini <i>tanpa</i> menghapus jurnalnya akan
	 * membuat baris ini muncul lagi sebagai &quot;belum diposting&quot; dan dapat dijurnal ulang
	 * &mdash; saldo awal masuk buku besar dua kali. Urutan yang benar (nihilkan cap lalu hapus
	 * <code>grup_transaksi</code>) hanya ditegakkan oleh kode layar posting, bukan oleh entity
	 * ini.</p>
	 *
	 * @param postingHistory cap posting baru, atau <code>null</code> untuk menandai belum
	 *                       diposting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Baris antrean pembayaran untuk penyerahan dana saldo awal dompet ini.
	 *
	 * <p>Getter polos (tanpa <code>check(...)</code>), dimuat lewat SELECT terpisah.</p>
	 *
	 * <p><b>Kenapa data master punya baris antrean pembayaran.</b> Saat sebuah jenis kas kecil
	 * dibentuk, dana pertamanya harus benar-benar ditransfer ke pemegang kas. Karena katalog ini
	 * tidak punya dokumen pengajuan sendiri, {@code DaftarPengajuanTransfer.simpanJenisKasKecil(...)}
	 * membuatkan satu baris antrean bernama <i>&quot;Saldo awal &lt;nama&gt;&quot;</i>, berkode
	 * buatan <code>&quot;SKK-&lt;id&gt;&quot;</code>, bernominal {@link #getSaldoAwal()}, lalu
	 * menulis balik baris itu ke sini. Pemicunya ada dua: timer sekali-tembak di
	 * {@code JenisKasKecilAction.onSave} (hanya bila kolom ini masih kosong) dan penyapu
	 * terjadwal {@code SinkronDaftarPengajuanTransferHelper} untuk baris lama yang terlewat.</p>
	 *
	 * <p><b>Konsekuensi untuk posting.</b> Akun KREDIT jurnal saldo awal <b>hanya</b> dapat
	 * diperoleh dari rantai
	 * <code>daftarPengajuanTransfer.prosesTransfer.caraPembayaranTransfer</code> (akun transitori
	 * bila transfer bersifat transitori, akun kas/bank bila transfer biasa). Selama dana belum
	 * benar-benar diproses transfer, akun kredit bernilai <code>null</code> dan baris ini
	 * <b>dilewati diam-diam</b> oleh mesin posting. Renderer layar posting bahkan memanggil
	 * <code>getDaftarPengajuanTransfer().getTransitori()</code> tanpa penjaga <code>null</code>,
	 * sehingga baris tanpa antrean akan gagal dirender &mdash; itulah sebabnya kriteria layarnya
	 * mensyaratkan keterkaitan proses transfer.</p>
	 *
	 * @return baris antrean pembayaran saldo awal, atau <code>null</code> bila belum dibentuk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menautkan baris antrean pembayaran saldo awal ke katalog ini. Penugasan polos.
	 *
	 * <p><b>Dipanggil dari:</b> {@code DaftarPengajuanTransfer.simpanJenisKasKecil(...)} sebagai
	 * langkah penulisan balik setelah baris antrean tersimpan. Method itu juga bertindak sebagai
	 * penjaga idempoten: bila kolom ini sudah terisi, ia langsung <code>return</code> tanpa
	 * membuat baris kedua.</p>
	 *
	 * @param daftarPengajuanTransfer baris antrean pembayaran; boleh <code>null</code>
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Tanggal realisasi dana saldo awal &mdash; <b>diturunkan ulang dari alur transfer setiap kali
	 * dibaca</b>, bukan sekadar dibaca dari kolom.
	 *
	 * <p><b>Cara kerja.</b> Tiga cabang berurutan, yang pertama cocok yang dipakai:</p>
	 * <ol>
	 *   <li>Bila antrean pembayaran bersifat <b>transitori</b> dan punya data transitori beserta
	 *       proses transitorinya &rarr; tanggal pembuatan proses transitori.</li>
	 *   <li>Bila antrean punya <b>proses transfer</b> &rarr; tanggal realisasi transfer, atau
	 *       tanggal pembuatannya bila belum direalisasikan.</li>
	 *   <li>Selain itu &rarr; {@link #getTanggal()}, yaitu tanggal saldo awal (yang untuk baris
	 *       berkolom tanggal kosong berarti <b>waktu saat pembacaan</b>).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping &mdash; getter destruktif.</b> Nilai hasil turunan itu <b>ditulis kembali
	 * ke field</b> <code>tanggalTransaksi</code>, dan field itu dipetakan ke kolom sungguhan
	 * <code>@Column(name = &quot;tanggal_transaksi&quot;)</code>. Dengan property-access +
	 * <code>dynamicUpdate</code>, sekadar <b>membaca</b> baris ini di dalam session yang
	 * dirty-check dapat menerbitkan UPDATE kolom tersebut &mdash; termasuk saat cabang ketiga
	 * yang menyalin waktu-sekarang. Kolom ini karena itu bukan data yang stabil untuk dipakai
	 * sebagai kriteria historis; perlakukan sebagai cache tampilan.</p>
	 *
	 * <p><b>Penelan {@code LazyInitializationException}.</b> Seluruh badan method dibungkus
	 * <code>try/catch</code> bertanda <code>auto-audit(empty-catch)</code>. Alasannya tertulis di
	 * komentar kode: {@code daftarPengajuanTransfer} bisa berupa instance canonical/berbagi dari
	 * {@code AuditTimestampInterceptor} yang rantai proxy-nya terikat ke Session lain yang sudah
	 * ditutup. Bila itu terjadi, penurunan dilewati dan nilai <i>fallback</i> yang sudah ada di
	 * field dipertahankan &mdash; halaman tetap ter-render, tetapi tanggal yang tampil bisa
	 * ketinggalan dari keadaan transfer sebenarnya. Perhatikan bahwa cabang-cabang di dalamnya
	 * memanggil <code>getDaftarPengajuanTransfer()</code> untuk penjagaan <code>null</code>
	 * kemudian mengakses <b>field</b> <code>daftarPengajuanTransfer</code> secara langsung; karena
	 * getter itu polos, keduanya menunjuk objek yang sama.</p>
	 *
	 * @return tanggal realisasi transfer bila alurnya sudah terbentuk; selain itu tanggal saldo
	 *         awal. Dapat mengembalikan nilai lama (bukan hasil turunan) bila proxy relasinya
	 *         gagal di-inisialisasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		try {
			// FIX LazyInitializationException: daftarPengajuanTransfer bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy rantai
			// getTransitoriData()/getProsesTransfer()-nya terikat ke Session lain yang sudah
			// closed -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai
			// fallback dipertahankan).
			if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getTransitori()
					&& daftarPengajuanTransfer.getTransitoriData() != null
					&& daftarPengajuanTransfer.getTransitoriData().getProsesTransitori() != null) {
				tanggalTransaksi = daftarPengajuanTransfer.getTransitoriData().getProsesTransitori().getTanggalPembuatan();
			} else if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getProsesTransfer() != null) {
				tanggalTransaksi = daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan() == null
						? daftarPengajuanTransfer.getProsesTransfer().getTanggalPembuatan()
						: daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan();
			} else {
				tanggalTransaksi = getTanggal();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/JenisKasKecil.java:getTanggalTransaksi-lazy");
		}
		return tanggalTransaksi;
	}

	/**
	 * Menyetel tanggal realisasi transfer secara manual. Penugasan polos.
	 *
	 * <p><b>Praktis tidak berguna dipanggil dari luar:</b> nilai apa pun yang disetel di sini akan
	 * <b>ditimpa</b> pada pembacaan {@link #getTanggalTransaksi()} berikutnya, kecuali bila
	 * penurunan gagal karena proxy detached (dan nilai ini bertindak sebagai <i>fallback</i>).
	 * Method ini ada terutama agar Hibernate dapat mengisi field saat memuat baris.</p>
	 *
	 * @param tanggalTransaksi tanggal realisasi transfer
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Akun lawan untuk <b>penutupan</b> dompet kas kecil &mdash; label layar
	 * <i>&quot;Akun Penutup Kas Kecil&quot;</i>, opsional.
	 *
	 * <p><b>Satu-satunya konsumen (terverifikasi):</b> {@code PostingKasKecilAction}. Ketika
	 * dokumen {@code KasKecil} ditandai {@code merupakanPenutupanKasKecil}, layar itu menambahkan
	 * satu kaki <b>DEBET</b> ke akun ini sebesar <code>KasKecil.getSisa()</code> &mdash; yaitu
	 * jurnal yang menihilkan sisa dompet saat kas kecil ditutup atau di-reset. Di luar skenario
	 * penutupan, kolom ini tidak pernah dibaca.</p>
	 *
	 * <p><b>Bila kosong:</b> kaki penutup <b>tidak terbentuk</b> dan penutupan tetap dianggap
	 * berhasil &mdash; sisa dompet tidak dinihilkan di buku besar, tanpa pesan galat apa pun.
	 * {@code MasterKeuanganApiHelper} mengekspos kolom ini sebagai medan posisional
	 * <code>akunKeduaId</code> dan menandainya <b>tidak</b> wajib untuk jurnal, sehingga bendera
	 * <code>akunLengkap</code> tetap bernilai benar meski kolom ini kosong.</p>
	 *
	 * <p><b>Getter menulis balik ke field</b> lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}; berlaku catatan yang sama
	 * seperti pada {@link #getAkun()}.</p>
	 *
	 * @return akun penutup yang sudah di-resolve, atau <code>null</code> bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_penutup_kas_kecil", nullable = true)
	public Akun getAkunPenutupKasKecil() {
		akunPenutupKasKecil = check(akunPenutupKasKecil);
		return akunPenutupKasKecil;
	}

	/**
	 * Menyetel akun lawan untuk penutupan dompet. Penugasan polos tanpa validasi.
	 *
	 * <p>Sama seperti {@link #setAkun(Akun)}, nilai ini dibaca <i>live</i> saat posting, sehingga
	 * mengubahnya mengubah akun penutup seluruh dokumen penutupan yang belum diposting. Setter ini
	 * juga terjangkau lewat REST {@code MasterKeuanganApiHelper} (medan <code>akunKeduaId</code>)
	 * dengan penjaga fail-open yang sama &mdash; lihat catatan keamanan pada
	 * {@link #setAkun(Akun)}.</p>
	 *
	 * @param akunPenutupKasKecil akun penutup baru; boleh <code>null</code>, dengan akibat kaki
	 *                            jurnal penutupan tidak terbentuk
	 */
	public void setAkunPenutupKasKecil(Akun akunPenutupKasKecil) {
		this.akunPenutupKasKecil = akunPenutupKasKecil;
	}
}
