package ais.database.model.sirs;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;

/**
 * Entitas <b>komponen biaya</b> modul SIRS — satu baris rincian pembentuk tarif atau satu baris
 * penyesuaian (diskon/pajak) atas tarif tersebut. Inilah "buku besar mini" tarif medis: sebuah
 * tindakan seharga Rp 500.000 tidak disimpan sebagai satu angka, melainkan sebagai kumpulan baris
 * {@code Biaya} (jasa dokter, jasa sarana, bahan habis pakai, administrasi, dan seterusnya), yang
 * masing-masing membawa {@link JenisBiaya} sendiri dan {@link Akun} buku besar sendiri sehingga
 * pendapatan dapat langsung dipecah ke akun akuntansi yang tepat.
 *
 * <h3>Urutan resolusi tarif medis dan posisi kelas ini di dalamnya</h3>
 * Penetapan harga layanan medis di modul SIRS berlangsung berlapis. Urutan sesungguhnya — hasil
 * penelusuran {@code ais.action.master.sirs.util.CommonTarifTindakan},
 * {@code CommonTarifAlatMedis}, {@code CommonTarif}, dan
 * {@code ais.action.master.sirs.util.CommonPendaftaranUtil} — adalah sebagai berikut:
 * <ol>
 * <li><b>Pemilihan tarif khusus lebih dahulu, bukan belakangan.</b> Berbeda dari dugaan umum bahwa
 * {@link TarifKhusus} adalah <i>override</i> yang diterapkan di akhir atas harga dasar, tarif khusus
 * justru diselesaikan <b>paling awal</b>. {@code CommonTarif.getTarif} mencari baris
 * {@link TarifKhususPunyaTindakan}/{@link TarifKhususPunyaAlatMedis}/{@link TarifKhususPunyaItem}
 * yang berlaku hari ini untuk kombinasi dokter/asuransi/komunitas/pasien, dengan strategi
 * generalisasi bertingkat: dimulai dari kecocokan paling spesifik lalu dilonggarkan langkah demi
 * langkah hingga jatuh ke tarif umum. Hasilnya menentukan <i>baris tarif mana</i> yang dipakai,
 * bukan mengoreksi angka yang sudah dihitung.</li>
 * <li><b>Tarif per kelas perawatan.</b> Dengan tarif khusus (atau {@code null} bila tak ada) di
 * tangan, {@code getBiayaTindakanPerKelas}/{@code getBiayaAlatMedisPerKelas} mengambil baris
 * {@link BiayaTindakanPerKelas} atau {@link BiayaAlatMedisPerKelas} untuk kelas perawatan yang
 * bersangkutan. Perhatikan bahwa baris tarif khusus dan baris tarif standar disimpan di
 * <b>tabel yang sama</b> dan dibedakan hanya oleh pola pengisian kolom: tarif standar memiliki
 * {@code tindakan} terisi dan {@code tarif_khusus_punya_tindakan} kosong, sedangkan tarif khusus
 * kebalikannya. Jadi tarif khusus <b>menggantikan</b> baris tarif dasar, tidak bertumpuk di
 * atasnya — tidak ada situasi "keduanya berlaku lalu salah satu menang".</li>
 * <li><b>Rincian komponen biaya.</b> Baris-baris {@code Biaya} yang menempel pada baris tarif
 * per-kelas tersebut (atau pada {@link HargaJualItem} untuk barang/obat) dijumlahkan menjadi nilai
 * tarif. Inilah peran utama kelas ini.</li>
 * <li><b>Diskon dan pajak, keduanya atas basis yang sama.</b> {@link Diskon} dan
 * {@link PajakMedis} yang berlaku diterapkan terakhir, dan hasilnya <b>juga dicatat sebagai baris
 * {@code Biaya}</b> — diskon sebagai baris bernilai negatif, pajak sebagai baris positif. Penting
 * dicatat: keduanya dihitung sebagai persentase dari {@code detailTransaksi(Layanan).getAmount()}
 * yang sama, sehingga <b>pajak dikenakan atas jumlah sebelum diskon</b>, bukan atas dasar
 * pengenaan setelah diskon. Diskon dan pajak juga tidak saling meniadakan atau berebut menang;
 * seluruh aturan yang cocok diterapkan bersama-sama secara akumulatif.</li>
 * </ol>
 * Ringkasnya: lapisan 1&ndash;2 memilih <i>baris tarif mana</i>, lapisan 3 menjumlahkan
 * <i>berapa</i>, lapisan 4 menambahkan penyesuaian. Tidak ada tumpang tindih yang perlu
 * diperebutkan antar lapisan.
 *
 * <h3>Satu tabel, empat peran</h3>
 * Baris {@code Biaya} dipakai untuk empat maksud berbeda, dibedakan hanya oleh kombinasi kolom mana
 * yang terisi — tanpa kolom diskriminator eksplisit dan tanpa <i>check constraint</i>:
 * <ol>
 * <li><b>Baris cetakan (template) tarif</b> — {@link #getJenisBiaya()} terisi, salah satu dari
 * {@link #getBiayaTindakanPerKelas()}/{@link #getBiayaAlatMedisPerKelas()}/{@link #getHargaJualItem()}
 * terisi, sedangkan {@link #getDetailTransaksiLayanan()} dan {@link #getDetailTransaksi()} keduanya
 * kosong. Inilah rincian tarif master yang dikonfigurasi operator.</li>
 * <li><b>Baris transaksi</b> — salinan (lewat {@code clone()} dengan id dikosongkan) dari baris
 * cetakan, dengan {@code detailTransaksiLayanan} atau {@code detailTransaksi} terisi. Inilah yang
 * benar-benar ditagihkan dan diposting ke akuntansi.</li>
 * <li><b>Baris diskon</b> — {@link #getDiskon()} terisi menggantikan {@code jenisBiaya}; nilainya
 * negatif.</li>
 * <li><b>Baris pajak</b> — {@link #getPajak()} terisi menggantikan {@code jenisBiaya}.</li>
 * </ol>
 * Pembedaan peran itu dilakukan lewat kriteria kueri ({@code Restrictions.isNull("detailTransaksi")}
 * dan sejenisnya) di kode pemanggil, sehingga kesalahan penulisan kriteria menyebabkan baris
 * cetakan ikut terhitung sebagai transaksi atau sebaliknya, tanpa peringatan dari basis data.
 *
 * <h3>PERINGATAN — getter turunan yang saling bergantung dan destruktif</h3>
 * {@link #getJumlah()}, {@link #getPersen()}, dan {@link #getJumlahTotal()} bukan getter biasa: ia
 * menghitung ulang nilai dari relasi induknya dan menulis hasilnya balik ke field. Dua di antaranya
 * <b>saling memanggil</b> — {@link #getJumlah()} memanggil {@link #getPersen()} dan sebaliknya —
 * sehingga yang mencegah rekursi tak berhingga hanyalah kenyataan bahwa syarat penjaganya adalah
 * negasi persis satu sama lain: {@code getJumlah()} masuk cabang perhitungan hanya ketika
 * {@code getPembagianBiayaDalamPersen()} bernilai benar, sedangkan {@code getPersen()} hanya ketika
 * bernilai salah. Invarian ini rapuh dan tidak tertulis di mana pun pada kode aslinya: mengubah
 * salah satu syarat (mis. menambahkan kondisi tambahan atau membalik negasinya) akan langsung
 * menghasilkan {@code StackOverflowError} pada saat entitas dibaca. Siapa pun yang menyunting
 * kedua metode itu wajib mempertahankan sifat saling-meniadakan tersebut.
 *
 * <h3>Pola arsitektur lain</h3>
 * <ul>
 * <li><b>Getter destruktif</b> di hampir seluruh relasi ({@code check(...)}) dan seluruh nilai
 * turunan; membaca entitas ini dapat mengubah state-nya dan menghasilkan pembaruan basis data pada
 * flush berikutnya.</li>
 * <li><b>Field audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}: keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — sesuai keterbatasan seluruh modul {@code sirs}.</li>
 * <li>{@code jumlah}, {@code persen}, {@code jumlahTotal}, {@code tanggal}, {@code tgl},
 * {@code bln}, dan {@code thn} tidak diberi {@code @Column} maupun {@code @Transient}, sehingga
 * seluruhnya dipetakan Hibernate ke kolom bernama sesuai nama properti — termasuk nilai-nilai
 * turunan.</li>
 * </ul>
 *
 * @see JenisBiaya penggolong komponen biaya beserta akun buku besarnya
 * @see BiayaTindakanPerKelas tarif tindakan per kelas perawatan
 * @see BiayaAlatMedisPerKelas tarif alat medis per kelas perawatan
 * @see Diskon aturan diskon yang dicatat sebagai baris biaya negatif
 * @see PajakMedis aturan pajak yang dicatat sebagai baris biaya positif
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "biaya")
public class Biaya extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.biaya}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris biaya ini.
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
	 * Representasi teks baris biaya untuk komponen ZK, berbentuk {@code jenisBiaya-jumlah}. Memakai
	 * kedua field langsung tanpa lewat getter, sehingga menampilkan nilai {@code jumlah} yang
	 * tersimpan apa adanya — bukan hasil perhitungan ulang {@link #getJumlah()} — dan menghasilkan
	 * awalan {@code "null"} untuk baris diskon/pajak yang memang tidak memiliki jenis biaya.
	 *
	 * @return label baris biaya
	 */
	public String toString() {
		return jenisBiaya + "-" + jumlah;
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
	 * Mengembalikan nama pengguna yang terakhir mengubah baris biaya ini.
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
	 * Mengembalikan cap waktu perubahan terakhir baris ini. Berbeda dari {@link #getTanggal()},
	 * yang merupakan tanggal domain baris biaya.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Aturan diskon bila baris ini berperan sebagai baris diskon (menggantikan {@link #jenisBiaya}). */
	private Diskon diskon;

	/** Aturan pajak bila baris ini berperan sebagai baris pajak (menggantikan {@link #jenisBiaya}). */
	private PajakMedis pajak;

	/** Penggolong komponen biaya (jasa dokter, BHP, administrasi, dan sebagainya). */
	private JenisBiaya jenisBiaya;

	/** Nilai komponen dalam rupiah; nilai turunan pada mode pembagian-persen. */
	private Double jumlah = 0.0;

	/** Porsi komponen dalam persen; nilai turunan pada mode pembagian-rupiah. */
	private Double persen = 0.0;

	/** Basis perhitungan porsi persen: nilai penuh tarif atau amount baris transaksi. */
	private Double jumlahTotal = 0.0;

	/** Keterangan bebas atas baris biaya. */
	private String keterangan;

	/** Tanggal domain baris biaya; menjadi sumber {@link #tgl}, {@link #bln}, dan {@link #thn}. */
	private Date tanggal;

	/** Baris tarif alat medis per kelas yang menjadi induk baris cetakan ini. */
	private BiayaAlatMedisPerKelas biayaAlatMedisPerKelas;

	/** Baris tarif tindakan per kelas yang menjadi induk baris cetakan ini. */
	private BiayaTindakanPerKelas biayaTindakanPerKelas;

	/** Harga jual item/barang yang menjadi induk baris cetakan ini. */
	private HargaJualItem hargaJualItem;

	/** Penggolong biaya lain-lain (kas/deposit/penjualan) beserta akunnya. */
	private JenisBiayaLain jenisBiayaLain;

	/** Baris transaksi layanan yang menagihkan baris biaya ini; menandai peran "baris transaksi". */
	private DetailTransaksiLayanan detailTransaksiLayanan;

	/** Baris transaksi pasien yang menagihkan baris biaya ini; menandai peran "baris transaksi". */
	private DetailTransaksiPasien detailTransaksi;

	/** Akun buku besar tujuan posting baris biaya ini; kolomnya {@code NOT NULL}. */
	private Akun akun;

	/** Komponen tanggal (hari) turunan dari {@link #tanggal}, untuk pengelompokan laporan. */
	private Integer tgl;

	/** Komponen bulan (1&ndash;12) turunan dari {@link #tanggal}, untuk pengelompokan laporan. */
	private Integer bln;

	/** Komponen tahun turunan dari {@link #tanggal}, untuk pengelompokan laporan. */
	private Integer thn;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public Biaya() {
	}

	/**
	 * Mengembalikan kunci utama baris biaya.
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
	 * Mengisi kunci utama. Selain dipakai kerangka kerja persistensi, setter ini penting pada alur
	 * penyalinan baris cetakan menjadi baris transaksi: kode pemanggil melakukan {@code clone()}
	 * lalu {@code setId(null)} agar salinan disimpan sebagai baris baru.
	 *
	 * @param id kunci utama baris; {@code null} untuk menandai salinan sebagai baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris biaya, menormalkan {@code null} menjadi string kosong.
	 * <b>Getter destruktif</b>: normalisasi ditulis balik ke field, sehingga baris yang keterangannya
	 * {@code NULL} di basis data dapat berubah menjadi string kosong hanya karena dibaca.
	 *
	 * @return keterangan, tidak pernah {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas baris biaya.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nilai komponen biaya ini dalam rupiah, menghitung ulang dari porsi persen bila
	 * induk tarifnya dikonfigurasi dalam mode pembagian-persen.
	 *
	 * <p>
	 * Modul ini menyediakan dua cara mengonfigurasi rincian tarif. Pada <b>mode rupiah</b>
	 * ({@code pembagianBiayaDalamPersen} bernilai salah pada induk tarif), operator mengisi nilai
	 * rupiah tiap komponen secara langsung dan porsi persennya-lah yang diturunkan — lihat
	 * {@link #getPersen()}. Pada <b>mode persen</b> ({@code pembagianBiayaDalamPersen} bernilai
	 * benar), yang diisi operator adalah porsi persen tiap komponen dan nilai rupiahnya diturunkan
	 * di sini, dengan rumus {@code jumlahTotal * (persen / 100)}. Metode ini memeriksa mode tersebut
	 * pada ketiga kemungkinan induk tarif secara berurutan: {@link #getBiayaTindakanPerKelas()},
	 * lalu {@link #getBiayaAlatMedisPerKelas()}, lalu {@link #getHargaJualItem()}. Cabang pertama
	 * yang induknya ada <i>dan</i> bermode persen menang; bila tidak satu pun cocok, nilai
	 * {@code jumlah} yang tersimpan dipakai apa adanya.
	 * </p>
	 *
	 * <p>
	 * Setelah nilai per satuan diperoleh, metode ini menerapkan dua pengali tambahan bila baris ini
	 * sudah berperan sebagai baris transaksi — yaitu bila {@link #getDetailTransaksiLayanan()} atau
	 * {@link #getDetailTransaksi()} terisi. Pengali pertama adalah kuantitas baris transaksi
	 * ({@code getQty()}). Pengali kedua adalah {@code -kodeTransaksi.getJenis()}, yakni tanda arah
	 * transaksi yang dibalik: kode transaksi membawa penanda debit/kredit, dan pembalikan tanda di
	 * sini membuat baris biaya bernilai positif untuk penjualan/penagihan dan negatif untuk
	 * pembatalan/retur, sehingga penjumlahan seluruh baris langsung menghasilkan nilai bersih tanpa
	 * perlu memisahkan arahnya. Perhatikan bahwa kedua pengali itu dibaca lewat field mentah
	 * ({@code detailTransaksiLayanan}, {@code detailTransaksi}), bukan lewat getter, sehingga proxy
	 * malas yang belum dimaterialisasi tetap terbaca sebagai referensi non-{@code null} dan cabang
	 * pengali tetap dijalankan.
	 * </p>
	 *
	 * <p>
	 * Dua sifat metode ini perlu diwaspadai. <b>Pertama, ia destruktif dan berpotensi menimpa nilai
	 * tersimpan.</b> Hasil perhitungan ditulis balik ke field {@code jumlah}. Karena Hibernate
	 * memakai akses properti, getter ini ikut dipanggil saat entitas di-flush, sehingga nilai yang
	 * sebelumnya ditulis eksplisit lewat {@link #setJumlah(Double)} — misalnya oleh
	 * {@code CommonPendaftaranUtil} yang menyalin baris cetakan menjadi baris transaksi lalu
	 * mengalikannya sendiri dengan qty dan tanda — dapat dihitung ulang dan ditimpa pada pembacaan
	 * berikutnya, dengan basis {@link #getJumlahTotal()} yang untuk baris transaksi sudah berubah
	 * menjadi {@code amount} baris transaksi itu, bukan nilai tarif master. Nilai yang tersimpan di
	 * kolom karena itu tidak dijamin stabil antar pembacaan pada baris bermode persen; laporan yang
	 * membaca kolom {@code jumlah} langsung lewat SQL dapat memperoleh angka yang berbeda dari yang
	 * dilihat aplikasi.
	 * </p>
	 *
	 * <p>
	 * <b>Kedua, metode ini saling memanggil dengan {@link #getPersen()}.</b> Cabang perhitungan di
	 * sini memanggil {@code getPersen()}, sementara {@code getPersen()} memanggil {@code getJumlah()}
	 * pada cabangnya sendiri. Yang mencegah rekursi tak berhingga semata-mata adalah kenyataan bahwa
	 * kedua penjaga merupakan negasi persis satu sama lain — di sini {@code getPembagianBiayaDalamPersen()}
	 * harus benar, di sana harus salah. Invarian itu tidak dinyatakan di mana pun pada kode asli dan
	 * mudah dirusak tanpa sengaja; menambah syarat pada salah satu penjaga, atau membalik salah satu
	 * negasi, akan langsung menghasilkan {@code StackOverflowError} pada saat entitas dibaca. Siapa
	 * pun yang menyunting pasangan metode ini wajib mempertahankan sifat saling-meniadakan tersebut.
	 * </p>
	 *
	 * <p>
	 * Sebagai penutup, nilai {@code null} maupun {@code NaN} (yang dapat muncul dari pembagian nol
	 * pada {@link #getPersen()}) dinormalkan menjadi {@code 0.0} sebelum dikembalikan.
	 * </p>
	 *
	 * @return nilai komponen biaya dalam rupiah, tidak pernah {@code null} maupun {@code NaN}
	 * @see #getPersen() pasangan turunan arah sebaliknya
	 * @see #getJumlahTotal() basis perhitungan porsi persen
	 */
	public Double getJumlah() {

		if (getBiayaTindakanPerKelas() != null && getBiayaTindakanPerKelas().getPembagianBiayaDalamPersen()) {
			jumlah = getJumlahTotal() * (getPersen() / 100.0);

			if (detailTransaksiLayanan != null) {
				jumlah = (-detailTransaksiLayanan.getKodeTransaksi().getJenis()) * detailTransaksiLayanan.getQty()
						* jumlah;
			} else if (detailTransaksi != null) {
				jumlah = (-detailTransaksi.getKodeTransaksi().getJenis()) * detailTransaksi.getQty() * jumlah;
			}
		} else if (getBiayaAlatMedisPerKelas() != null && getBiayaAlatMedisPerKelas().getPembagianBiayaDalamPersen()) {
			jumlah = getJumlahTotal() * (getPersen() / 100.0);

			if (detailTransaksiLayanan != null) {
				jumlah = (-detailTransaksiLayanan.getKodeTransaksi().getJenis()) * detailTransaksiLayanan.getQty()
						* jumlah;
			} else if (detailTransaksi != null) {
				jumlah = (-detailTransaksi.getKodeTransaksi().getJenis()) * detailTransaksi.getQty() * jumlah;
			}
		} else if (getHargaJualItem() != null && getHargaJualItem().getPembagianBiayaDalamPersen()) {
			jumlah = getJumlahTotal() * (getPersen() / 100.0);

			if (detailTransaksiLayanan != null) {
				jumlah = (-detailTransaksiLayanan.getKodeTransaksi().getJenis()) * detailTransaksiLayanan.getQty()
						* jumlah;
			} else if (detailTransaksi != null) {
				jumlah = (-detailTransaksi.getKodeTransaksi().getJenis()) * detailTransaksi.getQty() * jumlah;
			}
		}

		if (jumlah == null || Double.isNaN(jumlah)) {
			jumlah = 0.0;
		}
		return jumlah;
	}

	/**
	 * Mengisi nilai komponen biaya dalam rupiah. Pada baris bermode persen, nilai ini akan dihitung
	 * ulang dan ditimpa oleh {@link #getJumlah()} pada pembacaan berikutnya; pada baris bermode
	 * rupiah, nilai inilah yang bertahan dan justru menjadi sumber turunan {@link #getPersen()}.
	 *
	 * @param jumlah nilai komponen dalam rupiah
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan penggolong komponen biaya. <b>Getter destruktif</b>: memanggil {@code check(...)}
	 * yang memaksa materialisasi proxy malas Hibernate dan menulis hasilnya balik ke field.
	 *
	 * @return jenis biaya, atau {@code null} bila baris ini berperan sebagai baris diskon/pajak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya", nullable = true)
	public JenisBiaya getJenisBiaya() {
		jenisBiaya = check(jenisBiaya);
		return jenisBiaya;
	}

	/**
	 * Menetapkan penggolong komponen biaya.
	 *
	 * @param jenisBiaya jenis biaya, boleh {@code null} untuk baris diskon/pajak
	 */
	public void setJenisBiaya(JenisBiaya jenisBiaya) {
		this.jenisBiaya = jenisBiaya;
	}

	/**
	 * Mengembalikan baris tarif tindakan per kelas yang menjadi induk baris cetakan ini.
	 * <b>Getter destruktif</b> ({@code check(...)}). Dipanggil pertama pada rantai pemeriksaan mode
	 * di {@link #getJumlah()} dan {@link #getPersen()}, sehingga induk tindakan diprioritaskan bila
	 * (secara tidak lazim) lebih dari satu induk terisi.
	 *
	 * @return tarif tindakan per kelas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biaya_tindakan_per_kelas", nullable = true)
	public BiayaTindakanPerKelas getBiayaTindakanPerKelas() {
		biayaTindakanPerKelas = check(biayaTindakanPerKelas);
		return biayaTindakanPerKelas;
	}

	/**
	 * Menautkan baris biaya ini ke tarif tindakan per kelas sebagai induknya.
	 *
	 * @param biayaTindakanPerKelas tarif tindakan per kelas, boleh {@code null}
	 */
	public void setBiayaTindakanPerKelas(BiayaTindakanPerKelas biayaTindakanPerKelas) {
		this.biayaTindakanPerKelas = biayaTindakanPerKelas;
	}

	/**
	 * Mengembalikan baris tarif alat medis per kelas yang menjadi induk baris cetakan ini. Getter
	 * ini murni-baca (tanpa {@code check(...)}) dan memakai {@link FetchMode#SELECT}.
	 *
	 * @return tarif alat medis per kelas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "biaya_alat_medis_per_kelas", nullable = true)
	public BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas() {
		return biayaAlatMedisPerKelas;
	}

	/**
	 * Menautkan baris biaya ini ke tarif alat medis per kelas sebagai induknya.
	 *
	 * @param biayaAlatMedisPerKelas tarif alat medis per kelas, boleh {@code null}
	 */
	public void setBiayaAlatMedisPerKelas(BiayaAlatMedisPerKelas biayaAlatMedisPerKelas) {
		this.biayaAlatMedisPerKelas = biayaAlatMedisPerKelas;
	}

	/**
	 * Mengembalikan baris transaksi layanan yang menagihkan baris biaya ini. Terisinya nilai ini
	 * adalah penanda bahwa baris ini berperan sebagai baris transaksi, bukan baris cetakan tarif.
	 *
	 * @return baris transaksi layanan, atau {@code null} bila baris ini masih berupa cetakan tarif
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_transaksi_layanan", nullable = true)
	public DetailTransaksiLayanan getDetailTransaksiLayanan() {
		return detailTransaksiLayanan;
	}

	/**
	 * Menautkan baris biaya ini ke baris transaksi layanan, sekaligus mengubah perannya dari cetakan
	 * tarif menjadi baris transaksi.
	 *
	 * @param detailTransaksiLayanan baris transaksi layanan, boleh {@code null}
	 */
	public void setDetailTransaksiLayanan(DetailTransaksiLayanan detailTransaksiLayanan) {
		this.detailTransaksiLayanan = detailTransaksiLayanan;
	}

	/**
	 * Mengembalikan baris transaksi pasien yang menagihkan baris biaya ini. Sama seperti
	 * {@link #getDetailTransaksiLayanan()}, terisinya nilai ini menandai peran "baris transaksi".
	 *
	 * @return baris transaksi pasien, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_transaksi", nullable = true)
	public DetailTransaksiPasien getDetailTransaksi() {
		return detailTransaksi;
	}

	/**
	 * Menautkan baris biaya ini ke baris transaksi pasien.
	 *
	 * @param detailTransaksi baris transaksi pasien, boleh {@code null}
	 */
	public void setDetailTransaksi(DetailTransaksiPasien detailTransaksi) {
		this.detailTransaksi = detailTransaksi;
	}

	/**
	 * Mengembalikan akun buku besar tujuan posting baris biaya ini, dengan rantai pewarisan dari
	 * entitas induk bila akun baris belum ditetapkan sendiri.
	 *
	 * <p>
	 * Kolom {@code akun} bersifat {@code NOT NULL} di basis data, tetapi pada praktiknya operator
	 * jarang menetapkan akun per baris biaya: akun sudah melekat pada masternya. Metode ini menutup
	 * jarak itu dengan mencoba empat sumber warisan secara berurutan, masing-masing hanya bila akun
	 * baris masih {@code null}: {@link JenisBiaya#getAkun()} lebih dahulu (kasus paling umum, yaitu
	 * baris rincian tarif), lalu {@link Diskon#getAkun()} (baris diskon), lalu
	 * {@link PajakMedis#getAkun()} (baris pajak), lalu {@link JenisBiayaLain#getAkun()} (baris biaya
	 * lain-lain seperti setoran kas atau deposit). Urutan itu sekaligus mencerminkan urutan
	 * kelaziman keempat peran baris {@code Biaya}. Bila tidak satu pun sumber tersedia, cabang
	 * terakhir memanggil {@code check(akun)} yang sekadar memaksa materialisasi proxy akun yang
	 * sudah ada — atau mengembalikan {@code null} bila memang tidak ada.
	 * </p>
	 *
	 * <p>
	 * Seluruh rantai itu dibungkus {@code try/catch (Exception)} yang menelan setiap kesalahan tanpa
	 * jejak, lalu jatuh ke {@code check(akun)} yang sama. Pembungkusan tersebut memang melindungi
	 * pembacaan entitas dari kegagalan materialisasi proxy pada relasi induk yang sudah tidak ada
	 * atau tidak dapat dimuat (mis. sesi Hibernate sudah tertutup), sehingga layar tidak ambruk
	 * hanya karena satu baris biaya bermasalah. Harganya adalah kesalahan itu menjadi tidak terlihat
	 * sama sekali: getter mengembalikan {@code null} tanpa keluhan, dan karena kolomnya
	 * {@code NOT NULL}, kegagalan baru muncul jauh kemudian sebagai pelanggaran <i>constraint</i>
	 * saat penyimpanan — pesan galat yang tidak menunjuk sebab aslinya. Saat menelusuri kasus
	 * "biaya gagal disimpan karena akun kosong", periksa lebih dulu apakah induk tarifnya
	 * ({@code jenisBiaya}, {@code diskon}, {@code pajak}, atau {@code jenisBiayaLain}) benar-benar
	 * memiliki akun.
	 * </p>
	 *
	 * <p>
	 * Metode ini destruktif: akun hasil pewarisan ditulis balik ke field, sehingga baris yang semula
	 * mewarisi akun dari induknya akan tersimpan dengan akun itu secara eksplisit pada flush
	 * berikutnya. Konsekuensinya, mengubah akun pada master {@link JenisBiaya} <b>tidak</b> mengubah
	 * baris biaya yang sudah pernah dibaca dan disimpan — pewarisan hanya berlaku sekali, saat
	 * pembacaan pertama. Sifat ini justru diinginkan untuk baris transaksi (agar posting historis
	 * tidak berubah surut ketika bagan akun disunting), tetapi berarti perubahan akun master perlu
	 * disertai penyesuaian data untuk baris cetakan tarif yang sudah terlanjur terisi.
	 * </p>
	 *
	 * @return akun buku besar baris ini, atau {@code null} bila tidak ada sumber warisan yang tersedia
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = false)
	public Akun getAkun() {
		try {
			if (akun == null && getJenisBiaya() != null && getJenisBiaya().getAkun() != null) {
				akun = getJenisBiaya().getAkun();
			} else if (akun == null && getDiskon() != null && getDiskon().getAkun() != null) {
				akun = getDiskon().getAkun();
			} else if (akun == null && getPajak() != null && getPajak().getAkun() != null) {
				akun = getPajak().getAkun();
			} else if (akun == null && getJenisBiayaLain() != null && getJenisBiayaLain().getAkun() != null) {
				akun = getJenisBiayaLain().getAkun();
			} else {
				akun = check(akun);
			}
		} catch (Exception e) {
			akun = check(akun);
		}
		return akun;
	}

	/**
	 * Menetapkan akun buku besar baris biaya ini secara eksplisit, mengalahkan seluruh rantai
	 * pewarisan di {@link #getAkun()}.
	 *
	 * @param akun akun buku besar tujuan posting
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan harga jual item/barang yang menjadi induk baris cetakan ini. Getter murni-baca
	 * dengan {@link FetchMode#SELECT}. Induk jenis ini dipakai untuk rincian biaya atas obat/barang,
	 * bukan atas jasa/tindakan.
	 *
	 * @return harga jual item, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "harga_jual_item", nullable = true)
	public HargaJualItem getHargaJualItem() {
		return hargaJualItem;
	}

	/**
	 * Menautkan baris biaya ini ke harga jual item sebagai induknya.
	 *
	 * @param hargaJualItem harga jual item, boleh {@code null}
	 */
	public void setHargaJualItem(HargaJualItem hargaJualItem) {
		this.hargaJualItem = hargaJualItem;
	}

	/**
	 * Mengembalikan aturan diskon bila baris ini berperan sebagai baris diskon. <b>Getter
	 * destruktif</b> ({@code check(...)}).
	 *
	 * @return aturan diskon, atau {@code null} bila baris ini bukan baris diskon
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon", nullable = true)
	public Diskon getDiskon() {
		diskon = check(diskon);
		return diskon;
	}

	/**
	 * Menjadikan baris ini baris diskon dengan menautkannya ke aturan diskon tertentu. Nilai
	 * rupiahnya diisi terpisah lewat {@link #setJumlah(Double)} dan secara konvensi bertanda
	 * negatif.
	 *
	 * @param diskon aturan diskon, boleh {@code null}
	 */
	public void setDiskon(Diskon diskon) {
		this.diskon = diskon;
	}

	/**
	 * Mengembalikan aturan pajak bila baris ini berperan sebagai baris pajak. <b>Getter
	 * destruktif</b> ({@code check(...)}).
	 *
	 * @return aturan pajak, atau {@code null} bila baris ini bukan baris pajak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pajak", nullable = true)
	public PajakMedis getPajak() {
		pajak = check(pajak);
		return pajak;
	}

	/**
	 * Menjadikan baris ini baris pajak dengan menautkannya ke aturan pajak tertentu.
	 *
	 * @param pajak aturan pajak, boleh {@code null}
	 */
	public void setPajak(PajakMedis pajak) {
		this.pajak = pajak;
	}

	/**
	 * Mengembalikan penggolong biaya lain-lain (kas, deposit, penjualan, pembelian). <b>Getter
	 * destruktif</b> ({@code check(...)}). Dipakai untuk baris biaya di luar tarif medis, dan
	 * menjadi sumber warisan akun terakhir pada {@link #getAkun()}.
	 *
	 * @return jenis biaya lain, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya_lain", nullable = true)
	public JenisBiayaLain getJenisBiayaLain() {
		jenisBiayaLain = check(jenisBiayaLain);
		return jenisBiayaLain;
	}

	/**
	 * Menetapkan penggolong biaya lain-lain untuk baris ini.
	 *
	 * @param jenisBiayaLain jenis biaya lain, boleh {@code null}
	 */
	public void setJenisBiayaLain(JenisBiayaLain jenisBiayaLain) {
		this.jenisBiayaLain = jenisBiayaLain;
	}

	/**
	 * Mengembalikan tanggal domain baris biaya, menormalkan {@code null} menjadi waktu saat ini.
	 * <b>Getter destruktif</b>: normalisasi ditulis balik ke field, sehingga baris lama yang
	 * kolom tanggalnya {@code NULL} akan memperoleh tanggal <i>hari pembacaan</i> — bukan tanggal
	 * transaksinya — dan tanggal itu ikut tersimpan pada flush berikutnya. Karena
	 * {@link #getTgl()}, {@link #getBln()}, dan {@link #getThn()} seluruhnya diturunkan dari nilai
	 * ini, baris semacam itu akan terkelompok ke periode laporan yang keliru.
	 *
	 * @return tanggal baris biaya, tidak pernah {@code null}
	 */
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = new Date();
		}
		return tanggal;
	}

	/**
	 * Mengisi tanggal domain baris biaya.
	 *
	 * @param tanggal tanggal baris biaya
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan komponen hari dalam bulan dari {@link #getTanggal()}, dihitung memakai kalender
	 * dan zona waktu aplikasi ({@code ais.ui.util.WaktuUtil}). <b>Getter destruktif</b>: hasilnya
	 * ditulis balik ke field, sehingga nilai apa pun yang diisi lewat {@link #setTgl(Integer)}
	 * selalu ditimpa. Field ini merupakan denormalisasi untuk pengelompokan laporan per periode.
	 *
	 * @return hari dalam bulan (1&ndash;31)
	 */
	public Integer getTgl() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(getTanggal());
		tgl = calendar.get(Calendar.DATE);
		return tgl;
	}

	/**
	 * Mengisi komponen hari secara langsung. Nilai ini selalu ditimpa oleh {@link #getTgl()},
	 * sehingga setter ini praktis hanya berguna bagi kerangka kerja persistensi saat memuat entitas.
	 *
	 * @param tgl komponen hari
	 */
	public void setTgl(Integer tgl) {
		this.tgl = tgl;
	}

	/**
	 * Mengembalikan komponen bulan dari {@link #getTanggal()} dalam penomoran manusiawi
	 * (1 = Januari), yaitu {@code Calendar.MONTH + 1}. <b>Getter destruktif</b>, sama seperti
	 * {@link #getTgl()}.
	 *
	 * @return bulan (1&ndash;12)
	 */
	public Integer getBln() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(getTanggal());
		bln = calendar.get(Calendar.MONTH) + 1;
		return bln;
	}

	/**
	 * Mengisi komponen bulan secara langsung; selalu ditimpa oleh {@link #getBln()}.
	 *
	 * @param bln komponen bulan
	 */
	public void setBln(Integer bln) {
		this.bln = bln;
	}

	/**
	 * Mengembalikan komponen tahun dari {@link #getTanggal()}. <b>Getter destruktif</b>, sama
	 * seperti {@link #getTgl()}.
	 *
	 * @return tahun empat digit
	 */
	public Integer getThn() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(getTanggal());
		thn = calendar.get(Calendar.YEAR);
		return thn;
	}

	/**
	 * Mengisi komponen tahun secara langsung; selalu ditimpa oleh {@link #getThn()}.
	 *
	 * @param thn komponen tahun
	 */
	public void setThn(Integer thn) {
		this.thn = thn;
	}

	/**
	 * Mengembalikan porsi komponen biaya ini dalam persen terhadap {@link #getJumlahTotal()},
	 * menghitung ulang dari nilai rupiah bila induk tarifnya dikonfigurasi dalam mode
	 * pembagian-rupiah.
	 *
	 * <p>
	 * Metode ini adalah pasangan arah sebaliknya dari {@link #getJumlah()}. Pada <b>mode rupiah</b>
	 * — yaitu ketika induk tarif ({@link #getBiayaTindakanPerKelas()},
	 * {@link #getBiayaAlatMedisPerKelas()}, atau {@link #getHargaJualItem()}) memiliki
	 * {@code pembagianBiayaDalamPersen} bernilai <b>salah</b> — operator mengisi rupiah tiap
	 * komponen dan porsi persennya diturunkan di sini dengan rumus
	 * {@code jumlah * 100 / jumlahTotal}. Pada mode persen, cabang ini dilewati dan nilai
	 * {@code persen} yang tersimpan (yang memang diisi operator) dipakai apa adanya.
	 * </p>
	 *
	 * <p>
	 * Rumus pembagian di atas tidak dijaga terhadap pembagi nol. Bila {@link #getJumlahTotal()}
	 * bernilai {@code 0.0} — keadaan yang lazim untuk tarif yang baru dibuat dan belum diisi, karena
	 * {@code CommonTarifTindakan.getBiayaTindakanPerKelas} membuat baris tarif baru dengan biaya
	 * awal 0 — hasilnya menjadi {@code NaN} (bila {@code jumlah} juga nol) atau tak hingga. Kedua
	 * keadaan itu ditangkap oleh normalisasi di akhir metode, yang mengubah {@code null} dan
	 * {@code NaN} menjadi {@code 0.0}. Perlu dicatat bahwa {@code Double.isNaN} <b>tidak</b>
	 * menangkap nilai tak hingga: bila {@code jumlah} bukan nol sementara {@code jumlahTotal} nol,
	 * hasilnya {@code Infinity} dan nilai itu lolos normalisasi lalu tersimpan ke kolom. Baris tarif
	 * yang komponennya sudah terisi rupiah tetapi total tarifnya masih nol karena itu dapat
	 * menghasilkan porsi persen tak hingga yang merusak tampilan dan penjumlahan berikutnya.
	 * </p>
	 *
	 * <p>
	 * Seperti pasangannya, metode ini destruktif — hasil perhitungan ditulis balik ke field
	 * {@code persen} — dan terlibat dalam pemanggilan saling-silang dengan {@link #getJumlah()}.
	 * Penjaga kedua metode adalah negasi persis satu sama lain, dan hanya kenyataan itulah yang
	 * mencegah rekursi tak berhingga; lihat penjelasan lengkapnya pada {@link #getJumlah()}.
	 * </p>
	 *
	 * @return porsi komponen dalam persen, tidak pernah {@code null} maupun {@code NaN}
	 * @see #getJumlah() pasangan turunan arah sebaliknya
	 */
	public Double getPersen() {

		if (getBiayaTindakanPerKelas() != null && !getBiayaTindakanPerKelas().getPembagianBiayaDalamPersen()) {
			persen = (getJumlah() * 100.0 / getJumlahTotal());
		} else if (getBiayaAlatMedisPerKelas() != null && !getBiayaAlatMedisPerKelas().getPembagianBiayaDalamPersen()) {
			persen = (getJumlah() * 100.0 / getJumlahTotal());
		} else if (getHargaJualItem() != null && !getHargaJualItem().getPembagianBiayaDalamPersen()) {
			persen = (getJumlah() * 100.0 / getJumlahTotal());
		}

		if (persen == null || Double.isNaN(persen)) {
			persen = 0.0;
		}
		return persen;
	}

	/**
	 * Mengisi porsi komponen dalam persen. Pada baris bermode rupiah, nilai ini akan dihitung ulang
	 * dan ditimpa oleh {@link #getPersen()}; pada baris bermode persen, nilai inilah yang bertahan
	 * dan menjadi sumber turunan {@link #getJumlah()}.
	 *
	 * @param persen porsi komponen dalam persen
	 */
	public void setPersen(Double persen) {
		this.persen = persen;
	}

	/**
	 * Mengembalikan basis perhitungan porsi persen — nilai penuh yang dibagi-bagi ke seluruh
	 * komponen biaya.
	 *
	 * <p>
	 * Basis itu berbeda-beda menurut peran baris, dan metode ini memilihnya lewat rantai prioritas
	 * yang <b>mengutamakan konteks transaksi di atas konteks master</b>. Bila baris ini sudah
	 * berperan sebagai baris transaksi, basisnya adalah {@code amount} baris transaksi tersebut —
	 * {@link DetailTransaksiLayanan#getAmount()} lebih dahulu, lalu
	 * {@link DetailTransaksiPasien#getAmount()}. Hanya bila keduanya kosong (yakni baris ini masih
	 * berupa cetakan tarif), basisnya diambil dari induk tarifnya: {@link BiayaTindakanPerKelas#getBiaya()},
	 * lalu {@link BiayaAlatMedisPerKelas#getBiaya()}, lalu {@link HargaJualItem#getHargaJual()}.
	 * </p>
	 *
	 * <p>
	 * Pengutamaan itu bermakna penting dan mudah terlewat: begitu sebuah baris cetakan disalin
	 * menjadi baris transaksi, basis perhitungannya <b>berpindah</b> dari nilai tarif master ke
	 * nilai amount transaksi. Bila amount transaksi berbeda dari tarif master — misalnya karena
	 * harga boleh diubah saat transaksi
	 * ({@link BiayaTindakanPerKelas#getHargaBisaDirubahSaatTransaksi()}), atau karena tarif master
	 * sudah disunting setelah transaksi terjadi — maka {@link #getJumlah()} pada baris bermode
	 * persen akan menghasilkan angka yang berbeda dari saat baris itu pertama disimpan. Inilah
	 * mekanisme yang membuat nilai tersimpan pada baris bermode persen tidak stabil antar pembacaan.
	 * </p>
	 *
	 * <p>
	 * Berbeda dari {@link #getJumlah()} dan {@link #getPersen()} yang membaca relasi lewat getter,
	 * rantai pemeriksaan di sini membaca <b>field mentah</b> ({@code detailTransaksiLayanan},
	 * {@code biayaTindakanPerKelas}, dan seterusnya) tanpa melewati {@code check(...)}. Perbedaan
	 * gaya ini berarti relasi yang masih berupa proxy malas tetap terbaca sebagai non-{@code null}
	 * dan cabangnya tetap dipilih, sehingga pemanggilan {@code getAmount()}/{@code getBiaya()}
	 * berikutnyalah yang memicu materialisasi — dan berpotensi melempar
	 * {@code LazyInitializationException} bila sesi Hibernate sudah tertutup, tanpa penangkapan
	 * seperti yang ada di {@link #getAkun()}.
	 * </p>
	 *
	 * <p>
	 * Metode ini destruktif dan menormalkan {@code null} maupun {@code NaN} menjadi {@code 0.0}
	 * sebelum mengembalikan nilai.
	 * </p>
	 *
	 * @return basis perhitungan porsi persen, tidak pernah {@code null} maupun {@code NaN}
	 */
	public Double getJumlahTotal() {

		if (detailTransaksiLayanan != null) {
			jumlahTotal = detailTransaksiLayanan.getAmount();
		} else if (detailTransaksi != null) {
			jumlahTotal = detailTransaksi.getAmount();
		} else if (biayaTindakanPerKelas != null) {
			jumlahTotal = biayaTindakanPerKelas.getBiaya();
		} else if (biayaAlatMedisPerKelas != null) {
			jumlahTotal = biayaAlatMedisPerKelas.getBiaya();
		} else if (hargaJualItem != null) {
			jumlahTotal = hargaJualItem.getHargaJual();
		}

		if (jumlahTotal == null || Double.isNaN(jumlahTotal)) {
			jumlahTotal = 0.0;
		}
		return jumlahTotal;
	}

	/**
	 * Mengisi basis perhitungan porsi persen secara langsung. Nilai ini akan ditimpa oleh
	 * {@link #getJumlahTotal()} pada pembacaan berikutnya bila salah satu relasi induk/transaksi
	 * terisi, sehingga setter ini hanya efektif untuk baris yang sama sekali tidak memiliki induk.
	 *
	 * @param jumlahTotal basis perhitungan porsi persen
	 */
	public void setJumlahTotal(Double jumlahTotal) {
		this.jumlahTotal = jumlahTotal;
	}

}
