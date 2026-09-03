package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Pemasok/supplier RINGAN (hanya nama+keterangan) khusus katalog Produk POS -- SENGAJA terpisah
 * dari {@link ais.database.model.asset.PenyediaAsset} (entitas vendor pengadaan aset yang berat,
 * dengan alur persetujuan/NPWP/rekening) karena kebutuhan di sini murni "nama pemasok utama" yang
 * dibawa apa adanya dari impor Excel katalog barang (fitur "Unggah/Unduh Excel" di layar Produk
 * Kasir Desktop/Android) -- lihat JavaDoc {@code KantinHelper.produkImporExcel}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pemasok_produk")
public class PemasokProduk extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	/** Primary key baris pemasok. Digenerasi database ({@code IDENTITY}, kolom {@code insertable = false}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Userid/nama yang terakhir MENGISI baris pemasok ini via {@link #setOleh(String)} -- lihat javadoc method tersebut untuk perilaku guard terhadap nilai kosong. */
	private String oleh;
	/** Id user yang terakhir mengisi baris pemasok ini via {@link #setOlehId(String)} -- pelengkap {@link #oleh} untuk pencarian presisi berbasis id. */
	private String olehId;

	/**
	 * Id user yang terakhir mengisi baris pemasok ini. Lihat javadoc {@link #setOlehId(String)} untuk
	 * perilaku setter yang mengabaikan nilai kosong.
	 * @return id user pengisi terakhir, atau {@code null} bila belum pernah diisi dengan nilai valid.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id user yang mengisi baris pemasok ini -- BUKAN setter pasif biasa. Nilai {@code null}
	 * atau string kosong/berisi-spasi-saja DIABAIKAN secara diam-diam (method langsung {@code return}
	 * tanpa mengubah field, tanpa melempar exception, tanpa log) -- pola guard yang sama dipakai di
	 * banyak model klaster ini (lihat mis. {@code AmbangStokGudang.setOlehId}). Efek praktisnya: sekali
	 * field ini terisi nilai valid, memanggil setter ini dengan nilai kosong TIDAK PERNAH bisa
	 * mengosongkannya lagi.
	 * @param olehId id user pengisi; nilai kosong/hanya-spasi diabaikan secara diam-diam.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi userid/nama yang mengisi baris pemasok ini. Perilaku guard SAMA seperti {@link
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
	 * Userid/nama yang terakhir mengisi baris pemasok ini. Lihat javadoc {@link #setOleh(String)} untuk
	 * perilaku setter yang mengabaikan nilai kosong.
	 * @return userid/nama pengisi terakhir, atau {@code null} bila belum pernah diisi dengan nilai valid.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook lifecycle Hibernate dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris pemasok
	 * ini (dipicu anotasi {@link javax.persistence.PreUpdate}, BUKAN dipanggil manual). Mendelegasikan
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menstempel ulang
	 * {@link #tanggal_dirubah}. Method ini murni hook siklus hidup entity -- tidak melakukan validasi
	 * apa pun terhadap {@link #nama}/{@link #keterangan}; validasi semacam itu, bila ada, berada di
	 * lapisan pemanggil ({@code KantinHelper.resolvePemasokProduk}, dsb).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Stempel waktu terakhir baris pemasok ini diubah -- field audit shadow diisi otomatis oleh
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
	 * Stempel waktu terakhir baris pemasok ini diubah, diisi otomatis oleh {@link #onUpdate()} pada
	 * tiap {@code UPDATE}.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas baris pemasok ini untuk kebutuhan log/debug/tampilan combobox (dipakai
	 * langsung oleh Produk saat memilih pemasok di layar Produk Kasir).
	 * @return nilai {@link #nama} (tanpa {@code trim}, membaca field mentah, bukan {@link #getNama()}),
	 *         atau string kosong bila {@link #nama} {@code null}.
	 */
	public String toString() {
		return nama == null ? "" : nama;
	}

	/**
	 * Nama pemasok utama -- SATU-SATUNYA identitas pemasok yang benar-benar dipakai lintas kode
	 * (dibandingkan tanpa {@code case} lewat {@code toUpperCase()} saat impor Excel mencocokkan baris
	 * pemasok yang sudah ada, lihat {@code KantinHelper.resolvePemasokProduk}). Wajib diisi
	 * ({@code nullable = false}); tidak ada {@code unique constraint} eksplisit di level entity, jadi
	 * duplikasi nama (mis. beda kapitalisasi atau spasi berlebih yang lolos {@code trim()}) tetap
	 * mungkin lolos bila jalur penyimpanan lain (di luar {@code resolvePemasokProduk}) dipakai.
	 */
	private String nama;
	/** Catatan bebas teks tentang pemasok ini, opsional. */
	private String keterangan;
	/** Penanda aktif/nonaktif baris pemasok ini -- lihat javadoc {@link #getAktif()} untuk default. */
	private Boolean aktif;

	/** Constructor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via refleksi saat memuat baris dari database; kode aplikasi yang membuat pemasok baru (mis. {@code KantinHelper.resolvePemasokProduk}) juga memakainya lalu mengisi field lewat setter. */
	public PemasokProduk() {
	}

	/**
	 * Primary key baris pemasok ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}. Kolom dideklarasikan {@code insertable = false} --
	 * konsisten dengan penggunaan {@code IDENTITY} standar Hibernate: nilai kolom diserahkan sepenuhnya
	 * ke database saat insert.
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
	 * Nama pemasok utama, di-{@code trim()} setiap kali dibaca (field mentah {@link #nama} sendiri
	 * TIDAK di-{@code trim} saat disimpan oleh {@link #setNama(String)}) -- pola normalisasi
	 * baca-saja yang umum dipakai di model-model klaster ini. Wajib diisi ({@code nullable = false},
	 * panjang maksimum 255 karakter).
	 * @return nama pemasok yang sudah di-{@code trim}, atau {@code null} bila field mentah {@code null}.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama pemasok utama; disimpan APA ADANYA (tanpa {@code trim}) -- pemangkasan spasi baru terjadi saat dibaca lewat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas teks tentang pemasok ini.
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan bebas teks tentang pemasok ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Penanda aktif/nonaktif baris pemasok ini. {@code null} dinormalisasi menjadi {@code true}
	 * (default AKTIF) -- baris baru yang belum pernah men-set field ini eksplisit dianggap aktif
	 * secara implisit. Tidak ada anotasi {@code @Column} eksplisit pada getter ini (berbeda dari
	 * kebanyakan field lain di kelas ini) -- nama kolom di-fold otomatis oleh Hibernate mengikuti
	 * aturan proyek (huruf kecil tanpa underscore).
	 * @return {@code true} bila pemasok aktif; default {@code true} bila belum diisi.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code false} untuk menonaktifkan baris pemasok ini tanpa menghapusnya. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
