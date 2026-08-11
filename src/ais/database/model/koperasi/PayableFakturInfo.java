package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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
import ais.database.model.inventory.PengadaanFaktur;

/**
 * Info hutang per faktur kulakan (layar legacy 20-23, varian Inventory &amp; Sales) -- EXTENSION
 * 1:1 di atas {@link PengadaanFaktur} existing (kontrak kulakan_faktur_* TIDAK diubah): jenis
 * pembayaran {@link #JENIS_CASH}/{@link #JENIS_DP}/{@link #JENIS_CREDIT}, termin (dasar jatuh
 * tempo, pola SYARAT_BYR/TRAN_HUT.DBF), dan nilai dibayar saat faktur (cash penuh / DP).
 *
 * <p>OUTSTANDING TIDAK DISIMPAN -- selalu dihitung: {@code totalFakturFinal - dibayarAwal -
 * SUM(alokasi pembayaran)} (register event, Matriks layar 22). Faktur legacy TANPA baris info
 * dianggap CASH lunas (alur kulakan lama memang tunai) -- TIDAK menimbulkan hutang diam-diam;
 * pemilik dapat melengkapi info lewat aksi {@code si_purchase_terms_save} bila faktur lama
 * ternyata kredit (backfill sadar, bukan tebakan migrasi).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "payable_faktur_info")
public class PayableFakturInfo extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String JENIS_CASH = "CASH";
	public static final String JENIS_DP = "DP";
	public static final String JENIS_CREDIT = "CREDIT";

	private Long id;
	private PengadaanFaktur pengadaanFaktur;
	private String jenisPembayaran;
	private Integer terminHari;
	private Date jatuhTempo;
	private BigDecimal dibayarAwal;
	private String keterangan;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PayableFakturInfo() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengadaan_faktur", nullable = false)
	public PengadaanFaktur getPengadaanFaktur() {
		return pengadaanFaktur;
	}

	public void setPengadaanFaktur(PengadaanFaktur pengadaanFaktur) {
		this.pengadaanFaktur = pengadaanFaktur;
	}

	@Column(name = "jenis_pembayaran", length = 20)
	public String getJenisPembayaran() {
		return jenisPembayaran == null || jenisPembayaran.trim().isEmpty() ? JENIS_CASH : jenisPembayaran;
	}

	public void setJenisPembayaran(String jenisPembayaran) {
		this.jenisPembayaran = jenisPembayaran;
	}

	@Column(name = "termin_hari")
	public Integer getTerminHari() {
		return terminHari == null ? Integer.valueOf(0) : terminHari;
	}

	public void setTerminHari(Integer terminHari) {
		this.terminHari = terminHari;
	}

	/** Jatuh tempo = tanggal faktur + termin (dihitung &amp; disimpan saat simpan info -- kolom
	 *  sendiri supaya bisa di-query aging tanpa join berulang, dan bisa dikoreksi manual). */
	@Temporal(TemporalType.DATE)
	@Column(name = "jatuh_tempo")
	public Date getJatuhTempo() {
		return jatuhTempo;
	}

	public void setJatuhTempo(Date jatuhTempo) {
		this.jatuhTempo = jatuhTempo;
	}

	@Column(name = "dibayar_awal", precision = 19, scale = 2)
	public BigDecimal getDibayarAwal() {
		return dibayarAwal == null ? BigDecimal.ZERO : dibayarAwal;
	}

	public void setDibayarAwal(BigDecimal dibayarAwal) {
		this.dibayarAwal = dibayarAwal;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
