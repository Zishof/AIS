package ais.database.model.asset;

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
 * <h2>PengirimanGudang — dokumen kepala pengiriman antar {@link Lokasi} (Gudang Pusat ↔ Cabang/Outlet).</h2>
 *
 * <p>
 * Melengkapi {@code StokLokasiUtil.catatTransfer} (transfer instan-atomik, tanpa jeda waktu) dengan
 * alur <b>kirim → dalam perjalanan → terima</b> yang mencerminkan kenyataan fisik: barang butuh waktu
 * tempuh, dan stok di lokasi tujuan seharusnya baru bertambah SETELAH penerima mengonfirmasi barang
 * benar-benar diterima (boleh penuh, boleh sebagian) — bukan seketika saat pengiriman dibuat.
 * </p>
 *
 * <h3>Desain: lokasi transit virtual, BUKAN kolom status baru di {@link MutasiLokasi}</h3>
 * <p>
 * Entity ini SENGAJA tidak menambah kolom status ke {@code MutasiLokasi} (berisiko terhadap seluruh
 * laporan yang sudah bergantung pada {@code SUM(qty)} polos). Sebagai gantinya, setiap
 * {@code PengirimanGudang} memiliki SATU {@link #getLokasiTransit() lokasi transit} virtual
 * (dibuat otomatis, {@code aktif=false} agar tak muncul di picker manual mana pun) yang berperan
 * sebagai "gudang singgah": saat dikirim, barang dicatat KELUAR dari {@link #getLokasiAsal()} dan
 * MASUK ke lokasi transit (dua baris {@code MutasiLokasi} riil, lewat
 * {@code StokLokasiUtil.catatKeluar}/{@code catatMasuk} yang SUDAH ADA, tanpa perubahan apa pun ke
 * kelas itu). Saat diterima, barang dicatat KELUAR dari lokasi transit dan MASUK ke
 * {@link #getLokasiTujuan()} — hanya sejumlah yang benar-benar dikonfirmasi diterima
 * ({@code qtyTerima} di {@link PengirimanGudangDetail}, boleh kurang dari {@code qtyKirim} untuk
 * penerimaan sebagian). Sisa yang belum diterima tetap "mengendap" di lokasi transit sebagai jejak
 * audit yang terlihat pada laporan pergudangan — TIDAK dikoreksi otomatis (selaras prinsip stock
 * opname: temuan selisih perlu ditindaklanjuti, bukan langsung dikoreksi diam-diam).
 * </p>
 *
 * <h3>Status dokumen</h3>
 * <p>
 * {@link #getStatus() status} murni penanda tampilan/alur kerja pada level DOKUMEN (bukan pada baris
 * {@code MutasiLokasi} mana pun): {@link #DIKIRIM}, {@link #DITERIMA}, {@link #DITERIMA_SEBAGIAN},
 * {@link #DIBATALKAN}. Tidak memengaruhi perhitungan stok — itu murni hasil {@code SUM(qty)} atas
 * {@code MutasiLokasi} seperti biasa.
 * </p>
 *
 * <h3>Cakupan yang SENGAJA di luar entity ini</h3>
 * <p>
 * Tidak menyentuh {@code koperasi.Produk.stok}, {@code KantinHelper}, atau alur checkout POS sama
 * sekali — murni pergerakan stok internal gudang/outlet di skema {@code asset}, terpisah total dari
 * jalur penjualan POS (lihat catatan arsitektur pada dokumen analisis sistem terpadu).
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see PengirimanGudangDetail
 * @see ais.action.master.inventory.PengirimanGudangUtil
 * @see ais.action.master.inventory.StokLokasiUtil
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "pengiriman_gudang")
public class PengirimanGudang extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 1L;

	/** Dikirim, menunggu konfirmasi terima (barang berada di lokasi transit). */
	public static final String DIKIRIM = "DIKIRIM";
	/** Seluruh baris diterima penuh (qtyTerima == qtyKirim di semua baris). */
	public static final String DITERIMA = "DITERIMA";
	/** Sebagian baris/qty diterima; sisanya masih mengendap di lokasi transit. */
	public static final String DITERIMA_SEBAGIAN = "DITERIMA_SEBAGIAN";
	/** Dibatalkan sebelum diterima (barang dikembalikan manual ke asal, di luar cakupan otomatis). */
	public static final String DIBATALKAN = "DIBATALKAN";

	/** Primary key auto-generated (IDENTITY) tabel {@code asset.pengiriman_gudang}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;
	/** Waktu perubahan terakhir baris ini; diperbarui otomatis oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private Lokasi lokasiAsal;
	private Lokasi lokasiTujuan;
	private Lokasi lokasiTransit;
	/** Tanggal dokumen ini dikirim; default waktu saat objek dibuat. */
	private Date tanggalKirim = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal dokumen ini dikonfirmasi diterima (penuh/sebagian); {@code null} selama {@link #DIKIRIM}. */
	private Date tanggalTerima;
	private String status;
	/** Keterangan bebas saat pengiriman dibuat, opsional. */
	private String keterangan;
	/** Keterangan bebas saat penerimaan dikonfirmasi, opsional. */
	private String keteranganTerima;
	/** Nama pengguna yang mengirim (mengonfirmasi pengiriman); opsional. */
	private String dikirimOleh;
	/** Id pengguna yang mengirim; opsional. */
	private String dikirimOlehId;
	/** Nama pengguna yang menerima (mengonfirmasi penerimaan); opsional. */
	private String diterimaOleh;
	/** Id pengguna yang menerima; opsional. */
	private String diterimaOlehId;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public PengirimanGudang() {
	}

	/** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila
	 *         belum pernah diubah sejak dimuat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengisi nama pengguna audit. Nilai {@code null}/kosong diabaikan agar jejak lama tidak
	 * tertimpa hampa.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return id pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila belum
	 *         pernah diubah sejak dimuat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Nilai {@code null}/kosong diabaikan, sama seperti
	 * {@link #setOleh(String)}.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu serta
	 * identitas pengguna aktif. Dipicu otomatis oleh Hibernate, tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return waktu perubahan terakhir baris ini; tidak pernah {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat objek dibuat, lalu
	 * ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return kode dokumen unik (format {@code "KRM-" + barcode}), dipakai juga sbg {@code referensi} baris MutasiLokasi terkait. */
	@Column(name = "kode", length = 64, unique = true)
	public String getKode() {
		return kode;
	}

	/**
	 * Mengisi kode dokumen.
	 *
	 * @param kode kode dokumen unik (format {@code "KRM-" + barcode}).
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return lokasi asal (mis. Gudang Pusat). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi_asal", nullable = false)
	public Lokasi getLokasiAsal() {
		lokasiAsal = check(lokasiAsal);
		return lokasiAsal;
	}

	/**
	 * Mengisi lokasi asal.
	 *
	 * @param lokasiAsal lokasi asal (wajib diisi sebelum simpan).
	 */
	public void setLokasiAsal(Lokasi lokasiAsal) {
		this.lokasiAsal = lokasiAsal;
	}

	/** @return lokasi tujuan (mis. Cabang/Outlet). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi_tujuan", nullable = false)
	public Lokasi getLokasiTujuan() {
		lokasiTujuan = check(lokasiTujuan);
		return lokasiTujuan;
	}

	/**
	 * Mengisi lokasi tujuan.
	 *
	 * @param lokasiTujuan lokasi tujuan (wajib diisi sebelum simpan).
	 */
	public void setLokasiTujuan(Lokasi lokasiTujuan) {
		this.lokasiTujuan = lokasiTujuan;
	}

	/** @return lokasi transit virtual milik pengiriman ini (dibuat otomatis, satu per dokumen). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi_transit", nullable = false)
	public Lokasi getLokasiTransit() {
		lokasiTransit = check(lokasiTransit);
		return lokasiTransit;
	}

	/**
	 * Mengisi lokasi transit virtual.
	 *
	 * @param lokasiTransit lokasi transit (wajib diisi sebelum simpan; biasanya dibuat otomatis
	 *                      oleh {@code PengirimanGudangUtil}, bukan diisi manual).
	 */
	public void setLokasiTransit(Lokasi lokasiTransit) {
		this.lokasiTransit = lokasiTransit;
	}

	/** @return tanggal dokumen ini dikirim. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_kirim")
	public Date getTanggalKirim() {
		return tanggalKirim;
	}

	/**
	 * Mengisi tanggal kirim.
	 *
	 * @param tanggalKirim tanggal kirim.
	 */
	public void setTanggalKirim(Date tanggalKirim) {
		this.tanggalKirim = tanggalKirim;
	}

	/** @return tanggal dokumen dikonfirmasi diterima; {@code null} selama status {@link #DIKIRIM}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_terima")
	public Date getTanggalTerima() {
		return tanggalTerima;
	}

	/**
	 * Mengisi tanggal terima.
	 *
	 * @param tanggalTerima tanggal terima, boleh {@code null}.
	 */
	public void setTanggalTerima(Date tanggalTerima) {
		this.tanggalTerima = tanggalTerima;
	}

	/** @return status dokumen ({@link #DIKIRIM}/{@link #DITERIMA}/{@link #DITERIMA_SEBAGIAN}/{@link #DIBATALKAN}). */
	@Column(name = "status", length = 32)
	public String getStatus() {
		return status == null ? DIKIRIM : status;
	}

	/**
	 * Mengisi status dokumen.
	 *
	 * @param status salah satu {@link #DIKIRIM}/{@link #DITERIMA}/{@link #DITERIMA_SEBAGIAN}/
	 *               {@link #DIBATALKAN}; {@code null} diperlakukan sebagai {@link #DIKIRIM} oleh
	 *               {@link #getStatus()}.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return keterangan bebas saat pengiriman dibuat, boleh {@code null}. */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan pengiriman.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return keterangan bebas saat penerimaan dikonfirmasi, boleh {@code null}. */
	@Column(name = "keterangan_terima", columnDefinition = "text")
	public String getKeteranganTerima() {
		return keteranganTerima;
	}

	/**
	 * Mengisi keterangan penerimaan.
	 *
	 * @param keteranganTerima teks keterangan, boleh {@code null}.
	 */
	public void setKeteranganTerima(String keteranganTerima) {
		this.keteranganTerima = keteranganTerima;
	}

	/** @return nama pengguna yang mengirim (mengonfirmasi pengiriman), boleh {@code null}. */
	@Column(name = "dikirim_oleh", length = 255)
	public String getDikirimOleh() {
		return dikirimOleh;
	}

	/**
	 * Mengisi nama pengguna pengirim.
	 *
	 * @param dikirimOleh nama pengguna, boleh {@code null}.
	 */
	public void setDikirimOleh(String dikirimOleh) {
		this.dikirimOleh = dikirimOleh;
	}

	/** @return id pengguna yang mengirim, boleh {@code null}. */
	@Column(name = "dikirim_oleh_id", length = 64)
	public String getDikirimOlehId() {
		return dikirimOlehId;
	}

	/**
	 * Mengisi id pengguna pengirim.
	 *
	 * @param dikirimOlehId id pengguna, boleh {@code null}.
	 */
	public void setDikirimOlehId(String dikirimOlehId) {
		this.dikirimOlehId = dikirimOlehId;
	}

	/** @return nama pengguna yang menerima (mengonfirmasi penerimaan), boleh {@code null}. */
	@Column(name = "diterima_oleh", length = 255)
	public String getDiterimaOleh() {
		return diterimaOleh;
	}

	/**
	 * Mengisi nama pengguna penerima.
	 *
	 * @param diterimaOleh nama pengguna, boleh {@code null}.
	 */
	public void setDiterimaOleh(String diterimaOleh) {
		this.diterimaOleh = diterimaOleh;
	}

	/** @return id pengguna yang menerima, boleh {@code null}. */
	@Column(name = "diterima_oleh_id", length = 64)
	public String getDiterimaOlehId() {
		return diterimaOlehId;
	}

	/**
	 * Mengisi id pengguna penerima.
	 *
	 * @param diterimaOlehId id pengguna, boleh {@code null}.
	 */
	public void setDiterimaOlehId(String diterimaOlehId) {
		this.diterimaOlehId = diterimaOlehId;
	}
}
