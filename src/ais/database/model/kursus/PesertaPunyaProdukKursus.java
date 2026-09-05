package ais.database.model.kursus;

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

import ais.common.BarcodeCommon;
import ais.database.model.GeneralValueObject;

/**
 * Header <b>transaksi pembelian produk kursus</b> oleh seorang peserta — satu-satunya entitas di
 * klaster peserta&ndash;produk yang benar-benar menjadi <b>gerbang pembayaran</b>: memegang
 * {@link #getStatus()} (siklus {@link #PESAN} → {@link #TERBELI}/{@link #BATAL}),
 * {@link #getHargaDibayar()}, {@link #getWaktuBeli()}, dan tautan opsional ke {@link KuponKursus}
 * yang dipakai.
 *
 * <p><b>Beda dengan dua entitas kursus lain yang mirip namanya.</b></p>
 * <ul>
 * <li>{@code PesertaPunyaProdukKursus} (kelas ini) — <b>header pembelian</b>, satu baris per
 * transaksi pemesanan/pembelian produk kursus oleh peserta.</li>
 * <li>{@link ProdukPeserta} — <b>detail 1:1</b> dari baris ini (FK {@code peserta_punya_produk_kursus}
 * bertanda {@code unique = true, nullable = false}), berisi turunan daftar komponen harga yang
 * dihitung ulang dari definisi {@link ProdukKursus} terkini, bukan snapshot pada saat pembelian.</li>
 * <li>{@link PesertaInginProdukKursus} — <b>minat/wishlist</b> sebelum pembelian, tanpa status
 * maupun nilai uang, dan tidak otomatis terhapus/tertaut setelah baris ini dibuat.</li>
 * </ul>
 *
 * <p><b>Status pembelian, tiga nilai baku ({@link #PESAN}/{@link #TERBELI}/{@link #BATAL}).</b>
 * {@link #getStatus()} memakai bawaan {@link #PESAN} bila kolom {@code status} masih
 * {@code null}/kosong — konsisten dengan alur wajar "pesan dulu, baru dikonfirmasi terbeli atau
 * dibatalkan". <b>Perlu diketahui:</b> tidak ada validasi transisi status pada level entity ini —
 * tidak ada yang mencegah status berpindah langsung dari {@link #TERBELI} kembali ke
 * {@link #PESAN}, atau dari {@link #BATAL} ke {@link #TERBELI}, lewat pemanggilan
 * {@link #setStatus(String)} apa pun; penjaga transisi (bila ada) berada di lapisan Action/service
 * pemanggil, bukan di entity ini.</p>
 *
 * <p><b>{@link #getHargaDibayar()} vs harga produk.</b> Nilai ini murni angka yang disetor
 * pemanggil (default {@code 0.0} bila belum diisi) — kelas ini tidak memvalidasi bahwa
 * {@code hargaDibayar} sesuai dengan harga {@link ProdukKursus} dikurangi potongan
 * {@link KuponKursus} yang ditautkan; kebenaran nilai sepenuhnya bergantung pada lapisan
 * Action/service yang menuliskannya.</p>
 *
 * <p><b>Pemetaan.</b> Skema {@code public}, tabel {@code peserta_punya_produk_kursus}, beranotasi
 * {@code @Audited} (Envers) dengan {@code dynamicInsert}/{@code dynamicUpdate}. Kolom {@code kode}
 * unik dibangkitkan otomatis via {@link BarcodeCommon#generateCode()} bila belum diisi.
 *
 * @see ProdukPeserta
 * @see PesertaInginProdukKursus
 * @see KuponKursus
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "peserta_punya_produk_kursus")
public class PesertaPunyaProdukKursus extends GeneralValueObject {

	/** Status baku: pemesanan baru belum dikonfirmasi, atau kolom {@code status} masih kosong (bawaan {@link #getStatus()}). */
	public final static String PESAN = "Pesan";
	/** Status baku: pembelian sudah dikonfirmasi/lunas. */
	public final static String TERBELI = "Terbeli";
	/** Status baku: pemesanan/pembelian dibatalkan. */
	public final static String BATAL = "Batal";
	/**
	 * Penanda versi serialisasi Java, bernilai sama dengan entitas lain sepaket karena kerangka
	 * kelasnya disalin dari sumber yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama sekuensial dari basis data. Lihat {@link #getId()}. */
	private Long id;
	/**
	 * Blok audit bayangan yang dipadatkan ke satu baris — pola penyisipan otomatis yang dipakai
	 * di seluruh basis kode AIS agar dapat ditempelkan ke entitas lama tanpa mengubah struktur
	 * berkas. Isinya: field {@code oleh} (nama tampil pengubah terakhir, lihat
	 * {@link #getOleh()}), field {@code olehId} beserta getter-nya (identitas pengubah terakhir),
	 * dan setter {@code setOlehId} yang berpenjaga satu arah — argumen {@code null} atau berisi
	 * spasi saja diabaikan sehingga jejak audit yang sudah terisi tidak dapat dikosongkan kembali
	 * lewat setter.
	 *
	 * <p>Pengulangan blok ini di hampir setiap entitas adalah keharusan teknis, bukan cacat:
	 * {@link ais.database.model.GeneralValueObject} merupakan POJO abstrak biasa dan bukan
	 * {@code @MappedSuperclass}, sehingga properti yang dideklarasikan di sana tidak ikut dipetakan
	 * Hibernate ke kolom tabel turunannya.
	 */
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna pengubah terakhir baris transaksi ini.
	 *
	 * @return identitas (id) pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas pengguna pengubah terakhir dengan penjaga satu arah: argumen
	 * {@code null} atau berisi spasi saja diabaikan.
	 *
	 * @param olehId identitas pengguna pengubah; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama tampil pengguna pengubah terakhir dengan penjaga satu arah yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama tampil pengguna pengubah; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang menyimpan baris transaksi ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA sebelum setiap {@code UPDATE}; mendelegasikan pencatatan stempel waktu
	 * dan identitas pengubah ke {@code AuditTimestampInterceptor.ubah(this)}. Deklarasi field
	 * {@code tanggal_dirubah} sengaja ditempelkan pada baris yang sama, mengikuti pola blok audit
	 * yang sama seperti pada baris {@code oleh}/{@code olehId} di atas.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir; biasanya sudah diurus {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; untuk objek baru berisi waktu objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengembalikan representasi teks baris transaksi ini, gabungan {@code kode} dan {@code nama}.
	 *
	 * @return string "{@code kode} - {@code nama}"
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	/** Kode unik baris transaksi, dibangkitkan otomatis bila belum diisi. Lihat {@link #getKode()}. */
	private String kode;
	/** Nama gabungan peserta+produk, dihitung ulang tiap akses. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Peserta pembeli. Lihat {@link #getPesertaKursus()}. */
	private PesertaKursus pesertaKursus;
	/** Produk kursus yang dibeli. Lihat {@link #getProdukKursus()}. */
	private ProdukKursus produkKursus;
	/** Waktu pembelian/pemesanan. Lihat {@link #getWaktuBeli()}. */
	private Date waktuBeli;

	/** Status transaksi: {@link #PESAN}/{@link #TERBELI}/{@link #BATAL}. Lihat {@link #getStatus()}. */
	private String status;
	/** Nominal yang sudah dibayarkan peserta untuk transaksi ini. Lihat {@link #getHargaDibayar()}. */
	private Double hargaDibayar;
	/** Kupon diskon yang dipakai pada transaksi ini, opsional. Lihat {@link #getKuponKursus()}. */
	private KuponKursus kuponKursus;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate. */
	public PesertaPunyaProdukKursus() {
	}

	/**
	 * Mengembalikan kunci utama baris transaksi. Kolomnya {@code insertable = false} karena
	 * nilainya dibangkitkan basis data.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama; dipakai Hibernate dan proses impor.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris transaksi ini, apa adanya.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris transaksi ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode unik baris transaksi ini, membangkitkan kode baru lewat
	 * {@link BarcodeCommon#generateCode()} pada akses pertama bila field masih {@code null} —
	 * getter ini punya efek samping menulis field {@code kode} (konsisten dengan
	 * {@code dynamicUpdate} entitas ini).
	 *
	 * @return kode unik baris, tidak pernah {@code null} setelah dipanggil sekali
	 */
	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	/**
	 * Menetapkan kode unik baris transaksi ini apa adanya.
	 *
	 * @param kode kode unik; boleh {@code null} untuk memicu pembangkitan otomatis pada
	 *             {@link #getKode()} berikutnya
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama gabungan "nama peserta + nama produk kursus", dihitung ulang setiap kali
	 * dipanggil selama kedua relasi ({@link #getPesertaKursus()}, {@link #getProdukKursus()})
	 * bukan {@code null} — nilai field {@code nama} yang tersimpan sebelumnya ditimpa tiap
	 * pemanggilan berhasil (getter dengan efek samping tulis-field, bukan sekadar accessor pasif).
	 * Bila salah satu relasi {@code null}, nilai lama field {@code nama} (bisa {@code null})
	 * dikembalikan apa adanya tanpa dihitung ulang.
	 *
	 * @return nama gabungan peserta+produk, atau nilai lama/{@code null} bila relasi belum lengkap
	 */
	public String getNama() {
		if (getProdukKursus() != null && getPesertaKursus() != null) {
			nama = pesertaKursus.getNama() + " " + produkKursus.getNama();
		}
		return nama;
	}

	/**
	 * Menetapkan nama gabungan secara manual; akan ditimpa oleh {@link #getNama()} pada
	 * pemanggilan berikutnya selama kedua relasi peserta dan produk sudah terisi.
	 *
	 * @param nama nama gabungan yang ingin ditetapkan sementara
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan peserta pembeli, meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan.
	 *
	 * <p><b>Tidak ada penyaringan kepemilikan/tenant pada level getter ini</b> — pemanggil yang
	 * memuat baris transaksi berdasarkan id mentah (mis. dari parameter URL) mendapat objek
	 * {@link PesertaKursus} apa adanya tanpa cek hak akses; penyaringan (bila ada) harus dilakukan
	 * oleh pemanggil di lapisan Action/service, sejalan dengan pola broken-access-control berulang
	 * yang sudah tercatat luas di basis kode ini pada entitas lain, dan berlaku pula untuk data
	 * pembayaran ({@link #getHargaDibayar()}, {@link #getStatus()}) yang dipegang baris ini.
	 *
	 * @return peserta pembeli, atau {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_kursus", nullable = true)
	public PesertaKursus getPesertaKursus() {
		pesertaKursus = check(pesertaKursus);
		return pesertaKursus;
	}

	/**
	 * Menetapkan peserta pembeli.
	 *
	 * @param pesertaKursus peserta pembeli
	 */
	public void setPesertaKursus(PesertaKursus pesertaKursus) {
		this.pesertaKursus = pesertaKursus;
	}

	/**
	 * Mengembalikan produk kursus yang dibeli, meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan.
	 *
	 * @return produk kursus yang dibeli, atau {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_kursus", nullable = true)
	public ProdukKursus getProdukKursus() {
		produkKursus = check(produkKursus);
		return produkKursus;
	}

	/**
	 * Menetapkan produk kursus yang dibeli.
	 *
	 * @param produkKursus produk kursus yang dibeli
	 */
	public void setProdukKursus(ProdukKursus produkKursus) {
		this.produkKursus = produkKursus;
	}

	/**
	 * Mengembalikan waktu pembelian/pemesanan, dengan nilai bawaan waktu saat ini
	 * ({@code new Date()}) bila field belum pernah diisi — nilai bawaan ini <b>tidak</b> ditulis
	 * balik ke field, sehingga setiap pemanggilan pada objek yang {@code waktuBeli}-nya masih
	 * {@code null} mengembalikan waktu saat itu juga, bukan waktu yang konsisten antar
	 * pemanggilan.
	 *
	 * @return waktu pembelian, atau waktu saat ini bila belum pernah diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuBeli() {
		return waktuBeli == null ? new Date() : waktuBeli;
	}

	/**
	 * Menetapkan waktu pembelian/pemesanan.
	 *
	 * @param waktuBeli waktu pembelian yang ingin ditetapkan
	 */
	public void setWaktuBeli(Date waktuBeli) {
		this.waktuBeli = waktuBeli;
	}

	/**
	 * Mengembalikan status transaksi, dengan bawaan {@link #PESAN} bila kolom {@code status}
	 * masih {@code null} atau string kosong — nilai bawaan ini <b>tidak</b> ditulis balik ke
	 * field. Lihat javadoc kelas untuk catatan bahwa tidak ada penjaga transisi status pada level
	 * entity ini.
	 *
	 * @return status transaksi ({@link #PESAN}/{@link #TERBELI}/{@link #BATAL} atau nilai bebas
	 *         lain yang pernah ditetapkan), tidak pernah {@code null}/kosong
	 */
	public String getStatus() {
		return status == null || status.isEmpty() ? PESAN : status;
	}

	/**
	 * Menetapkan status transaksi tanpa validasi nilai maupun transisi — pemanggil bertanggung
	 * jawab memakai salah satu konstanta {@link #PESAN}/{@link #TERBELI}/{@link #BATAL} agar
	 * konsisten dengan pemakaian di tempat lain.
	 *
	 * @param status status transaksi yang ingin ditetapkan
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan nominal yang sudah dibayarkan peserta untuk transaksi ini, dengan bawaan
	 * {@code 0.0} bila kolom masih {@code null} — nilai bawaan ini <b>tidak</b> ditulis balik ke
	 * field. Lihat javadoc kelas untuk catatan bahwa nilai ini tidak divalidasi terhadap harga
	 * produk maupun potongan kupon oleh entity ini.
	 *
	 * @return nominal dibayar, tidak pernah {@code null}
	 */
	public Double getHargaDibayar() {
		return hargaDibayar == null ? 0.0 : hargaDibayar;
	}

	/**
	 * Menetapkan nominal yang sudah dibayarkan peserta, tanpa validasi terhadap harga produk atau
	 * status transaksi.
	 *
	 * @param hargaDibayar nominal yang ingin ditetapkan
	 */
	public void setHargaDibayar(Double hargaDibayar) {
		this.hargaDibayar = hargaDibayar;
	}

	/**
	 * Mengembalikan kupon diskon yang dipakai pada transaksi ini, meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan.
	 *
	 * @return kupon yang dipakai, atau {@code null} bila transaksi ini tidak memakai kupon
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kupon_kursus", nullable = true)
	public KuponKursus getKuponKursus() {
		kuponKursus = check(kuponKursus);
		return kuponKursus;
	}

	/**
	 * Menetapkan kupon diskon yang dipakai pada transaksi ini.
	 *
	 * @param kuponKursus kupon yang ingin ditautkan, atau {@code null} untuk melepas kupon
	 */
	public void setKuponKursus(KuponKursus kuponKursus) {
		this.kuponKursus = kuponKursus;
	}

}
