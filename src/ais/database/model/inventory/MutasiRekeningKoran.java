package ais.database.model.inventory;

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
 * <h2>MutasiRekeningKoran — Baris Rekening Koran Bank untuk Rekonsiliasi Bank (Toko/Kantin).</h2>
 *
 * <p>
 * Entity BARU untuk menampung baris <b>rekening koran dari BANK</b> (sumber eksternal), yang
 * dientri/diimpor lalu <i>dicocokkan</i> dengan Buku Besar (jurnal akuntansi) — sehingga tersedia
 * laporan <b>Rekonsiliasi Bank</b> gaya Accurate (Saldo Buku vs Saldo Rekening Koran + item belum
 * cocok). Ini melengkapi laporan "Rekening Koran" yang selama ini diturunkan dari JURNAL (sisi buku);
 * entity ini adalah sisi BANK-nya. Dengan pendaftaran di {@code hibernate.cfg.xml}, tabel
 * {@code koperasi.mutasi_rekening_koran} otomatis dibuat (hbm2ddl=update) saat RESTART.
 * </p>
 *
 * <h3>Konvensi nilai</h3>
 * <ul>
 *   <li><b>masuk</b> = uang MASUK ke rekening (menambah saldo bank kita) — setoran/penerimaan.</li>
 *   <li><b>keluar</b> = uang KELUAR dari rekening (mengurangi saldo) — penarikan/pembayaran/biaya bank.</li>
 *   <li><b>Mutasi bersih</b> = masuk − keluar. Ini dibandingkan dengan (debet − kredit) jurnal pada
 *       akun bank yang sama untuk mendapatkan <i>selisih</i> rekonsiliasi.</li>
 *   <li><b>sudahRekon</b> = baris ini sudah dicocokkan dengan entri buku.</li>
 * </ul>
 *
 * <p>
 * Penamaan kolom mengikuti aturan proyek: field ber-@Column memakai nama eksplisit, field
 * numerik/tanggal/boolean tanpa @Column ter-<i>fold</i> menjadi huruf kecil tanpa underscore
 * (mis. {@code namaAkunBank}→{@code namaakunbank}, {@code sudahRekon}→{@code sudahrekon},
 * {@code tanggalRekon}→{@code tanggalrekon}). {@code akunBank} menyimpan id akun bank di
 * {@code akunting.akun}. Kompatibel Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul rekonsiliasi bank)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "mutasi_rekening_koran")
public class MutasiRekeningKoran extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Toko toko;
	private Long akunBank;
	private String namaAkunBank;
	private Date tanggal;
	private String keterangan;
	private Double masuk;
	private Double keluar;
	private Double saldo;
	private String referensi;
	private Boolean sudahRekon;
	private Date tanggalRekon;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public MutasiRekeningKoran() {
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
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	@Column(name = "akun_bank")
	public Long getAkunBank() {
		return akunBank;
	}

	public void setAkunBank(Long akunBank) {
		this.akunBank = akunBank;
	}

	public String getNamaAkunBank() {
		return namaAkunBank;
	}

	public void setNamaAkunBank(String namaAkunBank) {
		this.namaAkunBank = namaAkunBank;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Double getMasuk() {
		return masuk == null ? 0.0 : masuk;
	}

	public void setMasuk(Double masuk) {
		this.masuk = masuk;
	}

	public Double getKeluar() {
		return keluar == null ? 0.0 : keluar;
	}

	public void setKeluar(Double keluar) {
		this.keluar = keluar;
	}

	public Double getSaldo() {
		return saldo == null ? 0.0 : saldo;
	}

	public void setSaldo(Double saldo) {
		this.saldo = saldo;
	}

	public String getReferensi() {
		return referensi;
	}

	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}

	public Boolean getSudahRekon() {
		return sudahRekon == null ? Boolean.FALSE : sudahRekon;
	}

	public void setSudahRekon(Boolean sudahRekon) {
		this.sudahRekon = sudahRekon;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalRekon() {
		return tanggalRekon;
	}

	public void setTanggalRekon(Date tanggalRekon) {
		this.tanggalRekon = tanggalRekon;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
