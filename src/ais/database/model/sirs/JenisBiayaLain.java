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
 * Master <b>jenis biaya lain-lain</b>: penggolong arus uang di modul SIRS yang berada
 * <b>di luar tarif medis</b> — pembayaran kasir (tunai, bukan tunai, klaim asuransi), simpanan dan
 * penarikan deposit pasien, setoran hasil penjualan pada penutupan shift, pembelian dan penerimaan
 * barang, serta pos lain-lain. Sama seperti {@link JenisBiaya} pada sisi tarif, entitas ini
 * memasangkan tiap pos dengan satu {@link Akun} buku besar, dan {@link Biaya} mewarisi akun itu
 * lewat {@link Biaya#getJenisBiayaLain()}.
 *
 * <h3>Peran {@link #getJenis()} sebagai kunci pemilih di layar</h3>
 * Perbedaannya dengan {@link JenisBiaya} terletak pada cara katalog ini dipakai. Setiap layar
 * transaksi keuangan tidak menampilkan seluruh isi katalog, melainkan hanya pos yang
 * {@link #getJenis()}-nya sama dengan salah satu konstanta di kelas ini. Contohnya:
 * <ul>
 * <li>{@code ais.action.master.sirs.PembayaranAction} menyaring dengan
 * {@link #PEMBAYARAN_KASIR_TUNAI}, {@link #PEMBAYARAN_KASIR_BUKAN_TUNAI},
 * {@link #PEMBAYARAN_KASIR_ASURANSI}, dan {@link #PEMBAYARAN_DEPOSIT} untuk mengisi pilihan cara
 * bayar;</li>
 * <li>{@code DepositAction} memakai {@link #SIMPAN_DEPOSIT} dan {@link #CARA_BAYAR_DEPOSIT};</li>
 * <li>{@code ShiftAction} memakai {@link #SETOR_TRANSAKSI_PENJUALAN} saat penutupan shift;</li>
 * <li>{@code PesananPembelianAction} dan {@code PenerimaanOrderAction} memakai {@link #PEMBELIAN}
 * dan {@link #PENERIMAAN}.</li>
 * </ul>
 * Konstanta di kelas ini karena itu bukan sekadar dokumentasi: masing-masing merupakan
 * <b>kontrak antara data master dan sebuah layar</b>. Bila katalog tidak memuat satu pun baris
 * dengan nilai {@code jenis} yang dicari sebuah layar, daftar pilihannya kosong dan transaksi
 * bersangkutan tidak dapat diselesaikan — kegagalan konfigurasi yang muncul sebagai kotak pilihan
 * kosong, bukan sebagai pesan kesalahan yang menjelaskan sebabnya.
 * <p>
 * Nilai {@code jenis} disimpan sebagai <b>string bebas</b> tanpa kendala basis data. Layar
 * {@code JenisBiayaLainAction} memang membatasi pilihannya lewat combobox berisi kesebelas
 * konstanta, tetapi penyaringnya memakai pencocokan string persis, sehingga nilai yang menyimpang —
 * dari impor, penyemaian awal, atau perubahan data langsung — membuat pos tersebut tidak pernah
 * muncul di layar mana pun.
 * </p>
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — hanya {@link #getAkun()} yang memanggil {@code check(...)} dan
 * menulis balik ke field; getter lain di kelas ini murni-baca, termasuk {@link #getJenis()} dan
 * {@link #getNama()} yang dapat mengembalikan {@code null}.</li>
 * <li><b>Field audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, dan {@link #onUpdate()}: keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — katalog bersifat global lintas unit, sehingga pos pembayaran dan
 * akun tujuannya tidak dapat dibedakan per satuan kerja.</li>
 * <li><b>Tanpa bendera aktif</b> — berbeda dari {@link JenisBiaya}, pos yang sudah tidak dipakai
 * tidak dapat disembunyikan dari daftar pilihan tanpa dihapus; penghapusannya sendiri tidak dijaga
 * terhadap baris {@link Biaya} yang masih merujuknya.</li>
 * <li>{@code jenis} tidak diberi {@code @Column}, sehingga dipetakan ke kolom bernama sesuai nama
 * properti. {@link #getKode()} dibangkitkan {@code Common.generateCode} dan tidak berindeks
 * unik.</li>
 * </ul>
 *
 * @see JenisBiaya penggolong komponen pembentuk tarif medis
 * @see Biaya baris biaya yang mewarisi akun dari pos ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "jenis_biaya_lain")
public class JenisBiayaLain extends GeneralValueObject {

	/** Nilai {@link #getJenis()} untuk pos penjualan barang/jasa. */
	public static final String PENJUALAN = "Penjualan";

	/** Nilai {@link #getJenis()} untuk pos penerimaan barang atas pesanan pembelian. */
	public static final String PENERIMAAN = "Penerimaan";

	/** Nilai {@link #getJenis()} untuk pos pembayaran kasir yang ditagihkan ke asuransi. */
	public static final String PEMBAYARAN_KASIR_ASURANSI = "Pembayaran Kasir Asuransi";

	/** Nilai {@link #getJenis()} untuk pos pembayaran kasir non-tunai (kartu, transfer). */
	public static final String PEMBAYARAN_KASIR_BUKAN_TUNAI = "Pembayaran Kasir Bukan Tunai";

	/** Nilai {@link #getJenis()} untuk pos pembayaran kasir secara tunai. */
	public static final String PEMBAYARAN_KASIR_TUNAI = "Pembayaran Kasir Tunai";

	/** Nilai {@link #getJenis()} untuk pos penyetoran deposit oleh pasien. */
	public static final String SIMPAN_DEPOSIT = "Simpan Deposit";

	/** Nilai {@link #getJenis()} untuk pos cara pembayaran yang dipakai saat menyetor deposit. */
	public static final String CARA_BAYAR_DEPOSIT = "Cara Bayar Deposit";

	/** Nilai {@link #getJenis()} untuk pos pemakaian deposit sebagai alat pembayaran tagihan. */
	public static final String PEMBAYARAN_DEPOSIT = "Pembayaran Deposit";

	/** Nilai {@link #getJenis()} untuk pos penyetoran hasil penjualan pada penutupan shift kasir. */
	public static final String SETOR_TRANSAKSI_PENJUALAN = "Setor hasil penjualan";

	/** Nilai {@link #getJenis()} untuk pos pembelian barang ke pemasok. */
	public static final String PEMBELIAN = "Pembelian";

	/** Nilai {@link #getJenis()} untuk pos serba-serbi yang tidak tercakup jenis lain. */
	public static final String LAIN = "Lain-lain";

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.jenis_biaya_lain}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah pos ini.
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
	 * Representasi teks pos untuk komponen ZK, memakai field {@link #nama} langsung. Inilah label
	 * yang muncul pada kotak pilihan cara bayar dan sejenisnya, sehingga nama yang tidak jelas akan
	 * langsung terasa oleh kasir.
	 *
	 * @return nama pos; dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
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
	 * Mengembalikan nama pengguna yang terakhir mengubah pos ini.
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
	 * Mengembalikan cap waktu perubahan terakhir pos ini. Karena entitas ini tidak memiliki masa
	 * berlaku maupun bendera aktif, nilai inilah satu-satunya penanda kapan pos terakhir disentuh —
	 * penting saat menelusuri perubahan akun tujuan yang memengaruhi posting.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode pos, wajib diisi; dibangkitkan {@code Common.generateCode} sepanjang 8 karakter. */
	private String kode;

	/** Nama pos (kolom bertipe {@code text}); dipakai sebagai label pada kotak pilihan. */
	private String nama;

	/** Penggolong pos; semestinya salah satu konstanta di kelas ini, menentukan layar mana yang memakainya. */
	private String jenis;

	/** Keterangan bebas atas pos. */
	private String keterangan;

	/** Akun buku besar tujuan posting; kolomnya {@code NOT NULL}. */
	private Akun akun;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public JenisBiayaLain() {
	}

	/**
	 * Mengembalikan kunci utama pos.
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
	 * Mengembalikan kode pos.
	 *
	 * @return kode pos (kolom wajib, maksimal 255 karakter)
	 */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Mengisi kode pos. Dibangkitkan {@code Common.generateCode} pada alur normal; tidak ada penjaga
	 * tabrakan kode di lapisan model maupun indeks unik pada kolomnya.
	 *
	 * @param kode kode pos
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan bebas pos.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas pos.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi nama pos. Nama inilah yang tampil pada kotak pilihan cara bayar dan sejenisnya.
	 *
	 * @param nama nama pos
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan nama pos.
	 *
	 * @return nama pos, atau {@code null} bila tidak diisi
	 */
	@Column(name = "nama", columnDefinition = "text", nullable = true)
	public String getNama() {
		return nama;
	}

	/**
	 * Mengembalikan akun buku besar tujuan posting pos ini. <b>Getter destruktif</b>
	 * ({@code check(...)}). Kolomnya {@code NOT NULL}.
	 *
	 * <p>
	 * Akun ini adalah sumber warisan keempat — dan terakhir — pada rantai {@link Biaya#getAkun()},
	 * dipakai bagi baris biaya yang bukan rincian tarif, bukan diskon, dan bukan pajak. Karena
	 * pewarisan itu ditulis balik ke baris {@link Biaya} dan hanya berlaku sekali, mengubah akun di
	 * sini <b>tidak</b> memindahkan posting yang sudah terbentuk; perubahan hanya berlaku bagi
	 * transaksi berikutnya. Sifat ini tepat untuk menjaga posting historis, tetapi berarti koreksi
	 * salah-akun atas transaksi yang sudah terjadi harus dilakukan lewat penyesuaian data atau
	 * jurnal koreksi, bukan dengan menyunting master ini.
	 * </p>
	 *
	 * @return akun buku besar tujuan posting, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = false)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun buku besar tujuan posting pos ini.
	 *
	 * @param akun akun buku besar
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan penggolong pos. Getter murni-baca tanpa normalisasi, sehingga dapat
	 * mengembalikan {@code null}.
	 *
	 * <p>
	 * Nilai ini menentukan <b>layar mana</b> yang akan menampilkan pos bersangkutan: setiap layar
	 * transaksi keuangan menyaring katalog dengan {@code Restrictions.eq("jenis", <konstanta>)},
	 * sehingga pos hanya muncul pada layar yang konstantanya cocok persis. Nilai yang menyimpang
	 * dari kesebelas konstanta di kelas ini — termasuk {@code null}, spasi berlebih, atau beda huruf
	 * besar-kecil — membuat pos tidak pernah muncul di mana pun, dan bila akibatnya sebuah jenis
	 * tidak memiliki pos sama sekali, kotak pilihan pada layar terkait menjadi kosong sehingga
	 * transaksinya tidak dapat diselesaikan. Kegagalan itu tampil sebagai daftar kosong, bukan
	 * sebagai pesan yang menjelaskan sebabnya, sehingga patut diperiksa lebih dahulu ketika sebuah
	 * layar pembayaran atau deposit tiba-tiba tidak menawarkan pilihan apa pun.
	 * </p>
	 *
	 * @return penggolong pos, dapat {@code null}
	 */
	public String getJenis() {
		return jenis;
	}

	/**
	 * Mengisi penggolong pos. Pakailah salah satu konstanta di kelas ini; nilai lain akan tersimpan
	 * tanpa penolakan tetapi membuat pos tidak pernah muncul di layar mana pun.
	 *
	 * @param jenis salah satu konstanta {@code JenisBiayaLain}, mis. {@link #PEMBAYARAN_KASIR_TUNAI}
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

}
