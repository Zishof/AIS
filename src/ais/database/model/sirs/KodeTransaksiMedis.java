package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Entitas katalog master kode transaksi medis (mis. transaksi
 * apotik/farmasi) pada schema {@code sirs} (tabel
 * {@code kode_transaksi_medis}) — mis. "Pemakaian", "Retur", "Koreksi",
 * "Penerimaan", dsb. Dipakai luas di modul apotik/farmasi
 * ({@code ApotikApiHelper}, {@code ApotikPersediaanHelper},
 * {@code ApotikRacikanProduksiHelper}, {@code ApotikKodeTransaksiHelper},
 * dsb.) sebagai relasi {@code kodeTransaksi} pada baris detail transaksi.
 *
 * <p>
 * <b>Field {@link #getJenis()} BUKAN SEKADAR LABEL DESKRIPTIF</b> —
 * diverifikasi dari kode di {@code Biaya}, nilainya (salah satu dari
 * {@link #PENAMBAHAN} {@code = 1} atau {@link #PENGURANGAN} {@code = -1})
 * DIPAKAI LANGSUNG SEBAGAI PENGALI ARITMATIKA saat menghitung jumlah
 * pemakaian/perubahan stok: {@code jumlah = (-kodeTransaksi.getJenis()) *
 * qty * ...}. Artinya baris katalog ini menentukan ARAH TANDA
 * (penambahan vs pengurangan stok) transaksi secara langsung — mengubah
 * nilai {@code jenis} pada baris katalog yang sudah dipakai transaksi
 * berjalan akan MEMBALIK ARAH perhitungan stok transaksi historis yang
 * merujuknya, karena nilai dibaca ulang dari relasi setiap kali
 * dihitung, bukan disalin statis saat transaksi dibuat.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "kode_transaksi_medis")
public class KodeTransaksiMedis extends GeneralValueObject {

	/**
	 * Nilai {@link #getJenis()} penanda arah transaksi PENAMBAHAN stok
	 * (mis. penerimaan). Dipakai sebagai pengali {@code +1} langsung
	 * dalam perhitungan jumlah — lihat javadoc kelas.
	 */
	public static final Integer PENAMBAHAN = 1;
	/**
	 * Nilai {@link #getJenis()} penanda arah transaksi PENGURANGAN stok
	 * (mis. pemakaian/retur keluar). Dipakai sebagai pengali {@code -1}
	 * langsung dalam perhitungan jumlah — lihat javadoc kelas.
	 */
	public static final Integer PENGURANGAN = -1;

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
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
	 * Representasi ringkas kode transaksi medis ini untuk keperluan
	 * tampilan/log.
	 *
	 * @return string {@code kode - nama}.
	 */
	public String toString() {
		return kode + " - " + nama;
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

	private String kode;
	private String nama;
	private String keterangan;
	private Integer jenis;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public KodeTransaksiMedis() {
	}

	/**
	 * Primary key baris kode transaksi medis, auto-increment (IDENTITY)
	 * dan diisi database.
	 *
	 * @return ID unik kode transaksi medis ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID kode transaksi medis.
	 *
	 * @param id ID kode transaksi medis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama kode transaksi medis.
	 *
	 * @return nama kode transaksi medis.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama kode transaksi medis.
	 *
	 * @param nama nama kode transaksi medis.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan bebas kode transaksi medis ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas kode transaksi medis ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kode singkat kode transaksi medis ini.
	 *
	 * @param kode kode singkat.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil kode singkat kode transaksi medis ini.
	 *
	 * @return kode singkat, atau {@code null} jika belum diisi.
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan arah transaksi ini — WAJIB salah satu dari
	 * {@link #PENAMBAHAN} atau {@link #PENGURANGAN}. Lihat javadoc kelas:
	 * nilai ini dipakai langsung sebagai pengali aritmatika perhitungan
	 * stok, bukan sekadar label.
	 *
	 * @param jenis {@link #PENAMBAHAN} atau {@link #PENGURANGAN}.
	 */
	public void setJenis(Integer jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengambil arah transaksi ini.
	 *
	 * @return {@link #PENAMBAHAN} atau {@link #PENGURANGAN}, atau
	 *         {@code null} jika belum diisi.
	 */
	public Integer getJenis() {
		return jenis;
	}

}
