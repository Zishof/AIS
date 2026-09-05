package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Baris komponen (bahan) penyusun satu {@link Racikan}: satu {@link ItemMedis} beserta takarannya
 * dalam formula racikan. Selain data formula murni ({@link #getItem()}, {@link #getJumlah()},
 * {@link #getKeterangan()}), kelas ini juga menampung <b>salinan data transaksi</b> — harga,
 * diskon, pajak, dan jumlah retur — untuk keperluan penagihan pada saat racikan benar-benar
 * dilayani apotik. Pemisahan itu ditandai komentar {@code // Optional -- Hanya untuk transaksi saja}
 * pada deklarasi field: bila baris ini hanya bagian dari formula/resep (belum ditransaksikan),
 * field-field harga tersebut tidak bermakna.
 *
 * <h3>Kedudukan dalam alur penetapan harga racikan</h3>
 * Racikan tidak memiliki tarif tersendiri. Harganya adalah agregat harga tiap komponen: harga
 * jual per {@link ItemMedis} diambil dari {@link HargaJualItem} (lihat
 * {@code ais.common.CommonSirs#hitungHargaRacikan} dan {@code #hitungDiskonRacikan}), lalu
 * disalin ke {@link #getHargaTransaksi()} baris ini saat racikan dilayani. Diskon dan pajak yang
 * berlaku ditentukan lewat {@code CommonSirs.getDiskonSekarang}/{@code getPajakSekarang}
 * berdasarkan item, asuransi, komunitas, jumlah, dan tanggal transaksi; aturan yang terpilih
 * disimpan sebagai koleksi {@link #getDiskons()}/{@link #getPajaks()}, sedangkan persentase
 * totalnya disalin ke {@link #getDiskonPersen()}/{@link #getPajakPersen()}.
 *
 * <h3>PERINGATAN — diskon rupiah tidak pernah dapat diberikan pada baris ini</h3>
 * {@link #getDiskon()} dan {@link #getPajak()} tampak dirancang untuk mendukung dua cara
 * pengisian: nilai rupiah langsung lewat {@link #setDiskon(Double)}, atau perhitungan dari
 * persentase. Pemilihan cara itu dikendalikan penjaga {@code if (getDiskonPersen() != null &&
 * getHargaTransaksi() != null)}. Penjaga tersebut <b>tidak pernah bernilai salah</b>: kedua getter
 * yang diperiksa sudah menormalkan {@code null} menjadi {@code 0.0} sebelum kembali, sehingga
 * keduanya secara struktural tidak mungkin mengembalikan {@code null}. Akibatnya cabang persentase
 * selalu dijalankan dan nilai rupiah apa pun yang sudah ditulis lewat {@code setDiskon}/
 * {@code setPajak} selalu ditimpa — bila {@code diskonPersen} bernilai 0 (nilai awal field), hasil
 * akhirnya selalu 0. Ini instance tambahan dari pola "diskon mustahil diberikan" yang sudah
 * ditemukan pada {@link TransaksiMedisDetail}; mekanismenya identik (normalisasi {@code null}
 * menjadi {@code 0.0} pada getter yang dipakai sebagai penjaga pemilih-mode), sehingga tercatat di
 * sini sebagai kejadian berulang dari pola yang sama, bukan temuan baru. Konsekuensi praktisnya:
 * pada baris racikan, diskon dan pajak <b>hanya</b> dapat dinyatakan dalam persen.
 *
 * <h3>Getter destruktif dan pemetaan kolom implisit</h3>
 * Hampir seluruh getter numerik di kelas ini bersifat destruktif — {@link #getHargaTransaksi()},
 * {@link #getJumlah()}, {@link #getDiskon()}, {@link #getPajak()}, {@link #getDiskonPersen()},
 * {@link #getPajakPersen()}, dan {@link #getHasilPenghitunganTotal()} semuanya menulis balik ke
 * field sebelum mengembalikan nilai. Karena Hibernate memakai akses properti, penulisan balik itu
 * terjadi juga saat kerangka kerja membaca entitas, sehingga nilai tersimpan dapat berubah hanya
 * karena entitas dibaca lalu di-flush. Perlu diperhatikan pula bahwa {@code jumlah}, {@code diskon},
 * {@code pajak}, {@code diskonPersen}, {@code pajakPersen}, dan {@code hasilPenghitunganTotal}
 * tidak diberi {@code @Column} maupun {@code @Transient}, sehingga Hibernate memetakannya ke kolom
 * bernama sesuai nama properti secara implisit — termasuk {@code hasilPenghitunganTotal} yang
 * sesungguhnya nilai turunan; nilai turunan yang ikut tersimpan berpotensi menyimpang dari
 * komponennya bila salah satu komponen berubah tanpa getter itu dipanggil ulang.
 *
 * <h3>Pola arsitektur lain</h3>
 * <ul>
 * <li><b>Field audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, dan {@link #onUpdate()} adalah infrastruktur audit
 * ({@code AuditTimestampInterceptor} + Hibernate Envers), keharusan teknis dan bukan cacat.</li>
 * <li><b>Tanpa sumbu tenant</b> — sesuai keterbatasan seluruh modul {@code sirs}, tidak ada kolom
 * satuan kerja untuk isolasi data antar unit.</li>
 * <li>Relasi {@link #getRacikan()} ke induknya bersifat satu arah dari sisi anak; {@link Racikan}
 * tidak memetakan koleksi {@code RacikanDetail}, sehingga penghapusan racikan tidak
 * meng-<i>cascade</i> ke baris komponen dan dapat meninggalkan baris yatim.</li>
 * </ul>
 *
 * @see Racikan formula racikan yang menjadi induk baris ini
 * @see Diskon aturan diskon yang dapat menempel pada baris ini
 * @see PajakMedis aturan pajak yang dapat menempel pada baris ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "racikan_detail")
public class RacikanDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.racikan_detail}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir; nilai kosong/spasi diabaikan agar jejak audit
	 * yang sudah ada tidak terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;

	/**
	 * Representasi teks baris komponen untuk komponen ZK. Memakai field {@link #keterangan}
	 * langsung, sehingga menghasilkan {@code null} bila keterangan belum diisi — pemanggil yang
	 * merangkai string perlu mengantisipasi hal itu.
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
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
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
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Item medis (obat/bahan) yang menjadi komponen racikan pada baris ini. */
	private ItemMedis item;

	/** Racikan induk yang memuat baris komponen ini. */
	private Racikan racikan;

	/** Takaran komponen dalam formula; juga dipakai sebagai kuantitas pengali saat penagihan. */
	private Double jumlah = 0.0;

	/** Keterangan bebas baris, sekaligus label {@link #toString()}. */
	private String keterangan;

	// Optional -- Hanya untuk transaksi saja
	/** Harga jual satuan komponen pada saat transaksi; disalin dari {@link HargaJualItem}. */
	private Double hargaTransaksi;

	/** Kuantitas komponen yang diretur/dikembalikan. */
	private Double jumlahRetur = 0.0;

	/**
	 * Nilai diskon dalam rupiah. Lihat peringatan pada dokumentasi kelas: nilai yang ditulis lewat
	 * {@link #setDiskon(Double)} selalu ditimpa hasil perhitungan dari {@link #diskonPersen}.
	 */
	private Double diskon = 0.0;

	/**
	 * Nilai pajak dalam rupiah. Sama seperti {@link #diskon}, nilai yang ditulis langsung selalu
	 * ditimpa hasil perhitungan dari {@link #pajakPersen}.
	 */
	private Double pajak = 0.0;

	/** Persentase diskon yang berlaku; hasil agregasi {@code CommonSirs.getTotalDiskonDalamPersen}. */
	private Double diskonPersen = 0.0;

	/** Persentase pajak yang berlaku; hasil agregasi {@code CommonSirs.getTotalPajakDalamPersen}. */
	private Double pajakPersen = 0.0;

	/** Nilai turunan total baris (lihat {@link #getHasilPenghitunganTotal()}); ikut dipetakan ke kolom. */
	private Double hasilPenghitunganTotal = 0.0;

	/**
	 * Mengisi harga jual satuan komponen pada saat transaksi.
	 *
	 * @param hargaTransaksi harga satuan, boleh {@code null} (akan dinormalkan menjadi 0 saat dibaca)
	 */
	public void setHargaTransaksi(Double hargaTransaksi) {
		this.hargaTransaksi = hargaTransaksi;
	}

	/**
	 * Mengembalikan harga jual satuan komponen pada saat transaksi, menormalkan {@code null}
	 * menjadi {@code 0.0}. <b>Getter destruktif</b>: normalisasi ditulis balik ke field. Karena
	 * getter ini tidak pernah mengembalikan {@code null}, ia turut membuat penjaga pemilih-mode di
	 * {@link #getDiskon()} dan {@link #getPajak()} selalu benar.
	 *
	 * @return harga satuan, tidak pernah {@code null}
	 */
	@Column(name = "harga_transaksi", nullable = true)
	public Double getHargaTransaksi() {
		if (hargaTransaksi == null) {
			hargaTransaksi = 0.0;
		}
		return hargaTransaksi;
	}

	/** Aturan-aturan {@link Diskon} yang berlaku untuk baris komponen ini. */
	private Set<Diskon> diskons = new HashSet<Diskon>();

	/**
	 * Mengembalikan aturan diskon yang menempel pada baris ini, diurutkan berdasarkan nama.
	 * Koleksi disimpan lewat tabel penghubung {@code sirs.racikan_detail_has_diskon}. Isi koleksi
	 * ini bersifat dokumentatif — nilai rupiah diskon dihitung dari {@link #getDiskonPersen()},
	 * bukan dengan menjumlahkan ulang koleksi ini di sini.
	 *
	 * @return himpunan aturan diskon, tidak pernah {@code null} (default himpunan kosong)
	 */
	@ManyToMany(targetEntity = Diskon.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "racikan_detail_has_diskon", schema = "sirs", joinColumns = @JoinColumn(name = "racikan_detail"), inverseJoinColumns = @JoinColumn(name = "diskon"))
	public Set<Diskon> getDiskons() {
		return diskons;
	}

	/**
	 * Mengganti seluruh himpunan aturan diskon baris ini.
	 *
	 * @param diskons himpunan aturan diskon yang baru
	 */
	public void setDiskons(Set<Diskon> diskons) {
		this.diskons = diskons;
	}

	/** Aturan-aturan {@link PajakMedis} yang berlaku untuk baris komponen ini. */
	private Set<PajakMedis> pajaks = new HashSet<PajakMedis>();

	/**
	 * Mengembalikan aturan pajak yang menempel pada baris ini, diurutkan berdasarkan nama, lewat
	 * tabel penghubung {@code sirs.racikan_detail_has_pajak}.
	 *
	 * @return himpunan aturan pajak, tidak pernah {@code null} (default himpunan kosong)
	 */
	@ManyToMany(targetEntity = PajakMedis.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "racikan_detail_has_pajak", schema = "sirs", joinColumns = @JoinColumn(name = "racikan_detail"), inverseJoinColumns = @JoinColumn(name = "pajak"))
	public Set<PajakMedis> getPajaks() {
		return pajaks;
	}

	/**
	 * Mengganti seluruh himpunan aturan pajak baris ini.
	 *
	 * @param pajaks himpunan aturan pajak yang baru
	 */
	public void setPajaks(Set<PajakMedis> pajaks) {
		this.pajaks = pajaks;
	}

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public RacikanDetail() {
	}

	/**
	 * Mengembalikan kunci utama baris komponen.
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
	 * Mengembalikan keterangan bebas baris komponen.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas baris komponen.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang menjadi komponen racikan pada baris ini.
	 *
	 * @param item item medis komponen, boleh {@code null}
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengembalikan item medis komponen racikan. Berbeda dari sebagian besar relasi di modul ini,
	 * getter ini tidak memanggil {@code check(...)} sehingga tetap murni-baca; relasi diambil
	 * dengan {@link FetchMode#SELECT}.
	 *
	 * @return item medis komponen, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		return item;
	}

	/**
	 * Menautkan baris komponen ini ke racikan induknya.
	 *
	 * @param racikan racikan induk, boleh {@code null}
	 */
	public void setRacikan(Racikan racikan) {
		this.racikan = racikan;
	}

	/**
	 * Mengembalikan racikan induk baris komponen ini. Kolom FK bersifat {@code nullable}, sehingga
	 * baris komponen tanpa induk (yatim) dapat tersimpan tanpa ditolak basis data.
	 *
	 * @return racikan induk, atau {@code null} bila baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "racikan", nullable = true)
	public Racikan getRacikan() {
		return racikan;
	}

	/**
	 * Mengisi takaran/kuantitas komponen.
	 *
	 * @param jumlah takaran komponen, boleh {@code null} (dinormalkan menjadi 0 saat dibaca)
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan takaran/kuantitas komponen, menormalkan {@code null} menjadi {@code 0.0}.
	 * <b>Getter destruktif</b>: normalisasi ditulis balik ke field. Nilai ini dipakai sebagai
	 * pengali pada {@link #getHasilPenghitunganTotal()}, sehingga takaran 0 menghasilkan total 0.
	 *
	 * @return takaran komponen, tidak pernah {@code null}
	 */
	public Double getJumlah() {
		if (jumlah == null) {
			jumlah = 0.0;
		}
		return jumlah;
	}

	/**
	 * Mengisi kuantitas komponen yang diretur.
	 *
	 * @param jumlahRetur kuantitas retur, boleh {@code null}
	 */
	public void setJumlahRetur(Double jumlahRetur) {
		this.jumlahRetur = jumlahRetur;
	}

	/**
	 * Mengembalikan kuantitas komponen yang diretur. Berbeda dari getter numerik lain di kelas ini,
	 * getter ini <b>tidak</b> menormalkan {@code null}, sehingga dapat mengembalikan {@code null}
	 * meskipun field diinisialisasi {@code 0.0} — nilai {@code null} muncul bila baris dimuat dari
	 * basis data dengan kolom {@code jumlah_retur} kosong. Pemanggil yang melakukan aritmetika
	 * langsung atas nilai ini perlu memeriksa {@code null} sendiri.
	 *
	 * @return kuantitas retur, dapat {@code null}
	 */
	@Column(name = "jumlah_retur", nullable = true)
	public Double getJumlahRetur() {
		return jumlahRetur;
	}

	/**
	 * Mengisi nilai diskon dalam rupiah. <b>Nilai yang ditulis lewat setter ini tidak akan
	 * bertahan</b>: {@link #getDiskon()} selalu menghitung ulang diskon dari
	 * {@link #getDiskonPersen()} sebelum mengembalikannya (lihat penjelasan lengkap pada
	 * {@link #getDiskon()}).
	 *
	 * @param diskon nilai diskon rupiah; efektif diabaikan oleh getter
	 */
	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * Mengembalikan nilai diskon baris komponen ini dalam rupiah, dihitung sebagai
	 * {@code hargaTransaksi * (diskonPersen / 100)}.
	 *
	 * <p>
	 * Secara tekstual, metode ini tampak menyediakan dua cara pengisian diskon. Langkah pertama
	 * menormalkan field {@code diskon} yang {@code null} menjadi {@code 0.0}, sehingga nilai rupiah
	 * yang sudah tersimpan sebelumnya — apakah dari {@link #setDiskon(Double)} maupun dari kolom
	 * basis data — dipertahankan apa adanya. Langkah kedua adalah penjaga
	 * {@code if (getDiskonPersen() != null && getHargaTransaksi() != null)} yang, bila terpenuhi,
	 * menimpa nilai rupiah tadi dengan hasil perhitungan dari persentase. Pembacaan wajar atas
	 * struktur ini adalah: "kalau persentase dan harga tersedia, hitung dari persentase; kalau
	 * tidak, pakai nilai rupiah yang sudah diisi operator".
	 * </p>
	 *
	 * <p>
	 * Pembacaan itu <b>tidak sesuai dengan perilaku sesungguhnya</b>. Penjaga tersebut memeriksa
	 * hasil dua getter, bukan dua field. {@link #getDiskonPersen()} menormalkan {@code null}
	 * menjadi {@code 0.0} sebelum mengembalikan nilainya, dan {@link #getHargaTransaksi()}
	 * melakukan hal yang persis sama. Keduanya karena itu tidak mungkin mengembalikan {@code null}
	 * dalam keadaan apa pun — objek baru, objek hasil pemuatan dengan kolom {@code NULL}, maupun
	 * objek yang field-nya baru saja dikosongkan secara eksplisit. Penjaga yang seharusnya memilih
	 * antara dua mode dengan demikian selalu bernilai benar, dan cabang persentase selalu
	 * dijalankan. Normalisasi pada langkah pertama menjadi tidak relevan karena nilainya langsung
	 * ditimpa pada langkah kedua.
	 * </p>
	 *
	 * <p>
	 * Dampak praktisnya: nilai diskon rupiah <b>tidak pernah dapat diberikan</b> pada baris racikan.
	 * Operator yang mengisi diskon dalam rupiah lewat {@link #setDiskon(Double)} akan melihat
	 * nilainya hilang begitu getter dipanggil — dan karena Hibernate memakai akses properti,
	 * getter itu ikut dipanggil saat entitas di-flush, sehingga nilai 0 itulah yang tersimpan ke
	 * basis data. Bila {@code diskonPersen} bernilai {@code 0.0} (nilai awal field, yakni keadaan
	 * ketika tidak ada aturan {@link Diskon} yang cocok), hasil akhirnya selalu {@code 0.0}: diskon
	 * yang secara sengaja dimasukkan operator lenyap tanpa pesan kesalahan apa pun. Satu-satunya
	 * cara diskon benar-benar berlaku pada baris racikan adalah lewat persentase, yaitu dengan
	 * mengisi {@link #setDiskonPersen(Double)} — yang pada alur normal diisi oleh
	 * {@code CommonSirs.getTotalDiskonDalamPersen}.
	 * </p>
	 *
	 * <p>
	 * Pola ini identik dengan yang sudah ditemukan pada {@link TransaksiMedisDetail} dan dicatat di
	 * sana: getter yang menormalkan {@code null} menjadi angka dipakai sebagai penjaga pemilih-mode,
	 * sehingga mode alternatifnya menjadi kode mati. Karena mekanisme, dampak, dan kelas kerugiannya
	 * sama persis, kejadian di sini didokumentasikan sebagai instance berulang dari pola yang sudah
	 * dikenal, bukan sebagai temuan tersendiri. Perbaikan yang setara di kedua tempat adalah
	 * memeriksa field mentah ({@code diskonPersen != null}) alih-alih hasil getter, atau menambahkan
	 * penanda mode eksplisit yang menyatakan apakah baris memakai diskon rupiah atau persen.
	 * </p>
	 *
	 * <p>
	 * Catatan tambahan: metode ini juga bersifat destruktif — hasil perhitungan ditulis balik ke
	 * field {@code diskon}, sehingga sekadar membaca nilai diskon sudah mengubah state entitas dan
	 * berpotensi menghasilkan pembaruan basis data pada flush berikutnya.
	 * </p>
	 *
	 * @return nilai diskon rupiah hasil perhitungan dari persentase, tidak pernah {@code null}
	 * @see #getDiskonPersen()
	 * @see #getHargaTransaksi()
	 */
	public Double getDiskon() {

		if (diskon == null) {
			diskon = 0.0;
		}

		if (getDiskonPersen() != null && getHargaTransaksi() != null) {
			diskon = getHargaTransaksi() * (getDiskonPersen() / 100.0);
		}

		return diskon;
	}

	/**
	 * Mengisi nilai pajak dalam rupiah. Sama seperti {@link #setDiskon(Double)}, nilai ini tidak
	 * akan bertahan karena {@link #getPajak()} selalu menghitung ulang dari
	 * {@link #getPajakPersen()}.
	 *
	 * @param pajak nilai pajak rupiah; efektif diabaikan oleh getter
	 */
	public void setPajak(Double pajak) {
		this.pajak = pajak;
	}

	/**
	 * Mengembalikan nilai pajak baris komponen ini dalam rupiah, dihitung sebagai
	 * {@code hargaTransaksi * (pajakPersen / 100)}.
	 *
	 * <p>
	 * Struktur metode ini merupakan cerminan persis {@link #getDiskon()}, termasuk cacatnya.
	 * Penjaga {@code if (getPajakPersen() != null && getHargaTransaksi() != null)} memeriksa hasil
	 * dua getter yang keduanya sudah menormalkan {@code null} menjadi {@code 0.0}
	 * ({@link #getPajakPersen()} dan {@link #getHargaTransaksi()}), sehingga penjaga itu selalu
	 * bernilai benar dan cabang perhitungan-dari-persentase selalu dijalankan. Nilai rupiah apa pun
	 * yang ditulis lewat {@link #setPajak(Double)} karena itu selalu ditimpa sebelum sempat dibaca
	 * atau disimpan.
	 * </p>
	 *
	 * <p>
	 * Untuk pajak, dampaknya berlawanan arah dengan diskon namun setara bobotnya dari sisi
	 * integritas: bila {@code pajakPersen} bernilai {@code 0.0}, pajak rupiah yang dimasukkan
	 * operator lenyap sehingga baris <b>kurang</b> menagihkan pajak; sebaliknya, penetapan pajak
	 * per-baris dalam bentuk nominal tetap (yang lazim untuk beberapa jenis pungutan) tidak dapat
	 * direpresentasikan sama sekali pada model ini. Sama seperti diskon, satu-satunya jalur yang
	 * berfungsi adalah persentase lewat {@link #setPajakPersen(Double)}, yang pada alur normal
	 * diisi {@code CommonSirs.getTotalPajakDalamPersen}.
	 * </p>
	 *
	 * <p>
	 * Metode ini juga destruktif: hasil perhitungan ditulis balik ke field {@code pajak}.
	 * </p>
	 *
	 * @return nilai pajak rupiah hasil perhitungan dari persentase, tidak pernah {@code null}
	 * @see #getDiskon() penjelasan lengkap mekanisme penjaga yang selalu benar
	 */
	public Double getPajak() {

		if (pajak == null) {
			pajak = 0.0;
		}

		if (getPajakPersen() != null && getHargaTransaksi() != null) {
			pajak = getHargaTransaksi() * (getPajakPersen() / 100.0);
		}

		return pajak;
	}

	/**
	 * Mengembalikan persentase diskon yang berlaku untuk baris ini, menormalkan {@code null}
	 * menjadi {@code 0.0}. <b>Getter destruktif</b>, dan sekaligus penyebab langsung penjaga
	 * pemilih-mode di {@link #getDiskon()} selalu bernilai benar.
	 *
	 * @return persentase diskon (0&ndash;100), tidak pernah {@code null}
	 */
	public Double getDiskonPersen() {
		if (diskonPersen == null) {
			diskonPersen = 0.0;
		}
		return diskonPersen;
	}

	/**
	 * Mengisi persentase diskon yang berlaku. Inilah satu-satunya jalur pengisian diskon yang
	 * benar-benar berpengaruh pada baris racikan.
	 *
	 * @param diskonPersen persentase diskon (0&ndash;100); tidak divalidasi rentangnya di sini
	 */
	public void setDiskonPersen(Double diskonPersen) {
		this.diskonPersen = diskonPersen;
	}

	/**
	 * Mengembalikan persentase pajak yang berlaku untuk baris ini, menormalkan {@code null} menjadi
	 * {@code 0.0}. <b>Getter destruktif</b>, dan penyebab langsung penjaga pemilih-mode di
	 * {@link #getPajak()} selalu bernilai benar.
	 *
	 * @return persentase pajak (0&ndash;100), tidak pernah {@code null}
	 */
	public Double getPajakPersen() {
		if (pajakPersen == null) {
			pajakPersen = 0.0;
		}
		return pajakPersen;
	}

	/**
	 * Mengisi persentase pajak yang berlaku. Satu-satunya jalur pengisian pajak yang berpengaruh
	 * pada baris racikan.
	 *
	 * @param pajakPersen persentase pajak (0&ndash;100); tidak divalidasi rentangnya di sini
	 */
	public void setPajakPersen(Double pajakPersen) {
		this.pajakPersen = pajakPersen;
	}

	/**
	 * Menghitung nilai total baris komponen ini dengan rumus
	 * {@code jumlah * ((hargaTransaksi - diskon) + pajak)}.
	 *
	 * <p>
	 * Urutan operasinya perlu dicermati karena menentukan basis perhitungan pajak. Diskon dan pajak
	 * di sini bukan dua penyesuaian berurutan atas harga: keduanya dihitung dari basis yang sama,
	 * yaitu {@link #getHargaTransaksi()} — lihat {@link #getDiskon()} dan {@link #getPajak()} yang
	 * masing-masing mengalikan harga transaksi dengan persentasenya sendiri. Rumus di metode ini
	 * hanya menjumlahkan hasilnya secara aljabar. Konsekuensinya, <b>pajak dihitung atas harga
	 * sebelum diskon</b>, bukan atas harga setelah diskon. Untuk baris dengan harga 100.000,
	 * diskon 10%, dan pajak 10%, hasilnya adalah 100.000 &minus; 10.000 + 10.000 = 100.000 per
	 * satuan; bila pajak dihitung atas dasar pengenaan setelah diskon, hasilnya akan 99.000. Selisih
	 * ini bersifat sistematis dan membesar seiring nilai transaksi, sehingga penting diketahui saat
	 * merekonsiliasi angka aplikasi dengan perhitungan pajak manual.
	 * </p>
	 *
	 * <p>
	 * Pengali {@link #getJumlah()} diterapkan terakhir atas seluruh nilai bersih per satuan. Karena
	 * getter itu menormalkan {@code null} menjadi {@code 0.0}, baris yang takarannya belum diisi
	 * akan menghasilkan total {@code 0.0} tanpa peringatan apa pun, bukan kesalahan.
	 * </p>
	 *
	 * <p>
	 * Metode ini bersifat destruktif dalam arti yang lebih kuat daripada getter lain di kelas ini:
	 * ia memanggil {@link #getHargaTransaksi()}, {@link #getDiskon()}, {@link #getPajak()}, dan
	 * {@link #getJumlah()}, yang masing-masing menulis balik ke field-nya sendiri, lalu menulis
	 * hasil akhirnya ke field {@link #hasilPenghitunganTotal}. Sekali pemanggilan getter total
	 * karena itu dapat mengubah hingga lima field sekaligus. Karena {@code hasilPenghitunganTotal}
	 * tidak ditandai {@code @Transient}, nilainya ikut dipetakan ke kolom dan tersimpan — sebuah
	 * denormalisasi yang berpotensi menyimpang dari komponennya bila salah satu komponen berubah
	 * (mis. harga diperbarui) tanpa getter ini dipanggil ulang sebelum flush. Laporan yang membaca
	 * kolom itu langsung lewat SQL, alih-alih lewat entitas, karena itu bisa menampilkan angka
	 * basi.
	 * </p>
	 *
	 * <p>
	 * Perhatikan pula bahwa {@link #getJumlahRetur()} sama sekali tidak ikut dalam rumus ini: nilai
	 * total baris tidak berkurang oleh kuantitas yang diretur. Pengurangan akibat retur ditangani
	 * di tempat lain, yaitu lewat baris transaksi retur tersendiri
	 * ({@link Racikan#getTransaksiReturDetail()}), bukan dengan mengoreksi baris asalnya.
	 * </p>
	 *
	 * @return total nilai baris setelah diskon dan pajak, dikali takaran; tidak pernah {@code null}
	 */
	public Double getHasilPenghitunganTotal() {
		Double jumlah = ((getHargaTransaksi() - getDiskon()) + getPajak());
		hasilPenghitunganTotal = getJumlah() * jumlah;
		return hasilPenghitunganTotal;
	}

	/**
	 * Mengisi nilai total baris secara langsung. Nilai ini akan ditimpa pada pemanggilan
	 * {@link #getHasilPenghitunganTotal()} berikutnya, sehingga setter ini praktis hanya berguna
	 * untuk kerangka kerja persistensi saat memuat entitas.
	 *
	 * @param hasilPenghitunganTotal nilai total baris
	 */
	public void setHasilPenghitunganTotal(Double hasilPenghitunganTotal) {
		this.hasilPenghitunganTotal = hasilPenghitunganTotal;
	}
}
