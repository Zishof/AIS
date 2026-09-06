package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
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
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.AbsensiSiswaHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.VOSiswa;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.Ambildata;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHashMap;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper utama layar "Hasil Ujian" (modul sekolah) — menampilkan dan mengelola hasil
 * ujian seluruh peserta ({@link Siswa}/{@link CalonSiswa}) untuk satu
 * {@link PertemuanPunyaUjian}. Ini adalah salah satu layar terbesar dan paling banyak
 * fitur di modul {@code ais.action.master.helper}, dengan tiga area utama:
 * <ol>
 * <li><b>Toolbar aksi massal</b> ({@link #display}) — "Ulang Semua" (menghapus seluruh
 * jawaban tersimpan agar peserta dapat mengulang, hanya bila jendela ujian masih
 * berlangsung), "Peserta dianggap hadir" (mengonversi hasil ujian menjadi catatan
 * absensi lewat {@link #ujianDianggapHadir}), "Hitung Ulang Semua" (rekalkulasi nilai
 * seluruh peserta), <b>"Koreksi Otomatis via AI"</b> (mengumpulkan seluruh jawaban esai
 * yang belum dikoreksi lewat {@link KoreksiHasilUjian#kumpulkanEssay}, mengirim prompt ke
 * LLM lewat {@link GenerateAiHelper#panggilAi} di thread terpisah dengan progress bar
 * streaming, lalu menerapkan skor+koreksi hasilnya lewat
 * {@link KoreksiHasilUjian#terapkanKoreksiEssay} dan menghitung ulang nilai), cetak rekap
 * Excel (termasuk kolom jawaban lengkap/belum terjawab), unduh seluruh lampiran jawaban
 * sebagai satu ZIP (disimpan sementara di direktori server
 * {@code /opt/ecampus/lampiran_hasil_ujian_*}), dan unggah lampiran ke Google Drive
 * pengguna ({@link GDriveUtilPerPengguna}).</li>
 * <li><b>Kartu per peserta</b> ({@link DetailPertemuanPunyaUjianRenderer}) — satu baris
 * per peserta menampilkan identitas, waktu mulai/selesai, jumlah pengulangan ujian, sisa
 * waktu pengerjaan (dapat diubah admin lewat HQL bulk update langsung — bukan
 * save-entity — karena getter properti sisa waktu menimpa nilai in-memory dengan cache
 * file), nilai (auto-save dengan indikator ✓/✗), tombol "Hitung Ulang" per peserta,
 * catatan pengawas, kolom "Pelanggaran" (rekap anti-curang: jumlah + log pelanggaran),
 * dan tombol "Reset Ujian" per peserta (admin/guru saja, menghapus seluruh jawaban dan
 * riwayat pengerjaan satu peserta seolah belum pernah ujian). Membuka kartu memicu
 * {@link #tampilRow}, yang mendelegasikan tampilan detail koreksi soal-per-soal ke
 * {@link KoreksiHasilUjian}.</li>
 * <li><b>Tab Statistik</b> ({@link #displayStatistik}) — ringkasan donat chart (HTML/CSS
 * via {@link ais.ui.util.HtmlChartHelper}) untuk kelengkapan jawaban, keikutsertaan
 * ujian, dan akses ujian.</li>
 * </ol>
 *
 * <p>
 * Helper ini dipakai dalam tiga konteks berbeda (menentukan cakupan peserta dan mode
 * baca/tulis): admin/guru melihat SEMUA peserta ({@link #pertemuan} terisi, {@link #siswa}/
 * {@link #calonSiswa} {@code null}); satu siswa melihat hasilnya sendiri; atau satu calon
 * siswa (PMB) melihat hasilnya sendiri.
 * </p>
 *
 * <h3>Hubungan dengan mekanisme ujian mahasiswa — bukan mesin terpisah</h3>
 * <p>
 * Meskipun nama kelas ini memakai kata "Siswa" (domain sekolah) dan bukan "Mahasiswa"
 * (domain perguruan tinggi), <b>tidak ada mesin ujian tersendiri untuk sekolah</b>.
 * Penelusuran memastikan bahwa <b>tidak ada entity {@code HasilUjianSiswa} maupun
 * {@code HasilUjianSiswaDetail}</b> di {@code ais.database.model} maupun
 * {@code ais.database.model.sekolah}. Modul sekolah <b>memakai ulang secara langsung</b>
 * rantai tiga lapis yang sama dengan modul mahasiswa:
 * </p>
 * <ol>
 * <li>{@link ais.database.model.Ujian} — master ujian (jenis soal, aturan tampilan
 * huruf pilihan, dsb.);</li>
 * <li>{@link PertemuanPunyaUjian} — penempelan ujian pada satu {@link Pertemuan}
 * beserta parameter pelaksanaan ({@code jmlDitampilkan}, {@code mulaiUjian},
 * {@code sampaiUjian}, {@code mhsYgTidakIkut});</li>
 * <li>{@link HasilUjianMahasiswa} + {@link HasilUjianMahasiswaDetail} — lembar jawaban
 * satu peserta dan jawaban per soal.</li>
 * </ol>
 * <p>
 * Yang membedakan siswa dari mahasiswa hanyalah <b>kolom peserta mana yang terisi</b> pada
 * {@link HasilUjianMahasiswa}: entity tersebut bersifat polimorfik dengan empat kolom
 * peserta yang saling eksklusif — {@code mahasiswa}, {@code biodataCalonMahasiswa},
 * {@code siswa}, dan {@code calonSiswa} — yang dirangkum menjadi satu kunci alami
 * {@code keyhasil} lewat {@code HasilUjianMahasiswa.genKey(...)} dan diambil lewat
 * {@link HasilUjianMahasiswa#ambilByKey(PertemuanPunyaUjian, ais.database.model.Mahasiswa,
 * ais.database.model.BiodataCalonMahasiswa, Siswa, CalonSiswa)}. Karena itu kelas ini
 * bekerja penuh di atas tipe {@code HasilUjianMahasiswa} walaupun pesertanya siswa; nama
 * variabel lokal {@code hasilUjianMahasiswa}/{@code hasilUjianMahasiswas} di sepanjang
 * berkas ini bukan salah salin-tempel, melainkan memang tipe entity yang benar.
 * </p>
 * <p>
 * Konsekuensinya kelas ini adalah <b>kembaran UI</b> dari
 * {@link HasilUjianMahasiswaHelper} — bukan turunan, bukan pemakainya, melainkan salinan
 * paralel dengan perbedaan pada sumber daftar peserta (daftar hadir kelas sekolah lewat
 * {@link AbsensiSiswaHelper#populateSiswaDariPertemuan(Pertemuan)} / gelombang PSB, bukan
 * daftar peserta perkuliahan), pada label kolom cetak
 * ({@code siswa.nim}/{@code calonSiswa.noRegistrasi} alih-alih padanan mahasiswa), dan
 * pada pemilihan cabang di pemanggilnya ({@code DetailUjianHelper} memilih kelas ini bila
 * {@code pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null},
 * selain itu memakai {@link HasilUjianMahasiswaHelper}). Beberapa bagian bahkan langsung
 * memanggil kembali milik kembarannya, misalnya tombol analisis butir soal
 * ({@code HasilUjianMahasiswaHelper.analsisButirSoal}). Perender detail koreksi
 * soal-per-soal ({@link KoreksiHasilUjian}) dan mesin penilaian pilihan ganda
 * ({@link ProsesUjianHelper#hitungPilihanGanda}) juga dipakai bersama oleh kedua domain.
 * </p>
 *
 * <h3>Catatan otorisasi (fakta arsitektur)</h3>
 * <p>
 * Kelas ini <b>tidak memiliki gerbang otorisasi maupun pemeriksaan kepemilikan sendiri</b>.
 * Tidak ada pemeriksaan cakupan satuan kerja
 * ({@code getMelihatDataSatkerLain()}), tidak ada verifikasi bahwa pengguna yang sedang
 * masuk adalah guru pengampu {@link #pertemuan} tersebut, dan tidak ada pembatasan bahwa
 * peserta yang ditampilkan berada dalam lingkup tenant pengguna. Seluruh kendali akses
 * bertumpu pada layar pemanggil, dan di sana pun bentuknya hanya
 * {@code button.setVisible(...)} — gerbang tingkat tampilan, bukan gerbang tingkat
 * server. Satu-satunya pemeriksaan peran di dalam berkas ini adalah syarat kasar
 * "akun bukan mahasiswa dan bukan siswa"
 * ({@code tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null}) yang
 * memunculkan tombol "Reset Ujian" per peserta. Beberapa tombol destruktif/berdampak luas
 * bahkan tidak diberi {@code setVisible} sama sekali (misalnya "Lampiran ke Drive", yang
 * memindahkan berkas jawaban ke Google Drive pengguna yang sedang masuk lalu
 * <b>menghapus BLOB aslinya</b> dari basis data lewat {@code FileFoto.hapusTotal}) — sama
 * seperti pada kembarannya {@link HasilUjianMahasiswaHelper}, jadi ini konsistensi
 * lintas-kelas, bukan penyimpangan lokal. Fakta ini didokumentasikan agar pembaca tidak
 * mengira ada perlindungan tersembunyi di lapisan helper.
 * </p>
 */
public class HasilUjianSiswaHelper implements DataLoader {

	/**
	 * Ujian yang sedang ditampilkan — lapisan KEDUA dari mekanisme ujian AIS
	 * ({@link ais.database.model.Ujian} = master soal/aturan &rarr;
	 * {@link PertemuanPunyaUjian} = penempelan ujian pada satu pertemuan/jadwal
	 * beserta parameter pelaksanaannya seperti {@code jmlDitampilkan},
	 * {@code mulaiUjian}, {@code sampaiUjian}, {@code mhsYgTidakIkut} &rarr;
	 * {@link HasilUjianMahasiswa} = lembar jawaban satu peserta).
	 *
	 * <p>
	 * Diisi <b>bukan</b> lewat konstruktor melainkan pada awal
	 * {@link #display(PertemuanPunyaUjian, Component)}, sehingga sebelum {@code display}
	 * dipanggil field ini masih {@code null}. {@link #tampilRow} juga dapat
	 * menyinkronkan ulang field ini dari {@link HasilUjianMahasiswa#getPertemuanPunyaUjian()}
	 * baris yang dibuka, sebagai pengaman bila satu instance helper dipakai lintas
	 * beberapa ujian.
	 * </p>
	 */
	private PertemuanPunyaUjian pertemuanPunyaUjian;

	/**
	 * Grid utama tab "Peserta": satu baris {@link Row} per peserta, dirender oleh
	 * {@link DetailPertemuanPunyaUjianRenderer}. Dibuat di awal
	 * {@link #display(PertemuanPunyaUjian, Component)} dan dipasang dengan
	 * {@code pageSize} 1000 (praktis "semua peserta dalam satu halaman") serta paging
	 * di atas dan bawah. Model-nya diisi ulang setiap kali {@link #loadData(Object)}
	 * dijalankan.
	 */
	private MyGrid grid;

	/**
	 * Mode "satu siswa": bila terisi, layar hanya menampilkan hasil ujian milik siswa
	 * ini dan sebagian besar tombol aksi massal disembunyikan (lihat
	 * {@link #display(PertemuanPunyaUjian, Component)}). Bernilai {@code null} pada mode
	 * admin/guru.
	 *
	 * <p>
	 * <b>Catatan penelusuran:</b> konstruktor yang mengisi field ini
	 * ({@link #HasilUjianSiswaHelper(Siswa, CalonSiswa, Pertemuan)}) tidak dipanggil dari
	 * mana pun di basis kode saat dokumentasi ini ditulis; kedua pemanggil nyata
	 * ({@code DetailUjianHelper} dan {@code sekolah.helper.PertemuanPunyaUjianSiswaHelper})
	 * memakai konstruktor mode admin/guru. Jadi cabang-cabang {@code siswa != null} di
	 * kelas ini praktis merupakan jalur cadangan yang belum aktif.
	 * </p>
	 */
	private Siswa siswa;

	/**
	 * Mode "satu calon siswa" (peserta PMB/PSB yang belum menjadi siswa): sepadan dengan
	 * {@link #siswa} tetapi untuk pendaftar. Bernilai {@code null} pada mode admin/guru.
	 * Sama seperti {@link #siswa}, hanya terisi lewat konstruktor tiga-argumen yang
	 * saat ini tidak dipakai pemanggil mana pun.
	 */
	private CalonSiswa calonSiswa;

	/**
	 * Region ZK tempat tab "Statistik" digambar ulang setiap kali
	 * {@link #displayStatistik(int, int, int)} dipanggil (isi lama dibersihkan lebih
	 * dulu dengan {@link Common#clear(Component)}). Dibuat di
	 * {@link #display(PertemuanPunyaUjian, Component)} dan dipasang sebagai anak
	 * borderlayout tab kedua.
	 */
	private Center east;

	/**
	 * Pertemuan (sesi kelas/jadwal) sumber daftar peserta. Diisi lewat konstruktor.
	 * Perannya ada dua:
	 * <ol>
	 * <li>menentukan sumber daftar peserta pada {@link #loadData(Object)} — bila terisi,
	 * daftar hadir kelas diambil lewat
	 * {@link AbsensiSiswaHelper#populateSiswaDariPertemuan(Pertemuan)};</li>
	 * <li>mengaktifkan tombol "Peserta dianggap hadir" (konversi hasil ujian menjadi
	 * catatan absensi lewat {@link #ujianDianggapHadir}) — tombol tersebut hanya tampil
	 * bila field ini tidak {@code null}.</li>
	 * </ol>
	 * Perhatikan bahwa nilai ini bisa berbeda objek dari
	 * {@code pertemuanPunyaUjian.getPertemuan()}; sebagian besar logika justru memakai
	 * yang terakhir.
	 */
	private Pertemuan pertemuan;

	/**
	 * Kotak pencarian pada toolbar (nama/NIM peserta, atau nama/no registrasi/no ujian
	 * calon siswa). Hanya tampil pada mode admin/guru. Nilainya dibaca ulang setiap
	 * {@link #loadData(Object)} dan dipakai sebagai filter {@code ILIKE ANYWHERE} pada
	 * seluruh jalur pengambilan daftar peserta. Kosongnya kotak ini juga menjadi penanda
	 * "muat penuh" yang memicu penulisan ulang cache lokasi hasil ujian
	 * ({@code bersihkanLokasiHasilUjianMahasiswa}/{@code tulisLokasiHasilUjianMahasiswa}).
	 */
	private Textbox nama;

	/**
	 * Membuat helper dalam mode admin/guru: menampilkan hasil ujian SELURUH peserta
	 * {@code pertemuan}. {@link #siswa} dan {@link #calonSiswa} sengaja dikosongkan
	 * eksplisit, karena kedua field itulah yang menjadi saklar mode di sepanjang kelas
	 * ini (mengendalikan visibilitas tombol aksi massal pada
	 * {@link #display(PertemuanPunyaUjian, Component)} dan pemilihan sumber daftar
	 * peserta pada {@link #loadData(Object)}).
	 *
	 * <p>
	 * Inilah satu-satunya konstruktor yang benar-benar dipakai pemanggil
	 * ({@code DetailUjianHelper} dan {@code sekolah.helper.PertemuanPunyaUjianSiswaHelper}).
	 * Perhatikan bahwa {@link #pertemuanPunyaUjian} belum terisi di sini — ujian yang
	 * ditampilkan baru ditentukan saat {@link #display(PertemuanPunyaUjian, Component)}
	 * dipanggil.
	 * </p>
	 *
	 * @param pertemuan pertemuan/sesi kelas sumber daftar peserta; boleh {@code null},
	 *                  yang membuat {@link #loadData(Object)} jatuh ke jalur cadangan
	 *                  "peserta yang sudah punya hasil ujian tersimpan" dan menyembunyikan
	 *                  tombol "Peserta dianggap hadir"
	 */
	public HasilUjianSiswaHelper(Pertemuan pertemuan) {
		this.siswa = null;
		this.calonSiswa = null;
		this.pertemuan = pertemuan;
	}

	/**
	 * Membuat helper dalam mode satu peserta: menampilkan hasil ujian milik
	 * {@code siswa} <i>atau</i> {@code calonSiswa} (secara konvensi hanya salah satu yang
	 * terisi) saja. Dalam mode ini {@link #display(PertemuanPunyaUjian, Component)}
	 * menyembunyikan tombol-tombol aksi massal ("Ulang Semua", "Peserta dianggap hadir",
	 * "Download Lampiran") serta kotak pencarian, dan {@link #loadData(Object)}
	 * memasukkan tepat satu peserta ke daftar.
	 *
	 * <p>
	 * <b>Peringatan pemeliharaan:</b> konstruktor ini <b>tidak dipanggil dari mana pun</b>
	 * di basis kode saat dokumentasi ini ditulis, sehingga seluruh cabang
	 * {@code siswa != null} / {@code calonSiswa != null} di kelas ini merupakan jalur
	 * yang belum pernah dieksekusi di produksi. Bila suatu saat konstruktor ini
	 * diaktifkan (misalnya untuk portal siswa "lihat hasil ujian saya"), perlu diingat
	 * bahwa penyempitan cakupan di sini bersifat <b>penyaringan daftar</b>, bukan gerbang
	 * otorisasi: tombol-tombol yang tidak diberi {@code setVisible} — terutama
	 * "Lampiran ke Drive" dan "Hitung Ulang Semua" — akan ikut tampil dan bekerja atas
	 * data yang termuat. Lihat catatan otorisasi pada Javadoc kelas.
	 * </p>
	 *
	 * @param siswa       siswa yang hasil ujiannya ditampilkan, atau {@code null}
	 * @param calonSiswa  calon siswa (pendaftar PMB/PSB) yang hasil ujiannya
	 *                    ditampilkan, atau {@code null}
	 * @param pertemuan   pertemuan/sesi kelas terkait; lihat
	 *                    {@link #HasilUjianSiswaHelper(Pertemuan)}
	 */
	public HasilUjianSiswaHelper(Siswa siswa, CalonSiswa calonSiswa, Pertemuan pertemuan) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
		this.pertemuan = pertemuan;
	}

	/**
	 * Membangun (menggambar ulang) seluruh isi tab "Statistik" ke dalam region
	 * {@link #east}. Dipanggil dari dalam callback {@link Common#displayLoadBar} milik
	 * {@link #loadData(Object)}, yaitu setelah grid peserta selesai dirender, dengan
	 * angka agregat yang dihitung di sana dari isi {@link #hasilUjianMahasiswas}.
	 *
	 * <h4>Langkah kerja</h4>
	 * <ol>
	 * <li>{@link Common#clear(Component)} pada {@link #east} — tab ini tidak pernah
	 * ditambal sebagian, selalu dibongkar total lalu dibangun ulang, sehingga aman
	 * dipanggil berkali-kali oleh setiap siklus muat ulang.</li>
	 * <li>Membuat {@link MyGrid} dua kolom (label 40% + nilai) berisi deretan
	 * {@link MyFormRow}.</li>
	 * <li>Menyusun tiga blok metrik, masing-masing ditutup satu donat chart HTML/CSS
	 * lewat {@link ais.ui.util.HtmlChartHelper#donut}, dirender sebagai
	 * {@link ais.ui.util.MyHtml} pada baris ber-{@code colspan} 2. Donat HTML/CSS ini
	 * menggantikan pie 3D JFreeChart pada versi terdahulu, sehingga tidak ada lagi
	 * gambar sisi-server yang perlu dibuat, disimpan, dan dibersihkan.</li>
	 * </ol>
	 *
	 * <h4>Tiga blok metrik</h4>
	 * <ol>
	 * <li><b>Kelengkapan Jawaban</b> — "Jumlah Soal" adalah
	 * {@code pertemuanPunyaUjian.getJmlDitampilkan()}, yaitu banyaknya soal yang
	 * ditampilkan ke setiap peserta (bukan jumlah soal di bank soal). "Total Soal" =
	 * {@code jmlDitampilkan * jumlahPeserta}, yakni jumlah slot jawaban ideal bila semua
	 * peserta menjawab semua soal. "Total Terjawab" adalah parameter {@code terjawab},
	 * dan "Total Belum Terjawab" adalah selisihnya. Donat membandingkan terjawab vs
	 * belum.</li>
	 * <li><b>Keikutsertaan Ujian</b> — membandingkan {@code pesertaYgIkutUjian} dengan
	 * {@code jumlahPeserta - pesertaYgIkutUjian}. Perlu dicatat bahwa "ikut ujian" di
	 * sini didefinisikan oleh pemanggil sebagai "punya minimal satu soal terjawab",
	 * bukan "punya {@code mulaiPada} terisi"; peserta yang membuka ujian lalu keluar
	 * tanpa menjawab apa pun tetap terhitung "belum ujian" pada blok ini.</li>
	 * <li><b>Akses Ujian</b> — dihitung ulang di dalam method ini (tidak diterima sebagai
	 * parameter) dari log akses yang disimpan pertemuan:
	 * {@code pertemuanPunyaUjian.getPertemuan().ambilData("ujian_" + id, null)}
	 * mengembalikan {@link TreeMap} berisi jejak siapa saja yang pernah membuka ujian
	 * ini. Penyebutnya adalah {@code jumlahPeserta + jumlah dosen/pengajar pertemuan}
	 * ({@code ambilDosen().size()}), karena pengajar juga tercatat di log akses yang
	 * sama. Karena itu angka "Total peserta yg bisa akses" pada blok ketiga sengaja
	 * lebih besar daripada "Jumlah Peserta" pada blok kedua — bukan ketidakkonsistenan
	 * data.</li>
	 * </ol>
	 *
	 * <h4>Catatan pembulatan dan pembagian</h4>
	 * <p>
	 * Seluruh persentase dihitung sebagai {@code double} murni tanpa pembulatan
	 * eksplisit; pembulatan tampilan sepenuhnya diserahkan kepada
	 * {@code Common.numberFormat}. Pembagi tidak dijaga: bila {@code jumlahPeserta}
	 * bernilai 0 (ujian tanpa peserta terdaftar) atau {@code jmlDitampilkan} bernilai 0,
	 * hasil bagi {@code double} menjadi {@code NaN} (0/0) atau {@code Infinity} sehingga
	 * yang tampil di layar adalah teks non-numerik, bukan pengecualian yang menggagalkan
	 * halaman. Karena tab ini murni informatif dan tidak menulis apa pun ke basis data,
	 * dampaknya terbatas pada tampilan.
	 * </p>
	 * <p>
	 * Variabel {@code persenBelum} dihitung pada ketiga blok tetapi hanya sebagian yang
	 * benar-benar dirender; sisanya sengaja dibiarkan sebagai nilai antara yang tidak
	 * dipakai. Di akhir method, {@code d} dan {@code dsn} dikosongkan lalu di-{@code null}-kan
	 * secara manual — pola pelepasan memori eksplisit yang dipakai konsisten di seluruh
	 * berkas ini karena satu halaman dapat menahan ribuan baris peserta.
	 * </p>
	 *
	 * @param jumlahPeserta      total peserta yang berhak mengikuti ujian, sebagaimana
	 *                           dihitung {@link #loadData(Object)} ke {@link #jumlahPeserta}
	 * @param terjawab           total soal yang sudah terjawab lintas SELURUH peserta
	 *                           (penjumlahan ukuran himpunan id bank soal terjawab)
	 * @param pesertaYgIkutUjian jumlah peserta yang memiliki minimal satu jawaban tersimpan
	 */
	@SuppressWarnings({ "deprecation" })
	private void displayStatistik(int jumlahPeserta, int terjawab, int pesertaYgIkutUjian) {
		Common.clear(east);

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(east);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Soal")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(pertemuanPunyaUjian.getJmlDitampilkan())));

		int totalSoal = pertemuanPunyaUjian.getJmlDitampilkan() * jumlahPeserta;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Soal")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(totalSoal)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Terjawab")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(terjawab)));

		int belum = totalSoal - terjawab;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Belum Terjawab")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belum)));

		Double persen = (100.0 * terjawab) / totalSoal;
		Double persenBelum = (100.0 * belum) / totalSoal;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		// Donut HTML/CSS (HtmlChartHelper) menggantikan pie 3D JFreeChart + penjelasan ringkas.
		row.appendChild(new ais.ui.util.MyHtml(ais.ui.util.HtmlChartHelper.donut(
				"Kelengkapan Jawaban",
				"Menampilkan perbandingan jumlah soal yang telah dijawab dengan yang belum dijawab.",
				new String[] { "Terjawab", "Belum Terjawab" }, new double[] { terjawab, belum },
				new String[] { "#42b72a", "#e4e6eb" }, "terjawab")));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Peserta")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(jumlahPeserta)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta yg melaksanakan ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(pesertaYgIkutUjian)));

		belum = jumlahPeserta - pesertaYgIkutUjian;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta yg belum ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belum)));

		persen = (100.0 * pesertaYgIkutUjian) / jumlahPeserta;
		persenBelum = (100.0 * belum) / jumlahPeserta;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		// Donut HTML/CSS (HtmlChartHelper) menggantikan pie 3D JFreeChart + penjelasan ringkas.
		row.appendChild(new ais.ui.util.MyHtml(ais.ui.util.HtmlChartHelper.donut(
				"Keikutsertaan Ujian",
				"Menampilkan jumlah peserta yang telah mengikuti ujian dibandingkan dengan yang belum mengikuti.",
				new String[] { "Ikut ujian", "Belum ujian" }, new double[] { pesertaYgIkutUjian, belum },
				new String[] { "#1877f2", "#e4e6eb" }, "ikut ujian")));

		TreeMap<String, String> d = pertemuanPunyaUjian.getPertemuan().ambilData("ujian_" + pertemuanPunyaUjian.getId(),
				null);
		List<Dosen> dsn = pertemuanPunyaUjian.getPertemuan().ambilDosen();
		int jumlahTotal = jumlahPeserta + dsn.size();
		int telahAkses1 = d.size();
		int belumAkses1 = jumlahTotal - telahAkses1;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total perserta yg bisa akses")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(jumlahTotal)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perserta yg akses ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(telahAkses1)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perserta yg belum akses")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belumAkses1)));

		persen = (100.0 * telahAkses1) / jumlahTotal;
		persenBelum = (100.0 * belumAkses1) / jumlahTotal;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		// Donut HTML/CSS (HtmlChartHelper) menggantikan pie 3D JFreeChart + penjelasan ringkas.
		row.appendChild(new ais.ui.util.MyHtml(ais.ui.util.HtmlChartHelper.donut(
				"Akses Ujian",
				"Menampilkan jumlah peserta yang telah mengakses ujian dibandingkan dengan yang belum mengakses.",
				new String[] { "Sudah akses", "Belum akses" }, new double[] { telahAkses1, belumAkses1 },
				new String[] { "#1877f2", "#e4e6eb" }, "sudah akses")));

		d.clear();
		d = null;
		dsn.clear();
		dsn = null;
	}

	/**
	 * Membangun layar lengkap "Hasil Ujian" untuk {@code pertemuanPunyaUjian} ke dalam
	 * komponen {@code detail}. Ini adalah titik masuk tunggal kelas ini: pemanggil
	 * membuat helper lewat konstruktor lalu langsung memanggil method ini dengan sebuah
	 * {@code Window} modal sebagai induk. Method ini mengisi {@link #pertemuanPunyaUjian},
	 * menyusun seluruh pohon komponen ZK, dan diakhiri dengan pemanggilan
	 * {@link #loadData(Object)} untuk memuat data awal.
	 *
	 * <h4>Kerangka tata letak</h4>
	 * <p>
	 * Sebuah {@link Borderlayout} dengan tiga region: {@link North} berisi toolbar aksi,
	 * {@link South} berisi tombol "Tutup" (memanggil {@code detail.detach()}), dan
	 * {@link Center} berisi {@link Tabbox} dua tab — "Peserta" (memuat {@link #grid})
	 * dan "Statistik" (memuat {@link #east}, diisi oleh
	 * {@link #displayStatistik(int, int, int)}).
	 * </p>
	 *
	 * <h4>Isi toolbar, berurutan, beserta gerbang visibilitasnya</h4>
	 * <ol>
	 * <li><b>"Ulang Semua"</b> — tampil bila ada pengguna masuk dan helper berada pada
	 * mode admin/guru; <b>di-{@code disable}</b> bila jendela ujian sudah lewat
	 * ({@code masihAdaWaktu} = {@code mulaiUjian} sudah lewat/kosong DAN
	 * {@code sampaiUjian} belum lewat/kosong). Setelah konfirmasi, seluruh
	 * {@link HasilUjianMahasiswaDetail} milik ujian ini dikosongkan
	 * ({@code bankSoalDetail}, {@code jawaban}, {@code waktuJawab} di-{@code null}-kan)
	 * dan setiap {@link HasilUjianMahasiswa} di-{@code reset()}, sehingga semua peserta
	 * dapat mengerjakan ulang. Perhatikan bahwa baris detail <b>tidak dihapus</b>
	 * melainkan dikosongkan isinya — struktur soal per peserta dipertahankan.
	 * Penonaktifan tombol saat waktu habis adalah gerbang <b>tampilan</b>, bukan gerbang
	 * sisi-server.</li>
	 * <li><b>"Rekap Hasil Ujian"</b> (cetak Excel) — dibangun lewat
	 * {@link Common#cetakDataCustomButton} atas {@link HasilUjianMahasiswa} dengan
	 * kriteria {@code keyhasil is not null} dan {@code pertemuanPunyaUjian = ...},
	 * ditambah penyempitan opsional ke {@link #siswa}/{@link #calonSiswa} bila helper
	 * berada pada mode satu peserta (memakai {@code Restrictions.sqlRestriction("true")}
	 * sebagai penanda "tanpa filter"). Empat kolom tambahan dihitung sendiri lewat
	 * {@code EventListener dataAdding}: jumlah soal dikerjakan, jumlah belum dikerjakan,
	 * teks seluruh jawaban ("SOAL;JAWABAN:..."), dan teks soal yang belum dikerjakan;
	 * kedua kolom teks dipangkas ke 20.000 karakter lewat {@code Common.maxPanjang}
	 * karena batas panjang sel Excel. Hanya tampil bila konfigurasi
	 * {@code tampilkan_rekap_hasil_ujian} aktif.</li>
	 * <li><b>"Peserta dianggap hadir"</b> — hanya pada mode admin/guru dan bila
	 * {@link #pertemuan} terisi; mendelegasikan ke
	 * {@link #ujianDianggapHadir(PertemuanPunyaUjian, EventListener)}, lalu menyegarkan
	 * layar pertemuan lewat {@code PertemuanHelper}.</li>
	 * <li><b>Cabang menurut jenis ujian.</b> Bila
	 * {@code ujian.getJenis().equals(BankSoal.PILIHAN_GANDA)}:
	 * <ul>
	 * <li>"Hitung Ulang Semua" — menjalankan {@link ProsesUjianHelper#hitungPilihanGanda}
	 * untuk setiap peserta di {@link #hasilUjianMahasiswas} pada thread latar dengan
	 * indikator persentase, masing-masing dalam sesi Hibernate native tersendiri yang
	 * dibuka dan ditutup per peserta;</li>
	 * <li>tombol analisis butir soal, yang <b>dipinjam langsung</b> dari kembaran
	 * mahasiswa lewat {@code HasilUjianMahasiswaHelper.analsisButirSoal(...)} — bukti
	 * konkret bahwa kedua domain berbagi satu mesin ujian.</li>
	 * </ul>
	 * Selain pilihan ganda (esai/uraian):
	 * <ul>
	 * <li><b>"Koreksi Otomatis via AI"</b> — mengumpulkan jawaban esai yang belum
	 * dikoreksi per peserta lewat {@link KoreksiHasilUjian#kumpulkanEssay}, menyusun
	 * prompt lewat {@code promptKoreksiEssay} + {@code bangunKonteksUjian}, lalu pada
	 * thread latar memanggil {@link GenerateAiHelper#panggilAi} berurutan per peserta dan
	 * menerapkan hasilnya lewat {@link KoreksiHasilUjian#terapkanKoreksiEssay}. Kemajuan
	 * ditampilkan pada jendela modal berisi {@code Progressmeter} dan kotak teks aliran
	 * keluaran LLM, yang disegarkan oleh {@code Timer} 800&nbsp;ms; komunikasi antara
	 * thread pekerja dan timer memakai array satu elemen ({@code done}, {@code selesai},
	 * {@code statusNow}) dan {@link StringBuffer} sebagai penampung aliran. Peserta yang
	 * tidak punya jawaban esai dilewati.</li>
	 * <li>"Hitung Ulang Semua" versi esai — menghitung ulang nilai dari agregat
	 * {@link HasilUjianMahasiswaDetail} (lihat catatan integritas di bawah).</li>
	 * </ul>
	 * </li>
	 * <li><b>"Download Lampiran"</b> — hanya pada mode admin/guru; menyalin seluruh
	 * berkas lampiran jawaban ke direktori sementara sisi server
	 * {@code /opt/ecampus/lampiran_hasil_ujian_<epochMillis>}, mengelompokkannya per
	 * folder berdasarkan 55 karakter pertama teks soal, menamai berkas dengan
	 * {@code NIM_Nama_idLampiran_namaAsli} (di-{@code URLEncoder.encode} agar aman),
	 * lalu memampatkannya menjadi ZIP dan mengirimkannya lewat {@link Filedownload}.
	 * Lampiran yang tersimpan di Google Drive atau berupa tautan tidak disalin isinya,
	 * melainkan ditulis sebagai berkas {@code .txt} berisi URL-nya. Efek samping yang
	 * perlu diketahui: bila sebuah jawaban berlampiran tetapi kolom {@code jawaban}-nya
	 * kosong, kolom itu <b>ditulisi</b> teks "Jawaban terdapat di file terlampir" —
	 * artinya tombol yang secara nama bersifat "unduh" ini juga <b>mengubah data</b>.
	 * Direktori sementara dan ZIP-nya tidak dibersihkan setelah dikirim.</li>
	 * <li><b>"Lampiran ke Drive"</b> — <b>tidak diberi {@code setVisible} sama sekali</b>,
	 * jadi selalu tampil pada setiap mode. Mengunggah seluruh lampiran jawaban ke Google
	 * Drive <b>milik pengguna yang sedang masuk</b> ({@link GDriveUtilPerPengguna} dengan
	 * {@code Common.getCurrentUser()}), lalu untuk setiap berkas yang berhasil terkirim
	 * meng-{@code null}-kan kolom {@code foto}, mengisi {@code gdrive} dengan id berkas
	 * Drive dan {@code gdriveUsername} dengan pengguna tersebut, dan terakhir memanggil
	 * {@code FileFoto.hapusTotal(...)} untuk <b>menghapus BLOB aslinya dari basis data</b>.
	 * Jadi ini operasi pemindahan permanen satu arah, bukan pencadangan. Daftar id
	 * lampiran dirangkai menjadi klausa {@code IN (...)} pada {@code createSQLQuery};
	 * nilai-nilainya berasal dari {@code lampiranLain.getId()} bertipe {@code Long}
	 * sehingga tidak dapat disisipi SQL, namun daftar yang sangat panjang berpotensi
	 * melewati batas panjang pernyataan. Pengiriman berhenti pada kegagalan pertama
	 * ({@code break}).</li>
	 * <li><b>"Soal dan Jawaban"</b> (cetak Excel kedua) — atas
	 * {@link HasilUjianMahasiswaDetail}, memuat huruf pilihan, teks soal, jawaban benar,
	 * nilai, jawaban peserta, catatan koreksi, dan waktu jawab. Tidak diberi gerbang
	 * konfigurasi maupun mode; selalu tampil.</li>
	 * <li><b>"Refresh"</b> — {@code loadData(true)}, yaitu muat ulang dengan pemaksaan
	 * penyegaran cache jawaban.</li>
	 * <li><b>Kotak pencarian {@link #nama} + tombol cari</b> — hanya pada mode admin/guru;
	 * keduanya memanggil {@code loadData(null)} sehingga memuat ulang <b>tanpa</b>
	 * pemaksaan penyegaran.</li>
	 * </ol>
	 *
	 * <h4>Kolom grid tab "Peserta"</h4>
	 * <p>
	 * Sembilan kolom: penanda detail (40px), "Peserta Ujian" (20%), "Waktu Pengerjaan"
	 * (15%), "Lama Pengerjaan" (15%), "Skor/Max" (8%, hanya tampil untuk ujian pilihan
	 * ganda), "Statistik" (15%), "Nilai" (8%), "Keterangan" (sisa), dan "Pelanggaran"
	 * (13%). Grid dipasang {@code mold="paging"} dengan {@code pageSize} 1000 dan paging
	 * ganda (atas dan bawah) — pilihan sadar agar seluruh peserta satu kelas muat dalam
	 * satu halaman; konsekuensinya seluruh baris dirender sekaligus, yang menjadi alasan
	 * pola pelepasan memori eksplisit di sepanjang berkas ini.
	 * </p>
	 *
	 * <h4>Catatan integritas nilai pada "Hitung Ulang Semua" versi esai</h4>
	 * <p>
	 * Rumusnya menjumlahkan {@code (nilai * 100.0) / skor} untuk setiap
	 * {@link HasilUjianMahasiswaDetail}, lalu membaginya dengan jumlah baris detail.
	 * Ada tiga perilaku yang perlu diketahui pemelihara:
	 * </p>
	 * <ul>
	 * <li>Penyebut {@code skor} (dari {@code bankSoal.skor}) tidak dijaga terhadap nilai
	 * nol; soal dengan skor 0 menghasilkan {@code Infinity}/{@code NaN} yang merambat ke
	 * total.</li>
	 * <li>Rata-rata dibagi jumlah baris detail yang <b>ada</b>, bukan jumlah soal yang
	 * ditampilkan ({@code jmlDitampilkan}). Bila sebagian soal belum memiliki baris
	 * detail sama sekali, pembaginya mengecil sehingga nilai akhir naik.</li>
	 * <li>Penulisan hanya terjadi bila {@code sumNilai > 0.1}. Peserta yang seluruh
	 * jawabannya bernilai nol karena itu <b>tidak pernah ditulisi nilai 0</b>; nilai lama
	 * pada entity dibiarkan apa adanya. Ambang yang sama juga dipakai pada tombol "Hitung
	 * Ulang" per peserta, yang di sana justru menampilkan pesan "Hasil ujian siswa belum
	 * Anda koreksi".</li>
	 * </ul>
	 * <p>
	 * Ketiganya bersifat konsisten dengan implementasi kembaran mahasiswa dan
	 * didokumentasikan sebagai perilaku terpasang, bukan sebagai perubahan yang dilakukan
	 * di sini.
	 * </p>
	 *
	 * @param pertemuanPunyaUjian ujian yang hasilnya ditampilkan; nilainya juga disimpan
	 *                            ke field {@link #pertemuanPunyaUjian}
	 * @param detail              komponen ZK induk yang akan diisi tampilan (umumnya
	 *                            {@code Window} modal milik pemanggil)
	 */
	public void display(final PertemuanPunyaUjian pertemuanPunyaUjian, final Component detail) {
		this.pertemuanPunyaUjian = pertemuanPunyaUjian;
		grid = new MyGrid();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(detail);

		North north = new North();
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);
		Tbmuser tbmuser = Common.getCurrentUser();

		boolean rakhasil = Common.bolehKonfigurasi("tampilkan_rekap_hasil_ujian");

		boolean masihAdaWaktu = (pertemuanPunyaUjian.getMulaiUjian() == null
				|| pertemuanPunyaUjian.getMulaiUjian().before(ais.ui.util.WaktuUtil.getDate()))
				&& (pertemuanPunyaUjian.getSampaiUjian() == null
						|| pertemuanPunyaUjian.getSampaiUjian().after(ais.ui.util.WaktuUtil.getDate()));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ulang Semua", "/img/svg/trash.svg");
		button.setVisible(tbmuser != null && siswa == null && calonSiswa == null);
		button.setDisabled(!masihAdaWaktu);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin mengulang semua ujian ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										List<HasilUjianMahasiswa> ujianMahasiswas = session
												.createCriteria(HasilUjianMahasiswa.class)
												.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
												.list();

										for (HasilUjianMahasiswa hasilUjianMahasiswa : ujianMahasiswas) {

											List<HasilUjianMahasiswaDetail> hasilUjianMahasiswaDetails = session
													.createCriteria(HasilUjianMahasiswaDetail.class)
													.add(Restrictions.eq("hasilUjianMahasiswa", hasilUjianMahasiswa))
													.list();
											for (HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail : hasilUjianMahasiswaDetails) {
												hasilUjianMahasiswaDetail.setBankSoalDetail(null);
												hasilUjianMahasiswaDetail.setJawaban(null);
												hasilUjianMahasiswaDetail.setWaktuJawab(null);
												Common.refreshUpdate(session, hasilUjianMahasiswaDetail);
											}
											hasilUjianMahasiswa.reset();
											Common.refreshUpdate(session, hasilUjianMahasiswa);
											session.flush();

										}

										session.flush();

										loadData(null);
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

											}
										});
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});
			}
		});
		button.setParent(toolbar);

		final String[] contents = new String[] {
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "siswa.nim"
						: "calonSiswa.noRegistrasi",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "siswa.nama" : "calonSiswa.nama",
				"pertemuanPunyaUjian", "lamaPengerjaan", "sisaWaktuPengerjaan", "totalNilai-number",
				"jumlahSoal-number", "jawabanBenar-number", "jawabanBenarMax-number", "telahIkutUjian", "nilai-number",
				"lulus", "mulaiPada", "selesaiPada", "jumlahIkut-number", "keyhasil" };

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Jumlah dikerjakan");
		columnHeadersAdding.add("Jumlah Belum dikerjakan");
		columnHeadersAdding.add("Telah dikerjakan");
		columnHeadersAdding.add("Belum dikerjakan");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) objects[0];
				// Long id = (Long) objects[1];
				XSSFRow row = (XSSFRow) objects[2];

				try {

					MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
							hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);

					int total = ujianPunyaSoals.size();
					Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
							.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

					String jawaban = "";
					for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
						for (Long hasilUjianMahasiswaDetailid : aa) {
							try {
								HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
										.ambilData(HasilUjianMahasiswaDetail.class,
												hasilUjianMahasiswaDetailid.toString());
								if (hasilUjianMahasiswaDetail != null) {

									String h = (hasilUjianMahasiswaDetail.getBankSoalDetail() != null
											&& hasilUjianMahasiswaDetail.getUjianPunyaSoal() != null
											&& hasilUjianMahasiswaDetail.getUjianPunyaSoal().getUjian() != null
											&& Boolean.TRUE.equals(hasilUjianMahasiswaDetail.getUjianPunyaSoal().getUjian()
											.getTampilanHurufDiPilihanJawaban())
													? hasilUjianMahasiswaDetail.getBankSoalDetail().getHuruf() + ". "
													: "");

									String j = hasilUjianMahasiswaDetail.getBankSoalDetail() != null
											? (h + hasilUjianMahasiswaDetail.getBankSoalDetail().getJawaban())
											: hasilUjianMahasiswaDetail.getJawaban();

									if (j != null && !j.trim().isEmpty()) {
										j = hasilUjianMahasiswaDetail.getUjianPunyaSoal().getBankSoal().getSoal()
												+ ";JAWABAN:" + j + "\n\n";

										jawaban += jawaban.isEmpty() ? j : "; " + j;
										ujianPunyaSoals.remove(hasilUjianMahasiswaDetail.getUjianPunyaSoal().getId());
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:418");
							}
						}
					}

					int size = ujianPunyaSoals.size();
					row.createCell(contents.length).setCellValue(total - size);
					row.createCell(contents.length + 1).setCellValue(size);

					jawaban = Common.maxPanjang(jawaban, 20000);

					row.createCell(contents.length + 2).setCellValue(jawaban);

					String belum = "";
					for (Long id : ujianPunyaSoals) {
						UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
								.ambilData(UjianPunyaSoal.class, id.toString());
						if (ujianPunyaSoal != null) {
							belum += belum.isEmpty() ? ujianPunyaSoal.getBankSoal().getSoal()
									: "; " + ujianPunyaSoal.getBankSoal().getSoal();
						}
					}

					belum = Common.maxPanjang(belum, 20000);

					row.createCell(contents.length + 3).setCellValue(belum);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:445");
				}
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(HasilUjianMahasiswa.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {
						Session session = HibernateUtil.currentSession();
						return session.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
								.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
								.add(siswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("siswa", siswa))
								.add(calonSiswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("calonSiswa", calonSiswa));
					}
				}, "Rekap Hasil Ujian", "/img/print.png", columnHeadersAdding, dataAdding, contents);
		cetakToolbarbutton.setVisible(tbmuser != null && rakhasil);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Peserta dianggap hadir", "/img/svg/check2.svg");
		masuk.setVisible(pertemuan != null && tbmuser != null && siswa == null && calonSiswa == null);
		masuk.setTooltiptext("Tutup");
		masuk.setParent(toolbar);
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				HasilUjianSiswaHelper.ujianDianggapHadir(pertemuanPunyaUjian, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						new PertemuanHelper().display(pertemuan, new DataLoader() {

							@Override
							public void loadData(Object value) {

							}
						}, 0);
					}
				});

			}
		});

		if (pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA)) {
			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Hitung Ulang Semua", "/img/svg/check2-circle.svg");
			cari.setParent(toolbar);
			cari.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);

						}
					});

					new Thread(new Runnable() {

						@Override
						public void run() {
							try {

							int rowIndex = 1;
							for (Object[] a : hasilUjianMahasiswas.values()) {
								Session session = HibernateUtil.currentNativeSession();
								try {
									HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
									session.refresh(hasilUjianMahasiswa);
									MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
											hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
											new Label(), true);
									Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
											.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(),
													ujianPunyaSoals, false);

									hasilUjianMahasiswa
											.setJumlahSoal(pertemuanPunyaUjian.getJmlDitampilkan() == null ? 0.0
													: pertemuanPunyaUjian.getJmlDitampilkan().doubleValue());
									ProsesUjianHelper.hitungPilihanGanda(hasilUjianMahasiswa,
											hasilUjianMahasiswaDetails);
									hasilUjianMahasiswaDetails = null;

									session.getTransaction().begin();
									Common.refreshUpdate(session, hasilUjianMahasiswa);
									session.getTransaction().commit();

									label.setValue("Sedang memproses data " + hasilUjianMahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / hasilUjianMahasiswas.size())
											+ " %)");
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:542");
								}
								HibernateUtil.closeSession();

								rowIndex++;
							}

							label.setValue("");
													} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
						}
					}).start();

				}
			});

			toolbar.appendChild(HasilUjianMahasiswaHelper.analsisButirSoal(pertemuanPunyaUjian, new Ambildata() {

				@Override
				public Object ambil() {
					return hasilUjianMahasiswas;
				}
			}));

		} else {
			MyToolbarbuttonConfig koreksiAiSemua = new MyToolbarbuttonConfig("Koreksi Otomatis via AI",
					"/img/svg/sparkles.svg");
			koreksiAiSemua.setTooltiptext(
					"Koreksi otomatis SEMUA peserta essay via AI (isi Skor & Koreksi) lalu hitung ulang");
			koreksiAiSemua.setStyle("color:#ffffff;background-color:#7c3aed;border-radius:6px;");
			koreksiAiSemua.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					final java.util.List<Object[]> tugas = new java.util.ArrayList<Object[]>();
					for (Object[] a : hasilUjianMahasiswas.values()) {
						HasilUjianMahasiswa hum = (HasilUjianMahasiswa) a[0];
						java.util.List<Object[]> items = KoreksiHasilUjian.kumpulkanEssay(hum);
						if (items.isEmpty()) {
							continue;
						}
						String nama = "";
						try {
							if (hum.getSiswa() != null && hum.getSiswa().getNama() != null) {
								nama = hum.getSiswa().getNama();
							}
						} catch (Exception e) {
						}
						tugas.add(new Object[]{ hum.getId(), nama, KoreksiHasilUjian.promptKoreksiEssay(items, KoreksiHasilUjian.bangunKonteksUjian(hum)), items });
					}
					if (tugas.isEmpty()) {
						MyMessageboxConfig.show("Tidak ada jawaban essay untuk dikoreksi.", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					final org.zkoss.zul.Window win = new org.zkoss.zul.Window("Koreksi Otomatis via AI", "normal",
							false);
					win.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					win.setWidth("560px");
					org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
					vb.setStyle("padding:16px;");
					vb.setHflex("1");
					vb.setParent(win);
					final org.zkoss.zul.Label statusLbl = new org.zkoss.zul.Label("Menyiapkan...");
					statusLbl.setStyle("font-size:13px;color:#1e40af;font-weight:bold;");
					vb.appendChild(statusLbl);
					final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter(0);
					meter.setWidth("100%");
					vb.appendChild(meter);
					final org.zkoss.zul.Textbox streamBox = new org.zkoss.zul.Textbox();
					streamBox.setMultiline(true);
					streamBox.setReadonly(true);
					streamBox.setRows(8);
					streamBox.setHflex("1");
					streamBox.setStyle("width:100%;margin-top:10px;font-family:monospace;font-size:11px;");
					vb.appendChild(streamBox);
					win.doHighlighted();

					final int total = tugas.size();
					final int[] done = { 0 };
					final boolean[] selesai = { false };
					final StringBuffer sink = new StringBuffer();
					final String[] statusNow = { "" };

					new Thread(new Runnable() {
						@Override
						@SuppressWarnings("unchecked")
						public void run() {
							for (int i = 0; i < tugas.size(); i++) {
								Object[] t = tugas.get(i);
								statusNow[0] = "Mengoreksi " + (i + 1) + "/" + total
										+ (((String) t[1]).length() > 0 ? " — " + t[1] : "");
								sink.setLength(0);
								try {
									String resp = GenerateAiHelper.panggilAi((String) t[2], sink, 2048);
									KoreksiHasilUjian.terapkanKoreksiEssay((java.util.List<Object[]>) t[3], resp,
											(Long) t[0]);
								} catch (Exception e) {
									ais.common.ErrorAuditUtil.record(e, "HasilUjianSiswaHelper.koreksiAiSemua");
								}
								done[0] = i + 1;
							}
							selesai[0] = true;
						}
					}).start();

					final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer(800);
					timer.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.setRepeats(true);
					timer.addEventListener("onTimer", new EventListener() {
						@Override
						public void onEvent(Event evtTimer) throws Exception {
							try {
								meter.setValue(total > 0 ? (int) (done[0] * 100L / total) : 100);
								statusLbl.setValue(statusNow[0]);
								String cur = sink.toString();
								if (!cur.equals(streamBox.getValue())) {
									streamBox.setValue(cur);
								}
							} catch (Exception ig) {
							}
							if (selesai[0]) {
								timer.stop();
								timer.detach();
								win.detach();
								loadData(true);
								MyMessageboxConfig.show(
										total + " peserta selesai dikoreksi via AI. Nilai dihitung ulang.",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
					timer.start();
				}
			});
			koreksiAiSemua.setParent(toolbar);

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Hitung Ulang Semua",
					"/img/Button-Refresh-icon.png");
			cari.setTooltiptext("Hitung Ulang Semua");
			cari.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);

						}
					});

					new Thread(new Runnable() {

						@Override
						public void run() {
							try {

							int rowIndex = 1;
							for (Object[] a : hasilUjianMahasiswas.values()) {
								Session session = HibernateUtil.currentNativeSession();
								try {
									HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
									List<Object[]> sumNilais = session.createCriteria(HasilUjianMahasiswaDetail.class)
											.createAlias("bankSoal", "bankSoal")
											.add(Restrictions.eq("hasilUjianMahasiswa", hasilUjianMahasiswa))
											.setProjection(
													Projections.projectionList().add(Projections.property("nilai"))
															.add(Projections.property("bankSoal.skor")))
											.list();

									Double sumNilai = 0.0;
									for (Object[] o : sumNilais) {
										Double nilai = ((Number) o[0]).doubleValue();
										Double skor = ((Number) o[1]).doubleValue();
										sumNilai += (nilai * 100.0) / skor;
									}

									System.out.println("sumNilais = " + sumNilais + ", sumNilai = " + sumNilai);

									if (sumNilai != null && sumNilai.doubleValue() > 0.1) {

										Double n = sumNilai.doubleValue() / sumNilais.size();

										hasilUjianMahasiswa.setNilai(n);

										session.getTransaction().begin();
										Common.refreshUpdate(session, hasilUjianMahasiswa);
										session.getTransaction().commit();

									}
									label.setValue("Sedang memproses data " + hasilUjianMahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / hasilUjianMahasiswas.size())
											+ " %)");
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:628");
								}
								HibernateUtil.closeSession();

								rowIndex++;
							}

							label.setValue("");
													} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
						}
					}).start();

				}

			});
			cari.setParent(toolbar);
		}

		button = new MyToolbarbuttonConfig("Download Lampiran", FileFoto.icon(null));
		button.setVisible(tbmuser != null && siswa == null && calonSiswa == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						File fileFolderLampiran = new File("/opt/ecampus/lampiran_hasil_ujian_"
								+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
						fileFolderLampiran.mkdirs();
						System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());
						Session session = HibernateUtil.currentSession();
						for (Object[] a : hasilUjianMahasiswas.values()) {
							HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
							Siswa siswa = hasilUjianMahasiswa.getSiswa();

							MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
									hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(),
									true);

							Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
									.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(),
											ujianPunyaSoals);

							for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
								for (Long hasilUjianMahasiswaDetailid : aa) {
									HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													hasilUjianMahasiswaDetailid.toString());
									if (hasilUjianMahasiswaDetail != null) {
										BankSoal bankSoal = hasilUjianMahasiswaDetail.getBankSoal();
										for (int i = 0; i < bankSoal.getJumlahLampiran(); i++) {
											LampiranLain lampiranLain = LampiranLain
													.ambil(hasilUjianMahasiswaDetail.getId(), "Jawaban ke-" + (i + 1));

											if (lampiranLain != null) {

												if (hasilUjianMahasiswaDetail.getJawaban().trim().isEmpty()) {
													hasilUjianMahasiswaDetail
															.setJawaban("Jawaban terdapat di file terlampir");
													Common.refreshUpdate(session, hasilUjianMahasiswaDetail);
												}

												File fileFoto = lampiranLain.ambilFile();

												File folder = new File(fileFolderLampiran.getAbsolutePath() + "/"
														+ URLEncoder.encode((bankSoal.getSoal().length() > 55
																? bankSoal.getSoal().substring(0, 55)
																: bankSoal.getSoal()), "UTF-8"));
												folder.mkdirs();

												if (lampiranLain.getGdrive() != null
														&& !lampiranLain.getGdrive().trim().isEmpty()) {
													fileFoto = new File(folder.getAbsolutePath() + "/"
															+ URLEncoder.encode(siswa.getNim() + "_" + siswa.getNama(),
																	"UTF-8")
															+ "_" + lampiranLain.getId() + "_" + fileFoto.getName()
															+ ".txt");
													ais.common.BacaTulisUtil.tulis(fileFoto,
															lampiranLain.forwardGDriveUrl());
												} else if (lampiranLain.getLink() != null
														&& !lampiranLain.getLink().trim().isEmpty()) {
													fileFoto = new File(folder.getAbsolutePath() + "/"
															+ URLEncoder.encode(siswa.getNim() + "_" + siswa.getNama(),
																	"UTF-8")
															+ "_" + lampiranLain.getId() + "_" + fileFoto.getName()
															+ ".txt");
													ais.common.BacaTulisUtil.tulis(fileFoto,
															lampiranLain.getLink().trim());
												} else {
													File fileCopy = new File(folder.getAbsolutePath() + "/"
															+ URLEncoder.encode(siswa.getNim() + "_" + siswa.getNama(),
																	"UTF-8")
															+ "_" + lampiranLain.getId() + "_" + fileFoto.getName());
													System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
													FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
													FileInputStream fileInputStream = new FileInputStream(fileFoto);
													IOUtils.copyLarge(fileInputStream, fileOutputStream);
													fileInputStream.close();
													fileOutputStream.close();
												}
											}
										}
									}
								}
							}

						}

						File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
						Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
						Filedownload.save(fileFolderLampiranZip, "application/zip");

					}
				}, "Harap tunggu.. sedang melakukan proses download lampiran..");
			}
		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig drive = new MyToolbarbuttonConfig("Lampiran ke Drive", FileFoto.icon("drive.google"));
		drive.setParent(toolbar);
		drive.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final Tbmuser tbmuser = Common.getCurrentUser();

				final List<Long> tugasFileContents = new ArrayList<Long>();

				for (Object[] a : hasilUjianMahasiswas.values()) {
					HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];

					MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
							hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);

					Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
							.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

					for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
						for (Long hasilUjianMahasiswaDetailid : aa) {
							HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetailid.toString());
							if (hasilUjianMahasiswaDetail != null) {
								BankSoal bankSoal = hasilUjianMahasiswaDetail.getBankSoal();
								for (int i = 0; i < bankSoal.getJumlahLampiran(); i++) {
									LampiranLain lampiranLain = LampiranLain.ambil(hasilUjianMahasiswaDetail.getId(),
											"Jawaban ke-" + (i + 1));

									if (lampiranLain != null) {
										tugasFileContents.add(lampiranLain.getId());
									}
								}
							}
						}
					}
				}

				if (tugasFileContents.isEmpty()) {
					MyMessageboxConfig.show("Tidak ada file ujian yang bisa dikirim ke google drive", "Peringatan",
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

												String tableName = "lampiran_lain";
												String colFotoName = "foto";

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
																.createCriteria(LampiranLain.class)
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
																								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:910");
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

		String[] contents1 = new String[] {
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "id"
						: "hasilUjianMahasiswa.calonSiswa.noRegistrasi",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "hasilUjianMahasiswa.siswa.nim"
						: "hasilUjianMahasiswa.calonSiswa.noUjian",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "hasilUjianMahasiswa.siswa.nama"
						: "hasilUjianMahasiswa.calonSiswa.nama",
				"bankSoalDetail.huruf", "bankSoal.soal-text", "bankSoalDetail.jawaban", "bankSoalDetail.betul", "nilai",
				"jawaban", "koreksi", "waktuJawab", "hasilUjianMahasiswa", "hasilUjianMahasiswa.keyhasil" };
		cetakToolbarbutton = Common.cetakDataCustomButton(HasilUjianMahasiswaDetail.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(HasilUjianMahasiswaDetail.class)
						.createAlias("hasilUjianMahasiswa", "hasilUjianMahasiswa")
						.add(Restrictions.eq("hasilUjianMahasiswa.pertemuanPunyaUjian", pertemuanPunyaUjian));

				if (order) {
					criteria.addOrder(Order.asc("hasilUjianMahasiswa")).addOrder(Order.asc("id"));
				}
				return criteria;
			}
		}, "Soal dan Jawaban", "/img/print.png", contents1);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(true);
			}
		});

		nama = new Textbox();
		nama.setVisible(siswa == null && calonSiswa == null);
		nama.setCols(7);
		nama.setParent(toolbar);
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.setVisible(siswa == null && calonSiswa == null);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				detail.detach();
			}
		});
		cancel.setParent(toolbar);

		east = new Center();
		east.setBorder("none");

		Center center = new Center();
		center.setParent(borderlayout);
		center.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("15000px");
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabSoal = new MyTabConfig("Peserta", "/img/svg/user-group.svg");
		tabs.appendChild(tabSoal);
		MyTabConfig tabPeserta = new MyTabConfig("Statistik", "/img/svg/chart-line-light.svg");
		tabs.appendChild(tabPeserta);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel parent = new ais.ui.util.MyTabpanel();
		parent.setHeight("15000px");
		parent.setParent(tabpanels);

		Borderlayout borderlayoutLagi = new Borderlayout();
		borderlayoutLagi.setParent(parent);

		Center centerLagi = new Center();
		centerLagi.setParent(borderlayoutLagi);
		centerLagi.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(centerLagi, true);

		parent = new ais.ui.util.MyTabpanel();
		parent.setHeight("15000px");
		parent.setParent(tabpanels);

		borderlayoutLagi = new Borderlayout();
		borderlayoutLagi.setParent(parent);

		east.setParent(borderlayoutLagi);
		ais.ui.util.ZkCompat.setFlex(east, true);

		grid.setParent(centerLagi);

		grid.setSclass("fgrid ais-data-grid");
		grid.setHeight("15000px");
		grid.setWidth("100%");
		grid.setMold("paging");
		// Tampilkan SEMUA peserta dalam satu halaman (page size besar).
		grid.setPageSize(1000);
		grid.getPagingChild().setMold("os");
		// Paging tampil di ATAS dan BAWAH tabel (sebelumnya hanya "top").
		grid.setPagingPosition("both");
		try {
			grid.getPagingChild().setDetailed(true);
		} catch (Exception ignorePg) { ais.common.ErrorAuditUtil.record(ignorePg, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianSiswaHelper.java:1082");
		}

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig("Peserta Ujian");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Waktu Pengerjaan");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Lama Pengerjaan");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Skor/Max");
		column.setParent(columns);
		column.setWidth("8%");
		column.setVisible(pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA));

		column = new MyColumnConfig("Statistik");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Nilai");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);

		column = new MyColumnConfig("Pelanggaran");
		column.setParent(columns);
		column.setWidth("13%");

		loadData(null);

	}

	/**
	 * Menampilkan detail koreksi soal-per-soal satu peserta ke dalam panel
	 * {@code detail}. Dipicu oleh peristiwa {@code onOpen} pada {@link MyDetail} milik
	 * kartu peserta, yaitu ketika pengguna membentangkan sebuah baris grid.
	 *
	 * <p>
	 * Sebelum mendelegasikan, method ini <b>menyinkronkan ulang</b>
	 * {@link #pertemuanPunyaUjian} dari {@code tempHasilUjianMahasiswa.getPertemuanPunyaUjian()}
	 * bila keduanya tersedia. Ini pengaman untuk kasus satu instance helper dipakai
	 * berpindah-pindah antar ujian: tanpa sinkronisasi tersebut, {@link KoreksiHasilUjian}
	 * dapat menerima ujian yang berbeda dari pemilik lembar jawaban yang sedang dibuka,
	 * sehingga jumlah soal yang ditampilkan ({@code jmlDitampilkan}) dan pemetaan soal
	 * ke jawaban bisa meleset.
	 * </p>
	 * <p>
	 * Seluruh isi panel — daftar soal, jawaban peserta, kunci jawaban, kotak skor dan
	 * catatan koreksi per soal, serta tombol koreksi AI per peserta — dibangun
	 * sepenuhnya oleh {@link KoreksiHasilUjian#display}; kelas ini tidak menggambar apa
	 * pun sendiri di sini. {@link KoreksiHasilUjian} itu sendiri dipakai bersama oleh
	 * domain siswa dan mahasiswa, karena keduanya beroperasi pada entity
	 * {@link HasilUjianMahasiswa} yang sama.
	 * </p>
	 *
	 * @param detail                    panel tempat detail koreksi dirender
	 * @param tempHasilUjianMahasiswa   lembar jawaban peserta yang akan dikoreksi/dilihat;
	 *                                  bila {@code null}, sinkronisasi dilewati dan
	 *                                  {@link KoreksiHasilUjian#display} tetap dipanggil
	 */
	public void tampilRow(final MyDetail detail, final HasilUjianMahasiswa tempHasilUjianMahasiswa) {
		if (tempHasilUjianMahasiswa != null && tempHasilUjianMahasiswa.getPertemuanPunyaUjian() != null) {
			pertemuanPunyaUjian = tempHasilUjianMahasiswa.getPertemuanPunyaUjian();
		}
		new KoreksiHasilUjian().display(detail, tempHasilUjianMahasiswa, pertemuanPunyaUjian);
	}



	/**
	 * Perender kartu grid untuk satu peserta ujian ({@link Siswa} atau
	 * {@link CalonSiswa}): foto+identitas dengan riwayat revisi, waktu mulai/selesai,
	 * jumlah pengulangan ("Ikut ujian ... kali", dapat diedit), lama pengerjaan, sisa
	 * waktu pengerjaan yang dapat diedit admin (disimpan via HQL bulk update langsung
	 * ke database — BUKAN save-entity biasa — karena getter properti
	 * {@code sisaWaktuPengerjaan} sengaja menimpa nilai in-memory dengan cache "live"
	 * dari file, sehingga save-entity biasa akan kehilangan perubahan admin saat
	 * Hibernate memanggil getter tersebut ketika dirty-checking), nilai auto-save
	 * dengan indikator status, tombol "Hitung Ulang" nilai satu peserta, catatan
	 * pengawas auto-save, ringkasan pelanggaran anti-curang (jumlah + log), dan tombol
	 * "Reset Ujian" (admin/guru saja) yang menghapus seluruh jawaban dan riwayat
	 * pengerjaan peserta tersebut secara permanen (dengan konfirmasi). Membuka detail
	 * kartu ({@code onOpen}) memicu {@link #tampilRow}.
	 */
	public class DetailPertemuanPunyaUjianRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Menggambar satu baris grid untuk satu peserta. Dipanggil ZK sekali per baris
		 * setiap kali model grid diganti oleh {@link #loadData(Object)}, dan dipanggil
		 * ulang secara rekursif oleh beberapa aksi di dalamnya (perubahan data hasil
		 * ujian dan "Reset Ujian") setelah membersihkan baris dengan
		 * {@link Common#clear(Component)}.
		 *
		 * <h4>Pemetaan model ke data</h4>
		 * <p>
		 * {@code arg1} adalah elemen model, bertipe {@link Siswa} atau {@link CalonSiswa}
		 * (keduanya turunan {@link VOSiswa}). Lembar jawabannya <b>tidak</b> diambil ulang
		 * dari basis data di sini, melainkan dicari di peta {@link #hasilUjianMahasiswas}
		 * dengan kunci id peserta — peta itu diisi oleh kolam thread di
		 * {@link #loadData(Object)}. Karena pengisian peta berjalan asinkron sementara
		 * grid sudah dirender, baris yang datanya belum selesai dihitung akan
		 * mendapatkan {@code null} dan method ini <b>keluar lebih awal</b> tanpa
		 * menggambar apa pun; baris tersebut baru terisi pada siklus render berikutnya.
		 * Ekspresi ternary bertingkat pemilihan kunci di awal method mengandung dua
		 * cabang terakhir yang tidak pernah tercapai (duplikat dari dua cabang pertama) —
		 * sisa penyederhanaan yang tidak berdampak pada hasil.
		 * </p>
		 * <p>
		 * Isi peta adalah {@code Object[]} dua elemen: indeks 0 berisi
		 * {@link HasilUjianMahasiswa}, indeks 1 berisi {@code Set<Long>} berisi id bank
		 * soal yang sudah terjawab peserta tersebut (dipakai untuk kolom "Statistik").
		 * </p>
		 *
		 * <h4>Isi kartu, berurutan sesuai kolom</h4>
		 * <ol>
		 * <li><b>Detail</b> — {@link MyDetail} dengan pendengar {@code onOpen} yang
		 * memanggil {@link #tampilRow(MyDetail, HasilUjianMahasiswa)}. Pendengar yang
		 * sama juga menangani peristiwa bermuatan {@link HasilUjianMahasiswa}, yang
		 * diartikan sebagai "data berubah, gambar ulang baris ini".</li>
		 * <li><b>Peserta Ujian</b> — foto kecil ({@link CommonMedia#tampilkanGambarKecil})
		 * dan blok riwayat revisi ({@link RevisiHelper#createNewRevisi} atas
		 * {@link HasilUjianMahasiswa}) berisi NIM/no registrasi, lalu nama peserta.</li>
		 * <li><b>Waktu Pengerjaan</b> — tanggal ujian, jam mulai, dan jam selesai; setiap
		 * label hanya muncul bila nilainya terisi.</li>
		 * <li><b>Lama Pengerjaan</b> — berisi beberapa kendali sekaligus: jumlah
		 * pengulangan ujian ("Ikut ujian ... kali", {@link Intbox} yang langsung
		 * menyimpan lewat {@code Common.refreshUpdate}); durasi pengerjaan yang
		 * <b>hanya bagian jamnya</b> yang diformat, karena
		 * {@code getLamaPengerjaan()} dibentuk dari {@code GregorianCalendar(0,0,0,...)}
		 * sehingga bagian tanggalnya tidak bermakna; sisa waktu pengerjaan
		 * ({@link Timebox}, lihat catatan khusus di bawah); nomor soal terakhir yang
		 * dikerjakan, disimpan ke penyimpanan bebas entity lewat
		 * {@code put(nilai, "index")} dengan pergeseran satu (tampil 1-berbasis, tersimpan
		 * 0-berbasis).</li>
		 * <li><b>Skor/Max</b> — {@code jawabanBenar} dan {@code jawabanBenarMax};
		 * kolomnya hanya kasatmata untuk ujian pilihan ganda, tetapi labelnya tetap
		 * dibuat pada semua jenis ujian.</li>
		 * <li><b>Statistik</b> — jumlah soal, terjawab, dan belum terjawab beserta
		 * persentasenya. Baris juga diberi warna latar: kemerahan bila pengerjaan
		 * sebagian (0&nbsp;&lt;&nbsp;persen&nbsp;&lt;&nbsp;100) dan kehijauan bila 100%.
		 * Perbandingan memakai {@code persen.intValue()} sehingga pengerjaan yang
		 * membulat ke bawah menjadi 0% (misalnya 1 dari 200 soal) tidak diberi warna.
		 * Kolom ini juga memuat kotak centang "Lengkapi ulang jawaban".</li>
		 * <li><b>Nilai</b> — untuk pilihan ganda hanya label baca-saja; untuk esai berupa
		 * {@link MyDoublebox} yang menyimpan otomatis pada {@code onChange} (mengambil
		 * ulang entity lewat {@code Restrictions.idEq} lalu {@code session.update}) dengan
		 * indikator ✓/✗ yang dipudarkan lewat JavaScript sisi klien, ditambah tombol
		 * "Hitung Ulang" satu peserta.</li>
		 * <li><b>Keterangan</b> — catatan pengawas ({@link MyTextbox}) dengan pola simpan
		 * otomatis dan indikator ✓/✗ yang sama.</li>
		 * <li><b>Pelanggaran</b> — rekap pengawasan anti-curang: jumlah pelanggaran
		 * (merah bila &gt; 0, hijau "0 (bersih)" bila tidak ada) dan cuplikan
		 * {@code logPelanggaran} yang dipangkas 400 karakter di layar namun ditampilkan
		 * utuh sebagai tooltip. Kolom ini juga menampung tombol "Reset Ujian".</li>
		 * </ol>
		 *
		 * <h4>Mengapa "Sisa Waktu" dan "Lengkapi ulang jawaban" memakai HQL bulk update</h4>
		 * <p>
		 * Kedua kendali ini <b>tidak</b> disimpan dengan pola biasa
		 * (setter lalu {@code Common.refreshUpdate}), melainkan lewat
		 * {@code update HasilUjianMahasiswa set ... where id = :id} langsung. Alasannya
		 * tercatat pada komentar di badan method: pemetaan Hibernate entity ini berbasis
		 * akses <b>properti</b>, sehingga saat dirty-check/flush Hibernate memanggil
		 * {@code getSisaWaktuPengerjaan()}; getter tersebut <b>sengaja menimpa</b> nilai
		 * in-memory dengan cache "live" dari berkas lewat {@code retreive()}, yang pada
		 * saat itu masih berisi nilai LAMA karena penulisan cache berkas ({@code put()})
		 * baru terjadi sesudahnya. Akibatnya perubahan admin selalu batal tersimpan
		 * ("admin tidak bisa menambah waktu ujian, kembali ke 0 setelah refresh"). Bulk
		 * update melewati pemuatan entity dan pemanggilan getter, sehingga nilai yang
		 * tersimpan pasti sesuai masukan admin. Ini adalah wujud konkret <b>pola getter
		 * yang memutasi field</b> yang tercatat sistemik di {@code ais/database/model/} —
		 * di sini dampaknya sudah ditambal di sisi pemanggil, bukan di getter-nya.
		 * Urutan pada penangan {@code onChange} penting dan disengaja: bulk update lebih
		 * dulu, baru {@code put()} ke cache berkas, baru setter in-memory.
		 * </p>
		 *
		 * <h4>Gerbang pada tombol "Reset Ujian"</h4>
		 * <p>
		 * Satu-satunya pemeriksaan peran di dalam berkas ini:
		 * {@code tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null},
		 * yaitu "akun bukan mahasiswa dan bukan siswa". Ini gerbang kasar berbasis
		 * <b>jenis akun</b>, bukan berbasis kepemilikan — tidak ada verifikasi bahwa
		 * pengguna adalah guru pengampu pertemuan ini, tidak ada penyempitan satuan kerja,
		 * dan gerbangnya hanya menyembunyikan tombol di tampilan. Perhatikan pula bahwa
		 * akun {@code calonSiswa} tidak termasuk yang dikecualikan. Aksinya sendiri
		 * bersifat merusak dan tanpa jejak audit: seluruh {@link HasilUjianMahasiswaDetail}
		 * peserta dikosongkan, entity utama di-{@code reset()}, dan
		 * {@code sisaWaktuPengerjaan}, {@code jumlahPelanggaran}, serta
		 * {@code logPelanggaran} di-{@code null}-kan — termasuk menghapus bukti
		 * pelanggaran anti-curang. Berbeda dengan tombol "Ulang Semua", aksi ini
		 * <b>tidak</b> memeriksa apakah jendela ujian masih berlangsung. Transaksinya
		 * memakai sesi Hibernate tersendiri ({@code openSession}) dengan
		 * {@code rollback} pada kegagalan dan penutupan sesi di blok {@code finally}.
		 * </p>
		 *
		 * @param arg0 baris grid yang akan diisi komponen
		 * @param arg1 elemen model: {@link Siswa} atau {@link CalonSiswa}
		 * @throws Exception diteruskan dari pembangunan komponen ZK
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, final Object arg1) throws Exception {
			// Kartu peserta hasil ujian (foto+nama dari Vbox/Hbox ZK). Tandai dengan
			// ais-peserta-row agar CSS meratakan latar kotak bersarang -> hover tanpa garis putih.
			arg0.setSclass("ais-peserta-row");

			Siswa siswa = (arg1 instanceof Siswa) ? (Siswa) arg1 : null;
			CalonSiswa calonSiswa = (arg1 instanceof CalonSiswa) ? (CalonSiswa) arg1 : null;

			Object[] s = siswa != null ? hasilUjianMahasiswas.get(siswa.getId())
					: calonSiswa != null ? hasilUjianMahasiswas.get(calonSiswa.getId())
							: siswa != null ? hasilUjianMahasiswas.get(siswa.getId())
									: calonSiswa != null ? hasilUjianMahasiswas.get(calonSiswa.getId()) : null;

			if (s == null) {
				return;
			}

			final HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) s[0];

			final HasilUjianMahasiswa tempHasilUjianMahasiswa = hasilUjianMahasiswa;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					detail.setAttribute("eventListener", this);

					if (event != null && event.getData() != null && event.getData() instanceof HasilUjianMahasiswa) {
						Common.clear(arg0);
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								render(arg0, arg1);
							}
						});
					} else {
						tampilRow(detail, tempHasilUjianMahasiswa);
					}

				}
			};

			detail.addEventListener("onOpen", eventListener);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			if (siswa != null) {
				CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox);
			} else if (calonSiswa != null) {
				CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(hbox);
			}

			Vbox vb = RevisiHelper.createNewRevisi(HasilUjianMahasiswa.class, hasilUjianMahasiswa,
					siswa == null ? calonSiswa.getNoRegistrasi() : siswa.getNim());
			vb.setParent(hbox);

			vb.appendChild(new Label(siswa == null ? calonSiswa.getNama() : siswa.getNama()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			if (hasilUjianMahasiswa.getMulaiPada() != null) {
				new MyLabelKecil("Ujian tgl : " + (hasilUjianMahasiswa.getMulaiPada() == null ? ""
						: Common.dateFormat6.get().format(hasilUjianMahasiswa.getMulaiPada()))).setParent(vbox);
			}

			if (hasilUjianMahasiswa.getMulaiPada() != null) {
				new MyLabelKecil("waktu mulai : " + (hasilUjianMahasiswa.getMulaiPada() == null ? ""
						: Common.timeFormat.get().format(hasilUjianMahasiswa.getMulaiPada()))).setParent(vbox);
			}
			if (hasilUjianMahasiswa.getSelesaiPada() != null) {
				new MyLabelKecil("waktu selesai : " + (hasilUjianMahasiswa.getSelesaiPada() == null ? ""
						: Common.timeFormat.get().format(hasilUjianMahasiswa.getSelesaiPada()))).setParent(vbox);
			}

			vbox = new Vbox();
			vbox.setParent(arg0);

			Hbox hb = new Hbox();
			hb.setParent(vbox);

			new MyLabelKecil("Ikut ujian").setParent(hb);
			final Intbox ikut = new Intbox(hasilUjianMahasiswa.getJumlahIkut());
			ikut.setCols(1);
			ikut.setStyle("font-size:9px;");
			ikut.setParent(hb);
			new MyLabelKecil("kali").setParent(hb);
			ikut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					hasilUjianMahasiswa.setJumlahIkut(ikut.getValue());
					Common.refreshUpdate(hasilUjianMahasiswa);
				}
			});

			// Lama Waktu = DURASI pengerjaan (getLamaPengerjaan dibuat via GregorianCalendar(0,0,0,jam,menit,detik)
			// -> bagian tanggalnya ngawur "31-12-0002"). Format bagian WAKTU-nya saja: HH:mm:ss (timeFormat1).
			new MyLabelKecil("Lama Waktu : " + (hasilUjianMahasiswa.getLamaPengerjaan() == null ? ""
					: Common.timeFormat1.get().format(hasilUjianMahasiswa.getLamaPengerjaan()))).setParent(vbox);

			final Timebox sisaWaktu = new ais.ui.util.MyTimebox(hasilUjianMahasiswa.getSisaWaktuPengerjaan());
			sisaWaktu.setStyle("font-size:9px;");
			sisaWaktu.setDisabled(hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null);
			sisaWaktu.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (sisaWaktu.getValue() != null) {
						// FIX (laporan: admin tidak bisa menambah waktu ujian, kembali ke 0 setelah
						// refresh) — pola lama (set lalu refreshUpdate) memicu Hibernate MEMANGGIL
						// getter getSisaWaktuPengerjaan() saat dirty-check/flush (mapping berbasis
						// PROPERTY access); getter itu SENGAJA menimpa nilai in-memory dengan cache
						// file "live" (retreive()) yang masih menyimpan nilai LAMA (sinkron ke file
						// baru terjadi SETELAH ini, lewat put() di bawah) -> nilai admin batal
						// tersimpan. FIX: UPDATE langsung via HQL bulk update (tanpa memuat entity /
						// memanggil getter) supaya nilai yang tersimpan pasti sesuai input admin.
						try {
							HibernateUtil.currentSession().createQuery(
									"update HasilUjianMahasiswa set sisaWaktuPengerjaan = :v where id = :id")
									.setParameter("v", sisaWaktu.getValue())
									.setParameter("id", hasilUjianMahasiswa.getId()).executeUpdate();
						} catch (Exception e) {
							ais.common.ErrorAuditUtil.record(e, "HasilUjianSiswaHelper sisaWaktu onChange");
						}
						hasilUjianMahasiswa.put(Common.databaseDateFormat1.get().format(sisaWaktu.getValue()));
						hasilUjianMahasiswa.setSisaWaktuPengerjaan(sisaWaktu.getValue());
					}
				}
			});

			hb = new Hbox();
			hb.setParent(vbox);
			new MyLabelKecil("Sisa Waktu").setParent(hb);
			sisaWaktu.setParent(hb);
			sisaWaktu.setCols(4);

			new Label(Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenar())
					+ (hasilUjianMahasiswa.getJawabanBenarMax() == null ? ""
							: " / " + Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenarMax())))
					.setParent(arg0);

			hb = new Hbox();
			hb.setParent(vbox);
			new MyLabelKecil("Nomor Terakhir").setParent(hb);
			int startIndex = 0;
			try {
				String ss = hasilUjianMahasiswa.retreive("index");
				if (ss != null && !ss.trim().isEmpty()) {
					startIndex = Integer.parseInt(ss.trim());
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1282");
			}

			final MyIntbox startIndexInput = new MyIntbox(startIndex + 1);
			startIndexInput.setStyle("font-size:8px;");
			startIndexInput.setCols(2);
			startIndexInput.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					hasilUjianMahasiswa.put(
							(startIndexInput.getValue() == null ? 0 : (startIndexInput.getValue() - 1)) + "", "index");
				}
			});
			startIndexInput.setParent(hb);

			int totalSoal = pertemuanPunyaUjian.getJmlDitampilkan();

			Set<Long> idsa = (Set<Long>) s[1];
			int terjawab = idsa.size();

			int belum = totalSoal - terjawab;

			Double persen = (100.0 * terjawab) / totalSoal;
			Double persenBelum = (100.0 * belum) / totalSoal;

			vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil("Jml Soal : " + Common.numberFormat.get().format(totalSoal)).setParent(vbox);
			new MyLabelAgakKecil("Soal Terjawab : " + Common.numberFormat.get().format(terjawab) + " / "
					+ Common.numberFormat.get().format(persen) + "%").setParent(vbox);
			new MyLabelAgakKecil("Soal Belum Terjawab : " + Common.numberFormat.get().format(belum) + " / "
					+ Common.numberFormat.get().format(persenBelum) + "%").setParent(vbox);

			if (persen.intValue() > 0 && persen.intValue() < 100) {
				arg0.setStyle("background-color: rgba(205,92,92,0.4);");
			} else if (persen.intValue() == 100) {
				arg0.setStyle("background:#eeffeb;");
			}

			final MyCheckboxConfig lengkapiJawaban = new MyCheckboxConfig(
					"Lengkapi ulang jawaban (pilihan ini tidak aktif kembali ketika peserta telah ujian ulang)");
			lengkapiJawaban.setStyle("font-size:8px;");
			lengkapiJawaban.setChecked(hasilUjianMahasiswa.getLengkapiJawaban());
			lengkapiJawaban.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// FIX (laporan: checklist ini kembali hilang setelah refresh) — pola sama dengan
					// perbaikan "Sisa Waktu" di atas: pakai HQL bulk update supaya nilai yang
					// tersimpan pasti sesuai pilihan admin.
					try {
						HibernateUtil.currentSession().createQuery(
								"update HasilUjianMahasiswa set lengkapiJawaban = :v where id = :id")
								.setParameter("v", lengkapiJawaban.isChecked())
								.setParameter("id", hasilUjianMahasiswa.getId()).executeUpdate();
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "HasilUjianSiswaHelper lengkapiJawaban onClick");
					}
					hasilUjianMahasiswa.setLengkapiJawaban(lengkapiJawaban.isChecked());
				}
			});
			lengkapiJawaban.setParent(vbox);

			if (pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA)) {

				new Label(Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai()) + "").setParent(arg0);
			} else {

				hbox = new Hbox();
				hbox.setParent(arg0);

				final MyDoublebox doublebox = new MyDoublebox();
				doublebox.setCols(3);
				doublebox.setValue(tempHasilUjianMahasiswa.getNilai());
				doublebox.setParent(hbox);
				final org.zkoss.zul.Label lblAutoNilaiSiswa = new org.zkoss.zul.Label("");
				lblAutoNilaiSiswa.setStyle("color:green;font-size:13px;font-weight:bold;");
				lblAutoNilaiSiswa.setParent(hbox);
				doublebox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Session session = HibernateUtil.currentSession();
							HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) session
									.createCriteria(HasilUjianMahasiswa.class)
									.add(Restrictions.idEq(tempHasilUjianMahasiswa.getId())).uniqueResult();
							hasilUjianMahasiswa.setNilai(doublebox.getValue());
							session.update(hasilUjianMahasiswa);
							lblAutoNilaiSiswa.setStyle("color:green;font-size:13px;font-weight:bold;");
							lblAutoNilaiSiswa.setValue("✓");
							lblAutoNilaiSiswa.setTooltiptext("Tersimpan");
							org.zkoss.zk.ui.util.Clients.evalJavaScript(
								"(function(){var e=document.getElementById('" + lblAutoNilaiSiswa.getUuid() + "');" +
								"if(!e)return;e.style.transition='none';e.style.opacity='1';" +
								"setTimeout(function(){e.style.transition='opacity 0.8s';e.style.opacity='0';},2500);" +
								"})();"
							);
						} catch (Exception eSave) {
							lblAutoNilaiSiswa.setValue("✗");
							lblAutoNilaiSiswa.setStyle("color:red;font-size:13px;font-weight:bold;");
							lblAutoNilaiSiswa.setTooltiptext("Gagal simpan: " + eSave.getMessage());
							ais.common.ErrorAuditUtil.record(eSave, "auto-audit HasilUjianSiswaHelper onChange nilai auto-save");
						}
					}
				});

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang",
						"/img/Button-Refresh-icon.png");
				button.setOrient("vertical");
				button.setTooltiptext("Hitung Ulang");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyArrayList<Long> ujianPunyaSoals = tempHasilUjianMahasiswa.ambilUjianPunyaSoals(
								tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(),
								true);
						MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = tempHasilUjianMahasiswa
								.ambilHasilUjianMahasiswaDetail(true,
										tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
										new Label(), ujianPunyaSoals);

						Double sumNilai = 0.0;
						for (Long ujianPunyaSoalid : ujianPunyaSoals) {
							UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
									.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
							if (ujianPunyaSoal != null) {
								BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
								Set<Long> hasilUjianMahasiswaDetails = hasilUjianMahasiswaDetailsa
										.get(bankSoal.getId());
								if (hasilUjianMahasiswaDetails != null && !hasilUjianMahasiswaDetails.isEmpty()) {
									HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													hasilUjianMahasiswaDetails.iterator().next().toString());
									if (hasilUjianMahasiswaDetail != null) {
										Double nilai = hasilUjianMahasiswaDetail.getNilai();
										Double skor = bankSoal.getSkor();
										sumNilai += (nilai * 100.0) / skor;
									}
								}
							}
						}

						System.out.println("ujianPunyaSoals = " + ujianPunyaSoals + ", sumNilai = " + sumNilai);

						if (sumNilai != null && sumNilai.doubleValue() > 0.1) {

							Double n = sumNilai.doubleValue() / ujianPunyaSoals.size();

							tempHasilUjianMahasiswa.setNilai(n);

							Session session = HibernateUtil.currentNativeSession();
							try {
								session.getTransaction().begin();
								Common.refreshUpdate(session, tempHasilUjianMahasiswa);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1422");
							}
							HibernateUtil.closeSession();
							doublebox.setValue(n);

						} else {
							MyMessageboxConfig.show("Hasil ujian siswa belum Anda koreksi", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}
					}

				});
				button.setParent(hbox);

			}

			final MyTextbox keterangan = new MyTextbox(tempHasilUjianMahasiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);
			org.zkoss.zul.Hbox hboxKet = new org.zkoss.zul.Hbox();
			hboxKet.setParent(arg0);
			keterangan.setParent(hboxKet);
			final org.zkoss.zul.Label lblAutoKetSiswa = new org.zkoss.zul.Label("");
			lblAutoKetSiswa.setStyle("color:green;font-size:13px;font-weight:bold;");
			lblAutoKetSiswa.setParent(hboxKet);

			// === Kolom Pelanggaran (rekap pengawasan ujian / anti-curang) ===
			int jmlLgr = tempHasilUjianMahasiswa.getJumlahPelanggaran() == null ? 0
					: tempHasilUjianMahasiswa.getJumlahPelanggaran().intValue();
			Vbox vboxLgr = new Vbox();
			vboxLgr.setParent(arg0);
			Label lblJmlLgr = new Label(jmlLgr > 0 ? (jmlLgr + " pelanggaran") : "0 (bersih)");
			lblJmlLgr.setStyle(jmlLgr > 0 ? "color:#b91c1c;font-weight:bold;" : "color:#16a34a;");
			lblJmlLgr.setParent(vboxLgr);
			String logLgr = tempHasilUjianMahasiswa.getLogPelanggaran();
			if (logLgr != null && !logLgr.trim().isEmpty()) {
				Label lblLogLgr = new Label(logLgr.length() > 400 ? logLgr.substring(0, 400) + " ..." : logLgr);
				lblLogLgr.setMultiline(true);
				lblLogLgr.setPre(true);
				lblLogLgr.setStyle("font-size:10px;color:#64748b;white-space:pre-wrap;");
				lblLogLgr.setTooltiptext(logLgr);
				lblLogLgr.setParent(vboxLgr);
			}

			// === Tombol Reset Ujian per peserta (admin/guru saja) ===
			final String namaPesertaReset = siswa != null ? siswa.getNama()
					: calonSiswa != null ? calonSiswa.getNama() : "peserta";
			Tbmuser tbmuserCurrent = Common.getCurrentUser();
			if (tbmuserCurrent != null && tbmuserCurrent.getMahasiswa() == null
					&& tbmuserCurrent.getSiswa() == null) {
				Vbox vboxReset = new Vbox();
				vboxReset.setParent(arg0);
				MyToolbarbuttonConfig btnReset = new MyToolbarbuttonConfig("Reset Ujian", "/img/svg/trash.svg");
				btnReset.setTooltiptext("Reset ujian " + namaPesertaReset + " — seolah belum pernah mengikuti ujian sama sekali");
				btnReset.setStyle("color:#b91c1c;");
				btnReset.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event onClickEvent) throws Exception {
						MyMessageboxConfig.show(
							"Yakin mereset ujian " + namaPesertaReset + "?\n\nSemua jawaban dan riwayat pengerjaan akan dihapus.",
							"Konfirmasi Reset Ujian",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event okEvent) throws Exception {
									int pilihan = Integer.parseInt(okEvent.getData().toString());
									if (pilihan == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {
											@SuppressWarnings("unchecked")
											@Override
											public void onEvent(Event timerEvent) throws Exception {
												org.hibernate.Session sess = null;
												org.hibernate.Transaction tx = null;
												try {
													sess = HibernateUtil.getSessionFactory().openSession();
													tx = sess.beginTransaction();
													// 1. Hapus semua jawaban detail
													java.util.List<HasilUjianMahasiswaDetail> details = sess
															.createCriteria(HasilUjianMahasiswaDetail.class)
															.add(org.hibernate.criterion.Restrictions.eq("hasilUjianMahasiswa", tempHasilUjianMahasiswa))
															.list();
													for (HasilUjianMahasiswaDetail hmd : details) {
														hmd.setBankSoalDetail(null);
														hmd.setJawaban(null);
														hmd.setWaktuJawab(null);
														sess.update(hmd);
													}
													// 2. Reset entitas utama
													HasilUjianMahasiswa humRefresh = (HasilUjianMahasiswa) sess.get(
															HasilUjianMahasiswa.class, tempHasilUjianMahasiswa.getId());
													if (humRefresh != null) {
														humRefresh.reset();
														humRefresh.setSisaWaktuPengerjaan(null);
														humRefresh.setJumlahPelanggaran(null);
														humRefresh.setLogPelanggaran(null);
														sess.update(humRefresh);
													}
													tx.commit();
													// 3. Reload baris
													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event reloadEvent) throws Exception {
															Common.clear(arg0);
															render(arg0, arg1);
														}
													});
												} catch (Exception ex) {
													if (tx != null) tx.rollback();
													ais.common.ErrorAuditUtil.record(ex,
															"auto-audit resetUjian HasilUjianSiswaHelper id=" + tempHasilUjianMahasiswa.getId());
													MyMessageboxConfig.show("Gagal reset: " + ex.getMessage(),
															"Error", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												} finally {
													if (sess != null && sess.isOpen()) {
														try { sess.close(); } catch (Exception ex) {
															ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianSiswaHelper.java:resetUjian");
														}
													}
												}
											}
										});
									}
								}
							});
					}
				});
				btnReset.setParent(vboxReset);
			}

			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						tempHasilUjianMahasiswa.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(tempHasilUjianMahasiswa);
						lblAutoKetSiswa.setStyle("color:green;font-size:13px;font-weight:bold;");
						lblAutoKetSiswa.setValue("✓");
						lblAutoKetSiswa.setTooltiptext("Tersimpan");
						org.zkoss.zk.ui.util.Clients.evalJavaScript(
							"(function(){var e=document.getElementById('" + lblAutoKetSiswa.getUuid() + "');" +
							"if(!e)return;e.style.transition='none';e.style.opacity='1';" +
							"setTimeout(function(){e.style.transition='opacity 0.8s';e.style.opacity='0';},2500);" +
							"})();"
						);
					} catch (Exception eSave) {
						lblAutoKetSiswa.setValue("✗");
						lblAutoKetSiswa.setStyle("color:red;font-size:13px;font-weight:bold;");
						lblAutoKetSiswa.setTooltiptext("Gagal simpan: " + eSave.getMessage());
						ais.common.ErrorAuditUtil.record(eSave, "auto-audit HasilUjianSiswaHelper onChange keterangan auto-save");
					}
				}
			});

		}
	}
	
	/**
	 * Peta hasil ujian per peserta: <b>kunci</b> adalah id peserta ({@link Siswa} atau
	 * {@link CalonSiswa}) — bukan id lembar jawaban — dan <b>nilainya</b> adalah
	 * {@code Object[]} dua elemen:
	 * <ul>
	 * <li>indeks 0: {@link HasilUjianMahasiswa}, lembar jawaban peserta tersebut;</li>
	 * <li>indeks 1: {@code Set<Long>} berisi id bank soal yang sudah terjawab, hasil
	 * {@link HasilUjianMahasiswa#ambilBankSoalIdTerjawab}.</li>
	 * </ul>
	 *
	 * <p>
	 * Dibuat ulang sebagai {@link java.util.concurrent.ConcurrentHashMap} pada setiap
	 * {@link #loadData(Object)}. Tipe konkuren dipilih secara sadar (lihat komentar
	 * "PERBAIKAN 2") karena peta ini <b>ditulisi oleh kolam thread latar</b> sementara
	 * pada saat yang sama dibaca oleh benang UI lewat
	 * {@link DetailPertemuanPunyaUjianRenderer#render(Row, Object)} dan oleh penangan
	 * tombol-tombol aksi massal pada toolbar.
	 * </p>
	 * <p>
	 * <b>Konsekuensi yang perlu diketahui pemelihara:</b> {@code ConcurrentHashMap}
	 * menjamin keamanan struktur peta, bukan kelengkapan isinya pada saat dibaca. Grid
	 * dirender sebelum kolam thread selesai, sehingga baris yang datanya belum masuk
	 * mendapat {@code null} dan dilewati; demikian pula tombol aksi massal yang ditekan
	 * terlalu cepat akan bekerja atas sebagian peserta saja tanpa peringatan. Peta ini
	 * juga menahan referensi entity Hibernate hidup untuk seluruh peserta satu kelas
	 * sekaligus, yang menjadi alasan pola pelepasan memori eksplisit di sepanjang berkas
	 * ini.
	 * </p>
	 */
	private Map<Long, Object[]> hasilUjianMahasiswas = null;

	/**
	 * Daftar peserta yang menjadi model {@link #grid} pada siklus muat terakhir.
	 * Bertipe {@link VOSiswa}, yaitu antarmuka bersama {@link Siswa} dan
	 * {@link CalonSiswa}, sehingga satu daftar dapat memuat campuran keduanya (jalur
	 * cadangan {@link #loadData(Object)} memang menggabungkan calon siswa dan siswa).
	 *
	 * <p>
	 * Komentar "PERBAIKAN 1" pada berkas mencatat bahwa tipe generiknya sengaja
	 * dipastikan {@code <VOSiswa>} alih-alih {@code <? extends VOSiswa>} agar
	 * {@code addAll} dari berbagai sumber dapat dilakukan langsung.
	 * </p>
	 * <p>
	 * <b>Peringatan siklus hidup:</b> daftar ini di-{@code clear()} oleh timer bawaan
	 * segera setelah grid selesai dirender, sebagai pelepasan memori. Namun thread latar
	 * yang mengisi {@link #hasilUjianMahasiswas} <b>juga</b> melakukan iterasi atas
	 * daftar yang sama ({@code for (VOSiswa voSiswa : siswasTemorary)}) dan membaca
	 * {@code siswasTemorary.size()} untuk menghitung persentase kemajuan. Karena
	 * {@link ArrayList} bukan struktur konkuren, pengosongan yang bersamaan dengan
	 * iterasi tersebut berpotensi memunculkan
	 * {@link java.util.ConcurrentModificationException} — yang pada praktiknya tertelan
	 * oleh blok {@code catch} pembungkus thread latar sehingga muncul sebagai "sebagian
	 * peserta tidak tampil datanya" alih-alih galat yang kasatmata.
	 * </p>
	 */
	// PERBAIKAN 1: Gunakan tipe pasti <VOSiswa>, hilangkan '? extends'
	private List<VOSiswa> siswasTemorary = null;

	/**
	 * Jumlah peserta yang dipakai sebagai <b>penyebut</b> statistik pada
	 * {@link #displayStatistik(int, int, int)}. Perlu dicatat bahwa maknanya
	 * <b>berbeda-beda menurut jalur pengambilan data</b> di {@link #loadData(Object)},
	 * dan ini disengaja:
	 * <ul>
	 * <li>mode satu siswa — jumlah peserta perkuliahan/kelas terkait
	 * ({@code ambilJumlahDetailperkuliahan()}), sehingga statistik tetap relatif
	 * terhadap seluruh kelas walaupun yang tampil hanya satu baris;</li>
	 * <li>mode satu calon siswa — jumlah baris {@link HasilUjianMahasiswa} bercalon
	 * siswa yang sudah tersimpan untuk ujian ini;</li>
	 * <li>seluruh jalur mode admin/guru — sekadar {@code siswasTemorary.size()},
	 * yaitu banyaknya baris yang benar-benar ditampilkan.</li>
	 * </ul>
	 * Karena pada jalur admin/guru penyebut ini mengikuti hasil <b>pencarian</b>,
	 * persentase pada tab "Statistik" ikut menyempit ketika kotak pencarian
	 * {@link #nama} terisi — statistik yang tampil adalah statistik atas hasil saring,
	 * bukan atas seluruh kelas.
	 */
	private int jumlahPeserta = 0;

	/**
	 * Memuat ulang daftar peserta dan hasil ujian mereka ke {@link #grid}, lalu
	 * menghitung dan menggambar ulang tab "Statistik". Implementasi kontrak
	 * {@link DataLoader}. Dipanggil di akhir
	 * {@link #display(PertemuanPunyaUjian, Component)} dan oleh hampir semua aksi
	 * toolbar yang mengubah data.
	 *
	 * <h4>Tahap 1 — menyiapkan wadah</h4>
	 * <p>
	 * {@link #hasilUjianMahasiswas} dibuat ulang sebagai
	 * {@link java.util.concurrent.ConcurrentHashMap} dan {@link #siswasTemorary} sebagai
	 * {@link ArrayList} kosong. Nilai kotak pencarian {@link #nama} dibaca sekali ke
	 * variabel efektif-final {@code searchValue}; kosongnya kotak tersebut
	 * ({@code isSearchEmpty}) kemudian dipakai di dua tempat berbeda: sebagai penanda
	 * "tanpa filter" pada kriteria, dan sebagai penanda "muat penuh" yang memicu
	 * penulisan ulang cache lokasi hasil ujian.
	 * </p>
	 *
	 * <h4>Tahap 2 — lima jalur pengambilan daftar peserta</h4>
	 * <ol>
	 * <li><b>Mode satu siswa</b> ({@link #siswa} terisi) — daftar berisi tepat satu
	 * elemen; {@link #jumlahPeserta} justru diambil dari jumlah peserta perkuliahan agar
	 * statistik tetap relatif terhadap kelas.</li>
	 * <li><b>Mode satu calon siswa</b> ({@link #calonSiswa} terisi) — daftar berisi satu
	 * elemen; {@link #jumlahPeserta} dihitung lewat {@code rowCount} atas
	 * {@link HasilUjianMahasiswa} yang bercalon siswa untuk ujian ini.</li>
	 * <li><b>Gelombang PSB/PMB</b> — bila
	 * {@code pertemuan.getJadwalUjianPSB().getGelombangPendaftaranPsb()} terisi, daftar
	 * diambil dari seluruh {@link CalonSiswa} pada gelombang tersebut, diurutkan menurut
	 * {@code nomorInduk}. Inilah satu-satunya jalur yang menampilkan peserta yang
	 * <b>belum</b> punya lembar jawaban sama sekali.</li>
	 * <li><b>Jadwal ujian PSB tanpa gelombang</b> — daftar diambil dari calon siswa yang
	 * <b>sudah</b> punya {@link HasilUjianMahasiswa} untuk ujian ini
	 * ({@code keyhasil is not null}), lewat {@code groupProperty("calonSiswa.id")}.</li>
	 * <li><b>Daftar hadir kelas</b> — bila pertemuan ada tetapi bukan jadwal ujian PSB,
	 * peserta diambil dari {@link AbsensiSiswaHelper#populateSiswaDariPertemuan}, lalu
	 * disaring dua kali: menurut kata kunci pencarian (dicocokkan di memori, bukan di
	 * basis data — perhatikan perbedaan semantik dengan jalur lain yang memakai
	 * {@code ILIKE}), dan menurut daftar pengecualian {@code mhsYgTidakIkut}. Daftar
	 * pengecualian itu berupa <b>teks berisi id yang dipisah koma</b> dan diuji dengan
	 * {@code contains("," + id + ",")}; karena itu id yang berada di posisi paling depan
	 * atau paling belakang hanya terdeteksi bila teksnya diapit koma di kedua ujungnya —
	 * ketergantungan format yang perlu dijaga oleh penulis data tersebut.</li>
	 * <li><b>Jalur cadangan umum</b> (tanpa pertemuan) — menggabungkan calon siswa dan
	 * siswa yang sudah punya hasil ujian untuk ujian ini menjadi satu daftar campuran.</li>
	 * </ol>
	 * <p>
	 * Pencarian diterapkan pada seluruh jalur, dengan bidang yang berbeda menurut jenis
	 * peserta: nama/no registrasi/no ujian untuk calon siswa, nama/NIM untuk siswa.
	 * Jalur yang tidak memakai filter menyisipkan {@code Restrictions.sqlRestriction("true")}
	 * sebagai penanda "tanpa syarat" agar rantai {@code add(...)} tetap seragam.
	 * </p>
	 * <p>
	 * <b>Catatan cakupan (fakta arsitektur):</b> tidak satu pun dari jalur di atas
	 * menyempitkan hasil menurut satuan kerja, tenant, atau kepemilikan pengajar. Yang
	 * membatasi hanyalah {@code pertemuanPunyaUjian} yang diterima dari pemanggil.
	 * Konsisten dengan catatan otorisasi pada Javadoc kelas.
	 * </p>
	 *
	 * <h4>Tahap 3 — render grid dan statistik</h4>
	 * <p>
	 * Dibungkus {@link Common#displayLoadBar} agar pengguna melihat indikator proses.
	 * Model grid diisi {@link SimpleListModel} atas {@link #siswasTemorary} dengan
	 * perender {@link DetailPertemuanPunyaUjianRenderer}. Setelah itu total soal
	 * terjawab dan jumlah peserta yang "ikut ujian" dihitung dari isi
	 * {@link #hasilUjianMahasiswas} yang <b>sudah tersedia pada saat itu</b>, lalu
	 * diteruskan ke {@link #displayStatistik(int, int, int)}. Karena pengisian peta
	 * berjalan asinkron di tahap 4, statistik yang pertama kali tampil dapat lebih kecil
	 * dari kenyataan sampai pengguna menekan "Refresh". Terakhir, sebuah timer bawaan
	 * mengosongkan {@link #siswasTemorary} sebagai pelepasan memori.
	 * </p>
	 *
	 * <h4>Tahap 4 — pengisian data secara paralel</h4>
	 * <p>
	 * Bila kotak pencarian kosong (dianggap "muat penuh"), cache lokasi hasil ujian pada
	 * {@link PertemuanPunyaUjian} dibersihkan dan ditulis ulang menjadi objek JSON
	 * kosong lebih dahulu, sehingga pemetaan peserta ke lembar jawaban dibangun ulang
	 * dari nol. Sesudahnya sebuah thread membuka kolam berukuran
	 * {@code ais.common.DbThreadPool.safe(50)} — plafon aman terhadap ukuran kolam
	 * koneksi c3p0 — dan untuk setiap peserta menjalankan tugas yang: mengambil lembar
	 * jawaban lewat {@link HasilUjianMahasiswa#ambilByKey}, memperbarui cache lokasi bila
	 * sedang muat penuh, mengambil daftar soal yang ditampilkan, dan akhirnya menaruh
	 * pasangan {lembar jawaban, himpunan id bank soal terjawab} ke
	 * {@link #hasilUjianMahasiswas}. Kemajuan dilaporkan lewat
	 * {@link java.util.concurrent.atomic.AtomicInteger} agar penghitungnya aman
	 * lintas-thread. Kolam ditutup lalu ditunggu hingga selesai tanpa batas waktu
	 * ({@code Long.MAX_VALUE} nanodetik).
	 * </p>
	 * <p>
	 * Setiap kegagalan per peserta ditangkap dan dicatat lewat
	 * {@code ais.common.ErrorAuditUtil}, sehingga satu peserta bermasalah tidak
	 * menggagalkan seluruh muat; efeknya baris peserta itu tampil kosong. Peserta yang
	 * idnya sudah ada di peta dilewati, yang membuat method ini aman dipanggil berulang.
	 * </p>
	 *
	 * @param value parameter kontrak {@link DataLoader}. Bila bernilai {@link Boolean}
	 *              {@code true}, diteruskan sebagai bendera {@code refresh} ke
	 *              {@link HasilUjianMahasiswa#ambilBankSoalIdTerjawab} sehingga cache
	 *              jawaban dihitung ulang alih-alih dibaca dari simpanan. {@code null}
	 *              diperlakukan sama dengan {@code false}. Nilai non-{@link Boolean}
	 *              akan memicu {@link ClassCastException}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void loadData(Object value) {
		// PERBAIKAN 2: Gunakan ConcurrentHashMap agar aman diakses oleh Thread Background
		hasilUjianMahasiswas = new java.util.concurrent.ConcurrentHashMap<Long, Object[]>();
		siswasTemorary = new ArrayList<VOSiswa>();

		final Boolean refresh = (Boolean) (value == null ? false : value);
		final String searchValue = (nama != null && nama.getValue() != null) ? nama.getValue().trim() : "";
		final boolean isSearchEmpty = searchValue.isEmpty();

		if (siswa != null) {
			siswasTemorary.add(siswa);
			jumlahPeserta = pertemuanPunyaUjian.getPertemuan().getPerkuliahan().ambilJumlahDetailperkuliahan();
		} else if (calonSiswa != null) {
			siswasTemorary.add(calonSiswa);
			Session session = HibernateUtil.currentSession();
			jumlahPeserta = ((Number) session.createCriteria(HasilUjianMahasiswa.class)
					.add(Restrictions.isNotNull("keyhasil"))
					.setProjection(Projections.rowCount())
					.add(Restrictions.isNotNull("calonSiswa"))
					.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)).uniqueResult()).intValue();
		} else {

			if (pertemuanPunyaUjian.getPertemuan() != null
					&& pertemuanPunyaUjian.getPertemuan().getJadwalUjianPSB() != null) {
				Session session = HibernateUtil.currentSession();

				if (pertemuanPunyaUjian.getPertemuan().getJadwalUjianPSB().getGelombangPendaftaranPsb() != null) {
					// Gunakan .addAll() untuk menyisipkan data list
					siswasTemorary.addAll(ConstantValues.simpleList(
							session.createCriteria(CalonSiswa.class)
									.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
									.add(isSearchEmpty ? Restrictions.sqlRestriction("true")
											: Restrictions.or(
													Restrictions.ilike("nama", searchValue, MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("noRegistrasi", searchValue, MatchMode.ANYWHERE),
															Restrictions.ilike("noUjian", searchValue, MatchMode.ANYWHERE))))
									.add(Restrictions.eq("gelombangPendaftaranPsb", pertemuanPunyaUjian.getPertemuan().getJadwalUjianPSB().getGelombangPendaftaranPsb()))
									.addOrder(Order.asc("nomorInduk")), 
							CalonSiswa.class));
					
					jumlahPeserta = siswasTemorary.size();
				} else {
					siswasTemorary.addAll(ConstantValues.simpleList(
							session.createCriteria(HasilUjianMahasiswa.class)
									.add(Restrictions.isNotNull("keyhasil"))
									.setProjection(Projections.groupProperty("calonSiswa.id"))
									.add(Restrictions.isNotNull("calonSiswa"))
									.createAlias("calonSiswa", "calonSiswa")
									.add(isSearchEmpty ? Restrictions.sqlRestriction("true")
											: Restrictions.or(
													Restrictions.ilike("calonSiswa.nama", searchValue, MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("calonSiswa.noRegistrasi", searchValue, MatchMode.ANYWHERE),
															Restrictions.ilike("calonSiswa.noUjian", searchValue, MatchMode.ANYWHERE))))
									.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)),
							CalonSiswa.class, false));

					jumlahPeserta = siswasTemorary.size();
				}
			} else if (pertemuanPunyaUjian.getPertemuan() != null) {
				List<Siswa> temp = AbsensiSiswaHelper.populateSiswaDariPertemuan(pertemuanPunyaUjian.getPertemuan());
				List<Siswa> siswas = new ArrayList<Siswa>();
				String searchLower = searchValue.toLowerCase();
				
				for (Siswa s : temp) {
					if (isSearchEmpty || 
					   (s.getNama() != null && s.getNama().toLowerCase().contains(searchLower)) || 
					   (s.getNim() != null && s.getNim().toLowerCase().contains(searchLower))) {
						
						Long id = s.getId();
						if (pertemuanPunyaUjian.getMhsYgTidakIkut() == null || !pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + id + ",")) {
							siswas.add(s);
						}
					}
				}
				temp = null;
				siswasTemorary.addAll(siswas);
				jumlahPeserta = siswasTemorary.size();
			} else {
				Session session = HibernateUtil.currentSession();
				
				siswasTemorary.addAll(ConstantValues.simpleList(
						session.createCriteria(HasilUjianMahasiswa.class)
								.add(Restrictions.isNotNull("keyhasil"))
								.setProjection(Projections.groupProperty("calonSiswa.id"))
								.add(Restrictions.isNotNull("calonSiswa"))
								.createAlias("calonSiswa", "calonSiswa")
								.add(isSearchEmpty ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("calonSiswa.nama", searchValue, MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("calonSiswa.noRegistrasi", searchValue, MatchMode.ANYWHERE),
														Restrictions.ilike("calonSiswa.noUjian", searchValue, MatchMode.ANYWHERE))))
								.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)),
						CalonSiswa.class, false));

				List<Siswa> mhs = ConstantValues.simpleList(
						session.createCriteria(HasilUjianMahasiswa.class)
								.add(Restrictions.isNotNull("keyhasil"))
								.setProjection(Projections.groupProperty("siswa.id"))
								.add(Restrictions.isNotNull("siswa"))
								.createAlias("siswa", "siswa")
								.add(isSearchEmpty ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("siswa.nama", searchValue, MatchMode.ANYWHERE),
												Restrictions.ilike("siswa.nim", searchValue, MatchMode.ANYWHERE)))
								.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)), 
						Siswa.class, false);

				if (mhs != null) {
					siswasTemorary.addAll(mhs);
				}
				jumlahPeserta = siswasTemorary.size();
			}
		}

		final Label label = Common.displayLoadBar(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				ListModel strset = new SimpleListModel(siswasTemorary);
				grid.setRowRenderer(new DetailPertemuanPunyaUjianRenderer());
				grid.setModelCheckMobile(strset);

				int terjawab = 0;
				int pesertaYgIkutUjian = 0;
				
				for (Object[] obj : hasilUjianMahasiswas.values()) {
					if (obj != null && obj.length > 1) {
						Set<Long> terjwb = (Set<Long>) obj[1];
						int jumlhaTerjawab = terjwb == null ? 0 : terjwb.size();
						terjawab += jumlhaTerjawab;
						if (jumlhaTerjawab > 0) {
							pesertaYgIkutUjian++;
						}
					}
				}

				displayStatistik(jumlahPeserta, terjawab, pesertaYgIkutUjian);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (siswasTemorary != null) {
							siswasTemorary.clear();
						}
					}
				});
			}
		});

		final boolean reloadNama = isSearchEmpty;
		if (reloadNama) {
			pertemuanPunyaUjian.bersihkanLokasiHasilUjianMahasiswa();
			pertemuanPunyaUjian.tulisLokasiHasilUjianMahasiswa(new JSONObject().toString());
		}

		// PERBAIKAN 3: Penggunaan Multithreading Thread-Pool Executor untuk proses background
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final int size = siswasTemorary.size();
					if (size == 0) {
						if (label != null) label.setValue("");
						return;
					}

					// AtomicInteger mencegah bentrok saat update persentase dari berbagai thread
					final java.util.concurrent.atomic.AtomicInteger processedCounter = new java.util.concurrent.atomic.AtomicInteger(0);
					java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(ais.common.DbThreadPool.safe(50)); // Plafon aman c3p0

					for (final VOSiswa voSiswa : siswasTemorary) {
						if (voSiswa == null || hasilUjianMahasiswas.containsKey(voSiswa.getId())) {
							processedCounter.incrementAndGet();
							continue;
						}

						executor.submit(new Runnable() {
							@Override
							public void run() {
								try {
									Siswa s = (voSiswa instanceof Siswa) ? (Siswa) voSiswa : null;
									CalonSiswa cSiswa = (voSiswa instanceof CalonSiswa) ? (CalonSiswa) voSiswa : null;

									HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(
											pertemuanPunyaUjian, null, null, s, cSiswa);
									
									if (reloadNama && hasilUjianMahasiswa != null) {
										pertemuanPunyaUjian.populateHasilUjianMahasiswa(hasilUjianMahasiswa, true);
									}
									
									if (hasilUjianMahasiswa != null) {
										MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
												hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
												new Label(), true);

										hasilUjianMahasiswas.put(voSiswa.getId(), new Object[] { 
												hasilUjianMahasiswa,
												hasilUjianMahasiswa.ambilBankSoalIdTerjawab(
														pertemuanPunyaUjian.getJmlDitampilkan(),
														ujianPunyaSoals, refresh) 
										});
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1686");
								} finally {
									int currentIdx = processedCounter.incrementAndGet();
									double percentage = (currentIdx * 100.0) / size;
									if (label != null && voSiswa.getNama() != null) {
										try {
											label.setValue("Sedang memproses data " + voSiswa.getNama() + " ("
													+ Common.numberFormat.get().format(percentage) + " %)");
										} catch (Exception uiEx) { ais.common.ErrorAuditUtil.record(uiEx, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianSiswaHelper.java:1694");}
									}
								}
							}
						});
					}

					executor.shutdown();
					try {
						executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
					} catch (InterruptedException ie) {
						ie.printStackTrace(); ais.common.ErrorAuditUtil.record(ie, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1705");
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1709");
				} finally {
					if (label != null) {
						try { label.setValue(""); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianSiswaHelper.java:1712");}
					}
				}
			}
		}).start();
	}

	/**
	 * Mengonversi hasil ujian menjadi catatan absensi kelas ("Peserta dianggap hadir").
	 * Satu-satunya method {@code static} pada kelas ini — sengaja, agar dapat dipanggil
	 * layar lain tanpa membuat instance helper. Di dalam berkas ini ia dipicu oleh
	 * tombol "Peserta dianggap hadir" pada toolbar
	 * {@link #display(PertemuanPunyaUjian, Component)}, yang hanya tampil pada mode
	 * admin/guru dan bila {@link #pertemuan} terisi.
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 * <li>Menampilkan dialog konfirmasi OK/Batal yang menyebut nama ujian. Seluruh kerja
	 * berikutnya hanya berjalan bila pengguna memilih OK, dan dijalankan di dalam timer
	 * bawaan ({@link Common#createDefaultTimer}) agar tidak memblokir benang UI.</li>
	 * <li>Menyegarkan {@link Pertemuan} dari basis data bila entity-nya sudah tersimpan,
	 * supaya penulisan absensi berikutnya bertumpu pada keadaan terbaru.</li>
	 * <li>Mengiterasi seluruh id {@link HasilUjianMahasiswa} milik ujian ini lewat
	 * {@code pertemuanPunyaUjian.ambilHasilUjianMahasiswa(true)} (argumen {@code true}
	 * memaksa pengambilan segar, bukan dari cache), lalu memuat tiap entity lewat
	 * {@link GeneralValueObject#ambilData}.</li>
	 * <li>Untuk setiap lembar jawaban yang lolos syarat, menulis status absensi lewat
	 * {@link Pertemuan#populate}.</li>
	 * <li>Menyimpan {@link Pertemuan} sekali di akhir dengan
	 * {@code Common.refreshUpdate}, lalu memanggil {@code eventListener} lewat timer
	 * bawaan.</li>
	 * </ol>
	 *
	 * <h4>Syarat seorang peserta dianggap hadir</h4>
	 * <p>
	 * Tiga syarat yang harus terpenuhi bersamaan: peserta teridentifikasi
	 * ({@code calonSiswa} <i>atau</i> {@code siswa} terisi), <b>dan</b> {@code mulaiPada}
	 * terisi, <b>dan</b> {@code selesaiPada} terisi. Artinya peserta yang membuka ujian
	 * tetapi tidak menekan selesai (misalnya terputus jaringan atau kehabisan waktu tanpa
	 * penutupan yang tercatat) <b>tidak</b> ditandai hadir, meskipun jawabannya
	 * tersimpan. Ini perbedaan definisi yang penting dibanding tab "Statistik", yang
	 * mendefinisikan "ikut ujian" cukup dengan adanya minimal satu jawaban tersimpan.
	 * Pemelihara yang membandingkan angka kedua layar perlu menyadari perbedaan ini.
	 * </p>
	 *
	 * <h4>Nilai yang ditulis</h4>
	 * <p>
	 * Status absensi selalu {@link ConstantValues#MASUK} — tidak ada penentuan
	 * terlambat/izin/alfa, dan tidak ada penurunan status bila peserta mengerjakan di
	 * luar jam pertemuan. Catatan absensi diisi kalimat otomatis berisi nama ujian,
	 * rentang waktu pengerjaan, jumlah soal, dan jumlah soal terjawab; jumlah terjawab
	 * dihitung ulang di sini lewat
	 * {@link HasilUjianMahasiswa#ambilBankSoalIdTerjawab} atas daftar soal yang
	 * ditampilkan. Jam masuk dan jam selesai absensi diambil dari catatan absensi yang
	 * sudah ada untuk peserta tersebut
	 * ({@code retreiveAbsensiMulai}/{@code retreiveAbsensiSampai}); bila belum ada,
	 * keduanya jatuh kembali ke jam mulai/selesai baku pertemuan — <b>bukan</b> ke jam
	 * pengerjaan ujian yang sebenarnya. Jadi jam pada catatan absensi mencerminkan jadwal
	 * kelas, sedangkan waktu pengerjaan yang sesungguhnya hanya terekam sebagai teks di
	 * dalam catatan.
	 * </p>
	 *
	 * <h4>Catatan perilaku</h4>
	 * <ul>
	 * <li>Kunci peserta yang diteruskan ke {@link Pertemuan#populate} adalah id
	 * {@code calonSiswa} bila ada, selain itu id {@code siswa}. Karena keduanya berasal
	 * dari tabel berbeda, id yang sama dapat merujuk peserta berbeda; pembeda yang
	 * dipakai adalah argumen jenis {@code "Siswa"} pada pemanggilan {@code populate}.</li>
	 * <li>Operasi ini <b>menimpa</b> status absensi yang sudah ada tanpa memeriksa nilai
	 * sebelumnya, sehingga peserta yang sebelumnya ditandai alfa/izin secara manual akan
	 * berubah menjadi masuk. Tidak ada aksi kebalikan (pembatalan) yang disediakan
	 * kelas ini.</li>
	 * <li>Tidak ada pemeriksaan otorisasi maupun kepemilikan di dalam method ini; karena
	 * bersifat {@code static} dan publik, gerbang satu-satunya adalah visibilitas tombol
	 * pemanggil. Lihat catatan otorisasi pada Javadoc kelas.</li>
	 * <li>Seluruh pemantauan berjalan lewat {@code System.out.println} — tidak ada jejak
	 * audit tersimpan atas siapa yang menjalankan konversi massal ini dan kapan.</li>
	 * </ul>
	 *
	 * @param pertemuanPunyaUjian ujian yang hasilnya dikonversi menjadi absensi; pertemuan
	 *                            tujuan diambil dari
	 *                            {@code pertemuanPunyaUjian.getPertemuan()}
	 * @param eventListener       callback yang dipanggil lewat timer bawaan setelah
	 *                            seluruh penulisan selesai (dipakai pemanggil untuk
	 *                            menyegarkan layar pertemuan)
	 * @throws Exception diteruskan dari dialog {@link MyMessageboxConfig}
	 */
	public static void ujianDianggapHadir(final PertemuanPunyaUjian pertemuanPunyaUjian,
			final EventListener eventListener) throws Exception {

		MyMessageboxConfig.show(
				"Apakah yakin semua siswa yang mengikuti \"" + pertemuanPunyaUjian.getUjian().getNama()
						+ "\" dianggap hadir kelas ini ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
									if (pertemuan != null && pertemuan.getId() != null) {
										HibernateUtil.currentSession().refresh(pertemuan);
									}

									List<Long> hasilUjianMahasiswas = pertemuanPunyaUjian
											.ambilHasilUjianMahasiswa(true);
									System.out.println("hasilUjianMahasiswas -> " + hasilUjianMahasiswas);
									for (Long hasilUjianMahasiswaid : hasilUjianMahasiswas) {

										HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) GeneralValueObject
												.ambilData(HasilUjianMahasiswa.class, hasilUjianMahasiswaid.toString());
										System.out.println("hasilUjianMahasiswa -> " + hasilUjianMahasiswa);
										if (hasilUjianMahasiswa != null) {
											if ((hasilUjianMahasiswa.getCalonSiswa() != null
													|| hasilUjianMahasiswa.getSiswa() != null)
													&& hasilUjianMahasiswa.getMulaiPada() != null
													&& hasilUjianMahasiswa.getSelesaiPada() != null) {
												Long mhs = hasilUjianMahasiswa.getCalonSiswa() != null
														? hasilUjianMahasiswa.getCalonSiswa().getId()
														: hasilUjianMahasiswa.getSiswa().getId();
												Statusabsensi statusabsensi = ConstantValues.MASUK;

												MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa
														.ambilUjianPunyaSoals(hasilUjianMahasiswa
																.getPertemuanPunyaUjian().getJmlDitampilkan(),
																new Label(), true);

												Set<Long> idsa = hasilUjianMahasiswa.ambilBankSoalIdTerjawab(
														pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

												int terjawab = idsa.size();

												String mulai = pertemuan.retreiveAbsensiMulai(mhs);
												String sampai = pertemuan.retreiveAbsensiSampai(mhs);
												if (mulai == null || mulai.trim().isEmpty()) {
													mulai = pertemuan.getWaktuMulai();
												}
												if (sampai == null || sampai.trim().isEmpty()) {
													sampai = pertemuan.getWaktuSelesai();
												}

												System.out.println("terjawab -> " + terjawab + ", mulai " + mulai
														+ ", sampai " + sampai);

												pertemuan.populate(mhs, statusabsensi, "Mengikuti ujian \""
														+ pertemuanPunyaUjian.getUjian().getNama() + "\" pada "
														+ Common.dateFormat5.get().format(hasilUjianMahasiswa.getMulaiPada())
														+ " sampai dengan "
														+ Common.dateFormat5.get()
																.format(hasilUjianMahasiswa.getSelesaiPada())
														+ " dengan jumlah soal "
														+ Common.numberFormat.get()
																.format(hasilUjianMahasiswa.getJumlahSoal())
														+ " dan telah terjawab " + Common.numberFormat.get().format(terjawab),
														null, mulai, sampai, "Siswa");
											}
										}
									}

									Common.refreshUpdate(pertemuan);

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				});

	}
}
