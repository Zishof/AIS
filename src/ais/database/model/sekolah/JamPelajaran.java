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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.ui.util.WaktuUtil;

/**
 * Master <b>slot jam pelajaran</b> pada modul sekolah — tabel {@code sekolah.jam_pelajaran}.
 *
 * <h3>Peran domain (TERVERIFIKASI)</h3>
 * <p>Satu baris entity ini adalah <b>satu potongan waktu bernama</b> pada hari sekolah:
 * sebuah nama ("Jam ke-1"), sepasang jam dinding ({@code mulai} 07:30 – {@code selesai} 08:19),
 * dan sebuah bobot beban mengajar ({@code jp}, biasanya 1,0). Baris itu milik satu
 * {@link Sekolah}, <b>wajib</b> dikategorikan oleh satu {@link JenisJadwalPelajaran}
 * (Reguler/Ujian/Ramadhan — kolom {@code jenis_jadwal_pelajaran_id} {@code nullable = false}),
 * dan <b>boleh</b> dikelompokkan oleh satu {@link KelompokJamPelajaran}
 * (kolom {@code kelompok_jam_pelajaran_id} nullable).</p>
 *
 * <p>Entity ini adalah <b>ujung yang dirujuk</b>, bukan yang merujuk, di seluruh rantai
 * penjadwalan sekolah: {@code JadwalPelajaran} menyimpan dua belas FK ke tabel ini
 * ({@code jamPelajaran} … {@code jamPelajaran12}, satu per slot yang dipakai baris jadwal
 * tersebut), layar Guru Mengajar menyediakan dua puluh lima combobox yang seluruhnya diisi dari
 * kelas ini, dan seluruh teks jam yang tercetak pada jadwal/SK/laporan berasal dari
 * {@link #getMulaiS()}/{@link #getSampaiS()} milik baris-baris di sini. Ubah satu baris di sini
 * dan jam tayang seluruh jadwal yang memakainya ikut berubah — tidak ada penyalinan nilai waktu
 * ke sisi {@code JadwalPelajaran}.</p>
 *
 * <p><b>Slot, bukan jadwal.</b> Baris di sini tidak mengenal hari, kelas, guru, mata pelajaran,
 * tahun ajaran, maupun semester. Semua itu milik {@code JadwalPelajaran}. Konsekuensinya: satu
 * baris {@code JamPelajaran} dipakai ulang oleh berapa pun baris jadwal, dan menghapusnya
 * memutus rujukan semua baris jadwal tersebut sekaligus.</p>
 *
 * <h3>Tiga representasi waktu yang sama, disinkronkan hanya oleh getter</h3>
 * <p>Kejanggalan struktural terpenting berkas ini: satu fakta ("slot ini mulai 07:30")
 * disimpan <b>tiga kali</b> dalam tiga kolom berbeda.</p>
 * <ol>
 * <li>{@link #getMulai()}/{@link #getSelesai()} — kolom {@code mulai}/{@code selesai} bertipe
 *     {@code TIME} ({@code @Temporal(TemporalType.TIME)}), keduanya {@code nullable = false}.
 *     Inilah sumber kebenaran.</li>
 * <li>{@link #getMulaiS()}/{@link #getSampaiS()} — properti {@code String} <b>tanpa</b>
 *     {@code @Column} maupun {@code @Transient}. Karena {@code @Id} dipasang pada getter, JPA
 *     memakai <i>property access</i> dan setiap getter publik tanpa {@code @Transient}
 *     <b>ikut dipetakan</b>; dengan {@code hbm2ddl.auto=update} (lihat {@code hibernate.cfg.xml})
 *     kolomnya dibuat otomatis dengan nama sama seperti propertinya. Jadi ini kolom sungguhan,
 *     bukan turunan dalam memori.</li>
 * <li>{@link #getWaktuMulaiD()}/{@link #getWaktuSelesaiD()} — kolom {@code waktu_mulai_d}/
 *     {@code waktu_selesai_d} bertipe {@code Double}, berisi "pseudo-desimal" 7.30 untuk 07:30.
 *     Dipakai <b>hanya</b> untuk mengurutkan ({@code Order.asc("waktuMulaiD")} di helper
 *     timetable), tidak pernah untuk berhitung — dan memang tidak boleh (lihat Javadoc
 *     method-nya).</li>
 * </ol>
 * <p>Tidak ada satu pun setter, validator, atau service yang menjaga ketiganya konsisten. Yang
 * menyinkronkan hanyalah efek samping di dalam getter: {@link #getMulaiS()} menulis ulang bentuk
 * teks dari {@code mulai}, dan {@link #getWaktuMulaiD()} menulis ulang bentuk angka dari bentuk
 * teks. Selama semua penulisan lewat layar master hal ini tidak terasa; begitu sebuah nilai
 * masuk lewat jalur lain (impor Excel, {@link #setMulaiS(String)} dengan format tak dikenal,
 * baris hasil skrip), ketiga kolom bisa <b>berbeda isi secara permanen</b>. Rinciannya pada
 * Javadoc {@link #setMulaiS(String)}.</p>
 *
 * <h3>Siapa yang memakai</h3>
 * <ul>
 * <li><b>Layar master</b> {@code ais.action.master.sekolah.JamPelajaranAction} +
 * {@code /pages/master/sekolah/jam_pelajaran.zul} — menu "Jam Pelajaran"
 * ({@code ais.common.MenuInitializer} id 2345629, induk 5700 "Setup"). CRUD penuh; grid berkolom
 * Nama Jam Pelajaran / Sekolah / Mulai / Sampai / Jenis Jam Pelajaran / Kelompok Jam Pelajaran /
 * Jml JP / Keterangan / Aktif. Formulirnya berisi Nama, Jumlah JP, sepasang {@code Timebox}
 * "Waktu * … s.d …", Yayasan, Sekolah, Jenis Jam Pelajaran (wajib), Kelompok Jam Pelajaran
 * (opsional), dan Keterangan. Tombol Download/Upload memakai kolom
 * {@code id, nama, sekolah, mulaiS, sampaiS, jp, jenisJadwalPelajaran, kelompokJamPelajaran,
 * keterangan}.</li>
 * <li><b>Jadwal pelajaran</b> {@code ais.action.master.sekolah.JadwalPelajaranAction} — dua belas
 * combobox slot, masing-masing diisi
 * {@code Common.insertCombo(..., JamPelajaran.class, ...)} dengan tapis <i>sekolah cocok ATAU
 * sekolah NULL</i> DAN <i>aktif true ATAU aktif NULL</i>, label item dirangkai dari
 * {@code nama + mulaiS + sampaiS + jenisJadwalPelajaran}. Hasil pilihannya disimpan pada
 * {@code JadwalPelajaran.jamPelajaranN}.</li>
 * <li><b>Guru mengajar</b> {@code ais.action.master.sekolah.GuruMengajarAction} — dua puluh lima
 * combobox dengan tapis dan label yang persis sama.</li>
 * <li><b>Beban mengajar guru</b> {@code ais.action.master.sekolah.GuruAction} — menjumlahkan
 * {@link #getJp()} seluruh slot yang guru bersangkutan pegang menjadi angka "jumlah JP" pada
 * layar guru. Lihat catatan pada {@link #getJp()} soal baris ber-{@code jp} {@code NULL}.</li>
 * <li><b>Pertemuan &amp; presensi</b> {@code PertemuanJadwalPelajaranAction} (mencocokkan
 * pertemuan dengan dua belas slot jadwal) dan {@code helper/PenjadwalanSiswaHelper} (mengisi
 * {@code waktuMulai}/{@code waktuSelesai} pertemuan dari {@link #getMulaiS()}/
 * {@link #getSampaiS()}).</li>
 * <li><b>Laporan</b> {@code report.format1.sekolah.LaporanJadwalPelajaran} (memecah cetak per
 * {@link KelompokJamPelajaran} milik slot), {@code report.format1.sekolah.LaporanSKGuru}
 * (menyuntikkan properti slot ke parameter Jasper lewat
 * {@code Common.insertProperty(JamPelajaran.class, ...)}), dan {@code report.CommonReportHelper}
 * (merangkai teks "07:30 s.d 08:19").</li>
 * <li><b>Tampilan ringkas</b> {@code ais.common.JadwalDisplayHelper} — merangkai HTML "hari +
 * jam" untuk kedua belas slot sebuah baris jadwal.</li>
 * <li><b>Helper timetable</b> {@code ais.action.master.sekolah.helper.TimetableJadwalPelajaranWindow}
 * — disisipkan sebagai komponen di dalam {@code /pages/master/sekolah/jadwal_pelajaran.zul}.
 * Membaca, <b>membuat</b> (tombol "buat waktu default", 10 slot 07:30–17:49), <b>mengubah</b>
 * dan <b>menghapus</b> baris entity ini. Lihat bagian Hak akses — di sinilah temuan paling
 * serius berkas ini.</li>
 * <li><b>Dasbor</b> {@code dashboard.admin.DasboardJadwalPelajaran} dan
 * {@code dashboard.admin.DasborAkademikSekolah} — kartu metrik "Jam Pelajaran" dan tabel ringkas
 * "Top Jenis Jadwal/Jam".</li>
 * <li><b>Pramuat cache</b> {@code ais.common.InitData} — kelasnya terdaftar pada
 * {@code initClasses(...)} sehingga barisnya dimuat ke cache aplikasi saat startup. Murni
 * pramuat; <b>tidak ada auto-seed</b>, sehingga instalasi baru mulai tanpa satu slot pun.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 * <ol>
 * <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Field-nya dideklarasikan ulang di sini
 * (lihat catatan tentang {@link GeneralValueObject} di bawah).</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()} dan {@link #getYayasan()}; yayasan diturunkan
 * ulang dari sekolah pada setiap pembacaan.</li>
 * <li><b>Pengelompokan</b> — {@link #getJenisJadwalPelajaran()} (wajib) dan
 * {@link #getKelompokJamPelajaran()} (opsional).</li>
 * <li><b>Waktu — bentuk kanonik</b> — {@link #getMulai()}, {@link #getSelesai()} beserta
 * setternya.</li>
 * <li><b>Waktu — bentuk teks</b> — {@link #getMulaiS()}, {@link #setMulaiS(String)},
 * {@link #getSampaiS()}, {@link #setSampaiS(String)}. Satu-satunya tempat parsing string waktu
 * di kelas ini.</li>
 * <li><b>Waktu — bentuk angka untuk pengurutan</b> — {@link #getWaktuMulaiD()},
 * {@link #getWaktuSelesaiD()} beserta setternya.</li>
 * <li><b>Atribut deskriptif</b> — {@link #getNama()}, {@link #getKeterangan()},
 * {@link #getJp()}, {@link #getAktif()}.</li>
 * <li><b>Konstruktor</b> — konstruktor kosong (dipakai) dan konstruktor lima argumen
 * (<b>tidak</b> dipakai di mana pun; lihat Javadocnya).</li>
 * </ol>
 * <p>Tidak ada koleksi, tidak ada method bisnis, dan tidak ada query statis di kelas ini.
 * Seluruh perilaku non-trivial terkonsentrasi pada tujuh getter berperilaku khusus
 * ({@link #getYayasan()}, {@link #getMulai()}, {@link #getSelesai()}, {@link #getMulaiS()},
 * {@link #getSampaiS()}, {@link #getWaktuMulaiD()}, {@link #getWaktuSelesaiD()}) dan dua setter
 * parsing ({@link #setMulaiS(String)}, {@link #setSampaiS(String)}).</p>
 *
 * <h3>Catatan penting tentang {@link GeneralValueObject}</h3>
 * <p>Induknya <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — hanya POJO abstrak
 * biasa, sehingga Hibernate <b>tidak</b> memetakan satu pun properti induk. Karena itu field
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b>
 * dideklarasikan ulang di setiap entity turunan. Duplikasi ini <b>bukan bug</b>, melainkan
 * keharusan teknis; jangan "dirapikan" dengan memindahkannya ke induk. Yang tetap diwarisi
 * adalah helper statis, terutama {@code check(...)} yang dipakai keempat getter relasi di sini.</p>
 *
 * <h3>Pola arsitektur berulang — hasil verifikasi pada berkas ini</h3>
 * <ul>
 * <li><b>Getter dengan efek tulis balik (write-back)</b> — <b>ADA, lima buah</b>:
 * {@link #getYayasan()}, {@link #getMulaiS()}, {@link #getSampaiS()}, {@link #getWaktuMulaiD()},
 * dan {@link #getWaktuSelesaiD()} semuanya menulis ke field saat dibaca. Karena Hibernate
 * membaca entity ini lewat <i>property access</i>, pembacaan biasa (merender satu baris grid,
 * mengisi satu item combobox) dapat berubah menjadi UPDATE nyata plus satu revisi Envers baru.
 * {@link #getSekolah()}, {@link #getJenisJadwalPelajaran()} dan
 * {@link #getKelompokJamPelajaran()} juga menulis ke field, tetapi hanya menukar proxy lazy
 * dengan instance teresolusi (baris logis yang sama) sehingga tidak mengubah nilai kolom.</li>
 * <li><b>Getter destruktif yang mengosongkan data</b> (pola {@code KelasSiswaPSB.getNama()}) —
 * <b>TIDAK ADA</b>. Tidak satu pun getter di sini menghapus nilai yang sudah ada.
 * {@link #getMulai()}/{@link #getSelesai()} melakukan hal yang berbeda dan tetap perlu
 * diwaspadai: keduanya <b>mengarang</b> nilai (jam server saat itu) alih-alih mengembalikan
 * {@code null}. Lihat Javadoc masing-masing.</li>
 * <li><b>{@code getKeterangan()} yang membalik kontraknya</b> — <b>TIDAK ADA</b>;
 * {@link #getKeterangan()} adalah getter polos.</li>
 * <li><b>Penciutan {@code TreeSet}</b> — <b>TIDAK RELEVAN</b>: kelas ini tidak punya koleksi.</li>
 * <li><b>Cakupan tenant fail-open</b> — <b>ADA, tiga varian sekaligus</b> (nol filter di layar
 * master, {@code return} dini di dasbor, dan tapis yang benar-benar hilang di helper timetable).
 * Lihat bagian berikutnya.</li>
 * <li><b>Pewarisan hak lewat menu induk</b> — <b>ADA, dua arah</b>. Lihat bagian berikutnya.</li>
 * <li><b>Validasi tumpang-tindih waktu antar slot</b> — <b>TIDAK ADA SAMA SEKALI</b>. Lihat
 * bagian tersendiri di bawah.</li>
 * <li><b>Kotak centang filter "aktif" yang mati</b> (temuan {@link KelompokJamPelajaran}) —
 * <b>TERULANG PERSIS</b>. Berkas {@code jam_pelajaran.zul} mendeklarasikan
 * {@code <checkbox id="searchaktif" label="Tampilkan hanya yang aktif" checked="true"
 * forward="onClick=onSearchDefault"/>}, tetapi {@code JamPelajaranAction} tidak punya field
 * bernama {@code searchaktif} (jadi tidak pernah di-autowire) dan {@code initCriteria()} tidak
 * pernah menyentuh kolom {@code aktif}. Centang itu memicu pencarian ulang namun hasilnya tak
 * pernah berubah: slot yang sudah dinonaktifkan tetap tampil. Karena centangnya sudah menyala
 * sejak halaman dibuka, wajar disangka bekerja.</li>
 * </ul>
 *
 * <h3>Hak akses dan cakupan tenant</h3>
 * <p><b>Layar master: gerbang benar, tapis tenant nol.</b> {@code JamPelajaranAction} memanggil
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose}, menyembunyikan tombol Tambah tanpa
 * hak {@code CREATE}, menonaktifkan kotak centang Aktif serta tombol Ubah/Hapus tanpa hak
 * {@code UPDATE}/{@code DELETE}, dan menampilkan tombol unggah massal hanya bila ketiga hak
 * dimiliki sekaligus. Namun {@code initCriteria()} hanya menambahkan pembatas untuk nilai yang
 * <i>dipilih pengguna</i> pada combobox Yayasan/Sekolah; bila dibiarkan kosong, pembatasnya
 * menjadi {@code Restrictions.sqlRestriction("1=1")}. Tidak ada pembatas bawaan ke sekolah milik
 * pengguna, sehingga pemegang hak BACA menu ini melihat — dan dengan hak UBAH/HAPUS dapat
 * mengubah atau menghapus — slot jam milik <b>seluruh sekolah dan yayasan</b> pada satu
 * instalasi. Isinya metadata penjadwalan, bukan data pribadi, sehingga dampak kerahasiaannya
 * rendah; dampak <b>integritasnya</b> besar — menghapus atau menggeser satu slot langsung
 * mengubah jam tayang setiap baris {@code JadwalPelajaran} sekolah lain yang memakainya.
 * Mekanismenya sama dengan yang sudah tercatat pada {@link JenisJadwalPelajaran} dan
 * {@link KelompokJamPelajaran}: bukan tapis yang gagal terbuka, melainkan tapis yang memang tidak
 * pernah ditulis. Combobox Yayasan/Sekolah sendiri terisi seluruh tenant instalasi begitu
 * resolusi tenant gagal ({@code SekolahUtil.getSekolah()}/{@code getYayasan()} mengembalikan
 * objek ber-id {@code null} — akar struktural batch 67), sehingga layar ini juga mewarisi
 * pelebaran cakupan dari sana.</p>
 *
 * <p><b>Dasbor: fail-open varian {@code return} dini.</b>
 * {@code DasboardJadwalPelajaran.applySekolahFilter(...)} langsung {@code return} tanpa memasang
 * pembatas apa pun bila {@code currentSekolah == null} — kondisi yang terjadi persis saat
 * pengguna memilih "Semua Sekolah". Kartu "Jam Pelajaran" dan tabel "Top Jenis Jadwal/Jam"
 * karenanya menghitung seluruh instalasi. Sama persis dengan yang sudah tercatat pada
 * {@link JenisJadwalPelajaran}.</p>
 *
 * <p><b>Pewarisan hak lewat menu induk — arah keluar.</b> Layar ini menyisipkan berkas ZUL
 * <i>master lain</i> apa adanya sebagai tab kedua berjudul "Kelompok":
 * {@code JamPelajaranAction.onMasa(...)} membuat
 * {@code MyInclude("/pages/master/sekolah/kelompok_jam_pelajaran.zul")}. Sisipan itu berbagi
 * halaman ZK yang sama, sedangkan {@code CommonPrivilages.checkPrevilages(...)} menentukan hak
 * dari {@code Common.getCurrentMenu()} — yaitu menu <b>Jam Pelajaran</b>. Jadi hak
 * TAMBAH/UBAH/HAPUS atas master {@link KelompokJamPelajaran} sesungguhnya diberikan oleh hak menu
 * ini; peran yang sengaja tidak diberi menu "Kelompok Jam Pelajaran" tetap memperoleh CRUD penuh
 * atasnya. Ini sisi lain dari instance yang sudah tercatat di Javadoc
 * {@link KelompokJamPelajaran}.</p>
 *
 * <p><b>Pewarisan hak lewat menu induk — arah masuk, dan ini yang paling serius.</b>
 * {@code /pages/master/sekolah/jadwal_pelajaran.zul} menyisipkan komponen
 * {@code use="ais.action.master.sekolah.helper.TimetableJadwalPelajaranWindow"}. Kelas helper itu
 * <b>tidak memanggil {@code CommonPrivilages.checkPrevilages(...)} maupun
 * {@code Common.doCheckSecurity()} satu kali pun</b> pada 1.508 barisnya, namun menyediakan:</p>
 * <ul>
 * <li>tombol <i>buat waktu default</i> — menyimpan sepuluh baris {@code JamPelajaran} baru untuk
 *     sekolah dari kelas yang dipilih;</li>
 * <li>dialog <i>Kelola Jam Pelajaran</i> — mengubah nama/mulai/selesai seluruh slot aktif sekolah
 *     tersebut, dan pada tombol Simpan <b>menghapus</b> ({@code session.delete}) setiap slot yang
 *     tidak lagi muncul di daftar dialog.</li>
 * </ul>
 * <p>Akibatnya <b>hak BACA menu "Jadwal Pelajaran" sudah cukup untuk membuat, mengubah, dan
 * menghapus permanen master Jam Pelajaran</b> — tanpa pernah memegang hak apa pun atas menu
 * "Jam Pelajaran" yang seharusnya mengatur data itu. Dua rincian memperburuknya: (a) baris yang
 * kolom "Mulai"-nya dikosongkan pengguna akan dilewati perulangan simpan
 * ({@code if (wm.isEmpty()) continue;}) sehingga id-nya tidak masuk himpunan {@code present} dan
 * <b>ikut terhapus</b> — menghapus slot cukup dengan mengosongkan satu kotak teks, tanpa dialog
 * konfirmasi apa pun; (b) slot yang dihapus masih dirujuk kolom {@code jam_pelajaran_id} pada
 * baris {@code JadwalPelajaran} mana pun, sehingga hasilnya bukan penghapusan bersih melainkan
 * kegagalan FK saat flush atau jadwal yang kehilangan jamnya. Pola "layar detail/helper yang
 * disisipkan tanpa gerbang" ini identik dengan temuan berulang sejak batch 50.</p>
 *
 * <p><b>Fail-open lintas tenant di helper yang sama.</b> {@code loadJamList(Session, String)}
 * mencoba memuat slot milik sekolah dari kelas terpilih; bila daftarnya kosong (atau kelas belum
 * dipilih), ia jatuh ke {@code createCriteria(JamPelajaran.class).add(eq("aktif", TRUE))
 * .addOrder(asc("waktuMulaiD")).setMaxResults(20)} — <b>tanpa tapis sekolah/yayasan sama
 * sekali</b>. Kisi timetable sebuah sekolah karenanya dapat terisi dua puluh slot milik sekolah
 * lain, dan slot-slot itulah yang kemudian dipakai saat pengguna menyeret mata pelajaran ke
 * dalam kisi ({@code jp.setJamPelajaran(...)}) — jadi bukan sekadar kebocoran tampilan,
 * melainkan jalan masuk data lintas tenant ke tabel jadwal. Pasangannya sudah tercatat pada
 * {@link JenisJadwalPelajaran}: {@code jenisDefault(Session, Sekolah)} pun jatuh ke
 * {@code setMaxResults(1)} tanpa tapis.</p>
 *
 * <h3>Validasi tumpang-tindih waktu antar slot: TIDAK ADA (terverifikasi)</h3>
 * <p>Pemeriksaan menyeluruh atas seluruh jalur tulis entity ini menghasilkan kesimpulan tegas:
 * <b>tidak ada satu pun pemeriksaan bahwa dua slot tidak saling menindih, dan bahkan tidak ada
 * pemeriksaan bahwa {@code mulai} lebih awal daripada {@code selesai}</b>.</p>
 * <ul>
 * <li>{@code JamPelajaranAction.onSave(...)} hanya memvalidasi <i>keterisian</i>: nama tidak
 *     kosong, jam mulai terisi, jam selesai terisi, yayasan terisi, sekolah terisi, jenis jadwal
 *     terpilih. Tidak ada perbandingan waktu apa pun. Slot "Jam ke-2 = 08:00–07:00" tersimpan
 *     tanpa keluhan, begitu pula "Jam ke-1 = 07:00–09:00" berdampingan dengan
 *     "Jam ke-2 = 07:30–08:15".</li>
 * <li>Dialog <i>Kelola Jam Pelajaran</i> pada helper timetable menyimpan apa adanya, hanya
 *     melewati baris yang kolom mulainya kosong.</li>
 * <li>Tombol <i>buat waktu default</i> menghindari duplikasi dengan membandingkan
 *     <b>string jam mulai</b> saja ({@code ada.contains(d[1])}). Slot yang sudah ada dengan jam
 *     mulai berbeda tetapi rentangnya menindih tidak terdeteksi; sebaliknya, slot lama yang jam
 *     mulainya kebetulan sama tetapi jam selesainya berbeda akan dianggap "sudah ada" dan slot
 *     bawaan tidak dibuat.</li>
 * <li>Tidak ada {@code unique constraint} pada kombinasi apa pun. Dua slot bernama sama, dengan
 *     jam identik, pada sekolah yang sama, tetap dapat dibuat berulang kali.</li>
 * </ul>
 * <p><b>Mengapa ini penting untuk deteksi bentrok jadwal.</b> Seluruh pemeriksa bentrok di
 * modul ini — pemeriksa ruang/kelas/guru pada {@code JadwalPelajaran} maupun
 * {@code buildConflictIds(...)} pada helper timetable yang berkunci
 * {@code guruId_hari_jamId} — membandingkan <b>identitas baris slot</b>, bukan rentang waktunya.
 * Dua slot berbeda yang rentang waktunya menindih karena itu <b>tidak pernah</b> dianggap
 * bentrok: seorang guru dapat dijadwalkan pada "Jam ke-1 (07:00–08:00)" di satu kelas dan pada
 * "Jam Blok A (07:30–09:00)" di kelas lain, dan tidak satu pun pemeriksa mengeluh. Ini lapisan
 * kegagalan yang <b>berbeda</b> dari, dan menumpuk di atas, kerusakan pemeriksa bentrok yang
 * sudah tercatat pada {@code JadwalPelajaran} (syarat "hari slot pertama harus sama" yang membuat
 * bentrok slot II–XII tak pernah terdeteksi). Bila suatu saat pemeriksa bentrok
 * {@code JadwalPelajaran} diperbaiki, ia tetap tidak akan menangkap kasus ini selama
 * perbandingannya berbasis id slot.</p>
 *
 * <h3>Kuirk dan jebakan lain yang terverifikasi</h3>
 * <ol>
 * <li><b>Baris baru dari layar master tidak terlihat oleh layar timetable.</b>
 *     {@code JamPelajaranAction.onSave(...)} tidak pernah memanggil {@link #setAktif(Boolean)}
 *     (formulirnya memang tidak punya kotak centang Aktif — centangnya hanya ada di grid),
 *     sehingga baris baru tersimpan dengan {@code aktif = NULL}. Pembaca yang menulis tapisnya
 *     toleran-NULL ({@code JadwalPelajaranAction}, {@code GuruMengajarAction}) tetap
 *     menampilkannya, tetapi {@code TimetableJadwalPelajaranWindow} memakai
 *     {@code Restrictions.eq("aktif", Boolean.TRUE)} yang <b>ketat</b>. Slot yang baru dibuat
 *     dari layar master karenanya tidak muncul di kisi timetable sampai seseorang menyalakan lalu
 *     — pada praktiknya — mengklik kotak centang Aktif di grid master (yang memanggil
 *     {@link #setAktif(Boolean)} dan menyimpan {@code true} sungguhan).</li>
 * <li><b>Yayasan pada formulir praktis tidak berarti.</b> {@code onSave(...)} menyimpan yayasan
 *     pilihan pengguna, tetapi {@link #getYayasan()} menimpanya dengan {@code sekolah.getYayasan()}
 *     pada pembacaan berikutnya. Selama sekolah terisi (dan itu wajib), nilai yang berbeda tidak
 *     pernah bertahan. Sama dengan {@link JenisJadwalPelajaran}.</li>
 * <li><b>Slot "global" hanya bisa lahir dari luar UI.</b> Tapis combobox di layar Jadwal
 *     Pelajaran/Guru Mengajar menerima baris ber-{@code sekolah = NULL} sebagai "berlaku untuk
 *     semua sekolah", tetapi {@code onSave(...)} mewajibkan Sekolah terisi dan
 *     {@link #setSekolah(Sekolah)} membuang objek ber-id {@code null}. Baris global karenanya
 *     hanya lahir dari impor/skrip — dan sekali dibuka lalu disimpan dari layar master, ia
 *     berubah permanen menjadi milik satu sekolah.</li>
 * <li><b>Penanganan galat yang tidak simetris.</b> {@link #getWaktuSelesaiD()} menelan kegagalan
 *     parsing diam-diam, sedangkan {@link #getWaktuMulaiD()} — kode yang selain itu identik —
 *     memanggil {@code Common.tampilErrorJikaAdmin(e)} sehingga memunculkan dialog galat kepada
 *     pengguna admin. Sisa salin-tempel yang tidak dirapikan; gejalanya "kadang muncul error saat
 *     membuka grid, kadang tidak".</li>
 * <li><b>Cabang parsing 8 karakter memakai pola tanggal, bukan jam.</b>
 *     {@link #setMulaiS(String)}/{@link #setSampaiS(String)} mem-parsing masukan sepanjang 8
 *     karakter dengan {@code Common.dateFormat1} yang berpola {@code "dd-MM-yyyy"}. Lihat Javadoc
 *     method-nya.</li>
 * <li><b>Konstruktor lima argumen tidak dipakai dan tidak dapat dipakai.</b> Ia tidak mengisi
 *     {@code jenisJadwalPelajaran} yang FK-nya {@code nullable = false}, sehingga baris hasilnya
 *     pasti gagal INSERT. Terverifikasi tidak dipanggil dari mana pun.</li>
 * <li><b>Ekspor/impor massal menyertakan kolom {@code id}.</b> Daftar kolom Download/Upload di
 *     layar master diawali {@code "id"}, dan {@code CommonDownloadUpload} memakai kolom itu
 *     sebagai identitas baris saat impor. Dikombinasikan dengan tapis tenant nol pada
 *     {@code initCriteria()}, satu berkas Excel dapat menimpa slot milik sekolah mana pun dalam
 *     instalasi. Tombolnya sendiri hanya tampil bila pengguna memegang CREATE+UPDATE+DELETE
 *     sekaligus — jadi gerbangnya ada, cakupannya yang terlalu luas. Pola yang sama sudah
 *     tercatat pada {@code JenisItemPenilaianSiswa} (batch 64).</li>
 * <li><b>Kolom hasil {@code hbm2ddl} vs tabel audit Envers.</b> Entity ini {@code @Audited} dan
 *     memiliki properti yang kolomnya dibuat otomatis tanpa {@code @Column}
 *     ({@code mulaiS}, {@code sampaiS}, {@code aktif}). Sesuai peringatan di
 *     {@code hibernate.cfg.xml}, {@code hbm2ddl.auto=update} menambah kolom ke tabel basis di
 *     {@code sekolah} tetapi tidak selalu ke tabel {@code new_audit.*__audit}; bila tertinggal,
 *     INSERT audit gagal dan seluruh flush ikut rollback. Jangan menambah properti baru ke kelas
 *     ini tanpa menyelaraskan tabel auditnya.</li>
 * </ol>
 *
 * <h3>Persistensi</h3>
 * <p>Ber-{@code @Audited} (Hibernate Envers menyimpan riwayat perubahan) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate}, sehingga hanya kolom yang benar-benar berubah yang
 * ikut dalam pernyataan SQL — properti penting mengingat banyaknya getter berefek tulis balik di
 * kelas ini. Id memakai strategi {@code IDENTITY} — berurutan dan mudah ditebak, jadi jangan
 * pernah menjadikan id sebagai satu-satunya pembatas akses. Seluruh relasi ber-{@code FetchType.LAZY}
 * dengan kaskade {@code PERSIST}+{@code MERGE}; tidak ada kaskade hapus ke arah mana pun.</p>
 *
 * @see JenisJadwalPelajaran
 * @see KelompokJamPelajaran
 * @see JadwalPelajaran
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "jam_pelajaran", schema = "sekolah")
public class JamPelajaran extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya harus tetap agar sesi ZK dan cache aplikasi yang sudah
	 * berisi objek versi lama tidak menolak instance baru dengan {@code InvalidClassException} —
	 * entity ini ikut diserialisasi ke sesi ZK (dipegang {@code JamPelajaranAction} dan seluruh
	 * layar jadwal) maupun ke cache pramuat {@code ais.common.InitData}.
	 */
	private static final long serialVersionUID = 4964672204305044550L;
	/**
	 * Kunci utama baris, dibangkitkan basis data ({@code IDENTITY}). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 * Dideklarasikan ulang karena induknya tidak dipetakan Hibernate.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh} dan diisi
	 * dari sumber yang sama. Dideklarasikan ulang karena induknya tidak dipetakan Hibernate.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila baris belum pernah melewati UPDATE
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong.</b> Argumen {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama tetap bertahan. Ini disengaja: jejak audit tidak boleh
	 * terhapus oleh alur yang kebetulan tidak mengenal pengguna (thread latar, servlet bank,
	 * pekerjaan terjadwal). Konsekuensinya jejak audit <b>tidak dapat dikosongkan</b> lewat
	 * setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Berperilaku sama dengan
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan sehingga jejak audit yang
	 * sudah ada tidak dapat dihapus.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila baris belum pernah melewati UPDATE
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum pernyataan UPDATE baris ini
	 * dijalankan, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} dari konteks pengguna aktif.
	 *
	 * <p><b>Efek samping:</b> mengubah tiga field audit entity ini. Jangan dipanggil manual —
	 * ini kait lifecycle, bukan API.</p>
	 *
	 * <p><b>Catatan:</b> karena beberapa getter di kelas ini menulis balik ke field-nya sendiri
	 * (lihat Javadoc kelas), pembacaan biasa dapat memicu <i>dirty checking</i> menemukan
	 * perubahan, dan kait ini ikut berjalan — sehingga baris dapat tercatat "diubah oleh X" tanpa
	 * X pernah menyunting apa pun.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi dengan waktu "sekarang" versi aplikasi
	 * ({@code WaktuUtil.getDate()}, bukan {@code new Date()}) agar mengikuti zona waktu dan
	 * penyetelan jam institusi, lalu diperbarui {@link #onUpdate()} pada setiap UPDATE.
	 * Dideklarasikan ulang karena induknya tidak dipetakan Hibernate.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya tidak perlu dipanggil manual — nilainya
	 * diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir (tanggal + jam)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Kategori slot ini — FK <b>WAJIB</b> ke {@code sekolah.jenis_jadwal_pelajaran}
	 * (Reguler/Ujian/Ramadhan/…). Lihat {@link #getJenisJadwalPelajaran()}.
	 */
	private JenisJadwalPelajaran jenisJadwalPelajaran;
	/**
	 * Kelompok pencetakan slot ini — FK <b>opsional</b> ke {@code sekolah.kelompok_jam_pelajaran}.
	 * Lihat {@link #getKelompokJamPelajaran()}.
	 */
	private KelompokJamPelajaran kelompokJamPelajaran;
	/** Sekolah pemilik slot ini; FK wajib ({@code sekolah_id}, {@code nullable = false}). */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik slot ini. Bukan data mandiri: selalu diturunkan ulang dari
	 * {@link #sekolah} setiap kali {@link #getYayasan()} dipanggil.
	 */
	private Yayasan yayasan;
	/** Catatan bebas administrator; kolom {@code varchar(10000)}. */
	private String keterangan;
	/** Jam mulai slot (hanya jam:menit yang bermakna; kolom {@code TIME}, wajib). */
	private Date mulai;
	/** Nama slot yang tampil di seluruh combobox dan cetakan, mis. "Jam ke-1". Wajib. */
	private String nama;
	/** Jam selesai slot (kolom {@code TIME}, wajib). */
	private Date selesai;
	/**
	 * Bobot beban mengajar slot ini dalam satuan JP; kolom {@code jp_data}. Dijumlahkan
	 * {@code GuruAction} menjadi angka "jumlah JP" per guru. Lihat {@link #getJp()}.
	 */
	private Double jp;
	/**
	 * Bentuk teks {@code "HH:mm"} dari {@link #mulai}. Meski tanpa {@code @Column}, properti ini
	 * <b>tetap dipetakan</b> ke kolom sendiri (lihat Javadoc kelas, bagian "tiga representasi").
	 */
	private String mulaiS;
	/** Bentuk teks {@code "HH:mm"} dari {@link #selesai}; padanan {@link #mulaiS}. */
	private String sampaiS;
	/**
	 * Saklar aktif. Dibiarkan {@code NULL} oleh layar master saat baris dibuat; {@link #getAktif()}
	 * memetakan {@code NULL} menjadi {@code true}, tetapi helper timetable memakai tapis yang
	 * ketat (lihat Javadoc kelas, kuirk no. 1).
	 */
	private Boolean aktif;
	/**
	 * Bentuk "pseudo-desimal" dari {@link #selesai} (08:19 menjadi 8.19); kolom
	 * {@code waktu_selesai_d}. Bukan jam desimal — lihat {@link #getWaktuSelesaiD()}.
	 */
	private Double waktuSelesaiD;
	/**
	 * Bentuk "pseudo-desimal" dari {@link #mulai} (07:30 menjadi 7.30); kolom
	 * {@code waktu_mulai_d}. Satu-satunya kolom yang dipakai untuk mengurutkan slot.
	 */
	private Double waktuMulaiD;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA, dan juga dipakai langsung oleh
	 * {@code JamPelajaranAction.onAdd(...)} serta {@code TimetableJadwalPelajaranWindow} saat
	 * membuat slot baru.
	 */
	public JamPelajaran() {
	}

	/**
	 * Konstruktor lengkap peninggalan generator hbm2java.
	 *
	 * <p><b>Tidak dipakai di mana pun</b> pada basis kode ini (terverifikasi: seluruh pembuatan
	 * instance memakai konstruktor kosong), dan sesungguhnya <b>tidak dapat dipakai</b>: ia tidak
	 * mengisi {@link #jenisJadwalPelajaran}, padahal FK-nya {@code nullable = false}, sehingga
	 * baris hasilnya pasti gagal INSERT. Jangan jadikan contoh untuk kode baru.</p>
	 *
	 * <p>Perhatikan pula bahwa {@code sekolah} disaring dengan aturan yang sama seperti
	 * {@link #setSekolah(Sekolah)}: objek ber-id {@code null} dianggap tidak ada.</p>
	 *
	 * @param id      kunci utama; disetel manual walau kolomnya {@code IDENTITY}
	 * @param sekolah sekolah pemilik; objek ber-id {@code null} diperlakukan sebagai {@code null}
	 * @param mulai   jam mulai slot
	 * @param nama    nama slot
	 * @param selesai jam selesai slot
	 */
	public JamPelajaran(long id, Sekolah sekolah, Date mulai, String nama, Date selesai) {
		this.id = id;
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
		this.mulai = mulai;
		this.nama = nama;
		this.selesai = selesai;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Nilainya dibangkitkan basis data ({@code IDENTITY}) sehingga <b>berurutan dan mudah
	 * ditebak</b>; jangan pernah menjadikannya satu-satunya pembatas akses. Kolomnya
	 * {@code insertable = false} karena diisi sepenuhnya oleh basis data.</p>
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
	 * Menyetel kunci utama secara manual. Hanya untuk kebutuhan teknis (impor, rekonstruksi
	 * objek); alur normal menyerahkannya kepada basis data.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik slot ini.
	 *
	 * <p><b>Efek samping:</b> memanggil {@code GeneralValueObject.check(...)} yang menukar proxy
	 * lazy dengan instance yang sudah teresolusi (dari {@code EntityIdentityMap}, cache aplikasi,
	 * atau muat ulang) dan <b>menulis hasilnya kembali ke field</b>. Yang berubah hanya
	 * <i>instance Java</i>, bukan baris yang ditunjuk, sehingga nilai kolom {@code sekolah_id}
	 * tidak bergeser. Efeknya adalah pembacaan ini dapat memicu kueri basis data walau tampak
	 * seperti getter biasa.</p>
	 *
	 * @return sekolah pemilik; secara praktis tidak pernah {@code null} untuk baris tersimpan
	 *         karena kolomnya {@code nullable = false}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik slot ini.
	 *
	 * <p><b>Menolak objek "hampa".</b> Argumen {@code null} <i>maupun</i> objek {@link Sekolah}
	 * yang id-nya masih {@code null} (mis. hasil {@code SekolahUtil.getSekolah()} saat resolusi
	 * tenant gagal — akar struktural yang tercatat pada {@code Yayasan}, batch 67) sama-sama
	 * menyimpan {@code null}. Karena kolomnya {@code nullable = false}, akibatnya bukan baris
	 * "global" melainkan <b>kegagalan INSERT</b> saat flush. Perlakukan ini sebagai penjaga
	 * integritas, bukan sebagai cara membuat slot lintas sekolah.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek ber-id {@code null} menyimpan
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik slot ini — <b>selalu diturunkan ulang dari sekolah</b>.
	 *
	 * <p><b>Getter dengan efek tulis balik (write-back).</b> Alurnya: baca {@link #getSekolah()};
	 * bila sekolah ada, timpa field {@link #yayasan} dengan {@code sekolah.getYayasan()}; lalu
	 * resolusi proxy lewat {@code check(...)}. Karena Hibernate membaca entity ini lewat
	 * <i>property access</i>, nilai hasil penimpaan itulah yang ikut dibandingkan saat
	 * <i>dirty checking</i>. Konsekuensi praktisnya:</p>
	 * <ul>
	 * <li>Nilai kolom {@code yayasan_id} yang berbeda dari yayasan sekolahnya — mis. hasil impor,
	 *     hasil pindah sekolah antar yayasan, atau pilihan pengguna pada combobox Yayasan di
	 *     formulir — akan <b>ditimpa permanen</b> pada pembacaan berikutnya, tanpa pemberitahuan.
	 *     Sekadar membuka grid Jam Pelajaran sudah cukup untuk memicunya bagi setiap baris yang
	 *     terender.</li>
	 * <li>Penimpaan itu menghasilkan UPDATE nyata dan satu revisi Envers baru, sehingga riwayat
	 *     audit dapat berisi perubahan yang tidak pernah dilakukan manusia.</li>
	 * </ul>
	 *
	 * @return yayasan pemilik hasil penurunan dari sekolah; {@code null} bila sekolah maupun
	 *         yayasan tidak dapat diresolusi
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
	 * Menyetel yayasan pemilik slot ini.
	 *
	 * <p><b>Praktis tanpa pengaruh jangka panjang:</b> nilai apa pun yang disimpan di sini akan
	 * ditimpa {@link #getYayasan()} dengan yayasan milik sekolahnya pada pembacaan berikutnya.
	 * Sama seperti {@link #setSekolah(Sekolah)}, objek ber-id {@code null} diperlakukan sebagai
	 * {@code null}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek ber-id {@code null} menyimpan
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan catatan bebas administrator untuk slot ini.
	 *
	 * <p>Getter polos — tidak ada efek samping, tidak ada nilai bawaan. Kolomnya eksplisit
	 * {@code length = 10000} sehingga teks panjang aman disimpan (bandingkan
	 * {@code JenisJadwalPelajaran.keterangan} yang tanpa panjang eksplisit dan gagal pada teks
	 * lebih dari 255 karakter).</p>
	 *
	 * @return keterangan slot, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", length = 10000)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas administrator. Setter polos, menerima {@code null}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jam mulai slot ini.
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}.</b> Bila field {@link #mulai} masih kosong,
	 * getter ini mengembalikan <b>waktu "sekarang" versi aplikasi</b> ({@code WaktuUtil.getDate()},
	 * lengkap dengan tanggal hari ini) alih-alih {@code null}. Nilai karangan itu tidak disimpan
	 * ke field — jadi bukan getter destruktif — tetapi konsekuensinya nyata dan ada dua:</p>
	 * <ol>
	 * <li><b>Nilai karangan itu ikut tersimpan.</b> Hibernate membaca entity ini lewat
	 *     <i>property access</i>, sehingga saat baris baru di-INSERT kolom {@code mulai} terisi
	 *     jam server saat itu, bukan gagal dengan galat "not null". Slot yang jam mulainya gagal
	 *     diparsing (lihat {@link #setMulaiS(String)}) karena itu <b>lahir diam-diam dengan jam
	 *     acak</b> alih-alih ditolak.</li>
	 * <li><b>Penjaga {@code != null} di pemanggil menjadi mati.</b> Pola
	 *     {@code if (jadwal.getJamPelajaran() != null && jadwal.getJamPelajaran().getMulai() != null)}
	 *     yang muncul di {@code JadwalPelajaran.populateJamPelajaran()} dan
	 *     {@code LaporanJadwalPelajaran} tidak pernah gagal pada bagian keduanya. Bagian itu
	 *     hanya menyaring rujukan slot yang memang {@code null}, bukan slot berjam kosong seperti
	 *     yang tampaknya dimaksud penulisnya.</li>
	 * </ol>
	 *
	 * @return jam mulai slot; bila belum diisi, waktu sekarang versi aplikasi (bukan {@code null})
	 */
	@Temporal(TemporalType.TIME)
	@Column(name = "mulai", nullable = false, length = 15)
	public Date getMulai() {
		return this.mulai == null ? WaktuUtil.getDate() : mulai;
	}

	/**
	 * Menyetel jam mulai slot.
	 *
	 * <p><b>Tidak dapat dikosongkan:</b> argumen {@code null} diabaikan diam-diam sehingga nilai
	 * lama bertahan. Dipanggil {@code JamPelajaranAction.onSave(...)} dari {@code Timebox}
	 * "Waktu *" dan oleh {@link #setMulaiS(String)} setelah parsing berhasil.</p>
	 *
	 * <p><b>Tidak ada validasi urutan:</b> nilai yang lebih akhir daripada {@link #selesai} tetap
	 * diterima, dan tidak ada pemeriksaan tumpang-tindih dengan slot lain (lihat Javadoc kelas).</p>
	 *
	 * @param mulai jam mulai baru; {@code null} diabaikan
	 */
	public void setMulai(Date mulai) {
		if (mulai != null) {
			this.mulai = mulai;
		}
	}

	/**
	 * Mengembalikan nama slot, mis. "Jam ke-1".
	 *
	 * <p>Getter polos. Nama inilah yang menjadi potongan pertama label setiap item combobox slot
	 * pada layar Jadwal Pelajaran dan Guru Mengajar, serta kunci pengurutan grid layar master
	 * ({@code Order.asc("nama")} — pengurutan <i>teks</i>, sehingga "Jam ke-10" berdiri sebelum
	 * "Jam ke-2").</p>
	 *
	 * @return nama slot; wajib terisi untuk baris tersimpan ({@code nullable = false})
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama slot. Setter polos.
	 *
	 * <p>{@code JamPelajaranAction.onSave(...)} menolak nama kosong, tetapi tidak memeriksa
	 * duplikasi: dua slot bernama sama pada sekolah yang sama tetap dapat dibuat.</p>
	 *
	 * @param nama nama slot baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan jam selesai slot ini.
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}</b> — berperilaku persis sama dengan
	 * {@link #getMulai()}: bila field {@link #selesai} kosong, yang dikembalikan adalah waktu
	 * sekarang versi aplikasi, dan nilai karangan itu ikut tersimpan saat INSERT. Baca Javadoc
	 * {@link #getMulai()} untuk kedua konsekuensinya.</p>
	 *
	 * @return jam selesai slot; bila belum diisi, waktu sekarang versi aplikasi (bukan
	 *         {@code null})
	 */
	@Temporal(TemporalType.TIME)
	@Column(name = "selesai", nullable = false, length = 15)
	public Date getSelesai() {
		return this.selesai == null ? WaktuUtil.getDate() : selesai;
	}

	/**
	 * Menyetel jam selesai slot. Sama seperti {@link #setMulai(Date)}: argumen {@code null}
	 * diabaikan sehingga nilai lama bertahan, dan tidak ada validasi bahwa jam selesai berada
	 * setelah jam mulai maupun bahwa rentangnya tidak menindih slot lain.
	 *
	 * @param selesai jam selesai baru; {@code null} diabaikan
	 */
	public void setSelesai(Date selesai) {
		if (selesai != null) {
			this.selesai = selesai;
		}
	}

	/**
	 * Mengembalikan kategori slot ini ({@link JenisJadwalPelajaran} — Reguler/Ujian/Ramadhan/…).
	 *
	 * <p>FK-nya <b>wajib</b> ({@code jenis_jadwal_pelajaran_id}, {@code nullable = false}), dan
	 * inilah satu-satunya kolom di seluruh basis data yang menunjuk ke tabel jenis jadwal — jadi
	 * yang sesungguhnya dikategorikan adalah slot jam, bukan baris jadwal. Karena
	 * {@code JadwalPelajaran} menyimpan dua belas rujukan slot yang masing-masing bebas memilih
	 * jenisnya sendiri, tidak ada mekanisme apa pun yang mencegah satu baris jadwal mencampur
	 * slot "Reguler" dengan slot "Ujian".</p>
	 *
	 * <p><b>Efek samping:</b> resolusi proxy lazy lewat {@code check(...)} dengan penulisan balik
	 * ke field — instance dapat berganti, baris yang ditunjuk tidak.</p>
	 *
	 * @return kategori slot; secara praktis tidak pernah {@code null} untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_jadwal_pelajaran_id", nullable = false)
	public JenisJadwalPelajaran getJenisJadwalPelajaran() {
		jenisJadwalPelajaran = check(jenisJadwalPelajaran);
		return this.jenisJadwalPelajaran;
	}

	/**
	 * Menyetel kategori slot ini.
	 *
	 * <p>Setter polos — <b>tidak</b> memakai penjaga "objek ber-id {@code null} dianggap
	 * {@code null}" seperti {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}. Perbedaan
	 * ini tampaknya tak disengaja, namun berakibat: objek {@link JenisJadwalPelajaran} transient
	 * akan ikut ter-{@code PERSIST} lewat kaskade relasi ini, bukan ditolak.</p>
	 *
	 * @param jenisJadwalPelajaran kategori slot; {@code null} membuat baris gagal INSERT karena
	 *                             FK-nya wajib
	 */
	public void setJenisJadwalPelajaran(JenisJadwalPelajaran jenisJadwalPelajaran) {
		this.jenisJadwalPelajaran = jenisJadwalPelajaran;
	}

	/**
	 * Mengembalikan jam mulai dalam bentuk teks {@code "HH:mm"}, mis. {@code "07:30"}.
	 *
	 * <p><b>Getter dengan efek tulis balik.</b> Bila field {@link #mulai} terisi, nilai teks
	 * dihitung ulang dari sana ({@code Common.timeFormat} berpola {@code "HH:mm"}, {@code Locale}
	 * Indonesia) dan <b>ditimpakan ke field {@link #mulaiS}</b>. Karena {@code mulaiS} ikut
	 * dipetakan ke kolom sendiri (lihat Javadoc kelas), pembacaan ini dapat menghasilkan UPDATE
	 * nyata plus revisi Envers — dan itu terjadi pada setiap baris grid, setiap item combobox
	 * slot di layar Jadwal Pelajaran/Guru Mengajar, serta setiap baris laporan.</p>
	 *
	 * <p><b>Kapan hasilnya bisa "salah":</b> bila {@link #mulai} masih {@code null} (baris baru
	 * yang jam mulainya gagal diparsing), penghitungan ulang dilewati dan yang dikembalikan
	 * adalah string mentah apa adanya. Pada saat yang sama {@link #getMulai()} mengarang jam
	 * sekarang untuk kolom {@code mulai}. Kedua kolom karenanya tersimpan dengan isi yang
	 * berbeda dan tetap berbeda sampai ada yang menyunting slot itu.</p>
	 *
	 * <p><b>Siapa yang membacanya:</b> label combobox slot (12 slot di Jadwal Pelajaran, 25 di
	 * Guru Mengajar), kolom "Mulai" pada ekspor Excel, {@code JadwalDisplayHelper},
	 * {@code CommonReportHelper}, {@code PenjadwalanSiswaHelper} (mengisi {@code waktuMulai}
	 * pertemuan), dan {@code TimetableJadwalPelajaranWindow} (kunci dedup "buat waktu default").</p>
	 *
	 * @return jam mulai sebagai teks {@code "HH:mm"}, atau isi mentah/{@code null} bila
	 *         {@link #mulai} belum terisi
	 */
	public String getMulaiS() {
		if (mulai != null) {
			mulaiS = Common.timeFormat.get().format(mulai);
		}
		return mulaiS;
	}

	/**
	 * Menyetel jam mulai dari teks, sekaligus mencoba mem-parsingnya menjadi {@link #mulai}.
	 *
	 * <p>Ini satu-satunya jalur yang dipakai {@code TimetableJadwalPelajaranWindow} untuk
	 * menyimpan jam (baik pada tombol "buat waktu default" maupun dialog "Kelola Jam Pelajaran"),
	 * dan juga jalur yang dipakai impor Excel karena {@code mulaiS} termasuk kolom impor. Layar
	 * master sendiri tidak memakainya — ia menyetel {@link #setMulai(Date)} langsung dari
	 * {@code Timebox}.</p>
	 *
	 * <p><b>Cara kerja.</b> Teks selalu disimpan apa adanya ke field terlebih dahulu, lalu dua
	 * percobaan parsing dijalankan <b>berdasarkan panjang string</b>:</p>
	 * <ul>
	 * <li>panjang tepat <b>5</b> → diparsing dengan {@code Common.timeFormat} ({@code "HH:mm"}).
	 *     Inilah satu-satunya cabang yang benar-benar bekerja.</li>
	 * <li>panjang tepat <b>8</b> → diparsing dengan {@code Common.dateFormat1}, yang berpola
	 *     <b>{@code "dd-MM-yyyy"}</b> — pola <i>tanggal</i>, bukan jam. Cabang ini hampir pasti
	 *     dimaksudkan untuk {@code "HH:mm:ss"} dan sebagaimana tertulis tidak pernah berguna:
	 *     {@code "07:30:00"} gagal diparsing dan galatnya ditelan. Lebih buruk lagi,
	 *     {@code SimpleDateFormat} bersifat <i>lenient</i>, sehingga masukan 8 karakter yang
	 *     kebetulan menyerupai tanggal (mis. {@code "07-30-20"}) <b>berhasil</b> diparsing menjadi
	 *     tanggal absurd dan menimpa jam mulai slot.</li>
	 * <li>panjang lain ({@code "7:30"}, {@code "07.30"}, {@code "07:30 WIB"}, string kosong) →
	 *     <b>tidak diparsing sama sekali</b>. {@link #mulai} tidak berubah, tidak ada galat, tidak
	 *     ada peringatan. Untuk baris baru artinya {@link #getMulai()} akan mengarang jam sekarang
	 *     saat INSERT.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> menulis {@link #mulaiS} dan berpotensi {@link #mulai}. Argumen
	 * {@code null} menyebabkan {@code NullPointerException} di dalam kedua blok {@code try}, yang
	 * ditelan {@code catch (Exception)} dan dicatat {@code ErrorAuditUtil} — jadi tidak meledak,
	 * tetapi juga tidak menghapus nilai lama {@link #mulai}.</p>
	 *
	 * <p><b>Tidak ada validasi</b> bahwa jam mulai berada sebelum jam selesai maupun bahwa
	 * rentangnya tidak menindih slot lain di sekolah yang sama.</p>
	 *
	 * @param mulaiS teks jam mulai; hanya format {@code "HH:mm"} (5 karakter) yang benar-benar
	 *               diproses
	 */
	public void setMulaiS(String mulaiS) {
		this.mulaiS = mulaiS;

		try {
			if (mulaiS.length() == 5) {
				mulai = Common.timeFormat.get().parse(mulaiS);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/JamPelajaran.java:211");
			// TODO: handle exception
		}

		try {
			if (mulaiS.length() == 8) {
				mulai = Common.dateFormat1.get().parse(mulaiS);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/JamPelajaran.java:219");
			// TODO: handle exception
		}
	}

	/**
	 * Mengembalikan jam selesai dalam bentuk teks {@code "HH:mm"}.
	 *
	 * <p>Padanan {@link #getMulaiS()} untuk sisi selesai, dengan perilaku yang persis sama —
	 * termasuk <b>efek tulis balik</b> ke field {@link #sampaiS} setiap kali dibaca, dan
	 * kemungkinan isi kolom teks berbeda dari kolom {@code selesai} bila {@link #selesai} masih
	 * {@code null}. Baca Javadoc {@link #getMulaiS()} untuk rinciannya.</p>
	 *
	 * <p><b>Perhatikan penamaan yang tidak konsisten:</b> propertinya bernama {@code sampaiS}
	 * (dan kolom grid berlabel "Sampai"), sementara pasangan kanoniknya bernama {@code selesai}.
	 * Kode pemanggil yang mencari {@code "selesaiS"} tidak akan menemukan apa pun.</p>
	 *
	 * @return jam selesai sebagai teks {@code "HH:mm"}, atau isi mentah/{@code null} bila
	 *         {@link #selesai} belum terisi
	 */
	public String getSampaiS() {
		if (selesai != null) {
			sampaiS = Common.timeFormat.get().format(selesai);
		}
		return sampaiS;
	}

	/**
	 * Menyetel jam selesai dari teks, sekaligus mencoba mem-parsingnya menjadi {@link #selesai}.
	 *
	 * <p>Padanan {@link #setMulaiS(String)} untuk sisi selesai — <b>seluruh kuirknya identik</b>:
	 * hanya string sepanjang 5 karakter yang diparsing sebagai {@code "HH:mm"}, cabang 8 karakter
	 * memakai pola tanggal {@code "dd-MM-yyyy"} yang keliru, panjang lain diabaikan diam-diam,
	 * dan kedua galat ditelan. Baca Javadoc {@link #setMulaiS(String)} untuk uraian lengkapnya.</p>
	 *
	 * @param sampaiS teks jam selesai; hanya format {@code "HH:mm"} (5 karakter) yang benar-benar
	 *                diproses
	 */
	public void setSampaiS(String sampaiS) {
		this.sampaiS = sampaiS;

		try {
			if (sampaiS.length() == 5) {
				selesai = Common.timeFormat.get().parse(sampaiS);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/JamPelajaran.java:238");
			// TODO: handle exception
		}

		try {
			if (sampaiS.length() == 8) {
				selesai = Common.dateFormat1.get().parse(sampaiS);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/JamPelajaran.java:246");
			// TODO: handle exception
		}
	}

	/**
	 * Menyetel bentuk angka jam selesai secara langsung. Setter polos.
	 *
	 * <p>Nilai yang disetel di sini <b>tidak bertahan</b>: {@link #getWaktuSelesaiD()} menghitung
	 * ulang dari {@link #getSampaiS()} pada pembacaan berikutnya. Tidak dipanggil dari mana pun
	 * pada alur sekolah.</p>
	 *
	 * @param waktuSelesaiD nilai pseudo-desimal jam selesai
	 */
	public void setWaktuSelesaiD(Double waktuSelesaiD) {
		this.waktuSelesaiD = waktuSelesaiD;
	}

	/**
	 * Mengembalikan jam selesai sebagai angka "pseudo-desimal", mis. 8.19 untuk 08:19.
	 *
	 * <p><b>Ini bukan jam desimal.</b> Nilainya dibentuk dengan mengganti tanda titik dua pada
	 * teks {@code "HH:mm"} menjadi titik desimal, sehingga bagian pecahannya adalah <b>menit apa
	 * adanya</b>, bukan pecahan jam. Akibatnya:</p>
	 * <ul>
	 * <li><b>Pengurutan aman.</b> Karena {@code HH} dan {@code mm} selalu dua digit berpengisi
	 *     nol, urutan numerik nilai ini sama dengan urutan waktu sebenarnya. Inilah satu-satunya
	 *     pemakaian nyata di modul sekolah: {@code Order.asc("waktuMulaiD")} pada
	 *     {@code TimetableJadwalPelajaranWindow}.</li>
	 * <li><b>Aritmetika salah.</b> Selisih 07:30 dan 08:19 menghasilkan 0.89, bukan 49 menit
	 *     maupun 0,82 jam. Jangan pernah memakai nilai ini untuk menghitung durasi, beban jam,
	 *     atau perbandingan rentang.</li>
	 * </ul>
	 *
	 * <p><b>Getter dengan efek tulis balik.</b> Method ini memanggil {@link #getSampaiS()} (yang
	 * sendirinya menulis ke field {@link #sampaiS}), lalu menulis hasil parsing ke field
	 * {@link #waktuSelesaiD}, dan bila hasilnya masih {@code null} <b>menuliskan nilai bawaan
	 * {@code 0.0}</b>. Karena kolomnya dipetakan eksplisit ({@code waktu_selesai_d}), pembacaan
	 * biasa dapat berubah menjadi UPDATE nyata plus revisi Envers.</p>
	 *
	 * <p><b>Kegagalan parsing ditelan diam-diam</b> — berbeda dengan {@link #getWaktuMulaiD()}
	 * yang justru memunculkan dialog galat kepada admin. Ketidaksimetrisan ini disengaja atau
	 * tidak, ia membuat gejala di lapangan tampak acak.</p>
	 *
	 * @return jam selesai sebagai pseudo-desimal; {@code 0.0} bila belum dapat dihitung — perhatikan
	 *         bahwa {@code 0.0} juga merupakan nilai sah untuk pukul 00:00
	 */
	@Column(name = "waktu_selesai_d", length = 15, precision = 15, scale = 2)
	public Double getWaktuSelesaiD() {
		sampaiS = getSampaiS();
		if (sampaiS != null && !sampaiS.trim().equals("")) {
			try {
				waktuSelesaiD = Double.parseDouble(sampaiS.trim().replaceAll(":", ".").replaceAll(",", "."));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/JamPelajaran.java:261");
				// TODO Auto-generated catch block
				// Common.tampilErrorJikaAdmin(e);
			}
		}
		if (waktuSelesaiD == null) {
			waktuSelesaiD = 0.0;
		}
		return waktuSelesaiD;
	}

	/**
	 * Menyetel bentuk angka jam mulai secara langsung. Setter polos.
	 *
	 * <p>Seperti {@link #setWaktuSelesaiD(Double)}, nilainya tidak bertahan karena
	 * {@link #getWaktuMulaiD()} menghitung ulang pada pembacaan berikutnya.</p>
	 *
	 * @param waktuMulaiD nilai pseudo-desimal jam mulai
	 */
	public void setWaktuMulaiD(Double waktuMulaiD) {
		this.waktuMulaiD = waktuMulaiD;
	}

	/**
	 * Mengembalikan jam mulai sebagai angka "pseudo-desimal", mis. 7.30 untuk 07:30.
	 *
	 * <p>Semua penjelasan pada {@link #getWaktuSelesaiD()} berlaku sama di sini — bukan jam
	 * desimal, aman untuk mengurutkan, salah untuk berhitung, dan menulis balik ke field
	 * (termasuk nilai bawaan {@code 0.0}) sehingga pembacaan biasa dapat menjadi UPDATE nyata.</p>
	 *
	 * <p><b>Satu perbedaan yang perlu diketahui:</b> kegagalan parsing di sini <b>tidak</b>
	 * ditelan diam-diam melainkan diteruskan ke {@code Common.tampilErrorJikaAdmin(e)}, yang
	 * memunculkan dialog galat kepada pengguna admin. Kode kedua getter selain baris itu identik,
	 * jadi ini sisa salin-tempel yang tidak dirapikan. Bila seorang admin melaporkan "muncul error
	 * saat membuka layar jadwal", slot berjam tak-terparsing adalah tersangka pertama.</p>
	 *
	 * <p><b>Kolom inilah kunci pengurutan slot</b> pada {@code TimetableJadwalPelajaranWindow}
	 * ({@code addOrder(Order.asc("waktuMulaiD"))}). Baris yang belum pernah ter-flush setelah
	 * jam mulainya diisi masih menyimpan {@code NULL}/{@code 0.0} di kolom itu dan karenanya
	 * melompat ke ujung urutan kisi timetable.</p>
	 *
	 * @return jam mulai sebagai pseudo-desimal; {@code 0.0} bila belum dapat dihitung
	 */
	@Column(name = "waktu_mulai_d", length = 15, precision = 15, scale = 2)
	public Double getWaktuMulaiD() {
		mulaiS = getMulaiS();
		if (mulaiS != null && !mulaiS.trim().equals("")) {
			try {
				waktuMulaiD = Double.parseDouble(mulaiS.trim().replaceAll(":", ".").replaceAll(",", "."));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}
		if (waktuMulaiD == null) {
			waktuMulaiD = 0.0;
		}
		return waktuMulaiD;
	}

	/**
	 * Mengembalikan status aktif slot ini, dengan {@code NULL} dipetakan menjadi {@code true}.
	 *
	 * <p>Pemetaan itu penting karena {@code JamPelajaranAction.onSave(...)} <b>tidak pernah</b>
	 * memanggil {@link #setAktif(Boolean)} — formulir tambah/ubah tidak punya kotak centang Aktif
	 * sama sekali. Setiap baris yang lahir dari layar master karenanya tersimpan dengan
	 * {@code aktif = NULL}, dan getter ini membuatnya <i>tampak</i> aktif di grid (kotak centang
	 * tercentang).</p>
	 *
	 * <p><b>Jebakan:</b> pembaca yang menulis tapisnya toleran-NULL
	 * ({@code Restrictions.or(isNull("aktif"), eq("aktif", true))} — dipakai layar Jadwal
	 * Pelajaran dan Guru Mengajar) memang ikut menampilkannya, tetapi
	 * {@code TimetableJadwalPelajaranWindow} memakai {@code Restrictions.eq("aktif", Boolean.TRUE)}
	 * yang <b>ketat</b> dan karenanya <b>melewatkan seluruh slot ber-{@code aktif = NULL}</b>.
	 * Gejala di lapangan: slot yang baru dibuat muncul di combobox jadwal tetapi kisi timetable
	 * tetap kosong. Menyalakan-lalu-mematikan-lalu-menyalakan kotak centang Aktif di grid master
	 * memaksa nilai {@code true} sungguhan tersimpan dan menyelesaikan gejala itu.</p>
	 *
	 * @return {@code true} bila slot aktif atau kolomnya masih {@code NULL}; {@code false} hanya
	 *         bila dinonaktifkan eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif slot ini.
	 *
	 * <p>Satu-satunya pemanggil pada alur sekolah adalah kotak centang "Aktif" di grid layar
	 * master, yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate(jamPelajaran)}
	 * begitu diklik — tanpa dialog konfirmasi. Kotak centang itu <b>digerbangi dengan benar</b>
	 * ({@code checkbox.setDisabled(!edit)}, dengan {@code edit} berasal dari
	 * {@code CommonPrivilages.checkPrevilages(UPDATE)}). {@code TimetableJadwalPelajaranWindow}
	 * juga memanggilnya, tetapi hanya untuk slot yang baru dibuatnya.</p>
	 *
	 * <p><b>Menonaktifkan slot tidak membatalkan jadwal yang sudah memakainya</b> — baris
	 * {@code JadwalPelajaran} tetap menunjuk ke slot ini dan tetap tercetak; yang hilang hanyalah
	 * kehadirannya di combobox pemilihan berikutnya.</p>
	 *
	 * @param aktif status aktif baru; {@code null} dibaca sebagai aktif oleh {@link #getAktif()}
	 *              tetapi disaring keluar oleh tapis ketat helper timetable
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kelompok pencetakan slot ini ({@link KelompokJamPelajaran}) — dimensi
	 * pengelompokan <b>opsional</b>, berbeda dari {@link #getJenisJadwalPelajaran()} yang wajib.
	 *
	 * <p>FK-nya {@code kelompok_jam_pelajaran_id} dengan {@code nullable = true} dinyatakan
	 * eksplisit. Pembaca "bisnis" satu-satunya adalah
	 * {@code report.format1.sekolah.LaporanJadwalPelajaran}, yang memakai kelompok milik slot
	 * untuk memecah cetak jadwal menjadi blok terpisah; dua kuirk laporan itu (slot ke-11/12 tidak
	 * masuk kriteria seleksi, dan baris yang menyentuh dua kelompok dipanen berulang) tercatat
	 * pada Javadoc {@link KelompokJamPelajaran}.</p>
	 *
	 * <p><b>Efek samping:</b> resolusi proxy lazy lewat {@code check(...)} dengan penulisan balik
	 * ke field.</p>
	 *
	 * @return kelompok slot, atau {@code null} bila slot tidak dikelompokkan (kondisi normal —
	 *         instalasi baru tidak punya satu kelompok pun)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_jam_pelajaran_id", nullable = true)
	public KelompokJamPelajaran getKelompokJamPelajaran() {
		kelompokJamPelajaran = check(kelompokJamPelajaran);
		return kelompokJamPelajaran;
	}

	/**
	 * Menyetel kelompok pencetakan slot ini.
	 *
	 * <p>Setter polos yang menerima {@code null} — dan {@code JamPelajaranAction.onSave(...)}
	 * memang menyetel {@code null} bila combobox "Kelompok Jam Pelajaran" tidak dipilih. Sama
	 * seperti {@link #setJenisJadwalPelajaran(JenisJadwalPelajaran)}, tidak ada penjaga "objek
	 * ber-id {@code null}".</p>
	 *
	 * @param kelompokJamPelajaran kelompok baru; {@code null} berarti tidak dikelompokkan
	 */
	public void setKelompokJamPelajaran(KelompokJamPelajaran kelompokJamPelajaran) {
		this.kelompokJamPelajaran = kelompokJamPelajaran;
	}

	/**
	 * Mengembalikan bobot beban mengajar slot ini dalam satuan JP, dengan {@code NULL} dipetakan
	 * menjadi {@code 1.0}.
	 *
	 * <p>Kolomnya bernama {@code jp_data} (bukan {@code jp} — kemungkinan besar untuk menghindari
	 * tabrakan dengan kata terpakai), dan formulir layar master menyebutnya "Jumlah JP&nbsp;*".</p>
	 *
	 * <p><b>Ke mana angka ini mengalir.</b> {@code GuruAction} menjumlahkan {@code getJp()}
	 * seluruh slot yang seorang guru pegang pada kedua belas posisi
	 * {@code JadwalPelajaran.jamPelajaranN} — dan hanya untuk posisi yang guru-nya benar-benar
	 * dia ({@code jadwalPelajaran.getGuruN().getId().equals(guru.getId())}) — menjadi angka
	 * "jumlah JP" yang tampil di layar guru. Karena {@code NULL} dibaca sebagai {@code 1.0},
	 * slot yang jumlah JP-nya tidak pernah diisi <b>tetap menambah satu JP</b> ke beban guru
	 * alih-alih nol; ini nilai bawaan yang masuk akal untuk slot tunggal, tetapi menyesatkan untuk
	 * slot blok ganda yang lupa diisi.</p>
	 *
	 * <p>Nilainya bertipe {@code Double} sehingga bobot pecahan (mis. 0,5 JP) dimungkinkan;
	 * grid menampilkannya lewat {@code Common.numberFormat}.</p>
	 *
	 * @return bobot JP slot; {@code 1.0} bila kolomnya masih {@code NULL}
	 */
	@Column(name = "jp_data")
	public Double getJp() {
		return jp == null ? 1.0 : jp;
	}

	/**
	 * Menyetel bobot beban mengajar slot ini dalam satuan JP. Setter polos.
	 *
	 * <p>Diisi {@code JamPelajaranAction.onSave(...)} dari {@code MyDoublebox} "Jumlah JP".
	 * Tidak ada validasi nilai: nol maupun bilangan negatif diterima dan akan ikut mengurangi
	 * jumlah beban mengajar guru yang dihitung {@code GuruAction}.</p>
	 *
	 * @param jp bobot JP baru; {@code null} dibaca sebagai {@code 1.0} oleh {@link #getJp()}
	 */
	public void setJp(Double jp) {
		this.jp = jp;
	}
}
