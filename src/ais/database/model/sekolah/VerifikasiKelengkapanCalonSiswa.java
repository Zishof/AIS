package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.zul.Messagebox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyMessageboxConfig;

/**
 * Entity Hibernate yang memetakan tabel {@code public.verifikasi_kelengkapan_calon_siswa} pada
 * modul <b>PSB/PPDB</b> (penerimaan siswa baru) &mdash; <b>katalog MASTER jenis berkas
 * persyaratan pendaftaran</b>.
 *
 * <h2>Apa sebenarnya entity ini (hasil verifikasi dari kode, bukan dari namanya)</h2>
 * <p>Namanya menyesatkan: kata "Verifikasi" membuatnya terbaca seperti <i>catatan hasil
 * verifikasi</i>, padahal satu baris di sini <b>tidak menyimpan status verifikasi siapa pun</b>.
 * Satu baris = <b>satu jenis dokumen persyaratan</b> yang ditetapkan panitia, mis. "Fotocopy
 * Kartu Keluarga yang telah dilegalisir", "Fotocopy raport", "Pas photo warna terbaru". Isinya
 * murni definisi katalog: {@link #getNama() nama dokumen}, {@link #getKeterangan() keterangan},
 * {@link #getAktif() saklar aktif}, empat {@linkplain #getWajibUploadSebelumUjian() bendera
 * kebijakan}, dan sepasang kolom pemilik tenant {@link #getSekolah()}/{@link #getYayasan()}.</p>
 *
 * <p>Jawaban atas dua kemungkinan pembacaan yang sering tertukar:</p>
 * <ul>
 * <li><b>Bukan</b> catatan verifikasi per-berkas per-calon-siswa &mdash; itu
 * {@link CalonSiswaPunyaVerifikasiBerkas}, entity transaksi yang <i>menunjuk</i> ke kelas ini.</li>
 * <li><b>Bukan pula</b> verifikasi "per-parameter" (prestasi/tingkat kejuaraan) &mdash; itu
 * pasangan {@code ParameterVerifikasiCalonSiswa} +
 * {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa} +
 * {@code CalonSiswaPunyaVerifikasiParameter}, dikelola
 * {@code ais.action.master.sekolah.psb.VerifikasiParameterPSBHelper} dan sama sekali tidak
 * bersinggungan dengan tabel ini.</li>
 * </ul>
 *
 * <h2>Perbandingan eksplisit dengan {@link CalonSiswaPunyaVerifikasiBerkas}</h2>
 * <p>Keduanya <b>berpasangan master&ndash;transaksi</b>, bukan duplikasi. Perbedaannya
 * terverifikasi baris-per-baris:</p>
 * <table border="1">
 * <caption>Master vs transaksi pada rantai verifikasi berkas PPDB</caption>
 * <tr><th>&nbsp;</th><th>kelas ini (MASTER)</th>
 *     <th>{@link CalonSiswaPunyaVerifikasiBerkas} (TRANSAKSI)</th></tr>
 * <tr><td>Tabel</td><td>{@code public.verifikasi_kelengkapan_calon_siswa}
 *     &mdash; perhatikan schema <b>{@code public}</b>, berbeda dari kerabatnya</td>
 *     <td>{@code sekolah.calon_siswa_punya_verifikasi_berkas}</td></tr>
 * <tr><td>Arti satu baris</td><td>"dokumen apa saja yang wajib" (definisi)</td>
 *     <td>"dokumen X milik calon siswa Y sudah/belum diverifikasi" (fakta)</td></tr>
 * <tr><td>Kardinalitas</td><td>beberapa puluh baris per sekolah; dibuat manual oleh panitia
 *     (plus tujuh baris bawaan hasil auto-seed, lihat di bawah)</td>
 *     <td>(jumlah calon siswa &times; jumlah dokumen aktif); dibuat otomatis oleh mesin</td></tr>
 * <tr><td>Kolom pembeda</td><td>{@code aktif}, {@code wajibUploadSebelumUjian},
 *     {@code wajibUploadSebelumInterview}, {@code wajibVerifikasiSebelumUjian},
 *     {@code wajibVerifikasiSebelumInterview}</td>
 *     <td>{@code verified}, {@code keterangan} petugas</td></tr>
 * <tr><td>Cakupan tenant</td><td><b>punya sendiri</b> ({@code sekolah_id} + {@code yayasan_id})</td>
 *     <td><b>tidak punya</b>; menumpang pada {@code CalonSiswa}</td></tr>
 * <tr><td>Lampiran ({@code LampiranLain})</td><td>label {@code "Lampiran"} &mdash; <b>formulir/contoh
 *     kosong dari panitia</b> yang boleh diunduh calon siswa</td>
 *     <td>label {@code "Berkas"} &mdash; <b>dokumen asli milik calon siswa</b> (akte, KK, KTP)</td></tr>
 * <tr><td>Layar</td><td>{@code VerifikasiKelengkapanCalonSiswaAction} (grid master, bergerbang hak)</td>
 *     <td>{@code VerifikasiPSBHelper} (panel detail di formulir calon siswa, tanpa gerbang hak)</td></tr>
 * </table>
 * <p>Satu-satunya kemiripan yang sifatnya kebetulan: {@code serialVersionUID} kedua kelas
 * <b>identik</b> ({@code 2463821577548439808L}) &mdash; artefak salin-tempel, bukan penanda klon.</p>
 *
 * <h2>Posisi dalam rantai PSB</h2>
 * <pre>
 * GelombangPendaftaranPsb
 *   &boxur;&boxh; (&#64;ManyToMany) kelas ini                        &larr; MASTER "berkas apa yang wajib"
 *                     &boxur;&boxh; CalonSiswaPunyaVerifikasiBerkas  &larr; TRANSAKSI "sudah diverifikasi belum"
 *                          &boxur;&boxh; LampiranLain / FileFotoLain &larr; BERKAS FISIK hasil unggahan
 * </pre>
 * <p><b>Relasi ke {@link GelombangPendaftaranPsb} (terverifikasi):</b> many-to-many lewat tabel
 * gabung {@code sekolah.gelombang_punya_verifikasi_siswa} (kolom {@code gelombang} &harr;
 * {@code verifikasi}), dideklarasikan <b>hanya di sisi gelombang</b>
 * ({@code GelombangPendaftaranPsb.getVerifikasiKelengkapanCalonSiswas()}, {@code CascadeType.MERGE}
 * saja). Kelas ini <b>tidak memiliki FK maupun koleksi balik</b> &mdash; satu baris master tidak
 * tahu gelombang mana saja yang memakainya. Konsekuensi praktis:</p>
 * <ul>
 * <li>Menyimpan gelombang hanya <i>mengaitkan</i> baris master yang sudah ada, tidak pernah
 * membuatnya.</li>
 * <li><b>Menghapus baris master dari layar ini tidak membersihkan tabel gabung</b> &mdash; tidak ada
 * cascade {@code REMOVE} dan tidak ada kode yang menyapu {@code gelombang_punya_verifikasi_siswa}.
 * Penghapusan akan ditolak constraint FK, atau (bila constraint tidak terpasang di instalasi
 * tersebut) meninggalkan baris gabung yatim.</li>
 * <li>Satu baris master dapat dipakai banyak gelombang sekaligus; menonaktifkan
 * {@link #getAktif()} mematikannya untuk <b>semua</b> gelombang serentak.</li>
 * </ul>
 *
 * <h2>Lampiran: nama kelas ini adalah NILAI DATA di basis data</h2>
 * <p>Formulir/contoh berkas yang disediakan panitia disimpan di
 * {@link ais.database.model.file.LampiranLain} (basis data STREAMING terpisah) dengan pasangan
 * kunci {@code lampiran_lain.ref} = {@link #getId()} dan {@code lampiran_lain.jenis} =
 * <b>nama kelas berkualifikasi penuh</b> ({@code VerifikasiKelengkapanCalonSiswa.class.getName()}),
 * label {@code "Lampiran"}. Sama seperti pada {@link CalonSiswaPunyaVerifikasiBerkas},
 * <b>mengganti nama kelas atau memindahkan paketnya akan memutus tautan seluruh lampiran yang sudah
 * terunggah</b> &mdash; ini bukan refactor kompilasi belaka. Titik yang mengeja string itu:
 * {@code VerifikasiKelengkapanCalonSiswaAction} (dua kali) dan
 * {@code ais.action.master.sekolah.psb.VerifikasiPSBHelper} (empat kali).</p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ol>
 * <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()} beserta setternya dan hook {@link #onUpdate()}. Field-field ini
 * <b>dideklarasikan ulang</b> dari {@link GeneralValueObject}; lihat catatan di
 * {@link #getOlehId()} &mdash; itu keharusan teknis, bukan duplikasi keliru.</li>
 * <li><b>Identitas &amp; deskripsi</b> &mdash; {@link #getId()}, {@link #getNama()},
 * {@link #getKeterangan()}, {@link #toString()}.</li>
 * <li><b>Bendera kebijakan</b> &mdash; {@link #getAktif()} plus empat bendera
 * {@code wajib*Sebelum*} yang menentukan tahap mana yang diblokir bila berkas belum ada/belum
 * diverifikasi.</li>
 * <li><b>Cakupan tenant</b> &mdash; {@link #getSekolah()}, {@link #getYayasan()}.
 * <b>Perhatikan {@link #getYayasan()}: getter destruktif</b>, lihat di bawah.</li>
 * <li><b>Gerbang bisnis statis</b> &mdash; enam method {@code static} yang membaca katalog ini,
 * mencocokkannya dengan {@link CalonSiswaPunyaVerifikasiBerkas} milik seorang calon siswa, lalu
 * memutuskan boleh/tidaknya calon itu melanjutkan: {@link #checkBerkas(CalonSiswa)},
 * {@link #checkBerkasSebelumUjian(CalonSiswa)},
 * {@link #ambilPesanGagalSebelumUjian(CalonSiswa)},
 * {@link #ambilPesanGagalSebelumInterview(CalonSiswa, Session)},
 * {@link #ambilPesanGagalVerifikasiSebelumUjian(CalonSiswa, Session)},
 * {@link #ambilPesanGagalVerifikasiSebelumInterview(CalonSiswa, Session)}.</li>
 * </ol>
 * <p>Enam gerbang itu <b>tidak sesuai tempatnya</b> secara arsitektur: entity model memuat query
 * Hibernate, penulisan basis data, dan bahkan pemanggilan {@code Messagebox} ZK. Pemanggil baru
 * sebaiknya memakai varian {@code ambilPesanGagal*} (mengembalikan {@code String}) dan menampilkan
 * pesannya sendiri, bukan varian {@code check*} yang menyeret UI ke lapisan model.</p>
 *
 * <h2>TEMUAN 1 (TERBERAT) &mdash; dua bendera kebijakan yang TIDAK PERNAH DIPERIKSA SIAPA PUN</h2>
 * <p>Layar master menawarkan empat checkbox kebijakan. Dua di antaranya
 * &mdash; <b>"Wajib Verifikasi sebelum Ujian"</b> ({@link #getWajibVerifikasiSebelumUjian()}) dan
 * <b>"Wajib Verifikasi sebelum Interview"</b> ({@link #getWajibVerifikasiSebelumInterview()}) &mdash;
 * <b>tidak menghasilkan efek apa pun</b>. Satu-satunya kode yang membacanya adalah
 * {@link #ambilPesanGagalVerifikasiSebelumUjian(CalonSiswa, Session)} dan
 * {@link #ambilPesanGagalVerifikasiSebelumInterview(CalonSiswa, Session)}, dan <b>kedua method itu
 * nol pemanggil</b> di seluruh pohon sumber ({@code src/**} maupun {@code webapp/**}) &mdash;
 * terverifikasi dengan pencarian menyeluruh, bukan asumsi.</p>
 * <p>Akibatnya panitia yang mencentang kedua kotak itu <b>meyakini</b> bahwa seorang calon siswa
 * baru boleh ikut ujian/wawancara setelah berkasnya diperiksa manusia, padahal sistem
 * <b>mempersilakan siapa saja lewat</b>. Kontrol keamanan yang tampil di layar tetapi tidak pernah
 * menyala lebih berbahaya daripada kontrol yang jelas-jelas tidak ada, karena menghilangkan
 * kewaspadaan operator. (Catatan koreksi: Javadoc {@link CalonSiswaPunyaVerifikasiBerkas} menyebut
 * kedua method ini sebagai "gerbang bisnis" &mdash; benar secara <i>niat</i>, tetapi tidak
 * tersambung ke jalur eksekusi mana pun.)</p>
 * <p>Yang benar-benar ditegakkan hanyalah pasangan <b>upload</b>-nya
 * ({@code wajibUploadSebelumUjian}/{@code wajibUploadSebelumInterview}, dicek oleh
 * {@link #ambilPesanGagalSebelumUjian(CalonSiswa)} dan
 * {@link #ambilPesanGagalSebelumInterview(CalonSiswa, Session)}) &mdash; yaitu "ada berkasnya",
 * bukan "berkasnya benar". Berkas kosong/asal-asalan tetap lolos.</p>
 *
 * <h2>TEMUAN 2 &mdash; jalur "Ikut Ujian Online" versi JSP publik melewati SELURUH gerbang</h2>
 * <p>Penegakan {@code wajibUploadSebelumUjian} hanya terjadi pada jalur ZK
 * ({@code TampilanPengumumanAkademisAction}, tombol "Ikut Ujian Sekarang", yang memanggil
 * {@link #checkBerkasSebelumUjian(CalonSiswa)} lalu {@link #checkBerkas(CalonSiswa)}). Berkas
 * layanan portal PPDB {@code /WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp}
 * <b>tidak menyebut kata "verifikasi" maupun "berkas" sama sekali</b> &mdash; ia tidak memanggil
 * satu pun gerbang di kelas ini. Bandingkan dengan saudaranya {@code _wawancara_service.jsp}, yang
 * <i>memang</i> memanggil {@link #ambilPesanGagalSebelumInterview(CalonSiswa, Session)}. Jadi
 * kewajiban unggah berkas ditegakkan untuk <b>wawancara</b> tetapi <b>tidak untuk ujian</b> pada
 * portal publik yang sama. Padanan modul PMB justru lengkap
 * ({@code pmb/_ikut_ujian_online_service.jsp} memanggil
 * {@code VerifikasiPMBHelper.ambilPesanGagalSebelumUjian}), sehingga ini kelalaian khas jalur PSB.</p>
 *
 * <h2>TEMUAN 3 &mdash; SELURUH gerbang di kelas ini FAIL-OPEN</h2>
 * <p>Keenam method statis membungkus logikanya dalam {@code try}/{@code catch (Exception)} yang
 * <b>menelan galat lalu mengembalikan nilai "aman"</b> ({@code true} untuk varian {@code check*},
 * {@code null} untuk varian {@code ambilPesanGagal*} &mdash; keduanya berarti "silakan lanjut").
 * Kegagalan sesi Hibernate, {@code LazyInitializationException}, atau gangguan basis data sesaat
 * karena itu <b>membuka</b> gerbang, bukan menutupnya. Ini pola yang sama dengan
 * {@code GelombangPendaftaranPsb.chekUmur()} (batch 71) dan merupakan arah kegagalan yang salah
 * untuk kontrol kelayakan.</p>
 * <p>Ditambah lagi, tiga dari empat method yang memeriksa gelombang memakai penjaga
 * {@code gelombang == null &rarr; return null} &mdash; artinya calon siswa yang <b>tidak</b>
 * terhubung ke gelombang mana pun otomatis lolos semua persyaratan berkas.</p>
 *
 * <h2>TEMUAN 4 &mdash; {@link #getYayasan()} adalah getter DESTRUKTIF (menulis balik)</h2>
 * <p>{@link #getYayasan()} tidak sekadar membaca: ia <b>menimpa field {@code yayasan} dengan
 * {@code getSekolah().getYayasan()} pada setiap pemanggilan</b>. Karena entity ini memakai
 * <i>property access</i> (anotasi {@code @Id} berada pada getter), nilai hasil getter itulah yang
 * dibaca Hibernate saat dirty-check &mdash; sehingga <b>sekadar merender grid dapat menerbitkan
 * {@code UPDATE}</b> plus revisi Envers palsu yang tidak berasal dari tindakan pengguna. Dua
 * skenario merusak data:</p>
 * <ul>
 * <li>Baris yang sengaja dimiliki yayasan berbeda dari yayasan sekolahnya akan
 * <b>dipindahkan diam-diam</b> ke yayasan sekolah.</li>
 * <li>Bila {@code sekolah.getYayasan()} bernilai {@code null} (sekolah tanpa yayasan &mdash; sah
 * menurut {@code Sekolah.setYayasan()}, lihat batch 71), maka <b>{@code yayasan} baris ini
 * dikosongkan</b>. Baris itu lalu hilang dari setiap pencarian yang memfilter yayasan, termasuk
 * {@code initCriteria()} layar masternya sendiri &mdash; dokumen persyaratan "menguap" dari layar
 * meski masih ditagih {@link #checkBerkas(CalonSiswa)}.</li>
 * </ul>
 * <p>Bandingkan dengan {@link #getSekolah()} yang hanya memanggil
 * {@link GeneralValueObject#check(Object)} &mdash; itu <b>memoisasi resolusi proxy lazy</b> dan
 * bersifat aman (mengembalikan argumen apa adanya bila resolusi gagal), bukan destruktif.</p>
 *
 * <h2>TEMUAN 5 &mdash; penciutan {@code TreeSet} menyembunyikan dokumen persyaratan</h2>
 * <p>Kelas ini <b>tidak</b> meng-override {@code getNomorUrut()} maupun {@code getNim()}, sehingga
 * {@link GeneralValueObject#compareTo(GeneralValueObject)} selalu jatuh ke cabang ketiga:
 * perbandingan {@link #getNama()}. Kolom {@code nama} bersifat {@code nullable = false} tetapi
 * <b>tanpa {@code unique}</b>, jadi dua baris master boleh bernama persis sama.</p>
 * <p>Koleksi terpetakan di {@code GelombangPendaftaranPsb} tidak memakai {@code @Sort}, sehingga
 * Hibernate memuatnya sebagai {@code PersistentSet} (semantik {@code HashSet}) &mdash; aman. Namun
 * tiga pemanggil <b>menyalinnya ulang ke {@code TreeSet}</b>:
 * {@code VerifikasiPSBHelper} (dua tempat) dan {@code CalonSiswaAction}. Di sanalah dua baris
 * bernama sama <b>menciut menjadi satu</b>, sehingga satu dokumen persyaratan
 * <b>tidak pernah muncul di panel unggah/verifikasi</b>.</p>
 * <p>Yang membuatnya berbahaya: {@link #checkBerkas(CalonSiswa)} dan
 * {@link #ambilPesanGagalSebelumUjian(CalonSiswa)} memakai {@code ArrayList} +
 * {@code Collections.sort} (<b>tanpa</b> penciutan) sehingga tetap menagih dokumen yang menghilang
 * itu. Hasil akhirnya: calon siswa <b>diblokir permanen</b> dari cetak kartu ujian atas dokumen
 * yang secara harfiah tidak bisa ia unggah karena barisnya tidak dirender.</p>
 *
 * <h2>Broken access control &mdash; HASIL VERIFIKASI</h2>
 *
 * <h3>a. Layar master: BERGERBANG BENAR (verifikasi NEGATIF)</h3>
 * <p>{@code VerifikasiKelengkapanCalonSiswaAction} memanggil {@code Common.doCheckSecurity()} di
 * {@code doBeforeCompose()} (otentikasi) dan menghormati ketiga hak CRUD:
 * {@code add.setVisible(checkPrevilages(CREATE))}, {@code edit = checkPrevilages(UPDATE)} yang
 * dipakai untuk {@code setDisabled(!edit)} pada kelima checkbox baris,
 * {@code delete = checkPrevilages(DELETE)}, dan tombol unggah massal yang menuntut ketiganya
 * sekaligus. Ini <b>mengulang pola keluarga PSB</b>: layar <b>master</b> benar-benar bergerbang,
 * sedangkan panel <b>detail</b> ({@code VerifikasiPSBHelper}, lihat
 * {@link CalonSiswaPunyaVerifikasiBerkas}) tidak. Dicatat sebagai CONFIRMED-NEGATIVE.</p>
 *
 * <h3>b. Pewarisan hak lewat menu induk: POSITIF</h3>
 * <p>Berkas layar {@code /pages/master/sekolah/verifikasi_kelengkapan_calon_siswa.zul}
 * <b>tidak terdaftar sebagai menu di mana pun</b>. Satu-satunya jalan masuk adalah
 * {@code new MyInclude("/pages/master/sekolah/verifikasi_kelengkapan_calon_siswa.zul")} di
 * {@code GelombangPendaftaranPsbAction} &mdash; salah satu dari sembilan tab CRUD yang menumpang
 * menu "Gelombang Pendaftaran PSB" (lihat batch 71). Karena
 * {@code CommonPrivilages.checkPrevilages(...)} mengevaluasi hak terhadap menu yang <i>sedang
 * aktif</i>, siapa pun yang berhak {@code CREATE}/{@code UPDATE}/{@code DELETE} atas menu gelombang
 * <b>otomatis</b> berhak penuh atas katalog persyaratan ini &mdash; termasuk mengubah dokumen apa
 * saja yang wajib diunggah seluruh calon siswa. Tidak ada hak terpisah yang bisa diberikan atau
 * dicabut untuk katalog ini.</p>
 *
 * <h3>c. Primitif TULIS pra-otentikasi lewat gerbang kelas ini</h3>
 * <p>{@link #ambilPesanGagalSebelumInterview(CalonSiswa, Session)} dipanggil dari
 * {@code /WEB-INF/baru/modul/ppdb/_wawancara_service.jsp}, yang memilih calon siswa <b>semata-mata
 * dari {@code request.getParameter("id")}</b> &mdash; tanpa sesi, tanpa pemeriksaan kepemilikan,
 * tanpa penyaring sekolah/yayasan (terverifikasi pada berkas JSP tersebut). Karena method ini
 * memakai pola "buat bila belum ada" dan meng-{@code INSERT} baris
 * {@link CalonSiswaPunyaVerifikasiBerkas} lewat {@code Common.refreshSaveOrUpdate(...)}, permintaan
 * HTTP anonim <b>menulis ke basis data</b>. Selain itu pesan galat yang dikembalikan
 * <b>membocorkan nama dokumen persyaratan</b> untuk id calon siswa mana pun. Temuan ini
 * <b>memperkuat</b> {@code task_1f9c66d3} (dispatcher {@code /ppdb} anonim) dan
 * {@code task_4ca32776} (kebocoran PII PSB); yang baru di sini adalah bahwa <b>method statis milik
 * entity model inilah</b> yang menjadi primitif tulisnya.</p>
 *
 * <h3>d. Fail-open cakupan tenant: POSITIF, pada layar masternya</h3>
 * <p>{@code VerifikasiKelengkapanCalonSiswaAction.doAfterCompose()} menghitung jumlah baris dengan
 * {@code sekolah != null &amp;&amp; sekolah.getId() != null ? Restrictions.eq(...)
 * : Restrictions.sqlRestriction("true")}. Karena {@code SekolahUtil.getSekolah()} <b>tidak pernah
 * mengembalikan {@code null}</b> melainkan {@code new Sekolah()} berid-{@code null} (cacat yang
 * dikonfirmasi batch 67/71), cabang yang benar-benar terpakai pada sesi tanpa konteks sekolah
 * adalah {@code sqlRestriction("true")} &mdash; <b>pencacahan lintas seluruh instalasi</b>.
 * {@code initCriteria()} punya varian yang sama ({@code sqlRestriction("1=1")} saat combo "Semua"
 * dipilih).</p>
 *
 * <h3>e. Auto-seed: tujuh baris DITULIS saat layar sekadar DIRENDER</h3>
 * <p>Bila pencacahan di atas menghasilkan nol, {@code doAfterCompose()} langsung
 * {@code session.save(...)} + {@code flush()} untuk <b>tujuh dokumen persyaratan bawaan</b>
 * (ijazah/SKHU, raport, sertifikat prestasi, identitas diri, kartu keluarga, SKTM, pas foto).
 * Tiga hal yang perlu disadari:</p>
 * <ol>
 * <li>Penulisan terjadi <b>sebelum</b> {@code checkPrevilages(CREATE)} dievaluasi &mdash; hak
 * {@code CREATE} hanya mengatur <i>kelihatan atau tidaknya tombol</i>, bukan seeding. Pengguna
 * ber-hak baca saja tetap menerbitkan tujuh baris beserta revisi Envers-nya.</li>
 * <li>{@code setSekolah(...)} menormalkan object berid-{@code null} menjadi {@code null} asli,
 * sehingga pada sesi tanpa konteks sekolah ketujuh baris lahir <b>tanpa pemilik tenant</b>
 * &mdash; baris global yatim.</li>
 * <li>Karena pencacahannya global (poin d), kemunculan baris tanpa tenant itu <b>mematikan
 * seeding untuk seluruh sekolah lain selamanya</b>: hitungan tidak akan pernah nol lagi.</li>
 * </ol>
 *
 * <h3>f. New UI / Generic CRUD v2 ({@code task_7b6038ac}): NEGATIF</h3>
 * <p>{@code webapp/WEB-INF/new/sekolah/services/verifikasi_kelengkapan_calon_siswa_service.jsp}
 * mendeklarasikan {@code nuiServiceEntities = {"VerifikasiKelengkapanCalonSiswa"}} dan menyertakan
 * {@code _shared/services/dispatcher.jsp}, jadi entity ini <b>memang</b> terjangkau lewat
 * auto-registrasi Generic CRUD v2. Namun properti tenantnya bernama persis {@code sekolah} dan
 * {@code yayasan} &mdash; keduanya ada di dalam daftar putih 12 nama
 * {@code GenericCrudAutoEntityAdapter.scopeBindings()}, sehingga penyaringan tenant tetap
 * terpasang. <b>Tidak</b> termasuk kelompok rentan {@code task_7b6038ac}.</p>
 *
 * <h3>g. Tabrakan namespace kunci lampiran ({@code task_3c8413c2}): NEGATIF</h3>
 * <p>Lampiran di sini memakai kunci {@code (ref = id, jenis = nama kelas berkualifikasi penuh)},
 * bukan kunci komposit berformat string seperti {@code "{idKelompok}->{idParameter}"} yang menjadi
 * pokok {@code task_3c8413c2}. Diskriminator nama kelas membuat ruang kunci antar-entity tidak
 * mungkin berimpit.</p>
 *
 * <h2>Kuirk dan catatan lain</h2>
 * <ul>
 * <li><b>Schema {@code public}, bukan {@code sekolah}.</b> Ini satu-satunya anggota rantai
 * verifikasi berkas PSB yang berada di schema {@code public}; pasangan transaksinya dan tabel
 * gabungnya ada di schema {@code sekolah}. Skrip migrasi/backup yang menyalin per-schema akan
 * memisahkan master dari transaksinya.</li>
 * <li><b>Tidak ada {@code @PrePersist}.</b> {@code oleh}/{@code olehId} baru terisi pada
 * {@code UPDATE} pertama; ketujuh baris hasil auto-seed karena itu <b>tanpa atribusi sama
 * sekali</b>.</li>
 * <li><b>{@link #getNama()} mem-{@code trim} saat membaca</b> tetapi
 * {@link #getKeterangan()} tidak &mdash; kebalikan dari
 * {@link CalonSiswaPunyaVerifikasiBerkas#getKeterangan()} yang mem-{@code trim} sekaligus
 * menormalkan {@code null} menjadi {@code ""}.</li>
 * <li><b>{@link #getAktif()} berbawaan {@code true}</b> (baris {@code NULL} warisan dianggap aktif,
 * jadi tetap ditagih), sedangkan keempat bendera {@code wajib*} berbawaan {@code false}. Arah
 * bawaan itu benar untuk sebuah katalog persyaratan.</li>
 * <li><b>Impor {@code org.zkoss.zul.Messagebox} tidak terpakai</b> &mdash; yang dipakai adalah
 * {@code ais.ui.util.MyMessageboxConfig}. Sisa refactor lama.</li>
 * <li><b>Salah label pada scaffold New UI.</b> Berkas {@code uiux} menandai halaman ini
 * {@code nuiPageType = "finance"} dengan deskripsi "Keuangan untuk eSchool" &mdash; keliru
 * (ini master persyaratan berkas, bukan modul keuangan); murni heuristik generator, tanpa dampak
 * fungsional.</li>
 * <li><b>Kembaran modul PMB.</b> {@code ais.database.model.VerifikasiKelengkapanCalonMahasiswa}
 * adalah padanan struktural untuk jalur perguruan tinggi, dengan empat bendera yang sama. Di sana
 * gerbangnya <b>benar-benar terpakai</b> lewat {@code VerifikasiPMBHelper} &mdash; perbedaan yang
 * mempertegas TEMUAN 1 sebagai kelalaian jalur PSB, bukan desain yang disengaja.</li>
 * </ul>
 *
 * @see CalonSiswaPunyaVerifikasiBerkas pasangan transaksi (status verifikasi per calon siswa)
 * @see GelombangPendaftaranPsb pemilik relasi many-to-many yang memilih dokumen wajib per gelombang
 * @see CalonSiswa subjek yang dinilai kelayakannya oleh gerbang-gerbang di kelas ini
 * @see ais.database.model.VerifikasiKelengkapanCalonMahasiswa padanan modul PMB
 * @see ais.database.model.GeneralValueObject induk POJO (bukan {@code @Entity}/{@code @MappedSuperclass})
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "verifikasi_kelengkapan_calon_siswa")
public class VerifikasiKelengkapanCalonSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Nilainya <b>identik</b> dengan {@link CalonSiswaPunyaVerifikasiBerkas} &mdash; artefak
	 * salin-tempel, bukan penanda entity klon (kedua kelas memetakan tabel berbeda dan memang
	 * berpasangan master&ndash;transaksi). Tidak dipakai untuk apa pun selain kontrak
	 * {@link java.io.Serializable}.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama; dibangkitkan basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris katalog ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()} lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, jadi tidak perlu (dan tidak boleh)
	 * disetel dari kode layar.</p>
	 *
	 * <p><b>Mengapa field ini dideklarasikan ulang di sini.</b> {@link GeneralValueObject} adalah
	 * POJO abstrak biasa &mdash; <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
	 * &mdash; sehingga Hibernate tidak memetakan properti induknya. Pengulangan deklarasi
	 * {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} pada kelas ini adalah
	 * <b>keharusan teknis</b>, bukan duplikasi yang keliru.</p>
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila baris belum pernah di-{@code UPDATE}
	 *         (termasuk seluruh baris hasil auto-seed layar master)
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Setter penjaga:</b> argumen {@code null}, kosong, atau berisi spasi saja
	 * <b>diabaikan diam-diam</b> dan nilai lama dipertahankan. Pola ini dipakai konsisten di
	 * seluruh entity repo agar atribusi audit tidak pernah terhapus oleh binding formulir yang
	 * mengirim string kosong.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Setter penjaga</b> dengan semantik yang sama seperti {@link #setOlehId(String)}:
	 * {@code null}/kosong/hanya spasi diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris katalog ini.
	 *
	 * <p>Maknanya penting secara akuntabilitas: baris ini menentukan <b>dokumen apa saja yang wajib
	 * dipenuhi seluruh calon siswa</b> pada gelombang yang memakainya. Menambah atau menonaktifkan
	 * satu baris berdampak massal, sehingga jejak "siapa yang terakhir mengubah" adalah satu-satunya
	 * petunjuk saat persyaratan berubah tanpa pengumuman.</p>
	 *
	 * <p><b>Perhatikan keterbatasannya:</b> karena tidak ada {@code @PrePersist}, baris yang
	 * <i>dibuat</i> (termasuk ketujuh baris auto-seed) tidak beratribusi siapa pun sampai ada yang
	 * menyuntingnya. Selain itu penulisan-balik pada {@link #getYayasan()} dapat menerbitkan
	 * {@code UPDATE} tanpa tindakan pengguna, sehingga nilai di sini bisa berubah karena sekadar
	 * ada yang membuka layar.</p>
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila baris belum pernah di-{@code UPDATE}
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} beserta deklarasi field {@code tanggal_dirubah} (keduanya berbagi
	 * satu baris sumber; gaya asli berkas dipertahankan apa adanya).
	 *
	 * <p><b>{@code onUpdate()}</b> dipanggil Hibernate tepat sebelum pernyataan {@code UPDATE}
	 * dieksekusi dan mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)} dan {@link #setTanggal_dirubah(Date)}
	 * dari pengguna yang sedang login.</p>
	 *
	 * <p><b>Hanya berlaku untuk UPDATE.</b> Tidak ada pasangan {@code @PrePersist}: pada
	 * {@code INSERT} pertama {@code oleh}/{@code olehId} tetap {@code null} dan
	 * {@code tanggal_dirubah} mengandalkan nilai awal field
	 * ({@code ais.ui.util.WaktuUtil.getDate()}, dievaluasi saat object dikonstruksi &mdash; waktu
	 * konstruksi, bukan waktu INSERT).</p>
	 *
	 * <p><b>Dapat menyala tanpa tindakan pengguna.</b> {@link #getYayasan()} menulis balik ke
	 * fieldnya dan {@link #getNama()} mem-{@code trim} saat membaca; karena entity ini memakai
	 * <i>property access</i>, dirty-check Hibernate melihat perubahan tersebut dan menerbitkan
	 * {@code UPDATE} &mdash; sehingga hook ini, dan revisi Envers yang menyertainya, dapat muncul
	 * dari operasi yang secara semantik hanya membaca (mis. merender grid master).</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi maupun penjaga {@code null}.
	 *
	 * <p>Normalnya dipanggil {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan dari
	 * kode layar.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Nilai awalnya adalah <b>waktu konstruksi object</b>,
	 * bukan waktu {@code INSERT} &mdash; perbedaan yang biasanya tidak terasa karena baris disimpan
	 * segera setelah dibuat.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object hasil konstruksi normal
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris katalog: {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai ZK sebagai label bawaan bila entity dimasukkan ke komponen daftar tanpa renderer
	 * khusus, serta pada keluaran log/debug. <b>Bukan</b> label yang tampil di grid master maupun di
	 * panel verifikasi &mdash; keduanya memanggil {@link #getNama()} secara eksplisit.</p>
	 *
	 * <p>Memakai field {@code nama} secara langsung (bukan {@link #getNama()}), sehingga spasi tepi
	 * yang tersimpan di basis data ikut tampil apa adanya. Aman terhadap {@code null}: menghasilkan
	 * mis. {@code "null-null"} alih-alih melempar.</p>
	 *
	 * @return gabungan id dan nama dokumen persyaratan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama dokumen persyaratan (label yang dilihat calon siswa). Lihat {@link #getNama()}. */
	private String nama;
	/** Saklar aktif katalog; bawaan dianggap {@code true}. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Penjelasan tambahan untuk panitia/calon siswa. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Blokir ikut ujian bila berkas belum DIUNGGAH. Lihat {@link #getWajibUploadSebelumUjian()}. */
	private Boolean wajibUploadSebelumUjian;
	/** Blokir wawancara bila berkas belum DIUNGGAH. Lihat {@link #getWajibUploadSebelumInterview()}. */
	private Boolean wajibUploadSebelumInterview;
	/**
	 * Blokir ikut ujian bila berkas belum DIVERIFIKASI petugas.
	 * <b>Tidak pernah ditegakkan</b> &mdash; lihat {@link #getWajibVerifikasiSebelumUjian()}.
	 */
	private Boolean wajibVerifikasiSebelumUjian;
	/**
	 * Blokir wawancara bila berkas belum DIVERIFIKASI petugas.
	 * <b>Tidak pernah ditegakkan</b> &mdash; lihat {@link #getWajibVerifikasiSebelumInterview()}.
	 */
	private Boolean wajibVerifikasiSebelumInterview;
	/** Sekolah pemilik baris katalog. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik baris katalog. <b>Ditimpa saat dibaca</b> &mdash; lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Object hasil konstruksi belum lengkap: pemanggil <b>wajib</b> mengisi
	 * {@link #setNama(String)} (kolomnya {@code nullable = false}) sebelum menyimpan, dan
	 * seharusnya mengisi {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)} agar baris tidak
	 * menjadi katalog global yatim. Bendera {@code wajib*} dibiarkan {@code null} dan dibaca sebagai
	 * {@code false}; {@link #getAktif()} dibaca sebagai {@code true}.</p>
	 */
	public VerifikasiKelengkapanCalonSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris katalog.
	 *
	 * <p><b>Perannya melampaui sekadar kunci basis data:</b> nilai ini menjadi
	 * {@code lampiran_lain.ref} untuk menemukan <b>formulir/contoh berkas</b> yang diunggah panitia
	 * (berpasangan dengan {@code jenis} = nama kelas berkualifikasi penuh, label {@code "Lampiran"}).
	 * Ia juga menjadi kolom {@code verifikasi} pada tabel gabung
	 * {@code sekolah.gelombang_punya_verifikasi_siswa} dan kolom
	 * {@code verifikasi_kelengkapan_calon_siswa} pada {@link CalonSiswaPunyaVerifikasiBerkas}.</p>
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}); kolomnya {@code insertable = false} sehingga
	 * nilai yang disetel manual lewat {@link #setId(Long)} tidak ikut dalam pernyataan
	 * {@code INSERT}.</p>
	 *
	 * @return id baris; {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Tanpa validasi.
	 *
	 * <p>Praktis hanya dipakai Hibernate setelah {@code INSERT}; kode aplikasi tidak perlu
	 * memanggilnya.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama dokumen persyaratan &mdash; teks yang dilihat calon siswa pada panel
	 * "Kelengkapan Berkas" dan yang disisipkan ke seluruh pesan kendala yang dibangun kelas ini.
	 *
	 * <p><b>Getter penormal:</b> hasilnya di-{@code trim}. Karena entity ini memakai
	 * <i>property access</i>, nilai <b>hasil getter</b> inilah yang dibaca Hibernate saat
	 * {@code INSERT} dan dirty-check &mdash; jadi baris warisan yang menyimpan spasi tepi akan
	 * "dirapikan sendiri" pada flush berikutnya, disertai satu revisi Envers yang tidak berasal dari
	 * tindakan pengguna.</p>
	 *
	 * <p><b>Sekaligus kunci urut satu-satunya.</b> Kelas ini tidak meng-override
	 * {@code getNomorUrut()} maupun {@code getNim()}, sehingga
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} selalu jatuh ke perbandingan nilai
	 * ini. Kolom {@code nama} {@code nullable = false} tetapi <b>tanpa {@code unique}</b>, sehingga
	 * dua baris bernama sama akan <b>menciut menjadi satu</b> pada tiga tempat yang menyalin koleksi
	 * gelombang ke {@code TreeSet} ({@code VerifikasiPSBHelper} dua kali,
	 * {@code CalonSiswaAction}) &mdash; dokumen yang hilang itu tetap ditagih
	 * {@link #checkBerkas(CalonSiswa)}. Lihat "TEMUAN 5" pada Javadoc kelas.</p>
	 *
	 * @return nama dokumen yang sudah di-{@code trim}; {@code null} hanya pada object yang belum
	 *         diisi (baris tersimpan selalu punya nilai karena kolomnya {@code nullable = false})
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama dokumen persyaratan. Tanpa validasi, tanpa {@code trim} (pemangkasan dilakukan
	 * {@link #getNama()} saat pembacaan), dan <b>tanpa pemeriksaan keunikan</b>.
	 *
	 * <p>Dipanggil dari {@code VerifikasiKelengkapanCalonSiswaAction.onSave()} (yang hanya
	 * memvalidasi bahwa teksnya tidak kosong) dan dari rutin auto-seed di
	 * {@code doAfterCompose()}.</p>
	 *
	 * <p><b>Peringatan:</b> memberi dua baris nama yang persis sama memicu penciutan
	 * {@code TreeSet} yang dijelaskan pada {@link #getNama()} &mdash; salah satu dokumen menghilang
	 * dari panel unggah namun tetap menjadi syarat cetak kartu ujian, sehingga calon siswa
	 * terblokir tanpa jalan keluar dari UI.</p>
	 *
	 * @param nama nama dokumen persyaratan; kolomnya {@code nullable = false} sehingga {@code null}
	 *             akan ditolak basis data saat flush
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan penjelasan tambahan atas dokumen persyaratan ini (mis. format yang diterima,
	 * pejabat yang harus melegalisir).
	 *
	 * <p>Ditampilkan pada formulir sunting layar master. Berbeda dari
	 * {@link CalonSiswaPunyaVerifikasiBerkas#getKeterangan()}, getter ini <b>tidak</b> menormalkan
	 * {@code null} menjadi {@code ""} dan <b>tidak</b> mem-{@code trim} &mdash; nilainya
	 * dikembalikan apa adanya. Pemanggil harus siap menerima {@code null}.</p>
	 *
	 * <p>Rutin auto-seed layar master mengisi kolom ini dengan salinan {@code nama}, sehingga pada
	 * instalasi baru keterangan dan nama identik sampai panitia menyuntingnya.</p>
	 *
	 * @return keterangan dokumen; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel penjelasan tambahan atas dokumen persyaratan. Tanpa validasi maupun {@code trim}.
	 *
	 * @param keterangan keterangan baru; {@code null} diperbolehkan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris katalog &mdash; <b>saklar utama</b> yang menentukan apakah
	 * dokumen ini masih ditagih.
	 *
	 * <p>Seluruh gerbang di kelas ini, panel {@code VerifikasiPSBHelper}, dasbor ringkasan
	 * {@code CalonSiswaAction}, dan jalur JSP {@code VerifikasiPSBHtmlHelper} melewati baris yang
	 * tidak aktif. Menonaktifkan satu baris berlaku serentak untuk <b>semua gelombang</b> yang
	 * mengaitkannya, karena relasi many-to-many-nya tidak menyimpan status per-gelombang.</p>
	 *
	 * <p><b>Getter penormal, berbawaan {@code true}:</b> kolom {@code NULL} dibaca sebagai
	 * <i>aktif</i>. Untuk katalog persyaratan arah ini benar (baris warisan tetap ditagih, bukan
	 * diam-diam dilewati), tetapi perlu disadari bahwa &mdash; karena <i>property access</i>
	 * &mdash; normalisasi ini ikut tertulis kembali ke basis data pada flush berikutnya.</p>
	 *
	 * <p><b>Catatan penghapusan:</b> menonaktifkan adalah satu-satunya cara aman untuk "mencabut"
	 * sebuah dokumen persyaratan. Menghapus barisnya lewat tombol Hapus akan berbenturan dengan
	 * baris di {@code sekolah.gelombang_punya_verifikasi_siswa} dan
	 * {@code sekolah.calon_siswa_punya_verifikasi_berkas} yang tidak ikut dibersihkan.</p>
	 *
	 * @return {@code true} bila dokumen masih ditagih; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris katalog. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" pada grid master, yang
	 * <b>langsung menyimpan seketika</b> lewat {@code Common.refreshSaveOrUpdate(...)} tanpa
	 * menunggu tombol Simpan. Checkbox itu dinonaktifkan bila pengguna tidak memiliki hak
	 * {@code UPDATE} &mdash; hak yang diwarisi dari menu "Gelombang Pendaftaran PSB" (lihat Javadoc
	 * kelas, bagian pewarisan hak).</p>
	 *
	 * @param aktif status baru; {@code null} diperbolehkan dan akan dibaca sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kebijakan <b>"berkas ini harus sudah DIUNGGAH sebelum boleh ikut ujian"</b>.
	 *
	 * <p>Dibaca oleh {@link #ambilPesanGagalSebelumUjian(CalonSiswa)} (dan pembungkusnya
	 * {@link #checkBerkasSebelumUjian(CalonSiswa)}), yang memeriksa keberadaan berkas fisik lewat
	 * {@code FileFotoLain.ambil(...)}. Perhatikan: yang diperiksa hanya <b>ada/tidaknya berkas</b>,
	 * bukan kebenaran isinya &mdash; berkas kosong atau salah tetap lolos.</p>
	 *
	 * <p><b>Cakupan penegakan tidak merata (lihat "TEMUAN 2" pada Javadoc kelas):</b> bendera ini
	 * hanya ditegakkan pada jalur ZK {@code TampilanPengumumanAkademisAction} ("Ikut Ujian
	 * Sekarang"). Berkas layanan portal publik
	 * {@code /WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp} <b>tidak memanggil gerbang
	 * ini sama sekali</b>.</p>
	 *
	 * <p><b>Getter penormal:</b> {@code null} dibaca sebagai {@code false} (tidak wajib).</p>
	 *
	 * @return {@code true} bila berkas wajib sudah terunggah sebelum ujian; tidak pernah {@code null}
	 */
	public Boolean getWajibUploadSebelumUjian() {
		return wajibUploadSebelumUjian == null ? false : wajibUploadSebelumUjian;
	}

	/**
	 * Menyetel kebijakan wajib-unggah-sebelum-ujian. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Wajib Upload sebelum Ujian" pada grid
	 * master, yang menyimpan seketika. Kolomnya juga termasuk dalam daftar
	 * {@code contents} untuk cetak dan <b>unggah massal</b> ({@code Common.uploadData(...)}),
	 * sehingga nilainya dapat diubah borongan lewat berkas Excel oleh pengguna yang memegang
	 * ketiga hak {@code CREATE}+{@code UPDATE}+{@code DELETE}.</p>
	 *
	 * @param wajibUploadSebelumUjian kebijakan baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setWajibUploadSebelumUjian(Boolean wajibUploadSebelumUjian) {
		this.wajibUploadSebelumUjian = wajibUploadSebelumUjian;
	}

	/**
	 * Mengembalikan kebijakan <b>"berkas ini harus sudah DIUNGGAH sebelum boleh ikut wawancara"</b>.
	 *
	 * <p>Padanan {@link #getWajibUploadSebelumUjian()} untuk tahap wawancara; dibaca oleh
	 * {@link #ambilPesanGagalSebelumInterview(CalonSiswa, Session)}. Berbeda dari saudaranya,
	 * bendera ini <b>ditegakkan pada portal publik</b>: {@code _wawancara_service.jsp} memanggil
	 * gerbangnya pada aksi {@code get_data} maupun saat konfirmasi kesiapan.</p>
	 *
	 * <p><b>Getter penormal:</b> {@code null} dibaca sebagai {@code false} (tidak wajib).</p>
	 *
	 * @return {@code true} bila berkas wajib sudah terunggah sebelum wawancara; tidak pernah
	 *         {@code null}
	 */
	public Boolean getWajibUploadSebelumInterview() {
		return wajibUploadSebelumInterview == null ? false : wajibUploadSebelumInterview;
	}

	/**
	 * Menyetel kebijakan wajib-unggah-sebelum-wawancara. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Wajib Upload sebelum Interview" pada grid
	 * master (menyimpan seketika) dan dapat diubah borongan lewat unggah Excel.</p>
	 *
	 * @param wajibUploadSebelumInterview kebijakan baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setWajibUploadSebelumInterview(Boolean wajibUploadSebelumInterview) {
		this.wajibUploadSebelumInterview = wajibUploadSebelumInterview;
	}

	/**
	 * Mengembalikan kebijakan <b>"berkas ini harus sudah DIVERIFIKASI petugas sebelum boleh ikut
	 * ujian"</b>.
	 *
	 * <p><b>PERINGATAN &mdash; kebijakan ini TIDAK PERNAH DITEGAKKAN.</b> Satu-satunya pembacanya
	 * adalah {@link #ambilPesanGagalVerifikasiSebelumUjian(CalonSiswa, Session)}, dan method itu
	 * <b>tidak dipanggil dari mana pun</b> di seluruh pohon sumber (terverifikasi menyeluruh atas
	 * {@code src/**} dan {@code webapp/**}). Checkbox "Wajib Verifikasi sebelum Ujian" pada layar
	 * master tersimpan, terekam Envers, dan ikut terekspor &mdash; tetapi tidak memblokir siapa
	 * pun.</p>
	 *
	 * <p>Yang <i>mirip</i> namun berbeda: {@link #checkBerkas(CalonSiswa)} juga memeriksa status
	 * {@code verified}, namun pemicunya adalah bendera
	 * {@code GelombangPendaftaranPsb.getCetakKartuUjianHarusVerifikasiBerkas()} yang berlaku
	 * <b>global untuk semua dokumen aktif gelombang itu</b> dan hanya mengunci <b>cetak kartu
	 * ujian</b> &mdash; bukan bendera per-dokumen ini, dan bukan tahap "ikut ujian". Jadi tidak ada
	 * jalur mana pun yang menerjemahkan kebijakan per-dokumen ini menjadi tindakan.</p>
	 *
	 * <p><b>Getter penormal:</b> {@code null} dibaca sebagai {@code false}.</p>
	 *
	 * @return nilai kebijakan sebagaimana tersimpan; tidak pernah {@code null}. Tidak berpengaruh
	 *         pada perilaku sistem saat ini.
	 * @see #ambilPesanGagalVerifikasiSebelumUjian(CalonSiswa, Session)
	 */
	public Boolean getWajibVerifikasiSebelumUjian() {
		return wajibVerifikasiSebelumUjian == null ? false : wajibVerifikasiSebelumUjian;
	}

	/**
	 * Menyetel kebijakan wajib-verifikasi-sebelum-ujian. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Wajib Verifikasi sebelum Ujian" pada grid
	 * master, yang menyimpan seketika. <b>Menyetelnya tidak mengubah perilaku sistem</b> selama
	 * {@link #ambilPesanGagalVerifikasiSebelumUjian(CalonSiswa, Session)} tetap tanpa pemanggil
	 * &mdash; lihat peringatan pada {@link #getWajibVerifikasiSebelumUjian()}.</p>
	 *
	 * @param wajibVerifikasiSebelumUjian kebijakan baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setWajibVerifikasiSebelumUjian(Boolean wajibVerifikasiSebelumUjian) {
		this.wajibVerifikasiSebelumUjian = wajibVerifikasiSebelumUjian;
	}

	/**
	 * Mengembalikan kebijakan <b>"berkas ini harus sudah DIVERIFIKASI petugas sebelum boleh ikut
	 * wawancara"</b>.
	 *
	 * <p><b>PERINGATAN &mdash; kebijakan ini TIDAK PERNAH DITEGAKKAN</b>, dengan alasan yang persis
	 * sama seperti {@link #getWajibVerifikasiSebelumUjian()}: pembaca satu-satunya
	 * ({@link #ambilPesanGagalVerifikasiSebelumInterview(CalonSiswa, Session)}) tidak dipanggil dari
	 * mana pun. Portal wawancara ({@code _wawancara_service.jsp}) hanya memanggil varian
	 * <b>unggah</b>-nya, sehingga berkas yang sudah terunggah namun ditolak/belum diperiksa petugas
	 * tetap meloloskan calon siswa ke wawancara.</p>
	 *
	 * <p><b>Getter penormal:</b> {@code null} dibaca sebagai {@code false}.</p>
	 *
	 * @return nilai kebijakan sebagaimana tersimpan; tidak pernah {@code null}. Tidak berpengaruh
	 *         pada perilaku sistem saat ini.
	 * @see #ambilPesanGagalVerifikasiSebelumInterview(CalonSiswa, Session)
	 */
	public Boolean getWajibVerifikasiSebelumInterview() {
		return wajibVerifikasiSebelumInterview == null ? false : wajibVerifikasiSebelumInterview;
	}

	/**
	 * Menyetel kebijakan wajib-verifikasi-sebelum-wawancara. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Wajib Verifikasi sebelum Interview" pada
	 * grid master, yang menyimpan seketika. <b>Menyetelnya tidak mengubah perilaku sistem</b>
	 * &mdash; lihat peringatan pada {@link #getWajibVerifikasiSebelumInterview()}.</p>
	 *
	 * @param wajibVerifikasiSebelumInterview kebijakan baru; {@code null} dibaca sebagai
	 *                                        {@code false}
	 */
	public void setWajibVerifikasiSebelumInterview(Boolean wajibVerifikasiSebelumInterview) {
		this.wajibVerifikasiSebelumInterview = wajibVerifikasiSebelumInterview;
	}

	/**
	 * Menyusun pesan kendala bila ada dokumen ber-{@link #getWajibVerifikasiSebelumUjian()} yang
	 * <b>belum diverifikasi petugas</b>, untuk tahap <b>ikut ujian</b>.
	 *
	 * <p><b>KODE MATI &mdash; nol pemanggil.</b> Pencarian menyeluruh atas {@code src/**} dan
	 * {@code webapp/**} tidak menemukan satu pun pemanggil method ini. Akibatnya kebijakan
	 * "Wajib Verifikasi sebelum Ujian" yang dicentang panitia <b>tidak pernah menghalangi
	 * siapa pun</b>. Ini kontrol keamanan yang tampak di layar tetapi tidak tersambung ke jalur
	 * eksekusi &mdash; lihat "TEMUAN 1" pada Javadoc kelas. Bandingkan dengan modul PMB, yang
	 * padanannya benar-benar terpakai lewat {@code VerifikasiPMBHelper}.</p>
	 *
	 * <p><b>Cara kerja</b> (bila kelak disambungkan): mengambil gelombang milik {@code calonSiswa},
	 * membaca koleksi many-to-many dokumen wajibnya, mengurutkannya, lalu untuk setiap dokumen yang
	 * {@link #getAktif() aktif} <b>dan</b> ber-{@code wajibVerifikasiSebelumUjian} mencari baris
	 * {@link CalonSiswaPunyaVerifikasiBerkas} pasangannya. Dokumen pertama yang barisnya tidak ada
	 * atau ber-{@code verified = false} menghentikan pemeriksaan dan mengembalikan pesan berisi
	 * nama dokumen tersebut plus tiga langkah perbaikan.</p>
	 *
	 * <p><b>Perbedaan penting dari {@link #ambilPesanGagalSebelumUjian(CalonSiswa)}:</b> method ini
	 * <b>tidak membuat</b> baris {@link CalonSiswaPunyaVerifikasiBerkas} yang belum ada &mdash; ia
	 * memperlakukan ketiadaan baris sebagai "belum diverifikasi" dan langsung menolak. Jadi
	 * satu-satunya method gerbang di kelas ini yang <b>bebas efek samping tulis</b> (bersama
	 * saudaranya untuk wawancara).</p>
	 *
	 * <p><b>Fail-open:</b> setiap galat ditelan {@code catch} terluar dan method mengembalikan
	 * {@code null} (= "silakan lanjut"). Gelombang {@code null} juga mengembalikan {@code null}.</p>
	 *
	 * <p><b>Tidak menyaring tenant</b> &mdash; penyaringan sepenuhnya bergantung pada gelombang
	 * milik calon siswa yang diberikan pemanggil.</p>
	 *
	 * @param calonSiswa      calon siswa yang diperiksa kelayakannya; gelombangnya diambil lewat
	 *                        {@code getGelombangPendaftaranPsb()}
	 * @param externalSession sesi Hibernate milik pemanggil. Parameter ini ada supaya method aman
	 *                        dipanggil dari JSP yang membuka sesinya sendiri
	 *                        ({@code HibernateUtil.openSession()}) alih-alih memakai sesi thread ZK
	 * @return pesan siap tampil bila ada dokumen yang belum diverifikasi; {@code null} bila semua
	 *         aman, gelombang tidak ada, koleksi kosong, atau terjadi galat
	 */
	public static String ambilPesanGagalVerifikasiSebelumUjian(CalonSiswa calonSiswa, org.hibernate.Session externalSession) {
		GelombangPendaftaranPsb myGelombang = calonSiswa.getGelombangPendaftaranPsb();
		if (myGelombang == null) { return null; }
		try {
			java.util.Set<VerifikasiKelengkapanCalonSiswa> set = myGelombang.getVerifikasiKelengkapanCalonSiswas();
			if (set == null) { return null; }
			List<VerifikasiKelengkapanCalonSiswa> list = new ArrayList<VerifikasiKelengkapanCalonSiswa>(set);
			try { Collections.sort(list); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:178");}
			for (VerifikasiKelengkapanCalonSiswa v : list) {
				if (!v.getAktif() || !v.getWajibVerifikasiSebelumUjian()) { continue; }
				CalonSiswaPunyaVerifikasiBerkas cpvb = (CalonSiswaPunyaVerifikasiBerkas) externalSession
						.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
						.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa", v))
						.add(Restrictions.eq("calonSiswa", calonSiswa))
						.setMaxResults(1).uniqueResult();
				if (cpvb == null || !cpvb.getVerified()) {
					String nama = v.getNama();
					return "Maaf, Anda belum dapat mengikuti Ujian karena berkas \"" + nama + "\" belum diverifikasi oleh panitia.\n\n"
							+ "Langkah yang perlu dilakukan:\n"
							+ "1. Pastikan berkas \"" + nama + "\" sudah diunggah dengan benar.\n"
							+ "2. Tunggu proses verifikasi dari panitia PPDB.\n"
							+ "3. Setelah berkas terverifikasi, kembali ke halaman ini dan klik \"Ikut Ujian\" kembali.\n\n"
							+ "Jika mengalami kendala, hubungi panitia penerimaan siswa baru untuk mendapatkan bantuan.";
				}
			}
		} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:196"); }
		return null;
	}

	/**
	 * Menyusun pesan kendala bila ada dokumen ber-{@link #getWajibVerifikasiSebelumInterview()} yang
	 * <b>belum diverifikasi petugas</b>, untuk tahap <b>wawancara</b>.
	 *
	 * <p><b>KODE MATI &mdash; nol pemanggil</b>, persis seperti saudaranya
	 * {@link #ambilPesanGagalVerifikasiSebelumUjian(CalonSiswa, Session)}. Portal wawancara
	 * {@code _wawancara_service.jsp} hanya memanggil varian <b>unggah</b>
	 * ({@link #ambilPesanGagalSebelumInterview(CalonSiswa, Session)}), sehingga berkas yang sudah
	 * diunggah namun <b>ditolak atau belum diperiksa</b> petugas tetap meloloskan calon siswa ke
	 * wawancara. Lihat "TEMUAN 1" pada Javadoc kelas.</p>
	 *
	 * <p>Logikanya identik dengan saudaranya kecuali bendera yang dibaca
	 * ({@code wajibVerifikasiSebelumInterview}) dan kata "Wawancara" pada pesan. Sama-sama
	 * <b>tidak menulis</b> apa pun ke basis data, dan sama-sama <b>fail-open</b> (galat maupun
	 * gelombang {@code null} menghasilkan {@code null} = lolos).</p>
	 *
	 * @param calonSiswa      calon siswa yang diperiksa kelayakannya
	 * @param externalSession sesi Hibernate milik pemanggil, agar aman dipanggil dari JSP yang
	 *                        membuka sesinya sendiri
	 * @return pesan siap tampil bila ada dokumen yang belum diverifikasi; {@code null} bila aman
	 *         atau terjadi galat
	 */
	public static String ambilPesanGagalVerifikasiSebelumInterview(CalonSiswa calonSiswa, org.hibernate.Session externalSession) {
		GelombangPendaftaranPsb myGelombang = calonSiswa.getGelombangPendaftaranPsb();
		if (myGelombang == null) { return null; }
		try {
			java.util.Set<VerifikasiKelengkapanCalonSiswa> set = myGelombang.getVerifikasiKelengkapanCalonSiswas();
			if (set == null) { return null; }
			List<VerifikasiKelengkapanCalonSiswa> list = new ArrayList<VerifikasiKelengkapanCalonSiswa>(set);
			try { Collections.sort(list); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:208");}
			for (VerifikasiKelengkapanCalonSiswa v : list) {
				if (!v.getAktif() || !v.getWajibVerifikasiSebelumInterview()) { continue; }
				CalonSiswaPunyaVerifikasiBerkas cpvb = (CalonSiswaPunyaVerifikasiBerkas) externalSession
						.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
						.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa", v))
						.add(Restrictions.eq("calonSiswa", calonSiswa))
						.setMaxResults(1).uniqueResult();
				if (cpvb == null || !cpvb.getVerified()) {
					String nama = v.getNama();
					return "Maaf, Anda belum dapat mengikuti Wawancara karena berkas \"" + nama + "\" belum diverifikasi oleh panitia.\n\n"
							+ "Langkah yang perlu dilakukan:\n"
							+ "1. Pastikan berkas \"" + nama + "\" sudah diunggah dengan benar.\n"
							+ "2. Tunggu proses verifikasi dari panitia PPDB.\n"
							+ "3. Setelah berkas terverifikasi, kembali ke halaman ini dan klik \"Wawancara\" kembali.\n\n"
							+ "Jika mengalami kendala, hubungi panitia penerimaan siswa baru untuk mendapatkan bantuan.";
				}
			}
		} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:226"); }
		return null;
	}

	/**
	 * Mengembalikan sekolah pemilik baris katalog ini.
	 *
	 * <p>Bersama {@link #getYayasan()} membentuk cakupan tenant katalog: layar master menyaring
	 * daftar berdasarkan kedua kolom ini, dan dokumen persyaratan satu sekolah tidak seharusnya
	 * muncul di sekolah lain.</p>
	 *
	 * <p><b>Efek samping (aman):</b> memanggil {@link GeneralValueObject#check(Object)} dan
	 * menugaskan ulang hasilnya ke field. Ini <b>memoisasi resolusi proxy lazy</b> (kolomnya
	 * {@code FetchType.LAZY}) agar entity tetap terbaca setelah sesi Hibernate-nya ditutup &mdash;
	 * penting karena gerbang-gerbang statis di kelas ini kerap dipanggil dari JSP dengan sesi
	 * berumur pendek. Bukan pola destruktif: {@code check()} mengembalikan argumennya apa adanya
	 * bila keempat sumber resolusinya gagal.</p>
	 *
	 * <p><b>Boleh {@code null}</b> &mdash; dan pada instalasi nyata memang sering {@code null}:
	 * rutin auto-seed layar master menyetelnya dari {@code SekolahUtil.getSekolah()}, yang pada sesi
	 * tanpa konteks sekolah mengembalikan {@code new Sekolah()} berid-{@code null} sehingga
	 * {@link #setSekolah(Sekolah)} menormalkannya menjadi {@code null}. Baris seperti itu menjadi
	 * katalog global yatim (lihat "e. Auto-seed" pada Javadoc kelas).</p>
	 *
	 * @return sekolah pemilik; {@code null} pada baris global/yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris katalog.
	 *
	 * <p><b>Setter penormal:</b> argumen {@code null} <i>atau</i> object berid-{@code null}
	 * disimpan sebagai {@code null} asli. Normalisasi ini penting karena
	 * {@code SekolahUtil.getSekolah()} tidak pernah mengembalikan {@code null} melainkan
	 * {@code new Sekolah()} kosong (cacat yang dikonfirmasi batch 67/71); tanpa penjaga ini,
	 * Hibernate akan mencoba mem-{@code persist} sekolah kosong itu karena
	 * {@code CascadeType.PERSIST} aktif.</p>
	 *
	 * <p>Efek sampingnya: baris yang dibuat pada sesi tanpa konteks sekolah <b>kehilangan
	 * tenant secara senyap</b> alih-alih gagal dengan jelas.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object berid-{@code null} disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris katalog &mdash; <b>getter DESTRUKTIF, baca peringatan
	 * di bawah sebelum memakainya</b>.
	 *
	 * <p><b>Efek samping merusak data.</b> Method ini tidak sekadar membaca: ia memanggil
	 * {@link #getSekolah()}, lalu &mdash; bila sekolahnya ada &mdash; <b>menimpa field
	 * {@code yayasan} dengan {@code sekolah.getYayasan()}</b>. Karena entity ini memakai
	 * <i>property access</i> (anotasi {@code @Id} berada pada getter), nilai hasil getter itulah
	 * yang dibaca Hibernate saat dirty-check, sehingga <b>sekadar membaca entity dalam sesi hidup
	 * dapat menerbitkan {@code UPDATE}</b> beserta revisi Envers yang tidak berasal dari tindakan
	 * pengguna mana pun. Dua akibat konkret:</p>
	 * <ul>
	 * <li>Baris yang sengaja dimiliki yayasan berbeda dari yayasan sekolahnya akan
	 * <b>dipindahkan diam-diam</b> mengikuti sekolah.</li>
	 * <li>Bila {@code sekolah.getYayasan()} bernilai {@code null} &mdash; sah, karena relasi
	 * {@code Sekolah}&rarr;{@code Yayasan} bersifat opsional (batch 71) &mdash; maka
	 * <b>{@code yayasan} baris ini dikosongkan</b>. Baris itu lalu hilang dari setiap pencarian
	 * yang memfilter yayasan, termasuk {@code initCriteria()} layar masternya sendiri, padahal
	 * {@link #checkBerkas(CalonSiswa)} tetap menagihnya.</li>
	 * </ul>
	 * <p>Ini adalah varian pola "getter penulis-balik" yang sama seperti pada {@code Sekolah}
	 * (batch 71), tetapi lebih agresif: {@code check()} bersifat memoisasi non-destruktif,
	 * sedangkan penurunan nilai dari {@code sekolah} di sini <b>menggantikan</b> data yang tersimpan.
	 * Pemanggil yang hanya ingin membaca nilai tersimpan sebaiknya membaca kolomnya lewat query,
	 * bukan lewat getter ini.</p>
	 *
	 * <p>Setelah penurunan nilai, hasilnya masih dilewatkan {@link GeneralValueObject#check(Object)}
	 * untuk resolusi proxy lazy.</p>
	 *
	 * @return yayasan pemilik &mdash; hasil penurunan dari sekolah bila sekolahnya ada, atau nilai
	 *         tersimpan bila sekolahnya {@code null}; boleh {@code null}
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
	 * Menyetel yayasan pemilik baris katalog.
	 *
	 * <p><b>Setter penormal</b> dengan semantik yang sama seperti {@link #setSekolah(Sekolah)}:
	 * {@code null} atau object berid-{@code null} disimpan sebagai {@code null} asli, sehingga
	 * {@code CascadeType.PERSIST} tidak mencoba menyimpan yayasan kosong.</p>
	 *
	 * <p><b>Nilai yang disetel di sini mudah hilang:</b> {@link #getYayasan()} akan menimpanya
	 * dengan yayasan milik {@link #getSekolah()} pada pembacaan berikutnya. Menyetel yayasan yang
	 * berbeda dari yayasan sekolah karena itu tidak pernah bertahan.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object berid-{@code null} disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Gerbang <b>cetak kartu ujian</b>: memeriksa apakah seluruh dokumen persyaratan aktif milik
	 * gelombang {@code calonSiswa} sudah ber-{@code verified = true}, dan <b>menampilkan
	 * Messagebox</b> berisi daftar dokumen yang belum lolos bila ada.
	 *
	 * <p><b>Pemanggil terverifikasi:</b> {@code CommonReportPsb.onCetakKartuUjianPSB(...)} (langkah
	 * pertama sebelum syarat bayar dan penetapan ruang ujian) dan
	 * {@code TampilanPengumumanAkademisAction} pada tombol "Ikut Ujian Sekarang" (dipanggil setelah
	 * {@link #checkBerkasSebelumUjian(CalonSiswa)}).</p>
	 *
	 * <p><b>Pemicunya bukan bendera per-dokumen.</b> Seluruh badan method dibungkus syarat
	 * {@code myGelombangPendaftaranPsb.getCetakKartuUjianHarusVerifikasiBerkas()} &mdash; sebuah
	 * bendera <b>tingkat gelombang</b>. Bila bendera itu mati, method langsung mengembalikan
	 * {@code true} tanpa memeriksa apa pun. Bila menyala, <b>semua</b> dokumen
	 * {@link #getAktif() aktif} diperiksa, tanpa memandang keempat bendera {@code wajib*}
	 * per-dokumen.</p>
	 *
	 * <h3>Efek samping</h3>
	 * <ol>
	 * <li><b>{@code refresh()} paksa</b> atas entity gelombang di awal (dibungkus {@code try} yang
	 * menelan galat), untuk memastikan koleksi many-to-many-nya mutakhir.</li>
	 * <li><b>MENULIS ke basis data.</b> Untuk setiap dokumen aktif yang belum punya pasangan, method
	 * membuat {@link CalonSiswaPunyaVerifikasiBerkas} baru dan langsung menyimpannya lewat
	 * {@code Common.refreshSaveOrUpdate(...)}. Jadi operasi yang secara semantik hanya "memeriksa
	 * kelayakan" <b>menerbitkan baris baru beserta revisi Envers</b>. Tabel transaksi tumbuh sebesar
	 * (jumlah calon siswa &times; jumlah dokumen aktif) tanpa tindakan pengguna yang eksplisit.</li>
	 * <li><b>Menampilkan UI dari lapisan model</b> ({@code MyMessageboxConfig.showFormat(...)}),
	 * sehingga method ini <b>tidak aman dipanggil dari luar thread event ZK</b> &mdash; termasuk dari
	 * JSP atau job latar. Pemanggil non-ZK harus memakai varian {@code ambilPesanGagal*}.</li>
	 * </ol>
	 *
	 * <h3>Perilaku kegagalan</h3>
	 * <ul>
	 * <li><b>{@code NullPointerException} bila gelombang {@code null}.</b> Pemanggilan
	 * {@code myGelombangPendaftaranPsb.getCetakKartuUjianHarusVerifikasiBerkas()} berada
	 * <b>di luar</b> blok {@code try}, sehingga calon siswa tanpa gelombang membuat method ini
	 * melempar &mdash; berbeda dari keempat method {@code ambilPesanGagal*} yang justru
	 * mengembalikan {@code null} (lolos). Ketidakkonsistenan ini nyata dan patut diwaspadai
	 * pemanggil baru.</li>
	 * <li><b>Fail-open</b> untuk galat lainnya: seluruh pemeriksaan dibungkus {@code catch
	 * (Exception)} yang hanya mencatat, lalu eksekusi jatuh ke {@code return true} &mdash; kartu
	 * ujian tetap tercetak.</li>
	 * <li><b>Tidak menyaring tenant</b>; cakupan sepenuhnya ditentukan gelombang milik calon siswa.</li>
	 * </ul>
	 *
	 * <p><b>Catatan pengurutan:</b> koleksi disalin ke {@code ArrayList} lalu
	 * {@code Collections.sort(...)} &mdash; <b>tanpa</b> penciutan {@code TreeSet}. Karena panel
	 * unggah ({@code VerifikasiPSBHelper}/{@code CalonSiswaAction}) justru memakai {@code TreeSet},
	 * dua dokumen bernama sama akan ditagih di sini tetapi hanya satu yang dapat diunggah &mdash;
	 * lihat "TEMUAN 5" pada Javadoc kelas.</p>
	 *
	 * @param calonSiswa calon siswa yang diperiksa; gelombangnya diambil lewat
	 *                   {@code getGelombangPendaftaranPsb()} dan <b>tidak boleh {@code null}</b>
	 * @return {@code true} bila kartu ujian boleh dicetak (semua dokumen aktif terverifikasi,
	 *         bendera gelombang mati, atau terjadi galat); {@code false} bila ada dokumen yang belum
	 *         terverifikasi &mdash; dalam hal ini Messagebox peringatan sudah ditampilkan
	 */
	public static boolean checkBerkas(CalonSiswa calonSiswa) {
		GelombangPendaftaranPsb myGelombangPendaftaranPsb = calonSiswa.getGelombangPendaftaranPsb();

		try {

			HibernateUtil.currentSession().refresh(myGelombangPendaftaranPsb);

			
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:264");
			// TODO: handle exception
		}

		if (myGelombangPendaftaranPsb.getCetakKartuUjianHarusVerifikasiBerkas()) {
			try {

				Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswasTemp = myGelombangPendaftaranPsb
						.getVerifikasiKelengkapanCalonSiswas();

				List<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = new ArrayList<VerifikasiKelengkapanCalonSiswa>(
						verifikasiKelengkapanCalonSiswasTemp);

				try {
					Collections.sort(verifikasiKelengkapanCalonSiswas);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:279");
					// TODO: handle exception
				}
				String s = "";
				Session session = HibernateUtil.currentSession();
				for (VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa : verifikasiKelengkapanCalonSiswas) {
					if (verifikasiKelengkapanCalonSiswa.getAktif()) {
						CalonSiswaPunyaVerifikasiBerkas calonSiswaPunyaVerifikasiBerkas = (CalonSiswaPunyaVerifikasiBerkas) session
								.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
								.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa",
										verifikasiKelengkapanCalonSiswa))
								.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

						if (calonSiswaPunyaVerifikasiBerkas == null) {
							calonSiswaPunyaVerifikasiBerkas = new CalonSiswaPunyaVerifikasiBerkas();
							calonSiswaPunyaVerifikasiBerkas.setCalonSiswa(calonSiswa);
							calonSiswaPunyaVerifikasiBerkas
									.setVerifikasiKelengkapanCalonSiswa(verifikasiKelengkapanCalonSiswa);
							Common.refreshSaveOrUpdate(session, calonSiswaPunyaVerifikasiBerkas);
						}

						if (!calonSiswaPunyaVerifikasiBerkas.getVerified()) {
							s += s.isEmpty() ? verifikasiKelengkapanCalonSiswa.getNama()
									: "\n" + verifikasiKelengkapanCalonSiswa.getNama();
						}

					}
				}
				if (!s.isEmpty()) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu. Terdapat persyaratan yang belum diverifikasi, yaitu sebagai berikut:\n\n{V1}\n\nLangkah yang dapat dilakukan: (1) periksa kembali kelengkapan berkas persyaratan; (2) lakukan verifikasi terhadap seluruh persyaratan tersebut; (3) ulangi proses setelah seluruh persyaratan terverifikasi.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, s);
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:314");
			}

		}
		return true;
	}

	/**
	 * Menyusun pesan kendala bila ada dokumen ber-{@link #getWajibUploadSebelumUjian()} yang
	 * <b>belum diunggah</b> oleh {@code calonSiswa}.
	 *
	 * <p>Ini gerbang <b>"berkasnya ada"</b>, bukan "berkasnya benar": yang diperiksa hanyalah
	 * keberadaan berkas fisik lewat
	 * {@code FileFotoLain.ambil(false, <id transaksi>, CalonSiswaPunyaVerifikasiBerkas.class.getName(),
	 * LampiranLain.class)}. Berkas kosong, salah dokumen, atau yang sudah ditolak petugas tetap
	 * dianggap memenuhi syarat &mdash; pemeriksaan status {@code verified} adalah urusan
	 * {@link #ambilPesanGagalVerifikasiSebelumUjian(CalonSiswa, Session)} yang tidak pernah
	 * dipanggil.</p>
	 *
	 * <p><b>Pemanggil terverifikasi:</b> hanya {@link #checkBerkasSebelumUjian(CalonSiswa)}, yang
	 * pada gilirannya hanya dipanggil dari tombol "Ikut Ujian Sekarang" di
	 * {@code TampilanPengumumanAkademisAction}. Portal PPDB versi JSP
	 * ({@code _ikut_ujian_online_service.jsp}) <b>tidak memanggilnya</b> &mdash; lihat "TEMUAN 2"
	 * pada Javadoc kelas.</p>
	 *
	 * <p><b>MENULIS ke basis data.</b> Sama seperti {@link #checkBerkas(CalonSiswa)}, dokumen aktif
	 * yang belum punya pasangan {@link CalonSiswaPunyaVerifikasiBerkas} dibuatkan barisnya dan
	 * langsung disimpan. Baris yang baru saja dibuat pasti belum punya berkas, sehingga pemeriksaan
	 * berikutnya langsung menghasilkan pesan kendala.</p>
	 *
	 * <p><b>Memakai sesi thread ZK</b> ({@code HibernateUtil.currentSession()}) dan memanggil
	 * {@code session.refresh(gelombang)}. Meski komentar aslinya menyebut "aman dipanggil dari JSP",
	 * varian yang benar-benar aman untuk JSP adalah
	 * {@link #ambilPesanGagalSebelumInterview(CalonSiswa, Session)} yang menerima sesi eksternal.
	 * Method ini sendiri tidak menampilkan UI, jadi setidaknya bebas dari ketergantungan thread
	 * event ZK.</p>
	 *
	 * <p><b>Fail-open</b> menyeluruh: gelombang {@code null} &rarr; {@code null}; koleksi
	 * {@code null} &rarr; {@code null}; galat apa pun ditelan dan method jatuh ke {@code return
	 * null} &mdash; semuanya berarti "silakan ikut ujian".</p>
	 *
	 * @param calonSiswa calon siswa yang diperiksa kelengkapan berkasnya
	 * @return pesan lima langkah siap tampil (menyebut nama dokumen yang belum diunggah) bila ada
	 *         yang kurang; {@code null} bila lengkap, tidak ada gelombang, atau terjadi galat
	 */
	public static String ambilPesanGagalSebelumUjian(CalonSiswa calonSiswa) {
		GelombangPendaftaranPsb myGelombangPendaftaranPsb = calonSiswa.getGelombangPendaftaranPsb();
		if (myGelombangPendaftaranPsb == null) {
			return null;
		}
		try {
			Session session = HibernateUtil.currentSession();
			session.refresh(myGelombangPendaftaranPsb);

			Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswasTemp = myGelombangPendaftaranPsb
					.getVerifikasiKelengkapanCalonSiswas();
			if (verifikasiKelengkapanCalonSiswasTemp == null) {
				return null;
			}

			List<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = new ArrayList<VerifikasiKelengkapanCalonSiswa>(
					verifikasiKelengkapanCalonSiswasTemp);
			try {
				Collections.sort(verifikasiKelengkapanCalonSiswas);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:341");
			}

			for (VerifikasiKelengkapanCalonSiswa v : verifikasiKelengkapanCalonSiswas) {
				if (!v.getAktif()) {
					continue;
				}
				if (!v.getWajibUploadSebelumUjian()) {
					continue;
				}

				CalonSiswaPunyaVerifikasiBerkas calonSiswaPunyaVerifikasiBerkas = (CalonSiswaPunyaVerifikasiBerkas) session
						.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
						.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa", v))
						.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

				if (calonSiswaPunyaVerifikasiBerkas == null) {
					calonSiswaPunyaVerifikasiBerkas = new CalonSiswaPunyaVerifikasiBerkas();
					calonSiswaPunyaVerifikasiBerkas.setCalonSiswa(calonSiswa);
					calonSiswaPunyaVerifikasiBerkas.setVerifikasiKelengkapanCalonSiswa(v);
					Common.refreshSaveOrUpdate(session, calonSiswaPunyaVerifikasiBerkas);
				}

				FileFotoLain fileFotoLain = calonSiswaPunyaVerifikasiBerkas.getId() == null ? null
						: FileFotoLain.ambil(false, calonSiswaPunyaVerifikasiBerkas.getId(),
								CalonSiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class);
				if (fileFotoLain == null) {
					String nama = v.getNama();
					return "Maaf, Anda belum dapat mengikuti Ujian karena berkas \"" + nama + "\" belum diunggah.\n\n"
							+ "Langkah yang perlu dilakukan:\n"
							+ "1. Tutup jendela ini.\n"
							+ "2. Buka menu \"Kelengkapan Berkas\" atau \"Unggah Berkas\" di halaman utama PPDB.\n"
							+ "3. Cari berkas \"" + nama + "\", lalu klik tombol Unggah.\n"
							+ "4. Pilih file dari perangkat Anda (format: PDF, JPG, atau PNG).\n"
							+ "5. Setelah berhasil diunggah, kembali ke halaman ini dan klik \"Ikut Ujian\" kembali.\n\n"
							+ "Jika mengalami kendala:\n"
							+ "- Pastikan format dan ukuran file sesuai ketentuan panitia.\n"
							+ "- Coba gunakan browser lain (Chrome / Firefox) jika proses unggah gagal.\n"
							+ "- Hubungi panitia penerimaan siswa baru untuk mendapatkan bantuan lebih lanjut.";
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:383");
		}
		return null;
	}

	/**
	 * Pembungkus ZK atas {@link #ambilPesanGagalSebelumUjian(CalonSiswa)}: bila ada pesan kendala,
	 * menampilkannya sebagai Messagebox dan mengembalikan {@code false}.
	 *
	 * <p><b>Pemanggil terverifikasi satu-satunya:</b> {@code TampilanPengumumanAkademisAction},
	 * listener tombol "Ikut Ujian Sekarang", dipanggil sebagai gerbang kedua setelah
	 * {@code GelombangPendaftaranPsb.chekSyaratBayar(...)} dan sebelum
	 * {@link #checkBerkas(CalonSiswa)}.</p>
	 *
	 * <p><b>Hanya aman di thread event ZK</b> karena memanggil
	 * {@code MyMessageboxConfig.show(...)}. Pemanggil JSP/latar harus memakai
	 * {@link #ambilPesanGagalSebelumUjian(CalonSiswa)} langsung. Efek samping tulis milik method
	 * yang dibungkusnya (pembuatan baris {@link CalonSiswaPunyaVerifikasiBerkas}) tetap berlaku.</p>
	 *
	 * <p><b>Fail-open ganda:</b> selain fail-open pada method yang dibungkus, kegagalan menampilkan
	 * Messagebox pun ditelan &mdash; namun di sini arahnya benar, karena method tetap mengembalikan
	 * {@code false} (menolak) setelah gagal menampilkan pesan.</p>
	 *
	 * @param calonSiswa calon siswa yang hendak mengikuti ujian
	 * @return {@code true} bila boleh lanjut; {@code false} bila ada berkas wajib yang belum
	 *         diunggah (Messagebox sudah ditampilkan, atau setidaknya dicoba)
	 */
	public static boolean checkBerkasSebelumUjian(CalonSiswa calonSiswa) {
		String pesan = ambilPesanGagalSebelumUjian(calonSiswa);
		if (pesan != null) {
			try {
				MyMessageboxConfig.show(pesan, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:394");
			}
			return false;
		}
		return true;
	}

	/**
	 * Menyusun pesan kendala bila ada dokumen ber-{@link #getWajibUploadSebelumInterview()} yang
	 * <b>belum diunggah</b> oleh {@code calonSiswa}. Padanan
	 * {@link #ambilPesanGagalSebelumUjian(CalonSiswa)} untuk tahap wawancara, dengan satu perbedaan
	 * struktural penting: <b>sesi Hibernate diterima dari pemanggil</b>.
	 *
	 * <p><b>Pemanggil terverifikasi:</b> {@code /WEB-INF/baru/modul/ppdb/_wawancara_service.jsp}
	 * pada dua tempat &mdash; aksi {@code get_data} (sebelum menampilkan jadwal wawancara) dan
	 * konfirmasi kesiapan peserta. Berkas JSP itu membuka sesinya sendiri lewat
	 * {@code HibernateUtil.openSession()}, itulah alasan keberadaan parameter
	 * {@code externalSession}: memakai {@code currentSession()} dari konteks non-ZK akan gagal.</p>
	 *
	 * <p><b>Berbeda dari saudaranya, method ini tidak memanggil {@code refresh()}</b> atas
	 * gelombang &mdash; koleksi dokumen dibaca apa adanya dari sesi pemanggil.</p>
	 *
	 * <h3>PERINGATAN KEAMANAN &mdash; primitif TULIS yang terjangkau tanpa otentikasi</h3>
	 * <p>Method ini memakai pola "buat bila belum ada": dokumen aktif yang belum punya pasangan
	 * dibuatkan baris {@link CalonSiswaPunyaVerifikasiBerkas} dan <b>langsung disimpan</b> lewat
	 * {@code Common.refreshSaveOrUpdate(externalSession, cpvb)}. Sementara itu
	 * {@code _wawancara_service.jsp} memilih calon siswa <b>semata-mata dari
	 * {@code request.getParameter("id")}</b> &mdash; tanpa sesi login, tanpa pemeriksaan
	 * kepemilikan, tanpa penyaring sekolah/yayasan (terverifikasi langsung pada berkas JSP
	 * tersebut). Akibatnya permintaan HTTP anonim dengan id sembarang:</p>
	 * <ul>
	 * <li><b>menulis baris baru</b> ke {@code sekolah.calon_siswa_punya_verifikasi_berkas} beserta
	 * revisi Envers atas nama "tidak ada siapa-siapa"; dan</li>
	 * <li><b>membocorkan nama dokumen persyaratan</b> (dan dengan itu, keberadaan serta status
	 * kelengkapan berkas) milik calon siswa mana pun lintas sekolah/yayasan, lewat pesan kendala
	 * yang dikembalikan sebagai JSON.</li>
	 * </ul>
	 * <p>Temuan ini memperkuat {@code task_1f9c66d3} (dispatcher {@code /ppdb} anonim) dan
	 * {@code task_4ca32776} (kebocoran PII PSB); yang baru adalah bahwa <b>method statis pada entity
	 * model inilah</b> yang menjadi primitif tulisnya.</p>
	 *
	 * <p><b>Fail-open</b> menyeluruh, sama seperti saudara-saudaranya: gelombang {@code null},
	 * koleksi {@code null}, atau galat apa pun menghasilkan {@code null} = "silakan lanjut".</p>
	 *
	 * @param calonSiswa      calon siswa yang diperiksa kelengkapan berkasnya
	 * @param externalSession sesi Hibernate milik pemanggil; <b>wajib terbuka</b> karena dipakai
	 *                        untuk query maupun penyimpanan baris baru
	 * @return pesan lima langkah siap tampil (menyebut nama dokumen yang belum diunggah) bila ada
	 *         yang kurang; {@code null} bila lengkap, tidak ada gelombang, atau terjadi galat
	 */
	public static String ambilPesanGagalSebelumInterview(CalonSiswa calonSiswa, org.hibernate.Session externalSession) {
		GelombangPendaftaranPsb myGelombangPendaftaranPsb = calonSiswa.getGelombangPendaftaranPsb();
		if (myGelombangPendaftaranPsb == null) {
			return null;
		}
		try {
			Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswasTemp = myGelombangPendaftaranPsb
					.getVerifikasiKelengkapanCalonSiswas();
			if (verifikasiKelengkapanCalonSiswasTemp == null) {
				return null;
			}

			List<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas =
					new ArrayList<VerifikasiKelengkapanCalonSiswa>(verifikasiKelengkapanCalonSiswasTemp);
			try { Collections.sort(verifikasiKelengkapanCalonSiswas); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:421");}

			for (VerifikasiKelengkapanCalonSiswa v : verifikasiKelengkapanCalonSiswas) {
				if (!v.getAktif() || !v.getWajibUploadSebelumInterview()) {
					continue;
				}

				CalonSiswaPunyaVerifikasiBerkas cpvb = (CalonSiswaPunyaVerifikasiBerkas) externalSession
						.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
						.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa", v))
						.add(Restrictions.eq("calonSiswa", calonSiswa))
						.setMaxResults(1)
						.uniqueResult();

				if (cpvb == null) {
					cpvb = new CalonSiswaPunyaVerifikasiBerkas();
					cpvb.setCalonSiswa(calonSiswa);
					cpvb.setVerifikasiKelengkapanCalonSiswa(v);
					Common.refreshSaveOrUpdate(externalSession, cpvb);
				}

				FileFotoLain fileFotoLain = cpvb.getId() == null ? null
						: FileFotoLain.ambil(false, cpvb.getId(),
								CalonSiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class);
				if (fileFotoLain == null) {
					String nama = v.getNama();
					return "Maaf, Anda belum dapat mengikuti Wawancara karena berkas \""
							+ nama + "\" belum diunggah.\n\n"
							+ "Langkah yang perlu dilakukan:\n"
							+ "1. Tutup jendela ini.\n"
							+ "2. Buka menu \"Kelengkapan Berkas\" di halaman utama PPDB.\n"
							+ "3. Cari berkas \"" + nama + "\", lalu klik tombol Unggah.\n"
							+ "4. Pilih file dari perangkat Anda (format: PDF, JPG, atau PNG).\n"
							+ "5. Setelah berhasil diunggah, kembali ke halaman ini dan klik \"Wawancara\" kembali.\n\n"
							+ "Jika mengalami kendala:\n"
							+ "- Pastikan format dan ukuran file sesuai ketentuan panitia.\n"
							+ "- Coba gunakan browser lain (Chrome / Firefox) jika proses unggah gagal.\n"
							+ "- Hubungi panitia penerimaan siswa baru untuk mendapatkan bantuan lebih lanjut.";
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/VerifikasiKelengkapanCalonSiswa.java:462");
		}
		return null;
	}
}
