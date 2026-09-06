package ais.action.master.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.SertifikatAction;
import ais.action.master.UjianAction;
import ais.action.master.dashboard.admin.RekapHasilUjian;
import ais.action.master.dashboard.admin.RekapHasilUjianPerVoPertemuan;
import ais.action.master.helper.generic.AmbilDataUjianBanyak;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.RuangPaketPMB;
import ais.database.model.Tbmuser;
import ais.database.model.Ujian;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHashMap;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelAgakKecilBoldHijau;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;

/**
 * Helper ZK yang menampilkan dan mengelola seluruh {@link PertemuanPunyaUjian} (ujian/kuis yang
 * dijadwalkan) milik satu {@link Pertemuan} akademik (sesi perkuliahan/KKN/PKL — <b>bukan</b>
 * jadwal sekolah, yang punya helper sekolah tersendiri: {@code PertemuanPunyaUjianSiswaHelper}).
 * Dipanggil dari tab "Ujian" milik {@link PertemuanHelper} lewat {@link #display(Pertemuan,
 * Component)}, dan didaftarkan sebagai {@link DataLoader} sehingga sub-window (mis. modal
 * "Pengaturan Data Ujian") bisa memicu {@link #loadData(Object)} untuk memuat ulang daftar dari
 * DB setelah selesai.
 *
 * <p><b>Dua identitas pemakai (pola sama dengan {@link PertemuanHelper}).</b> Konstruktor
 * {@link #PertemuanPunyaUjianHelper(Mahasiswa, BiodataCalonMahasiswa)} menyimpan siapa yang
 * melihat: bila keduanya {@code null} dan {@code Common.getCurrentUser()} juga bukan
 * peserta kursus/siswa/calon siswa, tampilan dianggap milik <b>pengelola/dosen</b> (kartu
 * ringkas + tombol Pengaturan/Ubah/Hapus/Hasil/Preview/Sertifikat/Rekap); selain itu tampilan
 * dianggap milik <b>peserta</b> (kartu "Ikut Ujian" tanpa kontrol pengaturan apa pun). Banyak
 * blok kode memeriksa kombinasi {@code mahasiswa}/{@code biodataCalonMahasiswa}/
 * {@code tbmuser.getPesertaKursus()}/{@code getSiswa()}/{@code getCalonSiswa()} berulang-ulang
 * untuk membedakan kedua peran ini di titik yang berbeda-beda.
 *
 * <p><b>Tampilan utama: kartu responsif, BUKAN grid tabel.</b> {@link #display(Pertemuan,
 * Component)} membangun {@code kartuWrap} (grid CSS 1 kolom di HP, 2 kolom di layar lebar,
 * gaya {@link #GAYA_KARTU_UJIAN}) lalu memanggil {@link #loadData(Object)}, yang mengisi kartu
 * lewat {@link #buatKartuUjianRingkas(PertemuanPunyaUjian, Tbmuser, EventListener)} (pengelola)
 * atau {@link #buatKartuUjianPeserta(PertemuanPunyaUjian, Tbmuser, EventListener)} (peserta).
 * Detail pengaturan tiap ujian (jumlah soal, batas ikut, dibatasi waktu, jadwal, Sub-CPMK/OBE,
 * dsb.) TIDAK ada di kartu — semuanya dibuka lewat modal
 * {@link #bukaPengaturanUjian(PertemuanPunyaUjian, EventListener)}, yang me-reuse
 * {@link DetailPertemuanRenderer} (renderer grid lama) pada sebuah {@code MyGrid} 1-baris LOKAL
 * bernama {@code gridModal} agar seluruh kontrol edit inline lama tetap berfungsi tanpa
 * ditulis ulang.
 *
 * <p><b>Kuirk: field instance {@code grid} sudah tidak pernah diisi grid sungguhan.</b>
 * Satu-satunya penulisan ke field ini adalah {@code grid = null;} di akhir
 * {@link #display(Pertemuan, Component)}. Akibatnya cabang lama di {@link #loadData(Object)}
 * yang memakai {@code MyGrid} tabel penuh ({@code if (grid == null) return;} lalu
 * {@code grid.setRowRenderer(...)}) sekarang TIDAK PERNAH tereksekusi lewat alur normal —
 * satu-satunya {@code MyGrid} yang benar-benar dipakai adalah variabel lokal {@code gridModal}
 * di {@link #bukaPengaturanUjian}. Kode grid lama dibiarkan apa adanya (bukan dihapus) karena
 * sejalan dengan gaya migrasi UI di file ini: mengganti kemasan visual tanpa mengubah logika.
 *
 * <p><b>Kelompok operasi:</b></p>
 * <ul>
 * <li><b>Kelayakan ikut ujian</b> — {@link #tampilBolekIkutUjianAtauTidak} menentukan status
 * peserta (belum waktunya, sudah lewat, kuota habis, sisa percobaan, dibatasi jurusan/fakultas
 * untuk ujian PMB) dan merender tombol "Ikut Ujian"/"Lihat Hasil"/"Ubah Jawaban" yang
 * mendelegasikan aksi ke {@code ProsesUjianHelper}; dipanggil baik dari kartu peserta maupun
 * dari {@link DetailPertemuanRenderer}.</li>
 * <li><b>Renderer detail/edit</b> — {@link DetailPertemuanRenderer#render} membangun SEMUA
 * kontrol pengaturan satu {@link PertemuanPunyaUjian} (checkbox, datebox, timebox, kombo
 * pindah pertemuan, bulk assignment Sub-CPMK, tombol Kelola Soal/Cetak/Sinkronkan Nilai/
 * Hasil/Ubah/Hapus/Preview/Sertifikat); setiap perubahan field langsung
 * {@code Common.refreshUpdate(session, ppu)} tanpa tombol simpan terpisah (autosave per
 * field), kecuali footer modal "Simpan/Batal" yang sebetulnya hanya memuat ulang &amp; menutup
 * (karena datanya sudah tersimpan saat itu juga).</li>
 * <li><b>Pelaporan OBE</b> — {@link #cetak(PertemuanPunyaUjian)} dan {@link #parameter}
 * menyusun parameter template "TemplateObe" (capaian Sub-CPMK, bobot per ujian, tanda tangan
 * petugas/kaprodi/pudek) untuk dicetak lewat {@code Report.generatePDFReport}.</li>
 * <li><b>Sinkronisasi soal massal</b> — {@link #prosesUlangSoal} membuka dialog filter
 * (fakultas/prodi/dosen/rentang tanggal), lalu di background thread meng-generate file Excel
 * berisi soal &amp; jawaban tiap peserta ujian yang cocok filter — SEKALIGUS, sebagai efek
 * samping, membuat baris {@link HasilUjianMahasiswaDetail} baru (assignment soal ke peserta)
 * bila belum ada, agar file Excel dan data penilaian konsisten.</li>
 * </ul>
 *
 * <p><b>Peran file ini dalam siklus hidup ujian.</b> Tiga lapis data ujian dipegang oleh tiga
 * helper yang berbeda dan file ini adalah lapis TENGAH — <b>penjadwalan &amp; penyiapan</b>, yaitu
 * segala sesuatu yang terjadi SEBELUM peserta mengerjakan soal:
 * <ol>
 * <li><b>Bahan ujian</b> ({@link Ujian}, {@link UjianPunyaSoal}, {@link BankSoal}) — dikelola
 * oleh {@code UjianAction} dan {@code DetailUjianHelper}; file ini hanya <i>membukanya</i> lewat
 * tombol "Kelola Soal Ujian" dan "Buat Ujian"/"Ambil Bahan Ujian".</li>
 * <li><b>Penjadwalan &amp; penyiapan (FILE INI)</b> — menautkan satu {@link Ujian} ke satu
 * {@link Pertemuan} sebagai {@link PertemuanPunyaUjian}, lalu mengatur SEMUA parameter
 * pelaksanaannya: jumlah soal yang ditampilkan, jumlah percobaan, jendela waktu mulai/selesai,
 * durasi, pengacakan urutan, saklar anti-curang, siapa yang tidak perlu ikut, komponen penilaian
 * tujuan (atau pemetaan nomor soal ke Sub-CPMK bila kurikulum OBE), serta memindahkan ujian ke
 * pertemuan lain. Di sini pula assignment soal↔peserta dipastikan lengkap lewat
 * {@link #prosesUlangSoal}.</li>
 * <li><b>Pengerjaan &amp; hasil</b> ({@link HasilUjianMahasiswa},
 * {@link HasilUjianMahasiswaDetail}) — dikerjakan {@code ProsesUjianHelper} (saat ujian
 * berlangsung), {@code HasilUjianMahasiswaHelper} (rekap hasil setelah ujian), dan
 * {@code KoreksiHasilUjian}/{@code DetailperkuliahanForPenilaianHelper} (koreksi &amp;
 * penilaian). File ini hanya MEMBACA lapis ini untuk menampilkan status peserta, dan
 * mendelegasikan setiap aksi pengerjaan ke {@code ProsesUjianHelper}.</li>
 * </ol>
 * Satu-satunya tempat file ini MENULIS ke lapis ketiga adalah {@link #prosesUlangSoal} (membuat
 * {@link HasilUjianMahasiswaDetail} yang belum ada) dan tombol "Hapus" pada
 * {@link DetailPertemuanRenderer#render(Row, Object)} (menghapus seluruh hasil ujian milik
 * {@link PertemuanPunyaUjian} yang dihapus, lewat SQL mentah).
 *
 * <p><b>FAKTA ARSITEKTUR (bukan bug yang perlu "diperbaiki" saat menyunting file ini) —
 * render menulis ke database.</b> Tampilan di file ini bukan murni baca. Beberapa titik
 * melakukan mutasi saat merender atau saat kontrol disentuh, tanpa tombol simpan terpisah:
 * <ul>
 * <li><b>Autosave per field.</b> Hampir setiap kontrol pengaturan di
 * {@link DetailPertemuanRenderer#render(Row, Object)} memanggil
 * {@code Common.refreshUpdate(session, ppu)} langsung pada {@code onChange}/{@code onCheck}.
 * Footer "Simpan/Batal" pada modal {@link #bukaPengaturanUjian} karena itu hanya memuat ulang
 * dan menutup — datanya sudah tersimpan sejak field diubah.</li>
 * <li><b>Penulisan saat render.</b> Bila {@code ppu.getJmlDitampilkan()} kosong atau {@code <= 0},
 * {@code render} menghitung jumlah soal riil lalu MENYIMPANNYA ke DB — sekadar membuka daftar
 * ujian bisa mengubah data.</li>
 * <li><b>Mutasi entity tanpa simpan.</b> Bila konfigurasi
 * {@code tampilkan_ujian_dibatasi_waktu} nonaktif, {@code render} memanggil
 * {@code ppu.setDibatasiWaktu(true)} tanpa {@code refreshUpdate}; entity menjadi kotor (dirty) di
 * sesi Hibernate dan bisa ikut ter-flush oleh operasi lain pada request yang sama. Ini varian
 * lokal dari pola mutasi-field-di-jalur-baca yang tercatat sistemik di modul
 * {@code ais/database/model/}.</li>
 * </ul>
 *
 * <p><b>FAKTA ARSITEKTUR — gerbang peran ditulis ulang manual, bukan lewat satu helper.</b>
 * Predikat "pengguna saat ini pengelola/dosen, bukan peserta" tidak punya method tunggal; ia
 * dieja ulang dengan tangan di ~20 titik dan TIDAK seragam. Ejaan terlengkap ada di kontrol
 * "Pindahkan ke pertemuan" ({@code mahasiswa}, {@code biodataCalonMahasiswa},
 * {@code tbmuser.getMahasiswa()}, {@code getBiodataCalonMahasiswa()}, {@code getPesertaKursus()},
 * {@code getSiswa()}, {@code getCalonSiswa()} semuanya {@code null}); ejaan lain menghilangkan
 * salah satu peran dan justru mengulang {@code getSiswa() == null} dua sampai tiga kali. Selain
 * itu semua gerbang ini bersifat UI-only ({@code setVisible(...)}) — listener {@code onClick}
 * tidak memeriksa ulang peran. Jangan menyalin salah satu ejaan ke kode baru; lihat tugas
 * penyeragaman {@code task_d45feed7}. Perlu dicatat pula bahwa file ini TIDAK memeriksa
 * kepemilikan mata kuliah sama sekali: tidak ada padanan {@code getMelihatDataSatkerLain()}
 * atau pemeriksaan bahwa dosen yang login benar-benar mengampu
 * {@code pertemuan.getPerkuliahan()}; cakupan data sepenuhnya diserahkan kepada
 * {@code pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser)} dan kepada gerbang hak akses menu
 * milik halaman pemanggil ({@code CommonPrivilages}).
 *
 * <p><b>Entity Hibernate utama:</b> {@link PertemuanPunyaUjian} (jadwal satu {@link Ujian} pada
 * satu {@link Pertemuan}), {@link Ujian}/{@link UjianPunyaSoal}/{@link BankSoal} (bank soal),
 * {@link HasilUjianMahasiswa}/{@link HasilUjianMahasiswaDetail} (progres &amp; jawaban per
 * peserta), {@link FormatNilai} (komponen penilaian biasa maupun Sub-CPMK bila kurikulum OBE),
 * {@link Mahasiswa}/{@link BiodataCalonMahasiswa}/{@link Tbmuser} (identitas peserta/pengguna).
 */
public class PertemuanPunyaUjianHelper implements DataLoader {

	/**
	 * Grid tabel lama untuk daftar ujian. Lihat kuirk pada Javadoc kelas: field ini hanya pernah
	 * di-{@code null}-kan (di {@link #display(Pertemuan, Component)}), tidak pernah diisi grid
	 * sungguhan lagi, sehingga cabang grid di {@link #loadData(Object)} kini efektif tak terpakai.
	 */
	private MyGrid grid;
	/** Wadah kartu ujian responsif yang diisi ulang oleh {@link #loadData(Object)}; ini yang sungguhan dipakai tampilan saat ini. */
	private Div kartuWrap;
	/** Pertemuan yang sedang ditampilkan, diisi oleh {@link #display(Pertemuan, Component)}. */
	private Pertemuan pertemuan;

	/** Identitas peserta bila yang login adalah mahasiswa; {@code null} untuk pengelola/dosen atau jenis peserta lain. */
	private Mahasiswa mahasiswa = null;

	/** Identitas peserta bila yang login adalah calon mahasiswa (ujian PMB); {@code null} untuk pengelola/dosen atau jenis peserta lain. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa = null;

	/**
	 * Membuat helper untuk konteks peserta ujian tertentu. Isi kedua parameter dengan {@code null}
	 * untuk konteks pengelola/dosen (izin edit penuh); isi salah satunya untuk konteks peserta
	 * (mahasiswa reguler atau calon mahasiswa ujian PMB), yang membatasi tampilan hanya pada kartu
	 * "Ikut Ujian" tanpa kontrol pengaturan. Lihat juga penentuan peran berbasis
	 * {@code Tbmuser} (siswa/calon siswa/peserta kursus) yang dilakukan on-the-fly di banyak
	 * method lain lewat {@code Common.getCurrentUser()}.
	 *
	 * @param mahasiswa               peserta mahasiswa aktif, atau {@code null}.
	 * @param biodataCalonMahasiswa   peserta calon mahasiswa (ujian PMB), atau {@code null}.
	 */
	public PertemuanPunyaUjianHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Membangun tombol toolbar "Singkronkan Soal Peserta" yang dipasang di {@link #display}.
	 * Saat diklik, membuka dialog filter (fakultas, prodi, dosen pengampu, rentang tanggal
	 * "Mulai"/"Sampai", checkbox "Hanya ujian di pertemuan ini") untuk memilih cakupan
	 * {@link PertemuanPunyaUjian} yang akan diproses — otomatis mendeteksi konteks ujian PMB
	 * ({@code jadwalUjianPMB}) atau ujian PSB ({@code jadwalUjianPSB}) pada {@code pertemuan}
	 * dan membatasi filter sesuai jadwal tersebut bila ada.
	 *
	 * <p>Setelah "Proses" diklik, pekerjaan berat berjalan di {@link Thread} terpisah (dengan
	 * {@link Timer} pemoll status tiap 200ms yang menampilkan progres lewat {@code Clients.showBusy}):
	 * untuk setiap {@link PertemuanPunyaUjian} yang cocok filter dan setiap peserta (mahasiswa,
	 * atau {@link BiodataCalonMahasiswa} hasil query ruang/gelombang PMB), method ini menentukan
	 * urutan soal yang seharusnya tampil untuk peserta tersebut (memakai
	 * {@code ProsesUjianHelper.randomPosisiton}/{@code chekPosisitonJikaKurang} bila belum ada),
	 * lalu — <b>efek samping penting</b> — MEMBUAT baris {@link HasilUjianMahasiswaDetail} baru
	 * (assignment soal↔peserta, dengan {@code nilai} awal = skor default bank soal) untuk setiap
	 * soal yang belum punya assignment, disimpan lewat transaksi {@link Session} terpisah per
	 * baris. Hasil akhirnya ditulis ke satu berkas {@code .xlsx} (kolom NIM/nama, fakultas/prodi,
	 * status awal, angkatan, ujian, teks soal (di-strip HTML lewat Jsoup), jawaban huruf/teks,
	 * dan status "BETUL") yang ditawarkan untuk diunduh lewat {@link Filedownload}.
	 *
	 * <p>Karena itu, nama method ini ("proses ULANG soal") lebih tepat dibaca sebagai
	 * "pastikan assignment soal-ke-peserta lengkap, lalu ekspor rekapnya ke Excel" — bukan
	 * sekadar laporan baca-saja.
	 *
	 * <p><b>Cakupan filter dan isi berkas yang dihasilkan.</b> Perlu disadari saat menyunting
	 * bagian ini: ketiga filter opsional (fakultas, prodi, dosen) boleh dibiarkan kosong, dan
	 * checkbox "Hanya ujian di pertemuan ini" boleh dilepas. Bila semuanya dikosongkan pada
	 * pertemuan yang BUKAN ujian PMB/PSB, satu-satunya penyaring yang tersisa adalah rentang
	 * tanggal {@code mulai_ujian} — sehingga cakupannya menjadi seluruh {@link PertemuanPunyaUjian}
	 * di rentang tersebut, lintas fakultas dan lintas program studi. Tidak ada penyempitan
	 * berbasis satuan kerja maupun berbasis mata kuliah yang diampu pengguna. Berkas keluarannya
	 * pun bukan sekadar daftar peserta: kolom SOAL berisi teks soal (di-strip HTML lewat Jsoup),
	 * kolom JAWABAN HURUF dan JAWABAN TEKS berisi opsi jawaban dari {@link BankSoalDetail}, dan
	 * kolom BETUL berisi penanda kunci jawaban. Penjagaan aksesnya sepenuhnya bergantung pada
	 * visibilitas tombol pemanggil di {@link #display(Pertemuan, Component)} — yang bersifat
	 * UI-only dan tidak diperiksa ulang di dalam listener ini.
	 *
	 * <p><b>Dua {@code sqlRestriction} mentah pada pemilihan data.</b> Cabang PMB/PSB menyusun
	 * penyaring tanggal dengan merangkai string
	 * ({@code "date(this_.mulai_ujian) between date('...')"}) dari nilai {@link MyDatebox} yang
	 * sudah diformat {@code Common.databaseDateFormat}, sehingga isinya selalu berupa tanggal, bukan
	 * teks bebas pengguna. Yang lebih perlu diperhatikan adalah
	 * {@code Restrictions.sqlRestriction("ruang_pmb in (-1" + ...getRuanganYgIkut() + "-1)")}: nilai
	 * itu berasal dari kolom {@code ruanganYgIkut} milik {@code JadwalUjianPMB} dan dirangkai
	 * langsung ke SQL. Pembungkus {@code -1} di kedua ujung adalah trik agar daftar CSV
	 * ber-pagar-koma ({@code ",3,7,12,"}) tetap menghasilkan {@code in (...)} yang sintaksnya sah
	 * walau daftarnya kosong. Sifat getter {@code getRuanganYgIkut()} sendiri — termasuk bahwa ia
	 * MENULIS ULANG field-nya sendiri saat dibaca dan mengosongkan daftar ruang pada kondisi
	 * tertentu — sudah diuraikan panjang lebar pada Javadoc
	 * {@code ais.database.model.JadwalUjianPMB#getRuanganYgIkut()}, yang bahkan menyebut baris di
	 * file ini sebagai contoh. Baca Javadoc di sana lebih dulu sebelum menyunting cabang PMB ini.
	 *
	 * <p><b>Pola sesi dan transaksi.</b> Query pemilihan data memakai
	 * {@code HibernateUtil.currentNativeSession()} pada thread background, dan ditutup di blok
	 * {@code finally} lewat {@code HibernateUtil.closeSession()} agar tidak bocor walau terjadi
	 * error. Sebaliknya, setiap {@link HasilUjianMahasiswaDetail} baru disimpan lewat
	 * {@code HibernateUtil.openSession()} TERSENDIRI dengan transaksi per baris, lengkap dengan
	 * rollback pada catch serta {@code disconnect}/{@code close} pada finally. Konsekuensinya
	 * penyimpanan bersifat idempoten per baris tetapi TIDAK atomik secara keseluruhan: bila proses
	 * gagal di tengah, assignment yang sudah tersimpan tetap ada dan pemanggilan berikutnya
	 * melanjutkan dari sisa yang belum punya assignment.
	 *
	 * @param pertemuan   pertemuan konteks; dipakai untuk deteksi ujian PMB/PSB dan sebagai
	 *                    default filter "hanya ujian di pertemuan ini".
	 * @param buttonLabel label tombol toolbar yang ditampilkan.
	 * @param buttonImage path ikon tombol toolbar.
	 * @return konfigurasi tombol toolbar siap dipasang ke {@link Toolbar} pemanggil.
	 */
	public MyToolbarbuttonConfig prosesUlangSoal(final Pertemuan pertemuan, String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tanggal Ujian", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("95%");
				window.setWidth("600px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();

				rows.setParent(grid);

				final Combobox fakultas;
				final Combobox jurusan;
				fakultas = new Combobox();
				jurusan = new Combobox();
				Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

				final MyDatebox start;
				final MyDatebox end;
				start = new MyDatebox();
				end = new MyDatebox();

				if (start != null) start.setReadonly(true);
				if (end != null) end.setReadonly(true);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				if (start != null) start.setValue(calendar.getTime());
				calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 3);
				if (end != null) end.setValue(calendar.getTime());

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setVisible(pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
				row.appendChild(fakultas);
				fakultas.setWidth("90%");

				row = new MyFormRow();
				row.setVisible(pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
				row.appendChild(jurusan);
				jurusan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
				row.appendChild(start);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
				row.appendChild(end);

				row = new MyFormRow();
				row.setVisible(pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
				final AmbilDataDosenBanbox dosen;
				row.appendChild(dosen = new AmbilDataDosenBanbox());
				dosen.setWidth("90%");
				dosen.setReadonly(true);

				row = new MyFormRow();
				row.setVisible(pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final Checkbox hanya;
				row.appendChild(hanya = new Checkbox("Hanya ujian di pertemuan \"" + pertemuan.info() + "\""));
				hanya.setChecked(pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						fakultas.setDisabled(hanya.isChecked());
						jurusan.setDisabled(hanya.isChecked());
						if (start != null) start.setDisabled(hanya.isChecked());
						if (end != null) end.setDisabled(hanya.isChecked());
						dosen.setDisabled(hanya.isChecked());
					}
				};

				hanya.addEventListener("onClick", eventListener);
				eventListener.onEvent(null);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final Fakultas fak = (Fakultas) (fakultas.getSelectedItem() == null ? null
								: fakultas.getSelectedItem().getValue());
						final Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null ? null
								: jurusan.getSelectedItem().getValue());

						final boolean hny = hanya.isChecked();

						final Dosen dsn = (Dosen) (hny ? null : dosen.getAttribute("dosen"));

						window.detach();

						final Label labelmy = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						final Intbox colS = new Intbox(10);
						Clients.showBusy(labelmy.getValue());

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file;
						(file = new File(filename)).createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								try {

									Clients.showBusy(labelmy.getValue());
									System.out.println("label " + labelmy.getValue());

									if (labelmy.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (labelmy.getValue().isEmpty()) {

										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										System.out.println("loading file " + file.getAbsolutePath());
										if (center != null) {
											Common.clear(center);
										}
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										if (center != null) {
											Common.clear(center);
										}
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 3);
										spreadsheet.setMaxcolumns(colS.getValue());
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										// toolbar.setHeight("25px");
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {

												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:366");

												}
											}
										});
										print.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}

								} catch (Exception e) {
									Clients.clearBusy();
								}

							}
						});
						timer.start();

						try {

							Clients.showBusy(labelmy.getValue());

							new Thread(new Runnable() {

								@SuppressWarnings({ "unchecked" })
								@Override
								public void run() {

									try {
										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DATA SOAL PESERTA");
										sheet.setDefaultColumnWidth(20);
										XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
										lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
										lockedNumericStyle.setLocked(true);

										Session session = HibernateUtil.currentNativeSession();
										List<PertemuanPunyaUjian> pertemuanPunyaUjians =

												pertemuan.getJadwalUjianPSB() != null ?

														ConstantValues.simpleList(session
																.createCriteria(PertemuanPunyaUjian.class)

																.createAlias("pertemuan", "pertemuan")

																.add(Restrictions.eq("pertemuan.jadwalUjianPSB",
																		pertemuan.getJadwalUjianPSB()))

																.add(Restrictions.sqlRestriction(
																		"date(this_.mulai_ujian) between date('"
																				+ Common.databaseDateFormat.get()
																						.format(start.getValue())
																				+ "') and date('"
																				+ Common.databaseDateFormat.get()
																						.format(end.getValue())
																				+ "')"))

																, PertemuanPunyaUjian.class)
														:

														pertemuan.getJadwalUjianPMB() != null ?

																ConstantValues.simpleList(session
																		.createCriteria(PertemuanPunyaUjian.class)

																		.createAlias("pertemuan", "pertemuan")

																		.add(Restrictions.eq("pertemuan.jadwalUjianPMB",
																				pertemuan.getJadwalUjianPMB()))

																		.add(Restrictions.sqlRestriction(
																				"date(this_.mulai_ujian) between date('"
																						+ Common.databaseDateFormat.get()
																								.format(start
																										.getValue())
																						+ "') and date('"
																						+ Common.databaseDateFormat.get()
																								.format(end.getValue())
																						+ "')"))

																		, PertemuanPunyaUjian.class)
																:

																hny ? ConstantValues.simpleList(session
																		.createCriteria(PertemuanPunyaUjian.class)
																		.add(Restrictions.eq("pertemuan", pertemuan)),
																		PertemuanPunyaUjian.class) :

																		ConstantValues.simpleList(session
																				.createCriteria(
																						PertemuanPunyaUjian.class)

																				.createAlias("ujian", "ujian")

																				.add(fak == null
																						? Restrictions
																								.sqlRestriction("true")
																						: Restrictions.eq(
																								"ujian.fakultas", fak))
																				.add(jur == null
																						? Restrictions
																								.sqlRestriction("true")
																						: Restrictions.eq(
																								"ujian.jurusan", jur))

																				.add(Restrictions.sqlRestriction(
																						"date(this_.mulai_ujian) between date('"
																								+ Common.databaseDateFormat.get()
																										.format(start
																												.getValue())
																								+ "') and date('"
																								+ Common.databaseDateFormat.get()
																										.format(end
																												.getValue())
																								+ "')"))

																				, PertemuanPunyaUjian.class);

										XSSFRow rowhead = sheet.createRow((short) 0);
										rowhead.createCell(0).setCellValue("NIM/NO REG");
										rowhead.createCell(1).setCellValue("NAMA");
										rowhead.createCell(2).setCellValue(Common.getBahasaConfig("FAKULTAS"));
										rowhead.createCell(3).setCellValue(Common.getBahasaConfig("JURUSAN"));
										rowhead.createCell(4).setCellValue("STATUS AWAL");
										rowhead.createCell(5).setCellValue("ANGKATAN");

										rowhead.createCell(6).setCellValue("UJIAN");
										rowhead.createCell(7).setCellValue("SOAL");
										rowhead.createCell(8).setCellValue("JAWABAN HURUF");
										rowhead.createCell(9).setCellValue("JAWABAN TEKS");
										rowhead.createCell(10).setCellValue("BETUL");

										int size = pertemuanPunyaUjians.size();
										int rowIndexMhs = 0;
										int rowIndex = 0;

										for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
											Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
											if (pertemuan != null
													&& (dsn == null || (dsn.getId() != null && pertemuanPunyaUjian
															.getPertemuan().ambilDosenId().contains(dsn.getId())))) {

												List<Long> ujianPunyaSoalsTemp = pertemuanPunyaUjian.getUjian()
														.ambilUjianPunyaSoal(pertemuanPunyaUjian, true);

												System.out.println(
														"ujianPunyaSoalsTemp -> " + ujianPunyaSoalsTemp.size());

												rowIndexMhs++;

												labelmy.setValue("Sedang memproses data "
														+ pertemuanPunyaUjian.getUjian().getNama() + " ("
														+ Common.numberFormat.get().format(rowIndexMhs * 100.0 / size)
														+ " %)");

												if (pertemuan != null) {

													if (pertemuan.getJadwalUjianPMB() != null) {
														List<BiodataCalonMahasiswa> biodataCalonMahasiswas;

														if (pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB()
																.getUjianPMB() != null
																&& pertemuanPunyaUjian.getPertemuan()
																		.getJadwalUjianPMB().getUjianPMB()
																		.getGelombangPendaftaran() != null
																&& !pertemuanPunyaUjian.getPertemuan()
																		.getJadwalUjianPMB().getRuanganYgIkut()
																		.isEmpty()) {

															biodataCalonMahasiswas = ConstantValues.simpleList(
																	session.createCriteria(RuangPaketPMB.class)

																			.setProjection(Projections.property(
																					"biodataCalonMahasiswa.id"))
																			.add(Restrictions
																					.sqlRestriction("ruang_pmb in (-1"
																							+ pertemuanPunyaUjian
																									.getPertemuan()
																									.getJadwalUjianPMB()
																									.getRuanganYgIkut()
																							+ "-1)")),
																	BiodataCalonMahasiswa.class, false);

														}

														else if (pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB()
																.getUjianPMB() != null
																&& pertemuanPunyaUjian.getPertemuan()
																		.getJadwalUjianPMB().getUjianPMB()
																		.getGelombangPendaftaran() != null) {
															biodataCalonMahasiswas = ConstantValues.simpleList(
																	session.createCriteria(BiodataCalonMahasiswa.class)
																			.add(Restrictions.or(
																					Restrictions.isNull("aktif"),
																					Restrictions.eq("aktif", true)))

																			.add(pertemuanPunyaUjian.getPertemuan()
																					.getJadwalUjianPMB()
																					.getPaket() == null ? Restrictions
																							.sqlRestriction("true")
																							: Restrictions.eq("paket",
																									pertemuanPunyaUjian
																											.getPertemuan()
																											.getJadwalUjianPMB()
																											.getPaket()))
																			.add(Restrictions.eq("gelombangPendaftaran",
																					pertemuanPunyaUjian.getPertemuan()
																							.getJadwalUjianPMB()
																							.getUjianPMB()
																							.getGelombangPendaftaran()))
																			.addOrder(Order.asc("noRegistrasi")),
																	BiodataCalonMahasiswa.class);

														} else {
															biodataCalonMahasiswas = ConstantValues.simpleList(
																	session.createCriteria(RuangPaketPMB.class)
																			.setProjection(Projections.property(
																					"biodataCalonMahasiswa.id"))
																			.createAlias("ruangPMB", "ruangPMB")
																			.createAlias(
																					"biodataCalonMahasiswa",
																					"biodataCalonMahasiswa")
																			.add(Restrictions.eq("ruangPMB.ujianPMB",
																					pertemuan
																							.getJadwalUjianPMB()
																							.getUjianPMB()))
																			.add(pertemuan.getJadwalUjianPMB()
																					.getPaket() == null ? Restrictions
																							.sqlRestriction("true")
																							: Restrictions.eq(
																									"biodataCalonMahasiswa.paket",
																									pertemuan
																											.getJadwalUjianPMB()
																											.getPaket()))
																			.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim")
																							? Order.asc(
																									"biodataCalonMahasiswa.noRegistrasi")
																							: Order.asc(
																									"biodataCalonMahasiswa.nama")),
																	BiodataCalonMahasiswa.class, false);
														}

														for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {
															HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa
																	.ambilByKey(pertemuanPunyaUjian, null,
																			biodataCalonMahasiswa, null, null);

															labelmy.setValue(
																	"Sedang memproses data "
																			+ pertemuanPunyaUjian.getUjian().getNama()
																			+ "-" + biodataCalonMahasiswa.getNama()
																			+ " ("
																			+ Common.numberFormat.get()
																					.format(rowIndexMhs * 100.0 / size)
																			+ " %)");

															MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa
																	.ambilUjianPunyaSoals(
																			pertemuanPunyaUjian.getJmlDitampilkan(),
																			null, true);

															if (ujianPunyaSoals.isEmpty()) {
																ujianPunyaSoals = ProsesUjianHelper.randomPosisiton(
																		ujianPunyaSoalsTemp,
																		pertemuanPunyaUjian.getRandom(), null,
																		pertemuanPunyaUjian.getJmlDitampilkan());
															}

															ProsesUjianHelper.chekPosisitonJikaKurang(
																	ujianPunyaSoalsTemp, ujianPunyaSoals,
																	pertemuanPunyaUjian.getJmlDitampilkan());

															MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = new MyHashMap<Long, Set<Long>>(
																	pertemuanPunyaUjian.getJmlDitampilkan());

															if (hasilUjianMahasiswa != null) {
																hasilUjianMahasiswaDetailsa = hasilUjianMahasiswa
																		.ambilHasilUjianMahasiswaDetail(true,
																				pertemuanPunyaUjian.getJmlDitampilkan(),
																				null, ujianPunyaSoals);

															}

															for (Long ujianPunyaSoalid : ujianPunyaSoals) {

																UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
																		.ambilData(UjianPunyaSoal.class,
																				ujianPunyaSoalid.toString());
																if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null) {
																	Set<Long> s = hasilUjianMahasiswaDetailsa
																			.get(ujianPunyaSoal.getBankSoal().getId());
																	HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail;
																	if (s == null || s.isEmpty()) {

																		Session saveSession = null;
																		try {
																			myHasilUjianMahasiswaDetail = new HasilUjianMahasiswaDetail();
																			myHasilUjianMahasiswaDetail.setBankSoal(
																					ujianPunyaSoal.getBankSoal());
																			myHasilUjianMahasiswaDetail
																					.setHasilUjianMahasiswa(
																							hasilUjianMahasiswa);
																			myHasilUjianMahasiswaDetail
																					.setUjianPunyaSoal(ujianPunyaSoal);
																			myHasilUjianMahasiswaDetail.setNilai(
																					ujianPunyaSoal.getBankSoal()
																							.getSkorDefault());

																			saveSession = HibernateUtil.openSession();
																			saveSession.beginTransaction();
																			saveSession.save(myHasilUjianMahasiswaDetail);
																			saveSession.getTransaction().commit();

																			Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
																			hasilUjianMahasiswaDetails
																					.add(myHasilUjianMahasiswaDetail
																							.getId());
																			hasilUjianMahasiswaDetailsa.put(
																					myHasilUjianMahasiswaDetail
																							.getBankSoal().getId(),
																					hasilUjianMahasiswaDetails);
																			GeneralValueObject.masukkanData(
																					HasilUjianMahasiswaDetail.class,
																					myHasilUjianMahasiswaDetail);
																		} catch (Exception e) {
																			myHasilUjianMahasiswaDetail = null;
																			if (saveSession != null && saveSession.getTransaction() != null && saveSession.getTransaction().isActive()) {
																				try { saveSession.getTransaction().rollback(); } catch (Exception re) { re.printStackTrace(); ais.common.ErrorAuditUtil.record(re, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:699"); }
																			}
																			Common.tampilErrorJikaAdmin(e);
																		} finally {
																			if (saveSession != null && saveSession.isOpen()) {
																				try { saveSession.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:704");}
																				try { saveSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:705");}
																			}
																		}

																	} else {
																		Long myHasilUjianMahasiswaDetailid = s
																				.iterator().next();
																		myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
																				.ambilData(
																						HasilUjianMahasiswaDetail.class,
																						myHasilUjianMahasiswaDetailid
																								.toString());
																	}
																	rowIndex++;
																	XSSFRow row = sheet.createRow(rowIndex);
																	XSSFCell cell = row.createCell(0);
																	cell.setCellValue(
																			biodataCalonMahasiswa.getNoRegistrasi());

																	cell = row.createCell(1);
																	cell.setCellValue(biodataCalonMahasiswa.getNama());

																	Jurusan jurusan = biodataCalonMahasiswa
																			.getProdiLulus();
																	if (jurusan == null) {
																		jurusan = biodataCalonMahasiswa.getProdi1();
																	}

																	cell = row.createCell(2);
																	cell.setCellValue(jurusan == null ? ""
																			: jurusan.getFakultas().getNama());

																	cell = row.createCell(3);
																	cell.setCellValue(
																			jurusan == null ? "" : jurusan.getNama());

																	cell = row.createCell(4);
																	cell.setCellValue(biodataCalonMahasiswa
																			.getStatusAwalMahasiswa() == null
																					? ""
																					: biodataCalonMahasiswa
																							.getStatusAwalMahasiswa()
																							.getNama());

																	cell = row.createCell(5);
																	cell.setCellValue(biodataCalonMahasiswa.getTahun());

																	cell = row.createCell(6);
																	cell.setCellValue(
																			pertemuanPunyaUjian.getUjian().getNama());

																	String soal = ujianPunyaSoal.getBankSoal()
																			.getSoal();
																	try {
																		soal = Jsoup.parse(soal).text();
																	} catch (Exception e) {
																		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:761");
																	}

																	cell = row.createCell(7);
																	cell.setCellValue(soal);

																	cell = row.createCell(8);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? ""
																									: (myHasilUjianMahasiswaDetail
																											.getUjianPunyaSoal()
																											.getUjian()
																											.getTampilanHurufDiPilihanJawaban()
																													? myHasilUjianMahasiswaDetail
																															.getBankSoalDetail()
																															.getHuruf()
																															+ ""
																													: ""));

																	cell = row.createCell(9);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? (myHasilUjianMahasiswaDetail != null
																											? myHasilUjianMahasiswaDetail
																													.getJawaban()
																											: "")
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getJawaban());

																	cell = row.createCell(10);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? ""
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getBetul()
																											.toString());
																}
															}
														}

													} else {

														List<Mahasiswa> mahasiswas = pertemuan.ambilMahasiswa();

														for (Mahasiswa mahasiswa : mahasiswas) {
															HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa
																	.ambilByKey(pertemuanPunyaUjian, mahasiswa, null,
																			null, null);

															labelmy.setValue("Sedang memproses data "
																	+ pertemuanPunyaUjian.getUjian().getNama() + " "
																	+ mahasiswa.getNama() + " (" + Common.numberFormat.get()
																			.format(rowIndexMhs * 100.0 / size)
																	+ " %)");

															MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa
																	.ambilUjianPunyaSoals(
																			pertemuanPunyaUjian.getJmlDitampilkan(),
																			null, true);

															if (ujianPunyaSoals.isEmpty()) {
																ujianPunyaSoals = ProsesUjianHelper.randomPosisiton(
																		ujianPunyaSoalsTemp,
																		pertemuanPunyaUjian.getRandom(), null,
																		pertemuanPunyaUjian.getJmlDitampilkan());
															}

															ProsesUjianHelper.chekPosisitonJikaKurang(
																	ujianPunyaSoalsTemp, ujianPunyaSoals,
																	pertemuanPunyaUjian.getJmlDitampilkan());

															MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = new MyHashMap<Long, Set<Long>>(
																	pertemuanPunyaUjian.getJmlDitampilkan());

															if (hasilUjianMahasiswa != null) {
																hasilUjianMahasiswaDetailsa = hasilUjianMahasiswa
																		.ambilHasilUjianMahasiswaDetail(true,
																				pertemuanPunyaUjian.getJmlDitampilkan(),
																				null, ujianPunyaSoals);

															}

															for (Long ujianPunyaSoalid : ujianPunyaSoals) {

																UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
																		.ambilData(UjianPunyaSoal.class,
																				ujianPunyaSoalid.toString());
																if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null) {
																	Set<Long> s = hasilUjianMahasiswaDetailsa
																			.get(ujianPunyaSoal.getBankSoal().getId());
																	HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail;
																	if (s == null || s.isEmpty()) {

																		Session saveSession = null;
																		try {
																			myHasilUjianMahasiswaDetail = new HasilUjianMahasiswaDetail();
																			myHasilUjianMahasiswaDetail.setBankSoal(
																					ujianPunyaSoal.getBankSoal());
																			myHasilUjianMahasiswaDetail
																					.setHasilUjianMahasiswa(
																							hasilUjianMahasiswa);
																			myHasilUjianMahasiswaDetail
																					.setUjianPunyaSoal(ujianPunyaSoal);
																			myHasilUjianMahasiswaDetail.setNilai(
																					ujianPunyaSoal.getBankSoal()
																							.getSkorDefault());

																			saveSession = HibernateUtil.openSession();
																			saveSession.beginTransaction();
																			saveSession.save(myHasilUjianMahasiswaDetail);
																			saveSession.getTransaction().commit();

																			Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
																			hasilUjianMahasiswaDetails
																					.add(myHasilUjianMahasiswaDetail
																							.getId());
																			hasilUjianMahasiswaDetailsa.put(
																					myHasilUjianMahasiswaDetail
																							.getBankSoal().getId(),
																					hasilUjianMahasiswaDetails);
																			GeneralValueObject.masukkanData(
																					HasilUjianMahasiswaDetail.class,
																					myHasilUjianMahasiswaDetail);
																		} catch (Exception e) {
																			myHasilUjianMahasiswaDetail = null;
																			if (saveSession != null && saveSession.getTransaction() != null && saveSession.getTransaction().isActive()) {
																				try { saveSession.getTransaction().rollback(); } catch (Exception re) { re.printStackTrace(); ais.common.ErrorAuditUtil.record(re, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:896"); }
																			}
																			Common.tampilErrorJikaAdmin(e);
																		} finally {
																			if (saveSession != null && saveSession.isOpen()) {
																				try { saveSession.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:901");}
																				try { saveSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:902");}
																			}
																		}

																	} else {
																		Long myHasilUjianMahasiswaDetailid = s
																				.iterator().next();
																		myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
																				.ambilData(
																						HasilUjianMahasiswaDetail.class,
																						myHasilUjianMahasiswaDetailid
																								.toString());
																	}
																	rowIndex++;
																	XSSFRow row = sheet.createRow(rowIndex);
																	XSSFCell cell = row.createCell(0);
																	cell.setCellValue(mahasiswa.getNim());

																	cell = row.createCell(1);
																	cell.setCellValue(mahasiswa.getNama());

																	cell = row.createCell(2);
																	cell.setCellValue(mahasiswa.getJurusan()
																			.getFakultas().getNama());

																	cell = row.createCell(3);
																	cell.setCellValue(mahasiswa.getJurusan().getNama());

																	cell = row.createCell(4);
																	cell.setCellValue(
																			mahasiswa.getStatusAwalMahasiswa() == null
																					? ""
																					: mahasiswa.getStatusAwalMahasiswa()
																							.getNama());

																	cell = row.createCell(5);
																	cell.setCellValue(mahasiswa.getTahunangkatan());

																	cell = row.createCell(6);
																	cell.setCellValue(
																			pertemuanPunyaUjian.getUjian().getNama());

																	String soal = ujianPunyaSoal.getBankSoal()
																			.getSoal();
																	try {
																		soal = Jsoup.parse(soal).text();
																	} catch (Exception e) {
																		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:949");
																	}

																	cell = row.createCell(7);
																	cell.setCellValue(soal);

																	cell = row.createCell(8);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? ""
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getHuruf());

																	cell = row.createCell(9);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? (myHasilUjianMahasiswaDetail != null
																											? myHasilUjianMahasiswaDetail
																													.getJawaban()
																											: "")
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getJawaban());

																	cell = row.createCell(10);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? ""
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getBetul()
																											.toString());
																}
															}
														}
													}
												}

											}
										}

										intbox.setValue(rowIndex + 1);

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											Common.tampilErrorJikaAdmin(e);
										}
										System.out.println("Your excel file has been generated! ");

										labelmy.setValue("");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										labelmy.setValue("-");
									} finally {
										// Sesi native (currentNativeSession) WAJIB ditutup di finally agar tidak
										// bocor walau terjadi error/exception yang tak tertangkap di atasnya.
										HibernateUtil.closeSession();
									}
								}
							}).start();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException(
									"mengekspor data hasil ujian ke Excel",
									e, new String[] {
											"Muat ulang (refresh) halaman ini lalu coba ekspor kembali.",
											"Periksa apakah jumlah data yang akan diekspor tidak terlalu besar.",
											"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
									});
						}

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	/**
	 * Menentukan status kelayakan seorang peserta untuk mengikuti satu {@link PertemuanPunyaUjian},
	 * lalu merender tombol aksi ("Ikut Ujian" / "Lihat Hasil" / "Ubah/Perbaiki Jawaban") beserta
	 * label status ke dalam {@code arg0} (dipakai baik oleh kartu peserta maupun oleh
	 * {@link DetailPertemuanRenderer}). Nama method (typo lama "Bolek" untuk "Boleh") sengaja
	 * dipertahankan agar tidak memecah pemanggil yang sudah ada.
	 *
	 * <p>Urutan pemeriksaan (masing-masing menghentikan evaluasi lebih lanjut bila cocok):</p>
	 * <ol>
	 * <li>Untuk ujian PMB ({@code pertemuan.getJadwalUjianPMB() != null}): peserta harus berada
	 * pada jurusan/fakultas yang sesuai dengan {@code pertemuanPunyaUjian.getJurusan()}/
	 * {@code getFakultas()} — dicek lewat pilihan jurusan/fakultas {@link BiodataCalonMahasiswa}
	 * atau jurusan/fakultas {@link Mahasiswa} langsung. Tidak sesuai → tombol tidak dirender,
	 * hanya label penolakan (dan pesan popup ditambahkan ke {@code eventListeners} bila diisi).</li>
	 * <li>Selain itu, bila jendela waktu ujian sedang berlaku ATAU peserta berstatus "lengkapi
	 * jawaban" (melanjutkan sesi yang belum selesai): tombol "Ikut Ujian"/"Lihat Hasil" dirender,
	 * dengan sub-kasus tambahan untuk kuota habis, sisa waktu pengerjaan habis, dan status
	 * "tidak perlu ikut ujian" ({@code pertemuanPunyaUjian.getMhsYgTidakIkut()}). Klik tombol
	 * mendelegasikan ke {@code ProsesUjianHelper.ikut(...)} atau {@code ProsesUjianHelper.tampil(...)}.</li>
	 * <li>Selain itu (ujian belum mulai atau sudah lewat): hanya label status yang dirender,
	 * tanpa tombol aksi.</li>
	 * </ol>
	 *
	 * <p><b>Method inilah penegak jatah percobaan ujian.</b> {@code ProsesUjianHelper.ikut(...)}
	 * yang dipanggil dari sini TIDAK memeriksa {@code getJumlahBolehIkut()} sebagai gerbang; di
	 * sana angkanya hanya ditampilkan sebagai teks informasi pada jendela tata tertib. Jadi
	 * seluruh penegakan batas percobaan bergantung pada percabangan di method ini.
	 *
	 * <p><b>Kuirk: pembanding jatah tidak konsisten antara label tombol dan aksi klik.</b>
	 * Variabel {@code masihBolehIkut} memakai {@code jumlahIkut < jumlahBolehIkut} dan menentukan
	 * label tombol ("Ikut Ujian" bila masih boleh, "Lihat Hasil" bila tidak). Namun
	 * {@code eventListenerData} — listener {@code onClick} tombol yang sama — memakai
	 * {@code jumlahBolehIkut >= jumlahIkut} untuk memutuskan memanggil
	 * {@code ProsesUjianHelper.ikut(...)}. Pada kasus jatah terpakai PERSIS habis
	 * ({@code jumlahIkut == jumlahBolehIkut}), kedua ekspresi berbeda hasil: tombol berlabel
	 * "Lihat Hasil" tetapi kliknya memulai percobaan baru, dan cabang
	 * {@code else if (!masihBolehIkut)} yang seharusnya memanggil
	 * {@code ProsesUjianHelper.tampil(...)} tidak pernah tercapai karena cabang pertama sudah
	 * {@code return}. Tombolnya tetap aktif bila {@code getLihatJawabanSetelahUjian()} atau
	 * {@code getLihatNilaiSetelahUjian()} bernilai true, karena hanya di luar kondisi itulah
	 * {@code button.setDisabled(true)} dijalankan. Bandingkan pula dengan cabang label
	 * "Ubah/Perbaiki Jawaban" di bawahnya yang untuk maksud serupa memakai {@code >}, bukan
	 * {@code >=}. Perbaikannya ditangani terpisah lewat {@code task_72b24378}; jangan menyamakan
	 * operator di sini tanpa lebih dulu menelusuri jalur yang menaikkan {@code jumlahIkut}.
	 *
	 * <p><b>Efek samping:</b> tidak melakukan mutasi database — murni membangun komponen ZK dan,
	 * bila {@code eventListeners} diisi, menambahkan {@link EventListener} popup peringatan yang
	 * BELUM dijalankan (pemanggil yang memutuskan kapan menjalankannya, mis. saat klik baris grid).
	 * Klik tombol aksi sendiri mendelegasikan mutasi (mulai/lanjut ujian) ke {@code ProsesUjianHelper}.
	 *
	 * @param arg0                  komponen ZK tujuan tempat tombol/label dirender (anak ditambahkan langsung).
	 * @param pertemuanPunyaUjian   ujian pada pertemuan yang sedang dievaluasi.
	 * @param mahasiswa             peserta mahasiswa, atau {@code null} bila peserta jenis lain.
	 * @param biodataCalonMahasiswa peserta calon mahasiswa (ujian PMB), atau {@code null}.
	 * @param hasilUjianMahasiswa   progres ujian peserta ini ({@code null} berarti belum pernah dimulai —
	 *                              method langsung mengembalikan {@link Label} kosong).
	 * @param eventListener         listener yang diteruskan ke {@code ProsesUjianHelper} untuk memberi
	 *                              tahu pemanggil agar memuat ulang tampilan setelah ujian selesai/ditutup.
	 * @param eventListeners        daftar (boleh {@code null}) yang diisi dengan listener popup peringatan
	 *                              yang harus dipicu pemanggil bila ingin menampilkan alasan penolakan.
	 * @return {@link Label} status yang dirender pada kasus TIDAK BOLEH ikut ujian; {@code null}
	 *         (variabel lokal tidak pernah diisi) pada kasus BOLEH ikut ujian karena yang dirender
	 *         di sana adalah tombol, bukan label.
	 */
	public static Label tampilBolekIkutUjianAtauTidak(Component arg0, final PertemuanPunyaUjian pertemuanPunyaUjian,
			final Mahasiswa mahasiswa, final BiodataCalonMahasiswa biodataCalonMahasiswa,
			final HasilUjianMahasiswa hasilUjianMahasiswa, final EventListener eventListener,
			List<EventListener> eventListeners) {
		if (hasilUjianMahasiswa == null) {
			return new Label();
		}
		Label label = null;
		Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
		if (pertemuan.getJadwalUjianPMB() != null && pertemuanPunyaUjian.getJurusan() != null
				&& biodataCalonMahasiswa != null && !biodataCalonMahasiswa.populatePilihanJurusanIds()
						.contains(pertemuanPunyaUjian.getJurusan().getId())) {
			if (eventListeners != null)
				eventListeners.add(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.showFormat(
				"Mohon maaf, Anda tidak dapat mengikuti ujian ini karena ujian ini hanya diperuntukkan bagi {V1} {V2}. Langkah yang dapat dilakukan: (1) pastikan Anda terdaftar pada program studi atau fakultas yang sesuai; (2) hubungi bagian Akademik apabila terdapat ketidaksesuaian data.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				Common.getBahasaConfig("jurusan"), pertemuanPunyaUjian.getJurusan().getNama());
					}
				});
			arg0.appendChild(
					label = new MyLabelKecil("Anda tidak bisa mengikuti ujian ini, karena ujian ini hanya untuk "
							+ Common.getBahasaConfig("jurusan") + " " + pertemuanPunyaUjian.getJurusan().getNama()));
		} else if (pertemuan.getJadwalUjianPMB() != null && pertemuanPunyaUjian.getFakultas() != null
				&& biodataCalonMahasiswa != null && !biodataCalonMahasiswa.populatePilihanFakultasIds()
						.contains(pertemuanPunyaUjian.getFakultas().getId())) {
			if (eventListeners != null)
				eventListeners.add(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.showFormat(
				"Mohon maaf, Anda tidak dapat mengikuti ujian ini karena ujian ini hanya diperuntukkan bagi {V1} {V2}. Langkah yang dapat dilakukan: (1) pastikan Anda terdaftar pada program studi atau fakultas yang sesuai; (2) hubungi bagian Akademik apabila terdapat ketidaksesuaian data.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				Common.getBahasaConfig("fakultas"), pertemuanPunyaUjian.getFakultas().getNama());
					}
				});

			arg0.appendChild(
					label = new MyLabelKecil("Anda tidak bisa mengikuti ujian ini, karena ujian ini hanya untuk "
							+ Common.getBahasaConfig("fakultas") + " " + pertemuanPunyaUjian.getFakultas().getNama()));
		} else if (pertemuan.getJadwalUjianPMB() != null && pertemuanPunyaUjian.getJurusan() != null
				&& mahasiswa != null && mahasiswa.getJurusan() != null
				&& !mahasiswa.getJurusan().getId().equals(pertemuanPunyaUjian.getJurusan().getId())) {
			if (eventListeners != null)
				eventListeners.add(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.showFormat(
				"Mohon maaf, Anda tidak dapat mengikuti ujian ini karena ujian ini hanya diperuntukkan bagi {V1} {V2}. Langkah yang dapat dilakukan: (1) pastikan Anda terdaftar pada program studi atau fakultas yang sesuai; (2) hubungi bagian Akademik apabila terdapat ketidaksesuaian data.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				Common.getBahasaConfig("jurusan"), pertemuanPunyaUjian.getJurusan().getNama());
					}
				});

			arg0.appendChild(
					label = new MyLabelKecil("Anda tidak bisa mengikuti ujian ini, karena ujian ini hanya untuk "
							+ Common.getBahasaConfig("jurusan") + " " + pertemuanPunyaUjian.getJurusan().getNama()));
		} else if (pertemuan.getJadwalUjianPMB() != null && pertemuanPunyaUjian.getFakultas() != null
				&& mahasiswa != null && mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
				&& !mahasiswa.getJurusan().getFakultas().getId().equals(pertemuanPunyaUjian.getFakultas().getId())) {
			if (eventListeners != null)
				eventListeners.add(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.showFormat(
				"Mohon maaf, Anda tidak dapat mengikuti ujian ini karena ujian ini hanya diperuntukkan bagi {V1} {V2}. Langkah yang dapat dilakukan: (1) pastikan Anda terdaftar pada program studi atau fakultas yang sesuai; (2) hubungi bagian Akademik apabila terdapat ketidaksesuaian data.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				Common.getBahasaConfig("fakultas"), pertemuanPunyaUjian.getFakultas().getNama());
					}
				});

			arg0.appendChild(
					label = new MyLabelKecil("Anda tidak bisa mengikuti ujian ini, karena ujian ini hanya untuk "
							+ Common.getBahasaConfig("fakultas") + " " + pertemuanPunyaUjian.getFakultas().getNama()));
		} else {

			if ((pertemuanPunyaUjian.getMulaiUjian() == null
					|| pertemuanPunyaUjian.getMulaiUjian().before(ais.ui.util.WaktuUtil.getDate()))
					&& (pertemuanPunyaUjian.getSampaiUjian() == null
							|| pertemuanPunyaUjian.getSampaiUjian().after(ais.ui.util.WaktuUtil.getDate()))
					|| (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getLengkapiJawaban())) {

				final boolean masihBolehIkut = (hasilUjianMahasiswa == null
						|| hasilUjianMahasiswa.getJumlahIkut() < pertemuanPunyaUjian.getJumlahBolehIkut())
						|| (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getLengkapiJawaban());

				// "Tidak perlu ikut ujian": mahasiswa yang dicentang tidak perlu ikut
				// (pertemuanPunyaUjian.mhsYgTidakIkut) DIBLOKIR saat klik "Ikut Ujian" + diberi
				// balon/info kecil di dekat tombol.
				Tbmuser tbmuserCekIkut = Common.getCurrentUser();
				final Long idCekIkutUjian = mahasiswa != null ? mahasiswa.getId()
						: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
								: (tbmuserCekIkut != null && tbmuserCekIkut.getMahasiswa() != null)
										? tbmuserCekIkut.getMahasiswa().getId()
										: (tbmuserCekIkut != null && tbmuserCekIkut.getSiswa() != null)
												? tbmuserCekIkut.getSiswa().getId()
												: (tbmuserCekIkut != null && tbmuserCekIkut.getCalonSiswa() != null)
														? tbmuserCekIkut.getCalonSiswa().getId()
														: null;
				final boolean tidakPerluIkutUjian = idCekIkutUjian != null
						&& pertemuanPunyaUjian.getMhsYgTidakIkut() != null
						&& pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + idCekIkutUjian + ",");

				final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
						!masihBolehIkut ? "Lihat Hasil" : "Ikut Ujian",
						!masihBolehIkut ? "/img/eye-icon.png" : "/img/stock_data_edit_table.png");
				button.setOrient("vertical");
				Tbmuser tbmuser = Common.getCurrentUser();
				button.setVisible(mahasiswa != null || biodataCalonMahasiswa != null
						|| (tbmuser != null && tbmuser.getPesertaKursus() != null)
						|| (tbmuser != null && tbmuser.getSiswa() != null)
						|| (tbmuser != null && tbmuser.getCalonSiswa() != null));
				button.setTooltiptext(!masihBolehIkut ? "Lihat Hasil" : "Ikut Ujian");
				Vbox toolbar = new Vbox();

				if (hasilUjianMahasiswa != null) {
					if (hasilUjianMahasiswa.getSelesaiPada() == null) {
						toolbar.appendChild(new MyLabelBoldMerah("Belum Ikut Ujian"));
					} else {
						toolbar.appendChild(new MyLabelAgakKecilBoldHijau("Selesai dikerjakan pada "
								+ Common.dateFormat61.get().format(hasilUjianMahasiswa.getSelesaiPada())));
					}
				}

				EventListener eventListenerData = new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();

						if (tidakPerluIkutUjian) {
							MyMessageboxConfig.show(
				"Bapak/Ibu tidak perlu mengikuti ujian ini, sehingga tidak ada yang perlu dikerjakan.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getSisaWaktuPengerjaan() != null
								&& Double.parseDouble(Common.timeFormat2.get()
										.format(hasilUjianMahasiswa.getSisaWaktuPengerjaan())) < 0.01) {
							MyMessageboxConfig.show(
				"Mohon maaf, sisa waktu pengerjaan telah habis sehingga Anda tidak dapat lagi mengerjakan ujian ini.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getLengkapiJawaban()
								|| (hasilUjianMahasiswa.getJumlahIkut() > 0 && pertemuanPunyaUjian
										.getJumlahBolehIkut() >= hasilUjianMahasiswa.getJumlahIkut())) {
							ProsesUjianHelper.ikut(mahasiswa, biodataCalonMahasiswa,
									tbmuser == null ? null : tbmuser.getSiswa(),
									tbmuser == null ? null : tbmuser.getCalonSiswa(), pertemuanPunyaUjian,
									hasilUjianMahasiswa, true, eventListener);
							return;
						} else if (!masihBolehIkut) {
							ProsesUjianHelper.tampil(mahasiswa, biodataCalonMahasiswa,
									tbmuser == null ? null : tbmuser.getSiswa(),
									tbmuser == null ? null : tbmuser.getCalonSiswa(), pertemuanPunyaUjian, true,
									eventListener, true);
							return;
						}

						ProsesUjianHelper.ikut(mahasiswa, biodataCalonMahasiswa,
								tbmuser == null ? null : tbmuser.getSiswa(),
								tbmuser == null ? null : tbmuser.getCalonSiswa(), pertemuanPunyaUjian,
								hasilUjianMahasiswa, true, eventListener);

					}
				};

				if ((mahasiswa != null || biodataCalonMahasiswa != null
						|| (tbmuser != null && tbmuser.getPesertaKursus() != null)
						|| (tbmuser != null && tbmuser.getSiswa() != null)
						|| (tbmuser != null && tbmuser.getCalonSiswa() != null))) {
					if (button.getLabel().equalsIgnoreCase("Lihat Hasil")
							&& !(pertemuanPunyaUjian.getLihatJawabanSetelahUjian()
									|| pertemuanPunyaUjian.getLihatNilaiSetelahUjian())) {
						button.setDisabled(true);
						try {

							toolbar.appendChild(new MyLabelAgakKecilBoldBiru("Anda telah mengikuti ujian ini sebanyak "
									+ hasilUjianMahasiswa.getJumlahIkut() + " kali dari total boleh ikut sebanyak "
									+ pertemuanPunyaUjian.getJumlahBolehIkut() + " kali"
									+ (hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? ""
											: ", sisa waktu " + Common.timeFormat.get()
													.format(hasilUjianMahasiswa.getSisaWaktuPengerjaan()))));
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:1229");
						}

						if (eventListeners != null)
							eventListeners.add(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									MyMessageboxConfig.showFormat(
				"Bapak/Ibu telah mengikuti ujian ini sebanyak {V1} kali dari total {V2} kali yang diperbolehkan{V3}.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				hasilUjianMahasiswa.getJumlahIkut(), pertemuanPunyaUjian.getJumlahBolehIkut(),
				(hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? "" : ", sisa waktu " + Common.timeFormat.get().format(hasilUjianMahasiswa.getSisaWaktuPengerjaan())));
								}
							});
					}

					else if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getSisaWaktuPengerjaan() != null
							&& Double.parseDouble(
									Common.timeFormat2.get().format(hasilUjianMahasiswa.getSisaWaktuPengerjaan())) < 0.01) {
						button.setDisabled(true);
						try {
							toolbar.appendChild(new MyLabelAgakKecilBoldBiru(
									"Sisa waktu pengerjaan telah habis, Anda telah mengikuti ujian ini sebanyak "
											+ hasilUjianMahasiswa.getJumlahIkut()
											+ " kali dari total boleh ikut sebanyak "
											+ pertemuanPunyaUjian.getJumlahBolehIkut() + " kali"
											+ (hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? ""
													: ", sisa waktu " + Common.timeFormat.get()
															.format(hasilUjianMahasiswa.getSisaWaktuPengerjaan()))));
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:1260");
						}
//						toolbar.appendChild(new MyLabelBoldMerah("Sisa waktu pengerjaan telah habis"));

						if (eventListeners != null)
							eventListeners.add(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									MyMessageboxConfig.showFormat(
				"Mohon maaf, sisa waktu pengerjaan telah habis. Bapak/Ibu telah mengikuti ujian ini sebanyak {V1} kali dari total {V2} kali yang diperbolehkan{V3}.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				hasilUjianMahasiswa.getJumlahIkut(), pertemuanPunyaUjian.getJumlahBolehIkut(),
				(hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? "" : ", sisa waktu " + Common.timeFormat.get().format(hasilUjianMahasiswa.getSisaWaktuPengerjaan())));
								}
							});
					}

					else if (hasilUjianMahasiswa != null && (hasilUjianMahasiswa.getLengkapiJawaban()
							|| (hasilUjianMahasiswa.getJumlahIkut() > 0 && pertemuanPunyaUjian
									.getJumlahBolehIkut() > hasilUjianMahasiswa.getJumlahIkut()))) {
						try {
							button.setLabel("Ubah/Perbaiki Jawaban");
							toolbar.appendChild(new MyLabelAgakKecilBoldBiru("Anda telah mengikuti ujian ini sebanyak "
									+ hasilUjianMahasiswa.getJumlahIkut() + " kali dari total boleh ikut sebanyak "
									+ pertemuanPunyaUjian.getJumlahBolehIkut() + " kali"
									+ (hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? ""
											: ", sisa waktu " + Common.timeFormat.get()
													.format(hasilUjianMahasiswa.getSisaWaktuPengerjaan()))));
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:1290");
						}
					}
				}

				else if (!masihBolehIkut && !(pertemuanPunyaUjian.getLihatJawabanSetelahUjian()
						|| pertemuanPunyaUjian.getLihatNilaiSetelahUjian())) {
					button.setDisabled(true);
					try {
						toolbar.appendChild(new MyLabelAgakKecilBoldBiru(
								"Ujian telah selesai/terlewat, Anda telah mengikuti ujian ini sebanyak "
										+ hasilUjianMahasiswa.getJumlahIkut() + " kali dari total boleh ikut sebanyak "
										+ pertemuanPunyaUjian.getJumlahBolehIkut() + " kali"
										+ (hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? ""
												: ", sisa waktu " + Common.timeFormat.get()
														.format(hasilUjianMahasiswa.getSisaWaktuPengerjaan()))));
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:1307");
					}
//					toolbar.appendChild(new MyLabelBoldMerah("Ujian telah selesai/terlewat"));

					if (eventListeners != null)
						eventListeners.add(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								MyMessageboxConfig.showFormat(
				"Mohon maaf, ujian telah selesai atau terlewat. Bapak/Ibu telah mengikuti ujian ini sebanyak {V1} kali dari total {V2} kali yang diperbolehkan{V3}.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				hasilUjianMahasiswa.getJumlahIkut(), pertemuanPunyaUjian.getJumlahBolehIkut(),
				(hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? "" : ", sisa waktu " + Common.timeFormat.get().format(hasilUjianMahasiswa.getSisaWaktuPengerjaan())));
							}
						});

				} else if (hasilUjianMahasiswa != null
						&& (hasilUjianMahasiswa.getSisaWaktuPengerjaan() != null && Double.parseDouble(
								Common.timeFormat2.get().format(hasilUjianMahasiswa.getSisaWaktuPengerjaan())) < 0.01)) {
					button.setDisabled(true);
					try {
						toolbar.appendChild(new MyLabelAgakKecilBoldBiru(
								"Sisa waktu pengerjaan telah habis, Anda telah mengikuti ujian ini sebanyak "
										+ hasilUjianMahasiswa.getJumlahIkut() + " kali dari total boleh ikut sebanyak "
										+ pertemuanPunyaUjian.getJumlahBolehIkut() + " kali"
										+ (hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? ""
												: ", sisa waktu " + Common.timeFormat.get()
														.format(hasilUjianMahasiswa.getSisaWaktuPengerjaan()))));
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:1337");
					}
//					toolbar.appendChild(new MyLabelBoldMerah("Sisa waktu pengerjaan telah habis"));

					if (eventListeners != null)
						eventListeners.add(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								MyMessageboxConfig.showFormat(
				"Mohon maaf, sisa waktu pengerjaan telah habis. Bapak/Ibu telah mengikuti ujian ini sebanyak {V1} kali dari total {V2} kali yang diperbolehkan{V3}.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				hasilUjianMahasiswa.getJumlahIkut(), pertemuanPunyaUjian.getJumlahBolehIkut(),
				(hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? "" : ", sisa waktu " + Common.timeFormat.get().format(hasilUjianMahasiswa.getSisaWaktuPengerjaan())));
							}
						});

				} else {
					if (eventListeners != null)
						eventListeners.add(eventListenerData);

				}
				if (tidakPerluIkutUjian) {
					button.setTooltiptext("Anda tidak perlu mengikuti ujian ini");
					toolbar.appendChild(new MyLabelBoldMerah("Anda tidak perlu mengikuti ujian ini"));
				}
				button.addEventListener("onClick", eventListenerData);
				button.setParent(toolbar);
				toolbar.setParent(arg0);

				if (Common.bolehKonfigurasi("setelah_klik_selesai_tidak_boleh_ikut_ujian_kembali", Konfigurasi.TIDAK_AKTIF)) {
					if (hasilUjianMahasiswa.getTelahIkutUjian() && !hasilUjianMahasiswa.getLengkapiJawaban()) {
						button.setVisible(false);
					}
				}

			} else {

				Vbox toolbar = new Vbox();
				toolbar.setParent(arg0);

				if (hasilUjianMahasiswa != null) {
					if (hasilUjianMahasiswa.getSelesaiPada() == null) {
						toolbar.appendChild(new MyLabelBoldMerah("Belum Ikut Ujian"));
					} else {
						toolbar.appendChild(new MyLabelAgakKecilBoldHijau("Selesai dikerjakan pada "
								+ Common.dateFormat61.get().format(hasilUjianMahasiswa.getSelesaiPada())));
					}
				}

				if (pertemuanPunyaUjian.getMulaiUjian() != null
						&& pertemuanPunyaUjian.getMulaiUjian().after(ais.ui.util.WaktuUtil.getDate())) {
					if (eventListeners != null)
						eventListeners.add(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								MyMessageboxConfig.showFormat(
				"Mohon maaf, ujian belum dimulai. Ujian akan dimulai pada {V1} {V2}. Silakan kembali pada waktu yang telah ditentukan.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null), Common.dateFormat5.get().format(pertemuanPunyaUjian.getMulaiUjian()));
							}
						});

					toolbar.appendChild(label = new MyLabelKecil("Ujian belum mulai, ujian akan dimulai "
							+ SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null) + " "
							+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getMulaiUjian())));
				} else if (pertemuanPunyaUjian.getSampaiUjian() != null
						&& pertemuanPunyaUjian.getSampaiUjian().before(ais.ui.util.WaktuUtil.getDate())) {
					if (eventListeners != null)
						eventListeners.add(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								MyMessageboxConfig.showFormat(
				"Mohon maaf, ujian telah terlewat. Ujian telah berakhir pada {V1} {V2}.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null), Common.dateFormat5.get().format(pertemuanPunyaUjian.getSampaiUjian()));
							}
						});

					toolbar.appendChild(label = new MyLabelKecil("Ujian telah terlewat, ujian telah berakhir "
							+ SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null) + " "
							+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getSampaiUjian())));
				} else {
					if (eventListeners != null)
						eventListeners.add(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								MyMessageboxConfig.show(
				"Mohon maaf, ujian telah terlewat atau belum dimulai. Silakan periksa kembali jadwal ujian Anda.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						});

					toolbar.appendChild(label = new MyLabelKecil("Ujian telah terlewat atau belum mulai"));
				}
				label.setStyle("font-size:11px;color:red;");
			}
		}

		return label;
	}

	/**
	 * Renderer {@code MyGrid} yang menerjemahkan satu {@link PertemuanPunyaUjian} menjadi satu
	 * baris berisi SEMUA kontrol tampilan/pengaturan ujian tersebut. Ini adalah renderer "lama"
	 * (sebelum tampilan kartu {@link PertemuanPunyaUjianHelper#buatKartuUjianRingkas} ditambahkan)
	 * yang tetap dipertahankan dan dipakai ulang dari dua tempat: grid tabel penuh di
	 * {@link PertemuanPunyaUjianHelper#loadData(Object)} (jalur yang kini efektif tidak
	 * tereksekusi — lihat kuirk field {@code grid} pada Javadoc kelas induk) dan, yang aktif
	 * dipakai sekarang, grid 1-baris di dalam modal
	 * {@link PertemuanPunyaUjianHelper#bukaPengaturanUjian(PertemuanPunyaUjian, EventListener)}.
	 *
	 * <p><b>Scope:</b> tipe {@code static} — tidak menangkap instance
	 * {@link PertemuanPunyaUjianHelper}; identitas peserta/pengelola diberikan lewat konstruktor.
	 *
	 * <p><b>State:</b> {@code detailUjianHelper} (dibuat baru per renderer, dipakai untuk membuka
	 * window "Kelola Soal Ujian"), {@code mahasiswa}/{@code biodataCalonMahasiswa} (identitas
	 * peserta, {@code null} untuk pengelola), {@code eventListener} (dipanggil setelah aksi yang
	 * mengubah data agar pemanggil me-refresh), {@code tampilInfo} (saklar tampilan: {@code true}
	 * merender blok info ringkas pertemuan/dosen/jadwal read-only untuk peserta,
	 * {@code false} — satu-satunya nilai yang dipakai kedua pemanggil di file ini saat ini —
	 * merender kontrol admin penuh: checkbox dibatasi waktu/acak/anti-curang/dsb., datebox/timebox
	 * jadwal, kombo pindah pertemuan, bulk assignment Sub-CPMK, dan baris tombol aksi).
	 *
	 * <p><b>Efek samping:</b> {@link #render(Row, Object)} adalah satu-satunya operasi lokal, dan
	 * BANYAK kontrol di dalamnya menyimpan perubahan langsung ke DB saat {@code onChange}/
	 * {@code onCheck} lewat {@code Common.refreshUpdate(session, ppu)} (autosave per field) —
	 * lihat Javadoc {@link #render(Row, Object)} untuk rinciannya.
	 *
	 * @see PertemuanPunyaUjianHelper
	 */
	public static class DetailPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Helper pengelola bank soal satu {@link Ujian} ("Kelola Soal Ujian"), dibuat sekali per
		 * renderer dan dipakai ulang untuk SEMUA baris yang dirender oleh instance ini. Dibuka lewat
		 * tombol "Kelola Soal Ujian" pada {@link #render(Row, Object)}, di dalam {@link MyWindow}
		 * tersendiri. Karena satu instance dipakai lintas baris, helper ini menyimpan state ujian
		 * terakhir yang dibuka; hal ini tidak menimbulkan masalah pada pemakaian nyata di file ini
		 * karena kedua pemanggil merender paling banyak satu baris pada satu waktu (grid modal
		 * {@code gridModal} berisi tepat satu {@link PertemuanPunyaUjian}).
		 */
		private DetailUjianHelper detailUjianHelper = new DetailUjianHelper();
		/**
		 * Identitas peserta bila baris dirender untuk seorang mahasiswa; {@code null} bila dirender
		 * untuk pengelola/dosen atau untuk jenis peserta lain (siswa, calon siswa, peserta kursus,
		 * calon mahasiswa). Nilainya diteruskan apa adanya ke
		 * {@link PertemuanPunyaUjianHelper#tampilBolekIkutUjianAtauTidak} dan menentukan apakah blok
		 * kontrol pengaturan admin dirender atau tidak.
		 */
		private Mahasiswa mahasiswa;
		/**
		 * Identitas peserta bila baris dirender untuk seorang calon mahasiswa (ujian PMB);
		 * {@code null} untuk pengelola/dosen atau jenis peserta lain. Bersama {@link #mahasiswa}
		 * membentuk pasangan "identitas peserta" yang sama seperti pada kelas induk
		 * {@link PertemuanPunyaUjianHelper}.
		 */
		private BiodataCalonMahasiswa biodataCalonMahasiswa;
		/**
		 * Callback yang dipicu setelah aksi yang mengubah data pada baris ini — memindahkan ujian ke
		 * pertemuan lain, menutup window "Kelola Soal Ujian", menghapus ujian, atau menyelesaikan
		 * sesi ujian peserta — agar pemanggil (kartu/daftar ujian) memuat ulang tampilannya. Boleh
		 * {@code null}; setiap titik pemakaian menjaga sendiri terhadap {@code null}.
		 */
		private EventListener eventListener;
		/**
		 * Saklar mode tampilan baris. {@code true} merender blok info ringkas read-only (pertemuan,
		 * dosen, jadwal) yang cocok untuk peserta; {@code false} merender kontrol pengaturan admin
		 * penuh (checkbox, datebox/timebox, kombo pindah pertemuan, editor bobot Sub-CPMK, baris
		 * tombol aksi). Kedua pemanggil di file ini —
		 * {@link PertemuanPunyaUjianHelper#loadData(Object)} dan
		 * {@link PertemuanPunyaUjianHelper#bukaPengaturanUjian(PertemuanPunyaUjian, EventListener)} —
		 * saat ini selalu mengirim {@code false}; nilai {@code true} dipertahankan untuk pemanggil
		 * lain di luar file ini.
		 */
		private boolean tampilInfo;

		/**
		 * @param mahasiswa             peserta mahasiswa, atau {@code null} untuk pengelola/peserta lain.
		 * @param biodataCalonMahasiswa peserta calon mahasiswa (ujian PMB), atau {@code null}.
		 * @param eventListener         dipanggil setelah aksi yang mengubah data (pindah pertemuan, hapus,
		 *                              tutup window "Kelola Soal Ujian", dsb.) agar pemanggil me-refresh.
		 * @param tampilInfo            {@code true} untuk blok info ringkas read-only (peserta),
		 *                              {@code false} untuk kontrol pengaturan admin penuh.
		 */
		public DetailPertemuanRenderer(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
				EventListener eventListener, boolean tampilInfo) {
			this.mahasiswa = mahasiswa;
			this.biodataCalonMahasiswa = biodataCalonMahasiswa;
			this.eventListener = eventListener;
			this.tampilInfo = tampilInfo;
		}

		/**
		 * Merender satu baris grid untuk satu {@link PertemuanPunyaUjian} ({@code data}). Method ini
		 * adalah jantung tampilan/pengaturan detail ujian, membangun (tergantung peran pemanggil dan
		 * {@code tampilInfo}):
		 * <ul>
		 * <li>Tombol "Kelola Soal Ujian" (membuka {@code detailUjianHelper} dalam {@link MyWindow}
		 * tersendiri; hanya untuk pengelola) dan riwayat revisi lewat
		 * {@code RevisiHelper.createNewRevisi(PertemuanPunyaUjian.class, ...)}.</li>
		 * <li>Kombo "Pindahkan ke pertemuan" (hanya pengelola) yang memindahkan
		 * {@code pertemuanPunyaUjian.setPertemuan(...)} ke pertemuan lain pada VOPembelajaran yang
		 * sama, setelah konfirmasi dialog.</li>
		 * <li>Bila {@code tampilInfo=false} &amp; pengelola: checkbox pengaturan (lanjut otomatis
		 * saat koneksi putus, larang tombol kembali, sembunyikan bila waktu lewat), field jumlah
		 * soal ditampilkan/boleh ikut/dibatasi waktu/durasi/mulai-sampai/acak/anti-tangkap-layar/
		 * lihat jawaban-nilai setelah ujian — SEMUA tersimpan otomatis ke DB saat diubah.</li>
		 * <li>Bila kurikulum perkuliahan berstatus OBE: editor bobot Sub-CPMK (per soal, format
		 * cepat rentang nomor "1-10 sub cpmk 2", info bobot gabungan antar-ujian lewat
		 * {@link PertemuanPunyaUjianHelper#buildInfoBobotInline}) plus tombol
		 * Cetak/Sinkronkan Nilai/Refresh.</li>
		 * <li>Baris tombol aksi (Sertifikat, Hasil, Preview, Ubah, Hapus) lewat
		 * {@code ais.ui.util.UIHelper.buatBarisAksi}; tombol Hapus menghapus
		 * {@link HasilUjianMahasiswaDetail}+{@link HasilUjianMahasiswa} terkait via SQL mentah lalu
		 * {@code Common.refreshDelete} pada {@code pertemuanPunyaUjian} sendiri, dibungkus konfirmasi.</li>
		 * <li>Untuk peserta: memanggil
		 * {@link PertemuanPunyaUjianHelper#tampilBolekIkutUjianAtauTidak} untuk tombol ikut/lihat hasil,
		 * dan penjagaan kuota ujian penuh / larangan ikut yang membekukan baris ({@code Common.freeze}).</li>
		 * </ul>
		 *
		 * <p><b>Efek samping:</b> banyak titik menulis ke DB langsung via {@code Common.refreshUpdate}/
		 * {@code refreshDelete} tanpa transaksi eksplisit terpisah (mengandalkan sesi Hibernate
		 * per-request), memicu {@code eventListener} setelah operasi yang mengubah relasi
		 * (pindah pertemuan, tutup window Kelola Soal), dan pada checkbox "Random" memanggil
		 * ulang {@code render(arg0, pertemuanPunyaUjian)} sendiri setelah {@code Common.clear(arg0)}
		 * untuk menyegarkan seluruh baris.
		 *
		 * <p><b>Merender saja sudah bisa mengubah data.</b> Selain autosave per field di atas, ada
		 * tiga mutasi yang terjadi tanpa pengguna menyentuh kontrol apa pun:
		 * <ul>
		 * <li>Bila {@code pertemuanPunyaUjian.getJmlDitampilkan()} kosong atau {@code <= 0},
		 * jumlah soal riil dihitung dari {@code ujian.ambilUjianPunyaSoal(...)} lalu DISIMPAN ke
		 * database lewat {@code Common.refreshUpdate}. Membuka modal pengaturan pada ujian yang
		 * belum pernah dikonfigurasi karena itu menulis satu baris.</li>
		 * <li>Bila konfigurasi {@code tampilkan_ujian_dibatasi_waktu} nonaktif, checkbox
		 * "Ujian ini dibatasi waktu" disembunyikan dan {@code setDibatasiWaktu(true)} dipanggil
		 * TANPA {@code refreshUpdate}. Entity menjadi kotor di sesi Hibernate dan bisa ikut
		 * ter-flush oleh operasi lain pada request yang sama — perubahan yang tampak "muncul
		 * sendiri" dan sulit dilacak.</li>
		 * <li>Bila {@code getUjian()} {@code null} padahal id ada, entity di-{@code refresh} dari
		 * database dan entri cache {@code ProsesUjianHelper.kuotaUjian} milik peserta ini dibuang.</li>
		 * </ul>
		 *
		 * <p><b>Gerbang peran di dalam method ini tidak seragam.</b> Predikat "pengguna adalah
		 * pengelola" dieja ulang manual di empat blok dengan isi berbeda: blok "Kelola Soal Ujian"
		 * dan blok tiga checkbox autosave memeriksa {@code getPesertaKursus()}/{@code getSiswa()}/
		 * {@code getCalonSiswa()} tetapi TIDAK memeriksa {@code tbmuser.getMahasiswa()} maupun
		 * {@code getBiodataCalonMahasiswa()}, sedangkan visibilitas tombol Hasil/Preview/Ubah/Hapus
		 * memeriksa keduanya tetapi justru TIDAK memeriksa {@code getPesertaKursus()} — padahal
		 * peserta kursus diperlakukan sebagai peserta pada dua blok lain di method yang sama. Hanya
		 * blok "Pindahkan ke pertemuan" yang mengejanya lengkap. Pada beberapa varian
		 * {@code getSiswa() == null} bahkan ditulis dua sampai tiga kali. Jangan menyalin salah
		 * satu ejaan ini ke kode baru; penyeragamannya ditangani terpisah lewat
		 * {@code task_d45feed7}.
		 *
		 * @param arg0 baris grid tujuan yang akan diisi komponen.
		 * @param data instance {@link PertemuanPunyaUjian} yang akan dirender (di-cast langsung, NPE
		 *             bila tipe lain).
		 */
		@SuppressWarnings("deprecation")
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			arg0.setValign("top");
			Session session = HibernateUtil.currentSession();
			Tbmuser tbmuser = Common.getCurrentUser();
			final PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data;
			final Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
			if (pertemuan != null) {
				pertemuan.masukkanData("ujian_" + pertemuanPunyaUjian.getId());
			}
			final HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(pertemuanPunyaUjian,
					mahasiswa, biodataCalonMahasiswa, null, null);
			if (pertemuanPunyaUjian.getUjian() == null && pertemuanPunyaUjian.getId() != null) {
				HibernateUtil.currentSession().refresh(pertemuanPunyaUjian);
				ProsesUjianHelper.kuotaUjian.remove(hasilUjianMahasiswa.getKeyhasil());
			}

			final Ujian ujian = pertemuanPunyaUjian.getUjian();

			HasilUjianHelper.reinitUjian(ujian, pertemuan);

			if (tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
					&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null) {
				// Sebelumnya berupa MyDetail (expander "+") yang membuka konten soal INLINE.
				// Diubah: konten soal dibuka PENUH di dalam MyWindow tersendiri (bukan Detail),
				// sesuai permintaan agar "full menginduk ke MyWindow".
				MyToolbarbuttonConfig btnKelolaSoal = new MyToolbarbuttonConfig("Kelola Soal Ujian",
						"/img/svg/edit-box-line.svg");
				btnKelolaSoal.setStyle(
						"background:#0f766e;color:#fff;border-radius:8px;padding:7px 12px;font-weight:700;font-size:11px;");
				btnKelolaSoal.setParent(arg0);
				btnKelolaSoal.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event ev) throws Exception {
						boolean tampilMenuSoalDiManajemenUjian = Common
								.bolehKonfigurasi("tampil_menu_soal_di_manajemen_ujian");
						MyWindow win = new MyWindow();
						win.setTitle("Kelola Soal Ujian - " + (ujian == null ? "" : ujian.getNama()));
						win.setClosable(true);
						win.setWidth(Common.isMobile() ? "100%" : "95%");
						win.setHeight(Common.isMobile() ? "100%" : "95%");
						win.setContentStyle("overflow:auto;background:#fff;");
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);
						detailUjianHelper.display(ujian, win, pertemuan, pertemuanPunyaUjian,
								tampilMenuSoalDiManajemenUjian, false);
						// Saat jendela "Kelola Soal Ujian" DITUTUP (Simpan/Batal/X/Esc), muat ulang daftar ujian
						// di grid dari DATABASE — perubahan soal/anti-curang/dll disimpan langsung, sehingga kartu
						// di belakang harus diperbarui otomatis tanpa klik Refresh manual.
						win.addEventListener("onClose", new EventListener() {
							@Override
							public void onEvent(Event evClose) throws Exception {
								try {
									if (eventListener != null) {
										eventListener.onEvent(evClose);
									}
								} catch (Exception exReload) {
									Common.tampilErrorJikaAdmin(exReload);
								}
							}
						});
						win.onModal();
					}
				});
			} else {
				new Label().setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			RevisiHelper.createNewRevisi(PertemuanPunyaUjian.class, pertemuanPunyaUjian, ujian.getNama())
					.setParent(vbox);

			Number tg = pertemuanPunyaUjian.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
			MyLabelKecil labelKecil = new MyLabelKecil(
					"Ikut Ujian : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
			labelKecil.setStyle("font-size:8px;color:blue;");
			labelKecil.setParent(vbox);

			/*
			 * === Pindahkan ujian ke pertemuan lain ===
			 * Combobox berisi DAFTAR PERTEMUAN yang tersedia pada VOPembelajaran yang SAMA
			 * (ambilVOPembelajaran()) sehingga ujian ini bisa dipindah "ke pertemuan ke berapa"
			 * — sama seperti TugasMandiriHelper. PertemuanPunyaUjian menyimpan relasi Pertemuan
			 * langsung (setPertemuan(Pertemuan)); hasil ujian terkait ikut otomatis. Hanya untuk
			 * pengelola (bukan mahasiswa/siswa/calon).
			 */
			if (pertemuan != null && tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
					&& tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null) {
				final ais.database.model.VOPembelajaran pembelajaranPindah = pertemuan.ambilVOPembelajaran();
				if (pembelajaranPindah != null) {
					vbox.appendChild(new MyLabelKecil("Pindahkan ke pertemuan :"));
					final Combobox comboPertemuan = new Combobox();
					comboPertemuan.setReadonly(true);
					comboPertemuan.setWidth("95%");
					comboPertemuan.setStyle("font-size:9px;");
					vbox.appendChild(comboPertemuan);

					final Long pertemuanSaatIni = pertemuan.getId();
					try {
						// Ambil daftar pertemuan dari VOPembelajaran. Bila cache lokasi-pertemuan
						// KOSONG (mis. objek pembelajaran baru dimuat hanya untuk satu pertemuan,
						// sehingga udah()=true namun lokasiPertemuan belum terisi), paksa muat ulang
						// dari DB (refresh=true) agar dropdown TIDAK kosong. Saat daftar berisi, semua
						// pertemuan sudah dimasukkan ke cache oleh ambilPertemuan() sehingga ambilData
						// di bawah pasti menemukannya.
						java.util.TreeMap<String, Long> daftarPertemuan = pembelajaranPindah.ambilPertemuan();
						if (daftarPertemuan == null || daftarPertemuan.isEmpty()) {
							daftarPertemuan = pembelajaranPindah.ambilPertemuan(true);
						}
						boolean adaSaatIni = false;
						if (daftarPertemuan != null) {
							for (Long pid : daftarPertemuan.values()) {
								if (pid == null) {
									continue;
								}
								Pertemuan p = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pid.toString());
								if (p == null) {
									continue;
								}
								String topik = p.getTopik() == null ? "" : p.getTopik().trim();
								if (topik.length() > 40) {
									topik = topik.substring(0, 40) + "...";
								}
								String tgl = p.getTanggal() == null ? ""
										: (" - " + Common.dateFormat.get().format(p.getTanggal()));
								org.zkoss.zul.Comboitem item = new org.zkoss.zul.Comboitem("Pertemuan ke-"
										+ p.getPertemuanKe() + (topik.isEmpty() ? "" : " : " + topik) + tgl);
								item.setValue(pid);
								item.setParent(comboPertemuan);
								if (pertemuanSaatIni != null && pertemuanSaatIni.equals(pid)) {
									comboPertemuan.setSelectedItem(item);
									adaSaatIni = true;
								}
							}
						}
						// Pastikan pertemuan yang SEDANG dipilih SELALU ada di daftar dan terpilih,
						// walau tidak termuat (mis. terfilter batas jumlah pertemuan / non-aktif).
						if (!adaSaatIni && pertemuanSaatIni != null) {
							String topik = pertemuan.getTopik() == null ? "" : pertemuan.getTopik().trim();
							if (topik.length() > 40) {
								topik = topik.substring(0, 40) + "...";
							}
							String tgl = pertemuan.getTanggal() == null ? ""
									: (" - " + Common.dateFormat.get().format(pertemuan.getTanggal()));
							org.zkoss.zul.Comboitem itemSaatIni = new org.zkoss.zul.Comboitem("Pertemuan ke-"
									+ pertemuan.getPertemuanKe() + (topik.isEmpty() ? "" : " : " + topik) + tgl);
							itemSaatIni.setValue(pertemuanSaatIni);
							itemSaatIni.setParent(comboPertemuan);
							comboPertemuan.setSelectedItem(itemSaatIni);
						}
					} catch (Exception eDaftarPertemuan) {
						Common.tampilErrorJikaAdmin(eDaftarPertemuan);
					}

					comboPertemuan.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event evtChange) throws Exception {
							if (comboPertemuan.getSelectedItem() == null) {
								return;
							}
							final Long pidBaru = (Long) comboPertemuan.getSelectedItem().getValue();
							if (pidBaru == null || pidBaru.equals(pertemuanSaatIni)) {
								return;
							}
							final String labelTujuan = comboPertemuan.getSelectedItem().getLabel();
							MyMessageboxConfig.show(Common.pesan("Apakah Bapak/Ibu yakin ingin memindahkan ujian ini ke \"{V1}\"? Silakan pilih OK untuk melanjutkan atau Batal untuk membatalkan.", labelTujuan), "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {
										@Override
										public void onEvent(Event ev) throws Exception {
											if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
												return;
											}
											Session session = HibernateUtil.currentSession();
											if (pertemuanPunyaUjian.getId() != null) {
												session.refresh(pertemuanPunyaUjian);
											}
											Pertemuan pBaru = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
													pidBaru.toString());
											pertemuanPunyaUjian.setPertemuan(pBaru);
											Common.refreshUpdate(session, pertemuanPunyaUjian);

											if (eventListener != null) {
												eventListener.onEvent(ev);
											}
										}
									});
						}
					});
				}
			}

			if (!tampilInfo) {
				if (tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
						&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {

					final MyCheckboxConfig otomatisMunculKetikaBelumSelesai = new MyCheckboxConfig(
							"Apabila peserta belum selesai ujian dan tiba-tiba terputus koneksi / baterai ponselnya habis / browser-nya crash dan bermasalah dll, saat login ulang, secara otomatis tampilan ujian akan muncul dengan melanjutkan waktu terakhir berhenti.");
					otomatisMunculKetikaBelumSelesai.setParent(vbox);
					otomatisMunculKetikaBelumSelesai
							.setChecked(pertemuanPunyaUjian.getOtomatisMunculKetikaBelumSelesai());
					otomatisMunculKetikaBelumSelesai.setDisabled(mahasiswa != null);
					otomatisMunculKetikaBelumSelesai.setStyle("font-size:8px");
					otomatisMunculKetikaBelumSelesai.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							pertemuanPunyaUjian
									.setOtomatisMunculKetikaBelumSelesai(otomatisMunculKetikaBelumSelesai.isChecked());
							Common.refreshUpdate(session, pertemuanPunyaUjian);
						}
					});

					final MyCheckboxConfig tidakDiaktifkanTombolKembali = new MyCheckboxConfig(
							"Peserta tidak boleh melihat atau kembali ke soal sebelumnya. Misal : peserta sudah berada di soal nomor 5, tidak bisa kembali lagi ke soal nomor 3.");
					tidakDiaktifkanTombolKembali.setParent(vbox);
					tidakDiaktifkanTombolKembali.setChecked(pertemuanPunyaUjian.getTidakDiaktifkanTombolKembali());
					tidakDiaktifkanTombolKembali.setDisabled(mahasiswa != null);
					tidakDiaktifkanTombolKembali.setStyle("font-size:8px");
					tidakDiaktifkanTombolKembali.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							pertemuanPunyaUjian
									.setTidakDiaktifkanTombolKembali(tidakDiaktifkanTombolKembali.isChecked());
							Common.refreshUpdate(session, pertemuanPunyaUjian);
						}
					});

					final MyCheckboxConfig tidakDitampilkanJikaWaktuSudahTerlewat = new MyCheckboxConfig(
							"Ujian tidak ditampilkan apabila waktu belum mulai atau telah terlewat");
					tidakDitampilkanJikaWaktuSudahTerlewat.setParent(vbox);
					tidakDitampilkanJikaWaktuSudahTerlewat
							.setChecked(pertemuanPunyaUjian.getTidakDitampilkanJikaWaktuSudahTerlewat());
					tidakDitampilkanJikaWaktuSudahTerlewat.setDisabled(mahasiswa != null);
					tidakDitampilkanJikaWaktuSudahTerlewat.setStyle("font-size:8px");
					tidakDitampilkanJikaWaktuSudahTerlewat.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							pertemuanPunyaUjian.setTidakDitampilkanJikaWaktuSudahTerlewat(
									tidakDitampilkanJikaWaktuSudahTerlewat.isChecked());
							Common.refreshUpdate(session, pertemuanPunyaUjian);
						}
					});

				}
			} else {
				MyLabelAgakKecilBold myLabelAgakKecilBold = new MyLabelAgakKecilBold(pertemuan.info());
				myLabelAgakKecilBold.setParent(vbox);

				String dosens = "";
				for (Dosen dosen : pertemuan.ambilDosen()) {
					dosens += dosens.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				}

				if (!dosens.isEmpty()) {
					myLabelAgakKecilBold = new MyLabelAgakKecilBold(dosens);
					myLabelAgakKecilBold.setParent(vbox);
				}

				Vbox vbox2 = new Vbox();
				vbox2.setParent(vbox);

				new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
						|| pertemuanPunyaUjian.getMulaiUjian() == null
								? ""
								: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null)
										+ (Common.dateFormat3.get().format(pertemuanPunyaUjian.getMulaiUjian())
												.endsWith("00:00:00")
														? Common.dateFormat1.get().format(pertemuanPunyaUjian.getMulaiUjian())
														: Common.dateFormat3.get()
																.format(pertemuanPunyaUjian.getMulaiUjian()))))
						.setParent(vbox2);

				vbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));

				new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
						|| pertemuanPunyaUjian.getSampaiUjian() == null
								? ""
								: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null)
										+ (Common.dateFormat3.get().format(pertemuanPunyaUjian.getSampaiUjian())
												.endsWith("00:00:00")
														? Common.dateFormat1.get()
																.format(pertemuanPunyaUjian.getSampaiUjian())
														: Common.dateFormat3.get()
																.format(pertemuanPunyaUjian.getSampaiUjian()))))
						.setParent(vbox2);
			}

			if (biodataCalonMahasiswa != null || mahasiswa != null) {
				if (pertemuanPunyaUjian.getFakultas() != null) {
					new MyLabelAgakKecil(
							Common.getBahasaConfig("Fakultas") + " : " + pertemuanPunyaUjian.getFakultas().getNama())
							.setParent(vbox);
				}
				if (pertemuanPunyaUjian.getJurusan() != null) {
					new MyLabelAgakKecil(
							Common.getBahasaConfig("Jurusan") + " : " + pertemuanPunyaUjian.getJurusan().getNama())
							.setParent(vbox);
				}

				if (hasilUjianMahasiswa != null && !hasilUjianMahasiswa.getKeterangan().isEmpty()) {
					new MyLabelAgakKecil("Keterangan : " + hasilUjianMahasiswa.getKeterangan()).setParent(vbox);
				}

			} else if (pertemuan.getJadwalUjianPMB() != null) {

				final Combobox fak = new Combobox();
				final Combobox jur = new Combobox();

				Common.initFakultasDanJurusanDanSemua(fak, jur, null, null);

				fak.setParent(vbox);
				jur.setParent(vbox);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuanPunyaUjian.setFakultas(
								(Fakultas) (fak.getSelectedItem() == null ? null : fak.getSelectedItem().getValue()));
						pertemuanPunyaUjian.setJurusan(
								(Jurusan) (jur.getSelectedItem() == null ? null : jur.getSelectedItem().getValue()));
						Common.refreshUpdate(pertemuanPunyaUjian);
					}
				};

				fak.addEventListener("onChange", eventListener);
				jur.addEventListener("onChange", eventListener);

				Common.selectComboItem(fak, pertemuanPunyaUjian.getFakultas());
				Common.selectComboItem(jur, pertemuanPunyaUjian.getJurusan());

				if (ujian.getFakultas() != null) {
					fak.setDisabled(true);
				}
				if (ujian.getJurusan() != null) {
					jur.setDisabled(true);
				}
			}

			RevisiHelper.createNewRevisi(Ujian.class, ujian,
					Common.getBahasaConfig(ujian.getJenis()) + " / " + Common.getBahasaConfig(ujian.getJenisKoreksi())
							+ " / " + Common.getBahasaConfig(ujian.getLevel()) + " / "
							+ Common.numberFormat.get().format(ujian.getNilaiLulus()))
					.setParent(arg0);

			if (pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				new Label(hasilUjianMahasiswa == null ? ""
						: Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenar())).setParent(hbox);
				new Label(hasilUjianMahasiswa == null ? "Belum pernah ikut"
						: Common.numberFormat.get().format(hasilUjianMahasiswa.getJumlahIkut()) + " kali").setParent(hbox);

				hbox = new Hbox();
				hbox.setParent(arg0);
				new Label((hasilUjianMahasiswa == null || hasilUjianMahasiswa.getNilai() == null ? ""
						: Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai()))
						+ ((ujian.getJenis().equalsIgnoreCase(BankSoal.ESAY)
								|| ujian.getJenis().equalsIgnoreCase(BankSoal.JAWABAN_SINGKAT))
										? ""
										: " / " + (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getNilai() == null
												? ""
												: (hasilUjianMahasiswa.getLulus() ? Common.getBahasaConfig("Lulus")
														: Common.getBahasaConfig("Tidak Lulus")))))
						.setParent(hbox);
				new Label(hasilUjianMahasiswa == null ? ""
						: Common.numberFormat.get().format(pertemuanPunyaUjian.getJumlahBolehIkut()) + " kali")
						.setParent(hbox);
			} else {
				new Label(hasilUjianMahasiswa == null ? "Belum pernah ikut"
						: Common.numberFormat.get().format(hasilUjianMahasiswa.getJumlahIkut()) + " kali").setParent(arg0);
				new Label(hasilUjianMahasiswa == null ? ""
						: Common.numberFormat.get().format(pertemuanPunyaUjian.getJumlahBolehIkut()) + " kali")
						.setParent(arg0);
			}

			if (pertemuanPunyaUjian.getJmlDitampilkan() == null || pertemuanPunyaUjian.getJmlDitampilkan() <= 0) {
				session = HibernateUtil.currentSession();
				List<Long> d = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(pertemuanPunyaUjian, false);
				int jmlDitampilkan = d.size();
				d = null;
				if (jmlDitampilkan > 0) {
					pertemuanPunyaUjian.setJmlDitampilkan(jmlDitampilkan);
					Common.refreshUpdate(session, (pertemuanPunyaUjian));
				}
			}

			if (mahasiswa != null || biodataCalonMahasiswa != null
					|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
					|| tbmuser.getCalonSiswa() != null) {

				try {
					new Label((pertemuanPunyaUjian.getJmlDitampilkan() == null ? ""
							: Common.numberFormat.get().format(pertemuanPunyaUjian.getJmlDitampilkan()))
							+ (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getJumlahSoal() == null ? ""
									: " / " + Common.numberFormat.get().format(hasilUjianMahasiswa.getJumlahSoal())))
							.setParent(arg0);

					new Label(pertemuanPunyaUjian.getDibatasiWaktu() != null && pertemuanPunyaUjian.getDibatasiWaktu()
							? "Ya"
							: "Tidak").setParent(arg0);

					new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
							|| pertemuanPunyaUjian.getLama() == null ? ""
									: Common.timeFormat1.get().format(pertemuanPunyaUjian.getLama()))
							.setParent(arg0);

					Vbox vbox2 = new Vbox();
					vbox2.setParent(arg0);

					new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
							|| pertemuanPunyaUjian.getMulaiUjian() == null
									? ""
									: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null)
											+ (Common.dateFormat3.get().format(pertemuanPunyaUjian.getMulaiUjian())
													.endsWith("00:00:00")
															? Common.dateFormat1.get()
																	.format(pertemuanPunyaUjian.getMulaiUjian())
															: Common.dateFormat3.get()
																	.format(pertemuanPunyaUjian.getMulaiUjian()))))
							.setParent(vbox2);

					vbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));

					new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
							|| pertemuanPunyaUjian.getSampaiUjian() == null
									? ""
									: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null)
											+ (Common.dateFormat3.get().format(pertemuanPunyaUjian.getSampaiUjian())
													.endsWith("00:00:00")
															? Common.dateFormat1.get()
																	.format(pertemuanPunyaUjian.getSampaiUjian())
															: Common.dateFormat3.get()
																	.format(pertemuanPunyaUjian.getSampaiUjian()))))
							.setParent(vbox2);

					new Label(pertemuanPunyaUjian.getFormatNilai() == null
							|| pertemuanPunyaUjian.getFormatNilai().getStatusPertemuan() == null
									? ""
									: pertemuanPunyaUjian.getFormatNilai().getNama() + " ("
											+ Common.numberFormat.get().format(pertemuanPunyaUjian.getProsentase()) + "%)")
							.setParent(arg0);

					new Label(ujian.getAktif() ? "Ya" : "Tidak").setParent(arg0);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				PertemuanPunyaUjianHelper.tampilBolekIkutUjianAtauTidak(arg0, pertemuanPunyaUjian, mahasiswa,
						biodataCalonMahasiswa, hasilUjianMahasiswa, eventListener, null);

			} else {

				final MyLabelAgakKecil agakKecil = new MyLabelAgakKecil();
				agakKecil.setStyle("font-size:9px;color:red");
				vbox = new Vbox();
				vbox.setParent(arg0);
				final Intbox jml = new Intbox(pertemuanPunyaUjian.getJmlDitampilkan());
				vbox.appendChild(new Hbox(
						new Component[] { new MyLabelAgakKecil("Ditampilkan:"), jml, new MyLabelAgakKecil("soal") }));

				jml.setCols(1);
				jml.setWidth("48px"); // lebar pas utk 1-2 digit (setCols kurang dihormati di sel grid)
				jml.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						List<Long> bankSoals = pertemuanPunyaUjian.getUjian().ambilBankSoal(pertemuanPunyaUjian, false);
						int jumlah = bankSoals.size();
						bankSoals = null;

						System.out.println("jumlah soal => " + jumlah + ", input => " + jml.getValue());

						if (jumlah == 0) {
							MyMessageboxConfig.show(
				"Mohon maaf, soal ujian harus dimasukkan terlebih dahulu sebelum menentukan jumlah soal yang diujikan. Langkah yang dapat dilakukan: (1) klik tombol detail atau tanda plus di sebelah kiri untuk membuat soal; (2) setelah soal tersedia, tentukan kembali jumlah soal yang diujikan.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							jml.setValue(0);
							return;
						}

						if (jml.getValue() != null && jml.getValue() > jumlah) {
							MyMessageboxConfig.showFormat(
				"Mohon maaf, jumlah soal yang dapat diujikan maksimal adalah {V1} soal. Silakan sesuaikan jumlah soal yang diujikan agar tidak melebihi batas tersebut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, jumlah);
							jml.setValue((jumlah));
							pertemuanPunyaUjian.setJmlDitampilkan(jml.getValue());
							Common.refreshUpdate(session, (pertemuanPunyaUjian));

							return;
						}

						pertemuanPunyaUjian.setJmlDitampilkan(jml.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

						if (pertemuanPunyaUjian.getJmlDitampilkan() < 1) {
							agakKecil.setValue("Jml soal tidak boleh 0");
						} else {
							agakKecil.setValue("");
						}
					}
				});

				vbox.appendChild(agakKecil);
				if (pertemuanPunyaUjian.getJmlDitampilkan() < 1) {
					agakKecil.setValue("Jumlah soal tidak boleh 0");
					MyButtonConfig samakan;
					vbox.appendChild(samakan = new MyButtonConfig("Samakan dg jml soal tersedia"));
					samakan.setStyle("font-size:9px;");
					samakan.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							List<Long> bankSoals = pertemuanPunyaUjian.getUjian().ambilBankSoal(pertemuanPunyaUjian,
									false);
							int jumlah = bankSoals.size();
							bankSoals = null;
							if (jumlah == 0) {
								MyMessageboxConfig.show(
				"Mohon maaf, soal ujian harus dimasukkan terlebih dahulu sebelum menentukan jumlah soal yang diujikan. Langkah yang dapat dilakukan: (1) klik tombol detail atau tanda plus di sebelah kiri untuk membuat soal; (2) setelah soal tersedia, tentukan kembali jumlah soal yang diujikan.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								jml.setValue(0);
								return;
							}
							jml.setValue((jumlah));
							pertemuanPunyaUjian.setJmlDitampilkan(jml.getValue());
							Common.refreshUpdate(session, (pertemuanPunyaUjian));
							if (pertemuanPunyaUjian.getJmlDitampilkan() < 1) {
								agakKecil.setValue("Jml soal tidak boleh 0");
							} else {
								agakKecil.setValue("");
								arg0.getTarget().setVisible(false);
							}
						}
					});
				}

				// else if (count > 0) {
				// agakKecil.setValue("Jumlah soal yg ditampilkan "
				// +
				// Common.numberFormat.get().format(pertemuanPunyaUjian.getJmlDitampilkan())
				// + " dan tidak bisa diubah ketika peserta telah melakukan
				// ujian");
				// }

				else {
					agakKecil.setValue("");
				}

				final Intbox jumlahBolehIkut = new Intbox(pertemuanPunyaUjian.getJumlahBolehIkut());
				vbox.appendChild(new Hbox(new Component[] { new MyLabelAgakKecil("Boleh ikut ujian sebanyak :"),
						jumlahBolehIkut, new MyLabelAgakKecil("kali") }));
				jumlahBolehIkut.setCols(1);
				jumlahBolehIkut.setWidth("48px"); // lebar pas utk 1-2 digit
				jumlahBolehIkut.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();

						if (jumlahBolehIkut.getValue() < 1) {
							MyMessageboxConfig.show(
				"Mohon maaf, jumlah minimal keikutsertaan ujian adalah 1 kali. Silakan isikan nilai minimal 1 pada kolom jumlah boleh ikut ujian.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							jumlahBolehIkut.setValue((1));
							pertemuanPunyaUjian.setJumlahBolehIkut(jumlahBolehIkut.getValue());
							Common.refreshUpdate(session, (pertemuanPunyaUjian));
							return;
						}

						pertemuanPunyaUjian.setJumlahBolehIkut(jumlahBolehIkut.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				vbox = new Vbox();
				vbox.setParent(arg0);

				final MyCheckboxConfig dibatasiWaktu = new MyCheckboxConfig("Ujian ini dibatasi waktu");
				dibatasiWaktu.setStyle("font-size:9px;");
				dibatasiWaktu.setParent(vbox);
				dibatasiWaktu.setChecked(pertemuanPunyaUjian.getDibatasiWaktu());
				dibatasiWaktu.setDisabled(mahasiswa != null);

				dibatasiWaktu.setVisible(Common.bolehKonfigurasi("tampilkan_ujian_dibatasi_waktu"));
				if (!dibatasiWaktu.isVisible()) {
					pertemuanPunyaUjian.setDibatasiWaktu(true);
				}

				final MyCheckboxConfig lihatJawabanSetelahUjian = new MyCheckboxConfig(
						"Peserta bisa melihat jawaban setelah ujian");
				lihatJawabanSetelahUjian.setStyle("font-size:9px;");
				lihatJawabanSetelahUjian.setParent(vbox);
				lihatJawabanSetelahUjian.setChecked(pertemuanPunyaUjian.getLihatJawabanSetelahUjian());
				lihatJawabanSetelahUjian.setDisabled(mahasiswa != null);

				final MyCheckboxConfig lihatNilaiSetelahUjian = new MyCheckboxConfig(
						"Peserta bisa melihat nilai setelah ujian");
				lihatNilaiSetelahUjian.setStyle("font-size:9px;");
				lihatNilaiSetelahUjian.setParent(vbox);
				lihatNilaiSetelahUjian.setChecked(pertemuanPunyaUjian.getLihatNilaiSetelahUjian());
				lihatNilaiSetelahUjian.setDisabled(mahasiswa != null);

				final MyCheckboxConfig random = new MyCheckboxConfig("Random / Urutan nomor soal diacak");
				random.setStyle("font-size:9px;");
				random.setParent(vbox);
				random.setChecked(pertemuanPunyaUjian.getRandom());
				random.setDisabled(mahasiswa != null);

				// Feature 7: Larang tangkap layar — ditampilkan di sini (Pengaturan Data Ujian)
				// dan juga tetap ada di tab Anti Curang. Keduanya membaca/menulis field yang sama
				// sehingga selalu sinkron tanpa perlakuan khusus.
				final MyCheckboxConfig cbTangkapLayar = new MyCheckboxConfig(
						"Larang tangkap layar (screenshot) browser saat ujian berlangsung (default aktif)");
				cbTangkapLayar.setStyle("font-size:9px;");
				cbTangkapLayar.setParent(vbox);
				cbTangkapLayar.setChecked(Boolean.TRUE.equals(pertemuanPunyaUjian.getAntiCurangBlokirTangkapLayar()));
				cbTangkapLayar.setDisabled(mahasiswa != null);

				vbox = new Vbox();
				vbox.setParent(arg0);
				labelFieldUjian("Durasi pengerjaan (jam : menit : detik) — lama waktu peserta mengerjakan ujian:")
						.setParent(vbox);
				final MyTimebox lama = new MyTimebox(pertemuanPunyaUjian.getLama());
				lama.setFormat(Common.timeFormat1.get().toPattern());
				lama.setParent(vbox);
				// lama.setWidth("90%");
				lama.setDisabled(mahasiswa != null);
				lama.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setLama(lama.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				final MyCheckboxConfig tiapSoal = new MyCheckboxConfig(
						"Waktu berlaku untuk setiap soal, apabila opsi ini tidak dipilih, maka waktu berlaku untuk seluruh soal");
				tiapSoal.setStyle("font-size:9px");
				tiapSoal.setParent(vbox);
				tiapSoal.setChecked(pertemuanPunyaUjian.getTiapSoal());
				tiapSoal.setDisabled(mahasiswa != null);

				tiapSoal.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setTiapSoal(tiapSoal.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);
					}
				});

				Vbox vbox2 = new Vbox();
				vbox2.setParent(arg0);
				vbox2.setWidth("100%");

				labelFieldUjian("Waktu mulai ujian tersedia (kapan ujian mulai bisa dikerjakan peserta):")
						.setParent(vbox2);
				final MyDatebox mulaiUjian = new MyDatebox(pertemuanPunyaUjian.getMulaiUjian());
				mulaiUjian.setFormat(Common.dateFormat.get().toPattern());

				mulaiUjian.setParent(vbox2);
				mulaiUjian.setWidth("100%");
				mulaiUjian.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setMulaiUjian(mulaiUjian.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				vbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
				labelFieldUjian("Waktu berakhir ujian (batas akhir / tenggat pengerjaan):").setParent(vbox2);

				final MyDatebox sampaiUjian = new MyDatebox(pertemuanPunyaUjian.getSampaiUjian());
				sampaiUjian.setFormat(Common.dateFormat.get().toPattern());

				sampaiUjian.setParent(vbox2);
				sampaiUjian.setWidth("100%");
				sampaiUjian.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setSampaiUjian(sampaiUjian.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				EventListener dibatasiWaktuEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lama.setDisabled(!dibatasiWaktu.isChecked());
						lama.setValue(dibatasiWaktu.isChecked() ? pertemuanPunyaUjian.getLama() : null);

						mulaiUjian.setDisabled(!dibatasiWaktu.isChecked());
						mulaiUjian.setValue(dibatasiWaktu.isChecked() ? pertemuanPunyaUjian.getMulaiUjian() : null);

						sampaiUjian.setDisabled(!dibatasiWaktu.isChecked());
						sampaiUjian.setValue(dibatasiWaktu.isChecked() ? pertemuanPunyaUjian.getSampaiUjian() : null);

						if (mahasiswa != null) {
							sampaiUjian.setDisabled(true);
							mulaiUjian.setDisabled(true);
							lama.setDisabled(true);
						}
					}
				};
				dibatasiWaktu.addEventListener("onCheck", dibatasiWaktuEventListener);
				dibatasiWaktuEventListener.onEvent(null);

				dibatasiWaktu.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setDibatasiWaktu(dibatasiWaktu.isChecked());

						if (dibatasiWaktu.isChecked()) {
							pertemuanPunyaUjian.setSampaiUjian(sampaiUjian.getValue());
							pertemuanPunyaUjian.setMulaiUjian(mulaiUjian.getValue());
							pertemuanPunyaUjian.setLama(lama.getValue());
						}
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				lihatJawabanSetelahUjian.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setLihatJawabanSetelahUjian(lihatJawabanSetelahUjian.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);

					}
				});

				lihatNilaiSetelahUjian.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setLihatNilaiSetelahUjian(lihatNilaiSetelahUjian.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);

					}
				});

				random.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event a) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setRandom(random.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);
						if (arg0 != null) {
							Common.clear(arg0);
						}
						render(arg0, pertemuanPunyaUjian);
					}
				});

				cbTangkapLayar.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setAntiCurangBlokirTangkapLayar(cbTangkapLayar.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);
					}
				});

				vbox = new Vbox();
				vbox.setParent(arg0);

				if (pertemuan.getPerkuliahan() != null) {
					List<FormatNilai> formatNilais = Common.getFormatNilais(session, pertemuan.getPerkuliahan());
					if (pertemuan != null && pertemuan.getPerkuliahan() != null
							&& pertemuan.getPerkuliahan().getKurikulum() != null
							&& pertemuan.getPerkuliahan().getKurikulum().apakahObe(
									pertemuan.getPerkuliahan().getTahunAjaran(),
									pertemuan.getPerkuliahan().getGanjilGenap())) {

						if (!formatNilais.isEmpty()) {
							boolean sudahadasubCpmk = false;
							for (FormatNilai nilai : formatNilais) {
								if (nilai.getNama().toLowerCase().contains("cpmk")) {
									sudahadasubCpmk = true;
									break;
								}
							}

							if (!sudahadasubCpmk) {
								formatNilais = Common.getFormatNilais(pertemuan.getPerkuliahan(), true);
							}
						}

						final JSONObject jsonObject = new JSONObject(pertemuanPunyaUjian.getFormatNilais());

						Hbox hbox = new Hbox();
						hbox.setParent(vbox);

						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/svg/printer.svg");
						button.setTooltiptext("Cetak Data");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								cetak(pertemuanPunyaUjian);
							}
						});
						button.setParent(hbox);

						button = new MyToolbarbuttonConfig("Sinkronkan Nilai", "/img/Configure.gif");
						button.setParent(vbox);
						button.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilaiObe(pertemuan.getPerkuliahan(),
										pertemuanPunyaUjian.getFormatNilais());
							}
						});

						button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
						button.setTooltiptext("Refresh Data");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.getFormatNilais(pertemuan.getPerkuliahan(), true);

								if (arg0 != null) {

									Common.clear(arg0);

								}
								render(arg0, pertemuanPunyaUjian);
							}
						});
						button.setParent(hbox);

						// Kumpulkan sub-CPMK aktif (urutan tetap) untuk Format Cepat
						final java.util.List<FormatNilai> subCpmkAktif = new java.util.ArrayList<FormatNilai>();
						for (FormatNilai fn : formatNilais) {
							if (fn.getStatusPertemuan() != null) subCpmkAktif.add(fn);
						}

						if (!subCpmkAktif.isEmpty()) {
							// Bangun isi awal textarea dari assignment yang sudah ada
							StringBuilder sbFmt = new StringBuilder();
							for (int fi = 0; fi < subCpmkAktif.size(); fi++) {
								FormatNilai fn = subCpmkAktif.get(fi);
								if (!jsonObject.isNull(fn.getId().toString())) {
									Object cur = jsonObject.get(fn.getId().toString());
									if (cur != null && !cur.toString().trim().isEmpty()) {
										sbFmt.append(cur.toString().trim())
											.append(" sub cpmk ").append(fi + 1).append("\n");
									}
								}
							}

							new ais.ui.util.MyLabelKecil(
									"Format Cepat: [rentang nomor soal] sub cpmk [urutan] — mis. 1-10 sub cpmk 2")
									.setParent(vbox);
							final MyTextbox txBulk = new MyTextbox(sbFmt.toString().trim());
							txBulk.setWidth("100%");
							txBulk.setRows(4);
							txBulk.setTooltiptext(
									"Satu baris per Sub-CPMK. Rentang boleh daftar (1,2,3), rentang (1-10), atau gabungan (1-10,15,20-25).\n"
									+ "Urutan Sub-CPMK sesuai tabel di bawah (mulai dari 1).\n"
									+ "Contoh:\n1-10 sub cpmk 1\n11-20,25 sub cpmk 2");
							txBulk.setParent(vbox);

							MyToolbarbuttonConfig btnBulk = new MyToolbarbuttonConfig(
									"Terapkan Format Cepat", "/img/Button-Refresh-icon.png");
							btnBulk.setParent(vbox);
							btnBulk.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event ev) throws Exception {
									String teks = txBulk.getValue();
									if (teks == null || teks.trim().isEmpty()) return;
									Session sess = HibernateUtil.currentSession();
									if (pertemuanPunyaUjian.getId() != null) {
										sess.refresh(pertemuanPunyaUjian);
									}
									// Hapus assignment nomor soal lama untuk semua sub-CPMK aktif
									for (FormatNilai fn : subCpmkAktif) {
										jsonObject.remove(fn.getId().toString());
									}
									// Parse tiap baris
									for (String line : teks.split("\n")) {
										line = line.trim();
										if (line.isEmpty()) continue;
										String lineLower = line.toLowerCase();
										int idxSub = lineLower.indexOf("sub cpmk");
										if (idxSub < 0) continue;
										// Rentang = bagian sebelum "sub cpmk", hapus spasi ekstra dalam rentang
										String rentang = line.substring(0, idxSub).trim()
												.replaceAll("\\s+", "");
										if (rentang.isEmpty()) continue;
										String nStr = line.substring(idxSub + "sub cpmk".length()).trim();
										int n;
										try { n = Integer.parseInt(nStr); } catch (Exception ex) { continue; }
										if (n < 1 || n > subCpmkAktif.size()) continue;
										FormatNilai fn = subCpmkAktif.get(n - 1);
										// Gabungkan jika ada baris ganda untuk sub-CPMK yang sama
										String prev = jsonObject.isNull(fn.getId().toString()) ? ""
												: jsonObject.get(fn.getId().toString()) + "";
										String merged = prev.isEmpty() ? rentang : prev + "," + rentang;
										jsonObject.put(fn.getId().toString(), merged);
										if (jsonObject.isNull(fn.getId().toString() + "_bobot")) {
											jsonObject.put(fn.getId().toString() + "_bobot", 100.0);
										}
									}
									pertemuanPunyaUjian.setFormatNilais(jsonObject.toString());
									Common.refreshUpdate(sess, pertemuanPunyaUjian);
									// Refresh dari DB agar objek ppu punya nilai terbaru sebelum re-render
									try { sess.refresh(pertemuanPunyaUjian); } catch (Exception er) { /* abaikan */ }
									Common.clear(arg0);
									render(arg0, pertemuanPunyaUjian);
								}
							});
						}

						final MyGrid gridPilih = new MyGrid();
						gridPilih.setParent(vbox);
						gridPilih.setWidth("100%");
						gridPilih.setSclass("ppu-subcpmk-grid ais-data-grid");

						Columns columns = new Columns();
						columns.setParent(gridPilih);

						MyColumnConfig column = new MyColumnConfig("Sub-CPMK");
						column.setParent(columns);
						column.setWidth("40%");

						column = new MyColumnConfig("Nomor Soal");
						column.setParent(columns);
						column.setWidth("40%");

						column = new MyColumnConfig("Bobot");
						column.setParent(columns);
						column.setWidth("20%");

						Rows rowsPilih = new Rows();

						rowsPilih.setParent(gridPilih);

						for (FormatNilai nilai : formatNilais) {
							if (nilai.getStatusPertemuan() != null) {
								final Checkbox radio = new Checkbox(
										nilai.getNama() + " (" + Common.numberFormat.get().format(nilai.getPersen()) + "%)");
								radio.setAttribute("value", nilai);
								radio.setWidth("95%");

								MyFormRow rowPilih = new MyFormRow();
								rowPilih.setValign("top");
								rowPilih.setParent(rowsPilih);
								rowPilih.appendChild(radio);

								final MyTextbox doubleboxBobot = new MyTextbox(
										jsonObject.isNull(nilai.getId().toString()) ? ""
												: (jsonObject.get(nilai.getId().toString()) + ""));
								doubleboxBobot.setWidth("90%");
								doubleboxBobot.setRows(2);
								// Petunjuk: boleh daftar koma ATAU rentang dengan tanda minus, mis.
								// "1,2,3" atau "1-10" atau gabungan "1-10,15,20-25".
								doubleboxBobot.setTooltiptext(
										"Nomor soal untuk Sub-CPMK ini. Boleh daftar (1,2,3), rentang (1-10), atau gabungan (1-10,15,20-25).");
								rowPilih.appendChild(doubleboxBobot);

								final boolean subCpmkDipilih = !jsonObject.isNull(nilai.getId().toString());
								radio.setChecked(subCpmkDipilih);
								final MyDoublebox bobotN = new MyDoublebox(subCpmkDipilih
										? (jsonObject.isNull(nilai.getId().toString() + "_bobot") ? 100.0
												: jsonObject.getDouble(nilai.getId().toString() + "_bobot"))
										: null);
								bobotN.setWidth("90%");
								// Wrap bobot in a Div so inline summary appears below (Feature 2 inline,
								// replaces the removed "Pengaturan OBE" tab).
								org.zkoss.zul.Div bobotCell = new org.zkoss.zul.Div();
								rowPilih.appendChild(bobotCell);
								bobotN.setParent(bobotCell);
								// Inline summary: "Lainnya: Y% · Total: Z%"
								if (subCpmkDipilih) {
									try {
									double currentBobot = jsonObject.isNull(nilai.getId().toString() + "_bobot") ? 100.0
											: jsonObject.optDouble(nilai.getId().toString() + "_bobot", 100.0);
									String infoBobot = buildInfoBobotInline(pertemuanPunyaUjian, nilai,
											currentBobot, session, pertemuan.getPerkuliahan());
									if (infoBobot != null) {
										new ais.ui.util.MyHtml(infoBobot).setParent(bobotCell);
									}
									} catch (Exception eBobotInfo) {
										ais.common.ErrorAuditUtil.record(eBobotInfo,
											"auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:buildInfoBobotInline");
									}
								}

								doubleboxBobot.setDisabled(!radio.isChecked());
								bobotN.setDisabled(!radio.isChecked());
								radio.setDisabled(pertemuan.getPerkuliahan().getDikunci() != null);

								EventListener eventListenerD = new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Session session = HibernateUtil.currentSession();
										if (pertemuanPunyaUjian.getId() != null) {
											session.refresh(pertemuanPunyaUjian);
										}
										FormatNilai fn = (FormatNilai) radio.getAttribute("value");
										if (radio.isChecked()) {
											if (bobotN.getValue() == null) bobotN.setValue(Double.valueOf(100.0));
											jsonObject.put(fn.getId().toString(), doubleboxBobot.getValue());
											jsonObject.put(fn.getId().toString() + "_bobot", bobotN.getValue());
										} else {
											jsonObject.remove(fn.getId().toString());
											jsonObject.remove(fn.getId().toString() + "_bobot");
										}

										pertemuanPunyaUjian.setFormatNilais(jsonObject.toString());
										Common.refreshUpdate(session, (pertemuanPunyaUjian));
										doubleboxBobot.setDisabled(!radio.isChecked());
										bobotN.setDisabled(!radio.isChecked());
									}

								};

								radio.addEventListener("onClick", eventListenerD);
								doubleboxBobot.addEventListener("onChange", eventListenerD);
								bobotN.addEventListener("onChange", eventListenerD);
							}
						}

						// Sembunyikan grid Sub-CPMK bila kosong (tidak ada Sub-CPMK) agar tidak
						// tampil sebagai kotak kosong di modal Pengaturan Data Ujian.
						if (rowsPilih.getChildren().isEmpty()) {
							gridPilih.setVisible(false);
						}

					} else {

						Hbox hboxP = new Hbox();
						final Combobox formatNilai = new Combobox();

						formatNilai.setWidth("92px");
						MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
						comboitemTidakAda.setValue(null);
						formatNilai.appendChild(comboitemTidakAda);
						for (FormatNilai nilai : formatNilais) {
							if (nilai.getStatusPertemuan() != null) {
								org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
								comboitem.setValue(nilai);
								comboitem.setLabel(
										nilai.getNama() + " (" + Common.numberFormat.get().format(nilai.getPersen()) + "%)");
								formatNilai.appendChild(comboitem);
							}
						}
						formatNilai.setParent(hboxP);
						if (pertemuanPunyaUjian.getFormatNilai() == null) {
							formatNilai.setSelectedItem(comboitemTidakAda);
						} else {
							Common.selectComboItem(formatNilai, pertemuanPunyaUjian.getFormatNilai());
						}
						formatNilai.setReadonly(true);
						formatNilai.setDisabled(pertemuan.getPerkuliahan().getDikunci() != null);
						if (pertemuan.getPerkuliahan().getDikunci() != null) {
							new MyLabelKecil("Penilaian sudah dikunci").setParent(vbox);
							if (pertemuanPunyaUjian.getFormatNilai() != null) {
								new MyLabelKecil(
										"Nilai otomatis masuk ke " + pertemuanPunyaUjian.getFormatNilai().getNama())
										.setParent(vbox);
							}
						}

						labelFieldUjian("Nilai masuk ke komponen penilaian (pilih komponen tujuan nilai ujian ini) "
								+ "dan bobotnya (%):").setParent(vbox);
						hboxP.setParent(vbox);
						final MyDoublebox prosentase = new MyDoublebox(pertemuanPunyaUjian.getProsentase());
						prosentase.setDisabled(pertemuan.getPerkuliahan().getDikunci() != null);
						prosentase.setCols(2);
						final Label labelbobot;
						hboxP.appendChild(labelbobot = new Label(ais.common.Common.getBahasaConfig(" bobot ")));
						prosentase.setParent(hboxP);
						hboxP.appendChild(new Label(" "));

						prosentase.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pertemuanPunyaUjian.setProsentase(prosentase.getValue());
								Common.refreshUpdate(pertemuanPunyaUjian);
							}
						});

						final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Sinkronkan Nilai",
								"/img/Configure.gif");
						button.setParent(vbox);
						button.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(pertemuan.getPerkuliahan(),
										pertemuanPunyaUjian.getFormatNilai());
							}
						});

						prosentase.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
						button.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
						labelbobot.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);

						formatNilai.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								FormatNilai fn = (FormatNilai) (formatNilai.getSelectedItem() == null ? null
										: formatNilai.getSelectedItem().getValue());

								Session session = HibernateUtil.currentSession();
								pertemuanPunyaUjian.setFormatNilai(fn);
								try {
									Common.refreshUpdate(session, (pertemuanPunyaUjian));
								} catch (Exception eSimpan) {
									// FIX akar masalah ConstraintViolationException (pola sama dgn
									// TugasMandiriHelper): format nilai yang dipilih bisa saja sudah
									// dihapus admin lain sesaat sebelum combobox ini disimpan (race
									// condition lintas sesi) -- sebelumnya meledak mentah tanpa pesan
									// yang bisa dipahami user. Tangkap, rollback, catat, beri tahu user.
									try {
										if (session.getTransaction() != null && session.getTransaction().isActive()) {
											session.getTransaction().rollback();
										}
									} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback,
											"auto-audit(rollback-gagal) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java onFormatNilaiChange"); }
									ais.common.ErrorAuditUtil.record(eSimpan,
											"PertemuanPunyaUjianHelper: gagal simpan format nilai untuk PertemuanPunyaUjian id="
													+ (pertemuanPunyaUjian == null ? "null" : pertemuanPunyaUjian.getId()));
									MyMessageboxConfig.show(
											"Mohon maaf, gagal menyimpan format nilai karena ada data terkait yang tidak konsisten. "
													+ "Silakan muat ulang (refresh) halaman ini dan coba lagi. Jika masih gagal, hubungi Administrator.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									return;
								}
								prosentase.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
								button.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
								labelbobot.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
							}

						});
					}
				} else {
					Hbox hboxP = new Hbox();
					hboxP.setParent(vbox);
					final MyDoublebox prosentase = new MyDoublebox(pertemuanPunyaUjian.getProsentase());
					prosentase.setCols(2);
					hboxP.appendChild(new Label(ais.common.Common.getBahasaConfig("Bobot ")));
					prosentase.setParent(hboxP);
					hboxP.appendChild(new Label(" "));

					prosentase.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pertemuanPunyaUjian.setProsentase(prosentase.getValue());
							Common.refreshUpdate(pertemuanPunyaUjian);
						}
					});
				}

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
				checkbox.setChecked(ujian.getAktif());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ujian.setAktif(checkbox.isChecked());
						Common.refreshSaveOrUpdate(ujian);
					}
				});

				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
						new java.util.ArrayList<org.zkoss.zk.ui.Component>();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Sertifikat", "/img/certificate-icon.png");
				button.setOrient("vertical");

				button.setVisible(hasilUjianMahasiswa != null && ujian != null && hasilUjianMahasiswa.getLulus()
						&& ujian.getSertifikat() != null);
				button.setTooltiptext("Sertifikat");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						SertifikatAction.cetakSertifikat(hasilUjianMahasiswa);
					}
				});
				aksiButtons.add(button);

				if (pertemuanPunyaUjian != null) {
					button = new MyToolbarbuttonConfig("Hasil", "/img/album.png");
					button.setOrient("vertical");
					button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
							&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							HasilUjianMahasiswaHelper hasilUjianMahasiswaHelper = new HasilUjianMahasiswaHelper(
									pertemuan);
							Window window = new Window("Hasil Ujian " + ujian.getNama() + " - " + pertemuan.toString(),
									"none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("98%");
							window.setWidth("95%");
							hasilUjianMahasiswaHelper.display(pertemuanPunyaUjian, window);
							window.onModal();
						}
					});
					aksiButtons.add(button);
				}

				button = new MyToolbarbuttonConfig("Preview", "/img/eye-icon.png");
				button.setOrient("vertical");
				button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
						&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
				button.setTooltiptext("Preview");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						ProsesUjianHelper.ikut(mahasiswa, biodataCalonMahasiswa, tbmuser.getSiswa(),
								tbmuser.getCalonSiswa(), pertemuanPunyaUjian, hasilUjianMahasiswa, true, eventListener);
					}
				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
				button.setOrient("vertical");
				button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
						&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						UjianAction.onAddExternal(event, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(eventListener, "Loading..", false, 1500);

							}
						}, ujian, pertemuan.untuk());
					}
				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setOrient("vertical");
				button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
						&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
				// button.setDisabled(count > 0);
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diperhatikan bahwa data yang telah dihapus tidak dapat dikembalikan. Silakan pilih OK untuk melanjutkan penghapusan atau Batal untuk membatalkan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();

												session.createSQLQuery("delete from hasil_ujian_mahasiswa_detail where hasil_ujian_mahasiswa in (select id from hasil_ujian_mahasiswa where pertemuan_punya_ujian = "
														+ pertemuanPunyaUjian.getId() + ")").executeUpdate();

												String sql = "delete from hasil_ujian_mahasiswa where pertemuan_punya_ujian = "
														+ pertemuanPunyaUjian.getId();

												session.createSQLQuery(sql).executeUpdate();

												Common.refreshDelete(session, pertemuanPunyaUjian);

												Common.createDefaultTimer(eventListener, "Loading..", false, 1500);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
				"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain di dalam sistem. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang terkait; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala masih berlanjut, hubungi Admin untuk bantuan lebih lanjut.",
				e.getMessage()));
											}

										}

									}
								});

					}

				});
				aksiButtons.add(button);

				ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
			}

			if (hasilUjianMahasiswa != null
					&& (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null
							|| tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)) {

				int kuota = 120;
				try {
					kuota = parseIntegerDefault(Common.getKonfigurasi("kuota_ujian", kuota + "").getNilai(), kuota);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:2643");
					// TODO: handle exception
				}

				if (kuota <= ProsesUjianHelper.kuotaUjian.size()
						&& !ProsesUjianHelper.kuotaUjian.contains(hasilUjianMahasiswa.getKeyhasil())) {
					Common.freeze(arg0, true);

					if (arg0 != null) {

						Common.clear(arg0);

					}
					ais.ui.util.ZkCompat.setSpans(arg0, "10");
					Label lbl = new Label(
							"Maaf, kuota ujian masih penuh, jangan ditutup dan tunggu beberapa waktu untuk ikut kembali ujian. Klik tombol \"Lihat Peserta Ujian\" untuk mengetahui peserta yang saat ini sedang ujian.");
					arg0.appendChild(lbl);
					lbl.setStyle("font-size:15px;color:red;");

					Common.createDefaultTimer(eventListener, "", false, 5000);

					return;
				}

			}

			if (mahasiswa != null || biodataCalonMahasiswa != null
					|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
					|| tbmuser.getCalonSiswa() != null) {
				Long id = mahasiswa != null ? mahasiswa.getId()
						: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
								: tbmuser.getSiswa() != null ? tbmuser.getSiswa().getId()
										: tbmuser.getCalonSiswa() != null ? tbmuser.getCalonSiswa().getId() : null;

				if (id != null && (ujian != null && !ujian.getAktif())
						|| pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + id + ",")) {
					Common.freeze(arg0, true);

					if (arg0 != null) {

						Common.clear(arg0);

					}
					ais.ui.util.ZkCompat.setSpans(arg0, "10");
					Label lbl = new Label("Anda tidak diizinkan ikut ujian \"" + ujian.getNama() + "\"");
					arg0.appendChild(lbl);
					lbl.setStyle("font-size:15px;color:red;");

				}
			}
		}
	}

	/**
	 * Mencetak laporan PDF "TemplateObe" (rincian capaian Sub-CPMK/CPMK &amp; soal) untuk satu
	 * {@link PertemuanPunyaUjian}, dipanggil dari tombol "Cetak" di
	 * {@link DetailPertemuanRenderer#render(Row, Object)}. Menolak (menampilkan info, tanpa
	 * exception) bila {@link Perkuliahan#ambilKurikulumPunyaMatakuliah()} tidak ditemukan, karena
	 * template memerlukan data kurikulum-matakuliah yang valid. Parameter laporan disusun oleh
	 * {@link #parameter(PertemuanPunyaUjian, KurikulumPunyaMatakuliah)} lalu dicetak lewat
	 * {@code Report.generatePDFReport}, memakai {@code pertemuanPunyaUjian.getTanggal_dirubah()}
	 * sebagai kunci cache/versi laporan.
	 *
	 * @param pertemuanPunyaUjian ujian pada pertemuan yang akan dicetak laporannya.
	 */
	public static void cetak(PertemuanPunyaUjian pertemuanPunyaUjian) throws Exception {
		Perkuliahan perkuliahan = pertemuanPunyaUjian.getPertemuan().getPerkuliahan();
		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan.ambilKurikulumPunyaMatakuliah();

		if (kurikulumPunyaMatakuliah == null) {
			MyMessageboxConfig.show(
				"Mohon maaf, data kurikulum tidak sesuai. Langkah yang dapat dilakukan: (1) hubungi bagian Akademik untuk memeriksa dan memperbaiki data kurikulum; (2) setelah data diperbaiki, ulangi kembali tindakan ini.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		Report.generatePDFReport(Report.PDF, parameter(pertemuanPunyaUjian, kurikulumPunyaMatakuliah), "TemplateObe",
				pertemuanPunyaUjian.getTanggal_dirubah());
	}

	/**
	 * Menyusun {@link Map} parameter untuk template laporan PDF "TemplateObe", dipakai oleh
	 * {@link #cetak(PertemuanPunyaUjian)}. Mengumpulkan (murni baca, tanpa mutasi DB):
	 * <ul>
	 * <li>Daftar {@code maps}: satu entri per Sub-CPMK yang di-assign ke ujian ini lewat
	 * {@code pertemuanPunyaUjian.getFormatNilais()} (JSON), berisi nomor soal yang diujikan,
	 * kode/nama Sub-CPMK &amp; CPMK induknya (dicari dari {@code formula} JSON milik
	 * {@link CapaianLulusan}), bobot per-ujian (kunci {@code "<fnId>_bobot"}, default 100 — bukan
	 * {@code FormatNilai.getPersen()}, agar konsisten dengan editor bobot di modal pengaturan),
	 * serta kode/nama Capaian Pembelajaran Lulusan dan Profil Lulusan terkait.</li>
	 * <li>Daftar {@code mapsSoals}: seluruh {@link UjianPunyaSoal} ujian ini (teks soal di-strip
	 * HTML lewat Jsoup, plus bobot skor dari {@link BankSoal}).</li>
	 * <li>Data identitas perkuliahan (kelas, program, jurusan, semester, sks, tahun ajaran,
	 * matakuliah), dosen pengampu (tunggal atau gabungan bila lebih dari satu), serta path berkas
	 * tanda tangan (petugas 1-4, penanggung jawab dosen, pudek2/pudek3, kaprodi) yang dicari lewat
	 * {@link LampiranLain} bila formatnya berupa gambar (jpg/png/jpeg/gif/tif/bmp).</li>
	 * </ul>
	 * Properti entity {@link PertemuanPunyaUjian} dan {@link Perkuliahan} turut disalin otomatis
	 * lewat {@code Common.insertProperty(...)} agar field lain di template tetap terisi tanpa
	 * perlu didaftarkan satu per satu di sini.
	 *
	 * @param pertemuanPunyaUjian       ujian pada pertemuan yang akan dicetak.
	 * @param kurikulumPunyaMatakuliah  kurikulum-matakuliah terkait (sudah divalidasi tidak
	 *                                  {@code null} oleh {@link #cetak(PertemuanPunyaUjian)}).
	 * @return {@link Map} parameter siap diteruskan ke {@code Report.generatePDFReport}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(PertemuanPunyaUjian pertemuanPunyaUjian,
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) throws Exception {
		Perkuliahan perkuliahan = pertemuanPunyaUjian.getPertemuan().getPerkuliahan();
		Set<Long> longsProfile = new HashSet<Long>();
		for (String d : perkuliahan.getMatakuliah().getProfilLulusan().split(",")) {
			if (!d.trim().isEmpty()) {
				try {
					longsProfile.add(Long.parseLong(d.trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:2720");
					// TODO: handle exception
				}
			}
		}
		Session session = HibernateUtil.currentSession();

		Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
		List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
		Map parameters = ais.common.HashMapGenerator.getRand();

		List<Map> maps = new ArrayList<Map>();
		JSONObject jsonObjectFormat = new JSONObject(pertemuanPunyaUjian.getFormatNilais());
		for (FormatNilai nilai : formatNilais) {
			if (nilai.getStatusPertemuan() != null && nilai.getCapaianPembelajaranLulusan() != null
					&& nilai.getCapaianPembelajaranLulusan().getId() != null && nilai.getKodeSubCpmk() != null
					&& !jsonObjectFormat.isNull(nilai.getId().toString())) {

				try {
					String nomor = jsonObjectFormat.get(nilai.getId().toString()) + "";

					JSONObject subCpmkData = null;

					JSONArray array = new JSONArray(nilai.getCapaianPembelajaranLulusan().getFormula());
					for (int i = 0; i < array.length(); i++) {
						JSONObject subCpmk = array.getJSONObject(i);

						if (subCpmk.isNull("key")) {
							continue;
						}

						if (subCpmk != null && !subCpmk.isNull("key") && subCpmk.get("key").toString().trim()
								.equalsIgnoreCase(nilai.getKodeSubCpmk().trim())) {
							subCpmkData = subCpmk;
							break;
						}
					}

					System.out.println("subCpmkData -> " + subCpmkData + ", nomor -> " + nomor + ", nilai -> " + nilai);

					if (subCpmkData != null) {
						Map mapData = new HashMap();
						mapData.put("nomor", nomor);
						mapData.put("kode_sub_cpmk", subCpmkData.isNull("kode") ? "" : subCpmkData.getString("kode"));
						mapData.put("nama_sub_cpmk", subCpmkData.isNull("nama") ? "" : subCpmkData.getString("nama"));

						mapData.put("kode_cpmk", nilai.getCapaianPembelajaranLulusan().getKode());
						mapData.put("nama_cpmk", nilai.getCapaianPembelajaranLulusan().getNama());
						// Bobot = bobot PER-UJIAN yang diisi dosen di "Pengaturan Data Ujian" (kunci
						// "<fnId>_bobot" di formatNilais, default 100) — BUKAN FormatNilai.getPersen() (persen
						// Sub-CPMK di RPS yang di sini 0), agar konsisten dengan editor & tak tampil 0.
						mapData.put("bobot",
								jsonObjectFormat.isNull(nilai.getId().toString() + "_bobot") ? Double.valueOf(100.0)
										: Double.valueOf(jsonObjectFormat.getDouble(nilai.getId().toString() + "_bobot")));

						List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(session
								.createCriteria(CapaianLulusan.class)
								.add(Restrictions.ilike("capaianPembelajaranLulusan",
										"," + nilai.getCapaianPembelajaranLulusan().getId() + ",", MatchMode.ANYWHERE)),
								CapaianLulusan.class);

						Set<Long> profiles = new HashSet<Long>();
						String kodeCapaian = "";
						for (CapaianLulusan c : capaianLulusans) {
							kodeCapaian += kodeCapaian.isEmpty() ? c.getKode() : "," + c.getKode();
							for (String d : c.getProfil().split(",")) {
								try {
									if (!d.trim().isEmpty()) {
										Long idP = Long.parseLong(d);
										profiles.add(idP);
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:2791");
									// TODO: handle exception
								}
							}

						}

						String namaCapaian = "";
						for (CapaianLulusan c : capaianLulusans) {
							namaCapaian += namaCapaian.isEmpty() ? c.getNama() : ",\n" + c.getNama();
						}

						List<ProfilLulusan> profilLulusans = ConstantValues
								.simpleList(session.createCriteria(ProfilLulusan.class)
										.add(longsProfile.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("id", longsProfile))
										.add(profiles.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("id", profiles)),
										ProfilLulusan.class);

						String kodeProfilLulusan = "";
						String namaProfilLulusan = "";

						for (ProfilLulusan c : profilLulusans) {
							kodeProfilLulusan += kodeProfilLulusan.isEmpty() ? c.getKode() : ",\n" + c.getKode();
							namaProfilLulusan += namaProfilLulusan.isEmpty() ? c.getNama() : ",\n" + c.getNama();
						}

						mapData.put("kodeCapaian", kodeCapaian);
						mapData.put("namaCapaian", namaCapaian);
						mapData.put("kodeProfilLulusan", kodeProfilLulusan);
						mapData.put("namaProfilLulusan", namaProfilLulusan);

						maps.add(mapData);
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:2828");
				}

			}
		}

		Object[] objects = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(false, pertemuanPunyaUjian, "", 0, 1000);
		List<Long> ujianPunyaSoals = (List<Long>) objects[0];
		Integer size = (Integer) objects[1];

		parameters.put("jumlah_size", size);
		List<Map> mapsSoals = new ArrayList<Map>();
		int nomor = 1;
		for (Long soalId : ujianPunyaSoals) {
			UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
					soalId.toString());
			if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null) {
				Map mapData = new HashMap();
				mapData.put("id", soalId);
				mapData.put("nomor", nomor);

				String soal = ujianPunyaSoal.getBankSoal().getSoal();
				try {
					soal = Jsoup.parse(soal).text();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:2853");
				}

				mapData.put("soal", soal);
				mapData.put("bobot", ujianPunyaSoal.getBankSoal().getSkor());
				mapsSoals.add(mapData);
				nomor++;
			}
		}
		parameters.put("mapsSoals", mapsSoals);
		parameters.put("id", pertemuanPunyaUjian.getId());

		Common.insertProperty(PertemuanPunyaUjian.class, pertemuanPunyaUjian, parameters, "");

		if (perkuliahan != null) {
			Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");
		}

		parameters.put("perkuliahan", perkuliahan.getId());
		parameters.put("kelas", perkuliahan.getKelas());

		parameters.put("program", perkuliahan.getProgram());
		parameters.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
		parameters.put("semester", perkuliahan.getSemester());
		parameters.put("sks", perkuliahan.getMatakuliah().getSks());

		parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
		parameters.put("tampil_nilai", 1);
		parameters.put("fakultas",
				perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getFakultas().getNama());
		parameters.put("jenis_semester",
				((Integer) perkuliahan.getSemester()) % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
		parameters.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
		parameters.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());

		List<Dosen> dataDosens = perkuliahan.populateDosenBuNama();
		if (dataDosens.size() > 1) {
			String dosenPengampu = "";
			for (Dosen dosen : dataDosens) {
				dosenPengampu += dosenPengampu.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
			}
			parameters.put("dosen", dosenPengampu);
			dosenPengampu = "";
			for (Dosen dosen : dataDosens) {
				dosenPengampu += dosenPengampu.isEmpty() ? dosen.getNama() : "; " + dosen.getNama();
			}
			parameters.put("dosen_spl", dosenPengampu);
		} else {
			parameters.put("dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
			parameters.put("dosen_spl", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
		}

		int index = 1;
		for (Dosen dosen : dataDosens) {
			parameters.put("dosen_id" + index, dosen.getId());
			try {
				FileFotoLain lampiranLain = FileFotoLain.ambil(false, dosen.getId(), LampiranLain.TTD_DOSEN,
						LampiranLain.class);
				if (lampiranLain != null) {
					File file = lampiranLain.ambilFile();
					if (file.exists()) {
						parameters.put("ttd_dosen_" + index, file.getAbsolutePath());
					}
					file = null;
					lampiranLain = null;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:2921");
			}
			index++;
		}
		parameters.put("nidn_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());
		parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
		parameters.put("jurusan", perkuliahan.getKurikulum() == null ? "" : perkuliahan.getJurusan().getNama());

		if (perkuliahan.getKurikulum() != null && perkuliahan.getJurusan() != null
				&& perkuliahan.getJurusan().getGrupJurusan() != null
				&& perkuliahan.getJurusan().getGrupJurusan().getKajur() != null) {
			parameters.put("nama_kajur", perkuliahan.getJurusan() == null ? ""
					: perkuliahan.getJurusan().getGrupJurusan().getKajur().getNama());
			parameters.put("nip_kajur", perkuliahan.getJurusan() == null ? ""
					: perkuliahan.getJurusan().getGrupJurusan().getKajur().getCode());
		}

		Pegawai petugas = null;
		Pegawai petugas2 = null;
		Pegawai petugas3 = null;
		Pegawai petugas4 = null;

		Dosen pj = null;

		petugas = (Pegawai) (pertemuan == null || pertemuan.getPetugas() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas()));

		petugas2 = (Pegawai) (pertemuan == null || pertemuan.getPetugas2() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas2()));

		petugas3 = (Pegawai) (pertemuan == null || pertemuan.getPetugas3() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas3()));

		petugas4 = (Pegawai) (pertemuan == null || pertemuan.getPetugas4() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas4()));

		pj = (Dosen) (pertemuan == null || pertemuan.getPjDosen() == null ? null
				: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getPjDosen()));

		if (petugas != null) {
			LampiranLain lam = LampiranLain.ambil(petugas.getId(), LampiranLain.TTD_PEGAWAI);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:2972");
					}

					parameters.put("ttd_petugas", ttd);
				}
			} else if (petugas.getDosen() != null) {
				lam = LampiranLain.ambil(petugas.getDosen().getId(), LampiranLain.TTD_DOSEN);
				nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:2989");
						}

						parameters.put("ttd_petugas", ttd);
					}
				}
			}
		}
		if (petugas2 != null) {
			LampiranLain lam = LampiranLain.ambil(petugas2.getId(), LampiranLain.TTD_PEGAWAI);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3009");
					}

					parameters.put("ttd_petugas2", ttd);
				}
			} else if (petugas2.getDosen() != null) {
				lam = LampiranLain.ambil(petugas2.getDosen().getId(), LampiranLain.TTD_DOSEN);
				nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3026");
						}

						parameters.put("ttd_petugas2", ttd);
					}
				}
			}
		}
		if (petugas3 != null) {
			LampiranLain lam = LampiranLain.ambil(petugas3.getId(), LampiranLain.TTD_PEGAWAI);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3046");
					}

					parameters.put("ttd_petugas3", ttd);
				}
			} else if (petugas3.getDosen() != null) {
				lam = LampiranLain.ambil(petugas3.getDosen().getId(), LampiranLain.TTD_DOSEN);
				nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3063");
						}

						parameters.put("ttd_petugas3", ttd);
					}
				}
			}
		}

		if (petugas4 != null) {
			LampiranLain lam = LampiranLain.ambil(petugas4.getId(), LampiranLain.TTD_PEGAWAI);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3084");
					}

					parameters.put("ttd_petugas4", ttd);
				}
			} else if (petugas4.getDosen() != null) {
				lam = LampiranLain.ambil(petugas4.getDosen().getId(), LampiranLain.TTD_DOSEN);
				nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3101");
						}

						parameters.put("ttd_petugas4", ttd);
					}
				}
			}
		}

		Dosen pjawabDosen = (Dosen) (pertemuan == null || pertemuan.getPjDosen() == null ? null
				: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getPjDosen()));

		parameters.put("pjdosen", pjawabDosen == null ? "" : pjawabDosen.getNama());
		parameters.put("pjdosen_nip", pjawabDosen == null ? "" : pjawabDosen.getMycode());

		if (pjawabDosen != null) {
			LampiranLain lam = LampiranLain.ambil(pjawabDosen.getId(), LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3128");
					}
					parameters.put("ttd_pjdosen", ttd);
				}
			}
		}

		if (perkuliahan != null && perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getFakultas() != null
				&& perkuliahan.getJurusan().getFakultas().getDekan() != null) {

			parameters.put("nama_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getNama());
			parameters.put("kode_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getCode());
			parameters.put("mykode_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getMycode());
			parameters.put("nidn_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getNidn());

			LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getDekan().getId(),
					LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3155");
					}
					parameters.put("ttd_dekan", ttd);
				}
			}
		}

		if (perkuliahan != null && perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getFakultas() != null
				&& perkuliahan.getJurusan().getFakultas().getPudek1() != null) {

			parameters.put("nama_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getNama());
			parameters.put("kode_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getCode());
			parameters.put("mykode_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getMycode());
			parameters.put("nidn_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getNidn());

			LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getPudek1().getId(),
					LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3182");
					}
					parameters.put("ttd_pudek1", ttd);
				}
			}
		}

		if (perkuliahan != null && perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getFakultas() != null
				&& perkuliahan.getJurusan().getFakultas().getPudek2() != null) {

			parameters.put("nama_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getNama());
			parameters.put("kode_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getCode());
			parameters.put("mykode_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getMycode());
			parameters.put("nidn_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getNidn());

			LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getPudek2().getId(),
					LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3209");
					}
					parameters.put("ttd_pudek2", ttd);
				}
			}
		}

		if (perkuliahan != null && perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getFakultas() != null
				&& perkuliahan.getJurusan().getFakultas().getPudek3() != null) {

			parameters.put("nama_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getNama());
			parameters.put("kode_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getCode());
			parameters.put("mykode_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getMycode());
			parameters.put("nidn_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getNidn());

			LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getPudek3().getId(),
					LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3236");
					}
					parameters.put("ttd_pudek3", ttd);
				}
			}
		}

		if (perkuliahan != null && perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getKaprodi() != null) {

			parameters.put("nama_kaprodi", perkuliahan.getJurusan().getKaprodi().getNama());
			parameters.put("kode_kaprodi", perkuliahan.getJurusan().getKaprodi().getCode());
			parameters.put("mykode_kaprodi", perkuliahan.getJurusan().getKaprodi().getMycode());
			parameters.put("nidn_kaprodi", perkuliahan.getJurusan().getKaprodi().getNidn());

			LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getKaprodi().getId(),
					LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = "";
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3262");
					}
					parameters.put("ttd_kaprodi", ttd);
				}
			}
		}

		System.out.println("pertemuan => " + pertemuan);

		System.out.println("petugas => " + petugas + ", petugas2 " + petugas2 + ", petugas3 " + petugas3);

		parameters.put("petugas", petugas == null ? "" : petugas.getNama());
		parameters.put("petugas_nip", petugas == null ? "" : petugas.getMycode());

		parameters.put("petugas2", petugas2 == null ? "" : petugas2.getNama());
		parameters.put("petugas_nip2", petugas2 == null ? "" : petugas2.getMycode());

		parameters.put("petugas3", petugas3 == null ? "" : petugas3.getNama());
		parameters.put("petugas_nip3", petugas3 == null ? "" : petugas3.getMycode());

		parameters.put("petugas4", petugas4 == null ? "" : petugas4.getNama());
		parameters.put("petugas_nip4", petugas4 == null ? "" : petugas4.getMycode());

		parameters.put("pjdosen", pj == null ? "" : pj.getNama());
		parameters.put("pjdosen_nip", pj == null ? "" : pj.getMycode());

		parameters.put("pengawas_ujian", (petugas == null ? "" : petugas.getNama()) + " "
				+ (petugas2 == null ? "" : petugas2.getNama()) + " " + (petugas3 == null ? "" : petugas3.getNama()));

		parameters.put("catatan", pertemuan == null ? null : pertemuan.getCatatan());

		parameters.put("tanggal_ujian", pertemuan == null ? null : pertemuan.getTanggal());

		parameters.put("tanggal_ujian_format", pertemuan == null || pertemuan.getTanggal() == null ? null
				: Common.dateFormat2.get().format(pertemuan.getTanggal()));

		parameters.put("tanggal_ujian_tanggal", pertemuan == null || pertemuan.getTanggal() == null ? null
				: Common.dateFormatTgl.get().format(pertemuan.getTanggal()));

		parameters.put("tanggal_ujian_bulan", pertemuan == null || pertemuan.getTanggal() == null ? null
				: Common.dateFormatBln.get().format(pertemuan.getTanggal()));

		parameters.put("tanggal_ujian_tahun", pertemuan == null || pertemuan.getTanggal() == null ? null
				: Common.dateFormatThn.get().format(pertemuan.getTanggal()));

		parameters.put("tanggal_ujian_hari", pertemuan == null || pertemuan.getTanggal() == null ? null
				: Common.dateFormatHari.get().format(pertemuan.getTanggal()));

		parameters.put("tanggal_lengkap", pertemuan == null || pertemuan.getTanggal() == null ? ""
				: Common.dateFormat6.get().format(pertemuan.getTanggal()));

		parameters.put("waktu",
				(pertemuan == null ? (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai())
						: (pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai()) + ""
								+ (pertemuan == null || pertemuan.getTanggal() == null ? ""
										: ", " + Common.dateFormat4.get().format(pertemuan.getTanggal()))));

		parameters.put("waktu_mulai", pertemuan == null ? (perkuliahan.getWaktuMulai()) : (pertemuan.getWaktuMulai()));

		parameters.put("waktu_selesai",
				pertemuan == null ? (perkuliahan.getWaktuSelesai()) : (pertemuan.getWaktuSelesai()));

		parameters.put("waktu_aja",
				pertemuan == null ? (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai())
						: (pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai()));

		parameters.put("tanggal_aja", pertemuan == null ? ""
				: (pertemuan.getTanggal() == null ? "" : Common.dateFormat4.get().format(pertemuan.getTanggal())));
		parameters.put("ruang",
				pertemuan == null ? (perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKodeRuangan())
						: (pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getKodeRuangan()));

		parameters.put("nama_ruang",
				pertemuan == null ? (perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama())
						: (pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getNama()));
		parameters.put("ujian", pertemuanPunyaUjian.getNama());
		parameters.put("maps", maps);
		return parameters;
	}

	/**
	 * Implementasi {@link DataLoader}: memuat ulang seluruh {@link PertemuanPunyaUjian} milik
	 * {@link #pertemuan} dari {@code pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser)} lalu
	 * merender ulang tampilan. Dipanggil pertama kali oleh {@link #display(Pertemuan, Component)}
	 * dan berulang kali setelahnya sebagai callback refresh dari berbagai aksi (tutup modal
	 * pengaturan, tutup window Kelola Soal, simpan/hapus, tombol Refresh, dsb.).
	 *
	 * <p>Menentukan target render berdasarkan field mana yang terisi:</p>
	 * <ul>
	 * <li>Bila {@link #kartuWrap} != {@code null} (jalur AKTIF saat ini — lihat kuirk field
	 * {@link #grid} pada Javadoc kelas): {@code kartuWrap} dikosongkan lalu diisi ulang kartu per
	 * ujian — {@link #buatKartuUjianRingkas} untuk pengelola/dosen, {@link #buatKartuUjianPeserta}
	 * untuk peserta (mahasiswa/biodataCalonMahasiswa/siswa/calon siswa/peserta kursus); bila daftar
	 * kosong, ditampilkan pesan kosong yang beda teks untuk pengelola vs peserta. Method
	 * mengembalikan (return) segera setelah cabang ini, TIDAK sampai ke cabang {@code grid} di
	 * bawahnya.</li>
	 * <li>Bila {@link #kartuWrap} {@code null} DAN {@link #grid} != {@code null}: jalur grid tabel
	 * lama — memasang {@link DetailPertemuanRenderer} pada {@link #grid}. Karena {@link #grid}
	 * sekarang tidak pernah diisi grid sungguhan (selalu {@code null} dari
	 * {@link #display(Pertemuan, Component)}), cabang ini praktis tidak pernah tereksekusi lewat
	 * alur normal aplikasi.</li>
	 * </ul>
	 *
	 * <p><b>Pemilih peran ada di sini, dan ejaannya tidak lengkap.</b> Variabel lokal
	 * {@code pengelola} adalah SATU-SATUNYA penentu apakah seorang pengguna melihat kartu
	 * pengelola (dengan Kelola Soal, Ubah, Hapus, Gandakan) atau kartu peserta. Predikatnya
	 * memeriksa field {@link #mahasiswa}/{@link #biodataCalonMahasiswa} milik helper ini beserta
	 * {@code tbmuser.getPesertaKursus()}/{@code getSiswa()}/{@code getCalonSiswa()}, tetapi TIDAK
	 * memeriksa {@code tbmuser.getMahasiswa()} maupun {@code tbmuser.getBiodataCalonMahasiswa()}.
	 * Akibatnya, bila helper dikonstruksi dengan identitas kosong
	 * ({@code new PertemuanPunyaUjianHelper(null, null)}), akun yang {@link Tbmuser}-nya tertaut ke
	 * {@link Mahasiswa}/{@link BiodataCalonMahasiswa} tetap dianggap pengelola. Jalur pemanggilan
	 * lewat {@code PertemuanHelper} tidak terdampak karena konstruktornya sengaja mengambil ulang
	 * identitas dari sesi login, begitu pula {@code UjianOnlineCalonMahasiswaAction} yang selalu
	 * mengirim {@link BiodataCalonMahasiswa} non-null; yang memakai identitas kosong adalah
	 * {@code JadwalUjianAction} (masih dijaga {@code CommonPrivilages}) dan
	 * {@code HasilUjianMahasiswaHelper}. Lihat {@code task_d45feed7}.
	 *
	 * @param value {@code Boolean.TRUE} untuk memaksa {@code pertemuan.belum("pertemuan_punya_Ujian")}
	 *              (menghapus cache lokal koleksi ini di entity {@link Pertemuan} sehingga data
	 *              benar-benar dimuat ulang dari DB, bukan dari cache); nilai lain (termasuk
	 *              {@code null} atau {@code false}) memakai cache bila masih ada.
	 */
	public void loadData(Object value) {

		if (value != null && value.equals(true)) {
			pertemuan.belum("pertemuan_punya_Ujian");
		}
		Tbmuser tbmuser = Common.getCurrentUser();

		List<PertemuanPunyaUjian> pertemuanPunyaUjian = new ArrayList<PertemuanPunyaUjian>(
				pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser).values());

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// refresh=true agar cache pertemuan_punya_Ujian dihapus dan data terbaru dimuat dari DB.
				loadData(true);
			}
		};

		if (kartuWrap != null) {
			// Tampilan KARTU untuk semua peran. Pengelola → kartu ringkas + modal
			// pengaturan; peserta (mahasiswa/siswa/calon/biodata) → kartu ikut ujian.
			boolean pengelola = tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
					&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null;

			Common.clear(kartuWrap);
			if (pertemuanPunyaUjian.isEmpty()) {
				kartuWrap.appendChild(ais.ui.util.DashboardUiKit.html(
						"<div style='grid-column:1/-1;padding:22px;text-align:center;color:#64748b;font-size:12px;"
								+ "border:1px dashed #cbd5e1;border-radius:14px;background:#f8fafc;'>"
								+ (pengelola ? "Belum ada ujian pada pertemuan ini. Klik <b>Buat Ujian</b> atau "
										+ "<b>Ambil Bahan Ujian</b> di atas untuk menambahkan."
										: "Belum ada ujian pada pertemuan ini.")
								+ "</div>"));
			} else {
				for (PertemuanPunyaUjian ppu : pertemuanPunyaUjian) {
					try {
						if (pengelola) {
							buatKartuUjianRingkas(ppu, tbmuser, eventListener);
						} else {
							buatKartuUjianPeserta(ppu, tbmuser, eventListener);
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
			return;
		}

		if (grid == null) {
			return;
		}
		ListModel strset = new SimpleListModel(pertemuanPunyaUjian);
		grid.setRowRenderer(new DetailPertemuanRenderer(mahasiswa, biodataCalonMahasiswa, eventListener, false));
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mem-parse {@code nilai} sebagai {@link Integer}, mengembalikan {@code defaultValue} bila
	 * {@code null}, kosong setelah di-trim, literal {@code "null"}/{@code "-"}, atau gagal
	 * di-parse. Dipakai untuk membaca nilai konfigurasi teks (mis. {@code "kuota_ujian"}) yang
	 * kadang tersimpan sebagai placeholder non-angka.
	 *
	 * @param nilai        teks yang akan di-parse, boleh {@code null}.
	 * @param defaultValue nilai fallback bila {@code nilai} tidak bisa di-parse sebagai angka valid.
	 * @return hasil parse, atau {@code defaultValue}.
	 */
	private static int parseIntegerDefault(String nilai, int defaultValue) {
		if (nilai == null) {
			return defaultValue;
		}
		String teks = nilai.trim();
		if (teks.length() == 0 || "null".equalsIgnoreCase(teks) || "-".equals(teks)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(teks);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Membangun satu KARTU "Ikut Ujian" untuk peserta ujian, yaitu ketika pengguna login
	 * sebagai <b>Mahasiswa</b>, <b>Siswa</b>, <b>Calon Siswa</b>, atau
	 * <b>Biodata Calon Mahasiswa</b> (termasuk peserta kursus). Kartu ini menampilkan
	 * ringkasan ujian yang perlu diketahui peserta (nama, jenis, jumlah soal, durasi,
	 * jadwal pelaksanaan, batas maksimal ikut, dan — bila diizinkan — nilai/hasil) serta
	 * satu area aksi berisi tombol untuk mulai/melanjutkan/melihat hasil ujian.
	 *
	 * <p><b>Tujuan pemisahan.</b> Peserta hanya boleh <i>mengikuti</i> ujian, bukan
	 * mengubah pengaturannya. Karena itu kartu peserta sengaja dibuat terpisah dari kartu
	 * pengelola/dosen: tidak ada tombol pengaturan, edit, hapus, maupun kelola soal.
	 * Tampilan dibuat ringkas dan responsif (1 kartu per baris pada layar HP, 2 kartu per
	 * baris pada layar lebar) agar nyaman dibaca di perangkat apa pun.
	 *
	 * <p><b>Logika kelayakan ikut ujian DIPERTAHANKAN.</b> Seluruh keputusan boleh/tidak
	 * boleh ikut, teks tombol ("Ikut Ujian" / "Lihat Hasil" / "Ubah/Perbaiki Jawaban"),
	 * batas waktu, sisa percobaan, hingga aksi saat tombol diklik tetap ditangani oleh
	 * metode lama {@link #tampilBolekIkutUjianAtauTidak(org.zkoss.zk.ui.Component,
	 * PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, HasilUjianMahasiswa,
	 * EventListener, java.util.List)} yang dipanggil ke area aksi kartu. Metode ini hanya
	 * mengganti kemasan visual (dari sel tabel lebar menjadi kartu), bukan logikanya.
	 *
	 * <p><b>Penjagaan kuota & izin (identik dengan tampilan lama).</b> Bila kuota ujian
	 * yang sedang berjalan penuh, kartu menampilkan pesan menunggu dan otomatis memuat
	 * ulang setelah beberapa detik. Bila ujian tidak aktif atau peserta ditandai "tidak
	 * perlu ikut", kartu menampilkan pesan bahwa peserta tidak diizinkan ikut — sama
	 * seperti perilaku sebelumnya, hanya divisualisasikan sebagai kartu.
	 *
	 * <p><b>Kedua penjaga di atas bersifat TAMPILAN, bukan penegakan.</b> Keduanya hanya
	 * memutuskan kartu mana yang digambar lalu {@code return} — tidak ada satu pun pemeriksaan di
	 * sini yang menghalangi pemanggilan {@code ProsesUjianHelper}. Penegakan yang sesungguhnya
	 * berada di {@link #tampilBolekIkutUjianAtauTidak} yang dipanggil di akhir method untuk
	 * mengisi area aksi kartu. Perhatikan juga bahwa identitas peserta ({@code idPeserta}) yang
	 * dipakai penjaga kedua hanya menengok {@link #mahasiswa}, {@link #biodataCalonMahasiswa},
	 * {@code tbmuser.getSiswa()}, dan {@code tbmuser.getCalonSiswa()} — TIDAK
	 * {@code tbmuser.getPesertaKursus()}. Akibatnya, untuk akun peserta kursus {@code idPeserta}
	 * bernilai {@code null} sehingga daftar "tidak perlu ikut ujian"
	 * ({@code ppu.getMhsYgTidakIkut()}) maupun pemeriksaan ujian non-aktif tidak pernah berlaku
	 * bagi mereka pada lapis kartu ini; yang tersisa hanyalah pemeriksaan di
	 * {@link #tampilBolekIkutUjianAtauTidak}, yang memakai daftar identitas berbeda lagi.
	 *
	 * @param ppu ujian pada pertemuan yang akan dibuatkan kartu peserta
	 * @param tbmuser pengguna aktif (untuk menentukan identitas peserta)
	 * @param refresh listener untuk memuat ulang daftar setelah selesai/ubah ujian
	 */
	private void buatKartuUjianPeserta(final PertemuanPunyaUjian ppu, final Tbmuser tbmuser,
			final EventListener refresh) {
		final Ujian ujian = ppu.getUjian();
		if (ujian == null) {
			return;
		}

		HasilUjianMahasiswa hasil = null;
		try {
			hasil = HasilUjianMahasiswa.ambilByKey(ppu, mahasiswa, biodataCalonMahasiswa, null, null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// Identitas peserta (untuk cek izin) — sama seperti tampilan lama.
		Long idPeserta = mahasiswa != null ? mahasiswa.getId()
				: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
						: tbmuser != null && tbmuser.getSiswa() != null ? tbmuser.getSiswa().getId()
								: tbmuser != null && tbmuser.getCalonSiswa() != null ? tbmuser.getCalonSiswa().getId()
										: null;

		Div kartu = new Div();
		kartu.setSclass("ppu-kartu");
		kartu.setParent(kartuWrap);

		String nama = ujian.getNama() == null ? "(Tanpa nama)" : ujian.getNama();
		String jenis = Common.getBahasaConfig(ujian.getJenis());

		// --- Penjaga 1: kuota ujian penuh (identik dengan tampilan lama) ---
		if (hasil != null) {
			int kuota = 120;
			try {
				kuota = parseIntegerDefault(Common.getKonfigurasi("kuota_ujian", kuota + "").getNilai(), kuota);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:3466");
			}
			if (kuota <= ProsesUjianHelper.kuotaUjian.size()
					&& !ProsesUjianHelper.kuotaUjian.contains(hasil.getKeyhasil())) {
				kartu.appendChild(ais.ui.util.DashboardUiKit.html(kepalaKartuPeserta(nama, jenis)
						+ "<div style='padding:14px 16px;font-size:12px;color:#b45309;background:#fffbeb;"
						+ "border-top:1px solid #fde68a;line-height:1.5;'>Mohon menunggu, kuota ujian sedang penuh. "
						+ "Halaman akan otomatis diperbarui. Klik <b>Lihat Peserta Ujian</b> untuk melihat peserta "
						+ "yang sedang ujian.</div>"));
				Common.createDefaultTimer(refresh, "", false, 5000);
				return;
			}
		}

		// --- Penjaga 2: ujian tidak aktif / peserta tidak diizinkan (identik lama) ---
		boolean tidakDiizinkan = (idPeserta != null && ujian != null && !ujian.getAktif())
				|| (ppu.getMhsYgTidakIkut() != null && idPeserta != null
						&& ppu.getMhsYgTidakIkut().contains("," + idPeserta + ","));
		if (tidakDiizinkan) {
			kartu.appendChild(ais.ui.util.DashboardUiKit.html(kepalaKartuPeserta(nama, jenis)
					+ "<div style='padding:14px 16px;font-size:12px;color:#991b1b;background:#fef2f2;"
					+ "border-top:1px solid #fecaca;line-height:1.5;'>Anda tidak diizinkan mengikuti ujian \""
					+ ais.ui.util.DashboardUiKit.esc(nama) + "\".</div>"));
			return;
		}

		// --- Kartu normal: ringkasan + area aksi "Ikut Ujian" ---
		boolean dibatasi = Boolean.TRUE.equals(ppu.getDibatasiWaktu());
		String jmlSoal = ppu.getJmlDitampilkan() == null || ppu.getJmlDitampilkan() <= 0 ? "-"
				: Common.numberFormat.get().format(ppu.getJmlDitampilkan());
		String durasi = !dibatasi || ppu.getLama() == null ? "Tanpa batas"
				: Common.timeFormat1.get().format(ppu.getLama());
		String pelaksanaan = !dibatasi ? "Tanpa batas waktu"
				: ((ppu.getMulaiUjian() == null ? "-" : Common.dateFormat.get().format(ppu.getMulaiUjian())) + "  s.d  "
						+ (ppu.getSampaiUjian() == null ? "-" : Common.dateFormat.get().format(ppu.getSampaiUjian())));
		String maksIkut = ppu.getJumlahBolehIkut() == null ? "-"
				: Common.numberFormat.get().format(ppu.getJumlahBolehIkut()) + " kali";
		String sudahIkut = hasil == null || hasil.getJumlahIkut() == null ? "0 kali"
				: Common.numberFormat.get().format(hasil.getJumlahIkut()) + " kali";

		StringBuffer sb = new StringBuffer();
		sb.append(kepalaKartuPeserta(nama, jenis));
		sb.append("<div class='ppu-kartu-body'>");
		sb.append(chip("Jumlah soal", jmlSoal + " soal"));
		sb.append(chip("Durasi", durasi));
		sb.append(chip("Pelaksanaan", pelaksanaan));
		sb.append(chip("Maks. ikut", maksIkut));
		sb.append(chip("Sudah ikut", sudahIkut));
		if (Boolean.TRUE.equals(ppu.getLihatNilaiSetelahUjian()) && hasil != null && hasil.getNilai() != null) {
			String nilaiTeks = Common.numberFormat.get().format(hasil.getNilai())
					+ (ujian.getJenis() != null && (ujian.getJenis().equalsIgnoreCase(BankSoal.ESAY)
							|| ujian.getJenis().equalsIgnoreCase(BankSoal.JAWABAN_SINGKAT)) ? ""
									: " (" + (hasil.getLulus() ? Common.getBahasaConfig("Lulus")
											: Common.getBahasaConfig("Tidak Lulus")) + ")");
			sb.append(chip("Nilai", nilaiTeks));
		}
		if ((mahasiswa != null || biodataCalonMahasiswa != null) && ppu.getFakultas() != null) {
			sb.append(chip(Common.getBahasaConfig("Fakultas"), ppu.getFakultas().getNama()));
		}
		if ((mahasiswa != null || biodataCalonMahasiswa != null) && ppu.getJurusan() != null) {
			sb.append(chip(Common.getBahasaConfig("Jurusan"), ppu.getJurusan().getNama()));
		}
		if (hasil != null && hasil.getKeterangan() != null && !hasil.getKeterangan().isEmpty()) {
			sb.append(chip("Keterangan", hasil.getKeterangan()));
		}
		sb.append("</div>");
		sb.append(buatKetentuanUjianHtml(ppu));
		kartu.appendChild(ais.ui.util.DashboardUiKit.html(sb.toString()));

		// Area aksi: reuse logika lama tampilBolekIkutUjianAtauTidak (0 perubahan logika).
		Div foot = new Div();
		foot.setSclass("ppu-kartu-foot ppu-kartu-foot-peserta");
		foot.setParent(kartu);
		tampilBolekIkutUjianAtauTidak(foot, ppu, mahasiswa, biodataCalonMahasiswa, hasil, refresh, null);
	}

	/**
	 * Menyusun potongan HTML bagian kepala (header) kartu peserta: nama ujian sebagai judul, plus
	 * satu badge kecil berisi jenis ujian. Dipakai ulang oleh KETIGA keluaran
	 * {@link #buatKartuUjianPeserta(PertemuanPunyaUjian, Tbmuser, EventListener)} — kartu normal,
	 * kartu "kuota penuh", dan kartu "tidak diizinkan" — supaya ketiganya tetap punya kepala yang
	 * identik walau badan kartunya berbeda.
	 *
	 * <p>Berbeda dengan kepala kartu pengelola (dibangun inline di
	 * {@link #buatKartuUjianRingkas(PertemuanPunyaUjian, Tbmuser, EventListener)}), kepala kartu
	 * peserta sengaja TIDAK menampilkan badge jenis koreksi maupun penanda Aktif/Non-aktif, karena
	 * keduanya informasi pengelola, bukan informasi yang berguna bagi peserta.
	 *
	 * <p>Kedua argumen di-escape lewat {@code DashboardUiKit.esc(...)} sebelum disisipkan, sehingga
	 * nama ujian yang mengandung karakter HTML tidak bisa menyuntikkan markup ke dalam kartu.
	 *
	 * @param nama  nama ujian yang ditampilkan sebagai judul kartu; pemanggil sudah mengganti
	 *              {@code null} dengan teks "(Tanpa nama)".
	 * @param jenis jenis ujian yang sudah dilewatkan {@code Common.getBahasaConfig(...)} sehingga
	 *              berupa istilah terlokalisasi, bukan konstanta mentah.
	 * @return potongan HTML kepala kartu, siap disambung dengan badan kartu.
	 */
	private String kepalaKartuPeserta(String nama, String jenis) {
		return "<div class='ppu-kartu-head'><div style='min-width:0;'><div class='ppu-kartu-nama'>"
				+ ais.ui.util.DashboardUiKit.esc(nama) + "</div><span class='ppu-badge'>"
				+ ais.ui.util.DashboardUiKit.esc(jenis) + "</span></div></div>";
	}

	/**
	 * Membangun satu KARTU ringkas untuk sebuah {@link PertemuanPunyaUjian} pada
	 * tampilan pengelola/dosen, lalu memasangnya ke {@link #kartuWrap}. Dipanggil sekali per ujian
	 * dari {@link #loadData(Object)}, HANYA pada cabang {@code pengelola == true}; padanan untuk
	 * peserta adalah {@link #buatKartuUjianPeserta(PertemuanPunyaUjian, Tbmuser, EventListener)}.
	 * Karena pemilihan cabang itulah satu-satunya penjaga peran untuk kartu ini, method ini
	 * sendiri TIDAK memeriksa peran lagi — parameter {@code tbmuser} bahkan tidak dipakai untuk
	 * mengatur visibilitas tombol apa pun di sini. Keluar lebih awal tanpa membuat kartu bila
	 * {@code ppu.getUjian()} {@code null} (tautan ujian sudah terputus).
	 *
	 * <p><b>Isi kartu — ringkasan dan statistik.</b> Kepala kartu memuat nama ujian, badge jenis
	 * beserta jenis koreksi, dan penanda Aktif/Non-aktif. Badan kartu berupa deretan
	 * {@link #chip(String, String)}: status pelaksanaan yang dihitung dengan membandingkan waktu
	 * kini terhadap jendela {@code mulaiUjian}/{@code sampaiUjian} ("Belum dibuka" / "Sedang
	 * berlangsung" / "Sudah ditutup" / "Tanpa batas waktu"), total peserta kelas, jumlah yang
	 * sudah dan belum mengerjakan beserta persentase progres, jumlah soal RIIL di bank ujian
	 * berdampingan dengan jumlah yang ditampilkan per peserta, status pengacakan, durasi, batas
	 * percobaan, jadwal, dan komponen nilai tujuan.
	 *
	 * <p><b>Mengapa jumlah soal ditampilkan dua angka.</b> Sebelumnya kartu hanya menampilkan
	 * {@code getJmlDitampilkan()} — jumlah soal yang ditampilkan per peserta menurut konfigurasi,
	 * BUKAN jumlah soal yang benar-benar ada di bank. Akibatnya kartu bisa tertulis "1 soal"
	 * padahal bank soalnya kosong, dan pengelola tidak punya petunjuk mengapa peserta melapor
	 * tidak melihat soal. Kini keduanya ditampilkan berdampingan sehingga bank soal kosong
	 * langsung terlihat.
	 *
	 * <p><b>Statistik nilai.</b> Rata-rata, tertinggi, dan terendah diambil lewat SATU criteria
	 * agregat ({@code avg}/{@code max}/{@code min} atas {@code nilai} milik
	 * {@link HasilUjianMahasiswa} yang menunjuk {@code ppu}), bukan dengan memuat seluruh baris
	 * hasil. Bila kurikulum perkuliahan berstatus OBE, ditambahkan satu chip per Sub-CPMK: kolom
	 * {@code nilaiObe} (JSON {@code {idFormatNilai: nilai, idFormatNilai_max: maks}}) diambil
	 * lewat satu query proyeksi lalu diagregasi di memori; JSON yang rusak dilewati diam-diam agar
	 * satu baris cacat tidak menghilangkan seluruh blok statistik.
	 *
	 * <p><b>Ketahanan.</b> Setiap pengambilan data opsional (jumlah peserta yang sudah ikut, total
	 * mahasiswa kelas, jumlah soal bank, agregat nilai, agregat OBE) dibungkus {@code try/catch}
	 * sendiri yang melaporkan lewat {@code Common.tampilErrorJikaAdmin}. Kegagalan salah satunya
	 * hanya membuat chip terkait menampilkan nilai default ({@code 0} atau {@code "-"}); kartu
	 * tetap terbentuk dan tombol aksinya tetap berfungsi.
	 *
	 * <p><b>Tombol aksi.</b> Semua tombol dibungkus satu grup ({@code ppu-gbtngrp}) yang sengaja
	 * dipasang ke kartu SEBELUM badan info, sehingga tampil di atas ringkasan. Isinya:
	 * <ul>
	 * <li><b>Pengaturan Data Ujian</b> — {@link #bukaPengaturanUjian(PertemuanPunyaUjian,
	 * EventListener)}, satu-satunya pintu ke kontrol pengaturan rinci, yang me-reuse
	 * {@link DetailPertemuanRenderer} lama sehingga seluruh kontrol dan event editing berfungsi
	 * persis seperti tampilan tabel sebelumnya.</li>
	 * <li><b>Hasil Ujian</b> — {@code HasilUjianMahasiswaHelper} dalam window tersendiri, dengan
	 * {@code onClose} yang memicu {@code refresh} agar ringkasan nilai di kartu ikut terbarui.</li>
	 * <li><b>Preview</b> — menjalankan {@code ProsesUjianHelper.ikut(...)} atas identitas pengguna
	 * saat ini, sehingga pengelola bisa mencoba ujian seperti peserta.</li>
	 * <li><b>Ubah</b> — {@code UjianAction.onAddExternal} untuk menyunting master {@link Ujian}.</li>
	 * <li><b>Sinkronkan Nilai</b> (hanya bila pertemuan punya perkuliahan) —
	 * {@code GradingHelper.hitungNilaiBerdasarkanFormatNilaiObe} bila OBE, selain itu
	 * {@code hitungNilaiBerdasarkanFormatNilai}; cabang non-OBE menolak dengan pesan pemandu bila
	 * komponen penilaian tujuan belum dipilih.</li>
	 * <li><b>Hapus</b> — setelah konfirmasi, menghapus {@link HasilUjianMahasiswaDetail} lalu
	 * {@link HasilUjianMahasiswa} milik ujian ini lewat SQL mentah (id disisipkan sebagai
	 * {@code Long}, bukan teks pengguna), baru {@code Common.refreshDelete} pada {@code ppu}.</li>
	 * <li><b>Kelola Soal</b> — {@code DetailUjianHelper} penuh di {@link MyWindow} tersendiri;
	 * identik dengan tombol bernama sama di dalam modal pengaturan.</li>
	 * <li><b>Gandakan</b> — menyalin {@link Ujian}, seluruh {@link UjianPunyaSoal}, beserta
	 * {@link BankSoal} dan {@link BankSoalDetail}-nya, lalu membuat {@link PertemuanPunyaUjian}
	 * baru pada pertemuan yang sama dengan status non-aktif agar salinan tidak langsung terlihat
	 * peserta sebelum diperiksa. Penting untuk integritas bank soal: tiap {@link BankSoalDetail}
	 * salinan di-{@code setBankSoal(bankSoalBaru)} dan {@code kodeUnik}-nya dikosongkan, sehingga
	 * opsi jawaban salinan TIDAK menggantung pada soal aslinya.</li>
	 * <li><b>Download Soal</b> / <b>Upload Soal</b> — ekspor-impor soal lewat berkas Excel
	 * ({@code DetailUjianHelper.doDownload}); unggahan hanya menerima {@code .xlsx} dan menolak
	 * {@code .xls}/{@code .ods}/{@code .csv} dengan pesan pemandu.</li>
	 * </ul>
	 *
	 * @param ppu     ujian pada pertemuan yang akan dibuatkan kartu; diabaikan bila
	 *                {@code ppu.getUjian()} {@code null}.
	 * @param tbmuser pengguna aktif. Diterima demi keseragaman tanda tangan dengan
	 *                {@link #buatKartuUjianPeserta(PertemuanPunyaUjian, Tbmuser, EventListener)},
	 *                namun TIDAK dipakai di dalam method ini — penentuan peran sudah terjadi di
	 *                {@link #loadData(Object)}.
	 * @param refresh listener yang dipicu setelah aksi yang mengubah data (hapus, gandakan, ubah,
	 *                sinkronkan nilai, atau penutupan window Hasil/Kelola Soal) agar seluruh
	 *                daftar kartu dimuat ulang dari database.
	 */
	private void buatKartuUjianRingkas(final PertemuanPunyaUjian ppu, final Tbmuser tbmuser,
			final EventListener refresh) {
		final Ujian ujian = ppu.getUjian();
		if (ujian == null) {
			return;
		}

		String nama = ujian.getNama() == null ? "(Tanpa nama)" : ujian.getNama();
		String jenis = Common.getBahasaConfig(ujian.getJenis());
		String jenisKoreksi = Common.getBahasaConfig(ujian.getJenisKoreksi());
		boolean aktif = Boolean.TRUE.equals(ujian.getAktif());

		int peserta = 0;
		try {
			Number tg = ppu.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
			peserta = tg == null ? 0 : tg.intValue();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// Total peserta kelas + rincian sudah/belum mengerjakan (permintaan user).
		int totalPeserta = 0;
		try {
			if (ppu.getPertemuan() != null && ppu.getPertemuan().getPerkuliahan() != null) {
				java.util.List<?> daftarMhs = ppu.getPertemuan().getPerkuliahan().ambilMahasiswa();
				totalPeserta = daftarMhs == null ? 0 : daftarMhs.size();
			}
		} catch (Exception ePst) {
			Common.tampilErrorJikaAdmin(ePst);
		}
		int belumMengerjakan = totalPeserta - peserta;
		if (belumMengerjakan < 0) {
			belumMengerjakan = 0;
		}

		// "Jumlah soal" dulu memakai getJmlDitampilkan() (jumlah soal DITAMPILKAN per peserta, dari
		// konfig) — BUKAN jumlah soal RIIL di bank ujian. Akibatnya kartu bisa tertulis "1 soal"
		// padahal bank soal KOSONG -> peserta tak melihat soal ("jumlah soal tidak muncul" di akun
		// mhs). Kini tampilkan JUMLAH SOAL RIIL di bank + berapa yang ditampilkan per peserta, agar
		// admin bisa LANGSUNG melihat bila bank soal ternyata kosong.
		int jmlSoalBank = 0;
		try {
			java.util.List<Long> soalIdsBank = ujian.ambilUjianPunyaSoal(ppu, false);
			jmlSoalBank = soalIdsBank == null ? 0 : soalIdsBank.size();
		} catch (Exception eSoal) {
			Common.tampilErrorJikaAdmin(eSoal);
		}
		String ditampilkan = ppu.getJmlDitampilkan() == null || ppu.getJmlDitampilkan().intValue() <= 0 ? "semua"
				: Common.numberFormat.get().format(ppu.getJmlDitampilkan());
		boolean dibatasi = Boolean.TRUE.equals(ppu.getDibatasiWaktu());
		String durasi = !dibatasi || ppu.getLama() == null ? "Tanpa batas"
				: Common.timeFormat1.get().format(ppu.getLama());
		String mulai = !dibatasi || ppu.getMulaiUjian() == null ? "-"
				: Common.dateFormat.get().format(ppu.getMulaiUjian());
		String sampai = !dibatasi || ppu.getSampaiUjian() == null ? "-"
				: Common.dateFormat.get().format(ppu.getSampaiUjian());
		String pelaksanaan = (!dibatasi) ? "Tanpa batas waktu" : (mulai + "  s.d  " + sampai);
		String bolehIkut = ppu.getJumlahBolehIkut() == null ? "-"
				: Common.numberFormat.get().format(ppu.getJumlahBolehIkut()) + " kali";
		String nilaiMasukKe = ppu.getFormatNilai() == null || ppu.getFormatNilai().getStatusPertemuan() == null ? "-"
				: (ppu.getFormatNilai().getNama() + " (" + Common.numberFormat.get().format(ppu.getProsentase())
						+ "%)");

		// Info tambahan bermanfaat: status pelaksanaan (jadwal vs waktu kini), progres pengerjaan,
		// dan apakah urutan soal diacak.
		String statusPelaksanaan;
		if (!dibatasi) {
			statusPelaksanaan = "Tanpa batas waktu";
		} else {
			java.util.Date kini = ais.ui.util.WaktuUtil.getDate();
			if (ppu.getMulaiUjian() != null && kini.before(ppu.getMulaiUjian())) {
				statusPelaksanaan = "Belum dibuka";
			} else if (ppu.getSampaiUjian() != null && kini.after(ppu.getSampaiUjian())) {
				statusPelaksanaan = "Sudah ditutup";
			} else {
				statusPelaksanaan = "Sedang berlangsung";
			}
		}
		String progres = totalPeserta > 0 ? (Math.round(peserta * 100.0 / totalPeserta) + "%") : "-";
		String acakSoal = Boolean.TRUE.equals(ppu.getRandom()) ? "Ya (diacak)" : "Tidak (berurutan)";

		// Statistik nilai (rata-rata / tertinggi / terendah) dari hasil ujian yang sudah masuk —
		// SATU query agregat (avg/max/min) agar efisien.
		String rataNilai = "-", tinggiNilai = "-", rendahNilai = "-";
		try {
			java.text.DecimalFormat dfNilai = new java.text.DecimalFormat("#0.##");
			Object[] aggNilai = (Object[]) ais.database.hibernate.HibernateUtil.currentSession()
					.createCriteria(ais.database.model.HasilUjianMahasiswa.class)
					.add(org.hibernate.criterion.Restrictions.eq("pertemuanPunyaUjian", ppu))
					.setProjection(org.hibernate.criterion.Projections.projectionList()
							.add(org.hibernate.criterion.Projections.avg("nilai"))
							.add(org.hibernate.criterion.Projections.max("nilai"))
							.add(org.hibernate.criterion.Projections.min("nilai")))
					.uniqueResult();
			if (aggNilai != null && aggNilai[0] != null) {
				rataNilai = dfNilai.format(((Number) aggNilai[0]).doubleValue());
				tinggiNilai = aggNilai[1] == null ? "-" : dfNilai.format(((Number) aggNilai[1]).doubleValue());
				rendahNilai = aggNilai[2] == null ? "-" : dfNilai.format(((Number) aggNilai[2]).doubleValue());
			}
		} catch (Exception eNilai) {
			Common.tampilErrorJikaAdmin(eNilai);
		}

		// Bila kurikulum OBE: statistik nilai RATA/TERTINGGI/TERENDAH per Sub-CPMK. nilaiObe tiap
		// HasilUjianMahasiswa = JSON {subCpmkId: nilai, subCpmkId_max: max}. Diambil 1 query
		// (proyeksi nilaiObe) lalu diagregasi per Sub-CPMK.
		StringBuffer obeNilaiChips = new StringBuffer();
		try {
			Perkuliahan pkObe = ppu.getPertemuan() == null ? null : ppu.getPertemuan().getPerkuliahan();
			boolean obe = pkObe != null && pkObe.getKurikulum() != null
					&& pkObe.getKurikulum().apakahObe(pkObe.getTahunAjaran(), pkObe.getGanjilGenap());
			if (obe) {
				java.util.List<ais.database.model.FormatNilai> subCpmks = Common
						.getFormatNilais(ais.database.hibernate.HibernateUtil.currentSession(), pkObe);
				@SuppressWarnings("unchecked")
				java.util.List<String> nilaiObeList = ais.database.hibernate.HibernateUtil.currentSession()
						.createCriteria(ais.database.model.HasilUjianMahasiswa.class)
						.add(org.hibernate.criterion.Restrictions.eq("pertemuanPunyaUjian", ppu))
						.setProjection(org.hibernate.criterion.Projections.property("nilaiObe")).list();
				java.util.List<org.json.JSONObject> parsedObe = new java.util.ArrayList<org.json.JSONObject>();
				for (String sObe : nilaiObeList) {
					if (sObe == null || sObe.trim().isEmpty()) {
						continue;
					}
					try {
						parsedObe.add(new org.json.JSONObject(sObe));
					} catch (Exception ig) { /* abaikan JSON rusak */ }
				}
				java.text.DecimalFormat df2 = new java.text.DecimalFormat("#0.##");
				if (subCpmks != null) {
					for (ais.database.model.FormatNilai fn : subCpmks) {
						if (fn == null || fn.getId() == null || fn.getStatusPertemuan() == null) {
							continue;
						}
						String key = fn.getId().toString();
						double sumO = 0, maxO = Double.NEGATIVE_INFINITY, minO = Double.POSITIVE_INFINITY;
						int cntO = 0;
						for (org.json.JSONObject jo : parsedObe) {
							if (jo.isNull(key)) {
								continue;
							}
							double n = jo.getDouble(key);
							sumO += n;
							if (n > maxO) {
								maxO = n;
							}
							if (n < minO) {
								minO = n;
							}
							cntO++;
						}
						String val = cntO == 0 ? "belum ada nilai"
								: ("rata " + df2.format(sumO / cntO) + " · tertinggi " + df2.format(maxO)
										+ " · terendah " + df2.format(minO));
						obeNilaiChips.append(chip("Sub-CPMK: " + ais.ui.util.DashboardUiKit.esc(fn.getNama()), val));
					}
				}
			}
		} catch (Exception eObe) {
			Common.tampilErrorJikaAdmin(eObe);
		}

		Div kartu = new Div();
		kartu.setSclass("ppu-kartu");
		kartu.setParent(kartuWrap);

		StringBuffer sb = new StringBuffer();
		sb.append("<div class='ppu-kartu-head'>");
		sb.append("<div style='min-width:0;'><div class='ppu-kartu-nama'>")
				.append(ais.ui.util.DashboardUiKit.esc(nama)).append("</div>");
		sb.append("<span class='ppu-badge'>").append(ais.ui.util.DashboardUiKit.esc(jenis));
		if (jenisKoreksi != null && jenisKoreksi.length() > 0) {
			sb.append(" &middot; ").append(ais.ui.util.DashboardUiKit.esc(jenisKoreksi));
		}
		sb.append("</span></div>");
		sb.append("<span class='ppu-aktif ").append(aktif ? "ppu-aktif-on'>Aktif" : "ppu-aktif-off'>Non-aktif")
				.append("</span>");
		sb.append("</div>");

		sb.append("<div class='ppu-kartu-body'>");
		sb.append(chip("Status", statusPelaksanaan));
		sb.append(chip("Total peserta", totalPeserta + " peserta"));
		sb.append(chip("Sudah mengerjakan", peserta + " peserta"));
		sb.append(chip("Belum mengerjakan", belumMengerjakan + " peserta"));
		sb.append(chip("Progres pengerjaan", progres));
		sb.append(chip("Jumlah soal (bank)", jmlSoalBank + " soal"));
		sb.append(chip("Ditampilkan / peserta", ditampilkan + " soal"));
		sb.append(chip("Acak soal", acakSoal));
		sb.append(chip("Rata-rata nilai", rataNilai));
		sb.append(chip("Nilai tertinggi", tinggiNilai));
		sb.append(chip("Nilai terendah", rendahNilai));
		sb.append(obeNilaiChips.toString());
		sb.append(chip("Durasi", durasi));
		sb.append(chip("Maks. ikut", bolehIkut));
		sb.append(chip("Pelaksanaan", pelaksanaan));
		sb.append(chip("Nilai masuk ke", nilaiMasukKe));
		sb.append("</div>");
		sb.append(buatKetentuanUjianHtml(ppu));

		// Button group dibangun dulu (belum setParent ke kartu), agar bisa dipasang di ATAS body.
		Div foot = new Div();
		foot.setSclass("ppu-kartu-foot");

		// Semua tombol aksi dibungkus dalam SATU button group yang rapi (berbingkai, seragam).
		Div grp = new Div();
		grp.setSclass("ppu-gbtngrp");
		grp.setParent(foot);

		MyToolbarbuttonConfig btnSet = new MyToolbarbuttonConfig("Pengaturan Data Ujian", "/img/svg/edit-box-line.svg");
		btnSet.setStyle(
				"background:#1d4ed8;color:#fff;border-radius:8px;padding:7px 12px;font-weight:700;font-size:11px;");
		btnSet.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaPengaturanUjian(ppu, refresh);
			}
		});
		btnSet.setParent(grp);

		MyToolbarbuttonConfig btnHasil = new MyToolbarbuttonConfig("Hasil Ujian", "/img/album.png");
		btnHasil.setStyle(
				"background:#eff6ff;color:#1d4ed8;border:1px solid #bfdbfe;border-radius:8px;padding:7px 12px;font-weight:700;font-size:11px;");
		btnHasil.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				HasilUjianMahasiswaHelper hasilUjianMahasiswaHelper = new HasilUjianMahasiswaHelper(
						ppu.getPertemuan());
				Window window = new Window("Hasil Ujian " + ujian.getNama() + " - " + ppu.getPertemuan().toString(),
						"none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("98%");
				window.setWidth("95%");
				hasilUjianMahasiswaHelper.display(ppu, window);
				// Refresh setelah proses: saat jendela "Hasil Ujian" DITUTUP (mungkin ada perubahan
				// nilai/verifikasi), muat ulang kartu agar ringkasan terbarui tanpa klik Refresh manual.
				window.addEventListener("onClose", new EventListener() {
					@Override
					public void onEvent(Event evClose) throws Exception {
						try {
							if (refresh != null) {
								refresh.onEvent(evClose);
							}
						} catch (Exception exReload) {
							Common.tampilErrorJikaAdmin(exReload);
						}
					}
				});
				window.onModal();
			}
		});
		btnHasil.setParent(grp);

		// Aksi cepat pengelola langsung di kartu (dipindah dari dalam modal): Preview, Sinkronkan
		// Nilai, Ubah, Hapus — logikanya identik dengan tombol lama di DetailPertemuanRenderer.
		String gayaAksi = "background:#f8fafc;color:#334155;border:1px solid #e2e8f0;border-radius:8px;"
				+ "padding:7px 12px;font-weight:700;font-size:11px;";

		MyToolbarbuttonConfig btnPreview = new MyToolbarbuttonConfig("Preview", "/img/eye-icon.png");
		btnPreview.setStyle(gayaAksi);
		btnPreview.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Tbmuser tb = Common.getCurrentUser();
				HasilUjianMahasiswa h = HasilUjianMahasiswa.ambilByKey(ppu, mahasiswa, biodataCalonMahasiswa, null, null);
				ProsesUjianHelper.ikut(mahasiswa, biodataCalonMahasiswa, tb == null ? null : tb.getSiswa(),
						tb == null ? null : tb.getCalonSiswa(), ppu, h, true, refresh);
			}
		});
		btnPreview.setParent(grp);

		// Sinkronkan Nilai (hanya bila ada komponen nilai tujuan: OBE Sub-CPMK atau format nilai tunggal).
		final Perkuliahan perkuliahanKartu = ppu.getPertemuan() == null ? null : ppu.getPertemuan().getPerkuliahan();
		boolean obeKartu = perkuliahanKartu != null && perkuliahanKartu.getKurikulum() != null && perkuliahanKartu
				.getKurikulum().apakahObe(perkuliahanKartu.getTahunAjaran(), perkuliahanKartu.getGanjilGenap());

		MyToolbarbuttonConfig btnUbah = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
		btnUbah.setStyle(gayaAksi);
		btnUbah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UjianAction.onAddExternal(event, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(refresh, "Loading..", false, 1500);
					}
				}, ujian, ppu.getPertemuan() == null ? null : ppu.getPertemuan().untuk());
			}
		});
		btnUbah.setParent(grp);

		if (perkuliahanKartu != null) {
			final boolean obeSync = obeKartu;
			MyToolbarbuttonConfig btnSinkron = new MyToolbarbuttonConfig("Sinkronkan Nilai", "/img/Configure.gif");
			btnSinkron.setStyle(gayaAksi);
			btnSinkron.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (obeSync) {
						ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilaiObe(perkuliahanKartu,
								ppu.getFormatNilais());
					} else {
						if (ppu.getFormatNilai() == null || ppu.getFormatNilai().getStatusPertemuan() == null) {
							MyMessageboxConfig.show(
									"Nilai ujian belum dapat disinkronkan karena belum ada komponen penilaian tujuan. "
											+ "Klik tombol Pengaturan Data Ujian, pilih bagian 'Nilai masuk ke komponen penilaian', "
											+ "simpan, lalu klik Sinkronkan Nilai kembali.");
							return;
						}
						ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(perkuliahanKartu,
								ppu.getFormatNilai());
					}
					// Refresh setelah proses simpan/sinkron: muat ulang kartu agar ringkasan nilai terbarui.
					Common.createDefaultTimer(refresh, "Loading..", false, 1500);
				}
			});
			btnSinkron.setParent(grp);
		}

		MyToolbarbuttonConfig btnHapus = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		btnHapus.setStyle("background:#fef2f2;color:#b91c1c;border:1px solid #fecaca;border-radius:8px;"
				+ "padding:7px 12px;font-weight:700;font-size:11px;");
		btnHapus.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diperhatikan bahwa data yang telah dihapus tidak dapat dikembalikan. Silakan pilih OK untuk melanjutkan penghapusan atau Batal untuk membatalkan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
									return;
								}
								try {
									Session session = HibernateUtil.currentSession();
									session.createSQLQuery(
											"delete from hasil_ujian_mahasiswa_detail where hasil_ujian_mahasiswa in (select id from hasil_ujian_mahasiswa where pertemuan_punya_ujian = "
													+ ppu.getId() + ")")
											.executeUpdate();
									session.createSQLQuery(
											"delete from hasil_ujian_mahasiswa where pertemuan_punya_ujian = "
													+ ppu.getId())
											.executeUpdate();
									Common.refreshDelete(session, ppu);
									Common.createDefaultTimer(refresh, "Loading..", false, 1500);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									MyMessageboxConfig.show(Common.pesan(
				"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain di dalam sistem. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang terkait; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala masih berlanjut, hubungi Admin untuk bantuan lebih lanjut.",
				e.getMessage()));
								}
							}
						});
			}
		});
		btnHapus.setParent(grp);

		// Kelola Soal: buka pengelolaan soal ujian PENUH di MyWindow tersendiri (identik dengan
		// tombol di dalam modal Pengaturan). Hanya di kartu pengelola (buatKartuUjianRingkas
		// dipanggil khusus pengelola), jadi tidak muncul untuk mahasiswa/siswa/calon.
		MyToolbarbuttonConfig btnKelolaSoal = new MyToolbarbuttonConfig("Kelola Soal", "/img/svg/edit-box-line.svg");
		btnKelolaSoal.setStyle(gayaAksi);
		btnKelolaSoal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				boolean tampilMenuSoalDiManajemenUjian = Common.bolehKonfigurasi("tampil_menu_soal_di_manajemen_ujian");
				MyWindow win = new MyWindow();
				win.setTitle("Kelola Soal Ujian - " + (ujian == null ? "" : ujian.getNama()));
				win.setClosable(true);
				win.setWidth(Common.isMobile() ? "100%" : "95%");
				win.setHeight(Common.isMobile() ? "100%" : "95%");
				win.setContentStyle("overflow:auto;background:#fff;");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);
				new DetailUjianHelper().display(ujian, win, ppu.getPertemuan(), ppu, tampilMenuSoalDiManajemenUjian,
						false);
				// Saat jendela "Kelola Soal Ujian" DITUTUP (Simpan/Batal/X/Esc), muat ulang daftar ujian dari
				// DATABASE agar kartu di belakang mencerminkan perubahan (soal/anti-curang/dll tersimpan langsung).
				win.addEventListener("onClose", new EventListener() {
					@Override
					public void onEvent(Event evClose) throws Exception {
						try {
							if (refresh != null) {
								refresh.onEvent(evClose);
							}
						} catch (Exception exReload) {
							Common.tampilErrorJikaAdmin(exReload);
						}
					}
				});
				win.onModal();
			}
		});
		btnKelolaSoal.setParent(grp);

		// Gandakan: salin PPU + Ujian + semua UjianPunyaSoal + BankSoal + BankSoalDetail ke pertemuan yang sama.
		// Hasil salinan langsung non-aktif agar tidak langsung tampil ke mahasiswa sebelum diperiksa.
		MyToolbarbuttonConfig btnGandakan = new MyToolbarbuttonConfig("Gandakan", "/img/svg/copy.svg");
		btnGandakan.setStyle(gayaAksi);
		btnGandakan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show(
					"Apakah Bapak/Ibu yakin ingin menggandakan ujian ini beserta semua soalnya? "
					+ "Data baru akan dibuat sebagai salinan dengan status non-aktif. "
					+ "Silakan pilih OK untuk melanjutkan atau Batal untuk membatalkan.",
					"Konfirmasi Gandakan",
					MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
					MyMessageboxConfig.QUESTION,
					new EventListener() {
						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event ev) throws Exception {
							if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
								return;
							}
							try {
								Session session = HibernateUtil.currentSession();

								// 1. Salin Ujian
								Ujian ujianBaru = (Ujian) ujian.clone();
								ujianBaru.setId(null);
								ujianBaru.setNama(ujian.getNama() + " (salinan "
										+ Common.dateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + ")");
								session.save(ujianBaru);

								// 2. Salin semua UjianPunyaSoal beserta BankSoal dan BankSoalDetail
								List<UjianPunyaSoal> punyaSoals = session.createCriteria(UjianPunyaSoal.class)
										.add(Restrictions.eq("ujian", ujian)).list();
								for (UjianPunyaSoal ups : punyaSoals) {
									List<Long> detailIds = ups.getBankSoal().ambilBankSoalDetail(false);
									BankSoal bankSoalBaru = (BankSoal) ups.getBankSoal().clone();
									bankSoalBaru.setId(null);
									session.save(bankSoalBaru);

									for (Long detailId : detailIds) {
										BankSoalDetail bsd = (BankSoalDetail) GeneralValueObject
												.ambilData(BankSoalDetail.class, detailId.toString());
										if (bsd != null) {
											BankSoalDetail bsdBaru = (BankSoalDetail) bsd.clone();
											bsdBaru.setBankSoal(bankSoalBaru);
											bsdBaru.setKodeUnik(null);
											bsdBaru.setId(null);
											session.save(bsdBaru);
										}
									}
									session.flush();
									bankSoalBaru.reInitBankSoalDetail(session);

									UjianPunyaSoal upsBaru = new UjianPunyaSoal();
									upsBaru.setBankSoal(bankSoalBaru);
									upsBaru.setUjian(ujianBaru);
									upsBaru.setNomorUrut(ups.getNomorUrut());
									session.save(upsBaru);
								}
								session.flush();
								ujianBaru.reInitUjianPunyaSoal(session);

								// 3. Salin PertemuanPunyaUjian — tautkan ke Ujian baru, nonaktif dulu
								PertemuanPunyaUjian ppuBaru = (PertemuanPunyaUjian) ppu.clone();
								ppuBaru.setId(null);
								ppuBaru.setUjian(ujianBaru);
								ppuBaru.setAktif(false);
								session.save(ppuBaru);

								Common.createDefaultTimer(refresh, "Loading..", false, 1500);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								PesanFormalHelper.tampilkanGagalException(
										"menggandakan ujian beserta seluruh soalnya",
										e, new String[] {
												"Periksa apakah sebagian data (ujian/soal salinan) sempat tersimpan sebelum proses ini gagal, lalu hapus bila perlu untuk menghindari data ganda.",
												"Muat ulang (refresh) halaman ini lalu coba gandakan ujian kembali.",
												"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
										});
							}
						}
					});
			}
		});
		btnGandakan.setParent(grp);

		// Download Soal: unduh seluruh soal ujian ini ke file Excel agar bisa diedit offline.
		MyToolbarbuttonConfig btnDownloadSoal = new MyToolbarbuttonConfig("Download Soal", "/img/excel.png");
		btnDownloadSoal.setStyle(gayaAksi);
		btnDownloadSoal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					DetailUjianHelper.doDownload(ujian, ppu, null, false);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"mengunduh soal ujian ke Excel",
							e,
							new String[] {
									"Pastikan ujian ini memiliki soal yang sudah disimpan sebelum mengunduh.",
									"Coba muat ulang (refresh) halaman lalu klik Download Soal kembali.",
									"Jika masalah berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});
		btnDownloadSoal.setParent(grp);

		// Upload Soal: unggah soal dari file Excel hasil download/edit offline.
		// Format yang diterima: .xlsx (Excel Open XML). File .xls / .ods tidak didukung.
		MyToolbarbuttonConfig btnUploadSoal = new MyToolbarbuttonConfig(
				"Upload Soal" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		btnUploadSoal.setStyle(gayaAksi);
		btnUploadSoal.setUpload(Common.ukuranFileUpload());
		final PertemuanPunyaUjianHelper selfRef = this;
		btnUploadSoal.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent ue = (UploadEvent) event;
				Media media = ue.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) {
					return;
				}
				String nama = media.getName() == null ? "" : media.getName().toLowerCase();
				if (!nama.endsWith(".xlsx")) {
					PesanFormalHelper.tampilkanGagalException(
							"mengunggah soal ujian dari Excel",
							new RuntimeException("Format file tidak didukung: " + media.getName()),
							new String[] {
									"File yang diunggah harus berformat Excel Open XML (.xlsx).",
									"Buka file Excel Anda, lalu pilih 'Save As' → 'Excel Workbook (*.xlsx)', kemudian unggah kembali.",
									"Jangan gunakan format .xls (Excel lama), .ods, atau .csv — hanya .xlsx yang diterima.",
									"Jika sudah dalam format .xlsx tetapi tetap gagal, hubungi Admin dengan menyertakan tangkapan layar pesan ini."
							});
					return;
				}
				try {
					DetailUjianHelper.doUpload(media, ujian, selfRef, ppu);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"mengunggah soal ujian dari Excel",
							e,
							new String[] {
									"Pastikan file Excel yang diunggah adalah hasil Download Soal dari sistem ini (bukan file buatan sendiri dengan struktur berbeda).",
									"Periksa baris-baris data di file: kolom 'Soal' (kolom B) tidak boleh kosong pada setiap baris data.",
									"Kolom 'Benar' (kolom C) harus berisi nomor pilihan yang dipisah koma, mis. '1' atau '1,2'.",
									"Coba unduh ulang template soal via tombol 'Download Soal', isi ulang datanya, lalu unggah kembali.",
									"Jika kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});
		btnUploadSoal.setParent(grp);

		// Pasang button group ke kartu DAHULU, body info KEMUDIAN — tombol tampil di atas.
		foot.setParent(kartu);
		kartu.appendChild(ais.ui.util.DashboardUiKit.html(sb.toString()));
	}

	/**
	 * Menyusun satu "chip" ringkas untuk badan kartu ujian: label kecil di atas, nilai tebal di
	 * bawah, dibungkus {@code div.ppu-chip} yang diatur oleh {@link #GAYA_KARTU_UJIAN}. Ini adalah
	 * satuan tampilan terkecil dari kedua kartu — kartu pengelola
	 * ({@link #buatKartuUjianRingkas(PertemuanPunyaUjian, Tbmuser, EventListener)}) memakainya
	 * belasan kali (status pelaksanaan, jumlah peserta, progres, statistik nilai, chip per
	 * Sub-CPMK bila OBE, dan seterusnya), sedangkan kartu peserta
	 * ({@link #buatKartuUjianPeserta(PertemuanPunyaUjian, Tbmuser, EventListener)}) memakai
	 * subset yang jauh lebih sedikit.
	 *
	 * <p>Kedua argumen di-escape lewat {@code DashboardUiKit.esc(...)}. Konsekuensinya, nilai yang
	 * memang dimaksudkan mengandung markup TIDAK bisa dikirim lewat method ini — pemanggil yang
	 * membutuhkan markup (mis. blok penilaian) memakai {@link #penilaianBlok(String, String)} yang
	 * sengaja tidak meng-escape isinya.
	 *
	 * @param label nama informasi (baris atas, huruf kecil).
	 * @param value isi informasi (baris bawah, tebal); pemanggil sudah memformatnya menjadi teks
	 *              siap tampil, termasuk mengganti nilai kosong dengan tanda "-".
	 * @return potongan HTML satu chip.
	 */
	private String chip(String label, String value) {
		return "<div class='ppu-chip'><b>" + ais.ui.util.DashboardUiKit.esc(label) + "</b><span>"
				+ ais.ui.util.DashboardUiKit.esc(value) + "</span></div>";
	}

	/**
	 * Menyusun blok <b>"Ketentuan Ujian"</b> untuk kartu ujian dalam bahasa yang sederhana
	 * dan mudah dipahami peserta ujian (mahasiswa/siswa). Blok ini merangkum seluruh
	 * pengaturan penting yang tadinya hanya terlihat di modal <i>Pengaturan Data Ujian</i>
	 * (dibatasi waktu, waktu per soal/seluruh soal, pengacakan urutan soal, boleh/tidak
	 * kembali ke soal sebelumnya, lanjut-otomatis saat koneksi terputus, ujian hanya
	 * muncul dalam rentang waktu, serta apakah jawaban dan nilai bisa dilihat setelah
	 * ujian) menjadi daftar kalimat ramah-peserta. Di bagian akhir dipanggil
	 * {@link #buatPenilaianUjianHtml(PertemuanPunyaUjian)} untuk menjelaskan bagaimana
	 * nilai ujian dihitung (komponen penilaian / capaian Sub-CPMK bila kurikulum OBE).
	 *
	 * <p>Setiap butir diberi ikon status: centang hijau untuk hal yang berlaku/mendukung,
	 * tanda seru jingga untuk pembatasan yang perlu diperhatikan peserta, dan silang merah
	 * untuk hal yang tidak tersedia. Tujuannya agar peserta membaca sekali dan langsung
	 * paham aturan main ujian tanpa istilah teknis.
	 *
	 * @param ppu data ujian pada pertemuan yang akan dirangkum ketentuannya.
	 * @return potongan HTML blok ketentuan (siap disisipkan ke dalam kartu ujian).
	 */
	private String buatKetentuanUjianHtml(PertemuanPunyaUjian ppu) {
		if (ppu == null) {
			return "";
		}
		boolean dibatasi = Boolean.TRUE.equals(ppu.getDibatasiWaktu());
		StringBuffer sb = new StringBuffer();
		sb.append("<div class='ppu-ket'>");
		sb.append("<div class='ppu-ket-judul'>Ketentuan Ujian</div>");
		sb.append("<ul class='ppu-ket-list'>");

		if (dibatasi) {
			String durasi = ppu.getLama() == null ? "" : (" (" + Common.timeFormat1.get().format(ppu.getLama()) + ")");
			sb.append(ketItem("warn", "Ujian dibatasi waktu" + durasi + ". Kerjakan sebelum waktu habis."));
			if (Boolean.TRUE.equals(ppu.getTiapSoal())) {
				sb.append(ketItem("warn", "Batas waktu dihitung untuk SETIAP soal."));
			} else {
				sb.append(ketItem("ok", "Batas waktu berlaku untuk seluruh soal (bebas mengatur waktu antar soal)."));
			}
		} else {
			sb.append(ketItem("ok", "Ujian tidak dibatasi waktu."));
		}

		if (Boolean.TRUE.equals(ppu.getRandom())) {
			sb.append(ketItem("warn", "Urutan nomor soal diacak — bisa berbeda antar peserta."));
		} else {
			sb.append(ketItem("ok", "Urutan nomor soal sama untuk semua peserta."));
		}

		if (Boolean.TRUE.equals(ppu.getTidakDiaktifkanTombolKembali())) {
			sb.append(ketItem("warn", "Tidak bisa kembali ke soal sebelumnya. Pastikan jawaban sebelum lanjut."));
		} else {
			sb.append(ketItem("ok", "Boleh kembali dan memeriksa ulang soal sebelumnya."));
		}

		if (Boolean.TRUE.equals(ppu.getOtomatisMunculKetikaBelumSelesai())) {
			sb.append(ketItem("ok", "Jika koneksi/HP bermasalah, ujian otomatis dilanjutkan saat login ulang "
					+ "(sisa waktu tetap berjalan)."));
		}

		if (Boolean.TRUE.equals(ppu.getTidakDitampilkanJikaWaktuSudahTerlewat())) {
			sb.append(ketItem("warn", "Ujian hanya bisa diakses dalam rentang waktu pelaksanaan."));
		}

		if (Boolean.TRUE.equals(ppu.getLihatJawabanSetelahUjian())) {
			sb.append(ketItem("ok", "Jawaban yang benar bisa dilihat setelah ujian selesai."));
		} else {
			sb.append(ketItem("no", "Jawaban yang benar tidak ditampilkan setelah ujian."));
		}

		if (Boolean.TRUE.equals(ppu.getLihatNilaiSetelahUjian())) {
			sb.append(ketItem("ok", "Nilai bisa dilihat langsung setelah ujian selesai."));
		} else {
			sb.append(ketItem("no", "Nilai tidak ditampilkan langsung setelah ujian."));
		}

		// Status Anti-Curang (CBT) PER-UJIAN — beri tahu peserta apakah pengawasan aktif.
		if (Boolean.TRUE.equals(ppu.getAntiCurangAktif())) {
			sb.append(ketItem("warn", "Mode Anti-Curang AKTIF: layar penuh, deteksi berpindah tab/jendela, "
					+ "blokir klik kanan & shortcut diberlakukan selama ujian."));
		} else {
			sb.append(ketItem("ok", "Mode Anti-Curang tidak aktif untuk ujian ini."));
		}

		sb.append("</ul>");
		sb.append(buatPenilaianUjianHtml(ppu));
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Menyusun satu butir daftar pada blok "Ketentuan Ujian", didahului ikon status berwarna.
	 * Dipakai eksklusif oleh {@link #buatKetentuanUjianHtml(PertemuanPunyaUjian)}.
	 *
	 * <p>Pemetaan {@code tipe} ke ikon dan warna:</p>
	 * <ul>
	 * <li>{@code "no"} → silang merah ({@code #dc2626}) — sesuatu yang TIDAK tersedia bagi peserta
	 * (mis. jawaban benar tidak ditampilkan setelah ujian).</li>
	 * <li>{@code "warn"} → tanda seru jingga ({@code #d97706}) — pembatasan yang perlu
	 * diperhatikan peserta (mis. ujian dibatasi waktu, tidak boleh kembali ke soal sebelumnya,
	 * mode anti-curang aktif).</li>
	 * <li>nilai lain apa pun, termasuk {@code "ok"} dan {@code null} → centang hijau
	 * ({@code #059669}). Perhatikan bahwa cabang hijau adalah cabang {@code else} tanpa validasi,
	 * jadi salah ketik pada {@code tipe} akan diam-diam menghasilkan butir hijau, bukan error.</li>
	 * </ul>
	 *
	 * <p>Hanya {@code teks} yang di-escape; {@code tipe} tidak pernah masuk ke keluaran sebagai
	 * teks (hanya memilih ikon dan warna dari konstanta di dalam method), sehingga tidak menjadi
	 * jalur penyisipan markup.
	 *
	 * @param tipe kode status: {@code "no"}, {@code "warn"}, atau apa pun untuk hijau.
	 * @param teks kalimat ketentuan dalam bahasa ramah-peserta, akan di-escape.
	 * @return potongan HTML satu elemen {@code <li>} lengkap dengan ikonnya.
	 */
	private String ketItem(String tipe, String teks) {
		String ic;
		String warna;
		if ("no".equals(tipe)) {
			ic = "✗"; // ✗
			warna = "#dc2626";
		} else if ("warn".equals(tipe)) {
			ic = "!";
			warna = "#d97706";
		} else {
			ic = "✓"; // ✓
			warna = "#059669";
		}
		return "<li><span style='display:inline-block;width:16px;text-align:center;color:" + warna
				+ ";font-weight:900;'>" + ic + "</span>" + ais.ui.util.DashboardUiKit.esc(teks) + "</li>";
	}

	/**
	 * Menjelaskan <b>bagaimana nilai ujian dihitung</b> dalam bahasa sederhana untuk peserta.
	 * Untuk kurikulum non-OBE: menyebut berapa persen bobot ujian ini terhadap komponen
	 * penilaian tujuannya. Untuk kurikulum OBE: menyebut capaian (Sub-CPMK) yang diukur ujian
	 * ini beserta bobot persen dan banyaknya soal untuk tiap capaian — tanpa menampilkan
	 * deretan nomor soal yang teknis. Aman terhadap {@code null} dan kegagalan sesi/parse.
	 *
	 * @param ppu data ujian pada pertemuan.
	 * @return potongan HTML blok penilaian, atau string kosong bila tidak ada info penilaian.
	 */
	private String buatPenilaianUjianHtml(PertemuanPunyaUjian ppu) {
		if (ppu == null || ppu.getPertemuan() == null || ppu.getPertemuan().getPerkuliahan() == null) {
			return "";
		}
		Perkuliahan perkuliahan = ppu.getPertemuan().getPerkuliahan();
		boolean obe = perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
				.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap());

		if (!obe) {
			if (ppu.getFormatNilai() != null && ppu.getFormatNilai().getStatusPertemuan() != null) {
				String isi = "Nilai ujian ini menyumbang <b>" + fmtPersen(ppu.getProsentase())
						+ "%</b> pada komponen penilaian \"" + ais.ui.util.DashboardUiKit.esc(ppu.getFormatNilai().getNama())
						+ "\".";
				return penilaianBlok("Cara Nilai Dihitung", isi);
			}
			return "";
		}

		// OBE: rangkai daftar Sub-CPMK yang dinilai + bobot + jumlah soal.
		StringBuffer li = new StringBuffer();
		try {
			org.hibernate.Session session = ais.database.hibernate.HibernateUtil.currentSession();
			// refresh=true wajib: flag udah("format_nilai_baru") mungkin sudah di-set oleh
			// "Hitung Ulang Semua" sehingga getFormatNilais(refresh=false) melewati
			// setDefaultPembobotan → FormatNilai.statusPertemuan=null → semua sub-CPMK
			// terlewat di loop bawah → bagian CAPAIAN kosong.
			java.util.List<FormatNilai> formatNilais = perkuliahan.ambilFormatNilai(session, true);
			org.json.JSONObject json = new org.json.JSONObject(
					ppu.getFormatNilais() == null ? "{}" : ppu.getFormatNilais());
			if (formatNilais != null) {
				for (FormatNilai fn : formatNilais) {
					if (fn == null || fn.getStatusPertemuan() == null || fn.getId() == null) {
						continue;
					}
					String key = fn.getId().toString();
					if (json.isNull(key)) {
						continue;
					}
					int jml = hitungJumlahNomor(json.get(key) + "");
					li.append("<li><b>").append(ais.ui.util.DashboardUiKit.esc(fn.getNama())).append("</b> (")
							.append(fmtPersen(fn.getPersen())).append("%) — ").append(jml).append(" soal</li>");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		if (li.length() == 0) {
			return "";
		}
		return penilaianBlok("Capaian Pembelajaran yang Dinilai",
				"Ujian ini mengukur ketercapaian kemampuan berikut:<ul class='ppu-ket-list' style='margin-top:4px;'>"
						+ li + "</ul>");
	}

	/**
	 * Membungkus satu blok informasi penilaian bergaya kartu-info (judul kecil di atas, isi di
	 * bawah) memakai kelas CSS {@code ppu-pnl} dari {@link #GAYA_KARTU_UJIAN}. Dipakai eksklusif
	 * oleh {@link #buatPenilaianUjianHtml(PertemuanPunyaUjian)} untuk kedua bentuk keluarannya:
	 * "Cara Nilai Dihitung" (kurikulum non-OBE) dan "Capaian Pembelajaran yang Dinilai"
	 * (kurikulum OBE).
	 *
	 * <p><b>Perhatian saat memakai ulang:</b> {@code judul} di-escape, tetapi {@code isiHtml}
	 * <b>sengaja TIDAK di-escape</b> — parameter itu memang menerima markup (tag {@code <b>},
	 * daftar {@code <ul>/<li>}) yang dirakit pemanggil. Karena itu setiap nilai yang berasal dari
	 * data (nama komponen penilaian, nama Sub-CPMK) HARUS sudah di-escape oleh pemanggil sebelum
	 * disambungkan ke {@code isiHtml}; {@link #buatPenilaianUjianHtml(PertemuanPunyaUjian)} sudah
	 * melakukannya lewat {@code DashboardUiKit.esc(...)}. Jangan mengirim string data mentah ke
	 * parameter ini.
	 *
	 * @param judul   judul blok, akan di-escape.
	 * @param isiHtml isi blok berupa HTML jadi; TIDAK di-escape (lihat peringatan di atas).
	 * @return potongan HTML satu blok penilaian.
	 */
	private String penilaianBlok(String judul, String isiHtml) {
		return "<div class='ppu-pnl'><div class='ppu-pnl-judul'>" + ais.ui.util.DashboardUiKit.esc(judul)
				+ "</div><div class='ppu-pnl-isi'>" + isiHtml + "</div></div>";
	}

	/**
	 * Memformat sebuah angka persen menjadi teks siap tampil dengan penanganan {@code null} yang
	 * aman. Memakai {@code Common.numberFormat} (format angka global aplikasi, mengikuti locale),
	 * sehingga {@code 15.0} menjadi {@code "15"} dan bukan {@code "15.0"}.
	 *
	 * <p>Nilai {@code null} dipetakan ke {@code "0"} — bukan ke tanda hubung atau string kosong —
	 * karena keluarannya selalu langsung disambung dengan tanda {@code %} pada kalimat penjelasan
	 * di {@link #buatPenilaianUjianHtml(PertemuanPunyaUjian)}; menampilkan "0%" lebih masuk akal
	 * bagi peserta daripada "-%". Konsekuensinya, method ini TIDAK bisa membedakan bobot yang
	 * memang bernilai nol dari bobot yang belum pernah diisi.
	 *
	 * @param d nilai persen, boleh {@code null}.
	 * @return teks angka persen tanpa tanda {@code %}, atau {@code "0"} bila {@code d} null.
	 */
	private String fmtPersen(Double d) {
		return d == null ? "0" : Common.numberFormat.get().format(d);
	}

	/**
	 * Menghitung banyaknya nomor soal dari string dipisah koma, mendukung RENTANG dengan tanda "-"
	 * (mis. "1-10,15,20-25" → 10+1+6 = 17). Nomor unik (overlap tidak dihitung ganda). Selaras dengan
	 * {@link PertemuanPunyaUjian#ambilMapNomor(java.util.List)} yang juga memuai rentang saat scoring.
	 *
	 * <p>Dipakai oleh {@link #buatPenilaianUjianHtml(PertemuanPunyaUjian)} untuk memberi tahu
	 * peserta BERAPA BANYAK soal yang mengukur tiap Sub-CPMK, tanpa membeberkan deretan nomor
	 * soalnya yang bersifat teknis. Sumber datanya adalah nilai JSON pada
	 * {@code ppu.getFormatNilais()} dengan kunci id {@link FormatNilai} — string yang sama yang
	 * diisi lewat kolom "Nomor Soal" atau lewat "Format Cepat" ({@code 1-10 sub cpmk 2}) di modal
	 * {@link #bukaPengaturanUjian(PertemuanPunyaUjian, EventListener)}.
	 *
	 * <p><b>Toleransi masukan.</b> Method ini sengaja tidak pernah melempar exception: token
	 * kosong dilewati, token yang bukan angka atau rentang yang tidak berbentuk {@code a-b}
	 * diabaikan diam-diam, dan rentang terbalik ({@code "10-1"}) tetap dihitung benar karena
	 * batasnya dinormalisasi lewat {@code Math.min}/{@code Math.max}. Karena hasilnya adalah
	 * ukuran sebuah {@link java.util.Set}, nomor yang ditulis dua kali — baik langsung
	 * ({@code "3,3"}) maupun lewat rentang yang tumpang tindih ({@code "1-5,4-8"}) — hanya
	 * dihitung sekali. Angka negatif dan nol tidak ditolak; keduanya ikut terhitung bila ditulis.
	 *
	 * @param nomor daftar nomor soal dipisah koma, boleh berisi rentang; boleh {@code null}.
	 * @return banyaknya nomor soal unik yang bisa diurai; {@code 0} bila {@code nomor} null,
	 *         kosong, atau seluruh tokennya tidak valid.
	 */
	private int hitungJumlahNomor(String nomor) {
		if (nomor == null) {
			return 0;
		}
		java.util.Set<Integer> set = new java.util.HashSet<Integer>();
		for (String b : nomor.split(",")) {
			if (b == null) {
				continue;
			}
			b = b.trim();
			if (b.isEmpty()) {
				continue;
			}
			try {
				if (b.contains("-")) {
					String[] r = b.split("-");
					if (r.length == 2) {
						int s = Integer.parseInt(r[0].trim());
						int e = Integer.parseInt(r[1].trim());
						for (int i = Math.min(s, e); i <= Math.max(s, e); i++) {
							set.add(i);
						}
					}
				} else {
					set.add(Integer.parseInt(b));
				}
			} catch (Exception ig) { /* token tak valid diabaikan */ }
		}
		return set.size();
	}

	/**
	 * Membangun HTML ringkasan bobot inline untuk kolom bobot Sub-CPMK di modal Pengaturan Data Ujian.
	 * Menampilkan kontribusi bobot dari PPU lain dalam perkuliahan yang sama, dan total gabungan.
	 * Warna: hijau jika total ≈100%, merah jika &gt;100%, abu-abu jika di bawah.
	 * Dipanggil statik agar bisa digunakan dari inner class {@link DetailPertemuanRenderer}.
	 *
	 * <p><b>Masalah yang dijawab.</b> Satu Sub-CPMK biasanya diukur oleh BEBERAPA ujian pada
	 * perkuliahan yang sama, dan tiap ujian menyimpan bobotnya sendiri di JSON
	 * {@code formatNilais} dengan kunci {@code "<idFormatNilai>_bobot"}. Tanpa ringkasan ini,
	 * dosen yang mengatur bobot pada satu ujian tidak punya cara melihat berapa yang sudah
	 * dialokasikan ujian lain, sehingga total lintas-ujian gampang melenceng dari 100%. Method ini
	 * menghitung dan menampilkannya tepat di bawah kotak bobot yang sedang diedit.
	 *
	 * <p><b>Cara menghitung.</b> Mengambil SELURUH {@link PertemuanPunyaUjian} yang pertemuannya
	 * bernaung pada {@code perkuliahan} yang sama (satu criteria dengan alias {@code pertemuan}),
	 * melewati {@link PertemuanPunyaUjian} yang sedang diedit berdasarkan kesamaan id, lalu untuk
	 * setiap sisanya mem-parse {@code formatNilais}: bila {@code fn} memang di-assign di ujian itu,
	 * bobotnya ditambahkan — memakai {@code 100.0} sebagai default bila kunci {@code _bobot} belum
	 * ada, konsisten dengan default yang dipakai editor bobot dan
	 * {@link #parameter(PertemuanPunyaUjian, KurikulumPunyaMatakuliah)}. Totalnya adalah
	 * {@code currentBobot + bobotLain}; ambang warnanya toleran terhadap pembulatan
	 * ({@code >100.5} merah, {@code >=99.5} hijau, selain itu abu-abu).
	 *
	 * <p><b>Catatan biaya dan kegagalan.</b> Query dijalankan tanpa proyeksi dan tanpa batas
	 * jumlah, lalu diulang untuk SETIAP baris Sub-CPMK yang tercentang saat modal dirender —
	 * artinya perkuliahan dengan banyak ujian dan banyak Sub-CPMK menghasilkan pengambilan
	 * berulang atas himpunan baris yang sama. Seluruh badan method dibungkus {@code try/catch}
	 * yang mencatat error lewat {@code ErrorAuditUtil} lalu mengembalikan {@code null}; JSON
	 * {@code formatNilais} yang rusak pada ujian lain juga diabaikan per-baris. Ringkasan yang
	 * gagal dihitung karena itu hanya membuat baris info tidak muncul — tidak pernah menggagalkan
	 * render modal pengaturan.
	 *
	 * @param ppu          ujian yang sedang diedit; dikecualikan dari penjumlahan bobot "lainnya".
	 * @param fn           komponen penilaian (Sub-CPMK) yang barisnya sedang dirender.
	 * @param currentBobot bobot yang sedang tampil di kotak input untuk {@code fn} pada {@code ppu}.
	 * @param session      sesi Hibernate aktif milik pemanggil (tidak dibuka maupun ditutup di sini).
	 * @param perkuliahan  perkuliahan pemilik pertemuan, sebagai cakupan pencarian ujian lain.
	 * @return potongan HTML satu baris ringkasan bobot, atau {@code null} bila argumen wajib
	 *         {@code null}/tanpa id, atau bila terjadi error saat menghitung.
	 */
	@SuppressWarnings("unchecked")
	private static String buildInfoBobotInline(PertemuanPunyaUjian ppu, FormatNilai fn, double currentBobot,
			Session session, Perkuliahan perkuliahan) {
		if (ppu == null || fn == null || fn.getId() == null || perkuliahan == null || perkuliahan.getId() == null) {
			return null;
		}
		try {
			final String fnId = fn.getId().toString();
			final String bobotKey = fnId + "_bobot";
			double bobotLain = 0.0;
			java.util.List<PertemuanPunyaUjian> others = session.createCriteria(PertemuanPunyaUjian.class)
				.createAlias("pertemuan", "pt")
				.add(Restrictions.eq("pt.perkuliahan", perkuliahan))
				.list();
			for (PertemuanPunyaUjian other : others) {
				if (ppu.getId() != null && ppu.getId().equals(other.getId())) continue;
				String fnsStr = other.getFormatNilais();
				if (fnsStr == null || fnsStr.trim().isEmpty()) continue;
				try {
					JSONObject j = new JSONObject(fnsStr);
					if (!j.isNull(fnId)) {
						double bobot = j.isNull(bobotKey) ? 100.0 : j.optDouble(bobotKey, 100.0);
						bobotLain += bobot;
					}
				} catch (Exception ej) { /* abaikan parsing error */ }
			}
			double total = currentBobot + bobotLain;
			String warna = total > 100.5 ? "#dc2626" : (total >= 99.5 ? "#16a34a" : "#64748b");
			String lainStr = String.format("%.0f", Double.valueOf(bobotLain));
			String totalStr = String.format("%.0f", Double.valueOf(total));
			return "<div style='font-size:11px;color:#64748b;margin-top:2px;'>Lainnya: " + lainStr
				+ "% · Total: <span style='color:" + warna + ";font-weight:700;'>" + totalStr + "%</span></div>";
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
				"auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:buildInfoBobotInline");
			return null;
		}
	}

	/**
	 * Membuat label penjelas kecil untuk sebuah field pada modal <b>Pengaturan Data Ujian</b>.
	 * Digunakan agar setiap kontrol (durasi, jadwal mulai/selesai, komponen nilai, dsb.) punya
	 * keterangan yang jelas dan mudah dipahami, bukan sekadar kotak input tanpa nama. Dibuat
	 * {@code static} agar bisa dipakai ulang dari {@link DetailPertemuanRenderer}.
	 *
	 * @param teks teks keterangan yang ditampilkan di atas/atau di dekat field.
	 * @return {@link org.zkoss.zul.Label} bergaya seragam (tebal, kecil, abu-abu) siap dipasang.
	 */
	private static org.zkoss.zul.Label labelFieldUjian(String teks) {
		org.zkoss.zul.Label l = new org.zkoss.zul.Label(teks);
		l.setStyle("font-weight:700;color:#334155;font-size:10px;display:block;margin-top:6px;");
		return l;
	}

	/**
	 * Membuka modal <b>Pengaturan Data Ujian</b> untuk satu {@link PertemuanPunyaUjian}.
	 * Modal berisi sebuah {@link MyGrid} 1-baris yang dirender oleh
	 * {@link DetailPertemuanRenderer} lama (tanpa perubahan) sehingga SEMUA kontrol
	 * pengaturan (jumlah soal, batas ikut, dibatasi waktu, durasi, tanggal mulai/selesai,
	 * acak, lihat jawaban/nilai, format nilai/Sub-CPMK, tombol Ubah/Hapus/Preview/Hasil,
	 * pindah pertemuan, dsb.) tetap berfungsi identik. CSS {@code ppu-modal-grid}
	 * menumpuk sel secara vertikal agar tampil sebagai formulir, bukan baris tabel lebar.
	 *
	 * <p><b>Inilah satu-satunya {@link MyGrid} yang benar-benar hidup di file ini.</b> Grid
	 * dibuat sebagai variabel LOKAL bernama {@code gridModal}, kolomnya dibangun oleh
	 * {@link #buatKolomUjian(MyGrid, Tbmuser)} (fungsi yang sama yang dulu dipakai tampilan tabel
	 * penuh), lalu modelnya diisi {@link SimpleListModel} berisi TEPAT SATU elemen yaitu
	 * {@code ppu}. Dengan begitu {@link DetailPertemuanRenderer} lama dipakai ulang apa adanya:
	 * seluruh kontrol edit inline beserta autosave per field-nya tetap berfungsi tanpa satu baris
	 * pun ditulis ulang. Field instance {@link #grid} TIDAK dipakai di sini dan tetap
	 * {@code null} — lihat kuirk pada Javadoc kelas.
	 *
	 * <p><b>Renderer dibangun dengan identitas peserta milik helper ini</b>
	 * ({@link #mahasiswa}, {@link #biodataCalonMahasiswa}) dan {@code tampilInfo = false},
	 * sehingga modal selalu merender varian kontrol admin penuh, bukan blok info read-only.
	 * Perhatikan bahwa method ini sendiri bersifat {@code public} dan TIDAK memeriksa peran:
	 * penjagaannya sepenuhnya berada pada pemanggil — tombol "Pengaturan Data Ujian" di
	 * {@link #buatKartuUjianRingkas(PertemuanPunyaUjian, Tbmuser, EventListener)} (yang hanya
	 * dibuat pada cabang pengelola {@link #loadData(Object)}) dan {@code HasilUjianMahasiswaHelper}.
	 *
	 * <p><b>Tiga jalan keluar, semuanya memuat ulang.</b> Karena setiap perubahan sudah tersimpan
	 * ke database saat field disentuh, tidak ada aksi "simpan" yang sesungguhnya; yang dibutuhkan
	 * hanyalah menyegarkan kartu di belakang modal. Karena itu ketiga jalan keluar melakukan hal
	 * yang sama — memicu {@code refresh} lalu menutup:
	 * <ul>
	 * <li><b>{@code onClose}</b> (tombol X atau tombol Esc) — memicu {@code refresh} di dalam
	 * {@code try/catch} agar kegagalan muat ulang tidak menahan modal tetap terbuka.</li>
	 * <li><b>Tombol "Simpan"</b> — hanya memicu {@code refresh} lalu {@code window.detach()}.
	 * Namanya "Simpan" demi kejelasan bagi pengguna, bukan karena ada yang disimpan di sini.</li>
	 * <li><b>Tombol "Batal"</b> — <b>perilakunya IDENTIK dengan "Simpan"</b>: juga memicu
	 * {@code refresh} lalu menutup. "Batal" TIDAK membatalkan apa pun dan tidak bisa membatalkan
	 * apa pun, karena perubahan sudah tertulis ke database sejak field diubah. Jangan menambahkan
	 * pembatalan di sini tanpa lebih dulu mengubah pola autosave di
	 * {@link DetailPertemuanRenderer#render(Row, Object)}.</li>
	 * </ul>
	 * Footer aksi ini sengaja dibuat sendiri (bukan footer bawaan tema) dan dipasang
	 * {@code position:sticky} agar selalu terlihat, karena footer bawaan tidak terhubung ke
	 * listener {@code refresh}.
	 *
	 * @param ppu     ujian pada pertemuan yang akan diatur; menjadi satu-satunya elemen model grid
	 *                modal.
	 * @param refresh listener pemuat ulang daftar kartu, dipicu pada ketiga jalan keluar modal dan
	 *                juga diteruskan ke {@link DetailPertemuanRenderer} sebagai callback aksi
	 *                (pindah pertemuan, hapus, penutupan window Kelola Soal). Boleh {@code null};
	 *                setiap titik pemakaian sudah menjaganya.
	 * @throws Exception diteruskan dari pembangunan komponen ZK dan dari renderer.
	 */
	public void bukaPengaturanUjian(final PertemuanPunyaUjian ppu, final EventListener refresh) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();

		final Window window = new Window("Pengaturan Data Ujian", "normal", true);
		window.setClosable(true);
		window.setSizable(true);
		window.setMaximizable(true);
		window.setWidth(Common.isMobile() ? "100%" : "90%");
		window.setHeight(Common.isMobile() ? "100%" : "95%");
		window.setContentStyle("overflow:auto;height:100%;background:#fff;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Div wrap = new Div();
		wrap.setStyle("padding:6px 10px 12px 10px;");
		wrap.setParent(window);

		MyGrid gridModal = new MyGrid();
		gridModal.setWidth("100%");
		gridModal.setSclass("ppu-modal-grid");
		gridModal.setParent(wrap);
		buatKolomUjian(gridModal, tbmuser);

		// Listener refresh gabungan: muat ulang kartu daftar + tutup modal bila diminta.
		EventListener refreshDanTutup = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (refresh != null) {
					refresh.onEvent(arg0);
				}
			}
		};

		List<PertemuanPunyaUjian> satu = new ArrayList<PertemuanPunyaUjian>();
		satu.add(ppu);
		gridModal.setRowRenderer(new DetailPertemuanRenderer(mahasiswa, biodataCalonMahasiswa, refreshDanTutup, false));
		gridModal.setModel(new SimpleListModel(satu));

		// ENHANCE: saat modal "Pengaturan Data Ujian" DITUTUP (tombol X / Esc), muat ulang kartu
		// daftar secara OTOMATIS. Field pada modal (mis. tanggal Pelaksanaan, durasi) tersimpan
		// langsung saat diubah, tetapi kartu di belakang masih menampilkan data lama sampai ditekan
		// "Refresh". Dengan memanggil listener refresh pada onClose, data kartu langsung diperbarui
		// begitu modal ditutup tanpa perlu klik "Refresh" manual.
		window.addEventListener("onClose", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (refresh != null) {
					try {
						refresh.onEvent(arg0);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		});

		// Footer aksi EKSPLISIT (permintaan user: "klik Simpan → daftar ujian ter-Refresh otomatis").
		// SEMUA pengaturan sudah TERSIMPAN LANGSUNG saat field diubah, jadi tombol ini bertugas MEMUAT
		// ULANG daftar/kartu ujian dari DB lalu menutup modal. Dibuat sendiri agar PASTI berfungsi (tidak
		// bergantung tombol footer bawaan tema yang tak terhubung ke listener refresh).
		Div footerAksiModal = new Div();
		footerAksiModal.setStyle("position:sticky;bottom:0;left:0;right:0;background:#fff;"
				+ "border-top:1px solid #e2e8f0;padding:10px 6px;display:flex;gap:10px;"
				+ "justify-content:flex-end;z-index:5;");
		footerAksiModal.setParent(wrap);

		MyToolbarbuttonConfig btnBatalModal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		btnBatalModal.setStyle("background:#dc2626;color:#fff;border:none;border-radius:8px;"
				+ "padding:8px 24px;font-weight:700;cursor:pointer;");
		btnBatalModal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				if (refresh != null) {
					try { refresh.onEvent(ev); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
				}
				window.detach();
			}
		});
		btnBatalModal.setParent(footerAksiModal);

		MyToolbarbuttonConfig btnSimpanModal = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		btnSimpanModal.setStyle("background:#16a34a;color:#fff;border:none;border-radius:8px;"
				+ "padding:8px 34px;font-weight:700;cursor:pointer;");
		btnSimpanModal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				// Refresh data langsung dari DB (loadData(true) hapus cache PPU), lalu tutup modal.
				if (refresh != null) {
					try { refresh.onEvent(ev); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
				}
				window.detach();
			}
		});
		btnSimpanModal.setParent(footerAksiModal);

		window.onModal();
	}

	/**
	 * Satu blok {@code <style>} berisi SELURUH gaya CSS tampilan kartu Daftar Ujian Pertemuan
	 * beserta modal pengaturannya. Disisipkan sekali per pemanggilan
	 * {@link #display(Pertemuan, Component)}, tepat sebelum {@link #kartuWrap} dibuat, lewat
	 * {@code DashboardUiKit.html(...)} — bukan lewat berkas {@code .css} terpisah, agar helper ini
	 * bisa dipasang di halaman ZUL mana pun tanpa pemanggil perlu mendaftarkan stylesheet.
	 *
	 * <p><b>Kelas yang didefinisikan di sini</b> dan dipakai oleh perakit HTML di file ini:
	 * {@code ppu-kartu-wrap} (grid responsif: 1 kolom di HP, 2 kolom di layar lebar),
	 * {@code ppu-kartu} beserta {@code ppu-kartu-head}/{@code ppu-kartu-nama}/{@code ppu-badge}/
	 * {@code ppu-aktif-on}/{@code ppu-aktif-off} (kepala kartu), {@code ppu-kartu-body} dan
	 * {@code ppu-chip} (badan kartu — lihat {@link #chip(String, String)}), {@code ppu-ket},
	 * {@code ppu-ket-judul} dan {@code ppu-ket-list} (blok Ketentuan Ujian — lihat
	 * {@link #buatKetentuanUjianHtml(PertemuanPunyaUjian)}), {@code ppu-pnl},
	 * {@code ppu-pnl-judul} dan {@code ppu-pnl-isi} (blok penilaian — lihat
	 * {@link #penilaianBlok(String, String)}), {@code ppu-kartu-foot} dengan varian
	 * {@code ppu-kartu-foot-peserta} serta {@code ppu-gbtngrp} (baris tombol aksi), dan terakhir
	 * {@code ppu-modal-grid} plus {@code ppu-subcpmk-grid} untuk modal
	 * {@link #bukaPengaturanUjian(PertemuanPunyaUjian, EventListener)}.
	 *
	 * <p><b>Mengapa aturan modal memakai {@code !important}.</b> Blok {@code ppu-modal-grid}
	 * memaksa sel-sel {@link MyGrid} menumpuk vertikal sehingga satu baris grid tampil sebagai
	 * FORMULIR, bukan baris tabel yang sangat lebar; sedangkan {@code ppu-subcpmk-grid} justru
	 * mengembalikan grid Sub-CPMK di dalamnya ke perilaku tabel. Keduanya harus mengalahkan gaya
	 * bawaan tema ZK yang diterapkan langsung pada kelas {@code z-row}/{@code z-cell}, karena itu
	 * memakai {@code display:...!important}. Menghapus penanda tersebut membuat modal pengaturan
	 * kembali melebar dan tidak terbaca di layar sempit.
	 *
	 * <p>Karena isinya string statis dan tidak pernah menyisipkan data pengguna, konstanta ini
	 * tidak punya jalur penyisipan markup.
	 */
	private static final String GAYA_KARTU_UJIAN = "<style>"
			+ ".ppu-kartu-wrap{display:grid;grid-template-columns:1fr;gap:16px;padding:6px 2px 10px 2px;}"
			+ "@media(min-width:900px){.ppu-kartu-wrap{grid-template-columns:1fr 1fr;}}"
			+ ".ppu-kartu{border:1px solid #e2e8f0;border-radius:16px;background:#fff;overflow:hidden;"
			+ "box-shadow:0 10px 26px rgba(15,23,42,.07);display:flex;flex-direction:column;min-width:0;}"
			+ ".ppu-kartu-head{padding:13px 15px;background:linear-gradient(135deg,#1e3a8a,#2563eb);color:#fff;"
			+ "display:flex;align-items:flex-start;justify-content:space-between;gap:10px;}"
			+ ".ppu-kartu-nama{font-weight:800;font-size:13px;line-height:1.35;word-break:break-word;}"
			+ ".ppu-badge{display:inline-block;margin-top:5px;padding:2px 9px;border-radius:999px;"
			+ "background:rgba(255,255,255,.18);color:#fff;font-size:10px;font-weight:700;}"
			+ ".ppu-aktif{font-size:10px;font-weight:800;padding:3px 10px;border-radius:999px;white-space:nowrap;}"
			+ ".ppu-aktif-on{background:#dcfce7;color:#166534;}.ppu-aktif-off{background:#fee2e2;color:#991b1b;}"
			+ ".ppu-kartu-body{padding:12px 14px;display:grid;grid-template-columns:1fr 1fr;gap:8px;}"
			+ ".ppu-chip{display:flex;flex-direction:column;padding:7px 11px;border-radius:11px;background:#f8fafc;"
			+ "border:1px solid #eef2f7;min-width:0;}"
			+ ".ppu-chip b{font-size:8.5px;color:#64748b;font-weight:800;text-transform:uppercase;letter-spacing:.03em;}"
			+ ".ppu-chip span{font-size:12px;color:#0f172a;font-weight:700;margin-top:3px;word-break:break-word;}"
			+ ".ppu-kartu-foot{margin-top:auto;padding:11px 14px;border-top:1px solid #f1f5f9;display:flex;"
			+ "flex-wrap:wrap;gap:9px;align-items:center;}"
			// Button group rapi: bingkai halus, tombol seragam berjarak tipis, membungkus di layar sempit.
			+ ".ppu-gbtngrp{display:inline-flex;flex-wrap:wrap;gap:6px;padding:5px;background:#f8fafc;"
			+ "border:1px solid #e8edf3;border-radius:12px;max-width:100%;box-shadow:inset 0 1px 2px rgba(15,23,42,.03);}"
			+ ".ppu-gbtngrp .z-toolbarbutton{white-space:nowrap;}"
			// Blok "Ketentuan Ujian" (bahasa ramah-peserta) + penilaian/Sub-CPMK.
			+ ".ppu-ket{padding:2px 14px 12px;}"
			+ ".ppu-ket-judul{font-size:10px;font-weight:800;color:#0f172a;margin:8px 0 6px;text-transform:uppercase;"
			+ "letter-spacing:.03em;}"
			+ ".ppu-ket-list{list-style:none;margin:0;padding:0;}"
			+ ".ppu-ket-list li{font-size:11px;line-height:1.5;color:#334155;padding:2px 0;display:flex;gap:4px;"
			+ "align-items:flex-start;}"
			+ ".ppu-pnl{margin-top:9px;background:#f0f9ff;border:1px solid #bae6fd;border-radius:10px;padding:8px 11px;}"
			+ ".ppu-pnl-judul{font-size:9.5px;font-weight:800;color:#075985;text-transform:uppercase;"
			+ "letter-spacing:.03em;margin-bottom:3px;}"
			+ ".ppu-pnl-isi{font-size:11px;line-height:1.55;color:#0c4a6e;}"
			+ ".ppu-pnl-isi ul{list-style:none;margin:4px 0 0;padding:0;}"
			+ ".ppu-pnl-isi li{font-size:11px;line-height:1.5;color:#0c4a6e;padding:1px 0;}"
			// Area aksi peserta: tombol Ikut Ujian jadi CTA penuh & jelas.
			+ ".ppu-kartu-foot-peserta{flex-direction:column;align-items:stretch;gap:7px;}"
			+ ".ppu-kartu-foot-peserta .z-vbox,.ppu-kartu-foot-peserta .z-vbox>tbody>tr>td{width:100%!important;}"
			+ ".ppu-kartu-foot-peserta .z-toolbarbutton{display:flex!important;align-items:center;"
			+ "justify-content:center;gap:7px;width:100%;box-sizing:border-box;background:linear-gradient(135deg,"
			+ "#059669,#10b981)!important;color:#fff!important;border:0!important;border-radius:10px!important;"
			+ "padding:11px 14px!important;font-weight:800!important;font-size:12.5px!important;"
			+ "text-decoration:none!important;box-shadow:0 6px 14px rgba(5,150,105,.28);}"
			+ ".ppu-kartu-foot-peserta .z-toolbarbutton *{color:#fff!important;}"
			+ ".ppu-kartu-foot-peserta .z-toolbarbutton-disd,.ppu-kartu-foot-peserta .z-toolbarbutton[disabled]{"
			+ "background:#e5e7eb!important;color:#94a3b8!important;box-shadow:none!important;}"
			+ ".ppu-kartu-foot-peserta .z-toolbarbutton-disd *{color:#94a3b8!important;}"
			+ ".ppu-modal-grid .z-grid-header{display:none!important;}"
			+ ".ppu-modal-grid,.ppu-modal-grid .z-grid,.ppu-modal-grid .z-grid-body{border:0!important;"
			+ "background:transparent!important;height:auto!important;overflow:visible!important;}"
			+ ".ppu-modal-grid table,.ppu-modal-grid tbody,.ppu-modal-grid .z-rows,.ppu-modal-grid .z-row,"
			+ ".ppu-modal-grid .z-grid-odd{display:block!important;width:100%!important;background:transparent!important;"
			+ "border:0!important;}"
			+ ".ppu-modal-grid .z-row>td,.ppu-modal-grid .z-row-inner,.ppu-modal-grid .z-cell{display:block!important;"
			+ "width:auto!important;border:0!important;border-bottom:1px solid #f1f5f9!important;padding:9px 3px!important;"
			+ "white-space:normal!important;}"
			+ ".ppu-modal-grid .z-row-cnt,.ppu-modal-grid .z-cell-cnt{display:block!important;width:auto!important;"
			+ "white-space:normal!important;}"
			// Grid Sub-CPMK yang bersarang: kembalikan ke tata letak tabel normal (jangan
			// ikut diratakan/di-stack seperti grid modal utama), dan tampilkan header-nya.
			+ ".ppu-modal-grid .ppu-subcpmk-grid .z-grid-header{display:block!important;}"
			+ ".ppu-modal-grid .ppu-subcpmk-grid table,.ppu-modal-grid .ppu-subcpmk-grid tbody,"
			+ ".ppu-modal-grid .ppu-subcpmk-grid .z-rows{display:table!important;width:100%!important;}"
			+ ".ppu-modal-grid .ppu-subcpmk-grid .z-row{display:table-row!important;}"
			+ ".ppu-modal-grid .ppu-subcpmk-grid .z-row>td,.ppu-modal-grid .ppu-subcpmk-grid .z-cell{"
			+ "display:table-cell!important;width:auto!important;border-bottom:0!important;padding:4px 6px!important;"
			+ "vertical-align:top;}" + "</style>";

	/**
	 * Titik masuk utama: membangun tab "Ujian" untuk satu {@link Pertemuan} ke dalam
	 * {@code component} (dipanggil dari {@link PertemuanHelper}). Mengosongkan {@code component},
	 * lalu memasang toolbar aksi dan wadah kartu ({@link #kartuWrap}) sebelum memanggil
	 * {@link #loadData(Object)} untuk mengisi datanya.
	 *
	 * <p><b>Toolbar yang dibangun</b> (visibilitas sebagian besar tombol dibatasi hanya untuk
	 * pengelola/dosen, bukan peserta):</p>
	 * <ul>
	 * <li><b>Ambil Bahan Ujian</b> — membuka {@link AmbilDataUjianBanyak} untuk memilih
	 * {@link Ujian} yang sudah ada dan menautkannya ke pertemuan ini sebagai
	 * {@link PertemuanPunyaUjian} baru (durasi default 30 menit, mulai sekarang, berakhir besok);
	 * mengirim email/notifikasi lewat {@code CommonEmail.infoAdaUjianPerkuliahan}/
	 * {@code CommonNotifikasi.infoUjianBaru} untuk tiap ujian yang ditautkan.</li>
	 * <li><b>Buat Ujian</b> — sama seperti di atas tetapi membuat {@link Ujian} baru lewat
	 * {@code UjianAction.onAddExternal} lebih dulu.</li>
	 * <li><b>Format Nilai</b> (hanya bila perkuliahan tidak dikunci &amp; bukan kurikulum OBE) —
	 * membuka {@code FormatPenilaianHelper} untuk mengatur komponen penilaian perkuliahan.</li>
	 * <li><b>Rekap Hasil Ujian</b> / <b>Rekap Semua Hasil Ujian</b> — membuka
	 * {@link RekapHasilUjian}/{@link RekapHasilUjianPerVoPertemuan}.</li>
	 * <li><b>Singkronkan Soal Peserta</b> — tombol hasil {@link #prosesUlangSoal}.</li>
	 * <li><b>Rekap</b> — rekap pengawasan anti-curang lewat {@code RekapPengawasanUjianHelper}
	 * untuk {@link PertemuanPunyaUjian} pertama pada pertemuan ini.</li>
	 * <li><b>History</b> — riwayat revisi lewat {@code RevisiPertemuanPunyaUjianHelper}.</li>
	 * <li><b>Lihat Peserta Ujian</b> — membuka {@code /pages/master/hasil_ujian_mahasiswa.zul}.</li>
	 * <li><b>Refresh</b> — {@link #loadData(Object)} dengan {@code true} lalu memanggil ulang
	 * {@link #display(Pertemuan, Component)} sendiri (re-render toolbar &amp; kartu dari awal).</li>
	 * </ul>
	 *
	 * <p><b>Sifat gerbang toolbar: UI-only, dan tidak seragam.</b> Semua pembatasan tombol di atas
	 * dilakukan lewat {@code setVisible(...)} saja; TIDAK ada listener {@code onClick} di method
	 * ini yang memeriksa ulang peran pengguna sebelum menjalankan aksinya. Predikat perannya pun
	 * dieja ulang manual di tiap tombol dengan varian yang menghilangkan
	 * {@code tbmuser.getMahasiswa()}/{@code getBiodataCalonMahasiswa()} dan justru menulis
	 * {@code tbmuser.getSiswa() == null} dua kali (lihat Javadoc kelas dan
	 * {@code task_d45feed7}). Perlu dicatat pula dua tombol yang SAMA SEKALI tanpa gerbang
	 * visibilitas sehingga tampil untuk semua peran termasuk peserta: <b>Lihat Peserta Ujian</b>
	 * (membuka {@code /pages/master/hasil_ujian_mahasiswa.zul}, yang penjagaannya diserahkan
	 * sepenuhnya ke halaman tujuan) dan <b>Refresh</b>. Tombol <b>Format Nilai</b> memakai
	 * varian predikat yang lebih pendek lagi — tanpa {@code getPesertaKursus()} — namun ditambah
	 * syarat {@code perkuliahan.getDikunci() == null}.
	 *
	 * <p><b>Cakupan data tidak diperiksa di sini.</b> {@code display} tidak menyaring berdasarkan
	 * satuan kerja maupun berdasarkan mata kuliah yang diampu pengguna; ia menerima
	 * {@code pertemuan} apa adanya dari pemanggil. Penyaringan daftar ujian sepenuhnya berada di
	 * {@code pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser)} yang dipanggil
	 * {@link #loadData(Object)}, sedangkan kelayakan pengguna membuka pertemuan itu sendiri
	 * dijaga oleh halaman pemanggil (mis. {@code CommonPrivilages} pada {@code JadwalUjianAction}).
	 *
	 * <p><b>Efek pada state instance:</b> meng-set {@link #pertemuan}, membuat {@link #kartuWrap}
	 * baru, dan meng-set {@link #grid} ke {@code null} secara eksplisit di akhir method (lihat
	 * kuirk pada Javadoc kelas: field {@link #grid} sekarang tidak pernah diisi selain {@code null}
	 * di sini, sehingga jalur grid tabel lama di {@link #loadData(Object)} tidak lagi tereksekusi).
	 *
	 * @param pertemuan pertemuan yang tab Ujian-nya akan ditampilkan.
	 * @param component komponen ZK tujuan (dikosongkan lebih dulu via {@code Common.clear}, boleh
	 *                  {@code null} untuk melewati langkah pengosongan itu).
	 */
	@SuppressWarnings("unchecked")
	public void display(final Pertemuan pertemuan, final Component component) {
		this.pertemuan = pertemuan;
		if (component != null) {
			Common.clear(component);
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Div div = new Div();
		div.setStyle("min-height:3600px");
		div.setWidth("100%");
		div.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(div);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Bahan Ujian", "/img/new.gif");
		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				List<Ujian> ujians = HibernateUtil.currentSession().createCriteria(PertemuanPunyaUjian.class)
						.add(Restrictions.eq("pertemuan", pertemuan)).setProjection(Projections.property("ujian"))
						.list();

				AmbilDataUjianBanyak window = new AmbilDataUjianBanyak(ujians, pertemuan.untuk(),
						pertemuan.getPerkuliahan() == null ? null : pertemuan.getPerkuliahan().getMatakuliah(),
						pertemuan.getJadwalPelajaran() == null ? null
								: pertemuan.getJadwalPelajaran().getMatapelajaran());

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("95%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<Ujian> ujians = (List<Ujian>) arg0.getData();

						if (ujians != null) {
							Session session = HibernateUtil.currentSession();

							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

							Calendar waktu = ais.ui.util.WaktuUtil.getCalendar();
							waktu.set(Calendar.SECOND, 0);
							waktu.set(Calendar.HOUR_OF_DAY, 0);
							waktu.set(Calendar.MINUTE, 30);

							for (Ujian ujian : ujians) {

								// Pastikan Pertemuan MASIH ADA (bisa terhapus sejak dialog dibuka) agar tidak melanggar
								// FK "pertemuan_punya_ujian.pertemuan -> pertemuan" saat save (Key pertemuan tidak ada).
								Pertemuan pertemuanValid = (pertemuan == null || pertemuan.getId() == null) ? null
										: (Pertemuan) session.get(Pertemuan.class, pertemuan.getId());
								if (pertemuanValid == null) {
									MyMessageboxConfig.show(
				"Mohon maaf, data pertemuan tidak ditemukan (kemungkinan telah dihapus). Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) apabila kendala masih berlanjut, hubungi Admin untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									return;
								}
								PertemuanPunyaUjian pertemuanPunyaUjian = new PertemuanPunyaUjian();
								pertemuanPunyaUjian.setUjian(ujian);
								pertemuanPunyaUjian.setPertemuan(pertemuanValid);
								pertemuanPunyaUjian.setDibatasiWaktu(true);
								pertemuanPunyaUjian.setLama(waktu.getTime());
								pertemuanPunyaUjian.setMulaiUjian(ais.ui.util.WaktuUtil.getDate());
								pertemuanPunyaUjian.setSampaiUjian(calendar.getTime());

								session.save(pertemuanPunyaUjian);
								CommonEmail.infoAdaUjianPerkuliahan(pertemuanValid, ujian);
								ais.common.CommonNotifikasi.infoUjianBaru(pertemuanValid, ujian);
							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							}, "Loading..", false, 1500);

						}

					}
				});

				window.onModal();
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Buat Ujian", "/img/new.gif");
		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Ujian ujian = new Ujian();
				ujian.setDosen(pertemuan.getPerkuliahan() == null ? null : pertemuan.getPerkuliahan().getDosen1());
				ujian.setMatakuliah(
						pertemuan.getPerkuliahan() == null ? null : pertemuan.getPerkuliahan().getMatakuliah());

				UjianAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Ujian ujian = (Ujian) arg0.getData();
						if (ujian != null) {

							Session session = HibernateUtil.currentSession();

							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

							Calendar waktu = ais.ui.util.WaktuUtil.getCalendar();
							waktu.set(Calendar.SECOND, 0);
							waktu.set(Calendar.HOUR_OF_DAY, 0);
							waktu.set(Calendar.MINUTE, 30);

							// Pastikan Pertemuan MASIH ADA (bisa terhapus sejak dialog dibuka) agar tidak melanggar
							// FK "pertemuan_punya_ujian.pertemuan -> pertemuan" saat save (Key pertemuan tidak ada).
							Pertemuan pertemuanValid = (pertemuan == null || pertemuan.getId() == null) ? null
									: (Pertemuan) session.get(Pertemuan.class, pertemuan.getId());
							if (pertemuanValid == null) {
								MyMessageboxConfig.show(
				"Mohon maaf, data pertemuan tidak ditemukan (kemungkinan telah dihapus). Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) apabila kendala masih berlanjut, hubungi Admin untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}
							PertemuanPunyaUjian pertemuanPunyaUjian = new PertemuanPunyaUjian();
							pertemuanPunyaUjian.setUjian(ujian);
							pertemuanPunyaUjian.setPertemuan(pertemuanValid);
							pertemuanPunyaUjian.setDibatasiWaktu(true);
							pertemuanPunyaUjian.setLama(waktu.getTime());
							pertemuanPunyaUjian.setMulaiUjian(ais.ui.util.WaktuUtil.getDate());
							pertemuanPunyaUjian.setSampaiUjian(calendar.getTime());

							session.save(pertemuanPunyaUjian);

							CommonEmail.infoAdaUjianPerkuliahan(pertemuanValid, ujian);
							ais.common.CommonNotifikasi.infoUjianBaru(pertemuanValid, ujian);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							}, "Loading..", false, 1500);

						}
					}
				}, ujian, pertemuan.untuk());
			}
		});
		button.setParent(toolbar);

		if (pertemuan.getPerkuliahan() != null) {
			final Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
			if (perkuliahan != null && !perkuliahan.getSembunyikanFormatPenilaian()) {
				final MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Format Nilai",
						"/img/svg/edit-box-line.svg");
				buttonFormatNilai.setParent(toolbar);
				buttonFormatNilai.setVisible(
						perkuliahan.getDikunci() == null && mahasiswa == null && biodataCalonMahasiswa == null
								&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);

				if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
						.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
					buttonFormatNilai.setVisible(false);
				}

				buttonFormatNilai.addEventListener("onClick", new EventListener() {

					FormatPenilaianHelper formatPenilaianHelper = new FormatPenilaianHelper();

					@Override
					public void onEvent(Event event) throws Exception {

						MyWindow addWindow = new MyWindow();
						addWindow.setHeight("95%");
						addWindow.setWidth("700px");
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);

						formatPenilaianHelper.display(perkuliahan, addWindow, new TampilDetailNilaiInterface() {

							@Override
							public void realoadNilai(final Perkuliahan perkuliahan) {

								Common.realoadNilai(perkuliahan, perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(true);
													}
												}, "Loading..", false, 1500);

											}
										}, null);

							}
						});
					}

				});
			}
		}

		Common.bolehKonfigurasi("tampilkan_rekap_hasil_ujian");

		MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Ujian",
				"/img/svg/edit-box-line.svg");
		buttonFormatNilai.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		buttonFormatNilai.setParent(toolbar);
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				RekapHasilUjian addWindow = new RekapHasilUjian(pertemuan);
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Hasil Ujian");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}

		});

		buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Semua Hasil Ujian", "/img/svg/edit-box-line.svg");
		buttonFormatNilai.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		buttonFormatNilai.setParent(toolbar);
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				RekapHasilUjianPerVoPertemuan addWindow = new RekapHasilUjianPerVoPertemuan(false,
						pertemuan == null ? null : pertemuan.ambilVOPembelajaran());
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Semua Hasil Ujian");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}

		});

//		if (mahasiswa != null || biodataCalonMahasiswa != null || (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null) {
//			button = new MyToolbarbuttonConfig("Rekap Hasil Ujian", "/img/Document-Text-icon.png");
//			button.addEventListener("onClick", new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					try {
//
//						RekapHasilUjianMahasiswa addWindow = new RekapHasilUjianMahasiswa(false, mahasiswa,
//								biodataCalonMahasiswa, pertemuan == null ? null : pertemuan.ambilVOPembelajaran());
//						addWindow.setClosable(true);
//						addWindow.setTitle("Rekap Hasil Ujian");
//						addWindow.setHeight("95%");
//						addWindow.setWidth("90%");
//						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
//						addWindow.onModal();
//
//					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaUjianHelper.java:4408");
//						e.printStackTrace();
//					}
//				}
//			});
//			button.setParent(toolbar);
//		}

		button = prosesUlangSoal(pertemuan, "Singkronkan Soal Peserta", "/img/svg/refresh-cw.svg");
		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		button.setParent(toolbar);

		// Tombol Rekap: rekap pelanggaran pengawasan ujian (anti-curang) per peserta.
		button = new MyToolbarbuttonConfig("Rekap", "/img/print.png");
		button.setTooltiptext("Rekap pengawasan ujian (jumlah & log pelanggaran per peserta)");
		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) HibernateUtil.currentSession()
						.createCriteria(PertemuanPunyaUjian.class).add(Restrictions.eq("pertemuan", pertemuan))
						.setMaxResults(1).uniqueResult();
				if (ppu != null) {
					RekapPengawasanUjianHelper.tampilkanRekap(ppu);
				}
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPertemuanPunyaUjianHelper revisiHelper = new RevisiPertemuanPunyaUjianHelper(pertemuan,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(true);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Lihat Peserta Ujian", "/img/eye-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.displayWindow("/pages/master/hasil_ujian_mahasiswa.zul", true, "95%",
						Common.isMobile() ? "100%" : "950px", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(false);
							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						display(pertemuan, component);
					}
				});
			}
		});

		button.setParent(toolbar);

		div.appendChild(ais.ui.util.DashboardUiKit.html(ais.ui.util.DashboardUiKit.headerModul("ujian",
				"Daftar Ujian Pertemuan",
				"Daftar ujian pada pertemuan ini beserta nilai dan jumlah peserta yang sudah mengerjakan. Gunakan tombol Rekap untuk melihat ringkasan nilai dalam bentuk grafik.")));

		// ==== Tampilan KARTU responsif (kotak-kotak) untuk SEMUA peran ====
		// Mobile: 1 kartu per baris; Desktop: 2 kartu per baris.
		//  - Pengelola/dosen : kartu ringkas + tombol "Pengaturan Data Ujian" (buka modal).
		//  - Mahasiswa/Siswa/Calon/Biodata : kartu ringkas + tombol "Ikut Ujian" saja
		//    (logika kelayakan ikut ujian tetap memakai tampilBolekIkutUjianAtauTidak lama).
		div.appendChild(ais.ui.util.DashboardUiKit.html(GAYA_KARTU_UJIAN));
		kartuWrap = new Div();
		kartuWrap.setSclass("ppu-kartu-wrap");
		kartuWrap.setWidth("100%");
		kartuWrap.setParent(div);
		grid = null;

		loadData(null);

	}

	/**
	 * Membangun kolom-kolom {@link MyGrid} untuk Daftar Ujian Pertemuan. Diekstrak dari
	 * {@code display()} agar dipakai ulang oleh dua tempat: (1) tampilan lama berbasis
	 * grid untuk mahasiswa/siswa/calon, dan (2) grid 1-baris di dalam modal
	 * "Pengaturan Data Ujian" milik tampilan kartu pengelola. Menjaga struktur kolom
	 * identik sehingga jumlah/urutan sel yang dihasilkan {@link DetailPertemuanRenderer}
	 * tetap cocok di kedua tempat.
	 *
	 * <p><b>Kontrak yang wajib dijaga.</b> {@link DetailPertemuanRenderer#render(Row, Object)}
	 * menambahkan anak ke baris secara berurutan tanpa menyebut nama kolom. Artinya JUMLAH dan
	 * URUTAN kolom di sini harus persis mengikuti urutan komponen yang ditambahkan renderer.
	 * Menyisipkan, menghapus, atau menukar kolom di method ini akan menggeser seluruh isi baris
	 * tanpa error kompilasi maupun exception saat berjalan — kesalahannya hanya terlihat sebagai
	 * data yang tampil di kolom yang salah. Karena itu kolom yang tidak relevan untuk sebuah peran
	 * TIDAK dihapus, melainkan disembunyikan dengan {@code setWidth("0px")} atau
	 * {@code setVisible(false)}.
	 *
	 * <p><b>Kolom yang dibangun, berurutan:</b> kolom kosong pembuka (selebar 40px untuk
	 * pengelola, 0px untuk peserta — tempat tombol "Kelola Soal Ujian"), Ujian, Jenis,
	 * Skor/Jml.Ikut.Ujian, Nilai/Maks.blh.Ikut, Jml.Soal (labelnya berubah menjadi
	 * "Jml.Soal/Maks.Skor" untuk peserta), Dibatasi Wkt, Lama, Pelaksanaan, lalu SATU kolom yang
	 * bercabang — "Pengaturan Sub-CPMK" bila kurikulum perkuliahan berstatus OBE, selain itu
	 * "Nilai masuk ke" — dan ditutup kolom Aktif serta kolom kosong untuk baris tombol aksi.
	 * Perhatikan bahwa kedua cabang menghasilkan JUMLAH kolom yang sama, sehingga kontrak urutan
	 * di atas tetap terjaga pada kurikulum OBE maupun non-OBE.
	 *
	 * <p>Lebar dan visibilitas kolom ditentukan lewat predikat peran yang dieja ulang manual —
	 * termasuk varian dengan {@code getSiswa() == null} ganda yang dicatat pada Javadoc kelas.
	 * Di sini dampaknya terbatas pada tampilan (kolom melebar atau menyempit), bukan pada hak
	 * akses, karena kontrol yang sesungguhnya dijaga di dalam renderer.
	 *
	 * @param grid    grid tujuan; kolom ditambahkan sebagai anak {@link Columns} baru. Grid yang
	 *                sudah punya {@link Columns} sebaiknya tidak dikirim ke sini.
	 * @param tbmuser pengguna aktif, dipakai bersama field {@link #mahasiswa} dan
	 *                {@link #biodataCalonMahasiswa} untuk menentukan lebar/visibilitas kolom.
	 *                Sebagian ekspresi di dalam method ini memanggil {@code tbmuser.getSiswa()}
	 *                setelah cabang {@code tbmuser != null} sudah gugur, sehingga {@code null}
	 *                TIDAK aman dikirim.
	 */
	private void buatKolomUjian(MyGrid grid, Tbmuser tbmuser) {
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null ? "40px" : "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ujian");
		if (biodataCalonMahasiswa == null) {
			column.setWidth("20%");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skor/Jml.Ikut.Ujian");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai/Maks.blh.Ikut");
		column.setWidth((Common.bolehKonfigurasi("nilai_ujian_ditampilkan_ke_mahasiswa")
				&& (mahasiswa != null || biodataCalonMahasiswa != null
						|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null)) ? "12%" : "0px");
		column.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel((mahasiswa != null || biodataCalonMahasiswa != null
				|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
				|| tbmuser.getCalonSiswa() != null) ? "Jml.Soal/Maks.Skor" : "Jml Soal");
		column.setWidth(mahasiswa != null || biodataCalonMahasiswa != null
				|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
				|| tbmuser.getCalonSiswa() != null ? "12%" : "10%");
		column.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dibatasi Wkt");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lama");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pelaksanaan");
		column.setWidth(biodataCalonMahasiswa != null ? "0px" : "14%");

		if (pertemuan != null && pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getKurikulum() != null
				&& pertemuan.getPerkuliahan().getKurikulum().apakahObe(pertemuan.getPerkuliahan().getTahunAjaran(),
						pertemuan.getPerkuliahan().getGanjilGenap())) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Pengaturan Sub-CPMK");
			column.setWidth("20%");
		} else {

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nilai masuk ke");
			column.setWidth(biodataCalonMahasiswa != null ? "0px" : "14%");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null ? "40px" : "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");
	}

}
