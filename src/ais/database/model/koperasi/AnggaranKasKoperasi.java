package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — fitur Anggaran/Perencanaan Kas (RAPB).

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

/**
 * <h2>AnggaranKasKoperasi — Rencana Anggaran Kas (RAPB) Koperasi per Tahun Buku</h2>
 *
 * <p>
 * Entity ini menyimpan <b>rencana anggaran kas</b> koperasi untuk satu tahun buku: perkiraan seluruh
 * <b>penerimaan kas</b> (uang masuk) dan <b>pengeluaran kas</b> (uang keluar) beserta saldo kas awal
 * tahun. Sesuai SOM USPK dan praktik tata kelola koperasi, pengurus wajib menyusun Rencana Anggaran
 * Pendapatan dan Belanja (RAPB) yang disahkan Rapat Anggota; anggaran kas adalah bagian arus kas dari
 * rencana tersebut. Dengan menyimpannya di sini, koperasi dapat <b>membandingkan rencana dengan
 * realisasi</b> sepanjang tahun, mengetahui apakah penerimaan/pengeluaran sesuai target, serta
 * memproyeksikan saldo kas akhir agar likuiditas tetap terjaga.
 * </p>
 *
 * <h3>Mengapa rencana tahunan (bukan matriks bulanan)?</h3>
 * <p>
 * Anggaran disimpan sebagai angka tahunan per kategori — bukan matriks 12&nbsp;bulan &times; banyak pos —
 * agar mudah diisi pengurus koperasi kecil-menengah dan tidak memberatkan. Realisasi tetap dihitung
 * dari data transaksi nyata (setoran simpanan, angsuran pokok, jasa pinjaman, penyaluran pinjaman)
 * sehingga perbandingan tetap bermakna. Bila kelak diperlukan rincian bulanan, dapat ditambahkan
 * entity anak tanpa mengubah struktur ini.
 * </p>
 *
 * <h3>Kategori yang direncanakan</h3>
 * <ul>
 * <li><b>Penerimaan</b> — {@link #getRencanaSimpanan()} (setoran simpanan anggota),
 * {@link #getRencanaAngsuranPokok()} (pengembalian pokok pinjaman), {@link #getRencanaJasaPinjaman()}
 * (jasa/bunga pinjaman), {@link #getRencanaPenerimaanLain()} (penerimaan lain-lain).</li>
 * <li><b>Pengeluaran</b> — {@link #getRencanaPenyaluran()} (pinjaman yang disalurkan),
 * {@link #getRencanaBiayaOperasional()} (biaya operasional), {@link #getRencanaPengeluaranLain()}
 * (pengeluaran lain-lain).</li>
 * </ul>
 *
 * <p>
 * Beberapa metode {@code @Transient} menyediakan turunan yang sering dipakai tampilan/laporan tanpa
 * perlu disimpan: {@link #getTotalPenerimaanRencana()}, {@link #getTotalPengeluaranRencana()},
 * {@link #getSurplusRencana()} (selisih penerimaan dan pengeluaran), dan {@link #getSaldoAkhirRencana()}
 * (saldo kas awal ditambah surplus). Semuanya dihitung ulang dari komponen sehingga selalu konsisten.
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Mengikuti pola rumah AIS agar seragam dan mudah dipelihara: kunci {@code IDENTITY}, relasi
 * {@link Koperasi} lazy dengan {@code check(...)}, hook audit {@code @PreUpdate}, {@code @Audited}
 * (anggaran adalah keputusan penting yang harus dapat ditelusuri), seluruh getter numerik aman-null
 * (mengembalikan 0.0 bila belum diisi), serta kompatibel Java 1.7. Terdaftar di
 * {@code hibernate.cfg.xml} sehingga {@code hbm2ddl=update} membuat tabel
 * <code>koperasi.anggaran_kas</code> secara otomatis. Kombinasi koperasi+tahun sebaiknya unik dan
 * dijaga di lapisan Action. Entity ini tidak menyentuh basis data secara langsung, hemat memori, dan
 * tidak mengubah perilaku entity lain.
 * </p>
 *
 * @see PembagianShu
 * @see ais.action.master.koperasi.AnggaranKasKoperasiAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "anggaran_kas")
public class AnggaranKasKoperasi extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 6620100014412771001L;

	private Long id;
	private String oleh;
	private String olehId;

	private Koperasi koperasi;
	private Integer tahun = 0;
	private Double saldoAwalKas = 0.0;

	private Double rencanaSimpanan = 0.0;
	private Double rencanaAngsuranPokok = 0.0;
	private Double rencanaJasaPinjaman = 0.0;
	private Double rencanaPenerimaanLain = 0.0;

	private Double rencanaPenyaluran = 0.0;
	private Double rencanaBiayaOperasional = 0.0;
	private Double rencanaPengeluaranLain = 0.0;

	private String keterangan;
	private Boolean aktif = true;

	public AnggaranKasKoperasi() {
	}

	public AnggaranKasKoperasi(Long id) {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		koperasi = check(koperasi);
		return koperasi;
	}

	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi == null || koperasi.getId() == null ? null : koperasi;
	}

	@Column(name = "tahun")
	public Integer getTahun() {
		return tahun == null ? 0 : tahun;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	@Column(name = "saldo_awal_kas")
	public Double getSaldoAwalKas() {
		return saldoAwalKas == null ? 0.0 : saldoAwalKas;
	}

	public void setSaldoAwalKas(Double saldoAwalKas) {
		this.saldoAwalKas = saldoAwalKas;
	}

	@Column(name = "rencana_simpanan")
	public Double getRencanaSimpanan() {
		return rencanaSimpanan == null ? 0.0 : rencanaSimpanan;
	}

	public void setRencanaSimpanan(Double rencanaSimpanan) {
		this.rencanaSimpanan = rencanaSimpanan;
	}

	@Column(name = "rencana_angsuran_pokok")
	public Double getRencanaAngsuranPokok() {
		return rencanaAngsuranPokok == null ? 0.0 : rencanaAngsuranPokok;
	}

	public void setRencanaAngsuranPokok(Double rencanaAngsuranPokok) {
		this.rencanaAngsuranPokok = rencanaAngsuranPokok;
	}

	@Column(name = "rencana_jasa_pinjaman")
	public Double getRencanaJasaPinjaman() {
		return rencanaJasaPinjaman == null ? 0.0 : rencanaJasaPinjaman;
	}

	public void setRencanaJasaPinjaman(Double rencanaJasaPinjaman) {
		this.rencanaJasaPinjaman = rencanaJasaPinjaman;
	}

	@Column(name = "rencana_penerimaan_lain")
	public Double getRencanaPenerimaanLain() {
		return rencanaPenerimaanLain == null ? 0.0 : rencanaPenerimaanLain;
	}

	public void setRencanaPenerimaanLain(Double rencanaPenerimaanLain) {
		this.rencanaPenerimaanLain = rencanaPenerimaanLain;
	}

	@Column(name = "rencana_penyaluran")
	public Double getRencanaPenyaluran() {
		return rencanaPenyaluran == null ? 0.0 : rencanaPenyaluran;
	}

	public void setRencanaPenyaluran(Double rencanaPenyaluran) {
		this.rencanaPenyaluran = rencanaPenyaluran;
	}

	@Column(name = "rencana_biaya_operasional")
	public Double getRencanaBiayaOperasional() {
		return rencanaBiayaOperasional == null ? 0.0 : rencanaBiayaOperasional;
	}

	public void setRencanaBiayaOperasional(Double rencanaBiayaOperasional) {
		this.rencanaBiayaOperasional = rencanaBiayaOperasional;
	}

	@Column(name = "rencana_pengeluaran_lain")
	public Double getRencanaPengeluaranLain() {
		return rencanaPengeluaranLain == null ? 0.0 : rencanaPengeluaranLain;
	}

	public void setRencanaPengeluaranLain(Double rencanaPengeluaranLain) {
		this.rencanaPengeluaranLain = rencanaPengeluaranLain;
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

	/** Total seluruh penerimaan kas yang direncanakan (rupiah). Tidak dipersist. */
	@javax.persistence.Transient
	public double getTotalPenerimaanRencana() {
		return getRencanaSimpanan() + getRencanaAngsuranPokok() + getRencanaJasaPinjaman()
				+ getRencanaPenerimaanLain();
	}

	/** Total seluruh pengeluaran kas yang direncanakan (rupiah). Tidak dipersist. */
	@javax.persistence.Transient
	public double getTotalPengeluaranRencana() {
		return getRencanaPenyaluran() + getRencanaBiayaOperasional() + getRencanaPengeluaranLain();
	}

	/** Surplus/defisit kas yang direncanakan = penerimaan − pengeluaran. Tidak dipersist. */
	@javax.persistence.Transient
	public double getSurplusRencana() {
		return getTotalPenerimaanRencana() - getTotalPengeluaranRencana();
	}

	/** Perkiraan saldo kas akhir tahun = saldo awal + surplus rencana. Tidak dipersist. */
	@javax.persistence.Transient
	public double getSaldoAkhirRencana() {
		return getSaldoAwalKas() + getSurplusRencana();
	}

	@Override
	public String toString() {
		return "Anggaran Kas " + getTahun();
	}
}
