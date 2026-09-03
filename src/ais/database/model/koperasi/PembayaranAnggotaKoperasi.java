package ais.database.model.koperasi;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BankHost;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.bni.BniRequest;
import ais.database.model.bri.BriRequest;
import ais.database.model.bsi.BsiRequest;
import ais.ui.util.MyCheckboxConfig;

/**
 * <h2>PembayaranAnggotaKoperasi — Header Pembayaran Anggota Koperasi</h2>
 *
 * <p>
 * Entity ini adalah kepala (header) satu transaksi pembayaran yang dilakukan seorang
 * {@link AnggotaKoperasi} kepada koperasinya. Satu baris di sini bisa mewakili dua jenis peristiwa
 * uang yang berbeda, dibedakan dari kombinasi field yang terisi:
 * </p>
 * <ul>
 * <li><b>Pelunasan cicilan/angsuran</b> — anggota membayar satu atau beberapa rincian tagihan
 * ({@code TransaksiKoperasiDetail}, mis. pokok + margin pinjaman) yang dipilih lewat grid ZK.
 * Rinciannya tersimpan sebagai baris anak {@link PembayaranAnggotaKoperasiDetail}, dibuat oleh
 * {@link #saveDetail(Rows, Session)}. {@link #getNominal()} adalah total tagihan yang dibayar.</li>
 * <li><b>Topup saldo/deposit anggota</b> — anggota (atau sistem, lewat Virtual Account bank)
 * menambah saldo yang tersimpan di koperasi. Field {@link #getTambahanDeposit()},
 * {@link #getTotalDeposit()}, dan {@link #getSisaDeposit()} yang berperan; {@link #getBriRequest()},
 * {@link #getBniRequest()}, {@link #getBsiRequest()}, dan {@link #getVirtualAccountBank()} menyimpan
 * jejak permintaan pembayaran ke bank penyedia Virtual Account.</li>
 * </ul>
 *
 * <h3>Jurnal &amp; posting</h3>
 * <p>
 * Sejak dok 61 butir B, dokumen ini dapat dijurnal oleh mesin {@code PostingDanaAnggotaUtil}:
 * topup saldo dijurnal Debit akun kas/bank cara pembayaran — Kredit akun kewajiban saldo anggota;
 * pencairan diskon (status BERHASIL) dijurnal sebaliknya. {@link #getPostingHistory()} terisi begitu
 * mesin tersebut memprosesnya, mencegah dokumen yang sama dijurnal dua kali. Sebelum perubahan ini,
 * seluruh perputaran dana anggota bergerak tanpa menyentuh buku besar sama sekali.
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Beberapa getter di sini <b>menimpa nilai yang di-set sebelumnya</b> (pola getter destruktif yang
 * berulang di domain finansial AIS): {@link #getNama()} selalu menghitung ulang dari
 * {@link #getAnggotaKoperasi()} + nominal, dan {@link #getTanggalBayar()} selalu mengembalikan
 * {@link #getTanggal()} — sehingga {@code setNama(String)} dan {@code setTanggalBayar(Date)} pada
 * praktiknya tidak berpengaruh ke nilai yang dibaca kembali. {@link #setAnggotaKoperasi(AnggotaKoperasi)}
 * sebaliknya bersifat "sekali isi": begitu anggota tersimpan dengan id, pemanggilan berikutnya
 * diabaikan agar kepemilikan dokumen tidak bisa ditukar setelah dibuat.
 * </p>
 *
 * @see PembayaranAnggotaKoperasiDetail
 * @see AnggotaKoperasi
 * @see Koperasi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "pembayaran_anggota_koperasi", schema = "koperasi")
public class PembayaranAnggotaKoperasi extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas ini; dinaikkan hanya bila struktur field yang mempengaruhi
	 * kompatibilitas serialisasi berubah.
	 */
	private static final long serialVersionUID = -4008239631951156828L;
	/** Primary key, IDENTITY dari kolom {@code id}. */
	private Long id;
	/** Nama pengguna (username) pembuat/pengubah terakhir baris ini, untuk audit ringan. */
	private String oleh;
	/** Id pengguna pembuat/pengubah terakhir baris ini, pasangan {@link #oleh}. */
	private String olehId;

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
	 * {@link #tanggal_dirubah} (dan field audit sejenis) tanpa perlu campur tangan kode pemanggil —
	 * memastikan jejak "kapan terakhir diubah" konsisten di seluruh entity yang memakai pola ini.
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

	/** Nama tampilan dokumen; lihat catatan destruktif pada {@link #getNama()}. */
	private String nama;

	/** Bank host penyedia layanan pembayaran (mis. untuk resolusi Virtual Account). */
	private BankHost bankHost;
	/** Cara/metode pembayaran koperasi (kas manual, transfer, VA, dsb.) yang dipakai. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi;
	/** Koperasi pemilik dokumen; biasanya diturunkan dari {@link #anggotaKoperasi}. */
	private Koperasi koperasi;
	/** Anggota koperasi yang membayar. Sekali terisi dengan id, tidak bisa diganti lagi. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Nomor/kode inquiry pembayaran dari sistem pihak ketiga (bank/VA), bila ada. */
	private String inquiryPembayaran;
	/** Nominal total dokumen ini (total tagihan yang dibayar, atau total topup). */
	private Double nominal;
	/** Sisa saldo deposit anggota setelah transaksi ini diproses. */
	private Double sisaDeposit;
	/** Tanggal transaksi; lihat catatan self-healing pada {@link #getTanggal()}. */
	private Date tanggal;
	/** Tanggal bayar tersimpan; lihat catatan destruktif pada {@link #getTanggalBayar()}. */
	private Date tanggalBayar;
	/** Nominal tambahan yang menambah saldo deposit anggota pada transaksi topup. */
	private Double tambahanDeposit;
	/** Total saldo deposit anggota setelah ditambah {@link #tambahanDeposit}. */
	private Double totalDeposit;

	/** Jejak permintaan pembayaran ke BRI (Virtual Account/transfer), bila cara bayarnya lewat BRI. */
	private BriRequest briRequest;
	/** Jejak permintaan pembayaran ke BNI, bila cara bayarnya lewat BNI. */
	private BniRequest bniRequest;
	/** Jejak permintaan pembayaran ke BSI, bila cara bayarnya lewat BSI. */
	private BsiRequest bsiRequest;
	/** Rincian Virtual Account bank yang menerima pembayaran ini, bila topup lewat VA. */
	private VirtualAccountBank virtualAccountBank;
	/** Nama pengguna yang memvalidasi/menyetujui dokumen ini; lihat {@link #getValidator()}. */
	private String validator;
	/** Objek pengguna yang memvalidasi dokumen ini; resolusinya lihat {@link #getValidatorUser()}. */
	private Tbmuser validatorUser;
	/** Catatan/keterangan bebas untuk dokumen ini. */
	private String keterangan;

	/** Konstruktor kosong, dipakai Hibernate dan saat membangun dokumen baru sebelum diisi. */
	public PembayaranAnggotaKoperasi() {
	}

	/**
	 * Konstruktor pintasan untuk merujuk sebuah dokumen yang sudah ada hanya lewat id-nya (mis.
	 * saat membangun objek referensi ringan untuk relasi, tanpa memuat seluruh field lain dari
	 * database).
	 *
	 * @param id primary key dokumen yang sudah ada
	 */
	public PembayaranAnggotaKoperasi(Long id) {
		this.id = id;

	}

	/**
	 * <p>
	 * Menjumlahkan nominal seluruh baris tagihan ({@link TransaksiKoperasiDetail}) yang
	 * <b>dicentang</b> pengguna pada grid ZK {@link Rows}, tanpa menyimpan apa pun ke database.
	 * Dipakai layar pembayaran untuk menampilkan pratinjau total sebelum tombol simpan ditekan,
	 * sehingga pengguna tahu berapa yang akan dibayar sebelum transaksi benar-benar dibuat.
	 * </p>
	 *
	 * <p>
	 * <b>Cara kerja.</b> Setiap {@link Row} anak dari {@code rowsDetailBiaya} diharapkan membawa dua
	 * atribut context ZK: {@code "pilih"} berisi {@link MyCheckboxConfig} (status centang checkbox
	 * baris tersebut) dan {@code "transaksiKoperasiDetail"} berisi objek
	 * {@link TransaksiKoperasiDetail} (tagihan pada baris itu — cicilan/angsuran dengan komponen
	 * pokok dan margin/bunga). Untuk setiap baris yang checkbox-nya tercentang, nominal tagihan
	 * dihitung sebagai <code>pokok + margin</code> dan dijumlahkan ke {@code grandTotal}. Baris yang
	 * tidak membawa kedua atribut tersebut, atau checkbox-nya tidak tercentang, dilewati begitu
	 * saja. Setiap iterasi dibungkus try/catch individual — sebuah error pada satu baris (mis.
	 * atribut null atau cast gagal) dilaporkan lewat {@link Common#tampilErrorJikaAdmin(Exception)}
	 * dan tidak menggagalkan penjumlahan baris-baris lain.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan penting.</b> Method ini murni membaca state UI (atribut yang ditempel composer ZK
	 * pada tiap {@code Row}) — ia tidak menyentuh session Hibernate maupun database, sehingga aman
	 * dipanggil berulang kali (mis. tiap kali pengguna mencentang/melepas checkbox) tanpa efek
	 * samping. Nilai yang dikembalikan harus dianggap sekadar estimasi tampilan. Alur produksi
	 * sesungguhnya <b>tidak</b> memakai {@link #saveDetail(Rows, Session)} milik entity ini (lihat
	 * catatan pada method tersebut) — penyimpanan riil dilakukan
	 * {@code ais.common.TunaiAnggotaKoperasiCommon#onSave}, yang menghitung ulang nominal tiap baris
	 * secara independen langsung dari objek {@code TransaksiKoperasiDetail} terkelola (bukan dari
	 * atribut Row), sehingga kalaupun state UI antara pratinjau dan simpan berubah, nilai yang
	 * tersimpan ke database tetap konsisten dengan pilihan checkbox pada saat simpan.
	 * </p>
	 *
	 * @param rowsDetailBiaya grid ZK berisi baris-baris tagihan dengan atribut context
	 *                        {@code "pilih"} dan {@code "transaksiKoperasiDetail"}
	 * @return jumlah nominal (pokok + margin) seluruh baris yang tercentang; {@code 0.0} bila tidak
	 *         ada yang tercentang atau grid kosong
	 */
	@SuppressWarnings("unchecked")
	public static Double chekDetail(Rows rowsDetailBiaya) {

		Double grandTotal = 0.0;
		List<Row> rows = rowsDetailBiaya.getChildren();
		for (Object o : rows) {
			try {
				if (o instanceof Row) {
					Row row = (Row) o;

					if (row.getAttribute("pilih") != null && row.getAttribute("transaksiKoperasiDetail") != null) {
						MyCheckboxConfig checkboxConfig = (MyCheckboxConfig) row.getAttribute("pilih");
						TransaksiKoperasiDetail tagihan = (TransaksiKoperasiDetail) row
								.getAttribute("transaksiKoperasiDetail");
//						System.out.println("chekDetail pilih -> " + checkboxConfig.isChecked() + " tagihan " + tagihan);

						if (checkboxConfig != null && checkboxConfig.isChecked()) {

							Double nominal = tagihan.getPokok() + tagihan.getMargin();

							grandTotal += nominal;

						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return grandTotal;
	}

	/**
	 * <p>
	 * Dimaksudkan sebagai versi "simpan" dari {@link #chekDetail(Rows)}: membaca grid ZK yang sama,
	 * lalu untuk tiap baris tagihan yang tercentang membuat satu baris anak
	 * {@link PembayaranAnggotaKoperasiDetail} yang menautkan {@code this} (header pembayaran) ke
	 * {@link TransaksiKoperasiDetail} bersangkutan, dengan nominal <code>pokok + margin</code>.
	 * </p>
	 *
	 * <p>
	 * <b>Method ini tidak dipakai oleh alur produksi mana pun</b> — penelusuran seluruh pemanggil
	 * {@code PembayaranAnggotaKoperasi#saveDetail} di codebase tidak menemukan satu pun titik
	 * panggil; pembuatan {@link PembayaranAnggotaKoperasiDetail} yang sesungguhnya terjadi lewat
	 * {@code ais.common.TunaiAnggotaKoperasiCommon#onSave}, yang menyusun baris detail secara
	 * manual (dan menjaga agar satu {@code TransaksiKoperasiDetail} tidak dibayar dua kali lewat
	 * pengecekan {@code TransaksiKoperasiDetail#getPembayaranAnggotaKoperasiDetail() != null}
	 * sebelum menyimpan). Method ini kemungkinan sisa iterasi awal fitur yang belum sempat dibersihkan.
	 * </p>
	 *
	 * <p>
	 * <b>Dua cacat yang membuatnya tidak akan berfungsi bila suatu saat dipanggil lagi:</b>
	 * </p>
	 * <ol>
	 * <li>Guard-nya memeriksa keberadaan atribut Row {@code "tagihan"}
	 * (<code>row.getAttribute("tagihan") != null</code>), padahal composer koperasi yang membangun
	 * grid ini ({@code PembayaranKoperasiOnline}) hanya pernah menempelkan atribut bernama
	 * {@code "transaksiKoperasiDetail"} — bukan {@code "tagihan"} (pola atribut {@code "tagihan"}
	 * dipakai modul lain, mis. pembayaran siswa/SPP). Selama composer tidak diubah, guard ini selalu
	 * bernilai {@code false} dan badan blok {@code if} tidak akan pernah tereksekusi untuk satu baris
	 * pun, sehingga tidak ada {@link PembayaranAnggotaKoperasiDetail} yang tersimpan maupun
	 * {@code grandTotal} yang terisi.</li>
	 * <li>Bila guard di atas diperbaiki, {@code pembayaranAnggotaKoperasiDetail.setPembayaranAnggotaKoperasi(this)}
	 * pada baris berikutnya diam-diam tidak berefek untuk header yang belum tersimpan: setter pada
	 * {@link PembayaranAnggotaKoperasiDetail#setPembayaranAnggotaKoperasi(PembayaranAnggotaKoperasi)}
	 * menolak {@code this} bila {@code this.getId() == null}, sehingga bila method ini dipanggil
	 * sebelum header di-{@code session.save(...)}, seluruh baris detail akan tersimpan tanpa header
	 * induk (anak yatim).</li>
	 * </ol>
	 *
	 * <p>
	 * Total {@code grandTotal} yang dihitung di dalam method ini juga tidak pernah dikembalikan
	 * (method ini {@code void}) maupun dipakai — sejalan dengan status method ini yang tidak
	 * dipanggil dari mana pun.
	 * </p>
	 *
	 * @param rowsDetailBiaya grid ZK berisi baris-baris tagihan
	 * @param session         sesi Hibernate aktif tempat baris detail akan disimpan (dipanggil
	 *                        {@code save}+{@code flush} per baris, bukan dalam satu batch)
	 */
	@SuppressWarnings("unchecked")
	public void saveDetail(Rows rowsDetailBiaya, Session session) {

		Double grandTotal = 0.0;
		List<Row> rows = rowsDetailBiaya.getChildren();
		for (Object o : rows) {
			try {
				if (o instanceof Row) {
					Row row = (Row) o;

					if (row.getAttribute("pilih") != null && row.getAttribute("tagihan") != null) {
						MyCheckboxConfig checkboxConfig = (MyCheckboxConfig) row.getAttribute("pilih");
						TransaksiKoperasiDetail transaksiKoperasiDetail = (TransaksiKoperasiDetail) row
								.getAttribute("transaksiKoperasiDetail");
						System.out.println("pilih -> " + checkboxConfig.isChecked() + " transaksiKoperasiDetail "
								+ transaksiKoperasiDetail);

						if (checkboxConfig != null && checkboxConfig.isChecked()) {

							Double nominal = transaksiKoperasiDetail.getPokok() + transaksiKoperasiDetail.getMargin();

							PembayaranAnggotaKoperasiDetail pembayaranAnggotaKoperasiDetail = new PembayaranAnggotaKoperasiDetail(
									transaksiKoperasiDetail);
							pembayaranAnggotaKoperasiDetail.setTransaksiKoperasiDetail(transaksiKoperasiDetail);
							pembayaranAnggotaKoperasiDetail.setNominal(nominal);
							pembayaranAnggotaKoperasiDetail.setNominalManual(nominal);
							pembayaranAnggotaKoperasiDetail.setPembayaranAnggotaKoperasi(this);
							session.save(pembayaranAnggotaKoperasiDetail);
							session.flush();

							System.out.println("pembayaranAnggotaKoperasiDetail -> " + pembayaranAnggotaKoperasiDetail);

							grandTotal += pembayaranAnggotaKoperasiDetail.getNominal();

						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	/** @return primary key dokumen ini, atau {@code null} bila belum tersimpan. */
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
	 * <p>
	 * Nama tampilan dokumen ini, dibentuk dari gabungan representasi string
	 * {@link #getAnggotaKoperasi()} dan {@link #getNominal()} yang diformat.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif.</b> Setiap kali dipanggil, method ini <b>menimpa ulang</b> field
	 * {@code nama} dari data terkini ({@code anggotaKoperasi} + nominal saat ini) — nilai apa pun
	 * yang sebelumnya diberikan lewat {@link #setNama(String)} akan hilang begitu getter ini
	 * dipanggil sekali saja. Pola ini konsisten dipakai di banyak entity finansial AIS lain untuk
	 * memastikan label tampilan selalu sinkron dengan data terbaru, tapi berarti
	 * {@code setNama(String)} pada praktiknya bukan cara yang andal untuk mengubah nama dokumen ini.
	 * </p>
	 *
	 * @return nama tampilan yang baru dihitung ulang
	 */
	public String getNama() {
		anggotaKoperasi = getAnggotaKoperasi();
		nama = anggotaKoperasi + "-" + Common.numberFormat.get().format(getNominal());
		return nama;
	}

	/**
	 * Set nama tampilan secara manual. Lihat catatan destruktif pada {@link #getNama()} — nilai yang
	 * di-set di sini akan tertimpa pada pemanggilan {@link #getNama()} berikutnya.
	 *
	 * @param nama nama tampilan yang diinginkan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return bank host penyedia layanan pembayaran, atau {@code null} bila tidak relevan. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_host_id", nullable = true)
	public BankHost getBankHost() {
		bankHost = check(bankHost);
		return bankHost;
	}

	/** @param bankHost bank host penyedia layanan pembayaran yang dipakai dokumen ini. */
	public void setBankHost(BankHost bankHost) {
		this.bankHost = bankHost;
	}

	/**
	 * <p>
	 * Koperasi pemilik dokumen ini. Getter ini <b>tidak membaca field {@code koperasi} apa
	 * adanya</b> — setiap dipanggil, ia lebih dulu menurunkan ulang koperasi dari
	 * {@link #getAnggotaKoperasi()} (bila anggotanya ada, {@code koperasi = anggotaKoperasi.getKoperasi()}),
	 * baru kemudian jatuh ke nilai field {@code koperasi} yang di-set langsung sebagai fallback bila
	 * anggotanya tidak ada. Dengan kata lain, koperasi anggota selalu menjadi sumber kebenaran utama
	 * selama {@code anggotaKoperasi} terisi — {@link #setKoperasi(Koperasi)} hanya benar-benar
	 * berpengaruh untuk dokumen tanpa anggota (topup tanpa anggota spesifik, bila ada) atau sebelum
	 * anggotanya di-set.
	 * </p>
	 *
	 * @return koperasi pemilik dokumen, diturunkan dari anggota bila tersedia.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi_id")
	public Koperasi getKoperasi() {
		anggotaKoperasi = getAnggotaKoperasi();
		if (anggotaKoperasi != null) {
			koperasi = anggotaKoperasi.getKoperasi();
		}
		koperasi = check(koperasi);
		return this.koperasi;
	}

	/** @param koperasi koperasi pemilik dokumen; lihat catatan prioritas pada {@link #getKoperasi()}. */
	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi;
	}

	/** @return anggota koperasi yang membayar/menerima transaksi ini, dimuat lazy lewat {@code check(...)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggotaKoperasi_id", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return this.anggotaKoperasi;
	}

	/**
	 * Set anggota koperasi pemilik dokumen ini. <b>Bersifat sekali isi</b>: bila dokumen ini sudah
	 * memiliki {@code anggotaKoperasi} dengan id (artinya anggotanya sudah ditentukan/tersimpan),
	 * panggilan berikutnya diabaikan sepenuhnya — mencegah kepemilikan dokumen pembayaran ditukar ke
	 * anggota lain setelah ditetapkan, yang penting secara integritas karena dokumen ini
	 * merepresentasikan uang yang sudah/akan berpindah dari anggota tertentu.
	 *
	 * @param anggotaKoperasi anggota koperasi yang hendak diset; diabaikan bila field ini sudah
	 *                        terisi anggota dengan id
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {

		if (this.anggotaKoperasi != null && this.anggotaKoperasi.getId() != null) {
			return;
		}

		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Bangun kode periode "tahun-bulan" (format {@code YYYYMM}, mis. 202609 untuk September 2026)
	 * untuk <b>bulan berjalan berikutnya</b> relatif ke tanggal sistem saat ini — dipakai sebagai
	 * penanda periode iuran/simpanan wajib bulanan yang sedang berjalan. Perhatikan bahwa
	 * {@code bulanCurrent} dari {@link Calendar#MONTH} bersifat 0-based (Januari = 0), lalu
	 * ditambah 1 sebelum dikonversi — sehingga hasilnya adalah bulan kalender 1-based sekarang,
	 * bukan bulan berikutnya secara harfiah (nama variabel {@code bulanCurrentPlus} sedikit
	 * menyesatkan; ini murni konversi 0-based ke 1-based, bukan advance ke bulan depan).
	 *
	 * @return kode periode {@code YYYYMM} untuk bulan berjalan sekarang, lewat {@link #convert(Integer, Integer)}
	 */
	public static Integer sekarang() {
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.set(Calendar.DATE, 1);
		int tahunCurrent = cal.get(Calendar.YEAR);
		int bulanCurrent = cal.get(Calendar.MONTH);
		int bulanCurrentPlus = bulanCurrent + 1;
		Integer pembayaranSekarang = PembayaranAnggotaKoperasi.convert(tahunCurrent, bulanCurrentPlus);
		return pembayaranSekarang;
	}

	/**
	 * Gabungkan tahun dan bulan menjadi satu kode periode integer {@code YYYYMM} (mis. tahun 2026,
	 * bulan 9 → 202609), dengan bulan di-pad ke dua digit ({@code "09"} bukan {@code "9"}) agar
	 * urutan numerik kode periode tetap sesuai urutan waktu kalender (mis. 202609 &lt; 202610).
	 * Dipakai untuk membandingkan/mengurutkan periode pembayaran anggota secara numerik sederhana
	 * tanpa perlu tipe {@link Date} penuh.
	 *
	 * @param t tahun (mis. 2026); bila {@code null} diperlakukan sebagai string kosong
	 * @param b bulan 1-12; bila {@code null} diperlakukan sebagai string kosong (tanpa padding)
	 * @return kode periode {@code YYYYMM} sebagai {@link Integer}, atau {@code null} bila baik
	 *         {@code t} maupun {@code b} tidak menghasilkan string apa pun (keduanya {@code null})
	 */
	public static Integer convert(Integer t, Integer b) {
		String bS = "";
		if (b != null) {
			bS = ("00" + b).substring(("00" + b).length() - 2);
		}
		String tS = "";
		if (t != null) {
			tS = "" + t;
		}
		String tb = tS + "" + bS;
		Integer tahunDanBulan = null;
		if (!tb.isEmpty()) {
			tahunDanBulan = Integer.parseInt(tb);
		}
		return tahunDanBulan;
	}

	@Column(name = "inquiry_pembayaran")
	public String getInquiryPembayaran() {
		return this.inquiryPembayaran;
	}

	/** @param inquiryPembayaran nomor/kode inquiry pembayaran dari sistem pihak ketiga (bank/VA). */
	public void setInquiryPembayaran(String inquiryPembayaran) {
		this.inquiryPembayaran = inquiryPembayaran;
	}

	/** @return nominal total dokumen; {@code 0.0} (bukan {@code null}) bila belum di-set. */
	@Column(name = "nominal", nullable = false, precision = 17, scale = 17)
	public Double getNominal() {
		return this.nominal == null ? 0.0 : nominal;
	}

	/** @param nominal nominal total dokumen ini (total tagihan yang dibayar, atau total topup). */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/**
	 * @return sisa saldo deposit anggota setelah transaksi ini, atau {@code null} bila belum
	 *         dihitung/tidak relevan (berbeda dari getter numerik lain di kelas ini, method ini
	 *         TIDAK menormalkan {@code null} ke {@code 0.0}).
	 */
	@Column(name = "sisa_deposit", precision = 17, scale = 17)
	public Double getSisaDeposit() {
		return this.sisaDeposit;
	}

	/** @param sisaDeposit sisa saldo deposit anggota setelah transaksi ini diproses. */
	public void setSisaDeposit(Double sisaDeposit) {
		this.sisaDeposit = sisaDeposit;
	}

	/**
	 * <p>
	 * Tanggal transaksi ini, dengan dua efek samping "self-healing" setiap kali dipanggil:
	 * </p>
	 * <ol>
	 * <li>Bila tahun tanggal tersimpan kebetulan 1970 (indikasi bug lama epoch/parsing tanggal yang
	 * gagal dan jatuh ke {@code new Date(0)}), tahunnya diam-diam digeser ke tahun berjalan saat
	 * ini — mencegah dokumen lama tersangkut bertahun-tahun 1970 tampil di laporan/filter tanggal.</li>
	 * <li>Bila dokumen ini tertaut {@link #getVirtualAccountBank()} dan VA tersebut sudah mencatat
	 * {@code waktuBayar}, field {@code tanggal} <b>ditimpa</b> oleh waktu bayar VA tersebut — waktu
	 * pembayaran riil dari bank dianggap lebih otoritatif daripada tanggal yang di-set manual.</li>
	 * </ol>
	 * <p>
	 * Bila field {@code tanggal} masih {@code null} sama sekali (belum pernah di-set dan tidak ada
	 * VA), dikembalikan tanggal sistem saat ini ({@link ais.ui.util.WaktuUtil#getDate()}) sebagai
	 * fallback, bukan {@code null}.
	 * </p>
	 *
	 * @return tanggal transaksi, sudah dikoreksi/ditimpa sesuai aturan di atas
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false, length = 29)
	public Date getTanggal() {

		if (tanggal != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal);
			if (calendar.get(Calendar.YEAR) == 1970) {
				calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
				tanggal = calendar.getTime();
			}

		}

		if (virtualAccountBank != null && virtualAccountBank.getWaktuBayar() != null) {
			tanggal = virtualAccountBank.getWaktuBayar();
		}

		return this.tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/** @param tanggal tanggal transaksi; lihat catatan self-healing pada {@link #getTanggal()}. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * <p>
	 * <b>Getter destruktif.</b> Sebelum mengembalikan nilai, method ini terlebih dulu menimpa field
	 * {@code tanggalBayar} dengan hasil {@link #getTanggal()} — sehingga apa pun yang tersimpan di
	 * kolom {@code tanggal_bayar} sebelumnya (lewat {@link #setTanggalBayar(Date)} maupun dari
	 * database) selalu digantikan oleh tanggal transaksi saat getter ini dipanggil. Praktiknya,
	 * "tanggal bayar" dokumen ini selalu identik dengan {@link #getTanggal()}, termasuk seluruh
	 * efek self-healing (koreksi tahun 1970, override dari waktu bayar Virtual Account) yang
	 * dijelaskan di sana.
	 * </p>
	 *
	 * @return tanggal bayar, selalu sama dengan {@link #getTanggal()} saat ini
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_bayar", length = 13)
	public Date getTanggalBayar() {
		tanggalBayar = getTanggal();

		return this.tanggalBayar;
	}

	/**
	 * Set tanggal bayar secara manual. Lihat catatan destruktif pada {@link #getTanggalBayar()} —
	 * nilai yang di-set di sini akan tertimpa oleh {@link #getTanggal()} pada pemanggilan getter
	 * berikutnya.
	 *
	 * @param tanggalBayar tanggal bayar yang diinginkan
	 */
	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	/** @return nominal tambahan topup deposit; {@code 0.0} (bukan {@code null}) bila belum di-set. */
	@Column(name = "tambahan_deposit", precision = 17, scale = 17)
	public Double getTambahanDeposit() {
		return this.tambahanDeposit == null ? 0.0 : tambahanDeposit;
	}

	/** @param tambahanDeposit nominal tambahan yang menambah saldo deposit anggota pada transaksi topup. */
	public void setTambahanDeposit(Double tambahanDeposit) {
		this.tambahanDeposit = tambahanDeposit;
	}

	/** @return total saldo deposit anggota setelah topup, atau {@code null} bila belum dihitung. */
	@Column(name = "total_deposit", precision = 17, scale = 17)
	public Double getTotalDeposit() {
		return this.totalDeposit;
	}

	/** @param totalDeposit total saldo deposit anggota setelah ditambah {@link #getTambahanDeposit()}. */
	public void setTotalDeposit(Double totalDeposit) {
		this.totalDeposit = totalDeposit;
	}

	/** @return cara/metode pembayaran koperasi yang dipakai, dimuat lazy lewat {@code check(...)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_pembayaran_anggota_koperasi_id", nullable = false)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi() {
		caraPembayaranKoperasi = check(caraPembayaranKoperasi);
		return caraPembayaranKoperasi;
	}

	/** @param caraPembayaranKoperasi cara/metode pembayaran koperasi yang dipakai dokumen ini. */
	public void setCaraPembayaranKoperasi(CaraPembayaranKoperasi caraPembayaranKoperasi) {
		this.caraPembayaranKoperasi = caraPembayaranKoperasi;
	}

	/** @return jejak permintaan pembayaran ke BRI, atau {@code null} bila tidak lewat BRI. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "bri_request_id")
	public BriRequest getBriRequest() {
		return briRequest;
	}

	/** @param briRequest jejak permintaan pembayaran ke BRI yang menyertai dokumen ini. */
	public void setBriRequest(BriRequest briRequest) {
		this.briRequest = briRequest;
	}

	/** @return jejak permintaan pembayaran ke BNI, atau {@code null} bila tidak lewat BNI. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "bni_request_id")
	public BniRequest getBniRequest() {
		return bniRequest;
	}

	/** @param bniRequest jejak permintaan pembayaran ke BNI yang menyertai dokumen ini. */
	public void setBniRequest(BniRequest bniRequest) {
		this.bniRequest = bniRequest;
	}

	/**
	 * @return nama pengguna yang memvalidasi dokumen ini; bila belum pernah di-set, diisi lazy dari
	 *         nama pengguna yang sedang login ({@link Common#getCurrentUser()}) pada pemanggilan
	 *         pertama, lalu dipertahankan (bukan dihitung ulang tiap kali seperti {@link #getNama()}).
	 *         Tidak pernah mengembalikan {@code null}, selalu string (boleh kosong).
	 */
	public String getValidator() {
		if (validator == null || validator.trim().isEmpty()) {
			try {
				Tbmuser tbmuser = Common.getCurrentUser();
				validator = tbmuser == null ? "" : tbmuser.getUserNama();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return validator == null ? "" : validator;
	}

	/** @param validator nama pengguna yang memvalidasi dokumen ini. */
	public void setValidator(String validator) {
		this.validator = validator;
	}

	/** @return catatan/keterangan dokumen; string kosong (bukan {@code null}) bila belum di-set. */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk dokumen ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return jejak permintaan pembayaran ke BSI, atau {@code null} bila tidak lewat BSI. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "bsi_request_id")
	public BsiRequest getBsiRequest() {
		return bsiRequest;
	}

	/** @param bsiRequest jejak permintaan pembayaran ke BSI yang menyertai dokumen ini. */
	public void setBsiRequest(BsiRequest bsiRequest) {
		this.bsiRequest = bsiRequest;
	}

	/** @return rincian Virtual Account bank yang menerima pembayaran ini, atau {@code null} bila tidak lewat VA. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "virtual_account_bank")
	public VirtualAccountBank getVirtualAccountBank() {
		return virtualAccountBank;
	}

	/** @param virtualAccountBank rincian Virtual Account bank yang menerima pembayaran ini. */
	public void setVirtualAccountBank(VirtualAccountBank virtualAccountBank) {
		this.virtualAccountBank = virtualAccountBank;
	}

	/**
	 * <p>
	 * Resolusi objek pengguna ({@link Tbmuser}) yang memvalidasi dokumen ini, dari nama string
	 * {@link #getValidator()}. Urutan resolusi:
	 * </p>
	 * <ol>
	 * <li>Bila field {@code validatorUser} sudah terisi, cukup di-refresh lazy lewat
	 * {@code check(...)} dan dikembalikan apa adanya.</li>
	 * <li>Bila belum terisi dan {@link #getValidator()} tidak kosong, seluruh {@link Tbmuser} aktif
	 * di cache {@link ConstantValues#ambilBerdasarClass(Class)} dipindai satu per satu untuk mencari
	 * nama pengguna yang cocok (case-insensitive) dengan {@code validator} — pemindaian linear O(n)
	 * jumlah pengguna aktif, diulang tiap pemanggilan selama tidak pernah ketemu match.</li>
	 * <li>Bila masih belum ketemu <b>dan</b> dokumen ini belum tersimpan ({@link #getId()} masih
	 * {@code null}), jatuh ke pengguna yang sedang login saat ini sebagai fallback terakhir —
	 * fallback ini sengaja hanya berlaku untuk dokumen baru, bukan dokumen lama yang sedang dimuat
	 * ulang dari database (agar validator historis tidak diam-diam berubah jadi pengguna yang sedang
	 * membuka layar).</li>
	 * </ol>
	 * <p>
	 * Setiap tahap dibungkus try/catch individual yang mencatat kegagalan lewat
	 * {@link ais.common.ErrorAuditUtil#record(Exception, String)} tanpa menggagalkan pemanggil.
	 * </p>
	 *
	 * @return objek pengguna validator hasil resolusi, atau {@code null} bila tidak ketemu match dan
	 *         bukan dokumen baru
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "validator_user", nullable = true)
	public Tbmuser getValidatorUser() {

		if (validatorUser != null) {
			validatorUser = check(validatorUser);
		} else {
			try {
				if (validatorUser == null || validatorUser.getUserId() == null) {
					if (!getValidator().trim().isEmpty()) {
						for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
							try {
								Tbmuser tbmuser = (Tbmuser) o;
								if (tbmuser.getAktif()) {
									if (tbmuser.getUserNama().equalsIgnoreCase(validator)) {
										validatorUser = tbmuser;
										break;
									}
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/PembayaranAnggotaKoperasi.java:472");
							}
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/PembayaranAnggotaKoperasi.java:477");
			}

			try {
				if (getId() == null) {
					if (validatorUser == null || validatorUser.getUserId() == null) {
						Tbmuser tbmuser = Common.getCurrentUser();
						validatorUser = tbmuser;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/PembayaranAnggotaKoperasi.java:487");
			}
		}
		return validatorUser;
	}

	/**
	 * Set objek pengguna validator secara eksplisit. Bila di-set, ini melewati seluruh pencarian
	 * berbasis nama yang dijelaskan pada {@link #getValidatorUser()}.
	 *
	 * @param validatorUser objek pengguna yang memvalidasi dokumen ini.
	 */
	public void setValidatorUser(Tbmuser validatorUser) {
		this.validatorUser = validatorUser;
	}

	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal (dok 61 butir B): terisi begitu mesin
	 * {@code PostingDanaAnggotaUtil} menjurnalkan dokumen ini. Sebelumnya perputaran dana
	 * anggota tidak pernah menyentuh buku besar sama sekali.
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
