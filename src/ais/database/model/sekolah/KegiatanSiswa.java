package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>catatan keikutsertaan satu siswa pada satu kegiatan</b>. Satu baris
 * {@code sekolah.kegiatan_siswa} menyatakan bahwa seorang {@link Siswa} tertentu mengikuti satu
 * <i>jenis kegiatan</i> ({@link KelompokKegiatanSiswa}) pada satu titik waktu, disertai sampai tiga
 * <i>pembina</i> berupa akun pengguna ({@link Tbmuser}), keterangan bebas, tahun ajaran, saklar
 * aktif, dan &mdash; bagian yang paling khas dari entity ini &mdash; <b>jawaban atas sekumpulan
 * "parameter tambahan" dinamis</b> ({@link ParameterTambahan}) yang dikonfigurasi admin per jenis
 * kegiatan dan disimpan sebagai <b>dua blob teks berdelimiter</b> pada kolom {@code nilai} dan
 * {@code nilai_inds}.
 *
 * <p>Kelas ini terdaftar resmi di {@code hibernate.cfg.xml}
 * ({@code <mapping class="ais.database.model.sekolah.KegiatanSiswa">}), dianotasi {@code @Audited}
 * (Envers menulis satu revisi tiap kali baris berubah) dan dipetakan
 * {@code dynamicInsert}/{@code dynamicUpdate}. Seluruh anotasi pemetaan berada pada
 * <b>getter</b>, artinya Hibernate memakai <i>property access</i> &mdash; fakta yang menentukan
 * hampir semua kuirk di bawah, karena setiap logika di dalam getter ikut menentukan nilai yang
 * benar-benar tersimpan.</p>
 *
 * <h2>BUKAN {@link KegiatanKesiswaan} &mdash; perbandingan terverifikasi</h2>
 *
 * <p>Nama kedua entity nyaris sama dan keduanya berada di paket {@code ais.database.model.sekolah},
 * tetapi keduanya adalah <b>subsistem yang benar-benar terpisah</b>: tidak ada satu pun relasi,
 * import, atau kolom yang menghubungkannya, dan tidak ada satu pun kelas yang memakai keduanya
 * sekaligus. Perbedaannya:</p>
 * <ul>
 *   <li><b>Tabel.</b> Kelas ini {@code sekolah.kegiatan_siswa}; {@link KegiatanKesiswaan}
 *   {@code sekolah.kegiatan_kesiswaan}.</li>
 *   <li><b>Granularitas baris.</b> Kelas ini adalah <i>fact-table</i> per (siswa, jenis kegiatan,
 *   waktu) &mdash; FK {@code siswa_id} bersifat {@code nullable = false}, jadi satu baris SELALU
 *   milik tepat satu siswa. Sebaliknya {@link KegiatanKesiswaan} adalah <b>master kegiatan
 *   konkret</b> (satu lomba/kepanitiaan/seminar) yang sama sekali tidak menyimpan siswa; pesertanya
 *   ada di entity anak {@link KegiatanKesiswaanPunyaSiswa}.</li>
 *   <li><b>Katalog pengelompokan.</b> Kelas ini memakai katalog <b>satu tingkat</b>
 *   {@link KelompokKegiatanSiswa} (di layar disebut <i>"Jenis Kegiatan"</i>), yang membawa
 *   {@code poin} dan {@code bisaDipilihSiswa}. {@link KegiatanKesiswaan} memakai katalog
 *   <b>tiga tingkat</b> {@link JenisKelompokKegiatanKesiswaan} &rarr;
 *   {@link KelompokKegiatanKesiswaan} &rarr; {@link DetailKelompokKegiatanKesiswaan}, plus katalog
 *   Jabatan dan Skala.</li>
 *   <li><b>Alur pengajuan.</b> {@link KegiatanKesiswaan} punya mesin status
 *   ({@code BELUM_DIPROSES}/{@code SEDANG_DIPROSES}/{@code DISETUJUI}/{@code DITOLAK}), nomor SK,
 *   dan sertifikat. Kelas ini <b>tidak punya status sama sekali</b> &mdash; hanya saklar
 *   {@link #getAktif() aktif} yang bisa dicentang/dilepas langsung dari daftar.</li>
 *   <li><b>Data variabel.</b> Kelas ini menyimpan jawaban parameter tambahan dinamis sebagai blob
 *   teks; {@link KegiatanKesiswaan} tidak mengenal parameter tambahan sama sekali dan memakai kolom
 *   tetap.</li>
 *   <li><b>Pembina.</b> Kelas ini menunjuk TIGA {@link Tbmuser} (akun pengguna apa pun);
 *   {@link KegiatanKesiswaan} menunjuk dua {@code Guru}.</li>
 * </ul>
 * <p>Kesimpulan: <b>bukan duplikasi skema dan bukan "klon yatim"</b>. Keduanya subsistem paralel
 * yang hidup berdampingan dan sama-sama dipakai &mdash; misalnya {@code LaporanRaporSiswa}
 * mengambil {@code KegiatanSiswa} (untuk poin kegiatan) dan {@code PrestasiSiswa} pada bagian
 * berbeda dari rapor yang sama.</p>
 *
 * <p><b>Sumber kebingungan yang nyata dan patut diwaspadai:</b> pada layar biodata siswa,
 * {@code ais.action.master.sekolah.SiswaAction#onKegiatanKesiswaan(Event)} menyisipkan
 * {@code /pages/master/sekolah/kegiatan_siswa.zul} ke dalam tab yang <b>berlabel "Kegiatan
 * Kesiswaan"</b>. Jadi tab bernama "Kegiatan Kesiswaan" itu sebenarnya menampilkan data
 * <b>kelas ini</b>, bukan {@link KegiatanKesiswaan}. Jangan menyimpulkan entity dari label tab.</p>
 *
 * <h2>Layar dan jalur masuk (terverifikasi)</h2>
 * <ul>
 *   <li><b>Layar utama.</b> {@code /pages/master/sekolah/kegiatan_siswa.zul} dengan controller
 *   {@code ais.action.master.sekolah.KegiatanSiswaAction}. Tab pertama adalah daftar/CRUD baris
 *   entity ini; tab "Rekap Data" memasang {@code DashboardRekapKegiatanSiswaData}.</li>
 *   <li><b>Tab pada biodata siswa.</b> Lihat catatan di atas ({@code SiswaAction}).</li>
 *   <li><b>Rekap/ekspor.</b>
 *   {@code ais.action.master.dashboard.sekolah.DashboardRekapKegiatanSiswaData} membangun
 *   spreadsheet dan PDF ({@code sekolah/kegiatan_siswa}) dengan kolom dinamis: satu kolom per
 *   {@link ParameterTambahan} yang dicentang pengguna.</li>
 *   <li><b>Rapor.</b> {@code ais.action.report.format1.sekolah.LaporanRaporSiswa#masukkanPoin} membaca
 *   {@code poin} dari {@link KelompokKegiatanSiswa} tiap baris dan menjumlahkannya menjadi
 *   {@code totalPointKegiatan_<idSiswa>}; pengambilannya digerbangi flag
 *   {@code JenisRaporSiswa.getAmbilKegiatanSiswa()}.</li>
 *   <li><b>Dasbor profil.</b> {@code ProfileSekolahLanjutanDashboard} menampilkan kartu "Kegiatan
 *   Siswa" berisi COUNT seluruh baris entity ini.</li>
 *   <li><b>New UI.</b> {@code WEB-INF/new/sekolah/services/kegiatan_siswa_service.jsp}
 *   mendeklarasikan {@code nuiServiceEntities = {KelompokKegiatanSiswa, KegiatanSiswa,
 *   PembinaSiswa}} dan menyertakan {@code _shared/services/dispatcher.jsp}, sehingga entity ini
 *   ikut terdaftar otomatis pada Generic CRUD v2.</li>
 * </ul>
 *
 * <h2>Struktur dua blob parameter tambahan (WAJIB dipahami sebelum menyentuh {@code nilai})</h2>
 *
 * <p>Konfigurasi parameter berada di entity {@link ParameterTambahanKegiatanSiswa} (pasangan
 * {@link KelompokKegiatanSiswa} &times; {@link ParameterTambahan} + flag wajib diisi). Form-nya
 * dibangun {@code ais.action.master.sekolah.helper.ParameterTambahanKegiatanSiswaListener}, dan
 * hasil isian ditulis balik ke entity ini lewat
 * {@link #populateParameterTambahanKegiatanSiswa(List)}. Kedua kolom teks menyimpan data yang sama
 * dengan bentuk berbeda:</p>
 * <ul>
 *   <li><b>{@code nilai}</b> &mdash; bentuk "kaya", satu baris per parameter, dipisah {@code "\n"},
 *   antarkolom dipisah {@code <=>}:
 *   <br>{@code namaKelompok->labelInputan <=> nilai <=> urlLampiran <=> nomorUrut <=> idParameter
 *   <=> idKelompok <=> indexKe <=> keterangan}.
 *   <br>Inilah satu-satunya bentuk yang dibaca {@link #ambilDataParameterTambahan()}, dan lewat
 *   method itu dipakai renderer daftar, rekap spreadsheet, dan cetak PDF.</li>
 *   <li><b>{@code nilai_inds}</b> &mdash; bentuk "indeks" yang ringkas dan stabil terhadap
 *   penggantian nama katalog:
 *   <br>{@code idKelompok->idParameter <=> nilai <=> urlLampiran <=> keterangan}.
 *   <br>Dipakai untuk memuat ulang isian ke form dan untuk evaluasi <i>syarat tampil</i>
 *   (skip-logic) lewat {@code ais.common.ParameterTambahanHtmlHelper#petaNilaiDariInds}.</li>
 * </ul>
 * <p>Konsekuensi format ini: <b>tidak ada pelarian (escaping) apa pun</b>. Nilai atau keterangan
 * yang mengandung {@code <=>} atau baris baru akan merusak pembacaan record tersebut &mdash; dan
 * karena {@code keterangan} parameter diketik bebas oleh pengguna, risikonya nyata. Selain itu
 * kolom {@code nilai} <b>menyimpan salinan nama kelompok dan label parameter</b>; mengganti nama
 * katalog TIDAK memperbarui baris lama, sehingga tampilan daftar akan terus memakai label lama
 * sementara {@code nilai_inds} tetap benar.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit ringan</b> (wajib dideklarasikan ulang, lihat catatan pewarisan) &mdash;
 *   {@link #getOleh()}/{@link #setOleh(String)}, {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *   {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b> &mdash; {@link #getId()}/{@link #setId(Long)},
 *   {@link #getNama()}/{@link #setNama(String)}, dan dua konstruktor.</li>
 *   <li><b>Relasi inti</b> &mdash; {@link #getSiswa()}/{@link #setSiswa(Siswa)},
 *   {@link #getKelompokKegiatanSiswa()}/{@link #setKelompokKegiatanSiswa(KelompokKegiatanSiswa)}.</li>
 *   <li><b>Cakupan tenant (turunan, bukan masukan)</b> &mdash;
 *   {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 *   {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Pembina</b> &mdash; {@link #getPembina1()}/{@link #setPembina1(Tbmuser)},
 *   {@link #getPembina2()}/{@link #setPembina2(Tbmuser)},
 *   {@link #getPembina3()}/{@link #setPembina3(Tbmuser)}.</li>
 *   <li><b>Atribut sederhana</b> &mdash; {@link #getWaktu()}/{@link #setWaktu(Date)},
 *   {@link #getTa()}/{@link #setTa(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)},
 *   {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 *   <li><b>Blob parameter tambahan</b> &mdash; {@link #getNilai()}/{@link #setNilai(String)},
 *   {@link #getNilaiInds()}/{@link #setNilaiInds(String)}, pembaca
 *   {@link #ambilDataParameterTambahan()}, dan penulis
 *   {@link #populateParameterTambahanKegiatanSiswa(List)}.</li>
 * </ul>
 * <p>Entity ini <b>tidak</b> memiliki {@code toString()}, {@code equals()}/{@code hashCode()},
 * maupun {@code compareTo()} sendiri &mdash; ketiadaan {@code toString()} punya akibat langsung
 * pada {@link #getNama()}, lihat kuirk 1.</p>
 *
 * <h2>Kuirk &amp; temuan (verifikasi dari kode, bukan dugaan)</h2>
 * <ol>
 *   <li><b>{@link #getNama()} adalah getter DESTRUKTIF.</b> Ia menimpa field {@code nama} tanpa
 *   syarat dengan {@code getSiswa() + "_" + getKelompokKegiatanSiswa() + "_" + getWaktu()}.
 *   Karena property access, hasil timpaan itu ikut ter-{@code flush} ke kolom
 *   {@code nama (nullable = false)}. Efeknya: (a) {@link #setNama(String)} praktis <b>mati</b>
 *   &mdash; nilai apa pun yang diset akan hilang pada pembacaan berikutnya, termasuk nilai dari
 *   konstruktor {@link #KegiatanSiswa(long, String)}; (b) isi kolom bukan nama yang bisa dibaca
 *   manusia melainkan gabungan {@code toString()} milik {@link Siswa} ({@code id-nomorInduk-namaSiswa})
 *   dan {@link KelompokKegiatanSiswa} ({@code id-nama}); (c) potongan terakhir memakai
 *   {@code Date.toString()} sehingga <b>bergantung locale dan zona waktu JVM</b> &mdash; memindah
 *   server ke zona waktu lain membuat setiap baris yang tersentuh dianggap kotor lalu ditulis
 *   ulang, dan karena {@code @Audited} setiap penulisan ulang itu melahirkan revisi Envers baru;
 *   (d) bila {@code waktu} masih NULL, {@link #getWaktu()} mengembalikan waktu <i>sekarang</i>,
 *   sehingga {@code nama} berubah pada <b>setiap</b> pembacaan.</li>
 *   <li><b>{@link #getPembina1()} mengisi sendiri pembina dari cache {@link PembinaSiswa}.</b>
 *   Bila kolomnya masih kosong dan field {@code siswa} terisi, getter menyisir seluruh cache
 *   {@code ConstantValues.ambilBerdasarClass(PembinaSiswa.class)} lalu memasang pembina siswa yang
 *   cocok &mdash; <b>tanpa {@code break}</b>, sehingga yang menang adalah kecocokan
 *   <i>terakhir</i> menurut urutan iterasi peta (tidak deterministik antar restart bila satu siswa
 *   punya lebih dari satu baris pembina), dan <b>tanpa memeriksa flag {@code aktif}</b> pada
 *   {@link PembinaSiswa}. Nilai itu ditulis ke field, jadi ikut tersimpan. Akibat praktisnya:
 *   catatan kegiatan lama yang pembinanya sengaja dikosongkan akan <b>diatribusikan surut</b>
 *   kepada siapa pun yang saat ini terdaftar sebagai pembina siswa tersebut &mdash; termasuk
 *   pembina yang sudah dinonaktifkan.</li>
 *   <li><b>Cakupan tenant DITURUNKAN dari siswa, bukan diisi pengguna.</b> {@link #getSekolah()}
 *   selalu menimpa {@code sekolah} dari {@code siswa.getSekolah()}, dan {@link #getYayasan()}
 *   menimpa {@code yayasan} dari {@code sekolah.getYayasan()}. Keduanya juga getter yang menulis
 *   balik, tetapi di sini efeknya justru <b>menguntungkan</b>: kolom {@code sekolah_id}/
 *   {@code yayasan_id} adalah cermin denormalisasi yang selalu segar, sehingga tapis tenant
 *   bersifat <i>fail-closed</i>, bukan fail-open. {@link #setSekolah(Sekolah)} dan
 *   {@link #setYayasan(Yayasan)} karena itu praktis mati juga. Harga yang dibayar: memindahkan
 *   seorang siswa ke sekolah lain akan <b>memindahkan seluruh riwayat kegiatannya</b> ke sekolah
 *   baru pada pembacaan berikutnya &mdash; riwayat di sekolah lama lenyap dari laporan sekolah
 *   lama tanpa jejak.</li>
 *   <li><b>Empat getter lain memakai nilai bawaan yang ikut tersimpan.</b> {@link #getAktif()}
 *   mengembalikan {@code true} untuk NULL, {@link #getNilai()} dan {@link #getNilaiInds()}
 *   mengembalikan {@code ""} untuk NULL, dan {@link #getWaktu()} mengembalikan waktu sekarang
 *   untuk NULL. Berbeda dari kuirk 1&ndash;3, keempatnya <b>tidak menulis ke field</b>, tetapi
 *   karena Hibernate membaca nilai lewat getter, nilai bawaan itulah yang di-{@code INSERT}/
 *   {@code UPDATE}. Kolom-kolom tersebut karena itu praktis tidak pernah benar-benar NULL di
 *   database.</li>
 *   <li><b>{@link #getTa()} mengembalikan tahun akademik BERJALAN untuk baris lama yang
 *   {@code ta}-nya NULL.</b> Ini kasus paling tajam dari kuirk 4: {@code LaporanRaporSiswa}
 *   menyaring kegiatan dengan {@code Restrictions.eq("ta", ta)} <i>langsung ke kolom</i>, jadi
 *   selama kolom masih NULL baris itu tidak pernah muncul di rapor mana pun; begitu barisnya
 *   tersentuh dalam session terbuka, ia diberi cap tahun akademik <b>saat dibuka</b> &mdash;
 *   bukan tahun kegiatan sesungguhnya &mdash; lalu tiba-tiba muncul di rapor tahun berjalan.</li>
 *   <li><b>{@link #ambilDataParameterTambahan()} menghasilkan satu entri HANTU untuk baris yang
 *   {@code nilai}-nya kosong.</b> {@code "".split("\n")} mengembalikan larik berisi satu string
 *   kosong, sehingga method tetap memproduksi satu {@link CommonVO} dengan label/nilai/URL kosong
 *   dan <b>{@code id} bawaan {@code "1"}</b>. Renderer daftar kebetulan membuangnya (nilai dan URL
 *   sama-sama kosong), tetapi {@code DashboardRekapKegiatanSiswaData} mencocokkan
 *   {@code vo.getId()} dengan {@code parameterTambahan.getId()}: bila di instalasi tersebut ada
 *   {@link ParameterTambahan} ber-id 1, entri hantu itu akan <b>cocok</b>, mengisi kolomnya dengan
 *   string kosong dan menyalakan flag {@code adaSemua} sehingga baris tanpa isian apa pun tetap
 *   ikut tercetak di rekap. Sumber lain angka {@code 1} yang sama: kegagalan
 *   {@code Long.parseLong} pada kolom ke-5 juga jatuh ke {@code 1L}.</li>
 *   <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} MENOLAK nilai kosong secara
 *   diam-diam</b> (pola seragam seluruh keluarga entity ini). Jejak "diubah oleh" tidak pernah bisa
 *   dikosongkan kembali sekali terisi; sebaliknya, bila pemanggil mengirim string kosong, jejak
 *   lama tetap dipertahankan tanpa peringatan.</li>
 *   <li><b>Blok {@code catch} yang menelan galat.</b> Dua {@code catch} di
 *   {@link #ambilDataParameterTambahan()} hanya mencatat ke {@code ErrorAuditUtil} lalu memakai
 *   nilai bawaan, dan {@link #populateParameterTambahanKegiatanSiswa(List)} membungkus SELURUH
 *   pemrosesan satu baris form dalam {@code try}/{@code catch}: bila satu baris gagal diproses,
 *   baris itu <b>hilang diam-diam</b> dari blob yang disimpan &mdash; jawaban pengguna lenyap tanpa
 *   pesan, kecuali pengguna kebetulan berperan admin.</li>
 *   <li><b>Sisa kode debug.</b> {@link #populateParameterTambahanKegiatanSiswa(List)} memanggil
 *   {@code System.out.println("ket => " + ket)} yang mencetak isian bebas pengguna ke log server
 *   pada setiap penyimpanan.</li>
 * </ol>
 *
 * <h2>Catatan otorisasi &amp; cakupan data (hasil audit, tanpa perubahan kode)</h2>
 * <ol>
 *   <li><b>Gerbang layar utama ADA.</b> {@code KegiatanSiswaAction.doBeforeCompose()} memanggil
 *   {@code Common.doCheckSecurity()}, dan tombol Tambah/Ubah/Hapus dikendalikan
 *   {@code CommonPrivilages.checkPrevilages(CREATE|UPDATE|DELETE)}. Jadi layar ini <b>bukan</b>
 *   kasus zero-gate seperti yang pernah ditemukan pada {@code KegiatanKesiswaanAction}.</li>
 *   <li><b>Pewarisan hak lewat menu induk &mdash; instance nyata.</b> {@code kegiatan_siswa.zul}
 *   menyisipkan empat layar master lain sebagai tab: {@code parameter_tambahan_kegiatan_siswa.zul},
 *   {@code kelompok_kegiatan_siswa.zul}, <b>{@code /pages/master/parameter_tambahan.zul}</b>, dan
 *   {@code pembina_siswa.zul}. Karena {@code CommonPrivilages.checkPrevilages()} menyelesaikan hak
 *   terhadap {@code Common.getCurrentMenu()} &mdash; yaitu menu HALAMAN LUAR &mdash; hak CREATE/
 *   UPDATE/DELETE pada menu "Kegiatan Siswa" otomatis berlaku pada keempat master itu, termasuk
 *   katalog <b>{@link ParameterTambahan} yang bersifat global lintas modul</b>. Penyembunyian tab
 *   untuk akun guru/siswa
 *   ({@code parameterJenisKegiatanTab.setVisible(tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null)})
 *   bersifat kosmetik: komponen di dalam tabpanel tetap ter-compose. Efek yang sama merambat ke
 *   menu <b>Siswa</b>, karena {@code SiswaAction.onKegiatanKesiswaan()} menyisipkan seluruh layar
 *   ini ke dalam biodata siswa.</li>
 *   <li><b>Tapis tenant pada Generic CRUD v2 AMAN untuk entity ini.</b> Entity punya properti
 *   Hibernate bernama persis {@code sekolah} dan {@code yayasan}, yang termasuk enam nama institusi
 *   yang selalu dipasang {@code GenericCrudAutoEntityAdapter.scopeBindings()} tanpa syarat peran
 *   pengguna. Dikombinasikan dengan kuirk 3 (kolom tenant selalu disegarkan dari siswa), entity ini
 *   <b>tidak</b> termasuk populasi rentan pola whitelist nama-properti.</li>
 *   <li><b>Tidak ditemukan SQL injection lewat nama katalog.</b> Seluruh pembacaan entity ini
 *   ({@code KegiatanSiswaAction.initCriteria()},
 *   {@code DashboardRekapKegiatanSiswaData.buatCriteria()}, {@code LaporanRaporSiswa}) memakai
 *   Criteria API dengan parameter terikat; tidak ada {@code createSQLQuery} dan tidak ada nama baris
 *   katalog yang dirangkai menjadi alias kolom. Pola yang pernah ditemukan pada dasbor sejenis
 *   TIDAK berlaku di sini.</li>
 *   <li><b>Tapis tenant layar utama tidak lengkap secara bawaan.</b>
 *   {@code KegiatanSiswaAction.initCriteria()} hanya menambahkan pembatas sekolah/yayasan ketika
 *   pengguna benar-benar memilih salah satu di combobox; pilihan bawaan "Semua" menghasilkan
 *   {@code Restrictions.sqlRestriction("1=1")}. Pembatasan sesungguhnya karena itu bergantung
 *   sepenuhnya pada isi combobox yang diisi
 *   {@code Common.initYayasanDanSekolahDanSemua(...)}, bukan pada kueri ini.</li>
 * </ol>
 *
 * <h2>Catatan pewarisan {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti apa pun miliknya. Karena itu
 * deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, dan
 * {@code onUpdate()} di kelas ini <b>bukan duplikasi yang salah, melainkan keharusan teknis</b>:
 * tanpa deklarasi ulang, kolom-kolom itu tidak akan ada di tabel. Yang benar-benar diwarisi dan
 * dipakai di sini adalah utilitas statis {@link GeneralValueObject#check(Object)}, yang meresolusi
 * proxy lazy secara senyap dan tidak pernah melempar exception.</p>
 *
 * @see GeneralValueObject
 * @see KelompokKegiatanSiswa
 * @see ParameterTambahanKegiatanSiswa
 * @see PembinaSiswa
 * @see KegiatanKesiswaan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "kegiatan_siswa", schema = "sekolah")
public class KegiatanSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan sejak kelas dibuat; jangan diubah agar sesi ZK
	 * lama dan cache yang sudah ter-serialisasi tetap dapat dibaca.
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Kunci utama, dibangkitkan database ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> sehingga jejak lama tetap dipertahankan &mdash; pola seragam
	 * seluruh keluarga entity ini.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau string kosong/spasi <b>diabaikan diam-diam</b>.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pembaruan stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum
	 * {@code UPDATE} dieksekusi.
	 *
	 * <p><b>Efek samping:</b> mengubah field {@link #getTanggal_dirubah() tanggal_dirubah} milik
	 * instance ini. Dipanggil oleh Hibernate, bukan oleh kode aplikasi. Perhatikan bahwa setiap
	 * penulisan yang dipicu getter destruktif (kuirk 1&ndash;3 pada Javadoc kelas) juga memicu
	 * callback ini, sehingga stempel "terakhir diubah" bisa bergerak tanpa ada perubahan data yang
	 * disengaja pengguna.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris yang sama dengan method
	 * ini (bentuk asli hasil penyisipan otomatis lintas entity); jangan dipisah tanpa alasan agar
	 * tetap seragam dengan entity lain.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir. Nilai awalnya diisi
	 * {@code WaktuUtil.getDate()} saat instance dibuat, lalu diperbarui {@link #onUpdate()}.
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Cermin denormalisasi sekolah pemilik; SELALU diturunkan ulang dari {@code siswa} oleh {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Cermin denormalisasi yayasan pemilik; SELALU diturunkan ulang dari {@code sekolah} oleh {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Siswa pemilik catatan kegiatan ini; FK {@code siswa_id} bersifat {@code nullable = false}. */
	private Siswa siswa;
	/** Jenis kegiatan (katalog satu tingkat) yang diikuti; FK {@code kelompok_kegiatan_siswa} {@code nullable = false}. */
	private KelompokKegiatanSiswa kelompokKegiatanSiswa;
	/** Blob "kaya" jawaban parameter tambahan; format dijelaskan pada Javadoc kelas. */
	private String nilai;
	/** Blob "indeks" jawaban parameter tambahan; format dijelaskan pada Javadoc kelas. */
	private String nilaiInds;
	/** Tanggal dan jam kegiatan berlangsung. */
	private Date waktu;
	/** Keterangan bebas yang diketik petugas pada formulir. */
	private String keterangan;
	/** Label turunan; nilainya SELALU ditimpa {@link #getNama()} &mdash; lihat kuirk 1. */
	private String nama;
	/** Pembina utama (wajib diisi di layar); dapat terisi otomatis dari {@link PembinaSiswa}. */
	private Tbmuser pembina1;
	/** Pembina kedua (opsional). */
	private Tbmuser pembina2;
	/** Pembina ketiga (opsional). */
	private Tbmuser pembina3;
	/** Saklar aktif; dapat dicentang/dilepas langsung dari baris daftar. */
	private Boolean aktif;
	/** Tahun ajaran; bila NULL, {@link #getTa()} mengembalikan tahun akademik berjalan. */
	private String ta;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate, sekaligus dipakai
	 * {@code KegiatanSiswaAction.onAdd(Event)} untuk membuka formulir kosong.
	 *
	 * <p>Seluruh field tetap {@code null} kecuali {@code tanggal_dirubah}; nilai bawaan
	 * ({@code aktif}, {@code waktu}, {@code ta}, {@code nilai}, {@code nilaiInds}) baru terbentuk
	 * saat getter masing-masing dipanggil, dan karena property access ikut tersimpan pada flush
	 * berikutnya.</p>
	 */
	public KegiatanSiswa() {
	}

	/**
	 * Konstruktor ringkas bawaan hbm2java.
	 *
	 * <p><b>Perhatian:</b> argumen {@code nama} praktis tidak berguna &mdash; {@link #getNama()}
	 * menimpa field {@code nama} tanpa syarat pada pembacaan pertama (kuirk 1 pada Javadoc kelas),
	 * jadi nilai yang dikirim ke sini tidak akan pernah terbaca kembali maupun tersimpan.
	 * Konstruktor ini tidak dipakai di kode produksi mana pun saat ini.</p>
	 *
	 * @param id   nilai kunci utama yang dipasang langsung (tanpa validasi)
	 * @param nama label awal; segera ditimpa oleh {@link #getNama()}
	 */
	public KegiatanSiswa(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipakai antara lain sebagai kunci {@code ref} pada {@link LampiranLain} untuk lampiran
	 * parameter tambahan (lihat {@link #populateParameterTambahanKegiatanSiswa(List)}), sehingga
	 * lampiran hanya bisa ditautkan setelah baris tersimpan dan memperoleh id.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama secara manual.
	 *
	 * @param id nilai kunci utama; normalnya diisi Hibernate, bukan kode aplikasi
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan siswa pemilik catatan kegiatan ini, setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Efek samping:</b> hasil resolusi ditulis balik ke field {@code siswa} (bisa berupa
	 * instance yang berbeda dari proxy semula). Resolusi bersifat senyap: bila seluruh tahap
	 * {@code check()} gagal, proxy dikembalikan apa adanya.</p>
	 *
	 * <p>Getter ini adalah pintu bagi seluruh turunan tenant: {@link #getSekolah()} dan
	 * {@link #getYayasan()} memanggilnya lebih dulu.</p>
	 *
	 * @return siswa pemilik; secara skema tidak boleh {@code null} ({@code nullable = false})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return this.siswa;
	}

	/**
	 * Menyetel siswa pemilik catatan. Dipanggil {@code KegiatanSiswaAction.onSave(Event)} dari
	 * bandbox "Pilih Siswa" (wajib diisi; penyimpanan ditolak bila kosong).
	 *
	 * <p>Perhatikan bahwa mengganti siswa di sini juga <b>memindahkan cakupan tenant</b> baris ini
	 * pada pembacaan berikutnya, karena {@link #getSekolah()}/{@link #getYayasan()} diturunkan dari
	 * siswa.</p>
	 *
	 * @param siswa siswa pemilik catatan kegiatan
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini, <b>selalu diturunkan ulang</b> dari
	 * {@code getSiswa().getSekolah()}.
	 *
	 * <p><b>Efek samping (getter yang menulis balik):</b> field {@code siswa} dan {@code sekolah}
	 * ditimpa. Karena Hibernate memakai property access, nilai hasil turunan itulah yang tersimpan
	 * ke kolom {@code sekolah_id} pada flush berikutnya. Konsekuensinya kolom tenant adalah cermin
	 * denormalisasi yang selalu segar &mdash; menguntungkan untuk penyaringan (fail-closed), tetapi
	 * membuat riwayat kegiatan ikut berpindah ketika siswa dimutasi ke sekolah lain.</p>
	 *
	 * <p>Nilai apa pun yang dipasang lewat {@link #setSekolah(Sekolah)} akan tergantikan di sini,
	 * kecuali bila {@code siswa} bernilai {@code null} (secara skema tidak mungkin untuk baris yang
	 * tersimpan).</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila siswa maupun field {@code sekolah} kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		siswa = getSiswa();
		if (siswa != null) {
			sekolah = siswa.getSekolah();
		}
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik; objek yang {@code null} atau ber-{@code id} {@code null} disimpan
	 * sebagai {@code null} (pola "Semua" pada combobox layar).
	 *
	 * <p><b>Praktis tidak berpengaruh:</b> {@link #getSekolah()} menurunkan ulang nilai ini dari
	 * siswa pada pembacaan berikutnya. Setter dipertahankan karena dibutuhkan Hibernate saat
	 * memuat baris dari database.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau tanpa id akan dinormalkan menjadi {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini, <b>selalu diturunkan ulang</b> dari
	 * {@code getSekolah().getYayasan()}.
	 *
	 * <p><b>Efek samping (getter yang menulis balik):</b> memanggil {@link #getSekolah()} &mdash;
	 * sehingga ikut menimpa field {@code siswa} dan {@code sekolah} &mdash; lalu menimpa field
	 * {@code yayasan}. Nilai turunan itulah yang tersimpan ke kolom {@code yayasan_id}.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila rantai siswa &rarr; sekolah &rarr; yayasan terputus
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
	 * Menyetel yayasan pemilik; objek yang {@code null} atau ber-{@code id} {@code null} disimpan
	 * sebagai {@code null}.
	 *
	 * <p><b>Praktis tidak berpengaruh</b> dengan alasan yang sama seperti
	 * {@link #setSekolah(Sekolah)}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau tanpa id akan dinormalkan menjadi {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas kegiatan (diketik petugas pada kotak "Keterangan").
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas kegiatan. Dipanggil dari
	 * {@code KegiatanSiswaAction.onSave(Event)}.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan label turunan baris ini, <b>setelah menimpa field {@code nama} tanpa
	 * syarat</b> dengan gabungan {@code toString()} siswa, {@code toString()} jenis kegiatan, dan
	 * {@code toString()} waktu.
	 *
	 * <p><b>Getter destruktif &mdash; baca kuirk 1 pada Javadoc kelas sebelum mengubah apa pun.</b>
	 * Bentuk hasilnya kira-kira
	 * {@code "<idSiswa>-<nis>-<namaSiswa>_<idKelompok>-<namaKelompok>_<Date.toString()>"}. Karena
	 * kolom {@code nama} dipetakan {@code nullable = false} dan Hibernate membaca nilainya lewat
	 * getter ini, hasil timpaan tersebut yang benar-benar tersimpan. Potongan terakhir memakai
	 * {@code Date.toString()} sehingga bergantung locale/zona waktu JVM, dan bila {@code waktu}
	 * masih {@code null} maka {@link #getWaktu()} memasok waktu <i>sekarang</i> sehingga hasilnya
	 * berbeda pada setiap pembacaan.</p>
	 *
	 * <p><b>Efek samping berantai:</b> memanggil {@link #getSiswa()},
	 * {@link #getKelompokKegiatanSiswa()}, dan {@link #getWaktu()} &mdash; jadi ikut memicu
	 * resolusi proxy lazy pada dua relasi tersebut.</p>
	 *
	 * @return label turunan yang baru saja dihitung
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		nama = getSiswa() + "_" + getKelompokKegiatanSiswa() + "_" + getWaktu();
		return this.nama;
	}

	/**
	 * Menyetel label baris ini.
	 *
	 * <p><b>Praktis mati:</b> nilai apa pun yang diset akan ditimpa {@link #getNama()} pada
	 * pembacaan berikutnya. Setter dipertahankan karena dibutuhkan Hibernate saat memuat baris dari
	 * database.</p>
	 *
	 * @param nama label yang ingin dipasang; akan tergantikan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan saklar aktif baris ini, dengan bawaan {@code true} bila kolom masih
	 * {@code null}.
	 *
	 * <p>Karena property access, bawaan {@code true} itulah yang tersimpan; baris lama yang
	 * kolomnya NULL akan otomatis dianggap aktif. Saklar ini dapat diubah langsung dari daftar:
	 * {@code KegiatanSiswaAction} memasang {@code onCheck} yang memanggil
	 * {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan seketika tanpa dialog
	 * konfirmasi.</p>
	 *
	 * @return {@code true} bila kegiatan dianggap aktif
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel saklar aktif baris ini.
	 *
	 * @param aktif nilai saklar; {@code null} akan dibaca kembali sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan jenis kegiatan (katalog {@link KelompokKegiatanSiswa}) yang diikuti, setelah
	 * proxy lazy diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Efek samping:</b> hasil resolusi ditulis balik ke field {@code kelompokKegiatanSiswa}.</p>
	 *
	 * <p>Objek yang dikembalikan membawa {@code poin} yang dipakai
	 * {@code LaporanRaporSiswa.masukkanPoin(...)} untuk menghitung total poin kegiatan siswa, dan
	 * {@code nama}-nya ikut disalin ke dalam blob {@code nilai} oleh
	 * {@link #populateParameterTambahanKegiatanSiswa(List)}.</p>
	 *
	 * @return jenis kegiatan; secara skema tidak boleh {@code null} ({@code nullable = false})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_siswa", nullable = false)
	public KelompokKegiatanSiswa getKelompokKegiatanSiswa() {
		kelompokKegiatanSiswa = check(kelompokKegiatanSiswa);
		return kelompokKegiatanSiswa;
	}

	/**
	 * Menyetel jenis kegiatan yang diikuti. Dipanggil {@code KegiatanSiswaAction.onSave(Event)}
	 * dari combobox "Jenis Kegiatan" (wajib dipilih; penyimpanan ditolak bila kosong).
	 *
	 * <p>Mengganti jenis kegiatan juga mengganti <b>himpunan parameter tambahan</b> yang berlaku
	 * (lihat {@link ParameterTambahanKegiatanSiswa}); form dibangun ulang oleh
	 * {@code ParameterTambahanKegiatanSiswaListener} pada event {@code onChange} combobox.</p>
	 *
	 * @param kelompokKegiatanSiswa jenis kegiatan dari katalog
	 */
	public void setKelompokKegiatanSiswa(KelompokKegiatanSiswa kelompokKegiatanSiswa) {
		this.kelompokKegiatanSiswa = kelompokKegiatanSiswa;
	}

	/**
	 * Mengembalikan tanggal dan jam kegiatan; bila kolom masih {@code null}, mengembalikan waktu
	 * <b>sekarang</b> ({@code WaktuUtil.getDate()}).
	 *
	 * <p>Berbeda dari {@link #getNama()}, getter ini <b>tidak</b> menulis ke field. Namun karena
	 * Hibernate membaca nilai lewat getter, waktu sekarang itulah yang di-{@code INSERT}/
	 * {@code UPDATE}, sehingga kolom praktis tidak pernah NULL setelah baris tersentuh. Selama
	 * kolom masih NULL, nilai kembalian berubah pada setiap panggilan &mdash; itulah yang membuat
	 * {@link #getNama()} tidak stabil untuk baris yang belum punya waktu.</p>
	 *
	 * @return waktu kegiatan, atau waktu sekarang bila belum diisi (tidak pernah {@code null})
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menyetel tanggal dan jam kegiatan. Di layar, kotak tanggal bersifat {@code readonly} sehingga
	 * nilainya hanya bisa dipilih lewat pemilih tanggal.
	 *
	 * @param waktu waktu kegiatan; {@code null} akan dibaca kembali sebagai waktu sekarang
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan blob "kaya" jawaban parameter tambahan, dengan bawaan {@code ""} bila kolom
	 * masih {@code null}.
	 *
	 * <p>Formatnya (satu baris per parameter, dipisah {@code "\n"}, antarkolom {@code <=>})
	 * dijelaskan lengkap pada Javadoc kelas. Pembacaan terstruktur sebaiknya <b>tidak</b> dilakukan
	 * dengan mem-parse string ini sendiri &mdash; gunakan {@link #ambilDataParameterTambahan()}.</p>
	 *
	 * @return isi blob, atau {@code ""} bila belum pernah diisi (tidak pernah {@code null})
	 */
	@Column(name = "nilai", columnDefinition = "text")
	public String getNilai() {
		return nilai == null ? "" : nilai;
	}

	/**
	 * Menyetel blob "kaya" jawaban parameter tambahan.
	 *
	 * <p>Normalnya hanya dipanggil dari {@link #populateParameterTambahanKegiatanSiswa(List)}.
	 * Memanggilnya langsung dengan string yang tidak mengikuti format akan merusak seluruh
	 * pembacaan hilir (daftar, rekap spreadsheet, cetak PDF).</p>
	 *
	 * @param nilai blob berformat delimiter; boleh {@code null} (dibaca kembali sebagai {@code ""})
	 */
	public void setNilai(String nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mem-parse blob {@link #getNilai()} menjadi daftar {@link CommonVO} yang siap ditampilkan,
	 * lalu mengurutkannya.
	 *
	 * <p>Pemetaan kolom blob ke properti {@link CommonVO}:</p>
	 * <ul>
	 *   <li>kolom 0 &rarr; {@code name} &mdash; label gabungan {@code "namaKelompok->labelInputan"};</li>
	 *   <li>kolom 1 &rarr; {@code name1} &mdash; nilai jawaban;</li>
	 *   <li>kolom 2 &rarr; {@code name2} &mdash; URL lampiran (kosong bila tidak ada);</li>
	 *   <li>kolom 3 &rarr; {@code nomorUrut} &mdash; urutan tampil, bawaan {@code 1} bila gagal di-parse;</li>
	 *   <li>kolom 4 &rarr; {@code id} &mdash; id {@link ParameterTambahan} sebagai string, bawaan
	 *   {@code "1"} bila gagal di-parse.</li>
	 * </ul>
	 * <p>Kolom 5&ndash;7 blob ({@code idKelompok}, {@code indexKe}, {@code keterangan}) sengaja
	 * <b>tidak</b> dipetakan dan karenanya tidak pernah terbaca oleh pemanggil mana pun.
	 * Pengurutan akhir memakai {@code CommonVO.compareTo}, yaitu berdasarkan {@code nomorUrut} lalu
	 * {@code id}.</p>
	 *
	 * <p><b>Perhatian (kuirk 6 pada Javadoc kelas):</b> untuk baris yang {@code nilai}-nya kosong,
	 * {@code "".split("\n")} tetap menghasilkan satu elemen kosong sehingga method ini mengembalikan
	 * <b>satu entri hantu</b> ber-{@code id} {@code "1"} dengan seluruh teks kosong. Pemanggil yang
	 * mencocokkan {@code id} ke id parameter (mis. rekap spreadsheet dan cetak PDF) bisa salah
	 * mengira parameter ber-id 1 "ada isinya".</p>
	 *
	 * <p><b>Kegagalan senyap:</b> dua blok {@code catch} di dalamnya hanya mencatat ke
	 * {@code ErrorAuditUtil} lalu memakai nilai bawaan; angka yang tidak valid tidak pernah
	 * dilaporkan ke pengguna.</p>
	 *
	 * <p>Method ini <b>murni membaca</b> &mdash; tidak mengubah state entity dan tidak menyentuh
	 * database. Pemanggil terverifikasi: {@code KegiatanSiswaAction.KegiatanSiswaRenderer#render},
	 * {@code DashboardRekapKegiatanSiswaData#cetak()}, dan
	 * {@code DashboardRekapKegiatanSiswaData#initSpreadsheet()}.</p>
	 *
	 * @return daftar {@link CommonVO} terurut; tidak pernah {@code null}, dan berisi minimal satu
	 *         elemen (lihat catatan entri hantu di atas)
	 */
	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getNilai().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KegiatanSiswa.java:230");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KegiatanSiswa.java:236");

			}
			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Menyusun ulang <b>kedua</b> blob parameter tambahan ({@code nilai} dan {@code nilai_inds})
	 * dari baris-baris form ZK yang sedang tampil, lalu menuliskannya ke entity ini.
	 *
	 * <p>Setiap {@link Row} yang diproses diharapkan membawa tiga atribut yang dipasang
	 * {@code ParameterTambahanKegiatanSiswaListener}: {@code "parameterTambahan"}
	 * ({@link ParameterTambahan}), {@code "kelompokKegiatanSiswa"}
	 * ({@link KelompokKegiatanSiswa}), dan opsional {@code "indexKe"} ({@link Long}). Baris yang
	 * salah satu dari dua atribut pertamanya {@code null} dilewati diam-diam. Nilai jawaban diambil
	 * lewat {@code ParameterTambahan.ambilVal(row, parameterTambahan)}, dan keterangan diambil dari
	 * atribut {@code "keterangan"} bila &mdash; dan hanya bila &mdash; atribut itu benar-benar
	 * bertipe {@link Textbox}.</p>
	 *
	 * <p>Untuk parameter yang {@code getHarusMenyertakanLampiran()}-nya {@code true}, method
	 * mencari {@link LampiranLain} dengan {@code ref = getId()} dan {@code jenis =
	 * "idKelompok->idParameter"}, lalu menyimpan {@code createLinkUri()}-nya ke kolom URL.
	 * <b>Implikasi urutan:</b> pencarian memakai {@link #getId()}, jadi pada penyimpanan baris
	 * BARU lampiran belum tertaut saat method ini dipanggil &mdash;
	 * {@code KegiatanSiswaAction.onSave(Event)} memang lebih dulu menyimpan entity dan memperbarui
	 * {@code ref} setiap lampiran, baru kemudian memanggil listener yang meneruskan ke method
	 * ini.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #setNilaiInds(String)} lalu {@link #setNilai(String)}
	 * &mdash; keduanya menimpa isi lama secara total, sehingga baris form yang tidak ikut dikirim
	 * akan hilang dari blob. Method juga membuka pembacaan {@link LampiranLain} (akses database)
	 * dan mencetak {@code System.out.println("ket => " + ket)}, sisa kode debug yang membocorkan
	 * isian bebas pengguna ke log server.</p>
	 *
	 * <p><b>Kegagalan senyap:</b> seluruh pemrosesan satu baris dibungkus {@code try}/{@code catch}
	 * yang hanya memanggil {@code Common.tampilErrorJikaAdmin(e)}. Bagi pengguna non-admin, satu
	 * baris yang gagal diproses akan <b>lenyap dari data tersimpan tanpa pesan apa pun</b>.</p>
	 *
	 * <p>Dipanggil dari {@code ParameterTambahanKegiatanSiswaListener#onSave(KegiatanSiswa)} (saat
	 * tombol Simpan ditekan) dan dari listener {@code isi} di dalam
	 * {@code ParameterTambahanKegiatanSiswaListener#displayRinci(...)} (setiap kali sebuah isian
	 * berubah, agar syarat tampil/skip-logic dapat dievaluasi ulang).</p>
	 *
	 * @param parameterRows daftar baris form parameter tambahan yang sedang tampil; bila
	 *                      {@code null} atau kosong, method langsung kembali <b>tanpa mengosongkan
	 *                      blob yang sudah ada</b>
	 */
	public void populateParameterTambahanKegiatanSiswa(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokKegiatanSiswa kelompokKegiatanSiswa = (KelompokKegiatanSiswa) row
						.getAttribute("kelompokKegiatanSiswa");
				Long indexKe = (Long) row.getAttribute("indexKe");
				if (parameterTambahan != null && kelompokKegiatanSiswa != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(KegiatanSiswa.class, getId(),
							kelompokKegiatanSiswa.getId() + "->" + parameterTambahan.getId());

					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null && row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);
					String ket = keterangan == null ? "" : keterangan.getValue().trim();
					String val = ParameterTambahan.ambilVal(row, parameterTambahan);

					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					System.out.println("ket => " + ket);

					String s = kelompokKegiatanSiswa.getNama() + "->" + parameterTambahan.getLabelInputan() + "<=>"
							+ val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokKegiatanSiswa.getId() + "<=>"
							+ (indexKe == null ? 0 : indexKe) + "<=>" + ket;

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokKegiatanSiswa.getId() + "->" + parameterTambahan.getId() + "<=>" + val + "<=>"
							+ url + "<=>" + ket;
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		setNilaiInds(parameterTambahanInds);
		setNilai(parameterTambahanStr);
	}

	/**
	 * Mengembalikan blob "indeks" jawaban parameter tambahan, dengan bawaan {@code ""} bila kolom
	 * masih {@code null}.
	 *
	 * <p>Formatnya {@code idKelompok->idParameter <=> nilai <=> urlLampiran <=> keterangan} per
	 * baris. Blob inilah yang dipakai untuk <b>memuat ulang isian ke form</b> dan untuk evaluasi
	 * <i>syarat tampil</i> (skip-logic) lewat
	 * {@code ais.common.ParameterTambahanHtmlHelper#petaNilaiDariInds(String)}; karena berkunci id
	 * (bukan nama), isinya tetap benar walau nama katalog diubah.</p>
	 *
	 * @return isi blob indeks, atau {@code ""} bila belum pernah diisi (tidak pernah {@code null})
	 */
	@Column(name = "nilai_inds", columnDefinition = "text")
	public String getNilaiInds() {
		return nilaiInds == null ? "" : nilaiInds;
	}

	/**
	 * Menyetel blob "indeks" jawaban parameter tambahan.
	 *
	 * <p>Normalnya hanya dipanggil dari {@link #populateParameterTambahanKegiatanSiswa(List)}.
	 * Isinya harus tetap sinkron dengan {@link #getNilai()}; menyetel salah satu saja membuat form
	 * dan tampilan daftar menunjukkan data yang berbeda.</p>
	 *
	 * @param nilaiInds blob indeks berformat delimiter; boleh {@code null} (dibaca kembali sebagai {@code ""})
	 */
	public void setNilaiInds(String nilaiInds) {
		this.nilaiInds = nilaiInds;
	}

	/**
	 * Mengembalikan pembina utama kegiatan, dan &mdash; bila kolomnya masih kosong &mdash;
	 * <b>mengisinya otomatis</b> dari pemetaan {@link PembinaSiswa} milik siswa terkait.
	 *
	 * <p>Alurnya: proxy diresolusi lewat {@link GeneralValueObject#check(Object)}; bila hasilnya
	 * {@code null} dan <b>field</b> {@code siswa} (bukan {@link #getSiswa()}) terisi, seluruh cache
	 * memori {@code ConstantValues.ambilBerdasarClass(PembinaSiswa.class)} disisir untuk mencari
	 * baris yang siswanya cocok, lalu {@code pembinaSiswa.getPembina()} dipasang ke field
	 * {@code pembina1}.</p>
	 *
	 * <p><b>Efek samping (getter yang menulis balik):</b> nilai hasil pencarian disimpan ke field,
	 * dan karena property access ikut ter-{@code flush} ke kolom {@code pembina1}. Catatan kegiatan
	 * lama yang pembinanya sengaja dikosongkan karena itu akan <b>diatribusikan surut</b> kepada
	 * pembina yang terdaftar saat baris dibuka.</p>
	 *
	 * <p><b>Tiga kuirk yang perlu diketahui.</b> (1) Penyisiran tidak memakai {@code break},
	 * sehingga bila satu siswa punya lebih dari satu baris {@link PembinaSiswa} yang menang adalah
	 * kecocokan <i>terakhir</i> menurut urutan iterasi peta &mdash; tidak deterministik antar
	 * restart. (2) Flag {@code aktif} pada {@link PembinaSiswa} <b>tidak diperiksa</b>, jadi pembina
	 * yang sudah dinonaktifkan tetap terpasang. (3) Syaratnya memakai field {@code siswa} mentah;
	 * bila field itu belum terisi (mis. entity baru yang siswanya belum diset), pengisian otomatis
	 * dilewati tanpa jejak. Sumber datanya adalah cache memori JVM, sehingga baris
	 * {@link PembinaSiswa} yang baru ditambahkan mungkin belum terlihat sampai cache disegarkan.</p>
	 *
	 * <p>Perilaku yang setara diulang di sisi UI oleh {@code KegiatanSiswaAction}, yang memasang
	 * sekaligus <b>mengunci</b> bandbox "Pembina Utama" begitu siswa dipilih.</p>
	 *
	 * @return pembina utama, atau {@code null} bila kolom kosong dan tidak ada pemetaan
	 *         {@link PembinaSiswa} yang cocok
	 */
	@SuppressWarnings("unchecked")
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembina1", nullable = true)
	public Tbmuser getPembina1() {
		pembina1 = check(pembina1);

		if (pembina1 == null && siswa != null) {
			Map<Long, PembinaSiswa> p = ConstantValues.ambilBerdasarClass(PembinaSiswa.class);
			for (PembinaSiswa pembinaSiswa2 : p.values()) {
				if (pembinaSiswa2 != null && pembinaSiswa2.getSiswa() != null
						&& pembinaSiswa2.getSiswa().getId().equals(siswa.getId())) {
					pembina1 = pembinaSiswa2.getPembina();
				}
			}
		}

		return pembina1;
	}

	/**
	 * Menyetel pembina utama kegiatan. Wajib terisi di layar &mdash;
	 * {@code KegiatanSiswaAction.onSave(Event)} menolak penyimpanan bila bandbox "Pembina Utama"
	 * kosong.
	 *
	 * <p>Menyetel {@code null} tidak permanen: {@link #getPembina1()} akan mencoba mengisinya lagi
	 * dari pemetaan {@link PembinaSiswa} pada pembacaan berikutnya.</p>
	 *
	 * @param pembina1 akun pengguna pembina utama; boleh {@code null}
	 */
	public void setPembina1(Tbmuser pembina1) {
		this.pembina1 = pembina1;
	}

	/**
	 * Mengembalikan pembina kedua (opsional), setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Berbeda dari {@link #getPembina1()}, getter ini <b>tidak</b> punya pengisian otomatis:
	 * kolom yang kosong tetap kosong. Satu-satunya efek sampingnya adalah penulisan balik hasil
	 * resolusi proxy ke field.</p>
	 *
	 * @return pembina kedua, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembina2", nullable = true)
	public Tbmuser getPembina2() {
		pembina2 = check(pembina2);
		return pembina2;
	}

	/**
	 * Menyetel pembina kedua (opsional).
	 *
	 * @param pembina2 akun pengguna pembina kedua; boleh {@code null}
	 */
	public void setPembina2(Tbmuser pembina2) {
		this.pembina2 = pembina2;
	}

	/**
	 * Mengembalikan pembina ketiga (opsional), setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Sama seperti {@link #getPembina2()}: tanpa pengisian otomatis, hanya penulisan balik hasil
	 * resolusi proxy ke field.</p>
	 *
	 * @return pembina ketiga, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembina3", nullable = true)
	public Tbmuser getPembina3() {
		pembina3 = check(pembina3);
		return pembina3;
	}

	/**
	 * Menyetel pembina ketiga (opsional).
	 *
	 * @param pembina3 akun pengguna pembina ketiga; boleh {@code null}
	 */
	public void setPembina3(Tbmuser pembina3) {
		this.pembina3 = pembina3;
	}

	/**
	 * Mengembalikan tahun ajaran kegiatan; bila kolom masih {@code null}, mengembalikan
	 * <b>tahun akademik yang sedang berjalan</b> ({@code Common.getCurrentTahunAkademik()}).
	 *
	 * <p>Getter ini tidak menulis ke field, tetapi karena property access nilai bawaan itulah yang
	 * tersimpan begitu baris tersentuh dalam session terbuka. Akibatnya nyata dan mudah terlewat:
	 * {@code LaporanRaporSiswa} menyaring kegiatan dengan {@code Restrictions.eq("ta", ta)}
	 * langsung ke kolom, sehingga baris ber-{@code ta} NULL tidak pernah muncul di rapor mana pun
	 * &mdash; sampai baris itu tersentuh, lalu ia mendapat cap tahun akademik <b>saat dibuka</b>
	 * (bukan tahun kegiatan sesungguhnya) dan mulai muncul di rapor tahun berjalan.</p>
	 *
	 * <p>Perhatikan juga bahwa kolom ini <b>tidak</b> dianotasi {@code @Column}, jadi nama
	 * kolomnya mengikuti bawaan Hibernate ({@code ta}).</p>
	 *
	 * @return tahun ajaran kegiatan, atau tahun akademik berjalan bila belum diisi
	 */
	public String getTa() {
		return ta == null ? Common.getCurrentTahunAkademik() : ta;
	}

	/**
	 * Menyetel tahun ajaran kegiatan. Dipanggil {@code KegiatanSiswaAction.onSave(Event)} dari
	 * combobox "Tahun Ajaran" yang diisi {@code Common.generateTahunAjaran(...)}.
	 *
	 * @param ta tahun ajaran (mis. {@code "2025/2026"}); {@code null} akan dibaca kembali sebagai
	 *           tahun akademik berjalan
	 */
	public void setTa(String ta) {
		this.ta = ta;
	}

}
