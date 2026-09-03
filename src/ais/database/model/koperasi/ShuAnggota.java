package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — fitur pembagian SHU.

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

/**
 * <h2>ShuAnggota — Rincian Bagian SHU per Anggota</h2>
 *
 * <p>
 * Entity anak dari {@link PembagianShu} ini menyimpan <b>hak SHU setiap anggota</b> pada satu tahun
 * buku. Satu baris pada tabel <code>koperasi.shu_anggota</code> memuat dasar perhitungan (total
 * simpanan sebagai basis jasa modal dan total partisipasi/jasa sebagai basis jasa usaha), nilai
 * jasa modal, nilai jasa usaha, total SHU anggota, serta status pembayarannya.
 * </p>
 *
 * <h3>Prinsip perhitungan (adil &amp; sebanding)</h3>
 * <p>
 * Sesuai UU Perkoperasian dan SOM USPK, SHU dibagikan secara adil sebanding dengan jasa usaha dan
 * jasa modal masing-masing anggota. Bagian anggota dihitung proporsional oleh sistem:
 * <em>jasa modal anggota = (simpanan anggota / total simpanan seluruh anggota) × nominal jasa
 * modal</em>, dan <em>jasa usaha anggota = (partisipasi anggota / total partisipasi seluruh anggota)
 * × nominal jasa usaha</em>. Basis {@link #getTotalSimpanan()} dan {@link #getTotalTransaksi()}
 * disimpan agar perhitungan dapat diaudit ulang kapan pun.
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * <b>Tidak {@code @Audited}</b>: baris rincian bervolume besar (satu per anggota per tahun) dan
 * sudah tertaut ke {@link PembagianShu} yang teraudit; menghindari tabel <code>_aud</code>
 * menghemat ruang. Relasi lazy dengan {@code check(...)}, getter numerik null-safe, kompatibel
 * Java 1.7.
 * </p>
 *
 * @see PembagianShu
 * @see AnggotaKoperasi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "shu_anggota")
public class ShuAnggota extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 7742100014412002002L;

	/** Primary key, IDENTITY dari kolom {@code id}. */
	private Long id;
	/** Nama pengguna (username) pembuat/pengubah terakhir baris ini, untuk audit ringan. */
	private String oleh;
	/** Id pengguna pembuat/pengubah terakhir baris ini, pasangan {@link #oleh}. */
	private String olehId;

	/** Kepala pembagian SHU tahun buku yang menaungi rincian ini. */
	private PembagianShu pembagianShu;
	/** Anggota koperasi pemilik hak SHU pada baris ini. */
	private AnggotaKoperasi anggota;
	private Double totalSimpanan = 0.0;
	private Double totalTransaksi = 0.0;
	/** Nilai bagian jasa modal anggota (rupiah), sebanding {@link #totalSimpanan} terhadap total simpanan seluruh anggota. */
	private Double jasaModal = 0.0;
	/** Nilai bagian jasa usaha anggota (rupiah), sebanding {@link #totalTransaksi} terhadap total partisipasi seluruh anggota. */
	private Double jasaUsaha = 0.0;
	/** Total hak SHU anggota ({@link #jasaModal} + {@link #jasaUsaha}). */
	private Double totalShu = 0.0;
	/**
	 * Status pembayaran baris ini ke anggota. <b>Catatan arsitektur:</b> penelusuran seluruh
	 * codebase tidak menemukan satu pun titik panggil {@code setSudahDibayar(...)} selain
	 * deklarasi setter-nya sendiri di bawah — field ini selalu bernilai default {@code false} pada
	 * data mana pun, karena tidak ada alur pencairan/pembayaran SHU per anggota yang benar-benar
	 * mengubahnya. Field ini beserta {@link #tanggalBayar} sejauh ini adalah tempat penampung
	 * (placeholder) status pencairan yang belum dihubungkan ke proses nyata, bukan indikasi bahwa
	 * SHU benar-benar sudah/belum dibayarkan secara operasional.
	 */
	private Boolean sudahDibayar = false;
	/** Tanggal SHU baris ini dibayarkan ke anggota; lihat catatan pada {@link #sudahDibayar} — tidak pernah di-set oleh kode manapun saat ini. */
	private Date tanggalBayar;

	/** Konstruktor kosong, dipakai Hibernate dan saat membangun baris rincian baru sebelum diisi. */
	public ShuAnggota() {
	}

	/**
	 * Konstruktor pintasan untuk merujuk sebuah baris rincian yang sudah ada hanya lewat id-nya.
	 *
	 * @param id primary key baris rincian yang sudah ada
	 */
	public ShuAnggota(Long id) {
		this.id = id;
	}

	/** @return primary key baris rincian ini, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; kolom {@code insertable = false} sehingga id sesungguhnya berasal dari IDENTITY database. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return id pengguna pembuat/pengubah terakhir baris ini, atau {@code null} bila belum
	 *         pernah di-set.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Set id pengguna pembuat/pengubah. Nilai kosong/hanya-spasi diabaikan (tidak menimpa nilai
	 * lama) agar audit tidak pernah kehilangan jejak pengguna karena panggilan kosong yang tidak
	 * disengaja.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Set nama pengguna pembuat/pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * kosong/hanya-spasi diabaikan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna pembuat/pengubah terakhir baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Hibernate yang dipanggil otomatis sebelum setiap {@code UPDATE}. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} (dan field audit sejenis) tanpa perlu campur tangan kode pemanggil.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu terakhir baris ini diubah; default saat objek dibuat, diperbarui oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang hendak diset secara manual. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu terakhir baris ini diubah. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return kepala pembagian SHU tahun buku yang menaungi rincian ini, dimuat lazy lewat
	 *         {@code check(...)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembagian_shu", nullable = true)
	public PembagianShu getPembagianShu() {
		pembagianShu = check(pembagianShu);
		return pembagianShu;
	}

	/**
	 * Set kepala pembagian SHU. Ditolak diam-diam (field tetap {@code null}) bila
	 * {@code pembagianShu} bernilai {@code null} atau belum memiliki id (belum tersimpan) — mencegah
	 * baris rincian tertaut ke header yang belum valid di database.
	 *
	 * @param pembagianShu kepala pembagian SHU yang menaungi rincian ini; diabaikan (diset
	 *                      {@code null}) bila belum memiliki id
	 */
	public void setPembagianShu(PembagianShu pembagianShu) {
		this.pembagianShu = pembagianShu == null || pembagianShu.getId() == null ? null : pembagianShu;
	}

	/** @return anggota koperasi pemilik hak SHU pada baris ini, dimuat lazy lewat {@code check(...)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota", nullable = true)
	public AnggotaKoperasi getAnggota() {
		anggota = check(anggota);
		return anggota;
	}

	/**
	 * Set anggota pemilik baris rincian ini. Sama seperti {@link #setPembagianShu(PembagianShu)},
	 * ditolak diam-diam bila {@code anggota} bernilai {@code null} atau belum memiliki id.
	 *
	 * @param anggota anggota koperasi pemilik hak SHU pada baris ini; diabaikan (diset {@code null})
	 *                bila belum memiliki id
	 */
	public void setAnggota(AnggotaKoperasi anggota) {
		this.anggota = anggota == null || anggota.getId() == null ? null : anggota;
	}

	/**
	 * @return total simpanan anggota — basis proporsi jasa modal, sesuai rumus <em>jasa modal
	 *         anggota = (simpanan anggota / total simpanan seluruh anggota) &times; nominal jasa
	 *         modal</em> yang dijelaskan di Javadoc kelas. Tidak pernah {@code null} ({@code 0.0}
	 *         sebagai fallback).
	 */
	@Column(name = "total_simpanan")
	public Double getTotalSimpanan() {
		return totalSimpanan == null ? 0.0 : totalSimpanan;
	}

	/** @param totalSimpanan total simpanan anggota, basis proporsi jasa modal. */
	public void setTotalSimpanan(Double totalSimpanan) {
		this.totalSimpanan = totalSimpanan;
	}

	/**
	 * @return total partisipasi/jasa anggota (mis. margin/bunga pinjaman aktif yang dibayar) —
	 *         basis proporsi jasa usaha, sesuai rumus <em>jasa usaha anggota = (partisipasi
	 *         anggota / total partisipasi seluruh anggota) &times; nominal jasa usaha</em> yang
	 *         dijelaskan di Javadoc kelas. Tidak pernah {@code null} ({@code 0.0} sebagai fallback).
	 */
	@Column(name = "total_transaksi")
	public Double getTotalTransaksi() {
		return totalTransaksi == null ? 0.0 : totalTransaksi;
	}

	/** @param totalTransaksi total partisipasi/jasa anggota, basis proporsi jasa usaha. */
	public void setTotalTransaksi(Double totalTransaksi) {
		this.totalTransaksi = totalTransaksi;
	}

	/**
	 * <h3>Nilai bagian jasa modal anggota (rupiah)</h3>
	 *
	 * <p>
	 * Field ini adalah <b>hasil kalkulasi tersimpan</b>, bukan nilai transient yang dihitung ulang
	 * tiap kali dibaca — sumber perhitungannya berada di
	 * {@code ais.action.master.koperasi.util.PembagianShuHelper#hitungDanSimpan(Session, int, Parameter)},
	 * satu-satunya tempat di codebase yang memanggil {@link #setJasaModal(Double)}. Berikut rumus
	 * dan alur lengkapnya, penting untuk memahami keandalan angka yang disimpan di kolom ini:
	 * </p>
	 *
	 * <h4>Rumus</h4>
	 * <p>
	 * <code>jasaModal(anggota) = simpanan(anggota) / totalSimpananSeluruhAnggota &times; nominalJasaModal</code>
	 * </p>
	 * <p>
	 * di mana {@code nominalJasaModal = totalShu(header) &times; persenJasaModal / 100} (lihat
	 * {@link PembagianShu#getNominalJasaModal()}), dan {@code simpanan(anggota)} adalah jumlah
	 * seluruh {@code TransaksiKoperasi} anggota tersebut yang produknya bertipe SIMPANAN
	 * ({@code ConstantValues.SIMPANAN}), dijumlahkan dari {@code TransaksiKoperasi#getNilai()}
	 * tanpa filter status aktif/tidak aktif. Bila {@code totalSimpananSeluruhAnggota} bernilai 0
	 * (tidak ada satu pun transaksi simpanan tercatat di koperasi), {@code jasaModal} untuk semua
	 * anggota jatuh ke 0 (dibagi-nol dihindari lewat pengecekan eksplisit di helper, bukan
	 * dibiarkan menghasilkan {@code NaN}/{@code Infinity}).
	 * </p>
	 *
	 * <h4>Alur penyimpanan &amp; risiko pembagian ganda (double-payout)</h4>
	 * <p>
	 * Setiap kali {@code hitungDanSimpan} dipanggil untuk tahun buku yang sama, helper tersebut
	 * <b>menghapus seluruh baris {@code ShuAnggota} lama</b> untuk {@link PembagianShu} tahun itu
	 * ({@code delete from ShuAnggota where pembagianShu.id = :id}) sebelum menyisipkan baris baru
	 * hasil hitungan ulang — jadi <b>tidak mungkin ada dua baris {@code ShuAnggota} yang bertumpuk
	 * untuk kombinasi (anggota, tahun buku) yang sama</b>; menghitung ulang bersifat menggantikan,
	 * bukan menambah, sehingga risiko "duplikasi baris" di tabel ini sendiri tertutup oleh desain
	 * hapus-lalu-buat-ulang tersebut. {@link PembagianShu#getStatus()} juga langsung diset ke
	 * {@link PembagianShu#STATUS_DIBAGIKAN} begitu {@code hitungDanSimpan} selesai — bukan hanya
	 * saat {@link PembagianShu#STATUS_DISAHKAN}, sehingga status "DIBAGIKAN" pada header tidak
	 * membedakan antara "baru dihitung sistem" dan "benar-benar sudah dicairkan ke rekening
	 * anggota".
	 * </p>
	 * <p>
	 * <b>Namun demikian, penjagaan itu ada di level rekalkulasi, bukan di level pencairan uang
	 * nyata ke anggota.</b> Pencairan riil ke saldo/rekening anggota (mis. lewat
	 * {@link PembayaranAnggotaKoperasi} sebagai topup deposit) TIDAK ditemukan tertaut ke
	 * {@code ShuAnggota} di mana pun dalam codebase saat ini — tidak ada Action/util yang membaca
	 * baris {@code ShuAnggota} lalu membuat {@code PembayaranAnggotaKoperasi} atau entri jurnal
	 * setara untuk anggota tersebut. Dengan kata lain, fitur pembagian SHU per tahun ini
	 * (setidaknya per paket yang didokumentasikan di sini) baru mencakup <b>kalkulasi dan
	 * pencatatan hak</b> per anggota, bukan <b>eksekusi pencairan</b>-nya — {@link #sudahDibayar}
	 * dan {@link #tanggalBayar} adalah placeholder untuk proses pencairan itu (lihat catatan pada
	 * field {@link #sudahDibayar}), yang berarti risiko "SHU dicairkan dua kali" belum relevan
	 * karena mekanisme pencairannya sendiri belum ada — bukan berarti sudah aman selamanya:
	 * begitu proses pencairan riil ditambahkan di masa depan, penjaga anti-pencairan-ganda per
	 * baris (mis. mengunci baris yang {@code sudahDibayar == true} agar tidak diproses lagi) perlu
	 * dipastikan ada sejak awal implementasi tersebut.
	 * </p>
	 *
	 * @return nilai bagian jasa modal anggota; tidak pernah {@code null} ({@code 0.0} sebagai
	 *         fallback)
	 */
	@Column(name = "jasa_modal")
	public Double getJasaModal() {
		return jasaModal == null ? 0.0 : jasaModal;
	}

	/** @param jasaModal nilai bagian jasa modal anggota (rupiah); lihat rumus lengkap pada {@link #getJasaModal()}. */
	public void setJasaModal(Double jasaModal) {
		this.jasaModal = jasaModal;
	}

	/**
	 * <p>
	 * Nilai bagian jasa usaha anggota (rupiah), pasangan {@link #getJasaModal()}. Rumusnya:
	 * </p>
	 * <p>
	 * <code>jasaUsaha(anggota) = partisipasi(anggota) / totalPartisipasiSeluruhAnggota &times; nominalJasaUsaha</code>
	 * </p>
	 * <p>
	 * di mana {@code nominalJasaUsaha = totalShu(header) &times; persenJasaUsaha / 100} (lihat
	 * {@link PembagianShu#getNominalJasaUsaha()}), dan {@code partisipasi(anggota)} adalah jumlah
	 * {@code TransaksiKoperasi#getMargin()} dari seluruh transaksi anggota bertipe PINJAMAN
	 * ({@code ConstantValues.PINJAMAN}) yang <b>masih aktif</b> ({@code getAktif() == true}) —
	 * berbeda dari basis jasa modal yang tidak memfilter status aktif, basis jasa usaha secara
	 * eksplisit hanya menghitung margin dari pinjaman yang belum lunas/dihapus. Sama seperti
	 * {@link #getJasaModal()}, pembagian-nol dihindari eksplisit di helper perhitungan, dan seluruh
	 * catatan mengenai penggantian-bukan-penumpukan saat rekalkulasi serta ketiadaan mekanisme
	 * pencairan riil pada {@link #getJasaModal()} berlaku sama persis di sini.
	 * </p>
	 *
	 * @return nilai bagian jasa usaha anggota; tidak pernah {@code null} ({@code 0.0} sebagai
	 *         fallback)
	 */
	@Column(name = "jasa_usaha")
	public Double getJasaUsaha() {
		return jasaUsaha == null ? 0.0 : jasaUsaha;
	}

	/** @param jasaUsaha nilai bagian jasa usaha anggota (rupiah); lihat rumus lengkap pada {@link #getJasaUsaha()}. */
	public void setJasaUsaha(Double jasaUsaha) {
		this.jasaUsaha = jasaUsaha;
	}

	/**
	 * @return total hak SHU anggota, yaitu {@link #getJasaModal()} + {@link #getJasaUsaha()}
	 *         (dijumlahkan sekali oleh helper perhitungan saat menyimpan, bukan getter transient di
	 *         sini — memanggil {@link #setJasaModal(Double)}/{@link #setJasaUsaha(Double)} tanpa
	 *         memanggil ulang {@link #setTotalShu(Double)} TIDAK akan menyinkronkan ulang nilai
	 *         ini secara otomatis, karena kelas ini bukan {@code @Transient}). Tidak pernah
	 *         {@code null} ({@code 0.0} sebagai fallback).
	 */
	@Column(name = "total_shu")
	public Double getTotalShu() {
		return totalShu == null ? 0.0 : totalShu;
	}

	/** @param totalShu total hak SHU anggota; idealnya selalu dijaga tetap sama dengan jasaModal + jasaUsaha. */
	public void setTotalShu(Double totalShu) {
		this.totalShu = totalShu;
	}

	/**
	 * @return status pencairan SHU baris ini ke anggota; tidak pernah {@code null}
	 *         ({@code false} sebagai fallback). Lihat catatan penting pada field
	 *         {@link #sudahDibayar} — flag ini saat ini tidak pernah di-set {@code true} oleh kode
	 *         mana pun di codebase, sehingga secara praktis selalu tampil "Belum" pada laporan mana
	 *         pun yang membacanya.
	 */
	@Column(name = "sudah_dibayar")
	public Boolean getSudahDibayar() {
		return sudahDibayar == null ? false : sudahDibayar;
	}

	/**
	 * @param sudahDibayar status pencairan yang hendak diset. Perhatikan bahwa tidak ada satu pun
	 *                     pemanggil setter ini di codebase saat ini (lihat {@link #sudahDibayar}) —
	 *                     mengisi lewat setter ini (mis. dari layar/Action baru) tidak akan
	 *                     berkonflik dengan proses lain, tapi juga berarti belum ada validasi
	 *                     bawaan yang mencegah baris yang sama ditandai dibayar berkali-kali; bila
	 *                     hendak dipakai untuk alur pencairan riil, tambahkan penjagaan idempoten
	 *                     di lapisan Action pemanggilnya.
	 */
	public void setSudahDibayar(Boolean sudahDibayar) {
		this.sudahDibayar = sudahDibayar;
	}

	/** @return tanggal SHU baris ini dibayarkan ke anggota, atau {@code null} bila belum dibayar/tidak diisi. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_bayar")
	public Date getTanggalBayar() {
		return tanggalBayar;
	}

	/** @param tanggalBayar tanggal SHU baris ini dibayarkan ke anggota. */
	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	/**
	 * @return representasi ringkas "nama anggota - SHU total" untuk debug/log/tampilan sederhana.
	 *         Kegagalan mengambil nama anggota (mis. relasi lazy gagal dimuat) ditangkap dan
	 *         dicatat lewat {@link ais.common.ErrorAuditUtil#record(Exception, String)}, jatuh ke
	 *         string kosong untuk bagian nama agar method ini tidak pernah melempar exception.
	 */
	@Override
	public String toString() {
		String namaAnggota = "";
		try {
			namaAnggota = getAnggota() == null ? "" : getAnggota().getNama();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/ShuAnggota.java:222");
		}
		return namaAnggota + " - SHU " + getTotalShu();
	}
}
