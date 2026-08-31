package ais.database.model.koperasi;

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

import ais.database.model.Deposit;
import ais.database.model.GeneralValueObject;

/**
 * <h3>Penyesuaian Saldo Anggota &mdash; "stok opname" untuk saldo voucher/deposit member.</h3>
 *
 * <p>Perannya persis seperti {@code StokOpname} pada barang, hanya objeknya saldo: mencatat
 * <b>saldo menurut sistem</b>, <b>saldo yang seharusnya</b> menurut hitungan petugas, selisih di
 * antara keduanya, dan alasannya. Tanpa catatan seperti ini, koreksi saldo hanya berupa entri
 * deposit tambahan yang tidak menerangkan apa pun ketika ditanyakan berbulan-bulan kemudian.</p>
 *
 * <p><b>Cara koreksinya diterapkan.</b> Saldo member TIDAK disimpan sebagai satu kolom, melainkan
 * dihitung ({@code DepositHelper.hitungDeposit} = jumlah {@link Deposit} dikurangi pemakaian).
 * Karena itu penyesuaian tidak "menimpa" saldo, melainkan membuat satu baris {@link Deposit}
 * senilai selisihnya &mdash; positif bila saldo kurang, negatif bila saldo lebih. Dengan begitu
 * riwayat mutasi tetap utuh dan saldo hasil hitungan langsung cocok dengan hasil opname.</p>
 *
 * <p>Baris {@link Deposit} yang terbentuk disimpan di {@link #getDeposit()} sebagai jejak balik,
 * sehingga dari catatan opname selalu dapat ditelusuri entri mana yang mengoreksinya.</p>
 */
@Entity
@Audited
@Table(schema = "koperasi", name = "penyesuaian_saldo_anggota")
public class PenyesuaianSaldoAnggota extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private AnggotaKoperasi anggotaKoperasi;
	private Double saldoSistem;
	private Double saldoFisik;
	private Double selisih;
	private Date waktu = ais.ui.util.WaktuUtil.getDate();
	private String keterangan;
	private Deposit deposit;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PenyesuaianSaldoAnggota() {
	}

	@Id
	@GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		return anggotaKoperasi;
	}

	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/** Saldo hasil hitungan sistem PADA SAAT opname disimpan (dibekukan sebagai bukti). */
	@Column(name = "saldo_sistem")
	public Double getSaldoSistem() {
		return saldoSistem;
	}

	public void setSaldoSistem(Double saldoSistem) {
		this.saldoSistem = saldoSistem;
	}

	/** Saldo yang seharusnya menurut petugas (padanan "stok fisik" pada opname barang). */
	@Column(name = "saldo_fisik")
	public Double getSaldoFisik() {
		return saldoFisik;
	}

	public void setSaldoFisik(Double saldoFisik) {
		this.saldoFisik = saldoFisik;
	}

	/** Saldo fisik dikurangi saldo sistem; positif = saldo ditambah, negatif = saldo dikurangi. */
	@Column(name = "selisih")
	public Double getSelisih() {
		return selisih;
	}

	public void setSelisih(Double selisih) {
		this.selisih = selisih;
	}

	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/** Alasan penyesuaian; wajib diisi supaya koreksi saldo selalu dapat dipertanggungjawabkan. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Baris Deposit koreksi yang dibentuk penyesuaian ini (jejak balik ke mutasi saldonya). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "deposit", nullable = true)
	public Deposit getDeposit() {
		return deposit;
	}

	public void setDeposit(Deposit deposit) {
		this.deposit = deposit;
	}

	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	@Column(name = "olehid")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal (dok 61 butir B tahap 2): terisi begitu mesin
	 * {@code PostingDanaAnggotaUtil} menjurnalkan dokumen ini.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
