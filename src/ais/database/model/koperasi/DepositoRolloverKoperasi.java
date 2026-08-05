package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — otomasi ARO simpanan berjangka (deposito).

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

/**
 * <h2>DepositoRolloverKoperasi — Status ARO (Automatic Roll Over) Simpanan Berjangka</h2>
 *
 * <p>
 * Entity ini menyimpan status <b>perpanjangan otomatis (Automatic Roll Over/ARO)</b> untuk tiap
 * simpanan berjangka (deposito) koperasi. Deposito adalah simpanan bertenor: bila jatuh tempo, ada
 * dua kemungkinan — <b>diperpanjang otomatis</b> untuk satu tenor berikutnya, atau <b>dicairkan</b>.
 * Karena tanggal jatuh tempo perlu bergerak maju setiap kali diperpanjang, informasinya disimpan di
 * sini (bukan sekadar dihitung) agar penjadwal (scheduler) dapat memprosesnya secara otomatis dan
 * pengurus dapat memantau/menyetel perilakunya.
 * </p>
 *
 * <h3>Hubungan ke deposito</h3>
 * <p>
 * Satu baris mewakili satu deposito, ditautkan lewat {@link #getTransaksiKoperasiId()} (id
 * {@link TransaksiKoperasi} simpanan berjangka). Sengaja memakai id (bukan relasi objek) agar ringan
 * dan tidak menambah keterikatan audit lintas-entity. Detail anggota/nominal dibaca saat diperlukan
 * dari transaksi terkait.
 * </p>
 *
 * <h3>Perilaku otomatis</h3>
 * <ul>
 * <li>{@link #getAroOtomatis()} = true dan sudah lewat {@link #getTanggalJatuhTempo()} → jatuh tempo
 * diperpanjang satu tenor ({@link #getJangkaWaktuBulan()}), {@link #getJumlahPerpanjangan()}
 * bertambah, dan {@link #getTanggalRolloverTerakhir()} dicatat.</li>
 * <li>{@link #getAroOtomatis()} = false dan sudah lewat jatuh tempo → status menjadi
 * {@link #STATUS_JATUH_TEMPO} (menunggu pencairan manual oleh pengurus).</li>
 * </ul>
 * Perpanjangan hanya menggeser tanggal jatuh tempo (menggulung pokok); perhitungan/pembayaran bunga
 * mengikuti mekanisme bunga simpanan yang sudah ada, sehingga proses ARO bersifat aman dan tidak
 * mengubah nilai akad deposito.
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Mengikuti pola rumah AIS: kunci {@code IDENTITY}, hook audit {@code @PreUpdate}, {@code @Audited},
 * getter aman-null, dan kompatibel Java 1.7. Terdaftar di {@code hibernate.cfg.xml} sehingga
 * {@code hbm2ddl=update} membuat tabel <code>koperasi.deposito_rollover</code> otomatis. Entity tidak
 * menyentuh basis data langsung dan tidak mengubah entity lain (khususnya {@link TransaksiKoperasi}
 * tidak diubah sama sekali).
 * </p>
 *
 * @see ais.action.master.koperasi.helper.DepositoAroHelper
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "deposito_rollover")
public class DepositoRolloverKoperasi extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 6620300014413771001L;

	public static final String STATUS_BERJALAN = "BERJALAN";
	public static final String STATUS_JATUH_TEMPO = "JATUH_TEMPO";
	public static final String STATUS_DICAIRKAN = "DICAIRKAN";

	private Long id;
	private String oleh;
	private String olehId;

	private Long transaksiKoperasiId;
	private Boolean aroOtomatis = true;
	private Date tanggalJatuhTempo;
	private Integer jangkaWaktuBulan = 0;
	private Integer jumlahPerpanjangan = 0;
	private Date tanggalRolloverTerakhir;
	private String status = STATUS_BERJALAN;
	private String keterangan;
	private Boolean aktif = true;

	public DepositoRolloverKoperasi() {
	}

	public DepositoRolloverKoperasi(Long id) {
		this.id = id;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	@Column(name = "transaksi_koperasi_id")
	public Long getTransaksiKoperasiId() {
		return transaksiKoperasiId;
	}

	public void setTransaksiKoperasiId(Long transaksiKoperasiId) {
		this.transaksiKoperasiId = transaksiKoperasiId;
	}

	@Column(name = "aro_otomatis")
	public Boolean getAroOtomatis() {
		return aroOtomatis == null ? true : aroOtomatis;
	}

	public void setAroOtomatis(Boolean aroOtomatis) {
		this.aroOtomatis = aroOtomatis;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_jatuh_tempo")
	public Date getTanggalJatuhTempo() {
		return tanggalJatuhTempo;
	}

	public void setTanggalJatuhTempo(Date tanggalJatuhTempo) {
		this.tanggalJatuhTempo = tanggalJatuhTempo;
	}

	@Column(name = "jangka_waktu_bulan")
	public Integer getJangkaWaktuBulan() {
		return jangkaWaktuBulan == null ? 0 : jangkaWaktuBulan;
	}

	public void setJangkaWaktuBulan(Integer jangkaWaktuBulan) {
		this.jangkaWaktuBulan = jangkaWaktuBulan;
	}

	@Column(name = "jumlah_perpanjangan")
	public Integer getJumlahPerpanjangan() {
		return jumlahPerpanjangan == null ? 0 : jumlahPerpanjangan;
	}

	public void setJumlahPerpanjangan(Integer jumlahPerpanjangan) {
		this.jumlahPerpanjangan = jumlahPerpanjangan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_rollover_terakhir")
	public Date getTanggalRolloverTerakhir() {
		return tanggalRolloverTerakhir;
	}

	public void setTanggalRolloverTerakhir(Date tanggalRolloverTerakhir) {
		this.tanggalRolloverTerakhir = tanggalRolloverTerakhir;
	}

	@Column(name = "status", length = 20)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_BERJALAN : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Override
	public String toString() {
		return "DepositoRollover[tx=" + transaksiKoperasiId + ", jatuhTempo=" + tanggalJatuhTempo + "]";
	}
}
