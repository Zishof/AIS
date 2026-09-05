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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.BarcodeCommon;
import ais.database.model.GeneralValueObject;

/**
 * Entitas <b>minat/wishlist peserta</b> terhadap sebuah produk kursus — mencatat bahwa seorang
 * {@link PesertaKursus} pernah menandai {@link ProdukKursus} tertentu sebagai "ingin dibeli",
 * tanpa menyiratkan transaksi maupun pembayaran apa pun.
 *
 * <p><b>Beda dengan dua entitas kursus lain yang mirip namanya.</b> Modul peserta kursus punya
 * tiga entitas relasi peserta&ndash;produk yang mudah tertukar:</p>
 * <ul>
 * <li>{@code PesertaInginProdukKursus} (kelas ini) — <b>minat</b>: hanya {@code waktuIngin}, tanpa
 * status maupun nilai uang. Murni penanda ketertarikan, sepenuhnya reversibel (baris dapat
 * dihapus tanpa dampak transaksi).</li>
 * <li>{@link PesertaPunyaProdukKursus} — <b>header pembelian</b>: entitas yang benar-benar
 * menjadi gerbang pembayaran, punya {@code status} ({@code Pesan}/{@code Terbeli}/{@code Batal}),
 * {@code hargaDibayar}, {@code waktuBeli}, dan tautan {@link KuponKursus} opsional.</li>
 * <li>{@link ProdukPeserta} — <b>detail 1:1</b> dari sebuah {@link PesertaPunyaProdukKursus}
 * (kolom join-nya {@code unique = true, nullable = false}), berisi rincian komponen harga yang
 * dihitung ulang dari {@link ProdukKursus#getHargaKomponens()} setiap kali diakses.</li>
 * </ul>
 * <p>Dengan kata lain alur wajarnya: peserta menandai minat ({@code PesertaInginProdukKursus}) →
 * bertransaksi ({@code PesertaPunyaProdukKursus} dibuat dengan status {@code Pesan}, lalu
 * {@code Terbeli}/{@code Batal}) → rincian komponen harga tersimpan di {@code ProdukPeserta}
 * sebagai detail 1:1 dari baris pembelian tersebut. Namun tidak ada kode yang menghapus baris
 * {@code PesertaInginProdukKursus} setelah pembelian terjadi — baris minat lama tetap ada
 * berdampingan dengan baris pembelian, sehingga tabel ini bukan sumber kebenaran untuk "peserta
 * mana yang belum membeli" tanpa memeriksa silang {@link PesertaPunyaProdukKursus}.</p>
 * <p>Tidak ada penjaga keunikan pasangan peserta+produk kursus pada level entity/DB (berbeda dari
 * {@link ProdukPeserta} yang FK-nya {@code unique = true}) — peserta yang sama dapat menandai
 * minat pada produk yang sama berkali-kali, membentuk baris duplikat.</p>
 *
 * <p><b>Pemetaan.</b> Skema {@code public}, tabel {@code peserta_ingin_produk_kursus}, beranotasi
 * {@code @Audited} (Envers) dengan {@code dynamicInsert}/{@code dynamicUpdate}. Kolom {@code kode}
 * unik dibangkitkan otomatis via {@link BarcodeCommon#generateCode()} bila belum diisi.
 *
 * @see PesertaPunyaProdukKursus
 * @see ProdukPeserta
 * @see PesertaKursus
 * @see ProdukKursus
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "peserta_ingin_produk_kursus")
public class PesertaInginProdukKursus extends GeneralValueObject {

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
	 * Mengembalikan identitas pengguna pengubah terakhir baris minat ini.
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
	 * Mengembalikan nama tampil pengguna terakhir yang menyimpan baris minat ini.
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
	 * Mengembalikan representasi teks baris minat ini, gabungan {@code kode} dan {@code nama}.
	 *
	 * @return string "{@code kode} - {@code nama}"
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	/** Kode unik baris minat, dibangkitkan otomatis bila belum diisi. Lihat {@link #getKode()}. */
	private String kode;
	/** Nama gabungan peserta+produk, dihitung ulang tiap akses. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Peserta yang menandai minat. Lihat {@link #getPesertaKursus()}. */
	private PesertaKursus pesertaKursus;
	/** Produk kursus yang diminati. Lihat {@link #getProdukKursus()}. */
	private ProdukKursus produkKursus;
	/** Waktu minat dicatat. Lihat {@link #getWaktuIngin()}. */
	private Date waktuIngin;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate. */
	public PesertaInginProdukKursus() {
	}

	/**
	 * Mengembalikan kunci utama baris minat. Kolomnya {@code insertable = false} karena nilainya
	 * dibangkitkan basis data.
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
	 * Mengembalikan keterangan bebas baris minat ini, apa adanya.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris minat ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode unik baris minat ini, membangkitkan kode baru lewat
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
	 * Menetapkan kode unik baris minat ini apa adanya.
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
	 * Bila salah satu relasi {@code null} (mis. objek baru belum ditautkan), nilai lama field
	 * {@code nama} (bisa {@code null}) dikembalikan apa adanya tanpa dihitung ulang.
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
	 * Mengembalikan peserta kursus yang menandai minat ini. Berbeda dari kebanyakan relasi
	 * {@code @ManyToOne} lain di modul ini, relasi ini <b>bukan</b> {@code FetchType.LAZY}
	 * (memakai {@code @Fetch(FetchMode.SELECT)} bawaan/EAGER) dan getter-nya <b>tidak</b>
	 * memanggil {@code check(...)} untuk resolusi proxy lazy — konsisten karena memang tidak ada
	 * proxy lazy yang perlu diresolusi di sini.
	 *
	 * <p><b>Tidak ada penyaringan kepemilikan/tenant pada level getter ini</b> — pemanggil yang
	 * memuat baris berdasarkan id mentah (mis. dari parameter URL) mendapat objek
	 * {@link PesertaKursus} apa adanya tanpa cek hak akses; penyaringan (bila ada) harus
	 * dilakukan oleh pemanggil di lapisan Action/service, sejalan dengan pola broken-access-control
	 * berulang yang sudah tercatat luas di basis kode ini pada entitas lain.
	 *
	 * @return peserta kursus pemilik minat, atau {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peserta_kursus", nullable = true)
	public PesertaKursus getPesertaKursus() {
		return pesertaKursus;
	}

	/**
	 * Menetapkan peserta kursus yang menandai minat ini.
	 *
	 * @param pesertaKursus peserta kursus pemilik minat
	 */
	public void setPesertaKursus(PesertaKursus pesertaKursus) {
		this.pesertaKursus = pesertaKursus;
	}

	/**
	 * Mengembalikan produk kursus yang diminati, meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan.
	 *
	 * @return produk kursus yang diminati, atau {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_kursus", nullable = true)
	public ProdukKursus getProdukKursus() {
		produkKursus = check(produkKursus);
		return produkKursus;
	}

	/**
	 * Menetapkan produk kursus yang diminati.
	 *
	 * @param produkKursus produk kursus yang diminati
	 */
	public void setProdukKursus(ProdukKursus produkKursus) {
		this.produkKursus = produkKursus;
	}

	/**
	 * Mengembalikan waktu minat dicatat, dengan nilai bawaan waktu saat ini ({@code new Date()})
	 * bila field belum pernah diisi — nilai bawaan ini <b>tidak</b> ditulis balik ke field
	 * (berbeda dari pola getter destruktif pada entitas lain di modul ini), sehingga setiap
	 * pemanggilan pada objek yang {@code waktuIngin}-nya masih {@code null} mengembalikan waktu
	 * saat itu juga, bukan waktu yang konsisten antar pemanggilan.
	 *
	 * @return waktu minat, atau waktu saat ini bila belum pernah diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuIngin() {
		return waktuIngin == null ? new Date() : waktuIngin;
	}

	/**
	 * Menetapkan waktu minat dicatat.
	 *
	 * @param waktuIngin waktu minat yang ingin ditetapkan
	 */
	public void setWaktuIngin(Date waktuIngin) {
		this.waktuIngin = waktuIngin;
	}

}
