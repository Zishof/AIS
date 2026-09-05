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

/**
 * Entitas harga jual item pada schema {@code sirs} (tabel
 * {@code harga_jual_item}). Mencatat harga jual {@link #getHargaJual()}
 * untuk satu {@link ItemMedis} (relasi opsional, {@link #getItem()}) PER
 * {@link KelasPerawatan} (kelas rawat pasien — relasi WAJIB,
 * {@link #getKelasPerawatan()}) — inilah tabel yang dimaksud
 * {@link ItemMedis#getSemuahargasama()}: bila flag itu {@code false},
 * satu item medis mestinya punya beberapa baris {@code HargaJualItem},
 * satu per kelas perawatan yang berbeda harganya.
 *
 * <p>
 * TIDAK ADA HISTORI HARGA BERBASIS RENTANG TANGGAL: sama seperti
 * {@link HargaBeliItem}, sudah diverifikasi dari kode bahwa kelas ini
 * TIDAK punya field periode/tanggal berlaku. Setiap baris adalah harga
 * "saat ini" untuk kombinasi item+kelas perawatan (dan opsional tarif
 * khusus); riwayat harga dari waktu ke waktu hanya tersedia lewat tabel
 * audit Envers ({@code @Audited}), bukan lewat query rentang tanggal
 * terhadap {@code harga_jual_item}.
 * </p>
 *
 * <p>
 * Relasi opsional {@link #getTarifKhususPunyaItem()} memungkinkan baris
 * harga jual ini terikat ke satu skema tarif khusus tertentu (mis. tarif
 * khusus per asuransi/kerja sama), di luar harga jual "umum" per kelas
 * perawatan.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "harga_jual_item")
public class HargaJualItem extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas baris harga jual ini untuk tampilan/log.
	 *
	 * <p>
	 * Method ini memanggil {@link #getItem()} dan
	 * {@link #getKelasPerawatan()} terlebih dahulu (memicu lazy-load
	 * guard {@link #check(Object)} keduanya dan menulis-balik hasilnya
	 * ke field instance) sebelum menyusun string dari field mentah
	 * {@code item}/{@code kelasPerawatan}/{@code hargaJual} — pola yang
	 * sama dengan getter relasi lain di kelas ini.
	 * </p>
	 *
	 * @return string {@code item-kelasPerawatan-hargaJual}.
	 */
	public String toString() {
		getItem();
		getKelasPerawatan();
		return item + "-" + kelasPerawatan + "-" + hargaJual;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String keterangan;
	private ItemMedis item;
	private KelasPerawatan kelasPerawatan;
	private Double hargaJual;
	private TarifKhususPunyaItem tarifKhususPunyaItem;

	private Boolean pembagianBiayaDalamPersen = false;
	private Boolean hargaBisaDirubahSaatTransaksi = false;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public HargaJualItem() {
	}

	/**
	 * Primary key baris harga jual item, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik baris harga jual item ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris harga jual item.
	 *
	 * @param id ID baris harga jual item.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris harga jual ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris harga jual ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil item medis yang harga jualnya dicatat baris ini — relasi
	 * OPSIONAL ({@code nullable = true}), berbeda dari
	 * {@link HargaBeliItem#getItem()} yang wajib.
	 *
	 * @return item medis terkait, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan item medis yang harga jualnya dicatat baris ini.
	 *
	 * @param item item medis terkait.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil kelas perawatan yang menjadi konteks harga jual ini —
	 * relasi WAJIB ({@code nullable = false}). Inilah kelas rawat pasien
	 * (mis. VIP/Kelas 1/2/3), TIDAK terkait dengan {@link KelasItem}
	 * (dimensi klasifikasi katalog item medis) meski sama-sama disebut
	 * "kelas" — lihat javadoc {@link KelasItem} untuk penjelasan
	 * perbedaan ini.
	 *
	 * @return kelas perawatan terkait.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_perawatan", nullable = false)
	public KelasPerawatan getKelasPerawatan() {
		kelasPerawatan = check(kelasPerawatan);
		return kelasPerawatan;
	}

	/**
	 * Menetapkan kelas perawatan yang menjadi konteks harga jual ini.
	 *
	 * @param kelasPerawatan kelas perawatan terkait.
	 */
	public void setKelasPerawatan(KelasPerawatan kelasPerawatan) {
		this.kelasPerawatan = kelasPerawatan;
	}

	/**
	 * Mengambil harga jual yang dicatat baris ini. Flag SATU-ARAH:
	 * {@code null} ATAU {@link Double#isNaN(double)} otomatis dibaca
	 * dan DITULIS-BALIK ke field {@link #hargaJual} sebagai {@code 0.0}
	 * lewat lazy-init di getter — berbeda dari
	 * {@link HargaBeliItem#getHargaBeli()} yang mengembalikan
	 * {@code null} apa adanya tanpa fallback. Berarti baris harga jual
	 * lama yang belum pernah diisi (atau yang tersimpan sebagai NaN)
	 * akan otomatis dianggap {@code 0.0} begitu getter ini dipanggil
	 * sekali saja.
	 *
	 * @return harga jual; default {@code 0.0} bila belum pernah diset
	 *         atau bernilai NaN.
	 */
	@Column(name = "harga_jual", nullable = true)
	public Double getHargaJual() {
		if (hargaJual == null || Double.isNaN(hargaJual)) {
			hargaJual = 0.0;
		}
		return hargaJual;
	}

	/**
	 * Menetapkan harga jual yang dicatat baris ini.
	 *
	 * @param hargaJual nilai harga jual.
	 */
	public void setHargaJual(Double hargaJual) {
		this.hargaJual = hargaJual;
	}

	/**
	 * Mengambil skema tarif khusus (mis. per asuransi/kerja sama) yang
	 * terkait baris harga jual ini — relasi OPSIONAL.
	 *
	 * @return tarif khusus terkait, atau {@code null} jika harga jual
	 *         ini adalah harga umum (bukan tarif khusus).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tarif_khusus_punya_item", nullable = true)
	public TarifKhususPunyaItem getTarifKhususPunyaItem() {
		tarifKhususPunyaItem = check(tarifKhususPunyaItem);
		return tarifKhususPunyaItem;
	}

	/**
	 * Menetapkan skema tarif khusus yang terkait baris harga jual ini.
	 *
	 * @param tarifKhususPunyaItem tarif khusus terkait; {@code null}
	 *                             berarti harga jual umum.
	 */
	public void setTarifKhususPunyaItem(TarifKhususPunyaItem tarifKhususPunyaItem) {
		this.tarifKhususPunyaItem = tarifKhususPunyaItem;
	}

	/**
	 * Mengambil flag apakah pembagian biaya untuk harga jual ini
	 * dinyatakan dalam persen (bukan nominal tetap). {@code null}
	 * otomatis dibaca dan ditulis-balik sebagai {@code false}.
	 *
	 * @return {@code true} jika pembagian biaya dalam persen; default
	 *         {@code false} bila belum pernah diset.
	 */
	public Boolean getPembagianBiayaDalamPersen() {
		if (pembagianBiayaDalamPersen == null) {
			pembagianBiayaDalamPersen = false;
		}
		return pembagianBiayaDalamPersen;
	}

	/**
	 * Menetapkan flag pembagian biaya dalam persen untuk harga jual ini.
	 *
	 * @param pembagianBiayaDalamPersen {@code true} jika pembagian biaya
	 *                                  dinyatakan dalam persen.
	 */
	public void setPembagianBiayaDalamPersen(Boolean pembagianBiayaDalamPersen) {
		this.pembagianBiayaDalamPersen = pembagianBiayaDalamPersen;
	}

	/**
	 * Mengambil flag apakah harga jual ini boleh diubah manual saat
	 * transaksi berlangsung (mis. kasir bisa override harga). {@code null}
	 * otomatis dibaca dan ditulis-balik sebagai {@code false}.
	 *
	 * @return {@code true} jika harga boleh diubah saat transaksi;
	 *         default {@code false} bila belum pernah diset.
	 */
	public Boolean getHargaBisaDirubahSaatTransaksi() {
		if (hargaBisaDirubahSaatTransaksi == null) {
			hargaBisaDirubahSaatTransaksi = false;
		}
		return hargaBisaDirubahSaatTransaksi;
	}

	/**
	 * Menetapkan flag boleh-ubah-harga-saat-transaksi untuk harga jual
	 * ini.
	 *
	 * @param hargaBisaDirubahSaatTransaksi {@code true} jika harga boleh
	 *                                      diubah manual saat transaksi.
	 */
	public void setHargaBisaDirubahSaatTransaksi(Boolean hargaBisaDirubahSaatTransaksi) {
		this.hargaBisaDirubahSaatTransaksi = hargaBisaDirubahSaatTransaksi;
	}

}
