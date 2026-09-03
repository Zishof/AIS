package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.envers.Audited;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import ais.database.model.GeneralValueObject;

/**
 * Master kebijakan retur yang dapat dipilih pada setiap produk.
 *
 * <p>Baris baku bernama {@value #TANPA_KEBIJAKAN} dibuat oleh migrasi startup
 * dan dipakai untuk produk lama maupun produk baru yang belum memilih kebijakan
 * khusus.</p>
 *
 * <p><b>Verifikasi penegakan -- MURNI DESKRIPTIF/INFORMASIONAL, tidak ditegakkan oleh kode retur.</b>
 * Penelusuran {@code ReturPenjualan}, {@code ReturPembelian}, dan {@code ReturBarang} (paket ini,
 * sudah didokumentasikan batch 91) TIDAK menemukan satu pun referensi ke {@code KebijakanRetur}
 * atau {@code Produk.getKebijakanRetur()} -- ketiga alur retur tersebut TIDAK membaca kebijakan
 * retur produk untuk memutuskan boleh/tidaknya suatu retur diproses, tidak pula memvalidasi jangka
 * waktu atau syarat lain yang mungkin dimaksud oleh kebijakan tersebut. Satu-satunya konsumen field
 * ini yang benar-benar ada di backend adalah: (1) {@code ais.action.master.inventory.ProdukAction}
 * (memilih kebijakan pada form Produk), (2) {@code ais.action.servlet.PosApi} (mengekspos
 * {@code kebijakanReturId}/{@code kebijakanReturNama} sebuah produk ke aplikasi klien POS
 * Desktop/Android), dan (3) {@code ais.action.servlet.api.KebijakanReturApiHelper} (CRUD master
 * lewat API POS, dengan hak kelola dibatasi supervisor toko -- lihat javadoc {@link
 * ais.action.servlet.api.KebijakanReturApiHelper#simpan}). Artinya kebijakan ini hanya DITAMPILKAN
 * ke pengguna (mis. sebagai teks informatif di layar kasir/Android saat retur diajukan) -- penegakan
 * aturan retur yang sesungguhnya (jika ada, mis. batas hari retur) sepenuhnya bergantung pada
 * disiplin manual staf toko atau pada logika sisi klien yang tidak terlihat dari backend Java ini,
 * BUKAN dipaksakan oleh validasi server pada alur {@code ReturPenjualan}/{@code
 * ReturPembelian}/{@code ReturBarang}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "kebijakan_retur")
@Audited
public class KebijakanRetur extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	/**
	 * Nama baku baris kebijakan "tanpa kebijakan retur" yang dibuat otomatis oleh migrasi startup
	 * ({@code InitIndex.initKebijakanReturProduk}) bila belum ada -- dipakai sebagai fallback saat
	 * produk lama/baru belum memilih kebijakan khusus (lihat {@code
	 * KebijakanReturApiHelper#resolveAtauBawaan}). Baris dengan nama ini DILINDUNGI dari penghapusan
	 * ({@code KebijakanReturApiHelper.hapus} menolak bila nama cocok) dan namanya DIKUNCI (upaya
	 * mengganti nama baris ini lewat {@code simpan} otomatis dikembalikan ke nilai konstanta ini,
	 * begitu juga status aktifnya selalu dipaksa {@code true}).
	 */
	public static final String TANPA_KEBIJAKAN = "Tanpa Kebijakan Retur";

	/** Primary key baris kebijakan retur. Digenerasi database ({@code IDENTITY}, kolom {@code insertable = false}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Nama kebijakan retur -- lihat javadoc {@link #getNama()} untuk perilaku {@code trim}. */
	private String nama;
	/** Deskripsi/isi kebijakan retur dalam teks bebas, mis. syarat dan jangka waktu retur -- lihat javadoc kelas untuk catatan bahwa isi ini murni informasional. */
	private String keterangan;
	/** Penanda aktif/nonaktif baris kebijakan ini -- lihat javadoc {@link #getAktif()} untuk default. */
	private Boolean aktif;
	/** Userid/nama yang terakhir mengisi/mengubah baris kebijakan ini. */
	private String oleh;
	/** Id user terkait {@link #oleh}. */
	private String olehId;
	/**
	 * Stempel waktu terakhir baris kebijakan ini diubah -- field audit shadow diisi otomatis oleh
	 * {@link #onUpdate()} pada tiap {@code UPDATE}, terpisah dari riwayat versi penuh envers
	 * ({@code @Audited}). Inisialisasi default memakai waktu konstruksi objek Java, bukan waktu
	 * commit transaksi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Hook lifecycle Hibernate dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris
	 * kebijakan ini (dipicu anotasi {@link javax.persistence.PreUpdate}, BUKAN dipanggil manual).
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang
	 * menstempel ulang {@link #tanggal_dirubah}. Murni hook siklus hidup entity -- tidak
	 * memvalidasi/mengunci ulang {@link #nama} baris {@value #TANPA_KEBIJAKAN}; penguncian nama/status
	 * aktif baris baku itu dilakukan secara eksplisit di {@code KebijakanReturApiHelper.simpan},
	 * bukan di sini.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

	/**
	 * Primary key baris kebijakan ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}. Kolom dideklarasikan {@code insertable = false} --
	 * konsisten dengan penggunaan {@code IDENTITY} standar Hibernate.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long id) { this.id = id; }

	/**
	 * Nama kebijakan retur, di-{@code trim()} setiap kali dibaca (field mentah {@link #nama} sendiri
	 * TIDAK di-{@code trim} saat disimpan oleh {@link #setNama(String)}). Wajib diisi ({@code
	 * nullable = false}, panjang maksimum 255 karakter); ada {@code unique index} pada
	 * {@code lower(btrim(nama))} di level database (dibuat oleh {@code
	 * InitIndex.initKebijakanReturProduk}) yang mencegah duplikat nama tanpa memandang huruf
	 * besar/kecil atau spasi pembatas -- constraint ini TIDAK terlihat dari anotasi JPA di kelas ini
	 * (bukan {@code @Column(unique=true)}), murni dibuat manual lewat SQL startup.
	 * @return nama kebijakan yang sudah di-{@code trim}, tidak pernah {@code null} (default string kosong).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() { return nama == null ? "" : nama.trim(); }
	/** @param nama nama kebijakan retur; disimpan APA ADANYA (tanpa {@code trim}) -- pemangkasan spasi baru terjadi saat dibaca lewat {@link #getNama()}. */
	public void setNama(String nama) { this.nama = nama; }

	/**
	 * Deskripsi/isi kebijakan retur dalam teks bebas (mis. syarat, jangka waktu, atau pengecualian
	 * retur). Lihat javadoc kelas: isi field ini murni INFORMASIONAL -- tidak ada kode server yang
	 * mem-parsing atau menegakkannya secara otomatis pada alur {@code ReturPenjualan}/{@code
	 * ReturPembelian}/{@code ReturBarang}.
	 * @return keterangan/isi kebijakan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() { return keterangan; }
	/** @param keterangan deskripsi/isi kebijakan retur dalam teks bebas. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Penanda aktif/nonaktif baris kebijakan ini. {@code null} dinormalisasi menjadi {@code
	 * Boolean.TRUE} (default AKTIF). Baris {@value #TANPA_KEBIJAKAN} secara khusus SELALU dipaksa
	 * aktif oleh {@code KebijakanReturApiHelper.simpan} (nilai yang dikirim klien untuk baris ini
	 * diabaikan); baris kebijakan lain yang dinonaktifkan tidak lagi muncul di daftar pilihan combo
	 * pada form Produk maupun daftar API ({@code termasuk_nonaktif=false}, default), tapi produk yang
	 * SUDAH memakai kebijakan tersebut tetap menyimpan relasinya (tidak otomatis dialihkan ke {@value
	 * #TANPA_KEBIJAKAN}).
	 * @return {@code true} bila kebijakan ini aktif; default {@code true} bila belum diisi.
	 */
	@Column(name = "aktif")
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	/** @param aktif {@code false} untuk menonaktifkan baris kebijakan ini dari daftar pilihan tanpa menghapusnya. */
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	/**
	 * Userid/nama yang terakhir mengisi/mengubah baris kebijakan ini.
	 * @return userid/nama pengisi terakhir, atau {@code null} bila tidak diisi.
	 */
	public String getOleh() { return oleh; }
	/** @param oleh userid/nama yang mengisi/mengubah baris kebijakan ini. Berbeda dari pola guard di beberapa model lain klaster ini, setter ini menerima nilai {@code null}/kosong apa adanya tanpa diabaikan. */
	public void setOleh(String oleh) { this.oleh = oleh; }

	/**
	 * Id user terkait {@link #getOleh()}.
	 * @return id user pengisi terakhir, atau {@code null} bila tidak diisi.
	 */
	public String getOlehId() { return olehId; }
	/** @param olehId id user terkait {@link #getOleh()}. */
	public void setOlehId(String olehId) { this.olehId = olehId; }

	/**
	 * Stempel waktu terakhir baris kebijakan ini diubah, diisi otomatis oleh {@link #onUpdate()} pada
	 * tiap {@code UPDATE}.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/**
	 * @param tanggal_dirubah waktu terakhir diubah. Normalnya TIDAK diisi manual oleh kode aplikasi --
	 *                        dikelola otomatis oleh {@link #onUpdate()}; setter ini ada untuk kebutuhan
	 *                        Hibernate (property accessor) dan skenario migrasi/backfill data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	/**
	 * Representasi ringkas baris kebijakan ini untuk kebutuhan log/debug/tampilan combobox (dipakai
	 * langsung oleh Produk saat memilih kebijakan retur di layar Produk Kasir).
	 * @return nama kebijakan (via {@link #getNama()}, sudah di-{@code trim}), tidak pernah {@code null}.
	 */
	@Override
	public String toString() { return getNama(); }
}

