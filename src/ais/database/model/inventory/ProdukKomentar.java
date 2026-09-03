package ais.database.model.inventory;

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

import ais.database.model.GeneralValueObject;

/**
 * Satu baris komentar/ulasan pelanggan pada sebuah {@link Produk}.
 *
 * <p><b>Catatan penamaan field yang menyesatkan -- verifikasi dari kode pemakai, bukan dari nama.</b>
 * Meskipun namanya {@code nama}, field ini sesungguhnya menyimpan ISI KOMENTAR/ULASAN itu sendiri
 * (dipakai sebagai teks utama di {@link #toString()} dan ditampilkan di kolom "Komentar" pada grid
 * {@code ProdukKomentarHelper}), BUKAN nama pengomentar. Sebaliknya, {@link #getAlamat()} tidak
 * dirender sama sekali oleh {@code ProdukKomentarHelper} (hanya {@link #getNama()}, {@link
 * #getKontak()}, {@link #getEmail()} yang ditampilkan, masing-masing di kolom berlabel "Komentar",
 * "Oleh", "Email") -- sehingga kolom UI berlabel "Oleh" sesungguhnya menampilkan field {@link
 * #kontak}, bukan {@link #alamat} ataupun identitas pengomentar yang eksplisit. Dokumentasikan sesuai
 * perilaku kode aktual di atas, bukan asumsi dari nama field.</p>
 *
 * <p>Tidak ada mekanisme moderasi/persetujuan di level model ini -- setiap baris yang tersimpan
 * langsung tampil di grid komentar produk (hanya bisa dihapus, oleh user berhak {@code DELETE}, lewat
 * {@code ProdukKomentarHelper}); tidak ada flag {@code disetujui}/{@code aktif} maupun filter tenant
 * (toko) pada entity ini -- query pemuatannya ({@code ProdukKomentarHelper.loadDataDetail}) hanya
 * memfilter berdasarkan {@link #getProduk()}, mewarisi cakupan toko secara implisit dari produk itu
 * sendiri.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "produk_komentar")

public class ProdukKomentar extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris komentar. Digenerasi database ({@code IDENTITY}, kolom {@code insertable = false}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Userid/nama yang mengisi baris komentar ini secara internal (mis. staf yang mengentrikan komentar pelanggan atas nama pelanggan) -- lihat javadoc {@link #getOleh()}. BUKAN nama pengomentar; lihat javadoc kelas untuk field yang benar-benar menampung isi komentar. */
	private String oleh;
	/** Id user terkait {@link #oleh} -- lihat javadoc {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id user yang terkait {@link #getOleh()}. Lihat javadoc {@link #setOlehId(String)} untuk
	 * perilaku setter yang mengabaikan nilai kosong.
	 * @return id user pengisi terakhir, atau {@code null} bila belum pernah diisi dengan nilai valid.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id user terkait baris komentar ini -- BUKAN setter pasif biasa. Nilai {@code null}
	 * atau string kosong/berisi-spasi-saja DIABAIKAN secara diam-diam (method langsung {@code return}
	 * tanpa mengubah field, tanpa melempar exception, tanpa log) -- pola guard yang sama dipakai di
	 * banyak model klaster ini. Efek praktisnya: sekali field ini terisi nilai valid, memanggil setter
	 * ini dengan nilai kosong TIDAK PERNAH bisa mengosongkannya lagi.
	 * @param olehId id user pengisi; nilai kosong/hanya-spasi diabaikan secara diam-diam.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi userid/nama yang mengisi baris komentar ini. Perilaku guard SAMA seperti {@link
	 * #setOlehId(String)}: nilai {@code null} atau kosong/hanya-spasi diabaikan diam-diam, field tidak
	 * pernah dikosongkan kembali lewat setter ini setelah pernah terisi nilai valid.
	 * @param oleh userid/nama pengisi; nilai kosong/hanya-spasi diabaikan secara diam-diam.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Userid/nama yang mengisi baris komentar ini. Lihat javadoc {@link #setOleh(String)} untuk
	 * perilaku setter yang mengabaikan nilai kosong.
	 * @return userid/nama pengisi terakhir, atau {@code null} bila belum pernah diisi dengan nilai valid.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook lifecycle Hibernate dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris komentar
	 * ini (dipicu anotasi {@link javax.persistence.PreUpdate}, BUKAN dipanggil manual). Mendelegasikan
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menstempel ulang
	 * {@link #tanggal_dirubah}. Murni hook siklus hidup entity -- tidak melakukan moderasi konten
	 * komentar apa pun.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu terakhir baris komentar ini diubah -- field audit shadow diisi otomatis oleh
	 * {@link #onUpdate()} pada tiap {@code UPDATE}, terpisah dari riwayat versi penuh envers
	 * ({@code @Audited}). Inisialisasi default memakai waktu konstruksi objek Java, bukan waktu commit
	 * transaksi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu terakhir diubah. Normalnya TIDAK diisi manual oleh kode aplikasi --
	 *                        dikelola otomatis oleh {@link #onUpdate()}; setter ini ada untuk kebutuhan
	 *                        Hibernate (property accessor) dan skenario migrasi/backfill data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu terakhir baris komentar ini diubah, diisi otomatis oleh {@link #onUpdate()} pada
	 * tiap {@code UPDATE}.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas baris komentar ini untuk kebutuhan log/debug.
	 * @return nilai {@link #nama} (isi komentar, lihat javadoc kelas untuk catatan penamaan yang
	 *         menyesatkan) apa adanya, TANPA null-guard -- bisa mengembalikan {@code null} secara
	 *         langsung bila {@link #nama} belum diisi (berbeda dari kebanyakan {@code toString()} lain
	 *         di klaster ini yang menjaga agar tidak pernah mengembalikan {@code null}).
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Isi komentar/ulasan pelanggan itu sendiri -- lihat javadoc kelas untuk penjelasan bahwa nama
	 * field ini MENYESATKAN (bukan nama pengomentar). Wajib diisi ({@code nullable = false}),
	 * kolom bertipe {@code text} (tanpa batas panjang praktis).
	 */
	private String nama;
	/** Alamat pengomentar, opsional. Lihat javadoc kelas: field ini TIDAK dirender oleh {@code ProdukKomentarHelper} (tidak ada kolom "Alamat" pada grid komentar produk saat ini). */
	private String alamat;
	/** Kontak (mis. nomor telepon) pengomentar, opsional. Ditampilkan pada kolom berlabel "Oleh" di grid {@code ProdukKomentarHelper} -- lihat javadoc kelas untuk catatan bahwa label kolom ini tidak sepenuhnya mencerminkan makna field. */
	private String kontak;
	/** Alamat email pengomentar, opsional. Ditampilkan pada kolom "Email" di grid {@code ProdukKomentarHelper}. */
	private String email;
	/** Produk yang dikomentari -- lihat javadoc {@link #getProduk()}. */
	private Produk produk;

	/** Constructor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via refleksi saat memuat baris dari database; kode aplikasi yang mencatat komentar baru juga memakainya lalu mengisi field lewat setter. */
	public ProdukKomentar() {
	}

	/**
	 * Primary key baris komentar ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}. Kolom dideklarasikan {@code insertable = false} --
	 * konsisten dengan penggunaan {@code IDENTITY} standar Hibernate.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Isi komentar/ulasan pelanggan, di-{@code trim()} setiap kali dibaca (field mentah {@link #nama}
	 * sendiri TIDAK di-{@code trim} saat disimpan oleh {@link #setNama(String)}). Wajib diisi
	 * ({@code nullable = false}), kolom bertipe {@code text}. Lihat javadoc kelas untuk penjelasan
	 * bahwa nama getter ini ({@code getNama}) MENYESATKAN -- yang dikembalikan adalah isi komentar,
	 * bukan nama pengomentar.
	 * @return isi komentar yang sudah di-{@code trim}, atau {@code null} bila field mentah {@code null}.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama isi komentar/ulasan pelanggan; disimpan APA ADANYA (tanpa {@code trim}) -- pemangkasan spasi baru terjadi saat dibaca lewat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Alamat pengomentar. Lihat javadoc kelas: field ini tersimpan tapi TIDAK dirender oleh
	 * {@code ProdukKomentarHelper} saat ini.
	 * @return alamat pengomentar, atau {@code null} bila tidak diisi.
	 */
	public String getAlamat() {
		return alamat;
	}

	/** @param alamat alamat pengomentar. */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Kontak (mis. nomor telepon) pengomentar. Ditampilkan di kolom berlabel "Oleh" pada grid
	 * {@code ProdukKomentarHelper} -- lihat javadoc kelas untuk catatan ketidaksesuaian label ini.
	 * @return kontak pengomentar, atau {@code null} bila tidak diisi.
	 */
	public String getKontak() {
		return kontak;
	}

	/** @param kontak kontak (mis. nomor telepon) pengomentar. */
	public void setKontak(String kontak) {
		this.kontak = kontak;
	}

	/**
	 * Alamat email pengomentar.
	 * @return email pengomentar, atau {@code null} bila tidak diisi.
	 */
	public String getEmail() {
		return email;
	}

	/** @param email alamat email pengomentar. */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Produk yang dikomentari oleh baris ini. Relasi {@code LAZY}, opsional ({@code nullable = true});
	 * getter memanggil {@code check(produk)} milik {@link GeneralValueObject} yang menormalisasi
	 * proxy/nilai kosong sebelum dikembalikan. Dipakai sebagai satu-satunya filter query saat memuat
	 * daftar komentar per produk ({@code ProdukKomentarHelper.loadDataDetail}, {@code
	 * Restrictions.eq("produk", produk)}).
	 * @return produk yang dikomentari (bisa proxy lazy, dinormalisasi via {@code check()}), atau
	 *         {@code null} bila tidak terkait produk mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = true)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/** @param produk produk yang dikomentari oleh baris ini. */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

}
