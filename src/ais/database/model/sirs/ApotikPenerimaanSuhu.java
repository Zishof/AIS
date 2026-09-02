package ais.database.model.sirs;

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
 * Bukti suhu penerimaan barang cold-chain (bagian IR-09 yang dapat dikerjakan
 * tanpa mengarang alur pengadaan).
 *
 * <p><b>Kenapa hanya suhu, bukan seluruh IR-09.</b> Nomor PO dan penerimaan
 * sebagian menuntut adanya dokumen pesanan lebih dulu — dan bentuk alur
 * pengadaan (siapa menyetujui, bagaimana harga disepakati, apakah PO dibuat di
 * AIS atau di luar) berbeda-beda antar apotek. Membuatnya tanpa keputusan
 * pemilik proses berarti menebak. Pencatatan suhu tidak butuh tebakan: barang
 * rantai dingin memang harus diukur saat diterima, dan rentang 2–8 °C adalah
 * standar yang sudah dipakai di layar formularium.</p>
 *
 * <p>Dicatat per FAKTUR, bukan per lot: termometer dibaca sekali saat kotak
 * dibuka, bukan per butir obat. Tabel BARU sehingga {@code hbm2ddl=update}
 * membuatnya berikut tabel auditnya — tanpa migrasi manual.</p>
 *
 * <p><b>Batas jujur:</b> server MENYIMPAN, tidak menolak. Tidak ada aturan
 * "tolak penerimaan bila suhu di luar rentang", karena keputusan menerima atau
 * menolak barang rantai dingin adalah wewenang apoteker penanggung jawab dan
 * bergantung pada SOP tiap apotek. Layar memperingatkan; yang memutuskan
 * manusia.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_penerimaan_suhu")
public class ApotikPenerimaanSuhu extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Rentang rantai dingin baku yang dipakai layar (2–8 derajat Celsius). */
	public static final double SUHU_MIN_WAJAR = 2d;
	public static final double SUHU_MAKS_WAJAR = 8d;

	private Long id;
	private String noFaktur;
	private String penyedia;

	/** Suhu terbaca saat barang diterima, derajat Celsius. */
	private Double suhuCelsius;

	/** true bila faktur ini memang memuat item bertanda cold-chain. */
	private Boolean adaColdChain;
	private String keterangan;
	private Date waktu;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Apakah [suhu] berada di luar rentang rantai dingin baku. */
	public static boolean diLuarRentang(Double suhu) {
		if (suhu == null) return false;
		double v = suhu.doubleValue();
		return v < SUHU_MIN_WAJAR || v > SUHU_MAKS_WAJAR;
	}

	public String toString() {
		return (id == null ? "" : id) + "-" + (noFaktur == null ? "" : noFaktur);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "no_faktur", length = 80)
	public String getNoFaktur() { return noFaktur; }
	public void setNoFaktur(String noFaktur) { this.noFaktur = noFaktur; }

	@Column(name = "penyedia", length = 160)
	public String getPenyedia() { return penyedia; }
	public void setPenyedia(String penyedia) { this.penyedia = penyedia; }

	@Column(name = "suhu_celsius")
	public Double getSuhuCelsius() { return suhuCelsius; }
	public void setSuhuCelsius(Double suhuCelsius) { this.suhuCelsius = suhuCelsius; }

	@Column(name = "ada_cold_chain")
	public Boolean getAdaColdChain() { return adaColdChain == null ? Boolean.FALSE : adaColdChain; }
	public void setAdaColdChain(Boolean adaColdChain) { this.adaColdChain = adaColdChain; }

	@Column(name = "keterangan", length = 500)
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	@Column(name = "oleh", length = 60)
	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { this.oleh = oleh; }

	@Column(name = "oleh_id", length = 60)
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
