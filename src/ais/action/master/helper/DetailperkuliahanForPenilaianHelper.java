package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.MahasiswaAction;
import ais.action.master.dashboard.admin.RekapHasilTugasKelompokPerVoPertemuan;
import ais.action.master.dashboard.admin.RekapHasilTugasPerVoPertemuan;
import ais.action.master.dashboard.admin.RekapHasilUjianPerVoPertemuan;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.helper.util.NilaiLoader;
import ais.action.master.helper.util.PenilaianUtil;
import ais.action.master.helper.util.PerubahanNilaiListener;
import ais.action.report.Report;
import ais.action.report.helper.nilai.LaporanDaftarPrestasiBelajarWindow;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPenilaian;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisEvaluasi;
import ais.database.model.Jurusan;
import ais.database.model.KomentarPerkuliahan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <b>Mesin penilaian (grading) tingkat kelas</b> untuk satu {@link Perkuliahan}. Kelas ini membangun
 * seluruh layar &quot;Input Nilai&quot; yang dipakai dosen, asisten dosen, dan admin akademik untuk
 * mengisi, memverifikasi, mengunci, mencetak, dan menganalisis nilai seluruh mahasiswa yang terdaftar
 * pada satu jadwal perkuliahan. Ia adalah <b>antarmuka utama</b> di atas entitas
 * {@link Detailperkuliahan} (satu baris = satu mahasiswa pada satu perkuliahan) dan entitas
 * {@link FormatNilai} (satu kolom = satu komponen penilaian beserta bobot persennya).
 *
 * <h3>Peta besar layar</h3>
 * <p>{@link #display(Perkuliahan, Component, EventListener, MyToolbarbuttonConfig, boolean)} adalah
 * pintu masuk tunggal; ia memilih perkuliahan induk bila kelas ini paralel, memuat konfigurasi periode
 * penilaian, lalu menyerahkan pembangunan komponen ke
 * {@link #prosesDisplay(Perkuliahan, Component, EventListener, MyToolbarbuttonConfig, Boolean)}. Hasilnya
 * adalah sebuah {@link Tabbox} berisi tujuh tab: <i>Input Nilai</i> (grid utama, dibangun <b>eager</b>),
 * <i>Asisten Dosen</i>, <i>Rekap Tugas</i>, <i>Rekap Ujian</i>, <i>Rekap Tugas Kelompok</i>,
 * <i>Rekap Total Nilai</i>, dan <i>Prestasi Belajar</i> &mdash; enam terakhir dibangun <b>lazy</b> saat
 * tab diklik. Grid utama dirender baris demi baris oleh {@link DetailPerkuliahanRenderer}.</p>
 *
 * <h3>Hubungan dengan {@link Detailperkuliahan}</h3>
 * <p>Kelas ini adalah <b>salah satu penulis utama</b> kolom nilai pada entitas tersebut, namun ia
 * <b>tidak pernah menulis {@code detailNilai} secara langsung</b>. Semua pengisian nilai per komponen
 * melalui {@link Detailperkuliahan#populateDetailNilai} (baik langsung dari kelas ini maupun lewat
 * {@code PerubahanNilaiListener}), semua pembacaan melalui {@code retreiveDetailNilai} /
 * {@code retreiveDetailNilaiBelumVerify} / {@code retreiveDetailVerifikasiNilai}, dan seluruh
 * rekapitulasi total melalui {@code hitungTotalNilai} / {@code hitungTotalNilaiSementara}. Kolom
 * ringkasan {@code totalNilai}, {@code nilaiHuruf}, {@code totalIP}, {@code lulus}, beserta kembaran
 * &quot;sementara&quot;-nya ditulis lewat setter entitas setelah pemetaan huruf dilakukan
 * {@code Common.getNilaiHuruf(...)}.</p>
 *
 * <p><b>Soal &quot;kunci nilai tidak sepenuhnya beku&quot;.</b> Perilaku itu <b>bukan</b> berasal dari
 * kelas ini. Yang menyebabkannya adalah {@link Detailperkuliahan#getNilaiHuruf()} dan
 * {@link Detailperkuliahan#getTotalIP()}: pada baris terkunci keduanya <i>memetakan ulang</i>
 * {@code totalNilaiKunci} lewat tabel Format Nilai Huruf yang masih bisa diubah admin, dan hanya
 * memakai snapshot {@code nilaiHurufKunci}/{@code totalIPKunci} sebagai cadangan. Peran kelas ini
 * justru dua-duanya sisi berlawanan: ia <b>membuat</b> snapshot itu (lihat tombol Kunci, yang memanggil
 * {@link Detailperkuliahan#bekukanSemuaNilai()} untuk setiap mahasiswa sebelum {@code setDikunci}
 * dipasang, dan tombol kunci per kolom yang memanggil
 * {@link Detailperkuliahan#bekukanDetailNilai(FormatNilai)}), dan ia juga <b>satu-satunya tempat
 * perbedaan itu ditampilkan kepada pengguna</b> &mdash; panel Analisis Nilai Huruf secara eksplisit
 * melaporkan &quot;snapshot huruf kunci sudah tidak sesuai&quot; dan menghitung berapa banyak baris
 * yang snapshot-nya menyimpang. Jadi kelas ini memperlakukan pemetaan ulang tersebut sebagai
 * <b>perilaku yang disengaja</b>, bukan bug yang perlu ditutup di lapisan tampilan.</p>
 *
 * <h3>Lapisan penjaga (gate) yang bekerja di sini</h3>
 * <p>Kemampuan mengubah nilai ditentukan oleh gabungan banyak syarat yang dihitung ulang di beberapa
 * tempat: bendera {@code edit} dari pemanggil, {@link #editDisable} (konfigurasi
 * <code>hanya_dosen_yg_boleh_entry_nilai</code>), {@code aktifPenilaian} beserta {@link Konfigurasi}
 * periode, status {@code perkuliahan.getDikunci()}, kunci per kolom {@code formatNilai.getKunci()},
 * kunci status pertemuan, batas ketidakhadiran UTS/UAS, status pembayaran mahasiswa
 * (<code>mhs_yg_belum_bayar_belum_bisa_di_ntry_nilai</code> dan gerbang semester pendek
 * {@code GateBayarSpUtil}), serta apakah pengguna adalah mahasiswa yang berstatus asisten penilai.
 * <b>Penjaga sesungguhnya berada di lapisan model</b>: {@code populateDetailNilai} keluar lebih awal
 * bila komponen terkunci, dan setter ringkasan menolak menulis saat kunci global aktif. Penjaga di
 * kelas ini bersifat antarmuka &mdash; ia menentukan apa yang tampil, bukan apa yang boleh tersimpan.</p>
 *
 * <p><b>Catatan cakupan.</b> Kelas ini <b>tidak memverifikasi bahwa pengguna adalah dosen pengampu
 * kelas yang sedang dibuka.</b> Ia menerima keputusan itu dari Action pemanggil (
 * {@code PenilaianAction}, {@code AktifitasPerkuliahanHelper}, {@code FormulirKegiatanAction}) lewat
 * parameter {@code edit} dan {@code aktifPenilaianData}. Pembeda yang dikenali kelas ini hanyalah
 * &quot;pengguna mahasiswa vs bukan mahasiswa&quot; ({@code tbmuser.getMahasiswa() == null}) dan
 * &quot;punya profil dosen vs tidak&quot; ({@code tbmuser.ambilDosen()}). Konsekuensinya, tombol Kunci,
 * Buka Kunci, Verifikasi, Reset, Masukkan Nilai Absen, dan Hitung Ulang terbuka bagi <b>semua</b> akun
 * non-mahasiswa yang berhasil mencapai layar ini.</p>
 *
 * <h3>Efek samping dan model transaksi</h3>
 * <p>Hampir seluruh listener di kelas ini membuka {@link Session} Hibernate sendiri
 * ({@code currentNativeSession()} atau {@code openSession()}), menjalankan
 * {@code begin/commit}, lalu menutupnya. Beberapa operasi berat berjalan di dalam
 * {@code Common.createDefaultTimer(...)} atau {@link Thread} latar dengan session dedikasi. Tidak ada
 * penguncian baris basis data (<i>pessimistic lock</i>) di mana pun: dua pengguna yang membuka kelas
 * yang sama secara bersamaan dapat saling menimpa nilai. Operasi destruktif (Reset, Buka Kunci, Hapus
 * komentar, Hapus asisten) selalu meminta konfirmasi melalui {@link MyMessageboxConfig}.</p>
 *
 * @see Detailperkuliahan
 * @see Perkuliahan
 * @see FormatNilai
 * @see ais.action.master.helper.util.PerubahanNilaiListener
 * @see ais.action.master.helper.util.PenilaianUtil
 */
public class DetailperkuliahanForPenilaianHelper implements DataLoader {

	/**
	 * Pola pengenal <b>kode Sub-CPMK</b> pada awal nama sebuah {@link FormatNilai}, dipakai
	 * {@link #ambilNamaFormatNilaiRingkas(FormatNilai)} untuk memotong nama komponen OBE yang panjang
	 * menjadi kodenya saja.
	 *
	 * <p>Pola ini bersifat <i>case-insensitive</i> ({@code (?i)}), mengizinkan spasi awal, menerima
	 * penulisan {@code Sub-CPMK}, {@code SubCPMK}, maupun {@code Sub CPMK} (tanda hubung dan spasi
	 * sama-sama opsional), lalu menangkap nomor berupa satu angka atau rangkaian angka bertitik seperti
	 * {@code 1}, {@code 2.3}, atau {@code 1.2.4}. Batas kata {@code \b} di akhir mencegah pola ikut
	 * memakan karakter berikutnya. Grup tangkap pertama berisi kode utuh, misalnya
	 * <code>&quot;Sub-CPMK 2.1&quot;</code> dari nama
	 * <code>&quot;Sub-CPMK 2.1 Mahasiswa mampu menganalisis ...&quot;</code>.</p>
	 *
	 * <p>Objek {@link Pattern} sengaja dikompilasi sekali sebagai konstanta {@code static final} karena
	 * ia dipanggil sekali untuk setiap kolom pada setiap pembangunan ulang layar; mengompilasinya
	 * berulang kali akan boros. {@link Pattern} bersifat <i>immutable</i> dan aman dipakai banyak
	 * thread, sedangkan {@link Matcher} yang dihasilkan tidak &mdash; karena itu {@code Matcher} selalu
	 * dibuat baru di dalam pemanggil.</p>
	 */
	private static final Pattern POLA_KODE_SUB_CPMK = Pattern
			.compile("(?i)^\\s*(sub\\s*-?\\s*cpmk\\s*[0-9]+(?:\\.[0-9]+)*)\\b");

	/**
	 * Meringkas nama sebuah {@link FormatNilai} menjadi <b>label pendek untuk kepala kolom</b> pada
	 * grid Input Nilai. Tanpa peringkasan ini nama komponen OBE &mdash; yang lazimnya berbentuk
	 * kalimat capaian pembelajaran sepanjang satu paragraf &mdash; akan melebarkan tabel sampai tidak
	 * terbaca. Uraian lengkapnya tidak dibuang: pemanggil memasangnya kembali sebagai
	 * {@code tooltiptext} pada label kolom, sehingga dosen tetap bisa membaca teks penuh dengan
	 * mengarahkan tetikus.
	 *
	 * <h3>Urutan keputusan</h3>
	 * <ol>
	 * <li><b>Penjagaan null.</b> {@code formatNilai} yang {@code null}, atau yang namanya {@code null},
	 * menghasilkan string kosong. Metode ini tidak pernah melempar dan tidak pernah mengembalikan
	 * {@code null}, sehingga pemanggil boleh langsung merangkainya ke dalam label.</li>
	 * <li><b>Deteksi mode OBE.</b> Sebuah komponen dianggap OBE bila ia terhubung ke Capaian
	 * Pembelajaran Lulusan ({@code getCapaianPembelajaranLulusan() != null}) <i>atau</i> memiliki kode
	 * Sub-CPMK yang tidak kosong. Bila <b>bukan</b> OBE &mdash; misalnya komponen klasik
	 * &quot;UTS&quot;, &quot;UAS&quot;, &quot;Tugas&quot; &mdash; namanya memang sudah pendek dan
	 * dikembalikan apa adanya setelah {@code trim()}. Peringkasan sengaja tidak diterapkan di luar
	 * mode OBE agar nama komponen klasik yang kebetulan mengandung tanda hubung tidak ikut terpotong.</li>
	 * <li><b>Pemotongan pada pemisah eksplisit.</b> Bila nama OBE mengandung urutan
	 * <code>&quot; - &quot;</code> (spasi, tanda hubung, spasi) pada posisi lebih dari nol, bagian
	 * sebelum pemisah itulah yang dipakai. Pemeriksaan {@code pemisah > 0} penting: pemisah pada indeks
	 * nol akan menghasilkan potongan kosong. Konvensi penamaan yang dianjurkan memang menaruh kode di
	 * depan pemisah ini, misalnya <code>&quot;Sub-CPMK 1.2 - Mampu merancang ...&quot;</code>.</li>
	 * <li><b>Penyelamat berbasis pola.</b> Bila pemisah tidak ada, {@link #POLA_KODE_SUB_CPMK} dicoba
	 * pada awal nama. Bila cocok, kode hasil tangkapan dinormalkan &mdash; setiap rentetan spasi
	 * (termasuk tab dan baris baru) diringkas menjadi satu spasi tunggal oleh
	 * {@code replaceAll("\\s+", " ")} &mdash; supaya <code>&quot;Sub  -  CPMK   3&quot;</code> tampil
	 * rapi sebagai <code>&quot;Sub - CPMK 3&quot;</code>.</li>
	 * <li><b>Menyerah dengan aman.</b> Bila pola pun tidak cocok, nama OBE dikembalikan utuh. Lebih
	 * baik kolom melebar daripada label kosong yang membuat dosen tidak tahu komponen mana yang sedang
	 * diisi.</li>
	 * </ol>
	 *
	 * <p><b>Sifat.</b> Metode ini murni: ia tidak menyentuh basis data, tidak mengubah
	 * {@code formatNilai}, dan tidak menyimpan state. Visibilitasnya sengaja <i>package-private</i> dan
	 * {@code static} agar dapat dipakai ulang oleh helper penilaian lain dalam paket yang sama tanpa
	 * membocorkannya ke seluruh aplikasi.</p>
	 *
	 * @param formatNilai komponen penilaian yang namanya hendak diringkas; boleh {@code null}.
	 * @return label pendek siap pakai untuk kepala kolom; string kosong bila masukan tidak memadai,
	 *         tidak pernah {@code null}.
	 * @see #POLA_KODE_SUB_CPMK
	 */
	static String ambilNamaFormatNilaiRingkas(FormatNilai formatNilai) {
		if (formatNilai == null) {
			return "";
		}

		String nama = formatNilai.getNama();
		if (nama == null) {
			return "";
		}

		String ringkas = nama.trim();
		boolean formatObe = formatNilai.getCapaianPembelajaranLulusan() != null
				|| (formatNilai.getKodeSubCpmk() != null && formatNilai.getKodeSubCpmk().trim().length() > 0);
		if (!formatObe) {
			return ringkas;
		}

		int pemisah = ringkas.indexOf(" - ");
		if (pemisah > 0) {
			return ringkas.substring(0, pemisah).trim();
		}

		Matcher matcher = POLA_KODE_SUB_CPMK.matcher(ringkas);
		return matcher.find() ? matcher.group(1).replaceAll("\\s+", " ").trim() : ringkas;
	}

	/**
	 * Grid utama tab <i>Input Nilai</i>: satu baris per mahasiswa yang terdaftar dan berstatus
	 * {@link Detailperkuliahan#DISETUJUI} pada perkuliahan ini. Dibuat ulang setiap kali
	 * {@link #prosesDisplay} berjalan, diisi oleh {@link #loadData(Object)}, dan dirender oleh
	 * {@link DetailPerkuliahanRenderer}. Ukuran halaman sengaja dipasang sangat besar (10000) agar
	 * seluruh kelas tampil dalam satu halaman &mdash; dosen mengharapkan daftar utuh, bukan paginasi.
	 * Dibekukan lewat {@code Common.freeze} ketika perkuliahan terkunci.
	 */
	private MyGrid grid;

	/**
	 * Grid daftar {@link KomentarPerkuliahan} yang ditampilkan di bawah grid nilai. Berisi catatan
	 * bebas dari dosen/verifikator mengenai kelas ini, diisi oleh {@link #loadDataKomentar()} dan
	 * dirender oleh {@link KomentarPerkuliahanRenderer}. Hanya terlihat bila {@link #semester} lebih
	 * dari nol, karena baris semester 0 mewakili konversi/transfer yang tidak punya komentar kuliah.
	 */
	private MyGrid gridKomentar;

	/**
	 * Perkuliahan yang <b>benar-benar</b> sedang dinilai. Nilainya belum tentu sama dengan objek yang
	 * diserahkan pemanggil: {@link #display} menggantinya dengan
	 * {@code kuliyah.getPerkuliahan_paralel()} bila kelas yang dibuka merupakan kelas paralel, sebab
	 * kelas paralel berbagi satu tempat penyimpanan nilai dengan kelas induknya. Semua pembacaan
	 * konfigurasi kelas (kunci, bobot, aturan nilai 0, batas kehadiran) dan semua penyimpanan nilai
	 * mengacu ke objek ini, bukan ke parameter {@code kuliyah} yang masih dibawa berkeliling untuk
	 * keperluan tab rekap.
	 */
	private Perkuliahan perkuliahan;

	/**
	 * Daftar komponen penilaian aktif untuk {@link #perkuliahan}, yakni definisi kolom nilai beserta
	 * bobot persennya. Diambil dari {@code Common.getFormatNilais(perkuliahan)} yang memakai cache;
	 * tombol Refresh dan Refresh pada tab Asisten memanggil varian dengan penyegaran paksa. Urutan
	 * daftar menentukan urutan kolom pada grid <b>dan</b> indeks {@code nilai_1..nilai_n} pada laporan
	 * PDF, sehingga perubahan urutan berdampak langsung ke keluaran cetak.
	 */
	private List<FormatNilai> formatNilais;

	/**
	 * Konfigurasi periode penilaian untuk kombinasi tahun akademik, ganjil/genap, dan status semester
	 * pendek kelas ini, hasil {@code CommonPenilaian.getKonfigurasi(...)}. Dipakai berulang kali
	 * sebagai gerbang waktu: entri nilai hanya terbuka bila {@code getNilai()} bernilai
	 * {@link Konfigurasi#AKTIF}, kecuali pemanggil sudah menyalakan {@link #aktifPenilaian} secara
	 * eksplisit. Perhatikan bahwa {@link #prosesDisplay} membaca field ini, bukan variabel lokal,
	 * sehingga nilainya harus sudah terpasang oleh {@link #display} sebelum pembangunan layar dimulai.
	 */
	private Konfigurasi konfigurasi;

	/**
	 * Kumpulan string absensi mentah dari seluruh {@link Pertemuan} kelas ini yang kolom absensinya
	 * tidak kosong. Setiap string memuat rekaman kehadiran semua mahasiswa untuk satu pertemuan dan
	 * diuraikan oleh {@code Perkuliahan.hitungStatus(statusPertemuan, mahasiswaId)} menjadi peta
	 * jumlah per kode status (M = masuk, A = alpa, S = sakit, I = izin, T = total). Disiapkan sekali
	 * di awal {@link #prosesDisplay} lalu dipakai ulang oleh setiap baris renderer, sehingga rekap
	 * kehadiran tidak perlu membaca ulang tabel pertemuan untuk tiap mahasiswa.
	 */
	private List<String> statusPertemuan;

	/**
	 * Callback milik pemanggil yang dijalankan setiap kali sebuah nilai berubah, diteruskan ke setiap
	 * {@code PerubahanNilaiListener} yang dipasang pada kotak nilai. Umumnya dipakai layar induk untuk
	 * menyegarkan ringkasan di luar helper ini. {@link #display} hanya menimpanya bila argumen yang
	 * masuk tidak {@code null}, sehingga pemanggilan ulang tanpa callback tidak menghapus callback
	 * yang sudah terpasang sebelumnya.
	 */
	private EventListener onPerubahanNilai;

	/**
	 * Kotak pencarian mahasiswa pada toolbar. Isinya dipakai {@link #loadData(Object)} sebagai kata
	 * kunci pencocokan NIM atau nama, dan juga dibaca oleh kriteria tombol Cetak Data sehingga hasil
	 * cetak mengikuti penyaringan yang sedang tampak di layar. Menekan Enter di dalamnya memicu
	 * pencarian yang sama dengan tombol Cari.
	 */
	private Textbox nama;

	/**
	 * Profil dosen milik pengguna yang sedang masuk, hasil {@code tbmuser.ambilDosen()}; bernilai
	 * {@code null} untuk akun admin/staf yang tidak terikat data dosen. Dipakai sebagai pembeda
	 * kewenangan verifikasi: tombol dan kotak centang Verifikasi hanya aktif bila pengguna bukan dosen
	 * ({@code dosen == null}) atau bila kelas ini mengizinkan dosen memverifikasi nilainya sendiri
	 * lewat {@code perkuliahan.getDosenBolehVerifikasiNilaiSendiri()}. Aturan ini menegakkan pemisahan
	 * peran pengisi dan pemeriksa nilai.
	 */
	private Dosen dosen;

	/**
	 * Penanda bahwa pemanggil membuka layar ini dalam <b>mode penilaian aktif</b>, yaitu jalur yang
	 * secara sengaja mengizinkan entri nilai meskipun {@link #konfigurasi} periode sedang tertutup.
	 * Nilainya sepenuhnya berasal dari argumen {@code aktifPenilaianData} milik {@link #display};
	 * helper ini tidak pernah menghitungnya sendiri. Selain membuka entri, bendera ini juga
	 * mengaktifkan kembali tombol Kunci dan melonggarkan tombol Buka Kunci bagi pemilik kunci.
	 */
	private Boolean aktifPenilaian = false;

	/**
	 * Bendera izin ubah tingkat layar yang diterima constructor dari Action pemanggil. Bernilai
	 * {@code false} berarti layar dibuka dalam mode baca saja. Bersama {@link #aktifPenilaian} dan
	 * {@link #konfigurasi}, inilah tiga syarat dasar yang dirangkai ulang di banyak tempat untuk
	 * menentukan apakah sebuah kotak nilai boleh diedit. Perlu dicatat bahwa nilainya <b>dipercaya
	 * apa adanya</b>: helper tidak memeriksa apakah pengguna benar-benar berhak mengubah kelas ini.
	 */
	private boolean edit = false;

	/**
	 * Kotak centang toolbar &quot;Nilai 0 tidak masuk pembagi nilai akhir&quot;. Mengubahnya menulis
	 * langsung ke {@code perkuliahan.setNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir(...)} lalu
	 * memicu penghitungan ulang seluruh kelas. Efeknya pada perhitungan: bobot komponen yang bernilai
	 * nol dikeluarkan dari penyebut rata-rata tertimbang, sehingga mahasiswa tidak &quot;dihukum&quot;
	 * oleh komponen yang memang belum diselenggarakan. Hanya tampil bila konfigurasi global
	 * mengizinkan, pengguna bukan mahasiswa, dan kelas belum dikunci.
	 */
	private MyCheckboxConfig nilai0masukNilaiAkhir;

	/**
	 * Kotak centang toolbar &quot;Jika ada nilai 0 tidak menghitung nilai akhir&quot;. Aturan ini
	 * lebih keras daripada {@link #nilai0masukNilaiAkhir}: bila satu saja komponen bernilai nol, nilai
	 * akhir tidak dihitung sama sekali (menjadi 0/E), yang dipakai kampus untuk memaksa kelengkapan
	 * seluruh komponen sebelum nilai diumumkan. Keduanya saling meniadakan dalam praktiknya, dan panel
	 * Analisis Pintar menampilkan aturan ini lebih dulu bila keduanya menyala.
	 */
	private MyCheckboxConfig jikaNilai0masukNilaiAkhir;

	/**
	 * Nilai bawaan untuk aturan &quot;nilai 0 tidak masuk perhitungan&quot; yang dibaca sekali di
	 * constructor dari konfigurasi global
	 * <code>nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir</code>. Dipakai sebagai <b>penambal
	 * data lama</b>: bila kolom sejenis pada {@link Perkuliahan} masih {@code null} &mdash; kelas yang
	 * dibuat sebelum kolom itu ada &mdash; renderer mengisinya dengan nilai ini agar perhitungan punya
	 * jawaban pasti. Ini juga berarti kebijakan global merembes ke kelas lama pada saat pertama kali
	 * dibuka.
	 */
	private boolean nilai0MasukPenghitungan;

	/**
	 * Pengguna yang sedang masuk. Diambil ulang dari {@code Common.getCurrentUser()} di beberapa titik
	 * &mdash; constructor, {@link #display}, bahkan di tengah {@link DetailPerkuliahanRenderer#render}
	 * &mdash; karena renderer dapat berjalan pada siklus permintaan yang berbeda. Dipakai untuk tiga
	 * hal: membedakan akun mahasiswa dari akun pegawai, mencatat identitas verifikator dan pemasang
	 * kunci, serta memeriksa peran {@link Tbmrole#ADMINISTRATOR} pada jalur buka-kunci istimewa.
	 */
	private Tbmuser tbmuser;

	/**
	 * Menyalakan seluruh antarmuka verifikasi nilai: kolom centang per komponen, kolom
	 * <i>Verify</i> per mahasiswa, tombol Verifikasi massal, dan pilihan menyembunyikan nilai yang
	 * belum diverifikasi. Bernilai {@code true} bila konfigurasi
	 * <code>ada_proses_verifikasi_penilaian_kepada_dosen</code> aktif <b>atau</b>
	 * <code>nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk</code> aktif &mdash; yang kedua
	 * ikut menyalakannya karena tanpa antarmuka verifikasi, nilai tidak akan pernah bisa masuk IPK.
	 * Bila mati, {@link #prosesDisplay} sekalian memaksa
	 * {@code perkuliahan.setSembunyikanNilaiJikaBelumDiverifikasi(false)} supaya nilai tidak
	 * tersembunyi selamanya tanpa cara memverifikasinya. Hanya dihitung untuk pengguna non-mahasiswa.
	 */
	private boolean adaProsesVerifikasiNilai = false;

	/**
	 * Menandai bahwa pengguna mahasiswa yang sedang masuk berstatus <b>asisten penilai</b> pada kelas
	 * ini, hasil {@code perkuliahan.merupakanAsistenNilai(tbmuser.getMahasiswa())}. Inilah satu-satunya
	 * jalan bagi akun mahasiswa untuk mengisi nilai: tanpa bendera ini, setiap syarat edit menutup
	 * kotak nilai bagi akun mahasiswa. Dihitung ulang setiap {@link #prosesDisplay} dan selalu diawali
	 * dengan {@code false} agar sisa keadaan dari pemanggilan sebelumnya tidak bocor.
	 */
	private boolean mahasiswaBolehUbahNilai = false;

	/**
	 * Kotak centang toolbar &quot;Hanya input nilai huruf&quot;. Bila menyala, seluruh kolom komponen
	 * disembunyikan dan grid menampilkan satu kotak teks huruf per mahasiswa; nilai angka diturunkan
	 * dari titik tengah rentang huruf yang dipilih. Mode ini dipakai untuk kelas yang nilainya datang
	 * dari luar sistem &mdash; mata kuliah konversi, program pertukaran, atau kelas kerja sama &mdash;
	 * sehingga rincian komponennya memang tidak ada.
	 */
	private MyCheckboxConfig hanyaInputNilaiHuruf;

	/**
	 * Kotak centang toolbar &quot;Sembunyikan nilai ke mhs, jika blm di-verifikasi&quot;. Bila menyala,
	 * mahasiswa melihat pasangan kolom <i>sementara</i> ({@code totalNilaiSementara},
	 * {@code nilaiHurufSementara}, {@code totalIPSementara}) alih-alih nilai final, sampai baris
	 * berstatus {@link Detailperkuliahan#VERIFIED}. Mengubahnya menyimpan ke {@link #perkuliahan} lalu
	 * memicu pemuatan ulang seluruh nilai kelas, sebab pemisahan nilai final dan sementara terjadi saat
	 * penulisan, bukan saat pembacaan.
	 */
	private MyCheckboxConfig sembunyikanNilaiJikaBelumDiverifikasi;

	// private boolean delete = false;

	/**
	 * Penjaga tambahan dari konfigurasi <code>hanya_dosen_yg_boleh_entry_nilai</code>: bernilai
	 * {@code true} bila kebijakan itu aktif <b>dan</b> pengguna yang masuk tidak memiliki profil dosen.
	 * Dihitung <b>sekali di constructor</b> dan tidak pernah dihitung ulang setelahnya.
	 *
	 * <p><b>Hati-hati membacanya.</b> {@link #prosesDisplay} mendeklarasikan variabel lokal
	 * {@code editDisable} yang <i>membayangi</i> field ini dan bernilai jauh lebih luas (turut
	 * memperhitungkan {@link #edit}, {@link #aktifPenilaian}, dan status asisten mahasiswa). Variabel
	 * lokal itu tidak pernah ditulis kembali ke field. Akibatnya, kode di dalam
	 * {@link DetailPerkuliahanRenderer} &mdash; yang berada di luar {@code prosesDisplay} &mdash;
	 * membaca <b>field</b> ini yang bermakna sempit, sedangkan toolbar membaca variabel lokal yang
	 * bermakna luas. Perbedaan cakupan ini adalah sumber kemunculan gerbang yang tidak seragam antara
	 * jalur kolom komponen dan jalur kotak nilai huruf.</p>
	 */
	private boolean editDisable = false;

	/**
	 * Menyiapkan helper untuk satu sesi penilaian dan menetapkan dua keputusan kebijakan yang berlaku
	 * seumur hidup objek ini.
	 *
	 * <p>Pertama, bendera {@link #edit} diterima apa adanya dari Action pemanggil. Helper <b>tidak
	 * memverifikasi</b> apakah pengguna berhak mengubah nilai kelas yang nanti dibuka; keputusan itu
	 * sepenuhnya milik pemanggil ({@code PenilaianAction}, {@code AktifitasPerkuliahanHelper},
	 * {@code FormulirKegiatanAction}), dan constructor ini bahkan belum tahu kelas mana yang akan
	 * ditampilkan &mdash; {@link Perkuliahan} baru diserahkan pada {@link #display}.</p>
	 *
	 * <p>Kedua, kebijakan <code>hanya_dosen_yg_boleh_entry_nilai</code> dievaluasi. Bila kebijakan itu
	 * aktif dan pengguna yang sedang masuk tidak punya profil dosen ({@code tbmuser.ambilDosen()}
	 * bernilai {@code null}), {@link #editDisable} dinyalakan sehingga kolom komponen nilai tampil
	 * sebagai label, bukan kotak isian. Pembacaan {@code Common.getCurrentUser()} hanya dilakukan di
	 * dalam cabang ini, sehingga {@link #tbmuser} bisa saja masih {@code null} setelah constructor
	 * selesai bila kebijakan tersebut mati &mdash; {@link #display} mengisinya kemudian, dan kode lain
	 * bergantung pada urutan itu.</p>
	 *
	 * <p>Ketiga, nilai bawaan aturan &quot;nilai 0 tidak masuk perhitungan&quot; dibaca sekali ke
	 * {@link #nilai0MasukPenghitungan} untuk menambal kelas lama yang kolomnya masih kosong.</p>
	 *
	 * <p><b>Efek samping.</b> Constructor mencetak nilai {@link #editDisable} ke {@code System.out}
	 * sebagai jejak diagnostik penelusuran keluhan &quot;kotak nilai tidak bisa diisi&quot;; keluaran
	 * itu tidak memuat data pribadi. Selain itu ia hanya membaca konfigurasi dan tidak memulai
	 * transaksi apa pun. Objek yang dihasilkan <b>terikat pada satu desktop ZK</b> karena menyimpan
	 * rujukan pengguna dan komponen; jangan menyimpannya di scope aplikasi atau membagikannya antar
	 * sesi.</p>
	 *
	 * @param edit {@code true} bila layar dibuka untuk mengubah nilai, {@code false} untuk baca saja.
	 *             Nilai ini tetap harus lolos gerbang periode dan gerbang kunci sebelum kotak nilai
	 *             benar-benar terbuka.
	 * @see #display(Perkuliahan, Component, EventListener, MyToolbarbuttonConfig, boolean)
	 */
	public DetailperkuliahanForPenilaianHelper(boolean edit) {

		this.edit = edit;

		if (Common.bolehKonfigurasi("hanya_dosen_yg_boleh_entry_nilai", Konfigurasi.TIDAK_AKTIF)) {
			tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.ambilDosen() == null) {
				editDisable = true;
			}
		}

		System.out.println("editDisable -> " + editDisable);

		nilai0MasukPenghitungan = Common.bolehKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir");

	}

	/**
	 * Perender satu baris mahasiswa pada grid <i>Input Nilai</i>. Kelas ini adalah <b>jantung layar
	 * penilaian</b>: ia mengubah satu id {@link Detailperkuliahan} menjadi seluruh sel yang tampak
	 * &mdash; foto, identitas, rekap kehadiran, kotak nilai untuk setiap komponen, nilai total beserta
	 * hurufnya, dan kotak centang verifikasi &mdash; sekaligus memutuskan sel mana yang boleh diedit
	 * dan mana yang hanya boleh dibaca.
	 *
	 * <h3>Keterikatan pada kelas induk</h3>
	 * <p>Sebagai kelas dalam non-statis, setiap instance melekat pada satu
	 * {@link DetailperkuliahanForPenilaianHelper} dan membaca state induknya secara langsung:
	 * {@link #formatNilais} untuk daftar kolom, {@link #perkuliahan} untuk aturan kelas,
	 * {@link #statusPertemuan} untuk rekap kehadiran, {@link #columns} dan {@link #columnMahasiswa}
	 * untuk mengatur lebar kolom, serta {@link #edit}, {@link #editDisable}, {@link #aktifPenilaian},
	 * {@link #konfigurasi}, {@link #tbmuser}, {@link #dosen}, dan {@link #adaProsesVerifikasiNilai}
	 * sebagai gerbang perizinan. Karena itu instance ini <b>tidak boleh</b> disimpan atau dipakai
	 * ulang di luar desktop ZK tempat ia dibuat.</p>
	 *
	 * <h3>Konfigurasi yang dibaca sekali per instance</h3>
	 * <p>Keempat field di bawah dibaca pada saat instansiasi, bukan per baris. Ini disengaja: renderer
	 * dibuat sekali untuk seluruh grid, sehingga konfigurasi batas ketidakhadiran cukup dibaca satu
	 * kali dan tidak menghasilkan ratusan pembacaan konfigurasi untuk kelas berisi banyak mahasiswa.
	 * Konsekuensinya, perubahan konfigurasi di tengah sesi baru terlihat setelah layar dibangun
	 * ulang.</p>
	 *
	 * <h3>Efek samping</h3>
	 * <p>{@link #render(Row, Object)} tidak hanya membaca. Ia menulis kembali ke {@link #perkuliahan}
	 * bila kolom aturan nilai 0 masih {@code null}, memperbarui {@link #tbmuser} milik induk, mengubah
	 * lebar kolom grid, dan memasang belasan listener yang masing-masing membuka transaksi Hibernate
	 * sendiri ketika dipicu pengguna. Semua itu harus berjalan di event thread ZK dengan konteks
	 * sesi pengguna yang aktif.</p>
	 *
	 * @see DetailperkuliahanForPenilaianHelper
	 * @see ais.action.master.helper.util.PerubahanNilaiListener
	 * @see ais.action.master.helper.util.NilaiLoader
	 */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Menyalakan pemeriksaan batas ketidakhadiran untuk <b>UTS</b>, dari konfigurasi
		 * <code>aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uts</code>
		 * yang bawaannya {@link Konfigurasi#TIDAK_AKTIF}. Bila mati,
		 * {@link #checkWarningUts(Detailperkuliahan, Map)} langsung mengembalikan string kosong tanpa
		 * membaca konfigurasi apa pun &mdash; jalur cepat yang penting karena metode itu dipanggil
		 * sekali untuk setiap mahasiswa.
		 */
		private boolean aturanUts = Common.bolehKonfigurasi("aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uts", Konfigurasi.TIDAK_AKTIF);

		/**
		 * Kembaran {@link #aturanUts} untuk <b>UAS</b>. Keduanya terpisah agar kampus dapat memberlakukan
		 * syarat kehadiran hanya pada ujian akhir &mdash; pola yang lazim, karena pada saat UTS jumlah
		 * pertemuan yang sudah berlangsung masih terlalu sedikit untuk dinilai adil.
		 */
		private boolean aturanUas = Common.bolehKonfigurasi("aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uas", Konfigurasi.TIDAK_AKTIF);

		/**
		 * Nama status pertemuan yang <b>menjadi sasaran</b> sanksi ketidakhadiran UTS, misalnya
		 * <code>&quot;UTS&quot;</code>. Field ini menentukan <i>seberapa luas</i> sanksi bekerja, dan
		 * kedua kemungkinannya berlawanan arah:
		 *
		 * <ul>
		 * <li><b>Kosong</b> &rarr; sanksi berlaku <b>menyeluruh</b>. Begitu peringatan UTS muncul,
		 * <i>seluruh</i> kolom nilai mahasiswa itu dikunci menjadi label.</li>
		 * <li><b>Terisi</b> &rarr; sanksi berlaku <b>selektif</b>. Hanya kolom yang nama status
		 * pertemuannya sama persis (dibandingkan tanpa membedakan huruf besar-kecil) yang dikunci;
		 * kolom lain tetap dapat diisi. Ini memungkinkan dosen tetap memasukkan nilai tugas dan
		 * kehadiran bagi mahasiswa yang kehilangan hak UTS.</li>
		 * </ul>
		 *
		 * <p>Perbandingan selektif itu memanggil {@code formatNilai.getStatusPertemuan().getNama()}
		 * tanpa penjagaan {@code null}, sehingga komponen OBE murni yang tidak terhubung ke status
		 * pertemuan berpotensi memicu galat pada jalur tersebut.</p>
		 */
		private String statusPertemuanUts = Common.getKonfigurasi(
				"status_pertemuan_aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uts",
				"").getNilai();

		/**
		 * Kembaran {@link #statusPertemuanUts} untuk UAS, dengan makna kosong/terisi yang sama persis.
		 * Bila keduanya terisi dengan nama status pertemuan yang berbeda, seorang mahasiswa dapat
		 * kehilangan hak nilai UTS dan UAS secara terpisah.
		 */
		private String statusPertemuanUas = Common.getKonfigurasi(
				"status_pertemuan_aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uas",
				"").getNilai();

		/**
		 * Menyusun <b>teks peringatan ketidakhadiran menjelang UTS</b> untuk satu mahasiswa. Keluarannya
		 * dipakai dua kali oleh {@link #render(Row, Object)}: ditampilkan sebagai teks merah di bawah
		 * nama mahasiswa, dan &mdash; jauh lebih berat konsekuensinya &mdash; dipakai sebagai
		 * <b>gerbang yang mengunci kotak nilai</b>. String tidak kosong berarti mahasiswa melanggar
		 * setidaknya satu batas.
		 *
		 * <h3>Jalan pintas</h3>
		 * <p>Bila {@link #aturanUts} mati, metode langsung mengembalikan string kosong. Seluruh
		 * pembacaan konfigurasi berada di dalam cabang tersebut, sehingga kelas yang tidak memberlakukan
		 * aturan ini tidak menanggung biaya apa pun.</p>
		 *
		 * <h3>Lima ambang batas</h3>
		 * <p>Setiap ambang dibaca lewat {@code Common.getKonfigurasi(...)} varian <b>berlapis</b>, yang
		 * menerima semester, tahun angkatan, jurusan, program, dan status awal mahasiswa. Artinya
		 * kampus dapat menetapkan batas berbeda untuk, misalnya, mahasiswa alih jenjang atau program
		 * tertentu. Kunci konfigurasi dirakit dari nama tetap ditambah akhiran <code>uts</code>:</p>
		 * <ol>
		 * <li><b>Alpa</b> &mdash; jumlah ketidakhadiran tanpa keterangan, bawaan 34.</li>
		 * <li><b>Sakit</b> &mdash; bawaan 34.</li>
		 * <li><b>Izin</b> &mdash; bawaan 34.</li>
		 * <li><b>Semua</b> &mdash; jumlah gabungan alpa, sakit, dan izin, bawaan 34.</li>
		 * <li><b>Persen</b> &mdash; batas persentase ketidakhadiran terhadap
		 * {@code getJumlahMaksimalPertemuan()}, bawaan 0.</li>
		 * </ol>
		 *
		 * <p>Angka bawaan 34 sengaja dipilih jauh lebih besar daripada jumlah pertemuan satu semester
		 * (lazimnya 14&ndash;16), sehingga <b>ambang yang belum dikonfigurasi tidak akan pernah
		 * terpicu</b> &mdash; sikap gagal-terbuka yang disengaja agar kesalahan konfigurasi tidak
		 * diam-diam menghapus hak ujian mahasiswa. Sebaliknya, ambang persen berbawaan <b>0</b> dan
		 * dibandingkan dengan {@code persen > maxPersen}: begitu {@link #aturanUts} dinyalakan tanpa
		 * mengisi batas persen, <i>setiap</i> mahasiswa dengan satu saja ketidakhadiran akan memicu
		 * peringatan. Perbedaan sikap antara empat ambang pertama dan ambang kelima ini penting
		 * dipahami sebelum menyalakan aturan.</p>
		 *
		 * <p>Setiap penguraian angka dibungkus {@code try/catch} yang mencatat galat ke
		 * {@code ErrorAuditUtil}; konfigurasi yang bukan angka menyebabkan ambang kembali ke bawaannya,
		 * bukan menggagalkan render baris.</p>
		 *
		 * <h3>Perakitan pesan</h3>
		 * <p>Empat pemeriksaan pertama memakai pembanding <b>lebih besar atau sama dengan</b>
		 * ({@code >=}), sehingga mencapai batas persis sudah dianggap melanggar. Potongan pesannya
		 * <b>dirangkai tanpa pemisah</b>, sehingga pelanggaran ganda menghasilkan kalimat yang menempel
		 * satu sama lain; hanya bagian persen yang diawali baris baru ganda. Pemeriksaan persen
		 * dijalankan di dalam penjagaan {@code detailperkuliahan.getPerkuliahan() != null}, tetapi
		 * pembagian terhadap {@code getJumlahMaksimalPertemuan()} tidak dijaga terhadap nilai nol.</p>
		 *
		 * @param detailperkuliahan baris nilai mahasiswa yang diperiksa; semester dan data mahasiswanya
		 *                          dipakai untuk mencari konfigurasi berlapis.
		 * @param statuses          peta jumlah kehadiran per kode status hasil
		 *                          {@code Perkuliahan.hitungStatus(...)}, dengan kunci
		 *                          <code>A</code>, <code>S</code>, <code>I</code>, dan <code>T</code>.
		 * @return teks peringatan siap tampil; string kosong berarti tidak ada pelanggaran dan kotak
		 *         nilai tidak perlu dikunci.
		 * @see #checkWarningUas(Detailperkuliahan, Map)
		 * @see #statusPertemuanUts
		 */
		private String checkWarningUts(Detailperkuliahan detailperkuliahan, Map<String, Integer> statuses) {
			String warning = "";
			Integer semester = detailperkuliahan.getSemester();
			Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			if (aturanUts) {
				String ujian = "uts";
				int maxAlpa = 34;
				try {
					maxAlpa = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_alpa_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:174");

				}
				int maxSakit = 34;
				try {
					maxSakit = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_sakit_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:186");

				}
				int maxIzin = 34;
				try {
					maxIzin = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_izin_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:198");

				}

				int maxSemua = 34;
				try {
					maxSemua = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_semua_tidak_masuk_kuliah_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:211");

				}

				int maxPersen = 0;
				try {
					maxPersen = Integer.parseInt(Common.getKonfigurasi(
							"batas_maksimal_persen_tidak_masuk_kuliah_untuk_mengikuti_" + ujian.toLowerCase(), "0",
							semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
							mahasiswa.getStatusAwalMahasiswa()).getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:221");

				}

				if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {

					int semua = 0;

					int qtyAlpa = statuses.containsKey("A") ? statuses.get("A") : 0;
					semua += qtyAlpa;
					if (qtyAlpa >= maxAlpa) {
						warning += "Status Kehadiran A (Alpa) = " + qtyAlpa + " kali";
					}

					int qtySakit = statuses.containsKey("S") ? statuses.get("S") : 0;
					semua += qtySakit;
					if (qtySakit >= maxSakit) {
						warning += "Status Kehadiran S (Sakit) = " + qtySakit + " kali";
					}

					int qtyIzin = statuses.containsKey("I") ? statuses.get("I") : 0;
					semua += qtyIzin;
					if (qtyIzin >= maxIzin) {
						warning += "Status Kehadiran I (Izin) = " + qtyIzin + " kali";
					}

					if (semua >= maxSemua) {
						warning += "Tidak hadir kuliah = " + semua + " kali";
					}

					double persen = (semua * 100.0) / detailperkuliahan.getPerkuliahan().getJumlahMaksimalPertemuan();

					if (persen > maxPersen) {
						warning += "\n\nPerkuliahan " + detailperkuliahan.getPerkuliahan().toString()
								+ " => Persen tidak hadir kuliah = " + Common.numberFormat.get().format(persen) + "%";
					}
				}
			}

			return warning;
		}

		/**
		 * Menyusun <b>teks peringatan ketidakhadiran menjelang UAS</b> untuk satu mahasiswa. Metode ini
		 * adalah <b>salinan struktural persis</b> dari {@link #checkWarningUts(Detailperkuliahan, Map)}:
		 * lima ambang yang sama, sikap gagal-terbuka bawaan 34 yang sama, ambang persen bawaan 0 yang
		 * sama, pembanding {@code >=} yang sama, dan perakitan pesan tanpa pemisah yang sama. Seluruh
		 * catatan pada metode kembarannya berlaku di sini tanpa perubahan.
		 *
		 * <p><b>Yang membedakan hanyalah dua hal:</b> bendera penyalanya adalah {@link #aturanUas}, dan
		 * akhiran kunci konfigurasi yang dirakit adalah <code>uas</code>, bukan <code>uts</code>.
		 * Pemisahan ini memberi kampus dua rangkaian ambang yang benar-benar independen &mdash; batas
		 * kehadiran untuk ujian tengah semester lazimnya memang lebih longgar daripada ujian akhir,
		 * karena pada pertengahan semester jumlah pertemuan yang telah berlangsung masih separuh.</p>
		 *
		 * <p>Hasilnya dipasangkan dengan {@link #statusPertemuanUas} pada
		 * {@link #render(Row, Object)}: bila konfigurasi status pertemuan itu kosong, peringatan UAS
		 * mengunci seluruh kolom nilai; bila terisi, hanya kolom yang nama status pertemuannya cocok
		 * yang dikunci. Seorang mahasiswa dapat memicu peringatan UTS dan UAS sekaligus, dan kedua
		 * teksnya ditampilkan sebagai dua blok merah terpisah di bawah namanya.</p>
		 *
		 * <p><b>Catatan pemeliharaan.</b> Karena kedua metode ini sepenuhnya sejajar, setiap perbaikan
		 * pada salah satunya &mdash; misalnya menambahkan pemisah antarpesan, menjaga pembagian
		 * terhadap {@code getJumlahMaksimalPertemuan()} bernilai nol, atau menyeragamkan sikap ambang
		 * persen &mdash; wajib diterapkan pada keduanya. Menyatukannya menjadi satu metode
		 * berparameter jenis ujian akan menghapus risiko itu, tetapi belum dilakukan.</p>
		 *
		 * @param detailperkuliahan baris nilai mahasiswa yang diperiksa.
		 * @param statuses          peta jumlah kehadiran per kode status hasil
		 *                          {@code Perkuliahan.hitungStatus(...)}.
		 * @return teks peringatan siap tampil; string kosong berarti tidak ada pelanggaran.
		 * @see #checkWarningUts(Detailperkuliahan, Map)
		 * @see #statusPertemuanUas
		 */
		private String checkWarningUas(Detailperkuliahan detailperkuliahan, Map<String, Integer> statuses) {
			String warning = "";
			Integer semester = detailperkuliahan.getSemester();
			Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			if (aturanUas) {
				String ujian = "uas";
				int maxAlpa = 34;
				try {
					maxAlpa = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_alpa_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:278");

				}
				int maxSakit = 34;
				try {
					maxSakit = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_sakit_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:290");

				}
				int maxIzin = 34;
				try {
					maxIzin = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_izin_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:302");

				}

				int maxSemua = 34;
				try {
					maxSemua = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_semua_tidak_masuk_kuliah_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:315");

				}

				int maxPersen = 0;
				try {
					maxPersen = Integer.parseInt(Common.getKonfigurasi(
							"batas_maksimal_persen_tidak_masuk_kuliah_untuk_mengikuti_" + ujian.toLowerCase(), "0",
							semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
							mahasiswa.getStatusAwalMahasiswa()).getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:325");

				}

				if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {

					int semua = 0;

					int qtyAlpa = statuses.containsKey("A") ? statuses.get("A") : 0;
					semua += qtyAlpa;
					if (qtyAlpa >= maxAlpa) {
						warning += "Status Kehadiran A (Alpa) = " + qtyAlpa + " kali";
					}

					int qtySakit = statuses.containsKey("S") ? statuses.get("S") : 0;
					semua += qtySakit;
					if (qtySakit >= maxSakit) {
						warning += "Status Kehadiran S (Sakit) = " + qtySakit + " kali";
					}

					int qtyIzin = statuses.containsKey("I") ? statuses.get("I") : 0;
					semua += qtyIzin;
					if (qtyIzin >= maxIzin) {
						warning += "Status Kehadiran I (Izin) = " + qtyIzin + " kali";
					}

					if (semua >= maxSemua) {
						warning += "Tidak hadir kuliah = " + semua + " kali";
					}

					double persen = (semua * 100.0) / detailperkuliahan.getPerkuliahan().getJumlahMaksimalPertemuan();

					if (persen > maxPersen) {
						warning += "\n\nPerkuliahan " + detailperkuliahan.getPerkuliahan().toString()
								+ " => Persen tidak hadir kuliah = " + Common.numberFormat.get().format(persen) + "%";
					}
				}
			}
			return warning;
		}

		/**
		 * Gerbang tunggal yang menentukan apakah nilai komponen atau nilai huruf seorang mahasiswa
		 * BOLEH diubah. Dipakai oleh {@link #render(Row, Object)} pada KEDUA jalur entry nilai &mdash;
		 * kolom komponen ({@code formatNilai} bukan {@code null}) dan kotak nilai huruf mode
		 * {@code getHanyaInputNilaiHuruf()} ({@code formatNilai} bernilai {@code null}, karena kotak
		 * itu menulis ke SELURUH {@link #formatNilais} sekaligus).
		 *
		 * <p>Sebelumnya kedua jalur memakai syarat yang disalin terpisah dan perlahan menyimpang:
		 * jalur kotak huruf kehilangan {@link #editDisable} (kebijakan
		 * <code>hanya_dosen_yg_boleh_entry_nilai</code>) dan gerbang tunggakan
		 * {@link #mhsYgBelumBayarBelumBisaDiEntryNilai}, sehingga akun non-dosen tetap mendapat kotak
		 * isian nilai huruf walau kebijakan itu aktif. Method ini menyatukan sumbernya sehingga kedua
		 * jalur tidak bisa menyimpang lagi.</p>
		 *
		 * @param detailperkuliahan baris nilai yang diperiksa; dipakai untuk gerbang tunggakan
		 *                          pembayaran.
		 * @param formatNilai       komponen yang diperiksa penguncinya; {@code null} untuk jalur nilai
		 *                          huruf, yang membuat method ini memeriksa kunci SELURUH
		 *                          {@link #formatNilais} sekaligus &mdash; bila SATU SAJA terkunci,
		 *                          kotak huruf ikut dikunci, karena {@code populateDetailNilai} akan
		 *                          diam-diam melewati komponen terkunci itu sementara total tetap
		 *                          tertulis.
		 * @param warningUts        hasil {@link #checkWarningUts(Detailperkuliahan, Map)} baris ini.
		 * @param warningUas        hasil {@link #checkWarningUas(Detailperkuliahan, Map)} baris ini.
		 * @return {@code true} bila nilai TIDAK boleh diubah (harus tampil sebagai label baca-saja).
		 */
		private boolean nilaiTidakBolehDiubah(Detailperkuliahan detailperkuliahan, FormatNilai formatNilai,
				String warningUts, String warningUas) {
			if (editDisable || perkuliahan.getDikunci() != null || !edit
					|| (!warningUts.trim().isEmpty() && statusPertemuanUts.trim().isEmpty())
					|| (!warningUas.trim().isEmpty() && statusPertemuanUas.trim().isEmpty())
					|| (tbmuser.getMahasiswa() != null && !mahasiswaBolehUbahNilai)
					|| (mhsYgBelumBayarBelumBisaDiEntryNilai && !PenilaianMahasiswaHelper.checkBolehLihatNilai(
							detailperkuliahan.getMahasiswa(), detailperkuliahan.getSemester()))
					|| (!aktifPenilaian && (konfigurasi.getNilai() == null
							|| !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)))) {
				return true;
			}
			if (formatNilai != null) {
				return formatNilai.getKunci() != null || (formatNilai.getStatusPertemuan() != null
						&& formatNilai.getStatusPertemuan().getKunci());
			}
			for (FormatNilai fn : formatNilais) {
				if (fn.getKunci() != null
						|| (fn.getStatusPertemuan() != null && fn.getStatusPertemuan().getKunci())) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Membangun <b>satu baris utuh</b> layar Input Nilai untuk seorang mahasiswa: dari foto sampai
		 * kotak centang verifikasi, lengkap dengan seluruh listener yang menyimpan perubahan ke basis
		 * data. Inilah metode terpanjang dan paling berpengaruh di kelas ini &mdash; hampir setiap
		 * aturan penilaian yang dirasakan dosen di layar diputuskan di sini.
		 *
		 * <h3>Urutan sel yang dihasilkan</h3>
		 * <ol>
		 * <li><b>Foto</b> mahasiswa lewat {@code CommonMedia.tampilkanGambarKecil}.</li>
		 * <li><b>Identitas</b>: sebuah {@code Vbox} dari {@code RevisiHelper.createNewRevisi} sehingga
		 * NIM dapat diklik untuk melihat riwayat revisi baris ini, dengan nama mahasiswa di bawahnya.
		 * Bila {@link #checkWarningUts(Detailperkuliahan, Map)} atau
		 * {@link #checkWarningUas(Detailperkuliahan, Map)} menghasilkan teks, peringatan itu ditempel
		 * sebagai HTML merah di dalam kotak yang sama, dengan baris baru diterjemahkan menjadi
		 * {@code <br>}. Bila integrasi Feeder aktif, lencana &quot;Feeder valid&quot; atau
		 * &quot;Feeder blm valid&quot; ditambahkan, disusul tombol kirim-ke-Feeder dari
		 * {@code DetailperkuliahanHelper.kirimKeFeeder}.</li>
		 * <li><b>Semester</b> baris ini sebagai label sederhana.</li>
		 * <li><b>Rekap kehadiran</b>: rincian jumlah per kode status diambil dari peta {@code statuses}
		 * hasil {@code Perkuliahan.hitungStatus(statusPertemuan, mahasiswaId)}, ditampilkan
		 * berdampingan, diakhiri total {@code T} dan persentase kehadiran dari
		 * {@code detailperkuliahan.hitungPersenKehadiran()}.</li>
		 * <li><b>Kolom komponen nilai</b>, satu sel untuk setiap {@link FormatNilai} &mdash; bagian
		 * terpenting, diuraikan di bawah.</li>
		 * <li><b>Total</b> berupa angka dan huruf, yang diberi tautan popup analisis lewat
		 * {@code NilaiHurufAnalisisPopupHelper.pasangLink}.</li>
		 * <li><b>Verifikasi</b>: kotak centang &quot;semua&quot; per mahasiswa, atau sekadar label
		 * &quot;Ya/Belum&quot; bila pengguna adalah dosen yang tidak berhak memverifikasi nilainya
		 * sendiri.</li>
		 * </ol>
		 *
		 * <h3>Keputusan besar: sel nilai dapat diedit atau tidak</h3>
		 * <p>Untuk setiap komponen, sebuah syarat gabungan panjang menentukan apakah sel tampil sebagai
		 * <b>label baca-saja</b> atau sebagai <b>kotak isian</b>. Sel dikunci bila salah satu terpenuhi:
		 * {@link #editDisable} menyala; kelas sudah dikunci ({@code perkuliahan.getDikunci() != null});
		 * layar dibuka tanpa izin ubah ({@code !edit}); kolom itu sendiri dikunci
		 * ({@code formatNilai.getKunci() != null}); status pertemuan kolom itu dikunci; peringatan UTS
		 * muncul sementara {@link #statusPertemuanUts} kosong; peringatan UAS muncul sementara
		 * {@link #statusPertemuanUas} kosong; pengguna adalah mahasiswa yang bukan asisten penilai;
		 * mahasiswa belum melunasi tagihan sementara kebijakan
		 * {@link #mhsYgBelumBayarBelumBisaDiEntryNilai} menyala; atau periode penilaian tertutup dan
		 * {@link #aktifPenilaian} mati.</p>
		 *
		 * <p>Pada <b>cabang terkunci</b>, sel diisi label &quot;Load..&quot; yang nilainya diisi
		 * belakangan secara asinkron oleh {@code NilaiLoader.startLoad}. Perlu dicatat bahwa cabang ini
		 * tetap membuat sebuah {@code MyDoublebox} beserta {@code PerubahanNilaiListener}-nya, namun
		 * kotak itu <b>tidak pernah dipasang ke baris</b>; ia hanya diperlukan sebagai wadah oleh
		 * listener verifikasi dan tidak pernah sampai ke peramban. Pada <b>cabang terbuka</b>, sebuah
		 * {@code MyDoublebox} dipasang, diberi listener {@code onChange} berupa
		 * {@code PerubahanNilaiListener}, dan diisi asinkron oleh {@code NilaiLoader}.</p>
		 *
		 * <p>Bila {@link #adaProsesVerifikasiNilai} menyala, setiap sel nilai berbagi tempat dengan
		 * kotak centang verifikasi per komponen. Kotak itu dinonaktifkan bila kelas terkunci atau bila
		 * pengguna adalah dosen yang kelasnya tidak mengizinkan verifikasi mandiri; jika tidak, ia
		 * memasang listener yang menyegarkan entitas, memeriksa apakah <i>semua</i> komponen sudah
		 * tercentang, menulis status {@link Detailperkuliahan#VERIFIED} atau
		 * {@link Detailperkuliahan#NOT_VERIFIED} beserta identitas verifikator dan waktunya, lalu
		 * menyimpan dalam transaksi tersendiri.</p>
		 *
		 * <h3>Mode &quot;hanya input nilai huruf&quot;</h3>
		 * <p>Bila {@code perkuliahan.getHanyaInputNilaiHuruf()} menyala, seluruh kolom komponen
		 * disembunyikan dan digantikan satu kotak teks huruf. Nilai angka disintesis dari
		 * <b>titik tengah rentang</b> huruf yang diketik: {@code (mulai + sampai) / 2}. Pencarian
		 * aturan huruf berjenjang tiga tingkat pada {@code ConstantValues.nilaiHurufs} &mdash; cocok
		 * jurusan mahasiswa, lalu cocok fakultasnya, lalu <b>cocok huruf saja tanpa memandang
		 * jurusan/fakultas mana pun</b>. Tingkat ketiga itu membuat skala penilaian milik program studi
		 * lain dapat terpakai bila prodi mahasiswa belum punya definisi huruf sendiri.</p>
		 *
		 * <p>Setelah huruf ditemukan, listener menulis nilai hasil sintesis ke <i>seluruh</i> komponen
		 * melalui {@code populateDetailNilai}, memasang {@code totalIP}, {@code totalNilai}, dan
		 * {@code nilaiHuruf}, lalu menghitung ulang trio kolom &quot;sementara&quot; dan menyimpannya
		 * dalam satu transaksi. Sebelum semua itu, gerbang pembayaran semester pendek
		 * {@code GateBayarSpUtil.alasanBlokir} dijalankan dan menghentikan penyimpanan bila mahasiswa
		 * belum lunas.</p>
		 *
		 * <p><b>Perbedaan gerbang yang perlu diketahui.</b> Syarat yang mengunci kotak huruf ini
		 * <b>lebih pendek</b> daripada syarat yang mengunci kolom komponen: ia tidak menyertakan
		 * {@link #editDisable}, tidak menyertakan {@code formatNilai.getKunci()}, dan tidak
		 * menyertakan kebijakan {@link #mhsYgBelumBayarBelumBisaDiEntryNilai}. Akibatnya, pada kelas
		 * yang memakai mode ini, pengguna non-dosen tetap memperoleh kotak isian meskipun kebijakan
		 * <code>hanya_dosen_yg_boleh_entry_nilai</code> sedang aktif. Perlindungan terakhir tetap ada
		 * di lapisan model &mdash; {@code populateDetailNilai} menolak menulis komponen terkunci dan
		 * setter ringkasan menolak menulis saat kunci global aktif &mdash; sehingga yang bocor adalah
		 * gerbang <i>siapa</i>, bukan gerbang <i>kapan</i>.</p>
		 *
		 * <h3>Sel Total dan peringatan &quot;nilai 0&quot;</h3>
		 * <p>Angka yang ditampilkan mengikuti kebijakan penyembunyian: bila kelas menyembunyikan nilai
		 * yang belum diverifikasi dan baris masih {@code NOT_VERIFIED}, yang tampil adalah pasangan
		 * kolom sementara. Label dan peringatan dibungkus dalam <b>satu</b> {@code Vbox} agar keduanya
		 * jatuh pada sel yang sama; sebelumnya peringatan dipasang sebagai sel tambahan dan terdorong
		 * ke kolom sempit di ujung baris sehingga tak terbaca. Bila total di bawah 0,01 padahal
		 * komponen sudah terisi, {@code detailperkuliahan.alasanNilaiJadiNol(...)} dipanggil untuk
		 * menjelaskan sebabnya &mdash; komponen terkunci ber-snapshot nol, bobot persen kosong,
		 * kehadiran di bawah minimal, atau aturan nilai 0 &mdash; sehingga dosen tahu tindakan apa yang
		 * harus diambil.</p>
		 *
		 * <h3>Efek samping yang tidak terduga dari sebuah perender</h3>
		 * <p>Metode ini <b>menulis</b>, bukan sekadar menggambar. Ia memperbarui {@link #tbmuser} milik
		 * kelas induk di tengah jalan; ia mengubah lebar {@link #columnMahasiswa} dan visibilitas
		 * seluruh {@link #columns} berdasarkan mode nilai huruf &mdash; artinya kolom grid ditata ulang
		 * setiap kali sebuah baris dirender, bukan sekali saat grid dibangun; dan bila kolom aturan
		 * nilai 0 pada {@link Perkuliahan} masih {@code null}, ia mengisinya dengan
		 * {@link #nilai0MasukPenghitungan} pada objek dalam memori. Ia juga menyetel ulang keadaan
		 * tercentang keempat kotak centang toolbar dari nilai {@link #perkuliahan} pada setiap baris.
		 * Semua listener yang dipasang membuka transaksi Hibernate sendiri tanpa penguncian baris,
		 * sehingga dua penilai yang bekerja bersamaan pada kelas yang sama dapat saling menimpa.</p>
		 *
		 * @param row  baris grid yang akan diisi; setiap komponen yang di-{@code setParent} ke sini
		 *             menjadi satu sel, sehingga <b>jumlah dan urutan</b> pemasangan harus sepadan
		 *             dengan definisi kolom yang dibangun {@link #prosesDisplay}.
		 * @param data id {@link Detailperkuliahan} dalam bentuk objek; diubah menjadi teks lalu
		 *             diselesaikan menjadi entitas melalui {@code GeneralValueObject.ambilData}.
		 * @throws Exception bila pembangunan komponen atau pembacaan data gagal; ZK akan menampilkan
		 *                   galat render kepada pengguna.
		 * @see ais.action.master.helper.util.PerubahanNilaiListener
		 * @see ais.action.master.helper.util.NilaiLoader
		 * @see Detailperkuliahan#populateDetailNilai
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");

			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, data.toString());

			detailperkuliahan.getPerkuliahan();
			Map<String, Integer> statuses = Perkuliahan.hitungStatus(statusPertemuan,
					detailperkuliahan.getMahasiswa().getId());

			CommonMedia.tampilkanGambarKecil(detailperkuliahan.getMahasiswa()).setParent(row);

			Vbox vbox = RevisiHelper.createNewRevisi(Detailperkuliahan.class, detailperkuliahan,
					detailperkuliahan.getMahasiswa().getNim());
			new Label(detailperkuliahan.getMahasiswa().getNama()).setParent(vbox);
			vbox.setParent(row);

			String warningUts = checkWarningUts(detailperkuliahan, statuses);
			if (!warningUts.trim().isEmpty()) {
				new ais.ui.util.MyHtml("<font style=\"font-weight:bold;color:red;font-size: 9px;\">"
						+ warningUts.replaceAll("\n", "<br>") + "</font>").setParent(vbox);
			}
			String warningUas = checkWarningUas(detailperkuliahan, statuses);
			if (!warningUas.trim().isEmpty()) {
				new ais.ui.util.MyHtml("<font style=\"font-weight:bold;color:red;font-size: 9px;\">"
						+ warningUas.replaceAll("\n", "<br>") + "</font>").setParent(vbox);
			}

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);
			if (Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
				if (detailperkuliahan.getFeeder() != null && !detailperkuliahan.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}

			tbmuser = Common.getCurrentUser();
			DetailperkuliahanHelper.kirimKeFeeder(tbmuser, detailperkuliahan, DetailperkuliahanForPenilaianHelper.this,
					myHbox, false);

			new Label(detailperkuliahan.getSemester() + "").setParent(row);

			vbox = new Vbox();
			vbox.setParent(row);

			int semua = statuses.get("T") == null ? 0 : statuses.get("T");

			Hbox hbox = new Hbox();
			vbox.appendChild(hbox);
			for (String key : statuses.keySet()) {
				if (!key.equals("T")) {
					int v = statuses.get(key);
					hbox.appendChild(new MyLabelAgakKecil(key + "=" + v + ","));
				}
			}

			hbox.appendChild(new MyLabelAgakKecil("T=" + semua));

			double persen = detailperkuliahan.hitungPersenKehadiran();

			vbox.appendChild(new MyLabelAgakKecil("Presensi = " + Common.numberFormat.get().format(persen) + "%"));

			final Label label = new Label(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
					+ detailperkuliahan.getNilaiHuruf() + ")");
			ais.ui.util.NilaiHurufAnalisisPopupHelper.pasangLink(label, detailperkuliahan, perkuliahan, formatNilais);
			final MyCheckboxConfig verifyAll = new MyCheckboxConfig();
			final List<PerubahanNilaiListener> checkboxs = new ArrayList<PerubahanNilaiListener>();
			for (FormatNilai formatNilai : formatNilais) {
				MyCheckboxConfig verify = new MyCheckboxConfig();
				if (nilaiTidakBolehDiubah(detailperkuliahan, formatNilai, warningUts, warningUas)) {

					MyDoublebox doublebox = new MyDoublebox();
					NilaiLoader.startLoad(detailperkuliahan, formatNilai, doublebox);
					final PerubahanNilaiListener perubahanNilaiListener = new PerubahanNilaiListener(detailperkuliahan,
							formatNilai, formatNilais, onPerubahanNilai, label, doublebox, verify);

					Label myLabel = new Label(ais.common.Common.getBahasaConfig("Load.."));
					myLabel.setStyle("text-align: right;");
					NilaiLoader.startLoad(detailperkuliahan, formatNilai, myLabel);
					if (!adaProsesVerifikasiNilai) {
						myLabel.setParent(row);
					} else {
						verify.setChecked(detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai));
						checkboxs.add(perubahanNilaiListener);
						hbox = new Hbox();
						hbox.setWidth("95%");
						hbox.setParent(row);
						myLabel.setParent(hbox);
						verify.setParent(hbox);
						if (perkuliahan.getDikunci() != null
								|| !perkuliahan.getDosenBolehVerifikasiNilaiSendiri() && dosen != null) {
							verify.setDisabled(true);
						} else {
							verify.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									try {

										Session session = HibernateUtil.currentNativeSession();
										session.refresh(detailperkuliahan);
										Boolean checkSemua = true;
										for (PerubahanNilaiListener perubahanNilaiListener : checkboxs) {
											if (!perubahanNilaiListener.getVerify().isChecked()) {
												checkSemua = false;
												break;
											}

											if (perubahanNilaiListener.getDoublebox() == null) {
												checkSemua = false;
												break;
											}
										}
										verifyAll.setChecked(checkSemua);
										detailperkuliahan.setVerify(verifyAll.isChecked() ? Detailperkuliahan.VERIFIED
												: Detailperkuliahan.NOT_VERIFIED);
										Tbmuser tbmuser = Common.getCurrentUser();
										detailperkuliahan
												.setVerifikator(tbmuser.getUserId() + " " + tbmuser.getUserNama());
										detailperkuliahan.setWaktuVerifikasi(ais.ui.util.WaktuUtil.getDate());
										session.getTransaction().begin();
										Common.refreshUpdate(session, detailperkuliahan);
										session.getTransaction().commit();
										// session.disconnect();
										if (session.isOpen()) {
											session.disconnect();
											session.close();
										}

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:515");
									}
									HibernateUtil.closeSession();

									Common.createDefaultTimer(perubahanNilaiListener);
								}
							});
						}
					}

				} else {

					MyDoublebox doublebox = new MyDoublebox();
					final PerubahanNilaiListener perubahanNilaiListener = new PerubahanNilaiListener(detailperkuliahan,
							formatNilai, formatNilais, onPerubahanNilai, label, doublebox, verify);

					/* width:85% !important agar kotak nilai mengikuti lebar kolom (lihat MyDoublebox). */
					doublebox.setStyle("text-align: right; width:85% !important;");
					doublebox.setWidth("95%");
					if (!adaProsesVerifikasiNilai) {

						if ((!warningUts.trim().isEmpty() && statusPertemuanUts.trim()
								.equalsIgnoreCase(formatNilai.getStatusPertemuan().getNama()))
								|| (!warningUas.trim().isEmpty() && statusPertemuanUas.trim()
										.equalsIgnoreCase(formatNilai.getStatusPertemuan().getNama()))) {
							Label myLabel = new Label(ais.common.Common.getBahasaConfig("Load.."));
							myLabel.setStyle("text-align: right;");
							NilaiLoader.startLoad(detailperkuliahan, formatNilai, myLabel);
							myLabel.setParent(row);
						} else {
							doublebox.setParent(row);
						}

					} else {
						verify.setChecked(detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai));
						checkboxs.add(perubahanNilaiListener);
						hbox = new Hbox();
						hbox.setWidth("95%");
						hbox.setParent(row);

						if ((!warningUts.trim().isEmpty() && statusPertemuanUts.trim()
								.equalsIgnoreCase(formatNilai.getStatusPertemuan().getNama()))
								|| (!warningUas.trim().isEmpty() && statusPertemuanUas.trim()
										.equalsIgnoreCase(formatNilai.getStatusPertemuan().getNama()))) {
							Label myLabel = new Label(ais.common.Common.getBahasaConfig("Load.."));
							myLabel.setStyle("text-align: right;");
							NilaiLoader.startLoad(detailperkuliahan, formatNilai, myLabel);
							myLabel.setParent(hbox);
						} else {
							doublebox.setParent(hbox);
						}
						verify.setParent(hbox);
						if (perkuliahan.getDikunci() != null
								|| !perkuliahan.getDosenBolehVerifikasiNilaiSendiri() && dosen != null) {
							verify.setDisabled(true);
						} else {
							verify.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									try {
										Session session = HibernateUtil.currentNativeSession();
										session.refresh(detailperkuliahan);
										Boolean checkSemua = true;
										for (PerubahanNilaiListener perubahanNilaiListener : checkboxs) {
											if (!perubahanNilaiListener.getVerify().isChecked()) {
												checkSemua = false;
												break;
											}

											if (perubahanNilaiListener.getDoublebox() == null) {
												checkSemua = false;
												break;
											}
										}
										verifyAll.setChecked(checkSemua);
										detailperkuliahan.setVerify(verifyAll.isChecked() ? Detailperkuliahan.VERIFIED
												: Detailperkuliahan.NOT_VERIFIED);
										Tbmuser tbmuser = Common.getCurrentUser();
										detailperkuliahan
												.setVerifikator(tbmuser.getUserId() + " " + tbmuser.getUserNama());
										detailperkuliahan.setWaktuVerifikasi(ais.ui.util.WaktuUtil.getDate());
										session.getTransaction().begin();
										Common.refreshUpdate(session, detailperkuliahan);
										session.getTransaction().commit();
										// session.disconnect();
										if (session.isOpen()) {
											session.disconnect();
											session.close();
										}

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:607");
									}
									HibernateUtil.closeSession();

									Common.createDefaultTimer(perubahanNilaiListener);
								}
							});
						}
					}

					doublebox.setDisabled((!edit || !aktifPenilaian)
							&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)));
					doublebox.addEventListener("onChange", perubahanNilaiListener);
					NilaiLoader.startLoad(detailperkuliahan, formatNilai, doublebox);
				}
			}

			for (Column column : columns) {
				column.setVisible(!perkuliahan.getHanyaInputNilaiHuruf());
			}

			if (perkuliahan.getHanyaInputNilaiHuruf()) {
				columnMahasiswa.setWidth("85%");

				if (nilaiTidakBolehDiubah(detailperkuliahan, null, warningUts, warningUas)) {

					ais.ui.util.NilaiHurufAnalisisPopupHelper.buatLabel(detailperkuliahan.getNilaiHuruf(), detailperkuliahan)
							.setParent(row);

				} else {

					final Textbox nilaiHurufText = new Textbox(detailperkuliahan.getNilaiHuruf());
					nilaiHurufText.setWidth("95%");
					nilaiHurufText.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Double nilai = 0.0;
							// GATE SP (semester pendek): tolak entry nilai bila pembayaran SP mahasiswa belum lunas.
							String alasanSpNilaiHuruf = ais.action.master.helper.util.GateBayarSpUtil.alasanBlokir(detailperkuliahan);
							if (alasanSpNilaiHuruf != null) {
								try {
									ais.ui.util.MyMessageboxConfig.show(alasanSpNilaiHuruf, "Peringatan", ais.ui.util.MyMessageboxConfig.OK,
											ais.ui.util.MyMessageboxConfig.EXCLAMATION);
								} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:658");
								}
								return;
							}
							Session session = HibernateUtil.currentNativeSession();
							session.refresh(detailperkuliahan);
							// Cari aturan konversi huruf->angka lewat indeks terpusat (prioritas per Jurusan ->
							// Fakultas -> global murni), sama seperti ConstantValues.lulusDariNilaiHuruf() dkk --
							// menggantikan 3 loop tangan yang sebelumnya disalin-tempel di sini (tier terakhirnya
							// sempat tanpa syarat cakupan sama sekali, lihat r86082).
							NilaiHuruf nilaiHuruf = ConstantValues.nilaiHurufTerkait(nilaiHurufText.getValue().trim(),
									detailperkuliahan.getMahasiswa());

							if (nilaiHuruf != null) {
								nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
							}
							for (FormatNilai formatNilai : formatNilais) {
								detailperkuliahan.populateDetailNilai(formatNilai, null, nilai,
										detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai),
										perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(), tbmuser);
							}
							detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
							detailperkuliahan.setTotalNilai(nilai);
							detailperkuliahan.setNilaiHuruf(nilaiHurufText.getValue().trim());

							Matakuliah matakuliah = detailperkuliahan == null ? null
									: detailperkuliahan.getPerkuliahan() != null
											? detailperkuliahan.getPerkuliahan().getMatakuliah()
											: detailperkuliahan.getMatakuliahKonversi();

							Double totalSementara = nilai;
							nilaiHuruf = Common.getNilaiHuruf(totalSementara,
									detailperkuliahan.getMahasiswa().getTahunangkatan(),
									detailperkuliahan.getMahasiswa().getJurusan(),
									detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
									detailperkuliahan.getTahunAkademik(),
									detailperkuliahan.getPerkuliahan() == null ? null
											: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
									matakuliah == null ? "" : matakuliah.getKode(),
									matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

							detailperkuliahan.setTotalNilaiSementara(totalSementara);
							detailperkuliahan
									.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
							detailperkuliahan
									.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

							session.getTransaction().begin();
							Common.refreshUpdate(session, detailperkuliahan);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
							HibernateUtil.closeSession();
						}
					});
					nilaiHurufText.setParent(row);
				}
			} else {
				columnMahasiswa.setWidth((75 - (formatNilais.size() * 5)) + "%");
				if (perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					label.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilaiSementara()) + " ("
							+ detailperkuliahan.getNilaiHurufSementara() + ")");
				} else {
					label.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
							+ detailperkuliahan.getNilaiHuruf() + ")");
				}

				// SEL TOTAL: label nilai + (bila perlu) peringatan merah digabung dalam SATU sel.
				// Sebelumnya peringatan di-setParent(row) sebagai SEL TAMBAHAN di ujung baris —
				// jatuh ke kolom sempit sehingga teks berdesakan tak terbaca (laporan dosen 19-08).
				org.zkoss.zul.Vbox selTotal = new org.zkoss.zul.Vbox();
				selTotal.setStyle("width:100%;");
				label.setParent(selTotal);

				// PERINGATAN MERAH: bila Nilai Total = 0 PADAHAL komponen sudah di-entry, jelaskan
				// penyebabnya (komponen terkunci ber-snapshot 0, bobot persen 0/kosong, kehadiran
				// di bawah minimal, atau aturan "nilai 0 tak dihitung"). Membantu dosen paham
				// kenapa nilai akhir "tidak sesuai" dan APA tindakannya.
				try {
					double totalTampil = (perkuliahan != null
							&& perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
							&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED))
									? detailperkuliahan.getTotalNilaiSementara()
									: detailperkuliahan.getTotalNilai();
					if (totalTampil < 0.01) {
						String alasan = detailperkuliahan.alasanNilaiJadiNol(true, formatNilais);
						if (alasan != null && !alasan.trim().isEmpty()) {
							org.zkoss.zul.Label peringatan = new org.zkoss.zul.Label(alasan);
							peringatan.setMultiline(true);
							peringatan.setStyle(
									"color:#c62828;font-weight:bold;font-size:10px;line-height:1.35;display:block;"
											+ "margin-top:3px;white-space:normal;word-wrap:break-word;max-width:230px;");
							peringatan.setParent(selTotal);
						}
					}
				} catch (Exception eWarn) {
					Common.tampilErrorJikaAdmin(eWarn);
				}
				selTotal.setParent(row);
			}

			if (perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir() == null) {
				perkuliahan.setNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir(nilai0MasukPenghitungan);
			}

			nilai0masukNilaiAkhir.setChecked(perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir());
			jikaNilai0masukNilaiAkhir.setChecked(perkuliahan.getJikaAdaNilai0TidakMenghitungNilaiAkhir());
			hanyaInputNilaiHuruf.setChecked(perkuliahan.getHanyaInputNilaiHuruf());

			if (dosen == null || (dosen != null && perkuliahan.getDosenBolehVerifikasiNilaiSendiri())) {

				verifyAll.setChecked(detailperkuliahan.getVerify().equals(Detailperkuliahan.VERIFIED));
				verifyAll.setParent(row);
				verifyAll.setAttribute("janganDisabled", true);
				verifyAll.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {

							for (PerubahanNilaiListener perubahanNilaiListener : checkboxs) {
								perubahanNilaiListener.getVerify().setChecked(verifyAll.isChecked());
								perubahanNilaiListener.process();
							}

							Session session = HibernateUtil.currentNativeSession();
							session.refresh(detailperkuliahan);
							detailperkuliahan.setVerify(verifyAll.isChecked() ? Detailperkuliahan.VERIFIED
									: Detailperkuliahan.NOT_VERIFIED);
							Tbmuser tbmuser = Common.getCurrentUser();
							detailperkuliahan.setVerifikator(tbmuser.getUserId() + " " + tbmuser.getUserNama());
							detailperkuliahan.setWaktuVerifikasi(ais.ui.util.WaktuUtil.getDate());
							session.getTransaction().begin();
							Common.refreshUpdate(session, detailperkuliahan);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:825");
						}
						HibernateUtil.closeSession();
					}
				});
			} else {

				Label labelVerifikasi;
				(labelVerifikasi = new Label(
						detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED) ? "Belum" : "Ya"))
						.setParent(row);
				labelVerifikasi.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
			}
		}

	}

	/**
	 * Kumpulan id {@link Detailperkuliahan} hasil pencarian terakhir oleh {@link #loadData(Object)}.
	 * Sengaja disimpan sebagai daftar <b>id</b>, bukan entitas, karena kelas ini kerap memuat ratusan
	 * baris dan menahan seluruh objeknya akan membebani memori serta menyimpan entitas basi.
	 *
	 * <p>Field ini merangkap peran sebagai <b>daftar sasaran operasi massal</b>: tombol Hitung Ulang,
	 * Masukkan Nilai Absen, Reset, Ambil Nilai dari Feeder, Restore, dan seluruh pemanggilan
	 * {@code Common.realoadNilai*} menerimanya sebagai lingkup kerja. Konsekuensinya, operasi massal
	 * bekerja pada <b>hasil pencarian yang sedang tampak</b>, bukan pada seluruh kelas &mdash; bila
	 * dosen sedang menyaring dengan kotak {@link #nama}, operasi massal hanya menyentuh mahasiswa yang
	 * tersaring.</p>
	 *
	 * <p><b>Urutan inisialisasi.</b> Field ini masih {@code null} sampai {@link #loadData(Object)}
	 * berjalan pertama kali. {@link #display} sudah meneruskannya ke
	 * {@code Common.realoadNilaiLangsung} sebelum grid pernah dimuat, sehingga pada pembukaan pertama
	 * yang dikirim adalah {@code null} dan pemanggil di sisi sana harus menanganinya.</p>
	 */
	private Collection<Long> detailperkuliahans;

	/**
	 * Mengisi ulang grid {@link #grid} dengan daftar mahasiswa yang mengikuti {@link #perkuliahan},
	 * menerapkan penyaringan nama/NIM yang sedang diketik, dan memasang kembali perendernya. Inilah
	 * implementasi kontrak {@link DataLoader} milik kelas ini, sehingga helper lain dapat memicu
	 * penyegaran layar penilaian tanpa mengetahui isinya.
	 *
	 * <h3>Arti parameter yang tidak biasa</h3>
	 * <p>Parameter {@code value} <b>bukan</b> data yang akan dimuat, melainkan sebuah bendera
	 * penyegaran terselubung: ia diperlakukan sebagai {@code true} hanya bila tidak {@code null} dan
	 * sama dengan {@link Boolean#TRUE}. Bentuk longgar ini adalah harga dari tanda tangan
	 * {@link DataLoader} yang generik. Nilai {@code true} diteruskan ke
	 * {@code perkuliahan.ambilDetailperkuliahan(...)} sebagai perintah <b>melewati cache</b> dan
	 * membaca ulang dari basis data; {@code null} atau nilai lain memakai cache. Pemanggil yang baru
	 * saja menulis nilai wajib mengirim {@code true}, jika tidak layar akan menampilkan data basi.</p>
	 *
	 * <h3>Penyaringan dua lapis</h3>
	 * <p>Lapis pertama terjadi di basis data: kata kunci dari {@link #nama} dan pilihan
	 * {@link #urutkanBerdasarkanNama} diserahkan ke {@code ambilDetailperkuliahan}. Lapis kedua terjadi
	 * di memori: setiap id diselesaikan menjadi entitas, dan <b>hanya baris berstatus
	 * {@link Detailperkuliahan#DISETUJUI}</b> yang masuk ke model grid. Mahasiswa yang KRS-nya belum
	 * disetujui atau sudah dibatalkan karena itu tidak pernah muncul di layar penilaian, meskipun
	 * barisnya ada di basis data.</p>
	 *
	 * <p>Perhatikan bahwa {@link #detailperkuliahans} menyimpan hasil <b>lapis pertama</b> &mdash;
	 * sebelum penyaringan persetujuan &mdash; sedangkan yang tampil di grid adalah hasil lapis kedua.
	 * Karena operasi massal mengambil lingkupnya dari {@link #detailperkuliahans}, operasi seperti
	 * Reset dan Hitung Ulang dapat menyentuh baris yang <b>tidak terlihat</b> di layar. Baris yang
	 * gagal diselesaikan menjadi entitas ({@code null}) dilewati diam-diam.</p>
	 *
	 * <h3>Pemasangan ulang perender</h3>
	 * <p>Sebuah {@link DetailPerkuliahanRenderer} <b>baru</b> dibuat setiap pemanggilan. Ini disengaja
	 * dan penting: perender membaca konfigurasi batas ketidakhadiran pada saat instansiasi, sehingga
	 * membuatnya ulang memastikan aturan yang dipakai selalu segar. Model dipasang lewat
	 * {@code setModelCheckMobile} yang menyesuaikan perilaku untuk peramban ponsel, dan grid diberi
	 * kelas gaya {@code fgrid}.</p>
	 *
	 * <p><b>Efek samping penutup.</b> {@code Common.freeze(grid, ...)} membekukan seluruh grid bila
	 * {@code perkuliahan.getDikunci()} tidak {@code null}, sehingga status kunci selalu tercermin
	 * setelah pemuatan ulang apa pun. Metode ini tidak membuka transaksi sendiri dan tidak menangkap
	 * galat &mdash; kegagalan pembacaan akan naik ke pemanggil.</p>
	 *
	 * @param value bendera penyegaran; kirim {@link Boolean#TRUE} untuk memaksa pembacaan ulang dari
	 *              basis data, {@code null} untuk memakai cache.
	 * @see #detailperkuliahans
	 * @see DetailPerkuliahanRenderer
	 */
	public void loadData(Object value) {
		boolean refresh = (value != null && value.equals(true));
		detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, nama.getValue().trim(),
				urutkanBerdasarkanNama.isChecked(), refresh);

		List<Long> baru = new ArrayList<Long>();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
					baru.add(detailperkuliahan.getId());
				}
			}
		}
		ListModel strset = new SimpleListModel(baru);
		grid.setSclass("fgrid");
		grid.setRowRenderer(new DetailPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

		Common.freeze(grid, perkuliahan.getDikunci() != null);
	}

	/**
	 * Mengisi grid tab <i>Asisten Dosen</i> dengan daftar {@link MahasiswaJadiAsisten} pada satu
	 * perkuliahan, lengkap dengan kendali sunting langsung di setiap baris. Asisten dosen adalah
	 * mahasiswa yang diberi wewenang membantu pengelolaan kelas &mdash; dan, bila kotak
	 * &quot;Nilai&quot; dicentang, wewenang <b>mengisi nilai temannya</b> lewat
	 * {@link #mahasiswaBolehUbahNilai}. Karena itu grid ini adalah tempat pemberian hak istimewa,
	 * bukan sekadar daftar administratif.
	 *
	 * <h3>Sifat statis dan alasannya</h3>
	 * <p>Metode ini {@code static} dan menerima {@code perkuliahan} serta {@code gridDetailAsisten}
	 * sebagai parameter, bukan membacanya dari state instance. Itu memungkinkan
	 * {@link #displayAsistenMahasiswa(Component, Perkuliahan)} &mdash; yang juga statis &mdash;
	 * membangun tab ini tanpa memerlukan instance helper. Konsekuensinya, semua listener di dalamnya
	 * menangkap kedua parameter itu sebagai variabel {@code final} agar dapat memuat ulang dirinya
	 * sendiri secara rekursif setelah data berubah.</p>
	 *
	 * <h3>Penyegaran cache</h3>
	 * <p>Bila {@code refresh} bernilai {@code true}, {@code perkuliahan.belum("mahasiswaJadiAsisten")}
	 * dipanggil lebih dulu untuk membatalkan cache asosiasi tersebut, sehingga
	 * {@code ambilMahasiswaJadiAsisten()} berikutnya benar-benar membaca ulang dari basis data.
	 * Tanpa itu, asisten yang baru ditambahkan atau dihapus tidak akan tampak.</p>
	 *
	 * <h3>Kendali per baris</h3>
	 * <p>Setiap baris menampilkan NIM yang tertaut ke riwayat revisi, nama mahasiswa, lalu tiga kotak
	 * centang dan satu kotak teks yang <b>menyimpan seketika saat diubah</b>, tanpa tombol simpan dan
	 * tanpa konfirmasi:</p>
	 * <ul>
	 * <li><b>Nilai</b> &mdash; {@code setInputNilai}: memberi asisten hak mengisi nilai.</li>
	 * <li><b>Absen</b> &mdash; {@code setInputAbsen}: memberi hak mengisi presensi.</li>
	 * <li><b>Aktif</b> &mdash; {@code setAktif}: mengaktifkan atau menonaktifkan penugasan.</li>
	 * <li><b>Keterangan</b> &mdash; {@code setKeterangan} pada peristiwa {@code onChange}.</li>
	 * </ul>
	 * <p>Ketiga kotak centang memakai {@code Common.refreshSaveOrUpdate}, sedangkan kotak keterangan
	 * memakai {@code Common.refreshUpdate}. Tidak ada pemeriksaan wewenang di dalam listener mana pun:
	 * penjagaan satu-satunya adalah visibilitas toolbar, yang disembunyikan bila
	 * {@code Common.getCurrentUser()} bernilai {@code null}. Pemberian hak penilaian di sini karena
	 * itu bergantung sepenuhnya pada Action yang membuka tab ini.</p>
	 *
	 * <h3>Penghapusan</h3>
	 * <p>Tombol tempat sampah meminta konfirmasi lebih dulu, lalu memanggil
	 * {@code Common.refreshDelete}. Kegagalan &mdash; lazimnya karena baris masih dirujuk data lain
	 * &mdash; ditangkap dan diterjemahkan menjadi pesan ramah lewat
	 * {@code PesanFormalHelper.tampilkanGagalException} yang menjelaskan sebab kendala relasi beserta
	 * langkah yang dapat ditempuh pengguna, bukan menampilkan jejak tumpukan. Setelah berhasil, grid
	 * dimuat ulang lewat {@code Common.createDefaultTimer} agar penyegaran terjadi setelah siklus
	 * peristiwa saat ini tuntas.</p>
	 *
	 * <p>Diakhiri {@code renderAll()} sehingga seluruh baris dibangun serentak, bukan malas per
	 * halaman &mdash; ukuran halaman grid ini memang dipasang 1000, jauh di atas jumlah asisten yang
	 * masuk akal.</p>
	 *
	 * @param value             tidak dipakai; ada semata agar tanda tangan metode ini menyerupai pola
	 *                          {@link DataLoader} yang dipakai di seluruh modul, sehingga pemanggil
	 *                          dapat meneruskannya begitu saja. Kirim {@code null}.
	 * @param perkuliahan       kelas yang daftar asistennya ditampilkan; ditangkap oleh listener untuk
	 *                          pemuatan ulang.
	 * @param gridDetailAsisten grid tujuan; isinya diganti seluruhnya.
	 * @param refresh           {@code true} untuk membatalkan cache asosiasi asisten sebelum membaca.
	 * @see #displayAsistenMahasiswa(Component, Perkuliahan)
	 * @see #mahasiswaBolehUbahNilai
	 */
	public static void loadDataDetailAsisten(Object value, final Perkuliahan perkuliahan,
			final MyGrid gridDetailAsisten, boolean refresh) {

		if (refresh) {
			perkuliahan.belum("mahasiswaJadiAsisten");
		}

		List<MahasiswaJadiAsisten> mahasiswaJadiAsistens = perkuliahan.ambilMahasiswaJadiAsisten();

		ListModel strset = new SimpleListModel(mahasiswaJadiAsistens);
		gridDetailAsisten.setRowRenderer(new ais.ui.util.MyRowRenderer() {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				final MahasiswaJadiAsisten mahasiswaJadiAsisten = (MahasiswaJadiAsisten) arg1;

				RevisiHelper.createNewRevisi(MahasiswaJadiAsisten.class, mahasiswaJadiAsisten,
						mahasiswaJadiAsisten.getMahasiswa().getNim()).setParent(arg0);
				new Label(mahasiswaJadiAsisten.getMahasiswa().getNama()).setParent(arg0);

				final MyCheckboxConfig inputNilai = new MyCheckboxConfig("Nilai");
				inputNilai.setChecked(mahasiswaJadiAsisten.getInputNilai());
				inputNilai.setParent(arg0);
				inputNilai.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaJadiAsisten.setInputNilai(inputNilai.isChecked());
						Common.refreshSaveOrUpdate(mahasiswaJadiAsisten);
					}
				});

				final MyCheckboxConfig inputAbsen = new MyCheckboxConfig("Absen");
				inputAbsen.setChecked(mahasiswaJadiAsisten.getInputAbsen());
				inputAbsen.setParent(arg0);
				inputAbsen.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaJadiAsisten.setInputAbsen(inputAbsen.isChecked());
						Common.refreshSaveOrUpdate(mahasiswaJadiAsisten);
					}
				});

				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(mahasiswaJadiAsisten.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaJadiAsisten.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(mahasiswaJadiAsisten);
					}
				});

				final Textbox keterangan = new Textbox(mahasiswaJadiAsisten.getKeterangan());
				keterangan.setParent(arg0);
				keterangan.setWidth("90%");
				keterangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaJadiAsisten.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(mahasiswaJadiAsisten);
					}
				});

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setOrient("vertical");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Common.refreshDelete(mahasiswaJadiAsisten);
												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadDataDetailAsisten(null, perkuliahan, gridDetailAsisten,
																true);
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
				button.setParent(arg0);
			}
		});
		gridDetailAsisten.setModelCheckMobile(strset);

		gridDetailAsisten.renderAll();

	}

	/**
	 * Daftar kolom grid yang mewakili <b>komponen nilai</b>, satu entri untuk setiap
	 * {@link FormatNilai}, dalam urutan yang sama dengan {@link #formatNilais}. Kolom tetap seperti
	 * Foto, Mahasiswa, Semester, Minimal Kehadiran, Total, dan Verify sengaja <b>tidak</b> masuk ke
	 * daftar ini. Pemisahan itu memungkinkan {@link DetailPerkuliahanRenderer#render(Row, Object)}
	 * menyembunyikan seluruh kolom komponen sekaligus saat mode &quot;hanya input nilai huruf&quot;
	 * menyala, tanpa menyentuh kolom tetap. Daftar dibuat ulang dari nol pada setiap
	 * {@link #prosesDisplay} agar tidak menumpuk kolom dari pembangunan layar sebelumnya.
	 */
	private List<Column> columns = new ArrayList<Column>();

	/**
	 * Rujukan ke kolom &quot;Mahasiswa&quot; yang lebarnya <b>menyesuaikan diri</b> terhadap jumlah
	 * komponen nilai. Ketika kolom komponen banyak, kolom nama dipersempit agar total lebar tetap
	 * sekitar 95%; ketika mode nilai huruf menyala dan semua kolom komponen tersembunyi, kolom ini
	 * melebar menjadi 85% supaya nama mahasiswa tidak terapung di tengah baris yang nyaris kosong.
	 * Penyetelannya terjadi di dalam perender, sehingga berulang pada setiap baris.
	 */
	private MyColumnConfig columnMahasiswa;

	/**
	 * Kotak centang toolbar &quot;Urutkan berdasar nama&quot;, bawaannya tercentang. Nilainya
	 * diteruskan ke {@code perkuliahan.ambilDetailperkuliahan(...)} sehingga pengurutan dilakukan di
	 * basis data, bukan di memori. Bila tidak tercentang, urutan mengikuti bawaan query &mdash;
	 * lazimnya NIM. Selain memuat ulang grid, field ini juga dibaca oleh operasi massal (kunci per
	 * kolom, verifikasi massal, snapshot penguncian) yang mengambil ulang daftar mahasiswa dengan
	 * pengurutan yang sama agar konsisten dengan yang tampak di layar.
	 */
	private MyCheckboxConfig urutkanBerdasarkanNama;

	/**
	 * Menandai bahwa pengguna memperoleh <b>hak buka-kunci istimewa</b>: konfigurasi
	 * <code>kunci_nilai_untuk_admin</code> aktif <i>dan</i> perannya
	 * {@link Tbmrole#ADMINISTRATOR}. Bila menyala, tombol Buka Kunci tingkat kelas dan tombol buka
	 * kunci per kolom diaktifkan meskipun kunci dipasang pengguna lain &mdash; pengecualian yang
	 * memang diperlukan ketika dosen pemasang kunci sudah tidak dapat dihubungi. Dihitung ulang setiap
	 * {@link #prosesDisplay} dan selalu diawali {@code false}.
	 */
	private boolean adminBoleh = false;

	/**
	 * Semester {@link #perkuliahan} yang sedang dinilai, disalin saat {@link #display} berjalan.
	 * Dipakai untuk satu keputusan tampilan: grid komentar hanya terlihat bila nilainya lebih dari
	 * nol. Semester bernilai 0 menandai baris konversi atau transfer yang tidak memiliki kegiatan
	 * perkuliahan nyata, sehingga komentar kelas tidak relevan baginya.
	 */
	private Integer semester;

	/**
	 * Menyalakan gerbang tunggakan dari konfigurasi
	 * <code>mhs_yg_belum_bayar_belum_bisa_di_ntry_nilai</code> (perhatikan ejaan kunci yang memang
	 * demikian di basis data). Bila menyala, kotak nilai komponen dikunci bagi mahasiswa yang tidak
	 * lolos {@code PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, semester)}.
	 *
	 * <p>Gerbang ini <b>hanya diterapkan pada jalur kolom komponen</b>; jalur kotak nilai huruf tidak
	 * memeriksanya. Ia juga terpisah dari gerbang pembayaran semester pendek
	 * {@code GateBayarSpUtil.alasanBlokir} yang justru dipasang di jalur nilai huruf. Kedua gerbang
	 * pembayaran itu karena itu berlaku pada jalur yang berbeda dan tidak saling menggantikan.</p>
	 */
	private boolean mhsYgBelumBayarBelumBisaDiEntryNilai = false;

	/**
	 * Tanamkan isi rekap (toolbar + grafik + tabel) yang dibangun kelas
	 * {@code RekapHasil*PerVoPertemuan} LANGSUNG ke {@code target} (tabpanel),
	 * meniru pola Rekap Total Nilai (GradingHelper) yang BERHASIL tampil: borderlayout
	 * di-set parent ke tabpanel dengan TINGGI PASTI (520px), bukan dibiarkan di dalam
	 * MyWindow. Saat berada di tabpanel, borderlayout di dalam window collapse 0px
	 * sehingga konten tidak tampil; dengan ditanam langsung + tinggi pasti, konten
	 * ter-render seperti tab Rekap Total Nilai.
	 *
	 * <h3>Cara kerjanya</h3>
	 * <p>Metode menelusuri anak langsung {@code windowRekap}, mencari yang bertipe
	 * {@link org.zkoss.zul.Borderlayout}, lalu memindahkannya ke {@code target} sambil memasang lebar
	 * 100% dan tinggi tetap 2000 piksel. Penelusuran dilakukan atas <b>salinan</b> daftar anak
	 * ({@code new ArrayList<Object>(...)}) karena {@code setParent} mengubah daftar itu sendiri;
	 * mengiterasi daftar aslinya akan memicu {@code ConcurrentModificationException}. Wadah
	 * {@code windowRekap} sendiri dibiarkan &mdash; ia tidak pernah dilampirkan ke halaman, sehingga
	 * cukup ditinggalkan untuk dikumpulkan pemulung memori.</p>
	 *
	 * <p>Tinggi 2000 piksel sengaja jauh lebih besar daripada 520 piksel yang disebut di atas: nilai
	 * itu adalah hasil penyetelan berikutnya agar tabel rekap yang panjang tidak terpotong. Pemanggil
	 * melengkapinya dengan {@code setStyle("min-height: 2000px;")} pada tabpanel supaya wadah luarnya
	 * ikut memberi ruang.</p>
	 *
	 * <p><b>Penanganan galat.</b> Seluruh badan dibungkus penangkap {@link Throwable} &mdash; bukan
	 * sekadar {@link Exception} &mdash; dan diteruskan ke {@code Common.tampilErrorJikaAdmin} sehingga
	 * hanya administrator yang melihat rinciannya. Sikap gagal-diam ini disengaja: kegagalan menanam
	 * satu tab rekap tidak boleh menjatuhkan seluruh layar penilaian, dan tab yang gagal cukup tampil
	 * kosong. Bila {@code windowRekap} tidak memiliki anak bertipe borderlayout sama sekali, metode
	 * selesai tanpa melakukan apa pun dan tanpa memberi tahu siapa pun.</p>
	 *
	 * @param windowRekap instance RekapHasil*PerVoPertemuan yang sudah membangun
	 *                    borderlayout di dalamnya (belum dilampirkan ke halaman)
	 * @param target      tabpanel tujuan tampilan
	 */
	private static void tanamkanRekapKeTabpanel(org.zkoss.zk.ui.Component windowRekap,
			org.zkoss.zk.ui.Component target) {
		try {
			for (Object anak : new java.util.ArrayList<Object>(windowRekap.getChildren())) {
				if (anak instanceof org.zkoss.zul.Borderlayout) {
					org.zkoss.zul.Borderlayout bl = (org.zkoss.zul.Borderlayout) anak;
					bl.setParent(target);
					bl.setWidth("100%");
					bl.setHeight("2000px");
				}
			}
		} catch (Throwable t) {
			Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
		}
	}

	/**
	 * Membangun seluruh isi tab <i>Asisten Dosen</i>: toolbar, definisi kolom, dan grid daftar
	 * {@link MahasiswaJadiAsisten}. Dipanggil secara <b>malas</b> &mdash; hanya ketika tab diklik
	 * pertama kali dan panelnya masih kosong &mdash; sehingga membuka layar penilaian tidak menanggung
	 * biaya membangun tab yang mungkin tidak pernah dilihat.
	 *
	 * <p>Metode ini {@code static} dan tidak menyentuh state instance sama sekali; seluruh konteks
	 * datang dari kedua parameternya. Karena itu ia juga dapat dipakai layar lain yang perlu
	 * menampilkan daftar asisten tanpa membangun mesin penilaian lengkap.</p>
	 *
	 * <h3>Susunan</h3>
	 * <p>Panel dikosongkan lebih dulu dengan {@code Common.clear}, lalu diisi sebuah pembungkus
	 * bertinggi minimum 300 piksel agar tab tidak mengempis saat daftar kosong. Toolbar memuat dua
	 * tombol dan <b>seluruhnya disembunyikan bila tidak ada pengguna yang masuk</b>
	 * ({@code Common.getCurrentUser()} bernilai {@code null}) &mdash; satu-satunya penjagaan wewenang
	 * pada tab ini:</p>
	 * <ul>
	 * <li><b>Ambil Mahasiswa</b> membuka {@code AmbilDataMahasiswaForAsistenHelper} untuk memilih
	 * mahasiswa yang akan diangkat menjadi asisten; setelah pemilihan selesai, grid dimuat ulang
	 * dengan penyegaran cache lewat {@code Common.createDefaultTimer}.</li>
	 * <li><b>Refresh</b> memaksa {@code Common.getFormatNilais(perkuliahan, true)} membaca ulang
	 * definisi komponen nilai, lalu memuat ulang grid. Pembacaan ulang format nilai di tab asisten
	 * mungkin tampak ganjil, tetapi berguna karena tab ini kerap dibuka setelah bobot penilaian
	 * diubah di layar lain.</li>
	 * </ul>
	 *
	 * <p>Tujuh kolom didefinisikan: NIM, Nama, Input Nilai, Input Absen, Aktif, Keterangan, dan satu
	 * kolom aksi. Kolom terakhir dilebarkan menjadi 5% hanya bila ada pengguna yang masuk, dan
	 * dikempiskan ke 0% bila tidak &mdash; cara sederhana menyembunyikan tombol hapus. Grid memakai
	 * cetakan {@code paging} dengan ukuran halaman 1000, praktis menampilkan semua asisten dalam satu
	 * halaman.</p>
	 *
	 * <p>Pemuatan awal dilakukan dengan {@code refresh} bernilai {@code false} sehingga cache asosiasi
	 * yang mungkin sudah hangat tetap dipakai; hanya aksi pengguna yang memaksa pembacaan ulang.</p>
	 *
	 * @param detailPenilaian komponen tujuan, lazimnya {@link Tabpanel} tab Asisten Dosen; isinya
	 *                        dikosongkan lebih dulu.
	 * @param perkuliahan     kelas yang daftar asistennya dikelola; ditangkap oleh listener tombol
	 *                        sehingga harus {@code final}.
	 * @see #loadDataDetailAsisten(Object, Perkuliahan, MyGrid, boolean)
	 */
	public static void displayAsistenMahasiswa(org.zkoss.zk.ui.Component detailPenilaian, final Perkuliahan perkuliahan) {
		Common.clear(detailPenilaian);

		final MyGrid gridDetailAsisten = new MyGrid();
		Tbmuser tbmuser = Common.getCurrentUser();
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 300px;");
		groupbox.setParent(detailPenilaian);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.setVisible(tbmuser != null);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaForAsistenHelper dataMahasiswaHelper = new AmbilDataMahasiswaForAsistenHelper(
						perkuliahan);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetailAsisten(null, perkuliahan, gridDetailAsisten, true);
							}
						});
					}
				});
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.getFormatNilais(perkuliahan, true);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadDataDetailAsisten(null, perkuliahan, gridDetailAsisten, true);
					}
				});
			}

		});
		button.setParent(toolbar);

		gridDetailAsisten.setMold("paging");
		gridDetailAsisten.setPageSize(1000);
		gridDetailAsisten.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(gridDetailAsisten);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Input Nilai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Input Absen");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(tbmuser == null ? "0%" : "5%");

		loadDataDetailAsisten(null, perkuliahan, gridDetailAsisten, false);
	}

	/**
	 * <b>Pintu masuk tunggal</b> layar penilaian. Metode ini tidak membangun komponen apa pun sendiri;
	 * tugasnya adalah menyiapkan konteks yang benar lalu memutuskan apakah data nilai perlu
	 * dibangkitkan lebih dulu sebelum {@link #prosesDisplay} boleh menggambar layar.
	 *
	 * <h3>Empat langkah persiapan</h3>
	 * <ol>
	 * <li><b>Memasang callback.</b> {@link #onPerubahanNilai} hanya ditimpa bila argumen yang masuk
	 * tidak {@code null}, sehingga pemanggilan ulang tanpa callback tidak menghapus callback yang
	 * sudah terpasang pada pemanggilan sebelumnya.</li>
	 *
	 * <li><b>Menentukan kelas yang sebenarnya dinilai.</b> Inilah keputusan terpenting di sini. Bila
	 * {@code kuliyah} ditandai sebagai kelas paralel ({@code getMerupakan_paralel()}) dan memiliki
	 * rujukan induk ({@code getPerkuliahan_paralel()}), maka {@link #perkuliahan} diarahkan ke
	 * <b>induknya</b>, bukan ke kelas yang diklik pengguna. Alasannya: kelas paralel berbagi satu
	 * tempat penyimpanan nilai dengan induknya, sehingga bobot, kunci, dan baris nilai harus dibaca
	 * dan ditulis di sana. Parameter {@code kuliyah} tetap dibawa berkeliling dan diteruskan ke
	 * {@link #prosesDisplay} karena tab-tab rekap memang harus menampilkan data kelas yang diklik,
	 * bukan induknya. Kedua objek ini <b>berbeda peran</b> dan tidak boleh saling ditukar.</li>
	 *
	 * <li><b>Mengambil identitas dan periode.</b> {@link #tbmuser} dan {@link #dosen} dibaca ulang,
	 * {@link #semester} disalin, dan {@link #konfigurasi} diambil dari
	 * {@code CommonPenilaian.getKonfigurasi(tahunAkademik, jenisSemester, statusSemesterPendek)}.
	 * Bendera {@link #aktifPenilaian} diisi apa adanya dari {@code aktifPenilaianData} milik
	 * pemanggil. Variabel lokal {@code jenisSemester} sempat diisi dua kali dengan nilai yang sama;
	 * pengulangan itu tidak berdampak.</li>
	 *
	 * <li><b>Memeriksa kelengkapan data nilai.</b> Seluruh baris kelas ditelusuri untuk menghitung
	 * berapa banyak yang kolom {@code detailNilai}-nya masih {@code null} atau kosong. Daftar
	 * sementara itu langsung dilepas ({@code temp = null}) setelah dihitung agar tidak menahan memori
	 * pada kelas besar.</li>
	 * </ol>
	 *
	 * <h3>Percabangan penutup</h3>
	 * <p>Bila ditemukan baris yang {@code detailNilai}-nya kosong &mdash; lazimnya mahasiswa yang baru
	 * saja disetujui KRS-nya, atau kelas yang komponen nilainya baru pertama kali didefinisikan
	 * &mdash; {@code Common.realoadNilaiLangsung} dijalankan lebih dulu untuk <b>membangkitkan</b>
	 * struktur nilai kosong bagi semua baris, dan {@link #prosesDisplay} baru dipanggil dari dalam
	 * callback-nya dengan {@code refresh} bernilai {@code true}. Tanpa langkah ini, kotak nilai akan
	 * muncul tanpa kunci komponen yang benar dan penyimpanan pertama bisa meleset. Bila semua baris
	 * sudah lengkap, {@link #prosesDisplay} dipanggil langsung dengan {@code refresh} bernilai
	 * {@code null} sehingga cache tetap dipakai.</p>
	 *
	 * <p><b>Catatan wewenang.</b> Metode ini tidak memeriksa apakah pengguna berhak membuka atau
	 * mengubah kelas ini. Ia menerima {@code aktifPenilaianData} dan bendera {@link #edit} dari
	 * constructor sebagai keputusan yang sudah final. Pembedaan yang dilakukannya sendiri hanya
	 * &quot;punya profil dosen atau tidak&quot; lewat {@link #dosen}, yang dipakai untuk aturan
	 * verifikasi, bukan untuk membatasi akses ke kelas tertentu.</p>
	 *
	 * <p><b>Efek samping.</b> Menulis ke tujuh field instance dan mencetak nilai
	 * {@link #aktifPenilaian} ke {@code System.out}. Pada cabang pembangkitan nilai, ia meneruskan
	 * {@link #detailperkuliahans} yang pada pembukaan pertama masih {@code null} karena
	 * {@link #loadData(Object)} belum pernah berjalan.</p>
	 *
	 * @param kuliyah           kelas yang dipilih pengguna; dipakai apa adanya oleh tab rekap, dan
	 *                          diganti dengan induknya bila merupakan kelas paralel.
	 * @param component         wadah tujuan seluruh layar; isinya dikosongkan oleh
	 *                          {@link #prosesDisplay}.
	 * @param onPerubahanNilai  callback yang dipicu setiap kali sebuah nilai berubah; boleh
	 *                          {@code null} untuk mempertahankan callback yang sudah ada.
	 * @param buttonFormatNilai tombol Format Nilai milik layar induk yang visibilitasnya ikut diatur
	 *                          saat kelas dikunci atau dibuka; boleh {@code null}.
	 * @param aktifPenilaianData {@code true} bila pemanggil membuka layar dalam mode penilaian aktif,
	 *                          yang mengizinkan entri meskipun periode penilaian tertutup.
	 * @throws Exception bila pembacaan data atau pembangunan komponen gagal.
	 * @see #prosesDisplay(Perkuliahan, Component, EventListener, MyToolbarbuttonConfig, Boolean)
	 */
	public void display(final Perkuliahan kuliyah, final Component component, final EventListener onPerubahanNilai,
			final MyToolbarbuttonConfig buttonFormatNilai, boolean aktifPenilaianData) throws Exception {

		this.onPerubahanNilai = onPerubahanNilai == null ? this.onPerubahanNilai : onPerubahanNilai;
		this.perkuliahan = kuliyah.getMerupakan_paralel() && kuliyah.getPerkuliahan_paralel() != null
				? kuliyah.getPerkuliahan_paralel()
				: kuliyah;

		String jenisSemester = perkuliahan.getGanjilGenap();
		String tahunAkademik = perkuliahan.getTahunAjaran();

		tbmuser = Common.getCurrentUser() == null ? null : Common.getCurrentUser();

		dosen = tbmuser == null ? null : tbmuser.ambilDosen();

		semester = perkuliahan.getSemester();
		jenisSemester = perkuliahan.getGanjilGenap();

		aktifPenilaian = aktifPenilaianData;
		System.out.println("aktifPenilaian = " + aktifPenilaian);
		Konfigurasi konfigurasi = CommonPenilaian.getKonfigurasi(tahunAkademik, jenisSemester,
				perkuliahan.getStatusSemesterPendek());

		this.konfigurasi = konfigurasi;

		Collection<Long> temp = this.perkuliahan.ambilDetailperkuliahan();

		int adaygkosong = 0;
		for (Long detailperkuliahanid : temp) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getDetailNilai() == null || detailperkuliahan.getDetailNilai().trim().isEmpty()) {
					adaygkosong++;
				}
			}
		}
		temp = null;

		if (adaygkosong > 0) {
			Common.realoadNilaiLangsung(perkuliahan, perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, true);
						}
					}, detailperkuliahans);
		} else {
			prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, null);
		}
	}

	/**
	 * Membangun <b>seluruh layar penilaian</b> dari nol: tujuh tab, toolbar dengan belasan tombol
	 * beserta listener-nya, definisi kolom grid yang lebarnya dihitung adaptif, dan grid komentar.
	 * Metode ini adalah bagian terbesar kelas ini dan sekaligus tempat hampir seluruh <b>aksi
	 * mengubah data</b> didaftarkan.
	 *
	 * <p>Ia dipanggil oleh {@link #display} dan &mdash; ini yang perlu diperhatikan &mdash; juga
	 * memanggil <b>dirinya sendiri</b> dari dalam banyak listener sebagai cara membangun ulang layar
	 * setelah data berubah. Setiap pemanggilan ulang mengosongkan {@code component} dan membuat
	 * kembali semua komponen, sehingga rujukan komponen yang disimpan pemanggil menjadi basi.</p>
	 *
	 * <h3>Persiapan awal</h3>
	 * <p>Kebijakan tunggakan {@link #mhsYgBelumBayarBelumBisaDiEntryNilai} dibaca, lalu
	 * {@link #statusPertemuan} diisi dengan string absensi dari setiap {@link Pertemuan} kelas yang
	 * kolom absensinya tidak kosong. Pengumpulan sekali di muka inilah yang membuat perender dapat
	 * menghitung rekap kehadiran tanpa menyentuh basis data per baris.</p>
	 *
	 * <h3>Tabbox dan penambalan ZK5</h3>
	 * <p>Tujuh tab dibuat: Input Nilai, Asisten Dosen, Rekap Tugas, Rekap Ujian, Rekap Tugas Kelompok,
	 * Rekap Total Nilai, dan Prestasi Belajar. Hanya tab pertama dibangun serentak; enam sisanya
	 * dibangun malas lewat listener {@code onClick} masing-masing. Karena pada konteks bersarang
	 * peristiwa {@code onClick} tab tidak selalu terpicu, sebuah listener {@code onSelect} dipasang di
	 * tingkat tabbox sebagai penambal: bila panel terpilih masih kosong, ia mengirim {@code onClick}
	 * secara manual, lalu memaksa {@code invalidate()} lewat timer agar konten yang baru dibangun
	 * benar-benar tergambar. Pemeriksaan &quot;masih kosong&quot; mencegah tab yang sudah terisi
	 * dibangun ulang. Tab Asisten Dosen disembunyikan dari akun mahasiswa.</p>
	 *
	 * <h3>Toolbar: aksi yang mengubah data</h3>
	 * <ul>
	 * <li><b>Cari</b> dan kotak {@link #nama} memuat ulang grid dengan penyaringan.</li>
	 * <li><b>Cetak</b> memanggil {@link #onLaporan(Perkuliahan, Component)}.</li>
	 * <li><b>Format Nilai</b> membuka {@code FormatPenilaianHelper}; pada kurikulum OBE tombol tetap
	 * tampil dan membuka pengaturan bobot CPMK/Sub-CPMK, dengan tooltip yang menjelaskannya.</li>
	 * <li><b>Download</b> dan <b>Upload</b> menangani berkas Excel lewat {@code PenilaianUtil}. Unggahan
	 * hanya menerima {@code .xlsx}; berkas disalin ke direktori {@code /temp} aplikasi bita demi bita
	 * sebelum diproses.</li>
	 * <li><b>Kunci</b> membekukan nilai kelas. Bila konfigurasi
	 * <code>sebelum_dikunci_harus_diverifikasi_dulu</code> aktif, penguncian ditolak selama masih ada
	 * baris {@link Detailperkuliahan#NOT_VERIFIED}. Sebelum {@code setDikunci} dipasang,
	 * {@link Detailperkuliahan#bekukanSemuaNilai()} dijalankan untuk <b>setiap</b> mahasiswa selagi
	 * kelas masih terbuka &mdash; langkah ini krusial karena snapshot kunci hanya tercermin otomatis
	 * pada kondisi tertentu, dan tanpa penyalinan ulang ini nilai yang tampil bisa &quot;berubah&quot;
	 * ke snapshot lama begitu kunci dipasang. Bila penyalinan gagal, penguncian <b>dibatalkan</b> dan
	 * pengguna diberi tahu.</li>
	 * <li><b>Buka Kunci</b> membatalkan penguncian. Tombolnya dinonaktifkan bila kunci dipasang
	 * pengguna lain, kecuali bagi pemegang {@link #adminBoleh}.</li>
	 * <li><b>Hitung Ulang</b> memuat ulang tabel nilai huruf dari basis data, menyegarkan objek
	 * {@link Perkuliahan} di tempat agar cache tidak basi, lalu menghitung ulang seluruh mahasiswa
	 * secara paralel dengan batas 50 utas.</li>
	 * <li><b>Analisis Keseluruhan</b> membuka {@link #tampilkanAnalisisKeseluruhanNilai()}.</li>
	 * <li><b>Verifikasi</b> memverifikasi seluruh kelas sekaligus; baris yang salah satu komponennya
	 * masih bernilai nol tetap ditandai {@code NOT_VERIFIED}.</li>
	 * <li><b>Singkronkan</b> menjalankan {@code perkuliahan.singkronkan(session)} pada utas latar
	 * dengan session <b>dedikasi</b> &mdash; bukan session ThreadLocal &mdash; karena metode yang
	 * dipanggilnya menutup session ThreadLocal miliknya sendiri di tengah proses.</li>
	 * <li><b>Masukkan Nilai Absen</b> menghitung nilai kehadiran dengan rumus
	 * {@code (masuk*100 + sakit*50 + izin*50) / total}, yakni sakit dan izin dihargai setengah,
	 * lalu menuliskannya ke komponen yang namanya mengandung &quot;absen&quot;, &quot;hadir&quot;,
	 * atau &quot;presensi&quot;. Ditolak bila bobot komponen belum berjumlah 100%.</li>
	 * <li><b>Reset</b> mengosongkan seluruh nilai kelas. Bersifat destruktif dan tak dapat dibatalkan,
	 * sehingga selalu meminta konfirmasi; komponen yang sudah dikunci beserta snapshot permanennya
	 * sengaja tidak ikut dikosongkan.</li>
	 * <li><b>Ambil Nilai dari Feeder</b>, <b>History</b>, <b>Restore</b>, <b>Komentar</b>, dan
	 * <b>Refresh</b> melengkapi toolbar.</li>
	 * </ul>
	 *
	 * <h3>Kolom grid yang adaptif</h3>
	 * <p>Lebar kolom dihitung agar totalnya mendekati 95% berapa pun jumlah komponen: kolom tetap
	 * memakan sekitar 28% (ditambah 5% bila verifikasi aktif), sisanya dibagi rata antar komponen
	 * dengan batas bawah 4% dan batas atas 14%, dan kolom Mahasiswa mengambil sisa dengan lantai 15%.
	 * Setiap kolom komponen memuat nama ringkas hasil
	 * {@link #ambilNamaFormatNilaiRingkas(FormatNilai)}, nomor urut yang dapat diubah, pilihan jenis
	 * evaluasi, serta sepasang tombol kunci/buka-kunci per kolom. Mengunci satu kolom memanggil
	 * {@link Detailperkuliahan#bekukanDetailNilai(FormatNilai)} untuk seluruh mahasiswa lebih dulu,
	 * meniru pola penguncian tingkat kelas.</p>
	 *
	 * <h3>Hal yang perlu diketahui</h3>
	 * <p>Variabel lokal {@code editDisable} di dalam metode ini <b>membayangi</b> field
	 * {@link #editDisable} dan bermakna jauh lebih luas, tetapi tidak pernah ditulis kembali ke field.
	 * Akibatnya toolbar memakai makna luas sementara perender memakai makna sempit. Selanjutnya,
	 * seluruh gerbang wewenang di sini bertumpu pada {@code tbmuser.getMahasiswa() == null} &mdash;
	 * yaitu &quot;bukan akun mahasiswa&quot; &mdash; sehingga tombol Kunci, Buka Kunci, Verifikasi,
	 * Reset, dan Masukkan Nilai Absen terbuka bagi <b>semua</b> akun pegawai yang mencapai layar ini,
	 * tanpa pemeriksaan bahwa yang bersangkutan mengampu kelas tersebut. Terakhir, tidak ada satu pun
	 * operasi massal di sini yang mengunci baris basis data, sehingga dua penilai yang bekerja
	 * bersamaan dapat saling menimpa hasil.</p>
	 *
	 * @param kuliyah           kelas yang dipilih pengguna &mdash; dipakai apa adanya untuk tab rekap
	 *                          dan untuk pemanggilan ulang metode ini; berbeda dari
	 *                          {@link #perkuliahan} bila kelas ini paralel.
	 * @param component         wadah tujuan; <b>dikosongkan seluruhnya</b> di awal.
	 * @param onPerubahanNilai  callback perubahan nilai yang diteruskan ke perender.
	 * @param buttonFormatNilai tombol Format Nilai milik layar induk yang visibilitasnya diselaraskan
	 *                          dengan status kunci; boleh {@code null}.
	 * @param refresh           diteruskan ke {@link #loadData(Object)} sebagai bendera penyegaran
	 *                          cache; {@code null} berarti memakai cache.
	 * @see #display(Perkuliahan, Component, EventListener, MyToolbarbuttonConfig, boolean)
	 * @see DetailPerkuliahanRenderer
	 */
	public void prosesDisplay(final Perkuliahan kuliyah, final Component component,
			final EventListener onPerubahanNilai, final MyToolbarbuttonConfig buttonFormatNilai, Boolean refresh) {

		mhsYgBelumBayarBelumBisaDiEntryNilai = Common.bolehKonfigurasi("mhs_yg_belum_bayar_belum_bisa_di_ntry_nilai", Konfigurasi.TIDAK_AKTIF);
		statusPertemuan = new ArrayList<String>();

		TreeMap<String, Long> pertemuans = perkuliahan.ambilPertemuan();
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				if (!pertemuan.getAbsensi().trim().isEmpty()) {
					statusPertemuan.add(pertemuan.getAbsensi());
				}
			}
		}

		Common.clear(component);
		final Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);

		// FIX ZK5: konten sub-tab Nilai yang dibangun LAZY (Rekap Tugas/Ujian/
		// Tugas Kelompok/Total Nilai/Prestasi) kadang tidak keluar — pada konteks
		// nested (Aktifitas Perkuliahan) event onClick tab tidak selalu memicu
		// pembangunan + render. Solusi andal lewat onSelect tabbox:
		//   1) bila panel terpilih masih KOSONG, picu onClick tab tsb untuk
		//      membangun kontennya (tidak rebuild bila sudah terisi → Input Nilai
		//      yang eager tetap aman),
		//   2) invalidate panel via timer agar konten yang baru dibangun
		//      benar-benar ter-render setelah event seleksi tuntas.
		tabbox.addEventListener("onSelect", new EventListener() {

			@Override
			public void onEvent(Event evtSelTab) throws Exception {
				try {
					org.zkoss.zul.Tab tabTerpilih = tabbox.getSelectedTab();
					org.zkoss.zul.Tabpanel panelTerpilih = tabbox.getSelectedPanel();
					if (panelTerpilih != null && panelTerpilih.getFirstChild() == null
							&& tabTerpilih != null) {
						org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", tabTerpilih, null));
					}
				} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1216");
				}
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event evtTimerTab) throws Exception {
						try {
							org.zkoss.zul.Tabpanel panelTerpilih = tabbox.getSelectedPanel();
							if (panelTerpilih != null) {
								panelTerpilih.invalidate();
							}
						} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1227");
						}
					}
				});
			}
		});

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig();
		tab1.setParent(tabs);
		tab1.setLabel("Input Nilai");

		MyTabConfig tab1AsistenMahasiswa = new MyTabConfig();
		tab1AsistenMahasiswa.setParent(tabs);
		tab1AsistenMahasiswa.setLabel("Asisten Dosen");
		tab1AsistenMahasiswa.setVisible(tbmuser.getMahasiswa() == null);
//
//		MyTabConfig tab1LihatRekapKehadiran = new MyTabConfig();
//		tab1LihatRekapKehadiran.setParent(tabs);
//		tab1LihatRekapKehadiran.setLabel("Rekap Kehadiran");

		MyTabConfig tab1LihatRekapTugas = new MyTabConfig();
		tab1LihatRekapTugas.setParent(tabs);
		tab1LihatRekapTugas.setLabel("Rekap Tugas");

		MyTabConfig tab1LihatRekapUjian = new MyTabConfig();
		tab1LihatRekapUjian.setParent(tabs);
		tab1LihatRekapUjian.setLabel("Rekap Ujian");

		MyTabConfig tab1LihatRekapTugasKelompok = new MyTabConfig();
		tab1LihatRekapTugasKelompok.setParent(tabs);
		tab1LihatRekapTugasKelompok.setLabel("Rekap Tugas Kelompok");

		MyTabConfig tab1LihatRekapNilai = new MyTabConfig();
		tab1LihatRekapNilai.setParent(tabs);
		tab1LihatRekapNilai.setLabel("Rekap Total Nilai");

		MyTabConfig tab1Prestasi = new MyTabConfig();
		tab1Prestasi.setParent(tabs);
		tab1Prestasi.setLabel("Prestasi Belajar");

//		MyTabConfig tab1PrestasiSemua = new MyTabConfig();
//		tab1PrestasiSemua.setParent(tabs);
//		tab1PrestasiSemua.setLabel("Prestasi Belajar Semua");

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel managemenPenilaian = new ais.ui.util.MyTabpanel();
		managemenPenilaian.setParent(tabpanels);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(managemenPenilaian);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang mengikuti perkuliahan " + perkuliahan.toString()));

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);

		nilai0masukNilaiAkhir = new MyCheckboxConfig("Nilai 0 tidak masuk pembagi nilai akhir");
		nilai0masukNilaiAkhir.setStyle("font-size:9px");
		jikaNilai0masukNilaiAkhir = new MyCheckboxConfig("Jika ada nilai 0 tidak menghitung nilai akhir");
		jikaNilai0masukNilaiAkhir.setStyle("font-size:9px");

		hanyaInputNilaiHuruf = new MyCheckboxConfig("Hanya input nilai huruf");
		hanyaInputNilaiHuruf.setStyle("font-size:9px");

		sembunyikanNilaiJikaBelumDiverifikasi = new MyCheckboxConfig(
				"Sembunyikan nilai ke mhs, jika blm di-verifikasi");
		sembunyikanNilaiJikaBelumDiverifikasi.setStyle("font-size:9px");
		sembunyikanNilaiJikaBelumDiverifikasi.setChecked(perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi());

		sembunyikanNilaiJikaBelumDiverifikasi.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				perkuliahan.setSembunyikanNilaiJikaBelumDiverifikasi(sembunyikanNilaiJikaBelumDiverifikasi.isChecked());
				Common.refreshUpdate(perkuliahan);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, true);
									}
								}, detailperkuliahans);
					}
				});
			}
		});

		urutkanBerdasarkanNama = new MyCheckboxConfig("Urutkan berdasar nama");
		urutkanBerdasarkanNama.setStyle("font-size:9px");
		urutkanBerdasarkanNama.setChecked(true);
		urutkanBerdasarkanNama.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(groupbox);
		hbox.appendChild(nilai0masukNilaiAkhir);
		hbox.appendChild(jikaNilai0masukNilaiAkhir);
		hbox.appendChild(hanyaInputNilaiHuruf);
		hbox.appendChild(sembunyikanNilaiJikaBelumDiverifikasi);
		hbox.appendChild(urutkanBerdasarkanNama);

		final Html warning = new ais.ui.util.MyHtml(
				"<font style='font-size:12px;color:red;'>Demi menjaga integritas data penilaian, harap segera mengunci data nilai mahasiswa anda setelah semua nilai dimasukkan.</font>");
		warning.setParent(hbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(8);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);
		button.setOrient("vertical");

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onLaporan(perkuliahan, null);
			}
		});
		print.setParent(toolbar);
		print.setOrient("vertical");

		mahasiswaBolehUbahNilai = false;
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			mahasiswaBolehUbahNilai = perkuliahan.merupakanAsistenNilai(tbmuser.getMahasiswa());
		}

		boolean editDisableTemp = !edit
				|| (!aktifPenilaian
						&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)))
				|| (tbmuser.getMahasiswa() != null && !mahasiswaBolehUbahNilai);

		if (Common.bolehKonfigurasi("hanya_dosen_yg_boleh_entry_nilai", Konfigurasi.TIDAK_AKTIF)) {
			if (tbmuser != null && tbmuser.ambilDosen() == null) {
				editDisableTemp = true;
			}
		}

		final boolean editDisable = editDisableTemp;

		final MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Format Nilai", "/img/svg/edit-box-line.svg");
		if (component instanceof Tabpanel) {
			if (perkuliahan != null && !perkuliahan.getSembunyikanFormatPenilaian()) {
				btn.setOrient("vertical");
				if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
						.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
					// OBE: tombol TETAP TAMPIL. Bobot penilaian OBE ditentukan CPMK/Sub-CPMK, jadi klik
					// tombol ini akan membuka popup RPS OBE (tab CPMK & Sub-CPMK) lewat FormatPenilaianHelper,
					// bukan lagi disembunyikan. Beri tooltip agar maksudnya jelas.
					btn.setTooltiptext(Common.getBahasaConfig("Atur bobot penilaian OBE (CPMK & Sub-CPMK)"));
				}
				btn.addEventListener("onClick", new EventListener() {

					FormatPenilaianHelper formatPenilaianHelper = new FormatPenilaianHelper();

					@Override
					public void onEvent(Event event) throws Exception {
						MyWindow addWindow = new MyWindow();
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
						formatPenilaianHelper.display(perkuliahan, addWindow, new TampilDetailNilaiInterface() {

							@Override
							public void realoadNilai(final Perkuliahan perkuliahan) {

								Common.realoadNilai(perkuliahan, perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												display(perkuliahan, component, onPerubahanNilai, btn, aktifPenilaian);
											}
										}, detailperkuliahans);

							}
						});
					}

				});
				btn.setParent(toolbar);
			}
		}

		final MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		download.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PenilaianUtil.downloadPenilaian(perkuliahan, formatNilais);
			}
		});

		download.setDisabled(editDisable);
		download.setOrient("vertical");
		download.setParent(toolbar);

		final MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setDisabled(editDisable);
		upload.setVisible(Common.bolehKonfigurasi("tampilkan_upload_nilai_di_modul_penilaian"));
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// // System.out.println("media = " + media);
					File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// // System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					PenilaianUtil.uploadPenilaian(perkuliahan, file, formatNilais, onPerubahanNilai,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									display(perkuliahan, component, onPerubahanNilai, buttonFormatNilai,
											aktifPenilaian);
								}
							});

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		upload.setParent(toolbar);
		upload.setOrient("vertical");
		final MyToolbarbuttonConfig bukaKunci = new MyToolbarbuttonConfig("Buka", "/img/svg/unlock.svg");
		final MyToolbarbuttonConfig kunci = new MyToolbarbuttonConfig("Kunci", "/img/Lock-Lock-icon.png");

		bukaKunci.setStyle("font-size:11px;");
		kunci.setStyle("font-size:11px;");

		final MyToolbarbuttonConfig buttonMasukkanNilaiAbsen = new MyToolbarbuttonConfig("Masukkan Nilai Absen",
				"/img/excel.png");

		adminBoleh = false;

		if (tbmuser.getMahasiswa() == null) {

			kunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin mengunci nilai ini ?\n\nCatatan : Nilai akan terkunci dan tidak bisa dirubah oleh orang lain kecuali jika anda membuka kunci penilain kembali.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										if (Common.bolehKonfigurasi("sebelum_dikunci_harus_diverifikasi_dulu", Konfigurasi.TIDAK_AKTIF)) {

											Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(
													null, null, "", urutkanBerdasarkanNama.isChecked(), true);

											for (Long detailperkuliahanid : detailperkuliahans) {
												Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
														.ambilData(Detailperkuliahan.class,
																detailperkuliahanid.toString());
												if (detailperkuliahan != null && detailperkuliahan.getVerify()
														.equals(Detailperkuliahan.NOT_VERIFIED)) {
													MyMessageboxConfig.show(
															"Semua nilai harus diverifikasi dulu sebelum bisa di kunci",
															"Peringatan", MyMessageboxConfig.OK,
															MyMessageboxConfig.INFORMATION);
													return;
												}
											}

										}

										// PENTING (cegah nilai berubah saat dikunci, mis. "Hasil Proyek" 85 -> 0):
										// Saat terkunci, getDetailNilai() mengembalikan snapshot detailNilaiKunci,
										// BUKAN detailNilai yang sedang tampil. Snapshot itu hanya ter-mirror
										// otomatis (getDetailNilaiKunci) ketika entitas di-flush SELAGI perkuliahan
										// masih terbuka DAN asosiasi perkuliahan termuat. Bila nilai sempat di-update
										// lewat jalur yang tidak memenuhi syarat itu (mis. sinkronisasi feeder /
										// hasil proyek dengan perkuliahan tak termuat), detailNilaiKunci jadi TERTINGGAL
										// (desync) dari detailNilai -> begitu dikunci, nilai yang tampil "berubah" ke
										// snapshot lama. Maka sebelum mengunci, salin ulang detailNilai (nilai LIVE
										// yang sedang ditampilkan) -> detailNilaiKunci untuk SEMUA mahasiswa, selagi
										// perkuliahan masih terbuka, agar penguncian membekukan persis nilai saat ini.
										try {
											Collection<Long> idsSnapshotKunci = perkuliahan.ambilDetailperkuliahan(
													null, null, "", urutkanBerdasarkanNama.isChecked(), true);
											for (Long idSnapshotKunci : idsSnapshotKunci) {
												Detailperkuliahan dpkKunci = (Detailperkuliahan) GeneralValueObject
														.ambilData(Detailperkuliahan.class, idSnapshotKunci.toString());
												if (dpkKunci != null) {
													// Perkuliahan masih terbuka: bekukan detail komponen sekaligus
													// total, huruf, IP, kelulusan, dan nilai sementara ke kolom
													// snapshot masing-masing sebelum status global dipasang.
													dpkKunci.bekukanSemuaNilai();
													Common.refreshUpdate(dpkKunci);
												}
											}
										} catch (Exception exSnapshotKunci) {
											Common.tampilErrorJikaAdmin(exSnapshotKunci);
											MyMessageboxConfig.show(
													"Penguncian dibatalkan karena snapshot permanen nilai belum berhasil disimpan seluruhnya.",
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										perkuliahan.setDikunci(tbmuser);
										Common.refreshUpdate(perkuliahan);

										loadData(null);

										kunci.setVisible(perkuliahan.getDikunci() == null);
										bukaKunci.setVisible(perkuliahan.getDikunci() != null);
										if (perkuliahan.getDikunci() != null) {
											bukaKunci.setLabel(
													"Buka Kunci (" + perkuliahan.getDikunci().getUserNama() + ")");
										}
										Common.freeze(grid, perkuliahan.getDikunci() != null);
										upload.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										download.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										btn.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										warning.setVisible(perkuliahan.getDikunci() == null && !editDisable);

										if (buttonFormatNilai != null)
											buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null);

										onLaporan(perkuliahan, null);

										buttonMasukkanNilaiAbsen.setVisible(perkuliahan.getDikunci() == null);
										nilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_pilihan_nilai_0_tidak_masuk_penghitungan_nilai_akhir")
												&& tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null);
										jikaNilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_jika_ada_nilai_0_tidak_masuk_penghitungan_nilai_akhir")
												&& tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null);

										hanyaInputNilaiHuruf.setVisible(Common.bolehKonfigurasi("tampilkan_hanya_input_nilai_huruf") && tbmuser.getMahasiswa() == null
												&& perkuliahan.getDikunci() == null);

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai,
														true);
											}
										});
									}

								}
							});
				}
			});

			kunci.setVisible(perkuliahan.getDikunci() == null);

			kunci.setParent(toolbar);
			kunci.setOrient("vertical");

			bukaKunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin membuka kunci nilai ini ?\n\nCatatan : Nilai akan terbuka dan bisa dirubah oleh orang lain yang berhak mengakses penilaian anda (misalnya: admin).",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										perkuliahan.setDikunci(null);
										Common.refreshUpdate(perkuliahan);

										loadData(null);

										kunci.setVisible(perkuliahan.getDikunci() == null);
										bukaKunci.setVisible(perkuliahan.getDikunci() != null);

										Common.freeze(grid, perkuliahan.getDikunci() != null);
										upload.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										download.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										btn.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										warning.setVisible(perkuliahan.getDikunci() == null && !editDisable);

										if (buttonFormatNilai != null)
											buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null);
										buttonMasukkanNilaiAbsen.setVisible(perkuliahan.getDikunci() == null);
										nilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_pilihan_nilai_0_tidak_masuk_penghitungan_nilai_akhir")
												&& tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null);
										jikaNilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_jika_ada_nilai_0_tidak_masuk_penghitungan_nilai_akhir")
												&& tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null);

										hanyaInputNilaiHuruf.setVisible(Common.bolehKonfigurasi("tampilkan_hanya_input_nilai_huruf") && tbmuser.getMahasiswa() == null
												&& perkuliahan.getDikunci() == null);

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai,
														true);
											}
										});
									}

								}
							});
				}
			});
			bukaKunci.setVisible(perkuliahan.getDikunci() != null);
			if (perkuliahan.getDikunci() != null) {
				bukaKunci.setLabel("Buka Kunci (" + perkuliahan.getDikunci().getUserNama() + ")");
			}
			bukaKunci.setDisabled((perkuliahan.getDikunci() != null && tbmuser.getUserId() != null
					&& !perkuliahan.getDikunci().getUserId().equals(tbmuser.getUserId())) || !edit
					|| (!aktifPenilaian
							&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))));

			bukaKunci.setParent(toolbar);
			bukaKunci.setOrient("vertical");

			Konfigurasi konfigurasiKunci = Common.getKonfigurasi("kunci_nilai_untuk_admin", Konfigurasi.TIDAK_AKTIF);

			if (konfigurasiKunci.getNilai().equals(Konfigurasi.AKTIF)) {
				if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses() != null && tbmuser != null
						&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR)) {
					bukaKunci.setDisabled(false);
					adminBoleh = true;
				}
			}

			if (aktifPenilaian) {
				if ((perkuliahan.getDikunci() != null && tbmuser.getUserId() != null
						&& perkuliahan.getDikunci().getUserId().equals(tbmuser.getUserId()))) {
					bukaKunci.setDisabled(false);
				}

				if (perkuliahan.getJumlahDosen().intValue() == 0) {
					if (Common.bolehKonfigurasi("buka_kunci_nilai_untuk_jadwal_tanpa_dosen", Konfigurasi.TIDAK_AKTIF)) {
						bukaKunci.setDisabled(false);
					}
				}

				kunci.setDisabled(false);
			}

		}

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Detailperkuliahan.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI));

				criteria.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
						.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.ilike("mahasiswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(),
												MatchMode.ANYWHERE)))
						.add(Restrictions.eq("perkuliahan", perkuliahan));

				if (order)
					criteria.addOrder(Order.asc("mahasiswa.nim"));

				return criteria;
			}
		}, "perkuliahan", "mahasiswa", "semester", "tahunAkademik", "totalNilai", "nilaiHuruf", "totalIP");
		cetakToolbarbutton.setOrient("vertical");
		toolbar.appendChild(cetakToolbarbutton);

		button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/options.png");
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session nilaiHurufSession = null;
						try {
							nilaiHurufSession = HibernateUtil.openSession();
							ConstantValues.realoadNilaiHuruf(nilaiHurufSession);
						} finally {
							if (nilaiHurufSession != null) {
								try {
									nilaiHurufSession.close();
								} catch (Exception eClose) {
									ais.common.ErrorAuditUtil.record(eClose,
											"DetailperkuliahanForPenilaianHelper:reloadNilaiHuruf");
								}
							}
						}

						// MUAT ULANG DARI DATABASE lebih dulu agar cache = DB. Ini memperbaiki kelas bug
						// "status kunci / nilai BASI di cache" (mis. perkuliahan sudah dibuka kuncinya di DB
						// tetapi objek Perkuliahan di cache masih 'terkunci' -> getDetailNilai menimpa nilai
						// ketikan dengan snapshot lama). Reload dilakukan IN-PLACE (Common.refresh) ke objek
						// yang SAMA, lalu di-masukkan ulang ke cache (DataUtil.masukkanData) sehingga tetap
						// SATU objek per (kelas,id) di JVM dan seluruh pemegang melihat data terbaru DB.
						try {
							Common.refresh(perkuliahan);
							ais.common.DataUtil.masukkanData(Perkuliahan.class, perkuliahan);

						} catch (Exception eReload) {
							Common.tampilErrorJikaAdmin(eReload);
						}

						// HITUNG ULANG PARALEL: tiap mahasiswa dihitung di thread & session sendiri, sebanyak
						// jumlah mahasiswa TAPI maksimal 50 thread sekali jalan (dipatok DbThreadPool.safe).
						Common.realoadNilaiLangsungParalel(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, true);
									}
								}, detailperkuliahans, 50);
					}
				});

			}

		});
		toolbar.appendChild(button);

		button = new MyToolbarbuttonConfig("Analisis Keseluruhan", "/img/svg/search.svg");
		button.setOrient("vertical");
		button.setTooltiptext("Analisis pintar nilai seluruh mahasiswa pada perkuliahan ini");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				tampilkanAnalisisKeseluruhanNilai();
			}
		});
		toolbar.appendChild(button);

		if (tbmuser.getMahasiswa() == null) {

			boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);

			adaProsesVerifikasiNilai = Common.bolehKonfigurasi("ada_proses_verifikasi_penilaian_kepada_dosen", Konfigurasi.TIDAK_AKTIF) || nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk;
			if (!adaProsesVerifikasiNilai) {
				perkuliahan.setSembunyikanNilaiJikaBelumDiverifikasi(false);
			}
			sembunyikanNilaiJikaBelumDiverifikasi.setVisible(adaProsesVerifikasiNilai);
			button = new MyToolbarbuttonConfig("Verifikasi", "/img/svg/check2.svg");
			button.setOrient("vertical");
			button.setVisible((dosen == null || (dosen != null && perkuliahan.getDosenBolehVerifikasiNilaiSendiri()))
					&& adaProsesVerifikasiNilai);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan verifikasi nilai ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												for (Long detailperkuliahanid : perkuliahan.ambilDetailperkuliahan()) {
													Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
															.ambilData(Detailperkuliahan.class,
																	detailperkuliahanid.toString());
													if (detailperkuliahan != null) {

														try {
															Session session = HibernateUtil.currentNativeSession();
															session.refresh(detailperkuliahan);

															boolean adayangBelumVerified = false;
															for (FormatNilai formatNilai : formatNilais) {
																Double jumlah = detailperkuliahan
																		.retreiveDetailNilaiBelumVerify(formatNilai);
																if (jumlah < 0.01) {
																	adayangBelumVerified = true;
																} else {
																	detailperkuliahan.populateDetailNilai(formatNilai,
																			null, jumlah, true,
																			perkuliahan
																					.getSembunyikanNilaiJikaBelumDiverifikasi(),
																			tbmuser);
																}
															}

															detailperkuliahan.setVerify(adayangBelumVerified
																	? Detailperkuliahan.NOT_VERIFIED
																	: Detailperkuliahan.VERIFIED);
															detailperkuliahan.setVerifikator(
																	tbmuser.getUserId() + " " + tbmuser.getUserNama());
															detailperkuliahan.setWaktuVerifikasi(
																	ais.ui.util.WaktuUtil.getDate());
															session.getTransaction().begin();
															Common.refreshUpdate(session, detailperkuliahan);
															session.getTransaction().commit();
															// session.disconnect();
															if (session.isOpen()) {
																session.disconnect();
																session.close();
															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1863");
														}
														HibernateUtil.closeSession();
													}
												}

												KomentarPerkuliahanHelper komentarHelper = new KomentarPerkuliahanHelper(
														perkuliahan);

												komentarHelper.display(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadDataKomentar();

														Common.realoadNilai(perkuliahan,
																sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
																new EventListener() {

																	@Override
																	public void onEvent(Event arg0) throws Exception {
																		loadData(null);
																	}
																}, detailperkuliahans);
													}
												});

											}
										});

									}

								}
							});

				}

			});
			toolbar.appendChild(button);

			MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
			cetakSksDosen.setOrient("vertical");
			cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_sinkronkan_semua"));
			toolbar.appendChild(cetakSksDosen);
			cetakSksDosen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi perkuliahan"));

							new Thread(new Runnable() {

								@Override
								public void run() {
									// Thread latar: openSession DEDIKASI (bukan currentNativeSession). Method
									// reInit* yang dipanggil singkronkan (mis. pengumpulan email pertemuan)
									// memanggil HibernateUtil.closeSession() untuk session ThreadLocal-nya
									// sendiri; bila kita ikut memakai session ThreadLocal, session kita ikut
									// TERTUTUP di tengah proses → "Session is closed!". Session dedikasi tidak
									// tersimpan di ThreadLocal sehingga kebal. Ditutup di finally.
									Session session = null;
									try {
										session = HibernateUtil.openSession();
										perkuliahan.singkronkan(session);
									} finally {
										if (session != null) {
											try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1934");}
											try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1935");}
											try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1936");}
										}
										HibernateUtil.closeSession();
										label.setValue("");
									}

								}
							}).start();

							final Timer timer = new Timer(500);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									// // System.out.println("process = " +
									// label.getValue());
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {

										DetailperkuliahanForPenilaianHelper.this.loadData(true);
										Clients.clearBusy();
										MyMessageboxConfig.show("Singkronisasi perkuliahan berhasil dilakukan",
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
										timer.detach();

									}

								}
							});
							timer.start();

						}
					});
				}
			});

		} else {
			sembunyikanNilaiJikaBelumDiverifikasi.setVisible(false);
		}

		button = new MyToolbarbuttonConfig("Komentar", "/img/m3.gif");
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				KomentarPerkuliahanHelper komentarHelper = new KomentarPerkuliahanHelper(perkuliahan);

				komentarHelper.display(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadDataKomentar();
					}
				});

			}

		});
		button.setParent(toolbar);

		buttonMasukkanNilaiAbsen.setOrient("vertical");
		buttonMasukkanNilaiAbsen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!Detailperkuliahan.formatNilaiSiapDihitung(formatNilais)) {
					MyMessageboxConfig.show(
							"Nilai kehadiran tidak diproses karena format nilai belum lengkap atau total bobotnya bukan 100%.",
							"Format nilai belum valid", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						for (Long detailperkuliahanid : detailperkuliahans) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {

								FormatNilai formatNilaiAbsen = null;
								for (FormatNilai formatNilai : formatNilais) {
									if (formatNilai != null && formatNilai.getNama() != null
											&& (formatNilai.getNama().toLowerCase().trim().contains("absen")
													|| formatNilai.getNama().toLowerCase().trim().contains("hadir")
													|| formatNilai.getNama().toLowerCase().trim()
															.contains("presensi"))) {
										formatNilaiAbsen = formatNilai;
										break;
									}
								}

								if (formatNilaiAbsen != null) {
									Map<String, Integer> absensi = Perkuliahan.hitungStatus(statusPertemuan,
											detailperkuliahan.getMahasiswa().getId());

									int semua = absensi.get("T") == null ? 0 : absensi.get("T");
									int masuk = absensi.get("M") == null ? 0 : absensi.get("M");
									int sakit = absensi.get("S") == null ? 0 : absensi.get("S");
									int izin = absensi.get("I") == null ? 0 : absensi.get("I");

									double nilaiAbsensi = semua == 0 ? 0.0
											: ((masuk * 100.0) + (sakit * 0.5 * 100.0) + (izin * 0.5 * 100.0)) / semua;

									detailperkuliahan.populateDetailNilai(formatNilaiAbsen, null, nilaiAbsensi, true,
											perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(), tbmuser);
									Matakuliah matakuliah = detailperkuliahan == null ? null
											: detailperkuliahan.getPerkuliahan() != null
													? detailperkuliahan.getPerkuliahan().getMatakuliah()
													: detailperkuliahan.getMatakuliahKonversi();
									Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);
									NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
											detailperkuliahan.getMahasiswa().getTahunangkatan(),
											detailperkuliahan.getMahasiswa().getJurusan(),
											detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
											detailperkuliahan.getTahunAkademik(),
											detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
													: Perkuliahan.GANJIL,
											matakuliah == null ? "" : matakuliah.getKode(),
											matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

									detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
									detailperkuliahan
											.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
									detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

									detailperkuliahan.setTotalNilai(total);

									Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true,
											formatNilais);

									nilaiHuruf = Common.getNilaiHuruf(totalSementara,
											detailperkuliahan.getMahasiswa().getTahunangkatan(),
											detailperkuliahan.getMahasiswa().getJurusan(),
											detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
											detailperkuliahan.getTahunAkademik(),
											detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
													: Perkuliahan.GANJIL,
											matakuliah == null ? "" : matakuliah.getKode(),
											matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

									detailperkuliahan.setTotalNilaiSementara(totalSementara);
									detailperkuliahan.setNilaiHurufSementara(
											nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
									detailperkuliahan
											.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

									// Timer callback: currentNativeSession() bisa stale → openSession eksplisit
									Session session = HibernateUtil.openSession();
									try {
										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, detailperkuliahan);
										session.getTransaction().commit();
									} catch (Exception eSess) {
										try { if (session.getTransaction() != null && session.getTransaction().isActive()) { session.getTransaction().rollback(); } } catch (Exception eRb) {}
										throw eSess;
									} finally {
										try { session.clear(); session.disconnect(); session.close(); } catch (Exception eClose) {}
									}

								} else {
									MyMessageboxConfig.show("Format nilai absen tidak ditemukan", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}
							}
						}
						loadData(true);
					}
				});

			}
		});
		toolbar.appendChild(buttonMasukkanNilaiAbsen);
		buttonMasukkanNilaiAbsen.setVisible(
				tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null && !download.isDisabled());

		/*
		 * Tombol "Reset": mengosongkan SELURUH nilai mahasiswa pada kelas ini menjadi semula
		 * (kosong/0). Hanya tampil selama belum dikunci dan masih boleh edit (syarat sama dengan
		 * "Masukkan Nilai Absen": bukan mahasiswa + getDikunci()==null + !download.isDisabled()).
		 * Selalu meminta konfirmasi peringatan dulu karena bersifat destruktif & tak bisa dibatalkan.
		 */
		final MyToolbarbuttonConfig buttonReset = new MyToolbarbuttonConfig("Reset", "/img/svg/arrow-go-back-line.svg");
		buttonReset.setOrient("vertical");
		buttonReset.setTooltiptext("Reset / kosongkan seluruh nilai mahasiswa menjadi semula");
		buttonReset.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"PERINGATAN: Anda akan MERESET SELURUH nilai mahasiswa pada kelas ini menjadi SEMULA (kosong/0).\n\n"
								+ "Seluruh nilai per komponen (Sub-CPMK) dan nilai tambahan untuk SEMUA mahasiswa akan "
								+ "dikosongkan, lalu total nilai dihitung ulang menjadi 0 (E). Tindakan ini TIDAK DAPAT "
								+ "dibatalkan.\n\nApakah Anda yakin ingin melanjutkan?",
						"Peringatan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.EXCLAMATION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								if (!Detailperkuliahan.formatNilaiSiapDihitung(formatNilais)) {
									MyMessageboxConfig.show(
											"Reset dibatalkan karena format nilai belum lengkap atau total bobotnya bukan 100%.",
											"Format nilai belum valid", MyMessageboxConfig.OK,
											MyMessageboxConfig.EXCLAMATION);
									return;
								}

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										for (Long detailperkuliahanid : detailperkuliahans) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan == null) {
												continue;
											}

											/*
											 * Kembalikan nilai yang masih terbuka ke kondisi semula. Nilai komponen
											 * terkunci dan snapshot permanennya harus tetap utuh. Total dinolkan
											 * sebelum dihitung ulang supaya refreshNilaiKeDefault() tidak membangun
											 * kembali nilai terbuka dari total lama.
											 */
											// Reset tidak boleh menghapus komponen yang sudah dikunci. Model akan
											// mengosongkan nilai terbuka dan memulihkan setiap entri terkunci dari
											// kolom snapshot permanen. Snapshot sengaja tidak pernah dikosongkan.
											detailperkuliahan.resetDetailNilaiYangTidakDikunci(formatNilais);
											detailperkuliahan.setTotalNilai(0.0);
											detailperkuliahan.setTotalNilaiSementara(0.0);

											Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
													? detailperkuliahan.getPerkuliahan().getMatakuliah()
													: detailperkuliahan.getMatakuliahKonversi();

											Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);
											NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
													detailperkuliahan.getMahasiswa().getTahunangkatan(),
													detailperkuliahan.getMahasiswa().getJurusan(),
													detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
													detailperkuliahan.getTahunAkademik(),
													detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
															: Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

											detailperkuliahan
													.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
											detailperkuliahan
													.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
											detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
											detailperkuliahan.setTotalNilai(total);

											Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true,
													formatNilais);
											nilaiHuruf = Common.getNilaiHuruf(totalSementara,
													detailperkuliahan.getMahasiswa().getTahunangkatan(),
													detailperkuliahan.getMahasiswa().getJurusan(),
													detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
													detailperkuliahan.getTahunAkademik(),
													detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
															: Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
											detailperkuliahan.setTotalNilaiSementara(totalSementara);
											detailperkuliahan.setNilaiHurufSementara(
													nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
											detailperkuliahan.setTotalIPSementara(
													nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

											Session session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, detailperkuliahan);
											session.getTransaction().commit();
											HibernateUtil.closeSession();
										}

										loadData(true);
									}
								});
							}
						});
			}
		});
		toolbar.appendChild(buttonReset);
		buttonReset.setVisible(
				tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null && !download.isDisabled());

		nilai0masukNilaiAkhir.setDisabled(editDisable);
		jikaNilai0masukNilaiAkhir.setDisabled(editDisable);
		hanyaInputNilaiHuruf.setDisabled(editDisable);

		nilai0masukNilaiAkhir.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				perkuliahan.setNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir(nilai0masukNilaiAkhir.isChecked());
				Common.refreshUpdate(perkuliahan);
				Common.realoadNilai(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						}, detailperkuliahans);
			}
		});
		nilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_pilihan_nilai_0_tidak_masuk_penghitungan_nilai_akhir") && tbmuser.getMahasiswa() == null
				&& perkuliahan.getDikunci() == null && !download.isDisabled());

		jikaNilai0masukNilaiAkhir.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				perkuliahan.setJikaAdaNilai0TidakMenghitungNilaiAkhir(jikaNilai0masukNilaiAkhir.isChecked());
				Common.refreshUpdate(perkuliahan);
				Common.realoadNilai(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						}, detailperkuliahans);
			}
		});
		jikaNilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_jika_ada_nilai_0_tidak_masuk_penghitungan_nilai_akhir") && tbmuser.getMahasiswa() == null
				&& perkuliahan.getDikunci() == null && !download.isDisabled());

		hanyaInputNilaiHuruf.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				perkuliahan.setHanyaInputNilaiHuruf(hanyaInputNilaiHuruf.isChecked());
				Common.refreshUpdate(perkuliahan);
				Common.realoadNilai(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						}, detailperkuliahans);
			}
		});
		hanyaInputNilaiHuruf.setVisible(Common.bolehKonfigurasi("tampilkan_hanya_input_nilai_huruf") && tbmuser.getMahasiswa() == null
				&& perkuliahan.getDikunci() == null && !download.isDisabled());

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.getFormatNilais(perkuliahan, true);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, true);
					}
				});
			}

		});
		button.setParent(toolbar);

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Ambil Nilai dri Feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setOrient("vertical");
			buttonTagihan.setStyle("font-size:8px;");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Data nilai yang sudah dinputkan di sistem atau nilai mahasiswa lebih dari nilai 0, tidak bisa diambil dari Feeder. Hanya perkuliahan yg belum dinilai saja yg bisa diambil dari feeder.\nApakah Anda yakin ingin melanjutkan ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];

										if (!EksporFromFeederAction.exists(url)) {

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final List<String> errorLog = new ArrayList<String>();
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												loadData(true);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + (token == null || token.isEmpty() ? "(gagal)" : "(berhasil, disamarkan)"));

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													int size = detailperkuliahans.size();
													int index = 1;
													for (Long detailperkuliahanid : detailperkuliahans) {
														Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
																.ambilData(Detailperkuliahan.class,
																		detailperkuliahanid.toString());
														if (detailperkuliahan != null) {
															Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
															myLabelProsesDetail
																	.setValue(
																			"Memproses " + mahasiswa.getNim() + " "
																					+ mahasiswa.getNama() + " ("
																					+ Common.numberFormat.get().format(
																							(index * 100.0) / size)
																					+ "%");
															index++;
															try {
																MahasiswaAction.ambilNilaiDariFeeder(feederConnector,
																		token, 0, mahasiswa, tbmuser, null,
																		perkuliahan);
															} catch (Exception e) {
																errorLog.add("[" + mahasiswa.getNim() + " " + mahasiswa.getNama() + "] " + e.getMessage());
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:2413");
															}
														}
													}

													// FIX "gagal diam-diam": sebelumnya penanda SUKSES (setValue(""))
													// berada DI LUAR try, sehingga tetap dijalankan walau blok try di
													// atas melempar exception (mis. gagal konek/parse port) - popup
													// menutup dengan status "berhasil" padahal proses ambil nilai
													// dari Feeder sebenarnya gagal total. Sekarang penanda sukses
													// adalah pernyataan TERAKHIR di dalam try, sehingga hanya
													// tercapai bila tidak ada exception yang lolos.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengambilan data nilai perkuliahan \"" + perkuliahan.info()
																			+ "\" dari Neo Feeder",
																	null, e,
																	new String[] {
																			"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																			"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																			"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																	.replace("\n", " "));
												}
											}
										}).start();
									}

								}
							});

				}
			});
			toolbar.appendChild(buttonTagihan);
		}

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setDisabled(editDisable);
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiDetailPerkuliahanHelper revisiHelper = new RevisiDetailPerkuliahanHelper(perkuliahan,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								perkuliahan.belum("detailperkulaiahan");
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										DetailperkuliahanForPenilaianHelper.this.loadData(true);
									}
								});
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		// ── Restore: kembalikan bobot + nilai ke revisi terakhir per tanggal terpilih ──
		// Aktif hanya bila BELUM dikunci & masih waktu entry (!editDisable). KECUALI admin (konfigurasi
		// "aktifkan_restore_untuk_admin_walau_terkunci" default AKTIF) → tetap aktif walau terkunci.
		{
			MyToolbarbuttonConfig buttonRestore = new MyToolbarbuttonConfig("Restore", "/img/svg/clock-history.svg");
			buttonRestore.setOrient("vertical");
			boolean adminOverride = Common.getApakahAdmin()
					&& Common.bolehKonfigurasi("aktifkan_restore_untuk_admin_walau_terkunci");
			boolean bolehRestore = adminOverride || (perkuliahan.getDikunci() == null && !editDisable);
			buttonRestore.setDisabled(!bolehRestore);
			buttonRestore.setTooltiptext(adminOverride
					? "Restore nilai/bobot ke revisi tanggal tertentu (admin: aktif walau terkunci)"
					: "Restore nilai/bobot ke revisi tanggal tertentu (aktif saat nilai belum dikunci)");
			buttonRestore.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					RestoreNilaiPerkuliahanHelper.bukaDialog(perkuliahan, detailperkuliahans, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							perkuliahan.belum("detailperkulaiahan");
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									DetailperkuliahanForPenilaianHelper.this.loadData(true);
								}
							});
						}
					});
				}
			});
			buttonRestore.setParent(toolbar);
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("95%");
		grid.setMold("paging");
		grid.setPageSize(10000);
		grid.setParent(groupbox);

		formatNilais = Common.getFormatNilais(perkuliahan);

		// Hitung lebar kolom adaptif agar total % ~ 95 tanpa meluap saat komponen banyak
		int _n = formatNilais.size();
		int _verPct = adaProsesVerifikasiNilai ? 5 : 0;
		// Kolom tetap: Foto(70px) + Smt(5%) + Minimal(10%) + Total(8%) + Ver(0-5%)
		// Minimal dipersempit 15%->10% & cap per-komponen dinaikkan 10->14% agar nama
		// ranah/jenis evaluasi (mis. "Kognitif/ Pengetahuan") muat di header kolom.
		int _budget = 95 - (28 + _verPct); // sisa untuk Mahasiswa + N kolom FormatNilai
		int perColPct = _n > 0 ? Math.max(4, Math.min(14, (_budget - 15) / _n)) : 9;
		int mhsColPct = Math.max(15, _budget - _n * perColPct);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		columnMahasiswa = new MyColumnConfig();
		columnMahasiswa.setParent(columns);
		columnMahasiswa.setLabel("Mahasiswa");
		columnMahasiswa.setWidth(mhsColPct + "%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		Vbox k = new Vbox();
		k.setParent(column);
		// RAPIKAN kolom "Minimal nilai kehadiran": label panjang & baris "Min: [box] %" tampak berdesakan.
		// Kini label ringkas + penjelasan lengkap dipindah ke tooltip, dan baris input ditata rapi (flex,
		// rata, tak wrap acak). Fungsinya sama: persen kehadiran minimal agar Nilai Total dihitung; di
		// bawah nilai ini total menjadi 0 (itulah sebab sebagian mahasiswa "0 (E)" walau komponen terisi).
		MyLabelKecilBold aa;
		k.appendChild(aa = new MyLabelKecilBold("Min. Kehadiran"));
		aa.setMultiline(true);
		aa.setStyle("display:block;text-align:center;line-height:1.15;");
		aa.setTooltiptext(
				"Minimal persen kehadiran mahasiswa agar Nilai Total dihitung. Bila kehadiran mahasiswa DI BAWAH nilai ini, Nilai Total-nya menjadi 0 (E) meskipun komponen nilainya terisi. Kosongkan / isi 0 untuk menonaktifkan.");

		boolean nggakBolehUbah = perkuliahan.getDikunci() != null || !edit

				|| (tbmuser.getMahasiswa() != null && !mahasiswaBolehUbahNilai)

				|| (!aktifPenilaian
						&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)));

		if (nggakBolehUbah) {
			org.zkoss.zul.Div dMin = new org.zkoss.zul.Div();
			dMin.setStyle("display:flex;align-items:center;justify-content:center;gap:3px;white-space:nowrap;");
			dMin.appendChild(new MyLabelAgakKecil("Min:"));
			dMin.appendChild(new MyLabelAgakKecil(
					Common.numberFormat.get().format(perkuliahan.getPersenKehadiranDinilai0())));
			dMin.appendChild(new MyLabelAgakKecil("%"));
			k.appendChild(dMin);
		} else {

			final MyDoublebox min;
			org.zkoss.zul.Div dMin = new org.zkoss.zul.Div();
			dMin.setStyle("display:flex;align-items:center;justify-content:center;gap:3px;white-space:nowrap;");
			dMin.appendChild(new MyLabelAgakKecil("Min:"));
			dMin.appendChild(min = new MyDoublebox(perkuliahan.getPersenKehadiranDinilai0()));
			dMin.appendChild(new MyLabelAgakKecil("%"));
			k.appendChild(dMin);
			min.setCols(1);
			min.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					perkuliahan.setPersenKehadiranDinilai0(min.getValue());
					Common.refreshUpdate(perkuliahan);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai,
													true);
										}
									}, detailperkuliahans);
						}
					});
				}
			});

		}

		this.columns = new ArrayList<Column>();

		int index = 1;
		for (final FormatNilai formatNilai : formatNilais) {
			column = new MyColumnConfig();
			this.columns.add(column);
			column.setParent(columns);
			column.setVisible(formatNilai.getPersen() > 0.01);
			column.setWidth(perColPct + "%");

			column.setAlign("right");
			Vbox hb = new Vbox();
			hb.setParent(column);

			Vbox lbl;
			try {
				String namaLengkap = formatNilai == null ? "" : formatNilai.getNama();
				(lbl = RevisiHelper.createNewRevisi(FormatNilai.class, formatNilai,
						formatNilai == null ? ""
								: ambilNamaFormatNilaiRingkas(formatNilai) + " " + formatNilai.getPersen() + "%"))
						.setParent(hb);
				lbl.setStyle("font-size: xx-small;text-align: center;");
				lbl.setTooltiptext(namaLengkap);
				lbl.setParent(hb);
				lbl.setWidth("100%");
				lbl.setHeight("100%");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:2629");
			}

			// Vbox (bukan Hbox) agar nomor urut & combobox ranah/jenis evaluasi menumpuk
			// vertikal -> masing-masing dapat lebar penuh kolom (combobox "Kognitif" tak terpotong).
			// sclass "ranah-cell" dipakai CSS utk mengecilkan font combobox ranah (lihat css_utama.css).
			Vbox hboxD = new Vbox();
			hboxD.setWidth("100%");
			hboxD.setSclass("ranah-cell");
			hboxD.setParent(hb);

			try {
				Integer nomorUrutData = formatNilai.getNomorUrut();
				Long n = null;
				try {
					JSONObject jsonData = new JSONObject(perkuliahan.getPembombotanNilai().getNomorUrutFormat());
					n = jsonData.isNull(formatNilai.getStatusPertemuan().getId().toString()) ? null
							: ais.common.CommonJSONUtil.ambilLong(jsonData,
									formatNilai.getStatusPertemuan().getId().toString());
					if (n != null) {
						nomorUrutData = n.intValue();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:2652");
				}

				if (nggakBolehUbah || n != null) {
					new Label(Common.numberFormat.get().format(nomorUrutData == null ? index : nomorUrutData))
							.setParent(hboxD);
				} else {
					final Intbox nomorUrut = new Intbox(
							formatNilai.getNomorUrut() == null ? index : formatNilai.getNomorUrut());
					nomorUrut.setCols(1);
					nomorUrut.setParent(hboxD);
					nomorUrut.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							formatNilai.setNomorUrut(nomorUrut.getValue());
							Common.refreshUpdate(formatNilai);
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, null);
								}
							});
						}
					});
				}
				index++;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:2680");
				// TODO: handle exception
			}

			if (nggakBolehUbah) {
				new Label(formatNilai.getJenisEvaluasi() == null ? "" : formatNilai.getJenisEvaluasi().getNama())
						.setParent(hboxD);
			} else {
				final Combobox jenisEvaluasi = new Combobox();
				jenisEvaluasi.setReadonly(true);
				Common.insertCombo(jenisEvaluasi, new String[] { "nama" }, "keterangan", JenisEvaluasi.class,
						Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisEvaluasi, formatNilai.getJenisEvaluasi());
				// Isi penuh lebar kolom (jangan dipaksa 2 kolom karakter). Font dikecilkan lewat CSS
				// (.ranah-cell .z-combobox-inp) agar nama ranah panjang muat & tidak terpotong.
				jenisEvaluasi.setWidth("95%");
				jenisEvaluasi.setTooltiptext(
						formatNilai.getJenisEvaluasi() == null ? "" : formatNilai.getJenisEvaluasi().getNama());
				jenisEvaluasi.setParent(hboxD);
				jenisEvaluasi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						formatNilai.setJenisEvaluasi((JenisEvaluasi) (jenisEvaluasi.getSelectedItem() == null ? null
								: jenisEvaluasi.getSelectedItem().getValue()));
						Common.refreshUpdate(formatNilai);
					}
				});
			}

			final MyToolbarbuttonConfig bukaKunciDetail = new MyToolbarbuttonConfig(
					formatNilai.getKunci() == null ? "" : formatNilai.getKunci().getUserNama(), "/img/svg/unlock.svg");
			final MyToolbarbuttonConfig kunciDetail = new MyToolbarbuttonConfig(
					formatNilai.getKunci() == null ? "" : formatNilai.getKunci().getUserNama(),
					"/img/Lock-Lock-icon.png");

			bukaKunciDetail.setStyle("font-size:8px;");
			kunciDetail.setStyle("font-size:8px;");

			if (formatNilai.getKunci() != null) {
				bukaKunciDetail.setTooltiptext("Dikunci oleh " + formatNilai.getKunci().getUserId());
			}
			if (tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null
					&& (formatNilai.getStatusPertemuan() == null || !formatNilai.getStatusPertemuan().getKunci())) {

				kunciDetail.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show(
								"Apakah yakin ingin mengunci nilai ini ?\n\nCatatan : Nilai akan terkunci dan tidak bisa dirubah oleh orang lain kecuali jika anda membuka kunci penilain kembali.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											if (Common.bolehKonfigurasi("sebelum_dikunci_harus_diverifikasi_dulu", Konfigurasi.TIDAK_AKTIF)) {

												Collection<Long> detailperkuliahans = perkuliahan
														.ambilDetailperkuliahan(null, null, "",
																urutkanBerdasarkanNama.isChecked(), true);

												for (Long detailperkuliahanid : detailperkuliahans) {
													Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
															.ambilData(Detailperkuliahan.class,
																	detailperkuliahanid.toString());
													if (detailperkuliahan != null) {

														boolean verif = detailperkuliahan
																.retreiveDetailVerifikasiNilai(formatNilai);
														if (!verif) {
															MyMessageboxConfig.show("Semua nilai \""
																	+ formatNilai.getNama()
																	+ "\" harus diverifikasi dulu sebelum bisa di kunci",
																	"Peringatan", MyMessageboxConfig.OK,
																	MyMessageboxConfig.INFORMATION);
															return;
														}
													}
												}

											}

											// Bekukan nilai kolom ini untuk seluruh mahasiswa sebelum status
											// kunci dipasang. Nilai disimpan di detail nilai utama sekaligus
											// snapshot, sehingga sinkronisasi eksternal tidak dapat menimpanya.
											Collection<Long> idsSnapshotKolom = perkuliahan.ambilDetailperkuliahan(
													null, null, "", urutkanBerdasarkanNama.isChecked(), true);
											for (Long idSnapshotKolom : idsSnapshotKolom) {
												Detailperkuliahan dpkKolom = (Detailperkuliahan) GeneralValueObject
														.ambilData(Detailperkuliahan.class, idSnapshotKolom.toString());
												if (dpkKolom != null) {
													dpkKolom.bekukanDetailNilai(formatNilai);
													Common.refreshUpdate(dpkKolom);
												}
											}

											formatNilai.setKunci(Common.getCurrentUser());
											Common.refreshUpdate(formatNilai);

											loadData(null);

											bukaKunciDetail.setLabel(formatNilai.getKunci() == null ? ""
													: formatNilai.getKunci().getUserNama());

											kunciDetail.setVisible(formatNilai.getKunci() == null);
											bukaKunciDetail.setVisible(formatNilai.getKunci() != null);
											if (formatNilai.getKunci() != null) {
												bukaKunciDetail.setTooltiptext(
														"Dikunci oleh " + formatNilai.getKunci().getUserId());
											}

											bukaKunciDetail.setDisabled((formatNilai.getKunci() != null
													&& Common.getCurrentUser().getUserId() != null
													&& !formatNilai.getKunci().getUserId()
															.equals(Common.getCurrentUser().getUserId()))
													|| !edit || (!aktifPenilaian && (konfigurasi.getNilai() == null
															|| !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))));

										}

									}
								});
					}
				});
				kunciDetail.setVisible(formatNilai.getKunci() == null);
				kunciDetail.setDisabled(!edit || (!aktifPenilaian
						&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))));

				kunciDetail.setParent(toolbar);
				kunciDetail.setOrient("vertical");

				bukaKunciDetail.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show(
								"Apakah yakin ingin membuka kunci nilai ini ?\n\nCatatan : Nilai akan terbuka dan bisa dirubah oleh orang lain yang berhak mengakses penilaian anda (misalnya: admin).",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											formatNilai.setKunci(null);
											Common.refreshUpdate(formatNilai);

											loadData(null);

											kunciDetail.setVisible(formatNilai.getKunci() == null);
											bukaKunciDetail.setVisible(formatNilai.getKunci() != null);

										}

									}
								});
					}
				});
				bukaKunciDetail.setVisible(formatNilai.getKunci() != null);
				if (formatNilai.getKunci() != null) {
					bukaKunciDetail.setTooltiptext("Dikunci oleh " + formatNilai.getKunci().getUserId());
				}
				bukaKunciDetail
						.setDisabled((formatNilai.getKunci() != null && Common.getCurrentUser().getUserId() != null
								&& !formatNilai.getKunci().getUserId().equals(Common.getCurrentUser().getUserId()))
								|| !edit || (!aktifPenilaian && (konfigurasi.getNilai() == null
										|| !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))));

				bukaKunciDetail.setOrient("vertical");
				kunciDetail.setOrient("vertical");

				bukaKunciDetail.setVisible(formatNilai.getKunci() != null);
				bukaKunciDetail.setDisabled(tbmuser == null || formatNilai.getKunci() == null
						|| !formatNilai.getKunci().getUserId().equals(tbmuser.getUserId()));
				kunciDetail.setVisible(formatNilai.getKunci() == null);

				Hbox hboxK = new Hbox();
				hb.appendChild(hboxK);

				hboxK.appendChild(bukaKunciDetail);
				hboxK.appendChild(kunciDetail);

				if (adminBoleh) {
					bukaKunciDetail.setDisabled(false);
				}

				if (adaProsesVerifikasiNilai && tbmuser.ambilDosen() == null) {

					final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig();

					hboxK.appendChild(checkboxConfig);

					Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
							urutkanBerdasarkanNama.isChecked(), false);
					boolean checkData = true;
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {

							boolean verif = detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai);
							checkData = checkData && verif;

						}
					}

					checkboxConfig.setChecked(checkData);
					checkboxConfig.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
									urutkanBerdasarkanNama.isChecked(), false);

							for (Long detailperkuliahanid : detailperkuliahans) {
								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
										.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
								if (detailperkuliahan != null) {

									Session session = HibernateUtil.currentNativeSession();
									session.refresh(detailperkuliahan);
									Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
									detailperkuliahan.populateDetailNilai(formatNilai, null, nilai,
											checkboxConfig.isChecked(), tbmuser);
									session.getTransaction().begin();
									session.update(detailperkuliahan);
									session.getTransaction().commit();

									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										session.close();
									}
									HibernateUtil.closeSession();

								}
							}

							loadData(null);
						}
					});
				}
			}

		}
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setWidth("8%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth(adaProsesVerifikasiNilai ? "5%" : "0%");

		Vbox hboxK = new Vbox();
		column.appendChild(hboxK);

		hboxK.appendChild(new Label(ais.common.Common.getBahasaConfig("Verify")));

		if (adaProsesVerifikasiNilai && tbmuser.ambilDosen() == null) {

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig();

			Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
					urutkanBerdasarkanNama.isChecked(), false);
			boolean checkData = true;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					for (FormatNilai formatNilai : formatNilais) {
						boolean verif = detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai);
						checkData = checkData && verif;
					}
				}
			}

			hboxK.appendChild(checkboxConfig);
			checkboxConfig.setChecked(checkData);
			checkboxConfig.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
							urutkanBerdasarkanNama.isChecked(), false);

					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {

							Session session = HibernateUtil.currentNativeSession();
							session.refresh(detailperkuliahan);
							for (FormatNilai formatNilai : formatNilais) {
								Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
								detailperkuliahan.populateDetailNilai(formatNilai, null, nilai,
										checkboxConfig.isChecked(), tbmuser);
							}
							detailperkuliahan.setVerify(checkboxConfig.isChecked() ? Detailperkuliahan.VERIFIED
									: Detailperkuliahan.NOT_VERIFIED);
							session.getTransaction().begin();
							session.update(detailperkuliahan);
							session.getTransaction().commit();

							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
							HibernateUtil.closeSession();

						}
					}

					loadData(null);
				}
			});
		}

		loadData(refresh);
		Common.freeze(grid, perkuliahan.getDikunci() != null);
		upload.setDisabled(perkuliahan.getDikunci() != null || editDisable);
		download.setDisabled(perkuliahan.getDikunci() != null || editDisable);
		btn.setDisabled(perkuliahan.getDikunci() != null || editDisable);
		warning.setVisible(perkuliahan.getDikunci() == null && !editDisable);

		final Tabpanel detailAsistenMahasiswa = new ais.ui.util.MyTabpanel();
		detailAsistenMahasiswa.setParent(tabpanels);

		tab1AsistenMahasiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (detailAsistenMahasiswa.getChildren().isEmpty()) {
					DetailperkuliahanForPenilaianHelper.displayAsistenMahasiswa(detailAsistenMahasiswa, kuliyah);

				}
			}
		});

//		final Tabpanel detailRekapKehadiran = new ais.ui.util.MyTabpanel();
//		detailRekapKehadiran.setParent(tabpanels);
//
//		tab1LihatRekapKehadiran.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				Common.clear(detailRekapKehadiran);
//				DashboardRekapAbsensiPerMahasiswa dashboardRekapAbsensiMahasiswa = new DashboardRekapAbsensiPerMahasiswa(
//						kuliyah);
//				dashboardRekapAbsensiMahasiswa.setHeight("500px");
//				detailRekapKehadiran.appendChild(dashboardRekapAbsensiMahasiswa);
//				detailRekapKehadiran.setStyle("min-height: 500px;");
//
//			}
//		});

		final Tabpanel detailRekapTugas = new ais.ui.util.MyTabpanel();
		detailRekapTugas.setParent(tabpanels);

		tab1LihatRekapTugas.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapTugas);
				// Tiru pola Rekap Total Nilai yang BERHASIL tampil (GradingHelper): isi rekap
				// (borderlayout) ditanam LANGSUNG ke tabpanel dengan tinggi PASTI, BUKAN
				// dibungkus MyWindow (di dalam tabpanel window membuat borderlayout collapse
				// 0px sehingga konten tidak tampil).
				tanamkanRekapKeTabpanel(new RekapHasilTugasPerVoPertemuan(true, kuliyah), detailRekapTugas);
				detailRekapTugas.setStyle("min-height: 2000px;");

			}
		});

		final Tabpanel detailRekapUjian = new ais.ui.util.MyTabpanel();
		detailRekapUjian.setParent(tabpanels);

		tab1LihatRekapUjian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapUjian);
				// Sama dengan Rekap Tugas: tanam isi langsung ke tabpanel (lihat GradingHelper).
				tanamkanRekapKeTabpanel(new RekapHasilUjianPerVoPertemuan(true, kuliyah), detailRekapUjian);
				detailRekapUjian.setStyle("min-height: 2000px;");

			}
		});

		final Tabpanel detailRekapTugasKelompok = new ais.ui.util.MyTabpanel();
		detailRekapTugasKelompok.setParent(tabpanels);

		tab1LihatRekapTugasKelompok.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapTugasKelompok);
				// Sama dengan Rekap Tugas: tanam isi langsung ke tabpanel (lihat GradingHelper).
				tanamkanRekapKeTabpanel(new RekapHasilTugasKelompokPerVoPertemuan(true, kuliyah),
						detailRekapTugasKelompok);
				detailRekapTugasKelompok.setStyle("min-height: 2000px;");

			}
		});

		final Tabpanel detailRekapNilai = new ais.ui.util.MyTabpanel();
		detailRekapNilai.setParent(tabpanels);

		tab1LihatRekapNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapNilai);
				ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(detailRekapNilai, kuliyah,
						kuliyah.ambilFormatNilai(HibernateUtil.currentSession()).toArray(new FormatNilai[] {}));
				detailRekapNilai.setStyle("min-height: 500px;");
				detailRekapNilai.invalidate();  // paksa render konten tabpanel (fix ZK5: konten tak tampil saat dibangun lazy)

			}
		});

		final Tabpanel detailPrestasi = new ais.ui.util.MyTabpanel();
		detailPrestasi.setParent(tabpanels);
		detailPrestasi.setHeight("500px");
		tab1Prestasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailPrestasi);
				LaporanDaftarPrestasiBelajarWindow daftarPrestasiBelajarWindow = new LaporanDaftarPrestasiBelajarWindow(
						kuliyah);
				daftarPrestasiBelajarWindow.setHeight("100%");
				daftarPrestasiBelajarWindow.setWidth("100%");
				daftarPrestasiBelajarWindow.setTitle("");
				detailPrestasi.appendChild(daftarPrestasiBelajarWindow);
				detailPrestasi.setStyle("min-height: 500px;");
				detailPrestasi.invalidate();  // paksa render konten tabpanel (fix ZK5: konten tak tampil saat dibangun lazy)

			}
		});

//		final Tabpanel detailPrestasiSemua = new ais.ui.util.MyTabpanel();
//		detailPrestasiSemua.setParent(tabpanels);
//		detailPrestasiSemua.setHeight("500px");
//		tab1PrestasiSemua.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				Common.clear(detailPrestasiSemua);
//				LaporanDaftarPrestasiBelajarWindow daftarPrestasiBelajarWindow = new LaporanDaftarPrestasiBelajarWindow(
//						kuliyah, null);
//				daftarPrestasiBelajarWindow.setHeight("100%");
//				daftarPrestasiBelajarWindow.setWidth("100%");
//				daftarPrestasiBelajarWindow.setTitle("");
//				detailPrestasiSemua.appendChild(daftarPrestasiBelajarWindow);
//				detailPrestasiSemua.setStyle("min-height: 500px;");
//
//			}
//		});

		gridKomentar = new MyGrid();
		gridKomentar.setMold("paging");
		gridKomentar.setPageSize(20);
		gridKomentar.setParent(groupbox);

		gridKomentar.setVisible(semester > 0);

		Columns columns2 = new Columns();
		columns2.setMenupopup("auto");
		columns2.setParent(gridKomentar);

		MyColumnConfig column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Komentar");
		column2.setWidth("50%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Oleh");
		column2.setWidth("20%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Tanggal");
		column2.setWidth("20%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("");
		column2.setWidth("10%");

		loadDataKomentar();

	}

	/**
	 * Mengisi ulang {@link #gridKomentar} dengan seluruh {@link KomentarPerkuliahan} milik
	 * {@link #perkuliahan}, diurutkan menaik berdasarkan waktu perubahan sehingga catatan terlama
	 * berada di atas dan percakapan terbaca sebagai urutan kronologis.
	 *
	 * <p>Komentar perkuliahan adalah catatan bebas yang dipakai dosen, verifikator, dan admin akademik
	 * untuk saling meninggalkan pesan mengenai kelas ini &mdash; misalnya alasan sebuah nilai dikoreksi
	 * atau permintaan agar komponen tertentu dibuka kembali. Alur verifikasi massal memanggil metode
	 * ini setelah {@code KomentarPerkuliahanHelper} selesai, sehingga catatan yang baru ditulis
	 * langsung tampak.</p>
	 *
	 * <h3>Cara pengambilan data</h3>
	 * <p>Kriteria Hibernate dibangun langsung di sini pada {@code HibernateUtil.currentSession()},
	 * bukan lewat metode pengambilan pada entitas. Penyaringannya hanya satu: {@code perkuliahan} sama
	 * dengan kelas yang sedang dibuka. Karena kelas ini sudah dipilih di layar sebelumnya, tidak ada
	 * penyaringan cakupan tambahan berdasarkan satuan kerja atau kepemilikan. Anotasi
	 * {@code @SuppressWarnings("unchecked")} diperlukan karena {@code Criteria.list()} pada Hibernate
	 * versi ini mengembalikan {@link List} mentah.</p>
	 *
	 * <p>Berbeda dari {@link #loadData(Object)}, metode ini <b>tidak menerima bendera penyegaran</b>:
	 * ia selalu membaca dari basis data. Itu memang wajar untuk daftar yang pendek dan sering berubah.
	 * Perender dipasang baru setiap pemanggilan, model dipasang lewat {@code setModelCheckMobile},
	 * lalu {@code renderAll()} membangun seluruh baris serentak sehingga tombol hapus per baris sudah
	 * terpasang sebelum pengguna menggulir. Kelas gaya {@code non-odd} dipasang terakhir untuk
	 * meniadakan pewarnaan baris berselang-seling.</p>
	 *
	 * <p><b>Prasyarat.</b> {@link #gridKomentar} dan {@link #perkuliahan} harus sudah terisi; metode
	 * ini dipanggil pertama kali di akhir {@link #prosesDisplay} setelah keduanya siap. Ia tidak
	 * membuka transaksi sendiri dan tidak menangkap galat.</p>
	 *
	 * @see KomentarPerkuliahanRenderer
	 * @see #gridKomentar
	 */
	@SuppressWarnings("unchecked")
	public void loadDataKomentar() {
		Session session = HibernateUtil.currentSession();
		List<KomentarPerkuliahan> komentarPerkuliahanPerkuliahans = session.createCriteria(KomentarPerkuliahan.class)
				.addOrder(Order.asc("tanggal_dirubah")).add(Restrictions.eq("perkuliahan", perkuliahan)).list();
		ListModel strset = new SimpleListModel(komentarPerkuliahanPerkuliahans);

		// grid = new MyGrid();grid.setWidth("100%");
		gridKomentar.setRowRenderer(new KomentarPerkuliahanRenderer());
		gridKomentar.setModelCheckMobile(strset);

		gridKomentar.renderAll();
		gridKomentar.setOddRowSclass("non-odd");

	}

	/**
	 * Perender satu baris pada grid komentar perkuliahan. Jauh lebih sederhana daripada
	 * {@link DetailPerkuliahanRenderer}: ia hanya menampilkan isi komentar, penulisnya, waktunya, dan
	 * sebuah tombol hapus bersyarat.
	 *
	 * <p>Sebagai kelas dalam non-statis, ia membaca {@link #tbmuser} milik kelas induk untuk
	 * menentukan siapa yang boleh menghapus, dan memanggil {@link #loadDataKomentar()} untuk menyegarkan
	 * daftar setelah penghapusan. Karena keterikatan itu, instance-nya tidak boleh dipakai ulang di
	 * luar desktop ZK tempat ia dibuat.</p>
	 *
	 * <p><b>Aturan kepemilikan.</b> Tombol hapus hanya terlihat bila nama penulis komentar sama persis
	 * dengan {@code tbmuser.getUserId()} &mdash; jadi setiap orang hanya dapat menghapus komentarnya
	 * sendiri, dan tidak ada pengecualian untuk administrator. Perhatikan bahwa yang dibandingkan
	 * adalah kolom {@code nama} pada komentar terhadap <b>id pengguna</b>, bukan terhadap nama
	 * pengguna; penyimpanan komentar memang mengisi kolom itu dengan id, sehingga perbandingannya
	 * konsisten meski penamaannya membingungkan. Penjagaan ini bersifat <b>visibilitas saja</b>: tidak
	 * ada pemeriksaan ulang kepemilikan di dalam listener penghapusan.</p>
	 *
	 * @see DetailperkuliahanForPenilaianHelper
	 * @see #loadDataKomentar()
	 */
	class KomentarPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Menggambar satu baris komentar sebagai empat sel: isi komentar, penulis, tanggal, dan
		 * toolbar aksi.
		 *
		 * <p>Isi komentar dirender melalui {@code MyHtml}, artinya <b>ditafsirkan sebagai HTML</b>.
		 * Sejak perbaikan XSS tersimpan, isinya dilewatkan {@link #teksAmanHtml(String)} lebih dulu
		 * di sini juga (bukan hanya bergantung pada penyaringan di sisi penulisan
		 * {@code KomentarPerkuliahanHelper}), sehingga karakter markah pada komentar mentah tidak
		 * ditafsirkan sebagai tag HTML.</p>
		 *
		 * <p>Nama penulis dan tanggal ditampilkan sebagai label biasa; tanggal diformat dengan
		 * {@code Common.dateFormat} yang bersifat per-utas. Tombol hapus dipasang di dalam sebuah
		 * {@code Hbox} dan hanya terlihat bagi penulis komentar itu sendiri, dengan penjagaan
		 * {@code tbmuser == null} yang menghasilkan string kosong sehingga sesi tanpa pengguna tidak
		 * pernah cocok dengan penulis mana pun.</p>
		 *
		 * <p>Penghapusan meminta konfirmasi lebih dulu, lalu memanggil {@code Common.refreshDelete}
		 * dan menyegarkan daftar. Berbeda dari penghapusan asisten dosen, kegagalan di sini
		 * <b>tidak</b> ditangkap dan tidak diterjemahkan menjadi pesan ramah &mdash; galat relasi akan
		 * naik sebagai galat ZK. Dalam praktiknya komentar jarang dirujuk data lain, sehingga jalur
		 * itu hampir tidak pernah terjadi.</p>
		 *
		 * @param row  baris grid yang akan diisi; urutan pemasangan komponen menentukan sel mana yang
		 *             terisi dan harus sepadan dengan keempat kolom yang didefinisikan
		 *             {@link #prosesDisplay}.
		 * @param data objek {@link KomentarPerkuliahan}; berbeda dari perender nilai, di sini model
		 *             sudah berisi entitas utuh sehingga tidak perlu diselesaikan dari id.
		 * @throws Exception bila pembangunan komponen gagal.
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final KomentarPerkuliahan komentarPerkuliahanBeans = (KomentarPerkuliahan) data;

			new ais.ui.util.MyHtml(teksAmanHtml(komentarPerkuliahanBeans.getKeterangan())).setParent(row);
			new Label(komentarPerkuliahanBeans.getNama()).setParent(row);
			new Label(Common.dateFormat.get().format(komentarPerkuliahanBeans.getTanggal_dirubah())).setParent(row);

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setParent(toolbar);
			button.setVisible(komentarPerkuliahanBeans.getNama().equals(tbmuser == null ? "" : tbmuser.getUserId()));
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Common.refreshDelete(komentarPerkuliahanBeans);
										loadDataKomentar();
									}

								}
							});
				}
			});

		}

	}

	/**
	 * Mencetak <b>Daftar Nilai Ujian</b> untuk satu kelas dengan pengaturan paling ringkas: tanpa
	 * komponen ZK penampung dan tanpa penyegaran cache komponen nilai.
	 *
	 * <p>Ini adalah bentuk paling pendek dari tiga kelebihan beban {@code onLaporan} dan sekadar
	 * meneruskan ke {@link #onLaporan(Perkuliahan, Component)} dengan {@code component} bernilai
	 * {@code null}. Dipakai oleh pemanggil di luar kelas ini &mdash; antara lain
	 * {@link Perkuliahan} dan modul dasbor &mdash; yang hanya ingin menghasilkan berkas PDF tanpa
	 * mengaitkannya ke sebuah komponen tampilan tertentu.</p>
	 *
	 * <p>Karena {@code refresh} akhirnya bernilai {@code false}, daftar komponen nilai diambil dari
	 * cache. Bila bobot penilaian baru saja diubah, panggil kelebihan beban tiga argumen dengan
	 * {@code refresh} bernilai {@code true} agar kolom laporan mengikuti bobot terbaru.</p>
	 *
	 * @param perkuliahan kelas yang daftar nilainya dicetak; bila {@code null}, kelebihan beban
	 *                    terdalam menampilkan peringatan dan berhenti tanpa melempar.
	 * @throws Exception bila pembangunan parameter laporan gagal.
	 * @see #onLaporan(Perkuliahan, Component, boolean)
	 */
	public static void onLaporan(Perkuliahan perkuliahan) throws Exception {
		onLaporan(perkuliahan, null);
	}

	/**
	 * Mencetak <b>Daftar Nilai Ujian</b> untuk satu kelas dan menautkan keluarannya pada sebuah
	 * komponen ZK, tanpa menyegarkan cache komponen nilai.
	 *
	 * <p>Kelebihan beban ini meneruskan ke {@link #onLaporan(Perkuliahan, Component, boolean)} dengan
	 * {@code refresh} bernilai {@code false}. Inilah bentuk yang dipakai tombol <b>Cetak</b> pada
	 * toolbar layar penilaian, dan juga dipanggil otomatis tepat setelah sebuah kelas berhasil
	 * dikunci &mdash; sehingga penguncian nilai sekaligus menghasilkan berkas daftar nilai sebagai
	 * bukti cetak keadaan saat itu.</p>
	 *
	 * @param perkuliahan kelas yang daftar nilainya dicetak; boleh {@code null} dan ditangani di
	 *                    kelebihan beban terdalam.
	 * @param component   komponen ZK yang menjadi konteks penampil berkas hasil; boleh {@code null}
	 *                    bila laporan cukup diunduh tanpa dikaitkan ke komponen tertentu.
	 * @throws Exception bila pembangunan parameter laporan gagal.
	 * @see #onLaporan(Perkuliahan, Component, boolean)
	 */
	public static void onLaporan(Perkuliahan perkuliahan, Component component) throws Exception {
		onLaporan(perkuliahan, component, false);
	}

	/**
	 * Menyusun dan menghasilkan berkas PDF <b>Daftar Nilai Ujian</b> untuk satu kelas: mengumpulkan
	 * seluruh parameter kop laporan, merakit satu baris data per mahasiswa, melampirkan tanda tangan
	 * pejabat, memilih berkas templat yang sesuai, lalu menyerahkannya ke mesin laporan. Inilah
	 * implementasi sesungguhnya; dua kelebihan beban lainnya hanya meneruskan ke sini.
	 *
	 * <h3>Penjagaan di muka</h3>
	 * <p>Bila {@code perkuliahan} bernilai {@code null}, metode menampilkan peringatan dan
	 * <b>berhenti</b>. Penjagaan ini penting karena seluruh baris di bawahnya memakai objek itu tanpa
	 * pemeriksaan ulang; tanpa penjagaan ini dosen akan melihat galat sistem yang tidak dapat
	 * dipahami, bukan pesan yang menjelaskan bahwa data perkuliahan belum lengkap.</p>
	 *
	 * <h3>Parameter kop laporan</h3>
	 * <p>Peta parameter diawali {@code HashMapGenerator.getRand()} lalu diisi bertingkat: properti
	 * {@link Perkuliahan}, {@link Jurusan}, {@link Fakultas}, dan {@link PerguruanTinggi} disalin
	 * lewat {@code Common.insertProperty} dengan awalan masing-masing, sehingga templat dapat
	 * merujuknya sebagai {@code perkuliahan_*}, {@code jur_*}, {@code fak_*}, dan {@code pt_*}. Judul
	 * laporan berganti menjadi <i>Daftar Nilai Ujian Semester Pendek</i> bila kelas berstatus semester
	 * pendek. Ditambahkan pula kelas, tanggal cetak, fakultas, jenis semester yang diturunkan dari
	 * kegenapan nomor semester, tahun ajaran, kode dan nama mata kuliah, daftar nama dosen yang
	 * dirangkai dengan pemisah garis miring, NIP dosen pertama, jurusan, program, serta nama dan NIP
	 * ketua jurusan bila tersedia.</p>
	 *
	 * <p>Untuk setiap komponen nilai ditulis empat parameter berindeks &mdash; {@code col}<i>i</i>,
	 * {@code col_nama_}<i>i</i>, {@code col_persen_}<i>i</i>, dan {@code persen_}<i>i</i> &mdash;
	 * sehingga <b>urutan {@link #formatNilais} menentukan urutan kolom pada berkas cetak</b>.</p>
	 *
	 * <h3>Baris data per mahasiswa</h3>
	 * <p>Seluruh baris kelas ditelusuri; hanya yang memiliki mahasiswa <b>dan</b> berstatus
	 * {@link Detailperkuliahan#DISETUJUI} yang ikut dicetak. Nama mahasiswa dan nama dosen diubah
	 * menjadi huruf kapital. Nilai per komponen mengikuti kebijakan penyembunyian: bila kelas
	 * menyembunyikan nilai yang belum diverifikasi dan baris masih {@code NOT_VERIFIED}, yang dicetak
	 * adalah nilai dari {@code retreiveDetailNilaiBelumVerify} beserta pasangan kolom
	 * <i>sementara</i>; jika tidak, nilai final. Dengan begitu berkas cetak tidak pernah membocorkan
	 * nilai yang belum disetujui.</p>
	 *
	 * <p>Data pejabat penanda tangan &mdash; kaprodi dari jurusan mahasiswa, serta dekan dan tiga
	 * pembantu dekan dari fakultasnya &mdash; diselesaikan sekali per baris dan <b>hanya diisikan bila
	 * objeknya tersedia</b>. Sikap hati-hati itu disengaja: tanpa pemeriksaan berlapis, kelas dengan
	 * pejabat yang belum diisi akan membanjiri log dengan galat penunjuk kosong. Seluruh blok ini
	 * dibungkus penangkap galat yang mencatat ke {@code ErrorAuditUtil} sehingga kegagalan resolusi
	 * pejabat tidak menggagalkan laporan.</p>
	 *
	 * <h3>Tanda tangan dan pemilihan templat</h3>
	 * <p>Berkas tanda tangan diambil dari {@link LampiranLain} berjenis {@code TTD_DOSEN} milik
	 * kaprodi dan setiap dosen pengampu, dan hanya diterima bila berekstensi gambar yang dikenali
	 * (jpg, png, jpeg, gif, tif, bmp). Nama templat dipilih dari jumlah komponen nilai: nol atau lebih
	 * dari sembilan komponen &mdash; juga kelas bermode nilai huruf &mdash; memakai
	 * {@code Daftar_Nilai_1} sebagai penampung serbaguna; tepat tiga komponen memakai
	 * {@code Daftar_Nilai}; selebihnya memakai {@code Daftar_Nilai_<jumlah>}. Kasus nol dijabarkan
	 * secara eksplisit karena berkas {@code Daftar_Nilai_0} memang tidak pernah ada.</p>
	 *
	 * <p>Kegagalan pembuatan laporan &mdash; lazimnya karena berkas templat {@code .jasper} belum
	 * ter-<i>deploy</i> &mdash; ditangkap dan diubah menjadi pesan informasi yang ramah, bukan
	 * dibiarkan menjadi galat sistem yang memenuhi log. Itu masalah penempatan berkas, bukan cacat
	 * aplikasi, dan pengguna diarahkan menghubungi administrator.</p>
	 *
	 * <p><b>Catatan wewenang.</b> Metode ini {@code static} dan tidak memeriksa siapa yang mencetak.
	 * Siapa pun yang dapat memanggilnya memperoleh daftar nilai lengkap satu kelas beserta NIM dan
	 * nama seluruh mahasiswanya; pembatasan sepenuhnya bergantung pada layar pemanggil.</p>
	 *
	 * @param perkuliahan kelas yang dicetak; {@code null} ditangani dengan peringatan dan penghentian.
	 * @param component   komponen ZK konteks penampil berkas; boleh {@code null}.
	 * @param refresh     {@code true} untuk membaca ulang daftar komponen nilai dari basis data
	 *                    alih-alih memakai cache &mdash; perlu bila bobot penilaian baru diubah.
	 * @throws Exception bila pembangunan parameter atau pembacaan data gagal; kegagalan pada tahap
	 *                   pembuatan berkas itu sendiri sudah ditangani di dalam.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporan(Perkuliahan perkuliahan, Component component, boolean refresh) throws Exception {
		if (perkuliahan == null) {
			// data pertemuan/perkuliahan belum lengkap/tidak ditemukan saat dosen
			// klik tombol laporan -> jangan lanjut (baris2 di bawah memakai
			// perkuliahan tanpa null-check lagi), beri tahu user drpd NPE diam.
			MyMessageboxConfig.show("Data perkuliahan tidak ditemukan/belum lengkap, laporan tidak dapat dibuat.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan, refresh);
		Map parameters = ais.common.HashMapGenerator.getRand();
		if (perkuliahan != null) {
			Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");

			if (perkuliahan.getJurusan() != null) {
				Common.insertProperty(Jurusan.class, perkuliahan.getJurusan(), parameters, "jur");
			}
			if (perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getFakultas() != null) {
				Common.insertProperty(Fakultas.class, perkuliahan.getJurusan().getFakultas(), parameters, "fak");
			}
			if (perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getFakultas() != null
					&& perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() != null) {
				Common.insertProperty(PerguruanTinggi.class,
						perkuliahan.getJurusan().getFakultas().getPerguruanTinggi(), parameters, "pt");
			}

		}

		parameters.put("judul_laporan_nilai",
				(perkuliahan.getStatusSemesterPendek() != null
						&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK))
								? "Daftar Nilai Ujian Semester Pendek"
								: "Daftar Nilai Ujian");

		parameters.put("perkuliahan", perkuliahan.getId());
		parameters.put("kelas",
				perkuliahan.getSemester() + " " + (perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas()));

		parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
		parameters.put("tampil_nilai", 1);
		parameters.put("fakultas",
				perkuliahan.getJurusan() == null || perkuliahan.getJurusan().getFakultas() == null ? ""
						: perkuliahan.getJurusan().getFakultas().getNama());
		parameters.put("jenis_semester",
				((Integer) perkuliahan.getSemester()) % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
		parameters.put("kode_matakuliah",
				perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());
		parameters.put("nama_matakuliah",
				perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

		Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");

		String dosen = "";
		int indexDosen = 1;
		for (Dosen d : perkuliahan.populateDosenBuNama()) {
			Common.insertProperty(Dosen.class, d, parameters, "dosen_" + indexDosen);
			String namaDosen = d == null || d.getNama() == null ? "" : d.getNama().toUpperCase();
			dosen += dosen.isEmpty() ? namaDosen : " / " + namaDosen;
		}
		parameters.put("dosen", dosen);
		parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
		parameters.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
		parameters.put("program", perkuliahan.getProgram());

		if (perkuliahan.getKurikulum() != null && perkuliahan.getJurusan() != null
				&& perkuliahan.getJurusan().getGrupJurusan() != null
				&& perkuliahan.getJurusan().getGrupJurusan().getKajur() != null) {
			parameters.put("nama_kajur", perkuliahan.getJurusan().getGrupJurusan().getKajur().getNama());
			parameters.put("nip_kajur", perkuliahan.getJurusan().getGrupJurusan().getKajur().getCode());
		}

		int i = 1;
		for (FormatNilai formatNilai : formatNilais) {
			parameters.put("col" + i, formatNilai.getNama() + "\n" + formatNilai.getPersen() + "%");
			parameters.put("col_nama_" + i, formatNilai.getNama());
			parameters.put("col_persen_" + i, Common.numberFormat.get().format(formatNilai.getPersen()) + "%");
			parameters.put("persen_" + i, formatNilai.getPersen());
			i++;
		}

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Collection<Long> terdaftar = perkuliahan.ambilDetailperkuliahan();
		for (Long detailperkuliahanid : terdaftar) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null && detailperkuliahan.getMahasiswa() != null
					&& Detailperkuliahan.DISETUJUI.equals(detailperkuliahan.getPersetujuan())) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
				map.put("nim", mahasiswa.getNim());
				map.put("nama", mahasiswa.getNama() == null ? "" : mahasiswa.getNama().toUpperCase());
				map.put("kode_matakuliah",
						perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

				Common.insertProperty(Detailperkuliahan.class, detailperkuliahan, map, "detailperkuliahan");

				i = 1;
				for (FormatNilai formatNilai : formatNilais) {
					if (perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
							&& Detailperkuliahan.NOT_VERIFIED.equals(detailperkuliahan.getVerify())) {
						Double nilai = detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai);
						map.put("nilai_" + i, (nilai));
					} else {
						Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
						map.put("nilai_" + i, (nilai));
					}
					i++;
				}

				if (perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
						&& Detailperkuliahan.NOT_VERIFIED.equals(detailperkuliahan.getVerify())) {
					map.put("nilai", detailperkuliahan.getTotalNilaiSementara());
					map.put("nilai_huruf", detailperkuliahan.getNilaiHurufSementara());
				} else {
					map.put("nilai", detailperkuliahan.getTotalNilai());
					map.put("nilai_huruf", detailperkuliahan.getNilaiHuruf());
				}

				// Data pejabat penanda-tangan (kajur/dekan, kaprodi, pudek1-3). Null-safe: resolusi
				// jurusan/fakultas/pejabat sekali, set nilai HANYA bila objek tersedia — agar tidak
				// membanjiri log dengan NullPointerException saat pejabat belum diisi.
				try {
					Jurusan jurusanTtd = mahasiswa.getJurusan();
					Fakultas fakultasTtd = jurusanTtd == null ? null : jurusanTtd.getFakultas();

					if (jurusanTtd != null) {
						Dosen kaprodiTtd = jurusanTtd.getKaprodi();
						if (kaprodiTtd != null) {
							map.put("nip_kaprodi", kaprodiTtd.getCode());
							map.put("nama_kaprodi", kaprodiTtd.getNama());
						}
					}

					if (fakultasTtd != null) {
						map.put("id_fakultas", fakultasTtd.getId());

						Dosen dekanTtd = fakultasTtd.getDekan();
						if (dekanTtd != null) {
							map.put("nip_kajur", dekanTtd.getCode());
							map.put("nama_kajur", dekanTtd.getNama());
						}

						Dosen pudek1Ttd = fakultasTtd.getPudek1();
						if (pudek1Ttd != null) {
							map.put("nama_pudek1", pudek1Ttd.getNama());
							map.put("nip_pudek1", pudek1Ttd.getCode());
							map.put("nidn_pudek1", pudek1Ttd.getNidn());
						}

						Dosen pudek2Ttd = fakultasTtd.getPudek2();
						if (pudek2Ttd != null) {
							map.put("nama_pudek2", pudek2Ttd.getNama());
							map.put("nip_pudek2", pudek2Ttd.getCode());
							map.put("nidn_pudek2", pudek2Ttd.getNidn());
						}

						Dosen pudek3Ttd = fakultasTtd.getPudek3();
						if (pudek3Ttd != null) {
							map.put("nama_pudek3", pudek3Ttd.getNama());
							map.put("nip_pudek3", pudek3Ttd.getCode());
							map.put("nidn_pudek3", pudek3Ttd.getNidn());
						}
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit DetailperkuliahanForPenilaianHelper pejabat-ttd");
				}

				maps.add(map);
			}
		}

		parameters.put("terdaftar", terdaftar.size());

		String tahunAkademik = perkuliahan.getTahunAjaran();

		parameters.put("bar", "3-" + tahunAkademik + "-" + perkuliahan.getSemester() + "-" + perkuliahan.getId());

		String ttd = null;
		Dosen kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
				: perkuliahan.getJurusan().getKaprodi();
		if (kaprodi != null) {
			LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					ttd = lam.ambilFile().getAbsolutePath();

					parameters.put("ttd_kaprodi", ttd);
				}
			}
		}
		// System.out.println("ttd_kaprodi => " + ttd);

		if (perkuliahan != null) {
			int d = 1;
			for (Dosen dosena : perkuliahan.populateDosenBuNama()) {
				LampiranLain lam = LampiranLain.ambil(dosena.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						ttd = lam.ambilFile().getAbsolutePath();
						parameters.put("ttd_dosen_" + d, ttd);
						// System.out.println("ttd_dosen_" + d + " => " + ttd);
					}
				}
				d++;
			}

			if (kaprodi != null) {
				LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						ttd = lam.ambilFile().getAbsolutePath();

						parameters.put("ttd_dosen_" + d, ttd);
					}
				}
			}
		}

		try {
			// Pilih template: size 0 atau >9 → Daftar_Nilai_1 (catch-all), size 3 → Daftar_Nilai,
			// selain itu → Daftar_Nilai_<size>. Size 0 WAJIB dijabarkan eksplisit karena
			// Daftar_Nilai_0 tidak pernah ada sebagai berkas template.
			String namaTemplateDaftarNilai = (formatNilais.size() == 0 || formatNilais.size() > 9
					|| perkuliahan.getHanyaInputNilaiHuruf()) ? "Daftar_Nilai_1"
							: formatNilais.size() == 3 ? "Daftar_Nilai"
									: "Daftar_Nilai_" + formatNilais.size();
			Report.generatePDFReport(Report.PDF, parameters, namaTemplateDaftarNilai,
					ais.ui.util.WaktuUtil.getDate(), maps, Common.locale, component);
		} catch (Exception eLaporan) {
			// Berkas template .jasper laporan belum ter-deploy / gagal dibuka. Tampilkan pesan RAMAH ke
			// pengguna dan JANGAN biarkan menjadi UiException yang memenuhi log error sistem (masalah
			// deploy template, bukan bug aplikasi). Fungsi laporan tetap berjalan bila template tersedia.
			String pesanLaporan = eLaporan.getMessage();
			if (pesanLaporan == null || pesanLaporan.trim().isEmpty()) {
				pesanLaporan = "Laporan belum dapat dibuat. Silakan hubungi administrator "
						+ "untuk menyediakan berkas template laporan. "
						+ "Mohon sertakan tangkapan layar (screenshot) pesan ini saat menghubungi administrator.";
			}
			ais.ui.util.MyMessageboxConfig.show(pesanLaporan,
					"Informasi", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
		}
		terdaftar = null;
	}

	/**
	 * Membuka jendela modal <b>Analisis Nilai Huruf</b> untuk seorang mahasiswa, yang menjelaskan
	 * mengapa nilai hurufnya seperti yang tampak: komponen mana yang berkontribusi berapa, bobot mana
	 * yang ikut membentuk pembagi, apakah snapshot kunci sudah menyimpang, dan berapa poin lagi yang
	 * dibutuhkan untuk naik ke huruf berikutnya.
	 *
	 * <p>Fitur ini menjawab keluhan yang paling sering muncul dari dosen &mdash; &quot;kenapa nilai
	 * mahasiswa ini 0 padahal komponennya terisi&quot; atau &quot;kenapa hurufnya B padahal totalnya
	 * seharusnya A&quot; &mdash; dengan memaparkan langkah perhitungan yang dipakai sistem, bukan
	 * sekadar menampilkan hasilnya.</p>
	 *
	 * <p>Isi jendela dibangkitkan sebagai HTML utuh oleh
	 * {@link #buatHtmlAnalisisNilaiHuruf(Detailperkuliahan)} dan dipasang lewat sebuah {@link Html}.
	 * Ukurannya menyesuaikan perangkat: memenuhi layar pada ponsel, atau 680&times;620 piksel pada
	 * peramban meja. Bila jendela belum terpasang pada halaman mana pun, ia dilekatkan ke halaman
	 * pertama desktop yang sedang berjalan &mdash; penjagaan berlapis terhadap
	 * {@code Executions.getCurrent()} dan turunannya diperlukan karena metode ini dapat terpanggil
	 * dari konteks yang tidak memiliki eksekusi aktif.</p>
	 *
	 * <p>Masukan {@code null} menyebabkan metode berhenti tanpa membuka apa pun. Metode tidak mengubah
	 * data dan tidak membuka transaksi; ia hanya membaca. Panggilan {@code doModal()} bersifat
	 * memblokir alur peristiwa ZK sampai pengguna menutup jendela.</p>
	 *
	 * <p><b>Catatan.</b> Pada keadaan sekarang popup analisis per mahasiswa dipasang lewat
	 * {@code NilaiHurufAnalisisPopupHelper.pasangLink} di dalam perender, sehingga metode ini
	 * merupakan jalur alternatif yang tetap dipertahankan sebagai pintu masuk internal ke pembangun
	 * HTML yang sama.</p>
	 *
	 * @param detailperkuliahan baris nilai yang dianalisis; {@code null} diabaikan.
	 * @throws Exception bila pembangunan atau penampilan jendela gagal.
	 * @see #buatHtmlAnalisisNilaiHuruf(Detailperkuliahan)
	 * @see #tampilkanAnalisisKeseluruhanNilai()
	 */
	private void tampilkanAnalisisNilaiHuruf(Detailperkuliahan detailperkuliahan) throws Exception {
		if (detailperkuliahan == null) {
			return;
		}
		MyWindow window = new MyWindow("Analisis Nilai Huruf", "normal", true);
		window.setWidth(Common.isMobile() ? "100%" : "680px");
		window.setHeight(Common.isMobile() ? "90%" : "620px");
		window.setContentStyle("overflow:auto;background:#f8fafc;padding:0;");
		window.appendChild(new Html(buatHtmlAnalisisNilaiHuruf(detailperkuliahan)));
		if (window.getPage() == null && org.zkoss.zk.ui.Executions.getCurrent() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage() != null) {
			window.setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());
		}
		window.doModal();
	}

	/**
	 * Membuka jendela modal <b>Analisis Keseluruhan Nilai</b> untuk seluruh mahasiswa pada kelas yang
	 * sedang dibuka. Dipicu tombol &quot;Analisis Keseluruhan&quot; pada toolbar.
	 *
	 * <p>Bila {@link #tampilkanAnalisisNilaiHuruf(Detailperkuliahan)} menjelaskan satu mahasiswa,
	 * metode ini memeriksa satu <b>kelas</b>: berapa banyak yang belum diverifikasi, berapa yang huruf
	 * tersimpannya tidak lagi cocok dengan rentang konfigurasi, berapa snapshot kunci yang sudah
	 * menyimpang, berapa yang bernilai nol karena aturan penilaian, dan berapa yang berisiko karena
	 * kehadiran di bawah batas. Fungsinya adalah <b>alat audit mandiri</b> bagi dosen dan admin
	 * akademik sebelum nilai dikunci dan diumumkan.</p>
	 *
	 * <p>Seluruh isi dibangkitkan {@link #buatHtmlAnalisisKeseluruhanNilai()} sebagai HTML dan
	 * dipasang lewat {@link Html}. Jendelanya sedikit lebih besar daripada analisis per mahasiswa
	 * &mdash; 820&times;680 piksel pada peramban meja &mdash; karena memuat tabel distribusi huruf dan
	 * daftar data bermasalah. Penjagaan penempatan halaman dan pemanggilan {@code doModal()} identik
	 * dengan analisis per mahasiswa.</p>
	 *
	 * <p>Metode ini hanya membaca dan tidak mengubah data apa pun. Namun perlu diketahui bahwa
	 * pembangkit HTML-nya menelusuri seluruh baris kelas dan menyelesaikan setiap entitas satu per
	 * satu, sehingga pada kelas berisi ratusan mahasiswa pembukaannya terasa lambat; daftar data
	 * bermasalah karena itu dibatasi 12 catatan pertama agar jendela tetap ringan.</p>
	 *
	 * @throws Exception bila pembangunan atau penampilan jendela gagal.
	 * @see #buatHtmlAnalisisKeseluruhanNilai()
	 */
	private void tampilkanAnalisisKeseluruhanNilai() throws Exception {
		MyWindow window = new MyWindow("Analisis Keseluruhan Nilai", "normal", true);
		window.setWidth(Common.isMobile() ? "100%" : "820px");
		window.setHeight(Common.isMobile() ? "90%" : "680px");
		window.setContentStyle("overflow:auto;background:#f8fafc;padding:0;");
		window.appendChild(new Html(buatHtmlAnalisisKeseluruhanNilai()));
		if (window.getPage() == null && org.zkoss.zk.ui.Executions.getCurrent() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage() != null) {
			window.setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());
		}
		window.doModal();
	}

	/**
	 * Menelusuri seluruh mahasiswa pada kelas ini, mendeteksi anomali data nilai, lalu merangkai
	 * temuannya menjadi satu dokumen HTML lengkap untuk jendela Analisis Keseluruhan. Inilah mesin
	 * audit sesungguhnya di balik tombol &quot;Analisis Keseluruhan&quot;.
	 *
	 * <h3>Persiapan</h3>
	 * <p>Bila {@link #formatNilais} belum terisi, ia dimuat lebih dulu &mdash; metode ini dapat
	 * terpanggil sebelum grid sempat dibangun. Daftar id diambil dengan pengurutan yang sama seperti
	 * layar, dengan penjagaan terhadap {@link #urutkanBerdasarkanNama} yang mungkin masih {@code null}.
	 * Dua larik sejajar disiapkan untuk menghitung rata-rata per komponen: satu menampung jumlah
	 * nilai, satu menampung banyaknya data.</p>
	 *
	 * <h3>Lima anomali yang dideteksi</h3>
	 * <ol>
	 * <li><b>Belum diverifikasi.</b> Baris yang masih {@code NOT_VERIFIED} pada kelas yang
	 * menyembunyikan nilai belum terverifikasi; seluruh analisis untuk baris itu beralih memakai
	 * pasangan kolom <i>sementara</i>, dan fakta itu dinyatakan dalam catatannya.</li>
	 * <li><b>Huruf tidak sinkron.</b> Huruf tersimpan dibandingkan dengan hasil
	 * {@link #ambilAturanNilaiHuruf(Detailperkuliahan, double)} atas total yang sama. Bila aturan tidak
	 * ditemukan sama sekali, itu dihitung sebagai anomali tersendiri &mdash; menandakan konfigurasi
	 * rentang nilai huruf untuk prodi, fakultas, tahun akademik, atau jenis nilai belum lengkap.</li>
	 * <li><b>Snapshot kunci menyimpang.</b> {@code getNilaiHurufKunci()} yang terisi dibandingkan
	 * dengan huruf yang seharusnya menurut total terbaru. Inilah yang secara langsung <b>memperlihatkan
	 * akibat</b> dari pemetaan ulang pada {@link Detailperkuliahan#getNilaiHuruf()}: ketika tabel
	 * Format Nilai Huruf diubah setelah sebuah kelas dikunci, snapshot lama dan hasil pemetaan baru
	 * berbeda, dan kelas ini menghitung berapa banyak baris yang terdampak alih-alih
	 * menyembunyikannya.</li>
	 * <li><b>Nilai nol bermasalah.</b> Untuk total di bawah 0,01, {@code alasanNilaiJadiNol} dipanggil;
	 * bila ia mengembalikan penjelasan, baris ditandai bermasalah beserta alasannya.</li>
	 * <li><b>Kehadiran kurang.</b> Bila kelas menetapkan batas kehadiran di atas 0,1 dan
	 * {@code hitungPersenKehadiran()} berada di bawahnya.</li>
	 * </ol>
	 *
	 * <h3>Statistik yang dikumpulkan</h3>
	 * <p>Selain kelima pencacah anomali, metode mengumpulkan jumlah data, rata-rata total, nilai
	 * tertinggi dan terendah beserta identitas pemiliknya, distribusi jumlah per huruf dalam sebuah
	 * {@link TreeMap} sehingga terurut abjad, dan rata-rata per komponen yang diserahkan ke
	 * {@link #tambahAnalisisKomponen(StringBuilder, double[], int[])} untuk menemukan komponen dengan
	 * rata-rata terendah dan tertinggi. Komponen berbobot di bawah 0,01 dilewati di mana-mana. Nilai
	 * awal {@code nilaiTerendah} dipasang 999999 dan {@code nilaiTertinggi} dipasang &minus;1 sebagai
	 * penampung sentinel; bila kelas kosong, kedua angka itu akan tercetak apa adanya karena bagian
	 * pembanding hanya dilewati saat {@code jumlah} bernilai nol.</p>
	 *
	 * <h3>Ketahanan dan keamanan keluaran</h3>
	 * <p>Kegagalan menyelesaikan satu entitas atau menghitung satu anomali ditangkap dan diteruskan ke
	 * {@code Common.tampilErrorJikaAdmin}, lalu penelusuran berlanjut &mdash; satu baris rusak tidak
	 * boleh menggagalkan audit seluruh kelas. Setiap teks yang berasal dari data &mdash; nama
	 * mahasiswa, huruf, nama komponen, alasan &mdash; dilewatkan {@link #teksAmanHtml(String)}
	 * sebelum ditempelkan, sehingga nama yang mengandung karakter markah tidak dapat merusak atau
	 * menyisipkan struktur HTML. Daftar data bermasalah dibatasi 12 catatan pertama, dengan
	 * keterangan penutup bila masih ada sisanya, agar jendela tetap ringan pada kelas besar.</p>
	 *
	 * @return dokumen HTML utuh siap dipasang ke sebuah {@link Html}; tidak pernah {@code null},
	 *         dan tetap menghasilkan halaman yang bermakna ketika kelas belum berisi mahasiswa.
	 * @see #tampilkanAnalisisKeseluruhanNilai()
	 * @see #tambahKartuRingkas(StringBuilder, String, String)
	 * @see #tambahAnalisisKomponen(StringBuilder, double[], int[])
	 */
	private String buatHtmlAnalisisKeseluruhanNilai() {
		if (formatNilais == null) {
			formatNilais = Common.getFormatNilais(perkuliahan);
		}
		Collection<Long> ids = perkuliahan == null ? null
				: perkuliahan.ambilDetailperkuliahan(null, null, "", urutkanBerdasarkanNama != null
						&& urutkanBerdasarkanNama.isChecked(), false);
		int jumlah = 0;
		int belumVerifikasi = 0;
		int hurufTidakSinkron = 0;
		int hurufKunciTidakSinkron = 0;
		int nilaiNolBermasalah = 0;
		int hadirKurang = 0;
		double totalNilai = 0.0;
		double nilaiTertinggi = -1.0;
		double nilaiTerendah = 999999.0;
		String mahasiswaTertinggi = "";
		String mahasiswaTerendah = "";
		Map<String, Integer> distribusiHuruf = new TreeMap<String, Integer>();
		StringBuilder masalah = new StringBuilder();
		int jumlahMasalahDitampilkan = 0;
		double[] jumlahNilaiKomponen = new double[formatNilais == null ? 0 : formatNilais.size()];
		int[] jumlahDataKomponen = new int[formatNilais == null ? 0 : formatNilais.size()];

		if (ids != null) {
			for (Long id : ids) {
				if (id == null) {
					continue;
				}
				Detailperkuliahan detail = null;
				try {
					detail = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, id.toString());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				if (detail == null) {
					continue;
				}
				jumlah++;
				boolean tampilSementara = perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
						&& Detailperkuliahan.NOT_VERIFIED.equals(detail.getVerify());
				double total = tampilSementara ? nilaiAman(detail.getTotalNilaiSementara())
						: nilaiAman(detail.getTotalNilai());
				String huruf = tampilSementara ? detail.getNilaiHurufSementara() : detail.getNilaiHuruf();
				NilaiHuruf aturan = ambilAturanNilaiHuruf(detail, total);
				String hurufSeharusnya = aturan == null ? "" : aturan.getNilaiHuruf();
				Mahasiswa mahasiswa = detail.getMahasiswa();
				String identitas = mahasiswa == null ? "Mahasiswa"
						: ((mahasiswa.getNim() == null ? "" : mahasiswa.getNim()) + " - "
								+ (mahasiswa.getNama() == null ? "" : mahasiswa.getNama()));

				totalNilai += total;
				if (total > nilaiTertinggi) {
					nilaiTertinggi = total;
					mahasiswaTertinggi = identitas;
				}
				if (total < nilaiTerendah) {
					nilaiTerendah = total;
					mahasiswaTerendah = identitas;
				}

				String hurufDistribusi = huruf == null || huruf.trim().isEmpty() ? "-" : huruf.trim();
				Integer jumlahHuruf = distribusiHuruf.get(hurufDistribusi);
				distribusiHuruf.put(hurufDistribusi, Integer.valueOf(jumlahHuruf == null ? 1 : jumlahHuruf.intValue() + 1));

				boolean bermasalah = false;
				StringBuilder alasan = new StringBuilder();
				if (tampilSementara) {
					belumVerifikasi++;
					bermasalah = true;
					alasan.append("nilai belum diverifikasi; ");
				}
				if (aturan == null) {
					hurufTidakSinkron++;
					bermasalah = true;
					alasan.append("rentang nilai huruf tidak ditemukan; ");
				} else if (huruf == null || !huruf.trim().equalsIgnoreCase(hurufSeharusnya)) {
					hurufTidakSinkron++;
					bermasalah = true;
					alasan.append("huruf tampil ").append(huruf == null ? "-" : huruf).append(", seharusnya ")
							.append(hurufSeharusnya).append("; ");
				}
				if (detail.getNilaiHurufKunci() != null && !detail.getNilaiHurufKunci().trim().isEmpty()
						&& aturan != null && !detail.getNilaiHurufKunci().trim().equalsIgnoreCase(hurufSeharusnya)) {
					hurufKunciTidakSinkron++;
					bermasalah = true;
					alasan.append("snapshot huruf kunci ").append(detail.getNilaiHurufKunci()).append(" tidak sama; ");
				}
				try {
					if (total < 0.01) {
						String alasanNol = detail.alasanNilaiJadiNol(true, formatNilais);
						if (alasanNol != null && !alasanNol.trim().isEmpty()) {
							nilaiNolBermasalah++;
							bermasalah = true;
							alasan.append(alasanNol).append("; ");
						}
					}
					if (perkuliahan != null && perkuliahan.getPersenKehadiranDinilai0() > 0.1
							&& detail.hitungPersenKehadiran() < perkuliahan.getPersenKehadiranDinilai0()) {
						hadirKurang++;
						bermasalah = true;
						alasan.append("kehadiran di bawah batas minimal; ");
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				if (formatNilais != null) {
					for (int i = 0; i < formatNilais.size(); i++) {
						FormatNilai formatNilai = formatNilais.get(i);
						if (formatNilai == null || formatNilai.getPersen() == null
								|| formatNilai.getPersen().doubleValue() < 0.01) {
							continue;
						}
						double nilaiKomponen = tampilSementara ? nilaiAman(detail.retreiveDetailNilaiBelumVerify(formatNilai))
								: nilaiAman(detail.retreiveDetailNilai(formatNilai));
						jumlahNilaiKomponen[i] += nilaiKomponen;
						jumlahDataKomponen[i]++;
					}
				}

				if (bermasalah && jumlahMasalahDitampilkan < 12) {
					masalah.append("<tr><td style='padding:6px;border:1px solid #dbe5f0;'>")
							.append(teksAmanHtml(identitas)).append("</td><td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>")
							.append(Common.numberFormat.get().format(total)).append("</td><td style='padding:6px;border:1px solid #dbe5f0;'>")
							.append(teksAmanHtml(huruf == null ? "-" : huruf)).append("</td><td style='padding:6px;border:1px solid #dbe5f0;'>")
							.append(teksAmanHtml(alasan.toString())).append("</td></tr>");
					jumlahMasalahDitampilkan++;
				}
			}
		}

		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;color:#172033;font-size:13px;line-height:1.45;'>");
		html.append("<div style='background:#0b63ce;color:white;padding:14px 18px;'>");
		html.append("<div style='font-size:18px;font-weight:bold;'>Analisis Keseluruhan</div>");
		html.append("<div style='font-size:12px;opacity:.92;'>Ringkasan pintar seluruh mahasiswa pada perkuliahan ini, termasuk huruf, bobot, verifikasi, dan risiko data.</div>");
		html.append("</div><div style='padding:16px 18px;'>");
		html.append("<div style='display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px;'>");
		tambahKartuRingkas(html, "Mahasiswa", jumlah + " data");
		tambahKartuRingkas(html, "Rata-rata", jumlah == 0 ? "-" : Common.numberFormat.get().format(totalNilai / jumlah));
		tambahKartuRingkas(html, "Belum Verifikasi", belumVerifikasi + " data");
		tambahKartuRingkas(html, "Huruf Tidak Sinkron", hurufTidakSinkron + " data");
		tambahKartuRingkas(html, "Snapshot Kunci Beda", hurufKunciTidakSinkron + " data");
		html.append("</div>");

		html.append("<div style='background:#f0f7ff;border:1px solid #bfdbfe;border-radius:8px;padding:12px;margin-bottom:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;color:#0b3b78;'>Analisis Pintar</div><ol style='margin:0;padding-left:20px;'>");
		if (jumlah == 0) {
			html.append("<li>Belum ada mahasiswa yang bisa dianalisis pada perkuliahan ini.</li>");
		} else {
			if (hurufTidakSinkron > 0 || hurufKunciTidakSinkron > 0) {
				html.append("<li><b>Ada nilai huruf yang perlu diperiksa.</b> Sistem menemukan ")
						.append(hurufTidakSinkron).append(" data huruf tampil tidak sesuai rentang dan ")
						.append(hurufKunciTidakSinkron)
						.append(" snapshot huruf kunci yang berbeda dari total terbaru. Jalankan Hitung Ulang/Singkronkan agar data tersimpan ikut rentang yang benar.</li>");
			} else {
				html.append("<li><b>Nilai huruf konsisten.</b> Huruf yang tampil sudah sesuai total dan rentang konfigurasi nilai huruf.</li>");
			}
			if (belumVerifikasi > 0) {
				html.append("<li><b>").append(belumVerifikasi)
						.append(" mahasiswa belum diverifikasi.</b> Jika setting nilai belum verifikasi disembunyikan aktif, analisis memakai nilai sementara.</li>");
			}
			if (nilaiNolBermasalah > 0) {
				html.append("<li><b>").append(nilaiNolBermasalah)
						.append(" mahasiswa bernilai 0 karena aturan penilaian.</b> Periksa komponen kosong/0 dan aturan nilai 0 pada kelas ini.</li>");
			}
			if (hadirKurang > 0) {
				html.append("<li><b>").append(hadirKurang)
						.append(" mahasiswa berisiko karena kehadiran di bawah batas minimal.</b> Nilai akhir bisa menjadi 0 sesuai konfigurasi perkuliahan.</li>");
			}
			html.append("<li>Nilai tertinggi: <b>").append(Common.numberFormat.get().format(nilaiTertinggi))
					.append("</b> oleh ").append(teksAmanHtml(mahasiswaTertinggi)).append("; nilai terendah: <b>")
					.append(Common.numberFormat.get().format(nilaiTerendah)).append("</b> oleh ")
					.append(teksAmanHtml(mahasiswaTerendah)).append(".</li>");
			tambahAnalisisKomponen(html, jumlahNilaiKomponen, jumlahDataKomponen);
		}
		html.append("</ol></div>");

		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;margin-bottom:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;'>Distribusi Nilai Huruf</div>");
		if (distribusiHuruf.isEmpty()) {
			html.append("<div style='color:#64748b;'>Belum ada distribusi nilai huruf.</div>");
		} else {
			html.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
			html.append("<tr style='background:#eef4fb;'><th style='text-align:left;padding:6px;border:1px solid #dbe5f0;'>Huruf</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Jumlah</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Persen</th></tr>");
			for (Map.Entry<String, Integer> entry : distribusiHuruf.entrySet()) {
				double persen = jumlah == 0 ? 0.0 : (entry.getValue().doubleValue() * 100.0 / jumlah);
				html.append("<tr><td style='padding:6px;border:1px solid #dbe5f0;'>")
						.append(teksAmanHtml(entry.getKey())).append("</td><td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>")
						.append(entry.getValue()).append("</td><td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>")
						.append(Common.numberFormat.get().format(persen)).append("%</td></tr>");
			}
			html.append("</table>");
		}
		html.append("</div>");

		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;'>Data Yang Perlu Perhatian</div>");
		if (masalah.length() == 0) {
			html.append("<div style='color:#166534;font-weight:bold;'>Tidak ditemukan anomali utama pada data yang dianalisis.</div>");
		} else {
			html.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
			html.append("<tr style='background:#eef4fb;'><th style='text-align:left;padding:6px;border:1px solid #dbe5f0;'>Mahasiswa</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Total</th><th style='text-align:left;padding:6px;border:1px solid #dbe5f0;'>Huruf</th><th style='text-align:left;padding:6px;border:1px solid #dbe5f0;'>Catatan</th></tr>");
			html.append(masalah);
			html.append("</table>");
			if (hurufTidakSinkron + hurufKunciTidakSinkron + belumVerifikasi + nilaiNolBermasalah + hadirKurang > jumlahMasalahDitampilkan) {
				html.append("<div style='margin-top:6px;color:#64748b;'>Sebagian data diringkas; tampilkan 12 catatan pertama agar popup tetap ringan.</div>");
			}
		}
		html.append("</div>");
		html.append("</div></div>");
		return html.toString();
	}

	/**
	 * Menambahkan satu <b>kartu ringkas</b> ke dokumen HTML yang sedang dirangkai: sebuah kotak putih
	 * bergaris tepi dengan judul kecil berwarna kelabu di atas dan angka besar bercetak tebal di
	 * bawahnya. Lima kartu semacam ini berjajar di kepala jendela Analisis Keseluruhan &mdash;
	 * Mahasiswa, Rata-rata, Belum Verifikasi, Huruf Tidak Sinkron, dan Snapshot Kunci Beda &mdash;
	 * memberi gambaran keadaan kelas dalam sekali pandang.
	 *
	 * <p>Gaya ditulis sebaris ({@code style="..."}) alih-alih memakai kelas CSS karena keluaran ini
	 * dipasang ke dalam komponen {@link Html} milik ZK, yang tidak menjamin lembar gaya aplikasi ikut
	 * berlaku di dalamnya. Lebar minimum 130 piksel menjaga kartu tetap terbaca, sementara pembungkus
	 * di pemanggil memakai {@code flex-wrap} sehingga kartu turun ke baris berikutnya pada layar
	 * sempit alih-alih terpotong.</p>
	 *
	 * <p>Kedua teks dilewatkan {@link #teksAmanHtml(String)} lebih dulu. Untuk pemakaian saat ini
	 * judul dan angka memang selalu berupa literal atau angka terformat, tetapi pelarian tetap
	 * diterapkan agar metode ini aman dipakai ulang dengan nilai yang berasal dari data.</p>
	 *
	 * <p>Metode menulis <b>langsung ke {@code html}</b> dan tidak mengembalikan apa pun; pemanggil
	 * bertanggung jawab atas pembuka dan penutup wadah berjajar.</p>
	 *
	 * @param html  perangkai dokumen yang sedang dibangun; diubah di tempat.
	 * @param judul label kecil di atas kartu, misalnya <code>&quot;Belum Verifikasi&quot;</code>.
	 * @param nilai teks besar di bawah judul, biasanya angka beserta satuannya seperti
	 *              <code>&quot;12 data&quot;</code>.
	 * @see #buatHtmlAnalisisKeseluruhanNilai()
	 */
	private void tambahKartuRingkas(StringBuilder html, String judul, String nilai) {
		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:10px 12px;min-width:130px;'>")
				.append("<div style='font-size:11px;color:#64748b;'>").append(teksAmanHtml(judul)).append("</div>")
				.append("<div style='font-size:17px;font-weight:bold;color:#0f172a;'>").append(teksAmanHtml(nilai))
				.append("</div></div>");
	}

	/**
	 * Menghitung rata-rata setiap komponen penilaian di seluruh kelas, lalu menambahkan satu butir
	 * daftar yang menyebut <b>komponen dengan rata-rata terendah dan tertinggi</b>. Butir ini menutup
	 * bagian Analisis Pintar pada jendela Analisis Keseluruhan.
	 *
	 * <p>Temuannya bernilai secara pedagogis: komponen dengan rata-rata terendah menunjukkan bagian
	 * mata kuliah yang paling sulit dikuasai mahasiswa &mdash; petunjuk bagi dosen untuk meninjau
	 * kembali metode pengajaran atau tingkat kesukaran soal pada komponen tersebut.
	 *
	 * <h3>Larik sejajar</h3>
	 * <p>Kedua parameter adalah larik <b>sejajar</b> yang diisi pemanggil saat menelusuri mahasiswa:
	 * indeks ke-<i>i</i> pada keduanya merujuk komponen ke-<i>i</i> pada {@link #formatNilais}.
	 * {@code jumlahNilaiKomponen} menampung penjumlahan nilai, {@code jumlahDataKomponen} menampung
	 * banyaknya baris yang ikut dijumlahkan, dan rata-rata diperoleh dengan membagi keduanya.
	 * Pemisahan pencacah dari penjumlah diperlukan karena tidak setiap mahasiswa berkontribusi pada
	 * setiap komponen.</p>
	 *
	 * <h3>Penyaringan berlapis</h3>
	 * <p>Sebuah indeks dilewati bila berada di luar batas larik pencacah, bila pencacahnya nol atau
	 * kurang &mdash; penjagaan pembagian dengan nol sekaligus penanda komponen tanpa data &mdash;
	 * atau bila komponennya {@code null}, berbobot {@code null}, atau berbobot di bawah 0,01.
	 * Ambang 0,01 dipakai konsisten di seluruh kelas ini untuk menyatakan &quot;praktis nol&quot; pada
	 * bilangan pecahan, menghindari perbandingan kesamaan langsung yang tidak dapat diandalkan.</p>
	 *
	 * <p>Metode mengembalikan diri lebih awal bila {@link #formatNilais} masih {@code null}. Nilai awal
	 * sentinel &mdash; {@code rataTerendah} 999999 dan {@code rataTertinggi} &minus;1 &mdash; tidak
	 * pernah sampai tercetak karena butir hanya ditulis bila nama komponen terendah sudah terisi,
	 * yang hanya terjadi setelah sedikitnya satu komponen lolos penyaringan. Nama komponen dilarikan
	 * lewat {@link #teksAmanHtml(String)} sebelum ditempelkan.</p>
	 *
	 * <p>Keluaran ditulis sebagai satu elemen {@code <li>} dan karena itu <b>harus dipanggil dari
	 * dalam daftar berurut yang sudah terbuka</b>; pemanggil satu-satunya menempatkannya sebagai butir
	 * terakhir sebelum daftar ditutup.</p>
	 *
	 * @param html                perangkai dokumen yang sedang dibangun; diubah di tempat.
	 * @param jumlahNilaiKomponen penjumlahan nilai per komponen, sejajar dengan {@link #formatNilais}.
	 * @param jumlahDataKomponen  banyaknya baris yang dijumlahkan per komponen, sejajar dengan larik
	 *                            di atas.
	 * @see #buatHtmlAnalisisKeseluruhanNilai()
	 */
	private void tambahAnalisisKomponen(StringBuilder html, double[] jumlahNilaiKomponen, int[] jumlahDataKomponen) {
		String komponenTerendah = "";
		String komponenTertinggi = "";
		double rataTerendah = 999999.0;
		double rataTertinggi = -1.0;
		if (formatNilais == null) {
			return;
		}
		for (int i = 0; i < formatNilais.size(); i++) {
			if (i >= jumlahDataKomponen.length || jumlahDataKomponen[i] <= 0) {
				continue;
			}
			FormatNilai formatNilai = formatNilais.get(i);
			if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
				continue;
			}
			double rata = jumlahNilaiKomponen[i] / jumlahDataKomponen[i];
			if (rata < rataTerendah) {
				rataTerendah = rata;
				komponenTerendah = formatNilai.getNama();
			}
			if (rata > rataTertinggi) {
				rataTertinggi = rata;
				komponenTertinggi = formatNilai.getNama();
			}
		}
		if (komponenTerendah != null && komponenTerendah.trim().length() > 0) {
			html.append("<li>Komponen rata-rata terendah adalah <b>").append(teksAmanHtml(komponenTerendah))
					.append("</b> (").append(Common.numberFormat.get().format(rataTerendah))
					.append("), sedangkan tertinggi <b>").append(teksAmanHtml(komponenTertinggi)).append("</b> (")
					.append(Common.numberFormat.get().format(rataTertinggi)).append(").</li>");
		}
	}

	/**
	 * Merangkai dokumen HTML lengkap untuk jendela <b>Analisis Nilai Huruf</b> seorang mahasiswa:
	 * kartu identitas dan nilai, bagian Analisis Pintar, tabel komponen pembentuk nilai, dan
	 * kesimpulan beserta jarak menuju huruf berikutnya.
	 *
	 * <h3>Memilih nilai final atau nilai sementara</h3>
	 * <p>Keputusan pertama menentukan sisa dokumen. Bila kelas menyembunyikan nilai yang belum
	 * diverifikasi <b>dan</b> baris ini masih {@link Detailperkuliahan#NOT_VERIFIED}, seluruh analisis
	 * memakai pasangan kolom <i>sementara</i> ({@code totalNilaiSementara},
	 * {@code nilaiHurufSementara}) dan pembacaan komponen beralih ke
	 * {@code retreiveDetailNilaiBelumVerify}. Bendera {@code tampilSementara} itu diteruskan ke
	 * seluruh metode pembantu agar tidak ada bagian dokumen yang mencampur kedua sumber, dan
	 * pembaca diberi tahu secara eksplisit lewat catatan berwarna oranye.</p>
	 *
	 * <h3>Empat bagian dokumen</h3>
	 * <ol>
	 * <li><b>Kartu identitas.</b> NIM dan nama mahasiswa, nilai akhir, nilai huruf, serta rentang
	 * mulai&ndash;sampai dan bobot IP dari aturan huruf yang cocok. Bila kelas terkunci dan snapshot
	 * {@code nilaiHurufKunci} berbeda dari huruf yang tampil, perbedaan itu <b>dinyatakan terbuka</b>:
	 * &quot;Snapshot huruf saat dikunci: X; tampilan dikoreksi mengikuti total menjadi Y&quot;. Inilah
	 * pengakuan langsung atas pemetaan ulang pada {@link Detailperkuliahan#getNilaiHuruf()} &mdash;
	 * kelas ini memilih menjelaskannya kepada pengguna alih-alih menyembunyikannya.</li>
	 * <li><b>Analisis Pintar</b> dari
	 * {@link #buatHtmlAnalisisPintar(Detailperkuliahan, double, String, String, NilaiHuruf, NilaiHuruf, boolean)}.</li>
	 * <li><b>Tabel komponen</b> dari {@link #buatHtmlKomponenNilai(Detailperkuliahan, boolean)}.</li>
	 * <li><b>Kesimpulan.</b> Bila total di bawah 0,01, {@code alasanNilaiJadiNol} ditampilkan dengan
	 * warna merah sebagai sebab utama. Jika tidak, dinyatakan bahwa total masuk rentang huruf tertentu
	 * &mdash; atau, bila aturan tidak ditemukan, pengguna diarahkan memeriksa konfigurasi rentang
	 * nilai huruf untuk prodi, fakultas, dan tahun akademik yang bersangkutan. Ditutup dengan jarak
	 * menuju huruf berikutnya bila {@link #ambilAturanNilaiHurufBerikut(Detailperkuliahan, double)}
	 * menemukan target dan selisihnya masih positif.</li>
	 * </ol>
	 *
	 * <p><b>Ketahanan dan keamanan.</b> Pemanggilan {@code alasanNilaiJadiNol} dibungkus penangkap
	 * galat sehingga kegagalannya menyisakan kesimpulan kosong, bukan menggagalkan jendela. Setiap
	 * teks yang berasal dari data dilewatkan {@link #teksAmanHtml(String)}, sedangkan angka diformat
	 * lewat {@code Common.numberFormat} yang bersifat per-utas. Metode ini murni membaca dan tidak
	 * mengubah entitas mana pun.</p>
	 *
	 * <p><b>Prasyarat.</b> {@code detailperkuliahan} diasumsikan tidak {@code null} &mdash; pemanggil
	 * sudah menjaganya &mdash; sedangkan {@link #perkuliahan} dan {@link #formatNilais} boleh
	 * {@code null} dan sudah dijaga di dalam.</p>
	 *
	 * @param detailperkuliahan baris nilai yang dianalisis.
	 * @return dokumen HTML utuh siap dipasang ke sebuah {@link Html}.
	 * @see #tampilkanAnalisisNilaiHuruf(Detailperkuliahan)
	 */
	private String buatHtmlAnalisisNilaiHuruf(Detailperkuliahan detailperkuliahan) {
		StringBuilder html = new StringBuilder();
		boolean tampilSementara = perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
				&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED);
		double total = tampilSementara ? nilaiAman(detailperkuliahan.getTotalNilaiSementara())
				: nilaiAman(detailperkuliahan.getTotalNilai());
		String hurufKunci = detailperkuliahan.getNilaiHurufKunci();
		String huruf = tampilSementara ? detailperkuliahan.getNilaiHurufSementara() : detailperkuliahan.getNilaiHuruf();
		NilaiHuruf aturanHuruf = ambilAturanNilaiHuruf(detailperkuliahan, total);
		NilaiHuruf targetBerikut = ambilAturanNilaiHurufBerikut(detailperkuliahan, total);

		html.append("<div style='font-family:Arial,sans-serif;color:#172033;font-size:13px;line-height:1.45;'>");
		html.append("<div style='background:#0b63ce;color:white;padding:14px 18px;'>");
		html.append("<div style='font-size:18px;font-weight:bold;'>Analisis Nilai Huruf</div>");
		html.append("<div style='font-size:12px;opacity:.92;'>Rincian ini membaca komponen nilai, bobot, verifikasi, kehadiran, dan tabel Nilai Huruf yang sama dengan perhitungan sistem.</div>");
		html.append("</div>");
		html.append("<div style='padding:16px 18px;'>");

		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;margin-bottom:12px;'>");
		html.append("<div style='font-weight:bold;font-size:15px;margin-bottom:6px;'>")
				.append(teksAmanHtml(mahasiswa == null ? "Mahasiswa" : mahasiswa.getNim() + " - " + mahasiswa.getNama()))
				.append("</div>");
		html.append("<div>Nilai akhir: <b>").append(Common.numberFormat.get().format(total)).append("</b></div>");
		html.append("<div>Nilai huruf: <b>").append(teksAmanHtml(huruf == null || huruf.trim().isEmpty() ? "-" : huruf))
				.append("</b></div>");
		if (apakahPerkuliahanTerkunci(detailperkuliahan) && hurufKunci != null && !hurufKunci.trim().isEmpty()
				&& huruf != null && !hurufKunci.trim().equalsIgnoreCase(huruf.trim())) {
			html.append("<div style='color:#a16207;'>Snapshot huruf saat dikunci: <b>")
					.append(teksAmanHtml(hurufKunci)).append("</b>; tampilan dikoreksi mengikuti total menjadi <b>")
					.append(teksAmanHtml(huruf)).append("</b>.</div>");
		}
		if (aturanHuruf != null) {
			html.append("<div>Rentang huruf ini: <b>")
					.append(Common.numberFormat.get().format(aturanHuruf.getMulai())).append(" s.d ")
					.append(Common.numberFormat.get().format(aturanHuruf.getSampai())).append("</b>");
			if (aturanHuruf.getNilaiDiIPK() != null) {
				html.append(", IP: <b>").append(Common.numberFormat.get().format(aturanHuruf.getNilaiDiIPK()))
						.append("</b>");
			}
			html.append("</div>");
		}
		if (tampilSementara) {
			html.append("<div style='margin-top:6px;color:#a16207;'>Nilai yang dianalisis adalah nilai sementara karena nilai belum diverifikasi dan setting sembunyikan nilai belum verifikasi sedang aktif.</div>");
		}
		html.append("</div>");

		html.append(buatHtmlAnalisisPintar(detailperkuliahan, total, huruf, hurufKunci, aturanHuruf, targetBerikut,
				tampilSementara));
		html.append(buatHtmlKomponenNilai(detailperkuliahan, tampilSementara));

		String alasanNol = "";
		try {
			if (total < 0.01) {
				alasanNol = detailperkuliahan.alasanNilaiJadiNol(true, formatNilais);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;margin-top:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:6px;'>Kesimpulan</div>");
		if (alasanNol != null && !alasanNol.trim().isEmpty()) {
			html.append("<div style='color:#b91c1c;font-weight:bold;'>").append(teksAmanHtml(alasanNol)).append("</div>");
		} else if (aturanHuruf != null) {
			html.append("<div>Total <b>").append(Common.numberFormat.get().format(total)).append("</b> masuk rentang <b>")
					.append(teksAmanHtml(aturanHuruf.getNilaiHuruf())).append("</b>, sehingga sistem menampilkan nilai huruf tersebut.</div>");
		} else {
			html.append("<div>Nilai huruf belum ditemukan dari tabel konfigurasi Nilai Huruf. Periksa setting rentang nilai huruf untuk prodi/fakultas/tahun akademik ini.</div>");
		}
		if (targetBerikut != null && targetBerikut.getMulai() != null && targetBerikut.getNilaiHuruf() != null) {
			double kurang = targetBerikut.getMulai().doubleValue() - total;
			if (kurang > 0.0) {
				html.append("<div style='margin-top:6px;'>Untuk mencapai <b>")
						.append(teksAmanHtml(targetBerikut.getNilaiHuruf())).append("</b>, kurang sekitar <b>")
						.append(Common.numberFormat.get().format(kurang)).append("</b> poin dari batas bawah ")
						.append(Common.numberFormat.get().format(targetBerikut.getMulai())).append(".</div>");
			}
		}
		html.append("</div>");

		html.append("</div></div>");
		return html.toString();
	}

	/**
	 * Merangkai bagian <b>Analisis Pintar</b> untuk seorang mahasiswa: sebuah daftar berurut yang
	 * menjelaskan keadaan nilainya dalam bahasa yang dapat ditindaklanjuti dosen, bukan sekadar
	 * menampilkan angka.
	 *
	 * <h3>Butir pertama: satu dari empat kemungkinan</h3>
	 * <p>Butir pembuka bersifat saling meniadakan dan urutannya menentukan prioritas diagnosis:</p>
	 * <ol>
	 * <li><b>Aturan huruf tidak ditemukan</b> &mdash; {@code aturanHuruf} bernilai {@code null}.
	 * Diagnosis paling mendasar: konfigurasi Nilai Huruf untuk prodi, fakultas, tahun akademik, atau
	 * jenis nilai belum lengkap sehingga total berapa pun tidak dapat dipetakan.</li>
	 * <li><b>Terkunci tetapi snapshot sudah basi</b> &mdash; kelas terkunci, {@code hurufKunci} terisi,
	 * dan berbeda dari huruf yang seharusnya menurut total sekarang. Butir ini <b>menyatakan secara
	 * terbuka</b> bahwa sistem &quot;mengutamakan huruf sesuai total/rentang, bukan huruf kunci yang
	 * basi&quot;. Inilah dokumentasi paling langsung atas perilaku pemetaan ulang pada
	 * {@link Detailperkuliahan#getNilaiHuruf()}: penguncian membekukan angka, tetapi pemetaan angka ke
	 * huruf tetap mengikuti tabel Format Nilai Huruf yang berlaku saat ini.</li>
	 * <li><b>Huruf tersimpan tidak sinkron</b> &mdash; huruf yang tampil berbeda dari hasil pemetaan
	 * total, pada kelas yang tidak terkunci. Pengguna diarahkan menekan Hitung Ulang atau
	 * Singkronkan.</li>
	 * <li><b>Huruf sudah konsisten</b> &mdash; keadaan sehat, disertai rentang yang berlaku.</li>
	 * </ol>
	 *
	 * <h3>Butir bersyarat berikutnya</h3>
	 * <p>Ditambahkan hanya bila relevan: pemberitahuan bahwa analisis memakai nilai sementara;
	 * perbandingan persen kehadiran terhadap batas minimal kelas; aturan nilai 0 yang sedang aktif
	 * &mdash; dengan aturan &quot;jika ada nilai 0 tidak menghitung nilai akhir&quot; diperiksa lebih
	 * dulu karena dampaknya lebih keras daripada aturan &quot;nilai 0 tidak masuk pembagi&quot;; serta
	 * peringatan bila total bobot efektif berada di luar rentang 99,9&ndash;100,1 persen, yang
	 * menandakan bobot Format Nilai perlu ditinjau.</p>
	 *
	 * <h3>Butir penutup: jalur tercepat menaikkan huruf</h3>
	 * <p>Bila ada huruf berikutnya yang dapat dicapai, metode menghitung berapa poin tambahan yang
	 * dibutuhkan pada <b>komponen berbobot terbesar</b>. Rumusnya membalik sumbangan tertimbang:
	 * selisih total yang dibutuhkan dibagi dengan pangsa bobot komponen itu terhadap total bobot
	 * efektif, yakni {@code kurangTotal / (bobot / totalBobot)}. Pembagian dijaga oleh syarat
	 * {@code totalBobot > 0.0} di muka dan {@code bobot <= 0.0} di dalam. Hasilnya disebut sendiri
	 * sebagai perkiraan &quot;secara kasar&quot; dan disertai pengingat bahwa nilai maksimal komponen
	 * mungkin tidak memungkinkan kenaikan sebesar itu &mdash; kejujuran yang penting, sebab perhitungan
	 * ini mengabaikan batas atas 100 per komponen.</p>
	 *
	 * <p>Penghitungan persen kehadiran dibungkus penangkap galat sehingga kegagalannya menyisakan nilai
	 * nol, bukan menggagalkan seluruh jendela. Setiap teks dari data dilarikan lewat
	 * {@link #teksAmanHtml(String)}.</p>
	 *
	 * @param detailperkuliahan baris nilai yang dianalisis.
	 * @param total             nilai akhir yang dipakai analisis; sudah dipilih antara final atau
	 *                          sementara oleh pemanggil.
	 * @param hurufTampil       huruf yang sedang ditampilkan sistem.
	 * @param hurufKunci        snapshot huruf saat penguncian; boleh {@code null} atau kosong.
	 * @param aturanHuruf       aturan {@link NilaiHuruf} yang cocok dengan {@code total}; boleh
	 *                          {@code null} bila tidak ditemukan.
	 * @param targetBerikut     aturan huruf satu tingkat di atas; boleh {@code null}.
	 * @param tampilSementara   {@code true} bila seluruh analisis memakai kolom nilai sementara.
	 * @return potongan HTML berisi satu blok Analisis Pintar, siap disisipkan ke dokumen.
	 * @see #buatHtmlAnalisisNilaiHuruf(Detailperkuliahan)
	 * @see #hitungTotalBobotEfektif(Detailperkuliahan, boolean)
	 * @see #ambilFormatNilaiBobotTerbesar(Detailperkuliahan, boolean)
	 */
	private String buatHtmlAnalisisPintar(Detailperkuliahan detailperkuliahan, double total, String hurufTampil,
			String hurufKunci, NilaiHuruf aturanHuruf, NilaiHuruf targetBerikut, boolean tampilSementara) {
		StringBuilder html = new StringBuilder();
		String hurufSeharusnya = aturanHuruf == null ? "" : aturanHuruf.getNilaiHuruf();
		double persenHadir = 0.0;
		try {
			persenHadir = detailperkuliahan.hitungPersenKehadiran();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		double totalBobot = hitungTotalBobotEfektif(detailperkuliahan, tampilSementara);
		FormatNilai bobotTerbesar = ambilFormatNilaiBobotTerbesar(detailperkuliahan, tampilSementara);

		html.append("<div style='background:#f0f7ff;border:1px solid #bfdbfe;border-radius:8px;padding:12px;margin-bottom:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;color:#0b3b78;'>Analisis Pintar</div>");
		html.append("<ol style='margin:0;padding-left:20px;'>");

		boolean terkunci = apakahPerkuliahanTerkunci(detailperkuliahan);
		if (aturanHuruf == null) {
			html.append("<li><b>Rentang nilai huruf belum cocok.</b> Sistem tidak menemukan konfigurasi Nilai Huruf untuk total ")
					.append(Common.numberFormat.get().format(total))
					.append(". Ini biasanya karena setting Nilai Huruf prodi/fakultas/tahun akademik/jenis nilai belum lengkap.</li>");
		} else if (terkunci && hurufKunci != null && !hurufKunci.trim().isEmpty()
				&& !hurufKunci.trim().equalsIgnoreCase(hurufSeharusnya)) {
			html.append("<li><b>Nilai terkunci, tetapi snapshot huruf kunci sudah tidak sesuai.</b> Saat dikunci tersimpan <b>")
					.append(teksAmanHtml(hurufKunci)).append("</b>, sementara total ")
					.append(Common.numberFormat.get().format(total)).append(" sekarang masuk rentang <b>")
					.append(teksAmanHtml(hurufSeharusnya))
					.append("</b>. Karena itu sistem mengutamakan huruf sesuai total/rentang, bukan huruf kunci yang basi.</li>");
		} else if (hurufTampil == null || !hurufTampil.trim().equalsIgnoreCase(hurufSeharusnya)) {
			html.append("<li><b>Ada indikasi huruf tersimpan tidak sinkron.</b> Berdasarkan total ")
					.append(Common.numberFormat.get().format(total)).append(", sistem membaca rentang <b>")
					.append(teksAmanHtml(hurufSeharusnya)).append("</b>, tetapi yang tampil <b>")
					.append(teksAmanHtml(hurufTampil)).append("</b>. Klik Hitung Ulang/Singkronkan Nilai agar nilai huruf tersimpan mengikuti rentang terbaru.</li>");
		} else {
			html.append("<li><b>Huruf sudah konsisten.</b> Total ")
					.append(Common.numberFormat.get().format(total)).append(" berada pada rentang <b>")
					.append(teksAmanHtml(hurufSeharusnya)).append("</b> yaitu ")
					.append(Common.numberFormat.get().format(aturanHuruf.getMulai())).append(" s.d ")
					.append(Common.numberFormat.get().format(aturanHuruf.getSampai())).append(".</li>");
		}

		if (tampilSementara) {
			html.append("<li><b>Nilai belum diverifikasi.</b> Analisis memakai nilai sementara, sehingga hasil akhir dapat berubah setelah verifikasi selesai.</li>");
		}
		if (perkuliahan != null && perkuliahan.getPersenKehadiranDinilai0() > 0.1) {
			html.append("<li>Kehadiran mahasiswa <b>").append(Common.numberFormat.get().format(persenHadir))
					.append("%</b>; batas minimal agar nilai tidak menjadi 0 adalah <b>")
					.append(Common.numberFormat.get().format(perkuliahan.getPersenKehadiranDinilai0()))
					.append("%</b>.</li>");
		}
		if (perkuliahan != null && perkuliahan.getJikaAdaNilai0TidakMenghitungNilaiAkhir()) {
			html.append("<li>Aturan <b>jika ada nilai 0 maka nilai akhir tidak dihitung</b> sedang aktif. Komponen bernilai 0 wajib diperiksa.</li>");
		} else if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()) {
			html.append("<li>Aturan <b>nilai 0 tidak masuk pembagi</b> sedang aktif. Bobot komponen bernilai 0 tidak ikut membentuk rata-rata akhir.</li>");
		}
		if (totalBobot < 99.9 || totalBobot > 100.1) {
			html.append("<li><b>Total bobot efektif ").append(Common.numberFormat.get().format(totalBobot))
					.append("%</b>. Jika tidak sesuai harapan, cek bobot Format Nilai karena perhitungan memakai bobot efektif ini.</li>");
		}
		if (targetBerikut != null && targetBerikut.getMulai() != null && bobotTerbesar != null && totalBobot > 0.0) {
			double kurangTotal = targetBerikut.getMulai().doubleValue() - total;
			double bobot = nilaiAman(bobotTerbesar.getPersen());
			double perluNaikKomponen = bobot <= 0.0 ? 0.0 : kurangTotal / (bobot / totalBobot);
			if (kurangTotal > 0.0 && perluNaikKomponen > 0.0) {
				html.append("<li>Jalur tercepat untuk naik ke <b>").append(teksAmanHtml(targetBerikut.getNilaiHuruf()))
						.append("</b>: komponen berbobot terbesar adalah <b>")
						.append(teksAmanHtml(bobotTerbesar.getNama())).append("</b> (")
						.append(Common.numberFormat.get().format(bobot)).append("%). Secara kasar perlu tambahan sekitar <b>")
						.append(Common.numberFormat.get().format(perluNaikKomponen))
						.append("</b> poin pada komponen itu, selama nilai maksimal komponen masih memungkinkan.</li>");
			}
		}
		html.append("</ol>");
		html.append("</div>");
		return html.toString();
	}

	/**
	 * Memeriksa apakah kelas tempat sebuah baris nilai berada sedang <b>terkunci</b>, dengan menelusuri
	 * {@code detailperkuliahan.getPerkuliahan().getDikunci()}.
	 *
	 * <p>Pemeriksaan ini dipakai bagian analisis untuk memutuskan apakah perbedaan antara snapshot
	 * {@code nilaiHurufKunci} dan huruf hasil pemetaan total layak dilaporkan. Pada kelas yang belum
	 * terkunci, snapshot memang belum bermakna dan perbedaannya tidak perlu diributkan; barulah setelah
	 * penguncian, selisih itu menjadi temuan yang berarti.</p>
	 *
	 * <p>Perlu diperhatikan bahwa metode ini menelusuri perkuliahan <b>milik baris tersebut</b>, bukan
	 * field {@link #perkuliahan} milik helper. Keduanya dapat berbeda pada kelas paralel, dan pilihan
	 * ini memang yang benar untuk analisis: status kunci yang mengikat sebuah nilai adalah status kelas
	 * tempat nilai itu tersimpan.</p>
	 *
	 * <p>Seluruh badan dibungkus penangkap galat dan mengembalikan {@code false} bila gagal &mdash;
	 * misalnya karena asosiasi perkuliahan tidak dapat dimuat di luar sesi Hibernate. Sikap
	 * gagal-terbuka ini aman di sini karena keluarannya hanya memengaruhi <i>kalimat penjelasan</i>
	 * yang ditampilkan, tidak pernah menentukan boleh atau tidaknya sebuah nilai ditulis. Jangan
	 * memakai ulang metode ini sebagai penjaga penulisan; penegakan kunci yang sesungguhnya ada di
	 * lapisan model.</p>
	 *
	 * @param detailperkuliahan baris nilai yang diperiksa; {@code null} menghasilkan {@code false}.
	 * @return {@code true} bila kelas terkunci dan status itu berhasil dibaca.
	 * @see #buatHtmlAnalisisPintar(Detailperkuliahan, double, String, String, NilaiHuruf, NilaiHuruf, boolean)
	 */
	private boolean apakahPerkuliahanTerkunci(Detailperkuliahan detailperkuliahan) {
		try {
			Perkuliahan kuliah = detailperkuliahan == null ? null : detailperkuliahan.getPerkuliahan();
			return kuliah != null && kuliah.getDikunci() != null;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	/**
	 * Merangkai tabel <b>Komponen Pembentuk Nilai</b>: satu baris untuk setiap komponen penilaian
	 * berbobot, memuat nama, nilai, bobot, <b>kontribusi</b>, dan status verifikasi, ditutup ringkasan
	 * total bobot pembagi serta komponen penyumbang terbesar dan terkecil.
	 *
	 * <p>Kolom kontribusi adalah inti tabel ini. Ia memperlihatkan berapa poin yang benar-benar
	 * disumbangkan sebuah komponen ke nilai akhir, dihitung sebagai
	 * {@code nilai * (bobot / totalBobot)} &mdash; yaitu rata-rata tertimbang dengan penyebut berupa
	 * total bobot <b>efektif</b>, bukan 100 tetap. Dengan begitu dosen dapat melihat mengapa komponen
	 * bernilai tinggi tetapi berbobot kecil hampir tidak menggeser nilai akhir.</p>
	 *
	 * <h3>Dua lintasan yang diperlukan</h3>
	 * <p>Metode menelusuri komponen dua kali, dan urutan itu tidak dapat dibalik. Lintasan pertama
	 * mengumpulkan data baris ke dalam daftar {@code Object[]} sekaligus <b>menjumlahkan
	 * {@code totalBobot}</b>; lintasan kedua baru dapat menghitung kontribusi, sebab pembaginya adalah
	 * total bobot yang hanya diketahui setelah seluruh komponen diperiksa.</p>
	 *
	 * <p>Setiap entri larik menyimpan lima hal berurutan: nama komponen, nilai, bobot, apakah bobotnya
	 * ikut menjadi pembagi, dan status verifikasinya. Penggunaan {@code Object[]} alih-alih kelas kecil
	 * memaksa pembacaan lewat indeks berkode keras dan pengubahan tipe manual di lintasan kedua &mdash;
	 * bentuk yang ringkas tetapi rapuh bila kolom bertambah.</p>
	 *
	 * <h3>Bobot yang tidak ikut menjadi pembagi</h3>
	 * <p>Sebuah komponen dikeluarkan dari penyebut bila aturan
	 * {@code getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()} sedang aktif <b>dan</b> nilainya
	 * di bawah 0,01. Barisnya tetap ditampilkan, tetapi diberi keterangan kecil berwarna oranye
	 * &quot;Bobot tidak masuk pembagi karena nilai 0&quot; dan kontribusinya dipaksa nol. Menampilkan
	 * baris yang dikecualikan &mdash; alih-alih menyembunyikannya &mdash; adalah pilihan yang tepat:
	 * dosen justru perlu tahu komponen mana yang tidak dihitung dan mengapa.</p>
	 *
	 * <p>Komponen berbobot {@code null} atau di bawah 0,01 dilewati sepenuhnya, karena komponen tanpa
	 * bobot memang tidak pernah membentuk nilai akhir. Bila tidak ada satu pun komponen yang lolos,
	 * tabel diganti keterangan bahwa belum ada komponen aktif yang dapat dianalisis.</p>
	 *
	 * <p><b>Catatan pada penanda sentinel.</b> {@code kontribusiMin} diawali 999999 dan
	 * {@code kontribusiMax} diawali &minus;1. Karena keduanya diperbarui tanpa syarat tambahan di
	 * lintasan kedua, keduanya selalu tergantikan begitu ada sedikitnya satu baris; nama penyumbang
	 * terkecil baru ditampilkan bila string-nya tidak kosong. Dengan hanya satu komponen, komponen
	 * yang sama akan disebut sebagai penyumbang terbesar sekaligus terkecil.</p>
	 *
	 * @param detailperkuliahan baris nilai yang komponennya dibedah.
	 * @param tampilSementara   {@code true} untuk membaca nilai lewat
	 *                          {@code retreiveDetailNilaiBelumVerify}, {@code false} untuk nilai final.
	 * @return potongan HTML berisi tabel komponen beserta ringkasannya.
	 * @see #buatHtmlAnalisisNilaiHuruf(Detailperkuliahan)
	 * @see #hitungTotalBobotEfektif(Detailperkuliahan, boolean)
	 */
	private String buatHtmlKomponenNilai(Detailperkuliahan detailperkuliahan, boolean tampilSementara) {
		StringBuilder html = new StringBuilder();
		double totalBobot = 0.0;
		List<Object[]> baris = new ArrayList<Object[]>();
		double kontribusiMax = -1.0;
		double kontribusiMin = 999999.0;
		String namaMax = "";
		String namaMin = "";

		if (formatNilais != null) {
			for (FormatNilai formatNilai : formatNilais) {
				if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
					continue;
				}
				double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
						: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
				double bobot = nilaiAman(formatNilai.getPersen());
				boolean bobotMasuk = !(perkuliahan != null
						&& perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir() && nilai < 0.01);
				if (bobotMasuk) {
					totalBobot += bobot;
				}
				baris.add(new Object[] { formatNilai.getNama(), Double.valueOf(nilai), Double.valueOf(bobot),
						Boolean.valueOf(bobotMasuk), Boolean.valueOf(detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai)) });
			}
		}

		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;'>Komponen Pembentuk Nilai</div>");
		html.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
		html.append("<tr style='background:#eef4fb;'><th style='text-align:left;padding:6px;border:1px solid #dbe5f0;'>Komponen</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Nilai</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Bobot</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Kontribusi</th><th style='text-align:center;padding:6px;border:1px solid #dbe5f0;'>Ver.</th></tr>");
		for (Object[] data : baris) {
			String nama = data[0] == null ? "" : data[0].toString();
			double nilai = nilaiAman((Double) data[1]);
			double bobot = nilaiAman((Double) data[2]);
			boolean bobotMasuk = ((Boolean) data[3]).booleanValue();
			boolean verifikasi = ((Boolean) data[4]).booleanValue();
			double kontribusi = totalBobot > 0.0 && bobotMasuk ? nilai * (bobot / totalBobot) : 0.0;
			if (kontribusi > kontribusiMax) {
				kontribusiMax = kontribusi;
				namaMax = nama;
			}
			if (kontribusi < kontribusiMin) {
				kontribusiMin = kontribusi;
				namaMin = nama;
			}
			html.append("<tr>");
			html.append("<td style='padding:6px;border:1px solid #dbe5f0;'>").append(teksAmanHtml(nama));
			if (!bobotMasuk) {
				html.append("<div style='color:#a16207;font-size:11px;'>Bobot tidak masuk pembagi karena nilai 0.</div>");
			}
			html.append("</td>");
			html.append("<td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>").append(Common.numberFormat.get().format(nilai)).append("</td>");
			html.append("<td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>").append(Common.numberFormat.get().format(bobot)).append("%</td>");
			html.append("<td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>").append(Common.numberFormat.get().format(kontribusi)).append("</td>");
			html.append("<td style='text-align:center;padding:6px;border:1px solid #dbe5f0;'>").append(verifikasi ? "Ya" : "Belum").append("</td>");
			html.append("</tr>");
		}
		html.append("</table>");
		if (baris.isEmpty()) {
			html.append("<div style='color:#64748b;margin-top:8px;'>Belum ada komponen format nilai aktif yang dapat dianalisis.</div>");
		} else {
			html.append("<div style='margin-top:8px;color:#334155;'>Total bobot pembagi: <b>")
					.append(Common.numberFormat.get().format(totalBobot)).append("%</b>.</div>");
			html.append("<div style='margin-top:4px;color:#334155;'>Kontribusi terbesar berasal dari <b>")
					.append(teksAmanHtml(namaMax)).append("</b>");
			if (namaMin != null && !namaMin.trim().isEmpty()) {
				html.append(", sedangkan kontribusi terkecil dari <b>").append(teksAmanHtml(namaMin)).append("</b>");
			}
			html.append(".</div>");
		}
		html.append("</div>");
		return html.toString();
	}

	/**
	 * Mencari aturan {@link NilaiHuruf} yang <b>seharusnya</b> berlaku untuk sebuah total nilai pada
	 * satu baris perkuliahan, dengan memakai jalur resolusi yang persis sama dengan yang dipakai sistem
	 * saat menyimpan nilai.
	 *
	 * <p>Kesamaan jalur itulah yang membuat metode ini berguna: karena ia memanggil
	 * {@code Common.getNilaiHuruf(...)} dengan rangkaian argumen yang identik dengan pemanggilan di
	 * jalur penyimpanan, selisih antara hasilnya dan huruf yang tersimpan di basis data merupakan bukti
	 * ketidaksinkronan yang sesungguhnya &mdash; bukan sekadar perbedaan cara menghitung. Seluruh
	 * deteksi &quot;huruf tidak sinkron&quot; dan &quot;snapshot kunci menyimpang&quot; pada kedua
	 * jendela analisis bertumpu pada metode ini.</p>
	 *
	 * <h3>Delapan penentu pemetaan</h3>
	 * <p>Rentang huruf di AIS tidak tunggal; ia dipilih berdasarkan total nilai, tahun angkatan
	 * mahasiswa, jurusan, fakultas, tahun akademik baris ini, jenis semester ganjil/genap, kode mata
	 * kuliah, serta jenis nilai huruf yang ditetapkan pada mata kuliah tersebut. Keluwesan itu
	 * memungkinkan satu perguruan tinggi menerapkan skala berbeda antar fakultas atau antar angkatan,
	 * tetapi juga berarti konfigurasi yang tidak lengkap pada salah satu dimensi membuat pemetaan
	 * gagal.</p>
	 *
	 * <p>Mata kuliah diresolusi dengan pola yang berulang di seluruh berkas ini: dari
	 * {@code getPerkuliahan().getMatakuliah()} bila perkuliahan ada, atau dari
	 * {@code getMatakuliahKonversi()} bila tidak &mdash; jalur konversi dipakai baris nilai yang
	 * berasal dari alih kredit dan tidak terikat jadwal kuliah mana pun. Jurusan dan fakultas
	 * diturunkan dari mahasiswa, bukan dari kelas, sehingga mahasiswa titipan dari prodi lain tetap
	 * dinilai memakai skala prodinya sendiri.</p>
	 *
	 * <p>Setiap penunjuk dijaga terhadap {@code null} secara berlapis, dan seluruh badan dibungkus
	 * penangkap galat yang mengembalikan {@code null} bila gagal &mdash; misalnya ketika proksi
	 * jurusan tidak dapat dimuat di luar sesi Hibernate. Pemanggil memperlakukan {@code null} sebagai
	 * temuan bermakna, yaitu &quot;rentang nilai huruf tidak ditemukan&quot;, dan menampilkannya
	 * sebagai anomali tersendiri.</p>
	 *
	 * @param detailperkuliahan baris nilai yang konteksnya dipakai untuk memilih aturan.
	 * @param total             nilai akhir yang hendak dipetakan.
	 * @return aturan huruf yang cocok, atau {@code null} bila tidak ada yang mencakup total tersebut
	 *         pada kombinasi konteks ini.
	 * @see #ambilAturanNilaiHurufBerikut(Detailperkuliahan, double)
	 */
	private NilaiHuruf ambilAturanNilaiHuruf(Detailperkuliahan detailperkuliahan, double total) {
		try {
			Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null ? detailperkuliahan.getMatakuliahKonversi()
					: detailperkuliahan.getPerkuliahan().getMatakuliah();
			Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
			Fakultas fakultas = jurusan == null ? null : jurusan.getFakultas();
			return Common.getNilaiHuruf(Double.valueOf(total), mahasiswa == null ? null : mahasiswa.getTahunangkatan(),
					jurusan, fakultas, detailperkuliahan.getTahunAkademik(),
					detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
					matakuliah == null ? "" : matakuliah.getKode(),
					matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	/**
	 * Mencari aturan {@link NilaiHuruf} <b>satu tingkat di atas</b> total nilai sekarang, yaitu huruf
	 * terdekat yang batas bawahnya masih di atas {@code total}. Hasilnya dipakai analisis untuk
	 * menjawab pertanyaan yang paling sering diajukan mahasiswa: berapa poin lagi yang dibutuhkan
	 * untuk naik satu huruf.
	 *
	 * <h3>Mengapa tidak cukup memindai daftar</h3>
	 * <p>{@code ConstantValues.nilaiHurufs} memuat aturan huruf dari <b>seluruh</b> prodi, fakultas,
	 * dan tahun akademik yang ada di sistem. Memilih begitu saja baris dengan {@code mulai} terkecil di
	 * atas total akan menghasilkan huruf milik prodi lain. Karena itu setiap kandidat harus
	 * <b>diverifikasi ulang</b>: untuk setiap baris yang batas bawahnya di atas total, metode memanggil
	 * {@link #ambilAturanNilaiHuruf(Detailperkuliahan, double)} pada nilai batas bawah itu, lalu hanya
	 * menerima kandidat bila aturan yang benar-benar berlaku pada nilai tersebut memang menghasilkan
	 * huruf yang sama. Langkah verifikasi ini menyaring aturan yang tidak berlaku bagi mahasiswa
	 * bersangkutan, dengan biaya satu pemanggilan resolusi penuh per kandidat.</p>
	 *
	 * <p>Di antara kandidat yang lolos, dipilih yang batas bawahnya <b>paling rendah</b> &mdash; yakni
	 * huruf berikutnya yang paling mudah dijangkau, bukan huruf tertinggi. Baris dengan {@code mulai}
	 * atau {@code nilaiHuruf} bernilai {@code null} dilewati sejak awal.</p>
	 *
	 * <p>Seluruh penelusuran dibungkus penangkap galat; kegagalan menyisakan kandidat seadanya
	 * ({@code null} bila belum ada yang lolos) dan analisis cukup melewatkan butir &quot;jalur tercepat
	 * naik huruf&quot;. Metode mengembalikan {@code null} secara wajar ketika mahasiswa sudah berada
	 * pada huruf tertinggi.</p>
	 *
	 * @param detailperkuliahan baris nilai yang konteksnya dipakai untuk memverifikasi kandidat.
	 * @param total             nilai akhir sekarang; kandidat harus berbatas bawah di atas angka ini.
	 * @return aturan huruf berikutnya yang benar-benar berlaku, atau {@code null} bila tidak ada.
	 * @see #ambilAturanNilaiHuruf(Detailperkuliahan, double)
	 */
	private NilaiHuruf ambilAturanNilaiHurufBerikut(Detailperkuliahan detailperkuliahan, double total) {
		NilaiHuruf kandidat = null;
		try {
			for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
				if (nilaiHuruf == null || nilaiHuruf.getMulai() == null || nilaiHuruf.getNilaiHuruf() == null
						|| nilaiHuruf.getMulai().doubleValue() <= total) {
					continue;
				}
				NilaiHuruf cocok = ambilAturanNilaiHuruf(detailperkuliahan, nilaiHuruf.getMulai().doubleValue());
				if (cocok == null || cocok.getNilaiHuruf() == null
						|| !cocok.getNilaiHuruf().equalsIgnoreCase(nilaiHuruf.getNilaiHuruf())) {
					continue;
				}
				if (kandidat == null || nilaiHuruf.getMulai().doubleValue() < kandidat.getMulai().doubleValue()) {
					kandidat = nilaiHuruf;
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return kandidat;
	}

	/**
	 * Menjumlahkan <b>bobot efektif</b> seluruh komponen penilaian bagi seorang mahasiswa, yakni
	 * penyebut yang benar-benar dipakai untuk menghitung rata-rata tertimbang nilai akhirnya.
	 *
	 * <p>Angka ini bisa berbeda dari 100% karena dua sebab yang perlu dibedakan. Pertama, bobot yang
	 * didefinisikan admin memang belum berjumlah 100% &mdash; kesalahan konfigurasi yang oleh Analisis
	 * Pintar dilaporkan sebagai peringatan. Kedua, aturan
	 * {@code getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()} sedang aktif dan mengeluarkan
	 * komponen bernilai nol dari penyebut. Sebab kedua bersifat <b>per mahasiswa</b>: dua mahasiswa
	 * pada kelas yang sama dapat memiliki penyebut berbeda tergantung komponen mana yang masih kosong.
	 * Karena itulah metode ini menerima {@code detailperkuliahan} dan bukan sekadar bekerja atas
	 * {@link #formatNilais}.</p>
	 *
	 * <p>Komponen berbobot {@code null} atau di bawah 0,01 selalu dilewati &mdash; komponen tanpa bobot
	 * tidak pernah membentuk nilai akhir. Pembacaan nilai mengikuti {@code tampilSementara} agar
	 * penyebut yang dihitung sepadan dengan angka yang sedang ditampilkan; menghitung penyebut dari
	 * nilai final sementara pembilangnya dari nilai sementara akan menghasilkan kontribusi yang keliru.
	 * Setiap nilai dilewatkan {@link #nilaiAman(Double)} sehingga {@code null} dan bilangan tidak valid
	 * diperlakukan sebagai nol.</p>
	 *
	 * <p>Mengembalikan {@code 0.0} bila {@link #formatNilais} masih {@code null}. Pemanggil wajib
	 * menjaga pembagian terhadap hasil ini; keduanya memang sudah melakukannya lewat syarat
	 * {@code totalBobot > 0.0}.</p>
	 *
	 * @param detailperkuliahan baris nilai yang bobot efektifnya dihitung.
	 * @param tampilSementara   {@code true} untuk membaca nilai sementara, {@code false} untuk final.
	 * @return jumlah bobot dalam persen; {@code 0.0} bila tidak ada komponen yang memenuhi syarat.
	 * @see #buatHtmlKomponenNilai(Detailperkuliahan, boolean)
	 */
	private double hitungTotalBobotEfektif(Detailperkuliahan detailperkuliahan, boolean tampilSementara) {
		double totalBobot = 0.0;
		if (formatNilais == null) {
			return totalBobot;
		}
		for (FormatNilai formatNilai : formatNilais) {
			if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
				continue;
			}
			double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
					: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
			if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()
					&& nilai < 0.01) {
				continue;
			}
			totalBobot += nilaiAman(formatNilai.getPersen());
		}
		return totalBobot;
	}

	/**
	 * Menentukan komponen penilaian dengan <b>bobot terbesar</b> yang masih ikut membentuk nilai akhir
	 * seorang mahasiswa. Analisis Pintar memakainya untuk menyarankan &quot;jalur tercepat&quot; menaikkan
	 * huruf: menaikkan nilai pada komponen berbobot terbesar memberi pertambahan total paling banyak
	 * per poin usaha.
	 *
	 * <p>Penyaringannya sengaja dibuat sepadan dengan {@link #hitungTotalBobotEfektif} agar saran yang
	 * diberikan konsisten dengan penyebut yang dipakai menghitung kontribusi. Komponen dilewati bila
	 * berbobot {@code null} atau di bawah 0,01, dan juga bila aturan &quot;nilai 0 tidak masuk
	 * pembagi&quot; sedang aktif sementara nilainya masih nol. Pengecualian terakhir itu penting:
	 * menyarankan mahasiswa mengejar komponen yang justru sedang dikeluarkan dari penyebut akan
	 * menyesatkan, sebab menaikkannya dari nol malah mengubah penyebut dan tidak berdampak sesederhana
	 * yang diperkirakan.</p>
	 *
	 * <p>Pemilihan memakai penanda sentinel {@code bobotTerbesar} bernilai &minus;1 sehingga komponen
	 * pertama yang lolos selalu terpilih. Bila beberapa komponen berbobot sama besar, yang <b>pertama
	 * ditemui</b> dalam urutan {@link #formatNilais} yang menang, karena perbandingannya memakai
	 * &quot;lebih besar dari&quot; dan bukan &quot;lebih besar atau sama dengan&quot;. Mengembalikan
	 * {@code null} bila {@link #formatNilais} masih {@code null} atau tidak ada komponen yang lolos
	 * penyaringan; pemanggil sudah menjaganya dan cukup melewatkan butir saran.</p>
	 *
	 * @param detailperkuliahan baris nilai yang komponennya dinilai.
	 * @param tampilSementara   {@code true} untuk membaca nilai sementara, {@code false} untuk final.
	 * @return komponen berbobot terbesar yang masih dihitung, atau {@code null} bila tidak ada.
	 * @see #buatHtmlAnalisisPintar(Detailperkuliahan, double, String, String, NilaiHuruf, NilaiHuruf, boolean)
	 */
	private FormatNilai ambilFormatNilaiBobotTerbesar(Detailperkuliahan detailperkuliahan, boolean tampilSementara) {
		FormatNilai kandidat = null;
		double bobotTerbesar = -1.0;
		if (formatNilais == null) {
			return null;
		}
		for (FormatNilai formatNilai : formatNilais) {
			if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
				continue;
			}
			double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
					: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
			if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()
					&& nilai < 0.01) {
				continue;
			}
			if (formatNilai.getPersen().doubleValue() > bobotTerbesar) {
				bobotTerbesar = formatNilai.getPersen().doubleValue();
				kandidat = formatNilai;
			}
		}
		return kandidat;
	}

	/**
	 * Menormalkan sebuah {@link Double} menjadi {@code double} primitif yang <b>selalu layak
	 * dihitung</b>, memetakan {@code null}, {@code NaN}, dan tak-hingga menjadi {@code 0.0}.
	 *
	 * <p>Ketiga keadaan itu benar-benar muncul di jalur penilaian, masing-masing dari sumber berbeda:
	 * {@code null} dari kolom nilai yang belum pernah diisi, {@code NaN} dari pembagian nol dibagi nol
	 * pada perhitungan rata-rata kelas kosong, dan tak-hingga dari pembagian dengan bobot bernilai nol.
	 * Tanpa penormalan ini, satu nilai buruk akan menjalar: {@code NaN} yang ikut dijumlahkan membuat
	 * seluruh rata-rata menjadi {@code NaN}, dan tercetak sebagai teks yang tidak dapat dipahami di
	 * tengah tabel analisis.</p>
	 *
	 * <p>Metode ini murni, tanpa efek samping, dan dipakai di hampir setiap titik pembacaan nilai pada
	 * kedua jendela analisis &mdash; termasuk saat membaca bobot {@code getPersen()}, bukan hanya nilai
	 * mahasiswa.</p>
	 *
	 * <p><b>Perhatikan konsekuensinya.</b> Memetakan &quot;tidak ada nilai&quot; menjadi angka nol
	 * membuat komponen yang belum diisi tidak dapat dibedakan dari komponen yang memang bernilai nol.
	 * Untuk keperluan analisis ini perbedaan tersebut memang tidak penting, tetapi jangan memakai ulang
	 * metode ini pada jalur penyimpanan, tempat perbedaan itu justru menentukan &mdash; aturan
	 * &quot;nilai 0 tidak masuk pembagi&quot; memperlakukan keduanya secara berbeda.</p>
	 *
	 * @param nilai angka yang hendak dinormalkan; boleh {@code null}.
	 * @return angka yang sama, atau {@code 0.0} bila masukan {@code null} atau bukan bilangan berhingga.
	 */
	private double nilaiAman(Double nilai) {
		if (nilai == null || nilai.isNaN() || nilai.isInfinite()) {
			return 0.0;
		}
		return nilai.doubleValue();
	}

	/**
	 * Melarikan karakter khusus HTML pada sebuah teks sebelum ia ditempelkan ke dokumen yang dirangkai
	 * kedua jendela analisis. Inilah <b>penjagaan tunggal</b> yang mencegah data berujung menjadi
	 * markah aktif di dalam komponen {@link Html} milik ZK.
	 *
	 * <p>Empat karakter dipetakan: {@code &} menjadi {@code &amp;amp;}, {@code <} menjadi
	 * {@code &amp;lt;}, {@code >} menjadi {@code &amp;gt;}, dan tanda kutip ganda menjadi
	 * {@code &amp;quot;}. <b>Urutannya tidak boleh diubah</b>: ampersand wajib dilarikan lebih dulu,
	 * sebab bila dikerjakan belakangan ia akan melarikan ulang ampersand milik entitas yang baru saja
	 * dibuat dan menghasilkan {@code &amp;amp;lt;} alih-alih {@code &amp;lt;}.</p>
	 *
	 * <p>Metode ini wajib dipakai untuk setiap teks yang berasal dari basis data &mdash; nama
	 * mahasiswa, NIM, nama komponen penilaian, huruf, dan seluruh teks alasan &mdash; karena data itu
	 * dapat memuat karakter markah, baik karena kesalahan pemasukan maupun dengan sengaja. Nama
	 * mahasiswa yang mengandung tanda kurang-dari, misalnya, akan merusak struktur tabel analisis bila
	 * ditempelkan mentah. Literal yang ditulis langsung di dalam kode tidak memerlukannya, tetapi
	 * melarikannya tetap dilakukan agar pola pemakaiannya seragam dan tidak perlu dipikirkan ulang
	 * setiap kali.</p>
	 *
	 * <p>Tanda kutip <b>tunggal</b> sengaja tidak dilarikan. Itu aman pada pemakaian sekarang karena
	 * seluruh atribut pada HTML yang dirangkai kelas ini &mdash; yang memang memakai kutip tunggal
	 * untuk {@code style} &mdash; berisi literal tetap, dan tidak ada satu pun teks hasil pelarian ini
	 * yang ditempatkan di dalam atribut. Bila kelak sebuah nilai data disisipkan ke dalam atribut,
	 * pelarian kutip tunggal harus ditambahkan lebih dulu.</p>
	 *
	 * <p>Masukan {@code null} menghasilkan string kosong, sehingga pemanggil dapat merangkainya tanpa
	 * pemeriksaan tambahan. Metode ini murni dan tanpa efek samping.</p>
	 *
	 * @param teks teks mentah yang mungkin memuat karakter markah; boleh {@code null}.
	 * @return teks yang aman ditempelkan ke badan dokumen HTML; tidak pernah {@code null}.
	 */
	private String teksAmanHtml(String teks) {
		if (teks == null) {
			return "";
		}
		return teks.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

}
