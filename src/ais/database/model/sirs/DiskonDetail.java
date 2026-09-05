package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import ais.database.model.akunting.Akun;

/**
 * Baris rincian <b>sasaran diskon</b>: menyatakan objek mana — satu {@link ItemMedis}, satu
 * {@link Tindakan}, atau satu {@link AlatMedis} — yang berhak atas aturan {@link Diskon} induknya.
 * Aturan diskon sendiri hanya membawa besaran persentase dan cakupan
 * asuransi/komunitas/tanggal/kuantitas; entitas inilah yang menentukan sasarannya.
 *
 * <h3>Tiga FK sasaran yang tidak dipaksa saling eksklusif</h3>
 * {@link #getItem()}, {@link #getTindakan()}, dan {@link #getAlatMedis()} semuanya {@code nullable}
 * dan tidak ada <i>check constraint</i> maupun validasi di lapisan model yang memastikan tepat satu
 * terisi. Cara kueri penyaring memperlakukan ketiganya membuat hal itu berdampak nyata.
 * {@code ais.common.CommonSirs.getDiskonSekarang} menambahkan syarat hanya untuk sumbu yang
 * ditanyakan — mis. {@code eq("item", item)} bila yang dicari diskon sebuah item — dan memasang
 * {@code sqlRestriction("1=1")} (yakni: tanpa syarat sama sekali) untuk dua sumbu lainnya. Artinya:
 * <ul>
 * <li>Baris yang <b>ketiga sasarannya kosong</b> akan cocok dengan setiap pencarian pada sumbu apa
 * pun, karena satu-satunya syarat sumbu yang aktif adalah kesamaan dengan objek yang dicari —
 * dan baris kosong tidak lolos syarat itu. Dengan kata lain baris kosong tidak berbahaya untuk
 * pencarian bersasaran, tetapi ia juga tidak pernah berguna: ia baris mati yang tetap terhitung
 * pada layar rincian diskon.</li>
 * <li>Baris yang <b>lebih dari satu sasarannya terisi</b> akan cocok pada setiap sumbu yang
 * terisi, sehingga satu baris rincian dapat memberikan diskon yang sama kepada sebuah item
 * <i>dan</i> sebuah tindakan sekaligus. Tidak ada yang mencegahnya, dan dari layar rincian hal itu
 * tidak terlihat sebagai kejanggalan.</li>
 * </ul>
 * Perhatikan pula bahwa kueri mengelompokkan hasilnya per {@code diskon}
 * ({@code Projections.groupProperty("diskon")}), sehingga satu aturan diskon dengan banyak baris
 * rincian yang sama-sama cocok tetap dihitung sekali — pengelompokan itulah yang mencegah diskon
 * berlipat akibat rincian ganda. Namun perlindungan itu hanya berlaku dalam satu aturan; dua
 * <i>aturan</i> diskon berbeda yang sama-sama cocok tetap dijumlahkan secara akumulatif oleh
 * {@code CommonSirs.getTotalDiskonDalamPersen}.
 *
 * <h3>Bahaya {@code NullPointerException} pada pewarisan akun</h3>
 * {@link #getAkun()} mewarisi akun dari {@link Diskon#getAkun()} bila akun baris belum ditetapkan,
 * tetapi berbeda dari {@link Biaya#getAkun()} yang membungkus rantai serupa dalam
 * {@code try/catch}, versi di sini memanggil {@code getDiskon().getAkun()} <b>tanpa memeriksa
 * apakah {@code getDiskon()} bernilai {@code null}</b>. Kolom {@code diskon} memang
 * {@code NOT NULL} di basis data, sehingga baris tersimpan selalu punya induk; tetapi objek
 * {@code DiskonDetail} yang baru dibuat di memori dan belum ditautkan ke aturan diskon akan
 * melempar {@code NullPointerException} begitu {@code getAkun()} dipanggil — termasuk saat
 * dipanggil kerangka kerja persistensi. Urutan penulisan yang aman adalah menetapkan
 * {@link #setDiskon(Diskon)} lebih dahulu sebelum objek disentuh getter mana pun.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — seluruh relasi memanggil {@code check(...)} dan menulis balik ke
 * field; {@link #getAkun()} bahkan menulis hasil pewarisan, sehingga akun warisan menjadi permanen
 * setelah pembacaan pertama dan tidak lagi mengikuti perubahan akun pada aturan diskon
 * induknya.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — sesuai keterbatasan seluruh modul {@code sirs}.</li>
 * <li>Tidak ada koleksi {@code DiskonDetail} yang dipetakan pada {@link Diskon}; pembersihan baris
 * rincian saat aturan diskon dihapus dilakukan secara manual oleh
 * {@code ais.action.master.sirs.DiskonAction}, bukan lewat <i>cascade</i>.</li>
 * </ul>
 *
 * @see Diskon aturan diskon induk yang membawa besaran dan cakupan
 * @see PajakDetail padanan entitas ini untuk pungutan pajak medis
 * @see ais.common.CommonSirs#getDiskonSekarang kueri penyaring yang membaca ketiga FK sasaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "diskon_detail")
public class DiskonDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.diskon_detail}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris rincian ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir; nilai kosong/spasi diabaikan agar jejak audit
	 * tidak terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;

	/**
	 * Representasi teks baris rincian untuk komponen ZK, memakai field {@link #keterangan}
	 * langsung. Menghasilkan {@code null} bila keterangan belum diisi — dan karena keterangan tidak
	 * wajib, itulah keadaan yang lazim.
	 *
	 * @return keterangan baris, dapat {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; nilai kosong/spasi diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris rincian ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir; normalnya diisi otomatis oleh interceptor audit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris rincian ini. Baris rincian tidak memiliki
	 * masa berlaku sendiri; masa berlaku diatur pada aturan {@link Diskon} induknya.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sasaran diskon berupa item medis (obat/barang); salah satu dari tiga sumbu sasaran. */
	private ItemMedis item;

	/** Sasaran diskon berupa tindakan/layanan medis; salah satu dari tiga sumbu sasaran. */
	private Tindakan tindakan;

	/** Sasaran diskon berupa alat medis/alat kesehatan; salah satu dari tiga sumbu sasaran. */
	private AlatMedis alatMedis;

	/** Aturan diskon induk; kolomnya {@code NOT NULL}. */
	private Diskon diskon;

	/** Keterangan bebas atas baris rincian; sekaligus label {@link #toString()}. */
	private String keterangan;

	/** Akun buku besar baris rincian; diwarisi dari aturan diskon induk bila kosong. */
	private Akun akun;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public DiskonDetail() {
	}

	/**
	 * Mengembalikan kunci utama baris rincian.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya untuk kerangka kerja persistensi atau saat menyalin
	 * entitas menjadi baris baru.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris rincian.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas baris rincian.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan sasaran diskon berupa item medis. Tidak ada penjaga yang mengosongkan dua sumbu
	 * sasaran lainnya, sehingga satu baris dapat menyasar beberapa jenis objek sekaligus.
	 *
	 * @param item item medis sasaran, boleh {@code null}
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengembalikan sasaran diskon berupa item medis. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return item medis sasaran, atau {@code null} bila sasaran baris ini bukan item
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menautkan baris rincian ini ke aturan diskon induknya. <b>Wajib dipanggil sebelum getter mana
	 * pun</b> pada objek yang baru dibuat, karena {@link #getAkun()} akan melempar
	 * {@code NullPointerException} bila induk masih kosong.
	 *
	 * @param diskon aturan diskon induk
	 */
	public void setDiskon(Diskon diskon) {
		this.diskon = diskon;
	}

	/**
	 * Mengembalikan aturan diskon induk baris ini. <b>Getter destruktif</b> ({@code check(...)}).
	 * Kolomnya {@code NOT NULL}, sehingga baris tersimpan selalu memiliki induk.
	 *
	 * @return aturan diskon induk, atau {@code null} untuk objek yang belum ditautkan di memori
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon", nullable = false)
	public Diskon getDiskon() {
		diskon = check(diskon);
		return diskon;
	}

	/**
	 * Mengembalikan sasaran diskon berupa tindakan/layanan medis. <b>Getter destruktif</b>
	 * ({@code check(...)}).
	 *
	 * @return tindakan sasaran, atau {@code null} bila sasaran baris ini bukan tindakan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tindakan", nullable = true)
	public Tindakan getTindakan() {
		tindakan = check(tindakan);
		return tindakan;
	}

	/**
	 * Menetapkan sasaran diskon berupa tindakan/layanan medis.
	 *
	 * @param tindakan tindakan sasaran, boleh {@code null}
	 */
	public void setTindakan(Tindakan tindakan) {
		this.tindakan = tindakan;
	}

	/**
	 * Mengembalikan sasaran diskon berupa alat medis. <b>Getter destruktif</b>
	 * ({@code check(...)}).
	 *
	 * @return alat medis sasaran, atau {@code null} bila sasaran baris ini bukan alat medis
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alat_medis", nullable = true)
	public AlatMedis getAlatMedis() {
		alatMedis = check(alatMedis);
		return alatMedis;
	}

	/**
	 * Menetapkan sasaran diskon berupa alat medis.
	 *
	 * @param alatMedis alat medis sasaran, boleh {@code null}
	 */
	public void setAlatMedis(AlatMedis alatMedis) {
		this.alatMedis = alatMedis;
	}

	/**
	 * Mengembalikan akun buku besar baris rincian, mewarisinya dari {@link Diskon#getAkun()} bila
	 * akun baris belum ditetapkan sendiri.
	 *
	 * <p>
	 * Pewarisan ini menutup jarak antara kolom {@code akun} yang {@code NOT NULL} dan kenyataan
	 * bahwa operator biasanya hanya menetapkan akun sekali di tingkat aturan diskon, bukan
	 * berulang-ulang di tiap baris sasaran. Bila akun baris masih {@code null} dan aturan diskon
	 * induknya punya akun, akun induk itulah yang dipakai; bila tidak, {@code check(akun)} dipanggil
	 * untuk memaksa materialisasi proxy akun yang sudah ada.
	 * </p>
	 *
	 * <p>
	 * Dua sifat metode ini perlu diwaspadai. <b>Pertama, tidak ada penjagaan {@code null} pada
	 * induk.</b> Ekspresi {@code getDiskon().getAkun()} dievaluasi tanpa memeriksa lebih dahulu
	 * apakah {@code getDiskon()} mengembalikan {@code null}, dan berbeda dari
	 * {@link Biaya#getAkun()} yang membungkus rantai pewarisan serupa dalam {@code try/catch},
	 * di sini tidak ada jaring pengaman apa pun. Untuk baris yang sudah tersimpan hal ini aman
	 * karena kolomnya {@code NOT NULL}, tetapi objek {@code DiskonDetail} baru yang belum
	 * ditautkan ke aturan diskon akan melempar {@code NullPointerException} pada pemanggilan getter
	 * ini — termasuk ketika pemanggilnya adalah kerangka kerja persistensi atau perender grid ZK,
	 * sehingga galat muncul di tempat yang jauh dari penyebabnya. Selalu panggil
	 * {@link #setDiskon(Diskon)} lebih dahulu setelah membuat objek baru.
	 * </p>
	 *
	 * <p>
	 * <b>Kedua, metode ini destruktif dan pewarisannya hanya sekali.</b> Akun hasil pewarisan
	 * ditulis balik ke field, sehingga setelah pembacaan pertama diikuti flush, baris rincian
	 * memiliki akunnya sendiri secara eksplisit. Mengubah akun pada aturan {@link Diskon} induk
	 * sesudah itu <b>tidak</b> lagi merambat ke baris rincian yang sudah pernah dibaca — perubahan
	 * akun master perlu disertai penyesuaian data pada baris rincian yang terlanjur terisi.
	 * </p>
	 *
	 * @return akun buku besar baris rincian, atau {@code null} bila tidak ada sumber warisan
	 * @throws NullPointerException bila aturan diskon induk belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = false)
	public Akun getAkun() {
		if (akun == null && getDiskon().getAkun() != null) {
			akun = getDiskon().getAkun();
		} else {
			akun = check(akun);
		}
		return akun;
	}

	/**
	 * Menetapkan akun buku besar baris rincian secara eksplisit, mengalahkan pewarisan dari aturan
	 * diskon induk.
	 *
	 * @param akun akun buku besar
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

}
