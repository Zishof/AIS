package ais.database.model.akunting;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Saldo awal (neraca awal) satu akun pada tanggal pembukaan pembukuan.
 *
 * <p><b>Kenapa perlu.</b> Tanpa saldo awal, Neraca dan Buku Besar hanya berisi transaksi yang
 * tercatat sejak sistem dipakai; kas, persediaan, piutang, utang, dan modal yang sudah ada
 * sebelum go-live tidak pernah muncul, sehingga laporan tidak akan pernah sama dengan keadaan
 * sebenarnya walau seluruh dokumen sudah diposting. Entitas ini menyimpan angka pembukaannya,
 * lalu diposting menjadi SATU jurnal pembukaan.</p>
 *
 * <p><b>Bentuk data sengaja datar</b> (satu baris = satu akun, bukan header + detail) supaya
 * bisa diisi/diperbaiki per akun, diunggah dari Excel, dan diposting bertahap tanpa mengunci
 * seluruh daftar. Baris yang sudah diposting ditandai {@code postingHistory} sehingga tidak
 * mungkin terposting dua kali; koreksi setelah posting dilakukan lewat jurnal penyesuaian,
 * bukan mengubah baris ini.</p>
 *
 * <p>Tabelnya dibuat otomatis oleh Hibernate (aturan proyek: ALTER/CREATE diserahkan ke Hibernate).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "saldo_awal_akun")
public class SaldoAwalAkun extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Akun akun;
	private Date tanggal;
	private Double debet;
	private Double kredit;
	private String keterangan;
	private ais.database.model.rab.SatuanKerja satuanKerja;
	private PostingHistory postingHistory;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SaldoAwalAkun() {
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		return akun;
	}

	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/** Tanggal pembukaan; jurnal pembukaan dibuat pada tanggal ini. */
	@Column(name = "tanggal", nullable = true)
	public Date getTanggal() {
		return tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(name = "debet", nullable = true)
	public Double getDebet() {
		return debet == null ? Double.valueOf(0) : debet;
	}

	public void setDebet(Double debet) {
		this.debet = debet;
	}

	@Column(name = "kredit", nullable = true)
	public Double getKredit() {
		return kredit == null ? Double.valueOf(0) : kredit;
	}

	public void setKredit(Double kredit) {
		this.kredit = kredit;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public ais.database.model.rab.SatuanKerja getSatuanKerja() {
		return satuanKerja;
	}

	public void setSatuanKerja(ais.database.model.rab.SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Terisi bila baris ini sudah masuk jurnal pembukaan; kunci anti-posting-ganda. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	@Column(name = "oleh", nullable = true)
	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	@Column(name = "olehid", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	@Column(name = "tanggal_dirubah", nullable = true)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
