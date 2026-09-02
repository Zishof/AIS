package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
import ais.database.model.koperasi.CaraPembayaranKoperasi;

/**
 * Metode pembayaran yang dipakai satu transaksi apotik (IR-07).
 *
 * <p><b>Mengapa tabel terpisah, bukan kolom baru di {@code TransaksiMedis}?</b>
 * {@code TransaksiMedis} dipakai bersama modul rumah sakit dan sudah
 * {@code @Audited}; menambah kolom di sana menuntut ALTER pada tabel audit
 * lama (gotcha Envers) dan menyentuh jalur yang tidak sedang dimodernisasi.
 * Tabel BARU dibuat otomatis oleh {@code hbm2ddl=update} berikut tabel
 * auditnya, sehingga tidak ada migrasi manual sama sekali.</p>
 *
 * <p>Satu transaksi boleh punya lebih dari satu baris; sejak IR-11 hal itu
 * benar-benar dipakai untuk pembayaran terpisah (mis. sebagian tunai,
 * sebagian QRIS). Penjumlahan nominal divalidasi pemanggil ({@code bayar}),
 * bukan entity ini.</p>
 *
 * <p><b>PERHATIAN untuk perubahan berikutnya.</b> Kalimat di atas tentang
 * "tidak ada migrasi manual" hanya berlaku saat tabel ini BELUM ada di
 * produksi. Sejak ia terbentuk, {@code hbm2ddl=update} menambah kolom baru ke
 * tabel utama tetapi TIDAK ke tabel {@code __audit}-nya, sehingga INSERT audit
 * gagal dan seluruh transaksi ter-rollback. Kolom {@code tunai}/{@code kembalian}
 * (IR-11) karena itu disertai
 * {@code webapp/sql/migrasi_apotik_ir11_pembayaran.sql} yang WAJIB dijalankan
 * sebelum restart.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_pembayaran_transaksi")
public class ApotikPembayaranTransaksi extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private TransaksiMedis transaksi;
	private CaraPembayaranKoperasi caraBayar;

	/** Salinan nama metode saat transaksi -- master boleh berubah/dinonaktifkan. */
	private String namaCaraBayar;
	private Double nominal;

	/**
	 * Uang tunai yang DITERIMA kasir dan kembalian yang diberikan (IR-11).
	 *
	 * <p>Keduanya hanya bermakna untuk metode yang memberi kembalian. Sebelum
	 * kolom ini ada, angka tersebut hanya hidup di layar kasir dan hilang
	 * begitu transaksi selesai -- akibatnya rekonsiliasi laci tidak dapat
	 * membedakan "kembalian belum diberikan" dari "uang kurang". Nominal tetap
	 * jumlah yang DIBUKUKAN; tunai/kembalian adalah catatan penyerahan uang.</p>
	 */
	private Double tunai;
	private Double kembalian;
	private String referensi;
	private Date waktu;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (namaCaraBayar == null ? "" : namaCaraBayar);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi", nullable = false)
	public TransaksiMedis getTransaksi() { return transaksi; }
	public void setTransaksi(TransaksiMedis transaksi) { this.transaksi = transaksi; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_bayar", nullable = true)
	public CaraPembayaranKoperasi getCaraBayar() { return caraBayar; }
	public void setCaraBayar(CaraPembayaranKoperasi caraBayar) { this.caraBayar = caraBayar; }

	@Column(name = "nama_cara_bayar", length = 120)
	public String getNamaCaraBayar() { return namaCaraBayar; }
	public void setNamaCaraBayar(String namaCaraBayar) { this.namaCaraBayar = namaCaraBayar; }

	@Column(name = "nominal", nullable = false)
	public Double getNominal() { return nominal == null ? Double.valueOf(0) : nominal; }
	public void setNominal(Double nominal) { this.nominal = nominal; }

	/** Nomor referensi kanal (mis. nomor approval EDC / QRIS) bila ada. */
	@Column(name = "tunai")
	public Double getTunai() { return tunai; }
	public void setTunai(Double tunai) { this.tunai = tunai; }

	@Column(name = "kembalian")
	public Double getKembalian() { return kembalian; }
	public void setKembalian(Double kembalian) { this.kembalian = kembalian; }

	@Column(name = "referensi", length = 160)
	public String getReferensi() { return referensi; }
	public void setReferensi(String referensi) { this.referensi = referensi; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
