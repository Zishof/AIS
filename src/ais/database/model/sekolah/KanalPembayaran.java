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
import ais.database.model.akunting.Akun;

/**
 * Konfigurasi <b>kanal pembayaran</b> (payment channel) untuk sebuah sekolah, yayasan, atau
 * seluruh instalasi &mdash; entity master yang menyimpan <b>kredensial LIVE payment gateway</b>
 * bagi enam integrasi bank/agregator sekaligus dalam SATU baris tabel:
 * <b>BNI</b>, <b>BSI Maja</b>, <b>Flip</b>, <b>Finpay</b>, <b>E-Smartlink</b>, dan <b>Online BMT</b>.
 *
 * <p style="border-left:4px solid #b00; padding-left:8px">
 * <b>&#9888; PERINGATAN SENSITIVITAS DATA &mdash; BACA SEBELUM MENYENTUH KELAS INI.</b>
 * Baris tabel <code>sekolah.kanal_pembayaran</code> adalah aset paling sensitif di seluruh modul
 * sekolah. Isinya bukan sekadar konfigurasi: ia berisi <i>password</i>, <i>API secret key</i>,
 * <i>validation token</i>, <i>AES encryption key</i>, dan <i>HMAC key</i> produksi milik bank dan
 * agregator pembayaran. Siapa pun yang dapat membaca satu baris di sini dapat, di luar sistem,
 * memanggil endpoint bank atas nama sekolah tersebut. Setiap perubahan pada kelas ini &mdash;
 * termasuk menambah getter baru, mengekspos properti ke laporan/Excel, atau menambahkannya ke
 * daftar kolom cetak/unggah &mdash; harus diperlakukan sebagai perubahan berdampak keamanan.
 * <b>Seluruh kredensial di kelas ini disimpan PLAINTEXT</b> (lihat "Kredensial disimpan
 * plaintext" di bawah).</p>
 *
 * <h3>Peran dalam rantai finansial</h3>
 * <p>Rantai biaya-ke-pembayaran di modul sekolah berbentuk
 * <code>JenisBiayaSekolah &rarr; PengaturanBiaya &rarr; ItemBiayaSekolah/NominalBiaya &rarr;
 * Tagihan &rarr; PembayaranSiswaDetail &rarr; PembayaranSiswa</code>. Entity ini berada di
 * <i>samping</i> rantai tersebut: ia bukan penyimpan nominal, melainkan penyimpan <b>identitas
 * dan rahasia teknis</b> yang dipakai saat tagihan dari rantai itu diterbitkan sebagai Virtual
 * Account atau dikirim ke gateway. Titik sambungnya adalah properti
 * <code>kanalPembayaran</code> pada beberapa entity pemilik:</p>
 * <ul>
 *   <li>{@link ais.database.model.sekolah.JenisBiayaSekolah} &mdash; kolom
 *       <code>kanal_pembayaran</code>; menentukan kanal yang dipakai untuk satu jenis biaya
 *       (SPP, uang gedung, dsb.). Nilai <code>null</code> berarti "Ikuti Kanal Pembayaran
 *       Default". <b>Inilah relasi yang membuat entity ini terjangkau lewat menu Konfigurasi
 *       Invoice</b> &mdash; lihat "Pewarisan hak lewat menu induk" di bawah.</li>
 *   <li>{@link ais.database.model.sekolah.Sekolah} &mdash; kolom <code>kanal_pembayaran</code>;
 *       kanal bawaan sekolah, dipakai bila jenis biaya tidak menunjuk kanal sendiri.</li>
 *   <li>{@code ais.database.model.JenisKegiatan} dan
 *       {@code ais.database.model.koperasi.CaraPembayaranKoperasi} &mdash; kanal untuk
 *       pembayaran kegiatan dan koperasi/kantin.</li>
 *   <li>{@code ais.database.model.VirtualAccountBank} &mdash; kolom
 *       <code>kanal_pembayaran</code>; SETIAP invoice VA yang diterbitkan menyimpan kanal yang
 *       dipakai, sehingga callback bank dapat menemukan kembali kredensial yang benar.</li>
 * </ul>
 *
 * <h3>Kredensial yang disimpan (hasil verifikasi kode, bukan asumsi)</h3>
 * <p>Label pada kolom "Label layar" diambil persis dari
 * {@code ais.action.master.sekolah.KanalPembayaranAction#init(KanalPembayaran)}.</p>
 * <table border="1" cellpadding="3" summary="Daftar kredensial per penyedia">
 * <tr><th>Penyedia</th><th>Properti</th><th>Label layar</th><th>Sifat</th></tr>
 * <tr><td rowspan="3">BNI</td><td>{@link #getBniMerchantId()}</td><td>BNI Merchant</td>
 *     <td>identitas merchant; boleh berformat per angkatan
 *     <code>{ANGKATAN}:{KODE};{ANGKATAN}:{KODE}</code></td></tr>
 * <tr><td>{@link #getBniPassword()}</td><td>BNI Password</td>
 *     <td><b>RAHASIA</b> (kunci enkripsi H2H BNI; juga boleh per angkatan)</td></tr>
 * <tr><td>{@link #getBniGatewayUrl()}</td><td>BNI Gateway Url</td>
 *     <td>endpoint; barisnya {@code setVisible(false)} di layar</td></tr>
 * <tr><td rowspan="5">BSI Maja</td><td>{@link #getBsiMerchantId()}</td><td>BSI Maja ClientID</td>
 *     <td>client id OAuth</td></tr>
 * <tr><td>{@link #getBsiScretId()}</td><td>BSI Maja SecretKey</td><td><b>RAHASIA</b></td></tr>
 * <tr><td>{@link #getBsiUsername()}</td><td>BSI Maja username</td>
 *     <td>separuh kredensial &mdash; <b>juga dipakai sebagai prefix nomor VA yang ditampilkan ke
 *     siswa</b>, lihat catatan pada getter</td></tr>
 * <tr><td>{@link #getBsiPassword()}</td><td>BSI Maja password</td><td><b>RAHASIA</b></td></tr>
 * <tr><td>{@link #getBsiGatewayUrl()}</td><td>BSI Maja API Endpoint</td><td>endpoint</td></tr>
 * <tr><td rowspan="3">Flip</td><td>{@link #getApiKeyFlip()}</td><td>Flip API SECRET KEY</td>
 *     <td><b>RAHASIA</b> (dipakai sebagai HTTP Basic auth)</td></tr>
 * <tr><td>{@link #getTokenFlip()}</td><td>Flip VALIDATION TOKEN</td>
 *     <td><b>RAHASIA</b> (validasi callback)</td></tr>
 * <tr><td>{@link #getBiayaAdminFlip()}</td><td>Biaya Admin Flip</td><td>nominal, bukan rahasia</td></tr>
 * <tr><td rowspan="3">Finpay</td><td>{@link #getApiKeyFinpay()}</td><td>Finpay API SECRET KEY</td>
 *     <td><b>RAHASIA</b></td></tr>
 * <tr><td>{@link #getTokenFinpay()}</td><td>Finpay VALIDATION TOKEN</td><td><b>RAHASIA</b></td></tr>
 * <tr><td>{@link #getBiayaAdminFinpay()}</td><td>Biaya Admin Finpay</td><td>nominal</td></tr>
 * <tr><td rowspan="4">E-Smartlink</td><td>{@link #getUsernameEsmartlink()}</td>
 *     <td>Smartlink Username</td><td>identitas</td></tr>
 * <tr><td>{@link #getPasswordEsmartlink()}</td><td>Smartlink Password</td><td><b>RAHASIA</b></td></tr>
 * <tr><td>{@link #getBiayaAdminEsmartlink()}</td><td>Biaya Admin Smartlink Default</td><td>nominal</td></tr>
 * <tr><td>{@link #getVariableBiayaAdminEsmartlink()}</td><td>Variable Biaya Admin Smartlink</td>
 *     <td>katalog biaya per bank berbentuk CSV; punya nilai bawaan hardcode</td></tr>
 * <tr><td rowspan="10">Online BMT</td><td>{@link #getAktfkanPembayaranViaOnlineBmt()}</td>
 *     <td>Aktifkan Pembayaran Via Online BMT</td><td>sakelar</td></tr>
 * <tr><td>{@link #getOnlineBmtApiKey()}</td><td>API Key</td>
 *     <td><b>RAHASIA</b> &mdash; <b>ini kunci autentikasi endpoint publik</b>, lihat "Jangkauan
 *     endpoint H2H anonim"</td></tr>
 * <tr><td>{@link #getOnlineBmtEncryptionKey()}</td><td>Encryption Key AES</td>
 *     <td><b>RAHASIA</b> (AES-256-CBC)</td></tr>
 * <tr><td>{@link #getOnlineBmtHmacKey()}</td><td>HMAC Key</td>
 *     <td><b>RAHASIA</b> (HMAC-SHA256)</td></tr>
 * <tr><td>{@link #getOnlineBmtPrefixInvoice()}</td><td>Prefix Invoice</td><td>konfigurasi</td></tr>
 * <tr><td>{@link #getOnlineBmtBiayaAdministrasi()}</td><td>Biaya Administrasi</td><td>nominal</td></tr>
 * <tr><td>{@link #getOnlineBmtKodeMitra()} / {@link #getOnlineBmtNamaMitra()}</td>
 *     <td>Fallback Kode/Nama Mitra BMT</td><td>identitas kontraktual</td></tr>
 * <tr><td>{@link #getOnlineBmtKodeMerchant()} / {@link #getOnlineBmtNamaMerchant()}</td>
 *     <td>Fallback Kode/Nama Merchant</td><td>identitas kontraktual</td></tr>
 * <tr><td>{@link #getOnlineBmtRequestTimeTolerance()}</td><td>Toleransi Request 30-3600 Detik</td>
 *     <td>parameter anti-replay</td></tr>
 * <tr><td colspan="3">&nbsp;</td></tr>
 * </table>
 *
 * <h3>&#9888; Kredensial disimpan PLAINTEXT (TERVERIFIKASI, bukan dugaan)</h3>
 * <p>Penelusuran menyeluruh atas jalur tulis dan jalur baca <b>tidak menemukan satu pun</b>
 * pemanggilan enkripsi, hashing, atau masking:</p>
 * <ul>
 *   <li><b>Jalur tulis.</b> {@code KanalPembayaranAction#onSave(Event)} memanggil
 *       {@code setBniPassword(bniPassword.getValue())},
 *       {@code setApiKeyFlip(apiKeyFlip.getValue().trim())},
 *       {@code setOnlineBmtEncryptionKey(OnlineBmtUtil.emptyToNull(...))}, dan seterusnya
 *       &mdash; nilai mentah dari {@code Textbox} langsung masuk ke setter. Setter di kelas ini
 *       hanya melakukan penugasan (<code>this.x = x</code>).</li>
 *   <li><b>Jalur baca.</b> {@code ais.common.BSIMajaUtil} memakai {@code getBsiScretId()} dan
 *       {@code getBsiPassword()} apa adanya sebagai CLIENT_SECRET/PASSWORD HTTP;
 *       {@code DownloadTagihan*BankOnline} membangun header Basic dari
 *       {@code getApiKeyFlip()}/{@code getTokenFlip()};
 *       {@code ais.common.OnlineBmtUtil#overlayChannel} menyalin ketiga kunci Online BMT ke
 *       objek {@code Settings} tanpa transformasi. Tidak ada langkah dekripsi di mana pun,
 *       yang membuktikan tidak ada langkah enkripsi saat menyimpan.</li>
 *   <li><b>Skema kolom.</b> {@code ais.common.InitIndex} membuat kolom sebagai
 *       <code>text</code>/<code>varchar</code> biasa
 *       (<code>online_bmt_api_key text</code>, <code>online_bmt_encryption_key text</code>,
 *       <code>online_bmt_hmac_key text</code>) &mdash; bukan <code>bytea</code>, tanpa pgcrypto.</li>
 *   <li><b>Penggandaan riwayat.</b> Kelas ini beranotasi {@code @Audited} (Hibernate Envers),
 *       sehingga setiap versi lama dari SETIAP kredensial ikut tersalin plaintext ke
 *       <code>sekolah.kanal_pembayaran_aud</code>. <b>Mengganti kunci yang bocor tidak
 *       menghapus kunci lama dari database.</b> Tabel audit itu ditampilkan lewat tombol
 *       "Revisi" ({@code RevisiHelper#createNewRevisi}) yang gerbangnya BUKAN hak menu,
 *       melainkan daftar id/role pada konfigurasi <code>boleh_lihat_revisi</code>
 *       (bawaan <code>"am,amp"</code>).</li>
 * </ul>
 * <p>Pola ini konsisten dengan temuan berulang di seluruh codebase (mis. kredensial polos di
 * jalur H2H yang tercatat lewat {@code LogHostToHostAction}) dan termasuk dalam cakupan task
 * audit "kredensial plaintext" yang sudah ada.</p>
 *
 * <h3>&#9888; Siapa yang dapat membaca/mengubah &mdash; pewarisan hak lewat menu induk</h3>
 * <p><b>Verifikasi ulang dan perluasan temuan batch 61, kali ini dari sisi entity kredensial
 * itu sendiri.</b> Layar CRUD entity ini adalah
 * <code>/WEB-INF/z/x/y/pages/master/sekolah/kanal_pembayaran.zul</code> yang menerapkan
 * {@code KanalPembayaranAction}. Fakta yang diverifikasi:</p>
 * <ol>
 *   <li><b>Layar ini TIDAK terdaftar sebagai menu.</b> Penelusuran
 *       {@code ais.common.MenuInitializer} dan {@code ais.common.MenuSnapshotData} tidak
 *       menemukan satu pun baris yang menunjuk <code>kanal_pembayaran.zul</code>. Tidak ada
 *       entri menu &rArr; tidak ada baris {@code RolePrivilage} yang khusus mengatur siapa
 *       boleh membaca/mengubah kredensial bank.</li>
 *   <li><b>Layar ini disisipkan sebagai tab pada TIGA layar induk</b> (semuanya lewat
 *       {@code MyButtonTabbox#tambahTabLazy}, di dalam halaman induk yang sama &mdash; bukan
 *       iframe dengan konteks menu tersendiri):
 *       <ul>
 *         <li>{@code JenisBiayaSekolahAction} tab 1 "Kanal Pembayaran" &mdash; halaman
 *             <code>jenis_biaya_sekolah.zul</code>, terdaftar di
 *             {@code MenuSnapshotData} sebagai menu <b>"Konfigurasi Invoice"</b>;</li>
 *         <li>{@code JenisKegiatanAction} tab 2 "Kanal Pembayaran" &mdash; menu Jenis Kegiatan;</li>
 *         <li>{@code CaraPembayaranKoperasiAction} tab 1 "Kanal Pembayaran" &mdash; menu Cara
 *             Pembayaran Koperasi.</li>
 *       </ul></li>
 *   <li><b>Gerbang hak memakai menu AKTIF, bukan menu layar ini.</b>
 *       {@code KanalPembayaranAction#doAfterCompose} memanggil
 *       {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)}, dan
 *       {@code CommonPrivilages#checkPrevilages(Integer)} mengambil menu lewat
 *       {@code Common.getCurrentMenu()} &mdash; yaitu menu <i>induk</i> yang sedang dibuka.</li>
 * </ol>
 * <p><b>Akibatnya:</b> hak <i>Create/Update/Delete</i> atas menu "Konfigurasi Invoice" (atau
 * "Jenis Kegiatan", atau "Cara Pembayaran Koperasi") secara otomatis memberi <b>CRUD penuh atas
 * kredensial bank produksi</b>, termasuk membaca nilai <i>secret</i> yang sudah tersimpan:
 * {@code init(KanalPembayaran)} mengisi ulang setiap {@code Textbox} dengan nilai tersimpan
 * (untuk Online BMT bahkan dengan komentar eksplisit bahwa itu disengaja agar penyimpanan form
 * lain tidak menghapus secret). Untuk BNI/BSI/Flip/Finpay/E-Smartlink kotaknya bahkan
 * <b>teks biasa</b> &mdash; hanya tiga kotak Online BMT yang diberi
 * {@code setType("password")}, dan itu pun hanya menyembunyikan karakter di layar sementara
 * nilainya tetap terkirim utuh ke browser. Seorang operator keuangan yang hanya seharusnya
 * mengelola daftar jenis biaya memperoleh akses ini tanpa satu pun keputusan admin yang
 * eksplisit. Batch 61 mencatat satu induk; verifikasi dari sisi entity ini menemukan
 * <b>tiga</b>.</p>
 *
 * <h3>&#9888; Cakupan tenant bersifat fail-open</h3>
 * <p>{@code KanalPembayaranAction#initCriteria(boolean)} <b>tidak memiliki pembatas tenant di
 * sisi server sama sekali</b>. Filter sekolah/yayasan hanya ditambahkan bila combobox pencarian
 * kebetulan berisi pilihan; bila tidak, yang dipasang adalah
 * {@code Restrictions.sqlRestriction("1=1")}. Pengisian combobox dilakukan
 * {@code InitComboUtil#initYayasanDanSekolahDanSemua}, yang menetapkan konteks hanya jika
 * {@code SekolahUtil.getSekolah()} / {@code getYayasan()} atau relasi pada {@code Tbmuser}
 * menghasilkan objek ber-id. Untuk pengguna yang hak aksesnya diatur murni lewat menu tanpa
 * pengikatan sekolah/yayasan, kedua filter runtuh ke <code>1=1</code> dan daftar menampilkan
 * <b>kanal pembayaran milik SELURUH sekolah dan yayasan di instalasi</b> &mdash; masing-masing
 * dengan tombol ubah yang membuka form berisi kredensialnya. Pembatasan yang ada bersifat
 * kosmetik di sisi klien ({@code setDisabledSafe}), bukan pemeriksaan kepemilikan di server.</p>
 * <p>Kontras yang menegaskan bahwa ini memang kelalaian: combobox pemilih kanal di
 * {@code JenisBiayaSekolahAction} justru <b>fail-closed</b> &mdash; ia memakai
 * <code>sekolah IS NULL OR sekolah.id = &lt;sekolah terpilih&gt;</code> dan
 * {@code sqlRestriction("false")} bila sekolah tak teridentifikasi. Jadi memilih kanal dibatasi
 * dengan benar, sementara mendaftar dan mengubah kanal tidak.</p>
 * <p>Jalur kedua yang memperparah: toolbar layar ini memasang
 * {@code Common.uploadData(this, KanalPembayaran.class, "id", "nama", "sekolah", "yayasan",
 * "akun", "keterangan", "aktif")} &mdash; impor Excel <b>tanpa {@code idCrit}</b>. Karena kolom
 * <code>id</code> ikut diimpor dan tidak ada kriteria pembatas baris, satu berkas Excel dapat
 * menunjuk id kanal milik sekolah lain dan memindahkan kepemilikannya
 * (<code>sekolah</code>/<code>yayasan</code>) ke sekolah penyerang &mdash; setelah itu barisnya
 * tampil di daftar dan kredensialnya dapat dibaca lewat form ubah. Tombol ini hanya tampil bila
 * pengguna punya create+update+delete, yang &mdash; sesuai butir sebelumnya &mdash; diwarisi
 * dari menu induk.</p>
 *
 * <h3>Jangkauan endpoint H2H anonim (verifikasi POSITIF, dengan sisi menenangkan)</h3>
 * <p><b>Ya, entity ini terjangkau dari endpoint tanpa otentikasi sesi.</b> Servlet
 * {@code ais.action.servlet.OnlineBmt} melayani POST dari mitra BMT tanpa sesi login. Alur
 * autentikasinya justru <i>bersandar</i> pada tabel ini:
 * {@code OnlineBmtUtil#findCredentialCandidates(String)} menjalankan
 * <code>createCriteria(KanalPembayaran.class).add(eq("aktfkanPembayaranViaOnlineBmt", true))
 * .add(eq("onlineBmtApiKey", &lt;API_KEY dari body&gt;))</code> &mdash; <b>lintas seluruh
 * tenant</b>, karena memang belum diketahui pemiliknya. Kandidat yang cocok dipakai untuk
 * mendekripsi amplop <code>v1.iv.ciphertext.hmac</code>.</p>
 * <p><b>Verifikasi menenangkan:</b> penyalahgunaan lintas-tenant di titik ini <b>dicegah</b>.
 * Setelah dekripsi berhasil, {@code OnlineBmt#process} mencari invoice dari
 * <code>NO_INVOICE</code>, menghitung ulang {@code OnlineBmtUtil.resolveSettings(invoice)}
 * milik pemilik invoice, lalu menuntut {@code candidate.sameSecurity(invoiceSettings)} sebelum
 * memproses; pesan galat pun sengaja tidak membocorkan kandidat mana yang hampir cocok. Jadi
 * kunci sekolah A tidak dapat dipakai membayar/menginkuiri invoice sekolah B. Berbeda dengan
 * 12+ servlet H2H bank lain yang dikonfirmasi anonim pada batch 66, jalur Online BMT ini
 * memiliki pengikatan tenant yang benar.</p>
 * <p>Yang tetap perlu disadari: karena API key adalah <i>satu-satunya</i> faktor untuk memilih
 * kandidat, dan karena kunci itu plaintext di dua tabel (utama + <code>_aud</code>) serta dapat
 * dibaca lewat pewarisan menu di atas, kompromi baris ini setara dengan kompromi kanal
 * pembayaran sekolah tersebut secara penuh.</p>
 *
 * <h3>Warisan: mengapa {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah} muncul lagi</h3>
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}, tetapi kelas
 * induk itu <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO
 * abstrak biasa berisi utilitas ({@code check}, {@code chek}, {@code resolveLazy}) dan
 * kontrak {@code Serializable}. Hibernate karena itu <b>tidak memetakan</b> properti apa pun
 * dari induk. Deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi keliru melainkan keharusan
 * teknis</b>: tanpa itu tabel tidak punya primary key maupun kolom jejak audit. Pola yang sama
 * berlaku di seluruh entity turunan {@code GeneralValueObject}.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit ringan:</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #onUpdate()},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(java.util.Date)}.</li>
 *   <li><b>Identitas &amp; deskripsi:</b> {@link #getId()}, {@link #setId(Long)},
 *       {@link #getNama()}, {@link #setNama(String)}, {@link #getKeterangan()},
 *       {@link #setKeterangan(String)}, {@link #getAktif()}, {@link #setAktif(Boolean)}.</li>
 *   <li><b>Cakupan kepemilikan (tenant) &amp; akuntansi:</b> {@link #getSekolah()},
 *       {@link #setSekolah(Sekolah)}, {@link #getYayasan()}, {@link #setYayasan(Yayasan)},
 *       {@link #getAkun()}, {@link #setAkun(ais.database.model.akunting.Akun)}.</li>
 *   <li><b>Kredensial per penyedia:</b> lihat tabel di atas &mdash; blok BNI, BSI Maja, Flip,
 *       Finpay, E-Smartlink, dan Online BMT.</li>
 * </ul>
 * <p>Tidak ada method bisnis (perhitungan, pencarian, atau posting) di kelas ini; seluruh
 * logika pemakaian kredensial berada di {@code OnlineBmtUtil}, {@code BSIMajaUtil},
 * {@code PembayaranOnline}, dan keluarga {@code DownloadTagihan*}.</p>
 *
 * <h3>Hal-hal non-obvious</h3>
 * <ul>
 *   <li><b>{@link #getYayasan()} adalah getter yang menulis balik.</b> Ia menimpa field
 *       {@code yayasan} dengan {@code getSekolah().getYayasan()} setiap kali dipanggil. Dengan
 *       {@code dynamicUpdate = true} dan objek yang masih ter-<i>attach</i> pada sesi Hibernate,
 *       sekadar MEMBACA properti ini dapat menghasilkan UPDATE yang memindahkan kanal ke
 *       yayasan lain. Lihat javadoc getter tersebut.</li>
 *   <li><b>Getter kredensial tidak pernah mengembalikan {@code null}.</b> Sebagian besar
 *       mengembalikan {@code ""} dan memangkas spasi, sehingga pemanggil membedakan "belum
 *       diatur" dengan {@code isEmpty()}, bukan {@code == null}. Kelompok Online BMT sengaja
 *       menyimpang: getternya mengembalikan {@code null} apa adanya agar resolver dapat
 *       membedakan "tidak diatur" (mewarisi induk) dari "sengaja dikosongkan".</li>
 *   <li><b>{@link #getAktif()} default {@code true}.</b> Baris lama dengan kolom NULL dianggap
 *       AKTIF &mdash; kebalikan dari sakelar Online BMT yang default OFF.</li>
 *   <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} menolak nilai kosong secara
 *       diam-diam</b> (guard {@code return} di awal), sehingga jejak "diubah oleh" tidak dapat
 *       dikosongkan sekali terisi.</li>
 *   <li><b>Salah eja yang sudah terlanjur menjadi kontrak:</b> {@code bsiScretId} (mestinya
 *       "Secret") dan {@code aktfkanPembayaran...} (mestinya "aktifkan") muncul di nama
 *       properti Hibernate dan nama kolom; mengganti namanya berarti migrasi skema.</li>
 *   <li><b>Komentar hbm2java yang menyesatkan.</b> Header kelas hasil generator semula
 *       tertulis "JenisGuru generated by hbm2java" &mdash; sisa salin-tempel dari entity lain,
 *       tidak ada hubungannya dengan kanal pembayaran.</li>
 *   <li><b>Filter mati di layar.</b> <code>kanal_pembayaran.zul</code> memuat checkbox
 *       {@code searchaktif} berlabel "Tampilkan hanya yang aktif" dalam keadaan tercentang,
 *       tetapi {@code KanalPembayaranAction} tidak mendeklarasikan field itu dan
 *       {@code initCriteria} tidak pernah memfilter {@code aktif}. Kanal nonaktif tetap
 *       tampil.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.JenisBiayaSekolah
 * @see ais.database.model.sekolah.Sekolah
 * @see ais.database.model.akunting.Akun
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "kanal_pembayaran", schema = "sekolah")
public class KanalPembayaran extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Dibutuhkan karena
	 * {@link ais.database.model.GeneralValueObject} mengimplementasikan
	 * {@link java.io.Serializable} dan instance entity ikut tersimpan dalam state
	 * desktop ZK. Nilai tetap agar sesi yang sudah berjalan tidak rusak saat kelas
	 * dikompilasi ulang tanpa perubahan bentuk.
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Primary key tabel <code>sekolah.kanal_pembayaran</code>; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris kanal pembayaran ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna yang mengubah baris ini.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} atau string kosong/berisi spasi
	 * <b>diabaikan diam-diam</b> (method langsung {@code return} tanpa menulis apa pun),
	 * sehingga jejak audit yang sudah terisi tidak dapat dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna; nilai kosong tidak menimbulkan perubahan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}
	 * atau kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong tidak menimbulkan perubahan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris kanal pembayaran ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA sekaligus deklarasi field {@code tanggal_dirubah}
	 * (keduanya sengaja ditulis pada satu baris oleh generator).
	 *
	 * <p><b>{@code onUpdate()}</b> dipanggil Hibernate tepat sebelum setiap UPDATE atas
	 * baris ini dan mendelegasikan pencatatan stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Karena kelas
	 * ini beranotasi {@code @Audited}, UPDATE yang sama juga menerbitkan satu revisi baru
	 * pada tabel <code>sekolah.kanal_pembayaran_aud</code> &mdash;
	 * <b>termasuk salinan plaintext seluruh kredensial pada saat itu</b>.</p>
	 *
	 * <p><b>Field {@code tanggal_dirubah}</b> diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil#getDate()} saat objek dibuat, sehingga baris baru yang
	 * belum pernah di-update tetap memiliki stempel waktu.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi &mdash; nilainya diisi otomatis oleh
	 * {@link #onUpdate()}. Tersedia agar Hibernate dapat memuat nilai dari database dan
	 * agar proses impor dapat menetapkan waktu historis.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini
	 * (kolom bertipe {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat
	 *         lewat konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik kanal; {@code null} = kanal milik yayasan/global. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik kanal; diturunkan dari sekolah bila ada. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Keterangan bebas yang tampil di daftar dan di combobox pemilih kanal. */
	private String keterangan;
	/** Nama kanal (wajib); identitas yang dilihat operator saat memilih kanal. */
	private String nama;

	/** Sakelar aktif kanal; {@code null} diperlakukan AKTIF oleh {@link #getAktif()}. */
	private Boolean aktif;

	/*
	 * ------------------------------------------------------------------
	 * BLOK KREDENSIAL BNI (Host-to-Host / Virtual Account)
	 * Seluruh nilai disimpan PLAINTEXT; lihat peringatan pada javadoc kelas.
	 * ------------------------------------------------------------------
	 */
	/** Merchant/kode BNI; boleh berformat per angkatan. Lihat {@link #getBniMerchantId()}. */
	private String bniMerchantId;
	private String bniPassword;
	private String bniGatewayUrl;

	/*
	 * ------------------------------------------------------------------
	 * BLOK KREDENSIAL BSI MAJA (OAuth client + basic auth)
	 * ------------------------------------------------------------------
	 */
	/** Client ID BSI Maja. Lihat {@link #getBsiMerchantId()}. */
	private String bsiMerchantId;
	private String bsiScretId;
	private String bsiUsername;
	private String bsiPassword;
	private String bsiGatewayUrl;

	/*
	 * ------------------------------------------------------------------
	 * BLOK FLIP (agregator; API key dipakai sebagai HTTP Basic auth)
	 * ------------------------------------------------------------------
	 */
	/** Sakelar aktif pembayaran via Flip; {@code null} = OFF. */
	private Boolean aktfkanPembayaranViaFlip;
	private String apiKeyFlip;
	private String tokenFlip;
	private Double biayaAdminFlip;

	/*
	 * ------------------------------------------------------------------
	 * BLOK FINPAY (agregator; bentuk konfigurasi identik dengan Flip)
	 * ------------------------------------------------------------------
	 */
	/** Sakelar aktif pembayaran via Finpay; {@code null} = OFF. */
	private Boolean aktfkanPembayaranViaFinpay;
	private String apiKeyFinpay;
	private String tokenFinpay;
	private Double biayaAdminFinpay;

	/*
	 * ------------------------------------------------------------------
	 * BLOK E-SMARTLINK dan ONLINE BMT
	 * ------------------------------------------------------------------
	 */
	/** Sakelar aktif pembayaran via E-Smartlink; {@code null} = OFF. */
	private Boolean aktfkanPembayaranViaEsmartlink;
	/** Sakelar Online BMT khusus kanal; null berarti OFF. */
	private Boolean aktfkanPembayaranViaOnlineBmt;
	private String onlineBmtPrefixInvoice;
	private Double onlineBmtBiayaAdministrasi;
	private String onlineBmtKodeMitra;
	private String onlineBmtNamaMitra;
	private String onlineBmtKodeMerchant;
	private String onlineBmtNamaMerchant;
	private String onlineBmtApiKey;
	private String onlineBmtEncryptionKey;
	private String onlineBmtHmacKey;
	private Integer onlineBmtRequestTimeTolerance;
	private String usernameEsmartlink;
	private String passwordEsmartlink;
	private Double biayaAdminEsmartlink;
	private String variableBiayaAdminEsmartlink;
	private Akun akun;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JavaBeans.
	 *
	 * <p>Dipakai juga oleh {@code KanalPembayaranAction#onAdd(Event)} untuk membuka form
	 * "Tambah Kanal Pembayaran" dengan objek kosong. Seluruh sakelar penyedia dibiarkan
	 * {@code null} sehingga getternya melaporkan OFF, kecuali {@link #getAktif()} yang
	 * melaporkan AKTIF.</p>
	 */
	public KanalPembayaran() {
	}

	/**
	 * Konstruktor ringkas untuk membentuk referensi kanal dengan identitas minimal
	 * (id dan nama), tanpa memuat kredensial apa pun.
	 *
	 * @param id   primary key kanal
	 * @param nama nama kanal
	 */
	public KanalPembayaran(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Primary key baris kanal pembayaran.
	 *
	 * <p>Kolom {@code id} bertipe IDENTITY dan {@code insertable = false}: nilainya
	 * ditentukan sepenuhnya oleh database. Id inilah yang disimpan pada kolom
	 * <code>kanal_pembayaran</code> milik {@link ais.database.model.sekolah.JenisBiayaSekolah},
	 * {@link ais.database.model.sekolah.Sekolah}, {@code JenisKegiatan},
	 * {@code CaraPembayaranKoperasi}, dan {@code VirtualAccountBank}.</p>
	 *
	 * @return primary key, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Dipanggil Hibernate saat memuat entity; kode aplikasi
	 * tidak boleh menetapkannya sendiri untuk baris baru.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Sekolah pemilik kanal ini &mdash; penentu <b>cakupan tenant</b> baris kredensial.
	 *
	 * <p>Nilai {@code null} berarti kanal tidak dimiliki sekolah tertentu (kanal tingkat
	 * yayasan atau global). Perbedaan ini bermakna langsung: combobox pemilih kanal pada
	 * {@code JenisBiayaSekolahAction} menerima kanal bila
	 * <code>sekolah IS NULL OR sekolah.id = &lt;sekolah terpilih&gt;</code>, sehingga kanal
	 * ber-{@code sekolah} {@code null} dapat dipakai oleh semua sekolah.</p>
	 *
	 * <p><b>Efek samping (write-back):</b> getter memanggil {@code check(sekolah)} dari
	 * {@link ais.database.model.GeneralValueObject} dan <b>menugaskan ulang hasilnya ke
	 * field</b>. {@code check} dapat mengganti proxy lazy yang sudah detached dengan objek
	 * kanonik dari {@code EntityIdentityMap}/cache atau hasil pembacaan ulang lewat sesi
	 * baru. Membaca properti ini karena itu tidak sepenuhnya bebas efek samping.</p>
	 *
	 * <p><b>Catatan cakupan:</b> pemilik tenant di sini <b>tidak</b> menjadi pembatas
	 * otomatis pada layar CRUD &mdash; {@code KanalPembayaranAction#initCriteria} tidak
	 * memasang pembatas tenant sisi server. Lihat bagian fail-open pada javadoc kelas.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila kanal berlaku lintas sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik kanal.
	 *
	 * <p><b>Normalisasi penting:</b> objek {@code Sekolah} yang ber-id {@code null}
	 * (mis. hasil {@code new Sekolah()} yang dikembalikan {@code SekolahUtil.getSekolah()}
	 * saat konteks tidak dikenal) <b>diubah menjadi {@code null} yang sesungguhnya</b>.
	 * Tanpa normalisasi ini Hibernate akan mencoba menyimpan objek transient dan gagal;
	 * dengan normalisasi ini, konteks tenant yang tidak dikenal berakhir sebagai kanal
	 * tanpa pemilik (berlaku lintas sekolah) &mdash; perilaku yang <b>menguntungkan
	 * ketersediaan, bukan keamanan</b>, dan perlu diingat saat menilai cakupan.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id berarti tanpa pemilik
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Yayasan pemilik kanal ini.
	 *
	 * <p><b>&#9888; Getter destruktif (write-back).</b> Method ini bukan pembaca murni. Urutan
	 * kerjanya: (1) memanggil {@link #getSekolah()} dan <b>menugaskan hasilnya ke field
	 * {@code sekolah}</b>; (2) bila sekolah tidak {@code null}, <b>menimpa field
	 * {@code yayasan} dengan {@code sekolah.getYayasan()}</b> &mdash; membuang nilai yayasan
	 * yang tersimpan di kolom <code>yayasan_id</code>; (3) menormalkan hasilnya lewat
	 * {@code check(yayasan)} dan menugaskannya lagi.</p>
	 *
	 * <p><b>Mengapa ini berbahaya pada entity kredensial.</b> Kelas dianotasi
	 * {@code dynamicUpdate = true}. Bila objek masih ter-<i>attach</i> pada sesi Hibernate
	 * yang akan di-flush, perubahan field yang dilakukan getter ini ikut terdeteksi sebagai
	 * <i>dirty</i> dan diterbitkan sebagai UPDATE. Artinya <b>sekadar merender daftar atau
	 * mencetak laporan yang membaca properti {@code yayasan} dapat memindahkan kepemilikan
	 * baris kredensial</b> ke yayasan milik sekolahnya, dan &mdash; karena {@code @Audited}
	 * &mdash; menambah satu revisi baru berisi salinan seluruh kredensial. Perpindahan ini
	 * mengubah baris mana yang lolos filter <code>yayasan</code> pada pencarian, sehingga
	 * berdampak langsung pada siapa yang melihat kredensial tersebut. Ini varian dari pola
	 * getter write-back yang tercatat pada {@code Sekolah} (8 getter, termasuk
	 * {@code getDomain()}).</p>
	 *
	 * <p>Konsekuensi fungsional lain: selama {@code sekolah} tidak {@code null}, kolom
	 * <code>yayasan_id</code> yang tersimpan <b>tidak pernah menang</b> &mdash; yayasan selalu
	 * diturunkan dari sekolah. Menyetel yayasan berbeda dari yayasan sekolahnya tidak akan
	 * bertahan sampai pembacaan berikutnya.</p>
	 *
	 * @return yayasan pemilik (diturunkan dari sekolah bila ada), atau {@code null}
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
	 * Menetapkan yayasan pemilik kanal.
	 *
	 * <p>Sama seperti {@link #setSekolah(Sekolah)}, objek {@code Yayasan} tanpa id
	 * dinormalkan menjadi {@code null} agar Hibernate tidak menemui referensi transient.</p>
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini akan <b>ditimpa</b> oleh
	 * {@link #getYayasan()} pada pembacaan berikutnya bila {@link #getSekolah()} tidak
	 * {@code null}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id berarti tanpa pemilik
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Keterangan bebas kanal.
	 *
	 * <p>Ditampilkan sebagai kolom "Keterangan" pada daftar dan sebagai deskripsi item pada
	 * combobox pemilih kanal di {@code JenisBiayaSekolahAction}. Bukan data rahasia, tetapi
	 * perlu diingat bahwa isian bebas semacam ini kerap dipakai operator untuk mencatat
	 * hal-hal yang seharusnya tidak dituliskan (mis. potongan kredensial) &mdash; dan kolom
	 * ini termasuk kolom yang ikut tercetak/terunduh Excel.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan kanal.
	 *
	 * @param keterangan teks bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama kanal pembayaran; kolom {@code nullable = false}.
	 *
	 * <p>Nama inilah yang divalidasi wajib-isi oleh {@code KanalPembayaranAction#onSave},
	 * dipakai sebagai label item combobox pemilih kanal, dan sebagai label tombol "Revisi"
	 * pada baris daftar.</p>
	 *
	 * @return nama kanal
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama kanal.
	 *
	 * <p>Tidak melakukan validasi apa pun &mdash; kewajiban isi ditegakkan di layar
	 * ({@code onSave} menolak nama kosong), bukan di entity. Tidak ada batasan keunikan
	 * nama, baik di entity maupun di skema.</p>
	 *
	 * @param nama nama kanal
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Akun kas/bank ({@link ais.database.model.akunting.Akun}) tempat penerimaan lewat kanal
	 * ini dibukukan.
	 *
	 * <p>Wajib diisi di layar ({@code onSave} menolak akun kosong) meskipun kolomnya
	 * {@code nullable = true} di skema. Relasi ini yang menyambungkan kanal pembayaran ke
	 * modul akuntansi.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(akun)} dan menugaskan ulang hasilnya
	 * ke field &mdash; write-back ringan yang sama dengan {@link #getSekolah()}.</p>
	 *
	 * @return akun kas/bank, atau {@code null} untuk baris lama yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_id", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun; 
	}

	/**
	 * Menetapkan akun kas/bank pembukuan kanal ini.
	 *
	 * <p>Berbeda dengan {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}, setter ini
	 * <b>tidak</b> menormalkan objek ber-id {@code null} menjadi {@code null}. Pemanggil
	 * bertanggung jawab menyerahkan {@code Akun} yang sudah tersimpan.</p>
	 *
	 * @param akun akun kas/bank; boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}
	
	/**
	 * Sakelar aktif kanal.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} diperlakukan sebagai <b>AKTIF</b>
	 * ({@code aktif == null ? true : aktif}) &mdash; kebalikan dari sakelar Online BMT yang
	 * memperlakukan {@code null} sebagai OFF. Baris lama yang dibuat sebelum kolom ini ada
	 * karena itu langsung ikut terpilih.</p>
	 *
	 * <p>Dipakai sebagai filter oleh combobox pemilih kanal di
	 * {@code JenisBiayaSekolahAction} ({@code Restrictions.eq("aktif", true)}) &mdash;
	 * perhatikan bahwa filter itu membandingkan <b>kolom</b>, bukan hasil getter, sehingga
	 * baris ber-kolom NULL justru <b>tidak</b> lolos filter walaupun getter melaporkannya
	 * aktif. Layar daftar kanal sendiri tidak pernah memfilter kolom ini (checkbox
	 * "Tampilkan hanya yang aktif" pada ZUL tidak terhubung ke kode).</p>
	 *
	 * @return {@code true} bila kanal aktif atau kolomnya belum pernah diisi
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif kanal.
	 *
	 * <p>Dipanggil langsung dari checkbox pada baris daftar
	 * ({@code KanalPembayaranRenderer#render}), yang segera menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate} &mdash; menonaktifkan/mengaktifkan kanal tidak
	 * memerlukan pembukaan form maupun konfirmasi.</p>
	 *
	 * @param aktif {@code true} = aktif; {@code null} juga dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Kode merchant BNI untuk kanal ini (label layar: "BNI Merchant").
	 *
	 * <p><b>Format khusus:</b> keterangan pada layar menjelaskan nilai boleh dipecah per
	 * angkatan dengan pola <code>{ANGKATAN}:{KODE_BNI};{ANGKATAN}:{KODE_BNI}</code>, contoh
	 * <code>2019:8979;2020:8977</code>. Jadi satu kolom dapat memuat beberapa kode sekaligus
	 * dan pemanggil wajib mengurai isinya, bukan memakainya mentah-mentah.</p>
	 *
	 * @return kode merchant, atau string kosong (bukan {@code null}) bila belum diisi;
	 *         spasi di ujung selalu dipangkas
	 */
	@Column(name = "bni_merchant_id", nullable = true)
	public String getBniMerchantId() {
		return bniMerchantId == null ? "" : bniMerchantId.trim();
	}

	/**
	 * Menetapkan kode merchant BNI apa adanya, tanpa pemangkasan maupun validasi format.
	 *
	 * @param bniMerchantId kode merchant (boleh berformat per angkatan)
	 */
	public void setBniMerchantId(String bniMerchantId) {
		this.bniMerchantId = bniMerchantId;
	}

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> Password Host-to-Host BNI (label layar:
	 * "BNI Password").
	 *
	 * <p>Nilai dipakai apa adanya sebagai kunci pada jalur H2H BNI (lihat
	 * {@code ais.action.servlet.Bniresponse}). Seperti {@link #getBniMerchantId()}, nilainya
	 * boleh dipecah per angkatan dengan pola
	 * <code>{ANGKATAN}:{PASSWORD_BNI};{ANGKATAN}:{PASSWORD_BNI}</code>.</p>
	 *
	 * <p><b>Disimpan plaintext</b> di kolom <code>bni_password</code> dan disalin plaintext
	 * ke tabel revisi <code>kanal_pembayaran_aud</code>. Kotak isian pada layar adalah
	 * {@code Textbox} biasa (bukan bertipe password), sehingga nilainya tampil terbaca bagi
	 * siapa pun yang dapat membuka form ubah.</p>
	 *
	 * @return password, atau string kosong bila belum diisi; spasi di ujung dipangkas
	 */
	@Column(name = "bni_password", nullable = true)
	public String getBniPassword() {
		return bniPassword == null ? "" : bniPassword.trim();
	}

	/**
	 * <b>&#9888;</b> Menyimpan password H2H BNI apa adanya (tanpa enkripsi, hashing, maupun
	 * pemangkasan).
	 *
	 * @param bniPassword password H2H BNI
	 */
	public void setBniPassword(String bniPassword) {
		this.bniPassword = bniPassword;
	}

	/**
	 * URL gateway BNI untuk kanal ini.
	 *
	 * <p>Barisnya di form ubah dibuat {@code setVisible(false)} oleh
	 * {@code KanalPembayaranAction}, jadi praktis tidak dapat diubah operator lewat UI
	 * standar &mdash; namun {@code onSave} tetap menuliskannya kembali dari komponen
	 * tersembunyi itu.</p>
	 *
	 * @return URL gateway, atau string kosong bila belum diisi
	 */
	@Column(name = "bni_gateway_url", nullable = true)
	public String getBniGatewayUrl() {
		return bniGatewayUrl == null ? "" : bniGatewayUrl.trim();
	}

	/**
	 * Menetapkan URL gateway BNI.
	 *
	 * @param bniGatewayUrl URL endpoint BNI
	 */
	public void setBniGatewayUrl(String bniGatewayUrl) {
		this.bniGatewayUrl = bniGatewayUrl;
	}

	/**
	 * Client ID BSI Maja (label layar: "BSI Maja ClientID").
	 *
	 * <p>Pasangan dari {@link #getBsiScretId()}; dipakai {@code ais.common.BSIMajaUtil}
	 * untuk memperoleh token akses. Resolusi nilainya berlapis: {@code BSIMajaUtil} memakai
	 * nilai milik {@link ais.database.model.sekolah.Sekolah} lebih dulu, lalu
	 * <b>menimpanya</b> dengan nilai kanal bila kanal mengisi field yang sama.</p>
	 *
	 * @return client ID, atau string kosong bila belum diisi
	 */
	@Column(name = "bsi_merchant_id", nullable = true)
	public String getBsiMerchantId() {
		return bsiMerchantId == null ? "" : bsiMerchantId.trim();
	}

	/**
	 * Menetapkan client ID BSI Maja.
	 *
	 * @param bsiMerchantId client ID
	 */
	public void setBsiMerchantId(String bsiMerchantId) {
		this.bsiMerchantId = bsiMerchantId;
	}

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> Client secret BSI Maja (label layar:
	 * "BSI Maja SecretKey").
	 *
	 * <p>Nama properti salah eja &mdash; "Scret" seharusnya "Secret" &mdash; namun sudah
	 * menjadi bagian kontrak pemetaan Hibernate. Nilai dibaca
	 * {@code ais.common.BSIMajaUtil} sebagai {@code CLIENT_SECRET} dan dipakai langsung.
	 * <b>Plaintext</b>, kotak isian layar bukan bertipe password.</p>
	 *
	 * <p>Perhatikan pola resolusi di {@code BSIMajaUtil}: kredensial kanal <b>menimpa</b>
	 * kredensial sekolah hanya bila tidak kosong ({@code !isEmpty()}). Karena getter ini
	 * mengembalikan {@code ""} alih-alih {@code null}, "kosong" dan "tidak diatur" tidak
	 * dapat dibedakan &mdash; yang di sini justru menghasilkan perilaku pewarisan yang
	 * diinginkan.</p>
	 *
	 * @return client secret, atau string kosong bila belum diisi
	 */
	public String getBsiScretId() {
		return bsiScretId == null ? "" : bsiScretId.trim();
	}

	/**
	 * <b>&#9888;</b> Menyimpan client secret BSI Maja apa adanya (tanpa enkripsi).
	 *
	 * @param bsiScretId client secret BSI Maja
	 */
	public void setBsiScretId(String bsiScretId) {
		this.bsiScretId = bsiScretId;
	}

	/**
	 * Username BSI Maja (label layar: "BSI Maja username") &mdash; separuh dari pasangan
	 * kredensial basic auth bersama {@link #getBsiPassword()}.
	 *
	 * <p><b>&#9888; Nilai ini bocor keluar dari lingkungan admin.</b> Selain sebagai
	 * kredensial, nilainya dipakai sebagai <b>prefix nomor Virtual Account</b> yang
	 * ditampilkan kepada siswa/wali. Jalur yang diverifikasi:
	 * {@code ais.action.servlet.api.TagihanSiswa} (<code>va = kanalPembayaran.getBsiUsername()
	 * + va</code>), {@code ais.action.servlet.api.TopupHelper}, dan
	 * {@code ais.action.master.sekolah.helper.PembayaranOnline}. Karena itu nilai ini harus
	 * dianggap <b>semi-publik</b>: siapa pun yang dapat melihat tagihannya sendiri lewat API
	 * juga dapat menyimpulkan username BSI sekolahnya. Menyimpan rahasia sungguhan di field
	 * ini adalah kesalahan.</p>
	 *
	 * @return username, atau string kosong bila belum diisi
	 */
	public String getBsiUsername() {
		return bsiUsername == null ? "" : bsiUsername.trim();
	}

	/**
	 * Menetapkan username BSI Maja.
	 *
	 * <p>Ingat konsekuensi ganda dari field ini: nilainya juga menjadi prefix nomor VA yang
	 * ditampilkan ke siswa (lihat {@link #getBsiUsername()}), sehingga mengubahnya mengubah
	 * bentuk nomor VA yang diterbitkan.</p>
	 *
	 * @param bsiUsername username BSI Maja
	 */
	public void setBsiUsername(String bsiUsername) {
		this.bsiUsername = bsiUsername;
	}

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> Password BSI Maja (label layar:
	 * "BSI Maja password").
	 *
	 * <p>Dibaca {@code ais.common.BSIMajaUtil} sebagai {@code PASSWORD} dan dikirim apa
	 * adanya ke endpoint BSI. <b>Plaintext</b> di kolom <code>bsi_password</code>, kotak
	 * isian layar bukan bertipe password.</p>
	 *
	 * @return password, atau string kosong bila belum diisi
	 */
	@Column(name = "bsi_password", nullable = true)
	public String getBsiPassword() {
		return bsiPassword == null ? "" : bsiPassword.trim();
	}

	/**
	 * <b>&#9888;</b> Menyimpan password BSI Maja apa adanya (tanpa enkripsi).
	 *
	 * @param bsiPassword password BSI Maja
	 */
	public void setBsiPassword(String bsiPassword) {
		this.bsiPassword = bsiPassword;
	}

	/**
	 * Endpoint API BSI Maja (label layar: "BSI Maja API Endpoint").
	 *
	 * <p>Bukan rahasia, tetapi termasuk data berdampak keamanan: mengubahnya mengarahkan
	 * seluruh permintaan berkredensial kanal ini ke host lain. Tidak ada validasi bentuk URL
	 * maupun daftar host yang diizinkan, baik di entity maupun di {@code onSave}.</p>
	 *
	 * @return URL endpoint, atau string kosong bila belum diisi
	 */
	@Column(name = "bsi_gateway_url", nullable = true)
	public String getBsiGatewayUrl() {
		return bsiGatewayUrl == null ? "" : bsiGatewayUrl.trim();
	}

	/**
	 * Menetapkan endpoint API BSI Maja. Tidak ada validasi bentuk URL.
	 *
	 * @param bsiGatewayUrl URL endpoint BSI Maja
	 */
	public void setBsiGatewayUrl(String bsiGatewayUrl) {
		this.bsiGatewayUrl = bsiGatewayUrl;
	}

	/**
	 * Sakelar aktif pembayaran via Flip untuk kanal ini.
	 *
	 * <p>Nilai {@code null} diperlakukan <b>OFF</b>, sehingga baris lama tidak tiba-tiba
	 * mengaktifkan kanal baru.</p>
	 *
	 * @return {@code true} bila pembayaran via Flip diaktifkan
	 */
	public Boolean getAktfkanPembayaranViaFlip() {
		return aktfkanPembayaranViaFlip == null ? false : aktfkanPembayaranViaFlip;
	}

	/**
	 * Mengaktifkan/menonaktifkan pembayaran via Flip pada kanal ini.
	 *
	 * @param aktfkanPembayaranViaFlip {@code true} untuk mengaktifkan (perhatikan salah eja
	 *        pada nama properti: "aktfkan")
	 */
	public void setAktfkanPembayaranViaFlip(Boolean aktfkanPembayaranViaFlip) {
		this.aktfkanPembayaranViaFlip = aktfkanPembayaranViaFlip;
	}

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> API secret key Flip (label layar:
	 * "Flip API SECRET KEY").
	 *
	 * <p>Dipakai langsung sebagai kredensial HTTP Basic oleh keluarga
	 * {@code DownloadTagihan*BankOnline} (mis.
	 * {@code getBasicAuthenticationHeader(kanalPembayaran.getApiKeyFlip(),
	 * kanalPembayaran.getTokenFlip())}). <b>Plaintext</b>; kotak isian layar bukan bertipe
	 * password.</p>
	 *
	 * <p>Berbeda dengan getter BNI/BSI, getter ini <b>tidak</b> memangkas spasi &mdash;
	 * pemangkasan dilakukan di {@code onSave} saat menyimpan.</p>
	 *
	 * @return API secret key, atau string kosong bila belum diisi
	 */
	public String getApiKeyFlip() {
		return apiKeyFlip == null ? "" : apiKeyFlip;
	}

	/**
	 * <b>&#9888;</b> Menyimpan API secret key Flip apa adanya (tanpa enkripsi).
	 *
	 * @param apiKeyFlip API secret key Flip
	 */
	public void setApiKeyFlip(String apiKeyFlip) {
		this.apiKeyFlip = apiKeyFlip;
	}

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> Validation token Flip (label layar:
	 * "Flip VALIDATION TOKEN") &mdash; dipakai untuk memvalidasi callback dari Flip dan
	 * sebagai bagian header Basic bersama {@link #getApiKeyFlip()}.
	 *
	 * <p><b>Plaintext</b>; tidak dipangkas oleh getter.</p>
	 *
	 * @return validation token, atau string kosong bila belum diisi
	 */
	public String getTokenFlip() {
		return tokenFlip == null ? "" : tokenFlip;
	}

	/**
	 * <b>&#9888;</b> Menyimpan validation token Flip apa adanya (tanpa enkripsi).
	 *
	 * @param tokenFlip validation token Flip
	 */
	public void setTokenFlip(String tokenFlip) {
		this.tokenFlip = tokenFlip;
	}

	/**
	 * Biaya administrasi yang ditambahkan pada transaksi lewat Flip.
	 *
	 * <p>Bukan kredensial. Nilai {@code null} dilaporkan sebagai {@code 0.0}, sehingga
	 * "belum diatur" dan "sengaja nol" tidak dapat dibedakan lewat getter ini &mdash;
	 * berbeda dengan {@link #getOnlineBmtBiayaAdministrasi()} yang sengaja mempertahankan
	 * {@code null} untuk keperluan pewarisan.</p>
	 *
	 * @return biaya admin dalam satuan mata uang; {@code 0.0} bila belum diatur
	 */
	public Double getBiayaAdminFlip() {
		return biayaAdminFlip == null ? 0.0 : biayaAdminFlip;
	}

	/**
	 * Menetapkan biaya administrasi Flip. Tidak ada validasi nilai negatif.
	 *
	 * @param biayaAdminFlip nominal biaya administrasi
	 */
	public void setBiayaAdminFlip(Double biayaAdminFlip) {
		this.biayaAdminFlip = biayaAdminFlip;
	}

	/**
	 * Sakelar aktif pembayaran via Finpay untuk kanal ini; {@code null} = OFF.
	 *
	 * @return {@code true} bila pembayaran via Finpay diaktifkan
	 */
	public Boolean getAktfkanPembayaranViaFinpay() {
		return aktfkanPembayaranViaFinpay == null ? false : aktfkanPembayaranViaFinpay;
	}

	/**
	 * Mengaktifkan/menonaktifkan pembayaran via Finpay pada kanal ini.
	 *
	 * @param aktfkanPembayaranViaFinpay {@code true} untuk mengaktifkan
	 */
	public void setAktfkanPembayaranViaFinpay(Boolean aktfkanPembayaranViaFinpay) {
		this.aktfkanPembayaranViaFinpay = aktfkanPembayaranViaFinpay;
	}

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> API secret key Finpay (label layar:
	 * "Finpay API SECRET KEY").
	 *
	 * <p>Bentuk dan pemakaiannya cermin dari {@link #getApiKeyFlip()}: dipakai sebagai
	 * kredensial HTTP Basic pada jalur unduh/terbit tagihan. <b>Plaintext</b>, tidak
	 * dipangkas getter, kotak isian layar bukan bertipe password.</p>
	 *
	 * @return API secret key, atau string kosong bila belum diisi
	 */
	public String getApiKeyFinpay() {
		return apiKeyFinpay == null ? "" : apiKeyFinpay;
	}

	/**
	 * <b>&#9888;</b> Menyimpan API secret key Finpay apa adanya (tanpa enkripsi).
	 *
	 * @param apiKeyFinpay API secret key Finpay
	 */
	public void setApiKeyFinpay(String apiKeyFinpay) {
		this.apiKeyFinpay = apiKeyFinpay;
	}

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> Validation token Finpay (label layar:
	 * "Finpay VALIDATION TOKEN"), pasangan dari {@link #getApiKeyFinpay()}.
	 *
	 * @return validation token, atau string kosong bila belum diisi
	 */
	public String getTokenFinpay() {
		return tokenFinpay == null ? "" : tokenFinpay;
	}

	/**
	 * <b>&#9888;</b> Menyimpan validation token Finpay apa adanya (tanpa enkripsi).
	 *
	 * @param tokenFinpay validation token Finpay
	 */
	public void setTokenFinpay(String tokenFinpay) {
		this.tokenFinpay = tokenFinpay;
	}

	/**
	 * Biaya administrasi yang ditambahkan pada transaksi lewat Finpay; {@code null}
	 * dilaporkan sebagai {@code 0.0}.
	 *
	 * @return nominal biaya administrasi
	 */
	public Double getBiayaAdminFinpay() {
		return biayaAdminFinpay == null ? 0.0 : biayaAdminFinpay;
	}

	/**
	 * Menetapkan biaya administrasi Finpay. Tidak ada validasi nilai negatif.
	 *
	 * @param biayaAdminFinpay nominal biaya administrasi
	 */
	public void setBiayaAdminFinpay(Double biayaAdminFinpay) {
		this.biayaAdminFinpay = biayaAdminFinpay;
	}

	/**
	 * Sakelar aktif pembayaran via E-Smartlink untuk kanal ini; {@code null} = OFF.
	 *
	 * @return {@code true} bila pembayaran via E-Smartlink diaktifkan
	 */
	public Boolean getAktfkanPembayaranViaEsmartlink() {
		return aktfkanPembayaranViaEsmartlink == null ? false : aktfkanPembayaranViaEsmartlink;
	}

	/**
	 * Mengaktifkan/menonaktifkan pembayaran via E-Smartlink pada kanal ini.
	 *
	 * @param aktfkanPembayaranViaEsmartlink {@code true} untuk mengaktifkan
	 */
	public void setAktfkanPembayaranViaEsmartlink(Boolean aktfkanPembayaranViaEsmartlink) {
		this.aktfkanPembayaranViaEsmartlink = aktfkanPembayaranViaEsmartlink;
	}

	/**
	 * Sakelar lapis kanal untuk Online BMT.
	 *
	 * <p>Nilai {@code null} dari database lama selalu diperlakukan OFF sehingga aktivasi
	 * harus merupakan keputusan admin yang eksplisit &mdash; kebalikan dari
	 * {@link #getAktif()} yang memperlakukan {@code null} sebagai aktif.</p>
	 *
	 * <p><b>Berdampak keamanan:</b> hanya baris dengan sakelar ini bernilai {@code true}
	 * yang ikut dipindai {@code OnlineBmtUtil#findCredentialCandidates(String)} saat
	 * endpoint publik {@code ais.action.servlet.OnlineBmt} mencocokkan API_KEY pengirim.
	 * Lihat pembahasan "Jangkauan endpoint H2H anonim" pada javadoc kelas.</p>
	 *
	 * @return {@code true} bila kanal ini melayani Online BMT
	 */
	@Column(name = "aktfkan_pembayaran_via_online_bmt")
	public Boolean getAktfkanPembayaranViaOnlineBmt() {
		return aktfkanPembayaranViaOnlineBmt == null ? false : aktfkanPembayaranViaOnlineBmt;
	}

	/**
	 * Mengaktifkan/menonaktifkan pembayaran via Online BMT pada kanal ini.
	 *
	 * <p><b>Berdampak keamanan.</b> Sakelar ini adalah salah satu dari dua syarat yang
	 * membuat baris kanal ini ikut dipindai oleh
	 * {@code OnlineBmtUtil#findCredentialCandidates(String)} pada endpoint publik
	 * {@code ais.action.servlet.OnlineBmt} (syarat kedua: {@link #getOnlineBmtApiKey()}
	 * cocok dengan API_KEY yang dikirim pemanggil). Mematikan sakelar ini mengeluarkan baris
	 * dari daftar kandidat autentikasi.</p>
	 *
	 * @param aktfkanPembayaranViaOnlineBmt {@code true} untuk mengaktifkan
	 */
	public void setAktfkanPembayaranViaOnlineBmt(Boolean aktfkanPembayaranViaOnlineBmt) {
		this.aktfkanPembayaranViaOnlineBmt = aktfkanPembayaranViaOnlineBmt;
	}

	/**
	 * Override prefix invoice Online BMT pada kanal ini.
	 *
	 * <p>Nilai kosong berarti mengikuti sekolah pemilik kanal, lalu konfigurasi global.
	 * Getter <b>sengaja tidak</b> memberikan nilai default agar
	 * {@code OnlineBmtUtil#resolveSettings} dapat membedakan "tidak diatur" dari override
	 * yang sengaja diberikan administrator &mdash; berbeda dengan getter kredensial
	 * BNI/BSI/Flip/Finpay di kelas ini yang meratakan {@code null} menjadi {@code ""}.</p>
	 *
	 * <p>Prefix dinormalkan di resolver (hanya huruf/angka, huruf besar, maksimal 8
	 * karakter, jatuh ke <code>"BMT"</code> bila kosong), bukan di setter kelas ini.</p>
	 *
	 * @return prefix invoice, atau {@code null} bila mewarisi Sekolah/global
	 */
	@Column(name = "online_bmt_prefix_invoice", length = 8)
	public String getOnlineBmtPrefixInvoice() {
		return onlineBmtPrefixInvoice;
	}

	/**
	 * Menetapkan override prefix invoice Online BMT untuk kanal ini.
	 *
	 * <p>{@code KanalPembayaranAction#onSave} melewatkan nilai lewat
	 * {@code OnlineBmtUtil.emptyToNull(...)}, sehingga isian kosong tersimpan sebagai
	 * {@code null} dan pewarisan ke Sekolah/global tetap bekerja.</p>
	 *
	 * @param onlineBmtPrefixInvoice prefix invoice; {@code null} berarti mewarisi induk
	 */
	public void setOnlineBmtPrefixInvoice(String onlineBmtPrefixInvoice) {
		this.onlineBmtPrefixInvoice = onlineBmtPrefixInvoice;
	}

	/**
	 * Override biaya administrasi Online BMT pada kanal ini.
	 *
	 * <p>Semantik tiga keadaan yang penting dipertahankan: {@code null} berarti mewarisi
	 * Sekolah lalu global, sedangkan {@code 0} adalah override sah yang berarti "tanpa
	 * biaya administrasi". Karena itu getter ini tidak boleh diubah menjadi mengembalikan
	 * {@code 0.0} untuk {@code null} seperti {@link #getBiayaAdminFlip()}.</p>
	 *
	 * @return nominal biaya administrasi, atau {@code null} bila mewarisi induk
	 */
	@Column(name = "online_bmt_biaya_administrasi")
	public Double getOnlineBmtBiayaAdministrasi() {
		return onlineBmtBiayaAdministrasi;
	}

	/**
	 * Menetapkan override biaya administrasi Online BMT untuk kanal ini.
	 *
	 * @param onlineBmtBiayaAdministrasi nominal biaya; {@code null} berarti mewarisi induk,
	 *        sedangkan {@code 0} adalah override sah "tanpa biaya"
	 */
	public void setOnlineBmtBiayaAdministrasi(Double onlineBmtBiayaAdministrasi) {
		this.onlineBmtBiayaAdministrasi = onlineBmtBiayaAdministrasi;
	}

	/**
	 * Fallback kode mitra BMT (label layar: "Fallback Kode Mitra BMT (utama = Kode
	 * Yayasan)"). Identitas kontraktual, bukan rahasia.
	 *
	 * <p>Dipakai {@code OnlineBmtUtil} hanya bila identitas utama &mdash; kode yayasan
	 * pemilik &mdash; tidak tersedia. Bersama tiga nilai identitas lainnya, nilai ini wajib
	 * lengkap agar inquiry Online BMT dapat dijawab sukses; konfigurasi setengah lengkap
	 * sengaja dilaporkan sebagai "layanan belum siap" (HTTP 503) oleh
	 * {@code OnlineBmt#requireMerchantIdentity}.</p>
	 *
	 * @return kode mitra, atau {@code null} bila mewarisi induk
	 */
	@Column(name = "online_bmt_kode_mitra")
	public String getOnlineBmtKodeMitra() { return onlineBmtKodeMitra; }
	/**
	 * Menetapkan fallback kode mitra BMT.
	 *
	 * @param value kode mitra; {@code null} berarti mewarisi Sekolah/global
	 */
	public void setOnlineBmtKodeMitra(String value) { this.onlineBmtKodeMitra = value; }

	/**
	 * Fallback nama mitra BMT (utama = nama yayasan). Identitas kontraktual, bukan rahasia.
	 *
	 * @return nama mitra, atau {@code null} bila mewarisi induk
	 */
	@Column(name = "online_bmt_nama_mitra")
	public String getOnlineBmtNamaMitra() { return onlineBmtNamaMitra; }
	/**
	 * Menetapkan fallback nama mitra BMT.
	 *
	 * @param value nama mitra; {@code null} berarti mewarisi Sekolah/global
	 */
	public void setOnlineBmtNamaMitra(String value) { this.onlineBmtNamaMitra = value; }

	/**
	 * Fallback kode merchant Online BMT (utama = pemilik transaksi). Identitas kontraktual.
	 *
	 * @return kode merchant, atau {@code null} bila mewarisi induk
	 */
	@Column(name = "online_bmt_kode_merchant")
	public String getOnlineBmtKodeMerchant() { return onlineBmtKodeMerchant; }
	/**
	 * Menetapkan fallback kode merchant Online BMT.
	 *
	 * @param value kode merchant; {@code null} berarti mewarisi Sekolah/global
	 */
	public void setOnlineBmtKodeMerchant(String value) { this.onlineBmtKodeMerchant = value; }

	/**
	 * Fallback nama merchant Online BMT (utama = pemilik transaksi). Identitas kontraktual.
	 *
	 * @return nama merchant, atau {@code null} bila mewarisi induk
	 */
	@Column(name = "online_bmt_nama_merchant")
	public String getOnlineBmtNamaMerchant() { return onlineBmtNamaMerchant; }
	/**
	 * Menetapkan fallback nama merchant Online BMT.
	 *
	 * @param value nama merchant; {@code null} berarti mewarisi Sekolah/global
	 */
	public void setOnlineBmtNamaMerchant(String value) { this.onlineBmtNamaMerchant = value; }

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> API key Online BMT untuk kanal ini &mdash;
	 * <b>faktor autentikasi tunggal</b> bagi endpoint publik
	 * {@code ais.action.servlet.OnlineBmt}.
	 *
	 * <p>Alur yang diverifikasi: servlet membaca {@code API_KEY} dari body permintaan tanpa
	 * sesi login, lalu {@code OnlineBmtUtil#findCredentialCandidates(String)} mencari baris
	 * kanal <b>di seluruh tenant</b> dengan
	 * <code>aktfkanPembayaranViaOnlineBmt = true</code> dan
	 * <code>onlineBmtApiKey = &lt;API_KEY&gt;</code>. Kandidat yang cocok dipakai untuk
	 * mendekripsi amplop {@code DATA}. Penyalahgunaan lintas tenant dicegah di lapis
	 * berikutnya: servlet menuntut {@code candidate.sameSecurity(...)} terhadap konfigurasi
	 * efektif pemilik invoice sebelum memproses, sehingga kunci sekolah A tidak dapat
	 * dipakai atas invoice sekolah B.</p>
	 *
	 * <p><b>Ketiga kredensial Online BMT merupakan satu paket keamanan.</b> Semuanya kosong
	 * berarti mewarisi induk; bila melakukan override, API key, AES key, dan HMAC key wajib
	 * diisi bersama agar request tidak dapat didekripsi dengan profil yang ambigu
	 * ({@code OnlineBmtUtil#securityOverride} menandai override setengah jadi sebagai tidak
	 * sah). Semuanya disimpan <b>plaintext</b> pada kolom bertipe <code>text</code>.</p>
	 *
	 * @return API key, atau {@code null} bila mewarisi Sekolah/global
	 */
	@Column(name = "online_bmt_api_key", columnDefinition = "text")
	public String getOnlineBmtApiKey() { return onlineBmtApiKey; }
	/**
	 * <b>&#9888;</b> Menyimpan API key Online BMT apa adanya (tanpa enkripsi). Nilai kosong
	 * dinormalkan menjadi {@code null} oleh pemanggil ({@code onSave} lewat
	 * {@code OnlineBmtUtil.emptyToNull}) agar pewarisan tetap bekerja.
	 *
	 * @param value API key; {@code null} berarti mewarisi Sekolah/global
	 */
	public void setOnlineBmtApiKey(String value) { this.onlineBmtApiKey = value; }

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> Kunci AES-256-CBC untuk membuka dan menyusun
	 * amplop {@code DATA} Online BMT (label layar: "Encryption Key AES").
	 *
	 * <p>Dipakai {@code OnlineBmt#decrypt(String, String, String)} bersama
	 * {@link #getOnlineBmtHmacKey()}. Disimpan <b>plaintext</b> pada kolom
	 * <code>online_bmt_encryption_key text</code> &mdash; sehingga isi seluruh lalu lintas
	 * Online BMT sekolah ini dapat dibuka oleh siapa pun yang membaca baris ini.</p>
	 *
	 * <p>Kotak isian layar diberi {@code setType("password")}, yang hanya menyamarkan
	 * karakter secara visual; nilai lengkap tetap dikirim ke browser saat form dibuka.</p>
	 *
	 * @return kunci AES, atau {@code null} bila mewarisi Sekolah/global
	 */
	@Column(name = "online_bmt_encryption_key", columnDefinition = "text")
	public String getOnlineBmtEncryptionKey() { return onlineBmtEncryptionKey; }
	/**
	 * <b>&#9888;</b> Menyimpan kunci AES Online BMT apa adanya (tanpa enkripsi).
	 *
	 * @param value kunci AES; {@code null} berarti mewarisi Sekolah/global
	 */
	public void setOnlineBmtEncryptionKey(String value) { this.onlineBmtEncryptionKey = value; }

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> Kunci HMAC-SHA256 untuk memverifikasi keutuhan
	 * amplop Online BMT (label layar: "HMAC Key").
	 *
	 * <p>Dipakai {@code OnlineBmt#hmac(String, String)} baik saat memverifikasi permintaan
	 * masuk maupun saat menandatangani respons. Bocornya kunci ini memungkinkan pihak luar
	 * memalsukan tanda tangan permintaan pembayaran. Disimpan <b>plaintext</b> pada kolom
	 * <code>online_bmt_hmac_key text</code>.</p>
	 *
	 * @return kunci HMAC, atau {@code null} bila mewarisi Sekolah/global
	 */
	@Column(name = "online_bmt_hmac_key", columnDefinition = "text")
	public String getOnlineBmtHmacKey() { return onlineBmtHmacKey; }
	/**
	 * <b>&#9888;</b> Menyimpan kunci HMAC Online BMT apa adanya (tanpa enkripsi).
	 *
	 * @param value kunci HMAC; {@code null} berarti mewarisi Sekolah/global
	 */
	public void setOnlineBmtHmacKey(String value) { this.onlineBmtHmacKey = value; }

	/**
	 * Toleransi kesegaran permintaan Online BMT dalam detik (anti-replay).
	 *
	 * <p>Nilai {@code null} berarti mewarisi Sekolah lalu global. Rentang yang dianggap
	 * valid adalah 30 sampai 3600 detik; nilai di luar rentang itu dinormalkan menjadi 300
	 * detik oleh resolver. Semakin besar toleransi, semakin lama jendela permintaan lama
	 * masih dapat diterima.</p>
	 *
	 * @return toleransi dalam detik, atau {@code null} bila mewarisi induk
	 */
	@Column(name = "online_bmt_request_time_tolerance")
	public Integer getOnlineBmtRequestTimeTolerance() { return onlineBmtRequestTimeTolerance; }
	/**
	 * Menetapkan toleransi kesegaran permintaan Online BMT.
	 *
	 * <p>Nilai divalidasi di layar oleh {@code OnlineBmtUtil.parseOptionalTolerance} dan
	 * {@code validateOverrides}, bukan di entity ini. Di sisi resolver, nilai di luar
	 * rentang 30&ndash;3600 detik dinormalkan menjadi 300 detik.</p>
	 *
	 * @param value toleransi dalam detik; {@code null} berarti mewarisi Sekolah/global
	 */
	public void setOnlineBmtRequestTimeTolerance(Integer value) { this.onlineBmtRequestTimeTolerance = value; }

	/**
	 * Username E-Smartlink (label layar: "Smartlink Username").
	 *
	 * <p>Berbeda dengan getter kredensial lain di kelas ini, getter ini mengembalikan
	 * nilai apa adanya &mdash; termasuk {@code null} dan spasi &mdash; sehingga pemanggil
	 * wajib memeriksa {@code null} sendiri.</p>
	 *
	 * @return username, atau {@code null} bila belum diisi
	 */
	public String getUsernameEsmartlink() {
		return usernameEsmartlink;
	}

	/**
	 * Menetapkan username E-Smartlink.
	 *
	 * @param usernameEsmartlink username E-Smartlink
	 */
	public void setUsernameEsmartlink(String usernameEsmartlink) {
		this.usernameEsmartlink = usernameEsmartlink;
	}

	/**
	 * Biaya administrasi bawaan E-Smartlink (label layar: "Biaya Admin Smartlink Default").
	 *
	 * <p>Dipakai bila kode bank yang dipilih tidak ditemukan pada katalog
	 * {@link #getVariableBiayaAdminEsmartlink()}. Nilai {@code null} dilaporkan sebagai
	 * {@code 0}.</p>
	 *
	 * @return nominal biaya administrasi bawaan
	 */
	public Double getBiayaAdminEsmartlink() {
		return biayaAdminEsmartlink == null ? 0 : biayaAdminEsmartlink;
	}

	/**
	 * Menetapkan biaya administrasi bawaan E-Smartlink.
	 *
	 * @param biayaAdminEsmartlink nominal biaya administrasi
	 */
	public void setBiayaAdminEsmartlink(Double biayaAdminEsmartlink) {
		this.biayaAdminEsmartlink = biayaAdminEsmartlink;
	}

	/**
	 * <b>&#9888; KREDENSIAL RAHASIA.</b> Password E-Smartlink (label layar:
	 * "Smartlink Password").
	 *
	 * <p>Dibaca apa adanya oleh {@code VirtualAccountBankAction} dan keluarga
	 * {@code DownloadTagihan*BankOnline}/{@code DownloadNoUjian*BankOnline}, dengan pola
	 * pewarisan "kanal menang atas sekolah". <b>Plaintext</b>; kotak isian layar bukan
	 * bertipe password. Getter ini <b>tidak</b> memberi nilai default, sehingga dapat
	 * mengembalikan {@code null}.</p>
	 *
	 * @return password, atau {@code null} bila belum diisi
	 */
	public String getPasswordEsmartlink() {
		return passwordEsmartlink;
	}

	/**
	 * <b>&#9888;</b> Menyimpan password E-Smartlink apa adanya (tanpa enkripsi).
	 *
	 * @param passwordEsmartlink password E-Smartlink
	 */
	public void setPasswordEsmartlink(String passwordEsmartlink) {
		this.passwordEsmartlink = passwordEsmartlink;
	}

	/**
	 * Katalog biaya administrasi E-Smartlink per metode/bank, berbentuk CSV
	 * (label layar: "Variable Biaya Admin Smartlink").
	 *
	 * <p>Formatnya rangkaian entri dipisah <code>;</code>, tiap entri terdiri atas tiga
	 * bagian dipisah <code>:</code> &mdash; <code>KODE:NOMINAL:LABEL</code>, contoh
	 * <code>VA_BNI:2500:BNI</code>. Dibaca antara lain oleh
	 * {@code ais.action.servlet.api.KantinMemberApi} dan {@code TopupHelper} untuk menyusun
	 * daftar metode pembayaran beserta biayanya yang ditampilkan ke pengguna.</p>
	 *
	 * <p><b>Non-obvious:</b> bila kolom {@code null}, getter mengembalikan <b>katalog
	 * bawaan yang di-hardcode di kelas ini</b> (BNI, BRI, BCA, BNC, CIMB, Mandiri, Permata,
	 * BSI, Danamon, Alfamart, Indomaret beserta nominalnya) &mdash; jadi biaya administrasi
	 * yang ditagihkan ke pengguna dapat berasal dari konstanta di kode, bukan dari
	 * konfigurasi yang pernah disetujui siapa pun. Mengubah nominal bawaan berarti mengubah
	 * biaya bagi semua kanal yang belum pernah mengisi kolom ini. Bila kolom terisi, nilainya
	 * dipangkas spasinya.</p>
	 *
	 * @return katalog CSV biaya admin; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getVariableBiayaAdminEsmartlink() {
		return variableBiayaAdminEsmartlink == null
				? "VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart"
				: variableBiayaAdminEsmartlink.trim();
	}

	/**
	 * Menetapkan katalog biaya administrasi E-Smartlink.
	 *
	 * <p>Tidak ada validasi bentuk sama sekali &mdash; entri yang salah format akan gagal
	 * diurai di sisi pemanggil dan dapat membuat metode pembayaran hilang dari daftar.
	 * Menyimpan string kosong tidak sama dengan mengosongkan: getter akan tetap
	 * mengembalikan string kosong itu, bukan katalog bawaan.</p>
	 *
	 * @param variableBiayaAdminEsmartlink katalog CSV <code>KODE:NOMINAL:LABEL;...</code>
	 */
	public void setVariableBiayaAdminEsmartlink(String variableBiayaAdminEsmartlink) {
		this.variableBiayaAdminEsmartlink = variableBiayaAdminEsmartlink;
	}
}
