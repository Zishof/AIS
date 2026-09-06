package ais.action.master.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.SyaratUjianAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.dashboard.admin.RekapHasilTugasMahasiswa;
import ais.action.master.dashboard.admin.RekapHasilTugasPerVoPertemuan;
import ais.action.master.helper.generic.AmbilDataTugasMandiri;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.AIGenerator;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.TugasFileContent;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHboxToolbar;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMenuitem;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

/**
 * TugasMandiriHelper — Helper UI utama untuk manajemen Tugas Mandiri pada modul e-Learning.
 *
 * <p>Kelas ini bertanggung jawab penuh atas pembangunan antarmuka pengguna (UI) yang berkaitan
 * dengan Tugas Mandiri (tugas individu) dalam sistem e-Learning berbasis ZK Framework 5.0.13.
 * Helper ini dipanggil dari modul pembelajaran setiap kali sebuah tab "Tugas" pada pertemuan
 * dibuka, sehingga seluruh interaksi peserta (upload berkas) dan pengelola (penilaian, rekap,
 * kehadiran) diproses di sini secara terpusat.</p>
 *
 * <p><strong>Konteks Penggunaan dalam e-Learning:</strong><br>
 * Sistem e-Learning AIS mendukung dua jenis entitas pembelajaran utama: {@link Perkuliahan}
 * (kuliah tingkat perguruan tinggi) dan {@link JadwalPelajaran} (pelajaran tingkat sekolah).
 * TugasMandiriHelper bekerja untuk keduanya — ia memeriksa tipe entitas dari parameter
 * {@link Tugas} yang diterima dan menyesuaikan tampilan serta logika penilaian secara otomatis.
 * Untuk konteks perguruan tinggi dengan kurikulum OBE (Outcome Based Education), helper ini
 * juga menangani penilaian berbasis Sub-CPMK (Capaian Pembelajaran Mata Kuliah) yang memiliki
 * bobot berbeda-beda per tugas dan disimpan dalam format JSON.</p>
 *
 * <p><strong>Arsitektur dan Pola yang Digunakan:</strong><br>
 * Helper ini mengikuti pola <em>procedural UI builder</em> yang lazim di ZK Framework 5:
 * komponen-komponen UI (Grid, Tabbox, Borderlayout, Toolbar) dibuat dan dirangkai secara
 * programatik di dalam metode {@code createTugas}. Tidak ada ZUML/ZUL file yang digunakan;
 * seluruh layout dibangun dari Java. Setiap komponen interaktif mendaftarkan
 * {@link EventListener} anonim sesuai gaya pemrograman Java 1.6 (tanpa lambda maupun
 * try-with-resources).</p>
 *
 * <p>State antarmuka disimpan sebagai field instance: {@code tugas}, {@code perkuliahan},
 * {@code jadwalPelajaran}, {@code pa}, {@code tbmuser}, {@code treemapData}, dan
 * {@code uploadTugasGrid}. Satu instance TugasMandiriHelper berkorespondensi dengan satu
 * sesi interaksi untuk satu tugas tertentu.</p>
 *
 * <p><strong>Fitur Utama:</strong></p>
 * <ul>
 *   <li><em>Portal Layout dua kolom</em>: kiri menampilkan instruksi/perintah tugas (40%),
 *       kanan menampilkan toolbar aksi dan daftar pengumpulan (60%).</li>
 *   <li><em>Toolbar terstruktur dengan pengelompokan visual lewat separator</em>:
 *       Grup Berkas (Upload, Download Semua, Drive, Akses),
 *       Grup Nilai (Masukkan Nilai, Download Nilai, Upload Nilai),
 *       Grup Kelola (Refresh, Recovery),
 *       Grup Kehadiran (Anggap Hadir Pengumpul, Anggap Hadir Pengakses, Anggap Sudah Upload).</li>
 *   <li><em>Tab Telah Upload</em>: daftar peserta yang sudah mengumpulkan, lengkap dengan
 *       kolom nilai dan keterangan yang dapat diedit langsung (inline edit). Indikator warna
 *       pada baris menunjukkan status penilaian: hijau = sudah dinilai, merah = belum dinilai.</li>
 *   <li><em>Tab Belum Upload</em>: daftar peserta yang belum mengumpulkan, dilengkapi tombol
 *       "Anggap Alpa" untuk keperluan presensi otomatis.</li>
 *   <li><em>Tab Statistik</em>: dasbor HTML/CSS responsif menggunakan DashboardUiKit berisi
 *       kartu ringkasan, donut chart pengumpulan, progress bar capaian, bar chart tren per
 *       tanggal, histogram distribusi nilai (5 rentang), dan radar chart Sub-CPMK (OBE).</li>
 *   <li><em>Tab Peserta yg tidak perlu ikut</em>: pengelola dapat menandai mahasiswa yang
 *       dikecualikan dari tugas ini tanpa menghapus data pengumpulan mereka.</li>
 *   <li><em>Tab Rekap Tugas</em>: rekap lintas pertemuan via RekapHasilTugasPerVoPertemuan.</li>
 *   <li><em>Penilaian OBE</em>: mendukung bobot nilai per Sub-CPMK yang disimpan dalam JSON
 *       dan dapat diedit per peserta secara langsung di grid.</li>
 *   <li><em>Upload Nilai Massal</em>: dosen/admin dapat mengunggah file Excel berisi nilai
 *       seluruh peserta sekaligus (format xlsx).</li>
 *   <li><em>Integrasi Google Drive</em>: berkas tugas peserta dapat dikirim otomatis ke
 *       Google Drive pengajar via GDriveUtilPerPengguna.</li>
 *   <li><em>Recovery/Riwayat Tugas</em>: tombol Recovery membuka riwayat perubahan tugas
 *       (Hibernate Envers) sehingga tugas yang terhapus dapat dipulihkan.</li>
 *   <li><em>Anggap Sudah Upload</em>: menandai seluruh peserta sebagai telah mengumpulkan
 *       dengan berkas kosong — berguna untuk kebutuhan administrasi absensi.</li>
 * </ul>
 *
 * <p><strong>Cara Penggunaan:</strong><br>
 * Instansiasi dengan mahasiswa/biodataCalonMahasiswa yang sedang login (null untuk dosen/admin),
 * kemudian panggil {@code createTugas(tugas, tabpanel, hapusEvent, tampilInfo)} untuk
 * membangun seluruh UI di dalam {@code tabpanel} yang diberikan. Metode ini membersihkan
 * konten tabpanel terlebih dahulu sebelum membangun ulang seluruh komponen.</p>
 *
 * <p><strong>Ketergantungan Penting:</strong></p>
 * <ul>
 *   <li>{@link HibernateUtil} — sesi Hibernate untuk operasi baca/tulis DB biasa.</li>
 *   <li>{@link StreamingHibernateUtil} — sesi khusus untuk operasi berkas biner (BLOB).</li>
 *   <li>{@code ais.ui.util.DashboardUiKit} — generator HTML/CSS untuk kartu, chart, dasbor.</li>
 *   <li>{@link Tugas} — antarmuka tugas yang diimplementasikan oleh {@link Pertemuan} dan
 *       {@link TugasPertemuan}.</li>
 *   <li>{@link Common} — utilitas umum: format tanggal, current user, upload, dll.</li>
 * </ul>
 *
 * <p><strong>Catatan Kompatibilitas:</strong><br>
 * Kelas ini ditulis dengan gaya Java 1.6: tidak menggunakan lambda, diamond operator {@code <>},
 * Stream API, try-with-resources, atau multi-catch. Semua EventListener ditulis sebagai
 * anonymous inner class. Dikompilasi bersama ZK Framework 5.0.13 dan Hibernate 3.x.
 * Penggunaan {@code @SuppressWarnings("deprecation")} diperlukan karena beberapa API ZK 5
 * telah ditandai deprecated di versi yang lebih baru.</p>
 *
 * <p><strong>Hubungan dengan mekanisme Tugas Kelompok — hasil penelusuran kode.</strong><br>
 * Meskipun namanya berpasangan dengan {@code TugasKelompokHelper}, kelas ini tidak memiliki
 * ketergantungan apa pun terhadap mekanisme tugas kelompok. Tidak ada {@code import} maupun rujukan
 * ke {@code ais.database.model.NamaTugasKelompok}, {@code NamaTugasKelompokPunyaMahasiswa}, atau
 * kelas bernama {@code TugasKelompok} di berkas ini. Kedua mekanisme berbagi konsep umum yang sama —
 * keduanya menggantung pada {@link Pertemuan} dan menyimpan berkas sebagai
 * {@link ais.database.model.file.TugasFileContent} — tetapi jalur datanya terpisah penuh:</p>
 * <ul>
 *   <li><em>Tugas Mandiri</em> (kelas ini): satuan pengumpulan adalah <strong>individu</strong>.
 *       Setiap {@link ais.database.model.file.TugasFileContent} menunjuk satu peserta lewat salah
 *       satu dari empat kolom id ({@code mahasiswa}, {@code siswa}, {@code biodataCalonMahasiswa},
 *       {@code calonSiswa}), dan menunjuk tugasnya lewat kolom {@code pertemuan} yang berisi
 *       {@code tugas.getId()}. Tidak ada entitas kelompok di antara peserta dan berkas.</li>
 *   <li><em>Tugas Kelompok</em>: satuan pengumpulan adalah kelompok, dengan entitas perantara
 *       tersendiri untuk mendaftar anggota. Berkas dimiliki kelompok, bukan individu.</li>
 * </ul>
 * <p>Karena itu perubahan pada salah satu mekanisme tidak otomatis berlaku pada yang lain, dan
 * keduanya perlu diaudit terpisah.</p>
 *
 * <p><strong>Peta entitas yang benar-benar dipakai kelas ini.</strong></p>
 * <ul>
 *   <li>{@link ais.database.model.Tugas} — antarmuka; implementasinya {@link Pertemuan} dan
 *       {@link ais.database.model.TugasPertemuan}.</li>
 *   <li>{@link ais.database.model.file.TugasFileContent} — satu berkas pengumpulan milik satu
 *       peserta. Turunan {@link ais.database.model.file.FileFoto}, sehingga berbagi mekanisme BLOB,
 *       tautan Google Drive, dan pratinjau.</li>
 *   <li>{@link ais.database.model.file.LampiranLain} dengan jenis
 *       {@code LampiranLain.TUGAS_MANDIRI_PERKULIAHAN} — lampiran soal/instruksi milik pengelola,
 *       berbeda dari berkas jawaban peserta.</li>
 *   <li>Peserta: {@link ais.database.model.Mahasiswa},
 *       {@link ais.database.model.sekolah.Siswa},
 *       {@link ais.database.model.BiodataCalonMahasiswa},
 *       {@link ais.database.model.sekolah.CalonSiswa}.</li>
 *   <li>Penilaian perguruan tinggi: {@link ais.database.model.FormatNilai} (tunggal maupun
 *       Sub-CPMK OBE).</li>
 *   <li>Penilaian sekolah: {@link ais.database.model.sekolah.JenisPenilaian} &rarr;
 *       {@link ais.database.model.sekolah.GrupPenilaian} &rarr;
 *       {@link ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa} &rarr;
 *       {@link ais.database.model.sekolah.KategoriItemPenilaianSiswa} &rarr;
 *       {@link ais.database.model.sekolah.JenisItemPenilaianSiswa}.</li>
 *   <li>Prasyarat: {@link ais.database.model.SyaratUjian}. Absensi:
 *       {@link ais.database.model.Statusabsensi} lewat {@code Pertemuan.populate(...)}.</li>
 * </ul>
 *
 * <p><strong>Tempat penyimpanan nilai — penting untuk diketahui sebelum menyunting.</strong><br>
 * Nilai tugas mandiri tidak disimpan pada kolom {@code nilai} milik masing-masing
 * {@link ais.database.model.file.TugasFileContent}, melainkan pada satu dokumen JSON di kolom
 * {@code keteranganNilai} milik baris {@link ais.database.model.Tugas}. Kolom {@code nilai} pada
 * berkas tetap dibaca sebagai jalur mundur bagi data lama dan dipakai sebagai sumber indikator warna
 * baris, sehingga kedua sumber itu dapat berbeda. Rincian bentuk kunci JSON dijelaskan pada
 * {@code jsonObjectTugas}.</p>
 *
 * <p><strong>Ringkasan gerbang kewenangan.</strong><br>
 * Kelas ini tidak memiliki satu titik pemeriksaan kewenangan terpusat. Pembedaan peran dilakukan
 * lewat tiga dasar yang berbeda dan tidak selalu selaras: (1) {@code peserta}, dihitung dari
 * {@link ais.database.model.Tbmuser} sesi berjalan; (2) field {@code mahasiswa} dan
 * {@code biodataCalonMahasiswa} yang berasal dari argumen konstruktor dan hanya menyatakan konteks
 * pemanggilan; dan (3) rantai perbandingan {@code == null} terhadap kelima tautan pelajar pada
 * {@link ais.database.model.Tbmuser}, yang disalin berulang dengan variasi kecil. Bentuk terpusat
 * dari dasar ketiga tersedia lewat {@code bolehKelolaTugas(Tbmuser)} dan {@code bolehUpload(Tbmuser)}.
 * Perlu diperhatikan pula bahwa daftar pengumpulan yang menjadi model grid tidak disaring per
 * pengguna: {@code Tugas.ambilTugasFileContentTotal(...)} memakai {@code currentUser} hanya untuk
 * menandai baris milik pengguna, bukan untuk membatasi baris yang dikembalikan.</p>
 *
 * @author AIS System
 * @version 2.0
 * @since 2010
 * @see ais.database.model.Tugas
 * @see ais.database.model.file.TugasFileContent
 */
public class TugasMandiriHelper {

	/**
	 * Membersihkan referensi format nilai yatim sebelum Tugas/Pertemuan dimasukkan
	 * ke persistence-context milik request. Perbaikan memakai session terisolasi
	 * agar query pembersihan tidak memicu auto-flush entity yang sudah kotor.
	 *
	 * <h3>Masalah yang diselesaikan</h3>
	 * <p>Kolom {@code format_nilai} pada tabel {@code pertemuan} dan {@code tugas_pertemuan} adalah
	 * kunci asing ke tabel {@code formatnilai}. Baris master format nilai dapat terhapus tanpa ikut
	 * membersihkan rujukan pada kedua tabel itu, sehingga tertinggal kunci asing yatim — menunjuk id
	 * yang barisnya sudah tidak ada. Selama entity tugas hanya dibaca, keadaan itu tidak terasa.
	 * Masalah muncul saat entity tersebut ikut ter-flush: Hibernate mencoba menulis kembali kunci asing
	 * yang tidak sah dan melemparkan {@code ConstraintViolationException} mentah, yang pada layar
	 * tampak sebagai kegagalan menyimpan tugas tanpa penjelasan.</p>
	 *
	 * <h3>Mengapa memakai sesi terpisah</h3>
	 * <p>Pembersihan tidak boleh dijalankan pada sesi milik request. Menjalankan query pada sesi itu
	 * akan memicu <em>auto-flush</em>: Hibernate menulis lebih dahulu seluruh entity kotor yang ada di
	 * persistence context agar hasil query konsisten — dan justru penulisan itulah yang meledak karena
	 * kunci asing yatim belum sempat dibereskan. Karena itu metode ini membuka sesi sendiri lewat
	 * {@code HibernateUtil.openSession()} beserta transaksinya sendiri, sehingga persistence context
	 * request tidak tersentuh sama sekali.</p>
	 *
	 * <h3>Langkah yang dijalankan</h3>
	 * <ol>
	 *   <li>Membaca id format nilai yang sedang menempel pada entity, lalu menghitung apakah barisnya
	 *       masih ada dengan {@code select count(1) from formatnilai where id=:id}. Hasilnya disimpan
	 *       pada penanda {@code formatNilaiYatim}.</li>
	 *   <li>Menjalankan {@code update ... set format_nilai=null} pada tabel yang sesuai dengan jenis
	 *       entity — {@code pertemuan} untuk {@link Pertemuan}, {@code tugas_pertemuan} untuk
	 *       {@link TugasPertemuan}. Klausa {@code and not exists (select 1 from formatnilai ...)}
	 *       membuat pembaruan hanya mengenai baris yang rujukannya memang yatim, sehingga tugas yang
	 *       format nilainya sah tidak ikut dikosongkan.</li>
	 *   <li>Commit, lalu — hanya bila penanda menyatakan yatim — memanggil
	 *       {@code tugas.setFormatNilai(null)} agar objek di memori ikut selaras dengan basis data.</li>
	 * </ol>
	 *
	 * <h3>Penanganan kegagalan</h3>
	 * <p>Setiap kegagalan di-rollback bila transaksi masih aktif, lalu dicatat ke
	 * {@code ErrorAuditUtil} dan <em>tidak</em> dilempar ulang. Sikap ini disengaja: pembersihan
	 * bersifat pencegahan, dan kegagalannya tidak boleh menggagalkan aksi pengguna yang sedang
	 * berjalan. Blok {@code finally} menutup sesi secara berlapis — {@code clear()},
	 * {@code disconnect()}, lalu {@code close()} — masing-masing dibungkus {@code try}/{@code catch}
	 * agar satu kegagalan tidak menghalangi langkah penutupan berikutnya dan koneksi tetap
	 * dikembalikan ke pool.</p>
	 *
	 * <h3>Pemakaian</h3>
	 * <p>Dipanggil di empat titik pada {@link #onUbahPerintahTugas(EventListener)}: pada perubahan
	 * kotak bobot persen, pada perubahan combobox format nilai, pada perubahan judul, dan pada tombol
	 * "Simpan Tugas". Ketiga titik pertama melanjutkannya dengan {@code session.refresh(tugas)} agar
	 * entity pada sesi request memuat ulang kolom yang baru saja dikosongkan. Metode ini menangani
	 * data lama di basis data; pasangannya,
	 * {@link #ambilFormatNilaiValid(Session, FormatNilai)}, menangani objek usang yang datang dari
	 * layar. Keduanya diperlukan.</p>
	 *
	 * <p>Argumen {@code null} atau entity yang belum memiliki id langsung diabaikan.</p>
	 *
	 * @param tugas entity tugas yang kunci asing format nilainya akan diperiksa dan dibereskan; boleh
	 *              {@code null}.
	 */
	private static void bersihkanFormatNilaiYatim(Tugas tugas) {
		if (tugas == null || tugas.getId() == null) {
			return;
		}
		Session sesiPerbaikan = null;
		Transaction transaksi = null;
		boolean formatNilaiYatim = false;
		Long formatNilaiId = tugas.getFormatNilai() == null ? null : tugas.getFormatNilai().getId();
		try {
			sesiPerbaikan = HibernateUtil.openSession();
			transaksi = sesiPerbaikan.beginTransaction();
			if (formatNilaiId != null) {
				Number jumlah = (Number) sesiPerbaikan
						.createSQLQuery("select count(1) from formatnilai where id=:id")
						.setLong("id", formatNilaiId.longValue()).uniqueResult();
				formatNilaiYatim = jumlah == null || jumlah.longValue() == 0L;
			}
			if (tugas instanceof Pertemuan) {
				sesiPerbaikan.createSQLQuery("update pertemuan p set format_nilai=null "
						+ "where p.id=:id and p.format_nilai is not null "
						+ "and not exists (select 1 from formatnilai f where f.id=p.format_nilai)")
						.setLong("id", tugas.getId().longValue()).executeUpdate();
			}
			if (tugas instanceof TugasPertemuan) {
				sesiPerbaikan.createSQLQuery("update tugas_pertemuan tp set format_nilai=null "
						+ "where tp.id=:id and tp.format_nilai is not null "
						+ "and not exists (select 1 from formatnilai f where f.id=tp.format_nilai)")
						.setLong("id", tugas.getId().longValue()).executeUpdate();
			}
			transaksi.commit();
			if (formatNilaiYatim) {
				tugas.setFormatNilai(null);
			}
		} catch (Exception e) {
			if (transaksi != null && transaksi.isActive()) {
				try { transaksi.rollback(); } catch (Exception abaikan) { }
			}
			ais.common.ErrorAuditUtil.record(e,
					"TugasMandiriHelper: gagal membersihkan format_nilai yatim tugas=" + tugas.getId());
		} finally {
			if (sesiPerbaikan != null && sesiPerbaikan.isOpen()) {
				try { sesiPerbaikan.clear(); } catch (Exception ig) { }
				try { sesiPerbaikan.disconnect(); } catch (Exception ig) { }
				try { sesiPerbaikan.close(); } catch (Exception ig) { }
			}
		}
	}

	/**
	 * Mengubah sebuah objek {@link FormatNilai} yang berasal dari komponen UI menjadi instance yang
	 * benar-benar masih ada di basis data pada sesi Hibernate yang sedang berjalan.
	 *
	 * <p><strong>Masalah yang diselesaikan.</strong> Daftar pilihan format nilai dibangun sekali saat
	 * dialog {@link #onUbahPerintahTugas(EventListener)} dibuka, lalu objek {@link FormatNilai} hasil
	 * query itu ditempelkan sebagai {@code value} pada masing-masing {@code Comboitem}. Objek tersebut
	 * bertahan di memori selama dialog terbuka — bisa berjam-jam. Dalam rentang itu master format nilai
	 * dapat dihapus atau diganti oleh pengguna lain, atau sesi Hibernate yang melahirkan objek itu
	 * sudah ditutup sehingga objeknya menjadi <em>detached</em>. Bila objek usang semacam itu langsung
	 * dipasang lewat {@code tugas.setFormatNilai(...)} lalu di-flush, Hibernate akan mencoba menulis
	 * foreign key ke baris yang tidak ada dan melemparkan {@code ConstraintViolationException} mentah,
	 * atau melemparkan {@code NonUniqueObjectException} karena ada dua instance dengan identitas sama
	 * di dalam satu persistence context.</p>
	 *
	 * <p><strong>Cara kerja.</strong> Metode ini memakai tiga cabang sederhana:</p>
	 * <ol>
	 *   <li>Argumen {@code null} dikembalikan sebagai {@code null} — pengguna memang memilih
	 *       "Tidak Ada", dan itu keadaan yang sah.</li>
	 *   <li>Argumen dengan {@code getId() == null} dikembalikan apa adanya — objek belum pernah
	 *       tersimpan sehingga tidak ada baris yang perlu diverifikasi.</li>
	 *   <li>Selain itu, dilakukan {@code session.get(FormatNilai.class, id)}. Pemakaian {@code get}
	 *       (bukan {@code load}) disengaja: {@code get} mengembalikan {@code null} bila baris tidak
	 *       ada, sedangkan {@code load} akan memberi proxy yang baru meledak belakangan saat
	 *       diakses.</li>
	 * </ol>
	 *
	 * <p><strong>Kontrak nilai balik.</strong> Nilai balik {@code null} untuk argumen non-{@code null}
	 * adalah sinyal bermakna: "format nilai yang dipilih sudah tidak ada". Setiap pemanggil wajib
	 * membedakan kedua kasus itu. Pola yang dipakai di seluruh berkas ini adalah membandingkan argumen
	 * dengan hasil: bila {@code pilihan != null && hasil == null}, maka pilihan dikosongkan
	 * ({@code setFormatNilai(null)}) dan {@link #tampilkanPeringatanFormatNilaiTidakValid(FormatNilai)}
	 * dipanggil agar pengguna tahu apa yang terjadi dan tidak menyangka nilainya tersimpan.</p>
	 *
	 * <p><strong>Pemanggil.</strong> Tiga titik: listener {@code onChange} pada kotak bobot persen,
	 * listener {@code onChange} pada combobox format nilai, dan tombol "Simpan Tugas". Ketiganya
	 * didahului {@link #bersihkanFormatNilaiYatim(Tugas)} yang membereskan foreign key yatim pada
	 * baris tugas itu sendiri; metode ini melengkapinya dengan memvalidasi objek yang datang dari
	 * sisi UI. Keduanya diperlukan — yang pertama membersihkan data lama di basis data, yang kedua
	 * mencegah data usang dari layar masuk kembali.</p>
	 *
	 * <p>Metode ini tidak mengubah state apa pun dan tidak membuka sesi baru; ia hanya membaca dari
	 * sesi yang diberikan pemanggil.</p>
	 *
	 * @param session     sesi Hibernate aktif milik request yang sedang berjalan.
	 * @param formatNilai kandidat format nilai dari komponen UI, boleh {@code null}.
	 * @return instance {@link FormatNilai} yang terikat pada {@code session} bila barisnya masih ada;
	 *         {@code null} bila argumen {@code null} atau bila barisnya sudah tidak ada di basis data;
	 *         objek argumen apa adanya bila belum pernah tersimpan ({@code id} masih {@code null}).
	 */
	private static FormatNilai ambilFormatNilaiValid(Session session, FormatNilai formatNilai) {
		if (formatNilai == null) {
			return null;
		}
		if (formatNilai.getId() == null) {
			return formatNilai;
		}
		return (FormatNilai) session.get(FormatNilai.class, formatNilai.getId());
	}

	/**
	 * Memberi tahu pengguna bahwa format nilai yang dipilihnya sudah tidak ada di basis data dan
	 * karenanya dikosongkan oleh sistem.
	 *
	 * <p>Dipanggil pada tiga titik yang sama dengan {@link #ambilFormatNilaiValid(Session, FormatNilai)},
	 * yaitu tepat setelah pemeriksaan {@code pilihan != null && hasil == null} bernilai benar. Pesan
	 * yang ditampilkan menyebutkan nama format nilai yang dipilih di dalam tanda kurung — diambil dari
	 * objek usang itu sendiri, yang masih menyimpan nama terakhir yang diketahui walaupun barisnya
	 * sudah lenyap — lalu menjelaskan bahwa pilihan dikosongkan agar Tugas/UTS/UAS tetap dapat
	 * disimpan, dan meminta pengguna memilih ulang format nilai yang masih aktif.</p>
	 *
	 * <p><strong>Mengapa memberi tahu, bukan diam-diam mengoreksi.</strong> Mengosongkan format nilai
	 * berarti nilai tugas ini tidak akan pernah masuk ke komponen nilai akhir mana pun. Bila koreksi
	 * dilakukan tanpa pemberitahuan, pengelola akan mengira penilaiannya sudah terhubung padahal
	 * tidak, dan kesalahannya baru ketahuan pada saat rekap nilai akhir. Karena itu pesan ini bersifat
	 * wajib, bukan opsional.</p>
	 *
	 * <p><strong>Penanganan interupsi.</strong> {@code MyMessageboxConfig.show} bersifat modal dan
	 * memblokir event thread ZK, sehingga dapat melemparkan {@link InterruptedException} bila desktop
	 * ditutup atau sesi berakhir saat kotak pesan masih terbuka. Pengecualian itu ditangkap dan
	 * ditindaklanjuti dengan {@code Thread.currentThread().interrupt()} — status interupsi
	 * dipasang kembali agar lapisan di atasnya tetap dapat mengenali bahwa thread ini pernah
	 * diinterupsi. Pengecualian sengaja tidak diteruskan ke atas karena kegagalan menampilkan pesan
	 * tidak boleh menggagalkan alur penyimpanan tugas yang sedang berjalan.</p>
	 *
	 * <p>Metode ini murni bersifat presentasi: ia tidak membaca maupun menulis basis data, dan tidak
	 * mengubah state objek apa pun. Argumen {@code null} ditangani dengan aman — label nama cukup
	 * dikosongkan.</p>
	 *
	 * @param formatNilai objek format nilai usang yang gagal divalidasi, dipakai hanya untuk mengambil
	 *                    namanya pada pesan; boleh {@code null}.
	 */
	private static void tampilkanPeringatanFormatNilaiTidakValid(FormatNilai formatNilai) {
		String label = formatNilai == null ? "" : (" (" + formatNilai.getNama() + ")");
		try {
			MyMessageboxConfig.show("Format nilai yang dipilih" + label
					+ " sudah tidak ditemukan di database. Sistem mengosongkan pilihan tersebut agar Tugas/UTS/UAS tetap dapat disimpan. "
					+ "Silakan pilih format nilai yang masih aktif, lalu simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Grid ZK yang memuat daftar berkas pengumpulan tugas ("Telah upload").
	 *
	 * <p>Dibuat sekali di dalam {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} dan
	 * dipakai ulang oleh {@link #reloadTugasFileContent(boolean)} sebagai target
	 * {@code setModel()}/{@code setRowRenderer()}. Setiap baris grid mewakili satu
	 * {@link TugasFileContent} — yaitu satu berkas yang dikirim oleh satu peserta — dan dirender
	 * oleh {@link DetailTugasFileContentRenderer} melalui {@link #displayRow(TugasFileContent, List, Component)}.</p>
	 *
	 * <p><strong>Peran sebagai penanda siklus hidup.</strong> Field ini sekaligus dipakai sebagai
	 * penanda "UI sudah dibangun". {@link #reloadTugasFileContent(boolean)} langsung {@code return}
	 * bila field ini masih {@code null}, sehingga pemanggilan reload yang datang dari timer atau dari
	 * callback upload tidak meledak ketika {@code createTugas} belum sempat membangun grid (mis. tugas
	 * belum berjudul sehingga blok pembangunan grid dilewati seluruhnya).</p>
	 *
	 * <p><strong>Kolom.</strong> Grid ini memiliki tiga kolom: (1) identitas peserta beserta kotak
	 * pencarian di header, (2) tanggal dan waktu pengumpulan, dan (3) kolom tunggal
	 * "Nilai &amp; Keterangan" yang menampung ringkasan nilai read-only plus tombol "Edit Nilai".
	 * Lebar kolom disesuaikan bila {@link #mobile} bernilai {@code true}.</p>
	 *
	 * <p><strong>Catatan konkurensi.</strong> Field ini adalah state per-instance dan per-desktop ZK.
	 * Satu instance {@code TugasMandiriHelper} melayani satu tab tugas pada satu desktop; jangan
	 * membagikan instance atau grid ini lintas sesi pengguna.</p>
	 */
	private MyGrid uploadTugasGrid;
	/**
	 * Mahasiswa yang sedang login, bila layar tugas ini dibuka dari sudut pandang peserta kuliah.
	 *
	 * <p>Diisi sekali lewat {@link #TugasMandiriHelper(Mahasiswa, BiodataCalonMahasiswa)} dan tidak
	 * pernah diubah lagi. Bernilai {@code null} bila yang membuka layar adalah dosen, admin, atau
	 * pegawai — dan juga {@code null} untuk beberapa jalur pemanggil lain (lihat catatan di bawah).</p>
	 *
	 * <p><strong>Cara field ini dipakai.</strong></p>
	 * <ul>
	 *   <li>Sebagai gerbang visibilitas tombol pengelolaan: {@code rubah} ("Ubah Instruksi Tugas"),
	 *       {@code ambil} ("Ambil Tugas"), {@link #hapus}, dan {@link #buttonMasukkanNilai} hanya
	 *       ditampilkan ketika field ini {@code null}.</li>
	 *   <li>Sebagai identitas pengunggah pada {@code Common.uploadTugas(tugas, mahasiswa,
	 *       biodataCalonMahasiswa, listener)} di jalur perguruan tinggi.</li>
	 *   <li>Sebagai kunci pemeriksaan {@code tugas.getMhsBolehUploadUlang()} — bila id mahasiswa ini
	 *       terdaftar di sana, tombol {@link #upload} tetap ditampilkan walaupun jendela waktu tugas
	 *       sudah lewat.</li>
	 *   <li>Sebagai penentu apakah kartu kontak (HP/e-mail) peserta lain ditampilkan pada
	 *       {@link #displayRow(TugasFileContent, List, Component)}.</li>
	 * </ul>
	 *
	 * <p><strong>Perbedaan penting dengan {@link #peserta}.</strong> Field ini berasal dari
	 * <em>argumen konstruktor</em>, bukan dari pengguna yang sedang login. {@link #peserta} dihitung
	 * ulang di dalam {@code createTugas} langsung dari {@link #tbmuser} dan karenanya mencakup pula
	 * siswa, calon siswa, calon mahasiswa, dan peserta kursus. Untuk keputusan yang menyangkut peran
	 * pengguna, {@link #peserta} (atau {@link #bolehKelolaTugas(Tbmuser)}) adalah sumber kebenaran yang
	 * tepat; field ini hanya menggambarkan konteks pemanggilan.</p>
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Calon mahasiswa (pendaftar PMB) yang sedang login, bila layar tugas dibuka dari konteks
	 * penerimaan mahasiswa baru.
	 *
	 * <p>Kembaran {@link #mahasiswa} untuk jalur PMB: dipakai pada ujian/tugas yang melekat pada
	 * {@code JadwalUjianPMB}, ketika peserta belum berstatus mahasiswa penuh sehingga belum memiliki
	 * baris {@link Mahasiswa}. Diisi sekali lewat
	 * {@link #TugasMandiriHelper(Mahasiswa, BiodataCalonMahasiswa)} dan tidak pernah diubah.</p>
	 *
	 * <p>Perannya sejajar dengan {@link #mahasiswa}: menjadi salah satu operand gerbang visibilitas
	 * tombol pengelolaan, identitas pengunggah pada {@code Common.uploadTugas}, dan kunci pencarian di
	 * {@code tugas.getMhsBolehUploadUlang()}. Karena {@link TugasFileContent} menyimpan pemilik berkas
	 * pada empat kolom terpisah ({@code mahasiswa}, {@code siswa}, {@code biodataCalonMahasiswa},
	 * {@code calonSiswa}), id calon mahasiswa di sini dibandingkan dengan
	 * {@code TugasFileContent.getBiodataCalonMahasiswa()}, bukan dengan kolom mahasiswa.</p>
	 *
	 * <p>Sama seperti {@link #mahasiswa}, field ini menggambarkan konteks pemanggilan, bukan peran
	 * pengguna yang login; lihat {@link #peserta} untuk penanda peran yang sebenarnya.</p>
	 */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/**
	 * Entitas tugas yang sedang ditampilkan dan dikelola oleh instance helper ini.
	 *
	 * <p>{@link Tugas} adalah antarmuka yang diimplementasikan oleh dua entitas berbeda, dan helper ini
	 * bekerja untuk keduanya tanpa membedakan pemanggilnya:</p>
	 * <ul>
	 *   <li>{@link Pertemuan} — tugas yang melekat langsung pada satu pertemuan perkuliahan atau satu
	 *       pertemuan jadwal pelajaran. Judul, instruksi, jendela waktu, dan format nilai tugas
	 *       disimpan sebagai kolom pada baris pertemuan itu sendiri.</li>
	 *   <li>{@link TugasPertemuan} — sub-tugas yang menggantung pada sebuah {@link Pertemuan} lewat
	 *       kolom {@code pertemuan}. Sebuah pertemuan dapat memiliki banyak {@code TugasPertemuan},
	 *       dan hanya varian inilah yang dapat dipindahkan ke pertemuan lain lewat combo "Pertemuan"
	 *       pada {@link #onUbahPerintahTugas(EventListener)}.</li>
	 * </ul>
	 *
	 * <p><strong>Waktu pengisian.</strong> Berbeda dengan {@link #mahasiswa} dan
	 * {@link #biodataCalonMahasiswa} yang datang dari konstruktor, field ini baru diisi di awal
	 * {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} dari argumen {@code tugas}, lalu
	 * ditulis ulang beberapa kali oleh listener {@code ubahTugas} dan {@link #buttonMasukkanNilai}
	 * ketika dialog "Ubah Instruksi Tugas" mengembalikan entity yang sudah disegarkan.</p>
	 *
	 * <p><strong>Data yang disimpan pada entity ini dan dibaca helper.</strong> {@code judultugas},
	 * {@code isitugas}, {@code mulai}, {@code selesai}, {@code formatNilai} + {@code prosentase}
	 * (penilaian non-OBE), {@code formatNilais} (peta JSON Sub-CPMK untuk penilaian OBE),
	 * {@code jenisItemPenilaianSiswa} + {@code grupPenilaian} + {@code grupKategoriItemPenilaianSiswa}
	 * (penilaian jenjang sekolah), {@code syaratMengumpulkanTugas}, {@code keteranganNilai} (peta JSON
	 * nilai dan keterangan per peserta), {@code mhsYgTidakIkut} dan {@code mhsBolehUploadUlang}
	 * (daftar id berformat {@code ,id,} yang digabung menjadi satu kolom teks).</p>
	 *
	 * <p><strong>Catatan tentang {@code mhsYgTidakIkut}/{@code mhsBolehUploadUlang}.</strong> Kedua
	 * kolom itu adalah daftar id yang dirangkai sebagai teks dengan pembatas koma di kedua sisi setiap
	 * id, sehingga pengujian keanggotaan selalu ditulis {@code contains("," + id + ",")} — bukan
	 * pemecahan string. Konsekuensinya penghapusan anggota harus membuang pola {@code ,id,} DAN pola
	 * {@code id} telanjang, persis seperti yang dilakukan listener checkbox pada tab
	 * "Peserta yg tdk perlu ikt".</p>
	 */
	private Tugas tugas;

	/**
	 * Membuat helper Tugas Mandiri untuk satu konteks tampilan.
	 *
	 * <p>Konstruktor ini sengaja dibuat sangat ringan: ia hanya menyimpan dua argumen ke field
	 * {@link #mahasiswa} dan {@link #biodataCalonMahasiswa}, tanpa menyentuh basis data, tanpa membaca
	 * pengguna yang sedang login, dan tanpa membangun satu pun komponen ZK. Seluruh pekerjaan berat
	 * baru terjadi ketika {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} dipanggil.
	 * Karena itu instansiasi helper ini aman dilakukan di dalam loop render maupun di dalam listener.</p>
	 *
	 * <p><strong>Arti kedua argumen.</strong> Keduanya menyatakan "dari sudut pandang siapa layar ini
	 * dibuka", bukan "siapa yang login":</p>
	 * <ul>
	 *   <li>Keduanya {@code null} — layar dibuka dari sudut pandang pengelola (dosen, admin, pegawai)
	 *       ATAU dari jalur pemanggil yang memang tidak meneruskan identitas peserta. Contoh jalur
	 *       kedua: {@code DetailpertemuanHelper} memanggil {@code new PertemuanHelper(null, null)} dan
	 *       {@code HasilUjianSiswaHelper} memanggil {@code new PertemuanHelper()} tanpa argumen, yang
	 *       pada gilirannya membangun helper ini dengan dua {@code null} meskipun yang login adalah
	 *       seorang siswa.</li>
	 *   <li>{@code mahasiswa} terisi — layar dibuka oleh/untuk seorang mahasiswa peserta kuliah.
	 *       Tombol pengelolaan disembunyikan dan seluruh tab pengelolaan tidak dibangun.</li>
	 *   <li>{@code biodataCalonMahasiswa} terisi — layar dibuka oleh/untuk seorang calon mahasiswa
	 *       pada konteks ujian/tugas PMB.</li>
	 * </ul>
	 *
	 * <p><strong>Peringatan pemakaian.</strong> Karena kombinasi "dua {@code null}" tidak identik
	 * dengan "yang login adalah pengelola", kedua field ini tidak layak dipakai sendirian sebagai
	 * gerbang otorisasi. Penanda peran yang benar dihitung ulang di dalam {@code createTugas} sebagai
	 * {@link #peserta} dari {@link #tbmuser} hasil {@code Common.getCurrentUser()}, dan tersedia pula
	 * lewat {@link #bolehKelolaTugas(Tbmuser)} serta {@link #bolehUpload(Tbmuser)}.</p>
	 *
	 * @param mahasiswa              mahasiswa peserta yang menjadi sudut pandang tampilan, atau
	 *                               {@code null} bila layar dibuka bukan sebagai mahasiswa.
	 * @param biodataCalonMahasiswa  calon mahasiswa (PMB) yang menjadi sudut pandang tampilan, atau
	 *                               {@code null} bila layar dibuka bukan sebagai calon mahasiswa.
	 */
	public TugasMandiriHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Tombol toolbar "Masukkan Nilai ke ..." yang menyalin nilai tugas ke komponen nilai akhir.
	 *
	 * <p>Tombol ini tidak selalu ada, dan bila ada, labelnya berbeda-beda tergantung mode penilaian
	 * yang berlaku pada tugas. {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} membangun
	 * salah satu dari tiga varian berikut, dan hanya untuk pengguna non-pelajar:</p>
	 * <ol>
	 *   <li><em>OBE</em> — bila {@code tugas.getFormatNilais()} terisi dan kurikulum perkuliahan
	 *       berstatus OBE. Label: "Masukkan nilai ke nilai akhir". Aksinya memanggil
	 *       {@code GradingHelper.hitungNilaiBerdasarkanFormatNilaiObe(perkuliahan, tugas.getFormatNilais())},
	 *       yang membaca seluruh Sub-CPMK terpilih dari peta JSON pada kolom {@code formatNilais}.</li>
	 *   <li><em>Non-OBE perguruan tinggi</em> — bila {@code tugas.getFormatNilai()} terisi. Label
	 *       memuat nama {@link FormatNilai} tujuan. Aksinya memanggil
	 *       {@code GradingHelper.hitungNilaiBerdasarkanFormatNilai(perkuliahan, tugas.getFormatNilai())}.</li>
	 *   <li><em>Jenjang sekolah</em> — bila {@code tugas.getJenisItemPenilaianSiswa()} terisi. Label
	 *       memuat nama {@link JenisItemPenilaianSiswa} tujuan. Aksinya memanggil
	 *       {@code GradingHelper.hitungNilaiBerdasarkanJenisItemPenilaianSiswa(...)} dengan trio
	 *       jenis/kategori/grup penilaian.</li>
	 * </ol>
	 *
	 * <p><strong>Auto-simpan sebelum menghitung.</strong> Varian (2) dan (3) melakukan penyimpanan
	 * paksa lebih dahulu: {@code session.refresh(tugas)}, invalidasi cache
	 * {@code tugas.belum("tugas_file_content_" + ...)}, lalu menulis {@link #jsonObjectTugas} ke kolom
	 * {@code keteranganNilai} dan {@code Common.refreshUpdate}. Langkah ini diperlukan karena nilai yang
	 * diketik pengelola pada popup "Edit Nilai" hanya dipegang di memori {@link #jsonObjectTugas};
	 * tanpa auto-simpan, {@code GradingHelper} akan membaca kolom {@code keteranganNilai} versi lama
	 * dan menghasilkan nilai akhir yang tertinggal satu langkah.</p>
	 *
	 * <p><strong>Alasan field ini disimpan sebagai state.</strong> Karena tiga varian di atas ditulis
	 * sebagai rangkaian {@code if}/{@code else if} yang tidak saling eksklusif secara sempurna (varian
	 * jenjang sekolah diuji lewat {@code if} terpisah), sebuah tugas dapat memenuhi lebih dari satu
	 * syarat. Field ini menyimpan tombol yang sudah terlanjur dibuat sehingga varian berikutnya dapat
	 * memanggil {@code setVisible(false)} dan {@code detach()} lebih dahulu — mencegah dua tombol
	 * "Masukkan Nilai" muncul berdampingan pada toolbar yang sama.</p>
	 */
	private MyToolbarbutton buttonMasukkanNilai;
	/**
	 * Tombol "Upload Tugas" milik peserta.
	 *
	 * <p>Dibuat di awal {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)}, dipasang ke
	 * toolbar utama, lalu <em>dipindahkan</em> ke dalam kartu status peserta oleh
	 * {@link #tempelTombolUploadUtama(Groupbox)} agar mudah ditemukan dan tidak tertutup tombol lain
	 * di layar sempit. Karena tombol yang sama dipakai ulang (bukan dibuat baru), seluruh aturan
	 * visibilitas dan listener yang sudah terpasang tetap berlaku setelah pemindahan.</p>
	 *
	 * <p><strong>Aturan visibilitas.</strong> Nilai dasarnya adalah "tugas berjudul DAN sekarang berada
	 * di dalam jendela waktu tugas":</p>
	 * <ul>
	 *   <li>{@code judultugas} tidak boleh kosong — judul kosong berarti pertemuan ini memang belum
	 *       memiliki tugas sama sekali;</li>
	 *   <li>{@code selesai} harus {@code null} atau masih di masa depan;</li>
	 *   <li>{@code mulai} harus {@code null} atau sudah lewat.</li>
	 * </ul>
	 * <p>Setelah aturan dasar itu, ada satu jalur pengecualian: bila id {@link #mahasiswa} atau
	 * {@link #biodataCalonMahasiswa} tercantum pada {@code tugas.getMhsBolehUploadUlang()}, tombol
	 * dipaksa tampil kembali meskipun jendela waktu sudah tertutup. Inilah mekanisme "izin upload
	 * ulang" yang diberikan pengelola dari tab "Peserta yg tdk perlu ikt".</p>
	 *
	 * <p><strong>Perilaku klik.</strong> Listener {@code ON_CLICK} lebih dahulu memeriksa
	 * {@link #syaratAlert}; bila himpunan itu tidak kosong, seluruh pesan syarat ditampilkan dan proses
	 * upload dibatalkan. Bila lolos, ia bercabang berdasarkan konteks: jalur sekolah memanggil
	 * {@code Common.uploadTugas(tugas, tbmuser.getSiswa(), tbmuser.getCalonSiswa(), listener)},
	 * sedangkan jalur perguruan tinggi memanggil
	 * {@code Common.uploadTugas(tugas, mahasiswa, biodataCalonMahasiswa, listener)}. Kedua jalur
	 * menutup dengan invalidasi cache dan {@link #reloadTugasFileContent()}.</p>
	 */
	private MyToolbarbutton upload;
	/**
	 * Combobox pemilih {@link SyaratUjian} — prasyarat yang harus dipenuhi peserta sebelum boleh
	 * mengumpulkan tugas ini.
	 *
	 * <p>Hanya dibuat di dalam dialog {@link #onUbahPerintahTugas(EventListener)}, bukan di layar
	 * utama. Isinya diisi oleh {@code Common.insertComboDanSemua(...)} dengan seluruh {@link SyaratUjian}
	 * yang aktif ditambah satu item khusus "== Tanpa Syarat Mengikuti Ujian ==" sebagai pilihan kosong.
	 * Nilai terpilih disalin ke {@code tugas.setSyaratMengumpulkanTugas(...)} ketika tombol
	 * "Simpan Tugas" ditekan.</p>
	 *
	 * <p><strong>Penguncian oleh admin.</strong> Sebuah {@link SyaratUjian} dapat ditandai
	 * {@code hanyaBolehDiubahOlehAdmin}. Listener {@code listenerSyarat} — yang juga dijalankan sekali
	 * secara manual saat dialog dibuka — akan me-{@code setDisabled(true)} combobox ini bila syarat
	 * terpilih memiliki flag tersebut DAN pengguna yang login adalah dosen atau mahasiswa (atau
	 * {@link Tbmuser} tidak dapat dibaca sama sekali). Sebuah baris keterangan
	 * "Persyaratan ini hanya boleh diubah oleh admin" ikut ditampilkan/disembunyikan mengikuti kondisi
	 * yang sama.</p>
	 *
	 * <p><strong>Hubungan dengan {@link #syaratAlert}.</strong> Syarat yang dipilih di sini
	 * dievaluasi di dua tempat berbeda. Pada layar utama, {@code Tugas.tampilanSyarat(...)} /
	 * {@code Tugas.tampilanSyaratReadonly(...)} mengisi {@link #syaratAlert} dengan pesan pelanggaran.
	 * Pada panel instruksi, {@code SyaratUjianAction.checkSyaratSyaratUjian(...)} dipanggil untuk
	 * mahasiswa yang login guna menentukan apakah isi tugas boleh ditampilkan sama sekali.</p>
	 */
	private Combobox syaratMengumpulkanTugas;
	/**
	 * Tombol "Hapus Tugas" — mengosongkan perintah tugas pada pertemuan, bukan menghapus baris entity.
	 *
	 * <p><strong>Semantik penghapusan.</strong> Karena {@link Tugas} pada varian {@link Pertemuan}
	 * adalah pertemuan itu sendiri, baris entity tidak boleh dihapus. Yang dilakukan listener tombol
	 * ini setelah konfirmasi adalah <em>mengosongkan</em> seluruh atribut tugas pada baris tersebut:
	 * {@code judultugas} dan {@code isitugas} dijadikan string kosong, sedangkan {@code formatNilai},
	 * {@code syaratMengumpulkanTugas}, {@code mulai}, dan {@code selesai} dijadikan {@code null}.
	 * Karena hampir semua gerbang tampilan di helper ini bertumpu pada "judultugas kosong berarti tidak
	 * ada tugas", pengosongan judul sudah cukup untuk membuat tugas lenyap dari tampilan.</p>
	 *
	 * <p><strong>Lampiran ikut diputus.</strong> Setelah entity disimpan, listener membuka sesi
	 * streaming dan menjalankan {@code update lampiran_lain set ref = -111111111111 where ref = ...
	 * and jenis = 'TUGAS_MANDIRI_PERKULIAHAN'}. Nilai {@code -111111111111} adalah penanda "yatim"
	 * yang membuat lampiran tidak lagi terhubung ke tugas mana pun tanpa benar-benar menghapus berkas
	 * fisiknya.</p>
	 *
	 * <p><strong>Yang TIDAK ikut dihapus.</strong> Baris {@link TugasFileContent} milik peserta —
	 * yaitu berkas yang sudah terlanjur dikumpulkan — tidak disentuh sama sekali, demikian pula
	 * {@code keteranganNilai}, {@code mhsYgTidakIkut}, dan {@code mhsBolehUploadUlang}. Bila kemudian
	 * pertemuan yang sama diberi judul tugas lagi, pengumpulan dan nilai lama akan muncul kembali.</p>
	 *
	 * <p><strong>Visibilitas.</strong> Tombol disembunyikan untuk seluruh peran pelajar. Perhatikan
	 * bahwa gerbangnya dievaluasi dua kali dengan hasil berbeda: saat pertama dipasang ke toolbar,
	 * syarat "{@code judultugas} tidak kosong" sengaja dilepas agar pengelola tetap dapat mengelola
	 * pertemuan yang belum bertugas; sesudah dialog "Ubah Instruksi Tugas" disimpan, gerbang
	 * dievaluasi ulang lengkap dengan syarat judul tidak kosong.</p>
	 */
	private MyToolbarbutton hapus;

	/**
	 * Perkuliahan (kelas perguruan tinggi) tempat tugas ini bernaung, atau {@code null} bila tugas
	 * berasal dari jenjang sekolah.
	 *
	 * <p>Diturunkan di awal {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} dari entity
	 * {@link Tugas} yang diterima: untuk {@link Pertemuan} diambil langsung lewat
	 * {@code getPerkuliahan()}, sedangkan untuk {@link TugasPertemuan} diambil dari pertemuan induknya
	 * ({@code ambilPertemuan().getPerkuliahan()}). Bila entity bukan salah satu dari keduanya, field
	 * ini dikosongkan.</p>
	 *
	 * <p><strong>Field ini menentukan cabang penilaian.</strong> Bersama {@link #jadwalPelajaran} ia
	 * memilih model penilaian mana yang dipakai di seluruh helper:</p>
	 * <ul>
	 *   <li>{@code perkuliahan != null} dan kurikulumnya OBE — penilaian per Sub-CPMK. Daftar
	 *       {@link FormatNilai} yang dipilih disimpan sebagai peta JSON pada
	 *       {@code tugas.getFormatNilais()}, dan nilai per peserta disimpan dengan kunci
	 *       {@code <idPeserta>_<jenis>_nilai_<idFormatNilai>} pada {@code keteranganNilai}.</li>
	 *   <li>{@code perkuliahan != null} dan kurikulumnya non-OBE — penilaian tunggal: satu
	 *       {@link FormatNilai} pada {@code tugas.getFormatNilai()} plus bobot persen pada
	 *       {@code tugas.getProsentase()}.</li>
	 *   <li>{@code jadwalPelajaran != null} — penilaian jenjang sekolah lewat
	 *       {@link JenisItemPenilaianSiswa}.</li>
	 * </ul>
	 *
	 * <p><strong>Efek "dikunci".</strong> Bila {@code perkuliahan.getDikunci()} tidak {@code null},
	 * seluruh kontrol pemilihan format nilai pada dialog ubah instruksi di-{@code setDisabled(true)}
	 * dan label "Penilaian sudah dikunci" ditampilkan. Penguncian ini murni pada lapisan UI dialog;
	 * ia tidak menutup jalur penyimpanan lain.</p>
	 *
	 * <p>Field ini juga menjadi sumber jumlah peserta pada panel ringkasan
	 * ({@code perkuliahan.ambilMahasiswa()}) serta konteks bagi
	 * {@code Common.getFormatNilais(session, perkuliahan)} dan seluruh pemanggilan
	 * {@code GradingHelper}.</p>
	 */
	private Perkuliahan perkuliahan = null;
	/**
	 * Jadwal pelajaran (kelas jenjang sekolah) tempat tugas ini bernaung, atau {@code null} bila tugas
	 * berasal dari perguruan tinggi.
	 *
	 * <p>Pasangan {@link #perkuliahan} untuk jenjang sekolah, diturunkan dengan cara yang sama di awal
	 * {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)}. Kedua field tidak pernah terisi
	 * bersamaan pada praktiknya, karena sebuah {@link Pertemuan} hanya menunjuk salah satu induk.</p>
	 *
	 * <p><strong>Model penilaian sekolah.</strong> Bila field ini terisi, dialog ubah instruksi
	 * membangun combobox {@link JenisItemPenilaianSiswa} alih-alih combobox {@link FormatNilai}.
	 * Daftar isinya dirakit berjenjang dari basis data:
	 * {@link JenisPenilaian} (diambil dari mata pelajaran, dan ditimpa oleh kurikulum sekolah bila
	 * kurikulum menentukan jenis penilaian sendiri) &rarr; {@link GrupPenilaian} lewat
	 * {@link DetailJenisPenilaian} &rarr; {@link GrupKategoriItemPenilaianSiswa} lewat
	 * {@link DetailGrupPenilaian} &rarr; {@link KategoriItemPenilaianSiswa} lewat
	 * {@link DetailGrupKategoriItemPenilaianSiswa} &rarr; {@link JenisItemPenilaianSiswa}. Hanya jenis
	 * item bertipe input {@code ANGKA} atau {@code TEXT_ANGKA} yang ditawarkan, karena nilai tugas
	 * selalu numerik.</p>
	 *
	 * <p><strong>Penyaringan tingkat kelas.</strong> Baik {@link GrupPenilaian} maupun
	 * {@link GrupKategoriItemPenilaianSiswa} dapat memiliki atribut {@code khususTingkat}. Bila
	 * atribut itu terisi dan berbeda dari tingkat {@link KelasSiswa} pada jadwal pelajaran ini, grup
	 * yang bersangkutan dilewati sehingga tidak muncul sebagai pilihan.</p>
	 *
	 * <p>Selain menentukan combobox, field ini juga menjadi argumen pertama
	 * {@code GradingHelper.hitungNilaiBerdasarkanJenisItemPenilaianSiswa(...)} — baik dari tombol
	 * "Sinkronkan Nilai" di dalam dialog maupun dari tombol "Masukkan Nilai ke ..." di toolbar
	 * utama.</p>
	 */
	private JadwalPelajaran jadwalPelajaran = null;
	/**
	 * Pertemuan yang menjadi konteks absensi dan daftar peserta untuk tugas ini.
	 *
	 * <p>Untuk {@link Tugas} berupa {@link Pertemuan}, field ini adalah entity yang sama dengan
	 * {@link #tugas}. Untuk {@link TugasPertemuan}, field ini adalah pertemuan induk hasil
	 * {@code ambilPertemuan()}. Dengan begitu seluruh operasi yang bersifat "per pertemuan" —
	 * daftar peserta, absensi, rekap pembelajaran — selalu punya titik jangkar yang benar walaupun
	 * tugasnya berupa sub-tugas.</p>
	 *
	 * <p><strong>Dipakai untuk apa saja.</strong></p>
	 * <ul>
	 *   <li><em>Daftar peserta</em> — {@code pa.ambilMahasiswa()} dan {@code pa.ambilSiswa()} menjadi
	 *       model grid pada tab "Belum upload" dan tab "Peserta yg tdk perlu ikt", sumber angka pada
	 *       tab "Statistik", serta basis iterasi seluruh aksi kehadiran massal.</li>
	 *   <li><em>Absensi</em> — {@code pa.populate(idMahasiswa, statusabsensi, keterangan, ..., mulai,
	 *       sampai, "Mahasiswa")} adalah satu-satunya jalur penulisan kehadiran yang dipakai helper
	 *       ini, dengan jam mulai/selesai diambil dari {@code retreiveAbsensiMulai/Sampai} dan jatuh
	 *       kembali ke {@code getWaktuMulai()}/{@code getWaktuSelesai()} bila kosong.</li>
	 *   <li><em>Rekap lintas pertemuan</em> — {@code pa.ambilVOPembelajaran()} menjadi argumen
	 *       {@link RekapHasilTugasPerVoPertemuan} (tab "Rekap Tugas") dan
	 *       {@link RekapHasilTugasMahasiswa} (tombol "Rekap Semua Tugas" milik peserta).</li>
	 *   <li><em>Gerbang konteks ujian</em> — {@code pa.getJadwalUjianPMB()} dan
	 *       {@code pa.getJadwalUjianPSB()} harus keduanya {@code null} agar tombol Download Nilai,
	 *       Upload Nilai, dan tombol-tombol kehadiran ditampilkan. Pada konteks ujian penerimaan,
	 *       nilai dan absensi dikelola modul lain sehingga tombol-tombol itu tidak relevan.</li>
	 *   <li><em>Nama berkas ZIP</em> — {@code pa.getPertemuanKe()} dipakai menyusun nama file unduhan
	 *       massal {@code Tugas_untuk_pertemuan_ke_N.zip}.</li>
	 * </ul>
	 */
	private Pertemuan pa = null;
	/**
	 * Komponen paging ZK untuk grid daftar pengumpulan {@link #uploadTugasGrid}.
	 *
	 * <p>Dipasang di region South milik borderlayout kolom kanan, berbagi wadah {@code Hbox} dengan
	 * toolbar Simpan/Batal. Pembagian wadah ini disengaja: sebuah {@code South} pada ZK hanya boleh
	 * memiliki satu anak langsung, sehingga paging tidak dapat di-{@code setParent} langsung ke
	 * {@code South} bila toolbar sudah lebih dahulu menempati posisi itu.</p>
	 *
	 * <p>Field ini diteruskan sebagai argumen ke
	 * {@code tugas.ambilTugasFileContentTotal(treemap, cari, paging, 500, refresh)} sehingga entity
	 * {@link Tugas} sendiri yang mengatur offset dan jumlah total halaman. Listener {@code onPaging}
	 * cukup memanggil {@link #reloadTugasFileContent()}, yang akan membaca ulang halaman aktif.</p>
	 *
	 * <p>Perhatikan bahwa batas 500 baris per pengambilan bersifat tetap dan berlaku juga pada jalur
	 * Download Nilai serta Upload Nilai — keduanya memanggil
	 * {@code ambilTugasFileContentTotal} dengan angka yang sama. Untuk kelas berukuran sangat besar,
	 * berkas Excel nilai yang diunduh maupun diunggah hanya mencakup halaman yang termuat.</p>
	 */
	private Paging paging;
	/**
	 * Pengguna yang sedang login, sebagaimana dibaca dari {@code Common.getCurrentUser()}.
	 *
	 * <p>Berbeda dengan {@link #mahasiswa} dan {@link #biodataCalonMahasiswa} yang berasal dari
	 * argumen konstruktor, field ini benar-benar mencerminkan sesi yang aktif. Ia diisi di awal
	 * {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} dan diisi ulang setiap kali
	 * {@link #reloadTugasFileContent(boolean)} dijalankan, sehingga tetap segar sepanjang umur
	 * tampilan.</p>
	 *
	 * <p><strong>Bentuk pemeriksaan peran.</strong> {@link Tbmuser} menyimpan tautan opsional ke lima
	 * jenis pelajar sekaligus — {@code getMahasiswa()}, {@code getSiswa()},
	 * {@code getBiodataCalonMahasiswa()}, {@code getCalonSiswa()}, dan {@code getPesertaKursus()} —
	 * dan seorang pengelola adalah pengguna yang kelimanya {@code null}. Karena itu hampir setiap
	 * gerbang di helper ini berbentuk rantai panjang perbandingan {@code == null}. Bentuk ringkas dan
	 * konsisten dari rantai tersebut tersedia lewat {@link #bolehKelolaTugas(Tbmuser)} dan
	 * {@link #bolehUpload(Tbmuser)}, sedangkan {@link #peserta} menyimpan hasil evaluasinya untuk
	 * dipakai berulang.</p>
	 *
	 * <p><strong>Pemakaian di luar gerbang.</strong> Field ini juga dipakai sebagai identitas penulis
	 * pada jejak audit: {@code Common.generateOlehId(tbmuser)} dan nama pengguna disalin ke kolom
	 * {@code olehId}/{@code oleh} saat menyalin lampiran ("Ambil Tugas") maupun saat membuat baris
	 * {@link TugasFileContent} kosong lewat {@link #anggapSemuaSudahUpload(Tugas, Pertemuan, EventListener)}.
	 * Selain itu ia menjadi pemilik kredensial pada integrasi Google Drive
	 * ({@code new GDriveUtilPerPengguna(tbmuser)} dan {@code fileFoto.setGdriveUsername(tbmuser.getUserId())}).</p>
	 */
	private Tbmuser tbmuser;
	/**
	 * Borderlayout dalam yang membungkus grid pengumpulan beserta region North dan South-nya.
	 *
	 * <p>Layout ini berada di dalam Center milik borderlayout kolom kanan. Isinya berbeda menurut
	 * peran:</p>
	 * <ul>
	 *   <li><em>Peserta</em> — layout dipasang langsung ke Center; hanya berisi Center (grid) dan,
	 *       setelah {@link #reloadTugasFileContent(boolean)} berjalan, sebuah North berisi kartu
	 *       "Tugas yang Anda Upload".</li>
	 *   <li><em>Pengelola</em> — layout dipasang ke dalam panel tab pertama ("Telah upload") dari
	 *       {@code MyButtonTabbox}, dan memperoleh South berisi toolbar Simpan/Batal.</li>
	 * </ul>
	 *
	 * <p><strong>Alasan field ini disimpan.</strong> {@link #reloadTugasFileContent(boolean)}
	 * dijalankan berkali-kali sepanjang umur layar dan perlu membuat atau memakai ulang region North
	 * pada layout yang sama. Karena {@code Borderlayout} hanya menerima satu North dan satu South,
	 * kode selalu menulis {@code myborderlayoutlagi.getNorth() == null ? new North() :
	 * myborderlayoutlagi.getNorth()} — pola "pakai yang sudah ada, buat hanya bila belum ada" yang
	 * mencegah kesalahan runtime "Only one north child is allowed". Pola identik dipakai untuk South
	 * saat paging ditambahkan.</p>
	 *
	 * <p>Nama field yang berakhiran "lagi" berarti "yang satu lagi": ia membedakan layout dalam ini
	 * dari borderlayout luar yang dibangun sebagai variabel lokal pada {@code createTugas}.</p>
	 */
	private Borderlayout myborderlayoutlagi;

	/**
	 * Kumpulan pesan pelanggaran prasyarat yang menghalangi peserta mengumpulkan tugas.
	 *
	 * <p>Himpunan ini diisi oleh {@code Tugas.tampilanSyarat(...)} (jalur pengelola) atau
	 * {@code Tugas.tampilanSyaratReadonly(...)} (jalur peserta) yang dipanggil di ujung
	 * {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)}. Kedua metode itu mengevaluasi
	 * {@link SyaratUjian} yang terpasang pada tugas terhadap keadaan peserta yang sedang login —
	 * misalnya tunggakan pembayaran, kehadiran minimum, atau kelengkapan berkas — dan menuliskan satu
	 * kalimat penjelasan untuk setiap syarat yang tidak terpenuhi.</p>
	 *
	 * <p><strong>Cara pesan dipakai.</strong> Listener klik tombol {@link #upload} memeriksa himpunan
	 * ini lebih dahulu. Bila tidak kosong, seluruh pesan digabung dengan pemisah baris ganda,
	 * ditampilkan sebagai {@code MyMessageboxConfig} bertipe peringatan, lalu proses upload dibatalkan
	 * dengan {@code return}. Dengan demikian himpunan ini berfungsi sebagai gerbang terakhir sebelum
	 * berkas benar-benar diterima.</p>
	 *
	 * <p><strong>Mengapa {@link Set} dan bukan {@link List}.</strong> Evaluasi syarat dapat dipicu
	 * berulang kali oleh tombol "Refresh Syarat" tanpa membersihkan himpunan lebih dahulu. Pemakaian
	 * {@link HashSet} membuat pesan yang sama tidak menumpuk menjadi duplikat pada kotak peringatan.
	 * Konsekuensinya, urutan pesan tidak dijamin stabil antar-pemanggilan.</p>
	 */
	private Set<String> syaratAlert = new HashSet<String>();

	/**
	 * Membuka dialog modal untuk mengubah perintah dan instruksi tugas mandiri.
	 *
	 * <p>Dialog ini memungkinkan pengelola (dosen/admin) untuk mengubah judul tugas, isi
	 * instruksi (via CKEditor rich text), tanggal mulai dan selesai, format penilaian
	 * (FormatNilai/OBE Sub-CPMK), serta syarat mengumpulkan tugas. Mahasiswa/siswa hanya
	 * dapat melihat informasi tanggal dalam format read-only. Dialog juga mendukung
	 * pemindahan TugasPertemuan ke pertemuan lain dalam pembelajaran yang sama (VOPembelajaran),
	 * selama yang login bukan pelajar. Setelah disimpan, eventListener dipanggil untuk
	 * me-refresh tampilan tugas pada tab yang memanggilnya.</p>
	 *
	 * <h3>Susunan dialog</h3>
	 * <p>Dialog dibangun sebagai {@code MyWindow} modal setinggi 95% dan selebar 950 piksel, berisi
	 * satu {@code Borderlayout} dengan Center berupa grid form dan South berupa toolbar Batal/Simpan.
	 * Baris form disusun berurutan sebagai berikut.</p>
	 * <ol>
	 *   <li><strong>Pindahkan ke pertemuan lain.</strong> Hanya dibangun bila pertemuan induk memiliki
	 *       {@code VOPembelajaran} dan yang login bukan pelajar. Combobox diisi seluruh pertemuan pada
	 *       pembelajaran yang sama; bila cache lokasi-pertemuan kosong, daftar dipaksa dimuat ulang
	 *       dari basis data lewat {@code ambilPertemuan(true)} agar dropdown tidak tampil kosong.
	 *       Pertemuan yang sedang aktif selalu ditambahkan dan dipilih walaupun tidak termuat dalam
	 *       daftar. Combobox aktif hanya untuk {@link TugasPertemuan}; untuk {@link Pertemuan} ia
	 *       ditampilkan ternonaktif disertai keterangan bahwa tugas melekat pada pertemuannya sendiri.
	 *       Perpindahan cukup menulis {@code setPertemuan(idBaru)} dan {@code setPertemuanData(...)};
	 *       berkas pengumpulan ikut secara otomatis karena {@link TugasFileContent} merujuk id tugas,
	 *       bukan id pertemuan.</li>
	 *   <li><strong>Tugas Mulai dan Tugas Selesai.</strong> Untuk pengelola berupa {@code MyDatebox}
	 *       yang dapat disunting; untuk mahasiswa berupa teks merah read-only. Keduanya boleh
	 *       dikosongkan, yang berarti tugas tidak memiliki batas mulai atau batas selesai.</li>
	 *   <li><strong>Format penilaian.</strong> Tiga bentuk yang saling eksklusif, dipilih berdasarkan
	 *       {@link #perkuliahan} dan {@link #jadwalPelajaran}; lihat uraian pada kedua field itu.
	 *       Pada bentuk OBE, setiap Sub-CPMK memperoleh satu checkbox dan satu kotak bobot yang
	 *       menulis langsung ke dokumen JSON {@code tugas.getFormatNilais()} pada setiap perubahan —
	 *       tanpa menunggu tombol Simpan. Kotak bobot dinonaktifkan selama checkbox-nya belum
	 *       dicentang, dan seluruh checkbox dinonaktifkan bila perkuliahan sudah dikunci.</li>
	 *   <li><strong>Syarat mengumpulkan tugas.</strong> Lihat {@link #syaratMengumpulkanTugas}.</li>
	 *   <li><strong>Judul tugas.</strong> Kotak teks maksimal 255 karakter yang menyimpan sendiri
	 *       setiap kali kehilangan fokus. Judul kosong berarti "tidak ada tugas" bagi seluruh gerbang
	 *       tampilan di kelas ini, karena itu ia diberi keterangan wajib diisi.</li>
	 *   <li><strong>Lampiran dan Generate Tugas Individu.</strong> Unggahan lampiran ditangani
	 *       {@code LampiranLain.createDownloadUploadFileLain} dengan jenis
	 *       {@code TUGAS_MANDIRI_PERKULIAHAN}; bila lampiran pertama diunggah saat judul masih kosong,
	 *       judul diisi otomatis menjadi "Tugas pertemuan ke N". Tombol Generate memanggil
	 *       {@code AIGenerator.generateApa(...)} untuk menyusun langkah pengerjaan tugas secara
	 *       otomatis; hasilnya mengisi editor teks kaya dan, bila judul masih kosong, ikut mengisi
	 *       judul.</li>
	 *   <li><strong>Isi instruksi tugas.</strong> Editor teks kaya {@code MyCkEditor}.</li>
	 * </ol>
	 *
	 * <h3>Dua jalur penyimpanan</h3>
	 * <p>Perlu diperhatikan bahwa dialog ini menyimpan lewat dua jalur sekaligus. Sebagian kontrol
	 * menyimpan seketika pada kejadian {@code onChange}/{@code onClick}-nya sendiri — kotak judul,
	 * kotak bobot persen, combobox format nilai, checkbox Sub-CPMK, dan combobox jenis item penilaian
	 * siswa. Sisanya baru tersimpan ketika tombol "Simpan Tugas" ditekan. Akibatnya, menutup dialog
	 * lewat tombol Batal <em>tidak</em> membatalkan perubahan yang sudah tersimpan lewat jalur
	 * pertama.</p>
	 *
	 * <h3>Ketahanan terhadap data format nilai yang rusak</h3>
	 * <p>Setiap jalur yang menyentuh format nilai didahului {@link #bersihkanFormatNilaiYatim(Tugas)}
	 * untuk membereskan foreign key yatim di basis data, lalu memvalidasi objek dari layar lewat
	 * {@link #ambilFormatNilaiValid(Session, FormatNilai)}. Bila validasi gagal, pilihan dikosongkan
	 * dan {@link #tampilkanPeringatanFormatNilaiTidakValid(FormatNilai)} dipanggil. Tombol Simpan juga
	 * membungkus {@code Common.refreshUpdate} dengan {@code try}/{@code catch} yang melakukan rollback
	 * dan menampilkan pesan yang dapat dipahami pengguna, karena flush dapat ikut meng-update entity
	 * lain yang datanya tidak konsisten. Selain itu {@code session.refresh(tugas)} dibungkus
	 * {@code try}/{@code catch} tersendiri: sesi yang sedang berjalan belum tentu berada di dalam
	 * transaksi aktif, dan kegagalan menyegarkan bukan alasan untuk membatalkan penyimpanan.</p>
	 *
	 * <h3>Sesudah penyimpanan berhasil</h3>
	 * <p>Tombol Simpan menghitung ulang visibilitas {@link #upload} dan {@link #hapus} sesuai jendela
	 * waktu yang baru, memanggil {@code eventListener} agar pemanggil membangun ulang tampilan,
	 * mengirim pemberitahuan lewat {@code CommonEmail.infoAdaTugasPerkuliahan(tugas)} dan
	 * {@code CommonNotifikasi.infoTugasBaru(tugas)}, melepas dialog, lalu menyegarkan label tab.
	 * Pemberitahuan dikirim pada setiap penyimpanan yang berhasil, termasuk penyuntingan tugas lama —
	 * bukan hanya saat tugas pertama kali dibuat.</p>
	 *
	 * <h3>Batas kewenangan</h3>
	 * <p>Dialog ini tidak melakukan pemeriksaan kewenangan tersendiri sebelum menyimpan. Ia
	 * mengandalkan pemanggilnya: tombol "Ubah Instruksi Tugas" pada
	 * {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} hanya ditampilkan untuk pengguna
	 * non-pelajar. Di dalam dialog, pembedaan peran hanya memengaruhi tampilan — kolom tanggal menjadi
	 * read-only bagi mahasiswa, combobox pindah pertemuan disembunyikan bagi pelajar, dan combobox
	 * syarat dinonaktifkan bila syaratnya ditandai hanya-admin.</p>
	 *
	 * @param eventListener listener yang dipanggil setelah simpan atau pindah pertemuan,
	 *                      dengan data {@link Tugas} yang baru sebagai payload event.
	 * @throws Exception jika terjadi kesalahan akses DB atau ZK rendering.
	 */
	public void onUbahPerintahTugas(final EventListener eventListener) throws Exception {
		final MyWindow addWindow = new MyWindow();
		addWindow.setHeight("95%");
		addWindow.setWidth("950px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);

		addWindow.setTitle("Perintah Tugas");
		if (addWindow != null) {
			Common.clear(addWindow);
		}
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setSclass("fgrid");

		Rows rows = new Rows();

		rows.setParent(grid);

		/*
		 * === Pindahkan tugas ke pertemuan lain ===
		 * Combobox berisi DAFTAR PERTEMUAN yang tersedia pada VOPembelajaran yang SAMA
		 * (ambilVOPembelajaran()), sehingga sebuah tugas dapat dipindah "ke pertemuan ke berapa".
		 * Berlaku untuk TugasPertemuan (sub-tugas) — datanya cukup diarahkan ke pertemuan tujuan
		 * via setPertemuan(Long); berkas pengumpulan (TugasFileContent ref=tugas.id) ikut otomatis.
		 * Untuk tugas yang melekat langsung pada sebuah Pertemuan, pemindahan tidak relevan
		 * sehingga combobox ditampilkan TER-NONAKTIF (informatif).
		 */
		{
			Pertemuan pertemuanTugasIni = null;
			if (tugas instanceof Pertemuan) {
				pertemuanTugasIni = (Pertemuan) tugas;
			} else if (tugas instanceof TugasPertemuan) {
				pertemuanTugasIni = ((TugasPertemuan) tugas).ambilPertemuan();
			}
			final ais.database.model.VOPembelajaran pembelajaran = pertemuanTugasIni == null ? null
					: pertemuanTugasIni.ambilVOPembelajaran();

			final boolean bisaPindahPertemuan = (tugas instanceof TugasPertemuan) && tbmuser != null
					&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null;

			// Combo "Pindahkan ke pertemuan" TIDAK ditampilkan bila yang login adalah pelajar
			// (mahasiswa/siswa/calon/peserta kursus) — sebelumnya hanya di-disable, kini disembunyikan.
			final boolean loginPelajar = tbmuser != null && (tbmuser.getMahasiswa() != null
					|| tbmuser.getSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
					|| tbmuser.getCalonSiswa() != null || tbmuser.getPesertaKursus() != null);

			if (pembelajaran != null && !loginPelajar) {
				MyFormRow rowKe = new MyFormRow();
				rowKe.setValign("top");
				rowKe.setParent(rows);
				rowKe.appendChild(new MyLabelBoldConfig("Pertemuan"));

				rowKe = new MyFormRow();
				rowKe.setParent(rows);

				final Combobox comboPertemuan = new Combobox();
				comboPertemuan.setReadonly(true);
				comboPertemuan.setWidth("95%");
				rowKe.appendChild(comboPertemuan);

				final Long pertemuanSaatIni = (pertemuanTugasIni == null || pertemuanTugasIni.getId() == null) ? null
						: pertemuanTugasIni.getId();
				try {
					// Bila cache lokasi-pertemuan KOSONG, paksa muat ulang dari DB agar dropdown tidak kosong.
					TreeMap<String, Long> daftarPertemuan = pembelajaran.ambilPertemuan();
					if (daftarPertemuan == null || daftarPertemuan.isEmpty()) {
						daftarPertemuan = pembelajaran.ambilPertemuan(true);
					}
					boolean adaSaatIni = false;
					if (daftarPertemuan != null) {
						for (Long pid : daftarPertemuan.values()) {
							if (pid == null) {
								continue;
							}
							Pertemuan p = (Pertemuan) ais.database.model.GeneralValueObject.ambilData(Pertemuan.class,
									pid.toString());
							if (p == null) {
								continue;
							}
							String topik = p.getTopik() == null ? "" : p.getTopik().trim();
							if (topik.length() > 40) {
								topik = topik.substring(0, 40) + "...";
							}
							String tgl = p.getTanggal() == null ? ""
									: (" - " + Common.dateFormat.get().format(p.getTanggal()));
							org.zkoss.zul.Comboitem item = new org.zkoss.zul.Comboitem(
									"Pertemuan ke-" + p.getPertemuanKe() + (topik.isEmpty() ? "" : " : " + topik) + tgl);
							item.setValue(pid);
							item.setParent(comboPertemuan);
							if (pertemuanSaatIni != null && pertemuanSaatIni.equals(pid)) {
								comboPertemuan.setSelectedItem(item);
								adaSaatIni = true;
							}
						}
					}
					// Pastikan pertemuan yang SEDANG dipilih selalu ada & terpilih walau tidak termuat.
					if (!adaSaatIni && pertemuanSaatIni != null && pertemuanTugasIni != null) {
						String topik = pertemuanTugasIni.getTopik() == null ? "" : pertemuanTugasIni.getTopik().trim();
						if (topik.length() > 40) {
							topik = topik.substring(0, 40) + "...";
						}
						String tgl = pertemuanTugasIni.getTanggal() == null ? ""
								: (" - " + Common.dateFormat.get().format(pertemuanTugasIni.getTanggal()));
						org.zkoss.zul.Comboitem itemSaatIni = new org.zkoss.zul.Comboitem("Pertemuan ke-"
								+ pertemuanTugasIni.getPertemuanKe() + (topik.isEmpty() ? "" : " : " + topik) + tgl);
						itemSaatIni.setValue(pertemuanSaatIni);
						itemSaatIni.setParent(comboPertemuan);
						comboPertemuan.setSelectedItem(itemSaatIni);
					}
				} catch (Exception eDaftarPertemuan) {
					Common.tampilErrorJikaAdmin(eDaftarPertemuan);
				}

				comboPertemuan.setDisabled(!bisaPindahPertemuan);

				if (bisaPindahPertemuan) {
					comboPertemuan.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (comboPertemuan.getSelectedItem() == null) {
								return;
							}
							final Long pidBaru = (Long) comboPertemuan.getSelectedItem().getValue();
							if (pidBaru == null || pidBaru.equals(pertemuanSaatIni)) {
								return;
							}
							final String labelTujuan = comboPertemuan.getSelectedItem().getLabel();
							MyMessageboxConfig.showFormatCb("Apakah Bapak/Ibu yakin ingin memindahkan tugas ini ke \"{V1}\"? Tugas akan dipindahkan ke pertemuan yang dipilih.", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {
										@Override
										public void onEvent(Event ev) throws Exception {
											if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
												return;
											}
											Session session = HibernateUtil.currentSession();
											if (tugas.getId() != null) {
												session.refresh(tugas);
											}
											Pertemuan pBaru = (Pertemuan) ais.database.model.GeneralValueObject
													.ambilData(Pertemuan.class, pidBaru.toString());
											((TugasPertemuan) tugas).setPertemuan(pidBaru);
											((TugasPertemuan) tugas).setPertemuanData(pBaru);
											Common.refreshUpdate(session, tugas);

											/* Tutup dialog & muat ulang tampilan agar tugas tampil di pertemuan tujuan. */
											if (eventListener != null) {
												eventListener.onEvent(new Event("", addWindow, tugas));
											}
											addWindow.detach();
										}
									}, labelTujuan);
						}
					});
				} else if (tugas instanceof Pertemuan) {
					Common.initKeteranganSatuKolom(rows,
							"Tugas ini melekat pada pertemuannya sendiri sehingga tidak dapat dipindah ke pertemuan lain.");
				}
			}
		}

		final MyDatebox mulaiWaktuMengumpulkanTugas = new MyDatebox(tugas.getMulai());
		mulaiWaktuMengumpulkanTugas.setFormat(Common.dateFormat.get().toPattern());

		final MyDatebox batasWaktuMengumpulkanTugas = new MyDatebox(tugas.getSelesai());
		batasWaktuMengumpulkanTugas.setFormat(Common.dateFormat.get().toPattern());

		final Combobox[] formatNilaiPerkuliahanRef = new Combobox[1];
		final MyDoublebox[] prosentaseFormatNilaiRef = new MyDoublebox[1];
		final Combobox[] formatNilaiSekolahRef = new Combobox[1];

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Tugas Mulai"));

		row = new MyFormRow();
		row.setParent(rows);
		if (tbmuser.getMahasiswa() == null) {
			row.appendChild(mulaiWaktuMengumpulkanTugas);
		} else {
			Html html = new ais.ui.util.MyHtml(
					"<strong><font style='color:red'>" + (tugas.getMulai() == null ? "Tidak Ada"
							: (SmartDateTimeUtil.getDayString(tugas.getMulai(), null)
									+ Common.dateFormat5.get().format(tugas.getMulai())))
							+ "</font></strong>");
			row.appendChild(html);
		}

		Common.initKeteranganSatuKolom(rows,
				"Kosongkan tanggal tugas mulai jika tugas ini tidak ada batas waktu mulai-nya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Tugas Selesai"));

		row = new MyFormRow();
		row.setParent(rows);
		if (tbmuser.getMahasiswa() == null) {
			row.appendChild(batasWaktuMengumpulkanTugas);
		} else {
			Html html = new ais.ui.util.MyHtml(
					"<strong><font style='color:red'>" + (tugas.getSelesai() == null ? "Tidak Ada"
							: (SmartDateTimeUtil.getDayString(tugas.getSelesai(), null)
									+ Common.dateFormat5.get().format(tugas.getSelesai())))
							+ "</font></strong>");
			row.appendChild(html);
		}

		Common.initKeteranganSatuKolom(rows,
				"Kosongkan tanggal tugas selesai jika tugas ini tidak ada batas waktu selesai-nya");

		if (perkuliahan != null) {

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new MyLabelBoldConfig("Nilai masuk ke format penilaian :"));

			row = new MyFormRow();
			row.setParent(rows);

			Session session = HibernateUtil.currentSession();
			List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);

			if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(),
					perkuliahan.getGanjilGenap())) {

				if (!formatNilais.isEmpty()) {
					boolean sudahadasubCpmk = false;
					for (FormatNilai nilai : formatNilais) {
						if (nilai.getNama().toLowerCase().contains("cpmk")) {
							sudahadasubCpmk = true;
							break;
						}
					}

					if (!sudahadasubCpmk) {
						formatNilais = Common.getFormatNilais(perkuliahan, true);
					}
				}

				final JSONObject jsonObject = new JSONObject(tugas.getFormatNilais());

				MyGrid gridPilih = new MyGrid();
				gridPilih.setParent(row);
				gridPilih.setWidth("100%");
				gridPilih.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(gridPilih);

				MyColumnConfig column = new MyColumnConfig("Sub-CPMK");
				column.setParent(columns);
				column.setWidth("80%");

				column = new MyColumnConfig("Bobot");
				column.setParent(columns);

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

						final MyDoublebox doubleboxBobot = new MyDoublebox(
								jsonObject.isNull(nilai.getId().toString()) ? 100.0
										: jsonObject.getDouble(nilai.getId().toString()));
						doubleboxBobot.setWidth("90%");
						rowPilih.appendChild(doubleboxBobot);

						if (!jsonObject.isNull(nilai.getId().toString())) {
							radio.setChecked(true);
						}
						doubleboxBobot.setDisabled(!radio.isChecked());
						radio.setDisabled(perkuliahan.getDikunci() != null);

						EventListener eventListenerD = new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();
								if (tugas.getId() != null) {
									session.refresh(tugas);
								}
								FormatNilai fn = (FormatNilai) radio.getAttribute("value");
								if (radio.isChecked()) {
									jsonObject.put(fn.getId().toString(),
											doubleboxBobot.getValue() == null ? 100.0 : doubleboxBobot.getValue());
								} else {
									jsonObject.remove(fn.getId().toString());
								}

								tugas.setFormatNilais(jsonObject.toString());
								Common.refreshUpdate(session, (tugas));
								doubleboxBobot.setDisabled(!radio.isChecked());
							}

						};

						radio.addEventListener("onClick", eventListenerD);
						doubleboxBobot.addEventListener("onChange", eventListenerD);
					}
				}

			} else {
				Vbox vbox = new Vbox();
				vbox.setParent(row);
				final Combobox formatNilai = new Combobox();
				formatNilaiPerkuliahanRef[0] = formatNilai;

				formatNilai.setWidth("92px");
				final MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
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
				Hbox hboxP = new Hbox();
				formatNilai.setParent(hboxP);

				if (tugas.getFormatNilai() == null) {
					formatNilai.setSelectedItem(comboitemTidakAda);
				} else {
					Common.selectComboItem(formatNilai, tugas.getFormatNilai());
				}
				formatNilai.setReadonly(true);
				formatNilai.setDisabled(perkuliahan.getDikunci() != null);
				if (perkuliahan.getDikunci() != null) {
					new MyLabelKecil("Penilaian sudah dikunci").setParent(vbox);
					if (tugas.getFormatNilai() != null) {
						new MyLabelKecil("Nilai otomatis masuk ke " + tugas.getFormatNilai().getNama()).setParent(vbox);
					}
				}

				hboxP.setParent(vbox);
				final Label bobotLabel;
				hboxP.appendChild(bobotLabel = new Label(ais.common.Common.getBahasaConfig(" dengan bobot sebesar ")));
				final MyDoublebox prosentase = new MyDoublebox(tugas.getProsentase());
				prosentaseFormatNilaiRef[0] = prosentase;
				prosentase.setDisabled(perkuliahan.getDikunci() != null);
				prosentase.setCols(2);
				prosentase.setParent(hboxP);
				hboxP.appendChild(new Label(" "));

				prosentase.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						if (tugas.getId() != null) {
							bersihkanFormatNilaiYatim(tugas);
							try {
								session.refresh(tugas);
							} catch (Exception eRefresh) {
								ais.common.ErrorAuditUtil.record(eRefresh,
										"TugasMandiriHelper: refresh tugas setelah bersihkan format_nilai yatim gagal");
							}
						}
						FormatNilai fnAktif = ambilFormatNilaiValid(session, tugas.getFormatNilai());
						if (tugas.getFormatNilai() != null && fnAktif == null) {
							tugas.setFormatNilai(null);
						} else if (fnAktif != tugas.getFormatNilai()) {
							tugas.setFormatNilai(fnAktif);
						}
						tugas.setProsentase(prosentase.getValue());
						Common.refreshUpdate(session, tugas);
					}
				});

				prosentase.setVisible(tugas.getFormatNilai() != null);
				bobotLabel.setVisible(tugas.getFormatNilai() != null);

				formatNilai.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						if (tugas.getId() != null) {
							bersihkanFormatNilaiYatim(tugas);
							session.refresh(tugas);
						}
						FormatNilai fnPilihan = (FormatNilai) (formatNilai.getSelectedItem() == null ? null
								: formatNilai.getSelectedItem().getValue());
						FormatNilai fn = ambilFormatNilaiValid(session, fnPilihan);
						if (fnPilihan != null && fn == null) {
							tugas.setFormatNilai(null);
							Common.refreshUpdate(session, (tugas));
							formatNilai.setSelectedItem(comboitemTidakAda);
							prosentase.setVisible(false);
							bobotLabel.setVisible(false);
							tampilkanPeringatanFormatNilaiTidakValid(fnPilihan);
							return;
						}

						tugas.setFormatNilai(fn);
						Common.refreshUpdate(session, (tugas));
						prosentase.setVisible(tugas.getFormatNilai() != null);
						bobotLabel.setVisible(tugas.getFormatNilai() != null);
					}

				});
			}

		} else if (jadwalPelajaran != null) {

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new MyLabelBoldConfig("Nilai masuk ke format penilaian :"));

			row = new MyFormRow();
			row.setParent(rows);
			Vbox vbox = new Vbox();
			vbox.setParent(row);

			final Combobox formatNilai = new Combobox();
			formatNilaiSekolahRef[0] = formatNilai;

			formatNilai.setWidth("92px");
			MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
			comboitemTidakAda.setValue(null);
			formatNilai.appendChild(comboitemTidakAda);

			KelasSiswa kelasSiswa = jadwalPelajaran.getKelas();

			JenisPenilaian jenisPenilaian = jadwalPelajaran.getMatapelajaran().getJenisPenilaian();
			if (jadwalPelajaran.getKurikulumPunyaMatapelajaran() != null
					&& jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah() != null && jadwalPelajaran
							.getKurikulumPunyaMatapelajaran().getKurikulumSekolah().getJenisPenilaian() != null) {
				jenisPenilaian = jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah()
						.getJenisPenilaian();
			}

			Session session = HibernateUtil.currentSession();
			List<GrupPenilaian> grupPenilaians = ConstantValues
					.simpleList(session.createCriteria(DetailJenisPenilaian.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
							.setProjection(Projections.groupProperty("grupPenilaian.id")), GrupPenilaian.class, false);

			for (GrupPenilaian grupPenilaian : grupPenilaians) {

				if (grupPenilaian != null && kelasSiswa != null && kelasSiswa.getTingkat() > 0
						&& grupPenilaian.getKhususTingkat() != null
						&& !grupPenilaian.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
					continue;
				}

				List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
						.simpleList(
								session.createCriteria(DetailGrupPenilaian.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
										.setProjection(Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"))
										.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
								GrupKategoriItemPenilaianSiswa.class, false);

				if (grupKategoriItemPenilaianSiswas.isEmpty()) {
					return;
				}

				Collections.sort(grupKategoriItemPenilaianSiswas);
				for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

					if (grupKategoriItemPenilaianSiswa != null && kelasSiswa != null && kelasSiswa.getTingkat() > 0
							&& grupKategoriItemPenilaianSiswa.getKhususTingkat() != null
							&& !grupKategoriItemPenilaianSiswa.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
						continue;
					}

					List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues.simpleList(
							session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)

									.add(Restrictions.eq("grupKategoriItemPenilaianSiswa",
											grupKategoriItemPenilaianSiswa))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

									.setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id")),
							KategoriItemPenilaianSiswa.class, false);

					List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues.simpleList(
							session.createCriteria(JenisItemPenilaianSiswa.class)
									.createAlias("kategoriItemPenilaianSiswa", "kategoriItemPenilaianSiswa")
									.addOrder(Order.asc("kategoriItemPenilaianSiswa.kode"))
									.addOrder(Order.asc("nomorUrut"))
									.add(Restrictions.in("kategoriItemPenilaianSiswa", kategoriItemPenilaianSiswasId))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							JenisItemPenilaianSiswa.class);
					for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {

						if (jenisItemPenilaianSiswa.getTipeDataInputan().equals(JenisItemPenilaianSiswa.ANGKA)
								|| jenisItemPenilaianSiswa.getTipeDataInputan()
										.equals(JenisItemPenilaianSiswa.TEXT_ANGKA)) {

							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setValue(jenisItemPenilaianSiswa);
							comboitem.setLabel(
									jenisItemPenilaianSiswa.getNama() + " (" + jenisItemPenilaianSiswa.getKode() + ")");
							comboitem.setDescription(
									grupKategoriItemPenilaianSiswa.getNama() + " (" + grupPenilaian.getNama() + ")");

							comboitem.setAttribute("grupKategoriItemPenilaianSiswa", grupKategoriItemPenilaianSiswa);

							comboitem.setAttribute("grupPenilaian", grupPenilaian);

							formatNilai.appendChild(comboitem);
						}

					}

				}

			}

			formatNilai.setParent(vbox);
			if (tugas.getJenisItemPenilaianSiswa() == null) {
				formatNilai.setSelectedItem(comboitemTidakAda);
			} else {
				Common.selectComboItem(formatNilai, tugas.getJenisItemPenilaianSiswa());
			}
			formatNilai.setReadonly(true);
			formatNilai.setDisabled(jadwalPelajaran.getDikunci() != null);
			if (jadwalPelajaran.getDikunci() != null) {
				new MyLabelKecil("Penilaian sudah dikunci").setParent(vbox);
				if (tugas.getJenisItemPenilaianSiswa() != null) {
					new MyLabelKecil("Nilai otomatis masuk ke " + tugas.getJenisItemPenilaianSiswa().getNama() + " "
							+ (tugas.getJenisItemPenilaianSiswa().getKategoriItemPenilaianSiswa() == null ? ""
									: " " + tugas.getJenisItemPenilaianSiswa().getKategoriItemPenilaianSiswa()
											.getNama())

					).setParent(vbox);
				}
			}

			final MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Sinkronkan Nilai");
			button.setParent(vbox);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ais.common.GradingHelper.hitungNilaiBerdasarkanJenisItemPenilaianSiswa(jadwalPelajaran,
							tugas.getGrupKategoriItemPenilaianSiswa(), tugas.getGrupPenilaian(),
							tugas.getJenisItemPenilaianSiswa());
				}
			});
			button.setVisible(tugas.getJenisItemPenilaianSiswa() != null);

			formatNilai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					JenisItemPenilaianSiswa fn = (JenisItemPenilaianSiswa) (formatNilai.getSelectedItem() == null ? null
							: formatNilai.getSelectedItem().getValue());

					GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) (formatNilai
							.getSelectedItem() == null ? null
									: formatNilai.getSelectedItem().getAttribute("grupKategoriItemPenilaianSiswa"));

					GrupPenilaian grupPenilaian = (GrupPenilaian) (formatNilai.getSelectedItem() == null ? null
							: formatNilai.getSelectedItem().getAttribute("grupPenilaian"));

					Session session = HibernateUtil.currentSession();
					tugas.setJenisItemPenilaianSiswa(fn);
					tugas.setGrupKategoriItemPenilaianSiswa(grupKategoriItemPenilaianSiswa);
					tugas.setGrupPenilaian(grupPenilaian);
					Common.refreshUpdate(session, (tugas));

					button.setVisible(tugas.getJenisItemPenilaianSiswa() != null);
				}

			});

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Syarat peserta dapat mengumpulkan tugas"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(syaratMengumpulkanTugas = new Combobox());
		Common.insertComboDanSemua(syaratMengumpulkanTugas, new String[] { "nama" }, "keterangan", SyaratUjian.class,
				"== Tanpa Syarat Mengikuti Ujian ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(syaratMengumpulkanTugas, tugas.getSyaratMengumpulkanTugas(), true);
		syaratMengumpulkanTugas.setWidth("90%");
		syaratMengumpulkanTugas.setReadonly(true);

		final Row rowSyarat = Common.initKeteranganSatuKolom(rows, "Persyaratan ini hanya boleh diubah oleh admin");

		EventListener listenerSyarat = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();

				SyaratUjian syaratUjian = (SyaratUjian) (syaratMengumpulkanTugas.getSelectedItem() == null ? null
						: syaratMengumpulkanTugas.getSelectedItem().getValue());
				syaratMengumpulkanTugas.setDisabled(syaratUjian != null && syaratUjian.getHanyaBolehDiubahOlehAdmin()
						&& (tbmuser == null || tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null));

				rowSyarat.setVisible(syaratUjian != null && syaratUjian.getHanyaBolehDiubahOlehAdmin());
			}
		};
		listenerSyarat.onEvent(null);
		syaratMengumpulkanTugas.addEventListener("onChange", listenerSyarat);

		final MyCkEditor isiTugas = new MyCkEditor();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Judul tugas individu (*):"));

		row = new MyFormRow();
		row.setParent(rows);
		final Textbox judul;
		row.appendChild(judul = new Textbox(tugas.getJudultugas()));
		judul.setWidth("90%");
		judul.setRows(2);
		judul.setMaxlength(255);
		judul.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// FK lama dapat yatim setelah master FormatNilai dihapus. Perbaiki dahulu
				// memakai session terisolasi, kemudian refresh entity pada session request.
				bersihkanFormatNilaiYatim(tugas);
				Session session = HibernateUtil.currentSession();
				if (tugas.getId() != null) {
					session.refresh(tugas);
				}

				tugas.setJudultugas(judul.getValue());

				if (tugas.getId() != null) {
					Common.refreshUpdate(session, tugas);
				}
			}
		});

		Common.initKeteranganSatuKolom(rows,
				"Judul tugas harus diisi, karena sebagai penanda terdapat tugas atau tidak.. Jika judul tidak diisi, maka mahasiswa tidak bisa mengupload tugas..");

		row = new MyFormRow();
		row.setParent(rows);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);

		LampiranLain.createDownloadUploadFileLain(hbox, tugas.getId(), LampiranLain.TUGAS_MANDIRI_PERKULIAHAN,
				"Tugas Individu", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lampiranLain = (LampiranLain) arg0.getData();
						if (lampiranLain != null && tugas.getJudultugas().isEmpty()) {
							Session session = HibernateUtil.currentSession();
							if (tugas.getId() != null) {
								session.refresh(tugas);
							}

							Integer ke = 1;
							if (tugas instanceof Pertemuan) {
								ke = ((Pertemuan) tugas).getPertemuanKe();
							} else if (tugas instanceof TugasPertemuan) {
								Pertemuan pp = ((TugasPertemuan) tugas).ambilPertemuan();
								ke = pp == null ? 1 : pp.getPertemuanKe();
							}
							tugas.setJudultugas("Tugas pertemuan ke " + ke);
							judul.setValue(tugas.getJudultugas());
							Common.refreshUpdate(session, tugas);

							tabpanelFileTugasPertemuan.getLinkedTab().setLabel(tugas.getJudultugas());
						}
					}
				}, null, false, false, false,
				tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
						&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null,
				null, false, false, row);

		MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-cog", "Generate Tugas Individu");
		hbox.appendChild(toolbarbutton);

		Matakuliah mk = null;
		Matapelajaran matpel = null;
		String tanyaAkhiran = "";
		String tanyaMengajar = " apa saja";
		if (perkuliahan != null) {
			mk = (Matakuliah) perkuliahan.getMatakuliah();
			if (mk != null) {
				tanyaMengajar = " matakuliah " + mk.getNama();
				tanyaAkhiran = " pada matakuliah \"" + mk.getNama() + "\"";
			}
		} else if (jadwalPelajaran != null) {
			matpel = (Matapelajaran) jadwalPelajaran.getMatapelajaran();
			if (matpel != null) {
				tanyaMengajar = " matapelajaran " + matpel.getNama();
				tanyaAkhiran = " pada matapelajaran \"" + matpel.getNama() + "\"";
			}
		}

		EventListener eventListenerData = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		};

		toolbarbutton.addEventListener("onClick",
				AIGenerator.generateApa("Generate Tugas Individu", "Tugas individu tentang apa ?",
						"Buatkan tata cara dan langkah-langkah mengerjakan tugas ", false, tanyaAkhiran,
						Common.getKonfigurasi("llama_system_buat_tugas", "Kamu adalah Pengajar atau Dosen atau Guru ")
								.getNilai().trim(),
						isiTugas, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								String isi = (String) arg0.getData();
								Textbox s = (Textbox) arg0.getTarget();
								if (judul.getValue().trim().isEmpty()) {
									judul.setValue("Tugas \"" + s.getValue().trim() + "\"");
									tugas.setJudultugas(judul.getValue());
								}
								isiTugas.setValue(ais.action.servlet.Wa.ubahKeBold(isi).replaceAll("\n", "<br>"));
								tugas.setIsitugas(isiTugas.getValue());

								if (tugas.getId() != null) {
									Common.refreshUpdate(tugas);
								}
							}
						}, tanyaMengajar, eventListenerData));

		Common.initKeteranganSatuKolom(rows,
				"Jika file yang Anda upload lebih dari satu file, zip / compress / jadikan satu file terlebih dulu, kemudian baru di-upload");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig(
				"Masukkan tata cara dan langkah-langkah mengerjakan tugas individu dibawah ini :"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(isiTugas);
		isiTugas.setValue(tugas.getIsitugas());
		isiTugas.setWidth("97%");
		isiTugas.setHeight("120px");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		MyHboxToolbar toolbar = new MyHboxToolbar();
		toolbar.setParent(south);
		MyToolbarbutton cancel = new MyToolbarbutton("fa-ban", "Batal");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbutton save = new MyToolbarbutton("fa-floppy-o", "Simpan Tugas");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (judul.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Judul tugas wajib diisi. Mohon Bapak/Ibu mengisi judul tugas terlebih dahulu.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				bersihkanFormatNilaiYatim(tugas);
				Session session = HibernateUtil.currentSession();
				if (tugas.getId() != null) {
					// FIX akar masalah HibernateException "refresh is not valid without active
					// transaction" (KE-6): currentSession() bisa saja tidak sedang berada di
					// dalam transaksi aktif pada titik ini (mis. request sebelumnya sudah
					// commit, ambient transaction ZK belum/tak lagi terbuka). refresh() TANPA
					// syarat sebelumnya meledak mentah. `tugas` sudah entity yang sama yang
					// akan diisi ulang field-nya di bawah, jadi kalau refresh gagal, lanjut
					// pakai state yang ada saja (bukan kehilangan fungsi apa pun).
					try {
						session.refresh(tugas);
					} catch (Exception eRefresh) { ais.common.ErrorAuditUtil.record(eRefresh, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:1049");
					}
				}
				tugas.setJudultugas(judul.getValue());
				tugas.setIsitugas(isiTugas.getValue());
				tugas.setMulai(mulaiWaktuMengumpulkanTugas.getValue());
				tugas.setSelesai(batasWaktuMengumpulkanTugas.getValue());
				tugas.setSyaratMengumpulkanTugas((SyaratUjian) (syaratMengumpulkanTugas.getSelectedItem() == null ? null
						: syaratMengumpulkanTugas.getSelectedItem().getValue()));
				if (formatNilaiPerkuliahanRef[0] != null) {
					FormatNilai fnPilihan = (FormatNilai) (formatNilaiPerkuliahanRef[0].getSelectedItem() == null ? null
							: formatNilaiPerkuliahanRef[0].getSelectedItem().getValue());
					// Jangan memasang kembali object Comboitem yang sudah tidak ada di DB.
					FormatNilai fn = ambilFormatNilaiValid(session, fnPilihan);
					if (fnPilihan != null && fn == null) {
						tugas.setFormatNilai(null);
						tampilkanPeringatanFormatNilaiTidakValid(fnPilihan);
						return;
					}
					tugas.setFormatNilai(fn);
					tugas.setProsentase(prosentaseFormatNilaiRef[0] == null ? tugas.getProsentase()
							: prosentaseFormatNilaiRef[0].getValue());
				}
				if (formatNilaiSekolahRef[0] != null) {
					JenisItemPenilaianSiswa fn = (JenisItemPenilaianSiswa) (formatNilaiSekolahRef[0].getSelectedItem() == null
							? null : formatNilaiSekolahRef[0].getSelectedItem().getValue());
					GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) (formatNilaiSekolahRef[0]
							.getSelectedItem() == null ? null
									: formatNilaiSekolahRef[0].getSelectedItem()
											.getAttribute("grupKategoriItemPenilaianSiswa"));
					GrupPenilaian grupPenilaian = (GrupPenilaian) (formatNilaiSekolahRef[0].getSelectedItem() == null ? null
							: formatNilaiSekolahRef[0].getSelectedItem().getAttribute("grupPenilaian"));
					tugas.setJenisItemPenilaianSiswa(fn);
					tugas.setGrupKategoriItemPenilaianSiswa(grupKategoriItemPenilaianSiswa);
					tugas.setGrupPenilaian(grupPenilaian);
				}
				try {
					Common.refreshUpdate(session, (tugas));
				} catch (Exception eSimpan) {
					// FIX akar masalah ConstraintViolationException (KE-2): flush ini bisa
					// men-cascade update entity LAIN yang terkait (mis. Pertemuan yang
					// referensi format_nilai-nya sudah tidak ada di tabel formatnilai --
					// data yatim/stale FK), meledak mentah tanpa pesan yang bisa dipahami
					// user. Tangkap, catat, dan beri tahu user apa adanya, jangan biarkan
					// stack trace mentah tampil.
					try {
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback,
							"auto-audit(rollback-gagal) src/ais/action/master/helper/TugasMandiriHelper.java onSave-simpan");
					}
					ais.common.ErrorAuditUtil.record(eSimpan,
							"TugasMandiriHelper: gagal simpan Tugas judul=" + judul.getValue());
					MyMessageboxConfig.show(
							"Mohon maaf, gagal menyimpan tugas karena ada data terkait yang tidak konsisten. "
									+ "Silakan muat ulang (refresh) halaman ini dan coba lagi. Jika masih gagal, hubungi Administrator.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
				upload.setVisible(!tugas.getJudultugas().isEmpty()
						&& ((tugas.getSelesai() == null || tugas.getSelesai().after(kemarin.getTime()))
								&& (tugas.getMulai() == null || tugas.getMulai().before(kemarin.getTime()))));
				if (!tugas.getJudultugas().isEmpty() && mahasiswa != null
						&& tugas.getMhsBolehUploadUlang().contains("," + mahasiswa.getId() + ",")) {
					upload.setVisible(true);
				} else if (!tugas.getJudultugas().isEmpty() && biodataCalonMahasiswa != null
						&& tugas.getMhsBolehUploadUlang().contains("," + biodataCalonMahasiswa.getId() + ",")) {
					upload.setVisible(true);
				}
				hapus.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
						&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						&& !tugas.getJudultugas().isEmpty());
				eventListener.onEvent(new Event("", addWindow, tugas));

				CommonEmail.infoAdaTugasPerkuliahan(tugas);
				ais.common.CommonNotifikasi.infoTugasBaru(tugas);

				addWindow.detach();
				tabpanelFileTugasPertemuan.getLinkedTab().setLabel(tugas.getJudultugas());
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Penanda bahwa tampilan sedang dibuka dari perangkat bergerak (ponsel/tablet).
	 *
	 * <p>Diisi sekali di awal {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} dari
	 * {@code Common.isMobile()}, yang menyimpulkan jenis perangkat dari permintaan HTTP yang sedang
	 * berjalan. Karena diisi pada saat pembangunan UI, nilainya mencerminkan perangkat pada saat tab
	 * tugas pertama kali dibuka dan tidak berubah walaupun jendela peramban diperbesar.</p>
	 *
	 * <p><strong>Pengaruhnya terbatas pada lebar kolom grid.</strong> Pada mode bergerak, kolom
	 * "Tgl dan waktu" diberi lebar {@code 0%} — praktis disembunyikan agar layar sempit tidak penuh
	 * oleh kolom yang jarang dibaca — sementara kolom "Nilai &amp; Keterangan" dilebarkan dari
	 * {@code 30%} menjadi {@code 40%}. Di luar itu, penyesuaian tampilan untuk layar sempit ditangani
	 * oleh CSS: portal dua kolom {@code ais-tugas-equal-height-portal} menumpuk sendiri secara
	 * vertikal, dan toolbar memakai {@code flex-wrap:wrap} sehingga tombol melipat ke baris
	 * berikutnya.</p>
	 *
	 * <p>Field ini tidak dipakai sebagai gerbang otorisasi apa pun; jenis perangkat tidak pernah
	 * menambah atau mengurangi kewenangan.</p>
	 */
	private boolean mobile = false;
	/**
	 * Penanda peran: pengguna yang sedang login adalah pelajar, bukan pengelola.
	 *
	 * <p>Dihitung di awal {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} sebagai
	 * "{@link #tbmuser} tidak {@code null} DAN salah satu dari {@code getMahasiswa()},
	 * {@code getBiodataCalonMahasiswa()}, {@code getSiswa()}, atau {@code getCalonSiswa()} terisi".
	 * Inilah penanda peran yang benar di dalam kelas ini, karena ia dibaca dari sesi yang sedang
	 * berjalan — berbeda dengan {@link #mahasiswa} dan {@link #biodataCalonMahasiswa} yang hanya
	 * menyalin argumen konstruktor.</p>
	 *
	 * <p><strong>Cakupan yang dipakai sebagai gerbang.</strong></p>
	 * <ul>
	 *   <li><em>Panel ringkasan pengumpulan</em> pada kolom instruksi (status waktu tugas, jumlah
	 *       peserta, jumlah sudah/belum upload, jumlah sudah/belum dinilai, nilai rata-rata,
	 *       tertinggi, dan terendah) hanya dibangun bila {@code !peserta}. Angka-angka itu adalah
	 *       ringkasan seluruh kelas sehingga tidak layak ditampilkan kepada sesama peserta.</li>
	 *   <li><em>Toolbar Simpan/Batal</em> di bawah grid "Telah upload" hanya dipasang bila
	 *       {@code !peserta}.</li>
	 *   <li><em>Tombol "Recovery"</em> (riwayat Envers seluruh tugas pada pembelajaran ini) hanya
	 *       tampil bila {@code !peserta}.</li>
	 *   <li><em>Indikator warna baris</em> pada {@link DetailTugasFileContentRenderer} — garis kiri
	 *       hijau untuk yang sudah dinilai dan merah untuk yang belum — hanya dipasang bila
	 *       {@code !peserta}.</li>
	 *   <li><em>Sel nilai</em> pada {@link #displayRow(TugasFileContent, List, Component)}: cabang
	 *       {@code !peserta} membangun ringkasan nilai plus tombol "Edit Nilai" yang membuka popup
	 *       entri nilai; cabang peserta hanya menampilkan nilai milik dirinya sendiri sebagai teks
	 *       read-only.</li>
	 *   <li><em>Label kolom pencarian</em> berbunyi "Peserta Lain" bagi peserta dan "Peserta" bagi
	 *       pengelola.</li>
	 * </ul>
	 *
	 * <p><strong>Perhatian: cakupan tidak mencakup peserta kursus.</strong> Perhitungan field ini
	 * tidak menguji {@code tbmuser.getPesertaKursus()}, sedangkan sejumlah gerbang lain di berkas ini
	 * mengujinya. Selain itu {@code peserta} tidak dipakai sebagai gerbang pembangunan {@code
	 * MyButtonTabbox} berisi tab pengelolaan — gerbang itu memakai {@link #mahasiswa} dan
	 * {@link #biodataCalonMahasiswa}. Perbedaan dasar pemeriksaan antara kedua tempat itu perlu
	 * diperhatikan setiap kali gerbang di kelas ini diubah.</p>
	 */
	private boolean peserta = false;
	/**
	 * Panel ZK tempat seluruh antarmuka tugas ini dibangun.
	 *
	 * <p>Merupakan salinan argumen {@code tabpanelFileTugasPertemuan} milik
	 * {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)}, disimpan sebagai field agar
	 * listener-listener yang dibuat di dalam metode tersebut — dan yang berumur jauh lebih panjang
	 * daripada pemanggilannya — masih dapat menjangkaunya di kemudian hari.</p>
	 *
	 * <p><strong>Pemakaian utama: menyegarkan label tab.</strong> Judul tugas ikut menjadi label tab
	 * yang menampung panel ini. Setiap kali judul berubah, kode memanggil
	 * {@code tabpanelFileTugasPertemuan.getLinkedTab().setLabel(tugas.getJudultugas())} agar label
	 * tab tidak tertinggal dari isinya. Titik pemanggilannya cukup banyak: setelah menyimpan dialog
	 * ubah instruksi, setelah judul diisi otomatis ketika lampiran pertama diunggah, setelah tugas
	 * "dihapus" (judul dikosongkan sehingga label tab ikut kosong), setelah menyalin tugas lewat
	 * "Ambil Tugas", dan setelah setiap perubahan checkbox pada tab "Peserta yg tdk perlu ikt".</p>
	 *
	 * <p><strong>Pemakaian kedua: membangun ulang seluruh layar.</strong> Beberapa aksi memanggil
	 * kembali {@code createTugas(tugas, tabpanelFileTugasPertemuan, hapusEvent, tampilInfo)} lewat
	 * {@code Common.createDefaultTimer(...)}, yaitu setelah dialog ubah instruksi disimpan, setelah
	 * "Ambil Tugas" menyalin instruksi dan lampiran, dan setelah dialog Format Nilai menghitung ulang
	 * nilai. Pembangunan ulang itu diawali {@code Common.clear(tabpanelFileTugasPertemuan)} sehingga
	 * seluruh komponen lama dilepas dan tidak menumpuk.</p>
	 *
	 * <p><strong>Catatan siklus hidup.</strong> Field ini menunjuk komponen milik satu desktop ZK.
	 * Menyimpan instance helper melewati batas request atau membaginya antar-pengguna akan membuat
	 * field ini menunjuk komponen yang sudah terlepas dari halaman, dan setiap
	 * {@code getLinkedTab()} berikutnya berpotensi mengembalikan {@code null}.</p>
	 */
	private Tabpanel tabpanelFileTugasPertemuan;
	/**
	 * Peta bantu berisi berkas pengumpulan yang berhasil dimuat, dikunci berdasarkan id pemilik.
	 *
	 * <p>Peta ini dibuat kosong pada setiap {@link #reloadTugasFileContent(boolean)} lalu diteruskan
	 * sebagai argumen pertama ke
	 * {@code tugas.ambilTugasFileContentTotal(treemap, cari, paging, 500, refresh)}. Entity
	 * {@link Tugas} mengisinya sambil berjalan, sehingga peta ini berfungsi sebagai <em>nilai balik
	 * kedua</em> dari pemanggilan tersebut: kuncinya adalah id peserta pemilik berkas, bukan id baris
	 * {@link TugasFileContent}.</p>
	 *
	 * <p><strong>Peran sebagai jaring pengaman.</strong> Bila pemanggilan
	 * {@code ambilTugasFileContentTotal} melemparkan pengecualian di tengah jalan — misalnya karena
	 * satu berkas fisiknya hilang sehingga {@code FileFoto.ambilFile()} gagal — blok {@code catch}
	 * memakai {@code treemapData} sebagai hasil pengganti. Dengan begitu satu berkas bermasalah tidak
	 * menggagalkan seluruh daftar; baris yang sempat termuat tetap ditampilkan.</p>
	 *
	 * <p><strong>Peran sebagai penanda "sudah mengumpulkan".</strong> Karena kuncinya adalah id
	 * peserta, peta ini dipakai langsung untuk menjawab pertanyaan "siapa yang belum mengumpulkan":
	 * tab "Belum upload" menyaring {@code pa.ambilMahasiswa()} dengan syarat
	 * {@code treemapData == null || !treemapData.containsKey(id)}, sementara tab "Statistik" memakai
	 * {@code treemapData.size()} sebagai jumlah pengumpul.</p>
	 *
	 * <p><strong>Peran ketiga: sumber data unduh dan unggah nilai.</strong> Tombol "Download Nilai"
	 * dan "Upload Nilai" meneruskan peta ini kembali ke {@code ambilTugasFileContentTotal} agar
	 * bekerja pada kumpulan baris yang sama dengan yang sedang tampil di layar, sehingga kolom ID
	 * pada berkas Excel selalu cocok dengan baris yang ada.</p>
	 *
	 * <p><strong>Batasan.</strong> Peta ini hanya memuat hasil satu halaman dengan batas 500 baris
	 * dan sudah tersaring oleh kata kunci pada kotak {@link #cari}. Ia bukan gambaran lengkap seluruh
	 * pengumpulan; untuk itu tersedia {@code tugas.ambilTugasFileContentTotal()} tanpa argumen yang
	 * dipakai pada unduhan ZIP, integrasi Drive, statistik distribusi nilai, dan seluruh aksi
	 * kehadiran massal.</p>
	 */
	private TreeMap<Long, TugasFileContent> treemapData = null;
	/**
	 * Kotak isian kata kunci untuk menyaring daftar pengumpulan pada grid "Telah upload".
	 *
	 * <p>Ditempatkan di header kolom pertama grid, berdampingan dengan tombol "Cari". Baik penekanan
	 * tombol maupun kejadian {@code onOK} (menekan Enter di dalam kotak) memanggil
	 * {@link #reloadTugasFileContent()}, yang meneruskan {@code cari.getValue().trim()} sebagai
	 * argumen pencarian ke {@code tugas.ambilTugasFileContentTotal(...)}. Penyaringan karena itu
	 * dilakukan di sisi basis data, bukan di memori.</p>
	 *
	 * <p><strong>Perlakuan aman terhadap {@code null}.</strong> Field ini baru dibuat di dalam blok
	 * pembangunan grid, yang seluruhnya dilewati bila tugas belum berjudul. Karena itu setiap
	 * pembacaan ditulis sebagai {@code cari == null ? "" : cari.getValue().trim()} — pola yang muncul
	 * pada {@link #reloadTugasFileContent(boolean)}, pada listener "Download Nilai", dan pada
	 * {@code initSpreadsheet} milik "Upload Nilai". String kosong berarti "tanpa penyaringan".</p>
	 *
	 * <p><strong>Jangan dikelirukan dengan kotak cari lain.</strong> Tab "Belum upload" dan tab
	 * "Peserta yg tdk perlu ikt" masing-masing memiliki kotak {@code Textbox} bernama {@code cari}
	 * sendiri sebagai variabel lokal di dalam pemuat tab. Keduanya menyaring daftar peserta
	 * ({@code pa.ambilMahasiswa()}) di memori dengan mencocokkan NIM/nomor registrasi dan nama, bukan
	 * menyaring baris pengumpulan lewat basis data seperti field ini.</p>
	 */
	private Textbox cari;

	/**
	 * Membangun seluruh antarmuka pengguna untuk satu Tugas Mandiri di dalam {@code tabpanelFileTugasPertemuan}.
	 *
	 * <p>Metode utama helper ini. Membersihkan konten tabpanel sebelumnya, lalu membangun
	 * ulang layout portal dua kolom (kiri = instruksi tugas, kanan = toolbar + daftar
	 * pengumpulan). Toolbar dikelompokkan ke dalam empat grup visual: Berkas Tugas, Nilai,
	 * Kelola, dan Kehadiran, dipisahkan oleh separator tipis. Bagian kanan berisi Borderlayout
	 * dengan North (toolbar) dan Center (Tabbox: Telah Upload / Belum Upload / Statistik /
	 * Peserta yg tdk perlu ikut / Rekap Tugas). Tampilan dan aksi tombol disesuaikan
	 * otomatis berdasarkan peran pengguna (mahasiswa/siswa vs dosen/admin).</p>
	 *
	 * <h3>Urutan pembangunan</h3>
	 * <ol>
	 *   <li><strong>Penyiapan state.</strong> {@link #tabpanelFileTugasPertemuan} disimpan dan
	 *       dikosongkan, {@link #tbmuser} dibaca dari sesi, {@link #mobile} dan {@link #peserta}
	 *       dihitung, lalu {@link #perkuliahan}, {@link #jadwalPelajaran}, dan {@link #pa} diturunkan
	 *       dari jenis entitas {@link Tugas} yang diterima.</li>
	 *   <li><strong>Portal dua kolom.</strong> {@code MyPortallayout} berisi kolom kiri 40% (instruksi
	 *       tugas) dan kolom kanan 60% (pengumpulan dan rekap). Portal dipilih menggantikan
	 *       {@code Borderlayout} West+Center agar kedua kolom menumpuk sendiri pada layar sempit.</li>
	 *   <li><strong>Kolom kiri.</strong> Berisi info pertemuan opsional, tanggal mulai dan selesai,
	 *       panel ringkasan pengumpulan khusus pengelola, deretan tombol pengelolaan (Ubah Instruksi
	 *       Tugas, Ambil Tugas, Hapus Tugas, Format Nilai), kotak instruksi tugas beserta lampirannya,
	 *       dan di ujung pemanggilan {@code Tugas.tampilanSyarat}/{@code tampilanSyaratReadonly} yang
	 *       mengisi {@link #syaratAlert}.</li>
	 *   <li><strong>Toolbar kolom kanan.</strong> Dibangun sebagai {@code Hbox} ber-{@code flex-wrap}
	 *       dan dikelompokkan oleh {@link #createSeparator()} menjadi empat kelompok: Berkas (Upload
	 *       Tugas, Download Semua, Drive, Akses), Nilai (Masukkan Nilai, Rekap Semua Tugas untuk
	 *       peserta, Download Nilai, Upload Nilai), Kelola (Refresh, Recovery), dan Kehadiran (Anggap
	 *       Hadir Pengumpul, Anggap Hadir Pengakses, Anggap Sudah Upload).</li>
	 *   <li><strong>Isi kolom kanan.</strong> Bila tugas belum berjudul, seluruh bagian ini dilewati.
	 *       Bila berjudul, dibangun {@link #myborderlayoutlagi}; untuk peserta hanya berisi grid,
	 *       untuk selainnya dibungkus {@code MyButtonTabbox} berisi lima tab.</li>
	 * </ol>
	 *
	 * <h3>Lima tab pada tampilan pengelolaan</h3>
	 * <ol>
	 *   <li><em>Telah upload</em> — dimuat seketika. Berisi {@link #uploadTugasGrid} beserta toolbar
	 *       Simpan/Batal di bawahnya (khusus {@code !peserta}). Simpan menulis {@link #jsonObjectTugas}
	 *       ke kolom {@code keteranganNilai}; Batal membaca ulang kolom itu dari basis data sehingga
	 *       perubahan di memori terbuang.</li>
	 *   <li><em>Belum upload</em> — dimuat malas sekali saja. Menyaring {@code pa.ambilMahasiswa()}
	 *       terhadap {@link #treemapData} dan {@code mhsYgTidakIkut}, menampilkan waktu akses terakhir,
	 *       serta menyediakan dua tombol penandaan alpa.</li>
	 *   <li><em>Statistik</em> — dibangun ulang setiap kali tab dipilih. Berisi kartu ringkasan, dua
	 *       donut (pengumpulan dan keterbacaan), batang capaian, tren tugas masuk per tanggal,
	 *       histogram distribusi nilai lima rentang, dan pada kurikulum OBE sebuah radar Sub-CPMK
	 *       lewat {@link #buildRadarChartHtml(String, String, LinkedHashMap, double)}. Seluruh grafik
	 *       dihasilkan sebagai HTML/CSS/SVG oleh {@code DashboardUiKit}, tanpa pustaka grafik.</li>
	 *   <li><em>Peserta yg tdk perlu ikt</em> — dimuat malas sekali saja. Menyediakan checkbox
	 *       "Tidak perlu ikut tugas" dan "Boleh Upload Ulang" per peserta beserta checkbox massal di
	 *       header kolom, dan pada {@link TugasPertemuan} ber-OBE menambah dua kolom untuk nilai manual
	 *       per Sub-CPMK ({@code nilaiManualJson}) serta daftar Sub-CPMK yang dikerjakan peserta
	 *       ({@code subCpmkPerPeserta}). Checkbox "Paksa" menentukan apakah nilai manual dipakai
	 *       walaupun peserta tetap mengumpulkan tugas.</li>
	 *   <li><em>Rekap Tugas</em> — dibangun ulang setiap kali dipilih; menyematkan
	 *       {@link RekapHasilTugasPerVoPertemuan} untuk seluruh pembelajaran.</li>
	 * </ol>
	 *
	 * <h3>Catatan penting bagi pemelihara</h3>
	 * <ul>
	 *   <li><strong>Gerbang tabbox memakai field konstruktor.</strong> Pembangunan tabbox pengelolaan
	 *       diputuskan oleh {@link #mahasiswa} dan {@link #biodataCalonMahasiswa}, bukan oleh
	 *       {@link #peserta}. Karena kedua field itu menyatakan konteks pemanggilan dan bukan peran
	 *       pengguna yang login, dasar pemeriksaannya berbeda dari gerbang lain di kelas yang sama.
	 *       Setiap perubahan gerbang di sini perlu menyelaraskan kedua dasar tersebut.</li>
	 *   <li><strong>Rantai pemeriksaan peran ditulis berulang.</strong> Pola
	 *       {@code tbmuser.getSiswa() == null && ...} disalin puluhan kali dengan variasi kecil —
	 *       beberapa menguji {@code getSiswa()} lebih dari sekali, sebagian menyertakan
	 *       {@code getPesertaKursus()} dan sebagian tidak. Bentuk terpusatnya tersedia lewat
	 *       {@link #bolehKelolaTugas(Tbmuser)} dan {@link #bolehUpload(Tbmuser)}.</li>
	 *   <li><strong>Batasan satu anak pada region layout.</strong> {@code Borderlayout} hanya menerima
	 *       satu North dan satu South, dan {@code South} sendiri hanya menerima satu anak langsung.
	 *       Karena itu toolbar Simpan/Batal dan {@link #paging} berbagi satu {@code Hbox}, dan setiap
	 *       pembuatan region selalu ditulis "pakai yang sudah ada bila ada".</li>
	 *   <li><strong>Pembangunan ulang lewat timer.</strong> Beberapa aksi memanggil kembali metode ini
	 *       dari dalam {@code Common.createDefaultTimer(...)} alih-alih langsung, agar pembangunan
	 *       ulang terjadi pada siklus event berikutnya dan tidak merusak pohon komponen yang sedang
	 *       diproses.</li>
	 * </ul>
	 *
	 * @param tugas                    entitas tugas (dapat berupa {@link Pertemuan} atau
	 *                                 {@link TugasPertemuan}) yang akan ditampilkan.
	 * @param tabpanelFileTugasPertemuan panel ZK tempat seluruh UI dibangun.
	 * @param hapusEvent               listener yang dipanggil setelah tugas berhasil dihapus.
	 * @param tampilInfo               bila {@code true}, tampilkan info ringkasan pertemuan
	 *                                 di bagian atas grid perintah tugas.
	 * @throws Exception jika terjadi kesalahan akses DB atau ZK rendering.
	 */
	@SuppressWarnings("deprecation")
	public void createTugas(final Tugas tugas, final Tabpanel tabpanelFileTugasPertemuan,
			final EventListener hapusEvent, final boolean tampilInfo) throws Exception {
		this.tabpanelFileTugasPertemuan = tabpanelFileTugasPertemuan;
		if (tabpanelFileTugasPertemuan != null) {
			Common.clear(tabpanelFileTugasPertemuan);
		}
		tbmuser = Common.getCurrentUser();
		mobile = Common.isMobile();
		peserta = tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
				|| tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null);
		this.tugas = tugas;
		if (tugas instanceof Pertemuan) {
			perkuliahan = ((Pertemuan) tugas).getPerkuliahan();
			jadwalPelajaran = ((Pertemuan) tugas).getJadwalPelajaran();
		} else if (tugas instanceof TugasPertemuan) {
			Pertemuan pp = ((TugasPertemuan) tugas).ambilPertemuan();
			perkuliahan = pp == null ? null : pp.getPerkuliahan();
			jadwalPelajaran = pp == null ? null : pp.getJadwalPelajaran();
		} else {
			perkuliahan = null;
			jadwalPelajaran = null;
		}

		if (tugas instanceof Pertemuan) {
			pa = (Pertemuan) tugas;
		} else if (tugas instanceof TugasPertemuan) {
			pa = ((TugasPertemuan) tugas).ambilPertemuan();
		}
		upload = new MyToolbarbutton("fa-upload", "Upload Tugas");
		upload.setTooltiptext("Kirim berkas tugas Anda ke sistem");
		hapus = new MyToolbarbutton("fa-trash", "Hapus Tugas");
		hapus.setTooltiptext("Hapus perintah dan instruksi tugas ini (judul, isi, dan berkas lampiran)");

		/* Portal 2 kolom responsif (menumpuk di HP) menggantikan Borderlayout West+Center.
		 * Kiri = detail/instruksi tugas (40%), kanan = pengumpulan & rekap (60%). */
		ais.ui.util.MyPortallayout borderlayout = new ais.ui.util.MyPortallayout();
		borderlayout.setSclass("ais-tugas-equal-height-portal");
		borderlayout.setWidth("100%");
		borderlayout.setParent(tabpanelFileTugasPertemuan);

		ais.ui.util.MyPortalchildren west = new ais.ui.util.MyPortalchildren();
		west.setSclass("ais-tugas-equal-height-column ais-tugas-instruksi-column");
		west.setWidth("40%");
		west.setParent(borderlayout);

		ais.ui.util.MyPortalchildren center = new ais.ui.util.MyPortalchildren();
		center.setSclass("ais-tugas-equal-height-column ais-tugas-jawaban-column");
		center.setWidth("60%");
		center.setParent(borderlayout);

		Grid gridPerintah = new Grid();
		gridPerintah.setSclass("fgrid");
		gridPerintah.setParent(west);
		gridPerintah.setWidth("100%");

		// gridPerintah.setHeight("200px");

		Columns columns = new Columns();

		columns.setParent(gridPerintah);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();

		rows.setParent(gridPerintah);

		if (tampilInfo) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pa));

		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(RevisiHelper.createNewRevisi(tugas.getClass(), tugas, "Tugas Mulai"));

		final Html htmlMulai = new ais.ui.util.MyHtml(
				"<strong><font style='color:red'>" + (tugas.getMulai() == null ? "Tidak Ada"
						: (SmartDateTimeUtil.getDayString(tugas.getMulai(), null)
								+ Common.dateFormat5.get().format(tugas.getMulai())))
						+ "</font></strong>");
		row.appendChild(htmlMulai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tugas Selesai"));

		final Html htmlSampai = new ais.ui.util.MyHtml(
				"<strong><font style='color:red'>" + (tugas.getSelesai() == null ? "Tidak Ada"
						: (SmartDateTimeUtil.getDayString(tugas.getSelesai(), null)
								+ Common.dateFormat5.get().format(tugas.getSelesai())))
						+ "</font></strong>");
		row.appendChild(htmlSampai);

		// Info tambahan bermanfaat (HANYA untuk pengelola/dosen, bukan mahasiswa — privasi):
		// status waktu tugas + rekap pengumpulan (total peserta, sudah upload, belum upload).
		if (!peserta) {
			try {
				int totalPesertaTgs = 0;
				if (perkuliahan != null) {
					java.util.List<?> msTgs = perkuliahan.ambilMahasiswa();
					totalPesertaTgs = msTgs == null ? 0 : msTgs.size();
				}
				java.util.TreeMap<Long, ais.database.model.file.TugasFileContent> dTgs = tugas.ambilTugasFileContentTotal();
				int sudahUpload = dTgs == null ? 0 : dTgs.size();
				int belumUpload = totalPesertaTgs - sudahUpload;
				if (belumUpload < 0) {
					belumUpload = 0;
				}
				java.util.Date kiniTgs = ais.ui.util.WaktuUtil.getCalendar().getTime();
				String statusTugas;
				if (tugas.getMulai() != null && kiniTgs.before(tugas.getMulai())) {
					statusTugas = "Belum dibuka";
				} else if (tugas.getSelesai() != null && kiniTgs.after(tugas.getSelesai())) {
					statusTugas = "Sudah ditutup";
				} else {
					statusTugas = "Sedang berlangsung";
				}
				// Statistik nilai dari berkas yang sudah dikumpulkan (TugasFileContent.getNilai).
				java.text.DecimalFormat dfNilaiTgs = new java.text.DecimalFormat("#0.##");
				double sumN = 0, maxN = Double.NEGATIVE_INFINITY, minN = Double.POSITIVE_INFINITY;
				int cntN = 0, sudahDinilai = 0;
				if (dTgs != null) {
					for (ais.database.model.file.TugasFileContent tc : dTgs.values()) {
						if (tc == null) {
							continue;
						}
						double n = tc.getNilai() == null ? 0.0 : tc.getNilai().doubleValue();
						sumN += n;
						if (n > maxN) {
							maxN = n;
						}
						if (n < minN) {
							minN = n;
						}
						cntN++;
						if (n > 0) {
							sudahDinilai++;
						}
					}
				}
				String rataNilaiTgs = cntN > 0 ? dfNilaiTgs.format(sumN / cntN) : "-";
				String tinggiNilaiTgs = cntN > 0 ? dfNilaiTgs.format(maxN) : "-";
				String rendahNilaiTgs = cntN > 0 ? dfNilaiTgs.format(minN) : "-";
				int belumDinilai = sudahUpload - sudahDinilai;
				if (belumDinilai < 0) {
					belumDinilai = 0;
				}
				String[][] infoTgs = new String[][] { { "Status", statusTugas },
						{ "Total peserta", totalPesertaTgs + " mhs" }, { "Sudah upload", sudahUpload + " mhs" },
						{ "Belum upload", belumUpload + " mhs" }, { "Sudah dinilai", sudahDinilai + " mhs" },
						{ "Belum dinilai", belumDinilai + " mhs" }, { "Rata-rata nilai", rataNilaiTgs },
						{ "Nilai tertinggi", tinggiNilaiTgs }, { "Nilai terendah", rendahNilaiTgs } };
				for (int iInfo = 0; iInfo < infoTgs.length; iInfo++) {
					MyFormRow rowInfoTgs = new MyFormRow();
					rowInfoTgs.setParent(rows);
					rowInfoTgs.appendChild(new ais.ui.util.MyLabelConfig(infoTgs[iInfo][0]));
					rowInfoTgs.appendChild(new ais.ui.util.MyLabelBold(infoTgs[iInfo][1]));
				}
			} catch (Exception eInfoTgs) {
				ais.common.ErrorAuditUtil.record(eInfoTgs, "auto-audit(empty-catch) TugasMandiriHelper.infoRekapTugas");
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		MyHboxToolbar hboxa = new MyHboxToolbar();
		row.appendChild(hboxa);

		MyToolbarbutton rubah = new MyToolbarbutton("fa-pencil-square", "Ubah Instruksi Tugas");
		rubah.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getSiswa() == null);
		hboxa.appendChild(rubah);

		MyToolbarbutton ambil = new MyToolbarbutton("fa-list-alt", "Ambil Tugas");
		ambil.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getSiswa() == null);
		hboxa.appendChild(ambil);
		ambil.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final AmbilDataTugasMandiri ambilDataLampiranFileLain = new AmbilDataTugasMandiri();

				ambilDataLampiranFileLain.setHeight("95%");
				ambilDataLampiranFileLain.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataLampiranFileLain);
				ambilDataLampiranFileLain.onModal();
				ambilDataLampiranFileLain.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

						Pertemuan pertemuanCopy = (Pertemuan) arg0.getData();
						if (pertemuanCopy != null && !pertemuanCopy.getJudultugas().isEmpty()) {
							Session session = HibernateUtil.currentSession();
							if (tugas.getId() != null) {
								session.refresh(tugas);
							}
							tugas.setJudultugas(pertemuanCopy.getJudultugas());
							tugas.setIsitugas(pertemuanCopy.getIsitugas());
							tugas.setMulai(pertemuanCopy.getMulai());
							tugas.setSelesai(pertemuanCopy.getSelesai());

							Common.refreshUpdate(session, tugas);

							LampiranLain lampiranLain = LampiranLain.ambil(pertemuanCopy.getId(),
									LampiranLain.TUGAS_MANDIRI_PERKULIAHAN);

							session = StreamingHibernateUtil.getInstance().currentSession();

							try {

								if (lampiranLain != null) {

									session.getTransaction().begin();
									session.createSQLQuery(
											"update lampiran_lain set ref = -111111111111 where ref = " + tugas.getId()
													+ " and jenis = '" + LampiranLain.TUGAS_MANDIRI_PERKULIAHAN + "'")
											.executeUpdate();
									session.getTransaction().commit();

									final LampiranLain copy = new LampiranLain();
									copy.setRef(tugas.getId());
									copy.setCopyDari(lampiranLain);

									Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
									String olehId = Common.generateOlehId(tbmuser);
									copy.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
									copy.setOlehId(olehId);
									copy.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));

									session.getTransaction().begin();
									session.save(copy);
									session.getTransaction().commit();
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:1176");
							}
							StreamingHibernateUtil.getInstance().closeSession();

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									createTugas(tugas, tabpanelFileTugasPertemuan, hapusEvent, tampilInfo);
								}
							});
						}
						ambilDataLampiranFileLain.detach();
						tabpanelFileTugasPertemuan.getLinkedTab().setLabel(tugas.getJudultugas());
					}
				});
			}
		});

		// Tampilkan tombol aksi tugas untuk admin/dosen MESKI pertemuan belum punya tugas (judul
		// kosong) — permintaan user. Gerbang "!judultugas.isEmpty()" dilepas agar pengelola tetap
		// bisa mengelola. Aman: tugas di sini = Pertemuan yang sudah ada (id != null).
		hapus.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		hboxa.appendChild(hapus);

		hapus.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus tugas ini? Data tugas yang telah dihapus tidak dapat dikembalikan.", "Question",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										Session session = HibernateUtil.currentSession();
										if (tugas.getId() != null) {
											session.refresh(tugas);
										}
										tugas.setJudultugas("");
										tugas.setIsitugas("");
										tugas.setFormatNilai(null);
										tugas.setSyaratMengumpulkanTugas(null);
										tugas.setSelesai(null);
										tugas.setMulai(null);
										Common.refreshUpdate(session, tugas);

										session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.createSQLQuery(
												"update lampiran_lain set ref = -111111111111 where ref = "
														+ tugas.getId() + " and jenis = '"
														+ LampiranLain.TUGAS_MANDIRI_PERKULIAHAN + "'")
												.executeUpdate();
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												hapusEvent.onEvent(arg0);
											}
										});
										tabpanelFileTugasPertemuan.getLinkedTab().setLabel(tugas.getJudultugas());
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.showFormat(
												"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang berelasi dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon menghubungi Administrator sistem.",
														"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, e.getMessage());
									}

								}

							}
						});

			}
		});

		if (perkuliahan != null && !perkuliahan.getSembunyikanFormatPenilaian()) {

			final MyToolbarbutton buttonFormatNilai = new MyToolbarbutton("fa-th-list", "Format Nilai");
			buttonFormatNilai.setVisible(tbmuser != null && perkuliahan.getDikunci() == null && mahasiswa == null
					&& biodataCalonMahasiswa == null);
			if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(),
					perkuliahan.getGanjilGenap())) {
				buttonFormatNilai.setVisible(false);
			}
			buttonFormatNilai.setParent(hboxa);
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
													createTugas(tugas, tabpanelFileTugasPertemuan, hapusEvent,
															tampilInfo);
												}
											});

										}
									}, null);

						}
					});
				}

			});
		}

		if (!tugas.getJudultugas().isEmpty()) {
			tugas.masukkanData("tugas");
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setStyle("background-color: rgba(255,255,255,0.4);");

		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.setWidth("90%");
		final MyCaptionStyled judul = new MyCaptionStyled(tugas.getJudultugas());
		judul.setStyle(
				"font-size:12px;font-weight: bolder;text-decoration: none;color:black;border: 1px solid black;\r\n"
						+ "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);"
						+ "  border-radius: 5px 15px;");
		myGroupboxStyled.appendChild(judul);

		row.appendChild(myGroupboxStyled);
		final Html isi = new ais.ui.util.MyHtml(tugas.getIsitugas());
		myGroupboxStyled.appendChild(isi);

		final Hbox hbox = new Hbox();
		hbox.setParent(myGroupboxStyled);

		// Gunakan Hbox biasa (bukan MyHboxToolbar) karena MyHboxToolbar.setStyle() adalah no-op —
		// sehingga flex-wrap tidak pernah diterapkan. Hbox.setStyle() bekerja normal.
		// flex-wrap agar tombol melipat ke baris berikutnya di layar sempit / mobile.
		Hbox vbox = new Hbox();
		vbox.setWidth("100%");
		vbox.setStyle("display:flex;flex-wrap:wrap;gap:3px;align-items:center;"
				+ "padding:4px 6px;min-height:36px;background:#f8fafc;"
				+ "border-bottom:1px solid #e2e8f0;box-sizing:border-box;");
		final Html nilaiMasuk = new ais.ui.util.MyHtml(tugas.getFormatNilai() == null ? ""
				: "<strong><font style='color:blue'>" + ("Nilai akan masuk ke " + tugas.getFormatNilai().getNama())
						+ " dengan bobot sebesar " + Common.numberFormat.get().format(tugas.getProsentase())
						+ " </font></strong>");

		final Row rowTugasMhs = Common.tampilanScroll1(myGroupboxStyled);
		rowTugasMhs.getGrid().setWidth("90%");

		final MyLabelBold syarat = new MyLabelBold(tugas.getSyaratMengumpulkanTugas() == null ? ""
				: "Syarat mengumpulkan tugas : " + tugas.getSyaratMengumpulkanTugas().getNama());
		row = new MyFormRow();
		row.setStyle("background-color: rgba(255,255,255,0.4);");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		syarat.setParent(row);

		final EventListener visibleListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (tugas == null || tugas.getJudultugas().trim().isEmpty()) {
					hbox.setVisible(false);
					nilaiMasuk.setVisible(false);
					judul.setVisible(false);
					rowTugasMhs.setVisible(false);
					isi.setContent("<strong><font style='color:red;font-size: 15px;'>Untuk tugas \""
							+ (tugas.getJudultugas() == null || tugas.getJudultugas().trim().isEmpty() ? pa.getTopik()
									: tugas.getJudultugas())
							+ "\", tidak ada tugas</font></strong>");
					return;
				}

				if (mahasiswa != null || biodataCalonMahasiswa != null
						|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null) {
					Long id = mahasiswa != null ? mahasiswa.getId()
							: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
									: tbmuser.getSiswa() != null ? tbmuser.getSiswa().getId()
											: tbmuser.getCalonSiswa() != null ? tbmuser.getCalonSiswa().getId() : null;

					if (tugas.getMhsYgTidakIkut().contains("," + id + ",")) {

						upload.setVisible(false);
						hbox.setVisible(false);
						nilaiMasuk.setVisible(false);
						judul.setVisible(false);
						rowTugasMhs.setVisible(false);
						isi.setContent("<strong><font style='color:red;font-size: 15px;'>Untuk tugas \""
								+ (tugas.getJudultugas() == null || tugas.getJudultugas().trim().isEmpty()
										? pa.getTopik()
										: tugas.getJudultugas())
								+ "\", Anda tidak perlu mengumpulkan tugas ini.</font></strong><br><img style='width:90%' src=\""
								+ Common.getRequestHostWithProtocol()
								+ "</font></strong><br><img style='width:90%' src=\""
								+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />");
						return;
					}
				}

				Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
				List<String> warnings = new ArrayList<String>();
				if (tbmuser != null && tugas != null && perkuliahan != null && tbmuser.getMahasiswa() != null
						&& tugas.getSyaratMengumpulkanTugas() != null) {
					Detailperkuliahan detailperkuliahan = tbmuser.getMahasiswa().ambilDetailperkuliahan(perkuliahan);

					if (detailperkuliahan == null) {
						tbmuser.getMahasiswa().reInitDetailperkuliahan(HibernateUtil.currentSession());
						detailperkuliahan = tbmuser.getMahasiswa().ambilDetailperkuliahan(perkuliahan);
					}

					if (detailperkuliahan != null) {
						SyaratUjianAction.checkSyaratSyaratUjian(tugas.getSyaratMengumpulkanTugas(),
								pa.ambilVOPembelajaran(), tbmuser.getMahasiswa(), detailperkuliahan.getSemester(),
								tugas.getJudultugas(), warnings);
					}
				}
				if (!warnings.isEmpty()) {
					hbox.setVisible(false);
					nilaiMasuk.setVisible(false);
					judul.setVisible(false);
					rowTugasMhs.setVisible(false);
					isi.setContent("<strong><font style='color:red;font-size: 15px;'>" + warnings.get(0)
							+ "</font></strong><br><img style='width:90%' src=\"" + Common.getRequestHostWithProtocol()
							+ "/img/oh.gif\" alt=\"WebP rules.\" />");
				}

				else if (!(tugas.getMulai() == null || tugas.getMulai().before(kemarin.getTime()))) {
					hbox.setVisible(false);
					nilaiMasuk.setVisible(false);
					judul.setVisible(false);
					rowTugasMhs.setVisible(false);
					isi.setContent("<strong><font style='color:red;font-size: 15px;'>Untuk tugas \""
							+ (tugas.getJudultugas() == null || tugas.getJudultugas().trim().isEmpty() ? pa.getTopik()
									: tugas.getJudultugas())
							+ "\", belum mulai..<br>Tugas akan ditampilkan setelah "
							+ SmartDateTimeUtil.getDayString(tugas.getMulai(), null)
							+ Common.dateFormat5.get().format(tugas.getMulai()) + "</font></strong><br><img src=\""
							+ Common.getRequestHostWithProtocol()
							+ "/img/Apps-preferences-system-time-icon.png\" alt=\"WebP rules.\" />");

				} else if (!(tugas.getSelesai() == null || tugas.getSelesai().after(kemarin.getTime()))) {
					hbox.setVisible(false);
					nilaiMasuk.setVisible(false);
					judul.setVisible(false);
					rowTugasMhs.setVisible(false);
					isi.setContent("<strong><font style='color:red;font-size: 15px;'>Untuk tugas \""
							+ (tugas.getJudultugas() == null || tugas.getJudultugas().trim().isEmpty() ? pa.getTopik()
									: tugas.getJudultugas())
							+ "\", telah selesai..<br>Tugas telah ditampilkan sebelum "
							+ SmartDateTimeUtil.getDayString(tugas.getSelesai(), null)
							+ Common.dateFormat5.get().format(tugas.getSelesai()) + "</font></strong><br><img src=\""
							+ Common.getRequestHostWithProtocol()
							+ "/img/Apps-preferences-system-time-icon.png\" alt=\"WebP rules.\" />");

				} else {
					rowTugasMhs.setVisible(true);
					judul.setVisible(true);
					hbox.setVisible(true);
					nilaiMasuk.setVisible(true);
					isi.setContent(tugas.getIsitugas());
				}
			}
		};

		final EventListener ubahTugas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TugasMandiriHelper.this.tugas = tugas;
				onUbahPerintahTugas(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						final Tugas tugas = (Tugas) arg0.getData();
						TugasMandiriHelper.this.tugas = tugas;

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								createTugas(tugas, tabpanelFileTugasPertemuan, hapusEvent, tampilInfo);
							}
						});
					}
				});

			}
		};

		LampiranLain.createDownloadUploadFileLain(hbox, tugas.getId(), LampiranLain.TUGAS_MANDIRI_PERKULIAHAN,
				"Tugas Individu", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lampiranLain = (LampiranLain) arg0.getData();
						if (lampiranLain != null && tugas.getJudultugas().isEmpty()) {
							Session session = HibernateUtil.currentSession();
							if (tugas.getId() != null) {
								session.refresh(tugas);
							}
							Integer ke = 1;
							if (tugas instanceof Pertemuan) {
								ke = ((Pertemuan) tugas).getPertemuanKe();
							} else if (tugas instanceof TugasPertemuan) {
								Pertemuan pp = ((TugasPertemuan) tugas).ambilPertemuan();
								ke = pp == null ? 1 : pp.getPertemuanKe();
							}
							tugas.setJudultugas("Tugas pertemuan ke " + ke);
							judul.setLabel(tugas.getJudultugas());
							Common.refreshUpdate(session, tugas);
							TugasMandiriHelper.this.tugas = tugas;

							ubahTugas.onEvent(arg0);
							tabpanelFileTugasPertemuan.getLinkedTab().setLabel(tugas.getJudultugas());
						}

					}
				}, null, false, false, false,
				tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
						&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
						&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
						&& tbmuser.getSiswa() == null,
				null, false, false, rowTugasMhs);

		row = new MyFormRow();
		row.setStyle("background-color: rgba(255,255,255,0.4);");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		MyLabelAgakKecilBold a;
		row.appendChild(a = new MyLabelAgakKecilBold(
				"Catatan : Masing-masing mahasiswa hanya bisa memiliki satu tugas yang di-upload, jika mahasiswa telah mengupload ulang tugas, maka tugas sebelumnya akan digantikan dengan file tugas yang baru saja di-upload. Tugas yang telah dinilai tidak bisa diubah atau di-upload ulang."));
		a.setStyle("font-size:10px;font-weight: bolder;color:red");

		row = new MyFormRow();
		row.setStyle("background-color: rgba(255,255,255,0.4);");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setVisible(tugas.getFormatNilai() != null);

		row.appendChild(nilaiMasuk);

		rubah.addEventListener("onClick", ubahTugas);

		MyToolbarbutton download = new MyToolbarbutton("fa-cloud-download", "Download Semua");
		download.setTooltiptext("Unduh semua berkas tugas peserta dalam satu file ZIP");

		MyToolbarbutton drive = new MyToolbarbutton("fa-hdd-o", "Drive");
		drive.setTooltiptext("Kirim semua berkas tugas ke Google Drive akun Anda");

		Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
		upload.setVisible(!tugas.getJudultugas().isEmpty()
				&& ((tugas.getSelesai() == null || tugas.getSelesai().after(kemarin.getTime()))
						&& (tugas.getMulai() == null || tugas.getMulai().before(kemarin.getTime()))));

		if (!tugas.getJudultugas().isEmpty() && mahasiswa != null
				&& tugas.getMhsBolehUploadUlang().contains("," + mahasiswa.getId() + ",")) {
			upload.setVisible(true);
		} else if (!tugas.getJudultugas().isEmpty() && biodataCalonMahasiswa != null
				&& tugas.getMhsBolehUploadUlang().contains("," + biodataCalonMahasiswa.getId() + ",")) {
			upload.setVisible(true);
		}

		hapus.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && !tugas.getJudultugas().isEmpty());
		download.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
		drive.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);

		visibleListener.onEvent(null);

		upload.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				if (!syaratAlert.isEmpty()) {
					String s = "";
					for (String dd : syaratAlert) {
						s += s.isEmpty() ? dd : "\n\n" + dd;
					}
					MyMessageboxConfig.show(s, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if ((pa != null && jadwalPelajaran != null)
						|| (tbmuser != null && (tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null))) {
					Common.uploadTugas(tugas, tbmuser.getSiswa(), tbmuser.getCalonSiswa(), new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tugas != null)
								tugas.belum("tugas_file_content_" + tugas.getClass().getName());

							reloadTugasFileContent();
						}
					});
				}

				else {

					Common.uploadTugas(tugas, mahasiswa, biodataCalonMahasiswa, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tugas != null)
								tugas.belum("tugas_file_content_" + tugas.getClass().getName());

							reloadTugasFileContent();
						}
					});
				}
			}
		});

		download.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				// Create a buffer for reading the files

				final File fileOut = new File(
						Common.REAL_PATH + "/media/Tugas_untuk_pertemuan_ke_" + pa.getPertemuanKe() + ".zip");
				fileOut.getParentFile().mkdirs();

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Filedownload.save(fileOut, "application/zip");
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							byte[] buf = new byte[1024];
							ZipOutputStream out = new ZipOutputStream(new FileOutputStream(fileOut));
							File folderOut = new File(Common.REAL_PATH + "/media/");
							try {
								folderOut.mkdirs();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:1641");
								// TODO: handle exception
							}
							// Compress the files
							TreeMap<Long, TugasFileContent> d = tugas.ambilTugasFileContentTotal();
							int size = d.size();
							int index = 0;
							for (TugasFileContent content : d.values()) {
								index++;
								label.setValue("Sedang memproses tugas " + content.getNama() + " ("
										+ Common.numberFormat.get().format((index * 100.0) / size) + " %)");
								try {
									File file = null;

									if (content.getGdrive() != null && !content.getGdrive().trim().isEmpty()) {
										file = new File(folderOut.getAbsolutePath() + "/"
												+ URLEncoder.encode(content.getNama(), "UTF-8") + ".txt");
										FileUtils.writeStringToFile(file, content.forwardGDriveUrl());
									} else if (content.getLink() != null && !content.getLink().trim().isEmpty()) {
										file = new File(folderOut.getAbsolutePath() + "/"
												+ URLEncoder.encode(content.getNama(), "UTF-8") + ".txt");
										FileUtils.writeStringToFile(file, content.getLink().trim());
									} else {
										file = content.ambilFile();
									}

									FileInputStream in = new FileInputStream(file);
									// Add ZIP entry to output stream.
									out.putNextEntry(new ZipEntry((content.getGdrive() != null
											|| (content.getLink() != null && !content.getLink().trim().isEmpty()))
													? file.getName()
													: URLEncoder.encode(content.getNama(), "UTF-8")));

									// Transfer bytes from the file to the ZIP
									// file
									int len;
									while ((len = in.read(buf)) > 0) {
										out.write(buf, 0, len);
									}

									// Complete the entry
									out.closeEntry();
									in.close();
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}
							d = null;
							// Complete the ZIP file
							out.close();

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException(
									"mengunduh (ZIP) seluruh berkas tugas mandiri",
									e, new String[] {
											"Muat ulang (refresh) halaman ini lalu coba unduh kembali.",
											"Periksa apakah ruang penyimpanan (disk) server masih mencukupi.",
											"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
									});
						}
						label.setValue("");
					}
				}).start();

			}
		});

		drive.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				// Create a buffer for reading the files

				final List<Long> tugasFileContents = new ArrayList<Long>();
				TreeMap<Long, TugasFileContent> d = tugas.ambilTugasFileContentTotal();

				for (TugasFileContent content : d.values()) {

					try {
						File file = null;

						if (content.getGdrive() != null && !content.getGdrive().trim().isEmpty()) {
							file = null;
						} else if (content.getLink() != null && !content.getLink().trim().isEmpty()) {
							file = null;
						} else {
							file = content.ambilFile();
						}

						if (file != null && file.exists()) {
							tugasFileContents.add(content.getId());
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				d = null;

				if (tugasFileContents.isEmpty()) {
					MyMessageboxConfig.show("Tidak ada berkas tugas yang dapat dikirim ke Google Drive. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah mengunggah berkas tugas; (2) periksa kembali daftar berkas tugas yang tersedia.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				} else {

					final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
					final Label label = Common.displayLoadBarjanganBerhenti(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});

					final GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
					File file = new File("/opt/ecampus/test.txt");
					ais.common.BacaTulisUtil.tulis(file, "test send..");
					driveUtilPerPengguna.prosesBackup(file, "test_files",

							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
											.getData();

									if (fileUpload != null && fileUpload.getId() != null) {

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												Session session = StreamingHibernateUtil.getInstance().currentSession();

												String tableName = "tugas_file_content";
												String colFotoName = "filecontent";

												String ids = "";
												for (Long id : tugasFileContents) {
													ids += ids.isEmpty() ? id.toString() : "," + id.toString();
												}

												List<Object[]> inds = session.createSQLQuery("select id," + colFotoName
														+ " from " + tableName + " where " + colFotoName
														+ " is not null and id in (" + ids + ")  order by id desc;")
														.list();
												StreamingHibernateUtil.getInstance().closeSession();

												int size = inds.size();
												int index = 0;
												for (Object[] o : inds) {
													index++;

													try {
														Object id = o[0];
														final Object fotoId = o[1];

														session = StreamingHibernateUtil.getInstance().currentSession();
														final FileFoto fileFoto = (FileFoto) session
																.createCriteria(TugasFileContent.class)
																.add(Restrictions.idEq(Long.parseLong(id.toString())))
																.uniqueResult();
														StreamingHibernateUtil.getInstance().closeSession();
														if (fileFoto != null) {
															File file = fileFoto.ambilFile();
															if (file != null && file.exists()) {
																String s = "Mengirim file " + file.getName() + " ("
																		+ Common.numberFormat.get()
																				.format((index * 100.0) / size)
																		+ "%)";
																System.out.println(s);
																label.setValue(s);

																com.google.api.services.drive.model.File fileKirim = driveUtilPerPengguna
																		.kirimBackupLangsung(null, file,
																				perguruanTinggi,
																				fileFoto.getClass().getSimpleName(),
																				new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
																							throws Exception {
																						com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
																								.getData();

																						if (fileUpload != null
																								&& fileUpload
																										.getId() != null) {

																							Session session = StreamingHibernateUtil
																									.getInstance()
																									.currentSession();
																							try {

																								session.refresh(
																										fileFoto);

																								fileFoto.setFoto(null);
																								fileFoto.setGdrive(
																										fileUpload
																												.getId());
																								fileFoto.setGdriveUsername(
																										tbmuser.getUserId());

																								session.getTransaction()
																										.begin();
																								session.update(
																										fileFoto);
																								session.getTransaction()
																										.commit();

																								FileFoto.hapusTotal(
																										fotoId.toString(),
																										session);

																							} catch (Exception e) {
																								StreamingHibernateUtil
																										.getInstance()
																										.rollbackTransaction();
																								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:1854");
																							}

																							StreamingHibernateUtil
																									.getInstance()
																									.closeSession();
																						}

																					}
																				});

																if (fileKirim == null) {
																	System.out.println(
																			"Gagal Terkirim " + file.getAbsolutePath());
																	break;
																} else {
																	System.out.println(
																			"Terkirim " + fileKirim.toPrettyString());

																}
															}
														}
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														break;
													}

												}
												label.setValue("Selesai");
											}
										}).start();
									}

								}
							});

				}

			}
		});

		Borderlayout myborderlayout = new Borderlayout();
		myborderlayout.setSclass("ais-tugas-jawaban-layout");
		myborderlayout.setParent(center);
		// Tinggi mengikuti kolom parent yang diregangkan oleh flex portal. Minimum
		// 2000px sesuai kebutuhan ruang kerja daftar jawaban dan penilaian tugas.
		myborderlayout.setHeight("100%");
		myborderlayout.setStyle("min-height:2000px;flex:1 1 auto;");

		vbox.appendChild(upload);
		vbox.appendChild(download);
		vbox.appendChild(drive);

		TampilanELearningAction.dilihat(tugas, "tugas", "Akses").setParent(vbox);
		// --- separator: Berkas Tugas | Nilai ---
		createSeparator().setParent(vbox);

		if (tugas.getFormatNilais() != null && perkuliahan != null && perkuliahan.getKurikulum() != null
				&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())
				&& mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null) {
			if (buttonMasukkanNilai != null) {
				buttonMasukkanNilai.setVisible(false);
				buttonMasukkanNilai.detach();
			}
			buttonMasukkanNilai = new MyToolbarbutton("fa-refresh", "Masukkan nilai ke nilai akhir");
			buttonMasukkanNilai.setParent(vbox);
			buttonMasukkanNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TugasMandiriHelper.this.tugas = tugas;
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilaiObe(perkuliahan, tugas.getFormatNilais());
						}
					});
				}
			});
		}

		else if (tugas.getFormatNilai() != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getSiswa() == null) {
			if (buttonMasukkanNilai != null) {
				buttonMasukkanNilai.setVisible(false);
				buttonMasukkanNilai.detach();
			}
			buttonMasukkanNilai = new MyToolbarbutton("fa-refresh",
					"Masukkan Nilai ke " + tugas.getFormatNilai().getNama());
			buttonMasukkanNilai.setParent(vbox);
			buttonMasukkanNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TugasMandiriHelper.this.tugas = tugas;
					// Auto-simpan nilai memori ke DB sebelum GradingHelper membaca keteranganNilai
					try {
						org.hibernate.Session sAutoSave = HibernateUtil.currentSession();
						sAutoSave.refresh(tugas);
						tugas.belum("tugas_file_content_" + tugas.getClass().getName());
						tugas.setKeteranganNilai(jsonObjectTugas.toString());
						Common.refreshUpdate(sAutoSave, tugas);
					} catch (Exception eSave) {
						ais.common.ErrorAuditUtil.record(eSave, "auto-audit(empty-catch) TugasMandiriHelper auto-simpan-sebelum-masukkan-nilai");
					}
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(perkuliahan, tugas.getFormatNilai());
						}
					});
				}
			});
		}

		if (tugas.getJenisItemPenilaianSiswa() != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getSiswa() == null) {

			buttonMasukkanNilai = new MyToolbarbutton("fa-refresh",
					"Masukkan Nilai ke " + tugas.getJenisItemPenilaianSiswa().getNama());

			buttonMasukkanNilai.setParent(vbox);
			buttonMasukkanNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// Auto-simpan nilai memori ke DB sebelum GradingHelper membaca keteranganNilai
					try {
						org.hibernate.Session sAutoSave2 = HibernateUtil.currentSession();
						sAutoSave2.refresh(tugas);
						tugas.belum("tugas_file_content_" + tugas.getClass().getName());
						tugas.setKeteranganNilai(jsonObjectTugas.toString());
						Common.refreshUpdate(sAutoSave2, tugas);
					} catch (Exception eSave2) {
						ais.common.ErrorAuditUtil.record(eSave2, "auto-audit(empty-catch) TugasMandiriHelper auto-simpan-sebelum-masukkan-nilai-siswa");
					}
					ais.common.GradingHelper.hitungNilaiBerdasarkanJenisItemPenilaianSiswa(jadwalPelajaran,
							tugas.getGrupKategoriItemPenilaianSiswa(), tugas.getGrupPenilaian(),
							tugas.getJenisItemPenilaianSiswa());
				}
			});

		}

		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			MyToolbarbutton buttonRekap = new MyToolbarbutton("fa-table", "Rekap Semua Tugas");
			buttonRekap.setTooltiptext("Lihat rekap semua tugas Anda dalam mata kuliah ini");
			buttonRekap.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {

						RekapHasilTugasMahasiswa addWindow = new RekapHasilTugasMahasiswa(false, mahasiswa,
								biodataCalonMahasiswa, pa.ambilVOPembelajaran());
						addWindow.setClosable(true);
						addWindow.setTitle("Rekap Semua Tugas");
						addWindow.setHeight("95%");
						addWindow.setWidth("90%");
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
						addWindow.onModal();

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:2042");
					}
				}
			});
			buttonRekap.setParent(vbox);
		}

		MyToolbarbutton masukDownloadNilai = new MyToolbarbutton("fa-download", "Download Nilai");
		masukDownloadNilai.setTooltiptext("Unduh daftar nilai tugas dalam format Excel untuk diedit di komputer");
		masukDownloadNilai.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && pa != null
				&& pa.getJadwalUjianPMB() == null && pa.getJadwalUjianPSB() == null);

		masukDownloadNilai.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					TreeMap<Long, TugasFileContent> tfcs = TugasMandiriHelper.this.tugas
							.ambilTugasFileContentTotal(treemapData,
									cari == null ? "" : cari.getValue().trim(), null, 500);
					List<TugasFileContent> tfcList = new ArrayList<TugasFileContent>(tfcs.values());
					Collections.sort(tfcList);

					JSONObject jsonKet = new JSONObject(TugasMandiriHelper.this.tugas.getKeteranganNilai());
					boolean isObe = !TugasMandiriHelper.this.obeFormatNilais.isEmpty();

					XSSFWorkbook wb = new XSSFWorkbook();
					XSSFSheet sheet = wb.createSheet("Nilai");

					XSSFRow headerRow = sheet.createRow(0);
					headerRow.createCell(0).setCellValue("ID");
					headerRow.createCell(1).setCellValue("NAMATEMP");
					if (!isObe) {
						headerRow.createCell(2).setCellValue("Nilai");
						headerRow.createCell(3).setCellValue("Keterangan");
					} else {
						int col = 2;
						for (FormatNilai fn : TugasMandiriHelper.this.obeFormatNilais) {
							String nama = fn.getNama() != null ? fn.getNama() : ("CPMK_" + fn.getId());
							headerRow.createCell(col++).setCellValue(nama);
						}
						headerRow.createCell(col).setCellValue("Keterangan");
					}

					int rowIdx = 1;
					for (TugasFileContent tfc : tfcList) {
						XSSFRow row = sheet.createRow(rowIdx++);
						row.createCell(0).setCellValue(tfc.getId());
						row.createCell(1).setCellValue(tfc.getNamaTemp() != null ? tfc.getNamaTemp() : "");

						String key = "";
						if (tfc.getMahasiswa() != null) key = tfc.getMahasiswa() + "_mhs";
						else if (tfc.getSiswa() != null) key = tfc.getSiswa() + "_siswa";
						else if (tfc.getBiodataCalonMahasiswa() != null) key = tfc.getBiodataCalonMahasiswa() + "_cal_mhs";
						else if (tfc.getCalonSiswa() != null) key = tfc.getCalonSiswa() + "_cal_siswa";

						String ket = (!key.isEmpty() && !jsonKet.isNull(key + "_ket"))
								? jsonKet.getString(key + "_ket") : "";

						if (!isObe) {
							double nilaiVal = (!key.isEmpty() && !jsonKet.isNull(key + "_nilai"))
									? jsonKet.getDouble(key + "_nilai") : 0.0;
							row.createCell(2).setCellValue(nilaiVal);
							row.createCell(3).setCellValue(ket);
						} else {
							int col = 2;
							for (FormatNilai fn : TugasMandiriHelper.this.obeFormatNilais) {
								String scoreKey = key + "_nilai_" + fn.getId();
								double score = (!key.isEmpty() && !jsonKet.isNull(scoreKey))
										? jsonKet.getDouble(scoreKey) : 0.0;
								row.createCell(col++).setCellValue(score);
							}
							row.createCell(col).setCellValue(ket);
						}
					}

					String fname = "Nilai_Tugas_" + TugasMandiriHelper.this.tugas.getId() + ".xlsx";
					File outFile = new File(Common.REAL_PATH + "/temp/" + fname);
					outFile.getParentFile().mkdirs();
					java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile);
					wb.write(fos);
					fos.close();
					Filedownload.save(outFile,
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"mengunduh nilai tugas mandiri", e, new String[] {
									"Muat ulang (refresh) halaman ini lalu coba unduh kembali.",
									"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});
		masukDownloadNilai.setParent(vbox);

		final MyToolbarbutton buttonNilai = new MyToolbarbutton("fa-upload", "Upload Nilai");
		buttonNilai.setTooltiptext("Unggah file Excel berisi nilai tugas untuk diproses sekaligus (format xlsx)");
		buttonNilai.setParent(vbox);
		buttonNilai.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && pa != null
				&& pa.getJadwalUjianPMB() == null && pa.getJadwalUjianPSB() == null);
		buttonNilai.setUpload(Common.ukuranFileUpload());
		buttonNilai.addEventListener("onUpload", new EventListener() {

			private void initSpreadsheet(final File fileUpload) throws Exception {

				TreeMap<Long, TugasFileContent> tugasFileContentsa = TugasMandiriHelper.this.tugas
						.ambilTugasFileContentTotal(treemapData, cari == null ? "" : cari.getValue().trim(), paging,
								500);
				List<TugasFileContent> pertemuanFileContents = new ArrayList<TugasFileContent>(
						tugasFileContentsa.values());

				XSSFWorkbook workbookUpload;
				try {
					workbookUpload = new XSSFWorkbook(fileUpload.getAbsolutePath());

					XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
					int size = sheetUpload.getLastRowNum() + 1;
					boolean isObeUpload = !TugasMandiriHelper.this.obeFormatNilais.isEmpty();
					// kolom keterangan: non-OBE=3, OBE=2+jumlah CPMK
					int ketCol = isObeUpload ? 2 + TugasMandiriHelper.this.obeFormatNilais.size() : 3;

					for (int i = 1; i < size; i++) {
						Long id = Common.getSheetContentAsLong(sheetUpload, 0, i);

						for (TugasFileContent tugasFileContent : pertemuanFileContents) {
							if (tugasFileContent != null && id != null && tugasFileContent.getId().equals(id)) {
								String keterangan = Common.getSheetContentAsString(sheetUpload, ketCol, i);

								Session session = HibernateUtil.currentSession();
								session.refresh(TugasMandiriHelper.this.tugas);

								String key = "";
								if (tugasFileContent.getMahasiswa() != null) {
									key = tugasFileContent.getMahasiswa() + "_mhs";
								} else if (tugasFileContent.getSiswa() != null) {
									key = tugasFileContent.getSiswa() + "_siswa";
								} else if (tugasFileContent.getBiodataCalonMahasiswa() != null) {
									key = tugasFileContent.getBiodataCalonMahasiswa() + "_cal_mhs";
								} else if (tugasFileContent.getCalonSiswa() != null) {
									key = tugasFileContent.getCalonSiswa() + "_cal_siswa";
								}

								JSONObject jsonObject = new JSONObject(tugas.getKeteranganNilai());
								jsonObject.put(key + "_ket", keterangan.trim());
								if (!isObeUpload) {
									// non-OBE: kolom 2 = Nilai
									Double nilai = Common.getSheetContentAsDouble(sheetUpload, 2, i);
									jsonObject.put(key + "_nilai", nilai);
								} else {
									// OBE: kolom 2,3,4,... untuk tiap CPMK sesuai urutan header
									for (int c = 0; c < TugasMandiriHelper.this.obeFormatNilais.size(); c++) {
										FormatNilai fn = TugasMandiriHelper.this.obeFormatNilais.get(c);
										Double nilaiCpmk = Common.getSheetContentAsDouble(sheetUpload, 2 + c, i);
										jsonObject.put(key + "_nilai_" + fn.getId(), nilaiCpmk);
									}
								}
								TugasMandiriHelper.this.tugas.setKeteranganNilai(jsonObject.toString());
								Common.refreshUpdate(session, TugasMandiriHelper.this.tugas);
								break;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit TugasMandiriHelper initSpreadsheet upload nilai OBE");
				}

			}

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {
					InputStream inputStream = media.getStreamData();
					File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();
					initSpreadsheet(file);

					reloadTugasFileContent();
				} else {
					MyMessageboxConfig.showFormat(
							"Berkas yang Bapak/Ibu unggah harus berformat Excel Open XML Spreadsheet (xlsx). Berkas: {V1}. Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut; (2) pilih menu Save As dan simpan dalam format Excel Open XML Spreadsheet (xlsx); (3) unggah kembali berkas dengan format yang sesuai.",

							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
				}
			}
		});

		// --- separator: Nilai | Kelola ---
		createSeparator().setParent(vbox);

		MyToolbarbutton buttonRefresh = new MyToolbarbutton("fa-refresh", "Refresh");
		buttonRefresh.setTooltiptext("Muat ulang daftar pengumpulan tugas — gunakan setelah ada perubahan data");
		buttonRefresh.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					if (tugas != null)
						tugas.belum("tugas_file_content_" + tugas.getClass().getName());
					tugas.reInitTugasFileContent();
					reloadTugasFileContent(true);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:2204");
				}
			}
		});
		buttonRefresh.setParent(vbox);

		// Tombol "Recovery" (permintaan user): buka riwayat SEMUA tugas (Envers) pada pembelajaran
		// yang sama (VoPembelajaran) + tombol KEMBALIKAN untuk memulihkan tugas terhapus/berubah.
		// Reuse GenericRevisiHelper (riwayat ADD/MOD/DEL + restore). Difilter ke perkuliahan /
		// jadwalPelajaran bila tugas berupa Pertemuan; hanya untuk dosen/admin (bukan mahasiswa).
		final MyToolbarbutton recovery = new MyToolbarbutton("fa-history", "Recovery");
		recovery.setVisible(!peserta);
		recovery.setTooltiptext(
				"Riwayat semua tugas pada pembelajaran ini — kembalikan/pulihkan tugas yang terhapus atau berubah.");
		recovery.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event evRec) throws Exception {
				try {
					String prop = null;
					Object val = null;
					if (tugas instanceof Pertemuan) {
						if (perkuliahan != null && perkuliahan.getId() != null) {
							prop = "perkuliahan";
							val = perkuliahan;
						} else if (jadwalPelajaran != null && jadwalPelajaran.getId() != null) {
							prop = "jadwalPelajaran";
							val = jadwalPelajaran;
						}
					}
					RevisiTugasHelper rh = new RevisiTugasHelper(tugas.getClass(), prop, val, null);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(rh);
					rh.setVisible(true);
					rh.onModal();
				} catch (Exception eRec) {
					Common.tampilErrorJikaAdmin(eRec);
				}
			}
		});
		recovery.setParent(vbox);
		// --- separator: Kelola | Kehadiran ---
		createSeparator().setParent(vbox);

		North myNorth = new North();
		myNorth.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(myNorth, true);
		myNorth.appendChild(vbox);

		if (!tugas.getJudultugas().isEmpty()) {

			Center mycenter = new Center();
			mycenter.setParent(myborderlayout);
			ais.ui.util.ZkCompat.setFlex(mycenter, true);

			uploadTugasGrid = new MyGrid();
			uploadTugasGrid.setWidth("100%");
			uploadTugasGrid.setHeight("100%");
			uploadTugasGrid.setSclass("ais-data-grid");

			if (biodataCalonMahasiswa != null || mahasiswa != null) {

				myborderlayoutlagi = new Borderlayout();
				myborderlayoutlagi.setParent(mycenter);

				Center mycenterlagi = new Center();
				mycenterlagi.setParent(myborderlayoutlagi);
				ais.ui.util.ZkCompat.setFlex(mycenterlagi, true);

				uploadTugasGrid.setParent(mycenterlagi);

			} else {
				int[] tabAktif = new int[]{1};
				final ais.ui.util.MyButtonTabbox mbt =
						ais.ui.util.MyButtonTabbox.buat(mycenter, "100%", tabAktif);

				// Tab 1 "Telah upload" - EAGER
				final Div panelTab1 = mbt.tambahTab(1, "Telah upload");

				myborderlayoutlagi = new Borderlayout();
				myborderlayoutlagi.setParent(panelTab1);
				myborderlayoutlagi.setHeight("100%");

				Center mycenterlagi = new Center();
				mycenterlagi.setParent(myborderlayoutlagi);
				ais.ui.util.ZkCompat.setFlex(mycenterlagi, true);

				uploadTugasGrid.setParent(mycenterlagi);

				// Tombol Simpan dan Batal hanya untuk dosen/admin (bukan peserta)
				if (!peserta) {
					South southTugas = new South();
					ais.ui.util.ZkCompat.setFlex(southTugas, true);
					southTugas.setParent(myborderlayoutlagi);
					// South hanya boleh punya SATU child langsung (LayoutRegion.beforeChildAdded).
					// Toolbar Simpan/Batal dan Paging (ditambahkan belakangan) harus berbagi
					// satu wadah Hbox ini, bukan di-parent-kan langsung ke South.
					Hbox southBoxTugas = new Hbox();
					southBoxTugas.setParent(southTugas);
					ais.ui.util.ZkCompat.setFlex(southBoxTugas, true);
					southBoxTugas.setAlign("center");
					org.zkoss.zul.Toolbar tbTugas = new org.zkoss.zul.Toolbar();
					tbTugas.setParent(southBoxTugas);

					MyToolbarbutton btnSimpanTugas = new MyToolbarbutton("fa-save", "Simpan");
					btnSimpanTugas.setTooltiptext("Simpan semua nilai dan keterangan ke database");
					btnSimpanTugas.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							try {
								Session s = HibernateUtil.currentSession();
								if (tugas != null) {
									s.refresh(tugas);
									tugas.belum("tugas_file_content_" + tugas.getClass().getName());
									tugas.setKeteranganNilai(jsonObjectTugas.toString());
									Common.refreshUpdate(s, tugas);
								}
								reloadTugasFileContent();
								ais.ui.util.MyMessageboxConfig.show("Data berhasil disimpan.", "Berhasil",
										ais.ui.util.MyMessageboxConfig.OK,
										ais.ui.util.MyMessageboxConfig.INFORMATION, null);
							} catch (Exception e) {
								PesanFormalHelper.tampilkanGagalException(
										"menyimpan nilai tugas", e,
										new String[] {
												"Muat ulang halaman dan masukkan kembali nilai.",
												"Pastikan Anda belum logout saat menyimpan.",
												"Detail teknis: " + e.getClass().getSimpleName() + " — " + e.getMessage(),
												"Hubungi Admin dengan menyertakan screenshot pesan ini."
										});
							}
						}
					});
					btnSimpanTugas.setParent(tbTugas);

					MyToolbarbutton btnBatalTugas = new MyToolbarbutton("fa-undo", "Batal");
					btnBatalTugas.setTooltiptext("Batal: muat ulang data dari database");
					btnBatalTugas.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							try {
								// Reset jsonObjectTugas ke nilai DB supaya perubahan di memori dibatalkan
								if (tugas != null) {
									Session s = HibernateUtil.currentSession();
									s.refresh(tugas);
									String kn = tugas.getKeteranganNilai();
									jsonObjectTugas = new JSONObject(
											kn == null || kn.trim().isEmpty() ? "{}" : kn.replace('\0', ' '));
								}
								reloadTugasFileContent(true);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}
					});
					btnBatalTugas.setParent(tbTugas);
				}

				// Tab 2 "Belum upload" - LAZY (hanya dimuat pertama kali)
				mbt.tambahTabLazy(2, "Belum upload", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div tabpanelPesertaBelum) throws Exception {
						if (!tabpanelPesertaBelum.getChildren().isEmpty()) return;

							Borderlayout myborderlayoutlagi = new Borderlayout();
							myborderlayoutlagi.setParent(tabpanelPesertaBelum);
							myborderlayoutlagi.setHeight("100%"); // tinggi pasti -> grid scroll di dalam

							MyHboxToolbar hbox = new MyHboxToolbar();
							hbox.appendChild(new MyLabelConfig("Peserta : "));
							final Textbox cari = new Textbox("");
							cari.setParent(hbox);
							cari.setCols(10);

							Center mycenterlagi = new Center();
							mycenterlagi.setParent(myborderlayoutlagi);
							ais.ui.util.ZkCompat.setFlex(mycenterlagi, true);

							final Grid grid = new Grid();
							grid.setSclass("dgrid");
							grid.setParent(mycenterlagi);

							Columns columns = new Columns();
							columns.setParent(grid);

							MyColumnConfig column = new MyColumnConfig();
							column.appendChild(hbox);
							column.setParent(columns);
							column.setWidth("60%");

							column = new MyColumnConfig("Terakhir akses tugas");
							column.setParent(columns);

							grid.setHeight("100%");
							grid.setWidth("100%");

							grid.setRowRenderer(new ais.ui.util.MyRowRenderer() {
								@Override
								public void render(Row arg0, Object arg1) throws Exception {
									arg0.setValign("top");
									arg0.setSclass("ais-tugas-upload-row");
									Mahasiswa mahasiswa = (arg1 instanceof Mahasiswa) ? (Mahasiswa) arg1 : null;
									BiodataCalonMahasiswa biodataCalonMahasiswa = (arg1 instanceof BiodataCalonMahasiswa)
											? (BiodataCalonMahasiswa) arg1
											: null;
									Siswa siswa = (arg1 instanceof Siswa) ? (Siswa) arg1 : null;
									CalonSiswa calonSiswa = (arg1 instanceof CalonSiswa) ? (CalonSiswa) arg1 : null;

									Hbox hbox = new Hbox();
									hbox.setStyle("gap:6px;align-items:center;");
									hbox.setParent(arg0);
									if (mahasiswa != null) {
										CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);
									} else if (biodataCalonMahasiswa != null) {
										CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(hbox);
									} else if (siswa != null) {
										CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox);
									} else if (calonSiswa != null) {
										CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(hbox);
									}

									Vbox vb = new Vbox();
									vb.setParent(hbox);
									String nim2 = mahasiswa != null ? mahasiswa.getNim()
											: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNoRegistrasi()
													: siswa != null ? siswa.getNomorInduk()
															: calonSiswa != null ? calonSiswa.getNomorInduk() : "";
									String nama2 = mahasiswa != null ? mahasiswa.getNama()
											: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama()
													: siswa != null ? siswa.getNama()
															: calonSiswa != null ? calonSiswa.getNama() : "";
									Label namaLbl2 = new Label(nim2 + " / " + nama2);
									namaLbl2.setSclass("ais-tugas-upload-nama");
									vb.appendChild(namaLbl2);

									Long id = mahasiswa != null ? mahasiswa.getId()
											: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
													: siswa != null ? siswa.getId()
															: calonSiswa != null ? calonSiswa.getId() : null;

									TreeMap<String, String> d = tugas.ambilData("tugas",
											id == null ? "-1" : id.toString());
									Label checkboxConfig = new Label(d.isEmpty() ? "Belum Akses Tugas"
											: d.values().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
									checkboxConfig.setParent(arg0);

								}
							});

							EventListener cariAkun = new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									List<Mahasiswa> mahasiswasTemorary = pa.ambilMahasiswa();
									List<Mahasiswa> copy = new ArrayList<Mahasiswa>();
									for (Mahasiswa mahasiswa : mahasiswasTemorary) {
										BiodataCalonMahasiswa biodataCalonMahasiswa = null;
										Siswa siswa = null;
										CalonSiswa calonSiswa = null;

										Long id = mahasiswa != null ? mahasiswa.getId()
												: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
														: siswa != null ? siswa.getId()
																: calonSiswa != null ? calonSiswa.getId() : null;

										if (id != null && (treemapData == null || !treemapData.containsKey(id))) {

											if (!tugas.getMhsYgTidakIkut().contains("," + id + ",")) {

												if (cari.getValue().trim().isEmpty() ||

														(mahasiswa != null &&

																((mahasiswa.getNim() != null
																		&& mahasiswa.getNim().toLowerCase().contains(
																				cari.getValue().toLowerCase().trim()))

																		||

																		(mahasiswa.getNama() != null
																				&& mahasiswa.getNama().toLowerCase()
																						.contains(cari.getValue()
																								.toLowerCase().trim()))

																)

														)

														||

														(biodataCalonMahasiswa != null &&

																((biodataCalonMahasiswa.getNoRegistrasi() != null
																		&& biodataCalonMahasiswa.getNoRegistrasi()
																				.toLowerCase()
																				.contains(cari.getValue().toLowerCase()
																						.trim()))

																		||

																		(biodataCalonMahasiswa.getNama() != null
																				&& biodataCalonMahasiswa.getNama()
																						.toLowerCase()
																						.contains(cari.getValue()
																								.toLowerCase().trim()))

																)

														)

												) {
													copy.add(mahasiswa);
												}
											}
										}
									}
									ListModel strset = new SimpleListModel(copy);
									grid.setModel(strset);
									mahasiswasTemorary = null;
									copy = null;
								}
							};

							cariAkun.onEvent(null);
							cari.addEventListener("onOK", cariAkun);

							MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-search", "Cari");
							toolbarbutton.setParent(hbox);
							toolbarbutton.addEventListener("onClick", cariAkun);

							MyToolbarbutton masuk = new MyToolbarbutton("fa-ban", "Tdk.upload tgs.dianggp.alpa");
							masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
									&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
									&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
									&& pa.getJadwalUjianPMB() == null && pa.getJadwalUjianPSB() == null);
							masuk.setTooltiptext("Tutup");
							masuk.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									TugasMandiriHelper.tidakUploadTugasDiangapTidakHadir(tugas, pa,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null)
															.display(pa, new DataLoader() {

																@Override
																public void loadData(Object value) {
																	try {
																		reloadTugasFileContent();
																	} catch (Exception e) {
																		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:2491");
																	}
																}
															}, 0);
												}
											});

								}
							});
							masuk.setParent(hbox);

							masuk = new MyToolbarbutton("fa-ban", "Tdk.Akses tgs.dianggp.alpa");
							masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
									&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
									&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
									&& pa.getJadwalUjianPMB() == null && pa.getJadwalUjianPSB() == null);
							masuk.setTooltiptext("Tutup");
							masuk.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									PertemuanPunyaDiskusiHelper.tidakAksesDianggapAlpa(tugas, "tugas",
											"Tidak Akses / Baca Tugas \"" + tugas.getJudultugas() + "\"",
											tugas.getMulai(), tugas.getSelesai(), new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null)
															.display(pa, new DataLoader() {

																@Override
																public void loadData(Object value) {
																	try {
																		reloadTugasFileContent();
																	} catch (Exception e) {
																		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:2526");
																	}
																}
															}, 0);
												}
											});

								}
							});
							masuk.setParent(hbox);

					}
				});

				// Tab 3 "Statistik" - selalu dimuat ulang setiap dipilih
				final Div panelStatistik = mbt.tambahTab(3, "Statistik");
				mbt.onSetiapPilih(3, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(panelStatistik);

						Borderlayout myborderlayoutProsentase = new Borderlayout();
						myborderlayoutProsentase.setParent(panelStatistik);

						Center centerProsentase = new Center();
						centerProsentase.setParent(myborderlayoutProsentase);
						ais.ui.util.ZkCompat.setFlex(centerProsentase, true);

						// Wadah dasbor: HTML/CSS murni (responsif HP & desktop), tanpa JFreeChart.
						Vbox dasborWrap = new Vbox();
						dasborWrap.setWidth("100%");
						dasborWrap.setStyle("padding:12px;box-sizing:border-box;overflow:auto;");
						dasborWrap.setParent(centerProsentase);

						List<Mahasiswa> mahasiswasTemorary = pa.ambilMahasiswa();

						int jumlah = 0;
						for (Mahasiswa mahasiswa : mahasiswasTemorary) {
							if (!tugas.getMhsYgTidakIkut().contains("," + mahasiswa.getId() + ",")) {
								jumlah++;
							}
						}

						TreeMap<String, String> d = tugas.ambilData("tugas", null);
						List<Dosen> dsn = pa.ambilDosen();
						int akses = treemapData == null ? 0 : treemapData.size();
						int belumAkses = jumlah - akses;

						int telahAkses1 = d.size();
						int jumlahTotal = mahasiswasTemorary.size() + dsn.size();
						int belumAkses1 = jumlahTotal - telahAkses1;

						int persenKumpul = jumlah <= 0 ? 0 : (int) Math.round((akses * 100.0) / jumlah);
						int persenBuka = jumlahTotal <= 0 ? 0 : (int) Math.round((telahAkses1 * 100.0) / jumlahTotal);

						// --- Kartu ringkasan angka penting ---
						List<ais.ui.util.DashboardUiKit.Stat> kartu = new ArrayList<ais.ui.util.DashboardUiKit.Stat>();
						kartu.add(new ais.ui.util.DashboardUiKit.Stat("Jumlah Peserta",
								Common.numberFormat.get().format(jumlah), "wajib mengumpulkan",
								ais.ui.util.DashboardUiKit.PRIMARY));
						kartu.add(new ais.ui.util.DashboardUiKit.Stat("Sudah Mengumpulkan",
								Common.numberFormat.get().format(akses), persenKumpul + "% dari peserta",
								ais.ui.util.DashboardUiKit.GOOD));
						kartu.add(new ais.ui.util.DashboardUiKit.Stat("Belum Mengumpulkan",
								Common.numberFormat.get().format(Math.max(0, belumAkses)),
								Math.max(0, 100 - persenKumpul) + "% dari peserta", ais.ui.util.DashboardUiKit.WARN));
						kartu.add(new ais.ui.util.DashboardUiKit.Stat("Sudah Membuka Tugas",
								Common.numberFormat.get().format(telahAkses1), persenBuka + "% dari total akun",
								ais.ui.util.DashboardUiKit.ACCENT));

						StringBuilder dash = new StringBuilder();
						dash.append(ais.ui.util.DashboardUiKit.descChip(
								"Melihat sekilas siapa yang sudah mengirim tugas ini dan siapa yang belum, lengkap dengan grafik agar mudah dipahami."));
						dash.append(ais.ui.util.DashboardUiKit.cards(kartu));

						dash.append(ais.ui.util.DashboardUiKit.openGrid(260));

						LinkedHashMap<String, Double> kumpulParts = new LinkedHashMap<String, Double>();
						kumpulParts.put("Sudah mengumpulkan", (double) akses);
						kumpulParts.put("Belum mengumpulkan", (double) Math.max(0, belumAkses));
						dash.append(ais.ui.util.DashboardUiKit.donut("Perbandingan Pengumpulan",
								"Bagian terisi adalah peserta yang sudah mengirim tugas; sisanya belum mengirim.",
								kumpulParts, false, "Belum ada peserta."));

						LinkedHashMap<String, Double> bukaParts = new LinkedHashMap<String, Double>();
						bukaParts.put("Sudah membuka", (double) telahAkses1);
						bukaParts.put("Belum membuka", (double) Math.max(0, belumAkses1));
						dash.append(ais.ui.util.DashboardUiKit.donut("Perbandingan Membuka Tugas",
								"Berapa banyak akun (mahasiswa & dosen) yang pernah membuka halaman tugas ini.",
								bukaParts, false, "Belum ada yang membuka."));

						dash.append(ais.ui.util.DashboardUiKit.closeGrid());

						LinkedHashMap<String, Integer> prog = new LinkedHashMap<String, Integer>();
						prog.put("Tingkat pengumpulan", persenKumpul);
						prog.put("Tingkat keterbacaan tugas", persenBuka);
						dash.append(ais.ui.util.DashboardUiKit.progressLines("Capaian Keseluruhan",
								"Semakin penuh batangnya, semakin banyak peserta yang aktif mengerjakan tugas.", prog));

						dasborWrap.appendChild(ais.ui.util.DashboardUiKit.html(dash.toString()));

						// Tren: berapa banyak tugas masuk di tiap tanggal (HTML/CSS, dari seluruh data).
						LinkedHashMap<String, Double> perHari = new LinkedHashMap<String, Double>();
						try {
							java.text.SimpleDateFormat hariFmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
							TreeMap<String, Integer> tmpHari = new TreeMap<String, Integer>();
							for (TugasFileContent fc : tugas.ambilTugasFileContentTotal().values()) {
								if (fc.getUploadDate() == null) {
									continue;
								}
								String hari = hariFmt.format(fc.getUploadDate());
								Integer c = tmpHari.get(hari);
								tmpHari.put(hari, c == null ? 1 : c + 1);
							}
							for (Map.Entry<String, Integer> e : tmpHari.entrySet()) {
								perHari.put(e.getKey(), (double) e.getValue());
							}
						} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:2649");
						}
						if (!perHari.isEmpty()) {
							dasborWrap.appendChild(ais.ui.util.DashboardUiKit.html(
									ais.ui.util.DashboardUiKit.barList("Tugas Masuk per Tanggal",
											"Menunjukkan pada tanggal berapa saja tugas paling banyak dikirim peserta.",
											perHari, ais.ui.util.DashboardUiKit.PRIMARY, "tugas", false,
											"Belum ada tugas yang masuk.")));
						}

						// --- Distribusi Nilai: histogram 5 rentang (0-20, 21-40, 41-60, 61-80, 81-100) ---
						try {
							LinkedHashMap<String, Double> distribusiNilai = new LinkedHashMap<String, Double>();
							distribusiNilai.put("0 - 20", 0.0);
							distribusiNilai.put("21 - 40", 0.0);
							distribusiNilai.put("41 - 60", 0.0);
							distribusiNilai.put("61 - 80", 0.0);
							distribusiNilai.put("81 - 100", 0.0);
							int totalDinilai = 0;
							for (ais.database.model.file.TugasFileContent fcN : tugas.ambilTugasFileContentTotal().values()) {
								if (fcN == null || fcN.getNilai() == null) {
									continue;
								}
								double nv = fcN.getNilai();
								if (nv <= 0) {
									continue;
								}
								totalDinilai++;
								if (nv <= 20) {
									distribusiNilai.put("0 - 20", distribusiNilai.get("0 - 20") + 1);
								} else if (nv <= 40) {
									distribusiNilai.put("21 - 40", distribusiNilai.get("21 - 40") + 1);
								} else if (nv <= 60) {
									distribusiNilai.put("41 - 60", distribusiNilai.get("41 - 60") + 1);
								} else if (nv <= 80) {
									distribusiNilai.put("61 - 80", distribusiNilai.get("61 - 80") + 1);
								} else {
									distribusiNilai.put("81 - 100", distribusiNilai.get("81 - 100") + 1);
								}
							}
							if (totalDinilai > 0) {
								dasborWrap.appendChild(ais.ui.util.DashboardUiKit.html(
										ais.ui.util.DashboardUiKit.barList("Distribusi Nilai",
												"Sebaran nilai tugas dalam 5 rentang — berguna untuk melihat apakah sebagian besar peserta memahami materi dengan baik.",
												distribusiNilai, ais.ui.util.DashboardUiKit.GOOD, "peserta", false,
												"Belum ada nilai yang diinput.")));
							}
						} catch (Exception eDistrib) {
							ais.common.ErrorAuditUtil.record(eDistrib, "auto-audit(empty-catch) TugasMandiriHelper.distribusiNilai");
						}

						// --- Radar/Spider Chart Sub-CPMK (hanya untuk OBE) ---
						if (perkuliahan != null && perkuliahan.getKurikulum() != null
								&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
							try {
								Session sessionObe = HibernateUtil.currentSession();
								List<FormatNilai> obeNilaisRadar = Common.getFormatNilais(sessionObe, perkuliahan);
								JSONObject keteranganNilaiObj = new JSONObject(tugas.getKeteranganNilai());
								JSONObject formatNilaisObj = new JSONObject(tugas.getFormatNilais());
								TreeMap<Long, ais.database.model.file.TugasFileContent> allTfc = tugas.ambilTugasFileContentTotal();
								LinkedHashMap<String, Double> cpmkAvg = new LinkedHashMap<String, Double>();
								for (FormatNilai fn : obeNilaisRadar) {
									if (fn.getStatusPertemuan() == null) {
										continue;
									}
									if (formatNilaisObj.isNull(fn.getId().toString())) {
										continue;
									}
									double sumFn = 0;
									int countFn = 0;
									for (ais.database.model.file.TugasFileContent tfc : allTfc.values()) {
										String keyFn = "";
										if (tfc.getMahasiswa() != null) {
											keyFn = tfc.getMahasiswa() + "_mhs";
										} else if (tfc.getSiswa() != null) {
											keyFn = tfc.getSiswa() + "_siswa";
										} else if (tfc.getBiodataCalonMahasiswa() != null) {
											keyFn = tfc.getBiodataCalonMahasiswa() + "_cal_mhs";
										} else if (tfc.getCalonSiswa() != null) {
											keyFn = tfc.getCalonSiswa() + "_cal_siswa";
										}
										String scoreKey = keyFn + "_nilai_" + fn.getId();
										if (!keteranganNilaiObj.isNull(scoreKey)) {
											sumFn += keteranganNilaiObj.getDouble(scoreKey);
											countFn++;
										}
									}
									String lbl = fn.getNama() == null ? "CPMK" : fn.getNama();
									if (lbl.length() > 14) {
										lbl = lbl.substring(0, 14) + "..";
									}
									cpmkAvg.put(lbl, countFn > 0 ? sumFn / countFn : 0.0);
								}
								if (cpmkAvg.size() >= 3) {
									dasborWrap.appendChild(ais.ui.util.DashboardUiKit.html(
											buildRadarChartHtml("Rata-rata Nilai per Sub-CPMK",
													"Radar menunjukkan rata-rata nilai mahasiswa untuk setiap Sub-CPMK yang dinilai pada tugas ini.",
													cpmkAvg, 100.0)));
								}
							} catch (Exception eRadar) {
								ais.common.ErrorAuditUtil.record(eRadar, "auto-audit(empty-catch) TugasMandiriHelper.radarCpmk");
							}
						}

						d.clear();
						d = null;
						dsn.clear();
						dsn = null;

					}
				});

				// Tab 4 "Peserta yg tdk perlu ikt" - LAZY (hanya dimuat pertama kali)
				mbt.tambahTabLazy(4, "Peserta yg tdk perlu ikt", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div tabpanelPeserta) throws Exception {
						if (!tabpanelPeserta.getChildren().isEmpty()) return;
							final ais.database.model.TugasPertemuan tp =
									(tugas instanceof ais.database.model.TugasPertemuan)
									? (ais.database.model.TugasPertemuan) tugas : null;

							// Deteksi OBE: apakah perkuliahan ini memakai kurikulum OBE dan
							// apakah tugas ini punya Sub-CPMK yang terpilih?
							final ais.database.model.Perkuliahan perkuliahan = pa.getPerkuliahan();
							final boolean obe = perkuliahan != null
									&& perkuliahan.getKurikulum() != null
									&& perkuliahan.getKurikulum().apakahObe(
											perkuliahan.getTahunAjaran(),
											perkuliahan.getGanjilGenap());
							final java.util.List<ais.database.model.FormatNilai> subCpmkTerpilih =
									new java.util.ArrayList<ais.database.model.FormatNilai>();
							if (obe) {
								try {
									org.json.JSONObject jfn = new org.json.JSONObject(
											tugas.getFormatNilais());
									for (ais.database.model.FormatNilai fn : Common.getFormatNilais(
											HibernateUtil.currentSession(), perkuliahan)) {
										if (fn != null && fn.getId() != null
												&& fn.getStatusPertemuan() != null
												&& !jfn.isNull(fn.getId().toString())) {
											subCpmkTerpilih.add(fn);
										}
									}
								} catch (Exception eFn) {
									ais.common.ErrorAuditUtil.record(eFn, "auto-audit");
								}
							}
							final boolean adaSubCpmk = tp != null && obe && !subCpmkTerpilih.isEmpty();

							Borderlayout myborderlayoutlagi = new Borderlayout();
							myborderlayoutlagi.setParent(tabpanelPeserta);
							myborderlayoutlagi.setHeight("100%");

							MyHboxToolbar hbox = new MyHboxToolbar();
							hbox.appendChild(new MyLabelConfig("Peserta : "));
							final Textbox cari = new Textbox("");
							cari.setParent(hbox);
							cari.setCols(20);

							Center mycenterlagi = new Center();
							mycenterlagi.setParent(myborderlayoutlagi);
							ais.ui.util.ZkCompat.setFlex(mycenterlagi, true);

							final Grid grid = new Grid();
							grid.setSclass("dgrid");
							grid.setParent(mycenterlagi);

							Columns columns = new Columns();
							columns.setParent(grid);

							// Kolom 1: Nama Peserta + kotak pencarian
							MyColumnConfig column = new MyColumnConfig();
							column.appendChild(hbox);
							column.setParent(columns);
							column.setWidth(adaSubCpmk ? "25%" : "60%");
							column.setAlign("left");

							// Kolom 2: "Tidak perlu ikut tugas" + header checkbox massal
							final MyCheckboxConfig checkboxConfigAll =
									new MyCheckboxConfig("Tidak perlu ikut tugas");
							column = new MyColumnConfig();
							column.appendChild(checkboxConfigAll);
							column.setParent(columns);
							column.setWidth(adaSubCpmk ? "10%" : "20%");
							column.setAlign("left");

							// Kolom 3: "Boleh Upload Ulang" + header checkbox massal
							final MyCheckboxConfig checkboxConfigAllUpload =
									new MyCheckboxConfig("Boleh Upload Ulang");
							column = new MyColumnConfig();
							column.appendChild(checkboxConfigAllUpload);
							column.setParent(columns);
							column.setWidth(adaSubCpmk ? "10%" : "20%");
							column.setAlign("left");

							// Kolom 4 & 5 hanya tampil bila OBE ada Sub-CPMK
							if (adaSubCpmk) {
								MyColumnConfig columnNilai = new MyColumnConfig();
								columnNilai.appendChild(
										new MyLabelConfig("Nilai Manual (Nilai & Keterangan)"));
								columnNilai.setParent(columns);
								columnNilai.setWidth("25%");
								columnNilai.setAlign("left");

								MyColumnConfig columnCpmk = new MyColumnConfig();
								columnCpmk.appendChild(
										new MyLabelConfig("Sub-CPMK yang dikerjakan (OBE)"));
								columnCpmk.setParent(columns);
								columnCpmk.setWidth("30%");
								columnCpmk.setAlign("left");
							}

							ais.ui.util.ZkCompat.setFixedLayout(grid, true);
							grid.setHeight("100%");
							grid.setWidth("100%");

							grid.setRowRenderer(new ais.ui.util.MyRowRenderer() {
								@Override
								public void render(Row arg0, Object arg1) throws Exception {
									arg0.setValign("top");
									final Row rowPeserta = arg0;
									final Mahasiswa mahasiswa =
											(arg1 instanceof Mahasiswa) ? (Mahasiswa) arg1 : null;
									final BiodataCalonMahasiswa biodataCalonMahasiswa =
											(arg1 instanceof BiodataCalonMahasiswa)
											? (BiodataCalonMahasiswa) arg1 : null;
									final Siswa siswa =
											(arg1 instanceof Siswa) ? (Siswa) arg1 : null;
									final CalonSiswa calonSiswa =
											(arg1 instanceof CalonSiswa) ? (CalonSiswa) arg1 : null;

									// Sel 1: Foto + NIM/Nama (flex div agar rata-kiri dan sejajar)
									org.zkoss.zul.Div selPeserta = new org.zkoss.zul.Div();
									selPeserta.setParent(arg0);
									selPeserta.setStyle(
											"display:flex;align-items:center;gap:9px;width:100%;"
											+ "text-align:left;box-sizing:border-box;");
									if (mahasiswa != null)
										CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(selPeserta);
									else if (biodataCalonMahasiswa != null)
										CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa)
												.setParent(selPeserta);
									else if (siswa != null)
										CommonMedia.tampilkanGambarKecil(siswa).setParent(selPeserta);
									else if (calonSiswa != null)
										CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(selPeserta);

									String nim3 = mahasiswa != null ? mahasiswa.getNim()
											: biodataCalonMahasiswa != null
													? biodataCalonMahasiswa.getNoRegistrasi()
													: siswa != null ? siswa.getNomorInduk()
															: calonSiswa != null
																	? calonSiswa.getNomorInduk() : "";
									String nama3 = mahasiswa != null ? mahasiswa.getNama()
											: biodataCalonMahasiswa != null
													? biodataCalonMahasiswa.getNama()
													: siswa != null ? siswa.getNama()
															: calonSiswa != null ? calonSiswa.getNama() : "";
									new Html("<div style='text-align:left;line-height:1.45;'>"
											+ "<div style='font-size:11px;color:#64748b;font-weight:600;'>"
											+ ais.ui.util.DashboardUiKit.esc(nim3) + "</div>"
											+ "<div style='font-size:13px;color:#0f172a;font-weight:700;'>"
											+ ais.ui.util.DashboardUiKit.esc(nama3)
											+ "</div></div>").setParent(selPeserta);

									Long id = mahasiswa != null ? mahasiswa.getId()
											: biodataCalonMahasiswa != null
													? biodataCalonMahasiswa.getId()
													: siswa != null ? siswa.getId()
															: calonSiswa != null ? calonSiswa.getId() : null;

									// Referensi bersama untuk kontrol visibilitas OBE secara dinamis
									final org.zkoss.zul.Div[] wrapNilaiRef =
											new org.zkoss.zul.Div[1];
									final org.zkoss.zul.Div[] wrapCpmkRef =
											new org.zkoss.zul.Div[1];
									final org.zkoss.zul.Checkbox[] cbPaksaRef =
											new org.zkoss.zul.Checkbox[1];
									final java.util.LinkedHashMap<Long, org.zkoss.zul.Hbox> nilaiRowMap =
											new java.util.LinkedHashMap<Long, org.zkoss.zul.Hbox>();
									final java.util.LinkedHashMap<Long, org.zkoss.zul.Checkbox> obeCbMap =
											new java.util.LinkedHashMap<Long, org.zkoss.zul.Checkbox>();

									// Sel 2: Checkbox "Tidak perlu ikut tugas"
									final MyCheckboxConfig checkboxConfig =
											new MyCheckboxConfig("Tidak perlu ikut tugas");
									checkboxConfig.setDisabled(mahasiswa == null);
									checkboxConfig.setChecked(
											tugas.getMhsYgTidakIkut().contains("," + id + ","));
									checkboxConfig.setParent(arg0);

									// perbaruiVisibilitasNilai: segarkan tampilan OBE setiap
									// kali "Tidak perlu ikut", "Paksa", atau Sub-CPMK berubah.
									// Nilai Manual & Sub-CPMK tampil HANYA bila peserta AKTIF
									// (tidak perlu ikut = tidak dicentang). Baris nilai per
									// Sub-CPMK tampil hanya bila Paksa dicentang DAN Sub-CPMK
									// yang bersangkutan dicentang.
									final EventListener perbaruiVisibilitasNilai =
											new EventListener() {
										@Override
										public void onEvent(Event evVis) throws Exception {
											boolean ikut  = !checkboxConfig.isChecked();
											boolean paksa = cbPaksaRef[0] != null
													&& cbPaksaRef[0].isChecked();
											if (wrapNilaiRef[0] != null)
												wrapNilaiRef[0].setVisible(ikut);
											if (wrapCpmkRef[0] != null)
												wrapCpmkRef[0].setVisible(ikut);
											for (java.util.Map.Entry<Long, org.zkoss.zul.Hbox> en
													: nilaiRowMap.entrySet()) {
												org.zkoss.zul.Checkbox obeCheck =
														obeCbMap.get(en.getKey());
												boolean obeOn = obeCheck == null
														|| obeCheck.isChecked();
												en.getValue().setVisible(ikut && paksa && obeOn);
											}
											if (rowPeserta != null) rowPeserta.invalidate();
										}
									};

									checkboxConfig.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Session session = HibernateUtil.currentSession();
											if (tugas.getId() != null) session.refresh(tugas);
											Long id = mahasiswa != null ? mahasiswa.getId()
													: biodataCalonMahasiswa != null
															? biodataCalonMahasiswa.getId()
															: siswa != null ? siswa.getId()
																	: calonSiswa != null
																			? calonSiswa.getId() : null;
											tugas.setMhsYgTidakIkut(ais.common.GradingHelper.ubahIdPadaCsvBerpagarKoma(
													tugas.getMhsYgTidakIkut(), id, checkboxConfig.isChecked()));
											Common.refreshUpdate(session, tugas);
											tabpanelFileTugasPertemuan.getLinkedTab()
													.setLabel(tugas.getJudultugas());
											perbaruiVisibilitasNilai.onEvent(null);
										}
									});

									// Sel 3: Checkbox "Boleh Upload Ulang"
									final MyCheckboxConfig uploadulang =
											new MyCheckboxConfig("Boleh Upload Ulang");
									uploadulang.setDisabled(mahasiswa == null);
									uploadulang.setChecked(
											tugas.getMhsBolehUploadUlang().contains("," + id + ","));
									uploadulang.setParent(arg0);
									uploadulang.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Session session = HibernateUtil.currentSession();
											if (tugas.getId() != null) session.refresh(tugas);
											Long id = mahasiswa != null ? mahasiswa.getId()
													: biodataCalonMahasiswa != null
															? biodataCalonMahasiswa.getId()
															: siswa != null ? siswa.getId()
																	: calonSiswa != null
																			? calonSiswa.getId() : null;
											tugas.setMhsBolehUploadUlang(ais.common.GradingHelper.ubahIdPadaCsvBerpagarKoma(
													tugas.getMhsBolehUploadUlang(), id, uploadulang.isChecked()));
											Common.refreshUpdate(session, tugas);
											tabpanelFileTugasPertemuan.getLinkedTab()
													.setLabel(tugas.getJudultugas());
										}
									});

									// Sel 4 (OBE): Nilai Manual per Sub-CPMK + tombol Paksa
									if (adaSubCpmk) {
										if (mahasiswa != null) {
											org.zkoss.zul.Div wrapNilai = new org.zkoss.zul.Div();
											wrapNilai.setStyle("padding:2px 0;");
											wrapNilai.setParent(arg0);
											wrapNilaiRef[0] = wrapNilai;
											final String mhsKey = mahasiswa.getId().toString();

											// Praisi Doublebox dari JSON nilai_manual_json
											org.json.JSONObject mhsEntryTmp =
													new org.json.JSONObject();
											try {
												String njStr = tp.getNilaiManualJson();
												if (njStr != null && !njStr.isEmpty()
														&& !njStr.equals("{}")) {
													org.json.JSONObject jAll =
															new org.json.JSONObject(njStr);
													org.json.JSONObject ent =
															jAll.optJSONObject(mhsKey);
													if (ent != null) mhsEntryTmp = ent;
												}
											} catch (Exception eNilai) { /* abaikan */ }
											final org.json.JSONObject mhsEntryFinal = mhsEntryTmp;

											for (final ais.database.model.FormatNilai fn
													: subCpmkTerpilih) {
												if (fn == null || fn.getId() == null) continue;
												final String fnKey    = "fn_" + fn.getId();
												final String fnKeyKet = fnKey + "_ket";

												// Baris per Sub-CPMK: Label + Doublebox + Textbox
												org.zkoss.zul.Hbox rowN = new org.zkoss.zul.Hbox();
												rowN.setStyle(
														"align-items:center;gap:4px;margin-bottom:2px;");
												rowN.setParent(wrapNilai);
												nilaiRowMap.put(fn.getId(), rowN);
												new org.zkoss.zul.Label(fn.getNama() + ": ")
														.setParent(rowN);

												final org.zkoss.zul.Doublebox db =
														new org.zkoss.zul.Doublebox();
												Double initVal = null;
												if (!mhsEntryFinal.isNull(fnKey)) {
													try {
														initVal = mhsEntryFinal.getDouble(fnKey);
													} catch (Exception ex) { /* skip */ }
												}
												db.setValue(initVal);
												db.setWidth("90px");
												db.setParent(rowN);
												db.addEventListener("onChange", new EventListener() {
													@Override
													public void onEvent(Event ev) throws Exception {
														Session s = HibernateUtil.currentSession();
														if (tp.getId() != null) s.refresh(tp);
														String nj = tp.getNilaiManualJson();
														org.json.JSONObject jAll =
																(nj != null && !nj.isEmpty()
																		&& !nj.equals("{}"))
																? new org.json.JSONObject(nj)
																: new org.json.JSONObject();
														org.json.JSONObject entry =
																jAll.optJSONObject(mhsKey);
														if (entry == null)
															entry = new org.json.JSONObject();
														if (db.getValue() != null)
															entry.put(fnKey, db.getValue());
														else
															entry.remove(fnKey);
														jAll.put(mhsKey, entry);
														tp.setNilaiManualJson(jAll.toString());
														Common.refreshUpdate(s, tp);
													}
												});

												// Textbox keterangan — pakai setTooltiptext,
												// BUKAN setPlaceholder (ZK5 batch-error)
												final org.zkoss.zul.Textbox tbKet =
														new org.zkoss.zul.Textbox();
												tbKet.setValue(mhsEntryFinal.isNull(fnKeyKet) ? ""
														: mhsEntryFinal.optString(fnKeyKet, ""));
												tbKet.setWidth("180px");
												tbKet.setTooltiptext(
														"Keterangan nilai " + fn.getNama());
												tbKet.setParent(rowN);
												tbKet.addEventListener("onChange",
														new EventListener() {
													@Override
													public void onEvent(Event ev) throws Exception {
														Session s = HibernateUtil.currentSession();
														if (tp.getId() != null) s.refresh(tp);
														String nj = tp.getNilaiManualJson();
														org.json.JSONObject jAll =
																(nj != null && !nj.isEmpty()
																		&& !nj.equals("{}"))
																? new org.json.JSONObject(nj)
																: new org.json.JSONObject();
														org.json.JSONObject entry =
																jAll.optJSONObject(mhsKey);
														if (entry == null)
															entry = new org.json.JSONObject();
														String v = tbKet.getValue() == null ? ""
																: tbKet.getValue().trim();
														if (v.isEmpty()) entry.remove(fnKeyKet);
														else             entry.put(fnKeyKet, v);
														jAll.put(mhsKey, entry);
														tp.setNilaiManualJson(jAll.toString());
														Common.refreshUpdate(s, tp);
													}
												});
											}

											// Checkbox "Paksa pakai nilai ini jika tetap mengumpulkan tugas"
											final org.zkoss.zul.Checkbox cbPaksa =
													new org.zkoss.zul.Checkbox(
													"Paksa pakai nilai ini jika tetap mengumpulkan tugas");
											cbPaksa.setChecked(
													mhsEntryFinal.optBoolean("paksa", false));
											cbPaksa.setStyle("margin-top:4px;display:block;");
											cbPaksa.setParent(wrapNilai);
											cbPaksaRef[0] = cbPaksa;
											cbPaksa.addEventListener("onCheck",
													new EventListener() {
												@Override
												public void onEvent(Event ev) throws Exception {
													Session s = HibernateUtil.currentSession();
													if (tp.getId() != null) s.refresh(tp);
													String nj = tp.getNilaiManualJson();
													org.json.JSONObject jAll =
															(nj != null && !nj.isEmpty()
																	&& !nj.equals("{}"))
															? new org.json.JSONObject(nj)
															: new org.json.JSONObject();
													org.json.JSONObject entry =
															jAll.optJSONObject(mhsKey);
													if (entry == null)
														entry = new org.json.JSONObject();
													entry.put("paksa", cbPaksa.isChecked());
													jAll.put(mhsKey, entry);
													tp.setNilaiManualJson(jAll.toString());
													Common.refreshUpdate(s, tp);
													perbaruiVisibilitasNilai.onEvent(null);
												}
											});
										} else {
											// Bukan mahasiswa: biarkan sel kosong agar kolom sejajar
											new org.zkoss.zul.Label("").setParent(arg0);
										}
									}

									// Sel 5 (OBE): Checklist Sub-CPMK per peserta
									if (adaSubCpmk && mahasiswa != null) {
										org.zkoss.zul.Div wrapCpmk = new org.zkoss.zul.Div();
										wrapCpmk.setStyle(
												"display:flex;flex-wrap:wrap;gap:4px 14px;");
										wrapCpmk.setParent(arg0);
										wrapCpmkRef[0] = wrapCpmk;
										final Long mhsIdCpmk = mahasiswa.getId();
										final java.util.Set<Long> terpilihPeserta =
												tp.ambilSubCpmkPeserta(mhsIdCpmk);
										final java.util.LinkedHashMap<Long, org.zkoss.zul.Checkbox>
												cbMap = new java.util.LinkedHashMap<Long,
														org.zkoss.zul.Checkbox>();

										EventListener onCpmk = new EventListener() {
											@Override
											public void onEvent(Event ev) throws Exception {
												Session s = HibernateUtil.currentSession();
												if (tp.getId() != null) s.refresh(tp);
												org.json.JSONObject j = new org.json.JSONObject(
														tp.getSubCpmkPerPeserta());
												org.json.JSONArray arr =
														new org.json.JSONArray();
												int dipilih = 0;
												for (java.util.Map.Entry<Long,
														org.zkoss.zul.Checkbox> en
														: cbMap.entrySet()) {
													if (en.getValue().isChecked()) {
														arr.put(en.getKey().toString());
														dipilih++;
													}
												}
												// semua dicentang → hapus kunci (kembali ke default semua)
												if (dipilih >= cbMap.size()) {
													j.remove(mhsIdCpmk.toString());
												} else {
													j.put(mhsIdCpmk.toString(), arr);
												}
												tp.setSubCpmkPerPeserta(j.toString());
												Common.refreshUpdate(s, tp);
												perbaruiVisibilitasNilai.onEvent(null);
											}
										};

										for (ais.database.model.FormatNilai fn : subCpmkTerpilih) {
											if (fn == null || fn.getId() == null) continue;
											org.zkoss.zul.Checkbox cb =
													new org.zkoss.zul.Checkbox(fn.getNama());
											cb.setChecked(terpilihPeserta == null
													|| terpilihPeserta.contains(fn.getId()));
											cb.addEventListener("onCheck", onCpmk);
											cb.setParent(wrapCpmk);
											cbMap.put(fn.getId(), cb);
											obeCbMap.put(fn.getId(), cb);
										}
									} else if (adaSubCpmk) {
										new org.zkoss.zul.Label("").setParent(arg0);
									}

									// Terapkan visibilitas awal sesuai data yang sudah tersimpan
									perbaruiVisibilitasNilai.onEvent(null);
								}
							});

							// Listener pencarian peserta
							EventListener cariAkun = new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									List<Mahasiswa> mahasiswasTemorary = pa.ambilMahasiswa();
									List<Mahasiswa> copy = new ArrayList<Mahasiswa>();
									for (Mahasiswa mahasiswa : mahasiswasTemorary) {
										BiodataCalonMahasiswa biodataCalonMahasiswa = null;
										if (cari.getValue().trim().isEmpty()
												|| (mahasiswa != null && (
														(mahasiswa.getNim() != null
																&& mahasiswa.getNim().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
														|| (mahasiswa.getNama() != null
																&& mahasiswa.getNama().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
												))
												|| (biodataCalonMahasiswa != null && (
														(biodataCalonMahasiswa.getNoRegistrasi() != null
																&& biodataCalonMahasiswa
																.getNoRegistrasi().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
														|| (biodataCalonMahasiswa.getNama() != null
																&& biodataCalonMahasiswa.getNama()
																.toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
												))
										) {
											copy.add(mahasiswa);
										}
									}
									ListModel strset = new SimpleListModel(copy);
									grid.setModel(strset);
									mahasiswasTemorary = null;
									copy = null;
								}
							};

							cariAkun.onEvent(null);
							cari.addEventListener("onOK", cariAkun);

							MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-search", "Cari");
							toolbarbutton.setParent(hbox);
							toolbarbutton.addEventListener("onClick", cariAkun);

							// Tombol massal: centang/hapus semua "Tidak perlu ikut tugas"
							checkboxConfigAll.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									if (tugas.getId() != null) session.refresh(tugas);
									List<Mahasiswa> mahasiswasTemorary = pa.ambilMahasiswa();
									List<Mahasiswa> copy = new ArrayList<Mahasiswa>();
									for (Mahasiswa mahasiswa : mahasiswasTemorary) {
										BiodataCalonMahasiswa biodataCalonMahasiswa = null;
										Siswa siswa = null;
										CalonSiswa calonSiswa = null;
										if (cari.getValue().trim().isEmpty()
												|| (mahasiswa != null && (
														(mahasiswa.getNim() != null
																&& mahasiswa.getNim().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
														|| (mahasiswa.getNama() != null
																&& mahasiswa.getNama().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
												))
												|| (biodataCalonMahasiswa != null && (
														(biodataCalonMahasiswa.getNoRegistrasi() != null
																&& biodataCalonMahasiswa
																.getNoRegistrasi().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
														|| (biodataCalonMahasiswa.getNama() != null
																&& biodataCalonMahasiswa.getNama()
																.toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
												))
										) {
											Long id = mahasiswa != null ? mahasiswa.getId()
													: biodataCalonMahasiswa != null
															? biodataCalonMahasiswa.getId()
															: siswa != null ? siswa.getId()
																	: calonSiswa != null
																			? calonSiswa.getId() : null;
											tugas.setMhsYgTidakIkut(ais.common.GradingHelper.ubahIdPadaCsvBerpagarKoma(
													tugas.getMhsYgTidakIkut(), id, checkboxConfigAll.isChecked()));
											copy.add(mahasiswa);
										}
									}
									Common.refreshUpdate(session, tugas);
									ListModel strset = new SimpleListModel(copy);
									grid.setModel(strset);
									mahasiswasTemorary = null;
									copy = null;
									tabpanelFileTugasPertemuan.getLinkedTab()
											.setLabel(tugas.getJudultugas());
								}
							});

							// Tombol massal: centang/hapus semua "Boleh Upload Ulang"
							checkboxConfigAllUpload.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									if (tugas.getId() != null) session.refresh(tugas);
									List<Mahasiswa> mahasiswasTemorary = pa.ambilMahasiswa();
									List<Mahasiswa> copy = new ArrayList<Mahasiswa>();
									for (Mahasiswa mahasiswa : mahasiswasTemorary) {
										BiodataCalonMahasiswa biodataCalonMahasiswa = null;
										Siswa siswa = null;
										CalonSiswa calonSiswa = null;
										if (cari.getValue().trim().isEmpty()
												|| (mahasiswa != null && (
														(mahasiswa.getNim() != null
																&& mahasiswa.getNim().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
														|| (mahasiswa.getNama() != null
																&& mahasiswa.getNama().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
												))
												|| (biodataCalonMahasiswa != null && (
														(biodataCalonMahasiswa.getNoRegistrasi() != null
																&& biodataCalonMahasiswa
																.getNoRegistrasi().toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
														|| (biodataCalonMahasiswa.getNama() != null
																&& biodataCalonMahasiswa.getNama()
																.toLowerCase()
																.contains(cari.getValue()
																		.toLowerCase().trim()))
												))
										) {
											Long id = mahasiswa != null ? mahasiswa.getId()
													: biodataCalonMahasiswa != null
															? biodataCalonMahasiswa.getId()
															: siswa != null ? siswa.getId()
																	: calonSiswa != null
																			? calonSiswa.getId() : null;
											tugas.setMhsBolehUploadUlang(ais.common.GradingHelper.ubahIdPadaCsvBerpagarKoma(
													tugas.getMhsBolehUploadUlang(), id, checkboxConfigAllUpload.isChecked()));
											copy.add(mahasiswa);
										}
									}
									Common.refreshUpdate(session, tugas);
									ListModel strset = new SimpleListModel(copy);
									grid.setModel(strset);
									mahasiswasTemorary = null;
									copy = null;
									tabpanelFileTugasPertemuan.getLinkedTab()
											.setLabel(tugas.getJudultugas());
								}
							});
					}
				});

				// Tab 5 "Rekap Tugas" - selalu dimuat ulang setiap dipilih
				final Div panelRekap = mbt.tambahTab(5, "Rekap Tugas");
				mbt.onSetiapPilih(5, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(panelRekap);
						RekapHasilTugasPerVoPertemuan addWindow = new RekapHasilTugasPerVoPertemuan(true,
								pa.ambilVOPembelajaran());
						addWindow.setClosable(false);
						addWindow.setTitle("");
						addWindow.setHeight("100%");
						addWindow.setWidth("100%");
						panelRekap.appendChild(addWindow);
					}
				});

			}

			// FIX "Only one south child is allowed": pada tampilan dosen/admin (!peserta),
			// South "southTugas" (tombol Simpan/Batal) SUDAH dibuat & di-parent-kan ke
			// myborderlayoutlagi lebih awal (blok if (!peserta) di atas). Borderlayout
			// hanya boleh punya SATU South -- selaras pola getNorth() yang sudah dipakai
			// di bawah (baris ~4087): pakai South yang sudah ada bila sudah dibuat,
			// jangan buat instance South baru lagi.
			South mysouthlagi = myborderlayoutlagi.getSouth() == null ? new South()
					: (South) myborderlayoutlagi.getSouth();
			mysouthlagi.setParent(myborderlayoutlagi);
			ais.ui.util.ZkCompat.setFlex(mysouthlagi, true);

			// South sendiri (LayoutRegion) juga hanya boleh punya SATU child langsung.
			// Jika South ini adalah southTugas yang dipakai ulang (!peserta), child
			// pertamanya sudah berupa Hbox "southBoxTugas" (berisi Toolbar Simpan/Batal).
			// Pakai ulang Hbox tsb agar Paging bisa digabung di dalamnya, bukan
			// di-parent-kan langsung ke South (yang akan memicu "Only one child is allowed").
			Hbox southBoxLagi = (Hbox) mysouthlagi.getFirstChild();
			if (southBoxLagi == null) {
				southBoxLagi = new Hbox();
				southBoxLagi.setParent(mysouthlagi);
				ais.ui.util.ZkCompat.setFlex(southBoxLagi, true);
				southBoxLagi.setAlign("center");
			}

			paging = new Paging();
			paging.setParent(southBoxLagi);
			paging.addEventListener("onPaging", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					reloadTugasFileContent();
				}
			});

			columns = new Columns();

			columns.setParent(uploadTugasGrid);
			MyHboxToolbar hboxPencarian = new MyHboxToolbar();
			hboxPencarian.appendChild(new MyLabelConfig((peserta ? "Peserta Lain" : "Peserta") + " : "));
			cari = new Textbox("");
			cari.setParent(hboxPencarian);
			cari.setCols(10);
			MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-search", "Cari");
			toolbarbutton.setParent(hboxPencarian);

			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					reloadTugasFileContent();
				}
			});

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					reloadTugasFileContent();
				}
			});

			MyToolbarbutton masukAnggapHadirPengumpul = new MyToolbarbutton("fa-check", "Anggap Hadir (Pengumpul Tugas)");
			masukAnggapHadirPengumpul.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && pa != null
					&& pa.getJadwalUjianPMB() == null && pa.getJadwalUjianPSB() == null);
			masukAnggapHadirPengumpul.setTooltiptext("Tandai semua mahasiswa yang MENGUMPULKAN tugas ini sebagai HADIR di kelas");
			masukAnggapHadirPengumpul.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TugasMandiriHelper.uploadTugasDiangapHadir(tugas, pa, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null).display(pa,
									new DataLoader() {

										@Override
										public void loadData(Object value) {
											try {
												reloadTugasFileContent();
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:3055");
											}
										}
									}, 0);
						}
					});

				}
			});
			masukAnggapHadirPengumpul.setParent(vbox);

			MyToolbarbutton masukAnggapHadirPengakses = new MyToolbarbutton("fa-check", "Anggap Hadir (Pengakses Tugas)");
			masukAnggapHadirPengakses.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && pa != null
					&& pa.getJadwalUjianPMB() == null && pa.getJadwalUjianPSB() == null);
			masukAnggapHadirPengakses.setTooltiptext("Tandai semua mahasiswa yang MENGAKSES tugas ini sebagai HADIR di kelas");
			masukAnggapHadirPengakses.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					PertemuanPunyaDiskusiHelper.aksesDianggapHadir(tugas, "tugas",
							"Akses Tugas \"" + tugas.getJudultugas() + "\"", tugas.getMulai(), tugas.getSelesai(),
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null).display(pa,
											new DataLoader() {

												@Override
												public void loadData(Object value) {
													try {
														reloadTugasFileContent();
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:3090");
													}
												}
											}, 0);
								}
							});

				}
			});
			masukAnggapHadirPengakses.setParent(vbox);

			MyToolbarbutton anggapUpload = new MyToolbarbutton("fa-check-square-o", "Anggap Sudah Upload (Semua)");
			anggapUpload.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
					&& tbmuser.getPesertaKursus() == null && pa != null
					&& pa.getJadwalUjianPMB() == null && pa.getJadwalUjianPSB() == null);
			anggapUpload.setTooltiptext(
					"Tandai SEMUA mahasiswa/siswa telah mengumpulkan tugas dengan file KOSONG (hanya yang belum pernah upload)");
			anggapUpload.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TugasMandiriHelper.anggapSemuaSudahUpload(tugas, pa, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTugasFileContent();
						}
					});
				}
			});
			anggapUpload.setParent(vbox);

			column = new MyColumnConfig();
			column.setParent(columns);
			column.appendChild(hboxPencarian);

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Tgl dan waktu");
			column.setWidth(mobile ? "0%" : "15%");

			// Kolom tunggal "Nilai & Keterangan" \u2014 menggantikan kolom CPMK per-kolom.
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nilai & Keterangan");
			column.setWidth(mobile ? "40%" : "30%");

			// Paksa reinit cache saat pertama buka: memastikan entry "Dianggap mengumpulkan"
			// (ID lebih tinggi, dibuat belakangan) ikut termuat meski cold-start setelah restart.
			tugas.belum("tugas_file_content_" + tugas.getClass().getName());
			reloadTugasFileContent();

			MyToolbarbutton buttonRefreshSyarat = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

			buttonRefreshSyarat.setParent(row);

			if (tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
					&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null) {
				Tugas.tampilanSyarat(pa, tugas, null, null, null, null, rows, syaratAlert, buttonRefreshSyarat);
			} else {
				Tugas.tampilanSyaratReadonly(pa, tugas, null, null, null, null, rows, syaratAlert, buttonRefreshSyarat);

				Tugas.tampilanLain(pa, tugas, null, null, null, null, rows, buttonRefreshSyarat);
			}
		}
	}

	/**
	 * Memuat ulang konten grid daftar berkas tugas yang sudah dikumpulkan (tanpa paksa refresh cache).
	 * Alias untuk {@link #reloadTugasFileContent(boolean)} dengan nilai {@code refresh = false}.
	 *
	 * <p><strong>Kapan memakai varian ini.</strong> Varian tanpa argumen dipakai untuk penyegaran
	 * biasa: setelah peserta mengunggah berkas, setelah nilai disimpan lewat toolbar Simpan, setelah
	 * berkas nilai Excel diunggah, setelah halaman paging berpindah, setelah pencarian dijalankan, dan
	 * setelah setiap aksi kehadiran massal selesai. Pada semua kejadian itu cache
	 * {@link TugasFileContent} sudah diinvalidasi sendiri oleh pemanggilnya lewat
	 * {@code tugas.belum("tugas_file_content_" + ...)}, sehingga pembacaan ulang paksa tidak
	 * diperlukan.</p>
	 *
	 * <p>Varian {@link #reloadTugasFileContent(boolean)} dengan argumen {@code true} hanya dipakai di
	 * dua tempat yang memang menuntut pembacaan ulang penuh dari basis data: tombol "Refresh" pada
	 * toolbar Kelola, dan tombol "Batal" yang membuang perubahan nilai di memori.</p>
	 *
	 * <p>Variabel lokal {@code refresh} sengaja ditulis eksplisit alih-alih meneruskan literal
	 * {@code false} agar maksudnya terbaca pada titik pemanggilan.</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat mengambil data dari Hibernate.
	 */
	private void reloadTugasFileContent() throws Exception {
		boolean refresh = false;
		reloadTugasFileContent(refresh);
	}

	/**
	 * Memuat ulang konten {@link #uploadTugasGrid} dengan data terbaru dari
	 * {@link Tugas#ambilTugasFileContentTotal}.
	 *
	 * <p>Metode ini menyetel {@link ListModel} pada grid, menentukan RowRenderer yang sesuai
	 * (OBE vs non-OBE), dan bila pengguna adalah mahasiswa/siswa menampilkan kartu status
	 * pengumpulan di North region. Bila tugas belum memiliki judul (judultugas kosong),
	 * metode ini langsung return tanpa melakukan apapun.</p>
	 *
	 * <h3>Urutan langkah</h3>
	 * <ol>
	 *   <li><strong>Gerbang awal.</strong> Metode langsung kembali bila {@link #uploadTugasGrid} masih
	 *       {@code null} — UI belum sempat dibangun — atau bila judul tugas kosong, yang berarti
	 *       pertemuan ini memang belum memiliki tugas. Gerbang ini membuat metode aman dipanggil dari
	 *       timer maupun dari callback upload kapan saja.</li>
	 *   <li><strong>Penyegaran konteks.</strong> {@link #tbmuser} dibaca ulang dari sesi, lalu
	 *       {@code tugas.currentUser} diisi dan {@code tugas.currentTugasFileContent} dikosongkan.
	 *       Kedua field pada entity itu berperan sebagai kanal keluaran tambahan: entity akan mengisi
	 *       {@code currentTugasFileContent} bila menemukan baris milik pengguna yang sedang login.</li>
	 *   <li><strong>Pemuatan data.</strong> {@link #treemapData} dibuat kosong lalu diteruskan ke
	 *       {@code tugas.ambilTugasFileContentTotal(treemap, cari, paging, 500, refresh)}. Seluruh
	 *       pemanggilan dibungkus {@code try}/{@code catch}: bila satu berkas bermasalah membuat
	 *       pemuatan melempar pengecualian, hasilnya diganti dengan {@link #treemapData} yang sudah
	 *       terisi sebagian, sehingga baris yang sempat termuat tetap ditampilkan alih-alih seluruh
	 *       daftar gagal.</li>
	 *   <li><strong>Pencarian baris milik sendiri.</strong> Bila entity belum mengisi
	 *       {@code currentTugasFileContent}, metode ini mencarinya sendiri dengan menelusuri daftar
	 *       hasil dan mencocokkan id siswa, mahasiswa, atau calon mahasiswa pada {@link #tbmuser}.
	 *       Perhatikan bahwa cabang untuk {@link ais.database.model.sekolah.CalonSiswa} tidak
	 *       disediakan di sini.</li>
	 *   <li><strong>Penentuan mode penilaian.</strong> {@link #obeFormatNilais} dibangun ulang dari
	 *       nol, lalu {@link #jsonObjectTugas} dibaca ulang dari kolom {@code keteranganNilai}.
	 *       Pembacaan ulang ini berarti setiap pemanggilan metode ini <em>membuang</em> perubahan
	 *       nilai yang belum tersimpan ke basis data — perilaku yang justru dimanfaatkan tombol
	 *       "Batal".</li>
	 *   <li><strong>Pemasangan model.</strong> Renderer baru
	 *       ({@link DetailTugasFileContentRenderer}) dan model baru dipasang ke grid.</li>
	 *   <li><strong>Kartu status peserta.</strong> Bila yang melihat adalah peserta, region North pada
	 *       {@link #myborderlayoutlagi} diisi kartu "Tugas yang Anda Upload": berisi baris berkas milik
	 *       sendiri bila sudah mengumpulkan, atau kalimat "Anda belum mengumpulkan ..." bila belum.
	 *       Kedua cabang menutup dengan {@link #tempelTombolUploadUtama(Groupbox)} yang memindahkan
	 *       tombol {@link #upload} ke dalam kartu.</li>
	 *   <li><strong>Pelepasan rujukan.</strong> Peta dan daftar sementara dikosongkan lalu
	 *       di-{@code null}-kan. Pola ini muncul di banyak tempat pada berkas ini sebagai upaya
	 *       menekan jejak memori pada kelas dengan peserta sangat banyak.</li>
	 * </ol>
	 *
	 * <p><strong>Cakupan data.</strong> Batas 500 baris dan kata kunci pada {@link #cari} berlaku di
	 * sini, sehingga daftar yang tampil adalah satu halaman tersaring — bukan seluruh pengumpulan.
	 * Bagian lain yang membutuhkan gambaran penuh memakai {@code tugas.ambilTugasFileContentTotal()}
	 * tanpa argumen.</p>
	 *
	 * <p><strong>Cakupan pengguna.</strong> Pemuatan tidak disaring berdasarkan peran: daftar yang
	 * dikembalikan berisi baris seluruh peserta, baik bagi pengelola maupun bagi peserta. Pembatasan
	 * yang berlaku bagi peserta terjadi di lapisan tampilan, yaitu pada gerbang visibilitas tombol
	 * unduh dan pada cabang sel nilai di
	 * {@link #displayRow(TugasFileContent, List, Component)}.</p>
	 *
	 * @param refresh bila {@code true}, paksa invalidasi cache TugasFileContent sebelum reload.
	 * @throws Exception jika terjadi kesalahan saat mengambil data dari Hibernate.
	 */
	private void reloadTugasFileContent(boolean refresh) throws Exception {

		if (uploadTugasGrid == null || tugas.getJudultugas().isEmpty()) {
			return;
		}
		tbmuser = Common.getCurrentUser();
		tugas.currentTugasFileContent = null;
		tugas.currentUser = tbmuser;
		treemapData = new TreeMap<Long, TugasFileContent>();
		TreeMap<Long, TugasFileContent> tugasFileContentsa;
		try {
			tugasFileContentsa = tugas.ambilTugasFileContentTotal(treemapData,
					cari == null ? "" : cari.getValue().trim(), paging, 500, refresh);
		} catch (Exception eLoad) {
			// ISOLASI: 1 berkas tugas bermasalah (mis. fisik hilang/salah-baris, lihat
			// FileFoto.ambilFile) jangan menggagalkan seluruh reload daftar file tugas
			// mandiri; tampilkan daftar lain yang berhasil dimuat (bisa kosong).
			eLoad.printStackTrace(); ais.common.ErrorAuditUtil.record(eLoad, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:reloadTugasFileContent");
			tugasFileContentsa = treemapData;
		}
		if (tugasFileContentsa == null) {
			tugasFileContentsa = new TreeMap<Long, TugasFileContent>();
		}
		List<TugasFileContent> pertemuanFileContent = new ArrayList<TugasFileContent>(tugasFileContentsa.values());
		Collections.sort(pertemuanFileContent);
		if (tugas.currentTugasFileContent == null && tbmuser != null && tbmuser.getSiswa() != null) {
			for (TugasFileContent tugasFileContent : pertemuanFileContent) {
				if (tugasFileContent.getSiswa() != null
						&& tbmuser.getSiswa().getId().equals(tugasFileContent.getSiswa())) {
					tugas.currentTugasFileContent = tugasFileContent;
					break;
				}
			}
		} else if (tugas.currentTugasFileContent == null && tbmuser != null && tbmuser.getMahasiswa() != null) {
			for (TugasFileContent tugasFileContent : pertemuanFileContent) {
				if (tugasFileContent.getMahasiswa() != null
						&& tbmuser.getMahasiswa().getId().equals(tugasFileContent.getMahasiswa())) {
					tugas.currentTugasFileContent = tugasFileContent;
					break;
				}
			}
		} else if (tugas.currentTugasFileContent == null && tbmuser != null
				&& tbmuser.getBiodataCalonMahasiswa() != null) {
			for (TugasFileContent tugasFileContent : pertemuanFileContent) {
				if (tugasFileContent.getBiodataCalonMahasiswa() != null && tbmuser.getBiodataCalonMahasiswa().getId()
						.equals(tugasFileContent.getBiodataCalonMahasiswa())) {
					tugas.currentTugasFileContent = tugasFileContent;
					break;
				}
			}
		}

		obeFormatNilais = new ArrayList<FormatNilai>();
		if (perkuliahan != null && perkuliahan.getKurikulum() != null
				&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
			Session session = HibernateUtil.currentSession();
			List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
			JSONObject jsonObject = new JSONObject(tugas.getFormatNilais());
			for (FormatNilai nilai : formatNilais) {
				if (nilai.getStatusPertemuan() != null) {
					if (!jsonObject.isNull(nilai.getId().toString())) {
						obeFormatNilais.add(nilai);
					}
				}
			}
		}
		jsonObjectTugas = new JSONObject(tugas.getKeteranganNilai());

		ListModel strset = new SimpleListModel(pertemuanFileContent);
		uploadTugasGrid.setRowRenderer(new DetailTugasFileContentRenderer(obeFormatNilais));
		uploadTugasGrid.setModel(strset);

		if ((mahasiswa != null || biodataCalonMahasiswa != null
				|| (tbmuser != null && tbmuser.getPesertaKursus() != null)
				|| (tbmuser != null && tbmuser.getSiswa() != null)) && tugas.currentTugasFileContent != null) {
			TugasFileContent tugasFileContent = tugas.currentTugasFileContent;
			North north = myborderlayoutlagi.getNorth() == null ? new North() : myborderlayoutlagi.getNorth();
			north.setParent(myborderlayoutlagi);
			if (north != null) {
				Common.clear(north);
			}
			Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
			groupbox.setParent(north);
			groupbox.appendChild(ais.ui.util.DashboardUiKit.html(ais.ui.util.DashboardUiKit.headerModul("tugas",
					"Tugas yang Anda Upload", "Berkas tugas yang sudah Anda kirim beserta tanggal dan nilainya.")));
			Hbox hbox = new Hbox();
			hbox.setParent(groupbox);
			try {
				displayRow(tugasFileContent, obeFormatNilais, hbox);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasMandiriHelper.java:3274");
			}
			north.setAutoscroll(true);
			tempelTombolUploadUtama(groupbox);
		} else if (!tugas.getJudultugas().isEmpty() && (mahasiswa != null || biodataCalonMahasiswa != null
				|| tbmuser.getPesertaKursus() != null || (tbmuser != null && tbmuser.getSiswa() != null) || (tbmuser != null && tbmuser.getCalonSiswa() != null))) {
			North north = myborderlayoutlagi.getNorth() == null ? new North() : myborderlayoutlagi.getNorth();
			north.setParent(myborderlayoutlagi);
			north.setHeight("220px");
			north.setAutoscroll(true);
			if (north != null) {
				Common.clear(north);
			}
			Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
			groupbox.setParent(north);
			groupbox.appendChild(ais.ui.util.DashboardUiKit.html(ais.ui.util.DashboardUiKit.headerModul("tugas",
					"Tugas yang Anda Upload",
					"Status pengiriman tugas Anda. Tekan tombol Upload Tugas untuk mengirim berkas.")));
			groupbox.appendChild(
					new MyLabelBold("Anda belum mengumpulkan / meng-upload \"" + tugas.getJudultugas() + "\""));
			tempelTombolUploadUtama(groupbox);
		}
		tugasFileContentsa.clear();
		tugasFileContentsa = null;
		pertemuanFileContent.clear();
		pertemuanFileContent = null;

	}

	/**
	 * Tombol utama "Upload Tugas" untuk siswa/mahasiswa: ditaruh langsung di kartu status
	 * (tempat siswa membaca statusnya) supaya jelas, besar, mudah disentuh, dan tidak
	 * pernah tertutup toolbar. Memakai ulang tombol {@code upload} yang sudah ada sehingga
	 * aturan tampil (jadwal/boleh upload ulang) dan aksi klik tetap berlaku.
	 *
	 * <p><strong>Memindahkan, bukan menyalin.</strong> Metode ini melakukan
	 * {@code upload.setParent(aksi)} terhadap tombol yang sudah dibangun di
	 * {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)}, sehingga tombol berpindah dari
	 * toolbar ke dalam kartu. Karena instance-nya sama, seluruh aturan visibilitas (jendela waktu
	 * tugas dan izin upload ulang) serta listener {@code ON_CLICK} yang memeriksa
	 * {@link #syaratAlert} tetap berlaku tanpa perlu disalin ulang. Pendekatan ini juga mencegah
	 * munculnya dua tombol Upload yang aturannya bisa menyimpang satu sama lain.</p>
	 *
	 * <p><strong>Idempoten.</strong> Metode dipanggil dari kedua cabang kartu status pada
	 * {@link #reloadTugasFileContent(boolean)} — cabang "sudah mengumpulkan" dan cabang "belum
	 * mengumpulkan" — dan karena setiap pemuatan ulang membangun kartu baru, metode ini akan berjalan
	 * berkali-kali. Pemasangan kelas gaya dijaga oleh pemeriksaan
	 * {@code sc.indexOf("ais-upload-tugas-cta") < 0} agar nama kelas tidak menumpuk berulang pada
	 * atribut {@code sclass}.</p>
	 *
	 * <p><strong>Yang diubah pada tombol.</strong> Orientasi disetel {@code horizontal} agar ikon dan
	 * teks sejajar (bukan bertumpuk seperti pada toolbar), dan dua kelas gaya ditambahkan:
	 * {@code ais-upload-tugas-cta} untuk tampilan tombol ajakan berukuran besar, serta
	 * {@code ais-tbar-icon-white} agar ikonnya kontras di atas latar berwarna. Wadahnya berupa
	 * {@code Div} ber-{@code sclass} {@code ais-upload-cta-wrap}.</p>
	 *
	 * <p>Metode langsung kembali bila {@link #upload} atau {@code groupbox} bernilai {@code null},
	 * sehingga aman dipanggil sebelum toolbar sempat dibangun.</p>
	 *
	 * @param groupbox kartu status pengumpulan tempat tombol akan ditempelkan; boleh {@code null}.
	 */
	private void tempelTombolUploadUtama(Groupbox groupbox) {
		if (upload == null || groupbox == null) {
			return;
		}
		org.zkoss.zul.Div aksi = new org.zkoss.zul.Div();
		aksi.setSclass("ais-upload-cta-wrap");
		aksi.setParent(groupbox);
		upload.setParent(aksi);
		upload.setOrient("horizontal");
		String sc = upload.getSclass();
		sc = (sc == null || sc.trim().length() == 0) ? "" : sc.trim() + " ";
		if (sc.indexOf("ais-upload-tugas-cta") < 0) {
			upload.setSclass(sc + "ais-upload-tugas-cta ais-tbar-icon-white");
		}
	}

	/**
	 * Salinan di memori dari kolom {@code keteranganNilai} milik {@link #tugas} — tempat seluruh nilai
	 * dan keterangan per peserta disimpan.
	 *
	 * <p>Diisi ulang pada setiap {@link #reloadTugasFileContent(boolean)} dengan
	 * {@code new JSONObject(tugas.getKeteranganNilai())}. Objek ini adalah <em>satu-satunya</em>
	 * tempat penilaian tugas mandiri disimpan pada jalur perguruan tinggi dan sekolah: nilai tidak
	 * ditulis ke kolom {@code nilai} milik masing-masing {@link TugasFileContent}, melainkan ke satu
	 * dokumen JSON pada baris tugas.</p>
	 *
	 * <p><strong>Bentuk kunci.</strong> Setiap peserta memperoleh sebuah kunci dasar yang menyatakan
	 * id sekaligus jenisnya, karena {@link TugasFileContent} menyimpan pemilik pada empat kolom
	 * terpisah:</p>
	 * <ul>
	 *   <li>{@code <id>_mhs} bila {@code getMahasiswa()} terisi;</li>
	 *   <li>{@code <id>_siswa} bila {@code getSiswa()} terisi;</li>
	 *   <li>{@code <id>_cal_mhs} bila {@code getBiodataCalonMahasiswa()} terisi;</li>
	 *   <li>{@code <id>_cal_siswa} bila {@code getCalonSiswa()} terisi.</li>
	 * </ul>
	 * <p>Dari kunci dasar itu dibentuk tiga jenis entri: {@code <kunci>_ket} untuk keterangan teks,
	 * {@code <kunci>_nilai} untuk nilai tunggal pada mode non-OBE, dan
	 * {@code <kunci>_nilai_<idFormatNilai>} untuk nilai per Sub-CPMK pada mode OBE. Perhatikan bahwa
	 * kunci dasar dibentuk dari cabang {@code if}/{@code else if} berurutan, sehingga kolom pertama
	 * yang terisi menang — hal ini relevan karena
	 * {@link #anggapSemuaSudahUpload(Tugas, Pertemuan, EventListener)} sengaja mengisi kolom pemilik
	 * yang tidak dipakai dengan bilangan acak negatif agar tidak bentrok.</p>
	 *
	 * <p><strong>Alur baca-tulis.</strong> Nilai dibaca dari objek ini di tiga tempat: ringkasan
	 * read-only pada sel "Nilai &amp; Keterangan", isian awal popup "Edit Nilai", dan berkas Excel
	 * hasil "Download Nilai". Penulisan terjadi pada tombol Simpan di dalam popup "Edit Nilai" —
	 * yang mengubah objek ini <em>dan</em> langsung menyimpannya ke basis data — serta pada tombol
	 * "Simpan" di toolbar bawah dan pada langkah auto-simpan sebelum
	 * {@link #buttonMasukkanNilai} menjalankan {@code GradingHelper}. Tombol "Batal" membuang
	 * perubahan dengan membaca ulang kolom dari basis data dan membangun ulang objek ini.</p>
	 *
	 * <p><strong>Pembersihan karakter NUL.</strong> Saat tombol "Batal" membangun ulang objek ini,
	 * isi kolom dilewatkan {@code replace('\0', ' ')} lebih dahulu. Data lama dapat mengandung byte
	 * NUL yang membuat parser JSON gagal; penggantian dengan spasi membuat pembatalan tetap berhasil
	 * alih-alih melemparkan pengecualian.</p>
	 *
	 * <p><strong>Konsekuensi desain.</strong> Karena seluruh nilai satu kelas berada pada satu kolom
	 * teks di satu baris, penyimpanan bersifat baca-ubah-tulis penuh: dua pengelola yang menilai
	 * peserta berbeda pada saat bersamaan dapat saling menimpa, dan yang menyimpan belakangan menang.
	 * Setiap penulisan karena itu selalu didahului {@code session.refresh(tugas)} untuk memperkecil
	 * jendela tersebut, tetapi tidak menghilangkannya.</p>
	 */
	private JSONObject jsonObjectTugas;
	/**
	 * Daftar Sub-CPMK ({@link FormatNilai}) yang benar-benar dinilai pada tugas ini; kosong bila tugas
	 * tidak memakai penilaian OBE.
	 *
	 * <p>Dibangun ulang pada setiap {@link #reloadTugasFileContent(boolean)} dengan tiga syarat
	 * berlapis:</p>
	 * <ol>
	 *   <li>{@link #perkuliahan} terisi dan kurikulumnya dinyatakan OBE lewat
	 *       {@code getKurikulum().apakahObe(tahunAjaran, ganjilGenap)};</li>
	 *   <li>{@link FormatNilai} yang bersangkutan memiliki {@code getStatusPertemuan()} tidak
	 *       {@code null} — penanda bahwa komponen nilai itu memang diisi dari pertemuan/tugas, bukan
	 *       dari sumber lain;</li>
	 *   <li>id {@link FormatNilai} itu terdaftar sebagai kunci pada dokumen JSON
	 *       {@code tugas.getFormatNilais()}, yaitu Sub-CPMK yang dicentang pengelola pada dialog ubah
	 *       instruksi.</li>
	 * </ol>
	 *
	 * <p><strong>Fungsi sebagai sakelar mode.</strong> Sepanjang berkas ini, pemeriksaan
	 * {@code obeFormatNilais.isEmpty()} dipakai sebagai penentu mode penilaian, bukan pemeriksaan
	 * kurikulum secara langsung. Bila kosong, tampilan memakai satu nilai tunggal dengan kunci
	 * {@code <kunci>_nilai}; bila terisi, tampilan memakai satu nilai untuk setiap anggota daftar
	 * dengan kunci {@code <kunci>_nilai_<idFormatNilai>}. Titik yang memakainya: ringkasan sel nilai,
	 * popup "Edit Nilai", kolom-kolom berkas Excel pada "Download Nilai" dan "Upload Nilai", serta
	 * tampilan read-only nilai milik peserta sendiri.</p>
	 *
	 * <p><strong>Urutan penting.</strong> Berkas Excel nilai memakai posisi kolom, bukan nama, untuk
	 * memetakan nilai kembali ke Sub-CPMK: kolom {@code 2 + i} berpasangan dengan
	 * {@code obeFormatNilais.get(i)}, dan kolom keterangan berada di
	 * {@code 2 + obeFormatNilais.size()}. Karena itu urutan daftar ini harus tetap sama antara saat
	 * berkas diunduh dan saat diunggah kembali. Bila pengelola mengubah pilihan Sub-CPMK di antara
	 * kedua langkah tersebut, nilai akan tertulis ke Sub-CPMK yang salah — berkas hasil unduhan lama
	 * tidak boleh dipakai setelah pilihan Sub-CPMK berubah.</p>
	 *
	 * <p><strong>Sub-CPMK per peserta.</strong> Daftar ini menyatakan Sub-CPMK pada tingkat tugas.
	 * Untuk {@link TugasPertemuan}, tab "Peserta yg tdk perlu ikt" masih dapat mempersempitnya per
	 * peserta lewat {@code subCpmkPerPeserta} dan memberi nilai manual lewat {@code nilaiManualJson}
	 * — dua kolom yang berdiri sendiri dan tidak tercermin pada field ini.</p>
	 */
	private List<FormatNilai> obeFormatNilais = new ArrayList<FormatNilai>();

	// Foto profil dikunci min-height:70px (inline, di ProfileImageUtil) sehingga baris kartu
	// "Telah upload" jadi tinggi & jaraknya tampak melebar. Untuk kartu ini foto dikecilkan
	// ~46px supaya baris rapat (CSS tetap menjaga isi menumpuk di atas).
	/**
	 * Membuat tautan foto profil peserta berukuran kecil yang khusus disesuaikan untuk kartu baris
	 * pada daftar "Telah upload".
	 *
	 * <p>Metode ini membungkus {@code CommonMedia.tampilkanGambarKecil(obj)} — pabrik foto profil
	 * umum yang dipakai di seluruh aplikasi — lalu menimpa gayanya. Pembungkusan diperlukan karena
	 * {@code ProfileImageUtil} mengunci tinggi minimum gambar profil pada {@code 70px} lewat gaya
	 * sebaris. Nilai itu cocok untuk halaman profil, tetapi pada daftar pengumpulan tugas ia membuat
	 * setiap baris menjadi tinggi dan jarak antar-baris tampak melebar sehingga hanya sedikit peserta
	 * yang terlihat sekaligus.</p>
	 *
	 * <p><strong>Penyesuaian yang dilakukan.</strong> Bila anak pertama dari tautan yang dihasilkan
	 * benar-benar sebuah {@code org.zkoss.zul.Image}, gambar itu diberi tinggi {@code 46px}, kelas
	 * gaya {@code ais-tugas-avatar-img}, dan sekumpulan properti sebaris ber-{@code !important}:
	 * lebar dan tinggi tetap {@code 46px}, lebar minimum {@code 36px}, tinggi minimum dinolkan
	 * (inilah yang membatalkan kunci {@code 70px}), {@code object-fit:cover} agar foto tidak gepeng,
	 * sudut membulat, bayangan tipis, dan bingkai putih. Penanda {@code !important} diperlukan karena
	 * gaya yang ditimpa juga ditulis sebaris oleh pembuat aslinya.</p>
	 *
	 * <p><strong>Sifat toleran terhadap kegagalan.</strong> Seluruh penyesuaian dibungkus
	 * {@code try}/{@code catch} yang hanya mencatat pengecualian ke {@code ErrorAuditUtil} lalu
	 * melanjutkan. Sikap ini disengaja: kegagalan mempercantik foto tidak boleh membuat satu baris
	 * daftar pengumpulan gagal dirender. Bila anak pertama ternyata bukan {@code Image} — misalnya
	 * peserta tidak memiliki foto sehingga yang dihasilkan berupa placeholder jenis lain — penyesuaian
	 * dilewati diam-diam dan tautan dikembalikan apa adanya.</p>
	 *
	 * <p><strong>Argumen bertipe {@code GeneralValueObject}.</strong> Tipe ini adalah induk bersama
	 * dari {@link Mahasiswa}, {@link Siswa}, {@link BiodataCalonMahasiswa}, dan {@link CalonSiswa},
	 * sehingga satu metode dapat melayani keempat jenis peserta yang mungkin memiliki berkas
	 * pengumpulan. Pemanggilnya adalah tiga cabang identitas di dalam
	 * {@link #displayRow(TugasFileContent, List, Component)}.</p>
	 *
	 * <p>Perhatikan bahwa metode ini tidak menyentuh basis data secara langsung; pengambilan berkas
	 * foto sepenuhnya menjadi tanggung jawab {@code CommonMedia}. Deklarasi {@code throws Exception}
	 * berasal dari pemanggilan itu, bukan dari penyesuaian gaya di sini.</p>
	 *
	 * @param obj entitas peserta pemilik foto ({@link Mahasiswa}, {@link Siswa},
	 *            {@link BiodataCalonMahasiswa}, atau {@link CalonSiswa}); boleh {@code null} sejauh
	 *            {@code CommonMedia.tampilkanGambarKecil} menoleransinya.
	 * @return komponen tautan {@code org.zkoss.zul.A} berisi foto profil yang sudah diperkecil, siap
	 *         di-{@code setParent} ke kartu baris.
	 * @throws Exception bila {@code CommonMedia.tampilkanGambarKecil} gagal membangun komponen foto.
	 */
	private static org.zkoss.zul.A fotoKartuUpload(ais.database.model.GeneralValueObject obj) throws Exception {
		org.zkoss.zul.A a = CommonMedia.tampilkanGambarKecil(obj);
		try {
			if (a != null && a.getFirstChild() instanceof org.zkoss.zul.Image) {
				org.zkoss.zul.Image img = (org.zkoss.zul.Image) a.getFirstChild();
				img.setHeight("46px");
				img.setSclass("ais-tugas-avatar-img");
				img.setStyle("width:46px !important;height:46px !important;max-width:46px !important;"
						+ "min-width:36px !important;min-height:0 !important;object-fit:cover !important;"
						+ "border-radius:12px !important;box-shadow:0 1px 4px rgba(0,0,0,.18) !important;"
						+ "border:2px solid #fff !important;");
			}
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:3342");
		}
		return a;
	}

	/**
	 * Rapikan baris kontak (HP/Email) hasil {@code tampilkanHp()}/{@code tampilkanEmail()}.
	 *
	 * <p>Metode {@code tampilkanHp}/{@code tampilkanEmail} pada entitas SELALU membuat sebuah tombol
	 * meski nilai HP/Email kosong. Karena CSS {@code .ais-tugas-contact-row} menata setiap tombol
	 * menjadi "pil", tombol kosong ikut tampil sebagai <b>pil abu-abu kosong</b> yang mengotori baris
	 * kontak (lihat peserta tanpa email pada layar Tugas). Method ini membuang chip kosong
	 * (Toolbarbutton/A/Label tanpa teks); bila SEMUA kontak kosong, barisnya disembunyikan agar tidak
	 * menyisakan ruang kosong.</p>
	 *
	 * <p><strong>Cara kerja.</strong> Daftar anak disalin lebih dahulu ke {@link ArrayList} baru
	 * sebelum ditelusuri. Penyalinan itu wajib: {@code detach()} mengubah daftar anak milik komponen
	 * induk, dan menelusuri daftar yang sedang diubah akan memicu
	 * {@code ConcurrentModificationException}. Setiap anak diperiksa menurut tipenya —
	 * {@link Toolbarbutton} dan {@code org.zkoss.zul.A} lewat {@code getLabel()}, {@link Label} lewat
	 * {@code getValue()} — dan dilepas bila teksnya {@code null} atau hanya berisi spasi. Tipe lain
	 * dibiarkan karena tidak dapat dinilai kosong-tidaknya.</p>
	 *
	 * <p><strong>Baris kosong disembunyikan seluruhnya.</strong> Bila setelah pembersihan tidak ada
	 * anak yang tersisa dan wadahnya merupakan {@code HtmlBasedComponent}, wadah itu sendiri
	 * di-{@code setVisible(false)}. Tanpa langkah ini, wadah kosong tetap menyisakan ruang vertikal
	 * karena kelas gaya {@code ais-tugas-contact-row} memberinya padding dan jarak antar-chip.</p>
	 *
	 * <p><strong>Toleran terhadap kegagalan.</strong> Seluruh proses dibungkus {@code try}/{@code catch}
	 * yang hanya menampilkan galat kepada admin lewat {@code Common.tampilErrorJikaAdmin}. Perapian
	 * tampilan tidak boleh menggagalkan render satu baris daftar pengumpulan. Argumen {@code null}
	 * langsung diabaikan.</p>
	 *
	 * @param barisKontak kontainer chip kontak (Div) yang akan dirapikan.
	 */
	private static void rapikanBarisKontak(Component barisKontak) {
		if (barisKontak == null) {
			return;
		}
		try {
			java.util.List<Component> anak = new java.util.ArrayList<Component>(barisKontak.getChildren());
			for (Component c : anak) {
				String teks = null;
				if (c instanceof Toolbarbutton) {
					teks = ((Toolbarbutton) c).getLabel();
				} else if (c instanceof org.zkoss.zul.A) {
					teks = ((org.zkoss.zul.A) c).getLabel();
				} else if (c instanceof Label) {
					teks = ((Label) c).getValue();
				}
				if (teks == null || teks.trim().isEmpty()) {
					c.detach();
				}
			}
			if (barisKontak.getChildren().isEmpty() && barisKontak instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
				((org.zkoss.zk.ui.HtmlBasedComponent) barisKontak).setVisible(false);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun seluruh isi satu baris pengumpulan tugas: identitas peserta, waktu pengumpulan, sel
	 * nilai &amp; keterangan, dan tombol unduh berkas.
	 *
	 * <p>Metode ini adalah inti tampilan daftar "Telah upload". Ia dipanggil dari dua tempat dengan
	 * wadah yang berbeda, dan sengaja menerima {@link Component} generik alih-alih {@link Row} agar
	 * keduanya dapat dilayani:</p>
	 * <ul>
	 *   <li>dari {@link DetailTugasFileContentRenderer#render(Row, Object)}, dengan {@code arg0}
	 *       berupa {@link Row} — setiap komponen yang di-{@code setParent} ke sana menjadi satu sel;</li>
	 *   <li>dari {@link #reloadTugasFileContent(boolean)}, dengan {@code arg0} berupa {@code Hbox} di
	 *       dalam kartu "Tugas yang Anda Upload" milik peserta — di sini komponen yang sama tersusun
	 *       mendatar, bukan sebagai sel tabel.</li>
	 * </ul>
	 *
	 * <h3>Bagian 1 — identitas peserta</h3>
	 * <p>{@link TugasFileContent} menyimpan pemilik berkas pada empat kolom id terpisah
	 * ({@code mahasiswa}, {@code siswa}, {@code biodataCalonMahasiswa}, {@code calonSiswa}), sehingga
	 * langkah pertama adalah memulihkan entitasnya lewat
	 * {@code ConstantValues.ambil(NamaKelas, id)}. Cabang yang dipakai berurutan: siswa lebih dahulu,
	 * lalu mahasiswa, dan cabang terakhir menganggap pemiliknya calon mahasiswa. Cabang terakhir itu
	 * bersifat penampung sisa — ia tidak menguji {@link CalonSiswa}, sehingga berkas milik calon siswa
	 * akan jatuh ke sana dan menghasilkan entitas {@code null} yang ditangani dengan label kosong.</p>
	 *
	 * <p>Setiap cabang melakukan tiga hal: memanggil
	 * {@code tugasFileContent.ubahRealNameSesuaiDenganNIM(entitas)} agar nama berkas yang tersimpan
	 * diberi awalan NIM/nomor induk pemiliknya, mengambil nama tampilan berkas lewat
	 * {@code ambilRealNameSesuaiDenganNIM(entitas)}, lalu menyusun kartu berisi foto profil hasil
	 * {@link #fotoKartuUpload(ais.database.model.GeneralValueObject)} berdampingan dengan label
	 * "nomor induk / nama".</p>
	 *
	 * <p><strong>Baris kontak.</strong> Di bawah label nama dapat muncul baris chip berisi nomor HP
	 * dan alamat surel peserta, hasil {@code tampilkanHp()} dan {@code tampilkanEmail()} milik
	 * entitas, yang kemudian dibersihkan dari chip kosong oleh
	 * {@link #rapikanBarisKontak(Component)}. Perhatikan bahwa syarat kemunculannya tidak seragam:
	 * cabang mahasiswa memasangnya hanya bila field {@link #mahasiswa} bernilai {@code null},
	 * sedangkan cabang siswa dan cabang calon mahasiswa memasangnya tanpa syarat. Karena
	 * {@link #mahasiswa} berasal dari argumen konstruktor dan bukan dari peran pengguna yang login,
	 * syarat itu tidak setara dengan "yang melihat adalah pengelola".</p>
	 *
	 * <h3>Bagian 2 — waktu pengumpulan</h3>
	 * <p>Sebuah {@link Label} berisi {@code uploadDate} yang diformat dengan nama hari dari
	 * {@code SmartDateTimeUtil.getDayString} ditambah format tanggal standar. Bila {@code uploadDate}
	 * kosong, label dibiarkan kosong sehingga kolom tetap sejajar.</p>
	 *
	 * <h3>Bagian 3 — sel "Nilai &amp; Keterangan"</h3>
	 * <p>Isi sel ini ditentukan tiga cabang yang saling eksklusif.</p>
	 * <ol>
	 *   <li><strong>Peserta ditandai tidak perlu ikut.</strong> Bila id pemilik berkas tercantum pada
	 *       {@code tugas.getMhsYgTidakIkut()}, sel hanya berisi keterangan merah bahwa yang
	 *       bersangkutan tidak perlu mengumpulkan dan tidak perlu dinilai. Cabang ini sengaja berlaku
	 *       juga ketika penandaan dilakukan <em>setelah</em> peserta terlanjur mengunggah berkas.</li>
	 *   <li><strong>Pengelola ({@code !peserta}).</strong> Sel berisi ringkasan read-only —
	 *       baris keterangan bila ada, lalu satu baris nilai untuk setiap Sub-CPMK pada mode OBE atau
	 *       satu baris "Nilai" pada mode non-OBE — ditambah tombol "Edit Nilai". Tombol itu membuka
	 *       jendela modal berisi kotak keterangan dan satu kotak nilai per komponen. Menekan Simpan di
	 *       dalam popup menulis nilai ke {@link #jsonObjectTugas}, lalu <em>langsung</em> menyimpannya
	 *       ke basis data: {@code session.refresh(tugas)}, invalidasi cache berkas, penulisan
	 *       {@code setKeteranganNilai(jsonObjectTugas.toString())}, dan {@code Common.refreshUpdate}.
	 *       Setelah tersimpan, ringkasan di baris dibangun ulang di tempat tanpa memuat ulang seluruh
	 *       grid. Bila penyimpanan gagal, popup tetap terbuka dan menampilkan pesan merah.</li>
	 *   <li><strong>Peserta.</strong> Sel hanya menampilkan nilai sebagai teks read-only, dan hanya
	 *       bila baris ini memang milik mahasiswa yang sedang login. Untuk baris milik peserta lain
	 *       sel dibiarkan kosong.</li>
	 * </ol>
	 *
	 * <p><strong>Sumber angka nilai.</strong> Pada mode non-OBE, nilai dibaca dari kunci
	 * {@code <kunci>_nilai} pada {@link #jsonObjectTugas} dan jatuh kembali ke kolom
	 * {@code tugasFileContent.getNilai()} bila kunci itu belum ada — jalur mundur bagi data lama yang
	 * dinilai sebelum penyimpanan berbasis JSON diberlakukan. Pola yang sama berlaku untuk keterangan,
	 * yang jatuh kembali ke {@code tugasFileContent.getKeterangan()}. Pada mode OBE tidak ada jalur
	 * mundur: nilai yang tidak ditemukan di JSON dianggap {@code 0.0}.</p>
	 *
	 * <h3>Bagian 4 — tombol unduh berkas</h3>
	 * <p>Tombol bergambar ikon sesuai jenis berkas, berlabel nama berkas. Visibilitasnya adalah
	 * satu-satunya gerbang berbasis peran yang sepenuhnya fail-closed di metode ini: tombol tampil
	 * bila {@link #tbmuser} bukan pelajar sama sekali, ATAU bila salah satu identitas pelajar pada
	 * {@link #tbmuser} cocok persis dengan kolom pemilik yang bersesuaian pada baris ini. Dengan
	 * begitu seorang peserta tidak dapat mengunduh berkas peserta lain walaupun barisnya terlihat di
	 * grid.</p>
	 *
	 * <p><strong>Perilaku klik.</strong> Tiga jalur, diperiksa berurutan: berkas yang sudah dipindah
	 * ke Google Drive ditampilkan lewat {@code tampilGDrive}; berkas berupa tautan luar dipakai apa
	 * adanya bila diawali {@code http}; selain itu tautan internal dibangun lewat
	 * {@code createLinkUri()}. Berkas yang dapat dipratinjau dibuka di jendela ZK
	 * ({@code Common.displayWindow}), sedangkan sisanya diarahkan ke tab peramban baru. Bila tidak ada
	 * tautan yang dapat dibentuk, ditampilkan pesan bahwa berkas tidak ditemukan.</p>
	 *
	 * <p><strong>Catatan tentang {@code hbox.setVisible(...)}.</strong> Wadah tombol unduh diberi
	 * syarat {@code mahasiswa == null || mahasiswa.getId().equals(tugasFileContent.getMahasiswa())}.
	 * Variabel {@code mahasiswa} di titik itu adalah entitas <em>pemilik baris</em> yang baru saja
	 * dipulihkan dari {@code tugasFileContent.getMahasiswa()}, bukan pengguna yang login — sehingga
	 * ruas kedua selalu benar dan syarat itu efektif selalu terpenuhi. Gerbang yang benar-benar
	 * bekerja adalah {@code setVisible} pada tombol unduh itu sendiri.</p>
	 *
	 * <p><strong>Efek samping yang perlu diketahui.</strong> Metode ini bukan murni presentasi:
	 * {@code ubahRealNameSesuaiDenganNIM} berpotensi mengubah state entitas berkas, dan tombol Simpan
	 * pada popup menulis ke basis data. Karena itu metode ini harus dijalankan pada event thread ZK
	 * dengan sesi Hibernate dan konteks pengguna yang aktif.</p>
	 *
	 * @param tugasFileContent baris pengumpulan yang akan digambarkan.
	 * @param obeFormatNilais  daftar Sub-CPMK yang berlaku; diterima sebagai argumen namun untuk
	 *                         penentuan mode metode ini membaca {@link #obeFormatNilais} milik
	 *                         instance, bukan argumen ini.
	 * @param arg0             wadah tujuan — {@link Row} pada grid, atau {@code Hbox} pada kartu
	 *                         status peserta.
	 * @throws Exception bila pemulihan entitas peserta, pembangunan komponen, atau pembacaan berkas
	 *                   gagal.
	 */
	private void displayRow(final TugasFileContent tugasFileContent, List<FormatNilai> obeFormatNilais, Component arg0)
			throws Exception {

		Mahasiswa mahasiswa = (Mahasiswa) (tugasFileContent.getMahasiswa() == null ? null
				: ConstantValues.ambil(Mahasiswa.class.getName(), tugasFileContent.getMahasiswa()));
		Siswa siswa = (Siswa) (tugasFileContent.getSiswa() == null ? null
				: ConstantValues.ambil(Siswa.class.getName(), tugasFileContent.getSiswa()));

		String namaFile = tugasFileContent.getNama();
		Vbox a = new Vbox();
		if (siswa != null) {
			Hbox ahbox = new Hbox();
			ahbox.setStyle("gap:6px;align-items:flex-start;");
			tugasFileContent.ubahRealNameSesuaiDenganNIM(siswa);

			namaFile = tugasFileContent.ambilRealNameSesuaiDenganNIM(siswa);

			ahbox.setParent(arg0);
			fotoKartuUpload(siswa).setParent(ahbox);

			a.setParent(ahbox);
			Label namaLblS = new Label(siswa.getNomorInduk() + " / " + siswa.getNama());
			namaLblS.setSclass("ais-tugas-upload-nama");
			namaLblS.setParent(a);
			org.zkoss.zul.Div contactRowS = new org.zkoss.zul.Div();
			contactRowS.setSclass("ais-tugas-contact-row");
			contactRowS.setParent(a);
			siswa.tampilkanHp(contactRowS);
			siswa.tampilkanEmail(contactRowS);
			rapikanBarisKontak(contactRowS);

		} else if (mahasiswa != null) {
			Hbox ahbox = new Hbox();
			ahbox.setStyle("gap:6px;align-items:flex-start;");
			tugasFileContent.ubahRealNameSesuaiDenganNIM(mahasiswa);

			namaFile = tugasFileContent.ambilRealNameSesuaiDenganNIM(mahasiswa);

			ahbox.setParent(arg0);
			fotoKartuUpload(mahasiswa).setParent(ahbox);

			a.setParent(ahbox);
			Label namaLblM = new Label(mahasiswa.getNim() + " / " + mahasiswa.getNama());
			namaLblM.setSclass("ais-tugas-upload-nama");
			namaLblM.setParent(a);

			if (TugasMandiriHelper.this.mahasiswa == null) {
				org.zkoss.zul.Div contactRowM = new org.zkoss.zul.Div();
				contactRowM.setSclass("ais-tugas-contact-row");
				contactRowM.setParent(a);
				mahasiswa.tampilkanHp(contactRowM);
				mahasiswa.tampilkanEmail(contactRowM);
				rapikanBarisKontak(contactRowM);
			}
		} else {
			BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
					.ambil(BiodataCalonMahasiswa.class.getName(), tugasFileContent.getBiodataCalonMahasiswa());

			namaFile = tugasFileContent.ambilRealNameSesuaiDenganNIM(biodataCalonMahasiswa);
			tugasFileContent.ubahRealNameSesuaiDenganNIM(biodataCalonMahasiswa);

			Hbox ahbox = new Hbox();
			ahbox.setStyle("gap:6px;align-items:flex-start;");
			ahbox.setParent(arg0);
			fotoKartuUpload(biodataCalonMahasiswa).setParent(ahbox);

			a = new Vbox();
			a.setParent(ahbox);
			Label namaLblC = new Label((biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNoRegistrasi())
					+ (biodataCalonMahasiswa == null || biodataCalonMahasiswa.getNama() == null ? ""
							: " / " + biodataCalonMahasiswa.getNama()));
			namaLblC.setSclass("ais-tugas-upload-nama");
			namaLblC.setParent(a);
			if (biodataCalonMahasiswa != null) {
				org.zkoss.zul.Div contactRowC = new org.zkoss.zul.Div();
				contactRowC.setSclass("ais-tugas-contact-row");
				contactRowC.setParent(a);
				biodataCalonMahasiswa.tampilkanHp(contactRowC);
				biodataCalonMahasiswa.tampilkanEmail(contactRowC);
				rapikanBarisKontak(contactRowC);
			}
		}

		new Label(tugasFileContent.getUploadDate() == null ? ""
				: SmartDateTimeUtil.getDayString(tugasFileContent.getUploadDate(), null)
						+ Common.dateFormat.get().format(tugasFileContent.getUploadDate()))
				.setParent(arg0);
		// "Tidak perlu ikut tugas": bila mahasiswa PEMILIK file ini ditandai tidak perlu ikut
		// (tugas.mhsYgTidakIkut), JANGAN tampilkan isian keterangan & input nilai — cukup
		// informasi bahwa ybs tidak perlu mengumpulkan/upload tugas ini (juga berlaku bila
		// dicentang SETELAH mahasiswa terlanjur upload).
		final Long idPemilikTfc = tugasFileContent.getMahasiswa() != null ? tugasFileContent.getMahasiswa()
				: tugasFileContent.getSiswa() != null ? tugasFileContent.getSiswa()
						: tugasFileContent.getBiodataCalonMahasiswa() != null ? tugasFileContent.getBiodataCalonMahasiswa()
								: tugasFileContent.getCalonSiswa();
		final boolean tidakPerluIkutTfc = idPemilikTfc != null && tugas != null && tugas.getMhsYgTidakIkut() != null
				&& tugas.getMhsYgTidakIkut().contains("," + idPemilikTfc + ",");

		// === Kolom tunggal: ringkasan nilai + tombol Edit popup ===
		final String namaPopup = namaFile;
		final String keyTfc;
		if (tugasFileContent.getMahasiswa() != null) {
			keyTfc = tugasFileContent.getMahasiswa() + "_mhs";
		} else if (tugasFileContent.getSiswa() != null) {
			keyTfc = tugasFileContent.getSiswa() + "_siswa";
		} else if (tugasFileContent.getBiodataCalonMahasiswa() != null) {
			keyTfc = tugasFileContent.getBiodataCalonMahasiswa() + "_cal_mhs";
		} else if (tugasFileContent.getCalonSiswa() != null) {
			keyTfc = tugasFileContent.getCalonSiswa() + "_cal_siswa";
		} else {
			keyTfc = "";
		}

		final org.zkoss.zul.Vbox nilaiKetCell = new org.zkoss.zul.Vbox();
		nilaiKetCell.setStyle("width:100%;gap:2px;");
		nilaiKetCell.setParent(arg0);

		if (tidakPerluIkutTfc) {
			Label infoTidakIkutTfc = new Label(
					ais.common.Common.getBahasaConfig("Mahasiswa ini tidak perlu mengumpulkan tugas ini, sehingga tidak perlu dinilai."));
			infoTidakIkutTfc.setStyle("color:#b91c1c;font-weight:bold;");
			infoTidakIkutTfc.setParent(nilaiKetCell);
		} else if (!peserta) {
			// --- Ringkasan nilai (read-only, diperbarui setelah Simpan di popup) ---
			final org.zkoss.zul.Vbox summaryVbox = new org.zkoss.zul.Vbox();
			summaryVbox.setStyle("gap:1px;");
			summaryVbox.setParent(nilaiKetCell);

			String ketSummary = (!keyTfc.isEmpty() && !TugasMandiriHelper.this.jsonObjectTugas.isNull(keyTfc + "_ket"))
					? TugasMandiriHelper.this.jsonObjectTugas.getString(keyTfc + "_ket")
					: (tugasFileContent.getKeterangan() == null ? "" : tugasFileContent.getKeterangan());
			if (ketSummary != null && !ketSummary.trim().isEmpty()) {
				Label lblKetS = new Label("Ket: " + ketSummary);
				lblKetS.setStyle("color:#555;font-size:11px;white-space:normal;");
				lblKetS.setParent(summaryVbox);
			}
			if (!TugasMandiriHelper.this.obeFormatNilais.isEmpty()) {
				for (FormatNilai fnS : TugasMandiriHelper.this.obeFormatNilais) {
					double nS = (!keyTfc.isEmpty() && !TugasMandiriHelper.this.jsonObjectTugas.isNull(keyTfc + "_nilai_" + fnS.getId()))
							? TugasMandiriHelper.this.jsonObjectTugas.getDouble(keyTfc + "_nilai_" + fnS.getId()) : 0.0;
					String fnNamaS = fnS.getNama() != null ? fnS.getNama() : ("CPMK " + fnS.getId());
					Label lblNS = new Label(fnNamaS + ": " + (nS > 0.0 ? Common.numberFormat.get().format(nS) : "-"));
					lblNS.setStyle("font-size:11px;");
					lblNS.setParent(summaryVbox);
				}
			} else {
				double nS = (!keyTfc.isEmpty() && !TugasMandiriHelper.this.jsonObjectTugas.isNull(keyTfc + "_nilai"))
						? TugasMandiriHelper.this.jsonObjectTugas.getDouble(keyTfc + "_nilai") : tugasFileContent.getNilai();
				Label lblNS = new Label("Nilai: " + (nS > 0.0 ? Common.numberFormat.get().format(nS) : "-"));
				lblNS.setStyle("font-size:11px;");
				lblNS.setParent(summaryVbox);
			}

			// --- Tombol Edit: buka popup entry nilai ---
			MyToolbarbutton btnEditNilai = new MyToolbarbutton("fa-pencil", "Edit Nilai");
			btnEditNilai.setStyle("margin-top:2px;");
			btnEditNilai.setParent(nilaiKetCell);
			btnEditNilai.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event evEdit) throws Exception {
					final org.zkoss.zul.Window popupW = new org.zkoss.zul.Window();
					popupW.setTitle("Entry Nilai \u2014 " + namaPopup);
					popupW.setWidth("480px");
					popupW.setClosable(true);
					popupW.setBorder("normal");
					popupW.setPage(evEdit.getTarget().getPage());

					org.zkoss.zul.Vbox popupContent = new org.zkoss.zul.Vbox();
					popupContent.setStyle("padding:8px;gap:6px;width:100%;");
					popupContent.setParent(popupW);

					Label lblKetTitle = new Label("Keterangan:");
					lblKetTitle.setStyle("font-weight:bold;");
					lblKetTitle.setParent(popupContent);
					String ketCurrent = (!keyTfc.isEmpty() && !TugasMandiriHelper.this.jsonObjectTugas.isNull(keyTfc + "_ket"))
							? TugasMandiriHelper.this.jsonObjectTugas.getString(keyTfc + "_ket")
							: (tugasFileContent.getKeterangan() == null ? "" : tugasFileContent.getKeterangan());
					final Textbox popupKet = new Textbox(ketCurrent);
					popupKet.setRows(2);
					popupKet.setWidth("100%");
					popupKet.setMaxlength(255);
					popupKet.setParent(popupContent);

					Label lblNilaiTitle = new Label("Nilai:");
					lblNilaiTitle.setStyle("font-weight:bold;margin-top:4px;");
					lblNilaiTitle.setParent(popupContent);

					final java.util.ArrayList<MyDoublebox> popupNilaiBoxes = new java.util.ArrayList<MyDoublebox>();
					if (!TugasMandiriHelper.this.obeFormatNilais.isEmpty()) {
						for (FormatNilai fnP : TugasMandiriHelper.this.obeFormatNilais) {
							Hbox rowFnP = new Hbox();
							rowFnP.setStyle("align-items:center;gap:6px;");
							rowFnP.setParent(popupContent);
							String fnNamaP = fnP.getNama() != null ? fnP.getNama() : ("CPMK " + fnP.getId());
							Label lblFnP = new Label(fnNamaP + ":");
							lblFnP.setStyle("min-width:120px;font-size:12px;");
							lblFnP.setParent(rowFnP);
							double nP = (!keyTfc.isEmpty() && !TugasMandiriHelper.this.jsonObjectTugas.isNull(keyTfc + "_nilai_" + fnP.getId()))
									? TugasMandiriHelper.this.jsonObjectTugas.getDouble(keyTfc + "_nilai_" + fnP.getId()) : 0.0;
							MyDoublebox nilaiBoxP = new MyDoublebox(nP);
							ais.ui.util.UIUtil.gayaInputNilai(nilaiBoxP);
							nilaiBoxP.setParent(rowFnP);
							popupNilaiBoxes.add(nilaiBoxP);
						}
					} else {
						double nP = (!keyTfc.isEmpty() && !TugasMandiriHelper.this.jsonObjectTugas.isNull(keyTfc + "_nilai"))
								? TugasMandiriHelper.this.jsonObjectTugas.getDouble(keyTfc + "_nilai") : tugasFileContent.getNilai();
						MyDoublebox nilaiBoxP = new MyDoublebox(nP);
						ais.ui.util.UIUtil.gayaInputNilai(nilaiBoxP);
						nilaiBoxP.setParent(popupContent);
						popupNilaiBoxes.add(nilaiBoxP);
					}

					Hbox btnRowP = new Hbox();
					btnRowP.setStyle("margin-top:8px;justify-content:flex-end;gap:8px;");
					btnRowP.setParent(popupContent);
					final org.zkoss.zul.Label lblSimpanStatus = new org.zkoss.zul.Label("");
					lblSimpanStatus.setParent(btnRowP);
					org.zkoss.zul.Button btnSimpan = new org.zkoss.zul.Button("Simpan");
					btnSimpan.setSclass("btn btn-primary btn-sm");
					btnSimpan.setParent(btnRowP);
					btnSimpan.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event evSimpan) throws Exception {
							String ketNew = popupKet.getValue() == null ? "" : popupKet.getValue().trim();
							if (!keyTfc.isEmpty()) {
								TugasMandiriHelper.this.jsonObjectTugas.put(keyTfc + "_ket", ketNew);
							}
							if (!TugasMandiriHelper.this.obeFormatNilais.isEmpty()) {
								for (int idxP = 0; idxP < TugasMandiriHelper.this.obeFormatNilais.size(); idxP++) {
									FormatNilai fnSv = TugasMandiriHelper.this.obeFormatNilais.get(idxP);
									Double nvSv = popupNilaiBoxes.get(idxP).getValue();
									if (!keyTfc.isEmpty()) {
										TugasMandiriHelper.this.jsonObjectTugas.put(keyTfc + "_nilai_" + fnSv.getId(), nvSv);
									}
								}
							} else if (!popupNilaiBoxes.isEmpty()) {
								Double nvSv = popupNilaiBoxes.get(0).getValue();
								if (!keyTfc.isEmpty()) {
									TugasMandiriHelper.this.jsonObjectTugas.put(keyTfc + "_nilai", nvSv);
								}
							}
							try {
								org.hibernate.Session sSave = HibernateUtil.currentSession();
								sSave.refresh(TugasMandiriHelper.this.tugas);
								TugasMandiriHelper.this.tugas.belum("tugas_file_content_" + TugasMandiriHelper.this.tugas.getClass().getName());
								TugasMandiriHelper.this.tugas.setKeteranganNilai(TugasMandiriHelper.this.jsonObjectTugas.toString());
								Common.refreshUpdate(sSave, TugasMandiriHelper.this.tugas);
								while (summaryVbox.getFirstChild() != null) {
									summaryVbox.getFirstChild().detach();
								}
								if (ketNew != null && !ketNew.isEmpty()) {
									Label lblKetUpd = new Label("Ket: " + ketNew);
									lblKetUpd.setStyle("color:#555;font-size:11px;white-space:normal;");
									lblKetUpd.setParent(summaryVbox);
								}
								if (!TugasMandiriHelper.this.obeFormatNilais.isEmpty()) {
									for (int idxU = 0; idxU < TugasMandiriHelper.this.obeFormatNilais.size(); idxU++) {
										FormatNilai fnU = TugasMandiriHelper.this.obeFormatNilais.get(idxU);
										Double nvU = popupNilaiBoxes.get(idxU).getValue();
										String fnNamaU = fnU.getNama() != null ? fnU.getNama() : ("CPMK " + fnU.getId());
										Label lblNUpd = new Label(fnNamaU + ": " + (nvU != null && nvU > 0.0 ? Common.numberFormat.get().format(nvU) : "-"));
										lblNUpd.setStyle("font-size:11px;");
										lblNUpd.setParent(summaryVbox);
									}
								} else if (!popupNilaiBoxes.isEmpty()) {
									Double nvU = popupNilaiBoxes.get(0).getValue();
									Label lblNUpd = new Label("Nilai: " + (nvU != null && nvU > 0.0 ? Common.numberFormat.get().format(nvU) : "-"));
									lblNUpd.setStyle("font-size:11px;");
									lblNUpd.setParent(summaryVbox);
								}
								popupW.detach();
							} catch (Exception eSave) {
								lblSimpanStatus.setValue("Gagal: " + eSave.getMessage());
								lblSimpanStatus.setStyle("color:red;font-size:11px;");
								ais.common.ErrorAuditUtil.record(eSave, "TugasMandiriHelper popup Simpan");
							}
						}
					});
					popupW.doModal();
				}
			});
		} else if (!tidakPerluIkutTfc) {
			// Peserta: tampilkan nilai diri sendiri saja (read-only)
			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
					|| (tbmuser != null && tbmuser.getMahasiswa() != null
							&& tbmuser.getMahasiswa().getId().equals(tugasFileContent.getMahasiswa()))) {

				if (!TugasMandiriHelper.this.obeFormatNilais.isEmpty()) {
					for (FormatNilai formatNilaiR : TugasMandiriHelper.this.obeFormatNilais) {
						double nR = (!keyTfc.isEmpty() && !TugasMandiriHelper.this.jsonObjectTugas.isNull(keyTfc + "_nilai_" + formatNilaiR.getId()))
								? TugasMandiriHelper.this.jsonObjectTugas.getDouble(keyTfc + "_nilai_" + formatNilaiR.getId()) : 0.0;
						String fnNamaR = formatNilaiR.getNama() != null ? formatNilaiR.getNama() : ("CPMK " + formatNilaiR.getId());
						new MyLabelAgakKecilBold(nR > 0.1
								? fnNamaR + " : " + Common.numberFormat.get().format(nR) + "; "
								: fnNamaR + " belum di-input; ").setParent(nilaiKetCell);
					}
				} else {
					double nilaiLabelR = (!keyTfc.isEmpty() && !TugasMandiriHelper.this.jsonObjectTugas.isNull(keyTfc + "_nilai"))
							? TugasMandiriHelper.this.jsonObjectTugas.getDouble(keyTfc + "_nilai") : tugasFileContent.getNilai();
					new MyLabelAgakKecilBold(nilaiLabelR > 0.1
							? "Nilai : " + Common.numberFormat.get().format(nilaiLabelR)
							: "Nilai belum di-input").setParent(nilaiKetCell);
				}
			}
		}

		MyHboxToolbar hbox = new MyHboxToolbar();
		hbox.setSclass("ais-tugas-file-row");

		hbox.setVisible(mahasiswa == null || mahasiswa.getId().equals(tugasFileContent.getMahasiswa()));

		hbox.setParent(a);

		Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(namaFile,
				MyMenuitem.svgIcon(namaFile, FileFoto.icon(namaFile)));

		downloadButton.setStyle("font-size:10px;");

		downloadButton.setVisible(tbmuser != null && (

		(tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null) ||

				(

				(tbmuser.getMahasiswa() != null
						&& tbmuser.getMahasiswa().getId().equals(tugasFileContent.getMahasiswa()))

						|| (tbmuser.getSiswa() != null
								&& tbmuser.getSiswa().getId().equals(tugasFileContent.getSiswa()))

						|| (tbmuser.getBiodataCalonMahasiswa() != null && tbmuser.getBiodataCalonMahasiswa().getId()
								.equals(tugasFileContent.getBiodataCalonMahasiswa()))

						|| (tbmuser.getCalonSiswa() != null
								&& tbmuser.getCalonSiswa().getId().equals(tugasFileContent.getCalonSiswa()))

				)

		));

		downloadButton.setTooltiptext("Lihat / Download \"" + tugasFileContent.getNama() + "\"");
		hbox.appendChild(downloadButton);
		downloadButton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (tugasFileContent.getGdrive() != null) {
					tugasFileContent.tampilGDrive(null);
				} else {

					String link = tugasFileContent == null ? null
							: (tugasFileContent.getLink() == null || tugasFileContent.getLink().isEmpty() ? null
									: tugasFileContent.getLink());

					if (tugasFileContent != null
							&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
						link = tugasFileContent.createLinkUri();
						if (link != null) {
							// link = link.replaceAll("download=false", "download=true");
						}
					}

					if (tugasFileContent != null && link != null && !link.trim().isEmpty()) {

						if (tugasFileContent.bisaPreview()) {
							Common.displayWindow(tugasFileContent.merupakanGambar(), link, true, "95%", "95%",
									!tugasFileContent.getNama().toLowerCase().endsWith(".txt"), tugasFileContent);
						} else {
							ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
						}
					} else {
						MyMessageboxConfig.show("Mohon maaf, berkas yang Bapak/Ibu akses tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang halaman dan coba kembali; (2) pastikan berkas masih tersedia dan belum dihapus; (3) apabila kendala berlanjut, mohon menghubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
					}
				}
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TugasMandiriHelper}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TugasMandiriHelper} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List obeFormatNilais}; operasi lokal:
	 * {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * <p><strong>Peran konkret pada layar Tugas Mandiri.</strong> Renderer ini dipasang pada
	 * {@link TugasMandiriHelper#uploadTugasGrid} — grid daftar "Telah upload" — dan menerjemahkan satu
	 * {@link ais.database.model.file.TugasFileContent} menjadi satu baris. Ia sengaja dibuat sangat
	 * tipis: hanya menata perataan, kelas gaya, dan indikator warna status penilaian, lalu menyerahkan
	 * seluruh pembangunan sel kepada
	 * {@link TugasMandiriHelper#displayRow(ais.database.model.file.TugasFileContent, List, Component)}.
	 * Pembagian itu disengaja agar tampilan baris pada grid dan tampilan pada kartu
	 * "Tugas yang Anda Upload" milik peserta selalu identik — keduanya memanggil metode yang sama.</p>
	 *
	 * <p><strong>Umur instance.</strong> Sebuah instance hidup selama satu siklus pemuatan grid.
	 * {@link TugasMandiriHelper#reloadTugasFileContent(boolean)} selalu membuat instance baru sebelum
	 * memasang model, sehingga daftar Sub-CPMK yang dipegangnya tidak pernah usang.</p>
	 *
	 * <p><strong>Batas tanggung jawab.</strong> Renderer tidak melakukan query, tidak menghitung nilai,
	 * dan tidak memutuskan kewenangan. Satu-satunya keputusan berbasis peran yang diambilnya adalah
	 * memasang atau tidak memasang indikator warna, berdasarkan
	 * {@link TugasMandiriHelper#peserta}. Seluruh gerbang tampilan lainnya — termasuk visibilitas
	 * tombol unduh dan cabang sel nilai — berada di dalam {@code displayRow}.</p>
	 *
	 * @see TugasMandiriHelper
	 */
	class DetailTugasFileContentRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Daftar Sub-CPMK yang dipakai renderer ini untuk menyusun kolom nilai setiap baris.
		 *
		 * <p>Salinan rujukan dari {@link TugasMandiriHelper#obeFormatNilais} pada saat renderer dibuat,
		 * yaitu di dalam {@link TugasMandiriHelper#reloadTugasFileContent(boolean)} tepat setelah daftar
		 * itu dibangun ulang. Kosong berarti tugas ini tidak memakai penilaian OBE sehingga setiap baris
		 * hanya menampilkan satu nilai tunggal.</p>
		 *
		 * <p><strong>Catatan: bukan salinan dalam.</strong> Field ini menunjuk objek {@link List} yang
		 * sama dengan milik kelas induk, bukan salinannya. Karena kelas induk selalu membuat
		 * {@link ArrayList} baru pada setiap pemuatan ulang — bukan mengubah daftar lama di tempat —
		 * renderer yang sudah terpasang tetap melihat daftar versi pemuatan yang melahirkannya. Sifat
		 * ini aman selama pola "buat baru, jangan ubah di tempat" dipertahankan.</p>
		 *
		 * <p>Nilainya diteruskan apa adanya sebagai argumen kedua
		 * {@link TugasMandiriHelper#displayRow(TugasFileContent, List, Component)}.</p>
		 */
		private List<FormatNilai> obeFormatNilais;

		/**
		 * Membuat renderer baris untuk satu siklus pemuatan grid pengumpulan.
		 *
		 * <p>Konstruktor ini hanya menyimpan rujukan daftar Sub-CPMK yang berlaku saat renderer dibuat.
		 * Ia tidak menyentuh basis data, tidak membaca pengguna yang login, dan tidak membangun komponen
		 * apa pun — seluruh pekerjaan baru terjadi ketika ZK memanggil
		 * {@link #render(Row, Object)} untuk setiap baris yang terlihat.</p>
		 *
		 * <p><strong>Instance baru pada setiap pemuatan.</strong>
		 * {@link TugasMandiriHelper#reloadTugasFileContent(boolean)} selalu membuat renderer baru
		 * sebelum memasang model, bukan memakai ulang renderer lama. Dengan begitu perubahan mode
		 * penilaian — misalnya pengelola baru saja mencentang Sub-CPMK pada dialog ubah instruksi —
		 * langsung tercermin pada baris yang dirender berikutnya, tanpa perlu jalur pembaruan
		 * tersendiri.</p>
		 *
		 * <p><strong>Kelas dalam non-statis.</strong> Karena {@link DetailTugasFileContentRenderer}
		 * adalah kelas dalam biasa, setiap instance memegang rujukan tersembunyi ke instance
		 * {@link TugasMandiriHelper} yang melahirkannya. Rujukan itulah yang membuat
		 * {@link #render(Row, Object)} dapat membaca {@link TugasMandiriHelper#peserta} dan memanggil
		 * {@link TugasMandiriHelper#displayRow(TugasFileContent, List, Component)}. Konsekuensinya,
		 * renderer ini terikat pada satu desktop ZK dan tidak boleh disimpan atau dibagikan lintas
		 * sesi.</p>
		 *
		 * @param obeFormatNilais daftar Sub-CPMK yang dinilai pada tugas ini; kosong berarti mode
		 *                        penilaian nilai tunggal (non-OBE).
		 */
		public DetailTugasFileContentRenderer(List<FormatNilai> obeFormatNilais) {
			this.obeFormatNilais = obeFormatNilais;
		}

		/**
		 * Merender satu baris grid pengumpulan tugas.
		 *
		 * <p>Dipanggil oleh ZK untuk setiap elemen model yang perlu ditampilkan. Argumen {@code arg1}
		 * selalu berupa {@link TugasFileContent} karena model grid dibangun dari
		 * {@code new SimpleListModel(pertemuanFileContent)} yang isinya sudah homogen; tidak ada
		 * pemeriksaan tipe defensif di sini.</p>
		 *
		 * <p><strong>Tugas metode ini hanya dua.</strong> Pertama, menyetel perataan vertikal ke atas
		 * dan kelas gaya {@code ais-tugas-upload-row} pada baris. Kedua, memasang indikator status
		 * penilaian — dan itu pun hanya untuk pengelola. Seluruh isi sel didelegasikan sepenuhnya ke
		 * {@link TugasMandiriHelper#displayRow(TugasFileContent, List, Component)}.</p>
		 *
		 * <p><strong>Indikator warna.</strong> Bila {@link TugasMandiriHelper#peserta} bernilai
		 * {@code false}, baris diberi garis tepi kiri setebal 4 piksel: hijau ({@code #16a34a}) bila
		 * {@code tugasFileContent.getNilai()} tidak {@code null} dan lebih besar dari {@code 0.1},
		 * merah ({@code #dc2626}) bila tidak. Garis ini memungkinkan pengelola memindai daftar panjang
		 * dan langsung melihat siapa yang belum dinilai. Bagi peserta, indikator sengaja tidak dipasang
		 * karena status penilaian peserta lain bukan urusannya.</p>
		 *
		 * <p><strong>Perhatian: sumber angka indikator berbeda dari sumber angka yang ditampilkan.</strong>
		 * Indikator ini membaca kolom {@code nilai} pada baris {@link TugasFileContent}, sedangkan
		 * angka nilai yang benar-benar ditampilkan di sel "Nilai &amp; Keterangan" dibaca dari dokumen
		 * JSON {@link TugasMandiriHelper#jsonObjectTugas}. Keduanya tidak selalu sinkron: nilai yang
		 * disimpan lewat popup "Edit Nilai" hanya menulis dokumen JSON. Akibatnya sebuah baris dapat
		 * menampilkan nilai yang sudah terisi namun tetap bergaris merah sampai jalur lain menuliskan
		 * kolom {@code nilai}. Batas {@code 0.1} dipakai — bukan {@code 0} — agar nilai nol yang berarti
		 * "belum diisi" tidak terbaca sebagai sudah dinilai.</p>
		 *
		 * <p>Metode ini tidak menangkap pengecualian. Kegagalan pada
		 * {@link TugasMandiriHelper#displayRow(TugasFileContent, List, Component)} akan naik ke ZK dan
		 * menggagalkan render baris tersebut; isolasi kegagalan pemuatan dilakukan satu lapis di atas,
		 * di dalam {@link TugasMandiriHelper#reloadTugasFileContent(boolean)}.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen.
		 * @param arg1 elemen model, selalu berupa {@link TugasFileContent}.
		 * @throws Exception bila pembangunan komponen sel gagal.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			TugasFileContent tugasFileContent = (TugasFileContent) arg1;
			// Indikator warna: hijau = sudah dinilai (nilai > 0), merah = belum dinilai.
			// Ditampilkan hanya untuk pengelola (bukan peserta/mahasiswa).
			if (!peserta) {
				boolean sudahDinilai = tugasFileContent.getNilai() != null && tugasFileContent.getNilai() > 0.1;
				arg0.setSclass("ais-tugas-upload-row");
				arg0.setStyle(sudahDinilai
						? "border-left:4px solid #16a34a;"
						: "border-left:4px solid #dc2626;");
			} else {
				arg0.setSclass("ais-tugas-upload-row");
			}
			displayRow(tugasFileContent, obeFormatNilais, arg0);
		}
	}

	/**
	 * Menandai semua mahasiswa yang TIDAK ikut diskusi pada pertemuan tertentu sebagai alpa/mangkir.
	 *
	 * <p>Menampilkan dialog konfirmasi terlebih dahulu. Setelah dikonfirmasi, iterasi seluruh
	 * mahasiswa peserta pertemuan; yang tidak ada di {@code diskusis} akan di-set absensi
	 * {@code TIDAK_ADA_ALASAN} dengan keterangan "Tidak ikut diskusi di pertemuan". Setelah
	 * selesai, memanggil {@code eventListener} untuk me-refresh tampilan.</p>
	 *
	 * <p><strong>Tidak memiliki gerbang kewenangan sendiri.</strong> Seperti tiga metode kehadiran
	 * massal lainnya di kelas ini, metode ini bersifat {@code static}, dapat dipanggil dari mana saja,
	 * dan tidak memeriksa siapa pengguna yang sedang login. Satu-satunya pembatasan berada pada
	 * gerbang visibilitas tombol pemanggilnya. Setiap pemanggil baru wajib memasang gerbangnya
	 * sendiri.</p>
	 *
	 * <p><strong>Penulisan absensi bersifat menimpa.</strong> {@code Pertemuan.populate(...)} menulis
	 * status kehadiran peserta untuk pertemuan ini tanpa memeriksa apakah sudah ada catatan
	 * sebelumnya. Menjalankan aksi ini setelah kehadiran diisi manual akan menimpa isian manual
	 * tersebut, dan tidak ada jalur pembatalan otomatis — pemulihan harus dilakukan lewat penyuntingan
	 * absensi.</p>
	 *
	 * <p><strong>Jam mulai dan selesai.</strong> Diambil dari {@code retreiveAbsensiMulai(id)} dan
	 * {@code retreiveAbsensiSampai(id)} milik pertemuan; bila keduanya kosong, dipakai
	 * {@code getWaktuMulai()} dan {@code getWaktuSelesai()} pertemuan sebagai nilai bawaan. Pola ini
	 * identik pada keempat metode kehadiran massal.</p>
	 *
	 * <p>Seluruh pekerjaan dijalankan lewat {@code Common.createDefaultTimer(...)} sehingga terjadi
	 * pada siklus event berikutnya, dan diakhiri satu kali {@code Common.refreshUpdate(session, pa)}
	 * untuk menyimpan seluruh perubahan absensi sekaligus.</p>
	 *
	 * @param diskusis      map id mahasiswa ke daftar id diskusi yang diikuti; mahasiswa yang
	 *                      tidak ada di map ini dianggap tidak ikut diskusi.
	 * @param pa            pertemuan yang menjadi konteks absensi.
	 * @param eventListener callback dipanggil setelah proses selesai.
	 * @throws Exception jika terjadi kesalahan DB atau ZK.
	 */
	public static void tidakIkutDiskusiDiangapTidakHadir(final java.util.Map<String, List<Long>> diskusis,
			final Pertemuan pa, final EventListener eventListener) throws Exception {
		MyMessageboxConfig.showFormatCb(
				"Apakah Bapak/Ibu yakin semua mahasiswa yang tidak mengikuti diskusi pada pertemuan \"{V1}\" akan dianggap alpa atau mangkir pada kelas ini?",

				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									if (pa.getId() != null) {
										session.refresh(pa);
									}

									List<Mahasiswa> mahasiswasTemorary = pa.ambilMahasiswa();

									for (Mahasiswa o : mahasiswasTemorary) {

										if (diskusis != null && !diskusis.containsKey(o.getId() + "_mhs")) {
											Long mhs = o.getId();

											Statusabsensi statusabsensi = ConstantValues.TIDAK_ADA_ALASAN;

											String mulai = pa.retreiveAbsensiMulai(mhs);
											String sampai = pa.retreiveAbsensiSampai(mhs);
											if (mulai == null || mulai.trim().isEmpty()) {
												mulai = pa.getWaktuMulai();
											}
											if (sampai == null || sampai.trim().isEmpty()) {
												sampai = pa.getWaktuSelesai();
											}

											pa.populate(mhs, statusabsensi,
													"Tidak ikut diskusi di pertemuan \"" + pa.info() + "\"", null,
													mulai, sampai, "Mahasiswa");

										}

									}

									Common.refreshUpdate(session, pa);

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				}, pa.info());
	}

	/**
	 * Menandai semua mahasiswa yang TIDAK mengakses pertemuan sebagai alpa/mangkir.
	 *
	 * <p>Menampilkan dialog konfirmasi. Setelah dikonfirmasi, iterasi seluruh mahasiswa peserta;
	 * yang belum pernah membuka halaman pertemuan (ambilData "akses" kosong) akan di-set
	 * absensi {@code TIDAK_ADA_ALASAN} dengan keterangan sesuai. Setelah selesai memanggil
	 * {@code eventListener} untuk me-refresh tampilan absensi.</p>
	 *
	 * <p><strong>Definisi "tidak mengakses".</strong> Peserta dianggap belum mengakses bila
	 * {@code pa.ambilData("akses", idMahasiswa)} mengembalikan peta kosong. Data akses itu dicatat
	 * oleh {@code TampilanELearningAction.dilihat(...)} setiap kali halaman dibuka, sehingga metode ini
	 * mengukur keterbacaan halaman pertemuan — bukan pengumpulan tugas. Pasangannya untuk tugas adalah
	 * {@link #tidakUploadTugasDiangapTidakHadir(Tugas, Pertemuan, EventListener)}.</p>
	 *
	 * <p><strong>Tidak memiliki gerbang kewenangan sendiri.</strong> Metode {@code static} ini tidak
	 * memeriksa pengguna yang sedang login; pembatasan sepenuhnya berada pada gerbang visibilitas
	 * tombol pemanggilnya. Penulisan absensi bersifat menimpa catatan yang sudah ada dan tidak dapat
	 * dibatalkan secara otomatis.</p>
	 *
	 * <p><strong>Cakupan peserta.</strong> Hanya {@code pa.ambilMahasiswa()} yang ditelusuri, sehingga
	 * peserta berjenis siswa maupun calon tidak pernah ikut ditandai. Keterangan absensi yang ditulis
	 * berbunyi "Tidak akses di pertemuan" diikuti {@code pa.info()}.</p>
	 *
	 * @param pa            pertemuan yang menjadi konteks absensi.
	 * @param eventListener callback dipanggil setelah proses selesai.
	 * @throws Exception jika terjadi kesalahan DB atau ZK.
	 */
	public static void tidakAksesDiangapTidakHadir(final Pertemuan pa, final EventListener eventListener)
			throws Exception {
		MyMessageboxConfig.showFormatCb(
				"Apakah Bapak/Ibu yakin semua mahasiswa yang tidak mengakses pertemuan \"{V1}\" akan dianggap alpa atau mangkir pada kelas ini?",

				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									if (pa.getId() != null) {
										session.refresh(pa);
									}

									List<Mahasiswa> mahasiswasTemorary = pa.ambilMahasiswa();

									for (Mahasiswa o : mahasiswasTemorary) {

										TreeMap<String, String> d = pa.ambilData("akses", o.getId().toString());

										if (d.isEmpty()) {
											Long mhs = o.getId();

											Statusabsensi statusabsensi = ConstantValues.TIDAK_ADA_ALASAN;

											String mulai = pa.retreiveAbsensiMulai(mhs);
											String sampai = pa.retreiveAbsensiSampai(mhs);
											if (mulai == null || mulai.trim().isEmpty()) {
												mulai = pa.getWaktuMulai();
											}
											if (sampai == null || sampai.trim().isEmpty()) {
												sampai = pa.getWaktuSelesai();
											}

											pa.populate(mhs, statusabsensi,
													"Tidak akses di pertemuan \"" + pa.info() + "\"", null, mulai,
													sampai, "Mahasiswa");

										}
										d = null;
									}

									Common.refreshUpdate(session, pa);

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				}, pa.info());
	}

	/**
	 * Menandai semua mahasiswa yang TIDAK mengumpulkan berkas tugas sebagai alpa/mangkir.
	 *
	 * <p>Menampilkan dialog konfirmasi terlebih dahulu. Setelah dikonfirmasi, iterasi seluruh
	 * mahasiswa peserta perkuliahan; mahasiswa yang tidak memiliki catatan pengumpulan pada
	 * {@code TugasFileContent} untuk tugas ini akan di-set absensi menjadi tidak hadir
	 * (status {@code TIDAK_ADA_ALASAN}). Proses berjalan dalam transaksi Hibernate baru
	 * yang dibuka via {@code HibernateUtil.openSession()} dan ditutup di blok {@code finally}.
	 * Callback {@code eventListener} dipanggil setelah proses selesai untuk memperbarui
	 * tampilan absensi di grid.</p>
	 *
	 * <p><strong>Peserta yang dilewati.</strong> Selain peserta yang memang sudah mengumpulkan —
	 * dideteksi lewat {@code treemapData.containsKey(idMahasiswa)} atas hasil
	 * {@code tugas.ambilTugasFileContentTotal()} tanpa argumen, yaitu seluruh pengumpulan dan bukan
	 * satu halaman — peserta yang tercantum pada {@code tugas.getMhsYgTidakIkut()} juga dilewati.
	 * Penandaan "tidak perlu ikut" karena itu berdampak langsung pada absensi: peserta yang ditandai
	 * tidak akan pernah dicatat alpa oleh aksi ini.</p>
	 *
	 * <p><strong>Keterangan absensi.</strong> Ditulis sebagai "Tidak Mengumpulkan &quot;judul&quot;
	 * sampai tanggal/waktu ..." dengan batas waktu diambil dari {@code tugas.getSelesai()}, atau waktu
	 * saat ini bila tugas tidak memiliki batas selesai.</p>
	 *
	 * <p><strong>Tidak memiliki gerbang kewenangan sendiri.</strong> Metode {@code static} ini tidak
	 * memeriksa pengguna yang sedang login; pembatasan berada pada gerbang visibilitas tombol
	 * pemanggilnya di tab "Belum upload". Penulisan absensi bersifat menimpa dan tidak dapat
	 * dibatalkan secara otomatis. Hanya {@code pa.ambilMahasiswa()} yang ditelusuri, sehingga peserta
	 * berjenis siswa maupun calon tidak ikut ditandai.</p>
	 *
	 * @param tugas         entitas tugas (dapat {@code Pertemuan} atau {@code TugasPertemuan}).
	 * @param pa            pertemuan induk tempat absensi dicatat.
	 * @param eventListener callback dipanggil setelah proses selesai.
	 * @throws Exception jika terjadi kesalahan sesi Hibernate atau rendering ZK.
	 */
	public static void tidakUploadTugasDiangapTidakHadir(final Tugas tugas, final Pertemuan pa,
			final EventListener eventListener) throws Exception {
		MyMessageboxConfig.showFormatCb(
				"Apakah Bapak/Ibu yakin semua mahasiswa yang tidak mengumpulkan \"{V1}\" akan dianggap alpa atau mangkir pada kelas ini?",

				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									if (pa.getId() != null) {
										session.refresh(pa);
									}

									List<Mahasiswa> mahasiswasTemorary = pa.ambilMahasiswa();
									TreeMap<Long, TugasFileContent> treemapData = tugas.ambilTugasFileContentTotal();
									for (Mahasiswa o : mahasiswasTemorary) {

										if (treemapData != null && !treemapData.containsKey(o.getId())) {
											Long mhs = o.getId();
											if (!tugas.getMhsYgTidakIkut().contains("," + mhs + ",")) {
												Statusabsensi statusabsensi = ConstantValues.TIDAK_ADA_ALASAN;

												String mulai = pa.retreiveAbsensiMulai(mhs);
												String sampai = pa.retreiveAbsensiSampai(mhs);
												if (mulai == null || mulai.trim().isEmpty()) {
													mulai = pa.getWaktuMulai();
												}
												if (sampai == null || sampai.trim().isEmpty()) {
													sampai = pa.getWaktuSelesai();
												}

												pa.populate(mhs, statusabsensi,
														"Tidak Mengumpulkan \"" + tugas.getJudultugas()
																+ "\" sampai tanggal/waktu "
																+ Common.dateFormat5.get().format(
																		tugas == null || tugas.getSelesai() == null
																				? WaktuUtil.getDate()
																				: tugas.getSelesai()),
														null, mulai, sampai, "Mahasiswa");
											}
										}

									}

									Common.refreshUpdate(session, pa);

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				}, tugas.getJudultugas());
	}

	/**
	 * Menandai semua mahasiswa yang TELAH mengumpulkan berkas tugas sebagai hadir.
	 *
	 * <p>Kebalikan dari {@link #tidakUploadTugasDiangapTidakHadir}: iterasi
	 * {@code TugasFileContent} milik tugas ini; setiap entri yang memiliki pemilik mahasiswa
	 * peserta akan di-set absensi pertemuan menjadi hadir (status {@code HADIR}). Dialog
	 * konfirmasi muncul sebelum proses dimulai. Setelah selesai, callback {@code eventListener}
	 * dipanggil agar tampilan absensi diperbarui. Sesi Hibernate baru dibuka via
	 * {@code HibernateUtil.openSession()} dan selalu ditutup di blok {@code finally}.</p>
	 *
	 * <p><strong>Arah iterasi berbeda dari ketiga metode kehadiran lainnya.</strong> Metode ini
	 * menelusuri baris pengumpulan ({@code tugas.ambilTugasFileContentTotal().values()}), bukan daftar
	 * peserta. Konsekuensinya, id yang dipakai adalah {@code o.getMahasiswa()} — dan untuk baris yang
	 * pemiliknya bukan mahasiswa, nilai itu berupa bilangan acak negatif yang sengaja ditulis oleh
	 * {@link #prosesAnggapSemuaSudahUpload(Tugas, Pertemuan)}, sehingga tidak pernah cocok dengan
	 * peserta mana pun.</p>
	 *
	 * <p><strong>Baris "dianggap mengumpulkan" ikut terhitung hadir.</strong> Karena yang diperiksa
	 * hanyalah keberadaan baris pengumpulan, peserta yang ditandai lewat
	 * {@link #anggapSemuaSudahUpload(Tugas, Pertemuan, EventListener)} — yang berkasnya kosong — juga
	 * akan ditandai hadir oleh aksi ini. Keterangan absensinya menyebutkan nama berkas, yang untuk
	 * kasus tersebut berakhiran "_(kosong)".</p>
	 *
	 * <p><strong>Tidak memiliki gerbang kewenangan sendiri.</strong> Metode {@code static} ini tidak
	 * memeriksa pengguna yang sedang login; pembatasan berada pada gerbang visibilitas tombol
	 * "Anggap Hadir (Pengumpul Tugas)". Peserta yang tercantum pada {@code mhsYgTidakIkut} dilewati.
	 * Penulisan absensi bersifat menimpa dan tidak dapat dibatalkan secara otomatis.</p>
	 *
	 * @param tugas         entitas tugas yang menjadi referensi pengumpulan.
	 * @param pa            pertemuan induk tempat absensi dicatat.
	 * @param eventListener callback dipanggil setelah proses selesai.
	 * @throws Exception jika terjadi kesalahan sesi Hibernate atau rendering ZK.
	 */
	public static void uploadTugasDiangapHadir(final Tugas tugas, final Pertemuan pa, final EventListener eventListener)
			throws Exception {
		MyMessageboxConfig.showFormatCb(
				"Apakah Bapak/Ibu yakin semua mahasiswa yang mengumpulkan \"{V1}\" akan dianggap hadir pada kelas ini?",

				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									if (pa.getId() != null) {
										session.refresh(pa);
									}

									for (TugasFileContent o : tugas.ambilTugasFileContentTotal().values()) {
										Long mhs = o.getMahasiswa();
										if (!tugas.getMhsYgTidakIkut().contains("," + mhs + ",")) {
											Date uploadDate = o.getUploadDate();
											String nama = o.getNama();
											Statusabsensi statusabsensi = ConstantValues.MASUK;

											String mulai = pa.retreiveAbsensiMulai(mhs);
											String sampai = pa.retreiveAbsensiSampai(mhs);
											if (mulai == null || mulai.trim().isEmpty()) {
												mulai = pa.getWaktuMulai();
											}
											if (sampai == null || sampai.trim().isEmpty()) {
												sampai = pa.getWaktuSelesai();
											}

											pa.populate(mhs, statusabsensi,
													"Mengumpulkan \"" + tugas.getJudultugas() + "\" pada "
															+ Common.dateFormat5.get().format(uploadDate)
															+ " dengan nama file : " + nama,
													null, mulai, sampai, "Mahasiswa");
										}

									}

									Common.refreshUpdate(session, pa);

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				}, tugas.getJudultugas());
	}

	/**
	 * Menandai SEMUA mahasiswa peserta telah mengumpulkan tugas dengan FILE KOSONG
	 * (Blob foto = null) — HANYA untuk mahasiswa yang belum pernah upload. Yang sudah
	 * upload tidak diubah; yang ditandai "tidak ikut" dilewati. Minta konfirmasi dahulu,
	 * lalu jalankan proses di latar.
	 *
	 * <p><strong>Untuk apa fitur ini.</strong> Sejumlah proses administrasi — rekap kehadiran, syarat
	 * mengikuti ujian, atau penutupan komponen nilai — bertumpu pada keberadaan baris pengumpulan,
	 * bukan pada isinya. Ketika kelas dinilai secara luring atau pengumpulan dilakukan di luar sistem,
	 * pengelola membutuhkan cara menandai bahwa seluruh peserta "sudah mengumpulkan" tanpa harus
	 * mengunggah berkas satu per satu. Tombol ini menyediakan jalur itu dengan membuat baris
	 * {@link TugasFileContent} berisi Blob kosong.</p>
	 *
	 * <p><strong>Yang dilindungi.</strong> Proses bersifat menambah saja. Peserta yang sudah pernah
	 * mengunggah tidak disentuh sedikit pun — berkas, tanggal, dan nilainya tetap seperti semula — dan
	 * peserta yang ditandai pada {@code tugas.getMhsYgTidakIkut()} dilewati. Karena itu menekan tombol
	 * ini dua kali tidak menghasilkan baris ganda.</p>
	 *
	 * <p><strong>Alur.</strong> Kotak konfirmasi ditampilkan lebih dahulu dengan judul tugas
	 * disisipkan pada pesannya; bila pengguna memilih selain OK, proses dibatalkan. Bila disetujui,
	 * pekerjaan dijadwalkan lewat {@code Common.createDefaultTimer(...)} agar berjalan pada siklus
	 * event berikutnya, lalu {@link #prosesAnggapSemuaSudahUpload(Tugas, Pertemuan)} dijalankan.
	 * Jumlah baris yang benar-benar dibuat dilaporkan kepada pengguna, dan {@code eventListener}
	 * dipanggil — juga lewat timer — agar daftar pengumpulan dimuat ulang.</p>
	 *
	 * <p><strong>Gerbang kewenangan.</strong> Metode ini bersifat {@code static} dan tidak memeriksa
	 * kewenangan apa pun. Pembatasan sepenuhnya berada pada tombol pemanggilnya di
	 * {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)}, yang hanya ditampilkan bila
	 * {@link #tbmuser} bukan pelajar dan pertemuan bukan bagian dari jadwal ujian PMB/PSB. Setiap
	 * pemanggil baru wajib memasang gerbangnya sendiri.</p>
	 *
	 * @param tugas         tugas yang seluruh pesertanya akan ditandai sudah mengumpulkan.
	 * @param pa            pertemuan induk, dipakai sebagai sumber daftar peserta.
	 * @param eventListener callback yang dipanggil setelah proses selesai, untuk memuat ulang daftar.
	 * @throws Exception bila kotak konfirmasi gagal ditampilkan.
	 */
	public static void anggapSemuaSudahUpload(final Tugas tugas, final Pertemuan pa, final EventListener eventListener)
			throws Exception {
		MyMessageboxConfig.showFormatCb(
				"Apakah Bapak/Ibu yakin ingin menandai SEMUA mahasiswa/siswa telah mengumpulkan \"{V1}\" dengan FILE KOSONG?\n\nHanya mahasiswa/siswa yang BELUM pernah mengunggah yang akan ditambahkan (dengan isi berkas kosong). Data yang sudah diunggah tidak akan diubah.",


				"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i != MyMessageboxConfig.OK) {
							return;
						}
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								int dibuat = prosesAnggapSemuaSudahUpload(tugas, pa);
								MyMessageboxConfig.showFormat(
										"Sebanyak {V1} mahasiswa telah ditandai mengumpulkan tugas (dengan berkas kosong).", "Selesai",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, dibuat);
								Common.createDefaultTimer(eventListener);
							}
						});
					}
				}, tugas.getJudultugas());
	}

	/**
	 * Inti proses: untuk tiap mahasiswa peserta yang BELUM upload, buat
	 * {@link TugasFileContent} dengan Blob foto = null (file kosong). Memakai sesi
	 * streaming (sama dengan alur upload tugas biasa). Return jumlah baris yang dibuat.
	 *
	 * <h3>Langkah</h3>
	 * <ol>
	 *   <li>Menyusun dua himpunan id peserta yang sudah pernah mengunggah, dibaca dari
	 *       {@code tugas.ambilTugasFileContentTotal()} tanpa argumen — yakni seluruh pengumpulan, bukan
	 *       satu halaman. Pembacaan ini dibungkus {@code try}/{@code catch} pencatat audit; bila gagal,
	 *       himpunan tetap kosong sehingga proses berlanjut dengan anggapan belum ada yang
	 *       mengumpulkan.</li>
	 *   <li>Mengambil daftar peserta: {@code pa.ambilMahasiswa()} dan, dalam
	 *       {@code try}/{@code catch} tersendiri, {@code pa.ambilSiswa()}. Bila kedua daftar kosong,
	 *       metode langsung mengembalikan {@code 0}.</li>
	 *   <li>Membuka sesi streaming beserta transaksinya, lalu menelusuri kedua daftar. Peserta
	 *       dilewati bila id-nya sudah ada di himpunan "sudah mengunggah" atau tercantum pada
	 *       {@code mhsYgTidakIkut}.</li>
	 *   <li>Untuk sisanya dibuat {@link TugasFileContent} baru dengan {@code foto} bernilai
	 *       {@code null} (berkas kosong), {@code fileMimeType} {@code null}, nama berbentuk
	 *       "NIM_Nama_(kosong)", {@code pertemuan} berisi {@code tugas.getId()}, tanggal unggah saat
	 *       ini, keterangan "Dianggap mengumpulkan (file kosong)", serta {@code olehId} dan
	 *       {@code oleh} yang diisi dari pengguna yang sedang login sebagai jejak audit.</li>
	 *   <li>Commit, lalu di luar transaksi cache dibersihkan lewat {@code tugas.belum(...)} dan
	 *       {@code tugas.reInitTugasFileContent()} agar baris baru ikut tampil pada pemuatan
	 *       berikutnya.</li>
	 * </ol>
	 *
	 * <p><strong>Kolom pemilik yang tidak dipakai diisi bilangan acak negatif.</strong> Selain kolom
	 * pemiliknya yang sebenarnya, ketiga kolom pemilik lain diisi {@code -Common.randLong()}. Nilai
	 * negatif acak dipakai — bukan {@code null} — agar baris tetap membawa nilai unik pada kolom-kolom
	 * itu tanpa pernah cocok dengan id peserta mana pun. Konsekuensinya, kode yang menentukan pemilik
	 * lewat rangkaian {@code if}/{@code else if} harus tetap memeriksa kolom mahasiswa lebih dahulu,
	 * sebagaimana dilakukan pada penyusunan kunci JSON nilai dan pada
	 * {@link #displayRow(TugasFileContent, List, Component)}.</p>
	 *
	 * <p><strong>Penanganan kegagalan.</strong> Kegagalan memicu rollback lewat
	 * {@code StreamingHibernateUtil.getInstance().rollbackTransaction()}, pencatatan audit, dan pesan
	 * formal berisi langkah lanjutan bagi pengguna. Blok {@code finally} menutup sesi berlapis dengan
	 * masing-masing langkah dibungkus {@code try}/{@code catch} agar koneksi selalu dikembalikan.
	 * Perhatikan bahwa nilai balik {@code dibuat} sudah bertambah sebelum commit; bila commit gagal,
	 * angka yang dilaporkan kepada pengguna dapat lebih besar daripada jumlah baris yang benar-benar
	 * tersimpan — karena itu pesan kegagalan meminta pengguna memuat ulang halaman dan memeriksa data
	 * mana saja yang sempat tersimpan.</p>
	 *
	 * <p><strong>Peserta calon mahasiswa dan calon siswa tidak dicakup.</strong> Metode ini hanya
	 * menelusuri {@link Mahasiswa} dan {@link ais.database.model.sekolah.Siswa}; peserta berjenis
	 * {@link BiodataCalonMahasiswa} maupun {@link ais.database.model.sekolah.CalonSiswa} tidak pernah
	 * memperoleh baris kosong. Hal itu selaras dengan gerbang tombolnya yang mensyaratkan pertemuan
	 * bukan bagian dari jadwal ujian PMB/PSB.</p>
	 *
	 * @param tugas tugas yang baris pengumpulan kosongnya akan dibuat.
	 * @param pa    pertemuan induk, sumber daftar peserta.
	 * @return jumlah baris {@link TugasFileContent} yang dibuat; {@code 0} bila tidak ada peserta atau
	 *         seluruh peserta sudah mengumpulkan.
	 */
	private static int prosesAnggapSemuaSudahUpload(Tugas tugas, Pertemuan pa) {
		int dibuat = 0;
		Tbmuser tbmuser = Common.getCurrentUser();

		java.util.Set<Long> sudahMhs = new java.util.HashSet<Long>();
		java.util.Set<Long> sudahSiswa = new java.util.HashSet<Long>();
		try {
			for (TugasFileContent o : tugas.ambilTugasFileContentTotal().values()) {
				if (o == null) {
					continue;
				}
				if (o.getMahasiswa() != null) {
					sudahMhs.add(o.getMahasiswa());
				}
				if (o.getSiswa() != null) {
					sudahSiswa.add(o.getSiswa());
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:4072");
		}

		List<Mahasiswa> pesertaMhs = pa.ambilMahasiswa();
		List<Siswa> pesertaSiswa = null;
		try {
			pesertaSiswa = pa.ambilSiswa();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:4079");
		}

		boolean adaMhs = pesertaMhs != null && !pesertaMhs.isEmpty();
		boolean adaSiswa = pesertaSiswa != null && !pesertaSiswa.isEmpty();
		if (!adaMhs && !adaSiswa) {
			return 0;
		}

		String tidakIkut = tugas.getMhsYgTidakIkut();
		Session session = null;
		try {
			session = StreamingHibernateUtil.getInstance().currentSession();
			session.getTransaction().begin();

			// --- Mahasiswa ---
			if (adaMhs) {
				for (Mahasiswa mhs : pesertaMhs) {
					if (mhs == null || mhs.getId() == null) {
						continue;
					}
					Long id = mhs.getId();
					if (sudahMhs.contains(id)) {
						continue; // sudah pernah upload -> tidak diubah
					}
					if (tidakIkut != null && tidakIkut.contains("," + id + ",")) {
						continue; // ditandai tidak ikut -> dilewati
					}

					TugasFileContent c = new TugasFileContent(tugas.getClass().getName());
					c.setFoto(null); // FILE KOSONG: Blob ditulis null
					c.setNama((mhs.getNim() == null ? "" : mhs.getNim().trim()) + "_"
							+ (mhs.getNama() == null ? "" : mhs.getNama()) + "_(kosong)");
					c.setFileMimeType(null);
					c.setMahasiswa(id);
					c.setBiodataCalonMahasiswa(-Common.randLong());
					c.setCalonSiswa(-Common.randLong());
					c.setSiswa(-Common.randLong());
					c.setPertemuan(tugas.getId());
					c.setUploadDate(ais.ui.util.WaktuUtil.getDate());
					c.setKeterangan("Dianggap mengumpulkan (file kosong)");
					c.setOlehId(Common.generateOlehId(tbmuser));
					c.setOleh(tbmuser == null ? "external_update" : tbmuser.getUserNama());
					session.save(c);
					dibuat++;
				}
			}

			// --- Siswa ---
			if (adaSiswa) {
				for (Siswa sw : pesertaSiswa) {
					if (sw == null || sw.getId() == null) {
						continue;
					}
					Long id = sw.getId();
					if (sudahSiswa.contains(id)) {
						continue; // sudah pernah upload -> tidak diubah
					}
					if (tidakIkut != null && tidakIkut.contains("," + id + ",")) {
						continue; // ditandai tidak ikut -> dilewati
					}

					TugasFileContent c = new TugasFileContent(tugas.getClass().getName());
					c.setFoto(null); // FILE KOSONG: Blob ditulis null
					c.setNama((sw.getNim() == null ? "" : sw.getNim().trim()) + "_"
							+ (sw.getNama() == null ? "" : sw.getNama()) + "_(kosong)");
					c.setFileMimeType(null);
					c.setSiswa(id);
					c.setMahasiswa(-Common.randLong());
					c.setBiodataCalonMahasiswa(-Common.randLong());
					c.setCalonSiswa(-Common.randLong());
					c.setPertemuan(tugas.getId());
					c.setUploadDate(ais.ui.util.WaktuUtil.getDate());
					c.setKeterangan("Dianggap mengumpulkan (file kosong)");
					c.setOlehId(Common.generateOlehId(tbmuser));
					c.setOleh(tbmuser == null ? "external_update" : tbmuser.getUserNama());
					session.save(c);
					dibuat++;
				}
			}

			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:4164");
			}
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"memproses penandaan siswa/mahasiswa yang dianggap mengumpulkan (berkas kosong)",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu periksa data mana saja yang sudah sempat tersimpan.",
							"Ulangi kembali proses ini bila belum semua peserta selesai diproses.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			try {
				if (session != null && session.isOpen()) {
					session.disconnect();
					session.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:4173");
			}
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:4177");
			}
		}

		// segarkan cache agar baris baru tampil saat reload
		try {
			tugas.belum("tugas_file_content_" + tugas.getClass().getName());
			tugas.reInitTugasFileContent();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasMandiriHelper.java:4185");
		}

		return dibuat;
	}

	// =========================================================================
	// HELPER METHODS REUSABLE
	// =========================================================================

	/**
	 * Memeriksa apakah pengguna yang sedang login adalah pengelola (dosen/admin/pegawai),
	 * bukan pelajar (mahasiswa/siswa/calon/peserta kursus). Digunakan untuk mengontrol
	 * visibilitas tombol dan aksi yang hanya boleh dilakukan oleh pengelola, seperti
	 * mengubah instruksi tugas, menghapus tugas, memasukkan nilai, dan mengunduh berkas.
	 *
	 * <p><strong>Status pemakaian: belum dipanggil dari mana pun.</strong> Metode ini disediakan
	 * sebagai bentuk terpusat dari rantai pemeriksaan peran, tetapi seluruh gerbang di kelas ini masih
	 * menuliskan rantainya sendiri secara sebaris. Akibatnya rantai tersebut tersalin puluhan kali
	 * dengan variasi kecil: sebagian menguji {@code getSiswa()} lebih dari sekali, dan sebagian
	 * melewatkan {@code getPesertaKursus()} sehingga peserta kursus lolos dari gerbang yang seharusnya
	 * menutupnya. Metode ini menguji kelima tautan pelajar secara lengkap, sehingga memakainya
	 * sekaligus menyeragamkan dan memperbaiki gerbang-gerbang tersebut.</p>
	 *
	 * <p><strong>Semantik.</strong> "Pengelola" di sini didefinisikan secara negatif: pengguna yang
	 * tidak tertaut ke satu pun entitas pelajar. Definisi ini tidak melihat hak akses menu, satuan
	 * kerja, ataupun kepemilikan kelas — seorang dosen yang tidak mengampu perkuliahan ini tetap
	 * dinilai "boleh kelola" oleh metode ini. Pembatasan yang lebih halus, bila diperlukan, harus
	 * ditambahkan di atas metode ini, bukan menggantikannya.</p>
	 *
	 * <p>Pengguna {@code null} — sesi tidak dapat dibaca — dinilai {@code false}, sehingga sifatnya
	 * fail-closed.</p>
	 *
	 * @param user entitas Tbmuser yang sedang login, boleh null.
	 * @return {@code true} jika user bukan pelajar (boleh kelola tugas), {@code false} jika
	 *         user adalah mahasiswa/siswa/calon/peserta kursus atau user null.
	 */
	private boolean bolehKelolaTugas(Tbmuser user) {
		return user != null
				&& user.getMahasiswa() == null
				&& user.getSiswa() == null
				&& user.getBiodataCalonMahasiswa() == null
				&& user.getCalonSiswa() == null
				&& user.getPesertaKursus() == null;
	}

	/**
	 * Memeriksa apakah pengguna yang sedang login adalah pelajar yang berhak mengupload
	 * berkas tugas (mahasiswa, siswa, calon mahasiswa, calon siswa, atau peserta kursus).
	 * Digunakan untuk mengontrol visibilitas tombol Upload Tugas dan tampilan status upload.
	 *
	 * <p><strong>Status pemakaian: belum dipanggil dari mana pun.</strong> Sama seperti
	 * {@link #bolehKelolaTugas(Tbmuser)}, metode ini disediakan sebagai bentuk terpusat namun belum
	 * menggantikan rantai pemeriksaan sebaris yang tersebar di kelas ini.</p>
	 *
	 * <p><strong>Bukan kebalikan sempurna dari {@link #bolehKelolaTugas(Tbmuser)}.</strong> Untuk
	 * pengguna non-{@code null}, kedua metode memang saling melengkapi karena menguji kelima tautan
	 * pelajar yang sama. Namun untuk {@code user} bernilai {@code null} keduanya sama-sama
	 * mengembalikan {@code false} — bukan salah satu {@code true}. Sifat fail-closed ganda itu
	 * disengaja: sesi yang tidak dapat dibaca tidak boleh memperoleh kewenangan apa pun, baik
	 * mengelola maupun mengunggah.</p>
	 *
	 * <p><strong>Perhatikan perbedaannya dengan {@link #peserta}.</strong> Field {@link #peserta}
	 * hanya menguji empat tautan dan melewatkan {@code getPesertaKursus()}, sedangkan metode ini
	 * menguji kelimanya. Untuk pengguna berjenis peserta kursus, {@link #peserta} bernilai
	 * {@code false} sementara metode ini bernilai {@code true} — perbedaan yang perlu diperhitungkan
	 * bila salah satunya dipakai menggantikan yang lain.</p>
	 *
	 * <p>Perlu dicatat pula bahwa metode ini menjawab "apakah perannya seorang pelajar", bukan
	 * "apakah yang bersangkutan boleh mengunggah tugas ini sekarang". Kelayakan waktu (jendela
	 * {@code mulai}/{@code selesai}), izin upload ulang pada {@code mhsBolehUploadUlang}, penandaan
	 * {@code mhsYgTidakIkut}, dan pemenuhan {@link SyaratUjian} lewat {@link #syaratAlert} adalah
	 * syarat terpisah yang tetap harus diperiksa.</p>
	 *
	 * @param user entitas Tbmuser yang sedang login, boleh null.
	 * @return {@code true} jika user adalah pelajar yang boleh upload, {@code false} jika
	 *         user adalah pengelola atau user null.
	 */
	private boolean bolehUpload(Tbmuser user) {
		return user != null
				&& (user.getMahasiswa() != null
						|| user.getSiswa() != null
						|| user.getBiodataCalonMahasiswa() != null
						|| user.getCalonSiswa() != null
						|| user.getPesertaKursus() != null);
	}

	/**
	 * Membuat komponen separator visual untuk toolbar.
	 *
	 * <p>Separator berupa garis vertikal tipis (1px) berwarna abu-abu muda yang dirender
	 * sebagai HTML inline. Digunakan untuk memisahkan kelompok tombol dalam toolbar
	 * ({@link MyHboxToolbar}) sehingga pengelompokan fungsional menjadi jelas secara visual:
	 * Berkas Tugas | Nilai | Kelola | Kehadiran.</p>
	 *
	 * <p><strong>Mengapa berupa {@link Html} dan bukan komponen ZK.</strong> ZK 5 menyediakan
	 * {@code Separator}, tetapi orientasi vertikalnya tidak berperilaku konsisten di dalam wadah
	 * ber-{@code display:flex} yang dipakai toolbar ini. Sebuah {@code span} bergaya sebaris jauh
	 * lebih dapat diprediksi: ia ikut melipat bersama tombol ketika {@code flex-wrap} bekerja pada
	 * layar sempit, dan tingginya tetap {@code 24px} tanpa memaksa tinggi baris toolbar.</p>
	 *
	 * <p><strong>Sifat statis dan tanpa state.</strong> Setiap pemanggilan menghasilkan komponen baru;
	 * hasilnya tidak boleh dipakai ulang untuk lebih dari satu posisi karena satu komponen ZK hanya
	 * dapat memiliki satu induk. Pemanggilnya karena itu selalu menulis
	 * {@code createSeparator().setParent(vbox)} sebagai satu kesatuan.</p>
	 *
	 * <p>Dipanggil tiga kali di {@link #createTugas(Tugas, Tabpanel, EventListener, boolean)} sehingga
	 * membagi toolbar menjadi empat kelompok berurutan: Berkas Tugas, Nilai, Kelola, dan Kehadiran.</p>
	 *
	 * @return komponen {@link Html} berisi markup separator siap dipasang ke vbox toolbar.
	 */
	private static Html createSeparator() {
		return new ais.ui.util.MyHtml(
				"<span style='display:inline-block;width:1px;height:24px;"
				+ "background:#cbd5e1;margin:0 6px;vertical-align:middle;opacity:0.7;'></span>");
	}

	/**
	 * Membangun HTML untuk radar/spider chart distribusi rata-rata nilai per Sub-CPMK.
	 *
	 * <p>Menghasilkan string HTML yang berisi SVG polygon radar chart. Setiap sumbu mewakili
	 * satu Sub-CPMK; panjang sumbu sebanding dengan rata-rata nilai peserta pada Sub-CPMK
	 * tersebut relatif terhadap nilai maksimum. Dapat di-render melalui
	 * {@code DashboardUiKit.html()}. Memerlukan minimal 3 data point agar chart bermakna.</p>
	 *
	 * <p><strong>Geometri.</strong> Kanvas SVG berukuran {@code 220x230} dengan titik pusat
	 * {@code (110, 115)} dan jari-jari {@code 80}. Sumbu ke-{@code i} dari {@code n} sumbu diletakkan
	 * pada sudut {@code 2*PI*i/n - PI/2}; pengurangan seperempat putaran membuat sumbu pertama
	 * mengarah lurus ke atas. Empat poligon latar digambar pada 25%, 50%, 75%, dan 100% jari-jari
	 * sebagai garis bantu pembacaan.</p>
	 *
	 * <p><strong>Penskalaan nilai.</strong> Setiap titik data ditempatkan pada jarak
	 * {@code r * min(1, nilai/maxValue)} dari pusat. Pembatasan {@code min(1, ...)} membuat nilai yang
	 * melebihi {@code maxValue} tetap berada di dalam lingkaran terluar alih-alih menembus kanvas —
	 * dengan konsekuensi bahwa dua nilai yang sama-sama melampaui batas akan tampak identik. Nilai
	 * {@code null} diperlakukan sebagai {@code 0.0}, dan {@code maxValue} yang tidak positif membuat
	 * seluruh rasio menjadi {@code 0.0} sehingga poligon mengempis ke titik pusat.</p>
	 *
	 * <p><strong>Format angka.</strong> Seluruh koordinat diformat dengan
	 * {@code java.util.Locale.US} secara eksplisit. Hal ini wajib: pada locale Indonesia pemisah
	 * desimal adalah koma, dan koma pada atribut {@code points} sebuah {@code polygon} adalah pemisah
	 * antar-koordinat — tanpa penguncian locale, SVG yang dihasilkan akan rusak total.</p>
	 *
	 * <p><strong>Pelabelan.</strong> Label sumbu diletakkan {@code 16} piksel di luar lingkaran
	 * terluar, dengan {@code text-anchor} disesuaikan menurut posisi horizontalnya terhadap pusat
	 * ({@code end} di kiri, {@code start} di kanan, {@code middle} di atas atau bawah) agar teks tidak
	 * menabrak grafik. Teks label sudah dipendekkan menjadi maksimal 14 karakter oleh pemanggilnya
	 * sebelum sampai ke sini, dan di sini dilewatkan {@link #escapeXmlAttr(String)}.</p>
	 *
	 * <p><strong>Ambang minimum tiga sumbu.</strong> Radar dengan kurang dari tiga sumbu tidak
	 * membentuk bidang sehingga tidak bermakna; metode mengembalikan string kosong dan pemanggil
	 * memang sudah memeriksa {@code cpmkAvg.size() >= 3} lebih dahulu.</p>
	 *
	 * <p>Metode ini murni menghasilkan teks: tidak menyentuh basis data, tidak membangun komponen ZK,
	 * dan tidak bergantung pada state instance. Warna dan bayangannya memakai variabel CSS Bootstrap
	 * dengan nilai cadangan sehingga tetap terbaca bila tema tidak tersedia.</p>
	 *
	 * @param judul     judul chart yang ditampilkan di atas SVG.
	 * @param deskripsi deskripsi singkat yang muncul di bawah judul.
	 * @param data      map nama-Sub-CPMK ke rata-rata nilai (urutan dipertahankan via LinkedHashMap).
	 * @param maxValue  nilai maksimum yang mungkin (umumnya 100.0).
	 * @return string HTML lengkap siap di-render, atau string kosong jika data kurang dari 3 item.
	 */
	private static String buildRadarChartHtml(String judul, String deskripsi,
			LinkedHashMap<String, Double> data, double maxValue) {
		if (data == null || data.size() < 3) {
			return "";
		}
		int n = data.size();
		String[] keys = data.keySet().toArray(new String[data.size()]);
		Double[] vals = data.values().toArray(new Double[data.size()]);

		int cx = 110, cy = 115;
		double r = 80;

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:var(--bs-body-bg,#fff);border-radius:12px;"
				+ "box-shadow:0 2px 12px rgba(0,0,0,.07);padding:16px 8px 8px 8px;margin:8px 0;'>");
		sb.append("<div style='font-size:13px;font-weight:700;color:var(--bs-heading-color,#1e293b);"
				+ "margin-bottom:4px;padding-left:8px;'>").append(escapeXmlAttr(judul)).append("</div>");
		sb.append("<div style='font-size:11px;color:var(--bs-secondary-color,#64748b);"
				+ "margin-bottom:10px;padding-left:8px;'>").append(escapeXmlAttr(deskripsi)).append("</div>");
		sb.append("<div style='display:flex;justify-content:center;'>");
		sb.append("<svg viewBox='0 0 220 230' width='220' height='230' xmlns='http://www.w3.org/2000/svg'>");

		// Background rings at 25%, 50%, 75%, 100%
		for (int ring = 1; ring <= 4; ring++) {
			double rr = r * ring / 4.0;
			sb.append("<polygon points='");
			for (int i = 0; i < n; i++) {
				double angle = Math.PI * 2.0 * i / n - Math.PI / 2.0;
				double px = cx + rr * Math.cos(angle);
				double py = cy + rr * Math.sin(angle);
				if (i > 0) { sb.append(" "); }
				sb.append(String.format(java.util.Locale.US, "%.1f,%.1f", px, py));
			}
			sb.append("' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
		}

		// Axis lines
		for (int i = 0; i < n; i++) {
			double angle = Math.PI * 2.0 * i / n - Math.PI / 2.0;
			double px = cx + r * Math.cos(angle);
			double py = cy + r * Math.sin(angle);
			sb.append(String.format(java.util.Locale.US,
					"<line x1='%d' y1='%d' x2='%.1f' y2='%.1f' stroke='#e2e8f0' stroke-width='1'/>",
					cx, cy, px, py));
		}

		// Data polygon
		sb.append("<polygon points='");
		for (int i = 0; i < n; i++) {
			double angle = Math.PI * 2.0 * i / n - Math.PI / 2.0;
			double val = vals[i] == null ? 0.0 : vals[i];
			double ratio = maxValue > 0 ? Math.min(1.0, val / maxValue) : 0.0;
			double px = cx + r * ratio * Math.cos(angle);
			double py = cy + r * ratio * Math.sin(angle);
			if (i > 0) { sb.append(" "); }
			sb.append(String.format(java.util.Locale.US, "%.1f,%.1f", px, py));
		}
		sb.append("' fill='rgba(59,130,246,.22)' stroke='#3b82f6' stroke-width='2'/>");

		// Data points + labels
		for (int i = 0; i < n; i++) {
			double angle = Math.PI * 2.0 * i / n - Math.PI / 2.0;
			double val = vals[i] == null ? 0.0 : vals[i];
			double ratio = maxValue > 0 ? Math.min(1.0, val / maxValue) : 0.0;
			double px = cx + r * ratio * Math.cos(angle);
			double py = cy + r * ratio * Math.sin(angle);
			sb.append(String.format(java.util.Locale.US,
					"<circle cx='%.1f' cy='%.1f' r='3.5' fill='#3b82f6' stroke='#fff' stroke-width='1.5'/>",
					px, py));
			// Label axis
			double lx = cx + (r + 16) * Math.cos(angle);
			double ly = cy + (r + 16) * Math.sin(angle);
			String anchor = "middle";
			if (lx < cx - 5) { anchor = "end"; }
			else if (lx > cx + 5) { anchor = "start"; }
			sb.append(String.format(java.util.Locale.US,
					"<text x='%.1f' y='%.1f' text-anchor='%s' dominant-baseline='middle'"
					+ " font-size='7.5' fill='#64748b'>%s</text>",
					lx, ly, anchor, escapeXmlAttr(keys[i])));
		}

		sb.append("</svg></div></div>");
		return sb.toString();
	}

	/**
	 * Melakukan escaping karakter XML/HTML untuk konten teks yang akan dimasukkan ke dalam
	 * atribut atau konten elemen SVG/HTML yang dibuat secara programatik.
	 *
	 * <p><strong>Cakupan dan batasnya.</strong> Empat karakter diganti: {@code &} menjadi
	 * {@code &amp;} (dilakukan lebih dahulu agar entitas hasil penggantian berikutnya tidak ikut
	 * ter-escape dua kali), {@code <} menjadi {@code &lt;}, {@code >} menjadi {@code &gt;}, dan
	 * {@code "} menjadi {@code &quot;}. Tanda kutip tunggal {@code '} <em>tidak</em> diganti.</p>
	 *
	 * <p><strong>Mengapa hal itu perlu diperhatikan.</strong> Meski namanya menyebut "attr",
	 * satu-satunya pemanggilnya —
	 * {@link #buildRadarChartHtml(String, String, LinkedHashMap, double)} — hanya menyisipkan hasilnya
	 * ke dalam <em>isi elemen</em> ({@code <div>}...{@code </div>} dan {@code <text>}...{@code </text>}),
	 * tidak ke dalam nilai atribut. Pada posisi itu kutip tunggal memang tidak berbahaya, sehingga
	 * keadaan saat ini aman. Namun SVG yang dibangun di sana menuliskan seluruh atributnya dengan
	 * kutip <em>tunggal</em>; bila kelak metode ini dipakai untuk mengisi sebuah nilai atribut, teks
	 * yang mengandung {@code '} akan memutus atribut tersebut. Untuk pemakaian semacam itu, tambahkan
	 * penggantian {@code '} menjadi {@code &#39;} lebih dahulu.</p>
	 *
	 * <p>Sumber teks yang dilewatkan ke sini adalah nama {@link FormatNilai} beserta judul dan
	 * deskripsi grafik — data yang berasal dari basis data dan dapat diisi pengguna, sehingga escaping
	 * memang diperlukan dan tidak boleh dilewati.</p>
	 *
	 * @param s string input yang mungkin mengandung karakter khusus XML.
	 * @return string hasil escaping, aman digunakan di dalam tag HTML/SVG.
	 */
	private static String escapeXmlAttr(String s) {
		if (s == null) {
			return "";
		}
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		return s;
	}

}
