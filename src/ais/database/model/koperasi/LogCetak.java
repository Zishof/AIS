package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import ais.database.model.GeneralValueObject;

/**
 * Register riwayat cetak/ekspor (P10) -- syarat lintas-layar matriks paritas: setiap
 * cetak/ekspor tercatat pengguna, waktu, perangkat, jenis dokumen, referensi, dan parameter.
 * Append-only (tidak pernah diubah/dihapus); reprint terlihat sebagai baris baru per jenis+
 * referensi yang sama. TIDAK di-@Audited -- tabel ini sendiri sudah merupakan log.
 *
 * <p><b>Ditulis dan dibaca dari {@code SalesInventoryReversalHelper}</b> (dua aksi:
 * {@code printLogCreate} menyimpan satu baris per kejadian cetak, {@code printLogList}
 * menampilkannya kembali). Berbeda dari pola kebocoran lintas-pengguna yang sudah ditemukan di
 * {@code LogLogin}/{@code UploadLog} (dimuat tanpa gerbang otorisasi apa pun, siapa saja yang
 * terautentikasi bisa membaca log milik pengguna lain): pembacaan riwayat di sini SUDAH dijaga
 * gerbang peran -- {@code printLogList} menolak pemanggil yang bukan Pemilik/Admin
 * ({@code pemilikAtauAdmin(ctx)}) sebelum menjalankan query sama sekali. Ini memang wajar untuk
 * log cetak: baris di sini adalah riwayat operasional TOKO (siapa mencetak struk/laporan apa),
 * bukan data pribadi seorang pengguna, sehingga visibilitasnya utk Pemilik/Admin toko yg sama
 * bukan kebocoran. Query {@code printLogList} sendiri tetap TIDAK menyaring per {@link #getUserId()}
 * ataupun per toko/lokasi (mengandalkan skema per-tenant + gerbang peran di atasnya) -- konsisten
 * dgn baris query lain di domain ini yg tidak punya penyaring tenant bawaan pada level entity.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "log_cetak")
public class LogCetak extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String jenisDokumen;
	private String referensi;
	private String parameterJson;
	private String userId;
	private String perangkat;
	private Date waktu;

	/**
	 * Kait daur hidup JPA sebelum {@code UPDATE} -- mendelegasikan pengisian
	 * {@link #getTanggal_dirubah()} ke {@code AuditTimestampInterceptor.ubah(Object)}. Dalam praktiknya
	 * baris {@link LogCetak} bersifat append-only (tidak pernah di-{@code UPDATE} lagi setelah
	 * disimpan, lihat JavaDoc kelas), jadi kait ini nyaris tak pernah terpicu -- dipertahankan hanya
	 * demi konsistensi dengan pola field bayangan audit {@code tanggal_dirubah} di seluruh entity
	 * proyek ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib JPA/Hibernate. */
	public LogCetak() {
	}

	/** Kunci utama (identity/auto-increment). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** mis. kwitansi_penerimaan, laporan_sesi, laba_rugi, rekap_penjualan, voucher_hutang. */
	@Column(name = "jenis_dokumen", length = 60, nullable = false)
	public String getJenisDokumen() {
		return jenisDokumen;
	}

	public void setJenisDokumen(String jenisDokumen) {
		this.jenisDokumen = jenisDokumen;
	}

	/** Nomor/id dokumen yang dicetak (nomor kwitansi, id sesi, rentang periode, dst). */
	@Column(name = "referensi", length = 160)
	public String getReferensi() {
		return referensi;
	}

	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}

	/** Salinan mentah parameter permintaan cetak/ekspor sebagai teks JSON (mis. filter tanggal, opsi laporan) -- untuk telusur ulang "kenapa dokumen ini terlihat begini". */
	@Column(name = "parameter_json", columnDefinition = "text")
	public String getParameterJson() {
		return parameterJson;
	}

	public void setParameterJson(String parameterJson) {
		this.parameterJson = parameterJson;
	}

	/**
	 * {@link ais.database.model.Tbmuser#getUserId()} pengguna yang melakukan cetak/ekspor ini.
	 *
	 * <p>Bukan field bayangan audit generik seperti {@code oleh}/{@code olehId} di entity lain --
	 * inilah SUBJEK utama baris log ini (mirip {@code userId} di {@code LogLogin}), diisi eksplisit
	 * oleh pemanggil (lihat {@code SalesInventoryReversalHelper#printLogCreate}) saat baris dibuat,
	 * bukan lewat interceptor audit.</p>
	 */
	@Column(name = "user_id", length = 80)
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	/** Identitas perangkat/mesin asal permintaan cetak (opsional, teks bebas dari klien). */
	@Column(name = "perangkat", length = 120)
	public String getPerangkat() {
		return perangkat;
	}

	public void setPerangkat(String perangkat) {
		this.perangkat = perangkat;
	}

	/**
	 * Waktu kejadian cetak/ekspor tercatat.
	 *
	 * <p><b>Fallback lunak:</b> bila kolom {@code waktu} kosong (baris lama yang belum pernah mengisi
	 * nilainya, atau ditulis lewat jalur yang lupa mengisi), getter ini mengembalikan waktu SEKARANG
	 * ({@code WaktuUtil.getDate()}) alih-alih {@code null} -- nilai fallback itu TIDAK ditulis balik ke
	 * field/kolom, jadi pemanggilan berikutnya menghitung ulang waktu-sekarang yang baru lagi
	 * (berbeda dari getter destruktif yang menimpa field-nya sendiri).</p>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/** Field bayangan audit "terakhir diubah" -- lihat {@link #onUpdate()}. Karena baris ini append-only, nilainya pada praktiknya tetap sama dengan waktu penyimpanan awal. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
