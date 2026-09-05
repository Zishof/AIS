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
 * Baris rincian <b>sasaran pajak medis</b>: menyatakan objek mana — satu {@link ItemMedis}, satu
 * {@link Tindakan}, atau satu {@link AlatMedis} — yang dikenai aturan {@link PajakMedis} induknya.
 * Aturan pajak sendiri hanya membawa besaran persentase dan cakupan asuransi/komunitas/tanggal;
 * entitas inilah yang menentukan sasarannya. Struktur dan keterbatasannya adalah cerminan
 * {@link DiskonDetail}.
 *
 * <h3>Tiga FK sasaran yang tidak dipaksa saling eksklusif</h3>
 * {@link #getItem()}, {@link #getTindakan()}, dan {@link #getAlatMedis()} semuanya {@code nullable}
 * tanpa <i>check constraint</i> maupun validasi di lapisan model.
 * {@code ais.common.CommonSirs.getPajakSekarang} memasang syarat kesamaan hanya untuk sumbu yang
 * ditanyakan dan {@code sqlRestriction("1=1")} — yakni tanpa syarat sama sekali — untuk dua sumbu
 * lainnya. Akibatnya baris yang ketiga sasarannya kosong tidak pernah terpilih (baris mati yang
 * tetap terhitung di layar rincian), sedangkan baris yang beberapa sasarannya terisi akan mengenai
 * beberapa jenis objek sekaligus tanpa terlihat janggal dari layar.
 * <p>
 * Kueri mengelompokkan hasilnya per {@code pajak} ({@code Projections.groupProperty("pajak")}),
 * sehingga satu aturan pajak dengan banyak baris rincian yang sama-sama cocok tetap dihitung
 * sekali. Perlindungan itu hanya berlaku dalam satu aturan; dua <i>aturan</i> pajak berbeda yang
 * sama-sama cocok tetap dijumlahkan akumulatif oleh {@code CommonSirs.getTotalPajakDalamPersen}.
 * </p>
 *
 * <h3>Pewarisan akun yang lebih aman daripada padanan diskonnya</h3>
 * {@link #getAkun()} di sini menyusun rantai pewarisannya dengan urutan berbeda dari
 * {@link DiskonDetail#getAkun()}: ia memanggil {@code check(akun)} lebih dahulu, baru memeriksa
 * warisan dari induk — dan pemeriksaan itu diawali penjagaan {@code getPajak() != null}. Karena itu
 * versi ini <b>tidak</b> rentan terhadap {@code NullPointerException} pada objek yang belum
 * ditautkan ke aturan pajak, berbeda dari padanan diskonnya yang memanggil
 * {@code getDiskon().getAkun()} tanpa penjagaan. Perbedaan halus antara dua kelas kembar ini perlu
 * diketahui saat menyalin pola dari salah satunya.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — seluruh relasi memanggil {@code check(...)} dan menulis balik ke
 * field; {@link #getAkun()} juga menulis hasil pewarisan, sehingga akun warisan menjadi permanen
 * setelah pembacaan pertama dan tidak lagi mengikuti perubahan akun pada aturan pajak induknya.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — sesuai keterbatasan seluruh modul {@code sirs}.</li>
 * <li>Tidak ada koleksi {@code PajakDetail} yang dipetakan pada {@link PajakMedis}; pembersihan
 * baris rincian saat aturan pajak dihapus dilakukan manual oleh
 * {@code ais.action.master.sirs.PajakAction}, bukan lewat <i>cascade</i>.</li>
 * </ul>
 *
 * @see PajakMedis aturan pajak induk yang membawa besaran dan cakupan
 * @see DiskonDetail cerminan entitas ini untuk potongan harga
 * @see ais.common.CommonSirs#getPajakSekarang kueri penyaring yang membaca ketiga FK sasaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pajak_detail")
public class PajakDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.pajak_detail}, dibangkitkan basis data (IDENTITY). */
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
	 * langsung. Menghasilkan {@code null} bila keterangan belum diisi.
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
	 * masa berlaku sendiri; masa berlaku diatur pada aturan {@link PajakMedis} induknya.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sasaran pajak berupa item medis (obat/barang); salah satu dari tiga sumbu sasaran. */
	private ItemMedis item;

	/** Sasaran pajak berupa tindakan/layanan medis; salah satu dari tiga sumbu sasaran. */
	private Tindakan tindakan;

	/** Sasaran pajak berupa alat medis/alat kesehatan; salah satu dari tiga sumbu sasaran. */
	private AlatMedis alatMedis;

	/** Aturan pajak induk; kolomnya {@code NOT NULL}. */
	private PajakMedis pajak;

	/** Keterangan bebas atas baris rincian; sekaligus label {@link #toString()}. */
	private String keterangan;

	/** Akun buku besar baris rincian; diwarisi dari aturan pajak induk bila kosong. */
	private Akun akun;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public PajakDetail() {
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
	 * Menetapkan sasaran pajak berupa item medis. Tidak ada penjaga yang mengosongkan dua sumbu
	 * sasaran lainnya, sehingga satu baris dapat mengenai beberapa jenis objek sekaligus.
	 *
	 * @param item item medis sasaran, boleh {@code null}
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengembalikan sasaran pajak berupa item medis. <b>Getter destruktif</b> ({@code check(...)}).
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
	 * Menautkan baris rincian ini ke aturan pajak induknya.
	 *
	 * @param pajak aturan pajak induk
	 */
	public void setPajak(PajakMedis pajak) {
		this.pajak = pajak;
	}

	/**
	 * Mengembalikan aturan pajak induk baris ini. <b>Getter destruktif</b> ({@code check(...)}).
	 * Kolomnya {@code NOT NULL}, sehingga baris tersimpan selalu memiliki induk.
	 *
	 * @return aturan pajak induk, atau {@code null} untuk objek yang belum ditautkan di memori
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pajak", nullable = false)
	public PajakMedis getPajak() {
		pajak = check(pajak);
		return pajak;
	}

	/**
	 * Mengembalikan sasaran pajak berupa tindakan/layanan medis. <b>Getter destruktif</b>
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
	 * Menetapkan sasaran pajak berupa tindakan/layanan medis.
	 *
	 * @param tindakan tindakan sasaran, boleh {@code null}
	 */
	public void setTindakan(Tindakan tindakan) {
		this.tindakan = tindakan;
	}

	/**
	 * Mengembalikan sasaran pajak berupa alat medis. <b>Getter destruktif</b>
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
	 * Menetapkan sasaran pajak berupa alat medis.
	 *
	 * @param alatMedis alat medis sasaran, boleh {@code null}
	 */
	public void setAlatMedis(AlatMedis alatMedis) {
		this.alatMedis = alatMedis;
	}

	/**
	 * Mengembalikan akun buku besar baris rincian, mewarisinya dari {@link PajakMedis#getAkun()}
	 * bila akun baris belum ditetapkan sendiri.
	 *
	 * <p>
	 * Urutan langkahnya berbeda dari padanan diskonnya dan justru lebih aman. Metode ini memanggil
	 * {@code check(akun)} <b>lebih dahulu</b> — memaksa materialisasi proxy akun yang mungkin sudah
	 * ada pada baris ini — baru sesudahnya memeriksa apakah hasilnya masih {@code null} dan, jika
	 * ya, mewarisi akun dari aturan pajak induk. Pemeriksaan warisan itu diawali penjagaan
	 * {@code getPajak() != null}, sehingga objek {@code PajakDetail} baru yang belum ditautkan ke
	 * aturan pajak tetap aman: getter mengembalikan {@code null} alih-alih melempar
	 * {@code NullPointerException}. Bandingkan dengan {@link DiskonDetail#getAkun()} yang memanggil
	 * {@code getDiskon().getAkun()} tanpa penjagaan sama sekali — perbedaan halus antara dua kelas
	 * yang secara struktur kembar, dan perlu diketahui bila pola dari salah satunya hendak disalin.
	 * </p>
	 *
	 * <p>
	 * Metode ini destruktif dan pewarisannya hanya berlaku sekali: akun hasil pewarisan ditulis
	 * balik ke field, sehingga setelah pembacaan pertama diikuti flush, baris rincian memiliki
	 * akunnya sendiri secara eksplisit. Mengubah akun pada aturan {@link PajakMedis} induk sesudah
	 * itu tidak lagi merambat ke baris rincian yang sudah pernah dibaca. Perlu dicatat pula bahwa
	 * kolomnya {@code NOT NULL}, sehingga baris yang tidak memperoleh akun dari sumber mana pun
	 * baru akan gagal pada saat penyimpanan sebagai pelanggaran <i>constraint</i>, bukan pada saat
	 * getter dipanggil.
	 * </p>
	 *
	 * @return akun buku besar baris rincian, atau {@code null} bila tidak ada sumber warisan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = false)
	public Akun getAkun() {
		akun = check(akun);
		if (akun == null && getPajak() != null && getPajak().getAkun() != null) {
			akun = getPajak().getAkun();
		}
		return akun;
	}

	/**
	 * Menetapkan akun buku besar baris rincian secara eksplisit, mengalahkan pewarisan dari aturan
	 * pajak induk.
	 *
	 * @param akun akun buku besar
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}
}
