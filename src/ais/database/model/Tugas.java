package ais.database.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Group;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.profile.ProfileUtil;
import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyLabelAgakKecilBold;

/**
 * Model data untuk tugas. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String JSON}, {@code Tbmuser
 * currentUser}, {@code TugasFileContent currentTugasFileContent}; inisialisasi/lifecycle ({@code
 * reInitTugasFileContent()}, {@code reInitTugasFileContent()}); pembacaan/pencarian ({@code
 * getSyaratMengumpulkanTugas()}, {@code getFormatNilai()}, {@code getFormatNilais()}, {@code getProsentase()},
 * {@code getJudultugas()}, {@code getIsitugas()}); mutasi data ({@code setSyaratMengumpulkanTugas()}, {@code
 * setFormatNilai()}, {@code setFormatNilais()}, {@code setProsentase()}, {@code setJudultugas()}, {@code
 * setIsitugas()}); penghapusan/pembatalan ({@code removeTugasFileContent()}); operasi domain lain ({@code
 * tulisLokasiTugasFileContent()}, {@code bersihkanLokasiTugasFileContent()}, {@code populateTugasFileContent()},
 * {@code tugasFileContent()}, {@code apakahAkses()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
public abstract class Tugas extends GeneralValueObject {

	/**
	 * Cetakan awal dokumen JSON kosong ({@code "{}"}) yang dipakai sebagai nilai default berbagai
	 * kolom teks-JSON milik keluarga {@code Tugas} — terutama {@code syaratAkses} dan
	 * {@code keteranganNilai} pada subclass konkret.
	 *
	 * <p><b>Peringatan: field ini {@code public static} dan TIDAK {@code final}.</b> Nilainya
	 * dihitung sekali saat kelas dimuat, tetapi karena tidak final siapa pun dapat menimpanya dan
	 * perubahan itu berlaku untuk SELURUH JVM — termasuk seluruh tenant pada instalasi multi-tenant.
	 * Bila suatu saat ada kode yang menulis ke sini, seluruh entity yang belum pernah diisi akan
	 * mendadak memakai default yang berbeda. Perlakukan sebagai konstanta baca-saja; jangan menulis
	 * ke field ini, dan jangan mengandalkan identitas objeknya (bandingkan isinya, bukan
	 * referensinya).</p>
	 *
	 * <p>Nilai yang sama juga tersedia sebagai {@code VOMahasiswa.dataJSON}; keduanya dipakai
	 * bergantian di dalam kelas ini (lihat {@link #ambilLokasiTugasFileContent(Serializable, Class)}
	 * yang mengembalikan {@code VOMahasiswa.dataJSON}, bukan field ini) — perbedaan itu murni
	 * historis, bukan perbedaan makna.</p>
	 */
	public static String JSON = new JSONObject().toString();

	/**
	 * 
	 */
	/**
	 * Penanda versi serialisasi Java untuk keluarga {@code Tugas}.
	 *
	 * <p>Nilainya dikunci agar objek yang sudah pernah diserialisasi (misalnya ke dalam sesi ZK yang
	 * dipersistensi, atau ke cache lintas-node) tetap dapat dibaca setelah kelas ini diubah. Jangan
	 * mengganti angka ini hanya karena menambah field atau method; ganti hanya bila kontrak
	 * serialisasi memang sengaja diputus.</p>
	 */
	private static final long serialVersionUID = 5021212497292420069L;

	/**
	 * Aturan prasyarat yang harus dipenuhi peserta didik sebelum boleh mengumpulkan tugas ini.
	 *
	 * <p>Dideklarasikan abstrak karena setiap subclass konkret ({@link Pertemuan},
	 * {@link TugasPertemuan}, {@link TugasKelompok}) memetakan kolomnya sendiri lewat anotasi
	 * Hibernate pada tabelnya masing-masing. Kelas ini hanya menetapkan bahwa kontraknya ada,
	 * sehingga kode generik — misalnya {@link #tampilanSyarat} — dapat bekerja pada ketiganya tanpa
	 * mengetahui tipe konkretnya.</p>
	 *
	 * @return objek syarat ujian/pengumpulan, atau {@code null} bila tugas ini tidak memiliki
	 *         prasyarat berbentuk {@link SyaratUjian}
	 */
	public abstract SyaratUjian getSyaratMengumpulkanTugas();

	/**
	 * Menyetel aturan prasyarat pengumpulan tugas.
	 *
	 * @param syaratMengumpulkanTugas syarat baru, boleh {@code null} untuk mencabut prasyarat
	 * @see #getSyaratMengumpulkanTugas()
	 */
	public abstract void setSyaratMengumpulkanTugas(SyaratUjian syaratMengumpulkanTugas);

	/**
	 * Format penilaian tunggal yang dipakai untuk menilai pengumpulan tugas ini.
	 *
	 * <p>Berpasangan — dan sebagian tumpang tindih — dengan {@link #getFormatNilais()} yang menyimpan
	 * <i>beberapa</i> format sekaligus dalam bentuk teks. Bila keduanya terisi, perilaku yang
	 * berlaku ditentukan oleh helper penilaian di lapisan UI, bukan oleh kelas ini.</p>
	 *
	 * @return format nilai terpilih, atau {@code null} bila belum ditentukan
	 */
	public abstract FormatNilai getFormatNilai();

	/**
	 * Menyetel format penilaian tunggal untuk tugas ini.
	 *
	 * @param formatNilai format nilai baru, boleh {@code null}
	 * @see #getFormatNilai()
	 */
	public abstract void setFormatNilai(FormatNilai formatNilai);

	/**
	 * Daftar format penilaian dalam bentuk teks (biasanya JSON atau daftar id berpemisah).
	 *
	 * <p>Merupakan jalur "banyak format" yang berdampingan dengan {@link #getFormatNilai()} yang
	 * hanya menampung satu. Isinya diurai di lapisan helper penilaian; kelas ini memperlakukannya
	 * sebagai teks buram.</p>
	 *
	 * @return teks daftar format nilai, atau {@code null}/kosong bila belum diisi
	 */
	public abstract String getFormatNilais();

	/**
	 * Menyetel daftar format penilaian berbentuk teks.
	 *
	 * @param formatNilais teks daftar format nilai; tidak divalidasi oleh kelas ini
	 * @see #getFormatNilais()
	 */
	public abstract void setFormatNilais(String formatNilais);

	/**
	 * Bobot tugas ini terhadap nilai akhir, dinyatakan dalam persen.
	 *
	 * <p>Kelas ini tidak menjamin bahwa jumlah bobot seluruh komponen dalam satu pembelajaran sama
	 * dengan 100; penjumlahan dan validasinya dilakukan di lapisan helper penilaian.</p>
	 *
	 * @return bobot dalam persen, atau {@code null} bila belum ditentukan
	 */
	public abstract Double getProsentase();

	/**
	 * Menyetel bobot tugas ini terhadap nilai akhir.
	 *
	 * @param prosentase bobot dalam persen; boleh {@code null}, tidak divalidasi di sini
	 * @see #getProsentase()
	 */
	public abstract void setProsentase(Double prosentase);

	/**
	 * Judul tugas sebagaimana ditampilkan kepada peserta didik.
	 *
	 * @return judul tugas, atau {@code null} bila belum diisi
	 */
	public abstract String getJudultugas();

	/**
	 * Menyetel judul tugas.
	 *
	 * @param judultugas judul baru
	 * @see #getJudultugas()
	 */
	public abstract void setJudultugas(String judultugas);

	/**
	 * Menyetel isi/uraian tugas.
	 *
	 * @param isitugas uraian tugas, umumnya berupa HTML hasil editor kaya di UI
	 * @see #getIsitugas()
	 */
	public abstract void setIsitugas(String isitugas);

	/**
	 * Isi/uraian tugas yang dibaca peserta didik.
	 *
	 * <p>Umumnya berisi HTML hasil editor kaya. Kelas ini tidak melakukan sanitasi apa pun — penyaji
	 * yang menuliskannya ke halaman bertanggung jawab menghindari XSS tersimpan.</p>
	 *
	 * @return uraian tugas, atau {@code null} bila belum diisi
	 */
	public abstract String getIsitugas();

	/**
	 * Waktu mulai jendela pengumpulan tugas.
	 *
	 * <p>Berpasangan dengan {@link #getSelesai()}. Penegakan jendela waktu TIDAK dilakukan di kelas
	 * ini; pemeriksaannya ada di helper/action yang menerima unggahan, sehingga jalur unggah baru
	 * harus memeriksanya sendiri.</p>
	 *
	 * @return waktu mulai, atau {@code null} bila tanpa batas awal
	 */
	public abstract Date getMulai();

	/**
	 * Menyetel waktu mulai jendela pengumpulan tugas.
	 *
	 * @param mulai waktu mulai; boleh {@code null}
	 * @see #getMulai()
	 */
	public abstract void setMulai(Date mulai);

	/**
	 * Waktu berakhir jendela pengumpulan tugas (tenggat).
	 *
	 * @return waktu berakhir, atau {@code null} bila tanpa tenggat
	 * @see #getMulai()
	 */
	public abstract Date getSelesai();

	/**
	 * Menyetel waktu berakhir jendela pengumpulan tugas.
	 *
	 * @param selesai tenggat baru; boleh {@code null}
	 * @see #getSelesai()
	 */
	public abstract void setSelesai(Date selesai);

	/**
	 * Daftar peserta didik yang dikecualikan dari tugas ini, disimpan sebagai satu string berpemisah
	 * koma dengan koma pembuka dan penutup (bentuk {@code ",12,45,78,"}).
	 *
	 * <p><b>Bentuk penyimpanan ini penting dipahami</b> karena dipakai langsung oleh
	 * {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)} lewat pemeriksaan
	 * {@code getMhsYgTidakIkut().contains("," + id + ",")}. Konsekuensinya:</p>
	 * <ul>
	 *   <li>Getter ini <b>tidak boleh mengembalikan {@code null}</b> — subclass wajib mengembalikan
	 *       minimal string kosong, karena pemanggil di atas tidak melakukan penjagaan null. Bila
	 *       suatu subclass mengembalikan {@code null}, seluruh pembacaan daftar pengumpulan tugas
	 *       akan gagal dengan {@code NullPointerException}.</li>
	 *   <li>Koma pembuka/penutup wajib ada, jika tidak entri pertama dan terakhir tidak akan pernah
	 *       cocok.</li>
	 *   <li><b>Id yang disimpan tidak membawa penanda jenis orang.</b> Nilai yang dicocokkan
	 *       ({@code id}) dapat berasal dari {@code Mahasiswa}, {@code BiodataCalonMahasiswa},
	 *       {@code Siswa}, atau {@code CalonSiswa} — empat tabel dengan urutan id yang berdiri
	 *       sendiri. Mengecualikan mahasiswa ber-id 45 karena itu juga mengecualikan siswa ber-id 45
	 *       dari tugas yang sama. Ini varian dari pola tabrakan id lintas-tabel yang berulang di
	 *       basis kode ini.</li>
	 * </ul>
	 *
	 * @return daftar id yang dikecualikan dalam bentuk {@code ",id,id,"}; jangan mengembalikan
	 *         {@code null}
	 */
	public abstract String getMhsYgTidakIkut();

	/**
	 * Daftar peserta didik yang secara khusus diizinkan mengunggah ulang jawabannya, memakai bentuk
	 * string berpemisah koma yang sama dengan {@link #getMhsYgTidakIkut()}.
	 *
	 * <p>Dipakai oleh helper unggah untuk membuka kembali kesempatan pengumpulan bagi orang tertentu
	 * tanpa membuka tugas untuk semua orang. Penegakannya berada di luar kelas ini, dan karena id
	 * yang disimpan tidak membawa penanda jenis orang, kelemahan tabrakan id lintas-tabel yang
	 * dijelaskan pada {@link #getMhsYgTidakIkut()} berlaku sama persis di sini — dengan akibat yang
	 * lebih peka, karena yang diberikan adalah izin, bukan pengecualian.</p>
	 *
	 * @return daftar id yang boleh mengunggah ulang dalam bentuk {@code ",id,id,"}
	 */
	public abstract String getMhsBolehUploadUlang();

	/**
	 * Menyetel daftar peserta didik yang dikecualikan dari tugas ini.
	 *
	 * @param mhsYgTidakIkut daftar id dalam bentuk {@code ",id,id,"}; penulis wajib menjaga koma
	 *        pembuka/penutup dan tidak boleh mengirim {@code null}
	 * @see #getMhsYgTidakIkut()
	 */
	public abstract void setMhsYgTidakIkut(String mhsYgTidakIkut);

	/**
	 * Menyetel daftar peserta didik yang diizinkan mengunggah ulang.
	 *
	 * @param mhsBolehUploadUlang daftar id dalam bentuk {@code ",id,id,"}
	 * @see #getMhsBolehUploadUlang()
	 */
	public abstract void setMhsBolehUploadUlang(String mhsBolehUploadUlang);

	/**
	 * Prasyarat akses tugas ini dalam bentuk dokumen JSON.
	 *
	 * <p>Strukturnya adalah peta datar: kunci {@code "<id>_<NamaKelasSingkat>"} dan nilai
	 * {@code "<nama.kelas.penuh>_<id>"}. Setiap entri berarti "objek pembelajaran ini harus sudah
	 * dikerjakan/diakses sebelum tugas ini boleh dikumpulkan". Entri ditulis dan dihapus oleh
	 * listener checkbox di {@link #tampilanSyarat} dan {@link #tampilanLain}.</p>
	 *
	 * <p><b>Getter ini tidak boleh mengembalikan {@code null}</b>: hampir semua pemanggil membungkus
	 * hasilnya langsung dengan {@code new JSONObject(getSyaratAkses())}, yang akan melempar
	 * {@code JSONException} pada {@code null} atau string kosong. Subclass harus mengembalikan
	 * setidaknya {@code "{}"} — lihat {@link #JSON}.</p>
	 *
	 * @return dokumen JSON prasyarat akses; minimal {@code "{}"}
	 */
	public abstract String getSyaratAkses();

	/**
	 * Menyetel prasyarat akses tugas ini.
	 *
	 * @param syaratAkses dokumen JSON prasyarat; kirim {@code "{}"} untuk mengosongkan, jangan
	 *        {@code null}
	 * @see #getSyaratAkses()
	 */
	public abstract void setSyaratAkses(String syaratAkses);

	/**
	 * Jenis item penilaian (domain sekolah) tempat nilai tugas ini dibukukan.
	 *
	 * <p>Bagian dari tiga serangkai pemetaan nilai ke rapor sekolah bersama
	 * {@link #getGrupKategoriItemPenilaianSiswa()} dan {@link #getGrupPenilaian()}. Hanya relevan
	 * untuk instalasi yang memakai modul sekolah; pada instalasi perguruan tinggi umumnya
	 * {@code null}.</p>
	 *
	 * @return jenis item penilaian siswa, atau {@code null}
	 */
	public abstract JenisItemPenilaianSiswa getJenisItemPenilaianSiswa();

	/**
	 * Menyetel jenis item penilaian siswa untuk tugas ini.
	 *
	 * @param jenisItemPenilaianSiswa jenis item penilaian; boleh {@code null}
	 * @see #getJenisItemPenilaianSiswa()
	 */
	public abstract void setJenisItemPenilaianSiswa(JenisItemPenilaianSiswa jenisItemPenilaianSiswa);

	/**
	 * Grup kategori item penilaian (domain sekolah) yang menaungi nilai tugas ini.
	 *
	 * @return grup kategori item penilaian siswa, atau {@code null}
	 * @see #getJenisItemPenilaianSiswa()
	 */
	public abstract GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa();

	/**
	 * Menyetel grup kategori item penilaian siswa untuk tugas ini.
	 *
	 * @param grupKategoriItemPenilaianSiswa grup kategori; boleh {@code null}
	 * @see #getGrupKategoriItemPenilaianSiswa()
	 */
	public abstract void setGrupKategoriItemPenilaianSiswa(
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa);

	/**
	 * Grup penilaian (domain sekolah) tempat tugas ini dikelompokkan pada rapor.
	 *
	 * @return grup penilaian, atau {@code null}
	 * @see #getJenisItemPenilaianSiswa()
	 */
	public abstract GrupPenilaian getGrupPenilaian();

	/**
	 * Menyetel grup penilaian untuk tugas ini.
	 *
	 * @param grupPenilaian grup penilaian; boleh {@code null}
	 * @see #getGrupPenilaian()
	 */
	public abstract void setGrupPenilaian(GrupPenilaian grupPenilaian);

	/**
	 * Nilai dan keterangan per peserta didik untuk tugas ini, disimpan sebagai satu dokumen JSON
	 * pada baris tugas — bukan pada baris pengumpulan masing-masing peserta.
	 *
	 * <p>Kuncinya dibentuk dari id peserta ditambah akhiran jenis orang, lalu ditambah akhiran
	 * bidang: {@code "<id>_mhs_nilai"}, {@code "<id>_mhs_ket"}, {@code "<id>_siswa_nilai"},
	 * {@code "<id>_cal_mhs_ket"}, {@code "<id>_cal_siswa_nilai"}, dan seterusnya. Berbeda dengan
	 * {@link #getMhsYgTidakIkut()}, di sini jenis orang <i>ikut</i> masuk ke dalam kunci, sehingga
	 * jalur ini tidak menderita tabrakan id lintas-tabel.</p>
	 *
	 * <p><b>Ini adalah field bayangan terhadap kolom nilai pada {@link TugasFileContent}.</b>
	 * {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)} membaca dokumen ini
	 * lalu menimpa {@code nilai} dan {@code keterangan} pada objek {@code TugasFileContent} yang
	 * baru dimuat. Artinya nilai yang terlihat di UI berasal dari sini, bukan dari kolom pada baris
	 * pengumpulan. Kode yang membaca {@code TugasFileContent} langsung dari basis data — tanpa
	 * melewati method tersebut — berpotensi melihat nilai yang usang atau kosong.</p>
	 *
	 * <p>Seperti {@link #getSyaratAkses()}, getter ini diurai dengan {@code new JSONObject(...)} dan
	 * karenanya tidak boleh mengembalikan {@code null} atau string kosong.</p>
	 *
	 * @return dokumen JSON nilai dan keterangan per peserta; minimal {@code "{}"}
	 */
	public abstract String getKeteranganNilai();

	/**
	 * Menyetel dokumen JSON nilai dan keterangan per peserta didik.
	 *
	 * <p>Karena satu dokumen menampung nilai SELURUH peserta, penulisan harus dilakukan dengan pola
	 * baca-ubah-tulis pada dokumen yang sama. Dua penilai yang menyimpan nilai untuk dua peserta
	 * berbeda secara bersamaan akan saling menimpa — yang menyimpan belakangan menghapus pekerjaan
	 * yang menyimpan lebih dulu. Tidak ada penguncian di lapisan model.</p>
	 *
	 * @param keteranganNilai dokumen JSON; kirim {@code "{}"} untuk mengosongkan, jangan {@code null}
	 * @see #getKeteranganNilai()
	 */
	public abstract void setKeteranganNilai(String keteranganNilai);

	/**
	 * Membaca indeks pengumpulan tugas milik satu objek pembelajaran dari berkas cache di luar basis
	 * data, dan mengembalikan isinya sebagai teks JSON mentah.
	 *
	 * <p>Keluarga {@code Tugas} tidak memuat daftar pengumpulan lewat relasi Hibernate. Sebagai
	 * gantinya dipakai satu berkas per objek pembelajaran yang berisi peta datar
	 * {@code {"<idPeserta>": <idTugasFileContent>}}. Lokasi berkas ditentukan oleh
	 * {@code Common.getFileLocation(clazz, id, "tugas_file_content_" + id)}, jadi identitas indeks
	 * ditentukan oleh <b>pasangan</b> {@code (clazz, id)} — bukan oleh {@code id} saja. Ini yang
	 * mencegah {@code Pertemuan} ber-id 42 dan {@code TugasKelompok} ber-id 42 berbagi berkas yang
	 * sama, meskipun keduanya berada di tabel berbeda dengan urutan id yang berdiri sendiri.</p>
	 *
	 * <p><b>Gagal selalu berarti "kosong", tidak pernah berarti "error".</b> Argumen {@code null},
	 * berkas tidak ada, berkas kosong, atau kegagalan I/O apa pun sama-sama menghasilkan
	 * {@code VOMahasiswa.dataJSON} (yaitu {@code "{}"}). Konsekuensinya penting untuk diketahui
	 * pemanggil: <i>tidak ada cara membedakan "tugas ini memang belum ada yang mengumpulkan" dari
	 * "berkas indeksnya hilang atau tidak terbaca"</i>. Bila cache terhapus di tingkat sistem berkas,
	 * seluruh pengumpulan akan tampak lenyap padahal barisnya masih utuh di basis data — sampai
	 * {@link #reInitTugasFileContent(Session)} dijalankan untuk membangunnya kembali. Karena itu
	 * jangan pernah memakai hasil kosong dari method ini sebagai dasar untuk menghapus data atau
	 * memberi nilai nol.</p>
	 *
	 * <p>Nilai balik adalah teks JSON yang <b>belum diurai dan belum divalidasi</b>. Pemanggil wajib
	 * memeriksa sendiri bahwa isinya benar-benar sebuah objek JSON sebelum membungkusnya dengan
	 * {@code new JSONObject(...)}; lihat penjagaan awalan {@code "{"} di
	 * {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)}.</p>
	 *
	 * <p><b>Tanpa pemeriksaan otorisasi.</b> Siapa pun yang dapat menyebut sepasang {@code (clazz,
	 * id)} dapat membaca seluruh indeks pengumpulan objek pembelajaran itu, lintas kelas dan lintas
	 * tenant. Pemeriksaan hak akses harus sudah dilakukan di lapisan action/helper sebelum method ini
	 * dipanggil.</p>
	 *
	 * @param id id objek pembelajaran pemilik indeks; {@code null} menghasilkan {@code "{}"}
	 * @param clazz kelas pemilik indeks — menentukan direktori berkas, jadi harus kelas yang sama
	 *        dengan yang dipakai saat menulis. Perhatikan bahwa memanggil dengan
	 *        {@code getClass()} pada entity berproksi Hibernate akan menghasilkan kelas proksi;
	 *        gunakan kelas yang konsisten untuk baca dan tulis.
	 * @return teks JSON indeks pengumpulan, atau {@code "{}"} bila tidak ada/gagal dibaca; tidak
	 *         pernah {@code null}
	 * @see #tulisLokasiTugasFileContent(String, Serializable, Class)
	 * @see #reInitTugasFileContent(Session)
	 */
	@SuppressWarnings("rawtypes")
	public static String ambilLokasiTugasFileContent(Serializable id, Class clazz) {
		if (id == null || clazz == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(clazz, id, "tugas_file_content_" + id.toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:120");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas indeks pengumpulan tugas milik satu objek pembelajaran dengan teks yang
	 * diberikan.
	 *
	 * <p>Pasangan tulis dari {@link #ambilLokasiTugasFileContent(Serializable, Class)}, memakai
	 * penurunan lokasi berkas yang persis sama. Penulisan bersifat <b>menimpa seluruh isi</b>, bukan
	 * menambah; pemanggil bertanggung jawab melakukan baca-ubah-tulis bila hanya ingin mengubah satu
	 * entri (lihat {@link #populateTugasFileContent(TugasFileContent, Class, boolean)} dan
	 * {@link #removeTugasFileContent(Serializable, Class)} yang keduanya melakukan itu).</p>
	 *
	 * <p><b>Tidak atomik dan tidak terkunci.</b> Dua thread yang memperbarui indeks objek
	 * pembelajaran yang sama pada saat bersamaan akan saling menimpa: masing-masing membaca dokumen
	 * lama, menambahkan entrinya sendiri, lalu menulis ulang seluruh dokumen — sehingga entri yang
	 * ditulis lebih dulu hilang. Pada tugas yang dikumpulkan serentak oleh banyak peserta menjelang
	 * tenggat, kondisi ini bukan hal teoretis. Akibatnya terbatas pada indeks cache, bukan basis
	 * data: entri yang hilang dapat dipulihkan dengan {@link #reInitTugasFileContent(Session)}, dan
	 * baris {@link TugasFileContent} yang sesungguhnya tetap utuh.</p>
	 *
	 * <p>Kegagalan I/O ditelan (hanya dicatat ke audit error) dan method tetap kembali normal.
	 * Pemanggil tidak akan tahu bila penulisan gagal.</p>
	 *
	 * @param data teks JSON lengkap yang akan menggantikan isi berkas
	 * @param id id objek pembelajaran pemilik indeks; tidak boleh {@code null}, berbeda dengan
	 *        pasangan bacanya method ini akan melempar {@code NullPointerException}
	 * @param clazz kelas pemilik indeks; harus konsisten dengan yang dipakai saat membaca
	 * @see #ambilLokasiTugasFileContent(Serializable, Class)
	 */
	@SuppressWarnings("rawtypes")
	public static void tulisLokasiTugasFileContent(String data, Serializable id, Class clazz) {
		File file = Common.getFileLocation(clazz, id, "tugas_file_content_" + id.toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:130");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menghapus berkas indeks pengumpulan tugas milik entity ini dari sistem berkas.
	 *
	 * <p>Hanya menyentuh cache; tidak ada satu pun baris {@link TugasFileContent} di basis data yang
	 * dihapus. Dipanggil sebagai langkah pertama {@link #reInitTugasFileContent(Session)} sebelum
	 * indeks dibangun ulang dari basis data.</p>
	 *
	 * <p>Berbeda dengan sepasang method statis di atas yang menerima {@code (id, clazz)} secara
	 * eksplisit, method ini menurunkan lokasi berkas dari {@code this} lewat
	 * {@code Common.getFileLocation(this, ...)}. Pastikan pemakaian {@code this} dan pemakaian
	 * {@code getClass()} pada method statis menghasilkan lokasi yang sama untuk entity yang sama —
	 * bila entity sedang berupa proksi Hibernate, keduanya harus tetap sepakat, jika tidak yang
	 * dihapus adalah berkas yang berbeda dari yang ditulis.</p>
	 *
	 * <p>Memerlukan {@code getId() != null}; pada entity yang belum tersimpan akan melempar
	 * {@code NullPointerException}.</p>
	 */
	public void bersihkanLokasiTugasFileContent() {
		File file = Common.getFileLocation(this, "tugas_file_content_" + getId().toString());
		BacaTulisUtil.doHapus(file, "tugas_file_content");

	}

	/**
	 * Membangun ulang indeks pengumpulan tugas entity ini memakai session Hibernate streaming yang
	 * dikelola sendiri.
	 *
	 * <p>Pembungkus praktis atas {@link #reInitTugasFileContent(Session)}: membuka session dari
	 * {@code StreamingHibernateUtil}, mendelegasikan pekerjaan, lalu menutup session itu.</p>
	 *
	 * <p><b>Jangan memanggil versi ini bila sudah berada di dalam sebuah unit kerja.</b> Method ini
	 * menutup session streaming pada akhir eksekusinya. Bila pemanggil masih memegang session yang
	 * sama untuk pekerjaan lain — misalnya sedang di tengah transaksi — session itu akan tertutup di
	 * bawah kakinya dan operasi berikutnya gagal. Dalam alur seperti itu panggil
	 * {@link #reInitTugasFileContent(Session)} dengan session milik pemanggil.</p>
	 *
	 * @see #reInitTugasFileContent(Session)
	 */
	public void reInitTugasFileContent() {
		Session session = StreamingHibernateUtil.getInstance().currentSession();
		reInitTugasFileContent(session);
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/**
	 * Membangun ulang berkas indeks pengumpulan tugas entity ini dari basis data, menggantikan isi
	 * cache lama sepenuhnya.
	 *
	 * <p>Alurnya: menyusun penyaring pemilik, mengambil seluruh {@link TugasFileContent} yang cocok,
	 * menghapus berkas indeks lama, menulis dokumen kosong, lalu memasukkan kembali setiap baris
	 * lewat {@link #populateTugasFileContent(TugasFileContent, Class, boolean)}.</p>
	 *
	 * <h3>Bagaimana kepemilikan sebuah pengumpulan ditentukan</h3>
	 * <p>{@link TugasFileContent} <b>tidak memiliki relasi Hibernate ke pemiliknya</b>. Yang ada
	 * hanyalah dua kolom datar:</p>
	 * <ul>
	 *   <li>{@code pertemuan} — sebuah {@code Long} berisi id pemilik. Meskipun namanya
	 *       {@code pertemuan}, isinya <b>belum tentu id sebuah {@link Pertemuan}</b>: bisa id
	 *       {@link TugasPertemuan} atau {@link TugasKelompok}, tergantung objek pembelajaran mana
	 *       yang menaungi pengumpulan itu. Perhatikan baris
	 *       {@code Restrictions.eq("pertemuan", this.getId())} — yang dimasukkan adalah id
	 *       {@code this}, apa pun subclass-nya.</li>
	 *   <li>{@code class_from} — nama kelas Java pemiliknya, yang menjadi satu-satunya pembeda
	 *       ketika angka pada kolom {@code pertemuan} kebetulan sama.</li>
	 * </ul>
	 * <p>Karena {@code Pertemuan}, {@code TugasPertemuan}, dan {@code TugasKelompok} berada di tabel
	 * berbeda dengan urutan id yang berdiri sendiri, angka id 42 dapat menunjuk ketiganya sekaligus.
	 * {@code class_from} adalah satu-satunya hal yang memisahkan mereka.</p>
	 *
	 * <h3>Ketimpangan penyaring: cabang TugasPertemuan ketat, cabang lain longgar</h3>
	 * <p>Penyaring yang disusun berbeda untuk kedua cabang:</p>
	 * <ul>
	 *   <li>Bila {@code this instanceof TugasPertemuan}, penyaringnya
	 *       {@code class_from ilike 'ais.database.model.TugasPertemuan%'} — <b>ketat</b>: baris
	 *       dengan {@code class_from} bernilai {@code NULL} tidak ikut terambil.</li>
	 *   <li>Untuk selain itu ({@link Pertemuan} dan {@link TugasKelompok}), penyaringnya
	 *       {@code (class_from is null or class_from ilike '<namaKelas>%')} — <b>longgar</b>: baris
	 *       ber-{@code class_from} {@code NULL} ikut terambil semata-mata karena angka pada kolom
	 *       {@code pertemuan} cocok.</li>
	 * </ul>
	 * <p>Inilah varian pola tabrakan id anak-versus-induk pada kelas ini. Sebuah baris
	 * {@link TugasFileContent} lama yang {@code class_from}-nya belum terisi dan {@code pertemuan}-nya
	 * bernilai 42 akan diakui sebagai milik {@code Pertemuan} ber-id 42 <i>dan</i> milik
	 * {@code TugasKelompok} ber-id 42 — keduanya akan memasukkannya ke indeksnya masing-masing.
	 * Akibatnya pengumpulan seorang peserta dapat muncul pada objek pembelajaran yang bukan
	 * tujuannya, dan ikut terhitung pada {@link #ambilJumlahTugasFileContent()} di sana. Perlu
	 * dicatat bahwa {@code TugasFileContent} berusaha menutup celah ini dari sisi tulis — konstruktor
	 * defaultnya mengisi {@code classFrom} dengan {@code Pertemuan.class.getName()} dan getter-nya
	 * memulihkan nilai {@code null} menjadi nilai yang sama — sehingga baris yang ditulis lewat jalur
	 * normal aman. Yang berisiko adalah baris warisan yang sudah terlanjur {@code NULL} di basis
	 * data, karena penyaring di sini dijalankan sebagai SQL mentah dan tidak melewati getter
	 * tersebut.</p>
	 *
	 * <h3>Pembersihan nama kelas berproksi</h3>
	 * <p>{@code StringUtils.split(getClass().getName(), "_")[0]} memotong nama kelas pada garis bawah
	 * pertama. Tujuannya membuang akhiran proksi Javassist/CGLIB (bentuk
	 * {@code ...Pertemuan_$$_javassist_123}) agar penyaring tetap mengenai kelas aslinya. Efek
	 * sampingnya: seandainya ada kelas domain yang namanya sendiri memuat garis bawah, namanya akan
	 * ikut terpotong.</p>
	 *
	 * <h3>Perbandingan dengan awalan, bukan kesamaan</h3>
	 * <p>Penyaring memakai {@code ilike '<nama>%'}, bukan kesamaan persis. Nama kelas yang merupakan
	 * awalan dari nama kelas lain akan saling menyerap — {@code TugasKelompok} akan cocok dengan
	 * {@code class_from} bernilai {@code TugasKelompokApaPun}. Saat ini tidak ada pasangan nama
	 * seperti itu di paket ini, tetapi penambahan kelas baru dengan awalan yang sama akan
	 * memunculkannya tanpa peringatan.</p>
	 *
	 * <h3>Biaya dan efek samping</h3>
	 * <p>Seluruh baris pengumpulan dimuat ke memori sekaligus tanpa paging, lalu berkas indeks
	 * ditulis ulang satu kali per baris. Untuk tugas dengan ratusan peserta ini berarti ratusan
	 * operasi baca-ubah-tulis berkas. Di antara penghapusan berkas lama dan selesainya perulangan,
	 * indeks berada dalam keadaan tidak lengkap — pembaca lain pada saat itu akan melihat lebih
	 * sedikit pengumpulan daripada yang sebenarnya ada. Tidak ada penguncian yang mencegahnya.</p>
	 *
	 * @param session session Hibernate yang dipakai untuk kueri; dimiliki pemanggil dan tidak
	 *        ditutup oleh method ini
	 * @see #ambilLokasiTugasFileContent(Serializable, Class)
	 * @see #populateTugasFileContent(TugasFileContent, Class, boolean)
	 */
	@SuppressWarnings("unchecked")
	public void reInitTugasFileContent(Session session) {
		String sqlTambahan = "";
		if (this instanceof TugasPertemuan) {
			sqlTambahan = "class_from ilike '" + TugasPertemuan.class.getName() + "%'";
		} else {
			String clazzs = StringUtils.split(getClass().getName(), "_")[0];
			sqlTambahan = "(class_from is null or class_from ilike '" + clazzs + "%')";
		}
//		System.out.println("sqlTambahan => " + sqlTambahan);
		List<TugasFileContent> tugasFileContents = session.createCriteria(TugasFileContent.class)
				.add(Restrictions.sqlRestriction(sqlTambahan)).addOrder(Order.asc("id"))
				.add(Restrictions.eq("pertemuan", this.getId())).list();
		bersihkanLokasiTugasFileContent();
		tulisLokasiTugasFileContent(new JSONObject().toString(), getId(), getClass());
		for (TugasFileContent tugasFileContent : tugasFileContents) {
			populateTugasFileContent(tugasFileContent, getClass(), true);
		}
		tugasFileContents.clear();
		tugasFileContents = null;
	}

	/**
	 * Mengosongkan satu entri pada berkas indeks pengumpulan tugas.
	 *
	 * <p>Melakukan baca-ubah-tulis: memuat dokumen indeks, menyetel entri berkunci {@code id} menjadi
	 * string kosong, lalu menulis ulang seluruh dokumen. Entri tidak dihapus melainkan dikosongkan;
	 * pembacaan berikutnya melewatinya karena
	 * {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)} mengabaikan nilai
	 * kosong.</p>
	 *
	 * <p><b>Perhatikan makna parameter yang berbeda dari method lain sekeluarga.</b> Pada
	 * {@link #ambilLokasiTugasFileContent(Serializable, Class)} dan
	 * {@link #tulisLokasiTugasFileContent(String, Serializable, Class)}, {@code id} adalah id objek
	 * pembelajaran <i>pemilik indeks</i>. Di sini {@code id} dipakai untuk <b>dua peran sekaligus</b>:
	 * sebagai penentu berkas indeks yang dibuka <i>dan</i> sebagai kunci entri yang dikosongkan di
	 * dalamnya. Padahal kunci di dalam dokumen adalah id <i>peserta didik</i>, bukan id objek
	 * pembelajaran. Kedua nilai itu hanya kebetulan sama ketika angkanya bertabrakan.</p>
	 * <p>Dengan kata lain, memanggil {@code removeTugasFileContent(42L, Pertemuan.class)} membuka
	 * indeks milik pertemuan 42 lalu mengosongkan entri milik <i>peserta</i> 42 di dalamnya. Method
	 * ini hanya masuk akal bila pemanggil memang bermaksud demikian. Untuk sekadar membuang satu
	 * pengumpulan dari indeks, lebih aman membangun ulang indeks lewat
	 * {@link #reInitTugasFileContent(Session)} setelah barisnya dihapus di basis data.</p>
	 *
	 * <p>Tidak menghapus apa pun di basis data, dan seluruh kegagalan ditelan tanpa memberi tahu
	 * pemanggil.</p>
	 *
	 * @param id id yang berperan ganda seperti diuraikan di atas
	 * @param clazz kelas pemilik indeks
	 */
	@SuppressWarnings("rawtypes")
	public static void removeTugasFileContent(Serializable id, Class clazz) {
		try {
			JSONObject c = new JSONObject(ambilLokasiTugasFileContent(id, clazz));
			c.put(id.toString(), "");
			tulisLokasiTugasFileContent(c.toString(), id, clazz);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:176");

		}
	}

	/**
	 * Mendaftarkan satu baris pengumpulan ke dalam berkas indeks milik objek pembelajaran yang
	 * menaunginya.
	 *
	 * <p>Menulis entri {@code {"<idPeserta>": <idTugasFileContent>}} lewat baca-ubah-tulis pada
	 * dokumen indeks. Berkas yang dibuka ditentukan oleh {@code tugasFileContent.getPertemuan()}
	 * bersama {@code clazz} — jadi baris tersebut harus sudah membawa id pemilik yang benar sebelum
	 * method ini dipanggil.</p>
	 *
	 * <h3>Bagaimana identitas peserta didik dipilih</h3>
	 * <p>Satu baris {@link TugasFileContent} memiliki empat kolom identitas peserta yang saling
	 * eksklusif — {@code mahasiswa}, {@code biodataCalonMahasiswa}, {@code siswa}, dan
	 * {@code calonSiswa} — dan hanya satu angka yang dipakai sebagai kunci. Urutan pemilihannya
	 * berlapis: mulai dari {@code mahasiswa} bila terisi dan positif, jatuh ke
	 * {@code biodataCalonMahasiswa} bila tidak, kemudian <b>ditimpa</b> oleh {@code siswa} bila
	 * terisi dan positif, lalu ditimpa lagi oleh {@code calonSiswa} bila terisi dan positif. Jadi
	 * prioritas efektifnya adalah calonSiswa &gt; siswa &gt; mahasiswa &gt; biodataCalonMahasiswa,
	 * bukan urutan penulisan kodenya. Urutan berlapis yang sama diulang di tiga tempat lain pada
	 * kelas ini; bila salah satu diubah, semuanya harus ikut diubah agar tidak berbeda perilaku.</p>
	 *
	 * <p><b>Kunci indeks hanya berupa angka, tanpa penanda jenis orang.</b> Berbeda dengan
	 * {@link #getKeteranganNilai()} yang kuncinya memuat akhiran {@code _mhs}/{@code _siswa}/
	 * {@code _cal_mhs}/{@code _cal_siswa}, indeks ini menyimpan angka telanjang. Bila pada satu objek
	 * pembelajaran terdapat pengumpulan dari seorang mahasiswa ber-id 30 dan seorang siswa ber-id 30,
	 * keduanya menempati kunci yang sama dan yang didaftarkan belakangan menghapus yang lebih dulu
	 * dari indeks. Barisnya tetap ada di basis data, tetapi satu di antaranya menjadi tidak terlihat
	 * sampai indeks dibangun ulang — dan pembangunan ulang akan menghasilkan hasil yang sama.
	 * Instalasi yang memakai modul perguruan tinggi dan modul sekolah sekaligus paling terpapar
	 * keadaan ini.</p>
	 *
	 * <p>Bila keempat kolom identitas kosong, {@code id} bernilai {@code null} dan
	 * {@code id.toString()} melempar {@code NullPointerException} yang langsung ditelan blok
	 * {@code catch} — baris itu diam-diam tidak masuk indeks.</p>
	 *
	 * @param tugasFileContent baris pengumpulan yang akan didaftarkan; {@code null} diabaikan
	 * @param clazz kelas objek pembelajaran pemilik indeks
	 * @param tulisUlang <b>parameter mati</b> — tidak pernah dibaca di dalam badan method. Nilai apa
	 *        pun yang dikirim tidak berpengaruh. Dipertahankan demi tanda tangan yang sudah dipakai
	 *        pemanggil.
	 */
	public static void populateTugasFileContent(TugasFileContent tugasFileContent,
			@SuppressWarnings("rawtypes") Class clazz, boolean tulisUlang) {
		try {
			if (tugasFileContent == null) {
				return;
			}

			Long id = tugasFileContent.getMahasiswa() != null && tugasFileContent.getMahasiswa() > 0L
					? tugasFileContent.getMahasiswa()
					: tugasFileContent.getBiodataCalonMahasiswa();

			if (tugasFileContent.getSiswa() != null && tugasFileContent.getSiswa() > 0L) {
				id = tugasFileContent.getSiswa();
			}
			if (tugasFileContent.getCalonSiswa() != null && tugasFileContent.getCalonSiswa() > 0L) {
				id = tugasFileContent.getCalonSiswa();
			}

			JSONObject c = new JSONObject(ambilLokasiTugasFileContent(tugasFileContent.getPertemuan(), clazz));
			c.put(id.toString(), tugasFileContent.getId());
			tulisLokasiTugasFileContent(c.toString(), tugasFileContent.getPertemuan(), clazz);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:202");
		}
	}

	/**
	 * Menyatakan apakah sudah ada setidaknya satu pengumpulan untuk tugas ini.
	 *
	 * <p>Meskipun namanya menyerupai getter properti — dan karenanya mudah dikira murah — method ini
	 * <b>membangun seluruh peta pengumpulan lebih dahulu</b> lewat
	 * {@link #ambilTugasFileContentTotal()}, lalu hanya melihat ukurannya. Untuk tugas dengan banyak
	 * peserta ini berarti membaca berkas indeks dan memuat setiap baris {@link TugasFileContent} satu
	 * per satu, hanya untuk menjawab sebuah pertanyaan ya/tidak. Jangan memanggilnya di dalam
	 * perulangan atau di dalam penyaji baris daftar.</p>
	 *
	 * <p>Ikut mewarisi efek samping {@link #ambilTugasFileContentTotal()}, yaitu pengosongan field
	 * {@link #currentUser} dan penulisan ulang {@link #currentTugasFileContent}. Memanggil method ini
	 * di tengah alur yang mengandalkan kedua field itu akan merusak keadaan yang sedang dipakai.</p>
	 *
	 * @return {@code true} bila ada minimal satu pengumpulan yang terdaftar di indeks
	 */
	public boolean tugasFileContent() {

		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int ada = tugasFileContentsa.size();
		tugasFileContentsa = null;
		return ada > 0;
	}
	
	/**
	 * Menghitung berapa berkas yang sudah dikumpulkan oleh seorang siswa untuk tugas ini.
	 *
	 * <p>Memuat seluruh peta pengumpulan tugas ini, lalu menyaringnya di memori dengan membandingkan
	 * {@code tugasFileContent.getSiswa()} terhadap {@code siswa.getId()}. Penyaringan dilakukan
	 * setelah semua data dibaca, bukan di dalam kueri — jadi biayanya sama besar berapa pun jumlah
	 * hasil akhirnya.</p>
	 *
	 * <p><b>Perhatikan keterbatasan struktural.</b> Indeks pengumpulan hanya menyimpan satu entri per
	 * angka id peserta (lihat
	 * {@link #populateTugasFileContent(TugasFileContent, Class, boolean)}), sehingga peta yang
	 * dihitung di sini pada praktiknya berisi paling banyak satu baris per orang. Nilai balik yang
	 * lebih besar dari 1 karenanya tidak dapat diandalkan sebagai hitungan pengumpulan berganda.</p>
	 *
	 * <p>Tidak melakukan pemeriksaan hak akses dan tidak menyaring berdasarkan tenant: pemanggil
	 * dapat menanyakan siswa mana pun, termasuk siswa dari satuan pendidikan lain. Otorisasi harus
	 * sudah ditegakkan sebelum method ini dipanggil.</p>
	 *
	 * @param siswa siswa yang ditanyakan; {@code null} menghasilkan 0
	 * @return jumlah baris pengumpulan milik siswa tersebut
	 */
	public int ambilJumlahTugasFileContent(Siswa siswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int jumlah = 0;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && siswa != null && tugasFileContent.getSiswa() != null
					&& tugasFileContent.getSiswa().equals(siswa.getId())) {
				jumlah++;
			}
			tugasFileContent = null;
		}
		tugasFileContentsa = null;
		return jumlah;
	}

	/**
	 * Menghitung berapa berkas yang sudah dikumpulkan oleh seorang mahasiswa untuk tugas ini.
	 *
	 * <p>Kembaran {@link #ambilJumlahTugasFileContent(Siswa)} untuk domain perguruan tinggi; seluruh
	 * catatan pada method itu — penyaringan di memori atas seluruh peta, batas satu entri per angka
	 * id, dan tiadanya pemeriksaan hak akses — berlaku sama di sini.</p>
	 *
	 * @param mahasiswa mahasiswa yang ditanyakan; {@code null} menghasilkan 0
	 * @return jumlah baris pengumpulan milik mahasiswa tersebut
	 * @see #ambilJumlahTugasFileContent(Siswa)
	 */
	public int ambilJumlahTugasFileContent(Mahasiswa mahasiswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int jumlah = 0;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && mahasiswa != null && tugasFileContent.getMahasiswa() != null
					&& tugasFileContent.getMahasiswa().equals(mahasiswa.getId())) {
				jumlah++;
			}
			tugasFileContent = null;
		}
		tugasFileContentsa = null;
		return jumlah;
	}

	/**
	 * Menghitung berapa berkas yang sudah dikumpulkan oleh seorang calon mahasiswa untuk tugas ini.
	 *
	 * <p>Kembaran {@link #ambilJumlahTugasFileContent(Siswa)} untuk peserta yang belum berstatus
	 * mahasiswa penuh — dipakai pada alur seleksi/matrikulasi yang menugaskan calon mahasiswa. Semua
	 * catatan pada method itu berlaku sama.</p>
	 *
	 * <p>Tidak ada kembaran untuk {@code CalonSiswa} meskipun {@link TugasFileContent} memiliki kolom
	 * identitas untuk jenis itu dan
	 * {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)} menanganinya. Bila
	 * hitungan untuk calon siswa dibutuhkan, tambahkan overload yang mengikuti pola yang sama alih-alih
	 * menyalin logikanya ke pemanggil.</p>
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa yang ditanyakan; {@code null} menghasilkan 0
	 * @return jumlah baris pengumpulan milik calon mahasiswa tersebut
	 * @see #ambilJumlahTugasFileContent(Siswa)
	 */
	public int ambilJumlahTugasFileContent(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int jumlah = 0;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && biodataCalonMahasiswa != null
					&& tugasFileContent.getBiodataCalonMahasiswa() != null
					&& tugasFileContent.getBiodataCalonMahasiswa().equals(biodataCalonMahasiswa.getId())) {
				jumlah++;
			}
			tugasFileContent = null;
		}
		tugasFileContentsa = null;
		return jumlah;
	}
	
	/**
	 * Menghitung total pengumpulan untuk tugas ini memakai indeks yang sudah ada.
	 *
	 * <p>Setara dengan {@code ambilJumlahTugasFileContent(false)}: indeks berkas dipakai apa adanya
	 * bila sudah pernah dibangun, dan hanya dibangun ulang bila memang belum ada. Untuk memaksa
	 * pembacaan ulang dari basis data, pakai {@link #ambilJumlahTugasFileContent(boolean)} dengan
	 * {@code true}.</p>
	 *
	 * @return jumlah pengumpulan yang terdaftar pada indeks
	 * @see #ambilJumlahTugasFileContent(boolean)
	 */
	public int ambilJumlahTugasFileContent() {
		return ambilJumlahTugasFileContent(false);
	}

	/**
	 * Menghitung total pengumpulan untuk tugas ini, dengan pilihan membangun ulang indeks lebih
	 * dahulu.
	 *
	 * <p>Bila {@code refresh} bernilai {@code true}, atau bila penanda cache
	 * {@code "tugas_file_content_<namaKelas>"} belum tercatat lewat {@code udah(...)}, indeks
	 * dibangun ulang dari basis data melalui {@link #reInitTugasFileContent(Session)} memakai session
	 * streaming.</p>
	 *
	 * <p><b>Session yang dibuka di sini tidak ditutup.</b> Berbeda dengan
	 * {@link #reInitTugasFileContent()} yang menutup session streaming setelah selesai, cabang di
	 * method ini memanggil {@code StreamingHibernateUtil.getInstance().currentSession()} lalu
	 * meneruskannya tanpa penutupan. Pemanggil yang menjalankan method ini di luar daur permintaan
	 * ZK — misalnya dari thread latar — perlu memastikan session streaming ditutup di tempat lain,
	 * jika tidak koneksi akan menumpuk.</p>
	 *
	 * <p>Penanda cache dikunci pada {@code getClass().getName()}. Pada entity berproksi Hibernate
	 * nama kelasnya berbeda dari kelas aslinya, sehingga penanda untuk objek yang sama dapat
	 * tercatat lebih dari sekali dan indeks dibangun ulang lebih sering daripada yang diperlukan.</p>
	 *
	 * @param refresh {@code true} untuk memaksa pembangunan ulang indeks dari basis data
	 * @return jumlah pengumpulan yang terdaftar; 0 bila entity ini belum tersimpan
	 *         ({@code getId() == null})
	 */
	public int ambilJumlahTugasFileContent(boolean refresh) {
		if (getId() == null) {
			return 0;
		}
		if (refresh || !udah("tugas_file_content_" + getClass().getName())) {
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			reInitTugasFileContent(session);
		}
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int jumlah = tugasFileContentsa.size();
		tugasFileContentsa = null;
		return jumlah;
	}

	/**
	 * Mengambil baris pengumpulan milik seorang mahasiswa untuk tugas ini.
	 *
	 * <p>Membangun seluruh peta pengumpulan lalu memindainya sampai menemukan baris yang
	 * {@code mahasiswa}-nya cocok. Karena pencocokan dilakukan setelah semua data dimuat, biayanya
	 * tidak berkurang meskipun yang dicari hanya satu orang.</p>
	 *
	 * <p>Bila entah bagaimana terdapat lebih dari satu baris yang cocok, yang dikembalikan adalah
	 * yang pertama ditemui menurut urutan iterasi peta — bukan yang terbaru. Untuk tugas yang
	 * mengizinkan unggah ulang, jangan mengandalkan method ini untuk mendapatkan versi terakhir.</p>
	 *
	 * <p>Tidak memeriksa hak akses: pemanggil dapat mengambil pengumpulan mahasiswa mana pun.</p>
	 *
	 * @param mahasiswa mahasiswa yang dicari; {@code null} menghasilkan {@code null}
	 * @return baris pengumpulan milik mahasiswa tersebut, atau {@code null} bila belum mengumpulkan
	 */
	public TugasFileContent ambilTugasFileContent(Mahasiswa mahasiswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		TugasFileContent pilih = null;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && mahasiswa != null && tugasFileContent.getMahasiswa() != null
					&& tugasFileContent.getMahasiswa().equals(mahasiswa.getId())) {
				pilih = tugasFileContent;
				break;
			}
		}
		tugasFileContentsa = null;
		return pilih;
	}

	/**
	 * Mengambil baris pengumpulan milik seorang siswa untuk tugas ini.
	 *
	 * <p>Kembaran {@link #ambilTugasFileContent(Mahasiswa)} untuk domain sekolah; seluruh catatannya
	 * berlaku sama, termasuk tiadanya pemeriksaan hak akses dan tiadanya jaminan bahwa yang
	 * dikembalikan adalah pengumpulan terbaru.</p>
	 *
	 * @param siswa siswa yang dicari; {@code null} menghasilkan {@code null}
	 * @return baris pengumpulan milik siswa tersebut, atau {@code null} bila belum mengumpulkan
	 * @see #ambilTugasFileContent(Mahasiswa)
	 */
	public TugasFileContent ambilTugasFileContent(Siswa siswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		TugasFileContent pilih = null;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && siswa != null && tugasFileContent.getSiswa() != null
					&& tugasFileContent.getSiswa().equals(siswa.getId())) {
				pilih = tugasFileContent;
				break;
			}
		}
		tugasFileContentsa = null;
		return pilih;
	}

	/**
	 * Memuat seluruh pengumpulan untuk tugas ini sebagai peta berkunci id peserta didik.
	 *
	 * <p>Jalur ringkas yang dipakai hampir seluruh method pembaca pada kelas ini. Meneruskan ke
	 * {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int)} dengan peta kosong, tanpa
	 * kata pencarian, tanpa paging, dan batas 1000 baris.</p>
	 *
	 * <p><b>Batas 1000 itu diam.</b> Tugas dengan lebih dari seribu pengumpulan akan terpotong tanpa
	 * peringatan apa pun kepada pemanggil, dan seluruh hitungan yang diturunkan darinya — termasuk
	 * {@link #ambilJumlahTugasFileContent()} dan {@link #tugasFileContent()} — ikut terpotong.</p>
	 *
	 * <p><b>Efek samping: method ini mengosongkan field {@link #currentUser}.</b> Sebuah method yang
	 * namanya berawalan {@code ambil} dan tampak seperti pembacaan murni justru menghapus keadaan
	 * yang mungkin baru saja disetel pemanggil. Akibat langsungnya, blok pemilihan
	 * {@link #currentTugasFileContent} di dalam
	 * {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)} tidak akan pernah
	 * terpicu lewat jalur ini — {@code currentUser} sudah {@code null} sebelum peta dibangun.
	 * Pemanggil yang membutuhkan pemisahan "pengumpulan saya sendiri" harus menyetel
	 * {@link #currentUser} lalu memanggil overload yang lebih panjang secara langsung, bukan method
	 * ini.</p>
	 *
	 * @return peta pengumpulan berkunci id peserta didik; kosong bila entity belum tersimpan
	 */
	public TreeMap<Long, TugasFileContent> ambilTugasFileContentTotal() {
		currentUser = null;
		TreeMap<Long, TugasFileContent> treemap = new TreeMap<Long, TugasFileContent>();
		if (getId() == null) {
			return treemap;
		}
		TreeMap<Long, TugasFileContent> d = ambilTugasFileContentTotal(treemap, "", null, 1000);
		treemap = null;
		return d;
	}

	/**
	 * Pengguna yang sedang dilayani, dipakai untuk memisahkan "pengumpulan saya sendiri" dari
	 * pengumpulan orang lain saat peta dibangun.
	 *
	 * <p>Bila field ini terisi, {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int,
	 * boolean)} akan mengeluarkan baris milik pengguna tersebut dari hasil yang dikembalikan dan
	 * menaruhnya di {@link #currentTugasFileContent}. Ini yang memungkinkan satu halaman menampilkan
	 * "jawaban Anda" secara terpisah dari daftar jawaban peserta lain.</p>
	 *
	 * <p><b>Bukan mekanisme keamanan.</b> Field ini hanya mengatur penempatan baris di UI; tidak ada
	 * satu pun jalur pada kelas ini yang membatasi <i>apa</i> yang boleh dibaca berdasarkan
	 * nilainya. Peta yang dikembalikan tetap memuat pengumpulan seluruh peserta. Pembatasan akses
	 * harus dilakukan di lapisan action/helper.</p>
	 *
	 * <p><b>Keadaan yang dapat berubah sendiri dan tidak aman untuk banyak thread.</b> Ini field
	 * {@code public} pada entity yang dapat dibagi antar permintaan lewat cache. Nilainya dikosongkan
	 * sebagai efek samping oleh {@link #ambilTugasFileContentTotal()} — jadi menyetelnya lalu
	 * memanggil jalur ringkas itu akan membuang setelan tersebut. Setel field ini tepat sebelum
	 * memanggil overload yang panjang, dan jangan berasumsi nilainya bertahan setelahnya.</p>
	 */
	public Tbmuser currentUser = null;
	/**
	 * Baris pengumpulan milik {@link #currentUser}, ditaruh di sini sebagai nilai balik kedua.
	 *
	 * <p>Diisi oleh {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)} — yang
	 * mengosongkannya di awal, lalu mengisinya bila menemukan baris milik {@link #currentUser}.
	 * Karena baris itu sekaligus <i>dikeluarkan</i> dari peta yang dikembalikan, field ini adalah
	 * satu-satunya cara mendapatkannya.</p>
	 *
	 * <p><b>Bacalah segera setelah pemanggilan.</b> Nilainya ditimpa oleh pemanggilan berikutnya pada
	 * entity yang sama, sehingga pola "panggil, lalu baca field" pada entity yang dipakai bersama
	 * antar permintaan bersifat rapuh. Field ini juga tidak pernah dipersistensi; ia murni tempat
	 * penampungan sementara di memori.</p>
	 */
	public TugasFileContent currentTugasFileContent = null;

	/**
	 * Memuat pengumpulan untuk tugas ini dengan pencarian dan paging, tanpa memaksa pembangunan ulang
	 * indeks.
	 *
	 * <p>Meneruskan ke {@link #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)}
	 * dengan {@code refresh} bernilai {@code false}: indeks berkas dipakai apa adanya bila sudah
	 * pernah dibangun. Gunakan overload lima parameter bila data baru saja berubah dan hasil terkini
	 * memang dibutuhkan.</p>
	 *
	 * @param treemap peta penampung; boleh {@code null} — akan dibuatkan peta kosong
	 * @param cari kata kunci untuk menyaring berdasarkan nama berkas; kosong berarti tanpa penyaringan
	 * @param paging komponen paging ZK yang akan diperbarui; boleh {@code null}
	 * @param banyak jumlah baris per halaman
	 * @return peta pengumpulan untuk halaman yang diminta
	 * @see #ambilTugasFileContentTotal(TreeMap, String, Paging, int, boolean)
	 */
	public TreeMap<Long, TugasFileContent> ambilTugasFileContentTotal(TreeMap<Long, TugasFileContent> treemap,
			String cari, Paging paging, int banyak) {
		boolean refresh = false;
		return ambilTugasFileContentTotal(treemap, cari, paging, banyak, refresh);
	}

	/**
	 * Membangun daftar pengumpulan tugas yang siap ditampilkan: memuat baris dari indeks, menempelkan
	 * nilai dan keterangan, memisahkan pengumpulan milik pengguna yang sedang dilayani, menyaring
	 * dengan kata kunci, lalu memotongnya menjadi satu halaman.
	 *
	 * <p>Ini mesin utama kelas ini; hampir seluruh pembaca lain bermuara ke sini. Alurnya panjang dan
	 * memiliki beberapa perilaku yang tidak terlihat dari tanda tangannya, sehingga perlu diuraikan
	 * satu per satu.</p>
	 *
	 * <h3>1. Memuat baris dari indeks berkas</h3>
	 * <p>Isi berkas indeks dibaca sebagai teks lalu diperiksa: {@code null}, kosong, atau tidak
	 * diawali {@code "{"} membuat method mengembalikan peta penampung apa adanya. Penjagaan awalan
	 * ini yang mencegah berkas indeks yang rusak meruntuhkan seluruh halaman.</p>
	 * <p>Setiap nilai pada dokumen indeks diperlakukan dua cara: bila berupa angka, ia adalah id
	 * {@link TugasFileContent} dan barisnya dimuat lewat cache entity; bila bukan, ia diperlakukan
	 * sebagai <b>lintasan berkas</b> yang isinya diurai menjadi objek {@link TugasFileContent}. Jalur
	 * kedua adalah bentuk lama yang masih didukung. Perhatikan bahwa lintasan itu diambil dari isi
	 * berkas indeks dan dibuka tanpa pembatasan direktori — bila penyerang dapat memengaruhi isi
	 * berkas indeks, ia menentukan berkas mana yang dibaca server. Jalur tulis yang ada saat ini
	 * hanya pernah menulis angka, sehingga celah ini tidak terjangkau dari luar; jangan menambah
	 * jalur yang menulis lintasan dari masukan pengguna.</p>
	 * <p>Baris hanya diterima bila {@code getPertemuan()} terisi dan positif <b>dan</b> id pesertanya
	 * tidak tercantum pada {@link #getMhsYgTidakIkut()}. Pemeriksaan pengecualian itu memakai
	 * {@code contains("," + id + ",")} pada string, sehingga mewarisi tabrakan id lintas-jenis yang
	 * diuraikan pada getter tersebut.</p>
	 * <p>Setiap kegagalan pada satu entri ditelan dan perulangan berlanjut. Satu baris yang rusak
	 * tidak menghentikan yang lain, tetapi juga tidak pernah dilaporkan ke pemanggil — daftar sekadar
	 * tampil lebih pendek.</p>
	 *
	 * <h3>2. Menulis ulang kolom pemilik pada objek yang dimuat</h3>
	 * <p>Setiap baris yang lolos dikenai {@code tugasFileContent.setPertemuan(this.getId())}.
	 * Pembacaan yang menulis — bila objek yang dimuat sedang terikat pada session Hibernate, mutasi
	 * ini menjadikannya kotor dan dapat ikut tersimpan pada flush berikutnya. Karena nilai yang
	 * ditulis adalah id {@code this}, baris yang tadinya terambil karena tabrakan id (lihat
	 * {@link #reInitTugasFileContent(Session)}) akan <b>dipindahkan kepemilikannya secara permanen</b>
	 * ke objek pembelajaran yang sedang dibaca. Sekadar membuka halaman daftar pengumpulan sudah
	 * cukup untuk memicunya.</p>
	 *
	 * <h3>3. Menempelkan nilai dan keterangan dari dokumen JSON</h3>
	 * <p>Nilai dan keterangan tidak dibaca dari baris pengumpulan melainkan dari
	 * {@link #getKeteranganNilai()} milik tugas ini, memakai kunci {@code "<id>_<jenis>_nilai"} dan
	 * {@code "<id>_<jenis>_ket"}. Nilai yang ditemukan <b>ditimpakan</b> ke objek
	 * {@link TugasFileContent} lewat {@code setNilai(...)} dan {@code setKeterangan(...)}. Sama seperti
	 * butir sebelumnya, ini mutasi pada objek yang mungkin terikat session.</p>
	 * <p>Penurunan kunci di sini memakai urutan yang <b>berbeda</b> dari yang dipakai saat memilih
	 * kunci peta: di sini urutannya mahasiswa, siswa, calon mahasiswa, calon siswa dengan
	 * {@code else if} berantai dan hanya memeriksa {@code != null} tanpa memeriksa nilainya positif;
	 * di tempat lain pada kelas ini urutannya berlapis-timpa dengan pemeriksaan {@code > 0L}. Baris
	 * yang kolomnya terisi nol — bukan {@code null} — akan menghasilkan kunci yang berbeda antara
	 * kedua tempat itu, dan nilainya gagal tertempel tanpa pesan kesalahan.</p>
	 *
	 * <h3>4. Memisahkan pengumpulan milik pengguna yang sedang dilayani</h3>
	 * <p>Bila {@link #currentUser} terisi dan salah satu identitasnya cocok dengan baris yang sedang
	 * diproses, baris itu ditaruh di {@link #currentTugasFileContent} dan <b>tidak dimasukkan ke peta
	 * yang dikembalikan</b>. Perhatikan bahwa baris seperti itu juga <b>tidak menambah pencacah
	 * {@code index}</b>, sehingga total yang disetel ke {@link Paging} tidak menghitung pengumpulan
	 * milik pengguna sendiri. Ini disengaja untuk tampilan "jawaban Anda" yang terpisah, tetapi
	 * berarti angka total yang terlihat peserta lebih kecil satu daripada angka yang terlihat
	 * pengajar.</p>
	 * <p>Cabang ini hanya aktif bila {@link #currentUser} sudah disetel sebelum pemanggilan. Melalui
	 * {@link #ambilTugasFileContentTotal()} hal itu mustahil, karena method tersebut mengosongkan
	 * {@link #currentUser} terlebih dahulu.</p>
	 *
	 * <h3>5. Penyaringan dan pemotongan halaman</h3>
	 * <p>Kata kunci {@code cari} dicocokkan terhadap nama berkas secara tidak peka huruf besar-kecil.
	 * Pemotongan halaman dilakukan <b>di memori</b>, setelah seluruh baris dimuat: jendela
	 * {@code [banyak * halamanAktif, + banyak)} dipilih dari hasil yang sudah lengkap. Paging di sini
	 * karenanya hanya menghemat lebar tampilan, bukan biaya pemuatan.</p>
	 * <p>Bila {@code paging} diberikan, method ini juga mengubah komponen ZK tersebut: menyetel total,
	 * ukuran halaman, kenaikan halaman, cetakan {@code "os"}, serta menyembunyikan komponen dan
	 * induknya bila hasil muat dalam satu halaman. Sebuah method pemuat data yang sekaligus menata
	 * komponen UI — pemanggil dari luar daur ZK harus mengirim {@code null} agar tidak menyentuh
	 * pohon komponen milik permintaan lain.</p>
	 *
	 * <h3>Catatan ketahanan</h3>
	 * <p>Peta penampung {@code null} sudah ditangani: method membuatkan peta kosong sendiri, sesuai
	 * komentar perbaikan di dalam badan method — pemanggil dari thread latar sempat mengirim
	 * {@code null} dan meruntuhkan alur unduhan. Kontraknya kini "selalu mengembalikan peta yang
	 * sah".</p>
	 *
	 * @param treemap peta penampung untuk tahap perantara; boleh {@code null}. Perhatikan bahwa peta
	 *        yang dikembalikan adalah peta <i>baru</i>, bukan peta ini — peta ini hanya dipakai
	 *        sebagai penampung antara dan tetap ikut termutasi.
	 * @param cari kata kunci penyaring nama berkas; {@code null} atau kosong berarti tanpa penyaringan
	 * @param paging komponen paging ZK yang akan dibaca halaman aktifnya dan diperbarui; kirim
	 *        {@code null} bila memanggil dari luar daur permintaan ZK
	 * @param banyak jumlah baris per halaman
	 * @param refresh {@code true} untuk membangun ulang indeks dari basis data lebih dahulu
	 * @return peta berisi satu halaman pengumpulan, tidak termasuk pengumpulan milik
	 *         {@link #currentUser}
	 * @see #currentTugasFileContent
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Long, TugasFileContent> ambilTugasFileContentTotal(TreeMap<Long, TugasFileContent> treemap,
	        String cari, Paging paging, int banyak, boolean refresh) {
	    // FIX NPE: pemanggil (mis. TugasMandiriHelper dari background thread download) bisa
	    // mengirim treemap null (field belum diinisialisasi) -> treemap.keySet() di bawah meledak.
	    // Buat map kosong sendiri bila null, supaya kontrak method tetap "selalu return map valid".
	    if (treemap == null) {
	        treemap = new TreeMap<Long, TugasFileContent>();
	    }
	    if (!udah("tugas_file_content_" + getClass().getName()) || refresh) {
	        if (getId() == null) {
	            return treemap;
	        }
	        Session session = StreamingHibernateUtil.getInstance().currentSession();
	        reInitTugasFileContent(session);
	    }
	    TreeMap<Long, TugasFileContent> tugasFileContentsa = new TreeMap<Long, TugasFileContent>(
	            Collections.reverseOrder());

	    currentTugasFileContent = null;
	    try {
	        String lokasiContent = ambilLokasiTugasFileContent(getId(), getClass());
	        if (lokasiContent == null || lokasiContent.trim().isEmpty()
	                || !lokasiContent.trim().startsWith("{")) {
	            return treemap;
	        }
	        JSONObject c = new JSONObject(lokasiContent);
	        Iterator<String> keys = c.keys();
	        while (keys.hasNext()) {
	            String key = keys.next();
	            try {
	                // KODE YANG DIPERBAIKI: Menggunakan optString untuk menghindari JSONException "not a string"
	                String s = c.optString(key, ""); 
	                
	                if (!s.trim().isEmpty()) {

	                    if (Common.isNumber(s.trim())) {
	                        TugasFileContent tugasFileContent = (TugasFileContent) TugasFileContent.ambil(true,
	                                Long.parseLong(s.trim()), TugasFileContent.DEFAULT_JENIS, TugasFileContent.class);
	                        if (tugasFileContent != null) {
	                            Long id = tugasFileContent.getMahasiswa() != null
	                                    && tugasFileContent.getMahasiswa() > 0L ? tugasFileContent.getMahasiswa()
	                                            : tugasFileContent.getBiodataCalonMahasiswa();

	                            if (tugasFileContent.getSiswa() != null && tugasFileContent.getSiswa() > 0L) {
	                                id = tugasFileContent.getSiswa();
	                            }
	                            if (tugasFileContent.getCalonSiswa() != null && tugasFileContent.getCalonSiswa() > 0L) {
	                                id = tugasFileContent.getCalonSiswa();
	                            }

	                            if (tugasFileContent.getPertemuan() != null && tugasFileContent.getPertemuan() > 0L
	                                    && !getMhsYgTidakIkut().contains("," + id + ",")) {
	                                tugasFileContentsa.put(tugasFileContent.getId(), tugasFileContent);
	                            }
	                        }
	                    } else {
	                        File file = new File(s);
	                        if (file != null && file.exists()) {

	                            TugasFileContent tugasFileContent = (TugasFileContent) Common.convertToObject(
	                                    new JSONObject(ais.common.BacaTulisUtil.baca(file)), TugasFileContent.class);
	                            Long id = tugasFileContent.getMahasiswa() != null
	                                    && tugasFileContent.getMahasiswa() > 0L ? tugasFileContent.getMahasiswa()
	                                            : tugasFileContent.getBiodataCalonMahasiswa();

	                            if (tugasFileContent.getSiswa() != null && tugasFileContent.getSiswa() > 0L) {
	                                id = tugasFileContent.getSiswa();
	                            }
	                            if (tugasFileContent.getCalonSiswa() != null && tugasFileContent.getCalonSiswa() > 0L) {
	                                id = tugasFileContent.getCalonSiswa();
	                            }

	                            if (tugasFileContent.getPertemuan() != null && tugasFileContent.getPertemuan() > 0L
	                                    && !getMhsYgTidakIkut().contains("," + id + ",")) {
	                                tugasFileContentsa.put(tugasFileContent.getId(), tugasFileContent);
	                            }
	                        }
	                    }
	                }
	            } catch (Exception e) {
	                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:391");
	            }
	        }
	    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:394");
//	      e.printStackTrace();
	    }

	    for (Long id : tugasFileContentsa.keySet()) {

	        TugasFileContent tugasFileContent = tugasFileContentsa.get(id);
	        tugasFileContent.setPertemuan(this.getId());
	        if (tugasFileContent.getMahasiswa() != null && tugasFileContent.getMahasiswa() > 0L
	                && !treemap.containsKey(tugasFileContent.getMahasiswa()))
	            treemap.put(tugasFileContent.getMahasiswa(), tugasFileContent);
	        else if (tugasFileContent.getBiodataCalonMahasiswa() != null
	                && tugasFileContent.getBiodataCalonMahasiswa() > 0L
	                && !treemap.containsKey(tugasFileContent.getBiodataCalonMahasiswa()))
	            treemap.put(tugasFileContent.getBiodataCalonMahasiswa(), tugasFileContent);
	        else if (tugasFileContent.getSiswa() != null && tugasFileContent.getSiswa() > 0L
	                && !treemap.containsKey(tugasFileContent.getSiswa()))
	            treemap.put(tugasFileContent.getSiswa(), tugasFileContent);
	        else if (tugasFileContent.getCalonSiswa() != null && tugasFileContent.getCalonSiswa() > 0L
	                && !treemap.containsKey(tugasFileContent.getCalonSiswa()))
	            treemap.put(tugasFileContent.getCalonSiswa(), tugasFileContent);

	    }
	    tugasFileContentsa.clear();
	    tugasFileContentsa = null;
	    TreeMap<Long, TugasFileContent> treemapBaru = new TreeMap<Long, TugasFileContent>();
	    int index = 0;
	    int mulai = paging == null ? 0 : banyak * paging.getActivePage();
	    for (Long id : treemap.keySet()) {
	        TugasFileContent fileContent = treemap.get(id);

	        try {
	            String key = "";
	            if (fileContent.getMahasiswa() != null) {
	                key = fileContent.getMahasiswa() + "_mhs";
	            } else if (fileContent.getSiswa() != null) {
	                key = fileContent.getSiswa() + "_siswa";
	            } else if (fileContent.getBiodataCalonMahasiswa() != null) {
	                key = fileContent.getBiodataCalonMahasiswa() + "_cal_mhs";
	            } else if (fileContent.getCalonSiswa() != null) {
	                key = fileContent.getCalonSiswa() + "_cal_siswa";
	            }

	            JSONObject jsonObject = new JSONObject(getKeteranganNilai());
	            if (!jsonObject.isNull(key + "_ket")) {
	                String ket = jsonObject.get(key + "_ket") + "";
	                if (ket != null) {

//	                  System.out.println("key -> "+key+", ket "+ket);

	                    fileContent.setKeterangan(ket);
	                }
	            }

	            if (!jsonObject.isNull(key + "_nilai")) {
	                Double nilai = jsonObject.getDouble(key + "_nilai");
	                if (nilai != null) {

//	                  System.out.println("key -> "+key+", nilai "+nilai);

	                    fileContent.setNilai(nilai);
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:458");
	        }

	        if (fileContent.getMahasiswa() != null && fileContent.getMahasiswa() > 0L && currentUser != null
	                && currentUser.getMahasiswa() != null && currentUser.getMahasiswa().getId() != null
	                && currentUser.getMahasiswa().getId().equals(fileContent.getMahasiswa())) {
	            currentTugasFileContent = fileContent;
	        } else if (fileContent.getBiodataCalonMahasiswa() != null && fileContent.getBiodataCalonMahasiswa() > 0L
	                && currentUser != null && currentUser.getBiodataCalonMahasiswa() != null
	                && currentUser.getBiodataCalonMahasiswa().getId() != null
	                && currentUser.getBiodataCalonMahasiswa().getId().equals(fileContent.getBiodataCalonMahasiswa())) {
	            currentTugasFileContent = fileContent;
	        } else if (fileContent.getSiswa() != null && fileContent.getSiswa() > 0L && currentUser != null
	                && currentUser.getSiswa() != null && currentUser.getSiswa().getId() != null
	                && currentUser.getSiswa().getId().equals(fileContent.getSiswa())) {
	            currentTugasFileContent = fileContent;
	        } else if (fileContent.getCalonSiswa() != null && fileContent.getCalonSiswa() > 0L && currentUser != null
	                && currentUser.getCalonSiswa() != null && currentUser.getCalonSiswa().getId() != null
	                && currentUser.getCalonSiswa().getId().equals(fileContent.getCalonSiswa())) {
	            currentTugasFileContent = fileContent;
	        } else {
	            String realFile = fileContent.getNama();
	            if (cari == null || cari.trim().isEmpty()
	                    || (realFile != null && realFile.toLowerCase().trim().contains(cari.toLowerCase().trim()))) {
	                if (index >= mulai && index < (mulai + banyak)) {
	                    treemapBaru.put(id, fileContent);
	                }
	                index++;
	            }
	        }

	    }

	    if (paging != null) {
	        paging.setTotalSize(index);
	        paging.setVisible(index > banyak);
	        try {
	            paging.setPageSize(banyak);
	        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:496");
	        }
	        paging.setPageIncrement(Common.isMobile() ? 5 : 10);
	        paging.setMold("os");
	        try {
	            if (paging.getParent() != null) {
	                paging.getParent().setVisible(index > banyak);
	            }
	        } catch (Exception e) { /* paging/parent detached, safe to ignore */ }
	    }

	    return treemapBaru;
	}

	public List<TugasFileContent> ambilTugasFileContent(TreeMap<Long, TugasFileContent> tugasFileContentsa, int mulai,
			int banyak) {

		int index = 0;
		List<TugasFileContent> tugasFileContents = new ArrayList<TugasFileContent>();
		Iterator<Long> i = tugasFileContentsa.keySet().iterator();
		while (i.hasNext()) {
			TugasFileContent tugasFileContent = tugasFileContentsa.get(i.next());

			if (index >= mulai && index < (mulai + banyak)) {
				tugasFileContents.add(tugasFileContent);
			}
			index++;

		}
		return tugasFileContents;
	}

	public String ambilLink(TugasFileContent tugasFileContent) {

		if (tugasFileContent != null && tugasFileContent.getGdrive() != null) {
			return tugasFileContent.exportGDriveUrl();
		}

		String link = tugasFileContent == null ? null
				: (tugasFileContent.getLink() == null || tugasFileContent.getLink().isEmpty() ? null
						: tugasFileContent.getLink());

		if (link != null && link.toLowerCase().trim().contains("dropbox")) {
			return tugasFileContent.dropboxLinkRaw();
		}

		try {
			if (tugasFileContent != null && (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
				link = tugasFileContent.createLinkUri(false);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:547");
		}

		return link == null ? "" : link.trim();
	}

	@SuppressWarnings("deprecation")
	public static void tampilanSyarat(Pertemuan pertemuan, final Tugas tugas, final Ujian ujian,
			final PertemuanFileContent pertemuanFileContent, final AudioPertemuan audioPertemuan,
			final VideoPertemuan videoPertemuan, Rows rows, final Set<String> syaratAlert,
			final MyToolbarbutton buttonRefreshSyarat) {
		final VOPembelajaran pembelajaran = pertemuan == null ? null : pertemuan.ambilVOPembelajaran();
		if (pembelajaran != null) {

			Row rowTugas = new Row();
			Row rowUjian = new Row();
			Row rowMateri = new Row();

			final Rows rowsTugas = (Rows) Common.tampilanScroll1(rowTugas).getParent();
			final Rows rowsUjian = (Rows) Common.tampilanScroll1(rowUjian).getParent();
			final Rows rowsMateri = (Rows) Common.tampilanScroll1(rowMateri).getParent();

			final Textbox cariTugas = new Textbox();
			final Textbox cariUjian = new Textbox();
			final Textbox cariMateri = new Textbox();

			EventListener reloadMateri = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(rowsUjian);
					Common.clear(rowsTugas);
					Common.clear(rowsMateri);

					rowsTugas.setAttribute("index", 0);
					rowsUjian.setAttribute("index", 0);
					rowsMateri.setAttribute("index", 0);

					rowsTugas.setAttribute("ditampilkan", 25);
					rowsUjian.setAttribute("ditampilkan", 25);
					rowsMateri.setAttribute("ditampilkan", 25);

					rowsTugas.setAttribute("selesai", false);
					rowsUjian.setAttribute("selesai", false);
					rowsMateri.setAttribute("selesai", false);

					Group row = new Group();
					row.setParent(rowsTugas);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengumpulkan tugas sbb : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengikuti ujian ini, harus mengumpulkan tugas sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses materi ini, harus mengumpulkan tugas sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses audio ini, harus mengumpulkan tugas sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses video ini, harus mengumpulkan tugas sbb : "));

					row = new Group();
					row.setParent(rowsUjian);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengikuti ujian sbb : "));
					else if (ujian != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengikuti ujian ini, harus mengikuti ujian sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses materi ini, harus mengikuti ujian sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses audio ini, harus mengikuti ujian sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses video ini, harus mengikuti ujian sbb : "));

					row = new Group();
					row.setParent(rowsMateri);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengakses materi, audio, atau video sbb : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengikuti ujian ini, harus mengakses materi, audio, atau video sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses materi ini, harus mengakses materi, audio, atau video sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses audio ini, harus mengakses materi, audio, atau video sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses video ini, harus mengakses materi, audio, atau video sbb : "));

					if (tugas != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (tugas.getId() != null) {
											session.refresh(tugas);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(tugas.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										tugas.setSyaratAkses(jsonObject.toString());
										Common.refreshUpdate(session, tugas);
										session.flush();
									}

								}, new JSONObject(tugas.getSyaratAkses()), syaratAlert, tugas, buttonRefreshSyarat);
					} else if (ujian != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (ujian.getId() != null) {
											session.refresh(ujian);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(ujian.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										ujian.setSyaratAkses(jsonObject.toString());
										Common.refreshUpdate(session, ujian);
										session.flush();
									}

								}, new JSONObject(ujian.getSyaratAkses()), syaratAlert, ujian, buttonRefreshSyarat);
					} else if (pertemuanFileContent != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (pertemuanFileContent.getId() != null) {
											session.refresh(pertemuanFileContent);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(pertemuanFileContent.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										pertemuanFileContent.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, pertemuanFileContent);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(pertemuanFileContent.getSyaratAkses()), syaratAlert,
								pertemuanFileContent, buttonRefreshSyarat);
					} else if (audioPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (audioPertemuan.getId() != null) {
											session.refresh(audioPertemuan);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(audioPertemuan.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										audioPertemuan.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, audioPertemuan);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(audioPertemuan.getSyaratAkses()), syaratAlert, audioPertemuan,
								buttonRefreshSyarat);
					} else if (videoPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (videoPertemuan.getId() != null) {
											session.refresh(videoPertemuan);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(videoPertemuan.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										videoPertemuan.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, videoPertemuan);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(videoPertemuan.getSyaratAkses()), syaratAlert, videoPertemuan,
								buttonRefreshSyarat);
					}
				}
			};

			rowTugas.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowTugas, "2");

			rowUjian.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowUjian, "2");

			rowMateri.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowMateri, "2");

			try {
				reloadMateri.onEvent(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:819");
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void tampilanLain(Pertemuan pertemuan, final Tugas tugas, final Ujian ujian,
			final PertemuanFileContent pertemuanFileContent, final AudioPertemuan audioPertemuan,
			final VideoPertemuan videoPertemuan, Rows rows, final MyToolbarbutton buttonRefreshSyarat) {
		final VOPembelajaran pembelajaran = pertemuan == null ? null : pertemuan.ambilVOPembelajaran();
		if (pembelajaran != null) {

			Row rowTugas = new Row();
			Row rowUjian = new Row();
			Row rowMateri = new Row();

			final Rows rowsTugas = (Rows) Common.tampilanScroll1(rowTugas).getParent();
			final Rows rowsUjian = (Rows) Common.tampilanScroll1(rowUjian).getParent();
			final Rows rowsMateri = (Rows) Common.tampilanScroll1(rowMateri).getParent();

			final Textbox cariTugas = new Textbox();
			final Textbox cariUjian = new Textbox();
			final Textbox cariMateri = new Textbox();

			EventListener reloadMateri = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(rowsUjian);
					Common.clear(rowsTugas);
					Common.clear(rowsMateri);

					rowsTugas.setAttribute("index", 0);
					rowsUjian.setAttribute("index", 0);
					rowsMateri.setAttribute("index", 0);

					rowsTugas.setAttribute("ditampilkan", 25);
					rowsUjian.setAttribute("ditampilkan", 25);
					rowsMateri.setAttribute("ditampilkan", 25);

					rowsTugas.setAttribute("selesai", false);
					rowsUjian.setAttribute("selesai", false);
					rowsMateri.setAttribute("selesai", false);

					Group row = new Group();
					row.setParent(rowsTugas);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold("Tugas lain-nya : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar tugas : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar tugas : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar tugas : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar tugas : "));

					row = new Group();
					row.setParent(rowsUjian);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar ujian : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold("Ujian lain-nya : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar ujian : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar ujian : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar ujian : "));

					row = new Group();
					row.setParent(rowsMateri);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar materi, audio, atau video : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar materi, audio, atau video : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold("Materi, audio, atau video lainnya : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Materi, audio, atau video lainnya : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Materi, audio, atau video lainnya : "));

					if (tugas != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (tugas.getId() != null) {
											session.refresh(tugas);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(tugas.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										tugas.setSyaratAkses(jsonObject.toString());
										Common.refreshUpdate(session, tugas);
										session.flush();
									}

								}, new JSONObject(tugas.getSyaratAkses()), null, tugas, buttonRefreshSyarat);
					} else if (ujian != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (ujian.getId() != null) {
											session.refresh(ujian);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(ujian.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										ujian.setSyaratAkses(jsonObject.toString());
										Common.refreshUpdate(session, ujian);
										session.flush();
									}

								}, new JSONObject(ujian.getSyaratAkses()), null, ujian, buttonRefreshSyarat);
					} else if (pertemuanFileContent != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (pertemuanFileContent.getId() != null) {
											session.refresh(pertemuanFileContent);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(pertemuanFileContent.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										pertemuanFileContent.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, pertemuanFileContent);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(pertemuanFileContent.getSyaratAkses()), null, pertemuanFileContent,
								buttonRefreshSyarat);
					} else if (audioPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (audioPertemuan.getId() != null) {
											session.refresh(audioPertemuan);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(audioPertemuan.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										audioPertemuan.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, audioPertemuan);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(audioPertemuan.getSyaratAkses()), null, audioPertemuan,
								buttonRefreshSyarat);
					} else if (videoPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (videoPertemuan.getId() != null) {
											session.refresh(videoPertemuan);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(videoPertemuan.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										videoPertemuan.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, videoPertemuan);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(videoPertemuan.getSyaratAkses()), null, videoPertemuan,
								buttonRefreshSyarat);
					}
				}
			};

			rowTugas.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowTugas, "2");

			rowUjian.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowUjian, "2");

			rowMateri.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowMateri, "2");

			try {
				reloadMateri.onEvent(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:1074");
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void tampilanSyaratReadonly(Pertemuan pertemuan, final Tugas tugas, final Ujian ujian,
			final PertemuanFileContent pertemuanFileContent, final AudioPertemuan audioPertemuan,
			final VideoPertemuan videoPertemuan, Rows rows, final Set<String> syaratAlert,
			final MyToolbarbutton buttonRefreshSyarat) {
		final VOPembelajaran pembelajaran = pertemuan == null ? null : pertemuan.ambilVOPembelajaran();
		if (pembelajaran != null) {

			Row rowTugas = new Row();
			Row rowUjian = new Row();
			Row rowMateri = new Row();

			final Rows rowsTugas = (Rows) Common.tampilanScroll1(rowTugas).getParent();
			final Rows rowsUjian = (Rows) Common.tampilanScroll1(rowUjian).getParent();
			final Rows rowsMateri = (Rows) Common.tampilanScroll1(rowMateri).getParent();

			final Textbox cariTugas = new Textbox();
			final Textbox cariUjian = new Textbox();
			final Textbox cariMateri = new Textbox();

			EventListener reloadMateri = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(rowsUjian);
					Common.clear(rowsTugas);
					Common.clear(rowsMateri);

					rowsTugas.setAttribute("index", 0);
					rowsUjian.setAttribute("index", 0);
					rowsMateri.setAttribute("index", 0);

					rowsTugas.setAttribute("ditampilkan", 100);
					rowsUjian.setAttribute("ditampilkan", 100);
					rowsMateri.setAttribute("ditampilkan", 100);

					rowsTugas.setAttribute("selesai", false);
					rowsUjian.setAttribute("selesai", false);
					rowsMateri.setAttribute("selesai", false);

					Group row = new Group();
					row.setParent(rowsTugas);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengumpulkan tugas sbb : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengikuti ujian ini, harus mengumpulkan tugas sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses materi ini, harus mengumpulkan tugas sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses audio ini, harus mengumpulkan tugas sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses video ini, harus mengumpulkan tugas sbb : "));

					row = new Group();
					row.setParent(rowsUjian);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengikuti ujian sbb : "));
					else if (ujian != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengikuti ujian ini, harus mengikuti ujian sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses materi ini, harus mengikuti ujian sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses audio ini, harus mengikuti ujian sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses video ini, harus mengikuti ujian sbb : "));

					row = new Group();
					row.setParent(rowsMateri);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengakses materi, audio, atau video sbb : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengikuti ujian ini, harus mengakses materi, audio, atau video sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses materi ini, harus mengakses materi, audio, atau video sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses audio ini, harus mengakses materi, audio, atau video sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses video ini, harus mengakses materi, audio, atau video sbb : "));

					if (tugas != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(tugas.getSyaratAkses()), syaratAlert, tugas, buttonRefreshSyarat);
					} else if (ujian != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(ujian.getSyaratAkses()), syaratAlert, ujian, buttonRefreshSyarat);
					} else if (pertemuanFileContent != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(pertemuanFileContent.getSyaratAkses()), syaratAlert,
								pertemuanFileContent, buttonRefreshSyarat);
					} else if (audioPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(audioPertemuan.getSyaratAkses()), syaratAlert, audioPertemuan,
								buttonRefreshSyarat);
					} else if (videoPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(videoPertemuan.getSyaratAkses()), syaratAlert, videoPertemuan,
								buttonRefreshSyarat);
					}
				}
			};

			rowTugas.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowTugas, "2");

			rowUjian.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowUjian, "2");

			rowMateri.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowMateri, "2");

			try {
				reloadMateri.onEvent(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:1216");
			}
		}
	}

	public static boolean apakahAkses(Pertemuan pertemuan, String akses, Long id) {
		TreeMap<String, String> d = pertemuan.ambilData(akses, null);
//		System.out.println("id " + id + " d -> " + d);
		for (String user : d.keySet()) {
			try {
				String[] u = user.split("-");
				String kode = u[1];

				Long mhs = Long.parseLong(kode);

//				System.out.println("mhs " + mhs);

				if (id.equals(mhs)) {
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:1237");
			}
		}

		return false;
	}

}
