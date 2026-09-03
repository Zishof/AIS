package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.List;
import java.util.TreeSet;

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

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.employ.TipeMasaKerja;

/**
 * Entity <b>INDUK</b> sebuah aturan diskon siswa &mdash; "potongan biaya apa yang berlaku, untuk
 * tahun ajaran dan sekolah mana, dengan cara hitung apa, dan selama rentang tanggal berapa".
 *
 * <p>Tabel: {@code sekolah.diskon_siswa}. Satu baris kelas ini adalah <i>kepala</i> sebuah
 * kebijakan diskon; ia sendiri <b>tidak</b> menyimpan angka potongan per item biaya maupun daftar
 * penerimanya. Keduanya berada di dua entity anak, dan seluruh dampak rupiahnya baru terwujud saat
 * mesin sinkronisasi menuliskannya ke baris {@link Tagihan}:</p>
 *
 * <pre>
 * DiskonSiswa (KELAS INI)  &mdash; nama, jenis, tahun ajaran, sekolah, masa berlaku, persen/nominal,
 *   |                        saklar "memotong tagihan", saklar "aktif"
 *   |
 *   +-- DiskonSiswaItemBiaya   --&gt; ItemBiayaSekolah      "APA yang didiskon + BERAPA besarnya"
 *   |
 *   +-- DiskonSiswaPunyaSiswa  --&gt; Siswa / CalonSiswa    "SIAPA yang menerima" (+ flag setujui)
 *                    |
 *                    v
 *   DiskonSiswaSyncHelper.sinkronkan(..)  /  TagihanDiskonSiswaHelper.sinkronkanDiskon(..)
 *                    |
 *                    v
 *   Tagihan.diskon , Tagihan.diskonTidakLangsung , Tagihan.diskonSiswa   (RUPIAH NYATA)
 *                    |
 *                    v
 *   DaftarPengajuanTransfer.simpanDiskonPembayaran(..)   (bila diskon TIDAK memotong tagihan)
 * </pre>
 *
 * <p>Rincian sisi "apa yang didiskon" didokumentasikan pada
 * {@link ais.database.model.sekolah.DiskonSiswaItemBiaya} (termasuk semantik ganda
 * persen-atau-rupiah pada {@code defaultBiaya} yang ditentukan oleh saklar
 * {@link #getMenggunkanPersen()} milik kelas ini), dan sisi "siapa penerimanya" pada
 * {@link ais.database.model.sekolah.DiskonSiswaPunyaSiswa}.</p>
 *
 * <h2>Pengelompokan anggota kelas ini</h2>
 * <ol>
 *   <li><b>Identitas &amp; cakupan</b> &mdash; {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #getSekolah()}, {@link #getYayasan()}, {@link #getTahunAjaran()},
 *       {@link #getDiskonMulai()}/{@link #getDiskonSampai()}.</li>
 *   <li><b>Saklar perilaku</b> &mdash; {@link #getAktif()} (dibaca mesin billing sebagai gerbang),
 *       {@link #getMenggunkanPersen()} (persen vs rupiah), {@link #getMemotongTagihan()}
 *       (memotong tagihan langsung, atau justru dikirim ke alur pembayaran/transfer),
 *       {@link #getJenis()} (memilih mesin pencari penerima otomatis).</li>
 *   <li><b>Cache denormalisasi</b> &mdash; {@link #getItemBiaya()}, daftar id
 *       {@link ItemBiayaSekolah} dalam bentuk CSV. Lihat bagian khusus di bawah.</li>
 *   <li><b>Enam mesin statis pencari penerima</b> &mdash; {@link #prosesAlumni(DiskonSiswa)},
 *       {@link #prosesAnakAlumni(DiskonSiswa)}, {@link #prosesSaudara(DiskonSiswa)},
 *       {@link #prosesSaudaraAlumni(DiskonSiswa)},
 *       {@link #prosesAnakPegawai(DiskonSiswa, TipeMasaKerja)}, dan
 *       {@link #prosesSemua(DiskonSiswa)}. Ini satu-satunya bagian file yang berisi logika bisnis
 *       nyata; semuanya menulis baris {@link DiskonSiswaPunyaSiswa} ke database.</li>
 *   <li><b>Jejak audit warisan {@code GeneralValueObject}</b> &mdash; {@code oleh}, {@code olehId},
 *       {@code tanggal_dirubah}, {@code id}. Lihat catatan pewarisan di bawah.</li>
 * </ol>
 *
 * <h2>Katalog {@link #JENIS}: jenis diskon menentukan mesin pencari penerima</h2>
 *
 * <p>Tujuh konstanta {@code DISKON_*} pada kelas ini bukan sekadar label. Nilai {@link #getJenis()}
 * dibandingkan dengan {@code String.equals(..)} di {@code DiskonSiswaPunyaSiswaHelper} untuk
 * memilih mesin mana yang dijalankan tombol <b>"Singkronkan Data &lt;jenis&gt;"</b>. Karena
 * pencocokannya berbasis teks persis, <b>mengubah nilai literal konstanta ini akan memutus
 * baris-baris {@code diskon_siswa} lama yang sudah tersimpan di database</b> (kolom {@code jenis}
 * menyimpan teksnya, bukan kode). Nilai {@code null}/kosong berarti "Tanpa Jenis Diskon": tombol
 * sinkronisasi otomatis tidak dirender sama sekali dan penerima harus dipilih manual.</p>
 *
 * <h2>{@code itemBiaya}: cache CSV yang formatnya WAJIB berkoma di ujung</h2>
 *
 * <p>{@link #getItemBiaya()} bukan getter biasa. Nilai yang tersimpan adalah daftar id
 * {@link ItemBiayaSekolah} dipisah koma, dan getter ini <b>menormalkannya menjadi bentuk
 * berpembungkus koma</b> ({@code ,12,34,}) sekaligus <b>menulis balik hasilnya ke field</b>.
 * Bentuk itu bukan kosmetik: {@code TagihanDiskonSiswaHelper.hitungDiskon(..)} mencari aturan
 * diskon yang berlaku atas sebuah item biaya dengan
 * {@code Restrictions.ilike("diskonSiswa.itemBiaya", "," + itemBiayaId + ",", ANYWHERE)}. Tanpa
 * koma pembungkus, item <i>pertama</i> dan <i>terakhir</i> pada daftar tidak akan pernah cocok
 * (dan {@code ,1,} juga tidak boleh salah cocok dengan {@code ,11,} &mdash; itulah alasan koma di
 * kedua sisi). Sumber kebenaran sesungguhnya tetap tabel
 * {@link ais.database.model.sekolah.DiskonSiswaItemBiaya}; kolom ini hanya salinan cepat untuk
 * kebutuhan pencarian.</p>
 *
 * <h2>Pemakai nyata yang terverifikasi</h2>
 * <ul>
 *   <li><b>{@code DiskonSiswaAction}</b> &mdash; layar master "Konfigurasi Diskon"
 *       ({@code /pages/master/sekolah/diskon_siswa.zul}, menu id {@code 11098051}). Satu-satunya
 *       layar CRUD entity ini; panel rinciannya menyisipkan
 *       {@code DiskonSiswaPunyaSiswaHelper}.</li>
 *   <li><b>{@code DiskonSiswaSyncHelper}</b> &mdash; mesin yang benar-benar menulis potongan ke
 *       {@link Tagihan}, dipanggil dua tombol "Singkronkan Tagihan" (layar master dan panel
 *       rincian) serta tombol "Kirimkan Diskon Ke Pembayaran".</li>
 *   <li><b>{@code TagihanDiskonSiswaHelper}</b> &mdash; jalur otomatis: dipanggil
 *       {@code TagihanUtil}/{@code TagihanUtilCalonSiswa} saat tagihan baru dibangkitkan.</li>
 *   <li><b>{@code PostingUtangDiskonSiswaAction}</b> &mdash; posting jurnal utang diskon
 *       ({@code posting_utang_diskon.zul}); {@code PostingJurnalHelper.REF_DISKON_SISWA}.</li>
 *   <li><b>{@code DaftarPengajuanTransfer.simpanDiskonPembayaran(..)}</b> &mdash; membaca
 *       {@link #getMemotongTagihan()} untuk memutuskan apakah nilai diskon dibelokkan menjadi
 *       pengajuan transfer/pembayaran alih-alih memotong tagihan.</li>
 *   <li><b>{@code Bniresponse}/{@code Bsiresponse}/{@code Briresponse}/{@code MncBank}</b>,
 *       {@code TagihanSiswa} (JAX-RS {@code /Api}), {@code PembayaranOnline},
 *       {@code RekapPembayaran}, {@code CommonReportHelper}, {@code PembayaranSiswaUtil}
 *       &mdash; membaca nama/nilai diskon lewat {@code Tagihan.getDiskonSiswa()} saat menyusun
 *       tagihan, struk, dan respons kanal bank.</li>
 *   <li>Terdaftar resmi di {@code hibernate.cfg.xml}, jadi tabelnya memang dikelola Hibernate.</li>
 * </ul>
 *
 * <h2>Catatan pewarisan: mengapa {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 * dideklarasikan ulang di sini</h2>
 *
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti apa pun miliknya. Karena itu deklarasi ulang keempat anggota tersebut pada kelas ini
 * <b>bukan bug atau duplikasi ceroboh, melainkan keharusan teknis</b>: tanpa itu tidak ada kolom
 * {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang terpetakan. Lihat
 * {@link ais.database.model.GeneralValueObject} untuk uraian lengkapnya.</p>
 *
 * <h2>Penamaan kolom &amp; getter yang "menulis balik" saat flush</h2>
 *
 * <p>Kelas ini memakai <b>property access</b> (anotasi JPA menempel pada getter), sehingga
 * <b>Hibernate sendiri memanggil getter saat dirty-check dan saat menyusun INSERT/UPDATE</b>.
 * Akibatnya setiap getter yang mengubah state atau mengarang nilai pengganti untuk {@code null}
 * berpotensi <b>mengubah isi database</b> begitu baris tersentuh, tanpa ada tombol Simpan yang
 * ditekan pengguna:</p>
 * <ul>
 *   <li>{@link #getItemBiaya()} &mdash; menulis balik bentuk ternormalisasi ke field;</li>
 *   <li>{@link #getYayasan()} &mdash; <b>menimpa</b> yayasan dengan {@code getSekolah().getYayasan()}
 *       setiap kali dibaca;</li>
 *   <li>{@link #getTahunAjaran()} &mdash; {@code null} berubah menjadi tahun akademik berjalan,
 *       sehingga aturan diskon yang sengaja disimpan sebagai "berlaku semua tahun ajaran" bisa
 *       terkunci diam-diam ke satu tahun ajaran;</li>
 *   <li>{@link #getAktif()}, {@link #getMenggunkanPersen()}, {@link #getMemotongTagihan()}
 *       &mdash; {@code null} berubah menjadi {@code true}.</li>
 * </ul>
 * <p>Kolom yang tidak ber-{@code @Column} memakai {@code ais.database.hibernate.MyNamingStrategy}
 * (turunan {@code DefaultNamingStrategy}: nama kolom = nama properti apa adanya, tanpa konversi ke
 * {@code snake_case}), jadi kolomnya benar-benar bernama {@code tahunAjaran},
 * {@code menggunkanPersen}, {@code memotongTagihan}, {@code diskonMulai}, {@code diskonSampai},
 * dan {@code jenis} &mdash; termasuk salah eja {@code menggunkanPersen} (seharusnya
 * "menggunakan"), yang karena itu <b>tidak boleh diperbaiki tanpa migrasi kolom</b>.</p>
 *
 * <h2>Kuirk, bug, dan risiko yang ditemukan saat mendokumentasikan (di luar file ini, tetapi
 * menentukan perilakunya)</h2>
 * <ul>
 *   <li><b>Gerbang hak akses layar master ini SEHAT, gerbang panel rinciannya TIDAK.</b>
 *       {@code DiskonSiswaAction.doAfterCompose()} memasang {@code CommonPrivilages.checkPrevilages}
 *       untuk CREATE/UPDATE/DELETE dan benar-benar memakainya (tombol Tambah, checkbox "Aktif" dan
 *       "Memotong Tagihan", tombol Ubah/Hapus, bahkan tombol Upload Excel yang menuntut
 *       {@code add &amp;&amp; edit &amp;&amp; delete} sekaligus). Namun tombol
 *       <b>"Singkronkan Tagihan"</b> pada layar yang sama <b>tidak memeriksa hak apa pun</b>, dan
 *       seluruh tombol pada panel rincian {@code DiskonSiswaPunyaSiswaHelper} ("Ambil Siswa",
 *       "Ambil Calon Siswa", "Singkronkan Data &lt;jenis&gt;", "Singkronkan Tagihan", "Kirimkan
 *       Diskon Ke Pembayaran", "Bersihkan") juga tidak. Rinciannya di
 *       {@link ais.database.model.sekolah.DiskonSiswaPunyaSiswa} dan pada Javadoc masing-masing
 *       method {@code proses*} di bawah.</li>
 *   <li><b>Cakupan tenant fail-open.</b> {@code DiskonSiswaAction.initCriteria(..)} memakai
 *       {@code Restrictions.sqlRestriction("1=1")} bila combo Yayasan/Sekolah belum dipilih.
 *       Keadaan awal layar memang belum memilih apa pun, sehingga daftar (dan karena itu juga
 *       jangkauan tombol "Singkronkan Tagihan" pada layar master, yang bekerja atas
 *       <i>seluruh id hasil filter</i>) mencakup SEMUA sekolah dan SEMUA yayasan pada instalasi.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> {@code CommonPrivilages.checkPrevilages(..)}
 *       memeriksa hak terhadap {@code Common.getCurrentMenu()}, yang mengambil atribut sesi
 *       {@code currentMenu} &mdash; yaitu menu <i>terakhir yang diklik pengguna</i>, bukan halaman
 *       yang sedang dirender. Panel rincian penerima diskon disisipkan inline di halaman ini,
 *       sehingga hak yang berlaku atasnya adalah hak menu "Konfigurasi Diskon" (atau menu apa pun
 *       yang tersimpan terakhir di sesi).</li>
 *   <li><b>Penjaga "tidak ada perubahan" yang tidak pernah menyala.</b>
 *       {@code DiskonSiswaSyncHelper.updateItemBiayaCache(..)} membandingkan
 *       {@code diskonSiswa.getItemBiaya()} (sudah dinormalkan menjadi {@code ,12,34,}) dengan
 *       {@code buildItemBiayaString(..)} yang menghasilkan {@code 12,34} tanpa koma pembungkus.
 *       Keduanya tidak akan pernah sama, sehingga setiap sinkronisasi selalu melakukan UPDATE
 *       (beserta satu revisi Envers baru) dan selalu menambah 1 pada angka "data yang diperbarui"
 *       yang dilaporkan ke pengguna, walaupun tidak ada yang berubah.</li>
 *   <li><b>Lubang jejak audit.</b> Kelas ini {@link Audited}, tetapi {@code DiskonSiswaAction.onSave()}
 *       menghapus baris {@code diskon_siswa_item_biaya} dengan native SQL dan tombol "Bersihkan"
 *       menghapus seluruh {@code diskon_siswa_punya_siswa_baru} dengan native SQL pula &mdash;
 *       keduanya melewati Envers, sehingga riwayat perubahan data finansial berlubang.</li>
 *   <li><b>Diskon tidak kumulatif.</b> {@link Tagihan} hanya menyimpan satu FK
 *       {@code diskonSiswa}; bila dua aturan menyasar item biaya + siswa yang sama, yang tersimpan
 *       adalah aturan yang terakhir disinkronkan, bukan jumlah keduanya.</li>
 * </ul>
 *
 * @see ais.database.model.sekolah.DiskonSiswaItemBiaya
 * @see ais.database.model.sekolah.DiskonSiswaPunyaSiswa
 * @see ais.database.model.sekolah.Tagihan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "diskon_siswa")
public class DiskonSiswa extends GeneralValueObject {

	/**
	 * Jenis diskon untuk <b>anak dari seorang alumni</b> sekolah; memicu
	 * {@link #prosesAnakAlumni(DiskonSiswa)}. Nilai literal ini tersimpan apa adanya pada kolom
	 * {@code jenis} dan dicocokkan dengan {@code equals(..)}, jadi jangan diubah tanpa migrasi data.
	 */
	public static final String DISKON_ANAK_ALUMNI = "Diskon Anak Alumni";
	/**
	 * Jenis diskon untuk <b>alumni yang bersekolah lagi</b> di lingkungan yayasan; memicu
	 * {@link #prosesAlumni(DiskonSiswa)}.
	 */
	public static final String DISKON_ALUMNI = "Diskon Alumni";
	/**
	 * Jenis diskon untuk <b>siswa yang bersaudara</b> (berbagi orang tua) dan sama-sama masih
	 * aktif; memicu {@link #prosesSaudara(DiskonSiswa)}.
	 */
	public static final String DISKON_SAUDARA = "Diskon Saudara";
	/**
	 * Jenis diskon untuk <b>siswa yang saudaranya seorang alumni</b>; memicu
	 * {@link #prosesSaudaraAlumni(DiskonSiswa)}.
	 */
	public static final String DISKON_SAUDARA_ALUMNI = "Diskon Saudara Alumni";
	/**
	 * Jenis diskon untuk <b>anak pegawai bermasa kerja tetap</b>; memicu
	 * {@link #prosesAnakPegawai(DiskonSiswa, TipeMasaKerja)} dengan {@link TipeMasaKerja#Tetap}.
	 */
	public static final String DISKON_ANAK_PEGAWAI_TETAP = "Diskon Anak Pegawai Tetap";
	/**
	 * Jenis diskon untuk <b>anak pegawai honorer</b>; memicu
	 * {@link #prosesAnakPegawai(DiskonSiswa, TipeMasaKerja)} dengan {@link TipeMasaKerja#Honorer}.
	 */
	public static final String DISKON_ANAK_PEGAWAI_HONORER = "Diskon Anak Pegawai Honorer";

	/**
	 * Jenis diskon <b>tanpa syarat</b>: memicu {@link #prosesSemua(DiskonSiswa)}.
	 *
	 * <p>Perhatikan bahwa meski namanya "Diskon Semua Siswa", mesin di baliknya hanya mendaftarkan
	 * {@link CalonSiswa} (pendaftar PSB), <b>bukan</b> {@link Siswa} yang sudah bersekolah &mdash;
	 * lihat {@link #prosesSemua(DiskonSiswa)}.</p>
	 */
	public static final String DISKON_SEMUA = "Diskon Semua Siswa";

	/**
	 * Daftar pilihan jenis diskon yang ditawarkan combo "Jenis" pada dialog tambah/ubah
	 * {@code DiskonSiswaAction}.
	 *
	 * <p>Sengaja {@link TreeSet}, sehingga isinya <b>terurut alfabetis</b> (bukan urutan penambahan
	 * pada blok {@code static} di bawah) dan tidak mungkin ganda. Selain tujuh nilai ini,
	 * {@code DiskonSiswaAction} menambahkan sendiri satu item tambahan "Tanpa Jenis Diskon"
	 * bernilai {@code null} yang berarti "penerima dipilih manual, tanpa mesin otomatis".</p>
	 *
	 * <p><b>Peringatan:</b> koleksi ini {@code public static final} tetapi <b>tetap dapat diubah
	 * isinya</b> ({@code final} hanya mengikat referensinya). Jangan pernah memanggil
	 * {@code add}/{@code remove} atasnya dari kode pemanggil &mdash; perubahan akan bersifat global
	 * untuk seluruh JVM dan seluruh tenant.</p>
	 */
	public static final TreeSet<String> JENIS = new TreeSet<String>();
	/** Mengisi katalog {@link #JENIS} dengan tujuh konstanta {@code DISKON_*} di atas. */
	static {
		JENIS.add(DISKON_ANAK_PEGAWAI_TETAP);
		JENIS.add(DISKON_ANAK_PEGAWAI_HONORER);
		JENIS.add(DISKON_ALUMNI);
		JENIS.add(DISKON_ANAK_ALUMNI);
		JENIS.add(DISKON_SAUDARA_ALUMNI);
		JENIS.add(DISKON_SAUDARA);
		JENIS.add(DISKON_SEMUA);
	}

	/**
	 * Penanda versi serialisasi Java. Nilainya dibekukan agar baris yang pernah diserialisasi
	 * (mis. ke dalam sesi ZK) tetap dapat dibaca setelah kelas ini diubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer baris; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini (jejak audit "oleh siapa").
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah disimpan lewat jalur yang
	 *         mengisi jejak audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir, <b>hanya bila nilainya berisi</b>.
	 *
	 * <p>Nilai {@code null} atau string kosong/spasi diabaikan diam-diam (method langsung
	 * {@code return}), sehingga jejak audit lama tidak terhapus oleh proses latar yang tidak punya
	 * konteks pengguna. Perilaku ini sengaja dan seragam di seluruh entity repo ini.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir, <b>hanya bila nilainya berisi</b>. Sama seperti
	 * {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan agar jejak audit lama tidak
	 * tertimpa.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait siklus hidup JPA yang dijalankan Hibernate <b>tepat sebelum setiap UPDATE</b> baris ini;
	 * mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang memperbarui stempel
	 * waktu/pengguna perubahan.
	 *
	 * <p><b>Catatan tata letak:</b> pada baris sumber yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah}, yang diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()} (bukan {@code new Date()}, agar zona waktu/kalender
	 * institusi seragam). Penggabungan method dan field dalam satu baris itu adalah gaya bawaan
	 * repo untuk penyisipan jejak audit; jangan dipecah tanpa alasan kuat karena pola yang sama
	 * dipakai di ratusan entity lain.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object baru karena field-nya
	 *         diinisialisasi ke waktu pembuatan object
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk {@code <id>-<nama>}.
	 *
	 * <p>Membaca field {@code nama} secara langsung (bukan lewat {@link #getNama()}), sehingga
	 * tidak melakukan {@code trim()} dan aman dipanggil pada object yang belum ter-inisialisasi
	 * penuh. Dipakai antara lain oleh keluaran {@code System.out.println} pada mesin
	 * {@code proses*} dan oleh komponen ZK yang menampilkan object ini apa adanya.</p>
	 *
	 * @return {@code "<id>-<nama>"}; kedua bagian dapat berbunyi {@code null} bila belum terisi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama aturan diskon yang tampil di layar dan laporan; lihat {@link #getNama()}. */
	private String nama;
	/** Catatan bebas tentang aturan diskon ini; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Tahun ajaran cakupan aturan; lihat {@link #getTahunAjaran()} soal fallback {@code null}. */
	private String tahunAjaran;
	/** Sekolah pemilik aturan (kolom {@code sekolah_id}); lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik aturan (kolom {@code yayasan_id}); lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Saklar aktif/nonaktif aturan; lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Saklar "potong tagihan langsung" vs "kirim ke pembayaran"; lihat {@link #getMemotongTagihan()}. */
	private Boolean memotongTagihan;
	/** Jenis diskon, salah satu konstanta {@code DISKON_*}; lihat {@link #getJenis()}. */
	private String jenis;

	/** Awal masa berlaku diskon; lihat {@link #getDiskonMulai()}. */
	private Date diskonMulai;
	/** Akhir masa berlaku diskon; lihat {@link #getDiskonSampai()}. */
	private Date diskonSampai;
	/** Cache CSV id item biaya yang didiskon; lihat {@link #getItemBiaya()}. */
	private String itemBiaya;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA (dan dipakai
	 * {@code DiskonSiswaAction.onAdd(..)} untuk membuat aturan diskon baru).
	 *
	 * <p>Tidak mengisi apa pun. Nilai bawaan yang "terasa" pada object baru sebenarnya berasal dari
	 * getter, bukan dari konstruktor: {@link #getAktif()}, {@link #getMenggunkanPersen()}, dan
	 * {@link #getMemotongTagihan()} membaca {@code null} sebagai {@code true}, dan
	 * {@link #getTahunAjaran()} membaca {@code null} sebagai tahun akademik berjalan.</p>
	 */
	public DiskonSiswa() {
	}

	/**
	 * Kunci primer baris aturan diskon (kolom {@code id}, {@code IDENTITY} sisi database).
	 *
	 * <p>{@code insertable = false} berarti nilai ini tidak pernah dikirim pada INSERT &mdash;
	 * database yang menentukannya. Nilai {@code null} karena itu menjadi penanda baku "baris ini
	 * belum tersimpan", dan dipakai sebagai penanda di banyak tempat: {@code DiskonSiswaAction}
	 * memilih judul dialog "Tambah" atau "Ubah" berdasarkan {@code getId() == null}, dan
	 * {@code TagihanDiskonSiswaHelper} menolak memproses aturan diskon tanpa id.</p>
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
	 * Menyetel kunci primer. Umumnya hanya dipanggil Hibernate setelah INSERT; jangan disetel
	 * manual dari kode aplikasi.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama aturan diskon sebagaimana ditampilkan di seluruh layar dan laporan (mis. "Diskon Anak
	 * Guru 2026").
	 *
	 * <p>Kolom {@code nama}, {@code NOT NULL}, maksimal 255 karakter; {@code DiskonSiswaAction.onSave()}
	 * menolak menyimpan bila kosong. Getter ini <b>mem-{@code trim()} hasilnya</b> (tetapi tidak
	 * menulis balik ke field), sehingga nilai yang tampil bisa berbeda dari isi kolom bila di
	 * database ada spasi di ujung. Selain untuk tampilan, nama ini ikut tercetak sebagai keterangan
	 * diskon pada struk/rekap pembayaran lewat {@code PembayaranSiswaUtil} dan
	 * {@code Tagihan.getDiskonSiswa().getNama()}.</p>
	 *
	 * @return nama aturan diskon tanpa spasi di ujung, atau {@code null} bila field-nya {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama aturan diskon apa adanya (tanpa {@code trim()}; pemangkasan spasi baru terjadi
	 * saat dibaca lewat {@link #getNama()}).
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas tentang aturan diskon ini (kolom {@code keterangan}, boleh {@code null}).
	 *
	 * <p>Diisi lewat textbox 3 baris pada dialog tambah/ubah dan ditampilkan sebagai satu kolom
	 * tersendiri pada grid daftar diskon. Tidak dipakai logika bisnis mana pun &mdash; jangan
	 * dijadikan tempat menaruh kode/penanda yang dibaca program.</p>
	 *
	 * @return keterangan, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas aturan diskon.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Saklar aktif/nonaktif aturan diskon &mdash; <b>gerbang nyata yang dibaca mesin billing</b>.
	 *
	 * <p>{@code TagihanDiskonSiswaHelper.hitungDiskon(..)} menyaring dengan
	 * {@code Restrictions.eq("diskonSiswa.aktif", Boolean.TRUE)}, sehingga aturan yang dinonaktifkan
	 * tidak lagi ikut diperhitungkan pada tagihan yang <i>baru</i> disinkronkan. Perhatikan bahwa
	 * menonaktifkan aturan <b>tidak</b> menghapus potongan yang sudah terlanjur tertulis di baris
	 * {@link Tagihan} lama; nilai diskon di sana baru berubah bila tagihan itu disinkronkan ulang.
	 * Perhatikan juga bahwa filter Hibernate itu memakai perbandingan kolom langsung, jadi baris
	 * lama yang kolom {@code aktif}-nya {@code NULL} <b>tidak</b> lolos filter, walaupun getter ini
	 * membacanya sebagai {@code true} di sisi Java.</p>
	 *
	 * <p><b>Menormalkan {@code null} menjadi {@code true}.</b> Karena kelas ini memakai property
	 * access, nilai {@code true} hasil normalisasi itu ikut tertulis ke database begitu baris
	 * tersentuh flush &mdash; baris lama ber-{@code NULL} akan "menjadi aktif" secara permanen.</p>
	 *
	 * <p>Di UI, saklar ini muncul sebagai checkbox "Aktif" pada setiap baris grid
	 * {@code DiskonSiswaAction}; checkbox itu <b>dinonaktifkan bila pengguna tidak punya hak UPDATE</b>
	 * dan menyimpan perubahannya langsung tanpa tombol Simpan.</p>
	 *
	 * @return {@code true} bila aturan aktif (termasuk bila kolomnya masih {@code null})
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel saklar aktif/nonaktif aturan diskon.
	 *
	 * @param aktif status baru; {@code null} akan dibaca sebagai {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Tahun ajaran cakupan aturan diskon &mdash; salah satu kunci pencocokan terpenting kelas ini.
	 *
	 * <p>Dipakai sebagai penyaring di banyak tempat:</p>
	 * <ul>
	 *   <li>{@code DiskonSiswaSyncHelper.sinkronkanPenerima(..)} hanya menyentuh {@link Tagihan}
	 *       yang {@code tahunAjaran}-nya sama persis dengan nilai ini;</li>
	 *   <li>{@code TagihanDiskonSiswaHelper.hitungDiskon(..)} mencocokkannya dengan tahun ajaran
	 *       tagihan yang sedang dihitung;</li>
	 *   <li>seluruh mesin {@code proses*} yang menjaring {@link CalonSiswa} memakainya untuk
	 *       memilih gelombang pendaftaran PSB yang sesuai.</li>
	 * </ul>
	 *
	 * <p><b>Fallback yang berbahaya.</b> Bila kolomnya {@code null}, getter ini mengembalikan tahun
	 * akademik <i>berjalan</i> ({@code Common.getCurrentTahunAkademik()}). Karena kelas ini memakai
	 * property access, nilai karangan itu ikut tertulis ke kolom pada flush berikutnya &mdash;
	 * artinya aturan diskon yang sengaja disimpan sebagai "berlaku semua tahun ajaran" (grid
	 * {@code DiskonSiswaAction} memang merender {@code null}/kosong sebagai teks "Semua") dapat
	 * <b>terkunci diam-diam ke satu tahun ajaran</b> begitu barisnya tersentuh. Ketidaksesuaian ini
	 * nyata: renderer memperlakukan {@code null} sebagai "semua", sedangkan getter memperlakukannya
	 * sebagai "tahun berjalan".</p>
	 *
	 * @return tahun ajaran aturan diskon; tidak pernah {@code null} (jatuh ke tahun akademik
	 *         berjalan)
	 */
	public String getTahunAjaran() {
		return tahunAjaran == null ? Common.getCurrentTahunAkademik() : tahunAjaran;
	}

	/**
	 * Menyetel tahun ajaran cakupan aturan diskon.
	 *
	 * @param tahunAjaran tahun ajaran baru; {@code null} akan dibaca sebagai tahun akademik
	 *                    berjalan oleh {@link #getTahunAjaran()} (lihat peringatan di sana)
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Cache CSV berisi id {@link ItemBiayaSekolah} yang ikut didiskon oleh aturan ini &mdash;
	 * <b>getter yang menulis balik (destruktif) ke field-nya sendiri</b>.
	 *
	 * <p><b>Apa yang dilakukan.</b> Nilai mentah (mis. {@code "12,34"}) dibungkus koma di kedua
	 * ujung menjadi {@code ",12,34,"}, lalu koma ganda dirapikan dengan tiga kali
	 * {@code replaceAll(",,", ",")} berturut-turut (tiga kali, karena satu kali sapuan tidak
	 * menangani rentetan koma yang saling bertumpang tindih). Sisa-sisa yang hanya berisi koma
	 * ({@code ","} sampai {@code ",,,,"}) dianggap "tidak ada item" dan dijadikan string kosong.
	 * Hasilnya <b>ditugaskan kembali ke field {@code itemBiaya}</b>, bukan sekadar dikembalikan.</p>
	 *
	 * <p><b>Mengapa koma pembungkus itu wajib.</b> {@code TagihanDiskonSiswaHelper.hitungDiskon(..)}
	 * mencari aturan diskon yang berlaku atas satu item biaya dengan
	 * {@code Restrictions.ilike("diskonSiswa.itemBiaya", "," + itemBiayaId + ",", ANYWHERE)}. Tanpa
	 * koma pembungkus, item pertama dan terakhir daftar tidak akan pernah cocok; dan tanpa koma di
	 * kedua sisi pola, id {@code 1} akan salah cocok dengan {@code 11}, {@code 21}, dan seterusnya.
	 * Perlu dicatat bahwa {@code DiskonSiswaAction.onSave()} menyusun nilai ini <b>tanpa</b> koma
	 * pembungkus; getter inilah satu-satunya yang memperbaikinya sebelum Hibernate menuliskannya.</p>
	 *
	 * <p><b>Efek samping saat flush.</b> Karena kelas ini memakai property access, Hibernate sendiri
	 * memanggil getter ini saat dirty-check/UPDATE, sehingga membaca properti ini saja sudah cukup
	 * untuk mengubah isi kolom di database. Nilainya hanya berupa pemisah, jadi tidak ada id yang
	 * hilang &mdash; tetapi setiap sentuhan tetap berpotensi memunculkan revisi {@link Audited}
	 * (Envers) baru pada data finansial.</p>
	 *
	 * <p><b>Konsekuensi lanjutan (bug nyata di pemanggil).</b>
	 * {@code DiskonSiswaSyncHelper.updateItemBiayaCache(..)} membandingkan hasil getter ini (sudah
	 * berkoma pembungkus) dengan string yang dibangunnya sendiri <i>tanpa</i> koma pembungkus.
	 * Perbandingan itu tidak akan pernah sama, sehingga penjaga "tidak ada perubahan &rarr; jangan
	 * UPDATE" tidak pernah menyala: setiap sinkronisasi selalu menulis UPDATE, selalu membuat revisi
	 * Envers baru, dan selalu melaporkan sekurang-kurangnya satu "data diperbarui" kepada pengguna
	 * walaupun tidak ada yang benar-benar berubah.</p>
	 *
	 * <p><b>Kolom:</b> {@code item_biaya}, bertipe {@code text} (tanpa batas panjang praktis),
	 * boleh {@code null}. Sumber kebenaran yang sesungguhnya tetap tabel
	 * {@link ais.database.model.sekolah.DiskonSiswaItemBiaya}; kolom ini hanya salinan untuk
	 * pencarian cepat, dan bisa <i>berbeda</i> darinya bila baris anak diubah lewat jalur lain
	 * tanpa sinkronisasi.</p>
	 *
	 * @return daftar id item biaya berbentuk {@code ",12,34,"}; string kosong bila tidak ada item
	 *         (tidak pernah {@code null})
	 */
	@Column(name = "item_biaya", nullable = true, columnDefinition = "text")
	public String getItemBiaya() {

		itemBiaya = (itemBiaya == null || itemBiaya.trim().equalsIgnoreCase(",") ? "" : "," + itemBiaya.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (itemBiaya.equals(",")) {
			itemBiaya = "";
		} else if (itemBiaya.equals(",,")) {
			itemBiaya = "";
		} else if (itemBiaya.equals(",,,")) {
			itemBiaya = "";
		} else if (itemBiaya.equals(",,,,")) {
			itemBiaya = "";
		}

		return itemBiaya == null ? "" : itemBiaya.trim();
	}

	/**
	 * Menyetel cache CSV id item biaya <b>apa adanya, tanpa normalisasi</b>.
	 *
	 * <p>Pemanggil ({@code DiskonSiswaAction.onSave()} dan
	 * {@code DiskonSiswaSyncHelper.updateItemBiayaCache(..)}) menyerahkan bentuk polos
	 * {@code "12,34"}; pembungkusan komanya dikerjakan {@link #getItemBiaya()} saat dibaca. Jangan
	 * memakai nilai field ini langsung untuk pencocokan &mdash; selalu lewat getter.</p>
	 *
	 * @param itemBiaya daftar id item biaya dipisah koma; boleh {@code null}/kosong
	 */
	public void setItemBiaya(String itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Sekolah pemilik aturan diskon (kolom {@code sekolah_id}) &mdash; pembatas cakupan tenant
	 * terpenting kelas ini.
	 *
	 * <p>Seluruh mesin {@code proses*} memakai nilai ini untuk membatasi siswa/calon siswa mana yang
	 * boleh didaftarkan sebagai penerima, dan dialog tambah/ubah mengunci combo Sekolah ke
	 * {@code SekolahUtil.getSekolah()} milik sesi (combo-nya {@code setReadonly(true)}).</p>
	 *
	 * <p><b>Getter aktif, bukan sekadar pembaca field.</b> Nilai dilewatkan
	 * {@code GeneralValueObject.check(..)} yang menyelesaikan proxy lazy/detached menjadi object
	 * kanonik (lewat {@code EntityIdentityMap} dan cache entity), sehingga aman dibaca dari thread
	 * latar atau sesi yang berbeda &mdash; itulah sebabnya {@code DiskonSiswaSyncHelper} dan
	 * {@code TagihanDiskonSiswaHelper} berani bekerja pada {@code openSession()} terpisah.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik aturan diskon, <b>menolak object yang belum tersimpan</b>.
	 *
	 * <p>Bila argumennya {@code null} <i>atau</i> {@link Sekolah} tanpa id (mis. item combo
	 * "Semua Sekolah" yang berupa object kosong), field disetel {@code null} &mdash; bukan diisi
	 * object tanpa id yang akan membuat Hibernate gagal saat menulis FK. Konsekuensinya: memilih
	 * "Semua" pada combo sekolah berarti aturan diskon disimpan <b>tanpa sekolah</b>, dan seluruh
	 * mesin {@code proses*} kemudian menjaring dengan {@code sekolah IS NULL} sehingga tidak
	 * menemukan siapa pun.</p>
	 *
	 * @param sekolah sekolah baru; {@code null} atau tanpa id akan disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Yayasan pemilik aturan diskon (kolom {@code yayasan_id}) &mdash; <b>getter destruktif: nilai
	 * yang tersimpan ditimpa dari sekolah setiap kali dibaca</b>.
	 *
	 * <p><b>Apa yang dilakukan.</b> Getter ini pertama-tama memanggil {@link #getSekolah()}; bila
	 * sekolahnya ada, field {@code yayasan} <b>ditimpa</b> dengan {@code sekolah.getYayasan()}.
	 * Setelah itu barulah hasilnya dilewatkan {@code GeneralValueObject.check(..)} untuk
	 * menyelesaikan proxy.</p>
	 *
	 * <p><b>Akibatnya.</b> Kolom {@code yayasan_id} praktis bukan data yang berdiri sendiri,
	 * melainkan turunan dari {@code sekolah_id}. Nilai yayasan yang pernah disimpan berbeda dari
	 * yayasan sekolahnya (mis. karena sekolah dipindah ke yayasan lain di kemudian hari) akan
	 * <b>hilang permanen</b> pada flush berikutnya, tanpa peringatan dan tanpa jejak selain revisi
	 * Envers. Untuk aturan diskon tanpa sekolah, field yayasan dibiarkan apa adanya.</p>
	 *
	 * @return yayasan pemilik &mdash; yayasan milik {@link #getSekolah()} bila sekolahnya terisi,
	 *         atau nilai tersimpan bila tidak; dapat {@code null}
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
	 * Menyetel yayasan pemilik aturan diskon, <b>menolak object yang belum tersimpan</b> (pola sama
	 * dengan {@link #setSekolah(Sekolah)}).
	 *
	 * <p>Perlu diingat bahwa nilai apa pun yang disetel di sini akan ditimpa kembali oleh
	 * {@link #getYayasan()} selama {@link #getSekolah()} terisi.</p>
	 *
	 * @param yayasan yayasan baru; {@code null} atau tanpa id akan disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Saklar cara hitung potongan; lihat {@link #getMenggunkanPersen()}. Nama field salah eja
	 * ("menggunkan"), tetapi sekaligus menjadi nama kolomnya &mdash; jangan diperbaiki tanpa migrasi.
	 */
	private Boolean menggunkanPersen;

	/**
	 * Saklar cara hitung potongan: <b>persen dari nominal tagihan</b> ({@code true}, bawaan) atau
	 * <b>rupiah tetap</b> ({@code false}).
	 *
	 * <p>Saklar ini menentukan arti setiap angka {@code defaultBiaya} pada seluruh baris
	 * {@link ais.database.model.sekolah.DiskonSiswaItemBiaya} milik aturan ini sekaligus:</p>
	 * <ul>
	 *   <li>{@code true} &rarr; potongan = {@code nominalTagihan * defaultBiaya / 100};</li>
	 *   <li>{@code false} &rarr; potongan = {@code defaultBiaya} apa adanya.</li>
	 * </ul>
	 * <p>Rumus itu diterapkan {@code DiskonSiswaSyncHelper.sinkronkanPenerima(..)} dan
	 * {@code TagihanDiskonSiswaHelper.sinkronkanSatuTagihan(..)}. Tidak ada validasi maupun konversi
	 * yang mencegah pembalikan makna: angka {@code 10} yang tadinya berarti "diskon 10%" mendadak
	 * berarti "diskon Rp10" begitu checkbox "Menggunakan penghitungan persen" dilepas &mdash; dan
	 * sebaliknya, nominal {@code 500000} bisa berubah menjadi "diskon 500.000%".</p>
	 *
	 * <p>Menormalkan {@code null} menjadi {@code true}, dan seperti saklar lain di kelas ini nilai
	 * hasil normalisasi itu ikut tertulis ke database pada flush berikutnya.</p>
	 *
	 * @return {@code true} bila potongan dihitung sebagai persen (termasuk bila kolomnya masih
	 *         {@code null})
	 */
	public Boolean getMenggunkanPersen() {
		return menggunkanPersen == null ? true : menggunkanPersen;
	}

	/**
	 * Menyetel cara hitung potongan (persen atau rupiah).
	 *
	 * <p>Disetel dari checkbox "Menggunakan penghitungan persen" pada dialog tambah/ubah. Mengubah
	 * nilai ini mengubah arti seluruh angka diskon aturan ini sekaligus &mdash; lihat peringatan di
	 * {@link #getMenggunkanPersen()}.</p>
	 *
	 * @param menggunkanPersen {@code true} untuk persen, {@code false} untuk rupiah tetap;
	 *                         {@code null} dibaca sebagai {@code true}
	 */
	public void setMenggunkanPersen(Boolean menggunkanPersen) {
		this.menggunkanPersen = menggunkanPersen;
	}

	/**
	 * Jenis diskon &mdash; menentukan <b>mesin pencari penerima otomatis</b> mana yang tersedia
	 * untuk aturan ini.
	 *
	 * <p>Nilainya salah satu konstanta {@code DISKON_*} pada kelas ini (lihat {@link #JENIS}).
	 * {@code DiskonSiswaPunyaSiswaHelper} membandingkannya dengan {@code equals(..)} untuk memilih
	 * antara {@link #prosesAnakAlumni(DiskonSiswa)},
	 * {@link #prosesAnakPegawai(DiskonSiswa, TipeMasaKerja)} (varian Tetap/Honorer),
	 * {@link #prosesSaudara(DiskonSiswa)}, {@link #prosesSaudaraAlumni(DiskonSiswa)},
	 * {@link #prosesAlumni(DiskonSiswa)}, dan {@link #prosesSemua(DiskonSiswa)}. Label tombolnya
	 * pun dirakit dari nilai ini: {@code "Singkronkan Data " + getJenis()}.</p>
	 *
	 * <p><b>Menormalkan string kosong menjadi {@code null}.</b> Keduanya berarti "Tanpa Jenis
	 * Diskon": tombol sinkronisasi otomatis <b>tidak dirender sama sekali</b> dan penerima harus
	 * ditambahkan manual lewat tombol "Ambil Siswa"/"Ambil Calon Siswa". Berbeda dengan saklar
	 * boolean di kelas ini, normalisasi di sini justru menghapus nilai (kosong &rarr; {@code null}),
	 * sehingga aman.</p>
	 *
	 * <p>Perhatikan bahwa jenis <b>hanya memengaruhi cara penerima ditemukan</b>, bukan cara
	 * potongan dihitung. Setelah baris {@link DiskonSiswaPunyaSiswa} terbentuk, mengubah jenis tidak
	 * membatalkan penerima yang sudah terdaftar.</p>
	 *
	 * @return jenis diskon, atau {@code null} bila "Tanpa Jenis Diskon"
	 */
	public String getJenis() {
		return jenis == null || jenis.isEmpty() ? null : jenis;
	}

	/**
	 * Menyetel jenis diskon.
	 *
	 * @param jenis salah satu konstanta {@code DISKON_*}, atau {@code null}/kosong untuk "Tanpa
	 *              Jenis Diskon". Nilai di luar daftar itu tersimpan tetapi tidak akan cocok dengan
	 *              mesin {@code proses*} mana pun, sehingga tombol sinkronisasi otomatisnya muncul
	 *              namun tidak melakukan apa-apa.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mesin pencari penerima untuk jenis {@link #DISKON_ALUMNI}: mendaftarkan <b>siswa yang pernah
	 * bersekolah di lingkungan yayasan lalu masuk lagi</b> sebagai calon penerima diskon.
	 *
	 * <h3>Kapan dipanggil</h3>
	 * <p>Hanya dari tombol <b>"Singkronkan Data Diskon Alumni"</b> pada panel rincian penerima
	 * ({@code DiskonSiswaPunyaSiswaHelper}), yang muncul saat {@link #getJenis()} bernilai
	 * {@link #DISKON_ALUMNI}. Dijalankan di dalam {@code Common.createDefaultTimer(..)} (thread
	 * timer ZK), lalu daftar penerima dimuat ulang.</p>
	 *
	 * <h3>Cara kerja &mdash; dua tahap</h3>
	 * <ol>
	 *   <li><b>Tahap siswa terdaftar.</b> Satu native SQL mengelompokkan seluruh baris
	 *       {@code sekolah.siswa} berdasarkan {@code (nama_siswa, tanggal_lahir)} dan menyisakan
	 *       kelompok yang muncul lebih dari sekali &mdash; yakni orang yang sama tercatat sebagai
	 *       siswa lebih dari satu kali. Untuk tiap kelompok diambil {@code min(tahun_masuk)}
	 *       sebagai "pendaftaran lama", lalu dicari satu {@link Siswa} dengan nama + tanggal lahir
	 *       yang sama, {@code tahunMasuk} <b>lebih besar</b> dari tahun itu, dan bersekolah di
	 *       {@link #getSekolah()} aturan ini &mdash; itulah pendaftaran barunya.</li>
	 *   <li><b>Tahap calon siswa (PSB).</b> Seluruh {@link CalonSiswa} pada gelombang PSB yang
	 *       tahun ajarannya sama dengan {@link #getTahunAjaran()} dan sekolahnya sama dengan
	 *       {@link #getSekolah()} diperiksa: bila ada {@link Siswa} bernama + bertanggal lahir sama
	 *       dengan {@code tahunMasuk} lebih kecil dan bersekolah di <b>sekolah lain</b>, calon siswa
	 *       itu dianggap alumni yang mendaftar lagi.</li>
	 * </ol>
	 *
	 * <h3>Yang ditulis ke database</h3>
	 * <p>Untuk setiap kecocokan yang <b>belum</b> punya baris {@link DiskonSiswaPunyaSiswa} pada
	 * aturan diskon ini, dibuat satu baris baru ({@code session.save(..)} + {@code flush()}
	 * langsung, satu per satu, tanpa transaksi eksplisit). Kolom {@code keterangan} diisi kalimat
	 * "Alumni &lt;nama sekolah sebelumnya&gt; angkatan &lt;tahun&gt;" plus " lulus tahun
	 * &lt;tahun&gt;" bila tahun lulusnya wajar ({@code &gt; 1900}).</p>
	 *
	 * <p><b>Penting:</b> baris yang dibuat <b>tidak</b> menyetel {@code setujui}, sehingga
	 * {@code DiskonSiswaPunyaSiswa.getSetujui()} membacanya sebagai {@code false}. Baris seperti itu
	 * <b>belum</b> memengaruhi rupiah apa pun &mdash; baik {@code DiskonSiswaSyncHelper} maupun
	 * {@code TagihanDiskonSiswaHelper} menyaring {@code setujui = TRUE}. Jadi method ini hanya
	 * <i>mengusulkan</i> penerima; persetujuannya (checkbox "Setujui", yang memang dijaga hak
	 * UPDATE) adalah gerbang finansial sesungguhnya.</p>
	 *
	 * <h3>Kuirk &amp; batasan yang perlu diketahui</h3>
	 * <ul>
	 *   <li><b>Kunci pencocokan hanya nama + tanggal lahir.</b> Dua orang berbeda dengan nama dan
	 *       tanggal lahir persis sama (bukan hal mustahil pada nama-nama umum) akan dianggap orang
	 *       yang sama dan menerima diskon.</li>
	 *   <li><b>Native SQL tahap 1 tidak menyaring sekolah/yayasan sama sekali</b> &mdash; ia
	 *       memindai {@code sekolah.siswa} seluruh instalasi. Penyaringan tenant baru terjadi pada
	 *       query kedua ({@code eq("sekolah", diskonSiswa.getSekolah())}). Bila aturan diskon
	 *       tersimpan tanpa sekolah, query itu menjadi {@code sekolah IS NULL} dan tidak menemukan
	 *       siapa pun &mdash; gagal-tertutup, bukan gagal-terbuka.</li>
	 *   <li><b>Membanjiri log dengan PII.</b> Setiap iterasi mencetak nama siswa dan tanggal lahir
	 *       ke {@code System.out} (tiga {@code println} berbeda). Pada instalasi dengan puluhan ribu
	 *       siswa, satu klik tombol dapat menuliskan puluhan ribu baris berisi data pribadi anak ke
	 *       log server.</li>
	 *   <li><b>Berat dan tanpa batas.</b> Tidak ada {@code setMaxResults} pada query luar, dan
	 *       setiap kecocokan melakukan {@code flush()} sendiri. Untuk data besar, ini adalah operasi
	 *       O(jumlah siswa) berisi query bersarang &mdash; dijalankan pada thread timer ZK.</li>
	 *   <li><b>Tidak idempoten sempurna.</b> Baris yang sudah ada dilewati, tetapi baris yang
	 *       sebelumnya dihapus operator akan dibuat ulang setiap kali tombol ditekan.</li>
	 * </ul>
	 *
	 * <h3>Catatan hak akses (temuan)</h3>
	 * <p>Method ini statis dan tidak memeriksa hak apa pun &mdash; itu wajar untuk sebuah helper
	 * entity. Masalahnya, <b>pemanggilnya pun tidak</b>: tombol "Singkronkan Data &lt;jenis&gt;" di
	 * {@code DiskonSiswaPunyaSiswaHelper} dipasang tanpa {@code CommonPrivilages.checkPrevilages(..)}
	 * sama sekali, sehingga pengguna berhak BACA saja dapat menjalankan penjaringan massal ini.</p>
	 *
	 * @param diskonSiswa aturan diskon yang sedang disinkronkan; {@link #getSekolah()} dan
	 *                    {@link #getTahunAjaran()}-nya dipakai sebagai pembatas, dan setiap baris
	 *                    {@link DiskonSiswaPunyaSiswa} baru ditautkan kepadanya. Tidak boleh
	 *                    {@code null} (akan melempar {@code NullPointerException}).
	 * @see #prosesAnakAlumni(DiskonSiswa)
	 * @see #prosesSaudaraAlumni(DiskonSiswa)
	 */
	@SuppressWarnings("unchecked")
	public static void prosesAlumni(DiskonSiswa diskonSiswa) {
		Session session = HibernateUtil.currentSession();

		List<Object[]> namaTglLahirs = session.createSQLQuery(
				"select nama_siswa,tanggal_lahir,min(tahun_masuk) as tahun_masuk,max(tahun_masuk) as max_tahun_masuk from sekolah.siswa where nama_siswa != '' and nama_siswa is not null and tanggal_lahir is not null group by nama_siswa,tanggal_lahir having count(*)>1")
				.list();
		for (Object[] dataNamaTgl : namaTglLahirs) {
			String nama = (String) dataNamaTgl[0];
			Date tglLahir = (Date) dataNamaTgl[1];
			Number tahun_masuk = (Number) dataNamaTgl[2];

			System.out.println("nama -> " + nama + ", tglLahir -> " + tglLahir + ", tahun_masuk -> " + tahun_masuk);

			if (tahun_masuk != null) {

				Siswa siswa = (Siswa) ConstantValues.simpleObject(
						session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
								.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
								.add(Restrictions.eq("tanggalLahir", tglLahir))
								.add(Restrictions.ilike("namaSiswa", nama, MatchMode.EXACT))
								.add(Restrictions.gt("tahunMasuk", tahun_masuk.intValue()))
								.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())).setMaxResults(1),
						Siswa.class);

				System.out.println("siswa -> " + siswa);

				if (siswa != null) {
					DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
							session.createCriteria(DiskonSiswaPunyaSiswa.class)
									.add(Restrictions.or(Restrictions.eq("siswa", siswa),
											Restrictions.eq("calonSiswa.id", siswa.getCalonSiswa())))
									.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
							DiskonSiswaPunyaSiswa.class);
					if (diskonSiswaPunyaSiswa == null) {

						Siswa siswaSebelumnya = (Siswa) ConstantValues.simpleObject(
								session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
										.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
										.add(Restrictions.eq("tanggalLahir", tglLahir))
										.add(Restrictions.ilike("namaSiswa", nama, MatchMode.EXACT))
										.add(Restrictions.eq("tahunMasuk", tahun_masuk.intValue())).setMaxResults(1),
								Siswa.class);

						diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
						diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
						diskonSiswaPunyaSiswa.setSiswa(siswa);
						if (siswaSebelumnya != null) {
							diskonSiswaPunyaSiswa.setKeterangan("Alumni " + (siswaSebelumnya.getSekolah().getNama())
									+ " angkatan " + siswaSebelumnya.getTahunMasuk()
									+ (siswaSebelumnya.getTahunLulus() != null && siswaSebelumnya.getTahunLulus() > 1900
											? " lulus tahun " + siswaSebelumnya.getTahunLulus()
											: ""));
						}
						session.save(diskonSiswaPunyaSiswa);
						session.flush();
					}
				}
			}
		}

		List<Object[]> calonSiswas = session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
				.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb")
				.add(Restrictions.eq("gelombangPendaftaranPsb.tahunAjaran", diskonSiswa.getTahunAjaran()))
				.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())).add(Restrictions.ne("namaSiswa", ""))
				.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.isNotNull("tanggalLahir"))
				.add(Restrictions.isNotNull("tahunMasuk"))
				.setProjection(Projections.projectionList().add(Projections.property("id"))
						.add(Projections.property("namaSiswa")).add(Projections.property("tanggalLahir"))
						.add(Projections.property("tahunMasuk")))
				.list();
		for (Object[] dataNamaTgl : calonSiswas) {
			Long id = (Long) dataNamaTgl[0];
			String nama = (String) dataNamaTgl[1];
			Date tglLahir = (Date) dataNamaTgl[2];
			Number tahun_masuk = (Number) dataNamaTgl[3];

			Number ada = (Number) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.eq("tanggalLahir", tglLahir))
					.add(Restrictions.ilike("namaSiswa", nama, MatchMode.EXACT))
					.add(Restrictions.lt("tahunMasuk", tahun_masuk.intValue()))
					.add(Restrictions.ne("sekolah", diskonSiswa.getSekolah())).setProjection(Projections.rowCount())
					.uniqueResult();

			System.out.println("id -> " + id + " nama -> " + nama + ", tglLahir -> " + tglLahir + ", tahun_masuk -> "
					+ tahun_masuk + " ada " + ada);

			if (ada.intValue() > 0) {
				CalonSiswa calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id);
				if (calonSiswa != null) {

					DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
							session.createCriteria(DiskonSiswaPunyaSiswa.class)
									.add(Restrictions.or(Restrictions.eq("calonSiswa", calonSiswa),
											Restrictions.eq("siswa", calonSiswa.getSiswa())))
									.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
							DiskonSiswaPunyaSiswa.class);
					if (diskonSiswaPunyaSiswa == null) {

						Siswa siswaSebelumnya = (Siswa) ConstantValues.simpleObject(
								session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
										.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
										.add(Restrictions.eq("tanggalLahir", tglLahir))
										.add(Restrictions.ilike("namaSiswa", nama, MatchMode.EXACT))
										.add(Restrictions.eq("tahunMasuk", tahun_masuk.intValue())).setMaxResults(1),
								Siswa.class);

						diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
						diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
						diskonSiswaPunyaSiswa.setSiswa(calonSiswa.getSiswa());
						diskonSiswaPunyaSiswa.setCalonSiswa(calonSiswa);

						if (siswaSebelumnya != null) {
							diskonSiswaPunyaSiswa.setKeterangan("Alumni " + (siswaSebelumnya.getSekolah().getNama())
									+ " angkatan " + siswaSebelumnya.getTahunMasuk()
									+ (siswaSebelumnya.getTahunLulus() != null && siswaSebelumnya.getTahunLulus() > 1900
											? " lulus tahun " + siswaSebelumnya.getTahunLulus()
											: ""));
						}

						session.save(diskonSiswaPunyaSiswa);
						session.flush();
					}

				}
			}
		}
	}

	/**
	 * Mesin pencari penerima untuk jenis {@link #DISKON_SAUDARA_ALUMNI}: mendaftarkan <b>siswa yang
	 * memiliki saudara kandung berstatus alumni</b> sebagai calon penerima diskon.
	 *
	 * <h3>Kapan dipanggil</h3>
	 * <p>Hanya dari tombol "Singkronkan Data Diskon Saudara Alumni" pada panel rincian penerima,
	 * lewat {@code DiskonSiswaPunyaSiswaHelper}. Berjalan pada thread timer ZK.</p>
	 *
	 * <h3>Cara kerja &mdash; empat tahap, dua sumbu (ayah dan ibu)</h3>
	 * <p>"Bersaudara" di sini didefinisikan sebagai <b>berbagi (nama orang tua, tanggal lahir orang
	 * tua)</b> yang sama persis &mdash; dilakukan dua kali, sekali memakai pasangan
	 * {@code namaAyah}/{@code tanggalLahirAyah} dan sekali memakai
	 * {@code namaIbu}/{@code tanggalLahirIbu}. "Alumni" didefinisikan sebagai {@link Siswa} yang
	 * {@code statusKeluar.nama} mengandung kata "lulus" (pencocokan {@code ilike} di mana pun dalam
	 * teks) dan masih {@code aktif} atau {@code aktif IS NULL}.</p>
	 * <ol>
	 *   <li><b>Tahap 1 &amp; 2 (siswa terdaftar, jalur ayah lalu jalur ibu).</b> Dari setiap alumni
	 *       diambil identitas orang tuanya, lalu dicari seluruh {@link Siswa} yang masih bersekolah
	 *       ({@code statusKeluar IS NULL}) dengan identitas orang tua sama <b>di sekolah aturan
	 *       diskon ini</b>. Semuanya didaftarkan.</li>
	 *   <li><b>Tahap 3 &amp; 4 (calon siswa PSB, jalur ayah lalu jalur ibu).</b> Dari setiap
	 *       {@link CalonSiswa} pada gelombang PSB bertahun ajaran sama dan bersekolah sama, dicari
	 *       alumni dengan identitas orang tua sama <b>di sekolah LAIN</b>
	 *       ({@code ne("sekolah", ...)}) dan bernama berbeda dari calon siswa itu sendiri.</li>
	 * </ol>
	 * <p>Asimetri "sekolah sama" (tahap 1&amp;2) versus "sekolah lain" (tahap 3&amp;4) memang
	 * disengaja: pendaftar baru mendapat diskon karena kakaknya pernah bersekolah di unit lain
	 * dalam yayasan yang sama.</p>
	 *
	 * <h3>Yang ditulis ke database</h3>
	 * <p>Sama dengan {@link #prosesAlumni(DiskonSiswa)}: satu {@link DiskonSiswaPunyaSiswa} baru per
	 * kecocokan yang belum ada, disimpan dan di-{@code flush} satu per satu, dengan {@code setujui}
	 * dibiarkan kosong (dibaca sebagai belum disetujui). Kolom {@code keterangan} diisi kalimat
	 * "&lt;daftar nama&gt; adalah saudara alumni &lt;nama alumni&gt;".</p>
	 *
	 * <h3>Kuirk &amp; bug yang ditemukan</h3>
	 * <ul>
	 *   <li><b>Bug koma di awal daftar (tahap 3 &amp; 4).</b> Variabel penampung sudah diisi awalan
	 *       "&lt;nama&gt; adalah saudara alumni " <i>sebelum</i> perulangan penggabungan nama
	 *       dijalankan, sedangkan perulangan itu memakai pola
	 *       {@code penampung.isEmpty() ? n : "," + n}. Karena penampung tidak pernah kosong,
	 *       cabang pertama tidak pernah terpakai dan hasilnya selalu berkoma di depan:
	 *       "Budi adalah saudara alumni ,Andi,Cindy". Murni kosmetik (kolom keterangan), tetapi
	 *       nyata dan ikut tercetak di layar.</li>
	 *   <li><b>Keterangan identik untuk semua saudara (tahap 1 &amp; 2).</b> Daftar nama saudara
	 *       dirakit sekali untuk satu kelompok lalu dipasang ke <b>setiap</b> baris, sehingga
	 *       keterangan seorang siswa juga memuat namanya sendiri.</li>
	 *   <li><b>Pencocokan orang tua rapuh.</b> Berbasis kesamaan teks nama persis
	 *       ({@code MatchMode.EXACT}) plus tanggal lahir. Beda ejaan, gelar, atau tanggal lahir yang
	 *       kosong membuat saudara kandung tidak terdeteksi &mdash; dan sebaliknya, dua keluarga
	 *       dengan nama ayah umum dan tanggal lahir kebetulan sama akan digabung.</li>
	 *   <li><b>Definisi "lulus" berbasis substring.</b> Status keluar apa pun yang memuat kata
	 *       "lulus" ikut terjaring &mdash; termasuk, misalnya, "Tidak Lulus" bila status seperti itu
	 *       dipakai instalasi tersebut.</li>
	 *   <li><b>Tidak menyaring tahun ajaran pada tahap 1 &amp; 2</b>, dan tidak menyaring sekolah
	 *       pada pengambilan daftar alumninya &mdash; hanya pada pencarian saudaranya.</li>
	 *   <li>Setiap iterasi mencetak nama orang tua + tanggal lahirnya ke {@code System.out} (PII).</li>
	 * </ul>
	 *
	 * <h3>Catatan hak akses (temuan)</h3>
	 * <p>Sama seperti mesin {@code proses*} lain: tombol pemanggilnya tidak memeriksa hak apa pun.</p>
	 *
	 * @param diskonSiswa aturan diskon yang sedang disinkronkan; menyediakan sekolah dan tahun
	 *                    ajaran pembatas serta menjadi induk baris penerima yang dibuat. Tidak boleh
	 *                    {@code null}.
	 * @see #prosesSaudara(DiskonSiswa)
	 */
	@SuppressWarnings("unchecked")
	public static void prosesSaudaraAlumni(DiskonSiswa diskonSiswa) {
		Session session = HibernateUtil.currentSession();

		List<Object[]> namaTglLahirs = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
				.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
				.createAlias("statusKeluar", "statusKeluar")
				.add(Restrictions.ilike("statusKeluar.nama", "lulus", MatchMode.ANYWHERE))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("tanggalLahirAyah")).add(Restrictions.isNotNull("namaAyah"))
				.setProjection(Projections.projectionList().add(Projections.property("namaAyah"))
						.add(Projections.property("tanggalLahirAyah")).add(Projections.property("namaSiswa")))
				.list();

		for (Object[] dataNamaTgl : namaTglLahirs) {
			String nama = (String) dataNamaTgl[0];
			Date tglLahir = (Date) dataNamaTgl[1];

			String namaSiswa = (String) dataNamaTgl[2];

			System.out.println(
					"namaAyah -> " + nama + ", tanggalLahirAyah -> " + tglLahir + ", nama alumni -> " + namaSiswa);

			List<Siswa> siswas = ConstantValues
					.simpleList(session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
							.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
							.add(Restrictions.isNull("statusKeluar")).add(Restrictions.eq("tanggalLahirAyah", tglLahir))
							.add(Restrictions.ilike("namaAyah", nama, MatchMode.EXACT))
							.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())), Siswa.class);
			String namaSaudara = "";
			for (Siswa siswa : siswas) {
				namaSaudara += namaSaudara.isEmpty() ? siswa.getNamaSiswa() : "," + siswa.getNamaSiswa();
			}

			System.out.println("siswas -> " + siswas);
			for (Siswa siswa : siswas) {

				DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
						session.createCriteria(DiskonSiswaPunyaSiswa.class)
								.add(Restrictions.or(Restrictions.eq("siswa", siswa),
										Restrictions.eq("calonSiswa.id", siswa.getCalonSiswa())))
								.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
						DiskonSiswaPunyaSiswa.class);
				if (diskonSiswaPunyaSiswa == null) {

					diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
					diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
					diskonSiswaPunyaSiswa.setSiswa(siswa);

					diskonSiswaPunyaSiswa.setKeterangan(namaSaudara + " adalah saudara alumni " + namaSiswa);

					session.save(diskonSiswaPunyaSiswa);
					session.flush();
				}

			}
		}

		namaTglLahirs = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
				.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
				.createAlias("statusKeluar", "statusKeluar")
				.add(Restrictions.ilike("statusKeluar.nama", "lulus", MatchMode.ANYWHERE))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("tanggalLahirIbu")).add(Restrictions.isNotNull("namaIbu"))
				.setProjection(Projections.projectionList().add(Projections.property("namaIbu"))
						.add(Projections.property("tanggalLahirIbu")).add(Projections.property("namaSiswa")))
				.list();
		for (Object[] dataNamaTgl : namaTglLahirs) {
			String nama = (String) dataNamaTgl[0];
			Date tglLahir = (Date) dataNamaTgl[1];
			String namaSiswa = (String) dataNamaTgl[2];

			System.out.println(
					"namaIbu -> " + nama + ", tanggalLahirIbu -> " + tglLahir + ", nama alumni -> " + namaSiswa);

			List<Siswa> siswas = ConstantValues
					.simpleList(session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
							.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
							.add(Restrictions.isNull("statusKeluar")).add(Restrictions.eq("tanggalLahirIbu", tglLahir))
							.add(Restrictions.ilike("namaIbu", nama, MatchMode.EXACT))
							.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())), Siswa.class);
			String namaSaudara = "";
			for (Siswa siswa : siswas) {
				namaSaudara += namaSaudara.isEmpty() ? siswa.getNamaSiswa() : "," + siswa.getNamaSiswa();
			}

			System.out.println("siswas -> " + siswas);
			for (Siswa siswa : siswas) {

				DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
						session.createCriteria(DiskonSiswaPunyaSiswa.class)
								.add(Restrictions.or(Restrictions.eq("siswa", siswa),
										Restrictions.eq("calonSiswa.id", siswa.getCalonSiswa())))
								.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
						DiskonSiswaPunyaSiswa.class);
				if (diskonSiswaPunyaSiswa == null) {

					diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
					diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
					diskonSiswaPunyaSiswa.setSiswa(siswa);

					diskonSiswaPunyaSiswa.setKeterangan(namaSaudara + " adalah saudara alumni " + namaSiswa);

					session.save(diskonSiswaPunyaSiswa);
					session.flush();
				}

			}
		}

		List<Object[]> calonSiswas = session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
				.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb")
				.add(Restrictions.eq("gelombangPendaftaranPsb.tahunAjaran", diskonSiswa.getTahunAjaran()))
				.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())).add(Restrictions.ne("namaAyah", ""))
				.add(Restrictions.isNotNull("namaAyah")).add(Restrictions.isNotNull("tanggalLahirAyah"))
				.setProjection(Projections.projectionList().add(Projections.property("id"))
						.add(Projections.property("namaAyah")).add(Projections.property("tanggalLahirAyah"))
						.add(Projections.property("namaSiswa")))
				.list();
		for (Object[] dataNamaTgl : calonSiswas) {
			Long id = (Long) dataNamaTgl[0];
			String nama = (String) dataNamaTgl[1];
			Date tglLahir = (Date) dataNamaTgl[2];
			String namaSiswa = (String) dataNamaTgl[3];

			List<String> namas = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))

					.createAlias("statusKeluar", "statusKeluar")
					.add(Restrictions.ilike("statusKeluar.nama", "lulus", MatchMode.ANYWHERE))

					.add(Restrictions.eq("tanggalLahirAyah", tglLahir))
					.add(Restrictions.ilike("namaAyah", nama, MatchMode.EXACT))
					.add(Restrictions.not(Restrictions.ilike("namaSiswa", namaSiswa, MatchMode.EXACT)))
					.add(Restrictions.ne("sekolah", diskonSiswa.getSekolah()))
					.setProjection(Projections.property("namaSiswa")).list();

			System.out.println("id -> " + id + " nama ayah -> " + nama + ", namaSiswa -> " + namaSiswa
					+ ", tglLahir -> " + tglLahir + " ada " + namas);

			if (!namas.isEmpty()) {
				CalonSiswa calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id);
				if (calonSiswa != null) {

					String namaSaudara = namaSiswa + " adalah saudara alumni ";
					for (String n : namas) {
						namaSaudara += namaSaudara.isEmpty() ? n : "," + n;
					}

					DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
							session.createCriteria(DiskonSiswaPunyaSiswa.class)
									.add(Restrictions.or(Restrictions.eq("calonSiswa", calonSiswa),
											Restrictions.eq("siswa", calonSiswa.getSiswa())))
									.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
							DiskonSiswaPunyaSiswa.class);
					if (diskonSiswaPunyaSiswa == null) {

						diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
						diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
						diskonSiswaPunyaSiswa.setSiswa(calonSiswa.getSiswa());
						diskonSiswaPunyaSiswa.setCalonSiswa(calonSiswa);

						diskonSiswaPunyaSiswa.setKeterangan(namaSaudara);

						session.save(diskonSiswaPunyaSiswa);
						session.flush();
					}

				}
			}
		}

		calonSiswas = session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
				.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb")
				.add(Restrictions.eq("gelombangPendaftaranPsb.tahunAjaran", diskonSiswa.getTahunAjaran()))
				.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())).add(Restrictions.ne("namaIbu", ""))
				.add(Restrictions.isNotNull("namaIbu")).add(Restrictions.isNotNull("tanggalLahirIbu"))
				.setProjection(Projections.projectionList().add(Projections.property("id"))
						.add(Projections.property("namaIbu")).add(Projections.property("tanggalLahirIbu"))
						.add(Projections.property("namaSiswa")))
				.list();
		for (Object[] dataNamaTgl : calonSiswas) {
			Long id = (Long) dataNamaTgl[0];
			String nama = (String) dataNamaTgl[1];
			Date tglLahir = (Date) dataNamaTgl[2];
			String namaSiswa = (String) dataNamaTgl[3];

			List<String> namas = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))

					.createAlias("statusKeluar", "statusKeluar")
					.add(Restrictions.ilike("statusKeluar.nama", "lulus", MatchMode.ANYWHERE))

					.add(Restrictions.eq("tanggalLahirIbu", tglLahir))
					.add(Restrictions.ilike("namaIbu", nama, MatchMode.EXACT))
					.add(Restrictions.not(Restrictions.ilike("namaSiswa", namaSiswa, MatchMode.EXACT)))
					.add(Restrictions.ne("sekolah", diskonSiswa.getSekolah()))
					.setProjection(Projections.property("namaSiswa")).list();

			System.out.println("id -> " + id + " nama ibu -> " + nama + ", namaSiswa -> " + namaSiswa + ", tglLahir -> "
					+ tglLahir + " ada " + namas);

			if (!namas.isEmpty()) {
				CalonSiswa calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id);
				if (calonSiswa != null) {

					String namaSaudara = namaSiswa + " adalah saudara alumni ";
					for (String n : namas) {
						namaSaudara += namaSaudara.isEmpty() ? n : "," + n;
					}

					DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
							session.createCriteria(DiskonSiswaPunyaSiswa.class)
									.add(Restrictions.or(Restrictions.eq("calonSiswa", calonSiswa),
											Restrictions.eq("siswa", calonSiswa.getSiswa())))
									.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
							DiskonSiswaPunyaSiswa.class);
					if (diskonSiswaPunyaSiswa == null) {

						diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
						diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
						diskonSiswaPunyaSiswa.setSiswa(calonSiswa.getSiswa());
						diskonSiswaPunyaSiswa.setCalonSiswa(calonSiswa);

						diskonSiswaPunyaSiswa.setKeterangan(namaSaudara);

						session.save(diskonSiswaPunyaSiswa);
						session.flush();
					}

				}
			}
		}
	}

	/**
	 * Mesin pencari penerima untuk jenis {@link #DISKON_SAUDARA}: mendaftarkan <b>siswa yang
	 * bersaudara dan sama-sama masih bersekolah</b> sebagai calon penerima diskon.
	 *
	 * <p>Kembaran {@link #prosesSaudaraAlumni(DiskonSiswa)}, tetapi sisi "saudara"-nya adalah siswa
	 * <i>aktif</i> ({@code statusKeluar IS NULL}), bukan alumni.</p>
	 *
	 * <h3>Kapan dipanggil</h3>
	 * <p>Hanya dari tombol "Singkronkan Data Diskon Saudara" pada panel rincian penerima. Berjalan
	 * pada thread timer ZK.</p>
	 *
	 * <h3>Cara kerja &mdash; empat tahap</h3>
	 * <ol>
	 *   <li><b>Tahap 1 (jalur ayah).</b> Native SQL mengelompokkan {@code sekolah.siswa} yang masih
	 *       aktif berdasarkan {@code (nama_ayah, tanggal_lahir_ayah)} dan menyisakan kelompok
	 *       beranggota lebih dari satu &mdash; yakni keluarga dengan lebih dari satu anak
	 *       bersekolah. Untuk tiap kelompok, seluruh {@link Siswa} aktif dengan identitas ayah yang
	 *       sama <b>di sekolah aturan diskon ini</b> didaftarkan, dengan keterangan
	 *       "&lt;daftar nama&gt; adalah saudara".</li>
	 *   <li><b>Tahap 2 (jalur ibu).</b> Sama persis, memakai
	 *       {@code (nama_ibu, tanggal_lahir_ibu)}.</li>
	 *   <li><b>Tahap 3 &amp; 4 (calon siswa PSB, jalur ayah lalu ibu).</b> Untuk setiap
	 *       {@link CalonSiswa} pada gelombang PSB bertahun ajaran dan bersekolah sama, dicari
	 *       {@link Siswa} aktif dengan identitas orang tua sama <b>di sekolah LAIN</b> dan bernama
	 *       berbeda; bila ada, calon siswa itu didaftarkan.</li>
	 * </ol>
	 *
	 * <h3>Yang ditulis ke database</h3>
	 * <p>Satu {@link DiskonSiswaPunyaSiswa} per kecocokan baru, {@code save} + {@code flush} satu
	 * per satu, {@code setujui} dibiarkan kosong sehingga belum berdampak pada rupiah sampai
	 * seseorang mencentang "Setujui".</p>
	 *
	 * <h3>Kuirk &amp; bug yang ditemukan</h3>
	 * <ul>
	 *   <li><b>Klausa {@code status_keluar_siswa is null} ditulis dua kali</b> dalam kedua native
	 *       SQL tahap 1 dan 2 (sisa salin-tempel). Tidak mengubah hasil, tetapi menandakan blok itu
	 *       pernah diedit tanpa dibaca ulang.</li>
	 *   <li><b>Bug koma di awal daftar</b> pada tahap 3 &amp; 4, mekanisme persis sama seperti pada
	 *       {@link #prosesSaudaraAlumni(DiskonSiswa)} &mdash; penampung sudah diisi nama calon siswa
	 *       sebelum perulangan penggabungan, sehingga cabang "tanpa koma" tidak pernah terpakai.
	 *       Keterangan yang tersimpan pun tidak memuat kata penjelas apa pun (berbeda dari tahap 1
	 *       &amp; 2 yang menambahkan " adalah saudara"), sehingga isinya sekadar deretan nama.</li>
	 *   <li><b>Native SQL tahap 1 &amp; 2 memindai seluruh instalasi</b> tanpa penyaring
	 *       sekolah/yayasan; penyaringan tenant baru muncul pada query Criteria berikutnya.</li>
	 *   <li><b>Anak tunggal tetap bisa terjaring.</b> Kelompok {@code having count(*) &gt; 1}
	 *       dihitung lintas seluruh instalasi, sedangkan pendaftarannya dibatasi ke satu sekolah
	 *       &mdash; sehingga seorang anak yang saudaranya bersekolah di unit lain tetap masuk
	 *       daftar penerima "Diskon Saudara" walau di sekolah ini ia sendirian.</li>
	 *   <li>Setiap iterasi mencetak nama orang tua dan daftar object {@link Siswa} ke
	 *       {@code System.out} (PII, dan {@code toString()} siswa ikut membebani log).</li>
	 * </ul>
	 *
	 * <h3>Catatan hak akses (temuan)</h3>
	 * <p>Tombol pemanggilnya tidak memeriksa hak apa pun.</p>
	 *
	 * @param diskonSiswa aturan diskon yang sedang disinkronkan; menyediakan sekolah dan tahun
	 *                    ajaran pembatas serta menjadi induk baris penerima yang dibuat. Tidak boleh
	 *                    {@code null}.
	 * @see #prosesSaudaraAlumni(DiskonSiswa)
	 */
	@SuppressWarnings("unchecked")
	public static void prosesSaudara(DiskonSiswa diskonSiswa) {
		Session session = HibernateUtil.currentSession();
		List<Object[]> namaTglLahirs = session.createSQLQuery(
				"select nama_ayah,tanggal_lahir_ayah from sekolah.siswa where status_keluar_siswa is null and nama_ayah != '' and nama_ayah is not null and tanggal_lahir_ayah is not null and status_keluar_siswa is null group by nama_ayah,tanggal_lahir_ayah having count(*)>1;")
				.list();
		for (Object[] dataNamaTgl : namaTglLahirs) {
			String nama = (String) dataNamaTgl[0];
			Date tglLahir = (Date) dataNamaTgl[1];

			System.out.println("nama -> " + nama + ", tglLahir -> " + tglLahir);

			List<Siswa> siswas = ConstantValues
					.simpleList(session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
							.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
							.add(Restrictions.isNull("statusKeluar")).add(Restrictions.eq("tanggalLahirAyah", tglLahir))
							.add(Restrictions.ilike("namaAyah", nama, MatchMode.EXACT))
							.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())), Siswa.class);
			String namaSaudara = "";
			for (Siswa siswa : siswas) {
				namaSaudara += namaSaudara.isEmpty() ? siswa.getNamaSiswa() : "," + siswa.getNamaSiswa();
			}

			System.out.println("siswas -> " + siswas);
			for (Siswa siswa : siswas) {

				DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
						session.createCriteria(DiskonSiswaPunyaSiswa.class)
								.add(Restrictions.or(Restrictions.eq("siswa", siswa),
										Restrictions.eq("calonSiswa.id", siswa.getCalonSiswa())))
								.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
						DiskonSiswaPunyaSiswa.class);
				if (diskonSiswaPunyaSiswa == null) {

					diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
					diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
					diskonSiswaPunyaSiswa.setSiswa(siswa);

					diskonSiswaPunyaSiswa.setKeterangan(namaSaudara + " adalah saudara");

					session.save(diskonSiswaPunyaSiswa);
					session.flush();
				}

			}
		}

		namaTglLahirs = session.createSQLQuery(
				"select nama_ibu,tanggal_lahir_ibu from sekolah.siswa where status_keluar_siswa is null and nama_ibu != '' and nama_ibu is not null and tanggal_lahir_ibu is not null and status_keluar_siswa is null group by nama_ibu,tanggal_lahir_ibu having count(*)>1;")
				.list();
		for (Object[] dataNamaTgl : namaTglLahirs) {
			String nama = (String) dataNamaTgl[0];
			Date tglLahir = (Date) dataNamaTgl[1];

			System.out.println("nama -> " + nama + ", tglLahir -> " + tglLahir);

			List<Siswa> siswas = ConstantValues
					.simpleList(session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
							.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
							.add(Restrictions.isNull("statusKeluar")).add(Restrictions.eq("tanggalLahirIbu", tglLahir))
							.add(Restrictions.ilike("namaIbu", nama, MatchMode.EXACT))
							.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())), Siswa.class);
			String namaSaudara = "";
			for (Siswa siswa : siswas) {
				namaSaudara += namaSaudara.isEmpty() ? siswa.getNamaSiswa() : "," + siswa.getNamaSiswa();
			}

			System.out.println("siswas -> " + siswas);
			for (Siswa siswa : siswas) {

				DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
						session.createCriteria(DiskonSiswaPunyaSiswa.class)
								.add(Restrictions.or(Restrictions.eq("siswa", siswa),
										Restrictions.eq("calonSiswa.id", siswa.getCalonSiswa())))
								.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
						DiskonSiswaPunyaSiswa.class);
				if (diskonSiswaPunyaSiswa == null) {

					diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
					diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
					diskonSiswaPunyaSiswa.setSiswa(siswa);

					diskonSiswaPunyaSiswa.setKeterangan(namaSaudara + " adalah saudara");

					session.save(diskonSiswaPunyaSiswa);
					session.flush();
				}

			}
		}

		List<Object[]> calonSiswas = session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
				.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb")
				.add(Restrictions.eq("gelombangPendaftaranPsb.tahunAjaran", diskonSiswa.getTahunAjaran()))
				.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())).add(Restrictions.ne("namaAyah", ""))
				.add(Restrictions.isNotNull("namaAyah")).add(Restrictions.isNotNull("tanggalLahirAyah"))
				.setProjection(Projections.projectionList().add(Projections.property("id"))
						.add(Projections.property("namaAyah")).add(Projections.property("tanggalLahirAyah"))
						.add(Projections.property("namaSiswa")))
				.list();
		for (Object[] dataNamaTgl : calonSiswas) {
			Long id = (Long) dataNamaTgl[0];
			String nama = (String) dataNamaTgl[1];
			Date tglLahir = (Date) dataNamaTgl[2];
			String namaSiswa = (String) dataNamaTgl[3];

			List<String> namas = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.isNull("statusKeluar")).add(Restrictions.eq("tanggalLahirAyah", tglLahir))
					.add(Restrictions.ilike("namaAyah", nama, MatchMode.EXACT))
					.add(Restrictions.not(Restrictions.ilike("namaSiswa", namaSiswa, MatchMode.EXACT)))
					.add(Restrictions.ne("sekolah", diskonSiswa.getSekolah()))
					.setProjection(Projections.property("namaSiswa")).list();

			System.out.println("id -> " + id + " nama ayah -> " + nama + ", namaSiswa -> " + namaSiswa
					+ ", tglLahir -> " + tglLahir + " ada " + namas);

			if (!namas.isEmpty()) {
				CalonSiswa calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id);
				if (calonSiswa != null) {

					String namaSaudara = namaSiswa;
					for (String n : namas) {
						namaSaudara += namaSaudara.isEmpty() ? n : "," + n;
					}

					DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
							session.createCriteria(DiskonSiswaPunyaSiswa.class)
									.add(Restrictions.or(Restrictions.eq("calonSiswa", calonSiswa),
											Restrictions.eq("siswa", calonSiswa.getSiswa())))
									.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
							DiskonSiswaPunyaSiswa.class);
					if (diskonSiswaPunyaSiswa == null) {

						diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
						diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
						diskonSiswaPunyaSiswa.setSiswa(calonSiswa.getSiswa());
						diskonSiswaPunyaSiswa.setCalonSiswa(calonSiswa);

						diskonSiswaPunyaSiswa.setKeterangan(namaSaudara);

						session.save(diskonSiswaPunyaSiswa);
						session.flush();
					}

				}
			}
		}

		calonSiswas = session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
				.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb")
				.add(Restrictions.eq("gelombangPendaftaranPsb.tahunAjaran", diskonSiswa.getTahunAjaran()))
				.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())).add(Restrictions.ne("namaIbu", ""))
				.add(Restrictions.isNotNull("namaIbu")).add(Restrictions.isNotNull("tanggalLahirIbu"))
				.setProjection(Projections.projectionList().add(Projections.property("id"))
						.add(Projections.property("namaIbu")).add(Projections.property("tanggalLahirIbu"))
						.add(Projections.property("namaSiswa")))
				.list();
		for (Object[] dataNamaTgl : calonSiswas) {
			Long id = (Long) dataNamaTgl[0];
			String nama = (String) dataNamaTgl[1];
			Date tglLahir = (Date) dataNamaTgl[2];
			String namaSiswa = (String) dataNamaTgl[3];

			List<String> namas = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.isNull("statusKeluar")).add(Restrictions.eq("tanggalLahirIbu", tglLahir))
					.add(Restrictions.ilike("namaIbu", nama, MatchMode.EXACT))
					.add(Restrictions.not(Restrictions.ilike("namaSiswa", namaSiswa, MatchMode.EXACT)))
					.add(Restrictions.ne("sekolah", diskonSiswa.getSekolah()))
					.setProjection(Projections.property("namaSiswa")).list();

			System.out.println("id -> " + id + " nama ibu -> " + nama + ", namaSiswa -> " + namaSiswa + ", tglLahir -> "
					+ tglLahir + " ada " + namas);

			if (!namas.isEmpty()) {
				CalonSiswa calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id);
				if (calonSiswa != null) {

					String namaSaudara = namaSiswa;
					for (String n : namas) {
						namaSaudara += namaSaudara.isEmpty() ? n : "," + n;
					}

					DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
							session.createCriteria(DiskonSiswaPunyaSiswa.class)
									.add(Restrictions.or(Restrictions.eq("calonSiswa", calonSiswa),
											Restrictions.eq("siswa", calonSiswa.getSiswa())))
									.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
							DiskonSiswaPunyaSiswa.class);
					if (diskonSiswaPunyaSiswa == null) {

						diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
						diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
						diskonSiswaPunyaSiswa.setSiswa(calonSiswa.getSiswa());
						diskonSiswaPunyaSiswa.setCalonSiswa(calonSiswa);

						diskonSiswaPunyaSiswa.setKeterangan(namaSaudara);

						session.save(diskonSiswaPunyaSiswa);
						session.flush();
					}

				}
			}
		}
	}

	/**
	 * Mesin pencari penerima untuk jenis {@link #DISKON_SEMUA} ("Diskon Semua Siswa"): mendaftarkan
	 * <b>seluruh pendaftar PSB</b> pada gelombang, sekolah, tahun ajaran, dan rentang tanggal
	 * pendaftaran yang cocok.
	 *
	 * <h3>Peringatan penamaan: yang terjaring HANYA calon siswa</h3>
	 * <p>Meski konstantanya berbunyi "Diskon Semua <i>Siswa</i>", mesin ini <b>tidak pernah
	 * menyentuh tabel {@link Siswa}</b>. Ia hanya membaca {@link CalonSiswa} yang punya gelombang
	 * pendaftaran PSB. Siswa yang sudah bersekolah tidak akan pernah didaftarkan lewat tombol ini
	 * &mdash; harus lewat tombol "Ambil Siswa" secara manual. Baris {@link DiskonSiswaPunyaSiswa}
	 * yang dibuat tetap menautkan {@code siswa} sekaligus {@code calonSiswa} bila calon siswa itu
	 * sudah punya rekaman siswa ({@code calonSiswa.getSiswa()}).</p>
	 *
	 * <h3>Satu-satunya pembaca masa berlaku diskon</h3>
	 * <p>Method ini adalah satu-satunya tempat {@link #getDiskonMulai()}/{@link #getDiskonSampai()}
	 * benar-benar dipakai. Keempat kombinasi ditangani secara eksplisit: keduanya terisi menjadi
	 * {@code between("tanggalPendaftaran", mulai, sampai)}; hanya "mulai" menjadi {@code ge};
	 * hanya "sampai" menjadi {@code le}; keduanya kosong menjadi
	 * {@code Restrictions.sqlRestriction("true")} alias tanpa batas. Perlu dicatat bahwa yang
	 * disaring adalah <b>tanggal pendaftaran calon siswa</b>, bukan tanggal tagihan &mdash;
	 * rentang ini sama sekali tidak membatasi kapan potongan berlaku pada {@link Tagihan}.</p>
	 *
	 * <h3>Cara kerja</h3>
	 * <ol>
	 *   <li>Ambil id seluruh {@link CalonSiswa} yang punya gelombang PSB, tanggal pendaftarannya
	 *       masuk rentang, sekolahnya = {@link #getSekolah()}, dan
	 *       {@code gelombangPendaftaranPsb.tahunAjaran} = {@link #getTahunAjaran()}.</li>
	 *   <li>Muat ulang tiap calon siswa lewat {@code ConstantValues.ambil(..)}.</li>
	 *   <li>Bila belum ada {@link DiskonSiswaPunyaSiswa} untuk pasangan (calon siswa atau siswanya,
	 *       aturan diskon ini), buat satu baris baru dan {@code flush}.</li>
	 * </ol>
	 * <p>Berbeda dari mesin lain, baris yang dibuat di sini <b>tidak diberi keterangan apa pun</b>
	 * (tidak ada alasan pemberian diskon yang bisa dilihat kembali operator).</p>
	 *
	 * <h3>Kuirk yang ditemukan</h3>
	 * <ul>
	 *   <li><b>Proyeksi berisi tiga kolom yang tidak pernah dipakai.</b> Query mengambil
	 *       {@code namaIbu}, {@code tanggalLahirIbu}, dan {@code namaSiswa} bersama {@code id},
	 *       tetapi perulangannya hanya membaca indeks {@code [0]}. Sisa salin-tempel dari
	 *       {@link #prosesSaudara(DiskonSiswa)} &mdash; tidak berbahaya, hanya memboroskan
	 *       pengambilan data.</li>
	 *   <li><b>Tanpa batas jumlah.</b> Pada gelombang PSB besar, satu klik dapat menyisipkan ribuan
	 *       baris penerima dengan {@code flush()} per baris.</li>
	 *   <li>Seperti mesin lain, {@code setujui} dibiarkan kosong sehingga belum ada rupiah yang
	 *       berubah sampai dicentang.</li>
	 * </ul>
	 *
	 * <h3>Catatan hak akses (temuan)</h3>
	 * <p>Tombol pemanggilnya tidak memeriksa hak apa pun.</p>
	 *
	 * @param diskonSiswa aturan diskon yang sedang disinkronkan; menyediakan sekolah, tahun ajaran,
	 *                    dan rentang tanggal pembatas serta menjadi induk baris penerima yang
	 *                    dibuat. Tidak boleh {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public static void prosesSemua(DiskonSiswa diskonSiswa) {

		Session session = HibernateUtil.currentSession();
		List<Object[]> calonSiswas = session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))

				.add(diskonSiswa.getDiskonMulai() != null && diskonSiswa.getDiskonSampai() != null
						? Restrictions.between("tanggalPendaftaran", diskonSiswa.getDiskonMulai(),
								diskonSiswa.getDiskonSampai())
						: diskonSiswa.getDiskonMulai() != null && diskonSiswa.getDiskonSampai() == null
								? Restrictions.ge("tanggalPendaftaran", diskonSiswa.getDiskonMulai())
								: diskonSiswa.getDiskonMulai() == null && diskonSiswa.getDiskonSampai() != null
										? Restrictions.le("tanggalPendaftaran", diskonSiswa.getDiskonSampai())
										: Restrictions.sqlRestriction("true"))

				.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah()))
				.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb")
				.add(Restrictions.eq("gelombangPendaftaranPsb.tahunAjaran", diskonSiswa.getTahunAjaran()))
				.setProjection(Projections.projectionList().add(Projections.property("id"))
						.add(Projections.property("namaIbu")).add(Projections.property("tanggalLahirIbu"))
						.add(Projections.property("namaSiswa")))
				.list();
		for (Object[] dataNamaTgl : calonSiswas) {
			Long id = (Long) dataNamaTgl[0];
			CalonSiswa calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id);
			if (calonSiswa != null) {

				DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
						session.createCriteria(DiskonSiswaPunyaSiswa.class)
								.add(Restrictions.or(Restrictions.eq("calonSiswa", calonSiswa),
										Restrictions.eq("siswa", calonSiswa.getSiswa())))
								.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
						DiskonSiswaPunyaSiswa.class);
				if (diskonSiswaPunyaSiswa == null) {

					diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
					diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
					diskonSiswaPunyaSiswa.setSiswa(calonSiswa.getSiswa());
					diskonSiswaPunyaSiswa.setCalonSiswa(calonSiswa);

					session.save(diskonSiswaPunyaSiswa);
					session.flush();
				}

			}

		}

	}

	/**
	 * Mesin pencari penerima untuk jenis {@link #DISKON_ANAK_PEGAWAI_TETAP} dan
	 * {@link #DISKON_ANAK_PEGAWAI_HONORER}: mendaftarkan <b>siswa yang salah satu orang tuanya
	 * adalah pegawai</b> dengan tipe masa kerja tertentu.
	 *
	 * <h3>Kapan dipanggil</h3>
	 * <p>Dari tombol "Singkronkan Data &lt;jenis&gt;" pada panel rincian penerima, dengan
	 * {@code tipeMasaKerja} yang dipilih {@code DiskonSiswaPunyaSiswaHelper} berdasarkan
	 * {@link #getJenis()}: {@link TipeMasaKerja#Tetap} untuk "Diskon Anak Pegawai Tetap" dan
	 * {@link TipeMasaKerja#Honorer} untuk "Diskon Anak Pegawai Honorer". Berjalan pada thread timer
	 * ZK.</p>
	 *
	 * <h3>Cara kerja</h3>
	 * <ol>
	 *   <li>Ambil pasangan {@code (nama, tanggallahir)} seluruh {@link Pegawai} yang masih aktif
	 *       ({@code aktif = true} atau {@code aktif IS NULL}) dan bertipe masa kerja yang
	 *       diminta.</li>
	 *   <li>Untuk setiap pegawai, cari seluruh {@link Siswa} yang masih bersekolah
	 *       ({@code statusKeluar IS NULL}) di {@link #getSekolah()} aturan ini yang
	 *       <b>nama ayahnya atau nama ibunya</b> sama persis dengan nama pegawai <b>dan</b> tanggal
	 *       lahir orang tua bersangkutan sama dengan tanggal lahir pegawai.</li>
	 *   <li>Daftarkan setiap siswa yang belum punya baris penerima pada aturan diskon ini, dengan
	 *       keterangan "Merupakan anak dari karyawan &lt;nama pegawai&gt;".</li>
	 * </ol>
	 *
	 * <h3>Kuirk &amp; batasan yang perlu diketahui</h3>
	 * <ul>
	 *   <li><b>Daftar pegawai tidak dibatasi tenant.</b> Query {@link Pegawai} tidak menyaring
	 *       sekolah maupun yayasan, jadi ia memindai seluruh pegawai pada instalasi. Pembatas tenant
	 *       hanya ada di sisi siswa. Pada instalasi multi-yayasan, anak pegawai yayasan lain yang
	 *       kebetulan bersekolah di sini akan ikut terjaring.</li>
	 *   <li><b>{@code tipeMasaKerja} bernilai {@code null} berarti SEMUA pegawai.</b> Kondisi
	 *       filternya berubah menjadi {@code Restrictions.sqlRestriction("true")} &mdash; bukan
	 *       "tanpa hasil". Ini <b>gagal-terbuka yang bisa benar-benar terjadi</b>, bukan sekadar
	 *       risiko teoretis: {@link TipeMasaKerja#Tetap} dan {@link TipeMasaKerja#Honorer} adalah
	 *       field {@code static} yang baru terisi ketika {@code TipeMasaKerja.initData(..)}
	 *       dijalankan saat inisialisasi aplikasi. Selama keduanya masih {@code null}, tombol
	 *       "Diskon Anak Pegawai Tetap" akan mendaftarkan anak <b>seluruh pegawai</b> tanpa
	 *       memandang tipe masa kerja.</li>
	 *   <li><b>Tipe masa kerja "Semi Tetap" tidak tercakup jenis diskon mana pun.</b>
	 *       {@link #JENIS} hanya mengenal varian Tetap dan Honorer, sedangkan
	 *       {@code TipeMasaKerja} juga menyeed {@code Semi Tetap} dan {@code Pengalaman Kerja} pada
	 *       instalasi baru &mdash; anak pegawai bertipe itu tidak akan pernah terjaring
	 *       otomatis.</li>
	 *   <li><b>Pencocokan orang tua hanya nama + tanggal lahir.</b> Pegawai yang tanggal lahirnya
	 *       tidak tercatat sudah tersaring oleh {@code isNotNull("tanggallahir")}, tetapi siswa yang
	 *       tanggal lahir orang tuanya tidak diisi tidak akan pernah cocok &mdash; anak pegawai bisa
	 *       terlewat diam-diam tanpa pesan apa pun.</li>
	 *   <li><b>Tipe masa kerja tidak "dikunci" pada baris penerima.</b> Setelah baris terbentuk,
	 *       diskon tetap melekat walaupun pegawainya berhenti atau berubah tipe masa kerja;
	 *       menjalankan ulang tombol ini tidak akan menghapus baris yang tidak lagi memenuhi
	 *       syarat.</li>
	 *   <li>Setiap iterasi mencetak nama pegawai + tanggal lahirnya dan daftar siswa ke
	 *       {@code System.out} (PII pegawai dan anak).</li>
	 * </ul>
	 *
	 * <h3>Catatan hak akses (temuan)</h3>
	 * <p>Tombol pemanggilnya tidak memeriksa hak apa pun.</p>
	 *
	 * @param diskonSiswa   aturan diskon yang sedang disinkronkan; menyediakan sekolah pembatas dan
	 *                      menjadi induk baris penerima yang dibuat. Tidak boleh {@code null}.
	 * @param tipeMasaKerja tipe masa kerja pegawai yang berhak (mis. {@link TipeMasaKerja#Tetap}
	 *                      atau {@link TipeMasaKerja#Honorer}); <b>{@code null} berarti tanpa
	 *                      penyaringan sama sekali</b>, yakni seluruh pegawai aktif.
	 */
	@SuppressWarnings("unchecked")
	public static void prosesAnakPegawai(DiskonSiswa diskonSiswa, TipeMasaKerja tipeMasaKerja) {
		Session session = HibernateUtil.currentSession();
		List<Object[]> namaTglLahirs = session.createCriteria(Pegawai.class)
				.add(tipeMasaKerja == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tipeMasaKerja", tipeMasaKerja))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("nama")).add(Restrictions.isNotNull("tanggallahir"))
				.setProjection(Projections.projectionList().add(Projections.property("nama"))
						.add(Projections.property("tanggallahir")))
				.list();

		for (Object[] dataNamaTgl : namaTglLahirs) {
			String nama = (String) dataNamaTgl[0];
			Date tglLahir = (Date) dataNamaTgl[1];

			System.out.println("nama -> " + nama + ", tglLahir -> " + tglLahir);

			List<Siswa> siswas = ConstantValues.simpleList(session.createCriteria(Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah")).add(Restrictions.isNull("statusKeluar"))

					.add(Restrictions.or(

							Restrictions.and(Restrictions.ilike("namaAyah", nama, MatchMode.EXACT),
									Restrictions.eq("tanggalLahirAyah", tglLahir)),

							Restrictions.and(Restrictions.ilike("namaIbu", nama, MatchMode.EXACT),
									Restrictions.eq("tanggalLahirIbu", tglLahir))))

					.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())), Siswa.class);
			String namaSaudara = "Merupakan anak dari karyawan " + nama;

			System.out.println("siswas -> " + siswas);
			for (Siswa siswa : siswas) {

				DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
						session.createCriteria(DiskonSiswaPunyaSiswa.class)
								.add(Restrictions.or(Restrictions.eq("siswa", siswa),
										Restrictions.eq("calonSiswa.id", siswa.getCalonSiswa())))
								.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
						DiskonSiswaPunyaSiswa.class);
				if (diskonSiswaPunyaSiswa == null) {

					diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
					diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
					diskonSiswaPunyaSiswa.setSiswa(siswa);

					diskonSiswaPunyaSiswa.setKeterangan(namaSaudara);

					session.save(diskonSiswaPunyaSiswa);
					session.flush();
				}

			}

		}
	}

	/**
	 * Mesin pencari penerima untuk jenis {@link #DISKON_ANAK_ALUMNI}: mendaftarkan <b>siswa yang
	 * ayah atau ibunya merupakan alumni</b> sekolah di lingkungan yayasan.
	 *
	 * <h3>Kapan dipanggil</h3>
	 * <p>Dari tombol "Singkronkan Data Diskon Anak Alumni" pada panel rincian penerima. Berjalan
	 * pada thread timer ZK.</p>
	 *
	 * <h3>Cara kerja</h3>
	 * <ol>
	 *   <li>Ambil pasangan {@code (namaSiswa, tanggalLahir)} seluruh {@link Siswa} berstatus alumni
	 *       &mdash; {@code statusKeluar.nama} mengandung kata "lulus" dan barisnya masih
	 *       {@code aktif}/{@code aktif IS NULL}. Daftar alumni ini <b>tidak dibatasi sekolah</b>,
	 *       jadi mencakup seluruh instalasi.</li>
	 *   <li>Untuk setiap alumni, cari {@link Siswa} yang masih bersekolah di {@link #getSekolah()}
	 *       aturan ini yang <b>nama ayahnya atau nama ibunya</b> sama persis dengan nama alumni
	 *       <b>dan</b> tanggal lahir orang tua bersangkutan sama dengan tanggal lahir alumni.</li>
	 *   <li>Daftarkan setiap kecocokan baru dengan keterangan "Merupakan anak dari alumni
	 *       &lt;nama alumni&gt;".</li>
	 * </ol>
	 * <p>Perhatikan bahwa struktur langkah 2 dan 3 identik dengan
	 * {@link #prosesAnakPegawai(DiskonSiswa, TipeMasaKerja)} &mdash; yang berbeda hanya sumber
	 * "orang tua"-nya ({@link Siswa} alumni versus {@link Pegawai}) dan kata pada keterangan.</p>
	 *
	 * <h3>Kuirk &amp; batasan yang perlu diketahui</h3>
	 * <ul>
	 *   <li><b>Salah satu penyaring digandakan.</b> Kriteria alumni memuat
	 *       {@code isNotNull("namaSiswa")} dua kali (sekali di awal, sekali menjelang proyeksi);
	 *       tidak mengubah hasil, tetapi menandakan blok salin-tempel yang tidak dibaca ulang.</li>
	 *   <li><b>Definisi "lulus" berbasis substring</b> &mdash; status keluar apa pun yang memuat
	 *       kata itu ikut terjaring.</li>
	 *   <li><b>Kunci pencocokan hanya nama + tanggal lahir</b>, dengan risiko positif palsu maupun
	 *       negatif palsu yang sama seperti mesin lainnya; tanggal lahir orang tua yang tidak
	 *       tercatat membuat anak alumni tidak pernah ditemukan.</li>
	 *   <li><b>Riskan pada instalasi lama.</b> Karena daftar alumninya lintas sekolah dan tanpa
	 *       batas jumlah, method ini adalah yang paling berat di antara enam mesin: query bersarang
	 *       sekali per alumni.</li>
	 *   <li>Setiap iterasi mencetak nama + tanggal lahir alumni dan daftar siswa ke
	 *       {@code System.out} (PII).</li>
	 * </ul>
	 *
	 * <h3>Catatan hak akses (temuan)</h3>
	 * <p>Tombol pemanggilnya tidak memeriksa hak apa pun.</p>
	 *
	 * @param diskonSiswa aturan diskon yang sedang disinkronkan; menyediakan sekolah pembatas dan
	 *                    menjadi induk baris penerima yang dibuat. Tidak boleh {@code null}.
	 * @see #prosesAlumni(DiskonSiswa)
	 */
	@SuppressWarnings("unchecked")
	public static void prosesAnakAlumni(DiskonSiswa diskonSiswa) {
		Session session = HibernateUtil.currentSession();
		List<Object[]> namaTglLahirs = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
				.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
				.createAlias("statusKeluar", "statusKeluar")
				.add(Restrictions.ilike("statusKeluar.nama", "lulus", MatchMode.ANYWHERE))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("tanggalLahir")).add(Restrictions.isNotNull("namaSiswa"))
				.setProjection(Projections.projectionList().add(Projections.property("namaSiswa"))
						.add(Projections.property("tanggalLahir")))
				.list();

		for (Object[] dataNamaTgl : namaTglLahirs) {
			String nama = (String) dataNamaTgl[0];
			Date tglLahir = (Date) dataNamaTgl[1];

			System.out.println("nama -> " + nama + ", tglLahir -> " + tglLahir);

			List<Siswa> siswas = ConstantValues.simpleList(session.createCriteria(Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah")).add(Restrictions.isNull("statusKeluar"))

					.add(Restrictions.or(

							Restrictions.and(Restrictions.ilike("namaAyah", nama, MatchMode.EXACT),
									Restrictions.eq("tanggalLahirAyah", tglLahir)),

							Restrictions.and(Restrictions.ilike("namaIbu", nama, MatchMode.EXACT),
									Restrictions.eq("tanggalLahirIbu", tglLahir))))

					.add(Restrictions.eq("sekolah", diskonSiswa.getSekolah())), Siswa.class);
			String namaSaudara = "Merupakan anak dari alumni " + nama;

			System.out.println("siswas -> " + siswas);
			for (Siswa siswa : siswas) {

				DiskonSiswaPunyaSiswa diskonSiswaPunyaSiswa = (DiskonSiswaPunyaSiswa) ConstantValues.simpleObject(
						session.createCriteria(DiskonSiswaPunyaSiswa.class)
								.add(Restrictions.or(Restrictions.eq("siswa", siswa),
										Restrictions.eq("calonSiswa.id", siswa.getCalonSiswa())))
								.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1),
						DiskonSiswaPunyaSiswa.class);
				if (diskonSiswaPunyaSiswa == null) {

					diskonSiswaPunyaSiswa = new DiskonSiswaPunyaSiswa();
					diskonSiswaPunyaSiswa.setDiskonSiswa(diskonSiswa);
					diskonSiswaPunyaSiswa.setSiswa(siswa);

					diskonSiswaPunyaSiswa.setKeterangan(namaSaudara);

					session.save(diskonSiswaPunyaSiswa);
					session.flush();
				}

			}

		}
	}

	/**
	 * Saklar <b>ke mana nilai diskon dialirkan</b>: memotong tagihan siswa langsung ({@code true},
	 * bawaan) atau justru dibelokkan ke alur pembayaran/transfer ({@code false}).
	 *
	 * <p>Dibaca {@code TagihanDiskonSiswaHelper.diskonTidakMemotongTagihan(Tagihan)} &mdash; yang
	 * mengembalikan {@code true} justru ketika saklar ini {@code false}. Bila diskon "tidak
	 * memotong tagihan", {@code DiskonSiswaSyncHelper.sinkronkanPenerima(..)} dan
	 * {@code TagihanUtil}/{@code TagihanUtilCalonSiswa} memanggil
	 * {@code DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan)}, sehingga nilai diskon masuk
	 * ke <b>modul akuntansi sebagai pengajuan transfer/pembayaran</b> alih-alih mengurangi angka
	 * yang harus dibayar siswa. Itulah pola "diskon dibayarkan pihak ketiga/yayasan", dan itu pula
	 * yang membuat tombol "Kirimkan Diskon Ke Pembayaran" pada panel rincian <b>hanya muncul ketika
	 * saklar ini {@code false}</b>.</p>
	 *
	 * <p>Perhatikan bahwa dua metode baca dipakai bergantian di
	 * {@code diskonTidakMemotongTagihan(..)}: bila pembacaan pertama melempar exception (proxy
	 * detached), helper itu membuka {@code openSession()} baru dan mengulang. Bila keduanya gagal,
	 * hasilnya {@code false} &mdash; <b>fail-safe ke arah "diskon memotong tagihan"</b>, sehingga
	 * kegagalan teknis tidak memunculkan pengajuan transfer palsu.</p>
	 *
	 * <p>Menormalkan {@code null} menjadi {@code true}; nilai hasil normalisasi ikut tertulis ke
	 * database pada flush berikutnya. Di UI, saklar ini adalah checkbox "Memotong Tagihan" pada
	 * setiap baris grid {@code DiskonSiswaAction} &mdash; dinonaktifkan bila pengguna tidak punya
	 * hak UPDATE, dan tersimpan langsung tanpa tombol Simpan.</p>
	 *
	 * @return {@code true} bila diskon memotong tagihan siswa secara langsung (termasuk bila
	 *         kolomnya masih {@code null})
	 */
	public Boolean getMemotongTagihan() {
		return memotongTagihan == null ? true : memotongTagihan;
	}

	/**
	 * Menyetel saklar "memotong tagihan".
	 *
	 * <p>Mengubah nilai ini mengubah <b>arah aliran uang</b> aturan diskon (memotong tagihan versus
	 * menjadi pengajuan transfer). Tagihan yang sudah terlanjur dipotong tidak dipulihkan otomatis
	 * saat saklar dibalik; perlu sinkronisasi ulang.</p>
	 *
	 * @param memotongTagihan {@code true} untuk memotong tagihan langsung, {@code false} untuk
	 *                        mengalirkannya ke pembayaran/transfer; {@code null} dibaca sebagai
	 *                        {@code true}
	 */
	public void setMemotongTagihan(Boolean memotongTagihan) {
		this.memotongTagihan = memotongTagihan;
	}

	/**
	 * Awal masa berlaku diskon (kolom {@code diskonMulai}, presisi {@code DATE}).
	 *
	 * <p><b>Cakupannya jauh lebih sempit daripada yang terkesan dari layar.</b> Meski grid
	 * {@code DiskonSiswaAction} menampilkannya sebagai kolom "Masa Berlaku", satu-satunya kode yang
	 * benar-benar membacanya adalah {@link #prosesSemua(DiskonSiswa)}, yang memakainya untuk
	 * menyaring {@code CalonSiswa.tanggalPendaftaran}. Mesin diskon lainnya
	 * ({@code prosesAlumni}, {@code prosesSaudara}, {@code prosesAnakPegawai}, dan seterusnya)
	 * <b>mengabaikan rentang tanggal ini sepenuhnya</b>, begitu pula
	 * {@code DiskonSiswaSyncHelper} dan {@code TagihanDiskonSiswaHelper} yang menulis potongan ke
	 * {@link Tagihan}. Dengan kata lain: <b>masa berlaku ini tidak menghentikan diskon yang sudah
	 * berjalan</b>; untuk menghentikannya, matikan {@link #getAktif()}.</p>
	 *
	 * @return tanggal awal berlaku, atau {@code null} bila tidak dibatasi
	 */
	@Temporal(TemporalType.DATE)
	public Date getDiskonMulai() {
		return diskonMulai;
	}

	/**
	 * Menyetel awal masa berlaku diskon.
	 *
	 * @param diskonMulai tanggal awal; {@code null} berarti tanpa batas bawah
	 */
	public void setDiskonMulai(Date diskonMulai) {
		this.diskonMulai = diskonMulai;
	}

	/**
	 * Akhir masa berlaku diskon (kolom {@code diskonSampai}, presisi {@code DATE}).
	 *
	 * <p>Berpasangan dengan {@link #getDiskonMulai()} dan memiliki keterbatasan yang persis sama:
	 * hanya {@link #prosesSemua(DiskonSiswa)} yang membacanya. Keempat kombinasi
	 * terisi/{@code null} ditangani di sana &mdash; keduanya terisi menjadi {@code between},
	 * salah satu saja menjadi {@code &gt;=}/{@code &lt;=}, keduanya {@code null} menjadi "tanpa
	 * batas".</p>
	 *
	 * @return tanggal akhir berlaku, atau {@code null} bila tidak dibatasi
	 */
	@Temporal(TemporalType.DATE)
	public Date getDiskonSampai() {
		return diskonSampai;
	}

	/**
	 * Menyetel akhir masa berlaku diskon.
	 *
	 * @param diskonSampai tanggal akhir; {@code null} berarti tanpa batas atas
	 */
	public void setDiskonSampai(Date diskonSampai) {
		this.diskonSampai = diskonSampai;
	}
}
