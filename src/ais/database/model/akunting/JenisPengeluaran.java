package ais.database.model.akunting;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.asset.JenisAsset;

/**
 * <h2>Jenis Pengeluaran — katalog pilihan baris biaya pada rincian Reimbursement
 * Pegawai, sekaligus tempat admin memetakan akun biayanya</h2>
 *
 * <p>Entity ini memetakan tabel {@code akunting.jenis_pengeluaran}. Perannya
 * <b>murni katalog (master referensi)</b>: satu baris = satu "jenis pengeluaran"
 * berbahasa manusia yang biasa diklaim pegawai (mis. "BBM / Bensin", "Parkir",
 * "Tol", "Konsumsi Rapat", "ATK (Alat Tulis Kantor)"), plus <b>akun biaya</b>
 * yang dipetakan admin untuk jenis tersebut. Tujuannya membalik beban
 * pengetahuan: pegawai pengaju cukup memilih nama yang ia mengerti, sedangkan
 * pemilihan kode akun buku besar — yang rumit dan mudah salah — dilakukan
 * <b>sekali</b> oleh administrator pada master ini.</p>
 *
 * <p>Kelas mewarisi {@link ais.database.model.GeneralValueObject}. Perlu
 * diingat: {@code GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * <b>tidak</b> memetakan properti apa pun milik induk. Semua kolom yang
 * dibutuhkan <b>harus</b> dideklarasikan ulang di kelas ini; itu keharusan
 * teknis, bukan duplikasi yang keliru. Yang diwarisi dan benar-benar dipakai
 * di sini hanyalah utilitas statis {@code check(...)} (resolusi relasi lazy)
 * serta {@code equals()} berbasis primary key.</p>
 *
 * <h2>Kolom nyata (hasil verifikasi kode, bukan dugaan)</h2>
 *
 * <p>Katalog ini <b>sangat ramping</b> — hanya tujuh kolom, dan beberapa yang
 * lazim ada pada saudara-saudaranya justru <b>TIDAK ADA</b>:</p>
 *
 * <table border="1">
 * <tr><th>Properti</th><th>Kolom</th><th>Catatan</th></tr>
 * <tr><td>{@link #getId()}</td><td>{@code id}</td>
 *     <td>{@code bigserial}, {@code insertable = false} (nilai dibangkitkan basis data)</td></tr>
 * <tr><td>{@link #getNama()}</td><td>{@code nama varchar(255)}</td>
 *     <td>label yang dilihat pegawai; <b>tanpa indeks unik</b></td></tr>
 * <tr><td>{@link #getKeterangan()}</td><td>{@code keterangan text}</td>
 *     <td>teks bebas, hanya ditampilkan</td></tr>
 * <tr><td>{@link #getAkun()}</td><td>{@code akun int8} &rarr; {@link Akun}</td>
 *     <td><b>inti kelas ini</b>; nullable, artinya "belum dipetakan admin"</td></tr>
 * <tr><td>{@link #getJenisAsset()}</td><td>{@code jenis_asset int8} &rarr; {@link JenisAsset}</td>
 *     <td>nullable — dan <b>kolom tidur</b>, lihat butir "hal non-obvious" di bawah</td></tr>
 * <tr><td>{@link #getAktif()}</td><td>{@code aktif boolean}</td>
 *     <td>{@code null} diperlakukan sebagai {@code true} secara konsisten di tiga tempat</td></tr>
 * <tr><td>{@link #getTanggalDirubah()}</td><td>{@code tanggal_dirubah timestamp}</td>
 *     <td>stempel waktu sederhana; disegarkan {@link #onUpdate()}</td></tr>
 * </table>
 *
 * <p><b>Yang TIDAK dimiliki entity ini, dan itu penting:</b></p>
 * <ul>
 * <li><b>Tidak ada kolom {@code kode}.</b> Berbeda dari {@link JenisKasBesar},
 * {@link JenisKasKecil}, {@link JenisUangMuka} dan
 * {@link CaraPembayaranTransfer} yang semuanya punya kode. Identitas
 * praktisnya hanya {@code nama}. {@code MasterKeuanganApiHelper} mengakui hal
 * ini secara eksplisit dengan mengirim {@code punyaKode = false} untuk tipe
 * {@code jenis_pengeluaran} (satu-satunya lain yang begitu:
 * {@link JenisReimbursement}).</li>
 * <li><b>Tidak ada kolom satuan kerja / sekolah / yayasan.</b> Ini
 * <b>terverifikasi dari dua sisi</b>: (a) tidak ada properti apa pun bertipe
 * {@code SatuanKerja} di kelas ini, dan (b) {@code MasterKeuanganApiHelper}
 * mengirim {@code punyaSatuanKerja = false} untuk tipe ini disertai komentar
 * kode "{@code jenis_pengeluaran & kategori_biaya_sales tidak bertautan satuan
 * kerja}". Konsekuensinya katalog ini <b>global untuk seluruh instalasi</b> —
 * lihat butir keamanan.</li>
 * <li><b>Tidak ada kolom anggaran/plafon</b> ({@code punyaAnggaran} hanya
 * berlaku untuk {@link JenisReimbursement}), tidak ada urutan tampil, tidak ada
 * hierarki induk-anak, dan tidak ada jejak audit Envers ({@code @Audited} tidak
 * dipasang).</li>
 * </ul>
 *
 * <h2>Siapa yang memakai katalog ini (TERVERIFIKASI)</h2>
 *
 * <p>Pencarian menyeluruh atas nama kelas maupun nama tabel di seluruh repo
 * (Java, JSP, ZUL, XML) hanya menemukan enam berkas: kelas ini,
 * {@code hibernate.cfg.xml}, {@code InitIndex}, {@code ReimbursementPegawaiAction},
 * {@code MasterKeuanganApiHelper}, dan {@code ReimbursementApiHelper}. Tidak
 * ada lagi. Rinciannya:</p>
 *
 * <ol>
 * <li><b>Layar ZK "Reimbursement Pegawai" — tab "Jenis Pengeluaran".</b>
 * {@code ReimbursementPegawaiAction.onJenisPengeluaran()} menggambar daftar
 * seluruh baris (aktif maupun tidak) dan
 * {@code bukaFormJenisPengeluaran(JenisPengeluaran)} menyediakan form
 * tambah/ubah (nama, akun biaya, jenis asset, keterangan, aktif). Tab-nya
 * dideklarasikan di {@code reimbursement_pegawai.zul} sebagai
 * {@code jenisPengeluaranPanel}. Jalur ini <b>tidak menyediakan tombol
 * hapus</b> sama sekali — penghapusan hanya mungkin lewat REST.</li>
 * <li><b>Combo pemilih pada rincian item pengajuan.</b>
 * {@code ReimbursementPegawaiAction.ambilJenisPengeluaran()} memuat baris
 * <b>aktif</b> saja ({@code aktif IS NULL OR aktif = true}) ke sebuah cache
 * per-siklus-form, lalu setiap baris rincian menampilkannya sebagai
 * {@code Combobox}. Nama jenis yang akunnya belum dipetakan diberi imbuhan
 * "{@code (akun belum dipetakan)}" agar pengaju tahu sebelum ditolak
 * validasi.</li>
 * <li><b>API JSON POS Desktop/Android.</b> {@code MasterKeuanganApiHelper}
 * (aksi {@code master_keuangan_daftar|opsi|simpan|hapus}) memelihara CRUD-nya,
 * dan {@code ReimbursementApiHelper.opsi()} mengirim daftar jenis aktif beserta
 * {@code akunId}/{@code akunKode}/{@code akunNama} agar layar POS dapat
 * menampilkan pilihan yang sama dengan ZK.</li>
 * <li><b>Penyemai bootstrap.</b> {@code InitIndex.initDefaultJenisPengeluaran()}
 * — dipanggil dari rangkaian inisialisasi {@code InitIndex} saat Tomcat naik.</li>
 * </ol>
 *
 * <h2>Entity dokumen terkait — dan jebakan nama yang mirip</h2>
 *
 * <p><b>Tidak ada entity dokumen bernama "Pengeluaran" atau "PengeluaranKas" di
 * paket {@code akunting} yang memakai katalog ini.</b> Pencarian menyeluruh
 * memastikan hal itu. Satu-satunya dokumen yang memakainya adalah
 * {@link ReimbursementPegawai}, dan — inilah bagian non-obvious — <b>tautannya
 * bukan foreign key</b>:</p>
 *
 * <ul>
 * <li>Rincian item reimbursement disimpan sebagai <b>teks JSON</b> pada kolom
 * {@code ReimbursementPegawai.formula}. Setiap baris berbentuk objek dengan
 * kunci {@code nama}, {@code jenisPengeluaran} (id baris katalog ini),
 * {@code akun} (id {@link Akun}), {@code masterAsset}, {@code tanggal},
 * {@code qty}, {@code harga}, {@code jumlah}.</li>
 * <li>Karena hanya angka di dalam teks, <b>tidak ada integritas referensial
 * apa pun</b>: menghapus baris katalog ini tidak dicegah basis data, dan
 * dokumen lama akan menyimpan id yatim tanpa gejala apa pun.</li>
 * <li>Sebagai gantinya {@code MasterKeuanganApiHelper.hitungPemakaian()}
 * mencari pemakaian dengan pencocokan teks
 * ({@code formula LIKE '%"jenisPengeluaran":' || ? || '%'}) — pendekatan yang
 * punya cacat sendiri, lihat butir keamanan/integritas.</li>
 * </ul>
 *
 * <p><b>WASPADA nama kembar.</b> Repo ini juga punya
 * {@code ais.database.model.JenisPengeluaranMahasiswa} dan
 * {@code ais.database.model.PengeluaranMahasiswa} (beserta
 * {@code JenisPengeluaranMahasiswaAction}, {@code PengeluaranMahasiswaAction},
 * {@code PostingPengeluaranMahasiswaAction}) di paket {@code ais.database.model}
 * — itu <b>modul kemahasiswaan yang sama sekali berbeda</b>, tabel berbeda,
 * dengan mesin postingnya sendiri. <b>Nol relasi</b> dengan kelas ini: tidak
 * ada FK, tidak ada import silang, tidak ada kode bersama. Jangan sampai
 * tertukar saat mencari "pengeluaran" di repo.</p>
 *
 * <h2>Alur nyata akun biaya: SNAPSHOT, dan tidak pernah menjadi akun jurnal</h2>
 *
 * <p>Dua koreksi penting terhadap kesan yang mudah timbul dari nama kolomnya:</p>
 *
 * <ol>
 * <li><b>Akun disalin sebagai SNAPSHOT, bukan dibaca langsung saat posting.</b>
 * Saat pengaju memilih sebuah jenis pada satu baris rincian, penulis-balik ZK
 * menyimpan <b>dua</b> nilai sekaligus ke JSON: {@code jenisPengeluaran} (id
 * katalog) <i>dan</i> {@code akun} (id akun katalog <b>pada detik itu</b>;
 * {@code 0} bila belum dipetakan). Baris yang sudah tersimpan karena itu
 * <b>membeku</b> — mengubah pemetaan akun di master ini kemudian <b>tidak</b>
 * mengubah baris lama. Semantiknya sama dengan koreksi yang sudah dicatat untuk
 * {@link JenisReimbursement} (batch 78) dan <b>berbeda</b> dari rantai Uang
 * Muka / Kas Besar / Dana Talangan yang membaca akun jenisnya secara langsung
 * saat posting. Jendela dampak perubahan akun di sini hanya "ke depan", bukan
 * retroaktif.</li>
 * <li><b>Akun per baris ini TIDAK PERNAH menjadi akun jurnal.</b> Diverifikasi
 * ke hilir: penjurnalan reimbursement mengalir lewat
 * {@link DaftarPengajuanTransfer}, yang membaca akun dari
 * {@code getReimbursementPegawai().getAkun()} — akun tingkat <b>dokumen</b>
 * (turunan {@code Workspace}/{@link JenisReimbursement}) — dengan fallback
 * {@code getAkunBiaya()}; sedangkan kolom {@code ReimbursementPegawai
 * .postingPengeluaran} adalah kolom tidur yang tak pernah diisi. Akun hasil
 * pemetaan katalog ini nyatanya hanya dipakai untuk tiga hal:
 * (a) <b>gerbang validasi</b> — baris tanpa {@code akun} membuat pengajuan
 * ditolak, baik di ZK ({@code validasi}) maupun REST
 * ({@code ReimbursementApiHelper.masalahRincian()}), dengan pesan khusus
 * "Akun untuk Jenis Pengeluaran ... belum dipetakan oleh administrator";
 * (b) <b>penjumlahan nilai dokumen</b> — {@code hitungRincian()} <b>melewati</b>
 * baris yang {@code akun}-nya nol, sehingga baris tanpa akun tidak ikut
 * dihitung; (c) <b>label kolom "Akun" pada cetakan PDF</b>. Jadi label "akun
 * biaya per baris" pada dokumentasi {@code MasterKeuanganApiHelper} sebaiknya
 * dibaca sebagai "akun biaya <i>administratif</i> per baris" — ia menentukan
 * apakah dokumen boleh lewat dan berapa nilainya, bukan ke akun mana jurnalnya
 * jatuh.</li>
 * </ol>
 *
 * <h2>Penyemaian bawaan saat bootstrap</h2>
 *
 * <p>{@code InitIndex.initDefaultJenisPengeluaran()} berjalan saat Tomcat naik
 * dan melakukan dua hal lewat SQL mentah:</p>
 * <ol>
 * <li>{@code CREATE TABLE IF NOT EXISTS akunting.jenis_pengeluaran (...)} —
 * artinya <b>skema tabel ini dibuat oleh kode aplikasi</b>, bukan oleh
 * {@code hbm2ddl} maupun migrasi terpisah. DDL itulah sumber kebenaran daftar
 * kolom, dan ia cocok persis dengan properti di kelas ini (termasuk
 * ketiadaan {@code kode} dan {@code satuan_kerja}).</li>
 * <li>Menyisipkan <b>60 nama jenis umum</b> (BBM, Parkir, Tol, tiket, konsumsi,
 * ATK, perbaikan, sewa, pelatihan, biaya bank, dan seterusnya) — <b>tanpa
 * akun</b> ({@code INSERT} hanya mengisi {@code nama}, {@code aktif},
 * {@code tanggal_dirubah}). Pemetaan akun sengaja diserahkan ke admin karena
 * bagan akun berbeda-beda per tenant.</li>
 * </ol>
 * <p>Penjaga idempotensinya adalah
 * {@code WHERE NOT EXISTS (SELECT 1 FROM akunting.jenis_pengeluaran)} —
 * <b>seluruh tabel</b>, bukan per nama. Sifatnya "semua atau tidak sama
 * sekali": begitu ada <i>satu</i> baris tersisa, penyemaian tidak akan pernah
 * berjalan lagi. Bila admin menghapus 59 dari 60 bawaan, sisanya tidak akan
 * dipulihkan oleh restart. Sebaliknya ini juga berarti penyemaian tidak pernah
 * menduplikasi data yang sudah dipelihara operator.</p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 *
 * <ul>
 * <li><b>Identitas &amp; label</b> — {@link #getId()}, {@link #getNama()},
 * {@link #getKeterangan()}, {@link #toString()}.</li>
 * <li><b>Pemetaan akuntansi</b> — {@link #getAkun()} (inti kelas ini).</li>
 * <li><b>Pemetaan aset (tidur)</b> — {@link #getJenisAsset()}.</li>
 * <li><b>Status &amp; jejak waktu</b> — {@link #getAktif()},
 * {@link #getTanggalDirubah()}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <p>Tidak ada satu pun <b>method bisnis</b> di kelas ini: tidak ada
 * perhitungan, tidak ada query, tidak ada mesin posting, tidak ada
 * {@code reloadDefault()}. Seluruh logika yang memakainya berada di Action dan
 * ApiHelper. Ini benar-benar katalog pasif.</p>
 *
 * <h2>Hal non-obvious yang WAJIB diketahui sebelum menyunting</h2>
 *
 * <ol>
 * <li><b>{@code jenisAsset} adalah kolom TIDUR.</b> Kolom ini bisa diisi admin
 * lewat tab ZK dan ditampilkan di kolom "Jenis Asset" pada daftar, tetapi
 * <b>tidak dibaca oleh satu pun konsumen hilir</b>. Diverifikasi: jalur
 * penerimaan barang {@code PenerimaanPengadaanMasterAssetAction
 * .generateDetailReimbursement()} membuat baris BAST dari kunci
 * {@code masterAsset} <b>per baris rincian</b> (sebuah
 * {@code ais.database.model.asset.MasterAsset} yang dipilih pengaju sendiri) —
 * bukan dari {@code jenisAsset} katalog ini; baris tanpa {@code masterAsset}
 * dilewati sebagai "biaya murni". {@code MasterKeuanganApiHelper} pun tidak
 * pernah membaca ataupun menulis properti ini. Karena itu kalimat lama pada
 * Javadoc kelas ini yang menyebut mapping aset "agar proses lanjutan penerimaan
 * aset dapat mengenali jenis asetnya" <b>tidak terbukti pada kode saat ini</b>
 * dan dipertahankan di sini hanya sebagai catatan niat rancangan. Perlakukan
 * kolom ini sebagai dekoratif sampai ada konsumen nyata.</li>
 * <li><b>{@link #getNama()} memangkas spasi saat dibaca, {@link #setNama(String)}
 * tidak.</b> Nilai yang tersimpan bisa saja masih mengandung spasi di ujung
 * (jalur REST {@code simpan()} dan form ZK memang melakukan {@code trim()}
 * sendiri sebelum memanggil setter, tetapi baris hasil impor/SQL langsung tidak
 * dijamin). Akibatnya nilai yang dibaca aplikasi bisa berbeda dari nilai di
 * kolom, dan pencarian {@code ILIKE} di sisi SQL memakai nilai mentah, bukan
 * hasil {@code trim()}.</li>
 * <li><b>{@link #getAktif()} mengubah {@code null} menjadi {@link Boolean#TRUE}</b>
 * — dan penafsiran itu <b>konsisten</b> di tiga tempat berbeda: getter ini,
 * kriteria Hibernate pada combo ZK ({@code aktif IS NULL OR aktif = TRUE}), dan
 * SQL POS ({@code COALESCE(jp.aktif,true) = true}). Verifikasi menenangkan:
 * tidak ada split-brain seperti yang ditemukan pada {@code JenisLaporan} batch
 * 79. Perhatikan bahwa getter ini <b>tidak</b> menulis balik ke field, jadi
 * tidak ada risiko {@code UPDATE} tersembunyi dari sini.</li>
 * <li><b>Tab ZK menampilkan baris nonaktif, combo pengajuan tidak.</b> Akibat
 * sampingannya: dokumen lama yang menunjuk jenis yang kemudian dinonaktifkan
 * akan merender {@code Combobox} <b>kosong</b> (item-nya tidak ada dalam
 * daftar), karena cabang tampilan pengganti hanya menyala bila
 * {@code jenisPengeluaran <= 0}. Datanya sendiri <b>tidak hilang</b>: penulis
 * balik hanya menimpa kunci JSON bila ada item yang benar-benar terpilih.</li>
 * <li><b>{@code tanggalDirubah} diisi pada saat konstruksi objek Java</b>
 * ({@code = ais.ui.util.WaktuUtil.getDate()} pada deklarasi field), bukan pada
 * saat {@code INSERT}. Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi
 * <b>tidak ada</b> {@code @PrePersist}, sehingga stempel baris baru adalah
 * "kapan objeknya dibuat di memori". Baris hasil penyemaian {@code InitIndex}
 * melewati Hibernate sepenuhnya dan memakai {@code now()} basis data. Kolom ini
 * murni informatif — tidak ada satu pun pembaca di seluruh repo.</li>
 * <li><b>{@code @Column(name = "id", insertable = false)} berpasangan dengan
 * {@code IDENTITY}</b>: id selalu dibangkitkan {@code bigserial} PostgreSQL dan
 * tidak pernah dikirim aplikasi. Menyetel {@link #setId(Long)} secara manual
 * untuk baris baru tidak akan berpengaruh pada {@code INSERT}.</li>
 * <li><b>{@code nama} tanpa indeks unik.</b> Baik ZK maupun REST tidak
 * memeriksa duplikasi nama, dan DDL penyemai tidak membuat batasan unik. Dua
 * baris "Parkir" dengan akun berbeda bisa hidup berdampingan; pengaju melihat
 * dua entri identik pada combo tanpa cara membedakannya.</li>
 * <li><b>{@code cascade = {PERSIST, MERGE}} pada kedua relasi.</b> Menyimpan
 * satu {@code JenisPengeluaran} akan ikut mem-{@code persist}/{@code merge}
 * objek {@link Akun} maupun {@link JenisAsset} yang tertaut. Dalam praktik
 * keduanya selalu berupa entity yang sudah tersimpan (hasil {@code session.get}
 * atau pilihan banbox), jadi efeknya netral — tetapi jangan menautkan objek
 * {@link Akun} baru yang belum tersimpan ke sini kecuali memang ingin akun itu
 * ikut tercipta.</li>
 * <li><b>{@link #getAkun()} dan {@link #getJenisAsset()} menulis balik ke
 * field — tetapi TIDAK destruktif.</b> Keduanya menugaskan hasil
 * {@code check(...)} milik {@link ais.database.model.GeneralValueObject} ke
 * field-nya sendiri. Karena Hibernate di sini memakai <i>property access</i>
 * (anotasi menempel pada getter) ditambah {@code dynamicUpdate}, pola semacam
 * ini di kelas lain pernah menerbitkan {@code UPDATE} diam-diam (mis.
 * {@code Transaksi.getAkun()} batch 73, {@code ReimbursementPegawai.getAkun()}
 * batch 76). <b>Di sini tidak.</b> Diverifikasi ke implementasi {@code check()}:
 * jalur gagalnya berakhir pada {@code return data} — ia hanya dapat
 * <b>menukar referensi</b> ke instance kanonik/hasil muat-ulang untuk
 * <b>id yang sama</b>, dan tidak pernah mengembalikan {@code null} untuk
 * masukan non-{@code null}. Membaca entity ini karena itu tidak dapat
 * menghapus pemetaan akun katalog. Ini verifikasi <b>negatif yang
 * menenangkan</b> — jangan menyamakannya dengan getter destruktif di modul
 * dokumen.</li>
 * <li><b>{@link #toString()} dipakai sebagai label UI.</b> Ia mengembalikan
 * {@code nama} yang sudah di-{@code trim} (atau string kosong bila
 * {@code null}), sehingga baris tanpa nama tampil sebagai entri kosong pada
 * combo, bukan sebagai "null".</li>
 * </ol>
 *
 * <h2 id="keamanan">Catatan keamanan &amp; integritas (hasil audit)</h2>
 *
 * <ol>
 * <li><b>KONFIRMASI FINAL: entity ini adalah master KETUJUH yang terjangkau
 * fail-open {@code MasterKeuanganApiHelper.bolehAksi()} ({@code task_66986071}).</b>
 * Diverifikasi dari sisi entity ini: {@code jenis_pengeluaran} terdaftar pada
 * {@code tipeSah()}, punya cabangnya sendiri di {@code simpan()} (memanggil
 * {@code setNama}/{@code setKeterangan}/{@code setAktif}/<b>{@code setAkun}</b>
 * lalu {@code saveOrUpdate}) dan di {@code muat()} (yang dipakai {@code hapus()}
 * untuk {@code session.delete}). Gerbangnya adalah {@code bolehAksi(tbmuser,
 * "create"|"update"|"delete")} yang <b>fail-open</b>: bila
 * {@code tbmuser.hakAkses()} mengembalikan {@code null}, method itu
 * {@code return true} — mengizinkan penuh alih-alih menolak. Dengan ini
 * <b>ketujuh tipe</b> yang dijaga helper tersebut lengkap terverifikasi satu
 * per satu di inisiatif ini: {@code jenis_uang_muka}, {@code jenis_kas_kecil},
 * {@link JenisKasBesar}, {@link JenisReimbursement}, <b>{@code jenis_pengeluaran}
 * (berkas ini)</b>, {@link CaraPembayaranTransfer}, dan
 * {@code kategori_biaya_sales} (kelas terakhir berada di paket
 * {@code ais.database.model.koperasi}, di luar cakupan domain akunting).
 * Catatan kecil: Javadoc {@code MasterKeuanganApiHelper} sendiri masih menulis
 * "Enam data master" dan "Enam tipe yang dikenal" padahal kodenya sudah
 * menangani <b>tujuh</b> — dokumentasi helper itu tertinggal sejak
 * {@code kategori_biaya_sales} ditambahkan.</li>
 * <li><b>Dampak nyata bila fail-open itu tertembus — dan batasnya.</b>
 * Penyerang dapat mengubah pemetaan akun, menonaktifkan, atau menghapus jenis
 * pengeluaran. Dampak terbesarnya bersifat <b>gangguan proses dan penyesatan
 * dokumen</b>, bukan pembelokan jurnal: seperti dijelaskan di atas, akun per
 * baris tidak pernah menjadi akun jurnal, dan baris yang sudah tersimpan
 * memegang snapshot akun. Yang benar-benar bisa terjadi: (a) mengosongkan akun
 * seluruh katalog membuat <b>semua pengajuan reimbursement baru tertolak</b>
 * validasi di ZK maupun REST — penolakan layanan administratif untuk seluruh
 * instalasi; (b) mengarahkan akun ke akun yang keliru membuat <b>cetakan PDF
 * dan kolom "Akun" pada dokumen resmi menampilkan pembebanan yang salah</b>
 * meski jurnalnya jatuh ke tempat lain — dokumen kertas dan buku besar jadi
 * saling bertentangan; (c) baris rincian yang akunnya nol <b>tidak ikut
 * dihitung</b> {@code hitungRincian()}, sehingga nilai dokumen bisa berbeda dari
 * jumlah rincian yang tampak di layar.</li>
 * <li><b>Fail-open cakupan tenant — STRUKTURAL, bukan kondisional.</b> Katalog
 * ini <b>tidak punya sumbu tenant sama sekali</b> (tidak ada
 * {@code satuan_kerja}, {@code sekolah}, maupun {@code yayasan}), dan tidak
 * satu pun pembacanya menambahkan penyaring: tab ZK memuat seluruh baris,
 * {@code ambilJenisPengeluaran()} memuat seluruh baris aktif,
 * {@code MasterKeuanganApiHelper.daftar()} memuat 500 baris teratas tanpa
 * pembatas apa pun, dan {@code ReimbursementApiHelper.opsi()} mengirim seluruh
 * baris aktif. Artinya <b>satu admin tenant mana pun yang menyunting katalog
 * ini menyunting katalog SELURUH instalasi</b>. Polanya sama dengan
 * {@code Closing} (batch 75), {@code ProsesTransferStandingInstruction} (batch
 * 77) dan {@code Devisi} (batch 79): bukan penyaring tenant yang bocor,
 * melainkan memang tidak pernah ada sumbu tenantnya. Ini juga menjelaskan
 * kenapa penyemaian {@code InitIndex} tidak mengisi akun — bagan akun berbeda
 * per tenant, sementara katalognya dipaksa satu untuk semua.</li>
 * <li><b>Membaca daftar master ini tidak digerbangi hak sama sekali.</b>
 * {@code MasterKeuanganApiHelper.daftar()} dan {@code opsi()} <b>tidak</b>
 * memanggil {@code bolehAksi()} — token API yang terautentikasi apa pun dapat
 * membaca seluruh katalog beserta pemetaan akunnya (id, kode, dan nama akun
 * buku besar). Berkas helper itu sendiri sudah mencatat bahwa pola ini identik
 * di seluruh keluarga helper Keuangan, jadi ini bukan cacat unik entity ini,
 * tetapi tetap berarti struktur bagan akun bocor ke pemegang token peran
 * apa pun.</li>
 * <li><b>Pewarisan hak lewat menu induk [instance kumulatif ~20+].</b>
 * {@code ReimbursementPegawaiAction} — satu-satunya layar yang memelihara
 * katalog ini — <b>tidak memanggil {@code checkPrevilages()} sama sekali</b>
 * (nol kemunculan di seluruh berkas). Akses ke halaman sepenuhnya bergantung
 * pada apakah menu induknya terpasang bagi peran pengguna. Namun ada
 * <b>verifikasi menenangkan</b> khusus untuk tab ini: aksi tulis-nya
 * digerbangi {@code bolehKelolaJenis()} yang memeriksa
 * {@code u.ambilRolesId().contains(Tbmrole.ADMINISTRATOR)} dan <b>fail-closed</b>
 * ({@code catch} mengembalikan {@code false}). Gerbang itu dipasang di kedua
 * titik yang benar — tombol "Tambah" tidak dirender, <i>dan</i>
 * {@code bukaFormJenisPengeluaran()} tetap menolak di awal method meski
 * dipanggil lewat jalur lain. Non-admin melihat pesan penjelas alih-alih
 * tombol. Jadi jalur ZK di sini justru <b>lebih ketat</b> daripada jalur REST
 * yang fail-open.</li>
 * <li><b>{@code task_0a06e418} (grid centang tanpa gerbang): TIDAK berlaku —
 * verifikasi negatif.</b> Berbeda dari {@link CaraPembayaranTransfer} dan
 * {@link JenisKasBesar} yang muncul pada grid berkotak-centang massal,
 * katalog ini tidak pernah dirender sebagai grid centang di mana pun. Tab
 * ZK-nya hanya daftar baca dengan satu tombol "Ubah" per baris (dan tombol itu
 * pun digerbangi {@code bolehKelolaJenis()}), sementara pemilihannya pada
 * rincian memakai {@code Combobox} tunggal, bukan {@code Checkbox}. Satu-satunya
 * {@code Checkbox} yang menyentuh entity ini adalah medan "Aktif" di dalam form
 * ubah — yang sudah berada di balik gerbang admin.</li>
 * <li><b>Bug integritas: penjaga "sudah dipakai" pada penghapusan memakai
 * pencocokan awalan, sehingga OVER-BLOCKING.</b>
 * {@code MasterKeuanganApiHelper.hitungPemakaian()} untuk tipe ini memakai
 * {@code formula LIKE '%"jenisPengeluaran":' || ? || '%'} dengan parameter
 * di-{@code setString}. Karena tidak ada pembatas di ujung kanan pola,
 * <b>id {@code 1} ikut cocok dengan dokumen yang sebenarnya memakai id
 * {@code 12}, {@code 15}, {@code 100}</b>, dan seterusnya. Arah kesalahannya
 * konservatif — menolak penghapusan yang sebenarnya aman, bukan mengizinkan
 * penghapusan yang berbahaya — sehingga tidak dinaikkan sebagai temuan
 * keamanan, tetapi jumlah "dipakai N dokumen" yang ditampilkan ke admin
 * <b>salah dan bisa jauh melambung</b> untuk id berdigit sedikit. Perbaikan
 * yang aman memerlukan pembatas eksplisit (mis. mencocokkan
 * {@code "jenisPengeluaran":<id>} yang diikuti koma atau kurung tutup) atau
 * berhenti menyimpan rincian sebagai teks JSON.</li>
 * <li><b>Split-brain kecil pada tampilan jumlah pemakaian.</b> Pada
 * {@code daftar()}, kolom "berapa dokumen memakai" untuk tipe ini dikirim
 * sebagai konstanta {@code CAST(0 AS bigint)} — <b>selalu nol</b>, satu-satunya
 * tipe yang tidak menghitung sungguhan (tipe lain memakai sub-query
 * {@code count(*)}). Akibatnya layar POS menampilkan setiap jenis pengeluaran
 * sebagai "belum dipakai" dan mempersilakan menghapusnya, lalu
 * {@code hapus()} — yang memanggil {@code hitungPemakaian()} sungguhan —
 * menolaknya. Dua jalur di berkas yang sama menjawab pertanyaan yang sama
 * dengan cara berbeda; gejalanya membingungkan admin, bukan berbahaya.</li>
 * <li><b>Verifikasi menenangkan lain.</b> Tidak ada permukaan JSP/dispatcher
 * anonim yang menyentuh tabel ini (bandingkan {@code task_1f9c66d3} /
 * {@code task_8c26a446}); tidak ada SQL yang merangkai nilai dari luar (semua
 * memakai {@code PreparedStatement} dengan nama tabel tertulis di kode); dan
 * entity ini tidak menyentuh {@code Closing}, sehingga tidak ada kaitan dengan
 * rantai penguncian periode {@code task_6e542cda}.</li>
 * </ol>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ReimbursementPegawai
 * @see JenisReimbursement
 * @see Akun
 * @see JenisAsset
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "akunting", name = "jenis_pengeluaran")
public class JenisPengeluaran extends GeneralValueObject {
    private static final long serialVersionUID = 1L;

    /** Primary key {@code bigserial}; lihat {@link #getId()}. */
    private Long id;
    /** Label jenis pengeluaran yang dipilih pegawai; lihat {@link #getNama()}. */
    private String nama;
    /** Penjelasan bebas untuk admin; lihat {@link #getKeterangan()}. */
    private String keterangan;
    /** Akun biaya hasil pemetaan admin; lihat {@link #getAkun()}. */
    private Akun akun;
    /** Pemetaan opsional ke jenis aset — kolom tidur; lihat {@link #getJenisAsset()}. */
    private JenisAsset jenisAsset;
    /** Penanda jenis masih boleh dipilih; {@code null} berarti aktif. Lihat {@link #getAktif()}. */
    private Boolean aktif;
    /**
     * Stempel perubahan terakhir. Diisi saat objek Java <b>dibuat</b> (bukan saat
     * {@code INSERT}) dan disegarkan {@link #onUpdate()}; lihat {@link #getTanggalDirubah()}.
     */
    private Date tanggalDirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Primary key baris katalog.
     *
     * <p>Dibangkitkan basis data ({@code bigserial} PostgreSQL, strategi
     * {@code IDENTITY}) dan ditandai {@code insertable = false}, sehingga nilai yang
     * disetel aplikasi tidak pernah ikut dikirim pada {@code INSERT}. Id inilah yang
     * disalin ke dalam JSON rincian {@code ReimbursementPegawai.formula} dengan kunci
     * {@code jenisPengeluaran} — tautan lunak tanpa foreign key.</p>
     *
     * @return id baris, atau {@code null} bila entity belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }

    /**
     * Menyetel primary key. Praktis hanya dipakai Hibernate saat memuat baris; untuk
     * baris baru nilainya diabaikan karena kolomnya {@code insertable = false}.
     *
     * @param id id baris; boleh {@code null}
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Label jenis pengeluaran — satu-satunya identitas yang dilihat pengguna karena
     * entity ini tidak punya kolom kode.
     *
     * <p><b>Memangkas spasi saat dibaca</b> ({@code trim()}), sementara
     * {@link #setNama(String)} menyimpan apa adanya. Nilai yang tersimpan di kolom
     * karena itu bisa berbeda dari nilai yang dikembalikan getter ini. Tidak ada
     * indeks unik atas kolom ini, sehingga nama kembar mungkin terjadi.</p>
     *
     * @return nama yang sudah dipangkas spasinya, atau {@code null} bila belum diisi
     */
    @Column(name = "nama", length = 255)
    public String getNama() { return nama == null ? null : nama.trim(); }

    /**
     * Menyetel label jenis pengeluaran. <b>Tidak</b> memangkas spasi maupun memeriksa
     * duplikasi — pemanggilnya ({@code ReimbursementPegawaiAction
     * .bukaFormJenisPengeluaran()} dan {@code MasterKeuanganApiHelper.simpan()}) yang
     * melakukan {@code trim()} dan memvalidasi bahwa nama tidak kosong.
     *
     * @param nama label jenis pengeluaran; boleh {@code null}
     */
    public void setNama(String nama) { this.nama = nama; }

    /**
     * Penjelasan bebas untuk admin (kolom {@code text}, tanpa batas panjang).
     *
     * <p>Hanya ditampilkan: kolom "Keterangan" pada tab ZK dan medan
     * {@code keterangan} pada daftar POS. Tidak ada logika yang membacanya sebagai
     * semantik — berbeda dari {@code JenisLaporan} (batch 79) yang teks bebasnya
     * justru ditafsirkan mesin.</p>
     *
     * @return keterangan apa adanya (tanpa {@code trim}), atau {@code null}
     */
    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() { return keterangan; }

    /**
     * Menyetel keterangan bebas.
     *
     * @param keterangan teks penjelas; boleh {@code null}
     */
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /**
     * Akun biaya yang dipetakan admin untuk jenis pengeluaran ini — <b>inti kelas
     * ini</b>. Dipetakan sekali, lalu dipakai setiap kali seorang pegawai memilih
     * jenis ini pada baris rincian pengajuan reimbursement.
     *
     * <p><b>Boleh {@code null}</b>, dan {@code null} punya makna operasional yang
     * tegas: "belum dipetakan admin". Seluruh lapisan memperlakukannya sebagai
     * keadaan sementara yang harus terlihat, bukan galat: tab ZK menampilkan teks
     * merah "{@code (belum dipetakan)}", combo pengajuan memberi imbuhan
     * "{@code (akun belum dipetakan)}" pada nama jenisnya, dan
     * {@code MasterKeuanganApiHelper} menandai medannya {@code wajibUntukJurnal}
     * sehingga daftar master ikut menyorot jenis yang belum lengkap. Penyimpanan
     * tetap diizinkan supaya admin dapat melengkapi bertahap.</p>
     *
     * <p><b>Bagaimana nilainya sampai ke dokumen.</b> Saat pengaju memilih jenis ini
     * pada satu baris rincian, penulis-balik ZK menyalin
     * {@code getAkun().getId()} ke kunci {@code akun} pada JSON baris tersebut
     * (atau {@code 0} bila akun belum dipetakan) — sebuah <b>snapshot</b>. Baris yang
     * sudah tersimpan tidak akan mengikuti perubahan pemetaan di sini; jendela dampak
     * penggantian akun hanya berlaku untuk baris yang dipilih setelahnya.</p>
     *
     * <p><b>Yang tidak dilakukan nilai ini:</b> akun ini <b>tidak pernah menjadi akun
     * jurnal</b>. Penjurnalan reimbursement mengambil akun tingkat dokumen lewat
     * {@code DaftarPengajuanTransfer} ({@code ReimbursementPegawai.getAkun()},
     * fallback {@code getAkunBiaya()}). Peran akun per baris terbatas pada gerbang
     * validasi pengajuan, penjumlahan nilai dokumen
     * ({@code ReimbursementApiHelper.hitungRincian()} melewati baris ber-akun nol),
     * dan label kolom "Akun" pada cetakan PDF.</p>
     *
     * <p><b>Efek samping (tidak destruktif).</b> Getter ini menugaskan hasil
     * {@code check(akun)} kembali ke field-nya. {@code check(...)} milik
     * {@link ais.database.model.GeneralValueObject} hanya <b>menukar referensi</b> ke
     * instance kanonik atau hasil muat-ulang untuk id yang sama, dan pada semua jalur
     * gagalnya mengembalikan argumen aslinya — tidak pernah {@code null}. Jadi
     * berbeda dari getter write-back di modul dokumen ({@code Transaksi.getAkun()},
     * {@code ReimbursementPegawai.getAkun()}), membaca entity ini <b>tidak dapat</b>
     * menghapus atau memindahkan pemetaan akun katalog.</p>
     *
     * <p><b>Dipanggil dari:</b> tab ZK "Jenis Pengeluaran" (kolom "Akun" dan
     * pengisian awal banbox pada form ubah), combo rincian pengajuan (imbuhan
     * "akun belum dipetakan" dan penyalinan snapshot), serta secara tidak langsung
     * oleh Hibernate saat memuat/menyimpan baris. Jalur POS tidak memakai getter ini
     * — ia membaca kolom {@code akun} lewat SQL langsung.</p>
     *
     * @return akun biaya yang dipetakan, atau {@code null} bila admin belum memetakan
     * @see Akun
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "akun", nullable = true)
    public Akun getAkun() { akun = check(akun); return akun; }

    /**
     * Menyetel akun biaya jenis pengeluaran ini.
     *
     * <p>Dipanggil dari dua tempat saja: form ZK tab "Jenis Pengeluaran" (nilai
     * diambil dari {@code AmbilDataAkunBanbox}; menjadi {@code null} bila banbox
     * dikosongkan) dan {@code MasterKeuanganApiHelper.simpan()} (nilai dari
     * {@code session.get(Akun.class, akunId)}, juga {@code null} bila
     * {@code akunId = 0}). Kedua jalur mengizinkan pengosongan.</p>
     *
     * <p><b>Efek samping tidak langsung:</b> karena relasi ini
     * {@code cascade = {PERSIST, MERGE}}, menyimpan katalog ikut mem-{@code merge}
     * objek {@link Akun} yang tertaut. Untuk akun yang sudah tersimpan efeknya netral;
     * jangan menautkan objek {@link Akun} yang belum pernah disimpan kecuali memang
     * ingin akun itu ikut tercipta.</p>
     *
     * <p><b>Konsekuensi hulu yang perlu disadari:</b> mengosongkan akun membuat
     * seluruh <b>pengajuan baru</b> yang memilih jenis ini ditolak validasi (ZK maupun
     * REST) sampai admin melengkapinya kembali. Baris yang sudah tersimpan tidak
     * terpengaruh karena memegang snapshot.</p>
     *
     * @param akun akun biaya; {@code null} berarti "belum dipetakan"
     */
    public void setAkun(Akun akun) { this.akun = akun; }

    /**
     * Pemetaan opsional ke jenis aset, untuk jenis pengeluaran yang menghasilkan
     * barang (mis. printer, perkakas).
     *
     * <p><b>KOLOM TIDUR — tidak ada satu pun konsumen hilir.</b> Nilainya dapat
     * diisi admin lewat combo "Jenis Asset (opsional)" pada form ZK dan ditampilkan
     * pada kolom "Jenis Asset" di daftar tab, tetapi <b>berhenti di situ</b>.
     * Diverifikasi menyeluruh: jalur penerimaan barang
     * {@code PenerimaanPengadaanMasterAssetAction.generateDetailReimbursement()}
     * membangun baris BAST dari kunci {@code masterAsset} <b>per baris rincian</b>
     * (sebuah {@code ais.database.model.asset.MasterAsset} yang dipilih pengaju
     * sendiri), bukan dari properti ini; baris tanpa {@code masterAsset} dilewati
     * sebagai "biaya murni". {@code MasterKeuanganApiHelper} tidak pernah membaca
     * maupun menulis properti ini (cabang {@code jenis_pengeluaran} pada
     * {@code simpan()} hanya menyentuh nama, keterangan, aktif, dan akun), dan
     * {@code ReimbursementApiHelper} tidak mengirimkannya ke POS. Perlakukan sebagai
     * dekoratif sampai ada konsumen nyata.</p>
     *
     * <p><b>Efek samping (tidak destruktif).</b> Sama seperti {@link #getAkun()}:
     * menugaskan hasil {@code check(jenisAsset)} kembali ke field, yang hanya menukar
     * referensi ke instance kanonik untuk id yang sama dan tidak pernah menghasilkan
     * {@code null} dari masukan non-{@code null}.</p>
     *
     * @return jenis aset yang dipetakan, atau {@code null} (keadaan yang paling lazim)
     * @see JenisAsset
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "jenis_asset", nullable = true)
    public JenisAsset getJenisAsset() { jenisAsset = check(jenisAsset); return jenisAsset; }

    /**
     * Menyetel pemetaan jenis aset. Satu-satunya pemanggil di seluruh repo adalah form
     * ZK tab "Jenis Pengeluaran" (nilai dari {@code Combobox}; {@code null} bila
     * pilihan "(tanpa mapping asset)" dipakai). Karena tidak ada pembaca hilir,
     * menyetel nilai ini tidak mengubah perilaku sistem mana pun.
     *
     * <p>Relasi ini juga {@code cascade = {PERSIST, MERGE}}, dengan pertimbangan yang
     * sama seperti {@link #setAkun(Akun)}.</p>
     *
     * @param jenisAsset jenis aset; boleh {@code null}
     */
    public void setJenisAsset(JenisAsset jenisAsset) { this.jenisAsset = jenisAsset; }

    /**
     * Apakah jenis ini masih boleh dipilih pada pengajuan baru.
     *
     * <p><b>{@code null} diperlakukan sebagai {@link Boolean#TRUE}</b> — baris lama
     * atau baris hasil impor yang kolomnya kosong tetap dianggap aktif. Penafsiran ini
     * <b>konsisten di tiga tempat</b>: getter ini, kriteria Hibernate combo pengajuan
     * ({@code aktif IS NULL OR aktif = TRUE}), dan SQL POS
     * ({@code COALESCE(jp.aktif,true) = true}) — tidak ada split-brain.</p>
     *
     * <p>Perhatikan cakupannya berbeda antar layar: <b>tab admin menampilkan seluruh
     * baris</b> (termasuk yang nonaktif, dengan kolom "Aktif" berisi Ya/Tidak),
     * sedangkan <b>combo pengajuan hanya memuat yang aktif</b>. Menonaktifkan sebuah
     * jenis karena itu tidak merusak dokumen lama, hanya menyembunyikannya dari
     * pilihan baru — dan itu memang jalan keluar yang disarankan
     * {@code MasterKeuanganApiHelper} ketika penghapusan ditolak
     * ("Nonaktifkan saja bila tidak dipakai lagi").</p>
     *
     * <p>Getter ini <b>tidak</b> menulis balik ke field — nilai substitusi dihitung
     * pada ekspresi kembalian saja, sehingga tidak ada risiko {@code UPDATE}
     * tersembunyi.</p>
     *
     * @return {@code true} bila aktif (termasuk saat kolomnya {@code null});
     *         {@code false} hanya bila memang disetel {@code false}
     */
    @Column(name = "aktif")
    public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }

    /**
     * Menyetel status aktif. Dipanggil dari medan {@code Checkbox} "Aktif" pada form ZK
     * dan dari {@code MasterKeuanganApiHelper.simpan()} (medan {@code aktif} pada
     * permintaan JSON, bawaan {@code true}).
     *
     * @param aktif status aktif; {@code null} akan dibaca kembali sebagai
     *              {@link Boolean#TRUE} oleh {@link #getAktif()}
     */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Stempel waktu perubahan terakhir baris katalog.
     *
     * <p>Untuk baris baru nilainya berasal dari inisialisasi field
     * ({@code WaktuUtil.getDate()} saat objek Java dibuat, <b>bukan</b> saat
     * {@code INSERT} — tidak ada {@code @PrePersist}); untuk pembaruan disegarkan
     * {@link #onUpdate()}. Baris hasil penyemaian {@code InitIndex} melewati Hibernate
     * dan memakai {@code now()} basis data.</p>
     *
     * <p><b>Murni informatif</b>: pencarian menyeluruh menunjukkan tidak ada satu pun
     * pembaca kolom ini — tidak di ZK, tidak di API POS, tidak di laporan. Entity ini
     * juga tidak {@code @Audited}, sehingga kolom inilah satu-satunya jejak perubahan
     * yang ada, dan jejak itu tidak menyimpan <i>siapa</i> yang mengubah.</p>
     *
     * @return waktu perubahan terakhir; praktis tidak pernah {@code null}
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggalDirubah() { return tanggalDirubah; }

    /**
     * Menyetel stempel perubahan secara manual. Tidak ada pemanggil di seluruh repo —
     * nilainya selalu datang dari inisialisasi field atau dari {@link #onUpdate()}.
     *
     * @param tanggalDirubah stempel waktu; boleh {@code null}
     */
    public void setTanggalDirubah(Date tanggalDirubah) { this.tanggalDirubah = tanggalDirubah; }

    /**
     * Kait daur hidup JPA {@code @PreUpdate}: menyegarkan {@link #getTanggalDirubah()}
     * ke waktu "sekarang" versi aplikasi ({@code ais.ui.util.WaktuUtil.getDate()},
     * bukan {@code new Date()}) tepat sebelum Hibernate menerbitkan {@code UPDATE}.
     *
     * <p>Menulis langsung ke field, bukan lewat setter, sehingga tidak memicu logika
     * lain. Berjalan pada <b>setiap</b> pembaruan baris — termasuk pembaruan yang
     * dipicu jalur REST {@code MasterKeuanganApiHelper.simpan()} maupun form ZK.</p>
     *
     * <p><b>Tidak</b> berjalan saat {@code INSERT} (tidak ada {@code @PrePersist}
     * pasangannya) dan <b>tidak</b> berjalan untuk perubahan lewat SQL mentah seperti
     * penyemaian {@code InitIndex} — pola yang sama dengan stempel posting
     * {@code SaldoAwalAkun} (batch 77) yang luput dari kait daur hidup.</p>
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() { tanggalDirubah = ais.ui.util.WaktuUtil.getDate(); }

    /**
     * Representasi teks entity — dipakai sebagai <b>label tampilan</b>, bukan untuk
     * penelusuran galat.
     *
     * <p>Mengembalikan {@link #getNama()} yang sudah dipangkas spasinya, atau string
     * <b>kosong</b> (bukan {@code "null"}) bila nama belum diisi; akibatnya baris tanpa
     * nama muncul sebagai entri kosong pada daftar/combo. Membaca field {@code nama}
     * langsung, sehingga aman dipanggil pada entity detached tanpa memicu resolusi
     * relasi apa pun.</p>
     *
     * @return nama jenis pengeluaran, atau string kosong bila belum diisi
     */
    public String toString() { return nama == null ? "" : nama.trim(); }
}
