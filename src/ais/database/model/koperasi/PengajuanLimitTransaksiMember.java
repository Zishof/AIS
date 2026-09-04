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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Persetujuan satu-kali untuk transaksi member yang melampaui limit periodik
 * Tipe Member. Kode transaksi mengikat persetujuan ke satu checkout sehingga
 * keputusan tidak dapat dipakai untuk transaksi lain.
 */
@Entity
@Audited
@Table(schema = "koperasi", name = "pengajuan_limit_transaksi_member")
public class PengajuanLimitTransaksiMember extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/**
	 * Pengajuan baru dibuat oleh kasir dan sedang menunggu keputusan petugas berwenang
	 * (role dengan {@code Tbmrole.getBolehVerifikasiMemberMelebihiLimit() == true}). Status awal
	 * (bawaan field {@link #status}) setiap baris baru.
	 */
	public static final String MENUNGGU = "MENUNGGU";

	/**
	 * Petugas berwenang menyetujui pengajuan: transaksi dengan {@link #getKodeTransaksi()} yang
	 * sama boleh dikirim ulang dan akan lolos gerbang limit satu kali. Tidak mengubah limit
	 * periodik {@link TipeAnggotaKoperasi} anggota -- lihat catatan kelas.
	 */
	public static final String DISETUJUI = "DISETUJUI";

	/**
	 * Petugas berwenang menolak pengajuan. Kode transaksi yang sama tidak dapat diajukan ulang;
	 * kasir harus membuat kode transaksi baru bila ingin mencoba lagi (lihat
	 * {@code PengajuanLimitMemberApiHelper.periksaAtauAjukan}).
	 */
	public static final String DITOLAK = "DITOLAK";

	/**
	 * Persetujuan sudah terpakai oleh checkout yang berhasil ({@link #getPembelianAnggotaKoperasi()}
	 * terisi, {@link #getTanggalDipakai()} tercatat). Status akhir; sekali dipakai, persetujuan ini
	 * tidak dapat dipakai ulang untuk transaksi lain walau kode transaksinya sama.
	 */
	public static final String DIPAKAI = "DIPAKAI";

	private Long id;
	private AnggotaKoperasi anggotaKoperasi;
	private TipeAnggotaKoperasi tipeAnggotaKoperasi;
	private String kodeTransaksi;
	private Double nominalTransaksi;
	private String periodeLimit;
	private Double limitTransaksi;
	private Double pemakaianBerjalan;
	private String status = MENUNGGU;
	private Tbmuser diajukanOleh;
	private Tbmuser diputuskanOleh;
	private Date tanggalPengajuan = ais.ui.util.WaktuUtil.getDate();
	private Date tanggalKeputusan;
	private Date tanggalDipakai;
	private PembelianAnggotaKoperasi pembelianAnggotaKoperasi;
	private String catatan;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @return id baris (identity, dibuat DB). */
	@Id
	@GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id baris; biasanya tidak diset manual, dibuat DB saat {@code save}. */
	public void setId(Long id) { this.id = id; }

	/** @return anggota koperasi yang melakukan transaksi ini. Wajib diisi ({@code nullable = false}). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = false)
	public AnggotaKoperasi getAnggotaKoperasi() { anggotaKoperasi = check(anggotaKoperasi); return anggotaKoperasi; }
	/** @param value anggota koperasi yang mengajukan (pemilik transaksi). */
	public void setAnggotaKoperasi(AnggotaKoperasi value) { this.anggotaKoperasi = value; }

	/**
	 * @return tipe anggota (kelas limit) anggota <b>pada saat pengajuan dibuat</b> --
	 *         salinan referensi untuk audit historis, bukan sumber limit yang dipakai untuk
	 *         mengevaluasi transaksi lain. Bila tipe anggota berubah setelahnya, baris ini tetap
	 *         menunjuk tipe lama.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_anggota_koperasi")
	public TipeAnggotaKoperasi getTipeAnggotaKoperasi() { tipeAnggotaKoperasi = check(tipeAnggotaKoperasi); return tipeAnggotaKoperasi; }
	/** @param value tipe anggota koperasi pada saat pengajuan dibuat. */
	public void setTipeAnggotaKoperasi(TipeAnggotaKoperasi value) { this.tipeAnggotaKoperasi = value; }

	/**
	 * @return kode unik yang mengikat pengajuan ke satu transaksi checkout kasir (kolom
	 *         {@code unique = true}). Kode yang sama dipakai ulang oleh kasir untuk mengirim ulang
	 *         checkout setelah pengajuan disetujui -- inilah mekanisme "sekali pakai" persetujuan:
	 *         tanpa kode transaksi yang cocok persis, persetujuan tidak berlaku untuk transaksi lain.
	 */
	@Column(name = "kode_transaksi", nullable = false, unique = true, length = 100)
	public String getKodeTransaksi() { return kodeTransaksi; }
	/** @param value kode transaksi unik dari checkout kasir. */
	public void setKodeTransaksi(String value) { this.kodeTransaksi = value; }

	/** @return nominal transaksi yang diajukan (dicek presisi longgar 0.5 saat retry checkout). */
	@Column(name = "nominal_transaksi", nullable = false)
	public Double getNominalTransaksi() { return nominalTransaksi; }
	/** @param value nominal transaksi yang diajukan. */
	public void setNominalTransaksi(Double value) { this.nominalTransaksi = value; }

	/** @return kode periode limit yang terlampaui (mis. HARIAN/MINGGUAN/BULANAN), huruf besar. */
	@Column(name = "periode_limit", nullable = false, length = 20)
	public String getPeriodeLimit() { return periodeLimit; }
	/** @param value kode periode limit yang terlampaui. */
	public void setPeriodeLimit(String value) { this.periodeLimit = value; }

	/**
	 * @return nilai batas limit periodik ({@link TipeAnggotaKoperasi}) pada saat pengajuan dibuat --
	 *         salinan untuk audit/tampilan, bukan pointer hidup ke konfigurasi tipe anggota.
	 */
	@Column(name = "limit_transaksi", nullable = false)
	public Double getLimitTransaksi() { return limitTransaksi; }
	/** @param value nilai batas limit periodik pada saat pengajuan dibuat. */
	public void setLimitTransaksi(Double value) { this.limitTransaksi = value; }

	/** @return total pemakaian anggota pada periode berjalan sebelum transaksi ini, sebagai konteks audit. */
	@Column(name = "pemakaian_berjalan", nullable = false)
	public Double getPemakaianBerjalan() { return pemakaianBerjalan; }
	/** @param value total pemakaian berjalan pada periode limit terkait. */
	public void setPemakaianBerjalan(Double value) { this.pemakaianBerjalan = value; }

	/**
	 * @return status alur persetujuan saat ini: {@link #MENUNGGU}, {@link #DISETUJUI},
	 *         {@link #DITOLAK}, atau {@link #DIPAKAI}. Bawaan {@link #MENUNGGU}.
	 */
	@Column(name = "status", nullable = false, length = 20)
	public String getStatus() { return status; }
	/** @param value status baru; gunakan salah satu konstanta status kelas ini. */
	public void setStatus(String value) { this.status = value; }

	/** @return kasir/petugas yang membuat pengajuan (pengguna yang sedang login saat checkout ditolak gerbang limit). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh")
	public Tbmuser getDiajukanOleh() { diajukanOleh = check(diajukanOleh); return diajukanOleh; }
	/** @param value pengguna yang mengajukan (kasir). */
	public void setDiajukanOleh(Tbmuser value) { this.diajukanOleh = value; }

	/**
	 * @return petugas berwenang yang memutuskan (menyetujui/menolak). Terisi hanya setelah status
	 *         beranjak dari {@link #MENUNGGU}. Otorisasi diperiksa dari peran ({@code
	 *         Tbmrole.getBolehVerifikasiMemberMelebihiLimit()}), bukan dari kepemilikan pengajuan --
	 *         petugas berwenang boleh memutuskan pengajuan yang ia ajukan sendiri (tidak ada
	 *         pemeriksaan silang {@link #diajukanOleh} vs pemutus di gerbang keputusan; pola yang
	 *         sama dengan gerbang persetujuan lain di AIS yang belum ditambal untuk self-approval).
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "diputuskan_oleh")
	public Tbmuser getDiputuskanOleh() { diputuskanOleh = check(diputuskanOleh); return diputuskanOleh; }
	/** @param value petugas yang memutuskan pengajuan. */
	public void setDiputuskanOleh(Tbmuser value) { this.diputuskanOleh = value; }

	/** @return waktu pengajuan dibuat. Bawaan objek: {@code WaktuUtil.getDate()} saat instansiasi. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pengajuan", nullable = false)
	public Date getTanggalPengajuan() { return tanggalPengajuan; }
	/** @param value waktu pengajuan dibuat. */
	public void setTanggalPengajuan(Date value) { this.tanggalPengajuan = value; }

	/** @return waktu petugas memutuskan (setuju/tolak); {@code null} selama masih {@link #MENUNGGU}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_keputusan")
	public Date getTanggalKeputusan() { return tanggalKeputusan; }
	/** @param value waktu keputusan dicatat. */
	public void setTanggalKeputusan(Date value) { this.tanggalKeputusan = value; }

	/** @return waktu persetujuan benar-benar terpakai oleh checkout yang berhasil; {@code null} bila belum dipakai. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dipakai")
	public Date getTanggalDipakai() { return tanggalDipakai; }
	/** @param value waktu persetujuan dipakai oleh transaksi final. */
	public void setTanggalDipakai(Date value) { this.tanggalDipakai = value; }

	/**
	 * @return transaksi pembelian final yang mengunci pemakaian persetujuan ini. Diisi oleh
	 *         {@code PengajuanLimitMemberApiHelper.tandaiDipakai} setelah checkout sukses; sebelum
	 *         itu {@code null}. Ini yang membuat status {@link #DIPAKAI} tidak dapat dipakai ulang
	 *         untuk transaksi lain -- baris sudah terikat ke satu pembelian.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pembelian_anggota_koperasi")
	public PembelianAnggotaKoperasi getPembelianAnggotaKoperasi() { pembelianAnggotaKoperasi = check(pembelianAnggotaKoperasi); return pembelianAnggotaKoperasi; }
	/** @param value pembelian final yang mengunci pemakaian persetujuan ini. */
	public void setPembelianAnggotaKoperasi(PembelianAnggotaKoperasi value) { this.pembelianAnggotaKoperasi = value; }

	/** @return catatan bebas dari petugas yang memutuskan (mis. alasan tolak/setuju). */
	@Column(name = "catatan", length = 1000)
	public String getCatatan() { return catatan; }
	/** @param value catatan keputusan. */
	public void setCatatan(String value) { this.catatan = value; }

	/** @return waktu baris terakhir diubah; diperbarui otomatis lewat {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** @param value waktu perubahan terakhir (biasanya tidak diset manual). */
	public void setTanggal_dirubah(Date value) { this.tanggal_dirubah = value; }

	/**
	 * Hook JPA {@code @PreUpdate}: mencatat ulang {@link #tanggal_dirubah} setiap kali baris ini
	 * di-{@code UPDATE}, terlepas dari field mana yang berubah (pengajuan dibuat, disetujui/
	 * ditolak, atau ditandai dipakai semuanya lewat jalur {@code UPDATE} yang sama). Dipanggil
	 * otomatis oleh provider JPA, bukan untuk dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	}
}
