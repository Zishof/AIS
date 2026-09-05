package ais.database.model.sirs;

// Generated Apr 12, 2010 1:48:52 AM by Hibernate Tools 3.2.4.CR1

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
 * Entitas satuan (unit) item pada schema {@code sirs} (tabel
 * {@code satuan_item}). Baris entitas ini adalah nilai yang dirujuk oleh
 * relasi WAJIB {@link ItemMedis#getSatuanItem()} sebagai satuan dasar
 * item medis (mis. Tablet, Botol, Ampul, Box).
 *
 * <p>
 * DITEMUKAN DARI KODE (bukan diasumsikan dari nama field): satu baris
 * {@code SatuanItem} TIDAK sekadar berupa satu nama satuan — ia menyimpan
 * SEPASANG nama ({@link #getNamaAwal()} dan {@link #getNama()}) plus
 * faktor konversi bulat {@link #getJumlah()} di antara keduanya. Form
 * pengelolaannya ({@code ais.action.master.sirs.SatuanItemAction}) secara
 * eksplisit melabeli ketiga field ini sebagai "Nama Satuan Mulai"
 * (namaAwal), "Nama Satuan Sampai" (nama), dan "Jumlah Item per Satuan"
 * (jumlah) — menunjukkan bahwa satu baris {@code SatuanItem} sudah
 * mendeskripsikan sendiri satu langkah konversi (mis. "1 Box = 10
 * Strip").
 * </p>
 *
 * <p>
 * Ini membuat konversi satuan item TERSEBAR di DUA mekanisme paralel yang
 * tumpang tindih secara struktural:
 * </p>
 * <ol>
 * <li>Pasangan {@link #getNamaAwal()}/{@link #getNama()}/{@link #getJumlah()}
 * yang melekat pada baris {@code SatuanItem} itu sendiri (dipakai lewat
 * form "Jumlah Item per Satuan" di atas).</li>
 * <li>{@link KonversiSatuanItem}, tabel terpisah yang menghubungkan DUA
 * baris {@code SatuanItem} penuh ({@link KonversiSatuanItem#getSatuanDari()}
 * dan {@link KonversiSatuanItem#getSatuanMenjadi()}) lewat faktor
 * {@link KonversiSatuanItem#getNilaiPersamaan()}, opsional per-item
 * ({@link KonversiSatuanItem#getItem()}).</li>
 * </ol>
 * <p>
 * Kode di kelas ini SENDIRI tidak memakai kedua mekanisme itu untuk
 * saling memvalidasi/menyinkronkan satu sama lain — potensi pemanggil
 * salah pilih mekanisme atau membuat nilai konversi yang tidak konsisten
 * antar keduanya tidak bisa disingkirkan hanya dari model ini; perlu
 * penelusuran lebih lanjut di kode pemanggil (di luar cakupan dokumentasi
 * model ini) bila ingin memastikan konsistensinya.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "satuan_item")
public class SatuanItem extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -3088213612931036389L;
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
	 * Representasi ringkas satuan item ini untuk tampilan/log.
	 *
	 * @return nama satuan (sisi "sampai" dari pasangan konversi).
	 */
	public String toString() {
		return nama;
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

	private String namaAwal;
	private String nama;
	private Integer jumlah;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public SatuanItem() {
	}

	/**
	 * Konstruktor pintasan untuk membuat referensi ke baris satuan item
	 * yang sudah ada berdasarkan ID-nya saja, tanpa perlu memuat seluruh
	 * baris dari database (mis. untuk dipakai langsung sebagai nilai FK).
	 *
	 * @param id ID satuan item yang sudah ada.
	 */
	public SatuanItem(Long id) {
		this.id = id;
	}

	/**
	 * Primary key baris satuan item, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik baris satuan item ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris satuan item.
	 *
	 * @param id ID baris satuan item.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nama satuan sisi "sampai" (label form: "Nama Satuan
	 * Sampai") dari pasangan konversi baris ini.
	 *
	 * @param nama nama satuan tujuan konversi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil nama satuan sisi "sampai". Field inilah yang direferensikan
	 * langsung oleh {@link ItemMedis#getSatuanItem()} sebagai satuan dasar
	 * item, dan yang dicek keunikannya (case-insensitive) oleh
	 * {@code SatuanItemAction.checkNamaJenisBarang()} sebelum baris baru
	 * disimpan.
	 *
	 * @return nama satuan tujuan konversi.
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan faktor konversi (label form: "Jumlah Item per Satuan")
	 * antara {@link #getNamaAwal()} dan {@link #getNama()} — mis. bila
	 * {@code namaAwal} = "Box" dan {@code nama} = "Strip" dengan
	 * {@code jumlah} = 10, artinya 1 Box setara 10 Strip.
	 *
	 * @param jumlah faktor konversi bulat; form pengelolaannya menolak
	 *               nilai {@code null} sebelum simpan, tapi kolom
	 *               database sendiri tidak memaksa {@code NOT NULL}
	 *               (tidak ada anotasi {@code @Column(nullable=false)}
	 *               di getter ini).
	 */
	public void setJumlah(Integer jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil faktor konversi antara {@link #getNamaAwal()} dan
	 * {@link #getNama()} apa adanya, tanpa fallback bila {@code null}.
	 *
	 * @return faktor konversi bulat, atau {@code null} jika belum diisi.
	 */
	public Integer getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan nama satuan sisi "mulai" (label form: "Nama Satuan
	 * Mulai") dari pasangan konversi baris ini.
	 *
	 * @param namaAwal nama satuan asal konversi.
	 */
	public void setNamaAwal(String namaAwal) {
		this.namaAwal = namaAwal;
	}

	/**
	 * Mengambil nama satuan sisi "mulai" dari pasangan konversi baris
	 * ini — lihat javadoc kelas untuk penjelasan lengkap pola konversi
	 * dua-mekanisme ({@code namaAwal}/{@code nama}/{@code jumlah} di
	 * sini vs {@link KonversiSatuanItem}).
	 *
	 * @return nama satuan asal konversi, atau {@code null} jika belum
	 *         diisi.
	 */
	@Column(name = "nama_awal")
	public String getNamaAwal() {
		return namaAwal;
	}

}
