package ais.database.model.sekolah;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.ui.util.WaktuUtil;

/**
 * Kelas dasar <b>abstrak</b> untuk baris roster "kelas punya siswa" — satu baris = satu
 * keanggotaan seorang siswa pada sebuah kelas — sekaligus rumah bagi <b>mesin nilai berbasis
 * string</b> yang dipakai bersama oleh kelas reguler dan kelas les.
 *
 * <h2>Berkas ini BUKAN entity</h2>
 * <p>Tidak ada anotasi {@code @Entity}, {@code @MappedSuperclass}, {@code @Table}, maupun satu pun
 * field yang dipetakan di sini; isinya hanya {@code serialVersionUID}, dua belas deklarasi
 * <b>abstrak</b>, tujuh implementasi konkret mesin nilai, dan dua penolong privat. Tabel, kolom,
 * dan seluruh relasi dideklarasikan di kelas turunan. Rantai pewarisan yang sebenarnya (hasil
 * penelusuran seluruh repo — hanya dua kelas ini yang meng-{@code extends} berkas ini):</p>
 * <pre>
 *   KelasSiswaPunyaSiswa    (roster kelas REGULER)  --+
 *                                                     +--&gt; VoKelasPunyaSiswa --&gt; GeneralValueObject
 *   KelasLesSiswaPunyaSiswa (roster kelas LES)      --+
 * </pre>
 * <p>Karena {@link ais.database.model.GeneralValueObject} juga <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa — Hibernate <b>tidak memetakan satu pun
 * properti dari kedua induk ini</b>. Itulah sebabnya {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di setiap turunan konkret: deklarasi
 * ulang itu keharusan teknis, bukan duplikasi yang keliru. Jangan "membersihkannya".</p>
 *
 * <h2>Format payload nilai — dua kolom {@code text}, dua lapis</h2>
 * <p>Nilai satu baris roster tidak disimpan sebagai tabel anak, melainkan sebagai <b>string</b>
 * pada dua kolom milik turunan:</p>
 * <ul>
 *   <li>{@link #getDetailNilai()} — <b>lapis 1</b>: nilai mentah per <i>item penilaian</i>
 *       ({@link JenisItemPenilaianSiswa}, mis. "Tugas 1", "UH 2").</li>
 *   <li>{@link #getDetailNilaiTotal()} — <b>lapis 2</b>: nilai agregat per <i>kategori</i>
 *       ({@link GrupKategoriItemPenilaianSiswa}, mis. "Pengetahuan", "Keterampilan"), yaitu hasil
 *       formula atas lapis 1.</li>
 * </ul>
 * <p>Keduanya memakai tata letak yang <b>sama persis</b>: entri dipisah titik koma ({@code ;}),
 * ruas di dalam entri dipisah pipa ({@code |}), dan setiap entri wajib punya <b>delapan</b> ruas
 * dengan urutan tetap:</p>
 * <pre>
 *   indeks  isi
 *   ------  ------------------------------------------------------------------------
 *    [0]    id JenisItemPenilaianSiswa (selalu "0" pada payload detailNilaiTotal)
 *    [1]    id Matapelajaran
 *    [2]    NILAI — teks bebas: angka ("80"), nilai pilihan ("A:80"), atau kosong
 *    [3]    "0" — ruas cadangan, tidak pernah dibaca
 *    [4]    "0" — ruas cadangan, tidak pernah dibaca
 *    [5]    status verifikasi ("true"/"false")
 *    [6]    semester
 *    [7]    id GrupKategoriItemPenilaianSiswa
 * </pre>
 * <p>Karena {@code |} dan {@code ;} adalah pemisah, method penulis menyaring kedua karakter itu
 * dari nilai masukan (diganti spasi dan koma). <b>Jangan mengurai kedua kolom ini secara
 * manual</b> — selalu lewat method di kelas ini.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 *   <li><b>Kontrak abstrak identitas &amp; state</b> — {@link #getSiswa()},
 *       {@link #ambilKelasSiswa()}, {@link #getAktif()}, {@link #ambilMk()}.</li>
 *   <li><b>Kontrak abstrak akses payload</b> — {@link #getDetailNilai()}/
 *       {@link #setDetailNilai(String)}, {@link #getDetailNilaiTotal()}/
 *       {@link #setDetailNilaiTotal(String)}.</li>
 *   <li><b>Kontrak abstrak catatan rapor</b> — {@link #getKeterangan1()}/
 *       {@link #setKeterangan1(String)}, {@link #getKeterangan2()}/
 *       {@link #setKeterangan2(String)} (semester 1 dan 2, berformat JSON).</li>
 *   <li><b>Mesin nilai lapis 1</b> —
 *       {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)},
 *       {@link #retreiveDetailVerify(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)},
 *       {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)},
 *       {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}.</li>
 *   <li><b>Mesin nilai lapis 2</b> —
 *       {@link #retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)},
 *       {@link #retreiveTotalNilaiTotal(String, Matapelajaran, GrupPenilaian, Integer, List)},
 *       {@link #populateDetailNilaiTotal(Matapelajaran, GrupKategoriItemPenilaianSiswa, Double, Boolean, Integer)}.</li>
 *   <li><b>Penolong privat (penjaga hasil pengerasan)</b> — {@link #splitNilai(String)},
 *       {@link #nilaiUntukFormula(String)}.</li>
 * </ol>
 *
 * <h2>VERSI MESIN NILAI YANG SUDAH DIPERKERAS ADA DI SINI — dan kelas les menimpanya</h2>
 * <p>Ketujuh method mesin nilai di berkas ini adalah versi yang sudah <b>diperbaiki</b>. Tiga
 * perbaikan yang benar-benar hadir di sini dan dapat diverifikasi langsung dari kode:</p>
 * <ol>
 *   <li>{@link #splitNilai(String)} memakai {@code StringUtils.splitPreserveAllTokens} — yang
 *       <b>mempertahankan ruas kosong</b> — ditambah penjaga panjang minimal 8 ruas. Semua
 *       pembaca/penulis di berkas ini melewatinya, bukan memanggil {@code StringUtils.split}
 *       langsung.</li>
 *   <li>{@link #nilaiUntukFormula(String)} menormalkan nilai non-numerik (mis. {@code "A:80"}
 *       menjadi {@code "80"}, nilai kosong/tak terurai menjadi {@code "0.0"}) sebelum masuk mesin
 *       formula. Dipakai
 *       {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)}
 *       dan {@link #retreiveTotalNilaiTotal(String, Matapelajaran, GrupPenilaian, Integer, List)}.</li>
 *   <li>{@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}
 *       memuat baris {@code jumlah = jumlah == null ? "" : jumlah;} yang mencegah nilai
 *       {@code null} tersimpan sebagai <b>teks literal</b> {@code "null"} pada ruas [2].</li>
 * </ol>
 * <p>{@link ais.database.model.sekolah.KelasSiswaPunyaSiswa} (roster kelas <b>reguler</b>)
 * <b>tidak meng-override satu pun</b> dari ketujuh method itu, sehingga seluruh jalur penilaian dan
 * rapor kelas reguler memakai versi yang sudah diperkeras ini.</p>
 * <p>Sebaliknya, {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa} (roster kelas
 * <b>les</b>) <b>meng-override ketujuhnya</b> dengan salinan versi lama yang tidak punya kedua
 * penolong di atas. Perlu ditegaskan karena mudah salah dibaca: ini <b>override sungguhan lewat
 * pewarisan</b> ({@code KelasLesSiswaPunyaSiswa extends VoKelasPunyaSiswa}), bukan sekadar
 * kemiripan tanda tangan pada kelas yang tidak berkerabat — karena itulah perbaikan yang
 * ditanam di berkas ini <b>tidak pernah sampai ke kelas les</b>. Tiga akibat yang dapat
 * diamati:</p>
 * <ul>
 *   <li>{@code StringUtils.split(nn, "|")} di kelas les <b>membuang ruas kosong</b>; begitu satu
 *       nilai dikosongkan, indeks seluruh entri itu bergeser dan entri tersebut lenyap diam-diam
 *       dari rapor kelas les. Pada kelas reguler bug ini sudah tidak ada.</li>
 *   <li>Nilai bertipe pilihan ({@code "A:80"}) masuk ke {@code GrupPenilaianUtil.hitung()} apa
 *       adanya di kelas les, bukan lewat {@link #nilaiUntukFormula(String)}.</li>
 *   <li>Nilai {@code null} tersimpan sebagai teks {@code "null"} di kelas les.</li>
 * </ul>
 * <p>Bila suatu saat ketujuh override kelas les dihapus agar mewarisi versi ini, <b>perlu
 * pengujian</b>: penjaga verifikasi di berkas ini memakai {@link #ambilKelasSiswa()} yang pada
 * kelas les selalu {@code null}, sementara override lokalnya memakai {@code getKelasLesSiswa()};
 * perilaku saklar "Publikasi Nilai Harus Telah Diverifikasi" akan berubah.</p>
 *
 * <h2>Hal-hal non-obvious lain</h2>
 * <ul>
 *   <li><b>Entri rusak hilang permanen saat penulisan berikutnya.</b> Pada
 *       {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}
 *       dan {@link #populateDetailNilaiTotal(Matapelajaran, GrupKategoriItemPenilaianSiswa, Double, Boolean, Integer)},
 *       entri yang ditolak {@link #splitNilai(String)} (kurang dari 8 ruas) atau yang ruas [0]-nya
 *       kosong <b>tidak ikut disalin</b> ke payload baru. Kedua method itu menulis ulang payload
 *       secara utuh, jadi entri seperti itu terhapus permanen begitu ada satu nilai lain
 *       disimpan.</li>
 *   <li><b>Semua kegagalan parse ditelan.</b> Setiap loop dibungkus {@code try/catch (Exception)}
 *       yang hanya memanggil {@code ais.common.ErrorAuditUtil.record()} lalu melanjutkan iterasi.
 *       Nilai yang tidak terbaca <b>tidak pernah</b> memunculkan pesan kesalahan ke pengguna —
 *       ia sekadar tampil kosong/0. Ini disengaja (satu entri rusak tidak boleh merobohkan seluruh
 *       rapor), tetapi berarti kerusakan data hanya terlihat di log audit.</li>
 *   <li><b>Asimetri filter pada lapis 2.</b>
 *       {@link #retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)}
 *       menyaring mata pelajaran + semester + <i>kategori</i>, sedangkan
 *       {@link #retreiveTotalNilaiTotal(String, Matapelajaran, GrupPenilaian, Integer, List)}
 *       sengaja <b>tidak</b> menyaring kategori — karena seluruh kategori justru dibutuhkan
 *       sebagai variabel formula.</li>
 *   <li><b>Kuirk {@code verify} pada lapis 2.</b>
 *       {@link #populateDetailNilaiTotal(Matapelajaran, GrupKategoriItemPenilaianSiswa, Double, Boolean, Integer)}
 *       memaksa {@code verify = false} ketika {@code jumlah != null} — kebalikan dari kerabat
 *       lapis 1 yang memaksanya hanya ketika nilainya <i>kosong</i>. Praktisnya status verifikasi
 *       nilai agregat selalu tersimpan {@code false}. Kuirk ini dibawa juga oleh salinan di kelas
 *       les, jadi bukan divergensi antar-berkas.</li>
 *   <li><b>Resolusi id lewat cache global.</b>
 *       {@code ConstantValues.ambil(JenisItemPenilaianSiswa.class.getName(), id)} dan padanannya
 *       untuk {@link GrupKategoriItemPenilaianSiswa} mengambil baris master <b>berdasarkan id
 *       mentah dari payload, tanpa pemeriksaan sekolah/yayasan</b>. Risikonya rendah karena id itu
 *       berasal dari baris milik siswa itu sendiri (bukan masukan klien), tetapi konsekuensinya
 *       payload yang tercemar akan diam-diam menarik master lintas tenant.</li>
 *   <li><b>Penulisan lewat setter, bukan field.</b> Kedua method {@code populate*} di sini
 *       memanggil {@link #setDetailNilai(String)}/{@link #setDetailNilaiTotal(String)}, sementara
 *       salinan lama di kelas les menulis langsung ke field. Perbedaan ini penting bagi turunan
 *       yang ingin menyisipkan validasi pada setter-nya.</li>
 *   <li><b>Tidak ada kueri, tidak ada cakupan tenant di berkas ini.</b> Kelas ini murni mengolah
 *       string milik baris yang sudah dimuat; pola <i>fail-open</i> cakupan tenant yang berulang di
 *       audit repo ini <b>tidak</b> hadir di sini — letaknya di helper pemanggil. Demikian pula
 *       {@code getNomorUrut()} (kunci pertama {@code compareTo()} yang meruntuhkan {@code TreeSet}
 *       bila di-<i>coalesce</i> ke {@code 0}) dideklarasikan di kelas turunan, bukan di sini.</li>
 * </ul>
 *
 * <h2>Catatan hak akses (hasil audit menyertai dokumentasi ini)</h2>
 * <p>Kedua layar yang menulis payload nilai lewat method di kelas ini <b>tidak memiliki gerbang
 * hak sama sekali</b>, dan keduanya rusak dengan cara yang persis sama:</p>
 * <ul>
 *   <li>{@code DetailPenilaianSiswaHelper} (kelas <b>reguler</b>, jalur nilai rapor) — kedua
 *       baris {@code CommonPrivilages.checkPrevilages(DELETE/CREATE)} pada constructor-nya
 *       <b>dikomentari</b>;</li>
 *   <li>{@code DetailPenilaianLesSiswaHelper} (kelas <b>les</b>) — sama persis, kedua baris
 *       {@code checkPrevilages} dikomentari.</li>
 * </ul>
 * <p>Akibatnya panel penilaian mewarisi hak dari menu induknya: pengguna yang hanya diberi hak
 * <b>baca</b> atas menu kelas/kelas les tetap dapat mengubah nilai, status verifikasi, dan catatan
 * rapor lewat setiap sel yang diedit maupun lewat unggah Excel massal. Ini instance ke sekian dari
 * pola berulang audit ini — kerusakan terkonsentrasi pada panel/helper DETAIL, sementara layar
 * MASTER-nya bergerbang benar. Sebagai catatan penyeimbang, penapisan baris pada
 * {@code DetailPenilaianSiswaHelper.initCriteria()} sendiri <b>tidak</b> fail-open: ia selalu
 * mengikat {@code kelasSiswa} yang sedang dibuka, dan mempersempit lagi ke anak-anak sendiri bila
 * pengguna yang login adalah orang tua.</p>
 *
 * @see ais.database.model.sekolah.KelasSiswaPunyaSiswa
 * @see ais.database.model.sekolah.KelasLesSiswaPunyaSiswa
 * @see ais.database.model.sekolah.KelasSiswa
 * @see ais.database.model.sekolah.JenisItemPenilaianSiswa
 * @see ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa
 * @see ais.action.master.sekolah.util.GrupPenilaianUtil
 * @see ais.database.model.GeneralValueObject
 */
public abstract class VoKelasPunyaSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Dipatok tetap agar instance turunan yang sudah ter-serialisasi
	 * (mis. tersimpan pada sesi ZK atau cache) tetap dapat dibaca setelah berkas ini berubah.
	 * Jangan diubah tanpa alasan kuat.
	 */
	private static final long serialVersionUID = -9043530115369882721L;

	/**
	 * Mengembalikan siswa pemilik baris roster ini.
	 *
	 * <p>Kontrak abstrak; turunan memetakannya ke relasi {@code @ManyToOne} masing-masing.
	 * <b>Perhatikan:</b> kedua implementasi yang ada bersifat <i>write-back</i> — bila kolom
	 * {@code calonSiswa} terisi dan calon siswa itu sudah punya {@link Siswa}, getter <b>menimpa</b>
	 * field {@code siswa} dengan {@code calonSiswa.getSiswa()}. Penugasan manual lewat
	 * {@code setSiswa()} karena itu dapat dikembalikan diam-diam begitu barisnya dibaca ulang.</p>
	 *
	 * <p>Di dalam kelas ini method tersebut tidak dipanggil sama sekali; ia bagian dari kontrak agar
	 * helper yang bekerja atas {@code List<? extends VoKelasPunyaSiswa>} (mis.
	 * {@code AbsensiSiswaHelper}, {@code DetailpertemuanHelper},
	 * {@code DetailJadwalMatapelajaranHelper}, {@code DetailPenilaianSiswaHelper}) dapat menampilkan
	 * identitas siswa tanpa perlu tahu jenis rosternya.</p>
	 *
	 * @return siswa anggota kelas; dapat {@code null} pada baris yang masih berupa calon siswa
	 */
	public abstract Siswa getSiswa();

	/**
	 * Mengembalikan daftar id {@link Matapelajaran} yang <b>tidak</b> diambil siswa ini (daftar
	 * pengecualian mata pelajaran).
	 *
	 * <p>Dipakai {@code KelasSiswaPunyaSiswa.filterMk()} untuk membuang siswa dari roster layar
	 * absensi/penilaian/pertemuan ketika mata pelajaran yang sedang dibuka termasuk pengecualian
	 * baginya. Diberi awalan {@code ambil} alih-alih {@code get} justru supaya Hibernate
	 * <b>tidak</b> memperlakukannya sebagai properti yang harus dipetakan.</p>
	 *
	 * <p><b>Dua implementasi berbeda jauh:</b> {@code KelasSiswaPunyaSiswa} menggabungkan
	 * pengecualian tingkat siswa dengan tingkat kelas, sedangkan {@code KelasLesSiswaPunyaSiswa}
	 * hanyalah <i>stub</i> yang selalu mengembalikan daftar kosong (kelas les tidak mengenal
	 * pengecualian mata pelajaran).</p>
	 *
	 * @return daftar id mata pelajaran yang dikecualikan; kosong bila tidak ada
	 */
	public abstract List<Long> ambilMk();

	/**
	 * Mengembalikan payload nilai <b>lapis 1</b> (nilai mentah per item penilaian) sebagai string
	 * apa adanya.
	 *
	 * <p>Format delapan ruas per entri diuraikan lengkap pada dokumentasi kelas. <b>Jangan mengurai
	 * string ini secara manual</b> — gunakan
	 * {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)},
	 * {@link #retreiveDetailVerify(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)},
	 * atau
	 * {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)}.</p>
	 *
	 * <p>Seluruh pembaca di kelas ini menoleransi hasil {@code null} (diperlakukan sebagai payload
	 * kosong), meski kedua implementasi menginisialisasi field-nya dengan string kosong.</p>
	 *
	 * @return payload nilai per item penilaian; boleh kosong maupun {@code null}
	 */
	public abstract String getDetailNilai();

	/**
	 * Menyetel payload nilai lapis 1 secara <b>utuh</b> (menggantikan seluruh isi lama).
	 *
	 * <p>Satu-satunya pemanggil di kelas ini adalah
	 * {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)},
	 * setelah payload disusun ulang entri demi entri. Menyetel string yang menyimpang dari format
	 * delapan ruas membuat entri bersangkutan dilewati diam-diam oleh semua pembaca, dan terhapus
	 * pada penulisan berikutnya — lihat catatan "entri rusak" pada dokumentasi kelas.</p>
	 *
	 * <p><b>Efek samping:</b> hanya mengubah state objek; penyimpanan ke basis data adalah tanggung
	 * jawab pemanggil (helper penilaian memanggil {@code Common.refreshUpdate} sesudahnya).</p>
	 *
	 * @param detailNilai payload nilai berformat seperti dijelaskan pada dokumentasi kelas
	 */
	public abstract void setDetailNilai(String detailNilai);

	/**
	 * Mengembalikan payload nilai <b>lapis 2</b> (nilai agregat per kategori penilaian) sebagai
	 * string apa adanya.
	 *
	 * <p>Tata letaknya identik dengan {@link #getDetailNilai()}, dengan satu perbedaan: ruas [0]
	 * (id item penilaian) selalu ditulis {@code "0"} karena entri agregat tidak terikat satu item
	 * penilaian. Pembacanya adalah
	 * {@link #retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)} dan
	 * {@link #retreiveTotalNilaiTotal(String, Matapelajaran, GrupPenilaian, Integer, List)}.</p>
	 *
	 * @return payload nilai agregat per kategori; boleh kosong maupun {@code null}
	 */
	public abstract String getDetailNilaiTotal();

	/**
	 * Menyetel payload nilai lapis 2 secara <b>utuh</b> (menggantikan seluruh isi lama).
	 *
	 * <p>Satu-satunya pemanggil di kelas ini adalah
	 * {@link #populateDetailNilaiTotal(Matapelajaran, GrupKategoriItemPenilaianSiswa, Double, Boolean, Integer)}.
	 * Berlaku peringatan yang sama seperti {@link #setDetailNilai(String)} soal entri yang tidak
	 * berformat delapan ruas.</p>
	 *
	 * @param detailNilaiTotal payload agregat berformat seperti dijelaskan pada dokumentasi kelas
	 */
	public abstract void setDetailNilaiTotal(String detailNilaiTotal);

	/**
	 * Mengembalikan catatan rapor <b>semester 1</b> untuk baris roster ini, berupa string JSON
	 * object.
	 *
	 * <p>Bagian dari kontrak abstrak semata — <b>tidak ada satu pun method di kelas ini yang
	 * memakainya</b>. Penggunanya adalah layar penilaian, yang memilih antara semester 1 dan 2
	 * dengan pola {@code smt == 1 ? getKeterangan1() : getKeterangan2()}.</p>
	 *
	 * <p>Kedua implementasi mengembalikan {@code "{}"} (JSON hampa) bila kolomnya masih {@code null},
	 * sehingga pemanggil selalu dapat langsung membungkusnya dengan {@code new JSONObject(...)}.
	 * Nilai pengganti itu <b>tidak</b> ditulis balik ke field, jadi getter ini bukan getter
	 * destruktif dan kolomnya tetap {@code NULL} di basis data.</p>
	 *
	 * @return catatan rapor semester 1 sebagai string JSON object; tidak pernah {@code null} pada
	 *         kedua implementasi yang ada
	 */
	public abstract String getKeterangan1();

	/**
	 * Mengembalikan catatan rapor <b>semester 2</b> untuk baris roster ini, berupa string JSON
	 * object. Berperilaku persis sama dengan {@link #getKeterangan1()}, termasuk pengganti
	 * {@code "{}"} saat kolomnya {@code null}.
	 *
	 * @return catatan rapor semester 2 sebagai string JSON object; tidak pernah {@code null} pada
	 *         kedua implementasi yang ada
	 */
	public abstract String getKeterangan2();

	/**
	 * Menyetel catatan rapor semester 1.
	 *
	 * <p>Dipanggil layar penilaian setelah pengguna menyunting catatan; isinya diharapkan berupa
	 * string JSON object. Tidak dipanggil dari dalam kelas ini.</p>
	 *
	 * @param keterangan1 string JSON object; {@code null} akan dibaca sebagai {@code "{}"} oleh
	 *                    {@link #getKeterangan1()}
	 */
	public abstract void setKeterangan1(String keterangan1);

	/**
	 * Menyetel catatan rapor semester 2.
	 *
	 * @param keterangan2 string JSON object; {@code null} akan dibaca sebagai {@code "{}"} oleh
	 *                    {@link #getKeterangan2()}
	 */
	public abstract void setKeterangan2(String keterangan2);

	/**
	 * Mengembalikan {@link KelasSiswa} pemilik baris roster ini, tanpa pemanggil perlu tahu apakah
	 * dirinya roster kelas reguler atau kelas les.
	 *
	 * <p>Inilah satu-satunya kontrak abstrak yang benar-benar dipakai oleh mesin nilai di kelas
	 * ini: {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)}
	 * dan
	 * {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)}
	 * memanggilnya untuk membaca {@code getPublikasiNilaiHarusTelahDiverifikasi()} — yaitu untuk
	 * memutuskan apakah nilai yang belum diverifikasi guru boleh ikut ditampilkan/dihitung.</p>
	 *
	 * <p><b>Kondisi {@code null} yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li>{@code KelasSiswaPunyaSiswa} mengembalikan {@code getKelasSiswa()} — normalnya
	 *       terisi, sehingga saklar verifikasi benar-benar dievaluasi untuk kelas reguler.</li>
	 *   <li>{@code KelasLesSiswaPunyaSiswa} adalah <i>stub</i> yang <b>selalu</b> mengembalikan
	 *       {@code null} (kelas les tidak punya {@link KelasSiswa}), sehingga penjaga verifikasi
	 *       versi kelas ini tidak akan pernah menyala bagi kelas les.</li>
	 * </ul>
	 * <p>Karena penjaganya ditulis {@code ambilKelasSiswa() != null && ...}, hasil {@code null}
	 * berarti parameter {@code hanyaValid} diteruskan apa adanya — <b>tidak</b> memicu
	 * {@code NullPointerException}.</p>
	 *
	 * @return kelas pemilik baris ini, atau {@code null} bila roster ini bukan roster kelas reguler
	 */
	public abstract KelasSiswa ambilKelasSiswa();
	
	/**
	 * Menyatakan apakah keanggotaan pada baris roster ini masih berlaku.
	 *
	 * <p>Kontrak abstrak; tidak dipanggil dari dalam kelas ini, tetapi dipakai
	 * {@code KelasSiswaPunyaSiswa.filterMk()} (yang bekerja atas {@code VoKelasPunyaSiswa}) untuk
	 * membuang anggota tidak aktif dari roster layar absensi/penilaian/pertemuan.</p>
	 *
	 * <p><b>Kedua implementasi berperilaku berbeda dan keduanya perlu diwaspadai:</b>
	 * {@code KelasSiswaPunyaSiswa} sekadar meng-<i>coalesce</i> {@code null} menjadi {@code true}
	 * (baris lama tanpa nilai dianggap aktif), sedangkan {@code KelasLesSiswaPunyaSiswa} memakai
	 * <b>getter destruktif</b>: ia menghitung ulang status lalu <b>menulis balik</b> ke field yang
	 * terpetakan, sehingga sekadar membaca baris dapat mengubah basis data. Karena kontraknya
	 * abstrak di sini, jangan mengasumsikan method ini murni.</p>
	 *
	 * @return {@code true} bila keanggotaan dianggap aktif
	 */
	public abstract Boolean getAktif();

	/**
	 * Membaca <b>satu</b> nilai mentah dari payload {@link #getDetailNilai()} untuk kombinasi item
	 * penilaian + kategori + mata pelajaran + semester tertentu.
	 *
	 * <p><b>Versi diperkeras.</b> Penguraian entri dilakukan lewat {@link #splitNilai(String)}
	 * sehingga ruas kosong dipertahankan dan entri bermalformasi ditolak dengan bersih (bukan
	 * lewat {@code ArrayIndexOutOfBoundsException}). Salinan lama yang <b>tidak</b> memakai penjaga
	 * ini masih hidup sebagai override di
	 * {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa} — lihat dokumentasi kelas.</p>
	 *
	 * <p><b>Cara kerja:</b> payload dipecah per {@code ;}, lalu tiap entri diuji empat syarat
	 * sekaligus (id item penilaian, id mata pelajaran, semester, id kategori) ditambah filter
	 * validitas. Entri pertama yang cocok langsung dikembalikan; sisanya tidak diperiksa.</p>
	 *
	 * <p><b>Perlakuan {@code hanyaValid}:</b> bila kelas pemilik ({@link #ambilKelasSiswa()}) ada
	 * dan saklar {@code getPublikasiNilaiHarusTelahDiverifikasi()} bernilai {@code false},
	 * parameter ini <b>dipaksa {@code null}</b> — filter validitas dimatikan dan nilai yang
	 * belum diverifikasi guru pun ikut tampil. Pada roster kelas les {@link #ambilKelasSiswa()}
	 * selalu {@code null} sehingga penjaga ini tidak pernah menyala di jalur induk.</p>
	 *
	 * <p><b>Kasus tepi:</b> {@code jenisItemPenilaianSiswa} {@code null} atau tanpa id langsung
	 * menghasilkan string kosong tanpa menyentuh payload; {@code matapelajaran} atau
	 * {@code grupKategoriItemPenilaianSiswa} {@code null} melempar
	 * {@code NullPointerException} yang <b>ditelan</b> {@code catch} per entri, sehingga hasilnya
	 * juga string kosong. Setiap kegagalan dicatat {@code ErrorAuditUtil} lalu iterasi
	 * berlanjut.</p>
	 *
	 * <p><b>Pemanggil:</b> layar penilaian kelas reguler ({@code DetailPenilaianSiswaHelper}) untuk
	 * mengisi tiap sel nilai pada grid, dan lewat override-nya layar penilaian kelas les.</p>
	 *
	 * @param jenisItemPenilaianSiswa        item penilaian yang dicari; {@code null}/tanpa id
	 *                                       langsung menghasilkan string kosong
	 * @param grupKategoriItemPenilaianSiswa kategori/grup penilaian yang harus cocok
	 * @param matapelajaran                  mata pelajaran yang harus cocok
	 * @param smt                            semester yang harus cocok
	 * @param hanyaValid                     {@code true}/{@code false} menyaring berdasarkan status
	 *                                       verifikasi; {@code null} berarti tanpa filter (dan
	 *                                       dapat dipaksa {@code null} oleh penjaga di atas)
	 * @return nilai sebagai teks apa adanya (bisa {@code "80"}, {@code "A:80"}, atau string
	 *         kosong), atau string kosong bila tidak ditemukan
	 */
	public String retreiveDetailNilai(JenisItemPenilaianSiswa jenisItemPenilaianSiswa,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Matapelajaran matapelajaran, Integer smt,
			Boolean hanyaValid) {

		if (ambilKelasSiswa() != null && !ambilKelasSiswa().getPublikasiNilaiHarusTelahDiverifikasi()) {
			hanyaValid = null;
		}

		if (jenisItemPenilaianSiswa != null && jenisItemPenilaianSiswa.getId() != null) {
			String[] nilais = getDetailNilai() == null ? new String[] {} : getDetailNilai().split(";");
			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Boolean valid = Boolean.parseBoolean(s[5]);
					Long grupId = Long.parseLong(s[7]);
					if ((hanyaValid == null || hanyaValid.equals(valid))
							&& jenisItemPenilaianSiswa.getId().equals(formatId)
							&& matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
							&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:70");

				}
			}
		}

		return "";
	}

	/**
	 * Membaca <b>status verifikasi</b> (ruas [5] payload) untuk satu kombinasi item penilaian +
	 * kategori + mata pelajaran + semester.
	 *
	 * <p><b>Versi diperkeras</b> — memakai {@link #splitNilai(String)}; salinan lama tanpa
	 * penjaga itu ada sebagai override di
	 * {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa}.</p>
	 *
	 * <p>Berbeda dari
	 * {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)},
	 * method ini <b>tidak</b> membaca saklar {@code publikasiNilaiHarusTelahDiverifikasi}: ia murni
	 * pelapor status, bukan penegak kebijakan.</p>
	 *
	 * <p><b>Kasus tepi penting:</b> {@code false} berarti dua hal yang tidak dapat dibedakan —
	 * "entri ditemukan tetapi belum diverifikasi" dan "entri tidak ada". Selain itu
	 * {@code Boolean.parseBoolean()} memperlakukan teks apa pun selain {@code "true"} (termasuk
	 * ruas rusak) sebagai {@code false}, jadi kerusakan payload selalu condong ke arah "belum
	 * diverifikasi".</p>
	 *
	 * <p><b>Pemanggil:</b> layar penilaian, untuk menampilkan penanda "sudah diverifikasi"; juga
	 * dipanggil ulang dari
	 * {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)}
	 * untuk mengisi variabel formula bersufiks {@code _s}.</p>
	 *
	 * @param jenisItemPenilaianSiswa        item penilaian yang dicari; {@code null}/tanpa id
	 *                                       menghasilkan {@code false}
	 * @param grupKategoriItemPenilaianSiswa kategori/grup penilaian yang harus cocok
	 * @param matapelajaran                  mata pelajaran yang harus cocok
	 * @param smt                            semester yang harus cocok
	 * @return {@code true} bila entri ditemukan dan bertanda terverifikasi; {@code false} bila
	 *         tidak ditemukan, belum diverifikasi, atau entri gagal diurai
	 */
	public Boolean retreiveDetailVerify(JenisItemPenilaianSiswa jenisItemPenilaianSiswa,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Matapelajaran matapelajaran, Integer smt) {

		if (jenisItemPenilaianSiswa != null && jenisItemPenilaianSiswa.getId() != null) {
			String[] nilais = getDetailNilai() == null ? new String[] {} : getDetailNilai().split(";");
			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);
					if (jenisItemPenilaianSiswa.getId().equals(formatId) && matapelajaran.getId().equals(matpelId)
							&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return Boolean.parseBoolean(s[5]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:95");

				}
			}
		}

		return false;
	}

	/**
	 * Menghitung nilai agregat <b>satu kategori penilaian</b> bagi siswa ini, dengan menjalankan
	 * formula {@link GrupPenilaianUtil#hitung} atas seluruh item penilaian yang relevan — lapis
	 * pertama mesin nilai.
	 *
	 * <p><b>Cara kerja:</b> setiap item pada {@code jenisItemPenilaianSiswas} lebih dulu didaftarkan
	 * ke peta variabel formula dengan nilai awal {@code "0.0"}, berikut pasangan bersufiks
	 * {@code _s} untuk status verifikasinya; dengan begitu formula tetap dapat dievaluasi walau
	 * sebagian item belum dinilai. Payload {@link #getDetailNilai()} lalu ditelusuri, dan setiap
	 * entri yang cocok mata pelajaran + semester + kategori (serta lolos filter {@code hanyaValid})
	 * menimpa variabel bersangkutan. Terakhir formula {@code target} dijalankan dengan stempel
	 * waktu {@code WaktuUtil.getDate()}.</p>
	 *
	 * <p><b>Versi diperkeras — dua penjaga sekaligus:</b> entri diurai lewat
	 * {@link #splitNilai(String)}, dan nilainya dilewatkan {@link #nilaiUntukFormula(String)}
	 * sebelum masuk peta variabel, sehingga nilai bertipe pilihan seperti {@code "A:80"} menjadi
	 * {@code "80"} dan nilai kosong menjadi {@code "0.0"}. Salinan lama di
	 * {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa} memasukkan {@code s[2]} apa adanya
	 * — itulah sebabnya formula total kelas les bisa gagal pada kasus yang di kelas reguler
	 * sudah beres.</p>
	 *
	 * <p><b>Kunci peta adalah {@code getKode()} item penilaian</b>, bukan id. Dua item penilaian
	 * berkode sama karena itu akan saling menimpa di dalam satu perhitungan.</p>
	 *
	 * <p><b>Perlakuan {@code hanyaValid}</b> sama seperti
	 * {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)}:
	 * dipaksa {@code null} bila kelas pemilik tidak mensyaratkan verifikasi.</p>
	 *
	 * <p><b>Kasus tepi:</b> {@code matapelajaran} {@code null} atau tanpa id langsung mengembalikan
	 * {@code 0.0} tanpa menyentuh payload maupun formula. Entri yang item penilaiannya tidak lagi
	 * ada di cache {@code ConstantValues} memicu {@code NullPointerException} yang ditelan
	 * {@code catch}, sehingga entri itu hilang diam-diam dari perhitungan.</p>
	 *
	 * @param jenisItemPenilaianSiswas       daftar item penilaian yang menjadi variabel formula
	 * @param target                         nama/target formula yang dievaluasi
	 *                                       {@link GrupPenilaianUtil}
	 * @param matapelajaran                  mata pelajaran; {@code null}/tanpa id menghasilkan
	 *                                       {@code 0.0}
	 * @param grupPenilaian                  grup penilaian pemilik formula
	 * @param grupKategoriItemPenilaianSiswa kategori yang entri-entrinya diperhitungkan
	 * @param smt                            semester yang diperhitungkan
	 * @param hanyaValid                     filter status verifikasi; {@code null} berarti tanpa
	 *                                       filter
	 * @return hasil formula; {@code 0.0} bila mata pelajaran tidak valid
	 * @throws Exception diteruskan apa adanya dari {@link GrupPenilaianUtil#hitung}
	 */
	public Double retreiveTotalNilai(List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas, String target,
			Matapelajaran matapelajaran, GrupPenilaian grupPenilaian,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Integer smt, Boolean hanyaValid)
			throws Exception {

		if (ambilKelasSiswa() != null && !ambilKelasSiswa().getPublikasiNilaiHarusTelahDiverifikasi()) {
			hanyaValid = null;
		}

		Double total = 0.0;
		if (matapelajaran != null && matapelajaran.getId() != null) {
			String[] nilais = getDetailNilai() == null ? new String[] {} : getDetailNilai().split(";");
			Map<String, String> data = new HashMap<String, String>();

			for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {
				if (jenisItemPenilaianSiswa != null) {
					data.put(jenisItemPenilaianSiswa.getKode(), "0.0");
					data.put(jenisItemPenilaianSiswa.getKode() + "_s", "0.0");
				}
			}

			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}

					Long matpelId = Long.parseLong(s[1]);

					Integer smtId = Integer.parseInt(s[6]);
					Boolean valid = Boolean.parseBoolean(s[5]);
					Long grupId = Long.parseLong(s[7]);

					if ((hanyaValid == null || hanyaValid.equals(valid)) && matapelajaran.getId().equals(matpelId)
							&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						String n = nilaiUntukFormula(s[2]);
						JenisItemPenilaianSiswa jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) ConstantValues
								.ambil(JenisItemPenilaianSiswa.class.getName(), Long.parseLong(s[0]));
						data.put(jenisItemPenilaianSiswa.getKode(), n);

						boolean sesuai = retreiveDetailVerify(jenisItemPenilaianSiswa, grupKategoriItemPenilaianSiswa,
								matapelajaran, smt);
						data.put(jenisItemPenilaianSiswa.getKode() + "_s", sesuai ? "1.0" : "0.0");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:146");
//					e.printStackTrace();
				}
			}

			Date sekarang = WaktuUtil.getDate();

//			System.out.println("data -> " + data + " " + target);
			total = GrupPenilaianUtil.hitung(data, matapelajaran, target, grupPenilaian, null, sekarang, null);
		}

		return total;
	}

	/**
	 * Menyisipkan atau memperbarui <b>satu</b> nilai di dalam payload {@link #getDetailNilai()},
	 * dengan menulis ulang seluruh payload secara utuh.
	 *
	 * <p><b>Cara kerja:</b> payload lama diuraikan entri demi entri; entri yang cocok kombinasi item
	 * penilaian + mata pelajaran + semester + kategori diganti entri baru, entri lain disalin apa
	 * adanya. Bila tidak ada yang cocok, entri baru <b>ditambahkan</b> di akhir. Hasil akhirnya
	 * ditulis lewat {@link #setDetailNilai(String)} — berbeda dari salinan lama di
	 * {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa} yang menulis langsung ke
	 * field-nya.</p>
	 *
	 * <p><b>Sanitasi masukan (urutannya penting):</b></p>
	 * <ol>
	 *   <li>{@code jumlah} yang berisi spasi saja memaksa {@code verify = false} — nilai kosong
	 *       tidak boleh berstatus terverifikasi;</li>
	 *   <li><b>{@code jumlah = jumlah == null ? "" : jumlah;}</b> — inilah baris pengerasan yang
	 *       hilang dari salinan kelas les. Tanpa baris ini {@code null} lolos melewati
	 *       {@code StringUtils.replace} (yang mengembalikan {@code null}) dan berakhir tersimpan
	 *       sebagai <b>teks literal</b> {@code "null"} pada ruas [2];</li>
	 *   <li>karakter {@code |} diganti spasi dan {@code ;} diganti koma agar nilai yang diketik
	 *       pengguna tidak merusak pemisah format.</li>
	 * </ol>
	 *
	 * <p><b>Kehilangan data yang perlu disadari:</b> entri lama yang ditolak
	 * {@link #splitNilai(String)} (kurang dari 8 ruas) atau yang ruas [0]-nya kosong <b>tidak ikut
	 * disalin</b> ke payload baru. Karena method ini menulis ulang payload secara utuh, entri
	 * semacam itu terhapus permanen. Ini konsekuensi yang disengaja dari perbaikan format, tetapi
	 * berarti penyimpanan pertama setelah perbaikan dapat memangkas entri warisan yang rusak.</p>
	 *
	 * <p><b>Efek samping:</b> hanya mengubah state objek — tidak menyimpan ke basis data;
	 * pemanggil ({@code DetailPenilaianSiswaHelper}) bertanggung jawab memanggil
	 * {@code Common.refreshUpdate} sesudahnya. Dipanggil dari setiap sel nilai yang disunting di
	 * layar penilaian dan dari unggah Excel massal — <b>keduanya tanpa gerbang hak</b>, lihat
	 * catatan hak akses pada dokumentasi kelas.</p>
	 *
	 * @param jenisItemPenilaianSiswa        item penilaian yang nilainya ditulis; {@code null}
	 *                                       membuat method tidak melakukan apa pun
	 * @param matapelajaran                  mata pelajaran terkait
	 * @param grupKategoriItemPenilaianSiswa kategori/grup penilaian terkait
	 * @param jumlah                         nilai yang ditulis (teks bebas, disanitasi seperti di
	 *                                       atas); {@code null} aman dan menjadi string kosong
	 * @param verify                         status verifikasi yang ikut disimpan
	 * @param smt                            semester terkait
	 */
	public void populateDetailNilai(JenisItemPenilaianSiswa jenisItemPenilaianSiswa, Matapelajaran matapelajaran,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, String jumlah, Boolean verify, Integer smt) {
		if (jumlah != null && jumlah.trim().isEmpty()) {
			verify = false;
		}
		jumlah = jumlah == null ? "" : jumlah;
		jumlah = org.apache.commons.lang3.StringUtils.replace(jumlah, "|", " ");
		jumlah = org.apache.commons.lang3.StringUtils.replace(jumlah, ";", ",");
		if (jenisItemPenilaianSiswa != null) {
			String formatBaru = "";
			String[] nilais = getDetailNilai() == null ? new String[] {} : getDetailNilai().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						Long matpelId = Long.parseLong(s[1]);
						Integer smtId = Integer.parseInt(s[6]);
						Long grupId = Long.parseLong(s[7]);
						if (jenisItemPenilaianSiswa.getId().equals(formatId) && matapelajaran.getId().equals(matpelId)
								&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
							aformatBaru = jenisItemPenilaianSiswa.getId() + "|" + matapelajaran.getId() + "|" + jumlah
									+ "|0|0|" + verify + "|" + smt + "|" + grupKategoriItemPenilaianSiswa.getId();
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:192");
				}
			}

			if (!ada) {
				String aformatBaru = jenisItemPenilaianSiswa.getId() + "|" + matapelajaran.getId() + "|" + jumlah
						+ "|0|0|" + verify + "|" + smt + "|" + grupKategoriItemPenilaianSiswa.getId();
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			setDetailNilai(formatBaru);
		}
	}

	/**
	 * Membaca <b>satu</b> nilai agregat dari payload {@link #getDetailNilaiTotal()} untuk kombinasi
	 * kategori + mata pelajaran + semester — padanan lapis 2 dari
	 * {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)}.
	 *
	 * <p><b>Versi diperkeras</b> — memakai {@link #splitNilai(String)}. Tidak ada filter
	 * validitas di sini: nilai agregat memang tidak pernah bertanda terverifikasi (lihat kuirk
	 * {@code verify} pada
	 * {@link #populateDetailNilaiTotal(Matapelajaran, GrupKategoriItemPenilaianSiswa, Double, Boolean, Integer)}).</p>
	 *
	 * <p>Ruas [0] entri agregat selalu {@code "0"} dan sengaja tidak diperiksa; pencocokan memakai
	 * ruas [1] (mata pelajaran), [6] (semester), dan [7] (kategori).</p>
	 *
	 * <p><b>Kasus tepi:</b> {@code grupKategoriItemPenilaianSiswa} {@code null} atau tanpa id
	 * langsung menghasilkan string kosong; kegagalan parse per entri ditelan {@code catch} dan
	 * dicatat {@code ErrorAuditUtil}.</p>
	 *
	 * @param grupKategoriItemPenilaianSiswa kategori yang dicari; {@code null}/tanpa id
	 *                                       menghasilkan string kosong
	 * @param matapelajaran                  mata pelajaran yang harus cocok
	 * @param smt                            semester yang harus cocok
	 * @return nilai agregat sebagai teks, atau string kosong bila tidak ditemukan
	 */
	public String retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa,
			Matapelajaran matapelajaran, Integer smt) {

		if (grupKategoriItemPenilaianSiswa != null && grupKategoriItemPenilaianSiswa.getId() != null) {
			String[] nilais = getDetailNilaiTotal() == null ? new String[] {} : getDetailNilaiTotal().split(";");
			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);
					if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
							&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:221");

				}
			}
		}

		return "";
	}

	/**
	 * Menghitung nilai akhir <b>satu grup penilaian</b> dengan menjalankan formula
	 * {@link GrupPenilaianUtil#hitung} atas nilai-nilai <b>agregat per kategori</b> — lapis
	 * kedua mesin nilai, bertumpu di atas hasil
	 * {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)}
	 * yang sebelumnya sudah disimpan ke {@link #getDetailNilaiTotal()}.
	 *
	 * <p><b>Cara kerja:</b> seluruh kategori pada {@code grupKategoriItemPenilaianSiswas}
	 * didaftarkan lebih dulu ke peta variabel formula dengan nilai awal {@code "0.0"}, lalu ditimpa
	 * entri payload yang cocok. Kategori tiap entri diresolusi lewat cache
	 * {@code ConstantValues.ambil()} memakai id pada ruas [7], sehingga kategori <b>di luar</b>
	 * daftar parameter pun ikut menambah variabel formula.</p>
	 *
	 * <p><b>Asimetri filter yang disengaja:</b> berbeda dari
	 * {@link #retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)},
	 * pencocokan di sini hanya memakai mata pelajaran + semester dan <b>tidak</b> menyaring
	 * kategori — memang harus begitu, karena seluruh kategori dibutuhkan sebagai variabel
	 * formula sekaligus.</p>
	 *
	 * <p><b>Versi diperkeras:</b> memakai {@link #splitNilai(String)} dan melewatkan nilai lewat
	 * {@link #nilaiUntukFormula(String)}; salinan lama di
	 * {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa} tidak punya keduanya.</p>
	 *
	 * <p><b>Kasus tepi:</b> {@code matapelajaran} {@code null}/tanpa id mengembalikan {@code 0.0}.
	 * Kategori yang idnya tidak lagi ada di cache memicu {@code NullPointerException} yang ditelan
	 * {@code catch}, sehingga entri itu hilang dari perhitungan tanpa pesan. Kunci peta adalah
	 * {@code getKode()} kategori, jadi kategori berkode sama saling menimpa.</p>
	 *
	 * @param target                          nama/target formula yang dievaluasi
	 * @param matapelajaran                   mata pelajaran; {@code null}/tanpa id menghasilkan
	 *                                        {@code 0.0}
	 * @param grupPenilaian                   grup penilaian pemilik formula
	 * @param smt                             semester yang diperhitungkan
	 * @param grupKategoriItemPenilaianSiswas daftar kategori yang menjadi variabel formula
	 * @return hasil formula; {@code 0.0} bila mata pelajaran tidak valid
	 * @throws Exception diteruskan apa adanya dari {@link GrupPenilaianUtil#hitung}
	 */
	public Double retreiveTotalNilaiTotal(String target, Matapelajaran matapelajaran, GrupPenilaian grupPenilaian,
			Integer smt, List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas) throws Exception {
		Double total = 0.0;
		if (matapelajaran != null && matapelajaran.getId() != null) {
			String[] nilais = getDetailNilaiTotal() == null ? new String[] {} : getDetailNilaiTotal().split(";");
			Map<String, String> data = new HashMap<String, String>();

			for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
				if (grupKategoriItemPenilaianSiswa != null) {
					data.put(grupKategoriItemPenilaianSiswa.getKode(), "0.0");
				}
			}

			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}

					Long matpelId = Long.parseLong(s[1]);

					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);

					if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)) {
						String n = nilaiUntukFormula(s[2]);
						GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) ConstantValues
								.ambil(GrupKategoriItemPenilaianSiswa.class.getName(), grupId);
						data.put(grupKategoriItemPenilaianSiswa.getKode(), n);

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:259");
//					e.printStackTrace();
				}
			}

			Date sekarang = WaktuUtil.getDate();

//			System.out.println("retreiveTotalNilaiTotal data -> " + data + " " + target);
			total = GrupPenilaianUtil.hitung(data, matapelajaran, target, grupPenilaian, null, sekarang, null);
		}

		return total;
	}

	/**
	 * Menyisipkan atau memperbarui <b>satu</b> nilai agregat kategori di dalam payload
	 * {@link #getDetailNilaiTotal()}, dengan menulis ulang seluruh payload secara utuh.
	 *
	 * <p>Mekanismenya sejajar dengan
	 * {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}:
	 * entri yang cocok mata pelajaran + semester + kategori diganti, sisanya disalin, dan bila tak
	 * ada yang cocok entri baru ditambahkan di akhir. Ruas [0] entri agregat selalu ditulis
	 * {@code "0"}. Hasilnya ditulis lewat {@link #setDetailNilaiTotal(String)}.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> penjaga di awal method berbunyi
	 * {@code if (jumlah != null) { verify = false; }} — kebalikan dari kerabat lapis 1 yang
	 * memaksa {@code verify = false} hanya ketika nilainya <i>kosong</i>. Praktisnya, setiap kali
	 * nilai agregat ditulis dengan angka status verifikasinya <b>selalu dipaksa {@code false}</b>;
	 * parameter {@code verify} baru berpengaruh ketika {@code jumlah} bernilai {@code null}.
	 * Salinan lama di {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa} membawa kuirk yang
	 * sama, jadi ini bukan divergensi antar-berkas melainkan perilaku warisan.</p>
	 *
	 * <p><b>Perhatikan juga:</b> {@code jumlah} bertipe {@link Double} dan dirangkai lewat
	 * penyambungan string, sehingga {@code null} tersimpan sebagai teks {@code "null"} dan angka
	 * tersimpan dengan format {@code Double.toString()} (mis. {@code "80.0"}, atau notasi
	 * ilmiah untuk nilai ekstrem). Baris pengerasan {@code null}-ke-string-kosong yang ada pada
	 * kerabat lapis 1 <b>tidak</b> punya padanan di sini.</p>
	 *
	 * <p><b>Kehilangan data:</b> sama seperti kerabatnya, entri yang ditolak
	 * {@link #splitNilai(String)} atau yang ruas [0]-nya kosong tidak disalin ulang dan karenanya
	 * terhapus permanen.</p>
	 *
	 * <p><b>Efek samping:</b> hanya mengubah state objek; penyimpanan menjadi tanggung jawab
	 * pemanggil ({@code DetailPenilaianSiswaHelper}, tanpa gerbang hak — lihat dokumentasi
	 * kelas).</p>
	 *
	 * @param matapelajaran                  mata pelajaran terkait
	 * @param grupKategoriItemPenilaianSiswa kategori yang nilai agregatnya ditulis; {@code null}
	 *                                       membuat method tidak melakukan apa pun
	 * @param jumlah                         nilai agregat yang ditulis
	 * @param verify                         status verifikasi — lihat kuirk di atas
	 * @param smt                            semester terkait
	 */
	public void populateDetailNilaiTotal(Matapelajaran matapelajaran,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Double jumlah, Boolean verify, Integer smt) {
		if (jumlah != null) {
			verify = false;
		}
		if (grupKategoriItemPenilaianSiswa != null) {
			String formatBaru = "";
			String[] nilais = getDetailNilaiTotal() == null ? new String[] {} : getDetailNilaiTotal().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					if (!s[0].trim().isEmpty()) {
						Long matpelId = Long.parseLong(s[1]);
						Integer smtId = Integer.parseInt(s[6]);
						Long grupId = Long.parseLong(s[7]);
						if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
								&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
							aformatBaru = "0|" + matapelajaran.getId() + "|" + jumlah + "|0|0|" + verify + "|" + smt
									+ "|" + grupKategoriItemPenilaianSiswa.getId();
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:302");
				}
			}

			if (!ada) {
				String aformatBaru = "0|" + matapelajaran.getId() + "|" + jumlah + "|0|0|" + verify + "|" + smt + "|"
						+ grupKategoriItemPenilaianSiswa.getId();
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			setDetailNilaiTotal(formatBaru);
		}
	}

	/**
	 * Mengurai satu entri payload menjadi delapan ruas — <b>penjaga inti hasil pengerasan</b>
	 * yang membedakan berkas ini dari salinan lama di
	 * {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa}.
	 *
	 * <p>Dua hal yang dikerjakannya, dan keduanya penting:</p>
	 * <ol>
	 *   <li>Memakai {@code StringUtils.splitPreserveAllTokens} alih-alih
	 *       {@code StringUtils.split}. Varian biasa <b>membuang ruas kosong</b>, sehingga entri
	 *       bernilai kosong seperti {@code "12|34||0|0|false|1|56"} akan menyusut menjadi tujuh ruas
	 *       dan seluruh indeks setelahnya bergeser satu — nilai terbaca dari kolom yang salah,
	 *       atau {@code ArrayIndexOutOfBoundsException}. Varian {@code preserveAllTokens}
	 *       mempertahankan ruas kosong sehingga indeks tetap sahih.</li>
	 *   <li>Menolak entri yang ruasnya <b>kurang dari delapan</b> dengan mengembalikan {@code null},
	 *       sehingga pemanggil dapat melewatinya lewat {@code continue} alih-alih mengandalkan
	 *       exception yang tertelan.</li>
	 * </ol>
	 *
	 * <p><b>Catatan implementasi:</b> loop penormalan {@code null} menjadi string kosong pada
	 * indeks 0..7 bersifat <i>defensif</i> — {@code splitPreserveAllTokens} tidak pernah
	 * menghasilkan elemen {@code null} — tetapi dibiarkan agar kontrak "delapan ruas, tak satu
	 * pun {@code null}" berlaku mutlak bagi pemanggil. Ruas <b>berlebih</b> (lebih dari delapan)
	 * dibiarkan apa adanya dan tidak pernah dibaca.</p>
	 *
	 * <p>Bersifat {@code private}: turunan <b>tidak dapat</b> memanggilnya. Inilah sebab teknis
	 * mengapa override di kelas les terpaksa mengulang penguraiannya sendiri alih-alih memakai
	 * ulang penjaga ini.</p>
	 *
	 * @param nilai satu entri payload (teks antar-{@code ;}); boleh {@code null}
	 * @return array berisi minimal delapan ruas tanpa elemen {@code null}, atau {@code null} bila
	 *         entri tidak memenuhi syarat dan harus dilewati
	 */
	private String[] splitNilai(String nilai) {
		String[] s = StringUtils.splitPreserveAllTokens(nilai, "|");
		if (s == null || s.length < 8) {
			return null;
		}
		for (int i = 0; i < 8; i++) {
			if (s[i] == null) {
				s[i] = "";
			}
		}
		return s;
	}

	/**
	 * Menormalkan satu nilai menjadi teks angka yang aman dimasukkan ke mesin formula
	 * {@link GrupPenilaianUtil#hitung} — <b>penjaga kedua hasil pengerasan</b>, dan tidak punya
	 * padanan pada salinan lama di {@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa}.
	 *
	 * <p>Tiga jalur keputusan, berurutan:</p>
	 * <ol>
	 *   <li>Nilai {@code null} atau berisi spasi saja &rarr; {@code "0.0"} (item yang belum dinilai
	 *       tidak boleh merobohkan formula).</li>
	 *   <li>Nilai yang sudah numerik &rarr; dikembalikan dalam bentuk ter-{@code trim}.</li>
	 *   <li>Nilai bertipe <b>pilihan</b> berformat {@code "label:angka"} (mis. {@code "A:80"},
	 *       yang dihasilkan item penilaian bertipe pilihan) &rarr; bagian setelah titik dua diambil
	 *       dan dikembalikan bila memang numerik.</li>
	 * </ol>
	 * <p>Bila ketiganya gagal, nilainya dianggap {@code "0.0"} dan kejadiannya dicatat
	 * {@code ErrorAuditUtil} — sehingga nilai yang salah ketik tampak sebagai nol pada rapor,
	 * bukan sebagai kegagalan perhitungan.</p>
	 *
	 * <p><b>Kasus tepi:</b> pemisah dicari dengan {@code StringUtils.split(nilai, ":")} yang di
	 * sini justru <b>tepat</b> — label kosong atau bagian angka kosong menghasilkan array
	 * pendek dan jatuh ke jalur {@code "0.0"}. Nilai dengan lebih dari satu titik dua hanya diambil
	 * potongan keduanya; sisanya diabaikan.</p>
	 *
	 * <p>Dipanggil dari
	 * {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)}
	 * dan {@link #retreiveTotalNilaiTotal(String, Matapelajaran, GrupPenilaian, Integer, List)}.
	 * Bersifat {@code private}, jadi tidak terjangkau turunan — lihat catatan pada
	 * {@link #splitNilai(String)}.</p>
	 *
	 * @param nilai nilai mentah dari ruas [2] payload; boleh {@code null}
	 * @return teks yang dijamin dapat diurai sebagai {@code Double}; {@code "0.0"} bila tidak ada
	 *         angka yang dapat diambil
	 */
	private String nilaiUntukFormula(String nilai) {
		if (nilai == null || nilai.trim().isEmpty()) {
			return "0.0";
		}
		try {
			Double.parseDouble(nilai.trim());
			return nilai.trim();
		} catch (Exception e) {
			String[] pasangan = StringUtils.split(nilai, ":");
			if (pasangan != null && pasangan.length > 1) {
				try {
					Double.parseDouble(pasangan[1].trim());
					return pasangan[1].trim();
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex,
							"auto-audit(nilai pilihan bukan angka) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:nilaiUntukFormula");
				}
			}
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(nilai non angka dianggap 0 untuk formula) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:nilaiUntukFormula");
			return "0.0";
		}
	}

}
