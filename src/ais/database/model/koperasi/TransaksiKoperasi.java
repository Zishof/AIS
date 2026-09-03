package ais.database.model.koperasi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONArray;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * <h2>TransaksiKoperasi — Header Transaksi CENTRAL Koperasi (Simpanan &amp; Pinjaman/USPK).</h2>
 *
 * <p>Entity ini adalah HEADER tunggal untuk seluruh transaksi Unit Simpan Pinjam Koperasi (USPK):
 * satu baris di {@code koperasi.transaksi_koperasi} bisa berarti setoran/penarikan SIMPANAN
 * ataupun pencairan PINJAMAN, dibedakan lewat {@link #getProdukKoperasi()} →
 * {@link ProdukKoperasi#getTipeProdukKoperasi()} (dibandingkan dengan konstanta global
 * {@code ConstantValues.SIMPANAN}/{@code ConstantValues.PINJAMAN}). Field {@link #getNilai()}
 * menyimpan pokok/nominal transaksi, {@link #getMargin()} bunga/jasa yang dihitung dari produk,
 * dan {@link #getTotal()} adalah jumlah keduanya (nominal tagihan/pencairan penuh).</p>
 *
 * <p><b>Relasi ke {@link TransaksiKoperasiDetail}.</b> Untuk transaksi PINJAMAN, header ini punya
 * banyak baris anak {@link TransaksiKoperasiDetail} (via {@code transaksiKoperasiDetail
 * .transaksiKoperasi}) yang memecah {@link #getTotal()} menjadi jadwal angsuran per periode —
 * lihat {@link #getTanggalMulaiDiangsur()}/{@link #getTanggalTerakhirDiangsur()}/
 * {@link #getJumlahAngsur()} pada kelas ini yang menghitung parameter jadwal tsb, dan
 * {@link TransaksiKoperasiDetail#getPokok()}/{@link TransaksiKoperasiDetail#getMargin()} pada
 * baris anak yang membawa pecahan pokok/margin per angsuran.</p>
 *
 * <p><b>Basis perhitungan SHU ({@link ShuAnggota}).</b> Saat SHU tahun buku dihitung
 * ({@code PembagianShuHelper.hitungDanSimpan}), basis jasa modal dijumlahkan dari SELURUH baris
 * bertipe SIMPANAN milik anggota TANPA filter status aktif, sedangkan basis jasa usaha dijumlahkan
 * dari {@link #getMargin()} baris bertipe PINJAMAN yang HANYA {@link #getAktif()} == true — lihat
 * Javadoc {@link ShuAnggota#getJasaModal()}/{@link ShuAnggota#getJasaUsaha()} untuk rumus lengkap.
 * Konsekuensinya: perubahan {@link #getAktif()} atau {@link ProdukKoperasi} pada baris ini
 * langsung mempengaruhi hasil hitung SHU seluruh anggota pada perhitungan ulang berikutnya.</p>
 *
 * <p><b>Alur persetujuan (disposisi SOP).</b> Kelas ini mewarisi {@link DataSop} dan memakai
 * {@link #getDisposisiSop()} sebagai sumber kebenaran status approval: {@link #getStatus()},
 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()}, dan
 * {@link #getTanggalPembuatan()} semuanya DITURUNKAN dari objek disposisi terkait (bila ada) tiap
 * kali getter dipanggil — bukan murni dibaca dari kolom tersimpan. Lihat catatan integritas lebih
 * rinci pada masing-masing getter tsb.</p>
 *
 * <p><b>Kualitas pinjaman &amp; analisis (SOM USPK).</b> Field {@link #getKolektibilitas()},
 * {@link #getAgunan()}, {@link #getNilaiAgunan()}, {@link #getAnalisisKualitatif()}, dan
 * {@link #getAnalisisKuantitatif()} adalah hasil MERGE dari model {@code PinjamanAnggota} eks
 * terpisah ke kelas ini, agar tidak ada model ganda untuk satu pinjaman — seluruhnya nullable dan
 * null-safe supaya transaksi lama (simpanan, atau pinjaman yang dibuat sebelum field ini ada)
 * tetap berfungsi tanpa migrasi data.</p>
 *
 * @see TransaksiKoperasiDetail
 * @see ShuAnggota
 * @see ProdukKoperasi
 * @see JenisTransaksiKoperasi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "transaksi_koperasi")
public class TransaksiKoperasi extends DataSop {

	/**
	 * Versi serialisasi tetap; dipertahankan hanya krn kontrak {@code Serializable} yang diwarisi
	 * dari {@link DataSop}/{@code GeneralValueObject}.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Nama petugas yang membuat/mengubah header transaksi ini (jejak audit tampilan, bukan FK).
	 * Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang membuat/mengubah header transaksi ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * ID/username petugas yang membuat/mengubah header transaksi ini.
	 *
	 * @return id/username petugas, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank: nilai {@code null}/kosong/spasi DIABAIKAN
	 * (early return) — field yang sudah terisi tidak ditimpa balik ke kosong.
	 *
	 * @param olehId id/username petugas; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan {@link #oleh}. Guard null/blank sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama petugas; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama petugas yang membuat/mengubah header transaksi ini.
	 *
	 * @return nama petugas, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan header ini TERAKHIR diubah, dengan menuliskan
	 * waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum {@code UPDATE}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu header ini terakhir diubah, diisi otomatis lewat {@link #onUpdate()}. Lihat
	 * {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung — field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Timestamp perubahan terakhir header ini, diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir, atau waktu instansiasi objek bila belum pernah di-update.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Status awal: transaksi diajukan, belum disetujui. Nilai default {@link #getStatus()}. */
	public static final String PENGAJUAN = "Pengajuan";
	/** Status setelah disposisi SOP disetujui — lihat {@link #getStatus()}/{@link #getDisetujuiOleh()}. */
	public static final String DISETUJU = "Disetujui";
	/** Status ditolak; men-set ini lewat {@link #setStatus(String)} otomatis mengosongkan
	 * {@link #disetujuiOleh}/{@link #tanggalPersetujuan}. */
	public static final String DITOLAK = "Ditolak";

	// Kualitas pinjaman (kolektibilitas) sesuai SOM USPK — penanganan pinjaman bermasalah.
	/** Kolektibilitas lancar (default) — angsuran berjalan normal tanpa tunggakan. */
	public static final String KOL_LANCAR = "LANCAR";
	/** Kolektibilitas kurang lancar — mulai ada keterlambatan angsuran. */
	public static final String KOL_KURANG_LANCAR = "KURANG_LANCAR";
	/** Kolektibilitas ragu-ragu — tunggakan lebih lanjut, potensi bermasalah. */
	public static final String KOL_RAGU = "RAGU";
	/** Kolektibilitas macet — pinjaman bermasalah berat, kemungkinan gagal bayar. */
	public static final String KOL_MACET = "MACET";

	/**
	 * Representasi teks singkat header transaksi ini, dipakai a.l. oleh komponen UI ZK (mis. combo
	 * box) yang menampilkan objek lewat {@code toString()}.
	 *
	 * @return nilai field {@link #nama} APA ADANYA (properti {@code nama}, BUKAN
	 *         {@link #getNama()}) — berbeda dari kebanyakan {@code toString()} lain di paket ini,
	 *         method ini TIDAK memanggil getter, jadi tidak memicu logika auto-generate nama pada
	 *         {@link #getNama()}; bisa mengembalikan {@code null} bila field {@code nama} belum
	 *         pernah diisi/di-generate oleh pemanggil lain sebelumnya.
	 */
	public String toString() {
		return nama;
	}

	/** Kode/nomor transaksi. Lihat {@link #getKode()}. */
	private String kode;

	/** Nama tampilan transaksi; AUTO-GENERATE oleh getter bila {@link #anggotaKoperasi} dan
	 * {@link #produkKoperasi} tersedia. Lihat {@link #getNama()}. */
	private String nama;
	/** Catatan bebas transaksi. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Anggota koperasi pemilik transaksi ini. Lihat {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Produk koperasi (menentukan tipe SIMPANAN/PINJAMAN, bunga, jangka waktu). Lihat
	 * {@link #getProdukKoperasi()}. */
	private ProdukKoperasi produkKoperasi;
	/** Nominal pokok transaksi (setoran/penarikan/pencairan pinjaman), TANPA margin/bunga. Lihat
	 * {@link #getNilai()}. */
	private Double nilai;
	/** Bunga/margin transaksi; RE-DIHITUNG oleh getter dari konfigurasi {@link #produkKoperasi}
	 * saat ini setiap kali dibaca. Lihat {@link #getMargin()}. */
	private Double margin;
	/** Total transaksi = {@link #nilai} + {@link #margin}. Lihat {@link #getTotal()}. */
	private Double total;
	/** Status aktif header ini; getter bisa DIPAKSA {@code false} oleh status disposisi SOP. Lihat
	 * {@link #getAktif()}. */
	private Boolean aktif;
	/** Tanggal pengajuan transaksi. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Formula/skema perhitungan tambahan (JSON), dipakai kalkulasi non-standar. Lihat
	 * {@link #getFormula()}. */
	private String formula;

	/** Satuan kerja pemilik transaksi; DITURUNKAN dari {@link #anggotaKoperasi} bila tersedia. Lihat
	 * {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Disposisi alur SOP yang menaungi persetujuan transaksi ini — sumber kebenaran utk
	 * {@link #getStatus()}/{@link #getAktif()}/{@link #getDibuatOleh()}/{@link #getDisetujuiOleh()}.
	 * Lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;

	/** Pengajuan transfer terkait bila transaksi ini lewat jalur transfer bank (bukan kas
	 * langsung). Lihat {@link #getDaftarPengajuanTransfer()}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;

	/** Cara pembayaran/penerimaan yang dipakai transaksi ini. Lihat
	 * {@link #getCaraPembayaranKoperasi()}. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi;

	/** Petugas pembuat transaksi; DITURUNKAN dari disposisi awal SOP bila ada. Lihat
	 * {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Petugas penyetuju transaksi; DITURUNKAN dari disposisi setuju SOP / proses transfer bila ada.
	 * Lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Waktu persetujuan; DITURUNKAN dari disposisi setuju SOP, kecuali diisi manual lewat
	 * {@link #tanggalPersetujuanManual}. Lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Waktu pembuatan; DITURUNKAN dari disposisi awal SOP bila ada. Lihat
	 * {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Tanggal transaksi efektif (realisasi transfer/transitori bila ada, jika tidak jatuh ke
	 * tanggal pembuatan). Lihat {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi;
	/** Tanggal mulai jadwal angsuran (khusus PINJAMAN). Lihat {@link #getTanggalMulaiDiangsur()}. */
	private Date tanggalMulaiDiangsur;
	/** Tanggal jatuh tempo angsuran terakhir (khusus PINJAMAN), dihitung dari
	 * {@link #tanggalMulaiDiangsur} + jangka waktu produk. Lihat {@link #getTanggalTerakhirDiangsur()}. */
	private Date tanggalTerakhirDiangsur;
	/** Jumlah angsuran; dihitung dari rentang tanggal (PINJAMAN) atau jumlah transaksi terbentuk
	 * produk (non-PINJAMAN). Lihat {@link #getJumlahAngsur()}. */
	private Integer jumlahAngsur;
	/** Nominal yang benar-benar diterima anggota (setelah potongan dsb, bila berlaku). Lihat
	 * {@link #getYangDiterima()}. */
	private Double yangDiterima;
	/** Status transaksi ({@link #PENGAJUAN}/{@link #DISETUJU}/{@link #DITOLAK}); getter memaksa
	 * {@link #DISETUJU} bila {@link #getDisetujuiOleh()} terisi. Lihat {@link #getStatus()}. */
	private String status;
	/** Override manual waktu persetujuan, dipakai bila alur bukan lewat disposisi SOP standar. Lihat
	 * {@link #getTanggalPersetujuanManual()}. */
	private Date tanggalPersetujuanManual;
	/** Riwayat posting jurnal akuntansi transaksi ini. Lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;
	/** Nomor surat alur keuangan; default {@link NomorSuratAlurKeuangan#TRANSAKSI_KOPERASI_DATA}.
	 * Lihat {@link #getNomorSuratAlurKeuangan()}. */
	private NomorSuratAlurKeuangan nomorSuratAlurKeuangan;
	/** Bulan pembuatan (1-12); default bulan berjalan saat pertama dibaca. Lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Tahun pembuatan; default tahun berjalan saat pertama dibaca. Lihat {@link #getTahun()}. */
	private Integer tahun;

	// Analisis & kualitas pinjaman (SOM USPK) — hasil merge eks-PinjamanAnggota ke sini.
	private String kolektibilitas;
	private String agunan;
	private Double nilaiAgunan;
	private String analisisKualitatif;
	private String analisisKuantitatif;

	/** Konstruktor tanpa argumen wajib JPA/Hibernate. */
	public TransaksiKoperasi() {
	}

	/**
	 * PK identity header transaksi ini. {@code null} sebelum entity di-{@code save}/{@code flush}
	 * ke Hibernate. {@code insertable = false} pada {@code @Column} — kolom {@code id} TIDAK
	 * disertakan pada statement {@code INSERT} yang dibuat Hibernate utk kelas ini (nilainya murni
	 * ditentukan DB lewat {@link IDENTITY}).
	 *
	 * @return id header transaksi, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK — dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak perlu
	 * memanggil ini; id baru dibuat otomatis oleh DB saat insert.
	 *
	 * @param id id header transaksi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode/nomor transaksi. Getter menormalisasi: {@code trim()} nilai bila terisi, mengembalikan
	 * {@code null} (bukan string kosong) bila field {@code null} atau blank.
	 *
	 * @return kode transaksi yang sudah di-{@code trim()}, atau {@code null} bila belum diisi/blank.
	 */
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menetapkan kode transaksi. Tidak ada guard null/blank — nilai apa adanya disimpan; normalisasi
	 * {@code trim()}/{@code null}-kan hanya terjadi di {@link #getKode()} saat dibaca.
	 *
	 * @param kode kode transaksi baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama tampilan header transaksi ini.
	 *
	 * <p><b>Catatan integritas — getter destruktif dgn auto-generate.</b> Bila
	 * {@link #getAnggotaKoperasi()} DAN {@link #getProdukKoperasi()} sama-sama tersedia, getter ini
	 * MENIMPA field {@link #nama} di memori dgn string baru yang disusun dari
	 * {@code produk.getNama() + " " + anggotaKoperasi + " senilai " + format(getNilai())} SETIAP
	 * KALI dipanggil — termasuk saat Hibernate memanggil getter ini utk membentuk statement
	 * {@code INSERT}/{@code UPDATE} ({@code dynamicInsert}/{@code dynamicUpdate} di kelas ini
	 * bergantung pada nilai getter, bukan field mentah). Konsekuensinya: nilai kolom {@code nama}
	 * di DB pada dasarnya SELALU hasil generate otomatis ini ketika kedua relasi tsb tersedia saat
	 * disimpan — nilai yang di-{@code set} manual lewat {@link #setNama(String)} hanya bertahan
	 * selama relasi anggota/produk belum ter-resolve (mis. sebelum di-assign, atau saat salah satu
	 * relasinya {@code null}). Bergantung pada {@code toString()} {@link AnggotaKoperasi} utk bagian
	 * nama anggota (lihat Javadoc kelas tsb bila formatnya perlu diverifikasi).
	 *
	 * @return nama tampilan (auto-generate bila relasi anggota &amp; produk tersedia) hasil
	 *         {@code trim()}, atau {@code null} bila field {@code nama} belum pernah diisi/generate.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {

		if (getAnggotaKoperasi() != null && getProdukKoperasi() != null) {
			nama = getProdukKoperasi().getNama() + " " + getAnggotaKoperasi() + " senilai "
					+ Common.numberFormat.get().format(getNilai());
		}

		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama tampilan header transaksi. Lihat catatan penting pada {@link #getNama()} —
	 * nilai yang di-set di sini akan DITIMPA oleh getter tsb begitu relasi anggota &amp; produk
	 * koperasi sama-sama tersedia.
	 *
	 * @param nama nama tampilan baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas transaksi.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas transaksi.
	 *
	 * @param keterangan catatan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif header transaksi ini.
	 *
	 * <p><b>Bukan flag murni tersimpan — bisa DIPAKSA {@code false} oleh disposisi SOP.</b> Getter
	 * ini pertama-tama membaca ulang {@link #getDisposisiSop()}; bila disposisi tsb ada dan TIDAK
	 * aktif ({@code !disposisiSop.getAktif()}), ATAU bila disposisi tsb sudah mencapai
	 * {@code disposisiEnd} pada alur SOP yang {@code getPenolakanAdaDiSini() == true} (penanda titik
	 * penolakan), field {@link #aktif} di memori DITIMPA jadi {@code false} — TIDAK PERNAH
	 * sebaliknya (getter ini tidak pernah memaksa {@code true}). Dengan kata lain arahnya SATU
	 * ARAH: disposisi yang ditolak/nonaktif selalu bisa mematikan {@code aktif}, tapi disposisi yang
	 * aktif/disetujui tidak bisa menghidupkan kembali {@code aktif} yang sudah di-set {@code false}
	 * secara manual. Default {@code true} bila field maupun disposisi tidak menyatakan sebaliknya.
	 *
	 * @return {@code true} bila transaksi aktif/berlaku, {@code false} bila dinonaktifkan manual
	 *         ATAU oleh status disposisi SOP-nya.
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif header transaksi secara manual. Lihat catatan pada {@link #getAktif()}
	 * — nilai {@code true} yang di-set di sini tetap bisa DITIMPA kembali jadi {@code false} oleh
	 * getter tsb bila disposisi SOP terkait tidak aktif/ditolak.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Nominal pokok transaksi (setoran/penarikan/pencairan pinjaman), TANPA margin/bunga. Getter
	 * null-safe: mengembalikan {@code 0.0} bila kolom NULL di DB.
	 *
	 * @return nilai pokok transaksi, tidak pernah {@code null}.
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menetapkan nominal pokok transaksi.
	 *
	 * @param nilai nilai pokok baru.
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Anggota koperasi pemilik transaksi ini. Getter memanggil {@code check(...)} sebelum
	 * mengembalikan nilai — meresolusi proxy lazy Hibernate yang mungkin sudah "basi" (sesi asalnya
	 * tertutup) lewat cache identity map internal, supaya pemanggil di luar sesi Hibernate tidak
	 * selalu menabrak {@code LazyInitializationException}.
	 *
	 * @return anggota koperasi pemilik transaksi ini, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	/**
	 * Menetapkan anggota koperasi pemilik transaksi ini. Turut mempengaruhi hasil
	 * {@link #getNama()} (auto-generate) dan {@link #getSatuanKerja()} (diturunkan dari satuan kerja
	 * anggota) pada pemanggilan getter berikutnya.
	 *
	 * @param anggotaKoperasi anggota koperasi baru.
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Tanggal pengajuan transaksi (kolom {@code tanggal_pengajuan}).
	 *
	 * @return tanggal pengajuan, atau waktu saat ini ({@code new Date()}) bila belum diisi — CATATAN:
	 *         fallback ini dihitung ULANG setiap getter dipanggil (bukan nilai tetap saat entity
	 *         dibuat), jadi dua pemanggilan berturut-turut pada entity yang {@code tanggal}-nya
	 *         {@code null} bisa menghasilkan instan waktu yang sedikit berbeda.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pengajuan")
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	/**
	 * Menetapkan tanggal pengajuan transaksi.
	 *
	 * @param tanggal tanggal pengajuan baru.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** Nilai default {@link #getFormula()} bila belum pernah diisi: JSON array kosong {@code "[]"}. */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Formula/skema perhitungan tambahan (JSON) untuk transaksi ini.
	 *
	 * @return isi formula (JSON), atau {@link #DEFAULT_FORMULA} (array kosong) bila belum diisi.
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Menetapkan formula/skema perhitungan tambahan transaksi ini.
	 *
	 * @param formula isi formula (JSON) baru.
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Menetapkan petugas pembuat transaksi secara manual. Lihat catatan pada {@link #getDibuatOleh()}
	 * — nilai ini bisa DITIMPA oleh getter tsb bila disposisi awal SOP terkait sudah menunjuk
	 * pengaju yang berbeda.
	 *
	 * @param dibuatOleh petugas pembuat baru.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Petugas pembuat transaksi ini.
	 *
	 * <p>Getter memanggil {@code check(...)} lalu, bila {@link #getDisposisiSop()} tersedia dan
	 * disposisi awalnya ({@code getDisposisiStart()}) sudah punya pengaju
	 * ({@code getDiajukanOleh()}), field {@link #dibuatOleh} DITIMPA dgn pengaju disposisi tsb —
	 * disposisi SOP diperlakukan sbg sumber kebenaran yang lebih diutamakan drpd nilai yang di-set
	 * manual lewat {@link #setDibuatOleh(Tbmuser)}.
	 *
	 * @return petugas pembuat transaksi, diutamakan dari disposisi awal SOP bila ada, jika tidak
	 *         nilai field tersimpan (atau {@code null} bila keduanya kosong).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	/**
	 * Menetapkan petugas penyetuju transaksi secara manual. Lihat catatan pada
	 * {@link #getDisetujuiOleh()} — nilai ini bisa DITIMPA (termasuk di-{@code null}-kan) oleh
	 * getter tsb berdasarkan status disposisi setuju SOP terkait.
	 *
	 * @param disetujuiOleh petugas penyetuju baru.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Petugas penyetuju transaksi ini — dipakai a.l. oleh {@link #getStatus()} utk menentukan status
	 * {@link #DISETUJU}.
	 *
	 * <p><b>Prioritas sumber, dgn kemungkinan di-{@code null}-kan otomatis.</b> Urutan resolusi
	 * getter ini: (1) bila disposisi setuju SOP ({@code getDisposisiSop().getDisposisiSetuju()})
	 * ADA dan sudah punya pengaju, field DITIMPA dgn pengaju tsb; (2) SEBALIKNYA, bila disposisi SOP
	 * ada tapi disposisi setuju-nya BELUM ada/belum punya pengaju, field DIPAKSA {@code null} —
	 * artinya persetujuan manual yang pernah di-set lewat {@link #setDisetujuiOleh(Tbmuser)} akan
	 * HILANG begitu entity ini punya {@code disposisiSop} yang belum disetujui; (3) bila masih
	 * {@code null} setelah itu DAN transaksi ini punya {@link #getDaftarPengajuanTransfer()} dgn
	 * proses transfer yang sudah disetujui, jatuh ke penyetuju proses transfer tsb.
	 *
	 * @return petugas penyetuju transaksi, hasil resolusi prioritas disposisi SOP → proses transfer
	 *         → nilai field manual (bisa {@code null}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}

		if (disetujuiOleh == null && getDaftarPengajuanTransfer() != null
				&& getDaftarPengajuanTransfer().getProsesTransfer() != null
				&& getDaftarPengajuanTransfer().getProsesTransfer().getDisetujuiOleh() != null) {
			disetujuiOleh = getDaftarPengajuanTransfer().getProsesTransfer().getDisetujuiOleh();
		}

		return disetujuiOleh;
	}

	/**
	 * Menetapkan waktu persetujuan transaksi secara manual. Lihat catatan pada
	 * {@link #getTanggalPersetujuan()} — nilai ini bisa DITIMPA (termasuk di-{@code null}-kan) oleh
	 * getter tsb berdasarkan status disposisi setuju SOP/proses transfer, KECUALI
	 * {@link #getTanggalPersetujuanManual()} terisi.
	 *
	 * @param tanggalPersetujuan waktu persetujuan baru.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Waktu persetujuan transaksi ini.
	 *
	 * <p>Resolusi mengikuti pola yang sama dgn {@link #getDisetujuiOleh()} (disposisi setuju SOP →
	 * di-{@code null}-kan bila disposisi setuju belum ada → jatuh ke proses transfer bila masih
	 * kosong), DIBUNGKUS {@code try/catch} yang menelan {@code Exception} apa pun dari pemanggilan
	 * {@link #getDisposisiSop()}/{@link #getDaftarPengajuanTransfer()} (dicatat via
	 * {@link ais.common.ErrorAuditUtil#record(Exception, String)}) — proxy Hibernate berbagi
	 * (canonical instance dari {@code AuditTimestampInterceptor}) kadang terikat ke sesi lain yang
	 * sudah closed, jadi kegagalan lazy-load di sini SENGAJA dilewati saja drpd melempar
	 * {@code LazyInitializationException} ke pemanggil (nilai fallback dipertahankan). Setelah blok
	 * itu, bila {@link #getTanggalPersetujuanManual()} terisi DAN {@link #disetujuiOleh} (versi
	 * ter-{@code check}) tidak {@code null}, nilai MANUAL tsb yang menang — override tertinggi di
	 * atas seluruh sumber otomatis.
	 *
	 * @return waktu persetujuan hasil resolusi disposisi SOP/proses transfer, DIKALAHKAN oleh
	 *         {@link #getTanggalPersetujuanManual()} bila keduanya terisi; bisa {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: getDisposisiSop()/getDaftarPengajuanTransfer()
			// bisa berupa instance canonical/shared (AuditTimestampInterceptor) yang proxy-nya
			// terikat ke Session lain yang sudah closed -> jangan biarkan getter ini crash,
			// cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}

			if (disetujuiOleh == null && getDaftarPengajuanTransfer() != null
					&& getDaftarPengajuanTransfer().getProsesTransfer() != null
					&& getDaftarPengajuanTransfer().getProsesTransfer().getTanggalPersetujuan() != null) {
				tanggalPersetujuan = getDaftarPengajuanTransfer().getProsesTransfer().getTanggalPersetujuan();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/koperasi/TransaksiKoperasi.java:getTanggalPersetujuan-lazy");
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
		}

		return tanggalPersetujuan;
	}

	/**
	 * Menetapkan waktu pembuatan transaksi secara manual. Lihat catatan pada
	 * {@link #getTanggalPembuatan()} — nilai ini bisa DITIMPA oleh getter tsb bila disposisi awal
	 * SOP terkait tersedia.
	 *
	 * @param tanggalPembuatan waktu pembuatan baru.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Waktu pembuatan transaksi ini.
	 *
	 * @return waktu disposisi awal SOP diajukan bila tersedia, jika tidak nilai field tersimpan;
	 *         bila keduanya kosong jatuh ke waktu saat ini ({@link WaktuUtil#getDate()},
	 *         dihitung ulang setiap pemanggilan) — TIDAK PERNAH {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Status transaksi ini.
	 *
	 * @return {@link #DISETUJU} bila {@link #getDisetujuiOleh()} terisi (memaksa status naik
	 *         menjadi disetujui begitu ada penyetuju, terlepas dari nilai field {@code status}
	 *         tersimpan), jika tidak nilai field {@code status} apa adanya, atau {@link #PENGAJUAN}
	 *         bila field tsb {@code null}/blank. Getter TIDAK PERNAH mengembalikan {@link #DITOLAK}
	 *         dari override otomatis ini — status ditolak murni berasal dari field tersimpan lewat
	 *         {@link #setStatus(String)}.
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		}
		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Menetapkan status transaksi.
	 *
	 * <p>Efek samping penting: bila {@code status} yang di-set sama dgn {@link #DITOLAK}, setter ini
	 * OTOMATIS memanggil {@link #setDisetujuiOleh(Tbmuser)}({@code null}) dan
	 * {@link #setTanggalPersetujuan(Date)}({@code null}) — menolak transaksi selalu mengosongkan
	 * jejak persetujuan manual sebelumnya (meski, sesuai catatan {@link #getDisetujuiOleh()}/
	 * {@link #getTanggalPersetujuan()}, nilai efektif keduanya tetap bisa diisi ulang oleh getter
	 * dari sumber disposisi SOP/proses transfer pada pemanggilan berikutnya).
	 *
	 * @param status status baru; {@link #DITOLAK} memicu pengosongan penyetuju &amp; tanggal
	 *               persetujuan.
	 */
	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	/**
	 * Satuan kerja pemilik transaksi ini.
	 *
	 * @return satuan kerja {@link #getAnggotaKoperasi()} bila anggota &amp; satuan kerjanya
	 *         tersedia (DIUTAMAKAN drpd field {@link #satuanKerja} tersimpan), jika tidak field
	 *         {@link #satuanKerja} hasil {@code check(...)}; bisa {@code null} bila keduanya kosong.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		anggotaKoperasi = check(anggotaKoperasi);
		if (anggotaKoperasi != null && anggotaKoperasi.getSatuanKerja() != null) {
			satuanKerja = anggotaKoperasi.getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja transaksi secara manual. Lihat catatan pada {@link #getSatuanKerja()}
	 * — nilai ini hanya dipakai getter bila anggota koperasi terkait tidak punya satuan kerja sendiri.
	 *
	 * @param satuanKerja satuan kerja baru.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kode unik gabungan, dipakai constraint {@code unique} tambahan di luar {@link #kode} saja.
	 * Lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/**
	 * Kode unik gabungan header transaksi ini — dibentuk dari {@link #getKode()} disambung
	 * {@code "_"} + id disposisi SOP (bila ada) atau {@code "_"} + {@link #getId()} (bila tidak).
	 * Menjamin keunikan kolom {@code @Column(unique = true)} walau dua transaksi kebetulan punya
	 * {@link #getKode()} yang sama (mis. kode diketik manual/duplikat) — bagian id/disposisi di
	 * belakangnya membedakannya. DIHITUNG ULANG (menimpa field {@link #kodeUnik}) setiap getter
	 * dipanggil, bukan sekali saat entity dibuat.
	 *
	 * @return kode unik gabungan {@code "<kode>_<idDisposisiSopAtauId>"}.
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menetapkan kode unik secara manual. Nilai ini akan DITIMPA oleh {@link #getKodeUnik()} pada
	 * pemanggilan getter berikutnya — setter praktis hanya berguna sesaat sebelum entity dibaca
	 * ulang oleh Hibernate/kode lain.
	 *
	 * @param kodeUnik kode unik baru.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Disposisi alur SOP yang menaungi persetujuan transaksi ini — sumber kebenaran bagi
	 * {@link #getStatus()}, {@link #getAktif()}, {@link #getDibuatOleh()},
	 * {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()}, dan
	 * {@link #getTanggalPembuatan()}. Getter memanggil {@code check(...)} sebelum mengembalikan
	 * nilai utk meresolusi proxy lazy yang mungkin basi.
	 *
	 * @return disposisi SOP terkait, atau {@code null} bila belum ditautkan (mis. transaksi lama
	 *         sebelum alur disposisi dipakai).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan disposisi SOP transaksi ini.
	 *
	 * <p><b>Guard ganda anti-downgrade.</b> Ditolak diam-diam (tidak ada perubahan) bila
	 * {@code disposisiSop} baru bernilai {@code null} atau belum memiliki id (belum tersimpan) —
	 * early return sebelum baris assignment. Baris assignment itu sendiri membawa kondisi kedua yang
	 * SELALU false pada titik itu (karena early return di atas sudah menyaring kasus
	 * {@code disposisiSop == null || getId() == null}), sehingga secara praktis method ini selalu
	 * menetapkan {@code this.disposisiSop = disposisiSop} pada baris manapun yang lolos guard
	 * pertama — kondisi ganda pada ekspresi ternary tsb redundan/tidak pernah memilih cabang
	 * {@code this.disposisiSop} lama, dicatat di sini sbg observasi struktur kode, bukan bug
	 * fungsional (perilaku akhirnya tetap benar: hanya disposisi yang sudah tersimpan yang diterima).
	 *
	 * @param disposisiSop disposisi SOP baru; diabaikan bila {@code null} atau belum memiliki id.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Riwayat posting jurnal akuntansi transaksi ini — terisi begitu mesin posting koperasi
	 * (lihat {@code TransaksiKoperasiAction}/{@code DraftJurnalRingkasanUtil}) berhasil menjurnal
	 * transaksi ini ke buku besar, dgn akun debet/kredit ditentukan dari arah
	 * {@code getProdukKoperasi().getTipeProdukKoperasi().getJenis()} (masuk = debet kas/kredit akun
	 * produk, keluar = sebaliknya).
	 *
	 * @return riwayat posting jurnal, atau {@code null} bila transaksi ini belum/tidak diposting.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan riwayat posting jurnal akuntansi transaksi ini.
	 *
	 * @param postingHistory riwayat posting baru.
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Tahun pembuatan header transaksi ini, dipakai a.l. utk pengelompokan laporan per tahun buku.
	 *
	 * @return tahun pembuatan; bila belum diisi, DIISI SEKALI dgn tahun kalender saat ini dan
	 *         DISIMPAN ke field {@link #tahun} (bukan dihitung ulang tiap panggilan seperti
	 *         {@link #getBulan()} yang serupa) — pemanggilan berikutnya mengembalikan nilai yang
	 *         sudah "dibekukan" ini walau tahun kalender sebenarnya sudah berganti.
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun pembuatan transaksi.
	 *
	 * @param tahun tahun baru.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Nomor surat alur keuangan terkait transaksi ini, dipakai penomoran surat/dokumen keuangan
	 * otomatis.
	 *
	 * @return nomor surat alur keuangan; default {@link NomorSuratAlurKeuangan#TRANSAKSI_KOPERASI_DATA}
	 *         bila belum diisi (nilai ini DISIMPAN ke field, bukan sekadar fallback sesaat), jika
	 *         sudah terisi diresolusi via {@code check(...)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_keuangan", nullable = true)
	public NomorSuratAlurKeuangan getNomorSuratAlurKeuangan() {
		if (nomorSuratAlurKeuangan == null) {
			nomorSuratAlurKeuangan = NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA;
		} else {
			nomorSuratAlurKeuangan = check(nomorSuratAlurKeuangan);
		}
		return nomorSuratAlurKeuangan;
	}

	/**
	 * Menetapkan nomor surat alur keuangan transaksi ini.
	 *
	 * @param nomorSuratAlurKeuangan nomor surat alur keuangan baru.
	 */
	public void setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan nomorSuratAlurKeuangan) {
		this.nomorSuratAlurKeuangan = nomorSuratAlurKeuangan;
	}

	/**
	 * Bulan pembuatan header transaksi ini (1-12).
	 *
	 * @return bulan pembuatan; bila belum diisi, DIHITUNG ULANG SETIAP PANGGILAN dari bulan
	 *         kalender saat ini (berbeda dari {@link #getTahun()} yang membekukan nilainya setelah
	 *         diisi sekali) — TIDAK disimpan permanen sampai field {@link #bulan} benar-benar
	 *         di-{@code set} oleh pemanggil lain/Hibernate.
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menetapkan bulan pembuatan transaksi.
	 *
	 * @param bulan bulan baru (1-12).
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Pengajuan transfer terkait bila transaksi ini dibayar/diterima lewat jalur transfer bank
	 * (bukan kas langsung) — lihat kriteria pemisahan jalur pada Javadoc kelas dan
	 * {@code DraftJurnalRingkasanUtil.kriteriaSimpanPinjam} (jalur kas-langsung mensyaratkan field
	 * ini {@code null}).
	 *
	 * @return pengajuan transfer terkait, atau {@code null} bila transaksi ini jalur kas langsung.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menetapkan pengajuan transfer terkait transaksi ini. Turut mempengaruhi hasil
	 * {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()}, dan
	 * {@link #getTanggalTransaksi()} bila disposisi SOP tidak tersedia.
	 *
	 * @param daftarPengajuanTransfer pengajuan transfer baru.
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Tanggal transaksi efektif — dipakai penentuan periode akuntansi transaksi ini diposting.
	 *
	 * @return utk transaksi transitori: tanggal pembuatan proses transitori-nya; jika tidak, utk
	 *         transaksi via pengajuan transfer: tanggal realisasi proses transfer (atau tanggal
	 *         pembuatannya bila belum direalisasikan); jika tidak ada pengajuan transfer sama
	 *         sekali: jatuh ke {@link #getTanggalPembuatan()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getTransitori()
				&& daftarPengajuanTransfer.getTransitoriData() != null
				&& daftarPengajuanTransfer.getTransitoriData().getProsesTransitori() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getTransitoriData().getProsesTransitori().getTanggalPembuatan();
		} else if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getProsesTransfer() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan() == null
					? daftarPengajuanTransfer.getProsesTransfer().getTanggalPembuatan()
					: daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan();
		} else {
			tanggalTransaksi = getTanggalPembuatan();
		}
		return tanggalTransaksi;
	}

	/**
	 * Menetapkan tanggal transaksi efektif. Lihat catatan pada {@link #getTanggalTransaksi()} —
	 * nilai ini bisa DITIMPA oleh getter tsb bila ada pengajuan transfer/transitori terkait.
	 *
	 * @param tanggalTransaksi tanggal transaksi baru.
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Override manual waktu persetujuan (lihat pemakaiannya sbg penentu akhir pada
	 * {@link #getTanggalPersetujuan()}).
	 *
	 * @return waktu persetujuan manual, atau {@code null} bila tidak dipakai (alur normal lewat
	 *         disposisi SOP).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/**
	 * Menetapkan override manual waktu persetujuan. Lihat {@link #getTanggalPersetujuan()} — nilai
	 * ini hanya berlaku sbg hasil akhir getter tsb bila {@link #getDisetujuiOleh()} juga tidak
	 * {@code null}.
	 *
	 * @param tanggalPersetujuanManual waktu persetujuan manual baru.
	 */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}

	/**
	 * Produk koperasi transaksi ini — menentukan tipe SIMPANAN/PINJAMAN
	 * ({@link ProdukKoperasi#getTipeProdukKoperasi()}), bunga, dan jangka waktu yang dipakai
	 * {@link #getMargin()}/{@link #getTanggalTerakhirDiangsur()}/{@link #getJumlahAngsur()}. Getter
	 * memanggil {@code check(...)} sebelum mengembalikan nilai.
	 *
	 * @return produk koperasi transaksi ini, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_koperasi", nullable = true)
	public ProdukKoperasi getProdukKoperasi() {
		produkKoperasi = check(produkKoperasi);
		return produkKoperasi;
	}

	/**
	 * Menetapkan produk koperasi transaksi ini. Turut mempengaruhi hasil {@link #getNama()}
	 * (auto-generate), {@link #getMargin()} (re-hitung dari bunga produk), dan seluruh getter
	 * jadwal angsuran ({@link #getTanggalMulaiDiangsur()} dst) pada pemanggilan berikutnya.
	 *
	 * @param produkKoperasi produk koperasi baru.
	 */
	public void setProdukKoperasi(ProdukKoperasi produkKoperasi) {
		this.produkKoperasi = produkKoperasi;
	}

	/**
	 * Cara pembayaran/penerimaan yang dipakai transaksi ini (mis. tunai, kas tertentu) — dipakai
	 * mesin posting jurnal utk menentukan akun kas lawan. Getter memanggil {@code check(...)}
	 * sebelum mengembalikan nilai.
	 *
	 * @return cara pembayaran koperasi transaksi ini, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi() {
		caraPembayaranKoperasi = check(caraPembayaranKoperasi);
		return caraPembayaranKoperasi;
	}

	/**
	 * Menetapkan cara pembayaran/penerimaan transaksi ini.
	 *
	 * @param caraPembayaranKoperasi cara pembayaran baru.
	 */
	public void setCaraPembayaranKoperasi(CaraPembayaranKoperasi caraPembayaranKoperasi) {
		this.caraPembayaranKoperasi = caraPembayaranKoperasi;
	}

	/**
	 * Bunga/margin transaksi ini.
	 *
	 * <p><b>Catatan integritas — getter destruktif, DIHITUNG ULANG DARI KONFIGURASI PRODUK SAAT
	 * INI, bukan dibekukan pada saat transaksi dibuat.</b> Setiap kali dipanggil dan
	 * {@link #getProdukKoperasi()} tersedia, getter ini MENIMPA field {@link #margin} dgn
	 * {@code nilai * (produkKoperasi.getBunga() / 100.0) * produkKoperasi.getJangkaWaktuBulan()} —
	 * memakai nilai {@code bunga}/{@code jangkaWaktuBulan} produk SAAT getter dipanggil, BUKAN
	 * nilai yang berlaku saat transaksi ini pertama kali disetujui/dicairkan. Karena
	 * {@code dynamicInsert/dynamicUpdate} membuat Hibernate memanggil getter ini utk membentuk
	 * statement SQL, nilai yang benar-benar tersimpan di kolom {@code margin} pada saat SIMPAN
	 * sudah konsisten dgn bunga produk pada saat itu. Risikonya muncul pada PEMBACAAN ULANG
	 * berikutnya: {@code TransaksiKoperasiAction} dan {@code DraftJurnalRingkasanUtil} memanggil
	 * {@code getMargin()} lagi utk memecah nominal per angsuran (mis. {@code getMargin() /
	 * jumlahAngsur}) dan menyusun ringkasan jurnal — bila admin mengubah {@code bunga}/
	 * {@code jangkaWaktuBulan} pada {@link ProdukKoperasi} SETELAH transaksi lama dibuat, seluruh
	 * transaksi lama yang masih merujuk produk yang sama akan menampilkan/menghitung margin baru
	 * yang BERBEDA dari nilai yang sesungguhnya sudah disetujui/diposting ke jurnal saat itu —
	 * kolom {@code margin} tersimpan di DB pada dasarnya tidak pernah benar-benar dibaca apa
	 * adanya selama relasi produk masih bisa di-resolve. Ini adalah instansiasi dari pola "getter
	 * destruktif" yang berulang pada domain finansial AIS (lihat jugalah pola sejenis pada
	 * {@link #getNama()}) — dicatat di sini sbg observasi arsitektur yang sudah dikenal, bukan
	 * bug baru; mitigasi praktis: hindari mengubah bunga/jangka waktu produk yang sudah dipakai
	 * transaksi historis, atau baca kolom {@code margin} lewat query native/SQL langsung (bukan
	 * lewat entity ini) bila dibutuhkan nilai historis yang benar-benar dibekukan.
	 *
	 * @return margin/bunga transaksi, dihitung ulang dari konfigurasi produk SAAT INI bila produk
	 *         tersedia; {@code 0.0} bila field {@code null} dan produk tidak tersedia.
	 */
	public Double getMargin() {

		if (getProdukKoperasi() != null) {
			margin = (nilai * (produkKoperasi.getBunga() / 100.0)) * produkKoperasi.getJangkaWaktuBulan();
		}

		return margin == null ? 0.0 : margin;
	}

	/**
	 * Menetapkan margin/bunga transaksi secara manual. Lihat catatan penting pada
	 * {@link #getMargin()} — nilai ini akan DITIMPA oleh getter tsb setiap kali dipanggil selama
	 * {@link #getProdukKoperasi()} tersedia.
	 *
	 * @param margin margin baru.
	 */
	public void setMargin(Double margin) {
		this.margin = margin;
	}

	/**
	 * Total transaksi (nominal tagihan/pencairan penuh).
	 *
	 * @return {@link #getNilai()} + {@link #getMargin()} — DIHITUNG ULANG SETIAP PANGGILAN
	 *         (menimpa field {@link #total}), sehingga mewarisi seluruh catatan integritas
	 *         {@link #getMargin()} di atas: bila konfigurasi produk berubah setelah transaksi
	 *         dibuat, hasil {@code getTotal()} pada transaksi lama ikut bergeser mengikuti bunga
	 *         produk yang BARU, bukan yang berlaku saat transaksi tsb dibuat.
	 */
	public Double getTotal() {
		total = getNilai() + getMargin();
		return total;
	}

	/**
	 * Menetapkan total transaksi secara manual. Lihat catatan pada {@link #getTotal()} — nilai ini
	 * akan DITIMPA oleh getter tsb setiap kali dipanggil.
	 *
	 * @param total total baru.
	 */
	public void setTotal(Double total) {
		this.total = total;
	}

	/**
	 * Tanggal mulai jadwal angsuran (khusus produk bertipe PINJAMAN).
	 *
	 * @return utk produk PINJAMAN: tanggal yang sudah tersimpan, atau — bila belum pernah diisi —
	 *         DIISI SEKALI dgn awal bulan depan ({@link WaktuUtil#bulandepan()}) dan disimpan ke
	 *         field; utk produk non-PINJAMAN atau produk belum diisi: field TIDAK diubah/diisi
	 *         sama sekali oleh cabang ini. Setelahnya, method tetap mengembalikan waktu saat ini
	 *         ({@link WaktuUtil#getDate()}, dihitung ulang tiap panggilan) sbg fallback terakhir
	 *         bila field masih {@code null} (mis. produk non-PINJAMAN yang belum pernah diisi
	 *         manual) — TIDAK PERNAH {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulaiDiangsur() {
		if (getProdukKoperasi() != null && getProdukKoperasi().getTipeProdukKoperasi() != null
				&& ConstantValues.PINJAMAN != null
				&& getProdukKoperasi().getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId())) {
			if (tanggalMulaiDiangsur == null) {
				tanggalMulaiDiangsur = WaktuUtil.bulandepan();
			}
		}
		return tanggalMulaiDiangsur == null ? WaktuUtil.getDate() : tanggalMulaiDiangsur;
	}

	/**
	 * Menetapkan tanggal mulai jadwal angsuran.
	 *
	 * @param tanggalMulaiDiangsur tanggal mulai angsuran baru.
	 */
	public void setTanggalMulaiDiangsur(Date tanggalMulaiDiangsur) {
		this.tanggalMulaiDiangsur = tanggalMulaiDiangsur;
	}

	/**
	 * Tanggal jatuh tempo angsuran terakhir (khusus produk bertipe PINJAMAN).
	 *
	 * <p>Untuk produk PINJAMAN: DIHITUNG ULANG SETIAP PANGGILAN (menimpa field) dari
	 * {@link #getTanggalMulaiDiangsur()} ditambah {@code jangkaWaktuBulan} produk — satuan
	 * penambahan bergantung {@link ProdukKoperasi#getPenghitunganBunga()}: {@code BULANAN}
	 * menambah {@code Calendar.MONTH}, selain itu menambah {@code Calendar.YEAR} (perhatikan:
	 * satuan penambahan YEAR memakai angka {@code jangkaWaktuBulan} APA ADANYA, BUKAN dibagi 12 —
	 * jangka waktu "bulan" pada produk dgn skema bunga non-bulanan diperlakukan sbg jumlah TAHUN
	 * yang sama angkanya, bukan dikonversi; perhatikan konfigurasi produk pinjaman dgn
	 * penghitungan bunga selain bulanan agar jangka waktu tidak salah tafsir). Untuk produk
	 * non-PINJAMAN, field DIPAKSA {@code null} (berbeda dari {@link #getTanggalMulaiDiangsur()}
	 * yang justru punya fallback waktu-sekarang).
	 *
	 * @return tanggal jatuh tempo angsuran terakhir utk produk PINJAMAN, atau {@code null} utk
	 *         produk non-PINJAMAN/belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTerakhirDiangsur() {
		if (getProdukKoperasi() != null && getProdukKoperasi().getTipeProdukKoperasi() != null
				&& ConstantValues.PINJAMAN != null
				&& getProdukKoperasi().getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId())) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggalMulaiDiangsur());

			if (produkKoperasi.getPenghitunganBunga().equalsIgnoreCase(ProdukKoperasi.BULANAN)) {
				calendar.set(Calendar.MONTH,
						calendar.get(Calendar.MONTH) + getProdukKoperasi().getJangkaWaktuBulan().intValue());
			} else {
				calendar.set(Calendar.YEAR,
						calendar.get(Calendar.YEAR) + getProdukKoperasi().getJangkaWaktuBulan().intValue());
			}

			tanggalTerakhirDiangsur = calendar.getTime();
		} else {
			tanggalTerakhirDiangsur = null;
		}
		return tanggalTerakhirDiangsur;
	}

	/**
	 * Menetapkan tanggal jatuh tempo angsuran terakhir secara manual. Lihat catatan pada
	 * {@link #getTanggalTerakhirDiangsur()} — nilai ini akan DITIMPA (termasuk di-{@code null}-kan)
	 * oleh getter tsb setiap kali dipanggil.
	 *
	 * @param tanggalTerakhirDiangsur tanggal jatuh tempo terakhir baru.
	 */
	public void setTanggalTerakhirDiangsur(Date tanggalTerakhirDiangsur) {
		this.tanggalTerakhirDiangsur = tanggalTerakhirDiangsur;
	}

	/**
	 * Jumlah angsuran jadwal cicilan pinjaman ini — dipakai a.l. menentukan berapa banyak baris
	 * {@link TransaksiKoperasiDetail} yang perlu dibentuk, dan pembagi nominal per angsuran pada
	 * {@link #getMargin()}/{@link #getNilai()} (lihat {@code TransaksiKoperasiAction}).
	 *
	 * <p>Untuk produk PINJAMAN: DIHITUNG dgn men-simulasikan maju kalender dari
	 * {@link #getTanggalMulaiDiangsur()} sampai SEHARI SEBELUM {@link #getTanggalTerakhirDiangsur()}
	 * ({@code s.set(Calendar.DATE, ... - 1)}), menambah counter setiap loncatan satu periode sesuai
	 * {@link ProdukKoperasi#getDurasi()} (HARIAN/MINGGUAN/BULANAN/TAHUNAN). Untuk produk
	 * non-PINJAMAN yang tersedia: jatuh ke {@link ProdukKoperasi#getJumlahTransaksiTerbentuk()}.
	 * Bila produk sama sekali tidak tersedia: default {@code 1}.
	 *
	 * @return jumlah angsuran hasil simulasi kalender (PINJAMAN), jumlah transaksi terbentuk produk
	 *         (non-PINJAMAN), atau {@code 1} bila produk tidak diisi.
	 */
	public Integer getJumlahAngsur() {

		if (getProdukKoperasi() != null && getProdukKoperasi().getTipeProdukKoperasi() != null
				&& ConstantValues.PINJAMAN != null
				&& getProdukKoperasi().getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId())) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggalMulaiDiangsur());
			Calendar s = ais.ui.util.WaktuUtil.getCalendar();
			s.setTime(getTanggalTerakhirDiangsur());
			s.set(Calendar.DATE, s.get(Calendar.DATE) - 1);
			jumlahAngsur = 0;
			while (calendar.getTime().before(s.getTime())) {
				jumlahAngsur++;
				if (getProdukKoperasi().getDurasi().equals(ProdukKoperasi.HARIAN)) {
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
				} else if (getProdukKoperasi().getDurasi().equals(ProdukKoperasi.MINGGUAN)) {
					calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
				} else if (getProdukKoperasi().getDurasi().equals(ProdukKoperasi.BULANAN)) {
					calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
				} else if (getProdukKoperasi().getDurasi().equals(ProdukKoperasi.TAHUNAN)) {
					calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
				}
			}
		} else if (getProdukKoperasi() != null) {
			jumlahAngsur = getProdukKoperasi().getJumlahTransaksiTerbentuk();
		} else {
			jumlahAngsur = 1;
		}
		return jumlahAngsur;
	}

	/**
	 * Menetapkan jumlah angsuran secara manual. Lihat catatan pada {@link #getJumlahAngsur()} —
	 * nilai ini akan DITIMPA oleh getter tsb setiap kali dipanggil bila produk koperasi tersedia.
	 *
	 * @param jumlahAngsur jumlah angsuran baru.
	 */
	public void setJumlahAngsur(Integer jumlahAngsur) {
		this.jumlahAngsur = jumlahAngsur;
	}

	/**
	 * Nominal yang benar-benar diterima anggota (mis. pencairan pinjaman dikurangi potongan biaya
	 * admin/provisi, bila berlaku).
	 *
	 * @return nominal yang diterima; TIDAK null-safe, bisa {@code null} bila belum diisi (berbeda
	 *         dari kebanyakan getter numerik lain pada kelas ini seperti {@link #getNilai()}) —
	 *         pemanggil perlu menjaga sendiri terhadap {@code null} saat memformat/menjumlahkan
	 *         nilai ini.
	 */
	public Double getYangDiterima() {
		return yangDiterima;
	}

	/**
	 * Menetapkan nominal yang benar-benar diterima anggota.
	 *
	 * @param yangDiterima nominal diterima baru; boleh {@code null} (lihat catatan
	 *                     {@link #getYangDiterima()}).
	 */
	public void setYangDiterima(Double yangDiterima) {
		this.yangDiterima = yangDiterima;
	}

	// ═══════════════════════════════════════════════════════════════════════════════════════
	// Analisis & kualitas pinjaman (SOM USPK) — hasil merge eks-PinjamanAnggota agar tidak ada
	// model ganda. Kolom nullable & getter null-safe: transaksi lama (simpanan/pinjaman yang
	// belum mengisi field ini) tetap berfungsi persis seperti sebelumnya.
	// ═══════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Kualitas/kolektibilitas pinjaman: {@link #KOL_LANCAR}, {@link #KOL_KURANG_LANCAR},
	 * {@link #KOL_RAGU}, atau {@link #KOL_MACET}. Default {@link #KOL_LANCAR}.
	 */
	@Column(name = "kolektibilitas", length = 20)
	public String getKolektibilitas() {
		return kolektibilitas == null || kolektibilitas.trim().isEmpty() ? KOL_LANCAR : kolektibilitas;
	}

	/**
	 * Menetapkan kolektibilitas pinjaman.
	 *
	 * @param kolektibilitas salah satu {@link #KOL_LANCAR}/{@link #KOL_KURANG_LANCAR}/
	 *                       {@link #KOL_RAGU}/{@link #KOL_MACET}.
	 */
	public void setKolektibilitas(String kolektibilitas) {
		this.kolektibilitas = kolektibilitas;
	}

	/** Label ramah pengguna untuk kolektibilitas (untuk grid/kartu UI). Tidak dipersist. */
	@javax.persistence.Transient
	public String getKolektibilitasLabel() {
		String k = getKolektibilitas();
		if (KOL_KURANG_LANCAR.equals(k)) {
			return "Kurang Lancar";
		} else if (KOL_RAGU.equals(k)) {
			return "Ragu-ragu";
		} else if (KOL_MACET.equals(k)) {
			return "Macet";
		}
		return "Lancar";
	}

	/** Uraian agunan/jaminan pinjaman (SOM USPK BAB III). */
	@Column(name = "agunan", columnDefinition = "text")
	public String getAgunan() {
		return agunan;
	}

	/**
	 * Menetapkan uraian agunan/jaminan pinjaman.
	 *
	 * @param agunan uraian agunan baru.
	 */
	public void setAgunan(String agunan) {
		this.agunan = agunan;
	}

	/** Nilai taksiran agunan (rupiah). Untuk validasi batas pinjaman produktif maks 75% agunan. */
	public Double getNilaiAgunan() {
		return nilaiAgunan == null ? 0.0 : nilaiAgunan;
	}

	/**
	 * Menetapkan nilai taksiran agunan.
	 *
	 * @param nilaiAgunan nilai taksiran agunan baru (rupiah).
	 */
	public void setNilaiAgunan(Double nilaiAgunan) {
		this.nilaiAgunan = nilaiAgunan;
	}

	/** Analisis kualitatif — kemauan/watak & komitmen membayar (SOM USPK BAB III). */
	@Column(name = "analisis_kualitatif", columnDefinition = "text")
	public String getAnalisisKualitatif() {
		return analisisKualitatif;
	}

	/**
	 * Menetapkan analisis kualitatif pinjaman.
	 *
	 * @param analisisKualitatif uraian analisis kualitatif baru.
	 */
	public void setAnalisisKualitatif(String analisisKualitatif) {
		this.analisisKualitatif = analisisKualitatif;
	}

	/** Analisis kuantitatif — kemampuan membayar & sumber dana pengembalian (SOM USPK BAB III). */
	@Column(name = "analisis_kuantitatif", columnDefinition = "text")
	public String getAnalisisKuantitatif() {
		return analisisKuantitatif;
	}

	/**
	 * Menetapkan analisis kuantitatif pinjaman.
	 *
	 * @param analisisKuantitatif uraian analisis kuantitatif baru.
	 */
	public void setAnalisisKuantitatif(String analisisKuantitatif) {
		this.analisisKuantitatif = analisisKuantitatif;
	}

}
