package ais.database.model.sirs;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Profil farmasi per {@link ItemMedis} -- varian "POS Apotik" (FASE A).
 *
 * <p>ENTITY BARU (bukan kolom tambahan di ItemMedis) DISENGAJA: ItemMedis ber-{@code @Audited}
 * dan hbm2ddl TIDAK menyinkronkan kolom baru ke tabel audit {@code new_audit.item_medis__audit}
 * (lihat peringatan operasional di hibernate.cfg.xml) -- kolom baru di sana bisa menggagalkan
 * INSERT audit dan me-rollback simpan item. Tabel baru + tabel auditnya tercipta utuh sekaligus.</p>
 *
 * <p>{@code golonganObat} = konstanta teks tervalidasi ({@link #GOLONGAN_NARKOTIKA} dst.);
 * NARKOTIKA/PSIKOTROPIKA menuntut register penjualan ({@link ApotikNarkotikaLog}) -- transaksi
 * DITAHAN server bila register tidak bisa dibuat, bukan dilanjutkan diam-diam. {@code lasa}
 * (Look-Alike Sound-Alike) murni penanda tampilan kasir -- obat mirip ditampilkan berbeda.</p>
 *
 * <h3>Kedudukan: satu tempat yang menentukan seberapa berbahaya sebuah obat</h3>
 *
 * <p>Entity ini kecil tetapi berbobot besar. Ia satu-satunya tempat yang menyatakan
 * apakah sebuah item medis termasuk obat terkendali, wajib rantai dingin, atau
 * berisiko tinggi. Konsekuensinya menyebar ke seluruh modul: penjualan obat
 * terkendali ditahan bila identitas pembelinya tidak lengkap
 * ({@code ApotikApiHelper.bayar}), racikan yang memuat obat terkendali ditahan
 * dengan aturan yang sama ({@code ApotikRacikanProduksiHelper}), penerimaan
 * barang menuntut bukti suhu bila ada satu saja item rantai dingin
 * ({@code ApotikPersediaanHelper}), dan register wajib narkotika lahir dari
 * golongan yang tercatat di sini.</p>
 *
 * <p>Yang perlu dipahami dari kedudukan itu: kesalahan pada satu baris di sini
 * MEMATIKAN penjagaan tanpa suara. Item narkotika yang profilnya keliru dicatat
 * BEBAS akan terjual seperti obat warung — tanpa nama pembeli, tanpa nama
 * dokter, tanpa satu pun baris register yang wajib dilaporkan. Tidak ada pesan
 * kesalahan, sebab dari sudut pandang sistem tidak ada yang salah: obat bebas
 * memang tidak menuntut apa-apa. Yang hilang baru ketahuan ketika register
 * diminta pemeriksa dan angkanya tidak cocok dengan stok yang berkurang.</p>
 *
 * <p>Karena itu jalur penyuntingan profil ({@code apotik_item_profil_simpan})
 * digerbangi hak {@code apotik_formularium} dan memvalidasi golongannya lebih
 * dulu. Yang TIDAK ada adalah persetujuan kedua: seorang pemegang hak
 * formularium dapat menurunkan golongan sebuah item dari NARKOTIKA ke BEBAS
 * sendirian, dan sejak saat itu penjualan berikutnya berhenti membuat register.
 * Riwayat perubahannya tersimpan Envers di
 * {@code new_audit.apotik_item_profile__audit}, sehingga perbuatan itu dapat
 * ditelusuri setelah kejadian — tetapi tidak dicegah, dan tidak ada peringatan
 * yang terkirim saat terjadi.</p>
 *
 * <h3>Item tanpa profil</h3>
 *
 * <p>Relasi ke {@link ItemMedis} bersifat satu arah dari sisi ini, dan tidak ada
 * yang mewajibkan setiap item punya baris profil. Item medis tanpa profil
 * diperlakukan pemanggil sebagai {@link #GOLONGAN_BEBAS} — itulah bawaan yang
 * dipakai {@code bayar} ketika {@code profilItem} mengembalikan {@code null}.
 * Bawaan tersebut berarti seluruh item lama yang belum pernah disentuh
 * formularium melewati seluruh penjagaan obat terkendali. Untuk data warisan
 * itu tidak terhindarkan; yang penting diketahui adalah bahwa "tidak ada profil"
 * berperilaku persis seperti "dinyatakan bebas", bukan seperti "belum
 * ditentukan".</p>
 *
 * @see ApotikNarkotikaLog register yang wajib lahir dari golongan terkendali di sini
 * @see ApotikPenerimaanSuhu bukti suhu yang dipicu penanda {@link #getColdChain()}
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_item_profile")
public class ApotikItemProfile extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/**
	 * Golongan: obat bebas — boleh dijual tanpa resep, tanpa syarat tambahan.
	 *
	 * <p>Sekaligus bawaan yang dipakai {@link #getGolonganObat()} bila kolomnya
	 * kosong, dan bawaan yang dipakai pemanggil bila item tidak punya baris
	 * profil sama sekali. Arah bawaan itu paling longgar, dan itu perlu
	 * disadari: untuk penanda yang menentukan penjagaan, bawaan yang aman
	 * biasanya yang paling ketat. Pilihan longgar di sini adalah keputusan
	 * kompatibilitas — puluhan ribu item medis warisan tidak punya profil, dan
	 * bawaan ketat akan menahan penjualan seluruhnya sampai formularium
	 * dilengkapi satu per satu.</p>
	 *
	 * <p>Kelima konstanta golongan tersimpan sebagai TEKS di kolom
	 * {@code golongan_obat}. Mengubah teks konstanta yang sudah ada akan membuat
	 * baris lama tidak lagi cocok dengan {@link #terkendali(String)}, dan obat
	 * narkotika yang tercatat dengan teks lama akan diam-diam berhenti menuntut
	 * register. Tambahkan golongan baru bila perlu; jangan mengganti nama yang
	 * sudah ada.</p>
	 */
	public static final String GOLONGAN_BEBAS = "BEBAS";

	/** Golongan: obat bebas terbatas — tanpa resep, dengan peringatan khusus. */
	public static final String GOLONGAN_BEBAS_TERBATAS = "BEBAS_TERBATAS";

	/** Golongan: obat keras — menuntut resep dokter, tetapi tidak menuntut register. */
	public static final String GOLONGAN_KERAS = "KERAS";

	/** Golongan: narkotika — terkendali, wajib register penjualan. */
	public static final String GOLONGAN_NARKOTIKA = "NARKOTIKA";

	/** Golongan: psikotropika — terkendali, wajib register penjualan. */
	public static final String GOLONGAN_PSIKOTROPIKA = "PSIKOTROPIKA";

	/**
	 * Golongan yang menuntut register penjualan (obat terkendali).
	 *
	 * <p>Inilah predikat yang memicu seluruh penjagaan obat terkendali di modul
	 * apotek. Ia dipanggil di empat tempat yang berbeda: dua kali di
	 * {@code ApotikApiHelper} (menahan penjualan bila identitas pembeli tidak
	 * lengkap, lalu membuat {@link ApotikNarkotikaLog}) dan dua kali di
	 * {@code ApotikRacikanProduksiHelper} (menahan racikan, lalu membuat
	 * registernya). Karena satu metode ini menjadi gerbang bagi empat jalur
	 * sekaligus, mengubahnya berarti mengubah keempatnya — dan itu justru yang
	 * diinginkan: definisi "terkendali" tidak boleh berbeda-beda tergantung
	 * jalur mana yang kebetulan diambil.</p>
	 *
	 * <p>Statis dan tanpa keadaan, sehingga dapat dipanggil atas nilai golongan
	 * mana pun — termasuk atas bawaan {@link #GOLONGAN_BEBAS} yang dipakai
	 * pemanggil ketika item tidak punya profil. Perbandingannya PEKA HURUF
	 * ({@code equals}, bukan {@code equalsIgnoreCase}), sehingga nilai bertulisan
	 * lain — "Narkotika", "narkotika" — akan dijawab {@code false} dan obatnya
	 * lolos dari seluruh penjagaan. Kepekaan itu tidak berbahaya pada jalur yang
	 * ada, sebab {@code apotik_item_profil_simpan} memvalidasi golongan dengan
	 * {@link #golonganValid(String)} yang sama pekanya sebelum menyimpan; ia
	 * menjadi berbahaya bila kelak ada jalur lain — impor massal, penyuntingan
	 * langsung ke basis data — yang menulis kolom tanpa melewati validasi
	 * itu.</p>
	 *
	 * @param golongan nilai golongan yang diperiksa; boleh {@code null}
	 * @return {@code true} hanya untuk {@link #GOLONGAN_NARKOTIKA} dan
	 *         {@link #GOLONGAN_PSIKOTROPIKA}, dengan penulisan persis sama
	 */
	public static boolean terkendali(String golongan) {
		return GOLONGAN_NARKOTIKA.equals(golongan) || GOLONGAN_PSIKOTROPIKA.equals(golongan);
	}

	/**
	 * Apakah teks golongan termasuk kosakata yang dikenal.
	 *
	 * <p>Dipakai {@code ApotikApiHelper.itemProfilSimpan} untuk menolak nilai di
	 * luar kelima konstanta sebelum menyentuh entity. Penjagaan itu adalah
	 * satu-satunya yang menahan teks bebas masuk ke kolom {@code golongan_obat}
	 * — kolomnya sendiri sekadar varchar 30 tanpa batasan apa pun di basis
	 * data.</p>
	 *
	 * <p>Perhatikan mengapa penjagaan itu penting justru untuk hal yang
	 * sebaliknya. Nilai salah tulis TIDAK akan membuat obat bebas tampak
	 * terkendali — {@link #terkendali(String)} akan menjawab {@code false} —
	 * melainkan membuat obat terkendali tampak bebas. Kesalahan pada kolom ini
	 * karena itu selalu condong ke arah melonggarkan, tidak pernah ke arah
	 * mengetatkan, dan itulah alasan validasinya tidak boleh dilewati.</p>
	 *
	 * <p>Peka huruf dan peka spasi: nilai bertulisan lain atau berspasi di ujung
	 * akan ditolak. Ketegasan itu tepat untuk kolom yang menentukan penjagaan;
	 * memaafkan penulisan akan membuka pintu bagi nilai yang lolos validasi
	 * tetapi gagal dikenali {@link #terkendali(String)}.</p>
	 *
	 * @param golongan nilai golongan yang diperiksa; boleh {@code null}
	 * @return {@code true} bila termasuk salah satu dari kelima konstanta
	 */
	public static boolean golonganValid(String golongan) {
		return GOLONGAN_BEBAS.equals(golongan) || GOLONGAN_BEBAS_TERBATAS.equals(golongan)
				|| GOLONGAN_KERAS.equals(golongan) || GOLONGAN_NARKOTIKA.equals(golongan)
				|| GOLONGAN_PSIKOTROPIKA.equals(golongan);
	}

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Item medis yang diprofilkan. Wajib. */
	private ItemMedis item;

	/** Salah satu dari kelima konstanta golongan; menentukan penjagaan penjualan. */
	private String golonganObat;

	/** Penanda Look-Alike Sound-Alike; murni tampilan. */
	private Boolean lasa;
	// IR-01 (modernisasi UI/UX apotik): atribut yang dibutuhkan kasir untuk
	// membedakan obat secara cepat dan menandai risiko tinggi. Semua NULLABLE
	// supaya baris profil lama tetap sah tanpa migrasi data.

	/** Bentuk sediaan (tablet, sirup, injeksi); teks bebas. */
	private String bentukSediaan;

	/** Kekuatan/dosis satuan; teks bebas. */
	private String kekuatan;

	/** Penanda obat berisiko cedera tinggi; murni tampilan. */
	private Boolean highAlert;

	/** Penanda wajib rantai dingin; memicu bukti suhu saat penerimaan. */
	private Boolean coldChain;

	/** Catatan bebas tentang item. */
	private String keterangan;

	/** Nama tampil pelaku perubahan terakhir (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku perubahan terakhir (bayangan audit). */
	private String olehId;

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Berbeda dari entity apotek lain di paket ini yang praktis append-only,
	 * kait ini benar-benar berjalan di sini: profil item memang disunting
	 * berulang kali lewat layar formularium. Justru karena itu stempel waktunya
	 * berguna — ia menjawab "sejak kapan obat ini digolongkan begini", yang
	 * merupakan pertanyaan pertama bila register narkotika tidak cocok dengan
	 * stok yang berkurang.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Konstruktor tanpa argumen yang dituntut JPA.
	 *
	 * <p>Objek yang dihasilkan belum sah untuk disimpan: {@link #getItem()}
	 * {@code nullable = false}, sehingga pemanggil wajib mengisinya sebelum
	 * {@code save}. Golongan boleh dibiarkan kosong — {@link #getGolonganObat()}
	 * akan menjawab {@link #GOLONGAN_BEBAS}.</p>
	 */
	public ApotikItemProfile() {
	}

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * @return kunci baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
	 *
	 * @param id kunci baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Item medis yang diprofilkan.
	 *
	 * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field.
	 * {@code check} menormalkan proksi malas Hibernate yang sudah lepas dari
	 * sesinya menjadi {@code null}, mencegah {@code LazyInitializationException}
	 * ketika objek dibaca di luar sesi. Memanggilnya karena itu dapat mengubah
	 * keadaan objek dan bukan pembacaan murni.</p>
	 *
	 * <p>{@code nullable = false} — profil yang tidak menunjuk item tidak akan
	 * pernah ditemukan pencarian dan hanya menempati ruang.</p>
	 *
	 * <p><b>Tidak dijaga unik.</b> Tidak ada batasan yang mencegah satu item
	 * medis punya beberapa baris profil. Pemanggil mencari dengan
	 * {@code setMaxResults(1)}, sehingga bila duplikat ada, yang berlaku adalah
	 * satu baris yang kebetulan dikembalikan basis data lebih dulu — dan itu
	 * dapat berubah setelah pemeliharaan tabel. Untuk kolom yang menentukan
	 * apakah sebuah obat menuntut register narkotika, ketidakpastian semacam itu
	 * berarti penjagaannya dapat menyala dan padam sendiri tanpa ada yang
	 * mengubah data. {@code itemProfilSimpan} memang mencari baris yang sudah
	 * ada sebelum membuat yang baru, tetapi pencarian-lalu-simpan itu punya jeda
	 * dan tidak didukung batasan basis data.</p>
	 *
	 * @return item medis yang diprofilkan, atau {@code null} bila proksinya lepas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = false)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan item medis yang diprofilkan.
	 *
	 * @param item item medis; wajib terisi sebelum disimpan
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Golongan obat; menentukan penjagaan penjualan yang berlaku.
	 *
	 * <p>Mengembalikan {@link #GOLONGAN_BEBAS} bila kolom kosong atau hanya
	 * berisi spasi. Arah bawaan yang paling longgar itu perlu diketahui
	 * betul: baris profil yang golongannya belum diisi berperilaku persis
	 * seperti obat bebas — dijual tanpa nama pembeli, tanpa nama dokter, tanpa
	 * register. Ia TIDAK berperilaku seperti "belum ditentukan" dan tidak
	 * menimbulkan peringatan apa pun.</p>
	 *
	 * <p>Perhatikan pula bahwa getter dan kolom dapat menjawab berbeda untuk
	 * baris yang sama. Beberapa jalur — misalnya penyaringan dan pelaporan yang
	 * bekerja dengan SQL langsung, seperti
	 * {@code ApotikLaporanHelper.laporanTerkendali} dan pencarian item demo —
	 * membaca kolom {@code golongan_obat} apa adanya, di mana kosong tetap
	 * kosong dan tidak menjadi BEBAS. Pembacaan lewat entity dan pembacaan lewat
	 * SQL karena itu dapat mengelompokkan baris yang sama secara berbeda.</p>
	 *
	 * <p>Nilai yang berlaku dijaga {@link #golonganValid(String)} di pemanggil,
	 * bukan di setter — lihat catatan pada metode itu tentang mengapa kesalahan
	 * di kolom ini selalu condong melonggarkan.</p>
	 *
	 * @return golongan obat; {@link #GOLONGAN_BEBAS} bila kolom kosong
	 */
	@Column(name = "golongan_obat", length = 30)
	public String getGolonganObat() {
		return golonganObat == null || golonganObat.trim().isEmpty() ? GOLONGAN_BEBAS : golonganObat;
	}

	/**
	 * Menetapkan golongan obat.
	 *
	 * <p>Menyimpan apa adanya tanpa memvalidasi. Seluruh penjagaan ada di
	 * {@code ApotikApiHelper.itemProfilSimpan}, yang menolak nilai di luar
	 * kelima konstanta sebelum memanggil setter ini. Jalur baru mana pun yang
	 * menulis golongan WAJIB menyalin validasi itu — nilai yang lolos ke sini
	 * dalam bentuk yang tidak dikenali akan membuat obat terkendali diperlakukan
	 * sebagai obat bebas.</p>
	 *
	 * @param golonganObat salah satu konstanta {@code GOLONGAN_*}
	 */
	public void setGolonganObat(String golonganObat) {
		this.golonganObat = golonganObat;
	}

	/**
	 * Penanda Look-Alike Sound-Alike — obat yang rupa atau namanya mirip.
	 *
	 * <p>Mengembalikan {@code FALSE} bila kolom kosong; arah bawaan yang tepat
	 * untuk penanda tampilan, sebab menandai semua obat sebagai LASA akan
	 * membuat penandanya kehilangan arti.</p>
	 *
	 * <p>Murni penanda tampilan: kasir melihat baris obat mirip ditampilkan
	 * berbeda supaya tidak salah ambil. Tidak ada satu pun penjagaan peladen
	 * yang bersandar padanya — obat bertanda LASA dijual persis seperti yang
	 * tidak bertanda. Sifat itu perlu disebut agar penanda ini tidak dikira
	 * pengaman; ia alat bantu mata manusia, dan seluruh nilainya bergantung
	 * pada layar yang benar-benar menampilkannya secara mencolok.</p>
	 *
	 * @return {@code TRUE} bila item ditandai LASA; {@code FALSE} bila kolom kosong
	 */
	@Column(name = "lasa")
	public Boolean getLasa() {
		return lasa == null ? Boolean.FALSE : lasa;
	}

	/**
	 * Menetapkan penanda LASA.
	 *
	 * @param lasa {@code TRUE} bila item rupa/namanya mirip obat lain
	 */
	public void setLasa(Boolean lasa) {
		this.lasa = lasa;
	}

	/**
	 * Bentuk sediaan (tablet, sirup, injeksi, salep, ...). Teks bebas ringkas.
	 *
	 * <p>Tidak dinormalkan dan tidak divalidasi; dua orang dapat menuliskan
	 * bentuk yang sama dengan cara berbeda. Untuk kolom yang gunanya membantu
	 * kasir membedakan dua kemasan obat bernama sama di layar, kelonggaran itu
	 * memadai — ia tidak pernah dijadikan dasar pengelompokan maupun
	 * perhitungan.</p>
	 *
	 * @return bentuk sediaan, atau {@code null}
	 */
	@Column(name = "bentuk_sediaan", length = 60)
	public String getBentukSediaan() {
		return bentukSediaan;
	}

	/**
	 * Menetapkan bentuk sediaan.
	 *
	 * @param bentukSediaan bentuk sediaan
	 */
	public void setBentukSediaan(String bentukSediaan) {
		this.bentukSediaan = bentukSediaan;
	}

	/**
	 * Kekuatan/dosis satuan (mis. "500 mg", "5 mg/5 mL").
	 *
	 * <p>Teks bebas, bukan angka bersatuan. Konsekuensinya: kolom ini tidak
	 * dapat dipakai perhitungan dosis apa pun, dan tidak boleh dijadikan dasar
	 * pemeriksaan dosis otomatis. Gunanya membedakan dua kemasan obat bernama
	 * sama yang berbeda kekuatannya — persis jenis kekeliruan yang paling sering
	 * terjadi di meja penyiapan.</p>
	 *
	 * @return kekuatan/dosis satuan, atau {@code null}
	 */
	@Column(name = "kekuatan", length = 60)
	public String getKekuatan() {
		return kekuatan;
	}

	/**
	 * Menetapkan kekuatan/dosis satuan.
	 *
	 * @param kekuatan kekuatan satuan
	 */
	public void setKekuatan(String kekuatan) {
		this.kekuatan = kekuatan;
	}

	/**
	 * Obat high-alert (risiko cedera tinggi bila salah): insulin, heparin,
	 * elektrolit pekat, dsb. Dipakai UI untuk menandai baris secara mencolok.
	 * BUKAN pengganti golongan obat -- keduanya berdiri sendiri.
	 *
	 * <p>Mengembalikan {@code FALSE} bila kolom kosong. Berlaku catatan yang
	 * sama dengan {@link #getLasa()}: murni penanda tampilan, tidak ada
	 * penjagaan peladen yang bersandar padanya. Obat high-alert dijual dengan
	 * cara yang persis sama dengan obat lain.</p>
	 *
	 * <p>Kalimat "bukan pengganti golongan obat" pada paragraf di atas patut
	 * ditegaskan dari arah sebaliknya pula: golongan juga bukan pengganti
	 * penanda ini. Insulin adalah obat keras yang tidak terkendali — golongannya
	 * tidak menuntut register apa pun — namun kesalahan dosisnya dapat
	 * mematikan. Kedua penanda menjawab pertanyaan yang berbeda: golongan
	 * menjawab "apa syarat hukum penjualannya", high-alert menjawab "seberapa
	 * parah akibatnya bila salah".</p>
	 *
	 * @return {@code TRUE} bila item berisiko tinggi; {@code FALSE} bila kolom kosong
	 */
	@Column(name = "high_alert")
	public Boolean getHighAlert() {
		return highAlert == null ? Boolean.FALSE : highAlert;
	}

	/**
	 * Menetapkan penanda obat high-alert.
	 *
	 * @param highAlert {@code TRUE} bila item berisiko cedera tinggi
	 */
	public void setHighAlert(Boolean highAlert) {
		this.highAlert = highAlert;
	}

	/**
	 * Wajib rantai dingin (2-8 C). Menentukan peringatan penyimpanan/kirim.
	 *
	 * <p>Mengembalikan {@code FALSE} bila kolom kosong — arah yang tepat, sebab
	 * menganggap barang biasa sebagai rantai dingin akan membanjiri penerimaan
	 * dengan tuntutan bukti suhu yang tidak relevan.</p>
	 *
	 * <p>Berbeda dari {@link #getLasa()} dan {@link #getHighAlert()}, penanda ini
	 * BENAR-BENAR menggerakkan peladen: {@code ApotikPersediaanHelper}
	 * menelusuri profil setiap item pada faktur penerimaan dan, bila ada satu
	 * saja yang bertanda ini, membuat baris {@link ApotikPenerimaanSuhu} untuk
	 * faktur tersebut. Penelusuran itu dikerjakan peladen dari data, bukan
	 * diklaim klien.</p>
	 *
	 * <p>Yang perlu dipahami tentang batas pengaruhnya: penanda ini membuat
	 * bukti suhu TERCATAT, bukan membuat penerimaan DITOLAK ketika suhunya
	 * menyimpang. Lihat {@link ApotikPenerimaanSuhu} untuk penjelasan lengkap
	 * mengapa sikap itu dipilih dan apa akibatnya. Penanda ini juga tidak
	 * berpengaruh pada {@link ApotikDeliveryOrder}: pengantaran obat rantai
	 * dingin tidak menuntut layanan kurir tertentu, dan tidak ada peringatan
	 * yang muncul ketika obat bertanda ini dikirim dengan layanan biasa.</p>
	 *
	 * @return {@code TRUE} bila item wajib rantai dingin; {@code FALSE} bila kolom kosong
	 */
	@Column(name = "cold_chain")
	public Boolean getColdChain() {
		return coldChain == null ? Boolean.FALSE : coldChain;
	}

	/**
	 * Menetapkan penanda wajib rantai dingin.
	 *
	 * @param coldChain {@code TRUE} bila item wajib disimpan 2-8 derajat Celsius
	 */
	public void setColdChain(Boolean coldChain) {
		this.coldChain = coldChain;
	}

	/**
	 * Catatan bebas tentang item.
	 *
	 * <p>Kolom {@code text} tanpa batas panjang praktis. Tempat yang tepat untuk
	 * keterangan yang tidak muat di penanda mana pun — misalnya obat mana yang
	 * mudah tertukar dengan item ini, atau syarat penyimpanan khusus di luar
	 * rantai dingin biasa.</p>
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan catatan bebas.
	 *
	 * @param keterangan keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama tampil pelaku perubahan terakhir (bayangan audit).
	 *
	 * @return nama pelaku, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pelaku — MENGABAIKAN nilai kosong, tidak menimpanya.
	 *
	 * <p>Menolak {@code null} dan teks berisi spasi saja secara diam. Bentuk ini
	 * seragam di basis kode dan merupakan keharusan teknis: kolom bayangan audit
	 * ini melewati jalur-jalur yang menyalin seluruh properti tanpa memilah, dan
	 * satu penyalinan dengan string kosong sudah cukup untuk menghapus nama
	 * pelaku yang benar tanpa menyisakan nilai sebelumnya di baris itu.</p>
	 *
	 * <p>Untuk profil item, pertaruhannya termasuk yang tertinggi di seluruh
	 * modul apotek. Menurunkan golongan sebuah obat dari NARKOTIKA ke BEBAS
	 * mematikan register wajib bagi setiap penjualan sesudahnya, dan pertanyaan
	 * "siapa yang mengubahnya" adalah hal pertama yang ditanyakan ketika
	 * ketidakcocokan register ditemukan. Harganya: nilai tidak dapat dikosongkan
	 * kembali lewat setter — harga yang benar untuk kolom yang hanya boleh
	 * bertambah jelas.</p>
	 *
	 * @param oleh nama pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Identitas akun pelaku perubahan terakhir (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id akun pelaku — MENGABAIKAN nilai kosong.
	 *
	 * <p>Berlaku seluruh pertimbangan pada {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id akun pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel perubahan terakhir.
	 *
	 * <p>Benar-benar bergerak pada entity ini, berbeda dari entity apotek lain
	 * yang praktis append-only: profil item memang disunting berulang lewat
	 * layar formularium. Riwayat lengkapnya tersimpan Envers di
	 * {@code new_audit.apotik_item_profile__audit}, yang untuk kolom golongan
	 * obat merupakan satu-satunya cara mengetahui sejak kapan sebuah obat
	 * berhenti menuntut register.</p>
	 *
	 * @return waktu ubah terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan stempel perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu ubah
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
