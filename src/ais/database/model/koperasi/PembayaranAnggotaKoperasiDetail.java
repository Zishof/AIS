package ais.database.model.koperasi;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.PostingHistory;
import ais.ui.util.WaktuUtil;

/**
 * <h2>PembayaranAnggotaKoperasiDetail — Rincian per Komponen Pembayaran Anggota</h2>
 *
 * <p>
 * Entity anak dari {@link PembayaranAnggotaKoperasi} ini menyimpan <b>satu baris tagihan yang
 * dibayar</b> pada satu dokumen pembayaran header. Satu {@link TransaksiKoperasiDetail} (mis. satu
 * angsuran/cicilan pinjaman dengan komponen pokok + margin/bunga, lihat {@code getPokok()} dan
 * {@code getMargin()} pada kelas tersebut) dipasangkan dengan tepat satu baris di sini lewat
 * {@link #getTransaksiKoperasiDetail()}, dan {@link TransaksiKoperasiDetail} sendiri menyimpan
 * pointer balik ke baris pembayaran yang melunasinya — pasangan pointer dua arah inilah yang
 * dipakai alur produksi ({@code TunaiAnggotaKoperasiCommon#onSave}) sebagai <b>penjaga agar satu
 * tagihan tidak bisa dibayar dua kali</b>: sebelum membuat baris baru, kode pemanggil memeriksa
 * apakah {@code TransaksiKoperasiDetail#getPembayaranAnggotaKoperasiDetail()} sudah terisi, dan
 * bila sudah, tagihan tersebut dilewati.
 * </p>
 *
 * <h3>Dua nominal, satu yang menang</h3>
 * <p>
 * Kelas ini menyimpan {@link #getNominal()} (nominal hasil hitungan otomatis dari tagihan) dan
 * {@link #getNominalManual()} (koreksi manual, mis. pembulatan atau diskon khusus). Lihat catatan
 * pada {@link #getNominal()}: bila {@code nominalManual} terisi dan bukan nol, ia selalu menimpa
 * {@code nominal} setiap kali dibaca — pola getter destruktif yang sama dipakai berulang di entity
 * finansial AIS lain.
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * {@code @Audited} (perubahan/hapus baris detail pembayaran anggota harus dapat ditelusuri),
 * relasi lazy dengan {@code check(...)}, hook audit {@code @PreUpdate}, kompatibel Java 1.7.
 * {@link #buatPembayaran(Session, Koperasi, AnggotaKoperasi, Tbmuser, Double, String, String)} adalah
 * jalur alternatif pembuatan header {@link PembayaranAnggotaKoperasi} lewat upload/import (bukan
 * lewat grid ZK interaktif) — meski method-nya menempel di kelas Detail ini karena alasan historis,
 * ia sebenarnya tidak membuat baris Detail apa pun, hanya header topup deposit.
 * </p>
 *
 * @see PembayaranAnggotaKoperasi
 * @see TransaksiKoperasiDetail
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "pembayaran_anggota_koperasi_detail", schema = "koperasi")
public class PembayaranAnggotaKoperasiDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas ini; dinaikkan hanya bila struktur field yang mempengaruhi
	 * kompatibilitas serialisasi berubah.
	 */
	private static final long serialVersionUID = -4014084859898847843L;
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

	/** Tagihan (angsuran/cicilan) yang dilunasi baris pembayaran ini. */
	private TransaksiKoperasiDetail transaksiKoperasiDetail;
	/** Header pembayaran yang menaungi baris rincian ini. */
	private PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi;
	/** Catatan/keterangan bebas untuk baris rincian ini. */
	private String keterangan;
	/** Referensi eksternal unik untuk baris rincian ini (mis. nomor bukti/kwitansi), bila ada. */
	private String ref;
	/** Nominal hasil hitungan otomatis dari tagihan; lihat catatan destruktif pada {@link #getNominal()}. */
	private Double nominal;
	/** Nominal koreksi manual; bila terisi dan bukan nol, menimpa {@link #nominal} saat dibaca. */
	private Double nominalManual;
	/** Riwayat posting jurnal untuk baris rincian ini, bila mekanisme posting per-baris dipakai. */
	private PostingHistory postingHistory;

	/**
	 * <p>
	 * Membuat dan langsung menyimpan (dalam transaksi sendiri) satu dokumen header
	 * {@link PembayaranAnggotaKoperasi} berjenis <b>topup saldo/deposit</b> untuk seorang anggota,
	 * ditujukan untuk alur <i>upload/import batch</i> (mis. import Excel daftar setoran) — bukan
	 * lewat interaksi grid ZK yang dipakai {@link PembayaranAnggotaKoperasi#saveDetail(Rows, Session)}.
	 * Meski method ini menempel di kelas {@code PembayaranAnggotaKoperasiDetail} (kemungkinan
	 * penempatan historis, mengikuti kelas mana yang lebih dulu ada), <b>ia sama sekali tidak
	 * membuat baris {@link PembayaranAnggotaKoperasiDetail} apa pun</b> — hanya header topup
	 * deposit polos lewat {@link PembayaranAnggotaKoperasi#setTambahanDeposit(Double)}. Nama
	 * method-nya ("buatPembayaran") sedikit menyesatkan karena tidak menyentuh tagihan
	 * ({@link TransaksiKoperasiDetail}) sama sekali, berbeda dengan alur pelunasan cicilan yang
	 * dijelaskan pada Javadoc kelas ini.
	 * </p>
	 *
	 * <p>
	 * <b>Method ini tidak dipanggil dari mana pun di codebase</b> — sama seperti
	 * {@link PembayaranAnggotaKoperasi#saveDetail(Rows, Session)}, ini tampaknya sisa
	 * implementasi awal fitur upload yang belum (atau tidak jadi) dipakai; alur upload topup yang
	 * aktif sekarang kemungkinan memakai jalur lain di luar paket ini.
	 * </p>
	 *
	 * <p>
	 * <b>Cara kerja bila suatu saat dipakai.</b> Cara pembayaran ({@link CaraPembayaranKoperasi})
	 * dipilih otomatis lewat dua tahap fallback: pertama dicari cara bayar milik {@code koperasi}
	 * yang aktif <b>dan</b> ditandai {@code manual = true} (relevan untuk topup manual/upload);
	 * bila tidak ada, jatuh ke cara bayar aktif mana pun milik koperasi tersebut (tanpa syarat
	 * manual). Bila keduanya tidak ketemu, {@code caraPembayaranKoperasi} tetap {@code null} dan
	 * diset begitu saja ke header — padahal kolomnya {@code nullable = false} di level Hibernate,
	 * sehingga penyimpanan akan gagal pada tahap flush/commit jika tidak ada satu pun cara bayar
	 * yang cocok untuk koperasi tersebut.
	 * </p>
	 *
	 * <p>
	 * Parameter {@code tanggal} berupa String bebas diparse dengan mencoba berurutan beberapa
	 * format tanggal yang dipakai berbagai layar AIS ({@code dateFormat3} "dd-MM-yyyy HH:mm",
	 * {@code dateFormat1} "HH:mm:ss"/generic, {@code databaseDateFormat} "yyyy-MM-dd...",
	 * {@code dateFormat112} yang mengandung "/") berdasarkan heuristik panjang dan pemisah string
	 * — bila semua percobaan parse gagal (mis. format tak dikenal atau {@code tanggal} bernilai
	 * {@code null}, yang akan membuat {@code content.trim()} melempar {@link NullPointerException}),
	 * exception ditangkap diam-diam dan {@code t} tetap berupa tanggal sistem saat ini yang
	 * disiapkan di awal — dokumen tetap tersimpan, hanya dengan tanggal transaksi yang salah
	 * (hari ini, bukan tanggal asli di file upload) tanpa ada indikasi kegagalan ke pemanggil.
	 * </p>
	 *
	 * <p>
	 * Keterangan otomatis diberi akhiran {@code " (via upload)"} agar dokumen hasil import mudah
	 * dibedakan dari pembayaran yang dibuat lewat layar interaktif biasa. Penyimpanan memakai
	 * transaksi Hibernate sendiri ({@code session.getTransaction().begin()/commit()}) — bila method
	 * ini dipanggil dari kode yang sudah berada di dalam transaksi aktif lain, ini berpotensi
	 * konflik/nested-transaction tergantung konfigurasi Hibernate, sesuatu yang perlu diperhatikan
	 * bila method ini suatu saat diaktifkan kembali.
	 * </p>
	 *
	 * @param session     sesi Hibernate aktif; method ini membuka dan meng-commit transaksinya
	 *                    sendiri di dalamnya
	 * @param koperasi    koperasi pemilik dokumen topup
	 * @param anggotaKoperasi anggota yang menyetor/topup
	 * @param tbmuser     pengguna yang memvalidasi; boleh {@code null} (validator disimpan string
	 *                    kosong)
	 * @param nominal     nominal topup, langsung menjadi {@link PembayaranAnggotaKoperasi#getTambahanDeposit()}
	 * @param tanggal     tanggal transaksi dalam berbagai format string yang didukung; lihat catatan
	 *                    parsing di atas untuk perilaku saat parse gagal
	 * @param keterangan  keterangan dasar, akan ditambahi akhiran {@code " (via upload)"}
	 * @return header {@link PembayaranAnggotaKoperasi} yang baru dibuat dan sudah tersimpan/di-commit
	 */
	public static PembayaranAnggotaKoperasi buatPembayaran(Session session, Koperasi koperasi,
			AnggotaKoperasi anggotaKoperasi, Tbmuser tbmuser, Double nominal, String tanggal, String keterangan) {
		CaraPembayaranKoperasi caraPembayaranKoperasi = (CaraPembayaranKoperasi) ConstantValues.simpleObject(session
				.createCriteria(CaraPembayaranKoperasi.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("manual", true)).add(Restrictions.eq("koperasi", koperasi)).setMaxResults(1),
				CaraPembayaranKoperasi.class);
		if (caraPembayaranKoperasi == null) {
			caraPembayaranKoperasi = (CaraPembayaranKoperasi) ConstantValues
					.simpleObject(
							session.createCriteria(CaraPembayaranKoperasi.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("koperasi", koperasi)).setMaxResults(1),
							CaraPembayaranKoperasi.class);
		}

		PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi = new PembayaranAnggotaKoperasi();
		pembayaranAnggotaKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasi);

		pembayaranAnggotaKoperasi.setAnggotaKoperasi(anggotaKoperasi);
		pembayaranAnggotaKoperasi.setKoperasi(koperasi);
		pembayaranAnggotaKoperasi.setValidator(tbmuser == null ? "" : tbmuser.getUserNama());
		pembayaranAnggotaKoperasi.setNominal(nominal);
		String content = "";
		Date t = WaktuUtil.getDate();
		try {
			content = tanggal;
			if (content.trim().split("-")[2].split(" ")[0].length() == 4 && content.trim().split("-")[0].length() == 2
					&& content.trim().split(" ").length == 2) {
				t = Common.dateFormat3.get().parse(content.trim());
			} else if (content.trim().split(":").length == 3 && content.trim().length() == 8) {
				t = Common.dateFormat1.get().parse(content.trim());
			} else if (content.trim().split("-")[0].length() == 4) {
				t = Common.databaseDateFormat.get().parse(content.trim());
			} else if (content.trim().contains("/")) {
				t = Common.dateFormat112.get().parse(content.trim());
			} else {
				t = Common.dateFormat1.get().parse(content.trim());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/PembayaranAnggotaKoperasiDetail.java:135");

		}

		pembayaranAnggotaKoperasi.setTanggal(t);
		pembayaranAnggotaKoperasi.setTambahanDeposit(nominal);
		pembayaranAnggotaKoperasi.setKeterangan(keterangan + " (via upload)");
		session.getTransaction().begin();
		session.save(pembayaranAnggotaKoperasi);
		session.getTransaction().commit();

		return pembayaranAnggotaKoperasi;
	}

	/** @return representasi ringkas "id-tagihan" untuk debug/log, mis. pada pesan error. */
	public String toString() {
		return id + "-" + (transaksiKoperasiDetail == null ? "" : transaksiKoperasiDetail.toString());
	}

	/** Konstruktor kosong, dipakai Hibernate dan saat membangun baris rincian sebelum diisi. */
	public PembayaranAnggotaKoperasiDetail() {
	}

	/**
	 * Konstruktor pintasan yang langsung menautkan baris rincian baru ke tagihan yang dilunasinya.
	 *
	 * @param transaksiKoperasiDetail tagihan (angsuran/cicilan) yang akan dilunasi baris ini
	 */
	public PembayaranAnggotaKoperasiDetail(TransaksiKoperasiDetail transaksiKoperasiDetail) {
		this.transaksiKoperasiDetail = transaksiKoperasiDetail;
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

	/** @return header pembayaran yang menaungi baris rincian ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_anggotaKoperasi_id", nullable = false)
	public PembayaranAnggotaKoperasi getPembayaranAnggotaKoperasi() {
		return this.pembayaranAnggotaKoperasi;
	}

	/**
	 * Tautkan baris rincian ini ke header pembayarannya. Ditolak diam-diam (tidak mengubah apa
	 * pun) bila {@code pembayaranAnggotaKoperasi} bernilai {@code null} atau belum memiliki id
	 * (header belum tersimpan ke database) — kolom foreign key-nya {@code nullable = false},
	 * sehingga penjagaan ini mencegah baris rincian tersimpan tanpa induk yang valid, tetapi juga
	 * berarti header <b>harus</b> di-{@code session.save(...)} lebih dulu sebelum baris rincian
	 * ditautkan, kalau tidak tautannya diam-diam gagal terpasang tanpa exception.
	 *
	 * @param pembayaranAnggotaKoperasi header pembayaran; diabaikan bila {@code null} atau belum
	 *                                  memiliki id
	 */
	public void setPembayaranAnggotaKoperasi(PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi) {
		if (pembayaranAnggotaKoperasi == null || pembayaranAnggotaKoperasi.getId() == null) {
			return;
		}

		this.pembayaranAnggotaKoperasi = pembayaranAnggotaKoperasi;
	}

	/** @return catatan/keterangan baris rincian ini, boleh {@code null} bila belum di-set. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk baris rincian ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Varian null-safe dari {@link #getNominal()}: melakukan resolusi prioritas manual-vs-otomatis
	 * yang sama (bila {@link #getNominalManual()} terisi, ia menimpa {@code nominal}), tapi berbeda
	 * dari {@link #getNominal()}, hasil akhirnya <b>tidak pernah {@code null}</b> — jatuh ke
	 * {@code 0.0} bila {@code nominal} masih kosong setelah resolusi. Cocok dipakai di tempat yang
	 * langsung melakukan aritmetika (penjumlahan total, dsb.) tanpa perlu null-check tambahan.
	 *
	 * @return nominal final baris ini (manual bila ada, otomatis bila tidak), tidak pernah
	 *         {@code null}
	 */
	public Double ambilNominal() {
		if (getNominalManual() != null) {
			nominal = getNominalManual();
		}
		return this.nominal == null ? 0.0 : nominal;
	}

	/**
	 * <p>
	 * Nominal baris rincian ini — kolom yang dipetakan JPA/dipersist ke database.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif dengan resolusi prioritas.</b> Setiap kali dipanggil, method ini lebih
	 * dulu memeriksa {@link #getNominalManual()}: bila terisi (bukan {@code null} dan bukan nol),
	 * nilai tersebut <b>menimpa</b> field {@code nominal} sebelum dikembalikan — koreksi manual
	 * selalu menang atas nominal hasil hitungan otomatis dari tagihan. Berbeda dari
	 * {@link #ambilNominal()}, method ini <b>tidak</b> menormalkan hasil {@code null} ke
	 * {@code 0.0} — bila baik {@code nominal} maupun {@code nominalManual} kosong, {@code null}
	 * yang dikembalikan apa adanya. Pemanggil yang langsung memakai getter ini (bukan
	 * {@code ambilNominal()}) untuk aritmetika perlu menangani kemungkinan {@code null} sendiri.
	 * </p>
	 *
	 * @return nominal final baris ini (manual bila ada, otomatis bila tidak); bisa {@code null}
	 */
	@Column(name = "nominal", precision = 17, scale = 17)
	public Double getNominal() {

		if (getNominalManual() != null) {
			nominal = getNominalManual();
		}
		return this.nominal;
	}

	/** @param nominal nominal hasil hitungan otomatis dari tagihan (pokok + margin). */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/** @return tagihan (angsuran/cicilan) yang dilunasi baris ini, dimuat lazy lewat {@code check(...)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi_koperasi_detail", nullable = false)
	public TransaksiKoperasiDetail getTransaksiKoperasiDetail() {
		transaksiKoperasiDetail = check(transaksiKoperasiDetail);
		return transaksiKoperasiDetail;
	}

	/** @param transaksiKoperasiDetail tagihan (angsuran/cicilan) yang dilunasi baris ini. */
	public void setTransaksiKoperasiDetail(TransaksiKoperasiDetail transaksiKoperasiDetail) {
		this.transaksiKoperasiDetail = transaksiKoperasiDetail;
	}

	/** @return riwayat posting jurnal untuk baris rincian ini, atau {@code null} bila belum/tidak diposting per-baris. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_id")
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/** @param postingHistory riwayat posting jurnal untuk baris rincian ini. */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/** @return referensi eksternal unik baris ini (mis. nomor bukti/kwitansi), atau {@code null} bila tidak ada. */
	@Column(unique = true)
	public String getRef() {
		return ref;
	}

	/** @param ref referensi eksternal unik untuk baris rincian ini; harus unik di seluruh tabel bila di-set. */
	public void setRef(String ref) {
		this.ref = ref;
	}

	/**
	 * @return nominal koreksi manual, atau {@code null} bila belum di-set <b>atau</b> nilainya
	 *         kebetulan nol (dibulatkan lewat {@code intValue()}) — kedua kondisi ini sengaja
	 *         diperlakukan sama sebagai "tidak ada koreksi manual" oleh {@link #getNominal()} dan
	 *         {@link #ambilNominal()}, sehingga mengisi nominal manual dengan nilai 0 tidak bisa
	 *         dipakai untuk "meniadakan" tagihan — nominal otomatis dari tagihan akan tetap dipakai.
	 */
	public Double getNominalManual() {
		return nominalManual == null || nominalManual.intValue() == 0 ? null : nominalManual;
	}

	/** @param nominalManual nominal koreksi manual; lihat catatan pada {@link #getNominalManual()} soal nilai nol. */
	public void setNominalManual(Double nominalManual) {
		this.nominalManual = nominalManual;
	}

}
