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
 * <h2>Hasil pemeriksaan kesehatan (medical screening) calon siswa PSB</h2>
 *
 * <p>Entity ini memetakan tabel <b>{@code sekolah.cek_kesehatan_siswa}</b> dan menyimpan
 * <b>hasil pemeriksaan kesehatan jasmani</b> seorang pendaftar pada jalur Penerimaan Siswa
 * Baru (PSB): status kelayakan kesehatan, daftar penyakit yang ditemukan, tekanan darah,
 * hasil tes buta warna, hasil tes narkoba, hasil rontgen, serta catatan pembatasan
 * ("sehat terbatas"). Satu baris mewakili <b>satu berkas pemeriksaan untuk satu pendaftar</b>.</p>
 *
 * <h3>PERINGATAN KATEGORI DATA — data medis anak di bawah umur</h3>
 * <p>Isi entity ini adalah <b>data kesehatan perorangan</b> milik <b>anak</b> (pendaftar jenjang
 * sekolah), termasuk indikasi penyakit, hasil pemeriksaan radiologi, dan hasil tes narkoba.
 * Dalam kerangka perlindungan data pribadi Indonesia (UU 27/2022) kategori ini termasuk
 * <b>data pribadi spesifik</b> dan menuntut pembatasan akses yang lebih ketat daripada data
 * administratif biasa. Setiap perubahan pada kelas ini, pada Action pengelolanya, maupun pada
 * jalur ekspor/impor yang menyentuhnya harus dinilai dengan asumsi tersebut. Bagian
 * "Catatan keamanan" di bawah mendokumentasikan kondisi kode <b>apa adanya</b> saat Javadoc
 * ini ditulis; tidak ada logika yang diubah oleh dokumentasi ini.</p>
 *
 * <h3>Yang diperiksa: CALON siswa, bukan siswa aktif</h3>
 * <p>Meskipun namanya "CekKesehatan<b>Siswa</b>", satu-satunya relasi entity ini adalah
 * {@link CalonSiswa} (kolom FK {@code calon_siswa}, {@code nullable = false}) — <b>bukan</b>
 * {@link Siswa}. Jadi ini adalah pemeriksaan kesehatan <b>pra-penerimaan</b> (seleksi PSB),
 * bukan rekam kesehatan berkala siswa yang sudah bersekolah. Konsekuensi praktisnya:</p>
 * <ul>
 *   <li>data tetap tertinggal pada pendaftar yang <b>tidak diterima</b> — tidak ada mekanisme
 *       retensi/penghapusan apa pun di kelas ini maupun di Action pengelolanya;</li>
 *   <li>tidak ada jalur ke {@code ais.database.model.sekolah.OrangTua} dari entity ini,
 *       sehingga pola fail-open {@code OrangTua.ambilAnakSiswa()} yang berulang di domain
 *       "catatan siswa" <b>tidak berlaku</b> di sini (lihat verifikasi negatif di bawah);</li>
 *   <li>saat pendaftar berubah menjadi {@link Siswa}, baris ini <b>tidak ikut dipindahkan</b>
 *       dan tetap tergantung pada baris {@link CalonSiswa} lamanya.</li>
 * </ul>
 *
 * <h3>Kardinalitas: satu-per-pendaftar secara konvensi, bukan secara skema</h3>
 * <p>Seluruh kode pemanggil memperlakukan relasi ini sebagai <b>satu baris per pendaftar</b>:
 * {@code CekKesehatanSiswaAction} memuat dan menyimpannya lewat
 * {@code Restrictions.eq("calonSiswa", …).uniqueResult()}. Namun pemetaannya adalah
 * {@link ManyToOne} biasa dan <b>tidak ada unique constraint</b> yang dideklarasikan pada
 * {@code calon_siswa}. Bila sampai ada dua baris untuk pendaftar yang sama — misalnya karena
 * unggahan Excel massal, atau karena tombol cetak yang menyisipkan baris kosong (lihat
 * "Efek samping tak terduga") — maka {@code uniqueResult()} melempar
 * {@code NonUniqueResultException} dan <b>layar pemeriksaan kesehatan berhenti bekerja untuk
 * pendaftar itu</b>. Layar rekap {@code CariDataPesertaUjianAction} justru memakai
 * {@code setMaxResults(1)} pada query yang sama, sehingga di sana duplikat tidak terdeteksi
 * melainkan diam-diam memilih salah satu baris secara tak deterministik (tanpa
 * {@code addOrder}). Ketidakkonsistenan dua pemanggil ini perlu diingat saat menambah
 * pemanggil baru.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Konstanta status</b> — {@link #Sehat}, {@link #SehatTerbatas}, {@link #Sakit}:
 *       tiga nilai yang diisikan ke {@link #getSehat()}. Perhatikan bahwa ketiganya
 *       {@code public static} <b>tanpa {@code final}</b> (lihat catatan kuirk).</li>
 *   <li><b>Identitas &amp; relasi</b> — {@link #getId()}/{@link #setId(Long)},
 *       {@link #getCalonSiswa()}/{@link #setCalonSiswa(CalonSiswa)}.</li>
 *   <li><b>Kesimpulan pemeriksaan</b> — {@link #getSehat()}/{@link #setSehat(String)}.</li>
 *   <li><b>Temuan penyakit</b> — {@link #getPenyakit1()}…{@link #getPenyakit5()} beserta
 *       setternya: lima baris teks bebas, satu temuan per baris.</li>
 *   <li><b>Pemeriksaan fisik &amp; laboratorium</b> — {@link #getTekananDarah()},
 *       {@link #getButaWarna()}, {@link #getNarkoba()}, dan
 *       {@link #getRontgen1()}…{@link #getRontgen3()}.</li>
 *   <li><b>Pembatasan aktivitas</b> — {@link #getSehatTerbatas1()}…{@link #getSehatTerbatas4()}:
 *       empat baris keterangan yang menjelaskan status {@link #SehatTerbatas}.</li>
 *   <li><b>Penomoran berkas</b> — {@link #getNoUrut()}/{@link #setNoUrut(String)}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, dan hook {@code onUpdate()}.</li>
 *   <li><b>Lain-lain</b> — dua konstruktor dan {@link #toString()}.</li>
 * </ul>
 *
 * <h3>Label UI yang sesungguhnya (diverifikasi dari kode pemanggil)</h3>
 * <p>Nama properti di sini pendek dan tidak menjelaskan dirinya sendiri. Label yang benar-benar
 * dilihat petugas dibangun secara programatik di
 * {@code ais.action.master.psb.CekKesehatanSiswaAction.onPilihCalonMahasiswa()} —
 * bukan di berkas ZUL — sehingga inilah rujukan yang sah:</p>
 * <table border="1" summary="Pemetaan properti ke label layar">
 *   <tr><th>Properti</th><th>Label layar</th></tr>
 *   <tr><td>{@code noUrut}</td><td>"Nomor Urut" (kotak teks, selalu {@code disabled})</td></tr>
 *   <tr><td>{@code sehat}</td><td>"Status Kesehatan" (combobox 3 pilihan)</td></tr>
 *   <tr><td>{@code penyakit1}…{@code penyakit5}</td><td>"Penyakit 1" … "Penyakit 5"</td></tr>
 *   <tr><td>{@code tekananDarah}</td><td>"Tekanan Darah"</td></tr>
 *   <tr><td>{@code butaWarna}</td><td>"Buta Warna"</td></tr>
 *   <tr><td>{@code narkoba}</td><td>"Narkoba"</td></tr>
 *   <tr><td>{@code rontgen1}…{@code rontgen3}</td><td>"Rontgen 1" … "Rontgen 3"</td></tr>
 *   <tr><td>{@code sehatTerbatas1}…{@code sehatTerbatas4}</td><td>"Sehat Terbatas 1" … "Sehat Terbatas 4"</td></tr>
 * </table>
 * <p>Layar itu juga menampilkan (hanya baca) identitas pendaftar dari {@link CalonSiswa}:
 * foto, Nama, No. Peserta, Sekolah, Jenis Kelamin, dan Tempat/Tanggal Lahir — sehingga satu
 * layar ini menggabungkan identitas anak dengan hasil pemeriksaan medisnya.</p>
 *
 * <h3>Seluruh kolom medis adalah TEKS BEBAS</h3>
 * <p>Tidak satu pun field pemeriksaan bertipe boolean, enum, atau referensi tabel kamus.
 * "Buta Warna" dan "Narkoba" pun {@code String}: petugas mengetik apa saja ("negatif", "-",
 * "TIDAK", "parsial ringan"). Akibatnya nilai tidak dapat diagregasi, disaring, atau
 * dibandingkan antar-tahun secara andal, dan satu-satunya field yang punya kosakata
 * terkendali adalah {@code sehat} (itu pun hanya karena combobox di layar, bukan karena
 * batasan di entity/DB). Semua {@code @Column} tanpa atribut {@code length}, sehingga panjang
 * mengikuti default penyedia (255 karakter) — keterangan panjang akan terpotong/gagal simpan.</p>
 *
 * <h3>Riwayat perubahan disimpan permanen ({@code @Audited})</h3>
 * <p>Kelas ini dianotasi {@link Audited}, sehingga Hibernate Envers menulis <b>salinan setiap
 * versi</b> baris ke tabel audit. Konsekuensi yang mudah terlewat pada data medis:
 * mengoreksi atau mengosongkan kolom penyakit/narkoba <b>tidak menghapus nilai lama</b>, dan
 * menghapus baris pun tetap meninggalkan seluruh riwayat di tabel audit. Setiap rencana
 * "penghapusan data kesehatan" harus memperhitungkan jejak Envers ini, bukan hanya tabel utama.</p>
 *
 * <h3>Hubungan dengan entity/kelas lain (peta lengkap pemanggil)</h3>
 * <ul>
 *   <li>{@code ais.action.master.psb.CekKesehatanSiswaAction} — satu-satunya layar
 *       pengelola: memilih pendaftar, memuat/menyimpan berkas, mengekspor &amp; mengimpor
 *       Excel, dan mencetak surat keterangan.</li>
 *   <li>{@code ais.action.master.psb.CariDataPesertaUjianAction} — layar rekap peserta ujian
 *       PSB; menampilkan <b>satu kolom berisi {@link #getSehat()}</b> (atau "-" bila belum
 *       ada berkas) untuk setiap baris pendaftar.</li>
 *   <li>{@code webapp/WEB-INF/new/root/psb/uiux/cek_kesehatan_siswa.jsp} dan
 *       {@code …/services/cek_kesehatan_siswa_service.jsp} — halaman "New UI" hasil generator
 *       yang mendeklarasikan entity ini sebagai kandidatnya, dan menyalurkannya ke
 *       Generic CRUD v2 (lihat catatan keamanan butir 4).</li>
 *   <li>Laporan JasperReports {@code "Cek_Kesehatan"} — dicetak oleh
 *       {@code CekKesehatanSiswaAction.onCetak(CalonSiswa)}.</li>
 * </ul>
 * <p><b>Tidak ada</b> entity lain yang mereferensikan kelas ini (tidak ada FK masuk), dan
 * tidak ada koleksi turunan. Perhatikan pula bahwa {@code ais.database.model.CekKesehatan}
 * (dipakai {@code ais.action.master.pmb.CekKesehatanAction}, layar
 * {@code /pages/master/cek_kesehatan.zul}) adalah entity <b>yang sama sekali berbeda</b> untuk
 * jalur perguruan tinggi/PMB — nol relasi dengan kelas ini, kemiripan nama semata.</p>
 *
 * <h3>Efek samping tak terduga pada alur cetak &amp; simpan</h3>
 * <ul>
 *   <li><b>Aksi "cetak" MENULIS ke basis data.</b>
 *       {@code CekKesehatanSiswaAction.onCetak(CalonSiswa)} mencari berkas pendaftar; bila
 *       belum ada, ia <b>membuat dan menyimpan baris {@code CekKesehatanSiswa} kosong</b>
 *       (hanya berisi FK pendaftar) sebelum mencetak. Jadi sekadar mencetak menghasilkan
 *       berkas pemeriksaan hampa yang selanjutnya terhitung sebagai "sudah diperiksa" pada
 *       query {@code uniqueResult()} di atas.</li>
 *   <li><b>Tombol "simpan dan cetak" tidak menyimpan.</b> Listener {@code onClick} untuk
 *       {@code buttonSimpanDanCetak} di {@code doAfterCompose()} hanya memanggil
 *       {@code onCetak(calonMahasiswa)}; ia <b>tidak pernah</b> memanggil
 *       {@code onSaveCekKesehatanSiswa()}. Data yang baru diketik petugas hilang tanpa pesan
 *       apa pun, dan surat tercetak dari isi lama (atau dari baris kosong yang baru dibuat).
 *       Ini perilaku kode saat ini, bukan dugaan.</li>
 *   <li><b>Objek yang dimuat tidak dikirim ke laporan.</b> {@code onCetak} menaruh hanya
 *       parameter acak dan foto pendaftar ke {@code Map} laporan; instance
 *       {@code CekKesehatanSiswa} yang dimuatnya tidak pernah masuk ke parameter — isi surat
 *       sepenuhnya bergantung pada query internal berkas Jasper.</li>
 * </ul>
 *
 * <h3>Penomoran berkas ({@code noUrut}) — tabrakan prefiks yang nyata</h3>
 * <p>{@code CekKesehatanSiswaAction.generateNoUrut()} menyusun nomor sebagai
 * {@code tahun + bulan + tanggal + urutan}, dengan bulan/tanggal <b>tanpa padding nol</b>,
 * lalu menghitung urutan memakai
 * {@code Restrictions.ilike("noUrut", date, MatchMode.START)}. Karena tanggal tidak
 * berpanjang tetap, prefiks satu tanggal dapat menjadi awalan tanggal lain: prefiks
 * {@code "202693"} (3 September 2026) juga cocok dengan nomor
 * {@code "20269301"} (30 September 2026). Hitungan urut karena itu tercampur antar tanggal,
 * sehingga nomor bisa <b>melompat</b> maupun (setelah penghapusan) <b>terulang</b>. Nomor
 * tidak unik di skema, tidak diberi indeks, dan pembangkitannya tidak transaksional
 * (dua petugas serentak memperoleh angka yang sama). Lihat pula
 * {@link #setNoUrut(String)}.</p>
 *
 * <h3>Catatan keamanan &amp; privasi (hasil audit atas kode saat ini)</h3>
 * <p>Ringkasan ini melengkapi — bukan menggantikan — dokumentasi Action terkait, dan
 * sengaja ditulis di entity karena entity-lah yang menjadi muara seluruh jalur akses.</p>
 * <ol>
 *   <li><b>Gerbang hak akses ADA, tetapi hanya untuk BACA.</b>
 *       {@code CekKesehatanSiswaAction.doAfterCompose()} memanggil
 *       {@code CommonPrivilages.checkPrevilages(CommonPrivilages.READ)} — jadi berbeda dengan
 *       {@code CatatanSiswa}/{@code PrestasiSiswa}, gerbangnya <b>tidak dikomentari</b>.
 *       Namun tidak ada satu pun pemeriksaan {@code CREATE}/{@code UPDATE}/{@code DELETE} di
 *       seluruh kelas itu: tombol simpan, tombol unduh Excel, dan tombol unggah Excel
 *       dipasang tanpa gerbang tambahan. Pemegang hak <b>BACA</b> karena itu dapat
 *       <b>menulis</b> hasil pemeriksaan kesehatan anak.</li>
 *   <li><b>Unggah Excel dapat menimpa berkas milik pendaftar mana pun.</b> Daftar kolom
 *       {@code contents} yang diserahkan ke {@code Common.uploadData(…)} <b>menyertakan
 *       {@code "id"}</b>, dan {@code ais.common.CommonDownloadUpload} — yang mengeksekusi
 *       unggahan — <b>tidak memuat satu pun panggilan {@code checkPrevilages}</b> lalu
 *       memanggil {@code session.saveOrUpdate(valueObject)}. Satu berkas Excel dengan kolom
 *       {@code id} yang diisi sembarang karena itu cukup untuk <b>menimpa hasil pemeriksaan
 *       kesehatan siapa pun di seluruh instalasi</b>. Pola identik pernah dicatat pada
 *       {@code JenisItemPenilaianSiswa}.</li>
 *   <li><b>Fail-open cakupan tenant pada ekspor.</b> {@code initCriteria()} milik tombol
 *       unduh hanya menyaring {@code calonSiswa.tahunAkademik} terhadap konfigurasi global
 *       {@code tahunAkademikPenerimaanMahasiswaBaru}. <b>Tidak ada</b> penyaring
 *       {@code yayasan} maupun {@code sekolah}. Pada instalasi multi-tenant, satu klik
 *       "Download" mengekspor berkas kesehatan seluruh pendaftar <b>lintas yayasan dan
 *       lintas sekolah</b> ke satu berkas Excel.</li>
 *   <li><b>Jalur "New UI" Generic CRUD v2: cakupan tenant runtuh menjadi NOL.</b> Halaman
 *       {@code root/psb/cek_kesehatan_siswa} disalurkan
 *       {@code _shared/services/dispatcher.jsp} ke
 *       {@code GenericCrudDefinitionRegistry.tryAutoRegister(…)}, dan entity ini menang
 *       pemilihan kandidat karena namanya sama persis dengan nama halaman. Pembatas cakupan
 *       untuk definisi hasil-otomatis adalah
 *       {@code GenericCrudAutoEntityAdapter.scopeBindings()}, yang <b>hanya</b> memasang
 *       {@code Restrictions.eq} pada properti bernama {@code yayasan}, {@code sekolah},
 *       {@code program}, {@code fakultas}, {@code jurusan}, {@code satuanKerja},
 *       {@code mahasiswa}, {@code siswa}, {@code dosen}, {@code guru}, {@code orangTua}, atau
 *       {@code anggotaKoperasi}. <b>Entity ini tidak memiliki satu pun di antaranya</b>
 *       (relasinya hanya {@code calonSiswa}), sehingga peta pembatas yang dihasilkan
 *       <b>kosong</b>: {@code applyScope()} tidak menambahkan syarat apa pun dan
 *       {@code validateObjectScope()} lolos tanpa memeriksa apa pun. Akibatnya, bagi
 *       pengguna mana pun yang punya hak BACA atas menu tersebut, {@code action=list},
 *       {@code action=get&id=N} (id berurutan — IDOR langsung), dan
 *       {@code action=export_xlsx}/{@code export_pdf} mengembalikan <b>seluruh berkas
 *       kesehatan di seluruh instalasi</b>. Ini <b>bukan</b> kelalaian satu layar melainkan
 *       sifat pembatas yang digerakkan nama properti: ia merosot senyap menjadi "tanpa
 *       pembatas" bagi setiap entity yang mencapai tenant-nya secara tidak langsung.</li>
 *   <li><b>Tidak ada satu pun kolom medis yang dianggap sensitif oleh Generic CRUD.</b>
 *       {@code GenericCrudAutoDefinitionFactory.BLOCKED_FIELD_TOKENS} berisi token seperti
 *       {@code password}, {@code token}, {@code secret} — <b>tidak ada</b> token kesehatan.
 *       Karena itu {@code penyakit1}…{@code penyakit5}, {@code narkoba}, {@code rontgen*},
 *       {@code tekananDarah}, dan {@code butaWarna} semuanya ditandai
 *       {@code sensitive = false}: dapat dibaca, diekspor, diurutkan, <b>dicari</b>, dan
 *       dijadikan filter cepat di layar generik.</li>
 *   <li><b>Tabel ini terjangkau endpoint {@code /Data} anonim.</b>
 *       {@code ais.action.servlet.Data} menutup {@code tanpaLogin=true} hanya untuk aksi
 *       <b>tulis</b> ({@code update_data}/{@code update_file_data}); aksi <b>baca</b>
 *       ({@code daftar}, {@code cari}, {@code load}, {@code sql}) tetap melewati pemeriksaan
 *       login begitu klien mengirim penanda itu sendiri. {@code ais.common.SqlSecurityGuard}
 *       berdefault {@code MODE_OFF}, dan bahkan pada {@code MODE_ENFORCE} ia hanya melarang
 *       bentuk tulis/DDL — {@code SELECT} atas {@code sekolah.cek_kesehatan_siswa} tetap
 *       diizinkan. Artinya <b>dump anonim seluruh rekam pemeriksaan kesehatan anak</b>
 *       secara mekanis dimungkinkan. Ini bukan temuan baru milik entity ini melainkan
 *       penguat masalah endpoint yang sudah tercatat — tetapi dengan kategori data
 *       <b>paling sensitif</b> yang pernah dipetakan ke endpoint itu.</li>
 *   <li><b>Pewarisan hak lewat menu.</b> {@code checkPrevilages(READ)} menilai hak terhadap
 *       {@code Common.getCurrentMenu()}, yang mengembalikan atribut session
 *       {@code "currentMenu"} — yaitu menu yang <b>terakhir diklik</b> pengguna. Cabang
 *       pencocokan URL di {@code CommonMenuAccessHelper.getCurrentMenu()} tidak dapat
 *       menemukan menu milik layar ini karena <b>tidak ada berkas ZUL yang memasang
 *       {@code CekKesehatanSiswaAction}</b> (diverifikasi: nol rujukan di seluruh
 *       {@code webapp/}). Jadi hak yang diberlakukan adalah hak menu lain. Selain itu
 *       {@code NewUiNativeJspResolver} memetakan menu lama ke halaman New UI berdasarkan
 *       <b>nama file pada kolom {@code url} menu</b> tanpa mensyaratkan ZUL-nya masih ada —
 *       sehingga baris menu warisan yang menunjuk {@code cek_kesehatan_siswa.zul} yang sudah
 *       terhapus tetap membuka layar ini.</li>
 * </ol>
 *
 * <h4>Verifikasi NEGATIF (pola berulang yang ternyata TIDAK terjadi di sini)</h4>
 * <ul>
 *   <li><b>Seeder bawaan tidak memberi hak berlebihan.</b>
 *       {@code ais.common.MenuInitializer.ensureSiswaRoleAndPrivileges()} memberi role
 *       {@code SISWA} hak CRUD penuh hanya atas menu 431898 (Kuesioner Siswa), 127616 (Rapor
 *       Siswa), dan 48916 (Catatan Siswa). Menu untuk layar cek kesehatan <b>tidak dibuat
 *       sama sekali</b> oleh {@code MenuInitializer}, dan karena itu tidak pernah masuk ke
 *       daftar hak bawaan role siswa maupun orang tua. Pola {@code CatatanSiswa}
 *       <b>tidak terulang</b>.</li>
 *   <li><b>Tidak ada endpoint API khusus.</b> Tidak ada padanan {@code CatatanApi} /
 *       {@code AktifitasHarianSiswaApi} untuk data kesehatan; pencarian atas seluruh
 *       {@code src/} hanya menemukan tiga berkas Java yang menyebut kelas ini (entity ini
 *       sendiri dan dua Action PSB). Jalur IDOR yang ada berasal dari Generic CRUD dan
 *       {@code /Data} generik, bukan dari API khusus.</li>
 *   <li><b>Fail-open {@code OrangTua.ambilAnakSiswa()} tidak berlaku.</b> Entity ini tidak
 *       pernah menyentuh {@code OrangTua} maupun {@code Siswa}; penyaringan "anak sendiri"
 *       tidak pernah dicoba, sehingga tidak ada varian fail-open tersebut untuk dilanggar.</li>
 *   <li><b>Mutasi lewat Generic CRUD terkunci — secara kebetulan.</b>
 *       {@code GenericCrudExistingActionInvoker.supports(…)} mensyaratkan Action sumber punya
 *       method {@code boolean onSave(Event)}; {@code CekKesehatanSiswaAction} hanya memiliki
 *       {@code private void onSaveCekKesehatanSiswa()}. Definisi otomatis karena itu jatuh ke
 *       {@code READ_ONLY} dan {@code create}/{@code update}/{@code delete} dimatikan.
 *       Perlindungan ini bertumpu pada <b>tanda tangan method</b>, bukan pada keputusan
 *       keamanan — menambahkan {@code boolean onSave(Event)} ke Action itu akan
 *       <b>seketika</b> membuka tulis lintas tenant tanpa peringatan apa pun.</li>
 * </ul>
 *
 * <h3>Kuirk yang perlu diketahui sebelum menyunting</h3>
 * <ul>
 *   <li><b>Javadoc bawaan generator salah nama.</b> Komentar asli hasil {@code hbm2java}
 *       berbunyi "Bank generated by hbm2java" — sisa salin-tempel; tidak ada hubungan dengan
 *       entity bank mana pun.</li>
 *   <li><b>Konstanta status tidak {@code final}.</b> {@link #Sehat}, {@link #SehatTerbatas},
 *       dan {@link #Sakit} adalah {@code public static String} biasa sehingga dapat ditimpa
 *       kode mana pun saat runtime. Karena nilainya disimpan sebagai <b>teks apa adanya</b>
 *       di kolom {@code status_sehat}, mengubahnya membuat baris lama tidak lagi cocok.</li>
 *   <li><b>Nama properti ≠ nama field ≠ nama kolom</b> pada status kesehatan: field
 *       {@code status_sehat}, properti Hibernate <b>{@code sehat}</b> (dari
 *       {@link #getSehat()}), kolom {@code status_sehat}. Query kriteria harus memakai
 *       {@code "sehat"}, bukan {@code "status_sehat"}.</li>
 *   <li><b>Kolom {@code no__urut} memakai DUA garis bawah.</b> Lihat {@link #getNoUrut()};
 *       hampir pasti salah ketik yang telanjur menjadi skema produksi.</li>
 *   <li><b>Field {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *       dideklarasikan ulang di sini dan itu MEMANG HARUS.</b>
 *       {@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 *       {@code @MappedSuperclass} melainkan POJO abstrak biasa, sehingga Hibernate tidak
 *       memetakan properti induknya. Pengulangan ini bukan bug dan jangan "dirapikan".</li>
 * </ul>
 *
 * @see CalonSiswa
 * @see GeneralValueObject
 * @see ais.database.model.CekKesehatan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "cek_kesehatan_siswa")

public class CekKesehatanSiswa extends GeneralValueObject {

	/**
	 * Nilai status "lulus pemeriksaan tanpa catatan" untuk kolom {@code status_sehat}.
	 *
	 * <p>Dipakai {@code CekKesehatanSiswaAction.doAfterCompose()} sebagai label sekaligus
	 * value item pertama combobox "Status Kesehatan". Nilai inilah yang tersimpan apa adanya
	 * di basis data — tidak ada kode/enum di baliknya.</p>
	 *
	 * <p><b>Perhatian:</b> deklarasi ini {@code public static} <b>tanpa {@code final}</b>,
	 * sehingga secara teknis dapat ditimpa saat runtime. Mengubah teksnya juga membuat baris
	 * lama yang berisi teks lama tidak lagi cocok dengan pilihan combobox.</p>
	 */
	public static String Sehat = "SEHAT";

	/**
	 * Nilai status "sehat dengan pembatasan" untuk kolom {@code status_sehat}.
	 *
	 * <p>Rincian pembatasannya diketik pada empat kolom bebas
	 * {@link #getSehatTerbatas1()}…{@link #getSehatTerbatas4()}. Tidak ada validasi yang
	 * mengharuskan kolom-kolom itu terisi ketika status ini dipilih, sehingga status
	 * "SEHAT TERBATAS" tanpa keterangan apa pun adalah keadaan yang sah menurut kode.</p>
	 *
	 * <p>Sama seperti {@link #Sehat}, konstanta ini {@code public static} tanpa {@code final}.</p>
	 */
	public static String SehatTerbatas = "SEHAT TERBATAS";

	/**
	 * Nilai status "tidak lulus pemeriksaan" untuk kolom {@code status_sehat}.
	 *
	 * <p>Merupakan item ketiga combobox "Status Kesehatan". Kode di seluruh repo tidak pernah
	 * membandingkan status ini untuk menggugurkan pendaftar secara otomatis — pengaruhnya
	 * terhadap kelulusan PSB sepenuhnya berupa keputusan manusia yang membaca kolom rekap di
	 * {@code CariDataPesertaUjianAction}.</p>
	 *
	 * <p>Sama seperti {@link #Sehat}, konstanta ini {@code public static} tanpa {@code final}.</p>
	 */
	public static String Sakit = "SAKIT";
	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan generator dan dipertahankan agar instance
	 * lama (mis. yang tersimpan di session ZK yang di-passivate) tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama basis data; dideklarasikan ulang di sini karena induknya tidak dipetakan Hibernate. */
	private Long id;

	/** Nama/identitas pengubah terakhir untuk jejak audit ringan; lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna pengubah terakhir untuk jejak audit ringan; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan berkas pemeriksaan ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, <b>menolak nilai kosong secara diam-diam</b>.
	 *
	 * <p>Bila {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung
	 * {@code return} tanpa mengubah apa pun dan tanpa melempar exception. Efeknya: jejak
	 * audit tidak pernah bisa dikosongkan kembali setelah terisi — perilaku yang disengaja
	 * agar interceptor audit tidak menghapus nilai yang sudah ada saat memproses instance
	 * yang tidak membawa konteks pengguna.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengubah terakhir, <b>menolak nilai kosong secara diam-diam</b>.
	 *
	 * <p>Bersifat sama persis dengan {@link #setOlehId(String)}: argumen {@code null} atau
	 * hanya-spasi menyebabkan method keluar tanpa efek, sehingga nilai lama tetap bertahan.</p>
	 *
	 * @param oleh nama/identitas pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/identitas pengguna yang terakhir menyimpan berkas pemeriksaan ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook {@code @PreUpdate} yang memperbarui stempel waktu audit tepat sebelum Hibernate
	 * menjalankan {@code UPDATE}.
	 *
	 * <p>Mendelegasikan seluruhnya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, sehingga aturan
	 * pengisian waktu (termasuk sumber waktu yang dipakai) terpusat di sana dan tidak
	 * disalin ke tiap entity. Karena berupa callback JPA, method ini <b>tidak boleh dipanggil
	 * manual</b> dari kode aplikasi.</p>
	 *
	 * <p>Perlu diketahui: callback ini hanya menyala pada {@code UPDATE}, bukan pada
	 * {@code INSERT}. Untuk baris baru, nilai awal berasal dari inisialisasi field
	 * {@code tanggal_dirubah} yang dideklarasikan pada baris yang sama di bawah ini —
	 * {@code ais.ui.util.WaktuUtil.getDate()} dievaluasi saat objek dikonstruksi, bukan saat
	 * disimpan. Untuk baris yang dibuat lalu baru disimpan jauh kemudian (mis. layar yang
	 * lama dibiarkan terbuka), selisih itu terbawa ke basis data.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dengan {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini
	 * <b>tidak</b> menolak {@code null}: memanggilnya dengan {@code null} benar-benar
	 * mengosongkan kolom audit waktu.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir yang ingin disimpan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir berkas pemeriksaan ini.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#TIMESTAMP} sehingga jam/menit/detik ikut
	 * tersimpan. Nilai awalnya diisi saat objek dikonstruksi dan diperbarui oleh
	 * {@code onUpdate()} pada setiap {@code UPDATE}.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berkas pemeriksaan: identitas pendaftar diikuti satu spasi.
	 *
	 * <p><b>Tidak menampilkan data medis apa pun</b> — hanya {@code calonSiswa.toString()}.
	 * Dipakai antara lain oleh komponen ZK yang merender objek ini apa adanya.</p>
	 *
	 * <p>Dua perilaku yang mudah mengejutkan:</p>
	 * <ul>
	 *   <li>method membaca <b>field</b> {@code calonSiswa} secara langsung, bukan lewat
	 *       {@link #getCalonSiswa()}, sehingga mekanisme {@code check()} milik
	 *       {@link GeneralValueObject} <b>dilewati</b>. Pada instance yang sudah detached
	 *       dengan proxy lazy yang belum terinisialisasi, perangkaian string di sini dapat
	 *       memicu {@code LazyInitializationException};</li>
	 *   <li>bila relasi belum diisi, hasilnya adalah string {@code "null "} — bukan
	 *       {@code NullPointerException} — karena operator {@code +} pada {@code String}
	 *       menangani {@code null}.</li>
	 * </ul>
	 *
	 * @return identitas pendaftar diikuti spasi
	 */
	public String toString() {
		return calonSiswa + " ";
	}

	/** Pendaftar PSB yang diperiksa; satu-satunya relasi entity ini (FK {@code calon_siswa}). */
	private CalonSiswa calonSiswa;

	/** Kesimpulan pemeriksaan; label layar "Status Kesehatan". Properti Hibernate-nya bernama {@code sehat}. */
	private String status_sehat;

	/** Temuan penyakit ke-1 (teks bebas); label layar "Penyakit 1". */
	private String penyakit1;

	/** Temuan penyakit ke-2 (teks bebas); label layar "Penyakit 2". */
	private String penyakit2;

	/** Temuan penyakit ke-3 (teks bebas); label layar "Penyakit 3". */
	private String penyakit3;

	/** Temuan penyakit ke-4 (teks bebas); label layar "Penyakit 4". */
	private String penyakit4;

	/** Temuan penyakit ke-5 (teks bebas); label layar "Penyakit 5". */
	private String penyakit5;

	/** Hasil pengukuran tekanan darah (teks bebas); label layar "Tekanan Darah". */
	private String tekananDarah;

	/** Hasil tes buta warna (teks bebas, bukan boolean); label layar "Buta Warna". */
	private String butaWarna;

	/** Hasil rontgen ke-1 (teks bebas); label layar "Rontgen 1". */
	private String rontgen1;

	/** Hasil rontgen ke-2 (teks bebas); label layar "Rontgen 2". */
	private String rontgen2;

	/** Hasil rontgen ke-3 (teks bebas); label layar "Rontgen 3". */
	private String rontgen3;

	/** Hasil tes narkoba (teks bebas, bukan boolean); label layar "Narkoba". */
	private String narkoba;

	/** Keterangan pembatasan ke-1 untuk status {@link #SehatTerbatas}; label layar "Sehat Terbatas 1". */
	private String sehatTerbatas1;

	/** Keterangan pembatasan ke-2 untuk status {@link #SehatTerbatas}; label layar "Sehat Terbatas 2". */
	private String sehatTerbatas2;

	/** Keterangan pembatasan ke-3 untuk status {@link #SehatTerbatas}; label layar "Sehat Terbatas 3". */
	private String sehatTerbatas3;

	/** Keterangan pembatasan ke-4 untuk status {@link #SehatTerbatas}; label layar "Sehat Terbatas 4". */
	private String sehatTerbatas4;

	/** Nomor urut berkas pemeriksaan berbasis tanggal; lihat {@link #getNoUrut()}. */
	private String noUrut;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Juga dipakai langsung oleh {@code CekKesehatanSiswaAction} untuk menyiapkan berkas
	 * kosong ketika pendaftar terpilih belum pernah diperiksa. Perhatikan bahwa field
	 * {@code tanggal_dirubah} sudah terisi waktu <b>saat objek ini dibuat</b>, bukan saat
	 * disimpan.</p>
	 */
	public CekKesehatanSiswa() {
	}

	/**
	 * Konstruktor pintas yang langsung mengikat berkas pemeriksaan ke seorang pendaftar.
	 *
	 * <p>Satu-satunya pemakai di repo adalah
	 * {@code CekKesehatanSiswaAction.onCetak(CalonSiswa)}, yang memakainya untuk
	 * <b>menyimpan baris kosong</b> ketika pendaftar dicetakkan surat keterangan tetapi belum
	 * memiliki berkas pemeriksaan — lihat catatan "Aksi cetak MENULIS" pada Javadoc kelas.</p>
	 *
	 * @param calonSiswa pendaftar PSB yang diperiksa; menjadi FK {@code calon_siswa} yang
	 *                   {@code nullable = false}, sehingga {@code null} akan gagal saat simpan
	 */
	public CekKesehatanSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Mengembalikan kunci utama baris pemeriksaan ini.
	 *
	 * <p>Dibangkitkan basis data ({@link javax.persistence.GenerationType#IDENTITY}) dan
	 * dipetakan dengan {@code insertable = false} sehingga tidak pernah ikut dikirim pada
	 * {@code INSERT}.</p>
	 *
	 * <p><b>Relevansi keamanan:</b> id ini berurutan dan menjadi parameter langsung jalur
	 * baca Generic CRUD ({@code action=get&id=N}) yang, sebagaimana diuraikan pada Javadoc
	 * kelas, tidak dibatasi cakupan tenant mana pun. Juga menjadi kolom kunci pada berkas
	 * unggahan Excel yang memutuskan baris mana yang ditimpa.</p>
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
	 * Menyetel kunci utama baris pemeriksaan.
	 *
	 * <p>Normalnya diisi Hibernate setelah {@code INSERT}. Pengisian manual hanya masuk akal
	 * pada jalur unggahan Excel ({@code CommonDownloadUpload}) yang memakai nilai ini untuk
	 * mengarahkan {@code saveOrUpdate} ke baris tertentu.</p>
	 *
	 * @param id kunci utama yang ingin disetel
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan pendaftar PSB yang diperiksa, setelah proxy lazy-nya diresolusi.
	 *
	 * <p>Getter ini memanggil {@code check(calonSiswa)} milik {@link GeneralValueObject} lebih
	 * dulu dan <b>menyimpan kembali hasilnya ke field</b>. Mekanisme {@code check()} bersifat
	 * <i>resolusi</i>, bukan destruktif: ia berusaha mengembalikan instance yang benar-benar
	 * terinisialisasi (lewat flag {@code initData}, cache, atau reload session) dan, bila
	 * keempat tahapnya gagal, mengembalikan argumen apa adanya. Dengan kata lain getter ini
	 * tidak pernah mengosongkan relasi — pola "getter destruktif" yang sering ditemukan pada
	 * entity lain <b>tidak terjadi</b> di kelas ini (tidak ada satu pun setter yang dipanggil
	 * dari dalam getter).</p>
	 *
	 * <p>Relasi dipetakan {@code LAZY} dengan cascade {@code PERSIST} dan {@code MERGE}:
	 * menyimpan berkas pemeriksaan ikut mem-persist/merge objek {@link CalonSiswa} yang
	 * ditempelkan, tetapi <b>tidak</b> menghapusnya bila berkas ini dihapus.</p>
	 *
	 * @return pendaftar yang diperiksa; secara skema tidak boleh {@code null} pada baris
	 *         tersimpan, namun instance yang belum disimpan bisa saja masih kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = false)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menetapkan pendaftar PSB yang diperiksa oleh berkas ini.
	 *
	 * <p>Dipanggil {@code CekKesehatanSiswaAction.onSaveCekKesehatanSiswa()} pada setiap
	 * penyimpanan — termasuk saat memperbarui berkas yang sudah ada, sehingga sebuah berkas
	 * dapat dipindahkan ke pendaftar lain tanpa pemeriksaan tambahan apa pun.</p>
	 *
	 * <p>Tidak ada validasi keunikan di sini maupun di skema: menyetel pendaftar yang sudah
	 * memiliki berkas lain menghasilkan duplikat yang kemudian mematahkan
	 * {@code uniqueResult()} pada layar pemeriksaan (lihat Javadoc kelas).</p>
	 *
	 * @param calonSiswa pendaftar yang diperiksa; wajib terisi agar baris dapat disimpan
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Mengembalikan kesimpulan pemeriksaan kesehatan ("Status Kesehatan" di layar).
	 *
	 * <p>Nilainya diharapkan salah satu dari {@link #Sehat}, {@link #SehatTerbatas}, atau
	 * {@link #Sakit}, namun <b>tidak ada penegakan apa pun</b> di entity maupun skema —
	 * jalur unggahan Excel dan jalur Generic CRUD dapat menuliskan teks sembarang. Nilai ini
	 * pula yang ditampilkan sebagai satu kolom ringkas pada layar rekap peserta ujian
	 * ({@code CariDataPesertaUjianAction.CalonRenderer.render()}), yang menampilkan
	 * {@code "-"} ketika berkas pemeriksaan belum ada.</p>
	 *
	 * <p><b>Kuirk penamaan:</b> properti Hibernate bernama {@code sehat} (turunan nama
	 * getter), field Java bernama {@code status_sehat}, dan kolom basis data bernama
	 * {@code status_sehat}. Kriteria/HQL harus menyebut {@code "sehat"}.</p>
	 *
	 * @return teks status kesehatan, atau {@code null} bila belum ditetapkan
	 */
	@Column(name = "status_sehat")
	public String getSehat() {
		return status_sehat;
	}

	/**
	 * Menetapkan kesimpulan pemeriksaan kesehatan.
	 *
	 * <p>Dipanggil dari {@code onSaveCekKesehatanSiswa()} dengan value item combobox yang
	 * terpilih. Layar menolak menyimpan bila combobox belum dipilih, namun perlindungan itu
	 * ada di Action — bukan di sini: setter ini menerima nilai apa pun, termasuk
	 * {@code null} dan string kosong.</p>
	 *
	 * @param sehat teks status kesehatan; idealnya salah satu dari {@link #Sehat},
	 *              {@link #SehatTerbatas}, atau {@link #Sakit}
	 */
	public void setSehat(String sehat) {
		this.status_sehat = sehat;
	}

	/**
	 * Mengembalikan temuan penyakit ke-1 (label layar "Penyakit 1").
	 *
	 * @return teks bebas temuan penyakit pertama, atau {@code null} bila kosong
	 */
	@Column(name = "penyakit_1")
	public String getPenyakit1() {
		return penyakit1;
	}

	/**
	 * Menetapkan temuan penyakit ke-1.
	 *
	 * @param penyakit1 teks bebas temuan penyakit pertama
	 */
	public void setPenyakit1(String penyakit1) {
		this.penyakit1 = penyakit1;
	}

	/**
	 * Mengembalikan temuan penyakit ke-2 (label layar "Penyakit 2").
	 *
	 * @return teks bebas temuan penyakit kedua, atau {@code null} bila kosong
	 */
	@Column(name = "penyakit_2")
	public String getPenyakit2() {
		return penyakit2;
	}

	/**
	 * Menetapkan temuan penyakit ke-2.
	 *
	 * @param penyakit2 teks bebas temuan penyakit kedua
	 */
	public void setPenyakit2(String penyakit2) {
		this.penyakit2 = penyakit2;
	}

	/**
	 * Mengembalikan temuan penyakit ke-3 (label layar "Penyakit 3").
	 *
	 * @return teks bebas temuan penyakit ketiga, atau {@code null} bila kosong
	 */
	@Column(name = "penyakit_3")
	public String getPenyakit3() {
		return penyakit3;
	}

	/**
	 * Menetapkan temuan penyakit ke-3.
	 *
	 * @param penyakit3 teks bebas temuan penyakit ketiga
	 */
	public void setPenyakit3(String penyakit3) {
		this.penyakit3 = penyakit3;
	}

	/**
	 * Mengembalikan temuan penyakit ke-4 (label layar "Penyakit 4").
	 *
	 * @return teks bebas temuan penyakit keempat, atau {@code null} bila kosong
	 */
	@Column(name = "penyakit_4")
	public String getPenyakit4() {
		return penyakit4;
	}

	/**
	 * Menetapkan temuan penyakit ke-4.
	 *
	 * @param penyakit4 teks bebas temuan penyakit keempat
	 */
	public void setPenyakit4(String penyakit4) {
		this.penyakit4 = penyakit4;
	}

	/**
	 * Mengembalikan temuan penyakit ke-5 (label layar "Penyakit 5").
	 *
	 * <p>Ini slot terakhir yang tersedia: temuan keenam dan seterusnya tidak punya tempat
	 * dan — sesuai desain layar — harus digabungkan ke dalam salah satu kolom yang ada.</p>
	 *
	 * @return teks bebas temuan penyakit kelima, atau {@code null} bila kosong
	 */
	@Column(name = "penyakit_5")
	public String getPenyakit5() {
		return penyakit5;
	}

	/**
	 * Menetapkan temuan penyakit ke-5.
	 *
	 * @param penyakit5 teks bebas temuan penyakit kelima
	 */
	public void setPenyakit5(String penyakit5) {
		this.penyakit5 = penyakit5;
	}

	/**
	 * Mengembalikan hasil pengukuran tekanan darah (label layar "Tekanan Darah").
	 *
	 * <p>Disimpan sebagai teks bebas, sehingga format penulisan ("120/80", "120/80 mmHg",
	 * "normal") tidak seragam dan tidak dapat dihitung/diperbandingkan secara numerik.</p>
	 *
	 * @return teks hasil pengukuran tekanan darah, atau {@code null} bila kosong
	 */
	@Column(name = "tekanan_darah")
	public String getTekananDarah() {
		return tekananDarah;
	}

	/**
	 * Menetapkan hasil pengukuran tekanan darah.
	 *
	 * @param tekananDarah teks hasil pengukuran tekanan darah
	 */
	public void setTekananDarah(String tekananDarah) {
		this.tekananDarah = tekananDarah;
	}

	/**
	 * Mengembalikan hasil tes buta warna (label layar "Buta Warna").
	 *
	 * <p><b>Bukan boolean.</b> Kolom ini {@code String}, sehingga "ya"/"tidak"/"-"/"parsial"
	 * sama-sama sah dan tidak dapat diandalkan untuk penyaringan otomatis.</p>
	 *
	 * @return teks hasil tes buta warna, atau {@code null} bila kosong
	 */
	@Column(name = "buta_warna")
	public String getButaWarna() {
		return butaWarna;
	}

	/**
	 * Menetapkan hasil tes buta warna.
	 *
	 * @param butaWarna teks hasil tes buta warna
	 */
	public void setButaWarna(String butaWarna) {
		this.butaWarna = butaWarna;
	}

	/**
	 * Mengembalikan hasil pemeriksaan rontgen ke-1 (label layar "Rontgen 1").
	 *
	 * <p>Hanya menyimpan <b>kesimpulan tertulis</b> — tidak ada berkas citra yang tersimpan
	 * pada entity ini maupun relasinya.</p>
	 *
	 * @return teks hasil rontgen pertama, atau {@code null} bila kosong
	 */
	@Column(name = "rontgen_1")
	public String getRontgen1() {
		return rontgen1;
	}

	/**
	 * Menetapkan hasil pemeriksaan rontgen ke-1.
	 *
	 * @param rontgen1 teks hasil rontgen pertama
	 */
	public void setRontgen1(String rontgen1) {
		this.rontgen1 = rontgen1;
	}

	/**
	 * Mengembalikan hasil pemeriksaan rontgen ke-2 (label layar "Rontgen 2").
	 *
	 * @return teks hasil rontgen kedua, atau {@code null} bila kosong
	 */
	@Column(name = "rontgen_2")
	public String getRontgen2() {
		return rontgen2;
	}

	/**
	 * Menetapkan hasil pemeriksaan rontgen ke-2.
	 *
	 * @param rontgen2 teks hasil rontgen kedua
	 */
	public void setRontgen2(String rontgen2) {
		this.rontgen2 = rontgen2;
	}

	/**
	 * Mengembalikan hasil pemeriksaan rontgen ke-3 (label layar "Rontgen 3").
	 *
	 * @return teks hasil rontgen ketiga, atau {@code null} bila kosong
	 */
	@Column(name = "rontgen_3")
	public String getRontgen3() {
		return rontgen3;
	}

	/**
	 * Menetapkan hasil pemeriksaan rontgen ke-3.
	 *
	 * @param rontgen3 teks hasil rontgen ketiga
	 */
	public void setRontgen3(String rontgen3) {
		this.rontgen3 = rontgen3;
	}

	/**
	 * Mengembalikan hasil tes narkoba (label layar "Narkoba").
	 *
	 * <p>Kolom paling sensitif pada entity ini: hasil tes penyalahgunaan zat atas seorang
	 * anak, disimpan sebagai teks bebas tanpa penanda kerahasiaan apa pun. Perlu diingat
	 * bahwa nilai lama tetap tersimpan di tabel audit Envers walaupun kolom ini dikoreksi
	 * atau dikosongkan, dan bahwa lapisan Generic CRUD <b>tidak</b> menganggap kolom ini
	 * sensitif sehingga ia dapat diekspor maupun dijadikan filter pencarian.</p>
	 *
	 * @return teks hasil tes narkoba, atau {@code null} bila kosong
	 */
	@Column(name = "narkoba")
	public String getNarkoba() {
		return narkoba;
	}

	/**
	 * Menetapkan hasil tes narkoba.
	 *
	 * @param narkoba teks hasil tes narkoba
	 */
	public void setNarkoba(String narkoba) {
		this.narkoba = narkoba;
	}

	/**
	 * Mengembalikan keterangan pembatasan ke-1 (label layar "Sehat Terbatas 1").
	 *
	 * <p>Empat kolom {@code sehatTerbatas*} menjelaskan status {@link #SehatTerbatas} —
	 * misalnya aktivitas yang tidak boleh diikuti. Tidak ada aturan yang mengharuskan
	 * kolom ini terisi ketika status itu dipilih.</p>
	 *
	 * @return teks keterangan pembatasan pertama, atau {@code null} bila kosong
	 */
	@Column(name = "sehat_terbatas_1")
	public String getSehatTerbatas1() {
		return sehatTerbatas1;
	}

	/**
	 * Menetapkan keterangan pembatasan ke-1.
	 *
	 * @param sehatTerbatas1 teks keterangan pembatasan pertama
	 */
	public void setSehatTerbatas1(String sehatTerbatas1) {
		this.sehatTerbatas1 = sehatTerbatas1;
	}

	/**
	 * Mengembalikan keterangan pembatasan ke-2 (label layar "Sehat Terbatas 2").
	 *
	 * @return teks keterangan pembatasan kedua, atau {@code null} bila kosong
	 */
	@Column(name = "sehat_terbatas_2")
	public String getSehatTerbatas2() {
		return sehatTerbatas2;
	}

	/**
	 * Menetapkan keterangan pembatasan ke-2.
	 *
	 * @param sehatTerbatas2 teks keterangan pembatasan kedua
	 */
	public void setSehatTerbatas2(String sehatTerbatas2) {
		this.sehatTerbatas2 = sehatTerbatas2;
	}

	/**
	 * Mengembalikan keterangan pembatasan ke-3 (label layar "Sehat Terbatas 3").
	 *
	 * @return teks keterangan pembatasan ketiga, atau {@code null} bila kosong
	 */
	@Column(name = "sehat_terbatas_3")
	public String getSehatTerbatas3() {
		return sehatTerbatas3;
	}

	/**
	 * Menetapkan keterangan pembatasan ke-3.
	 *
	 * @param sehatTerbatas3 teks keterangan pembatasan ketiga
	 */
	public void setSehatTerbatas3(String sehatTerbatas3) {
		this.sehatTerbatas3 = sehatTerbatas3;
	}

	/**
	 * Mengembalikan keterangan pembatasan ke-4 (label layar "Sehat Terbatas 4").
	 *
	 * <p>Slot pembatasan terakhir yang tersedia.</p>
	 *
	 * @return teks keterangan pembatasan keempat, atau {@code null} bila kosong
	 */
	@Column(name = "sehat_terbatas_4")
	public String getSehatTerbatas4() {
		return sehatTerbatas4;
	}

	/**
	 * Menetapkan keterangan pembatasan ke-4.
	 *
	 * @param sehatTerbatas4 teks keterangan pembatasan keempat
	 */
	public void setSehatTerbatas4(String sehatTerbatas4) {
		this.sehatTerbatas4 = sehatTerbatas4;
	}

	/**
	 * Menetapkan nomor urut berkas pemeriksaan.
	 *
	 * <p>Diisi {@code onSaveCekKesehatanSiswa()} dari kotak teks "Nomor Urut" yang selalu
	 * {@code disabled} di layar, sehingga isinya berasal dari
	 * {@code CekKesehatanSiswaAction.generateNoUrut()} untuk berkas baru, atau dari nilai
	 * tersimpan untuk berkas lama. Setter ini sendiri menerima teks apa pun — jalur unggahan
	 * Excel dan Generic CRUD tidak melewati pembangkit tersebut.</p>
	 *
	 * @param noUrut nomor urut berkas dalam bentuk teks
	 */
	public void setNoUrut(String noUrut) {
		this.noUrut = noUrut;
	}

	/**
	 * Mengembalikan nomor urut berkas pemeriksaan (label layar "Nomor Urut").
	 *
	 * <p>Formatnya {@code tahun + bulan + tanggal + urutan} <b>tanpa padding nol</b>,
	 * misalnya {@code "2026931"} untuk berkas pertama tanggal 3 September 2026.</p>
	 *
	 * <p><b>Nomor ini tidak dapat diandalkan sebagai identitas.</b> Karena bulan dan tanggal
	 * tidak berpanjang tetap sementara pembangkitnya menghitung memakai pencocokan awalan
	 * ({@code MatchMode.START}), awalan satu tanggal dapat mencakup tanggal lain — hitungan
	 * urut karena itu tercampur antar tanggal, dan nomor dapat melompat maupun terulang.
	 * Tidak ada indeks unik pada kolom ini, dan pembangkitannya tidak transaksional. Untuk
	 * identitas gunakan {@link #getId()}.</p>
	 *
	 * <p><b>Kuirk skema:</b> nama kolomnya {@code no__urut} dengan <b>dua</b> garis bawah —
	 * salah ketik yang telanjur menjadi skema produksi. Jangan "diperbaiki" tanpa migrasi.</p>
	 *
	 * @return nomor urut berkas, atau {@code null} bila belum pernah dibangkitkan
	 */
	@Column(name = "no__urut")
	public String getNoUrut() {
		return noUrut;
	}

}
