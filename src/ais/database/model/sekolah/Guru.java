package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import org.hibernate.envers.NotAudited;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Label;
import org.zkoss.zul.Toolbarbutton;

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Agama;
import ais.database.model.GeneralValueObject;
import ais.database.model.GolonganPns;
import ais.database.model.JenisPendidikDanTenagaKependidikan;
import ais.database.model.LembagaPengangkat;
import ais.database.model.Negara;
import ais.database.model.Pegawai;
import ais.database.model.PekerjaanOrangTua;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusPegawai;
import ais.database.model.SumberGaji;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswaDosen;
import ais.database.model.Wilayah;
import ais.database.model.employ.Pendidikan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;

/**
 * Entity <b>identitas guru / tenaga kependidikan</b> pada instalasi sekolah — baris master tabel
 * {@code sekolah.guru}.
 *
 * <h2>Peran</h2>
 * <p>Kelas ini adalah <b>subjek pusat seluruh domain guru</b> di AIS: hampir setiap entity lain di
 * paket {@code ais.database.model.sekolah} yang berhubungan dengan tenaga pendidik menunjuk ke sini
 * lewat FK {@code guru_id}. Sekitar <b>184 berkas Java</b> mengimpor kelas ini secara langsung,
 * belum termasuk pemakaian lewat {@link Tbmuser#getGuru()}. Layar utamanya adalah
 * <i>Manajemen Guru</i> ({@code /pages/master/sekolah/guru.zul}, dikendalikan
 * {@code ais.action.master.sekolah.GuruAction}).</p>
 *
 * <p>Baris entity ini merangkap <b>tiga peran sekaligus</b> yang sering tertukar saat membaca kode:</p>
 * <ol>
 *   <li><b>Biodata kepegawaian</b> — sumber data Dapodik/laporan (NUPTK, NIP, golongan, TMT, dsb).</li>
 *   <li><b>Identitas akun</b> — {@link Tbmuser#getGuru()} menunjuk ke sini; itulah yang membuat
 *   sebuah akun login "menjadi guru" dan menentukan cakupan sekolahnya
 *   (lihat {@link #ambilSekolahs()}).</li>
 *   <li><b>Pihak yang ditugaskan</b> — pemilik jadwal mengajar, wali kelas, guru BK, pembina, penilai.</li>
 * </ol>
 *
 * <h2>Posisi dalam domain guru</h2>
 * <p>Entity tetangga yang sudah didokumentasikan dan bermuara ke kelas ini:</p>
 * <ul>
 *   <li>{@link JenisGuru} — klasifikasi guru (referensi {@link #getJenisGuru()}).</li>
 *   <li>{@link JenisSKGuru} dan {@link PenugasanGuruMengajar} — kepala SK penugasan mengajar beserta
 *   penomorannya.</li>
 *   <li>{@link GuruMengajar} — baris penugasan mengajar per kelas/mata pelajaran.</li>
 *   <li>{@link CatatanGuru} — catatan pembinaan/kinerja guru (padanan {@code CatatanSiswa}).</li>
 *   <li>{@link PrestasiGuru}, {@link KategoriPrestasiGuru}, {@link CabangPrestasiGuru} — rantai
 *   personalia prestasi guru.</li>
 *   <li>{@link Sekolah} dan {@link Yayasan} — pembatas cakupan (tenant) penempatan.</li>
 *   <li>{@link Pegawai} — sisi kepegawaian umum; satu orang bisa punya baris {@code Guru} DAN
 *   {@code Pegawai} yang disinkronkan lewat tombol "Singkronkan dg pegawai" di {@code GuruAction}.</li>
 * </ul>
 *
 * <p>Padanan entity ini di sisi peserta didik adalah {@link Siswa}: struktur biodata, pola getter
 * tulis-balik, dan pola broken access control di layarnya sangat mirip, sehingga temuan pada satu
 * sisi hampir selalu perlu diperiksa ulang di sisi lain.</p>
 *
 * <h2>Pemetaan Hibernate — hal yang WAJIB diketahui sebelum mengubah apa pun</h2>
 * <ul>
 *   <li><b>{@link GeneralValueObject} BUKAN {@code @Entity} maupun {@code @MappedSuperclass}</b> —
 *   ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti milik induk.
 *   Karena itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sengaja
 *   <b>dideklarasikan ULANG</b> di kelas ini. Itu <b>bukan duplikasi yang keliru, melainkan
 *   keharusan teknis</b>: menghapusnya membuat kolom-kolom tersebut hilang dari pemetaan.</li>
 *   <li><b>Akses properti (property access), bukan field access</b> — karena {@link Id} dipasang
 *   pada {@link #getId()}, Hibernate membaca SELURUH nilai lewat <i>getter</i>. Konsekuensinya
 *   setiap getter yang mengubah state object ikut menentukan apa yang tertulis ke database pada
 *   {@code flush} berikutnya. Lihat bagian "Getter yang menulis balik" di bawah.</li>
 *   <li><b>Tidak ada {@code @Transient} sama sekali di kelas ini</b> — jadi setiap getter berpola
 *   JavaBean di sini adalah kolom sungguhan. Method bisnis sengaja diberi nama non-getter
 *   ({@link #ambilSekolahs()}, {@link #ambilKode()}, {@link #ambilMateri}, {@link #ttdQr()},
 *   {@link #putPhoto(Map)}, {@link #tampilkanEmail(Component)}, {@link #tampilkanHp(Component)})
 *   supaya tidak ikut dipetakan.</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate}</b> aktif: Hibernate hanya menyertakan kolom
 *   yang benar-benar berubah pada {@code INSERT}/{@code UPDATE}. Ini <b>tidak</b> melindungi dari
 *   getter tulis-balik — kolom yang nilainya berubah karena getter tetap ikut ter-{@code UPDATE}.</li>
 *   <li><b>{@code @Audited} (Envers)</b> — seluruh perubahan direkam ke tabel audit, kecuali
 *   {@link #getBahasa()} yang ditandai {@link NotAudited}.</li>
 *   <li><b>Dua properti dipetakan ke kolom yang SAMA</b>: {@link #getNama()} dan
 *   {@link #getNamaGuru()} keduanya menunjuk {@code nama_guru}. Yang pertama dibuat
 *   {@code insertable=false, updatable=false} sehingga hanya-baca; itulah yang membuat pemetaan
 *   ganda ini legal. Alasannya kontrak {@link VOMahasiswaDosen#getNama()} dan agar kolom grid bisa
 *   disortir dengan {@code sort="auto(nama)"}.</li>
 *   <li><b>{@link #getPegawaiId()} bukan pasangan {@link #getPegawai()}</b> — {@code pegawaiId}
 *   dipetakan ke kolom skalar {@code pegawai_id}, sedangkan relasi {@code Pegawai} memakai
 *   {@link JoinColumn} bernama {@code pegawai}. Keduanya kolom BERBEDA dan tidak disinkronkan oleh
 *   kode mana pun di kelas ini.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Identitas &amp; kunci</b> — {@link #getId()}, {@link #getKode()}, {@link #getNuptk()},
 *   {@link #getNip()}, {@link #getNik()}, {@link #getKk()}, {@link #getKarpeg()},
 *   {@link #getKarisKarsu()}, {@link #getNuks()}, {@link #getIdfinger()}.</li>
 *   <li><b>Biodata pribadi</b> — {@link #getNamaGuru()}, {@link #getNama()}, {@link #getNamaAr()},
 *   {@link #getNamaCh()}, {@link #getPanggilan()}, {@link #getGelarDepan()},
 *   {@link #getGelarBelakang()}, {@link #getJenisKelamin()}, {@link #getTempatLahir()},
 *   {@link #getTanggalLahir()}, {@link #getAgama()}, {@link #getKewarganegaraan()},
 *   {@link #getNegara()}, {@link #getStatusNikah()}, {@link #getBahasa()}.</li>
 *   <li><b>Keluarga</b> — {@link #getNamaAyah()}/{@link #getPekerjaanAyah()},
 *   {@link #getNamaIbu()}/{@link #getPekerjaanIbu()}, {@link #getNamaSuamiIstri()},
 *   {@link #getNipSuamiIstri()}, {@link #getPekerjaanSuamiIstri()}.</li>
 *   <li><b>Kontak &amp; alamat</b> — {@link #getAlamatGuru()}, {@link #getRt()}, {@link #getRw()},
 *   {@link #getDusun()}, {@link #getKelurahan()}, {@link #getKecamatan()}, {@link #getKodePos()},
 *   {@link #getTeleponGuru()}, {@link #getHp()}, {@link #getAlamatEmail()}, {@link #getLintang()},
 *   {@link #getBujur()}.</li>
 *   <li><b>Penempatan &amp; cakupan (tenant)</b> — {@link #getSekolah()}, {@link #getSekolah1()},
 *   {@link #getSekolah2()}, {@link #getSekolah3()}, {@link #getYayasan()},
 *   {@link #getMilikUniversitas()}, {@link #getJenisGuru()}, {@link #getAktif()},
 *   {@link #ambilSekolahs()}.</li>
 *   <li><b>Kepegawaian</b> — {@link #getStatusPegawai()}, {@link #getStatusKepegawaian()},
 *   {@link #getJenisPendidikDanTenagaKependidikan()}, {@link #getSumberGaji()},
 *   {@link #getSkCpns()}, {@link #getTglSkCpns()}, {@link #getSkAngkat()},
 *   {@link #getTmtSkAngkat()}, {@link #getTmtKerja()}, {@link #getTmtPns()},
 *   {@link #getLembagaPengangkat()}, {@link #getGolonganPegawai()}, {@link #getPegawai()},
 *   {@link #getPegawaiId()}.</li>
 *   <li><b>Akademik &amp; beban kerja</b> — {@link #getPendidikan()}, {@link #getJurusan()},
 *   {@link #getSertifikasi()}, {@link #getKompetensi()}, {@link #getMengajar()},
 *   {@link #getTugasTambahan()}, {@link #getJamTugasTambahan()}, {@link #getJjm()},
 *   {@link #getTotalJjm()}, {@link #getSiswa()}, {@link #getKeterangan()}.</li>
 *   <li><b>Keuangan &amp; pajak</b> — {@link #getNpwp()}, {@link #getNamaWajibPajak()},
 *   {@link #getBank()}, {@link #getNomorRekeningBank()}, {@link #getRekeningAtasNama()}.</li>
 *   <li><b>Kualifikasi khusus</b> — {@link #getSudahLisensiKepalaSekolah()},
 *   {@link #getPernahDiklatKepengawasan()}, {@link #getKeahlianBraille()},
 *   {@link #getKeahlianBahasaIsyarat()}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Method bisnis / presentasi</b> — {@link #tampilkanEmail(Component)},
 *   {@link #tampilkanHp(Component)}, {@link #ambilKode()}, {@link #ambilMateri},
 *   {@link #ttdQr()}, {@link #putPhoto(Map)}.</li>
 * </ol>
 *
 * <h2>Getter yang menulis balik (write-back)</h2>
 * <p>Karena Hibernate memakai akses properti, <b>membaca baris bisa berarti mengubahnya</b>. Semua
 * getter berikut mengembalikan nilai yang berbeda dari nilai kolom di database bila kolomnya
 * kosong/tidak baku, sehingga <i>dirty checking</i> menuliskannya secara permanen pada flush
 * berikutnya — tanpa pengguna menekan tombol Simpan apa pun:</p>
 * <ul>
 *   <li>{@link #getJenisKelamin()} — membakukan "L"/"P"/"Laki-Laki" menjadi
 *   "Laki-laki"/"Perempuan", dan {@code null} menjadi string kosong (assignment eksplisit ke field).</li>
 *   <li>{@link #getAlamatEmail()} — merapatkan {@code ",,"} menjadi {@code ","} (5 kali),
 *   {@code null} dan {@code ","} tunggal menjadi string kosong (assignment eksplisit ke field).</li>
 *   <li>{@link #getStatusNikah()} — mengisi "Belum Kawin" bila kolom {@code null}
 *   (assignment eksplisit ke field).</li>
 *   <li>{@link #getNegara()} — mengembalikan {@link ConstantValues#INDONESIA} bila {@code null}.</li>
 *   <li>{@link #getBahasa()} — mengembalikan {@link Tbmuser#INDONESIA} bila kosong.</li>
 *   <li>{@link #getAktif()} — mengembalikan {@code true} bila {@code null}.</li>
 *   <li>{@link #getMilikUniversitas()}, {@link #getSudahLisensiKepalaSekolah()},
 *   {@link #getPernahDiklatKepengawasan()}, {@link #getKeahlianBraille()},
 *   {@link #getKeahlianBahasaIsyarat()} — mengembalikan {@code false} bila {@code null}
 *   (kolom tri-state "belum diisi" berubah permanen menjadi "tidak").</li>
 *   <li>{@link #getHp()} — membuang seluruh karakter selain digit dan titik; format asli
 *   ("+62 812-3456", "0812 3456") hilang permanen begitu baris tersentuh.</li>
 *   <li>{@link #getNamaGuru()} — memangkas spasi tepi ({@code trim}) dan mengubah {@code null}
 *   menjadi string kosong.</li>
 *   <li>{@link #getNamaWajibPajak()}, {@link #getNamaAr()}, {@link #getNamaCh()} — mengisi kolomnya
 *   dengan nama guru bila kosong. Efek lanjutannya: kolom itu tidak lagi kosong, sehingga saat nama
 *   guru diubah kemudian, nilai lama tertinggal sebagai data usang (dan
 *   {@code GuruAction.GuruRenderer} berhenti menganggapnya "sama dengan nama").</li>
 *   <li>{@link #getIdfinger()} — mengembalikan nilai dari penyimpanan kunci-nilai
 *   {@link GeneralValueObject#retreive(String)} bila kolomnya kosong, sehingga nilai dari luar
 *   database ikut tersalin ke kolom.</li>
 *   <li>Seluruh getter relasi yang memanggil {@link GeneralValueObject#check(Object)} menugaskan
 *   ulang hasilnya ke field. Itu memang pola wajib AIS (menghindari
 *   {@code LazyInitializationException}) dan biasanya tidak mengubah nilai kolom, karena instance
 *   pengganti tetap ber-ID sama.</li>
 * </ul>
 *
 * <h2>Getter DESTRUKTIF (menghapus/menimpa data yang sudah diisi manusia)</h2>
 * <p>Dua getter berikut bukan sekadar membakukan nilai kosong, melainkan <b>menimpa data yang sudah
 * terisi</b>. Pemeriksaan pola {@code Siswa.java} (yang menemukan 9 getter destruktif, termasuk yang
 * menghapus alamat/telepon orang tua secara permanen) memberi hasil <b>jauh lebih ringan di sini —
 * 2 instance, bukan 9</b>:</p>
 * <ol>
 *   <li><b>{@link #getYayasan()}</b> — <i>setiap</i> pemanggilan menimpa field {@code yayasan}
 *   dengan {@code getSekolah().getYayasan()} bila {@link #getSekolah()} tidak {@code null}. Dua
 *   akibatnya: (a) yayasan yang sengaja diisi berbeda oleh admin (mis. guru diperbantukan) tidak
 *   pernah bertahan; (b) bila sekolahnya sendiri belum punya yayasan, kolom {@code yayasan_id} guru
 *   <b>dikosongkan permanen</b>. Getter ini dipanggil pada SETIAP baris grid Manajemen Guru
 *   ({@code GuruRenderer} menampilkan nama yayasan), jadi kerusakan terjadi sekadar karena daftar
 *   guru dibuka — pola yang persis sama dengan getter destruktif {@code Siswa.java} (b66).</li>
 *   <li><b>{@link #getKecamatan()}</b> — bila {@link Wilayah} yang tersimpan tidak punya wilayah
 *   induk, getter ini menyapu seluruh cache {@link Wilayah} dan <b>mengganti kecamatan guru dengan
 *   baris wilayah LAIN</b> yang kebetulan berkode {@code feeder} sama namun punya induk. Bila ada
 *   lebih dari satu kandidat, yang terpilih bergantung urutan iterasi cache ({@code break} pada
 *   kecocokan pertama) — jadi hasilnya tidak deterministik antar-instalasi.</li>
 * </ol>
 *
 * <h2>Kredensial: TIDAK disimpan di entity ini</h2>
 * <p><b>Verifikasi eksplisit</b> terhadap pola {@code Siswa.getPass()} (b66, password bawaan = NISN
 * yang dienkripsi DES): entity {@code Guru} <b>tidak punya kolom password sama sekali</b> — tidak
 * ada {@code pass}, {@code pin}, maupun {@code passOrtu}. Kredensial guru sepenuhnya berada di
 * {@link Tbmuser} ({@code userPassword}) yang berelasi ke sini lewat {@code Tbmuser.guru}.</p>
 * <p>Namun itu <b>bukan berarti lebih aman</b>: {@code Tbmuser.userPassword} dienkripsi
 * {@code Common.desEncrypter} — DES dengan passphrase global {@code Common.DES_PASS_PHRASE} yang
 * tertanam di kode sumber dan sama untuk semua instalasi — jadi bersifat <b>reversibel</b>, dan
 * layar guru memang memanfaatkannya untuk mengekspor password apa adanya (lihat catatan keamanan
 * di bawah). Password yang dibangkitkan {@code GuruAction} hanya {@code randomNumeric(5)}
 * (100.000 kemungkinan).</p>
 *
 * <h2>Data sensitif yang tersimpan LANGSUNG di entity ini</h2>
 * <p>Satu baris {@code Guru} memuat, dalam satu tabel: <b>NIK</b> ({@link #getNik()}),
 * <b>nomor Kartu Keluarga</b> ({@link #getKk()}), <b>NPWP</b> ({@link #getNpwp()}),
 * <b>nomor rekening bank + nama pemilik rekening</b> ({@link #getNomorRekeningBank()},
 * {@link #getRekeningAtasNama()}), nomor Karpeg dan Karis/Karsu, alamat rumah lengkap sampai
 * RT/RW/dusun, nomor telepon dan HP pribadi, alamat email, nama ayah/ibu/pasangan berikut NIP
 * pasangan, serta <b>koordinat GPS rumah</b> ({@link #getLintang()}, {@link #getBujur()} — label UI
 * "Koordinat Lintang/Bujur Google Map"). Seluruh kolom itu termasuk dalam larik
 * {@code GuruAction.DATA} sehingga ikut terekspor mentah oleh tombol unduh Excel.</p>
 *
 * <h2>Catatan keamanan pada layar pemakainya</h2>
 * <p>Ringkasan hasil audit {@code ais.action.master.sekolah.GuruAction} (3.398 baris) dan
 * {@code guru.zul}. <b>Ini catatan, bukan instruksi perubahan</b> — kelas entity ini sendiri hanya
 * didokumentasikan, tidak diubah.</p>
 * <ul>
 *   <li><b>Tombol "Password Guru" tanpa gerbang hak sama sekali.</b> Di {@code doAfterCompose()},
 *   tombol {@code add} digerbangi {@code CommonPrivilages.CREATE}, tombol unggah Excel digerbangi
 *   {@code add+edit+delete}, dan tombol "Singkronkan dg pegawai" digerbangi {@code add+edit} —
 *   tetapi tombol <i>Password Guru</i> hanya dipasang lewat {@code Common.appendKeToolbar(...)}
 *   <b>tanpa satu pun {@code setVisible(...)}</b>. Padahal aksinya: (a) MEMBUAT akun
 *   {@link Tbmuser} baru berperan {@code roleGuru} untuk setiap guru yang belum punya akun, dan
 *   (b) mengekspor Excel berisi <b>username + password hasil DEKRIPSI</b> ({@code desEncrypter
 *   .decrypt(...)}) + nama + email + nomor HP seluruh guru. Artinya hak <b>BACA</b> pada menu Guru
 *   sudah cukup untuk memanen kredensial seluruh guru.</li>
 *   <li><b>Fail-open cakupan tenant.</b> {@code GuruAction.initCriteria()} hanya menambahkan
 *   {@code isNotNull("sekolah")} plus filter yang dipilih pengguna; <b>tidak ada</b> pembatas
 *   yayasan/sekolah bawaan dari sesi. Karena tombol "Password Guru" dan tombol unduh Excel memanggil
 *   {@code initCriteria(true)} tanpa memilih filter apa pun, jangkauannya adalah <b>SELURUH guru di
 *   seluruh yayasan pada instalasi</b>, bukan hanya tenant pengguna.</li>
 *   <li><b>Berkas ekspor ditulis ke dalam webapp.</b> Berkas Excel password disimpan ke
 *   {@code getWebApp().getRealPath("/tmp/user_password_guru_<timestamp>.xlsx")} — direktori di dalam
 *   webapp, pola yang sama dengan {@code task_a1e32ff3}; nama berkasnya hanya berbasis stempel waktu
 *   dan berkasnya tidak pernah dihapus. Ekspor {@code idfinger} juga memakai {@code /tmp/} yang sama.</li>
 *   <li><b>Data dikirim ke pihak ketiga.</b> Alur yang sama menembak {@code curl} ke URL
 *   konfigurasi {@code ambil_kode_url} (bawaan {@code https://dev.ecampus.id/ecampus/Api}) berisi
 *   username guru + alamat host instalasi + identitas perguruan tinggi.</li>
 *   <li><b>Unggahan Excel menyertakan kolom {@code "id"}.</b> {@code GuruAction.DATA} diawali
 *   {@code "id"}, jadi berkas unggahan bisa menargetkan baris guru mana pun berdasarkan ID —
 *   pola yang sama dengan {@code JenisItemPenilaianSiswa} (b64). Berbeda dari kasus b64, tombol
 *   unggahnya <b>di sini memang digerbangi</b> {@code add+edit+delete}.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> {@code guru.zul} menyisipkan sebagai tab:
 *   {@code penugasan_guru_mengajar.zul}, {@code jenis_sk_guru.zul}, laporan {@code LaporanSKGuru},
 *   ditambah <b>sepuluh</b> layar master lain lewat {@code onKonfigurasiBiodataGuru()}
 *   ({@code konfigurasi_guru}, {@code ikatan_kerja_dosen}, {@code status_kepegawaian},
 *   {@code jenis_pendidik_dan_tenaga_kependidikan}, {@code lembaga_pengangkat}, {@code sumber_gaji},
 *   {@code status_pegawai}, {@code jabatan}, {@code jenis_tenaga_kependidikan},
 *   {@code golongan_pns}). Baris grid juga menyisipkan {@code siswa_id.zul?guruPembina=}/{@code ?guruBk=}
 *   yang menampilkan daftar siswa. Instance lain dari pola kumulatif yang sudah tercatat.</li>
 *   <li><b>Yang sudah benar</b> (jangan dirusak saat memperbaiki): {@code doBeforeCompose()}
 *   memanggil {@code Common.doCheckSecurity()}; checkbox "Aktif" pada grid dinonaktifkan bila
 *   {@code edit} bernilai {@code false}; tombol RFID tambahan bahkan menuntut peran
 *   {@code ADMINISTRATOR}.</li>
 * </ul>
 *
 * <h2>Kuirk lain</h2>
 * <ul>
 *   <li>Field {@link #getMilikUniversitas()} berlabel <b>"Milik Yayasan"</b> di UI — nama field dan
 *   labelnya tidak sejalan (peninggalan basis kode perguruan tinggi).</li>
 *   <li>Kolom {@link #getSiswa()} adalah <b>teks bebas</b> pada biodata guru, sama sekali bukan
 *   relasi ke entity {@link Siswa}. Mudah tertukar saat membaca kode.</li>
 *   <li>Visibilitas, kewajiban isi, dan sifat hanya-baca setiap field biodata dikendalikan
 *   {@code KonfigurasiTampilanGuruAction.statusWajibIsi(key)} lewat
 *   {@code Common.checkApakahLabelGuruTampil(...)} — jadi field yang "hilang" dari form belum tentu
 *   tidak ada di database.</li>
 *   <li>{@link #getIdfinger()}/{@link #setIdfinger(String)} memakai penyimpanan kunci-nilai
 *   berbasis berkas milik {@link GeneralValueObject} ({@code retreive}/{@code put}) DI SAMPING kolom
 *   database biasa. Javadoc {@code GeneralValueObject.retreive(String)} mencatat bahwa pola ini
 *   pernah memicu I/O berkas pada setiap auto-flush; kini diredam cache per-instance.</li>
 *   <li>{@link #setOleh(String)} dan {@link #setOlehId(String)} <b>menolak diam-diam</b> nilai
 *   {@code null}/kosong — jejak "diubah oleh" tidak pernah bisa dikosongkan kembali.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see VOMahasiswaDosen
 * @see Siswa
 * @see JenisGuru
 * @see GuruMengajar
 * @see PenugasanGuruMengajar
 * @see CatatanGuru
 * @see PrestasiGuru
 * @see Tbmuser
 * @see Pegawai
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "guru", schema = "sekolah")
public class Guru extends GeneralValueObject implements VOMahasiswaDosen {

	/**
	 * Versi serialisasi. Entity ini ikut terserialisasi ke sesi ZK dan ke cache, jadi nilainya
	 * tidak boleh diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2820445053149767393L;
	/**
	 * Kunci utama {@code sekolah.guru.id}. Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} bukan kelas terpetakan.
	 */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit ringan, bukan FK). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (jejak audit ringan, bukan FK). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau kosong <b>diabaikan diam-diam</b> (method
	 * langsung {@code return} tanpa mengubah apa pun), sehingga jejak audit yang sudah terisi tidak
	 * bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId ID pengguna pengubah; nilai {@code null}/kosong tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong
	 * diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna pengubah; nilai {@code null}/kosong tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang menyerahkan pencatatan stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} sesaat sebelum baris
	 * ini ter-{@code UPDATE}.
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field {@code tanggal_dirubah} beserta
	 * nilai awalnya ({@code ais.ui.util.WaktuUtil.getDate()}) — penulisan satu baris ini sengaja
	 * dipertahankan apa adanya. Sama seperti {@code id}/{@code oleh}/{@code olehId}, field tersebut
	 * harus dideklarasikan ulang di sini karena {@link GeneralValueObject} tidak dipetakan
	 * Hibernate.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Biasanya diisi otomatis lewat {@link #onUpdate()}; setter ini dipakai jalur impor/migrasi
	 * yang ingin mempertahankan stempel waktu asal.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir (tidak pernah {@code null} untuk object baru, karena
	 *         field-nya diinisialisasi saat konstruksi)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Klasifikasi guru (FK {@code jenis_guru_id}); lihat {@link JenisGuru}. */
	private JenisGuru jenisGuru;
	/** Status pegawai (FK {@code status_pegawai}), mis. Aktif/Pensiun/Mutasi. */
	private StatusPegawai statusPegawai;
	/** Penanda "Milik Yayasan" pada UI, meski nama fieldnya menyebut universitas. */
	private Boolean milikUniversitas;
	/** Sekolah penempatan utama (FK {@code sekolah_id}) — pembatas tenant utama entity ini. */
	private Sekolah sekolah;
	/** Yayasan penaung; <b>selalu ditimpa</b> dari {@link #sekolah} oleh {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Sekolah tambahan I tempat guru juga mengajar (label UI "Juga mengajar di (I)"). */
	private Sekolah sekolah1;
	/** Sekolah tambahan II tempat guru juga mengajar (label UI "Juga mengajar di (II)"). */
	private Sekolah sekolah2;
	/** Sekolah tambahan III tempat guru juga mengajar (label UI "Juga mengajar di (III)"). */
	private Sekolah sekolah3;
	/** Kode mesin sidik jari / kartu RFID; disimpan ganda di kolom DB dan di penyimpanan kunci-nilai. */
	private String idfinger;

	/** Agama (FK {@code agama_id}). */
	private Agama agama;
	/** Alamat email guru; dibersihkan dari koma ganda oleh {@link #getAlamatEmail()}. */
	private String alamatEmail;
	/** Alamat jalan tempat tinggal (kolom {@code alamat_guru}, panjang 2000). */
	private String alamatGuru;
	/** Jenis kelamin; dibakukan menjadi "Laki-laki"/"Perempuan" oleh {@link #getJenisKelamin()}. */
	private String jenisKelamin;
	/** Nama lengkap guru (kolom {@code nama_guru}, wajib isi). */
	private String namaGuru;
	/** Salinan hanya-baca {@link #namaGuru} untuk kontrak {@link VOMahasiswaDosen#getNama()}. */
	private String nama;
	/** Kolom skalar {@code pegawai_id}; <b>bukan</b> FK relasi {@link #pegawai}. */
	private Long pegawaiId;

	/** Nama panggilan. */
	private String panggilan;
	/** Kewarganegaraan sebagai teks bebas (terpisah dari relasi {@link #negara}). */
	private String kewarganegaraan;
	/** Negara asal (FK {@code negara_id}); default {@link ConstantValues#INDONESIA} bila kosong. */
	private Negara negara;
	/** Tanggal lahir. */
	private Date tanggalLahir;
	/** Nomor telepon (kolom {@code telepon_guru}); dipakai {@link #tampilkanHp(Component)}. */
	private String teleponGuru;
	/** Tempat lahir. */
	private String tempatLahir;
	/** Keterangan sertifikasi pendidik (teks panjang). */
	private String sertifikasi;
	/** NIP (untuk guru berstatus PNS). */
	private String nip;
	/** <b>Data sensitif</b> — Nomor Induk Kependudukan. */
	private String nik;
	/** <b>Data sensitif</b> — nomor Kartu Keluarga. */
	private String kk;
	/** <b>Data sensitif</b> — NPWP. */
	private String npwp;

	/** Jumlah Jam Mengajar (JJM) sebagai teks. */
	private String jjm;
	/** Total Jumlah Jam Mengajar sebagai teks. */
	private String totalJjm;
	/** Teks bebas "Siswa" pada biodata — <b>bukan</b> relasi ke entity {@link Siswa}. */
	private String siswa;
	/** Kompetensi guru (teks panjang). */
	private String kompetensi;
	/** Nama wajib pajak; diisi dari nama guru bila kosong ({@link #getNamaWajibPajak()}). */
	private String namaWajibPajak;
	/** <b>Data sensitif</b> — nomor Kartu Pegawai. */
	private String karpeg;
	/** <b>Data sensitif</b> — nomor Karis/Karsu (kartu istri/suami). */
	private String karisKarsu;
	/** Nomor Unik Kepala Sekolah. */
	private String nuks;
	/** Penanda sudah memiliki lisensi kepala sekolah. */
	private Boolean sudahLisensiKepalaSekolah;
	/** Penanda pernah mengikuti diklat kepengawasan. */
	private Boolean pernahDiklatKepengawasan;
	/** Penanda menguasai huruf Braille. */
	private Boolean keahlianBraille;
	/** Penanda menguasai bahasa isyarat. */
	private Boolean keahlianBahasaIsyarat;

	/**
	 * <b>Data sensitif finansial</b> — nama bank, nomor rekening, dan nama pemilik rekening
	 * penerima gaji/honor. Ketiganya dideklarasikan pada satu baris.
	 */
	private String bank, nomorRekeningBank, rekeningAtasNama;
	/** Kode guru internal instalasi; juga dipakai sebagai calon username akun guru. */
	private String kode;
	/** NUPTK (Nomor Unik Pendidik dan Tenaga Kependidikan). */
	private String nuptk;
	/** Penanda guru masih aktif; {@code null} diperlakukan sebagai {@code true}. */
	private Boolean aktif;
	/** <b>Data sensitif</b> — koordinat lintang rumah guru (label UI "Koordinat Lintang Google Map"). */
	private String lintang;
	/** <b>Data sensitif</b> — koordinat bujur rumah guru (label UI "Koordinat Bujur Google Map"). */
	private String bujur;
	/** Relasi ke data kepegawaian umum (FK berkolom {@code pegawai}). */
	private Pegawai pegawai;
	/** Bahasa antarmuka pilihan guru; satu-satunya properti {@link NotAudited} di kelas ini. */
	private String bahasa;
	/** Status kepegawaian (FK {@code status_kepegawaian}), mis. PNS/GTY/Honorer. */
	private StatusKepegawaian statusKepegawaian;
	/** Jenis PTK — pendidik atau tenaga kependidikan (FK {@code jenis_pendidik_dan_tenaga_kependidikan}). */
	private JenisPendidikDanTenagaKependidikan jenisPendidikDanTenagaKependidikan;
	/** Kode pos alamat tempat tinggal. */
	private String kodePos;
	/** Kecamatan tempat tinggal (FK {@code kecamatan}); dapat <b>ditimpa</b> oleh {@link #getKecamatan()}. */
	private Wilayah kecamatan;
	/** Kelurahan/desa tempat tinggal (teks bebas). */
	private String kelurahan;
	/** Nama dusun tempat tinggal. */
	private String dusun;
	/** RW tempat tinggal. */
	private String rw;
	/** RT tempat tinggal. */
	private String rt;
	/** Sumber gaji (FK {@code sumber_gaji}), mis. APBN/APBD/Yayasan. */
	private SumberGaji sumberGaji;
	/** Nomor SK CPNS. */
	private String skCpns;
	/** Tanggal SK CPNS. */
	private Date tglSkCpns;
	/** Nomor SK pengangkatan. */
	private String skAngkat;
	/** TMT (terhitung mulai tanggal) SK pengangkatan. */
	private Date tmtSkAngkat;
	/** TMT mulai bekerja di lembaga ini. */
	private Date tmtKerja;
	/** Lembaga yang mengangkat (FK {@code lembaga_pengangkat}). */
	private LembagaPengangkat lembagaPengangkat;
	/** Pangkat/golongan PNS (FK {@code golongan_pns}). */
	private GolonganPns golonganPegawai;
	/** TMT status PNS. */
	private Date tmtPns;
	/** <b>Data pribadi pihak ketiga</b> — nama ayah kandung (kolom {@code nama_ayah}, panjang 100). */
	private String namaAyah;
	/** Pekerjaan ayah (FK {@code id_pekerjaan_ayah}). */
	private PekerjaanOrangTua pekerjaanAyah;
	/** <b>Data pribadi pihak ketiga</b> — nama ibu kandung (kolom {@code nama_ibu}, panjang 100). */
	private String namaIbu;
	/** Pekerjaan ibu (FK {@code id_pekerjaan_ibu}). */
	private PekerjaanOrangTua pekerjaanIbu;
	/** Status perkawinan (kolom {@code status_nikah_s}); default "Belum Kawin" bila kosong. */
	private String statusNikah;
	/** <b>Data pribadi pihak ketiga</b> — nama suami/istri. */
	private String namaSuamiIstri;
	/** <b>Data pribadi pihak ketiga</b> — NIP suami/istri. */
	private String nipSuamiIstri;
	/** Pekerjaan suami/istri (FK {@code pekerjaan_suami_istri}). */
	private PekerjaanOrangTua pekerjaanSuamiIstri;
	/** Gelar akademik di depan nama. */
	private String gelarDepan;
	/** Gelar akademik di belakang nama. */
	private String gelarBelakang;
	/** Pendidikan terakhir (FK {@code pendidikan}). */
	private Pendidikan pendidikan;
	/** Jurusan/program studi pendidikan terakhir. */
	private String jurusan;
	/** Uraian tugas tambahan (teks panjang). */
	private String tugasTambahan;
	/** Uraian mata pelajaran/kelas yang diampu (teks panjang, terpisah dari {@link GuruMengajar}). */
	private String mengajar;
	/** Uraian jam untuk tugas tambahan (teks panjang). */
	private String jamTugasTambahan;
	/** Keterangan bebas; ditampilkan sebagai kolom tersendiri di grid Manajemen Guru. */
	private String keterangan;
	/** Nomor HP; {@link #getHp()} membuang seluruh karakter non-digit sebelum mengembalikannya. */
	private String hp;
	/** Nama dalam aksara Arab; default mengikuti {@link #namaGuru} bila kosong. */
	private String namaAr;
	/** Nama dalam aksara Tionghoa; default mengikuti {@link #namaGuru} bila kosong. */
	private String namaCh;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate dan jalur pembuatan baris baru pada UI.
	 */
	public Guru() {
	}

	/**
	 * Konstruktor pintasan untuk membentuk referensi ber-ID saja (mis. sebagai parameter kriteria),
	 * tanpa memuat baris dari database.
	 *
	 * @param id kunci utama guru
	 */
	public Guru(long id) {
		this.id = id;
	}

	/**
	 * Konstruktor dengan seluruh kolom yang berstatus wajib pada pemetaan
	 * ({@code id}, {@code jenis_kelamin}, {@code nama_guru}).
	 *
	 * @param id           kunci utama guru
	 * @param jenisKelamin jenis kelamin apa adanya (belum dibakukan; pembakuan terjadi di
	 *                     {@link #getJenisKelamin()})
	 * @param namaGuru     nama lengkap guru
	 */
	public Guru(long id, String jenisKelamin, String namaGuru) {
		this.id = id;
		this.jenisKelamin = jenisKelamin;
		this.namaGuru = namaGuru;
	}

	/**
	 * Mengembalikan kunci utama guru.
	 *
	 * <p>Anotasi {@link Id} pada getter inilah yang membuat SELURUH kelas ini memakai
	 * <b>akses properti</b> — Hibernate membaca setiap nilai lewat getter, termasuk saat
	 * <i>dirty checking</i>. Itu dasar dari seluruh perilaku tulis-balik yang diuraikan pada
	 * Javadoc kelas.</p>
	 *
	 * <p>{@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@link javax.persistence.GenerationType#IDENTITY}).</p>
	 *
	 * @return kunci utama, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Hanya dipakai jalur impor/migrasi dan pembentukan referensi; pada alur normal nilai ini
	 * diisi Hibernate.</p>
	 *
	 * @param id kunci utama guru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Implementasi kontrak {@link VOMahasiswaDosen#getNama()} — mengembalikan nama guru.
	 *
	 * <p><b>Pemetaan ganda:</b> properti ini menunjuk kolom {@code nama_guru} yang sama dengan
	 * {@link #getNamaGuru()}, tetapi dibuat {@code insertable = false, updatable = false} sehingga
	 * bersifat hanya-baca. Itulah yang membuat dua properti ke satu kolom bisa diterima Hibernate,
	 * <b>dan</b> yang membuat penugasan {@code nama = getNamaGuru()} di dalam getter ini
	 * <b>tidak pernah tertulis ke database</b> — satu-satunya penugasan di dalam getter pada kelas
	 * ini yang aman dari efek tulis-balik.</p>
	 *
	 * <p>Properti inilah yang dipakai kolom grid {@code sort="auto(nama)"} pada {@code guru.zul}.</p>
	 *
	 * @return nama guru yang sudah dipangkas spasi (hasil {@link #getNamaGuru()}), tidak pernah
	 *         {@code null}
	 */
	@Column(name = "nama_guru", nullable = false, insertable = false, updatable = false)
	public String getNama() {
		nama = getNamaGuru();
		return nama;
	}

	/**
	 * Menyetel salinan nama.
	 *
	 * <p>Praktis tidak berpengaruh pada basis data karena kolomnya hanya-baca; nilainya akan
	 * ditimpa lagi oleh {@link #getNama()} pada pembacaan berikutnya. Setter ini ada semata agar
	 * kontrak JavaBean lengkap (dibutuhkan Hibernate dan pengikatan ZK).</p>
	 *
	 * @param nama nama yang hendak disalin
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan nilai kolom skalar {@code pegawai_id}.
	 *
	 * <p><b>Perhatian:</b> ini <b>bukan</b> identifier dari relasi {@link #getPegawai()} — relasi
	 * tersebut memakai kolom {@code pegawai}. Tidak ada kode di kelas ini yang menjaga keduanya
	 * tetap sinkron, jadi jangan menyimpulkan salah satu dari yang lain.</p>
	 *
	 * @return isi kolom {@code pegawai_id}, atau {@code null}
	 */
	@Column(name = "pegawai_id")
	public Long getPegawaiId() {
		return pegawaiId;
	}

	/**
	 * Menyetel kolom skalar {@code pegawai_id}.
	 *
	 * @param pegawaiId nilai kolom {@code pegawai_id}
	 */
	public void setPegawaiId(Long pegawaiId) {
		this.pegawaiId = pegawaiId;
	}

	/**
	 * Mengembalikan kode mesin sidik jari / kartu RFID guru.
	 *
	 * <p>Nilainya dicari berlapis: kolom database lebih dulu; bila kosong, diambil dari
	 * <b>penyimpanan kunci-nilai berbasis berkas</b> milik {@link GeneralValueObject} lewat
	 * {@code retreive("idfinger")}. Hasil akhirnya dipangkas spasi.</p>
	 *
	 * <p><b>Efek samping (tulis-balik):</b> nilai yang berasal dari penyimpanan kunci-nilai —
	 * maupun hasil pemangkasan spasi — berbeda dari isi kolom, sehingga ikut tertulis ke kolom
	 * {@code idfinger} pada flush berikutnya. Ini menjelaskan mengapa kolom tersebut bisa "terisi
	 * sendiri" tanpa ada yang menyuntingnya.</p>
	 *
	 * <p>Dipakai antara lain oleh ekspor kode finger pada {@code GuruAction} dan layar absensi
	 * kehadiran berbasis mesin sidik jari.</p>
	 *
	 * @return kode finger/RFID yang sudah dipangkas, atau {@code null} bila tidak ada di kedua sumber
	 * @see GeneralValueObject#retreive(String)
	 */
	public String getIdfinger() {
		String s = idfinger == null || idfinger.trim().isEmpty() ? retreive("idfinger") : idfinger;
		return s == null ? null : s.trim();
	}

	/**
	 * Menyimpan kode mesin sidik jari / kartu RFID.
	 *
	 * <p><b>Menulis ke DUA tempat:</b> penyimpanan kunci-nilai berbasis berkas
	 * ({@code put(nilai, "idfinger")}) dan field/kolom database. Nilai {@code null} atau kosong
	 * <b>diabaikan diam-diam</b> — kode finger yang sudah terpasang tidak bisa dihapus lewat setter
	 * ini.</p>
	 *
	 * @param idfinger kode finger/RFID; nilai {@code null}/kosong tidak berpengaruh
	 * @see GeneralValueObject#put(String, String)
	 */
	public void setIdfinger(String idfinger) {
		if (idfinger != null && !idfinger.trim().isEmpty()) {
			put(idfinger.trim(), "idfinger");
			this.idfinger = idfinger;
		}
	}

	/**
	 * Mengembalikan klasifikasi guru ({@link JenisGuru}).
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lebih dulu — pola wajib seluruh getter
	 * relasi di AIS untuk menghindari {@code LazyInitializationException} pada object yang sudah
	 * <i>detached</i>. Hasilnya ditugaskan ulang ke field karena {@code check()} bisa mengembalikan
	 * instance kanonik yang berbeda.</p>
	 *
	 * @return klasifikasi guru, atau {@code null} bila belum ditentukan
	 * @see JenisGuru
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_guru_id")
	public JenisGuru getJenisGuru() {
		jenisGuru = check(jenisGuru);
		return this.jenisGuru;
	}

	/**
	 * Menyetel klasifikasi guru.
	 *
	 * @param jenisGuru klasifikasi guru; boleh {@code null}
	 */
	public void setJenisGuru(JenisGuru jenisGuru) {
		this.jenisGuru = jenisGuru;
	}

	/**
	 * Mengembalikan sekolah penempatan utama guru.
	 *
	 * <p>Ini adalah <b>pembatas tenant utama</b> entity ini: {@code GuruAction.initCriteria()}
	 * menuntut kolom ini tidak {@code null} agar barisnya muncul di layar Manajemen Guru, dan
	 * {@link #getYayasan()} menurunkan yayasan dari sini.</p>
	 *
	 * @return sekolah penempatan utama, atau {@code null} bila belum ditetapkan
	 * @see #ambilSekolahs()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah penempatan utama.
	 *
	 * <p>Object {@link Sekolah} yang belum punya ID (baris baru yang belum tersimpan) diperlakukan
	 * sebagai {@code null} agar tidak menghasilkan FK menggantung.</p>
	 *
	 * @param sekolah sekolah penempatan; {@code null} atau sekolah tanpa ID akan disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengumpulkan ID seluruh sekolah tempat guru ini terdaftar — sekolah utama ditambah tiga
	 * sekolah tambahan.
	 *
	 * <p><b>Dipakai sebagai pelonggar kontrol akses saat login</b>: {@code FilterLoginAis
	 * .isSatuanKerjaValid(...)} membatalkan penolakan "satuan kerja tidak sesuai" bila sekolah yang
	 * hendak dimasuki termasuk dalam daftar ini. Jadi mengisi {@link #setSekolah1(Sekolah)} dan
	 * kawan-kawannya secara langsung memperluas cakupan login guru yang bersangkutan.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getSekolah()}, {@link #getSekolah1()},
	 * {@link #getSekolah2()}, {@link #getSekolah3()} — dan lewat {@link #getSekolah()} berpotensi
	 * memicu tulis-balik {@link #getYayasan()} pada pemanggil berikutnya. Nilai yang dibaca setelah
	 * pemeriksaan {@code null} sengaja diambil dari field, bukan dari getter lagi.</p>
	 *
	 * @return daftar ID sekolah (mungkin kosong, tidak pernah {@code null}); urutannya utama,
	 *         tambahan I, II, lalu III
	 */
	public List<Long> ambilSekolahs() {
		List<Long> list = new ArrayList<Long>();
		if (getSekolah() != null && sekolah.getId() != null) {
			list.add(sekolah.getId());
		}
		if (getSekolah1() != null && sekolah1.getId() != null) {
			list.add(sekolah1.getId());
		}
		if (getSekolah2() != null && sekolah2.getId() != null) {
			list.add(sekolah2.getId());
		}
		if (getSekolah3() != null && sekolah3.getId() != null) {
			list.add(sekolah3.getId());
		}
		return list;
	}

	/**
	 * Mengembalikan yayasan penaung guru.
	 *
	 * <p><b>GETTER DESTRUKTIF — baca uraian ini sebelum mengubah apa pun.</b> Method ini tidak
	 * sekadar membaca: bila {@link #getSekolah()} tidak {@code null}, field {@code yayasan}
	 * <b>selalu ditimpa</b> dengan {@code sekolah.getYayasan()}. Karena Hibernate memakai akses
	 * properti pada kelas ini, hasil penimpaan itu tertulis permanen ke kolom {@code yayasan_id}
	 * pada flush berikutnya. Dua akibat nyatanya:</p>
	 * <ul>
	 *   <li>Yayasan yang sengaja diisi berbeda oleh admin (mis. guru diperbantukan lintas yayasan)
	 *   tidak pernah bertahan — ia selalu kembali mengikuti yayasan sekolahnya.</li>
	 *   <li>Bila sekolahnya sendiri belum punya yayasan, kolom {@code yayasan_id} guru
	 *   <b>dikosongkan permanen</b>, sehingga guru tersebut hilang dari daftar dan laporan yang
	 *   menyaring per yayasan.</li>
	 * </ul>
	 * <p>Kerusakannya tidak menuntut aksi khusus: {@code GuruAction.GuruRenderer} memanggil getter
	 * ini untuk SETIAP baris grid Manajemen Guru, jadi cukup membuka daftar guru. Ini instance
	 * paling jelas dari pola getter destruktif yang sama dengan yang ditemukan di {@code Siswa.java}
	 * (b66).</p>
	 *
	 * @return yayasan penaung setelah diselaraskan dengan sekolah, atau {@code null}
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
	 * Menyetel yayasan penaung.
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini akan ditimpa lagi oleh {@link #getYayasan()}
	 * begitu baris ini dibaca sementara {@link #getSekolah()} terisi. Yayasan tidak bisa
	 * dipertahankan berbeda dari yayasan sekolahnya.</p>
	 *
	 * @param yayasan yayasan penaung; {@code null} atau yayasan tanpa ID disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan agama guru.
	 *
	 * @return agama, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "agama_id")
	public Agama getAgama() {
		agama = check(agama);
		return this.agama;
	}

	/**
	 * Menyetel agama guru.
	 *
	 * @param agama agama; boleh {@code null}
	 */
	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	/**
	 * Mengembalikan status kepegawaian (mis. PNS, GTY, honorer).
	 *
	 * @return status kepegawaian, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_kepegawaian")
	public StatusKepegawaian getStatusKepegawaian() {
		statusKepegawaian = check(statusKepegawaian);
		return statusKepegawaian;
	}

	/**
	 * Menyetel status kepegawaian.
	 *
	 * @param statusKepegawaian status kepegawaian; boleh {@code null}
	 */
	public void setStatusKepegawaian(StatusKepegawaian statusKepegawaian) {
		this.statusKepegawaian = statusKepegawaian;
	}

	/**
	 * Mengembalikan jenis PTK — apakah yang bersangkutan pendidik atau tenaga kependidikan.
	 *
	 * <p>Kolom ini yang membedakan "guru" sesungguhnya dari tenaga administrasi yang juga tersimpan
	 * di tabel {@code sekolah.guru}.</p>
	 *
	 * @return jenis pendidik/tenaga kependidikan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pendidik_dan_tenaga_kependidikan")
	public JenisPendidikDanTenagaKependidikan getJenisPendidikDanTenagaKependidikan() {
		jenisPendidikDanTenagaKependidikan = check(jenisPendidikDanTenagaKependidikan);
		return jenisPendidikDanTenagaKependidikan;
	}

	/**
	 * Menyetel jenis PTK.
	 *
	 * @param jenisPendidikDanTenagaKependidikan jenis pendidik/tenaga kependidikan; boleh {@code null}
	 */
	public void setJenisPendidikDanTenagaKependidikan(
			JenisPendidikDanTenagaKependidikan jenisPendidikDanTenagaKependidikan) {
		this.jenisPendidikDanTenagaKependidikan = jenisPendidikDanTenagaKependidikan;
	}

	/**
	 * Mengembalikan RT tempat tinggal.
	 *
	 * @return RT, atau {@code null}
	 */
	public String getRt() {
		return rt;
	}

	/**
	 * Menyetel RT tempat tinggal.
	 *
	 * @param rt RT
	 */
	public void setRt(String rt) {
		this.rt = rt;
	}

	/**
	 * Mengembalikan RW tempat tinggal.
	 *
	 * @return RW, atau {@code null}
	 */
	public String getRw() {
		return rw;
	}

	/**
	 * Menyetel RW tempat tinggal.
	 *
	 * @param rw RW
	 */
	public void setRw(String rw) {
		this.rw = rw;
	}

	/**
	 * Mengembalikan nomor HP guru dalam bentuk digit saja.
	 *
	 * <p><b>Efek samping (tulis-balik).</b> Seluruh karakter selain digit dan titik dibuang
	 * ({@code replaceAll("[^\\d.]", "")}), dan {@code null}/kosong menjadi string kosong. Karena
	 * Hibernate membaca kolom lewat getter ini, format asli yang diketik pengguna ("+62 812-3456",
	 * "0812 3456 (rumah)") <b>hilang permanen</b> pada flush pertama setelah baris ini dibaca.</p>
	 *
	 * @return nomor HP berisi digit dan titik saja; string kosong bila belum diisi (tidak pernah
	 *         {@code null})
	 */
	public String getHp() {
		return hp == null || hp.isEmpty() ? "" : hp.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menyetel nomor HP guru.
	 *
	 * @param hp nomor HP apa adanya; pembersihan dilakukan saat dibaca oleh {@link #getHp()}
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Mengembalikan sumber gaji guru (mis. APBN, APBD, yayasan).
	 *
	 * @return sumber gaji, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sumber_gaji")
	public SumberGaji getSumberGaji() {
		sumberGaji = check(sumberGaji);
		return sumberGaji;
	}

	/**
	 * Mengembalikan nomor SK CPNS.
	 *
	 * @return nomor SK CPNS, atau {@code null}
	 */
	public String getSkCpns() {
		return skCpns;
	}

	/**
	 * Menyetel nomor SK CPNS.
	 *
	 * @param skCpns nomor SK CPNS
	 */
	public void setSkCpns(String skCpns) {
		this.skCpns = skCpns;
	}

	/**
	 * Mengembalikan tanggal SK CPNS.
	 *
	 * @return tanggal SK CPNS, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSkCpns() {
		return tglSkCpns;
	}

	/**
	 * Menyetel tanggal SK CPNS.
	 *
	 * @param tglSkCpns tanggal SK CPNS
	 */
	public void setTglSkCpns(Date tglSkCpns) {
		this.tglSkCpns = tglSkCpns;
	}

	/**
	 * Mengembalikan nomor SK pengangkatan.
	 *
	 * @return nomor SK pengangkatan, atau {@code null}
	 */
	public String getSkAngkat() {
		return skAngkat;
	}

	/**
	 * Menyetel nomor SK pengangkatan.
	 *
	 * @param skAngkat nomor SK pengangkatan
	 */
	public void setSkAngkat(String skAngkat) {
		this.skAngkat = skAngkat;
	}

	/**
	 * Mengembalikan TMT (terhitung mulai tanggal) SK pengangkatan.
	 *
	 * @return TMT SK pengangkatan, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTmtSkAngkat() {
		return tmtSkAngkat;
	}

	/**
	 * Menyetel TMT SK pengangkatan.
	 *
	 * @param tmtSkAngkat TMT SK pengangkatan
	 */
	public void setTmtSkAngkat(Date tmtSkAngkat) {
		this.tmtSkAngkat = tmtSkAngkat;
	}

	/**
	 * Mengembalikan lembaga yang mengangkat guru ini.
	 *
	 * @return lembaga pengangkat, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lembaga_pengangkat")
	public LembagaPengangkat getLembagaPengangkat() {
		lembagaPengangkat = check(lembagaPengangkat);
		return lembagaPengangkat;
	}

	/**
	 * Mengembalikan TMT status PNS.
	 *
	 * @return TMT PNS, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTmtPns() {
		return tmtPns;
	}

	/**
	 * Menyetel TMT status PNS.
	 *
	 * @param tmtPns TMT PNS
	 */
	public void setTmtPns(Date tmtPns) {
		this.tmtPns = tmtPns;
	}

	/**
	 * Mengembalikan pangkat/golongan PNS guru.
	 *
	 * <p>Perhatikan ketidakselarasan penamaan: nama propertinya {@code golonganPegawai}, sedangkan
	 * kolomnya {@code golongan_pns} dan tipenya {@link GolonganPns}.</p>
	 *
	 * @return pangkat/golongan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "golongan_pns", nullable = true)
	public GolonganPns getGolonganPegawai() {
		golonganPegawai = check(golonganPegawai);
		return golonganPegawai;
	}

	/**
	 * Menyetel pangkat/golongan PNS.
	 *
	 * @param golonganPegawai pangkat/golongan; boleh {@code null}
	 */
	public void setGolonganPegawai(GolonganPns golonganPegawai) {
		this.golonganPegawai = golonganPegawai;
	}

	/**
	 * Menyetel lembaga pengangkat.
	 *
	 * @param lembagaPengangkat lembaga pengangkat; boleh {@code null}
	 */
	public void setLembagaPengangkat(LembagaPengangkat lembagaPengangkat) {
		this.lembagaPengangkat = lembagaPengangkat;
	}

	/**
	 * Menyetel sumber gaji.
	 *
	 * @param sumberGaji sumber gaji; boleh {@code null}
	 */
	public void setSumberGaji(SumberGaji sumberGaji) {
		this.sumberGaji = sumberGaji;
	}

	/**
	 * Mengembalikan nama dusun tempat tinggal.
	 *
	 * @return nama dusun, atau {@code null}
	 */
	public String getDusun() {
		return dusun;
	}

	/**
	 * Menyetel nama dusun tempat tinggal.
	 *
	 * @param dusun nama dusun
	 */
	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	/**
	 * Mengembalikan kelurahan/desa tempat tinggal (teks bebas, bukan relasi {@link Wilayah}).
	 *
	 * @return kelurahan, atau {@code null}
	 */
	public String getKelurahan() {
		return kelurahan;
	}

	/**
	 * Menyetel kelurahan/desa tempat tinggal.
	 *
	 * @param kelurahan nama kelurahan/desa
	 */
	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	/**
	 * Mengembalikan kecamatan tempat tinggal.
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Bila {@link Wilayah} yang tersimpan tidak punya wilayah induk
	 * ({@code getWilayahInduk() == null} — biasanya baris hasil impor Dapodik yang belum tertaut ke
	 * hierarki), method ini menyapu <b>seluruh</b> cache {@link Wilayah}
	 * ({@code ConstantValues.ambilBerdasarClass(Wilayah.class)}) mencari baris berkode
	 * {@code feeder} sama yang punya induk, lalu <b>mengganti field {@code kecamatan} dengan baris
	 * wilayah LAIN itu</b>. Karena kelas ini memakai akses properti, penggantian tersebut tertulis
	 * permanen ke kolom {@code kecamatan} pada flush berikutnya — alamat guru berubah tanpa ada
	 * yang menyuntingnya.</p>
	 *
	 * <p>Perilaku tambahan yang perlu diwaspadai: bila ada lebih dari satu kandidat berkode
	 * {@code feeder} sama, yang terpilih adalah yang <b>pertama ditemui saat iterasi cache</b>
	 * ({@code break} pada kecocokan pertama). Urutan iterasi cache tidak dijamin, sehingga hasilnya
	 * bisa berbeda antar-instalasi bahkan antar-restart.</p>
	 *
	 * @return kecamatan tempat tinggal (mungkin sudah digantikan baris wilayah lain), atau
	 *         {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan", nullable = true)
	public Wilayah getKecamatan() {
		kecamatan = check(kecamatan);

		if (kecamatan != null && kecamatan.getWilayahInduk() == null) {

			for (Object o : ConstantValues.ambilBerdasarClass(Wilayah.class).values()) {
				Wilayah w = (Wilayah) o;
				if (w != null && w.getFeeder() != null && kecamatan.getFeeder() != null
						&& kecamatan.getFeeder().equals(w.getFeeder()) && w.getWilayahInduk() != null) {
					kecamatan = w;
					break;
				}
			}

		}

		return kecamatan;
	}

	/**
	 * Menyetel kecamatan tempat tinggal.
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini bisa digantikan oleh {@link #getKecamatan()}
	 * bila wilayah yang dipilih belum punya wilayah induk.</p>
	 *
	 * @param kecamatan wilayah kecamatan; boleh {@code null}
	 */
	public void setKecamatan(Wilayah kecamatan) {
		this.kecamatan = kecamatan;
	}

	/**
	 * Mengembalikan kode pos alamat tempat tinggal.
	 *
	 * @return kode pos, atau {@code null}
	 */
	public String getKodePos() {
		return kodePos;
	}

	/**
	 * Menyetel kode pos alamat tempat tinggal.
	 *
	 * @param kodePos kode pos
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Mengembalikan alamat email guru.
	 *
	 * <p>Kolom ini boleh memuat <b>beberapa alamat sekaligus</b> dipisah koma — karena itulah ada
	 * pembersihan koma ganda di sini.</p>
	 *
	 * <p><b>Efek samping (tulis-balik):</b> field {@code alamatEmail} ditugaskan ulang di dalam
	 * getter, sehingga tiga normalisasi berikut tertulis permanen ke database pada flush
	 * berikutnya: {@code ",,"} dirapatkan menjadi {@code ","} (diulang lima kali sehingga menangani
	 * sampai sekitar 32 koma beruntun), {@code null} menjadi string kosong, dan koma tunggal
	 * menjadi string kosong. Bila jumlah koma beruntun melebihi batas itu, sisa koma ganda tetap
	 * tertinggal.</p>
	 *
	 * <p>Dipakai antara lain oleh {@link #tampilkanEmail(Component)}, pembuatan akun guru pada
	 * {@code GuruAction}, dan pengiriman surel massal.</p>
	 *
	 * @return alamat email (mungkin berisi beberapa alamat dipisah koma); string kosong bila tidak
	 *         ada, tidak pernah {@code null}
	 */
	@Column(name = "alamat_email")
	public String getAlamatEmail() {
		if (alamatEmail != null && alamatEmail.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				alamatEmail = alamatEmail.replaceAll(",,", ",");
			}
		}
		if (alamatEmail == null) {
			alamatEmail = "";
		}
		if (alamatEmail.trim().equals(",")) {
			alamatEmail = "";
		}
		return this.alamatEmail;
	}

	/**
	 * Menyetel alamat email guru.
	 *
	 * @param alamatEmail satu alamat, atau beberapa alamat dipisah koma
	 */
	public void setAlamatEmail(String alamatEmail) {
		this.alamatEmail = alamatEmail;
	}

	/**
	 * Mengembalikan alamat jalan tempat tinggal guru.
	 *
	 * <p>Kolom {@code alamat_guru} berkapasitas 2000 karakter; RT/RW/dusun/kelurahan/kecamatan/kode
	 * pos disimpan pada kolom terpisah.</p>
	 *
	 * @return alamat jalan, atau {@code null}
	 */
	@Column(name = "alamat_guru", length = 2000)
	public String getAlamatGuru() {
		return this.alamatGuru;
	}

	/**
	 * Menyetel alamat jalan tempat tinggal guru.
	 *
	 * @param alamatGuru alamat jalan
	 */
	public void setAlamatGuru(String alamatGuru) {
		this.alamatGuru = alamatGuru;
	}

	/**
	 * Mengembalikan jenis kelamin dalam bentuk baku.
	 *
	 * <p><b>Efek samping (tulis-balik):</b> field {@code jenisKelamin} ditugaskan ulang di dalam
	 * getter, sehingga pembakuan berikut tertulis permanen ke database: "L"/"Laki-Laki" dan setiap
	 * nilai yang <i>mengandung</i> "laki" menjadi <b>"Laki-laki"</b>; "P" dan setiap nilai yang
	 * mengandung "puan" menjadi <b>"Perempuan"</b>; {@code null} menjadi string kosong.</p>
	 *
	 * <p>Karena pemeriksaan tahap kedua memakai {@code contains}, nilai bebas seperti
	 * "Laki-laki (belum verifikasi)" ikut dipadatkan menjadi "Laki-laki" — keterangan tambahan yang
	 * pernah diketik petugas hilang permanen. Nilai yang tidak mengandung kedua kata kunci
	 * (mis. salah ketik "Lki-laki") dibiarkan apa adanya.</p>
	 *
	 * @return "Laki-laki", "Perempuan", string kosong, atau nilai asli yang tidak dikenali; tidak
	 *         pernah {@code null}
	 */
	@Column(name = "jenis_kelamin", nullable = true, length = 255)
	public String getJenisKelamin() {

		if (jenisKelamin != null
				&& (jenisKelamin.trim().equalsIgnoreCase("L") || jenisKelamin.trim().equals("Laki-Laki"))) {
			jenisKelamin = "Laki-laki";
		} else if (jenisKelamin != null && jenisKelamin.trim().equalsIgnoreCase("P")) {
			jenisKelamin = "Perempuan";
		} else if (jenisKelamin == null) {
			jenisKelamin = "";
		}

		if (jenisKelamin.toLowerCase().contains("laki")) {
			jenisKelamin = "Laki-laki";
		} else if (jenisKelamin.toLowerCase().contains("puan")) {
			jenisKelamin = "Perempuan";
		}

		return this.jenisKelamin;
	}

	/**
	 * Menyetel jenis kelamin apa adanya.
	 *
	 * @param jenisKelamin jenis kelamin; pembakuan dilakukan saat dibaca oleh
	 *                     {@link #getJenisKelamin()}
	 */
	public void setJenisKelamin(String jenisKelamin) {
		this.jenisKelamin = jenisKelamin;
	}

	/**
	 * Mengembalikan nama lengkap guru.
	 *
	 * <p>Kolom {@code nama_guru} berstatus wajib isi ({@code nullable = false}). Getter ini
	 * mengembalikan string kosong (bukan {@code null}) bila belum terisi, dan memangkas spasi tepi
	 * — pemangkasan itu ikut tertulis ke database pada flush berikutnya.</p>
	 *
	 * <p>Nilai inilah yang menjadi sumber {@link #getNama()}, {@link #getNamaAr()},
	 * {@link #getNamaCh()}, {@link #getNamaWajibPajak()}, dan urutan bawaan daftar guru
	 * ({@code Order.asc("namaGuru")}).</p>
	 *
	 * @return nama lengkap guru yang sudah dipangkas; string kosong bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	@Column(name = "nama_guru", nullable = false)
	public String getNamaGuru() {
		return this.namaGuru == null ? "" : namaGuru.trim();
	}

	/**
	 * Menyetel nama lengkap guru.
	 *
	 * @param namaGuru nama lengkap
	 */
	public void setNamaGuru(String namaGuru) {
		this.namaGuru = namaGuru;
	}

	/**
	 * Mengembalikan tanggal lahir guru.
	 *
	 * <p>Atribut {@code length = 13} pada anotasi kolom adalah peninggalan pembangkit hbm2java dan
	 * tidak berpengaruh pada tipe {@code DATE}.</p>
	 *
	 * @return tanggal lahir, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_lahir", length = 13)
	public Date getTanggalLahir() {
		return this.tanggalLahir;
	}

	/**
	 * Menyetel tanggal lahir guru.
	 *
	 * @param tanggalLahir tanggal lahir
	 */
	public void setTanggalLahir(Date tanggalLahir) {
		this.tanggalLahir = tanggalLahir;
	}

	/**
	 * Mengembalikan nomor telepon guru apa adanya (tanpa pembersihan).
	 *
	 * <p>Berbeda dari {@link #getHp()} yang membuang karakter non-digit, getter ini mengembalikan
	 * isi kolom apa adanya. {@link #tampilkanHp(Component)} membaca kolom INI — bukan {@link #getHp()}
	 * — saat membentuk tautan WhatsApp.</p>
	 *
	 * @return nomor telepon, atau {@code null}
	 */
	@Column(name = "telepon_guru")
	public String getTeleponGuru() {
		return this.teleponGuru;
	}

	/**
	 * Menyetel nomor telepon guru.
	 *
	 * @param teleponGuru nomor telepon
	 */
	public void setTeleponGuru(String teleponGuru) {
		this.teleponGuru = teleponGuru;
	}

	/**
	 * Mengembalikan tempat lahir guru.
	 *
	 * @return tempat lahir, atau {@code null}
	 */
	@Column(name = "tempat_lahir")
	public String getTempatLahir() {
		return this.tempatLahir;
	}

	/**
	 * Menyetel tempat lahir guru.
	 *
	 * @param tempatLahir tempat lahir
	 */
	public void setTempatLahir(String tempatLahir) {
		this.tempatLahir = tempatLahir;
	}

	/**
	 * Mengembalikan NIP guru (berlaku untuk guru berstatus PNS).
	 *
	 * <p>Nilai kolom ini juga dipakai sebagai <b>nama berkas</b> pada unggah/unduh foto massal di
	 * layar Manajemen Guru ("nama berkas = NIP"), jadi mengubahnya memutus keterkaitan foto yang
	 * sudah terunggah.</p>
	 *
	 * @return NIP, atau {@code null}
	 */
	@Column(name = "nip")
	public String getNip() {
		return this.nip;
	}

	/**
	 * Menyetel NIP guru.
	 *
	 * @param nip NIP
	 */
	public void setNip(String nip) {
		this.nip = nip;
	}

	/**
	 * Mengembalikan kode guru internal instalasi.
	 *
	 * <p>Selain sebagai kunci pencarian di layar Manajemen Guru (kotak "Kode/NUPTK"), nilai ini
	 * dipakai sebagai <b>calon username akun guru</b> bila konfigurasi
	 * {@code menggunakan_kode_generate_guru} aktif, dan ikut disematkan ke kode QR tanda tangan
	 * ({@link #ttdQr()}).</p>
	 *
	 * @return kode guru, atau {@code null}
	 * @see #ambilKode()
	 */
	@Column(name = "kode")
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode guru internal.
	 *
	 * @param kode kode guru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan status pegawai (mis. aktif, pensiun, mutasi).
	 *
	 * <p>Berbeda dari {@link #getAktif()} yang sekadar penanda boolean; status ini bernilai
	 * referensi dan ditampilkan sebagai kolom "Status" pada grid Manajemen Guru.</p>
	 *
	 * @return status pegawai, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_pegawai")
	public StatusPegawai getStatusPegawai() {
		statusPegawai = check(statusPegawai);
		return statusPegawai;
	}

	/**
	 * Menyetel status pegawai.
	 *
	 * @param statusPegawai status pegawai; boleh {@code null}
	 */
	public void setStatusPegawai(StatusPegawai statusPegawai) {
		this.statusPegawai = statusPegawai;
	}

	/**
	 * Mengembalikan penanda kepemilikan lembaga — berlabel <b>"Milik Yayasan"</b> pada UI meski
	 * nama propertinya menyebut universitas (peninggalan basis kode perguruan tinggi).
	 *
	 * <p><b>Efek samping (tulis-balik):</b> nilai {@code null} dikembalikan sebagai {@code false},
	 * sehingga keadaan "belum diisi" berubah permanen menjadi "tidak" pada flush berikutnya.</p>
	 *
	 * @return {@code true} bila milik yayasan; {@code false} bila tidak atau belum diisi (tidak
	 *         pernah {@code null})
	 */
	public Boolean getMilikUniversitas() {
		return milikUniversitas == null ? false : milikUniversitas;
	}

	/**
	 * Menyetel penanda kepemilikan lembaga.
	 *
	 * @param milikUniversitas {@code true} bila milik yayasan
	 */
	public void setMilikUniversitas(Boolean milikUniversitas) {
		this.milikUniversitas = milikUniversitas;
	}

	/**
	 * Mengembalikan nama panggilan guru.
	 *
	 * @return nama panggilan, atau {@code null}
	 */
	public String getPanggilan() {
		return panggilan;
	}

	/**
	 * Menyetel nama panggilan guru.
	 *
	 * @param panggilan nama panggilan
	 */
	public void setPanggilan(String panggilan) {
		this.panggilan = panggilan;
	}

	/**
	 * Mengembalikan nama ayah kandung guru (data pribadi pihak ketiga; kolom panjang 100).
	 *
	 * @return nama ayah, atau {@code null}
	 */
	@Column(name = "nama_ayah", length = 100)
	public String getNamaAyah() {
		return this.namaAyah;
	}

	/**
	 * Menyetel nama ayah kandung guru.
	 *
	 * @param namaAyah nama ayah
	 */
	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	/**
	 * Mengembalikan pekerjaan ayah guru.
	 *
	 * <p>Memakai katalog {@link PekerjaanOrangTua} yang sama dengan modul siswa — jadi katalog itu
	 * dipakai lintas domain, bukan khusus orang tua peserta didik.</p>
	 *
	 * @return pekerjaan ayah, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_pekerjaan_ayah", nullable = true)
	public PekerjaanOrangTua getPekerjaanAyah() {
		pekerjaanAyah = check(pekerjaanAyah);
		return this.pekerjaanAyah;
	}

	/**
	 * Menyetel pekerjaan ayah guru.
	 *
	 * @param pekerjaanAyah pekerjaan ayah; boleh {@code null}
	 */
	public void setPekerjaanAyah(PekerjaanOrangTua pekerjaanAyah) {
		this.pekerjaanAyah = pekerjaanAyah;
	}

	/**
	 * Mengembalikan nama ibu kandung guru (data pribadi pihak ketiga; kolom panjang 100).
	 *
	 * <p>Nama ibu kandung lazim dipakai sebagai pertanyaan verifikasi identitas, sehingga kolom ini
	 * lebih sensitif daripada tampilannya — dan ikut terekspor oleh unduhan Excel guru.</p>
	 *
	 * @return nama ibu, atau {@code null}
	 */
	@Column(name = "nama_ibu", length = 100)
	public String getNamaIbu() {
		return this.namaIbu;
	}

	/**
	 * Menyetel nama ibu kandung guru.
	 *
	 * @param namaIbu nama ibu
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Mengembalikan pekerjaan ibu guru.
	 *
	 * @return pekerjaan ibu, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_pekerjaan_ibu", nullable = true)
	public PekerjaanOrangTua getPekerjaanIbu() {
		pekerjaanIbu = check(pekerjaanIbu);
		return this.pekerjaanIbu;
	}

	/**
	 * Menyetel pekerjaan ibu guru.
	 *
	 * @param pekerjaanIbu pekerjaan ibu; boleh {@code null}
	 */
	public void setPekerjaanIbu(PekerjaanOrangTua pekerjaanIbu) {
		this.pekerjaanIbu = pekerjaanIbu;
	}

	/**
	 * Mengembalikan status perkawinan guru (kolom {@code status_nikah_s}).
	 *
	 * <p><b>Efek samping (tulis-balik):</b> field ditugaskan ulang menjadi "Belum Kawin" bila
	 * kolomnya {@code null}, sehingga keadaan "belum diisi" berubah permanen menjadi pernyataan
	 * faktual "Belum Kawin" begitu baris ini dibaca. Nilainya teks bebas — tidak ada katalog
	 * referensi yang membatasi.</p>
	 *
	 * @return status perkawinan; tidak pernah {@code null}
	 */
	@Column(name = "status_nikah_s")
	public String getStatusNikah() {
		if (statusNikah == null) {
			statusNikah = "Belum Kawin";
		}
		return this.statusNikah;
	}

	/**
	 * Menyetel status perkawinan guru.
	 *
	 * @param statusNikah status perkawinan sebagai teks bebas
	 */
	public void setStatusNikah(String statusNikah) {
		this.statusNikah = statusNikah;
	}

	/**
	 * Mengembalikan kewarganegaraan guru sebagai teks bebas.
	 *
	 * <p>Terpisah dan tidak disinkronkan dengan relasi {@link #getNegara()}; keduanya bisa saling
	 * bertentangan.</p>
	 *
	 * @return kewarganegaraan, atau {@code null}
	 */
	public String getKewarganegaraan() {
		return kewarganegaraan;
	}

	/**
	 * Menyetel kewarganegaraan guru.
	 *
	 * @param kewarganegaraan kewarganegaraan sebagai teks bebas
	 */
	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	/**
	 * Mengembalikan nama suami/istri guru (data pribadi pihak ketiga).
	 *
	 * @return nama suami/istri, atau {@code null}
	 */
	public String getNamaSuamiIstri() {
		return namaSuamiIstri;
	}

	/**
	 * Menyetel nama suami/istri guru.
	 *
	 * @param namaSuamiIstri nama suami/istri
	 */
	public void setNamaSuamiIstri(String namaSuamiIstri) {
		this.namaSuamiIstri = namaSuamiIstri;
	}

	/**
	 * Mengembalikan NIP suami/istri guru (data pribadi pihak ketiga).
	 *
	 * @return NIP suami/istri, atau {@code null}
	 */
	public String getNipSuamiIstri() {
		return nipSuamiIstri;
	}

	/**
	 * Menyetel NIP suami/istri guru.
	 *
	 * @param nipSuamiIstri NIP suami/istri
	 */
	public void setNipSuamiIstri(String nipSuamiIstri) {
		this.nipSuamiIstri = nipSuamiIstri;
	}

	/**
	 * Mengembalikan gelar akademik yang dipasang di depan nama.
	 *
	 * @return gelar depan, atau {@code null}
	 */
	public String getGelarDepan() {
		return gelarDepan;
	}

	/**
	 * Menyetel gelar akademik depan.
	 *
	 * @param gelarDepan gelar depan
	 */
	public void setGelarDepan(String gelarDepan) {
		this.gelarDepan = gelarDepan;
	}

	/**
	 * Mengembalikan gelar akademik yang dipasang di belakang nama.
	 *
	 * @return gelar belakang, atau {@code null}
	 */
	public String getGelarBelakang() {
		return gelarBelakang;
	}

	/**
	 * Menyetel gelar akademik belakang.
	 *
	 * @param gelarBelakang gelar belakang
	 */
	public void setGelarBelakang(String gelarBelakang) {
		this.gelarBelakang = gelarBelakang;
	}

	/**
	 * Mengembalikan jenjang pendidikan terakhir guru.
	 *
	 * @return pendidikan terakhir, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan", nullable = true)
	public Pendidikan getPendidikan() {
		pendidikan = check(pendidikan);
		return pendidikan;
	}

	/**
	 * Menyetel jenjang pendidikan terakhir guru.
	 *
	 * @param pendidikan pendidikan terakhir; boleh {@code null}
	 */
	public void setPendidikan(Pendidikan pendidikan) {
		this.pendidikan = pendidikan;
	}

	/**
	 * Mengembalikan pekerjaan suami/istri guru.
	 *
	 * @return pekerjaan suami/istri, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_suami_istri", nullable = true)
	public PekerjaanOrangTua getPekerjaanSuamiIstri() {
		pekerjaanSuamiIstri = check(pekerjaanSuamiIstri);
		return pekerjaanSuamiIstri;
	}

	/**
	 * Menyetel pekerjaan suami/istri guru.
	 *
	 * @param pekerjaanSuamiIstri pekerjaan suami/istri; boleh {@code null}
	 */
	public void setPekerjaanSuamiIstri(PekerjaanOrangTua pekerjaanSuamiIstri) {
		this.pekerjaanSuamiIstri = pekerjaanSuamiIstri;
	}

	/**
	 * Mengembalikan negara asal guru.
	 *
	 * <p><b>Efek samping (tulis-balik):</b> bila kolomnya {@code null}, yang dikembalikan adalah
	 * {@link ConstantValues#INDONESIA}. Berbeda dari getter lain, di sini field TIDAK ditugaskan
	 * ulang — namun karena Hibernate membaca nilai lewat getter, {@code negara_id} tetap ikut
	 * tertulis dengan ID Indonesia pada flush berikutnya. Akibatnya kolom ini tidak bisa
	 * mempertahankan keadaan "belum diisi".</p>
	 *
	 * @return negara asal; {@link ConstantValues#INDONESIA} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "negara_id")
	public Negara getNegara() {
		negara = check(negara);
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

	/**
	 * Menyetel negara asal guru.
	 *
	 * @param negara negara asal; boleh {@code null} (akan dibaca sebagai Indonesia)
	 */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/**
	 * Mengembalikan penanda guru masih aktif.
	 *
	 * <p><b>Efek samping (tulis-balik):</b> {@code null} dikembalikan sebagai {@code true} —
	 * artinya guru yang belum pernah ditandai akan tercatat permanen sebagai aktif. Pemilihan
	 * default {@code true} ini sejalan dengan filter bawaan layar Manajemen Guru
	 * ("Tampilkan hanya yang aktif" tercentang, dan kriterianya menerima {@code null} sebagai
	 * aktif).</p>
	 *
	 * <p>Nilai ini diubah langsung dari grid lewat checkbox "Aktif" yang dinonaktifkan bila
	 * pengguna tidak punya hak ubah.</p>
	 *
	 * @return {@code true} bila aktif atau belum ditandai; {@code false} bila dinonaktifkan (tidak
	 *         pernah {@code null})
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda guru aktif.
	 *
	 * @param aktif {@code true} bila aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan relasi ke data kepegawaian umum ({@link Pegawai}).
	 *
	 * <p>Tautan ini dibentuk lewat tombol "Singkronkan dg pegawai" di layar Manajemen Guru dan
	 * dipakai laporan kepegawaian. <b>Bukan</b> pasangan dari {@link #getPegawaiId()}: relasi ini
	 * memakai kolom {@code pegawai}, sedangkan {@code pegawaiId} adalah kolom skalar terpisah.</p>
	 *
	 * @return data pegawai terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel relasi ke data kepegawaian umum.
	 *
	 * @param pegawai data pegawai; boleh {@code null}
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Menempelkan tombol tautan surel guru ke komponen ZK yang diberikan.
	 *
	 * <p>Selalu membuat satu {@link Toolbarbutton} berlabel alamat surel dan memasangnya sebagai
	 * anak {@code vbox}; ikon amplop, gaya, target {@code _blank}, dan {@code href}
	 * {@code mailto:} hanya dipasang bila alamatnya tidak kosong. Jadi bila guru belum punya surel,
	 * yang muncul adalah tombol berlabel kosong tanpa tautan (bukan tanpa komponen sama sekali).</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getAlamatEmail()}, sehingga ikut memicu normalisasi
	 * tulis-balik alamat surel. Method ini adalah bagian dari kontrak presentasi bersama yang juga
	 * dipunyai {@code Mahasiswa}/{@code Dosen}/{@code Pegawai}/{@code Tbmuser}, dan dipanggil dari
	 * puluhan <i>renderer</i> daftar.</p>
	 *
	 * <p>Namanya sengaja tidak berpola {@code getXxx}/{@code setXxx} agar tidak ikut dipetakan
	 * Hibernate.</p>
	 *
	 * @param vbox komponen ZK induk tempat tombol ditempelkan
	 */
	public void tampilkanEmail(Component vbox) {
		String email = getAlamatEmail();
		Toolbarbutton a;
		(a = new ais.ui.util.MyToolbarbuttonConfig(email)).setParent(vbox);
		if (email != null && !email.trim().isEmpty()) {
			a.setImage("/img/svg/mail-send-line.svg");
			a.setStyle("font-size:9px;");
			a.setTarget("_blank");
			a.setHref("mailto:" + email);
		}

	}

	/**
	 * Menempelkan tombol tautan WhatsApp guru ke komponen ZK yang diberikan.
	 *
	 * <p>Sumber nomornya adalah {@link #getTeleponGuru()} — <b>bukan</b> {@link #getHp()} — jadi
	 * kolom HP tidak pernah dipakai untuk tautan ini. Nomor "kosong palsu" yang lazim muncul dari
	 * impor ({@code "00000000000000000000"} dan {@code "000000000"}) sengaja diperlakukan sebagai
	 * kosong sehingga tautannya tidak dipasang.</p>
	 *
	 * <p>Nomor yang lolos dinormalisasi ke format internasional secara berlapis: awalan "08" dan
	 * "0" diganti "+62", dan nomor yang belum berawalan "+" tetap diberi awalan "+62". Perhatikan
	 * bahwa lapis ketiga membuat nomor asing tanpa "+" (mis. "60123...") ikut diberi awalan "+62"
	 * sehingga menjadi keliru. Normalisasi ini hanya untuk tautan — <b>tidak</b> mengubah data yang
	 * tersimpan (nilainya ditampung variabel lokal).</p>
	 *
	 * @param vbox komponen ZK induk tempat tombol ditempelkan
	 */
	public void tampilkanHp(Component vbox) {
		Toolbarbutton a;
		String hp = getTeleponGuru();
		(a = new ais.ui.util.MyToolbarbuttonConfig(hp)).setParent(vbox);
		if (hp != null && !hp.trim().isEmpty() && !(hp == null || hp.toString().trim().isEmpty()
				|| hp.toString().trim().equals("00000000000000000000") || hp.toString().trim().equals("000000000"))) {
			hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
			hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
			hp = !hp.startsWith("+") ? "+62" + hp : hp;
			a.setStyle("font-size:9px;");
			a.setImage("/img/svg/whats.svg");
			a.setTarget("_blank");
			a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
		}
	}

	/**
	 * Mengembalikan bahasa antarmuka pilihan guru.
	 *
	 * <p>Satu-satunya properti {@link NotAudited} di kelas ini — perubahannya sengaja tidak direkam
	 * Envers karena dianggap preferensi tampilan, bukan data kepegawaian.</p>
	 *
	 * <p><b>Efek samping (tulis-balik):</b> nilai kosong dikembalikan sebagai
	 * {@link Tbmuser#INDONESIA}, sehingga kolomnya ikut terisi permanen pada flush berikutnya
	 * (tanpa jejak audit, karena {@code @NotAudited}).</p>
	 *
	 * @return kode bahasa antarmuka; tidak pernah kosong
	 */
	@Column(name = "bahasa")
	@NotAudited
	public String getBahasa() {
		return this.bahasa == null || bahasa.trim().isEmpty() ? Tbmuser.INDONESIA : bahasa;
	}

	/**
	 * Menyetel bahasa antarmuka pilihan guru.
	 *
	 * @param bahasa kode bahasa
	 */
	public void setBahasa(String bahasa) {
		this.bahasa = bahasa;
	}

	/**
	 * Implementasi {@link VOMahasiswaDosen#ambilKode()} — mengembalikan kode identitas pemilik
	 * materi pertemuan.
	 *
	 * <p>Bagi guru, kode itu adalah {@link #getKode()} (kode guru internal), sepadan dengan NIM
	 * bagi mahasiswa dan NIDN bagi dosen. Dipakai layar e-learning/materi yang memperlakukan guru,
	 * dosen, dan mahasiswa lewat antarmuka yang sama.</p>
	 *
	 * @return kode guru, atau {@code null} bila belum diisi
	 */
	@Override
	public String ambilKode() {
		// TODO Auto-generated method stub
		return getKode();
	}

	/**
	 * Implementasi {@link VOMahasiswaDosen#ambilMateri} — mengambil daftar berkas materi untuk
	 * sekumpulan pertemuan.
	 *
	 * <p>Seluruh pekerjaan didelegasikan ke
	 * {@code PertemuanFileContent.ambilMateri(pertemuans, refresh, label, tbmuser)}; kelas ini
	 * hanya menyediakan pengguna yang sedang login lewat {@code Common.getCurrentUser()}.</p>
	 *
	 * <p><b>Catatan penting:</b> konteks yang dipakai adalah <b>pengguna yang sedang login</b>,
	 * bukan guru yang diwakili object ini. Jadi hasilnya bergantung pada siapa yang memanggil, bukan
	 * pada guru mana object ini merujuk — pastikan hal itu memang yang diinginkan sebelum memakai
	 * method ini di luar konteks "guru melihat materinya sendiri".</p>
	 *
	 * @param pertemuans peta pertemuan yang hendak diambil materinya (kunci: penanda pertemuan,
	 *                   nilai: ID pertemuan)
	 * @param refresh    {@code true} untuk memaksa pembacaan ulang, mengabaikan cache
	 * @param label      label ZK yang dipakai menampilkan kemajuan proses; boleh {@code null}
	 * @return peta materi per pertemuan sebagaimana dikembalikan {@link PertemuanFileContent}
	 */
	@Override
	public TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label) {
		Tbmuser tbmuser = Common.getCurrentUser();
		return PertemuanFileContent.ambilMateri(pertemuans, refresh, label, tbmuser);
	}

	/**
	 * Mengembalikan koordinat lintang rumah guru (label UI "Koordinat Lintang Google Map").
	 *
	 * <p><b>Data sensitif</b>: bersama {@link #getBujur()} nilai ini menunjuk lokasi tempat tinggal
	 * secara presisi, dan ikut terekspor mentah oleh unduhan Excel guru.</p>
	 *
	 * @return koordinat lintang sebagai teks, atau {@code null}
	 */
	public String getLintang() {
		return lintang;
	}

	/**
	 * Menyetel koordinat lintang rumah guru.
	 *
	 * @param lintang koordinat lintang sebagai teks
	 */
	public void setLintang(String lintang) {
		this.lintang = lintang;
	}

	/**
	 * Mengembalikan koordinat bujur rumah guru (label UI "Koordinat Bujur Google Map").
	 *
	 * @return koordinat bujur sebagai teks, atau {@code null}
	 * @see #getLintang()
	 */
	public String getBujur() {
		return bujur;
	}

	/**
	 * Menyetel koordinat bujur rumah guru.
	 *
	 * @param bujur koordinat bujur sebagai teks
	 */
	public void setBujur(String bujur) {
		this.bujur = bujur;
	}

	/**
	 * Mengembalikan NUPTK (Nomor Unik Pendidik dan Tenaga Kependidikan).
	 *
	 * <p>Ikut menjadi kunci pencarian di layar Manajemen Guru (kotak "Kode/NUPTK") dan disematkan
	 * ke kode QR tanda tangan ({@link #ttdQr()}).</p>
	 *
	 * @return NUPTK, atau {@code null}
	 */
	public String getNuptk() {
		return nuptk;
	}

	/**
	 * Menyetel NUPTK guru.
	 *
	 * @param nuptk NUPTK
	 */
	public void setNuptk(String nuptk) {
		this.nuptk = nuptk;
	}

	/**
	 * Mengembalikan jurusan/program studi pendidikan terakhir guru.
	 *
	 * @return jurusan/prodi, atau {@code null}
	 */
	public String getJurusan() {
		return jurusan;
	}

	/**
	 * Menyetel jurusan/program studi pendidikan terakhir.
	 *
	 * @param jurusan jurusan/prodi
	 */
	public void setJurusan(String jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan TMT (terhitung mulai tanggal) guru bekerja di lembaga ini.
	 *
	 * @return TMT kerja, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTmtKerja() {
		return tmtKerja;
	}

	/**
	 * Menyetel TMT guru mulai bekerja.
	 *
	 * @param tmtKerja TMT kerja
	 */
	public void setTmtKerja(Date tmtKerja) {
		this.tmtKerja = tmtKerja;
	}

	/**
	 * Mengembalikan uraian tugas tambahan guru (kolom bertipe {@code text}).
	 *
	 * <p>Teks bebas untuk kebutuhan laporan Dapodik; tidak berelasi dengan
	 * {@link PenugasanGuruMengajar} maupun {@link GuruMengajar}.</p>
	 *
	 * @return uraian tugas tambahan, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getTugasTambahan() {
		return tugasTambahan;
	}

	/**
	 * Menyetel uraian tugas tambahan guru.
	 *
	 * @param tugasTambahan uraian tugas tambahan
	 */
	public void setTugasTambahan(String tugasTambahan) {
		this.tugasTambahan = tugasTambahan;
	}

	/**
	 * Mengembalikan uraian bebas tentang apa yang diajarkan guru (kolom bertipe {@code text}).
	 *
	 * <p><b>Bukan</b> sumber jadwal sesungguhnya — penugasan mengajar yang mengikat ada di
	 * {@link GuruMengajar} dan {@link PenugasanGuruMengajar}. Kolom ini semata catatan biodata,
	 * sehingga bisa saja bertentangan dengan jadwal yang tercatat.</p>
	 *
	 * @return uraian mengajar, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getMengajar() {
		return mengajar;
	}

	/**
	 * Menyetel uraian bebas tentang apa yang diajarkan guru.
	 *
	 * @param mengajar uraian mengajar
	 */
	public void setMengajar(String mengajar) {
		this.mengajar = mengajar;
	}

	/**
	 * Mengembalikan uraian jam untuk tugas tambahan (kolom bertipe {@code text}).
	 *
	 * @return uraian jam tugas tambahan, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getJamTugasTambahan() {
		return jamTugasTambahan;
	}

	/**
	 * Menyetel uraian jam untuk tugas tambahan.
	 *
	 * @param jamTugasTambahan uraian jam tugas tambahan
	 */
	public void setJamTugasTambahan(String jamTugasTambahan) {
		this.jamTugasTambahan = jamTugasTambahan;
	}

	/**
	 * Mengembalikan Nomor Induk Kependudukan guru.
	 *
	 * <p><b>Data sensitif.</b> Nilai ini ditampilkan langsung sebagai salah satu label pada setiap
	 * baris grid Manajemen Guru dan ikut terekspor mentah oleh unduhan Excel guru.</p>
	 *
	 * @return NIK, atau {@code null}
	 */
	public String getNik() {
		return nik;
	}

	/**
	 * Menyetel Nomor Induk Kependudukan guru.
	 *
	 * @param nik NIK
	 */
	public void setNik(String nik) {
		this.nik = nik;
	}

	/**
	 * Mengembalikan nomor Kartu Keluarga guru.
	 *
	 * <p><b>Data sensitif</b>; ikut terekspor oleh unduhan Excel guru.</p>
	 *
	 * @return nomor KK, atau {@code null}
	 */
	public String getKk() {
		return kk;
	}

	/**
	 * Menyetel nomor Kartu Keluarga guru.
	 *
	 * @param kk nomor Kartu Keluarga
	 */
	public void setKk(String kk) {
		this.kk = kk;
	}

	/**
	 * Mengembalikan nomor Kartu Pegawai (Karpeg) guru PNS.
	 *
	 * @return nomor Karpeg, atau {@code null}
	 */
	public String getKarpeg() {
		return karpeg;
	}

	/**
	 * Menyetel nomor Kartu Pegawai.
	 *
	 * @param karpeg nomor Karpeg
	 */
	public void setKarpeg(String karpeg) {
		this.karpeg = karpeg;
	}

	/**
	 * Mengembalikan nomor Karis/Karsu (kartu istri/suami) guru PNS.
	 *
	 * @return nomor Karis/Karsu, atau {@code null}
	 */
	public String getKarisKarsu() {
		return karisKarsu;
	}

	/**
	 * Menyetel nomor Karis/Karsu.
	 *
	 * @param karisKarsu nomor Karis/Karsu
	 */
	public void setKarisKarsu(String karisKarsu) {
		this.karisKarsu = karisKarsu;
	}

	/**
	 * Mengembalikan NUKS (Nomor Unik Kepala Sekolah).
	 *
	 * <p>Hanya bermakna bagi guru yang menjabat kepala sekolah; berpasangan dengan
	 * {@link #getSudahLisensiKepalaSekolah()}.</p>
	 *
	 * @return NUKS, atau {@code null}
	 */
	public String getNuks() {
		return nuks;
	}

	/**
	 * Menyetel NUKS.
	 *
	 * @param nuks Nomor Unik Kepala Sekolah
	 */
	public void setNuks(String nuks) {
		this.nuks = nuks;
	}

	/**
	 * Mengembalikan nomor rekening bank guru.
	 *
	 * <p><b>Data sensitif finansial.</b> Bersama {@link #getBank()} dan
	 * {@link #getRekeningAtasNama()} kolom ini menjadi tujuan pembayaran gaji/honor, dan ketiganya
	 * ikut terekspor mentah oleh unduhan Excel guru.</p>
	 *
	 * @return nomor rekening, atau {@code null}
	 */
	public String getNomorRekeningBank() {
		return nomorRekeningBank;
	}

	/**
	 * Menyetel nomor rekening bank guru.
	 *
	 * @param nomorRekeningBank nomor rekening
	 */
	public void setNomorRekeningBank(String nomorRekeningBank) {
		this.nomorRekeningBank = nomorRekeningBank;
	}

	/**
	 * Mengembalikan nama pemilik rekening bank.
	 *
	 * <p>Bisa berbeda dari nama guru (mis. rekening atas nama pasangan), karena itu disimpan
	 * terpisah dan tidak diturunkan dari {@link #getNamaGuru()}.</p>
	 *
	 * @return nama pemilik rekening, atau {@code null}
	 */
	public String getRekeningAtasNama() {
		return rekeningAtasNama;
	}

	/**
	 * Menyetel nama pemilik rekening bank.
	 *
	 * @param rekeningAtasNama nama pemilik rekening
	 */
	public void setRekeningAtasNama(String rekeningAtasNama) {
		this.rekeningAtasNama = rekeningAtasNama;
	}

	/**
	 * Mengembalikan nama bank tempat rekening guru dibuka (teks bebas, bukan katalog referensi).
	 *
	 * @return nama bank, atau {@code null}
	 */
	public String getBank() {
		return bank;
	}

	/**
	 * Menyetel nama bank.
	 *
	 * @param bank nama bank
	 */
	public void setBank(String bank) {
		this.bank = bank;
	}

	/**
	 * Mengembalikan penanda guru sudah memiliki lisensi kepala sekolah.
	 *
	 * <p><b>Efek samping (tulis-balik):</b> {@code null} dikembalikan sebagai {@code false},
	 * sehingga keadaan "belum diisi" berubah permanen menjadi "belum berlisensi".</p>
	 *
	 * @return {@code true} bila sudah berlisensi; {@code false} bila belum atau belum diisi
	 */
	public Boolean getSudahLisensiKepalaSekolah() {
		return sudahLisensiKepalaSekolah == null ? false : sudahLisensiKepalaSekolah;
	}

	/**
	 * Menyetel penanda lisensi kepala sekolah.
	 *
	 * @param sudahLisensiKepalaSekolah {@code true} bila sudah berlisensi
	 */
	public void setSudahLisensiKepalaSekolah(Boolean sudahLisensiKepalaSekolah) {
		this.sudahLisensiKepalaSekolah = sudahLisensiKepalaSekolah;
	}

	/**
	 * Mengembalikan penanda guru pernah mengikuti diklat kepengawasan.
	 *
	 * <p><b>Efek samping (tulis-balik):</b> {@code null} dikembalikan sebagai {@code false}.</p>
	 *
	 * @return {@code true} bila pernah; {@code false} bila belum atau belum diisi
	 */
	public Boolean getPernahDiklatKepengawasan() {
		return pernahDiklatKepengawasan == null ? false : pernahDiklatKepengawasan;
	}

	/**
	 * Menyetel penanda pernah diklat kepengawasan.
	 *
	 * @param pernahDiklatKepengawasan {@code true} bila pernah
	 */
	public void setPernahDiklatKepengawasan(Boolean pernahDiklatKepengawasan) {
		this.pernahDiklatKepengawasan = pernahDiklatKepengawasan;
	}

	/**
	 * Mengembalikan penanda guru menguasai huruf Braille.
	 *
	 * <p>Bagian dari data kompetensi pendidikan inklusif untuk pelaporan Dapodik.</p>
	 *
	 * <p><b>Efek samping (tulis-balik):</b> {@code null} dikembalikan sebagai {@code false}.</p>
	 *
	 * @return {@code true} bila menguasai; {@code false} bila tidak atau belum diisi
	 */
	public Boolean getKeahlianBraille() {
		return keahlianBraille == null ? false : keahlianBraille;
	}

	/**
	 * Menyetel penanda keahlian huruf Braille.
	 *
	 * @param keahlianBraille {@code true} bila menguasai
	 */
	public void setKeahlianBraille(Boolean keahlianBraille) {
		this.keahlianBraille = keahlianBraille;
	}

	/**
	 * Mengembalikan penanda guru menguasai bahasa isyarat.
	 *
	 * <p><b>Efek samping (tulis-balik):</b> {@code null} dikembalikan sebagai {@code false}.</p>
	 *
	 * @return {@code true} bila menguasai; {@code false} bila tidak atau belum diisi
	 */
	public Boolean getKeahlianBahasaIsyarat() {
		return keahlianBahasaIsyarat == null ? false : keahlianBahasaIsyarat;
	}

	/**
	 * Menyetel penanda keahlian bahasa isyarat.
	 *
	 * @param keahlianBahasaIsyarat {@code true} bila menguasai
	 */
	public void setKeahlianBahasaIsyarat(Boolean keahlianBahasaIsyarat) {
		this.keahlianBahasaIsyarat = keahlianBahasaIsyarat;
	}

	/**
	 * Mengembalikan NPWP guru.
	 *
	 * <p><b>Data sensitif</b>; berpasangan dengan {@link #getNamaWajibPajak()} untuk pemotongan
	 * pajak honor.</p>
	 *
	 * @return NPWP, atau {@code null}
	 */
	public String getNpwp() {
		return npwp;
	}

	/**
	 * Menyetel NPWP guru.
	 *
	 * @param npwp NPWP
	 */
	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	/**
	 * Mengembalikan nama wajib pajak, dengan nama guru sebagai cadangan.
	 *
	 * <p><b>Efek samping (tulis-balik):</b> bila kolomnya {@code null} atau kosong, yang
	 * dikembalikan adalah {@link #getNama()}. Karena Hibernate membaca kolom lewat getter ini,
	 * nama guru ikut tertulis permanen ke kolom {@code namaWajibPajak} pada flush berikutnya.
	 * Akibat lanjutannya: setelah itu kolom tersebut tidak lagi kosong, sehingga bila nama guru
	 * diperbaiki kemudian (mis. koreksi ejaan atau penambahan gelar), nama wajib pajak
	 * <b>tetap memakai nilai lama</b> dan tidak lagi ikut berubah.</p>
	 *
	 * @return nama wajib pajak; nama guru bila kolomnya kosong
	 */
	public String getNamaWajibPajak() {
		return namaWajibPajak == null || namaWajibPajak.isEmpty() ? getNama() : namaWajibPajak;
	}

	/**
	 * Menyetel nama wajib pajak.
	 *
	 * @param namaWajibPajak nama wajib pajak
	 */
	public void setNamaWajibPajak(String namaWajibPajak) {
		this.namaWajibPajak = namaWajibPajak;
	}

	/**
	 * Mengembalikan Jumlah Jam Mengajar (JJM) guru.
	 *
	 * <p>Disimpan sebagai <b>teks</b>, bukan angka — jadi tidak bisa dijumlahkan atau dibandingkan
	 * di tingkat basis data tanpa konversi, dan nilai non-angka bisa masuk lewat unggahan Excel.</p>
	 *
	 * @return JJM sebagai teks, atau {@code null}
	 */
	public String getJjm() {
		return jjm;
	}

	/**
	 * Menyetel Jumlah Jam Mengajar.
	 *
	 * @param jjm JJM sebagai teks
	 */
	public void setJjm(String jjm) {
		this.jjm = jjm;
	}

	/**
	 * Mengembalikan Total Jumlah Jam Mengajar guru.
	 *
	 * <p>Sama seperti {@link #getJjm()}, disimpan sebagai teks dan <b>tidak</b> dihitung otomatis
	 * dari jadwal mengajar mana pun — nilainya diketik manual pada biodata.</p>
	 *
	 * @return total JJM sebagai teks, atau {@code null}
	 */
	public String getTotalJjm() {
		return totalJjm;
	}

	/**
	 * Menyetel Total Jumlah Jam Mengajar.
	 *
	 * @param totalJjm total JJM sebagai teks
	 */
	public void setTotalJjm(String totalJjm) {
		this.totalJjm = totalJjm;
	}

	/**
	 * Mengembalikan isi kolom biodata berlabel "Siswa" (kolom bertipe {@code text}).
	 *
	 * <p><b>Mudah tertukar:</b> ini <b>teks bebas</b> pada biodata guru, sama sekali <b>bukan</b>
	 * relasi ke entity {@link Siswa} dan bukan daftar siswa binaan. Keterkaitan guru dengan siswa
	 * yang sesungguhnya ada pada {@code Siswa.guruPembina}/{@code Siswa.guruBk} dan pada roster
	 * kelas.</p>
	 *
	 * @return isi kolom teks "Siswa", atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getSiswa() {
		return siswa;
	}

	/**
	 * Menyetel isi kolom biodata berlabel "Siswa".
	 *
	 * @param siswa teks bebas
	 */
	public void setSiswa(String siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan uraian kompetensi guru (kolom bertipe {@code text}).
	 *
	 * @return uraian kompetensi, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKompetensi() {
		return kompetensi;
	}

	/**
	 * Menyetel uraian kompetensi guru.
	 *
	 * @param kompetensi uraian kompetensi
	 */
	public void setKompetensi(String kompetensi) {
		this.kompetensi = kompetensi;
	}

	/**
	 * Mengembalikan keterangan sertifikasi pendidik (kolom bertipe {@code text}).
	 *
	 * @return keterangan sertifikasi, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getSertifikasi() {
		return sertifikasi;
	}

	/**
	 * Menyetel keterangan sertifikasi pendidik.
	 *
	 * @param sertifikasi keterangan sertifikasi
	 */
	public void setSertifikasi(String sertifikasi) {
		this.sertifikasi = sertifikasi;
	}

	/**
	 * Mengembalikan keterangan bebas tentang guru (kolom bertipe {@code text}).
	 *
	 * <p>Ditampilkan sebagai kolom tersendiri pada grid Manajemen Guru, jadi apa pun yang diketik
	 * di sini terlihat oleh setiap pengguna yang bisa membuka daftar guru.</p>
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas tentang guru.
	 *
	 * @param keterangan keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama guru dalam aksara Arab, dengan nama biasa sebagai cadangan.
	 *
	 * <p><b>Efek samping (tulis-balik):</b> bila kolomnya {@code null}, yang dikembalikan adalah
	 * {@link #getNamaGuru()} sehingga nama Latin ikut tertulis ke kolom {@code namaAr}. Ada dua
	 * akibat lanjutan yang terlihat di UI: (a) {@code GuruAction.GuruRenderer} hanya menampilkan
	 * nama Arab bila berbeda dari nama Latin, sehingga sebelum tertulis ia tidak tampak, dan
	 * (b) setelah tertulis, koreksi nama Latin kemudian tidak lagi mengubah kolom ini.</p>
	 *
	 * <p>Perhatikan bahwa cadangan hanya berlaku untuk {@code null}, bukan untuk string kosong —
	 * berbeda dari {@link #getNamaWajibPajak()}.</p>
	 *
	 * @return nama dalam aksara Arab; nama Latin bila kolomnya {@code null}
	 */
	public String getNamaAr() {
		return namaAr == null ? getNamaGuru() : namaAr;
	}

	/**
	 * Menyetel nama guru dalam aksara Arab.
	 *
	 * @param namaAr nama dalam aksara Arab
	 */
	public void setNamaAr(String namaAr) {
		this.namaAr = namaAr;
	}

	/**
	 * Mengembalikan nama guru dalam aksara Tionghoa, dengan nama biasa sebagai cadangan.
	 *
	 * <p>Perilakunya persis sama dengan {@link #getNamaAr()}, termasuk efek tulis-baliknya.</p>
	 *
	 * @return nama dalam aksara Tionghoa; nama Latin bila kolomnya {@code null}
	 */
	public String getNamaCh() {
		return namaCh == null ? getNamaGuru() : namaCh;
	}

	/**
	 * Menyetel nama guru dalam aksara Tionghoa.
	 *
	 * @param namaCh nama dalam aksara Tionghoa
	 */
	public void setNamaCh(String namaCh) {
		this.namaCh = namaCh;
	}

	/**
	 * Mengembalikan sekolah tambahan I tempat guru juga mengajar (label UI "Juga mengajar di (I)").
	 *
	 * <p>Ikut menentukan cakupan login guru lewat {@link #ambilSekolahs()}, dan ikut dicari oleh
	 * filter "Sekolah" pada layar Manajemen Guru (yang memeriksa keempat kolom sekolah sekaligus).</p>
	 *
	 * @return sekolah tambahan I, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_1")
	public Sekolah getSekolah1() {
		sekolah1 = check(sekolah1);
		return sekolah1;
	}

	/**
	 * Menyetel sekolah tambahan I.
	 *
	 * <p><b>Perhatian:</b> berbeda dari {@link #setSekolah(Sekolah)}, setter ini <b>tidak</b>
	 * menyaring object tanpa ID. Nilainya juga memperluas cakupan login guru — lihat
	 * {@link #ambilSekolahs()}.</p>
	 *
	 * @param sekolah1 sekolah tambahan I; boleh {@code null}
	 */
	public void setSekolah1(Sekolah sekolah1) {
		this.sekolah1 = sekolah1;
	}

	/**
	 * Mengembalikan sekolah tambahan II tempat guru juga mengajar (label UI "Juga mengajar di (II)").
	 *
	 * @return sekolah tambahan II, atau {@code null}
	 * @see #getSekolah1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_2")
	public Sekolah getSekolah2() {
		sekolah2 = check(sekolah2);
		return sekolah2;
	}

	/**
	 * Menyetel sekolah tambahan II.
	 *
	 * @param sekolah2 sekolah tambahan II; boleh {@code null}
	 */
	public void setSekolah2(Sekolah sekolah2) {
		this.sekolah2 = sekolah2;
	}

	/**
	 * Mengembalikan sekolah tambahan III tempat guru juga mengajar (label UI "Juga mengajar di (III)").
	 *
	 * @return sekolah tambahan III, atau {@code null}
	 * @see #getSekolah1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_3")
	public Sekolah getSekolah3() {
		sekolah3 = check(sekolah3);
		return sekolah3;
	}

	/**
	 * Menyetel sekolah tambahan III.
	 *
	 * @param sekolah3 sekolah tambahan III; boleh {@code null}
	 */
	public void setSekolah3(Sekolah sekolah3) {
		this.sekolah3 = sekolah3;
	}

	/**
	 * Membangkitkan (bila belum ada) dan mengembalikan jalur berkas kode QR tanda tangan guru.
	 *
	 * <p>Berkas disimpan di {@code <direktori report>/ttd_gr_<id>.png}. Direktori report ditentukan
	 * {@code Common.ambilREAL_PATH_REPORT()} yang menghormati konfigurasi
	 * {@code directory_report_bersama}.</p>
	 *
	 * <p><b>Isi kode QR</b> adalah teks berbaris ganda: NUPTK (bila ada), kode guru (bila ada),
	 * nama guru, nama sekolah (bila ada), lalu alamat host instalasi. Jadi QR ini
	 * <b>bukan tanda tangan kriptografis</b> — ia hanya menuliskan ulang identitas guru dalam
	 * bentuk yang bisa dipindai, dan siapa pun bisa membuat QR serupa. Jangan memperlakukannya
	 * sebagai bukti keaslian dokumen.</p>
	 *
	 * <p><b>Efek samping:</b> menulis berkas PNG ke disk pada pemanggilan pertama; berkas yang sudah
	 * ada <b>tidak</b> pernah diperbarui, sehingga QR tetap memuat nama/sekolah lama setelah data
	 * guru berubah. Berkasnya dinamai berdasarkan ID guru yang berurutan.</p>
	 *
	 * @return jalur absolut berkas PNG kode QR
	 * @see BarcodeCommon#generateCRCode(String, File)
	 */
	public String ttdQr() {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/ttd_gr_" + getId() + ".png");
		if (!myfilebarcode.exists()) {
			String code = (getNuptk() == null || getNuptk().trim().isEmpty() ? "" : getNuptk() + "\n")
					+ (getKode() == null || getKode().trim().isEmpty() ? "" : getKode() + "\n") + getNama() + "\n"
					+ (getSekolah() == null ? "" : getSekolah().getNama() + "\n") + Common.getRequestHostWithProtocol();
			BarcodeCommon.generateCRCode(code, myfilebarcode);
		}
		return myfilebarcode.getAbsolutePath();
	}

	/**
	 * Mengisi peta parameter laporan JasperReports dengan foto, tanda tangan, dan QR guru.
	 *
	 * <p>Foto dicari lewat {@code FileFotoLain.ambil(id, FotoGuru.DEFAULT_JENIS, FotoGuru.class)}
	 * lalu diselesaikan menurut urutan berikut — jalur pertama yang cocok dipakai:</p>
	 * <ol>
	 *   <li>berkas lokal ({@code ambilFile()}) &rarr; jalur absolutnya;</li>
	 *   <li>tautan Dropbox &rarr; {@code dropboxLinkRaw()};</li>
	 *   <li>Google Drive &rarr; {@code exportGDriveUrl()};</li>
	 *   <li>ada baris fotonya tapi tanpa ketiganya &rarr; {@code createLinkUri()};</li>
	 *   <li>tidak ada sama sekali &rarr; ikon bawaan
	 *   {@code <REAL_PATH>/img/administrator-icon_default.png}.</li>
	 * </ol>
	 *
	 * <p>Nilai yang sama ditulis ke <b>tiga</b> kunci sekaligus ({@code foto}, {@code foto_guru},
	 * {@code foto_pegawai}) supaya berkas {@code .jrxml} yang dirancang untuk mahasiswa, guru,
	 * maupun pegawai sama-sama bisa memakainya tanpa diubah. Selain itu diisi {@code ttd_guru}
	 * (berkas tanda tangan dari {@link LampiranLain#TTD_GURU}, hanya bila berkasnya ada) dan
	 * {@code ttd_guru_qrcode} (hasil {@link #ttdQr()}, selalu diisi).</p>
	 *
	 * <p><b>Penanganan galat:</b> seluruh badan method dibungkus {@code try/catch} yang hanya
	 * mencetak jejak tumpukan dan mencatatnya ke {@code ErrorAuditUtil}. Artinya kegagalan bersifat
	 * <b>senyap</b> — laporan tetap tercetak, hanya saja tanpa foto/tanda tangan, dan parameter yang
	 * sudah sempat terisi sebelum galat tetap tertinggal di peta.</p>
	 *
	 * <p>Dipanggil antara lain oleh {@code LaporanCatatanGuru}, {@code LaporanPrestasiPegawai},
	 * modul perpustakaan ({@code AnggotaAction}), dan {@code Pegawai}.</p>
	 *
	 * @param parameters peta parameter laporan yang akan diisi (dimodifikasi di tempat)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			Guru guru = this;
			FileFotoLain fotoguru = FileFotoLain.ambil(guru.getId(), FotoGuru.DEFAULT_JENIS, FotoGuru.class);

			if (fotoguru != null && fotoguru.ambilFile() != null) {
				parameters.put("foto", fotoguru.ambilFile().getAbsolutePath());
				parameters.put("foto_guru", fotoguru.ambilFile().getAbsolutePath());
				parameters.put("foto_pegawai", fotoguru.ambilFile().getAbsolutePath());
			} else if (fotoguru != null && fotoguru.getLink() != null
					&& fotoguru.getLink().toLowerCase().contains("dropbox")) {
				parameters.put("foto", fotoguru.dropboxLinkRaw());
				parameters.put("foto_guru", fotoguru.dropboxLinkRaw());
				parameters.put("foto_pegawai", fotoguru.dropboxLinkRaw());
			} else if (fotoguru != null && fotoguru.getGdrive() != null) {
				parameters.put("foto", fotoguru.exportGDriveUrl());
				parameters.put("foto_guru", fotoguru.exportGDriveUrl());
				parameters.put("foto_pegawai", fotoguru.exportGDriveUrl());
			} else if (fotoguru != null) {
				parameters.put("foto", fotoguru.createLinkUri());
				parameters.put("foto_guru", fotoguru.createLinkUri());
				parameters.put("foto_pegawai", fotoguru.createLinkUri());
			} else {
				File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				parameters.put("foto", file.getAbsolutePath());
				parameters.put("foto_guru", file.getAbsolutePath());
				parameters.put("foto_pegawai", file.getAbsolutePath());
			}
			LampiranLain lampiranLain = LampiranLain.ambil(guru.getId(), LampiranLain.TTD_GURU);
			if (lampiranLain != null && lampiranLain.ambilFile() != null) {
				parameters.put("ttd_guru", lampiranLain.ambilFile().getAbsolutePath());
			}
			parameters.put("ttd_guru_qrcode", guru.ttdQr());
		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/sekolah/Guru.java:1141");
		}
	}
}
