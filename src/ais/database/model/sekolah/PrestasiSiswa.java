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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;

/**
 * Entity TRANSAKSI <b>prestasi siswa</b> — tabel <code>sekolah.prestasi_siswa</code>.
 *
 * <p><b>Domain TERVERIFIKASI dari kode, bukan dugaan.</b> Satu baris kelas ini adalah satu
 * <i>catatan kejuaraan/lomba yang diikuti seorang siswa</i>, lengkap dengan capaian yang diraihnya.
 * Label formulir di {@code ais.action.master.sekolah.PrestasiSiswaAction.init(PrestasiSiswa)} yang
 * membuktikannya: {@code nama} = "Nama Kejuaraan", {@code tempat} = "Tempat Kejuaraan",
 * {@code penyelenggara} = "Penyelenggara", {@code juara} = "Juara ke", {@code tanggal} /
 * {@code tanggalSelesai} = "Tanggal Kejuaraan" (rentang mulai s.d. selesai), {@code nomorSertifikat}
 * = "Nomor Sertifikat Prestasi", {@code url} = "Link / URL", {@code alamat} = "Lokasi / Alamat".
 * Jadi ini BUKAN katalog penghargaan, BUKAN nilai ekstrakurikuler, dan BUKAN kolom teks
 * {@code prestasiSiswa1..3} yang ada di {@link ais.database.model.sekolah.Siswa} (tiga kolom teks
 * bebas di biodata itu tidak punya relasi apa pun ke tabel ini dan tidak pernah disinkronkan).</p>
 *
 * <h3>Posisi dalam rantai prestasi siswa</h3>
 * <p>Kelas ini adalah <b>tabel fakta</b> yang menggantung pada dua katalog master yang saling
 * melengkapi, keduanya lewat FK yang boleh {@code null}:</p>
 * <ul>
 * <li>{@link ais.database.model.sekolah.KategoriPrestasiSiswa} (kolom
 *     <code>kategori_prestasi_siswa</code>) menjawab <b>seberapa tinggi tingkatnya</b> —
 *     Internasional / Nasional / Regional / Kab&#47;Kota / Kecamatan / Kampus-Sekolah / Lain-Lain.
 *     Meskipun namanya "kategori", isinya cakupan kejuaraan, bukan Akademik&#47;Non-Akademik.</li>
 * <li>{@link ais.database.model.sekolah.CabangPrestasiSiswa} (kolom
 *     <code>cabang_prestasi_siswa</code>) menjawab <b>di bidang apa</b> — Seni, Olah Raga,
 *     Kejuaraan Ilmiah, Lain-Lain.</li>
 * </ul>
 * <p>Kedua katalog itu di-<i>seed</i> otomatis oleh {@code PrestasiSiswaAction.doAfterCompose()}
 * pada kunjungan pertama ke layar, bukan oleh skrip migrasi. Relasinya searah: katalog tidak punya
 * koleksi balik ke kelas ini.</p>
 *
 * <h3>Bukti fisik prestasi TIDAK disimpan di entity ini</h3>
 * <p>Tidak ada kolom foto, scan, atau berkas sertifikat di sini — yang ada hanya
 * {@code nomorSertifikat} berupa teks. Lampiran "Scan / foto bukti prestasi" disimpan sebagai baris
 * {@link ais.database.model.file.LampiranLain} dengan <code>ref</code> = {@link #getId()} dan
 * <code>jenis</code> = nama lengkap kelas ini ({@code PrestasiSiswa.class.getName()}). Karena
 * tautannya berupa pasangan (ref, jenis) dan bukan FK sungguhan, menghapus baris prestasi TIDAK
 * menghapus lampirannya — berkasnya menjadi yatim di tabel lampiran.</p>
 *
 * <h3>Alur status persetujuan</h3>
 * <p>Kolom {@code status} memakai empat konstanta yang dideklarasikan di kelas ini:
 * {@link #BELUM_DIPROSES} &rarr; {@link #SEDANG_DIPROSES} &rarr; {@link #DISETUJUI} atau
 * {@link #DITOLAK}. Statusnya bukan sekadar label:</p>
 * <ul>
 * <li>{@link #DISETUJUI} adalah syarat sebuah prestasi ikut tercetak di rapor
 *     ({@code ais.action.report.format1.sekolah.LaporanRaporSiswa}, disaring bersama
 *     {@code tahunAkademik} dan {@code jenisSemester}).</li>
 * <li>{@link #DISETUJUI} juga satu-satunya status yang dihitung dashboard rekap (klausa
 *     <code>aaa.status='Disetujui'</code> pada SQL native-nya).</li>
 * <li>Di layar daftar, baris ber-status {@link #DISETUJUI} menyembunyikan grup tombol aksi
 *     (Ubah&#47;Hapus) — lihat catatan "kunci semu" di bawah.</li>
 * <li>Konstanta yang sama dipinjam entity tetangga: {@code KegiatanKesiswaanAction} dan
 *     {@code KegiatanKesiswaanPunyaSiswaHelper} membandingkan status kegiatan kesiswaan dengan
 *     {@code PrestasiSiswa.DISETUJUI}, jadi mengubah nilai literal konstanta di sini akan merusak
 *     modul kegiatan kesiswaan juga.</li>
 * </ul>
 *
 * <h3>Kolom yayasan/sekolah BUKAN kolom tenant</h3>
 * <p>Ini jebakan yang mudah salah baca. {@link #getYayasan()} dan {@link #getSekolah()} hanya diisi
 * bila centang "Apakah kejuaraan diluar sekolah?" ({@code prestasiLuarKampus}) DILEPAS, artinya
 * kedua kolom itu mencatat <i>penyelenggara internal</i> kejuaraan, bukan pemilik data. Penyaringan
 * tenant di layar justru dilakukan lewat {@code siswa.sekolah} dan {@code sekolah.yayasan}
 * (alias hasil {@code createAlias}), bukan lewat dua kolom ini. Dashboard rekap pun menggabungkan
 * berdasarkan <code>m.sekolah_id</code> (sekolah SISWA), bukan <code>aaa.sekolah</code>.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><i>Konstanta status</i> — {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES},
 *     {@link #DISETUJUI}, {@link #DITOLAK}.</li>
 * <li><i>Identitas &amp; representasi</i> — konstruktor {@link #PrestasiSiswa()},
 *     {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 * <li><i>Jejak audit warisan</i> — {@link #getOleh()}/{@link #setOleh(String)},
 *     {@link #getOlehId()}/{@link #setOlehId(String)},
 *     {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, callback {@link #onUpdate()}.</li>
 * <li><i>Identitas kejuaraan</i> — {@link #getNama()}/{@link #setNama(String)},
 *     {@link #getNamaEn()}/{@link #setNamaEn(String)},
 *     {@link #getPenyelenggara()}/{@link #setPenyelenggara(String)},
 *     {@link #getTempat()}/{@link #setTempat(String)},
 *     {@link #getAlamat()}/{@link #setAlamat(String)},
 *     {@link #getUrl()}/{@link #setUrl(String)}.</li>
 * <li><i>Waktu &amp; periode akademik</i> — {@link #getTanggal()}/{@link #setTanggal(Date)},
 *     {@link #getTanggalSelesai()}/{@link #setTanggalSelesai(Date)},
 *     {@link #getTahun()}/{@link #setTahun(Integer)},
 *     {@link #getTahunAkademik()}/{@link #setTahunAkademik(String)},
 *     {@link #getJenisSemester()}/{@link #setJenisSemester(String)}.</li>
 * <li><i>Capaian</i> — {@link #getJuara()}/{@link #setJuara(String)},
 *     {@link #getPeringkat()}/{@link #setPeringkat(Integer)},
 *     {@link #getCapaian()}/{@link #setCapaian(String)},
 *     {@link #getJumlahPeserta()}/{@link #setJumlahPeserta(String)},
 *     {@link #getNomorSertifikat()}/{@link #setNomorSertifikat(String)},
 *     {@link #getNoSk()}/{@link #setNoSk(String)}, {@link #getTglSk()}/{@link #setTglSk(Date)}.</li>
 * <li><i>Klasifikasi</i> — {@link #getKategoriPrestasiSiswa()}/{@link #setKategoriPrestasiSiswa(KategoriPrestasiSiswa)},
 *     {@link #getCabangPrestasiSiswa()}/{@link #setCabangPrestasiSiswa(CabangPrestasiSiswa)},
 *     {@link #getPrestasiLuarKampus()}/{@link #setPrestasiLuarKampus(Boolean)}.</li>
 * <li><i>Pelaku &amp; pembina</i> — {@link #getSiswa()}/{@link #setSiswa(Siswa)},
 *     {@link #getKelasSiswa()}/{@link #setKelasSiswa(KelasSiswa)},
 *     {@link #getGuru()}/{@link #setGuru(Guru)}, {@link #getGuru2()}/{@link #setGuru2(Guru)},
 *     {@link #getGuru3()}/{@link #setGuru3(Guru)}.</li>
 * <li><i>Konteks penyelenggaraan internal</i> — {@link #getYayasan()}/{@link #setYayasan(Yayasan)},
 *     {@link #getSekolah()}/{@link #setSekolah(Sekolah)}.</li>
 * <li><i>Alur kerja</i> — {@link #getStatus()}/{@link #setStatus(String)},
 *     {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyunting kelas ini</h3>
 * <ol>
 * <li><b>Warisan {@link ais.database.model.GeneralValueObject} bukan {@code @MappedSuperclass}.</b>
 *     Kelas induk adalah POJO abstrak biasa sehingga Hibernate TIDAK memetakan propertinya. Karena
 *     itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sengaja
 *     DIDEKLARASIKAN ULANG di sini — ini keharusan teknis pemetaan, bukan duplikasi yang bisa
 *     dibersihkan. Jangan menghapusnya.</li>
 * <li><b>Pemetaan berbasis <i>property access</i> + {@code dynamicUpdate}.</b> Anotasi {@code @Id}
 *     berada di {@link #getId()}, jadi Hibernate membaca SEMUA nilai lewat getter — termasuk saat
 *     pemeriksaan kotor sebelum flush. Konsekuensinya setiap getter yang mengembalikan nilai
 *     berbeda dari isi kolom akan MENULIS nilai itu ke basis data begitu baris tersentuh dalam
 *     sesi yang dapat menulis. Enam getter di kelas ini berperilaku begitu; lihat nomor 3-5.</li>
 * <li><b>{@link #getKelasSiswa()} adalah getter DESTRUKTIF yang menstempel ulang riwayat.</b> Bila
 *     kolomnya kosong, getter mengisinya dari {@code getSiswa().getKelas()} — kelas siswa
 *     <i>saat ini</i>, bukan kelas saat prestasi diraih. Membuka layar prestasi setelah siswa naik
 *     kelas akan menulis kelas baru ke baris prestasi lama.</li>
 * <li><b>{@link #getTahunAkademik()} dan {@link #getJenisSemester()} juga destruktif.</b> Keduanya
 *     mengisi periode dari JAM SERVER saat dibaca bila kolomnya kosong. Karena rapor menyaring
 *     prestasi berdasarkan pasangan tahun akademik + semester, baris lama yang periodenya kosong
 *     akan "pindah" ke periode berjalan begitu tersentuh, lalu ikut tercetak di rapor semester ini.
 *     {@link #getTahun()} menyusul: ia menurunkan {@code tahun} dari {@code tahunAkademik} dan
 *     menimpa apa pun yang pernah di-{@code set} pemanggil.</li>
 * <li><b>Empat getter melakukan normalisasi senyap.</b> {@link #getJumlahPeserta()},
 *     {@link #getCapaian()}, dan {@link #getUrl()} mengubah {@code null} menjadi string kosong;
 *     {@link #getPeringkat()} mengubah {@code null} menjadi {@code 0}; dan
 *     {@link #getPrestasiLuarKampus()} mengubah {@code null} menjadi {@code true}. Perubahan itu
 *     ikut tersimpan pada flush berikutnya. Yang terakhir paling berdampak: baris hasil impor Excel
 *     yang tidak mengisi kolom tersebut otomatis dianggap "kejuaraan di luar sekolah".
 *     {@link #getStatus()} sengaja TIDAK menulis balik, sehingga kolom {@code status} bisa tetap
 *     {@code null} di basis data walau layar menampilkan "Belum diproses" — dan baris seperti itu
 *     tidak akan pernah cocok dengan filter <code>status='Disetujui'</code> di SQL rekap.</li>
 * <li><b>{@link #toString()} membaca field {@code nama} langsung, bukan {@link #getNama()}</b>,
 *     sehingga hasilnya tidak ter-<i>trim</i> — berbeda dari nilai yang dilihat Hibernate.</li>
 * <li><b>Bug tampilan: baris dengan <code>kelas_siswa</code> kosong TIDAK PERNAH muncul di
 *     layarnya sendiri.</b> {@code PrestasiSiswaAction.initCriteria(boolean)} memanggil
 *     {@code createAlias("kelasSiswa", "kelasSiswa")} tanpa tipe join, yang di Hibernate Criteria
 *     berarti INNER JOIN, padahal kolomnya nullable. Prestasi milik siswa yang belum punya baris
 *     {@link ais.database.model.sekolah.KelasSiswa} (mutasi, siswa baru, hasil impor massal) hilang
 *     diam-diam dari daftar DAN dari hitungan paging, tanpa pesan galat.</li>
 * <li><b>Anotasi {@code @Audited} (Envers) aktif</b>, jadi setiap perubahan tercatat di tabel
 *     revisi dan ditampilkan tombol Revisi pada layar daftar. Operasi massal berbasis SQL native
 *     (mis. rekap dashboard) tentu tidak melewati Envers, tetapi kelas ini memang tidak punya jalur
 *     tulis massal semacam itu.</li>
 * <li><b>Komentar generator "Bank generated by hbm2java" pada versi lama adalah salah salin.</b>
 *     Kelas ini tidak ada hubungannya dengan entity Bank; string yang sama tersalin ke belasan
 *     berkas lain di repositori. Komentar itu digantikan Javadoc ini.</li>
 * </ol>
 *
 * <h3>CATATAN KEAMANAN &amp; PRIVASI — hasil verifikasi ulang dari sisi entity ini</h3>
 * <p>Seluruh butir di bawah diverifikasi langsung pada kode pengelola entity ini
 * ({@code PrestasiSiswaAction}, {@code DashboardRekapPrestasiSiswa}, {@code DasbordPrestasi}),
 * bukan dikutip dari laporan lain. Entity ini sendiri tidak melakukan pemeriksaan hak akses apa pun
 * — seluruh gerbang ada di lapisan Action, dan di sanalah masalahnya.</p>
 * <ol>
 * <li><b>Gerbang hak akses DIKOMENTARI TOTAL — TERVERIFIKASI MASIH BERLAKU.</b> Di
 *     {@code PrestasiSiswaAction.doAfterCompose(Component)} baris pemasangan
 *     {@code CommonPrivilages.checkPrevilages(CREATE)} serta variabel {@code edit}/{@code delete}
 *     dikomentari; tombol Tambah hanya diberi {@code add.setVisible(tbmuser != null)}. Di
 *     {@code PrestasiSiswaRenderer.render(Row, Object)}, {@code button.setVisible(edit)} dan
 *     {@code button.setVisible(delete)} juga dikomentari. Hasil akhirnya: <b>hak BACA saja sudah
 *     cukup untuk menambah, mengubah, dan menghapus catatan prestasi siapa pun yang tampil di
 *     layar.</b></li>
 * <li><b>Fail-open cakupan orang tua — TERVERIFIKASI MASIH BERLAKU.</b> Penyempitan data untuk akun
 *     wali murid dipasang sebagai
 *     {@code if (tbmuser.getOrangTua() != null &amp;&amp; !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty())}.
 *     {@code ambilAnakSiswa()} mem-parsing kolom JSON {@code anak} dan MENELAN setiap exception,
 *     mengembalikan daftar kosong. Kolom {@code anak} kosong, JSON rusak, atau kunci tidak berawalan
 *     "siswa" &rarr; daftar kosong &rarr; syarat gagal &rarr; <b>tidak ada pembatasan sama sekali</b>,
 *     sehingga wali murid melihat prestasi SELURUH siswa satu instalasi (nama, NIS, foto, kelas)
 *     — bukan hanya anaknya.</li>
 * <li><b>IDOR lewat parameter URL {@code ?siswa=} — temuan tambahan.</b>
 *     {@code doAfterCompose} membaca {@code execution.getParameter("siswa")} dan memuat
 *     {@link ais.database.model.sekolah.Siswa} dengan id itu <i>tanpa satu pun pemeriksaan
 *     kepemilikan</i>, lalu memakainya sebagai penyaring daftar sekaligus nilai bawaan formulir.
 *     Akun siswa yang seharusnya terkunci pada dirinya sendiri ({@code mhs = tbmuser.getSiswa()})
 *     cukup menambahkan {@code ?siswa=&lt;id lain&gt;} untuk melihat — dan, karena butir 1, juga
 *     mengubah, menghapus, atau menambah — prestasi siswa lain. Parameter {@code ?prestasi=},
 *     {@code ?sekolah=}, {@code ?cabangPrestasiSiswa=}, {@code ?kategoriPrestasiSiswa=} diperlakukan
 *     sama longgarnya; {@code ?prestasi=} bahkan memaksa satu baris tampil di puncak daftar
 *     ({@code prestasiSiswaSelected}) <b>melewati seluruh klausa penyaring</b>, termasuk pembatasan
 *     anak untuk wali murid.</li>
 * <li><b>Persetujuan tanpa gerbang.</b> Combobox status pada setiap baris dirender untuk semua
 *     pengguna non-siswa ({@code mhs == null &amp;&amp; tbmuser != null}) — termasuk akun wali murid
 *     dan guru — dan menulis langsung lewat {@code Common.refreshUpdate(prestasiSiswa)} tanpa
 *     konfirmasi maupun pemeriksaan hak. Artinya pihak yang tidak berwenang dapat menetapkan
 *     {@link #DISETUJUI}, yang berarti memasukkan prestasi ke rapor dan ke rekap resmi.</li>
 * <li><b>"Disetujui" adalah kunci semu.</b> Grup tombol Ubah&#47;Hapus disembunyikan saat status
 *     {@link #DISETUJUI}, tetapi combobox status TIDAK ikut disembunyikan. Siapa pun dapat
 *     mengembalikan status ke "Belum diproses", tombol aksi muncul lagi, lalu barisnya diubah atau
 *     dihapus. Penguncian pasca-persetujuan efektif tidak ada.</li>
 * <li><b>Tombol Cetak dan Upload tanpa gerbang hak.</b> Tombol ekspor dari
 *     {@code Common.cetakData(this, contents)} disisipkan lewat {@code Common.appendKeToolbar}, yang
 *     tidak menyalin {@code isVisible()} dari tombol jangkarnya (pola yang sama sudah tercatat pada
 *     modul lain). Tombol "Upload Data Siswa" hanya disaring
 *     {@code tbmuser.getSiswa() == null &amp;&amp; tbmuser.ambilGuru() == null} — syarat yang juga
 *     dipenuhi akun wali murid. Daftar kolom yang diekspor/diimpor menyertakan {@code "id"},
 *     sehingga unggahan Excel dapat menimpa baris yang sudah ada, termasuk milik siswa lain.</li>
 * <li><b>Tab Dasbor membocorkan lintas-siswa untuk wali murid.</b> {@code doAfterCompose} memanggil
 *     {@code onDasbor(null)} untuk SETIAP pengunjung, yang memasang
 *     {@code DasbordPrestasi(Lingkup.SISWA)}. Dashboard itu hanya mengenal dua penyempit:
 *     {@code user.getSiswa()} dan {@code user.ambilGuru()}. Akun wali murid tidak punya keduanya,
 *     sehingga kriterianya berjalan tanpa pembatasan dan menarik hingga 800 baris prestasi terbaru
 *     SELURUH siswa berikut namanya — bahkan ketika penyempit anak pada daftar utama bekerja
 *     dengan benar.</li>
 * <li><b>Entity ini adalah TABEL FAKTA dari SQL injection dashboard rekap — TERVERIFIKASI.</b>
 *     {@code ais.action.master.dashboard.helper.DashboardRekapPrestasiSiswa.initSpreadsheet()}
 *     menyusun query native dengan <code>from sekolah.prestasi_siswa aaa</code> sebagai FROM utama,
 *     lalu menyisipkan {@code Common.getBahasaConfig(nama katalog)} MENTAH ke dalam alias kolom
 *     berkutip ganda. Nama baris yang disisipkan berasal dari
 *     {@link ais.database.model.sekolah.KategoriPrestasiSiswa} atau
 *     {@link ais.database.model.sekolah.CabangPrestasiSiswa} — dan kolom yang dikelompokkan
 *     ({@code kolomGroup}) maupun kolom yang dijumlahkan ({@code namaKolom},
 *     yaitu <code>kategori_prestasi_siswa</code> / <code>cabang_prestasi_siswa</code>) adalah kolom
 *     milik entity INI. Satu tanda kutip ganda pada nama katalog sudah cukup untuk keluar dari alias
 *     dan menyambung SQL. Pintu masuknya pun ada di layar yang sama: tab "Cabang Prestasi Siswa" dan
 *     "Kategori Prestasi Siswa" menyisipkan halaman CRUD katalog tersebut. Parameter filter program
 *     ({@code m.program = '...'}) juga dirangkai sebagai literal string tanpa <i>escape</i>.</li>
 * <li><b>Pewarisan hak lewat menu induk.</b> Satu hak menu "Prestasi Siswa" membuka enam tab dalam
 *     satu halaman: daftar transaksi, CRUD katalog kategori, CRUD katalog cabang, dan dua dashboard
 *     rekap. Katalog tersebut GLOBAL satu instalasi, sehingga pemberian hak atas layar transaksi ini
 *     otomatis memberi kendali atas master yang dipakai seluruh sekolah.</li>
 * </ol>
 * <p>Butir-butir di atas memperkuat temuan audit yang sudah tercatat (gerbang hak yang hilang,
 * fail-open cakupan tenant, SQL injection lewat nama katalog); tidak ada kategori kerentanan baru
 * yang perlu dibuka sebagai temuan tersendiri.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.KategoriPrestasiSiswa
 * @see ais.database.model.sekolah.CabangPrestasiSiswa
 * @see ais.database.model.sekolah.Siswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "prestasi_siswa")
public class PrestasiSiswa extends GeneralValueObject {

	/**
	 * Status awal sebuah pengajuan prestasi: <b>"Belum diproses"</b>.
	 *
	 * <p>Nilai ini juga dikembalikan {@link #getStatus()} sebagai pengganti bila kolom
	 * {@code status} kosong di basis data, sehingga baris lama tampak konsisten di layar meski
	 * kolomnya masih {@code null}.</p>
	 */
	public static final String BELUM_DIPROSES = "Belum diproses";

	/** Status antara: pengajuan sedang ditinjau petugas — <b>"Sedang diproses"</b>. */
	public static final String SEDANG_DIPROSES = "Sedang diproses";

	/**
	 * Status akhir positif: <b>"Disetujui"</b>.
	 *
	 * <p>Satu-satunya status yang membuat baris ikut tercetak di rapor
	 * ({@code LaporanRaporSiswa}) dan ikut dihitung dashboard rekap. Konstanta ini juga dipinjam
	 * modul kegiatan kesiswaan ({@code KegiatanKesiswaanAction},
	 * {@code KegiatanKesiswaanPunyaSiswaHelper}) untuk membandingkan status entity lain, jadi
	 * mengubah literalnya berdampak lintas modul.</p>
	 */
	public static final String DISETUJUI = "Disetujui";

	/** Status akhir negatif: pengajuan ditolak petugas — <b>"Ditolak"</b>. */
	public static final String DITOLAK = "Ditolak";

	/** Versi serialisasi Java; jangan diubah agar sesi/cache lama tetap terbaca. */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama; dideklarasikan ulang karena kelas induk tidak dipetakan Hibernate. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit warisan). */
	private String oleh;

	/** Identitas (id akun) pengguna terakhir yang mengubah baris ini (jejak audit warisan). */
	private String olehId;

	/**
	 * Mengembalikan identitas akun terakhir yang mengubah baris ini.
	 *
	 * @return id akun pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas akun pengubah terakhir.
	 *
	 * <p><b>Efek samping:</b> nilai {@code null}, kosong, atau berisi spasi saja DIABAIKAN — field
	 * lama dipertahankan. Ini disengaja agar jejak audit tidak pernah terhapus oleh proses yang
	 * kebetulan menyimpan entity tanpa konteks pengguna (mis. impor massal atau callback).</p>
	 *
	 * @param olehId id akun pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #setOlehId(String)} — nilai kosong diabaikan
	 * sehingga jejak lama tidak tertimpa.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui stempel waktu audit sesaat sebelum UPDATE.
	 *
	 * <p>Dipanggil Hibernate, bukan kode aplikasi. Pekerjaannya didelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(Object)}, yang mengisi
	 * {@code tanggal_dirubah} beserta {@code oleh}/{@code olehId} dari konteks pengguna aktif bila
	 * tersedia.</p>
	 * <p>Pada baris yang sama dideklarasikan field {@code tanggal_dirubah} — stempel waktu
	 * perubahan terakhir, diinisialisasi ke waktu pembuatan object. Penempatannya menempel di
	 * belakang method ini adalah gaya asli berkas (hasil penyisipan otomatis) dan sengaja
	 * dipertahankan agar diff tetap bersih.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi — {@link #onUpdate()} yang mengisinya. Menyetel
	 * manual berguna hanya pada migrasi data yang ingin mempertahankan stempel asli.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object baru karena
	 *         diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas baris ini dalam bentuk {@code "<id>-<nama kejuaraan>"}.
	 *
	 * <p><b>Catatan:</b> method ini membaca field {@code nama} LANGSUNG, bukan lewat
	 * {@link #getNama()}, sehingga hasilnya tidak ter-<i>trim</i> dan bisa berbeda dari nilai yang
	 * dilihat Hibernate. Dipakai combobox/label ZK dan pesan galat.</p>
	 *
	 * @return gabungan id dan nama kejuaraan; keduanya bisa {@code null} sehingga hasilnya dapat
	 *         berupa {@code "null-null"} pada object yang belum diisi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kejuaraan/lomba (label formulir "Nama Kejuaraan"); wajib diisi. */
	private String nama;

	/** Nama kejuaraan dalam bahasa Inggris; opsional, hanya untuk keperluan tampilan/ekspor. */
	private String namaEn;

	/** Tempat kejuaraan digelar (label formulir "Tempat Kejuaraan"); wajib diisi lewat layar. */
	private String tempat;

	/** Pihak penyelenggara kejuaraan; wajib diisi lewat layar dan bisa dicari di daftar. */
	private String penyelenggara;

	/** Capaian juara dalam bentuk teks bebas (label formulir "Juara ke"); wajib diisi lewat layar. */
	private String juara;

	/** Peringkat numerik; barisnya disembunyikan di formulir sehingga praktis selalu bernilai bawaan. */
	private Integer peringkat;

	/** Tanggal mulai kejuaraan; menjadi kunci penyaring rentang di daftar maupun di rekap dashboard. */
	private Date tanggal;

	/** Tanggal selesai kejuaraan; wajib diisi lewat layar walau kolomnya nullable di basis data. */
	private Date tanggalSelesai;

	/** Nomor sertifikat prestasi (teks); berkas scan-nya disimpan terpisah di {@code LampiranLain}. */
	private String nomorSertifikat;

	/** Status alur persetujuan; nilainya salah satu dari empat konstanta di kelas ini. */
	private String status;

	/** Keterangan bebas dari petugas/pengaju. */
	private String keterangan;

	/** Siswa pemilik prestasi; satu-satunya relasi yang wajib (kolom {@code siswa} non-null). */
	private Siswa siswa;

	/** Penanda kejuaraan digelar di luar sekolah; bawaannya {@code true} bila kolom kosong. */
	private Boolean prestasiLuarKampus;

	/** Baris roster kelas siswa saat prestasi dicatat; diisi otomatis oleh getter-nya bila kosong. */
	private KelasSiswa kelasSiswa;

	/** Yayasan penyelenggara bila kejuaraan digelar internal; BUKAN kolom kepemilikan tenant. */
	private Yayasan yayasan;

	/** Sekolah penyelenggara bila kejuaraan digelar internal; BUKAN kolom kepemilikan tenant. */
	private Sekolah sekolah;

	/** Bidang lomba — FK ke katalog {@link CabangPrestasiSiswa}; wajib diisi lewat layar. */
	private CabangPrestasiSiswa cabangPrestasiSiswa;

	/** Tingkat/cakupan lomba — FK ke katalog {@link KategoriPrestasiSiswa}; wajib diisi lewat layar. */
	private KategoriPrestasiSiswa kategoriPrestasiSiswa;

	/** Jumlah peserta kejuaraan; bertipe teks (bukan angka) sehingga menerima isian seperti "±200". */
	private String jumlahPeserta;

	/** Uraian capaian/prestasi yang diraih; teks panjang, ditampilkan sebagai kolom tersendiri di daftar. */
	private String capaian;

	/** Tautan bukti atau publikasi kejuaraan (label formulir "Link / URL"). */
	private String url;

	/** Tahun kalender kejuaraan; diturunkan otomatis dari {@code tahunAkademik} oleh getter-nya. */
	private Integer tahun;

	/** Tahun akademik pencatatan (format {@code "2025/2026"}); kunci penyaring rapor. */
	private String tahunAkademik;

	/** Semester pencatatan (Ganjil/Genap, konstanta {@link Perkuliahan}); kunci penyaring rapor. */
	private String jenisSemester;

	/** Tanggal SK penetapan prestasi; opsional. */
	private Date tglSk;

	/** Nomor SK penetapan prestasi; opsional. */
	private String noSk;

	/** Lokasi/alamat lengkap penyelenggaraan (pelengkap {@code tempat}); teks panjang. */
	private String alamat;

	/** Guru pembimbing I; ikut dipakai sebagai penyaring "Guru PA" di layar daftar. */
	private Guru guru;

	/** Guru pembimbing II; opsional. */
	private Guru guru2;

	/** Guru pembimbing III; opsional. */
	private Guru guru3;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Membuat baris kosong. Nilai bawaan yang tampak kemudian ({@code status} "Belum diproses",
	 * tahun akademik dan semester berjalan, {@code prestasiLuarKampus} = {@code true}) tidak
	 * diberikan di sini melainkan oleh getter masing-masing saat pertama kali dibaca.</p>
	 */
	public PrestasiSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Nilainya dibangkitkan basis data ({@code IDENTITY}) sehingga selalu {@code null} sebelum
	 * baris tersimpan. Id yang sama dipakai sebagai {@code ref} lampiran bukti prestasi di
	 * {@link ais.database.model.file.LampiranLain}.</p>
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
	 * Menetapkan kunci utama.
	 *
	 * <p>Hanya untuk kebutuhan pemuatan/migrasi; alur normal membiarkan basis data yang mengisi.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kejuaraan, sudah ter-<i>trim</i>.
	 *
	 * <p><b>Efek samping tak langsung:</b> karena pemetaan berbasis property access, nilai
	 * ter-<i>trim</i> inilah yang dibaca Hibernate saat pemeriksaan kotor; spasi tepi pada data lama
	 * akan ikut terhapus di basis data begitu baris tersentuh.</p>
	 *
	 * @return nama kejuaraan tanpa spasi tepi, atau {@code null} bila memang belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama kejuaraan.
	 *
	 * @param nama nama kejuaraan; wajib terisi karena kolomnya {@code not null}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tempat kejuaraan digelar.
	 *
	 * @return nama tempat, atau {@code null} bila tidak diisi
	 */
	public String getTempat() {
		return tempat;
	}

	/**
	 * Menetapkan tempat kejuaraan digelar.
	 *
	 * @param tempat nama tempat
	 */
	public void setTempat(String tempat) {
		this.tempat = tempat;
	}

	/**
	 * Mengembalikan pihak penyelenggara kejuaraan.
	 *
	 * @return nama penyelenggara, atau {@code null} bila tidak diisi
	 */
	public String getPenyelenggara() {
		return penyelenggara;
	}

	/**
	 * Menetapkan pihak penyelenggara kejuaraan.
	 *
	 * @param penyelenggara nama penyelenggara
	 */
	public void setPenyelenggara(String penyelenggara) {
		this.penyelenggara = penyelenggara;
	}

	/**
	 * Mengembalikan capaian juara sebagai teks bebas.
	 *
	 * <p>Isinya mengikuti label formulir "Juara ke", tetapi karena bertipe teks nilainya di lapangan
	 * beragam ("1", "I", "Harapan 2", "Finalis"), sehingga tidak bisa diurutkan atau dijumlahkan
	 * secara numerik.</p>
	 *
	 * @return teks capaian juara, atau {@code null} bila tidak diisi
	 */
	public String getJuara() {
		return juara;
	}

	/**
	 * Menetapkan capaian juara.
	 *
	 * @param juara teks capaian juara
	 */
	public void setJuara(String juara) {
		this.juara = juara;
	}

	/**
	 * Mengembalikan tanggal mulai kejuaraan.
	 *
	 * <p>Kolom ini menjadi kunci penyaring rentang tanggal pada daftar dan pada rekap dashboard
	 * (klausa {@code aaa.tanggal between ...}), jadi baris yang tanggalnya kosong tidak akan pernah
	 * ikut terhitung dalam rekap.</p>
	 *
	 * @return tanggal mulai, atau {@code null} bila tidak diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menetapkan tanggal mulai kejuaraan.
	 *
	 * @param tanggal tanggal mulai; layar mewajibkan pengisiannya
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan nomor sertifikat prestasi.
	 *
	 * <p>Validasi wajib untuk kolom ini pernah ada namun kini dikomentari di
	 * {@code PrestasiSiswaAction.onSave(Event)}, jadi baris tanpa nomor sertifikat tetap bisa
	 * disimpan.</p>
	 *
	 * @return nomor sertifikat, atau {@code null} bila tidak diisi
	 */
	public String getNomorSertifikat() {
		return nomorSertifikat;
	}

	/**
	 * Menetapkan nomor sertifikat prestasi.
	 *
	 * @param nomorSertifikat nomor sertifikat
	 */
	public void setNomorSertifikat(String nomorSertifikat) {
		this.nomorSertifikat = nomorSertifikat;
	}

	/**
	 * Mengembalikan status alur persetujuan, dengan {@link #BELUM_DIPROSES} sebagai pengganti bila
	 * kolomnya kosong.
	 *
	 * <p><b>Penting:</b> berbeda dari getter bernilai bawaan lain di kelas ini, method ini TIDAK
	 * menulis balik ke field. Akibatnya kolom {@code status} bisa tetap {@code null} di basis data
	 * meski layar menampilkan "Belum diproses" — dan baris seperti itu tidak akan pernah cocok
	 * dengan filter <code>status='Disetujui'</code> pada SQL rekap maupun dengan penyaring status
	 * berbasis {@code Restrictions.eq} di layar daftar.</p>
	 * <p>Nilai kembalian dipakai langsung untuk menentukan visibilitas grup tombol aksi dan tombol
	 * kirim ke Feeder pada renderer baris.</p>
	 *
	 * @return status saat ini; tidak pernah {@code null}
	 */
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? BELUM_DIPROSES : status;
	}

	/**
	 * Menetapkan status alur persetujuan.
	 *
	 * <p>Dipanggil dari listener combobox status pada renderer baris, yang langsung menyimpan
	 * perubahannya lewat {@code Common.refreshUpdate(...)}. Nilai yang dipakai layar terbatas pada
	 * empat konstanta kelas ini, tetapi method ini sendiri tidak memvalidasi apa pun.</p>
	 *
	 * @param status status baru; boleh {@code null} (akan dibaca kembali sebagai
	 *               {@link #BELUM_DIPROSES})
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan siswa pemilik prestasi ini.
	 *
	 * <p>Relasi ini WAJIB (kolom {@code siswa} {@code not null}) dan menjadi jangkar seluruh
	 * penyaringan: daftar menyaring lewat {@code siswa} dan {@code siswa.sekolah}, rekap dashboard
	 * menggabungkan lewat <code>inner join sekolah.siswa m on (aaa.siswa = m.id)</code>, dan
	 * pembatasan wali murid memakai {@code Restrictions.in("siswa.id", ...)}.</p>
	 * <p>Pemanggilan {@code check(...)} hanya meresolusi proxy lazy menjadi object nyata (lihat
	 * {@link ais.database.model.GeneralValueObject}); ia tidak mengubah data.</p>
	 *
	 * @return siswa pemilik prestasi, atau {@code null} pada object yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan siswa pemilik prestasi.
	 *
	 * <p>Nilai yang disimpan layar berasal dari bandbox pencari siswa, yang pada akun siswa dikunci
	 * ke dirinya sendiri — kecuali bila halaman dibuka dengan parameter {@code ?siswa=} (lihat
	 * catatan keamanan di Javadoc kelas).</p>
	 *
	 * @param siswa siswa pemilik prestasi
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan guru pembimbing I.
	 *
	 * <p>Kolom ini juga dipakai penyaring "Guru PA" di layar daftar, bersama {@code guru2},
	 * {@code guru3}, dan {@code kelasSiswa.guruPembina} dalam satu rangkaian {@code Restrictions.or}
	 * — sehingga pencarian per guru menemukan prestasi yang dibimbingnya maupun prestasi anak
	 * kelas binaannya.</p>
	 *
	 * @return guru pembimbing I, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan guru pembimbing I.
	 *
	 * @param guru guru pembimbing; boleh {@code null}
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan yayasan penyelenggara bila kejuaraan digelar internal.
	 *
	 * <p><b>Bukan kolom tenant.</b> Baris formulirnya hanya tampil saat centang "kejuaraan di luar
	 * sekolah" dilepas, dan {@code onSave} mewajibkan pengisiannya hanya dalam kondisi itu.
	 * Penyaringan yayasan di layar dilakukan lewat {@code sekolah.yayasan} milik SISWA, bukan lewat
	 * kolom ini.</p>
	 *
	 * @return yayasan penyelenggara, atau {@code null} bila kejuaraan eksternal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan yayasan penyelenggara.
	 *
	 * <p><b>Efek samping:</b> object yang belum tersimpan (id masih {@code null}) DIBUANG dan
	 * disimpan sebagai {@code null}. Ini mencegah cascade {@code PERSIST} membuat baris yayasan baru
	 * secara tak sengaja dari pilihan combobox "Semua".</p>
	 *
	 * @param yayasan yayasan penyelenggara; {@code null} atau object tanpa id akan tersimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan sekolah penyelenggara bila kejuaraan digelar internal.
	 *
	 * <p>Sama seperti {@link #getYayasan()}: ini konteks penyelenggaraan, bukan kepemilikan data.
	 * Label formulirnya bahkan tertulis "Prodi" — sisa penamaan dari modul perguruan tinggi.</p>
	 *
	 * @return sekolah penyelenggara, atau {@code null} bila kejuaraan eksternal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan sekolah penyelenggara.
	 *
	 * <p><b>Efek samping:</b> object tanpa id dibuang menjadi {@code null}, dengan alasan yang sama
	 * seperti {@link #setYayasan(Yayasan)}.</p>
	 *
	 * @param sekolah sekolah penyelenggara; {@code null} atau object tanpa id akan tersimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan penanda "kejuaraan digelar di luar sekolah", dengan {@code true} sebagai nilai
	 * bawaan bila kolomnya kosong.
	 *
	 * <p><b>Efek samping:</b> karena pemetaan property access, nilai bawaan {@code true} itu ikut
	 * TERTULIS ke basis data pada flush berikutnya. Baris hasil impor Excel atau migrasi yang tidak
	 * mengisi kolom ini otomatis menjadi "prestasi luar sekolah" begitu tersentuh — dan konsekuensi
	 * tampilannya nyata: kolom "Keberadaan" di daftar berubah, serta blok yayasan/sekolah tidak lagi
	 * ditampilkan pada baris tersebut.</p>
	 *
	 * @return {@code true} bila kejuaraan digelar di luar sekolah; tidak pernah {@code null}
	 */
	public Boolean getPrestasiLuarKampus() {
		return prestasiLuarKampus == null ? true : prestasiLuarKampus;
	}

	/**
	 * Menetapkan penanda kejuaraan di luar sekolah.
	 *
	 * <p>Nilainya berasal dari centang "Apakah kejuaraan diluar sekolah?" dan mengendalikan
	 * tampil/tidaknya baris Yayasan dan Sekolah pada formulir.</p>
	 *
	 * @param prestasiLuarKampus {@code true} bila kejuaraan digelar di luar sekolah
	 */
	public void setPrestasiLuarKampus(Boolean prestasiLuarKampus) {
		this.prestasiLuarKampus = prestasiLuarKampus;
	}

	/**
	 * Mengembalikan kategori (tingkat/cakupan) kejuaraan.
	 *
	 * <p>Merujuk katalog global {@link KategoriPrestasiSiswa} — Internasional, Nasional, Regional,
	 * Kab/Kota, Kecamatan, Kampus/Sekolah, Lain-Lain. Berbeda dari relasi lain di kelas ini, relasi
	 * ini dipetakan {@code FetchMode.SELECT} tanpa {@code FetchType.LAZY}, sehingga katalognya
	 * dimuat lewat query terpisah saat entity dibaca.</p>
	 * <p>Nama baris katalog inilah yang disisipkan mentah ke alias kolom SQL native oleh
	 * {@code DashboardRekapPrestasiSiswaBerdasarKategori} — lihat catatan keamanan di Javadoc
	 * kelas.</p>
	 *
	 * @return kategori prestasi, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kategori_prestasi_siswa", nullable = true)
	public KategoriPrestasiSiswa getKategoriPrestasiSiswa() {
		return kategoriPrestasiSiswa;
	}

	/**
	 * Menetapkan kategori (tingkat/cakupan) kejuaraan.
	 *
	 * <p>Layar mewajibkan pengisiannya, tetapi kolomnya tetap nullable sehingga baris hasil impor
	 * massal bisa kosong — dan baris kosong tidak akan muncul di rekap per kategori (query rekap
	 * menyaring {@code Restrictions.isNotNull}).</p>
	 *
	 * @param kategoriPrestasiSiswa kategori dari katalog; boleh {@code null}
	 */
	public void setKategoriPrestasiSiswa(KategoriPrestasiSiswa kategoriPrestasiSiswa) {
		this.kategoriPrestasiSiswa = kategoriPrestasiSiswa;
	}

	/**
	 * Mengembalikan jumlah peserta kejuaraan, dengan string kosong sebagai pengganti {@code null}.
	 *
	 * <p><b>Efek samping:</b> string kosong itu ikut tertulis ke basis data pada flush berikutnya
	 * (normalisasi senyap {@code null} &rarr; {@code ""}). Perbedaannya tidak terlihat di layar,
	 * tetapi menyulitkan kueri yang membedakan "belum diisi" dari "diisi kosong".</p>
	 *
	 * @return jumlah peserta sebagai teks; tidak pernah {@code null}
	 */
	public String getJumlahPeserta() {
		return jumlahPeserta == null ? "" : jumlahPeserta;
	}

	/**
	 * Menetapkan jumlah peserta kejuaraan.
	 *
	 * @param jumlahPeserta jumlah peserta sebagai teks bebas
	 */
	public void setJumlahPeserta(String jumlahPeserta) {
		this.jumlahPeserta = jumlahPeserta;
	}

	/**
	 * Mengembalikan uraian capaian, dengan string kosong sebagai pengganti {@code null}.
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getJumlahPeserta()} — {@code null} dinormalkan
	 * menjadi {@code ""} dan ikut tersimpan.</p>
	 *
	 * @return uraian capaian; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getCapaian() {
		return capaian == null ? "" : capaian;
	}

	/**
	 * Menetapkan uraian capaian prestasi.
	 *
	 * @param capaian uraian capaian
	 */
	public void setCapaian(String capaian) {
		this.capaian = capaian;
	}

	/**
	 * Mengembalikan cabang (bidang) kejuaraan.
	 *
	 * <p>Merujuk katalog global {@link CabangPrestasiSiswa} — Seni, Olah Raga, Kejuaraan Ilmiah,
	 * Lain-Lain. Sama seperti {@link #getKategoriPrestasiSiswa()}, dipetakan
	 * {@code FetchMode.SELECT}, dan nama barisnya menjadi bahan alias kolom pada SQL native
	 * {@code DashboardRekapPrestasiSiswaBerdasarCabang}.</p>
	 *
	 * @return cabang prestasi, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "cabang_prestasi_siswa", nullable = true)
	public CabangPrestasiSiswa getCabangPrestasiSiswa() {
		return cabangPrestasiSiswa;
	}

	/**
	 * Menetapkan cabang (bidang) kejuaraan.
	 *
	 * @param cabangPrestasiSiswa cabang dari katalog; boleh {@code null}
	 */
	public void setCabangPrestasiSiswa(CabangPrestasiSiswa cabangPrestasiSiswa) {
		this.cabangPrestasiSiswa = cabangPrestasiSiswa;
	}

	/**
	 * Mengembalikan tautan bukti/publikasi kejuaraan, dengan string kosong sebagai pengganti
	 * {@code null}.
	 *
	 * <p><b>Efek samping:</b> normalisasi {@code null} &rarr; {@code ""} ikut tersimpan. Nilainya
	 * dirender apa adanya sebagai teks label di daftar (tidak dijadikan tautan aktif), jadi isian
	 * bebas apa pun aman dari sisi tampilan.</p>
	 *
	 * @return tautan sebagai teks; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getUrl() {
		return url == null ? "" : url;
	}

	/**
	 * Menetapkan tautan bukti/publikasi kejuaraan.
	 *
	 * @param url tautan; tidak divalidasi formatnya
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Mengembalikan tanggal selesai kejuaraan.
	 *
	 * <p>Ditampilkan berpasangan dengan {@link #getTanggal()} sebagai rentang "tanggal s.d tanggal"
	 * di daftar. Penyaringan rentang di daftar dan di rekap hanya memakai {@code tanggal} (mulai),
	 * kolom ini tidak ikut disaring.</p>
	 *
	 * @return tanggal selesai, atau {@code null} bila tidak diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Menetapkan tanggal selesai kejuaraan.
	 *
	 * @param tanggalSelesai tanggal selesai; layar mewajibkan pengisiannya
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Mengembalikan tahun kejuaraan, DITURUNKAN dari {@code tahunAkademik} setiap kali dibaca.
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Bila {@code tahunAkademik} terisi, method ini mengambil bagian
	 * sebelum garis miring (mis. {@code "2025/2026"} &rarr; {@code 2025}) dan MENIMPA field
	 * {@code tahun}. Nilai apa pun yang pernah diberikan lewat {@link #setTahun(Integer)} —
	 * termasuk hasil impor Excel — akan hilang, dan karena pemetaan property access nilai turunan
	 * itu ikut tertulis ke basis data. Layar pun memperlakukan kolom ini sebagai turunan:
	 * {@code Intbox} "Tahun" dibuat {@code readonly} dan diisi ulang oleh listener perubahan
	 * combobox tahun akademik.</p>
	 * <p>Kegagalan parsing (format tahun akademik tidak standar) ditelan diam-diam dan hanya
	 * dicatat ke audit galat; nilai {@code tahun} lama dipertahankan dalam kasus itu.</p>
	 *
	 * @return tahun kejuaraan, atau {@code null} bila tahun akademik kosong dan kolomnya belum
	 *         pernah diisi
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			try {
				tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PrestasiSiswa.java:314");

			}
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun kejuaraan.
	 *
	 * <p>Praktis tidak berguna sebagai penyimpan nilai mandiri: {@link #getTahun()} akan
	 * menimpanya kembali dari {@code tahunAkademik} pada pembacaan berikutnya. Nilai yang diset di
	 * sini hanya bertahan bila {@code tahunAkademik} benar-benar {@code null} — kondisi yang sendiri
	 * sulit dipertahankan karena {@link #getTahunAkademik()} mengisinya otomatis.</p>
	 *
	 * @param tahun tahun kejuaraan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun akademik pencatatan, mengisinya dengan tahun akademik BERJALAN bila
	 * kosong.
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Nilai bawaan diambil dari {@code Common.getCurrentTahunAkademik()}
	 * — konfigurasi instalasi saat ini — dan ditulis ke field, sehingga ikut tersimpan pada flush
	 * berikutnya. Dampaknya bukan kosmetik: rapor menyaring prestasi berdasarkan pasangan
	 * {@code tahunAkademik} + {@code jenisSemester}, jadi baris lama yang periodenya kosong akan
	 * "berpindah" ke periode berjalan begitu tersentuh dan bisa muncul di rapor semester ini padahal
	 * kejuaraannya bertahun-tahun lalu. Riwayat aslinya tidak dapat dipulihkan dari kolom ini —
	 * hanya {@code tanggal} yang tetap benar.</p>
	 *
	 * @return tahun akademik dalam format {@code "2025/2026"}; tidak pernah {@code null} setelah
	 *         pembacaan pertama
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik pencatatan.
	 *
	 * <p>Diisi layar dari combobox "Tahun Akademik". Menyetel {@code null} tidak berarti mengosongkan
	 * secara permanen — pembacaan berikutnya lewat {@link #getTahunAkademik()} akan mengisinya lagi
	 * dengan periode berjalan.</p>
	 *
	 * @param tahunAkademik tahun akademik dalam format {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan semester pencatatan, mengisinya dari JAM SERVER bila kosong.
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Bila kosong, nilainya ditentukan {@code Common.isNowSemensterGanjil()}
	 * dan diisi dengan konstanta {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}, lalu ikut
	 * tersimpan. Berpasangan dengan {@link #getTahunAkademik()}, pasangan periode inilah yang dipakai
	 * penyaring rapor — dengan konsekuensi "pindah periode" yang sama.</p>
	 *
	 * @return {@code "Ganjil"} atau {@code "Genap"}; tidak pernah {@code null} setelah pembacaan
	 *         pertama
	 */
	public String getJenisSemester() {
		if (jenisSemester == null) {
			jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return jenisSemester;
	}

	/**
	 * Menetapkan semester pencatatan.
	 *
	 * @param jenisSemester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan peringkat numerik, dengan {@code 0} sebagai pengganti {@code null}.
	 *
	 * <p><b>Efek samping:</b> nilai {@code 0} itu ikut tertulis ke basis data pada flush berikutnya.
	 * Karena baris formulir "Peringkat" sengaja disembunyikan
	 * ({@code row.setVisible(false)} di {@code PrestasiSiswaAction.init(...)}), kolom ini praktis
	 * selalu bernilai {@code 0} pada data yang dibuat lewat layar; capaian sesungguhnya dicatat di
	 * {@link #getJuara()} sebagai teks.</p>
	 *
	 * @return peringkat; tidak pernah {@code null}
	 */
	public Integer getPeringkat() {
		return peringkat == null ? 0 : peringkat;
	}

	/**
	 * Menetapkan peringkat numerik.
	 *
	 * @param peringkat peringkat; boleh {@code null} (akan dibaca kembali sebagai {@code 0})
	 */
	public void setPeringkat(Integer peringkat) {
		this.peringkat = peringkat;
	}

	/**
	 * Mengembalikan nama kejuaraan dalam bahasa Inggris.
	 *
	 * <p>Dipetakan ke kolom <code>namaen</code> (tanpa kapital, tanpa pemisah). Ditampilkan sebagai
	 * label pelengkap di bawah nama kejuaraan pada daftar dan tidak pernah dipakai untuk
	 * penyaringan.</p>
	 *
	 * @return nama dalam bahasa Inggris, atau {@code null} bila tidak diisi
	 */
	@Column(name = "namaen", columnDefinition = "text")
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menetapkan nama kejuaraan dalam bahasa Inggris.
	 *
	 * @param namaEn nama dalam bahasa Inggris
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan lokasi/alamat lengkap penyelenggaraan.
	 *
	 * <p>Pelengkap {@link #getTempat()} untuk alamat panjang; tidak ditampilkan di daftar, hanya di
	 * formulir dan ekspor.</p>
	 *
	 * @return alamat, atau {@code null} bila tidak diisi
	 */
	@Column(columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menetapkan lokasi/alamat lengkap penyelenggaraan.
	 *
	 * @param alamat alamat penyelenggaraan
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan nomor SK penetapan prestasi.
	 *
	 * <p>Opsional dan tidak divalidasi; berpasangan dengan {@link #getTglSk()}. Tidak ada generator
	 * nomor SK untuk entity ini — nilainya sepenuhnya ketikan pengguna.</p>
	 *
	 * @return nomor SK, atau {@code null} bila tidak diisi
	 */
	public String getNoSk() {
		return noSk;
	}

	/**
	 * Menetapkan nomor SK penetapan prestasi.
	 *
	 * @param noSk nomor SK
	 */
	public void setNoSk(String noSk) {
		this.noSk = noSk;
	}

	/**
	 * Mengembalikan tanggal SK penetapan prestasi.
	 *
	 * @return tanggal SK, atau {@code null} bila tidak diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	/**
	 * Menetapkan tanggal SK penetapan prestasi.
	 *
	 * @param tglSk tanggal SK
	 */
	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}

	/**
	 * Mengembalikan baris roster kelas yang melekat pada prestasi ini, MENGISINYA dari kelas siswa
	 * saat ini bila kosong.
	 *
	 * <p><b>GETTER DESTRUKTIF — dan yang paling berdampak di kelas ini.</b> Bila kolomnya kosong dan
	 * {@link #getSiswa()} tersedia, method mengambil {@code getSiswa().getKelas()} — kelas siswa
	 * <i>pada saat pembacaan</i>, bukan kelas saat prestasi diraih — dan menuliskannya ke field.
	 * Karena pemetaan property access, nilai itu ikut tersimpan. Akibatnya prestasi lama akan
	 * ter-stempel ulang dengan kelas terbaru setiap kali siswa naik kelas dan barisnya tersentuh
	 * dalam sesi yang dapat menulis.</p>
	 * <p><b>Konsekuensi kedua yang lebih tersembunyi:</b>
	 * {@code PrestasiSiswaAction.initCriteria(boolean)} memasang
	 * {@code createAlias("kelasSiswa", "kelasSiswa")} — INNER JOIN — padahal kolom
	 * <code>kelas_siswa</code> nullable. Baris yang kolomnya tetap kosong (siswa belum punya roster
	 * kelas, atau baris dibuat lewat impor massal tanpa pernah dibaca getter ini) TIDAK PERNAH muncul
	 * di layar daftar maupun ikut terhitung paging, tanpa pesan galat apa pun. Relasi ini juga
	 * dipakai penyaring "Guru PA" lewat {@code kelasSiswa.guruPembina}.</p>
	 *
	 * @return baris roster kelas, atau {@code null} bila siswa memang belum punya kelas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		kelasSiswa = check(kelasSiswa);
		if (kelasSiswa == null && getSiswa() != null) {
			kelasSiswa = getSiswa().getKelas();
		}
		return kelasSiswa;
	}

	/**
	 * Menetapkan baris roster kelas untuk prestasi ini.
	 *
	 * <p>Tidak ada kontrol di layar untuk kolom ini — satu-satunya pengisi dalam praktik adalah
	 * pengisian otomatis di {@link #getKelasSiswa()}. Setter ini disediakan untuk migrasi data yang
	 * ingin mempertahankan kelas historis yang benar.</p>
	 *
	 * @param kelasSiswa baris roster kelas
	 */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}

	/**
	 * Mengembalikan guru pembimbing II.
	 *
	 * <p>Ikut diperiksa penyaring "Guru PA" bersama {@link #getGuru()} dan {@link #getGuru3()}.</p>
	 *
	 * @return guru pembimbing II, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru2")
	public Guru getGuru2() {
		guru2 = check(guru2);
		return guru2;
	}

	/**
	 * Menetapkan guru pembimbing II.
	 *
	 * @param guru2 guru pembimbing II; boleh {@code null}
	 */
	public void setGuru2(Guru guru2) {
		this.guru2 = guru2;
	}

	/**
	 * Mengembalikan guru pembimbing III.
	 *
	 * <p>Ikut diperiksa penyaring "Guru PA" bersama {@link #getGuru()} dan {@link #getGuru2()}.</p>
	 *
	 * @return guru pembimbing III, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru3")
	public Guru getGuru3() {
		guru3 = check(guru3);
		return guru3;
	}

	/**
	 * Menetapkan guru pembimbing III.
	 *
	 * @param guru3 guru pembimbing III; boleh {@code null}
	 */
	public void setGuru3(Guru guru3) {
		this.guru3 = guru3;
	}
}
