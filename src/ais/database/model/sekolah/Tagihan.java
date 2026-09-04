package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.sekolah.helper.TagihanUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.PostingHistory;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Baris <b>kewajiban finansial aktual</b> seorang siswa (atau calon siswa) atas satu
 * komponen biaya, pada satu periode, untuk satu urutan angsuran. Tabel
 * <code>sekolah.tagihan</code> adalah <b>simpul paling ujung sekaligus paling sentral</b>
 * dari seluruh rantai billing sekolah: di sinilah tarif yang tadinya masih berupa
 * konfigurasi berubah menjadi angka rupiah yang benar-benar ditagihkan, dicetak di kuitansi,
 * dikirim ke bank sebagai Virtual Account, dan dicocokkan dengan uang masuk.
 *
 * <h3>Posisi dalam rantai billing (terverifikasi dari kode, bukan dugaan)</h3>
 * <pre>
 *   {@link ais.database.model.sekolah.JenisBiayaSekolah}      (perilaku billing: periode, gunakanCalonSiswa)
 *        &rarr; {@link ais.database.model.sekolah.PengaturanBiaya}         (paket biaya per TA/angkatan/jurusan)
 *        &rarr; {@link ais.database.model.sekolah.PengaturanBiayaItemBiaya} (komponen apa saja + batas min/maks)
 *        &rarr; {@link ais.database.model.sekolah.ItemBiayaSekolah}        (katalog komponen: SPP, Uang Gedung, ...)
 *        &rarr; {@link ais.database.model.sekolah.NominalBiaya}            (tarif termaterialisasi per siswa/periode)
 *        &rarr; <b>Tagihan</b> (KELAS INI)                                 (kewajiban rupiah per angsuran)
 *        &rarr; {@link ais.database.model.sekolah.PembayaranSiswaDetail}   (baris pelunasan)
 *        &rarr; {@link ais.database.model.sekolah.PembayaranSiswa}         (kuitansi/nota pembayaran)
 * </pre>
 * Perbedaan peran dengan tetangga terdekatnya:
 * <ul>
 *   <li>{@link ais.database.model.sekolah.NominalBiaya} menjawab <i>"berapa tarif komponen
 *       ini untuk siswa ini pada periode ini"</i>. Satu <code>NominalBiaya</code> dapat
 *       memayungi BEBERAPA <code>Tagihan</code> bila komponen tersebut dicicil
 *       (<code>dibayarSebayak &gt; 1</code>): satu baris <code>Tagihan</code> per angsuran,
 *       dibedakan oleh {@link #getBayarKe()}.</li>
 *   <li><code>Tagihan</code> menjawab <i>"berapa yang harus dibayar SEKARANG untuk cicilan
 *       ke-N, sudah dipotong diskon, ditambah denda, dan apakah masih aktif"</i>.</li>
 *   <li>{@link ais.database.model.sekolah.PembayaranSiswaDetail} menjawab <i>"berapa yang
 *       BENAR-BENAR masuk"</i>. Relasi <code>Tagihan &harr; PembayaranSiswaDetail</code>
 *       dipetakan <code>unique</code> — satu tagihan hanya boleh punya satu baris pelunasan.</li>
 * </ul>
 *
 * <h3>Kunci identitas: <code>kode_unik</code></h3>
 * Identitas logis sebuah tagihan BUKAN kolom <code>id</code>, melainkan
 * {@link #getKodeUnik()} yang dibentuk {@link #genCode} dari
 * (item biaya, pengaturan biaya, tahunbulan, siswa/calon siswa, bayarKe). Seluruh jalur
 * pembuatan tagihan ({@link #ambilAtauBuat}, {@link #buatAtauLoadTagihan},
 * <code>TagihanUtil</code>, <code>TagihanUtilCalonSiswa</code>, servlet <code>/Api</code>)
 * memakai kunci ini untuk memutuskan "ambil yang sudah ada" versus "buat baru", dan cache
 * global <code>MemoryDbUtil.getAllTagihan()</code> juga di-key olehnya.
 * <p>
 * <b>Kuirk mapping:</b> anotasi kolomnya menyatakan <code>unique = false</code>, padahal
 * database sungguhan memiliki indeks unik bernama <code>tagihan_kode_unik_key</code> — nama
 * itu dicari secara literal oleh {@link #isDuplicateKodeUnikException(Throwable)} untuk
 * membedakan tabrakan kunci dari error lain. Jadi jaminan keunikan ditegakkan oleh DB,
 * bukan oleh mapping.
 *
 * <h3>Entity ini "hidup": hampir semua getter menghitung ulang DAN menulis balik</h3>
 * Entity ini dipetakan dengan <b>property access</b> (anotasi ada di getter). Konsekuensinya
 * Hibernate memanggil getter/setter SETIAP kali baris dimuat, di-refresh, dan di-flush —
 * <b>bukan hanya</b> saat pengguna benar-benar mengubah sesuatu. Karena hampir seluruh getter
 * di kelas ini menghitung ulang nilainya lalu <i>menyimpan hasilnya ke field</i>, sekadar
 * <b>MEMBACA</b> sebuah tagihan sudah cukup untuk mengubah isi baris di database pada flush
 * berikutnya. Daftar getter yang menulis balik: {@link #getBulan()}, {@link #getTahun()},
 * {@link #getTahunbulan()}, {@link #getBayarKe()}, {@link #getNominal()},
 * {@link #getKodeUnik()}, {@link #getAktif()}, {@link #getDenda()}, {@link #getDiskon()},
 * {@link #getDibayar()}, {@link #getTanggalTagihan()}, {@link #getTanggalDeadline()},
 * {@link #getTanggalBayar()}, {@link #getTahunAjaran()}, {@link #getTahunAngkatan()},
 * {@link #getSekolah()}, {@link #getSiswa()}, {@link #getCalonSiswa()},
 * {@link #getPengaturanBiaya()}, {@link #getKelasSiswa()}, {@link #getBiayaTemporary()},
 * {@link #getPembayaranPada()}, {@link #getPembayaranBerakhirPada()},
 * {@link #getPembayaranSiswaDetail()}, {@link #getDiskonSiswa()},
 * {@link #getDiskonSiswaAsli()}.
 * <p>
 * Tiga di antaranya bersifat <b>destruktif</b> (bukan sekadar menghitung ulang, tapi
 * MENGHAPUS informasi yang tak dapat dipulihkan) — lihat bagian berikutnya.
 *
 * <h3>Getter destruktif atas data finansial</h3>
 * <ol>
 *   <li>{@link #getPembayaranSiswaDetail()} — bila baris pelunasan yang tertaut ternyata
 *       bernominal 0, referensinya <b>diputus</b> (<code>pembayaranSiswaDetail = null</code>).
 *       Pada flush berikutnya kolom <code>pembayaran_siswa_detail_id</code> menjadi NULL:
 *       tagihan kembali dianggap belum dibayar dan jejak ke kuitansinya hilang.</li>
 *   <li>{@link #getDiskonSiswa()} — mengosongkan <code>diskonSiswa</code> secara permanen
 *       untuk angsuran ke-2 ke atas, dan juga ketika masa berlaku diskon sudah lewat
 *       (lihat {@link #isDiskonSiswaMasihBerlaku}). Artinya begitu periode diskon berakhir,
 *       cukup <i>membuka layar</i> tagihan lama untuk menghapus catatan "diskon apa yang
 *       dahulu dipakai". Kolom cadangan {@link #getDiskonSiswaAsli()} memang ada untuk
 *       menyelamatkan informasi ini, tetapi pemulihannya bergantung urutan pemanggilan
 *       getter (lihat catatan di method tersebut).</li>
 *   <li>{@link #getBayarKe()} — memaksa <code>bayarKe = 1</code> untuk periode "Bulanan" dan
 *       memangkas nilai &gt; 500 menjadi 500. Bila periode sebuah jenis biaya diubah menjadi
 *       "Bulanan" pada instalasi berjalan, seluruh penomoran angsuran yang ada rata menjadi 1
 *       begitu barisnya tersentuh, dan {@link #getKodeUnik()} ikut berubah.</li>
 * </ol>
 * Pola ini sama dengan yang tercatat pada <code>ItemBiayaSekolah.getKelamin()</code> dan
 * <code>NominalBiaya.getNominal()</code>, tetapi di kelas ini akibatnya langsung menyentuh
 * angka rupiah dan status lunas.
 *
 * <h3>Bug produksi yang sudah didokumentasikan di dalam kode</h3>
 * {@link #setNominal(Double)} memuat komentar perbaikan atas bug nyata "tagihan bertambah
 * terus tiap klik Cari". Ringkasnya: setter itu dahulu ikut menulis
 * <code>nominalBiaya.setNominal(nominal + getDiskon())</code>, sementara {@link #getDiskon()}
 * pada kondisi tertentu justru DIHITUNG DARI <code>nominalBiaya.getNominal()</code> — sehingga
 * terbentuk umpan balik yang membesarkan tagihan setiap kali entity dimuat ulang. Komentar
 * aslinya sengaja dipertahankan utuh; jangan hapus.
 *
 * <h3>Terjangkau langsung dari endpoint bank &amp; API tanpa/kurang autentikasi</h3>
 * Diverifikasi ulang dari sisi entity ini (bukan sekadar mewarisi temuan
 * {@link ais.database.model.sekolah.NominalBiaya}): baris <code>Tagihan</code>
 * <b>benar-benar dibaca DAN ditulis</b> oleh jalur berikut.
 * <ul>
 *   <li><code>/MncBank</code> dan <code>/Va</code> — kedua servlet host-to-host bank ini
 *       tidak berada di daftar <code>intercept-url</code> manapun, sehingga jatuh ke aturan
 *       terakhir <code>/** =&gt; IS_AUTHENTICATED_ANONYMOUSLY</code>: <b>tanpa autentikasi
 *       sama sekali</b>. Keduanya memanggil
 *       <code>VirtualAccountBank.bayarSiswa(...)</code> yang mengembalikan
 *       <code>Map&lt;String, List&lt;Tagihan&gt;&gt;</code> lalu melunasi baris-baris tersebut,
 *       dan membacakan kembali ke pemanggil anonim: nama item biaya, bulan, tahun, denda,
 *       tanggal deadline, dan nama diskon siswa. Nomor VA yang dikirim klien berperan sebagai
 *       orakel atas isi tabel tagihan.</li>
 *   <li><code>/Api TagihanSiswa.va()</code> — sudah memeriksa token, TAPI menyusun
 *       <code>Restrictions.sqlRestriction("this_.id in (" + tagihansParam + ")")</code> dari
 *       parameter klien secara mentah: <b>SQL injection sekaligus IDOR</b>, tanpa satu pun
 *       pembatas kepemilikan siswa pada query Tagihan tersebut.</li>
 *   <li><code>/Api TagihanSiswa.hapus_split()</code> dan <code>split()</code> — memuat tagihan
 *       hanya dengan <code>Restrictions.idEq(...)</code> dari parameter klien, tanpa cek
 *       kepemilikan. <code>hapus_split()</code> kemudian <b>MENGHAPUS</b> baris tagihan itu
 *       (<code>Common.refreshDelete</code>), <b>MENULIS</b>
 *       <code>tagihanTerakhir.setNominal(...)</code>, dan menomori ulang <code>bayarKe</code>
 *       seluruh angsuran milik <code>NominalBiaya</code> tersebut — dengan token siswa
 *       manapun, atas tagihan siswa manapun.</li>
 * </ul>
 * Temuan-temuan tersebut sudah tercatat pada audit keamanan berjalan; entri ini hanya
 * mengonfirmasi bahwa <b>entity inilah tabel sasarannya</b>, bukan sekadar tetangga.
 *
 * <h3>Hal non-obvious lain</h3>
 * <ul>
 *   <li><b>Cache global lintas tenant.</b> <code>MemoryDbUtil.getAllTagihan()</code> adalah
 *       satu <code>Map&lt;String, Tagihan&gt;</code> untuk seluruh proses, di-key hanya oleh
 *       <code>kodeUnik</code>. {@link #genCode} baru menyertakan identitas siswa bila
 *       populasi dan bendera <code>gunakanCalonSiswa</code> cocok; pada kombinasi yang tidak
 *       cocok (mis. seorang <code>Siswa</code> ditagih memakai jenis biaya ber-flag PSB)
 *       prefiks identitas menjadi kosong sehingga kunci hanya berisi id pengaturan biaya +
 *       periode + angsuran — dan dapat bertabrakan antar siswa. {@link #cocokDenganKunciTagihan}
 *       adalah penjaga yang ditambahkan justru untuk memburu kasus salah-ambil semacam ini.</li>
 *   <li><b>Fail-open cakupan tenant.</b> Pada pemilih siswa yang memasok tagihan
 *       (<code>DetailTagihanSiswaHelper.initCriteriaDenganNama</code>) filter sekolah ditulis
 *       <code>getKhususBuatSiswaTertentu() ? sqlRestriction("1=1") : eq("sekolah", ...)</code>:
 *       begitu opsi "khusus buat siswa tertentu" dicentang, <b>pembatas sekolah hilang
 *       sepenuhnya</b> dan daftar siswa mencakup seluruh instalasi. Pada entity ini sendiri
 *       {@link #getSekolah()} juga hanya diturunkan dari siswa/calon siswa, tidak pernah
 *       dipakai sebagai penyaring.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Tiga helper terbesar yang membuat, menyunting,
 *       dan menghapus tagihan — <code>DetailTagihanSiswaHelper</code> (3325 baris),
 *       <code>TagihanUtil</code> (2069 baris), dan <code>PembayaranOnline</code> (4366 baris) —
 *       <b>tidak memiliki satu pun panggilan <code>checkPrevilages</code></b>. Ketiganya
 *       ditanam sebagai panel detail di layar <code>pengaturan_biaya_sekolah.zul</code> dan
 *       <code>PembayaranSiswaAction</code>, sehingga hak yang berlaku adalah hak menu induk
 *       tersebut; tombol "Cari", "Sinkronkan", dan sejenisnya berjalan tanpa gerbang apa pun.</li>
 *   <li><b>Riwayat audit.</b> Kelas ini <code>&#64;Audited</code> (Envers), tetapi operasi
 *       massal berbasis HQL/native SQL yang menyentuh tabel tagihan melewati Envers sehingga
 *       tidak muncul di riwayat.</li>
 *   <li><b>Field {@code oleh}/{@code olehId}/{@code tanggal_dirubah}/{@code id} dideklarasikan
 *       ulang.</b> Ini BUKAN duplikasi yang keliru: {@link ais.database.model.GeneralValueObject}
 *       adalah POJO abstrak biasa (bukan <code>&#64;MappedSuperclass</code>), sehingga properti
 *       induk tidak dipetakan Hibernate dan harus dideklarasikan ulang di setiap entity.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ol>
 *   <li><b>Identitas &amp; jejak audit</b> — {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi rantai billing</b> — {@link #getItemBiayaSekolah()},
 *       {@link #getNominalBiaya()}, {@link #getPengaturanBiaya()},
 *       {@link #getPembayaranSiswaDetail()}.</li>
 *   <li><b>Relasi pihak tertagih</b> — {@link #getSiswa()}, {@link #getCalonSiswa()},
 *       {@link #getSekolah()}, {@link #getKelasSiswa()}, {@link #getTahunAngkatan()}.</li>
 *   <li><b>Periode</b> — {@link #getBulan()}, {@link #getTahun()}, {@link #getTahunbulan()},
 *       {@link #getTahunAjaran()}, {@link #getTanggalTagihan()}, {@link #getTanggalDeadline()}.</li>
 *   <li><b>Uang</b> — {@link #getNominal()}, {@link #ambilNominal()},
 *       {@link #getNominalManual()}, {@link #getDenda()}, {@link #getDiskon()},
 *       {@link #ambilDiskonTanpaDikonBayarSatuKali()}, {@link #getDiskonManual()},
 *       {@link #getDiskonTidakLangsung()}, {@link #getDibayar()},
 *       {@link #getDibayarManual()}, {@link #getBiayaTemporary()}.</li>
 *   <li><b>Status &amp; kategori</b> — {@link #getAktif()}, {@link #getBoleh},
 *       {@link #getAktifkanmanual()}, {@link #getNonaktifManual()},
 *       {@link #getBukanTagihan()}, {@link #ambilBukanTagihan()},
 *       {@link #ambilBukanTagihanData()}, {@link #getBayarKe()}.</li>
 *   <li><b>Kunci identitas &amp; pabrik entity</b> — {@link #genCode}, {@link #getKodeUnik()},
 *       {@link #findByKodeUnik}, {@link #findByKodeUnikLain}, {@link #ambilAtauBuat},
 *       {@link #buatAtauLoadTagihan}, {@link #cocokDenganKunciTagihan},
 *       {@link #saveTagihanDenganKodeUnikAman}.</li>
 *   <li><b>Kanal pembayaran online</b> — {@link #getVa()}, {@link #getLink()},
 *       {@link #getExpired()}, {@link #getKunci()}, {@link #getPembayaranPada()},
 *       {@link #getPembayaranBerakhirPada()}, {@link #getTanggalBayar()}.</li>
 *   <li><b>Jurnal akuntansi</b> — {@link #getPostingHistory()},
 *       {@link #getPostingHistoryDenda()}, {@link #getPostingHistoryDiskon()},
 *       {@link #getPostingHistoryUangMuka()}.</li>
 *   <li><b>Prasyarat pembayaran &amp; urutan</b> — {@link #tagihanLainWajib(List)},
 *       {@link #checkUlangRekursif}, {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Antarmuka pengguna</b> — {@link #pindahkan(Tagihan, EventListener)}.</li>
 * </ol>
 *
 * @see ais.database.model.sekolah.NominalBiaya
 * @see ais.database.model.sekolah.PengaturanBiaya
 * @see ais.database.model.sekolah.ItemBiayaSekolah
 * @see ais.database.model.sekolah.JenisBiayaSekolah
 * @see ais.database.model.sekolah.PembayaranSiswaDetail
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "tagihan", schema = "sekolah")
public class Tagihan extends GeneralValueObject {

	/** Versi serialisasi; entity ini dikirim antar-lapisan sebagai {@link Serializable}. */
	private static final long serialVersionUID = -5958155911087405007L;
	/** Kunci utama baris tagihan (identity/serial database). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (diisi interceptor audit). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (diisi interceptor audit). */
	private String olehId;
	/** Cap waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Komponen biaya yang ditagih (SPP, Uang Gedung, Seragam, ...). */
	private ItemBiayaSekolah itemBiayaSekolah;
	/** Tarif termaterialisasi asal tagihan ini; wajib ada (kolom NOT NULL). */
	private NominalBiaya nominalBiaya;
	/** Baris pelunasan yang menutup tagihan ini; {@code null} berarti belum dibayar. */
	private PembayaranSiswaDetail pembayaranSiswaDetail;
	/** Saat pembayaran benar-benar diterima (diturunkan dari kuitansi). */
	private Date pembayaranPada;
	/** Batas berlakunya pembayaran untuk pola langganan/subscription. */
	private Date pembayaranBerakhirPada;
	/** Paket biaya (TA/angkatan/jurusan) yang menaungi tagihan ini. */
	private PengaturanBiaya pengaturanBiaya;
	/** Siswa aktif yang ditagih; saling menggantikan dengan {@link #calonSiswa}. */
	private Siswa siswa;
	/** Calon siswa (PPDB) yang ditagih; saling menggantikan dengan {@link #siswa}. */
	private CalonSiswa calonSiswa;
	/** Bulan periode tagihan (1-12); kolom turunan dari {@link #tahunbulan}. */
	private Integer bulan;
	/** Keterangan bebas yang ditampilkan bersama tagihan. */
	private String informasi;
	/** Tahun periode tagihan; kolom turunan dari {@link #tahunbulan}. */
	private Integer tahun;
	/** Periode tagihan berformat YYYYMM — SUMBER KEBENARAN periode. */
	private Integer tahunbulan;
	/** Tahun angkatan pihak tertagih; diturunkan dari siswa/calon siswa. */
	private Integer tahunAngkatan;
	/** Tahun ajaran ("2025/2026"); diturunkan dari {@link #pengaturanBiaya}. */
	private String tahunAjaran;
	/** Kunci identitas logis tagihan; lihat {@link #genCode}. */
	private String kodeUnik;
	/** Nominal kewajiban rupiah untuk angsuran ini. */
	private Double nominal;
	/** Nominal yang ditetapkan manual oleh petugas, mengalahkan hasil hitung. */
	private Double nominalManual;
	/** Denda keterlambatan; dihitung ulang oleh {@link #getDenda()}. */
	private Double denda;
	/** Potongan yang berlaku; dihitung ulang oleh {@link #getDiskon()}. */
	private Double diskon;
	/** Jumlah yang sudah dibayar; diturunkan dari {@link #pembayaranSiswaDetail}. */
	private Double dibayar;
	/** Diskon siswa yang sedang berlaku; DAPAT DIKOSONGKAN oleh {@link #getDiskonSiswa()}. */
	private DiskonSiswa diskonSiswa;
	/** Salinan cadangan {@link #diskonSiswa} agar jejak diskon tidak hilang total. */
	private DiskonSiswa diskonSiswaAsli;
	/** Potongan yang ditetapkan manual oleh petugas. */
	private Double diskonManual;
	/** Potongan yang berasal dari sumber lain (beasiswa/subsidi) di luar diskon siswa. */
	private Double diskonTidakLangsung;
	/** Sekolah pemilik; diturunkan dari siswa/calon siswa, bukan penyaring tenant. */
	private Sekolah sekolah;
	/** Tanggal uang diterima; diturunkan dari kuitansi. */
	private Date tanggalBayar;
	/** Tanggal tagihan diterbitkan; dihitung dari pengaturan biaya per bulan. */
	private Date tanggalTagihan;
	/** Batas akhir sebelum denda berlaku. */
	private Date tanggalDeadline;
	/** Nomor urut angsuran (1 = angsuran pertama / pembayaran sekali lunas). */
	private Integer bayarKe;
	/** Cache status keaktifan; nilai sebenarnya dihitung {@link #getAktif()}. */
	private Boolean aktif;
	/** Override AKTIFKAN MANUAL oleh admin: melewati seluruh validasi keaktifan. */
	private Boolean aktifkanmanual;
	/** Override NONAKTIFKAN MANUAL oleh admin: memaksa tagihan mati. */
	private Boolean nonaktifManual;
	/** Nominal angsuran yang ditentukan manual saat pembuatan tagihan. */
	private Double dibayarManual;

	/** Jejak posting jurnal untuk pokok tagihan. */
	private PostingHistory postingHistory;
	/** Jejak posting jurnal untuk denda. */
	private PostingHistory postingHistoryDenda;
	/** Jejak posting jurnal untuk diskon. */
	private PostingHistory postingHistoryDiskon;
	/** Jejak posting jurnal untuk uang muka. */
	private PostingHistory postingHistoryUangMuka;

	/** Snapshot kelas siswa pada tahun ajaran tagihan (untuk laporan per kelas). */
	private KelasSiswa kelasSiswa;
	/** Nomor Virtual Account bank yang diterbitkan untuk tagihan ini. */
	private String va;
	/** Tautan pembayaran (payment link) kanal online. */
	private String link;
	/** Kedaluwarsa nomor VA / tautan pembayaran. */
	private Date expired;
	/** Penanda "bukan tagihan": baris ada tapi nominalnya dipaksa nol. */
	private Boolean bukanTagihan;

	/** Pengguna yang "mengunci" tagihan agar memakai {@link #biayaTemporary}. */
	private Tbmuser kunci;
	/** Nominal sementara hasil simulasi/negosiasi, dipakai bila {@link #kunci} terisi. */
	private Double biayaTemporary;

	/** Konstruktor kosong yang diwajibkan Hibernate. */
	public Tagihan() {
	}

	/**
	 * Konstruktor ringkas untuk membuat referensi tagihan berdasarkan kunci utama saja
	 * (dipakai saat hanya id yang tersedia, mis. saat menyusun kriteria pencarian).
	 *
	 * @param id kunci utama baris tagihan
	 */
	public Tagihan(long id) {
		this.id = id;
	}

	/**
	 * Konstruktor lengkap untuk pembuatan tagihan dari alur generator.
	 *
	 * @param nominalBiaya          tarif termaterialisasi asal tagihan (wajib, kolom NOT NULL)
	 * @param tahunbulan            periode berformat YYYYMM
	 * @param untukBulan            bulan periode (1-12)
	 * @param untukTahun            tahun periode
	 * @param pembayaranSiswaDetail baris pelunasan yang sudah ada, atau {@code null}
	 * @param siswa                 siswa yang ditagih
	 * @param bayarKe               nomor urut angsuran
	 */
	public Tagihan(NominalBiaya nominalBiaya, Integer tahunbulan, Integer untukBulan, Integer untukTahun,
			PembayaranSiswaDetail pembayaranSiswaDetail, Siswa siswa, Integer bayarKe) {
		this.nominalBiaya = nominalBiaya;
		this.tahunbulan = tahunbulan;
		this.bulan = untukBulan;
		this.tahun = untukTahun;
		this.pembayaranSiswaDetail = pembayaranSiswaDetail;
		this.siswa = siswa;
		this.bayarKe = bayarKe;
	}

	/**
	 * Kait siklus hidup JPA yang dijalankan tepat sebelum baris ini di-UPDATE. Meneruskan
	 * entity ke <code>AuditTimestampInterceptor.ubah(...)</code> agar {@link #getOleh()},
	 * {@link #getOlehId()}, dan {@link #getTanggal_dirubah()} terisi dari sesi pengguna
	 * yang sedang berjalan.
	 * <p>
	 * Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * @return kunci utama baris tagihan; {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id kunci utama baris tagihan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return id pengguna terakhir yang mengubah baris ini
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong
	 * SENGAJA DIABAIKAN agar jejak audit yang sudah ada tidak terhapus oleh proses batch
	 * yang berjalan tanpa konteks pengguna.
	 *
	 * @param olehId id pengguna; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {
		if (olehId != null && !olehId.trim().isEmpty()) {
			this.olehId = olehId;
		}
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * nilai kosong diabaikan agar jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {
		if (oleh != null && !oleh.trim().isEmpty()) {
			this.oleh = oleh;
		}
	}

	/**
	 * @return cap waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Komponen biaya yang ditagih oleh baris ini (SPP, Uang Gedung, Seragam, ...).
	 * Kolom <code>item_biaya_id</code> NOT NULL.
	 * <p>
	 * Nilainya dilewatkan <code>check(...)</code> milik
	 * {@link ais.database.model.GeneralValueObject} agar proxy Hibernate yang terikat ke
	 * sesi lain yang sudah tertutup tidak menimbulkan <i>LazyInitializationException</i>.
	 * Item biaya inilah yang menyediakan sifat per-komponen yang dipakai di seluruh kelas
	 * ini: <code>khususBulan</code>, <code>harusBayar</code> (prasyarat berjenjang),
	 * <code>angsuranSeragam</code>, <code>bolehDiangsur</code>, dan <code>kelamin</code>.
	 *
	 * @return item biaya yang ditagih, atau {@code null} bila tak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_id", nullable = false)
	public ItemBiayaSekolah getItemBiayaSekolah() {
		itemBiayaSekolah = check(itemBiayaSekolah);
		return itemBiayaSekolah;
	}

	/**
	 * Mengubah komponen biaya yang ditagih.
	 * <p>
	 * <b>Hati-hati:</b> nilai ini ikut membentuk {@link #getKodeUnik()}. Mengubahnya pada
	 * tagihan yang sudah tersimpan membuat kunci identitas baris bergeser, sehingga
	 * pencarian lewat {@link #findByKodeUnik} tidak lagi menemukannya dan generator dapat
	 * membuat tagihan duplikat.
	 *
	 * @param itemBiayaSekolah komponen biaya yang ditagih
	 */
	public void setItemBiayaSekolah(ItemBiayaSekolah itemBiayaSekolah) {
		this.itemBiayaSekolah = itemBiayaSekolah;
	}

	/**
	 * Tarif termaterialisasi yang menjadi asal tagihan ini. Kolom
	 * <code>nominal_biaya_id</code> NOT NULL — tagihan tidak boleh berdiri tanpa
	 * {@link ais.database.model.sekolah.NominalBiaya}.
	 * <p>
	 * Relasi ini adalah <b>poros seluruh kelas</b>: {@link #getPengaturanBiaya()},
	 * {@link #getSiswa()}, {@link #getCalonSiswa()}, {@link #getBayarKe()},
	 * {@link #getNominal()}, {@link #getDenda()} dan {@link #getAktif()} semuanya menelusur
	 * lewat sini. Satu <code>NominalBiaya</code> dapat memayungi beberapa tagihan bila
	 * komponennya dicicil (<code>dibayarSebayak &gt; 1</code>).
	 * <p>
	 * Dibungkus <code>check(...)</code> agar proxy dari sesi yang sudah tertutup tidak
	 * meledak saat diakses.
	 *
	 * @return tarif asal tagihan ini, atau {@code null} bila tak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "nominal_biaya_id", nullable = false)
	public NominalBiaya getNominalBiaya() {
		return (NominalBiaya) check(nominalBiaya);
	}

	/**
	 * @param nominalBiaya tarif termaterialisasi asal tagihan (wajib terisi sebelum INSERT;
	 *                     lihat penjaga id di {@link #ambilAtauBuat})
	 */
	public void setNominalBiaya(NominalBiaya nominalBiaya) {
		this.nominalBiaya = nominalBiaya;
	}

	/**
	 * Pembaca MENTAH baris pelunasan: mengembalikan field apa adanya <b>tanpa</b> efek
	 * samping apa pun.
	 * <p>
	 * Ini adalah pendamping aman bagi {@link #getPembayaranSiswaDetail()} yang justru
	 * menulis balik dan bahkan dapat MEMUTUS relasi. Pakai method ini bila yang dibutuhkan
	 * hanya "apakah field-nya terisi" — mis. saat memeriksa konsistensi dua arah — supaya
	 * pemeriksaan itu sendiri tidak mengubah data.
	 *
	 * @return baris pelunasan apa adanya, atau {@code null}
	 */
	public PembayaranSiswaDetail ambilPembayaranSiswaDetail() {
		return pembayaranSiswaDetail;
	}

	/**
	 * Baris pelunasan yang menutup tagihan ini; {@code null} berarti tagihan belum dibayar.
	 * Relasi dipetakan <code>unique</code> — satu tagihan hanya boleh punya satu pelunasan.
	 *
	 * <p><b>GETTER DENGAN EFEK SAMPING — DUA-DUANYA MENULIS DATA:</b></p>
	 * <ol>
	 *   <li>Bila baris pelunasan yang tertaut belum menunjuk balik ke tagihan ini, method
	 *       ini <b>menulis ke entity LAIN</b>: <code>pembayaranSiswaDetail.setTagihan(this)</code>.
	 *       Ini menutup relasi dua arah, tetapi juga berarti membaca tagihan A dapat
	 *       mengubah kepemilikan sebuah baris pelunasan.</li>
	 *   <li><b>DESTRUKTIF:</b> bila pelunasan sudah tersimpan (punya id) dan nominalnya
	 *       dibulatkan ke bilangan bulat bernilai 0, referensinya <b>diputus</b>
	 *       (<code>pembayaranSiswaDetail = null</code>). Karena entity ini property-access,
	 *       Hibernate akan menuliskan <code>pembayaran_siswa_detail_id = NULL</code> pada
	 *       flush berikutnya: tagihan yang tadinya lunas kembali muncul sebagai tunggakan
	 *       dan jejak ke kuitansinya hilang. Melepas penyebabnya TIDAK memulihkan tautan.</li>
	 * </ol>
	 * Sebagai efek samping ketiga, field {@link #nominal} ikut ditimpa dengan
	 * <code>pembayaranSiswaDetail.ambilNominal()</code>.
	 * <p>
	 * Dipanggil dari {@link #getNominal()}, {@link #getDenda()}, {@link #getAktif()},
	 * {@link #getTanggalBayar()}, {@link #getPembayaranPada()}, dan dari seluruh layar
	 * pembayaran — sehingga efek di atas terpicu jauh lebih sering daripada yang terlihat.
	 *
	 * @return baris pelunasan, atau {@code null} bila belum dibayar / baru saja diputus
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_siswa_detail_id", unique = true)
	public PembayaranSiswaDetail getPembayaranSiswaDetail() {
		if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.ambilTagihan() == null) {
			pembayaranSiswaDetail.setTagihan(this);
		}

		if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null) {
			nominal = pembayaranSiswaDetail.ambilNominal();
			if (nominal != null && nominal.intValue() == 0) {
				pembayaranSiswaDetail = null;
			}
		}

		return this.pembayaranSiswaDetail;
	}

	/**
	 * @param pembayaranSiswaDetail baris pelunasan yang menutup tagihan ini, atau
	 *                              {@code null} untuk menandai belum dibayar
	 */
	public void setPembayaranSiswaDetail(PembayaranSiswaDetail pembayaranSiswaDetail) {
		this.pembayaranSiswaDetail = pembayaranSiswaDetail;
	}

	/**
	 * Varian setter yang isinya PERSIS SAMA dengan {@link #setPembayaranSiswaDetail}.
	 * <p>
	 * Keberadaannya bersifat historis: pada {@link #pindahkan(Tagihan, EventListener)}
	 * keduanya dipanggil berurutan atas tagihan asal. Dahulu varian "True" ini dimaksudkan
	 * sebagai jalur yang melewati logika tambahan; kini tidak ada bedanya sama sekali.
	 * Dipertahankan agar pemanggil lama tetap terkompilasi (kompatibilitas mundur).
	 *
	 * @param pembayaranSiswaDetail baris pelunasan, atau {@code null}
	 */
	public void setPembayaranSiswaDetailTrue(PembayaranSiswaDetail pembayaranSiswaDetail) {
		this.pembayaranSiswaDetail = pembayaranSiswaDetail;
	}

	/**
	 * Siswa aktif yang ditagih. Saling menggantikan dengan {@link #getCalonSiswa()}:
	 * jenis biaya ber-flag <code>gunakanCalonSiswa</code> menagih calon siswa (PPDB),
	 * selebihnya menagih siswa aktif.
	 *
	 * <p><b>GETTER MENULIS BALIK — kepemilikan tagihan dapat berpindah saat dibaca.</b>
	 * Urutan penentuannya:</p>
	 * <ol>
	 *   <li>bila {@link #getNominalBiaya()} punya siswa, siswa ITULAH yang dipakai
	 *       (tarif yang menang, bukan kolom <code>siswa_id</code> milik tagihan);</li>
	 *   <li>kalau tidak, bila calon siswa sudah dikonversi menjadi siswa
	 *       (<code>calonSiswa.getSiswa()</code>), siswa hasil konversi itu yang dipakai —
	 *       inilah jalur yang memindahkan tagihan PPDB ke siswa setelah diterima;</li>
	 *   <li>kalau tidak, pakai nilai kolom sendiri (lewat <code>check(...)</code>).</li>
	 * </ol>
	 * Hasilnya disimpan ke field, sehingga pada flush berikutnya kolom
	 * <code>siswa_id</code> ikut berubah. Bila <code>NominalBiaya</code> yang tertaut
	 * ternyata milik siswa lain (kasus salah-ambil dari cache, lihat
	 * {@link #cocokDenganKunciTagihan}), tagihan ini akan <b>berpindah tangan diam-diam</b>.
	 * <p>
	 * Seluruh <i>exception</i> ditelan dan dijatuhkan ke nilai kolom sendiri.
	 *
	 * @return siswa yang ditagih, atau {@code null} bila tagihan ditujukan ke calon siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id")
	public Siswa getSiswa() {
		try {
			if (getNominalBiaya() != null && getNominalBiaya().getSiswa() != null) {
				siswa = getNominalBiaya().getSiswa();
			} else {
				calonSiswa = getCalonSiswa();
				if (calonSiswa != null && calonSiswa.getSiswa() != null) {
					siswa = calonSiswa.getSiswa();
				} else {
					siswa = check(siswa);
				}
			}
		} catch (Exception e) {
			siswa = check(siswa);
		}
		return this.siswa;
	}

	/**
	 * @param siswa siswa yang ditagih; ikut membentuk {@link #getKodeUnik()}
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Calon siswa (PPDB) yang ditagih. Saling menggantikan dengan {@link #getSiswa()}.
	 *
	 * <p><b>GETTER MENULIS BALIK:</b> bila {@link #getNominalBiaya()} menunjuk ke seorang
	 * calon siswa, nilai itulah yang dipakai dan disimpan ke field — kolom
	 * <code>calon_siswa_id</code> milik tagihan kalah oleh tarifnya. Selebihnya nilai kolom
	 * sendiri dipakai lewat <code>check(...)</code>.</p>
	 *
	 * @return calon siswa yang ditagih, atau {@code null} bila tagihan ditujukan ke siswa aktif
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa_id")
	public CalonSiswa getCalonSiswa() {
		try {
			if (getNominalBiaya() != null && getNominalBiaya().getCalonSiswa() != null) {
				calonSiswa = getNominalBiaya().getCalonSiswa();
			}
			calonSiswa = check(calonSiswa);
		} catch (Exception e) {
			calonSiswa = check(calonSiswa);
		}
		return calonSiswa;
	}

	/**
	 * @param calonSiswa calon siswa yang ditagih; ikut membentuk {@link #getKodeUnik()}
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	// =========================================================================================
	// OPTIMISASI MEMORI: Pengambilan Tahun dan Bulan menggunakan aritmatika, bukan
	// String
	// =========================================================================================
	/**
	 * Bulan periode tagihan (1-12), atau {@code null} bila tagihan tidak terikat bulan
	 * tertentu.
	 *
	 * <p><b>GETTER MENULIS BALIK.</b> Nilai kolom <code>bulan</code> adalah <i>turunan</i>,
	 * bukan sumber kebenaran; sumber kebenarannya adalah {@link #getTahunbulan()} berformat
	 * YYYYMM. Method ini menyusun ulang nilainya melalui empat lapis, berurutan, yang
	 * belakangan mengalahkan yang terdahulu:</p>
	 * <ol>
	 *   <li><code>JenisBiayaSekolah.untukBulan</code> — hanya untuk periode selain
	 *       "Bulanan"/"Harian" (untuk kedua periode itu bulan ditentukan per baris tagihan,
	 *       bukan per jenis biaya);</li>
	 *   <li><code>ItemBiayaSekolah.khususBulan</code> — komponen yang memang hanya ditagih
	 *       pada satu bulan tertentu setiap tahun;</li>
	 *   <li><code>NominalBiaya.tahunbulan</code>, <b>tetapi hanya bila konsisten</b> dengan
	 *       <code>tahunbulan</code> milik tagihan ini. Penjaga ini penting: satu
	 *       <code>NominalBiaya</code> milik bulan lain dapat dipakai untuk membuat tagihan
	 *       bulan ini, dan tanpa penjaga tersebut bulan tagihan akan ikut tergeser
	 *       (lihat komentar di dalam kode);</li>
	 *   <li><code>this.tahunbulan % 100</code> — <b>override terakhir</b> dan sumber
	 *       kebenaran sebenarnya, konsisten dengan {@link #getTahun()}.</li>
	 * </ol>
	 * Terakhir, nilai di luar rentang 1-12 dinormalisasi menjadi {@code null}.
	 * <p>
	 * Ambang <code>&gt; 2100</code> dipakai di seluruh kelas untuk membedakan angka
	 * berformat YYYYMM (mis. 202607) dari nilai lama yang hanya berisi tahun (mis. 2026).
	 *
	 * @return bulan periode 1-12, atau {@code null}
	 */
	@Column(name = "bulan")
	public Integer getBulan() {
		if (getPengaturanBiaya() != null) {
			JenisBiayaSekolah jbs = getPengaturanBiaya().getJenisBiayaSekolah();
			if (jbs != null && jbs.getUntukBulan() != null) {
				String periode = jbs.getPeriode();
				if (!"Bulanan".equalsIgnoreCase(periode) && !"Harian".equalsIgnoreCase(periode)) {
					bulan = jbs.getUntukBulan();
				}
			}
		}

		if (getItemBiayaSekolah() != null && getItemBiayaSekolah().getKhususBulan() != null) {
			bulan = getItemBiayaSekolah().getKhususBulan();
		}

		// Ambil bulan dari NominalBiaya.tahunbulan HANYA jika berformat YYYYMM dan
		// konsisten dengan tagihan.tahunbulan yang sudah di-set. Jika berbeda (mis.
		// NominalBiaya milik bulan lain dipakai untuk membuat tagihan bulan ini),
		// jangan timpa — tahunbulan milik tagihan yang menjadi acuan kebenaran.
		if (getNominalBiaya() != null && getNominalBiaya().getTahunbulan() != null
				&& getNominalBiaya().getTahunbulan() > 2100) {
			Integer nbTb = getNominalBiaya().getTahunbulan();
			if (this.tahunbulan == null || this.tahunbulan.equals(nbTb)) {
				bulan = nbTb % 100;
			}
		}

		// SUMBER KEBENARAN: bulan SELALU mengikuti tahunbulan milik tagihan ini bila
		// berformat YYYYMM — override terakhir, konsisten dengan getTahun().
		if (this.tahunbulan != null && this.tahunbulan > 2100) {
			int blDariTb = this.tahunbulan % 100;
			if (blDariTb >= 1 && blDariTb <= 12) {
				bulan = blDariTb;
			}
		}

		if (bulan != null && (bulan < 1 || bulan > 12)) {
			bulan = null;
		}
		return this.bulan;
	}

	/**
	 * Menyetel bulan periode secara langsung.
	 * <p>
	 * Perhatikan bahwa nilai ini akan <b>ditimpa lagi</b> oleh {@link #getBulan()} bila
	 * {@link #getTahunbulan()} berformat YYYYMM. Untuk mengubah periode tagihan, pakai
	 * {@link #setTahunbulan(Integer)} yang menyelaraskan ketiganya sekaligus.
	 *
	 * @param bulan bulan periode 1-12, atau {@code null}
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Tahun periode tagihan, atau {@code null} bila tidak terikat tahun tertentu.
	 *
	 * <p><b>GETTER MENULIS BALIK.</b> Kembaran {@link #getBulan()} dengan struktur empat
	 * lapis yang sama:</p>
	 * <ol>
	 *   <li><code>JenisBiayaSekolah.untukTahun</code> — hanya untuk periode "Insidentil";</li>
	 *   <li>untuk komponen ber-<code>khususBulan</code>, tahun disimpulkan dari tahun ajaran
	 *       <code>PengaturanBiaya</code>: bulan &ge; 7 (Juli ke atas) memakai potongan
	 *       PERTAMA "2025/2026" &rarr; 2025, bulan &lt; 7 memakai potongan KEDUA &rarr; 2026.
	 *       Ini mencerminkan tahun ajaran Indonesia yang dimulai Juli. Format tahun ajaran
	 *       yang salah ditelan diam-diam (hanya tercatat ke audit error);</li>
	 *   <li><code>NominalBiaya.tahunbulan / 100</code>, dengan penjaga konsistensi yang sama
	 *       seperti {@link #getBulan()} — komentar di dalam kode mencatat kasus nyata
	 *       "tahun=2026 padahal tagihan.tahunbulan=202706";</li>
	 *   <li><code>this.tahunbulan / 100</code> — <b>override terakhir</b>.</li>
	 * </ol>
	 * Nilai di luar rentang 1900-2200 dinormalisasi menjadi {@code null}.
	 *
	 * @return tahun periode, atau {@code null}
	 */
	@Column(name = "tahun")
	public Integer getTahun() {
		if (getPengaturanBiaya() != null) {
			JenisBiayaSekolah jbs = getPengaturanBiaya().getJenisBiayaSekolah();
			if (jbs != null && jbs.getUntukTahun() != null && "Insidentil".equalsIgnoreCase(jbs.getPeriode())) {
				tahun = jbs.getUntukTahun();
			}
		}

		if (getItemBiayaSekolah() != null && getItemBiayaSekolah().getKhususBulan() != null && getNominalBiaya() != null
				&& getPengaturanBiaya() != null
				&& getPengaturanBiaya().getTahunAjaran() != null) {
			try {
				String[] taSplit = getPengaturanBiaya().getTahunAjaran().split("/");
				if (taSplit.length > 1) {
					if (getItemBiayaSekolah().getKhususBulan() >= 7) {
						tahun = Integer.parseInt(taSplit[0].trim());
					} else {
						tahun = Integer.parseInt(taSplit[1].trim());
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:362");
				/* Abaikan format salah */ }
		}

		// Turunkan dari NominalBiaya HANYA bila formatnya YYYYMM dan konsisten dengan
		// tahunbulan milik tagihan ini — guard yang sama dengan getBulan(). Tanpa guard,
		// NominalBiaya milik periode lain menimpa tahun (kasus: tahun=2026 padahal
		// tagihan.tahunbulan=202706/Juni 2027).
		if (getNominalBiaya() != null && getNominalBiaya().getTahunbulan() != null
				&& getNominalBiaya().getTahunbulan() > 2100
				&& (this.tahunbulan == null || this.tahunbulan.equals(getNominalBiaya().getTahunbulan()))) {
			tahun = getNominalBiaya().getTahunbulan() / 100;
		}

		// SUMBER KEBENARAN: tahun SELALU mengikuti tahunbulan milik tagihan ini bila
		// berformat YYYYMM (>2100 dengan bulan valid). Override terakhir agar menang
		// atas seluruh derivasi lain di atas.
		if (this.tahunbulan != null && this.tahunbulan > 2100) {
			int blDariTb = this.tahunbulan % 100;
			if (blDariTb >= 1 && blDariTb <= 12) {
				tahun = this.tahunbulan / 100;
			}
		}

		if (tahun != null && (tahun < 1900 || tahun > 2200)) {
			tahun = null;
		}
		return this.tahun;
	}

	/**
	 * Menyetel tahun periode secara langsung; sama seperti {@link #setBulan(Integer)},
	 * nilainya dapat ditimpa kembali oleh {@link #getTahun()}. Pakai
	 * {@link #setTahunbulan(Integer)} untuk mengubah periode secara konsisten.
	 *
	 * @param tahun tahun periode, atau {@code null}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Periode tagihan berformat <b>YYYYMM</b> (mis. 202607 = Juli 2026). Inilah
	 * <b>SUMBER KEBENARAN</b> periode; {@link #getBulan()} dan {@link #getTahun()} hanyalah
	 * turunannya.
	 *
	 * <p><b>GETTER MENULIS BALIK</b> — tetapi hanya bila field masih kosong (memoisasi):</p>
	 * <ol>
	 *   <li>bila sudah terisi, dikembalikan apa adanya;</li>
	 *   <li>kalau tidak, disusun dari {@link #getTahun()} + {@link #getBulan()} lewat
	 *       <code>PembayaranSiswa.convert(...)</code>;</li>
	 *   <li>bila hasilnya masih {@code null}, dijatuhkan ke
	 *       <code>NominalBiaya.getTahunbulan()</code>.</li>
	 * </ol>
	 * Nilai ini ikut membentuk {@link #getKodeUnik()}, dipakai sebagai kunci pengurutan di
	 * {@link #compareTo(GeneralValueObject)}, sebagai pembanding "bulan yang tidak ada
	 * dendanya"/"bulan yang tidak ada tagihannya" di {@link #getTanggalDeadline()} dan
	 * {@link #ambilBukanTagihan()}, serta sebagai batas <code>bulanSampai</code> di
	 * {@link #getAktif()}.
	 *
	 * @return periode berformat YYYYMM, atau {@code null} bila tidak dapat disimpulkan
	 */
	@Column(name = "tahunbulan")
	public Integer getTahunbulan() {
		if (tahunbulan != null) {
			return tahunbulan;
		}
		tahunbulan = PembayaranSiswa.convert(getTahun(), getBulan());
		if (tahunbulan == null && getNominalBiaya() != null && getNominalBiaya().getTahunbulan() != null) {
			tahunbulan = getNominalBiaya().getTahunbulan();
		}
		return this.tahunbulan;
	}

	/**
	 * Menyetel periode tagihan sekaligus <b>menyelaraskan kolom turunan</b>
	 * {@link #getBulan()} dan {@link #getTahun()} bila nilainya berformat YYYYMM.
	 * <p>
	 * Ini adalah cara yang BENAR untuk mengubah periode sebuah tagihan; menyetel
	 * bulan/tahun sendiri-sendiri berisiko menghasilkan pasangan nilai yang bertentangan.
	 * Alasan lengkapnya tercatat pada komentar di dalam method: urutan pemanggilan setter
	 * oleh Hibernate saat hidrasi tidak dijamin, sehingga aturan yang sama ditegakkan dua
	 * kali — di sini (saat ditulis) dan di getter (saat dibaca/di-flush).
	 * <p>
	 * <b>Efek samping:</b> ikut mengubah {@link #getKodeUnik()} bila kunci belum tersimpan.
	 *
	 * @param tahunbulan periode berformat YYYYMM, atau {@code null}
	 */
	public void setTahunbulan(Integer tahunbulan) {
		this.tahunbulan = tahunbulan;
		// tahunbulan = SUMBER KEBENARAN periode tagihan. Saat di-set dengan format
		// YYYYMM (mis. 202706 = Juni 2027), kolom turunan bulan/tahun langsung
		// diselaraskan agar tidak ada jalur pembuat tagihan yang bisa menyimpan
		// pasangan nilai yang bertentangan. (Urutan pemanggilan setter oleh Hibernate
		// saat hidrasi tidak dijamin — getBulan()/getTahun() juga menegakkan aturan
		// yang sama sebagai lapis kedua saat dibaca/di-flush.)
		if (tahunbulan != null && tahunbulan > 2100) {
			int bl = tahunbulan % 100;
			if (bl >= 1 && bl <= 12) {
				this.bulan = bl;
				this.tahun = tahunbulan / 100;
			}
		}
	}

	/**
	 * @return keterangan bebas yang ditampilkan bersama tagihan, atau {@code null}
	 */
	@Column(name = "informasi")
	public String getInformasi() {
		return this.informasi;
	}

	/**
	 * @param informasi keterangan bebas yang ditampilkan bersama tagihan
	 */
	public void setInformasi(String informasi) {
		this.informasi = informasi;
	}

	// =========================================================================================
	// OPTIMISASI: Pengecekan Rekursif tagihanLainWajib Diekstrak Menjadi Method
	// Terpisah
	// =========================================================================================
	/**
	 * Menelusuri rantai prasyarat <code>ItemBiayaSekolah.harusBayar</code> secara rekursif
	 * dan mengumpulkan id seluruh komponen biaya yang harus dilunasi lebih dahulu.
	 * <p>
	 * Rantai ini dapat berjenjang: "Uang Gedung harus dibayar sebelum SPP", sementara
	 * "Uang Pendaftaran harus dibayar sebelum Uang Gedung". Method ini menyusuri jenjang
	 * tersebut dengan menambahkan setiap prasyarat baru ke <code>harusBayars</code> lalu
	 * memanggil dirinya sendiri untuk tagihan yang bersangkutan.
	 * <p>
	 * <b>Pengaman siklus</b> bersifat implisit: rekursi hanya berlanjut untuk id prasyarat
	 * yang BELUM ada di <code>harusBayars</code>, sehingga konfigurasi melingkar
	 * (A butuh B, B butuh A) berhenti sendiri alih-alih menjadi rekursi tak berujung.
	 *
	 * @param harusBayars  daftar id komponen biaya yang sudah teridentifikasi sebagai
	 *                     prasyarat; <b>dimodifikasi di tempat</b> oleh method ini
	 * @param tagihanCek   tagihan yang sedang ditelusuri prasyaratnya
	 * @param allTagihans  seluruh tagihan yang tersedia sebagai bahan penelusuran
	 */
	private void checkUlangRekursif(List<Long> harusBayars, Tagihan tagihanCek, List<Tagihan> allTagihans) {
		if (tagihanCek == null || tagihanCek.getItemBiayaSekolah() == null)
			return;
		for (Tagihan tag : allTagihans) {
			if (tagihanCek.getItemBiayaSekolah().getHarusBayar() != null && tag.getItemBiayaSekolah() != null && tag
					.getItemBiayaSekolah().getId().equals(tagihanCek.getItemBiayaSekolah().getHarusBayar().getId())) {

				if (tag.getItemBiayaSekolah().getHarusBayar() != null
						&& !harusBayars.contains(tag.getItemBiayaSekolah().getHarusBayar().getId())) {
					harusBayars.add(tag.getItemBiayaSekolah().getHarusBayar().getId());
					checkUlangRekursif(harusBayars, tag, allTagihans);
				}
			}
		}
	}

	/**
	 * Menentukan tagihan mana saja dari <code>tagihans</code> yang <b>WAJIB ikut dibayar</b>
	 * bila pengguna memilih membayar tagihan ini. Hasilnya dipakai antarmuka pembayaran
	 * (web maupun aplikasi) untuk mencentang-paksa baris lain dan mencegah pembayaran
	 * melompati urutan.
	 *
	 * <p>Empat aturan diperiksa berurutan untuk setiap kandidat; yang cocok pertama menang:</p>
	 * <ol>
	 *   <li><b>Daftar eksplisit.</b> <code>PengaturanBiaya.wajibDibayarSebelumnya</code>
	 *       berisi id tagihan yang diapit koma; pencocokannya memakai pola
	 *       <code>","+id+","</code> sehingga daftar itu harus berawal dan berakhir koma —
	 *       kalau tidak, entri pertama dan terakhir tidak akan pernah cocok.</li>
	 *   <li><b>Cicilan berurutan.</b> Bila komponen ini dicicil
	 *       (<code>dibayarSebayak &gt; 1</code>), seluruh angsuran komponen yang sama wajib
	 *       ikut — atau seluruh komponen apa pun bila jenis biayanya ber-flag
	 *       <code>pilihanItemBiayaTerakumulasiBulanan</code>.</li>
	 *   <li><b>Periode "Bulanan".</b> Tunggakan bulan-bulan SEBELUMNYA wajib ikut
	 *       (<code>tag.tahunbulan &le; tagihan.tahunbulan</code>) — inilah yang mencegah
	 *       siswa membayar SPP bulan Desember sementara Oktober masih menunggak. Untuk
	 *       jenis biaya terakumulasi bulanan, syarat periodenya dilonggarkan menjadi
	 *       "tahunbulan sama persis".</li>
	 *   <li><b>Prasyarat berjenjang.</b> Selebihnya, rantai
	 *       <code>ItemBiayaSekolah.harusBayar</code> ditelusuri lewat
	 *       {@link #checkUlangRekursif}.</li>
	 * </ol>
	 *
	 * <p><b>Catatan kehalusan:</b> pada cabang (2) pemeriksaan <code>tag.getNominalBiaya()
	 * != null</code> dipakai sebagai penjaga, namun nilai yang diambil justru
	 * <code>tag.getPengaturanBiaya().getJenisBiayaSekolah()</code> — penjaga dan pemakaian
	 * tidak menunjuk objek yang sama. Karena {@link #getPengaturanBiaya()} sendiri
	 * diturunkan dari <code>NominalBiaya</code>, dalam praktiknya keduanya nyaris selalu
	 * sejalan, tetapi ini bukan penjagaan yang ketat.</p>
	 *
	 * <p>Seluruh <i>exception</i> ditangkap dan dicatat; kegagalan menghasilkan daftar
	 * parsial, bukan error ke pengguna — artinya kegagalan senyap di sini akan
	 * <b>melonggarkan</b> aturan pembayaran, bukan mengetatkannya.</p>
	 *
	 * @param tagihans daftar tagihan kandidat (biasanya seluruh tagihan aktif siswa yang sama)
	 * @return array JSON berisi id tagihan yang wajib ikut dibayar; kosong bila
	 *         <code>tagihans</code> kosong/{@code null}
	 */
	public JSONArray tagihanLainWajib(final List<Tagihan> tagihans) {
		JSONArray wajib = new JSONArray();
		if (tagihans == null || tagihans.isEmpty())
			return wajib;

		try {
			Tagihan tagihan = this;
			PengaturanBiaya pb = tagihan.getPengaturanBiaya();
			NominalBiaya nb = tagihan.getNominalBiaya();

			for (Tagihan tag : tagihans) {
				if (tag.getId() == null)
					continue;

				if (pb != null && pb.getWajibDibayarSebelumnya() != null
						&& pb.getWajibDibayarSebelumnya().contains("," + tag.getId() + ",")) {
					wajib.put(tag.getId());
					continue;
				}

				if (nb != null && nb.getDibayarSebayak() != null && nb.getDibayarSebayak() > 1) {
					JenisBiayaSekolah jbs = tag.getNominalBiaya() != null
							? tag.getPengaturanBiaya().getJenisBiayaSekolah()
							: null;
					boolean akumulasiBulanan = jbs != null
							&& Boolean.TRUE.equals(jbs.getPilihanItemBiayaTerakumulasiBulanan());

					if (akumulasiBulanan || (tag.getItemBiayaSekolah() != null && tagihan.getItemBiayaSekolah() != null
							&& tag.getItemBiayaSekolah().getId().equals(tagihan.getItemBiayaSekolah().getId()))) {
						wajib.put(tag.getId());
						continue;
					}
				} else if (nb != null && nb.getPengaturanBiaya() != null
						&& nb.getPengaturanBiaya().getJenisBiayaSekolah() != null
						&& "Bulanan".equalsIgnoreCase(nb.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode())) {

					JenisBiayaSekolah jbsThis = nb.getPengaturanBiaya().getJenisBiayaSekolah();
					JenisBiayaSekolah jbsTag = tag.getNominalBiaya() != null
							&& tag.getPengaturanBiaya() != null
									? tag.getPengaturanBiaya().getJenisBiayaSekolah()
									: null;

					boolean cond1 = jbsThis != null
							&& Boolean.TRUE.equals(jbsThis.getPilihanItemBiayaTerakumulasiBulanan()) && jbsTag != null
							&& Boolean.TRUE.equals(jbsTag.getPilihanItemBiayaTerakumulasiBulanan())
							&& tagihan.getTahunbulan() != null && tag.getTahunbulan() != null
							&& tagihan.getTahunbulan().equals(tag.getTahunbulan());

					boolean cond2 = tag.getItemBiayaSekolah() != null && tagihan.getItemBiayaSekolah() != null
							&& tag.getItemBiayaSekolah().getId().equals(tagihan.getItemBiayaSekolah().getId());

					if (cond1 || cond2) {
						int tag1 = tag.getTahunbulan() == null ? 0 : tag.getTahunbulan();
						int tag2 = tagihan.getTahunbulan() == null ? 0 : tagihan.getTahunbulan();

						if (tag1 <= tag2 || cond1) {
							wajib.put(tag.getId());
						}
					}
				} else {
					if (tagihan.getItemBiayaSekolah() != null
							&& tagihan.getItemBiayaSekolah().getHarusBayar() != null) {
						List<Long> harusBayars = new ArrayList<Long>();
						harusBayars.add(tagihan.getItemBiayaSekolah().getHarusBayar().getId());

						checkUlangRekursif(harusBayars, tagihan, tagihans);

						if (tag.getItemBiayaSekolah() != null
								&& harusBayars.contains(tag.getItemBiayaSekolah().getId())) {
							wajib.put(tag.getId());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:529");
		}
		return wajib;
	}

	/**
	 * Representasi teks tagihan untuk log, combobox, dan pesan diagnostik, berbentuk
	 * <code>id-siswa - calonSiswa - itemBiaya - tahunbulan-bayarKe-nominal</code>.
	 *
	 * <p><b>Perhatian:</b> method ini <b>bukan operasi baca murni</b>. Ia memanggil
	 * {@link #getSiswa()}, {@link #getCalonSiswa()}, {@link #getItemBiayaSekolah()}, dan
	 * {@link #getBayarKe()} yang semuanya menulis balik ke field. Menaruh sebuah
	 * <code>Tagihan</code> ke dalam pesan log karena itu dapat mengubah isi baris pada
	 * flush berikutnya. Field {@link #nominal} sengaja dibaca mentah (tanpa
	 * {@link #getNominal()}) agar setidaknya angka rupiah tidak ikut dihitung ulang.</p>
	 *
	 * <p>Seluruh <i>exception</i> ditelan menjadi string kosong supaya kegagalan render
	 * satu baris tidak menjatuhkan seluruh grid.</p>
	 *
	 * @return ringkasan tagihan, atau string kosong bila terjadi kesalahan
	 */
	@Override
	public String toString() {
		try {
			siswa = getSiswa();
			calonSiswa = getCalonSiswa();
			itemBiayaSekolah = getItemBiayaSekolah();
			return id + "-" + (siswa == null ? "" : siswa + " - ") + (calonSiswa == null ? "" : calonSiswa + " - ")
					+ (itemBiayaSekolah == null ? "" : itemBiayaSekolah + " - ") + tahunbulan + "-" + getBayarKe() + "-"
					+ nominal;
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Mencari satu tagihan berdasarkan kunci identitas logisnya ({@link #getKodeUnik()}).
	 * Inilah pintu masuk baku "apakah tagihan ini sudah pernah dibuat?" yang dipakai
	 * {@link #ambilAtauBuat}, {@link #buatAtauLoadTagihan},
	 * {@link #saveTagihanDenganKodeUnikAman}, dan servlet <code>/Api</code>.
	 * <p>
	 * Diurutkan <code>id</code> menaik lalu diambil satu — jadi bila (karena data lama)
	 * ada lebih dari satu baris berkode sama, yang TERTUA yang dimenangkan. Ini disengaja:
	 * baris tertua adalah yang paling mungkin sudah tertaut ke pembayaran.
	 * <p>
	 * Bersifat defensif terhadap sesi yang sudah tertutup dan kode kosong: keduanya
	 * mengembalikan {@code null}, bukan melempar.
	 *
	 * @param kodeUnik kunci identitas tagihan; kosong/{@code null} menghasilkan {@code null}
	 * @param session  sesi Hibernate terbuka; tertutup/{@code null} menghasilkan {@code null}
	 * @return tagihan dengan kode tersebut, atau {@code null} bila tidak ada
	 */
	public static Tagihan findByKodeUnik(String kodeUnik, Session session) {
		if (kodeUnik == null || kodeUnik.trim().length() == 0 || session == null || !session.isOpen()) {
			return null;
		}
		return (Tagihan) session.createCriteria(Tagihan.class).add(Restrictions.eq("kodeUnik", kodeUnik))
				.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
	}
	/**
	 * Mencari tagihan LAIN yang memakai kunci identitas yang sama — yaitu deteksi tabrakan
	 * <code>kode_unik</code> sebelum menyimpan.
	 * <p>
	 * Berbeda dari {@link #findByKodeUnik}, method ini <b>tidak</b> mengurutkan hasil dan
	 * <b>tidak</b> defensif: <code>session</code> {@code null}/tertutup akan melempar. Bila
	 * <code>id</code> diberikan, baris dengan id tersebut dikecualikan sehingga entity yang
	 * sedang disunting tidak dianggap bertabrakan dengan dirinya sendiri.
	 *
	 * @param kodeUnik kunci identitas yang diuji
	 * @param id       id yang dikecualikan (entity yang sedang disunting), boleh {@code null}
	 * @param session  sesi Hibernate terbuka (wajib)
	 * @return tagihan lain berkunci sama, atau {@code null} bila tidak ada tabrakan
	 */
	public static Tagihan findByKodeUnikLain(String kodeUnik, Long id, Session session) {
		Criteria criteria = session.createCriteria(Tagihan.class).add(Restrictions.eq("kodeUnik", kodeUnik));
		if (id != null) {
			criteria.add(Restrictions.ne("id", id));
		}
		return (Tagihan) criteria.setMaxResults(1).uniqueResult();
	}

	/**
	 * Pastikan entity dari cache benar-benar mewakili kunci angsuran yang diminta.
	 * Nomor bayarKe dapat berubah ketika cicilan dirapikan; cache lama sebelumnya masih
	 * dapat menunjuk entity tersebut lewat kunci sebelum perubahan. Akibatnya satu
	 * pembayaran tampil pada dua urutan angsuran.
	 *
	 * <p>Penjaga ini menjadi jauh lebih penting karena cache
	 * <code>MemoryDbUtil.getAllTagihan()</code> bersifat <b>global untuk seluruh proses</b>
	 * dan hanya di-key oleh <code>kodeUnik</code>, tanpa pemisahan sekolah/yayasan. Tanpa
	 * verifikasi ulang di sini, satu kunci basi cukup untuk mengembalikan tagihan milik
	 * siswa lain kepada pemanggil — dan karena {@link #getSiswa()} menulis balik, tagihan
	 * itu berpotensi ikut berpindah tangan.</p>
	 *
	 * <p>Tiga hal diverifikasi, semuanya harus lolos:</p>
	 * <ol>
	 *   <li>nomor angsuran entity sama dengan yang diminta;</li>
	 *   <li>bila <code>nominalBiaya</code> diberikan dan kedua id tersedia, keduanya harus
	 *       menunjuk tarif yang sama;</li>
	 *   <li>kunci yang dihitung ULANG dari isi entity ({@link #genCode}) harus persis sama
	 *       dengan <code>kodeUnik</code> yang diminta — ini yang menangkap kasus
	 *       <code>kode_unik</code> historis yang tertinggal di kolom.</li>
	 * </ol>
	 *
	 * <p>Seluruh <i>exception</i> dianggap "tidak cocok" ({@code false}), yaitu <b>gagal
	 * ke sisi aman</b>: lebih baik memuat ulang dari database daripada memakai entity yang
	 * meragukan.</p>
	 *
	 * @param tagihan      entity kandidat (dari cache atau hasil query)
	 * @param kodeUnik     kunci identitas yang sedang dicari
	 * @param bayarKe      nomor angsuran yang sedang dicari
	 * @param nominalBiaya tarif yang seharusnya menaungi angsuran ini, boleh {@code null}
	 * @return {@code true} bila entity benar-benar mewakili kunci yang diminta
	 */
	private static boolean cocokDenganKunciTagihan(Tagihan tagihan, String kodeUnik, Integer bayarKe,
			NominalBiaya nominalBiaya) {
		if (tagihan == null || kodeUnik == null || bayarKe == null) {
			return false;
		}
		try {
			if (!bayarKe.equals(tagihan.getBayarKe())) {
				return false;
			}
			if (nominalBiaya != null && nominalBiaya.getId() != null && tagihan.getNominalBiaya() != null
					&& tagihan.getNominalBiaya().getId() != null
					&& !nominalBiaya.getId().equals(tagihan.getNominalBiaya().getId())) {
				return false;
			}
			String kodeAktual = genCode(tagihan.getItemBiayaSekolah(), tagihan.getPengaturanBiaya(),
					tagihan.getTahunbulan(), tagihan.getSiswa(), tagihan.getCalonSiswa(), tagihan.getBayarKe());
			return kodeUnik.equals(kodeAktual);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Memeriksa apakah sebuah <i>exception</i> (beserta seluruh rantai penyebabnya)
	 * disebabkan oleh pelanggaran indeks unik <code>tagihan_kode_unik_key</code>.
	 * <p>
	 * Deteksinya berbasis <b>pencocokan teks pesan</b> pada nama indeks database. Ini
	 * rapuh terhadap perubahan nama indeks maupun lokalisasi pesan driver, tetapi merupakan
	 * satu-satunya cara membedakan tabrakan kunci dari kegagalan INSERT lain tanpa
	 * bergantung pada kode SQLSTATE spesifik vendor.
	 * <p>
	 * Perhatikan bahwa indeks tersebut <b>tidak dideklarasikan</b> pada mapping
	 * {@link #getKodeUnik()} (yang justru menyatakan <code>unique = false</code>) — jaminan
	 * keunikannya hidup di skema database, bukan di kode.
	 *
	 * @param e exception yang diperiksa, boleh {@code null}
	 * @return {@code true} bila salah satu penyebab menyebut <code>tagihan_kode_unik_key</code>
	 */
	private static boolean isDuplicateKodeUnikException(Throwable e) {
		Throwable t = e;
		while (t != null) {
			String msg = t.getMessage();
			if (msg != null && msg.toLowerCase().indexOf("tagihan_kode_unik_key") >= 0) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Menyimpan tagihan baru dengan penanganan tabrakan <code>kode_unik</code> yang aman
	 * terhadap balapan (<i>race</i>) antar-permintaan.
	 *
	 * <p>Alurnya bersifat "cek-simpan-cek":</p>
	 * <ol>
	 *   <li>cari lebih dahulu; bila kodenya sudah ada, kembalikan baris yang ada dan
	 *       <b>jangan</b> menyimpan apa pun;</li>
	 *   <li>simpan di dalam transaksi — transaksi baru dibuka HANYA bila pemanggil belum
	 *       punya transaksi aktif, dan hanya transaksi yang dibuka sendiri yang di-commit
	 *       (transaksi milik pemanggil tidak diserobot);</li>
	 *   <li>bila gagal karena indeks unik dilanggar (dua permintaan menyimpan kode yang
	 *       sama nyaris bersamaan), sesi dibersihkan lalu baris pemenang dicari kembali dan
	 *       dikembalikan — sehingga pemanggil tetap memperoleh tagihan yang valid alih-alih
	 *       error.</li>
	 * </ol>
	 *
	 * <p><code>session.clear()</code> pada blok penanganan galat penting: tanpa itu entity
	 * gagal-simpan tertinggal di <i>persistence context</i> dan akan meracuni flush
	 * berikutnya milik pemanggil.</p>
	 *
	 * <p>Kegagalan yang BUKAN tabrakan kunci dilempar ulang apa adanya.</p>
	 *
	 * @param session sesi Hibernate; {@code null} membuat method mengembalikan argumen apa adanya
	 * @param tagihan tagihan yang hendak disimpan; {@code null} dikembalikan apa adanya
	 * @return tagihan yang tersimpan, atau baris existing bila kodenya sudah dipakai
	 * @throws Exception bila penyimpanan gagal karena sebab selain tabrakan kunci unik
	 */
	private static Tagihan saveTagihanDenganKodeUnikAman(Session session, Tagihan tagihan) throws Exception {
		if (session == null || tagihan == null) {
			return tagihan;
		}
		String kodeUnik = tagihan.getKodeUnik();
		Tagihan existing = findByKodeUnik(kodeUnik, session);
		if (existing != null) {
			return existing;
		}
		Transaction tx = null;
		boolean createdTx = false;
		try {
			tx = session.getTransaction();
			if (tx == null || !tx.isActive()) {
				tx = session.beginTransaction();
				createdTx = true;
			}
			session.save(tagihan);
			if (createdTx && tx != null && tx.isActive()) {
				tx.commit();
			}
			return tagihan;
		} catch (Exception e) {
			try { if (createdTx && tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:598");}
			try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:599");}
			if (isDuplicateKodeUnikException(e)) {
				existing = findByKodeUnik(kodeUnik, session);
				if (existing != null) {
					return existing;
				}
			}
			throw e;
		}
	}


	/**
	 * Urutan tampil baku tagihan di grid, kuitansi, dan daftar pembayaran.
	 *
	 * <p>Kunci pengurutan berjenjang, dari yang paling menentukan:</p>
	 * <ol>
	 *   <li>id {@link #getPengaturanBiaya()} — mengelompokkan tagihan per paket biaya;</li>
	 *   <li>nama {@link #getItemBiayaSekolah()} (abaikan besar-kecil huruf);</li>
	 *   <li>{@link #getBayarKe()} — angsuran ke-1 sebelum ke-2;</li>
	 *   <li>{@link #getTahunbulan()} — periode lebih awal lebih dahulu;</li>
	 *   <li>{@link #getId()} sebagai pemutus terakhir agar urutan stabil.</li>
	 * </ol>
	 *
	 * <p>Nilai {@code null} selalu diurutkan lebih DAHULU pada setiap tingkat (lihat
	 * {@link #bandingLong}, {@link #bandingInteger}, {@link #bandingString}).</p>
	 *
	 * <p><b>Konsekuensi penting:</b> karena kunci ini membaca lima getter yang menulis
	 * balik, <b>mengurutkan sebuah koleksi tagihan bukan operasi baca murni</b> — pada
	 * entity terkelola, sekadar <code>Collections.sort(...)</code> dapat memicu perubahan
	 * yang tersimpan pada flush berikutnya.</p>
	 *
	 * <p>Objek yang bukan <code>Tagihan</code> didelegasikan ke implementasi induk
	 * {@link ais.database.model.GeneralValueObject#compareTo}.</p>
	 *
	 * @param arg0 objek pembanding
	 * @return bilangan negatif/nol/positif sesuai kontrak {@link Comparable}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (!(arg0 instanceof Tagihan)) {
			return super.compareTo(arg0);
		}
		Tagihan tagihan = (Tagihan) arg0;
		int hasil = bandingLong(ambilPengaturanBiayaId(this), ambilPengaturanBiayaId(tagihan));
		if (hasil != 0) return hasil;
		hasil = bandingString(ambilNamaItem(this), ambilNamaItem(tagihan));
		if (hasil != 0) return hasil;
		hasil = bandingInteger(getBayarKe(), tagihan.getBayarKe());
		if (hasil != 0) return hasil;
		hasil = bandingInteger(getTahunbulan(), tagihan.getTahunbulan());
		if (hasil != 0) return hasil;
		return bandingLong(getId(), tagihan.getId());
	}

	/**
	 * Pembantu {@link #compareTo(GeneralValueObject)}: mengambil id paket biaya secara aman.
	 *
	 * @param tagihan tagihan yang dibaca
	 * @return id {@link #getPengaturanBiaya()}, atau {@code null} bila relasinya kosong
	 */
	private static Long ambilPengaturanBiayaId(Tagihan tagihan) {
		return tagihan.getPengaturanBiaya() == null ? null : tagihan.getPengaturanBiaya().getId();
	}

	/**
	 * Pembantu {@link #compareTo(GeneralValueObject)}: mengambil nama komponen biaya
	 * secara aman.
	 *
	 * @param tagihan tagihan yang dibaca
	 * @return nama {@link #getItemBiayaSekolah()}, atau {@code null} bila relasinya kosong
	 */
	private static String ambilNamaItem(Tagihan tagihan) {
		return tagihan.getItemBiayaSekolah() == null ? null : tagihan.getItemBiayaSekolah().getNama();
	}

	/**
	 * Pembanding {@link Long} yang tahan {@code null} untuk
	 * {@link #compareTo(GeneralValueObject)}; {@code null} diurutkan lebih dahulu.
	 *
	 * @param kiri  operan kiri, boleh {@code null}
	 * @param kanan operan kanan, boleh {@code null}
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}
	 */
	private static int bandingLong(Long kiri, Long kanan) {
		if (kiri == kanan) return 0;
		if (kiri == null) return -1;
		if (kanan == null) return 1;
		return kiri.compareTo(kanan);
	}

	/**
	 * Pembanding {@link Integer} yang tahan {@code null} untuk
	 * {@link #compareTo(GeneralValueObject)}; {@code null} diurutkan lebih dahulu.
	 *
	 * @param kiri  operan kiri, boleh {@code null}
	 * @param kanan operan kanan, boleh {@code null}
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}
	 */
	private static int bandingInteger(Integer kiri, Integer kanan) {
		if (kiri == kanan) return 0;
		if (kiri == null) return -1;
		if (kanan == null) return 1;
		return kiri.compareTo(kanan);
	}

	/**
	 * Pembanding {@link String} yang tahan {@code null} dan mengabaikan besar-kecil huruf,
	 * untuk mengurutkan nama komponen biaya di {@link #compareTo(GeneralValueObject)};
	 * {@code null} diurutkan lebih dahulu.
	 *
	 * @param kiri  operan kiri, boleh {@code null}
	 * @param kanan operan kanan, boleh {@code null}
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}
	 */
	private static int bandingString(String kiri, String kanan) {
		if (kiri == kanan) return 0;
		if (kiri == null) return -1;
		if (kanan == null) return 1;
		return kiri.compareToIgnoreCase(kanan);
	}

	// =========================================================================================
	// OPTIMISASI: Pengelolaan Session Database dengan blok finally yang ketat
	// =========================================================================================
	/**
	 * Varian ringkas {@link #ambilAtauBuat(ItemBiayaSekolah, PengaturanBiaya, Siswa,
	 * CalonSiswa, Integer, NominalBiaya, Integer, PengaturanBiayaItemBiaya, boolean)}
	 * dengan <code>ambilManual = false</code> (boleh memakai cache memori).
	 *
	 * @param itemBiayaSekolah         komponen biaya yang ditagih
	 * @param pengaturanBiaya          paket biaya yang menaungi
	 * @param siswa                    siswa yang ditagih, atau {@code null}
	 * @param calonSiswa               calon siswa yang ditagih, atau {@code null}
	 * @param bayarKe                  nomor urut angsuran
	 * @param nominalBiaya             tarif termaterialisasi (wajib)
	 * @param tahunbulan               periode YYYYMM
	 * @param pengaturanBiayaItemBiaya konfigurasi komponen, dipakai bila tarif perlu dibuat ulang
	 * @return tagihan yang sudah ada atau baru dibuat, atau {@code null} bila gagal/ditolak
	 */
	public static Tagihan ambilAtauBuat(ItemBiayaSekolah itemBiayaSekolah, PengaturanBiaya pengaturanBiaya, Siswa siswa,
			CalonSiswa calonSiswa, Integer bayarKe, NominalBiaya nominalBiaya, Integer tahunbulan,
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya) {
		return ambilAtauBuat(itemBiayaSekolah, pengaturanBiaya, siswa, calonSiswa, bayarKe, nominalBiaya, tahunbulan,
				pengaturanBiayaItemBiaya, false);
	}

	/**
	 * Varian yang <b>membuka sesi Hibernate sendiri</b> dan menambahkan pemulihan khusus
	 * untuk angsuran ke-2 ke atas.
	 * <p>
	 * Setelah tagihan diperoleh, bila nomor angsurannya &gt; 1 method ini memastikan
	 * barisnya benar-benar ADA di database. Kalau ternyata tidak ada (entity berasal dari
	 * cache basi atau dari sesi lain yang sudah tertutup), id dikosongkan, tarif
	 * <b>dibuat/diambil ulang</b> lewat <code>TagihanUtil.ambilNominalBiaya(...)</code>, lalu
	 * baris disimpan sebagai tagihan baru melalui {@link #saveTagihanDenganKodeUnikAman}.
	 * <p>
	 * Sesi yang dibuka selalu ditutup di blok <code>finally</code>; seluruh kegagalan
	 * dicatat ke audit error dan menghasilkan {@code null}, bukan exception ke pemanggil.
	 *
	 * @param itemBiayaSekolah         komponen biaya yang ditagih
	 * @param pengaturanBiaya          paket biaya yang menaungi
	 * @param siswa                    siswa yang ditagih, atau {@code null}
	 * @param calonSiswa               calon siswa yang ditagih, atau {@code null}
	 * @param bayarKe                  nomor urut angsuran
	 * @param nominalBiaya             tarif termaterialisasi (wajib)
	 * @param tahunbulan               periode YYYYMM
	 * @param pengaturanBiayaItemBiaya konfigurasi komponen, dipakai saat tarif dibuat ulang
	 * @param ambilManual              {@code true} untuk MELEWATI cache memori dan selalu
	 *                                 membaca dari database
	 * @return tagihan yang sudah ada atau baru dibuat, atau {@code null} bila gagal/ditolak
	 */
	public static Tagihan ambilAtauBuat(ItemBiayaSekolah itemBiayaSekolah, PengaturanBiaya pengaturanBiaya, Siswa siswa,
			CalonSiswa calonSiswa, Integer bayarKe, NominalBiaya nominalBiaya, Integer tahunbulan,
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, boolean ambilManual) {
		Tagihan tagihan = null;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tagihan = ambilAtauBuat(session, itemBiayaSekolah, pengaturanBiaya, siswa, calonSiswa, bayarKe,
					nominalBiaya, tahunbulan, ambilManual);

			if (tagihan != null && tagihan.getBayarKe() != null && tagihan.getBayarKe() > 1) {
				int count = tagihan.getId() == null ? 0
						: ((Number) session.createCriteria(Tagihan.class).add(Restrictions.idEq(tagihan.getId()))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				if (count == 0) {
					tagihan.setId(null);
					NominalBiaya nominalBiayaBaru = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
							tahunbulan, session);
					tagihan.setNominalBiaya(nominalBiayaBaru);

					tagihan = saveTagihanDenganKodeUnikAman(session, tagihan);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:683");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/database/model/sekolah/Tagihan.java:689");
				}
			}
		}
		return tagihan;
	}

	/**
	 * Varian paling ringkas: membuka sesi sendiri, tanpa konfigurasi komponen, dengan
	 * <code>ambilManual = false</code>.
	 *
	 * @param itemBiayaSekolah komponen biaya yang ditagih
	 * @param pengaturanBiaya  paket biaya yang menaungi
	 * @param siswa            siswa yang ditagih, atau {@code null}
	 * @param calonSiswa       calon siswa yang ditagih, atau {@code null}
	 * @param bayarKe          nomor urut angsuran
	 * @param nominalBiaya     tarif termaterialisasi (wajib)
	 * @param tahunbulan       periode YYYYMM
	 * @return tagihan yang sudah ada atau baru dibuat, atau {@code null} bila gagal/ditolak
	 */
	public static Tagihan ambilAtauBuat(ItemBiayaSekolah itemBiayaSekolah, PengaturanBiaya pengaturanBiaya, Siswa siswa,
			CalonSiswa calonSiswa, Integer bayarKe, NominalBiaya nominalBiaya, Integer tahunbulan) {
		return ambilAtauBuat(itemBiayaSekolah, pengaturanBiaya, siswa, calonSiswa, bayarKe, nominalBiaya, tahunbulan,
				false);
	}

	/**
	 * Varian yang membuka sesi Hibernate sendiri lalu mendelegasikan ke inti
	 * {@link #ambilAtauBuat(Session, ItemBiayaSekolah, PengaturanBiaya, Siswa, CalonSiswa,
	 * Integer, NominalBiaya, Integer, Double, boolean, boolean)}. Tidak melakukan
	 * pemulihan angsuran ke-2 seperti varian ber-<code>PengaturanBiayaItemBiaya</code>.
	 * <p>
	 * Sesi selalu ditutup di <code>finally</code>; kegagalan dicatat dan menghasilkan
	 * {@code null}.
	 *
	 * @param itemBiayaSekolah komponen biaya yang ditagih
	 * @param pengaturanBiaya  paket biaya yang menaungi
	 * @param siswa            siswa yang ditagih, atau {@code null}
	 * @param calonSiswa       calon siswa yang ditagih, atau {@code null}
	 * @param bayarKe          nomor urut angsuran
	 * @param nominalBiaya     tarif termaterialisasi (wajib)
	 * @param tahunbulan       periode YYYYMM
	 * @param ambilManual      {@code true} untuk melewati cache memori
	 * @return tagihan yang sudah ada atau baru dibuat, atau {@code null} bila gagal/ditolak
	 */
	public static Tagihan ambilAtauBuat(ItemBiayaSekolah itemBiayaSekolah, PengaturanBiaya pengaturanBiaya, Siswa siswa,
			CalonSiswa calonSiswa, Integer bayarKe, NominalBiaya nominalBiaya, Integer tahunbulan,
			boolean ambilManual) {
		Tagihan tagihan = null;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tagihan = ambilAtauBuat(session, itemBiayaSekolah, pengaturanBiaya, siswa, calonSiswa, bayarKe,
					nominalBiaya, tahunbulan, null, ambilManual);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:712");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/database/model/sekolah/Tagihan.java:718");
				}
			}
		}
		return tagihan;
	}

	/**
	 * Varian yang memakai sesi milik pemanggil, tanpa nominal angsuran manual.
	 *
	 * @param session          sesi Hibernate milik pemanggil (tidak ditutup di sini)
	 * @param itemBiayaSekolah komponen biaya yang ditagih
	 * @param pengaturanBiaya  paket biaya yang menaungi
	 * @param siswa            siswa yang ditagih, atau {@code null}
	 * @param calonSiswa       calon siswa yang ditagih, atau {@code null}
	 * @param bayarKe          nomor urut angsuran
	 * @param nominalBiaya     tarif termaterialisasi (wajib)
	 * @param tahunbulan       periode YYYYMM
	 * @param ambilManual      {@code true} untuk melewati cache memori
	 * @return tagihan yang sudah ada atau baru dibuat, atau {@code null} bila gagal/ditolak
	 */
	public static Tagihan ambilAtauBuat(Session session, ItemBiayaSekolah itemBiayaSekolah,
			PengaturanBiaya pengaturanBiaya, Siswa siswa, CalonSiswa calonSiswa, Integer bayarKe,
			NominalBiaya nominalBiaya, Integer tahunbulan, boolean ambilManual) {
		return ambilAtauBuat(session, itemBiayaSekolah, pengaturanBiaya, siswa, calonSiswa, bayarKe, nominalBiaya,
				tahunbulan, null, ambilManual);
	}

	/**
	 * Varian yang memakai sesi milik pemanggil dan menerima nominal angsuran manual, dengan
	 * <code>chekPembayaranJuga = false</code> — artinya <b>tidak</b> mencari baris pelunasan
	 * yatim yang belum tertaut ke tagihan mana pun.
	 *
	 * @param session          sesi Hibernate milik pemanggil (tidak ditutup di sini)
	 * @param itemBiayaSekolah komponen biaya yang ditagih
	 * @param pengaturanBiaya  paket biaya yang menaungi
	 * @param siswa            siswa yang ditagih, atau {@code null}
	 * @param calonSiswa       calon siswa yang ditagih, atau {@code null}
	 * @param bayarKe          nomor urut angsuran
	 * @param nominalBiaya     tarif termaterialisasi (wajib)
	 * @param tahunbulan       periode YYYYMM
	 * @param dibayarManual    nominal angsuran yang ditetapkan petugas, atau {@code null}
	 * @param ambilManual      {@code true} untuk melewati cache memori
	 * @return tagihan yang sudah ada atau baru dibuat, atau {@code null} bila gagal/ditolak
	 */
	public static Tagihan ambilAtauBuat(Session session, ItemBiayaSekolah itemBiayaSekolah,
			PengaturanBiaya pengaturanBiaya, Siswa siswa, CalonSiswa calonSiswa, Integer bayarKe,
			NominalBiaya nominalBiaya, Integer tahunbulan, Double dibayarManual, boolean ambilManual) {
		boolean chekPembayaranJuga = false;
		return ambilAtauBuat(session, itemBiayaSekolah, pengaturanBiaya, siswa, calonSiswa, bayarKe, nominalBiaya,
				tahunbulan, dibayarManual, ambilManual, chekPembayaranJuga);
	}

	/**
	 * Mengambil atau membuat Tagihan baru. Telah dioptimasi: Perbaikan alias
	 * Hibernate (Left Join), pencegahan memory leak, dan manajemen transaksi
	 * database yang aman (Auto-Rollback).
	 *
	 * <p><b>Inti dari seluruh keluarga <code>ambilAtauBuat</code></b> — semua overload lain
	 * bermuara ke sini. Method inilah yang menegakkan aturan "satu kewajiban = satu baris",
	 * yaitu pertahanan utama terhadap tagihan ganda.</p>
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li><b>Penolakan dini.</b> <code>bayarKe &gt; 50</code> ditolak (pengaman terhadap
	 *       loop generator yang lepas kendali), begitu pula tarif yang tidak lengkap
	 *       (<code>nominalBiaya</code>, paket biaya, atau jenis biayanya {@code null}) —
	 *       keduanya mengembalikan {@code null} tanpa menyentuh database.</li>
	 *   <li><b>Hitung kunci identitas</b> lewat {@link #genCode}. Perhatikan bahwa siswa/calon
	 *       siswa yang dipakai diambil dari <code>nominalBiaya</code>, BUKAN dari parameter
	 *       <code>siswa</code>/<code>calonSiswa</code> — tarif yang menjadi acuan kebenaran.</li>
	 *   <li><b>Cache memori.</b> Bila <code>ambilManual == false</code>, entity dicari di
	 *       <code>MemoryDbUtil.getAllTagihan()</code>. Hasilnya WAJIB lolos
	 *       {@link #cocokDenganKunciTagihan}; bila tidak, hanya kunci yang diminta yang
	 *       dibuang dari cache (entity itu sendiri mungkin masih sah pada kunci barunya).</li>
	 *   <li><b>Database.</b> {@link #findByKodeUnik}; bila hasilnya tidak lolos verifikasi
	 *       kunci, dicoba lagi berdasarkan identitas angsuran sebenarnya
	 *       (<code>nominalBiaya</code> + <code>bayarKe</code>, id terbesar) — ini menangani
	 *       <code>kode_unik</code> historis yang tertinggal setelah penomoran angsuran
	 *       pernah dirapikan.</li>
	 *   <li><b>Pembuatan baru.</b> Bila tetap tidak ketemu: periode disimpulkan dari
	 *       <code>tahunbulan</code> bila berformat YYYYMM, kalau tidak dari
	 *       <code>JenisBiayaSekolah.untukTahun/untukBulan</code>. Dengan
	 *       <code>chekPembayaranJuga</code>, dicari lebih dahulu baris pelunasan "yatim"
	 *       yang cocok (dua query berurutan: dengan filter bulan/tahun, lalu tanpa filter
	 *       sebagai <i>fallback</i>); bila ketemu dan sudah punya tagihan, tagihan ITULAH
	 *       yang dikembalikan alih-alih membuat baru. Alias <code>"detail"</code>/
	 *       <code>"ps"</code> diberi nama eksplisit dengan <code>LEFT_JOIN</code> agar entity
	 *       akar tidak bergeser (bug pengurutan yang disebut komentar di dalam kode).</li>
	 *   <li><b>Penjaga <code>nominal_biaya_id</code> NOT NULL.</b> Sebelum INSERT, id tarif
	 *       dibaca (aman pada proxy, tidak memicu lazy-load). Bila id-nya {@code null},
	 *       insert <b>DIBATALKAN</b> dan {@code null} dikembalikan. Komentar di dalam kode
	 *       menjelaskan alasannya: tanpa penjaga ini <code>ConstraintViolationException</code>
	 *       akan ikut membatalkan transaksi milik pemanggil pada flush berikutnya —
	 *       "lebih baik gagal senyap/tercatat daripada meracuni transaksi pemanggil".</li>
	 *   <li><b>Pasca-simpan.</b> Baris pelunasan yatim ditautkan ke tagihan baru dalam
	 *       transaksinya sendiri; bila tagihan membawa diskon yang TIDAK memotong tagihan,
	 *       pengajuan transfer diskon dicatat lewat
	 *       <code>DaftarPengajuanTransfer.simpanDiskonPembayaran(...)</code>.</li>
	 *   <li><b>Sinkronisasi <code>dibayarManual</code>.</b> Untuk tagihan yang sudah ada,
	 *       nilai baru (&gt; 0,1 dan berbeda) langsung ditulis dan di-commit.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> method ini MENULIS ke database (INSERT tagihan, UPDATE
	 * <code>PembayaranSiswaDetail</code>, UPDATE <code>dibayarManual</code>) memakai
	 * transaksi pendek yang dibuka dan di-commit sendiri, bukan transaksi pemanggil.</p>
	 *
	 * <p><b>Dipanggil dari:</b> generator tagihan (<code>TagihanUtil</code>,
	 * <code>TagihanUtilCalonSiswa</code>), layar rincian tagihan siswa/calon siswa, layar
	 * pembayaran online, dan alur bank. Karena helper-helper tersebut tidak memiliki
	 * <code>checkPrevilages</code> sendiri (lihat Javadoc kelas), tombol yang memicunya —
	 * "Cari", "Sinkronkan", "Reset", unggah Excel — berjalan dengan hak menu induk saja.</p>
	 *
	 * @param session            sesi Hibernate milik pemanggil (tidak ditutup di sini)
	 * @param itemBiayaSekolah   komponen biaya yang ditagih
	 * @param pengaturanBiaya    paket biaya yang menaungi (dipakai untuk tahun ajaran)
	 * @param siswa              siswa yang ditagih, atau {@code null}
	 * @param calonSiswa         calon siswa yang ditagih, atau {@code null}
	 * @param bayarKe            nomor urut angsuran; &gt; 50 langsung ditolak
	 * @param nominalBiaya       tarif termaterialisasi; wajib lengkap sampai jenis biayanya
	 * @param tahunbulan         periode YYYYMM
	 * @param dibayarManual      nominal angsuran manual, atau {@code null}
	 * @param ambilManual        {@code true} untuk melewati cache memori
	 * @param chekPembayaranJuga {@code true} untuk ikut mencari baris pelunasan yatim
	 * @return tagihan yang sudah ada atau baru dibuat; {@code null} bila ditolak penjaga
	 *         atau bila tarif tidak punya id valid
	 */
	public static Tagihan ambilAtauBuat(Session session, ItemBiayaSekolah itemBiayaSekolah,
			PengaturanBiaya pengaturanBiaya, Siswa siswa, CalonSiswa calonSiswa, Integer bayarKe,
			NominalBiaya nominalBiaya, Integer tahunbulan, Double dibayarManual, boolean ambilManual,
			boolean chekPembayaranJuga) {

		// Validasi awal untuk mencegah eksekusi berlebih
		if (bayarKe != null && bayarKe > 50)
			return null;
		if (nominalBiaya == null || nominalBiaya.getPengaturanBiaya() == null
				|| nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah() == null) {
			return null;
		}

		String kodeUnik = Tagihan.genCode(itemBiayaSekolah, nominalBiaya.getPengaturanBiaya(), tahunbulan,
				nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(), bayarKe);

		Tagihan tagihan = ambilManual ? null : MemoryDbUtil.getAllTagihan().get(kodeUnik);
		if (tagihan != null && !cocokDenganKunciTagihan(tagihan, kodeUnik, bayarKe, nominalBiaya)) {
			// Buang hanya kunci yang diminta. Entity yang sama mungkin masih valid pada
			// kunci barunya dan akan dimasukkan kembali oleh AuditListener/reload cache.
			MemoryDbUtil.getAllTagihan().remove(kodeUnik);
			tagihan = null;
		}

		// Ambil dari database jika tidak ada di memori
		if (tagihan == null) {
			tagihan = Tagihan.findByKodeUnik(kodeUnik, session);
			if (tagihan != null && !cocokDenganKunciTagihan(tagihan, kodeUnik, bayarKe, nominalBiaya)) {
				// kode_unik historis dapat tertinggal setelah bayarKe pernah dirapikan.
				// Cari entity berdasarkan identitas angsuran sebenarnya agar baris lain
				// tidak ikut dianggap sebagai pembayaran untuk urutan ini.
				tagihan = (Tagihan) session.createCriteria(Tagihan.class)
						.add(Restrictions.eq("nominalBiaya", nominalBiaya))
						.add(Restrictions.eq("bayarKe", bayarKe)).addOrder(Order.desc("id"))
						.setMaxResults(1).uniqueResult();
				if (tagihan != null && !cocokDenganKunciTagihan(tagihan, kodeUnik, bayarKe, nominalBiaya)) {
					tagihan = null;
				}
			}
			if (tagihan != null && !ambilManual) {
				MemoryDbUtil.getAllTagihan().put(kodeUnik, tagihan);
			}
		}

		if (tagihan == null) {
			try {
				Integer tahun = nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getUntukTahun();
				Integer bulan = nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getUntukBulan();

				if (tahunbulan != null && tahunbulan > 2100) {
					tahun = tahunbulan / 100;
					bulan = tahunbulan % 100;
				}
				PembayaranSiswaDetail pembayaranSiswaDetail = null;
				if (chekPembayaranJuga) {
					// OPTIMASI: Menggunakan createAlias dengan nama agar Root Entity tidak bergeser
					// (Fix Sorting Bug)
					// Wajib LEFT_OUTER_JOIN pada tagihan, karena kita juga mencari detail yang
					// belum punya tagihan
					Criteria critWithBulanTahun = session.createCriteria(PembayaranSiswaDetail.class, "detail")
							.createAlias("detail.tagihan", "tagihan", Criteria.LEFT_JOIN)
							.createAlias("detail.pembayaranSiswa", "ps", Criteria.LEFT_JOIN)

							.add(Restrictions.or(Restrictions.isNull("detail.tagihan"),
									Restrictions.eq("tagihan.bayarKe", bayarKe)))
							.add(Restrictions.eq("detail.nominalBiaya", nominalBiaya))
							.add(Restrictions.eq("detail.itemBiayaSekolah", itemBiayaSekolah))

							.add(Restrictions.eq("ps.siswa", siswa))
							.add(Restrictions.eq("ps.jenisBiayaSekolah",
									nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah()))
							.add(Restrictions.or(Restrictions.isNull("ps.bulan"), Restrictions.eq("ps.bulan", bulan)))
							.add(Restrictions.or(Restrictions.isNull("ps.tahun"), Restrictions.eq("ps.tahun", tahun)))

							.addOrder(Order.desc("detail.id")).setMaxResults(1);

					pembayaranSiswaDetail = (PembayaranSiswaDetail) critWithBulanTahun.uniqueResult();

					// Fallback jika tidak ditemukan (Query tanpa filter bulan & tahun)
					if (pembayaranSiswaDetail == null) {
						Criteria critFallback = session.createCriteria(PembayaranSiswaDetail.class, "detail")
								.createAlias("detail.tagihan", "tagihan", Criteria.LEFT_JOIN)
								.createAlias("detail.pembayaranSiswa", "ps", Criteria.LEFT_JOIN)

								.add(Restrictions.or(Restrictions.isNull("detail.tagihan"),
										Restrictions.eq("tagihan.bayarKe", bayarKe)))
								.add(Restrictions.eq("detail.nominalBiaya", nominalBiaya))
								.add(Restrictions.eq("detail.itemBiayaSekolah", itemBiayaSekolah))

								.add(Restrictions.eq("ps.siswa", siswa))
								.add(Restrictions.eq("ps.jenisBiayaSekolah",
										nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah()))

								.addOrder(Order.desc("detail.id")).setMaxResults(1);

						pembayaranSiswaDetail = (PembayaranSiswaDetail) critFallback.uniqueResult();
					}
				}

				if (pembayaranSiswaDetail == null || pembayaranSiswaDetail.getTagihan() == null) {
					tagihan = new Tagihan();
					tagihan.setNominalBiaya(nominalBiaya);
					tagihan.setBulan(bulan);
					tagihan.setTahun(tahun);
					tagihan.setTahunbulan(tahunbulan);
					tagihan.setTahunAjaran(pengaturanBiaya.getTahunAjaran());
					tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
					tagihan.setSiswa(siswa);
					tagihan.setCalonSiswa(calonSiswa);
					tagihan.setPengaturanBiaya(pengaturanBiaya);
					tagihan.setItemBiayaSekolah(itemBiayaSekolah);
					tagihan.setBayarKe(bayarKe);
					tagihan.setKodeUnik(kodeUnik);

					if (dibayarManual != null) {
						tagihan.setDibayarManual(dibayarManual);
					}

					// KE-13/14/16: kolom "nominal_biaya_id" NOT NULL di tabel tagihan -- meski
					// tagihan.setNominalBiaya(nominalBiaya) di atas sudah dipanggil, nominalBiaya yang
					// dioper ke method ini bisa berasal dari sesi Hibernate LAIN yang sudah ditutup
					// (mis. dari NominalBiaya.ambilTagihans/TagihanUtilCalonSiswa yang membuka
					// beberapa sesi terisolasi berbeda). Bila referensi itu sudah tidak punya id valid
					// (proxy rusak/entity belum tersimpan), INSERT baris ini SELALU gagal dengan
					// ConstraintViolationException, dan sesi milik pemanggil (mis.
					// CommonHibernateHelper.safeFlush) ikut ter-abort pada flush berikutnya. Deteksi
					// & hentikan SEBELUM insert dicoba -- lebih baik gagal senyap/tercatat drpd crash
					// & meracuni transaksi pemanggil.
					Long idNominalBiaya = null;
					try {
						// getId() aman dipanggil pada proxy Hibernate TANPA memicu lazy-load penuh
						// (id disimpan langsung di proxy) -- jadi ini tidak menyentuh sesi manapun.
						if (tagihan.getNominalBiaya() != null) {
							idNominalBiaya = tagihan.getNominalBiaya().getId();
						}
					} catch (Exception exId) {
						ais.common.ErrorAuditUtil.record(exId,
								"Tagihan.ambilAtauBuat: gagal membaca id NominalBiaya sebelum insert Tagihan baru");
					}
					if (idNominalBiaya == null) {
						ais.common.ErrorAuditUtil.record(
								new IllegalStateException(
										"Tagihan.ambilAtauBuat: dibatalkan -- NominalBiaya referensi tidak punya id valid (kodeUnik="
												+ kodeUnik + "), insert Tagihan baru DILEWATI utk mencegah "
												+ "ConstraintViolationException nominal_biaya_id NOT NULL."),
								"auto-audit src/ais/database/model/sekolah/Tagihan.java:855-guard");
						return null;
					}

					tagihan = saveTagihanDenganKodeUnikAman(session, tagihan);

					if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null) {
						pembayaranSiswaDetail.setTagihan(tagihan);

						Transaction tx2 = session.beginTransaction();
						try {
							Common.refreshUpdate(session, pembayaranSiswaDetail);
							tx2.commit();
						} catch (Exception exUpdate) {
							if (tx2 != null && tx2.isActive())
								tx2.rollback();
							throw exUpdate;
						}

						if (tagihan.getDiskonSiswa() != null
								&& Boolean.FALSE.equals(tagihan.getDiskonSiswa().getMemotongTagihan())) {
							ais.database.model.akunting.DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
						}
					}
				} else {
					tagihan = pembayaranSiswaDetail.getTagihan();
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		// Pengecekan perubahan dibayarManual
		if (tagihan != null && tagihan.getId() != null && dibayarManual != null && dibayarManual > 0.1) {
			Double existingDibayarManual = tagihan.getDibayarManual() != null ? tagihan.getDibayarManual() : 0.0;

			if (Double.compare(existingDibayarManual, dibayarManual) != 0) {
				tagihan.setDibayarManual(dibayarManual);
				Transaction tx3 = session.beginTransaction();
				try {
					Common.refreshUpdate(session, tagihan);
					tx3.commit();
				} catch (Exception exMan) {
					if (tx3 != null && tx3.isActive())
						tx3.rollback();
					Common.tampilErrorJikaAdmin(exMan);
				}
			}
		}

		return tagihan;
	}

	/**
	 * Membentuk <b>kunci identitas logis</b> sebuah tagihan. Nilai inilah yang disimpan di
	 * kolom <code>kode_unik</code>, dipakai sebagai kunci cache global
	 * <code>MemoryDbUtil.getAllTagihan()</code>, dan menjadi dasar keputusan
	 * "ambil yang sudah ada" versus "buat baru" di {@link #ambilAtauBuat} dan
	 * {@link #buatAtauLoadTagihan}.
	 *
	 * <h4>Bentuk keluaran</h4>
	 * <ul>
	 *   <li>calon siswa: <code>c-{idCalonSiswa}-{idItem}-{idPengaturan}-{periode}[-{bayarKe}]</code></li>
	 *   <li>siswa aktif: <code>s-{idSiswa}-{idItem}-{idPengaturan}-{periode}[-{bayarKe}]</code></li>
	 *   <li>tanpa prefiks identitas: <code>{idPengaturan}-{periode}[-{bayarKe}]</code></li>
	 * </ul>
	 * Sufiks <code>-{bayarKe}</code> hanya ditambahkan untuk angsuran ke-2 ke atas, sehingga
	 * angsuran pertama tetap berkunci sama seperti sebelum fitur cicilan ada
	 * (kompatibilitas data lama).
	 *
	 * <h4>Penyesuaian periode untuk komponen ber-<code>khususBulan</code></h4>
	 * Bila komponen biaya hanya ditagih pada satu bulan tertentu, parameter
	 * <code>pembayaranTerakhir</code> yang dioper pemanggil <b>diabaikan</b> dan dihitung
	 * ulang dari tahun ajaran: bulan &ge; 7 memakai potongan pertama "2025/2026", bulan
	 * &lt; 7 memakai potongan kedua. Format tahun ajaran yang salah ditelan diam-diam.
	 *
	 * <h4>KUIRK PENTING — prefiks identitas bisa hilang</h4>
	 * Prefiks siswa/calon siswa hanya ditambahkan bila populasi COCOK dengan bendera
	 * <code>JenisBiayaSekolah.gunakanCalonSiswa</code>:
	 * <code>calonSiswa != null</code> memerlukan bendera <code>true</code>, dan
	 * <code>siswa != null</code> memerlukan bendera <code>false</code>. Pada kombinasi yang
	 * tidak cocok — misalnya seorang <code>Siswa</code> ditagih memakai jenis biaya
	 * ber-flag PSB, atau sebaliknya — <b>tidak satu pun cabang terpenuhi</b> sehingga kunci
	 * yang dihasilkan hanya berisi id paket biaya + periode + angsuran, <b>tanpa identitas
	 * siapa pun</b>. Seluruh siswa pada paket biaya dan periode yang sama akan berbagi kunci
	 * yang sama; karena indeks <code>tagihan_kode_unik_key</code> menegakkan keunikan di
	 * database, siswa kedua dan seterusnya tidak akan memperoleh baris tagihannya sendiri,
	 * dan cache memori dapat menyerahkan tagihan siswa lain. {@link #cocokDenganKunciTagihan}
	 * adalah penjaga yang ditambahkan justru untuk memburu gejala kasus semacam ini.
	 *
	 * @param itemBiayaSekolah   komponen biaya; {@code null} menjadi <code>"-1"</code>
	 * @param pengaturanBiaya    paket biaya; {@code null} menjadi <code>"-1"</code>
	 * @param pembayaranTerakhir periode YYYYMM; diabaikan bila komponen ber-<code>khususBulan</code>
	 * @param siswa              siswa yang ditagih, atau {@code null}
	 * @param calonSiswa         calon siswa yang ditagih, atau {@code null}
	 * @param bayarKe            nomor angsuran; nilai {@code null} atau &lt; 2 tidak menambah sufiks
	 * @return kunci identitas tagihan (tidak pernah {@code null})
	 */
	public static String genCode(ItemBiayaSekolah itemBiayaSekolah, PengaturanBiaya pengaturanBiaya,
			Integer pembayaranTerakhir, Siswa siswa, CalonSiswa calonSiswa, Integer bayarKe) {

		Integer thn = null;
		Integer bln = null;
		if (itemBiayaSekolah != null && itemBiayaSekolah.getKhususBulan() != null && pengaturanBiaya != null
				&& pengaturanBiaya.getTahunAjaran() != null) {
			try {
				String[] taSplit = pengaturanBiaya.getTahunAjaran().split("/");
				if (taSplit.length > 1) {
					if (itemBiayaSekolah.getKhususBulan() >= 7)
						thn = Integer.parseInt(taSplit[0].trim());
					else
						thn = Integer.parseInt(taSplit[1].trim());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:909");
				/* Abaikan */ }
			bln = itemBiayaSekolah.getKhususBulan();
		}

		if (thn != null && bln != null)
			pembayaranTerakhir = PembayaranSiswa.convert(thn, bln);

		String prefix = "";
		if (calonSiswa != null && pengaturanBiaya != null
				&& Boolean.TRUE.equals(pengaturanBiaya.getJenisBiayaSekolah().getGunakanCalonSiswa())) {
			prefix = "c-" + calonSiswa.getId() + "-";
		} else if (siswa != null && pengaturanBiaya != null
				&& Boolean.FALSE.equals(pengaturanBiaya.getJenisBiayaSekolah().getGunakanCalonSiswa())) {
			prefix = "s-" + siswa.getId() + "-";
		}

		String idItem = (itemBiayaSekolah == null ? "-1" : itemBiayaSekolah.getId().toString());
		String idPb = (pengaturanBiaya == null ? "-1" : pengaturanBiaya.getId().toString());
		String pt = (pembayaranTerakhir == null ? "" : pembayaranTerakhir.toString());
		String bk = (bayarKe == null || bayarKe < 2 ? "" : "-" + bayarKe);

		if (!prefix.isEmpty()) {
			return prefix + idItem + "-" + idPb + "-" + pt + bk;
		} else {
			return idPb + "-" + pt + bk;
		}
	}

	/**
	 * Kunci identitas logis tagihan ini.
	 *
	 * <p><b>GETTER MENULIS BALIK (memoisasi).</b> Bila kolom masih kosong, nilainya dihitung
	 * dari isi entity lewat {@link #genCode} lalu <i>disimpan ke field</i> — sehingga
	 * membaca sebuah tagihan lama yang belum berkode akan mengisi kolom
	 * <code>kode_unik</code>-nya pada flush berikutnya. Bila kolom sudah terisi, nilai
	 * tersimpan itulah yang dikembalikan tanpa dihitung ulang, sehingga kunci historis
	 * tetap awet meski isi entity berubah.</p>
	 *
	 * <p><b>Kuirk mapping:</b> dideklarasikan <code>unique = false</code>, padahal database
	 * memiliki indeks unik <code>tagihan_kode_unik_key</code> — lihat
	 * {@link #isDuplicateKodeUnikException(Throwable)}. Ketidakcocokan ini berarti Hibernate
	 * tidak akan pernah memperingatkan tabrakan lebih awal; pelanggaran baru muncul sebagai
	 * exception saat INSERT.</p>
	 *
	 * <p>Karena nilai tersimpan bisa "membatu" sementara isi entity bergeser (mis. setelah
	 * penomoran angsuran dirapikan), pemakai kunci ini wajib memverifikasi ulang lewat
	 * {@link #cocokDenganKunciTagihan}.</p>
	 *
	 * @return kunci identitas tagihan (tidak pernah {@code null} setelah dipanggil sekali)
	 */
	@Column(name = "kode_unik", unique = false)
	public String getKodeUnik() {
		if (kodeUnik != null && kodeUnik.trim().length() > 0) {
			return this.kodeUnik;
		}
		kodeUnik = genCode(getItemBiayaSekolah(), getPengaturanBiaya(), getTahunbulan(), getSiswa(), getCalonSiswa(),
				bayarKe);
		return this.kodeUnik;
	}

	/**
	 * Menyetel kunci identitas secara eksplisit. Dipakai jalur pembuatan tagihan yang sudah
	 * menghitung kunci lebih dahulu ({@link #ambilAtauBuat}) agar {@link #getKodeUnik()}
	 * tidak menghitung ulang dan menghasilkan nilai yang berbeda.
	 *
	 * @param kodeUnik kunci identitas tagihan
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Nomor urut angsuran. Nilai 1 berarti angsuran pertama, atau pembayaran sekali lunas.
	 *
	 * <p><b>GETTER DESTRUKTIF.</b> Dua normalisasi ditulis balik ke field:</p>
	 * <ol>
	 *   <li>bila periode jenis biayanya <b>"Bulanan"</b>, nomor angsuran <b>dipaksa menjadi
	 *       1</b>. Alasannya masuk akal (tagihan bulanan tidak dicicil — satu baris per
	 *       bulan), tetapi akibatnya permanen: bila periode sebuah jenis biaya diubah menjadi
	 *       "Bulanan" pada instalasi berjalan, seluruh penomoran angsuran yang sudah ada rata
	 *       menjadi 1 begitu barisnya tersentuh, dan karena <code>bayarKe</code> ikut
	 *       membentuk {@link #genCode}, kunci identitasnya pun bergeser;</li>
	 *   <li>nilai &gt; 500 dipangkas menjadi 500 (pengaman terhadap data rusak).</li>
	 * </ol>
	 *
	 * <p>Nilai {@code null} dikembalikan sebagai 1 — <b>tanpa</b> menulis ke field, sehingga
	 * kolom tetap NULL sementara pembacaan mengembalikan 1. Perbedaan halus ini penting bagi
	 * kode yang membandingkan langsung ke field <code>bayarKe</code> (lihat
	 * {@link #getAktif()} yang membaca field mentah, bukan getter ini).</p>
	 *
	 * <p>Seluruh <i>exception</i> saat menelusuri jenis biaya ditelan dan dicatat.</p>
	 *
	 * @return nomor urut angsuran, minimal 1, maksimal 500
	 */
	public Integer getBayarKe() {
		try {
			nominalBiaya = getNominalBiaya();
			if (nominalBiaya != null && nominalBiaya.getPengaturanBiaya() != null
					&& nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah() != null) {
				if ("Bulanan".equalsIgnoreCase(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode())) {
					bayarKe = 1;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:961");
			// TODO: handle exception
		}
		if (bayarKe != null && bayarKe > 500)
			bayarKe = 500;
		return bayarKe == null ? 1 : bayarKe;
	}

	/**
	 * Menyetel nomor urut angsuran.
	 * <p>
	 * Dipakai antara lain oleh <code>/Api TagihanSiswa.hapus_split()</code> untuk menomori
	 * ulang seluruh angsuran sebuah <code>NominalBiaya</code> setelah salah satu cicilan
	 * dihapus. Karena nilai ini ikut membentuk {@link #genCode}, penomoran ulang membuat
	 * <code>kode_unik</code> yang tersimpan menjadi historis — inilah persis kasus yang
	 * dijaga {@link #cocokDenganKunciTagihan}.
	 *
	 * @param bayarKe nomor urut angsuran
	 */
	public void setBayarKe(Integer bayarKe) {
		this.bayarKe = bayarKe;
	}

	/**
	 * Varian ringkas perhitungan nominal — <b>bukan</b> pembaca murni, tetapi jauh lebih
	 * sedikit cabangnya daripada {@link #getNominal()} dan <b>tidak</b> menyentuh
	 * {@link #getPembayaranSiswaDetail()} (sehingga tidak ikut memicu pemutusan relasi
	 * pelunasan yang dijelaskan di getter tersebut).
	 *
	 * <p>Aturan yang diterapkan, berurutan:</p>
	 * <ol>
	 *   <li>untuk pembayaran sekali lunas, nominal dinaikkan ke <code>minimalBiaya</code>
	 *       bila berada di bawahnya;</li>
	 *   <li>bila baris ditandai "bukan tagihan" (dari tarif, dari kolom sendiri, atau dari
	 *       daftar bulan tanpa tagihan) &rarr; <b>0</b>;</li>
	 *   <li>untuk komponen dicicil non-"Bulanan" dengan angsuran manual terisi &rarr; pakai
	 *       {@link #getDibayarManual()};</li>
	 *   <li>bila nominal manual terisi dan angsuran manual tidak &rarr; pakai
	 *       {@link #getNominalManual()};</li>
	 *   <li>pada paket biaya "khusus buat siswa tertentu" dengan angsuran manual terisi
	 *       &rarr; pakai {@link #getDibayarManual()} (override terakhir).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> hasilnya tetap ditulis ke field {@link #nominal}. Perhatikan
	 * pula bahwa method ini membaca field <code>nominalBiaya</code> MENTAH (tanpa
	 * {@link #getNominalBiaya()}), sehingga proxy yang belum terinisialisasi dapat membuatnya
	 * mengembalikan 0 alih-alih menelusur ke tarif.</p>
	 *
	 * <p>Dipakai antara lain oleh layar/laporan yang menghitung total kewajiban tanpa perlu
	 * menyentuh data pembayaran.</p>
	 *
	 * @return nominal kewajiban rupiah, minimal 0,0 (tidak pernah {@code null})
	 */
	public Double ambilNominal() {
		if (nominalBiaya == null)
			return 0.0;
		PengaturanBiayaItemBiaya pbi = nominalBiaya.getPengaturanBiayaItemBiaya();

		if (nominalBiaya.getDibayarSebayak() != null && nominalBiaya.getDibayarSebayak() == 1) {
			if (nominal != null && pbi != null && pbi.getMinimalBiaya() != null && pbi.getMinimalBiaya() > nominal) {
				nominal = pbi.getMinimalBiaya();
			}
		}

		if (Boolean.TRUE.equals(nominalBiaya.getBukanTagihan()) || ambilBukanTagihanData() || ambilBukanTagihan()) {
			nominal = 0.0;
		} else if (nominalBiaya.getDibayarSebayak() != null && nominalBiaya.getDibayarSebayak() > 1
				&& getDibayarManual() > 0.01 && nominalBiaya.getPengaturanBiaya() != null
				&& nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah() != null
				&& !"Bulanan".equalsIgnoreCase(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode())) {
			nominal = getDibayarManual();
		} else if (getNominalManual() != null && getNominalManual() > 0 && getDibayarManual() < 0.01) {
			nominal = getNominalManual();
		}

		if (getDibayarManual() > 0.01 && nominalBiaya.getPengaturanBiaya() != null
				&& Boolean.TRUE.equals(nominalBiaya.getPengaturanBiaya().getKhususBuatSiswaTertentu())) {
			nominal = getDibayarManual();
		}

		return nominal == null ? 0.0 : nominal;
	}

	/**
	 * <b>Angka rupiah yang benar-benar ditagihkan</b> untuk angsuran ini. Ini adalah method
	 * terpenting sekaligus paling rumit di kelas ini: nilainya dipakai untuk mencetak
	 * kuitansi, menghitung total Virtual Account yang dikirim ke bank, dan menentukan sisa
	 * tunggakan.
	 *
	 * <p><b>GETTER MENULIS BALIK.</b> Hasil akhirnya selalu disimpan ke field
	 * {@link #nominal}, jadi setiap pembacaan berpotensi mengubah kolom
	 * <code>nominal</code> pada flush berikutnya. Ia juga memanggil
	 * {@link #getPembayaranSiswaDetail()} di baris pertama, sehingga ikut mewarisi efek
	 * destruktif getter tersebut (pemutusan relasi pelunasan bernominal nol).</p>
	 *
	 * <h4>Struktur keputusan</h4>
	 * Empat cabang utama, dipilih yang cocok pertama:
	 * <ol>
	 *   <li><b>Tarif terkunci.</b> Sekali lunas dengan <code>maksimalBiaya == minimalBiaya</code>
	 *       (&gt; 0,1) &rarr; ambil langsung <code>NominalBiaya.getNominal()</code>. Inilah
	 *       kondisi yang disebut komentar {@link #setNominal(Double)} sebagai pemicu bug
	 *       "tagihan bertambah tiap klik Cari".</li>
	 *   <li><b>Sudah ada pelunasan.</b> &rarr; nominal mengikuti
	 *       <code>PembayaranSiswaDetail.ambilNominal()</code>, yaitu jumlah yang benar-benar
	 *       dibayarkan.</li>
	 *   <li><b>Non-"Bulanan" sekali lunas.</b> &rarr; ambil langsung dari tarif.</li>
	 *   <li><b>Sisanya</b> — blok panjang yang memproses, berurutan: nilai awal bila field
	 *       masih kosong (0 untuk komponen yang boleh diangsur berapa pun, selebihnya tarif
	 *       penuh); penyesuaian non-"Bulanan"; pemulihan angsuran pertama yang bernilai 0;
	 *       pemulihan komponen ber-<code>angsuranSeragam</code>; batas bawah
	 *       <code>minimalBiaya</code>; override angsuran manual; override "khusus buat siswa
	 *       tertentu"; penanda "bukan tagihan" &rarr; 0; override nominal manual; dan
	 *       terakhir <code>PembayaranSiswaDetail.getNominalManual()</code>.</li>
	 * </ol>
	 * Setelah keempat cabang, dua penyesuaian berlaku untuk semua jalur:
	 * <ul>
	 *   <li>bila {@link #getKunci()} terisi (atau paket biayanya terkunci) dan
	 *       {@link #getBiayaTemporary()} ada, nominal <b>diganti</b> nilai sementara itu —
	 *       jalur simulasi/negosiasi biaya;</li>
	 *   <li>nominal 0 untuk pembayaran sekali lunas dijatuhkan ke
	 *       <code>PengaturanBiayaItemBiaya.defaultBiaya</code>.</li>
	 * </ul>
	 *
	 * <h4>Ketahanan terhadap sesi tertutup</h4>
	 * Pengambilan <code>PengaturanBiayaItemBiaya</code>/<code>PengaturanBiaya</code>/
	 * <code>JenisBiayaSekolah</code> dibungkus <code>try/catch</code>: bila proxy-nya terikat
	 * ke sesi yang sudah ditutup, ketiganya dibiarkan {@code null} dan cabang-cabang yang
	 * bergantung padanya dilewati. Perhatikan konsekuensinya — <b>kegagalan itu senyap dan
	 * dapat menghasilkan nominal yang berbeda</b> dari perhitungan normal, bukan error.
	 *
	 * <h4>Kuirk</h4>
	 * <ul>
	 *   <li>Cabang <code>if (dibayarSatuKali) ... else ...</code> di dalam blok "nilai awal"
	 *       menghasilkan nilai yang IDENTIK pada kedua sisi
	 *       (<code>nominalBiaya.getNominal()</code>) — sisa penyederhanaan yang belum
	 *       dirapikan.</li>
	 *   <li>Field <code>nominalBiaya</code> dibaca mentah di baris pertama; bila
	 *       {@code null}, method langsung mengembalikan 0,0 tanpa menelusur apa pun.</li>
	 *   <li>Anotasi kolom menyatakan <code>precision = 17, scale = 17</code> — skala sama
	 *       dengan presisi, yang secara harfiah berarti nol digit sebelum koma. Nilai ini
	 *       warisan generator hbm2java dan tidak mencerminkan skema sebenarnya.</li>
	 * </ul>
	 *
	 * <p><b>Terjangkau dari luar:</b> nilai kembalian method ini dijumlahkan langsung oleh
	 * <code>/Api TagihanSiswa.va()</code> (yang query-nya rentan IDOR + SQL injection) dan
	 * dibacakan ke pemanggil anonim oleh servlet <code>/MncBank</code> serta <code>/Va</code>.
	 * Lihat Javadoc kelas.</p>
	 *
	 * @return nominal kewajiban rupiah, minimal 0,0 (tidak pernah {@code null})
	 */
	@Column(name = "nominal", nullable = false, precision = 17, scale = 17)
	public Double getNominal() {
		if (nominalBiaya == null)
			return 0.0;
		pembayaranSiswaDetail = getPembayaranSiswaDetail();
		PengaturanBiayaItemBiaya pbi = null;
		PengaturanBiaya pb = null;
		JenisBiayaSekolah jbs = null;
		try {
			// FIX LazyInitializationException: nominalBiaya.getPengaturanBiayaItemBiaya()/getPengaturanBiaya()
			// bisa berupa instance canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke
			// Session lain yang sudah closed -> jangan biarkan getter ini crash, cukup lewati bagian ini
			// (nilai fallback null dipertahankan, ditangani oleh pengecekan null di bawah).
			pbi = nominalBiaya.getPengaturanBiayaItemBiaya();
			pb = nominalBiaya.getPengaturanBiaya();
			jbs = pb != null ? pb.getJenisBiayaSekolah() : null;
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:getNominal-lazy");
		}

		boolean dibayarSatuKali = nominalBiaya.getDibayarSebayak() != null && nominalBiaya.getDibayarSebayak() == 1;

		if (pbi != null && dibayarSatuKali && pbi.getMaksimalBiaya() != null && pbi.getMinimalBiaya() != null
				&& pbi.getMaksimalBiaya() > 0.1 && pbi.getMaksimalBiaya().equals(pbi.getMinimalBiaya())) {
			nominal = nominalBiaya.getNominal();
		} else if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null) {
			nominal = pembayaranSiswaDetail.ambilNominal();
		} else if (jbs != null && !"Bulanan".equalsIgnoreCase(jbs.getPeriode()) && dibayarSatuKali) {
			nominal = nominalBiaya.getNominal();
		} else {
			if (nominal == null) {
				if (nominalBiaya.getItemBiayaSekolah() != null
						&& Boolean.TRUE.equals(nominalBiaya.getItemBiayaSekolah().getBolehDiangsur()) && jbs != null
						&& Boolean.TRUE.equals(jbs.getBolehAngsurBerapapun())) {
					nominal = 0.0;
				} else if (dibayarSatuKali) {
					nominal = nominalBiaya.getNominal();
				} else {
					nominal = nominalBiaya.getNominal();
				}
			}

			if (jbs != null && !"Bulanan".equalsIgnoreCase(jbs.getPeriode())) {
				if (dibayarSatuKali)
					nominal = nominalBiaya.getNominal();
				if (nominalBiaya.getNominal() != null && nominalBiaya.getNominal() < nominal)
					nominal = 0.0;
			}

			if (getBayarKe() == 1 && nominal != null && nominal == 0)
				nominal = nominalBiaya.getNominal();
			if ((nominal == null || nominal == 0) && getItemBiayaSekolah() != null
					&& Boolean.TRUE.equals(getItemBiayaSekolah().getAngsuranSeragam())) {
				nominal = nominalBiaya.getNominal();
			}

			if (dibayarSatuKali && pbi != null && nominal != null && pbi.getMinimalBiaya() != null
					&& pbi.getMinimalBiaya() > nominal) {
				nominal = pbi.getMinimalBiaya();
			}

			if (!dibayarSatuKali && getDibayarManual() > 0.01 && jbs != null
					&& !"Bulanan".equalsIgnoreCase(jbs.getPeriode())) {
				nominal = getDibayarManual();
			}

			if (getDibayarManual() > 0.01 && pb != null && Boolean.TRUE.equals(pb.getKhususBuatSiswaTertentu())) {
				nominal = getDibayarManual();
			}

			if (Boolean.TRUE.equals(nominalBiaya.getBukanTagihan()) || ambilBukanTagihanData() || ambilBukanTagihan()) {
				nominal = 0.0;
			} else if (getNominalManual() != null && getNominalManual() > 0 && getDibayarManual() < 0.01) {
				nominal = getNominalManual();
			} else if (getNominalManual() != null && getNominalManual() > 0 && getDibayarManual() > 0.01) {
				nominal = getDibayarManual();
			}

			if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null
					&& pembayaranSiswaDetail.getNominalManual() != null
					&& pembayaranSiswaDetail.getNominalManual() > 0.01) {
				nominal = pembayaranSiswaDetail.getNominalManual();
			}
		}

		Tbmuser k = getKunci();
		if (k != null && biayaTemporary != null)
			nominal = biayaTemporary;

		if (k == null && pb != null) {
			Tbmuser k1 = pb.getKunci();
			if (k1 != null && biayaTemporary != null)
				nominal = biayaTemporary;
		}

		if (nominal != null && nominal == 0 && dibayarSatuKali && pbi != null && pbi.getDefaultBiaya() != null
				&& pbi.getDefaultBiaya() > 0.1) {
			nominal = pbi.getDefaultBiaya();
		}

		return nominal == null ? 0.0 : nominal;
	}

	/**
	 * Menyimpan nominal kewajiban rupiah, dengan normalisasi {@code null} &rarr; 0,0.
	 *
	 * <h4>Setter ini adalah lokasi BUG PRODUKSI NYATA yang sudah diperbaiki</h4>
	 * Komentar di dalam badan method (dipertahankan apa adanya, <b>jangan dihapus</b>)
	 * mendokumentasikan bug <i>"tagihan bertambah terus tiap klik Cari"</i>. Ringkasan
	 * mekanismenya, diverifikasi ulang terhadap kode yang ada sekarang:
	 * <ol>
	 *   <li>Versi lama setter ini ikut menulis ke entity LAIN:
	 *       <code>nominalBiaya.setNominal(nominal + getDiskon())</code>.</li>
	 *   <li>Namun {@link #getDiskon()} pada kondisi "sudah dibayar,
	 *       <code>dibayarSebayak == 1</code>, <code>minimalBiaya == maksimalBiaya</code>"
	 *       justru DIHITUNG DARI <code>nominalBiaya.getNominal()</code> itu sendiri —
	 *       cabang <code>diskon = nominalBiaya.getNominal() - currentDibayar</code> yang
	 *       masih dapat dibaca di method tersebut hari ini.</li>
	 *   <li>Karena entity ini <b>property access</b>, Hibernate memanggil setter ini setiap
	 *       kali baris <code>Tagihan</code> dimuat atau di-refresh — bukan hanya ketika
	 *       pengguna benar-benar mengubah nilai.</li>
	 *   <li>Hasilnya sebuah <b>umpan balik</b>, bukan operasi idempoten: baca
	 *       <code>nominalBiaya</code> (yang mungkin sudah merupakan hasil tulisan
	 *       sebelumnya) &rarr; hitung diskon darinya &rarr; tulis balik nilai yang lebih
	 *       besar &rarr; tersimpan &rarr; pencarian BERIKUTNYA membaca angka yang sudah
	 *       membesar itu sebagai titik awal.</li>
	 * </ol>
	 * Gejala yang terlihat pengguna: tagihan seorang siswa membengkak sedikit demi sedikit
	 * setiap kali layar dibuka atau tombol <b>"Cari"</b> diklik. Kasus nyata yang tercatat
	 * adalah komponen "Biaya Pendaftaran SD" pada layar <b>Pembayaran Calon Siswa</b>.
	 * <p>
	 * <b>Perbaikannya</b> adalah membuat setter ini murni: hanya menyimpan nilai milik
	 * <code>Tagihan</code> sendiri, tanpa efek samping ke entity lain. Normalisasi
	 * {@code null} &rarr; 0,0 dipertahankan karena kolom <code>nominal</code> NOT NULL.
	 * <p>
	 * <b>Pelajaran yang berlaku untuk seluruh kelas ini:</b> pada entity property-access,
	 * setter maupun getter yang menulis ke entity lain akan dieksekusi pada setiap
	 * pemuatan, dan bila nilai yang ditulis ikut menjadi masukan perhitungan berikutnya,
	 * hasilnya adalah kerusakan data yang menumpuk secara diam-diam.
	 * <p>
	 * <b>Catatan keamanan:</b> setter ini dipanggil langsung dari
	 * <code>/Api TagihanSiswa.hapus_split()</code>
	 * (<code>tagihanTerakhir.setNominal(tagihanTerakhir.getNominal() + tag)</code>) atas
	 * tagihan yang dipilih hanya berdasarkan id dari klien tanpa cek kepemilikan — sehingga
	 * jalur tulis ke kolom ini terbuka bagi token siswa mana pun atas tagihan siswa mana
	 * pun. Lihat Javadoc kelas.
	 *
	 * @param nominal nominal kewajiban rupiah; {@code null} disimpan sebagai 0,0
	 */
	public void setNominal(Double nominal) {
		// PERBAIKAN (tagihan bertambah terus tiap klik "Cari"): setter ini SEBELUMNYA juga
		// menulis nominalBiaya.setNominal(nominal + getDiskon()) -- padahal getDiskon() pada
		// kondisi "sudah dibayar, dibayarSebayak==1, min==maks" (persis kasus Biaya Pendaftaran
		// SD di layar Pembayaran Calon Siswa) DIHITUNG DARI nominalBiaya.getNominal() itu
		// SENDIRI (lihat getDiskon(): diskon = nominalBiaya.getNominal() - currentDibayar).
		// Setter POJO seperti ini dipanggil Hibernate SENDIRI setiap kali entitas Tagihan
		// di-load/di-refresh (property access) -- BUKAN cuma saat user benar-benar mengubah
		// nilai. Akibatnya setiap kali layar dibuka/tombol "Cari" diklik: baca nominalBiaya
		// (sudah mungkin hasil tulisan sebelumnya) -> hitung diskon darinya -> tulis balik
		// nilai baru ke nominalBiaya -> tersimpan -> pencarian BERIKUTNYA membaca nilai yang
		// SUDAH membesar itu sebagai titik awal, dan seterusnya (feedback loop, bukan idempoten).
		// FIX: setter hanya menyimpan nilai Tagihan sendiri (dengan normalisasi null->0 yang
		// sudah ada sebelumnya); TIDAK LAGI menulis balik ke entitas lain sebagai efek samping.
		if (nominal == null) {
			nominal = 0.0;
		}
		this.nominal = nominal;
	}

	/**
	 * Penanda AKTIFKAN MANUAL (force-ON) oleh admin. Bila {@code true},
	 * {@link #getAktif()} mengembalikan {@code true} dengan <b>melewati SELURUH validasi</b>
	 * — tahun angkatan, penjurusan, status awal, gelombang PSB, jenis kelamin, kedaluwarsa,
	 * tahun lulus, dan batas <code>bulanSampai</code> semuanya diabaikan.
	 * <p>
	 * Dipakai untuk memaksa munculnya tagihan pada kasus yang tidak tertangkap konfigurasi
	 * normal, dan merupakan satu-satunya cara mengaktifkan tagihan berperiode "Harian"
	 * (lihat {@link #getBoleh}).
	 * <p>
	 * Perhatikan urutannya di {@link #getAktif()}: penanda "bukan tagihan" diperiksa
	 * <b>SEBELUM</b> override ini, sehingga baris yang ditandai bukan tagihan tetap mati
	 * meskipun diaktifkan manual.
	 *
	 * @return {@code true} bila admin memaksa tagihan aktif; {@code null} berarti tidak diatur
	 */
	public Boolean getAktifkanmanual() {
		return aktifkanmanual;
	}

	/**
	 * @param aktifkanmanual {@code true} untuk memaksa tagihan aktif melewati semua validasi
	 */
	public void setAktifkanmanual(Boolean aktifkanmanual) {
		this.aktifkanmanual = aktifkanmanual;
	}

	/**
	 * Penanda NONAKTIFKAN MANUAL (force-OFF) oleh admin. Bila {@code true}, {@link #getAktif()}
	 * mengembalikan {@code false} tanpa dihitung ulang — dipakai untuk mematikan tagihan hasil
	 * revisi yang keliru agar tidak muncul lagi di daftar tagihan maupun laporan tunggakan.
	 * Tidak diaudit (envers) karena hanya flag kontrol tampilan/keaktifan.
	 * <p>
	 * Perhatikan bahwa penanda ini hanya berlaku untuk tagihan yang <b>belum dibayar</b>:
	 * di {@link #getAktif()} pemeriksaannya diletakkan SETELAH blok <i>rescue</i> yang
	 * memaksa {@code true} untuk tagihan yang sudah punya kuitansi. Konsekuensi
	 * pilihan <code>&#64;NotAudited</code>: penonaktifan manual sebuah tagihan
	 * <b>tidak muncul di riwayat Envers</b>, sehingga tidak ada jejak siapa yang mematikan
	 * tagihan mana dan kapan.
	 *
	 * @return {@code true} bila admin memaksa tagihan nonaktif; {@code null} berarti tidak diatur
	 */
	@org.hibernate.envers.NotAudited
	@javax.persistence.Column(name = "nonaktif_manual")
	public Boolean getNonaktifManual() {
		return nonaktifManual;
	}

	/**
	 * @param nonaktifManual {@code true} untuk memaksa tagihan nonaktif tanpa dihitung ulang
	 */
	public void setNonaktifManual(Boolean nonaktifManual) {
		this.nonaktifManual = nonaktifManual;
	}

	/**
	 * Varian ringkas {@link #getBoleh(PengaturanBiaya, Siswa, CalonSiswa, ItemBiayaSekolah,
	 * Boolean, boolean)} tanpa komponen biaya spesifik dan tanpa penelusuran debug.
	 *
	 * @param pengaturanBiaya paket biaya yang diuji; {@code null} selalu menghasilkan {@code true}
	 * @param siswa           siswa yang diuji, atau {@code null}
	 * @param calonSiswa      calon siswa yang diuji, atau {@code null}
	 * @return {@code true} bila paket biaya boleh dikenakan kepada pihak tersebut
	 */
	public static Boolean getBoleh(PengaturanBiaya pengaturanBiaya, Siswa siswa, CalonSiswa calonSiswa) {
		return getBoleh(pengaturanBiaya, siswa, calonSiswa, null, false);
	}

	/**
	 * Varian dengan komponen biaya spesifik, tanpa penelusuran debug.
	 *
	 * @param pengaturanBiaya  paket biaya yang diuji
	 * @param siswa            siswa yang diuji, atau {@code null}
	 * @param calonSiswa       calon siswa yang diuji, atau {@code null}
	 * @param itemBiayaSekolah komponen biaya yang harus terdaftar di paket tersebut
	 * @return {@code true} bila paket biaya boleh dikenakan kepada pihak tersebut
	 */
	public static Boolean getBoleh(PengaturanBiaya pengaturanBiaya, Siswa siswa, CalonSiswa calonSiswa,
			ItemBiayaSekolah itemBiayaSekolah) {
		return getBoleh(pengaturanBiaya, siswa, calonSiswa, itemBiayaSekolah, false);
	}

	// 1. Method Overloading: Menjaga agar class lain yang memanggil method ini tidak error (Backward Compatibility)
	/**
	 * Varian kompatibilitas mundur (tanpa parameter <code>debug</code>) — dipertahankan agar
	 * kelas lain yang memanggilnya tetap terkompilasi.
	 *
	 * @param pengaturanBiaya  paket biaya yang diuji
	 * @param siswa            siswa yang diuji, atau {@code null}
	 * @param calonSiswa       calon siswa yang diuji, atau {@code null}
	 * @param itemBiayaSekolah komponen biaya yang harus terdaftar di paket tersebut
	 * @param aktifkanmanual   penanda aktifkan manual; satu-satunya penentu untuk periode "Harian"
	 * @return {@code true} bila paket biaya boleh dikenakan kepada pihak tersebut
	 */
		public static Boolean getBoleh(PengaturanBiaya pengaturanBiaya, Siswa siswa, CalonSiswa calonSiswa,
				ItemBiayaSekolah itemBiayaSekolah, Boolean aktifkanmanual) {
			return getBoleh(pengaturanBiaya, siswa, calonSiswa, itemBiayaSekolah, aktifkanmanual, false);
		}

		// 2. Method Utama: Dengan tambahan parameter "boolean debug" dan Perbaikan Bug Gelombang
	/**
	 * Menentukan apakah sebuah paket biaya <b>boleh dikenakan</b> kepada seorang siswa atau
	 * calon siswa. Inilah penyaring kelayakan yang dipakai {@link #getAktif()}, dan lewat
	 * itu menentukan tagihan mana yang muncul di layar pembayaran serta laporan tunggakan.
	 *
	 * <h4>Urutan pemeriksaan (gagal pada salah satu &rarr; {@code false})</h4>
	 * <ol>
	 *   <li><b>Paket biaya {@code null} &rarr; {@code true}.</b> Ini adalah <b>bypass
	 *       fail-open</b>: tanpa konteks paket biaya, seluruh validasi dilewati dan tagihan
	 *       dianggap layak. Baris debug di dalam kode menyebutnya secara terbuka
	 *       ("bypass validasi (return true)").</li>
	 *   <li><b>Tahun angkatan.</b> Harus sama dengan tahun masuk siswa/calon siswa;
	 *       <code>tahunAngkatan == 0</code> berarti "berlaku untuk semua angkatan".</li>
	 *   <li><b>Keanggotaan komponen.</b> Bila <code>itemBiayaSekolah</code> diberikan, ia
	 *       harus terdaftar di paket tersebut.</li>
	 *   <li><b>Penjurusan &amp; status awal</b> — <b>dilewati sepenuhnya</b> bila paket
	 *       ber-flag <code>khususBuatSiswaTertentu</code>. Untuk penjurusan ada aturan
	 *       tambahan: bila sekolah mewajibkan pemilihan jurusan
	 *       (<code>penjurusanWajibDipilih</code>) sementara siswa belum memilih, hasilnya
	 *       {@code false}.</li>
	 *   <li><b>Gelombang PSB</b> — hanya diproses bila jenis biayanya ber-flag
	 *       <code>gunakanCalonSiswa</code>. Jenis biaya harus tercatat pada salah satu dari
	 *       tiga slot gelombang calon siswa (<code>jenisBiayaSekolah</code>,
	 *       <code>...Lulus</code>, atau <code>...Terverifikasi</code>), dan bila paket biaya
	 *       menyebut gelombang tertentu, gelombangnya harus sama.</li>
	 *   <li><b>Periode "Harian"</b> &rarr; hasilnya semata-mata
	 *       <code>aktifkanmanual</code>; {@code null} diperlakukan sebagai {@code false}.
	 *       Artinya tagihan harian TIDAK PERNAH aktif kecuali dinyalakan manual.</li>
	 * </ol>
	 *
	 * <h4>Titik-titik yang gagal ke sisi LONGGAR</h4>
	 * Beberapa pemeriksaan hanya berjalan bila data pembandingnya ada. Bila siswa belum
	 * punya penjurusan (dan sekolah tidak mewajibkannya), belum punya status awal, atau
	 * belum punya gelombang PSB, pemeriksaan yang bersangkutan <b>dilewati</b> dan tagihan
	 * dianggap layak. Data induk yang tidak lengkap karena itu cenderung menghasilkan
	 * tagihan yang terlalu banyak, bukan terlalu sedikit.
	 *
	 * <p>Mode <code>debug</code> mencetak alasan setiap kegagalan ke <code>System.out</code>
	 * dengan awalan <code>[DEBUG-BIAYA]</code> — berguna untuk menelusuri "kenapa tagihan
	 * ini tidak muncul", tetapi keluarannya masuk ke log server, jadi jangan diaktifkan
	 * secara permanen pada instalasi produksi.</p>
	 *
	 * @param pengaturanBiaya  paket biaya yang diuji; {@code null} menghasilkan {@code true}
	 * @param siswa            siswa yang diuji, atau {@code null}
	 * @param calonSiswa       calon siswa yang diuji, atau {@code null}
	 * @param itemBiayaSekolah komponen biaya yang harus terdaftar di paket, atau {@code null}
	 * @param aktifkanmanual   penanda aktifkan manual; penentu tunggal untuk periode "Harian"
	 * @param debug            {@code true} untuk mencetak alasan tiap keputusan ke stdout
	 * @return {@code true} bila paket biaya boleh dikenakan kepada pihak tersebut
	 */
		public static Boolean getBoleh(PengaturanBiaya pengaturanBiaya, Siswa siswa, CalonSiswa calonSiswa,
				ItemBiayaSekolah itemBiayaSekolah, Boolean aktifkanmanual, boolean debug) {
			
			if (debug) System.out.println("\n[DEBUG-BIAYA] === Memulai validasi getBoleh ===");
			
			if (pengaturanBiaya == null) {
				if (debug) System.out.println("[DEBUG-BIAYA] OK: PengaturanBiaya bernilai null, bypass validasi (return true).");
				return true;
			}

			Integer tahunMasuk = (siswa != null) ? siswa.getTahunMasuk()
					: ((calonSiswa != null) ? calonSiswa.getTahunMasuk() : null);
					
			if (tahunMasuk != null && pengaturanBiaya.getTahunAngkatan() != null
					&& pengaturanBiaya.getTahunAngkatan() != 0) {
				if (!pengaturanBiaya.getTahunAngkatan().equals(tahunMasuk)) {
					if (debug) System.out.println("[DEBUG-BIAYA] GAGAL: Tahun angkatan biaya (" + pengaturanBiaya.getTahunAngkatan() + ") != Tahun masuk siswa (" + tahunMasuk + ").");
					return false;
				}
			}

			if (itemBiayaSekolah != null && !pengaturanBiaya.checkAdaItemBiaya(itemBiayaSekolah)) {
				if (debug) System.out.println("[DEBUG-BIAYA] GAGAL: Item biaya spesifik tidak ditemukan di dalam pengaturanBiaya.");
				return false;
			}

			if (!Boolean.TRUE.equals(pengaturanBiaya.getKhususBuatSiswaTertentu())) {
				PenjurusanSekolah penjurusanSekolah = (siswa != null) ? siswa.getPenjurusanSekolah()
						: ((calonSiswa != null) ? calonSiswa.getPenjurusanSekolah() : null);
				StatusAwalSiswa statusAwal = (siswa != null) ? siswa.getStatusAwalSiswa()
						: ((calonSiswa != null) ? calonSiswa.getStatusAwalSiswa() : null);

				if (pengaturanBiaya.getPenjurusanSekolah() != null) {
					if (penjurusanSekolah != null
							&& !pengaturanBiaya.getPenjurusanSekolah().getId().equals(penjurusanSekolah.getId())) {
						if (debug) System.out.println("[DEBUG-BIAYA] GAGAL: Penjurusan berbeda. Biaya untuk ID: " + pengaturanBiaya.getPenjurusanSekolah().getId() + ", Siswa di ID: " + penjurusanSekolah.getId());
						return false;
					}
					
					Boolean wajibDipilih = (siswa != null && siswa.getSekolah() != null)
							? siswa.getSekolah().getPenjurusanWajibDipilih()
							: ((calonSiswa != null && calonSiswa.getSekolah() != null)
									? calonSiswa.getSekolah().getPenjurusanWajibDipilih()
									: null);
									
					if (penjurusanSekolah == null && Boolean.TRUE.equals(wajibDipilih)) {
						if (debug) System.out.println("[DEBUG-BIAYA] GAGAL: Sekolah mewajibkan penjurusan, tapi Siswa/CalonSiswa belum diset penjurusannya.");
						return false;
					}
				}

				if (pengaturanBiaya.getStatusAwalSiswa() != null) {
					if (statusAwal != null && !pengaturanBiaya.getStatusAwalSiswa().getId().equals(statusAwal.getId())) {
						if (debug) System.out.println("[DEBUG-BIAYA] GAGAL: Status awal siswa berbeda. Biaya untuk Status ID: " + pengaturanBiaya.getStatusAwalSiswa().getId() + ", Siswa Status ID: " + statusAwal.getId());
						return false;
					}
				}
			}

			// BLOK PERBAIKAN: Filter Gelombang PSB (Hanya diproses jika jenis biaya menggunakan sistem CalonSiswa)
			if (pengaturanBiaya.getJenisBiayaSekolah() != null && Boolean.TRUE.equals(pengaturanBiaya.getJenisBiayaSekolah().getGunakanCalonSiswa())) {
				
				if (calonSiswa != null && calonSiswa.getGelombangPendaftaranPsb() != null) {

					Long jenisBiayaId = pengaturanBiaya.getJenisBiayaSekolah().getId();
					boolean matchJenisBiaya = false;
					GelombangPendaftaranPsb gpp = calonSiswa.getGelombangPendaftaranPsb();

					if (gpp.getJenisBiayaSekolahLulus() != null
							&& gpp.getJenisBiayaSekolahLulus().getId().equals(jenisBiayaId))
						matchJenisBiaya = true;
					else if (gpp.getJenisBiayaSekolah() != null && gpp.getJenisBiayaSekolah().getId().equals(jenisBiayaId))
						matchJenisBiaya = true;
					else if (gpp.getJenisBiayaSekolahTerverifikasi() != null
							&& gpp.getJenisBiayaSekolahTerverifikasi().getId().equals(jenisBiayaId))
						matchJenisBiaya = true;

					if (!matchJenisBiaya) {
						if (debug) System.out.println("[DEBUG-BIAYA] GAGAL: Jenis Biaya ID (" + jenisBiayaId + ") BUKAN merupakan jenis biaya yang tercatat di tabel Gelombang PSB calon siswa ini.");
						return false;
					}

					if (pengaturanBiaya.getGelombangPendaftaranPsb() != null
							&& pengaturanBiaya.getGelombangPendaftaranPsb().getId() != null && gpp.getId() != null) {
						if (!gpp.getId().equals(pengaturanBiaya.getGelombangPendaftaranPsb().getId())) {
							if (debug) System.out.println("[DEBUG-BIAYA] GAGAL: Gelombang pendaftaran dari biaya tsb tidak sama dengan gelombang asal calon siswa.");
							return false;
						}
					}
				}
			}

			if (pengaturanBiaya.getJenisBiayaSekolah() != null
					&& "Harian".equalsIgnoreCase(pengaturanBiaya.getJenisBiayaSekolah().getPeriode())) {
				boolean hasilHarian = aktifkanmanual != null ? aktifkanmanual : false;
				if (debug) System.out.println("[DEBUG-BIAYA] HASIL (Harian): Return berdasarkan aktifkanmanual (" + hasilHarian + ").");
				return hasilHarian;
			}

			if (debug) System.out.println("[DEBUG-BIAYA] OK: Lolos semua validasi (return true).");
			return true;
		}

	/**
	 * Menentukan apakah tagihan ini <b>masih berlaku</b> — yaitu apakah ia muncul di layar
	 * pembayaran, ikut dijumlahkan ke total Virtual Account, dan tampil di laporan
	 * tunggakan.
	 *
	 * <p><b>GETTER MENULIS BALIK.</b> Setiap cabang menyimpan hasilnya ke field
	 * {@link #aktif} sebelum mengembalikannya (<code>return this.aktif = ...</code>),
	 * sehingga kolom <code>aktif</code> ikut berubah pada flush berikutnya. Method ini juga
	 * memanggil sembilan getter lain yang semuanya menulis balik, termasuk
	 * {@link #getPembayaranSiswaDetail()} dengan efek destruktifnya.</p>
	 *
	 * <h4>Urutan keputusan (yang di atas mengalahkan yang di bawah)</h4>
	 * <ol>
	 *   <li><b>Sudah dibayar &rarr; SELALU aktif.</b> Bila ada baris pelunasan yang tertaut
	 *       ke sebuah kuitansi (<i>rescue</i>), tagihan dipaksa aktif tanpa validasi apa pun.
	 *       Tanpa aturan ini, mengubah konfigurasi biaya dapat membuat tagihan yang sudah
	 *       dilunasi menghilang dari riwayat pembayaran siswa.</li>
	 *   <li><b>Nonaktifkan manual</b> ({@link #getNonaktifManual()}) &rarr; mati, tidak
	 *       dihitung ulang.</li>
	 *   <li><b>Bukan tagihan</b> (dari tarif atau dari kolom sendiri) &rarr; mati. Perhatikan
	 *       urutannya: ini diperiksa <b>SEBELUM</b> override aktifkan manual, jadi penanda
	 *       "bukan tagihan" menang atas paksaan admin. Inilah alasan
	 *       {@link #setBukanTagihan(Boolean)} tidak perlu lagi punya efek samping.</li>
	 *   <li><b>Aktifkan manual</b> ({@link #getAktifkanmanual()}) &rarr; aktif, melewati
	 *       seluruh validasi di bawah.</li>
	 *   <li><b>Kelayakan paket biaya</b> lewat {@link #getBoleh} — angkatan, penjurusan,
	 *       status awal, gelombang PSB, periode "Harian".</li>
	 *   <li><b>Jenis kelamin</b>: komponen yang dibatasi
	 *       <code>ItemBiayaSekolah.kelamin</code> tidak berlaku bagi siswa berjenis kelamin
	 *       lain. Bila jenis kelamin siswa {@code null}, pembatasan ini <b>dilewati</b>
	 *       (gagal ke sisi longgar).</li>
	 * </ol>
	 *
	 * <h4>Validasi lanjutan (hanya untuk tagihan yang belum "diselamatkan")</h4>
	 * Blok <code>!isRescued</code> dilewati bila tagihan sudah dibayar, diaktifkan manual,
	 * atau merupakan angsuran pertama dari komponen sekali lunas. Selebihnya diperiksa:
	 * bulan di luar 1-12; periode melewati <code>bulanSampai</code> (dua kali, dengan ambang
	 * berbeda); angsuran ke-2+ yang nominalnya persis sama dengan tarif penuh padahal
	 * komponennya BUKAN <code>angsuranSeragam</code> (indikasi cicilan yang belum dibagi);
	 * tahun yang tidak terkandung di string tahun ajaran; tanggal
	 * <code>tagihanKadaluarsa</code> yang sudah lewat atau jatuh hari ini; tahun ajaran yang
	 * sudah melewati tahun kelulusan siswa; dan komponen biaya yang sudah dinonaktifkan.
	 * <p>
	 * Terakhir, untuk paket "semua angkatan" (<code>tahunAngkatan == 0</code>) berperiode
	 * "Bulanan", batas <code>bulanSampai</code> diperiksa sekali lagi.
	 *
	 * <h4>Kuirk &amp; catatan</h4>
	 * <ul>
	 *   <li>Komentar di dalam kode menegaskan bahwa <code>bulanMulai</code> hanya mengontrol
	 *       PEMBANGKITAN tagihan, bukan keaktifan tagihan yang sudah ada — karena itu hanya
	 *       <code>bulanSampai</code> yang diuji di sini.</li>
	 *   <li>Pemeriksaan angsuran ke-2+ membaca field mentah <code>bayarKe</code> dan
	 *       <code>nominal</code>, bukan getter-nya — sehingga perilakunya berbeda pada
	 *       entity yang belum dihidrasi penuh.</li>
	 *   <li><b>Kegagalan berujung fail-open:</b> bila terjadi <i>exception</i> di mana pun,
	 *       method mengembalikan nilai <code>aktif</code> terakhir, dan bila itu pun
	 *       {@code null}, mengembalikan {@code true}. Artinya kerusakan data induk cenderung
	 *       membuat tagihan tetap tampil (dan tetap ditagihkan), bukan hilang.</li>
	 * </ul>
	 *
	 * @return {@code true} bila tagihan masih berlaku dan harus ditagihkan
	 */
	public Boolean getAktif() {
		try {
			Siswa s = getSiswa();
			CalonSiswa cs = getCalonSiswa();
			NominalBiaya nb = getNominalBiaya();
			PengaturanBiaya pb = getPengaturanBiaya();
			ItemBiayaSekolah ibs = getItemBiayaSekolah();
			Integer tb = getTahunbulan();
			PembayaranSiswaDetail psd = getPembayaranSiswaDetail();
			boolean isRescued = (psd != null && psd.getPembayaranSiswa() != null);
			if (isRescued) {
				return this.aktif = true;
			}

			// NONAKTIFKAN MANUAL oleh admin (force-OFF): tagihan sengaja dimatikan — mis.
			// tagihan hasil REVISI yang keliru namun lupa dinonaktifkan sehingga siswa yang
			// sudah lunas tetap muncul di laporan tunggakan. Hormati keputusan admin dan
			// JANGAN dihitung ulang menjadi aktif kembali. Hanya untuk tagihan BELUM dibayar
			// (bila sudah dibayar sudah ter-"rescue" true di blok isRescued di atas).
			if (Boolean.TRUE.equals(nonaktifManual)) {
				return this.aktif = false;
			}

			if ((nb != null && Boolean.TRUE.equals(nb.getBukanTagihan())) || ambilBukanTagihanData()) {
				return this.aktif = false;
			}

			// aktifkanmanual=true = override penuh oleh admin: bypass SEMUA validasi
			if (Boolean.TRUE.equals(aktifkanmanual)) {
				return this.aktif = true;
			}

			if (!getBoleh(pb, s, cs, ibs, getAktifkanmanual())) {
				return this.aktif = false;
			}

			if (ibs != null && ibs.getKelamin() != null) {
				String jenisKelamin = (s != null) ? s.getJenisKelamin() : ((cs != null) ? cs.getJenisKelamin() : null);
				if (jenisKelamin != null && !jenisKelamin.equalsIgnoreCase(ibs.getKelamin())) {
					return this.aktif = false;
				}
			}

			boolean validStatus = true;

			if (Boolean.TRUE.equals(aktifkanmanual))
				isRescued = true;
			if (bayarKe != null && bayarKe == 1 && nb != null && nb.getDibayarSebayak() != null
					&& nb.getDibayarSebayak() == 1)
				isRescued = true;

			if (!isRescued) {
				if (bulan != null && (bulan <= 0 || bulan > 12))
					validStatus = false;

				if (pb != null && pb.getJenisBiayaSekolah() != null
						&& "Bulanan".equalsIgnoreCase(pb.getJenisBiayaSekolah().getPeriode())) {
					// bulanMulai hanya mengontrol PEMBANGKITAN tagihan (doGenerateTagihanBulanan),
					// bukan keaktifan tagihan yang sudah ada di DB.
					if (tb != null && pb.getBulanSampai() != null) {
						if (tb > pb.getBulanSampai())
							validStatus = false;
					}
				}

				if (ibs != null && Boolean.FALSE.equals(ibs.getAngsuranSeragam())) {
					if (nb != null && nb.ambilNominal() != null && nb.ambilNominal() > 0.1 && nominal != null
							&& bayarKe != null && bayarKe > 1) {
						if (Double.compare(nb.ambilNominal(), nominal) == 0)
							validStatus = false;
					}
				}

				if (getTahun() != null && getTahun() > 2000 && pb != null && pb.getTahunAjaran() != null) {
					if (!pb.getTahunAjaran().contains(getTahun().toString()))
						validStatus = false;
				}

				if (tb != null && tb > 200007 && pb != null && pb.getBulanSampai() != null
						&& pb.getBulanSampai() > 200007) {
					if (tb > pb.getBulanSampai())
						validStatus = false;
				}

				Date sekarang = WaktuUtil.getDate();
				if (pb != null && pb.getTagihanKadaluarsa() != null) {
					if (sekarang.after(pb.getTagihanKadaluarsa()) || Common.dateFormat83.get().format(sekarang)
							.equals(Common.dateFormat83.get().format(pb.getTagihanKadaluarsa()))) {
						validStatus = false;
					}
				}

				if (pb != null && pb.getTahunAjaran() != null && s != null && s.getTahunLulus() != null) {
					try {
						String[] splitTa = StringUtils.split(pb.getTahunAjaran(), "/");
						if (splitTa != null && splitTa.length > 0) {
							Integer tahunAjaran = Integer.parseInt(splitTa[0].trim());
							if (tahunAjaran >= s.getTahunLulus())
								validStatus = false;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:1365");
						/* Abaikan format string error */ }
				}

				if (ibs != null && psd == null && !ibs.getAktif()) {
					validStatus = false;
				}
			}

			if (pb != null && pb.getTahunAngkatan() != null && pb.getTahunAngkatan() == 0) {
				if (pb.getJenisBiayaSekolah() != null
						&& "Bulanan".equalsIgnoreCase(pb.getJenisBiayaSekolah().getPeriode())) {
					if (tb != null && pb.getBulanSampai() != null) {
						if (tb > pb.getBulanSampai())
							validStatus = false;
					}
				}
			}

			this.aktif = validStatus;
			return this.aktif;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:1387");
		}

		return this.aktif == null ? true : this.aktif;
	}

	/**
	 * Menyetel status keaktifan secara langsung.
	 * <p>
	 * Nilai yang disetel di sini bersifat sementara: {@link #getAktif()} akan
	 * menghitungnya ulang pada pembacaan berikutnya. Untuk mematikan tagihan secara
	 * permanen pakai {@link #setNonaktifManual(Boolean)}; untuk menyalakannya pakai
	 * {@link #setAktifkanmanual(Boolean)}.
	 *
	 * @param aktif status keaktifan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Jejak posting jurnal akuntansi untuk <b>pokok</b> tagihan ini. Terisi setelah tagihan
	 * diposting ke buku besar (layar Posting Piutang Siswa); {@code null} berarti belum
	 * pernah diposting, dan nilainya dikosongkan kembali saat posting dibatalkan.
	 * <p>
	 * Salah satu dari empat jejak posting yang terpisah — pokok, denda, diskon, dan uang
	 * muka masing-masing masuk ke jurnal sendiri sehingga dapat dibatalkan sendiri-sendiri.
	 * Pembaca murni, tanpa efek samping.
	 *
	 * @return jejak posting pokok tagihan, atau {@code null} bila belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_id")
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * @param postingHistory jejak posting pokok tagihan, atau {@code null} untuk
	 *                       menandai belum/batal diposting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	// =========================================================================================
	// OPTIMISASI: Penggunaan Helper untuk membersihkan IF-ELSE bersarang di
	// TanggalTagihan
	// =========================================================================================
	/**
	 * Memilih tanggal terbit tagihan khusus untuk sebuah bulan dari dua belas kolom
	 * <code>tanggalTagihanBulan1</code> .. <code>tanggalTagihanBulan12</code> milik
	 * {@link ais.database.model.sekolah.PengaturanBiaya}.
	 * <p>
	 * Bentuk <code>switch</code> berjenjang ini menggantikan rantai IF-ELSE bersarang;
	 * kedua belas kolom itu memang dideklarasikan terpisah di entity induk, sehingga
	 * pemetaan manual seperti ini tidak terhindarkan.
	 *
	 * @param pb  paket biaya sumber tanggal (diasumsikan tidak {@code null} oleh pemanggil)
	 * @param bln bulan 1-12; nilai di luar rentang jatuh ke
	 *            <code>PengaturanBiaya.getTanggalTagihan()</code> umum
	 * @return tanggal terbit untuk bulan tersebut, atau {@code null} bila belum diisi
	 */
	private Date getTanggalTagihanBulanHelper(PengaturanBiaya pb, int bln) {
		switch (bln) {
		case 1:
			return pb.getTanggalTagihanBulan1();
		case 2:
			return pb.getTanggalTagihanBulan2();
		case 3:
			return pb.getTanggalTagihanBulan3();
		case 4:
			return pb.getTanggalTagihanBulan4();
		case 5:
			return pb.getTanggalTagihanBulan5();
		case 6:
			return pb.getTanggalTagihanBulan6();
		case 7:
			return pb.getTanggalTagihanBulan7();
		case 8:
			return pb.getTanggalTagihanBulan8();
		case 9:
			return pb.getTanggalTagihanBulan9();
		case 10:
			return pb.getTanggalTagihanBulan10();
		case 11:
			return pb.getTanggalTagihanBulan11();
		case 12:
			return pb.getTanggalTagihanBulan12();
		default:
			return pb.getTanggalTagihan();
		}
	}

	/**
	 * Tanggal tagihan diterbitkan — yang dicetak di kuitansi dan dipakai sebagai patokan
	 * "sejak kapan kewajiban ini berlaku".
	 *
	 * <p><b>GETTER MENULIS BALIK.</b> Nilainya diturunkan ulang dari paket biaya melalui
	 * tiga jalur:</p>
	 * <ol>
	 *   <li>bila paket biaya <b>tidak</b> ber-flag
	 *       <code>tanggalTagihanMengikutiBulanBerjalan</code> dan bulan tagihan diketahui
	 *       &rarr; ambil tanggal khusus bulan itu lewat
	 *       {@link #getTanggalTagihanBulanHelper};</li>
	 *   <li>untuk periode "Bulanan" dengan bulan &amp; tahun diketahui &rarr; disusun sebagai
	 *       <b>tanggal 1 pukul 07.00</b> pada bulan yang bersangkutan (jam 7 pagi dipakai
	 *       konsisten di seluruh kelas ini agar perbandingan tanggal tidak terpengaruh zona
	 *       waktu / batas tengah malam);</li>
	 *   <li>selebihnya &rarr; <code>PengaturanBiaya.getTanggalTagihan()</code> umum, tetapi
	 *       hanya bila paket ber-flag <code>tanggalTagihanMengikutiDefault</code> atau nilai
	 *       tersimpan masih kosong — sehingga tanggal yang sudah pernah ditetapkan tidak
	 *       tertimpa.</li>
	 * </ol>
	 *
	 * <p>Akses ke paket biaya dibungkus <code>try/catch</code> untuk melindungi dari proxy
	 * yang terikat sesi tertutup; kegagalan bersifat senyap dan nilai tersimpan
	 * dipertahankan.</p>
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}</b> — bila semua jalur gagal, waktu
	 * server saat ini yang dikembalikan. Karena itu jangan memakai method ini untuk menguji
	 * "apakah tanggal tagihan sudah diisi".</p>
	 *
	 * @return tanggal terbit tagihan; waktu sekarang bila tidak dapat disimpulkan
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_tagihan")
	public Date getTanggalTagihan() {
		try {
			// FIX LazyInitializationException: getPengaturanBiaya() bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getPengaturanBiaya() != null) {
				PengaturanBiaya pb = getPengaturanBiaya();
				if (!Boolean.TRUE.equals(pb.getTanggalTagihanMengikutiBulanBerjalan()) && getBulan() != null) {
					tanggalTagihan = getTanggalTagihanBulanHelper(pb, getBulan());
				} else if (pb.getJenisBiayaSekolah() != null
						&& "Bulanan".equalsIgnoreCase(pb.getJenisBiayaSekolah().getPeriode())) {
					if (getBulan() != null && getTahun() != null) {
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.YEAR, getTahun());
						calendar.set(Calendar.MONTH, getBulan() - 1);
						calendar.set(Calendar.DATE, 1);
						calendar.set(Calendar.HOUR_OF_DAY, 7);
						calendar.set(Calendar.MINUTE, 0);
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
						tanggalTagihan = calendar.getTime();
					} else {
						tanggalTagihan = pb.getTanggalTagihan();
					}
				} else {
					if (Boolean.TRUE.equals(pb.getTanggalTagihanMengikutiDefault()) || tanggalTagihan == null) {
						tanggalTagihan = pb.getTanggalTagihan();
					}
				}
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:getTanggalTagihan-lazy");
		}
		return tanggalTagihan == null ? WaktuUtil.getDate() : tanggalTagihan;
	}

	/**
	 * @param tanggalTagihan tanggal terbit tagihan; dapat ditimpa kembali oleh
	 *                       {@link #getTanggalTagihan()}
	 */
	public void setTanggalTagihan(Date tanggalTagihan) {
		this.tanggalTagihan = tanggalTagihan;
	}

	/**
	 * Denda keterlambatan yang menempel pada tagihan ini. Nilainya ditambahkan ke
	 * {@link #getNominal()} di seluruh titik penjumlahan — layar pembayaran, total Virtual
	 * Account, dan servlet bank.
	 *
	 * <p><b>GETTER MENULIS BALIK</b> ke field {@link #denda}.</p>
	 *
	 * <h4>Cara hitung</h4>
	 * <ol>
	 *   <li>Tagihan yang tidak aktif ({@link #getAktif()}) &rarr; <b>0</b>, tanpa
	 *       perhitungan lebih lanjut.</li>
	 *   <li>Tanggal pembanding: normalnya waktu server sekarang; <b>tetapi</b> bila tagihan
	 *       sudah dibayar, yang dipakai adalah tanggal bayar pada kuitansi. Ini penting agar
	 *       denda tagihan lama tidak terus tumbuh setelah dilunasi.</li>
	 *   <li>Bila {@link #getTanggalDeadline()} sudah terlewat &rarr; denda dihitung; kalau
	 *       tidak &rarr; 0.</li>
	 *   <li>Denda tetap &rarr; <code>PengaturanBiaya.getDenda()</code> apa adanya.</li>
	 *   <li>Denda persentase (<code>dendaMengunakanPersen</code>) &rarr; rumus yang dipakai
	 *       adalah <code>nominal - (nominal * persen/100)</code>.</li>
	 *   <li>Penanda "bukan tagihan" &rarr; denda dipaksa 0 (override terakhir).</li>
	 * </ol>
	 *
	 * <p><b>Kuirk pada rumus persentase.</b> Bacaan harfiah rumus di atas menghasilkan
	 * <i>sisa</i> setelah potongan persen, bukan besaran denda itu sendiri: untuk nominal
	 * 1.000.000 dengan denda 5%, hasilnya 950.000 — hampir seluruh nilai tagihan menjadi
	 * denda, alih-alih 50.000. Rumus yang lazim adalah <code>nominal * persen / 100</code>.
	 * Perilaku ini didokumentasikan apa adanya; <b>tidak diubah</b> karena instalasi yang
	 * berjalan mungkin sudah bergantung padanya dan koreksinya akan mengubah angka rupiah
	 * yang sudah tercetak.</p>
	 *
	 * <p>Perhatikan juga bahwa method ini memanggil {@link #getAktif()},
	 * {@link #getPembayaranSiswaDetail()}, {@link #getTanggalDeadline()}, dan
	 * {@link #getNominal()} — seluruhnya menulis balik, sehingga sekadar menampilkan kolom
	 * denda pada sebuah grid akan menyentuh banyak kolom lain.</p>
	 *
	 * @return besaran denda, minimal 0,0 (tidak pernah {@code null})
	 */
	public Double getDenda() {
		if (!Boolean.TRUE.equals(getAktif()))
			return 0.0;

		if (getPengaturanBiaya() != null) {
			Date tgl = WaktuUtil.getDate();
			pembayaranSiswaDetail = getPembayaranSiswaDetail();
			if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getPembayaranSiswa() != null) {
				tgl = pembayaranSiswaDetail.getPembayaranSiswa().getTanggalBayar();
			}

			tanggalDeadline = getTanggalDeadline();

			if (tanggalDeadline != null && tanggalDeadline.before(tgl)) {
				PengaturanBiaya pb = getPengaturanBiaya();
				if (!Boolean.TRUE.equals(pb.getDendaMengunakanPersen())) {
					denda = pb.getDenda();
				} else {
					Double nominalDenda = pb.getDenda() != null ? pb.getDenda() : 0.0;
					denda = getNominal() - (getNominal() * (nominalDenda / 100.0));
				}
			} else {
				denda = 0.0;
			}
		}

		nominalBiaya = getNominalBiaya();
		if ((nominalBiaya != null && Boolean.TRUE.equals(nominalBiaya.getBukanTagihan())) || ambilBukanTagihanData()
				|| ambilBukanTagihan()) {
			denda = 0.0;
		}

		return denda == null ? 0.0 : denda;
	}

	/**
	 * @param denda besaran denda; akan dihitung ulang oleh {@link #getDenda()}
	 */
	public void setDenda(Double denda) {
		this.denda = denda;
	}

	/**
	 * Batas akhir pembayaran sebelum denda mulai berlaku. Nilai {@code null} berarti
	 * <b>tidak ada denda</b> untuk tagihan ini.
	 *
	 * <p><b>GETTER MENULIS BALIK.</b> Diturunkan ulang dari paket biaya:</p>
	 * <ol>
	 *   <li>paket tanpa <code>terdapatDenda</code> &rarr; <b>{@code null}</b> (denda mati);</li>
	 *   <li>periode tagihan terdaftar di <code>bulanYangTidakAdaDendanya</code> &rarr;
	 *       <b>{@code null}</b>. Pencocokannya memakai pola <code>","+tahunbulan+","</code>,
	 *       jadi daftar itu harus berawal dan berakhir koma agar entri pertama/terakhir ikut
	 *       terbaca;</li>
	 *   <li>periode "Bulanan" dengan bulan &amp; tahun diketahui &rarr; disusun sebagai
	 *       tanggal <code>tanggalDeadlineDenda</code> (bawaan: 1) pukul 07.00 pada bulan
	 *       yang bersangkutan;</li>
	 *   <li>selebihnya &rarr; <code>PengaturanBiaya.getDeadlineTagihan()</code> umum.</li>
	 * </ol>
	 *
	 * <p>Berbeda dengan {@link #getTanggalTagihan()}, method ini <b>boleh</b> mengembalikan
	 * {@code null} — dan {@code null} di sini bermakna, bukan sekadar "belum diisi".</p>
	 *
	 * <p>Akses ke paket biaya dibungkus <code>try/catch</code> untuk proxy dari sesi
	 * tertutup; kegagalan bersifat senyap dan mempertahankan nilai tersimpan.</p>
	 *
	 * @return batas akhir pembayaran, atau {@code null} bila tagihan ini bebas denda
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_deadline")
	public Date getTanggalDeadline() {
		try {
			// FIX LazyInitializationException: getPengaturanBiaya() bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getPengaturanBiaya() != null) {
				PengaturanBiaya pb = getPengaturanBiaya();

				if (Boolean.TRUE.equals(pb.getTerdapatDenda())) {
					if (getTahunbulan() != null && pb.getBulanYangTidakAdaDendanya() != null
							&& pb.getBulanYangTidakAdaDendanya().contains("," + getTahunbulan() + ",")) {
						tanggalDeadline = null;
					} else if (pb.getJenisBiayaSekolah() != null
							&& "Bulanan".equalsIgnoreCase(pb.getJenisBiayaSekolah().getPeriode())) {
						if (getBulan() != null && getTahun() != null) {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.YEAR, getTahun());
							calendar.set(Calendar.MONTH, getBulan() - 1);
							calendar.set(Calendar.DATE,
									pb.getTanggalDeadlineDenda() != null ? pb.getTanggalDeadlineDenda() : 1);
							calendar.set(Calendar.HOUR_OF_DAY, 7);
							calendar.set(Calendar.MINUTE, 0);
							calendar.set(Calendar.SECOND, 0);
							calendar.set(Calendar.MILLISECOND, 0);
							tanggalDeadline = calendar.getTime();
						} else {
							tanggalDeadline = pb.getDeadlineTagihan();
						}
					} else {
						tanggalDeadline = pb.getDeadlineTagihan();
					}
				} else {
					tanggalDeadline = null;
				}
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:getTanggalDeadline-lazy");
		}
		return tanggalDeadline;
	}

	/**
	 * @param tanggalDeadline batas akhir pembayaran; akan dihitung ulang oleh
	 *                        {@link #getTanggalDeadline()}
	 */
	public void setTanggalDeadline(Date tanggalDeadline) {
		this.tanggalDeadline = tanggalDeadline;
	}

	/**
	 * Menghitung potongan <b>tanpa</b> memperhitungkan diskon khusus pembayaran sekali lunas
	 * — versi "polos" dari {@link #getDiskon()}. Namanya mengandung salah ketik yang sudah
	 * terlanjur dipakai luas ("Dikon", seharusnya "Diskon").
	 *
	 * <p>Aturannya jauh lebih sederhana:</p>
	 * <ol>
	 *   <li>bila ada {@link #getDiskonSiswa()} yang <b>tidak</b> memotong tagihan (diskon
	 *       yang dibayarkan sebagai transfer terpisah, bukan pengurang tagihan) &rarr; 0;</li>
	 *   <li>selebihnya &rarr; {@link #getDiskonTidakLangsung()};</li>
	 *   <li>penanda "bukan tagihan" &rarr; 0;</li>
	 *   <li><b>angsuran ke-2 ke atas &rarr; 0</b> — diskon hanya melekat pada angsuran
	 *       pertama, konsisten dengan {@link #getDiskonSiswa()} yang mengosongkan relasi
	 *       diskon untuk <code>bayarKe &gt; 1</code>.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> hasilnya ditulis ke field {@link #diskon}, sehingga memanggil
	 * method ini dapat mengubah nilai yang kemudian dibaca {@link #getDiskon()}.</p>
	 *
	 * <p>Dipakai layar/aksi yang menampilkan "Tagihan bersih" (mis.
	 * {@link #pindahkan(Tagihan, EventListener)} dan <code>/Api TagihanSiswa.split()</code>)
	 * dengan rumus <code>denda + nominal - diskon</code>.</p>
	 *
	 * @return besaran potongan, minimal 0,0 (tidak pernah {@code null})
	 */
	public Double ambilDiskonTanpaDikonBayarSatuKali() {
		nominalBiaya = getNominalBiaya();
		if (getDiskonSiswa() != null && !Boolean.TRUE.equals(getDiskonSiswa().getMemotongTagihan())) {
			diskon = 0.0;
		} else {
			diskon = getDiskonTidakLangsung();
		}

		if ((nominalBiaya != null && Boolean.TRUE.equals(nominalBiaya.getBukanTagihan())) || ambilBukanTagihanData()
				|| ambilBukanTagihan()) {
			diskon = 0.0;
		}
		if (getBayarKe() > 1)
			diskon = 0.0;

		return diskon == null ? 0.0 : diskon;
	}

	/**
	 * Potongan yang berlaku atas tagihan ini.
	 *
	 * <p><b>GETTER MENULIS BALIK</b> ke field {@link #diskon}.</p>
	 *
	 * <h4>Cabang perhitungan</h4>
	 * <ol>
	 *   <li><b>Tarif terkunci &amp; sudah dibayar.</b> Bila komponen sekali lunas dengan
	 *       <code>maksimalBiaya == minimalBiaya</code> (&gt; 0,1) sudah punya pelunasan
	 *       bernilai &gt; 0,1 &rarr; <code>diskon = NominalBiaya.getNominal() - dibayar</code>,
	 *       yaitu selisih antara tarif resmi dan uang yang benar-benar masuk. <b>Inilah
	 *       cabang yang dahulu menjadi setengah dari umpan balik bug "tagihan bertambah tiap
	 *       klik Cari"</b> — lihat {@link #setNominal(Double)}: karena diskon dihitung DARI
	 *       <code>nominalBiaya.getNominal()</code>, setter yang menulis balik ke sana
	 *       menutup lingkarannya.</li>
	 *   <li><b>Sudah dibayar dengan diskon siswa.</b> &rarr;
	 *       <code>diskon = getNominal() - dibayar</code>.</li>
	 *   <li><b>Diskon manual</b> (&gt; 0,1) &rarr; dipakai apa adanya.</li>
	 *   <li><b>Selebihnya</b> &rarr; {@link #ambilDiskonTanpaDikonBayarSatuKali()} ditambah
	 *       <code>PengaturanBiayaItemBiaya.diskonBiaya</code>, tetapi tambahan itu HANYA
	 *       berlaku untuk komponen sekali lunas.</li>
	 * </ol>
	 *
	 * <h4>Kuirk</h4>
	 * <ul>
	 *   <li>Dua cabang pertama membaca field mentah <code>dibayar</code>,
	 *       <code>pembayaranSiswaDetail</code>, dan <code>nominalBiaya</code> — bukan
	 *       getter-nya. Pada entity yang belum dihidrasi penuh, cabang-cabang itu tidak
	 *       terpicu dan perhitungan jatuh ke cabang terakhir.</li>
	 *   <li>Ada dua lapis <code>try/catch</code> bersarang yang keduanya hanya mencetak
	 *       jejak dan mencatat ke audit — kegagalan menghasilkan diskon parsial secara
	 *       senyap, bukan error.</li>
	 * </ul>
	 *
	 * @return besaran potongan, minimal 0,0 (tidak pernah {@code null})
	 */
	public Double getDiskon() {
		PengaturanBiayaItemBiaya pbi = getNominalBiaya() == null ? null
				: getNominalBiaya().getPengaturanBiayaItemBiaya();
		try {
			boolean dibayarSatuKali = nominalBiaya != null && nominalBiaya.getDibayarSebayak() != null
					&& nominalBiaya.getDibayarSebayak() == 1;
			Double currentDibayar = dibayar != null ? dibayar : 0.0;

			if (pbi != null && pembayaranSiswaDetail != null && currentDibayar > 0.1 && dibayarSatuKali
					&& pbi.getMaksimalBiaya() != null && pbi.getMinimalBiaya() != null && pbi.getMaksimalBiaya() > 0.1
					&& pbi.getMaksimalBiaya().equals(pbi.getMinimalBiaya())) {
				diskon = nominalBiaya.getNominal() - currentDibayar;
			} else if (pembayaranSiswaDetail != null && currentDibayar > 0.1 && dibayarSatuKali
					&& getDiskonSiswa() != null) {
				diskon = getNominal() - currentDibayar;
			} else {
				if (getDiskonManual() != null && getDiskonManual() > 0.1) {
					diskon = getDiskonManual();
				} else {
					diskon = ambilDiskonTanpaDikonBayarSatuKali();
					try {
						Double diskonBiaya = pbi != null && pbi.getDiskonBiaya() != null ? pbi.getDiskonBiaya() : 0.0;
						diskon += (pbi == null || !dibayarSatuKali ? 0.0 : diskonBiaya);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:1599");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:1604");
		}
		return diskon == null ? 0.0 : diskon;
	}

	/**
	 * @param diskon besaran potongan; akan dihitung ulang oleh {@link #getDiskon()}
	 */
	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * Jejak posting jurnal akuntansi untuk komponen <b>denda</b>, terpisah dari jurnal pokok
	 * agar pembatalan posting denda tidak ikut membatalkan posting pokok. Pembaca murni.
	 *
	 * @return jejak posting denda, atau {@code null} bila belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_denda_id")
	public PostingHistory getPostingHistoryDenda() {
		return postingHistoryDenda;
	}

	/**
	 * @param postingHistoryDenda jejak posting denda, atau {@code null}
	 */
	public void setPostingHistoryDenda(PostingHistory postingHistoryDenda) {
		this.postingHistoryDenda = postingHistoryDenda;
	}

	/**
	 * Jejak posting jurnal akuntansi untuk komponen <b>uang muka</b> (pembayaran di muka /
	 * <i>dibayar dimuka</i>), yang diposting lewat layar tersendiri karena secara akuntansi
	 * merupakan kewajiban, bukan pendapatan. Pembaca murni.
	 *
	 * @return jejak posting uang muka, atau {@code null} bila belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_uang_muka_id")
	public PostingHistory getPostingHistoryUangMuka() {
		return postingHistoryUangMuka;
	}

	/**
	 * @param postingHistoryUangMuka jejak posting uang muka, atau {@code null}
	 */
	public void setPostingHistoryUangMuka(PostingHistory postingHistoryUangMuka) {
		this.postingHistoryUangMuka = postingHistoryUangMuka;
	}

	/**
	 * Nomor <b>Virtual Account</b> bank yang diterbitkan untuk tagihan ini.
	 * <p>
	 * String kosong dinormalisasi menjadi {@code null} sehingga pemanggil cukup memeriksa
	 * satu kondisi untuk tahu "apakah VA sudah terbit". Tidak ada efek samping.
	 * <p>
	 * <b>Catatan keamanan:</b> nomor VA adalah kunci masuk servlet <code>/MncBank</code>
	 * dan <code>/Va</code> yang tidak terautentikasi. Siapa pun yang memegang atau menebak
	 * nomor ini dapat memperoleh nama siswa, NIS, rincian item biaya, denda, dan nominal
	 * rupiah dari kedua endpoint tersebut. Lihat Javadoc kelas.
	 *
	 * @return nomor Virtual Account, atau {@code null} bila belum terbit
	 */
	public String getVa() {
		return va == null || va.isEmpty() ? null : va;
	}

	/**
	 * @param va nomor Virtual Account bank
	 */
	public void setVa(String va) {
		this.va = va;
	}

	/**
	 * Kedaluwarsa nomor {@link #getVa()} / {@link #getLink()} pembayaran. Setelah waktu ini
	 * terlewat, kanal pembayaran online untuk tagihan ini harus diterbitkan ulang.
	 * <p>
	 * Pembaca murni. Perhatikan bahwa penegakan kedaluwarsa di sisi servlet bank
	 * dikendalikan konfigurasi <code>chek_kadaluarsa</code> — bila konfigurasi itu mati,
	 * VA lama tetap dilayani.
	 *
	 * @return waktu kedaluwarsa kanal pembayaran, atau {@code null} bila tanpa batas
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getExpired() {
		return expired;
	}

	/**
	 * @param expired waktu kedaluwarsa kanal pembayaran
	 */
	public void setExpired(Date expired) {
		this.expired = expired;
	}

	/**
	 * Tahun ajaran tagihan dalam bentuk string "2025/2026".
	 *
	 * <p><b>GETTER MENULIS BALIK:</b> bila paket biaya dapat dimuat, nilainya SELALU
	 * disalin dari <code>PengaturanBiaya.getTahunAjaran()</code> — nilai kolom sendiri tidak
	 * pernah menang. Artinya mengubah tahun ajaran sebuah paket biaya akan menulis ulang
	 * kolom ini pada seluruh tagihan yang menaunginya begitu barisnya tersentuh.</p>
	 *
	 * <p>String ini dipakai sebagai kriteria pencarian di {@link #buatAtauLoadTagihan} dan
	 * sebagai pembanding di {@link #getAktif()} (yang menguji apakah tahun periode
	 * terkandung di dalamnya), sehingga formatnya harus konsisten "YYYY/YYYY".</p>
	 *
	 * @return tahun ajaran, atau {@code null} bila belum dapat disimpulkan
	 */
	public String getTahunAjaran() {
		if (getPengaturanBiaya() != null) {
			tahunAjaran = getPengaturanBiaya().getTahunAjaran();
		}
		return tahunAjaran;
	}

	/**
	 * @param tahunAjaran tahun ajaran "YYYY/YYYY"; akan ditimpa kembali oleh
	 *                    {@link #getTahunAjaran()} bila paket biaya dapat dimuat
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * <i>Snapshot</i> kelas siswa pada tahun ajaran tagihan ini, dipakai laporan tunggakan
	 * dan rekap pembayaran per kelas.
	 *
	 * <p><b>GETTER MENULIS BALIK</b> dengan logika pemilihan yang cukup halus:</p>
	 * <ol>
	 *   <li><b>Utamakan snapshot yang sudah tersimpan</b> — bila kolom sudah terisi, tahun
	 *       ajarannya cocok dengan paket biaya, DAN barisnya masih ada di database
	 *       ({@link #kelasSiswaMasihAdaDiDb}), nilai itu dipertahankan. Komentar di dalam
	 *       kode mencatat alasannya: sebelumnya nilai ini selalu ditimpa oleh
	 *       <code>Siswa.ambilKelas(...)</code> yang dapat membaca cache relasi lama
	 *       <b>setelah siswa pindah kelas</b>.</li>
	 *   <li>Kalau tidak, kandidat baru diambil dari <code>Siswa.ambilKelas(...)</code>.</li>
	 *   <li><b>Kandidat divalidasi keberadaannya di database.</b> Ini perbaikan akar
	 *       masalah, bukan tambalan: karena relasi ini dipetakan
	 *       <code>cascade = PERSIST/MERGE</code>, referensi ke baris kelas yang sudah
	 *       terhapus akan membuat INSERT tagihan baru <b>ditolak Postgres</b> dengan
	 *       pelanggaran foreign key. Kandidat basi karena itu dijadikan {@code null}.</li>
	 * </ol>
	 *
	 * <p>Seluruh <i>exception</i> ditelan dan dijatuhkan ke nilai kolom sendiri lewat
	 * <code>check(...)</code>.</p>
	 *
	 * @return kelas siswa pada tahun ajaran tagihan, atau {@code null} bila tidak diketahui
	 *         atau referensinya sudah tidak valid
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa_id")
	public KelasSiswa getKelasSiswa() {
		try {
			kelasSiswa = check(kelasSiswa);
			siswa = getSiswa();
			pengaturanBiaya = getPengaturanBiaya();
			if (siswa != null && siswa.getId() != null && pengaturanBiaya != null
					&& pengaturanBiaya.getTahunAjaran() != null) {
				// Utamakan snapshot kelas yang sudah disinkronkan pada Tagihan. Sebelumnya nilai
				// ini selalu ditimpa oleh Siswa.ambilKelas(), padahal helper tersebut dapat membaca
				// cache relasi lama setelah siswa pindah kelas.
				if (kelasSiswa != null && kelasSiswa.getId() != null
						&& pengaturanBiaya.getTahunAjaran().equals(kelasSiswa.getTahunAjaran())
						&& kelasSiswaMasihAdaDiDb(kelasSiswa.getId())) {
					return kelasSiswa;
				}
				KelasSiswa kandidat = Siswa.ambilKelas(siswa, pengaturanBiaya.getTahunAjaran());
				// Root cause fix: siswa.kelas / cache KelasSiswaPunyaSiswa (dipakai oleh
				// Siswa.ambilKelas) bisa BASI -- menyimpan referensi ke baris "kelas" yang
				// sudah terhapus di DB. Karena field ini dipetakan @ManyToOne dgn
				// cascade PERSIST/MERGE, jika kandidat basi ini dipakai apa adanya saat
				// Hibernate meng-INSERT Tagihan baru, Postgres menolak dengan
				// ConstraintViolationException (FK kelas_siswa_id -> kelas, id tidak ada).
				// Validasi dulu keberadaannya di DB sebelum dipakai sebagai referensi.
				if (kandidat != null && kandidat.getId() != null && !kelasSiswaMasihAdaDiDb(kandidat.getId())) {
					kandidat = null;
				}
				kelasSiswa = kandidat;
			}
		} catch (Exception e) {
			kelasSiswa = check(kelasSiswa);
		}
		return kelasSiswa;
	}

	// Cache kecil (in-memory, TTL) hasil validasi keberadaan baris "kelas" di DB,
	// supaya getKelasSiswa() yang dipanggil berulang kali (mis. saat render grid
	// utk banyak siswa dgn kelas yg sama) tidak membanjiri DB dengan query yang sama.
	/**
	 * Cache validasi keberadaan baris kelas: id kelas &rarr; cap waktu (milidetik) saat
	 * terakhir dipastikan ada di database. Bersifat statis, jadi dibagi seluruh proses.
	 */
	private static final java.util.concurrent.ConcurrentHashMap<Long, Long> KELAS_EXIST_CACHE_AT =
			new java.util.concurrent.ConcurrentHashMap<Long, Long>();
	/** Masa berlaku entri {@link #KELAS_EXIST_CACHE_AT}: 5 menit. */
	private static final long KELAS_EXIST_CACHE_TTL_MS = 5 * 60 * 1000L;

	/**
	 * Memastikan sebuah baris kelas masih benar-benar ada di database, sebagai penjaga
	 * terhadap referensi basi yang akan ditolak <i>foreign key</i> saat
	 * {@link #getKelasSiswa()} dipakai untuk INSERT tagihan baru.
	 *
	 * <p>Hasil positif di-cache selama 5 menit ({@link #KELAS_EXIST_CACHE_TTL_MS}) agar
	 * render grid berisi banyak siswa sekelas tidak menghasilkan satu query per baris.
	 * Hasil negatif justru <b>menghapus</b> entri cache sehingga selalu diuji ulang.</p>
	 *
	 * <p>Membuka dan menutup sesi Hibernate sendiri (ditutup di <code>finally</code>) —
	 * disengaja, agar validasi ini tidak mengotori <i>persistence context</i> pemanggil.</p>
	 *
	 * <p><b>Gagal ke sisi longgar:</b> bila query-nya sendiri gagal (mis. basis data sedang
	 * sibuk), method mengembalikan {@code true} — kelas dianggap masih ada. Ini disengaja
	 * agar gangguan sesaat tidak menghapus snapshot kelas yang sebenarnya sah, tetapi
	 * berarti penjagaannya tidak mutlak.</p>
	 *
	 * @param id id baris kelas yang diuji; {@code null} menghasilkan {@code false}
	 * @return {@code true} bila baris kelas ada, atau bila validasinya gagal
	 */
	private static boolean kelasSiswaMasihAdaDiDb(Long id) {
		if (id == null) {
			return false;
		}
		Long cachedAt = KELAS_EXIST_CACHE_AT.get(id);
		if (cachedAt != null && (System.currentTimeMillis() - cachedAt) < KELAS_EXIST_CACHE_TTL_MS) {
			return true;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Object hasil = session.createCriteria(KelasSiswa.class).add(Restrictions.idEq(id))
					.setProjection(Projections.rowCount()).uniqueResult();
			boolean ada = hasil != null && ((Number) hasil).intValue() > 0;
			if (ada) {
				KELAS_EXIST_CACHE_AT.put(id, System.currentTimeMillis());
			} else {
				KELAS_EXIST_CACHE_AT.remove(id);
			}
			return ada;
		} catch (Exception e) {
			// Validasi gagal (mis. DB sedang sibuk) -- jangan blokir alur lama, anggap
			// masih valid supaya perilaku existing tidak berubah kalau bukan kasus stale.
			ais.common.ErrorAuditUtil.record(e, "Tagihan.kelasSiswaMasihAdaDiDb id=" + id);
			return true;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:kelasSiswaMasihAdaDiDb");
				}
			}
		}
	}

	/**
	 * @param kelasSiswa snapshot kelas siswa; akan diverifikasi ulang oleh
	 *                   {@link #getKelasSiswa()}
	 */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}

	/**
	 * Paket biaya yang menaungi tagihan ini — pemasok tahun ajaran, tahun angkatan,
	 * konfigurasi denda, tanggal terbit/deadline per bulan, daftar bulan tanpa tagihan,
	 * dan jenis biaya.
	 *
	 * <p><b>GETTER MENULIS BALIK.</b> Nilainya <b>selalu</b> diambil ulang dari
	 * {@link #getNominalBiaya()} bila tarif dapat dimuat; kolom
	 * <code>pengaturan_biaya</code> milik tagihan hanya dipakai sebagai cadangan saat tarif
	 * tidak tersedia. Dengan kata lain, tarif adalah acuan kebenaran dan tagihan akan
	 * "mengikuti" bila tarifnya dipindahkan ke paket biaya lain.</p>
	 *
	 * <p>Seluruh <i>exception</i> ditelan dan dijatuhkan ke nilai kolom sendiri lewat
	 * <code>check(...)</code>.</p>
	 *
	 * @return paket biaya yang menaungi, atau {@code null} bila tak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengaturan_biaya")
	public PengaturanBiaya getPengaturanBiaya() {
		try {
			nominalBiaya = getNominalBiaya();
			if (nominalBiaya != null) {
				pengaturanBiaya = nominalBiaya.getPengaturanBiaya();
			} else {
				pengaturanBiaya = check(pengaturanBiaya);
			}
		} catch (Exception e) {
			pengaturanBiaya = check(pengaturanBiaya);
		}
		return pengaturanBiaya;
	}

	/**
	 * @param pengaturanBiaya paket biaya yang menaungi; akan ditimpa kembali oleh
	 *                        {@link #getPengaturanBiaya()} bila tarif dapat dimuat
	 */
	public void setPengaturanBiaya(PengaturanBiaya pengaturanBiaya) {
		this.pengaturanBiaya = pengaturanBiaya;
	}

	/**
	 * Menentukan apakah periode ini termasuk <b>"bulan yang tidak ada tagihannya"</b>
	 * menurut konfigurasi paket biaya — misalnya SPP yang diliburkan pada bulan Ramadan
	 * atau libur akhir tahun.
	 *
	 * <p>Berbeda dengan {@link #ambilBukanTagihanData()} yang sekadar membaca kolom, method
	 * ini <b>menghitung</b> dari daftar <code>PengaturanBiaya.bulanYangTidakAdaTagihannya</code>.
	 * Formatnya berupa pasangan <code>kodeItem:bulan</code> yang diapit koma, dicocokkan
	 * sebagai <code>","+kode+":"+bulan+","</code> dan dibandingkan tanpa memperhatikan
	 * besar-kecil huruf. Konsekuensinya sama seperti daftar berkoma lain di kelas ini:
	 * daftar harus berawal dan berakhir koma agar entri pertama/terakhir ikut terbaca.</p>
	 *
	 * <p>Berlaku hanya bila paket ber-flag
	 * <code>terdapatBulanYangTidakAdaTagihannya</code>, komponen biaya punya kode, dan bulan
	 * tagihan diketahui serta berada di rentang 1-12.</p>
	 *
	 * <p><b>Tidak ada efek samping langsung</b> pada field kelas ini, tetapi memanggil
	 * {@link #getPengaturanBiaya()}, {@link #getItemBiayaSekolah()}, dan {@link #getBulan()}
	 * yang menulis balik.</p>
	 *
	 * <p>Hasil {@code true} memaksa {@link #getNominal()}, {@link #ambilNominal()},
	 * {@link #getDenda()}, dan {@link #ambilDiskonTanpaDikonBayarSatuKali()} menjadi 0, serta
	 * mematikan {@link #getAktif()}.</p>
	 *
	 * @return {@code true} bila periode ini dikecualikan dari penagihan
	 */
	public Boolean ambilBukanTagihan() {
		Boolean isBukanTagihan = false;
		pengaturanBiaya = getPengaturanBiaya();
		if (pengaturanBiaya != null && Boolean.TRUE.equals(pengaturanBiaya.getTerdapatBulanYangTidakAdaTagihannya())
				&& getItemBiayaSekolah() != null && getItemBiayaSekolah().getKode() != null && getBulan() != null) {

			String t = getItemBiayaSekolah().getKode().toLowerCase();
			String bBulan = pengaturanBiaya.getBulanYangTidakAdaTagihannya() != null
					? pengaturanBiaya.getBulanYangTidakAdaTagihannya().toLowerCase()
					: "";

			if (getBulan() >= 1 && getBulan() <= 12) {
				if (bBulan.contains("," + t + ":" + getBulan() + ",")) {
					isBukanTagihan = true;
				}
			}
		}
		return isBukanTagihan;
	}

	/**
	 * Membaca penanda "bukan tagihan" milik baris ini dengan {@code null} dinormalisasi
	 * menjadi {@code false} — versi bebas-{@code null} dari {@link #getBukanTagihan()}.
	 * <p>
	 * Dipakai di dalam ekspresi perhitungan ({@link #getNominal()}, {@link #getDenda()},
	 * {@link #getAktif()}) yang selalu digabung dengan {@link #ambilBukanTagihan()} lewat
	 * operator ATAU: baris dikecualikan bila salah satu sumber mengatakannya bukan tagihan.
	 * <p>
	 * Pembaca murni, tanpa efek samping — sengaja dibedakan dari getter berpola JavaBean
	 * agar Hibernate tidak memetakannya sebagai properti kedua atas kolom yang sama.
	 *
	 * @return {@code true} bila baris ini ditandai bukan tagihan
	 */
	public Boolean ambilBukanTagihanData() {
		return bukanTagihan == null ? false : bukanTagihan;
	}

	/**
	 * Penanda "bukan tagihan" yang dipetakan ke kolom database (dicentang petugas untuk
	 * membatalkan sebuah kewajiban tanpa menghapus barisnya).
	 * <p>
	 * Mengembalikan nilai mentah — dapat {@code null} untuk baris lama yang belum pernah
	 * disetel. Untuk pemakaian di dalam perhitungan, pakai {@link #ambilBukanTagihanData()}
	 * yang sudah bebas-{@code null}.
	 *
	 * @return {@code true}/{@code false}/{@code null} sesuai isi kolom
	 */
	public Boolean getBukanTagihan() {
		return bukanTagihan;
	}

	/**
	 * Menyetel penanda "bukan tagihan".
	 *
	 * <p><b>Setter ini adalah lokasi bug produksi kedua yang sudah diperbaiki</b> — komentar
	 * di dalam badan method (dipertahankan apa adanya, jangan dihapus) mendokumentasikan
	 * gejala <i>"centang aktif kembali ter-uncheck saat refresh"</i>. Mekanismenya sejenis
	 * dengan bug {@link #setNominal(Double)}: versi lama setter ini punya efek samping
	 * (menghapus <code>aktifkanmanual</code> dan memaksa <code>setAktif(!bukanTagihan)</code>),
	 * sementara entity ini property-access sehingga setter dipanggil Hibernate SETIAP kali
	 * baris dimuat. Akibatnya centang "aktifkan manual" yang baru disimpan admin langsung
	 * terhapus lagi pada pemuatan berikutnya, dan riwayat audit memperlihatkan pasangan
	 * nilai yang bolak-balik <code>true/true &rarr; false/kosong</code> setiap beberapa
	 * detik.</p>
	 *
	 * <p>Perbaikannya sama: setter dibuat murni. Kebenaran status tetap terjaga karena
	 * {@link #getAktif()} memeriksa penanda "bukan tagihan" <b>sebelum</b> override
	 * <code>aktifkanmanual</code>, sehingga efek samping itu memang tidak diperlukan.</p>
	 *
	 * @param bukanTagihan {@code true} untuk membatalkan kewajiban tanpa menghapus barisnya
	 */
	public void setBukanTagihan(Boolean bukanTagihan) {
		// SETTER MURNI (perbaikan "centang aktif kembali un-check saat refresh"):
		// dulu setter ini ikut MENGHAPUS aktifkanmanual (set null) + memaksa
		// setAktif(!bukanTagihan). Entity ini property-access, sehingga Hibernate
		// memanggil setter ini SETIAP kali baris dimuat dari DB -> aktifkanmanual=true
		// yang baru dicentang admin langsung terhapus lagi pada muatan berikutnya
		// (flush menulis aktif=false + aktifkanmanual=null; lihat audit Tagihan yang
		// bolak-balik true/true -> false/kosong berpasangan tiap beberapa detik).
		// Status aktif kini murni diturunkan getAktif() -- di sana bukanTagihan sudah
		// diprioritaskan SEBELUM override aktifkanmanual, jadi efek samping ini tidak
		// diperlukan untuk kebenaran status.
		this.bukanTagihan = bukanTagihan;
	}

	/**
	 * Pengguna yang <b>mengunci</b> nominal tagihan ini pada nilai sementara hasil
	 * negosiasi/keringanan ({@link #getBiayaTemporary()}), alih-alih membiarkannya dihitung
	 * dari tarif.
	 * <p>
	 * Selama relasi ini terisi, {@link #getNominal()} mengembalikan
	 * {@link #getBiayaTemporary()} dan mengabaikan seluruh cabang perhitungannya. Ini
	 * sekaligus menjadi catatan <i>siapa</i> yang bertanggung jawab atas keringanan
	 * tersebut. Kunci serupa juga dapat berada di tingkat paket biaya
	 * (<code>PengaturanBiaya.getKunci()</code>), yang diperiksa sebagai cadangan.
	 * <p>
	 * Dibungkus <code>check(...)</code> terhadap proxy dari sesi tertutup.
	 *
	 * @return pengguna pengunci nominal, atau {@code null} bila nominal dihitung normal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kunci", nullable = true)
	public Tbmuser getKunci() {
		kunci = check(kunci);
		return kunci;
	}

	/**
	 * @param kunci pengguna yang mengunci nominal pada {@link #getBiayaTemporary()}
	 */
	public void setKunci(Tbmuser kunci) {
		this.kunci = kunci;
	}

	/**
	 * Nominal sementara hasil simulasi/negosiasi keringanan biaya.
	 *
	 * <p><b>GETTER MENULIS BALIK, dan arah logikanya terbalik dari dugaan:</b> ketika
	 * {@link #getKunci()} <b>kosong</b> (tidak ada keringanan), field ini justru
	 * <i>ditimpa</i> dengan {@link #getNominal()}. Jadi nilai sementara hanya "bertahan"
	 * selama kuncinya terpasang; begitu kunci dilepas, angka negosiasi lama tertimpa nominal
	 * hasil hitung dan <b>tidak dapat dipulihkan</b>.</p>
	 *
	 * <p>Sisi baiknya, ini membuat field selalu berisi angka yang wajar sebagai nilai awal
	 * saat petugas mulai menyunting keringanan. Sisi buruknya, ia menjadi jalur ketiga
	 * di kelas ini yang menghapus data finansial hanya karena barisnya dibaca.</p>
	 *
	 * @return nominal sementara; sama dengan {@link #getNominal()} bila tidak terkunci
	 */
	public Double getBiayaTemporary() {
		kunci = getKunci();
		if (kunci == null)
			biayaTemporary = getNominal();
		return biayaTemporary;
	}

	/**
	 * @param biayaTemporary nominal sementara hasil negosiasi; hanya bertahan selama
	 *                       {@link #getKunci()} terisi
	 */
	public void setBiayaTemporary(Double biayaTemporary) {
		this.biayaTemporary = biayaTemporary;
	}

	/**
	 * Tautan pembayaran (<i>payment link</i>) kanal online untuk tagihan ini.
	 *
	 * <p>Getter ini <b>menormalkan tanpa menyimpan</b>: nilai kosong menjadi string kosong,
	 * dan tautan yang belum berawalan <code>https</code> diberi awalan
	 * <code>https://</code>. Hasil normalisasi TIDAK ditulis balik ke field, jadi berbeda
	 * dari getter lain di kelas ini — kolom database tetap menyimpan apa yang diisikan.</p>
	 *
	 * <p><b>Kuirk:</b> pemeriksaannya <code>startsWith("https")</code>, bukan
	 * <code>"https://"</code>. Tautan yang kebetulan dimulai dengan kata "https" tanpa
	 * pemisah (mis. <code>httpsku.example.com</code>) akan lolos tanpa diberi skema.
	 * Tautan <code>http://</code> biasa akan menjadi
	 * <code>https://http://...</code> karena tidak berawalan "https".</p>
	 *
	 * @return tautan pembayaran yang sudah dinormalkan, atau string kosong bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getLink() {
		return link == null || link.trim().isEmpty() ? ""
				: !link.startsWith("https") ? "https://" + link.trim() : link.trim();
	}

	/**
	 * @param link tautan pembayaran kanal online; disimpan apa adanya tanpa normalisasi
	 */
	public void setLink(String link) {
		this.link = link;
	}


	/**
	 * Diskon siswa yang sedang berlaku atas tagihan ini.
	 *
	 * <p><b>GETTER DESTRUKTIF — mengosongkan relasi secara permanen.</b> Ada dua jalur
	 * penghapusan:</p>
	 * <ol>
	 *   <li><b>Angsuran ke-2 ke atas</b> ({@link #getBayarKe()} &gt; 1) &rarr;
	 *       <code>diskonSiswa = null</code> dan langsung {@code null} dikembalikan. Aturan
	 *       bisnisnya wajar (diskon hanya melekat pada angsuran pertama), tetapi
	 *       implementasinya <b>menulis</b> alih-alih sekadar menyembunyikan: kolom
	 *       <code>diskon_siswa</code> pada baris angsuran itu akan menjadi NULL di
	 *       database.</li>
	 *   <li><b>Masa berlaku diskon sudah lewat</b> ({@link #isDiskonSiswaMasihBerlaku})
	 *       &rarr; <code>diskonSiswa = null</code>. Ini yang lebih berbahaya: begitu periode
	 *       sebuah diskon berakhir, cukup MEMBUKA layar tagihan lama untuk menghapus catatan
	 *       "diskon apa yang dahulu dipakai" pada seluruh tagihan yang tersentuh.</li>
	 * </ol>
	 *
	 * <p>Kolom cadangan {@link #getDiskonSiswaAsli()} ada justru untuk menyelamatkan
	 * informasi ini, dan method ini memang berusaha memulihkan dari sana
	 * (<code>diskonSiswa == null &amp;&amp; diskonSiswaAsli != null</code>). Namun pemulihan
	 * itu terjadi <b>sebelum</b> uji masa berlaku, sehingga diskon yang sudah kedaluwarsa
	 * tetap berakhir {@code null}. Selain itu {@link #setDiskonSiswa(DiskonSiswa)} mengisi
	 * kedua kolom sekaligus, jadi cadangan hanya terjaga bila penyetelan aslinya lewat
	 * setter tersebut.</p>
	 *
	 * <p>Kedua relasi dilewatkan {@link #resolveDiskonSiswaForRead(DiskonSiswa)} lebih dahulu
	 * agar proxy dari sesi tertutup dimuat ulang, bukan meledak. Seluruh <i>exception</i>
	 * ditelan menjadi {@code null} — yaitu kegagalan pun berujung "tidak ada diskon".</p>
	 *
	 * @return diskon yang berlaku, atau {@code null} bila tidak ada / sudah kedaluwarsa /
	 *         ini bukan angsuran pertama
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon_siswa", nullable = true)
	public DiskonSiswa getDiskonSiswa() {
		// LAZY_GETTER_CHECK_EXCEPTION: resolver khusus juga memvalidasi masa berlaku diskon.
		try {
			if (getBayarKe() != null && getBayarKe().intValue() > 1) {
				diskonSiswa = null;
				return null;
			}

			diskonSiswa = resolveDiskonSiswaForRead(diskonSiswa);
			diskonSiswaAsli = resolveDiskonSiswaForRead(diskonSiswaAsli);

			if (diskonSiswa == null && diskonSiswaAsli != null) {
				diskonSiswa = diskonSiswaAsli;
			}

			if (diskonSiswa != null && !isDiskonSiswaMasihBerlaku(diskonSiswa)) {
				diskonSiswa = null;
			}
		} catch (Exception e) {
			diskonSiswa = null;
		}
		return diskonSiswa;
	}

	/**
	 * Menguji apakah sebuah diskon masih berada dalam masa berlakunya pada saat ini.
	 * <p>
	 * Batas mulai dan batas akhir keduanya opsional ({@code null} berarti tanpa batas), dan
	 * <b>inklusif</b>: hari yang sama persis dengan tanggal batas dianggap masih berlaku.
	 * Perbandingan inklusif itu dilakukan dengan membandingkan hasil format tanggal
	 * (<code>Common.dateFormat83</code>), bukan aritmetika waktu — cara yang lugas untuk
	 * mengabaikan komponen jam.
	 * <p>
	 * <b>Gagal ke sisi ketat:</b> bila membaca tanggal diskon melempar exception, method
	 * mengembalikan {@code false}. Komentar di dalam kode menjelaskan alasannya — satu baris
	 * diskon yang datanya rusak (proxy <code>Sekolah</code> bertipe id salah) tidak boleh
	 * menggagalkan SELURUH perhitungan tagihan/pembayaran. Konsekuensinya, kerusakan data
	 * diskon muncul sebagai "diskon hilang", bukan sebagai error.
	 *
	 * @param diskon diskon yang diuji; {@code null} menghasilkan {@code false}
	 * @return {@code true} bila diskon masih berlaku hari ini
	 */
	private boolean isDiskonSiswaMasihBerlaku(DiskonSiswa diskon) {
		if (diskon == null) {
			return false;
		}
		Date sekarang = WaktuUtil.getDate();
		Date dMulai;
		Date dSampai;
		try {
			// FIX TypeMismatchException (proxy Sekolah id salah tipe saat lazy-load DiskonSiswa yang
			// datanya rusak): SATU diskon rusak jangan menggagalkan SELURUH hitung tagihan/pembayaran.
			// Anggap diskon tidak berlaku bila akses tanggalnya gagal.
			dMulai = diskon.getDiskonMulai();
			dSampai = diskon.getDiskonSampai();
		} catch (Exception e) {
			return false;
		}

		boolean mulaiValid = dMulai == null || sekarang.after(dMulai)
				|| Common.dateFormat83.get().format(dMulai).equalsIgnoreCase(Common.dateFormat83.get().format(sekarang));
		boolean sampaiValid = dSampai == null || sekarang.before(dSampai)
				|| Common.dateFormat83.get().format(dSampai).equalsIgnoreCase(Common.dateFormat83.get().format(sekarang));

		return mulaiValid && sampaiValid;
	}

	/**
	 * Menjadikan sebuah referensi diskon <b>aman untuk dibaca</b>, apa pun keadaan sesi
	 * Hibernate yang mengikatnya.
	 *
	 * <p>Tiga keadaan ditangani:</p>
	 * <ol>
	 *   <li><b>Proxy dengan sesi masih terbuka</b> &rarr; dikembalikan apa adanya (murah,
	 *       tidak ada query tambahan);</li>
	 *   <li><b>Proxy dengan sesi sudah tertutup</b> &rarr; id-nya diambil dari
	 *       <code>LazyInitializer</code> (aman, tidak memicu lazy-load) lalu entity dimuat
	 *       ulang lewat {@link #loadDiskonSiswaById(Serializable)} pada sesi baru;</li>
	 *   <li><b>Objek belum terinisialisasi</b> (<code>!Hibernate.isInitialized</code>)
	 *       &rarr; dimuat ulang berdasarkan id-nya.</li>
	 * </ol>
	 *
	 * <p>Bila langkah mana pun gagal, method mencoba sekali lagi lewat
	 * <code>diskon.getId()</code>; kegagalan kedua menghasilkan {@code null}. Ini pola
	 * "putus rantai lazy" yang dipakai agar satu relasi diskon yang bermasalah tidak
	 * merambat menjadi kegagalan seluruh perhitungan tagihan.</p>
	 *
	 * @param diskon referensi diskon yang mungkin berupa proxy basi; boleh {@code null}
	 * @return diskon yang aman dibaca, atau {@code null} bila tidak dapat dipulihkan
	 */
	private DiskonSiswa resolveDiskonSiswaForRead(DiskonSiswa diskon) {
		if (diskon == null) {
			return null;
		}

		Serializable identifier = null;
		try {
			if (diskon instanceof HibernateProxy) {
				LazyInitializer initializer = ((HibernateProxy) diskon).getHibernateLazyInitializer();
				identifier = initializer == null ? null : initializer.getIdentifier();
				if (initializer != null && initializer.getSession() != null && initializer.getSession().isOpen()) {
					return diskon;
				}
				return loadDiskonSiswaById(identifier);
			}

			if (!Hibernate.isInitialized(diskon)) {
				return loadDiskonSiswaById(diskon.getId());
			}

			return diskon;
		} catch (Exception e) {
			try {
				if (identifier == null) {
					identifier = diskon.getId();
				}
				return loadDiskonSiswaById(identifier);
			} catch (Exception ignored) {
				return null;
			}
		}
	}

	/**
	 * Memuat sebuah {@link ais.database.model.sekolah.DiskonSiswa} pada <b>sesi Hibernate
	 * baru yang terisolasi</b>, lalu "menghangatkan" properti yang akan dipakai
	 * ({@code id}, {@code nama}, {@code memotongTagihan}, {@code diskonMulai},
	 * {@code diskonSampai}) selagi sesi masih terbuka.
	 * <p>
	 * Pemanggilan getter yang tampak sia-sia itu justru intinya: setelah sesi ditutup di
	 * {@link #closeDiskonLookupSession(Session)}, entity menjadi <i>detached</i> dan properti
	 * yang belum sempat dimuat tidak akan bisa diakses lagi.
	 * <p>
	 * Kegagalan hanya dicetak ke <code>System.out</code> (bukan dicatat ke audit error
	 * seperti di tempat lain pada kelas ini) dan menghasilkan {@code null}.
	 *
	 * @param identifier id diskon; {@code null} menghasilkan {@code null}
	 * @return diskon yang sudah dimuat dan siap dibaca dalam keadaan detached, atau {@code null}
	 */
	private DiskonSiswa loadDiskonSiswaById(Serializable identifier) {
		if (identifier == null) {
			return null;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Object data = session.get(DiskonSiswa.class, identifier);
			if (data instanceof DiskonSiswa) {
				DiskonSiswa diskon = (DiskonSiswa) data;
				diskon.getId();
				diskon.getNama();
				diskon.getMemotongTagihan();
				diskon.getDiskonMulai();
				diskon.getDiskonSampai();
				return diskon;
			}
		} catch (Exception e) {
			System.out.println("DiskonSiswa id " + identifier + " tidak dapat dimuat saat hitung tagihan: "
					+ e.getMessage());
		} finally {
			closeDiskonLookupSession(session);
		}
		return null;
	}

	/**
	 * Menutup sesi bantu milik {@link #loadDiskonSiswaById(Serializable)} secara bertahap:
	 * <code>clear()</code> &rarr; <code>disconnect()</code> &rarr; <code>close()</code>,
	 * masing-masing dalam blok <code>try/catch</code>-nya sendiri sehingga kegagalan satu
	 * langkah tidak menghalangi langkah berikutnya.
	 * <p>
	 * <code>clear()</code> lebih dahulu penting agar entity yang baru dimuat tidak
	 * tertinggal di <i>persistence context</i>, dan <code>disconnect()</code> mengembalikan
	 * koneksi ke pool lebih awal — pada layar yang merender ratusan baris tagihan, sesi
	 * bantu seperti ini dibuka sangat sering.
	 *
	 * @param session sesi bantu yang hendak ditutup; {@code null} diabaikan
	 */
	private void closeDiskonLookupSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:1891");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:1895");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/Tagihan.java:1901");
		}
	}

	/**
	 * Menyetel diskon siswa. <b>Mengisi DUA kolom sekaligus</b>: {@link #diskonSiswa}
	 * (nilai kerja, yang dapat dikosongkan {@link #getDiskonSiswa()}) dan
	 * {@link #diskonSiswaAsli} (cadangan permanen).
	 * <p>
	 * Karena itu, satu-satunya cara agar jejak "diskon apa yang dahulu diberikan" bertahan
	 * adalah menyetelnya lewat method ini, bukan lewat
	 * {@link #setDiskonSiswaAsli(DiskonSiswa)} atau penulisan kolom secara terpisah.
	 *
	 * @param diskonSiswa diskon yang diberikan, atau {@code null} untuk mencabutnya
	 */
	public void setDiskonSiswa(DiskonSiswa diskonSiswa) {
		setDiskonSiswaAsli(diskonSiswa);
		this.diskonSiswa = diskonSiswa;
	}

	/**
	 * Potongan yang berasal dari sumber lain di luar {@link #getDiskonSiswa()} — beasiswa,
	 * subsidi yayasan, atau potongan yang dihitung oleh alur pembayaran dan dititipkan ke
	 * baris tagihan.
	 * <p>
	 * Pembaca murni dengan normalisasi {@code null} &rarr; 0,0. Nilai inilah yang menjadi
	 * dasar {@link #ambilDiskonTanpaDikonBayarSatuKali()}.
	 *
	 * @return besaran potongan tidak langsung, minimal 0,0
	 */
	public Double getDiskonTidakLangsung() {
		return diskonTidakLangsung == null ? 0.0 : diskonTidakLangsung;
	}

	/**
	 * @param diskonTidakLangsung besaran potongan dari sumber lain
	 */
	public void setDiskonTidakLangsung(Double diskonTidakLangsung) {
		this.diskonTidakLangsung = diskonTidakLangsung;
	}

	/**
	 * Jejak posting jurnal akuntansi untuk komponen <b>diskon</b>, terpisah dari jurnal
	 * pokok, denda, dan uang muka. Pembaca murni.
	 *
	 * @return jejak posting diskon, atau {@code null} bila belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_diskon_id")
	public PostingHistory getPostingHistoryDiskon() {
		return postingHistoryDiskon;
	}

	/**
	 * @param postingHistoryDiskon jejak posting diskon, atau {@code null}
	 */
	public void setPostingHistoryDiskon(PostingHistory postingHistoryDiskon) {
		this.postingHistoryDiskon = postingHistoryDiskon;
	}

	/**
	 * Tahun angkatan pihak tertagih, dipakai laporan rekap per angkatan.
	 *
	 * <p><b>GETTER MENULIS BALIK:</b> nilainya selalu disalin ulang dari
	 * <code>tahunMasuk</code> milik {@link #getSiswa()} atau, bila tidak ada,
	 * {@link #getCalonSiswa()}. Kolom sendiri tidak pernah menang, sehingga koreksi tahun
	 * masuk seorang siswa merambat ke seluruh tagihannya.</p>
	 *
	 * <p>Nilai ini dibandingkan dengan <code>PengaturanBiaya.tahunAngkatan</code> di
	 * {@link #getBoleh} untuk menentukan kelayakan tagihan.</p>
	 *
	 * @return tahun angkatan, atau {@code null} bila tidak diketahui
	 */
	public Integer getTahunAngkatan() {
		siswa = getSiswa();
		calonSiswa = getCalonSiswa();
		if (siswa != null)
			tahunAngkatan = siswa.getTahunMasuk();
		else if (calonSiswa != null)
			tahunAngkatan = calonSiswa.getTahunMasuk();
		return tahunAngkatan;
	}

	/**
	 * @param tahunAngkatan tahun angkatan; akan ditimpa kembali oleh
	 *                      {@link #getTahunAngkatan()} bila siswa/calon siswa dapat dimuat
	 */
	public void setTahunAngkatan(Integer tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * Jumlah rupiah yang sudah benar-benar dibayarkan untuk tagihan ini.
	 *
	 * <p><b>GETTER MENULIS BALIK:</b> nilainya selalu diambil ulang dari
	 * <code>PembayaranSiswaDetail.getNominal()</code>, atau 0 bila belum ada pelunasan.</p>
	 *
	 * <p><b>Kuirk penting:</b> method ini membaca field <code>pembayaranSiswaDetail</code>
	 * secara MENTAH, bukan lewat {@link #getPembayaranSiswaDetail()}. Akibatnya nilainya
	 * bergantung pada apakah getter tersebut sudah pernah dipanggil pada instance ini —
	 * termasuk apakah efek pemutusan relasi di sana sudah terjadi. Pada entity yang baru
	 * dimuat dan belum "dihangatkan", method ini dapat mengembalikan 0 meski tagihan
	 * sebenarnya sudah lunas.</p>
	 *
	 * <p>Bila akses melempar exception, relasi dianggap tidak ada: field
	 * <code>pembayaranSiswaDetail</code> <b>dikosongkan</b> dan hasilnya 0 — satu lagi
	 * jalur di mana kegagalan pembacaan berujung pada penulisan.</p>
	 *
	 * <p>Nilai ini menjadi masukan {@link #getDiskon()} pada cabang "sudah dibayar".</p>
	 *
	 * @return jumlah yang sudah dibayarkan, 0,0 bila belum ada pelunasan
	 */
	public Double getDibayar() {
		try {
			if (pembayaranSiswaDetail != null) {
				dibayar = pembayaranSiswaDetail.getNominal();
			} else {
				dibayar = 0.0;
			}
		} catch (Exception e) {
			pembayaranSiswaDetail = null;
			dibayar = 0.0;
		}
		return dibayar;
	}

	/**
	 * @param dibayar jumlah yang sudah dibayarkan; akan dihitung ulang oleh
	 *                {@link #getDibayar()}
	 */
	public void setDibayar(Double dibayar) {
		this.dibayar = dibayar;
	}

	/**
	 * Sekolah pemilik tagihan ini.
	 *
	 * <p><b>GETTER MENULIS BALIK:</b> selalu diturunkan dari {@link #getSiswa()} atau
	 * {@link #getCalonSiswa()}; kolom sendiri hanya dipakai sebagai cadangan terakhir
	 * (lewat <code>check(...)</code>) ketika kedua pihak tertagih tidak tersedia.</p>
	 *
	 * <p><b>Catatan cakupan tenant.</b> Kolom ini bersifat <i>denormalisasi untuk
	 * pelaporan</i>, <b>bukan</b> penyaring keamanan: tidak ada satu pun jalur di kelas ini
	 * yang memfilter tagihan berdasarkan sekolah pengguna yang sedang masuk. Penyaringan
	 * tenant terjadi (atau tidak terjadi) di lapisan layar. Pada pemilih siswa yang memasok
	 * tagihan, filter sekolah itu sendiri <b>dilepas sepenuhnya</b> ketika paket biaya
	 * ber-flag "khusus buat siswa tertentu" — lihat Javadoc kelas.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila tidak dapat disimpulkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		siswa = getSiswa();
		calonSiswa = getCalonSiswa();
		if (siswa != null)
			sekolah = siswa.getSekolah();
		else if (calonSiswa != null)
			sekolah = calonSiswa.getSekolah();
		else
			sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan penjaga: entity yang <b>belum tersimpan</b>
	 * (id-nya {@code null}) ditolak dan diganti {@code null}.
	 * <p>
	 * Penjaga ini mencegah Hibernate mencoba meng-<i>cascade</i> penyimpanan sebuah
	 * <code>Sekolah</code> baru hanya karena tagihan menunjuk padanya.
	 *
	 * @param sekolah sekolah pemilik; entity tanpa id disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Tanggal uang benar-benar diterima, diambil dari kuitansi
	 * (<code>PembayaranSiswa.getTanggalBayar()</code>).
	 *
	 * <p><b>GETTER MENULIS BALIK, termasuk MENGOSONGKAN:</b> bila tidak ada pelunasan atau
	 * pelunasan itu belum tertaut ke kuitansi, field <b>di-set {@code null}</b> — bukan
	 * sekadar tidak diisi. Digabung dengan efek destruktif
	 * {@link #getPembayaranSiswaDetail()} yang dipanggilnya di baris pertama, tanggal bayar
	 * yang tersimpan dapat terhapus bersamaan dengan putusnya relasi pelunasan.</p>
	 *
	 * <p>Bedakan dari {@link #getPembayaranPada()} yang mengambil
	 * <code>PembayaranSiswa.getTanggal()</code> (tanggal dokumen/entri) alih-alih tanggal
	 * bayar, dan yang <b>tidak</b> mengosongkan nilainya.</p>
	 *
	 * @return tanggal pembayaran diterima, atau {@code null} bila belum dibayar
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalBayar() {
		if (getPembayaranSiswaDetail() != null && pembayaranSiswaDetail.getPembayaranSiswa() != null) {
			tanggalBayar = pembayaranSiswaDetail.getPembayaranSiswa().getTanggalBayar();
		} else
			tanggalBayar = null;
		return tanggalBayar;
	}

	/**
	 * @param tanggalBayar tanggal pembayaran; akan dihitung ulang (atau dikosongkan) oleh
	 *                     {@link #getTanggalBayar()}
	 */
	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	/**
	 * Nominal angsuran yang <b>ditentukan manual</b> oleh petugas saat memecah kewajiban
	 * menjadi cicilan — misalnya "cicilan pertama 2 juta, sisanya menyusul".
	 * <p>
	 * Pembaca murni dengan normalisasi {@code null} &rarr; 0,0, sehingga aman dibandingkan
	 * langsung dengan ambang <code>0,01</code> yang dipakai di seluruh kelas ini untuk
	 * membedakan "terisi" dari "nol".
	 * <p>
	 * Bila terisi, nilai ini <b>mengalahkan</b> hasil hitung tarif pada beberapa cabang
	 * {@link #getNominal()} dan {@link #ambilNominal()} — khususnya untuk komponen dicicil
	 * non-"Bulanan" dan untuk paket biaya "khusus buat siswa tertentu".
	 *
	 * @return nominal angsuran manual, 0,0 bila tidak ditentukan
	 */
	public Double getDibayarManual() {
		return dibayarManual == null ? 0.0 : dibayarManual;
	}

	/**
	 * Menyetel nominal angsuran manual.
	 * <p>
	 * Disinkronkan otomatis oleh {@link #ambilAtauBuat} bila pemanggil mengoper nilai baru
	 * yang berbeda — dengan UPDATE dan commit tersendiri, di luar transaksi pemanggil.
	 *
	 * @param dibayarManual nominal angsuran yang ditentukan petugas
	 */
	public void setDibayarManual(Double dibayarManual) {
		this.dibayarManual = dibayarManual;
	}


	/**
	 * <b>Cadangan permanen</b> {@link #getDiskonSiswa()} — merekam diskon apa yang pernah
	 * diberikan, agar jejaknya tidak ikut hilang ketika getter diskon utama mengosongkan
	 * relasinya (untuk angsuran ke-2+ atau setelah masa berlaku habis).
	 *
	 * <p><b>GETTER MENULIS BALIK, dua arah:</b> bila kolom cadangan kosong sementara
	 * {@link #diskonSiswa} terisi, nilai itu <b>disalin</b> ke sini. Jadi kedua kolom saling
	 * memulihkan.</p>
	 *
	 * <p><b>Namun pemulihannya bergantung urutan.</b> {@link #getDiskonSiswa()} berjalan
	 * lebih dahulu di hampir semua alur perhitungan, dan ia mengosongkan
	 * {@link #diskonSiswa}. Bila pada saat itu cadangan belum pernah terisi — misalnya
	 * karena diskon disetel lewat jalur yang tidak memanggil
	 * {@link #setDiskonSiswa(DiskonSiswa)} — maka ketika getter ini akhirnya dipanggil,
	 * tidak ada lagi yang bisa disalin. Cadangan ini karena itu bukan jaminan mutlak.</p>
	 *
	 * @return diskon asli yang pernah diberikan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon_siswa_asli", nullable = true)
	public DiskonSiswa getDiskonSiswaAsli() {
		// LAZY_GETTER_CHECK_EXCEPTION: resolver khusus menjaga pasangan diskon utama/cadangan.
		diskonSiswaAsli = resolveDiskonSiswaForRead(diskonSiswaAsli);
		if (diskonSiswaAsli == null) {
			diskonSiswa = resolveDiskonSiswaForRead(diskonSiswa);
			if (diskonSiswa != null) {
				diskonSiswaAsli = diskonSiswa;
			}
		}
		return diskonSiswaAsli;
	}

	/**
	 * Menyetel cadangan diskon secara langsung. Umumnya <b>tidak</b> dipanggil sendiri —
	 * {@link #setDiskonSiswa(DiskonSiswa)} sudah memanggilnya sehingga kedua kolom terisi
	 * bersamaan.
	 *
	 * @param diskonSiswaAsli diskon asli yang dicatat sebagai cadangan
	 */
	public void setDiskonSiswaAsli(DiskonSiswa diskonSiswaAsli) {
		this.diskonSiswaAsli = diskonSiswaAsli;
	}

	/**
	 * Nominal yang ditetapkan manual oleh petugas untuk <b>menggantikan</b> hasil hitung
	 * tarif — berbeda dari {@link #getDibayarManual()} yang mengatur besaran satu angsuran.
	 * <p>
	 * Pembaca murni tanpa normalisasi: dapat mengembalikan {@code null}. Seluruh pemakaiannya
	 * di kelas ini karena itu selalu memeriksa {@code null} lebih dahulu.
	 * <p>
	 * Dipakai {@link #getNominal()} dan {@link #ambilNominal()} pada kondisi "nominal manual
	 * terisi TETAPI angsuran manual tidak" — jadi bila keduanya diisi, angsuran manual yang
	 * menang.
	 *
	 * @return nominal manual, atau {@code null} bila tidak ditetapkan
	 */
	public Double getNominalManual() {
		return nominalManual;
	}

	/**
	 * @param nominalManual nominal yang ditetapkan petugas untuk menggantikan hasil hitung tarif
	 */
	public void setNominalManual(Double nominalManual) {
		this.nominalManual = nominalManual;
	}

	/**
	 * Membuka dialog ZK <b>"Pindahkan Pembayaran"</b>: memindahkan satu baris pelunasan yang
	 * sudah tercatat dari tagihan ini ke tagihan lain milik siswa/calon siswa yang sama.
	 * Dipakai untuk memperbaiki uang yang telanjur tercatat pada komponen atau periode yang
	 * keliru, tanpa membatalkan kuitansinya.
	 *
	 * <h4>Isi dialog</h4>
	 * Menampilkan nama komponen biaya, nilai tagihan bersih
	 * (<code>denda + nominal - {@link #ambilDiskonTanpaDikonBayarSatuKali()}</code>), dan
	 * sebuah combobox tujuan.
	 *
	 * <h4>Penyaringan tagihan tujuan</h4>
	 * Combobox diisi tagihan milik pihak yang sama
	 * (<code>calonSiswa</code> bila ada, kalau tidak <code>siswa</code>) yang berstatus
	 * aktif DAN belum terbayar — yaitu belum punya <code>pembayaranSiswaDetail</code>, atau
	 * masa <code>pembayaran_berakhir_pada</code>-nya sudah lewat. Pembatas
	 * "komponen biaya harus sama" sengaja <b>dikomentari</b> di dalam kode, sehingga
	 * pemindahan lintas komponen diizinkan.
	 *
	 * <h4>Aksi Simpan</h4>
	 * Dalam satu transaksi: kedua tagihan dan baris pelunasan di-<code>refresh</code>;
	 * pelunasan dialihkan ke tagihan tujuan beserta <code>nominalBiaya</code>-nya; tagihan
	 * asal dilepas dari pelunasan; ketiganya disimpan; lalu <i>listener</i> pemanggil
	 * dipicu untuk menyegarkan layar. Kegagalan memicu <code>rollback</code> dan hanya
	 * dicatat — tidak ada pesan ke pengguna, sehingga <b>kegagalan tampak seperti
	 * keberhasilan</b> sampai layar disegarkan.
	 *
	 * <h4>Catatan hak akses</h4>
	 * Method ini <b>tidak memeriksa hak apa pun</b>: tidak ada
	 * <code>checkPrevilages</code>, dan tidak ada verifikasi bahwa pengguna berwenang atas
	 * siswa yang bersangkutan. Gerbangnya sepenuhnya bergantung pada layar pemanggil —
	 * yang, untuk keluarga helper tagihan, juga tidak memilikinya (lihat Javadoc kelas).
	 * Pemindahan uang antar-tagihan karena itu berjalan dengan hak menu induk saja.
	 *
	 * @param tag           tagihan asal yang pembayarannya hendak dipindahkan
	 * @param eventListener aksi yang dijalankan setelah pemindahan berhasil (penyegaran layar)
	 * @throws Exception bila komponen dialog gagal dibangun atau dilekatkan ke halaman
	 */
	public static void pindahkan(final Tagihan tag, final EventListener eventListener) throws Exception {
		final MyWindow addWindow = new MyWindow("Pindahkan Pembayaran", "none", false);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setHeight("400px");
		addWindow.setWidth("450px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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

		MyFormRow r = new MyFormRow();
		r.setValign("top");
		r.setParent(rows);
		r.appendChild(new ais.ui.util.MyLabelConfig("Biaya"));
		r.appendChild(new MyLabelBoldAja(tag.getItemBiayaSekolah().getNama()));

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new ais.ui.util.MyLabelConfig("Tagihan"));
		Double taga = (tag.getDenda() + tag.getNominal()) - tag.ambilDiskonTanpaDikonBayarSatuKali();
		r.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(taga)));

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new ais.ui.util.MyLabelConfig("Pindahkan ke *"));
		final Combobox dibayar = new Combobox();
		r.appendChild(dibayar);
		dibayar.setWidth("90%");

		Criterion criterion = tag.getCalonSiswa() != null ? Restrictions.eq("calonSiswa", tag.getCalonSiswa())
				: Restrictions.eq("siswa", tag.getSiswa());
//		criterion = Restrictions.and(criterion, Restrictions.eq("itemBiayaSekolah", tag.getItemBiayaSekolah()));
		criterion = Restrictions
				.and(criterion,
						Restrictions.or(
								Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
										Restrictions
												.sqlRestriction("this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
								Restrictions.isNull("pembayaranSiswaDetail")));
		criterion = Restrictions.and(criterion, Restrictions.eq("aktif", true));
		Common.insertCombo(dibayar, new String[] { "itemBiayaSekolah", "pengaturanBiaya", "nominal", "tahunbulan" },
				"tahunAjaran", Tagihan.class, criterion);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (dibayar.getSelectedItem() == null || dibayar.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Tujuan tagihan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				addWindow.detach();
				Session session = null;
				Transaction tx = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					tx = session.beginTransaction();

					Tagihan tagihanLama = (Tagihan) tag;
					Tagihan tagihanPilih = (Tagihan) dibayar.getSelectedItem().getValue();
					session.refresh(tagihanLama);
					session.refresh(tagihanPilih);

					PembayaranSiswaDetail pembayaranSiswaDetail = tagihanLama.getPembayaranSiswaDetail();
					session.refresh(pembayaranSiswaDetail);
					pembayaranSiswaDetail.setTagihan(tagihanPilih);
					pembayaranSiswaDetail.setNominalBiaya(tagihanPilih.getNominalBiaya());

					tagihanLama.setPembayaranSiswaDetailTrue(null);
					tagihanLama.setPembayaranSiswaDetail(null);
					tagihanPilih.setPembayaranSiswaDetail(pembayaranSiswaDetail);

					Common.refreshUpdate(session, tagihanLama);
					Common.refreshUpdate(session, pembayaranSiswaDetail);
					Common.refreshUpdate(session, tagihanPilih);

					tx.commit();
					Common.createDefaultTimer(eventListener);
				} catch (Exception e) {
					if (tx != null)
						tx.rollback();
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:2138");
				} finally {
					if (session != null && session.isOpen()) {
						try {
							session.close();
						} catch (Exception ex) {
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/database/model/sekolah/Tagihan.java:2144");
						}
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	/**
	 * Batas berlakunya pembayaran untuk pola <b>langganan/subscription</b> — misalnya
	 * layanan yang otomatis tertagih kembali setelah sekian hari.
	 *
	 * <p><b>GETTER MENULIS BALIK, dua arah:</b></p>
	 * <ul>
	 *   <li>bila paket biaya ber-flag
	 *       <code>otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion</code> dan
	 *       {@link #getPembayaranPada()} sudah terisi &rarr; nilainya dihitung sebagai
	 *       tanggal bayar + <code>jumlahHariPenagihanBerikutnya</code> hari;</li>
	 *   <li>bila paket <b>tidak</b> ber-flag tersebut &rarr; field dipaksa {@code null}.
	 *       Mematikan opsi langganan pada sebuah paket biaya karena itu <b>menghapus
	 *       permanen</b> seluruh tanggal berakhir yang sudah tercatat pada tagihan-tagihan
	 *       di bawahnya, begitu barisnya tersentuh.</li>
	 * </ul>
	 *
	 * <p><b>Kuirk aritmetika tanggal:</b> penambahan hari dilakukan dengan
	 * <code>calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + n)</code>, bukan
	 * <code>add(...)</code>. Bentuk <code>set</code> tetap benar di sini karena
	 * {@link Calendar} bersifat <i>lenient</i> dan meluapkan tanggal ke bulan berikutnya,
	 * tetapi <code>add</code> adalah bentuk yang lazim dan lebih jelas maksudnya.</p>
	 *
	 * <p>Kolom ini juga dipakai sebagai kriteria "tagihan yang masih boleh menjadi tujuan
	 * pemindahan" di {@link #pindahkan(Tagihan, EventListener)} dan sebagai penyaring
	 * tagihan yang belum lunas di <code>/Api TagihanSiswa.va()</code>.</p>
	 *
	 * @return batas berlakunya pembayaran, atau {@code null} bila bukan pola langganan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "pembayaran_berakhir_pada")
	public Date getPembayaranBerakhirPada() {
		if (getPengaturanBiaya() != null
				&& Boolean.TRUE
						.equals(getPengaturanBiaya().getOtomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion())
				&& pembayaranPada != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(pembayaranPada);
			int hariPenagihan = getPengaturanBiaya().getJumlahHariPenagihanBerikutnya() != null
					? getPengaturanBiaya().getJumlahHariPenagihanBerikutnya()
					: 0;
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + hariPenagihan);
			pembayaranBerakhirPada = calendar.getTime();
		}

		if (getPengaturanBiaya() != null && !Boolean.TRUE
				.equals(getPengaturanBiaya().getOtomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion())) {
			pembayaranBerakhirPada = null;
		}
		return pembayaranBerakhirPada;
	}

	/**
	 * @param pembayaranBerakhirPada batas berlakunya pembayaran; akan dihitung ulang atau
	 *                               dikosongkan oleh {@link #getPembayaranBerakhirPada()}
	 */
	public void setPembayaranBerakhirPada(Date pembayaranBerakhirPada) {
		this.pembayaranBerakhirPada = pembayaranBerakhirPada;
	}

	/**
	 * Waktu pembayaran menurut <b>tanggal dokumen</b> kuitansi
	 * (<code>PembayaranSiswa.getTanggal()</code>) — menjadi titik awal perhitungan
	 * {@link #getPembayaranBerakhirPada()} pada pola langganan.
	 *
	 * <p><b>GETTER MENULIS BALIK</b>, tetapi berbeda dari {@link #getTanggalBayar()} ia
	 * <b>tidak mengosongkan</b> nilai lama ketika pelunasan tidak ditemukan — nilai
	 * tersimpan dipertahankan.</p>
	 *
	 * <p>Bedakan ketiganya: {@link #getTanggalTagihan()} = kapan ditagihkan;
	 * {@link #getTanggalBayar()} = tanggal uang diterima; method ini = tanggal dokumen
	 * kuitansi.</p>
	 *
	 * @return waktu pembayaran menurut dokumen, atau {@code null} bila belum ada
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getPembayaranPada() {
		if (getPembayaranSiswaDetail() != null && getPembayaranSiswaDetail().getPembayaranSiswa() != null) {
			pembayaranPada = getPembayaranSiswaDetail().getPembayaranSiswa().getTanggal();
		}
		return pembayaranPada;
	}

	/**
	 * @param pembayaranPada waktu pembayaran menurut dokumen kuitansi
	 */
	public void setPembayaranPada(Date pembayaranPada) {
		this.pembayaranPada = pembayaranPada;
	}

	/**
	 * Potongan yang ditetapkan <b>manual</b> oleh petugas, menggantikan seluruh perhitungan
	 * diskon otomatis.
	 * <p>
	 * Pembaca murni tanpa normalisasi ({@code null} mungkin). Di {@link #getDiskon()} nilai
	 * ini dipakai bila lebih besar dari 0,1, tetapi hanya setelah dua cabang "sudah dibayar"
	 * tidak terpenuhi — jadi ia tidak mengalahkan diskon yang sudah terwujud sebagai selisih
	 * pembayaran nyata.
	 *
	 * @return besaran potongan manual, atau {@code null} bila tidak ditetapkan
	 */
	public Double getDiskonManual() {
		return diskonManual;
	}

	/**
	 * @param diskonManual besaran potongan yang ditetapkan petugas
	 */
	public void setDiskonManual(Double diskonManual) {
		this.diskonManual = diskonManual;
	}

	// =========================================================================================
	// OPTIMISASI: finally block untuk mencegah memory/connection leak
	// =========================================================================================
	/**
	 * Mencari tagihan yang sesuai untuk sebuah kombinasi siswa/komponen/periode, dipakai
	 * layar dan laporan yang perlu "menempelkan" tagihan ke baris yang sedang dirender.
	 *
	 * <p><b>Bila argumen <code>tagihan</code> sudah terisi, method langsung
	 * mengembalikannya</b> tanpa menyentuh database sama sekali — bentuk memoisasi yang
	 * membuatnya murah dipanggil di dalam perulangan render.</p>
	 *
	 * <p>Meski namanya mengandung "buat", method ini <b>tidak pernah membuat baris tagihan
	 * baru</b>. Untuk itu pakai {@link #ambilAtauBuat}. Satu-satunya penulisan yang
	 * dilakukannya adalah menyinkronkan {@link #setDibayarManual(Double)} pada jalur
	 * "khusus siswa" (lihat di bawah).</p>
	 *
	 * <h4>Dua jalur pencarian</h4>
	 * <ol>
	 *   <li><b>Paket "khusus buat siswa tertentu".</b> Konfigurasi komponen
	 *       (<code>PengaturanBiayaItemBiaya</code>) dicari lebih dahulu — hanya untuk
	 *       komponen yang aktif atau yang bendera aktifnya belum diisi. Bila periodenya
	 *       "Bulanan"/"Insidentil", tarif diambil lewat
	 *       <code>TagihanUtil.ambilNominalBiaya(...)</code>, kunci identitas dihitung
	 *       {@link #genCode}, lalu tagihan dicari dengan {@link #findByKodeUnik}. Bila
	 *       ketemu dan <code>nominal</code> yang dioper berbeda, <b>nilai angsuran manual
	 *       diperbarui dan di-commit</b> — inilah satu-satunya efek tulis method ini.</li>
	 *   <li><b>Paket biasa.</b> Pencarian langsung ke tabel tagihan dengan kriteria: paket
	 *       biaya tidak null, tahun ajaran sama, dan identitas pihak tertagih. Bila
	 *       <b>tidak ada</b> siswa maupun calon siswa, ditambahkan
	 *       <code>sqlRestriction("false")</code> sehingga hasilnya dijamin kosong — penjaga
	 *       yang tepat, karena tanpa itu query akan mengembalikan tagihan milik siswa
	 *       sembarang. Komponen biaya dan periode disaring bila diberikan; bila periode
	 *       kosong, dipakai <code>sqlRestriction("true")</code> sebagai penanda "tanpa
	 *       filter". Diambil id terbesar (paling baru).</li>
	 * </ol>
	 *
	 * <p><b>Pemulihan kegagalan.</b> Bila jalur di atas melempar, method membuka <i>sesi
	 * kedua</i> dan mencoba sekali lagi lewat <code>kode_unik</code> saja. Kedua sesi
	 * ditutup di blok <code>finally</code> masing-masing.</p>
	 *
	 * <p>Parameter <code>tahunbulan</code> bertipe {@link String} (bukan {@link Integer}
	 * seperti di tempat lain) karena nilainya datang langsung dari kotak isian layar;
	 * konversinya dijaga <code>Common.isNumber(...)</code>.</p>
	 *
	 * @param tagihan          tagihan yang sudah diketahui; bila tidak {@code null}
	 *                         langsung dikembalikan
	 * @param pengaturanBiaya  paket biaya yang menaungi
	 * @param siswa            siswa yang ditagih, atau {@code null}
	 * @param calonSiswa       calon siswa yang ditagih, atau {@code null}
	 * @param itemBiayaSekolah komponen biaya yang dicari, atau {@code null} untuk semua
	 * @param tahunbulan       periode YYYYMM sebagai teks; kosong/non-numerik berarti tanpa filter
	 * @param nominal          nominal angsuran manual yang hendak disinkronkan (jalur "khusus siswa")
	 * @param tahunAjaran      tahun ajaran "YYYY/YYYY" sebagai penyaring
	 * @param bayarke          nomor angsuran, dipakai membentuk kunci identitas
	 * @return tagihan yang ditemukan, atau {@code null} bila tidak ada
	 */
	public static Tagihan buatAtauLoadTagihan(Tagihan tagihan, PengaturanBiaya pengaturanBiaya, Siswa siswa,
			CalonSiswa calonSiswa, ItemBiayaSekolah itemBiayaSekolah, String tahunbulan, Double nominal,
			String tahunAjaran, Integer bayarke) {

		if (tagihan != null)
			return tagihan;

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			boolean isKhususSiswa = pengaturanBiaya != null && pengaturanBiaya.getId() != null
					&& Boolean.TRUE.equals(pengaturanBiaya.getKhususBuatSiswaTertentu());

			if (isKhususSiswa) {
				Criteria critItem = session.createCriteria(PengaturanBiayaItemBiaya.class)
						.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
						.add(Restrictions.or(Restrictions.isNull("itemBiayaSekolah.aktif"),
								Restrictions.eq("itemBiayaSekolah.aktif", true)))
						.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))
						.add(Restrictions.eq("itemBiayaSekolah", itemBiayaSekolah)).setMaxResults(1);

				PengaturanBiayaItemBiaya pbi = (PengaturanBiayaItemBiaya) ConstantValues.simpleObject(critItem,
						PengaturanBiayaItemBiaya.class);

				if (pbi != null && pbi.getId() != null && pengaturanBiaya.getJenisBiayaSekolah() != null) {
					String periode = pengaturanBiaya.getJenisBiayaSekolah().getPeriode();
					if ("Bulanan".equalsIgnoreCase(periode) || "Insidentil".equalsIgnoreCase(periode)) {
						NominalBiaya nominalBiaya = TagihanUtil.ambilNominalBiaya(pbi, siswa, session);

						if (nominalBiaya != null) {
							Integer thnBln = (tahunbulan == null || !Common.isNumber(tahunbulan)) ? null
									: Integer.parseInt(tahunbulan);
							String kodeUnik = Tagihan.genCode(itemBiayaSekolah, pengaturanBiaya, thnBln, siswa,
									calonSiswa, bayarke);
							tagihan = Tagihan.findByKodeUnik(kodeUnik, session);

							if (tagihan != null && tagihan.getId() != null && nominal != null && nominal > 0.1) {
								if (tagihan.getDibayarManual() == null
										|| tagihan.getDibayarManual().intValue() != nominal.intValue()) {
									tagihan.setDibayarManual(nominal);
									Transaction tx = null;
									try {
										tx = session.beginTransaction();
										Common.refreshUpdate(session, tagihan);
										tx.commit();
									} catch (Exception ex) {
										if (tx != null)
											tx.rollback();
										throw ex;
									}
								}
							}
						}
					}
				}
			} else {
				Criteria critTagihan = session.createCriteria(Tagihan.class)
						.add(Restrictions.isNotNull("pengaturanBiaya"))
						.add(Restrictions.eq("tahunAjaran", tahunAjaran));

				if (calonSiswa != null && calonSiswa.getId() != null)
					critTagihan.add(Restrictions.eq("calonSiswa.id", calonSiswa.getId()));
				else if (siswa != null && siswa.getId() != null)
					critTagihan.add(Restrictions.eq("siswa.id", siswa.getId()));
				else
					critTagihan.add(Restrictions.sqlRestriction("false"));

				if (itemBiayaSekolah != null && itemBiayaSekolah.getId() != null)
					critTagihan.add(Restrictions.eq("itemBiayaSekolah.id", itemBiayaSekolah.getId()));

				if (tahunbulan != null && !tahunbulan.isEmpty() && Common.isNumber(tahunbulan)) {
					critTagihan.add(Restrictions.eq("tahunbulan", Integer.parseInt(tahunbulan)));
				} else {
					critTagihan.add(Restrictions.sqlRestriction("true"));
				}

				critTagihan.addOrder(Order.desc("id")).setMaxResults(1);
				tagihan = (Tagihan) critTagihan.uniqueResult();
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:2287");
			Session sessionBaru = null;
			try {
				sessionBaru = HibernateUtil.getSessionFactory().openSession();
				Integer thnBln = (tahunbulan == null || !Common.isNumber(tahunbulan)) ? null
						: Integer.parseInt(tahunbulan);
				String kodeUnik = Tagihan.genCode(itemBiayaSekolah, pengaturanBiaya, thnBln, siswa, calonSiswa,
						bayarke);

				Tagihan tagihanBaru = (Tagihan) sessionBaru.createCriteria(Tagihan.class)
						.add(Restrictions.eq("kodeUnik", kodeUnik)).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();
				if (tagihanBaru != null)
					tagihan = tagihanBaru;
			} catch (Exception ea) {
				ea.printStackTrace(); ais.common.ErrorAuditUtil.record(ea, "auto-audit src/ais/database/model/sekolah/Tagihan.java:2302");
			} finally {
				if (sessionBaru != null && sessionBaru.isOpen()) {
					try {
						sessionBaru.close();
					} catch (Exception ex) {
						ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/database/model/sekolah/Tagihan.java:2308");
					}
				}
			}

		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/Tagihan.java:2318");
				}
			}
		}

		return tagihan;
	}
}
