package ais.action.master.akunting;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.SaldoAwalMasterAssetAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.Pajak;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PertangungjawabanPajakAction — Controller Pertanggungjawaban Pajak</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK GenericAutowireComposer yang bertanggung jawab mengelola
 * halaman pertanggungjawaban pajak dalam modul akuntansi. Halaman ini dipakai
 * oleh bagian keuangan untuk memantau, memperbarui, dan mencetak status pajak
 * (misalnya PPh, PPN) yang belum maupun sudah dibayarkan ke kas negara.
 * Setiap baris data mewakili satu entitas {@link Pajak} yang merupakan pajak
 * yang dipungut dari transaksi pengadaan aset, pertanggungjawaban kas besar,
 * atau transaksi serupa.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Siklus hidup halaman mengikuti pola standar ZK:
 * <ol>
 *   <li>{@code doBeforeCompose} — memanggil {@code Common.doCheckSecurity()}
 *       untuk memastikan pengguna yang mengakses sudah terautentikasi dan
 *       memiliki hak akses ke halaman ini.</li>
 *   <li>{@code doAfterCompose} — menginisialisasi komponen ZUL yang telah
 *       di-autowire (paging, grid, filter tanggal, filter satuan kerja),
 *       mengatur rentang tanggal default (6 bulan lalu hingga besok),
 *       mendaftarkan listener paging dan timer default, lalu memanggil
 *       {@code onSearchDefault} untuk menampilkan data awal.</li>
 *   <li>Inner class {@code PajakRenderer} — merender setiap baris grid,
 *       menampilkan kode pajak, nama, jenis pajak, DPP, nilai pajak, NPWP,
 *       nama wajib pajak, tanggal setor, tanggal transaksi, dan tombol aksi.
 *       Baris yang tidak valid (misalnya {@code jenisPajakBarang} null) akan
 *       dihapus langsung dari database kemudian disembunyikan dari tampilan.</li>
 *   <li>{@code initCriteria} — membangun objek Hibernate {@link Criteria} untuk
 *       query dengan filter satuan kerja (hierarki pohon), rentang tanggal,
 *       status bayar (aktif/tidak aktif), kode, dan nama.</li>
 *   <li>{@code onSearchDefault} — menjalankan query menggunakan {@code initCriteria},
 *       mengisi grid dengan renderer {@code PajakRenderer}.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi berjalan di thread ZK event-dispatcher. Operasi hapus data
 * tidak valid di dalam renderer menggunakan native Hibernate session yang dibuka
 * dan di-commit secara eksplisit, kemudian ditutup melalui
 * {@code Common.closeOpenedSession}. Timer default ZK digunakan untuk operasi
 * yang perlu ditunda satu siklus event (misalnya {@code DaftarPengajuanTransfer.simpanPajak}).
 * Tidak ada thread latar yang dikelola langsung oleh kelas ini.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * - Saat menambah filter baru, tambahkan kondisi di {@code initCriteria} serta
 *   komponen ZUL terkait di file {@code pertangungjawaban_pajak.zul}.<br>
 * - Kolom export Excel dikontrol melalui array {@code contents} di
 *   {@code doAfterCompose}; perbarui juga saat ada kolom model baru.<br>
 * - Perubahan pada entitas {@link Pajak} (nama properti Hibernate) harus
 *   diikuti dengan perubahan pada referensi string di {@code initCriteria}.<br>
 * - Hak akses dikontrol melalui {@link CommonPrivilages}; flag {@code edit}
 *   dipakai untuk mengaktifkan/menonaktifkan checkbox dan datebox di renderer.</p>
 */
public class PertangungjawabanPajakAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * Serial version UID untuk serialisasi kelas ini sebagai komponen ZK.
	 * Nilai ini harus dijaga konsistensinya agar tidak terjadi
	 * {@code InvalidClassException} saat deserialisasi sesi HTTP.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private Paging paging;
	private MyGrid grid;

	/** Wadah tab "Dasbor" (autowire dari pertangungjawaban_pajak.zul). */
	private org.zkoss.zul.Vbox dasborPajakBox;

	private Textbox serachnama;
	private Textbox serachkode;
	private Checkbox searchaktif;
	private Checkbox searchtidakaktif;
	private MyDatebox start;
	private MyDatebox end;

	private MyToolbarbuttonConfig find;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean edit;

	/**
	 * Konstruktor default tanpa argumen yang diperlukan oleh framework ZK
	 * untuk melakukan instansiasi kelas composer ini secara reflektif.
	 * Tidak ada inisialisasi yang dilakukan di sini; semua inisialisasi
	 * dilakukan di {@link #doAfterCompose(Component)}.
	 */
	public PertangungjawabanPajakAction() {
	}

	/**
	 * <b>Tujuan:</b> Melakukan pemeriksaan keamanan sebelum halaman ZUL dikompilasi
	 * dan komponen-komponen di-wire. Metode ini dipanggil oleh framework ZK sebagai
	 * bagian dari siklus hidup komposisi halaman, tepat sebelum komponen apapun
	 * dibuat dari file ZUL.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mendelegasikan pemeriksaan hak akses ke {@code Common.doCheckSecurity()}.
	 * Jika pengguna tidak terotentikasi atau tidak memiliki hak akses, metode tersebut
	 * akan mengarahkan pengguna ke halaman logoff secara otomatis. Setelah itu
	 * memanggil implementasi induk untuk melanjutkan proses komposisi ZUL secara normal.
	 * Pola ini memastikan tidak ada komponen UI yang pernah dirender untuk pengguna
	 * yang tidak berhak, karena pemeriksaan terjadi sedini mungkin dalam siklus hidup.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika {@code Common.doCheckSecurity()} melempar exception atau mengarahkan ke
	 * logoff, eksekusi tidak akan berlanjut ke {@code doAfterCompose}. Exception
	 * dari super dikembalikan ke framework ZK untuk ditangani sesuai konfigurasi
	 * error handler aplikasi.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan {@code Common.doCheckSecurity()} dari metode ini.
	 * Penambahan logika pra-inisialisasi tambahan (seperti pengecekan parameter URL)
	 * sebaiknya dilakukan di {@link #doAfterCompose(Component)}, bukan di sini,
	 * karena komponen ZUL belum tersedia pada titik ini.</p>
	 *
	 * @param page     halaman ZK yang sedang dikompilasi
	 * @param parent   komponen induk tempat composer ini dilampirkan
	 * @param compInfo metadata komponen dari definisi ZUL
	 * @return {@code ComponentInfo} dari implementasi induk, diteruskan ke framework ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan logika bisnis halaman
	 * pertanggungjawaban pajak setelah framework ZK selesai meng-autowire semua
	 * komponen dari file ZUL ke field-field pada kelas ini.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah inisialisasi yang dilakukan secara berurutan:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar autowiring selesai.</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk menerapkan lokalisasi bahasa.</li>
	 *   <li>Memeriksa sesi pengguna; jika tidak ada atau tidak memiliki hak READ,
	 *       pengguna diarahkan ke logoff.</li>
	 *   <li>Mendaftarkan {@code EventListener} pada komponen {@code searchparent}
	 *       (filter satuan kerja) sehingga setiap pemilihan satuan kerja akan memicu
	 *       pencarian ulang data.</li>
	 *   <li>Menginisialisasi {@code SatuanKerjaTreeModel} untuk mendukung pencarian
	 *       hierarki satuan kerja (termasuk turunan).</li>
	 *   <li>Mengatur komponen datebox {@code start} dan {@code end} menjadi readonly
	 *       (hanya dapat diubah melalui datepicker), kemudian menetapkan nilai default:
	 *       {@code start} = 6 bulan lalu, {@code end} = besok.</li>
	 *   <li>Menetapkan flag {@code edit} berdasarkan hak akses UPDATE.</li>
	 *   <li>Menginisialisasi paging dengan listener yang memanggil {@code onSearchDefault}
	 *       setiap kali halaman berubah.</li>
	 *   <li>Membuat timer default ZK yang memanggil {@code onSearchDefault} satu kali
	 *       saat halaman pertama dimuat.</li>
	 *   <li>Mendaftarkan tombol ekspor Excel/cetak dengan daftar kolom yang ditentukan
	 *       dalam array {@code contents}, kemudian menambahkannya ke toolbar.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika pengguna tidak valid, metode akan keluar lebih awal setelah menghapus
	 * atribut sesi dan memanggil {@code Common.goLogoff()}. Exception dari
	 * super.doAfterCompose diteruskan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Array {@code contents} menentukan kolom yang diekspor ke Excel. Jika ada
	 * penambahan kolom pada entitas {@link Pajak}, perbarui array ini. Perhatikan
	 * juga bahwa pemeriksaan null pada {@code start} dan {@code end} diperlukan
	 * karena komponen ini bersifat opsional di ZUL.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai di-autowire oleh framework ZK
	 * @throws Exception jika terjadi error saat inisialisasi komponen atau paging
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "jenisPajakBarang.nama", "dpp", "nilai", "npwp",
				"namaWp", "tanggalStor", "tanggalTransaksi", "pertangungjawaban.kode", "pertangungjawabanKasBesar.kode",
				"saldoAwalMasterAssetDetail.saldoAwal.kode", "satuanKerja.nama", "pajakData", "daftarPengajuanTransfer",
				"postingHistory", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Pajak.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		// Tab "Dasbor": render dasbor pemantauan pajak (asinkron lewat Timer di dalamnya).
		if (dasborPajakBox != null) {
			try {
				ais.action.master.akunting.helper.DasboardPajak.render(dasborPajakBox);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	/**
	 * Handler tombol "Singkronkan" (di ZUL, forward onClick=onSingkronkan). Menyinkronkan data Pajak
	 * dari semua entitas terkait ke pipeline transfer: untuk setiap Pajak yang BELUM memiliki
	 * {@link DaftarPengajuanTransfer}, dibuatkan pengajuan transfer-nya lewat
	 * {@link DaftarPengajuanTransfer#simpanPajak(Pajak)} (idempoten — melewati yang sudah punya).
	 * TIDAK membuat {@link ais.database.model.akunting.ProsesTransfer} otomatis — pembuatan
	 * ProsesTransfer tetap lewat layar "Proses Transfer" agar alur persetujuan tetap berjalan.
	 * Dijalankan via timer (indikator sibuk) dan dibatasi per batch agar request tidak timeout.
	 */
	public void onSingkronkan(Event event) throws Exception {
		if (!CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE)) {
			ais.ui.util.MyMessageboxConfig.show("Mohon maaf, Anda tidak memiliki hak akses untuk menyinkronkan data pajak. Langkah yang dapat dilakukan: (1) Hubungi Administrator untuk meminta hak akses UPDATE pada modul ini; (2) Pastikan akun yang digunakan sudah memiliki role yang sesuai; (3) coba proses ini setelah hak akses diberikan. Jika masih mengalami kendala, hubungi tim teknis.", "Peringatan",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Pajak ke Pengajuan Transfer");
				// 1) Backfill baris Pajak PPh untuk Pembayaran Termin yang belum terwakili (mis. termin
				// lama yang belum di-simpan-ulang setelah fitur PPh termin aktif). Dijalankan LEBIH DULU
				// agar baris pajak baru ikut terhitung/terfilter di langkah sinkron berikutnya.
				int[] hasilTermin = backfillPajakTermin(laporan);
				int baruTermin = hasilTermin[0];
				int kandidatTermin = hasilTermin[1];
				int[] hasil = sinkronkanPajakKePengajuanTransfer(laporan);
				int total = hasil[0];
				int baruPajak = hasil[1];
				int diproses = hasil[2];
				int sudahPajak = diproses - baruPajak; // baris pajak yang sudah ada sebelumnya
				String pesan;
				if (total == 0 && baruTermin == 0) {
					pesan = "Tidak ada pajak yang cocok dengan filter saat ini, jadi tidak ada yang disinkronkan.";
				} else {
					pesan = "Sinkronisasi selesai. " + diproses + " pajak diproses (dari " + total
							+ " yang cocok filter): " + baruPajak + " baris pajak baru dibuat";
					if (sudahPajak > 0) {
						pesan += ", " + sudahPajak + " baris pajak sudah ada";
					}
					if (baruTermin > 0) {
						pesan += ". " + baruTermin + " baris pajak PPh termin dibuat/diperbarui";
					}
					pesan += ". Baris barang/jasa (sumber pengadaan) juga ikut disinkronkan.";
					if (diproses >= MAKS_SINGKRON_PER_KLIK) {
						pesan += " (Baris pajak dibatasi " + MAKS_SINGKRON_PER_KLIK
								+ " per klik — silakan klik lagi bila masih ada.)";
					}
					if (kandidatTermin >= MAKS_BACKFILL_TERMIN) {
						pesan += " (PPh termin dibatasi " + MAKS_BACKFILL_TERMIN
								+ " per klik — silakan klik lagi untuk sisanya.)";
					}
				}
				laporan.tambahCatatan(pesan);
				laporan.selesaikan(new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						onSearchDefault(null);
					}
				});
			}
		}, "Menyinkronkan data pajak ke pengajuan transfer...");
	}

	private static final int MAKS_SINGKRON_PER_KLIK = 1000;

	/**
	 * Batas MAKS detail termin yang di-backfill per klik. Dibuat lebih besar dari
	 * {@link #MAKS_SINGKRON_PER_KLIK} agar sekali klik "Singkronkan" bisa menutup SELURUH PPh termin
	 * pada skala normal (kandidat sudah dipersempit ke PO yang benar-benar memuat pajak, jadi jumlahnya
	 * kecil). Tetap dibatasi supaya satu request tidak timeout.
	 */
	private static final int MAKS_BACKFILL_TERMIN = 5000;

	/**
	 * Backfill baris {@link Pajak} PPh untuk SEMUA detail Pembayaran Termin yang memuat pajak namun
	 * BELUM terwakili di daftar pajak ini.
	 *
	 * <p><b>Kenapa perlu:</b> baris Pajak termin hanya dibuat saat termin DISIMPAN (lewat
	 * {@link Pajak#buatDariTermin}). Termin yang dibuat sebelum fitur PPh termin aktif — atau yang
	 * belum di-<i>simpan ulang</i> setelah deploy — tidak punya baris Pajak, sehingga tidak muncul di
	 * daftar Pertanggungjawaban Pajak (dan otomatis tidak ikut disinkronkan ke DPC). Tombol
	 * "Singkronkan" sebelumnya hanya membuat DaftarPengajuanTransfer dari Pajak yang SUDAH ada, jadi
	 * tidak bisa memperbaiki kasus ini. Method ini menutup celah tersebut.</p>
	 *
	 * <p><b>Cara kerja &amp; kelengkapan:</b> memindai {@code PembayaranTerminMasterAssetDetail} yang
	 * tertaut ke PO yang formula-nya BENAR-BENAR memuat referensi pajak (item ber-{@code "pajak":<id>};
	 * item tanpa PPh tidak menulis key "pajak" — lihat {@code reloadFormula}, {@code put("pajak", null)}
	 * menghapus key), lalu memanggil {@link Pajak#buatDariTermin} untuk SETIAP kandidat. buatDariTermin
	 * idempoten: mencari baris Pajak lama via {@code keyData = id detail} lalu MEMBUAT (bila belum ada)
	 * atau MEMPERBARUI (bila sudah ada). Sengaja TIDAK memakai filter "belum ada baris Pajak" supaya
	 * baris lama yang datanya belum lengkap (mis. kode kosong sehingga tombol Kode/Nama tampil "Revisi")
	 * ikut DIPERBAIKI, bukan hanya yang hilang. Karena PO tanpa pajak DIKELUARKAN dari kandidat,
	 * himpunan yang diproses kecil sehingga satu klik menutup seluruh PPh termin pada skala normal;
	 * buatDariTermin memakai session-nya sendiri sehingga tidak mengganggu currentSession. Diproses dari
	 * termin TERBARU (id desc), dibatasi {@link #MAKS_BACKFILL_TERMIN} per klik demi mencegah timeout;
	 * bila kandidat menyentuh batas, pemanggil memberi tahu agar klik lagi.</p>
	 *
	 * @return array {@code [dibuat, kandidatDipindai]} — {@code dibuat} = baris Pajak PPh termin yang
	 *         berhasil dibuat/diperbarui; {@code kandidatDipindai} = jumlah kandidat yang diambil (bila
	 *         == {@link #MAKS_BACKFILL_TERMIN} berarti masih mungkin ada sisa untuk klik berikutnya)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private int[] backfillPajakTermin(ais.common.LaporanUpload laporan) {
		List<Long> ids = new java.util.ArrayList<Long>();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			ids = session
					.createQuery("select d.id from ais.database.model.asset.PembayaranTerminMasterAssetDetail d "
							+ "where d.pemesananPengadaanMasterAsset is not null "
							// Hanya PO yang formula-nya memuat pajak sungguhan ("pajak":<angka>).
							// Item tanpa PPh tak menulis key "pajak" sama sekali. Guard '"pajak":null'
							// untuk berjaga bila varian org.json menyimpan null alih-alih menghapus key.
							+ "and d.pemesananPengadaanMasterAsset.formula like '%\"pajak\":%' "
							+ "and d.pemesananPengadaanMasterAsset.formula not like '%\"pajak\":null%' "
							// TANPA filter "belum punya baris Pajak": buatDariTermin idempoten (create ATAU
							// update via keyData), sehingga baris lama berkode kosong ikut diperbaiki.
							+ "order by d.id desc")
					.setMaxResults(MAKS_BACKFILL_TERMIN).list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new int[] { 0, 0 };
		} finally {
			if (session != null) {
				try {
					if (session.isOpen()) {
						session.close();
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/PertangungjawabanPajakAction.java:409");
				}
			}
		}

		int baru = 0;
		int nomorBaris = 0;
		for (Long id : ids) {
			if (id == null) {
				continue;
			}
			String kunci = "Termin ID:" + id;
			try {
				// buatDariTermin hanya butuh id detail (di-reload di dalamnya via session sendiri),
				// jadi cukup kirim stub agar tidak perlu menahan session di sini.
				ais.database.model.asset.PembayaranTerminMasterAssetDetail stub = new ais.database.model.asset.PembayaranTerminMasterAssetDetail();
				stub.setId(id);
				if (Pajak.buatDariTermin(stub)) {
					baru++;
					laporan.catatBerhasil(nomorBaris, kunci, "Baris pajak PPh termin dibuat/diperbarui");
				} else {
					laporan.catatDilewati(nomorBaris, kunci, "Tidak ada perubahan (buatDariTermin mengembalikan false)");
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				laporan.catatGagalDetail(nomorBaris, kunci, e);
			}
			nomorBaris++;
		}
		return new int[] { baru, ids.size() };
	}

	/**
	 * Memuat Pajak yang BELUM punya DaftarPengajuanTransfer (maks {@link #MAKS_SINGKRON_PER_KLIK})
	 * SESUAI FILTER PENCARIAN yang sedang diterapkan, lalu membuat pengajuan transfer-nya satu per
	 * satu. Memakai ulang {@link #initCriteria(boolean)} agar filter (Judul/Kode/Status/Satuan Kerja/
	 * rentang Tanggal/aktif-tidakaktif) PERSIS sama dengan yang tampil di grid; ditambah syarat
	 * {@code daftarPengajuanTransfer IS NULL}. Query memakai {@code currentSession} (TIDAK ditutup
	 * manual). Tiap Pajak di-evict dari currentSession sebelum diproses agar {@code simpanPajak}
	 * (yang memakai native session sendiri) bisa meng-attach ulang tanpa konflik antar-sesi.
	 *
	 * <p>CATATAN: grid {@code PajakRenderer} ikut memanggil {@code simpanPajak} untuk tiap baris yang
	 * DITAMPILKAN, jadi pajak yang sedang terlihat biasanya SUDAH punya pengajuan saat tombol ditekan
	 * (hasil "baru dibuat" = 0 untuk baris yang tampil). Tombol ini berguna terutama untuk menyinkron
	 * pajak terfilter yang BELUM ditampilkan (halaman lain). Karena itu hasil dikembalikan sebagai
	 * rincian agar pesan jelas.</p>
	 *
	 * @return array {@code [totalTerfilter, baruDibuat, jumlahKandidat]} (kandidat = yang belum punya)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private int[] sinkronkanPajakKePengajuanTransfer(ais.common.LaporanUpload laporan) {
		int total = 0;
		java.util.List<Pajak> semua = new java.util.ArrayList<Pajak>();
		try {
			// Total pajak yang cocok filter (untuk pesan yang informatif).
			Number n = (Number) initCriteria(false)
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			total = n == null ? 0 : n.intValue();

			// Proses SEMUA pajak yang cocok filter (bukan hanya yang belum punya), agar baris
			// BARANG/JASA (sumber) tetap dibuat walau baris PAJAK-nya sudah ada. TIDAK di-evict:
			// getter sumber bersifat lazy (perlu sesi), dan simpanPajak/simpan sumber memakai native
			// session sendiri (pola sama dengan renderer yang juga memanggil simpanPajak).
			List hasil = initCriteria(false).setMaxResults(MAKS_SINGKRON_PER_KLIK).list();
			semua = new java.util.ArrayList<Pajak>(hasil);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new int[] { 0, 0, 0 };
		}

		int baruPajak = 0;
		int nomorBaris = 0;
		for (java.util.Iterator<Pajak> it = semua.iterator(); it.hasNext();) {
			Pajak p = it.next();
			if (p == null) {
				continue;
			}
			String kunci = String.valueOf(p);
			try {
				boolean baru = false;
				// 1) Baris PAJAK (pembayaran pajak ke kas negara).
				if (p.getDaftarPengajuanTransfer() == null) {
					DaftarPengajuanTransfer.simpanPajak(p);
					baruPajak++;
					baru = true;
				}
				// 2) Baris BARANG/JASA (sumber yang dipajaki) — row TERPISAH.
				simpanSumberBarangJasa(p);
				laporan.catatBerhasil(nomorBaris, kunci,
						baru ? "Baris pajak baru dibuat + barang/jasa disinkronkan"
								: "Baris pajak sudah ada, barang/jasa disinkronkan");
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				laporan.catatGagalDetail(nomorBaris, kunci, e);
			}
			nomorBaris++;
		}
		return new int[] { total, baruPajak, semua.size() };
	}

	/**
	 * Memastikan baris BARANG/JASA (sumber yang dipajaki) juga punya pengajuan transfer TERPISAH dari
	 * baris pajak. Untuk pengadaan (SaldoAwalMasterAsset) memakai
	 * {@link DaftarPengajuanTransfer#simpanSaldoAwalMasterAsset} (nominal vendor = netto = bruto-PPh).
	 * Untuk sumber Pertanggungjawaban / Kas Besar memakai method masing-masing (keduanya idempoten;
	 * CATATAN: kedua method itu saat ini dinonaktifkan di model — lihat {@code if(true) return} —
	 * sehingga baris barang/jasa untuk sumber tersebut belum dibuat sampai method-nya diaktifkan).
	 */
	private void simpanSumberBarangJasa(Pajak p) {
		if (p == null) {
			return;
		}
		try {
			// Pajak BREAKDOWN: tertaut LANGSUNG ke tagihan vendor (tanpa detail PO) -> baris
			// netto vendor dibuat dari saldoAwal yang tertaut langsung pada Pajak.
			if (p.getSaldoAwal() != null) {
				DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset(p.getSaldoAwal());
				return;
			}
			SaldoAwalMasterAssetDetail det = p.getSaldoAwalMasterAssetDetail();
			if (det != null && det.getSaldoAwal() != null) {
				DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset(det.getSaldoAwal());
				return;
			}
			if (p.getPertangungjawaban() != null) {
				DaftarPengajuanTransfer.simpanPertangungjawaban(p.getPertangungjawaban());
				return;
			}
			if (p.getPertangungjawabanKasBesar() != null) {
				DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar(p.getPertangungjawabanKasBesar());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * <h3>PajakRenderer — Renderer Baris Grid Pajak</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class ini bertanggung jawab merender setiap baris pada grid
	 * pertanggungjawaban pajak. Setiap objek {@link Pajak} dari hasil query
	 * Hibernate diubah menjadi komponen-komponen ZK visual yang ditampilkan
	 * dalam satu baris grid. Renderer ini juga menangani logika bisnis level
	 * tampilan seperti validasi data, penyimpanan otomatis saat status berubah,
	 * dan penampilan informasi terkait SOP atau status transfer.</p>
	 *
	 * <p><b>Cara kerja render:</b><br>
	 * Untuk setiap baris:
	 * <ol>
	 *   <li>Memeriksa apakah {@code jenisPajakBarang} null pada detail aset terkait;
	 *       jika null, data pajak dianggap tidak valid dan akan dihapus dari database
	 *       menggunakan native session, kemudian baris disembunyikan.</li>
	 *   <li>Jika {@code daftarPengajuanTransfer} belum ada, membuat timer untuk
	 *       menyimpannya di latar belakang melalui {@code DaftarPengajuanTransfer.simpanPajak}.</li>
	 *   <li>Menampilkan kolom: kode (dengan widget revisi), nama, jenis pajak, DPP,
	 *       nilai pajak, NPWP, nama WP, tanggal setor, tanggal transaksi, keterangan,
	 *       link SOP jika ada, status transfer jika ada.</li>
	 *   <li>Checkbox "Bayar" memungkinkan pengguna mengubah status pembayaran secara
	 *       inline; perubahan langsung disimpan ke database via {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li>Datebox tanggal transaksi dapat diubah hanya jika status bayar aktif.</li>
	 *   <li>Tombol cetak membuka laporan cetak yang sesuai: jika pajak berasal dari
	 *       pertanggungjawaban, cetak dokumen pertanggungjawaban; jika dari saldo awal
	 *       aset, cetak dokumen saldo awal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b><br>
	 * Semua listener event berjalan di thread ZK event-dispatcher. Penghapusan
	 * data tidak valid menggunakan {@code HibernateUtil.currentNativeSession()}
	 * dengan transaksi eksplisit dan diakhiri dengan {@code closeOpenedSession}.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika ada kolom baru yang perlu ditambahkan di grid, tambahkan juga di file
	 * ZUL terkait dan di metode render ini secara sinkron agar urutan kolom tetap
	 * sesuai. Perhatikan bahwa operasi null-check pada {@code pajak} dan
	 * {@code saldoAwalMasterAssetDetail} penting untuk mencegah NPE.</p>
	 */

	/**
	 * Memformat angka secara aman untuk ditampilkan. Mengembalikan string kosong bila {@code nilai}
	 * null; memformat dengan {@link Common#numberFormat} bila berupa {@link Number}; selain itu
	 * mengembalikan representasi teks apa adanya. Mencegah
	 * {@code IllegalArgumentException: Cannot format given Object as a Number} pada DecimalFormat
	 * ketika nilai yang diterima ternyata bukan Number.
	 */
	static String fmtAngkaAman(Object nilai) {
		if (nilai == null) {
			return "";
		}
		if (nilai instanceof Number) {
			return Common.numberFormat.get().format(((Number) nilai).doubleValue());
		}
		return String.valueOf(nilai);
	}

	class PajakRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris grid dengan data dari objek {@link Pajak}
		 * yang diberikan, menampilkan semua kolom yang relevan beserta kontrol
		 * interaktif untuk mengubah status pembayaran pajak secara inline.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Metode ini dipanggil oleh framework ZK untuk setiap elemen dalam model
		 * daftar yang diset ke grid. Alur kerjanya:
		 * <ol>
		 *   <li>Menetapkan alignment vertikal baris ke "top".</li>
		 *   <li>Melakukan validasi: jika detail aset terkait ({@code saldoAwalMasterAssetDetail})
		 *       ada tetapi {@code jenisPajakBarang}-nya null, record tidak valid —
		 *       dihapus menggunakan native Hibernate session dan baris disembunyikan.</li>
		 *   <li>Jika {@code daftarPengajuanTransfer} belum ada, menjadwalkan pembuatannya
		 *       melalui timer ZK agar tidak memblokir render.</li>
		 *   <li>Menampilkan kolom teks satu per satu sesuai urutan kolom grid ZUL.</li>
		 *   <li>Menampilkan keterangan dan link SOP (jika ada) dalam Vbox yang sama.</li>
		 *   <li>Menampilkan status transfer dari {@code DaftarPengajuanTransfer} bila ada.</li>
		 *   <li>Membuat checkbox "Bayar" yang terhubung langsung ke field {@code aktif}
		 *       pada entitas; setiap perubahan langsung disimpan.</li>
		 *   <li>Membuat datebox tanggal transaksi yang dinonaktifkan jika belum bayar;
		 *       otomatis terisi tanggal hari ini saat checkbox dicentang.</li>
		 *   <li>Menambahkan tombol cetak dengan ikon printer yang membuka dokumen
		 *       sumber yang sesuai.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Null check dilakukan pada setiap relasi entitas sebelum diakses. Operasi
		 * penghapusan data tidak valid menggunakan blok try-native-session dengan
		 * commit eksplisit. Jika terjadi error pada penghapusan, exception akan
		 * diteruskan ke framework ZK.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Urutan pemanggilan {@code setParent(arg0)} harus sesuai persis dengan
		 * urutan {@code <column>} di file ZUL agar kolom tidak geser. Jika ada
		 * kolom baru ditambahkan ke ZUL, tambahkan juga di sini pada posisi yang
		 * tepat.</p>
		 *
		 * @param arg0 baris ZK ({@link Row}) yang akan diisi komponen-komponen UI
		 * @param arg1 objek data, dicast ke {@link Pajak}, yang berisi data satu
		 *             record pajak dari hasil query Hibernate
		 * @throws Exception jika terjadi error pada operasi database atau komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Pajak pajak = (Pajak) arg1;

			SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = pajak.getSaldoAwalMasterAssetDetail();
			if (pajak != null && saldoAwalMasterAssetDetail != null
					&& saldoAwalMasterAssetDetail.getJenisPajakBarang() == null) {
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshDelete(session, pajak);
				session.getTransaction().commit();
				// session.disconnect();
				ais.common.Common.closeOpenedSession(session);
				arg0.setVisible(false);
				return;
			}

			if (pajak != null && pajak.getDaftarPengajuanTransfer() == null) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						DaftarPengajuanTransfer.simpanPajak(pajak);
					}
				});
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pajak.class, pajak,
					pajak.getKode() == null ? "" : pajak.getKode().trim().toString())).setParent(arg0);
			new Label(pajak.getNama()).setParent(a);

			new Label(pajak.getJenisPajakBarang() == null ? "" : pajak.getJenisPajakBarang().getNama()).setParent(arg0);

			new Label(fmtAngkaAman(pajak.getDpp())).setParent(arg0);

			new Label(fmtAngkaAman(pajak.getNilai())).setParent(arg0);

			new Label(pajak.getNpwp()).setParent(arg0);
			new Label(pajak.getNamaWp()).setParent(arg0);
			new Label(pajak.getTanggalStor() == null ? "" : Common.dateFormat3.get().format(pajak.getTanggalStor()))
					.setParent(arg0);
			new Label(pajak.getTanggal() == null ? "" : Common.dateFormat3.get().format(pajak.getTanggal()))
					.setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new Label(Common.simpleString(pajak.getKeterangan())).setParent(vbox1);
			if (pajak.getPertangungjawaban() != null && pajak.getPertangungjawaban().getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pajak.getPertangungjawaban().getDisposisiSop().getKeterangan()
						+ " (" + pajak.getPertangungjawaban().getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pajak.getPertangungjawaban().getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			if (pajak.getPertangungjawaban() != null
					&& pajak.getPertangungjawaban().getDaftarPengajuanTransfer() != null) {
				DaftarPengajuanTransfer.tampilStatus(pajak.getPertangungjawaban().getDaftarPengajuanTransfer(), vbox1);
			}

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Bayar");
			final MyDatebox tanggalTransaksi = new MyDatebox(pajak.getTanggalTransaksi());
			aktif.setChecked(pajak.getAktif());
			aktif.setParent(arg0);
			aktif.setDisabled(!edit);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					tanggalTransaksi.setDisabled(!edit || !aktif.isChecked());
					if (aktif.isChecked() && tanggalTransaksi.getValue() == null) {
						tanggalTransaksi.setValue(WaktuUtil.getDate());
					} else if (!aktif.isChecked() && tanggalTransaksi.getValue() != null) {
						tanggalTransaksi.setValue(null);
					}

					pajak.setTanggalTransaksi(tanggalTransaksi.getValue());
					pajak.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(pajak);
				}
			});

			tanggalTransaksi.setParent(arg0);
			tanggalTransaksi.setDisabled(!edit || !aktif.isChecked());
			tanggalTransaksi.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tanggalTransaksi.setDisabled(!edit || !aktif.isChecked());
					if (aktif.isChecked() && tanggalTransaksi.getValue() == null) {
						tanggalTransaksi.setValue(WaktuUtil.getDate());
					} else if (!aktif.isChecked() && tanggalTransaksi.getValue() != null) {
						tanggalTransaksi.setValue(null);
					}
					pajak.setAktif(aktif.isChecked());
					pajak.setTanggalTransaksi(tanggalTransaksi.getValue());
					Common.refreshSaveOrUpdate(pajak);
				}
			});

			Hbox hbx = new Hbox();
			hbx.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					if (pajak.getPertangungjawaban() != null) {
						PertangungjawabanAction.cetak(pajak.getPertangungjawaban());
					} else if (pajak.getSaldoAwalMasterAssetDetail() != null
							&& pajak.getSaldoAwalMasterAssetDetail().getSaldoAwal() != null) {
						SaldoAwalMasterAssetAction.cetak(pajak.getSaldoAwalMasterAssetDetail().getSaldoAwal());
					}
				}
			});
			button.setParent(hbx);
		}

	}

	/**
	 * <b>Tujuan:</b> Membangun objek Hibernate {@link Criteria} yang digunakan
	 * untuk melakukan query data {@link Pajak} sesuai filter yang dipilih pengguna
	 * di toolbar pencarian.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode ini mendukung dua mode: dengan pengurutan ({@code order=true}) untuk
	 * pengambilan data aktual, dan tanpa pengurutan ({@code order=false}) untuk
	 * penghitungan total record (keperluan paging).
	 *
	 * Filter yang dibangun mencakup:
	 * <ul>
	 *   <li><b>Satuan kerja:</b> Jika filter {@code searchparent} terisi, query
	 *       dibatasi ke satuan kerja yang dipilih beserta seluruh turunannya dalam
	 *       hierarki (menggunakan {@code SatuanKerjaTreeModel.getChildsSet}).
	 *       Jika tidak terisi, menggunakan semua satuan kerja yang diperoleh dari
	 *       {@code SekolahUtil.ambilSatuanKerjas()}.</li>
	 *   <li><b>Rentang tanggal:</b> Filter tanggal transaksi ({@code tanggal})
	 *       menggunakan SQL raw {@code date()} untuk perbandingan tanggal saja
	 *       (tanpa komponen waktu). Jika komponen {@code start} atau {@code end}
	 *       null, filter ini diabaikan.</li>
	 *   <li><b>Status bayar:</b> Kombinasi checkbox aktif/tidak aktif menentukan
	 *       apakah hanya data bayar, belum bayar, atau keduanya yang ditampilkan.</li>
	 *   <li><b>Kode:</b> Filter ILIKE (case-insensitive) pada field {@code kode}.</li>
	 *   <li><b>Nama:</b> Filter ILIKE (case-insensitive) pada field {@code nama}.</li>
	 * </ul>
	 *
	 * Jika {@code order=true}, criteria diurutkan berdasarkan {@code id} descending
	 * sehingga data terbaru muncul di atas.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Tidak ada exception yang ditangkap di sini. Jika nilai filter tidak valid
	 * (misalnya format tanggal salah), Hibernate akan melempar exception yang
	 * akan diteruskan ke framework ZK untuk ditangani.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Gunakan alias Hibernate jika perlu join ke relasi lain untuk filter tambahan.
	 * Hindari penggunaan {@code sqlRestriction} yang berisi nilai langsung dari
	 * input pengguna tanpa escaping untuk mencegah SQL injection. Dalam konteks ini
	 * aman karena nilai tanggal berasal dari objek {@code Date} yang diformat
	 * menggunakan {@code Common.databaseDateFormat}.</p>
	 *
	 * @param order {@code true} jika criteria harus menyertakan klausa ORDER BY
	 *              (untuk pengambilan data), {@code false} untuk query COUNT (paging)
	 * @return objek {@link Criteria} Hibernate yang telah dikonfigurasi dengan semua
	 *         filter aktif
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Pajak.class)

				// Sembunyikan baris PPh PER-ITEM (tertaut ke detail aset) bila tagihan
				// vendornya memakai breakdown = ya. Pada mode breakdown, PPh yang sah =
				// "Bukti Potong" (baris pajak tertaut LANGSUNG ke tagihan, tanpa detail),
				// BUKAN PPh per-item hasil DPP x %. LEFT JOIN dipakai agar baris Bukti
				// Potong (saldoAwalMasterAssetDetail = null) TETAP tampil; hanya baris
				// per-item yang tagihannya breakdown=ya yang disaring. Filter TAMPILAN ini
				// robust untuk record lama yang baris per-item-nya belum sempat dinonaktifkan
				// oleh BreakdownTagihanVendorHelper.sinkronPajakBreakdown.
				.createAlias("saldoAwalMasterAssetDetail", "bdDetail", Criteria.LEFT_JOIN)
				.createAlias("bdDetail.saldoAwal", "bdTagihan", Criteria.LEFT_JOIN)
				.add(Restrictions.disjunction()
						.add(Restrictions.isNull("bdDetail.id"))
						.add(Restrictions.isNull("bdTagihan.breakdownAktif"))
						.add(Restrictions.eq("bdTagihan.breakdownAktif", false)))

//				.add(Restrictions.ge("nilai", 0.01))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchtidakaktif.isChecked() && searchaktif == null || searchaktif.isChecked() ? Restrictions.sqlRestriction("true")
						: searchaktif == null || searchaktif.isChecked()
								? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
								: Restrictions.eq("aktif", false))

				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: buatFilterKodePengajuan(session, serachkode.getValue().trim()))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	/** Cache hasil resolusi keyData termin untuk sepasang pemanggilan initCriteria (count + list). */
	private transient String cacheKodeIndukSearch = null;
	private transient java.util.Set<Long> cacheKeyDataTermin = null;

	/**
	 * Bangun filter kolom "Kode Pengajuan" yang mencocokkan BUKAN HANYA {@code Pajak.kode} tersimpan,
	 * tetapi juga kode INDUK/sumber pajak-nya: PR, PO, BAST, dan Terima Tagihan. Untuk baris pajak
	 * TERMIN (Opsi B, tanpa relasi induk) hal ini diselesaikan dengan menelusuri termin-detail yang
	 * kode induknya cocok lalu mencocokkan {@code keyData} = id termin-detail (lihat
	 * {@link #keyDataTerminByKodeInduk}).
	 *
	 * @return {@code OR(ilike kode, (keyData in [...] AND baris termin))}
	 */
	private org.hibernate.criterion.Criterion buatFilterKodePengajuan(Session session, String search) {
		org.hibernate.criterion.Disjunction or = Restrictions.disjunction();
		// (a) kode yang tersimpan langsung di baris Pajak (kode induk terpilih, mis. Terima Tagihan).
		or.add(Restrictions.ilike("kode", search, MatchMode.ANYWHERE));
		// (b) baris pajak TERMIN yang kode induknya (PR/PO/BAST/Terima Tagihan) cocok.
		java.util.Set<Long> keyDatas = keyDataTerminByKodeInduk(session, search);
		if (keyDatas != null && !keyDatas.isEmpty()) {
			org.hibernate.criterion.Conjunction termin = Restrictions.conjunction();
			termin.add(Restrictions.in("keyData", keyDatas));
			termin.add(Restrictions.isNull("saldoAwalMasterAssetDetail"));
			termin.add(Restrictions.isNull("saldoAwal"));
			termin.add(Restrictions.isNull("pertangungjawaban"));
			termin.add(Restrictions.isNull("pertangungjawabanKasBesar"));
			or.add(termin);
		}
		return or;
	}

	/**
	 * Kumpulkan id {@code PembayaranTerminMasterAssetDetail} yang kode INDUK-nya (PR / PO / BAST /
	 * Terima Tagihan) mengandung {@code search}. Dipakai untuk memfilter baris pajak termin lewat
	 * {@code keyData}. Hasil di-cache per string pencarian karena {@code initCriteria} dipanggil dua
	 * kali (count lalu list) dengan filter yang sama.
	 *
	 * <ul>
	 *   <li>PO: {@code PembayaranTerminMasterAssetDetail.pemesananPengadaanMasterAsset.kode}</li>
	 *   <li>BAST: {@code PenerimaanPengadaanMasterAsset.kode} yang PO-nya sama dengan PO termin</li>
	 *   <li>Terima Tagihan: {@code BAST.saldoAwalMasterAsset.kode / .kodeTagihan}</li>
	 *   <li>PR: kode PR → id PR-detail → PO yang {@code permintaanPengadaanMasterAssets} (CSV) memuatnya</li>
	 * </ul>
	 */
	private java.util.Set<Long> keyDataTerminByKodeInduk(Session session, String search) {
		if (search != null && search.equals(cacheKodeIndukSearch) && cacheKeyDataTermin != null) {
			return cacheKeyDataTermin;
		}
		java.util.Set<Long> td = new java.util.HashSet<Long>();
		if (session != null && search != null && !search.trim().isEmpty()) {
			String q = "%" + search.trim() + "%";
			final String TD = "ais.database.model.asset.PembayaranTerminMasterAssetDetail";
			final String BAST = "ais.database.model.asset.PenerimaanPengadaanMasterAsset";
			final String PRD = "ais.database.model.asset.PermintaanPengadaanMasterAssetDetail";
			// Tiap query di-isolasi try/catch sendiri (di tambahIdTermin) agar kegagalan satu level
			// tidak membatalkan level lain.
			// PO
			tambahIdTermin(session, td, "select d.id from " + TD + " d "
					+ "where d.pemesananPengadaanMasterAsset.kode like :q", q);
			// BAST (kode Penerimaan) — PO Penerimaan sama dengan PO termin
			tambahIdTermin(session, td, "select d.id from " + TD + " d, " + BAST + " b "
					+ "where b.pemesananPengadaanMasterAsset = d.pemesananPengadaanMasterAsset "
					+ "and b.kode like :q", q);
			// Terima Tagihan (SaldoAwalMasterAsset dari BAST)
			tambahIdTermin(session, td, "select d.id from " + TD + " d, " + BAST + " b "
					+ "where b.pemesananPengadaanMasterAsset = d.pemesananPengadaanMasterAsset "
					+ "and (b.saldoAwalMasterAsset.kode like :q or b.saldoAwalMasterAsset.kodeTagihan like :q)", q);
			// PR: kode PR → id PR-detail → PO ber-CSV memuat id itu
			try {
				List<?> prDetailIds = session.createQuery("select pd.id from " + PRD + " pd "
						+ "where pd.permintaanPengadaanMasterAsset.kode like :q").setString("q", q).list();
				for (Object o : prDetailIds) {
					if (o == null) {
						continue;
					}
					tambahIdTermin(session, td, "select d.id from " + TD + " d "
							+ "where d.pemesananPengadaanMasterAsset.permintaanPengadaanMasterAssets like :q",
							"%" + o + "%");
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		cacheKodeIndukSearch = search;
		cacheKeyDataTermin = td;
		return td;
	}

	/** Jalankan satu query HQL (parameter {@code :q}) dan tambahkan id Long hasilnya ke {@code out}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void tambahIdTermin(Session session, java.util.Set<Long> out, String hql, String qParam) {
		try {
			List hasil = session.createQuery(hql).setString("q", qParam).list();
			for (Object o : hasil) {
				if (o instanceof Long) {
					out.add((Long) o);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian data pajak berdasarkan filter yang aktif
	 * saat ini dan memperbarui tampilan grid dengan hasil yang ditemukan.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode ini melakukan dua pemanggilan {@link #initCriteria(boolean)}:
	 * <ol>
	 *   <li>Pertama dengan {@code order=false} untuk menghitung total record dan
	 *       memperbarui komponen paging (jumlah halaman, navigasi).</li>
	 *   <li>Kedua dengan {@code order=true} untuk mengambil data aktual dengan
	 *       pembatasan {@code setMaxResults} dan {@code setFirstResult} berdasarkan
	 *       halaman aktif pada komponen paging.</li>
	 * </ol>
	 * Hasil query berupa {@code List<Pajak>} dibungkus dalam {@code SimpleListModel}
	 * dan di-set ke grid bersama dengan instance {@code PajakRenderer} yang baru.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate akan diteruskan ke framework ZK. Jika {@code paging}
	 * null (misalnya pada halaman tanpa paging), nilai {@code setFirstResult(0)}
	 * digunakan sehingga selalu menampilkan dari awal.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil dari banyak tempat: timer default, listener paging,
	 * listener filter satuan kerja, dan callback event lainnya. Pastikan metode
	 * ini tetap idempoten dan tidak memiliki efek samping selain memperbarui
	 * tampilan grid.</p>
	 *
	 * @param event event ZK yang memicu pencarian (bisa null jika dipanggil
	 *              secara programatik, misalnya saat inisialisasi atau dari timer)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pajak> pajak = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pajak);
		grid.setRowRenderer(new PajakRenderer());
		grid.setModelCheckMobile(strset);

	}

}
