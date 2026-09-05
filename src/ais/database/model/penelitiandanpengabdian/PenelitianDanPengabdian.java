package ais.database.model.penelitiandanpengabdian;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
import ais.database.model.PengumumanAkademis;
import ais.database.model.Perkuliahan;

/**
 * Entitas <b>skema/program penelitian &amp; pengabdian kepada masyarakat</b> (sering disebut
 * "skim" atau "gelombang penawaran") — bukan catatan satu proyek penelitian milik seorang
 * dosen. Satu baris di sini mewakili SATU penawaran yang dibuka oleh LPPM/lembaga penelitian,
 * misalnya "Penelitian Dasar Internal 2026" atau "Pengabdian Masyarakat Semester Ganjil
 * 2026/2027": lengkap dengan periode pengajuan, audiens yang boleh mengusulkan, dan seluruh
 * naskah panduan (pendahuluan, tujuan, luaran, kriteria pengusulan, sistematika proposal,
 * format sampul/pengesahan, borang desk evaluasi, borang pembahasan, borang monev, borang
 * kelayakan, serta lampiran-lampiran baku).
 *
 * <p><b>Posisi dalam alur modul.</b> Proposal yang diajukan dosen TIDAK disimpan di sini,
 * melainkan pada {@link PengajuanPenelitianDanPengabdian} yang memegang FK ke entitas ini;
 * tahapan pelaporan pasca-penerimaan disimpan pada
 * {@link TahapanPelaporanPenelitianDanPengabdian} (juga ber-FK ke sini) dengan berkas
 * unggahan pada {@link FilePengajuanPenelitianDanPengabdian} dan
 * {@link FilePengajuanTahapanPelaporanPenelitianDanPengabdian}. Jadi urutan konseptualnya:
 * <i>skema (kelas ini) &rarr; pengajuan/proposal &rarr; tahapan pelaporan &rarr; luaran
 * (artikel/publikasi)</i>. Layar pengelolanya adalah
 * {@code ais.action.master.penelitiandanpengabdian.PenelitianDanPengabdianAction}, sedangkan
 * layar pengusul memakai {@code PengajuanPenelitianDanPengabdianHelper} beserta turunannya
 * ({@code PengajuanPenelitianHelper}, {@code PengajuanPengabdianHelper},
 * {@code PersetujuanPenelitianHelper}).
 *
 * <p><b>Dua sumbu klasifikasi yang saling bebas (diverifikasi dari pemakaian, bukan dari
 * penamaan).</b> Entitas ini memegang dua FK master yang mudah tertukar:
 * <ul>
 * <li>{@link #getTipePenelitianDanPengabdian() tipePenelitianDanPengabdian} &rarr;
 * {@link TipePenelitianDanPengabdian}: sumbu <i>kategori kegiatan</i> — inilah yang secara
 * de-facto memisahkan penelitian dari pengabdian. Tiga barisnya dibuat otomatis oleh
 * {@code InitDataHelper} dengan kode tetap {@code 001.000} ("Penelitian Ilmiah"),
 * {@code 002.000} ("Pengabdian Masyarakat"), dan {@code 003.000} ("Lainnya"), lalu dipegang
 * sebagai singleton di {@code ConstantValues.PENELITIAN}, {@code ConstantValues.PENGABDIAN},
 * dan {@code ConstantValues.PENELITIAN_LAINNYA}. Hampir semua penyaringan lintas modul
 * (BKD/kinerja dosen, dasbor, LKPS akreditasi 3.a.2 &amp; 4.a.2, profil dosen) memakai sumbu
 * ini. Struktur masternya rata (tanpa parent).</li>
 * <li>{@link #getJenisPenelitianDanPengabdian() jenisPenelitianDanPengabdian} &rarr;
 * {@link JenisPenelitianDanPengabdian}: sumbu <i>klasifikasi/rumpun berjenjang</i> — master
 * yang mereferensi dirinya sendiri ({@code parent}) sehingga bisa membentuk pohon skema.
 * Dipakai untuk label dan pengelompokan pada layar skema, bukan untuk memilah
 * penelitian-vs-pengabdian.</li>
 * </ul>
 * Perhatikan pembalikan kewajiban yang kontra-intuitif: FK <i>jenis</i> dideklarasikan
 * {@code nullable = false} (wajib), sedangkan FK <i>tipe</i> — yang justru menentukan makna
 * fungsional record — dideklarasikan {@code nullable = true} (opsional). Skema tanpa tipe
 * tetap tersimpan sah, dan pada penyaringan
 * {@code PengajuanPenelitianDanPengabdianHelper} baris bertipe {@code null} sengaja
 * ikut lolos ({@code Restrictions.isNull("tipePenelitianDanPengabdian")}), sehingga sebuah
 * skema tanpa tipe akan muncul di layar pengajuan penelitian MAUPUN pengabdian.
 *
 * <p><b>Naskah panduan sebagai kolom {@code text}.</b> Lima belas field bertipe {@link String}
 * di kelas ini menyimpan potongan HTML utuh, masing-masing dengan nilai bawaan panjang yang
 * mereplikasi Panduan Pelaksanaan Penelitian dan Pengabdian kepada Masyarakat terbitan
 * Ditlitabmas Ditjen Dikti (lengkap dengan tabel bobot penilaian, rentang skor, dan
 * placeholder titik-titik yang harus diganti tiap perguruan tinggi). Nilai bawaan itu
 * ditetapkan pada inisialisasi field, jadi ia hanya berlaku untuk objek baru
 * ({@code new PenelitianDanPengabdian()}) dan akan ditimpa Hibernate saat baris lama dimuat.
 * Isi HTML ini dirender apa adanya ke layar ZK/laporan, sehingga bertindak sebagai konten
 * tepercaya yang hanya boleh disunting administrator skema.
 *
 * <p><b>Gerbang periode pengajuan bersifat kosmetik.</b> {@link #getDibuka() dibuka},
 * {@link #getTanggalMulaiPengajuan() tanggalMulaiPengajuan}, dan
 * {@link #getTanggalSampaiPengajuan() tanggalSampaiPengajuan} hanya ditampilkan dan disunting
 * di layar admin; penelusuran seluruh basis kode menunjukkan tidak ada satu pun jalur
 * penyimpanan proposal yang membacanya. Kriteria pemilihan skema pada layar pengusul
 * ({@code PengajuanPenelitianDanPengabdianHelper}) hanya menyaring {@code aktif},
 * {@code diperuntukkan}, dan {@code tipePenelitianDanPengabdian} — bukan {@code dibuka},
 * bukan pula rentang tanggal. Ini pengulangan pola "gerbang UI-only" yang sudah terdokumentasi
 * di beberapa domain lain pada basis kode ini; rinciannya dicatat pada
 * {@link #getDibuka()} dan {@link #getTanggalSampaiPengajuan()}.
 *
 * <p><b>Pembuatan otomatis.</b> Bila untuk sebuah kombinasi tipe/audiens belum ada satu pun
 * skema aktif, {@code PengajuanPenelitianDanPengabdianHelper} membuat sendiri satu baris
 * {@code PenelitianDanPengabdian} (judul "&lt;tipe&gt; &lt;tahun&gt;", {@code dibuka=true},
 * jenis diambil dari baris {@code JenisPenelitianDanPengabdian} ber-id terkecil) dan langsung
 * menyimpannya. Konsekuensinya: tabel ini dapat bertambah baris tanpa ada administrator yang
 * menekan tombol simpan, dan skema hasil pembuatan otomatis mewarisi seluruh naskah panduan
 * bawaan Dikti apa adanya (masih penuh placeholder "....").
 *
 * <p><b>Pemetaan.</b> Skema basis data {@code penelitiandanpengabdian}, tabel
 * {@code penelitian_dan_pengabdian}. Beranotasi {@code @Audited} (Envers) sehingga setiap
 * perubahan direkam ke tabel bayangan audit, serta {@code dynamicInsert}/{@code dynamicUpdate}
 * sehingga hanya kolom yang benar-benar berubah yang ikut dalam pernyataan SQL — poin ini
 * penting karena banyak getter di kelas ini menulis balik nilai bawaan ke field (lihat
 * catatan per-method).
 *
 * <p>Komentar pembangkit aslinya berbunyi "Bank generated by hbm2java" — sisa kerangka
 * Hibernate Tools yang disalin dari entitas lain dan tidak menggambarkan isi kelas ini.
 *
 * @see PengajuanPenelitianDanPengabdian
 * @see TahapanPelaporanPenelitianDanPengabdian
 * @see JenisPenelitianDanPengabdian
 * @see TipePenelitianDanPengabdian
 * @see LampiranUmumPenelitian
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "penelitiandanpengabdian", name = "penelitian_dan_pengabdian")



public class PenelitianDanPengabdian extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya identik dengan hampir semua entitas lain di
	 * paket ini karena kerangka kelas disalin dari entitas yang sama — jadi nilai ini tidak
	 * bisa dipakai untuk membedakan kelas, dan tidak boleh diubah selama masih ada objek
	 * ter-serialisasi (sesi ZK yang dipulihkan, cache terdistribusi) yang memakai versi lama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama sekuensial dari basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama tampil pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Identitas (username/id) pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang menyimpan baris ini (biasanya username
	 * atau id numerik pengguna, tergantung pemanggil yang mengisinya).
	 *
	 * <p>Bersama {@link #getOleh()} dan {@link #getTanggal_dirubah()}, field ini adalah
	 * <i>field audit bayangan</i>: ia diulang di hampir setiap entitas AIS alih-alih
	 * diwariskan dari {@link ais.database.model.GeneralValueObject}. Itu keharusan teknis,
	 * bukan cacat — {@code GeneralValueObject} adalah POJO abstrak biasa (bukan
	 * {@code @MappedSuperclass}), sehingga properti yang dideklarasikan di sana tidak ikut
	 * dipetakan Hibernate ke kolom tabel turunannya.
	 *
	 * @return identitas pengguna pengubah terakhir, atau {@code null} bila baris belum pernah
	 *         disimpan lewat jalur yang mengisi kolom audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas pengguna pengubah terakhir, dengan <b>penjaga satu arah</b>: bila
	 * argumen {@code null} atau hanya berisi spasi, method langsung keluar dan nilai lama
	 * dipertahankan.
	 *
	 * <p>Akibatnya nilai audit yang sudah terisi tidak dapat dikosongkan kembali lewat setter
	 * ini, termasuk oleh mekanisme penyalinan objek atau form CRUD generik yang mengirim
	 * string kosong untuk field yang tidak diisi. Untuk benar-benar mengosongkannya diperlukan
	 * pembaruan langsung di basis data.
	 *
	 * @param olehId identitas pengguna pengubah; nilai {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama tampil pengguna pengubah terakhir, dengan penjaga satu arah yang sama
	 * seperti {@link #setOlehId(String)}: argumen {@code null} atau kosong diabaikan sehingga
	 * jejak audit yang sudah ada tidak tertimpa nilai hampa.
	 *
	 * @param oleh nama tampil pengguna pengubah; nilai {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang menyimpan baris ini, untuk keperluan
	 * kolom "diubah oleh" pada layar daftar skema dan pada riwayat revisi.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum setiap {@code UPDATE} baris ini;
	 * ia mendelegasikan pencatatan stempel waktu dan identitas pengubah ke
	 * {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang sama dengan
	 * method ini — pola penyisipan otomatis yang dipakai di seluruh basis kode AIS agar blok
	 * audit dapat ditempelkan ke entitas lama tanpa mengubah struktur berkas. Nilai awalnya
	 * diambil dari {@code WaktuUtil.getDate()} sehingga objek baru sudah membawa waktu
	 * pembuatan meski belum pernah disimpan.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Umumnya tidak dipanggil kode aplikasi
	 * secara langsung karena {@link #onUpdate()} sudah mengurusnya; setter ini disediakan
	 * untuk Hibernate dan untuk proses migrasi/impor yang perlu mempertahankan waktu asli.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini, dipetakan sebagai
	 * {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir; untuk objek baru berisi waktu saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengembalikan judul skema sebagai representasi teks objek. Nilai inilah yang muncul di
	 * combobox pemilihan skema, di label ringkas layar pengajuan, dan di keluaran log.
	 *
	 * <p>Perhatikan bahwa method ini membaca field {@code judul} secara langsung, bukan lewat
	 * {@link #getJudul()}, dan tidak memberi nilai pengganti bila judul belum diisi. Untuk
	 * baris yang judulnya masih {@code null}, komponen ZK yang menerima hasil {@code toString()}
	 * akan menampilkan teks kosong (atau "null", tergantung komponennya) alih-alih melempar
	 * kesalahan.
	 *
	 * @return judul skema apa adanya, boleh {@code null}
	 */
	public String toString() {
		return judul;
	}

	/** Judul/nama skema yang diumumkan, mis. "Penelitian Dasar Internal 2026". Lihat {@link #getJudul()}. */
	private String judul;
	/** Tahun anggaran/pelaksanaan skema; diisi otomatis tahun berjalan bila kosong. Lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Audiens skema, salah satu konstanta {@code PengumumanAkademis.UNTUK_*}. Lihat {@link #getDiperuntukkan()}. */
	private String diperuntukkan;
	/** Tanggal awal periode pengajuan proposal (tidak ditegakkan saat penyimpanan). Lihat {@link #getTanggalMulaiPengajuan()}. */
	private Date tanggalMulaiPengajuan = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal akhir periode pengajuan proposal (tidak ditegakkan saat penyimpanan). Lihat {@link #getTanggalSampaiPengajuan()}. */
	private Date tanggalSampaiPengajuan = ais.ui.util.WaktuUtil.getDate();
	/** Penanda skema boleh tampil di kanal publik/tanpa login; bawaan {@code false}. Lihat {@link #getPublik()}. */
	private Boolean publik;

	/** FK wajib ke sumbu klasifikasi berjenjang. Lihat {@link #getJenisPenelitianDanPengabdian()}. */
	private JenisPenelitianDanPengabdian jenisPenelitianDanPengabdian;
	/** FK opsional ke sumbu kategori kegiatan (penelitian/pengabdian/lainnya). Lihat {@link #getTipePenelitianDanPengabdian()}. */
	private TipePenelitianDanPengabdian tipePenelitianDanPengabdian;

	/**
	 * Naskah HTML bab "Pendahuluan" panduan skema. Nilai bawaannya adalah teks latar belakang
	 * penelitian dasar versi panduan Ditlitabmas, masih memuat placeholder "...." yang harus
	 * diganti nama skema oleh administrator. Lihat {@link #getPendahuluan()}.
	 */
	private String pendahuluan = "Penelitian .... diluncurkan untuk mendorong .... melakukan penelitian dalam rangka memperoleh karya ilmiah yang mungkin tidak berdampak secara ekonomi dalam jangka pendek. Hal ini merupakan perbedaan paling penting dibandingkan dengan penelitian hibah bersaing.\n"
			+ "<br>"
			+ "Penelitian .... berorientasi kepada penjelasan atau penemuan (invensi) untuk mengantisipasi suatu gejala/fenomena, kaidah, model, atau postulat baru yang mendukung suatu proses teknologi, kesehatan, pertanian, dan lain-lain dalam rangka mendukung penelitian terapan. Termasuk dalam penelitian ini adalah pencarian metode atau teori baru.";
	/**
	 * Naskah HTML bab "Tujuan" panduan skema, bawaannya berupa daftar bernomor tiga butir
	 * tujuan penelitian dasar. Lihat {@link #getTujuan()}.
	 */
	private String tujuan = "Tujuan Penelitian .... adalah:\n<ol>"
			+ "<li>mendorong dosen melakukan penelitian dasar yang bersifat temuan sehingga memperoleh invensi, baik metode atau teori baru yang belum pernah ada sebelumnya;</li>"
			+ "<li>memperoleh modal ilmiah yang dapat mendukung perkembangan penelitian terapan; dan</li>"
			+ "<li>meningkatkan kuantitas dan kualitas publikasi ilmiah dosen.</li></ol>";

	/**
	 * Naskah HTML bab "Luaran Penelitian": luaran wajib (publikasi jurnal terakreditasi /
	 * bereputasi internasional) dan luaran tambahan (produk ipteks-sosbud, HKI, bahan ajar).
	 * Lihat {@link #getLuaranPenelitian()}.
	 */
	private String luaranPenelitian = "Luaran wajib dari Penelitian .... ini adalah publikasi dalam jurnal ilmiah terakreditasi atau jurnal ilmiah bereputasi internasional. Sedangkan luaran tambahan yang diharapkan dari penelitian ini adalah:\n"
			+ "<ol>"
			+ "<li>produk ipteks-sosbud (metode, blueprint , prototip, sistem, kebijakan, model, rekayasa sosial);</li>"
			+ "<li>HKI dan/atau bahan ajar.</li></ol>";
	/**
	 * Naskah HTML bab "Kriteria dan Pengusulan": syarat jabatan fungsional ketua, jumlah
	 * maksimum anggota tim, jangka waktu, rentang biaya, dan aturan satu usulan per pengusul.
	 *
	 * <p>Seluruh aturan di sini murni naratif — tidak satu pun divalidasi program saat proposal
	 * disimpan; pemeriksaannya diserahkan kepada penilai manusia pada tahap desk evaluasi.
	 * Lihat {@link #getKriteriaDanPengusulan()}.
	 */
	private String kriteriaDanPengusulan = "Kriteria dan persyaratan umum pengusulan Penelitian .... adalah:\n" + "<ol>"
			+ "<li>ketua tim peneliti adalah dosen bergelar minimum ... dengan jabatan fungsional Lektor Kepala atau dosen bergelar Doktor, sedangkan anggota tim peneliti boleh bergelar ... dengan jabatan di bawah Lektor Kepala;</li>"
			+ "<li>tim peneliti berjumlah maksimum tiga orang (satu ketua dan dua anggota) dengan tugas dan peran setiap peneliti diuraikan secara jelas dan disetujui oleh yang bersangkutan, disertai bukti tanda tangan pada setiap biodata yang dilampirkan;</li>"
			+ "<li>anggota peneliti dapat berubah pada tahun berikutnya sesuai dengan keperluan penelitian dan kompetensinya;</li>"
			+ "<li>ketua dan semua anggota tim peneliti harus memiliki track-record publikasi ilmiah yang relevan dengan bidang keilmuan dan mata kuliah yang diampu;</li>"
			+ "<li>jangka waktu penelitian adalah 1-2 tahun, dengan biaya berkisar antara Rp............,- - Rp............,-,-/judul/tahun;</li>"
			+ "<li>bagi pengusul yang berstatus mahasiswa, lembaga pengusul adalah perguruan tinggi asal yang bersangkutan;</li>"
			+ "<li>tiap pengusul hanya boleh mengusulkan satu usulan pada skema dan tahun yang sama, baik sebagai ketua maupun sebagai anggota; dan</li>"
			+ "<li>usulan penelitian disimpan menjadi satu file dalam format pdf dengan ukuran maksimum 5 MB dan diberi nama NamaKetuaPeneliti_NamaPT_PF.pdf, kemudian diunggah ke ..... dan hardcopy dikumpulkan di perguruan tinggi masing-masing.</li></ol>";
	/**
	 * Naskah HTML bab "Sistematika Usulan": batas halaman, aturan huruf/spasi/kertas, lalu
	 * urutan bab proposal dari Halaman Sampul sampai Lampiran, termasuk tabel ringkasan
	 * anggaran biaya per tahun. Lihat {@link #getSistematika()}.
	 */
	private String sistematika = "	<p class=\"font-IsiBab\">Usulan Penelitian .... <b>maksimum berjumlah 15 halaman</b> \n"
			+ "        (tidak termasuk halaman sampul, halaman pengesahan, dan lampiran), yang ditulis menggunakan <i>font</i>\n"
			+ "        Times New Roman ukuran 12 dengan jarak baris 1,5 spasi kecuali ringkasan satu spasi dan \n"
			+ "        ukuran kertas A-4 serta mengikuti sistematika sebagai berikut.</p>\n" + "	<ul>\n"
			+ "        <li class=\"font-IsiBab\">HALAMAN SAMPUL</li>\n" + "            &nbsp;<br>\n"
			+ "        <li class=\"font-IsiBab\">HALAMAN PENGESAHAN</li>\n" + "            &nbsp;<br>\n"
			+ "        <li class=\"font-IsiBab\">DAFTAR ISI </li>\n" + "            &nbsp;<br>\n"
			+ "        <li class=\"font-IsiBab\">RINGKASAN (maksimum satu halaman) <br>\n"
			+ "            Kemukakan tujuan jangka panjang dan target khusus yang ingin dicapai serta metode yang akan \n"
			+ "            dipakai dalam pencapaian tujuan tersebut. Ringkasan harus mampu menguraikan secara cermat dan \n"
			+ "            singkat tentang rencana kegiatan yang diusulkan. </li>\n" + "            &nbsp;<br>\n"
			+ "        <li class=\"font-IsiBab\">BAB 1. PENDAHULUAN <br>\n"
			+ "            Uraikan latar belakang dan permasalahan yang akan diteliti, tujuan khusus, dan urgensi penelitian. \n"
			+ "	    Pada bab ini juga dijelaskan temuan yang ditargetkan (gejala atau kaidah, metode, teori, atau \n"
			+ "		antisipasi) yang mempunyai kontribusi mendasar pada bidang ilmu dengan penekanan pada gagasan \n"
			+ "		fundamental dan orisinil untuk mendukung pengembangan IPTEKS-SOSBUD. </li>\n"
			+ "            &nbsp;<br>\n" + "        <li class=\"font-IsiBab\">BAB 2. TINJAUAN PUSTAKA <br>\n"
			+ "            Kemukakan <i>state of the art</i> dalam bidang yang diteliti, gunakan sumber pustaka acuan \n"
			+ "		primer yang relevan dan terkini dengan mengutamakan hasil penelitian pada jurnal ilmiah. \n"
			+ "		Jelaskan juga studi pendahuluan yang telah dilaksanakan dan hasil yang sudah dicapai \n"
			+ "		dalam bentuk <i>peta jalan</i> penelitian secara utuh..</li>\n" + "            &nbsp;<br>\n"
			+ "        <li class=\"font-IsiBab\">BAB 3. METODE PENELITIAN <br>\n"
			+ "          Lengkapi dengan alur penelitian dengan diagram alir penelitian yang menggambarkan apa yang \n"
			+ "		sudah dilaksanakan dan yang akan dikerjakan dalam 1 atau 2 tahun dalam bentuk <i>fishbone</i> diagram. \n"
			+ "		Bagan penelitian harus dibuat secara utuh dengan pentahapan yang jelas, mulai dari awal bagaimana \n"
			+ "		proses dan luarannya, dimana akan dilaksanakan, dan indikator capaian yang terukur.</li>\n"
			+ "            &nbsp;<br>\n" + "        <li class=\"font-IsiBab\">BAB 4. BIAYA DAN JADWAL PENELITIAN <br>\n"
			+ "            &nbsp;<br>\n" + "            4.1 Anggaran Biaya<br>\n"
			+ "            Anggaran biaya yang diajukan disusun secara rinci dan dilampirkan dengan format seperti pada Lampiran 2. \n"
			+ "		Ringkasan anggaran biaya yang diajukan pertahun disusun mengikuti komponen sebagaimana dalam \n"
			+ "		Tabel  berikut,<br>\n" + "            &nbsp;<br>\n"
			+ "            <table border=\"1\" cellpadding=\"3\" class=\"font-IsiBab\">\n"
			+ "                <tbody><tr><td rowspan=\"2\" class=\"font-IsiBab\"><b>No</b></td><td rowspan=\"2\"><b>Jenis Pengeluaran</b></td><td align=\"center\" colspan=\"3\"><b>Biaya yang diusulkan (Rp)</b></td></tr><tr>\n"
			+ "                    <td align=\"center\"><b>Tahun I</b></td><td align=\"center\"><b>Tahun ...</b></td><td align=\"center\"><b>Tahun n</b></td></tr> \n"
			+ "                <tr><td>1.</td><td>Gaji dan upah (Maks. 30%)</td><td>&nbsp; </td><td> &nbsp;</td><td>&nbsp; </td> </tr>\n"
			+ "                <tr><td>2.</td><td>Bahan habis pakai dan peralatan (30-40%)</td>\n"
			+ "                    <td>&nbsp; </td><td> &nbsp;</td><td>&nbsp; </td> </tr>\n"
			+ "                <tr><td>3.</td><td>Perjalanan (15-25%)</td>\n"
			+ "                    <td>&nbsp; </td><td> &nbsp;</td><td>&nbsp; </td> </tr>\n"
			+ "                <tr><td>4.</td><td>Lain-lain: publikasi, seminar, laporan, lainnya sebutkan (Maks. 15%)</td><td>&nbsp; </td>\n"
			+ "                    <td> &nbsp;</td><td>&nbsp; </td> </tr>\n" + "                \n"
			+ "                <tr><td colspan=\"2\">&nbsp;&nbsp;&nbsp;&nbsp;<b>Jumlah</b></td><td>&nbsp; </td><td> &nbsp;</td><td>&nbsp; </td> </tr>\n"
			+ "            </tbody></table>\n" + "            &nbsp;<br>\n" + "            4.2 Jadwal Penelitian<br>\n"
			+ "            Jadwal penelitian disusun dalam bentuk bar chart untuk rencana penelitian yang diajukan dan sesuai dengan \n"
			+ "            format pada Lampiran 3.</li>\n" + "            &nbsp;<br>\n"
			+ "        <li class=\"font-IsiBab\">DAFTAR PUSTAKA <br>\n"
			+ "            Daftar pustaka disusun berdasarkan sistem nama dan tahun, dengan urutan abjad nama pengarang, tahun, judul tulisan, \n"
			+ "            dan sumber. Hanya pustaka yang dikutip dalam usulan penelitian yang dicantumkan di dalam daftar pustaka.</li>\n"
			+ "            &nbsp;<br>\n" + "        <li class=\"font-IsiBab\">LAMPIRAN-LAMPIRAN <br>\n"
			+ "            <ul>\n"
			+ "                <li>Lampiran 1. Justifikasi Anggaran Penelitian (Lampiran 2). </li>\n"
			+ "                <li>Lampiran 2.	Dukungan sarana dan prasarana penelitian menjelaskan fasilitas yang menunjang penelitian, \n"
			+ "                        yaitu prasarana utama yang diperlukan dalam penelitian ini dan ketersediannya di perguruan tinggi \n"
			+ "                        pengusul. Apabila tidak tersedia, jelaskan bagaimana cara mengatasinya.</li>\n"
			+ "                <li>Lampiran 3.	Susunan organisasi tim peneliti dan pembagian tugas (Lampiran 4). </li>\n"
			+ "                <li>Lampiran 4.	Biodata ketua dan anggota (Lampiran 5). </li>\n"
			+ "                <li>Lampiran 5.	Surat pernyataan ketua peneliti (Lampiran 6).</li>\n"
			+ "            </ul>\n" + "  	 \n" + "            </li>	\n" + "    </ul>    	\n";
	/**
	 * Naskah HTML bab "Seleksi dan Evaluasi": menerangkan dua tahap seleksi (evaluasi daring
	 * lalu undangan pembahasan) dan menunjuk borang penilaian pada field
	 * {@link #deskEvaluasi} serta {@link #pembahasan}. Lihat {@link #getSeleksiDanEvaluasi()}.
	 */
	private String seleksiDanEvaluasi = "Seleksi dan evaluasi proposal Penelitian .... dilakukan dalam dua tahapan, yaitu evaluasi online dan undangan pembahasan untuk proposal yang dinyatakan lolos dalam evaluasi online. Komponen penilaian desk evaluasi proposal online dan komponen penilaian pembahasan proposal menggunakan formulir sebagaimana pada bagian Instrumen Penilaian";

	/**
	 * Naskah HTML contoh "Halaman Sampul" proposal, berupa tabel kosong siap-isi lengkap dengan
	 * keterangan warna sampul dan kode rumpun ilmu. Lihat {@link #getSampul()}.
	 */
	private String sampul = "	<p class=\"font-IsiBab\">Format Halaman Sampul Penelitian ....    (Warna Kuning)</p>\n"
			+ "	   \n"
			+ "            <table width=\"620\" border=\"1\" cellpadding=\"10\" height=\"700\" style=\"background-color: rgba(144,238,144,0.4);\">\n"
			+ "                <tbody><tr><td>\n"
			+ "                <table width=\"300\" border=\"1\" cellpadding=\"3\" height=\"10\" align=\"right\">\n"
			+ "    \n" + "                </table>\n"
			+ "                <table width=\"300\" border=\"1\" cellpadding=\"3\" height=\"10\" align=\"right\">\n"
			+ "                    <tbody><tr><td class=\"font-IsiBab\"> Kode/Nama Rumpun Ilmu** : ........./............. </td></tr>\n"
			+ "                </tbody></table>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b>USULAN</b></p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b>PENELITIAN ....</b></p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b>Logo Perguruan Tinggi</b></p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b>JUDUL PENELITIAN</b></p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b>TIM PENGUSUL</b></p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b>(Nama ketua dan anggota tim, lengkap dengan gelar dan NIDN)</b></p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b>PERGURUAN TINGGI</b></p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b> Bulan dan Tahun</b></p>\n"
			+ "            \n" + "                </td></tr>\n" + "            </tbody></table>\n"
			+ "            <p class=\"font-IsiBab\">* 	Tulis salah satu kode dan nama rumpun ilmu mengacu pada Lampiran 1. <br>\n"
			+ "            </p>\n" + "        \n";

	/**
	 * Naskah HTML contoh "Halaman Pengesahan" proposal: identitas ketua dan dua anggota, lama
	 * penelitian, rincian biaya per sumber dana, serta blok tanda tangan Dekan/Ketua, ketua
	 * peneliti, dan ketua lembaga penelitian.
	 *
	 * <p>Blok tanda tangan pada template ini adalah pengesahan <i>di atas kertas</i>; sistem
	 * tidak merekam persetujuan berbasis template ini. Persetujuan elektronik proposal ditangani
	 * terpisah pada {@link PengajuanPenelitianDanPengabdian}. Lihat {@link #getPengesahan()}.
	 */
	private String pengesahan = "	<p class=\"font-IsiBab\">Format Halaman Pengesahan Penelitian ....</p>\n" + "\n"
			+ "            <table width=\"620\" border=\"1\" cellpadding=\"10\" height=\"700\">\n"
			+ "                <tbody><tr><td>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\"><b>HALAMAN PENGESAHAN <br>\n"
			+ "                    PENELITIAN ....</b></p>\n"
			+ "                <table width=\"570\" border=\"0\" cellpadding=\"1\">\n"
			+ "                    <tbody><tr><td class=\"font-IsiBab\">Judul Penelitian</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> ................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">Kode/Nama Rumpun Ilmu</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> ............../.................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">Ketua Peneliti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> &nbsp; </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;a. Nama Lengkap</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;b. NIDN</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;c. Jabatan Fungsional</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;d. Program Studi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;e. Nomor HP</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;f.	Alamat surel (e-mail)</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">Anggota Peneliti (1)</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> &nbsp; </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;a. Nama Lengkap</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;b. NIDN</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;c. Perguruan Tinggi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">Anggota Peneliti (2)</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> &nbsp; </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;a. Nama Lengkap</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;b. NIDN</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;c. Perguruan Tinggi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .................................................................................................... </td></tr>\n"
			+ "                     <tr><td class=\"font-IsiBab\">Lama Penelitian Keseluruhan</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> .......... tahun </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">Penelitian Tahun ke</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> ........................ </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">Biaya Penelitian Keseluruhan</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> Rp ................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">Biaya Tahun Berjalan</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                        <td class=\"font-IsiBab\"> - diusulkan ke DIKTI &nbsp;&nbsp;&nbsp; Rp ................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;</td><td class=\"font-IsiBab\"> &nbsp; </td>\n"
			+ "                        <td class=\"font-IsiBab\"> - dana internal PT &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Rp ................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;</td><td class=\"font-IsiBab\"> &nbsp; </td>\n"
			+ "                        <td class=\"font-IsiBab\"> - dana institusi lain &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Rp ................... </td></tr>\n"
			+ "                    <tr><td class=\"font-IsiBab\">&nbsp;</td><td class=\"font-IsiBab\"> &nbsp; </td>\n"
			+ "                        <td class=\"font-IsiBab\"> - <i>inkind</i> sebutkan &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ....................... </td></tr>\n"
			+ "                </tbody></table>\n" + "                &nbsp; <br>\n"
			+ "                <table width=\"570\" border=\"0\" cellpadding=\"1\">\n"
			+ "                    <tbody><tr><td width=\"380\" class=\"font-IsiBab\">Mengetahui</td>\n"
			+ "                        <td class=\"font-IsiBab\">Kota, tanggal-bulan-tahun</td></tr>\n"
			+ "                    <tr><td width=\"380\" class=\"font-IsiBab\">Dekan/Ketua</td>\n"
			+ "                        <td class=\"font-IsiBab\">Ketua Peneliti,</td></tr>\n"
			+ "                    <tr><td width=\"380\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                        <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "                    <tr><td width=\"380\" class=\"font-IsiBab\">tanda tangan</td>\n"
			+ "                        <td class=\"font-IsiBab\">tanda tangan</td></tr>\n"
			+ "                    <tr><td width=\"380\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                        <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "                    <tr><td width=\"380\" class=\"font-IsiBab\">( Nama Lengkap )</td>\n"
			+ "                        <td class=\"font-IsiBab\">( Nama Lengkap )</td></tr>\n"
			+ "                    <tr><td width=\"380\" class=\"font-IsiBab\">NIP/NIK</td>\n"
			+ "                        <td class=\"font-IsiBab\">NIP/NIK</td></tr>\n"
			+ "                </tbody></table>\n" + "                \n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">Menyetujui,<br>\n"
			+ "                                                      Ketua lembaga penelitian</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;</p>                      \n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">tanda tangan</p>\n"
			+ "                <p class=\"font-IsiBab\" align=\"center\">&nbsp;<br>\n"
			+ "                                                      ( Nama Lengkap )<br>\n"
			+ "                                                      NIP/NIK</p>\n" + "                </td></tr>\n"
			+ "            </tbody></table> ";

	/**
	 * Naskah HTML bab "Sumber Dana" panduan skema — <b>uraian naratif</b>, bukan relasi ke
	 * master {@link SumberDanaPenelitianDanPengabdian}. Sumber dana yang benar-benar dipilih
	 * pengusul disimpan sebagai relasi banyak-ke-banyak pada
	 * {@link PengajuanPenelitianDanPengabdian}. Kesamaan nama antara field teks ini dan master
	 * tersebut mudah menyesatkan pembaca kode. Lihat {@link #getSumberDana()}.
	 */
	private String sumberDana = "        <p class=\"font-IsiBab\">Sumber dana Penelitian .... dapat berasal dari:</p>    \n"
			+ "        <ol>\n"
			+ "            <li class=\"font-IsiBab\">Ditlitabmas Ditjen Dikti termasuk BOPTN; dan</li>\n"
			+ "            <li class=\"font-IsiBab\">Internal perguruan tinggi.</li>\n" + "        </ol>\n";

	/**
	 * Naskah HTML bab "Pelaksanaan": alur pemantauan oleh penilai internal, kunjungan lapangan,
	 * dan evaluasi terpusat selama penelitian berjalan. Lihat {@link #getPelaksanaan()}.
	 */
	private String pelaksanaan = "Pelaksanaan Penelitian .... akan dipantau dan dievaluasi oleh penilai internal. Hasil pemantauan dan evaluasi internal dilaporkan oleh masing-masing perguruan tinggi melalui ...... Selanjutnya, penilai Ditlitabmas melakukan kunjungan lapangan (site visit) dan evaluasi terpusat terhadap pelaksanaan penelitian pada perguruan tinggi setelah menelaah hasil monitoring dan evaluasi internal yang masuk dalam ...... Hasil penilaian evaluasi terpusat diunggah ke ...... Pada akhir pelaksanaan penelitian, setiap peneliti melaporkan kegiatan hasil penelitian dalam bentuk kompilasi luaran penelitian.";
	/**
	 * Naskah HTML bab "Pelaporan": kewajiban logbook harian, unggah laporan kemajuan/akhir,
	 * presentasi kelayakan, dan kompilasi luaran. Padanan terekamnya dalam sistem adalah
	 * {@link TahapanPelaporanPenelitianDanPengabdian} beserta pengajuan tiap tahapnya.
	 * Lihat {@link #getPelaporan()}.
	 */
	private String pelaporan = "        <p class=\"font-IsiBab\">Setiap peneliti wajib melaporkan pelaksanaan penelitian \n"
			+ "            dengan melakukan hal-hal berikut:</p>\n" + "        <ol>\n"
			+ "            <li class=\"font-IsiBab\">mencatat semua kegiatan pelaksanaan program pada Buku Catatan Harian Penelitian \n"
			+ "                (<i>logbook</i>) dan mengisi kegiatan harian secara rutin terhitung sejak penandatanganan perjanjian \n"
			+ "                penelitian secara online di ..... ; </li>\n"
			+ "            <li class=\"font-IsiBab\">menyiapkan bahan pemantauan oleh penilai internal melalui ..... \n"
			+ "                dengan mengisi/mengunggah laporan kemajuan mengikuti format pada Lampiran 8 \n"
			+ "                (format penilaian pemantauan dan evaluasi mengikuti format di bagian Instrumen Penilaian); </li>\n"
			+ "            <li class=\"font-IsiBab\">mengunggah ke ..... softcopy laporan tahunan atau laporan akhir \n"
			+ "                (Lampiran 9) yang telah disahkan lembaga penelitian dalam format pdf dengan ukuran file \n"
			+ "                maksimum 5 MB, berikut softcopy luaran penelitian (publikasi ilmiah, HKI, paten, \n"
			+ "                makalah yang diseminarkan, teknologi tepat guna, rekayasa sosial, buku ajar, dan lain-lain) \n"
			+ "                atau dokumen bukti luaran;</li>\n"
			+ "            <li class=\"font-IsiBab\">menyiapkan bahan presentasi kelayakan capaian dan usulan tahun berikutnya \n"
			+ "                (format penilaian pembahasan/ kelayakan mengikuti format di bagian Instrumen Penilaian); </li>\n"
			+ "            <li class=\"font-IsiBab\">bagi peneliti yang dinyatakan lolos dalam presentasi kelayakan, \n"
			+ "                harus mengunggah proposal tahun berikutnya dengan format mengikuti proposal tahun sebelumnya; dan </li>\n"
			+ "            <li class=\"font-IsiBab\">kompilasi luaran penelitian sesuai dengan formulir pada Lampiran 10 pada akhir \n"
			+ "                pelaksanaan penelitian melalui ..... termasuk bukti luaran penelitian yang dihasilkan. </li>\n"
			+ "        </ol>\n";

	/**
	 * Naskah HTML borang "Desk Evaluasi Proposal": identitas usulan plus tabel lima kriteria
	 * penilaian berbobot (masalah 15%, orientasi 30%, metode 15%, luaran 30%, kelayakan
	 * sumber daya 10%) dengan skala skor 1-7.
	 *
	 * <p>Borang ini hanya <i>ditampilkan</i>; tidak ada perhitungan nilai otomatis dari template
	 * ini — skor asesor direkam lewat modul penilaian tersendiri. Lihat {@link #getDeskEvaluasi()}.
	 */
	private String deskEvaluasi = "        <p class=\"font-IsiBab\" align=\"center\"><b>FORMULIR DESK EVALUASI PROPOSAL <br>\n"
			+ "            PENELITIAN ....</b></p>\n" + "        <table width=\"620\" border=\"0\" cellpadding=\"1\">\n"
			+ "            <tbody><tr><td class=\"font-IsiBab\">Judul Penelitian</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Bidang Penelitian</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Perguruan Tinggi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Program Studi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Ketua Peneliti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> &nbsp; </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;a. Nama Lengkap</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;b. NIDN</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;c. Jabatan Fungsional</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Anggota Peneliti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> .......... orang </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Lama Penelitian Keseluruhan </td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> .......... tahun </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Biaya Penelitian Tahun ke-1</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> &nbsp; </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;a. Diusulkan ke Dikti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\">Rp .................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;b. Direkomendasikan</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "            <td class=\"font-IsiBab\">Rp .................... </td></tr>    \n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;c. Dana dari instansi lain</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "			<td class=\"font-IsiBab\">Rp .................... / <i>in kind</i> : ...................................... </td></tr>\n"
			+ "			</tbody></table>\n" + "         &nbsp;<br>\n"
			+ "        <table width=\"620\" border=\"1\" cellpadding=\"3\" class=\"font-IsiBab\">\n"
			+ "            <tbody><tr><td align=\"center\"><b>No</b></td><td><b>Kriteria Penilaian</b></td> <td align=\"center\"><b>Bobot (%)</b></td>\n"
			+ "		<td><b>Skor</b></td><td><b>Nilai</b></td></tr>\n" + "      \n"
			+ "       		<tr><td align=\"center\">1.</td><td> Masalah yang Diteliti: <br>\n"
			+ "                                a.	Kontribusi pada Ipteks-Sosbud, <br>\n"
			+ "                                b.	Tinjauan pustaka,<br>\n"
			+ "                                c.	Perumusan masalah.</td><td align=\"center\">15</td>\n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>								\n"
			+ "			<tr><td align=\"center\">2.</td><td> Orientasi Penelitian:  <br>\n"
			+ "                                a.	Makna Ilmiah, <br>\n"
			+ "                                b.	Orisinalitas dan kemutakhiran.</td><td align=\"center\">30</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "			<tr><td align=\"center\">3.</td><td> Metode Penelitian:  <br>\n"
			+ "                                a.	Pola pendekatan ilmiah, <br>\n"
			+ "								b.	Kesesuaian metode.</td><td align=\"center\">15</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>								\n"
			+ "			<tr><td align=\"center\">4.</td><td> Luaran Penelitian: <br>\n"
			+ "                                a.	Publikasi ilmiah, <br>\n"
			+ "                                b.	Teori/hipotesis baru,<br>\n"
			+ "                                c.	Metode baru dan informasi/desain baru.</td><td align=\"center\">30</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "            <tr><td align=\"center\">5.</td><td> Kelayakan Sumberdaya:   <br>\n"
			+ "                                a.	Peneliti, <br>\n"
			+ "                                b.	Peralatan,<br>\n"
			+ "                                c.	Rencana jadwal dan rencana biaya.</td><td align=\"center\">10</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "            <tr><td colspan=\"2\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Jumlah</b></td><td align=\"center\">100</td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "        </tbody></table>\n" + "        <p class=\"font-IsiBab\">Keterangan:	<br>\n"
			+ "            Skor : 1, 2, 3, 5, 6, 7 (1 = Buruk; 2 = Sangat kurang; 3 = Kurang; 5 = Cukup; 6 = Baik; 7 = Sangat baik); <br>\n"
			+ "            Nilai = Bobot x Skor</p>\n"
			+ "        <p class=\"font-IsiBab\"><b>Komentar Penilai: </b></p>\n"
			+ "        <p class=\"fontIsiBab\">.............................................................................................\n"
			+ "		............................................................ </p>\n"
			+ "        <p class=\"fontIsiBab\">.............................................................................................\n"
			+ "		............................................................ </p>\n" + "\n"
			+ "        <table width=\"620\" border=\"0\" cellpadding=\"1\">\n"
			+ "            <tbody><tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">Kota, tanggal-bulan-tahun</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">Penilai,</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">tanda tangan</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">( Nama Lengkap )</td></tr>\n" + "       </tbody></table>\n"
			+ "\n";

	/**
	 * Naskah HTML borang "Evaluasi Pembahasan Proposal" — tahap kedua seleksi, dipakai saat
	 * pengusul diundang mempresentasikan proposalnya. Bobot kriterianya berbeda dari
	 * {@link #deskEvaluasi} karena menambahkan komponen kemampuan presentasi (10%).
	 * Lihat {@link #getPembahasan()}.
	 */
	private String pembahasan = "        <p class=\"font-IsiBab\" align=\"center\"><b>FORMULIR EVALUASI PEMBAHASAN PROPOSAL <br>\n"
			+ "            PENELITIAN ....</b></p>\n" + "        <table width=\"620\" border=\"0\" cellpadding=\"1\">\n"
			+ "            <tbody><tr><td class=\"font-IsiBab\">Judul Penelitian</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "           <tr><td class=\"font-IsiBab\">Perguruan Tinggi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Program Studi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Ketua Peneliti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> &nbsp; </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;a. Nama Lengkap</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;b. NIDN</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;c. Jabatan Fungsional</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Anggota Peneliti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> .......... orang </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Lama Penelitian Keseluruhan </td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> .......... tahun </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Biaya Penelitian Tahun ke-1</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> &nbsp; </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;a. Diusulkan ke Dikti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\">Rp .................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;b. Direkomendasikan</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "            <td class=\"font-IsiBab\">Rp .................... </td></tr>    \n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;c. Dana dari instansi lain</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "			<td class=\"font-IsiBab\">Rp .................... / <i>in kind</i> : ...................................... </td></tr>\n"
			+ "			</tbody></table>\n" + "         &nbsp;<br>\n"
			+ "        <table width=\"620\" border=\"1\" cellpadding=\"3\" class=\"font-IsiBab\">\n"
			+ "            <tbody><tr><td align=\"center\"><b>No</b></td><td><b>Kriteria Penilaian</b></td> <td align=\"center\"><b>Bobot (%)</b></td> \n"
			+ "		<td><b>Skor</b></td><td><b>Nilai</b></td></tr>\n"
			+ "            <tr><td align=\"center\">1.</td><td> Kemampuan presentasi dan penguasaan materi penelitian</td><td align=\"center\">10</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "            <tr><td align=\"center\">2.</td><td> Masalah yang diteliti: <br>\n"
			+ "                                a.	Kontribusi pada Ipteks-Sosbud <br>\n"
			+ "                                b.	Tinjauan pustaka<br>\n"
			+ "	                            c.	Perumusan masalah</td><td align=\"center\">20</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "		        <tr><td align=\"center\">3.</td><td> Metode Penelitian: <br>\n"
			+ "                                a.	Makna Ilmiah  <br>\n"
			+ "                                b.	Orisinalitas<br>\n"
			+ "								c.	Kemutakhiran<br>\n"
			+ "                                d.	Pola pendekatan dan kesesuaian metode</td><td align=\"center\">30</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "		        <tr><td align=\"center\">4.</td><td> Potensi tercapainya luaran: <br>\n"
			+ "                                a.	Publikasi ilmiah <br>\n"
			+ "                                b.	Teori/hipotesis baru<br>\n"
			+ "								c.	Metode baru<br>\n"
			+ "                                d.	Informasi/desain baru</td><td align=\"center\">30</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "		        <tr><td align=\"center\">5.</td><td> Kelayakan Sumberdaya: <br>\n"
			+ "                                a.	<i>Track record</i> tim peneliti <br>\n"
			+ "                                b.	Sarana dan prasarana<br>\n"
			+ "								c.	Rencana jadwal penelitian.<br>\n"
			+ "                                d.	Rencana usulan biaya</td><td align=\"center\">10</td> \n"
			+ "		<td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "            <tr><td colspan=\"2\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Jumlah</b></td><td align=\"center\">100</td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "        </tbody></table>\n" + "        <p class=\"font-IsiBab\">Keterangan:	<br>\n"
			+ "            Skor : 1, 2, 3, 5, 6, 7 (1 = Buruk; 2 = Sangat kurang; 3 = Kurang; 5 = Cukup; 6 = Baik; 7 = Sangat baik); <br>\n"
			+ "            Nilai = Bobot x Skor</p>\n"
			+ "        <p class=\"font-IsiBab\"><b>Komentar Penilai: </b></p>\n"
			+ "        <p class=\"font-IsiBab\">.....................................................................................................................\n"
			+ ".		................................... </p>\n"
			+ "        <p class=\"font-IsiBab\">......................................................................................................................\n"
			+ "		................................... </p>\n" + "    \n"
			+ "        <table width=\"620\" border=\"0\" cellpadding=\"1\">\n"
			+ "            <tbody><tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">Kota, tanggal-bulan-tahun</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">Penilai,</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">tanda tangan</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">( Nama Lengkap )</td></tr>\n" + "       </tbody></table>\n"
			+ "\n";

	/**
	 * Naskah HTML "Borang Monitoring dan Evaluasi Lapangan": penilaian capaian penelitian yang
	 * sedang berjalan (persentase capaian 25%, publikasi ilmiah 25%, pemakalah 10%, HKI 10%,
	 * produk/model 25%, buku ajar 5%) dengan skala skor 1/2/4/5 beserta rubrik penjelasnya.
	 * Lihat {@link #getMonitoringDanEvaluasi()}.
	 */
	private String monitoringDanEvaluasi = "        <p class=\"font-IsiBab\" align=\"center\"><b>BORANG MONITORING DAN EVALUASI LAPANGAN<br>\n"
			+ "            PENELITIAN ....</b></p>\n" + "        <table width=\"620\" border=\"0\" cellpadding=\"1\">\n"
			+ "            <tbody><tr><td class=\"font-IsiBab\">Judul Penelitian</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>            \n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;</td><td class=\"font-IsiBab\"> &nbsp; </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Peneliti Utama</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">NIK/NIP</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">NIDN</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Perguruan Tinggi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Waktu Pelaksanaan Penelitian </td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> Tahun ke .....  dari rencana ..... tahun </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Biaya yang diusulkan ke Dikti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\">Rp .................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Biaya yang disetujui Dikti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\">Rp .................... </td></tr>            \n"
			+ "        </tbody></table>\n" + "         &nbsp;<br>\n"
			+ "        <table width=\"620\" border=\"1\" cellpadding=\"3\" class=\"font-IsiBab\">\n"
			+ "            <tbody><tr><td align=\"center\"><b>No</b></td><td colspan=\"2\" align=\"center\"><b>Komponen Penilaian</b></td> \n"
			+ "		<td colspan=\"4\" align=\"center\"><b>Keterangan</b></td> <td align=\"center\"><b>Bobot (%)</b></td> \n"
			+ "		<td><b>Skor</b></td><td><b>Nilai</b></td></tr>\n"
			+ "            <tr><td align=\"center\">1.</td><td colspan=\"2\"> Capaian penelitian</td><td align=\"center\">&lt; 25% </td> \n"
			+ "		<td align=\"center\"> 25 - 50% </td><td align=\"center\">51 - 75%</td><td align=\"center\"> &gt; 75%</td> \n"
			+ "		<td align=\"center\"> 25 </td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "            <tr><td rowspan=\"3\" align=\"center\">2.</td><td rowspan=\"3\"> Publikasi Ilmiah</td><td align=\"center\"> &nbsp;</td>\n"
			+ "                <td align=\"center\"><i>Draft</i></td><td align=\"center\"><i>Submitted</i></td><td align=\"center\"><i> Accepted</i></td>\n"
			+ "		<td align=\"center\"><i>Published</i></td><td rowspan=\"3\" align=\"center\">25</td> <td rowspan=\"3\">&nbsp;</td>\n"
			+ "		<td rowspan=\"3\">&nbsp;</td></tr>            \n"
			+ "            <tr><td>Internasional</td><td align=\"center\">&nbsp;</td><td align=\"center\">&nbsp;</td><td align=\"center\"> &nbsp;</td>\n"
			+ "		<td align=\"center\">&nbsp;</td></tr>\n"
			+ "            <tr><td>Nasional terakreditasi</td><td align=\"center\">&nbsp;</td><td align=\"center\">&nbsp;</td><td align=\"center\"> &nbsp;</td>\n"
			+ "		<td align=\"center\">&nbsp;</td></tr>\n"
			+ "            <tr><td rowspan=\"4\" align=\"center\">3.</td><td rowspan=\"4\"> Sebagai pemakalah dalam pertemuan ilmiah</td><td align=\"center\"> &nbsp;</td>\n"
			+ "                <td align=\"center\"><i>Draft</i></td><td align=\"center\">Terdaftar</td><td colspan=\"2\" align=\"center\">Sudah Dilaksanakan</td>\n"
			+ "		<td rowspan=\"4\" align=\"center\">10</td> <td rowspan=\"4\">&nbsp;</td><td rowspan=\"4\">&nbsp;</td></tr> \n"
			+ "            <tr><td>Internasional</td><td align=\"center\">&nbsp;</td><td align=\"center\">&nbsp;</td><td colspan=\"2\" align=\"center\">&nbsp;</td></tr>\n"
			+ "            <tr><td>Nasional</td><td align=\"center\">&nbsp;</td><td align=\"center\">&nbsp;</td><td colspan=\"2\" align=\"center\">&nbsp;</td></tr>\n"
			+ "            <tr><td>Lokal</td><td align=\"center\">&nbsp;</td><td align=\"center\">&nbsp;</td><td colspan=\"2\" align=\"center\">&nbsp;</td></tr>\n"
			+ "            <tr><td rowspan=\"2\" align=\"center\">4.</td><td rowspan=\"2\" colspan=\"2\"> Hak Kekayaan Intelektual: paten, paten sederhana, \n"
			+ "		hak cipta, merek dagang, rahasia dagang, desain produk industri, indikasi geografis, perlindungan varietas tanaman, \n"
			+ "		perlindungan topografi sirkuit terpadu</td> <td align=\"center\"><i>Draft</i></td><td align=\"center\">Terdaftar</td>\n"
			+ "		<td colspan=\"2\" align=\"center\"><i>Granted</i></td><td rowspan=\"2\" align=\"center\">10</td> <td rowspan=\"2\">&nbsp;</td>\n"
			+ "		<td rowspan=\"2\">&nbsp;</td></tr> \n"
			+ "            <tr><td align=\"center\">&nbsp;</td><td align=\"center\">&nbsp;</td><td colspan=\"2\" align=\"center\">&nbsp;</td></tr>\n"
			+ "            <tr><td rowspan=\"2\" align=\"center\">5.</td><td rowspan=\"2\" colspan=\"2\"> Produk/Model/Prototype/Desain/ Karya seni/ Rekayasa Sosial</td>\n"
			+ "                <td align=\"center\"><i>Draft</i></td><td align=\"center\">Produk</td><td colspan=\"2\" align=\"center\">Penerapan</td>\n"
			+ "		<td rowspan=\"2\" align=\"center\">25</td> <td rowspan=\"2\">&nbsp;</td><td rowspan=\"2\">&nbsp;</td></tr> \n"
			+ "            <tr><td align=\"center\">&nbsp;</td><td align=\"center\">&nbsp;</td><td colspan=\"2\" align=\"center\">&nbsp;</td></tr>\n"
			+ "            <tr><td rowspan=\"2\" align=\"center\">6.</td><td rowspan=\"2\" colspan=\"2\"> Buku Ajar</td>\n"
			+ "                <td align=\"center\"><i>Draft</i></td><td align=\"center\"><i>Editing</i></td><td colspan=\"2\" align=\"center\">Sudah terbit</td>\n"
			+ "		<td rowspan=\"2\" align=\"center\">5</td> <td rowspan=\"2\">&nbsp;</td><td rowspan=\"2\">&nbsp;</td></tr> \n"
			+ "            <tr><td align=\"center\">&nbsp;</td><td align=\"center\">&nbsp;</td><td colspan=\"2\" align=\"center\">&nbsp;</td></tr>\n"
			+ "            <tr><td colspan=\"7\" align=\"center\"><b>Jumlah</b></td> <td align=\"center\"><b>100</b></td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "        </tbody></table>\n" + "       \n"
			+ "        <p class=\"font-IsiBab\"><b>Komentar Pemantau: </b></p>\n"
			+ "        <p class=\"font-IsiBab\">...................................................................................................................\n"
			+ "		...................................... </p>\n"
			+ "        <p class=\"font-IsiBab\">...................................................................................................................\n"
			+ "		...................................... </p>\n" + "    \n"
			+ "        <table width=\"620\" border=\"0\" cellpadding=\"1\">\n"
			+ "            <tbody><tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">Kota, tanggal-bulan-tahun</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">Penilai</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">tanda tangan</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">( Nama Lengkap )</td></tr>\n" + "       </tbody></table>\n"
			+ "       <p class=\"font-IsiBab\"><b> Keterangan: </b></p>\n"
			+ "       <p class=\"font-IsiBab\"> Skor: 1, 2, 4, 5 (1 = kurang, 2 = cukup, 4 = baik, 5 = sangat baik) </p>\n"
			+ "       <ol>\n"
			+ "            <li class=\"font-IsiBab\">Capaian penelitian: Skor 5 = &gt; 75 %, 4 = 51-75 %, 2 = 25-50 %, 1 = &lt; 25 %.</li>\n"
			+ "            <li class=\"font-IsiBab\">Publikasi ilmiah dalam jurnal internasional/nasional terakreditasi: \n"
			+ "                Skor 5 = published/accepted, 4 = submitted, 2 = draft/belum ada.</li>\n"
			+ "            <li class=\"font-IsiBab\">Pemakalah pada pertemuan ilmiah internasional/nasional: \n"
			+ "                Skor 5 = sudah dilaksanakan/ terdaftar, 4 = draft, 2 = belum ada. \n"
			+ "                Untuk pertemuan ilmiah lokal : Skor 2 = sudah dilaksanakan, 1 = submitted/draft. </li>\n"
			+ "            <li class=\"font-IsiBab\">HKI: Skor 5 = granted/terdaftar, 4 = draft, 2 = belum/tidak ada.</li>\n"
			+ "            <li class=\"font-IsiBab\">Produk/Model/Prototype/Desain/Karya seni/ Rekayasa Sosial: \n"
			+ "                Skor 5 = penerapan/produk, 2 = draft/belum ada.</li>\n"
			+ "            <li class=\"font-IsiBab\">Buku Ajar: Skor 5 = sudah terbit/proses editing, 4 = draft, 2 = belum/tidak ada</li>\n"
			+ "       </ol>\n";

	/**
	 * Naskah HTML "Formulir Evaluasi Kelayakan dan Monev Terpusat", dipakai untuk menilai
	 * kelayakan usulan tahun berikutnya pada skema multitahun (perumusan masalah 15%,
	 * pentingnya penelitian 35%, tinjauan pustaka 15%, desain metode 20%, fisibilitas 15%)
	 * dan ditandatangani dua pembahas. Lihat {@link #getKelayakan()}.
	 */
	private String kelayakan = "        <p class=\"font-IsiBab\" align=\"center\"><b>FORMULIR EVALUASI KELAYAKAN DAN MONEV TERPUSAT <br>\n"
			+ "            PENELITIAN ....</b></p>\n" + "        <table width=\"620\" border=\"0\" cellpadding=\"1\">\n"
			+ "              <tbody><tr><td class=\"font-IsiBab\">Judul Penelitian</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Bidang Penelitian</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Perguruan Tinggi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Program Studi</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Ketua Peneliti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> &nbsp; </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;a. Nama Lengkap</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;b. NIDN</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">&nbsp;&nbsp;c. Jabatan Fungsional</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> ............................................................................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Jumlah Anggota Peneliti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> .......... orang </td></tr>\n"
			+ "			<tr><td class=\"font-IsiBab\">Waktu Pelaksanaan Penelitian </td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> Tahun ke .....  dari rencana ..... tahun </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Lama Penelitian Keseluruhan</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\"> .......... tahun </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Biaya disetujui tahun berjalan dari Dikti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\">Rp .................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Biaya tahun berikutnya diusulkan ke Dikti</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\">Rp .................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Biaya yang direkomendasikan</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\">Rp .................... </td></tr>\n"
			+ "            <tr><td class=\"font-IsiBab\">Biaya keseluruhan yang diusulkan</td><td class=\"font-IsiBab\"> : </td>\n"
			+ "                <td class=\"font-IsiBab\">Rp .................... </td></tr>\n"
			+ "        </tbody></table>\n" + "         &nbsp;<br>\n"
			+ "        <table width=\"620\" border=\"1\" cellpadding=\"3\" class=\"font-IsiBab\">\n"
			+ "            <tbody><tr><td align=\"center\"><b>No</b></td><td><b>Kriteria Penilaian</b></td> <td align=\"center\"><b>Bobot (%)</b></td> 		<td><b>Nilai</b></td><td><b>Skor</b></td></tr>\n"
			+ "            <tr><td align=\"center\">1.</td><td> Perumusan Masalah dan Tujuan Penelitian\n"
			+ "			</td><td align=\"center\">15</td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "            <tr><td align=\"center\">2.</td><td> Pentingnya Penelitian yang akan dilakukan:  <br>\n"
			+ "                a. Manfaat <br>\n" + "                b. Pengembangan Ipteks <br>\n"
			+ "                c. Penunjang Pembangunan<br>\n" + "                d. Pengembangan Institusi  </td>\n"
			+ "				<td align=\"center\">35</td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "          <tr><td align=\"center\">3.</td><td> Tinjauan Pustaka (Studi Pustaka/Kemajuan yang telah dicapai) <br>\n"
			+ "			</td><td align=\"center\">15</td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "			          <tr><td align=\"center\">4.</td><td> Desain Metode Penelitian <br>\n"
			+ "			</td><td align=\"center\">20</td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "            <tr><td align=\"center\">5.</td><td> Fisibilitas: <br>\n"
			+ "                a. Jadwal <br>\n" + "                b. Personalia <br>\n"
			+ "                c. Biaya<br>\n"
			+ "                d. Sarana dan prasarana penunjang </td><td align=\"center\">15</td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "              <tr><td colspan=\"2\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Jumlah</b></td><td align=\"center\">100</td> <td>&nbsp;</td><td>&nbsp;</td></tr>\n"
			+ "        </tbody></table>\n"
			+ "        <p class=\"font-IsiBab\">* Dinilai dari usulan penelitian untuk tahun berikutnya. <br>\n"
			+ "            Skor : 1, 2, 3, 5, 6, 7 (1 = Buruk; 2 = Sangat kurang; 3 = Kurang; 5 = Cukup; 6 = Baik; 7 = Sangat baik); <br>\n"
			+ "            Nilai = Bobot x Skor</p>\n"
			+ "        <p class=\"font-IsiBab\"><b>Saran dan Komentar: </b></p>\n"
			+ "        <p class=\"font-IsiBab\">....................................................................................................\n"
			+ "		..................................................... </p>\n"
			+ "        <p class=\"font-IsiBab\">....................................................................................................\n"
			+ "		..................................................... </p>\n" + "    \n"
			+ "        <table width=\"620\" border=\"0\" cellpadding=\"1\">\n"
			+ "            <tbody><tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">Kota, tanggal-bulan-tahun</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">Pembahas I,</td>\n"
			+ "                <td class=\"font-IsiBab\">Pembahas II,</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">tanda tangan</td>\n"
			+ "                <td class=\"font-IsiBab\">tanda tangan</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">&nbsp;</td>\n"
			+ "                <td class=\"font-IsiBab\">&nbsp;</td></tr>\n"
			+ "            <tr><td width=\"400\" class=\"font-IsiBab\">( Nama Lengkap )</td>\n"
			+ "                <td class=\"font-IsiBab\">( Nama Lengkap )</td></tr>\n" + "       </tbody></table>\n"
			+ "\n";

	/**
	 * Naskah HTML gabungan sembilan lampiran baku (justifikasi anggaran, jadwal kegiatan,
	 * susunan tim, biodata, surat pernyataan ketua, logbook, laporan kemajuan, laporan
	 * tahunan/akhir, evaluasi capaian luaran).
	 *
	 * <p>Berbeda dari field naskah lain yang nilai bawaannya berupa literal di kelas ini,
	 * bawaan field ini dibangkitkan {@link LampiranUmumPenelitian#init()} — pabrik HTML statis
	 * tanpa I/O maupun akses basis data. Pemanggilan itu terjadi pada setiap
	 * {@code new PenelitianDanPengabdian()}, termasuk saat Hibernate membuat instance untuk
	 * memuat baris lama (nilainya lalu langsung ditimpa isi kolom), sehingga string raksasa
	 * tersebut ikut dirakit sekali per objek.
	 */
	private String lampiranUmum = LampiranUmumPenelitian.init();
	/** Penanda skema masih dipakai; bawaan {@code true}. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Penanda pendaftaran proposal sedang dibuka; bawaan {@code true}. Lihat {@link #getDibuka()}. */
	private Boolean dibuka;
	/** Daftar alamat surel penerima notifikasi, dipisah koma. Lihat {@link #getKorespondensi()}. */
	private String korespondensi;
	/** Semester akademik skema (Ganjil/Genap), diturunkan dari tanggal mulai. Lihat {@link #getSemester()}. */
	private String semester;
	/** Tahun akademik skema, diturunkan dari tanggal mulai. Lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Bobot sks skema untuk perhitungan beban kerja dosen (BKD). Lihat {@link #getSks()}. */
	private Integer sks;
	/** Daftar nama grup pengguna penerima notifikasi, dipisah koma. Lihat {@link #getKorespondensiGrupPengguna()}. */
	private String korespondensiGrupPengguna;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate. Seluruh nilai bawaan naskah panduan
	 * (termasuk hasil {@link LampiranUmumPenelitian#init()}) terpasang lewat inisialisasi field,
	 * sehingga objek baru langsung berisi panduan lengkap versi Ditlitabmas yang siap disunting
	 * administrator skema.
	 */
	public PenelitianDanPengabdian() {
	}

	/**
	 * Mengembalikan kunci utama skema. Kolomnya dideklarasikan {@code insertable = false}
	 * karena nilainya dibangkitkan basis data ({@code IDENTITY}/sequence), sehingga nilai id
	 * yang disetel manual sebelum {@code save()} tidak ikut dikirim pada {@code INSERT}.
	 *
	 * <p>Id inilah yang dipakai sebagai parameter pada tautan pengumuman skema dan pada
	 * pemanggilan {@code session.get(PenelitianDanPengabdian.class, id)} di layar pengajuan;
	 * karena skema memang dimaksudkan sebagai katalog penawaran yang boleh dilihat calon
	 * pengusul, id di sini bukan kunci kepemilikan data pribadi.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Dipakai Hibernate saat memuat baris dan oleh proses
	 * impor/penyalinan; kode aplikasi biasa tidak perlu memanggilnya.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan naskah HTML bab "Pendahuluan" panduan skema apa adanya, untuk dirender ke
	 * tab panduan pada layar skema maupun layar pengajuan.
	 *
	 * <p>Nilai balik boleh {@code null} untuk baris lama yang kolomnya belum pernah diisi —
	 * pemanggil yang menyusun HTML gabungan perlu menjaga diri terhadap {@code null} tersebut.
	 * Isi kolom dirender sebagai HTML mentah, jadi hak sunting field ini setara hak menyisipkan
	 * markup ke halaman: batasi ke administrator skema.
	 *
	 * @return naskah HTML pendahuluan, boleh {@code null}
	 */
	@Column(name = "pendahuluan", nullable = true, columnDefinition = "text")
	public String getPendahuluan() {
		return this.pendahuluan;
	}

	/**
	 * Menetapkan naskah HTML bab "Pendahuluan". Menyimpan {@code null} atau string kosong
	 * diperbolehkan dan akan menghapus panduan bawaan Ditlitabmas untuk skema ini.
	 *
	 * @param pendahuluan naskah HTML pengganti
	 */
	public void setPendahuluan(String pendahuluan) {
		this.pendahuluan = pendahuluan;
	}

	/**
	 * Mengembalikan daftar alamat surel penerima notifikasi skema, satu string dipisah koma
	 * (mis. {@code "lppm@kampus.ac.id,wakilrektor2@kampus.ac.id"}).
	 *
	 * <p><b>Getter dengan efek samping:</b> bila field masih {@code null}, method menulis string
	 * kosong ke field lalu mengembalikannya. Karena entitas ini beranotasi {@code dynamicUpdate},
	 * penulisan balik itu mengubah kondisi objek terkelola dan dapat memunculkan
	 * {@code UPDATE} kolom {@code korespondensi} pada saat flush walau pengguna tidak menyunting
	 * apa pun — sekaligus mengubah nilai {@code NULL} lama menjadi string kosong.
	 *
	 * <p>Seluruh pemanggilnya ({@code PenelitianDanPengabdianAction},
	 * {@code PengajuanPenelitianDanPengabdianHelper},
	 * {@code PengajuanTahapanPelaporanPenelitianDanPengabdianHelper},
	 * {@code TahapanPelaporanPenelitianDanPengabdianHelper}) memecah hasilnya dengan
	 * {@code split(",")} lalu mengirim surel pemberitahuan pengajuan/pelaporan. Tidak ada
	 * validasi format alamat di lapisan entitas, sehingga kesalahan ketik hanya terlihat sebagai
	 * kegagalan kirim di log.
	 *
	 * @return daftar surel dipisah koma; tidak pernah {@code null}, tetapi bisa string kosong
	 */
	@Column(name = "korespondensi", nullable = true, length = 1000)
	public String getKorespondensi() {
		if (korespondensi == null) {
			korespondensi = "";
		}
		return korespondensi;
	}

	/**
	 * Menetapkan daftar alamat surel penerima notifikasi (dipisah koma, maksimum 1000 karakter
	 * sesuai panjang kolom). Nilai yang lebih panjang akan ditolak basis data saat flush.
	 *
	 * @param korespondensi daftar surel dipisah koma
	 */
	public void setKorespondensi(String korespondensi) {
		this.korespondensi = korespondensi;
	}

	/**
	 * Mengembalikan daftar nama grup pengguna penerima notifikasi, dipisah koma, sudah
	 * dipangkas spasi tepinya.
	 *
	 * <p>Berbeda dari {@link #getKorespondensi()}, normalisasi di sini dilakukan pada nilai
	 * balik saja ({@code null} dipetakan ke string kosong tanpa menulis field), sehingga getter
	 * ini bebas efek samping. Ketidakseragaman antara dua getter bersaudara ini sebaiknya
	 * diingat saat menyalin pola ke entitas lain.
	 *
	 * <p>Field ini tidak beranotasi {@code @Column}, jadi nama kolomnya mengikuti strategi
	 * penamaan Hibernate yang berlaku di konfigurasi AIS.
	 *
	 * @return daftar nama grup dipisah koma; tidak pernah {@code null}
	 */
	public String getKorespondensiGrupPengguna() {
		return korespondensiGrupPengguna == null ? "" : korespondensiGrupPengguna.trim();
	}

	/**
	 * Menetapkan daftar nama grup pengguna penerima notifikasi (dipisah koma). Nama grup
	 * kemudian dipakai pemanggil untuk mencari anggota grup dan mengumpulkan alamat surel
	 * mereka.
	 *
	 * @param korespondensiGrupPengguna daftar nama grup dipisah koma
	 */
	public void setKorespondensiGrupPengguna(String korespondensiGrupPengguna) {
		this.korespondensiGrupPengguna = korespondensiGrupPengguna;
	}

	/**
	 * Mengembalikan naskah HTML bab "Tujuan" panduan skema.
	 *
	 * @return naskah HTML tujuan, boleh {@code null} untuk baris lama
	 */
	@Column(name = "tujuan", nullable = true, columnDefinition = "text")
	public String getTujuan() {
		return this.tujuan;
	}

	/**
	 * Menetapkan naskah HTML bab "Tujuan".
	 *
	 * @param tujuan naskah HTML pengganti
	 */
	public void setTujuan(String tujuan) {
		this.tujuan = tujuan;
	}

	/**
	 * Mengembalikan klasifikasi berjenjang skema ({@link JenisPenelitianDanPengabdian}) —
	 * sumbu pelabelan/rumpun, bukan sumbu yang memisahkan penelitian dari pengabdian
	 * (lihat {@link #getTipePenelitianDanPengabdian()} untuk sumbu tersebut).
	 *
	 * <p>Relasi dipetakan {@code LAZY}, karena itu nilai field lebih dulu dilewatkan
	 * {@code check(...)} milik {@link ais.database.model.GeneralValueObject}: pembantu itu
	 * menyelesaikan proxy Hibernate menjadi objek nyata (lewat peta identitas entitas atau
	 * query ulang) sehingga pemanggil tetap dapat membaca {@code getIsi()} walau sesi asalnya
	 * sudah ditutup — pola lazy-safe yang dipakai konsisten di seluruh entitas AIS. Hasil
	 * resolusi ditulis balik ke field, jadi objek yang dikembalikan bisa berbeda instance dari
	 * proxy semula.
	 *
	 * <p>FK-nya {@code nullable = false}: setiap skema wajib punya jenis. Pemanggil di
	 * {@code PenelitianDanPengabdianAction} langsung memanggil {@code .getIsi()} dan
	 * {@code .getId()} atas hasil method ini tanpa pemeriksaan {@code null}, sehingga baris
	 * warisan yang kolomnya kosong akan memunculkan {@code NullPointerException} di layar.
	 *
	 * @return jenis/rumpun skema; secara skema data tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penelitian_dan_pengabdian", nullable = false)
	public JenisPenelitianDanPengabdian getJenisPenelitianDanPengabdian() {
		jenisPenelitianDanPengabdian = check(jenisPenelitianDanPengabdian);
		return jenisPenelitianDanPengabdian;
	}

	/**
	 * Menetapkan klasifikasi berjenjang skema.
	 *
	 * <p>Karena relasi memakai {@code cascade = PERSIST, MERGE}, menyimpan skema ikut menyimpan
	 * objek jenis yang belum tersimpan — jalur inilah yang dipakai
	 * {@code PengajuanPenelitianDanPengabdianHelper} saat membuat skema otomatis dengan jenis
	 * ber-id terkecil.
	 *
	 * @param jenisPenelitianDanPengabdian jenis/rumpun skema
	 */
	public void setJenisPenelitianDanPengabdian(JenisPenelitianDanPengabdian jenisPenelitianDanPengabdian) {
		this.jenisPenelitianDanPengabdian = jenisPenelitianDanPengabdian;
	}

	/**
	 * Mengembalikan naskah HTML bab "Luaran Penelitian" (luaran wajib dan luaran tambahan yang
	 * diharapkan dari skema ini).
	 *
	 * @return naskah HTML luaran, boleh {@code null} untuk baris lama
	 */
	@Column(name = "luaran_penelitian", nullable = true, columnDefinition = "text")
	public String getLuaranPenelitian() {
		return luaranPenelitian;
	}

	/**
	 * Menetapkan naskah HTML bab "Luaran Penelitian".
	 *
	 * @param luaranPenelitian naskah HTML pengganti
	 */
	public void setLuaranPenelitian(String luaranPenelitian) {
		this.luaranPenelitian = luaranPenelitian;
	}

	/**
	 * Mengembalikan naskah HTML bab "Kriteria dan Pengusulan" (syarat ketua/anggota tim, jangka
	 * waktu, rentang biaya, aturan satu usulan per pengusul).
	 *
	 * <p>Perlu ditegaskan: aturan pada naskah ini tidak divalidasi program mana pun saat
	 * proposal disimpan. Misalnya larangan "tiap pengusul hanya boleh mengusulkan satu usulan
	 * pada skema dan tahun yang sama" hanya mengikat secara administratif.
	 *
	 * @return naskah HTML kriteria pengusulan, boleh {@code null} untuk baris lama
	 */
	@Column(name = "kriteria_dan_pengusulan", nullable = true, columnDefinition = "text")
	public String getKriteriaDanPengusulan() {
		return kriteriaDanPengusulan;
	}

	/**
	 * Menetapkan naskah HTML bab "Kriteria dan Pengusulan".
	 *
	 * @param kriteriaDanPengusulan naskah HTML pengganti
	 */
	public void setKriteriaDanPengusulan(String kriteriaDanPengusulan) {
		this.kriteriaDanPengusulan = kriteriaDanPengusulan;
	}

	/**
	 * Mengembalikan naskah HTML bab "Sistematika Usulan" — kerangka bab proposal yang harus
	 * diikuti pengusul, lengkap dengan tabel ringkasan anggaran.
	 *
	 * @return naskah HTML sistematika, boleh {@code null} untuk baris lama
	 */
	@Column(name = "sistematika", nullable = true, columnDefinition = "text")
	public String getSistematika() {
		return sistematika;
	}

	/**
	 * Menetapkan naskah HTML bab "Sistematika Usulan".
	 *
	 * @param sistematika naskah HTML pengganti
	 */
	public void setSistematika(String sistematika) {
		this.sistematika = sistematika;
	}

	/**
	 * Mengembalikan naskah HTML bab "Seleksi dan Evaluasi" yang menerangkan dua tahap seleksi
	 * proposal beserta rujukan ke borangnya.
	 *
	 * @return naskah HTML seleksi dan evaluasi, boleh {@code null} untuk baris lama
	 */
	@Column(name = "seleksi_dan_evaluasi", nullable = true, columnDefinition = "text")
	public String getSeleksiDanEvaluasi() {
		return seleksiDanEvaluasi;
	}

	/**
	 * Menetapkan naskah HTML bab "Seleksi dan Evaluasi".
	 *
	 * @param seleksiDanEvaluasi naskah HTML pengganti
	 */
	public void setSeleksiDanEvaluasi(String seleksiDanEvaluasi) {
		this.seleksiDanEvaluasi = seleksiDanEvaluasi;
	}

	/**
	 * Mengembalikan naskah HTML contoh "Halaman Sampul" proposal untuk skema ini.
	 *
	 * @return naskah HTML halaman sampul, boleh {@code null} untuk baris lama
	 */
	@Column(name = "sampul", nullable = true, columnDefinition = "text")
	public String getSampul() {
		return sampul;
	}

	/**
	 * Menetapkan naskah HTML contoh "Halaman Sampul".
	 *
	 * @param sampul naskah HTML pengganti
	 */
	public void setSampul(String sampul) {
		this.sampul = sampul;
	}

	/**
	 * Mengembalikan naskah HTML contoh "Halaman Pengesahan" proposal.
	 *
	 * <p>Ini semata contoh format cetak; kolom tanda tangan di dalamnya tidak berhubungan dengan
	 * mekanisme persetujuan elektronik proposal yang dijalankan modul pengajuan.
	 *
	 * @return naskah HTML halaman pengesahan, boleh {@code null} untuk baris lama
	 */
	@Column(name = "pengesahan", nullable = true, columnDefinition = "text")
	public String getPengesahan() {
		return pengesahan;
	}

	/**
	 * Menetapkan naskah HTML contoh "Halaman Pengesahan".
	 *
	 * @param pengesahan naskah HTML pengganti
	 */
	public void setPengesahan(String pengesahan) {
		this.pengesahan = pengesahan;
	}

	/**
	 * Mengembalikan naskah HTML bab "Sumber Dana" panduan skema.
	 *
	 * <p>Jangan tertukar dengan master {@link SumberDanaPenelitianDanPengabdian}: method ini
	 * hanya mengembalikan uraian naratif, sedangkan sumber dana yang dipilih pengusul disimpan
	 * sebagai relasi banyak-ke-banyak di {@link PengajuanPenelitianDanPengabdian}.
	 *
	 * @return naskah HTML sumber dana, boleh {@code null} untuk baris lama
	 */
	@Column(name = "sumber_dana", nullable = true, columnDefinition = "text")
	public String getSumberDana() {
		return sumberDana;
	}

	/**
	 * Menetapkan naskah HTML bab "Sumber Dana".
	 *
	 * @param sumberDana naskah HTML pengganti
	 */
	public void setSumberDana(String sumberDana) {
		this.sumberDana = sumberDana;
	}

	/**
	 * Mengembalikan naskah HTML bab "Pelaksanaan" (pemantauan internal, kunjungan lapangan,
	 * evaluasi terpusat selama kegiatan berjalan).
	 *
	 * @return naskah HTML pelaksanaan, boleh {@code null} untuk baris lama
	 */
	@Column(name = "pelaksanaan", nullable = true, columnDefinition = "text")
	public String getPelaksanaan() {
		return pelaksanaan;
	}

	/**
	 * Menetapkan naskah HTML bab "Pelaksanaan".
	 *
	 * @param pelaksanaan naskah HTML pengganti
	 */
	public void setPelaksanaan(String pelaksanaan) {
		this.pelaksanaan = pelaksanaan;
	}

	/**
	 * Mengembalikan judul skema — nama penawaran yang dilihat pengusul, mis. "Penelitian Dasar
	 * Internal 2026".
	 *
	 * <p>Kolomnya {@code nullable = true} walau judul praktis wajib bagi kegunaan skema;
	 * {@link #toString()} membaca field yang sama dan ikut mengembalikan {@code null} bila
	 * belum diisi. Skema yang dibuat otomatis oleh helper pengajuan mengisi judul dengan pola
	 * "&lt;tipe&gt; &lt;tahun berjalan&gt;".
	 *
	 * @return judul skema, boleh {@code null}
	 */
	@Column(name = "judul", nullable = true, columnDefinition = "text")
	public String getJudul() {
		return judul;
	}

	/**
	 * Menetapkan judul skema.
	 *
	 * <p>Tidak ada penjaga keunikan: dua skema dengan judul persis sama pada tahun yang sama
	 * dapat tersimpan berdampingan dan akan tampil sebagai dua pilihan kembar di combobox
	 * pengajuan. Pembedanya hanya id.
	 *
	 * @param judul judul skema
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Mengembalikan naskah HTML bab "Pelaporan" (kewajiban logbook, laporan kemajuan, laporan
	 * akhir, dan kompilasi luaran).
	 *
	 * @return naskah HTML pelaporan, boleh {@code null} untuk baris lama
	 */
	@Column(name = "pelaporan", nullable = true, columnDefinition = "text")
	public String getPelaporan() {
		return pelaporan;
	}

	/**
	 * Menetapkan naskah HTML bab "Pelaporan".
	 *
	 * @param pelaporan naskah HTML pengganti
	 */
	public void setPelaporan(String pelaporan) {
		this.pelaporan = pelaporan;
	}

	/**
	 * Mengembalikan naskah HTML borang "Desk Evaluasi Proposal" (penilaian tahap pertama,
	 * daring) berikut tabel bobot dan rentang skornya.
	 *
	 * <p>Borang ini murni tampilan: nilai yang diketik penilai pada tabel HTML tidak terekam
	 * lewat kolom ini. Angka bobot di sini pun tidak terhubung ke perhitungan apa pun di
	 * program, sehingga mengubah bobot pada naskah tidak memengaruhi skor yang tersimpan.
	 *
	 * @return naskah HTML borang desk evaluasi, boleh {@code null} untuk baris lama
	 */
	@Column(name = "desk_evaluasi", nullable = true, columnDefinition = "text")
	public String getDeskEvaluasi() {
		return deskEvaluasi;
	}

	/**
	 * Menetapkan naskah HTML borang "Desk Evaluasi Proposal".
	 *
	 * @param deskEvaluasi naskah HTML pengganti
	 */
	public void setDeskEvaluasi(String deskEvaluasi) {
		this.deskEvaluasi = deskEvaluasi;
	}

	/**
	 * Mengembalikan naskah HTML borang "Evaluasi Pembahasan Proposal" — tahap kedua seleksi
	 * yang menambahkan komponen kemampuan presentasi pengusul.
	 *
	 * @return naskah HTML borang pembahasan, boleh {@code null} untuk baris lama
	 */
	@Column(name = "pembahasan", nullable = true, columnDefinition = "text")
	public String getPembahasan() {
		return pembahasan;
	}

	/**
	 * Menetapkan naskah HTML borang "Evaluasi Pembahasan Proposal".
	 *
	 * @param pembahasan naskah HTML pengganti
	 */
	public void setPembahasan(String pembahasan) {
		this.pembahasan = pembahasan;
	}

	/**
	 * Mengembalikan naskah HTML "Borang Monitoring dan Evaluasi Lapangan" untuk menilai capaian
	 * kegiatan yang sedang berjalan (capaian, publikasi, pemakalah, HKI, produk, buku ajar).
	 *
	 * @return naskah HTML borang monev, boleh {@code null} untuk baris lama
	 */
	@Column(name = "monitoring_dan_evaluasi", nullable = true, columnDefinition = "text")
	public String getMonitoringDanEvaluasi() {
		return monitoringDanEvaluasi;
	}

	/**
	 * Menetapkan naskah HTML "Borang Monitoring dan Evaluasi Lapangan".
	 *
	 * @param monitoringDanEvaluasi naskah HTML pengganti
	 */
	public void setMonitoringDanEvaluasi(String monitoringDanEvaluasi) {
		this.monitoringDanEvaluasi = monitoringDanEvaluasi;
	}

	/**
	 * Mengembalikan naskah HTML "Formulir Evaluasi Kelayakan dan Monev Terpusat", dipakai pada
	 * skema multitahun untuk menilai kelayakan usulan tahun berikutnya.
	 *
	 * @return naskah HTML borang kelayakan, boleh {@code null} untuk baris lama
	 */
	@Column(name = "kelayakan", nullable = true, columnDefinition = "text")
	public String getKelayakan() {
		return kelayakan;
	}

	/**
	 * Menetapkan naskah HTML "Formulir Evaluasi Kelayakan dan Monev Terpusat".
	 *
	 * @param kelayakan naskah HTML pengganti
	 */
	public void setKelayakan(String kelayakan) {
		this.kelayakan = kelayakan;
	}

	/**
	 * Mengembalikan naskah HTML gabungan sembilan lampiran baku proposal/laporan.
	 *
	 * <p>Untuk objek baru isinya berasal dari {@link LampiranUmumPenelitian#init()}; untuk baris
	 * tersimpan isinya adalah salinan yang mungkin sudah disunting administrator, sehingga
	 * pembaruan format lampiran di kelas {@code LampiranUmumPenelitian} <b>tidak</b> merambat ke
	 * skema yang sudah terlanjur tersimpan — tiap baris memegang salinannya sendiri.
	 *
	 * @return naskah HTML lampiran umum, boleh {@code null} untuk baris lama
	 */
	@Column(name = "lampiran_umum", nullable = true, columnDefinition = "text")
	public String getLampiranUmum() {
		return lampiranUmum;
	}

	/**
	 * Menetapkan naskah HTML lampiran umum.
	 *
	 * @param lampiranUmum naskah HTML pengganti
	 */
	public void setLampiranUmum(String lampiranUmum) {
		this.lampiranUmum = lampiranUmum;
	}

	/**
	 * Mengembalikan penanda apakah skema masih dipakai.
	 *
	 * <p><b>Bawaan condong-aktif dan menulis balik:</b> bila kolom masih {@code null} (baris
	 * warisan sebelum kolom ini ada, atau objek baru), method menetapkan {@code true} ke field
	 * lalu mengembalikannya. Dengan kata lain "belum diputuskan" diterjemahkan menjadi "aktif",
	 * bukan sebaliknya — pola gagal-terbuka yang lazim di master AIS dan perlu diingat saat
	 * memigrasi data.
	 *
	 * <p>Inilah satu-satunya penanda yang benar-benar ditegakkan pada jalur pengajuan: kriteria
	 * pemilihan skema di {@code PengajuanPenelitianDanPengabdianHelper} menyaring
	 * {@code Restrictions.eq("aktif", true)}. Karena penyaringan itu dilakukan dengan
	 * membandingkan kolom di basis data (bukan lewat getter ini), baris yang kolom
	 * {@code aktif}-nya masih {@code NULL} justru <i>tidak</i> lolos saringan meski getter ini
	 * melaporkannya aktif — ketidakcocokan halus antara aturan di Java dan aturan di SQL.
	 *
	 * @return {@code true} bila skema aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan penanda aktif skema. Menonaktifkan skema hanya menyembunyikannya dari daftar
	 * pilihan pengajuan; proposal yang sudah menunjuk skema ini tetap utuh dan tetap tampil.
	 *
	 * @param aktif {@code true} bila skema masih dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda "pendaftaran proposal sedang dibuka", dengan bawaan {@code true}
	 * bila kolom masih {@code null} (menulis balik ke field seperti {@link #getAktif()}).
	 *
	 * <p><b>Penanda ini tidak menjaga apa pun.</b> Penelusuran seluruh basis kode menemukan
	 * hanya tiga pemakaian: {@code PenelitianDanPengabdianAction} membacanya untuk mencentang
	 * kotak centang di layar admin dan menuliskannya kembali saat disimpan, serta
	 * {@code PengajuanPenelitianDanPengabdianHelper} menyetel {@code true} pada skema yang
	 * dibuatnya otomatis. Tidak ada satu pun jalur penyimpanan proposal yang memeriksa nilainya,
	 * dan kriteria pemilihan skema pada layar pengusul pun tidak menyertakannya (hanya
	 * {@code aktif}, {@code diperuntukkan}, dan {@code tipePenelitianDanPengabdian}).
	 *
	 * <p>Akibatnya, menutup pendaftaran lewat kotak centang ini tidak menghentikan pengajuan
	 * proposal baru — skema yang "ditutup" tetap muncul di combobox dan tetap bisa dipilih.
	 * Ini pengulangan pola gerbang UI-only yang sudah tercatat di beberapa domain lain AIS
	 * (kepegawaian, persuratan); yang menutup pendaftaran secara efektif hanyalah menonaktifkan
	 * skema lewat {@link #setAktif(Boolean)}.
	 *
	 * @return {@code true} bila pendaftaran dinyatakan dibuka; tidak pernah {@code null}
	 */
	public Boolean getDibuka() {
		if (dibuka == null) {
			dibuka = true;
		}
		return dibuka;
	}

	/**
	 * Menetapkan penanda buka/tutup pendaftaran. Lihat {@link #getDibuka()} untuk peringatan
	 * bahwa penanda ini bersifat informatif dan tidak menghalangi pengajuan baru.
	 *
	 * @param dibuka {@code true} bila pendaftaran dinyatakan dibuka
	 */
	public void setDibuka(Boolean dibuka) {
		this.dibuka = dibuka;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_penelitian_dan_pengabdian", nullable = true)
	public TipePenelitianDanPengabdian getTipePenelitianDanPengabdian() {
		tipePenelitianDanPengabdian = check(tipePenelitianDanPengabdian);
		return tipePenelitianDanPengabdian;
	}

	public void setTipePenelitianDanPengabdian(TipePenelitianDanPengabdian tipePenelitianDanPengabdian) {
		this.tipePenelitianDanPengabdian = tipePenelitianDanPengabdian;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalMulaiPengajuan() {
		if (tanggalMulaiPengajuan == null) {
			tanggalMulaiPengajuan = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalMulaiPengajuan;
	}

	public void setTanggalMulaiPengajuan(Date tanggalMulaiPengajuan) {
		this.tanggalMulaiPengajuan = tanggalMulaiPengajuan;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalSampaiPengajuan() {
		if (tanggalSampaiPengajuan == null) {
			tanggalSampaiPengajuan = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalSampaiPengajuan;
	}

	public void setTanggalSampaiPengajuan(Date tanggalSampaiPengajuan) {
		this.tanggalSampaiPengajuan = tanggalSampaiPengajuan;
	}

	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	public String getDiperuntukkan() {
		if (diperuntukkan == null) {
			diperuntukkan = PengumumanAkademis.UNTUK_UMUM;
		}
		return diperuntukkan;
	}

	public void setDiperuntukkan(String diperuntukkan) {
		this.diperuntukkan = diperuntukkan;
	}

	public Boolean getPublik() {
		if (publik == null) {
			publik = false;
		}
		return publik;
	}

	public void setPublik(Boolean publik) {
		this.publik = publik;
	}

	public String getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil(getTanggalMulaiPengajuan()) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return semester;
	}

	public void setSemester(String semester) {
		this.semester = semester;
	}

	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik(getTanggalMulaiPengajuan());
		}
		return tahunAkademik;
	}

	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	public Integer getSks() {
		return sks == null ? 0 : sks;
	}

	public void setSks(Integer sks) {
		this.sks = sks;
	}
}
