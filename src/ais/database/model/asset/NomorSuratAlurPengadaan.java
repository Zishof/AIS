package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.surat.NomorSurat;

/**
 * <h2>NomorSuratAlurPengadaan — katalog/lookup "jenis alur dokumen pengadaan" (BUKAN mesin
 * penomoran; lihat catatan penting di bawah).</h2>
 *
 * <p>
 * Nama kelas ini bisa menyesatkan: berbeda dari {@link ais.database.model.surat.NomorSurat} yang
 * benar-benar menyimpan &amp; menaikkan nomor urut dokumen per tahun/bulan, entity ini murni
 * <b>tabel referensi statis</b> berisi katalog jenis alur pengadaan (mis. "Permintaan Pembelian",
 * "Pemesanan Pembelian", "Penerimaan Barang/Jasa", "Pemakaian Barang", dst — lihat konstanta
 * {@code String} publik di kelas ini) beserta {@link #getKode() kode} tiga-digit dan
 * {@link #getKeterangan() keterangan} bahasa Inggrisnya. Field {@link #getNomorSurat() nomorSurat}
 * hanyalah relasi <i>opsional</i> ke entity {@link ais.database.model.surat.NomorSurat} — mesin
 * penomoran sesungguhnya berada&#42; di kelas itu, bukan di sini.
 * </p>
 *
 * <h3>Implikasi untuk analisis bug "reset nomor per tahun/bulan"</h3>
 * <p>
 * Karena entity ini <b>tidak memiliki logika penomoran apa pun</b> (tidak ada counter, tidak ada
 * field tahun/bulan, tidak ada method generate/increment), ia <b>tidak mewarisi/mereplikasi</b> bug
 * reset tahun/bulan yang pernah ditemukan pada mesin penomoran lain di modul ini (lihat catatan
 * arsitektur pada {@code KelompokAsset}/{@code AssetDetail}). Bug semacam itu — bila ada — akan
 * berada pada {@link ais.database.model.surat.NomorSurat} atau pada kode pemanggil yang
 * menghasilkan nomor dokumen aktual, bukan pada katalog jenis alur ini. Kelas ini sekadar
 * "kamus" nama+kode+keterangan yang dipasangkan ke transaksi (lewat field
 * {@code nomorSuratAlurPengadaan} pada berbagai entity transaksi seperti
 * {@link SaldoAwalMasterAsset}) supaya setiap dokumen tahu ia tergolong alur pengadaan jenis apa.
 * </p>
 *
 * <h3>Bootstrap data via {@link #reloadDefault()}</h3>
 * <p>
 * Karena bersifat data master statis (18 jenis alur tetap, didefinisikan sebagai konstanta
 * {@code String} di kelas ini, bukan dikonfigurasi dinamis oleh admin), entity ini di-<i>seed</i>
 * dan disinkronkan lewat method statis {@link #reloadDefault()}: bila tabel masih kosong, seluruh
 * baris di array {@link #S} (format {@code "kode;nama;keterangan"}) di-<code>INSERT</code> satu
 * per satu di dalam transaksi terpisah masing-masing. Sesudahnya, method yang sama membaca-ulang
 * (atau membuat bila belum ada) setiap baris dan menyimpannya ke field statis publik (mis.
 * {@link #PERMINTAAN_PEMBELIAN_DATA}, {@link #PEMESANAN_PEMBELIAN_DATA}, dst) sehingga kode
 * aplikasi lain dapat merujuk entity ini sebagai konstanta in-memory tanpa query berulang —
 * pola yang lazim untuk data master yang jarang berubah dan sering dirujuk. Dipanggil biasanya
 * sekali saat aplikasi start (lihat pemanggil {@code reloadDefault} di inisialisasi data).
 * </p>
 *
 * <h3>Pemetaan basis data &amp; audit</h3>
 * <p>
 * Dipetakan ke tabel <code>asset.nomor_surat_alur_pengadaan</code>. Field jejak {@code oleh}/
 * {@code olehId}/{@code tanggal_dirubah} diisi otomatis lewat hook
 * {@link javax.persistence.PreUpdate} {@link #onUpdate()}
 * ({@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}), dan setiap perubahan
 * direkam ke tabel revisi Envers karena kelas ditandai {@link org.hibernate.envers.Audited @Audited}.
 * </p>
 *
 * @author AIS
 * @see ais.database.model.surat.NomorSurat
 * @see SaldoAwalMasterAsset
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "nomor_surat_alur_pengadaan")
public class NomorSuratAlurPengadaan extends GeneralValueObject {

	/** Nama alur "Peminjaman Barang" (kode {@code 006}). */
	public static final String PEMINJAMAN_BARANG = "Peminjaman Barang";
	/** Nama alur "Pengembalian Barang" (kode {@code 008}). */
	public static final String PENGEMBALIAN_BARANG = "Pengembalian Barang";
	/** Nama alur "Permintaan Pembelian" (kode {@code 001}). */
	public static final String PERMINTAAN_PEMBELIAN = "Permintaan Pembelian";
//	public static final String PERSETUJUAN_PEMESANAN = "Persetujuan Pemesanan";
	/** Nama alur "Pemesanan Pembelian" (kode {@code 002}). */
	public static final String PEMESANAN_PEMBELIAN = "Pemesanan Pembelian";
	/** Nama alur "Perjanjian Kerjasama" (kode {@code 010}). */
	public static final String PERJANJIAN_KERJASAMA = "Perjanjian Kerjasama";
	/** Nama alur "Penerimaan Barang/Jasa" (kode {@code 003}). */
	public static final String PENERIMAAN_PEMBELIAN = "Penerimaan Barang/Jasa";
	/** Nama alur "Penerimaan Tagihan" (kode {@code 004}). */
	public static final String PENERIMAAN_TAGIHAN = "Penerimaan Tagihan";
	/** Nama alur "Pembayaran Pembelian" (kode {@code 005}). */
	public static final String PEMBAYARAN_PEMBELIAN = "Pembayaran Pembelian";
	/** Nama alur "Pembayaran DP Pembelian" (kode {@code 007}). */
	public static final String PEMBAYARAN_DP_PEMBELIAN = "Pembayaran DP Pembelian";

	/** Nama alur "Pembayaran Termin Pekerjaan" (kode {@code 011}). */
	public static final String PEMBAYARAN_TERMIN_PEKERJAAN = "Pembayaran Termin Pekerjaan";

	/** Nama alur "Penghapusan Barang" (kode {@code 009}). */
	public static final String PENGHAPUSAN_BARANG = "Penghapusan Barang";

	/**
	 * Nama alur "Gaji Pegawai". Catatan: berbagi kode {@code 011} dengan
	 * {@link #PEMBAYARAN_TERMIN_PEKERJAAN} pada bootstrap {@link #reloadDefault()} (bukan kode
	 * {@code 012} seperti pada array {@link #S}) — lihat catatan di {@link #reloadDefault()}.
	 */
	public static final String GAJI_PEGAWAI = "Gaji Pegawai";

	/** Nama alur "Pinjaman Pegawai" (kode {@code 015}). */
	public static final String PINJAMAN_PEGAWAI = "Pinjaman Pegawai";

	/** Nama alur "Pengajuan KPI" (kode {@code 013}). */
	public static final String PENGAJUAN_KPI = "Pengajuan KPI";

	/** Nama alur "Pemakaian Barang" (kode {@code 014}). */
	public static final String PEMAKAIAN_BARANG = "Pemakaian Barang";

	/** Nama alur "Penyedia" (kode {@code 016}). */
	public static final String PENYEDIA = "Penyedia";
	/** Nama alur "Pemilihan Penilaian Vendor" (kode {@code 017}). */
	public static final String PEMILIHAN_PENILAIAN_VENDOR = "Pemilihan Penilaian Vendor";
	/** Nama alur "Reimbursement Pegawai" (kode {@code 018}). */
	public static final String REIMBURSEMENT_PEGAWAI = "Reimbursement Pegawai";

	/**
	 * Data seed default: 18 baris katalog jenis alur dalam format string
	 * {@code "kode;nama;keterangan"} (dipisah {@code ;}), dipakai oleh {@link #reloadDefault()}
	 * untuk mengisi tabel saat masih kosong. Urutan array TIDAK menentukan primary key baris yang
	 * ter-INSERT (IDENTITY auto-generate), hanya konten kode/nama/keterangan masing-masing baris.
	 */
	public static final String[] S = new String[] { "001;" + PERMINTAAN_PEMBELIAN + ";Purchase Request",
			"002;" + PEMESANAN_PEMBELIAN + ";Purchase Order", "003;" + PENERIMAAN_PEMBELIAN + ";Receipt Order",
			"004;" + PENERIMAAN_TAGIHAN + ";Billing Acceptence", "005;" + PEMBAYARAN_PEMBELIAN + ";Purchase Payment",
			"006;" + PEMINJAMAN_BARANG + ";Peminjaman", "007;" + PEMBAYARAN_DP_PEMBELIAN + ";Down Payment",
			"008;" + PENGEMBALIAN_BARANG + ";Pengembalian", "009;" + PENGHAPUSAN_BARANG + ";Penghapusan",
			"010;" + PERJANJIAN_KERJASAMA + ";Perjanjian Kerjasama",
			"011;" + PEMBAYARAN_TERMIN_PEKERJAAN + ";Pembayaran Termin Pekerjaan",
			"012;" + GAJI_PEGAWAI + ";Pembayaran Gaji Pegawai", "013;" + PENGAJUAN_KPI + ";Pengajuan KPI",
			"014;" + PEMAKAIAN_BARANG + ";Pemakaian Barang", "015;" + PINJAMAN_PEGAWAI + ";Pinjaman Pegawai",
			"016;" + PENYEDIA + ";" + PENYEDIA,
			"017;" + PEMILIHAN_PENILAIAN_VENDOR + ";" + PEMILIHAN_PENILAIAN_VENDOR,
			"018;" + REIMBURSEMENT_PEGAWAI + ";Nomor pengajuan Reimbursement Pegawai" };

	/**
	 * Cache in-memory baris {@link #PERMINTAAN_PEMBELIAN} setelah {@link #reloadDefault()}
	 * dipanggil; {@code null} sebelum bootstrap pertama dijalankan.
	 */
	public static NomorSuratAlurPengadaan PERMINTAAN_PEMBELIAN_DATA;
	/** Cache in-memory baris {@link #PEMESANAN_PEMBELIAN}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PEMESANAN_PEMBELIAN_DATA;
	/** Cache in-memory baris {@link #PERJANJIAN_KERJASAMA}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PERJANJIAN_KERJASAMA_DATA;
	/** Cache in-memory baris {@link #PENERIMAAN_PEMBELIAN}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PENERIMAAN_PEMBELIAN_DATA;
	/** Cache in-memory baris {@link #PENERIMAAN_TAGIHAN}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PENERIMAAN_TAGIHAN_DATA;
	/** Cache in-memory baris {@link #PEMBAYARAN_PEMBELIAN}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PEMBAYARAN_PEMBELIAN_DATA;
	/** Cache in-memory baris {@link #PEMBAYARAN_DP_PEMBELIAN}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PEMBAYARAN_DP_PEMBELIAN_DATA;
	/** Cache in-memory baris {@link #PEMBAYARAN_TERMIN_PEKERJAAN}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PEMBAYARAN_TERMIN_PEKERJAAN_DATA;
	/** Cache in-memory baris {@link #PEMINJAMAN_BARANG}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PEMINJAMAN_BARANG_DATA;
	/** Cache in-memory baris {@link #PENGEMBALIAN_BARANG}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PENGEMBALIAN_BARANG_DATA;
	/** Cache in-memory baris {@link #PENGHAPUSAN_BARANG}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PENGHAPUSAN_BARANG_DATA;

	/** Cache in-memory baris {@link #PEMAKAIAN_BARANG}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PEMAKAIAN_BARANG_DATA;

	/** Cache in-memory baris {@link #PINJAMAN_PEGAWAI}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PEMINJAMAN_PEGAWAI;

	/**
	 * Cache in-memory baris {@link #GAJI_PEGAWAI}; diisi oleh {@link #reloadDefault()}. Nama
	 * field ini ("PEMBAYARAN_...") tidak selaras dengan konstanta sumbernya ("GAJI_PEGAWAI",
	 * tanpa awalan PEMBAYARAN) — perbedaan penamaan historis, tidak memengaruhi fungsi.
	 */
	public static NomorSuratAlurPengadaan PEMBAYARAN_GAJI_PEGAWAI;

	/** Cache in-memory baris {@link #PENGAJUAN_KPI}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PENGAJUAN_KPI_PEGAWAI;

	/**
	 * Cache in-memory baris {@link #PENYEDIA}; diisi oleh {@link #reloadDefault()}. Nama field
	 * ("PENGAJUAN_PENYEDIA") juga tidak selaras dengan konstanta sumbernya ("PENYEDIA"), sama
	 * seperti {@link #PEMBAYARAN_GAJI_PEGAWAI}.
	 */
	public static NomorSuratAlurPengadaan PENGAJUAN_PENYEDIA;
	/** Cache in-memory baris {@link #PEMILIHAN_PENILAIAN_VENDOR}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan PEMILIHAN_PENILAIAN_VENDOR_DATA;
	/** Cache in-memory baris {@link #REIMBURSEMENT_PEGAWAI}; diisi oleh {@link #reloadDefault()}. */
	public static NomorSuratAlurPengadaan REIMBURSEMENT_PEGAWAI_DATA;

	/**
	 * Bootstrap &amp; sinkronisasi data master katalog jenis alur pengadaan. Dipanggil biasanya
	 * sekali saat aplikasi start (pemanasan cache data master).
	 *
	 * <p><b>Langkah 1 — seed awal bila tabel kosong:</b> menghitung jumlah baris via
	 * {@code Projections.rowCount()}; bila {@code 0}, setiap entri {@link #S} (format
	 * {@code "kode;nama;keterangan"}, dipisah {@code split(";")}) di-{@code INSERT} satu per satu,
	 * masing-masing dalam transaksi Hibernate terpisah ({@code session.getTransaction().begin()}/
	 * {@code commit()} per baris, bukan satu transaksi gabungan).</p>
	 *
	 * <p><b>Langkah 2 — muat/lengkapi cache in-memory:</b> untuk setiap jenis alur, method ini
	 * mencari baris via {@code Restrictions.eq("nama", ...)} dan menyimpannya ke field statis
	 * publik terkait (mis. {@link #PERMINTAAN_PEMBELIAN_DATA}). Untuk beberapa jenis alur
	 * ({@link #PENYEDIA}, {@link #PEMINJAMAN_BARANG}, {@link #PENGEMBALIAN_BARANG},
	 * {@link #PEMBAYARAN_DP_PEMBELIAN}, {@link #PEMBAYARAN_TERMIN_PEKERJAAN},
	 * {@link #GAJI_PEGAWAI}, {@link #PINJAMAN_PEGAWAI}, {@link #PENGHAPUSAN_BARANG},
	 * {@link #PERJANJIAN_KERJASAMA}, {@link #PENGAJUAN_KPI}, {@link #PEMAKAIAN_BARANG},
	 * {@link #PEMILIHAN_PENILAIAN_VENDOR}, {@link #REIMBURSEMENT_PEGAWAI}), bila baris belum
	 * ditemukan (mis. {@link #S} sempat berubah setelah seed pertama kali dilakukan, atau baris
	 * dihapus manual), method ini membuatnya sendiri secara eksplisit — jaring pengaman kedua di
	 * luar seed massal langkah 1, sehingga field statis tersebut tidak pernah {@code null} setelah
	 * method ini selesai walau tabel sudah pernah di-seed dengan versi {@link #S} yang lebih lama.
	 * Beberapa jenis alur (mis. {@link #PERMINTAAN_PEMBELIAN}, {@link #PEMESANAN_PEMBELIAN},
	 * {@link #PENERIMAAN_PEMBELIAN}, {@link #PENERIMAAN_TAGIHAN}, {@link #PEMBAYARAN_PEMBELIAN})
	 * TIDAK mendapat jaring pengaman kedua ini — hanya diisi dari hasil query langkah 1, sehingga
	 * bila baris tidak ditemukan, field statisnya tetap {@code null} setelah method selesai.</p>
	 *
	 * <p><b>Catatan kode duplikat kode "011":</b> baris seed {@link #GAJI_PEGAWAI} dibuat manual
	 * dengan {@code kode = "011"}, padahal kode itu sudah dipakai {@link #PEMBAYARAN_TERMIN_PEKERJAAN}
	 * baik di {@link #S} (kode {@code "011"}) maupun pada pembuatan manualnya. Kolom {@code kode}
	 * pada entity ini tidak dideklarasikan {@code unique} (tidak seperti {@code kodeUnik} pada
	 * beberapa entity lain di paket ini), sehingga duplikasi ini tidak ditolak database — nilai
	 * kode sekadar label tampilan, bukan pengenal yang divalidasi unik.</p>
	 *
	 * <p>Session Hibernate dipakai lewat {@link ais.database.hibernate.HibernateUtil#currentNativeSession()}
	 * dan ditutup eksplisit di akhir method ({@code session.disconnect()}/{@code close()} lalu
	 * {@code HibernateUtil.closeSession()}) — berbeda dari kebiasaan {@code currentSession()} di
	 * banyak tempat lain yang tidak boleh ditutup manual; pola penutupan manual ini sesuai karena
	 * dipanggil di luar siklus request/response normal (saat inisialisasi aplikasi).</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		int count = ((Number) session.createCriteria(NomorSuratAlurPengadaan.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {

			for (String s : S) {
				NomorSuratAlurPengadaan nomorSuratAlurPengadaan = new NomorSuratAlurPengadaan();
				nomorSuratAlurPengadaan.setKode(s.split(";")[0]);
				nomorSuratAlurPengadaan.setNama(s.split(";")[1]);
				nomorSuratAlurPengadaan.setKeterangan(s.split(";")[2]);
				session.getTransaction().begin();
				session.save(nomorSuratAlurPengadaan);
				session.getTransaction().commit();
			}

		}

		PENGAJUAN_PENYEDIA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PENYEDIA)).setMaxResults(1).uniqueResult();

		PERMINTAAN_PEMBELIAN_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PERMINTAAN_PEMBELIAN)).setMaxResults(1).uniqueResult();

		PEMESANAN_PEMBELIAN_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PEMESANAN_PEMBELIAN)).setMaxResults(1).uniqueResult();

		PENERIMAAN_PEMBELIAN_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PENERIMAAN_PEMBELIAN)).setMaxResults(1).uniqueResult();

		PENERIMAAN_TAGIHAN_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PENERIMAAN_TAGIHAN)).setMaxResults(1).uniqueResult();

		PEMBAYARAN_PEMBELIAN_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PEMBAYARAN_PEMBELIAN)).setMaxResults(1).uniqueResult();

		PEMINJAMAN_BARANG_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PEMINJAMAN_BARANG)).setMaxResults(1).uniqueResult();

		if (PENGAJUAN_PENYEDIA == null) {
			PENGAJUAN_PENYEDIA = new NomorSuratAlurPengadaan();
			PENGAJUAN_PENYEDIA.setKode("016");
			PENGAJUAN_PENYEDIA.setNama(PENYEDIA);
			PENGAJUAN_PENYEDIA.setKeterangan(PENYEDIA);
			session.getTransaction().begin();
			session.save(PENGAJUAN_PENYEDIA);
			session.getTransaction().commit();
		}

		if (PEMINJAMAN_BARANG_DATA == null) {
			PEMINJAMAN_BARANG_DATA = new NomorSuratAlurPengadaan();
			PEMINJAMAN_BARANG_DATA.setKode("006");
			PEMINJAMAN_BARANG_DATA.setNama(PEMINJAMAN_BARANG);
			PEMINJAMAN_BARANG_DATA.setKeterangan("Peminjaman");
			session.getTransaction().begin();
			session.save(PEMINJAMAN_BARANG_DATA);
			session.getTransaction().commit();
		}

		PENGEMBALIAN_BARANG_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PENGEMBALIAN_BARANG)).setMaxResults(1).uniqueResult();
		if (PENGEMBALIAN_BARANG_DATA == null) {
			PENGEMBALIAN_BARANG_DATA = new NomorSuratAlurPengadaan();
			PENGEMBALIAN_BARANG_DATA.setKode("008");
			PENGEMBALIAN_BARANG_DATA.setNama(PENGEMBALIAN_BARANG);
			PENGEMBALIAN_BARANG_DATA.setKeterangan("Pengembalian");
			session.getTransaction().begin();
			session.save(PENGEMBALIAN_BARANG_DATA);
			session.getTransaction().commit();
		}

		PEMBAYARAN_DP_PEMBELIAN_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PEMBAYARAN_DP_PEMBELIAN)).setMaxResults(1).uniqueResult();
		if (PEMBAYARAN_DP_PEMBELIAN_DATA == null) {
			PEMBAYARAN_DP_PEMBELIAN_DATA = new NomorSuratAlurPengadaan();
			PEMBAYARAN_DP_PEMBELIAN_DATA.setKode("007");
			PEMBAYARAN_DP_PEMBELIAN_DATA.setNama(PEMBAYARAN_DP_PEMBELIAN);
			PEMBAYARAN_DP_PEMBELIAN_DATA.setKeterangan("Down Payment");
			session.getTransaction().begin();
			session.save(PEMBAYARAN_DP_PEMBELIAN_DATA);
			session.getTransaction().commit();
		}

		PEMBAYARAN_TERMIN_PEKERJAAN_DATA = (NomorSuratAlurPengadaan) session
				.createCriteria(NomorSuratAlurPengadaan.class).add(Restrictions.eq("nama", PEMBAYARAN_TERMIN_PEKERJAAN))
				.setMaxResults(1).uniqueResult();
		if (PEMBAYARAN_TERMIN_PEKERJAAN_DATA == null) {
			PEMBAYARAN_TERMIN_PEKERJAAN_DATA = new NomorSuratAlurPengadaan();
			PEMBAYARAN_TERMIN_PEKERJAAN_DATA.setKode("011");
			PEMBAYARAN_TERMIN_PEKERJAAN_DATA.setNama(PEMBAYARAN_TERMIN_PEKERJAAN);
			PEMBAYARAN_TERMIN_PEKERJAAN_DATA.setKeterangan("Pembayaran Termin Pekerjaan");
			session.getTransaction().begin();
			session.save(PEMBAYARAN_TERMIN_PEKERJAAN_DATA);
			session.getTransaction().commit();
		}

		PEMBAYARAN_GAJI_PEGAWAI = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", GAJI_PEGAWAI)).setMaxResults(1).uniqueResult();
		if (PEMBAYARAN_GAJI_PEGAWAI == null) {
			PEMBAYARAN_GAJI_PEGAWAI = new NomorSuratAlurPengadaan();
			PEMBAYARAN_GAJI_PEGAWAI.setKode("011");
			PEMBAYARAN_GAJI_PEGAWAI.setNama(GAJI_PEGAWAI);
			PEMBAYARAN_GAJI_PEGAWAI.setKeterangan("Pembayaran Gaji Pegawai");
			session.getTransaction().begin();
			session.save(PEMBAYARAN_GAJI_PEGAWAI);
			session.getTransaction().commit();
		}

		PEMINJAMAN_PEGAWAI = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PINJAMAN_PEGAWAI)).setMaxResults(1).uniqueResult();
		if (PEMINJAMAN_PEGAWAI == null) {
			PEMINJAMAN_PEGAWAI = new NomorSuratAlurPengadaan();
			PEMINJAMAN_PEGAWAI.setKode("015");
			PEMINJAMAN_PEGAWAI.setNama(PINJAMAN_PEGAWAI);
			PEMINJAMAN_PEGAWAI.setKeterangan("Pinjaman Pegawai");
			session.getTransaction().begin();
			session.save(PEMINJAMAN_PEGAWAI);
			session.getTransaction().commit();
		}

		PENGHAPUSAN_BARANG_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PENGHAPUSAN_BARANG)).setMaxResults(1).uniqueResult();
		if (PENGHAPUSAN_BARANG_DATA == null) {
			PENGHAPUSAN_BARANG_DATA = new NomorSuratAlurPengadaan();
			PENGHAPUSAN_BARANG_DATA.setKode("009");
			PENGHAPUSAN_BARANG_DATA.setNama(PENGHAPUSAN_BARANG);
			PENGHAPUSAN_BARANG_DATA.setKeterangan("Penghapusan");
			session.getTransaction().begin();
			session.save(PENGHAPUSAN_BARANG_DATA);
			session.getTransaction().commit();
		}

		PERJANJIAN_KERJASAMA_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PERJANJIAN_KERJASAMA)).setMaxResults(1).uniqueResult();
		if (PERJANJIAN_KERJASAMA_DATA == null) {
			PERJANJIAN_KERJASAMA_DATA = new NomorSuratAlurPengadaan();
			PERJANJIAN_KERJASAMA_DATA.setKode("010");
			PERJANJIAN_KERJASAMA_DATA.setNama(PERJANJIAN_KERJASAMA);
			PERJANJIAN_KERJASAMA_DATA.setKeterangan("Perjanjian Kerjasama");
			session.getTransaction().begin();
			session.save(PERJANJIAN_KERJASAMA_DATA);
			session.getTransaction().commit();
		}

		PENGAJUAN_KPI_PEGAWAI = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PENGAJUAN_KPI)).setMaxResults(1).uniqueResult();
		if (PENGAJUAN_KPI_PEGAWAI == null) {
			PENGAJUAN_KPI_PEGAWAI = new NomorSuratAlurPengadaan();
			PENGAJUAN_KPI_PEGAWAI.setKode("013");
			PENGAJUAN_KPI_PEGAWAI.setNama(PENGAJUAN_KPI);
			PENGAJUAN_KPI_PEGAWAI.setKeterangan(PENGAJUAN_KPI);
			session.getTransaction().begin();
			session.save(PENGAJUAN_KPI_PEGAWAI);
			session.getTransaction().commit();
		}

		PEMAKAIAN_BARANG_DATA = (NomorSuratAlurPengadaan) session.createCriteria(NomorSuratAlurPengadaan.class)
				.add(Restrictions.eq("nama", PEMAKAIAN_BARANG)).setMaxResults(1).uniqueResult();
		if (PEMAKAIAN_BARANG_DATA == null) {
			PEMAKAIAN_BARANG_DATA = new NomorSuratAlurPengadaan();
			PEMAKAIAN_BARANG_DATA.setKode("014");
			PEMAKAIAN_BARANG_DATA.setNama(PEMAKAIAN_BARANG);
			PEMAKAIAN_BARANG_DATA.setKeterangan(PEMAKAIAN_BARANG);
			session.getTransaction().begin();
			session.save(PEMAKAIAN_BARANG_DATA);
			session.getTransaction().commit();
		}

		PEMILIHAN_PENILAIAN_VENDOR_DATA = (NomorSuratAlurPengadaan) session
				.createCriteria(NomorSuratAlurPengadaan.class).add(Restrictions.eq("nama", PEMILIHAN_PENILAIAN_VENDOR))
				.setMaxResults(1).uniqueResult();
		if (PEMILIHAN_PENILAIAN_VENDOR_DATA == null) {
			PEMILIHAN_PENILAIAN_VENDOR_DATA = new NomorSuratAlurPengadaan();
			PEMILIHAN_PENILAIAN_VENDOR_DATA.setKode("017");
			PEMILIHAN_PENILAIAN_VENDOR_DATA.setNama(PEMILIHAN_PENILAIAN_VENDOR);
			PEMILIHAN_PENILAIAN_VENDOR_DATA.setKeterangan(PEMILIHAN_PENILAIAN_VENDOR);
			session.getTransaction().begin();
			session.save(PEMILIHAN_PENILAIAN_VENDOR_DATA);
			session.getTransaction().commit();
		}

		REIMBURSEMENT_PEGAWAI_DATA = (NomorSuratAlurPengadaan) session
				.createCriteria(NomorSuratAlurPengadaan.class).add(Restrictions.eq("nama", REIMBURSEMENT_PEGAWAI))
				.setMaxResults(1).uniqueResult();
		if (REIMBURSEMENT_PEGAWAI_DATA == null) {
			REIMBURSEMENT_PEGAWAI_DATA = new NomorSuratAlurPengadaan();
			REIMBURSEMENT_PEGAWAI_DATA.setKode("018");
			REIMBURSEMENT_PEGAWAI_DATA.setNama(REIMBURSEMENT_PEGAWAI);
			REIMBURSEMENT_PEGAWAI_DATA.setKeterangan("Nomor pengajuan Reimbursement Pegawai");
			session.getTransaction().begin();
			session.save(REIMBURSEMENT_PEGAWAI_DATA);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}

		HibernateUtil.closeSession();
	}

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.nomor_surat_alur_pengadaan}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;

	/**
	 * @return id pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila belum
	 *         pernah diubah sejak dimuat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Nilai {@code null}/kosong diabaikan agar jejak lama tidak
	 * tertimpa hampa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna audit. Nilai {@code null}/kosong diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila
	 *         belum pernah diubah sejak dimuat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu serta
	 * identitas pengguna aktif. Dipicu otomatis oleh Hibernate, tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat objek dibuat, lalu
	 * ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini; tidak pernah {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas {@code id-nama} untuk log/combobox. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode tiga-digit jenis alur (mis. {@code "001"}); TIDAK dideklarasikan unik pada kolomnya. */
	private String kode;
	/** Nama jenis alur pengadaan (mis. "Permintaan Pembelian"); wajib diisi. */
	private String nama;
	/** Keterangan/nama Inggris jenis alur (mis. "Purchase Request"); opsional. */
	private String keterangan;
	/** Relasi opsional ke mesin penomoran dokumen sesungguhnya; lihat catatan javadoc kelas. */
	private NomorSurat nomorSurat;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public NomorSuratAlurPengadaan() {
	}

	/** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama jenis alur pengadaan, sudah di-{@code trim}; wajib diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis alur. Tidak melakukan trim di sisi setter — trimming terjadi hanya saat
	 * dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama jenis alur.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/nama Inggris jenis alur, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return kode tiga-digit jenis alur, boleh {@code null}. */
	public String getKode() {
		return kode;
	}

	/**
	 * Mengisi kode jenis alur. Catatan: kolom ini tidak dideklarasikan {@code unique}, sehingga
	 * duplikasi kode antar baris (lihat catatan di {@link #reloadDefault()} soal kode {@code 011})
	 * tidak ditolak pada level database.
	 *
	 * @param kode kode tiga-digit, boleh {@code null}.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan relasi opsional ke mesin penomoran dokumen sesungguhnya, meresolusi proxy
	 * lazy Hibernate lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link NomorSurat} terkait, atau {@code null} bila jenis alur ini belum dipasangkan
	 *         dengan mesin penomoran manapun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/**
	 * Mengisi relasi ke mesin penomoran dokumen.
	 *
	 * @param nomorSurat mesin penomoran terkait, boleh {@code null}.
	 */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

}
